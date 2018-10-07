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

import junit.framework.TestCase;

/**
 * Test cases for SQLLine.
 */
public class SqlLineTest extends TestCase {
  /**
   * Public constructor.
   */
  public SqlLineTest() {}

  /**
   * Public constructor with test name, required by junit.
   *
   * @param testName Test name
   */
  public SqlLineTest(String testName) {
    super(testName);
  }

  /**
   * Unit test for {@link SqlLine#splitCompound(String)}.
   */
  public void testSplitCompound() {
    final SqlLine line = new SqlLine();
    String[][] strings;

    // simple line
    strings = line.splitCompound("abc de  fgh");
    assertEquals(new String[][] {{"ABC"}, {"DE"}, {"FGH"}}, strings);

    // line with double quotes
    strings = line.splitCompound("abc \"de fgh\" ijk");
    assertEquals(new String[][] {{"ABC"}, {"de fgh"}, {"IJK"}}, strings);

    // line with double quotes as first and last
    strings = line.splitCompound("\"abc de\"  fgh \"ijk\"");
    assertEquals(new String[][] {{"abc de"}, {"FGH"}, {"ijk"}}, strings);

    // escaped double quotes, and dots inside quoted identifiers
    strings = line.splitCompound("\"ab.c \"\"de\"  fgh.ij");
    assertEquals(new String[][] {{"ab.c \"de"}, {"FGH", "IJ"}}, strings);

    // single quotes do not affect parsing
    strings = line.splitCompound("'abc de'  fgh");
    assertEquals(new String[][] {{"'ABC"}, {"DE'"}, {"FGH"}}, strings);

    // incomplete double-quoted identifiers are implicitly completed
    strings = line.splitCompound("abcdefgh   \"ijk");
    assertEquals(new String[][] {{"ABCDEFGH"}, {"ijk"}}, strings);

    // dot at start of line is illegal, but we are lenient and ignore it
    strings = line.splitCompound(".abc def.gh");
    assertEquals(new String[][] {{"ABC"}, {"DEF", "GH"}}, strings);

    // spaces around dots are fine
    strings = line.splitCompound("abc de .  gh .i. j");
    assertEquals(new String[][] {{"ABC"}, {"DE", "GH", "I", "J"}}, strings);

    // double-quote inside an unquoted identifier is treated like a regular
    // character; should be an error, but we are lenient
    strings = line.splitCompound("abc\"de \"fg\"");
    assertEquals(new String[][] {{"ABC\"DE"}, {"fg"}}, strings);

    // null value only if unquoted
    strings = line.splitCompound("abc null");
    assertEquals(new String[][] {{"ABC"}, {null}}, strings);
    strings = line.splitCompound("abc foo.null.bar");
    assertEquals(new String[][] {{"ABC"}, {"FOO", null, "BAR"}}, strings);
    strings = line.splitCompound("abc foo.\"null\".bar");
    assertEquals(new String[][] {{"ABC"}, {"FOO", "null", "BAR"}}, strings);
    strings = line.splitCompound("abc foo.\"NULL\".bar");
    assertEquals(new String[][] {{"ABC"}, {"FOO", "NULL", "BAR"}}, strings);

    // trim trailing whitespace and semicolon
    strings = line.splitCompound("abc ;\t     ");
    assertEquals(new String[][] {{"ABC"}}, strings);
    // keep semicolon inside line
    strings = line.splitCompound("abc\t;def");
    assertEquals(new String[][] {{"ABC"}, {";DEF"}}, strings);
  }

  public void testCenterString() {
    assertEquals("abc", ColorBuffer.centerString("abc", -1));
    assertEquals("abc", ColorBuffer.centerString("abc", 1));
    assertEquals("abc ", ColorBuffer.centerString("abc", 4));
    assertEquals(" abc ", ColorBuffer.centerString("abc", 5));
    // centerString used to have cartesian performance
    assertEquals(1234567, ColorBuffer.centerString("abc", 1234567).length());
  }

  public void testLoadingSystemPropertiesOnCreate() {
    System.setProperty("sqlline.Isolation", "TRANSACTION_NONE");
    SqlLine line = new SqlLine();
    try {
      assertEquals("TRANSACTION_NONE", line.getOpts().getIsolation());
    } finally {
      // set back to the default for tests running in the same JVM.
      System.setProperty("sqlline.Isolation", "TRANSACTION_REPEATABLE_READ");
    }
  }

  public void testSplit() {
    SqlLine line = new SqlLine();
    String[] strings;

    // query check
    strings = line.split("values (1, cast(null as integer), "
        + "cast(null as varchar(3));", " ");
    assertEquals(new String[] {"values", "(1,", "cast(null", "as", "integer),",
        "cast(null", "as", "varchar(3));"}, strings);
    // space
    strings = line.split("set csvdelimiter ' '", " ");
    assertEquals(new String[]{"set", "csvdelimiter", " "}, strings);
    // space with double quotes
    strings = line.split("set csvdelimiter \" \"", " ");
    assertEquals(new String[]{"set", "csvdelimiter", " "}, strings);
    // table and space
    strings = line.split("set csvdelimiter '\t. '", " ");
    assertEquals(new String[]{"set", "csvdelimiter", "\t. "}, strings);
    // \n
    strings = line.split("set csvdelimiter ' ,\n; '", " ");
    assertEquals(new String[]{"set", "csvdelimiter", " ,\n; "}, strings);
    // double quote inside singles
    strings = line.split("set csvdelimiter ' \"\" \" '", " ");
    assertEquals(new String[]{"set", "csvdelimiter", " \"\" \" "}, strings);
    // single quotes inside doubles
    strings = line.split("set csvdelimiter \" ' '' \"", " ");
    assertEquals(new String[]{"set", "csvdelimiter", " ' '' "}, strings);
    // timestamp string
    strings = line.split("set timestampformat 01/01/1970T12:32:12", " ");
    assertEquals(
        new String[]{"set", "timestampformat", "01/01/1970T12:32:12"}, strings);

    strings = line.split("?", " ");
    assertEquals(new String[]{"?"}, strings);

    strings = line.split("#", " ");
    assertEquals(new String[]{"#"}, strings);

    try {
      line.split("set csvdelimiter '", " ");
      fail("Non-paired quote is not allowed");
    } catch (IllegalArgumentException e) {
      //ok
    }
    try {
      line.split("set csvdelimiter \"", " ");
      fail("Non-paired quote is not allowed");
    } catch (IllegalArgumentException e) {
      //ok
    }
    try {
      line.split("set csvdelimiter \"'", " ");
      fail("Non-paired quote is not allowed");
    } catch (IllegalArgumentException e) {
      //ok
    }
    try {
      line.split("set csvdelimiter \"a", " ");
      fail("Non-paired quote is not allowed");
    } catch (IllegalArgumentException e) {
      //ok
    }
    try {
      line.split("set csvdelimiter q'", " ");
      fail("Non-paired quote is not allowed");
    } catch (IllegalArgumentException e) {
      //ok
    }
    try {
      line.dequote("\"'");
      fail("Non-paired quote is not allowed");
    } catch (IllegalArgumentException e) {
      //ok
    }
    try {
      line.dequote("\"a");
      fail("Non-paired quote is not allowed");
    } catch (IllegalArgumentException e) {
      //ok
    }
    try {
      line.dequote("q'");
      fail("Non-paired quote is not allowed");
    } catch (IllegalArgumentException e) {
      //ok
    }
  }

  void assertEquals(String[][] expectedses, String[][] actualses) {
    assertEquals(expectedses.length, actualses.length);
    for (int i = 0; i < expectedses.length; ++i) {
      String[] expecteds = expectedses[i];
      String[] actuals = actualses[i];
      assertEquals(expecteds.length, actuals.length);
      for (int j = 0; j < expecteds.length; ++j) {
        String expected = expecteds[j];
        String actual = actuals[j];
        assertEquals(expected, actual);
      }
    }
  }

  void assertEquals(String[] expectedses, String[] actualses) {
    assertEquals(expectedses.length, actualses.length);
    for (int j = 0; j < expectedses.length; ++j) {
      String expected = expectedses[j];
      String actual = actualses[j];
      if (expected == null) {
        assertNull(actual);
      } else {
        assertEquals(expected, actual);
      }
    }
  }
}

// End SqlLineTest.java
