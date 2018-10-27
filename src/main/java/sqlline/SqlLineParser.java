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
 * for sql, !sql, !all while its not ended with non commented ';'.
 * The following table shows each of the prompts you may see and
 * summarizes what they mean about the state that sqlline is in.
 *
 * +---------------+-----------------------------------------------------+
 * |    Prompt     |                    Meaning                          |
 * +---------------+-----------------------------------------------------+
 * | sqlline&gt;   |  Ready for a new query                              |
 * +---------------+-----------------------------------------------------+
 * | semicolon&gt; |  Waiting for next line of multiple-line query,      |
 * |               |  waiting for completion of query with semicolon (;) |
 * +---------------+-----------------------------------------------------+
 * | quote&gt;     |  Waiting for next line, waiting for completion of   |
 * |               |  a string that began with a single quote (')        |
 * +---------------+-----------------------------------------------------+
 * | dquote&gt;    |  Waiting for next line, waiting for completion of   |
 * |               |  a string that began with a double quote (")        |
 * +---------------+-----------------------------------------------------+
 * | *\/&gt;       |  Waiting for next line, waiting for completion of   |
 * |               |  a multiline comment that began with /*             |
 * +---------------+-----------------------------------------------------+
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

    boolean containsNonCommentData = false;
    int wordCursor = -1;
    int wordIndex = -1;
    int quoteStart = -1;
    int oneLineCommentStart = -1;
    int multiLineCommentStart = -1;
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
      if (oneLineCommentStart == -1
          && multiLineCommentStart == -1
          && quoteStart < 0 && isQuoteChar(line, i)) {
        // Start a quote block
        quoteStart = i;
        containsNonCommentData = true;
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
      } else if (oneLineCommentStart == -1 && isMultilineComment(line, i)) {
        multiLineCommentStart = i;
        rawWordLength = getRawWordLength(
            words, current, rawWordCursor, rawWordLength, rawWordStart, i);
        rawWordStart = i + 1;
      } else if (multiLineCommentStart >= 0) {
        if (line.charAt(i) == '/' && line.charAt(i - 1) == '*') {
          // End the block; arg could be empty, but that's fine
          words.add(current.toString());
          current.setLength(0);
          multiLineCommentStart = -1;
          if (rawWordCursor >= 0 && rawWordLength < 0) {
            rawWordLength = i - rawWordStart + 1;
          }
        }
      } else if (oneLineCommentStart == -1 && isOneLineComment(line, i)) {
        oneLineCommentStart = i;
        rawWordLength = getRawWordLength(
            words, current, rawWordCursor, rawWordLength, rawWordStart, i);
        rawWordStart = i + 1;
      } else if (oneLineCommentStart >= 0) {
        if (line.charAt(i) == 13) {
          // End the block; arg could be empty, but that's fine
          oneLineCommentStart = -1;
          rawWordLength = getRawWordLength(
              words, current, rawWordCursor, rawWordLength, rawWordStart, i);
          rawWordStart = i + 1;
        } else {
          current.append(line.charAt(i));
        }
      } else {
        // Not in a quote or comment block
        containsNonCommentData = true;
        if (isDelimiter(line, i)) {
          rawWordLength = getRawWordLength(
              words, current, rawWordCursor, rawWordLength, rawWordStart, i);
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

    if (isSql && context != ParseContext.COMPLETE
        && multiLineCommentStart != -1) {
      throw new EOFError(-1, -1, "Missing end of comment",
          getPaddedPrompt("*/"));
    }

    if (isSql && containsNonCommentData && context != ParseContext.COMPLETE
        && !isLineFinishedWithSemicolon(line)) {
      throw new EOFError(-1, -1, "Missing semicolon at the end",
          getPaddedPrompt("semicolon"));
    }

    String openingQuote = quoteStart >= 0
        ? line.substring(quoteStart, quoteStart + 1) : null;
    return new ArgumentList(line, words, wordIndex, wordCursor,
        cursor, openingQuote, rawWordCursor, rawWordLength);
  }

  private int getRawWordLength(
      List<String> words,
      StringBuilder current,
      int rawWordCursor,
      int rawWordLength,
      int rawWordStart,
      int i) {
    if (current.length() > 0) {
      words.add(current.toString());
      current.setLength(0); // reset the arg
      if (rawWordCursor >= 0 && rawWordLength < 0) {
        rawWordLength = i - rawWordStart;
      }
    }
    return rawWordLength;
  }

  private boolean isSql(String line, ParseContext context) {
    String trimmedLine = trimLeadingSpacesIfPossible(line, context);
    return  !trimmedLine.isEmpty() && (trimmedLine.charAt(0) != '!'
        || trimmedLine.regionMatches(0, "!sql", 0, "!sql".length())
        || trimmedLine.regionMatches(0, "!all", 0, "!all".length()));
  }

  /**
   * Checks if the line (trimmed) ends with semicolon which
   * is not commented with one line comment
   * ASSUMPTION: to have correct behavior should be
   * called after quote and multiline check calls
   * @param buffer input line to check for ending with ;
   * @return true if the ends with non commented `;`
   */
  private boolean isLineFinishedWithSemicolon(final CharSequence buffer) {
    final String line = buffer.toString().trim();
    boolean result = false;
    for (int i = line.length() - 1; i >= 0; i--) {
      switch (line.charAt(i)) {
      case ';' :
        result = true;
        break;
      case '-' :
        if (i > 0 && line.charAt(i - 1) == '-') {
          return false;
        }
        break;
      case '\n' :
        return result;
      }
    }
    return result;
  }

  private boolean isOneLineComment(final CharSequence buffer, final int pos) {
    return pos < buffer.length() - 1
        && buffer.charAt(pos) == '-'
        && buffer.charAt(pos + 1) == '-';
  }

  private boolean isMultilineComment(final CharSequence buffer, final int pos) {
    return pos < buffer.length() - 1
        && buffer.charAt(pos) == '/'
        && buffer.charAt(pos + 1) == '*';
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
