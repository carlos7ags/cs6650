import lombok.Getter;
import lombok.Setter;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import org.apache.log4j.Logger;


class ClientConfig {
  static Logger log = Logger.getLogger(ClientConfig.class.getName());

  @Getter @Setter int dayLength;
  @Getter private int numThreads;
  @Getter private int numSkiers;
  @Getter private int numLifts;
  @Getter private int numRuns;
  @Getter @Setter private String server;


  public ClientConfig() {
    String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
    String clientConfigPath = rootPath + "app.properties";
    Properties clientProps = new Properties();
    try {
      clientProps.load(new FileInputStream(clientConfigPath));
    } catch (IOException e) {
      log.warn("Unable to read client properties file. Default parameters will be used.", e);
    }

    this.setDayLength(Integer.parseInt(getValueOrDefault(clientProps.getProperty("dayLength"), "420")));
    this.setNumThreads(Integer.parseInt(getValueOrDefault(clientProps.getProperty("numThreads"), "1024")));
    this.setNumSkiers(Integer.parseInt(getValueOrDefault(clientProps.getProperty("numSkiers"), "100000")));
    this.setNumLifts(Integer.parseInt(getValueOrDefault(clientProps.getProperty("numLifts"), "40")));
    this.setNumRuns(Integer.parseInt(getValueOrDefault(clientProps.getProperty("numRuns"), "10")));
    this.setServer(getValueOrDefault(clientProps.getProperty("server"), "http://localhost:8080"));
  }

  private static <T> T getValueOrDefault(T value, T defaultValue) {
    return value == null ? defaultValue : value;
  }

  public void setNumThreads(int numThreads) throws IllegalArgumentException {
    if (numThreads < 1 || numThreads > 1024) {
      log.error("Maximum number of threads must be between 1 and 1024");
      throw new IllegalArgumentException("Maximum number of threads must be between 1 and 1024");
    }
    this.numThreads = numThreads;
  }

  public void setNumSkiers(int numSkiers) throws IllegalArgumentException {
    if (numSkiers < 1 || numSkiers > 1000000) {
      log.error("Number of skiers must be between 1 and 100000");
      throw new IllegalArgumentException("Number of skiers must be between 1 and 100000");
    }
    this.numSkiers = numSkiers;
  }

  public void setNumLifts(int numLifts) throws IllegalArgumentException {
    if (numLifts < 5 || numLifts > 60) {
      log.error("Number of ski lifts must be between 5 and 60");
      throw new IllegalArgumentException("Number of ski lifts must be between 5 and 60");
    }
    this.numLifts = numLifts;
  }

  public void setNumRuns(int numRuns) throws IllegalArgumentException {
    if (numRuns < 1 || numRuns > 20) {
      log.error("Mean numbers of ski lifts each skier rides each day must be between 1 and 20");
      throw new IllegalArgumentException("Mean numbers of ski lifts each skier rides each day must be between 1 and 20");
    }
    this.numRuns = numRuns;
  }

  @Override
  public String toString() {
    return String.format("PARAMETERS: numThreads: %d, numSkiers: %d, numLifts: %d, numRuns: %d", numThreads, numSkiers, numLifts, numRuns);
  }
}
