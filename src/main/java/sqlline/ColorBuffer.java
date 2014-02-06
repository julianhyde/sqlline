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
  private static final ColorAttr BOLD = new ColorAttr("\033[1m");
  private static final ColorAttr NORMAL = new ColorAttr("\033[m");
  private static final ColorAttr REVERS = new ColorAttr("\033[7m");
  private static final ColorAttr LINED = new ColorAttr("\033[4m");
  private static final ColorAttr GREY = new ColorAttr("\033[1;30m");
  private static final ColorAttr RED = new ColorAttr("\033[1;31m");
  private static final ColorAttr GREEN = new ColorAttr("\033[1;32m");
  private static final ColorAttr BLUE = new ColorAttr("\033[1;34m");
  private static final ColorAttr CYAN = new ColorAttr("\033[1;36m");
  private static final ColorAttr YELLOW = new ColorAttr("\033[1;33m");
  private static final ColorAttr MAGENTA = new ColorAttr("\033[1;35m");
  private static final ColorAttr INVISIBLE = new ColorAttr("\033[8m");

  private final List<Object> parts = new LinkedList<Object>();

  private final boolean useColor;

  public ColorBuffer(boolean useColor) {
    this.useColor = useColor;
    append("");
  }

  public ColorBuffer(String str, boolean useColor) {
    this.useColor = useColor;
    append(str);
  }

  /**
   * Pad the specified String with spaces to the indicated length
   *
   * @param str
   *          the String to pad
   * @param len
   *          the length we want the return String to be
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
    StringBuilder buf = new StringBuilder(str);
    while (buf.length() < len) {
      buf.append(" ");

      if (buf.length() < len) {
        buf.insert(0, " ");
      }
    }
    return append(buf.toString());
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
    if (lastAttr != null && lastAttr != NORMAL) {
      cbuff.append(NORMAL);
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
    parts.add(NORMAL);
    return this;
  }

  public ColorBuffer bold(String str) {
    return append(BOLD, str);
  }

  public ColorBuffer lined(String str) {
    return append(LINED, str);
  }

  public ColorBuffer grey(String str) {
    return append(GREY, str);
  }

  public ColorBuffer red(String str) {
    return append(RED, str);
  }

  public ColorBuffer blue(String str) {
    return append(BLUE, str);
  }

  public ColorBuffer green(String str) {
    return append(GREEN, str);
  }

  public ColorBuffer cyan(String str) {
    return append(CYAN, str);
  }

  public ColorBuffer yellow(String str) {
    return append(YELLOW, str);
  }

  public ColorBuffer magenta(String str) {
    return append(MAGENTA, str);
  }

  public int compareTo(Object other) {
    return getMono().compareTo(((ColorBuffer) other).getMono());
  }

  /** Style attribute. */
  private static class ColorAttr {
    private final String attr;

    public ColorAttr(String attr) {
      this.attr = attr;
    }

    public String toString() {
      return attr;
    }
  }
}

// End ColorBuffer.java
