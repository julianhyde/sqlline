# SQLLine release history and change log

For a full list of releases, see <a href="https://github.com/julianhyde/sqlline/releases">github</a>.

## <a href="https://github.com/julianhyde/sqlline/releases/tag/sqlline-1.9.0">1.9.0</a> (2019-10-16)

There are several improvements regarding user interaction, such as
<a href="https://github.com/julianhyde/sqlline/issues/323">line numbers support</a>,
<a href="https://github.com/julianhyde/sqlline/issues/317">long query
editing which take more than a screen</a>,
and improvements for autocompletion
(<a href="https://github.com/julianhyde/sqlline/issues/331">autocompletion
for schema, table, column names</a>
and <a href="https://github.com/julianhyde/sqlline/issues/314">show
description for completion candidates</a>).

Now there is also a
<a href="https://github.com/julianhyde/sqlline/wiki/Demos">demo page</a>
for some of sqlline's features.

This release requires Java version 8 or higher.

Bugs and functional changes:

* Autocompletion improvements
  * [<a href="https://github.com/julianhyde/sqlline/issues/331">SQLLINE-331</a>]
    Autocompletion for schema, table, column names
  * [<a href="https://github.com/julianhyde/sqlline/issues/314">SQLLINE-314</a>]
    Description for command name completion candidates depending on
    `showCompletionDesc` property value
* [<a href="https://github.com/julianhyde/sqlline/issues/326">SQLLINE-326</a>]
  Respect `user`, `password` properties in connection url;
  add `connectInteractionMode` property
* [<a href="https://github.com/julianhyde/sqlline/issues/354">SQLLINE-354</a>]
  Use minimum possible `width` if calculated is negative
* [<a href="https://github.com/julianhyde/sqlline/issues/332">SQLLINE-332</a>]
  Add `historyFlags` property to customize `!history` output
* [<a href="https://github.com/julianhyde/sqlline/issues/343">SQLLINE-343</a>]
  Use batches accordingly
* [<a href="https://github.com/julianhyde/sqlline/issues/337">SQLLINE-337</a>]
  Move connection information to debug level of tracing
* Add `setReadOnly` connection option (iconeb)
* [<a href="https://github.com/julianhyde/sqlline/issues/248">SQLLINE-248</a>]
  `!dropall` for specific schemas
* [<a href="https://github.com/julianhyde/sqlline/issues/327">SQLLINE-327</a>]
  `!reconnect` should respect `fastconnect` property value
* [<a href="https://github.com/julianhyde/sqlline/issues/321">SQLLINE-321</a>]
  Ansiconsole outputformat
* [<a href="https://github.com/julianhyde/sqlline/issues/323">SQLLINE-323</a>]
  Add line numbers support
* [<a href="https://github.com/julianhyde/sqlline/issues/306">SQLLINE-306</a>]
  Turn off menu because of issue mentioned in #306
* [<a href="https://github.com/julianhyde/sqlline/issues/312">SQLLINE-312</a>]
  Show error if there are wrong arguments for `!metadata` methods
* [<a href="https://github.com/julianhyde/sqlline/issues/309">SQLLINE-309</a>]
  Fix highlighting of sqlline commands with quoted arguments
* [<a href="https://github.com/julianhyde/sqlline/issues/305">SQLLINE-305</a>]
  Posix compatibility for double quotes to allow spaces inside command arguments
* [<a href="https://github.com/julianhyde/sqlline/issues/310">SQLLINE-310</a>]
  `!schemas` command
* [<a href="https://github.com/julianhyde/sqlline/issues/318">SQLLINE-318</a>]
  Correct highlighting and line continuation for '`/*/`'
* [<a href="https://github.com/julianhyde/sqlline/issues/340">SQLLINE-340</a>]
  Rethrow `SQLException` from `DatabaseMetaDataHandler` if it is the cause

Other:
* [<a href="https://github.com/julianhyde/sqlline/issues/316">SQLLINE-316</a>]
  Link to demos page
* [<a href="https://github.com/julianhyde/sqlline/issues/302">SQLLINE-302</a>]
  Make tests run with their own `sqlline.properties` via `propertiesFile` property;
  remove all instantiations of `SqlLine` from `PromptTest`
* [<a href="https://github.com/julianhyde/sqlline/issues/210">SQLLINE-210</a>]
  Add property to skip doc generation
* Upgrades:
  * Update JLine3 to 3.12.1
  * Update Checkstyle to 8.23
  * Update Jmockit to 1.48
  * Update Junit to 5.5.1
  * Update maven-jar-plugin to 3.1.2
  * Update maven-surefire-plugin to 2.22.2
  * Update hsqldb up to 2.5.0
  * Update h2 up to 1.4.199

## <a href="https://github.com/julianhyde/sqlline/releases/tag/sqlline-1.8.0">1.8.0</a> (2019-05-30)

1.8 comes quite soon after 1.7, and the most significant change is
<a href="https://github.com/julianhyde/sqlline/issues/292">fixing Ctrl-C to abort the current statement, not crash SQLLine</a>.
Among the improvements are
<a href="https://github.com/julianhyde/sqlline/issues/237">allowing multiple statements on the same line</a>,
a more powerful
<a href="https://github.com/julianhyde/sqlline/issues/298">!outputformat</a> command,
and allowing null values to be
<a href="https://github.com/julianhyde/sqlline/issues/288">output as "null"</a>.

This release requires Java version 8 or higher.

Bugs and functional changes:

* [<a href="https://github.com/julianhyde/sqlline/issues/295">SQLLINE-295</a>]
  Find all db metadata methods including class hierarchy cases
* [<a href="https://github.com/julianhyde/sqlline/issues/284">SQLLINE-284</a>]
  Use proxy for `DatabaseMetaData`
* [<a href="https://github.com/julianhyde/sqlline/issues/237">SQLLINE-237</a>]
  Allow several SQL statements on the same line, separated by ';'
* [<a href="https://github.com/julianhyde/sqlline/issues/288">SQLLINE-288</a>]
  Output null value as `null`
* [<a href="https://github.com/julianhyde/sqlline/issues/298">SQLLINE-298</a>]
  Optional extra parameters for `!outputformat` command
* Improve the `hashCode` of the key used in the internal table cache
* [<a href="https://github.com/julianhyde/sqlline/issues/292">SQLLINE-292</a>]
  `Ctrl-C` causes SQLLine to quit, but should just cancel current query
  (Volodymyr Vysotskyi)
* Upgrades:
  * [<a href="https://github.com/julianhyde/sqlline/issues/301">SQLLINE-301</a>]
    Upgrade checkstyle to 8.20, jmockit to 1.46 and junit to 5.4.2
  * [<a href="https://github.com/julianhyde/sqlline/issues/287">SQLLINE-287</a>]
    Support JDK 12

Other:

* [<a href="https://github.com/julianhyde/sqlline/issues/296">SQLLINE-296</a>]
  Replace class `ColorBuffer` with Jline3's class `AttributedString`

## <a href="https://github.com/julianhyde/sqlline/releases/tag/sqlline-1.7.0">1.7.0</a> (2019-03-27)

Continuing the major changes in the previous release, this release
adds more improvements to interactivity, including custom prompts
(<a href="https://github.com/julianhyde/sqlline/issues/278">`promptscript`</a>
property and
<a href="https://github.com/julianhyde/sqlline/issues/246">custom prompt handler</a>),
warnings before executing dangerous commands
(<a href="https://github.com/julianhyde/sqlline/issues/239">`confirm` and
`confirmPattern` properties</a>),
and improved validation for
<a href="https://github.com/julianhyde/sqlline/issues/260">enum</a>
and
<a href="https://github.com/julianhyde/sqlline/issues/224">integer</a>
properties.

Large result sets are formatted better, faster, and using less memory,
due to two improvements: the
<a href="https://github.com/julianhyde/sqlline/issues/228">default
for the `incremental` prompty is now false</a>,
and a new
<a href="https://github.com/julianhyde/sqlline/issues/251">`incrementalBufferRows`</a>
property is 1,000 by default. SQLLine will now read the first 1,000
rows of a query, set column widths accordingly, and adjust column
widths if larger values are seen later.

This release requires Java version 8 or higher.

Bugs and functional changes:

* Prompt customization:
  * [<a href="https://github.com/julianhyde/sqlline/issues/278">SQLLINE-278</a>]
    Add 'promptscript' property, generating prompt from a JavaScript program
  * [<a href="https://github.com/julianhyde/sqlline/issues/246">SQLLINE-246</a>]
    Custom prompt handler
* [<a href="https://github.com/julianhyde/sqlline/issues/276">SQLLINE-276</a>]
  Unable to submit password interactively
* Column-widths
  * [<a href="https://github.com/julianhyde/sqlline/issues/251">SQLLINE-251</a>]
    Enter "incremental" mode if a query returns more than `incrementalBufferRows`
  * [<a href="https://github.com/julianhyde/sqlline/issues/228">SQLLINE-228</a>]
    Set `incremental` property to false by default (Cheng Lian)
* [<a href="https://github.com/julianhyde/sqlline/issues/239">SQLLINE-239</a>]
  Confirm before executing 'dangerous' SQL commands such as `DROP` and
  `DELETE`; controlled by new properties `confirm` and `confirmPattern`
  (Swaroopa Kadam)
* [<a href="https://github.com/julianhyde/sqlline/issues/258">SQLLINE-258</a>]
  Correct highlighting of numbers followed by `]`, `!`, `*`, `|`, `&`, `^`
* [<a href="https://github.com/julianhyde/sqlline/issues/260">SQLLINE-260</a>]
  Validate given values for enum type properties (e.g. `outputFormat`,
  `colorScheme`) before setting
* [<a href="https://github.com/julianhyde/sqlline/issues/226">SQLLINE-226</a>]
  Correct highlighting and line continuation for quotes inside sqlline comments
* [<a href="https://github.com/julianhyde/sqlline/issues/186">SQLLINE-186</a>]
  In definitions of built-in properties, define available values for completion
* [<a href="https://github.com/julianhyde/sqlline/issues/236">SQLLINE-236</a>]
  Add error message if URL property is absent but user, password or driver is
  present
* [<a href="https://github.com/julianhyde/sqlline/issues/232">SQLLINE-232</a>]
  Use `SqlLineParser` for file parsing, the same parser as for interactive
* [<a href="https://github.com/julianhyde/sqlline/issues/242">SQLLINE-242</a>]
  Script command should not write itself to file when spooling ends
* [<a href="https://github.com/julianhyde/sqlline/issues/5">SQLLINE-5</a>]
  Use `ServiceLoader` to load drivers, rather than `Class.forName`
* [<a href="https://github.com/julianhyde/sqlline/issues/224">SQLLINE-224</a>]
  Print exception traces only in case of `verbose` on
* [<a href="https://github.com/julianhyde/sqlline/issues/224">SQLLINE-224</a>]
  Validate values for integer properties before setting
* [<a href="https://github.com/julianhyde/sqlline/issues/134">SQLLINE-134</a>]
  Reuse history object while resetting history, use `-e` option to hide prompt
  output
* Upgrades:
  * [<a href="https://github.com/julianhyde/sqlline/issues/252">SQLLINE-252</a>]
    Update `jmockit` to 1.44
  * [<a href="https://github.com/julianhyde/sqlline/issues/266">SQLLINE-266</a>]
    Update `junit` to version 5 and `checkstyle` to version 8
  * [<a href="https://github.com/julianhyde/sqlline/issues/268">SQLLINE-268</a>]
    Bump `jline3` version to 3.10.0
  * [<a href="https://github.com/julianhyde/sqlline/issues/275">SQLLINE-275</a>]
    Upgrade `checkstyle` to 8.18

Other:

* [<a href="https://github.com/julianhyde/sqlline/issues/191">SQLLINE-279</a>]
  Release 1.7
* [<a href="https://github.com/julianhyde/sqlline/issues/281">SQLLINE-281</a>]
  Remove temporary directory after test finishes
* [<a href="https://github.com/julianhyde/sqlline/issues/257">SQLLINE-257</a>]
  Separate the code that switches off `SqlLineParser` from the code that reads
  user answer
* [<a href="https://github.com/julianhyde/sqlline/issues/11">SQLLINE-11</a>]
  Add 'Command line options' and 'Prompting' sections to manual
* [<a href="https://github.com/julianhyde/sqlline/issues/270">SQLLINE-270</a>]
  Remove `maven-compiler-plugin` from `pom.xml`
* [<a href="https://github.com/julianhyde/sqlline/issues/273">SQLLINE-273</a>]
  Failed to generate javadoc on JDK 11 (Masayuki Takahashi)
* Enable Travis CI on 'scratch' branch
* [<a href="https://github.com/julianhyde/sqlline/issues/255">SQLLINE-255</a>]
  Explicitly set `incremental` property inside
  `sqlline.SqlLineArgsTest#testMultilineScriptWithH2Comments`
* [<a href="https://github.com/julianhyde/sqlline/issues/217">SQLLINE-217</a>]
  Enable JDK 11 for AppVeyor
* [<a href="https://github.com/julianhyde/sqlline/issues/245">SQLLINE-245</a>]
  Example of launching SQLLine using a bash command-line, explaining how to
  quote special characters in the URL (Terence Namusonge)
* [<a href="https://github.com/julianhyde/sqlline/issues/221">SQLLINE-221</a>]
  Now require Java 8 or higher

## <a href="https://github.com/julianhyde/sqlline/releases/tag/sqlline-1.6.0">1.6.0</a> (2018-11-26)

This is the most colorful and interactive SQLLine ever! Upgrading to
<a href="https://github.com/julianhyde/sqlline/issues/105">`jline3`</a>
and improved
<a href="https://github.com/julianhyde/sqlline/issues/190">dialect support</a>
allowed us to add
<a href="https://github.com/julianhyde/sqlline/issues/164">syntax highlighting</a>,
<a href="https://github.com/julianhyde/sqlline/issues/190">line continuation</a>
and
<a href="https://github.com/julianhyde/sqlline/issues/184">multi-line editing</a>.
There are new commands
<a href="https://github.com/julianhyde/sqlline/issues/105">`!rerun`</a>,
<a href="https://github.com/julianhyde/sqlline/issues/105">`!/`</a> and
<a href="https://github.com/julianhyde/sqlline/issues/143">`!reset`</a>,
new properties
<a href="https://github.com/julianhyde/sqlline/issues/164">`colorScheme`</a>,
<a href="https://github.com/julianhyde/sqlline/issues/151">`escapeOutput`</a>,
<a href="https://github.com/julianhyde/sqlline/issues/177">`maxHistoryRows`</a>,
<a href="https://github.com/julianhyde/sqlline/issues/177">`maxHistoryFileRows`</a>,
<a href="https://github.com/julianhyde/sqlline/issues/60">`mode`</a>,
<a href="https://github.com/julianhyde/sqlline/issues/199">`prompt`</a>,
<a href="https://github.com/julianhyde/sqlline/issues/199">`rightPrompt`</a>,
<a href="https://github.com/julianhyde/sqlline/issues/183">`strictJdbc`</a>,
<a href="https://github.com/julianhyde/sqlline/issues/190">`useLineContinuation`</a>,
<a href="https://github.com/julianhyde/sqlline/issues/146">`version`</a>,
and improvements to existing commands.

This release requires Java version 8 or higher. (Since the previous
release, we have dropped support for JDK 1.6 and 1.7.)

WARNING: Between version 2 and 3, jline changed the format of its
history file. After this change, you may need to remove your history
file (~/.sqlline/history) or provide a `--historyfile` argument before
SQLLine will start successfully.

Bugs and functional changes:

* [<a href="https://github.com/julianhyde/sqlline/issues/218">SQLLINE-218</a>]
  Update default of `maxColumnWidth` property to -1,
  and add getter to `SqlLineOpts` for `maxWidth` property
* [<a href="https://github.com/julianhyde/sqlline/issues/213">SQLLINE-213</a>]
  Syntax highlighting does not work for `!sql`, `!all` commands
* [<a href="https://github.com/julianhyde/sqlline/issues/215">SQLLINE-215</a>]
  `!metadata 1` command should not show private methods, or methods inherited
  from `java.lang.Object`
* [<a href="https://github.com/julianhyde/sqlline/issues/183">SQLLINE-183</a>]
  Add wrapper around `DatabaseMetaData`, to make SQLLine less susceptible to
  errors in underlying JDBC driver
* [<a href="https://github.com/julianhyde/sqlline/issues/60">SQLLINE-60</a>]
  Add 'vi' editing mode, and add `mode` property to switch between emacs and
  vi
* [<a href="https://github.com/julianhyde/sqlline/issues/199">SQLLINE-199</a>]
  Make use of jline3's powerful `StyleResolver` functionality to parse styles
* [<a href="https://github.com/julianhyde/sqlline/issues/199">SQLLINE-199</a>]
  Add `prompt` and `rightPrompt` properties to allow customization of prompt
  and right prompt
* [<a href="https://github.com/julianhyde/sqlline/issues/201">SQLLINE-201</a>]
  Line continuation and highlight for sqlline comments
* [<a href="https://github.com/julianhyde/sqlline/issues/203">SQLLINE-203</a>]
  Carry on even if JDBC driver's `ResultSetMetaData.getColumnDisplaySize()`
  throws
* [<a href="https://github.com/julianhyde/sqlline/issues/205">SQLLINE-205</a>]
  Show human-readable message if `!go` command fails
* [<a href="https://github.com/julianhyde/sqlline/issues/190">SQLLINE-190</a>]
  Make sure that exceptions during highlighting/line continuation do not cause
  infinite loops
* [<a href="https://github.com/julianhyde/sqlline/issues/190">SQLLINE-190</a>]
  Add `Dialect` API, and various database-specific behaviors
* [<a href="https://github.com/julianhyde/sqlline/issues/164">SQLLINE-164</a>]
  Syntax highlighting
* [<a href="https://github.com/julianhyde/sqlline/issues/184">SQLLINE-184</a>]
  Multi-line parsing is fooled by a line that ends in a semi-colon followed by
  a comment
* [<a href="https://github.com/julianhyde/sqlline/issues/168">SQLLINE-168</a>]
  In multiline parsing, detect mismatched brackets and parentheses
* [<a href="https://github.com/julianhyde/sqlline/issues/129">SQLLINE-129</a>]
  Allow multiline query parsing in shell mode and files
* [<a href="https://github.com/julianhyde/sqlline/issues/151">SQLLINE-151</a>]
  Add property `escapeOutput` to escape control symbols
* [<a href="https://github.com/julianhyde/sqlline/issues/177">SQLLINE-177</a>]
  Add `maxHistoryRows` and `maxHistoryFileRows` properties
* [<a href="https://github.com/julianhyde/sqlline/issues/160">SQLLINE-160</a>]
  Use `maxColumnWidth` in `TableOutputFormat` column width calculation
* [<a href="https://github.com/julianhyde/sqlline/issues/158">SQLLINE-158</a>]
  Add `xmlattrs` output format name as a synonym for `xmlattr`
* [<a href="https://github.com/julianhyde/sqlline/issues/155">SQLLINE-155</a>]
  Do not use default encoding
* [<a href="https://github.com/julianhyde/sqlline/issues/155">SQLLINE-154</a>]
  Do not use default locale
* [<a href="https://github.com/julianhyde/sqlline/issues/105">SQLLINE-105</a>]
  Upgrade to jline3,
  drop support for Java versions lower than 8,
  and add `!rerun` command (also known as `!/`)
* [<a href="https://github.com/julianhyde/sqlline/issues/73">SQLLINE-73</a>]
  Re-execute the previous query
* [<a href="https://github.com/julianhyde/sqlline/issues/143">SQLLINE-143</a>]
  Add `!reset` command, and add a mode to `!set` command to show current value
* [<a href="https://github.com/julianhyde/sqlline/issues/146">SQLLINE-146</a>]
  Add read-only `version` property

Other:

* [<a href="https://github.com/julianhyde/sqlline/issues/191">SQLLINE-191</a>]
  Release 1.6
* [<a href="https://github.com/julianhyde/sqlline/issues/193">SQLLINE-193</a>]
  Add key strokes to help output
* Upgrade JMockit to 1.41
* [<a href="https://github.com/julianhyde/sqlline/issues/141">SQLLINE-141</a>]
  Add test for `!save` command
* [<a href="https://github.com/julianhyde/sqlline/issues/179">SQLLINE-179</a>]
  Add `<useManifestOnlyJar>false</useManifestOnlyJar>` setting for
  `maven-surefire-plugin`
* [<a href="https://github.com/julianhyde/sqlline/issues/181">SQLLINE-181</a>]
  Use `<code>` tag rather than `<tt>`, as `<tt>` is deprecated in HTML5
* [<a href="https://github.com/julianhyde/sqlline/issues/166">SQLLINE-166</a>]
  Refactor properties, adding `enum BuiltInProperty` and
  `interface SqlLineProperty.Writer`
* [<a href="https://github.com/julianhyde/sqlline/issues/172">SQLLINE-172</a>]
  Travis CI fails for Oracle JDK 10
* [<a href="https://github.com/julianhyde/sqlline/issues/98">SQLLINE-98</a>]
  Add `appveyor.yml`, to allow CI on Windows
* Generate javadoc in HTML 5
* [<a href="https://github.com/julianhyde/sqlline/issues/162">SQLLINE-162</a>]
  Various cleanup in `SqlLineArgsTest`
* [<a href="https://github.com/julianhyde/sqlline/issues/163">SQLLINE-163</a>]
  Upgrade `checkstyle` to 7.8.2, `maven-checkstyle-plugin` to 3.0.0 to support
  Java 8 syntax
* Site: Publish manual and API for release 1.5.0
* [<a href="https://github.com/julianhyde/sqlline/issues/153">SQLLINE-153</a>]
  Invalid link to API docs on `README.md` page
* [<a href="https://github.com/julianhyde/sqlline/issues/149">SQLLINE-149</a>]
  Correct typos
* [<a href="https://github.com/julianhyde/sqlline/issues/142">SQLLINE-142</a>]
  Add missed `-e` option in `--help` output

## <a href="https://github.com/julianhyde/sqlline/releases/tag/sqlline-1.5.0">1.5.0</a> (2018-09-09)

Bugs and functional changes:

* [<a href="https://github.com/julianhyde/sqlline/issues/135">SQLLINE-135</a>]
  Display connection class name during `!reconnect` (Arina Ielchiieva)
* [<a href="https://github.com/julianhyde/sqlline/issues/139">SQLLINE-139</a>]
  `!close` command wrongly claims to be ambiguous with `!closeall` command
  (Arina Ielchiieva)
* [<a href="https://github.com/julianhyde/sqlline/issues/106">SQLLINE-106</a>]
  New `class Application` allows customization of command handlers, output
  formats, startup message (Arina Ielchiieva)
* [<a href="https://github.com/julianhyde/sqlline/issues/114">SQLLINE-114</a>]
  Pluggable commands, by means of `interface CommandHandler` (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/52">SQLLINE-52</a>]
  Set isolation level if supported, otherwise use default (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/111">SQLLINE-111</a>]
  Enable multiline calls for `!all`, `!sql` commands from file
  (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/121">SQLLINE-121</a>]
  Cannot parse one-character commands `!?` and `!#` (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/109">SQLLINE-109</a>]
  In XML output formats, escape all required symbols in case of attribute/text
  (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/43">SQLLINE-43</a>]
  Allow quoting of arguments to `!connect` command (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/53">SQLLINE-53</a>]
  In `!connect` command, allow passing of JDBC properties outside of the URL
  (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/120">SQLLINE-120</a>]
  Do not keep connection if it fails to do connection stuff (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/55">SQLLINE-55</a>]
  SQLLine throws `NullPointerException` when JDBC URL is invalid
  (Arina Ielchiieva)
* [<a href="https://github.com/julianhyde/sqlline/issues/86">SQLLINE-86</a>]
  Parse arguments that contain quoted strings with spaces (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/104">SQLLINE-104</a>]
  Make output of `!scan` command valid (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/57">SQLLINE-57</a>]
  Respect `showheader=false` in file output (Renny Koshy and Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/107">SQLLINE-107</a>]
  If a user-specified driver cannot be found, fall back to the registered driver
  (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/101">SQLLINE-101</a>]
  Add `nullValue` property, to override the value printed for NULL values
  (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/67">SQLLINE-67</a>]
  Add `-log` command-line argument, equivalent to `!record` command
  (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/90">SQLLINE-90</a>]
  Use more relevant exception messages for `!metadata`, `!record`, `!run`
  and `!script` commands (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/88">SQLLINE-88</a>]
  Make `!set` command stable for the case of variables with null values
  (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/50">SQLLINE-50</a>]
  For CSV output format, specify delimiter and quote character (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/38">SQLLINE-38</a>]
  In `!run` command, expand "~" to user's home directory (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/83">SQLLINE-83</a>]
  Add `json` output format (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/66">SQLLINE-66</a>]
  Add `dateFormat`, `timeFormat` and `timestampFormat` properties
  (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/77">SQLLINE-77</a>]
  Poor performance with drivers that have a slow implementation of
  `DatabaseMetaData.getPrimaryKeys` JDBC method (Kevin Minder)

Other:

* [<a href="https://github.com/julianhyde/sqlline/issues/131">SQLLINE-131</a>]
  Release 1.5
* Add committers: Arina Ielchiieva and Sergey Nuyanzin (committers were
  previously called 'authors')
* [<a href="https://github.com/julianhyde/sqlline/issues/132">SQLLINE-132</a>]
  Fix 9 javadoc warnings (Arina Ielchiieva)
* [<a href="https://github.com/julianhyde/sqlline/issues/137">SQLLINE-137</a>]
  Typo in description of `!save` command (Arina Ielchiieva)
* [<a href="https://github.com/julianhyde/sqlline/issues/127">SQLLINE-127</a>]
  In manual, update `!help` description with actual output (Sergey Nuyanzin)
* Add more information to `pom.xml` and manifest
* [<a href="https://github.com/julianhyde/sqlline/issues/123">SQLLINE-123</a>]
  Make `SqlLineArgsTest.testScan` test stable in case of several registered
  drivers (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/69">SQLLINE-69</a>]
  Make `sqlline` and `sqlline.bat` scripts work in dev environment
  (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/96">SQLLINE-96</a>]
  Add to Travis CI all supported JDK versions (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/93">SQLLINE-93</a>]
  To fix flaky tests on Windows, use static final fields for
  `jline.Terminal#getWidth` and `jline.Terminal#getHeight` (Sergey Nuyanzin)
* [<a href="https://github.com/julianhyde/sqlline/issues/95">SQLLINE-95</a>]
  In `pom.xml`, replace 'prerequisites' element with `maven-enforcer-plugin`
* [<a href="https://github.com/julianhyde/sqlline/issues/80">SQLLINE-80</a>]
  In `HOWTO`, describe how to run SQLLine in IntelliJ IDEA's console on Windows
  (slankka)

## <a href="https://github.com/julianhyde/sqlline/releases/tag/sqlline-1.4.0">1.4.0</a> (2018-05-31)

Bugs and functional changes:

* [<a href="https://github.com/julianhyde/sqlline/issues/75">SQLLINE-75</a>]
  jline gives `NumberFormatException` during startup
  * Caused by [<a href="https://github.com/jline/jline2/issues/281">JLINE2-281</a>],
    and fixed by upgrading jline to 2.14.4
* [<a href="https://github.com/julianhyde/sqlline/issues/72">SQLLINE-72</a>]
  Allow quoted file names (including spaces) in `!record`, `!run` and `!script`
  commands (Jason Prodonovich)

Other:

* Upgrade `maven-javadoc-plugin` to 3.0.1 due to
  [<a href="https://issues.apache.org/jira/browse/MJAVADOC-485">MJAVADOC-517</a>]
* [<a href="https://github.com/julianhyde/sqlline/issues/76">SQLLINE-76</a>]
  During build, manual fails to validate against docbook DTD
* Travis CI: remove JDK 6 and 7 (no longer supported by Travis)
* Upgrade `maven-javadoc-plugin` to 3.0.0 due to
  [<a href="https://issues.apache.org/jira/browse/MJAVADOC-485">MJAVADOC-485</a>]
* Add hyperlinks to previous test-cases (Julian Hyde)
* Add coursier usage example, and update contact info (Marc Prud'hommeaux)

## <a href="https://github.com/julianhyde/sqlline/releases/tag/sqlline-1.3.0">1.3.0</a> (2017-06-09)

Bugs and functional changes:

* [<a href="https://github.com/julianhyde/sqlline/issues/63">SQLLINE-63</a>]
  Add Athena JDBC driver (Ben Poweski)
* [<a href="https://github.com/julianhyde/sqlline/issues/62">SQLLINE-62</a>]
  In `!record` and `!run` commands, expand '~' to user's home directory (Mike
  Mattozzi)
* [<a href="https://github.com/julianhyde/sqlline/issues/61">SQLLINE-61</a>]
  Add `!nickname` command, to set a friendly name for a connection (Mike Mattozzi)
* [<a href="https://github.com/julianhyde/sqlline/issues/54">SQLLINE-54</a>]
  Add JDK 9 support; drop JDK 1.5 support

Other:

* Fix tests on Windows
* Sort list of JDBC drivers
* Publish manual and API for release 1.2.0
* Create a property for the version number of each maven dependency
* Add "release" profile, and only sign if it is enabled
* Edit release instructions

## <a href="https://github.com/julianhyde/sqlline/releases/tag/sqlline-1.2.0">1.2.0</a> (2016-10-26)

Bugs and functional changes:

* [<a href="https://github.com/julianhyde/sqlline/issues/49">SQLLINE-49</a>]
  `!manual` command fails
* [<a href="https://github.com/julianhyde/sqlline/issues/48">SQLLINE-48</a>]
  Lazily load system properties on startup
* [<a href="https://github.com/julianhyde/sqlline/issues/35">SQLLINE-35</a>]
  Make `SqlLine.begin` method public
* [<a href="https://github.com/julianhyde/sqlline/issues/42">SQLLINE-42</a>]
  Script fails if first line is a comment
* [<a href="https://github.com/julianhyde/sqlline/issues/41">SQLLINE-41</a>]
  `!tables` command hangs in h2
* [<a href="https://github.com/julianhyde/sqlline/issues/39">SQLLINE-39</a>]
  `!help set` shouldn't break long lines
* Add `WrappedSqlException` to allow `IncrementalRows` to throw a `SQLException`
  (Parth Chandra)

Other:

* Change capitalization of project name to "SQLLine"
  (previously a mixture of "sqlline" and "SQLline")
* [<a href="https://github.com/julianhyde/sqlline/issues/37">SQLLINE-37</a>]
  Initial github-pages web site, including manual
* Checkstyle should always look for unix line endings, even on windows
* Fix Windows line endings
* Switch to "scott-data-hsqldb" as test data set; it is smaller than
  "foodmart-data-hsqldb"

## <a href="https://github.com/julianhyde/sqlline/releases/tag/sqlline-1.1.9">1.1.9</a> (2015-03-06)

No bug fixes or other functional changes

Other:
* Publish releases to <a href="https://search.maven.org/">Maven Central</a>
  (previous releases are in <a href="https://www.conjars.org/">Conjars</a>)
* Sign jars
* Use <a href="https://github.com/julianhyde/hydromatic-parent">net.hydromatic parent POM</a>,
  upgrading several maven plugins
* Fix code style for upgraded checkstyle

## <a href="https://github.com/julianhyde/sqlline/releases/tag/sqlline-1.1.8">1.1.8</a> (2015-02-16)

Bugs and functional changes:
* [<a href="https://github.com/julianhyde/sqlline/issues/32">SQLLINE-32</a>]
  `!help set` should print documentation for all variables
* Add `sqlline` and `sqlline.bat` scripts, and `jar-with-dependencies`
  (Jongyeol Choi)
* Fix color output: output style instead of name
* Add test for
  [<a href="https://issues.apache.org/jira/browse/HIVE-5768">HIVE-5768</a>]
  Beeline connection cannot be closed with `!close` command
* Test `!record` command
* [<a href="https://github.com/julianhyde/sqlline/issues/26">SQLLINE-26</a>]
  Flush output for each command when using `!record` command
* Use `ResultSet.getString()` for types that support it,
  `getObject().toString()` otherwise
* [<a href="https://github.com/julianhyde/sqlline/issues/25">SQLLINE-25</a>]
  Spaces in classpath
* Add mailing list to maven
* Upgrade `maven-release-plugin` to version 2.4.2
* Make return code enum public so other code can check it

## <a href="https://github.com/julianhyde/sqlline/releases/tag/sqlline-1.1.7">1.1.7</a> (2014-02-14)

Bugs and functional changes:
* Fix bug: SqlCompleter was skipping every other column.
* Fix <a href="https://issues.apache.org/jira/browse/HIVE-4566">HIVE-4566</a>, "NullPointerException if typeinfo and nativesql commands are executed at beeline before a DB connection is established" (Xuefu Zhang via Ashutosh Chauhan and Julian Hyde)
* Fix <a href="https://issues.apache.org/jira/browse/HIVE-4364">HIVE-4364</a>, "beeline always exits with 0 status, should exit with non-zero status on error" (Rob Weltman via Ashutosh Chauhan and Julian Hyde)
* Fix <a href="https://issues.apache.org/jira/browse/HIVE-4268">HIVE-4268</a>, "Beeline should support the -f option" (Rob Weltman via cws and Julian Hyde). SQLLine options '-f file' and '--run=file' are now equivalent.
* Add `SqlLineArgsTest`, copied from `BeeLineArgsTest`, but testing against hsqldb rather than Hive.

Code re-organization:
* Break out inner classes.
* Re-format in Apache style (indent 2).
* Enable maven-checkstyle-plugin, and fix code to comply.
* Modern Java style: add generics; use `StringBuilder` rather than `StringBuffer`; convert arrays to collections; convert some array parameters to varargs; convert loops to `foreach`; remove paranoid `!= null` checks; convert `ColorAttr` to an enum.
* Fix javadoc errors and warnings.

Other:
* Add release history.
* Enable oraclejdk8 in Travis CI.
* Fix verbose mode.

## <a href="https://github.com/julianhyde/sqlline/releases/tag/sqlline-1.1.6">1.1.6</a> (2013-08-23)

* When not running as a shell return boolean to indicate if command worked.
* Add travis build status to README.

## <a href="https://github.com/julianhyde/sqlline/releases/tag/sqlline-1.1.5">1.1.5</a> (2013-07-31)

* Follow the JDBC pattern of not putting extra checks in cancel calls.
* Specify source and target java versions.
* Enable Travis CI.

## <a href="https://github.com/julianhyde/sqlline/releases/tag/sqlline-1.1.4">1.1.4</a> (2013-07-22)

* Add maven release plugin.
* When reading from stdin redirect don't NullPointer at the end of data.

## 1.1.3 (2013-06-27)

* Fixed setting of working dot dir to allow override.
* Limit history saving to interactive shell.
* Fixed history so it saves; removed JLine1 workaround code.
* CTRL-C cancels screen output from query
