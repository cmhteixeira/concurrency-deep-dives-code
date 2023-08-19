package com.cmhteixeira.streams.tck;

import com.cmhteixeira.streams.publishers.CmhPublisher;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;

public class FromListPublisherTest extends PublisherVerification<Integer> {

  int numElems = 10_000;

  public FromListPublisherTest() {
    super(new TestEnvironment());
  }

  @Override
  public Publisher<Integer> createPublisher(long elements) {
    return CmhPublisher.fromList(
        IntStream.range(0, (int) Math.min(numElems, elements))
            .boxed()
            .collect(Collectors.toList()));
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
