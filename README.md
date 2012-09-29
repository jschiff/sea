# Simple Event Architecture

The Simple Event Architecture makes it simple to write event-based applications or components with a minimum of syntax.

## Design

The core API performs exactly two functions: event dispatch and event decoration.

### Events

An event is just an arbitrary object that implements the empty tag interface `Event`:

```java
import com.getperka.sea.Event;
public class MyEvent implements Event {}
```

No specific annotations or informal protocols are required to route an event object.  Event types may be as simple or as complicated as your use-case requires.  The core SEA code only provides for dispatch within a single JVM, so your event objects need not be serializable.

### Event Receivers

Event receivers are single-argument methods annotated with `@Receiver` that accept a type assignable to `Event`. These methods may have any access modifier, name, and return type.  A class may contain any number of receivers for any number of event types, or multiple receivers for the same event type.

```java
class MyReceiver {
  @Receiver
  void onMyEvent(MyEvent evt) {
  }
}
```

### Event Dispatch

Event dispatch is controlled by an `EventDispatch` instance.  Each instance of `EventDispatch` is a self-contained domain in which events will be dispatched, and a single instances per application is usually sufficient.

```java
EventDispatch dispatch = EventDispatchers.create();
dispatch.register(MyReceiver.class);
dispatch.fire(new MyEvent());
```

An explicit registration step is necessary to configure the methods that will receive an event. The `register()` method returns a `Registration` object which can be canceled.

There are several flavors of the `register()` method:
* `register(Class)`
  * Only receiver methods declared in the class will be considered.
  * Static receivers are simply invoked.
  * A new instance of the class will be created for each invocation of instance receiver methods.  It must have a zero-arg constructor or a Guice binding.
* `register(Class, Provider)`
  * Only receiver methods declared in the class will be considered.
  * Static receivers are simply invoked.
  * The `javax.inject.Provider.get()` method will be used to retrieve an instance to receive the event.
* `register(Object obj)`
  * This method is shorthand for `register(Class, Provider)`, reusing the provided instance.

When an event is fired, each registered `@Receiver` method whose erased `Event` parameter is assignable from the dispatched event`s class will be invoked. The invocations occur concurrently from a set of pooled threads as soon as possible.  By default, no synchronization or ordering on event dispatch is performed.

### Event Decoration

Every event dispatch may be decorated by an arbitrary number of `EventDecorator` instances which are declared via annotations.  The decorators represent reusable chunks of dispatch logic that may apply to all or some events targeted at a particular receiver method.

Creating an `EventDecorator` requires two type declarations: the decorator itself and a binding annotation which indicates which receiver methods the decorator should be applied to.  The binding annotation may also be used to provide configuration information to the decorator.

```java
@EventDecoratorBinding(MyDecorator.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.PACKAGE, ElementType.TYPE })
public @interface Decorated {
  boolean someConfigurationValue();
}

class MyDecorator implements EventDecorator<Decorated, MyEvent> {
  public Callable<Object> wrap(final EventDecorator.Context<Decorated, MyEvent> ctx) {
    return new Callable<Object>() {
      public Object call() throws Exception {
        // Do setup work
        try {
          // Call the underlying work, returns the receiver method's return value
          return ctx.getWork().call();
        } finally {
          // Do teardown
        }
      }
    }
  }
}

class MyReceiver {
  @Decorated
  @Receiver
  void decoratedReceiver(MyEvent event) { }
}
```

The example above starts by declaring the `Decorated` annotation.  The only requirement the binding annotation has is that it must have runtime retention and the `EventDecoratorBinding` meta-annotation. The `@Documented` annotation is suggested, but not required.

The parameterization of the `MyDecorator` declaration is important; it determines if the decorator should actually be invoked for a given event.  If the decorator does not apply to the specific event type that is being dispatched to a receiver method, the decorator will not be invoked.  This allows specialized decorators to be applied to a receiver method that accepts a wide variety of concrete event types; it is valid to have both an `EventDecorator<FooDecorated, FooEvent>` and an `EventDecorator<BarDecorated, BarEvent>` applied to a receiver method that accepts a common supertype of both `FooEvent` and `BarEvent`.

Decorators have their effects applied in the following order:
* Global decorators,  registered via `EventDispatch.addGlobalDecorator(AnnotatedElement)`.
* Decorators applied to a `package-info.java` declaration will be applied to all methods contained by that package, but not to sub-packages.
* Decorators applied to a class will affect all receiver methods defined by that class.
* Lastly, decorators applied to individual receiver methods.

Multiple calls to `EventDispatch.addGlobalDecorator()` register a set of decorators that are "more global" than any previously-registered global decorators. Decoration order within a particular decoration level is undefined.

Instances of `EventDecorators` must have a zero-arg constructor or a Guice binding.  Once the instance of the decorator has been obtained, its `wrap()` method will be called in order to build up a work chain.  The entire receiver dispatch chain is constructed before `call()` is invoked on the outermost decoration wrapper.  The `EventDecorator.Context` supplied to the decorator provides access to a variety of information about the event dispatch that is about to occur.

Because one of the chief functions of decorators is to act as a filter to prevent certain events from reaching a receiver, it is valid for a decorator's `wrap()` method to return `null`.  If an event is canceled by a decorator during dispatch setup, the decorators that have already run will not receive any notification that the event dispatch did not actually occur.  Thus, it is important for decorators to defer any externally-visible effects (e.g. persistence context setup) to the return `Callable.call()` method.  If filtering requires access to expensive resources, the check can be performed in the `call()` method itself, which simply elects to skip the call to `ctx.getWork().call()`.  If it is important for the decoration logic to know whether or not the receiver method was invoked or if it threw an exception, the `Context.wasDispatched()` or `Context.wasThrown()` methods can be consulted after the call to `ctx.getWork().call()` is made.