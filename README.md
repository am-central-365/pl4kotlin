# pl4kotlin
Persistence Layer for Kotlin

**The project needs a better name**

The project is a library for mapping between JDBC RDBMS tables and Java
objects. Currently only 1:1 mapping is supported. e.g. a POJO instance
maps to a single record in a table. All four CRUD operations are
supported.

The goal is to simplify basic database operations without getting in
the way. You can always tell what SQL statement is going to be executed
as a result of your request. All statements utilize bind variable to
improve efficiency and avoid SQL injection.

SQL beginners will appreciate simplicity of routine operations
automation. Experienced users may like flexibility of mixing the
library and JDBC code.

## Building
To build the library JAR, use Maven:
```
mvn package
```
The resulting jar is located in ```target``` subdirectory.

Other standard Maven variations are ```mvn clean install```,  ```mvn test```, and so on.
Check out [Apache Maven](https://maven.apache.org/) docs for details.

## Quick Start

Consider Oracle's [SCOTT schema](http://www.orafaq.com/wiki/SCOTT#Original_SCOTT.27s_tables_since_Oracle_4).
We can define its DAO as:

#### The DAO class
The class is derived from base class Entity provided by the library.
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


There are multiple ways to supply columns participating in the SQL
operation. Typically we can specify the Kotlin object's field, the
column name, or mention it in a SQL expression. The result is the same.


#### Creating data
To add a record to the database table, we instantiate a new object and
pass it to the InsertStatement:

```Kotlin
  val scott = Emp(empno=7788, ename="Scott" /* other fields */)
  val count = InsertStatement(scott).run()
```

Since we didn't provide ```Connection``` object to ```run()`` method, a
transaction new database connection is opened. Data is inserted into the
table, auto-generated fields are fetched back, and the transaction is
committed.

If we wanted more control over transaction lifetime, we could supply a
Connection object to ```run()```. Then the library does not attempt to
control the transaction.


#### Reading data
```Kotlin
  val martin: Emp(empno=7654)
  val count = SelectStatement(martin).select(martin.allColsButPk!!).byPk().run()
```

We've populated record's Primary Key column empno, and called
```SelectStatement```'s ```run()``` to fetched other fields from the
database. Variable ```count``` can be checked to see if the record was
indeed fetched.


#### Updating data
Let's increase Allen's commission to 350. We assign new value to the
object's field and call ```UpdateStatement``` to update it:
```Kotlin
  val allen = Emp(empno=7499, commission = 350f)
  val count = UpdateStatement(allen).update(Emp::commission).byPk().run()
```

Sometimes we want to update a column to a SQL expression. Say, we wanted
to increase commission by X percent. Further, to make the SQL reusable
in the RDBMS cache, we could specify as a bind variable:

```Kotlin
  val increasePercent = 1.1f
  val count = UpdateStatement(allen)
                .update(Emp::commission, "com * ?", increasePercent)
                .byPk()
                .fetchBack("com")
                .run()
```

##### Fetching data back
Sometimes we want to read back the updated data. This is especially
useful when column value is updated to SQL ```default``` expresion (as in
```create table T(..., modify_ts timestamp default current_timestamp)```).

In the example above, we fetch back commission by callig ```fetchBack```
method on the ```UpdateStatement``` object. The value is stored in the
appropriate POJO field after statement execution.


#### Deleting data
For the sake of demonstrating multi-row operations with expressions,
let's delete all salesmen with zero commission:

```Kotlin
  val count = DeleteStatement(Emp()).by("job = 'SALESMAN'").by("com <= 0").run()
```

Since we are not using any POJO data, we have simply passed a new object
without bothering to initialize it.
