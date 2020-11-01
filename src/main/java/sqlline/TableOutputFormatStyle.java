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

class TableOutputFormatStyle {
  private final char headerLine;
  private final char headerTopLeft;
  private final char headerTopRight;
  private final char headerCrossDown;
  private final char headerSeparator;
  private final char headerBodyCross;
  private final char headerBodyCrossLeft;
  private final char headerBodyCrossRight;
  private final char bodyLine;
  private final char bodySeparator;
  private final char bodyBottomLeft;
  private final char bodyBottomRight;
  private final char bodyCrossUp;

  TableOutputFormatStyle(char headerLine, char headerTopLeft,
      char headerTopRight, char headerCrossDown, char headerSeparator,
      char headerBodyCross, char headerBodyCrossLeft,
      char headerBodyCrossRight, char bodyLine, char bodySeparator,
      char bodyBottomLeft, char bodyBottomRight, char bodyCrossUp) {
    this.headerLine = headerLine;
    this.headerTopLeft = headerTopLeft;
    this.headerTopRight = headerTopRight;
    this.headerCrossDown = headerCrossDown;
    this.headerSeparator = headerSeparator;
    this.headerBodyCross = headerBodyCross;
    this.headerBodyCrossLeft = headerBodyCrossLeft;
    this.headerBodyCrossRight = headerBodyCrossRight;
    this.bodyLine = bodyLine;
    this.bodySeparator = bodySeparator;
    this.bodyBottomLeft = bodyBottomLeft;
    this.bodyBottomRight = bodyBottomRight;
    this.bodyCrossUp = bodyCrossUp;
  }

  public char getHeaderTopLeft() {
    return headerTopLeft;
  }

  public char getHeaderTopRight() {
    return headerTopRight;
  }

  public char getHeaderCrossDown() {
    return headerCrossDown;
  }

  public char getHeaderSeparator() {
    return headerSeparator;
  }

  public char getHeaderBodyCross() {
    return headerBodyCross;
  }

  public char getHeaderBodyCrossLeft() {
    return headerBodyCrossLeft;
  }

  public char getHeaderBodyCrossRight() {
    return headerBodyCrossRight;
  }

  public char getBodySeparator() {
    return bodySeparator;
  }

  public char getBodyBottomLeft() {
    return bodyBottomLeft;
  }

  public char getBodyBottomRight() {
    return bodyBottomRight;
  }

  public char getBodyCrossUp() {
    return bodyCrossUp;
  }

  public char getHeaderLine() {
    return headerLine;
  }

  public char getBodyLine() {
    return bodyLine;
  }

  public char getLine(boolean header) {
    return header ? getHeaderLine() : getBodyLine();
  }
}

// End TableOutputFormatStyle.java
