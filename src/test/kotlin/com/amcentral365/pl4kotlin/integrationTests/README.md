## Running Integration tests


Maven 3x allows to define integraton tests.
They are not ran as part of standard ```mvn package``` command, but
invoked automatically at ```verify```, ```install```, or ```deploy```
phases. More detals can be found on
[Maven Build Lifecycle](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html)
page

Test case file names must be prefixed or suffixed with "IT" ot "ITCase":
ITCaseBlah, ITBlah, BlahIT, and BlahITCase are examples of integration test Blah.

### Invocation

```
mvn -DdbConfig=file-name verify
```

### The config file
It is a standard Java properties file. Required properties are:
```jdbcUrl```, ```username```, and ```password```.

A sample config:
```INI
jdbcUrl=jdbc:mysql//localhost:3306/it
username=ituser
password=itpass
```

#### Canned configs
The code comes with sample configs for Oracle, MySql, PostgreSQL and SQLite.
They assume the database is installed on the local machine. See section
[Installing test databases with Docker](#Installing-test-databases-with-Docker)
below.

### Setup and teardown scripts
These scripts are used to create and clean database objects.
They are called prior to running every test.
The scripts assume the connecting user account and the database are
exist and possess all necessary privileges.

#### Script names
Each script is composed from the JDBC vendor name and ```Setup``` or
```Teardown``` suffix. The file extension is ```.sql```.

Database vendor is coming from the second element of jdbc URL:
```
  jdbc:vendor-name:...
```

The code extracts the portion between "jdbc:" and any of the characters
':', '/', and '@'.


Note: according to
[Conneciton string examples](http://www.java2s.com/Tutorials/Java/JDBC_Driver_Connection/JDBC_Driver_URL_and_Connection_String_List_for_all_databases.htm)
the second element isn't always the vendor name. But it is still
distinguishing enough for our purpose.

##### Examples
* jdbc:oracle:thin:@Host:Port:SID -> oracleSetup.sql and oracleTeardown.sql
* jdbc:mysql://Host:Port/DatabaseName -> mysqlSetup.sql and mysqlTeardown.sql
* jdbc:weblogic:mssqlserver4:DatabaseName@Host:Port -> weblogicSetup.sql and weblogicTeardown.sql
 (yes, Microsfot ways are always special)

#### Script location
The files is first searched in the current directory, and then among the
canned scripts, packaged into the jar file. Ansence of the file leads to
failure.

This allows to override internal scripts with custom versions or
supply scripts for non-canned database types.

#### Script format
A script file contains regular SQL statements (as suported by the
particular vendor). The statements are separated by any of:
1. A semicolon ';' on a single line (trailing blanks are ignored)
1. A slash '/'  on a single line (trailing blanks are ignored)
1. A blank line (spaces are ignored)

Ignored lines between statements:
* Blank lines
* Lines starting with '#' 

This allows to support complex stetements such as Oracle PL/SQL blocks,
the only requirement is to avoid blank lines.

## Installing test databases with Docker
If you don't have RDBMS installed elsewhere, you can install locally
with Docker. Examples below assume you already have Docker running.

#### MySql
https://hub.docker.com/_/mysql/

#### Oracle
https://github.com/oracle/docker-images/tree/master/OracleDatabase/SingleInstance

#### PostgreSQL
https://hub.docker.com/_/postgres/

#### SQLite

Since SQLite is embedded into your code, install its binaries:

```sudo apt-get install -y sqlite3 libsqlite3-dev```
