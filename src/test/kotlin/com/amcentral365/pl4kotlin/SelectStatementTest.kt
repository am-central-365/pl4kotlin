package com.amcentral365.pl4kotlin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.assertThrows
import java.sql.Timestamp


internal class SelectStatementTest {

    @Table("tx")
    class Tx: Entity() {
        @Column("pkCol1",  pkPos = 1) var pkField1:   Int = 0
        @Column("pkCol2",  pkPos = 2) var pkField2:   String = ""
        @Column("val1Col")            var val1Field:  Float = 0f
        @Column("val2Col")            var val2Field:  Boolean = true
        @Column("optLockCol", isOptimisticLock = true) var optLockField: Timestamp? = null
    }

    private val txInst = Tx()
    private val pk1ColDef  = txInst.colDefs.first { it.fieldName == Tx::pkField1.name }
    private val pk2ColDef  = txInst.colDefs.first { it.fieldName == Tx::pkField2.name }
    private val val1ColDef = txInst.colDefs.first { it.fieldName == Tx::val1Field.name }
    private val val2ColDef = txInst.colDefs.first { it.fieldName == Tx::val2Field.name }
    private val optLockColDef = txInst.colDefs.first { it.fieldName == Tx::optLockField.name }

    @Test
    fun build() {
        var sql: String

        // a simple statement
        sql = SelectStatement(txInst).select(Tx::val1Field).build()
        assertEquals("SELECT val1Col FROM tx", sql)

        // a simple statement
        sql = SelectStatement(txInst).select(Tx::val1Field).byPk().orderBy("val2Col").build()
        assertEquals("SELECT val1Col FROM tx WHERE pkCol1 = ? AND pkCol2 = ? ORDER BY val2Col", sql)

        // more complex statement
        sql = SelectStatement(txInst).select(Tx::val1Field).select(Tx::val2Field).byPkAndOptLock().orderBy("val2Col").orderBy("val1Col").orderBy(Tx::pkField2).build()
        assertEquals("SELECT val1Col, val2Col FROM tx WHERE optLockCol = ? AND pkCol1 = ? AND pkCol2 = ? ORDER BY val2Col, val1Col, pkCol2", sql)

        // fail on bad column name
        val x = assertThrows<IllegalArgumentException>("should have failed on noncol") { SelectStatement(txInst).select("noncol").build() }
        assertTrue(x.message!!.contains("unknown @Column with colName 'noncol'"))

        // where expression
        sql = SelectStatement(txInst).select(Tx::val1Field, expr="abs(pkCol1)+19").by(expr="rownum <= 1").build()
        assertEquals("SELECT abs(pkCol1)+19 FROM tx WHERE rownum <= 1", sql)
    }

    @Test
    fun `descriptors and binds`() {
        val stmt = SelectStatement(txInst)
                .select(Tx::pkField2)      // class property
                .select(txInst::pkField2)  // instance property
                .select(Tx::val1Field,     "sysdate - trunc(sysdate) + ? + ?", 7, 'x')  // expression into class property
                .select(txInst::val1Field, "sysdate - trunc(sysdate) + ? + ?", 7, 'x')  // expression into instance property
                .select("val2Col")                         // column name
                .select("val2Col", "trunc(sysdate, 'DD')") // expression into column
                .byPkAndOptLock()          // by canned expression
                .by(Tx::val1Field)         // by class property
                .by(txInst::val1Field)     // by instance property
                .by("val2Col")             // by column name
                .by("?+? = ?", 2, 29, 31)  // by expression
                .orderBy(Tx::val1Field,     false)  // order by class property
                .orderBy(txInst::val1Field, false)  // order by instance property
                .orderBy("pkCol1")              // order by column name
                .orderBy("random(?)", -18)      // order by expression
                .orderBy("concat(?,?) desc", 'a', "strB")   // order by another expression, binds should be flattened

        val sql = stmt.build()

        // select expression must have a target column/property, translates to "expr into column"
        assertTrue( stmt.selectDescrs.all { it.colDef != null },
                "All SELECT expressions must have a target column/property")
        assertEquals("SELECT pkCol2, pkCol2, sysdate - trunc(sysdate) + ? + ?, "+
                            "sysdate - trunc(sysdate) + ? + ?, val2Col, trunc(sysdate, 'DD') "+
                "FROM tx "+
                "WHERE optLockCol = ? AND pkCol1 = ? AND pkCol2 = ? "+
                  "AND val1Col = ? AND val1Col = ? " +
                  "AND val2Col = ? " +
                  "AND ?+? = ? " +
                "ORDER BY val1Col DESC, val1Col DESC, pkCol1, random(?), concat(?,?) desc",
                sql
        )

        assertEquals(6,  stmt.selectDescrs.size)
        EntityTest.checkDescr(stmt.selectDescrs[0], this.pk2ColDef)
        EntityTest.checkDescr(stmt.selectDescrs[1], this.pk2ColDef)
        EntityTest.checkDescr(stmt.selectDescrs[2], this.val1ColDef, "sysdate - trunc(sysdate) + ? + ?", true, 7, 'x')
        EntityTest.checkDescr(stmt.selectDescrs[3], this.val1ColDef, "sysdate - trunc(sysdate) + ? + ?", true, 7, 'x')
        EntityTest.checkDescr(stmt.selectDescrs[4], this.val2ColDef)
        EntityTest.checkDescr(stmt.selectDescrs[5], this.val2ColDef, "trunc(sysdate, 'DD')")

        assertEquals(7,  stmt.whereDescrs.size)
        EntityTest.checkDescr(stmt.whereDescrs[0], this.optLockColDef)
        EntityTest.checkDescr(stmt.whereDescrs[1], this.pk1ColDef)
        EntityTest.checkDescr(stmt.whereDescrs[2], this.pk2ColDef)
        EntityTest.checkDescr(stmt.whereDescrs[3], this.val1ColDef)
        EntityTest.checkDescr(stmt.whereDescrs[4], this.val1ColDef)
        EntityTest.checkDescr(stmt.whereDescrs[5], this.val2ColDef)
        EntityTest.checkDescr(stmt.whereDescrs[6], null, "?+? = ?", true, 2, 29, 31)

        assertEquals(5,  stmt.orderDescrs.size)
        EntityTest.checkDescr(stmt.orderDescrs[0], this.val1ColDef, asc=false)
        EntityTest.checkDescr(stmt.orderDescrs[1], this.val1ColDef, asc=false)
        EntityTest.checkDescr(stmt.orderDescrs[2], this.pk1ColDef)
        EntityTest.checkDescr(stmt.orderDescrs[3], null, "random(?)", true, -18)
        EntityTest.checkDescr(stmt.orderDescrs[4], null, "concat(?,?) desc", true, 'a', "strB")
    }

    @Test
    fun `by present values`() {
        @Table("tx")
        class Tx: Entity() {
            @Column("pk",  pkPos = 1) var pkField1: Int = 0
            @Column("c1")   var v1:   String? = null
            @Column("c2")   var v2:   Float?  = null
        }

        val tx = Tx()
        tx.v2 = 0.12f

        val sql = SelectStatement(tx).select(tx::v1).byPresentValues().build()
        assertEquals("SELECT c1 FROM tx WHERE pk = ? AND c2 = ?", sql)
    }
}
