package org.sample;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Thread)
@Threads(1)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 50)
@Fork(
    value = 1,
    jvmArgs = {"-Xms15G"})
public class JavaPriorityQueue {

  int max = 99_999_999;
  int min = 1;

  @Param({"1000", "10000", "100000", "1000000", "3000000", "7000000", "10000000"})
  private int n;

  PriorityQueue<Integer> queue;

  @Setup(Level.Iteration)
  public void setup() {
    queue = new PriorityQueue<>(Comparator.naturalOrder());
    for (int j = 0; j < n; ++j) {
      int randomWithMathRandom = (int) ((Math.random() * (max - min)) + min);
      queue.offer(randomWithMathRandom);
    }
  }

  @Benchmark
  @BenchmarkMode({Mode.SingleShotTime})
  @Measurement(iterations = 100)
  public void deleteMin() {
    queue.remove();
  }

  @Benchmark
  @BenchmarkMode({Mode.SingleShotTime})
  @Measurement(iterations = 100)
  public void insert() {
    int randomWithMathRandom = (int) ((Math.random() * (max - min)) + min);
    queue.offer(randomWithMathRandom);
  }

  public static void main(String[] args) throws RunnerException {
    Options opt =
        new OptionsBuilder()
            .include(JavaPriorityQueue.class.getSimpleName())
            .forks(1)
            .threads(1)
            .build();

    new Runner(opt).run();
  }
}
