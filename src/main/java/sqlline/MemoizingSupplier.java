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

import java.util.Objects;
import java.util.function.Supplier;

/** A wrapper for a singleton instance that initializes itself the first
 * time that {@link #get} is called.
 *
 * @param <E> Element type
 */
class MemoizingSupplier<E> implements Supplier<E> {
  private static final Object NOT_SET = new Object();

  private final Supplier<E> supplier;
  private E value;

  MemoizingSupplier(Supplier<E> supplier) {
    this.supplier = Objects.requireNonNull(supplier);
    value = (E) NOT_SET;
  }

  public E get() {
    if (value == NOT_SET) {
      value = supplier.get();
    }
    return value;
  }
}

// End MemoizingSupplier.java
