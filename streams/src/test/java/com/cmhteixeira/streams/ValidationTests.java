package com.cmhteixeira.streams;

import com.cmhteixeira.streams.ops.CmhPublisher;
import io.reactivex.rxjava3.core.Flowable;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

public class ValidationTests {
  @Test
  public void foo() throws InterruptedException {
    Publisher<String> errorPublisher = CmhPublisher.error(new RuntimeException("fooBar"));
    Flowable.fromPublisher(
            CmhPublisher.fromList(IntStream.range(0, 20).boxed().collect(Collectors.toList())))
        .subscribe(System.out::println, System.out::println, System.out::println);
  }
}
