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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for auxiliary methods in {@link SqlLineHighlighter}.
 */
public class SqlLineHighlighterLowLevelTest {
  private SqlLine defaultSqlline = null;
  private SqlLineHighlighter defaultHighlighter = null;

  @BeforeEach
  public void setUp() throws Exception {
    defaultSqlline = getSqlLine(SqlLineProperty.DEFAULT);
    defaultHighlighter = new SqlLineHighlighter(defaultSqlline);
  }

  @AfterEach
  public void tearDown() {
    defaultSqlline = null;
    defaultHighlighter = null;
  }

  /**
   * This is a low level test of
   * {@link SqlLineHighlighter#handleSqlSingleQuotes(String, BitSet, int)}.
   *
   * <p>WARNING: Change this test only if you know what you are doing.
   * Otherwise, put your test into
   * {@link SqlLineHighlighterTest#testSingleQuotedStrings()},
   */
  @Test
  public void testLowLevelHandleSqlSingleQuotes() {
    String[] linesRequiredToBeQuoted = {
        "'from'",
        "''''",
        "''",
        "'",
        "'test '' \n''select'",
        "'/* \n'",
        "'-- \n--'",
        "'\"'"
    };

    for (String line : linesRequiredToBeQuoted) {
      ExpectedHighlightStyle expectedStyle =
          new ExpectedHighlightStyle(line.length());
      expectedStyle.singleQuotes.set(0, line.length());
      BitSet actual = new BitSet(line.length());
      defaultHighlighter.handleSqlSingleQuotes(line, actual, 0);
      assertEquals(expectedStyle.singleQuotes, actual, "Line [" + line + "]");
    }
  }

  /**
   * Low level test of
   * {@link SqlLineHighlighter#handleSqlIdentifierQuotes}.
   *
   * <p>WARNING: Change this test only if you know what you are doing.
   * Otherwise put your test into
   * {@link SqlLineHighlighterTest#testSqlIdentifierQuotes()}.
   */
  @Test
  public void testLowLevelHandleSqlIdentifierQuotes() {
    String[] linesRequiredToBeDoubleQuoted = {
        "\"",
        "\"\"",
        "\"from\"",
        "\"''\"",
        "\"test '' \n''select\"",
        "\"/* \\\"kjh\"",
        "\"/* \\\" \\\" \\\"  \"",
        "\"--   \"",
        "\"\n  \n\""
    };

    String[] linesRequiredToBeBackTickQuoted = {
        "`",
        "``",
        "`from`",
        "`''`",
        "`test \\` \n\\`select`",
        "`/* \\`kjh`",
        "`/* \\` \\` \\`  `",
        "`--   `",
        "`\n  \n`"
    };

    String[] linesRequiredToBeSquareBracketQuoted = {
        "[]",
        "[from]",
        "['']",
        "[test \\] \n\\]select]",
        "[/* \\]kjh]",
        "[/* \\] \\[ \\]  ]",
        "[--   ]",
        "[\n  \n]"
    };

    for (String line : linesRequiredToBeDoubleQuoted) {
      ExpectedHighlightStyle expectedStyle =
          new ExpectedHighlightStyle(line.length());
      expectedStyle.sqlIdentifierQuotes.set(0, line.length());
      BitSet actual = new BitSet(line.length());
      defaultHighlighter.handleSqlIdentifierQuotes(line, "\"", "\"", actual, 0);
      assertEquals(expectedStyle.sqlIdentifierQuotes, actual,
          "Line [" + line + "]");
    }

    for (String line : linesRequiredToBeBackTickQuoted) {
      ExpectedHighlightStyle expectedStyle =
          new ExpectedHighlightStyle(line.length());
      expectedStyle.sqlIdentifierQuotes.set(0, line.length());
      BitSet actual = new BitSet(line.length());
      defaultHighlighter.handleSqlIdentifierQuotes(line, "`", "`", actual, 0);
      assertEquals(
          expectedStyle.sqlIdentifierQuotes, actual, "Line [" + line + "]");
    }

    for (String line : linesRequiredToBeSquareBracketQuoted) {
      ExpectedHighlightStyle expectedStyle =
          new ExpectedHighlightStyle(line.length());
      expectedStyle.sqlIdentifierQuotes.set(0, line.length());
      BitSet actual = new BitSet(line.length());
      defaultHighlighter.handleSqlIdentifierQuotes(line, "[", "]", actual, 0);
      assertEquals(
          expectedStyle.sqlIdentifierQuotes, actual, "Line [" + line + "]");
    }
  }

  /**
   * Low level test of
   * {@link SqlLineHighlighter#handleComments(String, BitSet, int, boolean)}.
   *
   * <p>WARNING: Change this test only if you know what you are doing.
   * Otherwise put your test into
   * {@link SqlLineHighlighterTest#testCommentedStrings()}.
   */
  @Test
  public void testLowLevelHandleComments() {
    String[] linesRequiredToBeComments = {
        "-- 'asdasd'asd",
        "--select",
        "/* \"''\"",
        "/*",
        "/*/ should be a comment",
        "--",
        "/* kh\n'asd'ad*/",
        "/*\"-- \"values*/"
    };

    for (String line : linesRequiredToBeComments) {
      ExpectedHighlightStyle expectedStyle =
          new ExpectedHighlightStyle(line.length());
      expectedStyle.comments.set(0, line.length());
      BitSet actual = new BitSet(line.length());
      defaultHighlighter.handleComments(line, actual, 0, true);
      assertEquals(expectedStyle.comments, actual, "Line [" + line + "]");
    }
  }

  /**
   * Low level test of
   * {@link SqlLineHighlighter#handleNumbers(String, BitSet, int)}.
   *
   * <p>WARNING: Change this test only if you know what you are doing.
   * Otherwise put your test into
   * {@link SqlLineHighlighterTest#testNumberStrings()}.
   */
  @Test
  public void testLowLevelHandleNumbers() {
    String[] linesRequiredToBeNumbers = {
        "123456789",
        "0123",
        "1"
    };

    for (String line : linesRequiredToBeNumbers) {
      ExpectedHighlightStyle expectedStyle =
          new ExpectedHighlightStyle(line.length());
      expectedStyle.numbers.set(0, line.length());
      BitSet actual = new BitSet(line.length());
      defaultHighlighter.handleNumbers(line, actual, 0);
      assertEquals(expectedStyle.numbers, actual, "Line [" + line + "]");
    }
  }

  /**
   * Low level test of
   * {@link SqlLineHighlighter#handleQuotesInCommands(String, BitSet, BitSet)}.
   *
   * <p>WARNING: Change this test only if you know what you are doing.
   * Otherwise put your test into {@link SqlLineHighlighterTest#testCommands()}
   * or {@link SqlLineHighlighterTest#testComplexStrings()}.
   */
  @Test
  public void testLowLevelQuotesInCommands() {
    String[] commandsWithSingleQuotedInput = {
        "!set csvdelimiter '\"'",
        "!set csvdelimiter '\"\"'",
        "!set csvdelimiter '\"\"\"'",
    };
    String[] commandsWithDoubleQuotedInput = {
        "!set csvdelimiter \"'\"",
        "!set csvdelimiter \"''\"",
        "!set csvdelimiter \"'''\"",
    };

    for (String line : commandsWithSingleQuotedInput) {
      ExpectedHighlightStyle expectedStyle =
          new ExpectedHighlightStyle(line.length());
      expectedStyle.singleQuotes
          .set(line.indexOf("'"), line.length());
      BitSet actualSingleQuotes = new BitSet(line.length());
      BitSet actualDoubleQuotes = new BitSet(line.length());
      defaultHighlighter
          .handleQuotesInCommands(line, actualSingleQuotes, actualDoubleQuotes);
      assertEquals(expectedStyle.singleQuotes, actualSingleQuotes,
          "Line [" + line + "]");
      assertEquals(expectedStyle.sqlIdentifierQuotes, actualDoubleQuotes,
          "Line [" + line + "]");
    }

    for (String line : commandsWithDoubleQuotedInput) {
      ExpectedHighlightStyle expectedStyle =
          new ExpectedHighlightStyle(line.length());
      expectedStyle.sqlIdentifierQuotes
          .set(line.indexOf("\""), line.length());
      BitSet actualSingleQuotes = new BitSet(line.length());
      BitSet actualDoubleQuotes = new BitSet(line.length());
      defaultHighlighter
          .handleQuotesInCommands(line, actualSingleQuotes, actualDoubleQuotes);
      assertEquals(expectedStyle.singleQuotes, actualSingleQuotes,
          "Line [" + line + "]");
      assertEquals(expectedStyle.sqlIdentifierQuotes, actualDoubleQuotes,
          "Line [" + line + "]");
    }
  }

  static SqlLine getSqlLine(String colorScheme) throws IOException {
    SqlLine sqlLine = new SqlLine();
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    PrintStream sqllineOutputStream =
        new PrintStream(os, false, StandardCharsets.UTF_8.name());
    sqlLine.setOutputStream(sqllineOutputStream);
    sqlLine.setErrorStream(sqllineOutputStream);
    final InputStream is = new ByteArrayInputStream(new byte[0]);
    sqlLine.begin(new String[]{"-e", "!set maxwidth 80"}, is, false);
    sqlLine.getOpts().setColorScheme(colorScheme);
    return sqlLine;
  }

  /**
   * Class to test highlight styles.
   */
  static class ExpectedHighlightStyle {
    final BitSet commands;
    final BitSet keywords;
    final BitSet singleQuotes;
    final BitSet sqlIdentifierQuotes;
    final BitSet defaults;
    final BitSet numbers;
    final BitSet comments;

    ExpectedHighlightStyle(int length) {
      commands = new BitSet(length);
      keywords = new BitSet(length);
      singleQuotes = new BitSet(length);
      sqlIdentifierQuotes = new BitSet(length);
      numbers = new BitSet(length);
      comments = new BitSet(length);
      defaults = new BitSet(length);
    }
  }

}

// End SqlLineHighlighterLowLevelTest.java
