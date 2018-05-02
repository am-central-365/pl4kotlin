package com.amcentral365.pl4kotlin

import mu.KLogging
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException
import com.amcentral365.pl4kotlin.Entity.ColDef
import java.util.Arrays
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.jvmName

/**
 * Provides DML statement executor and a number of helper functions. It is an abstract class requiring function
 * `build()` called to generate the SQL statement. Derived classes Select/Insert/Update/DeleteStatement override
 * the method.
 *
 * @constructor Takes [entityDef] instance of a class mapping POJOs to a database table. Such classes
 *   must be derived from [Entity]. The other argument, [getGoodConnection], is a user-provided function
 *   called to get a valid database connection. The function is used when [Connection] object was not
 *   supplied to [run] method.
 *
 * @property entityDef The object whose values are used to modify or read the database table.
 * @property getGoodConnection A user-provided function used to obtain connections. If you
 *   always provide the [Connection] object to [run] explicitly, there is no need to pass the argument.
 */
abstract class BaseStatement(val entityDef: Entity, private val getGoodConnection: () -> Connection?) {
    companion object: KLogging()

    /**
     * Creates SQL statement text. The method is specific to the statement and must be overridden.
     * The method is protected because some statements only build SQL text when they run:
     * `INSERT` statement with properties marked Generated.OnXwhenNull check the properties value
     *  while building the statement. Separating [build] and [run] could lead to inconsistency as
     *  the values may change between the calls.
     */
    protected abstract fun build(): String

    /**
     * Build (if needed) and execute the statement, using the [conn] connection. Most derived classes
     * implement the function by calling [runDML].
     *
     * @return the number of affected rows.
     */
    abstract fun run(conn: Connection): Int

    // ----------------------------------------------------------------------------------------------

    data class Descr(val colDef: Entity.ColDef?, val expr: String?, val asc: Boolean?, val binds: List<Any?>) {
        init {
            require( this.colDef != null || this.expr != null )
            require( expr != null || binds.isEmpty() )  // when expr is null, binds must also be null
        }

        constructor(colDef: Entity.ColDef): this(colDef, null, true, emptyList())
    }


    private   var manageTx = false

    /**
     * Establish a database connection by calling [getGoodConnection] and invoke [run] version with the
     * [Connection] parameter. The obtained connection is committed or rolled back depending on the outcome,
     * of the operation, and then closed.
     */
    fun run(): Any = this.connectAndRun<Any>(this::run)

    /**
     * Establish a database connection and invoke the provided action. Close the connection on exit.
     */
    private fun <R> connectAndRun(action: (conn:Connection) -> R): R {
        val conn: Connection = this.getGoodConnection() ?: throw SQLException("${this::class.jvmName} ${this.entityDef.tableName}: couldn't connect to the database")

        this.manageTx = true
        try {
            return action(conn)
        } catch(x: Exception) {
            try {
                conn.rollback()
            } catch (e: SQLException) {
                logger.warn("${this::class.jvmName} ${this.entityDef.tableName}: could not rollback failed operation: ${e.message}")
            }
            throw x
        } finally {
            this.manageTx = false
            try {
                conn.close()
            } catch (e: SQLException) {
                logger.warn("${this::class.jvmName} ${this.entityDef.tableName}: could not close connection: ${e.message}")
            }
        }
    }

    protected fun runDML(conn: Connection, bindVals: List<Any?>, fetchBacks: List<Descr>?): Int {
        var cnt = 0
        var reportSqlOnError = true
        try {
            val sql = this.build()
            conn.prepareStatement(sql).use { stmt ->
                this.bind(stmt, bindVals)
                cnt = stmt.executeUpdate()

                // if any values need to be fetched back (because SQL engine computed them), do that.
                if( cnt > 0 && fetchBacks != null && fetchBacks.isNotEmpty()  ) {
                    reportSqlOnError = false
                    // TODO: UpdateStatement may update multiple rows. Shall we use byPK (which may not
                    // TODO: even make sense), or use the original statement's whereDescrs ?
                    cnt = SelectStatement(this.entityDef).selectByDescrs(fetchBacks).byPk().run(conn)
                }

                if( this.manageTx )
                    conn.commit()
            }
        } catch(x: SQLException) {
            //TODO("replace code '23000' (duplicate entry) of x.getSQLState(), and throw proper exception")
            if( reportSqlOnError )
                logger.error {
                    "SelectStatement on ${this.entityDef::class.jvmName}: ${x::class.jvmName} ${x.message}; " +
                    "SQL: ${this.formatSqlWithParams(bindVals)}"
                }
            throw x
        }

        return cnt
    }

    private val isInserting: Boolean by lazy { this is InsertStatement }

    fun bind(stmt: PreparedStatement, idx: Int, value: Any?) {
        var bindVal = value
        if( value is ColDef )
            bindVal = value.getBindValue(isInserting)
        val jtc = JdbcTypeCode.from(bindVal?.javaClass)
        JdbcTypeCode.getBinder(jtc).bind(stmt, idx, bindVal)
    }

    // FIXME: we merge all bind lists into an uber list, and calling this function with the list. This is not efficient.
    // TODO:  take "last used index" parameter, add it to k, return last used index too. Call the function for each list.
    protected open fun bind(stmt: PreparedStatement, vals: List<Any?>) = vals.forEachIndexed { k, v -> this.bind(stmt, k+1, v) }

    // ----- emitters: help to build various lists
    protected fun emitList(descrs: List<Descr>, sep: String, colDefEmitter: (descr: Descr) -> String): String =
        descrs.joinToString(sep) { it.expr ?: colDefEmitter(it) }


    protected fun emitWhereList(whereDescrs: List<Descr>): String =
        this.emitList(whereDescrs, " AND ") { descr -> "${descr.colDef!!.columnName} = ?" }

    protected fun emitOrderByList(orderByDescrs: List<Descr>): String =
        this.emitList(orderByDescrs, ", ") {
            descr -> descr.colDef!!.columnName + if( descr.asc == null || descr.asc ) "" else " DESC"
        }


    /**
     * Formats SQL for printing in log messages. Values longer than DISP_MAX characters are truncated
     * @param bindVals placeholder values
     * @return The effective SQL statement with placeholders replaced with the actual (possibly truncated) values
     */
    protected fun formatSqlWithParams(bindVals: List<Any?>): String {
        val dispMax = 64 // should be enough for UUIDs (32), timestamps (19), ROWIDs (varies), etc
        var dbgSql = this.build()
        for(value in bindVals) {
            var dispVal = "null"
            if( value != null ) {
                val ps = (value as? ColDef)?.getValue()?.toString() ?: value.toString()
                dispVal = if (ps.length <= dispMax) ps else ps.substring(0, dispMax - 3) + "..."
            }
            dbgSql = dbgSql.replaceFirst("\\?", "'$dispVal'")
        }
        return dbgSql
    }


    private fun getColDefOrDie(predicate: (knownColDef: ColDef) -> Boolean, errmsg: String): ColDef =
        this.entityDef.colDefs.firstOrNull(predicate)
                ?: throw IllegalArgumentException("${this.entityDef::class.jvmName}(${this.entityDef.tableName}): $errmsg")


    protected fun addColName(list: MutableList<Descr>, colName: String?, expr: String? = null, asc: Boolean, vararg binds: Any?) {
        val colDef = if (colName == null) null else this.getColDefOrDie({ c -> c.columnName == colName }, "unknown @Column with colName '$colName'")
        list.add(Descr(colDef, expr, asc, Arrays.asList(*binds)))
    }

    protected fun addProperty(list: MutableList<Descr>, prop: KProperty<Any?>, expr: String? = null, asc: Boolean, vararg binds: Any?) {
        val colDef = this.getColDefOrDie({ it.prop.name == prop.name }, "property ${prop.name} doesn't have attribute @Column")
        list.add(Descr(colDef, expr, asc, Arrays.asList(*binds)))
    }

    // asc - less versions, defaulting asc to true
    protected fun addColName(list: MutableList<Descr>, colName: String?, expr: String? = null, vararg binds: Any?) =
            this.addColName(list, colName, expr, true, *binds)
    protected fun addProperty(list: MutableList<Descr>, prop: KProperty<Any?>, expr: String? = null, vararg binds: Any?) =
            this.addProperty(list, prop, expr, true, *binds)

}