package com.github.yhs0092.demo.counter;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test single thread function.
 */
public class TimeWindowedCounterTest {

  @Test
  public void count() {
    TimeWindowedCounter timeWindowedCounter = new TimeWindowedCounter(3);

    Assert.assertEquals(0, timeWindowedCounter.currentCounter.get());
    timeWindowedCounter.count();
    Assert.assertEquals(1, timeWindowedCounter.currentCounter.get());
  }

  @Test
  public void slip() {
    TimeWindowedCounter timeWindowedCounter = new TimeWindowedCounter(3);

    int[] timeWindows = timeWindowedCounter.timeWindows;
    for (int window : timeWindows) {
      Assert.assertEquals(0, window);
    }

    counterAdd(timeWindowedCounter, 1);
    timeWindowedCounter.slipWindow();
    Assert.assertEquals(1, timeWindows[0]);
    Assert.assertEquals(0, timeWindows[1]);
    Assert.assertEquals(0, timeWindows[2]);

    counterAdd(timeWindowedCounter, 2);
    timeWindowedCounter.slipWindow();
    Assert.assertEquals(1, timeWindows[0]);
    Assert.assertEquals(2, timeWindows[1]);
    Assert.assertEquals(0, timeWindows[2]);

    counterAdd(timeWindowedCounter, 3);
    timeWindowedCounter.slipWindow();
    Assert.assertEquals(1, timeWindows[0]);
    Assert.assertEquals(2, timeWindows[1]);
    Assert.assertEquals(3, timeWindows[2]);

    counterAdd(timeWindowedCounter, 4);
    timeWindowedCounter.slipWindow();
    Assert.assertEquals(4, timeWindows[0]);
    Assert.assertEquals(2, timeWindows[1]);
    Assert.assertEquals(3, timeWindows[2]);
  }

  @Test
  public void getTotal() {
    TimeWindowedCounter timeWindowedCounter = new TimeWindowedCounter(3);

    counterAdd(timeWindowedCounter, 1);
    Assert.assertEquals(0, timeWindowedCounter.getTotal());
    timeWindowedCounter.slipWindow();
    Assert.assertEquals(1, timeWindowedCounter.getTotal());

    counterAdd(timeWindowedCounter, 2);
    Assert.assertEquals(1, timeWindowedCounter.getTotal());
    timeWindowedCounter.slipWindow();
    Assert.assertEquals(3, timeWindowedCounter.getTotal());

    counterAdd(timeWindowedCounter, 3);
    Assert.assertEquals(3, timeWindowedCounter.getTotal());
    timeWindowedCounter.slipWindow();
    Assert.assertEquals(6, timeWindowedCounter.getTotal());

    counterAdd(timeWindowedCounter, 4);
    Assert.assertEquals(6, timeWindowedCounter.getTotal());
    timeWindowedCounter.slipWindow();
    Assert.assertEquals(9, timeWindowedCounter.getTotal());
  }

  private void counterAdd(TimeWindowedCounter timeWindowedCounter, int count) {
    for (int i = 0; i < count; ++i) {
      timeWindowedCounter.count();
    }
  }
}