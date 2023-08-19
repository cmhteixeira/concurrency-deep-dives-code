package com.cmhteixeira.streams.publishers;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.reactivestreams.Publisher;

public abstract class CmhPublisher<T> implements Publisher<T> {
  public static <T> CmhPublisher<T> fromList(List<T> elems) {
    return new FromList<>(elems);
  }

  public static <T> CmhPublisher<T> error(Throwable error) {
    return new CmhError<>(error);
  }

  public <R> CmhPublisher<R> map(Function<? super T, ? extends R> mapper) {
    return new StreamMap<>(mapper, this);
  }

  public static CmhPublisher<Integer> range(int inclusive, int exclusive) {
    return new Range(inclusive, exclusive);
  }

  public static <A> Publisher<A> fromFuture(Future<A> fut) {
    return new FromFuture<>(fut);
  }

  public static <T> CmhPublisher<T> fromFuture(
      Future<? super T> fut, long timeout, TimeUnit timeUnit) {
    return null;
  }

  public static <T> CmhPublisher<T> fromCompletionStage(CompletionStage<? super T> fut) {
    return null;
  }
}
