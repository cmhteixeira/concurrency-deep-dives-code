On the examples, could [RangePublisher](examples/src/main/java/org/reactivestreams/example/unicast/RangePublisher.java) rely on volatile variable rather than on `AtomicLong` ?

From the examples, the `RangePublisher`'s [Subscription](https://github.com/reactive-streams/reactive-streams-jvm/blob/master/examples/src/main/java/org/reactivestreams/example/unicast/RangePublisher.java#L61C2-L61C2) extends an AtomicLong. It does so because its methods can be run from different threads.  The following comment can be read:

>   We are using this `AtomicLong` to make sure that this `Subscription` doesn't run concurrently with itself, which would violate rule 1.3 among others (no concurrent notifications). The atomic transition from 0L to N > 0L will ensure this.

However, from rule [2.7](https://github.com/reactive-streams/reactive-streams-jvm/blob/master/README.md#term_serially:~:text=otherwise%20already%20completed.-,3,-onSubscribe%2C%20onNext)

> A Subscriber MUST ensure that all calls on its Subscription's request and cancel methods are performed [serially](https://github.com/reactive-streams/reactive-streams-jvm/blob/master/README.md#term_serially).

Would this then not mean that, regardless of how the subscriber behaves,  the subscription only needs to be concerned about publishing the index of that subscriber? In which case a volatile variable would suffice?

Provided I am right, using a less powerful form of synchronization is preferable by _the principle of least power_. More importantly, for people learning via these examples, usage of `volatile` would emphasize that the `subscriber` must comply with rule 2.7.

What do you think?
 