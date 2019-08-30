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
import java.util.stream.Stream;

import org.h2.result.RowImpl;
import org.h2.tools.SimpleResultSet;
import org.h2.value.Value;
import org.h2.value.ValueInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import mockit.internal.reflection.FieldReflection;

import static org.junit.jupiter.params.provider.Arguments.of;

/**
 * Test cases for BufferedRows.
 */
public class BufferedRowsTest {
  @ParameterizedTest
  @MethodSource("sizeOfBatchAndLimitProvider")
  public void nextListTest(int buffer, int size) {
    try {
      SimpleResultSet rs = new SimpleResultSet();
      for (int i = 0; i < size; i++) {
        rs.addRow(
            new RowImpl(new Value[] {ValueInt.get(1), ValueInt.get(2)}, 123));
      }

      SqlLine sqlLine = getSqlLine();
      sqlLine.getOpts().set(BuiltInProperty.INCREMENTAL_BUFFER_ROWS, buffer);
      BufferedRows bufferedRows = new BufferedRows(sqlLine, rs);
      while (bufferedRows.hasNext()) {
        bufferedRows.next();
      }
      final int batchValue = FieldReflection.<Integer>getFieldValue(
          bufferedRows.getClass().getDeclaredField("batch"),
          bufferedRows) - 1;
      Assertions.assertEquals(
          buffer == 0 ? size : size / buffer + (size % buffer == 0 ? 0 : 1),
          batchValue);
    } catch (Exception e) {
      // fail
      throw new RuntimeException(e);
    }
  }

  private static Stream<Arguments> sizeOfBatchAndLimitProvider() {
    return Stream.of(of(0, 10), of(1, 10), of(2, 10), of(3, 10), of(4, 10),
        of(5, 10), of(6, 10), of(7, 10), of(8, 10), of(9, 10), of(10, 10),
        of(11, 10), of(12, 10));
  }

  static SqlLine getSqlLine() throws IOException {
    SqlLine sqlLine = new SqlLine();
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    PrintStream sqllineOutputStream =
        new PrintStream(os, false, StandardCharsets.UTF_8.name());
    sqlLine.setOutputStream(sqllineOutputStream);
    sqlLine.setErrorStream(sqllineOutputStream);
    final InputStream is = new ByteArrayInputStream(new byte[0]);
    sqlLine.begin(new String[] {"-e", "!set maxwidth 80"}, is, false);
    return sqlLine;
  }
}

// End BufferedRowsTest.java
