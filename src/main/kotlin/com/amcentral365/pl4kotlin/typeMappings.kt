package com.amcentral365.pl4kotlin

import mu.KotlinLogging
import org.jetbrains.annotations.Contract
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.jvmErasure

private val logger = KotlinLogging.logger {}

private fun k2j(kc: KType): JdbcTypeCode {
    return when(kc.jvmErasure) {
        kotlin.Short::class     -> JdbcTypeCode.Short
        kotlin.Int::class       -> JdbcTypeCode.Integer
        kotlin.Long::class      -> JdbcTypeCode.Long
        kotlin.Float::class     -> JdbcTypeCode.Float
        kotlin.Double::class    -> JdbcTypeCode.Double
        kotlin.String::class    -> JdbcTypeCode.String
        kotlin.Char::class      -> JdbcTypeCode.String
        kotlin.Byte::class      -> JdbcTypeCode.Byte
        kotlin.ByteArray::class -> JdbcTypeCode.ByteArray
        kotlin.Boolean::class   -> JdbcTypeCode.Boolean

        else -> JdbcTypeCode.from(kc.javaType)
    }
}

/** Given a property [kp], determine its [JdbcTypeCode]. The function processes Kotlin and Java types. */
@Contract("null -> null")
fun JTC(kp: KProperty<*>?): JdbcTypeCode = if( kp == null ) JdbcTypeCode.Null else k2j(kp.returnType)
//fun JTC(kc: KClass<*>?):    JdbcTypeCode = if( kc == null ) JdbcTypeCode.Null else JdbcTypeCode.from(kc::class.java)




//fun uuidToBytes(uuid: UUID): ByteArray {}
//fun uuidFromBytes(bytes: ByteArray): UUID {}

/** run `close()` on [c], ignoring any errors. It is safe to pass `null` to the function */
fun closeIfCan(c: AutoCloseable?) {
    if (c != null)
        try {
            c.close()
        } catch (e: Exception) {
            logger.warn("closeIfCan of ${c::class.qualifiedName}: ${e::class.qualifiedName} ${e.message}")
        }
}
