package com.cs6650.server3.servlets;

import com.cs6650.server3.models.LiftRide;
import com.cs6650.server3.models.ResponseMessage;
import com.cs6650.server3.utilities.RabbitMQChannelFactory;
import com.cs6650.server3.utilities.RabbitMQUtil;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.Channel;
import org.apache.commons.pool2.impl.GenericObjectPool;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class SkiersServlet extends HttpServlet {
  private static final Gson gson = new Gson();
  static Logger log = Logger.getLogger(SkiersServlet.class.getName());
  private final String EXCHANGE_NAME = System.getenv("RABBITMQ_EXCHANGE_NAME");
  private RabbitMQUtil rabbitMQUtil = null;

  private static RequestURLType getRequestURLType(String urlPath) {
    Pattern isValidVertical = Pattern
        .compile("^/\\d+/seasons/[0-9]{4}/days/([1-9]|[1-9][0-9]|[1-2][0-9][0-9]|3[0-5][0-9]|36[0-6])/skiers/\\d+$");
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

    if (urlPath == null || urlPath.isEmpty() || getRequestURLType(urlPath) == RequestURLType.INVALID_URL) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      out.print(gson.toJson(new ResponseMessage("Invalid input. Provide a valid URL.")));
    } else {
      RequestURLType urlType = getRequestURLType(urlPath);
      switch (urlType) {
        case VERTICAL:
          response.setStatus(HttpServletResponse.SC_OK);
          out.print(gson.toJson(new ResponseMessage("Total vertical retrieved.")));
          break;
        case VERTICAL_BY_SEASON:
          Map<String, String[]> parameters = request.getParameterMap();
          if (!parameters.containsKey("resort")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(new ResponseMessage("Invalid input. Missing resort parameter.")));
            break;
          }
          for (Map.Entry<String, String[]> param : parameters.entrySet()) {
            System.out.println(param.getKey());
            System.out.println(Arrays.toString(param.getValue()));
          }
          response.setStatus(HttpServletResponse.SC_OK);
          out.print(gson.toJson(new ResponseMessage("Skier verticals retrieved.")));
          break;
      }
    }

    out.flush();
  }

  enum RequestURLType {VERTICAL, VERTICAL_BY_SEASON, INVALID_URL}
}
