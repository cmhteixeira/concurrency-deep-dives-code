package com.cmhteixeira.streams.subscribers;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class CountSubscriber<T> implements Subscriber<T> {
  private volatile Subscription s;
  private long counter = 0;
  private boolean completed = false;

  private AtomicBoolean alreadySubscribed = new AtomicBoolean(false);

  private final CompletableFuture<Long> cf = new CompletableFuture<>();

  @Override
  public void onSubscribe(Subscription s) {
    if (s == null) throw null;
    if (!alreadySubscribed.compareAndSet(false, true)) {
      s.cancel();
    }

    System.out.println("Onsubscribe. Current thread: " + Thread.currentThread().getName());
    this.s = s;
    s.request(10);
  }

  @Override
  public void onNext(T t) {
    if (t == null) throw null;
    System.out.printf("A-OnNext: %s\n", t);
    ++counter;
    //    s.request(4);
    System.out.printf("B-OnNext: %s\n", t);
  }

  @Override
  public void onError(Throwable t) {
    if (t == null) throw null;
    System.out.println("onError. Current thread: " + Thread.currentThread().getName());
    completed = true;
    cf.complete(counter);
  }

  @Override
  public void onComplete() {
    System.out.println("onComplete. Current thread: " + Thread.currentThread().getName());
    completed = true;
    cf.complete(counter);
  }

  public CompletableFuture<Long> counter() {
    return cf;
  }
}
