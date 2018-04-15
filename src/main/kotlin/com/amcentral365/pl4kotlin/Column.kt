package com.amcentral365.pl4kotlin

/**
 * Controls value of a field when new object is being inserted into the table
 * The value may be overwritten by the client, or replaced with database's "default" keyword
 * E.q. if c2's value is null and its onInsert annotation is OnTheDbWhenNull,
 *      the resulting INSERT statement will look like:
 *      {code}insert into T(c1, c2) values(?, default){code}
 */
enum class Generated {
    Never                 // the value is never altered
  , OnTheClientAlways     // the value is always generated prior to insert
  , OneTheClientWhenNull  // the value is generated if it is null
  , OnTheDbAlways         // instead of the value, SQL keyword "default" is substituted
  , OnTheDbWhenNull       // SQL keyword "default" is substituted when the value is null
}


@Target(AnnotationTarget.PROPERTY)
@Retention()
annotation class Column(
      val columnName: String
    , val restParamName: String = ""
    , val pkPos: Int = 0
    , val isOptimisticLock: Boolean = false
    , val onInsert: Generated = Generated.OnTheDbWhenNull
)
