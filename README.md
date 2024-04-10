# stellate-graphql-java

This Java library integrates Stellate with your existing GraphQL Java and allows you to log metrics about your requests to Stellate.

It achieves this by exposing an [`Instrumentation`](https://www.graphql-java.com/documentation/instrumentation) that can be used with [GraphQL Java](https://www.graphql-java.com/).

## Installation

This library is published to the Maven Central Repository:

- `groupId`: `co.stellate`
- `artifactId`: `stellate`

The library expects `com.graphql-java:graphql-java` to be installed in your project as well.

### Gradle

Add the library to your list of dependecies in your `build.gradle` file:

```gradle
dependencies {
  implementation 'co.stellate:stellate'
}
```

### Maven

Add the dependency like so in your `pom.xml` file:

```xml
<project>
  <dependencies>
    <dependency>
      <groupId>co.stellate</groupId>
      <artifactId>stellate</artifactId>
      <version>0.0.1</version>
    </dependency>
  </dependencies>
</project>
```

## Set up

When building your `GraphQL` object (from `com.graphql-java`), you can add the `StellateInstrumentation` exposed by this library like so:

```java
import co.stellate.stellate.StellateInstrumentation;

// The name of your Stellate service
String serviceName = "my-stellate-service";

// A logging token for the above Stellate service
String token = "stl8log_xyz";

GraphQL myGraphQL = GraphQL.newGraphQL(schema)
    .instrumentation(new StellateInstrumentation(serviceName, token))
    .build();
```

### Using with Spring

If you use [Spring for GraphQL](https://spring.io/projects/spring-graphql) then you can get access to the `GraphQL` object to add the `StellateInstrumentation` like so:

```java
import org.springframework.boot.autoconfigure.graphql.GraphQlSourceBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import co.stellate.stellate.StellateInstrumentation;

@Configuration(proxyBeanMethods = false)
class GraphQlConfig {
    @Bean
    public GraphQlSourceBuilderCustomizer sourceBuilderCustomizer() {
        return (builder) -> builder.configureGraphQl(graphQlBuilder -> {
            graphQlBuilder.instrumentation(new StellateInstrumentation(
                serviceName,
                token
            ));
        });
    }
}
```
