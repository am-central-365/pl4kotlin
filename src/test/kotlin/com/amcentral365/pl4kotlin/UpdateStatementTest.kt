package com.amcentral365.pl4kotlin

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.assertThrows
import java.sql.PreparedStatement
import java.sql.Timestamp
import java.util.Arrays


internal class UpdateStatementTest {

    @Table("tx")
    class Tx: Entity() {
        @Column("pkCol1",  pkPos = 1) var pkField1:   Int = 0
        @Column("pkCol2",  pkPos = 2) var pkField2:   String = ""
        @Column("val1Col")            var val1Field:  Float = 0f
        @Column("val2Col")            var val2Field:  Boolean = true
        @Column("val3Col")            var val3Field:  Float = 0f
        @Column("val4Col")            var val4Field:  Boolean = true
        @Column("optLockCol", isOptimisticLock = true) var optLockField: Timestamp? = null
    }

    val txInst = Tx()
    val pk1ColDef  = txInst.colDefs.first { it.fieldName == Tx::pkField1.name }
    val pk2ColDef  = txInst.colDefs.first { it.fieldName == Tx::pkField2.name }
    val val1ColDef = txInst.colDefs.first { it.fieldName == Tx::val1Field.name }
    val val2ColDef = txInst.colDefs.first { it.fieldName == Tx::val2Field.name }
    val val3ColDef = txInst.colDefs.first { it.fieldName == Tx::val3Field.name }
    val val4ColDef = txInst.colDefs.first { it.fieldName == Tx::val4Field.name }
    val optLockColDef = txInst.colDefs.first { it.fieldName == Tx::optLockField.name }


    @Test
    fun `simple statement`() {
        val stmt = UpdateStatement(txInst).update(Tx::val1Field).byPk()
        val sql = stmt.build()

        assertEquals("UPDATE tx SET val1Col = ? WHERE pkCol1 = ? AND pkCol2 = ?", sql)

        assertEquals(1, stmt.updateDescrs.size)
        SelectStatementTest.checkDescr(stmt.updateDescrs[0], this.val1ColDef)

        assertEquals(2, stmt.whereDescrs.size)
        SelectStatementTest.checkDescr(stmt.whereDescrs[0], this.pk1ColDef)
        SelectStatementTest.checkDescr(stmt.whereDescrs[1], this.pk2ColDef)
    }


    @Test
    fun `all but pk and optlock`() {
        val stmt = UpdateStatement(txInst).update(txInst.allColsButPkAndOptLock!!).byPkAndOptLock()
        val sql = stmt.build()

        assertEquals("UPDATE tx "+
                        "SET val1Col = ?, val2Col = ?, val3Col = ?, val4Col = ? "+
                      "WHERE optLockCol = ? AND pkCol1 = ? AND pkCol2 = ?", sql)

        assertEquals(4, stmt.updateDescrs.size)
        SelectStatementTest.checkDescr(stmt.updateDescrs[0], this.val1ColDef)
        SelectStatementTest.checkDescr(stmt.updateDescrs[1], this.val2ColDef)
        SelectStatementTest.checkDescr(stmt.updateDescrs[2], this.val3ColDef)
        SelectStatementTest.checkDescr(stmt.updateDescrs[3], this.val4ColDef)

        assertEquals(3, stmt.whereDescrs.size)
        SelectStatementTest.checkDescr(stmt.whereDescrs[0], this.optLockColDef)
        SelectStatementTest.checkDescr(stmt.whereDescrs[1], this.pk1ColDef)
        SelectStatementTest.checkDescr(stmt.whereDescrs[2], this.pk2ColDef)
    }


    @Test
    fun `colDefs and no where`() {
        val stmt = UpdateStatement(txInst).update(listOf(this.val3ColDef, this.pk1ColDef, this.optLockColDef))
        val sql = stmt.build()

        assertEquals("UPDATE tx SET val3Col = ?, pkCol1 = ?, optLockCol = ?", sql)

        assertEquals(3, stmt.updateDescrs.size)
        SelectStatementTest.checkDescr(stmt.updateDescrs[0], this.val3ColDef)
        SelectStatementTest.checkDescr(stmt.updateDescrs[1], this.pk1ColDef)
        SelectStatementTest.checkDescr(stmt.updateDescrs[2], this.optLockColDef)

        assertEquals(0, stmt.whereDescrs.size)
    }


    @Test
    fun `update expression variations`() {
        val stmt = UpdateStatement(txInst)
                .update(Tx::val2Field)
                .update(Tx::val1Field, "sysdate+?/?", 24, 60)
                .update("val4Col")
                .update("val3Col", "?+?", 17, 19)
                .by(Tx::pkField2)
                .by("pkCol1")
                .by("?*? + ?*? = ?*?", 17, 18, 27, 28, 37, 38)

        val sql = stmt.build()

        assertEquals("UPDATE tx "+
                        "SET val2Col = ?, val1Col = sysdate+?/?, val4Col = ?, val3Col = ?+? "+
                      "WHERE pkCol2 = ? AND pkCol1 = ? AND ?*? + ?*? = ?*?", sql)

        assertEquals(4, stmt.updateDescrs.size)
        SelectStatementTest.checkDescr(stmt.updateDescrs[0], this.val2ColDef)
        SelectStatementTest.checkDescr(stmt.updateDescrs[1], this.val1ColDef, "sysdate+?/?", true, 24, 60)
        SelectStatementTest.checkDescr(stmt.updateDescrs[2], this.val4ColDef)
        SelectStatementTest.checkDescr(stmt.updateDescrs[3], this.val3ColDef, "?+?", true, 17, 19)

        assertEquals(3, stmt.whereDescrs.size)
        SelectStatementTest.checkDescr(stmt.whereDescrs[0], this.pk2ColDef)
        SelectStatementTest.checkDescr(stmt.whereDescrs[1], this.pk1ColDef)
        SelectStatementTest.checkDescr(stmt.whereDescrs[2], null, "?*? + ?*? = ?*?", true, 17, 18, 27, 28, 37, 38)
    }


    @Test
    fun fetchbacks() {
        val stmt = UpdateStatement(txInst).update(Tx::pkField2).fetchBack(Tx::val2Field).fetchBack("val4Col")
        val sql = stmt.build()

        assertEquals("UPDATE tx SET pkCol2 = ?", sql)

        assertEquals(1, stmt.updateDescrs.size)
        SelectStatementTest.checkDescr(stmt.updateDescrs[0], this.pk2ColDef)

        assertEquals(0, stmt.whereDescrs.size)

        assertEquals(2, stmt.fetchbackDescrs.size)
        SelectStatementTest.checkDescr(stmt.fetchbackDescrs[0], this.val2ColDef)
        SelectStatementTest.checkDescr(stmt.fetchbackDescrs[1], this.val4ColDef)
    }


    @Test
    fun `update list shouldn't be empty`() {
        val stmt = UpdateStatement(txInst).byPk().fetchBack(Tx::val2Field).fetchBack("val4Col")
        val x = assertThrows<IllegalArgumentException>("shouldn't allow empty update list") {  stmt.build() }
        assertTrue(x.message!!.contains("no columns to update"))
    }
}