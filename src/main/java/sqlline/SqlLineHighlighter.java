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
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

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
  private final SqlLine sqlLine;

  public SqlLineHighlighter(SqlLine sqlLine) {
    this.sqlLine = sqlLine;
  }

  @Override public AttributedString highlight(LineReader reader,
      String buffer) {
    try {
      boolean skipSyntaxHighlighter =
          SqlLineProperty.DEFAULT.equals(sqlLine.getOpts().getColorScheme());
      if (skipSyntaxHighlighter) {
        return super.highlight(reader, buffer);
      }
      int underlineStart = -1;
      int underlineEnd = -1;
      int negativeStart = -1;
      int negativeEnd = -1;
      boolean command = false;
      final BitSet keywordBitSet = new BitSet(buffer.length());
      final BitSet quoteBitSet = new BitSet(buffer.length());
      final BitSet sqlIdentifierQuotesBitSet = new BitSet(buffer.length());
      final BitSet commentBitSet = new BitSet(buffer.length());
      final BitSet numberBitSet = new BitSet(buffer.length());
      final String trimmed = buffer.trim();
      final int startingPoint = getStartingPoint(buffer);
      final boolean isCommandPresent =
          trimmed.startsWith(SqlLine.COMMAND_PREFIX);
      final boolean isComment =
          !isCommandPresent && sqlLine.isOneLineComment(trimmed, false);
      final boolean isSql = !isComment
          && isSqlQuery(trimmed, isCommandPresent);

      if (trimmed.length() > 1 && isCommandPresent) {
        final int end = trimmed.indexOf(' ');
        final String possibleCommand =
            end == -1
                ? trimmed.substring(1)
                : trimmed.substring(1, end);
        for (CommandHandler ch : sqlLine.getCommandHandlers()) {
          if (Objects.equals(possibleCommand, ch.getName())
              || ch.getNames().contains(possibleCommand)) {
            command = true;
            break;
          }
        }
      }
      if (isSql) {
        handleSqlSyntax(buffer, keywordBitSet, quoteBitSet,
            sqlIdentifierQuotesBitSet, commentBitSet, numberBitSet,
            isCommandPresent);
      } else if (isCommandPresent) {
        handleQuotesInCommands(buffer, quoteBitSet, sqlIdentifierQuotesBitSet);
      } else {
        handleComments(buffer, commentBitSet, startingPoint, false);
      }

      final String search = reader.getSearchTerm();
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

      final AttributedStringBuilder sb = new AttributedStringBuilder();
      final int commandStart = command
          ? buffer.indexOf(SqlLine.COMMAND_PREFIX) : -1;
      final int commandEnd = command
          ? commandStart > -1 && buffer.indexOf(' ', commandStart) == -1
              ? buffer.length()
              : buffer.indexOf(' ', commandStart)
          : -1;

      final HighlightStyle highlightStyle = sqlLine.getHighlightStyle();
      for (int i = 0; i < buffer.length(); i++) {
        if (i < startingPoint) {
          sb.style(highlightStyle.getDefaultStyle());
        } else {
          final boolean defaultStyleStart =
              (i == 0 && commandEnd == -1 && commandStart == -1)
                  || (i > Math.max(commandEnd, commandStart)
                      && (i < underlineStart || i > underlineEnd)
                      && (i < negativeStart || i > negativeEnd));
          if (isSql) {
            if (keywordBitSet.get(i)) {
              sb.style(highlightStyle.getKeywordStyle());
            } else if (quoteBitSet.get(i)) {
              sb.style(highlightStyle.getQuotedStyle());
            } else if (sqlIdentifierQuotesBitSet.get(i)) {
              sb.style(highlightStyle.getIdentifierStyle());
            } else if (commentBitSet.get(i)) {
              sb.style(highlightStyle.getCommentStyle());
            } else if (numberBitSet.get(i)) {
              sb.style(highlightStyle.getNumberStyle());
            } else if (defaultStyleStart) {
              sb.style(highlightStyle.getDefaultStyle());
            }
          } else {
            if (quoteBitSet.get(i)) {
              sb.style(highlightStyle.getQuotedStyle());
            } else if (sqlIdentifierQuotesBitSet.get(i)) {
              sb.style(highlightStyle.getIdentifierStyle());
            } else if (commentBitSet.get(i)) {
              sb.style(highlightStyle.getCommentStyle());
            } else if (defaultStyleStart) {
              sb.style(highlightStyle.getDefaultStyle());
            }
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
    } catch (Exception e) {
      sqlLine.handleException(e);
      AttributedStringBuilder sb = new AttributedStringBuilder();
      return sb.append(buffer).toAttributedString();
    }
  }

  /**
   * Returns the index of the first non-whitespace character.
   *
   * @param buffer Input string
   * @return index of the first non-whitespace character
   */
  private int getStartingPoint(String buffer) {
    for (int i = 0; i < buffer.length(); i++) {
      if (!Character.isWhitespace(buffer.charAt(i))) {
        return i;
      }
    }
    return buffer.length();
  }

  void handleSqlSyntax(String buffer,
      BitSet keywordBitSet,
      BitSet quoteBitSet,
      BitSet sqlIdentifierQuotesBitSet,
      BitSet commentBitSet,
      BitSet numberBitSet,
      boolean isCommandPresent) {
    int wordStart = -1;
    int start = 0;
    if (isCommandPresent) {
      start = buffer.indexOf(SqlLine.COMMAND_PREFIX)
          + SqlLine.COMMAND_PREFIX.length();
      while (start < buffer.length()
          && !Character.isWhitespace(buffer.charAt(start))) {
        start++;
      }
    }

    final Dialect dialect = sqlLine.getDialect();
    for (int pos = start; pos < buffer.length(); pos++) {
      char ch = buffer.charAt(pos);
      if (wordStart > -1) {
        if (pos == buffer.length() - 1
            || (!Character.isLetterOrDigit(ch) && ch != '_')) {
          String word = !Character.isLetterOrDigit(ch)
              ? buffer.substring(wordStart, pos)
              : buffer.substring(wordStart);
          String upperWord = word.toUpperCase(Locale.ROOT);
          if (dialect.containsKeyword(upperWord)) {
            keywordBitSet.set(wordStart, wordStart + word.length());
          }
          wordStart = -1;
        } else {
          continue;
        }
      }
      if (ch == dialect.getOpenQuote()) {
        pos = handleSqlIdentifierQuotes(buffer,
            String.valueOf(dialect.getOpenQuote()),
            String.valueOf(dialect.getCloseQuote()),
            sqlIdentifierQuotesBitSet, pos);
      }
      if (ch == '\'') {
        pos = handleSqlSingleQuotes(buffer, quoteBitSet, pos);
      }
      if (pos <= buffer.length() - 1) {
        pos = handleComments(buffer, commentBitSet, pos, true);
      }
      if (wordStart == -1
          && (Character.isLetter(ch) || ch == '@' || ch == '#' || ch == '_')
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
   * Marks single/double quoted string position
   * in sqlline command based on input.
   *
   * <p>Assumes that the input is a sqlline command but not SQL itself.
   *
   * @param line              line with sqlline command where to handle
   *                          single/double quoted string
   * @param quoteBitSet       BitSet to use for positions of single-quoted lines
   * @param doubleQuoteBitSet BitSet to use for positions of double-quoted lines
   *
   * <p>For example,
   *
   * <blockquote><code>handleQuotesInCommands("!set csvDelimiter '"'",
   * quoteBitSet, doubleQuoteBitSet);</code></blockquote>
   *
   * <p>should mark a single-quoted string only. as a double-quote is
   * inside the quoted line.
   */
  void handleQuotesInCommands(String line, BitSet quoteBitSet,
      BitSet doubleQuoteBitSet) {
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
   * @param openSqlIdentifier           Start quote for SQL identifiers
   * @param closeSqlIdentifier          End quote for SQL identifiers
   * @param sqlIdentifierQuotesBitSet   BitSet to use for positions
   *                                    of sql identifiers quoted lines
   * @param startingPoint               start point
   * @return position of closing (non-escaped) sql identifier quote.
   *
   * <p>Example (for the case sql identifier quote is a double quote ("))
   * "Quoted \" line quoted" "another\"'' quoted''\" line"
   *
   * <blockquote><code>handleSqlIdentifierQuotes(line, sqlIdentifier,
   * sqlIdentifierQuotesBitSet, startingPoint);</code></blockquote>
   *
   * <p>should return the position of the sql identifier quote closing the first
   * quoted string and mark sql identifier quoted line inside
   * {@code sqlIdentifierQuotesBitSet}.
   */
  int handleSqlIdentifierQuotes(String line, String openSqlIdentifier,
      String closeSqlIdentifier, BitSet sqlIdentifierQuotesBitSet,
      int startingPoint) {
    if (!openSqlIdentifier.regionMatches(0, line, startingPoint,
        openSqlIdentifier.length())) {
      return startingPoint;
    }
    int backslashCounter = 0;
    for (int i = startingPoint + openSqlIdentifier.length();
         i < line.length();
         i++) {
      if (line.charAt(i) == '\\') {
        backslashCounter++;
      } else if (closeSqlIdentifier.regionMatches(0, line, i,
          closeSqlIdentifier.length())) {
        if (backslashCounter % 2 == 0) {
          sqlIdentifierQuotesBitSet
              .set(startingPoint, i + closeSqlIdentifier.length());
          return i + closeSqlIdentifier.length() - 1;
        }
      } else {
        backslashCounter = 0;
      }
    }
    sqlIdentifierQuotesBitSet.set(startingPoint, line.length());
    return line.length() - 1;
  }

  /**
   * Marks numbers position based on input.
   *
   * <p>Assumes that a number starts at the specified position,
   * i.e. {@code Character.isDigit(line.charAt(startingPoint)) == true}
   *
   * @param line           Line where to handle number string
   * @param numberBitSet   BitSet to use for position of number
   * @param startingPoint  Start point
   *
   * @return position where number finished
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
        || line.charAt(end) == '*'
        || line.charAt(end) == '!'
        || line.charAt(end) == '^'
        || line.charAt(end) == '|'
        || line.charAt(end) == '&'
        || line.charAt(end) == ']') {
      numberBitSet.set(startingPoint, end);
    }
    startingPoint = end - 1;
    return startingPoint;
  }

  /**
   * Marks commented string position based on input.
   *
   * <p>Assumes that there is a comment start since the specified position,
   * i.e. <code>line.charAt(startingPoint) == '-'
   * && line.charAt(startingPoint + 1) == '-'</code>
   * or
   * i.e. <code>line.charAt(startingPoint) == '/'
   * && line.charAt(startingPoint + 1) == '*'</code>
   *
   * @param line          Line where to handle commented string
   * @param commentBitSet BitSet to use for positions of single quoted lines
   * @param startingPoint Start point
   * @param isSql         Is the line sql
   *
   * @return position of closing comment (multi-line comment)
   * or a new line position (one line comment)
   *
   * <p>Example
   * String line = "test /*Single --'' line commented'*\/
   * '/*another-- commented'' line*\/";
   *
   * <p><code>handleComments(line, commentBitSet,
   * line.indexOf("/*Single"));</code>
   * should return the position of the first closing comment *\/
   * and mark comment position inside {@code commentBitSet}.
   */
  int handleComments(String line, BitSet commentBitSet,
      int startingPoint, boolean isSql) {
    Set<String> oneLineComments = isSql
        ? sqlLine.getDialect().getOneLineComments()
        : sqlLine.getDialect().getSqlLineOneLineComments();
    final char ch = line.charAt(startingPoint);
    if (startingPoint + 1 < line.length()
        && ch == '/'
        && line.charAt(startingPoint + 1) == '*') {
      int end = startingPoint + 2 < line.length()
          ? line.indexOf("*/", startingPoint + 2) : -1;
      end = end == -1 ? line.length() - 1 : end + 1;
      commentBitSet.set(startingPoint, end + 1);
      startingPoint = end;
    } else {
      for (String oneLineComment : oneLineComments) {
        if (startingPoint <= line.length() - oneLineComment.length()
            && oneLineComment
            .regionMatches(0, line, startingPoint, oneLineComment.length())) {
          int end = line.indexOf('\n', startingPoint);
          end = end == -1 ? line.length() - 1 : end;
          commentBitSet.set(startingPoint, end + 1);
          startingPoint = end;
        }
      }
    }
    return startingPoint;
  }

  /**
   * Marks single-quoted string position based on input.
   *
   * <p>Assumes that there is a single quote in the specified position,
   * i.e. {@code line.charAt(startingPoint) == '\''}.
   *
   * @param line          Line where to handle single-quoted string
   * @param quoteBitSet   BitSet to use for positions of single quoted lines
   * @param startingPoint Start point
   *
   * @return position of closing (non-escaped) quote (')
   *
   * <p>Example
   * String line = "test 'Single '' line quoted''' 'another'' quoted'' line'";
   *
   * <p><code>handleSqlSingleQuotes(line, quoteBitSet,
   * line.indexOf("'Single"));</code>
   * should return position of the quote (') closing the first quoted string
   * and mark single quoted line inside {@code quoteBitSet}.
   */
  int handleSqlSingleQuotes(String line, BitSet quoteBitSet,
      int startingPoint) {
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
}

// End SqlLineHighlighter.java
