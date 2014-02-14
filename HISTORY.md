# Sqlline release history and change log

For a full list of releases, see <a href="https://github.com/julianhyde/sqlline/releases">github</a>.

## <a href="https://github.com/julianhyde/sqlline/releases/tag/sqlline-1.1.7">1.1.7</a> (2014-02-14)

Bugs and functional changes:
* Fix bug: SqlCompleter was skipping every other column.
* Fix <a href="https://issues.apache.org/jira/browse/HIVE-4566">HIVE-4566</a>, "NullPointerException if typeinfo and nativesql commands are executed at beeline before a DB connection is established" (Xuefu Zhang via Ashutosh Chauhan and Julian Hyde)
* Fix <a href="https://issues.apache.org/jira/browse/HIVE-4364">HIVE-4364</a>, "beeline always exits with 0 status, should exit with non-zero status on error" (Rob Weltman via Ashutosh Chauhan and Julian Hyde)
* Fix <a href="https://issues.apache.org/jira/browse/HIVE-4268">HIVE-4268</a>, "Beeline should support the -f option" (Rob Weltman via cws and Julian Hyde). SqlLine options '-f file' and '--run=file' are now equivalent.
* Add SqlLineArgsTest, copied from BeeLineArgsTest, but testing against hsqldb rather than Hive.

Code re-organization:
* Break out inner classes.
* Re-format in Apache style (indent 2).
* Enable maven-checkstyle-plugin, and fix code to comply.
* Modern Java style: add generics; use `StringBuilder` rather than `StringBuffer`; convert arrays to collections; convert some array parameters to varargs; convert loops to `foreach`; remove paranoid `!= null` checks; convert `ColorAttr` to an enum.
* Fix javadoc errors and warnings.

Other:
* Add release history.
* Enable oraclejdk8 in Travis CI.
* Fix verbose mode.

## <a href="https://github.com/julianhyde/sqlline/releases/tag/sqlline-1.1.6">1.1.6</a> (2013-08-23)

* When not running as a shell return boolean to indicate if command worked.
* Add travis build status to README.

## <a href="https://github.com/julianhyde/sqlline/releases/tag/sqlline-1.1.5">1.1.5</a> (2013-07-31)

* Follow the JDBC pattern of not putting extra checks in cancel calls.
* Specify source and target java versions.
* Enable Travis CI.

## <a href="https://github.com/julianhyde/sqlline/releases/tag/sqlline-1.1.4">1.1.4</a> (2013-07-22)

* Add maven release plugin.
* When reading from stdin redirect don't NullPointer at the end of data.

## 1.1.3 (2013-06-27)

* Fixed setting of working dot dir to allow override.
* Limit history saving to interactive shell.
* Fixed history so it saves; removed JLine1 workaround code.
* CTRL-C cancels screen output from query
