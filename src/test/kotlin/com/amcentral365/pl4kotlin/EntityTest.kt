package com.amcentral365.pl4kotlin

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import java.util.Arrays
import java.util.UUID
import java.util.TreeMap


internal class EntityTest {

    companion object {
        fun checkColDefs(c1: Entity.ColDef?, c2: Entity.ColDef?) {
            if( c1 == null )
                assertNull(c2)
            else {
                assertNotNull(c2)
                assertEquals(0, c1.compareTo(c2!!))
            }

        }

        fun checkDescr(descr: BaseStatement.Descr, colDef: Entity.ColDef?, expr: String?=null, asc: Boolean=true, vararg  binds: Any?) {
            checkColDefs(colDef, descr.colDef)
            assertEquals(expr,   descr.expr)
            assertEquals(asc,    descr.asc)
            assertIterableEquals(Arrays.asList(*binds), descr.binds)
        }
    }

    @Test
    fun goodEntityConstructor() {

        @Table("tx")
        class Tx: Entity() {
            @Column("pkCol",  pkPos = 1) var pkField:  Int = 0
            @Column("valCol")            var valField: Int = 0
        }

        val tx = Tx()
        assertNotNull(tx.colDefs)
        assertNotNull(tx.pkCols)

        assertEquals(2, tx.colDefs.size)
        assertEquals("pkCol",    tx.colDefs[0].columnName)
        assertEquals("pkField",  tx.colDefs[0].fieldName)
        assertEquals("valCol",   tx.colDefs[1].columnName)
        assertEquals("valField", tx.colDefs[1].fieldName)

        assertEquals(1, tx.pkCols.size)
        checkColDefs(tx.colDefs[0], tx.pkCols[0])
    }


    @Test
    fun `no @Table annotation`() {
        class Tx: Entity()
        val x = assertThrows<IllegalArgumentException>("should have failed on missing @Table") { Tx() }
        assertTrue(x.message!!.contains("must be annotated with @Table"), "wrong error message: ${x.message}")
    }


    @Test
    fun `empty table name`() {
        @Table(" ") class Tx: Entity()
        val x = assertThrows<IllegalArgumentException>("should have failed on empty table name") { Tx() }
        assertTrue(x.message!!.contains("annotation attribute tableName can't be empty"), "wrong error message: ${x.message}")
    }


    @Test
    fun `no columns defined`() {
        @Table("tx") class Tx: Entity()
        val x = assertThrows<IllegalArgumentException>("should have failed on missing @Column") { Tx() }
        assertTrue(x.message!!.contains("does not declare any fields wih Column annotation"), "wrong error message: ${x.message}")
    }


    @Test
    fun `dup column name`() {
        @Table("tx")
        class Tx: Entity() {
            @Column("pk", pkPos = 1) var field0: Int = 0
            @Column("col") var bob:   Int = 0
            @Column("col") var alice: Int = 0
        }

        val x = assertThrows<IllegalArgumentException>("should have failed on duplicate column name") { Tx() }
        assertTrue(x.message!!.contains("declares same column name col in fields"), "wrong error message: ${x.message}")
    }


    @Test
    fun `multiple OptLock`() {
        @Table("tx")
        class Tx: Entity() {
            @Column("pk0", pkPos = 1) var field0: Int = 0
            @Column("pkA", isOptimisticLock = true) var fieldA: Int = 0
            @Column("pkB", isOptimisticLock = true) var fieldB: Int = 0
        }

        val x = assertThrows<IllegalArgumentException>("should have failed on multiple optLock columns") { Tx() }
        assertTrue(x.message!!.contains("defines more than one Optimistic Lock fields: fieldA and fieldB"), "wrong error message: ${x.message}")
    }


    @Test
    fun `OptLock is PK`() {
        @Table("tx")
        class Tx: Entity() {
            @Column("pkA", isOptimisticLock = true, pkPos = 1) var pkFieldA: Int = 0
        }

        val x = assertThrows<IllegalArgumentException>("should have failed on PK optLock") { Tx() }
        assertTrue(x.message!!.contains("optimistic lock can't be part of the PK"), "wrong error message: ${x.message}")
    }


    @Test
    fun `bad OptLock type`() {
        @Table("tx")
        class Tx: Entity() {
            @Column("pk0", pkPos = 1) var field0: Int = 0
            @Column("pkA", isOptimisticLock = true) var fieldA: String = "..."
        }

        val x = assertThrows<IllegalArgumentException>("should have failed on bad optLock type") { Tx() }
        assertTrue(x.message!!.contains("supported Optimistic Lock types are Timestamp and Number"), "wrong error message: ${x.message}")
    }


    @Test
    fun `bad client-generated-always type`() {
        @Table("tx")
        class Tx: Entity() {
            @Column("pk0", pkPos = 1) var field0: Int = 0
            @Column("pkA", onInsert = Generated.OnTheClientAlways) var fieldA: String = "..."
        }

        val x = assertThrows<IllegalArgumentException>("should have failed on bad client-generated type") { Tx() }
        assertTrue(x.message!!.contains("client-side generation is only supported for UUID and Timestamp"), "wrong error message: ${x.message}")
    }


    @Test
    fun `bad client-generated-when-null type`() {
        @Table("tx")
        class Tx: Entity() {
            @Column("pk0", pkPos = 1) var field0: Int = 0
            @Column("pkA", onInsert = Generated.OneTheClientWhenNull) var fieldA: String = "..."
        }

        val x = assertThrows<IllegalArgumentException>("should have failed on bad client-generated type") { Tx() }
        assertTrue(x.message!!.contains("client-side generation is only supported for UUID and Timestamp"), "wrong error message: ${x.message}")
    }


    /*@Test
    // Throws:
    // Kotlin.reflect.jvm.internal.KotlinReflectionInternalError: Cannot calculate JVM erasure for type: [ERROR : Unknown type parameter 0]
    fun `pass on proper client-generated type`() {
        fun <T> mustPass() {
            @Table("tx")
            class Tx: Entity() {
                @Column("pk0", pkPos = 1) var field0: Int = 0
                @Column("pkA", onInsert = Generated.OnTheClientAlways) var fieldA: T? = null
            }

            Tx()  // should throw no exceptions
        }

        mustPass<Number>()
        mustPass<Int>()
        mustPass<Short>()
        mustPass<Long>()
        mustPass<Float>()
        mustPass<Double>()
        mustPass<java.lang.Short>()
        mustPass<java.lang.Integer>()
        mustPass<java.lang.Long>()
        mustPass<java.lang.Float>()
        mustPass<java.lang.Double>()
        mustPass<java.lang.Boolean>()
        mustPass<java.sql.Timestamp>()
    }*/


    @Test
    fun `database-generated PK`() {
        @Table("tx1")
        class Tx1: Entity() {
            @Column("pkA", pkPos = 1, onInsert = Generated.OnTheDbAlways) var pkFieldA: Int = 0
        }

        @Table("tx2")
        class Tx2: Entity() {
            @Column("pkA", pkPos = 1, onInsert = Generated.OnTheDbWhenNull) var pkFieldA: Int = 0
        }

        val x1 = assertThrows<IllegalArgumentException>("should have failed on db-generated PK") { Tx1() }
        val x2 = assertThrows<IllegalArgumentException>("should have failed on db-generated PK") { Tx2() }
        val expectedMsg = "PK columns can't be generated on the database side"
        assertTrue(x1.message!!.contains(expectedMsg), "wrong error message: ${x1.message}")
        assertTrue(x2.message!!.contains(expectedMsg), "wrong error message: ${x2.message}")
    }


    @Test
    fun `missing PK`() {
        @Table("tx") class Tx: Entity() { @Column("c") var c = 0 }
        val x = assertThrows<IllegalArgumentException>("should have failed on missing PK") { Tx() }
        assertTrue(x.message!!.contains("PK is missing"), "wrong error message: ${x.message}")
    }


    @Test
    fun `extra pkPos`() {
        @Table("tx")
        class Tx: Entity() {
            @Column("pkA", pkPos = -1) var pkFieldA: Int = 0
            @Column("pkB", pkPos =  1) var pkFieldB: Int = 0
        }

        val x = assertThrows<IllegalArgumentException>("should have failed on negative pkPos") { Tx() }
        assertTrue(x.message!!.contains("unexpected pkPos values -1"), "wrong error message: ${x.message}")
    }


    @Test
    fun `too big pkPos`() {
        @Table("tx")
        class Tx: Entity() {
            @Column("pkA", pkPos = 1) var pkFieldA: Int = 0
            @Column("pkB", pkPos = 3) var pkFieldB: Int = 0
        }

        val x = assertThrows<IllegalArgumentException>("should have failed on too big pkPos") { Tx() }
        assertTrue(x.message!!.contains("missing pkPos values 2"), "wrong error message: ${x.message}")
    }


    @Test
    fun `missing pkPos`() {
        @Table("tx")
        class Tx: Entity() {
            @Column("pkA", pkPos = 1) var pkFieldA: Int = 0
            @Column("pkB", pkPos = 1) var pkFieldB: Int = 0
            @Column("pkC", pkPos = 1) var pkFieldC: Int = 0
            @Column("pkD", pkPos = 4) var pkFieldD: Int = 0
        }

        val x = assertThrows<IllegalArgumentException>("should have failed on missing pkPos") { Tx() }
        assertTrue(x.message!!.contains("missing pkPos values 2,3"), "wrong error message: ${x.message}")
    }


    @Test
    fun `duplicate pkPos`() {
        @Table("tx")
        class Tx: Entity() {
            @Column("pkA", pkPos = 1) var pkFieldA: Int = 0
            @Column("pkB", pkPos = 1) var pkFieldB: Int = 0
            @Column("pkC", pkPos = 2) var pkFieldC: Int = 0
        }

        val x = assertThrows<IllegalArgumentException>("should have failed on missing pkPos") { Tx() }
        assertTrue(x.message!!.contains("duplicate fields with pkPos 1"), "wrong error message: ${x.message}")
    }

    enum class GreekLetters { Alpha, Beta, Gamma, Delta, Epsilon }

    @Test
    fun `assign from REST params Map`() {
        @Table("tx")
        class Tx(rest: Map<String, String>): Entity(rest) {
            @Column("int_col",   pkPos = 1) var intVal: Int?   = null
            @Column("short_col", restParamName = "short_p") var shortVal: Short? = null
            @Column("uuid_col")   var uuidVal:   UUID?         = null
            @Column("ts_col")     var tsVal:     Timestamp?    = null
            @Column("vc_col")     var vcVal:     String?       = null
            @Column("char_col")   var charVal:   String?       = null
            @Column("date_col")   var dateVal:   Date?         = null
            @Column("time_col")   var timeVal:   Time?         = null
            @Column("num_col")    var numVal:    BigDecimal?   = null
            @Column("float_col")  var floatVal:  Float?        = null
            @Column("double_col") var doubleVal: Double?       = null
            @Column("bit17_col")  var bit17Val:  Long?         = null
            @Column("bool_col")   var boolVal:   Boolean?      = null
            @Column("enum_col")   var enumVal:   GreekLetters? = null
            @Column("null_col")   var nullVal:   String?       = null
        }

        val INT_VAL = 2253462
        val SHORT_VAL: Short = -456
        val UUID_STR = "80f30906-e089-4623-835c-83255e4f3c69"
        val TS_STR = "1941-06-22 04:13:03.45673"
        val VC_STR = "Dark + Light beer is The Best"
        val CH_STR = "TL;DR "
        val DATE_STR = "1945-05-09"
        val TIME_STR = "12:34:56"
        val NUM_STR = "7932098450457092.565424434934089534653453453454352342348768681"
        val FLOAT_VAL = 235346.675878f
        val DBL_VAL = 3232634.7457045572
        val BIT_VAL: Long = 0b10100011100111100

        val restParams = TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
        restParams.putAll(mapOf(
              "int_col"  to INT_VAL.toString()
            , "short_p"  to SHORT_VAL.toString()
            , "uuid_col" to UUID_STR
            , "ts_col"   to TS_STR
            , "vc_col"   to VC_STR
            , "char_col" to CH_STR
            , "date_col" to DATE_STR
            , "time_col" to TIME_STR
            , "num_col"  to NUM_STR
            , "float_col"  to FLOAT_VAL.toString()
            , "double_col" to DBL_VAL.toString()
            , "bit17_col"  to BIT_VAL.toString()
            , "bool_col"   to "true"
            , "enum_col"   to "Delta"
        ))


        val tx = Tx(restParams)

        assertEquals(INT_VAL, tx.intVal)
        assertEquals(SHORT_VAL, tx.shortVal)
        assertEquals(UUID.fromString(UUID_STR), tx.uuidVal)
        assertEquals(Timestamp.valueOf(TS_STR), tx.tsVal)
        assertEquals(VC_STR, tx.vcVal)
        assertEquals(CH_STR, tx.charVal)
        assertEquals(Date.valueOf(DATE_STR), tx.dateVal)
        assertEquals(Time.valueOf(TIME_STR), tx.timeVal)
        assertEquals(BigDecimal(NUM_STR), tx.numVal)
        assertEquals(FLOAT_VAL, tx.floatVal)
        assertEquals(DBL_VAL, tx.doubleVal)
        assertEquals(BIT_VAL, tx.bit17Val)
        assertEquals(GreekLetters.Delta, tx.enumVal)

        assertNull(tx.nullVal)

        assertNotNull(tx.boolVal)
        assertTrue(tx.boolVal!!)
    }


    @Test
    fun `identityStr for 1-col pk`() {
        @Table("tx")
        class Tx: Entity() {
            @Column("pkA", pkPos = 1) var pk1: Int = 135
        }

        val tx = Tx()
        assertEquals("""{"pk": {"pkA": 135}}""", tx.getIdentityAsJsonStr())
    }


    @Test
    fun `identityStr for 3-col pk`() {
        @Table("tx")
        class Tx: Entity() {
            @Column("pkA", pkPos = 1) var pk1: Int = 135
            @Column("pkB", pkPos = 2) var pk2: String = "Dom Perignon"
            @Column("pkC", pkPos = 3) var pk3: Boolean = true
        }

        val tx = Tx()
        assertEquals("""{"pk": {"pkA": 135, "pkB": "Dom Perignon", "pkC": true}}""", tx.getIdentityAsJsonStr())
    }


    @Test
    fun `identityStr for pk and optLock`() {
        val TS_STR = "1961-04-12 09:07:17.943"

        @Table("tx")
        class Tx: Entity() {
            @Column("pkA", pkPos = 1) var pk1: Int = 531
            @Column("pkB", pkPos = 2) var pk2: Boolean = false
            @Column("olc", isOptimisticLock = true) var modifyTs: Timestamp = Timestamp.valueOf(TS_STR)
        }

        val tx = Tx()
        assertEquals("""{"pk": {"pkA": 531, "pkB": false}, "optLock": {"olc": "$TS_STR"}}""", tx.getIdentityAsJsonStr())
    }


    @Test
    fun `as JSON`() {
        val tx = com.amcentral365.pl4kotlin.integrationTests.TestTbl()

        val expectedPretty =
            """
            {
              "bit17_val": 33540,
              "bool_col": true,
              "char_col": " a .b ",
              "created_ts": null,
              "date_col": "DATE_COL",
              "double_col": 3.141592653589793,
              "enum_col": "Epsilon",
              "float_col": -342.74237,
              "modified_ts": null,
              "null_col": null,
              "num_col": "2.718281828459045",
              "pk1": -451247,
              "pk2": 10221,
              "time_col": "TIME_COL",
              "uuid1": "UUID1",
              "uuid2": "UUID2",
              "vc_col": "Yea, from the table of my memory
            I’ll wipe away all trivial, fond records,"
            }"""
            .trimIndent()
            .replace("DATE_COL", tx.dateVal.toString())
            .replace("TIME_COL", tx.timeVal.toString())
            .replace("UUID1",    tx.uuid1.toString())
            .replace("UUID2",    tx.uuid2.toString())

        fun makeNonPretty(s: String) = "{" + s.replace("\n ", "").removePrefix("{ ").removeSuffix("\n}") + "}"

        // Test with nulls ON
        assertEquals(expectedPretty, tx.asJsonStr(pretty = true, withNulls = true))
        val expectedFunctional = makeNonPretty(expectedPretty)
        assertEquals(expectedFunctional, tx.asJsonStr(pretty = false, withNulls = true))

        // Same tests with nulls OFF
        val expectedPrettyNoNulls = expectedPretty
                .replace("  \"created_ts\": null,\n", "")
                .replace("  \"modified_ts\": null,\n", "")
                .replace("  \"null_col\": null,\n", "")

        assertEquals(expectedPrettyNoNulls, tx.asJsonStr(pretty = true, withNulls = false))
        val expectedFunctionalNoNulls = makeNonPretty(expectedPrettyNoNulls)
        assertEquals(expectedFunctionalNoNulls, tx.asJsonStr(pretty = false, withNulls = false))
    }

    @Test
    fun `isJson for String type`() {
        @Table("tx") class Tx: Entity() { @Column("c", pkPos = 1, isJson = true) var c: String? = null }
        val tx = Tx()  // should throw no exceptions
        //assertTrue(x.message!!.contains("PK is missing"), "wrong error message: ${x.message}")
    }

    @Test
    fun `isJson for non-String type`() {
        @Table("tx") class Tx: Entity() { @Column("c", pkPos = 1, isJson = true) var c: Int? = null }
        val x = assertThrows<IllegalArgumentException>("should have failed on non-String isJson column") { Tx() }
        assertTrue(x.message!!.contains("Json is only supported for client-side String type"), "wrong error message: ${x.message}")
    }
}