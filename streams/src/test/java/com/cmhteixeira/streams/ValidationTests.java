package com.cmhteixeira.streams;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    //    Flowable.fromPublisher(
    //            CmhPublisher.fromList(IntStream.range(0,
    // 20).boxed().collect(Collectors.toList())))
    //        .subscribe(System.out::println, System.out::println, System.out::println);
    var t =
        Flowable.fromPublisher(
                CmhPublisher.fromList(IntStream.range(0, 20).boxed().collect(Collectors.toList())))
            .takeUntil(p -> p == 4)
            .subscribe(System.out::println, System.out::println, System.out::println);

    System.out.println(t);
    //    var foo =
    //        Flowable.fromIterable(IntStream.range(0, 20).boxed().collect(Collectors.toList()))
    //            .blockingNext()
    //            .iterator();
    //
    //    while (foo.hasNext()) {
    //      Integer res = foo.next();
    //      System.out.println(res);
    //    }
  }

  @Test
  public void askForLessElemsThanSize() {
    var res =
        Flowable.fromPublisher(
                CmhPublisher.fromList(IntStream.range(0, 20).boxed().collect(Collectors.toList())))
            .take(2)
            .count()
            .blockingGet();
    assertEquals(res, 2);
  }
}
