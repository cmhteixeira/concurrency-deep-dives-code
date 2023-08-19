Thank you for the prompt reply.

> It uses that logic because request may happen from a different thread than the emission of items are currently happening. That requires atomics or thread confinement. RangePublisher uses the former.

But the Publisher is synchronous, it will emit on the same thread as the `request` calls. Correct? And since all request
calls are serialized by rule `2.7`, wouldn't that mean we can do it without atomics (but still require `volatile` for
visibility)?

In my mind, the `Susbcription`'s calls must follow the pattern:

```
Thread-1 -----| request() |----------------------------------------------------------
Thread-2 ---------------------| request() |------------------------------------------
Thread-3 -------------------------------------| request() |--------------------------
```
Within a particular call to `request`, the subscription calls `onNext`, which in turn might call `request` again.
If it does so on the same thread, then we are fine. If it calls it asynchronously, then rule `2.7` must be uphold.
Is my understanding mistaken?

On the current `RangePublisher` we see :

```java
@Override
public void request(long n) {
  // trimmed
  // Downstream requests are cumulative and may come from any thread
  for (; ; ) {
    long requested = get();
    long update = requested + n;
    // As governed by rule 3.17, when demand overflows `Long.MAX_VALUE`
    // we treat the signalled demand as "effectively unbounded"
    if (update < 0L) {
      update = Long.MAX_VALUE;
    }
    // atomically update the current requested amount
    if (compareAndSet(requested, update)) {
      // if there was no prior request amount, we start the emission loop
      if (requested == 0L) {
        emit(update);
      }
      break;
    }
  }
}
```

For example, between the `long requested = get()` and the `compareAndSet(requested, update)`  we can be 100% sure that no other threads could have ever run concurrently. No?

To illustrate my case further, I have set up this other implementation as a [gist](https://gist.github.com/cmhteixeira/a10fb34dd5e3318de223669beb696b49):

- No TCK tests failing (27 passed, 11 ignored).
- It's not  "water tight", but should be sufficient to illustrate my point.

It would be interesting to hear you opinion. 