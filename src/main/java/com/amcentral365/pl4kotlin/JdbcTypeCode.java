package com.amcentral365.pl4kotlin;

import org.jetbrains.annotations.Contract;


import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public enum JdbcTypeCode {
      Null
    , String, Timestamp, Integer, Long, UUID, Enum                           // most common
    , BigDecimal, Boolean, Byte, ByteArray, Date, Double, Float, Short, Time // less common
    , Array, Blob, Clob, NClob, Ref, Rowid, SQLXML, Reader, URL              // even less common
    , Object;                                                                // the last resort


    @FunctionalInterface interface BindSetter { void   bind(PreparedStatement ps, int idx, Object val) throws SQLException; }
    @FunctionalInterface interface StrParser  { Object parse(String val) throws SQLException; }
    @FunctionalInterface interface RsReader   { Object read(ResultSet rs, int idx) throws SQLException; }

    private static class Rec {
        JdbcTypeCode jtc;
        Type         javaType;
        BindSetter   binder;      // Sets bind variable of PreparedStatement
        RsReader     rsReader;    // Reads ResultSet value as Any
        StrParser    strParser;   // Parses value from string, Used when handling REST parameters,
                                  // May be NULL if parsing of the value is not supported.

        Rec(JdbcTypeCode jtc, Type type, BindSetter binder, StrParser strParser, RsReader rsReader)
          { this.jtc = jtc;  this.javaType = type;  this.binder = binder;  this.strParser = strParser;  this.rsReader = rsReader; }
    }

    static Map<JdbcTypeCode, Rec> meta = new HashMap<>();
    static Map<Type, Rec> typeRecMap = new HashMap<>();


    private static void _ms(JdbcTypeCode jtc, Type type, BindSetter binder, RsReader rsReader, StrParser fromStrConverter) {
        Rec rec = new Rec(jtc, type, binder, fromStrConverter, rsReader);
        JdbcTypeCode.meta.put(jtc, rec);
        JdbcTypeCode.typeRecMap.put(type, rec);
    }

    static {
        // java.lang types
        _ms(JdbcTypeCode.String,    java.lang.String.class,   (ps, idx, val) -> ps.setString (idx, (java.lang.String) val), (rs, idx) -> rs.getString(idx), val -> val);
        _ms(JdbcTypeCode.Short,     java.lang.Short.class,    (ps, idx, val) -> ps.setShort  (idx, (java.lang.Short)  val), (rs, idx) -> rs.getShort(idx),    java.lang.Short::valueOf);
        _ms(JdbcTypeCode.Integer,   java.lang.Integer.class,  (ps, idx, val) -> ps.setInt    (idx, (java.lang.Integer)val), (rs, idx) -> rs.getInt(idx),      java.lang.Integer::valueOf);
        _ms(JdbcTypeCode.Long,      java.lang.Long.class,     (ps, idx, val) -> ps.setLong   (idx, (java.lang.Long)   val), (rs, idx) -> rs.getLong(idx),     java.lang.Long::valueOf);
        _ms(JdbcTypeCode.Float,     java.lang.Float.class,    (ps, idx, val) -> ps.setFloat  (idx, (java.lang.Float)  val), (rs, idx) -> rs.getFloat(idx),    java.lang.Float::valueOf);
        _ms(JdbcTypeCode.Double,    java.lang.Double.class,   (ps, idx, val) -> ps.setDouble (idx, (java.lang.Double) val), (rs, idx) -> rs.getDouble(idx),   java.lang.Double::valueOf);
        _ms(JdbcTypeCode.Boolean,   java.lang.Boolean.class,  (ps, idx, val) -> ps.setBoolean(idx, (java.lang.Boolean)val), (rs, idx) -> rs.getBoolean(idx),  java.lang.Boolean::valueOf);
        _ms(JdbcTypeCode.Byte,      java.lang.Byte.class,     (ps, idx, val) -> ps.setByte   (idx, (java.lang.Byte)   val), (rs, idx) -> rs.getByte(idx),     java.lang.Byte::valueOf);
        _ms(JdbcTypeCode.Enum,      java.lang.Enum.class,     (ps, idx, val) -> ps.setString (idx, val.toString()), null, null);  // read/parse are handled specially: conversion needs the real enum type

        // java.sql types
        _ms(JdbcTypeCode.Date,       java.sql.Date.class,       (ps, idx, val) -> ps.setDate     (idx, (java.sql.Date)     val), (rs, idx) -> rs.getDate(idx),       java.sql.Date::valueOf);
        _ms(JdbcTypeCode.Time,       java.sql.Time.class,       (ps, idx, val) -> ps.setTime     (idx, (java.sql.Time)     val), (rs, idx) -> rs.getTime(idx),       java.sql.Time::valueOf);
        _ms(JdbcTypeCode.Timestamp,  java.sql.Timestamp.class,  (ps, idx, val) -> ps.setTimestamp(idx, (java.sql.Timestamp)val), (rs, idx) -> rs.getTimestamp(idx),  java.sql.Timestamp::valueOf);
        _ms(JdbcTypeCode.Array,      java.sql.Array.class,      (ps, idx, val) -> ps.setArray    (idx, (java.sql.Array)    val), (rs, idx) -> rs.getArray(idx),      null);
        _ms(JdbcTypeCode.Blob,       java.sql.Blob.class,       (ps, idx, val) -> ps.setBlob     (idx, (java.sql.Blob)     val), (rs, idx) -> rs.getBlob(idx), val -> new javax.sql.rowset.serial.SerialBlob(val.getBytes()));
        _ms(JdbcTypeCode.Clob,       java.sql.Clob.class,       (ps, idx, val) -> ps.setClob     (idx, (java.sql.Clob)     val), (rs, idx) -> rs.getClob(idx), val -> val);
        _ms(JdbcTypeCode.NClob,      java.sql.NClob.class,      (ps, idx, val) -> ps.setNClob    (idx, (java.sql.NClob)    val), (rs, idx) -> rs.getNClob(idx), val -> val);
        _ms(JdbcTypeCode.Ref,        java.sql.Ref.class,        (ps, idx, val) -> ps.setRef      (idx, (java.sql.Ref)      val), (rs, idx) -> rs.getRef(idx),        null);
        _ms(JdbcTypeCode.Rowid,      java.sql.RowId.class,      (ps, idx, val) -> ps.setRowId    (idx, (java.sql.RowId)    val), (rs, idx) -> rs.getRowId(idx),      null);
        _ms(JdbcTypeCode.SQLXML,     java.sql.SQLXML.class,     (ps, idx, val) -> ps.setSQLXML   (idx, (java.sql.SQLXML)   val), (rs, idx) -> rs.getSQLXML(idx),     null);

        // misc types
        _ms(JdbcTypeCode.Null,       null,                  (ps, idx, val) -> ps.setNull           (idx, Types.VARCHAR),  null,  null);  //  can't have Null var type
        _ms(JdbcTypeCode.URL,        java.net.URL.class,    (ps, idx, val) -> ps.setURL            (idx, (java.net.URL)   val), (rs, idx) -> rs.getURL(idx),              null);
        _ms(JdbcTypeCode.Reader,     java.io.Reader.class,  (ps, idx, val) -> ps.setCharacterStream(idx, (java.io.Reader) val), (rs, idx) -> rs.getCharacterStream(idx),  null);

        // complex lambdas
        _ms(JdbcTypeCode.ByteArray,
            java.lang.Byte[].class,
            (ps, idx, val) -> ps.setBytes(idx, (byte[])val),
            (rs, idx) -> rs.getBytes(idx),
            val -> new java.math.BigInteger(val,16).toByteArray()
        );

        _ms(JdbcTypeCode.BigDecimal,
            java.math.BigDecimal.class,
            (ps, idx, val) -> ps.setBigDecimal(idx, (java.math.BigDecimal) val),
            (rs, idx) -> rs.getBigDecimal(idx),
            val -> new java.math.BigDecimal(val.replaceAll(",", ""))
        );

        _ms(JdbcTypeCode.UUID,
            java.util.UUID.class,
            (ps, idx, val) -> ps.setBytes(idx, JdbcTypeCode.uuidToBytes((java.util.UUID)val)),
            (rs, idx) -> JdbcTypeCode.uuidFromBytes(rs.getBytes(idx)),
            java.util.UUID::fromString
        );

    }

    static Object enumValFromStr(Type realEnumType, String enumStr) {
        return java.lang.Enum.valueOf((Class<? extends java.lang.Enum>)realEnumType, enumStr);
    }

    static BindSetter getBinder(JdbcTypeCode jtc) { return meta.get(jtc).binder; }
    static RsReader   getReader(JdbcTypeCode jtc) { return meta.get(jtc).rsReader; }
    static StrParser  getParser(JdbcTypeCode jtc) { return meta.get(jtc).strParser; }



    /*@Deprecated
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
    ;*/

    static JdbcTypeCode from(Type cz) {
        return cz == null           ? JdbcTypeCode.Null
             : ((Class)cz).isEnum() ? JdbcTypeCode.Enum
             : typeRecMap.containsKey(cz) ? typeRecMap.get(cz).jtc
             : JdbcTypeCode.Object
        ;
    }

    @Contract("null -> null")
    static Class<?> clazz(JdbcTypeCode jtc) {
        return jtc == null ? null
             : meta.containsKey(jtc) ? (Class<?>)meta.get(jtc).javaType
             : java.lang.Object.class
        ;
    }


    private static UUID uuidFromBytes(byte[] bytes) {
        if( bytes == null )
            return null;
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long high = bb.getLong();
        long low = bb.getLong();
        return new UUID(high, low);
    }

    private static byte[] uuidToBytes(UUID uuid) {
        if( uuid == null )
            return null;
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

}
