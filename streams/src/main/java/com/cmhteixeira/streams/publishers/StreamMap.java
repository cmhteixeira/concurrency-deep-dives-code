package com.cmhteixeira.streams.publishers;

import java.util.function.Function;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

class StreamMap<T, R> extends CmhPublisher<R> {

  private final Function<? super T, ? extends R> mapper;
  private final CmhPublisher<T> originalPublisher;

  private Subscription upstreamSubs;
  private volatile Subscriber<? super R> downstreamSubscriber;

  StreamMap(Function<? super T, ? extends R> mapper, CmhPublisher<T> originalPublisher) {
    this.mapper = mapper;
    this.originalPublisher = originalPublisher;
  }

  @Override
  public void subscribe(Subscriber<? super R> s) {
    downstreamSubscriber = s;
    originalPublisher.subscribe(new UpstreamSubscriber());
  }

  private class DownstreamSubscription implements Subscription {

    @Override
    public void request(long n) {
      upstreamSubs.request(n);
    }

    @Override
    public void cancel() {
      downstreamSubscriber = null;
      upstreamSubs.cancel();
    }
  }

  private class UpstreamSubscriber implements Subscriber<T> {

    @Override
    public void onSubscribe(Subscription s) {
      upstreamSubs = s;
      downstreamSubscriber.onSubscribe(new DownstreamSubscription());
    }

    @Override
    public void onNext(T t) {
      R signDownstream;
      try {
        signDownstream = StreamMap.this.mapper.apply(t);
      } catch (Throwable error) {
        downstreamSubscriber.onError(
            new RuntimeException("Applying transformation to element: " + t, error));
        upstreamSubs.cancel();
        return;
      }
      downstreamSubscriber.onNext(signDownstream);
    }

    @Override
    public void onError(Throwable t) {
      downstreamSubscriber.onError(t);
    }

    @Override
    public void onComplete() {
      downstreamSubscriber.onComplete();
    }
  }
}
