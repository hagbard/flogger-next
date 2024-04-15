package net.goui.flogger.backend.common.formatter;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.*;

import com.google.common.flogger.backend.Metadata;
import com.google.common.flogger.backend.MetadataProcessor;
import com.google.common.flogger.testing.FakeLogData;
import com.google.common.flogger.testing.FakeLogSite;
import net.goui.flogger.backend.common.Options;
import org.junit.Test;

public class DefaultLocationFormatterTest {
  @Test
  public void testDefaultFormat() {
    DefaultLocationFormatter fmt = new DefaultLocationFormatter(Options.of(k -> null));
    FakeLogData data =
        FakeLogData.of("<message>")
            .setLogSite(FakeLogSite.create("com.foo.bar.Class", "someMethod", 123, "<unused>"));

    assertThat(fmt.format(data, noMetadata())).isEqualTo("com.foo.bar.Class#someMethod");
  }

  private static MetadataProcessor noMetadata() {
    return MetadataProcessor.forScopeAndLogSite(Metadata.empty(), Metadata.empty());
  }
}
