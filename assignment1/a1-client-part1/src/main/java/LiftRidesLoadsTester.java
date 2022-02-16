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

  public void testLoad() {
    log.info(this.clientConfig.toString());
    long phasesExecutorStartTime = System.currentTimeMillis();
    int numThreads = clientConfig.getNumThreads();
    ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

    try {
      double progressToReleaseLatch = 0.2;

      int phaseOneNumThreads = numThreads / 4;
      CountDownLatch phaseOneLatch = new CountDownLatch((int) Math.ceil(phaseOneNumThreads * progressToReleaseLatch));
      LiftRidesPhase phaseOneExecutor = new LiftRidesPhase(1, executorService, phaseOneNumThreads,
          0.2, 1, 90, phaseOneLatch, successfulCount, unsuccessfulCount);
      phaseOneExecutor.executePhase();
      phaseOneLatch.await();

      CountDownLatch phaseTwoLatch = new CountDownLatch((int) Math.ceil(numThreads * progressToReleaseLatch));
      LiftRidesPhase phaseTwoExecutor = new LiftRidesPhase(2, executorService, numThreads,
          0.6, 91, 360, phaseTwoLatch, successfulCount, unsuccessfulCount);
      phaseTwoExecutor.executePhase();
      phaseTwoLatch.await();

      int phaseThreeNumThreads = numThreads / 10;
      CountDownLatch phaseThreeLatch = new CountDownLatch(phaseThreeNumThreads);
      LiftRidesPhase phaseThreeExecutor = new LiftRidesPhase(3, executorService, phaseThreeNumThreads,
          0.1, 361, 420, phaseThreeLatch, successfulCount, unsuccessfulCount);
      phaseThreeExecutor.executePhase();
      phaseThreeLatch.await();

    } catch (InterruptedException e) {
      log.error(e);
    }
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
