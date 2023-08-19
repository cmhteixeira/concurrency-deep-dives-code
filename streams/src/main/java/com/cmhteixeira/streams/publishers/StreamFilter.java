package com.cmhteixeira.streams.publishers;

import java.util.function.Predicate;
import org.reactivestreams.Subscriber;

public class StreamFilter<T> extends CmhPublisher<T> {

  private final Predicate<? super T> predicate;
  private final CmhPublisher<? super T> originalPublisher;

  StreamFilter(CmhPublisher<T> originalPublisher, Predicate<? super T> predicate) {
    this.predicate = predicate;
    this.originalPublisher = originalPublisher;
  }

  @Override
  public void subscribe(Subscriber<? super T> s) {}
}
