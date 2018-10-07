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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.junit.Test;

import junit.framework.TestCase;

/**
 * SqlLineHighlighterTest.
 */
public class SqlLineHighlighterTest extends TestCase {
  @Test
  public void testHandleNumbers() {
    SqlLineHighlighter highlighter = new SqlLineHighlighter(new SqlLine());
    String inputString = "select 1+1, 2*2, 3-1, 1/1 from dual where 0=0";
    BitSet expectedBitSet = new BitSet(inputString.length());
    expectedBitSet.set(7);
    expectedBitSet.set(9);
    expectedBitSet.set(12);
    expectedBitSet.set(14);
    expectedBitSet.set(17);
    expectedBitSet.set(19);
    expectedBitSet.set(22);
    expectedBitSet.set(24);
    expectedBitSet.set(42);
    expectedBitSet.set(44);

    BitSet numberBitSet = new BitSet(inputString.length());
    for (int i = 0; i < inputString.length(); i++) {
      if (Character.isDigit(inputString.charAt(i))) {
        i = highlighter.handleNumbers(inputString, numberBitSet, i);
      }
    }
    assertEquals(expectedBitSet, numberBitSet);
  }

  @Test
  public void testHandleKeyWords() {
    SqlLineHighlighter highlighter = new SqlLineHighlighter(new SqlLine());
    String inputString = "select 1+1, 2*2, 3-1, 1/1 from dual where 0=0";
    BitSet expectedBitSet = new BitSet(inputString.length());
    expectedBitSet.set(7);
    expectedBitSet.set(9);
    expectedBitSet.set(12);
    expectedBitSet.set(14);
    expectedBitSet.set(17);
    expectedBitSet.set(19);
    expectedBitSet.set(22);
    expectedBitSet.set(24);
    expectedBitSet.set(42);
    expectedBitSet.set(44);

    BitSet numberBitSet = new BitSet(inputString.length());
    for (int i = 0; i < inputString.length(); i++) {
      if (Character.isDigit(inputString.charAt(i))) {
        i = highlighter.handleNumbers(inputString, numberBitSet, i);
      }
    }
    assertEquals(expectedBitSet, numberBitSet);
  }

  @Test
  public void testBoldStrings() throws IOException {
    SqlLine sqlLine = new SqlLine();
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    PrintStream sqllineOutputStream =
        new PrintStream(os, false, StandardCharsets.UTF_8.name());
    sqlLine.setOutputStream(sqllineOutputStream);
    sqlLine.setErrorStream(sqllineOutputStream);
    final InputStream is = new ByteArrayInputStream(new byte[0]);
    sqlLine.begin(new String[]{"-e", "!set maxwidth 80"}, is, false);
    SqlLineHighlighter sqlLineHighlighter = new SqlLineHighlighter(sqlLine);
    String[] stringsRequiredToBeBold = new String[] {
        "!set",
        "!commandhandler",
        "select",
        "values",
        "where",
        "join",
        "!connect"
    };
    for (String str: stringsRequiredToBeBold) {
      AttributedString stringSelect =
          sqlLineHighlighter.highlight(sqlLine.getLineReader(), str);
      for (int i = 0; i < str.length(); i++) {
        assertTrue("String '" + str + "' should be in bold",
            isBold(stringSelect.styleAt(i)));
      }
    }
  }

  @Test
  public void testNotBoldStrings() throws IOException {
    SqlLine sqlLine = new SqlLine();
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    PrintStream sqllineOutputStream =
        new PrintStream(os, false, StandardCharsets.UTF_8.name());
    sqlLine.setOutputStream(sqllineOutputStream);
    sqlLine.setErrorStream(sqllineOutputStream);
    final InputStream is = new ByteArrayInputStream(new byte[0]);
    sqlLine.begin(new String[]{"-e", "!set maxwidth 80"}, is, false);
    SqlLineHighlighter sqlLineHighlighter = new SqlLineHighlighter(sqlLine);
    String[] stringsRequiredToBeBold = new String[] {
        "!st",
        "!ohandler",
        "/* select */",
        "--values",
        "'where'",
        "\"join\""
    };
    for (String str: stringsRequiredToBeBold) {
      AttributedString stringSelect =
          sqlLineHighlighter.highlight(sqlLine.getLineReader(), str);
      for (int i = 0; i < str.length(); i++) {
        assertFalse("String '" + str + "' should be not in bold",
            isBold(stringSelect.styleAt(i)));
      }
    }
  }

  private boolean isBold(AttributedStyle style) {
    return style.getMask() % 2 != 0 && style.getStyle() % 2 != 0;
  }
}

// End SqlLineHighlighterTest.java
