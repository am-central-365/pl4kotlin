package com.amcentral365.pl4kotlin

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.logging.Logger

abstract internal class BaseStatement(val entityDef: Entity, val getGoodConnection: () -> Connection?) {

    abstract fun build(): String
    abstract fun run(conn: Connection): Int

    // ----------------------------------------------------------------------------------------------

    protected data class Descr(val colDef: ColDef?, val expr: String?, val binds: List<Any>?) {
        init {
            require( this.colDef != null || this.expr != null )
            require( expr != null || binds == null )  // when expr is null, binds must also be null
        }

        constructor(colDef: ColDef): this(colDef, null, null)
    }


    val sql: String by lazy { this.build() }
    var manageTx = false

    /**
     * Establish a database connection and invoke run(conn). Close the connection after running.
     * The function is just like connectAndRun, but executes fixed "run(Connection)" defined by the derived class.
     */
    fun run(): Any = this.connectAndRun<Any>(this::run)

    /**
     * Establish a database connection and ivoke the provided action. Close the connectino on exit.
     */
    fun <R> connectAndRun(action: (conn:Connection) -> R): R {
        val conn: Connection? = this.getGoodConnection()
        if( conn == null )
            throw SQLException("${this::class.qualifiedName} ${this.entityDef.tableName}: to connect to the database")

        this.manageTx = true
        try {
            return action(conn)
        } catch(x: Exception) {
            try {
                conn.rollback()
            } catch (e: SQLException) {
                // TODO: log warning
                "${this::class.qualifiedName} ${this.entityDef.tableName}: could not reollback failed operation: ${e.message}"
            }
            throw x
        } finally {
            conn.close()
        }
    }

}