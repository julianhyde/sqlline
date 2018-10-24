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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test cases for Rows.
 */
public class RowsTest {
  @Test
  public void testEscapeControlSymbols() {
    // empty string
    assertEquals("", Rows.escapeControlSymbols(""));
    // one symbol
    assertEquals("\\u0000", Rows.escapeControlSymbols("\u0000"));
    assertEquals("\\u001F", Rows.escapeControlSymbols("\u001F"));
    assertEquals("\\n", Rows.escapeControlSymbols("\n"));
    assertEquals("\\t", Rows.escapeControlSymbols("\t"));
    assertEquals("\\b", Rows.escapeControlSymbols("\b"));
    assertEquals("\\f", Rows.escapeControlSymbols("\f"));
    assertEquals("\\r", Rows.escapeControlSymbols("\r"));

    // several control symbols
    assertEquals("\\n\\n\\n", Rows.escapeControlSymbols("\n\n\n"));
    assertEquals("\\t\\n\\b\\f\\r", Rows.escapeControlSymbols("\t\n\b\f\r"));
    assertEquals("str\\tstr2\\nstr3\\bstr4\\fstr5\\rstr6",
        Rows.escapeControlSymbols("str\tstr2\nstr3\bstr4\fstr5\rstr6"));

    // including spaces
    assertEquals("  \\n  ", Rows.escapeControlSymbols("  \n  "));
    assertEquals("    \\n", Rows.escapeControlSymbols("    \n"));
    assertEquals("\\b text  \\n", Rows.escapeControlSymbols("\b text  \n"));
    assertEquals("\\t text", Rows.escapeControlSymbols("\t text"));
    assertEquals("  text  \\t", Rows.escapeControlSymbols("  text  \t"));
    assertEquals("str \\tstr2 \\nstr3\\b str4\\f str5\\r str6 ",
        Rows.escapeControlSymbols("str \tstr2 \nstr3\b str4\f str5\r str6 "));

    // non control symbols should not be escaped
    assertEquals("\"", Rows.escapeControlSymbols("\""));
    assertEquals("\\\"", Rows.escapeControlSymbols("\\\""));
    assertEquals("\\", Rows.escapeControlSymbols("\\"));
    assertEquals("\\\\", Rows.escapeControlSymbols("\\\\"));
  }
}

// End RowsTest.java
