package com.amcentral365.pl4kotlin.integrationTests

import mu.KotlinLogging
import org.junit.Test
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach

private val logger = KotlinLogging.logger {}

class statementsIT {

    @Test
    fun allStatements() {

        logger.info { "Starting statement tests" }
        var teardown = false
        try {
            initSql()
            runSqlSetup()
            teardown = true

            testInsertStatement()

        } finally {
            if( teardown )
                runSqlTeardown()
        }
    }

    fun testInsertStatement() {
        println("testing insert statement... pretending it is ok")
    }
}
