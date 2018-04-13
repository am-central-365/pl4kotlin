# pl4kotlin
Persistence layer for Kotlin

**The project desperately needs a better name**

The project is a library for mapping between RDBMS tables and Java
objects. Currently only 1:1 mapping is supported. e.g. a POJO instance
maps to a single record in a table. All four CRUD operations are
supported.

The goal is to simplify basic database operations without getting on
the way. You can always tell what SQL statement is going to be executed
as a result of your request.

Beginners and experienced SQL users will appreciate the simplicity of
routine operations automation, without sacrificing the flexibility.


## Quick Start

Let's say there is [SCOTT schema](http://www.orafaq.com/wiki/SCOTT#Original_SCOTT.27s_tables_since_Oracle_4)
We can define its DAO in the code as:

#### The DAO class
The class is derived from Entity provided by the linbrary. Persisted
members are marked with with @Column annotation. There are a few attributes
beside pkPos, they are described later.

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


#### Creating data

To add a record to the database table, we instantiate a new object,
and supplying it to the InsertStatement:

```Kotlin
  val scott = Emp(empno=7788, ename="Scott" /* other fields */)
  val count = InsertStatementUnderTest(scott).run()
```

A transaction is started, the record is inserted into table, auto-generated
fields are fetched back, and the transaction is committed.

If we wanted more control over transaction processing, we'd supply a
connection object to ```run```. Then the library would not attemptto control
the transaction.

#### Reading data

```Kotlin
  val martin: Emp(empno=7654)
  val count = SelectStatement(martin).select(martin.allColsButPk!!).byPk().run()
```

We've populated record's Primary Key column empno, and fetching other
fields frmo te database. Variable ```count``` can be checked to see if
the record was really fetched.

#### Updating data

Let's increase Allen's commission to 350.
```Kotlin
  val allen = Emp(empno=7499, commission = 350f)
  val count = UpdateStatement(allen).update(Emp::commission).byPk().run()
```


We could also increase it by X percent using expression. To make the SQL
reusable in RDBMS cache, let's specify increase percent as a bind variable:

```Kotlin
  val count = UpdateStatement(allen)
                .update(Emp::commission, "com*?", 1.1f)
                .byPk()
                .fetchBack("com")
                .run()
```

After updating the data, we read it back into the POJO variable with fetchBack().

#### Deleting data

For the sake of demonstrating multi-row operations with expressions,
let's delete all salesmen with zero commission

```Kotlin
  val count = DeleteStatement(Emp()).by("job = 'SALESMAN'").by("com <= 0").run()
```

Since we are not using any POJO data, we're simply passed a new object
without bothering to initialize it.
