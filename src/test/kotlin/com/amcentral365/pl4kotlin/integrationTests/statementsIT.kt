package com.amcentral365.pl4kotlin.integrationTests

import com.amcentral365.pl4kotlin.InsertStatement
import mu.KotlinLogging
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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

        assertEquals(TestTbl.KNOWN_VC, tto1.vcVal)
        assertEquals(TestTbl.KNOWN_CHAR, tto1.charVal)
        assertEquals(TestTbl.KNOWN_DATE, tto1.dateVal)
        assertEquals(TestTbl.KNOWN_TIME, tto1.timweVal)
        assertEquals(TestTbl.KNOWN_NUM, tto1.numVal)
        assertEquals(TestTbl.KNOWN_FLOAT, tto1.floatVal)
        assertEquals(TestTbl.KNOWN_DOUBLE, tto1.doubleVal)
        assertEquals(TestTbl.KNOWN_BITS, tto1.bit17Val)
        assertNull(tto1.boolVal)
        assertNull(tto1.enumVal)

        /*getConnection().use {
            count = InsertStatement(tto1).run(it)
        }*/


        println("testing insert statement... pretending it is ok")
    }
}
