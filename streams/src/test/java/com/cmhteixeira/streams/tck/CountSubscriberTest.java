package com.cmhteixeira.streams.tck;

import com.cmhteixeira.streams.subscribers.CountSubscriber;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.SubscriberWhiteboxVerification;
import org.reactivestreams.tck.TestEnvironment;

public class CountSubscriberTest extends SubscriberWhiteboxVerification<Integer> {

  public CountSubscriberTest() {
    super(new TestEnvironment());
  }

  // The implementation under test is "SyncSubscriber":
  // class SyncSubscriber<T> extends Subscriber<T> { /* ... */ }

  @Override
  public Subscriber<Integer> createSubscriber(final WhiteboxSubscriberProbe<Integer> probe) {
    // in order to test the SyncSubscriber we must instrument it by extending it,
    // and calling the WhiteboxSubscriberProbe in all of the Subscribers methods:
    return new CountSubscriber<>() {
      @Override
      public void onSubscribe(final Subscription s) {
        super.onSubscribe(s);

        // register a successful Subscription, and create a Puppet,
        // for the WhiteboxVerification to be able to drive its tests:
        probe.registerOnSubscribe(
            new SubscriberPuppet() {

              @Override
              public void triggerRequest(long elements) {
                s.request(elements);
              }

              @Override
              public void signalCancel() {
                s.cancel();
              }
            });
      }

      @Override
      public void onNext(Integer element) {
        // in addition to normal Subscriber work that you're testing, register onNext with the probe
        super.onNext(element);
        probe.registerOnNext(element);
      }

      @Override
      public void onError(Throwable cause) {
        // in addition to normal Subscriber work that you're testing, register onError with the
        // probe
        super.onError(cause);
        probe.registerOnError(cause);
      }

      @Override
      public void onComplete() {
        // in addition to normal Subscriber work that you're testing, register onComplete with the
        // probe
        super.onComplete();
        probe.registerOnComplete();
      }
    };
  }

  @Override
  public Integer createElement(int element) {
    return element;
  }
}
