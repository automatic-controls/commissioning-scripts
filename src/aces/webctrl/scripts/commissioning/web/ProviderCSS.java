package aces.webctrl.scripts.commissioning.web;
import aces.webctrl.scripts.commissioning.core.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
public class ProviderCSS extends HttpServlet {
  private static volatile String css = null;
  public static String getCSS(){
    if (css==null){
      try{
        css = Utility.loadResourceAsString("aces/webctrl/scripts/commissioning/html/main.css");
      }catch(Throwable t){
        Initializer.log(t);
      }
    }
    return css==null?"":css;
  }
  @Override public void doGet(final HttpServletRequest req, final HttpServletResponse res) throws ServletException, IOException {
    doPost(req,res);
  }
  @Override public void doPost(final HttpServletRequest req, final HttpServletResponse res) throws ServletException, IOException {
    try{
      req.setCharacterEncoding("UTF-8");
      res.setCharacterEncoding("UTF-8");
      res.addHeader("Cache-Control", "private");
      res.setContentType("text/css");
      res.getWriter().print(getCSS());
    }catch(Throwable t){
      Initializer.log(t);
      res.setStatus(500);
    }
  }
}