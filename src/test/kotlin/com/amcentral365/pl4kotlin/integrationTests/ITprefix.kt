package com.amcentral365.pl4kotlin.integrationTests

import org.junit.Test
import java.io.FileInputStream
import java.util.*

class ITprefix {

    @Test
    fun `with IT prefix`() {
        println("Integration test with IT prefix")

        val dbConfigFileName = System.getProperty("dbConfig", "mysql.properties")

        println("Reading config $dbConfigFileName")
        val cfg = Properties().apply { FileInputStream(dbConfigFileName).use { strm -> load(strm) } }

        println("url: ${cfg.getProperty("jdbcUrl")}")
        println("usr: ${cfg.getProperty("username")}")
        println("pwd: ${cfg.getProperty("password")}")
    }
}
