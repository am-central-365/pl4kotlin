package com.amcentral365.pl4kotlin

import java.sql.Timestamp
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation

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


    // ------------------------------------------------------------- Exposed variables
    internal val tableName = Entity.tblDefsMap.get(this.tblDefKey)?.tableName
    internal val colDefs   = Entity.tblDefsMap.get(this.tblDefKey)?.colDefs

    internal val pkCols                 = Entity.tblDefsMap.get(this.tblDefKey)?.pkCols
    internal val optLockCol             = Entity.tblDefsMap.get(this.tblDefKey)?.optLockCol
    internal val pkAndOptLockCols       = Entity.tblDefsMap.get(this.tblDefKey)?.pkAndOptLockCols
    internal val allColsButPk           = Entity.tblDefsMap.get(this.tblDefKey)?.allColsButPk
    internal val allColsButPkAndOptLock = Entity.tblDefsMap.get(this.tblDefKey)?.allColsButPkAndOptLock

    // ------------------------------------------------------------- Methods
    private fun constructFromAnnotations() {
        val table = this::class.findAnnotation<Table>()
        require( table != null ) {
            "DAO error: class: ${this::class.qualifiedName} must be annotated with @Table to be used as an Entity"
        }
        require( table!!.tableName.isNotEmpty() ) {
            "DAO error in class: ${this::class.qualifiedName}: annotation attribute tableName can't be empty"
        }

        var colWithOptLock: ColDef? = null
        val cdefs = mutableListOf<ColDef>()

        this::class.declaredMemberProperties.forEach {
            val column = it.findAnnotation<Column>()
            if( column != null ) {
                val cdef: ColDef = ColDef(it.name, Int::class, column)
                if( cdef.isOptLock ) {
                    require( colWithOptLock == null ) {
                        "DAO error: class ${this::class.qualifiedName} defines more than one "
                        "Optimistic Lock fields: ${colWithOptLock!!.fieldName} and ${cdef.fieldName}"
                    }
                    colWithOptLock = cdef
                }

                cdefs.add(cdef)
            }
        }

        require( cdefs.isNotEmpty() ) {
            "DAO error: class ${this::class.qualifiedName} does not declare any fields wih Column annotation"
        }

        val pkProblems: String? = this.validatePkPositions(cdefs)
        require( pkProblems.isNullOrEmpty() ) {
            "DAO error in class ${this::class.qualifiedName} the PKs are messed up: $pkProblems"
        }

        Entity.tblDefsMap.putIfAbsent(this.tblDefKey, TableDef(table.tableName, cdefs))
    }


    private fun validatePkPositions(cdefs: List<ColDef>): String? {
        val cdefsByPkPos = cdefs.filter { it.pkPos != 0 }.groupBy { it.pkPos }

        val maxPkPos = cdefsByPkPos.keys.max()
        if( maxPkPos == null )
            return "DAO class ${this::class.qualifiedName} has no PK fields. At least one field must have Column annotation with pkPos 1"
        if( maxPkPos > cdefsByPkPos.size )
            return "DAO error in class ${this::class.qualifiedName}, field ${cdefsByPkPos[maxPkPos]!![0].fieldName}: PK position $maxPkPos is greater than the number of PK fields ${cdefsByPkPos.size}"

        val missingPks = IntRange(1, maxPkPos).minus(cdefsByPkPos.keys)
        if( missingPks.isNotEmpty() )
            return "DAO error in class ${this::class.qualifiedName}: unexpected pkPos values ${missingPks.joinToString(", ")}"

        val dupPk = cdefsByPkPos.keys.firstOrNull { cdefsByPkPos[it]!!.size > 1 }
        if( dupPk != null )
            return "DAO error in class ${this::class.qualifiedName}: duplicate fields with pkPos ${dupPk}: ${cdefsByPkPos[dupPk]!!.map { it.fieldName }.joinToString(", ")}"

        return null
    }

}