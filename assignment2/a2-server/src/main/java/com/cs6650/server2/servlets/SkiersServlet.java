package com.cs6650.server2.servlets;

import com.cs6650.server2.utilities.RabbitMQChannelFactory;
import com.cs6650.server2.utilities.RabbitMQUtil;
import com.google.gson.Gson;
import com.cs6650.server2.models.LiftRide;
import com.cs6650.server2.models.ResponseMessage;
import org.apache.commons.pool2.impl.GenericObjectPool;

import javax.servlet.*;
import javax.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;

public class SkiersServlet extends HttpServlet {

  private static Gson gson = new Gson();
  RabbitMQUtil rabbitMQUtil = new RabbitMQUtil(new GenericObjectPool<>(new RabbitMQChannelFactory()));
  enum RequestURLType { VERTICAL, VERTICAL_BY_SEASON, INVALID_URL }
  private final static String QUEUE_NAME = "liftride";

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
        String skierID = urlPath.substring(urlPath.lastIndexOf('/') + 1);
        rabbitMQUtil.publishRecord(QUEUE_NAME, skierID, liftRide);
        response.setStatus(HttpServletResponse.SC_OK);
        out.print(gson.toJson(new ResponseMessage("Lift ride correctly registered.")));
      } catch (ConnectException e) {
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
}
