import org.apache.log4j.PropertyConfigurator;

public class LiftRidesLoadsTester {

  public static void main(String[] args) {
    LiftRidesPhasesExecutor phasesExecutor = new LiftRidesPhasesExecutor();
    phasesExecutor.run();
  }
}
