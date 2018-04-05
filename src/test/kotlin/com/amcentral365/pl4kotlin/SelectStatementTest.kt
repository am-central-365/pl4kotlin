package com.amcentral365.pl4kotlin

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import java.sql.Timestamp


internal class SelectStatementTest {

    @Test
    fun build() {
        @Table("tx")
        class Tx: Entity() {
            @Column("pkCol1",  pkPos = 1) var pkField1:   Int = 0
            @Column("pkCol2",  pkPos = 2) var pkField2:   String = ""
            @Column("val1Col")            var val1Field:  Float = 0f
            @Column("val2Col")            var val2Field:  Boolean = true
            @Column("optLockCol", isOptimisticLock = true) var optLockField: Timestamp? = null
        }

        val txInst = Tx()
        var sql: String

        sql = SelectStatement(txInst).select(Tx::val1Field).byPk().orderBy("val2Col").build()
        assertEquals("SELECT val1Col FROM tx WHERE pkCol1 = ? AND pkCol2 = ? ORDER BY val2Col", sql)

        sql = SelectStatement(txInst).select(Tx::val1Field, Tx::val2Field).byPkAndOptLock().orderBy("val2Col", "val1Col").orderBy(Tx::pkField2).build()
        assertEquals("SELECT val1Col, val2Col FROM tx WHERE optLockCol = ? AND pkCol1 = ? AND pkCol2 = ? ORDER BY val2Col, val1Col, pkCol2", sql)

        val x = assertThrows<IllegalArgumentException>("should have failed on noncol") { SelectStatement(txInst).selectCol("noncol").build() }
        assertTrue(x.message!!.contains("unknown @Column with colName 'noncol'"))

        sql = SelectStatement(txInst).select("abs(pkCol1) > 10").by("rownum <= 1").build()
        assertEquals("SELECT abs(pkCol1) > 10 FROM tx WHERE rownum <= 1", sql)
    }
}