package com.getperka.sea.inject;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Scope;

import com.getperka.sea.ext.EventDecorator;

/**
 * A scope annotation that is valid during the invocation of a single {@link EventDecorator} call.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Scope
public @interface DecoratorScoped {}
