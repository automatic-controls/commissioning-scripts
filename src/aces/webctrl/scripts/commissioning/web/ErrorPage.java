package aces.webctrl.scripts.commissioning.web;
import aces.webctrl.scripts.commissioning.core.*;
import javax.servlet.http.*;
public class ErrorPage extends ServletBase {
  @Override public void exec(HttpServletRequest req, HttpServletResponse res) throws Throwable {
    res.setContentType("text/html");
    res.getWriter().print(getHTML(req).replace("__LOG__", Utility.escapeHTML(Initializer.getErrors())));
  }
}