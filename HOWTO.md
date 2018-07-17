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
