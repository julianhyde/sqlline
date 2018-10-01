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
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.h2.util.StringUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import org.hsqldb.jdbc.JDBCDatabaseMetaData;
import org.hsqldb.jdbc.JDBCResultSet;

import org.jline.builtins.Commands;
import org.jline.terminal.Terminal;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;

import net.hydromatic.scott.data.hsqldb.ScottHsqldb;
import sqlline.extensions.CustomApplication;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Executes tests of the command-line arguments to SqlLine.
 */
@RunWith(JMockit.class)
public class SqlLineArgsTest {
  private static final ConnectionSpec CONNECTION_SPEC = ConnectionSpec.HSQLDB;
  private ConnectionSpec connectionSpec;

  public SqlLineArgsTest() {
    connectionSpec = CONNECTION_SPEC;
  }

  /**
   * Execute a script with "beeline -f".
   *
   * @throws java.lang.Throwable On error
   * @return The stderr and stdout from running the script
   * @param args Script arguments
   */
  private static Pair run(String... args) throws Throwable {
    SqlLine beeLine = new SqlLine();
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    PrintStream beelineOutputStream =
        new PrintStream(os, false, StandardCharsets.UTF_8.name());
    beeLine.setOutputStream(beelineOutputStream);
    beeLine.setErrorStream(beelineOutputStream);
    final InputStream is = new ByteArrayInputStream(new byte[0]);
    SqlLine.Status status = beeLine.begin(args, is, false);
    return new Pair(status, os.toString("UTF8"));
  }

  private Pair runScript(File scriptFile, boolean flag, String outputFileName)
      throws Throwable {
    return runScript(connectionSpec, scriptFile, flag, outputFileName);
  }

  private static Pair runScript(ConnectionSpec connectionSpec, File scriptFile,
      boolean flag, String outputFileName) throws Throwable {
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
    return run(args.toArray(new String[args.size()]));
  }

  /**
   * Attempts to execute a simple script file with the -f option to SqlLine.
   * Tests for presence of an expected pattern in the output (stdout or stderr).
   *
   * @param scriptText Script text
   * @param flag Command flag (--run or -f)
   * @param statusMatcher Checks whether status is as expected
   * @param outputMatcher Checks whether output is as expected
   * @throws Exception on command execution error
   */
  private void checkScriptFile(String scriptText, boolean flag,
      Matcher<SqlLine.Status> statusMatcher,
      Matcher<String> outputMatcher) throws Throwable {
    // Put the script content in a temp file
    File scriptFile = File.createTempFile("foo", "temp");
    scriptFile.deleteOnExit();
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
  }

  /** Windows line separators in a string into Linux line separators;
   * a Linux string is unchanged. */
  private static String toLinux(String s) {
    return s.replaceAll("\r\n", "\n");
  }

  @Test
  public void testMultilineScriptWithComments() throws Throwable {
    final String scriptText =
        "-- a comment  \n values\n--comment\n (\n1\n, ' ab'\n--comment\n)\n;\n";

    checkScriptFile(scriptText, true,
        equalTo(SqlLine.Status.OK),
        containsString("+-------------+-----+\n"
            + "|     C1      | C2  |\n"
            + "+-------------+-----+\n"
            + "| 1           |  ab |\n"
            + "+-------------+-----+"));
  }

  /**
   * Attempt to execute a simple script file with the -f option to SqlLine.
   * Test for presence of an expected pattern
   * in the output (stdout or stderr), fail if not found.
   */
  @Test
  public void testPositiveScriptFile() throws Throwable {
    checkScriptFile("call 100 + 23;\n",
        true,
        equalTo(SqlLine.Status.OK),
        containsString(" 123 "));
  }

  /**
   * As above, but using '-run' rather than '-f'.
   */
  @Test
  public void testPositiveScriptFileUsingRun() throws Throwable {
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
  public void testScriptFileStartsWithComment() throws Throwable {
    final String scriptText = "-- a comment\n"
        + "call 100 + 23;\n";
    checkScriptFile(scriptText, true,
        equalTo(SqlLine.Status.OK),
        containsString(" 123 "));
  }

  @Test
  public void testScriptFileStartsWithEmptyLine() throws Throwable {
    final String scriptText = "\n"
        + "call 100 + 23;\n";
    checkScriptFile(scriptText, true,
        equalTo(SqlLine.Status.OK),
        containsString(" 123 "));
  }

  @Test
  public void testScriptFileContainsComment() throws Throwable {
    final String scriptText = "values 10 + 23;\n"
        + "-- a comment\n"
        + "values 100 + 23;\n";
    checkScriptFile(scriptText, true,
        equalTo(SqlLine.Status.OK),
        allOf(containsString(" 33 "), containsString(" 123 ")));
  }

  /** Test case for
   * <a href="https://github.com/julianhyde/sqlline/issues/72">[SQLLINE-72]
   * Allow quoted file names (including spaces) in <tt>!record</tt>,
   * <tt>!run</tt> and <tt>!script</tt> commands</a>. */
  @Test
  public void testScriptFilenameWithSpace() throws Throwable {
    final String scriptText = "values 10 + 23;\n"
        + "-- a comment\n"
        + "values 100 + 23;\n";

    File scriptFile = File.createTempFile("Script with Spaces", ".sql");
    scriptFile.deleteOnExit();
    PrintStream os =
        new PrintStream(
            new FileOutputStream(scriptFile),
            false,
            StandardCharsets.UTF_8.name());
    os.print(scriptText);
    os.close();

    Pair pair = runScript(scriptFile, true, null);
    assertThat(pair.status, equalTo(SqlLine.Status.OK));
    assertThat(pair.output,
        allOf(containsString(" 33 "), containsString(" 123 ")));
  }

  @Test
  public void testScriptWithOutput() throws Throwable {
    final String scriptText = "values 100 + 123;\n"
        + "-- a comment\n"
        + "values 100 + 253;\n";

    File scriptFile = File.createTempFile("Script file name", ".sql");
    scriptFile.deleteOnExit();
    PrintStream os = new PrintStream(
        new FileOutputStream(scriptFile),
        false,
        StandardCharsets.UTF_8.name());
    os.print(scriptText);
    os.close();

    File outputFile = new File("testScriptWithOutput.out");
    outputFile.deleteOnExit();
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
  public void testNull() throws Throwable {
    checkScriptFile(
        "values (1, cast(null as integer), cast(null as varchar(3));\n",
        false,
        equalTo(SqlLine.Status.OK),
        containsString(
            "+-------------+-------------+-----+\n"
            + "|     C1      |     C2      | C3  |\n"
            + "+-------------+-------------+-----+\n"
            + "| 1           | null        |     |\n"
            + "+-------------+-------------+-----+\n"));
  }

  @Test
  public void testScan() throws Throwable {
    final String expectedLine0 = "Compliant Version Driver Class\n";
    final String expectedLine1 = "yes       2.4     org.hsqldb.jdbcDriver";
    checkScriptFile("!scan\n", false,
        equalTo(SqlLine.Status.OK),
        allOf(containsString(expectedLine0), containsString(expectedLine1)));
  }

  /**
   * Table output without header.
   */
  @Test
  public void testTableOutputNullWithoutHeader() throws Throwable {
    final String script = "!set showHeader false\n"
        + "values (1, cast(null as integer), cast(null as varchar(3));\n";
    checkScriptFile(script, false,
        equalTo(SqlLine.Status.OK),
        containsString("| 1           | null        |     |\n"));
  }

  /**
   * Csv output without header.
   */
  @Test
  public void testCsvNullWithoutHeader() throws Throwable {
    final String script = "!set showHeader false\n"
        + "!set outputformat csv\n"
        + "values (1, cast(null as integer), cast(null as varchar(3));\n";
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
  public void testClose() throws Throwable {
    final String expected = "!close\n"
        + "Closing: org.hsqldb.jdbc.JDBCConnection";
    checkScriptFile("!close\n", false, equalTo(SqlLine.Status.OK),
        containsString(expected));
  }

  /**
   * Test case for
   * <a href="https://github.com/julianhyde/sqlline/issues/32">[SQLLINE-32]
   * !help set' should print documentation for all variables</a>.
   */
  @Test
  public void testHelpSet() throws Throwable {
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
        + "autoSave        true/false Automatically save preferences\n";
    checkScriptFile("!help set\n", false, equalTo(SqlLine.Status.OK),
        containsString(expected));

    // Make sure that each variable (autoCommit, autoSave, color, etc.) has a
    // line in the output of '!help set'
    final SqlLine sqlLine = new SqlLine();
    String help = sqlLine.loc("help-set")
        + sqlLine.loc("variables");
    for (String p : sqlLine.getOpts().propertyNamesMixed()) {
      assertThat(help, containsString("\n" + p + " "));
    }
    assertThat(sqlLine.getOpts().propertyNamesMixed().contains("autoCommit"),
        is(true));
    assertThat(sqlLine.getOpts().propertyNamesMixed().contains("autocommit"),
        is(false));
    assertThat(sqlLine.getOpts().propertyNamesMixed().contains("trimScripts"),
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
  public void testHelpAll() throws Throwable {
    // Note that "connections" has been broken onto a new line.
    final String expected = "1/1          !help all\n"
        + "!all                Execute the specified SQL against all the current\n"
        + "                    connections\n"
        + "Closing: org.hsqldb.jdbc.JDBCConnection\n"
        + "sqlline version ???\n";
    checkScriptFile("!help all\n", false, equalTo(SqlLine.Status.OK),
        is(expected));
  }

  /**
   * Tests "!help go". "go" and "#" are synonyms.
   */
  @Test
  public void testHelpGo() throws Throwable {
    for (String c : new String[] {"go", "#"}) {
      final String expected = "1/1          !help " + c + "\n"
          + "!go                 Select the current connection\n"
          + "Closing: org.hsqldb.jdbc.JDBCConnection\n"
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
  public void testHelpAsQuestionMark() throws Throwable {
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
  public void testManual() throws Throwable {
    final String expectedLine = "sqlline version";
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    new MockUp<Commands>() {
      @Mock
      void less(Terminal terminal, InputStream in, PrintStream out,
          PrintStream err,
          Path currentDir,
          String[] argv) {
        return;
      }
    };

    SqlLine beeLine = new SqlLine();
    PrintStream baoswriter =
        new PrintStream(baos, false, StandardCharsets.UTF_8.name());
    beeLine.setOutputStream(baoswriter);
    beeLine.setErrorStream(baoswriter);
    final InputStream is = new ByteArrayInputStream(new byte[0]);
    SqlLine.Status status =
        beeLine.begin(new String[]{}, is, false);
    // Here it is the status is SqlLine.Status.OTHER
    // because of EOF as the result of InputStream which
    // is not used in the current test so it is ok
    // assertThat(status, equalTo(SqlLine.Status.OK));
    beeLine.runCommands(Collections.singletonList("!manual"),
        new DispatchCallback());
    String output = baos.toString("UTF8");

    assertThat(output, containsString(expectedLine));
  }

  /** Test case for
   * <a href="https://github.com/julianhyde/sqlline/issues/38">[SQLLINE-38]
   * Expand ~ to user's home directory</a>. */
  @Test
  public void testRunFromHome() throws Throwable {
    File home = new File(System.getProperty("user.home"));
    TemporaryFolder tmpFolder = new TemporaryFolder(home);
    tmpFolder.create();
    try {
      File tmpFile = tmpFolder.newFile("test.sql");
      Writer fw = new OutputStreamWriter(
          new FileOutputStream(tmpFile), StandardCharsets.UTF_8);
      try {
        fw.write("!set outputformat csv\n");
        fw.write("values (1, 2, 3, 4, 5);");
        fw.flush();
      } finally {
        fw.close();
      }
      checkScriptFile("!run ~" + File.separatorChar
              + tmpFolder.getRoot().getName()
              + File.separatorChar + tmpFile.getName(),
          false,
          equalTo(SqlLine.Status.OK),
          allOf(containsString("!set outputformat csv"),
              containsString("values (1, 2, 3, 4, 5);")));

    } finally {
      // will remove tmpFile as well
      tmpFolder.delete();
    }
  }

  /**
   * Test case for
   * <a href="https://github.com/julianhyde/sqlline/issues/26">[SQLLINE-26]
   * Flush output for each command when using !record command</a>.
   */
  @Test
  public void testRecord() throws Throwable {
    File file = File.createTempFile("sqlline", ".log");
    checkScriptFile(
        "values 1;\n"
        + "!record " + file.getAbsolutePath() + "\n"
        + "!set outputformat csv\n"
        + "values 2;\n"
        + "!record\n"
        + "!set outputformat csv\n"
        + "values 3;\n",
        false,
        equalTo(SqlLine.Status.OK),
        RegexMatcher.of("(?s)1/7          values 1;\n"
                + "\\+-------------\\+\n"
                + "\\|     C1      \\|\n"
                + "\\+-------------\\+\n"
                + "\\| 1           \\|\n"
                + "\\+-------------\\+\n"
                + "1 row selected \\([0-9.,]+ seconds\\)\n"
                + "2/7          !record .*.log\n"
                + "Saving all output to \".*.log\". Enter \"record\" with no arguments to stop it.\n"
                + "3/7          !set outputformat csv\n"
                + "4/7          values 2;\n"
                + "'C1'\n"
                + "'2'\n"
                + "1 row selected \\([0-9.,]+ seconds\\)\n"
                + "5/7          !record\n"
                + "Recording stopped.\n"
                + "6/7          !set outputformat csv\n"
                + "7/7          values 3;\n"
                + "'C1'\n"
                + "'3'\n"
                + "1 row selected \\([0-9.,]+ seconds\\)\n.*"));

    // Now check that the right stuff got into the file.
    assertFileContains(file,
        RegexMatcher.of("Saving all output to \".*.log\". "
            + "Enter \"record\" with no arguments to stop it.\n"
            + "3/7          !set outputformat csv\n"
            + "4/7          values 2;\n"
            + "'C1'\n"
            + "'2'\n"
            + "1 row selected \\([0-9.,]+ seconds\\)\n"
            + "5/7          !record\n"));
  }

  /** Test case for
   * <a href="https://github.com/julianhyde/sqlline/issues/62">[SQLLINE-62]
   * Expand ~ to user's home directory</a>. */
  @Test
  public void testRecordHome() throws Throwable {
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
  public void testRecordFilenameWithSpace() throws Throwable {
    File file = File.createTempFile("sqlline file with spaces", ".log");
    checkScriptFile(
        "values 1;\n"
            + "!record " + file.getAbsolutePath() + "\n"
            + "!set outputformat csv\n"
            + "values 2;\n"
            + "!record\n"
            + "!set outputformat csv\n"
            + "values 3;\n",
        false,
        equalTo(SqlLine.Status.OK),
        RegexMatcher.of("(?s)1/7          values 1;\n"
            + "\\+-------------\\+\n"
            + "\\|     C1      \\|\n"
            + "\\+-------------\\+\n"
            + "\\| 1           \\|\n"
            + "\\+-------------\\+\n"
            + "1 row selected \\([0-9.,]+ seconds\\)\n"
            + "2/7          !record .*.log\n"
            + "Saving all output to \".*.log\". Enter \"record\" with no arguments to stop it.\n"
            + "3/7          !set outputformat csv\n"
            + "4/7          values 2;\n"
            + "'C1'\n"
            + "'2'\n"
            + "1 row selected \\([0-9.,]+ seconds\\)\n"
            + "5/7          !record\n"
            + "Recording stopped.\n"
            + "6/7          !set outputformat csv\n"
            + "7/7          values 3;\n"
            + "'C1'\n"
            + "'3'\n"
            + "1 row selected \\([0-9.,]+ seconds\\)\n.*"));

    // Now check that the right stuff got into the file.
    assertFileContains(file,
        RegexMatcher.of("Saving all output to \".*.log\". "
            + "Enter \"record\" with no arguments to stop it.\n"
            + "3/7          !set outputformat csv\n"
            + "4/7          values 2;\n"
            + "'C1'\n"
            + "'2'\n"
            + "1 row selected \\([0-9.,]+ seconds\\)\n"
            + "5/7          !record\n"));
  }

  private void assertFileContains(File file, Matcher matcher)
      throws IOException {
    final BufferedReader br = new BufferedReader(
        new InputStreamReader(
            new FileInputStream(file), StandardCharsets.UTF_8.name()));
    final StringWriter stringWriter = new StringWriter();
    for (;;) {
      final String line = br.readLine();
      if (line == null) {
        break;
      }
      stringWriter.write(line);
      stringWriter.write("\n");
    }
    br.close();
    assertThat(toLinux(stringWriter.toString()), matcher);
  }

  /**
   * Test case for
   * <a href="https://github.com/julianhyde/sqlline/issues/61">[SQLLINE-61]
   * Add !nickname command</a>.
   */
  @Test
  public void testNickname() throws Throwable {
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
  public void testBreakOnErrorScriptFile() throws Throwable {
    checkScriptFile("select * from abcdefg01;\ncall 100 + 23;\n",
        true,
        equalTo(SqlLine.Status.OTHER),
        not(containsString(" 123 ")));
  }

  @Test
  public void testExecutionException(@Mocked final JDBCDatabaseMetaData meta,
                 @Mocked final JDBCResultSet resultSet)  throws Throwable {
    new Expectations() {
      {
        // prevent calls to functions that also call resultSet.next
        meta.getDatabaseProductName(); result = "hsqldb";
        // prevent calls to functions that also call resultSet.next
        meta.getDatabaseProductVersion(); result = "1.0";
        // Generate an exception on a call to resultSet.next
        resultSet.next(); result = new SQLException("Generated Exception.");
      }
    };
    SqlLine sqlLine = new SqlLine();
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
    Deencapsulation.setField(sqlLine, "initComplete", true);
    sqlLine.getConnection();
    sqlLine.runCommands(
        Arrays.asList("CREATE TABLE rsTest ( a int);",
            "insert into rsTest values (1);",
            "insert into rsTest values (2);",
            "select a from rsTest; "),
        callback);
    String output = os.toString("UTF8");
    assertThat(output, containsString("Generated Exception"));
  }

  /**
   * Attempts to execute a missing script file with the -f option to SqlLine.
   */
  @Test
  public void testNegativeScriptFile() throws Throwable {
    // Create and delete a temp file
    File scriptFile = File.createTempFile("sqllinenegative", "temp");
    final boolean delete = scriptFile.delete();
    assertThat(delete, is(true));

    Pair pair = runScript(scriptFile, true, null);
    assertThat(pair.status, equalTo(SqlLine.Status.OTHER));
    assertThat(pair.output, not(containsString(" 123 ")));
  }

  /** Displays usage. */
  @Test
  public void testUsage() throws Throwable {
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
  public void testInvalidArguments() throws Throwable {
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
  public void testIsolationSetting() throws Throwable {
    final String script0 = "!isolation TRANSACTION_NONE\n";
    final String expected = "Transaction isolation level TRANSACTION_NONE "
        + "is not supported. Default (TRANSACTION_READ_COMMITTED) "
        + "will be used instead.";
    checkScriptFile(script0, true, equalTo(SqlLine.Status.OTHER),
        containsString(expected));
  }

  @Test
  public void testDefaultIsolation() throws Throwable {
    final String script1 = "!isolation default\n";
    checkScriptFile(script1, true, equalTo(SqlLine.Status.OK),
        allOf(not(containsString("Transaction isolation level")),
            not(containsString("is not supported"))));
  }

  /**
   * HIVE-4566, "NullPointerException if typeinfo and nativesql commands are
   * executed at beeline before a DB connection is established".
   *
   * @throws UnsupportedEncodingException on unsupported encoding
   */
  @Test
  public void testNPE() throws UnsupportedEncodingException {
    SqlLine sqlLine = new SqlLine();

    ByteArrayOutputStream os = new ByteArrayOutputStream();
    PrintStream sqllineOutputStream =
        new PrintStream(os, false, StandardCharsets.UTF_8.name());
    sqlLine.setOutputStream(sqllineOutputStream);
    sqlLine.setErrorStream(sqllineOutputStream);

    sqlLine.runCommands(Arrays.asList("!typeinfo"), new DispatchCallback());
    String output = os.toString("UTF8");
    assertThat(output, not(containsString("java.lang.NullPointerException")));
    assertThat(output, containsString("No current connection"));

    sqlLine.runCommands(Arrays.asList("!nativesql"), new DispatchCallback());
    output = os.toString("UTF8");
    assertThat(output, not(containsString("java.lang.NullPointerException")));
    assertThat(output, containsString("No current connection"));
  }

  @Test
  public void testCommandHandlerOnStartup() throws IOException {
    SqlLine sqlLine = new SqlLine();
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    PrintStream sqllineOutputStream =
        new PrintStream(os, false, StandardCharsets.UTF_8.name());
    sqlLine.setOutputStream(sqllineOutputStream);
    sqlLine.setErrorStream(sqllineOutputStream);
    final InputStream is = new ByteArrayInputStream(new byte[0]);
    final String[] args = {
      "-e", "!set maxwidth 80",
      "-ch", "sqlline.extensions.HelloWorldCommandHandler"};
    sqlLine.begin(args, is, false);

    sqlLine.runCommands(Collections.singletonList("!hello"),
        new DispatchCallback());
    String output = os.toString("UTF8");
    assertThat(output, containsString("HELLO WORLD"));
    os.reset();
    sqlLine.runCommands(Collections.singletonList("!test"),
        new DispatchCallback());
    output = os.toString("UTF8");
    assertThat(output, containsString("HELLO WORLD"));
    os.reset();
    sqlLine.runCommands(Collections.singletonList("!help hello"),
        new DispatchCallback());
    output = os.toString("UTF8");
    assertThat(output, containsString("help for hello test"));
    sqlLine.runCommands(
        Collections.singletonList("!quit"), new DispatchCallback());
    assertTrue(sqlLine.isExit());
  }

  @Test
  public void testCommandHandler() throws IOException {
    SqlLine sqlLine = new SqlLine();

    ByteArrayOutputStream os = new ByteArrayOutputStream();
    PrintStream sqllineOutputStream =
        new PrintStream(os, false, StandardCharsets.UTF_8.name());
    sqlLine.setOutputStream(sqllineOutputStream);
    sqlLine.setErrorStream(sqllineOutputStream);
    final InputStream is = new ByteArrayInputStream(new byte[0]);
    SqlLine.Status status =
        sqlLine.begin(new String[]{"-e", "!set maxwidth 80"}, is, false);

    final String script = "!commandhandler"
        + " sqlline.extensions.HelloWorld2CommandHandler"
        + " sqlline.extensions.HelloWorldCommandHandler";
    sqlLine.runCommands(Collections.singletonList(script),
        new DispatchCallback());
    sqlLine.runCommands(Collections.singletonList("!hello"),
        new DispatchCallback());
    String output = os.toString("UTF8");
    assertThat(output, containsString("HELLO WORLD2"));
    final String expected = "Could not add command handler "
        + "sqlline.extensions.HelloWorldCommandHandler as one of commands "
        + "[hello, test] is already present";
    assertThat(output, containsString(expected));
    os.reset();
    sqlLine.runCommands(Collections.singletonList("!help hello"),
        new DispatchCallback());
    output = os.toString("UTF8");
    assertThat(output, containsString("help for hello2"));
    sqlLine.runCommands(
        Collections.singletonList("!quit"), new DispatchCallback());
    assertTrue(sqlLine.isExit());
  }

  @Test
  public void testTablesCsv() throws Throwable {
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
  public void testCsvDelimiterAndQuoteCharacter() throws Throwable {
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
  public void testSetForNulls() throws Throwable {
    final String script = "!set numberFormat null\n"
        + "!set\n";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        containsString("numberformat        null"));
  }

  @Test
  public void testSelectXmlAttributes() throws Throwable {
    final String script = "!set outputformat xmlattr\n"
        + "values (1, -1.5, 1 = 1, date '1969-07-20', null, ']]> 1''2\"3\t<>&4');\n";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        allOf(containsString("<resultset>"),
            containsString("<result C1=\"1\" C2=\"-1.5\" C3=\"TRUE\" "
                + "C4=\"1969-07-20\" C5=\"null\" "
                + "C6=\"]]&gt; 1'2&quot;3\t&lt;>&amp;4\"/>")));
  }

  @Test
  public void testSelectXmlElements() throws Throwable {
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
  public void testTablesJson() throws Throwable {
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
  public void testSelectJson() throws Throwable {
    final String script = "!set outputformat json\n"
        + "values (1, -1.5, 1 = 1, date '1969-07-20', null, ' 1''2\"3\t4');\n";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        allOf(containsString("{\"resultset\":["),
            containsString("{\"C1\":1,\"C2\":-1.5,\"C3\":true,"
                + "\"C4\":\"1969-07-20\",\"C5\":null,"
                + "\"C6\":\" 1'2\\\"3\\t4\"}")));
  }

  @Test
  public void testNullValue() throws Throwable {
    final String script = "!set nullValue %%%\n"
        + "!set outputformat csv\n"
        + "values (NULL, -1.5, null, date '1969-07-20', null, 'null');\n"
        + "!set nullValue \"'\"\n"
        + "values (NULL, -1.5, null, date '1969-07-20', null, 'null');\n";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        CoreMatchers.allOf(containsString("'C1','C2','C3','C4','C5','C6'"),
            containsString("'%%%','-1.5','%%%','1969-07-20','%%%','null'"),
            containsString("'C1','C2','C3','C4','C5','C6'"),
            containsString("'''','-1.5','''','1969-07-20','''','null'")));
  }

  @Test
  public void testTimeFormat() throws Throwable {
    // Use System.err as it is used in sqlline.SqlLineOpts#set
    final PrintStream originalErr = System.err;
    try {
      final ByteArrayOutputStream errBaos = new ByteArrayOutputStream();
      System.setErr(
          new PrintStream(errBaos, false, StandardCharsets.UTF_8.name()));

      // successful patterns
      final String okTimeFormat = "!set timeFormat HH:mm:ss\n";
      final String defaultTimeFormat = "!set timeFormat default\n";
      final String okDateFormat = "!set dateFormat YYYY-MM-dd\n";
      final String defaultDateFormat = "!set dateFormat default\n";
      final String okTimestampFormat = "!set timestampFormat default\n";
      final String defaultTimestampFormat = "!set timestampFormat default\n";

      // successful cases
      final SqlLine sqlLine = new SqlLine();
      sqlLine.runCommands(Arrays.asList(okTimeFormat), new DispatchCallback());
      assertThat(errBaos.toString("UTF8"),
          not(
              anyOf(containsString("Error setting configuration"),
                  containsString("Exception"))));
      sqlLine.runCommands(
          Arrays.asList(defaultTimeFormat), new DispatchCallback());
      assertThat(errBaos.toString("UTF8"),
          not(
              anyOf(containsString("Error setting configuration"),
                  containsString("Exception"))));
      sqlLine.runCommands(Arrays.asList(okDateFormat), new DispatchCallback());
      assertThat(errBaos.toString("UTF8"),
          not(
              anyOf(containsString("Error setting configuration"),
                  containsString("Exception"))));
      sqlLine.runCommands(Arrays.asList(defaultDateFormat),
          new DispatchCallback());
      assertThat(errBaos.toString("UTF8"),
          not(
              anyOf(containsString("Error setting configuration"),
                  containsString("Exception"))));
      sqlLine.runCommands(Arrays.asList(okTimestampFormat),
          new DispatchCallback());
      assertThat(errBaos.toString("UTF8"),
          not(
              anyOf(containsString("Error setting configuration"),
                  containsString("Exception"))));
      sqlLine.runCommands(Arrays.asList(defaultTimestampFormat),
          new DispatchCallback());
      assertThat(errBaos.toString("UTF8"),
          not(
              anyOf(containsString("Error setting configuration"),
                  containsString("Exception"))));

      // failed patterns
      final String wrongTimeFormat = "!set timeFormat qwerty\n";
      final String wrongDateFormat = "!set dateFormat ASD\n";
      final String wrongTimestampFormat =
          "!set timestampFormat 'YYYY-MM-ddTHH:MI:ss'\n";

      // failed cases
      sqlLine.runCommands(Arrays.asList(wrongTimeFormat),
          new DispatchCallback());
      assertThat(errBaos.toString("UTF8"),
          containsString("Illegal pattern character 'q'"));
      sqlLine.runCommands(Arrays.asList(wrongDateFormat),
          new DispatchCallback());
      assertThat(errBaos.toString("UTF8"),
          containsString("Illegal pattern character 'A'"));
      sqlLine.runCommands(Arrays.asList(wrongTimestampFormat),
          new DispatchCallback());
      assertThat(errBaos.toString("UTF8"),
          containsString("Illegal pattern character 'T'"));
    } finally {
      // Set error stream back
      System.setErr(originalErr);
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
  public void testConnectWithDbPropertyAsParameter() throws Throwable {
    SqlLine beeLine = new SqlLine();
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    PrintStream beelineOutputStream =
        new PrintStream(os, false, StandardCharsets.UTF_8.name());
    beeLine.setOutputStream(beelineOutputStream);
    beeLine.setErrorStream(beelineOutputStream);
    final InputStream is = new ByteArrayInputStream(new byte[0]);
    SqlLine.Status status =
        beeLine.begin(new String[]{"-e", "!set maxwidth 80"}, is, false);
    assertThat(status, equalTo(SqlLine.Status.OK));
    DispatchCallback dc = new DispatchCallback();
    beeLine.runCommands(Collections.singletonList("!set maxwidth 80"), dc);
    String fakeNonEmptyPassword = "nonEmptyPasswd";
    beeLine.runCommands(
        Collections.singletonList("!connect "
            + " -p PASSWORD_HASH TRUE "
            + ConnectionSpec.H2.url + " "
            + ConnectionSpec.H2.username + " "
            + StringUtils.convertBytesToHex(
                fakeNonEmptyPassword.getBytes(StandardCharsets.UTF_8))),
        dc);
    beeLine.runCommands(Collections.singletonList("!tables"), dc);
    String output = os.toString("UTF8");
    final String expected = "| TABLE_CAT | TABLE_SCHEM | "
        + "TABLE_NAME | TABLE_TYPE | REMARKS | TYPE_CAT | TYP |";
    assertThat(output, containsString(expected));
    beeLine.runCommands(
        Collections.singletonList("!quit"), new DispatchCallback());
    assertTrue(beeLine.isExit());
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
  public void testConnectWithDbPropertyAsParameter2() throws Throwable {
    SqlLine beeLine = new SqlLine();
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    PrintStream beelineOutputStream =
        new PrintStream(os, false, StandardCharsets.UTF_8.name());
    beeLine.setOutputStream(beelineOutputStream);
    beeLine.setErrorStream(beelineOutputStream);
    final InputStream is = new ByteArrayInputStream(new byte[0]);
    SqlLine.Status status =
        beeLine.begin(new String[]{"-e", "!set maxwidth 80"}, is, false);
    assertThat(status, equalTo(SqlLine.Status.OK));
    DispatchCallback dc = new DispatchCallback();
    beeLine.runCommands(Collections.singletonList("!set maxwidth 80"), dc);
    String fakeNonEmptyPassword = "nonEmptyPasswd";
    beeLine.runCommands(
        Collections.singletonList("!connect "
            + " -p PASSWORD_HASH TRUE -p ALLOW_LITERALS NONE "
            + ConnectionSpec.H2.url + " "
            + ConnectionSpec.H2.username + " "
            + StringUtils.convertBytesToHex(
                fakeNonEmptyPassword.getBytes(StandardCharsets.UTF_8))),
        dc);
    beeLine.runCommands(Collections.singletonList("select 1;"), dc);
    String output = os.toString("UTF8");
    final String expected = "Error:";
    assertThat(output, containsString(expected));
    beeLine.runCommands(
        Collections.singletonList("!quit"), new DispatchCallback());
    assertTrue(beeLine.isExit());
  }

  /**
   * Tests the {@code !connect} command passing the password in as a hash:
   *
   * <blockquote>
   * !connect "jdbc:h2:mem; PASSWORD_HASH=TRUE" sa 6e6f6e456d707479506173737764
   * </blockquote>
   */
  @Test
  public void testConnectWithDbProperty() throws Throwable {
    SqlLine beeLine = new SqlLine();
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    PrintStream beelineOutputStream =
        new PrintStream(os, false, StandardCharsets.UTF_8.name());
    beeLine.setOutputStream(beelineOutputStream);
    beeLine.setErrorStream(beelineOutputStream);
    final InputStream is = new ByteArrayInputStream(new byte[0]);
    SqlLine.Status status =
        beeLine.begin(new String[]{"-e", "!set maxwidth 80"}, is, false);
    assertThat(status, equalTo(SqlLine.Status.OK));
    DispatchCallback dc = new DispatchCallback();
    beeLine.runCommands(Collections.singletonList("!set maxwidth 80"), dc);

    // fail attempt
    String fakeNonEmptyPassword = "nonEmptyPasswd";
    beeLine.runCommands(
        Collections.singletonList("!connect \""
            + ConnectionSpec.H2.url
            + " ;PASSWORD_HASH=TRUE\" "
            + ConnectionSpec.H2.username
            + " \"" + fakeNonEmptyPassword + "\""), dc);
    String output = os.toString("UTF8");
    final String expected0 = "Error:";
    assertThat(output, containsString(expected0));
    os.reset();

    // success attempt
    beeLine.runCommands(
        Collections.singletonList("!connect \""
            + ConnectionSpec.H2.url
            + " ;PASSWORD_HASH=TRUE;ALLOW_LITERALS=NONE\" "
            + ConnectionSpec.H2.username + " \""
            + StringUtils.convertBytesToHex(
                fakeNonEmptyPassword.getBytes(StandardCharsets.UTF_8))
            + "\""), dc);
    beeLine.runCommands(Collections.singletonList("!tables"), dc);
    output = os.toString("UTF8");
    final String expected1 = "| TABLE_CAT | TABLE_SCHEM | "
        + "TABLE_NAME | TABLE_TYPE | REMARKS | TYPE_CAT | TYP |";
    assertThat(output, containsString(expected1));

    beeLine.runCommands(Collections.singletonList("select 5;"), dc);
    output = os.toString("UTF8");
    final String expected2 = "Error:";
    assertThat(output, containsString(expected2));
    os.reset();

    beeLine.runCommands(
        Collections.singletonList("!quit"), new DispatchCallback());
    output = os.toString("UTF8");
    assertThat(output,
        allOf(not(containsString("Error:")), containsString("!quit")));
    assertTrue(beeLine.isExit());
  }

  @Test
  public void testReconnect() throws Throwable {
    final String script = "!reconnect";
    final String expected = "Reconnecting to \"jdbc:hsqldb:res:scott\"...\n"
        + "Closing: org.hsqldb.jdbc.JDBCConnection";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        containsString(expected));
  }

  @Test
  public void testTables() throws Throwable {
    // Set width so we don't inherit from the current terminal.
    final String script = "!set maxwidth 80\n"
        + "!tables\n";
    final String line0 =
        "|                                                            TABLE_CAT         |";
    final String line1 =
        "| PUBLIC                                                                       |";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        allOf(containsString(line0), containsString(line1)));
  }

  @Test
  public void testTablesH2() throws Throwable {
    connectionSpec = ConnectionSpec.H2;
    // Set width so we don't inherit from the current terminal.
    final String script = "!set maxwidth 80\n"
        + "!tables\n";
    final String line0 = "| TABLE_CAT | TABLE_SCHEM | TABLE_NAME |";
    final String line1 =
        "| UNNAMED   | INFORMATION_SCHEMA | CATALOGS   | SYSTEM TABLE";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        allOf(containsString(line0), containsString(line1)));
  }

  /**
   * Test case for
   * <a href="https://github.com/julianhyde/sqlline/issues/107">[SQLLINE-107]
   * Script fails if the wrong driver is specified with -d option
   * and there is a valid registered driver for the specified url</a>.
   */
  @Test
  public void testTablesH2WithErrorDriver() throws Throwable {
    connectionSpec = ConnectionSpec.ERROR_H2_DRIVER;
    // Set width so we don't inherit from the current terminal.
    final String script = "!set maxwidth 80\n"
        + "!tables\n";
    final String line0 = "| TABLE_CAT | TABLE_SCHEM | TABLE_NAME |";
    final String line1 =
        "| UNNAMED   | INFORMATION_SCHEMA | CATALOGS   | SYSTEM TABLE";
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
  public void testH2TablesWithErrorUrl() throws Throwable {
    connectionSpec = ConnectionSpec.ERROR_H2_URL;
    final String script = "!tables\n";

    checkScriptFile(script, true, equalTo(SqlLine.Status.OTHER),
        CoreMatchers.allOf(containsString("No suitable driver"),
            not(containsString("NullPointerException"))));
  }

  @Test
  public void testEmptyMetadata() throws Throwable {
    final String script = "!metadata\n";
    final String line = "Usage: metadata <methodname> <params...>";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OTHER),
        allOf(containsString(line), not(containsString("Exception"))));
  }

  @Test
  public void testEmptyRecord() throws Throwable {
    final String line = "Usage: record <file name>";
    checkScriptFile("!record", true, equalTo(SqlLine.Status.OTHER),
        allOf(containsString(line), not(containsString("Exception"))));
  }

  @Test
  public void testEmptyRun() throws Throwable {
    final String line = "Usage: run <file name>";
    checkScriptFile("!run", true, equalTo(SqlLine.Status.OTHER),
        allOf(containsString(line), not(containsString("Exception"))));
  }

  @Test
  public void testEmptyScript() throws Throwable {
    final String line = "Usage: script <file name>";
    checkScriptFile("!script", true, equalTo(SqlLine.Status.OTHER),
        allOf(containsString(line), not(containsString("Exception"))));
  }

  @Test
  public void testSqlMultiline() throws Throwable {
    // Set width so we don't inherit from the current terminal.
    final String script = "!set maxwidth 80\n"
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
  public void testAllMultiline() throws Throwable {
    // Set width so we don't inherit from the current terminal.
    final String script = "!set maxwidth 80\n"
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
  public void testAppInfoMessage() throws Throwable {
    Pair pair = run();
    // Here it is the status is SqlLine.Status.OTHER
    // because of EOF as the result of InputStream which
    // is not used in the current test so it is ok
    // assertThat(status, equalTo(SqlLine.Status.OK));
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
  public void testCustomOutputFormats() throws Throwable {
    // json format was removed
    final String script = "!appconfig"
        + " sqlline.extensions.CustomApplication\n"
        + "!set outputformat json\n"
        + "values 1;";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        containsString("Unknown output format \"json\""));
  }

  @Test
  public void testCustomCommands() throws Throwable {
    // table command was removed
    final String script = "!appconfig"
        + " sqlline.extensions.CustomApplication\n"
        + "!tables";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OTHER),
        containsString("Unknown command: tables"));
  }

  @Test
  public void testAppConfigReset() throws Throwable {
    final String script = "!appconfig"
      + " sqlline.extensions.CustomApplication\n"
      + "!appconfig sqlline.Application\n"
      + "!tables";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
      containsString("TABLE_CAT"));
  }

  @Test
  public void testCustomOpts() throws Throwable {
    // nulls are displayed as custom_null
    final String script = "!appconfig"
      + " sqlline.extensions.CustomApplication\n"
      + "values(null)";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
      containsString("custom_null"));
  }

  @Test
  public void testVersion() throws Throwable {
    final String script = "!set\n";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        containsString(new Application().getVersion()));
  }

  @Test
  public void testSetVersion() throws Throwable {
    final String script = "!set version test-version\n";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OTHER),
        containsString("version property is read only"));
  }

  @Test
  public void testSetPropertySuccess() throws Throwable {
    final String script = "!set timeout\n"
        + "!set timeout 200\n"
        + "!set timeout";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        allOf(containsString("timeout             -1"),
            containsString("timeout             200")));
  }

  @Test
  public void testSetPropertyFailure() throws Throwable {
    final String script = "!set unk";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OTHER),
        containsString("Specified property [unk] does not exist."
            + " Use !set command to get list of all available properties."));
  }

  @Test
  public void testSetUsage() throws Throwable {
    final String script = "!set 1 2 3";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OTHER),
        containsString("Usage: set [all | <property name> [<value>]]"));
  }

  @Test
  public void testResetSuccess() throws Throwable {
    final String script = "!set timeout 200\n"
        + "!reset timeout";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        containsString("[timeout] was reset to [-1]"));
  }

  @Test
  public void testResetFailure() throws Throwable {
    final String script = "!reset unk";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OTHER),
        containsString("Specified property [unk] does not exist."
            + " Use !set command to get list of all available properties."));
  }

  @Test
  public void testResetAll() throws Throwable {
    final String script = "!set timeout 200\n"
        + "!reset all\n"
        + "!set timeout";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OK),
        allOf(containsString("All properties were reset to their defaults."),
            containsString("timeout             -1")));
  }

  @Test
  public void testResetUsage() throws Throwable {
    final String script = "!reset";
    checkScriptFile(script, true, equalTo(SqlLine.Status.OTHER),
        containsString("Usage: reset (all | <property name>)"));
  }

  @Test
  public void testRerun() throws Throwable {
    final SqlLine beeLine = new SqlLine();
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    PrintStream beelineOutputStream =
        new PrintStream(os, false, StandardCharsets.UTF_8.name());
    beeLine.setOutputStream(beelineOutputStream);
    beeLine.setErrorStream(beelineOutputStream);
    final InputStream is = new ByteArrayInputStream(new byte[0]);
    final File tmpHistoryFile = File.createTempFile("tmpHistory", "temp");
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
    }
    tmpHistoryFile.deleteOnExit();
    SqlLine.Status status = beeLine.begin(
        new String[]{"--historyfile=" + tmpHistoryFile.getAbsolutePath()},
        is, true);
    // Here the status is SqlLine.Status.OTHER
    // because of EOF as the result of InputStream which
    // is not used in the current test so it is ok
    assertThat(status, equalTo(SqlLine.Status.OTHER));
    DispatchCallback dc = new DispatchCallback();
    beeLine.runCommands(Collections.singletonList("!set maxwidth 80"), dc);
    beeLine.runCommands(
        Collections.singletonList("!connect "
            + ConnectionSpec.H2.url + " "
            + ConnectionSpec.H2.username + " "
            + "\"\""), dc);
    os.reset();

    beeLine.runCommands(Collections.singletonList("!/ 1"), dc);
    String output = os.toString("UTF8");
    final String expected0 = "+----------------------------+";
    final String expected1 = "|             C1             |";
    final String expected2 = "1 row selected";
    assertThat(output,
        allOf(containsString(expected0),
            containsString(expected1),
            containsString(expected2)));
    os.reset();

    beeLine.runCommands(Collections.singletonList("!/ 4"), dc);
    output = os.toString("UTF8");
    String expectedLine3 = "Cycled rerun of commands from history [2, 4]";
    assertThat(output, containsString(expectedLine3));
    os.reset();

    beeLine.runCommands(Collections.singletonList("!/ 3"), dc);
    output = os.toString("UTF8");
    String expectedLine4 = "Cycled rerun of commands from history [3, 5, 7]";
    assertThat(output, containsString(expectedLine4));
    os.reset();

    beeLine.runCommands(Collections.singletonList("!/ 8"), dc);
    output = os.toString("UTF8");
    String expectedLine5 = "Cycled rerun of commands from history [8]";
    assertThat(output, containsString(expectedLine5));
    os.reset();

    beeLine.runCommands(
        Collections.singletonList("!rerun " + Integer.MAX_VALUE), dc);
    output = os.toString("UTF8");
    String expectedLine6 =
        "Usage: rerun <offset>, available range of offset is -7..8";
    assertThat(output, containsString(expectedLine6));
    beeLine.runCommands(
        Collections.singletonList("!quit"), new DispatchCallback());
    assertTrue(beeLine.isExit());
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

    public RegexMatcher(String pattern) {
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
