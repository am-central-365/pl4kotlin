package com.amcentral365.pl4kotlin

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import java.sql.Connection
import java.sql.Timestamp
import java.util.UUID

internal class InsertStatementTest {

    @Test
    fun build() {
        val uuid = UUID.randomUUID()
        val ts   = Timestamp(14)

        @Table("tx")
        class Tx: Entity() {
            @Column("pkCol",  pkPos = 1) var pkField:   Int = 0
            @Column("genDbAlwA",   onInsert = Generated.OnTheDbAlways)        var genDbAlwFieldA:  String? = ""
            @Column("genDbAlwB",   onInsert = Generated.OnTheDbAlways)        var genDbAlwFieldB:  String? = null
            @Column("genDbNullA",  onInsert = Generated.OnTheDbWhenNull)      var genDbNull1FieldA: Int? = 23
            @Column("genDbNullB",  onInsert = Generated.OnTheDbWhenNull)      var genDbNull1FieldB: Int? = null
            @Column("genClAlwA",   onInsert = Generated.OnTheClientAlways)    var genClAlwFieldA: UUID? = uuid
            @Column("genClAlwB",   onInsert = Generated.OnTheClientAlways)    var genClAlwFieldB: UUID? = null
            @Column("genClNullA",  onInsert = Generated.OneTheClientWhenNull) var genClNullFieldA: Timestamp? = ts
            @Column("genClNullB",  onInsert = Generated.OneTheClientWhenNull) var genClNullFieldB: Timestamp? = null
        }

        class InsertStatementUnderTest(entityDef: Entity, getGoodConnection: () -> Connection? = { null }): InsertStatement(entityDef, getGoodConnection) {
            override fun run(conn: Connection): Int {
                return 0
            }
        }

        val txInst = Tx()
        var sql: String

        sql = InsertStatement(txInst).build()




    }
}