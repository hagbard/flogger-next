package net.goui.flogger;

import com.google.common.flogger.GoogleLoggingApi;
import java.lang.StringTemplate.Processor;

/** Extended Flogger API to support {@code StringTemplate} as part of a fluent log statement. */
public interface NextLoggingApi
    extends GoogleLoggingApi<NextLoggingApi>, Processor<LogString, RuntimeException> {}
