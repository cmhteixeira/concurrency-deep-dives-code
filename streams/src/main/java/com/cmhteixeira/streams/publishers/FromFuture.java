package com.cmhteixeira.streams.publishers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

class FromFuture<T> implements Publisher<T> {

  private final Future<T> fut;
  private final ExecutorService executor;

  public FromFuture(Future<T> theFut) {
    this.fut = theFut;
    this.executor = null;
  }

  public FromFuture(Future<T> theFut, ExecutorService executor) {
    this.fut = theFut;
    this.executor = executor;
  }

  @Override
  public void subscribe(Subscriber<? super T> s) {
    s.onSubscribe(
        new Subscription() {
          private volatile boolean cancelled = false;

          @Override
          public void request(long n) {
            if (executor != null) {
              executor.submit(() -> requestInternal(n));
            } else requestInternal(n);
          }

          public void requestInternal(long n) {
            System.out.println(Thread.currentThread().getName());
            if (cancelled) return;
            if (n <= 0L) {
              s.onError(new IllegalArgumentException("Non positive signals are illegal: " + n));
              return;
            }
            T t;
            try {
              t = fut.get();
            } catch (Throwable error) {
              s.onError(error);
              return;
            }
            s.onNext(t);
            s.onComplete();
            cancelled = true;
          }

          @Override
          public void cancel() {
            cancelled = true;
          }
        });
  }
}
