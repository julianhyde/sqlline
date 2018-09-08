# SQLLine release history and change log

For a full list of releases, see <a href="https://github.com/julianhyde/sqlline/releases">github</a>.

## <a href="https://github.com/julianhyde/sqlline/releases/tag/sqlline-1.5.0">1.5.0</a> (2018-09-09)

Bugs and functional changes:

* [<a href="https://github.com/julianhyde/sqlline/issues/135">SQLLINE-135</a>]
  Display connection class name during `!reconnect` (Arina Ielchiieva)
* [<a href="https://github.com/julianhyde/sqlline/issues/139">SQLLINE-139</a>]
  `!close` command wrongly claims to be ambiguous with `!closeall` command
  (Arina Ielchiieva)
* [<a href="https://github.com/julianhyde/sqlline/issues/106">SQLLINE-106</a>]
  New `class Application` allows customization of command handlers, output
  formats, startup message (Arina Ielchiieva)
* [<a href="https://github.com/julianhyde/sqlline/issues/114">SQLLINE-114</a>]
  Pluggable commands, by means of `interface CommandHandler` (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/52">SQLLINE-52</a>]
  Set isolation level if supported, otherwise use default (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/111">SQLLINE-111</a>]
  Enable multiline calls for `!all`, `!sql` commands from file
  (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/121">SQLLINE-121</a>]
  Cannot parse one-character commands `!?` and `!#` (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/109">SQLLINE-109</a>]
  In XML output formats, escape all required symbols in case of attribute/text
  (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/43">SQLLINE-43</a>]
  Allow quoting of arguments to `!connect` command (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/53">SQLLINE-53</a>]
  In `!connect` command, allow passing of JDBC properties outside of the URL
  (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/120">SQLLINE-120</a>]
  Do not keep connection if it fails to do connection stuff (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/55">SQLLINE-55</a>]
  SQLLine throws `NullPointerException` when JDBC URL is invalid
  (Arina Ielchiieva)
* [<a href="https://github.com/julianhyde/sqlline/issues/86">SQLLINE-86</a>]
  Parse arguments that contain quoted strings with spaces (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/104">SQLLINE-104</a>]
  Make output of `!scan` command valid (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/57">SQLLINE-57</a>]
  Respect `showheader=false` in file output (Renny Koshy and Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/107">SQLLINE-107</a>]
  If a user-specified driver cannot be found, fall back to the registered driver
  (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/101">SQLLINE-101</a>]
  Add `nullValue` property, to override the value printed for NULL values
  (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/67">SQLLINE-67</a>]
  Add `-log` command-line argument, equivalent to `!record` command
  (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/90">SQLLINE-90</a>]
  Use more relevant exception messages for `!metadata`, `!record`, `!run`
  and `!script` commands (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/88">SQLLINE-88</a>]
  Make `!set` command stable for the case of variables with null values
  (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/50">SQLLINE-50</a>]
  For CSV output format, specify delimiter and quote character (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/38">SQLLINE-38</a>]
  In `!run` command, expand "~" to user's home directory (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/83">SQLLINE-83</a>]
  Add `json` output format (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/66">SQLLINE-66</a>]
  Add `dateFormat`, `timeFormat` and `timestampFormat` properties
  (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/77">SQLLINE-77</a>]
  Poor performance with drivers that have a slow implementation of
  `DatabaseMetaData.getPrimaryKeys` JDBC method (Kevin Minder)

Other:

* [<a href="https://github.com/julianhyde/sqlline/issues/131">SQLLINE-131</a>]
  Release 1.5
* Add committers: Arina Ielchiieva and Sergey Nuyanzin (committers were
  previously called 'authors')
* [<a href="https://github.com/julianhyde/sqlline/issues/132">SQLLINE-132</a>]
  Fix 9 javadoc warnings (Arina Ielchiieva)
* [<a href="https://github.com/julianhyde/sqlline/issues/137">SQLLINE-137</a>]
  Typo in description of `!save` command (Arina Ielchiieva)
* [<a href="https://github.com/julianhyde/sqlline/issues/127">SQLLINE-127</a>]
  In manual, update `!help` description with actual output (Sergey Nuyanzin)
* Add more information to `pom.xml` and manifest
* [<a href="https://github.com/julianhyde/sqlline/issues/123">SQLLINE-123</a>]
  Make `SqlLineArgsTest.testScan` test stable in case of several registered
  drivers (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/69">SQLLINE-69</a>]
  Make `sqlline` and `sqlline.bat` scripts work in dev environment
  (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/96">SQLLINE-96</a>]
  Add to Travis CI all supported JDK versions (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/93">SQLLINE-93</a>]
  To fix flaky tests on Windows, use static final fields for
  `jline.Terminal#getWidth` and `jline.Terminal#getHeight` (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/95">SQLLINE-95</a>]
  In `pom.xml`, replace 'prerequisites' element with `maven-enforcer-plugin`
* [<a href="https://github.com/julianhyde/sqlline/issues/80">SQLLINE-80</a>]
  In `HOWTO`, describe how to run SQLLine in IntelliJ IDEA's console on Windows
  (slankka)

## <a href="https://github.com/julianhyde/sqlline/releases/tag/sqlline-1.4.0">1.4.0</a> (2018-05-31)

Bugs and functional changes:

* [<a href="https://github.com/julianhyde/sqlline/issues/75">SQLLINE-75</a>]
  jline gives `NumberFormatException` during startup
  * Caused by [<a href="https://github.com/jline/jline2/issues/281">JLINE2-281</a>],
    and fixed by upgrading jline to 2.14.4
* [<a href="https://github.com/julianhyde/sqlline/issues/72">SQLLINE-72</a>]
  Allow quoted file names (including spaces) in `!record`, `!run` and `!script`
  commands (Jason Prodonovich)

Other:

* Upgrade `maven-javadoc-plugin` to 3.0.1 due to
  [<a href="https://issues.apache.org/jira/browse/MJAVADOC-485">MJAVADOC-517</a>]
* [<a href="https://github.com/julianhyde/sqlline/issues/76">SQLLINE-76</a>]
  During build, manual fails to validate against docbook DTD
* Travis CI: remove JDK 6 and 7 (no longer supported by Travis)
* Upgrade `maven-javadoc-plugin` to 3.0.0 due to
  [<a href="https://issues.apache.org/jira/browse/MJAVADOC-485">MJAVADOC-485</a>]
* Add hyperlinks to previous test-cases (Julian Hyde)
* Add coursier usage example, and update contact info (Marc Prud'hommeaux)

## <a href="https://github.com/julianhyde/sqlline/releases/tag/sqlline-1.3.0">1.3.0</a> (2017-06-09)

Bugs and functional changes:

* [<a href="https://github.com/julianhyde/sqlline/issues/63">SQLLINE-63</a>]
  Add Athena JDBC driver (Ben Poweski)
* [<a href="https://github.com/julianhyde/sqlline/issues/62">SQLLINE-62</a>]
  In `!record` and `!run` commands, expand '~' to user's home directory (Mike
  Mattozzi)
* [<a href="https://github.com/julianhyde/sqlline/issues/61">SQLLINE-61</a>]
  Add `!nickname` command, to set a friendly name for a connection (Mike Mattozzi)
* [<a href="https://github.com/julianhyde/sqlline/issues/54">SQLLINE-54</a>]
  Add JDK 9 support; drop JDK 1.5 support

Other:

* Fix tests on Windows
* Sort list of JDBC drivers
* Publish manual and API for release 1.2.0
* Create a property for the version number of each maven dependency
* Add "release" profile, and only sign if it is enabled
* Edit release instructions

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
