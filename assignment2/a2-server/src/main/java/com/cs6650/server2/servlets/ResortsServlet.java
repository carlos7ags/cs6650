package com.cs6650.server2.servlets;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.cs6650.server2.models.Resort;
import com.cs6650.server2.models.ResponseMessage;

import javax.servlet.*;
import javax.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ResortsServlet extends HttpServlet {

  private Gson gson = new Gson();
  enum RequestURLType { UNIQUE_SKIERS, SEASONS, INVALID_URL }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, JsonSyntaxException, IOException {
    response.setContentType("application/json");
    String urlPath = request.getPathInfo();
    PrintWriter out = response.getWriter();

    if (urlPath == null || urlPath.isEmpty() || getRequestURLType(urlPath) != ResortsServlet.RequestURLType.SEASONS) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      out.print(gson.toJson(new ResponseMessage("Invalid input. Provide a valid URL.")));
      out.flush();
      return;
    }

    Boolean hasBody = (request.getContentLength() > 0);
    if (!hasBody) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      out.print(gson.toJson(new ResponseMessage("Invalid input. No request body provided.")));
      out.flush();
      return;
    }

    String payload = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
    Type seasonMapType = new TypeToken<Map<String, String>>() {}.getType();
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
    if (newSeason == null || !Pattern.compile("[0-9]{4}").matcher(newSeason).matches()) {
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

    if (urlPath == null || urlPath.isEmpty()) {
      response.setStatus(HttpServletResponse.SC_OK);
      out.print(gson.toJson(new ResponseMessage("Seasons list retrieved.")));
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

    RequestURLType urlType = getRequestURLType(urlPath);
    switch (urlType) {
      case UNIQUE_SKIERS:
        response.setStatus(HttpServletResponse.SC_OK);
        out.print(gson.toJson(new ResponseMessage("Skiers list retrieved.")));
        break;
      case SEASONS:
        response.setStatus(HttpServletResponse.SC_OK);
        out.print(gson.toJson(new ResponseMessage("Seasons list retrieved.")));
        break;
    }
    out.flush();

  }

  public static ResortsServlet.RequestURLType getRequestURLType(String urlPath) {
    Pattern isValidSeasonsByResort = Pattern
            .compile("^/\\d+/seasons$");
    Pattern isValidUniqueSkiers = Pattern
            .compile("^/\\d+/seasons/[0-9]{4}/day/([1-9]|[1-9][0-9]|[1-2][0-9][0-9]|3[0-5][0-9]|36[0-6])/skiers$");

    if (isValidSeasonsByResort.matcher(urlPath).matches()) {
      return RequestURLType.SEASONS;
    } else if (isValidUniqueSkiers.matcher(urlPath).matches()) {
      return RequestURLType.UNIQUE_SKIERS;
    } else {
      return ResortsServlet.RequestURLType.INVALID_URL;
    }
  }
}
