package com.cmhteixeira.streams.publishers;

import java.util.UUID;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

class Range extends CmhPublisher<Integer> {

  private final int inclusive;

  private final int exclusive;

  Range(int inclusive, int exclusive) {
    this.inclusive = inclusive;
    this.exclusive = exclusive;
  }

  @Override
  public void subscribe(Subscriber<? super Integer> s) {
    if (s == null) throw new NullPointerException("Subscriber cannot be null.");
    s.onSubscribe(new RangeSubscription(s, inclusive, exclusive));
  }

  static class RangeSubscription implements Subscription {

    Subscriber<? super Integer> s;

    private final int exclusive;

    private volatile boolean isCancelled = false;

    private volatile int index;
    private volatile long demandUnsign = 0L;

    public RangeSubscription(Subscriber<? super Integer> subscriber, int inclusive, int exclusive) {
      this.s = subscriber;
      this.index = inclusive;
      this.exclusive = exclusive;
    }

    @Override
    public void request(long n) {
      String uuid = UUID.randomUUID().toString().substring(0, 7);
      System.out.println("Request,Enter: " + uuid);
      if (isCancelled) return;
      if (n <= 0L) {
        s.onError(new IllegalArgumentException("Non positive signals are illegal: " + n));
        isCancelled = true;
        return;
      }
      int d = (int) Math.min(n, (exclusive - index));
      if (demandUnsign == 0) {
        demandUnsign = demandUnsign + d;
        emit(index + d);
      } else demandUnsign = demandUnsign + d;
      System.out.println("Request, Exit: " + uuid);
    }

    private void emit(long upTo) {
      for (int i = index; i < upTo; ++i) {
        s.onNext(i);
        ++index;
        --demandUnsign;
      }
      if (index == exclusive) {
        s.onComplete();
        isCancelled = true;
        return;
      }
      if (demandUnsign > 0) emit(index + demandUnsign);
    }

    @Override
    public void cancel() {
      isCancelled = true;
    }
  }
}
