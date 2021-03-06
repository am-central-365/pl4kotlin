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

/**
 * One of the core library classes defining supported types and bind/read translations.
 */
public enum JdbcTypeCode {
      /**
       * Only used for values, Property types can not be Null.
       */
      Null,

      /** {@link java.lang.String}. Maps to/from CHAR, VARCHAR, TEXT, etc. */
      String,

      /** date + time + fraction. See Java doc for {@link java.sql.Timestamp} */
      Timestamp,

      /** {@link java.lang.Integer}. Maps to/from SQL int, number(*,0) - see JDBC docs */
      Integer,

      /** {@link java.lang.Long}. Maps to/from ,SQL int, number(*,0) - see JDBC docs */
      Long,

      /**
       * {@link java.util.UUID}
       *
       * Currently, the library is only supporting raw database types such as {@code raw(16)},
       * {@code binary(16)}, {@code bytea}, and similar.
       */
      UUID,

      /** The Kotlin/Java type is an Enum. The database can be MySQL Enum or a VARCHAR column */
      Enum

    , BigDecimal, Boolean, Byte, ByteArray, Date, Double, Float, Short, Time // less common
    , Array, Blob, Clob, NClob, Ref, Rowid, SQLXML, Reader, URL              // even less common
    , JsonStr          // custom type for database-side JSON represented on the client as String
    , Object;                                                                // the last resort


    @FunctionalInterface interface BindSetter { void   bind(PreparedStatement ps, int idx, Object val) throws SQLException; }
    @FunctionalInterface interface StrParser  { Object parse(String val) throws SQLException; }
    @FunctionalInterface interface RsReader   { Object read(ResultSet rs, int idx) throws SQLException; }
    @FunctionalInterface interface ToJsonConverter { String convert(Object val); }

    private static class Rec {
        final JdbcTypeCode jtc;
        final Type         javaType;
        final BindSetter   binder;      // Sets bind variable of PreparedStatement
        final RsReader     rsReader;    // Reads ResultSet value as Any
        final StrParser    strParser;   // Parses value from string, Used when handling REST parameters,
                                        // May be NULL if parsing of the value is not supported.
        final ToJsonConverter jsonValCvt;

        Rec(JdbcTypeCode jtc, Type type, BindSetter binder, StrParser strParser, RsReader rsReader, ToJsonConverter jsonValCvt)
          { this.jtc = jtc;  this.javaType = type;  this.binder = binder;
            this.strParser = strParser;  this.rsReader = rsReader;  this.jsonValCvt = jsonValCvt; }
    }

    static final Map<JdbcTypeCode, Rec> meta = new HashMap<>();
    static Map<Type, Rec> typeRecMap = new HashMap<>();

    private static String quotedJsonConverter(Object val) { return "\"" + val.toString() + "\""; }
    private static String plainJsonConverter(Object val) { return val.toString(); }

    private static void _ms(JdbcTypeCode jtc, Type type, BindSetter binder, RsReader rsReader, StrParser fromStrConverter, ToJsonConverter jsonCvt) {
        Rec rec = new Rec(jtc, type, binder, fromStrConverter, rsReader, jsonCvt);
        JdbcTypeCode.meta.put(jtc, rec);
        JdbcTypeCode.typeRecMap.put(type, rec);
    }

    private static void _ms(JdbcTypeCode jtc, Type type, BindSetter binder, RsReader rsReader, StrParser fromStrConverter) {
        JdbcTypeCode._ms(jtc, type, binder, rsReader, fromStrConverter, JdbcTypeCode::quotedJsonConverter);
    }

    static {
        // java.lang types
        _ms(JdbcTypeCode.String,    java.lang.String.class,   (ps, idx, val) -> ps.setString (idx, (java.lang.String) val), ResultSet::getString, val -> val);
        _ms(JdbcTypeCode.JsonStr,   java.lang.String.class,   (ps, idx, val) -> ps.setString (idx, (java.lang.String) val), ResultSet::getString, val -> val, JdbcTypeCode::plainJsonConverter);
        _ms(JdbcTypeCode.Short,     java.lang.Short.class,    (ps, idx, val) -> ps.setShort  (idx, (java.lang.Short)  val), ResultSet::getShort,    java.lang.Short::valueOf,   JdbcTypeCode::plainJsonConverter);
        _ms(JdbcTypeCode.Integer,   java.lang.Integer.class,  (ps, idx, val) -> ps.setInt    (idx, (java.lang.Integer)val), ResultSet::getInt,      java.lang.Integer::valueOf, JdbcTypeCode::plainJsonConverter);
        _ms(JdbcTypeCode.Long,      java.lang.Long.class,     (ps, idx, val) -> ps.setLong   (idx, (java.lang.Long)   val), ResultSet::getLong,     java.lang.Long::valueOf,    JdbcTypeCode::plainJsonConverter);
        _ms(JdbcTypeCode.Float,     java.lang.Float.class,    (ps, idx, val) -> ps.setFloat  (idx, (java.lang.Float)  val), ResultSet::getFloat,    java.lang.Float::valueOf,   JdbcTypeCode::plainJsonConverter);
        _ms(JdbcTypeCode.Double,    java.lang.Double.class,   (ps, idx, val) -> ps.setDouble (idx, (java.lang.Double) val), ResultSet::getDouble,   java.lang.Double::valueOf,  JdbcTypeCode::plainJsonConverter);
        _ms(JdbcTypeCode.Boolean,   java.lang.Boolean.class,  (ps, idx, val) -> ps.setBoolean(idx, (java.lang.Boolean)val), ResultSet::getBoolean,  java.lang.Boolean::valueOf, JdbcTypeCode::plainJsonConverter);
        _ms(JdbcTypeCode.Byte,      java.lang.Byte.class,     (ps, idx, val) -> ps.setByte   (idx, (java.lang.Byte)   val), ResultSet::getByte,     java.lang.Byte::valueOf);
        _ms(JdbcTypeCode.Enum,      java.lang.Enum.class,     (ps, idx, val) -> ps.setString (idx, val.toString()), null, null);  // read/parse are handled specially: conversion needs the real enum type

        // java.sql types
        _ms(JdbcTypeCode.Date,       java.sql.Date.class,       (ps, idx, val) -> ps.setDate     (idx, (java.sql.Date)     val), ResultSet::getDate,       java.sql.Date::valueOf);
        _ms(JdbcTypeCode.Time,       java.sql.Time.class,       (ps, idx, val) -> ps.setTime     (idx, (java.sql.Time)     val), ResultSet::getTime,       java.sql.Time::valueOf);
        _ms(JdbcTypeCode.Timestamp,  java.sql.Timestamp.class,  (ps, idx, val) -> ps.setTimestamp(idx, (java.sql.Timestamp)val), ResultSet::getTimestamp,  java.sql.Timestamp::valueOf);
        _ms(JdbcTypeCode.Array,      java.sql.Array.class,      (ps, idx, val) -> ps.setArray    (idx, (java.sql.Array)    val), ResultSet::getArray,      null);
        _ms(JdbcTypeCode.Blob,       java.sql.Blob.class,       (ps, idx, val) -> ps.setBlob     (idx, (java.sql.Blob)     val), ResultSet::getBlob, val -> new javax.sql.rowset.serial.SerialBlob(val.getBytes()));
        _ms(JdbcTypeCode.Clob,       java.sql.Clob.class,       (ps, idx, val) -> ps.setClob     (idx, (java.sql.Clob)     val), ResultSet::getClob, val -> val);
        _ms(JdbcTypeCode.NClob,      java.sql.NClob.class,      (ps, idx, val) -> ps.setNClob    (idx, (java.sql.NClob)    val), ResultSet::getNClob, val -> val);
        _ms(JdbcTypeCode.Ref,        java.sql.Ref.class,        (ps, idx, val) -> ps.setRef      (idx, (java.sql.Ref)      val), ResultSet::getRef,        null);
        _ms(JdbcTypeCode.Rowid,      java.sql.RowId.class,      (ps, idx, val) -> ps.setRowId    (idx, (java.sql.RowId)    val), ResultSet::getRowId,      null);
        _ms(JdbcTypeCode.SQLXML,     java.sql.SQLXML.class,     (ps, idx, val) -> ps.setSQLXML   (idx, (java.sql.SQLXML)   val), ResultSet::getSQLXML,     null);

        // misc types
        _ms(JdbcTypeCode.Null,       null,                  (ps, idx, val) -> ps.setNull           (idx, Types.VARCHAR),  null,  null, val -> "null");  //  can't have Null var type
        _ms(JdbcTypeCode.URL,        java.net.URL.class,    (ps, idx, val) -> ps.setURL            (idx, (java.net.URL)   val), ResultSet::getURL,              null);
        _ms(JdbcTypeCode.Reader,     java.io.Reader.class,  (ps, idx, val) -> ps.setCharacterStream(idx, (java.io.Reader) val), ResultSet::getCharacterStream,  null);

        // complex lambdas
        _ms(JdbcTypeCode.ByteArray,
            java.lang.Byte[].class,
            (ps, idx, val) -> ps.setBytes(idx, (byte[])val),
            ResultSet::getBytes,
            val -> new java.math.BigInteger(val,16).toByteArray()
        );

        _ms(JdbcTypeCode.BigDecimal,
            java.math.BigDecimal.class,
            (ps, idx, val) -> ps.setBigDecimal(idx, (java.math.BigDecimal) val),
            ResultSet::getBigDecimal,
            val -> new java.math.BigDecimal(val.replaceAll(",", ""))
        );

        _ms(JdbcTypeCode.UUID,
            java.util.UUID.class,
            (ps, idx, val) -> ps.setBytes(idx, JdbcTypeCode.uuidToBytes((java.util.UUID)val)),
            (rs, idx) -> JdbcTypeCode.uuidFromBytes(rs.getBytes(idx)),
            java.util.UUID::fromString
        );

        _ms(JdbcTypeCode.Object,
            java.lang.Object.class,
            (ps, idx, val) -> ps.setObject(idx, (java.lang.Object)val),
            ResultSet::getObject,
            null
        );

    }

    @SuppressWarnings("unchecked")
    static Object enumValFromStr(Type realEnumType, String enumStr) {
        return java.lang.Enum.valueOf((Class<? extends java.lang.Enum>)realEnumType, enumStr);
    }

    static BindSetter getBinder(JdbcTypeCode jtc) { return meta.get(jtc).binder; }
    static RsReader   getReader(JdbcTypeCode jtc) { return meta.get(jtc).rsReader; }
    static StrParser  getParser(JdbcTypeCode jtc) { return meta.get(jtc).strParser; }
    static ToJsonConverter getToJsonConverter(JdbcTypeCode jtc) { return meta.get(jtc).jsonValCvt; }

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


    public static UUID uuidFromBytes(byte[] bytes) {
        if( bytes == null )
            return null;
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long high = bb.getLong();
        long low = bb.getLong();
        return new UUID(high, low);
    }

    public static byte[] uuidToBytes(UUID uuid) {
        if( uuid == null )
            return null;
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

}
