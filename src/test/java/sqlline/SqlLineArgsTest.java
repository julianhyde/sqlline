/*
// Licensed to Julian Hyde under one or more contributor license
// agreements. See the NOTICE file distributed with this work for
// additional information regarding copyright ownership.
//
// Julian Hyde licenses this file to you under the Modified BSD License
// (the "License"); you may not use this file except in compliance with
// the License. You may obtain a copy of the License at:
//
// http://opensource.org/licenses/BSD-3-Clause
*/
package sqlline;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.h2.util.StringUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hsqldb.jdbc.CustomDatabaseMetadata;
import org.hsqldb.jdbc.JDBCConnection;
import org.hsqldb.jdbc.JDBCDatabaseMetaData;
import org.hsqldb.jdbc.JDBCResultSet;
import org.hsqldb.jdbc.JDBCResultSetMetaData;
import org.jline.builtins.Commands;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.impl.DumbTerminal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import mockit.Expectations;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.internal.reflection.FieldReflection;
import net.hydromatic.scott.data.hsqldb.ScottHsqldb;
import sqlline.extensions.CustomApplication;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.of;

/**
 * Executes tests of the command-line arguments to SqlLine.
 */
public class SqlLineArgsTest {
  private static final ConnectionSpec CONNECTION_SPEC = ConnectionSpec.HSQLDB;
  private static final String DEV_NULL = "/dev/null";
  private ConnectionSpec connectionSpec;
  private SqlLine sqlLine;

  public SqlLineArgsTest() {
    connectionSpec = CONNECTION_SPEC;
  }

  @BeforeEach
  void init() {
    sqlLine = new SqlLine();
    sqlLine.getOpts().setPropertiesFile(DEV_NULL);
  }

  @AfterEach
  void finish() {
    sqlLine.setExit(true);
  }

  static SqlLine.Status begin(SqlLine sqlLine, OutputStream os,
      boolean saveHistory, String... args) {
    try {
      PrintStream beelineOutputStream = getPrintStream(os);
      sqlLine.setOutputStream(beelineOutputStream);
      sqlLine.setErrorStream(beelineOutputStream);
      final InputStream is = new ByteArrayInputStream(new byte[0]);
      return sqlLine.begin(args, is, saveHistory);
    } catch (Throwable t) {
      // fail
      throw new RuntimeException(t);
    }
  }

  /**
   * Execute a script with "beeline -f".
   *
   * @return The stderr and stdout from running the script
   * @param args Script arguments
   */
  private Pair run(String... args) {
    try {
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      SqlLine.Status status = begin(sqlLine, os, false, args);
      return new Pair(status, os.toString("UTF8"));
    } catch (Throwable t) {
      // fail
      throw new RuntimeException(t);
    }
  }

  private Pair runScript(File scriptFile, boolean flag, String outputFileName) {
    return runScript(connectionSpec, scriptFile, flag, outputFileName);
  }

  private Pair runScript(ConnectionSpec connectionSpec, File scriptFile,
      boolean flag, String outputFileName) {
    List<String> args = new ArrayList<>();
    Collections.addAll(args,
        "-d", connectionSpec.driver,
        "-u", connectionSpec.url,
        "-n", connectionSpec.username,
        "-p", connectionSpec.password);
    if (flag) {
      args.add("-f");
      args.add(scriptFile.getAbsolutePath());
    } else {
      args.add("--run=" + scriptFile.getAbsolutePath());
    }
    if (outputFileName != null) {
      args.add("-log");
      args.add(outputFileName);
    }
    final Pair result = run(args.toArray(new String[0]));
    sqlLine.getOpts().setRun(null);
    return result;
  }

  /**
   * Attempts to execute a simple script file with the -f option to SqlLine.
   * Tests for presence of an expected pattern in the output (stdout or stderr).
   *
   * @param scriptText    Script text
   * @param flag          Command flag (--run or -f)
   * @param statusMatcher Checks whether status is as expected
   * @param outputMatcher Checks whether output is as expected
   */
  private void checkScriptFile(String scriptText, boolean flag,
      Matcher<SqlLine.Status> statusMatcher,
      Matcher<String> outputMatcher) {
    try {
      // Put the script content in a temp file
      File scriptFile = createTempFile("foo", "temp");
      PrintStream os =
          new PrintStream(
              new FileOutputStream(scriptFile),
              false,
              StandardCharsets.UTF_8.name());
      os.print(scriptText);
      os.close();

      Pair pair = runScript(scriptFile, flag, null);

      // Check output before status. It gives a better clue what went wrong.
      assertThat(toLinux(pair.output), outputMatcher);
      assertThat(pair.status, statusMatcher);
      final boolean delete = scriptFile.delete();
      assertThat(delete, is(true));
    } catch (Throwable t) {
      // fail
      throw new RuntimeException(t);
    }
  }

  /** Windows line separators in a string into Linux line separators;
   * a Linux string is unchanged. */
  private static String toLinux(String s) {
    return s.replaceAll("\r\n", "\n");
  }

  private File createTempFile(String prefix, String suffix) {
    return createTempFile(prefix, suffix, null);
  }

  private File createTempFile(String prefix, String suffix, Path directory) {
    try {
      final File file = directory == null
          ? Files.createTempFile(prefix, suffix).toFile()
          : Files.createTempFile(directory, prefix, suffix).toFile();
      file.deleteOnExit();
      return file;
    } catch (IOException e) {
      // fail
      throw new RuntimeException(e);
    }
  }

  /**
   * Tests behavior in case of wrong input arguments.
   * Test case for
   * <a href="https://github.com/julianhyde/sqlline/issues/462">[SQLLINE-462]
   * StringIndexOutOfBoundsException for wrong input arguments</a>.
   */
  @ParameterizedTest
  @ValueSource(strings = {"--", "--bad-argument", "--wrong--argument--"})
  public void testWrongInputArguments(String args) {
    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      SqlLine.Status status = begin(sqlLine, os, false, args);
      assertThat(status, equalTo(SqlLine.Status.ARGS));
      sqlLine.runCommands(new DispatchCallback(), "!quit");
      assertTrue(sqlLine.isExit());
    } catch (Throwable t) {
      // fail
      throw new RuntimeException(t);
    }
  }

  @Test
  public void testMultilineScriptWithComments() {
    final String script1Text =
        "!set incremental true\n"
        + "-- a comment  \n values\n--comment\n (\n1\n, ' ab'\n--comment\n)\n;\n";

    checkScriptFile(script1Text, true,
        equalTo(SqlLine.Status.OK),
        containsString("+-------------+-----+\n"
            + "|     C1      | C2  |\n"
            + "+-------------+-----+\n"
            + "| 1           |  ab |\n"
            + "+-------------+-----+"));

    final String script2Text =
        "!set incremental true\n"
        + "--comment \n --comment2 \nvalues (';\n' /* comment */, '\"'"
        + "/*multiline;\n ;\n comment*/)\n -- ; \n; -- comment";

    checkScriptFile(script2Text, true,
        equalTo(SqlLine.Status.OK),
        containsString("+-----+----+\n"
            + "| C1  | C2 |\n"
            + "+-----+----+\n"
            + "| ; \n"
            + " | \"  |\n"
            + "+-----+----+"));
  }

  /**
   * Attempt to execute a simple script file with the -f option to SqlLine.
   * Test for presence of an expected pattern
   * in the output (stdout or stderr), fail if not found.
   */
  @Test
  public void testPositiveScriptFile() {
    checkScriptFile("call 100 + 23;\n",
        true,
        equalTo(SqlLine.Status.OK),
        containsString(" 123 "));
  }

  /**
   * As above, but using '-run' rather than '-f'.
   */
  @Test
  public void testPositiveScriptFileUsingRun() {
    checkScriptFile("call 100 + 23;\n",
        false,
        equalTo(SqlLine.Status.OK),
        containsString(" 123 "));
  }

  /**
   * Test case for
   * <a href="https://github.com/julianhyde/sqlline/issues/42">[SQLLINE-42],
   * Script fails if first line is a comment</a>.
   */
  @Test
  public void testScriptFileStartsWithComment() {
    final String scriptText = "-- a comment\n"
        + "call 100 + 23;\n";
    checkScriptFile(scriptText, true,
        equalTo(SqlLine.Status.OK),
        containsString(" 123 "));
  }

  @Test
  public void testScriptFileStartsWithEmptyLine() {
    final String scriptText = "\n"
        + "call 100 + 23;\n";
    checkScriptFile(scriptText, true,
        equalTo(SqlLine.Status.OK),
        containsString(" 123 "));
  }

  @Test
  public void testScriptFileContainsComment() {
    final String scriptText = "values 10 + 23;\n"
        + "-- a comment\n"
        + "values 100 + 23";
    checkScriptFile(scriptText, true,
        equalTo(SqlLine.Status.OK),
        allOf(containsString(" 33 "), containsString(" 123 ")));
  }

  @Test
  public void testMultilineScriptFileWithComments() {
    final String scriptText = "--comment\n\n"
        + "values \n10 \n+ \n23;\n\n\n"
        + "-- a comment\n"
        + "\n\nvalues "
            + "--comment inside\n\n"
            + "100 --comment inside\n\n"
            + "+ \n\n23\n\n\n;\n\n\n\n";
    checkScriptFile(scriptText, true,
        equalTo(SqlLine.Status.OK),
        allOf(containsString(" 33 "), containsString(" 123 ")));
  }

  @Test
  public void testMultilineScriptFileWithMultilineQuotedStrings() {
    final String scriptText = "--comment\n\n"
        + "values '\nmultiline;\n;\n; string\n'"
        + ";\n\n\n"
        + "values '\nmultiline2;\n;\n; string2\n'"
        + ";\n\n\n";
    checkScriptFile(scriptText, true,
        equalTo(SqlLine.Status.OK),
        allOf(containsString("multiline2;"), containsString("; string2")));
  }

  @Test
  public void testScriptWithMultilineStatementsInARow() {
    final String scriptText = "--comment\n\n"
        + "values 1;values 2;";
    checkScriptFile(scriptText, true,
        equalTo(SqlLine.Status.OK),
        allOf(
            containsString("+----+\n"
                + "| C1 |\n"
                + "+----+\n"
                + "| 1  |\n"
                + "+----+"),
            containsString("+----+\n"
                + "| C1 |\n"
                + "+----+\n"
                + "| 2  |\n"
                + "+----+")));
  }

  @Test
  public void testScriptWithMultilineStatementsAndCommentsInARow() {
    final String scriptText = "--comment;;\n\n"
        + "select * from (values ';') t (\";\");/*;select 1;*/values 2;";
    checkScriptFile(scriptText, true,
        equalTo(SqlLine.Status.OK),
        allOf(
            containsString("+---+\n"
                + "| ; |\n"
                + "+---+\n"
                + "| ; |\n"
                + "+---+"),
            containsString("+----+\n"
                + "| C1 |\n"
                + "+----+\n"
                + "| 2  |\n"
                + "+----+")));
  }
  /**
   * Tests sql with H2 specific one-line comment '//'
   */
  @Test
  public void testMultilineScriptWithH2Comments() {
    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      final File tmpHistoryFile = createTempFile("queryToExecute", "temp");
      try (BufferedWriter bw =
               new BufferedWriter(
                   new OutputStreamWriter(new FileOutputStream(tmpHistoryFile),
                       StandardCharsets.UTF_8))) {
        final String script = "\n"
            + "\n"
            + "!set incremental true\n"
            + "!set maxColumnWidth 10\n"
            + "\n"
            + "\n"
            + "select * from information_schema.tables// ';\n"
            + "// \";"
            + ";\n"
            + "\n"
            + "\n";
        bw.write(script);
        bw.flush();
      }
      SqlLine.Status status =
          begin(sqlLine, os, false,
              "-u", ConnectionSpec.H2.url,
              "-n", ConnectionSpec.H2.username,
              "-p", ConnectionSpec.H2.password,
              "--run=" + tmpHistoryFile.getAbsolutePath());
      assertThat(status, equalTo(SqlLine.Status.OK));
      String output = os.toString("UTF8");
      final String expected = "| UNNAMED    |"
          + " INFORMATION_SCHEMA | ENUM_VALUES | BASE TABLE";
      assertThat(output, containsString(expected));
      sqlLine.runCommands(new DispatchCallback(), "!quit");
      assertTrue(sqlLine.isExit());
    } catch (Throwable t) {
      // fail
      throw new RuntimeException(t);
    }
  }

  /** Test case for
   * <a href="https://github.com/julianhyde/sqlline/issues/72">[SQLLINE-72]
   * Allow quoted file names (including spaces) in <code>!record</code>,
   * <code>!run</code> and <code>!script</code> commands</a>. */
  @Test
  public void testScriptFilenameWithSpace() {
    final String scriptText = "values 10 + 23;\n"
        + "-- a comment\n"
        + "values 100 + 23;\n";
    File scriptFile = createTempFile("Script with Spaces", ".sql");
    try (PrintStream os = getPrintStream(new FileOutputStream(scriptFile))) {
      os.print(scriptText);

      Pair pair = runScript(scriptFile, true, null);
      assertThat(pair.status, equalTo(SqlLine.Status.OK));
      assertThat(pair.output,
          allOf(containsString(" 33 "), containsString(" 123 ")));
    } catch (Exception e) {
      // fail
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testScriptWithOutput() {
    final String scriptText = "!set maxcolumnwidth 20\n"
        + "!set incremental true\n"
        + "values 100 + 123;\n"
        + "-- a comment\n"
        + "values 100 + 253;\n";

    File scriptFile = createTempFile("Script file name", ".sql");
    try (PrintStream os = getPrintStream(new FileOutputStream(scriptFile))) {
      os.print(scriptText);
    } catch (Exception e) {
      // fail
      throw new RuntimeException(e);
    }

    File outputFile = createTempFile("testScriptWithOutput", ".out");
    runScript(scriptFile, true, outputFile.getAbsolutePath());
    assertFileContains(outputFile,
        allOf(containsString("| 223                  |"),
            containsString("| 353                  |")));
    final boolean delete = outputFile.delete();
    assertThat(delete, is(true));
  }

  /**
   * Values that contain null.
   */
  @Test
  public void testNull() {
    final String script = "!set incremental true\n"
        + "values (1, cast(null as integer), cast(null as varchar(3)));\n";
    checkScriptFile(script, false,
        equalTo(SqlLine.Status.OK),
        containsString(
            "+-------------+-------------+-----+\n"
            + "|     C1      |     C2      | C3  |\n"
            + "+-------------+-------------+-----+\n"
            + "| 1           | null        |     |\n"
            + "+-------------+-------------+-----+\n"));
  }

  @Test
  public void testScan() {
    final String expectedLine0 = "Compliant Version Driver Class";
    final String expectedLine1 = "yes       2.5     org.hsqldb.jdbc.JDBCDriver";
    checkScriptFile("!scan\n", false,
        equalTo(SqlLine.Status.OK),
        allOf(containsString(expectedLine0), containsString(expectedLine1)));
  }

  /**
   * Table output without header.
   */
  @Test
  public void testTableOutputNullWithoutHeader() {
    final String script = "!set showHeader false\n"
        + "!set incremental true\n"
        + "values (1, cast(null as integer), cast(null as varchar(3)));\n";
    checkScriptFile(script, false,
        equalTo(SqlLine.Status.OK),
        containsString("| 1           | null        |     |\n"));
  }

  @Test
  public void testTableOutputWithoutTypes() {
    final String script = "!set showTypes true\n"
        + "!set incremental true\n"
        + "values (1, cast(null as integer), cast(null as varchar(3)));\n";
    checkScriptFile(script, false,
        equalTo(SqlLine.Status.OK),
        allOf(
            containsString("| INTEGER | INTEGER | VARCHAR |\n"),
            containsString("| 1           | null        |     |\n")));
  }

  @Test
  public void testTableOutputWithoutTypesButWithoutHeader() {
    final String script = "!set showTypes true\n"
        + "!set showHeader false\n"
        + "!set incremental true\n"
        + "values (1, cast(null as integer), cast(null as varchar(3)));\n";
    checkScriptFile(script, false,
        equalTo(SqlLine.Status.OK),
            containsString("| 1           | null        |     |\n"));
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, 3, 4})
  public void testTableOutputWithoutTerminalAndWithSmallWidth(int maxWidth) {
    new MockUp<SqlLine>() {
      @Mock
      LineReader getLineReader() {
        return null;
      }
    };
    final String script = "!set maxwidth " + maxWidth + "\n"
        + "!set incremental true\n"
        + "values (1, cast(null as integer), cast(null as varchar(3)));\n";
    checkScriptFile(script, false,
        equalTo(SqlLine.Status.OK),
        containsString("|  |\n"));
  }

  @Test
  public void testTableOutputWithZeroMaxWidthAndExistingTerminal() {
    try {
      new MockUp<DumbTerminal>() {
        @Mock
        public Size getSize() {
          return new Size(80, 20);
        }
      };
      final LineReaderBuilder lineReaderBuilder = LineReaderBuilder.builder()
          .parser(new SqlLineParser(sqlLine))
          .terminal(TerminalBuilder.builder().dumb(true).build());
      sqlLine.setLineReader(lineReaderBuilder.build());
      final String script = "!set maxwidth 0\n"
          + "!set incremental true\n"
          + "values (1, cast(null as integer), cast(null as varchar(3)));\n";
      checkScriptFile(script, false,
          equalTo(SqlLine.Status.OK),
          containsString(
              "+-------------+-------------+-----+\n"
                  + "|     C1      |     C2      | C3  |\n"
                  + "+-------------+-------------+-----+\n"
                  + "| 1           | null        |     |\n"
                  + "+-------------+-------------+-----+\n"));
    } catch (Exception e) {
      fail("Test failed with ", e);
    }
  }

  /**
   * Csv output without header.
   */
  @Test
  public void testCsvNullWithoutHeader() {
    final String script = "!set showHeader false\n"
        + "!set outputformat csv\n"
        + "values (1, cast(null as integer), cast(null as varchar(3)));\n";
    checkScriptFile(script, false,
        equalTo(SqlLine.Status.OK),
        containsString("'1','null',''\n"));
  }

  /**
   * Tests the "close" command,
   * <a href="https://issues.apache.org/jira/browse/HIVE-5768">[HIVE-5768]
   * Beeline connection cannot be closed with '!close' command</a> and
   * <a href="https://github.com/julianhyde/sqlline/issues/139">[SQLLINE-139]
   * Look for the exact match if there are multiple matches</a>.
   */
  @Test
  public void testClose() {
    final String expected = "!close\n"
        + "Closing: org.hsqldb.jdbc.JDBCConnection";
    checkScriptFile("!verbose true;\n!close\n", false,
        equalTo(SqlLine.Status.OK), containsString(expected));
  }

  /**
   * Test case for
   * <a href="https://github.com/julianhyde/sqlline/issues/32">[SQLLINE-32]
   * !help set' should print documentation for all variables</a>.
   */
  @Test
  public void testHelpSet() {
    final String expected = "1/1          !help set\n"
        + "!set                List / set a sqlline variable\n"
        + "\n"
        + "Variables:\n"
        + "\n"
        + "Variable        Value      Description\n"
        + "=============== ========== "
        + "==================================================\n"
        + "autoCommit      true/false "
        + "Enable/disable automatic transaction commit\n"
        + "autoPairing     true/false Enable/disable widget that "
        + "auto-closes, deletes \n"
        + "                           and skips over matching delimiters\n"
        + "autoResize      true/false Enable/disable automatic resizing of\n"
        + "                           max height/width based on terminal size\n"
        + "autoSave        true/false Automatically save preferences\n";
    checkScriptFile("!help set\n", false, equalTo(SqlLine.Status.OK),
        containsString(expected));

    // Make sure that each variable (autoCommit, autoSave, color, etc.) has a
    // line in the output of '!help set'
    String help = sqlLine.loc("help-set")
        + sqlLine.loc("variables");

    final TreeSet<String> propertyNamesMixed =
        Arrays.stream(BuiltInProperty.values())
            .map(BuiltInProperty::propertyName)
            .collect(Collectors.toCollection(TreeSet::new));
    for (String p : propertyNamesMixed) {
      assertThat(help, containsString("\n" + p + " "));
    }
    assertThat(propertyNamesMixed.contains("autoCommit"),
        is(true));
    assertThat(propertyNamesMixed.contains("autocommit"),
        is(false));
    assertThat(propertyNamesMixed.contains("trimScripts"),
        is(true));

    while (help.length() > 0) {
      int i = help.indexOf("\n", 1);
      if (i < 0) {
        break;
      }
      if (i > 78) {
        fail("line exceeds 78 chars: " + i + ": " + help.substring(0, i));
      }
      help = help.substring(i);
    }
  }

  /**
   * Test case for
   * <a href="https://github.com/julianhyde/sqlline/issues/39">[SQLLINE-39]
   * 'help set' should not break long lines</a>.
   *
   * <p>But it should break 'help all', which consists of a single long line.
   */
  @Test
  public void testHelpAll() {
    // Note that "connections" has been broken onto a new line.
    final String expected = "1/1          !help all\n"
        + "!all                Execute the specified SQL against all the current\n"
        + "                    connections\n"
        + "sqlline version ???\n";
    checkScriptFile("!help all\n", false, equalTo(SqlLine.Status.OK),
        is(expected));
  }

  /**
   * Tests "!go &lt;non-existent connection indexes&gt;".
   */
  @Test
  public void testGoFailing() {
    final String[] connectionIndexes = {"321", "invalid"};
    for (String connectionIndex : connectionIndexes) {
      final String script = "!go " + connectionIndex + "\n";
      final String expected = "Invalid connection: " + connectionIndex;
      checkScriptFile(script, false,
          // Status.OTHER as checking of fail cases
          equalTo(SqlLine.Status.OTHER), containsString(expected));
    }
  }

  /**
   * Tests "!help go". "go" and "#" are synonyms.
   */
  @Test
  public void testHelpGo() {
    for (String c : new String[] {"go", "#"}) {
      final String expected = "1/1          !help " + c + "\n"
          + "!go                 Select the current connection\n"
          + "sqlline version ???\n";
      checkScriptFile("!help " + c + "\n", false, equalTo(SqlLine.Status.OK),
          is(expected));
    }
  }

  /**
   * Test case for
   * <a href="https://github.com/julianhyde/sqlline/issues/121">!?, !#
   * and other one-symbol length commands stopped working</a>
   *
   * <p>'!?' should work in the same way as '!help'.
   */
  @Test
  public void testHelpAsQuestionMark() {
    final String script = "!?\n";
    final String expected =
        "!autocommit         Set autocommit mode on or off";
    checkScriptFile(script, false,
        equalTo(SqlLine.Status.OK),
        allOf(not(containsString("Unknown command: ?")),
            containsString(expected)));
  }

  /**
   * Test case for
   * <a href="https://github.com/julianhyde/sqlline/issues/49">[SQLLINE-49]
   * !manual command fails</a>.
   */
  @Test
  public void testManual() {
    final String expectedLine = "sqlline version";
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      new MockUp<Commands>() {
        @Mock
        void less(Terminal terminal, InputStream in, PrintStream out,
            PrintStream err, Path currentDir, String[] argv) {
        }
      };

      SqlLine.Status status =
          begin(sqlLine, baos, false, "-e", "!set maxwidth 80");
      assertThat(status, equalTo(SqlLine.Status.OK));
      sqlLine.runCommands(new DispatchCallback(), "!manual");
      String output = baos.toString("UTF8");

      assertThat(output, containsString(expectedLine));
    } catch (Throwable t) {
      // fail
      throw new RuntimeException(t);
    }
  }

  /** Test case for
   * <a href="https://github.com/julianhyde/sqlline/issues/38">[SQLLINE-38]
   * Expand ~ to user's home directory</a>. */
  @Test
  public void testRunFromHome() {
    try {
      Path tmpDir =
          Files.createTempDirectory(
              Paths.get(System.getProperty("user.home")),
              "tmpdir" + System.nanoTime());
      tmpDir.toFile().deleteOnExit();

      File tmpFile = createTempFile("test", ".sql", tmpDir);
      try (Writer fw = new OutputStreamWriter(
          new FileOutputStream(tmpFile), StandardCharsets.UTF_8)) {
        fw.write("!set outputformat csv\n");
        fw.write("values (1, 2, 3, 4, 5);");
        fw.flush();
        checkScriptFile("!run ~" + File.separatorChar
                + tmpDir.getFileName()
                + File.separatorChar + tmpFile.getName().toString(),
            false,
            equalTo(SqlLine.Status.OK),
            allOf(containsString("!set outputformat csv"),
                containsString("values (1, 2, 3, 4, 5);")));
      }
    } catch (Exception e) {
      // fail
      throw new RuntimeException(e);
    }
  }

  /**
   * Test case for
   * <a href="https://github.com/julianhyde/sqlline/issues/26">[SQLLINE-26]
   * Flush output for each command when using !record command</a>.
   */
  @Test
  public void testRecord() {
    File file = createTempFile("sqlline", ".log");
    final String script = "!set incremental true\n"
        + "values 1;\n"
        + "!record " + file.getAbsolutePath() + "\n"
        + "!set outputformat csv\n"
        + "values 2;\n"
        + "!record\n"
        + "!set outputformat csv\n"
        + "values 3;\n";
    checkScriptFile(script, false,
        equalTo(SqlLine.Status.OK),
        RegexMatcher.of("(?s)1/8          !set incremental true\n"
                + "2/8          values 1;\n"
                + "\\+-------------\\+\n"
                + "\\|     C1      \\|\n"
                + "\\+-------------\\+\n"
                + "\\| 1           \\|\n"
                + "\\+-------------\\+\n"
                + "1 row selected \\([0-9.,]+ seconds\\)\n"
                + "3/8          !record .*.log\n"
                + "Saving all output to \".*.log\". Enter \"record\" with no arguments to stop it.\n"
                + "4/8          !set outputformat csv\n"
                + "5/8          values 2;\n"
                + "'C1'\n"
                + "'2'\n"
                + "1 row selected \\([0-9.,]+ seconds\\)\n"
                + "6/8          !record\n"
                + "Recording stopped.\n"
                + "7/8          !set outputformat csv\n"
                + "8/8          values 3;\n"
                + "'C1'\n"
                + "'3'\n"
                + "1 row selected \\([0-9.,]+ seconds\\)\n.*"));

    // Now check that the right stuff got into the file.
    assertFileContains(file,
        RegexMatcher.of("Saving all output to \".*.log\". "
            + "Enter \"record\" with no arguments to stop it.\n"
            + "4/8          !set outputformat csv\n"
            + "5/8          values 2;\n"
            + "'C1'\n"
            + "'2'\n"
            + "1 row selected \\([0-9.,]+ seconds\\)\n"
            + "6/8          !record\n"));
  }

  /** Test case for
   * <a href="https://github.com/julianhyde/sqlline/issues/62">[SQLLINE-62]
   * Expand ~ to user's home directory</a>. */
  @Test
  public void testRecordHome() {
    File home = new File(System.getProperty("user.home"));
    File file;
    for (int i = 0;; i++) {
      file = new File(home, "sqlline" + i + ".log");
      if (!file.exists()) {
        break;
      }
    }
    file.deleteOnExit();
    final String s = "Saving all output to \".*.log\". "
        + "Enter \"record\" with no arguments to stop it.\n"
        + "2/4          !set outputformat csv\n"
        + "3/4          values 2;\n"
        + "'C1'\n"
        + "'2'\n"
        + "1 row selected \\([0-9.,]+ seconds\\)\n"
        + "4/4          !record\n";
    checkScriptFile("!record " + file.getAbsolutePath() + "\n"
            + "!set outputformat csv\n"
            + "values 2;\n"
            + "!record\n",
        false,
        equalTo(SqlLine.Status.OK),
        RegexMatcher.of("(?s)1/4          !record .*.log\n"
            + s
            + "Recording stopped.\n"
            + ".*"));

    // Now check that the right stuff got into the file.
    assertFileContains(file, RegexMatcher.of(s));
    final boolean delete = file.delete();
    assertThat(delete, is(true));
  }

  /**
   * As {@link #testRecord()}, but file name is double-quoted and contains a
   * space.
   */
  @Test
  public void testRecordFilenameWithSpace() {
    final String fileNameWithSpaces = "sqlline' file with spaces";
    File file = createTempFile(fileNameWithSpaces, ".log");
    final SqlLine sqlLine = new SqlLine();
    final String script = "!set incremental true\n"
        + "values 1;\n"
        + "!record " + sqlLine.escapeAndQuote(file.getAbsolutePath()) + "\n"
        + "!set outputformat csv\n"
        + "values 2;\n"
        + "!record\n"
        + "!set outputformat csv\n"
        + "values 3;\n";

    checkScriptFile(script, false,
        equalTo(SqlLine.Status.OK),
            allOf(containsString("!set incremental true\n"),
                containsString("values 1;\n"),
                containsString("+-------------+\n"),
                containsString("|     C1      |\n"),
                containsString("+-------------+\n"),
                containsString("| 1           |\n"),
                containsString("+-------------+\n"),
                containsString("1 row selected ("),
                containsString(
                    "3/8          !record "
                        + sqlLine.escapeAndQuote(file.getAbsolutePath())),
                containsString(
                    "Saving all output to \"" + file.getAbsolutePath() + "\""),
                containsString(
                    "Enter \"record\" with no arguments to stop it.\n"),
                containsString("4/8          !set outputformat csv\n"),
                containsString("'C1'\n"),
                containsString("'2'\n"),
                containsString("1 row selected ("),
                containsString("6/8          !record\n"),
                containsString("Recording stopped.\n"),
                containsString("7/8          !set outputformat csv\n"),
                containsString("8/8          values 3;\n"),
                containsString("'C1'\n"),
                containsString("'3'\n"),
                containsString("1 row selected (")));


    // Now check that the right stuff got into the file.
    assertFileContains(file,
        RegexMatcher.of("Saving all output to \".*.log\". "
            + "Enter \"record\" with no arguments to stop it.\n"
            + "4/8          !set outputformat csv\n"
            + "5/8          values 2;\n"
            + "'C1'\n"
            + "'2'\n"
            + "1 row selected \\([0-9.,]+ seconds\\)\n"
            + "6/8          !record\n"));
  }

  @Test
  public void testRecordInSilentMode() {
    final String fileName = "filename";
    File file = createTempFile(fileName, ".log");
    final SqlLine sqlLine = new SqlLine();
    final String script = "!set incremental true\n!set silent true\n"
        + "!record " + sqlLine.escapeAndQuote(file.getAbsolutePath()) + "\n"
        + "values 1;\n"
        + "!set outputformat csv\n"
        + "values 2;\n"
        + "!set outputformat csv\n"
        + "values 3;\n"
        + "!record\n";

    checkScriptFile(script, false, equalTo(SqlLine.Status.OK),
        allOf(containsString("!set incremental true\n"),
            containsString("!set silent true\n"),
            containsString("+-------------+\n"),
            containsString("|     C1      |\n"),
            containsString("+-------------+\n"),
            containsString("| 1           |\n"),
            containsString("+-------------+\n"),
            containsString("'C1'\n"),
            containsString("'2'\n"),
            containsString("'C1'\n"),
            containsString("'3'\n")));

    assertFileContains(file,
        allOf(containsString("+-------------+\n"),
            containsString("|     C1      |\n"),
            containsString("+-------------+\n"),
            containsString("| 1           |\n"),
            containsString("+-------------+\n"),
            containsString("'C1'\n"),
            containsString("'2'\n"),
            containsString("'C1'\n"),
            containsString("'3'\n"),
            not(allOf(
                containsString("!set incremental true\n"),
                containsString("!set silent true\n"),
                containsString("Saving all output\n"),
                containsString(" selected \n")))));
  }

  private static void assertFileContains(File file, Matcher<String> matcher) {
    try (BufferedReader br = new BufferedReader(
        new InputStreamReader(new FileInputStream(file),
            StandardCharsets.UTF_8.name()))) {
      final StringWriter stringWriter = new StringWriter();
      for (;;) {
        final String line = br.readLine();
        if (line == null) {
          break;
        }
        stringWriter.write(line);
        stringWriter.write("\n");
      }
      assertThat(toLinux(stringWriter.toString()), matcher);
    } catch (Exception e) {
      // fail
      throw new RuntimeException(e);
    }
  }

  /**
   * Test case for
   * <a href="https://github.com/julianhyde/sqlline/issues/61">[SQLLINE-61]
   * Add !nickname command</a>.
   */
  @Test
  public void testNickname() {
    final String script = "!set outputformat csv\n"
        + "values 1;\n"
        + "!nickname foo\n"
        + "values 2;\n";
    final String expected = "(?s)1/4          !set outputformat csv\n"
        + "2/4          values 1;\n"
        + "'C1'\n"
        + "'1'\n"
        + "1 row selected \\([0-9.,]+ seconds\\)\n"
        + "3/4          !nickname foo\n"
        + "4/4          values 2;\n"
        + "'C1'\n"
        + "'2'\n"
        + "1 row selected \\([0-9.,]+ seconds\\)\n.*";
    checkScriptFile(script, false, equalTo(SqlLine.Status.OK),
        RegexMatcher.of(expected));
  }

  /**
   * Attempts to execute a simple script file with the -f option to SqlLine.
   * The first command should fail and the second command should not execute.
   */
  @Test
  public void testBreakOnErrorScriptFile() {
    checkScriptFile("select * from abcdefg01;\ncall 100 + 23;\n",
        true,
        equalTo(SqlLine.Status.OTHER),
        not(containsString(" 123 ")));
  }

  /**
   * Emulation of Apache Hive behavior with throwing exceptions
   * while calling {@link DatabaseMetaData#storesUpperCaseIdentifiers()}
   * and {@link DatabaseMetaData#supportsTransactionIsolationLevel(int)}.
   *
   * <p>Also please see
   * <a href="https://github.com/julianhyde/sqlline/issues/183">[SQLLINE-183]</a>.
   *
   * @param meta Mocked JDBCDatabaseMetaData
   */
  @Test
  public void testExecutionWithNotSupportedMethods(
      @Mocked final JDBCDatabaseMetaData meta) {
    try {
      new Expectations() {
        {
          // prevent calls to functions that also call resultSet.next
          meta.getDatabaseProductName();
          result = "hsqldb";
          // prevent calls to functions that also call resultSet.next
          meta.getDatabaseProductVersion();
          result = "1.0";

          // emulate apache hive behavior
          meta.supportsTransactionIsolationLevel(4);
          result = new SQLException("Method not supported");
        }
      };
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      PrintStream sqllineOutputStream =
          new PrintStream(os, false, StandardCharsets.UTF_8.name());
      sqlLine.setOutputStream(sqllineOutputStream);
      sqlLine.setErrorStream(sqllineOutputStream);
      String[] args = {
          "-d",
          "org.hsqldb.jdbcDriver",
          "-u",
          "jdbc:hsqldb:res:scott",
          "-n",
          "SCOTT",
          "-p",
          "TIGER"
      };
      DispatchCallback dc = new DispatchCallback();
      sqlLine.initArgs(args, dc);
      os.reset();
      sqlLine.runCommands(dc,
          "!set incremental true",
          "values 1;");
      final String output = os.toString("UTF8");
      final String line0 = "+-------------+";
      final String line1 = "|     C1      |";
      final String line2 = "+-------------+";
      final String line3 = "| 1           |";
      final String line4 = "+-------------+";
      assertThat(output,
          CoreMatchers.allOf(containsString(line0),
              containsString(line1),
              containsString(line2),
              containsString(line3),
              containsString(line4)));
    } catch (Throwable t) {
      // fail
      throw new RuntimeException(t);
    }
  }

  @Test
  public void testExecutionException(@Mocked final JDBCDatabaseMetaData meta,
      @Mocked final JDBCResultSet resultSet) {
    try {
      new Expectations() {
        {
          // prevent calls to functions that also call resultSet.next
          meta.getDatabaseProductName();
          result = "hsqldb";
          // prevent calls to functions that also call resultSet.next
          meta.getSQLKeywords();
          result = "";
          // prevent calls to functions that also call resultSet.next
          meta.getDatabaseProductVersion();
          result = "1.0";
          // Generate an exception on a call to resultSet.next
          resultSet.next();
          result = new SQLException("Generated Exception.");
        }
      };
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      PrintStream sqllineOutputStream =
          new PrintStream(os, false, StandardCharsets.UTF_8.name());
      sqlLine.setOutputStream(sqllineOutputStream);
      sqlLine.setErrorStream(sqllineOutputStream);
      String[] args = {
          "-d",
          "org.hsqldb.jdbcDriver",
          "-u",
          "jdbc:hsqldb:res:scott",
          "-n",
          "SCOTT",
          "-p",
          "TIGER"
      };
      DispatchCallback callback = new DispatchCallback();
      sqlLine.initArgs(args, callback);
      // If sqlline is not initialized, handleSQLException will print
      // the entire stack trace.
      // To prevent that, forcibly set init to true.
      FieldReflection.setFieldValue(
          sqlLine.getClass().getDeclaredField("initComplete"), sqlLine, true);
      sqlLine.getConnection();
      sqlLine.runCommands(callback,
          "CREATE TABLE rsTest ( a int);",
          "insert into rsTest values (1);",
          "insert into rsTest values (2);",
          "select a from rsTest; ");
      String output = os.toString("UTF8");
      assertThat(output, containsString("Generated Exception"));
    } catch (Throwable t) {
      // fail
      throw new RuntimeException(t);
    }
  }

  /**
   * Attempts to execute a missing script file with the -f option to SqlLine.
   */
  @Test
  public void testNegativeScriptFile() {
    // Create and delete a temp file
    File scriptFile = createTempFile("sqllinenegative", "temp");
    final boolean delete = scriptFile.delete();
    assertThat(delete, is(true));

    Pair pair = runScript(scriptFile, true, null);
    assertThat(pair.status, equalTo(SqlLine.Status.OTHER));
    assertThat(pair.output, not(containsString(" 123 ")));
  }

  /** Displays usage. */
  @Test
  public void testUsage() {
    Pair pair = run("--help");
    assertThat(pair.status, equalTo(SqlLine.Status.ARGS));
    assertThat(pair.output, containsString("-f <file>"));
    assertThat(countUsage(pair.output), equalTo(1));
  }

  private int countUsage(String output) {
    int n = 0;
    for (String line : output.split("\n")) {
      if (line.contains("Usage")) {
        ++n;
      }
    }
    return n;
  }

  /** Invalid arguments. */
  @Test
  public void testInvalidArguments() {
    Pair pair = run("--fuzz");
    assertThat(pair.status, equalTo(SqlLine.Status.ARGS));
    assertThat(pair.output, containsString("-f <file>"));
    assertThat(countUsage(pair.output), equalTo(1));
  }

  /** Result of executing sqlline: status code and output. */
  static class Pair {
    final SqlLine.Status status;
    final String output;

    Pair(SqlLine.Status status, String output) {
      this.status = status;
      this.output = output;
    }
  }

  @Test
  public void testIsolationSetting() {
    final String script0 = "!isolation TRANSACTION_NONE\n";
    final String expected = "Transaction isolation level TRANSACTION_NONE "
        + "is not supported. Default (TRANSACTION_READ_COMMITTED) "
        + "will be used instead.";
    checkScriptFile(script0, true, equalTo(SqlLine.Status.OTHER),
        containsString(expected));
  }

  @Test
  public void testDefaultIsolation() {
    final String script1 = "!isolation default\n";
    checkScriptFile(script1, true, equalTo(SqlLine.Status.OK),
        allOf(not(containsString("Transaction isolation level")),
            not(containsString("is not supported"))));
  }

  /**
   * HIVE-4566, "NullPointerException if typeinfo and nativesql commands are
   * executed at beeline before a DB connection is established".
   */
  @Test
  public void testNPE() {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    PrintStream sqllineOutputStream = getPrintStream(os);
    sqlLine.setOutputStream(sqllineOutputStream);
    sqlLine.setErrorStream(sqllineOutputStream);

    try {
      sqlLine.runCommands(new DispatchCallback(), "!typeinfo");
      String output = os.toString("UTF8");
      assertThat(
          output, not(containsString("java.lang.NullPointerException")));
      assertThat(output, containsString("No current connection"));

      sqlLine.runCommands(new DispatchCallback(), "!nativesql");
      output = os.toString("UTF8");
      assertThat(
          output, not(containsString("java.lang.NullPointerException")));
      assertThat(output, containsString("No current connection"));
    } catch (Exception e) {
      // fail
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testCommandHandlerOnStartup() {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    final String[] args = {
        "-e", "!set maxwidth 80",
        "-ch", "sqlline.extensions.HelloWorldCommandHandler"};
    begin(sqlLine, os, false, args);

    try {
      sqlLine.runCommands(new DispatchCallback(), "!hello");
      String output = os.toString("UTF8");
      assertThat(output, containsString("HELLO WORLD"));
      os.reset();
      sqlLine.runCommands(new DispatchCallback(), "!test");
      output = os.toString("UTF8");
      assertThat(output, containsString("HELLO WORLD"));
      os.reset();
      sqlLine.runCommands(new DispatchCallback(), "!help hello");
      output = os.toString("UTF8");
      assertThat(output, containsString("help for hello test"));
      sqlLine.runCommands(new DispatchCallback(), "!quit");
      assertTrue(sqlLine.isExit());
    } catch (Exception e) {
      // fail
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testCommandHandler() {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    begin(sqlLine, os, false, "-e", "!set maxwidth 80");

    try {
      final String script = "!commandhandler"
          + " sqlline.extensions.HelloWorld2CommandHandler"
          + " sqlline.extensions.HelloWorldCommandHandler";
      sqlLine.runCommands(new DispatchCallback(), script);
      sqlLine.runCommands(new DispatchCallback(), "!hello");
      String output = os.toString("UTF8");
      assertThat(output, containsString("HELLO WORLD2"));
      final String expected = "Could not add command handler "
          + "sqlline.extensions.HelloWorldCommandHandler as one of commands "
          + "[hello, test] is already present";
      assertThat(output, containsString(expected));
      os.reset();
      sqlLine.runCommands(new DispatchCallback(), "!help hello");
      output = os.toString("UTF8");
      assertThat(output, containsString("help for hello2"));
      sqlLine.runCommands(new DispatchCallback(), "!quit");
      assertTrue(sqlLine.isExit());
    } catch (Exception e) {
      // fail
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testTablesCsv() {
    final String script = "!set outputformat csv\n"
        + "!tables\n";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        allOf(containsString("'TABLE_CAT','TABLE_SCHEM','TABLE_NAME',"),
            containsString("'PUBLIC','SCOTT','SALGRADE','TABLE','',")));
  }

  /**
   *  java.lang.NullPointerException test case from
   *  https://github.com/julianhyde/sqlline/pull/86#issuecomment-410868361
   */
  @Test
  public void testCsvDelimiterAndQuoteCharacter() {
    final String script = "!set outputformat csv\n"
        + "!set csvDelimiter null\n"
        + "!set csvQuoteCharacter @\n"
        + "values ('#', '@#@', 1, date '1969-07-20', null, ' 1''2\"3\t4');\n"
        + "!set csvDelimiter ##\n"
        + "values ('#', '@#@', 1, date '1969-07-20', null, ' 1''2\"3\t4');\n";
    final String line1 = "@C1@null@C2@null@C3@null@C4@null@C5@null@C6@";
    final String line2 =
        "@#@null@@@#@@@null@1@null@1969-07-20@null@@null@ 1'2\"3\t4@";
    final String line3 = "@C1@##@C2@##@C3@##@C4@##@C5@##@C6@";
    final String line4 = "@#@##@@@#@@@##@1@##@1969-07-20@##@@##@ 1'2\"3\t4@";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        allOf(containsString(line1), containsString(line2),
            containsString(line3), containsString(line4)));
  }

  @Test
  public void testCsvOneLiner() {
    final String script = "!outputformat csv \"'\" @\n"
        + "values ('#', '@#@', 1, date '1969-07-20', null, ' 1''2\"3\t4');\n"
        + "!outputformat csv & default\n"
        + "values ('#', '@#@', 1, date '1969-07-20', null, ' 1''2\"3\t4');\n";
    final String line1 = "@C1@'@C2@'@C3@'@C4@'@C5@'@C6@";
    final String line2 =
        "@#@'@@@#@@@'@1@'@1969-07-20@'@@'@ 1'2\"3\t4@";
    final String line3 = "'C1'&'C2'&'C3'&'C4'&'C5'&'C6'";
    final String line4 =
        "'#'&'@#@'&'1'&'1969-07-20'&''&' 1''2\"3\t4'";

    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        allOf(containsString(line1), containsString(line2),
            containsString(line3), containsString(line4)));
  }

  @Test
  public void testSetForNulls() {
    final String script = "!set numberFormat null\n"
        + "!set\n";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        containsString("numberFormat        null"));
  }

  @Test
  public void testSetNonIntValuesToIntProperties() {
    final String headerIntervalScript = "!set headerinterval abc\n";
    checkScriptFile(headerIntervalScript, true, equalTo(SqlLine.Status.OK),
        containsString(sqlLine.loc("not-a-number", "headerinterval", "abc")));

    final String rowLimitScript = "!set rowlimit xxx\n";
    checkScriptFile(rowLimitScript, true, equalTo(SqlLine.Status.OK),
        containsString(sqlLine.loc("not-a-number", "rowlimit", "xxx")));
  }

  @Test
  public void testSelectXmlAttributes() {
    final String script = "!set outputformat xmlattr\n"
        + "values (1, -1.5, 1 = 1, date '1969-07-20', null, ']]> 1''2\"3\t<>&4');\n";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        allOf(containsString("<resultset>"),
            containsString("<result C1=\"1\" C2=\"-1.5\" C3=\"TRUE\" "
                + "C4=\"1969-07-20\" C5=\"null\" "
                + "C6=\"]]&gt; 1'2&quot;3\t&lt;>&amp;4\"/>")));
  }

  @Test
  public void testSelectXmlElements() {
    final String script = "!set outputformat xmlelements\n"
        + "values (1, -1.5, 1 = 1, date '1969-07-20', null, ' ]]>1''2\"3\t<>&4');\n";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        allOf(containsString("<resultset>"),
            containsString("<result>"),
            containsString("<C1>1</C1>"),
            containsString("<C2>-1.5</C2>"),
            containsString("<C3>TRUE</C3>"),
            containsString("<C4>1969-07-20</C4>"),
            containsString("<C5>null</C5>"),
            containsString("<C6> ]]&gt;1'2\"3\t&lt;>&amp;4</C6>"),
            containsString("</result>"),
            containsString("</resultset>")));
  }

  @Test
  public void testTablesJson() {
    final String script = "!set outputformat json\n"
        + "!tables\n";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        allOf(containsString("{\"resultset\":["),
            containsString("{\"TABLE_CAT\":\"PUBLIC\","
                + "\"TABLE_SCHEM\":\"SYSTEM_LOBS\",\"TABLE_NAME\":\"BLOCKS\","
                + "\"TABLE_TYPE\":\"SYSTEM TABLE\",\"REMARKS\":null,"
                + "\"TYPE_CAT\":null,")));
  }

  @Test
  public void testSelectJson() {
    final String script = "!set outputformat json\n"
        // two lines of values to cover case from
        // https://github.com/julianhyde/sqlline/issues/447
        + "values (1);\n"
        + "values (1, -1.5, 1 = 1, date '1969-07-20', null, ' 1''2\"3\t4');\n";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        allOf(containsString("{\"resultset\":["),
            containsString("{\"C1\":1,\"C2\":-1.5,\"C3\":true,"
                + "\"C4\":\"1969-07-20\",\"C5\":null,"
                + "\"C6\":\" 1'2\\\"3\\t4\"}")));
  }

  @Test
  public void testNullValue() {
    final String script = "!set nullValue %%%\n"
        + "!set outputformat csv\n"
        + "values (NULL, -1.5, null, date '1969-07-20', null, 'null');\n"
        + "!set nullValue \"'\"\n"
        + "values (NULL, -1.5, null, date '1969-07-20', null, 'null');\n"
        + "!set nullValue null\n"
        + "values (NULL, -1.5, null, date '1969-07-20', null, 'null');\n";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        CoreMatchers.allOf(containsString("'C1','C2','C3','C4','C5','C6'"),
            containsString("'%%%','-1.5','%%%','1969-07-20','%%%','null'"),
            containsString("'C1','C2','C3','C4','C5','C6'"),
            containsString("'''','-1.5','''','1969-07-20','''','null'"),
            containsString("'C1','C2','C3','C4','C5','C6'"),
            containsString("'null','-1.5','null','1969-07-20','null','null'")));
  }

  @Test
  public void testTimeFormat() {
    // Use System.err as it is used in sqlline.SqlLineOpts#set
    final PrintStream originalErr = System.err;
    try {
      final ByteArrayOutputStream errBaos = new ByteArrayOutputStream();
      System.setErr(
          new PrintStream(errBaos, false, StandardCharsets.UTF_8.name()));

      // successful patterns
      final String okTimeFormat = "!set timeFormat HH:mm:ss\n";
      final String defaultTimeFormat = "!set timeFormat default\n";
      final String legacyTimeFormat = "!set timeFormat ''\n";
      final String okDateFormat = "!set dateFormat YYYY-MM-dd\n";
      final String defaultDateFormat = "!set dateFormat default\n";
      final String legacyDateFormat = "!set dateFormat ''\n";
      final String okTimestampFormat = "!set timestampFormat default\n";
      final String defaultTimestampFormat = "!set timestampFormat default\n";
      final String legacyTimestampFormat = "!set timestampFormat ''\n";

      // successful cases
      sqlLine.getOpts().setPropertiesFile(DEV_NULL);
      sqlLine.runCommands(new DispatchCallback(), okTimeFormat);
      checkScriptFile(okTimeFormat, false, equalTo(SqlLine.Status.OK),
          not(
              anyOf(containsString("Error setting configuration"),
                  containsString("Exception"))));
      checkScriptFile(defaultTimeFormat, false, equalTo(SqlLine.Status.OK),
          not(
              anyOf(containsString("Error setting configuration"),
                  containsString("Exception"))));
      checkScriptFile(legacyTimeFormat, false, equalTo(SqlLine.Status.OK),
          not(
              anyOf(containsString("Error setting configuration"),
                  containsString("Exception"))));
      checkScriptFile(okDateFormat, false, equalTo(SqlLine.Status.OK),
          not(
              anyOf(containsString("Error setting configuration"),
                  containsString("Exception"))));
      checkScriptFile(defaultDateFormat, false, equalTo(SqlLine.Status.OK),
          not(
              anyOf(containsString("Error setting configuration"),
                  containsString("Exception"))));
      checkScriptFile(legacyDateFormat, false, equalTo(SqlLine.Status.OK),
          not(
              anyOf(containsString("Error setting configuration"),
                  containsString("Exception"))));
      checkScriptFile(okTimestampFormat, false, equalTo(SqlLine.Status.OK),
          not(
              anyOf(containsString("Error setting configuration"),
                  containsString("Exception"))));
      checkScriptFile(defaultTimestampFormat, false, equalTo(SqlLine.Status.OK),
          not(
              anyOf(containsString("Error setting configuration"),
                  containsString("Exception"))));
      checkScriptFile(legacyTimestampFormat, false, equalTo(SqlLine.Status.OK),
          not(
              anyOf(containsString("Error setting configuration"),
                  containsString("Exception"))));

      // failed patterns
      final String wrongTimeFormat = "!set timeFormat qwerty\n";
      final String wrongDateFormat = "!set dateFormat ASD\n";
      final String wrongTimestampFormat =
          "!set timestampFormat 'YYYY-MM-ddTHH:MI:ss'\n";

      // failed cases
      checkScriptFile(wrongTimeFormat, true, equalTo(SqlLine.Status.OTHER),
          allOf(containsString("Illegal pattern character 'q'"),
              containsString("Exception")));
      checkScriptFile(wrongDateFormat, true, equalTo(SqlLine.Status.OTHER),
          allOf(containsString("Illegal pattern character 'A'"),
              containsString("Exception")));
      checkScriptFile(wrongTimestampFormat, true, equalTo(SqlLine.Status.OTHER),
          allOf(containsString("Illegal pattern character 'T'"),
              containsString("Exception")));
    } catch (Throwable t) {
      // fail
      throw new RuntimeException(t);
    } finally {
      // Set error stream back
      System.setErr(originalErr);
    }
  }

  /**
   * Test case for
   * <a href="https://github.com/julianhyde/sqlline/issues/203">[SQLLINE-203]
   * !indexes command fails because of Hive bug</a>.
   * The bug in question is
   * <a href="https://issues.apache.org/jira/browse/HIVE-20938">[HIVE-20983]
   * HiveResultSetMetaData.getColumnDisplaySize throws for SHORT column</a>.
   *
   * @param meta Mocked hsqldb {@link JDBCResultSetMetaData} to use in the test
   */
  @Test
  public void testOutputWithFailingColumnDisplaySize(
      @Mocked final JDBCResultSetMetaData meta) {
    try {
      new Expectations() {
        {
          meta.getColumnCount();
          result = 3;

          meta.getColumnLabel(1);
          result = "TABLE_CAT";

          meta.getColumnLabel(2);
          result = "TABLE_SCHEM";

          meta.getColumnLabel(3);
          result = "TABLE_NAME";

          meta.getColumnDisplaySize(1);
          result = new Exception("getColumnDisplaySize exception");
        }
      };

      ByteArrayOutputStream os = new ByteArrayOutputStream();
      PrintStream sqllineOutputStream =
          new PrintStream(os, false, StandardCharsets.UTF_8.name());
      sqlLine.setOutputStream(sqllineOutputStream);
      sqlLine.setErrorStream(sqllineOutputStream);
      String[] args = {
          "-d",
          "org.hsqldb.jdbcDriver",
          "-u",
          "jdbc:hsqldb:res:scott",
          "-n",
          "SCOTT",
          "-p",
          "TIGER"
      };
      DispatchCallback callback = new DispatchCallback();
      sqlLine.initArgs(args, callback);
      FieldReflection.setFieldValue(
          sqlLine.getClass().getDeclaredField("initComplete"), sqlLine, true);
      sqlLine.getConnection();
      sqlLine.runCommands(callback,
          "!set incremental true",
          "!set maxwidth 80",
          "!set verbose true",
          "!indexes");
      assertThat(os.toString("UTF8"), not(containsString("Exception")));
    } catch (Throwable e) {
      // fail
      throw new RuntimeException(e);
    }
  }

  /**
   * Tests the {@code !connect} command with interactive password submission.
   */
  @Test
  public void testConnectWithoutPassword() {
    new MockUp<sqlline.Commands>() {
      @Mock
      String readPassword(String url) {
        return "";
      }
    };
    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      SqlLine.Status status =
          begin(sqlLine, os, false, "-e", "!set maxwidth 80");
      assertThat(status, equalTo(SqlLine.Status.OK));
      DispatchCallback dc = new DispatchCallback();
      sqlLine.runCommands(dc,
          "!set maxwidth 80",
          "!set maxColumnWidth 10",
          "!set incremental true");
      sqlLine.runCommands(dc, "!connect "
          + ConnectionSpec.H2.url + " "
          + ConnectionSpec.H2.username);
      sqlLine.runCommands(dc, "!tables");
      String output = os.toString("UTF8");
      final String expected = "| TABLE_CAT  | TABLE_SCHEM |"
          + " TABLE_NAME | TABLE_TYPE |  REMARKS   |  TYPE_CAT  |";
      assertThat(output, containsString(expected));
      sqlLine.runCommands(new DispatchCallback(), "!quit");
      assertTrue(sqlLine.isExit());
    } catch (Throwable t) {
      // fail
      throw new RuntimeException(t);
    }
  }

  /**
   * Tests the {@code !connect} command passing in the password in as a hash,
   * and using h2's {@code PASSWORD_HASH} property outside of the URL:
   *
   * <blockquote>
   * !connect -p PASSWORD_HASH TRUE jdbc:h2:mem sa 6e6f6e456d707479506173737764
   * </blockquote>
   */
  @Test
  public void testConnectWithDbPropertyAsParameter() {
    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      SqlLine.Status status =
          begin(sqlLine, os, false, "-e", "!set maxwidth 80");
      assertThat(status, equalTo(SqlLine.Status.OK));
      DispatchCallback dc = new DispatchCallback();
      sqlLine.runCommands(dc,
          "!set maxwidth 80",
          "!set maxColumnWidth 10\n",
          "!set incremental true");
      String fakeNonEmptyPassword = "nonEmptyPasswd";
      final byte[] bytes =
          fakeNonEmptyPassword.getBytes(StandardCharsets.UTF_8);
      sqlLine.runCommands(dc, "!connect "
          + " -p PASSWORD_HASH TRUE "
          + ConnectionSpec.H2.url + " "
          + ConnectionSpec.H2.username + " "
          + StringUtils.convertBytesToHex(bytes));
      sqlLine.runCommands(dc, "!tables");
      String output = os.toString("UTF8");
      final String expected = "| TABLE_CAT  | TABLE_SCHEM |"
          + " TABLE_NAME | TABLE_TYPE |  REMARKS   |  TYPE_CAT  |";
      assertThat(output, containsString(expected));
      sqlLine.runCommands(new DispatchCallback(), "!quit");
      assertTrue(sqlLine.isExit());
    } catch (Throwable t) {
      // fail
      throw new RuntimeException(t);
    }
  }

  /**
   * Tests the {@code !connect} command passing in the password in as a hash,
   * and using h2's {@code PASSWORD_HASH} and {@code ALLOW_LITERALS} properties
   * outside of the URL:
   *
   * <blockquote>
   * !connect -p PASSWORD_HASH TRUE -p ALLOW_LITERALS NONE
   * jdbc:h2:mem sa 6e6f6e456d707479506173737764
   * </blockquote>
   */
  @Test
  public void testConnectWithDbPropertyAllowLiteralsAsParameter() {
    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      SqlLine.Status status =
          begin(sqlLine, os, false, "-e", "!set maxwidth 80");
      assertThat(status, equalTo(SqlLine.Status.OK));
      DispatchCallback dc = new DispatchCallback();
      sqlLine.runCommands(dc, "!set maxwidth 80");
      String fakeNonEmptyPassword = "nonEmptyPasswd";
      final byte[] bytes =
          fakeNonEmptyPassword.getBytes(StandardCharsets.UTF_8);
      sqlLine.runCommands(dc, "!connect "
            + " -p PASSWORD_HASH TRUE -p ALLOW_LITERALS NONE "
            + ConnectionSpec.H2.url + " "
            + ConnectionSpec.H2.username + " "
            + StringUtils.convertBytesToHex(bytes));
      sqlLine.runCommands(dc, "select 1;");
      String output = os.toString("UTF8");
      final String expected = "Error:";
      assertThat(output, containsString(expected));
      sqlLine.runCommands(new DispatchCallback(), "!quit");
      assertTrue(sqlLine.isExit());
    } catch (Throwable t) {
      // fail
      throw new RuntimeException(t);
    }
  }

  /**
   * Tests the {@code !connect} command passing the password in as a hash:
   *
   * <blockquote>
   * !connect "jdbc:h2:mem; PASSWORD_HASH=TRUE" sa 6e6f6e456d707479506173737764
   * </blockquote>
   */
  @Test
  public void testConnectWithDbProperty() {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    SqlLine.Status status =
        begin(sqlLine, os, false, "-e", "!set maxwidth 80");
    assertThat(status, equalTo(SqlLine.Status.OK));
    DispatchCallback dc = new DispatchCallback();

    try {
      sqlLine.runCommands(dc, "!set maxwidth 80", "!set maxColumnWidth 10");

      // fail attempt
      String fakeNonEmptyPassword = "nonEmptyPasswd";
      sqlLine.runCommands(dc, "!connect \""
            + ConnectionSpec.H2.url
            + " ;PASSWORD_HASH=TRUE\" "
            + ConnectionSpec.H2.username
            + " \"" + fakeNonEmptyPassword + "\"");
      String output = os.toString("UTF8");
      final String expected0 = "Error:";
      assertThat(output, containsString(expected0));
      os.reset();

      // success attempt
      final byte[] bytes =
          fakeNonEmptyPassword.getBytes(StandardCharsets.UTF_8);
      sqlLine.runCommands(dc, "!connect \""
            + ConnectionSpec.H2.url
            + " ;PASSWORD_HASH=TRUE;ALLOW_LITERALS=NONE\" "
            + ConnectionSpec.H2.username + " \""
            + StringUtils.convertBytesToHex(bytes)
            + "\"");
      sqlLine.runCommands(dc, "!set incremental true");
      sqlLine.runCommands(dc, "!tables");
      output = os.toString("UTF8");
      final String expected1 = "| TABLE_CAT  | TABLE_SCHEM |"
          + " TABLE_NAME | TABLE_TYPE |  REMARKS   |  TYPE_CAT  |";
      assertThat(output, containsString(expected1));

      sqlLine.runCommands(dc, "select 5;");
      output = os.toString("UTF8");
      final String expected2 = "Error:";
      assertThat(output, containsString(expected2));
      os.reset();

      sqlLine.runCommands(new DispatchCallback(), "!quit");
      output = os.toString("UTF8");
      assertThat(output,
          allOf(not(containsString("Error:")), containsString("!quit")));
      assertTrue(sqlLine.isExit());
    } catch (Exception e) {
      // fail
      throw new RuntimeException(e);
    }
  }

  /**
   * Test case for
   * <a href="https://github.com/julianhyde/sqlline/issues/389">[SQLLINE-389]
   * DB specific properties not read when using !properties</a>
   *
   * Tests the {@code !properties} command passing in the password in as a hash,
   * and using h2's {@code PASSWORD_HASH} and {@code ALLOW_LITERALS} properties
   * in properties file.
   *
   * The test is similar to
   * {@link SqlLineArgsTest#testConnectWithDbPropertyAllowLiteralsAsParameter()}
   * however for another command {@code !properties}
   */
  @Test
  public void testPropertiesForDBSpecificProperties() {
    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      SqlLine.Status status =
          begin(sqlLine, os, false, "-e", "!set maxwidth 80");
      assertThat(status, equalTo(SqlLine.Status.OK));
      DispatchCallback dc = new DispatchCallback();
      sqlLine.runCommands(dc,
          "!set maxwidth 80",
          "!set incremental true");
      String fakeNonEmptyPassword = "nonEmptyPasswd";
      final byte[] bytes =
          fakeNonEmptyPassword.getBytes(StandardCharsets.UTF_8);
      final File tmpPropFile = createTempFile("prop", "temp");
      try (BufferedWriter bw =
          new BufferedWriter(
              new OutputStreamWriter(
                  new FileOutputStream(tmpPropFile),
                      StandardCharsets.UTF_8))) {
        bw.write("url=" + ConnectionSpec.H2.url + "\n"
            + "user=" + ConnectionSpec.H2.username + "\n"
            + "password=" + StringUtils.convertBytesToHex(bytes) + "\n"
            + "PASSWORD_HASH=TRUE\n"
            + "ALLOW_LITERALS=NONE\n"
        );
        bw.flush();
      }
      assertThat(status, equalTo(SqlLine.Status.OK));

      sqlLine.runCommands(dc, "!properties "
          + tmpPropFile.getAbsolutePath());
      sqlLine.runCommands(dc, "select 1;");
      String output = os.toString("UTF8");
      final String expected = "Error:";
      assertThat(output, containsString(expected));
      sqlLine.runCommands(new DispatchCallback(), "!quit");
      assertTrue(sqlLine.isExit());
    } catch (Throwable t) {
      // fail
      throw new RuntimeException(t);
    }
  }

  private static PrintStream getPrintStream(OutputStream os) {
    try {
      return new PrintStream(os, false, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      // fail
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testReconnect() {
    final String script = "!verbose true;\n!reconnect";
    final String expected = "Reconnecting to \"jdbc:hsqldb:res:scott\"...\n"
        + "Closing: org.hsqldb.jdbc.JDBCConnection";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        containsString(expected));
  }

  @Test
  public void testSchemas() {
    // Set width so we don't inherit from the current terminal.
    final String script = "!set maxwidth 80\n"
        + "!set maxcolumnwidth 15\n"
        + "!set incremental true\n"
        + "!schemas\n";
    final String line0 =
        "|   TABLE_SCHEM   |  TABLE_CATALOG  | IS_DEFAULT |";
    final String line1 = "| INFORMATION_SCHEMA | PUBLIC          | FALSE";
    final String line2 = "| PUBLIC          | PUBLIC          | TRUE       |";
    final String line3 = "| SCOTT           | PUBLIC          | FALSE      |";
    final String line4 = "| SYSTEM_LOBS     | PUBLIC          | FALSE      |";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        allOf(containsString(line0), containsString(line1),
            containsString(line2), containsString(line3),
            containsString(line4)));
  }

  @Test
  public void testTables() {
    // Set width so we don't inherit from the current terminal.
    final String script = "!set maxwidth 80\n"
        + "!set maxcolumnwidth 15\n"
        + "!set incremental true\n"
        + "!tables\n";
    final String line0 =
        "|    TABLE_CAT    |   TABLE_SCHEM   |   TABLE_NAME    |   TABLE_TYPE    |      |";
    final String line1 =
        "| PUBLIC          | SYSTEM_LOBS     | BLOCKS          | SYSTEM TABLE    |      |";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        allOf(containsString(line0), containsString(line1)));
  }

  @Test
  public void testTablesH2() {
    connectionSpec = ConnectionSpec.H2;
    // Set width so we don't inherit from the current terminal.
    final String script = "!set maxwidth 80\n"
        + "!set incremental true\n"
        + "!set maxColumnWidth 10\n"
        + "!tables\n";
    final String line0 = "| TABLE_CAT  | TABLE_SCHEM | TABLE_NAME |";
    final String line1 =
        "| UNNAMED    | INFORMATION_SCHEMA | CONSTANTS  | BASE TABLE";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        allOf(containsString(line0), containsString(line1)));
  }

  @Test
  public void testTablesWithTablePattern() {
    connectionSpec = ConnectionSpec.H2;
    // Set width so we don't inherit from the current terminal.
    final String script = "!set maxwidth 80\n"
        + "!set incremental true\n"
        + "!set maxColumnWidth 10\n"
        + "!tables INDEXES%\n";
    final String line0 =
        "| TABLE_CAT  | TABLE_SCHEM | TABLE_NAME | TABLE_TYPE |  REMARKS   |  TYPE_CAT  |\n";
    final String line1 =
        "| UNNAMED    | INFORMATION_SCHEMA | INDEXES    | BASE TABLE |            |     |\n";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        allOf(containsString(line0), containsString(line1)));
  }

  @Test
  public void testTablesWithSchemaTableType() {
    connectionSpec = ConnectionSpec.H2;
    // Set width so we don't inherit from the current terminal.
    final String script = "!set maxwidth 80\n"
        + "!set incremental true\n"
        + "!set maxColumnWidth 10\n"
        + "!tables INFORMATION_% \n";
    final String line0 =
        "| TABLE_CAT  | TABLE_SCHEM | TABLE_NAME | TABLE_TYPE |  REMARKS   |  TYPE_CAT  |\n";
    final String line1 =
        "| UNNAMED    | INFORMATION_SCHEMA | INFORMATION_SCHEMA_CATALOG_NAME | BASE TAB |\n";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        allOf(containsString(line0), containsString(line1)));
  }

  @Test
  public void testColumnsWithSchemaTableType() {
    connectionSpec = ConnectionSpec.H2;
    // Set width so we don't inherit from the current terminal.
    final String script = "!set maxwidth 80\n"
        + "!set maxColumnWidth 10\n"
        + "!set incremental true\n"
        + "!columns INFORMATION_SCHEMA % %\n";
    final String line0 =
        "| TABLE_CAT  | TABLE_SCHEM | TABLE_NAME | COLUMN_NAME | DATA_TYPE  | TYPE_NAME |\n";
    final String line1 =
        "| UNNAMED    | INFORMATION_SCHEMA | CHECK_CONSTRAINTS | CONSTRAINT_CATALOG | 1 |\n";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        allOf(containsString(line0), containsString(line1)));
  }

  @Test
  public void testColumnsWithExtraParameters() {
    connectionSpec = ConnectionSpec.H2;
    // Set width so we don't inherit from the current terminal.
    final String script = "!set maxwidth 80\n"
        + "!set incremental true\n"
        + "!columns INFORMATION% CAT% CAT% wqer\n";
    final String line0 = "Usage: ";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OTHER),
        allOf(containsString(line0)));
  }

  /**
   * Test case for
   * <a href="https://github.com/julianhyde/sqlline/issues/107">[SQLLINE-107]
   * Script fails if the wrong driver is specified with -d option
   * and there is a valid registered driver for the specified url</a>.
   *
   */
  @Test
  public void testTablesH2WithErrorDriver() {
    connectionSpec = ConnectionSpec.ERROR_H2_DRIVER;
    // Set width so we don't inherit from the current terminal.
    final String script = "!set maxwidth 80\n"
        + "!set incremental true\n"
        + "!set maxColumnWidth 10\n"
        + "!tables\n";
    final String line0 = "| TABLE_CAT  | TABLE_SCHEM | TABLE_NAME |";
    final String line1 =
        "| UNNAMED    | INFORMATION_SCHEMA | CONSTANTS  | BASE TABLE";
    final String message = "Could not find driver "
        + connectionSpec.driver
        + "; using registered driver org.h2.Driver instead";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        CoreMatchers.allOf(containsString(message),
            not(containsString("NullPointerException")),
            containsString(line0),
            containsString(line1)));
  }

  @Test
  public void testH2TablesWithErrorUrl() {
    connectionSpec = ConnectionSpec.ERROR_H2_URL;
    final String script = "!tables\n";

    checkScriptFile(script, true, equalTo(SqlLine.Status.OTHER),
        CoreMatchers.allOf(containsString("No suitable driver"),
            not(containsString("NullPointerException"))));
  }

  @Test
  public void testEmptyMetadata() {
    final String script = "!metadata\n";
    final String line = "Usage: metadata <methodname> <params...>";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OTHER),
        allOf(containsString(line), not(containsString("Exception"))));
  }

  /** Tests that methods inherited from Object are not included in the list
   * of possible metadata methods. */
  @Test
  public void testPossibleMetadataValues() {
    final String script = "!metadata 1\n";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OTHER),
        allOf(containsString("allProceduresAreCallable"),
            containsString("getIndexInfo"),
            not(containsString("Exception")),
            not(containsString("equals")),
            not(containsString("notify")),
            not(containsString("wait")),
            not(containsString("toString")),
            not(containsString("hashCode")),
            not(containsString("notifyAll"))));
  }

  @Test
  public void testMetadataForClassHierarchy() {
    new MockUp<JDBCConnection>() {
      @Mock
      public DatabaseMetaData getMetaData() throws SQLException {
        return new CustomDatabaseMetadata(
            (JDBCConnection) sqlLine.getConnection());
      }
    };

    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      SqlLine.Status status =
          begin(sqlLine, os, false, "-e", "!set maxwidth 80");
      assertThat(status, equalTo(SqlLine.Status.OK));
      DispatchCallback dc = new DispatchCallback();
      sqlLine.runCommands(dc, "!connect "
          + ConnectionSpec.HSQLDB.url + " \""
          + ConnectionSpec.HSQLDB.username + "\" \""
          + ConnectionSpec.HSQLDB.password + "\"");
      os.reset();
      sqlLine.runCommands(dc, "!tables");
      String output = os.toString("UTF8");
      assertThat(output,
          allOf(containsString("TABLE_CAT"),
              not(containsString("No such method \"getTables\""))));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testEmptyRecord() {
    final String line = "Usage: record <file name>";
    checkScriptFile("!record", true, equalTo(SqlLine.Status.OTHER),
        allOf(containsString(line), not(containsString("Exception"))));
  }

  @Test
  public void testEmptyRun() {
    final String line = "Usage: run <file name>";
    checkScriptFile("!run", true, equalTo(SqlLine.Status.OTHER),
        allOf(containsString(line), not(containsString("Exception"))));
  }

  @Test
  public void testEmptyScript() {
    final String line = "Usage: script <file name>";
    checkScriptFile("!script", true, equalTo(SqlLine.Status.OTHER),
        allOf(containsString(line), not(containsString("Exception"))));
  }

  @Test
  public void testEscapeSqlMultiline() {
    // Set width so we don't inherit from the current terminal.
    final String script = "!set maxwidth 80\n"
        + "!set maxColumnWidth 15\n"
        + "!set escapeOutput yes\n"
        + "values \n"
        + "('\n"
        + "multiline\n"
        + "value') \n"
        + ";\n";
    final String line1 = ""
        + "+-----------------+\n"
        + "|       C1        |\n"
        + "+-----------------+\n"
        + "|  \\nmultiline \\nvalue |\n"
        + "+-----------------+\n";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        containsString(line1));
  }

  @Test
  public void testSqlMultiline() {
    // Set width so we don't inherit from the current terminal.
    final String script = "!set maxwidth 80\n"
        + "!set incremental true \n"
        + "!sql \n"
        + "values \n"
        + "(1, 2) \n"
        + ";\n";
    final String line1 = ""
        + "+-------------+-------------+\n"
        + "|     C1      |     C2      |\n"
        + "+-------------+-------------+\n"
        + "| 1           | 2           |\n"
        + "+-------------+-------------+";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        containsString(line1));
  }

  @Test
  public void testAnsiConsoleOutputFormat() {
    // Set width so we don't inherit from the current terminal.
    final String script = "!set maxwidth 80\n"
        + "!set incremental true \n"
        + "!set outputformat ansiconsole \n"
        + "!all \n"
        + "values \n"
        + "(1, '2') \n"
        + ";\n";
    final String line1 = ""
        + "C1          C2\n"
        + "1           2 \n";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        containsString(line1));
  }

  @Test
  public void testAnsiConsoleFormatWithNonExistingTerminalAndSmallWidth() {
    new MockUp<SqlLine>() {
      @Mock
      LineReader getLineReader() {
        return null;
      }
    };
    final String script = "!set maxwidth 0\n"
        + "!set incremental true \n"
        + "!set outputformat ansiconsole \n"
        + "values (1, '2');\n";
    final String line1 = ""
        + "\n"
        + "\n";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        containsString(line1));
  }

  @Test
  public void testAnsiConsoleFormatWithZeroMaxWidthAndExistingTerminal() {
    try {
      new MockUp<DumbTerminal>() {
        @Mock
        public Size getSize() {
          return new Size(80, 20);
        }
      };
      final LineReaderBuilder lineReaderBuilder = LineReaderBuilder.builder()
          .parser(new SqlLineParser(sqlLine))
          .terminal(TerminalBuilder.builder().dumb(true).build());
      sqlLine.setLineReader(lineReaderBuilder.build());
      final String script = "!set maxwidth 0\n"
          + "!set incremental true \n"
          + "!set outputformat ansiconsole \n"
          + "!all \n"
          + "values \n"
          + "(1, '2') \n"
          + ";\n";
      final String line1 = ""
          + "C1          C2\n"
          + "1           2 \n";
      checkScriptFile(script, false,
          equalTo(SqlLine.Status.OK),
          containsString(line1));
    } catch (Exception e) {
      fail("Test failed with ", e);
    }
  }

  @Test
  public void testBoldHeaderSolidTableOutputFormat() {
    // Set width so we don't inherit from the current terminal.
    final String script = "!set maxwidth 80\n"
        + "!set incremental true \n"
        + "!set outputformat table\n"
        + "!set tablestyle bold_header_solid\n"
        + "!all \n"
        + "values \n"
        + "(1, '2') \n"
        + ";\n";
    final String line1 = ""
        + "┏━━━━━━━━━━━━━┳━━━━┓\n"
        + "┃     C1      ┃ C2 ┃\n"
        + "┡━━━━━━━━━━━━━╇━━━━┩\n"
        + "│ 1           │ 2  │\n"
        + "└─────────────┴────┘";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        containsString(line1));
  }

  @Test
  public void testSolidTableFormatWithNonExistingTerminalAndSmallWidth() {
    new MockUp<SqlLine>() {
      @Mock
      LineReader getLineReader() {
        return null;
      }
    };
    final String script = "!set maxwidth 0\n"
        + "!set incremental true \n"
        + "!set outputformat table\n"
        + "!set tablestyle solid\n"
        + "values (1, '2');\n";
    final String line1 = ""
        + "┌──┐\n"
        + "│  │\n"
        + "├──┤\n"
        + "│  │\n"
        + "└──┘";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        containsString(line1));
  }

  @Test
  public void testDoubleSolidTableFormatWithZeroMaxWidthAndExistingTerminal() {
    try {
      new MockUp<DumbTerminal>() {
        @Mock
        public Size getSize() {
          return new Size(80, 20);
        }
      };
      final LineReaderBuilder lineReaderBuilder = LineReaderBuilder.builder()
          .parser(new SqlLineParser(sqlLine))
          .terminal(TerminalBuilder.builder().dumb(true).build());
      sqlLine.setLineReader(lineReaderBuilder.build());
      final String script = "!set maxwidth 0\n"
          + "!set incremental true \n"
          + "!set outputformat table\n"
          + "!set tablestyle double_solid\n"
          + "!all \n"
          + "values \n"
          + "(1, '2') \n"
          + ";\n";
      final String line1 = ""
          + "╔═════════════╦════╗\n"
          + "║     C1      ║ C2 ║\n"
          + "╠═════════════╬════╣\n"
          + "║ 1           ║ 2  ║\n"
          + "╚═════════════╩════╝\n";
      checkScriptFile(script, false,
          equalTo(SqlLine.Status.OK),
          containsString(line1));
    } catch (Exception e) {
      fail("Test failed with ", e);
    }
  }

  @Test
  public void testAllMultiline() {
    // Set width so we don't inherit from the current terminal.
    final String script = "!set maxwidth 80\n"
        + "!set incremental true \n"
        + "!all \n"
        + "values \n"
        + "(1, '2') \n"
        + ";\n";
    final String line1 = ""
        + "+-------------+----+\n"
        + "|     C1      | C2 |\n"
        + "+-------------+----+\n"
        + "| 1           | 2  |\n"
        + "+-------------+----+";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        containsString(line1));
  }

  @Test
  public void testAppInfoMessage() {
    Pair pair = run("-e", "!set maxwidth 80");
    assertThat(pair.status, equalTo(SqlLine.Status.OK));
    assertThat(pair.output,
        containsString(Application.DEFAULT_APP_INFO_MESSAGE));

    String[] args = {"-e", "!set maxwidth 80", "-ac", "INCORRECT_CLASS_NAME"};
    pair = run(args);
    assertThat(pair.output,
        containsString("Could not initialize INCORRECT_CLASS_NAME"));
    assertThat(pair.output,
        containsString(CustomApplication.DEFAULT_APP_INFO_MESSAGE));

    String[] args2 = {"-e", "!set maxwidth 80",
        "-ac", "sqlline.extensions.CustomApplication"};
    pair = run(args2);
    assertThat(pair.output,
        containsString(CustomApplication.CUSTOM_INFO_MESSAGE));
  }

  @Test
  public void testCustomOutputFormats() {
    // json format was removed
    final String script = "!appconfig"
        + " sqlline.extensions.CustomApplication\n"
        + "!set outputformat json\n"
        + "values 1;";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        containsString("Unknown outputFormat \"json\""));
  }

  @Test
  public void testCustomCommands() {
    // table command was removed
    final String script = "!appconfig"
        + " sqlline.extensions.CustomApplication\n"
        + "!tables";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OTHER),
        containsString("Unknown command: tables"));
  }

  @Test
  public void testAppConfigReset() {
    final String script = "!appconfig"
        + " sqlline.extensions.CustomApplication\n"
        + "!appconfig sqlline.Application\n"
        + "!tables";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        containsString("TABLE_CAT"));
  }

  @Test
  public void testCustomOpts() {
    // nulls are displayed as custom_null
    final String script = "!appconfig"
        + " sqlline.extensions.CustomApplication\n"
        + "values(null)";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        containsString("custom_null"));
  }

  @Test
  public void testScanForEmptyAllowedDrivers() {
    new MockUp<CustomApplication>() {
      @Mock
      public List<String> allowedDrivers() {
        return Collections.emptyList();
      }
    };

    final String script = "!appconfig"
        + " sqlline.extensions.CustomApplication\n"
        + "!scan";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        containsString("No driver classes found"));
  }

  @Test
  public void testScanForOnlyH2Allowed() {
    new MockUp<CustomApplication>() {
      @Mock
      public List<String> allowedDrivers() {
        return Collections.singletonList("org.h2.Driver");
      }
    };

    final String script = "!appconfig"
        + " sqlline.extensions.CustomApplication\n"
        + "!scan";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        allOf(containsString("yes       2.1     org.h2.Driver"),
                not(containsString("org.hsqldb.jdbc.JDBCDriver"))));
  }

  @Test
  public void testScanForOnlyHsqldbAllowed() {
    new MockUp<CustomApplication>() {
      @Mock
      public List<String> allowedDrivers() {
        return Collections.singletonList("org.hsqldb.jdbc.JDBCDriver");
      }
    };

    final String script = "!appconfig"
        + " sqlline.extensions.CustomApplication\n"
        + "!scan";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        allOf(containsString("yes       2.5     org.hsqldb.jdbc.JDBCDriver"),
            not(containsString("org.h2.Driver"))));
  }

  @Test
  public void testVersion() {
    final String script = "!set\n";
    try {
      checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
          containsString(new Application().getVersion()));
    } catch (Exception e) {
      // fail
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testSetVersion() {
    final String script = "!set version test-version\n";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OTHER),
        containsString("version property is read only"));
  }

  @Test
  public void testSetPropertySuccess() {
    final String script = "!set timeout\n"
        + "!set timeout 200\n"
        + "!set timeout";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        allOf(containsString("timeout             -1"),
            containsString("timeout             200")));
  }

  @Test
  public void testSetPropertyFailure() {
    final String script = "!set unk";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OTHER),
        containsString("Specified property [unk] does not exist."
            + " Use !set command to get list of all available properties."));
  }

  @Test
  public void testSetUsage() {
    final String script = "!set 1 2 3";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OTHER),
        containsString("Usage: set [all | <property name> [<value>]]"));
  }

  @Test
  public void testSetWrongValueToEnumTypeProperties() {
    Collection<BuiltInProperty> propertiesWithAvailableValues =
        Arrays.stream(BuiltInProperty.values())
            .filter(t -> !t.getAvailableValues().isEmpty())
            .collect(Collectors.toSet());
    final String wrongValue = "wrong_value";
    for (BuiltInProperty property: propertiesWithAvailableValues) {
      final String script =
          "!set " + property.propertyName() + " " + wrongValue + "\n";
      checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
          allOf(
              containsString("Unknown " + property.propertyName() + " \""
                  + wrongValue + "\""),
              not(containsString("Exception"))));
    }
  }

  @Test
  public void testResetSuccess() {
    final String script = "!set timeout 200\n"
        + "!reset timeout";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        containsString("[timeout] was reset to [-1]"));
  }

  @Test
  public void testResetFailure() {
    final String script = "!reset unk";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OTHER),
        containsString("Specified property [unk] does not exist."
            + " Use !set command to get list of all available properties."));
  }

  @Test
  public void testResetAll() {
    final String script = "!set timeout 200\n"
        + "!reset all\n"
        + "!set timeout";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        allOf(containsString("All properties were reset to their defaults."),
            containsString("timeout             -1")));
  }

  @Test
  public void testResetUsage() {
    final String script = "!reset";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OTHER),
        containsString("Usage: reset (all | <property name>)"));
  }

  @Test
  public void testRerun() {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    final File tmpHistoryFile = createTempFile("tmpHistory", "temp");
    try (BufferedWriter bw =
             new BufferedWriter(
                 new OutputStreamWriter(
                     new FileOutputStream(tmpHistoryFile),
                     StandardCharsets.UTF_8))) {
      bw.write("1536743099591:SELECT \\n CURRENT_TIMESTAMP \\n as \\n c1;\n"
          + "1536743104551:!/ 4\n"
          + "1536743104551:!/ 5\n"
          + "1536743104551:!/ 2\n"
          + "1536743104551:!/ 7\n"
          + "1536743107526:!history\n"
          + "1536743115431:!/ 3\n"
          + "1536743115431:!/ 8\n");
      bw.flush();

      SqlLine.Status status = begin(sqlLine, os, true,
          "--historyfile=" + tmpHistoryFile.getAbsolutePath(),
          "-e", "!set maxwidth 80");
      assertThat(status, equalTo(SqlLine.Status.OK));
      DispatchCallback dc = new DispatchCallback();
      sqlLine.runCommands(dc,
          "!set incremental true",
          "!set maxcolumnwidth 30",
          "!connect "
              + ConnectionSpec.H2.url + " "
              + ConnectionSpec.H2.username + " "
              + "\"\"");
      os.reset();

      sqlLine.runCommands(dc, "!/ 1");
      String output = os.toString("UTF8");
      final String expected0 = "+----------------------------";
      final String expected1 = "C1";
      final String expected2 = "1 row selected";
      assertThat(output,
          allOf(containsString(expected0),
              containsString(expected1),
              containsString(expected2)));
      os.reset();

      sqlLine.runCommands(dc, "!/ 4");
      output = os.toString("UTF8");
      String expectedLine3 = "Cycled rerun of commands from history [2, 4]";
      assertThat(output, containsString(expectedLine3));
      os.reset();

      sqlLine.runCommands(dc, "!/ 3");
      output = os.toString("UTF8");
      String expectedLine4 = "Cycled rerun of commands from history [3, 5, 7]";
      assertThat(output, containsString(expectedLine4));
      os.reset();

      sqlLine.runCommands(dc, "!/ 8");
      output = os.toString("UTF8");
      String expectedLine5 = "Cycled rerun of commands from history [8]";
      assertThat(output, containsString(expectedLine5));
      os.reset();

      sqlLine.runCommands(dc, "!rerun " + Integer.MAX_VALUE);
      output = os.toString("UTF8");
      String expectedLine6 =
          "Usage: rerun <offset>, available range of offset is -7..8";
      assertThat(output, containsString(expectedLine6));
      sqlLine.runCommands(new DispatchCallback(), "!quit");
      assertTrue(sqlLine.isExit());
    } catch (Exception e) {
      // fail
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testMaxColumnWidthForLongString() {
    final String script1 = "!set maxcolumnwidth 30\n"
        + "!set incremental true\n"
        + "values ('This is a very long string\n"
        + "This is a very long string\n"
        + "This is a very long string')";
    final String line1 = ""
        + "+--------------------------------+\n"
        + "|               C1               |\n"
        + "+--------------------------------+\n"
        + "| This is a very long string \n"
        + "This is a very long string \n"
        + "This is a very long  |\n"
        + "+--------------------------------+";
    checkScriptFile(script1, true, equalTo(SqlLine.Status.OK),
        containsString(line1));
  }

  @Test
  public void testMaxColumnWidthIncremental() {
    final String script1 = "!set maxcolumnwidth -1\n"
            + "!set incremental true\n"
            + "values (100, 200)";
    final String line1 = ""
            + "+-------------+-------------+\n"
            + "|     C1      |     C2      |\n"
            + "+-------------+-------------+\n"
            + "| 100         | 200         |\n"
            + "+-------------+-------------+";
    checkScriptFile(script1, true, equalTo(SqlLine.Status.OK),
            containsString(line1));

    final String script2 = "!set maxcolumnwidth 0\n"
            + "!set incremental true\n"
            + "values (100, 200)";
    final String line2 = ""
            + "+-------------+-------------+\n"
            + "|     C1      |     C2      |\n"
            + "+-------------+-------------+\n"
            + "| 100         | 200         |\n"
            + "+-------------+-------------+";
    checkScriptFile(script2, true, equalTo(SqlLine.Status.OK),
            containsString(line2));

    final String script3 = "!set maxcolumnwidth 5000\n"
            + "!set incremental true\n"
            + "values (100, 200)";
    final String line3 = ""
            + "+-------------+-------------+\n"
            + "|     C1      |     C2      |\n"
            + "+-------------+-------------+\n"
            + "| 100         | 200         |\n"
            + "+-------------+-------------+";
    checkScriptFile(script3, true, equalTo(SqlLine.Status.OK),
            containsString(line3));

    final String script4 = "!set maxcolumnwidth 2\n"
            + "!set incremental true\n"
            + "values (100, 200)";
    final String line4 = ""
            + "+----+----+\n"
            + "| C1 | C2 |\n"
            + "+----+----+\n"
            + "| 100 | 200 |\n"
            + "+----+----+";
    checkScriptFile(script4, true, equalTo(SqlLine.Status.OK),
            containsString(line4));
  }

  @Test
  public void testMaxColumnWidthBuffered() {
    final String script1 = "!set maxcolumnwidth -1\n"
            + "values (100, 200)";
    final String line1 = ""
            + "+-----+-----+\n"
            + "| C1  | C2  |\n"
            + "+-----+-----+\n"
            + "| 100 | 200 |\n"
            + "+-----+-----+";
    checkScriptFile(script1, true, equalTo(SqlLine.Status.OK),
            containsString(line1));

    final String script2 = "!set maxcolumnwidth 0\n"
            + "values (100, 200)";
    final String line2 = ""
            + "+-----+-----+\n"
            + "| C1  | C2  |\n"
            + "+-----+-----+\n"
            + "| 100 | 200 |\n"
            + "+-----+-----+";
    checkScriptFile(script2, true, equalTo(SqlLine.Status.OK),
            containsString(line2));

    final String script3 = "!set maxcolumnwidth 5000\n"
            + "values (100, 200)";
    final String line3 = ""
            + "+-----+-----+\n"
            + "| C1  | C2  |\n"
            + "+-----+-----+\n"
            + "| 100 | 200 |\n"
            + "+-----+-----+";
    checkScriptFile(script3, true, equalTo(SqlLine.Status.OK),
            containsString(line3));

    final String script4 = "!set maxcolumnwidth 2\n"
            + "values (100, 200)";
    final String line4 = ""
            + "+----+----+\n"
            + "| C1 | C2 |\n"
            + "+----+----+\n"
            + "| 100 | 200 |\n"
            + "+----+----+";
    checkScriptFile(script4, true, equalTo(SqlLine.Status.OK),
            containsString(line4));
  }

  /** Tests with incrementalBufferRows = -1; query stays in non-incremental
   * mode. */
  @Test
  public void testIncrementalBufferRows() {
    final String script1 = "!set incrementalBufferRows -1\n"
        + "select * from (values (10, 20), (1, 2), (111, 2), (11, 2222))";
    final String line1 = ""
        + "+-----+------+\n"
        + "| C1  |  C2  |\n"
        + "+-----+------+\n"
        + "| 10  | 20   |\n"
        + "| 1   | 2    |\n"
        + "| 111 | 2    |\n"
        + "| 11  | 2222 |\n"
        + "+-----+------+";
    checkScriptFile(script1, true, equalTo(SqlLine.Status.OK),
        containsString(line1));
  }

  /** Tests with incrementalBufferRows = 0; query goes straight into
   * incremental mode. */
  @Test
  public void testIncrementalBufferRows2() {
    final String script2 = "!set incrementalBufferRows 0\n"
        + "select * from (values (1, 2), (1, 20), (111, 20), (11, 2222), (1, 22))";
    final String line2 = ""
        + "+----+----+\n"
        + "| C1 | C2 |\n"
        + "+----+----+\n"
        + "| 1  | 2  |\n"
        + "| 1  | 20 |\n"
        + "| 111 | 20 |\n"
        + "| 11  | 2222 |\n"
        + "| 1   | 22   |\n"
        + "+----+----+";
    checkScriptFile(script2, true, equalTo(SqlLine.Status.OK),
        containsString(line2));
  }

  /** Tests with incrementalBufferRows = 1; query starts in non-incremental mode
   * and switches to incremental after 1 row. */
  @Test
  public void testIncrementalBufferRows3() {
    final String script3 = "!set incrementalBufferRows 1\n"
        + "select * from (values (10, 20), (1, 2), (111, 2), (11, 2222), (111, 2222))";
    final String line3 = ""
        + "+----+----+\n"
        + "| C1 | C2 |\n"
        + "+----+----+\n"
        + "| 10 | 20 |\n"
        + "| 1  | 2  |\n"
        + "| 111 | 2  |\n"
        + "| 11  | 2222 |\n"
        + "| 111 | 2222 |\n"
        + "+----+----+";
    checkScriptFile(script3, true, equalTo(SqlLine.Status.OK),
        containsString(line3));
  }

  /** Tests with incrementalBufferRows = 4; query starts in non-incremental mode
   * and rows run out before the limit is reached. */
  @Test
  public void testIncrementalBufferRows4() {
    final String script4 = "!set incrementalBufferRows 4\n"
        + "select * from (values (10, 20), (1, 2), (111, 2), (11, 2222))";
    final String line4 = ""
        + "+-----+------+\n"
        + "| C1  |  C2  |\n"
        + "+-----+------+\n"
        + "| 10  | 20   |\n"
        + "| 1   | 2    |\n"
        + "| 111 | 2    |\n"
        + "| 11  | 2222 |\n"
        + "+-----+------+";
    checkScriptFile(script4, true, equalTo(SqlLine.Status.OK),
        containsString(line4));
  }

  @Test
  public void testIncrementalBufferRows5() {
    // With incrementalBufferRows default (1000), query starts and
    // stays in incremental mode.
    final String script5 = "!set incrementalBufferRows default\n"
        + "select * from (values (10, 20), (1, 2))";
    final String line5 = ""
        + "+----+----+\n"
        + "| C1 | C2 |\n"
        + "+----+----+\n"
        + "| 10 | 20 |\n"
        + "| 1  | 2  |\n"
        + "+----+----+";
    checkScriptFile(script5, true, equalTo(SqlLine.Status.OK),
        containsString(line5));
  }

  @Test
  public void testMaxHistoryFileRows() {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    final File tmpHistoryFile = createTempFile("tmpHistory", "temp");
    try (BufferedWriter bw =
             new BufferedWriter(
                 new OutputStreamWriter(
                     new FileOutputStream(tmpHistoryFile),
                     StandardCharsets.UTF_8))) {
      bw.write("1536743099591:SELECT \\n CURRENT_TIMESTAMP \\n as \\n c1;\n"
          + "1536743104551:SELECT \\n 'asd' as \"sdf\", 4 \\n \\n as \\n c2;\\n\n"
          + "1536743104551:SELECT \\n 'asd' \\n as \\n c2;\\n\n"
          + "1536743104551:!/ 2\n"
          + "1536743104551:SELECT \\n 2123 \\n as \\n c2 from dual;\\n\n"
          + "1536743107526:!history\n"
          + "1536743115431:SELECT \\n 2 \\n as \\n c2;\n"
          + "1536743115431:SELECT \\n '213' \\n as \\n c1;\n"
          + "1536743115431:!/ 8\n");
      bw.flush();
      bw.close();

      SqlLine.Status status = begin(sqlLine, os, true,
          "--historyfile=" + tmpHistoryFile.getAbsolutePath(),
          "-e", "!set maxwidth 80");
      assertThat(status, equalTo(SqlLine.Status.OK));
      DispatchCallback dc = new DispatchCallback();

      final int maxLines = 3;
      sqlLine.runCommands(dc, "!set maxHistoryFileRows " + maxLines);
      os.reset();
      sqlLine.runCommands(dc, "!history");
      assertEquals(maxLines + 1,
          os.toString("UTF8").split("\\s+\\d{2}:\\d{2}:\\d{2}\\s+").length);
      os.reset();
    } catch (Exception e) {
      // fail
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testSave() {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    final String testSqllinePropertiesFile = "test.sqlline.properties";
    try {
      SqlLine.Status status = begin(sqlLine, os, false,
          "--propertiesFile=" + testSqllinePropertiesFile, "-e", "!save");
      assertThat(status, equalTo(SqlLine.Status.OK));
      final DispatchCallback dc = new DispatchCallback();

      assertThat(os.toString("UTF8"),
          allOf(containsString("Saving preferences to"),
              not(containsString("Saving to /dev/null not supported"))));
      os.reset();
      sqlLine.runCommands(dc, "!set");
      assertThat(os.toString("UTF8"),
          allOf(containsString("autoCommit"),
              not(containsString("Unknown property:"))));
      os.reset();
      Files.delete(Paths.get(testSqllinePropertiesFile));
    } catch (Exception e) {
      // fail
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testSaveToDevNull() {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    try {
      SqlLine.Status status = begin(sqlLine, os, false,
          "--propertiesFile=/dev/null",
          "-e", "!save");
      assertThat(status, equalTo(SqlLine.Status.OK));
      final DispatchCallback dc = new DispatchCallback();

      assertThat(os.toString("UTF8"),
          allOf(containsString("Saving preferences to"),
              containsString("Saving to /dev/null not supported")));
      os.reset();
      sqlLine.runCommands(dc, "!set");
      assertThat(os.toString("UTF8"),
          allOf(containsString("autoCommit"),
              not(containsString("Unknown property:"))));
      os.reset();
    } catch (Exception e) {
      // fail
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testScript() {
    final File file = createTempFile("sqlline", ".script");
    final String script = "!script " + file.getAbsolutePath() + "\n"
        + "values (100, 200);\n"
        + "!set maxcolumnwidth -1\n"
        + "!script\n"
        + "values (1, 2);\n";
    final String expected = "(?s)1/5          !script.*"
        + "2/5          values \\(100, 200\\);.*"
        + "3/5          !set maxcolumnwidth -1.*"
        + "4/5          !script.*"
        + "5/5          values \\(1, 2\\);.*";
    checkScriptFile(script, false, equalTo(SqlLine.Status.OK),
        RegexMatcher.of(expected));

    final String output = "values \\(100, 200\\);\n"
        + "!set maxcolumnwidth -1\n";
    assertFileContains(file, RegexMatcher.of(output));
  }

  @Test
  public void testStartupArgsWithoutUrl() {
    try {
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      PrintStream sqllineOutputStream =
          new PrintStream(os, false, StandardCharsets.UTF_8.name());
      sqlLine.setOutputStream(sqllineOutputStream);
      sqlLine.setErrorStream(sqllineOutputStream);
      String[] args = {
          "-d",
          "org.hsqldb.jdbcDriver",
          "-n",
          "SCOTT",
          "-p",
          "TIGER"
      };
      DispatchCallback callback = new DispatchCallback();
      sqlLine.initArgs(args, callback);
      assertThat(os.toString("UTF8"), containsString(sqlLine.loc("no-url")));
    } catch (Throwable e) {
      // fail
      throw new RuntimeException(e);
    }
  }

  /**
   * Tests setting the "confirm" property.
   */
  @Test
  public void testConfirmFlag() {
    try {
      final ByteArrayOutputStream os = new ByteArrayOutputStream();
      assertThat(sqlLine.getOpts().getConfirm(), is(false));
      begin(sqlLine, os, false, "-e", "!set confirm true");
      assertThat(sqlLine.getOpts().getConfirm(), is(true));
      begin(sqlLine, os, false, "-e", "!set confirm false");
      assertThat(sqlLine.getOpts().getConfirm(), is(false));
    } catch (Throwable e) {
      // fail
      throw new RuntimeException(e);
    }
  }

  /**
   * Tests that the "confirm" property works.
   */
  @Test
  public void testConfirmFlagEffects() {
    new MockUp<sqlline.Commands>() {
      @Mock
      int getUserAnswer(String question, int... allowedAnswers) {
        return 'n';
      }
    };

    assertThat(sqlLine.getOpts().getConfirm(), is(false));
    assertThat(sqlLine.getOpts().getConfirmPattern(),
        is(BuiltInProperty.CONFIRM_PATTERN.defaultValue()));
    String script = "CREATE TABLE dummy_test(pk int);\n"
        + "INSERT INTO dummy_test(pk) VALUES(1);\n"
        + "INSERT INTO dummy_test(pk) VALUES(2);\n"
        + "INSERT INTO dummy_test(pk) VALUES(3);\n"
        + "DELETE FROM dummy_test;\n"
        + "DROP TABLE dummy_test;\n";
    checkScriptFile(script, true,
        equalTo(SqlLine.Status.OK),
        containsString(" "));

    script = "!set confirm true\n" + script;
    checkScriptFile(script, true,
        equalTo(SqlLine.Status.OTHER),
        containsString(sqlLine.loc("abort-action")));
  }

  /**
   * Tests that the "confirmPattern" property works.
   */
  @Test
  public void testConfirmPattern() {
    assertThat(sqlLine.getOpts().getConfirm(), is(false));
    final ByteArrayOutputStream os = new ByteArrayOutputStream();

    begin(sqlLine, os, false, "-e",
        "!set confirmPattern \"^(?i:(TRUNCATE|ALTER))\"");
    assertThat(sqlLine.getOpts().getConfirmPattern(),
        is("^(?i:(TRUNCATE|ALTER))"));

    begin(sqlLine, os, false, "-e", "!set confirmPattern default");
    assertThat(sqlLine.getOpts().getConfirmPattern(),
        is(sqlLine.loc("default-confirm-pattern")));
  }

  /**
   * Test for illegal regex in confirmPattern
   */
  @Test
  public void testExceptionConfirmPattern() {
    try {
      final ByteArrayOutputStream os = new ByteArrayOutputStream();
      assertThat(sqlLine.getOpts().getConfirm(), is(false));
      SqlLine.Status status =
          begin(sqlLine, os, false, "-e",
              "!set confirmPattern \"^(?i*:(TRUNCATE|ALTER))\"");
      assertThat(status, equalTo(SqlLine.Status.OTHER));
      assertThat(os.toString("UTF8"), containsString("invalid regex"));
      os.reset();
    } catch (Throwable e) {
      // fail
      throw new RuntimeException(e);
    }
  }

  @ParameterizedTest
  @MethodSource("commandProvider")
  public void testErrorStatusForSingleLineExecution(
      String line, String expectedOutput, SqlLine.Status expectedStatus) {
    try {
      final ByteArrayOutputStream os = new ByteArrayOutputStream();
      SqlLine.Status status =
          begin(sqlLine, os, false, "-e", line);
      assertEquals(status, expectedStatus);
      assertThat(os.toString("UTF8"), containsString(expectedOutput));
      os.reset();
    } catch (Throwable e) {
      // fail
      fail(e);
    }
  }

  static Stream<Arguments> commandProvider() {
    return Stream.of(
        of("!set non-existing-property test",
            "does not exist", SqlLine.Status.OTHER),
        of("!non-existing-command", "Unknown command", SqlLine.Status.OTHER),

        // OK
        of("!set confirmPattern \"^(?i:(TRUNCATE|ALTER))\"",
            "", SqlLine.Status.OK),
        of("!history", "", SqlLine.Status.OK),
        of("!quit", "", SqlLine.Status.OK)
    );
  }

  @Test
  public void testInitArgsForUserNameAndPasswordWithSpaces() {
    try {
      final DatabaseConnection[] databaseConnection = new DatabaseConnection[1];
      new MockUp<sqlline.DatabaseConnections>() {
        @Mock
        public void setConnection(DatabaseConnection connection) {
          databaseConnection[0] = connection;
        }
      };

      ByteArrayOutputStream os = new ByteArrayOutputStream();
      String[] connectionArgs = new String[]{
          "-u", ConnectionSpec.H2.url, "-n", "\"user'\\\" name\"",
          "-p", "\"user \\\"'\\\"password\"",
          "-e", "!set maxwidth 80"};
      begin(sqlLine, os, false, connectionArgs);

      assertEquals(ConnectionSpec.H2.url, databaseConnection[0].getUrl());
      Properties infoProperties =
          FieldReflection.getFieldValue(
              databaseConnection[0].getClass().getDeclaredField("info"),
              databaseConnection[0]);
      assertNotNull(infoProperties);
      assertEquals("user'\" name", infoProperties.getProperty("user"));
      assertEquals("user \"'\"password",
          infoProperties.getProperty("password"));
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  @Test
  public void testInitArgsForSuccessConnection() {
    try {
      final String[] nicknames = new String[1];
      new MockUp<sqlline.DatabaseConnection>() {
        @Mock
        void setNickname(String nickname) {
          nicknames[0] = nickname;
        }
      };
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      final String filename = "file' with spaces";
      String[] connectionArgs = {
          "-u", ConnectionSpec.H2.url,
          "-n", ConnectionSpec.H2.username,
          "-p", ConnectionSpec.H2.password,
          "-nn", "nickname with spaces",
          "-log", "target" + File.separator + filename,
          "-e", "!set maxwidth 80"};
      begin(sqlLine, os, false, connectionArgs);

      assertThat("file with spaces",
          Files.exists(Paths.get("target", filename)));
      assertEquals("nickname with spaces", nicknames[0]);
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  @Test
  public void testInitArgsForSuccessConnectionWithUserPassInUrl() {
    try {
      final ByteArrayOutputStream os = new ByteArrayOutputStream();
      final String[] connectionArgs = {
          "-u", ConnectionSpec.H2.url
              + ";user=" + ConnectionSpec.H2.username
              + ";password=" + ConnectionSpec.H2.password,
          "-e", "!set maxwidth 80"};
      begin(sqlLine, os, false, connectionArgs);
      assertThat(os.toString("UTF8"),
          not(containsString("Duplicate property")));
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  @Test
  public void testInitArgsForSuccessConnectionWithUserInUrl() {
    try {
      new MockUp<sqlline.Commands>() {
        @Mock
        String readUsername(String url) {
          return ConnectionSpec.H2.username;
        }
      };
      final ByteArrayOutputStream os = new ByteArrayOutputStream();
      final String[] connectionArgs = {
          "-u", ConnectionSpec.H2.url
              + ";user=" + ConnectionSpec.H2.username,
          "-p", ConnectionSpec.H2.password,
          " --connectInteractiveModes=useEmptyCredentials",
          "-e", "!set maxwidth 80"};
      begin(sqlLine, os, false, connectionArgs);
      assertThat(os.toString("UTF8"),
          allOf(not(containsString("Duplicate property")),
              not(containsString(">...."))));
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  @Test
  public void testInitArgsWithInteractiveAskForUserPassword() {
    try {
      new MockUp<sqlline.Commands>() {
        @Mock
        String readUsername(String url) {
          return ConnectionSpec.H2.username;
        }
      };
      new MockUp<sqlline.Commands>() {
        @Mock
        String readPassword(String url) {
          return ConnectionSpec.H2.password;
        }
      };
      final ByteArrayOutputStream os = new ByteArrayOutputStream();
      final String[] connectionArgs = {
          "-u", ConnectionSpec.H2.url,
          "-e", "!set maxwidth 80"};
      begin(sqlLine, os, false, connectionArgs);
      assertThat(os.toString("UTF8"),
          allOf(not(containsString("Duplicate property")),
              not(containsString(">....")),
              not(containsString("Usage:"))));
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  @Test
  public void testInitArgsWithInteractiveAskForPassword() {
    try {
      new MockUp<sqlline.Commands>() {
        @Mock
        String readPassword(String url) {
          return ConnectionSpec.H2.password;
        }
      };
      final ByteArrayOutputStream os = new ByteArrayOutputStream();
      final String[] connectionArgs = {
          "-u", ConnectionSpec.H2.url,
          "-n", ConnectionSpec.H2.username,
          "-e", "!set maxwidth 80"};
      begin(sqlLine, os, false, connectionArgs);
      assertThat(os.toString("UTF8"),
          allOf(not(containsString("Duplicate property")),
              not(containsString("Usage:"))));
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  @Test
  public void testInitArgsWithInteractiveAskForUser() {
    try {
      new MockUp<sqlline.Commands>() {
        @Mock
        String readUsername(String url) {
          return ConnectionSpec.H2.username;
        }
      };
      final ByteArrayOutputStream os = new ByteArrayOutputStream();
      final String[] connectionArgs = {
          "-u", ConnectionSpec.H2.url,
          "-p", ConnectionSpec.H2.password,
          "-e", "!set maxwidth 80"};
      begin(sqlLine, os, false, connectionArgs);
      assertThat(os.toString("UTF8"),
          allOf(not(containsString("Duplicate property")),
              not(containsString("Usage:"))));
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  @Test
  public void testConnectForSavedConnections() {
    final DatabaseConnection[] databaseConnection = new DatabaseConnection[1];
    new MockUp<sqlline.DatabaseConnections>() {
      @Mock
      public void setConnection(DatabaseConnection connection) {
        databaseConnection[0] = connection;
      }
    };

    ByteArrayOutputStream os = new ByteArrayOutputStream();
    final File tmpSavedConnections = createTempFile(
        "tmpconfconnections", "temp");
    try (BufferedWriter bw =
        new BufferedWriter(
            new OutputStreamWriter(
                new FileOutputStream(tmpSavedConnections),
                    StandardCharsets.UTF_8))) {
      String connectionName = "myconnection";
      bw.write(connectionName + ": \n"
          + "  url: " + ConnectionSpec.H2.url + "\n"
          + "  user: " + ConnectionSpec.H2.username + "\n"
          + "  password: " + ConnectionSpec.H2.password + "\n");
      bw.flush();
      bw.close();

      String[] connectionArgs = new String[]{
          "-c", connectionName,
          "--connectionConfig=" + tmpSavedConnections.getAbsolutePath(),
          "-e", "!set maxwidth 80"};
      SqlLine.Status status = begin(sqlLine, os, false, connectionArgs);
      assertThat(status, equalTo(SqlLine.Status.OK));
      assertEquals(ConnectionSpec.H2.url, databaseConnection[0].getUrl());
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  @Test
  public void testDropAll() {
    try {
      new MockUp<sqlline.Commands>() {
        @Mock
        int getUserAnswer(String question, int... allowedAnswers) {
          return 'y';
        }
      };
      assertThat(sqlLine.getOpts().getConfirm(), is(false));
      assertThat(sqlLine.getOpts().getConfirmPattern(),
          is(BuiltInProperty.CONFIRM_PATTERN.defaultValue()));
      final String createScript = "CREATE SCHEMA TESTDROPALL_1;\n"
          + "CREATE SCHEMA TESTDROPALL_2;\n"
          + "CREATE SCHEMA TESTDROPALL_3;\n"
          + "CREATE TABLE TESTDROPALL_1.TABLE_1(pk int);\n"
          + "CREATE TABLE TESTDROPALL_1.TABLE_2(pk int);\n"
          + "CREATE TABLE TESTDROPALL_2.TABLE_1(pk int);\n"
          + "CREATE TABLE TESTDROPALL_2.TABLE_2(pk int);\n"
          + "CREATE TABLE TESTDROPALL_3.TABLE_1(pk int);\n"
          + "CREATE TABLE TESTDROPALL_3.TABLE_2(pk int);\n";
      checkScriptFile(createScript, true,
          equalTo(SqlLine.Status.OK),
          containsString(" "));
      final String showTables = "!tables";
      checkScriptFile(showTables, true, equalTo(SqlLine.Status.OK),
          allOf(containsString("TESTDROPALL_1"),
              containsString("TESTDROPALL_2"),
              containsString("TESTDROPALL_3")));
      String dropScript = "!dropall TESTDROPALL%\n";
      checkScriptFile(dropScript, true,
          equalTo(SqlLine.Status.OK),
          containsString(" "));

      checkScriptFile(showTables, true, equalTo(SqlLine.Status.OK),
          allOf(containsString("INFORMATION_SCHEMA"),
              not(containsString("TESTDROPALL_1")),
              not(containsString("TESTDROPALL_2")),
              not(containsString("TESTDROPALL_3"))));
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  @Test
  public void testDropAllForSpecificSchema() {
    try {
      new MockUp<sqlline.Commands>() {
        @Mock
        int getUserAnswer(String question, int... allowedAnswers) {
          return 'y';
        }
      };
      assertThat(sqlLine.getOpts().getConfirm(), is(false));
      assertThat(sqlLine.getOpts().getConfirmPattern(),
          is(BuiltInProperty.CONFIRM_PATTERN.defaultValue()));
      final String createScript = "CREATE SCHEMA TESTDROPALL_SPECIFIC_1;\n"
          + "CREATE SCHEMA TESTDROPALL_SPECIFIC_2;\n"
          + "CREATE SCHEMA TESTDROPALL_SPECIFIC_3;\n"
          + "CREATE TABLE TESTDROPALL_SPECIFIC_1.TABLE_1(pk int);\n"
          + "CREATE TABLE TESTDROPALL_SPECIFIC_1.TABLE_2(pk int);\n"
          + "CREATE TABLE TESTDROPALL_SPECIFIC_2.TABLE_1(pk int);\n"
          + "CREATE TABLE TESTDROPALL_SPECIFIC_2.TABLE_2(pk int);\n"
          + "CREATE TABLE TESTDROPALL_SPECIFIC_3.TABLE_1(pk int);\n"
          + "CREATE TABLE TESTDROPALL_SPECIFIC_3.TABLE_2(pk int);\n";
      checkScriptFile(createScript, true,
          equalTo(SqlLine.Status.OK),
          containsString(" "));
      final String showTables = "!tables";
      checkScriptFile(showTables, true, equalTo(SqlLine.Status.OK),
          allOf(containsString("TESTDROPALL_SPECIFIC_1"),
              containsString("TESTDROPALL_SPECIFIC_2"),
              containsString("TESTDROPALL_SPECIFIC_3")));
      String dropScript = "!dropall TESTDROPALL_SPECIFIC_1\n";
      checkScriptFile(dropScript, true,
          equalTo(SqlLine.Status.OK),
          containsString(" "));

      checkScriptFile(showTables, true, equalTo(SqlLine.Status.OK),
          allOf(containsString("INFORMATION_SCHEMA"),
              containsString("TESTDROPALL_SPECIFIC_2"),
              containsString("TESTDROPALL_SPECIFIC_3"),
              not(containsString("TESTDROPALL_SPECIFIC_1"))));
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  @Test
  public void testGlobalConfiguration() {
    final File globalConnectionConf = createTempFile(
            "tmpconfconnections", "temp");
    Path confPath = Paths.get(globalConnectionConf.getAbsolutePath());
    String connectionName = "myconnection";
    try (BufferedWriter bw =
                 new BufferedWriter(
                         new OutputStreamWriter(
                                 new FileOutputStream(globalConnectionConf),
                                 StandardCharsets.UTF_8))) {
      // In the connection configuration we write a global-conf: section
      bw.write(ConnectionConfigParser.GLOBAL_CONFIG_NAME + ": \n"
              + "  maxWidth:15\n"
              + "  incremental:true\n"
              + connectionName + ": \n"
              + "  url: " + ConnectionSpec.H2.url + "\n"
              + "  user: " + ConnectionSpec.H2.username + "\n"
              + "  password: " + "somenonemptypw" + "\n");
      bw.flush();
      bw.close();
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      String[] connectionArgs = new String[]{
          "-c", connectionName,
          "--connectionConfig=" + globalConnectionConf.getAbsolutePath()};
      SqlLine.Status status = begin(sqlLine, os, false, connectionArgs);
      assertThat(status, equalTo(SqlLine.Status.OK));
      DispatchCallback dc = new DispatchCallback();
      sqlLine.runCommands(dc, "!connect -c "
              + connectionName);
      sqlLine.runCommands(dc, "values (123456789, 123456789)");
      String output = os.toString("UTF8");
      // Max widdth of the output should be 15 chars
      String expected = "| 123456789   |";
      assertThat(output, containsString(expected));
      // This is longer than 15, so it should not be in the output.
      String notExpected = "123456789   | 1";
      assertThat(output, not(containsString(notExpected)));

      sqlLine.runCommands(dc, "!set maxwidth 25");
      // We can override the maxWidth later
      sqlLine.runCommands(dc, "values (123456789, 123456789)");
      output = os.toString("UTF8");
      // It will write 25 chars.
      expected = "| 123456789   | 1234567 |";
      assertThat(output, containsString(expected));
      // This is longer than 25, so it should not be in the output.
      notExpected = "123456789   | 12345678";
      assertThat(output, not(containsString(notExpected)));

      sqlLine.runCommands(new DispatchCallback(), "!quit");
      assertTrue(sqlLine.isExit());
    } catch (Throwable t) {
      // fail
      throw new RuntimeException(t);
    }
    try {
      Files.delete(confPath);
    } catch (java.io.IOException e) {
      e.printStackTrace();
    }
  }

  /** Information necessary to create a JDBC connection. Specify one to run
   * tests against a different database. (hsqldb is the default.) */
  public static class ConnectionSpec {
    public final String url;
    public final String username;
    public final String password;
    public final String driver;

    public ConnectionSpec(String url, String username, String password,
        String driver) {
      this.url = url;
      this.username = username;
      this.password = password;
      this.driver = driver;
    }

    public static final ConnectionSpec H2 =
        new ConnectionSpec("jdbc:h2:mem:", "sa", "", "org.h2.Driver");

    public static final ConnectionSpec ERROR_H2_DRIVER =
        new ConnectionSpec("jdbc:h2:mem:", "sa", "", "ERROR_DRIVER");

    public static final ConnectionSpec ERROR_H2_URL =
        new ConnectionSpec("ERROR_URL", "sa", "", "org.h2.Driver");

    public static final ConnectionSpec HSQLDB =
        new ConnectionSpec(
            ScottHsqldb.URI, ScottHsqldb.USER, ScottHsqldb.PASSWORD,
            "org.hsqldb.jdbcDriver");

    public static final ConnectionSpec MYSQL =
        new ConnectionSpec(
            "jdbc:mysql://localhost/scott", "scott", "tiger",
            "com.mysql.jdbc.Driver");
  }

  /** Regular expression matcher. */
  private static class RegexMatcher extends BaseMatcher<String> {
    private final String pattern;

    RegexMatcher(String pattern) {
      super();
      this.pattern = pattern;
    }

    public static RegexMatcher of(String pattern) {
      return new RegexMatcher(pattern);
    }

    public boolean matches(Object o) {
      return o instanceof String
          && ((String) o).matches(pattern);
    }

    public void describeTo(Description description) {
      description.appendText("regular expression ").appendText(pattern);
    }
  }

}

// End SqlLineArgsTest.java
