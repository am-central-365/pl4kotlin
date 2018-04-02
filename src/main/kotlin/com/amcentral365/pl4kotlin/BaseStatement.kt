package com.amcentral365.pl4kotlin

import mu.KLogging
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException


abstract class BaseStatement(val entityDef: Entity, val getGoodConnection: () -> Connection?) {
    companion object: KLogging()

    abstract fun build(): String
    abstract fun run(conn: Connection): Int

    // ----------------------------------------------------------------------------------------------

    data class Descr(val colDef: Entity.ColDef?, val expr: String?, val binds: List<Any>?) {
        init {
            require( this.colDef != null || this.expr != null )
            require( expr != null || binds == null )  // when expr is null, binds must also be null
        }

        constructor(colDef: Entity.ColDef): this(colDef, null, null)
    }


    private val sql: String by lazy { this.build() }
    private var manageTx = false

    /**
     * Establish a database connection and invoke run(conn). Close the connection after running.
     * The function is just like connectAndRun, but executes fixed "run(Connection)" defined by the derived class.
     */
    fun run(): Any = this.connectAndRun<Any>(this::run)

    /**
     * Establish a database connection and ivoke the provided action. Close the connectino on exit.
     */
    private fun <R> connectAndRun(action: (conn:Connection) -> R): R {
        val conn: Connection = this.getGoodConnection() ?: throw SQLException("${this::class.qualifiedName} ${this.entityDef.tableName}: to connect to the database")

        this.manageTx = true
        try {
            return action(conn)
        } catch(x: Exception) {
            try {
                conn.rollback()
            } catch (e: SQLException) {
                logger.warn("${this::class.qualifiedName} ${this.entityDef.tableName}: could not rollback failed operation: ${e.message}")
            }
            throw x
        } finally {
            this.manageTx = false
            try {
                conn.close()
            } catch (e: SQLException) {
                logger.warn("${this::class.qualifiedName} ${this.entityDef.tableName}: could not close connection: ${e.message}")
            }
        }
    }

    fun runDML(conn: Connection, bindVals: List<Any>, fetchBacks: List<Descr>?): Int {
        var cnt: Int = 0
        try {
            conn.prepareStatement(this.sql).use { stmt ->
                this.bind(stmt, bindVals)
                cnt = stmt.executeUpdate()

                if( cnt > 0 && fetchBacks != null && fetchBacks.isNotEmpty()  )
                    cnt = SelectStatement(this.entityDef).selectDescr(fetchBacks).byPk().run(conn)

                if( this.manageTx )
                    conn.commit()
            }
        } catch(x: SQLException) {
            TODO("replace code '23000' (duplicate entry) of x.getSQLState(), and throw proper exception")
        }

        return cnt
    }

    fun bind(stmt: PreparedStatement, idx: Int, value: Any?) {
        val jtc = JdbcTypeCode.from(value?.javaClass)
        JdbcTypeCode.getBinder(jtc).bind(stmt, idx, value)
    }

    fun bind(stmt: PreparedStatement, vals: List<Any>) = vals.forEachIndexed { k, v -> this.bind(stmt, k+1, v) }

    /**
     * Add comma-separated list of unfolded descriptors to sb and bindVals.
     *
     * When a descriptor defines an expression - emit it, otherwise column name to the sb.
     * In case of expresion, also appends all the bind values, if any, to bindVals.
     * @param descrs: list of element descriptors: either column name, or an expression
     * @param sb: the generated text is appended to the db
     * @param bindVals: global list of statement's bind variables, where descriptor's binds are appended if emitted
     */
    protected fun emitSimlpeList(descrs: List<Descr>, sb: StringBuilder, bindVals: MutableList<Any?>) {
        var sep = ""
        for( (colDef, expr, binds) in descrs ) {
            sb.append(sep)
            sep = ", "
            if( expr == null )
                sb.append(colDef!!.columnName)
            else {
                sb.append(expr)
                if( binds != null )
                    bindVals.addAll(binds)
            }
        }
    }

    /**
     * Add separated list of unfolded descriptors to sb and bindVals.
     * Mostly used to build "where" clauses, with AND separator.
     *
     * When a descriptor defines an expression - emit it, otherwise column name to the sb.
     * In case of expresion, also appends all the bind values, if any, to bindVals.
     * @param descrs: list of element descriptors: either column name, or an expression
     * @param sb: the generated text is appended to the db
     * @param bindVals: global list of statement's bind variables, where descriptor's binds are appended if emitted
     */
    protected fun emitEqList(descrs: List<Descr>, sb: StringBuilder, bindVals: MutableList<Any?>, separator: String) {
        var sep = ""
        for( (colDef, expr, binds) in descrs ) {
            sb.append(sep)
            sep = separator
            if( expr == null ) {
                sb.append(colDef!!.columnName).append(" = ?")
                bindVals.add(colDef.getValue())
            } else {
                if( colDef != null )
                    sb.append(colDef.columnName).append(" = ")
                sb.append(expr)
                if( binds != null )
                    bindVals.addAll(binds)
            }
        }
    }

}