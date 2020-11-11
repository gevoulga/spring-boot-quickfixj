# Quickfixj Spring Boot

Integration of QuickfixJ with Spring-Boot-Starter.  
Spin-up QuickfixJ with **zero** code.

## Introduction

FIX message exchange protocol is a mainstay of financial institute communication, for real-time electronic exchange of securities.  
QuickFIX/J is an implementation engine of FIX protocol.

[Quickfixj Spring Boot](https://github.com/gevoulga/spring-boot-quickfixj) simplifies the process of spinning-up a FIX engine, by providing easy, spring-boot-style configuration of-the-shelf, with minimal FIX-layer config, enabling developers to focus on business logic.

## Artifacts

Quickfixj Spring Boot supports:

 * reactive programming style (flux)
 * imperative programming style (callbacks)
 * info, health-checks & metrics
 
## Quickfixj config file

As per the requirements from quickfixj, the FIX sessions need to be configured in a https://www.quickfixj.org/usermanual/2.1.0/usage/configuration.html[configuration file]. +
This can be done in `quickfixj.cfg` and will be picked up by default.

A different filename can also be specified, by setting in the `application.properties` or `application.yml`:

`application.yaml`:
```yaml
quickfixj:
  config: classpath:quickfixj-qa.cfg
```
`quickfixj.cfg`:
```properties
[default]
ConnectionType=acceptor
SocketAcceptPort=0
StartTime=00:00:00
EndTime=00:00:00
HeartBtInt=30
ReconnectInterval=5

[SESSION]
SessionName=SESSION1 #associate session with bean named SESSION1
BeginString=FIX.4.3
SenderCompID=TEST_CLIENT
TargetCompID=FIX
DataDictionary=${quickfixj.dataDictionary} # you can specify placeholder!
Username=cool #for authentication based on configuration
Password=stuff
```

## Session Beans
    
For each FIX session specified in the quickfixj.cfg file, a corresponding session bean is created on-the-fly during spring context initialization.

These beans are named after the name of the FIX session.  
To use them in our application we can `@Autowired` them.  
If more than one FIX sessions is defined, in standard spring fashion we can distinguish using `@Qualifier` or naming the bean the same as the session.  
For example:
```java
//This will wire-in the SESSION1 FIX session bean 
@Qualifier("SESSION1")
private ReactiveFixSession fixSession;
```

These beans allow sending and receiving FIX messages.

##### Note:

Custom implementation of FIX session beans is possible.  
These should either be annotated with `@FixSessionMapping` or implement `NamedBean`.  
The name of the beans needs to be the same with `SessionName` property in `quickfixj.cfg`.

## Reactive

#### Library

To start using reactive quickfixj you need to import the following dependency:
```xml
<dependency>
    <groupId>ch.voulgarakis</groupId>
    <artifactId>quickfixj-spring-boot-starter-flux</artifactId>
    <version>1.0.0.RELEASE</version>
</dependency>
```
### Usage Example

A Service that:
 * sends a quote request to the FIX session
 * receives the FIX quote responses
 * selects the 1st one
 
```java
@Service
public class QuotingService {

    @Qualifier("SESSION1") //this is not needed if only 1 session is defined in quickfixj.cfg
    @Autowired
    private ReactiveFixSession fixSession;

    public Mono<Message> getQuote(QuoteRequest quoteRequest) {

        //Create logging context for MDC. Then when using a logging framework, you can use %id% in the log messages.
        try (LoggingContext loggingContext = LoggingUtils.loggingContext("myRequestId")) {

            //Send the FIX quote request message
            return testFixSession.sendAndSubscribe(()->quoteRequest)

                //Get the first message
                .next()

                //Optional: Set MDC thread-local values, to be used when logging messages from the reactive stream
                //This way the logging context set in the try-catch is not lost
                //(reactive notification happen in different thread, so thread local varaibles -like MDC- are lost)
                .subscriberContext(LoggingUtils.withLoggingContext(loggingContext));
        }
    }
}
```

Remember to `@EnableQuickFixJ` on the application context, to make the magic happen.
```java
@EnableQuickFixJ
@SpringBootApplication
public class ApplicationContext {

    public static void main(String[] args) {
        SpringApplication.run(ApplicationContext.class, args);
    }
}
```

## Imperative

#### Library

```xml
<dependency>
    <groupId>ch.voulgarakis</groupId>
    <artifactId>quickfixj-spring-boot-starter</artifactId>
    <version>1.0.0.RELEASE</version>
</dependency>
```

#### Usage Example

Similar to the reactive programming, we can implement our `QuotingService` using callbacks (imperative model):

```java
@Service
public class QuotingService {

    @Qualifier("SESSION1") //this is not needed if only 1 session is defined in quickfixj.cfg
    @Autowired
    private FixSession fixSession;

    public Message getQuote(QuoteRequest quoteRequest) {

        //Create logging context for MDC. Then when using a logging framework, you can use %id% in the log messages.
        try (LoggingContext loggingContext = LoggingUtils.loggingContext("myRequestId")) {

            //We store the first response in this future
            CompletableFuture<Message> response = new CompletableFuture<>();
            //We send the quote request, and responses/errors will invoke the callbacks
            AutoCloseable subscription = fixSession
                .sendAndSubscribe(quoteRequest,
                    quote->{
                        //The first response will set the future
                        response.complete(quote);
                    },
                    error->{
                        //Or if error/rejection, we set the future with exception
                        response.completeExceptionally(error);
                    });
            
            //The future now holds the quote(or error)
            Message quote = response.get();
            //Close the subscription
            subscription.close();
            //And now honestly, isn't reactive better???
            return quote;
        }
    }
}
```

## Quickfixj Actuator

#### Library

Import `quickfixj-spring-boot-actuator` into your project:
```xml
<dependency>
    <groupId>ch.voulgarakis</groupId>
    <artifactId>quickfixj-spring-boot-actuator</artifactId>
    <version>1.0.0.RELEASE</version>
</dependency>
```

By default, quickfixj management and health endpoints are enabled, when   
If you want to only enable quickfixj endpoints, in your application properties:
```properties
#Disable endpoints by default
management.endpoints.enabled-by-default=false
#But enable the quickfixj quickfixj info and health endpoints only!
management.endpoint.quickfixj.enabled=true
```

As per typical spring-boot-actuator fashion:
 * The configuration endpoint is available under `/actuator/quickfixj`
 * The health check endpoint is available under `/actuator/health/quickfixj`.

## License and Acknowledgement

The QuickFixJ Spring Boot Starter is released under version 2.0 of the Apache License.  
This code includes software developed by quickfixengine.org.