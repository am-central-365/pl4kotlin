package com.amcentral365.pl4kotlin

import java.sql.Connection
import java.util.ArrayList


class SelectStatement(entityDef: Entity): BaseStatement(entityDef, { null }) {
    private val selectDescrs: MutableList<BaseStatement.Descr> = mutableListOf()
    private val whereDescrs:  MutableList<BaseStatement.Descr> = mutableListOf()
    private val orderDescrs:  MutableList<BaseStatement.Descr> = mutableListOf()
    private val bindVals:     MutableList<Any?> = mutableListOf()

    fun selectDescr(descrs: List<BaseStatement.Descr>): SelectStatement {
        this.selectDescrs.addAll(descrs)
        return this
    }

    fun byPk(): SelectStatement {
        this.entityDef.pkCols.forEach { c -> this.whereDescrs.add(BaseStatement.Descr(c)) }
        return this
    }

    override fun run(conn: Connection): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
