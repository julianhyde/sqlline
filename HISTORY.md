# SQLLine release history and change log

For a full list of releases, see <a href="https://github.com/julianhyde/sqlline/releases">github</a>.

## <a href="https://github.com/julianhyde/sqlline/releases/tag/sqlline-1.2.0">1.2.0</a> (2016-10-26)

Bugs and functional changes:

* [<a href="https://github.com/julianhyde/sqlline/issues/49">SQLLINE-49</a>]
  `!manual` command fails
* [<a href="https://github.com/julianhyde/sqlline/issues/48">SQLLINE-48</a>]
  Lazily load system properties on startup
* [<a href="https://github.com/julianhyde/sqlline/issues/35">SQLLINE-35</a>]
  Make `SqlLine.begin` method public
* [<a href="https://github.com/julianhyde/sqlline/issues/42">SQLLINE-42</a>]
  Script fails if first line is a comment
* [<a href="https://github.com/julianhyde/sqlline/issues/41">SQLLINE-41</a>]
  `!tables` command hangs in h2
* [<a href="https://github.com/julianhyde/sqlline/issues/39">SQLLINE-39</a>]
  `!help set` shouldn't break long lines
* Add `WrappedSqlException` to allow `IncrementalRows` to throw a `SQLException`
  (Parth Chandra)

Other:
* Change capitalization of project name to "SQLLine"
  (previously a mixture of "sqlline" and "SQLline")
* [<a href="https://github.com/julianhyde/sqlline/issues/37">SQLLINE-37</a>]
  Initial github-pages web site, including manual
* Checkstyle should always look for unix line endings, even on windows
* Fix Windows line endings
* Switch to "scott-data-hsqldb" as test data set; it is smaller than
  "foodmart-data-hsqldb"

## <a href="https://github.com/julianhyde/sqlline/releases/tag/sqlline-1.1.9">1.1.9</a> (2015-03-06)

No bug fixes or other functional changes

Other:
* Publish releases to <a href="http://search.maven.org/">Maven Central</a>
  (previous releases are in <a href="http://www.conjars.org/">Conjars</a>)
* Sign jars
* Use <a href="https://github.com/julianhyde/hydromatic-parent">net.hydromatic parent POM</a>,
  upgrading several maven plugins
* Fix code style for upgraded checkstyle

## <a href="https://github.com/julianhyde/sqlline/releases/tag/sqlline-1.1.8">1.1.8</a> (2015-02-16)

Bugs and functional changes:
* [<a href="https://github.com/julianhyde/sqlline/issues/32">SQLLINE-32</a>]
  `!help set` should print documentation for all variables
* Add `sqlline` and `sqlline.bat` scripts, and `jar-with-dependencies`
  (Jongyeol Choi)
* Fix color output: output style instead of name
* Add test for
  [<a href="https://issues.apache.org/jira/browse/HIVE-5768">HIVE-5768</a>]
  Beeline connection cannot be closed with `!close` command
* Test `!record` command
* [<a href="https://github.com/julianhyde/sqlline/issues/26">SQLLINE-26</a>]
  Flush output for each command when using `!record` command
* Use `ResultSet.getString()` for types that support it,
  `getObject().toString()` otherwise
* [<a href="https://github.com/julianhyde/sqlline/issues/25">SQLLINE-25</a>]
  Spaces in classpath
* Add mailing list to maven
* Upgrade `maven-release-plugin` to version 2.4.2
* Make return code enum public so other code can check it

## <a href="https://github.com/julianhyde/sqlline/releases/tag/sqlline-1.1.7">1.1.7</a> (2014-02-14)

Bugs and functional changes:
* Fix bug: SqlCompleter was skipping every other column.
* Fix <a href="https://issues.apache.org/jira/browse/HIVE-4566">HIVE-4566</a>, "NullPointerException if typeinfo and nativesql commands are executed at beeline before a DB connection is established" (Xuefu Zhang via Ashutosh Chauhan and Julian Hyde)
* Fix <a href="https://issues.apache.org/jira/browse/HIVE-4364">HIVE-4364</a>, "beeline always exits with 0 status, should exit with non-zero status on error" (Rob Weltman via Ashutosh Chauhan and Julian Hyde)
* Fix <a href="https://issues.apache.org/jira/browse/HIVE-4268">HIVE-4268</a>, "Beeline should support the -f option" (Rob Weltman via cws and Julian Hyde). SQLLine options '-f file' and '--run=file' are now equivalent.
* Add `SqlLineArgsTest`, copied from `BeeLineArgsTest`, but testing against hsqldb rather than Hive.

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
