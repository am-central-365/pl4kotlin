@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package com.amcentral365.pl4kotlin

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertEquals
import java.sql.Timestamp
import java.util.UUID

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
          //@Column("?") var jbytes:  java.lang.Byte[]   How to init it?
            @Column("f") var kfloat:  Float = 1f
            @Column("g") var jfloat:  java.lang.Float = java.lang.Float(1f)
            @Column("h") var kdouble: Double = 1.0
            @Column("i") var jdouble: java.lang.Double = java.lang.Double(1.0)
            @Column("j") var bigdc:   java.math.BigDecimal = java.math.BigDecimal(1.0)
            @Column("k") var bool:    java.lang.Boolean = java.lang.Boolean(true)
            @Column("l") var date:    java.sql.Date = java.sql.Date(4)
            @Column("m") var time:    java.sql.Time = java.sql.Time(7)
            @Column("n") var arr:     java.sql.Array? = null
            @Column("o") var blob:    java.sql.Blob? = null
            @Column("p") var clob:    java.sql.Clob? = null
            @Column("q") var nclob:   java.sql.NClob? = null
            @Column("r") var reff:    java.sql.Ref? = null
            @Column("s") var rowid:   java.sql.RowId? = null
            @Column("t") var sqlxml:  java.sql.SQLXML? = null
            @Column("u") var reader:  java.io.Reader? = null
            @Column("v") var url:     java.net.URL = java.net.URL("http://host")

            @Column("z") var obj:     java.lang.Object? = null
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
        ensure("kfloat",  JdbcTypeCode.Float)
        ensure("jfloat",  JdbcTypeCode.Float)
        ensure("kdouble", JdbcTypeCode.Double)
        ensure("jdouble", JdbcTypeCode.Double)
        ensure("str",     JdbcTypeCode.String)
        ensure("chr",     JdbcTypeCode.String)
        ensure("ts",      JdbcTypeCode.Timestamp)
        ensure("uuid",    JdbcTypeCode.UUID)
        ensure("enm",     JdbcTypeCode.Enum)
        ensure("kbyte",   JdbcTypeCode.Byte)
        ensure("kbytes",  JdbcTypeCode.ByteArray)
        ensure("jbyte",   JdbcTypeCode.Byte)
        ensure("bigdc",   JdbcTypeCode.BigDecimal)
        ensure("bool",    JdbcTypeCode.Boolean)
        ensure("date",    JdbcTypeCode.Date)
        ensure("time",    JdbcTypeCode.Time)
        ensure("arr",     JdbcTypeCode.Array)
        ensure("blob",    JdbcTypeCode.Blob)
        ensure("clob",    JdbcTypeCode.Clob)
        ensure("nclob",   JdbcTypeCode.NClob)
        ensure("reff",    JdbcTypeCode.Ref)
        ensure("rowid",   JdbcTypeCode.Rowid)
        ensure("sqlxml",  JdbcTypeCode.SQLXML)
        ensure("reader",  JdbcTypeCode.Reader)
        ensure("url",     JdbcTypeCode.URL)
        ensure("obj",     JdbcTypeCode.Object)
    }

}