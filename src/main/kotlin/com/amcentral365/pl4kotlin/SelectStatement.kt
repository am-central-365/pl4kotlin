package com.amcentral365.pl4kotlin

import mu.KLogging
import java.sql.Connection
import java.sql.SQLException
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.*
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.jvmName


open class SelectStatement(entityDef: Entity, getGoodConnection: () -> Connection? = { null }): BaseStatement(entityDef, getGoodConnection) {
    companion object: KLogging()

    private val selectDescrs: MutableList<BaseStatement.Descr> = mutableListOf()
    private val whereDescrs:  MutableList<BaseStatement.Descr> = mutableListOf()
    private val orderDescrs:  MutableList<BaseStatement.Descr> = mutableListOf()
    private val bindVals:     MutableList<Any?> = mutableListOf()

    // ----- Selected columns or expressions
    fun select(vararg props: KProperty<Any>):      SelectStatement { this.addProperties(this.selectDescrs, Arrays.asList(*props));  return this }
    fun select(descrs: List<BaseStatement.Descr>): SelectStatement { this.selectDescrs.addAll(descrs);                              return this }
    fun select(expr: String, vararg binds: Any):   SelectStatement { this.addColName(this.selectDescrs, null, expr, binds);         return this }
    fun selectCol(colName: String):                SelectStatement { this.addColName(this.selectDescrs, colName);                   return this }

    // ----- WHERE columns or expressions
    fun byPk():            SelectStatement { this.whereDescrs.addAll(this.entityDef.pkCols.          map { Descr(it) });     return this }
    fun byPkAndOptLock():  SelectStatement { this.whereDescrs.addAll(this.entityDef.pkAndOptLockCols.map { Descr(it) });     return this }
    fun by(expr: String, vararg binds: Any): SelectStatement { this.addColName(this.whereDescrs, null, expr, binds);         return this }
    fun by(vararg props: KProperty<Any>):    SelectStatement { this.addProperties(this.whereDescrs, Arrays.asList(*props));  return this }

    // ----- ORDER BY columns or expressions
    fun orderBy(vararg props: KProperty<Any>):        SelectStatement { this.addProperties(this.orderDescrs, Arrays.asList(*props));   return this }
    fun orderBy(vararg colNames: String):             SelectStatement { this.addColNames(this.orderDescrs, Arrays.asList(*colNames));  return this }
    fun orderByExpr(expr: String, vararg binds: Any): SelectStatement { this.addColName (this.orderDescrs, null, expr, binds);         return this }

    override fun run(conn: Connection): Int {
        var rowCount = 0
        var rs: ResultSet? = null
        try {
            conn.prepareStatement(this.sql).use {
                stmt ->
                    this.bind(stmt, this.bindVals)
                    rs = stmt.executeQuery()
                    if( rs!!.first() ) {
                        this.selectDescrs.forEachIndexed { k, v -> v.colDef!!.read(rs!!, k+1)}
                        rowCount++
                    }
            }
        } catch (e: SQLException) {
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
