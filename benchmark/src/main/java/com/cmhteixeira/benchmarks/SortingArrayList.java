package com.cmhteixeira.benchmarks;

import java.util.*;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Thread)
@Threads(1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 50)
@Fork(
    value = 1,
    jvmArgs = {"-Xms15G"})
public class SortingArrayList {

  int max = 99_999_999;
  int min = 1;

  @Param({"1", "10", "100", "1000", "10000", "100000", "1000000", "10000000"})
  private int n;

  List<Integer> queue;

  @Setup(Level.Iteration)
  public void setup() {
    queue = new ArrayList<>();
    for (int j = 0; j < n; ++j) {
      int randomWithMathRandom = (int) ((Math.random() * (max - min)) + min);
      queue.add(randomWithMathRandom);
    }
  }

  @Benchmark
  @BenchmarkMode({Mode.SingleShotTime})
  @Measurement(iterations = 100)
  public void javasQueue() {
    Collections.sort(queue);
    queue.get(0);
  }

  public static void main(String[] args) throws RunnerException {
    Options opt =
        new OptionsBuilder()
            .include(MyBinaryHeap.class.getSimpleName())
            .forks(1)
            .threads(1)
            .build();

    new Runner(opt).run();
  }
}
