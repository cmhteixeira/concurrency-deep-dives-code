package com.cmhteixeira.concurrency;

import com.cmhteixeira.MyConcurrentHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Thread)
@Threads(1)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 25, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(
    value = 1,
    jvmArgs = {"-Xms15G"})
public class MyConcurrentHashMapTest {

  int max = 99_999_999;
  int min = 1;

  MyConcurrentHashMap<String, Integer> map;

  @Setup(Level.Iteration)
  public void setup() {
    map = new MyConcurrentHashMap<>();
  }

  @Benchmark
  @BenchmarkMode({Mode.Throughput})
  @Measurement(iterations = 25, time = 2, timeUnit = TimeUnit.SECONDS)
  public void insert() {
    int randomWithMathRandom = (int) ((Math.random() * (max - min)) + min);
    Map.Entry<String, Integer> entry =
        Map.entry(UUID.randomUUID().toString(), randomWithMathRandom);
    map.put(entry.getKey(), entry.getValue());
  }

  public static void main(String[] args) throws RunnerException {
    Options opt =
        new OptionsBuilder()
            .include(org.sample.MyBinaryHeap.class.getSimpleName())
            .forks(1)
            .threads(1)
            .build();

    new Runner(opt).run();
  }
}
