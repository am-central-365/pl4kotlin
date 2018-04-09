package com.amcentral365.pl4kotlin

import com.google.common.annotations.VisibleForTesting
import mu.KLogging
import java.sql.Connection
import kotlin.reflect.KProperty


open class UpdateStatement(entityDef: Entity, getGoodConnection: () -> Connection? = { null }): BaseStatement(entityDef, getGoodConnection) {
    companion object: KLogging()

    @VisibleForTesting internal val updateDescrs:    MutableList<BaseStatement.Descr> = mutableListOf()
    @VisibleForTesting internal val whereDescrs:     MutableList<BaseStatement.Descr> = mutableListOf()
    @VisibleForTesting internal val fetchbackDescrs: MutableList<BaseStatement.Descr> = mutableListOf()
    @VisibleForTesting internal val bindVals:        MutableList<Any?> = mutableListOf()

    override fun run(conn: Connection): Int = this.runDML(conn, this.bindVals, this.fetchbackDescrs)

    // ----- Select columns and expressions
    // Just like SelectStatement's select(), we specify columns to update by property or name.
    // Both translate to "set column = ?".
    //
    // The expression form translates to: "set column = expr(binds)".
    // Unlike SelectStatement, we allow specifying target column by its name, because it makes sense in SQL.
    fun update(prop: KProperty<Any>): UpdateStatement  // set col = propVal
        { this.addProperty(this.updateDescrs, prop);  return this }
    fun update(targetProp: KProperty<Any>, expr: String, vararg binds: Any?): UpdateStatement   // set col = expr(binds)
        { this.addProperty(this.updateDescrs, targetProp, expr, *binds);  return this }

    fun update(colName: String): UpdateStatement  // set col = propVal
        { this.addColName(this.updateDescrs, colName);  return this }
    fun update(colName: String, expr: String, vararg binds: Any?): UpdateStatement   // set col = expr(binds)
        { this.addColName(this.updateDescrs, colName, expr, *binds);  return this }

    // to use with auxilarly column filters: allColsButPk, allColsButPkAndOptLock, etc
    fun update(colDefs: List<Entity.ColDef>): UpdateStatement { this.updateDescrs.addAll(colDefs.map { Descr(it) });  return this }


    // ----- WHERE columns or expressions
    // Beside frequently used PK/OptLock expressions, there are two other forms:
    // 1) property or column: translates to "WHERE column = ?", and the bind value comes from the property
    // 2) a free-form expressions with bind values. Bind values provided are computed and saved.

    // auxiliary, frequently used forms
    fun byPk():            UpdateStatement { this.whereDescrs.addAll(this.entityDef.pkCols.          map { Descr(it) });  return this }
    fun byPkAndOptLock():  UpdateStatement { this.whereDescrs.addAll(this.entityDef.pkAndOptLockCols.map { Descr(it) });  return this }

    // when prop or column name is used, its ColDef is detected, and the resulting statement translates to
    // "WHERE colName = ?". The property value is bound when the statement is ran.
    fun by(prop: KProperty<Any>): UpdateStatement { this.addProperty(this.whereDescrs, prop);     return this }
    fun by(colName: String):      UpdateStatement { this.addColName (this.whereDescrs, colName);  return this } // how is it not expr?

    // free form clause, allowing to specify expressions and use any column
    fun by(expr: String, vararg binds: Any?): UpdateStatement { this.addColName(this.whereDescrs, null, expr, *binds);  return this }


    // ----- FETCHBACK columns
    // These columns are read back after update, by the Pk (adding OptLock is redundant because we ar in the same transaction)
    fun fetchBack(prop:    KProperty<Any>): UpdateStatement { this.addProperty(this.fetchbackDescrs, prop);    return this }
    fun fetchBack(colName: String):         UpdateStatement { this.addColName(this.fetchbackDescrs, colName);  return this }


    public override fun build(): String {
        require(this.updateDescrs.isNotEmpty())
        this.bindVals.clear()
        val sb = StringBuilder("UPDATE ").append(this.entityDef.tableName).append(" SET ")

        sb.append(
            this.updateDescrs.joinToString(", ") {
                if( it.expr == null ) {
                    this.bindVals.add(it.colDef)
                    it.colDef!!.columnName + " = ?"
                } else {
                    this.bindVals.addAll(it.binds)
                    it.colDef!!.columnName + " = " + it.expr
                }
            }
        )

        if( this.whereDescrs.isNotEmpty() ) {
            sb.append(" WHERE ")
            sb.append(this.emitWhereList(this.whereDescrs))
            this.whereDescrs.forEach { this.bindVals.addAll(it.binds) }
        }

        return sb.toString()
    }

}