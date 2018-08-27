package com.github.yhs0092.demo.counter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

public class TimeWindowedCounterPerformanceTest {

  public static final int THREAD_COUNT = 10;

  public static final int THREAD_TASK_COUNT = 10000_0000;

  private static final ExecutorService executorService = Executors.newCachedThreadPool();

  private static final ScheduledExecutorService refreshTimer = Executors.newScheduledThreadPool(1);

  public static final int WINDOWS_NUM = 60;

  public static final int TIMED_WINDOW_SIZE_MS = 1000;

  @Test
  public void test() throws ExecutionException, InterruptedException {
    for (int i = 0; i < 10; ++i) {
      controlTest();
      timeWindowedCounterTest();
    }
  }

  public void controlTest() throws InterruptedException, ExecutionException {
    AtomicInteger counter = new AtomicInteger();
    ArrayList<Callable<Boolean>> tasks = new ArrayList<>();
    for (int i = 0; i < THREAD_COUNT; ++i) {
      tasks.add(() -> {
        for (int j = 0; j < THREAD_TASK_COUNT; ++j) {
          counter.incrementAndGet();
        }
        return true;
      });
    }
    long start = System.currentTimeMillis();
    List<Future<Boolean>> futures = executorService.invokeAll(tasks);
    boolean result = true;
    for (Future<Boolean> future : futures) {
      result &= future.get();
    }
    long timeCost = System.currentTimeMillis() - start;

    Assert.assertTrue(result);
    System.out.println("controlTest: " + timeCost);
  }

  public void timeWindowedCounterTest() throws ExecutionException, InterruptedException {
    TimeWindowedCounter timeWindowedCounter = new TimeWindowedCounter(WINDOWS_NUM);
    registerRefreshTimer(timeWindowedCounter);

    ArrayList<Callable<Boolean>> tasks = new ArrayList<>();
    for (int i = 0; i < THREAD_COUNT; ++i) {
      tasks.add(() -> {
        for (int j = 0; j < THREAD_TASK_COUNT; ++j) {
          timeWindowedCounter.count();
        }
        return true;
      });
    }
    long start = System.currentTimeMillis();
    List<Future<Boolean>> futures = executorService.invokeAll(tasks);
    boolean result = true;
    for (Future<Boolean> future : futures) {
      result &= future.get();
    }
    long timeCost = System.currentTimeMillis() - start;

    Assert.assertTrue(result);
    System.out.println("timeWindowedCounterTest: " + timeCost);
  }

  private void registerRefreshTimer(TimeWindowedCounter timeWindowedCounter) {
    refreshTimer.scheduleAtFixedRate(timeWindowedCounter::slipWindow,
        TIMED_WINDOW_SIZE_MS, TIMED_WINDOW_SIZE_MS, TimeUnit.MILLISECONDS);
  }

  @AfterClass
  public static void tearDown() {
    executorService.shutdown();
    refreshTimer.shutdown();
  }
}
