#
# sudo apt-get install -y sqlite3 libsqlite3-dev
# mkdir sqlite
# /usr/bin/sqlite3 sqlite/it.db
#

drop table if exists test_tbl

create table test_tbl(
  pk1      int      not null,
  pk2      smallint not null default 75,
  uuid1       text not null,           /* generated by the client always */
  uuid2       text not null,           /* generated by the client when null */
  created_ts  datetime default(strftime('%Y-%m-%d %H:%M:%f', 'NOW')) not null,  /* NB: second precision */
  modified_ts datetime default(strftime('%Y-%m-%d %H:%M:%f', 'NOW')) not null,  /* nanosecond precision, hope that's enough */
  vc_col      varchar(200),
  char_col    char(7),
  date_col    date,
  time_col    time,
  num_col     decimal(65, 30),
  float_col   real,
  double_col  double precision,
  bit17_val   bit(17),  /* postgres support for using 'bit varying' with JDBC is mysterious */
  enum_col    varchar(20),
  bool_col    boolean,
  null_col    text,
     constraint test_tbl_ck1 check(enum_col in('Alpha', 'Beta', 'Gamma', 'Delta', 'Epsilon')),
     constraint test_tbl_pk primary key(pk1, pk2)
     constraint test_tbl_ak1 unique(uuid1),
     constraint test_tbl_ak2 unique(uuid2)
)

commit