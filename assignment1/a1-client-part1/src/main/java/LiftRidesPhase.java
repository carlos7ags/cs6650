import org.apache.log4j.Logger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class LiftRidesPhase {
  static Logger log = Logger.getLogger(LiftRidesPhase.class.getName());

  private final ClientConfig clientConfig = new ClientConfig();
  private final AtomicInteger successfulCount;
  private final AtomicInteger unsuccessfulCount;

  private final int phase;
  private final ExecutorService executorService;
  private final int phaseThreads;
  private final double numRunsFactor;
  private final int startTime;
  private final int endTime;
  private final CountDownLatch nextPhaseLatch;

  public LiftRidesPhase(int phase, ExecutorService executorService, int phaseThreads, double numRunsFactor,
                        int startTime, int endTime, CountDownLatch nextPhaseLatch,
                        AtomicInteger successfulCount, AtomicInteger unsuccessfulCount) {
    this.phase = phase;
    this.executorService = executorService;
    this.phaseThreads = phaseThreads;
    this.numRunsFactor = numRunsFactor;
    this.startTime = startTime;
    this.endTime = endTime;
    this.nextPhaseLatch = nextPhaseLatch;
    this.successfulCount = successfulCount;
    this.unsuccessfulCount = unsuccessfulCount;
  }

  public void executePhase() {
    int numSkiers = clientConfig.getNumSkiers();
    int numRuns = clientConfig.getNumRuns();
    int skiLiftsNumber = clientConfig.getNumLifts();
    String serverBasePath = clientConfig.getServer();

    int skiersPerThread = numSkiers / phaseThreads;
    int requestsNumber = (int) ((numRuns * numRunsFactor) * skiersPerThread);

    for (int i = 0; i < phaseThreads; i++) {
      int startSkierID = (i * skiersPerThread) + 1;
      int endSkierID = (i + 1) * skiersPerThread;
      log.trace("Initializing thread in phase " + phase + " - Requests:" + requestsNumber + ", startSkierID:" + startSkierID
          + ", endSkierID:" + endSkierID + ", startTime:" + startTime + ", endTime:" + endTime);
      Runnable thread = new LiftRidesThread(requestsNumber, skiLiftsNumber, startSkierID, endSkierID, startTime, endTime,
          serverBasePath, nextPhaseLatch, successfulCount, unsuccessfulCount);
      executorService.execute(thread);
    }
  }
}
