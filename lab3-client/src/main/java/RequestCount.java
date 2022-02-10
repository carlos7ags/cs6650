import java.util.concurrent.CountDownLatch;

public class RequestCount {
  final static private int NUMTHREADS = 500;

  public static void main(String[] args) throws InterruptedException {
    final HttpClientAsync client = new HttpClientAsync();
    CountDownLatch completed = new CountDownLatch(NUMTHREADS);

    long initiall = System.currentTimeMillis();

    for (int i = 0; i < NUMTHREADS; i++) {
        Runnable thread = () -> {
            client.main(); completed.countDown();
        };
        new Thread(thread).start();
    }
    completed.await();
    long lasted = System.currentTimeMillis() - initiall;
    System.out.println("Lasted: " + (double)lasted/1000);
  }
}
