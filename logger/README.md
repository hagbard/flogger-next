# Next Generation Flogger API

Utilize Java's new `StringTemplate` syntax to improve readability of your log statements.

This small, drop-in extension of Google's `FluentLogger` logging API enables the proposed JDK
22`StringTemplate` functionality (current available as a preview in JDK 21).

## Syntax

This lets you write your log statements using the new string template syntax:

<!-- @formatter:off -->
```java
logger.atInfo()."fn(\{x}, \{y}) = \{lazy(() -> expensiveFn(x, y))}".log();
```
<!-- @formatter:on -->

which is equivalent to:

<!-- @formatter:off -->
```java
logger.atInfo().log("fn(%s, %s) = %s", x, y, lazy(() -> expensiveFn(x, y)));
```
<!-- @formatter:on -->

However, it also works with multiline comments such as:

<!-- @formatter:off -->
```java
// Ignores leading newline and common indentation.
logger.atInfo()."""
    {
      foo = "\{foo}",
      bar = "\{bar}",
    }""".log();
```
<!-- @formatter:on -->

which can make longer log statements far more readable.

## Compatibility

This works efficiently with all of Flogger's existing features such as:

* Zero evaluation for disabled log statements.
* Zero evaluation for rate-limited log statements.
* Supports Flogger's `LazyArg` mechanism to further defer expensive work at the call-site.
* Complete integration with all the existing fluent API methods, such as
  `withCause()`, `atMostEvery()` etc.

However since the new StringTemplate syntax does not support all of printf's formatting (
e.g. `"%08x"`), it may not be best suited for all log statements. But for these cases you can still
use Flogger's normal `log()` methods.

This class deliberately uses the exact class name of `FluentLogger` to allow migration by simply
changing import statements. Since it inherits from the same fluent API as Google's `FluentLogger`,
it should always be safe to swap to this class.

> **Note**
>
> Obviously since this functionality is currently (Oct 2023) only being previewed in the JDK, you
> will have to opt into the JDK preview features by setting the `--enable-preview` flag if you want
> to use it.

## Future Work

It is expected that other features, such a new fluent methods, will be also added to this class.