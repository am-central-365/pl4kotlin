package com.amcentral365.pl4kotlin

/**
 * Annotation, denoting the class as a Persistence Layer object
 *
 * @param tableName The database table name. It is emitted to all generated SQL statements.
 *                  Can't be empty.
 */
@Target(AnnotationTarget.CLASS)
@Retention()
annotation class Table(val tableName: String)
