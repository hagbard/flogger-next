/*******************************************************************************
 * Copyright (c) 2024, David Beaumont (https://github.com/hagbard).
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v. 2.0 available at https://www.eclipse.org/legal/epl-2.0, or the
 * Apache License, Version 2.0 available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 ******************************************************************************/

package net.goui.flogger.backend.common;

import com.google.common.flogger.MetadataKey;

/**
 * Loads metadata keys from string specifiers.
 *
 * <p>In order for metadata keys to be handled specially during formatting, it is important they can
 * be loaded early in the lifetime of an application, and without incurring unexpected static
 * initialization of classes.
 *
 * <p>As such it is recommended that custom keys are always contained in {@code public} static
 * classes (nested or top-level) and are themselves {@code public static final} fields. See {@link
 * com.google.common.flogger.LogContext.Key LogContext.Key} for an example of this pattern.
 */
public final class MetadataKeyLoader {
  /**
   * Loads a {@code public static} metadata key from a field-name specification of the form {@code
   * <class-name>#<field-name>}.
   *
   * @throws IllegalArgumentException if the key specification is invalid, or the field cannot be
   *     loaded.
   */
  public static MetadataKey<?> loadMetadataKey(String keySpecification) {
    // Expected: "foo.bar.Class#Field"
    int idx = keySpecification.indexOf('#');
    if (idx == -1) {
      throw new IllegalArgumentException("Invalid metadata key name: " + keySpecification);
    }
    String className = keySpecification.substring(0, idx);
    String fieldName = keySpecification.substring(idx + 1);
    try {
      return (MetadataKey<?>) Class.forName(className).getDeclaredField(fieldName).get(null);
    } catch (Exception e) {
      throw new IllegalArgumentException("Cannot load metadata key: " + keySpecification, e);
    }
  }

  private MetadataKeyLoader() {}
}
