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

      int phaseOneThreads = numThreads / 4;
      CountDownLatch phaseOneLatch = new CountDownLatch((int) Math.ceil(phaseOneThreads * progressToReleaseLatch));
      Runnable phaseOneExecutor = new LiftRidesPhase(1, executorService, phaseOneThreads,
          0.2, 1, 90, phaseOneLatch, successfulCount, unsuccessfulCount);
      executorService.execute(phaseOneExecutor);
      phaseOneLatch.await();

      CountDownLatch phaseTwoLatch = new CountDownLatch((int) Math.ceil(numThreads * progressToReleaseLatch));
      Runnable phaseTwoExecutor = new LiftRidesPhase(2, executorService, numThreads,
          0.6, 91, 360, phaseTwoLatch, successfulCount, unsuccessfulCount);
      executorService.execute(phaseTwoExecutor);
      phaseTwoLatch.await();

      int phaseThreeThreads = numThreads / 5;
      CountDownLatch phaseThreeLatch = new CountDownLatch(phaseThreeThreads);
      Runnable phaseThreeExecutor = new LiftRidesPhase(3, executorService, phaseThreeThreads,
          0.2, 361, 420, phaseThreeLatch, successfulCount, unsuccessfulCount);
      executorService.execute(phaseThreeExecutor);
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
