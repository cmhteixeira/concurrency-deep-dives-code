package com.cmhteixeira.streams;

import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.cmhteixeira.streams.publishers.CmhPublisher;
import com.google.common.collect.ImmutableList;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableSubscriber;
import io.reactivex.rxjava3.core.FlowableTransformer;
import org.junit.jupiter.api.Test;
import scala.PartialFunction;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FromFutureTest {

  @Test
  public void foo() {
    String elem = "foo";
    var res =
        ImmutableList.copyOf(
            Flowable.fromPublisher(CmhPublisher.fromFuture(CompletableFuture.completedFuture(elem)))
                .blockingIterable());

    assertEquals(res, List.of(elem));
  }

  @Test
  public void flowableTest() throws InterruptedException {
    Flowable.fromSupplier()
    Flowable<Long> g = Flowable.interval(2, TimeUnit.SECONDS);
    var disposable = g.subscribe(System.out::println);

    Thread.sleep(10000);
    disposable.dispose();
  }

  @Test
  public void akkaStreamTest() throws InterruptedException {
    final ActorSystem system = ActorSystem.create("QuickStart");
    Source<String, Cancellable> g =
        Source.tick(Duration.ZERO, Duration.of(2, ChronoUnit.SECONDS), "s");
    var t = g.runForeach(System.out::println, system);
    var res = g.to(Sink.ignore()).run(system);
    var res2 = g.to(Sink.collect(Collectors.toList())).run(system);

    var fooo = g.fla
  }
}
