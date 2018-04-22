package com.amcentral365.pl4kotlin.integrationTests

import com.amcentral365.pl4kotlin.InsertStatement
import com.amcentral365.pl4kotlin.JdbcTypeCode
import com.amcentral365.pl4kotlin.SelectStatement
import mu.KotlinLogging
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import java.math.BigDecimal
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

            val numOfRecordsToTest = 11

            testInsertStatement()
            insertRecords(numOfRecordsToTest)

            testSelectStatement(numOfRecordsToTest)


        } finally {
            if( teardown )
                runSqlTeardown()
        }
    }

    private fun testInsertStatement() {
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
        assertEquals(TestTbl.KNOWN_TIME, tto1.timeVal)
        assertEquals(TestTbl.KNOWN_NUM, tto1.numVal)
        assertEquals(TestTbl.KNOWN_FLOAT, tto1.floatVal)
        assertEquals(TestTbl.KNOWN_DOUBLE, tto1.doubleVal)
        assertEquals(TestTbl.KNOWN_BITS, tto1.bit17Val)
        assertTrue(tto1.boolVal!!)
        assertEquals(TestTbl.GreekLetters.epsilon, tto1.enumVal)
        assertNull(tto1.nullVal)

        val tto2 = this.jdbcreadTestTblRec(tto1.pk1, tto1.pk2!!)
        assertNotNull(tto2)
        ensureEq(tto1, tto2!!)
    }

    private fun ensureEq(tto1: TestTbl, tto2: TestTbl) {
        assertEquals(tto1.pk1,       tto2.pk1)
        assertEquals(tto1.pk2,       tto2.pk2)
        assertEquals(tto1.uuid1,     tto2.uuid1)
        assertEquals(tto1.uuid2,     tto2.uuid2)
        assertEquals(tto1.vcVal,     tto2.vcVal)
        assertEquals(tto1.dateVal,   tto2.dateVal)
        assertEquals(tto1.timeVal,   tto2.timeVal)
        assertEquals(tto1.floatVal,  tto2.floatVal)
        assertEquals(tto1.doubleVal, tto2.doubleVal)
        assertEquals(tto1.bit17Val,  tto2.bit17Val)
        assertEquals(tto1.boolVal,   tto2.boolVal)
        assertEquals(tto1.enumVal,   tto2.enumVal)
        assertEquals(tto1.nullVal,   tto2.nullVal)
        assertEquals(tto1.created,   tto2.created)
        assertEquals(tto1.modified,  tto2.modified)
        // MySQL right-trims CHAR unless PAD_CHAR_TO_FULL_LENGTH is enabled
        assertEquals(tto1.charVal!!.trimEnd(), tto2.charVal)

        // BigDecimal should use their own cmoparator
        assertTrue(tto1.numVal!!.compareTo(tto2.numVal) == 0)
    }

    private fun tweakForK(r: TestTbl, k: Int): TestTbl = r.apply {
        pk2 = k.toShort()
        vcVal = "record $k"
        uuid2 = null  // will be generated
        dateVal!!.time = dateVal!!.time + k * 1000 * 60 * 60 * 24  // add k days
        timeVal!!.time = timeVal!!.time + k * 1000 * 60            // add k minutes
        numVal = numVal!!.add(BigDecimal(k))
        floatVal = floatVal!! - k
        doubleVal = doubleVal!! - k
        bit17Val = bit17Val!! or (1L shl (k % 17))
        boolVal = k % 2 == 0
        enumVal = TestTbl.GreekLetters.values()[k % 5]  // round robin alpha to epsilon
    }


    private fun insertRecords(count: Int) {
        for(k in 1..count) {
            val r = this.tweakForK(TestTbl(), k)
            val insertCount = InsertStatement(r, getGoodConnection = ::getConnection).run()
            assertEquals(1, insertCount)
        }
    }


    private fun testSelectStatement(numOfRecordsToTest: Int) {
        logger.info { "running testSelectStatement($numOfRecordsToTest)" }
        for(k in numOfRecordsToTest downTo 1) {
            val tto1 = TestTbl(k)
            logger.debug { "  $k: running testSelectStatement for pk ${tto1.pk1}, ${tto1.pk2}" }
            val selCnt = SelectStatement(tto1, getGoodConnection = ::getConnection).select(tto1.allColsButPk!!).byPk().run()
            assertEquals(1, selCnt)

            val tto2 = this.jdbcreadTestTblRec(tto1.pk1, k.toShort())
            assertNotNull(tto2)

            ensureEq(tto1, tto2!!)
        }
    }



    private fun jdbcreadTestTblRec(ppk1: Int, ppk2: Short): TestTbl? {
        val sql = "select bit17_val, bool_col, char_col, created_ts, date_col, double_col, " +
                         "enum_col, float_col, modified_ts, num_col, time_col, uuid1, " +
                         "uuid2, vc_col, null_col " +
                    "from test_tbl " +
                   "where pk1 = ? and pk2 = ?"

        // do not catch exceptions, let test fail if one is thrown
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(  1, ppk1)
                stmt.setShort(2, ppk2)
                stmt.executeQuery().use { rs ->
                    if( !rs.next() )
                        return null

                    return TestTbl().apply {
                        var n = 0
                        pk1       = ppk1
                        pk2       = ppk2
                        bit17Val  = rs.getLong(++n)
                        boolVal   = rs.getBoolean(++n)
                        charVal   = rs.getString(++n)
                        created   = rs.getTimestamp(++n)
                        dateVal   = rs.getDate(++n)
                        doubleVal = rs.getDouble(++n)
                        enumVal   = TestTbl.GreekLetters.valueOf(rs.getString(++n))
                        floatVal  = rs.getFloat(++n)
                        modified  = rs.getTimestamp(++n)
                        numVal    = rs.getBigDecimal(++n)
                        timeVal   = rs.getTime(++n)
                        uuid1     = JdbcTypeCode.uuidFromBytes(rs.getBytes(++n))
                        uuid2     = JdbcTypeCode.uuidFromBytes(rs.getBytes(++n))
                        vcVal     = rs.getString(++n)
                        nullVal   = rs.getString(++n)
                    }
               }
            }
        }
    }

}
