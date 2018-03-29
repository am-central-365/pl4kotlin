package com.amcentral365.pl4kotlin

import java.sql.Timestamp
import kotlin.reflect.KClass

internal class ColDef constructor(val fieldName: String, val fieldType: KClass<*>, colDef: Column) {
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
                || this.fieldType == Timestamp::class
                || Number::class.javaObjectType.isAssignableFrom(this.fieldType.javaObjectType)
        ) {
            "DAO error in class ${this::class.java.name}, field ${this.fieldName}: "
            "supported Optimistic Lock types are Timestamp and Number, got ${this.fieldType.java.name}"
        }

        require( this.pkPos == 0 || !this.isOptLock ) {
            "DAO error in class ${this::class.java.name}, field ${this.fieldName}: "
            "optimistic lock can't be part of the PK"
        }

    }
}
