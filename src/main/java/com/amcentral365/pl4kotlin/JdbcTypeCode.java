package com.amcentral365.pl4kotlin;

import com.google.common.collect.ImmutableBiMap;
import org.jetbrains.annotations.Contract;


import java.lang.reflect.Type;


public enum JdbcTypeCode {
      Null
    , String, Timestamp, Integer, Long, UUID, Enum                           // most common
    , BigDecimal, Boolean, Byte, ByteArray, Date, Double, Float, Short, Time // less common
    , Array, Blob, Clob, NClob, Ref, Rowid, SQLXML, Reader, URL              // even less common
    , Object;                                                                // the last resort

    private static ImmutableBiMap<JdbcTypeCode, Type> jtcTypeMap = new ImmutableBiMap.Builder<JdbcTypeCode, Type>()
            .put(JdbcTypeCode.String,     java.lang.String.class)
            .put(JdbcTypeCode.Short,      java.lang.Short.class)
            .put(JdbcTypeCode.Integer,    java.lang.Integer.class)
            .put(JdbcTypeCode.Long,       java.lang.Long.class)
            .put(JdbcTypeCode.Float,      java.lang.Float.class)
            .put(JdbcTypeCode.Double,     java.lang.Double.class)
            .put(JdbcTypeCode.Boolean,    java.lang.Boolean.class)
            .put(JdbcTypeCode.Byte,       java.lang.Byte.class)
            .put(JdbcTypeCode.ByteArray,  java.lang.Byte[].class)
            .put(JdbcTypeCode.Enum,       java.lang.Enum.class)

            .put(JdbcTypeCode.Date,       java.sql.Date.class)
            .put(JdbcTypeCode.Time,       java.sql.Time.class)
            .put(JdbcTypeCode.Timestamp,  java.sql.Timestamp.class)
            .put(JdbcTypeCode.Array,      java.sql.Array.class)
            .put(JdbcTypeCode.Blob,       java.sql.Blob.class)
            .put(JdbcTypeCode.Clob,       java.sql.Clob.class)
            .put(JdbcTypeCode.NClob,      java.sql.NClob.class)
            .put(JdbcTypeCode.Ref,        java.sql.Ref.class)
            .put(JdbcTypeCode.Rowid,      java.sql.RowId.class)
            .put(JdbcTypeCode.SQLXML,     java.sql.SQLXML.class)

            .put(JdbcTypeCode.UUID,       java.util.UUID.class)
            .put(JdbcTypeCode.BigDecimal, java.math.BigDecimal.class)
            .put(JdbcTypeCode.URL,        java.net.URL.class)
            .put(JdbcTypeCode.Reader,     java.io.Reader.class)
        .build()
    ;

    static JdbcTypeCode from(Type cz) {
        return cz == null           ? JdbcTypeCode.Null
             : ((Class)cz).isEnum() ? JdbcTypeCode.Enum
             : jtcTypeMap.inverse().getOrDefault(cz, JdbcTypeCode.Object)
        ;
    }

    @Contract("null -> null")
    static Class<?> clazz(JdbcTypeCode jtc) {
        return jtc == null ? null
             : (Class<?>)jtcTypeMap.getOrDefault(jtc, java.lang.Object.class)
        ;
    }

}
