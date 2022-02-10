public class LiftRidesLoadsThreadsGen {


  public static void main(String[] args) {
    ClientConfig clientConfig = new ClientConfig();

    int numSkiers = clientConfig.getNumSkiers();
    int numThreads = clientConfig.getNumThreads();
    int numRuns = clientConfig.getNumRuns();
    int skiLiftsNumber = clientConfig.getNumLifts();

    for (int i = 0; i < (numThreads / 4); i++) {
      String serverBasePath = clientConfig.getServer();
      int requestsNumber = (int) ((numRuns * 0.2) * (numSkiers / (numThreads / 4)));
      int startSkierID = i * (numSkiers / (numThreads / 4)) + 1;
      int endSkierID = (i + 1) * (numSkiers / (numThreads / 4));
      int starTime = 1;
      int endTime = 90;
      Thread thread = new Thread(new LiftRidesThread(requestsNumber, skiLiftsNumber, startSkierID, endSkierID, starTime, endTime, serverBasePath, finishCount, successCount, failCount));
      thread.start();
    }
  }
}
