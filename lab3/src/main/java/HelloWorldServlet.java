import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(name = "HelloWorldServlet", value = "/hello")
public class HelloWorldServlet extends HttpServlet {


  private String msg;

  public void init() throws ServletException {
    msg = "Hello World";
  }

  @lombok.SneakyThrows
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    // Set response content type to text
    response.setContentType("text/html");

    // sleep for 1000ms. You can vary this value for different tests
    Thread.sleep(1000);

    // Send the response
    PrintWriter out = response.getWriter();
    out.println("<h1>" + msg + "</h1>");
  }

  public void destroy() {
    // nothing to do here
  }

}
