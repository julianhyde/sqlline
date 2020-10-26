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

import java.util.stream.Stream;

import org.jline.reader.EOFError;
import org.jline.reader.Parser;
import org.jline.reader.impl.DefaultParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import mockit.Mock;
import mockit.MockUp;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.of;
import static sqlline.SqlLineParserTest.QueryStatus.WRONG;
import static sqlline.SqlLineParserTest.QueryStatus.OK;

/**
 * Test cases for SqlLineParser.
 */
public class SqlLineParserTest {
  enum QueryStatus {
    WRONG, OK
  }

  @ParameterizedTest
  @MethodSource("provideListOfValidLines")
  public void testSqlLineParserForOkLines(String line) {
    final DefaultParser parser = new SqlLineParser(new SqlLine());
    parseValid(parser, line);
  }

  @ParameterizedTest
  @MethodSource("provideListOfInvalidLines")
  public void testSqlLineParserForWrongLines(String line) {
    final DefaultParser parser = new SqlLineParser(new SqlLine());
    parseInvalid(parser, line);
  }

  @ParameterizedTest
  @MethodSource("provideListOfInvalidLines")
  public void testSqlLineParserForWrongLinesWithEmptyPrompt(String line) {
    SqlLine sqlLine = new SqlLine();
    sqlLine.getOpts().set(BuiltInProperty.PROMPT, "");
    final DefaultParser parser = new SqlLineParser(sqlLine);
    parseInvalid(parser, line);
  }

  @ParameterizedTest
  @MethodSource("provideListOfInvalidLines")
  public void testSqlLineParserOfWrongLinesForSwitchedOfflineContinuation(
      String line) {
    final SqlLine sqlLine = new SqlLine();
    sqlLine.getOpts().set(BuiltInProperty.USE_LINE_CONTINUATION, false);
    final DefaultParser parser = new SqlLineParser(sqlLine);
    parseValid(parser, line);
  }

  /**
   * In case of exception while {@link sqlline.SqlLineParser#parse}
   * line continuation will be switched off for particular line.
   */
  @ParameterizedTest
  @MethodSource("provideListOfInvalidLines")
  public void testSqlLineParserWithException(String line) {
    new MockUp<SqlLineHighlighter>() {
      @Mock
      private boolean isLineFinishedWithSemicolon(
          final int lastNonQuoteCommentIndex, final CharSequence buffer) {
        throw new RuntimeException("Line continuation exception");
      }
    };

    final SqlLine sqlLine = new SqlLine();
    sqlLine.getOpts().set(BuiltInProperty.USE_LINE_CONTINUATION, false);
    final DefaultParser parser = new SqlLineParser(sqlLine);
    parseValid(parser, line);
  }

  @ParameterizedTest
  @MethodSource("provideListOfValidPLSQLLines")
  public void checkPLSQL(
      String line, final Dialect dialect, QueryStatus status) {
    new MockUp<DialectImpl>() {
      @Mock
      Dialect getDefault() {
        return dialect;
      }
    };

    final DefaultParser parser = new SqlLineParser(new SqlLine());
    if (status == OK) {
      parseValid(parser, line);
    } else {
      parseInvalid(parser, line);
    }
  }

  private void parseValid(Parser parser, String line) {
    try {
      parser.parse(line, line.length(), Parser.ParseContext.ACCEPT_LINE);
    } catch (Throwable t) {
      System.err.println("Problem line: [" + line + "]");
      throw t;
    }
  }

  private void parseInvalid(Parser parser, String line) {
    assertThrows(EOFError.class,
        () -> parser.parse(
            line, line.length(), Parser.ParseContext.ACCEPT_LINE));
  }

  static Stream<Arguments> provideListOfValidPLSQLLines() {
    return Stream.of(
      // POSTGRESQL
      of("$w$ wq $w$;", BuiltInDialect.POSTGRESQL, OK),
      of("$AbCd$ \"$'\"wq $AbCd$;", BuiltInDialect.POSTGRESQL, OK),
      of("$w$ wq  $w$\n/* ; */;", BuiltInDialect.POSTGRESQL, OK),
      of("$w$   $w$\n /* ; */ ;", BuiltInDialect.POSTGRESQL, OK),
      of("$www$   $www$\n /* dsfdsa' */ ;", BuiltInDialect.POSTGRESQL, OK),
      of("$wasd$   $wasd$\n /* dsfdsa */ ;", BuiltInDialect.POSTGRESQL, OK),
      // no semicolon or semicolon commented/quoted
      of("$w$ wq $$;", BuiltInDialect.POSTGRESQL, WRONG),
      of("$$ wq $w$;", BuiltInDialect.POSTGRESQL, WRONG),
      of("$w$ wq $w$", BuiltInDialect.POSTGRESQL, WRONG),
      of("$w$ wq';' $w$", BuiltInDialect.POSTGRESQL, WRONG),
      of("$w$ wq --; \n $w$", BuiltInDialect.POSTGRESQL, WRONG),
      of("$w$ wq  $w$ --;", BuiltInDialect.POSTGRESQL, WRONG),
      of("$w$ wq  $w$/*;*/", BuiltInDialect.POSTGRESQL, WRONG),
      // section name not matches
      of("$AbCd$ wq $Abcd$;", BuiltInDialect.POSTGRESQL, WRONG),
      of("$AbCd$ '$$'wq $ABcD$;", BuiltInDialect.POSTGRESQL, WRONG),
      of("$AbCd$ --$'\" \nwq $AbCd$", BuiltInDialect.POSTGRESQL, WRONG),

      // ORACLE
      of("begin end;", BuiltInDialect.ORACLE, OK),
      of("begin end\n;", BuiltInDialect.ORACLE, OK),
      of("begin --$'\" \nwq end;", BuiltInDialect.ORACLE, OK),
      of("begin --$'\" \nwq end\n;", BuiltInDialect.ORACLE, OK),
      of("begin --$'\" \nwq end /* */\n;", BuiltInDialect.ORACLE, OK),
      of("begin --$'\" \nwq end\t;", BuiltInDialect.ORACLE, OK),
      of("begin --$'\" \nwq loop end loop; /* end */ 'end ;' end ;",
          BuiltInDialect.ORACLE, OK),
      of("begin --$'\" \nwq end ;", BuiltInDialect.ORACLE, OK),
      of("declare integer test := 3; begin --$'\" \nwq end ;",
          BuiltInDialect.ORACLE, OK),
      of("integer test := 3; \n\n\nbegin --$'\" \nwq end ;",
          BuiltInDialect.ORACLE, OK)
    );
  }

  static Stream<Arguments> provideListOfInvalidLines() {
    return Stream.of(
      of("!sql"),
      of("   !all"),
      of(" \n select"),
      of(" \n test "),
      of("/"),
      of("select '1';-comment"),
      of("\nselect\n '1';- -comment\n"),
      of("select '1';comment\n\n"),
      of("\nselect '1'; -comment"),
      of(" select '1';\ncomment"),
      of("select '1';\n\n- -"),
      of("select '1';\n\n- "),
      of("select '1';\n\n-"),
      of("select '1';\n\n/"),
      of("select '1';\n \n-/-comment"),
      of("select '1'\n;\n-+-comment"),
      of("select '1'\n\n;\ncomment"),
      of("select '1';/ *comment*/"),
      of("select '1';/--*---comment */"),
      of("select '1';--/*comment\n*/\n"),
      of("select '1';/ *comment*/\n\n"),
      of("select '1'; /  *--comment*/"),
      // not ended quoted line
      of("  test ';"),
      of(" \n test ';'\";"),
      // not ended with ; (existing ; is commented)
      of("select --\n\n--\n--;"),
      of("select 1 --; --;"),
      of("select /*--\n\n--\n--;"),
      of("select /* \n ;"),
      of("select /*/;"),
      of("select --\n/*\n--\n--;"),
      of("select ' ''\n '' '\n /* ;"),
      of("select ` ``\n `` `\n /* ;"),
      // not closed quotes
      of("select ''' from t;"),
      of("select ``` from t;"),
      of("select ''' \n'' \n'' from t;"),
      of("select \"\\\" \n\\\" \n\\\" from t;"),
      // not closed brackets
      of("select to_char(123  from dual;"),
      of("select sum(count(1)  from dual;"),
      of("select [field  from t;"),
      // extra brackets
      of("select to_char)123) from dual;"),
      of("select count)123( from dual;"),
      of("select sum)count)123(( from dual;"),
      of("select sum(count)t.x)) from t;")
    );
  }

  static Stream<Arguments> provideListOfValidLines() {
    return Stream.of(
      // commands
      of("!set"),
      of(" !history"),
      of("   !scan"),
      of(" \n !set"),
      of(" \n test;"),
      of(" \n test';\n;\n';"),
      of("select \n 1\n, '\na\n ';"),
      // sql
      of("select 1;"),
      of("select '1';"),
      // sqlline command comment with odd number of quotes
      of("  #select '1"),
      of("--select '1`"),
      of(" -- select '\""),
      // one line comment right after semicolon
      of("select '1';--comment"),
      of("select '1';-----comment"),
      of("select '1';--comment\n"),
      of("select '1';--comment\n\n"),
      of("select '1'; --comment"),
      of("select '1';\n--comment"),
      of("select '1';\n\n--comment"),
      of("select '1';\n \n--comment"),
      of("select '1'\n;\n--comment"),
      of("select '1'\n\n;--comment"),
      of("select '1'\n\n;---comment"),
      of("select '1'\n\n;-- --comment"),
      of("select '1'\n\n;\n--comment"),
      of("select '1';/*comment*/"),
      of("select '1';/*---comment */"),
      of("select '1';/*comment\n*/\n"),
      of("select '1';/*comment*/\n\n"),
      of("select '1'; /*--comment*/"),
      // /* inside a quoted line
      of("select '1/*' as \"asd\";"),
      of("select '/*' as \"asd*/\";"),
      // quoted line
      of("select '1' as `asd`;"),
      of("select '1' as `\\`asd\\``;"),
      of("select '1' as \"asd\";"),
      of("select '1' as \"a's'd\";"),
      of("select '1' as \"'a's'd\n\" from t;"),
      of("select '1' as \"'a'\\\ns'd\\\n\n\" from t;"),
      of("select ' ''1'', ''2''' as \"'a'\\\ns'd\\\n\n\" from t;"),
      of("select ' ''1'', ''2''' as \"'a'\\\"\n s'd \\\" \n \\\"\n\" from t;"),
      // not a valid sql, but from sqlline parser's point of view it is ok
      // as there are no non-closed brackets, quotes, comments
      // and it ends with a semicolon
      of(" \n test;"),
      of(" \n test';\n;\n';"),

      of("select sum(my_function(x.[qwe], x.qwe)) as \"asd\" from t;"),
      of("select \n 1\n, '\na\n ';"),
      of("select /*\njust a comment\n*/\n'1';"),
      of("--comment \n values (';\n' /* comment */, '\"'"
          + "/*multiline;\n ;\n comment*/)\n -- ; \n;"),

      // non-closed or extra brackets but commented or quoted
      of("select '1(' from dual;"),
      of("select ')1' from dual;"),
      of("select 1/*count(123 */ from dual;"),
      of("select 2/* [qwe */ from dual;"),
      of("select 2 \" [qwe \" from dual;"),
      of("select 2 \" ]]][[[ \" from dual;"),
      of("select 2 \" ]]]\n[[[ \" from dual;"),
      of("select 2 \" \n]]]\n[[[ \n\" from dual;"),
      of("select 2 \n --]]]\n --[[[ \n from dual;"));
  }
}

// End SqlLineParserTest.java
