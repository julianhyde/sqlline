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

import java.io.IOException;
import java.util.Properties;

/**
 * Description of a JDBC driver.
 */
public class DriverInfo {
  public String sampleURL;

  public DriverInfo(String name) throws IOException {
    Properties props = new Properties();
    props.load(DriverInfo.class.getResourceAsStream(name));
    fromProperties(props);
  }

  public DriverInfo(Properties props) {
    fromProperties(props);
  }

  public void fromProperties(Properties props) {
  }
}

// End DriverInfo.java
