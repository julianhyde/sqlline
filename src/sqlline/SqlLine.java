/*
 *	Copyright (c) 2002,2003 Marc Prud'hommeaux
 *
 *	This library is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU General Public
 *	License as published by the Free Software Foundation; either
 *	version 2.1 of the License, or (at your option) any later version.
 *
 *	This library is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *	General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public
 *	License along with this library; if not, write to the Free Software
 *	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 *
 *	This software is hosted by SourceForge.
 *	SourceForge is a trademark of VA Linux Systems, Inc.
 */
package sqlline;

import jline.*;
import java.io.*;
import java.net.*;
import java.text.*;
import java.sql.*;
import java.util.*;
import java.lang.reflect.*;
import java.util.zip.*;
import java.util.jar.*;

/** 
 *  A console SQL shell with command completion.
 *  <p>
 *  TODO:
 *  <ul>
 *  <li>User-friendly connection prompts</li>
 *	<li>Page results</li>
 *	<li>Handle binary data (blob fields)</li>
 *	<li>Implement command aliases</li>
 *	<li>Stored procedure execution</li>
 *	<li>Binding parameters to prepared statements</li>
 *	<li>Scripting language</li>
 *	<li>XA transactions</li>
 *  </ul>
 *  
 *  @author  <a href="mailto:marc@apocalypse.org">Marc Prud'hommeaux</a>
 */
public class SqlLine
{
	private static final ResourceBundle loc = ResourceBundle.getBundle (
		SqlLine.class.getName ());

	/*
	public static final String APP_NAME = "sqlline";
	public static final String APP_VERSION = "0.7.3";
	public static final String APP_AUTHOR = "Marc Prud'hommeaux";
	public static final String APP_AUTHOR_EMAIL = "marc@apocalypse.org";
	public static final String APP_TITLE = APP_NAME + " " + APP_VERSION
											+ " by " + APP_AUTHOR;
	*/

	private static final String sep = System.getProperty ("line.separator");
	private boolean exit = false;
	private final SqlLine sqlline = this;
	private Collection drivers = null;
	private Connections connections = new Connections ();
	public static final String COMMAND_PREFIX = "!";
	private Completor sqlLineCommandCompletor;
	private Map completions = new HashMap ();
	private Opts opts = new Opts (System.getProperties ());
	String lastProgress = null;
	String prompt = "sqlline";
	private Map seenWarnings = new HashMap ();
	private final Commands command = new Commands ();
	private OutputFile script = null;
	private OutputFile record = null;
	private ConsoleReader reader;
	private List batch = null;


	private final Map formats = map (new Object [] {
		"vertical",			new VerticalOutputFormat (),
		"table",			new TableOutputFormat (),
		"csv",				new SeparatedValuesOutputFormat (','),
		"tsv",				new SeparatedValuesOutputFormat ('\t'),
		"xmlattr",			new XMLAttributeOutputFormat (),
		"xmlelements",		new XMLElementOutputFormat (),
		});


	private CommandHandler [] commands = new CommandHandler []
	{
		new ReflectiveCommandHandler (new String [] { "quit", "done", "exit" },
			null),
		new ReflectiveCommandHandler (new String [] { "connect", "open" },
			new Completor [] {
				new SimpleCompletor (getConnectionURLExamples ())}),
		new ReflectiveCommandHandler (new String [] { "describe" },
			new Completor [] { new TableNameCompletor ()}),
		new ReflectiveCommandHandler (new String [] { "indexes" },
			new Completor [] { new TableNameCompletor ()}),
		new ReflectiveCommandHandler (new String [] { "primarykeys" },
			new Completor [] { new TableNameCompletor ()}),
		new ReflectiveCommandHandler (new String [] { "exportedkeys" },
			new Completor [] { new TableNameCompletor ()}),
		new ReflectiveCommandHandler (new String [] { "manual" },
			null),
		new ReflectiveCommandHandler (new String [] { "importedkeys" },
			new Completor [] { new TableNameCompletor ()}),
		new ReflectiveCommandHandler (new String [] { "procedures" },
			null),
		new ReflectiveCommandHandler (new String [] { "tables" },
			null),
		new ReflectiveCommandHandler (new String [] { "columns" },
			new Completor [] { new TableNameCompletor ()}),
		new ReflectiveCommandHandler (new String [] { "reconnect" },
			null),
		new ReflectiveCommandHandler (new String [] { "dropall" },
			new Completor [] { new TableNameCompletor ()}),
		new ReflectiveCommandHandler (new String [] { "history" },
			null),
		new ReflectiveCommandHandler (new String [] { "metadata" },
			new Completor [] {
				new SimpleCompletor (getMetadataMethodNames ())}),
		new ReflectiveCommandHandler (new String [] { "dbinfo" },
			null),
		new ReflectiveCommandHandler (new String [] { "rehash" },
			null),
		new ReflectiveCommandHandler (new String [] { "verbose" },
			null),
		new ReflectiveCommandHandler (new String [] { "run" },
			new Completor [] { new FileNameCompletor ()}),
		new ReflectiveCommandHandler (new String [] { "batch" },
			null),
		new ReflectiveCommandHandler (new String [] { "list" },
			null),
		new ReflectiveCommandHandler (new String [] { "all" },
			null),
		new ReflectiveCommandHandler (new String [] { "go", "#" },
			null),
		new ReflectiveCommandHandler (new String [] { "script" },
			new Completor [] { new FileNameCompletor ()}),
		new ReflectiveCommandHandler (new String [] { "record" },
			new Completor [] { new FileNameCompletor ()}),
		new ReflectiveCommandHandler (new String [] { "brief" },
			null),
		new ReflectiveCommandHandler (new String [] { "close" },
			null),
		new ReflectiveCommandHandler (new String [] { "isolation" },
			new Completor [] { new SimpleCompletor (getIsolationLevels ())}),
		new ReflectiveCommandHandler (new String [] { "outputformat" },
			new Completor [] { new SimpleCompletor (
				(String [])formats.keySet ().toArray (new String [0]) )}),
		new ReflectiveCommandHandler (new String [] { "autocommit" },
			null),
		new ReflectiveCommandHandler (new String [] { "commit" },
			null),
		new ReflectiveCommandHandler (new String [] { "properties" },
			new Completor [] { new FileNameCompletor ()}),
		new ReflectiveCommandHandler (new String [] { "rollback" },
			null),
		new ReflectiveCommandHandler (new String [] { "help", "?" },
			null),
		new ReflectiveCommandHandler (new String [] { "set" },
			opts.optionCompletors ()),
		new ReflectiveCommandHandler (new String [] { "save" },
			null),
		new ReflectiveCommandHandler (new String [] { "scan" },
			null),
		new ReflectiveCommandHandler (new String [] { "sql" },
			null),
	};


	static final SortedSet KNOWN_DRIVERS = new TreeSet (Arrays.asList (
		new String [] {
			"com.merant.datadirect.jdbc.sqlserver.SQLServerDriver",
			"com.microsoft.jdbc.sqlserver.SQLServerDriver",
			"com.ddtek.jdbc.informix.InformixDriver",
			"org.sourceforge.jxdbcon.JXDBConDriver",
			"com.ddtek.jdbc.oracle.OracleDriver",
			"net.sourceforge.jtds.jdbc.Driver",
			"com.pointbase.jdbc.jdbcDriver",
			"com.internetcds.jdbc.tds.SybaseDriver",
			"org.enhydra.instantdb.jdbc.idbDriver",
			"com.sybase.jdbc2.jdbc.SybDriver",
			"com.ddtek.jdbc.sybase.SybaseDriver",
			"COM.cloudscape.core.JDBCDriver",
			"in.co.daffodil.db.jdbc.DaffodilDBDriver",
			"com.jnetdirect.jsql.JSQLDriver",
			"COM.ibm.db2.jdbc.net.DB2Driver",
			"org.hsqldb.jdbcDriver",
			"com.pointbase.jdbc.jdbcUniversalDriver",
			"com.ddtek.jdbc.sqlserver.SQLServerDriver",
			"com.ddtek.jdbc.db2.DB2Driver",
			"com.merant.datadirect.jdbc.oracle.OracleDriver",
			"oracle.jdbc.OracleDriver",
			"com.informix.jdbc.IfxDriver",
			"com.merant.datadirect.jdbc.informix.InformixDriver",
			"com.ibm.db2.jcc.DB2Driver",
			"com.pointbase.jdbc.jdbcEmbeddedDriver",
			"org.gjt.mm.mysql.Driver",
			"org.postgresql.Driver",
			"com.mysql.jdbc.Driver",
			"oracle.jdbc.driver.OracleDriver",
			"interbase.interclient.Driver",
			"com.mysql.jdbc.NonRegisteringDriver",
			"com.merant.datadirect.jdbc.db2.DB2Driver",
			"com.merant.datadirect.jdbc.sybase.SybaseDriver",
			"com.internetcds.jdbc.tds.Driver",
			"org.hsqldb.jdbcDriver",
			"org.hsql.jdbcDriver",
			"COM.cloudscape.core.JDBCDriver",
			"in.co.daffodil.db.jdbc.DaffodilDBDriver",
			"com.ddtek.jdbc.db2.DB2Driver",
			"interbase.interclient.Driver",
			"com.mysql.jdbc.Driver",
			"com.ddtek.jdbc.oracle.OracleDriver",
			"org.postgresql.Driver",
			"com.pointbase.jdbc.jdbcUniversalDriver",
			"org.sourceforge.jxdbcon.JXDBConDriver",
			"com.ddtek.jdbc.sqlserver.SQLServerDriver",
			"com.jnetdirect.jsql.JSQLDriver",
			"com.microsoft.jdbc.sqlserver.SQLServerDriver",
			"weblogic.jdbc.mssqlserver4.Driver",
			"com.ddtek.jdbc.sybase.SybaseDriver",
			"oracle.jdbc.pool.OracleDataSource",
			"org.axiondb.jdbc.AxionDriver",
			"COM.ibm.db2.jdbc.app.DB2Driver",
			"com.ibm.as400.access.AS400JDBCDriver",
			"COM.FirstSQL.Dbcp.DbcpDriver",
			"COM.ibm.db2.jdbc.net.DB2Driver",
			"org.enhydra.instantdb.jdbc.idbDriver",
			"com.informix.jdbc.IfxDriver",
			"com.microsoft.jdbc.sqlserver.SQLServerDriver",
			"com.imaginary.sql.msql.MsqlDriver",
			"sun.jdbc.odbc.JdbcOdbcDriver",
			"oracle.jdbc.driver.OracleDriver",
			"intersolv.jdbc.sequelink.SequeLinkDriver",
			"openlink.jdbc2.Driver",
			"com.pointbase.jdbc.jdbcUniversalDriver",
			"postgres95.PGDriver",
			"postgresql.Driver",
			"solid.jdbc.SolidDriver",
			"centura.java.sqlbase.SqlbaseDriver",
			"interbase.interclient.Driver",
			"com.mckoi.JDBCDriver",
			"com.inet.tds.TdsDriver",
			"com.microsoft.jdbc.sqlserver.SQLServerDriver",
			"com.thinweb.tds.Driver",
			"weblogic.jdbc.mssqlserver4.Driver",
			"com.mysql.jdbc.DatabaseMetaData",
			"org.gjt.mm.mysql.Driver",
			"com.sap.dbtech.jdbc.DriverSapDB",
			"com.sybase.jdbc2.jdbc.SybDriver",
			"com.sybase.jdbc.SybDriver",
			"com.internetcds.jdbc.tds.Driver",
			"weblogic.jdbc.pool.Driver",
	}));


	static
	{
		Class jline;

		try
		{
			jline = Class.forName ("jline.ConsoleReader");
		}
		catch (Throwable t)
		{
			throw new ExceptionInInitializerError (loc ("jline-missing"));
		}

		/*
		System.out.println ("JLine package: " + jline.getPackage ());

		String required = "1.0.3";

		if (!(jline.getPackage ().isCompatibleWith (required)))
			throw new ExceptionInInitializerError (loc ("jline-version",
				new Object [] { 
					jline.getPackage ().getSpecificationTitle (),
					jline.getPackage ().getSpecificationVersion (),
					required,
					}));
		*/
	}


	static Manifest getManifest ()
		throws IOException
	{
		URL base = SqlLine.class.getResource ("/META-INF/MANIFEST.MF");
		URLConnection c = base.openConnection ();
		if (c instanceof JarURLConnection)
			return ((JarURLConnection)c).getManifest ();

		return null;
	}


	static String getManifestAttribute (String name)
	{
		try
		{
			Manifest m = getManifest ();
			if (m == null)
				return "??";

			Attributes attrs = m.getAttributes ("sqlline");
			if (attrs == null)
				return "???";

			String val = attrs.getValue (name);
			if (val == null || "".equals (val))
				return "????";

			return val;
		}
		catch (Exception e)
		{
			e.printStackTrace ();
			return "?????";
		}
	}


	static String getApplicationTitle ()
	{
		Package pack = SqlLine.class.getPackage ();

		return loc ("app-introduction", new Object [] {
			pack.getImplementationTitle () == null ? "sqlline"
				: pack.getImplementationTitle (),
			pack.getImplementationVersion () == null ? "???"
				: pack.getImplementationVersion (),
			pack.getImplementationVendor () == null ? "Marc Prud'hommeaux"
				: pack.getImplementationVendor (),
			// getManifestAttribute ("Specification-Title"),
			// getManifestAttribute ("Implementation-Version"),
			// getManifestAttribute ("Implementation-ReleaseDate"),
			// getManifestAttribute ("Implementation-Vendor"),
			// getManifestAttribute ("Implementation-License"),
			});
	}


	static String getApplicationContactInformation ()
	{
		return getManifestAttribute ("Implementation-Vendor");
	}


	static String loc (String res)
	{
		return loc (res, new Object [0]);
	}


	static String loc (String res, int param)
	{
		try
		{
			return MessageFormat.format (
				new ChoiceFormat (loc.getString (res)).format (param),
				new Object [] { new Integer (param) });
		}
		catch (Exception e)
		{
			return res + ": " + param;
		}
	}


	static String loc (String res, Object param1)
	{
		return loc (res, new Object [] { param1 });
	}


	static String loc (String res, Object param1, Object param2)
	{
		return loc (res, new Object [] { param1, param2 });
	}


	static String loc (String res, Object [] params)
	{
		try
		{
			return MessageFormat.format (loc.getString (res), params);
		}
		catch (Exception e)
		{
			e.printStackTrace ();

			try
			{
				return res + ": " + Arrays.asList (params);
			}
			catch (Exception e2)
			{
				return res;
			}
		}
	}


	/** 
	 *  Starts the program.
	 */
	public static void main (String [] args)
		throws IOException
	{
		SqlLine sqlline = new SqlLine ();
		sqlline.begin (args);

		// exit the system: useful for Hypersonic and other
		// badly-behaving systems
		if (!Boolean.getBoolean ("sqlline.system.exit"))
			System.exit (0);
	}


	SqlLine ()
	{
		// registerKnownDrivers ();

		sqlLineCommandCompletor = new SQLLineCommandCompletor ();
	}


	DatabaseConnection con ()
	{
		return connections.current ();
	}


	Connection conn ()
	{
		if (connections.current () == null)
			throw new IllegalArgumentException (loc ("no-current-connection"));
		if (connections.current ().connection == null)
			throw new IllegalArgumentException (loc ("no-current-connection"));
		return connections.current ().connection;
	}


	DatabaseMetaData meta ()
	{
		if (connections.current () == null)
			throw new IllegalArgumentException (loc ("no-current-connection"));
		if (connections.current ().meta == null)
			throw new IllegalArgumentException (loc ("no-current-connection"));
		return connections.current ().meta;
	}


	public String [] getIsolationLevels ()
	{
		return new String [] {
			"TRANSACTION_NONE",
			"TRANSACTION_READ_COMMITTED",
			"TRANSACTION_READ_UNCOMMITTED",
			"TRANSACTION_REPEATABLE_READ",
			"TRANSACTION_SERIALIZABLE",
			};
	}


	public String [] getMetadataMethodNames ()
	{
		try
		{
			TreeSet mnames = new TreeSet ();
			Method [] m = DatabaseMetaData.class.getDeclaredMethods ();
			for (int i = 0; m != null && i < m.length; i++)
				mnames.add (m [i].getName ());

			return (String [])mnames.toArray (new String [0]);
		}
		catch (Throwable t)
		{
			return new String [0];
		}
	}


	public String [] getConnectionURLExamples ()
	{
		return new String [] {
			"jdbc:JSQLConnect://<hostname>/database=<database>",
			"jdbc:cloudscape:<database>;create=true",
			"jdbc:twtds:sqlserver://<hostname>/<database>",
			"jdbc:daffodilDB_embedded:<database>;create=true",
			"jdbc:datadirect:db2://<hostname>:50000;databaseName=<database>",
			"jdbc:inetdae:<hostname>:1433",
			"jdbc:datadirect:oracle://<hostname>:1521;SID=<database>;"
				+ "MaxPooledStatements=0",
			"jdbc:datadirect:sqlserver://<hostname>:1433;SelectMethod=cursor;"
				+ "DatabaseName=<database>",
			"jdbc:datadirect:sybase://<hostname>:5000",
			"jdbc:db2://<hostname>/<database>",
			"jdbc:hsqldb:<database>",
			"jdbc:idb:<database>.properties",
			"jdbc:informix-sqli://<hostname>:1526/<database>:INFORMIXSERVER"
				+ "=<database>",
			"jdbc:interbase://<hostname>//<database>.gdb",
			"jdbc:microsoft:sqlserver://<hostname>:1433;"
				+ "DatabaseName=<database>;SelectMethod=cursor",
			"jdbc:mysql://<hostname>/<database>?autoReconnect=true",
			"jdbc:oracle:thin:@<hostname>:1521:<database>",
			"jdbc:pointbase:<database>,database.home=<database>,create=true",
			"jdbc:postgresql://<hostname>:5432/<database>",
			"jdbc:postgresql:net//<hostname>/<database>",
			"jdbc:sybase:Tds:<hostname>:4100/<database>?ServiceName=<database>",
			"jdbc:weblogic:mssqlserver4:<database>@<hostname>:1433",
			"jdbc:odbc:<database>",
			"jdbc:sequelink://<hostname>:4003/[Oracle]",
			"jdbc:sequelink://<hostname>:4004/[Informix];Database=<database>",
			"jdbc:sequelink://<hostname>:4005/[Sybase];Database=<database>",
			"jdbc:sequelink://<hostname>:4006/[SQLServer];Database=<database>",
			"jdbc:sequelink://<hostname>:4011/[ODBC MS Access];"
				+ "Database=<database>",
			"jdbc:openlink://<hostname>/DSN=SQLServerDB/UID=sa/PWD=",
			"jdbc:solid://<hostname>:<port>/<UID>/<PWD>",
			"jdbc:dbaw://<hostname>:8889/<database>",
		};
	}
	
	/** 
	 *  Entry point to creating a {@link ColorBuffer} with color
	 *  enabled or disabled depending on the calue of
	 *  {@link Opts#getColor}.
	 */
	ColorBuffer color ()
	{
		return new ColorBuffer (opts.getColor ());
	}


	/** 
	 *  Entry point to creating a {@link ColorBuffer} with color
	 *  enabled or disabled depending on the calue of
	 *  {@link Opts#getColor}.
	 */
	ColorBuffer color (String msg)
	{
		return new ColorBuffer (msg, opts.getColor ());
	}


	/*
	public String nameToDriverClass (String name)
	{
		if (name.startsWith ("jdbc:"))
			return null;

		// ### FIXME: other drivers
		if (name.indexOf ("oracle") != -1)
			return "oracle.jdbc.OracleDriver";

		return null;
	}
	*/


	/*
	public boolean connectByPrompt (String className)
		throws Exception
	{
		return false;
	}
	*/


	/** 
	 *  Currently does nothing, because very few drivers actually support
	 *  {@link DriverPropertyInfo}.
	 */
	/*
	public boolean connectByDriverPropertyInfo (String className)
		throws Exception
	{
		Driver driver = (Driver)Class.forName (className).newInstance ();
		String url = "jdbc:oracle:thin:@10.0.0.50:1521:ora92";
		Properties props = new Properties ();
		DriverPropertyInfo [] info = driver.getPropertyInfo (url, props);
		while (info != null && info.length > 0)
		{
			for (int i = 0; info != null && i < info.length; i++)
			{
				output (info [i].description + " (" + info [i].name + "): "
					+ info [i].value
					+ " [" + Arrays.asList (info [i].choices) + "]");
			}

			info = driver.getPropertyInfo (url, props);
		}

		return false;
	}
	*/


	/** 
	 *  Walk through all the known drivers and try to register them.
	 */
	void registerKnownDrivers ()
	{
		for (Iterator i = KNOWN_DRIVERS.iterator (); i.hasNext (); )
		{
			try { Class.forName (i.next ().toString ()); }
				catch (Throwable t) { }
		}
	}


	boolean initArgs (String [] args)
	{
		List commands = new LinkedList ();
		List files = new LinkedList ();
		String driver = null, user = null, pass = null, url = null, cmd = null;

		for (int i = 0; i < args.length; i++)
		{
			if (args [i].equals ("--help") || args [i].equals ("-h"))
			{
				usage ();
				return false;
			}

			// -- arguments are treated as properties
			if (args [i].startsWith ("--"))
			{
				String [] parts = split (args [i].substring (2), "=");
				debug (loc ("setting-prop", Arrays.asList (parts)));
				if (parts.length > 0)
				{
					boolean ret;

					if (parts.length >= 2)
						ret = opts.set (parts [0], parts [1], true);
					else
						ret = opts.set (parts [0], "true", true);

					if (!ret)
						return false;

				}

				continue;
			}

			if (args [i].equals ("-d"))
				driver = args [i++ + 1];
			else if (args [i].equals ("-n"))
				user = args [i++ + 1];
			else if (args [i].equals ("-p"))
				pass = args [i++ + 1];
			else if (args [i].equals ("-u"))
				url = args [i++ + 1];
			else if (args [i].equals ("-e"))
				commands.add (args [i++ + 1]);
			else
				files.add (args [i]);
		}

		if (url != null)
		{
			String com = "!connect "
				+ url + " "
				+ (user == null || user.length () == 0 ? "''" : user) + " "
				+ (pass == null || pass.length () == 0 ? "''" : pass) + " "
				+ (driver == null ? "" : driver);
			debug ("issuing: " + com);
			dispatch (com);
		}

		// now load properties files
		for (Iterator i = files.iterator (); i.hasNext (); )
			dispatch ("!properties " + i.next ());


		if (commands.size () > 0)
		{
			// for single commane execute, disable color
			opts.setColor (false);
			opts.setHeaderInterval (-1);

			for (Iterator i = commands.iterator (); i.hasNext (); )
			{
				String command = i.next ().toString ();
				debug (loc ("executing-command", command));
				dispatch (command);
			}

			exit = true; // execute and exit
		}

		return true;
	}


	/** 
	 *  Start accepting input from stdin, and dispatch it
	 *  to the appropriate {@link CommandHandler} until the
	 *  global variable <code>exit</code> is true.
	 */
	void begin (String [] args)
		throws IOException
	{
		try
		{
			// load the options first, so we can override on the command line
			opts.load ();
		}
		catch (Exception e)
		{
		}

		ConsoleReader reader = getConsoleReader ();
		if (!(initArgs (args)))
		{
			usage ();
			return;
		}

		try
		{
			info (getApplicationTitle ());
		}
		catch (Exception e)
		{
		}


		while (!exit)
		{
			try
			{
				dispatch (reader.readLine (getPrompt ()));
			}
			catch (EOFException eof)
			{
				// CTRL-D
				command.quit (null);
			}
			catch (Throwable t)
			{
				handleException (t);
			}
		}
	}


	public ConsoleReader getConsoleReader ()
		throws IOException
	{
		Terminal.setupTerminal ();
		reader = new ConsoleReader ();


		// setup history
		ByteArrayInputStream historyBuffer = null;

		if (new File (opts.getHistoryFile ()).isFile ())
		{
			try
			{
				// save the current contents of the history buffer. This gets
				// around a bug in JLine where setting the output before the
				// input will clobber the history input, but setting the 
				// input before the output will cause the previous commands
				// to not be saved to the buffer.
				FileInputStream historyIn = new FileInputStream (
					opts.getHistoryFile ());
				ByteArrayOutputStream hist = new ByteArrayOutputStream ();
				int n;
				while ((n = historyIn.read ()) != -1)
					hist.write (n);
				historyIn.close ();
	
				historyBuffer = new ByteArrayInputStream (hist.toByteArray ());
			}
			catch (Exception e)
			{
				handleException (e);
			}
		}

		try
		{
			// now set the output for the history
			PrintWriter historyOut = new PrintWriter (new FileWriter (
				opts.getHistoryFile ()));
			reader.getHistory ().setOutput (historyOut);
		}
		catch (Exception e)
		{
			handleException (e);
		}

		try
		{
			// now load in the previous history
			if (historyBuffer != null)
				reader.getHistory ().load (historyBuffer);
		}
		catch (Exception e)
		{
			handleException (e);
		}


		reader.addCompletor (new SQLLineCompletor ());

		return reader;
	}


	void usage ()
	{
		output (loc ("cmd-usage"));
	}


	/** 
	 *  Dispatch the specified line to the appropriate
	 *  {@link CommandHandler}.
	 *  
	 *  @param  line  the commmand-line to dispatch
	 *  @return  true if the command was "successful"
	 */
	boolean dispatch (String line)
	{
		if (line == null)
		{
			// exit
			exit = true;
			return true;
		}

		if (line.trim ().length () == 0)
			return true;

		// Comments start with a "#" character.
		if (line.startsWith ("#"))
			return true;

		line = line.trim ();

		// save it to the current script, if any
		if (script != null)
			script.addLine (line);

		if (line.equals ("?") || line.equalsIgnoreCase ("help"))
			line = "!help";

		if (line.startsWith (COMMAND_PREFIX))
		{
			Map cmdMap = new TreeMap ();
			line = line.substring (1);
			for (int i = 0; i < commands.length; i++)
			{
				String match = commands [i].matches (line);
				if (match != null)
					cmdMap.put (match, commands [i]);
			}

			if (cmdMap.size () == 0)
				return error (loc ("unknown-command", line));
			else if (cmdMap.size () > 1)
				return error (loc ("multiple-matches",
					cmdMap.keySet ().toString ()));
			else
				return ((CommandHandler)cmdMap.values ().iterator ().next ())
					.execute (line);
		}
		else
			return command.sql (line);
	}


	/** 
	 *  Pint the specified message to the console
	 *  
	 *  @param  msg  the message to print
	 */
	void output (String msg)
	{
		output (msg, true);
	}


	void info (String msg)
	{
		if (!(opts.getSilent ()))
			output (msg, true, System.err);
	}


	void info (ColorBuffer msg)
	{
		if (!(opts.getSilent ()))
			output (msg, true, System.err);
	}


	/** 
	 *  Issue the specified error message
	 *  
	 *  @param  msg  the message to issue
	 *  @return  false always
	 */
	boolean error (String msg)
	{
		output (color ().red (msg), true, System.err);
		return false;
	}


	boolean error (Throwable t)
	{
		handleException (t);
		return false;
	}


	void debug (String msg)
	{
		if (opts.getVerbose ())
			output (color ().blue (msg), true, System.err);
	}


	void output (ColorBuffer msg)
	{
		output (msg, true);
	}


	void output (String msg, boolean newline, PrintStream out)
	{
		output (color (msg), newline, out);
	}


	void output (ColorBuffer msg, boolean newline)
	{
		output (msg, newline, System.out);
	}


	void output (ColorBuffer msg, boolean newline, PrintStream out)
	{
		if (newline)
			out.println (msg.getColor ());
		else
			out.print (msg.getColor ());

		if (record == null)
			return;

		// only write to the record file if we are writing a line ...
		// otherwise we might get garbage from backspaces and such.
		if (newline)
			record.addLine (msg.getMono ()); // always just write mono
	}


	/** 
	 *  Print the specified message to the console
	 *  
	 *  @param  msg  the message to print
	 *  @param  newline  if false, do not append a newline
	 */
	void output (String msg, boolean newline)
	{
		output (color (msg), newline);
	}


	void autocommitStatus (Connection c)
		throws SQLException
	{
		info (loc ("autocommit-status", c.getAutoCommit () + ""));
	}


	/** 
	 *  Ensure that autocommit is on for the current connection
	 *  
	 *  @return  true if autocommit is set
	 */
	boolean assertAutoCommit ()
	{
		if (!(assertConnection ()))
			return false;

		try
		{
			if (con ().connection.getAutoCommit ())
				return error (loc ("autocommit-needs-off"));
		}
		catch (Exception e)
		{
			return error (e);
		}

		return true;
	}


	/** 
	 *  Assert that we have an active, living connection. Print
	 *  an error message if we do not.
	 *  
	 *  @return  true if there is a current, active connection
	 */
	boolean assertConnection ()
	{
		try
		{
			if (con () == null || con ().connection == null)
				return error (loc ("no-current-connection"));

			if (con ().connection.isClosed ())
				return error (loc ("connection-is-closed"));
		}
		catch (SQLException sqle)
		{
			return error (loc ("no-current-connection"));
		}

		return true;
	}


	/** 
	 *  Print out any warnings that exist for the current connection.
	 */
	void showWarnings ()
	{
		if (con ().connection == null)
			return;

		if (!opts.getVerbose ())
			return;

		try
		{
			showWarnings (con ().connection.getWarnings ());
		}
		catch (Exception e)
		{
			handleException (e);
		}
	}


	/** 
	 *  Print the specified warning on the console, as well as
	 *  any warnings that are returned from
	 *  {@link SQLWarning#getNextWarning}.
	 *  
	 *  @param  warn  the {@link SQLWarning} to print
	 */
	void showWarnings (SQLWarning warn)
	{
		if (warn == null)
			return;

		if (seenWarnings.get (warn) == null)
		{
			// don't re-display warnings we have already seen
			seenWarnings.put (warn, new java.util.Date ());
			handleSQLException (warn);
		}

		SQLWarning next = warn.getNextWarning ();
		if (next != warn)
			showWarnings (next);
	}


	String getPrompt ()
	{
		if (con () == null || con ().url == null)
			return "sqlline> ";
		else
			return getPrompt (connections.getIndex ()
				+ ": " + con ().url) + "> ";
	}


	static String getPrompt (String url)
	{
		if (url == null || url.length () == 0)
			url = "sqlline";

		if (url.indexOf (";") > -1)
			url = url.substring (0, url.indexOf (";"));
		if (url.indexOf ("?") > -1)
			url = url.substring (0, url.indexOf ("?"));

		if (url.length () > 45)
			url = url.substring (0, 45);


		return url;
	}


	/** 
	 *  Try to obtain the current size of the specified {@link ResultSet}
	 *  by jumping to the last row and getting the row number.
	 *  
	 *  @param  rs	the {@link ResultSet} to get the size for
	 *  @return the size, or -1 if it could not be obtained
	 */
	int getSize (ResultSet rs)
	{
		try
		{
			if (rs.getType () == rs.TYPE_FORWARD_ONLY)
				return -1;

			rs.last ();
			int total = rs.getRow ();
			rs.beforeFirst ();
			return total;
		}
		catch (SQLException sqle)
		{
			return -1;
		}
		// JDBC 1 driver error
		catch (AbstractMethodError ame)
		{
			return -1;
		}
	}


	ResultSet getColumns (String table)
		throws SQLException
	{
		if (!(assertConnection ()))
			return null;

		return con ().meta.getColumns (
			con ().meta.getConnection ().getCatalog (), null, table, "%");
	}


	ResultSet getTables ()
		throws SQLException
	{
		if (!(assertConnection ()))
			return null;

		return con ().meta.getTables (
			con ().meta.getConnection ().getCatalog (), null, "%",
				new String [] { "TABLE" });
	}


	String [] getColumnNames (DatabaseMetaData meta)
		throws SQLException
	{
		Set names = new HashSet ();

		info (loc ("building-tables"));

		try
		{
			ResultSet columns = getColumns ("%");

			try
			{
				int total = getSize (columns);
				int index = 0;
	
				while (columns.next ())
				{
					// add the following strings:
					// 1. column name
					// 2. table name
					// 3. tablename.columnname

					progress (index++, total);
					String name = columns.getString ("TABLE_NAME");
					names.add (name);
					names.add (columns.getString ("COLUMN_NAME"));
					names.add (columns.getString ("TABLE_NAME") + "."
						+ columns.getString ("COLUMN_NAME"));
				}

				progress (index, index);
			}
			finally
			{
				columns.close ();
			}
	
			info (loc ("done"));
	
			return (String [])names.toArray (new String [0]);
		}
		catch (Throwable t)
		{
			handleException (t);
			return new String [0];
		}
	}


	////////////////////
	// String utilities
	////////////////////


	/** 
	 *  Split the line into an array by tokenizing on space characters
	 *  
	 *  @param  line	the line to break up
	 *  @return  		an array of individual words
	 */
	String [] split (String line)
	{
		return split (line, " ");
	}


	String dequote (String str)
	{
		if (str == null)
			return null;

		while ((str.startsWith ("'") && str.endsWith ("'")) 
			|| (str.startsWith ("\"") && str.endsWith ("\"")))
			str = str.substring (1, str.length () - 1);

		return str;
	}


	String [] split (String line, String delim)
	{
		StringTokenizer tok = new StringTokenizer (line, delim);
		String [] ret = new String [tok.countTokens ()];
		int index = 0;
		while (tok.hasMoreTokens ())
		{
			String t = tok.nextToken ();

			t = dequote (t);

			ret [index++] = t;
		}

		return ret;
	}


	static Map map (Object [] obs)
	{
		Map m = new HashMap ();
		for (int i = 0; i < obs.length - 1; i+=2)
			m.put (obs [i], obs [i + 1]);

		return Collections.unmodifiableMap (m);
	}


	static boolean getMoreResults (Statement stmnt)
	{
		try
		{
			return stmnt.getMoreResults ();
		}
		catch (Throwable t)
		{
			return false;
		}
	}
	

	static String xmlattrencode (String str)
	{
		str = replace (str, "\"", "&quot;");
		str = replace (str, "<", "&lt;");
		return str;
	}


	static String replace (String source, String from, String to)
	{
		if (source == null)
			return null;

		if (from.equals (to))
			return source;

		StringBuffer replaced = new StringBuffer ();

		int index = -1;
		while ((index = source.indexOf (from)) != -1)
		{
			replaced.append (source.substring (0, index));
			replaced.append (to);
			source = source.substring (index + from.length ());
		}

		replaced.append (source);

		return replaced.toString ();
	}


	/** 
	 *  Split the line based on spaces, asserting that the
	 *  number of words is correct.
	 *  
	 *  @param  line		the line to split
	 *  @param  assertLen	the number of words to assure
	 *  @param  usage		the message to output if there are an incorrect
	 *  					number of words.
	 *  @return  the split lines, or null if the assertion failed.
	 */
	String [] split (String line, int assertLen, String usage)
	{
		String [] ret = split (line);

		if (ret.length != assertLen)
		{
			error (usage);
			return null;
		}

		return ret;
	}


	/** 
	 *  Wrap the specified string by breaking on space characters.
	 *  
	 *  @param  toWrap		the string to wrap
	 *  @param  len			the maximum length of any line
	 *  @param  start		the number of spaces to pad at the
	 *  					beginning of a line
	 *  @return  the wrapped string
	 */
	String wrap (String toWrap, int len, int start)
	{
		String cur = toWrap;
		StringBuffer buff = new StringBuffer ();
		StringBuffer line = new StringBuffer ();

		char [] head = new char [start];
		Arrays.fill (head, ' ');

		for (StringTokenizer tok = new StringTokenizer (toWrap, " ");
			tok.hasMoreTokens (); )
		{
			String next = tok.nextToken ();
			if (line.length () + next.length () > len)
			{
				buff.append (line).append (sep).append (head);
				line.setLength (0);
			}

			line.append (line.length () == 0 ? "" : " ").append (next);
		}

		buff.append (line);

		return buff.toString ();
	}


	/** 
	 *  Output a progress indicator to the console.
	 *  
	 *  @param  cur  the current progress
	 *  @param  max  the maximum progress, or -1 if unknown
	 */
	void progress (int cur, int max)
	{
		StringBuffer out = new StringBuffer ();

		if (lastProgress != null)
		{
			char [] back = new char [lastProgress.length ()];
			Arrays.fill (back, '\b');
			out.append (back);
		}

		String progress = cur + "/" + (max == -1 ? "?" : "" + max) + " "
			+ (max == -1 ? "(??%)"
				: ("(" + (cur * 100 / (max == 0 ? 1 : max)) + "%)"));;

		if (cur >= max && max != -1)
		{
			progress += " " + loc ("done") + sep;
			lastProgress = null;
		}
		else
		{
			lastProgress = progress;
		}

		out.append (progress);

		System.out.print (out.toString ());
		System.out.flush ();
	}

	///////////////////////////////
	// Exception handling routines
	///////////////////////////////

	void handleException (Throwable e)
	{
		while (e instanceof InvocationTargetException)
		{
			e = ((InvocationTargetException)e).getTargetException ();
		}

		if (e instanceof SQLException)
		{
			handleSQLException ((SQLException)e);
		}
		else if (!(opts.getVerbose ()))
		{
			if (e.getMessage () == null)
				error (e.getClass ().getName ());
			else
				error (e.getMessage ());
		}
		else
		{
			e.printStackTrace (System.err);
		}
	}


	void handleSQLException (SQLException e)
	{
		if (e instanceof SQLWarning && !(opts.getShowWarnings ()))
			return;

		String type = e instanceof SQLWarning ? loc ("Warning") : loc ("Error");

		error (loc (e instanceof SQLWarning ? "Warning" : "Error",
			new Object [] {
				e.getMessage () == null ? "" : e.getMessage ().trim (),
				e.getSQLState () == null ? "" : e.getSQLState ().trim (),
				new Integer (e.getErrorCode ()) }));

		if (opts.getVerbose ())
			e.printStackTrace ();
	}


	private boolean scanForDriver (String url)
	{
		try
		{
			// already registered
			if (findRegisteredDriver (url) != null)
				return true;

			// first try known drivers...
			scanDrivers (true);

			if (findRegisteredDriver (url) != null)
				return true;

			// now really scan...
			scanDrivers (false);

			if (findRegisteredDriver (url) != null)
				return true;

			return false;
		}
		catch (Exception e)
		{
			debug (e.toString ());
			return false;
		}
	}


	private Driver findRegisteredDriver (String url)
	{
		for (Enumeration drivers = DriverManager.getDrivers ();
			drivers != null && drivers.hasMoreElements (); )
		{
			Driver driver = (Driver)drivers.nextElement ();
			try
			{
				if (driver.acceptsURL (url))
					return driver;
			}
			catch (Exception e)
			{
			}
		}

		return null;
	}


	Driver [] scanDrivers (String line)
		throws IOException
	{
		return scanDrivers (false);
	}


	Driver [] scanDrivers (boolean knownOnly)
		throws IOException
	{
		long start = System.currentTimeMillis ();

		Set classNames = new HashSet ();
		
		if (!knownOnly)
			classNames.addAll (Arrays.asList (
				ClassNameCompletor.getClassNames ()));

		classNames.addAll (KNOWN_DRIVERS);

		Set driverClasses = new HashSet ();

		for (Iterator i = classNames.iterator (); i.hasNext (); )
		{
			String className = i.next ().toString ();

			if (className.toLowerCase ().indexOf ("driver") == -1)
				continue;

			try
			{
				Class c = Class.forName (className, false,
					Thread.currentThread ().getContextClassLoader ());
				if (!Driver.class.isAssignableFrom (c))
					continue;

				if (Modifier.isAbstract (c.getModifiers ()))
					continue;

				// now instantiate and initialize it
				driverClasses.add (c.newInstance ());
			}
			catch (Throwable t)
			{
			}
		}
		info ("scan complete in "
			+ (System.currentTimeMillis () - start) + "ms");
		return (Driver [])driverClasses.toArray (new Driver [0]);
	}


	Driver [] scanDriversOLD (String line)
	{
		long start = System.currentTimeMillis ();

		Set paths = new HashSet ();
		Set driverClasses = new HashSet ();

		for (StringTokenizer tok = new StringTokenizer (
			System.getProperty ("java.ext.dirs"),
			System.getProperty ("path.separator"));
			tok.hasMoreTokens (); )
		{
			File [] files = new File (tok.nextToken ()).listFiles ();
			for (int i = 0; files != null && i < files.length; i++)
				paths.add (files [i].getAbsolutePath ());
		}

		for (StringTokenizer tok = new StringTokenizer (
			System.getProperty ("java.class.path"),
			System.getProperty ("path.separator"));
			tok.hasMoreTokens (); )
		{
			paths.add (new File (tok.nextToken ()).getAbsolutePath ());
		}


		for (Iterator i = paths.iterator (); i.hasNext (); )
		{
			File f = new File ((String)i.next ());
			output (color ().pad (loc ("scanning", f.getAbsolutePath ()), 60),
				false);

			try
			{
				ZipFile zf = new ZipFile (f);
				int total = zf.size ();
				int index = 0;

				for (Enumeration enum = zf.entries ();
					enum.hasMoreElements (); )
				{
					ZipEntry entry = (ZipEntry)enum.nextElement ();
					String name = entry.getName ();
					progress (index++, total);

					if (name.endsWith (".class"))
					{
						name = name.replace ('/', '.');
						name = name.substring (0, name.length () - 6);

						try
						{
							// check for the string "driver" in the class
							// to see if we should load it. Not perfect, but
							// it is far too slow otherwise.
							if (name.toLowerCase ().indexOf ("driver") != -1)
							{
								Class c = Class.forName (name, false,
									getClass ().getClassLoader ());
								if (Driver.class.isAssignableFrom (c)
									&& !(Modifier.isAbstract (
										c.getModifiers ())))
								{
									try
									{
										// load and initialize
										Class.forName (name);
									}
									catch (Exception e)
									{
									}
									driverClasses.add (c.newInstance ());
								}
							}
						}
						catch (Throwable t) { }
					}
				}

				progress (total, total);
			}
			catch (Exception e) { }
		}

		info ("scan complete in "
			+ (System.currentTimeMillis () - start) + "ms");
		return (Driver [])driverClasses.toArray (new Driver [0]);
	}


	///////////////////////////////////////
	// ResultSet output formatting classes
	///////////////////////////////////////


	
	int print (ResultSet rs)
		throws SQLException
	{
		String format = opts.getOutputFormat ();
		OutputFormat f = (OutputFormat)formats.get (format);

		if (f == null)
		{
			error (loc ("unknown-format", new Object [] {
				format, formats.keySet () }));
			f = new TableOutputFormat ();
		}

		return f.print (new Rows (rs));
	}


	interface OutputFormat
	{
		int print (Rows rows);
	}


	/** 
	 *  Abstract OutputFormat.
	 *  
	 *  @author  <a href="mailto:marc@apocalypse.org">Marc Prud'hommeaux</a>
	 */
	abstract class AbstractOutputFormat
		implements OutputFormat
	{
		public int print (Rows rows)
		{
			int count = 0;
			Iterator i = rows.iterator ();
			Rows.Row header = (Rows.Row)i.next ();

			printHeader (header);

			while (i.hasNext ())
			{
				printRow (rows, header, (Rows.Row)i.next ());
				count++;
			}

			printFooter (header);

			return count;
		}


		abstract void printHeader (Rows.Row header);
		abstract void printFooter (Rows.Row header);
		abstract void printRow (Rows rows, Rows.Row header, Rows.Row row);
	}


	class XMLAttributeOutputFormat
		extends AbstractOutputFormat
	{
		public void printHeader (Rows.Row header)
		{
			output ("<resultset>");
		}


		public void printFooter (Rows.Row header)
		{
			output ("</resultset>");
		}


		public void printRow (Rows rows, Rows.Row header, Rows.Row row)
		{
			String [] head = header.values;
			String [] vals = row.values;

			StringBuffer result = new StringBuffer ("  <result");

			for (int i = 0; i < head.length && i < vals.length; i++)
			{
				result.append (' ').append (head [i]).append ("=\"")
					.append (xmlattrencode (vals [i])).append ('"');
			}

			result.append ("/>");

			output (result.toString ());
		}
	}


	class XMLElementOutputFormat
		extends AbstractOutputFormat
	{
		public void printHeader (Rows.Row header)
		{
			output ("<resultset>");
		}


		public void printFooter (Rows.Row header)
		{
			output ("</resultset>");
		}


		public void printRow (Rows rows, Rows.Row header, Rows.Row row)
		{
			String [] head = header.values;
			String [] vals = row.values;

			output ("  <result>");
			for (int i = 0; i < head.length && i < vals.length; i++)
			{
				output ("    <" + head [i] + ">"
					+ (xmlattrencode (vals [i]))
					+ "</" + head [i] + ">");
			}

			output ("  </result>");
		}
	}


	/** 
	 *  OutputFormat for vertical column name: value format.
	 *  
	 *  @author  <a href="mailto:marc@apocalypse.org">Marc Prud'hommeaux</a>
	 */
	class VerticalOutputFormat
		implements OutputFormat
	{
		public int print (Rows rows)
		{
			int count = 0;
			Iterator i = rows.iterator ();
			Rows.Row header = (Rows.Row)i.next ();

			while (i.hasNext ())
			{
				printRow (rows, header, (Rows.Row)i.next ());
				count++;
			}

			return count;
		}


		public void printRow (Rows rows, Rows.Row header, Rows.Row row)
		{
			String [] head = header.values;
			String [] vals = row.values;
			int headwidth = 0;
			for (int i = 0; i < head.length && i < vals.length; i++)
				headwidth = Math.max (headwidth, head [i].length ());

			headwidth += 2;

			for (int i = 0; i < head.length && i < vals.length; i++)
				output (color ().bold (
					color ().pad (head [i], headwidth).getMono ())
					.append (vals [i] == null ? "" : vals [i]));

			output (""); // spacing
		}
	}


	/** 
	 *  OutputFormat for values separated by a delimiter.
	 *
	 *  <strong>TODO</strong>: Handle character escaping
	 *  
	 *  @author  <a href="mailto:marc@apocalypse.org">Marc Prud'hommeaux</a>
	 */
	class SeparatedValuesOutputFormat
		implements OutputFormat
	{
		private char separator;

		public SeparatedValuesOutputFormat (char separator)
		{
			setSeparator (separator);
		}


		public int print (Rows rows)
		{
			int count = 0;
			Iterator i = rows.iterator ();
			while (i.hasNext ())
			{
				printRow (rows, (Rows.Row)i.next ());
				count++;
			}

			return count - 1; // sans header row
		}


		public void printRow (Rows rows, Rows.Row row)
		{
			String [] vals = row.values;
			StringBuffer buf = new StringBuffer ();
			for (int i = 0; i < vals.length; i++)
				buf.append (buf.length () == 0 ? "" : "" + getSeparator ())
					.append ('\'')
					.append (vals [i] == null ? "" : vals [i])
					.append ('\'');
			output (buf.toString ());
		}


		public void setSeparator (char separator)
		{
			this.separator = separator;
		}


		public char getSeparator ()
		{
			return this.separator;
		}


	}


	/** 
	 *  OutputFormat for a pretty, table-like format.
	 *  
	 *  @author  <a href="mailto:marc@apocalypse.org">Marc Prud'hommeaux</a>
	 */
	class TableOutputFormat
		implements OutputFormat
	{
		public int print (Rows rows)
		{
			int index = 0;
			ColorBuffer header = null;
			ColorBuffer headerCols = null;
			final int width = opts.getMaxWidth () - 4;
	
	
			// normalize the columns sizes
			rows.normalizeWidths ();
	
			for (Iterator i = rows.iterator (); i.hasNext (); )
			{
				Rows.Row row = (Rows.Row)i.next ();
				ColorBuffer cbuf = getOutputString (rows, row);
				cbuf = cbuf.truncate (width);
	
				if (index == 0)
				{
					StringBuffer h = new StringBuffer ();
					for (int j = 0; j < row.sizes.length; j++)
					{
						for (int k = 0; k < row.sizes [j]; k++)
							h.append ('-');
						h.append ("-+-");
					}
	
					headerCols = cbuf;
					header = color ().green (h.toString ()).truncate (
						headerCols.getVisibleLength ());
				}
	
				if (index == 0 ||
					(opts.getHeaderInterval () > 0
				 	&& index % opts.getHeaderInterval () == 0
				 	&& opts.getShowHeader ()))
				{
					printRow (header, true);
					printRow (headerCols, false);
					printRow (header, true);
				}
	
				if (index != 0) // don't output the header twice
					printRow (cbuf, false);
	
				index++;
			}
	
			if (header != null && opts.getShowHeader ())
				printRow (header, true);
	
			return index - 1;
		}
	
	
		void printRow (ColorBuffer cbuff, boolean header)
		{
			if (header)
				output (color ().green ("+-").append (cbuff).green ("-+"));
			else
				output (color ().green ("| ").append (cbuff).green (" |"));
		}
	
	
		public ColorBuffer getOutputString (Rows rows, Rows.Row row)
		{
			return getOutputString (rows, row, " | ");
		}


		ColorBuffer getOutputString (Rows rows, Rows.Row row, String delim)
		{
			ColorBuffer buf = color ();

			for (int i = 0; i < row.values.length; i++)
			{
				if (buf.getVisibleLength () > 0)
					buf.green (delim);

				ColorBuffer v;
				
				if (row.isMeta)
				{
					v = color ().center (row.values [i], row.sizes [i]);
					if (rows.isPrimaryKey (i))
						buf.cyan (v.getMono ());
					else
						buf.bold (v.getMono ());
				}
				else
				{
					v = color ().pad (row.values [i], row.sizes [i]);
					if (rows.isPrimaryKey (i))
						buf.cyan (v.getMono ());
					else
						buf.append (v.getMono ());
				}
			}

			if (row.deleted) // make deleted rows red
				buf = color ().red (buf.getMono ());
			else if (row.updated) // make updated rows blue
				buf = color ().blue (buf.getMono ());
			else if (row.inserted) // make new rows green
				buf = color ().green (buf.getMono ());

			return buf;
		}
	}

	
	void runBatch (List statements)
	{
		try
		{
			Statement stmnt = con ().connection.createStatement ();
			try
			{
				for (Iterator i = statements.iterator (); i.hasNext (); )
					stmnt.addBatch (i.next ().toString ());

				int [] counts = stmnt.executeBatch ();
	
				output (color ().pad (color ().bold ("COUNT"), 8)
					.append (color ().bold ("STATEMENT")));

				for (int i = 0; counts != null && i < counts.length; i++)
					output (color ().pad (counts [i] + "", 8)
						.append (statements.get (i).toString ()));
			}
			finally
			{
				try { stmnt.close (); } catch (Exception e) { }
			}
		}
		catch (Exception e)
		{
			handleException (e);
		}
	}


	public int runCommands (List cmds)
	{
		int successCount = 0;

		try
		{
			int index = 1;
			int size = cmds.size ();
			for (Iterator i = cmds.iterator (); i.hasNext (); )
			{
				String cmd = i.next ().toString ();
				info (color ().pad ((index++) + "/" + (size), 13).append (cmd));
				boolean success = dispatch (cmd);

				// if we do not force script execution, abort
				// when a failure occurs.
				if (!success && !opts.getForce ())
				{
					error (loc ("abort-on-error", cmd));
					return successCount;
				}
				successCount += success == true ? 1 : 0;
			}
		}
		catch (Exception e)
		{
			handleException (e);
		}

		return successCount;
	}


	class Rows
		extends LinkedList
	{
		final ResultSetMetaData rsMeta;
		final Boolean [] primaryKeys;

		Rows (ResultSet rs)
			throws SQLException
		{
			this.rsMeta = rs.getMetaData ();

			int count = rsMeta.getColumnCount ();
			primaryKeys = new Boolean [count];

			add (new Row (count));

			while (rs.next ())
				add (new Row (count, rs));
		}


		public int getRowCount ()
		{
			// the number of rows minus the header
			return size () - 1;
		}


		/** 
		 *  Return whether the specified column (0-based index) is
		 *  a primary key. Since this method depends on whether the
		 *  JDBC driver property implements
		 *  {@link ResultSetMetaData#getTableName} (many do not), it
		 *  is not reliable for all databases.
		 */
		boolean isPrimaryKey (int col)
		{
			if (primaryKeys [col] != null)
				return primaryKeys [col].booleanValue ();

			try
			{
				// this doesn't always work, since some JDBC drivers (e.g.,
				// Oracle's) return a blank string from getTableName.
				String table = rsMeta.getTableName (col + 1);
				String column = rsMeta.getColumnName (col + 1);

				if (table == null || table.length () == 0 ||
						column == null || column.length () == 0)
				{
					return (primaryKeys [col] = new Boolean (false))
						.booleanValue ();
				}

				ResultSet pks = con ().meta.getPrimaryKeys (
					con ().meta.getConnection ().getCatalog (), null, table);

				try
				{
					while (pks.next ())
					{
						if (column.equalsIgnoreCase (
							pks.getString ("COLUMN_NAME")))
							return (primaryKeys [col] = new Boolean (true))
								.booleanValue ();
					}
				}
				finally
				{
					pks.close ();
				}

				return (primaryKeys [col] = new Boolean (false))
					.booleanValue ();
			}
			catch (SQLException sqle)
			{
				return (primaryKeys [col] = new Boolean (false))
					.booleanValue ();
			}
		}


		/** 
		 *  Update all of the rows to have the same size, set to the
		 *  maximum length of each column in the Rows. 
		 */
		void normalizeWidths ()
		{
			int [] max = null;
			for (int i = 0; i < size (); i++)
			{
				Row row = (Row)get (i);
				if (max == null)
					max = new int [row.values.length];

				for (int j = 0; j < max.length; j++)
				{
					max [j] = Math.max (max [j], row.sizes [j] + 1);
				}
			}

			for (int i = 0; i < size (); i++)
			{
				Row row = (Row)get (i);
				row.sizes = max;
			}
		}


		class Row
		{
			final String [] values;
			final boolean isMeta;
			private boolean deleted;
			private boolean inserted;
			private boolean updated;
			private int [] sizes;
	

			Row (int size)
				throws SQLException
			{
				isMeta = true;
				values = new String [size];
				sizes = new int [size];
				for (int i = 0; i < size; i++)
				{
					values [i] = rsMeta.getColumnLabel (i + 1);
					sizes [i] = values [i] == null ? 1 : values [i].length ();
				}

				deleted = false;
				updated = false;
				inserted = false;
			}


			Row (int size, ResultSet rs)
				throws SQLException
			{
				isMeta = false;
				values = new String [size];
				sizes = new int [size];
	
				try { deleted = rs.rowDeleted (); } catch (Throwable t) { }
				try { updated = rs.rowUpdated (); } catch (Throwable t) { }
				try { inserted = rs.rowInserted (); } catch (Throwable t) { }

				for (int i = 0; i < size; i++)
				{
					values [i] = rs.getString (i + 1);
					sizes [i] = values [i] == null ? 1 : values [i].length ();
				}
			}
		}
	}


	///////////////////////////////
	// Console interaction classes
	///////////////////////////////

	/** 
	 *  A buffer that can output segments using ANSI color.
	 *  
	 *  @author  <a href="mailto:marc@apocalypse.org">Marc Prud'hommeaux</a>
	 */
	final static class ColorBuffer
		implements Comparable
	{
		private static final ColorAttr BOLD =      new ColorAttr ("\033[1m");
		private static final ColorAttr NORMAL =    new ColorAttr ("\033[m");
		private static final ColorAttr REVERS =    new ColorAttr ("\033[7m");
		private static final ColorAttr LINED =     new ColorAttr ("\033[4m");
		private static final ColorAttr GREY =      new ColorAttr ("\033[1;30m");
		private static final ColorAttr RED =       new ColorAttr ("\033[1;31m");
		private static final ColorAttr GREEN =     new ColorAttr ("\033[1;32m");
		private static final ColorAttr BLUE =      new ColorAttr ("\033[1;34m");
		private static final ColorAttr CYAN =      new ColorAttr ("\033[1;36m");
		private static final ColorAttr YELLOW =    new ColorAttr ("\033[1;33m");
		private static final ColorAttr MAGENTA =   new ColorAttr ("\033[1;35m");
		private static final ColorAttr INVISIBLE = new ColorAttr ("\033[8m");

		private final List parts = new LinkedList ();

		private final boolean useColor;


		public ColorBuffer (boolean useColor)
		{
			this.useColor = useColor;

			append ("");
		}


		public ColorBuffer (String str, boolean useColor)
		{
			this.useColor = useColor;

			append (str);
		}


		/** 
		 *  Pad the specified String with spaces to the indicated length
		 *  
		 *  @param  str  the String to pad
		 *  @param  len  the length we want the return String to be
		 *  @return  the passed in String with spaces appended until the
		 *  		length matches the specified length.
		 */
		ColorBuffer pad (ColorBuffer str, int len)
		{
			while (str.getVisibleLength () < len)
				str.append (" ");
	
			return append (str);
		}
	
	
		ColorBuffer center (String str, int len)
		{
			StringBuffer buf = new StringBuffer (str);
	
			while (buf.length () < len)
			{
				buf.append (" ");
	
				if (buf.length () < len)
					buf.insert (0, " ");
			}
	
			return append (buf.toString ());
		}
	
	
		ColorBuffer pad (String str, int len)
		{
			if (str == null)
				str = "";
	
			return pad (new ColorBuffer (str, false), len);
		}
	
	
		public String getColor ()
		{
			return getBuffer (useColor);
		}


		public String getMono ()
		{
			return getBuffer (false);
		}


		String getBuffer (boolean color)
		{
			StringBuffer buf = new StringBuffer ();
			for (Iterator i = parts.iterator (); i.hasNext (); )
			{
				Object next = i.next ();
				if (!color && next instanceof ColorAttr)
					continue;

				buf.append (next.toString ());
			}

			return buf.toString ();
		}


		/** 
		 *  Truncate the ColorBuffer to the specified length and return
		 *  the new ColorBuffer. Any open color tags will be closed.
		 */
		public ColorBuffer truncate (int len)
		{
			ColorBuffer cbuff = new ColorBuffer (useColor);
			ColorAttr lastAttr = null;
			for (Iterator i = parts.iterator ();
				cbuff.getVisibleLength () < len && i.hasNext (); )
			{
				Object next = i.next ();
				if (next instanceof ColorAttr)
				{
					lastAttr = (ColorAttr)next;
					cbuff.append ((ColorAttr)next);
					continue;
				}

				String val = next.toString ();
				if (cbuff.getVisibleLength () + val.length () > len)
				{
					int partLen = len - cbuff.getVisibleLength ();
					val = val.substring (0, partLen);
				}

				cbuff.append (val);
			}

			// close off the buffer with a normal tag
			if (lastAttr != null && lastAttr != NORMAL)
			{
				cbuff.append (NORMAL);
			}

			return cbuff;
		}


		public String toString ()
		{
			return getColor ();
		}


		public ColorBuffer append (String str)
		{
			parts.add (str);
			return this;
		}


		public ColorBuffer append (ColorBuffer buf)
		{
			parts.addAll (buf.parts);
			return this;
		}


		public ColorBuffer append (ColorAttr attr)
		{
			parts.add (attr);
			return this;
		}


		public int getVisibleLength ()
		{
			return getMono ().length ();
		}


		public ColorBuffer append (ColorAttr attr, String val)
		{
			parts.add (attr);
			parts.add (val);
			parts.add (NORMAL);
			return this;
		}


		public ColorBuffer bold (String str)
		{
			return append (BOLD, str);
		}


		public ColorBuffer lined (String str)
		{
			return append (LINED, str);
		}


		public ColorBuffer grey (String str)
		{
			return append (GREY, str);
		}


		public ColorBuffer red (String str)
		{
			return append (RED, str);
		}


		public ColorBuffer blue (String str)
		{
			return append (BLUE, str);
		}


		public ColorBuffer green (String str)
		{
			return append (GREEN, str);
		}


		public ColorBuffer cyan (String str)
		{
			return append (CYAN, str);
		}


		public ColorBuffer yellow (String str)
		{
			return append (YELLOW, str);
		}


		public ColorBuffer magenta (String str)
		{
			return append (MAGENTA, str);
		}


		private static class ColorAttr
		{
			private final String attr;
	
			public ColorAttr (String attr)
			{
				this.attr = attr;
			}
	

			public String toString ()
			{
				return attr;
			}
		}


		public int compareTo (Object other)
		{
			return getMono ().compareTo (((ColorBuffer)other).getMono ());
		}
	}


	////////////////////////////
	// Command handling classes
	////////////////////////////

	/** 
	 *  A generic command to be executed. Execution of the command
	 *  should be dispatched to the {@link #execute(java.lang.String)}
	 *  method after determining that the command is appropriate with
	 *  the {@link #matches(java.lang.String)} method.
	 *  
	 *  @author  <a href="mailto:marc@apocalypse.org">Marc Prud'hommeaux</a>
	 */
	interface CommandHandler
	{
		/** 
		 *  @return  the name of the command
		 */
		public String getName ();


		/** 
		 *  @return  all the possible names of this command.
		 */
		public String [] getNames ();


		/** 
		 *  @return  the short help description for this command.
		 */
		public String getHelpText ();


		/** 
		 *  Check to see if the specified string can be dispatched to this
		 *  command.
		 *  
		 *  @param  line  the command line to check.
		 *  @return the command string that matches, or null if it no match
		 */
		public String matches (String line);


		/** 
		 *  Execute the specified command.
		 *  
		 *  @param  line  the full command line to execute.
		 */
		public boolean execute (String line);


		/** 
		 *  Returns the completors that can handle parameters.
		 */
		public Completor [] getParameterCompletors ();
	}


	/** 
	 *  An abstract implementation of CommandHandler.
	 *  
	 *  @author  <a href="mailto:marc@apocalypse.org">Marc Prud'hommeaux</a>
	 */
	public abstract class AbstractCommandHandler
		implements CommandHandler
	{
		private final String name;
		private final String [] names;
		private final String helpText;
		private Completor [] parameterCompletors = new Completor [0];


		public AbstractCommandHandler (String [] names, String helpText,
			Completor [] completors)
		{
			this.name = names [0];
			this.names = names;
			this.helpText = helpText;
			if (completors == null || completors.length == 0)
			{
				this.parameterCompletors = new Completor [] {
					new NullCompletor () };
			}
			else
			{
				List c = new LinkedList (Arrays.asList (completors));
				c.add (new NullCompletor ());
				this.parameterCompletors =
					(Completor [])c.toArray (new Completor [0]);
			}
		}


		public String getHelpText ()
		{
			return helpText;
		}


		public String getName ()
		{
			return this.name;
		}


		public String [] getNames ()
		{
			return this.names;
		}


		public String matches (String line)
		{
			if (line == null || line.length () == 0)
				return null;

			String [] parts = split (line);
			if (parts == null || parts.length == 0)
				return null;

			for (int i = 0; i < names.length; i++)
			{
				if (names [i].startsWith (parts [0]))
					return names [i];
			}


			return null;
		}



		public void setParameterCompletors (Completor [] parameterCompletors)
		{
			this.parameterCompletors = parameterCompletors;
		}


		public Completor [] getParameterCompletors ()
		{
			return this.parameterCompletors;
		}
	}


	/** 
	 *  A {@link Command} implementation that uses reflection to
	 *  determine the method to dispatch the command.
	 *  
	 *  @author  <a href="mailto:marc@apocalypse.org">Marc Prud'hommeaux</a>
	 */
	public class ReflectiveCommandHandler
		extends AbstractCommandHandler
	{
		public ReflectiveCommandHandler (String [] cmds, Completor [] completor)
		{
			super (cmds, loc ("help-" + cmds [0]), completor);
		}


		public boolean execute (String line)
		{
			try
			{
				Object ob = command.getClass ().getMethod (getName (),
					new Class [] { String.class })
					.invoke (command, new Object [] { line });
				return ob != null && ob instanceof Boolean
					&& ((Boolean)ob).booleanValue ();
			}
			catch (Throwable e)
			{
				return error (e);
			}
		}
	}


	//////////////////////////
	// Command methods follow
	//////////////////////////

	public class Commands
	{
		public boolean metadata (String line)
		{
			debug (line);

			String [] parts = split (line);
			List params = new LinkedList (Arrays.asList (parts));
			params.remove (0);
			params.remove (0);
			debug (params.toString ());
			return metadata (parts [1],
				(String [])params.toArray (new String [0]));
		}


		public boolean metadata (String cmd, String [] args)
		{
			try
			{
				Method [] m = con ().meta.getClass ().getMethods ();
				Set methodNames = new TreeSet ();
				Set methodNamesUpper = new TreeSet ();
				for (int i = 0; i < m.length; i++)
				{
					methodNames.add (m [i].getName ());
					methodNamesUpper.add (m [i].getName ().toUpperCase ());
				}

				if (!methodNamesUpper.contains (cmd.toUpperCase ()))
				{
					error (loc ("no-such-method", cmd));
					error (loc ("possible-methods"));
					for (Iterator i = methodNames.iterator (); i.hasNext (); )
						error ("   " + i.next ());
					return false;
				}

				Object res = Reflector.invoke (con ().meta, cmd,
					Arrays.asList (args));

				if (res instanceof ResultSet)
				{
					ResultSet rs = (ResultSet)res;

					if (rs != null)
					{
						try
						{
							print (rs);
						}
						finally
						{
							rs.close ();
						}
					}
				}
				else if (res != null)
				{
					output (res.toString ());
				}
			}
			catch (Exception e)
			{
				return error (e);
			}

			return true;
		}


		public boolean history (String line)
		{
			List hist = reader.getHistory ().getHistoryList ();
			int index = 1;
			for (Iterator i = hist.iterator (); i.hasNext (); index++)
			{
				output (color ().pad (index + ".", 6)
					.append (i.next ().toString ()));
			}

			return true;
		}


		String arg1 (String line, String paramname)
		{
			return arg1 (line, paramname, null);
		}


		String arg1 (String line, String paramname, String def)
		{
			String [] ret = split (line);

			if (ret == null || ret.length != 2)
			{
				if (def != null)
					return def;

				throw new IllegalArgumentException (loc ("arg-usage",
					new Object [] { ret.length == 0 ? "" : ret [0],
						paramname }));
			}

			return ret [1];
		}


		public boolean indexes (String line)
			throws Exception
		{
			return metadata ("getIndexInfo", new String [] {
				conn ().getCatalog (), null,
				arg1 (line, "table name"),
				false + "",
				true + "" });
		}


		public boolean primarykeys (String line)
			throws Exception
		{
			return metadata ("getPrimaryKeys", new String [] {
				conn ().getCatalog (), null,
				arg1 (line, "table name"), });
		}


		public boolean exportedkeys (String line)
			throws Exception
		{
			return metadata ("getExportedKeys", new String [] {
				conn ().getCatalog (), null,
				arg1 (line, "table name"), });
		}


		public boolean importedkeys (String line)
			throws Exception
		{
			return metadata ("getImportedKeys", new String [] {
				conn ().getCatalog (), null,
				arg1 (line, "table name"), });
		}


		public boolean procedures (String line)
			throws Exception
		{
			return metadata ("getProcedures", new String [] {
				conn ().getCatalog (), null,
				arg1 (line, "procedure name pattern", "%"), });
		}


		public boolean tables (String line)
			throws Exception
		{
			return metadata ("getTables", new String [] {
				conn ().getCatalog (), null,
				arg1 (line, "table name", "%"), null });
		}


		public boolean columns (String line)
			throws Exception
		{
			return metadata ("getColumns", new String [] {
				conn ().getCatalog (), null,
				arg1 (line, "table name"), "%" });
		}


		public boolean dropall (String line)
		{
			if (con () == null || con ().url == null)
				return error (loc ("no-current-connection"));
	

			try
			{
				if (!(reader.readLine (loc ("really-drop-all")).equals ("y")))
					return error ("abort-drop-all");

				List cmds = new LinkedList ();
				ResultSet rs = getTables (); 
				try
				{
					while (rs.next ())
						cmds.add ("DROP TABLE "
							+ rs.getString ("TABLE_NAME") + ";");
				}
				finally
				{
					try { rs.close (); } catch (Exception e) { }
				}

				// run as a batch
				return runCommands (cmds) == cmds.size ();
			}
			catch (Exception e)
			{
				return error (e);
			}
		}


		public boolean reconnect (String line)
		{
			if (con () == null || con ().url == null)
				return error (loc ("no-current-connection"));
	
			info (loc ("reconnecting", con ().url));
			try
			{
				con ().reconnect ();
			}
			catch (Exception e)
			{
				return error (e);
			}

			return true;
		}


		public boolean scan (String line)
			throws IOException
		{
			TreeSet names = new TreeSet ();

			if (drivers == null)
				drivers = Arrays.asList (scanDrivers (line));

			info (loc ("drivers-found-count", drivers.size ()));

			// unique the list
			for (Iterator i = drivers.iterator (); i.hasNext (); )
				names.add (((Driver)i.next ()).getClass ().getName ());

			output (color ()
				.bold (color ().pad (loc ("compliant"), 10).getMono ())
				.bold (color ().pad (loc ("jdbc-version"), 8).getMono ())
				.bold (color (loc ("driver-class")).getMono ()));

			for (Iterator i = names.iterator (); i.hasNext (); )
			{
				String name = i.next ().toString ();


				try
				{
					Driver driver = (Driver)Class.forName (name).newInstance ();
					ColorBuffer msg = color ()
						.pad (driver.jdbcCompliant () ? "yes" : "no", 10)
						.pad (driver.getMajorVersion () + "."
							+ driver.getMinorVersion (), 8)
						.append (name);
					if (driver.jdbcCompliant ())
						output (msg);
					else
						output (color ().red (msg.getMono ()));
				}
				catch (Throwable t)
				{
					output (color ().red (name)); // error with driver
				}
			}

			return true;
		}


		public boolean save (String line)
			throws IOException
		{
			info (loc ("saving-options", opts.rcFile));
			opts.save ();
			return true;
		}
	
	
		public boolean load (String line)
			throws IOException
		{
			opts.load ();
			info (loc ("loaded-options", opts.rcFile));
			return true;
		}
	
	
		public boolean config (String line)
		{
			try
			{
				Properties props = opts.toProperties ();
				Set keys = new TreeSet (props.keySet ());
				for (Iterator i = keys.iterator (); i.hasNext (); )
				{
					String key = (String)i.next ();
					output (color ()
						.green (color ().pad (key.substring (
							opts.PROPERTY_PREFIX.length ()), 20)
							.getMono ())
						.append (props.getProperty (key)));
				}
			}
			catch (Exception e)
			{
				return error (e);
			}

			return true;
		}
	
	
		public boolean set (String line)
		{
			if (line == null || line.trim ().equals ("set")
				|| line.length () == 0)
			{
				return config (null);
			}
	
			String [] parts = split (line, 3, "Usage: set <key> <value>");
			if (parts == null)
				return false;
	
			String key = parts [1];
			String value = parts [2];
			boolean success = opts.set (key, value, false);
			// if we autosave, then save
			if (success && opts.getAutosave ())
			{
				try
				{
					opts.save ();
				}
				catch (Exception saveException)
				{
				}
			}

			return success;
		}
	
	
		public boolean commit (String line)
			throws SQLException
		{
			if (!(assertConnection ()))
				return false;
			if (!(assertAutoCommit ()))
				return false;
	
			try
			{
				long start = System.currentTimeMillis ();
				con ().connection.commit ();
				long end = System.currentTimeMillis ();
				showWarnings ();
				info (loc ("commit-complete")
					+ " " + loc ("time-ms", new Object [] {
						new Double ((end - start) / 1000d) }));

				return true;
			}
			catch (Exception e)
			{
				return error (e);
			}
		}
	
	
		public boolean rollback (String line)
			throws SQLException
		{
			if (!(assertConnection ()))
				return false;
			if (!(assertAutoCommit ()))
				return false;
	
			try
			{
				long start = System.currentTimeMillis ();
				con ().connection.rollback ();
				long end = System.currentTimeMillis ();
				showWarnings ();
				info (loc ("rollback-complete")
					+ " " + loc ("time-ms", new Object [] {
						new Double ((end - start) / 1000d) }));
				return true;
			}
			catch (Exception e)
			{
				return error (e);
			}
		}
	
	
		public boolean autocommit (String line)
			throws SQLException
		{
			if (!(assertConnection ()))
				return false;
	
			if (line.endsWith ("on"))
				con ().connection.setAutoCommit (true);
			else if (line.endsWith ("off"))
				con ().connection.setAutoCommit (false);
	
			showWarnings ();
			autocommitStatus (con ().connection);
			return true;
		}
	
	
		public boolean dbinfo (String line)
		{
			if (!(assertConnection ()))
				return false;
	
			showWarnings ();
			int padlen = 50;

			String [] m = new String [] {
				"allProceduresAreCallable",
				"allTablesAreSelectable",
				"dataDefinitionCausesTransactionCommit",
				"dataDefinitionIgnoredInTransactions",
				"doesMaxRowSizeIncludeBlobs",
				"getCatalogSeparator",
				"getCatalogTerm",
				"getDatabaseProductName",
				"getDatabaseProductVersion",
				"getDefaultTransactionIsolation",
				"getDriverMajorVersion",
				"getDriverMinorVersion",
				"getDriverName",
				"getDriverVersion",
				"getExtraNameCharacters",
				"getIdentifierQuoteString",
				"getMaxBinaryLiteralLength",
				"getMaxCatalogNameLength",
				"getMaxCharLiteralLength",
				"getMaxColumnNameLength",
				"getMaxColumnsInGroupBy",
				"getMaxColumnsInIndex",
				"getMaxColumnsInOrderBy",
				"getMaxColumnsInSelect",
				"getMaxColumnsInTable",
				"getMaxConnections",
				"getMaxCursorNameLength",
				"getMaxIndexLength",
				"getMaxProcedureNameLength",
				"getMaxRowSize",
				"getMaxSchemaNameLength",
				"getMaxStatementLength",
				"getMaxStatements",
				"getMaxTableNameLength",
				"getMaxTablesInSelect",
				"getMaxUserNameLength",
				"getNumericFunctions",
				"getProcedureTerm",
				"getSchemaTerm",
				"getSearchStringEscape",
				"getSQLKeywords",
				"getStringFunctions",
				"getSystemFunctions",
				"getTimeDateFunctions",
				"getURL",
				"getUserName",
				"isCatalogAtStart",
				"isReadOnly",
				"nullPlusNonNullIsNull",
				"nullsAreSortedAtEnd",
				"nullsAreSortedAtStart",
				"nullsAreSortedHigh",
				"nullsAreSortedLow",
				"storesLowerCaseIdentifiers",
				"storesLowerCaseQuotedIdentifiers",
				"storesMixedCaseIdentifiers",
				"storesMixedCaseQuotedIdentifiers",
				"storesUpperCaseIdentifiers",
				"storesUpperCaseQuotedIdentifiers",
				"supportsAlterTableWithAddColumn",
				"supportsAlterTableWithDropColumn",
				"supportsANSI92EntryLevelSQL",
				"supportsANSI92FullSQL",
				"supportsANSI92IntermediateSQL",
				"supportsBatchUpdates",
				"supportsCatalogsInDataManipulation",
				"supportsCatalogsInIndexDefinitions",
				"supportsCatalogsInPrivilegeDefinitions",
				"supportsCatalogsInProcedureCalls",
				"supportsCatalogsInTableDefinitions",
				"supportsColumnAliasing",
				"supportsConvert",
				"supportsCoreSQLGrammar",
				"supportsCorrelatedSubqueries",
				"supportsDataDefinitionAndDataManipulationTransactions",
				"supportsDataManipulationTransactionsOnly",
				"supportsDifferentTableCorrelationNames",
				"supportsExpressionsInOrderBy",
				"supportsExtendedSQLGrammar",
				"supportsFullOuterJoins",
				"supportsGroupBy",
				"supportsGroupByBeyondSelect",
				"supportsGroupByUnrelated",
				"supportsIntegrityEnhancementFacility",
				"supportsLikeEscapeClause",
				"supportsLimitedOuterJoins",
				"supportsMinimumSQLGrammar",
				"supportsMixedCaseIdentifiers",
				"supportsMixedCaseQuotedIdentifiers",
				"supportsMultipleResultSets",
				"supportsMultipleTransactions",
				"supportsNonNullableColumns",
				"supportsOpenCursorsAcrossCommit",
				"supportsOpenCursorsAcrossRollback",
				"supportsOpenStatementsAcrossCommit",
				"supportsOpenStatementsAcrossRollback",
				"supportsOrderByUnrelated",
				"supportsOuterJoins",
				"supportsPositionedDelete",
				"supportsPositionedUpdate",
				"supportsSchemasInDataManipulation",
				"supportsSchemasInIndexDefinitions",
				"supportsSchemasInPrivilegeDefinitions",
				"supportsSchemasInProcedureCalls",
				"supportsSchemasInTableDefinitions",
				"supportsSelectForUpdate",
				"supportsStoredProcedures",
				"supportsSubqueriesInComparisons",
				"supportsSubqueriesInExists",
				"supportsSubqueriesInIns",
				"supportsSubqueriesInQuantifieds",
				"supportsTableCorrelationNames",
				"supportsTransactions",
				"supportsUnion",
				"supportsUnionAll",
				"usesLocalFilePerTable",
				"usesLocalFiles",
			};

			for (int i = 0; i < m.length; i++)
			{
				try
				{
					output (color ().pad (m [i], padlen).append (
						"" + Reflector.invoke (con ().meta,
							m [i], new Object [0])));
				}
				catch (Exception e)
				{
					handleException (e);
				}
			}

			return true;
		}
	
	
		public boolean verbose (String line)
		{
			info ("verbose: on");
			return set ("set verbose true");
		}


		public boolean outputformat (String line)
		{
			return set ("set " + line);
		}


		public boolean brief (String line)
		{
			info ("verbose: off");
			return set ("set verbose false");
		}
	
	
		public boolean isolation (String line)
			throws SQLException
		{
			if (!(assertConnection ()))
				return false;
	
			int i;
	
			if (line.endsWith ("TRANSACTION_NONE"))
				i = Connection.TRANSACTION_NONE;
			else if (line.endsWith ("TRANSACTION_READ_COMMITTED"))
				i = Connection.TRANSACTION_READ_COMMITTED;
			else if (line.endsWith ("TRANSACTION_READ_UNCOMMITTED"))
				i = Connection.TRANSACTION_READ_UNCOMMITTED;
			else if (line.endsWith ("TRANSACTION_REPEATABLE_READ"))
				i = Connection.TRANSACTION_REPEATABLE_READ;
			else if (line.endsWith ("TRANSACTION_SERIALIZABLE"))
				i = Connection.TRANSACTION_SERIALIZABLE;
			else
				return error ("Usage: isolation <TRANSACTION_NONE "
					+ "| TRANSACTION_READ_COMMITTED "
					+ "| TRANSACTION_READ_UNCOMMITTED "
					+ "| TRANSACTION_REPEATABLE_READ "
					+ "| TRANSACTION_SERIALIZABLE>");
	
			con ().connection.setTransactionIsolation (i);
	
			int isol = con ().connection.getTransactionIsolation ();
			final String isoldesc;
			switch (i)
			{
				case Connection.TRANSACTION_NONE:
					isoldesc = "TRANSACTION_NONE";
					break;
				case Connection.TRANSACTION_READ_COMMITTED:
					isoldesc = "TRANSACTION_READ_COMMITTED";
					break;
				case Connection.TRANSACTION_READ_UNCOMMITTED:
					isoldesc = "TRANSACTION_READ_UNCOMMITTED";
					break;
				case Connection.TRANSACTION_REPEATABLE_READ:
					isoldesc = "TRANSACTION_REPEATABLE_READ";
					break;
				case Connection.TRANSACTION_SERIALIZABLE:
					isoldesc = "TRANSACTION_SERIALIZABLE";
					break;
				default:
					isoldesc = "UNKNOWN";
			}
	
			info (loc ("isolation-status", isoldesc));
			return true;
		}
	
	
		public boolean batch (String line)
		{
			if (!(assertConnection ()))
				return false;

			if (batch == null)
			{
				batch = new LinkedList ();
				info (loc ("batch-start"));
				return true;
			}
			else
			{
				info (loc ("running-batch"));
				try
				{
					runBatch (batch);
					return true;
				}
				catch (Exception e)
				{
					return error (e);
				}
				finally
				{
					batch = null;
				}
			}
		}


		public boolean sql (String line)
		{
			if (line == null || line.length () == 0)
				return false; // ???
	
			// use multiple lines for statements not terminated by ";"
			try
			{
				while (!(line.trim ().endsWith (";")))
				{
					StringBuffer prompt = new StringBuffer (getPrompt ());
					for (int i = 0; i < prompt.length () - 1; i++)
					{
						if (prompt.charAt (i) != '>')
							prompt.setCharAt (i, i % 2 == 0 ? '.' : ' ');
					}

					line += " " + reader.readLine (prompt.toString ());
				}
			}
			catch (Exception e)
			{
				handleException (e);
			}

			if (line.endsWith (";"))
				line = line.substring (0, line.length () - 1);
	
			if (!(assertConnection ()))
				return false;
	
			String sql = line;
			
			if (sql.startsWith (COMMAND_PREFIX))
				sql = sql.substring (1);
	
			if (sql.startsWith ("sql"))
				sql = sql.substring (4);
	
			// batch statements?
			if (batch != null)
			{
				batch.add (sql);
				return true;
			}

			try
			{
				Statement stmnt = con ().connection.createStatement ();
				try
				{
					long start = System.currentTimeMillis ();
					boolean hasResults = stmnt.execute (sql);
					long end = System.currentTimeMillis ();

					showWarnings ();
	
					if (hasResults)
					{
						do
						{
							ResultSet rs = stmnt.getResultSet ();
							try
							{
								int count = print (rs);
	
								info (loc ("rows-selected", count) + " "
									+ loc ("time-ms", new Object [] {
										new Double ((end - start) / 1000d) }));
							}
							finally
							{
								rs.close ();
							}
						} while (getMoreResults (stmnt));
					}
					else
					{
						int count = stmnt.getUpdateCount ();
						info (loc ("rows-affected", count)
							+ " " + loc ("time-ms", new Object [] {
								new Double ((end - start) / 1000d) }));
					}
				}
				finally
				{
					stmnt.close ();
				}
			}
			catch (Exception e)
			{
				return error (e);
			}
	
			showWarnings ();
			return true;
		}


		public boolean quit (String line)
		{
			exit = true;
			close (null);
			return true;
		}
	
	
		public boolean close (String line)
		{
			if (con () == null)
				return false;
	
			try
			{
				if (con ().connection != null
					&& !(con ().connection.isClosed ()))
				{
					info (loc ("closing",
						con ().connection.getClass ().getName ()));
					con ().connection.close ();
				}
				else
				{
					info (loc ("already-closed"));
				}
			}
			catch (Exception e)
			{
				return error (e);
			}
	
			connections.remove ();
			return true;
		}


		public boolean properties (String line)
			throws Exception
		{
			String example = "";
			example += "Usage: properties <properties file>" + sep;
	
			String [] parts = split (line);
			if (parts.length < 2)
				return error (example);
	
			int successes = 0;

			for (int i = 1; i < parts.length; i++)
			{
				Properties props = new Properties ();
				props.load (new FileInputStream (parts[i]));
				if (connect (props))
					successes++;
			}

			if (successes != (parts.length - 1))
				return false;
			else
				return true;
		}
	
	
		public boolean connect (String line)
			throws Exception
		{
			String example = "";
			example += "Usage: connect <url> <username> <password> [driver]"
				+ sep;
	
			String [] parts = split (line);
			if (parts == null)
				return false;
	
			if (parts.length < 2)
				return error (example);
	
			/*
			if (parts.length == 2)
			{
				String cname = nameToDriverClass (parts [1]);
				if (cname != null)
					return connectByPrompt (cname);
			}
			*/


			String url = parts.length < 2 ? null : parts [1];
			String user = parts.length < 3 ? null : parts [2];
			String pass = parts.length < 4 ? null : parts [3];
			String driver = parts.length < 5 ? null : parts [4];
	
			Properties props = new Properties ();
			if (url != null)
				props.setProperty ("url", url);
			if (driver != null)
				props.setProperty ("driver", driver);
			if (user != null)
				props.setProperty ("user", user);
			if (pass != null)
				props.setProperty ("password", pass);

			return connect (props);
		}


		public boolean connect (Properties props)
			throws IOException
		{
			String url = props.getProperty ("url",
				props.getProperty ("javax.jdo.option.ConnectionURL"));
			String driver = props.getProperty ("driver",
				props.getProperty ("javax.jdo.option.ConnectionDriverName"));
			String username = props.getProperty ("user",
				props.getProperty ("javax.jdo.option.ConnectionUserName"));
			String password = props.getProperty ("password",
				props.getProperty ("javax.jdo.option.ConnectionPassword"));

			if (url == null || url.length () == 0)
				return error ("Property \"url\" is required");
			if (driver == null || driver.length () == 0)
			{
				if (!scanForDriver (url))
					return error (loc ("no-driver", url));
			}

			info ("Connecting to " + url);

			if (username == null)
				username = reader.readLine ("Enter username for " + url + ": ");
			if (password == null)
				password = reader.readLine ("Enter password for " + url + ": ",
					new Character ('*'));

			try
			{
				// clear old completions
				completions.clear ();
	
				connections.setConnection (
					new DatabaseConnection (driver, url, username, password));
				con ().getConnection ();
	
				setCompletions ();
				return true;
			}
			catch (SQLException sqle)
			{
				return error (sqle);
			}
			catch (IOException ioe)
			{
				return error (ioe);
			}
		}
	
	
		public boolean rehash (String line)
		{
			try
			{
				if (!(assertConnection ()))
					return false;
	
				completions.clear ();
				if (con () != null)
					con ().setCompletions (false);

				return true;
			}
			catch (Exception e)
			{
				return error (e);
			}
		}


		/** 
		 *  List the current connections
		 */
		public boolean list (String line)
		{
			int index = 0;
			info (loc ("active-connections", connections.size ()));

			for (Iterator i = connections.iterator (); i.hasNext (); index++)
			{
				DatabaseConnection c = (DatabaseConnection)i.next ();
				boolean closed = false;
				try
				{
					closed = c.connection.isClosed ();
				}
				catch (Exception e)
				{
					closed = true;
				}

				output (color ().pad (" #" + index + "", 5)
					.pad (closed ? loc ("closed") : loc ("open"), 9)
					.append (c.url));
			}

			return true;
		}


		public boolean all (String line)
		{
			int index = connections.getIndex ();

			boolean success = true;

			for (int i = 0; i < connections.size (); i++)
			{
				connections.setIndex (i);
				output (loc ("executing-con", con ()));
				success = sql (line.substring ("all ".length ())) && success;
			}

			// restore index
			connections.setIndex (index);
			return success;
		}


		public boolean go (String line)
		{
			String [] parts = split (line, 2, "Usage: go <connection index>");
			if (parts == null)
				return false;

			int index = Integer.parseInt (parts [1]);
			if (!(connections.setIndex (index)))
			{
				error (loc ("invalid-connection", "" + index));
				list (""); // list the current connections
				return false;
			}

			return true;
		}
	
	
		/** 
		 *  Save or stop saving a script to a file
		 */
		public boolean script (String line)
		{
			if (script == null)
				return startScript (line);
			else
				return stopScript (line);
		}


		/** 
		 *  Stop writing to the script file and close the script.
		 */
		private boolean stopScript (String line)
		{
			try
			{
				script.close ();
			}
			catch (Exception e)
			{
				handleException (e);
			}

			output (loc ("script-closed", script));
			script = null;
			return true;
		}


		/** 
		 *  Start writing to the specified script file.
		 */
		private boolean startScript (String line)
		{
			if (script != null)
				return error (loc ("script-already-running", script));


			String [] parts = split (line, 2, "Usage: script <filename>");
			if (parts == null)
				return false;
	
			try
			{
				script = new OutputFile (parts [1]);
				output (loc ("script-started", script));
				return true;
			}
			catch (Exception e)
			{
				return error (e);
			}
		}


		/** 
		 *  Run a script from the specified file.
		 */
		public boolean run (String line)
		{
			String [] parts = split (line, 2, "Usage: run <scriptfile>");
			if (parts == null)
				return false;
	
			List cmds = new LinkedList ();

			try
			{
				BufferedReader reader = new BufferedReader (new FileReader (
					parts [1]));
				try
				{
					String cmd = null;
					while ((cmd = reader.readLine ()) != null)
						cmds.add (cmd);
				}
				finally
				{
					reader.close ();
				}

				// success only if all the commands were successful
				return runCommands (cmds) == cmds.size ();
			}
			catch (Exception e)
			{
				return error (e);
			}
		}


		/** 
		 *  Save or stop saving all output to a file.
		 */
		public boolean record (String line)
		{
			if (record == null)
				return startRecording (line);
			else
				return stopRecording (line);
		}


		/** 
		 *  Stop writing output to the record file.
		 */
		private boolean stopRecording (String line)
		{
			try
			{
				record.close ();
			}
			catch (Exception e)
			{
				handleException (e);
			}

			output (loc ("record-closed", record));
			record = null;
			return true;
		}


		/** 
		 *  Start writing to the specified record file.
		 */
		private boolean startRecording (String line)
		{
			if (record != null)
				return error (loc ("record-already-running", record));


			String [] parts = split (line, 2, "Usage: record <filename>");
			if (parts == null)
				return false;
	
			try
			{
				record = new OutputFile (parts [1]);
				output (loc ("record-started", record));
				return true;
			}
			catch (Exception e)
			{
				return error (e);
			}
		}


	
	
		public boolean describe (String line)
			throws SQLException
		{
			String [] table = split (line, 2, "Usage: describe <table name>");
			if (table == null)
				return false;
	
			ResultSet rs;
	
			if (table [1].equals ("tables"))
			{
				rs = getTables ();
			}
			else
			{
				rs = getColumns (table [1]);
			}
	
			if (rs == null)
				return false;
	
			if (rs.isAfterLast ())
				return error ("No entries found for " + table [1]);
	
			print (rs);
			rs.close ();
			return true;
		}
	
	
		public boolean help (String line)
		{
			String [] parts = split (line);
			String cmd = parts.length > 1 ? parts [1] : "";
			int count = 0;
			TreeSet clist = new TreeSet ();

			for (int i = 0; i < commands.length; i++)
			{
				if (cmd.length () == 0 ||
					Arrays.asList (commands [i].getNames ()).contains (cmd))
				{
					clist.add (color ().pad ("!" + commands [i].getName (), 20)
						.append (wrap (commands [i].getHelpText (), 60, 20)));
				}
			}


			for (Iterator i = clist.iterator (); i.hasNext (); )
				output ((ColorBuffer)i.next ());

			if (cmd.length () == 0)
			{
				output ("");
				output (loc ("comments", getApplicationContactInformation ()));
			}

			return true;
		}


		public boolean manual (String line)
			throws IOException
		{
			InputStream in = SqlLine.class.getResourceAsStream ("manual.txt");
			if (in == null)
				return error (loc ("no-manual"));

			BufferedReader breader = new BufferedReader (
				new InputStreamReader (in));
			String man;
			int index = 0;
			while ((man = breader.readLine ()) != null)
			{
				index++;
				output (man);

				// silly little pager
				if (index % (opts.getMaxHeight () - 1) == 0)
				{
					String ret = reader.readLine (loc ("enter-for-more"));
					if (ret != null && ret.startsWith ("q"))
						break;
				}
			}

			breader.close ();

			return true;
		}
	}
	
	


	private void setCompletions ()
		throws SQLException, IOException
	{
		if (con () != null)
			con ().setCompletions (opts.getFastConnect ());
	}


	/** 
	 *  Completor for SQLLine. It dispatches to sub-completors based on the
	 *  current arguments.
	 *  
	 *  @author  <a href="mailto:marc@apocalypse.org">Marc Prud'hommeaux</a>
	 */
	class SQLLineCompletor
		implements Completor
	{
		public int complete (String buf, int pos, List cand)
		{
			if (buf != null && buf.startsWith (COMMAND_PREFIX)
				&& !buf.startsWith (COMMAND_PREFIX + "all")
				&& !buf.startsWith (COMMAND_PREFIX + "sql"))
			{
				return sqlLineCommandCompletor.complete (buf, pos, cand);
			}
			else
			{
				if (con () != null && con ().sqlLineSQLCompletor != null)
					return con ().sqlLineSQLCompletor.complete (buf, pos, cand);
				else
					return -1;
			}
		}
	}


	class SQLLineCommandCompletor
		extends MultiCompletor
	{
		public SQLLineCommandCompletor ()
		{
			List completors = new LinkedList ();

			for (int i = 0; i < commands.length; i++)
			{
				String [] cmds = commands [i].getNames ();
				for (int j = 0; cmds != null && j < cmds.length; j++)
				{
					Completor [] comps = commands [i].getParameterCompletors (); 
					List compl = new LinkedList ();
					compl.add (new SimpleCompletor (COMMAND_PREFIX + cmds [j]));
					compl.addAll (Arrays.asList (comps));
					compl.add (new NullCompletor ()); // last param no complete

					completors.add (new ArgumentCompletor (
						(Completor [])compl.toArray (new Completor [0])));
				}
			}

			setCompletors ((Completor [])completors
				.toArray (new Completor [0]));
		}
	}


	class TableNameCompletor
		implements Completor
	{
		public int complete (String buf, int pos, List cand)
		{
			if (con () == null)
				return -1;

			return new SimpleCompletor (con ().getTableNames (true))
				.complete (buf, pos, cand);
		}
	}


	class SQLLineSQLCompletor
		extends SimpleCompletor
	{
		public SQLLineSQLCompletor (boolean skipmeta)
			throws IOException, SQLException
		{
			super (new String [0]);

			Set completions = new TreeSet ();

			// add the default SQL completions
			String keywords = new BufferedReader (new InputStreamReader (
				SQLLineSQLCompletor.class.getResourceAsStream (
					"sql-keywords.properties"))).readLine ();

			// now add the keywords from the current connection

			try 
			{
				keywords += "," + con ().meta.getSQLKeywords ();
			}
			catch (Throwable t) { }
			try 
			{
				keywords += "," + con ().meta.getStringFunctions ();
			}
			catch (Throwable t) { }
			try 
			{
				keywords += "," + con ().meta.getNumericFunctions ();
			}
			catch (Throwable t) { }
			try 
			{
				keywords += "," + con ().meta.getSystemFunctions ();
			}
			catch (Throwable t) { }
			try 
			{
				keywords += "," + con ().meta.getTimeDateFunctions ();
			}
			catch (Throwable t) { }


			// also allow lower-case versions of all the keywords
			keywords += "," + keywords.toLowerCase ();

			for (StringTokenizer tok = new StringTokenizer (keywords, ", ");
				tok.hasMoreTokens ();
				completions.add (tok.nextToken ()));

			// now add the tables and columns from the current connection
			if (!(skipmeta))
			{
				String [] columns = getColumnNames (con ().meta);
				for (int i = 0; columns != null && i < columns.length; i++)
					completions.add (columns [i++]);
			}

			// set the Strings that will be completed
			setCandidateStrings (
				(String [])completions.toArray (new String [0]));
		}
	}


	private static class Connections
	{
		private final List connections = new ArrayList ();
		private int index = -1;

		public DatabaseConnection current ()
		{
			if (index != -1)
				return (DatabaseConnection)connections.get (index);

			return null;
		}


		public int size ()
		{
			return connections.size ();
		}


		public Iterator iterator ()
		{
			return connections.iterator ();
		}


		public void remove ()
		{
			if (index != -1)
			{
				connections.remove (index);
			}

			while (index >= connections.size ())
				index--;
		}


		public void addConnection (DatabaseConnection connection)
		{
			connections.add (connection);
		}


		public void setConnection (DatabaseConnection connection)
		{
			if (connections.indexOf (connection) == -1)
				connections.add (connection);

			index = connections.indexOf (connection);
		}


		public int getIndex ()
		{
			return index;
		}


		public boolean setIndex (int index)
		{
			if (index < 0 || index >= connections.size ())
				return false;

			this.index = index;
			return true;
		}
	}


	private class DatabaseConnection
	{
		Connection connection;
		DatabaseMetaData meta;
		private final String driver;
		private final String url;
		private final String username;
		private final String password;
		private Schema schema = null;
		private Completor sqlLineSQLCompletor = null;


		public DatabaseConnection (String driver, String url,
			String username, String password)
			throws SQLException
		{
			this.driver = driver;
			this.url = url;
			this.username = username;
			this.password = password;
		}


		public String toString ()
		{
			return url + "";
		}


		private void setCompletions (boolean skipmeta)
			throws SQLException, IOException
		{
			final String extraNameCharacters =
				meta.getExtraNameCharacters () == null ? ""
					: meta.getExtraNameCharacters ();

			// setup the completor for the database
			sqlLineSQLCompletor = new ArgumentCompletor (
				new SQLLineSQLCompletor (skipmeta),
				new ArgumentCompletor.AbstractArgumentDelimiter ()
				{
					// deleimiters for SQL statements are any
					// non-letter-or-number characters, except
					// underscore and characters that are specified
					// by the database to be valid name identifiers.
					public boolean isDelimiterChar (String buf, int pos)
					{
						char c = buf.charAt (pos);
						if (Character.isWhitespace (c))
							return true;

						return !(Character.isLetterOrDigit (c))
							&& c != '_'
							&& extraNameCharacters.indexOf (c) == -1;
					}
				});

			// not all argument elements need to hold true
			((ArgumentCompletor)sqlLineSQLCompletor).setStrict (false);
		}


		/** 
		 *  Connection to the specified data source.
		 *  
		 *  @param  driver		the driver class
		 *  @param  url			the connection URL
		 *  @param  username	the username
		 *  @param  password	the password
		 */
		boolean connect ()
			throws SQLException
		{
			try
			{
				if (driver != null && driver.length () != 0)
					Class.forName (driver);
			}
			catch (ClassNotFoundException cnfe)
			{
				return error (cnfe);
			}

			boolean foundDriver = false;
			try
			{
				foundDriver = DriverManager.getDriver (url) != null;
			}
			catch (Exception e)
			{
			}
	
			if (!(foundDriver))
			{
				output (loc ("autoloading-known-drivers", url));
				registerKnownDrivers ();
			}


			try
			{
				close ();
			}
			catch (Exception e)
			{
				return error (e);
			}

			connection = DriverManager.getConnection (url, username, password);
			meta = connection.getMetaData ();
	
			try
			{
				info (loc ("connected", new Object [] {
					meta.getDatabaseProductName (),
					meta.getDatabaseProductVersion () }));
			}
			catch (Exception e)
			{
				handleException (e);
			}

			try
			{
				info (loc ("driver", new Object [] {
					meta.getDriverName (),
					meta.getDriverVersion () }));
			}
			catch (Exception e)
			{
				handleException (e);
			}

			try
			{
				connection.setAutoCommit (opts.getAutoCommit ());
				autocommitStatus (connection);
			}
			catch (Exception e)
			{
				handleException (e);
			}

			try
			{
				command.isolation ("isolation: " + opts.getIsolation ());
			}
			catch (Exception e)
			{
				handleException (e);
			}

			return true;
		}
	

		public Connection getConnection ()
			throws SQLException
		{
			if (connection != null)
				return connection;

			connect ();

			return connection;
		}


		public void reconnect ()
			throws Exception
		{
			close ();
			getConnection ();
		}


		public void close ()
		{
			try
			{
				try
				{
					if (connection != null && !connection.isClosed ())
					{
						output (loc ("closing", connection));
						connection.close ();
					}
				}
				catch (Exception e)
				{
					handleException (e);
				}
			}
			finally
			{
				connection = null;
				meta = null;
			}
		}


		public String [] getTableNames (boolean force)
		{
			Schema.Table [] t = getSchema ().getTables ();
			Set names = new TreeSet ();
			for (int i = 0; t != null && i < t.length; i++)
				names.add (t [i].getName ());
			return (String [])names.toArray (new String [names.size ()]);
		}


		Schema getSchema ()
		{
			if (schema == null)
				schema = new Schema ();

			return schema;
		}


		class Schema
		{
			private Table [] tables = null;


			Table [] getTables ()
			{
				if (tables != null)
					return tables;

				List tnames = new LinkedList ();

				try
				{
					ResultSet rs = meta.getTables (connection.getCatalog (),
						null, "%", new String [] { "TABLE" });
					try
					{
						while (rs.next ())
							tnames.add (new Table (
								rs.getString ("TABLE_NAME")));
					}
					finally
					{
						try { rs.close (); } catch (Exception e) { }
					}

				}
				catch (Throwable t)
				{
				}

				return tables = (Table [])tnames.toArray (new Table [0]);
			}


			Table getTable (String name)
			{
				Table [] t = getTables ();
				for (int i = 0; t != null && i < t.length; i++)
				{
					if (name.equalsIgnoreCase (t [i].getName ()))
						return t [i];
				}

				return null;
			}


			class Table
			{
				final String name;
				Column [] columns;


				public Table (String name)
				{
					this.name = name;
				}


				public String getName ()
				{
					return name;
				}


				class Column
				{
					final String name;
					boolean isPrimaryKey;


					public Column (String name)
					{
						this.name = name;
					}
				}
			}
		}
	}


	class Opts
		implements Completor 
	{
		private boolean autosave = false;
		private boolean silent = false;
		// default to false for Windows, true for others
		private boolean color = System.getProperty ("os.name")
			.toLowerCase ().indexOf ("windows") != -1;
		private boolean showHeader = true;
		private int headerInterval = 100;
		private boolean fastConnect = true;
		private boolean autoCommit = true;
		private boolean verbose = false;
		private boolean force = false;
		private boolean showWarnings = false;
		private int maxWidth = Terminal.setupTerminal ().getTerminalWidth ();
		private int maxHeight = Terminal.setupTerminal ().getTerminalHeight ();
		private int maxColumnWidth = 15;
		private String isolation = "TRANSACTION_REPEATABLE_READ";
		private String outputFormat = "table";

		public static final String PROPERTY_PREFIX = "sqlline.";

		private File rcFile = new File (saveDir (), "sqlline.properties");
		private String historyFile = new File (saveDir (), "history")
			.getAbsolutePath ();


		public Opts (Properties props)
		{
			loadProperties (props);
		}


		public Completor [] optionCompletors ()
		{
			return new Completor [] {
				this,
				// new SimpleCompletor (possibleSettingValues ()),
			};
		}


		public String [] possibleSettingValues ()
		{
			List vals = new LinkedList ();
			vals.addAll (Arrays.asList (new String [] {
				"yes",
				"no",
				}));

			return (String [])vals.toArray (new String [vals.size ()]);
		}


		/** 
		 *  The save directory if HOME/.sqlline/ on UNIX, and
		 *  HOME/sqlline/ on Windows.
		 */
		public File saveDir ()
		{
			String dir = System.getProperty ("sqlline.rcfile");
			if (dir != null && dir.length () > 0)
				return new File (dir);

			File f = new File (System.getProperty ("user.home"),
				(System.getProperty ("os.name").toLowerCase ()
				 	.indexOf ("windows") != -1 ? "" : ".") + "sqlline")
					.getAbsoluteFile ();
			try
			{
				f.mkdirs ();
			}
			catch (Exception e)
			{
			}

			return f;
		}


		public int complete (String buf, int pos, List cand)
		{
			try
			{
				return new SimpleCompletor (propertyNames ())
					.complete (buf, pos, cand);
			}
			catch (Throwable t)
			{
				return -1;
			}
		}


		public void save ()
			throws IOException
		{
			OutputStream out = new FileOutputStream (rcFile);
			save (out);
			out.close ();
		}


		public void save (OutputStream out)
			throws IOException
		{
			try
			{
				Properties props = toProperties ();

				// don't save maxwidth: it is automatically set based on
				// the terminal configuration
				props.remove (PROPERTY_PREFIX + "maxwidth");

				props.store (out, getApplicationTitle ());
			}
			catch (Exception e)
			{
				handleException (e);
			}
		}


		String [] propertyNames ()
			throws IllegalAccessException, InvocationTargetException
		{
			TreeSet names = new TreeSet ();

			// get all the values from getXXX methods
			Method [] m = getClass ().getDeclaredMethods ();
			for (int i = 0; m != null && i < m.length; i++)
			{
				if (!(m [i].getName ().startsWith ("get")))
					continue;

				if (m [i].getParameterTypes ().length != 0)
					continue;

				String propName = m [i].getName ().substring (3).toLowerCase ();
				names.add (propName);
			}

			return (String [])names.toArray (new String [names.size ()]);
		}


		public Properties toProperties ()
			throws IllegalAccessException, InvocationTargetException,
				ClassNotFoundException
		{
			Properties props = new Properties ();

			String [] names = propertyNames ();
			for (int i = 0; names != null && i < names.length; i++)
			{
				props.setProperty (PROPERTY_PREFIX + names [i],
					Reflector.invoke (this, "get" + names [i], new Object [0])
						.toString ());
			}

			debug ("properties: " + props.toString ());
			return props;
		}


		public void load ()
			throws IOException
		{
			InputStream in = new FileInputStream (rcFile);
			load (in);
			in.close ();
		}


		public void load (InputStream fin)
			throws IOException
		{
			Properties p = new Properties ();
			p.load (fin);
			loadProperties (p);
		}


		public void loadProperties (Properties props)
		{
			for (Iterator i = props.keySet ().iterator (); i.hasNext (); )
			{
				String key = i.next ().toString ();
				if (key.startsWith (PROPERTY_PREFIX))
				{
					set (key.substring (PROPERTY_PREFIX.length ()),
						props.getProperty (key));
				}
			}
		}


		public void set (String key, String value)
		{
			set (key, value, false);
		}


		public boolean set (String key, String value, boolean quiet)
		{
			try
			{
				Reflector.invoke (this, "set" + key, new Object [] { value });
				return true;
			}
			catch (Exception e)
			{
				if (!quiet)
					error (loc ("error-setting", new Object [] { key, e }));
				return false;
			}
		}


		public void setFastConnect (boolean fastConnect)
		{
			this.fastConnect = fastConnect;
		}


		public boolean getFastConnect ()
		{
			return this.fastConnect;
		}




		public void setAutoCommit (boolean autoCommit)
		{
			this.autoCommit = autoCommit;
		}


		public boolean getAutoCommit ()
		{
			return this.autoCommit;
		}




		public void setVerbose (boolean verbose)
		{
			this.verbose = verbose;
		}


		public boolean getVerbose ()
		{
			return this.verbose;
		}




		public void setShowWarnings (boolean showWarnings)
		{
			this.showWarnings = showWarnings;
		}


		public boolean getShowWarnings ()
		{
			return this.showWarnings;
		}




		public void setMaxWidth (int maxWidth)
		{
			this.maxWidth = maxWidth;
		}


		public int getMaxWidth ()
		{
			return this.maxWidth;
		}




		public void setMaxColumnWidth (int maxColumnWidth)
		{
			this.maxColumnWidth = maxColumnWidth;
		}


		public int getMaxColumnWidth ()
		{
			return this.maxColumnWidth;
		}




		public void setIsolation (String isolation)
		{
			this.isolation = isolation;
		}


		public String getIsolation ()
		{
			return this.isolation;
		}


		public void setHistoryFile (String historyFile)
		{
			this.historyFile = historyFile;
		}


		public String getHistoryFile ()
		{
			return this.historyFile;
		}




		public void setColor (boolean color)
		{
			this.color = color;
		}


		public boolean getColor ()
		{
			return this.color;
		}



		public void setShowHeader (boolean showHeader)
		{
			this.showHeader = showHeader;
		}


		public boolean getShowHeader ()
		{
			return this.showHeader;
		}


		public void setHeaderInterval (int headerInterval)
		{
			this.headerInterval = headerInterval;
		}


		public int getHeaderInterval ()
		{
			return this.headerInterval;
		}


		public void setForce (boolean force)
		{
			this.force = force;
		}


		public boolean getForce ()
		{
			return this.force;
		}


		public void setSilent (boolean silent)
		{
			this.silent = silent;
		}


		public boolean getSilent ()
		{
			return this.silent;
		}




		public void setAutosave (boolean autosave)
		{
			this.autosave = autosave;
		}


		public boolean getAutosave ()
		{
			return this.autosave;
		}



		public void setOutputFormat (String outputFormat)
		{
			this.outputFormat = outputFormat;
		}


		public String getOutputFormat ()
		{
			return this.outputFormat;
		}




		public void setMaxHeight (int maxHeight)
		{
			this.maxHeight = maxHeight;
		}


		public int getMaxHeight ()
		{
			return this.maxHeight;
		}


	}


	static class Reflector
	{
		public static Object invoke (Object on, String method, Object [] args)
			throws InvocationTargetException, IllegalAccessException,
				ClassNotFoundException
		{
			return invoke (on, method, Arrays.asList (args));
		}


		public static Object invoke (Object on, String method, List args)
			throws InvocationTargetException, IllegalAccessException,
				ClassNotFoundException
		{
			Class c = on.getClass ();
			List candidateMethods = new LinkedList ();

			Method [] m = c.getMethods ();
			for (int i = 0; i < m.length; i++)
			{
				if (m [i].getName ().equalsIgnoreCase (method))
					candidateMethods.add (m [i]);
			}

			if (candidateMethods.size () == 0)
				throw new IllegalArgumentException (loc ("no-method",
					new Object [] { method, c.getName () }));

			for (Iterator i = candidateMethods.iterator (); i.hasNext (); )
			{
				Method meth = (Method)i.next ();
				Class [] ptypes = meth.getParameterTypes ();
				if (!(ptypes.length == args.size ()))
					continue;

				Object [] converted = convert (args, ptypes);
				if (converted == null)
					continue;

				return meth.invoke (on, converted);
			}

			return null;
		}


		public static Object [] convert (List objects, Class [] toTypes)
			throws ClassNotFoundException
		{
			Object [] converted = new Object [objects.size ()];
			for (int i = 0; i < converted.length; i++)
				converted [i] = convert (objects.get (i), toTypes [i]);
			return converted;
		}


		public static Object convert (Object ob, Class toType)
			throws ClassNotFoundException
		{
			if (ob == null || ob.toString ().equals ("null"))
				return null;

			if (toType == String.class)
				return new String (ob.toString ());
			else if (toType == Byte.class || toType == byte.class)
				return new Byte (ob.toString ());
			else if (toType == Character.class || toType == char.class)
				return new Character (ob.toString ().charAt (0));
			else if (toType == Short.class || toType == short.class)
				return new Short (ob.toString ());
			else if (toType == Integer.class || toType == int.class)
				return new Integer (ob.toString ());
			else if (toType == Long.class || toType == long.class)
				return new Long (ob.toString ());
			else if (toType == Double.class || toType == double.class)
				return new Double (ob.toString ());
			else if (toType == Float.class || toType == float.class)
				return new Float (ob.toString ());
			else if (toType == Boolean.class || toType == boolean.class)
				return new Boolean (ob.toString ().equals ("true")
					|| ob.toString ().equals (true + "")
					|| ob.toString ().equals ("1")
					|| ob.toString ().equals ("on")
					|| ob.toString ().equals ("yes"));
			else if (toType == Class.class)
				return Class.forName (ob.toString ());

			return null;
		}
	}


	public class OutputFile
	{
		final File file;
		final PrintWriter out;

		public OutputFile (String filename)
			throws IOException
		{
			file = new File (filename);
			out = new PrintWriter (new FileWriter (file));
		}


		public String toString ()
		{
			return file.getAbsolutePath ();
		}


		public void addLine (String command)
		{
			out.println (command);
		}


		public void close ()
			throws IOException
		{
			out.close ();
		}
	}


	static class DriverInfo
	{
		public String sampleURL;


		public DriverInfo (String name)
			throws IOException
		{
			Properties props = new Properties ();
			props.load (DriverInfo.class.getResourceAsStream (name));
			fromProperties (props);
		}


		public DriverInfo (Properties props)
		{
			fromProperties (props);
		}


		public void fromProperties (Properties props)
		{

		}
	}
}

