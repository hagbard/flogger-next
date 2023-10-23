# Improved Flogger APIs and Functionality

This project aims to add unlock new, powerful features for Flogger via API extensions, new backend
implementations and utility classes.

Currently, it provides two useful extensions:

## Automatic Task Tracking in Logs

Track your tasks and sub-tasks with unique hierarchical IDs per invocation, allowing you to reliably
reconstruct the execution flow of your code from debug logs.
[Learn more](https://github.com/hagbard/flogger-next/tree/main/tasks).

```xml
<!-- https://mvnrepository.com/artifact/net.goui.flogger-next/tasks -->
<dependency>
    <groupId>net.goui.flogger-next</groupId>
    <artifactId>tasks</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Extended String Template Log Syntax

Utilize Java's new `StringTemplate` syntax to improve readability of your log statements.
[Learn more](https://github.com/hagbard/flogger-next/tree/main/logger).

```xml
<!-- https://mvnrepository.com/artifact/net.goui.flogger-next/logger -->
<dependency>
    <groupId>net.goui.flogger-next</groupId>
    <artifactId>logger</artifactId>
    <version>1.0.0</version>
</dependency>
```

## About

David Beaumont is the designer and author of Google's Flogger logging library.