[![Build Status](https://travis-ci.org/julianhyde/sqlline.png)](https://travis-ci.org/julianhyde/sqlline)

Command-line shell for issuing SQL to relational databases via JDBC.

## History

A fork of the [Marc Prud'hommeaux](http://mprudhom.users.sourceforge.net/)'s
[sqlline](http://sourceforge.net/projects/sqlline/) project, also
incorporating changes made by the
[LucidDB](https://github.com/LucidDB/luciddb) project,
now modernized, mavenized and forkable in github.

## License

sqlline is distributed under the
[3-clause BSD License](http://opensource.org/licenses/BSD-3-Clause),
meaning that you are free to redistribute, modify, or sell it with
almost no restrictions.

## Getting started

Copy sqlline script (or sqlline.bat), sqlline-VERSION-jar-with-dependencies.jar
and a jdbc driver jar to the same directory. And, just put it to the `PATH`.

```bash
$ sqlline -d com.mysql.jdbc.Driver
sqlline> !connect jdbc:mysql://localhost:3306/foodmart user password
sqlline> !tables
+------------+--------------+-------------+---------------+----------+
| TABLE_CAT  | TABLE_SCHEM  | TABLE_NAME  |  TABLE_TYPE   | REMARKS  |
+------------+--------------+-------------+---------------+----------+
| null       | SALES        | DEPTS       | TABLE         | null     |
| null       | SALES        | EMPS        | TABLE         | null     |
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
sqlline --help
```

Or, an alternative method without sqlline script:

```bash
$ java -jar sqlline-VERSION-jar-with-dependencies.jar --help
```

Read [the manual](http://www.hydromatic.net/sqlline/manual.html).

## Maven Usage

Use the following definition to use `sqlline` in your maven project:

```xml
<dependency>
  <groupId>sqlline</groupId>
  <artifactId>sqlline</artifactId>
  <version>1.1.6</version>
</dependency>
```

## Building

Prerequisites:

* Maven 2+
* Java 5+

Check out and build:

```bash
git clone git://github.com/julianhyde/sqlline.git
cd sqlline
mvn package
```

## Authors

* Marc Prud'hommeaux <marc@apocalypse.org>
* John V. Sichi (jsichi@gmail.com)
* Stephan Zuercher (stephan@zuercher.us)
* Sunny Choi
* John Pham
* Steve Herskovitz
* Jack Hahn
* [Julian Hyde](https://github.com/julianhyde)
* [Joe Posner](https://github.com/joeposner)

## Resources

* [Project site](http://www.hydromatic.net/sqlline)
* [API](http://www.hydromatic.net/sqlline/apidocs)
* Mailing list: sqlline-dev@googlegroups.com (<a href="http://groups.google.com/group/sqlline-dev/subscribe">subscribe</a>, <a href="http://groups.google.com/group/sqlline-dev/topics">archive</a>)
* <a href="HISTORY.md">Release notes and history</a>

