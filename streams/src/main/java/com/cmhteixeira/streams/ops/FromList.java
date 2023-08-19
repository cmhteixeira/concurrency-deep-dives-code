package com.cmhteixeira.streams.ops;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

class FromList<T> extends CmhPublisher<T> {

  Set<Subscriber<? super T>> subscribers = Collections.newSetFromMap(new ConcurrentHashMap<>());
  private final List<T> listSource;

  FromList(List<T> source) {
    this.listSource = List.copyOf(source);
  }

  @Override
  public synchronized void subscribe(Subscriber<? super T> subscriber) {
    if (subscriber == null) throw new NullPointerException("Subscriber cannot be null.");
    if (subscribers.contains(subscriber)) {
      subscriber.onSubscribe(new DummySubscription());
      subscriber.onError(new RuntimeException("You cannot subscribe more than once."));
      return;
    }
    subscribers.add(subscriber);
    subscriber.onSubscribe(new FromList.FromIterableSubscription(subscriber));
  }

  class FromIterableSubscription implements Subscription {
    Subscriber<T> subscriber;
    int signalled = 0;
    int demandUnsign = 0;

    FromIterableSubscription(Subscriber<T> subscriber) {
      this.subscriber = subscriber;
    }

    @Override
    public void request(long l) {
      if (!subscribers.contains(subscriber)) return;
      if (l <= 0) {
        subscriber.onError(
            new IllegalArgumentException(
                "Non positive request signals are illegal. Requested: " + l));
        return;
      }
      if (signalled == listSource.size()) return;
      int castedL = (int) Math.min(l, listSource.size());
      if (demandUnsign == 0) {
        demandUnsign = demandUnsign + castedL;
        long upTo = signalled + castedL;
        signal(upTo);
      } else {
        demandUnsign = demandUnsign + castedL;
      }
    }

    private void signal(long upTo) {
      for (long i = signalled; i < upTo; ++i) {
        T elem;
        try {
          elem = listSource.get((int) i);
        } catch (IndexOutOfBoundsException e) {
          subscriber.onComplete();
          return;
        }
        subscriber.onNext(elem);
        ++signalled;
        --demandUnsign;
      }
      if (signalled == listSource.size()) {
        subscriber.onComplete();
        return;
      }
      if (demandUnsign > 0) signal(signalled + demandUnsign);
    }

    @Override
    public void cancel() {
      subscribers.remove(subscriber);
    }
  }
}
