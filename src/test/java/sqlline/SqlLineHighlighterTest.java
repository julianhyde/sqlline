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

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import mockit.Mock;
import mockit.MockUp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static sqlline.SqlLineHighlighterLowLevelTest.ExpectedHighlightStyle;
import static sqlline.SqlLineHighlighterLowLevelTest.getSqlLine;

/**
 * Tests for sql and command syntax highlighting in sqlline.
 */
public class SqlLineHighlighterTest {

  private Map<SqlLine, SqlLineHighlighter> sqlLine2Highlighter;
  private SqlLine sqlLineWithDefaultColorScheme;

  /**
   * To add your color scheme to tests just put sqlline object
   * with corresponding highlighter into the map like below.
   * @throws Exception if error while sqlline initialization happens
   */
  @BeforeEach
  public void setUp() throws Exception {
    sqlLine2Highlighter = new HashMap<>();
    sqlLineWithDefaultColorScheme = getSqlLine(SqlLineProperty.DEFAULT);
    SqlLine darkSqlLine = getSqlLine("dark");
    SqlLine lightSqlLine = getSqlLine("light");
    sqlLine2Highlighter
        .put(sqlLineWithDefaultColorScheme,
            new SqlLineHighlighter(sqlLineWithDefaultColorScheme));
    sqlLine2Highlighter.put(darkSqlLine, new SqlLineHighlighter(darkSqlLine));
    sqlLine2Highlighter.put(lightSqlLine, new SqlLineHighlighter(lightSqlLine));
  }

  @AfterEach
  public void tearDown() {
    sqlLine2Highlighter = null;
  }

  @Test
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
      checkLineAgainstAllHighlighters(line, expectedStyle);
    }
  }

  @Test
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
      checkLineAgainstAllHighlighters(line, expectedStyle);
    }
  }

  @Test
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
      checkLineAgainstAllHighlighters(line, expectedStyle);
    }
  }

  @Test
  public void testSqlIdentifierQuotes() {
    // default sql identifier is a double quote
    // {@code SqlLineHighlighter#DEFAULT_SQL_IDENTIFIER_QUOTE}.
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
      expectedStyle.sqlIdentifierQuotes.set(0, line.length());
      checkLineAgainstAllHighlighters(line, expectedStyle);
    }
  }

  @Test
  public void testCommentedStrings() {
    String[] linesRequiredToBeComments = {
        "-- 'asdasd'asd",
        "--select",
        "/* \"''\"",
        "/*",
        "/*/ should be a comment",
        "--",
        "--\n/*",
        "/* kh\n'asd'ad*/",
        "/*\"-- \"values*/"
    };

    for (String line : linesRequiredToBeComments) {
      ExpectedHighlightStyle expectedStyle =
          new ExpectedHighlightStyle(line.length());
      expectedStyle.comments.set(0, line.length());
      checkLineAgainstAllHighlighters(line, expectedStyle);
    }
  }

  @Test
  public void testMySqlCommentedStrings() {
    checkMySqlCommentedStrings(DialectImpl.create(null, null, "MySQL"));
    checkMySqlCommentedStrings(BuiltInDialect.MYSQL);
  }

  private void checkMySqlCommentedStrings(final Dialect dialect) {
    new MockUp<DialectImpl>() {
      @Mock
      Dialect getDefault() {
        return dialect;
      }
    };

    String[] linesRequiredToBeComments = {
        "-- 'asdasd'asd",
        "--\n",
        "/* \"''\"",
        "/*",
        "--\t",
        "#",
        "--\n/*",
        "/* kh\n'asd'ad*/",
        "/*\"-- \"values*/"
    };

    for (String line : linesRequiredToBeComments) {
      ExpectedHighlightStyle expectedStyle =
          new ExpectedHighlightStyle(line.length());
      expectedStyle.comments.set(0, line.length());
      checkLineAgainstAllHighlighters(line, expectedStyle);
    }
  }

  @Test
  public void testPhoenixCommentedStrings() {
    checkPhoenixCommentedStrings(DialectImpl.create(null, null, "Phoenix"));
    checkPhoenixCommentedStrings(BuiltInDialect.PHOENIX);
  }

  private void checkPhoenixCommentedStrings(final Dialect dialect) {
    new MockUp<DialectImpl>() {
      @Mock
      Dialect getDefault() {
        return dialect;
      }
    };

    String[] linesRequiredToBeComments = {
        "--'asdasd'asd",
        "--\tselect",
        "// \"''\"",
        "//",
        "--",
        "--\n/*",
        "/* kh\n'asd'ad*/",
        "/*\"-- \"values*/"
    };

    for (String line : linesRequiredToBeComments) {
      ExpectedHighlightStyle expectedStyle =
          new ExpectedHighlightStyle(line.length());
      expectedStyle.comments.set(0, line.length());
      checkLineAgainstAllHighlighters(line, expectedStyle);
    }
  }

  @Test
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
      checkLineAgainstAllHighlighters(line, expectedStyle);
    }
  }

  @Test
  public void testComplexStrings() {
    // comments
    String line = "#!set version";
    ExpectedHighlightStyle expectedStyle =
        new ExpectedHighlightStyle(line.length());
    expectedStyle.comments.set(0, line.length());
    checkLineAgainstAllHighlighters(line, expectedStyle);

    // spaces before comments
    line = "     #  !set";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.defaults.set(0, line.indexOf("#"));
    expectedStyle.comments.set(line.indexOf("#"), line.length());
    checkLineAgainstAllHighlighters(line, expectedStyle);

    line = "     --  select 1 from dual;";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.defaults.set(0, line.indexOf("--"));
    expectedStyle.comments.set(line.indexOf("--"), line.length());
    checkLineAgainstAllHighlighters(line, expectedStyle);

    // odd number of quotes inside comments
    line = "     --  select '1  as \" from dual;";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.defaults.set(0, line.indexOf("--"));
    expectedStyle.comments.set(line.indexOf("--"), line.length());
    checkLineAgainstAllHighlighters(line, expectedStyle);

    // odd number of quotes inside comments
    line = "     #  '`  \"  '2  as \";";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.defaults.set(0, line.indexOf("#"));
    expectedStyle.comments.set(line.indexOf("#"), line.length());
    checkLineAgainstAllHighlighters(line, expectedStyle);

    // command with argument
    line = "!set version";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.commands.set(0, "!set".length());
    expectedStyle.defaults.set("!set".length(), line.length());
    checkLineAgainstAllHighlighters(line, expectedStyle);

    // sqlline comments inside commands should be treated as default text
    line = "!set # -- version";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.commands.set(0, "!set".length());
    expectedStyle.defaults.set("!set".length(), line.length());
    checkLineAgainstAllHighlighters(line, expectedStyle);

    // command with quoted argument
    line = "!set csvdelimiter '\"'";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.commands.set(0, "!set".length());
    expectedStyle.defaults.set("!set".length(), line.indexOf("'\"'"));
    expectedStyle.singleQuotes.set(line.indexOf("'\"'"), line.length());
    checkLineAgainstAllHighlighters(line, expectedStyle);

    // command with double quoted argument
    line = "!set csvdelimiter \"'\"";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.commands.set(0, "!set".length());
    expectedStyle.defaults.set("!set".length(), line.indexOf("\"'\""));
    expectedStyle.sqlIdentifierQuotes.set(line.indexOf("\"'\""), line.length());
    checkLineAgainstAllHighlighters(line, expectedStyle);

    // command with double quoted argument and \n
    line = "!set csvdelimiter \"'\n\"";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.commands.set(0, "!set".length());
    expectedStyle.defaults.set("!set".length(), line.indexOf("\"'\n\""));
    expectedStyle
        .sqlIdentifierQuotes.set(line.indexOf("\"'\n\""), line.length());
    checkLineAgainstAllHighlighters(line, expectedStyle);

    // !connect command with quoted arguments
    line = "!connect \"jdbc:string\" admin 'pass \"word' driver";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.commands.set(0, "!connect".length());
    expectedStyle.defaults.set(line.indexOf(" \"jdbc:string"));
    expectedStyle.sqlIdentifierQuotes.set(
        line.indexOf("\"jdbc:string"), line.indexOf(" admin"));
    expectedStyle.defaults.set(
        line.indexOf(" admin"), line.indexOf("'pass \"word'"));
    expectedStyle.singleQuotes.set(
        line.indexOf("'pass \"word'"), line.indexOf(" driver"));
    expectedStyle.defaults.set(line.indexOf(" driver"), line.length());
    checkLineAgainstAllHighlighters(line, expectedStyle);

    // sql with !sql command
    line = "!sql select '1'";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.commands.set(0, "!sql".length());
    expectedStyle.defaults.set("!sql".length());
    expectedStyle.keywords.set(line.indexOf("select"), line.indexOf(" '1'"));
    expectedStyle.defaults.set(line.indexOf(" '1'"));
    expectedStyle.singleQuotes.set(line.indexOf("'1'"), line.length());
    checkLineAgainstAllHighlighters(line, expectedStyle);

    // sql with !sql command started not from the first symbol
    line = "  !sql select '1' /* comment*/";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.defaults.set(0, line.indexOf("!sql"));
    expectedStyle.commands.set(line.indexOf("!sql"), line.indexOf(" select"));
    expectedStyle.defaults.set(line.indexOf(" select"));
    expectedStyle.keywords.set(line.indexOf("select"), line.indexOf(" '1'"));
    expectedStyle.defaults.set(line.indexOf(" '1'"));
    expectedStyle.singleQuotes
        .set(line.indexOf("'1'"), line.indexOf(" /* comment*/"));
    expectedStyle.defaults.set(line.indexOf(" /* comment*/"));
    expectedStyle.comments.set(line.indexOf("/* comment*/"), line.length());
    checkLineAgainstAllHighlighters(line, expectedStyle);

    // sql with !all command
    line = "!all select '2' as \"two\"; -- comment";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.commands.set(0, "!all".length());
    expectedStyle.defaults.set("!all".length());
    expectedStyle.keywords.set(line.indexOf("select"), line.indexOf(" '2'"));
    expectedStyle.defaults.set(line.indexOf(" '2'"));
    expectedStyle.singleQuotes.set(line.indexOf("'2'"), line.indexOf(" as"));
    expectedStyle.defaults.set(line.indexOf(" as"));
    expectedStyle.keywords.set(line.indexOf("as"), line.indexOf(" \"two"));
    expectedStyle.defaults.set(line.indexOf(" \"two"));
    expectedStyle.sqlIdentifierQuotes
        .set(line.indexOf("\"two\""), line.indexOf(";"));
    expectedStyle.defaults.set(line.indexOf(";"), line.indexOf("--"));
    expectedStyle.comments.set(line.indexOf("--"), line.length());
    checkLineAgainstAllHighlighters(line, expectedStyle);

    // sqlline comments for default dialect
    // inside sql should be treated as default text
    line = "select #'1'";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.keywords.set(0, "select".length());
    expectedStyle.defaults.set("select".length(), line.indexOf('#') + 1);
    expectedStyle.singleQuotes.set(line.indexOf('#') + 1, line.length());
    checkLineAgainstAllHighlighters(line, expectedStyle);

    line = "select '1'";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.keywords.set(0, "select".length());
    expectedStyle.defaults.set("select".length(), line.indexOf(' ') + 1);
    expectedStyle.singleQuotes.set(line.indexOf(' ') + 1, line.length());
    checkLineAgainstAllHighlighters(line, expectedStyle);

    line = "select map[1*5,2],1|2,1^2,1!=0";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.keywords.set(0, "select".length());
    expectedStyle.defaults.set("select".length(), line.indexOf("1*"));
    expectedStyle.numbers.set(line.indexOf("1*"));
    expectedStyle.defaults.set(line.indexOf('*'));
    expectedStyle.numbers.set(line.indexOf('5'));
    expectedStyle.defaults.set(line.indexOf(','));
    expectedStyle.numbers.set(line.indexOf('2'));
    expectedStyle.defaults.set(line.indexOf(']'), line.indexOf("1|"));
    expectedStyle.numbers.set(line.indexOf("1|"));
    expectedStyle.defaults.set(line.indexOf('|'));
    expectedStyle.numbers.set(line.indexOf("2,1"));
    expectedStyle.defaults.set(line.indexOf(",1^"));
    expectedStyle.numbers.set(line.indexOf("1^"));
    expectedStyle.defaults.set(line.indexOf("^2"));
    expectedStyle.numbers.set(line.indexOf("2,1!"));
    expectedStyle.defaults.set(line.indexOf(",1!"));
    expectedStyle.numbers.set(line.indexOf("1!"));
    expectedStyle.defaults.set(line.indexOf("!="), line.indexOf('0'));
    expectedStyle.numbers.set(line.indexOf('0'));
    checkLineAgainstAllHighlighters(line, expectedStyle);

    //no spaces
    line = "select'1'as\"21\"";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.keywords.set(0, "select".length());
    expectedStyle.singleQuotes.set(line.indexOf('\''), line.indexOf("as"));
    expectedStyle.keywords.set(line.indexOf("as"), line.indexOf("\"21\""));
    expectedStyle
        .sqlIdentifierQuotes.set(line.indexOf("\"21\""), line.length());
    checkLineAgainstAllHighlighters(line, expectedStyle);

    //escaped sql identifiers
    line = "select '1' as \"\\\"value\n\\\"\"";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.keywords.set(0, "select".length());
    expectedStyle.defaults.set(line.indexOf(" '"));
    expectedStyle.singleQuotes.set(line.indexOf('\''), line.indexOf(" as"));
    expectedStyle.defaults.set(line.indexOf(" as"));
    expectedStyle
        .keywords.set(line.indexOf("as"), line.indexOf(" \"\\\"value"));
    expectedStyle
        .sqlIdentifierQuotes.set(line.indexOf("\"\\\"value"), line.length());
    checkLineAgainstAllHighlighters(line, expectedStyle);

    //not valid sql with comments /**/ and not ended quoted line
    line = "select/*123'1'*/'as\"21\"";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.keywords.set(0, "select".length());
    expectedStyle.comments
        .set(line.indexOf("/*123'1'*/"), line.indexOf("'as\"21\""));
    expectedStyle.singleQuotes.set(line.indexOf("'as\"21\""), line.length());
    checkLineAgainstAllHighlighters(line, expectedStyle);

    //not valid sql with comments /**/ and not ended sql identifier quoted line
    line = "select/*comment*/ as \"21\\\"";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.keywords.set(0, "select".length());
    expectedStyle.comments
        .set(line.indexOf("/*"), line.indexOf(" as"));
    expectedStyle.defaults.set(line.indexOf(" as"));
    expectedStyle.keywords.set(line.indexOf("as"), line.indexOf(" \"21"));
    expectedStyle.defaults.set(line.indexOf(" \"21"));
    expectedStyle.sqlIdentifierQuotes.set(line.indexOf("\"21"), line.length());
    checkLineAgainstAllHighlighters(line, expectedStyle);

    //not valid sql with not ended multiline comment
    line = "select /*\n * / \n 123 as \"q\" \nfrom dual\n where\n 1 = 1";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.keywords.set(0, "select".length());
    expectedStyle.defaults.set("select".length());
    expectedStyle.comments
        .set(line.indexOf("/*\n"), line.length());
    checkLineAgainstAllHighlighters(line, expectedStyle);

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
    expectedStyle.sqlIdentifierQuotes
        .set(line.indexOf("\"0\","), line.indexOf(",'qwe"));
    expectedStyle.defaults.set(line.indexOf(",'qwe"));
    expectedStyle.singleQuotes
        .set(line.indexOf("'qwe'"), line.indexOf("\n--comment\nas"));
    expectedStyle.defaults.set(line.indexOf("\n--comment"));
    expectedStyle.comments
        .set(line.indexOf("--comment\n"), line.indexOf("as\"21\""));
    expectedStyle.keywords
        .set(line.indexOf("as\"21\""), line.indexOf("\"21\"from"));
    expectedStyle.sqlIdentifierQuotes
        .set(line.indexOf("\"21\""), line.indexOf("from"));
    expectedStyle.keywords.set(line.indexOf("from"), line.indexOf(" t\n"));
    expectedStyle.defaults.set(line.indexOf(" t\n"), line.indexOf("where"));
    expectedStyle.keywords.set(line.indexOf("where"), line.indexOf(" 1=1"));
    expectedStyle.defaults.set(line.indexOf(" 1=1"));
    expectedStyle.numbers.set(line.indexOf("1=1"));
    expectedStyle.defaults.set(line.indexOf("=1"));
    expectedStyle.numbers.set(line.indexOf("=1") + 1);
    checkLineAgainstAllHighlighters(line, expectedStyle);

    //not valid sql with wrong symbols at the and
    line = "select 1 as \"one\" from dual //";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.keywords.set(0, "select".length());
    expectedStyle.defaults.set("select".length());
    expectedStyle.numbers.set(line.indexOf("1"));
    expectedStyle.defaults.set("select 1".length());
    expectedStyle.keywords.set(line.indexOf("as"), line.indexOf(" \"one"));
    expectedStyle.defaults.set(line.indexOf(" \"one"));
    expectedStyle.sqlIdentifierQuotes
        .set(line.indexOf("\"one\""), line.indexOf(" from"));
    expectedStyle.defaults.set(line.indexOf(" from"));
    expectedStyle.keywords.set(line.indexOf("from"), line.indexOf(" dual"));
    expectedStyle.defaults.set(line.indexOf(" dual"), line.length());
    checkLineAgainstAllHighlighters(line, expectedStyle);

    //one line comment first
    line = "-- \nselect 1;";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.comments.set(0, line.indexOf("select"));
    expectedStyle.keywords.set(line.indexOf("select"), line.indexOf(" 1"));
    expectedStyle.defaults.set(line.indexOf(" 1"), line.indexOf("1;"));
    expectedStyle.numbers.set(line.indexOf("1"));
    expectedStyle.defaults.set(line.length() - 1);
    checkLineAgainstAllHighlighters(line, expectedStyle);
  }

  /**
   * The test checks additional highlighting while having connection to db.
   * 1) if keywords from getSQLKeywords are highlighted
   * 2) if a connection is cleared from sqlhighlighter
   * in case the connection is closing
   */
  @Test
  public void testH2SqlKeywordsFromDatabase() {
    // The list is taken from H2 1.4.197 getSQLKeywords output
    String[] linesRequiredToBeConnectionSpecificKeyWords = {
        "LIMIT",
        "MINUS",
        "OFFSET",
        "ROWNUM",
        "SYSDATE",
        "SYSTIME",
        "SYSTIMESTAMP",
        "TODAY",
    };

    for (String line : linesRequiredToBeConnectionSpecificKeyWords) {
      ExpectedHighlightStyle expectedStyle =
          new ExpectedHighlightStyle(line.length());
      expectedStyle.defaults.set(0, line.length());
      checkLineAgainstAllHighlighters(line, expectedStyle);
    }
    DispatchCallback dc = new DispatchCallback();

    for (Map.Entry<SqlLine, SqlLineHighlighter> sqlLine2HighLighterEntry
        : sqlLine2Highlighter.entrySet()) {
      SqlLine sqlLine = sqlLine2HighLighterEntry.getKey();
      sqlLine.runCommands(dc, "!connect "
          + SqlLineArgsTest.ConnectionSpec.H2.url + " "
          + SqlLineArgsTest.ConnectionSpec.H2.username + " \"\"");

      for (String line : linesRequiredToBeConnectionSpecificKeyWords) {
        ExpectedHighlightStyle expectedStyle =
            new ExpectedHighlightStyle(line.length());
        expectedStyle.keywords.set(0, line.length());
        checkLineAgainstHighlighter(
            line, expectedStyle, sqlLine, sqlLine2HighLighterEntry.getValue());
      }

      sqlLine.getDatabaseConnection().close();
    }
  }

  /**
   * The test mocks default sql identifier to square bracket
   * and then checks that after connection done sql
   * identifier quote will be taken from driver
   */
  @Test
  public void testBracketsAsSqlIdentifier() {
    new MockUp<DialectImpl>() {
      @Mock
      Dialect getDefault() {
        return DialectImpl.create(null, "[", null);
      }
    };

    String[] linesWithSquareBracketsSqlIdentifiers = {
        "select 1 as [one] from dual",
        "select 1 as [one two one] from dual",
    };

    String[] linesWithDoubleQuoteSqlIdentifiers = {
        "select 1 as \"one\" from dual",
        "select 1 as \"one two one\" from dual",
    };

    ExpectedHighlightStyle[] expectedStyle =
        new ExpectedHighlightStyle[
            linesWithSquareBracketsSqlIdentifiers.length];
    for (int i = 0; i < expectedStyle.length; i++) {
      String line = linesWithSquareBracketsSqlIdentifiers[i];
      expectedStyle[i] = new ExpectedHighlightStyle(line.length());
      expectedStyle[i].keywords.set(0, "select".length());
      expectedStyle[i].defaults.set(line.indexOf(" 1"));
      expectedStyle[i].numbers.set(line.indexOf("1 as"));
      expectedStyle[i].defaults.set(line.indexOf(" as"));
      expectedStyle[i].keywords.set(line.indexOf("as"), line.indexOf(" [one"));
      expectedStyle[i].defaults.set(line.indexOf(" [one"));
      expectedStyle[i].sqlIdentifierQuotes.
          set(line.indexOf("[one"), line.indexOf(" from"));
      expectedStyle[i].defaults.set(line.indexOf(" from"));
      expectedStyle[i].keywords
          .set(line.indexOf("from"), line.indexOf(" dual"));
      expectedStyle[i].defaults.set(line.indexOf(" dual"), line.length());
      checkLineAgainstAllHighlighters(line, expectedStyle[i]);
    }

    DispatchCallback dc = new DispatchCallback();

    for (Map.Entry<SqlLine, SqlLineHighlighter> sqlLine2HighLighterEntry
        : sqlLine2Highlighter.entrySet()) {
      SqlLine sqlLine = sqlLine2HighLighterEntry.getKey();
      sqlLine.runCommands(dc, "!connect "
          + SqlLineArgsTest.ConnectionSpec.H2.url + " "
          + SqlLineArgsTest.ConnectionSpec.H2.username + " \"\"");

      for (int i = 0; i < linesWithDoubleQuoteSqlIdentifiers.length; i++) {
        checkLineAgainstHighlighter(
            linesWithDoubleQuoteSqlIdentifiers[i],
            expectedStyle[i],
            sqlLine,
            sqlLine2HighLighterEntry.getValue());
      }

      sqlLine.getDatabaseConnection().close();
    }
  }

  /**
   * The test mocks default sql identifier to back tick
   * and then checks that after connection done sql
   * identifier quote will be taken from driver
   */
  @Test
  public void testH2SqlIdentifierFromDatabase() {
    new MockUp<DialectImpl>() {
      @Mock
      Dialect getDefault() {
        return DialectImpl.create(null, "`", null);
      }
    };

    String[] linesWithBackTickSqlIdentifiers = {
        "select 1 as `one` from dual",
        "select 1 as `on\\`e` from dual",
        "select 1 as `on\\`\ne` from dual",
    };

    String[] linesWithDoubleQuoteSqlIdentifiers = {
        "select 1 as \"one\" from dual",
        "select 1 as \"on\\\"e\" from dual",
        "select 1 as \"on\\\"\ne\" from dual",
    };

    ExpectedHighlightStyle[] expectedStyle =
        new ExpectedHighlightStyle[linesWithBackTickSqlIdentifiers.length];
    for (int i = 0; i < expectedStyle.length; i++) {
      String line = linesWithBackTickSqlIdentifiers[i];
      expectedStyle[i] = new ExpectedHighlightStyle(line.length());
      expectedStyle[i].keywords.set(0, "select".length());
      expectedStyle[i].defaults.set(line.indexOf(" 1"));
      expectedStyle[i].numbers.set(line.indexOf("1 as"));
      expectedStyle[i].defaults.set(line.indexOf(" as"));
      expectedStyle[i].keywords.set(line.indexOf("as"), line.indexOf(" `on"));
      expectedStyle[i].defaults.set(line.indexOf(" `on"));
      expectedStyle[i].sqlIdentifierQuotes.
          set(line.indexOf("`on"), line.indexOf(" from"));
      expectedStyle[i].defaults.set(line.indexOf(" from"));
      expectedStyle[i].keywords
          .set(line.indexOf("from"), line.indexOf(" dual"));
      expectedStyle[i].defaults.set(line.indexOf(" dual"), line.length());
      checkLineAgainstAllHighlighters(line, expectedStyle[i]);
    }

    DispatchCallback dc = new DispatchCallback();

    for (Map.Entry<SqlLine, SqlLineHighlighter> sqlLine2HighLighterEntry
        : sqlLine2Highlighter.entrySet()) {
      SqlLine sqlLine = sqlLine2HighLighterEntry.getKey();
      sqlLine.runCommands(dc, "!connect "
          + SqlLineArgsTest.ConnectionSpec.H2.url + " "
          + SqlLineArgsTest.ConnectionSpec.H2.username + " \"\"");

      for (int i = 0; i < linesWithDoubleQuoteSqlIdentifiers.length; i++) {
        checkLineAgainstHighlighter(
            linesWithDoubleQuoteSqlIdentifiers[i],
            expectedStyle[i],
            sqlLine,
            sqlLine2HighLighterEntry.getValue());
      }

      sqlLine.getDatabaseConnection().close();
    }
  }

  /**
   * Check if there is an exception while highlight processing
   * then only the default style is applied
   */
  @Test
  public void testHighlightWithException() {
    new MockUp<SqlLineHighlighter>() {
      @Mock
      void handleSqlSyntax(String buffer, BitSet keywordBitSet,
          BitSet quoteBitSet, BitSet sqlIdentifierQuotesBitSet,
          BitSet commentBitSet, BitSet numberBitSet, boolean isCommandPresent) {
        throw new RuntimeException("Highlight exception");
      }
    };

    String[] linesWithDoubleQuoteSqlIdentifiers = {
        "select 1 as \"one\" from dual",
        "select 1 as \"on\\\"e\" from dual",
        "select 1 as \"on\\\"\ne\" from dual",
    };

    ExpectedHighlightStyle[] expectedStyle =
        new ExpectedHighlightStyle[linesWithDoubleQuoteSqlIdentifiers.length];
    for (int i = 0; i < expectedStyle.length; i++) {
      String line = linesWithDoubleQuoteSqlIdentifiers[i];
      expectedStyle[i] = new ExpectedHighlightStyle(line.length());
      expectedStyle[i].defaults.set(0, line.length());
      checkLineAgainstHighlighter(line,
          expectedStyle[i],
          sqlLineWithDefaultColorScheme,
          sqlLine2Highlighter.get(sqlLineWithDefaultColorScheme));
    }

    DispatchCallback dc = new DispatchCallback();

    for (Map.Entry<SqlLine, SqlLineHighlighter> sqlLine2HighLighterEntry
        : sqlLine2Highlighter.entrySet()) {
      SqlLine sqlLine = sqlLine2HighLighterEntry.getKey();
      sqlLine.runCommands(dc, "!connect "
          + SqlLineArgsTest.ConnectionSpec.H2.url + " "
          + SqlLineArgsTest.ConnectionSpec.H2.username + " \"\"");

      for (int i = 0; i < linesWithDoubleQuoteSqlIdentifiers.length; i++) {
        checkLineAgainstHighlighter(
            linesWithDoubleQuoteSqlIdentifiers[i],
            expectedStyle[i],
            sqlLineWithDefaultColorScheme,
            sqlLine2Highlighter.get(sqlLineWithDefaultColorScheme));
      }

      sqlLine.getDatabaseConnection().close();
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
    int commandStyle = highlightStyle.getCommandStyle().getStyle();
    int keywordStyle = highlightStyle.getKeywordStyle().getStyle();
    int singleQuoteStyle = highlightStyle.getQuotedStyle().getStyle();
    int identifierStyle = highlightStyle.getIdentifierStyle().getStyle();
    int commentStyle = highlightStyle.getCommentStyle().getStyle();
    int numberStyle = highlightStyle.getNumberStyle().getStyle();
    int defaultStyle = highlightStyle.getDefaultStyle().getStyle();

    for (int i = 0; i < line.length(); i++) {
      checkSymbolStyle(line, i, expectedHighlightStyle.commands,
          attributedString, commandStyle, "command");

      checkSymbolStyle(line, i, expectedHighlightStyle.keywords,
          attributedString, keywordStyle, "key word");

      checkSymbolStyle(line, i, expectedHighlightStyle.singleQuotes,
          attributedString, singleQuoteStyle, "single quote");

      checkSymbolStyle(line, i, expectedHighlightStyle.sqlIdentifierQuotes,
          attributedString, identifierStyle, "sql identifier quote");

      checkSymbolStyle(line, i, expectedHighlightStyle.numbers,
          attributedString, numberStyle, "number");

      checkSymbolStyle(line, i, expectedHighlightStyle.comments,
          attributedString, commentStyle, "comment");

      checkSymbolStyle(line, i, expectedHighlightStyle.defaults,
          attributedString, defaultStyle, "default");
    }
  }

  private void checkDefaultLine(
      SqlLine sqlLine,
      String line,
      SqlLineHighlighter defaultHighlighter) {
    final AttributedString attributedString =
        defaultHighlighter.highlight(sqlLine.getLineReader(), line);
    int defaultStyle = AttributedStyle.DEFAULT.getStyle();

    for (int i = 0; i < line.length(); i++) {
      if (Character.isWhitespace(line.charAt(i))) {
        continue;
      }
      assertEquals(i == 0 ? defaultStyle + 32 : defaultStyle,
          attributedString.styleAt(i).getStyle(),
          getFailedStyleMessage(line, i, "default"));
    }
  }

  private void checkLineAgainstAllHighlighters(
      String line, ExpectedHighlightStyle expectedHighlightStyle) {
    for (Map.Entry<SqlLine, SqlLineHighlighter> mapEntry
        : sqlLine2Highlighter.entrySet()) {
      checkLineAgainstHighlighter(
          line, expectedHighlightStyle, mapEntry.getKey(), mapEntry.getValue());
    }
  }

  private void checkLineAgainstHighlighter(
      String line,
      ExpectedHighlightStyle expectedHighlightStyle,
      SqlLine sqlLine,
      SqlLineHighlighter sqlLineHighlighter) {
    if (SqlLineProperty.DEFAULT.equals(sqlLine.getOpts().getColorScheme())) {
      checkDefaultLine(sqlLine, line, sqlLineHighlighter);
    } else {
      checkHighlightedLine(sqlLine,
          line, expectedHighlightStyle, sqlLineHighlighter);
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
      assertEquals(i == 0 ? style + 32 : style,
          highlightedLine.styleAt(i).getStyle(),
          getFailedStyleMessage(line, i, styleName));
    } else {
      if (!Character.isWhitespace(line.charAt(i))) {
        assertNotEquals(i == 0 ? style + 32 : style,
            highlightedLine.styleAt(i).getStyle(),
            getNegativeFailedStyleMessage(line, i, styleName));
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

}

// End SqlLineHighlighterTest.java
