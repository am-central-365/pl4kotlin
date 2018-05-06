package com.amcentral365.pl4kotlin

import mu.KLogging
import java.sql.Connection
import kotlin.reflect.KProperty

/** Run SQL `DELETE` statement for the given [entityDef]. See [BaseStatement] constructor for parameters description. */
open class DeleteStatement(entityDef: Entity, getGoodConnection: () -> Connection? = { null }): BaseStatement(entityDef, getGoodConnection) {
    private companion object: KLogging()  /** @suppress */

    /*@VisibleForTesting*/ internal val whereDescrs:  MutableList<BaseStatement.Descr> = mutableListOf()
    private  val bindVals:     MutableList<Any?> = mutableListOf()

    override fun run(conn: Connection): Int = this.runDML(conn, this.bindVals, null)

    // --- auxiliary, frequently used conditions

    /** Add Primary Key columns to `WHERE` clause of the `DELETE` statement */
    fun byPk(): DeleteStatement
        { this.whereDescrs.addAll(this.entityDef.pkCols. map { Descr(it) });  return this }

    /** Add Primary Key and, if defined, the Optimistic Lock column to `WHERE` clause of the `DELETE` statement */
    fun byPkAndOptLock(): DeleteStatement
        { this.whereDescrs.addAll(this.entityDef.pkAndOptLockCols.map { Descr(it) });  return this }

    // --- Individual column conditions

    /** Add text `colName = ?` to the statement's `WHERE` clause. `colName` is figured from the passed [prop] */
    fun by(prop: KProperty<Any?>): DeleteStatement
        { this.addProperty(this.whereDescrs, prop);  return this }

    /** Add text `colName = ?` to the statement's `WHERE` clause. [colName] must be defined in [Column] annotation. */
    fun by(colName: String): DeleteStatement
        { this.addColName (this.whereDescrs, colName);  return this } // how is it not expr?

    // --- Free form conditions

    /** Add [expr] text to the statement's `WHERE` clause. The expression may use bind variable placeholders, the values are passed in [binds] */
    fun by(expr: String, vararg binds: Any?): DeleteStatement
        { this.addColName(this.whereDescrs, null, expr, *binds);  return this }


    /** Generate and return the SQL statement text */
    public override fun build(): String {
        this.bindVals.clear()
        val sb = StringBuilder("DELETE FROM ").append(this.entityDef.tableName)

        if( this.whereDescrs.isNotEmpty() ) {
            sb.append(" WHERE ")
            sb.append(this.emitWhereList(this.whereDescrs))
            this.whereDescrs.forEach {
                if( it.expr == null ) bindVals.add(it.colDef)
                else                  bindVals.addAll(it.binds)
            }
        }

        return sb.toString()
    }

}