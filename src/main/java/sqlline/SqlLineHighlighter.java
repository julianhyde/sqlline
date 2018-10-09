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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.jline.reader.LineReader;
import org.jline.reader.impl.DefaultHighlighter;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.WCWidth;

/**
 * Highlighter class to implement logic of sql
 * and command syntax highlighting in sqlline.
 */
public class SqlLineHighlighter extends DefaultHighlighter {
  private static final String DEFAULT_SQL_IDENTIFIER_QUOTE = "\"";
  private final SqlLine sqlLine;
  private final Set<String> defaultSqlKeyWordsSet;
  private final Map<Connection, HighlightRule> connection2rules =
      new HashMap<>();

  public SqlLineHighlighter(SqlLine sqlLine) throws IOException {
    this.sqlLine = sqlLine;
    String keywords =
        new BufferedReader(
            new InputStreamReader(
                SqlCompleter.class.getResourceAsStream(
                    "sql-keywords.properties"), StandardCharsets.UTF_8)
        ).readLine();
    defaultSqlKeyWordsSet = new TreeSet<>();
    for (StringTokenizer tok = new StringTokenizer(keywords, ",");
         tok.hasMoreTokens();) {
      defaultSqlKeyWordsSet.add(tok.nextToken());
    }
  }

  @Override
  public AttributedString highlight(
      LineReader reader, String buffer) {
    boolean skipSyntaxHighlighter =
        SqlLineOpts.DEFAULT.equals(sqlLine.getOpts().getColorScheme());
    if (skipSyntaxHighlighter) {
      return super.highlight(reader, buffer);
    }

    int underlineStart = -1;
    int underlineEnd = -1;
    int negativeStart = -1;
    int negativeEnd = -1;
    boolean command = false;
    final BitSet sqlKeyWordsBitSet = new BitSet(buffer.length());
    final BitSet quoteBitSet = new BitSet(buffer.length());
    final BitSet sqlIdentifierQuotesBitSet = new BitSet(buffer.length());
    final BitSet commentBitSet = new BitSet(buffer.length());
    final BitSet numberBitSet = new BitSet(buffer.length());
    final String trimmed = buffer.trim();
    boolean isCommandPresent = trimmed.startsWith(SqlLine.COMMAND_PREFIX);
    final boolean isSql = isSqlQuery(trimmed, isCommandPresent);

    String possibleCommand;
    if (trimmed.length() > 1 && isCommandPresent) {
      int end = trimmed.indexOf(' ');
      possibleCommand = end == -1
          ? trimmed.substring(1) : trimmed.substring(1, end);
      for (CommandHandler ch : sqlLine.getCommandHandlers()) {
        if (Objects.equals(possibleCommand, ch.getName())
            || ch.getNames().contains(possibleCommand)) {
          command = true;
          break;
        }
      }
    }
    if (isSql) {
      handleSqlSyntax(
          buffer,
          sqlKeyWordsBitSet,
          quoteBitSet,
          sqlIdentifierQuotesBitSet,
          commentBitSet,
          numberBitSet,
          isCommandPresent);
    } else {
      handleQuotesInCommands(buffer, quoteBitSet, sqlIdentifierQuotesBitSet);
    }

    String search = reader.getSearchTerm();
    if (search != null && search.length() > 0) {
      underlineStart = buffer.indexOf(search);
      if (underlineStart >= 0) {
        underlineEnd = underlineStart + search.length() - 1;
      }
    }
    if (reader.getRegionActive() != LineReader.RegionType.NONE) {
      negativeStart = reader.getRegionMark();
      negativeEnd = reader.getBuffer().cursor();
      if (negativeStart > negativeEnd) {
        int x = negativeEnd;
        negativeEnd = negativeStart;
        negativeStart = x;
      }
      if (reader.getRegionActive() == LineReader.RegionType.LINE) {
        while (negativeStart > 0
            && reader.getBuffer().atChar(negativeStart - 1) != '\n') {
          negativeStart--;
        }
        while (negativeEnd < reader.getBuffer().length() - 1
            && reader.getBuffer().atChar(negativeEnd + 1) != '\n') {
          negativeEnd++;
        }
      }
    }

    AttributedStringBuilder sb = new AttributedStringBuilder();
    final int commandStart = command
        ? buffer.indexOf(SqlLine.COMMAND_PREFIX) : -1;
    final int commandEnd = command
        ? buffer.indexOf(' ', commandStart) : -1;


    final HighlightStyle highlightStyle = sqlLine.getHighlightStyle();
    for (int i = 0; i < buffer.length(); i++) {
      if (isSql) {
        if (sqlKeyWordsBitSet.get(i)) {
          sb.style(highlightStyle.getSqlKeywordStyle());
        } else if (quoteBitSet.get(i)) {
          sb.style(highlightStyle.getQuotedStyle());
        } else if (sqlIdentifierQuotesBitSet.get(i)) {
          sb.style(highlightStyle.getSqlIdentifierStyle());
        } else if (commentBitSet.get(i)) {
          sb.style(highlightStyle.getCommentedStyle());
        } else if (numberBitSet.get(i)) {
          sb.style(highlightStyle.getNumbersStyle());
        } else if (i == 0 || (i > commandEnd
            && (i < underlineStart || i > underlineEnd)
            && (i < negativeStart || i > negativeEnd))) {

          sb.style(highlightStyle.getDefaultStyle());
        }
      } else {
        if (quoteBitSet != null && quoteBitSet.get(i)) {
          sb.style(highlightStyle.getQuotedStyle());
        } else if (sqlIdentifierQuotesBitSet != null
            && sqlIdentifierQuotesBitSet.get(i)) {
          sb.style(highlightStyle.getSqlIdentifierStyle());
        }
      }

      if (i == commandStart && command) {
        sb.style(highlightStyle.getCommandStyle());
      }
      if (i == commandEnd) {
        sb.style(highlightStyle.getDefaultStyle());
      }
      if (i >= underlineStart && i <= underlineEnd) {
        sb.style(sb.style().underline());
      }
      if (i >= negativeStart && i <= negativeEnd) {
        sb.style(sb.style().inverse());
      }
      char c = buffer.charAt(i);
      if (c == '\t' || c == '\n') {
        sb.append(c);
      } else if (c < 32) {
        sb.style(AttributedStyle::inverseNeg)
            .append('^')
            .append((char) (c + '@'))
            .style(AttributedStyle::inverseNeg);
      } else {
        int w = WCWidth.wcwidth(c);
        if (w > 0) {
          sb.append(c);
        }
      }
      if (i == underlineEnd) {
        sb.style(sb.style().underlineOff());
      }
      if (i == negativeEnd) {
        sb.style(sb.style().inverseOff());
      }
    }
    return sb.toAttributedString();
  }

  private HighlightRule getConnectionSpecificRule() {
    try {
      DatabaseConnection databaseConnection = sqlLine.getDatabaseConnection();
      if (databaseConnection != null
          && databaseConnection.connection != null
          && !databaseConnection.connection.isClosed()) {
        if (connection2rules.get(databaseConnection.connection) != null) {
          return connection2rules.get(databaseConnection.connection);
        }
        DatabaseMetaData meta = databaseConnection.meta;
        Set<String> connectionSQLKeyWords =
            new HashSet<>(
                Arrays.asList(meta.getSQLKeywords().split(",")));
        String sqlIdentifier = meta.getIdentifierQuoteString();
        sqlIdentifier = " ".equals(sqlIdentifier)
            ? DEFAULT_SQL_IDENTIFIER_QUOTE : sqlIdentifier;
        HighlightRule rule =
            new HighlightRule(connectionSQLKeyWords, sqlIdentifier);
        connection2rules.put(databaseConnection.connection, rule);
        return rule;
      } else if (databaseConnection != null) {
        connection2rules.remove(databaseConnection.connection);
      }
    } catch (SQLException sqle) {
      sqlLine.handleException(sqle);
    }
    return null;
  }

  private void handleSqlSyntax(
      String buffer,
      BitSet sqlKeyWordsBitSet,
      BitSet quoteBitSet,
      BitSet sqlIdentifierQuotesBitSet,
      BitSet commentBitSet,
      BitSet numberBitSet,
      boolean isCommandPresent) {
    int wordStart = -1;
    int start = 0;
    if (isCommandPresent) {
      start = buffer.indexOf(
          SqlLine.COMMAND_PREFIX) + SqlLine.COMMAND_PREFIX.length();
      int nextSpace =
          buffer.indexOf(' ', buffer.indexOf(SqlLine.COMMAND_PREFIX));
      start = nextSpace == -1 ? buffer.length() : start + nextSpace;
    }

    final HighlightRule highlightRule = getConnectionSpecificRule();
    final Set<String> connectionSpecificSqlKeyWords =
        highlightRule == null
            ? null : highlightRule.connectionBasedSqlKeyWordsSet;
    final String sqlIdentifier = highlightRule == null
        ? DEFAULT_SQL_IDENTIFIER_QUOTE : highlightRule.sqlIdentifierQuote;
    for (int pos = start; pos < buffer.length(); pos++) {
      char ch = buffer.charAt(pos);
      if (wordStart > -1) {
        if (pos == buffer.length() - 1
            || (!Character.isLetterOrDigit(ch) && ch != '_')) {
          String word = !Character.isLetterOrDigit(ch)
              ? buffer.substring(wordStart, pos)
              : buffer.substring(wordStart);
          String upperWord = word.toUpperCase(Locale.ROOT);
          if (defaultSqlKeyWordsSet.contains(upperWord)
              || (connectionSpecificSqlKeyWords != null
              && connectionSpecificSqlKeyWords.contains(upperWord))) {
            sqlKeyWordsBitSet.set(wordStart, wordStart + word.length());
          }
          wordStart = -1;
        } else {
          continue;
        }
      }
      if (ch == sqlIdentifier.charAt(0) && (sqlIdentifier.length() == 1
          || sqlIdentifier
          .regionMatches(0, buffer, pos, sqlIdentifier.length()))) {
        pos = handleSqlIdentifierQuotes(
            buffer, sqlIdentifier, sqlIdentifierQuotesBitSet, pos);
      }
      if (ch == '\'') {
        pos = handleSqlSingleQuotes(buffer, quoteBitSet, pos);
      }
      if (pos < buffer.length() - 1) {
        pos = handleComments(buffer, commentBitSet, pos);
      }
      if (wordStart == -1
          && (Character.isLetter(ch)
          || ch == '@' || ch == '#' || ch == '_')
          && (pos == 0 || buffer.charAt(pos - 1) != '.')) {
        wordStart = pos;
        continue;
      }
      if (wordStart == -1 && Character.isDigit(ch)
          && (pos == 0
          || (!Character.isLetterOrDigit(buffer.charAt(pos - 1))
          && buffer.charAt(pos - 1) != '_'))) {
        pos = handleNumbers(buffer, numberBitSet, pos);
        continue;
      }
    }
  }

  /**
   * The method marks single/double quoted string position
   * in sqlline command based on input.
   * ASSUMPTION: the input is sqlline command but not sql itself.
   *
   * @param line              line with sqlline command where to handle
   *                          single/double quoted string
   * @param quoteBitSet       BitSet to use for positions of single quoted lines
   * @param doubleQuoteBitSet BitSet to use for positions of double quoted lines
   * <p>
   * Example
   * String line = "!set csvDelimiter '"'";
   * <p>
   * handleQuotesInCommands(line, quoteBitSet, doubleQuoteBitSet);
   * should mark single quoted string only as
   * a double quote is inside the quoted line.
   */
  void handleQuotesInCommands(
      String line, BitSet quoteBitSet, BitSet doubleQuoteBitSet) {
    int doubleQuoteStart = -1;
    int quoteStart = -1;
    for (int pos = 0; pos < line.length(); pos++) {
      char ch = line.charAt(pos);
      if (doubleQuoteStart > -1) {
        doubleQuoteBitSet.set(pos);
        if (ch == '"') {
          doubleQuoteStart = -1;
        }
        continue;
      } else if (quoteStart > -1) {
        quoteBitSet.set(pos);
        if (ch == '\'') {
          quoteStart = -1;
        }
        continue;
      }
      // so far doubleQuoteBitSet MUST BE -1 and quoteStart MUST BE -1
      if (ch == '"') {
        doubleQuoteBitSet.set(pos);
        doubleQuoteStart = pos;
      }

      // so far quoteStart MUST BE -1
      if (doubleQuoteStart == -1 && ch == '\'') {
        quoteBitSet.set(pos);
        quoteStart = pos;
      }
    }
  }

  private boolean isSqlQuery(String trimmed, boolean isCommandPresent) {
    return !isCommandPresent
        || trimmed.startsWith("!all")
        || trimmed.startsWith("!call")
        || trimmed.startsWith("!sql");
  }

  /**
   * The method marks quoted (with {@code sqlIdentifier}) string
   * position based on input.
   * ASSUMPTION: there is a {@code sqlIdentifier} quote starts since
   * the specified position,
   * i.e. sqlIdentifier.charAt(0) == line.charAt(startingPoint)
   *
   * @param line                        line where to handle
   *                                    sql identifier quoted string
   * @param sqlIdentifier               Quote to use to
   *                                    quote sql identifiers
   * @param sqlIdentifierQuotesBitSet   BitSet to use for positions
   *                                    of sql identifiers quoted lines
   * @param startingPoint               start point
   * @return position of closing (non-escaped) sql identifier quote.
   * <p>
   * Example (for the case sql identifier quote is a double quote ("))
   * "Quoted \" line quoted" "another\"'' quoted''\" line"
   * <p>
   * handleSqlIdentifierQuotes(
   * line, sqlIdentifier, sqlIdentifierQuotesBitSet, startingPoint);
   * should return the position of the sql identifier quote closing
   * the first quoted string and mark sql identifier quoted line
   * inside sqlIdentifierQuotesBitSet.
   */
  int handleSqlIdentifierQuotes(String line,
                                String sqlIdentifier,
                                BitSet sqlIdentifierQuotesBitSet,
                                int startingPoint) {
    if (!sqlIdentifier.regionMatches(
        0, line, startingPoint, sqlIdentifier.length())) {
      return startingPoint;
    }
    int backslashCounter = 0;
    for (int i = startingPoint + sqlIdentifier.length();
         i < line.length();
         i++) {
      if (line.charAt(i) == '\\') {
        backslashCounter++;
      } else if (
          sqlIdentifier.regionMatches(0, line, i, sqlIdentifier.length())) {
        if (backslashCounter % 2 == 0) {
          sqlIdentifierQuotesBitSet
              .set(startingPoint, i + sqlIdentifier.length());
          return i + sqlIdentifier.length() - 1;
        }
      } else {
        backslashCounter = 0;
      }
    }
    sqlIdentifierQuotesBitSet.set(startingPoint, line.length());
    return line.length() - 1;
  }

  /**
   * The method marks numbers position based on input.
   * ASSUMPTION: there is a number starts since
   * the specified position,
   * i.e. Character.isDigit(line.charAt(startingPoint)) == true
   *
   * @param line           line where to handle number string
   * @param numberBitSet   BitSet to use for position of number
   * @param startingPoint  start point
   * @return position where number finished.
   */
  int handleNumbers(String line, BitSet numberBitSet, int startingPoint) {
    int end = startingPoint + 1;
    while (end < line.length() && Character.isDigit(line.charAt(end))) {
      end++;
    }
    if (end == line.length()) {
      if (Character.isDigit(line.charAt(line.length() - 1))) {
        numberBitSet.set(startingPoint, end);
      }
    } else if (Character.isWhitespace(line.charAt(end))
        || line.charAt(end) == ';'
        || line.charAt(end) == ','
        || line.charAt(end) == '='
        || line.charAt(end) == '<'
        || line.charAt(end) == '>'
        || line.charAt(end) == '-'
        || line.charAt(end) == '+'
        || line.charAt(end) == '/'
        || line.charAt(end) == ')'
        || line.charAt(end) == '%'
        || line.charAt(end) == '*') {
      numberBitSet.set(startingPoint, end);
    }
    startingPoint = end - 1;
    return startingPoint;
  }

  /**
   * The method marks commented string position based on input.
   * ASSUMPTION: there is a comment start since the specified position,
   * i.e. line.charAt(startingPoint) == '-'
   * && line.charAt(startingPoint + 1) == '-'
   * or
   * i.e. line.charAt(startingPoint) == '/'
   * && line.charAt(startingPoint + 1) == '*'
   *
   * @param line          line where to handle commented string
   * @param commentBitSet BitSet to use for positions of single quoted lines
   * @param startingPoint start point
   * @return position of closing comment (multiline comment)
   * or a new line position (one line comment).
   * <p>
   * Example
   * String line = "test /*Single --'' line commented'*\/
   * '/*another-- commented'' line*\/";
   * <p>
   * handleComments(line, commentBitSet, line.indexOf("/*Single"));
   * should return position of first closing comment *\/
   * and mark comment position inside commentBitSet.
   */
  int handleComments(
      String line, BitSet commentBitSet, int startingPoint) {
    final char ch = line.charAt(startingPoint);
    if (ch == '-' && line.charAt(startingPoint + 1) == '-') {
      int end = line.indexOf('\n', startingPoint);
      end = end == -1 ? line.length() - 1 : end;
      commentBitSet.set(startingPoint, end + 1);
      startingPoint = end;
    } else if (ch == '/' && line.charAt(startingPoint + 1) == '*') {
      int end = line.indexOf("*/", startingPoint);
      end = end == -1 ? line.length() - 1 : end + 1;
      commentBitSet.set(startingPoint, end + 1);
      startingPoint = end;
    }
    return startingPoint;
  }

  /**
   * The method marks single quoted string position based on input.
   * ASSUMPTION: there is a single quote in the specified position,
   * i.e. line.charAt(startingPoint) == '\''
   *
   * @param line          line where to handle single quoted string
   * @param quoteBitSet   BitSet to use for positions of single quoted lines
   * @param startingPoint start point
   * @return position of closing (non-escaped) quote (').
   * <p>
   * Example
   * String line = "test 'Single '' line quoted''' 'another'' quoted'' line'";
   * <p>
   * handleSqlSingleQuotes(line, quoteBitSet, line.indexOf("'Single"));
   * should return position of the quote (') closing the first quoted string
   * and mark single quoted line inside quoteBitSet.
   */
  int handleSqlSingleQuotes(
      String line, BitSet quoteBitSet, int startingPoint) {
    int end;
    int quoteCounter = 1;
    boolean quotationEnded = false;
    do {
      end = line.indexOf('\'', startingPoint + 1);
      if (end > -1) {
        quoteCounter++;
      }
      if (end == -1 || end == line.length() - 1) {
        quoteBitSet.set(startingPoint, line.length());
        quotationEnded = true;
      } else if (line.charAt(end + 1) != '\'' && quoteCounter % 2 == 0) {
        quotationEnded = true;
      }
      end = end == -1 ? line.length() - 1 : end;
      quoteBitSet.set(startingPoint, end + 1);
      startingPoint = end;
    } while (!quotationEnded && end < line.length());
    return startingPoint;
  }

  /**
   * Check if the connection is present in connection2rules map.
   * The only purpose of this method is testing.
   * @param connection connection to check
   * @return if connection is present or no
   */
  boolean checkIfConnectionPresent(Connection connection) {
    return connection2rules.containsKey(connection);
  }

  public void removeConnection(Connection connection) {
    connection2rules.remove(connection);
  }

  /**
   * Class to provide rules for highlighting.
   * Currently it provides an additional set of keywords
   * and sql identifier quote to use.
   */
  private class HighlightRule {
    private final Set<String> connectionBasedSqlKeyWordsSet;
    private final String sqlIdentifierQuote;

    HighlightRule(
        Set<String> connectionBasedSqlKeyWordsSet, String sqlIdentifierQuote) {
      this.connectionBasedSqlKeyWordsSet = connectionBasedSqlKeyWordsSet;
      this.sqlIdentifierQuote = sqlIdentifierQuote;
    }
  }
}

// End SqlLineHighlighter.java
