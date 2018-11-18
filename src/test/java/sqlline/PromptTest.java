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

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Test cases for prompt and right prompt.
 */
public class PromptTest {
  @Test
  public void testPromptWithoutConnection() {
    SqlLine sqlLine = new SqlLine();
    // default prompt
    assertThat(Prompt.getPrompt(sqlLine).toAnsi(),
        is(BuiltInProperty.PROMPT.defaultValue()));
    assertThat(Prompt.getRightPrompt(sqlLine).toAnsi(),
        is(BuiltInProperty.RIGHT_PROMPT.defaultValue()));

    // custom constant prompt
    final SqlLineOpts opts = sqlLine.getOpts();
    opts.set(BuiltInProperty.PROMPT, "custom_prompt");
    opts.set(BuiltInProperty.RIGHT_PROMPT, "custom_right_prompt");
    assertThat(Prompt.getPrompt(sqlLine),
        is(new AttributedString("custom_prompt")));
    assertThat(Prompt.getRightPrompt(sqlLine),
        is(new AttributedString("custom_right_prompt")));

    // custom constant multiline prompt
    opts.set(BuiltInProperty.PROMPT, "custom\nprompt\n");
    opts.set(BuiltInProperty.RIGHT_PROMPT, "custom\nright\nprompt\n");
    assertThat(Prompt.getPrompt(sqlLine),
        is(new AttributedString("custom\nprompt\n")));
    assertThat(Prompt.getRightPrompt(sqlLine),
        is(new AttributedString("custom\nright\nprompt\n")));

    // custom constant colored prompt
    opts.set(BuiltInProperty.PROMPT, "%[f:y%]sqlline%[default%]>>");
    opts.set(BuiltInProperty.RIGHT_PROMPT, "%[bold%]end");
    AttributedStringBuilder promptBuilder = new AttributedStringBuilder();
    promptBuilder.append("sqlline", AttributedStyles.YELLOW).append(">>");
    AttributedStringBuilder rightPromptBuilder = new AttributedStringBuilder();
    rightPromptBuilder.append("end", AttributedStyle.BOLD);
    assertThat(Prompt.getPrompt(sqlLine),
        is(promptBuilder.toAttributedString()));
    assertThat(Prompt.getRightPrompt(sqlLine),
        is(rightPromptBuilder.toAttributedString()));

    opts.set(BuiltInProperty.PROMPT, "%[f:b,italic%]sqlline%[default%]>>");
    promptBuilder = new AttributedStringBuilder();
    promptBuilder.append("sqlline", AttributedStyles.ITALIC_BLUE);
    promptBuilder.append(">>");
    opts.set(BuiltInProperty.RIGHT_PROMPT, "[%[underline,f:m%]end%[default%]]");
    rightPromptBuilder = new AttributedStringBuilder();
    rightPromptBuilder.append("[");
    rightPromptBuilder
        .append("end",
            AttributedStyle.DEFAULT
                .foreground(AttributedStyle.MAGENTA).underline());
    rightPromptBuilder.append("]");
    assertThat(Prompt.getPrompt(sqlLine),
        is(promptBuilder.toAttributedString()));
    assertThat(Prompt.getRightPrompt(sqlLine),
        is(rightPromptBuilder.toAttributedString()));

    // custom prompt with date values
    opts.set(BuiltInProperty.PROMPT, "%w_%W_%o_%O_%y_%Y>");
    opts.set(BuiltInProperty.RIGHT_PROMPT, "[%w_%W_%o_%O_%y_%Y]");
    final SimpleDateFormat sdf =
        new SimpleDateFormat("d_E_MM_MMM_YY_YYYY", Locale.ROOT);
    assertThat(Prompt.getPrompt(sqlLine).toAnsi(),
        is(sdf.format(new Date()) + ">"));
    assertThat(Prompt.getRightPrompt(sqlLine).toAnsi(),
        is("[" + sdf.format(new Date()) + "]"));

    // custom prompt with data from connection (empty if there is no connection)
    opts.set(BuiltInProperty.PROMPT, "%u%n");
    opts.set(BuiltInProperty.RIGHT_PROMPT, "%d%c");
    assertThat(Prompt.getPrompt(sqlLine).toAnsi(), is(""));
    assertThat(Prompt.getRightPrompt(sqlLine).toAnsi(), is(""));

    opts.set(BuiltInProperty.PROMPT, "%u%c>");
    opts.set(BuiltInProperty.RIGHT_PROMPT, "<<<%n%d");
    assertThat(Prompt.getPrompt(sqlLine).toAnsi(), is(">"));
    assertThat(Prompt.getRightPrompt(sqlLine).toAnsi(), is("<<<"));

    // custom prompt with property value
    opts.set(BuiltInProperty.PROMPT, "%:color:>");
    opts.set(BuiltInProperty.RIGHT_PROMPT, "%:outputformat:");
    assertThat(Prompt.getPrompt(sqlLine).toAnsi(), is(opts.getColor() + ">"));
    assertThat(Prompt.getRightPrompt(sqlLine).toAnsi(),
        is(opts.getOutputFormat()));
  }

  @Test
  public void testPromptWithNickname() {
    SqlLine sqlLine = new SqlLine();
    DispatchCallback dc = new DispatchCallback();
    sqlLine.runCommands(
        Collections.singletonList("!connect "
            + SqlLineArgsTest.ConnectionSpec.H2.url + " "
            + SqlLineArgsTest.ConnectionSpec.H2.username + " \"\""),
        dc);

    // custom prompt with data from connection
    final SqlLineOpts opts = sqlLine.getOpts();
    sqlLine.getDatabaseConnection().setNickname("nickname");
    opts.set(BuiltInProperty.PROMPT, "%u@%n>");
    opts.set(BuiltInProperty.RIGHT_PROMPT, "//%d%c");
    // if nickname is specified for the connection
    // it has more priority than prompt.
    // Right prompt does not care about nickname
    assertThat(Prompt.getPrompt(sqlLine).toAnsi(), is("0: nickname> "));
    assertThat(Prompt.getRightPrompt(sqlLine).toAnsi(), is("//H20"));
  }

  @Test
  public void testPromptWithConnection() {
    SqlLine sqlLine = new SqlLine();
    DispatchCallback dc = new DispatchCallback();
    sqlLine.runCommands(
        Collections.singletonList("!connect "
            + SqlLineArgsTest.ConnectionSpec.H2.url + " "
            + SqlLineArgsTest.ConnectionSpec.H2.username + " \"\""),
        dc);

    // custom prompt with data from connection
    final SqlLineOpts opts = sqlLine.getOpts();
    opts.set(BuiltInProperty.PROMPT, "%u@%n>");
    opts.set(BuiltInProperty.RIGHT_PROMPT, "//%d%c");
    assertThat(Prompt.getPrompt(sqlLine).toAnsi(), is("jdbc:h2:mem:@SA>"));
    assertThat(Prompt.getRightPrompt(sqlLine).toAnsi(), is("//H20"));
    sqlLine.getDatabaseConnection().close();
  }
}

// End PromptTest.java
