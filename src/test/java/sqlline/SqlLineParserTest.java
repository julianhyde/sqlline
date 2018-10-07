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
import org.junit.Assert;
import org.junit.Test;

/**
 * Test cases for SqlLineParser.
 */
public class SqlLineParserTest {
  @Test
  public void testSqlLineParserForOkLines() {
    DefaultParser parser = new SqlLineParser(new SqlLine())
        .eofOnUnclosedQuote(true)
        .eofOnEscapedNewLine(true);
    Parser.ParseContext acceptLine = Parser.ParseContext.ACCEPT_LINE;
    String[] successfulLinesToCheck = {
        "!set",
        " !history",
        "   !scan",
        " \n !set",
        " \n test;",
        " \n test';\n;\n';",
        "select \n 1\n, '\na\n ';",
    };
    for (String line : successfulLinesToCheck) {
      parser.parse(line, line.length(), acceptLine);
    }
  }

  @Test
  public void testSqlLineParserForWrongLines() {
    DefaultParser parser = new SqlLineParser(new SqlLine())
        .eofOnUnclosedQuote(true)
        .eofOnEscapedNewLine(true);
    Parser.ParseContext acceptLine = Parser.ParseContext.ACCEPT_LINE;
    String[] successfulLinesToCheck = {
        "!sql",
        "   !all",
        " \n select",
        " \n test ",
        "  test ';",
        " \n test ';'\";",
    };
    for (String line : successfulLinesToCheck) {
      try {
        parser.parse(line, line.length(), acceptLine);
        Assert.fail("Missing closing quote or semicolon for line " + line);
      } catch (EOFError eofError) {
        //ok
      }
    }
  }
}

// End SqlLineParserTest.java
