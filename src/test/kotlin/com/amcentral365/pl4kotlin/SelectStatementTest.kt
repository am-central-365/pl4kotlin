package com.amcentral365.pl4kotlin

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.assertThrows
import java.sql.Timestamp
import java.util.Arrays



internal class SelectStatementTest {

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

    companion object {
        fun checkDescr(descr: BaseStatement.Descr, colDef: Entity.ColDef?, expr: String?=null, asc: Boolean=true, vararg  binds: Any?) {
            assertEquals(colDef, descr.colDef)
            assertEquals(expr,   descr.expr)
            assertEquals(asc,    descr.asc)
            assertIterableEquals(Arrays.asList(*binds), descr.binds)
        }
    }


    @Test
    fun build() {
        var sql: String

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
                .select(Tx::pkField2)      // property
                .select(Tx::val1Field, "sysdate - trunc(sysdate) + ? + ?", 7, 'x')  // expression into property
                .select("val2Col")         // column name
                .byPkAndOptLock()          // by canned expression
                .by(Tx::val1Field)         // by property
                .by("val2Col")             // by column name
                .by("?+? = ?", 2, 29, 31)  // by expression
                .orderBy(Tx::val1Field, false)  // order by property
                .orderBy("pkCol1")              // order by column name
                .orderBy("random(?)", -18)      // order by expression

        val sql = stmt.build()

        // select expression must have a target column/property, translates to "expr into column"
        assertTrue( stmt.selectDescrs.all { it.colDef != null },
                "All SELECT expressions must have a target column/property")
        assertEquals("SELECT pkCol2, sysdate - trunc(sysdate) + ? + ?, val2Col "+
                "FROM tx "+
                "WHERE optLockCol = ? AND pkCol1 = ? AND pkCol2 = ? "+
                  "AND val1Col = ? " +
                  "AND val2Col = ? " +
                  "AND ?+? = ? " +
                "ORDER BY val1Col DESC, pkCol1, random(?)",
                sql
        )

        assertEquals( 3,  stmt.selectDescrs.size)
        checkDescr(stmt.selectDescrs[0], this.pk2ColDef)
        checkDescr(stmt.selectDescrs[1], this.val1ColDef, "sysdate - trunc(sysdate) + ? + ?", true, 7, 'x')
        checkDescr(stmt.selectDescrs[2], this.val2ColDef)

        assertEquals( 6,  stmt.whereDescrs.size)
        checkDescr(stmt.whereDescrs[0], this.optLockColDef)
        checkDescr(stmt.whereDescrs[1], this.pk1ColDef)
        checkDescr(stmt.whereDescrs[2], this.pk2ColDef)
        checkDescr(stmt.whereDescrs[3], this.val1ColDef)
        checkDescr(stmt.whereDescrs[4], this.val2ColDef)
        checkDescr(stmt.whereDescrs[5], null, "?+? = ?", true, 2, 29, 31)

        assertEquals( 3,  stmt.orderDescrs.size)
        checkDescr(stmt.orderDescrs[0], this.val1ColDef, asc=false)
        checkDescr(stmt.orderDescrs[1], this.pk1ColDef)
        checkDescr(stmt.orderDescrs[2], null, "random(?)", true, -18)

    }
}