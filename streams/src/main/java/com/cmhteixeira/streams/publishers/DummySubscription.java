package com.cmhteixeira.streams.publishers;

import org.reactivestreams.Subscription;

class DummySubscription implements Subscription {
  @Override
  public void request(long n) {}

  @Override
  public void cancel() {}
}
