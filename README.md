# Persistence Layer for Kotlin
**The project needs a better name**

The project is a library for mapping between JDBC table rows and Java
objects. Currently only 1:1 mapping is supported. e.g. a POJO instance
maps to a single record of a table. All four CRUD operations are
supported.

The goal is to simplify basic database operations without getting in
the way. You can always tell what SQL statement is going to be executed
as a result of the request. All generated statements utilize bind variable
to improve efficiency and prevent SQL injection.

SQL beginners will appreciate simplicity of routine operations
automation. Experienced users may like flexibility of mixing the
library and JDBC code.

## Tested with

* MySql
* Oracle
* PostgreSQL
* SQLite

It is likely that other RDBMS are also supported, but we never had
chance to confirm that. The library avoids any non-ANSI SQL (like
Oracle ```returning``` clause).

## Building the library
To build the library JAR, use Maven:
```
mvn package
```
The resulting jar is located in ```target``` subdirectory.

Other standard Maven variations are ```mvn clean install```,
```mvn test```, and so on. Check out [Apache Maven](https://maven.apache.org/)
docs for details.

## Maven dependency

```XML
    <dependency>
        <groupId>com.amcentral365</groupId>
        <artifactId>pl4kotlin</artifactId>
        <version>${pl4kotlin.version}</version>
    </dependency>
```

## Quick Start

Consider Oracle's [SCOTT schema](http://www.orafaq.com/wiki/SCOTT#Original_SCOTT.27s_tables_since_Oracle_4).
We can define its DAO as follows:

#### The DAO class
All classes must be derived from base class Entity provided by the library.
Persisted members are marked with with @Column annotation. There are a
few attributes beside column name and pkPos, they are described in the
docs.

```Kotlin
@Table("emp")
class Emp: Entity() {
    @Column("empno",  pkPos = 1) var empno:      Int = 0
    @Column("ename")             var ename:      String?
    @Column("hiredate")          var hireDate:   Date?
    @Column("com")               var commission: Float?
    // other columns

    // feel free to add constructors, transient members, methods, etc
}
```

field ```hireDate``` corresponds to database column ```hiredate```,
```commission``` to column ```com```, etc. On write (Insert, Update)
field values are stored in the row. On read, they are loaded
from the row. Field values are used in the ```WHERE``` clause of the
corresponding SQL.


#### Referencing fields
There are multiple ways to supply participating columns. Typically we
can use a Kotlin object's field because it provides better type chacking
and refactoring. Column name can also be used - the generated SQL is the same.
Finally, a SQL expression form gives maximum flexibilty.


#### Creating data
To add a record to the database table, we instantiate a new object and
pass it to InsertStatement:

```Kotlin
  val scott = Emp(empno=7788, ename="Scott" /* other fields */)
  val count = InsertStatement(scott).run()
```

An INSERT statement is generated and ```scott``` object's property values
are written to their corresponding columns, If the class declared any
database-side generated fields, their values are automatically fetched
back in the same transaction.


#### Reading data
```Kotlin
  val martin: Emp(empno=7654)
  val count = SelectStatement(martin).select(martin.allColsButPk!!).byPk().run()
```

This transalates to SQL statment:
```SQL
SELECT ename, hiredate, com, ... FROM emp WHERE emptno = ?
```
Statement's bind variable is set to the value of property ```martin.empno```,
and the query is executed. If the record was found, values of the fetched
columns are stored in the corresponding fields of object ```martin```.


#### Updating data
Columns which need to be updated must be listed explicitly.

Let's change Allen's commission to 350. We assign new value to the
object's field and call ```UpdateStatement``` to update it:
```Kotlin
  val allen = Emp(empno=7499, commission = 350f)
  val count = UpdateStatement(allen).update(Emp::commission).byPk().run()
```
Note that we used class field ```Emp::commission```. We could also
specify the object's field: ```allen::commission```. Or, we could have
supplied the column name as string: ```update("com")```. In all three
forms the parameter is only used to identify the column and its corresponding
property.

The generated SQL is:
```SQL
UPDATE emp SET com = ? WHERE empno = ?
```

Sometimes we want to update a column to a SQL expression. Say, we wanted
to increase commission by 15 percent. Further, to make the SQL reusable
in the RDBMS cache, we wanted to provide its value via a bind variable:

```Kotlin
  val count = UpdateStatement(allen)
                .update(allen::commission, "com * ?", 1.15)
                .byPk()
                .fetchBack("com")
                .run()
```

##### Fetching data back
In cases like the above, we want to read back the updated value(s). This
is especially useful when column value is updated to SQL ```default```
expresion, as in
```create table T(..., modified_ts timestamp default systimestamp```.

In the example above, we fetched back the reesulting commission by calling
```fetchBack``` method. The value is stored in the appropriate POJO
property after statement execution.


#### Deleting data
For the sake of demonstrating multi-row operations with expressions,
let's delete all salesmen records with zero commission:

```Kotlin
  val count = DeleteStatement(Emp()).by("job = 'SALESMAN'").by("com <= 0").run()
```

Since we are not using any POJO data, we have simply passed a new object
without bothering to initialize it. We also did not use any properties,
specifying the condition as a free-form expression,


### Controlling transactions
As one of its arguments, POJO constructor takes a function to return
database connection. The function is called when ```run()``` is invoked
without parameters. The obtained connection is used to execute the
SQL statement, and then transaction is committed or rolled back
depending on the outcome. Then the connection object is closed.

If external control over transaction lifetime is needed, ```run()```
can be invoked with a ```Connection``` object. Then the library does not
attempt to commit/rollback nor close the connection. This allows running
multiple statements in the same transaction.
