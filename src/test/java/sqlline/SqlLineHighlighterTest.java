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

import org.jline.utils.AttributedString;

import junit.framework.TestCase;

import static org.junit.Assert.assertNotEquals;

/**
 * Tests for sql and command syntax highlighting in sqlline.
 */
public class SqlLineHighlighterTest extends TestCase {

  private SqlLine sqlLine = null;
  private SqlLineHighlighter highlighter;

  public void setUp() throws Exception {
    sqlLine = getSqlLine();
    highlighter = new SqlLineHighlighter(sqlLine);
  }

  public void tearDown() {
    sqlLine = null;
    highlighter = null;
  }

  public void testCommands() {
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
      checkLine(sqlLine, line, expectedStyle, highlighter);
    }
  }

  public void testKeywords() {
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
      checkLine(sqlLine, line, expectedStyle, highlighter);
    }
  }

  public void testSingleQuotedStrings() {
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
      checkLine(sqlLine, line, expectedStyle, highlighter);
    }
  }

  public void testDoubleQuotedStrings() {
    String[] linesRequiredToBeDoubleQuoted = {
        "\"",
        "\"\"",
        "\"from\"",
        "\"''\"",
        "\"test '' \n''select\"",
        "\"/* \"",
        "\"/*   \"",
        "\"--   \"",
        "\"\n  \n\""
    };

    for (String line : linesRequiredToBeDoubleQuoted) {
      ExpectedHighlightStyle expectedStyle =
          new ExpectedHighlightStyle(line.length());
      expectedStyle.doubleQuotes.set(0, line.length());
      checkLine(sqlLine, line, expectedStyle, highlighter);
    }
  }

  public void testCommentedStrings() {
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
      checkLine(sqlLine, line, expectedStyle, highlighter);
    }
  }

  public void testNumberStrings() {
    String[] linesRequiredToBeNumbers = {
        "123456789",
        "0123",
        "1"
    };

    for (String line : linesRequiredToBeNumbers) {
      ExpectedHighlightStyle expectedStyle =
          new ExpectedHighlightStyle(line.length());
      expectedStyle.numbers.set(0, line.length());
      checkLine(sqlLine, line, expectedStyle, highlighter);
    }
  }

  public void testComplexStrings() throws IOException {
    final SqlLine sqlLine = getSqlLine();
    final SqlLineHighlighter highlighter = new SqlLineHighlighter(sqlLine);

    // command with argument
    String line = "!set version";
    ExpectedHighlightStyle expectedStyle =
        new ExpectedHighlightStyle(line.length());
    expectedStyle.commands.set(0, "!set".length());
    expectedStyle.defaults.set("!set".length(), line.length());
    checkLine(sqlLine, line, expectedStyle, highlighter);

    // command with quoted argument
    line = "!set csvdelimiter '\"'";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.commands.set(0, "!set".length());
    expectedStyle.defaults.set("!set".length(), line.indexOf("'\"'"));
    expectedStyle.singleQuotes.set(line.indexOf("'\"'"), line.length());
    checkLine(sqlLine, line, expectedStyle, highlighter);

    // command with double quoted argument
    line = "!set csvdelimiter \"'\"";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.commands.set(0, "!set".length());
    expectedStyle.defaults.set("!set".length(), line.indexOf("\"'\""));
    expectedStyle.doubleQuotes.set(line.indexOf("\"'\""), line.length());
    checkLine(sqlLine, line, expectedStyle, highlighter);

    // command with double quoted argument and \n
    line = "!set csvdelimiter \"'\n\"";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.commands.set(0, "!set".length());
    expectedStyle.defaults.set("!set".length(), line.indexOf("\"'\n\""));
    expectedStyle.doubleQuotes.set(line.indexOf("\"'\n\""), line.length());
    checkLine(sqlLine, line, expectedStyle, highlighter);

    line = "select '1'";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.keywords.set(0, "select".length());
    expectedStyle.defaults.set("select".length(), line.indexOf(' ') + 1);
    expectedStyle.singleQuotes.set(line.indexOf(' ') + 1, line.length());
    checkLine(sqlLine, line, expectedStyle, highlighter);

    //no spaces
    line = "select'1'as\"21\"";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.keywords.set(0, "select".length());
    expectedStyle.defaults.set("select".length(), line.indexOf('\''));
    expectedStyle.singleQuotes.set(line.indexOf('\''), line.indexOf("as"));
    expectedStyle.keywords.set(line.indexOf("as"), line.indexOf("\"21\""));
    expectedStyle.doubleQuotes.set(line.indexOf("\"21\""), line.length());
    checkLine(sqlLine, line, expectedStyle, highlighter);

    //not valid sql with comments /**/ and not ended quoted line
    line = "select/*123'1'*/'as\"21\"";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.keywords.set(0, "select".length());
    expectedStyle.comments
        .set(line.indexOf("/*123'1'*/"), line.indexOf("'as\"21\""));
    expectedStyle.singleQuotes.set(line.indexOf("'as\"21\""), line.length());
    checkLine(sqlLine, line, expectedStyle, highlighter);

    //not valid sql with not ended multiline comment
    line = "select /*\n * / \n 123 as \"q\" \nfrom dual\n where\n 1 = 1";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.keywords.set(0, "select".length());
    expectedStyle.defaults.set("select".length());
    expectedStyle.comments
        .set(line.indexOf("/*\n"), line.length());
    checkLine(sqlLine, line, expectedStyle, highlighter);

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
    checkLine(sqlLine, line, expectedStyle, highlighter);
  }

  private void checkLine(
      SqlLine sqlLine,
      String line,
      ExpectedHighlightStyle expectedHighlightStyle,
      SqlLineHighlighter highlighter) {
    AttributedString attributedString =
        highlighter.highlight(sqlLine.getLineReader(), line);
    Application.HighlightConfig highlightConfig = sqlLine.getHighlightConfig();
    int commandsStyle = highlightConfig.getCommandStyle().getStyle();
    int keyWordStyle = highlightConfig.getSqlKeywordStyle().getStyle();
    int singleQuoteStyle = highlightConfig.getQuotedStyle().getStyle();
    int doubleQuoteStyle = highlightConfig.getDoubleQuotedStyle().getStyle();
    int commentsStyle = highlightConfig.getCommentedStyle().getStyle();
    int numbersStyle = highlightConfig.getNumbersStyle().getStyle();
    int defaultStyle = highlightConfig.getDefaultStyle().getStyle();

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

  private SqlLine getSqlLine() throws IOException {
    SqlLine sqlLine = new SqlLine();
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    PrintStream sqllineOutputStream =
        new PrintStream(os, false, StandardCharsets.UTF_8.name());
    sqlLine.setOutputStream(sqllineOutputStream);
    sqlLine.setErrorStream(sqllineOutputStream);
    final InputStream is = new ByteArrayInputStream(new byte[0]);
    sqlLine.begin(new String[]{"-e", "!set maxwidth 80"}, is, false);
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
