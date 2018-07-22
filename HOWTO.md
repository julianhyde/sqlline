# SQLLine HOWTO

## Branding

Capitalization is tricky:
* The project (and the program in general) is called "SQLLine";
* The main Java class is called "SqlLine"; other classes are in CamelCase also;
* The shell scripts are called "sqlline" and "sqlline.bat";
* Nothing is called "SQLline".

## How to make a release (for committers)

Make sure `mvn clean install` and `mvn site` pass under JDK 1.7, 8, 9,
and 10.

Write release notes. Run the
[relNotes](https://github.com/julianhyde/share/blob/master/tools/relNotes)
script and append the output to [HISTORY.md](HISTORY.md).

Update version numbers in README, README.md, src/docbkx/manual.xml,
and the copyright date in NOTICE.

Switch to JDK 1.7.

Check that the sandbox is clean:

```bash
git clean -nx
mvn clean
```

Prepare:

```bash
read -s GPG_PASSPHRASE
mvn -Prelease -DreleaseVersion=x.y.0 -DdevelopmentVersion=x.y+1-SNAPSHOT -Darguments="-Dgpg.passphrase=${GPG_PASSPHRASE}" release:prepare
```

Perform:

```bash
mvn -Prelease -DskipTests -Darguments="-Dgpg.passphrase=${GPG_PASSPHRASE}" release:perform
```

Publish the release:
* Go to http://oss.sonatype.org and log in.
* Under "Build Promotion", click on "Staging Repositories".
* Select the line "sqlline-nnnn", and click "Close". You might need to
  click "Refresh" a couple of times before it closes.
* If it closes without errors, click "Release".

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


## How to run sqlline in IntelliJ IDEA (Windows)
This is a guide may be helpful for Issue#80.
In Windows 10, the Sqlline (actually the Jline) may ignore user input in Run/Debug Console of IDEA.
When you type the command then hint <Enter>, the sqlline doesn't response.

The problem is caused by the compatiblity between Jline2 and IntelliJ Idea.

The Idea Console is not a standard console which is not support well by Jline.

The Jline program inside uses JNI call to get the Console or Terminal information.

If you're interested about it, you can try Jline3. God it takes me too much time.

Well, the Jline3 latest still not works well in IDEA, I have tried. 

For Jline2 the Sqlline now use, you can launch it like this:
```
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
//            stringList.add("--maxWidth=160"); //If you read the issue#80, you will understand why.
//            stringList.add("--maxHeight=2000");
//            stringList.add("--color=true");
            
            //Add this line, the Console should response you command.
            jline.TerminalFactory.registerFlavor(jline.TerminalFactory.Flavor.WINDOWS, UnsupportedTerminal.class);

            String join = StringUtils.join(stringList, " ");
            String[] argsGiven = join.split(" ");

            SqlLine.main(argsGiven);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
```
I should works fine. ^_^
