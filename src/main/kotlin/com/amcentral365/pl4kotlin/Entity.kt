package com.amcentral365.pl4kotlin

import mu.KotlinLogging
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.jvmName


private val logger = KotlinLogging.logger {}

/**
 * Serves as base for application classes defining POJO to database table mapping.
 *
 * The class's constructor parses [Table] and [Column] annotations and translates them into
 * a list of [ColDef]. The list is used by [BaseStatement] and its derivatives to build SQL
 * statements. It is also used to determine bind value types.
 *
 * The annotations are only parsed on first use and then cached. Instantiating subsequent
 * objects bears no penalty.
 */
abstract class Entity protected constructor() {

    /**
     * Called prior to inserting object into the table to ensure the data is consistent.
     * Needs to be implemented by the DAO.
     * @return the error message explaining what's wrong with the data, or null if the data is valid.
     */
    fun preInsertDataValidator(): String? = null

    // ------------------------------------------------------------- Housekeeping
    private val tblDefKey: String = this.javaClass.name
    private companion object {
        private val tblDefsMap = ConcurrentHashMap<String, TableDef>()
    }

    init {
        // There may be multiple objects of this class. We only parse/validate annotations the first time
        if( !Entity.tblDefsMap.containsKey(this.tblDefKey) ) {
            this.constructFromAnnotations()
        }
    }

    /** Assign instance values from REST parameters map. See [assignFrom] */
    constructor(restParams: Map<String, String>): this() { this.assignFrom(restParams) }

    /** Assign instance values from another Entity object */
    constructor(other: Entity): this() {
        require(this::class == other::class)
        for(k in 0..this.colDefs.size) {
            this.colDefs[k].setValue(other.colDefs[k].getValue())
        }
    }

    /**
     * Defines an Entity field (or variable, or property, in Kotlin terms) associated with a database column.
     *
     * The class is declared inner to associate a ColDef it with its corresponding Entity.
     * @property fieldName The Kotlin property name.
     * @property fieldType The library's code for the field type. Only a subset of JDBC types is supported.
     * @property columnName The database table's column name. Checked for uniqueness.
     * @property restParamName Used to instantiate objects from REST data
     * @property pkPos Column position withing the table's Primary Key. Sequential, must start with 1.
     * @property onInsert Defines handling of the database column when a record is added to the database table.
     * @property isOptLock Defines the column as Optimistic Lock. Only one column of type Number or Timestamp
     *                     can be marked as OptLock.
     */
    inner class ColDef private constructor(
            val fieldName:        String,
            val fieldType:        JdbcTypeCode,
            val columnName:       String,
            val restParamName:    String,
            val pkPos:            Int,
            val onInsert:         Generated,
            val isOptLock:        Boolean,
            private val javaType: java.lang.reflect.Type
    ): Comparable<ColDef> {

        internal var prop: KMutableProperty1<out Entity, Any?>

        init {
            val msgPrefix = "DAO error in class ${this@Entity::class.java.name}(${this@Entity.tableName}), field ${this.fieldName}"

            require( !this.isOptLock
                    || this.fieldType == JdbcTypeCode.Timestamp
                    || Number::class.java.isAssignableFrom(JdbcTypeCode.clazz(this.fieldType))
            ) {
                "$msgPrefix: supported Optimistic Lock types are Timestamp and Number, got ${this.fieldType.name}"
            }

            require( this.fieldType != JdbcTypeCode.JsonStr || javaType.typeName == "java.lang.String" ) {
                "$msgPrefix: db-side Json is only supported for client-side String type. The poperty type is ${javaType.typeName}"
            }

            require( this.pkPos == 0 || !this.isOptLock )
                { "$msgPrefix: optimistic lock can't be part of the PK" }

            // Check the column property is writable, and cache the property for reads and assignments
            val p = this@Entity::class.declaredMemberProperties.first { it.name == this.fieldName }
            require(p is KMutableProperty1<out Entity, Any?> )
                { "$msgPrefix: the field must be writable, e.g. have a Kotlin setter associated with it" }
            this.prop = p as KMutableProperty1<out Entity, Any?>

            // PK columns can't be generated by the database. When inserting, we fetch them back by PK and must know it.
            require( this.pkPos == 0 || (this.onInsert != Generated.OnTheDbAlways && this.onInsert != Generated.OnTheDbWhenNull) )
                { "$msgPrefix: PK columns can't be generated on the database side, value ${this.onInsert} is illegal" }

            // client-side generated fields must be of supported type
            val generatedOnTheClient = this.onInsert == Generated.OnTheClientAlways || this.onInsert == Generated.OneTheClientWhenNull
            val supportsGeneration   = this.fieldType == JdbcTypeCode.UUID || fieldType == JdbcTypeCode.Timestamp
            require( !generatedOnTheClient || supportsGeneration )
                { "$msgPrefix: client-side generation is only supported for UUID and Timestamp. Got ${this.fieldType.name}" }
        }


        internal constructor(other: ColDef):
            this(other.fieldName, other.fieldType, other.columnName, other.restParamName, other.pkPos,
                    other.onInsert, other.isOptLock, other.javaType)
        {
            this.prop = other.prop
        }

        internal constructor(kProp: KProperty<Any?>, colAnnotation: Column):
            this(kProp.name
                , JTC(kProp, colAnnotation.isJson)
                , colAnnotation.columnName
                , if( colAnnotation.restParamName.isNotEmpty() ) colAnnotation.restParamName else colAnnotation.columnName
                , colAnnotation.pkPos
                , colAnnotation.onInsert
                , colAnnotation.isOptimisticLock
                , kProp.returnType.javaType
            )

        /** Part of Comparable interface. The order is [fieldName], [fieldType], etc. */
        override fun compareTo(other: ColDef): Int {
            var cmp: Int
            cmp = this.fieldName    .compareTo(other.fieldName);      if( cmp != 0 ) return cmp
            cmp = this.fieldType    .compareTo(other.fieldType);      if( cmp != 0 ) return cmp
            cmp = this.columnName   .compareTo(other.columnName);     if( cmp != 0 ) return cmp
            cmp = this.restParamName.compareTo(other.restParamName);  if( cmp != 0 ) return cmp
            cmp = this.pkPos        .compareTo(other.pkPos);          if( cmp != 0 ) return cmp
            cmp = this.onInsert     .compareTo(other.onInsert);       if( cmp != 0 ) return cmp
            cmp = this.isOptLock    .compareTo(other.isOptLock);      if( cmp != 0 ) return cmp
            cmp = this.javaType.javaClass.name.compareTo(other.javaType.javaClass.name);   if( cmp != 0 ) return cmp
            return 0
        }

        /** Assign value to the appropriate Entity member. Nothing is changed on the database, just the property value is set. */
        fun setValue(value: Any?) = this.prop.setter.call(this@Entity, value)

        /** Get current property value of the Entity member. Nothing is read from the database, just the current value is returned.*/
        fun getValue(): Any? = this.prop.getter.call(this@Entity)

        /**
         * Return the property value, generating it if needed, as defined by the [onInsert] annotation.
         *
         * When the property's [onInsert] annotation is [Generated.OnTheClientAlways] or
         * [Generated.OneTheClientWhenNull], the value is computed by this function (when other
         * conditions apply). The property is set to the generated value.
         *
         * @throws UnsupportedOperationException when property type is not supported, Supported types
         *      are [UUID] and [Timestamp],
         */
        fun getBindValue(inserting: Boolean = false): Any? {
            var value = this.getValue()
            if( inserting )
                if(this.onInsert == Generated.OnTheClientAlways ||
                  (this.onInsert == Generated.OneTheClientWhenNull && value == null))
                {
                    value = this.generateValue(this.fieldType)
                    this.setValue(value)
                }
            return value
        }

        private fun generateValue(fieldType: JdbcTypeCode): Any {
            if( fieldType == JdbcTypeCode.UUID ) return UUID.randomUUID()
            else if( fieldType == JdbcTypeCode.Timestamp ) return Timestamp(System.currentTimeMillis())

            // we check for proper type in the constructor, but not in all paths
            throw UnsupportedOperationException(
                "${this@Entity::class.jvmName}(${this@Entity.tableName}) field ${this.prop.name}: "+
                " client-side generation is only supported for UUID and Timestamp. Got $fieldType"
            )
        }

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
            value = if( this.fieldType == JdbcTypeCode.Enum )
                        JdbcTypeCode.enumValFromStr(this.javaType, str)
                    else {
                        val parser = JdbcTypeCode.getParser(this.fieldType) ?:
                                throw UnsupportedOperationException("Parsing of type ${this.fieldType} is not supported")
                        parser.parse(str)
                    }

            logger.debug { "setting ${this@Entity.tableName}.${this.fieldName} to $value" }
            this.setValue(value)
        }

        /**
         * Get column value in JSON format
         *
         * The value is unquoted for types where it doesn't need to be, such as numeric and boolean.
         * Nulls are returned as an unquoted string 'null'
         */
        fun asJsonValue(): String {
            val converter = JdbcTypeCode.getToJsonConverter(this.fieldType)
            val rval = this.getValue()
            val sval = if( rval == null ) "null" else converter.convert(rval)
            return sval
        }

        /**
         * Get column as a JSON <code>"name": value</code> pair string
         */
        fun asJsonNameValue(): String = "\"${this.restParamName}\": ${this.asJsonValue()}"
    }


    // ------------------------------------------------------------- Exposed variables
    val tableName = Entity.tblDefsMap[this.tblDefKey]!!.tableName
    val colDefs   = Entity.tblDefsMap[this.tblDefKey]!!.colDefs.map { this.ColDef(it) }

    val pkCols                 = Entity.tblDefsMap[this.tblDefKey]!!.pkCols.map { this.ColDef(it) }
    val pkAndOptLockCols       = Entity.tblDefsMap[this.tblDefKey]!!.pkAndOptLockCols.map { this.ColDef(it) }
    val allCols                = this.colDefs
    val allColsButPk           = Entity.tblDefsMap[this.tblDefKey]?.allColsButPk?.map { this.ColDef(it) }
    val allColsButPkAndOptLock = Entity.tblDefsMap[this.tblDefKey]?.allColsButPkAndOptLock?.map { this.ColDef(it) }
    val optLockCol             =
        if( Entity.tblDefsMap[this.tblDefKey]?.optLockCol == null ) null
        else ColDef(Entity.tblDefsMap[this.tblDefKey]?.optLockCol!!)

    // ------------------------------------------------------------- Methods
    private fun constructFromAnnotations() {
        val table = this::class.findAnnotation<Table>()
        require( table != null )
            { "DAO error: class: ${this::class.java.name} must be annotated with @Table to be used as an Entity" }

        val tableName = table!!.tableName.trim()
        require( tableName.isNotEmpty() )
            { "DAO error in class: ${this::class.java.name}: annotation attribute tableName can't be empty" }

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

        require( cdefs.isNotEmpty() )
            { "DAO error: class ${this::class.java.name} does not declare any fields wih Column annotation" }

        val pkProblems: String? = this.validatePkPositions(cdefs)
        require( pkProblems.isNullOrEmpty() )
            { "DAO error in class ${this::class.java.name}, the PKs are messed up: $pkProblems" }

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
            return "duplicate fields with pkPos $dupPk: ${cdefsByPkPos[dupPk]!!.joinToString(", ") { it.fieldName }}"

        return null
    }

    /**
     * Assign instance values from REST parameters map
     *
     * Keys of [restParams] are mapped to [ColDef.restParamName] of each
     * object's property. The value is parsed to the appropriate type from String.
     * Note1: not all types have parsers,
     * Note2: [ColDef.restParamName] defaults to the database column name and isn't case sensitive
     *
     * @throws UnsupportedOperationException when the value has no string parser
     * @throws Exception on other errors
     */
    fun assignFrom(restParams: Map<String, String>) {
        for(colDef in this.colDefs) {
            var strVal = "~not-set~"
            try {
                if( restParams.contains(colDef.restParamName) ) {
                    strVal = restParams.getValue(colDef.restParamName)
                    colDef.parse(strVal)
                }
            } catch (x: Exception) {
                throw Exception(
                        "while trying to parse property ${colDef.fieldName} "+
                                "from rest param ${colDef.restParamName}, value '$strVal'"
                        , x
                )
            }
        }
    }

    /**
     * Get JSON string representing the object identity: PK and, if present, optLock
     *
     * The string format is {pk: [ col: val, ...], optLock: val}
     * All identifiers and values are double-quoted.
     * The optLock portion is only returned if the OptLock column was defined,
     */
    fun getIdentityAsJsonStr(): String {
        val pkStr = this.pkCols.joinToString(", ", prefix = "\"pk\": {", postfix = "}") { it.asJsonNameValue() }

        if( this.optLockCol == null )
            return "{$pkStr}"

        val optLockStr = """"optLock": {${this.optLockCol.asJsonNameValue()}}"""
        return "{$pkStr, $optLockStr}"
    }

    /**
     * Get JSON string representing the object value.
     *
     * The output format is {rest-param1-name: value1, rest-param2-name: value2, ...}
     * When [pretty] is set, each value appears on a separate line with 2 character indent.
     * Null values are included when [withNulls] is true.
     * The values are sorted by their [restParamName].
     */
    fun asJsonStr(pretty: Boolean = false, withNulls: Boolean = false): String {
        val sep = if( pretty ) ",\n  " else ", "
        val pre = if( pretty ) "{\n  " else "{"
        val pst = if( pretty ) "\n}"   else "}"
        return this.colDefs
                .filter { withNulls || it.getValue() != null }
                .sortedBy { it.restParamName }
                .joinToString(sep, prefix=pre, postfix=pst) { it.asJsonNameValue() }
    }


}