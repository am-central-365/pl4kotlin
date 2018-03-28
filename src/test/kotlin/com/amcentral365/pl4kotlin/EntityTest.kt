package com.amcentral365.pl4kotlin

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull

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
}