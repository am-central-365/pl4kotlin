package com.amcentral365.pl4kotlin;

import com.google.common.collect.ImmutableBiMap;
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
    @FunctionalInterface private interface FromString { Object convert(String val) throws SQLException; }
    @FunctionalInterface private interface RsAssigner { void   assign(Entity.ColDef colDef, int idx, ResultSet rs) throws SQLException; }

    private static class Rec {
        JdbcTypeCode jtc;
        Type         javaType;
        BindSetter   binder;
        FromString   fromStr;
        RsAssigner   rsAssigner;

        Rec(JdbcTypeCode jtc, Type type, BindSetter binder, FromString fromStrConverter, RsAssigner rsAssigner)
          { this.jtc = jtc;  this.javaType = type;  this.binder = binder;  this.fromStr = fromStrConverter;  this.rsAssigner = rsAssigner; }
    }

    private static Map<JdbcTypeCode, Rec> meta = new HashMap<>();
    private static Map<Type, Rec> typeRecMap = new HashMap<>();


    private static void _ms(JdbcTypeCode jtc, Type type, BindSetter binder, FromString fromStrConverter, RsAssigner assigner) {
        Rec rec = new Rec(jtc, type, binder, fromStrConverter, assigner);
        JdbcTypeCode.meta.put(jtc, rec);
        JdbcTypeCode.typeRecMap.put(type, rec);
    }

    static {
        _ms(JdbcTypeCode.Null,null, (ps, idx, val) -> ps.setNull(idx, Types.VARCHAR),null, null);  //  can't have Null var type

        // java.lang types
        _ms(JdbcTypeCode.String,     java.lang.String.class,     (ps, idx, val) -> ps.setString    (idx, (java.lang.String)  val),  val -> val,                 (colDef, idx, rs) -> colDef.setVal(rs.getString(idx)));
        _ms(JdbcTypeCode.Short,      java.lang.Short.class,      (ps, idx, val) -> ps.setShort     (idx, (java.lang.Short)   val),  java.lang.Short::valueOf,   (colDef, idx, rs) -> colDef.setVal(rs.getShort(idx)));
        _ms(JdbcTypeCode.Integer,    java.lang.Integer.class,    (ps, idx, val) -> ps.setInt       (idx, (java.lang.Integer) val),  java.lang.Integer::valueOf, (colDef, idx, rs) -> colDef.setVal(rs.getInt(idx)));
        _ms(JdbcTypeCode.Long,       java.lang.Long.class,       (ps, idx, val) -> ps.setLong      (idx, (java.lang.Long)    val),  java.lang.Long::valueOf,    (colDef, idx, rs) -> colDef.setVal(rs.getLong(idx)));
        _ms(JdbcTypeCode.Float,      java.lang.Float.class,      (ps, idx, val) -> ps.setFloat     (idx, (java.lang.Float)   val),  java.lang.Float::valueOf,   (colDef, idx, rs) -> colDef.setVal(rs.getFloat(idx)));
        _ms(JdbcTypeCode.Double,     java.lang.Double.class,     (ps, idx, val) -> ps.setDouble    (idx, (java.lang.Double)  val),  java.lang.Double::valueOf,  (colDef, idx, rs) -> colDef.setVal(rs.getDouble(idx)));
        _ms(JdbcTypeCode.Boolean,    java.lang.Boolean.class,    (ps, idx, val) -> ps.setBoolean   (idx, (java.lang.Boolean) val),  java.lang.Boolean::valueOf, (colDef, idx, rs) -> colDef.setVal(rs.getBoolean(idx)));
        _ms(JdbcTypeCode.Byte,       java.lang.Byte.class,       (ps, idx, val) -> ps.setByte      (idx, (java.lang.Byte)    val),  java.lang.Byte::valueOf,    (colDef, idx, rs) -> colDef.setVal(rs.getByte(idx)));

        // java.sql types
        _ms(JdbcTypeCode.Date,       java.sql.Date.class,        (ps, idx, val) -> ps.setDate      (idx, (java.sql.Date)     val),  java.sql.Date::valueOf,     (colDef, idx, rs) -> colDef.setVal(rs.getDate(idx)));
        _ms(JdbcTypeCode.Time,       java.sql.Time.class,        (ps, idx, val) -> ps.setTime      (idx, (java.sql.Time)     val),  java.sql.Time::valueOf,     (colDef, idx, rs) -> colDef.setVal(rs.getTime(idx)));
        _ms(JdbcTypeCode.Timestamp,  java.sql.Timestamp.class,   (ps, idx, val) -> ps.setTimestamp (idx, (java.sql.Timestamp)val),  java.sql.Timestamp::valueOf,(colDef, idx, rs) -> colDef.setVal(rs.getTimestamp(idx)));
        _ms(JdbcTypeCode.Array,      java.sql.Array.class,       (ps, idx, val) -> ps.setArray     (idx, (java.sql.Array)    val),  null,                       (colDef, idx, rs) -> colDef.setVal(rs.getArray(idx)));
        _ms(JdbcTypeCode.Blob,       java.sql.Blob.class,        (ps, idx, val) -> ps.setBlob      (idx, (java.sql.Blob)     val),  val -> new javax.sql.rowset.serial.SerialBlob(val.getBytes()), (colDef, idx, rs) -> colDef.setVal(rs.getBlob(idx)));
        _ms(JdbcTypeCode.Clob,       java.sql.Clob.class,        (ps, idx, val) -> ps.setClob      (idx, (java.sql.Clob)     val),  val -> val,                 (colDef, idx, rs) -> colDef.setVal(rs.getClob(idx)));
        _ms(JdbcTypeCode.NClob,      java.sql.NClob.class,       (ps, idx, val) -> ps.setNClob     (idx, (java.sql.NClob)    val),  val -> val,                 (colDef, idx, rs) -> colDef.setVal(rs.getNClob(idx)));
        _ms(JdbcTypeCode.Ref,        java.sql.Ref.class,         (ps, idx, val) -> ps.setRef       (idx, (java.sql.Ref)      val),  null,                       (colDef, idx, rs) -> colDef.setVal(rs.getRef(idx)));
        _ms(JdbcTypeCode.Rowid,      java.sql.RowId.class,       (ps, idx, val) -> ps.setRowId     (idx, (java.sql.RowId)    val),  null,                       (colDef, idx, rs) -> colDef.setVal(rs.getRowId(idx)));
        _ms(JdbcTypeCode.SQLXML,     java.sql.SQLXML.class,      (ps, idx, val) -> ps.setSQLXML    (idx, (java.sql.SQLXML)   val),  null,                       (colDef, idx, rs) -> colDef.setVal(rs.getSQLXML(idx)));

        // misc types
        _ms(JdbcTypeCode.URL,        java.net.URL.class,     (ps, idx, val) -> ps.setURL                      (idx, (java.net.URL)   val),  null,                    (colDef, idx, rs) -> colDef.setVal(rs.getURL(idx)));
        _ms(JdbcTypeCode.Reader,     java.io.Reader.class,   (ps, idx, val) -> ps.setCharacterStream          (idx, (java.io.Reader) val),  null,                    (colDef, idx, rs) -> colDef.setVal(rs.getCharacterStream(idx)));
        _ms(JdbcTypeCode.ByteArray,  java.lang.Byte[].class, (ps, idx, val) -> ps.setBytes(idx, (byte[])val), val -> new java.math.BigInteger(val,16).toByteArray(), (colDef, idx, rs) -> colDef.setVal(rs.getBytes(idx)));

        // complex lambdas
        _ms(JdbcTypeCode.Enum,  // FIXME: looks like we need to keep the real Enum type
            java.lang.Enum.class,
            (ps, idx, val) -> ps.setString(idx, val.toString()),
            null, //val -> java.lang.Enum.valueOf((Class<? extends java.lang.Enum>)colDef.javaType, val), // colDef.fieldType must be the real enum
            (colDef, idx, rs) -> colDef.setVal(java.lang.Enum.valueOf((Class<? extends java.lang.Enum>)colDef.getJavaType(), rs.getString(idx)))
        );


        _ms(JdbcTypeCode.BigDecimal,
            java.math.BigDecimal.class,
            (ps, idx, val) -> ps.setBigDecimal(idx, (java.math.BigDecimal) val),
            val -> new java.math.BigDecimal(val.replaceAll(",", "")),
            (colDef, idx, rs) -> colDef.setVal(rs.getBigDecimal(idx))
        );

        _ms(JdbcTypeCode.UUID,
                java.util.UUID.class,
                (ps, idx, val) -> ps.setBytes(idx, JdbcTypeCode.uuidToBytes((java.util.UUID)val)),
                java.util.UUID::fromString,
                (colDef, idx, rs) -> colDef.setVal(JdbcTypeCode.uuidFromBytes(rs.getBytes(idx)))
        );

    }


    static BindSetter getBinder(JdbcTypeCode jtc) { return meta.get(jtc).binder; }

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
