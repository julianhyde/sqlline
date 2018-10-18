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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Test cases for Rows.
 */
public class RowsTest {
  @Test
  public void testEscapeControlSymbols() {
    // empty string
    assertThat(Rows.escapeControlSymbols(""), is(""));
    // one symbol
    assertThat(Rows.escapeControlSymbols("\u0000"), is("\\u0000"));
    assertThat(Rows.escapeControlSymbols("\u001F"), is("\\u001F"));
    assertThat(Rows.escapeControlSymbols("\n"), is("\\n"));
    assertThat(Rows.escapeControlSymbols("\t"), is("\\t"));
    assertThat(Rows.escapeControlSymbols("\b"), is("\\b"));
    assertThat(Rows.escapeControlSymbols("\f"), is("\\f"));
    assertThat(Rows.escapeControlSymbols("\r"), is("\\r"));

    // several control symbols
    assertThat(Rows.escapeControlSymbols("\n\n\n"), is("\\n\\n\\n"));
    assertThat(Rows.escapeControlSymbols("\t\n\b\f\r"), is("\\t\\n\\b\\f\\r"));
    assertThat(Rows.escapeControlSymbols("str\tstr2\nstr3\bstr4\fstr5\rstr6"),
        is("str\\tstr2\\nstr3\\bstr4\\fstr5\\rstr6"));

    // including spaces
    assertThat(Rows.escapeControlSymbols("  \n  "), is("  \\n  "));
    assertThat(Rows.escapeControlSymbols("    \n"), is("    \\n"));
    assertThat(Rows.escapeControlSymbols("\b text  \n"), is("\\b text  \\n"));
    assertThat(Rows.escapeControlSymbols("\t text"), is("\\t text"));
    assertThat(Rows.escapeControlSymbols("  text  \t"), is("  text  \\t"));
    assertThat(
        Rows.escapeControlSymbols("str \tstr2 \nstr3\b str4\f str5\r str6 "),
        is("str \\tstr2 \\nstr3\\b str4\\f str5\\r str6 "));

    // non control symbols should not be escaped
    assertThat(Rows.escapeControlSymbols("\""), is("\""));
    assertThat(Rows.escapeControlSymbols("\\\""), is("\\\""));
    assertThat(Rows.escapeControlSymbols("\\"), is("\\"));
    assertThat(Rows.escapeControlSymbols("\\\\"), is("\\\\"));
  }
}

// End RowsTest.java
