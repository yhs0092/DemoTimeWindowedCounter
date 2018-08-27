package com.github.yhs0092.demo.counter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

public class TimeWindowedCounterConcurrentTest {

  @Test
  public void testConcurrentCountWithoutSlip() throws InterruptedException, ExecutionException {
    final int threadCount = 8;
    final int threadTaskCount = 1000_0000;
    final int windowsSize = 3;
    TimeWindowedCounter timeWindowedCounter = new TimeWindowedCounter(windowsSize);

    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    ArrayList<Callable<Boolean>> tasks = new ArrayList<>();
    for (int i = 0; i < threadCount; ++i) {
      tasks.add(() -> {
        for (int j = 0; j < threadTaskCount; ++j) {
          timeWindowedCounter.count();
        }
        return true;
      });
    }
    List<Future<Boolean>> futures = executor.invokeAll(tasks);

    boolean taskCompleted = true;
    for (Future<Boolean> future : futures) {
      taskCompleted &= future.get();
    }

    Assert.assertTrue(taskCompleted);
    Assert.assertEquals(threadCount * threadTaskCount, timeWindowedCounter.currentCounter.get());
  }

  @Test
  public void testConcurrentCountWithSlip() throws InterruptedException, ExecutionException {
    final int threadCount = 8;
    final int threadTaskCount = 1000_0000;
    final int windowsSize = 10;
    TimeWindowedCounter timeWindowedCounter = new TimeWindowedCounter(windowsSize);

    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    ArrayList<Callable<Boolean>> tasks = new ArrayList<>();
    for (int i = 0; i < threadCount - 1; ++i) {
      tasks.add(() -> {
        for (int j = 0; j < threadTaskCount; ++j) {
          timeWindowedCounter.count();
        }
        return true;
      });
    }
    tasks.add(() -> {
      // this task will slip window
      int cursor = 1;
      for (int j = 0; j < threadTaskCount; ++j) {
        timeWindowedCounter.count();

        if (j > (threadTaskCount / windowsSize) * cursor) {
          timeWindowedCounter.slipWindow();
          ++cursor;
        }
      }
      return true;
    });
    List<Future<Boolean>> futures = executor.invokeAll(tasks);

    boolean taskCompleted = true;
    for (Future<Boolean> future : futures) {
      taskCompleted &= future.get();
    }

    Assert.assertTrue(taskCompleted);
    Assert.assertEquals(threadCount * threadTaskCount,
        timeWindowedCounter.getTotal() + timeWindowedCounter.currentCounter.get());
  }

  @Test
  public void testConcurrentCountWithSlipOverwrite() throws InterruptedException, ExecutionException {
    final int threadCount = 8;
    final int threadTaskCount = 1000_0000;
    final int windowsSize = 10;
    TimeWindowedCounter timeWindowedCounter = new TimeWindowedCounter(windowsSize);

    // mount callback hook to analyse counter's state
    List<Integer> totalCountRecorder = new ArrayList<>();
    timeWindowedCounter.slipHook = counter -> totalCountRecorder.add(counter.getTotal());

    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    ArrayList<Callable<Boolean>> tasks = new ArrayList<>();
    for (int i = 0; i < threadCount - 1; ++i) {
      tasks.add(() -> {
        for (int j = 0; j < threadTaskCount; ++j) {
          timeWindowedCounter.count();
        }
        return true;
      });
    }
    tasks.add(() -> {
      // this task will slip window
      int cursor = 1;
      for (int j = 0; j < threadTaskCount; ++j) {
        timeWindowedCounter.count();

        if (j > (threadTaskCount / 2 / windowsSize) * cursor) {
          timeWindowedCounter.slipWindow();
          ++cursor;
        }
      }
      return true;
    });
    List<Future<Boolean>> futures = executor.invokeAll(tasks);

    boolean taskCompleted = true;
    for (Future<Boolean> future : futures) {
      taskCompleted &= future.get();
    }

    Assert.assertTrue(taskCompleted);

    // analyse counter state
    int[] windows = new int[windowsSize];
    List<Integer> actualWindowCount = new ArrayList<>();
    int windowCursor = 0;
    int previousCount = 0;
    for (Integer count : totalCountRecorder) {
      int currentWindowValue = count - previousCount + windows[windowCursor];
      actualWindowCount.add(currentWindowValue);
      windows[windowCursor] = currentWindowValue;
      windowCursor = ++windowCursor % windowsSize;
      previousCount = count;
    }

    AtomicInteger total = new AtomicInteger();
    actualWindowCount.forEach(total::addAndGet);
    Assert.assertEquals(threadCount * threadTaskCount,
        total.get() + timeWindowedCounter.currentCounter.get());
  }
}
