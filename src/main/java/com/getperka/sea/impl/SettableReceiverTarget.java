package com.getperka.sea.impl;

import java.lang.reflect.Method;

import javax.inject.Provider;

import com.getperka.sea.ext.ReceiverTarget;

/**
 * A {@link ReceiverTarget} that can be (re-)set.
 */
public interface SettableReceiverTarget extends ReceiverTarget {
  void setInstanceDispatch(Provider<? extends Object> provider, Method method);

  void setStaticDispatch(Method method);
}
