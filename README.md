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

Read [the manual](http://www.hydromatic.net/sqlline/manual.html).

## Maven Usage

Use the following definition to use sqlline in your maven project:

    <dependency>
      <groupId>sqlline</groupId>
      <artifactId>sqlline</artifactId>
      <version>1.09</version>
    </dependency>

## Building

### Requirements

* Maven 2+
* Java 5+

Check out and build:

    git clone git://github.com/julianhyde/sqlline.git
    cd sqlline
    mvn install

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
* [Conjars maven repository](http://conjars.org/sqlline)
