package com.amcentral365.pl4kotlin;

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertEquals
import java.sql.Timestamp
import java.util.*

class JdbcTypeCodeTest {

    @Test
    fun translation() {
        @Table("tx")
        class Tx: Entity() {
            @Column("0", pkPos = 1) var pk: Int = 0
            @Column("1") var kshort:  Short = 1
            @Column("2") var jshort:  java.lang.Short = java.lang.Short(1)
            @Column("3") var kint:    Int = 1
            @Column("4") var jint:    java.lang.Integer = java.lang.Integer(1)
            @Column("5") var klong:   Long = 1
            @Column("6") var jlong:   java.lang.Long = java.lang.Long(1)
            @Column("7") var str:     String = ""
            @Column("8") var chr:     Char = 'x'
            @Column("9") var ts:      Timestamp = Timestamp(1)
            @Column("a") var uuid:    UUID = UUID.randomUUID()
            @Column("b") var enm:     JdbcTypeCode = JdbcTypeCode.Null
            @Column("c") var kbyte:   Byte = 2
            @Column("d") var kbytes:  ByteArray = byteArrayOf(3)
            @Column("e") var jbyte:   java.lang.Byte  = java.lang.Byte(2)
          //@Column("f") var jbytes:  java.lang.Byte[]   How to init it?
        }

        val tx = Tx()
        assertNotNull(tx.colDefs?.size)

        fun ensure(fieldName: String, jtc: JdbcTypeCode) =
                assertEquals(jtc, tx.colDefs!!.first { it.fieldName == fieldName }.fieldType)
        ensure("kshort",  JdbcTypeCode.Short)
        ensure("jshort",  JdbcTypeCode.Short)
        ensure("kint",    JdbcTypeCode.Integer)
        ensure("jint",    JdbcTypeCode.Integer)
        ensure("klong",   JdbcTypeCode.Long)
        ensure("jlong",   JdbcTypeCode.Long)
        ensure("str",     JdbcTypeCode.String)
        ensure("chr",     JdbcTypeCode.String)
        ensure("ts",      JdbcTypeCode.Timestamp)
        ensure("uuid",    JdbcTypeCode.UUID)
        ensure("enm",     JdbcTypeCode.Enum)
        ensure("kbyte",   JdbcTypeCode.Byte)
        ensure("kbytes",  JdbcTypeCode.ByteArray)
        ensure("jbyte",   JdbcTypeCode.Byte)

    }

}