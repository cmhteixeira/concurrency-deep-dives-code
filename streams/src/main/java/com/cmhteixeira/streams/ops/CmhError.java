package com.cmhteixeira.streams.ops;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

class CmhError<T> extends CmhPublisher<T> {

  private final Throwable error;

  CmhError(Throwable error) {
    this.error = error;
  }

  @Override
  public void subscribe(Subscriber s) {
    s.onSubscribe(
        new Subscription() {
          @Override
          public void request(long n) {}

          @Override
          public void cancel() {}
        });
    s.onError(error);
  }
}
