/*******************************************************************************
 * Copyright (c) 2024, David Beaumont (https://github.com/hagbard).
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v. 2.0 available at https://www.eclipse.org/legal/epl-2.0, or the
 * Apache License, Version 2.0 available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 ******************************************************************************/

package net.goui.flogger;

import com.google.common.flogger.GoogleLoggingApi;
import java.lang.StringTemplate.Processor;

/** Extended Flogger API to support {@code StringTemplate} as part of a fluent log statement. */
public interface NextLoggingApi
    extends GoogleLoggingApi<NextLoggingApi>, Processor<LogString, RuntimeException> {}
