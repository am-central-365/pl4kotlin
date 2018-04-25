package com.amcentral365.pl4kotlin.integrationTests

import mu.KotlinLogging
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.runners.MethodSorters
import java.math.BigDecimal
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

import com.amcentral365.pl4kotlin.InsertStatement
import com.amcentral365.pl4kotlin.SelectStatement
import com.amcentral365.pl4kotlin.UpdateStatement
import com.amcentral365.pl4kotlin.DeleteStatement
import com.amcentral365.pl4kotlin.JdbcTypeCode
import java.sql.Connection
import java.sql.Timestamp
import java.util.UUID


private val logger = KotlinLogging.logger {}

/**
 * Run integration tests for all statements.
 * The tests are CRUD-ordered
 *
 * A few different techniques are used:
 *   - connection management: sometimes we supply connection getter, and sometimes passing a pre-allocated connection
 *   - specifying updated fields via class or entity instance reference (the result is the same)
 *   - fetching back values after update
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class StatementsIT {

    private val numOfRecordsToTest = 11
    private var runTearDown = false

    companion object {
        lateinit var conn: Connection
    }

    @Test fun m00Setup() {
        logger.info { "IT init" }
        initSql()
        runSqlSetup()
        this.runTearDown = true

        StatementsIT.conn = getConnection()
    }

    @Test fun m99TearDown() {
        logger.info { "IT cleanup" }
        if( this.runTearDown ) {
            runSqlTeardown()
        }
        StatementsIT.conn.close()
    }

    @Test fun m10InsertStatement() {
        logger.info { "running InsertStatement test" }
        val tto1 = TestTbl()

        val count = InsertStatement(tto1, getGoodConnection = ::getConnection).run()
        assertEquals(1, count)

        assertEquals(TestTbl.KNOWN_PK1, tto1.pk1)
        assertEquals(TestTbl.KNOWN_PK2, tto1.pk2)

        assertNotEquals(TestTbl.KNOWN_UUID1, tto1.uuid1)
        assertEquals   (TestTbl.KNOWN_UUID2, tto1.uuid2)

        assertNotNull(tto1.created)
        assertNotNull(tto1.modified)
        assertEquals(tto1.created!!.time / 1000, tto1.modified!!.time / 1000)  // ignore milliseconds

        assertEquals(TestTbl.KNOWN_VC, tto1.vcVal)
        assertEquals(TestTbl.KNOWN_CHAR.trimEnd(), tto1.charVal?.trimEnd())  // some DBs ignore truncate trailing whitespace, and some don't
        assertEquals(TestTbl.KNOWN_DATE, tto1.dateVal)
        assertEquals(TestTbl.KNOWN_TIME, tto1.timeVal)
        assertEquals(TestTbl.KNOWN_NUM, tto1.numVal)
        assertEquals(TestTbl.KNOWN_FLOAT, tto1.floatVal)
        assertEquals(TestTbl.KNOWN_DOUBLE, tto1.doubleVal)
        assertEquals(TestTbl.KNOWN_BITS, tto1.bit17Val)
        assertTrue(tto1.boolVal!!)
        assertEquals(TestTbl.GreekLetters.Epsilon, tto1.enumVal)
        assertNull(tto1.nullVal)

        val tto2 = this.jdbcreadTestTblRec(tto1.pk1, tto1.pk2!!)
        assertNotNull(tto2)
        ensureEq(tto1, tto2!!)
    }

    @Test fun m20testSelectStatement() {
        insertRecords(this.numOfRecordsToTest)

        logger.info { "running testSelectStatement(${this.numOfRecordsToTest})" }
        for(k in (1..this.numOfRecordsToTest).shuffled()) {
            val tto1 = TestTbl(k)
            logger.debug { "  $k: running SelectStatement for pk(${tto1.pk1}, ${tto1.pk2})" }
            val selCnt = SelectStatement(tto1).select(tto1.allColsButPk!!).byPk().run(StatementsIT.conn)
            assertEquals(1, selCnt)

            val tto2 = this.jdbcreadTestTblRec(tto1.pk1, k.toShort())
            assertNotNull(tto2)

            ensureEq(tto1, tto2!!)
        }
    }

    @Test fun m30testUpdateStatement() {
        logger.info { "running testUpdateStatement" }

        // the test used to sometimes fail when modified_ts was declared with second precision,
        // because things happened within the same second.
        // now modify_ts is declared as timestamp(6) to ensure nanosecond precision

        val pk2ToUpdate = 1
        val tto1 = TestTbl(pk2ToUpdate)
        val selCnt = SelectStatement(tto1).select(tto1.allColsButPk!!).byPk().run(StatementsIT.conn)
        assertEquals(1, selCnt)

        val newUuid1 = UUID.randomUUID()
        val oldUuid2 = tto1.uuid2
        val newEnumVal = TestTbl.GreekLetters.values()[(tto1.enumVal!!.ordinal+2) % 5]
        val oldModified = tto1.modified
        val oldCreated  = tto1.created

        tto1.uuid1   = newUuid1
        tto1.enumVal = newEnumVal
        tto1.created = Timestamp(tto1.created!!.time + 525252)

        val updCnt = UpdateStatement(tto1)
                .update(tto1::uuid1)
                .update(TestTbl::uuid2)
                .update(TestTbl::enumVal)
                .update(tto1::modified, "default")
                .byPkAndOptLock()
                .fetchBack(tto1::created)
                .fetchBack(tto1::modified)
                .run(StatementsIT.conn)

        assertEquals(1, updCnt)
        assertEquals(newUuid1, tto1.uuid1)
        assertEquals(oldUuid2, tto1.uuid2)
        assertEquals(oldCreated,     tto1.created)      // should have stayed the same
        assertNotEquals(oldModified, tto1.modified)     // should have changed to current timestamp

        val selRec = jdbcreadTestTblRec(tto1.pk1, tto1.pk2!!)
        assertNotNull(selRec)
        ensureEq(tto1, selRec!!)

        StatementsIT.conn.rollback()
    }

    @Test fun m40testDeleteStatement() {
        logger.info { "running testDeleteStatement($numOfRecordsToTest)" }

        for(k in (1..this.numOfRecordsToTest).shuffled()) {
            val tto = TestTbl(k)
            logger.debug { "  $k: running DeleteStatement for pk(${tto.pk1}, ${tto.pk2})" }
            val delCnt = DeleteStatement(tto).byPk().run(StatementsIT.conn)
            StatementsIT.conn.commit()
            assertEquals(1, delCnt)
            assertEquals(k.toShort(), tto.pk2)

            val selRec = jdbcreadTestTblRec(tto.pk1, k.toShort())
            assertNull(selRec)
        }
    }


    // ------------------------------------------------------- helper functions

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
        // Oracle doesn't truncate it.
        assertEquals(tto1.charVal?.trimEnd(), tto2.charVal?.trimEnd())

        // BigDecimal should use their own Compare
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
        enumVal = TestTbl.GreekLetters.values()[k % 5]  // round robin Alpha to Epsilon
    }


    private fun insertRecords(count: Int) {
        logger.info { "inserting $numOfRecordsToTest records" }
        for(k in 1..count) {
            val r = this.tweakForK(TestTbl(), k)
            val insertCount = InsertStatement(r).run(StatementsIT.conn)
            assertEquals(1, insertCount)
        }
        StatementsIT.conn.commit()
    }


    private fun jdbcreadTestTblRec(ppk1: Int, ppk2: Short): TestTbl? {
        val sql = "select bit17_val, bool_col, char_col, created_ts, date_col, double_col, " +
                         "enum_col, float_col, modified_ts, num_col, time_col, uuid1, " +
                         "uuid2, vc_col, null_col " +
                    "from test_tbl " +
                   "where pk1 = ? and pk2 = ?"

        // do not catch exceptions, let test fail if one is thrown
        StatementsIT.conn.prepareStatement(sql).use { stmt ->
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
