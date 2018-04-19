package com.amcentral365.pl4kotlin.integrationTests

import com.amcentral365.pl4kotlin.Column
import com.amcentral365.pl4kotlin.Entity
import com.amcentral365.pl4kotlin.Generated
import com.amcentral365.pl4kotlin.Table
import java.math.BigDecimal
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Table("test_tbl")
class TestTbl: Entity() {
    enum class GreekLetters { alpha, beta, gamma, delta, epsilon }

    companion object {
        const val KNOWN_PK1: Int   = -451247
        const val KNOWN_PK2: Short =  10221
        const val KNOWN_VC     = "Yea, from the table of my memory\nI’ll wipe away all trivial, fond records,"
        const val KNOWN_CHAR   = " a .b "
        const val KNOWN_FLOAT  = -342.74236e14f
        const val KNOWN_DOUBLE = Math.PI
        const val KNOWN_BITS   = (1L shl 15) or (1L shl 9) or (1L shl 8) or (1L shl 2)

        val KNOWN_DATE  = Date.valueOf(LocalDate.now())
        val KNOWN_TIME  = Time.valueOf(LocalTime.now())
        val KNOWN_NUM   = BigDecimal.valueOf(Math.E)
        val KNOWN_UUID1 = UUID.randomUUID()
        val KNOWN_UUID2 = UUID.randomUUID()
    }

    @Column("pk1", pkPos = 1, onInsert = Generated.OnTheDbAlways)        var pk1:   Int    = TestTbl.KNOWN_PK1
    @Column("pk2", pkPos = 2, onInsert = Generated.OnTheDbWhenNull)      var pk2:   Short? = TestTbl.KNOWN_PK2
    @Column("uuid1",          onInsert = Generated.OnTheClientAlways)    var uuid1: UUID?  = TestTbl.KNOWN_UUID1
    @Column("uuid2",          onInsert = Generated.OneTheClientWhenNull) var uuid2: UUID?  = TestTbl.KNOWN_UUID2

    @Column("created_ts",  onInsert = Generated.OnTheDbAlways)                          var created:  Timestamp? = null
    @Column("modified_ts", onInsert = Generated.OnTheDbAlways, isOptimisticLock = true) var modified: Timestamp? = null

    @Column("vc_col")               var vcVal:     String?       = TestTbl.KNOWN_VC
    @Column("char_col")             var charVal:   String?       = TestTbl.KNOWN_CHAR
    @Column("date_col")             var dateVal:   Date?         = TestTbl.KNOWN_DATE
    @Column("time_col")             var timweVal:  Time?         = TestTbl.KNOWN_TIME
    @Column("num_col")              var numVal:    BigDecimal?   = TestTbl.KNOWN_NUM
    @Column("float_col")            var floatVal:  Float?        = TestTbl.KNOWN_FLOAT
    @Column("double_col")           var doubleVal: Double?       = TestTbl.KNOWN_DOUBLE
    @Column("bit17_val")            var bit17Val:  Long?         = TestTbl.KNOWN_BITS
    @Column("bool_col")             var boolVal:   Boolean?      = null
    @Column("enum_col")             var enumVal:   GreekLetters? = null

    var transVal1 = 25
    var transVal2 = "twenty five"

}