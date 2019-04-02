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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * A buffer that can output segments using ANSI color.
 */
final class ColorBuffer implements Comparable {
  /** Style attribute. */
  enum ColorAttr {
    BOLD("\033[1m"),
    NORMAL("\033[m"),
    REVERS("\033[7m"),
    LINED("\033[4m"),
    GREY("\033[1;30m"),
    RED("\033[1;31m"),
    GREEN("\033[1;32m"),
    BLUE("\033[1;34m"),
    CYAN("\033[1;36m"),
    YELLOW("\033[1;33m"),
    MAGENTA("\033[1;35m"),
    INVISIBLE("\033[8m");

    private final String style;

    ColorAttr(String style) {
      this.style = style;
    }

    @Override public String toString() {
      return style;
    }
  }

  private final List<Object> parts = new LinkedList<>();

  private final boolean useColor;

  ColorBuffer(boolean useColor) {
    this.useColor = useColor;
    append("");
  }

  ColorBuffer(String str, boolean useColor) {
    this.useColor = useColor;
    append(str);
  }

  /**
   * Pad the specified String with spaces to the indicated length
   *
   * @param str The String to pad
   * @param len The length we want the return String to be
   * @return the passed in String with spaces appended until the
   *         length matches the specified length.
   */
  ColorBuffer pad(ColorBuffer str, int len) {
    int n = str.getVisibleLength();
    while (n < len) {
      str.append(" ");
      n++;
    }

    return append(str);
  }

  ColorBuffer center(String str, int len) {
    return append(centerString(str, len));
  }

  static String centerString(String str, int len) {
    final int n = len - str.length();
    if (n <= 0) {
      return str;
    }
    final StringBuilder buf = new StringBuilder();
    final int left = n / 2;
    final int right = n - left;
    for (int i = 0; i < left; i++) {
      buf.append(' ');
    }
    buf.append(str);
    for (int i = 0; i < right; i++) {
      buf.append(' ');
    }
    return buf.toString();
  }

  ColorBuffer pad(String str, int len) {
    if (str == null) {
      str = "";
    }

    return pad(new ColorBuffer(str, false), len);
  }

  public String getColor() {
    return getBuffer(useColor);
  }

  public String getMono() {
    return getBuffer(false);
  }

  String getBuffer(boolean color) {
    StringBuilder buf = new StringBuilder();
    for (Object part : parts) {
      if (!color && part instanceof ColorAttr) {
        continue;
      }
      buf.append(part.toString());
    }
    return buf.toString();
  }

  /**
   * Truncate the ColorBuffer to the specified length and return
   * the new ColorBuffer. Any open color tags will be closed.
   */
  public ColorBuffer truncate(int len) {
    ColorBuffer cbuff = new ColorBuffer(useColor);
    ColorAttr lastAttr = null;
    for (Iterator<Object> i = parts.iterator();
        cbuff.getVisibleLength() < len && i.hasNext();) {
      Object next = i.next();
      if (next instanceof ColorAttr) {
        lastAttr = (ColorAttr) next;
        cbuff.append((ColorAttr) next);
        continue;
      }

      String val = next.toString();
      if (cbuff.getVisibleLength() + val.length() > len) {
        int partLen = len - cbuff.getVisibleLength();
        val = val.substring(0, partLen);
      }

      cbuff.append(val);
    }

    // close off the buffer with a normal tag
    if (lastAttr != null && lastAttr != ColorAttr.NORMAL) {
      cbuff.append(ColorAttr.NORMAL);
    }

    return cbuff;
  }

  public String toString() {
    return getColor();
  }

  public ColorBuffer append(String str) {
    parts.add(str);
    return this;
  }

  public ColorBuffer append(ColorBuffer buf) {
    parts.addAll(buf.parts);
    return this;
  }

  public ColorBuffer append(ColorAttr attr) {
    parts.add(attr);
    return this;
  }

  public int getVisibleLength() {
    return getMono().length();
  }

  public ColorBuffer append(ColorAttr attr, String val) {
    parts.add(attr);
    parts.add(val);
    parts.add(ColorAttr.NORMAL);
    return this;
  }

  public ColorBuffer bold(String str) {
    return append(ColorAttr.BOLD, str);
  }

  public ColorBuffer lined(String str) {
    return append(ColorAttr.LINED, str);
  }

  public ColorBuffer grey(String str) {
    return append(ColorAttr.GREY, str);
  }

  public ColorBuffer red(String str) {
    return append(ColorAttr.RED, str);
  }

  public ColorBuffer blue(String str) {
    return append(ColorAttr.BLUE, str);
  }

  public ColorBuffer green(String str) {
    return append(ColorAttr.GREEN, str);
  }

  public ColorBuffer cyan(String str) {
    return append(ColorAttr.CYAN, str);
  }

  public ColorBuffer yellow(String str) {
    return append(ColorAttr.YELLOW, str);
  }

  public ColorBuffer magenta(String str) {
    return append(ColorAttr.MAGENTA, str);
  }

  public int compareTo(Object other) {
    return getMono().compareTo(((ColorBuffer) other).getMono());
  }
}

// End ColorBuffer.java
