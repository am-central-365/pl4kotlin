#
# create database it;
# create user ituser@localhost identified by 'itpass';
# grant all privileges on it.* to ituser@localhost;
# flush privileges;

create table Tx1(
  pk   int primary key,
  val1 varchar(200)
)

create table Tx2(
  pk   int primary key,
  val2 varchar(200)
)
