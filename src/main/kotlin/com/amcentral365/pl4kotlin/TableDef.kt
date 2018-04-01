package com.amcentral365.pl4kotlin

internal class TableDef constructor(val tableName: String, val colDefs: List<Entity.ColDef>) {

    val allCols    = this.colDefs // an alias
    val pkCols     = this.colDefs.filter { it.pkPos > 0 }.sortedBy { it.pkPos }
    val optLockCol = this.colDefs.firstOrNull { it.isOptLock }
    val pkAndOptLockCols       = this.colDefs.filter { it.pkPos > 0 || it.isOptLock }
    val allColsButPk           = this.colDefs.filter { it.pkPos == 0 }
    val allColsButPkAndOptLock = this.colDefs.filter { it.pkPos == 0 && !it.isOptLock }
}
