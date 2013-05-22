package sqlline;

/*
 * Copyright (c) 2002-2007, Marc Prud'hommeaux. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import jline.console.completer.StringsCompleter;

/**
 * A Completor implementation that completes java class names. By default,
 * it scans the java class path to locate all the classes.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public class ClassNameCompleter extends StringsCompleter
  {

  /**
   * Complete candidates using all the classes available in the
   * java <em>CLASSPATH</em>.
   */
  public ClassNameCompleter() throws IOException
    {
    super( getClassNames() );
    getStrings().add( "." );
    }

  public static String[] getClassNames() throws IOException
    {
    Set urls = new HashSet();

    for( ClassLoader loader = ClassNameCompleter.class
      .getClassLoader(); loader != null;
         loader = loader.getParent() )
      {
      if( !( loader instanceof URLClassLoader ) )
        {
        continue;
        }

      urls.addAll( Arrays.asList( ( (URLClassLoader) loader ).getURLs() ) );
      }

// Now add the URL that holds java.lang.String. This is because
// some JVMs do not report the core classes jar in the list of
// class loaders.
    Class[] systemClasses = new Class[]{
      String.class, javax.swing.JFrame.class
    };

    for( int i = 0; i < systemClasses.length; i++ )
      {
      URL classURL = systemClasses[ i ].getResource( "/"
        + systemClasses[ i ].getName().replace( '.', '/' ) + ".class" );

      if( classURL != null )
        {
        URLConnection uc = (URLConnection) classURL.openConnection();

        if( uc instanceof JarURLConnection )
          {
          urls.add( ( (JarURLConnection) uc ).getJarFileURL() );
          }
        }
      }

    Set classes = new HashSet();

    for( Iterator i = urls.iterator(); i.hasNext(); )
      {
      URL url = (URL) i.next();
      File file = new File( url.getFile() );

      if( file.isDirectory() )
        {
        Set files = getClassFiles( file.getAbsolutePath(),
          new HashSet(), file, new int[]{200} );
        classes.addAll( files );

        continue;
        }

      if( ( file == null ) || !file.isFile() ) // TODO: handle directories
        {
        continue;
        }
      if( !file.toString().endsWith( ".jar" ) )
        continue;

      JarFile jf = new JarFile( file );

      for( Enumeration e = jf.entries(); e.hasMoreElements(); )
        {
        JarEntry entry = (JarEntry) e.nextElement();

        if( entry == null )
          {
          continue;
          }

        String name = entry.getName();

        if( !name.endsWith( ".class" ) ) // only use class files
          {
          continue;
          }

        classes.add( name );
        }
      }

// now filter classes by changing "/" to "." and trimming the
// trailing ".class"
    Set classNames = new TreeSet();

    for( Iterator i = classes.iterator(); i.hasNext(); )
      {
      String name = (String) i.next();
      classNames.add( name.replace( '/', '.' ).
        substring( 0, name.length() - 6 ) );
      }

    return (String[]) classNames.toArray( new String[ classNames.size() ] );
    }

  private static Set getClassFiles( String root, Set holder, File directory,
                                    int[] maxDirectories )
    {
// we have passed the maximum number of directories to scan
    if( maxDirectories[ 0 ]-- < 0 )
      {
      return holder;
      }

    File[] files = directory.listFiles();

    for( int i = 0; ( files != null ) && ( i < files.length ); i++ )
      {
      String name = files[ i ].getAbsolutePath();

      if( !( name.startsWith( root ) ) )
        {
        continue;
        }
      else if( files[ i ].isDirectory() )
        {
        getClassFiles( root, holder, files[ i ], maxDirectories );
        }
      else if( files[ i ].getName().endsWith( ".class" ) )
        {
        holder.add( files[ i ].getAbsolutePath().
          substring( root.length() + 1 ) );
        }
      }

    return holder;
    }
  }