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

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static sqlline.SqlLineArgsTest.begin;

/**
 * Test cases for prompt and right prompt.
 */
public class PromptTest {
  private static final String DEV_NULL = "/dev/null";
  private SqlLine sqlLine;

  @BeforeEach
  private void init() {
    sqlLine = new SqlLine();
    sqlLine.getOpts().setPropertiesFile(DEV_NULL);
  }

  @AfterEach
  private void finish() {
    sqlLine.setExit(true);
  }

  @Test
  public void testPromptWithoutConnection() {
    // default prompt
    assertThat(sqlLine.getPromptHandler().getPrompt().toAnsi(),
        is(BuiltInProperty.PROMPT.defaultValue()));
    assertThat(sqlLine.getPromptHandler().getRightPrompt().toAnsi(),
        is(BuiltInProperty.RIGHT_PROMPT.defaultValue()));

    // custom constant prompt
    final SqlLineOpts opts = sqlLine.getOpts();
    opts.set(BuiltInProperty.PROMPT, "custom_prompt");
    opts.set(BuiltInProperty.RIGHT_PROMPT, "custom_right_prompt");
    assertThat(sqlLine.getPromptHandler().getPrompt(),
        is(new AttributedString("custom_prompt")));
    assertThat(sqlLine.getPromptHandler().getRightPrompt(),
        is(new AttributedString("custom_right_prompt")));

    // custom constant multiline prompt
    opts.set(BuiltInProperty.PROMPT, "custom\nprompt\n");
    opts.set(BuiltInProperty.RIGHT_PROMPT, "custom\nright\nprompt\n");
    assertThat(sqlLine.getPromptHandler().getPrompt(),
        is(new AttributedString("custom\nprompt\n")));
    assertThat(sqlLine.getPromptHandler().getRightPrompt(),
        is(new AttributedString("custom\nright\nprompt\n")));

    // custom constant colored prompt
    opts.set(BuiltInProperty.PROMPT, "%[f:y%]sqlline%[default%]>>");
    opts.set(BuiltInProperty.RIGHT_PROMPT, "%[bold%]end");
    AttributedStringBuilder promptBuilder = new AttributedStringBuilder();
    promptBuilder.append("sqlline", AttributedStyles.YELLOW).append(">>");
    AttributedStringBuilder rightPromptBuilder = new AttributedStringBuilder();
    rightPromptBuilder.append("end", AttributedStyle.BOLD);
    assertThat(sqlLine.getPromptHandler().getPrompt(),
        is(promptBuilder.toAttributedString()));
    assertThat(sqlLine.getPromptHandler().getRightPrompt(),
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
    assertThat(sqlLine.getPromptHandler().getPrompt(),
        is(promptBuilder.toAttributedString()));
    assertThat(sqlLine.getPromptHandler().getRightPrompt(),
        is(rightPromptBuilder.toAttributedString()));

    // custom prompt with date values
    opts.set(BuiltInProperty.PROMPT, "%w_%W_%o_%O_%y_%Y>");
    opts.set(BuiltInProperty.RIGHT_PROMPT, "[%w_%W_%o_%O_%y_%Y]");
    final SimpleDateFormat sdf =
        new SimpleDateFormat("d_E_MM_MMM_YY_YYYY", Locale.ROOT);
    assertThat(sqlLine.getPromptHandler().getPrompt().toAnsi(),
        is(sdf.format(new Date()) + ">"));
    assertThat(sqlLine.getPromptHandler().getRightPrompt().toAnsi(),
        is("[" + sdf.format(new Date()) + "]"));

    // custom prompt with data from connection (empty if there is no connection)
    opts.set(BuiltInProperty.PROMPT, "%u%n");
    opts.set(BuiltInProperty.RIGHT_PROMPT, "%d%c");
    assertThat(sqlLine.getPromptHandler().getPrompt().toAnsi(), is(""));
    assertThat(sqlLine.getPromptHandler().getRightPrompt().toAnsi(), is(""));

    opts.set(BuiltInProperty.PROMPT, "%u%c>");
    opts.set(BuiltInProperty.RIGHT_PROMPT, "<<<%n%d");
    assertThat(sqlLine.getPromptHandler().getPrompt().toAnsi(), is(">"));
    assertThat(sqlLine.getPromptHandler().getRightPrompt().toAnsi(), is("<<<"));

    // custom prompt with property value
    opts.set(BuiltInProperty.PROMPT, "%:color:>");
    opts.set(BuiltInProperty.RIGHT_PROMPT, "%:outputformat:");
    assertThat(sqlLine.getPromptHandler().getPrompt().toAnsi(),
        is(opts.getColor() + ">"));
    assertThat(sqlLine.getPromptHandler().getRightPrompt().toAnsi(),
        is(opts.getOutputFormat()));
  }

  @Test
  public void testPromptWithNickname() {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    try {
      final SqlLine.Status status =
          begin(sqlLine, os, false, "-e", "!set maxwidth 80");
      assertThat(status, equalTo(SqlLine.Status.OK));
      final DispatchCallback dc = new DispatchCallback();
      sqlLine.runCommands(dc, "!connect "
          + SqlLineArgsTest.ConnectionSpec.H2.url + " "
          + SqlLineArgsTest.ConnectionSpec.H2.username + " \"\"");

      // custom prompt with data from connection
      final SqlLineOpts opts = sqlLine.getOpts();
      sqlLine.getDatabaseConnection().setNickname("nickname");
      opts.set(BuiltInProperty.PROMPT, "%u@%n>");
      opts.set(BuiltInProperty.RIGHT_PROMPT, "//%d%c");
      // if nickname is specified for the connection
      // it has more priority than prompt.
      // Right prompt does not care about nickname
      assertThat(sqlLine.getPromptHandler().getPrompt().toAnsi(),
          is("0: nickname> "));
      assertThat(sqlLine.getPromptHandler().getRightPrompt().toAnsi(),
          is("//H20"));
      sqlLine.getDatabaseConnection().close();
    } catch (Exception e) {
      // fail
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testPromptWithConnection() {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    try {
      final SqlLine.Status status =
          begin(sqlLine, os, false, "-e", "!set maxwidth 80");
      assertThat(status, equalTo(SqlLine.Status.OK));
      final DispatchCallback dc = new DispatchCallback();
      sqlLine.runCommands(dc, "!connect "
          + SqlLineArgsTest.ConnectionSpec.H2.url + " "
          + SqlLineArgsTest.ConnectionSpec.H2.username + " \"\"");

      // custom prompt with data from connection
      final SqlLineOpts opts = sqlLine.getOpts();
      opts.set(BuiltInProperty.PROMPT, "%u@%n>");
      opts.set(BuiltInProperty.RIGHT_PROMPT, "//%d%c");
      assertThat(sqlLine.getPromptHandler().getPrompt().toAnsi(),
          is("jdbc:h2:mem:@SA>"));
      assertThat(sqlLine.getPromptHandler().getRightPrompt().toAnsi(),
          is("//H20"));
      sqlLine.getDatabaseConnection().close();
    } catch (Exception e) {
      // fail
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testPromptWithSchema() {
    sqlLine.getOpts().set(BuiltInProperty.PROMPT, "%u%S>");

    sqlLine.runCommands(new DispatchCallback(),
        "!connect "
            + SqlLineArgsTest.ConnectionSpec.H2.url + " "
            + SqlLineArgsTest.ConnectionSpec.H2.username + " \"\"");

    assertThat(sqlLine.getPromptHandler().getPrompt().toAnsi(),
        is("jdbc:h2:mem:PUBLIC>"));

    sqlLine.runCommands(new DispatchCallback(), "use information_schema");

    assertThat(sqlLine.getPromptHandler().getPrompt().toAnsi(),
        is("jdbc:h2:mem:INFORMATION_SCHEMA>"));

    sqlLine.getDatabaseConnection().close();
  }

  @Test
  public void testPromptScript() {
    sqlLine.getOpts().set(BuiltInProperty.PROMPT_SCRIPT, "'hel' + 'lo'");

    sqlLine.runCommands(new DispatchCallback(),
        "!connect "
            + SqlLineArgsTest.ConnectionSpec.H2.url + " "
            + SqlLineArgsTest.ConnectionSpec.H2.username + " \"\"");
    assertThat(sqlLine.getPromptHandler().getPrompt().toAnsi(),
        is("hello"));

    sqlLine.getOpts().set(BuiltInProperty.PROMPT_SCRIPT, ""
        + "'i=' + connectionIndex + ',p=' + databaseProductName +"
        + " ',n=' + userName + ',u=' + url + ',s=' + currentSchema + '>'");
    assertThat(sqlLine.getPromptHandler().getPrompt().toAnsi(),
        is("i=0,p=H2,n=SA,u=jdbc:h2:mem:,s=PUBLIC>"));

    sqlLine.getDatabaseConnection().close();
  }

  @Test
  public void testCustomPromptHandler() {
    sqlLine.runCommands(new DispatchCallback(),
        "!connect "
            + SqlLineArgsTest.ConnectionSpec.H2.url + " "
            + SqlLineArgsTest.ConnectionSpec.H2.username + " \"\"",
        "!prompthandler sqlline.extensions.CustomPromptHandler");

    assertThat(sqlLine.getPromptHandler().getPrompt().toAnsi(),
        is("my_app (PUBLIC)>"));

    sqlLine.runCommands(new DispatchCallback(), "!prompthandler default");

    assertThat(sqlLine.getPromptHandler().getPrompt().toAnsi(),
        is("0: jdbc:h2:mem:> "));

    sqlLine.getDatabaseConnection().close();
  }

}

// End PromptTest.java
