package com.amcentral365.pl4kotlin

import com.google.common.annotations.VisibleForTesting
import mu.KLogging
import java.sql.Connection
import java.sql.SQLException
import java.sql.ResultSet
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.jvmName


open class SelectStatement(entityDef: Entity, getGoodConnection: () -> Connection? = { null }): BaseStatement(entityDef, getGoodConnection) {
    companion object: KLogging()

    @VisibleForTesting internal val selectDescrs: MutableList<BaseStatement.Descr> = mutableListOf()
    @VisibleForTesting internal val whereDescrs:  MutableList<BaseStatement.Descr> = mutableListOf()
    @VisibleForTesting internal val orderDescrs:  MutableList<BaseStatement.Descr> = mutableListOf()


    // ----- Select columns and expressions

    // We can select by the property (recommended, as it checked at compile time and makes refactoring easier)
    // or by database column name. In both cases the appropriate ColDef is looked for and used.
    //
    // The property form may be extended with expression and optional bind list. It translates to 'select expr'
    // and the result of the exression is fetched into the property.
    // Technically we could have the same method for column name, but it doesn't make sense logically
    // (select expr into a column?), and therefore this form was deliberately omitted.
    fun select(prop: KProperty<Any>): SelectStatement
        { this.addProperty(this.selectDescrs, prop);  return this }
    fun select(targetProp: KProperty<Any>, expr: String, vararg binds: Any?): SelectStatement
        { this.addProperty(this.selectDescrs, targetProp, expr, *binds);  return this }

    fun select(colName: String): SelectStatement
        { this.addColName(this.selectDescrs, colName);  return this }

    // this form is used internally
    fun select(descrs: List<BaseStatement.Descr>): SelectStatement { this.selectDescrs.addAll(descrs);  return this }


    // ----- WHERE columns or expressions
    // Beside frequently used PK/OptLock expressions, there are two other forms:
    // 1) property or column: translates to "WHERE column = ?", and the bind value comes from the property
    // 2) a free-form expressions with bind values. Bind values provided are computed and saved.

    // auxiliary, frequently used forms
    fun byPk():            SelectStatement { this.whereDescrs.addAll(this.entityDef.pkCols.          map { Descr(it) });  return this }
    fun byPkAndOptLock():  SelectStatement { this.whereDescrs.addAll(this.entityDef.pkAndOptLockCols.map { Descr(it) });  return this }

    // when prop or column name is used, its ColDef is detected, and the resulting statement translates to
    // "WHERE colName = ?". The property value is bound when the statement is ran.
    fun by(prop: KProperty<Any>): SelectStatement { this.addProperty(this.whereDescrs, prop);     return this }
    fun by(colName: String):      SelectStatement { this.addColName (this.whereDescrs, colName);  return this } // how is it not expr?

    // free form clause, allowing to specify expressions and use any column
    fun by(expr: String, vararg binds: Any?): SelectStatement { this.addColName(this.whereDescrs, null, expr, *binds);  return this }


    // ----- ORDER BY columns or expressions
    // ORDER BY property/column ASC/DESC
    // The expression version is free form, with optional binds.

    fun orderBy(prop: KProperty<Any>, asc: Boolean=true):  SelectStatement { this.addProperty(this.orderDescrs, prop, null, asc);     return this }
    fun orderBy(colName: String, asc: Boolean=true):       SelectStatement { this.addColName(this.orderDescrs, colName, null, asc);   return this }

    fun orderBy(expr: String, vararg binds: Any?): SelectStatement { this.addColName(this.orderDescrs, null, expr, *binds);  return this }


    override fun run(conn: Connection): Int {
        var rowCount = 0
        var rs: ResultSet? = null

        val bindVals: MutableList<Any?> = mutableListOf()
        listOf(this.selectDescrs, this.whereDescrs, this.orderDescrs).forEach { it.forEach { bindVals.addAll(it.binds) } }

        try {
            conn.prepareStatement(this.sql).use {
                stmt ->
                    this.bind(stmt, bindVals)
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
