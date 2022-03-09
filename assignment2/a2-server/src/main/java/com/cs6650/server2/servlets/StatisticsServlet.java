package com.cs6650.server2.servlets;

import com.google.gson.Gson;
import com.cs6650.server2.models.ResponseMessage;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;

public class StatisticsServlet extends HttpServlet {

    private Gson gson = new Gson();

    @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    response.setContentType("application/json");
    String urlPath = request.getPathInfo();
    PrintWriter out = response.getWriter();

    if (urlPath == null) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      out.print(gson.toJson(new ResponseMessage("Invalid input. Provide a valid URL.")));
      out.flush();
      return;
    }

    response.setStatus(HttpServletResponse.SC_OK);
    out.print(gson.toJson(new ResponseMessage("Api performance stats retrieved.")));
    out.flush();
  }

}
