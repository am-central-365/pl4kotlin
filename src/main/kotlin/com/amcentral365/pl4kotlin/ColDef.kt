package com.amcentral365.pl4kotlin

import java.sql.Timestamp
import kotlin.reflect.KClass

internal class ColDef constructor(val fieldName: String, val fieldType: JdbcTypeCode, colDef: Column) {
    val columnName:    String = colDef.columnName
    val restParamName: String
    val pkPos:         Int
    val onInsert:      Generated
    val isOptLock:     Boolean

    init {
        this.restParamName = if( colDef.restParamName.isNotEmpty() ) colDef.restParamName else fieldName
        this.pkPos = colDef.pkPos
        this.onInsert = colDef.onInsert
        this.isOptLock = colDef.isOptimisticLock

        require( !this.isOptLock
                || this.fieldType == JdbcTypeCode.Timestamp
                || Number::class.java.isAssignableFrom(JdbcTypeCode.clazz(this.fieldType))
        ) {
            "DAO error in class ${this::class.java.name}, field ${this.fieldName}: "
            "supported Optimistic Lock types are Timestamp and Number, got ${this.fieldType.name}"
        }

        require( this.pkPos == 0 || !this.isOptLock ) {
            "DAO error in class ${this::class.java.name}, field ${this.fieldName}: "
            "optimistic lock can't be part of the PK"
        }

    }
}
