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

import org.jline.reader.EOFError;
import org.jline.reader.Parser;
import org.jline.reader.impl.DefaultParser;
import org.junit.jupiter.api.Test;

import mockit.Mock;
import mockit.MockUp;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test cases for SqlLineParser.
 */
public class SqlLineParserTest {
  private static final String[] WRONG_LINES = {
      "!sql",
      "   !all",
      " \n select",
      " \n test ",
      "/",
      "select '1';-comment",
      "\nselect\n '1';- -comment\n",
      "select '1';comment\n\n",
      "\nselect '1'; -comment",
      " select '1';\ncomment",
      "select '1';\n\n- -",
      "select '1';\n\n- ",
      "select '1';\n\n-",
      "select '1';\n\n/",
      "select '1';\n \n-/-comment",
      "select '1'\n;\n-+-comment",
      "select '1'\n\n;\ncomment",
      "select '1';/ *comment*/",
      "select '1';/--*---comment */",
      "select '1';--/*comment\n*/\n",
      "select '1';/ *comment*/\n\n",
      "select '1'; /  *--comment*/",
      // not ended quoted line
      "  test ';",
      " \n test ';'\";",
      // not ended with ; (existing ; is commented)
      "select --\n\n--\n--;",
      "select 1 --; --;",
      "select /*--\n\n--\n--;",
      "select /* \n ;",
      "select --\n/*\n--\n--;",
      "select ' ''\n '' '\n /* ;",
      "select ` ``\n `` `\n /* ;",
      // not closed quotes
      "select ''' from t;",
      "select ``` from t;",
      "select ''' \n'' \n'' from t;",
      "select \"\\\" \n\\\" \n\\\" from t;",
      // not closed brackets
      "select to_char(123  from dual;",
      "select sum(count(1)  from dual;",
      "select [field  from t;",
      // extra brackets
      "select to_char)123) from dual;",
      "select count)123( from dual;",
      "select sum)count)123(( from dual;",
      "select sum(count)t.x)) from t;"
  };

  @Test
  public void testSqlLineParserForOkLines() {
    final DefaultParser parser = new SqlLineParser(new SqlLine());
    final Parser.ParseContext acceptLine = Parser.ParseContext.ACCEPT_LINE;
    final String[] lines = {
        // commands
        "!set",
        " !history",
        "   !scan",
        " \n !set",
        " \n test;",
        " \n test';\n;\n';",
        "select \n 1\n, '\na\n ';",
        // sql
        "select 1;",
        "select '1';",
        // sqlline command comment with odd number of quotes
        "  #select '1",
        "--select '1`",
        " -- select '\"",
        // one line comment right after semicolon
        "select '1';--comment",
        "select '1';-----comment",
        "select '1';--comment\n",
        "select '1';--comment\n\n",
        "select '1'; --comment",
        "select '1';\n--comment",
        "select '1';\n\n--comment",
        "select '1';\n \n--comment",
        "select '1'\n;\n--comment",
        "select '1'\n\n;--comment",
        "select '1'\n\n;---comment",
        "select '1'\n\n;-- --comment",
        "select '1'\n\n;\n--comment",
        "select '1';/*comment*/",
        "select '1';/*---comment */",
        "select '1';/*comment\n*/\n",
        "select '1';/*comment*/\n\n",
        "select '1'; /*--comment*/",
        // /* inside a quoted line
        "select '1/*' as \"asd\";",
        "select '/*' as \"asd*/\";",
        // quoted line
        "select '1' as `asd`;",
        "select '1' as `\\`asd\\``;",
        "select '1' as \"asd\";",
        "select '1' as \"a's'd\";",
        "select '1' as \"'a's'd\n\" from t;",
        "select '1' as \"'a'\\\ns'd\\\n\n\" from t;",
        "select ' ''1'', ''2''' as \"'a'\\\ns'd\\\n\n\" from t;",
        "select ' ''1'', ''2''' as \"'a'\\\"\n s'd \\\" \n \\\"\n\" from t;",
        // not a valid sql, but from sqlline parser's point of view it is ok
        // as there are no non-closed brackets, quotes, comments
        // and it ends with a semicolon
        " \n test;",
        " \n test';\n;\n';",

        "select sum(my_function(x.[qwe], x.qwe)) as \"asd\" from t;",
        "select \n 1\n, '\na\n ';",
        "select /*\njust a comment\n*/\n'1';",
        "--comment \n values (';\n' /* comment */, '\"'"
            + "/*multiline;\n ;\n comment*/)\n -- ; \n;",

        // non-closed or extra brackets but commented or quoted
        "select '1(' from dual;",
        "select ')1' from dual;",
        "select 1/*count(123 */ from dual;",
        "select 2/* [qwe */ from dual;",
        "select 2 \" [qwe \" from dual;",
        "select 2 \" ]]][[[ \" from dual;",
        "select 2 \" ]]]\n[[[ \" from dual;",
        "select 2 \" \n]]]\n[[[ \n\" from dual;",
        "select 2 \n --]]]\n --[[[ \n from dual;",
    };
    for (String line : lines) {
      try {
        parser.parse(line, line.length(), acceptLine);
      } catch (Throwable t) {
        System.err.println("Problem line: [" + line + "]");
        throw t;
      }
    }
  }

  @Test
  public void testSqlLineParserForWrongLines() {
    final DefaultParser parser = new SqlLineParser(new SqlLine());
    final Parser.ParseContext acceptLine = Parser.ParseContext.ACCEPT_LINE;
    for (String line : WRONG_LINES) {
      try {
        parser.parse(line, line.length(), acceptLine);
        fail("Missing closing quote or semicolon for line " + line);
      } catch (EOFError eofError) {
        //ok
      }
    }
  }

  @Test
  public void testSqlLineParserForWrongLinesWithEmptyPrompt() {
    SqlLine sqlLine = new SqlLine();
    sqlLine.getOpts().set(BuiltInProperty.PROMPT, "");
    final DefaultParser parser = new SqlLineParser(sqlLine);
    final Parser.ParseContext acceptLine = Parser.ParseContext.ACCEPT_LINE;
    for (String line : WRONG_LINES) {
      try {
        parser.parse(line, line.length(), acceptLine);
        fail("Missing closing quote or semicolon for line " + line);
      } catch (EOFError eofError) {
        //ok
      }
    }
  }

  @Test
  public void testSqlLineParserOfWrongLinesForSwitchedOfflineContinuation() {
    final SqlLine sqlLine = new SqlLine();
    sqlLine.getOpts().set(BuiltInProperty.USE_LINE_CONTINUATION, false);
    final DefaultParser parser = new SqlLineParser(sqlLine);
    final Parser.ParseContext acceptLine = Parser.ParseContext.ACCEPT_LINE;
    for (String line : WRONG_LINES) {
      try {
        parser.parse(line, line.length(), acceptLine);
      } catch (Throwable t) {
        System.err.println("Problem line: [" + line + "]");
        throw t;
      }
    }
  }


  /**
   * In case of exception while {@link sqlline.SqlLineParser#parse}
   * line continuation will be switched off for particular line.
   */
  @Test
  public void testSqlLineParserWithException() {
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
    final Parser.ParseContext acceptLine = Parser.ParseContext.ACCEPT_LINE;
    for (String line : WRONG_LINES) {
      try {
        parser.parse(line, line.length(), acceptLine);
      } catch (Throwable t) {
        System.err.println("Problem line: [" + line + "]");
        throw t;
      }
    }
  }
}

// End SqlLineParserTest.java
