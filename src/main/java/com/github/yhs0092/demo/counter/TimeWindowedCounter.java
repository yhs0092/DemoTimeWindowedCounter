package com.github.yhs0092.demo.counter;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class TimeWindowedCounter {

  private final Object SLIP_LOCK = new Object();

  int[] timeWindows;

  AtomicInteger currentCounter;

  int currentWindowIndex;

  int total;

  /**
   * Be careful, it is invoked in synchronized block.
   */
  Consumer<TimeWindowedCounter> slipHook;

  public TimeWindowedCounter(int windowsSize) {
    timeWindows = new int[windowsSize];
    currentCounter = new AtomicInteger(0);
  }

  public void count() {
    currentCounter.incrementAndGet();
  }

  public void slipWindow() {
    int currentWindowValue = currentCounter.getAndSet(0);

    synchronized (SLIP_LOCK) {
      total -= timeWindows[currentWindowIndex];
      timeWindows[currentWindowIndex] = currentWindowValue;
      currentWindowIndex = ++currentWindowIndex % timeWindows.length;
      total += currentWindowValue;
      callBackHook();
    }
  }

  public int getTotal() {
    return total;
  }

  private void callBackHook() {
    if (null != slipHook) {
      slipHook.accept(this);
    }
  }
}
