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

/**
 * A signal handler interface for SQLLine. The interface is decoupled from the
 * implementation since signal handlers are not portable across JVMs, so we use
 * dynamic class-loading.
 */
public interface SqlLineSignalHandler {
  /**
   * Sets the dispatchCallback to be alerted of by signals.
   *
   * @param dispatchCallback statement affected
   */
  void setCallback(DispatchCallback dispatchCallback);
}

// End SqlLineSignalHandler.java
