package com.amcentral365.pl4kotlin

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.assertEquals


internal class SelectStatementTest {

    @Test
    fun build() {
        @Table("tx")
        class Tx: Entity() {
            @Column("pkCol",  pkPos = 1) var pkField:   Int = 0
            @Column("val1Col")           var val1Field: Int = 0
            @Column("val2Col")           var val2Field: Int = 0
        }

        val sql = SelectStatement(Tx()).select(Tx::val1Field).byPk().orderByCol("val2Col").build()
        assertEquals("SELECT val1Col FROM tx WHERE pkCol = ? ORDER BY val2Col", sql)
    }
}