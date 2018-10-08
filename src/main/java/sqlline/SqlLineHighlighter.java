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
import java.util.BitSet;
import java.util.Locale;
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
  private final SqlLine sqlLine;
  private final Set<String> sqlKeyWords;

  public SqlLineHighlighter(SqlLine sqlLine) throws IOException {
    this.sqlLine = sqlLine;
    String keywords =
        new BufferedReader(
            new InputStreamReader(
                SqlCompleter.class.getResourceAsStream(
                    "sql-keywords.properties"), StandardCharsets.UTF_8)
        ).readLine();
    sqlKeyWords = new TreeSet<>();
    for (StringTokenizer tok = new StringTokenizer(keywords, ",");
         tok.hasMoreTokens();) {
      sqlKeyWords.add(tok.nextToken());
    }
  }

  @Override public AttributedString highlight(
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
    final BitSet doubleQuoteBitSet = new BitSet(buffer.length());
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
          doubleQuoteBitSet,
          commentBitSet,
          numberBitSet,
          isCommandPresent);
    } else {
      handleQuotesInCommands(buffer, quoteBitSet, doubleQuoteBitSet);
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
        } else if (doubleQuoteBitSet.get(i)) {
          sb.style(highlightStyle.getDoubleQuotedStyle());
        } else if (commentBitSet.get(i)) {
          sb.style(highlightStyle.getCommentedStyle());
        } else if (numberBitSet.get(i)) {
          sb.style(highlightStyle.getNumbersStyle());
        } else if (i > commandEnd
            && (i < underlineStart || i > underlineEnd)
            && (i < negativeStart || i > negativeEnd)) {
          sb.style(highlightStyle.getDefaultStyle());
        }
      } else {
        if (quoteBitSet != null && quoteBitSet.get(i)) {
          sb.style(highlightStyle.getQuotedStyle());
        } else if (doubleQuoteBitSet != null && doubleQuoteBitSet.get(i)) {
          sb.style(highlightStyle.getDoubleQuotedStyle());
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

  private void handleSqlSyntax(
      String buffer,
      BitSet sqlKeyWordsBitSet,
      BitSet quoteBitSet,
      BitSet doubleQuoteBitSet,
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

    for (int pos = start; pos < buffer.length(); pos++) {
      char ch = buffer.charAt(pos);
      if (wordStart > -1) {
        if (pos == buffer.length() - 1
            || (!Character.isLetterOrDigit(ch) && ch != '_')) {
          String word = !Character.isLetterOrDigit(ch)
              ? buffer.substring(wordStart, pos)
              : buffer.substring(wordStart);
          if (sqlKeyWords.contains(word.toUpperCase(Locale.ROOT))) {
            sqlKeyWordsBitSet.set(wordStart, wordStart + word.length());
          }
          wordStart = -1;
        } else {
          continue;
        }
      }
      if (ch == '"') {
        pos = handleDoubleQuotes(buffer, doubleQuoteBitSet, pos);
      }
      if (ch == '\'') {
        pos = handleSqlSingleQuotes(buffer, quoteBitSet, pos);
      }
      if (pos < buffer.length() - 1) {
        pos = handleComments(buffer, commentBitSet, pos, ch);
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

  private void handleQuotesInCommands(
      String buffer, BitSet quoteBitSet, BitSet doubleQuoteBitSet) {
    int doubleQuoteStart = -1;
    int quoteStart = -1;
    for (int pos = 0; pos < buffer.length(); pos++) {
      char ch = buffer.charAt(pos);
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
      // so far doubleQuoteStart MUST BE -1 and quoteStart MUST BE -1
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
    return trimmed.startsWith("!all")
        || trimmed.startsWith("!call")
        || trimmed.startsWith("!sql")
        || !isCommandPresent;
  }

  private int handleDoubleQuotes(
      String buffer, BitSet doubleQuoteBitSet, int pos) {
    int end = buffer.indexOf('"', pos + 1);
    end = end == -1 ? buffer.length() - 1 : end;
    doubleQuoteBitSet.set(pos, end + 1);
    pos = end;
    return pos;
  }

  private int handleNumbers(String buffer, BitSet numberBitSet, int pos) {
    int end = pos + 1;
    while (end < buffer.length() && Character.isDigit(buffer.charAt(end))) {
      end++;
    }
    if (end == buffer.length()) {
      if (Character.isDigit(buffer.charAt(buffer.length() - 1))) {
        numberBitSet.set(pos, end);
      }
    } else if (Character.isWhitespace(buffer.charAt(end))
        || buffer.charAt(end) == ';'
        || buffer.charAt(end) == ','
        || buffer.charAt(end) == '='
        || buffer.charAt(end) == '<'
        || buffer.charAt(end) == '>'
        || buffer.charAt(end) == '-'
        || buffer.charAt(end) == '+'
        || buffer.charAt(end) == '/'
        || buffer.charAt(end) == ')'
        || buffer.charAt(end) == '%'
        || buffer.charAt(end) == '*') {
      numberBitSet.set(pos, end);
    }
    pos = end - 1;
    return pos;
  }

  private int handleComments(
      String buffer, BitSet commentBitSet, int pos, char ch) {
    if (ch == '-' && buffer.charAt(pos + 1) == '-') {
      int end = buffer.indexOf('\n', pos);
      end = end == -1 ? buffer.length() : end;
      commentBitSet.set(pos, end + 1);
      pos = end;
    } else if (ch == '/' && buffer.charAt(pos + 1) == '*') {
      int end = buffer.indexOf("*/", pos);
      end = end == -1 ? buffer.length() : end + 1;
      commentBitSet.set(pos, end + 1);
      pos = end;
    }
    return pos;
  }

  private int handleSqlSingleQuotes(
      String buffer, BitSet quoteBitSet, int pos) {
    int end;
    int quoteCounter = 1;
    boolean quotationEnded = false;
    do {
      end = buffer.indexOf('\'', pos + 1);
      if (end > -1) {
        quoteCounter++;
      }
      if (end == -1 || end == buffer.length() - 1) {
        quoteBitSet.set(pos, buffer.length());
        quotationEnded = true;
      } else if (buffer.charAt(end + 1) != '\'' && quoteCounter % 2 == 0) {
        quotationEnded = true;
      }
      end = end == -1 ? buffer.length() : end;
      quoteBitSet.set(pos, end + 1);
      pos = end;
    } while (!quotationEnded && end < buffer.length());
    return pos;
  }

}

// End SqlLineHighlighter.java
