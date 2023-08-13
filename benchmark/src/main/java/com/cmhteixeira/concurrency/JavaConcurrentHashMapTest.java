package com.cmhteixeira.concurrency;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;

@Threads(-1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
// @Warmup(iterations = 25, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(
    value = 1,
    jvmArgs = {"-Xms15G"})
public class JavaConcurrentHashMapTest {

  @State(Scope.Benchmark)
  public static class BenchmarkState {
    ConcurrentHashMap<String, Integer> map;

    @Setup(Level.Iteration)
    public void setup() {
      map = new ConcurrentHashMap<>();
    }
  }

  @Benchmark
  //  @Measurement(iterations = 25, time = 2, timeUnit = TimeUnit.SECONDS)
  public void insert(BenchmarkState state) {
    IntStream s = ThreadLocalRandom.current().ints(20, 0, 256);
    int[] res = s.toArray();
    String key = new String(res, 0, res.length);
    state.map.put(key, 1);
  }

  public static void main(String[] args) throws RunnerException {
    Options opt =
        new OptionsBuilder()
            .include(com.cmhteixeira.concurrency.JavaConcurrentHashMapTest.class.getSimpleName())
            .verbosity(VerboseMode.EXTRA)
            .syncIterations(false)
            .forks(1)
            .build();

    new Runner(opt).run();
  }
}
