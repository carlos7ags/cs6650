import org.apache.log4j.Logger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.MAX_PRIORITY;

public class LiftRidesPhasesExecutor implements Runnable {
  static Logger log = Logger.getLogger(LiftRidesPhasesExecutor.class.getName());

  private final ClientConfig clientConfig = new ClientConfig();
  private AtomicInteger successfulCount = new AtomicInteger(0);
  private AtomicInteger unsuccessfulCount = new AtomicInteger(0);

  private void executePhase(ExecutorService executorService, int numThreadsFraction, double numRunsFactor, double progressToReleaseLatch,
                            int startTime, int endTime) throws InterruptedException {
    
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
      Runnable thread = new LiftRidesThread(requestsNumber, skiLiftsNumber, startSkierID, endSkierID, startTime, endTime, serverBasePath, nextPhaseLatch, successfulCount, unsuccessfulCount);
      executorService.execute(thread);
    }
    nextPhaseLatch.await();
  }

  @Override
  public void run() {

    long phasesExecutorStartTime = System.currentTimeMillis();
    int numThreads = clientConfig.getNumThreads();
    int numRuns = clientConfig.getNumRuns();
    ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

    this.executePhase(executorService, 4, 0.2, 0.2, 1, 90);
    this.executePhase(executorService, 1, 0.6, 0.2, 91, 360);
    this.executePhase(executorService, 5, 0.1, 0.0, 361, 420);
    executorService.shutdown();

    long totalRunTime = (System.currentTimeMillis() - phasesExecutorStartTime) / 1000;
    log.info("Successful requests: " + this.successfulCount.get());
    log.info("Unsuccessful requests: " + this.unsuccessfulCount.get());
    log.info("Total run time / wall time (seconds)): " + totalRunTime);
    log.info("Total throughput in requests per second: " + (numRuns / totalRunTime));
  }
}
