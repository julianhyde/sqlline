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

import java.util.Objects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test cases for SQLLine.
 */
public class SqlLineTest {

  /**
   * Unit test for {@link SqlLine#splitCompound(String)}.
   */
  @Test
  public void testSplitCompound() {
    final SqlLine line = new SqlLine();
    String[][] strings;

    // simple line
    strings = line.splitCompound("abc de  fgh");
    assertArrayEquals(new String[][] {{"ABC"}, {"DE"}, {"FGH"}}, strings);

    // line with double quotes
    strings = line.splitCompound("abc \"de fgh\" ijk");
    assertArrayEquals(new String[][] {{"ABC"}, {"de fgh"}, {"IJK"}}, strings);

    // line with double quotes as first and last
    strings = line.splitCompound("\"abc de\"  fgh \"ijk\"");
    assertArrayEquals(new String[][] {{"abc de"}, {"FGH"}, {"ijk"}}, strings);

    // escaped double quotes, and dots inside quoted identifiers
    strings = line.splitCompound("\"ab.c \"\"de\"  fgh.ij");
    assertArrayEquals(new String[][] {{"ab.c \"de"}, {"FGH", "IJ"}}, strings);

    // single quotes do not affect parsing
    strings = line.splitCompound("'abc de'  fgh");
    assertArrayEquals(new String[][] {{"'ABC"}, {"DE'"}, {"FGH"}}, strings);

    // incomplete double-quoted identifiers are implicitly completed
    strings = line.splitCompound("abcdefgh   \"ijk");
    assertArrayEquals(new String[][] {{"ABCDEFGH"}, {"ijk"}}, strings);

    // dot at start of line is illegal, but we are lenient and ignore it
    strings = line.splitCompound(".abc def.gh");
    assertArrayEquals(new String[][] {{"ABC"}, {"DEF", "GH"}}, strings);

    // spaces around dots are fine
    strings = line.splitCompound("abc de .  gh .i. j");
    assertArrayEquals(
        new String[][] {{"ABC"}, {"DE", "GH", "I", "J"}}, strings);

    // double-quote inside an unquoted identifier is treated like a regular
    // character; should be an error, but we are lenient
    strings = line.splitCompound("abc\"de \"fg\"");
    assertArrayEquals(new String[][] {{"ABC\"DE"}, {"fg"}}, strings);

    // null value only if unquoted
    strings = line.splitCompound("abc null");
    assertArrayEquals(new String[][] {{"ABC"}, {null}}, strings);
    strings = line.splitCompound("abc foo.null.bar");
    assertArrayEquals(new String[][] {{"ABC"}, {"FOO", null, "BAR"}}, strings);
    strings = line.splitCompound("abc foo.\"null\".bar");
    assertArrayEquals(
        new String[][] {{"ABC"}, {"FOO", "null", "BAR"}}, strings);
    strings = line.splitCompound("abc foo.\"NULL\".bar");
    assertArrayEquals(
        new String[][] {{"ABC"}, {"FOO", "NULL", "BAR"}}, strings);

    // trim trailing whitespace and semicolon
    strings = line.splitCompound("abc ;\t     ");
    assertArrayEquals(new String[][] {{"ABC"}}, strings);
    // keep semicolon inside line
    strings = line.splitCompound("abc\t;def");
    assertArrayEquals(new String[][] {{"ABC"}, {";DEF"}}, strings);
  }

  @Test
  public void testCenterString() {
    assertEquals("abc", TableOutputFormat.centerString("abc", -1));
    assertEquals("abc", TableOutputFormat.centerString("abc", 1));
    assertEquals("abc ", TableOutputFormat.centerString("abc", 4));
    assertEquals(" abc ", TableOutputFormat.centerString("abc", 5));
    // centerString used to have cartesian performance
    assertEquals(
        1234567, TableOutputFormat.centerString("abc", 1234567).length());
  }

  @Test
  public void testLoadingSystemPropertiesOnCreate() {
    System.setProperty("sqlline.Isolation", "TRANSACTION_NONE");
    SqlLine line = new SqlLine();
    try {
      assertEquals(
          "TRANSACTION_NONE", line.getOpts().getIsolation());
    } finally {
      // set back to the default for tests running in the same JVM.
      System.setProperty("sqlline.Isolation", "TRANSACTION_REPEATABLE_READ");
    }
  }

  @Test
  public void testSplit() {
    SqlLine line = new SqlLine();
    String[] strings;

    // query check
    strings = line.split("values (1, cast(null as integer), "
        + "cast(null as varchar(3));", " ");
    assertArrayEquals(
        new String[]{"values", "(1,", "cast(null", "as", "integer),",
            "cast(null", "as", "varchar(3));"}, strings);
    // space
    strings = line.split("set csvdelimiter ' '", " ");
    assertArrayEquals(new String[]{"set", "csvdelimiter", " "}, strings);
    // space with double quotes
    strings = line.split("set csvdelimiter \" \"", " ");
    assertArrayEquals(new String[]{"set", "csvdelimiter", " "}, strings);
    // table and space
    strings = line.split("set csvdelimiter '\t. '", " ");
    assertArrayEquals(new String[]{"set", "csvdelimiter", "\t. "}, strings);
    // \n
    strings = line.split("set csvdelimiter ' ,\n; '", " ");
    assertArrayEquals(new String[]{"set", "csvdelimiter", " ,\n; "}, strings);
    // double quote inside singles
    strings = line.split("set csvdelimiter ' \"\" \" '", " ");
    assertArrayEquals(
        new String[] {"set", "csvdelimiter", " \"\" \" "}, strings);
    // single quotes inside doubles
    strings = line.split("set csvdelimiter \" ' '' \"", " ");
    assertArrayEquals(new String[]{"set", "csvdelimiter", " ' '' "}, strings);
    // timestamp string
    strings = line.split("set timestampformat 01/01/1970T12:32:12", " ");
    assertArrayEquals(
        new String[]{"set", "timestampformat", "01/01/1970T12:32:12"}, strings);

    strings = line.split("?", " ");
    assertArrayEquals(new String[]{"?"}, strings);

    strings = line.split("#", " ");
    assertArrayEquals(new String[]{"#"}, strings);

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

  @Test
  public void testNextColorScheme() {
    final SqlLine sqlLine = new SqlLine();
    final String initialScheme = sqlLine.getOpts().getColorScheme();
    String currentScheme = null;
    // the artificial limit to check
    // if finally we will come to the initial color scheme
    final int limit = 10000;
    int counter = 0;
    while (counter++ < 10000 && !Objects.equals(initialScheme, currentScheme)) {
      sqlLine.nextColorSchemeWidget();
      currentScheme = sqlLine.getOpts().getColorScheme();
    }
    assertNotEquals(limit, counter);
    assertEquals(initialScheme, currentScheme);
  }
}

// End SqlLineTest.java
