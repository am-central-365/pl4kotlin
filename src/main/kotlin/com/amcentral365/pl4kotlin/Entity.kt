package com.amcentral365.pl4kotlin

import mu.KotlinLogging
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaType

private val logger = KotlinLogging.logger {}

abstract class Entity protected constructor() {

    /**
     * Called prior to inserting object into the table to ensurethe data is consistent.
     * Needs to be implemented by the DAO.
     * @return the error message explaining what's wrong with the data, or null if the data is valid.
     */
    fun preInsertDataValidator(): String? = null

    // ------------------------------------------------------------- Housekeeping
    val tblDefKey = this.javaClass.name
    companion object {
        private val tblDefsMap = ConcurrentHashMap<String, TableDef>()
    }

    init {
        // There may be multiple objects of this class. We only parse/validate annotations the first time
        if( !Entity.tblDefsMap.containsKey(this.tblDefKey) ) {
            this.constructFromAnnotations()
        }
    }

    /**
     * Defines an Entity field (or variable, or property, in Kotlin terms) associated with a database column.
     *
     * The class is declared inner to associate a ColDef it with its corresponding Entity.
     */
    inner class ColDef constructor(kProp: KProperty<Any?>, colAnnotation: Column) {
        val fieldName:     String
        val fieldType:     JdbcTypeCode
        val columnName:    String = colAnnotation.columnName
        val restParamName: String
        val pkPos:         Int
        val onInsert:      Generated
        val isOptLock:     Boolean
        val javaType:      java.lang.reflect.Type

        internal val prop:     KMutableProperty1<out Entity, Any?>

        init {
            this.fieldName = kProp.name
            this.fieldType = JTC(kProp)
            this.restParamName = if( colAnnotation.restParamName.isNotEmpty() ) colAnnotation.restParamName else fieldName
            this.pkPos = colAnnotation.pkPos
            this.onInsert = colAnnotation.onInsert
            this.isOptLock = colAnnotation.isOptimisticLock
            this.javaType = kProp.returnType.javaType

            require( !this.isOptLock
                    || this.fieldType == JdbcTypeCode.Timestamp
                    || Number::class.java.isAssignableFrom(JdbcTypeCode.clazz(this.fieldType))
            ) {
                "DAO error in class ${this::class.java.name}, field ${this.fieldName}: " +
                "supported Optimistic Lock types are Timestamp and Number, got ${this.fieldType.name}"
            }

            require( this.pkPos == 0 || !this.isOptLock ) {
                "DAO error in class ${this::class.java.name}, field ${this.fieldName}: " +
                "optimistic lock can't be part of the PK"
            }

            // Check the column property is writeable, and cache the property for reads and assignments
            val p = this@Entity::class.declaredMemberProperties.first { it.name == this.fieldName }
            require(p is KMutableProperty1<out Entity, Any?> ) {
                "DAO error in class ${this::class.java.name}, field ${this.fieldName}: " +
                "the field must be writeable, e.g. there is no Kotlin setter associated with it"
            }
            this.prop = p as KMutableProperty1<out Entity, Any?>
        }

        /**
         * Assign value to the appropriate Entity member.
         * Nothing is changed on the database, just the property value is set.
         */
        fun setValue(value: Any?) = this.prop.setter.call(this@Entity, value)

        fun getValue(): Any? = this.prop.getter.call(this@Entity)

        /**
         * Read value at specific index of ResultSet into the property.
         * The indexes are 1-based. The method converts Enum strings to values.
         */
        fun read(rs: ResultSet, idx: Int): Any? =
            this.setValue(
                if( this.fieldType == JdbcTypeCode.Enum ) JdbcTypeCode.enumValFromStr(this.javaType, rs.getString(idx))
                else                                      JdbcTypeCode.getReader(this.fieldType).read(rs, idx)
            )

        /**
         * Assign value to the appropriate Entity member and bind it to the specified variable of the statement.
         */
        fun bind(ps: PreparedStatement, idx: Int, value: Any?) {
            this.setValue(value)
            this.bind(ps, idx)
        }

        /**
         * Bind property value to the specified variable of the statement.
         */
        fun bind(ps: PreparedStatement, idx: Int) =
            JdbcTypeCode.getBinder(this.fieldType).bind(ps, idx, this.prop.getter.call())


        /**
         * Parse value from String and assign it o the property
         * Assign value to the appropriate Entity member and bind it to the specified variable of the statement.
         */
        fun parse(str: String) {
            val value: Any?
            if( this.fieldType == JdbcTypeCode.Enum )
                value = JdbcTypeCode.enumValFromStr(this.javaType, str)
            else {
                val parser = JdbcTypeCode.getParser(this.fieldType) ?:
                    throw UnsupportedOperationException("Parsing of type ${this.fieldType} is not supported")
                value = parser.parse(str)
            }

            logger.debug { "setting ${this@Entity.tableName}.${this.fieldName} to $value" }
            this.setValue(value)
        }
    }


    // ------------------------------------------------------------- Exposed variables
    internal val tableName = Entity.tblDefsMap[this.tblDefKey]!!.tableName
    internal val colDefs   = Entity.tblDefsMap[this.tblDefKey]!!.colDefs

    internal val pkCols                 = Entity.tblDefsMap[this.tblDefKey]!!.pkCols
    internal val optLockCol             = Entity.tblDefsMap[this.tblDefKey]?.optLockCol
    internal val pkAndOptLockCols       = Entity.tblDefsMap[this.tblDefKey]!!.pkAndOptLockCols
    internal val allColsButPk           = Entity.tblDefsMap[this.tblDefKey]?.allColsButPk
    internal val allColsButPkAndOptLock = Entity.tblDefsMap[this.tblDefKey]?.allColsButPkAndOptLock

    // ------------------------------------------------------------- Methods
    private fun constructFromAnnotations() {
        val table = this::class.findAnnotation<Table>()
        require( table != null ) {
            "DAO error: class: ${this::class.java.name} must be annotated with @Table to be used as an Entity"
        }
        val tableName = table!!.tableName.trim()
        require( tableName.isNotEmpty() ) {
            "DAO error in class: ${this::class.java.name}: annotation attribute tableName can't be empty"
        }

        var colWithOptLock: ColDef? = null
        val cdefs = mutableListOf<ColDef>()

        this::class.declaredMemberProperties.forEach {
            val colAnnotation = it.findAnnotation<Column>()
            if( colAnnotation != null ) {
                val cdef = ColDef(it, colAnnotation)
                if( cdef.isOptLock ) {
                    require( colWithOptLock == null ) {
                        "DAO error: class ${this::class.java.name} defines more than one " +
                        "Optimistic Lock fields: ${colWithOptLock!!.fieldName} and ${cdef.fieldName}"
                    }
                    colWithOptLock = cdef
                }

                val sameColCdef = cdefs.firstOrNull { it.columnName == cdef.columnName }
                require(sameColCdef == null ) {
                    "DAO error: class ${this::class.java.name} declares same column name " +
                    "${cdef.columnName} in fields ${sameColCdef!!.fieldName} and ${cdef.fieldName}"
                }

                cdefs.add(cdef)
            }
        }

        require( cdefs.isNotEmpty() ) {
            "DAO error: class ${this::class.java.name} does not declare any fields wih Column annotation"
        }

        val pkProblems: String? = this.validatePkPositions(cdefs)
        require( pkProblems.isNullOrEmpty() ) {
            "DAO error in class ${this::class.java.name}, the PKs are messed up: $pkProblems"
        }

        Entity.tblDefsMap.putIfAbsent(this.tblDefKey, TableDef(tableName, cdefs))
    }

    private fun validatePkPositions(cdefs: List<ColDef>): String? {
        val cdefsByPkPos = cdefs.filter { it.pkPos != 0 }.groupBy { it.pkPos }

        val maxPkPos = cdefsByPkPos.keys.max() ?: return "PK is missing. At least one field must have Column annotation with pkPos 1"
        //if( maxPkPos > cdefsByPkPos.size )
        //    return "field ${cdefsByPkPos[maxPkPos]!![0].fieldName}: PK position $maxPkPos is greater than the number of PK fields ${cdefsByPkPos.size}"

        val missingPks = IntRange(1, maxPkPos).minus(cdefsByPkPos.keys)
        if( missingPks.isNotEmpty() )
            return "missing pkPos values ${missingPks.joinToString(",")}"

        val extraPks = cdefsByPkPos.keys.minus(IntRange(1, maxPkPos))
        if( extraPks.isNotEmpty() )
            return "unexpected pkPos values ${extraPks.joinToString(",")}"

        val dupPk = cdefsByPkPos.keys.firstOrNull { cdefsByPkPos[it]!!.size > 1 }
        if( dupPk != null )
            return "duplicate fields with pkPos $dupPk: ${cdefsByPkPos[dupPk]!!.map { it.fieldName }.joinToString(", ")}"

        return null
    }

}