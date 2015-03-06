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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Executes tests of the command-line arguments to SqlLine.
 */
public class SqlLineArgsTest {
  private static final ConnectionSpec CONNECTION_SPEC = ConnectionSpec.HSQLDB;

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
    PrintStream beelineOutputStream = new PrintStream(os);
    beeLine.setOutputStream(beelineOutputStream);
    beeLine.setErrorStream(beelineOutputStream);
    SqlLine.Status status = beeLine.begin(args, null, false);

    return new Pair(status, os.toString("UTF8"));
  }

  private static Pair runScript(File scriptFile, boolean flag)
      throws Throwable {
    List<String> args = new ArrayList<String>();
    Collections.addAll(args,
        "-d", CONNECTION_SPEC.driver,
        "-u", CONNECTION_SPEC.url,
        "-n", CONNECTION_SPEC.username,
        "-p", CONNECTION_SPEC.password);
    if (flag) {
      args.add("-f");
      args.add(scriptFile.getAbsolutePath());
    } else {
      args.add("--run=" + scriptFile.getAbsolutePath());
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
    PrintStream os = new PrintStream(new FileOutputStream(scriptFile));
    os.print(scriptText);
    os.close();

    Pair pair = runScript(scriptFile, flag);

    // Check output before status. It gives a better clue what went wrong.
    assertThat(pair.output, outputMatcher);
    assertThat(pair.status, statusMatcher);
    final boolean delete = scriptFile.delete();
    assertThat(delete, is(true));
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

  /**
   * Tests the "close" command,
   * [HIVE-5768] Beeline connection cannot be closed with '!close' command.
   */
  @Ignore
  @Test
  public void testClose() throws Throwable {
    checkScriptFile("!close 1\n", false, equalTo(SqlLine.Status.OK),
        equalTo("xx"));
  }

  /**
   * Test case for [SQLLINE-32], "!help set' should print documentation for all
   * variables".
   */
  @Test
  public void testHelpSet() throws Throwable {
    checkScriptFile("!help set\n", false, equalTo(SqlLine.Status.OK),
        containsString(
            "1/1          !help set\n"
                + "!set                Set a sqlline variable\n"));

    // Make sure that each variable (autoCommit, autoSave, color, etc.) has a
    // line in the output of '!help set'
    final SqlLine sqlLine = new SqlLine();
    String help = sqlLine.loc("help-set");
    for (String p : sqlLine.getOpts().propertyNamesMixed()) {
      assertThat(help, containsString("\n" + p + " "));
    }
    assertThat(sqlLine.getOpts().propertyNamesMixed().contains("autoCommit"),
        is(true));
    assertThat(sqlLine.getOpts().propertyNamesMixed().contains("autocommit"),
        is(false));
    assertThat(sqlLine.getOpts().propertyNamesMixed().contains("trimScripts"),
        is(true));

    while (!help.isEmpty()) {
      int i = help.indexOf("\n", 1);
      if (i < 0) {
        break;
      }
      if (i > 61) {
        fail("line exceeds 61 chars: " + help.substring(0, i));
      }
      help = help.substring(i);
    }
  }

  /**
   * Test case for [SQLLINE-26], "Flush output for each command when using
   * !record command."
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
            + "1 row selected \\([0-9.]+ seconds\\)\n"
            + "2/7          !record .*.log\n"
            + "Saving all output to \".*.log\". Enter \"record\" with no arguments to stop it.\n"
            + "3/7          !set outputformat csv\n"
            + "4/7          values 2;\n"
            + "'C1'\n"
            + "'2'\n"
            + "1 row selected \\([0-9.]+ seconds\\)\n"
            + "5/7          !record\n"
            + "Recording stopped.\n"
            + "6/7          !set outputformat csv\n"
            + "7/7          values 3;\n"
            + "'C1'\n"
            + "'3'\n"
            + "1 row selected \\([0-9.]+ seconds\\)\n.*"));

    // Now check that the right stuff got into the file.
    final FileReader fileReader = new FileReader(file);
    final StringWriter stringWriter = new StringWriter();
    final char[] chars = new char[1024];
    for (;;) {
      int c = fileReader.read(chars);
      if (c < 0) {
        break;
      }
      stringWriter.write(chars, 0, c);
    }
    assertThat(stringWriter.toString(),
        RegexMatcher.of(
            "Saving all output to \".*.log\". Enter \"record\" with no arguments to stop it.\n"
            + "3/7          !set outputformat csv\n"
            + "4/7          values 2;\n"
            + "'C1'\n"
            + "'2'\n"
            + "1 row selected \\([0-9.]+ seconds\\)\n"
            + "5/7          !record\n"));
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

  /**
   * Attempts to execute a missing script file with the -f option to SqlLine.
   */
  @Test
  public void testNegativeScriptFile() throws Throwable {
    // Create and delete a temp file
    File scriptFile = File.createTempFile("sqllinenegative", "temp");
    final boolean delete = scriptFile.delete();
    assertThat(delete, is(true));

    Pair pair = runScript(scriptFile, true);
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

  /**
   * HIVE-4566, "NullPointerException if typeinfo and nativesql commands are
   * executed at beeline before a DB connection is established".
   *
   * @throws UnsupportedEncodingException
   */
  @Test
  public void testNPE() throws UnsupportedEncodingException {
    SqlLine sqlLine = new SqlLine();

    ByteArrayOutputStream os = new ByteArrayOutputStream();
    PrintStream sqllineOutputStream = new PrintStream(os);
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

    public static final ConnectionSpec HSQLDB =
        new ConnectionSpec(
            "jdbc:hsqldb:res:foodmart", "FOODMART", "FOODMART",
            "org.hsqldb.jdbcDriver");

    public static final ConnectionSpec MYSQL =
        new ConnectionSpec(
            "jdbc:mysql://localhost/foodmart", "foodmart", "foodmart",
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
