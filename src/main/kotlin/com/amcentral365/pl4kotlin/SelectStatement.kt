package com.amcentral365.pl4kotlin

import mu.KLogging
import java.sql.Connection
import java.sql.SQLException
import java.sql.ResultSet
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.jvmName

/** Run SQL `SELECT` statement for the given [entityDef]. See [BaseStatement] constructor for parameters description. */
open class SelectStatement(entityDef: Entity, getGoodConnection: () -> Connection? = { null }): BaseStatement(entityDef, getGoodConnection) {
    private companion object: KLogging()  /** @suppress */

    /*@VisibleForTesting*/ internal val selectDescrs: MutableList<BaseStatement.Descr> = mutableListOf()
    /*@VisibleForTesting*/ internal val whereDescrs:  MutableList<BaseStatement.Descr> = mutableListOf()
    /*@VisibleForTesting*/ internal val orderDescrs:  MutableList<BaseStatement.Descr> = mutableListOf()


    // ----- Select columns and expressions

    /** Get property's column name and emit `SELECT columnName` to the SQL text. On [run], the property value is read from the database */
    fun select(prop:  KProperty<Any?>): SelectStatement
        { this.addProperty(this.selectDescrs,    prop);  return this }

    /** Emit SELECT [colName] to the SQL text. On [run], the property value is read from the database */
    fun select(colName: String): SelectStatement
        { this.addColName (this.selectDescrs, colName);  return this }

    /** Emit SELECT [expr] to the SQL text. If [expr] needs parameters, they can be passed in [binds]. The result is stored in property [targetProp] */
    fun select(targetProp: KProperty<Any?>, expr: String, vararg binds: Any?): SelectStatement
        { this.addProperty(this.selectDescrs, targetProp, expr, *binds);  return this }

    /** Emit SELECT [expr] to the SQL text. If [expr] needs parameters, they can be passed in [binds]. The result is stored in the property associated with [colName] */
    fun select(colName: String, expr: String, vararg binds: Any?): SelectStatement
        { this.addColName(this.selectDescrs, colName, expr, *binds);  return this }

    /** Allows specifying Column Groups like `Entity.allColsButPk`. See documentation for details. */
    fun select(colDefs: List<Entity.ColDef>): SelectStatement
        { this.selectDescrs.addAll(colDefs.map { Descr(it) });  return this }

    /** @suppress this form is used internally */
    internal fun selectByDescrs(descrs: List<BaseStatement.Descr>): SelectStatement
        { this.selectDescrs.addAll(descrs);  return this }


    // ----- WHERE columns or expressions

    // --- Auxiliary, frequently used conditions

    /** Add Primary Key columns to `WHERE` clause of the SELECT statement */
    fun byPk(): SelectStatement
        { this.whereDescrs.addAll(this.entityDef.pkCols.map { Descr(it) });  return this }

    /** Add Primary Key and, if defined, the Optimistic Lock column to `WHERE` clause of the `SELECT` statement */
    fun byPkAndOptLock():  SelectStatement
        { this.whereDescrs.addAll(this.entityDef.pkAndOptLockCols.map { Descr(it) });  return this }

    // --- Individual column conditions

    /** Add text `colName = ?` to the statement's `WHERE` clause. `colName` is figured from the passed [prop] */
    fun by(prop: KProperty<Any?>): SelectStatement
        { this.addProperty(this.whereDescrs, prop);  return this }

    /** Add text `colName = ?` to the statement's `WHERE` clause. [colName] must be defined in the [Column] annotation. */
    fun by(colName: String): SelectStatement
        { this.addColName (this.whereDescrs, colName);  return this } // how is it not expr?

    // --- Free form clause, allowing to specify expressions and use any column

    /** Add [expr] text to the statement's `WHERE` clause. The expression may use bind variable placeholders, the values are passed in [binds] */
    fun by(expr: String, vararg binds: Any?): SelectStatement
        { this.addColName(this.whereDescrs, null, expr, *binds);  return this }


    // ----- ORDER BY columns or expressions
    // ORDER BY property/column ASC/DESC.
    // The expression version is free form, with optional binds.

    /** Add text `colName \`[DESC`\]` to the statement's `ORDER BY` clause. `colName` is figured from the passed [prop] */
    fun orderBy(prop:    KProperty<Any?>, asc: Boolean=true): SelectStatement
        { this.addProperty(this.orderDescrs, prop, null, asc);     return this }

    /** Add text `colName = ?` to the statement's `ORDER BY` clause. [colName] must be defined in the [Column] annotation. */
    fun orderBy(colName: String, asc: Boolean=true): SelectStatement
        { this.addColName (this.orderDescrs, colName, null, asc);  return this }

    /** Add [expr] text to the statement's `ORDER BY` clause. The expression may use bind variable placeholders, the values are passed in [binds] */
    fun orderBy(expr: String, vararg binds: Any?): SelectStatement
        { this.addColName(this.orderDescrs, null, expr, *binds);  return this }


    /** Build and run `SELECT` using the provided [conn]. The connection is not closed. */
    override fun run(conn: Connection): Int {
        var rowCount = 0
        var rs: ResultSet? = null

        val bindVals: MutableList<Any?> = mutableListOf()
        this.selectDescrs.forEach { bindVals.addAll(it.binds) }
        listOf(this.whereDescrs, this.orderDescrs).forEach {
            it.forEach {
                if( it.expr == null ) bindVals.add(it.colDef)
                else                  bindVals.addAll(it.binds)
            }
        }

        val sql = this.build()
        //logger.debug { "running ${this.formatSqlWithParams(bindVals)}" }

        try {
            conn.prepareStatement(sql).use {
                stmt ->
                    this.bind(stmt, bindVals)
                    rs = stmt.executeQuery()
                    if( rs!!.next() ) {
                        this.selectDescrs.forEachIndexed { k, v -> v.colDef!!.read(rs!!, k+1)}
                        rowCount++
                    }
            }
        } catch(e: SQLException) {
            logger.error {
                "SelectStatement on ${this.entityDef::class.jvmName}: ${e::class.jvmName} ${e.message}; " +
                "SQL: ${this.formatSqlWithParams(bindVals)}"
            }
            throw e
        } finally {
            closeIfCan(rs)
        }

        return rowCount
    }

    /** Build text of the `SELECT` statement and return it. */
    public override fun build(): String {
        require( this.selectDescrs.isNotEmpty() )

        val sb = StringBuilder("SELECT ")
        sb.append(this.emitList(this.selectDescrs, ", ") { descr -> descr.colDef!!.columnName })

        sb.append(" FROM ").append(this.entityDef.tableName)

        if( this.whereDescrs.isNotEmpty() ) {
            sb.append(" WHERE ")
            sb.append(this.emitWhereList(this.whereDescrs))
        }

        if( orderDescrs.isNotEmpty() ) {
            sb.append(" ORDER BY ")
            sb.append(emitOrderByList(this.orderDescrs))
        }

        return sb.toString()
    }

}
