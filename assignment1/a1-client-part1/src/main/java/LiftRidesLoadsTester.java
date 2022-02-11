import org.apache.log4j.Logger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class LiftRidesLoadsTester {
  static Logger log = Logger.getLogger(LiftRidesLoadsTester.class.getName());

  private final ClientConfig clientConfig = new ClientConfig();
  private final AtomicInteger successfulCount = new AtomicInteger(0);
  private final AtomicInteger unsuccessfulCount = new AtomicInteger(0);

  private void executePhase(int phase, ExecutorService executorService, int numThreadsFraction, double numRunsFactor, double progressToReleaseLatch,
                            int startTime, int endTime) {
    
    int numSkiers = clientConfig.getNumSkiers();
    int numThreads = clientConfig.getNumThreads();
    int numRuns = clientConfig.getNumRuns();
    int skiLiftsNumber = clientConfig.getNumLifts();
    String serverBasePath = clientConfig.getServer();

    int phaseThreads = numThreads / numThreadsFraction;
    int skiersPerThread = numSkiers / phaseThreads;
    int requestsNumber = (int) ((numRuns * numRunsFactor) * skiersPerThread);

    CountDownLatch nextPhaseLatch = new CountDownLatch((int) Math.ceil(phaseThreads * progressToReleaseLatch));

    for (int i = 0; i < phaseThreads; i++) {
      int startSkierID = (i * skiersPerThread) + 1;
      int endSkierID = (i + 1) * skiersPerThread;
      log.debug("Initializing thread in phase " + phase + " - Requests:" + requestsNumber + ", startSkierID:" + startSkierID
          + ", endSkierID:" + endSkierID + ", startTime:" + startTime + ", endTime:" + endTime);
      Runnable thread = new LiftRidesThread(requestsNumber, skiLiftsNumber, startSkierID, endSkierID, startTime, endTime,
          serverBasePath, nextPhaseLatch, successfulCount, unsuccessfulCount);
      executorService.execute(thread);
    }
    try {
      nextPhaseLatch.await();
    } catch (InterruptedException e) {
      log.error(e);
    }
  }

  public void testLoad() {
    log.info(this.clientConfig.toString());
    long phasesExecutorStartTime = System.currentTimeMillis();
    int numThreads = clientConfig.getNumThreads();
    ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

    this.executePhase(1, executorService, 4, 0.2, 0.2, 1, 90);
    this.executePhase(2, executorService, 1, 0.6, 0.2, 91, 360);
    this.executePhase(3, executorService, 5, 0.2, 0.0, 361, 420);
    executorService.shutdown();
    while (!executorService.isTerminated()) {
      try {
        executorService.awaitTermination(1, TimeUnit.HOURS);
      } catch (InterruptedException e) {
        log.error(e);
      }
    }

    long totalRunTime = (System.currentTimeMillis() - phasesExecutorStartTime);
    log.info("Successful requests: " + this.successfulCount.get());
    log.info("Unsuccessful requests: " + this.unsuccessfulCount.get());
    log.info("Total run time / wall time (milliseconds): " + totalRunTime);
    log.info("Total throughput in requests per second: " + ((this.successfulCount.get() + this.unsuccessfulCount.get()) / (totalRunTime / 1000)));
  }

  public static void main(String[] args) {
    LiftRidesLoadsTester phasesExecutor = new LiftRidesLoadsTester();
    phasesExecutor.testLoad();
  }
}
