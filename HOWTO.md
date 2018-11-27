# SQLLine HOWTO

## Branding

Capitalization is tricky:
* The project (and the program in general) is called "SQLLine";
* The main Java class is called "SqlLine"; other classes are in CamelCase also;
* The shell scripts are called "sqlline" and "sqlline.bat";
* Nothing is called "SQLline".

## How to make a release (for committers)

Make sure `mvn clean install`, `mvn site`, and
`mvn javadoc:javadoc javadoc:test-javadoc` pass under JDK 8, 9, 10 and
11.

Write release notes. Run the
[relNotes](https://github.com/julianhyde/share/blob/master/tools/relNotes)
script and append the output to [HISTORY.md](HISTORY.md).

Update version numbers in README, README.md, src/docbkx/manual.xml,
and the copyright date in NOTICE.

Switch to JDK 11.

Check that the sandbox is clean:

```bash
git clean -nx
mvn clean
```

Prepare:

```bash
export GPG_TTY=$(tty)
mvn -Prelease -DreleaseVersion=x.y.0 -DdevelopmentVersion=x.(y+1).0-SNAPSHOT release:prepare
```

Perform:

```bash
mvn -Prelease -DskipTests release:perform
```

Stage the release:
* Go to http://oss.sonatype.org and log in.
* Under "Build Promotion", click on "Staging Repositories".
* Select the line "sqlline-nnnn", and click "Close". You might need to
  click "Refresh" a couple of times before it closes.

Start a vote by sending an email with subject
"[VOTE] Release sqlline-X.Y.0 (release candidate N)" to
[sqlline-dev](https://groups.google.com/forum/#!forum/sqlline-dev).
Borrow the text from
[a previous vote](https://groups.google.com/forum/#!topic/sqlline-dev/SWHPzpyBwv0)

If the vote is successful, send an email with subject
"[RESULT] [VOTE] Release sqlline-X.Y.0 (release candidate N)".

Publish the release:
* Go to http://oss.sonatype.org and log in.
* Under "Build Promotion", click on "Staging Repositories".
* Select the line "sqlline-nnnn", and click "Release".

Wait a couple of hours for the artifacts to appear on Maven central,
and announce the release.

Update the [github release list](https://github.com/julianhyde/sqlline/releases).

Publish the site:
```bash
git checkout branch-version-x.y
mvn package
mv target/docbkx/html/manual.html docs
git add docs/manual.html
rm -rf docs/apidocs
mv target/apidocs docs
git add docs/apidocs
git commit
git push
```

## Cleaning up after a failed release attempt (for committers)

```bash
# Make sure that the tag you are about to generate does not already
# exist (due to a failed release attempt)
git tag

# If the tag exists, delete it locally and remotely
git tag -d sqlline-X.Y.Z
git push origin :refs/tags/sqlline-X.Y.Z

# Remove modified files
mvn release:clean

# Check whether there are modified files and if so, go back to the
# original git commit
git status
git reset --hard HEAD
```

## Running SQLLine inside IntelliJ IDEA's console on Windows

On Windows 10, SQLLine ignores user input in IDEA's Run/Debug Console.
When you type the command then hint `<Enter>`, SQLLine does not
respond. (This is logged as
[<a href="https://github.com/julianhyde/sqlline/issues/80">SQLLINE-80</a>].)

The problem is caused by the compatibility between Jline2 and IDEA.
IDEA's console is a non-standard console and is poorly supported by
Jline. Jline uses a JNI call to get Console or Terminal information.

Even Jline3 does not work much better. While SQLLine still uses
JLine2, the best workaround is to use a launcher that sets terminal
options, as follows:

```java
public class LaunchSqlline {
  public static void main(String[] args) {
    try {
      List<String> stringList = new LinkedList<>();
      stringList.add("-d org.apache.phoenix.queryserver.client.Driver");
      stringList.add("-u 'jdbc:phoenix:thin:url=http://localhost:8765'");
      stringList.add("-n none");
      stringList.add("-p none");
      stringList.add("--incremental=false");
      stringList.add("--isolation=TRANSACTION_READ_COMMITTED");
/*
      // Read [SQLLINE-80] to see why maxWidth must be set
      stringList.add("--maxWidth=160");
      stringList.add("--maxHeight=2000");
      stringList.add("--color=true");
*/

      // Add this line, the Console should respond to your command
      jline.TerminalFactory.registerFlavor(
          jline.TerminalFactory.Flavor.WINDOWS,
          UnsupportedTerminal.class);

      String join = StringUtils.join(stringList, " ");
      String[] argsGiven = join.split(" ");

      SqlLine.main(argsGiven);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
```
