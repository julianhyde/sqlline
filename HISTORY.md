# Sqlline release history and change log

For a full list of releases, see <a href="https://github.com/julianhyde/sqlline/releases">github</a>.

## HEAD

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