package com.amcentral365.pl4kotlin

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

import java.sql.Connection
import java.sql.Timestamp
import java.util.UUID

internal class InsertStatementTest {
    companion object {
        val uuid = UUID.randomUUID()
        val ts   = Timestamp(427)
    }

    @Table("tx")
    class Tx: Entity() {
        @Column("clAlwA",   onInsert = Generated.OnTheClientAlways)    var clAlwFieldA:  UUID? = InsertStatementTest.uuid
        @Column("clAlwB",   onInsert = Generated.OnTheClientAlways)    var clAlwFieldB:  UUID? = null
        @Column("clNullA",  onInsert = Generated.OneTheClientWhenNull) var clNullFieldA: Timestamp? = InsertStatementTest.ts
        @Column("clNullB",  onInsert = Generated.OneTheClientWhenNull) var clNullFieldB: Timestamp? = null
        @Column("dbAlwA",   onInsert = Generated.OnTheDbAlways)        var dbAlwFieldA:  String? = ""
        @Column("dbAlwB",   onInsert = Generated.OnTheDbAlways)        var dbAlwFieldB:  String? = null
        @Column("dbNullA",  onInsert = Generated.OnTheDbWhenNull)      var dbNullFieldA: Int? = 23
        @Column("dbNullB",  onInsert = Generated.OnTheDbWhenNull)      var dbNullFieldB: Int? = null
        @Column("pkCol",  pkPos = 1) var pkField:   Int = 0
    }

    val txInst = Tx()
    val clAlwAColDef  = txInst.colDefs.first { it.fieldName == Tx::clAlwFieldA.name }
    val clAlwBColDef  = txInst.colDefs.first { it.fieldName == Tx::clAlwFieldB.name }
    val clNullAColDef = txInst.colDefs.first { it.fieldName == Tx::clNullFieldA.name }
    val clNullBColDef = txInst.colDefs.first { it.fieldName == Tx::clNullFieldB.name }
    val dbAlwAColDef  = txInst.colDefs.first { it.fieldName == Tx::dbAlwFieldA.name }
    val dbAlwBColDef  = txInst.colDefs.first { it.fieldName == Tx::dbAlwFieldB.name }
    val dbNullAColDef = txInst.colDefs.first { it.fieldName == Tx::dbNullFieldA.name }
    val dbNullBColDef = txInst.colDefs.first { it.fieldName == Tx::dbNullFieldB.name }
    val pkColDef      = txInst.colDefs.first { it.fieldName == Tx::pkField.name }

    @Test
    fun build() {

        class InsertStatementUnderTest(entityDef: Entity, getGoodConnection: () -> Connection? = { null }): InsertStatement(entityDef, getGoodConnection) {
            override fun run(conn: Connection): Int {
                assertEquals("INSERT INTO tx(clAlwA, clAlwB, clNullA, clNullB, dbNullA, pkCol) VALUES(?, ?, ?, ?, ?, ?)", this.build())
                assertEquals(6, this.bindColDefs.size)

                Assertions.assertIterableEquals(
                        listOf(clAlwAColDef, clAlwBColDef, clNullAColDef, clNullBColDef, dbNullAColDef, pkColDef),
                        this.bindColDefs
                )

                assertEquals(3, this.fetchDescr.size)
                EntityTest.checkDescr(this.fetchDescr[0], dbAlwAColDef)
                EntityTest.checkDescr(this.fetchDescr[1], dbAlwBColDef)
                EntityTest.checkDescr(this.fetchDescr[2], dbNullBColDef)

                return 0
            }
        }

        InsertStatementUnderTest(this.txInst).run( { null } )
    }
}