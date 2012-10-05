# Simple Event Architecture

The Simple Event Architecture makes possible to write a highly-concurrent, event-based application or component with a minimum of friction.

- [Simple Event Architecture](#simple-event-architecture)
 - [Background](#background)
 - [Design](#design)
  - [Events](#events)
  - [Event Receivers](#event-receivers)
  - [Event Dispatch](#event-dispatch)
  - [Event Decoration](#event-decoration)
 - [Provided Services](#provided-services)
  - [Logging](#logging)
  - [Tagging](#tagging)
   - [Literal Tags](#literal-tags)
   - [Instance Tags](#instance-tags)
  - [Timed Events](#timed-events)
  - [Outcome Events](#outcome-events)
 - [Meta Events](#meta-events)
  - [Dispatch Completed](#dispatch-completed)
 - [Ordered Events](#ordered-events)
 - [Sequencers](#sequencers)
  - [Event-Dispatched Sequencers](#event-dispatched-sequencers)
 - [Guice](#guice)

The Maven coordinates for SEA are:
```xml
<dependency>
  <groupId>com.getperka.sea</groupId>
  <artifactId>sea</artifactId>
  <version>RELEASE</version>
</dependency>
```

## Background

The impetus for this project was to re-write a tool for managing Perka's deployments to EC2.  Like most V1 system-administration tools, the original code had gotten fairly hairy due to continued hacking, the need to support many special cases, a lack of factoring, and was unable to easily handle partial-success scenarios.  The desired coding style for the V2 tool is to be semi-declarative (i.e. "Do this when" instead of "Did this happen?"), composeable, and easily maintainable.  Ideally, a "show all uses of" search in an IDE will definitively identify all pieces of code that are side-effects of a semantic event.  The ability to perform many concurrent operations without juggling `Futures` is a plus.

## Design

The core API performs exactly two functions: event dispatch and event decoration.  All "interesting" functionality is built using the decorator pattern.

### Events

An event is just an arbitrary object that implements the empty tag interface `Event`:

```java
import com.getperka.sea.Event;
public class MyEvent implements Event {}
```

No specific annotations or informal protocols are required to route an event object.  Event types may be as simple or as complicated as your use-case requires.  The core SEA code only provides for dispatch within a single JVM, so your event objects need not be serializable.

### Event Receivers

Event receivers are single-argument methods annotated with `@Receiver` that accept a type assignable to `Event`. These methods may have any access modifier, name, and return type.  Receivers are permitted to throw checked exceptions.  A class may contain any number of receivers for any number of event types, or multiple receivers for the same event type.

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

When an event is fired, each registered `@Receiver` method whose erased `Event` parameter is assignable from the dispatched event's class will be invoked. The invocations occur concurrently from a set of pooled threads as soon as possible.  By default, no synchronization or ordering on event dispatch is performed.

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

## Provided Services

The core distribution includes a variety of convenience services for building up complex behaviors.  Often, several decorators will be used simultaneously, thus these services attempt to maximize orthogonality.

### Logging

The `@Logged` decorator will emit a specified message via SLF4J whenever an event receiver is invoked.  The annotation has properties that correspond to a message at `info`, `warn`, or `error` levels.  Is it mainly useful for debugging when applied globally.

```java
@Logged(info = "Doing it")
class ReceivesMyEvent {
  @Receiver
  void myEvent(MyEvent evt) {}
}
```

### Tagging

The `@Tagged` filter is used with events that implement the `TaggedEvent` interface (or extend the `BaseTaggedEvent` class).  Events can be filtered by matching string or class-literal tags that are applied at runtime.

#### Literal Tags

```java
class FooEvent extends BaseTaggedEvent {
}

@Tagged(strings = "verbose")
class VerboseEventDescriber {
  @Receiver
  void describe(FooEvent evt) {
    logger.info(evt.getInterestingStuff());
  }

  @Receiver
  void describe(BarEvent evt) {
    logger.info(evt.getInterestingStuff());
  }
}

class Sender {
  void send() {
    FooEvent evt = new FooEvent();
    evt.addTag(Tag.create("verbose"));
    eventDispatch.fire(evt);
  }
}
```

The `@Tagged` annotation allows multiple string or class literals to be applied.  Matching can be further customized by providing a `mode = TagMode.ALL` or `mode = TagMode.NONE`.

#### Instance Tags

In many cases it is desirable to send events only to a specific instance of an event receiver.  This occurs when events are used to return the results of an operation requested by an event sender.  In this use-case, a `@Tagged(receiverInstance = true)` can be used to allow only events that refer to the instance of the receiver.  See `Sequencers` below for a fully-developed use case.

```java
class TalkingToMe {
  @Tagged(receiverInstance = true)
  @Receiver
  void onEvent(MyTaggedEvent evt) {}
}

class Sender {
  void send() {
    MyTaggedEvent evt = new MyTaggedEvent();
    evt.addTag(Tag.create(someInstanceOfTalkingToMe));
    eventDispatch.fire(evt);
  }
}
```

### Timed Events

In some cases, it is useful to generate an error condition if an event takes too long to process (e.g. events that trigger remote network operations).  The `@Timed` decorator can be used to specify a timeout before interrupting or killing the thread that is processing the event.

```java
class MightBeSlow {
  @Timed(value = 2, unit = TimeUnit.SECONDS)
  @Receiver
  void maybeSlow(MyEvent evt) throws InterruptedException {
    // Start a process
    someLockCondition.await();
    // Do more stuff
  }
}
```

The above declaration will trigger a `Thread.interrupt()` after the receiver has been running for two seconds.  If the `stop = true` value is specified in the annotation, `Thread.stop()` will be called.  The usual caveats to stopping a thread still apply.

### Outcome Events

An `OutcomeEvent` is used for scatter-gather events where one event receiver fires events that are operated on by a second receiver which returns information back to the original receiver via the same event object.  It uses several different decorators to specify implementation, on-success, and on-failure receivers.  `OutcomeEvent` extends `TaggedEvent` in order to use receiver tagging when returning the event to the original caller.

```java
class AddEvent extends BaseOutcomeEvent  {
  // Data as public properties for brevity
  public List<Integer> operands;
  public int total;
}

class Sender {
  void example() {
    // Set up the event
    AddEvent evt = new AddEvent();
    evt.operands = Arrays.asList(1, 2, 3);
    eventDispatch.fire(evt);
  }

  @Receiver
  @Success
  void addEventSuccess(AddEvent evt) {
    logger.info("The total is " + evt.total);
  }

  @Receiver
  @Failure
  void addEventFailure(AddEvent evt) {
    // OutcomeEvent.getFailure() returns a Throwable
    logger.error("Add failed", evt.getFailure());
  }
}

class Implementor {
  @Receiver
  @Implementation
  void addEventImplementation(AddEvent evt) {
    evt.total = sumOfList(evt.operands);
  }
}
```

The `@Implementation` filter checks the incoming `OutcomeEvent` to see if its `isSuccess()` or `getFailure()` are `false` and `null` respectively.  If the event has not yet been processed, it is passed on the receiver method.  One the receiver method returns or throws an exception, the event is updated accordingly and automatically re-fired.  Conversely, the `@Success` and `@Failure` filters only allow an `OutcomeEvent` through if the `isSuccess` or `getFailure` is `true` or non-`null`.

## Meta Events

### Dispatch Completed

A `DispatchCompleteEvent` is automatically fired once an event has been dispatched to all of its potential receiver methods.  This event type contains a reference to the source event as well as a collection of `DispatchResult` objects which provide information of the disposition of each receiver. This information includes a flag to indicate whether or not the receiver method was actually invoked, the value returned from the receiver method, and any exception thrown by the receiver method.

The `@Dispatched` decorator can be applied to a `DispatchCompleteEvent` receiver to filter by source event type and whether or not the event resulted in the invocation of any receiver methods.  This allows a fallback receiver methods to be built for an event:

```java
@Dispatched(eventType = MyEvent.class, onlyUnreceived = true)
@Receiver
void fallbackForMyEvent(DispatchCompleteEvent evt) {
  MyEvent myEvent = (MyEvent) evt.getSource();
  // Perform some default behavior, perhaps emitting a warning message, etc.
}
```

## Ordered Events

The concurrent nature of event dispatch means that the order in which events are received is essentially unpredictable.  In cases where predicable dispatch order is required, the `OrderedDispatch` utility class combined with the `@Ordered` decorator can be used.

```java
class LoadResourceEvent extends BaseOutcomeEvent {
  // Public properties for brevity
  public String contents;
  public String name;
}

class ResourceLoader {
  @Receiver
  @Implementation
  // Not @Ordered, because resources can be loaded concurrently
  void load(LoadResourceEvent evt) {
    evt.contents = loadContentsForName(evt.name);
  }
}

class Example {
  private final Tag last = Tag.create("last");

  void go() {
    List<LoadResourceEvent> orderedEvents = makeLoadEvents();
    orderedEvents.get(orderedEvents.size() - 1).addTag(last);

    // Assume both "this" and a ResourceLoader already registered elsewhere
    OrderedDispatchers.create(eventDispatch).fire(orderedEvents);
  }

  @Receiver
  @Success
  @Ordered
  void loadSuccess(LoadResourceEvent evt) {
    output.append(evt.contents);
    if (evt.getTags().contains(last)) {
      output.close();
    }
  }
}
```

For a given list of events, each call to an `@Ordered` receiver will return before the next call is made.

It is possible to mix ordered and un-ordered receiver methods from the same call to `OrderedDispatch.fire()`.  If there are multiple `@Ordered` receivers that would receive the event sequence, they each independently receive events in the same sequence, however they will do so at their own rate.

## Sequencers

It is often impractical to store the intermediate state of an ongoing process in a single event.  Furthermore, most non-trivial processes will require multiple, possibly concurrent, events to be sent and received.  The `Sequencer` base class is used to encapsulate non-trivial event sequences into a reusable component.  (This type could just as easily be called an "Activity" or a "Controller", but these terms are heavily overloaded.  If you're familiar with MIDI, the name should make more sense.)

Implementations of `Sequencer<T>` present their encapsulated event sequence as a blocking `T call()` method (and also implement `Callable<T>` for convenience).  When called, the `Sequencer` will register itself with an associated `EventDispatch` instance, and call the `protected abstract start()` method.  A `Sequencer` subclass will likely fire and receive multiple (concurrent) events, eventually culminating in a call to `finish()` or `fail()`.  When one of the termination methods is called, the blocking call to `call()` will either return the value passed to `finish()` or throw a `SequenceFailureException`.  If concurrent instances of a particular sequence are expected, using `TaggedEvent` with distinct tag values will prevent cross-talk.

```java
class ResourceLoadEvent extends BaseOutcomeEvent {
  // Public properties used for brevity, getTags() omitted
  public String contents;
  public String resourceName;
}

class ResourceLoader {
  @Receiver
  @Implementation
  void load(ResourceLoadEvent evt) {
    evt.contents = loadContentsForResource(evt.resourceName);
  }
}

@Tagged(receiverInstance = true)
class LoadResourcesSequencer extends Sequencer<Map<String, String>> {
  public List<String> resourceNames;
  // Return-to-sender tag
  private Tag myTag = Tag.create(this);
  private Map<String, String> state = new ConcurrentHashMap<String, String>();

  protected void start() {
    for (String name : resourceNames) {
      ResourceLoadEvent evt = new ResourceLoadEvent();
      evt.resourceName = name;
      evt.getTags().add(myTag);
      fire(evt);
    }
  }

  @Receiver
  @Success
  void loadEventSuccess(ResourceLoadEvent evt) {
    state.put(evt.resourceName, evt.contents);
    if (state.size() == resourceNames.size()) {
      finish(state);
    }
  }

  @Receiver
  @Failure
  void loadEventFailure(ResourceLoadEvent evt) {
    // This is overly simplistic, a more robust system would retry
    fail("Could not load resource", evt.getFailure());
  }
}

class Example {
  void go() {
    LoadResourcesSequencer seq = new LoadResourcesSequencer();
    seq.resourceNames = Arrays.asList("one", "two", "three");

    // Set the EventDispatch that we assume has a ResourceLoader already registered
    seq.setEventDispatch(eventDispatch);
    
    // All of the concurrency complexity is hidden behind a synchronous call!
    Map<String, String> contentMap = seq.call();

    // Do stuff
  }
}
```

It is important to note that the `call()` method is not reentrant.  If multiple threads attempt to call the same instance of a `Sequencer`, the invocations will stack up, one behind the other.  This behavior is necessary because the termination methods, `finish()` and `fail()` will likely be called from an event receiver thread and not directly from `start()`.  If the termination methods are not called, the `call()` method will never return.  In order to prevent the accumulation of dead threads, it is suggested that `Sequencers` are called from an `@Timed(stop = true)` event receiver.

### Event-Dispatched Sequencers

The use of event-dispatched sequences allows composite `Sequencers` to be built that can use `@Success` and `@Failure` pairs to robustly handle partial success and failures of their component sequences. For example:

```java
class LoadABunchOfResourcesEvent extends BaseOutcomeEvent {
  public List<String> resourceNames;
  public Map<String, String> namesToContent;
}

class ResourceLoader {
  @Receiver
  @Implementation
  @Timed(value = 5, unit = TimeUnit.MINUTES, stop = true)
  void load(LoadABunchOfResourcesEvent evt) {
    LoadResourcesSequencer seq = new LoadResourcesSequencer();
    seq.setEventDispatch(eventDispatch);
    seq.resourceNames = evt.resourceNames;
    evt.namesToContent = seq.call();
  }
}

// Elsewhere an @Success and @Failure pair exist to call the multiple-load event
```

## Guice

SEA uses Guice internally for object lifecycle management.  Users who already use Guice may mix `EventModule` into their configuration.  All receiver and decorator instance creation is delegated to the `Injector`.  Several scoped binding annotations are available in the `sea.inject` package.
