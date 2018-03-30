package com.amcentral365.pl4kotlin;

public enum JdbcTypeCode {
      Null
    , String, Timestamp, Integer, Long, UUID, Enum                           // most common
    , BigDecimal, Boolean, Byte, ByteArray, Date, Double, Float, Short, Time // less common
    , Array, Blob, Reader, Clob, NClob, Ref, Rowid, SQLXML, URL              // even less common
    , Object;                                                                // the last resort

    static JdbcTypeCode from(Class<?> cz) {
        if( cz == null                     ) return JdbcTypeCode.Null;
        if( cz == java.lang.String.class   ) return JdbcTypeCode.String;
        if( cz == java.sql.Timestamp.class ) return JdbcTypeCode.Timestamp;
        if( cz == java.lang.Integer.class  ) return JdbcTypeCode.Integer;
        if( cz == java.lang.Long.class     ) return JdbcTypeCode.Long;
        if( cz == java.util.UUID.class     ) return JdbcTypeCode.UUID;
        if( cz == java.lang.Enum.class     ) return JdbcTypeCode.Enum;

        // TODO: add less common types

        return JdbcTypeCode.Object;
    }


    static Class<?> clazz(JdbcTypeCode jtc) {
        if( jtc == null )
            return null;

        switch( jtc ) {
            case Null:      return null;
            case String:    return java.lang.String.class;
            case Timestamp: return java.sql.Timestamp.class;
            case Integer:   return java.lang.Integer.class;
            case Long:      return java.lang.Long.class;
            case UUID:      return java.util.UUID.class;
            case Enum:      return java.lang.Enum.class;

            // TODO: add less common types

            default:
                return java.lang.Object.class;
        }
    }

}
