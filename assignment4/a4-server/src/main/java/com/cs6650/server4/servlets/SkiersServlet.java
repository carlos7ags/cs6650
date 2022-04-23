package com.cs6650.server4.servlets;

import com.cs6650.server4.models.LiftRide;
import com.cs6650.server4.models.ResponseMessage;
import com.cs6650.server4.models.SkierVertical;
import com.cs6650.server4.utilities.RabbitMQChannelFactory;
import com.cs6650.server4.utilities.RabbitMQUtil;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.support.ConnectionPoolSupport;
import org.apache.commons.pool2.impl.GenericObjectPool;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SkiersServlet extends HttpServlet {
  private static final Gson gson = new Gson();
  static Logger log = Logger.getLogger(SkiersServlet.class.getName());
  private final String EXCHANGE_NAME = System.getenv("RABBITMQ_EXCHANGE_NAME");
  private RabbitMQUtil rabbitMQUtil = null;
  private GenericObjectPool<StatefulRedisConnection<String, String>> redisConnectionPool = null;

  private static RequestURLType getRequestURLType(String urlPath) {
    Pattern isValidVertical = Pattern
        .compile("^/\\d+/seasons/\\d{4}/days/([1-9]|[1-9]\\d|[1-2]\\d\\d|3[0-5]\\d|36[0-6])/skiers/\\d+$");
    Pattern isValidVerticalBySeason = Pattern
        .compile("^/\\d+/vertical$");

    if (isValidVertical.matcher(urlPath).matches()) {
      return RequestURLType.VERTICAL;
    } else if (isValidVerticalBySeason.matcher(urlPath).matches()) {
      return RequestURLType.VERTICAL_BY_SEASON;
    } else {
      return RequestURLType.INVALID_URL;
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    response.setContentType("application/json");
    String urlPath = request.getPathInfo();
    PrintWriter out = response.getWriter();

    if (urlPath == null || urlPath.isEmpty() || getRequestURLType(urlPath) != RequestURLType.VERTICAL) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      out.print(gson.toJson(new ResponseMessage("Invalid input. Provide a valid URL.")));
    } else {
      try {
        if (rabbitMQUtil == null) {
          rabbitMQUtil = new RabbitMQUtil(new GenericObjectPool<>(new RabbitMQChannelFactory()));
        }
        List<String> urlParts = Arrays.asList(urlPath.split("/"));
        Map<String, Object> messageHeaders = new HashMap<>();
        messageHeaders.put("resort", urlParts.get(1));
        messageHeaders.put("season", urlParts.get(3));
        messageHeaders.put("day", urlParts.get(5));
        messageHeaders.put("skierID", urlParts.get(7));
        LiftRide liftRide = gson.fromJson(request.getReader(), LiftRide.class);
        byte[] message = gson.toJson(liftRide).getBytes(StandardCharsets.UTF_8);
        rabbitMQUtil.publishRecord(EXCHANGE_NAME, messageHeaders, message);
        response.setStatus(HttpServletResponse.SC_OK);
        out.print(gson.toJson(new ResponseMessage("Lift ride correctly registered.")));
      } catch (JsonSyntaxException | JsonIOException e) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        out.print(gson.toJson(new ResponseMessage("Invalid input. Unprocessable entity.")));
        log.info(e.toString());
      } catch (Exception e) {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        out.print(gson.toJson(new ResponseMessage("Server error. Connection timed out or refused.")));
        log.info(e.toString());
      }
    }
    out.flush();
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    response.setContentType("application/json");
    String urlPath = request.getPathInfo();
    PrintWriter out = response.getWriter();

    RequestURLType urlType = getRequestURLType(urlPath);
    if (urlPath == null || urlPath.isEmpty() || urlType == RequestURLType.INVALID_URL) {
      out.print(gson.toJson(new ResponseMessage("Invalid input. Provide a valid URL.")));
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    } else {
      if (redisConnectionPool == null) {
        redisConnectionPool = createRedisConnectionPool(0, 512);
      }
      try {
        StatefulRedisConnection<String, String> redisConnection = redisConnectionPool.borrowObject();
        RedisCommands<String, String> command = redisConnection.sync();
        switch (urlType) {
          case VERTICAL:
            String vertical = command.get(getDayVerticalKey(urlPath));
            out.print(gson.toJson(vertical));
            response.setStatus(HttpServletResponse.SC_OK);
            break;
          case VERTICAL_BY_SEASON:
            Map<String, String[]> parameters = request.getParameterMap();
            String resort = parameters.get("resort")[0];
            if (!parameters.containsKey("resort")) {
              out.print(gson.toJson(new ResponseMessage("Invalid input. Missing resort parameter.")));
              response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            } else {
              List<String> seasons;
              if (!parameters.containsKey("season")) {
                seasons = new ArrayList<>(command.smembers(getSeasonsKey(urlPath, resort)));
              } else {
                seasons = new ArrayList<>();
                seasons.add(parameters.get("season")[0]);
              }
              List<String> keys = seasons.stream()
                      .map(s -> getTotalVerticalKey(urlPath, resort, s))
                      .collect(Collectors.toList());
              List<String> verticals = keys.stream()
                      .map(command::get)
                      .collect(Collectors.toList());
              verticals.replaceAll(v -> Objects.isNull(v) ? "0" : v);
              List<SkierVertical> skierVerticals = IntStream.range(0, seasons.size())
                      .filter(i -> Objects.nonNull(verticals.get(i)))
                      .mapToObj(i -> new SkierVertical(Integer.parseInt(seasons.get(i)), Integer.parseInt(verticals.get(i))))
                      .collect(Collectors.toList());
              HashMap<String, List<SkierVertical>> skierVerticalsMap = new HashMap<>();
              skierVerticalsMap.put("resorts", skierVerticals);
              out.print(gson.toJson(skierVerticalsMap));
              response.setStatus(HttpServletResponse.SC_OK);
            }
            break;
        }
        redisConnectionPool.returnObject(redisConnection);
      } catch (Exception e) {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        out.print(gson.toJson(new ResponseMessage("Server error. Connection timed out or refused.")));
        log.info(e.toString());
      }
    }
    out.flush();
  }

  private GenericObjectPool<StatefulRedisConnection<String, String>> createRedisConnectionPool(int redisDatabase, int numThreads) {
    String host = System.getenv("REDIS_HOST");
    int port = Integer.parseInt(System.getenv("REDIS_PORT"));
    RedisURI redisURI = RedisURI.create(host, port);
    redisURI.setDatabase(redisDatabase);
    RedisClient redisClient = RedisClient.create(redisURI);
    GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
    poolConfig.setMaxTotal(numThreads);
    return ConnectionPoolSupport.createGenericObjectPool(redisClient::connect, poolConfig);
  }

  private String getDayVerticalKey(String urlPath) {
    List<String> urlParts = Arrays.asList(urlPath.split("/"));
    return String.join("-",
            urlParts.get(1),
            urlParts.get(3),
            urlParts.get(5),
            urlParts.get(7),
            "dailyVertical");
  }

  private String getTotalVerticalKey(String urlPath, String resort, String season) {
    List<String> urlParts = Arrays.asList(urlPath.split("/"));
    return String.join("-",
            resort,
            season,
            urlParts.get(1),
            "totalVertical");
  }

  private String getSeasonsKey(String urlPath, String resort) {
    List<String> urlParts = Arrays.asList(urlPath.split("/"));
    return String.join("-",
            resort,
            urlParts.get(1),
            "seasons");
  }

  enum RequestURLType {VERTICAL, VERTICAL_BY_SEASON, INVALID_URL}
}
