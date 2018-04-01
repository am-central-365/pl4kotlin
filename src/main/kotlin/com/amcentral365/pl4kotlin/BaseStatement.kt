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


    fun bind(stmt: PreparedStatement, vals: List<Any>) =
        vals.forEachIndexed { k, v -> this.entityDef.colDefs[k].bind(stmt, k+1, v) }

}