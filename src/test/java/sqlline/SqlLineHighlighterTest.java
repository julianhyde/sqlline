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
import java.util.Collections;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Tests for sql and command syntax highlighting in sqlline.
 */
public class SqlLineHighlighterTest {

  private SqlLine darkSqlLine = null;
  private SqlLine defaultSqlline = null;
  private SqlLine lightSqlLine = null;
  private SqlLineHighlighter darkHighlighter;
  private SqlLineHighlighter defaultHighlighter;
  private SqlLineHighlighter lightHighlighter;

  @Before public void setUp() throws Exception {
    darkSqlLine = getSqlLine(SqlLineOpts.DARK_SCHEME);
    defaultSqlline = getSqlLine(SqlLineOpts.DEFAULT);
    lightSqlLine = getSqlLine(SqlLineOpts.LIGHT_SCHEME);
    darkHighlighter = new SqlLineHighlighter(darkSqlLine);
    defaultHighlighter = new SqlLineHighlighter(defaultSqlline);
    lightHighlighter = new SqlLineHighlighter(lightSqlLine);
  }

  @After public void tearDown() {
    darkSqlLine = null;
    defaultSqlline = null;
    lightSqlLine = null;
    darkHighlighter = null;
    defaultHighlighter = null;
    lightHighlighter = null;
  }

  @Test public void testCommands() {
    String[] linesRequiredToBeCommands = {
        "!set",
        "!commandhandler",
        "!quit",
        "!isolation",
        "!dbinfo",
        "!help",
        "!connect"
    };

    for (String line : linesRequiredToBeCommands) {
      ExpectedHighlightStyle expectedStyle =
          new ExpectedHighlightStyle(line.length());
      expectedStyle.commands.set(0, line.length());
      checkLine(line, expectedStyle);
    }
  }

  @Test public void testKeywords() {
    String[] linesRequiredToBeKeywords = {
        "from",
        "outer",
        "select",
        "values",
        "where",
        "join",
        "cross"
    };

    for (String line : linesRequiredToBeKeywords) {
      ExpectedHighlightStyle expectedStyle =
          new ExpectedHighlightStyle(line.length());
      expectedStyle.keywords.set(0, line.length());
      checkLine(line, expectedStyle);
    }
  }

  @Test public void testSingleQuotedStrings() {
    String[] linesRequiredToBeSingleQuoted = {
        "'from'",
        "''''",
        "''",
        "'",
        "'test '' \n''select'",
        "'/* \n'",
        "'-- \n--'",
        "'\"'"
    };

    for (String line : linesRequiredToBeSingleQuoted) {
      ExpectedHighlightStyle expectedStyle =
          new ExpectedHighlightStyle(line.length());
      expectedStyle.singleQuotes.set(0, line.length());
      checkLine(line, expectedStyle);
    }
  }

  @Test public void testDoubleQuotedStrings() {
    String[] linesRequiredToBeDoubleQuoted = {
        "\"",
        "\"\"",
        "\"from\"",
        "\"''\"",
        "\"test '' \n''select\"",
        "\"/* \\\"kjh\"",
        "\"/*   \"",
        "\"--   \"",
        "\"\n  \n\""
    };

    for (String line : linesRequiredToBeDoubleQuoted) {
      ExpectedHighlightStyle expectedStyle =
          new ExpectedHighlightStyle(line.length());
      expectedStyle.doubleQuotes.set(0, line.length());
      checkLine(line, expectedStyle);
    }
  }

  @Test public void testCommentedStrings() {
    String[] linesRequiredToBeComments = {
        "-- 'asdasd'asd",
        "--select",
        "/* \"''\"",
        "/*",
        "--",
        "/* kh\n'asd'ad*/",
        "/*\"-- \"values*/"
    };

    for (String line : linesRequiredToBeComments) {
      ExpectedHighlightStyle expectedStyle =
          new ExpectedHighlightStyle(line.length());
      expectedStyle.comments.set(0, line.length());
      checkLine(line, expectedStyle);
    }
  }

  @Test public void testNumberStrings() {
    String[] linesRequiredToBeNumbers = {
        "123456789",
        "0123",
        "1"
    };

    for (String line : linesRequiredToBeNumbers) {
      ExpectedHighlightStyle expectedStyle =
          new ExpectedHighlightStyle(line.length());
      expectedStyle.numbers.set(0, line.length());
      checkLine(line, expectedStyle);
    }
  }

  @Test public void testComplexStrings() {
    // command with argument
    String line = "!set version";
    ExpectedHighlightStyle expectedStyle =
        new ExpectedHighlightStyle(line.length());
    expectedStyle.commands.set(0, "!set".length());
    expectedStyle.defaults.set("!set".length(), line.length());
    checkLine(line, expectedStyle);

    // command with quoted argument
    line = "!set csvdelimiter '\"'";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.commands.set(0, "!set".length());
    expectedStyle.defaults.set("!set".length(), line.indexOf("'\"'"));
    expectedStyle.singleQuotes.set(line.indexOf("'\"'"), line.length());
    checkLine(line, expectedStyle);

    // command with double quoted argument
    line = "!set csvdelimiter \"'\"";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.commands.set(0, "!set".length());
    expectedStyle.defaults.set("!set".length(), line.indexOf("\"'\""));
    expectedStyle.doubleQuotes.set(line.indexOf("\"'\""), line.length());
    checkLine(line, expectedStyle);

    // command with double quoted argument and \n
    line = "!set csvdelimiter \"'\n\"";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.commands.set(0, "!set".length());
    expectedStyle.defaults.set("!set".length(), line.indexOf("\"'\n\""));
    expectedStyle.doubleQuotes.set(line.indexOf("\"'\n\""), line.length());
    checkLine(line, expectedStyle);

    line = "select '1'";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.keywords.set(0, "select".length());
    expectedStyle.defaults.set("select".length(), line.indexOf(' ') + 1);
    expectedStyle.singleQuotes.set(line.indexOf(' ') + 1, line.length());
    checkLine(line, expectedStyle);

    //no spaces
    line = "select'1'as\"21\"";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.keywords.set(0, "select".length());
    expectedStyle.defaults.set("select".length(), line.indexOf('\''));
    expectedStyle.singleQuotes.set(line.indexOf('\''), line.indexOf("as"));
    expectedStyle.keywords.set(line.indexOf("as"), line.indexOf("\"21\""));
    expectedStyle.doubleQuotes.set(line.indexOf("\"21\""), line.length());
    checkLine(line, expectedStyle);

    //not valid sql with comments /**/ and not ended quoted line
    line = "select/*123'1'*/'as\"21\"";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.keywords.set(0, "select".length());
    expectedStyle.comments
        .set(line.indexOf("/*123'1'*/"), line.indexOf("'as\"21\""));
    expectedStyle.singleQuotes.set(line.indexOf("'as\"21\""), line.length());
    checkLine(line, expectedStyle);

    //not valid sql with not ended multiline comment
    line = "select /*\n * / \n 123 as \"q\" \nfrom dual\n where\n 1 = 1";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.keywords.set(0, "select".length());
    expectedStyle.defaults.set("select".length());
    expectedStyle.comments
        .set(line.indexOf("/*\n"), line.length());
    checkLine(line, expectedStyle);

    //multiline sql with comments
    line = "select/*multiline\ncomment\n*/0 as \"0\","
        + "'qwe'\n--comment\nas\"21\"from t\n where 1=1";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.keywords.set(0, "select".length());
    expectedStyle.comments.set("select".length(), line.indexOf("0 as"));
    expectedStyle.numbers.set(line.indexOf("0 as"));
    expectedStyle.defaults.set(line.indexOf(" as \"0\","));
    expectedStyle.keywords
        .set(line.indexOf("as \"0\","), line.indexOf(" \"0\","));
    expectedStyle.defaults.set(line.indexOf(" \"0\","));
    expectedStyle.doubleQuotes
        .set(line.indexOf("\"0\","), line.indexOf(",'qwe"));
    expectedStyle.defaults.set(line.indexOf(",'qwe"));
    expectedStyle.singleQuotes
        .set(line.indexOf("'qwe'"), line.indexOf("\n--comment\nas"));
    expectedStyle.defaults.set(line.indexOf("\n--comment"));
    expectedStyle.comments
        .set(line.indexOf("--comment\n"), line.indexOf("as\"21\""));
    expectedStyle.keywords
        .set(line.indexOf("as\"21\""), line.indexOf("\"21\"from"));
    expectedStyle.doubleQuotes
        .set(line.indexOf("\"21\""), line.indexOf("from"));
    expectedStyle.keywords.set(line.indexOf("from"), line.indexOf(" t\n"));
    expectedStyle.defaults.set(line.indexOf(" t\n"), line.indexOf("where"));
    expectedStyle.keywords.set(line.indexOf("where"), line.indexOf(" 1=1"));
    expectedStyle.defaults.set(line.indexOf(" 1=1"));
    expectedStyle.numbers.set(line.indexOf("1=1"));
    expectedStyle.defaults.set(line.indexOf("=1"));
    expectedStyle.numbers.set(line.indexOf("=1") + 1);
    checkLine(line, expectedStyle);
  }

  @Test public void testSqlKeywordsFromDatabase() {
    String[] linesRequiredToBeNumbers = {
        "minus",
        "today",
    };

    for (String line : linesRequiredToBeNumbers) {
      ExpectedHighlightStyle expectedStyle =
          new ExpectedHighlightStyle(line.length());
      expectedStyle.defaults.set(0, line.length());
      checkLine(line, expectedStyle);
    }

    DispatchCallback dc = new DispatchCallback();
    darkSqlLine.runCommands(
        Collections.singletonList("!connect "
            + SqlLineArgsTest.ConnectionSpec.H2.url + " "
            + SqlLineArgsTest.ConnectionSpec.H2.username + " \"\""),
        dc);

    for (String line : linesRequiredToBeNumbers) {
      ExpectedHighlightStyle expectedStyle =
          new ExpectedHighlightStyle(line.length());
      expectedStyle.keywords.set(0, line.length());
      checkHighlightedLine(
          darkSqlLine, line, expectedStyle, darkHighlighter);
    }
  }

  private void checkHighlightedLine(
      SqlLine sqlLine,
      String line,
      ExpectedHighlightStyle expectedHighlightStyle,
      SqlLineHighlighter highlighter) {
    final AttributedString attributedString =
        highlighter.highlight(sqlLine.getLineReader(), line);
    final HighlightStyle highlightStyle = sqlLine.getHighlightStyle();
    int commandsStyle = highlightStyle.getCommandStyle().getStyle();
    int keyWordStyle = highlightStyle.getSqlKeywordStyle().getStyle();
    int singleQuoteStyle = highlightStyle.getQuotedStyle().getStyle();
    int doubleQuoteStyle = highlightStyle.getDoubleQuotedStyle().getStyle();
    int commentsStyle = highlightStyle.getCommentedStyle().getStyle();
    int numbersStyle = highlightStyle.getNumbersStyle().getStyle();
    int defaultStyle = highlightStyle.getDefaultStyle().getStyle();

    for (int i = 0; i < line.length(); i++) {
      checkSymbolStyle(line, i, expectedHighlightStyle.commands,
          attributedString, commandsStyle, "command");

      checkSymbolStyle(line, i, expectedHighlightStyle.keywords,
          attributedString, keyWordStyle, "key word");

      checkSymbolStyle(line, i, expectedHighlightStyle.singleQuotes,
          attributedString, singleQuoteStyle, "single quote");

      checkSymbolStyle(line, i, expectedHighlightStyle.doubleQuotes,
          attributedString, doubleQuoteStyle, "double quote");

      checkSymbolStyle(line, i, expectedHighlightStyle.numbers,
          attributedString, numbersStyle, "number");

      checkSymbolStyle(line, i, expectedHighlightStyle.comments,
          attributedString, commentsStyle, "comment");

      checkSymbolStyle(line, i, expectedHighlightStyle.defaults,
          attributedString, defaultStyle, "default");
    }
  }

  private void checkDefaultLine(
      SqlLine sqlLine,
      String line) {
    final AttributedString attributedString =
        defaultHighlighter.highlight(sqlLine.getLineReader(), line);
    int defaultStyle = AttributedStyle.DEFAULT.getStyle();

    for (int i = 0; i < line.length(); i++) {
      if (Character.isWhitespace(line.charAt(i))) {
        continue;
      }
      assertEquals(getFailedStyleMessage(line, i, "default"),
          i == 0 ? defaultStyle + 32 : defaultStyle,
          attributedString.styleAt(i).getStyle());
    }
  }

  private void checkLine(
      String line, ExpectedHighlightStyle expectedHighlightStyle) {
    checkHighlightedLine(
        darkSqlLine, line, expectedHighlightStyle, darkHighlighter);
    checkHighlightedLine(
        lightSqlLine, line, expectedHighlightStyle, lightHighlighter);
    checkDefaultLine(defaultSqlline, line);
  }

  private void checkSymbolStyle(
      String line,
      int i,
      BitSet styleBitSet,
      AttributedString highlightedLine,
      int style,
      String styleName) {
    if (styleBitSet.get(i)) {
      assertEquals(getFailedStyleMessage(line, i, styleName),
          i == 0 ? style + 32 : style,
          highlightedLine.styleAt(i).getStyle());
    } else {
      if (!Character.isWhitespace(line.charAt(i))) {
        assertNotEquals(getNegativeFailedStyleMessage(line, i, styleName),
            i == 0 ? style + 32 : style,
            highlightedLine.styleAt(i).getStyle());
      }
    }
  }

  private String getFailedStyleMessage(String line, int i, String style) {
    return getFailedStyleMessage(line, i, style, true);
  }

  private String getNegativeFailedStyleMessage(
      String line, int i, String style) {
    return getFailedStyleMessage(line, i, style, false);
  }

  private String getFailedStyleMessage(
      String line, int i, String style, boolean positive) {
    return "String '" + line + "', symbol '" + line.charAt(i)
        + "' at (" + i + ") " + "position should "
        + (positive ? "" : "not ") + "be " + style + " style";
  }

  private SqlLine getSqlLine(String colorScheme) throws IOException {
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
   *  Class to test highlight styles.
   */
  private class ExpectedHighlightStyle {
    private final BitSet commands;
    private final BitSet keywords;
    private final BitSet singleQuotes;
    private final BitSet doubleQuotes;
    private final BitSet defaults;
    private final BitSet numbers;
    private final BitSet comments;

    ExpectedHighlightStyle(int length) {
      commands = new BitSet(length);
      keywords = new BitSet(length);
      singleQuotes = new BitSet(length);
      doubleQuotes = new BitSet(length);
      numbers = new BitSet(length);
      comments = new BitSet(length);
      defaults = new BitSet(length);
    }
  }
}

// End SqlLineHighlighterTest.java
