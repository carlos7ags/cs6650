package com.cs6650.server4.servlets;

import com.cs6650.server4.models.Resort;
import com.cs6650.server4.models.ResponseMessage;
import com.cs6650.server4.models.SkierVertical;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.support.ConnectionPoolSupport;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ResortsServlet extends HttpServlet {

  private final Gson gson = new Gson();
  private GenericObjectPool<StatefulRedisConnection<String, String>> redisConnectionPool = null;
  static Logger log = Logger.getLogger(SkiersServlet.class.getName());

  private static RequestURLType getRequestURLType(String urlPath) {
    Pattern isValidSeasonsByResort = Pattern
        .compile("^/\\d+/seasons$");
    Pattern isValidUniqueSkiers = Pattern
        .compile("^/\\d+/seasons/\\d{4}/day/([1-9]|[1-9]\\d|[1-2]\\d\\d|3[0-5]\\d|36[0-6])/skiers$");

    if (isValidSeasonsByResort.matcher(urlPath).matches()) {
      return RequestURLType.SEASONS;
    } else if (isValidUniqueSkiers.matcher(urlPath).matches()) {
      return RequestURLType.UNIQUE_SKIERS;
    } else {
      return RequestURLType.INVALID_URL;
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, JsonSyntaxException, IOException {
    response.setContentType("application/json");
    String urlPath = request.getPathInfo();
    PrintWriter out = response.getWriter();

    if (urlPath == null || urlPath.isEmpty() || getRequestURLType(urlPath) != RequestURLType.SEASONS) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      out.print(gson.toJson(new ResponseMessage("Invalid input. Provide a valid URL.")));
      out.flush();
      return;
    }

    boolean hasBody = (request.getContentLength() > 0);
    if (!hasBody) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      out.print(gson.toJson(new ResponseMessage("Invalid input. No request body provided.")));
      out.flush();
      return;
    }

    String payload = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
    Type seasonMapType = new TypeToken<Map<String, String>>() {
    }.getType();
    Map<String, String> payloadMap;
    try {
      payloadMap = gson.fromJson(payload, seasonMapType);
    } catch (JsonSyntaxException e) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      out.print(gson.toJson(new ResponseMessage("Invalid input. Unprocessable entity.")));
      out.flush();
      return;
    }
    String newSeason = payloadMap.get("year");
    if (newSeason == null || !Pattern.compile("\\d{4}").matcher(newSeason).matches()) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      out.print(gson.toJson(new ResponseMessage("Invalid input. Invalid properties.")));
      out.flush();
      return;
    }

    int resortID = Integer.parseInt(urlPath.split("/")[1]);
    Resort resort = new Resort(resortID, "default"); //ToDo: Find resort in DB
    if (resort == null) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      out.print(gson.toJson(new ResponseMessage("Invalid input. com.cs6650.server2.models.Resort not found.")));
      out.flush();
      return;
    }

    List<String> seasonsList = new ArrayList<>();
    //ToDo: Find seasons for current resort, and update if not in current list
    seasonsList.add(newSeason);
    //ToDo: Save updated seasons list
    response.setStatus(HttpServletResponse.SC_OK);
    out.print(gson.toJson(new ResponseMessage("Season correctly registered.")));
    out.flush();
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    response.setContentType("application/json");
    String urlPath = request.getPathInfo();
    PrintWriter out = response.getWriter();

    if (redisConnectionPool == null) {
      redisConnectionPool = createRedisConnectionPool(1, 512);
    }

    try {
      StatefulRedisConnection<String, String> redisConnection = redisConnectionPool.borrowObject();
      RedisCommands<String, String> command = redisConnection.sync();

      if (urlPath == null || urlPath.isEmpty()) {
        response.setStatus(HttpServletResponse.SC_OK);
        out.print(gson.toJson(new ResponseMessage("Seasons list retrieved.")));
        out.flush();
        return;
      }

    RequestURLType urlType = getRequestURLType(urlPath);
    List<String> urlParts = Arrays.asList(urlPath.split("/"));
    String resort = urlParts.get(1);
    switch (urlType) {
      case UNIQUE_SKIERS:
        String uniqueSkiers = command.get(getUniqueSkiersKey(urlPath));
        uniqueSkiers = Objects.isNull(uniqueSkiers) ? "0" : uniqueSkiers;
        HashMap<String, Object> skierVerticalsMap = new HashMap<>();
        skierVerticalsMap.put("resort", resort);
        skierVerticalsMap.put("numSkiers", Integer.parseInt(uniqueSkiers));
        response.setStatus(HttpServletResponse.SC_OK);
        out.print(gson.toJson(new ResponseMessage("Skiers list retrieved.")));
        break;
      case SEASONS:
        response.setStatus(HttpServletResponse.SC_OK);
        out.print(gson.toJson(new ResponseMessage("Seasons list retrieved.")));
        break;
      case INVALID_URL:
        out.print(gson.toJson(new ResponseMessage("Invalid input. Provide a valid URL.")));
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        break;
    }
      redisConnectionPool.returnObject(redisConnection);
    } catch (Exception e) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      out.print(gson.toJson(new ResponseMessage("Server error. Connection timed out or refused.")));
      log.info(e.toString());
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

  private String getUniqueSkiersKey(String urlPath) {
    List<String> urlParts = Arrays.asList(urlPath.split("/"));
    return String.join("-",
            urlParts.get(1),
            urlParts.get(3),
            urlParts.get(5),
            "visitedSkierID");
  }

  enum RequestURLType {UNIQUE_SKIERS, SEASONS, INVALID_URL}
}
