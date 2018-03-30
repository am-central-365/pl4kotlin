package com.amcentral365.pl4kotlin

import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KType


private fun _k2j(kc: KType): JdbcTypeCode {
    // FIXME: This is ugly, but I couldn't figure a better way to check if type is kotlin.Int
    return when(kc.toString()) {
        "kotlin.Int"    -> JdbcTypeCode.Integer
        "kotlin.Long"   -> JdbcTypeCode.Long
        "kotlin.String" -> JdbcTypeCode.String

        "kotlin.Char"   -> JdbcTypeCode.String
        "kotlin.Short"  -> JdbcTypeCode.Short

        else -> JdbcTypeCode.from(kc.javaClass)
    }
}


fun JTC(kp: KProperty<*>?): JdbcTypeCode = if( kp == null ) JdbcTypeCode.Null else _k2j(kp.returnType)
//fun JTC(kc: KClass<*>?):    JdbcTypeCode = if( kc == null ) JdbcTypeCode.Null else JdbcTypeCode.from(kc::class.java)

/**
 * Provides mapping of Java and Kotlin types to/from JDBC
 */
/*
internal enum class JdbcTypeCodeX {
    Null, String, Timestamp, Integer, Long, UUID, Enum                     // most common
  , BigDecimal, Boolean, Byte, ByteArray, Date, Double, Float, Short, Time // less common
  , Array, Blob, Reader, Clob, NClob, Ref, Rowid, SQLXML, URL              // even less common
  , Object                                                                 // the last resort
    ;

    companion object {

        fun from(kp: KProperty<*>?): JdbcTypeCode = if( kp == null ) JdbcTypeCode.Null else from(kp.returnType::class.java)
      //fun from(ka: kotlin.Any?):   JdbcTypeCode = if( ka == null ) JdbcTypeCode.Null else from(ka::class.java)

        fun from(kp: java.lang.Class<*>?): JdbcTypeCode {
            if( kp == null )
                return JdbcTypeCode.Null

            return when(kp) {
                kotlin.String::class                       -> JdbcTypeCode.String
                java.sql.Timestamp::class                  -> JdbcTypeCode.Timestamp
                kotlin.Int::class,  java.lang.Integer.TYPE -> JdbcTypeCode.Integer
                kotlin.Long::class, java.lang.Long.TYPE    -> JdbcTypeCode.Long

                java.util.UUID  -> JdbcTypeCode.UUID
                java.lang.Enum::class.java  -> JdbcTypeCode.Enum

                java.math.BigDecimal::class.java -> JdbcTypeCode.BigDecimal
                kotlin.Boolean::class    -> JdbcTypeCode.Boolean
                kotlin.Byte::class       -> JdbcTypeCode.Byte
                ByteArray::class  -> JdbcTypeCode.ByteArray
                Date::class       -> JdbcTypeCode.Date
                kotlin.Double::class     -> JdbcTypeCode.Double
                kotlin.Float::class      -> JdbcTypeCode.Float
                kotlin.Short::class      -> JdbcTypeCode.Short
                Time::class       -> JdbcTypeCode.Time

                Array::class      -> JdbcTypeCode.Array
                Blob::class       -> JdbcTypeCode.Blob
                Reader::class     -> JdbcTypeCode.Reader
                Clob::class       -> JdbcTypeCode.Clob
                NClob::class      -> JdbcTypeCode.NClob
                Ref::class        -> JdbcTypeCode.Ref
                Rowid::class      -> JdbcTypeCode.Rowid
                SQLXML::class     -> JdbcTypeCode.SQLXML
                URL::class        -> JdbcTypeCode.URL

                else -> JdbcTypeCode.Object
            }
        }

        fun clazz(jtc: JdbcTypeCode?): KClass<*>? {
            if( jtc == null )
                return null

            return when(jtc) {
                JdbcTypeCode.String -> kotlin.String::class
            }
        }
    }
}
*/