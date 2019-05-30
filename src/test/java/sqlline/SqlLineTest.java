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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
  public void testRpad() {
    assertThat(SqlLine.rpad("x", 1), is("x"));
    assertThat(SqlLine.rpad("x", 2), is("x "));
    assertThat(SqlLine.rpad("xyz", 2), is("xyz"));
    assertThat(SqlLine.rpad(" x ", 5), is(" x   "));
    assertThat(SqlLine.rpad(null, 2), is("  "));
  }

  @Test
  public void testCenterString() {
    assertThat(SqlLine.center("abc", -1), is("abc"));
    assertThat(SqlLine.center("abc", 1), is("abc"));
    assertThat(SqlLine.center("abc", 4), is("abc "));
    assertThat(SqlLine.center("abc", 5), is(" abc "));
    // center used to have cartesian performance
    assertThat(SqlLine.center("abc", 1234567).length(), is(1234567));
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

    strings = line.split("set \"sec\\\"ret\"", " ");
    assertArrayEquals(new String[] {"set", "sec\"ret"}, strings);

    strings = line.split("set \"sec\\\"'ret\"", " ");
    assertArrayEquals(new String[] {"set", "sec\"'ret"}, strings);

    strings = line.split("set \"sec\\\"ret\"", " ");
    assertArrayEquals(new String[] {"set", "sec\"ret"}, strings);

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

  @Test
  public void testOneLineComment() {
    final SqlLine sqlLine = new SqlLine();
    // one line comments only
    assertTrue(sqlLine.isOneLineComment("-- comment"));
    assertTrue(sqlLine.isOneLineComment("-- comment\n-- comment2"));

    // not only one line comments
    assertFalse(sqlLine.isOneLineComment("-- comment\n-- comment2\nselect 1;"));
    assertFalse(sqlLine.isOneLineComment("-- comment\nselect 1-- comment2\n"));
    assertFalse(sqlLine.isOneLineComment("/*comment*/\n-- comment2\n"));
  }

  @Test
  public void testIsCharEscaped() {
    final SqlLine sqlLine = new SqlLine();
    assertTrue(sqlLine.isCharEscaped("\\'", 1));
    assertFalse(sqlLine.isCharEscaped("\\\\\'", 3));
  }

  @Test
  public void testEscapeAndQuote() {
    final SqlLine sqlLine = new SqlLine();
    assertEquals("\"\"", sqlLine.escapeAndQuote(""));
    assertEquals("\"\"", sqlLine.escapeAndQuote(null));
    assertEquals("\"a\"", sqlLine.escapeAndQuote("a"));
    assertEquals("\"a b\"", sqlLine.escapeAndQuote("a b"));
    assertEquals("\"\\\\\"", sqlLine.escapeAndQuote("\\"));
    assertEquals("\"\\\"\"", sqlLine.escapeAndQuote("\""));
    assertEquals("\"'\"", sqlLine.escapeAndQuote("'"));
    assertEquals("\"a\\\\ b\"", sqlLine.escapeAndQuote("a\\ b"));
    assertEquals("\"a\\\\ ' b\"", sqlLine.escapeAndQuote("a\\ ' b"));
    assertEquals("\"a\\\\ ' \\\"b\"", sqlLine.escapeAndQuote("a\\ ' \"b"));
  }

  @Test
  public void testUnescape() {
    final SqlLine sqlLine = new SqlLine();
    assertEquals("\"\"", sqlLine.unescape(sqlLine.escapeAndQuote("")));
    assertEquals("\"\"", sqlLine.unescape(sqlLine.escapeAndQuote(null)));
    assertEquals("\"a\"", sqlLine.unescape(sqlLine.escapeAndQuote("a")));
    assertEquals("\"a b\"", sqlLine.unescape(sqlLine.escapeAndQuote("a b")));
    assertEquals("\"\\\"", sqlLine.unescape(sqlLine.escapeAndQuote("\\")));
    assertEquals("\"\"\"", sqlLine.unescape(sqlLine.escapeAndQuote("\"")));
    assertEquals("\"'\"", sqlLine.unescape(sqlLine.escapeAndQuote("'")));
    assertEquals("\"a\\ b\"",
        sqlLine.unescape(sqlLine.escapeAndQuote("a\\ b")));
    assertEquals("\"a\\\\ ' b\"", sqlLine.escapeAndQuote("a\\ ' b"));
    assertEquals("\"a\\\\ ' \\\"b\"", sqlLine.escapeAndQuote("a\\ ' \"b"));
  }
}

// End SqlLineTest.java
