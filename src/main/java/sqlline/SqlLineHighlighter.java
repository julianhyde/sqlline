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
  private Set<String> sqlKeyWords;

  public SqlLineHighlighter(SqlLine sqlLine) {
    this.sqlLine = sqlLine;
    try {
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
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public AttributedString highlight(LineReader reader, String buffer) {
    int underlineStart = -1;
    int underlineEnd = -1;
    int negativeStart = -1;
    int negativeEnd = -1;
    boolean command = false;
    boolean isSql = false;
    BitSet sqlKeyWordsBitSet = null;
    BitSet quoteBitSet = new BitSet(buffer.length());
    BitSet doubleQuoteBitSet = new BitSet(buffer.length());
    BitSet commentBitSet = null;
    BitSet numberBitSet = new BitSet(buffer.length());
    String trimmed = buffer.trim();
    boolean isCommandPresent = trimmed.startsWith(SqlLine.COMMAND_PREFIX);
    if (trimmed.startsWith("!all")
        || trimmed.startsWith("!call")
        || trimmed.startsWith("!sql")
        || !isCommandPresent) {
      isSql = true;
    }

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
              if (sqlKeyWordsBitSet == null) {
                sqlKeyWordsBitSet = new BitSet(buffer.length());
              }
              sqlKeyWordsBitSet.set(wordStart, wordStart + word.length());
            }
            wordStart = -1;
          } else {
            continue;
          }
        }
        if (ch == '"') {
          if (doubleQuoteBitSet == null) {
            doubleQuoteBitSet = new BitSet(buffer.length());
          }
          pos = handleDoubleQuotes(buffer, doubleQuoteBitSet, pos);
        }
        if (ch == '\'') {
          if (quoteBitSet == null) {
            quoteBitSet = new BitSet(buffer.length());
          }
          pos = handleSqlSingleQuotes(buffer, quoteBitSet, pos);
        }
        if (pos < buffer.length() - 1) {
          if (commentBitSet == null) {
            commentBitSet = new BitSet(buffer.length());
          }
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
    } else {
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
        if (quoteStart == -1 && doubleQuoteStart == -1 && ch == '"') {
          doubleQuoteBitSet.set(pos);
          doubleQuoteStart = pos;
        }

        if (doubleQuoteStart == -1 && quoteStart == -1 && ch == '\'') {
          quoteBitSet.set(pos);
          quoteStart = pos;
        }
      }
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
    final int commandStart = buffer.indexOf(SqlLine.COMMAND_PREFIX);
    final int commandEnd = buffer.indexOf(' ', commandStart);
    final Application.HighlightConfig highlightConfig =
        sqlLine.getHighlightConfig();
    for (int i = 0; i < buffer.length(); i++) {
      if (isSql) {
        if (sqlKeyWordsBitSet != null && sqlKeyWordsBitSet.get(i)) {
          sb.style(highlightConfig.getSqlKeywordStyle());
        } else if (quoteBitSet != null && quoteBitSet.get(i)) {
          sb.style(highlightConfig.getQuotedStyle());
        } else if (doubleQuoteBitSet != null && doubleQuoteBitSet.get(i)) {
          sb.style(highlightConfig.getDoubleQuotedStyle());
        } else if (commentBitSet != null && commentBitSet.get(i)) {
          sb.style(highlightConfig.getCommentedStyle());
        } else if (numberBitSet != null && numberBitSet.get(i)) {
          sb.style(highlightConfig.getNumbersStyle());
        } else if (i > commandEnd
            && (i < underlineStart || i > underlineEnd)
            && (i < negativeStart || i > negativeEnd)) {
          sb.style(highlightConfig.getDefaultStyle());
        }
      } else {
        if (quoteBitSet != null && quoteBitSet.get(i)) {
          sb.style(highlightConfig.getQuotedStyle());
        } else if (doubleQuoteBitSet != null && doubleQuoteBitSet.get(i)) {
          sb.style(highlightConfig.getDoubleQuotedStyle());
        }
      }

      if (i == commandStart && command) {
        sb.style(highlightConfig.getCommandStyle());
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
      if (i == commandEnd) {
        sb.style(highlightConfig.getDefaultStyle());
      }

    }
    return sb.toAttributedString();
  }

  protected int handleDoubleQuotes(
      String buffer, BitSet doubleQuoteBitSet, int pos) {
    int end = buffer.indexOf('"', pos + 1);
    end = end == -1 ? buffer.length() - 1 : end;
    doubleQuoteBitSet.set(pos, end + 1);
    pos = end;
    return pos;
  }

  protected int handleNumbers(String buffer, BitSet numberBitSet, int pos) {
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

  protected int handleSqlSingleQuotes(
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
