package com.amcentral365.pl4kotlin.integrationTests

import com.amcentral365.pl4kotlin.InsertStatement
import mu.KotlinLogging
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

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
        val tto1 = TestTbl()

        var count = InsertStatement(tto1, getGoodConnection = ::getConnection).run()
        assertEquals(1, count)

        assertEquals(TestTbl.KNOWN_PK1, tto1.pk1)
        assertEquals(TestTbl.KNOWN_PK2, tto1.pk2)

        assertNotEquals(TestTbl.KNOWN_UUID1, tto1.uuid1)
        assertEquals   (TestTbl.KNOWN_UUID2, tto1.uuid2)

        assertNotNull(tto1.created)
        assertNotNull(tto1.modified)
        assertEquals(tto1.created, tto1.modified)

        /*getConnection().use {
            count = InsertStatement(tto1).run(it)
        }*/


        println("testing insert statement... pretending it is ok")
    }
}
