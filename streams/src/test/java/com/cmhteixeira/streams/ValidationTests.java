package com.cmhteixeira.streams;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cmhteixeira.streams.publishers.CmhPublisher;
import com.cmhteixeira.streams.subscribers.CountSubscriber;
import io.reactivex.rxjava3.core.Flowable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class ValidationTests {
  @Test
  public void foo() throws InterruptedException {
    Publisher<String> errorPublisher = CmhPublisher.error(new RuntimeException("fooBar"));
    //    Flowable.fromPublisher(
    //            CmhPublisher.fromList(IntStream.range(0,
    // 20).boxed().collect(Collectors.toList())))
    //        .subscribe(System.out::println, System.out::println, System.out::println);
    //    var t =
    //        Flowable.fromPublisher(
    //                CmhPublisher.fromList(IntStream.range(0,
    // 20).boxed().collect(Collectors.toList())))
    //
    //            .takeUntil(p -> p == 4)
    //                .
    //            .subscribe(System.out::println, System.out::println, System.out::println);

    //    System.out.println(t);
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

  @Test
  public void bar() {
    List<Integer> acc = new ArrayList<>();
    CmhPublisher.fromList(IntStream.range(0, 20).boxed().collect(Collectors.toList()))
        .subscribe(
            new Subscriber<>() {

              Subscription s;

              @Override
              public void onSubscribe(Subscription s) {
                this.s = s;
                s.request(20);
              }

              @Override
              public void onNext(Integer integer) {
                acc.add(integer);
                s.request(2);
              }

              @Override
              public void onError(Throwable t) {
                System.out.println("Error: " + t);
              }

              @Override
              public void onComplete() {
                System.out.println("Complete");
              }
            });

    System.out.println(acc);
  }

  @Test
  public void mapWorks() {
    List<Integer> source = IntStream.range(0, 20).boxed().toList();
    Function<Integer, Integer> mapper = l -> l * 10;

    assertEquals(
        source.stream().map(mapper).toList(),
        Flowable.fromPublisher(CmhPublisher.fromList(source).map(mapper))
            .collect(Collectors.toList())
            .blockingGet());
  }

  @Test
  public void qux() throws InterruptedException, ExecutionException {
    System.out.println("Current thread: " + Thread.currentThread().getName());
    int numElems = 1000;
    CountSubscriber<Integer> subs = new CountSubscriber<>();
    //    Flowable.fromPublisher(CmhPublisher.range(0, numElems))
    //      CmhPublisher.range(0, numElems)
    new RangePublisher(0, numElems)
        //    Flowable.range(0, numElems)
        //        .observeOn(new ExecutorScheduler(Executors.newFixedThreadPool(2), false, false))
        //        .subscribeOn(new ExecutorScheduler(Executors.newFixedThreadPool(2), false, false))

        .subscribe(subs);
    //    assertEquals(numElems, subs.counter().get());
  }
}
