package com.amcentral365.pl4kotlin

import mu.KLogging
import java.sql.Connection
import java.util.ArrayList
import java.sql.SQLException
import java.sql.PreparedStatement
import java.sql.ResultSet
import kotlin.reflect.KProperty


class SelectStatement(entityDef: Entity): BaseStatement(entityDef, { null }) {
    companion object: KLogging()

    private val selectDescrs: MutableList<BaseStatement.Descr> = mutableListOf()
    private val whereDescrs:  MutableList<BaseStatement.Descr> = mutableListOf()
    private val orderDescrs:  MutableList<BaseStatement.Descr> = mutableListOf()
    private val bindVals:     MutableList<Any?> = mutableListOf()

    // use entityObj::propName to pass a reference, i.e. tx::bob
    fun select(prop: KProperty<Any>): SelectStatement {
        val colDef = this.entityDef.colDefs.firstOrNull { it.prop == prop } ?:
            throw IllegalArgumentException("SelectStatment ${this::class.qualifiedName}: property ${prop.name} isn't a @Column")
        this.selectDescrs.add(BaseStatement.Descr(colDef))
        return this
    }


    fun selectDescr(descrs: List<BaseStatement.Descr>): SelectStatement {
        this.selectDescrs.addAll(descrs)
        return this
    }

    fun byPk(): SelectStatement {
        this.entityDef.pkCols.forEach { c -> this.whereDescrs.add(BaseStatement.Descr(c)) }
        return this
    }

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
