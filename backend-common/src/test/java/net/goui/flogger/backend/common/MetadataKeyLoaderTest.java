package net.goui.flogger.backend.common;

import static com.google.common.truth.Truth.assertThat;
import static net.goui.flogger.backend.common.MetadataKeyLoader.loadMetadataKey;
import static org.junit.Assert.assertThrows;

import com.google.common.flogger.MetadataKey;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MetadataKeyLoaderTest {
  public static final MetadataKey<String> KEY = MetadataKey.single("key", String.class);
  private static final MetadataKey<String> PRIVATE_KEY = MetadataKey.single("private", String.class);

  private static final String CLASS_NAME = MetadataKeyLoaderTest.class.getName();

  @Test
  public void testLoad() {
    assertThat(loadMetadataKey(CLASS_NAME + "#KEY")).isSameInstanceAs(KEY);
  }

  @Test
  public void testBadName() {
    assertThrows(IllegalArgumentException.class, () -> loadMetadataKey(""));
    assertThrows(IllegalArgumentException.class, () -> loadMetadataKey(CLASS_NAME));
    assertThrows(IllegalArgumentException.class, () -> loadMetadataKey(CLASS_NAME + ".KEY"));
    assertThrows(IllegalArgumentException.class, () -> loadMetadataKey(CLASS_NAME + "#KEYX"));
  }

  @Test
  public void testNonPublicKey() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> loadMetadataKey(CLASS_NAME + "#PRIVATE_KEY"));
    assertThat(e).hasCauseThat().isInstanceOf(IllegalAccessException.class);
  }
}
