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

import java.util.LinkedList;
import java.util.List;

import org.jline.reader.EOFError;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.DefaultParser;

/**
 * SqlLineParser implements multiline
 * for sql, !sql, !all while its not ended with ';'.
 */
public class SqlLineParser extends DefaultParser {
  private final SqlLine sqlLine;

  public SqlLineParser(final SqlLine sqlLine) {
    this.sqlLine = sqlLine;
  }

  public ParsedLine parse(final String line, final int cursor,
      ParseContext context) {
    List<String> words = new LinkedList<>();
    StringBuilder current = new StringBuilder();

    int wordCursor = -1;
    int wordIndex = -1;
    int quoteStart = -1;
    int rawWordCursor = -1;
    int rawWordLength = -1;
    int rawWordStart = 0;
    boolean isSql = isSql(line, context);

    for (int i = 0; i < line.length(); i++) {
      // once we reach the cursor, set the
      // position of the selected index
      if (i == cursor) {
        wordIndex = words.size();
        // the position in the current argument is just the
        // length of the current argument
        wordCursor = current.length();
        rawWordCursor = i - rawWordStart;
      }

      if (quoteStart < 0 && isQuoteChar(line, i)) {
        // Start a quote block
        quoteStart = i;
      } else if (quoteStart >= 0) {
        // In a quote block
        if (line.charAt(quoteStart) == line.charAt(i) && !isEscaped(line, i)) {
          // End the block; arg could be empty, but that's fine
          words.add(current.toString());
          current.setLength(0);
          quoteStart = -1;
          if (rawWordCursor >= 0 && rawWordLength < 0) {
            rawWordLength = i - rawWordStart + 1;
          }
        } else {
          if (!isEscapeChar(line, i)) {
            // Take the next character
            current.append(line.charAt(i));
          }
        }
      } else {
        // Not in a quote block
        if (isDelimiter(line, i)) {
          if (current.length() > 0) {
            words.add(current.toString());
            current.setLength(0); // reset the arg
            if (rawWordCursor >= 0 && rawWordLength < 0) {
              rawWordLength = i - rawWordStart;
            }
          }
          rawWordStart = i + 1;
        } else {
          if (!isEscapeChar(line, i)) {
            current.append(line.charAt(i));
          }
        }
      }
    }

    if (current.length() > 0 || cursor == line.length()) {
      words.add(current.toString());
      if (rawWordCursor >= 0 && rawWordLength < 0) {
        rawWordLength = line.length() - rawWordStart;
      }
    }

    if (cursor == line.length()) {
      wordIndex = words.size() - 1;
      wordCursor = words.get(words.size() - 1).length();
      rawWordCursor = cursor - rawWordStart;
      rawWordLength = rawWordCursor;
    }

    if (isEofOnEscapedNewLine() && isEscapeChar(line, line.length() - 1)) {
      throw new EOFError(
          -1, -1, "Escaped new line", getPaddedPrompt("newline"));
    }
    if (isEofOnUnclosedQuote() && quoteStart >= 0
        && context != ParseContext.COMPLETE) {
      throw new EOFError(-1, -1, "Missing closing quote",
          getPaddedPrompt(line.charAt(quoteStart) == '\''
              ? "quote" : "dquote"));
    }

    if (isSql && !line.trim().endsWith(";")
        && context != ParseContext.COMPLETE) {
      throw new EOFError(-1, -1, "Missing semicolon at the end",
          getPaddedPrompt("semicolon"));
    }
    String openingQuote = quoteStart >= 0
        ? line.substring(quoteStart, quoteStart + 1) : null;
    return new ArgumentList(line, words, wordIndex, wordCursor,
        cursor, openingQuote, rawWordCursor, rawWordLength);
  }

  private boolean isSql(String line, ParseContext context) {
    String trimmedLine = trimLeadingSpacesIfPossible(line, context);
    return  !trimmedLine.isEmpty() && (trimmedLine.charAt(0) != '!'
        || trimmedLine.regionMatches(0, "!sql", 0, "!sql".length())
        || trimmedLine.regionMatches(0, "!all", 0, "!all".length()));
  }

  public static String trimLeadingSpacesIfPossible(
      String line, ParseContext context) {
    if (context != ParseContext.ACCEPT_LINE) {
      return line;
    }
    char[] chars = line.toCharArray();
    int i = 0;
    while (i < chars.length && Character.isWhitespace(chars[i])) {
      i++;
    }
    return line.substring(i);
  }

  private String getPaddedPrompt(String waitingPattern) {
    int length = sqlLine.getPrompt().length();
    StringBuilder prompt = new StringBuilder(length - 2);
    for (int i = 0; i < length - 2 - waitingPattern.length(); i++) {
      prompt.append(i % 2 == 0 ? '.' : ' ');
    }
    prompt.append(waitingPattern);
    return prompt.toString();
  }
}

// End SqlLineParser.java
