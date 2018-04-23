package com.amcentral365.pl4kotlin

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

import java.sql.Connection
import java.sql.Timestamp
import java.util.UUID

internal class DeleteStatementTest {
    companion object {
        val uuid = UUID.randomUUID()
        val ts   = Timestamp(427)
    }

    @Table("tx")
    class Tx: Entity() {
        @Column("pkCol1",  pkPos = 1) var pkField1:   Int = 0
        @Column("pkCol2",  pkPos = 2) var pkField2:   String = ""
        @Column("val1Col")            var val1Field:  Float = 0f
        @Column("val2Col")            var val2Field:  Boolean = true
        @Column("optLockCol", isOptimisticLock = true) var optLockField: Timestamp? = null
    }

    val txInst = Tx()
    val pk1ColDef  = txInst.colDefs.first { it.fieldName == Tx::pkField1.name }
    val pk2ColDef  = txInst.colDefs.first { it.fieldName == Tx::pkField2.name }
    val val1ColDef = txInst.colDefs.first { it.fieldName == Tx::val1Field.name }
    val val2ColDef = txInst.colDefs.first { it.fieldName == Tx::val2Field.name }
    val optLockColDef = txInst.colDefs.first { it.fieldName == Tx::optLockField.name }

    @Test
    fun build() {
        var stmt = DeleteStatement(txInst)
        var sql = stmt.build()
        assertEquals("DELETE FROM tx", sql)
        assertEquals(0, stmt.whereDescrs.size)

        stmt = DeleteStatement(txInst).byPk()
        sql = stmt.build()
        assertEquals("DELETE FROM tx WHERE pkCol1 = ? AND pkCol2 = ?", sql)
        assertEquals(2, stmt.whereDescrs.size)
        EntityTest.checkDescr(stmt.whereDescrs[0], this.pk1ColDef)
        EntityTest.checkDescr(stmt.whereDescrs[1], this.pk2ColDef)

        stmt = DeleteStatement(txInst).byPkAndOptLock()
        sql = stmt.build()
        assertEquals("DELETE FROM tx WHERE optLockCol = ? AND pkCol1 = ? AND pkCol2 = ?", sql)
        assertEquals(3, stmt.whereDescrs.size)
        EntityTest.checkDescr(stmt.whereDescrs[0], this.optLockColDef)
        EntityTest.checkDescr(stmt.whereDescrs[1], this.pk1ColDef)
        EntityTest.checkDescr(stmt.whereDescrs[2], this.pk2ColDef)

        stmt = DeleteStatement(txInst)
                .by(Tx::pkField2)      // by class property
                .by(txInst::pkField2)  // by instance property
                .by("val2Col")
                .by("? / ? + ? > ?", 'e', uuid, ts, 45)
                .by("? * ?", "sX", ts)
        sql = stmt.build()
        assertEquals("DELETE FROM tx WHERE pkCol2 = ? AND pkCol2 = ? AND val2Col = ? AND ? / ? + ? > ? AND ? * ?", sql)
        assertEquals(5, stmt.whereDescrs.size)
        EntityTest.checkDescr(stmt.whereDescrs[0], this.pk2ColDef)
        EntityTest.checkDescr(stmt.whereDescrs[1], this.pk2ColDef)
        EntityTest.checkDescr(stmt.whereDescrs[2], this.val2ColDef)
        EntityTest.checkDescr(stmt.whereDescrs[3], null, "? / ? + ? > ?", true, 'e', uuid, ts, 45)
        EntityTest.checkDescr(stmt.whereDescrs[4], null, "? * ?", true, "sX", ts)
    }
}