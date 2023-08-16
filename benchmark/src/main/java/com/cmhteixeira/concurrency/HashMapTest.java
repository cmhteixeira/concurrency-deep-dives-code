package com.cmhteixeira.concurrency;

import com.cmhteixeira.MyConcurrentHashMap;
import java.util.Map;
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

public class HashMapTest {

  @Threads(1)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public abstract static class AbstractBenchmark {
    @State(Scope.Benchmark)
    public static class BenchmarkState {
      Map<String, Integer> map;

      @Setup(Level.Iteration)
      public void setup() {
        map = new ConcurrentHashMap<>();
      }
    }

    @Benchmark
    public void insert(BenchmarkState state) {
      IntStream s = ThreadLocalRandom.current().ints(20, 0, 256);
      int[] res = s.toArray();
      String key = new String(res, 0, res.length);
      state.map.put(key, 1);
    }

    protected abstract Map<String, Integer> obtainMap();
  }

  public static class JavaMapTest extends AbstractBenchmark {

    @Override
    protected Map<String, Integer> obtainMap() {
      return new ConcurrentHashMap<>();
    }
  }

  public static class CmhMapTest extends AbstractBenchmark {

    @Override
    protected Map<String, Integer> obtainMap() {
      return new MyConcurrentHashMap<>();
    }
  }

  public static void main(String[] args) throws RunnerException {
    Options opt =
        new OptionsBuilder()
            .include(com.cmhteixeira.concurrency.HashMapTest.class.getSimpleName())
            .verbosity(VerboseMode.EXTRA)
            .syncIterations(false)
            .forks(1)
            .build();

    new Runner(opt).run();
  }
}
