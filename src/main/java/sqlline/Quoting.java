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

/**
 * Quoting strategy.
 */
class Quoting {
  final char start;
  final char end;
  final boolean upper;

  Quoting(char start, char end, boolean upper) {
    this.start = start;
    this.end = end;
    this.upper = upper;
  }

  public static final Quoting DEFAULT = new Quoting('"', '"', true);
}

// End Quoting.java
