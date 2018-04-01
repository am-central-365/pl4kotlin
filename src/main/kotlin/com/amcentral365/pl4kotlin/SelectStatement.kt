package com.amcentral365.pl4kotlin

import java.sql.Connection
import java.util.ArrayList
import jdk.nashorn.internal.objects.NativeArray.forEach




class SelectStatement(entityDef: Entity): BaseStatement(entityDef, { null }) {
    private val sdescr = ArrayList<BaseStatement.Descr>()
    private val wdescr = ArrayList<BaseStatement.Descr>()
    private val odescr = ArrayList<BaseStatement.Descr>()
    private val bindVals = ArrayList<Any>()

    fun selectDescr(descrs: List<BaseStatement.Descr>): SelectStatement {
        this.sdescr.addAll(descrs)
        return this
    }

    fun byPk(): SelectStatement {
        this.entityDef.pkCols?.forEach { c -> this.wdescr.add(BaseStatement.Descr(c)) }
        return this
    }

    override fun run(conn: Connection): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun build(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
