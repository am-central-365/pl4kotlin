package com.amcentral365.pl4kotlin

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertTrue

internal class EntityTest {

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

        assertEquals(2, tx.colDefs?.size)
        assertEquals("pkCol",    tx.colDefs?.get(0)?.columnName)
        assertEquals("pkField",  tx.colDefs?.get(0)?.fieldName)
        assertEquals("valCol",   tx.colDefs?.get(1)?.columnName)
        assertEquals("valField", tx.colDefs?.get(1)?.fieldName)

        assertEquals(1, tx.pkCols?.size)
        assertEquals(tx.colDefs?.get(0), tx.pkCols?.get(0))
    }


    @Test
    fun `no @Table annotation`() {
        class Tx: Entity() {}
        val x = assertThrows<IllegalArgumentException>("should have failed on missing @Table") { Tx() }
        assertTrue(x.message!!.contains("must be annotated with @Table"), "wrong error message: ${x.message}")
    }


    @Test
    fun `empty table name`() {
        @Table(" ") class Tx: Entity() {}
        val x = assertThrows<IllegalArgumentException>("should have failed on empty table name") { Tx() }
        assertTrue(x.message!!.contains("annotation attribute tableName can't be empty"), "wrong error message: ${x.message}")
    }


    @Test
    fun `no columns defined`() {
        @Table("tx") class Tx: Entity() { var v1 = 0 }
        val x = assertThrows<IllegalArgumentException>("should have failed on missing @Column") { Tx() }
        assertTrue(x.message!!.contains("does not declare any fields wih Column annotation"), "wrong error message: ${x.message}")
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
    fun `too high pkPos`() {
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

}