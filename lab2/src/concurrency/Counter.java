package cs6650.lab2.concurrency;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class Counter {
  static private int NUMTHREADS = 1000;
  private int count = 0;

  public Counter(int numthreads) {
    NUMTHREADS = numthreads;
  }

  public Counter() {
  }

  synchronized public void incrementTen() {
    for (int i = 0; i < 10; i++) {
      count++;
    }
  }

  public int getCount() {
    return this.count;
  }

  public static void main(String[] args) {
    final Counter counter = new Counter();
    LocalDateTime startTime = LocalDateTime.now();
    for (int i = 0; i < NUMTHREADS; i++) {
      Runnable thread = counter::incrementTen;
      new Thread(thread).start();
    }
    LocalDateTime endTime = LocalDateTime.now();
    System.out.println("For " + NUMTHREADS + " threads it took " + ChronoUnit.MILLIS.between(startTime, endTime) + " milliseconds");
  }
}
