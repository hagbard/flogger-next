package net.goui.flogger.backend.common;

public final class OptionParseException extends IllegalStateException {
  OptionParseException(String fqn, String type, String value, Throwable e) {
    this(String.format("cannot parse option %s as %s from '%s'", fqn, type, value), e);
  }

  OptionParseException(String message, Object... args) {
    super(String.format(message, args));
  }
}
