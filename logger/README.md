# Next Generation Flogger API

Read the manual here: https://hagbard.github.io/the-flogger-manual/next/

Utilize Java's new `StringTemplate` syntax to improve readability of your log statements.

This artifact provides a drop-in replacement for Google's `FluentLogger` class enables use of the
proposed (JDK 21) `StringTemplate` functionality alongside all of Flogger's other features.

## Using String Template syntax

The new String Template syntax use variable directly inside formatted strings without incurring the
cost of string concatenation at the log site.

<!-- @formatter:off -->
```java
// Evaluate log arguments directly with the String Template syntax.
logger.atInfo()."\{x} + \{y} = \{x + y}".log();

// Use optional String Formatter syntax if desired.
logger.atInfo()."%d\{n} = %#x\{n} in hexadecimal".log();
```
<!-- @formatter:on -->

Use lazy arguments for greater efficiency in log statements that are disabled by default.

<!-- @formatter:off -->
```java
// Neither the template, nor lazy arguments are evaluated when logging is disabled.  
logger.atFine()."Statistics: \{lazy(() -> collectStatsExpensive())}".log();
```
<!-- @formatter:on -->

## Compatibility

This works efficiently with all of Flogger's existing features such as:

* Zero evaluation for disabled log statements.
* Zero evaluation for rate-limited log statements.
* Compatible with Flogger's `LazyArg` mechanism to defer expensive work at the log site.
* Compatible with all existing fluent API methods, such as `withCause()`, `atMostEvery()` etc.

> **Note**
> Since this functionality is currently (May 2024) only being previewed in JDK 21, you will have to
> opt into the JDK preview features by setting the `--enable-preview` flag if you want to use it.

## Installation

Maven dependency:

<!-- @formatter:off -->
```xml
<dependency>
    <groupId>net.goui.flogger-next</groupId>
    <artifactId>logger</artifactId>
    <version>${flogger-next.version}</version>
</dependency>
```
<!-- @formatter:on -->

Simply import the following class in preference to Google's `FluentLogger`

<!-- @formatter:off -->
```java
import net.goui.flogger.FluentLogger;
```
<!-- @formatter:on -->

This class deliberately uses the same class name of `FluentLogger` to allow migration by simply
changing import statements. Since it inherits from the same base API as Google's `FluentLogger`, it
should always be safe to swap to this class.
