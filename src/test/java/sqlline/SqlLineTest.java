/*
 *  Copyright (c) 2010-2010 The Eigenbase Project
 *  All rights reserved.
 *
 *
 *  Redistribution and use in source and binary forms,
 *  with or without modification, are permitted provided
 *  that the following conditions are met:
 *
 *  Redistributions of source code must retain the above
 *  copyright notice, this list of conditions and the following
 *  disclaimer.
 *  Redistributions in binary form must reproduce the above
 *  copyright notice, this list of conditions and the following
 *  disclaimer in the documentation and/or other materials
 *  provided with the distribution.
 *  Neither the name of the <ORGANIZATION> nor the names
 *  of its contributors may be used to endorse or promote
 *  products derived from this software without specific
 *  prior written permission.
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS
 *  AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 *  GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 *  BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 *  OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
 *  IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 *  ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *  This software is hosted by SourceForge.
 *  SourceForge is a trademark of VA Linux Systems, Inc.
 */
package sqlline;

import junit.framework.TestCase;

/**
 * Test cases for SqlLine.
 */
public class SqlLineTest extends TestCase
{
    /**
     * Public constructor.
     */
    public SqlLineTest()
    {
    }

    /**
     * Public constructor with test name, required by junit.
     *
     * @param testName Test name
     */
    public SqlLineTest(String testName)
    {
        super(testName);
    }

    /**
     * Unit test for {@link sqlline.SqlLine#splitCompound(String)}.
     */
    public void testSplitCompound()
    {
        final SqlLine line = new SqlLine();
        String[][] strings;

        // simple line
        strings = line.splitCompound("abc de  fgh");
        assertEquals(new String[][] {{"ABC"}, {"DE"}, {"FGH"}}, strings);

        // line with double quotes
        strings = line.splitCompound("abc \"de fgh\" ijk");
        assertEquals(new String[][] {{"ABC"}, {"de fgh"}, {"IJK"}}, strings);

        // line with double quotes as first and last
        strings = line.splitCompound("\"abc de\"  fgh \"ijk\"");
        assertEquals(new String[][] {{"abc de"}, {"FGH"}, {"ijk"}}, strings);

        // escaped double quotes, and dots inside quoted identifiers
        strings = line.splitCompound("\"ab.c \"\"de\"  fgh.ij");
        assertEquals(new String[][] {{"ab.c \"de"}, {"FGH", "IJ"}}, strings);

        // single quotes do not affect parsing
        strings = line.splitCompound("'abc de'  fgh");
        assertEquals(new String[][] {{"'ABC"}, {"DE'"}, {"FGH"}}, strings);

        // incomplete double-quoted identifiers are implicitly completed
        strings = line.splitCompound("abcdefgh   \"ijk");
        assertEquals(new String[][] {{"ABCDEFGH"}, {"ijk"}}, strings);

        // dot at start of line is illegal, but we are lenient and ignore it
        strings = line.splitCompound(".abc def.gh");
        assertEquals(new String[][] {{"ABC"}, {"DEF", "GH"}}, strings);

        // spaces around dots are fine
        strings = line.splitCompound("abc de .  gh .i. j");
        assertEquals(new String[][] {{"ABC"}, {"DE", "GH", "I", "J"}}, strings);

        // double-quote inside an unquoted identifier is treated like a regular
        // character; should be an error, but we are lenient
        strings = line.splitCompound("abc\"de \"fg\"");
        assertEquals(new String[][] {{"ABC\"DE"}, {"fg"}}, strings);

        // null value only if unquoted
        strings = line.splitCompound("abc null");
        assertEquals(new String[][] {{"ABC"}, {null}}, strings);
        strings = line.splitCompound("abc foo.null.bar");
        assertEquals(new String[][] {{"ABC"}, {"FOO", null, "BAR"}}, strings);
        strings = line.splitCompound("abc foo.\"null\".bar");
        assertEquals(new String[][] {{"ABC"}, {"FOO", "null", "BAR"}}, strings);
        strings = line.splitCompound("abc foo.\"NULL\".bar");
        assertEquals(new String[][] {{"ABC"}, {"FOO", "NULL", "BAR"}}, strings);

        // trim trailing whitespace and semicolon
        strings = line.splitCompound("abc ;\t     ");
        assertEquals(new String[][] {{"ABC"}}, strings);
        // keep semicolon inside line
        strings = line.splitCompound("abc\t;def");
        assertEquals(new String[][] {{"ABC"}, {";DEF"}}, strings);
    }

    void assertEquals(String[][] expectedses, String[][] actualses)
    {
        assertEquals(expectedses.length, actualses.length);
        for (int i = 0; i < expectedses.length; ++i) {
            String[] expecteds = expectedses[i];
            String[] actuals = actualses[i];
            assertEquals(expecteds.length, actuals.length);
            for (int j = 0; j < expecteds.length; ++j) {
                String expected = expecteds[j];
                String actual = actuals[j];
                assertEquals(expected, actual);
            }
        }
    }
}

// End SqlLineTest.java
