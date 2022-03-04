package com.cs6650.server2.servlets;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import com.google.gson.Gson;
import com.cs6650.server2.models.LiftRide;
import com.cs6650.server2.models.ResponseMessage;

import javax.servlet.*;
import javax.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

public class SkiersServlet extends HttpServlet {

  private static Gson gson = new Gson();
  private static ConnectionFactory factory;

  enum RequestURLType { VERTICAL, VERTICAL_BY_SEASON, INVALID_URL }
  private final static String QUEUE_NAME = "liftride";

  public SkiersServlet() {
    factory = new ConnectionFactory();
    factory.setHost("localhost");
    factory.setUsername("user");
    factory.setPassword("123");
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
        LiftRide liftRide = gson.fromJson(request.getReader(), LiftRide.class);
        int skierID = Integer.parseInt(urlPath.substring(urlPath.lastIndexOf('/') + 1));
        liftRide.setSkierID(skierID);
        Connection connection = this.factory.newConnection();
        Channel channel = connection.createChannel();
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        channel.basicPublish("", QUEUE_NAME, null, gson.toJson(liftRide).getBytes(StandardCharsets.UTF_8));
        channel.close();

        response.setStatus(HttpServletResponse.SC_OK);
        out.print(gson.toJson(new ResponseMessage("Lift ride correctly registered.")));
      } catch (TimeoutException | ConnectException e) {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        out.print(gson.toJson(new ResponseMessage("Server error. Connection timed out or refused.")));
      } catch (IOException e) {
        System.out.println(e);
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        out.print(gson.toJson(new ResponseMessage("Invalid input. Unprocessable entity.")));
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

  public static RequestURLType getRequestURLType(String urlPath) {
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
}
