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

import java.util.ArrayList;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test cases for reflector.
 */
public class ReflectorTest {
  @Test
  public void testInvoke() {
    try {
      final Reflector reflector = new Reflector(new SqlLine());

      // wrong method name
      assertThrows(IllegalArgumentException.class,
          () -> reflector.invoke(
              new ArrayList<>(),
              ArrayList.class,
              "getTables",
              Collections.emptyList()));
      // wrong number of arguments
      assertThrows(IllegalArgumentException.class,
          () -> reflector.invoke(
              new ArrayList<>(),
              ArrayList.class,
              "add",
              Collections.emptyList()));

      // should not fail
      reflector.invoke(
          new ArrayList<>(),
          ArrayList.class,
          "add",
          Collections.singletonList("test"));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}

// End ReflectorTest.java
