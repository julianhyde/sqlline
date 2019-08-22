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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.jline.reader.EOFError;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.LineReaderImpl;

import static sqlline.Commands.flush;

/**
 * SqlLineParser implements multi-line
 * for sql, !sql, !all while it's not ended with a non-commented ';'.
 *
 * <p>The following table shows each of the prompts you may see and
 * summarizes what they mean about the state that sqlline is in.
 *
 * <table>
 * <caption>SQLLine continuation prompts</caption>
 * <tr>
 *   <th>Prompt</th>
 *   <th>Meaning</th>
 * </tr>
 * <tr>
 *   <td>sqlline&gt;</td>
 *   <td>Ready for a new query</td>
 * </tr>
 * <tr>
 *   <td>semicolon&gt;</td>
 *   <td>Waiting for next line of multiple-line query,
 *       waiting for completion of query with semicolon (;)</td>
 * </tr>
 * <tr>
 *   <td>quote&gt;</td>
 *   <td>Waiting for next line, waiting for completion of
 *       a string that began with a single quote (')</td>
 * </tr>
 * <tr>
 *   <td>dquote&gt;</td>
 *   <td>Waiting for next line, waiting for completion of
 *       a string that began with a double quote (")</td>
 * </tr>
 * <tr>
 *   <td>`&gt;</td>
 *   <td>Waiting for next line, waiting for completion of
 *       a string that began with (`)</td>
 * </tr>
 * <tr>
 *   <td>*\/&gt;</td>
 *   <td>Waiting for next line, waiting for completion of
 *       a multi-line comment that began with "/*"</td>
 * </tr>
 * <tr>
 *   <td>)&gt;</td>
 *   <td>Waiting for next line, waiting for completion of
 *       a string that began with a round bracket, "("</td>
 * </tr>
 * <tr>
 *   <td>]&gt;</td>
 *   <td>Waiting for next line, waiting for completion of
 *       a string that began with a square bracket, "["</td>
 * </tr>
 * <tr>
 *   <td>extra ')'&gt;</td>
 *   <td>There is an extra round bracket, ")", that is
 *       not opened with "("</td>
 * </tr>
 * <tr>
 *   <td>extra ']'&gt;</td>
 *   <td>There is an extra square bracket "]" that is
 *       not opened with "["</td>
 * </tr>
 * </table>
 */
public class SqlLineParser extends DefaultParser {
  private static final String DEFAULT_QUOTES = "'\"`";

  private final SqlLine sqlLine;

  public SqlLineParser(final SqlLine sqlLine) {
    this.sqlLine = sqlLine;
    String quotes = DEFAULT_QUOTES;
    final char openQuote = sqlLine.getDialect().getOpenQuote();
    if ('[' != openQuote
        && '(' != openQuote
        && DEFAULT_QUOTES.indexOf(openQuote) == -1) {
      quotes += openQuote;
    }
    quoteChars(quotes.toCharArray());
  }

  public ParsedLine parse(final String line, final int cursor,
      ParseContext context) {
    try {
      if (sqlLine.getOpts().getUseLineContinuation()
          && !sqlLine.isPrompting()) {
        eofOnUnclosedQuote(true);
        eofOnEscapedNewLine(true);
      } else {
        eofOnUnclosedQuote(false);
        eofOnEscapedNewLine(false);
        return super.parse(line, cursor, context);
      }

      final SqlLineArgumentList argumentList =
          parseState(line, cursor, context);

      if (argumentList.state == SqlParserState.NEW_LINE) {
        throw new EOFError(-1, -1, argumentList.state.message,
            argumentList.supplier.get());
      }

      if (context != ParseContext.COMPLETE) {
        switch (argumentList.state) {
        case QUOTED:
        case MULTILINE_COMMENT:
        case ROUND_BRACKET_BALANCE_FAILED:
        case SQUARE_BRACKET_BALANCE_FAILED:
        case SEMICOLON_REQUIRED:
          throw new EOFError(-1, -1, argumentList.state.message,
              argumentList.supplier.get());
        }
      }

      if (argumentList.state == SqlParserState.LINE_CONTINUES) {
        throw new EOFError(-1, -1, argumentList.state.message,
            argumentList.supplier.get());
      }

      return argumentList;
    } catch (Exception e) {
      if (e instanceof EOFError) {
        // line continuation is expected
        throw e;
      }
      sqlLine.handleException(e);
      return super.parse(line, cursor, context);
    }
  }

  public SqlLineArgumentList parseState(
      final String line, final int cursor, ParseContext context) {
    final List<String> words = new LinkedList<>();
    final StringBuilder current = new StringBuilder();
    boolean containsNonCommentData = false;
    int wordCursor = -1;
    int wordIndex = -1;
    int quoteStart = -1;
    int oneLineCommentStart = -1;
    int multiLineCommentStart = -1;
    // if both elements of array are 0 then it is ok
    // otherwise it should fail
    final int[] roundBracketsBalance = new int[2];
    final int[] squareBracketsBalance = new int[2];
    int rawWordCursor = -1;
    int rawWordLength = -1;
    int rawWordStart = 0;
    int lastNonQuoteCommentIndex = 0;
    boolean isSql = isSql(sqlLine, line, context);
    final Dialect dialect = sqlLine.getDialect();

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
          && quoteStart < 0
          && (isQuoteChar(line, i)
              || dialect.getOpenQuote() == line.charAt(i)
              || dialect.getCloseQuote() == line.charAt(i))) {
        // Start a quote block
        quoteStart = i;
        if (line.charAt(quoteStart) == dialect.getOpenQuote()) {
          current.append(line.charAt(i));
        }
        containsNonCommentData = true;
      } else {
        char currentChar = line.charAt(i);
        if (quoteStart >= 0) {
          // In a quote block
          if ((line.charAt(quoteStart) == currentChar
              || (currentChar == dialect.getCloseQuote()
                  && line.charAt(quoteStart) == dialect.getOpenQuote()))
              && !isEscaped(line, i)) {
            // End the block; arg could be empty, but that's fine
            if (line.charAt(quoteStart) == dialect.getOpenQuote()) {
              current.append(dialect.getCloseQuote());
            } else {
              words.add(flush(current));
              if (rawWordCursor >= 0 && rawWordLength < 0) {
                rawWordLength = i - rawWordStart + 1;
              }
            }
            quoteStart = -1;
          } else {
            if (!isEscapeChar(line, i)) {
              // Take the next character
              current.append(currentChar);
            }
          }
        } else if (oneLineCommentStart == -1 && isMultiLineComment(line, i)) {
          multiLineCommentStart = i;
          rawWordLength = getRawWordLength(words, current, rawWordCursor,
              rawWordLength, rawWordStart, i);
          rawWordStart = i + 1;
        } else if (multiLineCommentStart >= 0) {
          if (i - multiLineCommentStart > 2
              && currentChar == '/' && line.charAt(i - 1) == '*') {
            // End the block; arg could be empty, but that's fine
            words.add(flush(current));
            multiLineCommentStart = -1;
            if (rawWordCursor >= 0 && rawWordLength < 0) {
              rawWordLength = i - rawWordStart + 1;
            }
          }
        } else if (oneLineCommentStart == -1 && isOneLineComment(line, i)) {
          oneLineCommentStart = i;
          rawWordLength = getRawWordLength(words, current, rawWordCursor,
              rawWordLength, rawWordStart, i);
          rawWordStart = i + 1;
        } else if (oneLineCommentStart >= 0) {
          if (currentChar == '\n') {
            // End the block; arg could be empty, but that's fine
            oneLineCommentStart = -1;
            rawWordLength = getRawWordLength(words, current, rawWordCursor,
                rawWordLength, rawWordStart, i);
            rawWordStart = i + 1;
          } else {
            current.append(currentChar);
          }
        } else {
          // Not in a quote or comment block
          checkBracketBalance(roundBracketsBalance, currentChar, '(', ')');
          if (dialect.getOpenQuote() != '[') {
            checkBracketBalance(squareBracketsBalance, currentChar, '[', ']');
          }
          containsNonCommentData = true;
          if (!Character.isWhitespace(currentChar)) {
            lastNonQuoteCommentIndex = i;
          }
          if (isDelimiter(line, i)) {
            rawWordLength = getRawWordLength(words, current, rawWordCursor,
                rawWordLength, rawWordStart, i);
            rawWordStart = i + 1;
          } else {
            if (!isEscapeChar(line, i)) {
              current.append(currentChar);
            }
          }
        }
      }
    }

    if (current.length() > 0 || cursor == line.length()) {
      if (quoteStart >= 0 && line.charAt(quoteStart)
          == dialect.getOpenQuote()) {
        words.add(current.toString());
      } else {
        words.add(current.toString());
      }
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

    final String openingQuote = quoteStart >= 0
        ? line.substring(quoteStart, quoteStart + 1) : null;

    if (isEofOnEscapedNewLine() && isEscapeChar(line, line.length() - 1)) {
      return new SqlLineArgumentList(SqlParserState.NEW_LINE,
          () -> getPaddedPrompt("newline"),
          line, words, wordIndex, wordCursor,
          cursor, openingQuote, rawWordCursor, rawWordLength);
    }

    if (context != ParseContext.COMPLETE) {
      if (isEofOnUnclosedQuote() && quoteStart >= 0) {
        int finalQuoteStart = quoteStart;
        return new SqlLineArgumentList(SqlParserState.QUOTED,
            () -> getPaddedPrompt(
                getQuoteWaitingPattern(line, finalQuoteStart)),
            line, words, wordIndex, wordCursor,
            cursor, openingQuote, rawWordCursor, rawWordLength);
      }

      if (isSql) {
        if (multiLineCommentStart != -1) {
          return new SqlLineArgumentList(SqlParserState.MULTILINE_COMMENT,
              () -> getPaddedPrompt("*/"), line, words, wordIndex,
              wordCursor, cursor, openingQuote, rawWordCursor, rawWordLength);
        }

        if (squareBracketsBalance[0] != 0 || squareBracketsBalance[1] != 0) {
          return new SqlLineArgumentList(
              SqlParserState.SQUARE_BRACKET_BALANCE_FAILED,
              () -> getPaddedPrompt(
                  squareBracketsBalance[0] == 0 ? "extra ']'" : "]"),
              line, words, wordIndex, wordCursor,
              cursor, openingQuote, rawWordCursor, rawWordLength);
        }

        if (roundBracketsBalance[0] != 0 || roundBracketsBalance[1] != 0) {
          return new SqlLineArgumentList(
              SqlParserState.ROUND_BRACKET_BALANCE_FAILED,
              () -> getPaddedPrompt(
                  roundBracketsBalance[0] == 0 ? "extra ')'" : ")"),
              line, words, wordIndex, wordCursor,
              cursor, openingQuote, rawWordCursor, rawWordLength);
        }

        final int lastNonQuoteCommentIndex1 =
            lastNonQuoteCommentIndex == line.length() - 1
                && lastNonQuoteCommentIndex - 1 >= 0
                ? lastNonQuoteCommentIndex - 1 : lastNonQuoteCommentIndex;
        if (containsNonCommentData
            && !isLineFinishedWithSemicolon(
            lastNonQuoteCommentIndex1, line)) {
          return new SqlLineArgumentList(SqlParserState.SEMICOLON_REQUIRED,
              () -> getPaddedPrompt("semicolon"),
              line, words, wordIndex, wordCursor,
              cursor, openingQuote, rawWordCursor, rawWordLength);
        }
      }
    }

    if (line.endsWith("\n")
        && sqlLine.getLineReader() != null
        && !Objects.equals(line,
        sqlLine.getLineReader().getBuffer().toString())) {
      return new SqlLineArgumentList(
          SqlParserState.NEW_LINE, () -> getPaddedPrompt(""));
    }

    return new SqlLineArgumentList(SqlParserState.OK, () -> "",
        line, words, wordIndex, wordCursor,
        cursor, openingQuote, rawWordCursor, rawWordLength);
  }

  public String getQuoteWaitingPattern(String line, int quoteStart) {
    switch (line.charAt(quoteStart)) {
    case '\'':
      return "quote";
    case '"':
      return "dquote";
    case '`':
      return "`";
    default:
      return String.valueOf(line.charAt(quoteStart));
    }
  }

  private void checkBracketBalance(int[] balance, char actual,
      char openBracket, char closeBracket) {
    if (actual == openBracket) {
      balance[0]++;
    } else if (actual == closeBracket) {
      if (balance[0] > 0) {
        balance[0]--;
      } else {
        // closed bracket without open
        balance[1]++;
      }
    }
  }

  private int getRawWordLength(
      List<String> words,
      StringBuilder current,
      int rawWordCursor,
      int rawWordLength,
      int rawWordStart,
      int i) {
    if (current.length() > 0) {
      words.add(flush(current));
      if (rawWordCursor >= 0 && rawWordLength < 0) {
        rawWordLength = i - rawWordStart;
      }
    }
    return rawWordLength;
  }

  static boolean isSql(SqlLine sqlLine, String line, ParseContext context) {
    String trimmedLine = trimLeadingSpacesIfPossible(line, context);
    return !trimmedLine.isEmpty()
        && !sqlLine.isOneLineComment(trimmedLine, false)
        && (trimmedLine.charAt(0) != '!'
            || trimmedLine.regionMatches(0, "!sql", 0, "!sql".length())
            || trimmedLine.regionMatches(0, "!all", 0, "!all".length()));
  }

  /**
   * Returns whether a line (already trimmed) ends with a semicolon that
   * is not commented with one line comment.
   *
   * <p>ASSUMPTION: to have correct behavior, this method must be
   * called after quote and multi-line comments check calls, which implies that
   * there are no non-finished quotations or multi-line comments.
   *
   * @param buffer Input line to check for ending with ';'
   * @return true if the ends with non-commented ';'
   */
  private boolean isLineFinishedWithSemicolon(
      final int lastNonQuoteCommentIndex, final CharSequence buffer) {
    final String line = buffer.toString();
    boolean lineEmptyOrFinishedWithSemicolon = line.isEmpty();
    boolean requiredSemicolon = false;
    for (int i = lastNonQuoteCommentIndex; i < line.length(); i++) {
      if (';' == line.charAt(i)) {
        lineEmptyOrFinishedWithSemicolon = true;
        continue;
      } else if (i < line.length() - 1
          && line.regionMatches(i, "/*", 0, "/*".length())) {
        int nextNonCommentedChar = line.indexOf("*/", i + "/*".length());
        // From one side there is an assumption that multi-line comment
        // is completed, from the other side nextNonCommentedChar
        // could be negative or less than lastNonQuoteCommentIndex
        // in case '/*' is a part of quoting string.
        if (nextNonCommentedChar > lastNonQuoteCommentIndex) {
          i = nextNonCommentedChar + "*/".length();
        }
      } else {
        final Dialect dialect = sqlLine.getDialect();
        for (String oneLineCommentString : dialect.getOneLineComments()) {
          if (i <= buffer.length() - oneLineCommentString.length()
              && oneLineCommentString
                  .regionMatches(0, line, i, oneLineCommentString.length())) {
            int nextLine = line.indexOf('\n', i + 1);
            if (nextLine > lastNonQuoteCommentIndex) {
              i = nextLine;
            } else {
              return !requiredSemicolon || lineEmptyOrFinishedWithSemicolon;
            }
          }
        }
      }
      requiredSemicolon = i == line.length()
          ? requiredSemicolon
          : !lineEmptyOrFinishedWithSemicolon
              || !Character.isWhitespace(line.charAt(i));
      if (requiredSemicolon) {
        lineEmptyOrFinishedWithSemicolon = false;
      }
    }
    return !requiredSemicolon || lineEmptyOrFinishedWithSemicolon;
  }

  private boolean isOneLineComment(final String buffer, final int pos) {
    final Dialect dialect = sqlLine.getDialect();
    final int newLinePos = buffer.indexOf('\n');
    if ((newLinePos == -1 || newLinePos > pos)
        && buffer.substring(0, pos).trim().isEmpty()) {
      for (String oneLineCommentString : dialect.getSqlLineOneLineComments()) {
        if (pos <= buffer.length() - oneLineCommentString.length()
            && oneLineCommentString
                .regionMatches(0, buffer, pos, oneLineCommentString.length())) {
          return true;
        }
      }
    }
    for (String oneLineCommentString : dialect.getOneLineComments()) {
      if (pos <= buffer.length() - oneLineCommentString.length()
          && oneLineCommentString
              .regionMatches(0, buffer, pos, oneLineCommentString.length())) {
        return true;
      }
    }
    return false;
  }

  private boolean isMultiLineComment(final CharSequence buffer, final int pos) {
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
    if (sqlLine.getOpts().getShowLineNumbers()
        && sqlLine.getLineReader() != null) {
      sqlLine.getLineReader()
          .setVariable(LineReader.SECONDARY_PROMPT_PATTERN, "%N%P.%M> ");
      return waitingPattern;
    } else {
      if (sqlLine.getLineReader() != null) {
        sqlLine.getLineReader().setVariable(LineReader.SECONDARY_PROMPT_PATTERN,
            LineReaderImpl.DEFAULT_SECONDARY_PROMPT_PATTERN);
      }
      int length = sqlLine.getPromptHandler().getPrompt().columnLength();
      StringBuilder prompt = new StringBuilder(length);
      for (int i = 0;
           i < length - "> ".length() - waitingPattern.length(); i++) {
        prompt.append(i % 2 == 0 ? '.' : ' ');
      }
      prompt.append(waitingPattern);
      return prompt.toString();
    }
  }

  public class SqlLineArgumentList extends DefaultParser.ArgumentList {
    private final SqlParserState state;
    private final Supplier<String> supplier;
    public SqlLineArgumentList(SqlParserState state, Supplier<String> supplier,
        String line, List<String> words, int wordIndex, int wordCursor,
        int cursor, String openingQuote, int rawWordCursor, int rawWordLength) {
      super(line, words, wordIndex, wordCursor, cursor,
          openingQuote, rawWordCursor, rawWordLength);
      this.state = state;
      this.supplier = supplier;
    }

    // constructor for negative states
    public SqlLineArgumentList(
        SqlParserState state, Supplier<String> supplier) {
      super(null, Collections.emptyList(), -1, -1, -1, null, -1, -1);
      assert state != SqlParserState.OK;
      this.state = state;
      this.supplier = supplier;
    }

    @Override public CharSequence escape(
        CharSequence candidate, boolean complete) {
      return candidate;
    }

    public SqlParserState getState() {
      return state;
    }

    public Supplier<String> getSupplier() {
      return supplier;
    }
  }

  public enum SqlParserState {
    LINE_CONTINUES("Line continues"),
    MULTILINE_COMMENT("Missing end of comment"),
    NEW_LINE("Escaped new line"),
    OK("ok"),
    QUOTED("Missing closing quote"),
    ROUND_BRACKET_BALANCE_FAILED("Round brackets balance fails"),
    SEMICOLON_REQUIRED("Missing semicolon at the end"),
    SQUARE_BRACKET_BALANCE_FAILED("Square brackets balance fails");

    private final String message;
    SqlParserState(String message) {
      this.message = message;
    }

    public String getMessage() {
      return message;
    }
  }
}

// End SqlLineParser.java
