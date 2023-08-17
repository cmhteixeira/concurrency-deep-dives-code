package com.cmhteixeira.streams.ops;

import java.util.List;
import org.reactivestreams.Publisher;

public abstract class CmhPublisher<T> implements Publisher<T> {
  public static <T> CmhPublisher<T> fromList(List<T> elems) {
    return new FromList<T>(elems);
  }

  public static <T> CmhPublisher<T> error(Throwable error) {
    return new CmhError<>(error);
  }
}
