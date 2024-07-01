# Improved Flogger APIs and Functionality

Flogger is a powerful logging API for Java, but the core library lacks configuration options and
doesn't make use of modern Java features.

The Flogger Next project aims to unlock new, powerful features for Flogger via API extensions and
new backend implementations.

This project provides two powerful extensions:

## Integration with String Template syntax for logging

Use the new String Template syntax to include variables directly in formatted message.

<!-- @formatter:off -->
```java
// Evaluate log arguments directly with the String Template syntax.
logger.atInfo()."\{x} + \{y} = \{x + y}".log();
```
<!-- @formatter:on -->

See the [logger artifact](logger/README.md) for more,
or [read the manual](https://hagbard.github.io/the-flogger-manual).

## Customizable logger backends with improved efficiency

Improve efficiency and customization for [JDK logging](backend-system/README.md)
or [Log4J2](backend-log4j/README.md).

Features include:

* Customizable [message formatting](https://hagbard.github.io/the-flogger-manual/next/formatter).
* Efficient, customizable
  [logger backend allocation strategy](https://hagbard.github.io/the-flogger-manual/next/backend).

## About the author

David Beaumont is the designer and author of Google's Flogger logging library.
