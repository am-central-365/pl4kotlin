package com.amcentral365.pl4kotlin

import mu.KLogging
import java.sql.Connection
import java.util.ArrayList
import java.sql.SQLException
import java.sql.PreparedStatement
import java.sql.ResultSet
import kotlin.reflect.KProperty


open class SelectStatement(entityDef: Entity, getGoodConnection: () -> Connection? = { null }): BaseStatement(entityDef, getGoodConnection) {
    companion object: KLogging()

    private val selectDescrs: MutableList<BaseStatement.Descr> = mutableListOf()
    private val whereDescrs:  MutableList<BaseStatement.Descr> = mutableListOf()
    private val orderDescrs:  MutableList<BaseStatement.Descr> = mutableListOf()
    private val bindVals:     MutableList<Any?> = mutableListOf()

    // ----- Selected columns or expressions
    fun select(prop: KProperty<Any>):              SelectStatement { this.addProperty(this.selectDescrs, prop);              return this }
    fun select(descrs: List<BaseStatement.Descr>): SelectStatement { this.selectDescrs.addAll(descrs);                       return this }
    fun select(expr: String, vararg binds: Any):   SelectStatement { this.addColName(this.selectDescrs, null, expr, binds);  return this }
    fun selectCol(colName: String):                SelectStatement { this.addColName(this.selectDescrs, colName);            return this }

    // ----- WHERE columns or expressions
    fun byPk():            SelectStatement { this.whereDescrs.addAll(this.entityDef.pkCols.          map { Descr(it) });  return this }
    fun byPkAndOptLock():  SelectStatement { this.whereDescrs.addAll(this.entityDef.pkAndOptLockCols.map { Descr(it) });  return this }
    fun byPresentValues(): SelectStatement { this.whereDescrs.addAll(this.entityDef.pkAndOptLockCols.map { Descr(it) });  return this }
    fun by(prop: KProperty<Any>): SelectStatement { this.addProperty(this.whereDescrs, prop);  return this }

    // ----- ORDER BY columns or expressions
    fun orderBy(prop: KProperty<Any>):                SelectStatement { this.addProperty(this.orderDescrs, prop);               return this }
    fun orderByExpr(expr: String, vararg binds: Any): SelectStatement { this.addColName (this.orderDescrs, null, expr, binds);  return this }
    fun orderByCol(colName: String):                  SelectStatement { this.addColName (this.orderDescrs, colName);            return this }

    override fun run(conn: Connection): Int {
        var rowCount = 0
        var rs: ResultSet? = null
        try {
            conn.prepareStatement(this.sql).use {
                stmt ->
                    this.bind(stmt, this.bindVals)
                    rs = stmt.executeQuery()
                    if( rs!!.first() ) {
                        this.selectDescrs.forEachIndexed() { k, v -> v.colDef!!.read(rs!!, k+1)}
                        rowCount++
                    }
            }
        } catch (e: SQLException) {
            logger.error {
                "SelectStatement on ${this.entityDef::class.qualifiedName}: ${e::class.qualifiedName} ${e.message}; " +
                "SQL: ${this.formatSqlWithParams(bindVals)}"
            }
            throw e
        } finally {
            closeIfCan(rs)
        }

        return rowCount
    }

    override fun build(): String {
        require( this.selectDescrs.isNotEmpty() )
        this.bindVals.clear()

        val sb = StringBuilder("SELECT ")
        this.emitSimlpeList(this.selectDescrs, sb, this.bindVals)

        sb.append(" FROM ").append(this.entityDef.tableName)

        if( this.whereDescrs.isNotEmpty() ) {
            sb.append(" WHERE ")
            this.emitEqList(this.whereDescrs, sb, this.bindVals, " AND ")
        }

        if( orderDescrs.isNotEmpty() ) {
            sb.append(" ORDER BY ")
            emitSimlpeList(this.orderDescrs, sb, this.bindVals)
        }

        return sb.toString()
    }

}
