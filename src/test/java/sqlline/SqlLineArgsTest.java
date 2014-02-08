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
   * Execute a script with "beeline -f"
   * @throws java.lang.Throwable On error
   * @return The stderr and stdout from running the script
   *
   * @param args Script arguments
   */
  private static String run(String... args) throws Throwable {
    SqlLine beeLine = new SqlLine();
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    PrintStream beelineOutputStream = new PrintStream(os);
    beeLine.setOutputStream(beelineOutputStream);
    beeLine.setErrorStream(beelineOutputStream);
    beeLine.begin(args, null, false);

    return os.toString("UTF8");
  }

  private static String runScript(File scriptFile, boolean flag)
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
   * Attempt to execute a simple script file with the -f option to SqlLine
   * Test for presence of an expected pattern
   * in the output (stdout or stderr), fail if not found
   * Print PASSED or FAILED.
   *
   * @param matcher Matches whether the test output is as desired
   * @param flag Command flag (-run or -f)
   * @throws Exception on command execution error
   */
  private void testScriptFile(String scriptText, Matcher<String> matcher,
      boolean flag) throws Throwable {
    // Put the script content in a temp file
    File scriptFile = File.createTempFile("foo", "temp");
    scriptFile.deleteOnExit();
    PrintStream os = new PrintStream(new FileOutputStream(scriptFile));
    os.print(scriptText);
    os.close();

    String output = runScript(scriptFile, flag);
    Assert.assertThat(output, matcher);
    final boolean delete = scriptFile.delete();
    Assert.assertThat(delete, is(true));
  }

  /**
   * Attempt to execute a simple script file with the -f option to SqlLine
   * Test for presence of an expected pattern
   * in the output (stdout or stderr), fail if not found
   * Print PASSED or FAILED
   */
  @Test
  public void testPositiveScriptFile() throws Throwable {
    testScriptFile("call 100 + 23;\n", containsString(" 123 "), true);
  }

  /**
   * As above, but using '-run' rather than '-f'.
   */
  @Test
  public void testPositiveScriptFileUsingRun() throws Throwable {
    testScriptFile("call 100 + 23;\n", containsString(" 123 "), false);
  }

  /**
   * Attempts to execute a simple script file with the -f option to SqlLine.
   * The first command should fail and the second command should not execute.
   */
  @Test
  public void testBreakOnErrorScriptFile() throws Throwable {
    testScriptFile("select * from abcdefg01;\ncall 100 + 23;\n",
        not(containsString(" 123 ")), true);
  }

  /**
   * Attempts to execute a missing script file with the -f option to SqlLine.
   */
  @Test
  public void testNegativeScriptFile() throws Throwable {
    // Create and delete a temp file
    File scriptFile = File.createTempFile("beelinenegative", "temp");
    final boolean delete = scriptFile.delete();
    Assert.assertThat(delete, is(true));

    String output = runScript(scriptFile, true);
    Assert.assertThat(output, not(containsString(" 123 ")));
  }

  /** Displays usage. */
  @Test
  public void testUsage() throws Throwable {
    String result = run("--help");
    Assert.assertThat(result, containsString("-f <file>"));
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
