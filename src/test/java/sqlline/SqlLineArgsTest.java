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
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;

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
    Assert.assertThat(pair.status, statusMatcher);
    Assert.assertThat(pair.output, outputMatcher);
    final boolean delete = scriptFile.delete();
    Assert.assertThat(delete, is(true));
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
    Assert.assertThat(delete, is(true));

    Pair pair = runScript(scriptFile, true);
    Assert.assertThat(pair.status, equalTo(SqlLine.Status.OTHER));
    Assert.assertThat(pair.output, not(containsString(" 123 ")));
  }

  /** Displays usage. */
  @Test
  public void testUsage() throws Throwable {
    Pair pair = run("--help");
    Assert.assertThat(pair.status, equalTo(SqlLine.Status.ARGS));
    Assert.assertThat(pair.output, containsString("-f <file>"));
    Assert.assertThat(countUsage(pair.output), equalTo(1));
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

  /** Invalid arugments. */
  @Test
  public void testInvalidArguments() throws Throwable {
    Pair pair = run("--fuzz");
    Assert.assertThat(pair.status, equalTo(SqlLine.Status.ARGS));
    Assert.assertThat(pair.output, containsString("-f <file>"));
    Assert.assertThat(countUsage(pair.output), equalTo(1));
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
}

// End SqlLineArgsTest.java
