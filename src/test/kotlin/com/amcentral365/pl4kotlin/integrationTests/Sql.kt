package com.amcentral365.pl4kotlin.integrationTests

import mu.KotlinLogging
import java.io.FileInputStream
import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties

private val logger = KotlinLogging.logger {}
internal var connInfo: ConnectionInfo? = null

private val dbVendorExtractor = Regex("^jdbc:([^:/@]+)")

internal data class ConnectionInfo(val jdbcUrl: String, private val username: String, private val password: String) {
    val connProps: Properties = Properties()
    val dbVendor: String = dbVendorExtractor.find(jdbcUrl)?.groups?.get(1)?.value ?:
                                throw IllegalArgumentException("couldn't extract vendor from jdbc url '$jdbcUrl'")

    init {
        this.connProps.setProperty("zeroDateTimeBehavior", "convertToNull")  // zeroDateTimeBehavior=convertToNull
        this.connProps.setProperty("user",     username)
        this.connProps.setProperty("password", password)
    }
}


internal fun initSql() {
    val dbConfigFileName = System.getProperty("dbConfig", "mysql.properties")
    logger.info { "using config file $dbConfigFileName" }

    val cfg = Properties().apply { FileInputStream(dbConfigFileName).use { strm -> load(strm) } }
    logger.debug { "read config: $cfg" }

    val jdbcUrl  = cfg.getProperty("jdbcUrl")
    logger.info { "  jdbcUrl: $jdbcUrl" }

    val username = cfg.getProperty("username")
    logger.info { "  username: $username" }

    val password = cfg.getProperty("password")
    logger.info { "  password: $password" }  // there is no point hiding the password: it isn't secret

    connInfo = ConnectionInfo(jdbcUrl, username, password)
}


internal fun getConnection(): Connection {
    require( connInfo != null )
    val conn = DriverManager.getConnection(connInfo!!.jdbcUrl, connInfo!!.connProps)
    conn.autoCommit = false
    conn.transactionIsolation = /* SQLite supports only TRANSACTION_SERIALIZABLE and TRANSACTION_READ_UNCOMMITTED */
            if( connInfo!!.dbVendor == "sqlite" ) Connection.TRANSACTION_SERIALIZABLE
            else                                  Connection.TRANSACTION_READ_COMMITTED
    logger.debug { "database connection obtained" }
    return conn
}

