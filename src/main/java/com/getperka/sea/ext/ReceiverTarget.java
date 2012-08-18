package com.getperka.sea.ext;

import java.lang.reflect.Method;

/**
 * Encapsulates a method and an instance on which to execute it.
 */
public interface ReceiverTarget {

  /**
   * The instance on which the method will be executed or {@code null} if the method is static.
   */
  public Object getInstance();

  /**
   * The method to execute.
   */
  public Method getMethod();

}
