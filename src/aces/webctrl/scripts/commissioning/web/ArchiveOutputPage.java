package aces.webctrl.scripts.commissioning.web;
import aces.webctrl.scripts.commissioning.core.*;
import javax.servlet.http.*;
public class ArchiveOutputPage extends ServletBase {
  @Override public void exec(final HttpServletRequest req, final HttpServletResponse res) throws Throwable {
    final String id = req.getParameter("ID");
    if (id==null){
      res.sendError(400, "HTTP request parameter \"ID\" is missing.");
    }else{
      final ArchivedTest at = ArchivedTest.instances.get(Integer.parseInt(id));
      if (at==null){
        res.sendError(404, "Requested archived test does not exist.");
      }else{
        res.setContentType("text/html");
        res.getWriter().print(ExpansionUtils.expandLinks(at.load(), req));
      }
    }
  }
}