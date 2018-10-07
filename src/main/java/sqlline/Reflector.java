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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Invokes methods via reflection.
 */
class Reflector {
  private final SqlLine sqlLine;

  Reflector(SqlLine sqlLine) {
    this.sqlLine = sqlLine;
  }

  public Object invoke(Object on, String method, Object... args)
      throws InvocationTargetException, IllegalAccessException,
      ClassNotFoundException {
    return invoke(on, method, Arrays.asList(args));
  }

  public Object invoke(Object on, String method, List args)
      throws InvocationTargetException, IllegalAccessException,
      ClassNotFoundException {
    return invoke(on, on == null ? null : on.getClass(), method, args);
  }

  public Object invoke(Object on, Class defClass, String methodName, List args)
      throws InvocationTargetException, IllegalAccessException,
      ClassNotFoundException {
    Class c = defClass != null ? defClass : on.getClass();
    List<Method> candidateMethods = new LinkedList<>();

    for (Method method : c.getMethods()) {
      if (method.getName().equalsIgnoreCase(methodName)) {
        candidateMethods.add(method);
      }
    }

    if (candidateMethods.size() == 0) {
      throw new IllegalArgumentException(
          sqlLine.loc("no-method", methodName, c.getName()));
    }

    for (Method method : candidateMethods) {
      Class[] ptypes = method.getParameterTypes();
      if (ptypes.length != args.size()) {
        continue;
      }

      Object[] converted = convert(args, ptypes);
      if (converted == null) {
        continue;
      }

      if (!Modifier.isPublic(method.getModifiers())) {
        continue;
      }

      return method.invoke(on, converted);
    }

    return null;
  }

  public static Object[] convert(List objects, Class[] toTypes)
      throws ClassNotFoundException {
    Object[] converted = new Object[objects.size()];
    for (int i = 0; i < converted.length; i++) {
      converted[i] = convert(objects.get(i), toTypes[i]);
    }
    return converted;
  }

  public static Object convert(Object ob, Class toType)
      throws ClassNotFoundException {
    if (ob == null || ob.toString().equals("null")) {
      return null;
    }
    if (toType == String.class) {
      return ob.toString();
    } else if (toType == Byte.class || toType == byte.class) {
      return Byte.valueOf(ob.toString());
    } else if (toType == Character.class || toType == char.class) {
      return ob.toString().charAt(0);
    } else if (toType == Short.class || toType == short.class) {
      return Short.valueOf(ob.toString());
    } else if (toType == Integer.class || toType == int.class) {
      return Integer.valueOf(ob.toString());
    } else if (toType == Long.class || toType == long.class) {
      return Long.valueOf(ob.toString());
    } else if (toType == Double.class || toType == double.class) {
      return Double.valueOf(ob.toString());
    } else if (toType == Float.class || toType == float.class) {
      return Float.valueOf(ob.toString());
    } else if (toType == Boolean.class || toType == boolean.class) {
      return ob.toString().equals("true")
          || ob.toString().equals(true + "")
          || ob.toString().equals("1")
          || ob.toString().equals("on")
          || ob.toString().equals("yes");
    } else if (toType == Class.class) {
      return Class.forName(ob.toString());
    }

    return null;
  }
}

// End Reflector.java
