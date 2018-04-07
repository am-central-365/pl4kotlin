package com.amcentral365.pl4kotlin

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
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

    val txInst = Tx()

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

        // where expressions may not have target column/property:
        //    when column is present, translates to "col = expr"
        //    when column is empty,   translates to "expr"
        // TODO: add test

        // order by has either column with asc/desc flag and no expression, or expresion and no column
        // TODO: add test
    }

    @Test
    fun `descriptors and binds`() {
        val stmt = SelectStatement(txInst)
                .select(Tx::pkField2)
                .select(Tx::val1Field, "sysdate - trunc(sysdate) + ? + ?", 7, 'x')
                .select("val2Col")
                .byPkAndOptLock()
                .orderBy(Tx::val1Field)

        val sql = stmt.build()

        // select expression must have a target column/property, translates to "expr into column"
        assertTrue( stmt.selectDescrs.all { it.colDef != null },
                "All SELECT expressions must have a target column/property")
        assertEquals("SELECT pkCol2, sysdate - trunc(sysdate) + ? + ?, val2Col "+
                "FROM tx "+
                "WHERE optLockCol = ? AND pkCol1 = ? AND pkCol2 = ? "+
                "ORDER BY val1Col",
                sql
        )

        assertEquals( 3,  stmt.selectDescrs.size)
        assertEquals( 2,  stmt.selectDescrs[1].binds?.size)
            assertEquals( 7,  stmt.selectDescrs[1].binds!![0])
            assertEquals('x', stmt.selectDescrs[1].binds!![1])

    }
}