package com.amcentral365.pl4kotlin

/**
 * Controls computation of property value when new object is being inserted into the table.
 * The value may be overwritten by the client or omitted in the `INSERT` statement resulting
 * in setting the column to `DEFAULT` by the database,
 *
 * E.q. if c2's onInsert annotation is OnTheDbWhenNull and its value is null, the executed
 * statement will look like:
 *
 *     INSERT INTO t(c1, c3) VALUES(?, ?)
 *
 * e.g. c2 was omitted.
 */
enum class Generated {
    Never                  /** The value is never altered */
  , OnTheClientAlways      /** The value is always generated prior to insert */
  , OneTheClientWhenNull   /** The value is generated if it is null */
  , OnTheDbAlways          /** The column is omitted from the `INSERT` statement, making the database compute it */
  , OnTheDbWhenNull        /** When the property value is null, its column is omitted from the `INSERT` statement */
}


/**
 * Property annotation, marking it as mapped to a column.
 *
 * @property columnName Defines property linkage to a table column. Mandatory.
 * @property restParamName Used when parsing REST API call values. The parsing isn't yet implemented.
 * @property pkPos  Specifies column position within the table Primary Key. Positions start with 1,
 *   and must be sequential. Zero, the default, indicates a non-PK column. A class must define
 *   at least one PK column.
 * @property isOptimisticLock When requested, the column participates in `WHERE` condition of the
 *  `UPDATE` statement to ensure the row data hasn't been changed since it's been read.
 *   See the docs for details.
 * @property onInsert Tells the library how to treat the column when inserting a new row.
 *  The column may be conditionally omitted from the statement making the database to evaluate its
 *  `DEFAULT` expression. It is also possible to ask the library to assign a value to it.
 * @property isJson is used for database-side columns containing JSON ('json' type in MySQL, is_json
 *   constraint in Oracle) and represented on the client as String. When the property is true, the object
 *   type must be `String` and all interactions with the database are the same as for the `String` type.
 *   The setting only affects how `asJson*` generate the value: them embed it without enclosing in double
 *   quotes (unlike `String`) and not masking special characters inside it.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention()
annotation class Column(
      val columnName: String
    , val restParamName: String = ""
    , val pkPos: Int = 0
    , val isOptimisticLock: Boolean = false
    , val onInsert: Generated = Generated.Never
    , val isJson: Boolean = false
)
