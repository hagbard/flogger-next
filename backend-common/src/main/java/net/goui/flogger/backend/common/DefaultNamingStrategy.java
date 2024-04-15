package net.goui.flogger.backend.common;

import static java.util.Arrays.stream;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class DefaultNamingStrategy implements NamingStrategy {
  private static final String OPTION_TRIM_AT_LEAST = "trim_at_least";
  private static final String OPTION_RETAIN_AT_MOST = "retain_at_most";

  private final int trimAtLeast;
  private final int retainAtMost;

  /** Map of trailing wildcard counts for root entries with trailing ".*" sequences. */
  private final Map<String, Integer> rootExtensions;

  /** Sorted array of combined explicit and system roots (without trailing wildcards). */
  private final String[] loggerRoots;

  /** Array of coded hierarchy indices, aligned to match the sorted roots. */
  private final int[] rootIndices;

  DefaultNamingStrategy(Options options) {
    this.trimAtLeast = unsignedInt(options, OPTION_TRIM_AT_LEAST);
    this.retainAtMost = unsignedInt(options, OPTION_RETAIN_AT_MOST);

    // Can contain trailing ".*.*" style wildcards to denote how many child levels to keep.
    List<String> explicitRoots = options.getStringArray("roots");
    this.rootExtensions = getRootExtensions(explicitRoots);
    List<String> systemRoots = options.getStringArray("system_roots");
    // Create the sorted array of root entries. This ordering is vital to correct lookup since,
    // for any given class name, the index of its parent entry cannot come after the insertion
    // index of the class name.
    this.loggerRoots =
        Stream.concat(
                // Remove trailing '.*' wildcards for explicit roots.
                explicitRoots.stream().map(this::removeAllWildcards),
                // Don't include the global system root (we don't want it to match everything).
                systemRoots.stream().filter(s -> !s.isEmpty()))
            .peek(DefaultNamingStrategy::checkValidRootName)
            // System roots and explicit roots can overlap (esp. after wildcard removal).
            .distinct()
            .sorted(DefaultNamingStrategy::compareRootNames)
            .toArray(String[]::new);
    // Create the array of index and depth information associated with each root.
    this.rootIndices = makeRootIndices(loggerRoots);
  }

  /** Helper to return an unsigned option value (possibly move into Options class later). */
  private static int unsignedInt(Options options, String name) {
    int value = Math.toIntExact(options.getLong(name, 0));
    if (value < 0) {
      throw new IllegalArgumentException(
          "Option '" + name + "' cannot be negative (was " + value + ")");
    }
    return value;
  }

  /** Counts occurrences of trailing ".*" wildcard sequences (including cases like "*" or "*.*"). */
  private static int countWildcards(String root) {
    int i = root.length();
    while (i >= 2 && root.charAt(i - 1) == '*' && root.charAt(i - 2) == '.') {
      i -= 2;
    }
    int wc = (root.length() - i) / 2;
    if (i == 1 && root.charAt(0) == '*') {
      wc += 1;
    }
    return wc;
  }

  /**
   * Returns a map of trailing wildcard counts for the given list of root entries.
   *
   * <ul>
   *   <li>{@code "foo.bar.*"}: {@code "foo.bar" -> 1}
   *   <li>{@code "foo.*.*"}: {@code "foo" -> 2}
   *   <li>{@code "*.*"}: {@code "" -> 2}
   *   <li>{@code "foo.bar"}: No mapping.
   * </ul>
   */
  private static Map<String, Integer> getRootExtensions(List<String> roots) {
    Map<String, Integer> map = new HashMap<>();
    for (String root : roots) {
      if (root.isEmpty()) {
        throw new IllegalArgumentException("logger root cannot be empty");
      }
      int wc = countWildcards(root);
      if (wc > 0) {
        String nonWildcardRoot = removeWildcards(root, wc);
        if (map.put(nonWildcardRoot, wc) != null) {
          throw new IllegalArgumentException("multiple logger roots match: " + nonWildcardRoot);
        }
      }
    }
    return map;
  }

  /** Removes N trailing ".*" wildcards from the specified root entry, possibly leaving it empty. */
  private static String removeWildcards(String root, int count) {
    // Efficient either if (count == 0) or the entire root is only wildcards.
    return root.substring(0, Math.max(root.length() - (2 * count), 0));
  }

  /** Removes all wildcards from the root entry. */
  private String removeAllWildcards(String root) {
    return removeWildcards(root, countWildcards(root));
  }

  @Override
  public boolean shouldCacheBackends() {
    // Any logging class trimming is likely to greatly reduce the number of backends allocated, and
    // so should benefit from caching. We don't trigger caching just for the existence of root
    // entries, since that might only cover a small fraction of loggers.
    return trimAtLeast > 0 || retainAtMost > 0;
  }

  @Override
  public String getBackendName(String className) {
    int rootIdx =
        Arrays.binarySearch(
            loggerRoots, 0, loggerRoots.length, className, DefaultNamingStrategy::compareRootNames);
    if (rootIdx >= 0) {
      // Found an exact match, return it.
      return className;
    }
    // We didn't match the root, but might be below it, or we might be below one of its parents.
    // To determine the correct parent, find the first point of difference between the class name
    // and the root entry immediately before us.
    int parentIdx = ~rootIdx - 1;
    if (parentIdx >= 0) {
      String parentCandidate = loggerRoots[parentIdx];
      // Find the depth at or below which a candidate will be a parent.
      int parentDepth = findMaxParentDepth(className, parentCandidate);
      // If -1, the first segment differs, so no parent exists.
      if (parentDepth >= 0) {
        while (true) {
          int codedIndex = rootIndices[parentIdx];
          if ((codedIndex & 0xFFFF) <= parentDepth) {
            // Return the first parent candidate with a depth <= the given name.
            return possiblyExtendRoot(parentCandidate, className);
          }
          // If -1, we ran out of parent candidates to test.
          parentIdx = codedIndex >> 16;
          if (parentIdx < 0) {
            break;
          }
          parentCandidate = loggerRoots[parentIdx];
        }
      }
    }
    // No root matches, so return the (possibly truncated) name.
    return truncateClassName(className, trimAtLeast, retainAtMost);
  }

  /**
   * Returns the maximum depth for the parent entry in the list of roots.
   *
   * @return the maximum parent depth (matching the encoded values in parentIndices) or -1 if there
   *     is no parent (i.e. a difference appears in the first segment).
   */
  private int findMaxParentDepth(String name, String parentCandidate) {
    // We know the parentCandidate is not equal to the name (from calling code), and if it were a
    // prefix of it, the candidate would appear after the class name in the sorted list of roots.
    // So either:
    // 1. The candidate is a parent of the name (a prefix ending at a '.').
    // 2. The candidate has a difference at some earlier depth.
    int depth = 0;
    int length = Math.min(parentCandidate.length(), name.length());
    for (int i = 0; i < length; i++) {
      char chr = name.charAt(i);
      if (chr != parentCandidate.charAt(i)) {
        return depth;
      }
      if (chr == '.') {
        depth += 1;
      }
    }
    // All common characters match, so since the candidate sorts *before* the name, the
    // name must be longer, but this doesn't mean the candidate is a parent.
    assert name.length() > length : "name expected to be longer than candidate";
    return name.charAt(length) == '.' ? depth + 1 : depth;
  }

  private String possiblyExtendRoot(String root, String className) {
    int extendBy = rootExtensions.getOrDefault(root, 0);
    if (extendBy == 0) {
      return root;
    }
    // Root can be "" here, but setting the 'dot' index to just before the start works.
    int dot = root.isEmpty() ? -1 : root.length();
    for (; extendBy > 0; extendBy--) {
      dot = className.indexOf('.', dot + 1);
      if (dot == -1) {
        return className;
      }
    }
    return className.substring(0, dot);
  }

  private static String truncateClassName(String className, int trimAtLeast, int retainAtMost) {
    int trimIdx = trimNestedOrInnerClasses(className, className.length());

    if (trimAtLeast > 0) {
      // (trimIdx == 0) means the name starts with '.' and we have trimmed everything.
      for (int trimmed = 0; trimIdx > 0 && trimmed < trimAtLeast; trimmed++) {
        trimIdx = className.lastIndexOf('.', trimIdx - 1);
      }
    }

    if (trimIdx > 0 && retainAtMost > 0) {
      int idx = className.indexOf('.');
      for (int depth = 1; 0 <= idx && idx < trimIdx && depth < retainAtMost; depth++) {
        idx = className.indexOf('.', idx + 1);
      }
      // We only get (idx < 0) if we did not trim anything (i.e. trimIdx == length), and we ran out
      // of segments. In that case we can just return the complete name.
      if (idx >= 0) {
        trimIdx = idx;
      }
    }
    // This is efficient if (trimIdx == length).
    return trimIdx > 0 ? className.substring(0, trimIdx) : "";
  }

  private static int trimNestedOrInnerClasses(String className, int trimIdx) {
    int classStart = className.lastIndexOf('.') + 1;
    if (classStart > 0) {
      // Skip the first character to guarantee we have a non-empty name (even if it starts with
      // '$').
      int innerStart = className.indexOf("$", classStart + 1);
      if (innerStart != -1) {
        trimIdx = innerStart;
      }
    }
    return trimIdx;
  }

  private static int compareRootNames(String lhs, String rhs) {
    int length = Math.min(lhs.length(), rhs.length());
    for (int i = 0; i < length; i++) {
      // Note: While Java identifiers *can* be formed from surrogate pairs, we don't have to
      // explicitly care about that here. The reason is that chars in a surrogate pair still
      // have a valid ordering when compared char-at-a-time (because the hi-surrogate is first
      // and orders the higher bits of the encoded code-point). The final order isn't identical
      // to ordering the code points themselves, but it's consistent, and we don't care about
      // the exact order used here, only that it works for the binary search.
      char lc = lhs.charAt(i);
      char rc = rhs.charAt(i);
      if (lc == rc) {
        continue;
      }
      if (lc == '.') {
        // lc < rc
        return -1;
      }
      if (rc == '.') {
        // rc < lc
        return 1;
      }
      // We can order unequal chars safely, including UTF-16 surrogate pair values.
      return lc < rc ? -1 : 1;
    }
    // Exhausted at least one of the inputs, so either equal, or one input is a prefix to the other.
    // Sort shorter inputs first.
    return length < rhs.length() ? -1 : length < lhs.length() ? 1 : 0;
  }

  private static void checkValidRootName(String pkg) {
    if (pkg.isEmpty()) {
      return;
    }
    // Names are limited by class-file semantics to 65535 UTF-8 bytes, and this ensures that the
    // eventual depth of a valid name is no more than 32768 (since dots must be separated by at
    // least one character). Knowing that depth is limited is good later.
    if (pkg.length() > 0xFFFF
        // Apologies for the truly ugly use of String#split(), Guava isn't an option here.
        || !stream(pkg.split("[.]", -1)).allMatch(DefaultNamingStrategy::isValidNamePart)) {
      throw new IllegalArgumentException("invalid root name: '" + pkg + "'");
    }
  }

  private static boolean isValidNamePart(String part) {
    return !part.isEmpty()
        && Character.isJavaIdentifierStart(part.codePointAt(0))
        && part.codePoints().skip(1).allMatch(Character::isJavaIdentifierPart);
  }

  private static int[] makeRootIndices(String[] roots) {
    if (roots.length > 0xFFFF) {
      throw new IllegalArgumentException("too many logger roots (no more than 65535 allowed)");
    }
    int[] indices = new int[roots.length];
    for (int idx = 0; idx < roots.length; idx++) {
      String pkg = roots[idx];
      // Depth is limited to 2 bytes (see above) and we also limit parent indices.
      int depth = pkg.isEmpty() ? 0 : numberOfDots(pkg) + 1;
      // With index in high-bit, the value can be tested for "no parent" by just checking if -ve.
      indices[idx] = (getParentIndex(pkg, idx, roots) << 16) | depth;
    }
    return indices;
  }

  private static int numberOfDots(String pkg) {
    return Math.toIntExact(pkg.chars().filter(c -> c == '.').count());
  }

  private static int getParentIndex(String pkg, int idx, String[] roots) {
    for (int j = idx - 1; j >= 0; j--) {
      // parent can be "" if the global root was present (e.g. via "*.*")
      String parent = roots[j];
      if (parent.isEmpty() || (pkg.startsWith(parent) && pkg.charAt(parent.length()) == '.')) {
        return j;
      }
    }
    return -1;
  }
}
