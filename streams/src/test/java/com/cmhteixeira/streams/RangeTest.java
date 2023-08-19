package com.cmhteixeira.streams;

import static org.testng.Assert.assertEquals;

import com.cmhteixeira.streams.publishers.CmhPublisher;
import io.reactivex.rxjava3.core.Flowable;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

public class RangeTest {

  @Test
  public void foo() {
    int inclusive = 1;
    int exclusive = 100_000_000;

    assertEquals(
        Flowable.fromPublisher(CmhPublisher.range(inclusive, exclusive))
            .collect(Collectors.toList())
            .blockingGet(),
        IntStream.range(inclusive, exclusive).boxed().toList());
  }

  @Test
  public void bar() {
    int inclusive = 1;
    int exclusive = Integer.MAX_VALUE - 10;

    var res =
        Flowable.fromPublisher(CmhPublisher.range(inclusive, exclusive))
            .collect(Collectors.summingLong(value -> value));

    double analytical = (double) exclusive / 2;
    assertEquals(res.blockingGet().longValue(), ((exclusive * 1L) * (exclusive * 1L - 1)) / 2);
  }
}
