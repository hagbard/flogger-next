package net.goui.flogger.backend.log4j;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class ValueQueueTest {
  @Test
  public void concat_singleItems() {
    Object head = ValueQueue.concat(null, 1);
    assertThat(head).isInstanceOf(Integer.class);

    head = ValueQueue.concat(head, 2);
    assertThat(head).isInstanceOf(ValueQueue.class);
    assertThat(head.toString()).isEqualTo("[1, 2]");

    head = ValueQueue.concat(head, 3);
    assertThat(head.toString()).isEqualTo("[1, 2, 3]");
  }

  @Test
  public void concat_queues() {
    Object head = ValueQueue.concat(1, 2);
    Object tail = ValueQueue.concat(3, 4);

    head = ValueQueue.concat(head, tail);
    assertThat(head.toString()).isEqualTo("[1, 2, 3, 4]");
  }

  @Test
  public void concat_arrays() {
    Object head = ValueQueue.concat(new int[] {1, 2}, new int[] {3, 4});
    assertThat(head.toString()).isEqualTo("[[1, 2], [3, 4]]");
  }
}
