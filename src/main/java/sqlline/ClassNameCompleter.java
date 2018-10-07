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

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.jline.reader.Candidate;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.utils.AttributedString;

/**
 * An implementation of {@link org.jline.reader.Completer} that completes
 * java class names. By default, it scans the java class path to locate all the
 * classes.
 */
public class ClassNameCompleter extends StringsCompleter {
  /**
   * Completes candidates using all the classes available in the
   * java <em>CLASSPATH</em>.
   *
   * @throws IOException on error
   */
  public ClassNameCompleter() throws IOException {
    super(getClassNames());
    candidates.add(
        new Candidate(AttributedString.stripAnsi("."),
            ".", null, null, null, null, true));
  }

  public static Set<String> getClassNames() throws IOException {
    Set<URL> urls = new HashSet<>();

    for (ClassLoader loader = ClassNameCompleter.class.getClassLoader();
         loader != null;
         loader = loader.getParent()) {
      if (!(loader instanceof URLClassLoader)) {
        continue;
      }

      Collections.addAll(urls, ((URLClassLoader) loader).getURLs());
    }

    // Now add the URL that holds java.lang.String. This is because
    // some JVMs do not report the core classes jar in the list of
    // class loaders.
    Class[] systemClasses = {
        String.class, javax.swing.JFrame.class
    };

    for (Class systemClass : systemClasses) {
      URL classURL = systemClass.getResource(
          "/" + systemClass.getName().replace('.', '/') + ".class");

      if (classURL != null) {
        URLConnection uc = classURL.openConnection();
        if (uc instanceof JarURLConnection) {
          urls.add(((JarURLConnection) uc).getJarFileURL());
        }
      }
    }

    Set<String> classes = new HashSet<>();
    for (URL url : urls) {
      File file = new File(URLDecoder.decode(url.getFile(), "UTF-8"));

      if (file.isDirectory()) {
        Set<String> files = getClassFiles(file.getAbsolutePath(),
            new HashSet<>(),
            file,
            new int[]{200});
        classes.addAll(files);

        continue;
      }

      if (!file.isFile()) {
        // TODO: handle directories
        continue;
      }
      if (!file.toString().endsWith(".jar")) {
        continue;
      }

      JarFile jf = new JarFile(file);

      for (Enumeration<JarEntry> e = jf.entries(); e.hasMoreElements();) {
        JarEntry entry = e.nextElement();

        if (entry == null) {
          continue;
        }

        String name = entry.getName();

        if (!name.endsWith(".class")) {
          // only use class files
          continue;
        }

        classes.add(name);
      }
    }

    // now filter classes by changing "/" to "." and trimming the
    // trailing ".class"
    Set<String> classNames = new TreeSet<>();

    for (String name : classes) {
      classNames.add(name.replace('/', '.').substring(0, name.length() - 6));
    }

    return classNames;
  }

  private static Set<String> getClassFiles(
      String root,
      Set<String> holder,
      File directory,
      int[] maxDirectories) {
    // we have passed the maximum number of directories to scan
    if (maxDirectories[0]-- < 0) {
      return holder;
    }

    File[] files = directory.listFiles();
    if (files == null) {
      return holder;
    }

    for (File file : files) {
      String name = file.getAbsolutePath();

      if (!name.startsWith(root)) {
        continue;
      } else if (file.isDirectory()) {
        getClassFiles(root, holder, file, maxDirectories);
      } else if (file.getName().endsWith(".class")) {
        holder.add(file.getAbsolutePath().substring(root.length() + 1));
      }
    }
    return holder;
  }
}

// End ClassNameCompleter.java
