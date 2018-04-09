package com.amcentral365.pl4kotlin

import com.google.common.annotations.VisibleForTesting
import mu.KLogging
import java.sql.Connection
import java.sql.PreparedStatement
import kotlin.reflect.KProperty


open class DeleteStatement(entityDef: Entity, getGoodConnection: () -> Connection? = { null }): BaseStatement(entityDef, getGoodConnection) {
    companion object: KLogging()

    @VisibleForTesting internal val whereDescrs:  MutableList<BaseStatement.Descr> = mutableListOf()
    @VisibleForTesting internal val bindVals:     MutableList<Any?> = mutableListOf()

    override fun run(conn: Connection): Int = this.runDML(conn, this.bindVals, null)


    // ----- WHERE columns or expressions
    // Beside frequently used PK/OptLock expressions, there are two other forms:
    // 1) property or column: translates to "WHERE column = ?", and the bind value comes from the property
    // 2) a free-form expressions with bind values. Bind values provided are computed and saved.

    // auxiliary, frequently used forms
    fun byPk():            DeleteStatement { this.whereDescrs.addAll(this.entityDef.pkCols.          map { Descr(it) });  return this }
    fun byPkAndOptLock():  DeleteStatement { this.whereDescrs.addAll(this.entityDef.pkAndOptLockCols.map { Descr(it) });  return this }

    // when prop or column name is used, its ColDef is detected, and the resulting statement translates to
    // "WHERE colName = ?". The property value is bound when the statement is ran.
    fun by(prop: KProperty<Any>): DeleteStatement { this.addProperty(this.whereDescrs, prop);     return this }
    fun by(colName: String):      DeleteStatement { this.addColName (this.whereDescrs, colName);  return this } // how is it not expr?

    // free form clause, allowing to specify expressions and use any column
    fun by(expr: String, vararg binds: Any): DeleteStatement { this.addColName(this.whereDescrs, null, expr, *binds);  return this }


    public override fun build(): String {
        this.bindVals.clear()
        val sb = StringBuilder("DELETE FROM ").append(this.entityDef.tableName)

        if( this.whereDescrs.isNotEmpty() ) {
            sb.append(" WHERE ")
            sb.append(this.emitWhereList(this.whereDescrs))
            this.whereDescrs.forEach { this.bindVals.addAll(it.binds) }
        }

        return sb.toString()
    }

}