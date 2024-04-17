package net.goui.flogger.backend.log4j;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import com.google.common.flogger.LogContext;
import com.google.common.flogger.LogSite;
import com.google.common.flogger.backend.LogData;
import com.google.common.flogger.backend.MetadataProcessor;
import java.util.Map;
import java.util.Optional;
import net.goui.flogger.backend.common.FloggerLogEntry;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.AbstractLogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.StringBuilderFormattable;

public class Log4jEvent extends AbstractLogEvent
    implements FloggerLogEntry, Message, StringBuilderFormattable {
  private static final Object[] EMPTY_ARGS = new Object[0];

  private final LogData logData;
  private final MetadataProcessor metadata;

  // Not (solely) available in LogData, must be evaluated at construction (in the logging thread).
  private final ThreadContext.ContextStack contextStack;
  private final ReadOnlyStringMap contextData;

  // Mutable thread data, should be evaluated at construction (in the logging thread).
  private final String threadName;
  private final long threadId;
  private final int threadPriority;

  // Can be derived idempotently from LogData and cached.
  private volatile Instant timestamp = null;
  private volatile StackTraceElement source = null;
  private volatile Optional<ThrowableProxy> thrownProxy = null;

  // Only used for asynchronous message handling.
  private volatile String cachedFormattedMessage = null;

  Log4jEvent(LogData logData, MetadataProcessor metadata) {
    this.logData = requireNonNull(logData);
    this.metadata = requireNonNull(metadata);

    this.contextStack = ThreadContext.getImmutableStack();
    this.contextData = Log4jEventUtil.createContextMap(metadata);

    // Resolve the current thread information here since none of these methods are idempotent, and
    // we want the state at the point logging starts. It costs a few extra fields, but it won't
    // cause weird behaviour later.
    Thread currentThread = Thread.currentThread();
    this.threadName = currentThread.getName();
    // Switch to calling threadId() once at JDK 19+.
    this.threadId = currentThread.getId();
    this.threadPriority = currentThread.getPriority();
  }

  /**
   * @deprecated use {@link #getContextData()} instead.
   */
  @Deprecated
  @Override
  public Map<String, String> getContextMap() {
    return getContextData().toMap();
  }

  @Override
  public ReadOnlyStringMap getContextData() {
    return contextData;
  }

  @Override
  public ThreadContext.ContextStack getContextStack() {
    return contextStack;
  }

  @Override
  public Level getLevel() {
    return Log4jEventUtil.getLog4jLevel(getLogData().getLevel());
  }

  /**
   * Returns the class name associated with the log statement. This is a misleadingly named method,
   * since the returned value need not be either "the logging class" (it could be a nested class) or
   * the name of the backend logger.
   *
   * <p>From the Log4J documentation:<br>
   * "Returns the fully qualified class name of the caller of the logging API."
   */
  @Override
  public String getLoggerFqcn() {
    return getLogData().getLogSite().getClassName();
  }

  /**
   * Returns the name of the underlying Log4J logger, which serves as the backend to the
   * FluentLogger instance. This need not be associated with the original name of the logging class.
   */
  @Override
  public String getLoggerName() {
    return getLogData().getLoggerName();
  }

  /** Returns {@link Message} API, which is a loggable view onto the log event. */
  @Override
  public Message getMessage() {
    return this;
  }

  /**
   * Returns a (possibly cached) {@link StackTraceElement}, denoting the log site information of
   * this event. Note that {@code StackTraceElement} requires that the given class name be the
   * "binary name" of the class (e.g. using '$' for nested/inner classes "com.foo.Bar$Baz"), but
   * that's what {@link LogSite} returns.
   */
  @Override
  public StackTraceElement getSource() {
    if (source == null) {
      LogSite logSite = getLogData().getLogSite();
      source =
          new StackTraceElement(
              logSite.getClassName(),
              logSite.getMethodName(),
              logSite.getFileName(),
              logSite.getLineNumber());
    }
    return source;
  }

  @Override
  public long getThreadId() {
    return threadId;
  }

  @Override
  public String getThreadName() {
    return threadName;
  }

  @Override
  public int getThreadPriority() {
    return threadPriority;
  }

  @Override
  public Throwable getThrown() {
    return getLogData().getMetadata().findValue(LogContext.Key.LOG_CAUSE);
  }

  @Override
  public ThrowableProxy getThrownProxy() {
    if (thrownProxy == null) {
      Throwable thrown = getThrown();
      thrownProxy = thrown != null ? Optional.of(new ThrowableProxy(thrown)) : Optional.empty();
    }
    return thrownProxy.orElse(null);
  }

  @Override
  public long getTimeMillis() {
    return NANOSECONDS.toMillis(getLogData().getTimestampNanos());
  }

  @Override
  public Instant getInstant() {
    if (timestamp == null) {
      this.timestamp = Log4jEventUtil.getLog4jInstantFromNanos(logData.getTimestampNanos());
    }
    return timestamp;
  }

  @Override
  public long getNanoTime() {
    return getLogData().getTimestampNanos();
  }

  // **** net.goui.flogger.backend.common.FloggerLogEntry ****

  @Override
  public LogData getLogData() {
    return logData;
  }

  @Override
  public MetadataProcessor getMetadataProcessor() {
    return metadata;
  }

  @Override
  public StringBuilder appendFormattedMessageTo(StringBuilder buffer) {
    return Log4jBackendFactory.getFormatter().append(getLogData(), getMetadataProcessor(), buffer);
  }

  // **** org.apache.logging.log4j.util.StringBuilderFormattable ****

  @Override
  public void formatTo(StringBuilder buffer) {
    appendFormattedMessageTo(buffer);
  }

  // **** org.apache.logging.log4j.message.Message ****.

  @Override
  public String getFormattedMessage() {
    // * "When configured to log asynchronously, this method is called before the Message is queued"
    // * "The intention is that the Message implementation caches this formatted message and returns
    //   it on subsequent calls. (See LOG4J2-763)"
    // * "When logging synchronously, this method WILL NOT BE CALLED for Messages that implement the
    //   StringBuilderFormattable interface."
    if (cachedFormattedMessage == null) {
      cachedFormattedMessage = appendFormattedMessageTo(new StringBuilder()).toString();
    }
    return cachedFormattedMessage;
  }

  /**
   * Returns the empty string to indicate this {@link Message} has no concept of accessing the
   * unformatted message data.
   *
   * <p>{@code Message} is a bit of an awkwardly designed API, since it appears to be trying to
   * allow callers to use the unformatted message information, while simultaneously being agnostic
   * about which format is used. This is something Flogger explicitly avoids (since it inevitably
   * creates ambiguity and confusion).
   */
  @Override
  public String getFormat() {
    return "";
  }

  /**
   * Returns the empty string to indicate this {@link Message} has no concept of accessing the
   * unformatted message parameters.
   */
  @Override
  public Object[] getParameters() {
    return EMPTY_ARGS;
  }

  /** Returns the cause associated with this {@link Message}. */
  @Override
  public Throwable getThrowable() {
    return getThrown();
  }
}
