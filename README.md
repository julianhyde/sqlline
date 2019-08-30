[![Build Status](https://travis-ci.org/julianhyde/sqlline.png)](https://travis-ci.org/julianhyde/sqlline)

Command-line shell for issuing SQL to relational databases via JDBC.

## History

A fork of [Marc Prud'hommeaux](https://github.com/mprudhom)'s
[sqlline](https://sourceforge.net/projects/sqlline/) project, also
incorporating changes made by the
[LucidDB](https://github.com/LucidDB/luciddb) project,
now modernized, mavenized and forkable in github.
See also [release history](HISTORY.md).

## License and distribution

SQLLine is distributed under the
[3-clause BSD License](https://opensource.org/licenses/BSD-3-Clause),
meaning that you are free to redistribute, modify, or sell it with
almost no restrictions.

It is distributed via the
[Maven Central Repository](https://search.maven.org/#search%7Cga%7C1%7Csqlline).

## Demos
[demos](https://github.com/julianhyde/sqlline/wiki/Demos)

## Quick start

If you have [Coursier](https://github.com/coursier/coursier) installed, you
can quickly connect to a [demo Hypersonic database](https://github.com/julianhyde/foodmart-data-hsqldb) with:

```
$ coursier launch sqlline:sqlline:1.9.0 org.hsqldb:hsqldb:2.5.0 net.hydromatic:foodmart-data-hsqldb:0.4 -M sqlline.SqlLine -- -u jdbc:hsqldb:res:foodmart -n FOODMART -p FOODMART -d org.hsqldb.jdbcDriver
0: jdbc:hsqldb:res:foodmart> select avg("shelf_height" * "shelf_width" * "shelf_depth") as "avg_volume" from "product";
+-------------------------+
|       avg_volume        |
+-------------------------+
| 2147.3845245442353      |
+-------------------------+
1 row selected (0.01 seconds)
0: jdbc:hsqldb:res:foodmart> !quit
```

## Getting started

Copy the `sqlline` script (or `sqlline.bat` for Windows),
`sqlline-VERSION-jar-with-dependencies.jar` and a JDBC driver jar into
the same directory. (Or just put `sqlline` on your `PATH`.)

```bash
$ sqlline -d com.mysql.jdbc.Driver
sqlline> !connect jdbc:mysql://localhost:3306/scott user password
sqlline> !tables
+------------+--------------+-------------+---------------+----------+
| TABLE_CAT  | TABLE_SCHEM  | TABLE_NAME  |  TABLE_TYPE   | REMARKS  |
+------------+--------------+-------------+---------------+----------+
| null       | SCOTT        | BONUS       | TABLE         | null     |
| null       | SCOTT        | DEPT        | TABLE         | null     |
| null       | SCOTT        | EMP         | TABLE         | null     |
| null       | SCOTT        | SALGRADE    | TABLE         | null     |
| null       | metadata     | COLUMNS     | SYSTEM_TABLE  | null     |
| null       | metadata     | TABLES      | SYSTEM_TABLE  | null     |
+------------+--------------+-------------+---------------+----------+
sqlline> SELECT 1 + 2 AS c;
+---+
| C |
+---+
| 3 |
+---+
sqlline> !quit
```

To get help:

```bash
$ sqlline --help
```

If you prefer, you can invoke Java directly, without using the
`sqlline` script:

```bash
$ java -jar sqlline-VERSION-jar-with-dependencies.jar --help
```

### Connecting using URLs

A URL (or connect string) is a string that specifies the location of your
database, and perhaps credentials and other parameters specific to your
database's JDBC driver. It always starts with '`jdbc:`', usually followed by
the machine name of the database and additional parameters.

For example, the following
[bash](https://en.wikipedia.org/wiki/Bash_\(Unix_shell\)) command connects to
[Apache Drill](https://drill.apache.org), assuming that Drill is installed in
`/opt/apache-drill-1.15.0`:

```bash
$ /opt/apache-drill-1.15.0/bin/sqlline -u "jdbc:drill:drillbit=example.com;auth=kerberos"
```

Because '`;`' is a command separator in bash, the URL is included in
double-quotes ('`"`'). You will need to quote the URL and other arguments if
they contain characters that are special in your shell; different shells have
different special characters, but
space ('<code>&nbsp;</code>'),
dollar ('`$`'),
single-quote ('`'`'),
bang ('`!`') and
percent ('`%`') are some common examples.

Read [the manual](https://julianhyde.github.io/sqlline/manual.html).

## Maven Usage

Use the following definition to use `sqlline` in your maven project:

```xml
<dependency>
  <groupId>sqlline</groupId>
  <artifactId>sqlline</artifactId>
  <version>1.9.0</version>
</dependency>
```

## Building

Prerequisites:

* Maven 3.2.5 or higher
* Java 8 or higher (12 preferred)

Check out and build:

```bash
git clone git://github.com/julianhyde/sqlline.git
cd sqlline
mvn package
```

## Committers

* [Arina Ielchiieva](https://github.com/arina-ielchiieva)
* Jack Hahn
* [Joe Posner](https://github.com/joeposner)
* John Pham
* [John V. Sichi](https://github.com/jsichi)
* [Julian Hyde](https://github.com/julianhyde)
* [Marc Prud'hommeaux](https://github.com/marcprux)
* [Sergey Nuyanzin](https://github.com/snuyanzin)
* [Stephan Zuercher](https://github.com/zuercher)
* Steve Herskovitz
* Sunny Choi

## More information

* License: Modified BSD License
* [Home page](https://julianhyde.github.io/sqlline)
* [API](https://julianhyde.github.io/sqlline/apidocs)
* [Source code](https://github.com/julianhyde/sqlline)
* Developers list:
  <a href="mailto:sqlline-dev@googlegroups.com">sqlline-dev at googlegroups.com</a>
  (<a href="https://groups.google.com/group/sqlline-dev/topics">archive</a>,
  <a href="https://groups.google.com/group/sqlline-dev/subscribe">subscribe</a>)
* Twitter: [@SQLLineShell](https://twitter.com/SQLLineShell)
* [Issues](https://github.com/julianhyde/sqlline/issues)
* [Release notes and history](HISTORY.md)
* [HOWTO](HOWTO.md)

