package com.cmhteixeira.streams.tck;

import com.cmhteixeira.streams.publishers.CmhPublisher;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;

public class RangeTest extends PublisherVerification<Integer> {

  int numElems = 10_000;

  public RangeTest() {
    super(new TestEnvironment());
  }

  @Override
  public Publisher<Integer> createPublisher(long elements) {
    if (elements > Integer.MAX_VALUE) throw new IllegalArgumentException("too many: " + elements);
    return CmhPublisher.range(0, (int) elements);
  }

  @Override
  public Publisher<Integer> createFailedPublisher() {
    return null;
  }

  // ADDITIONAL CONFIGURATION

  @Override
  public long maxElementsFromPublisher() {
    return numElems;
  }

  @Override
  public long boundedDepthOfOnNextAndRequestRecursion() {
    return 1;
  }
}
