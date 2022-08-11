package aces.webctrl.scripts.commissioning.web;
import aces.webctrl.scripts.commissioning.core.*;
import javax.servlet.http.*;
public class ArchiveOutputPage extends ServletBase {
  @Override public void exec(final HttpServletRequest req, final HttpServletResponse res) throws Throwable {
    final String id = req.getParameter("ID");
    if (id==null){
      String err = "HTTP request parameter \"ID\" is missing.";
      res.sendError(400, err);
      Initializer.log(new NullPointerException(err));
    }else{
      final ArchivedTest at = ArchivedTest.instances.get(Integer.parseInt(id));
      if (at==null){
        String err = "Requested archived test does not exist.";
        res.sendError(404, err);
        Initializer.log(new NullPointerException(err));
      }else if (req.getParameter("AJAX")==null){
        res.setContentType("text/html");
        res.getWriter().print(ExpansionUtils.expandLinks(at.load(), req));
      }else{
        String err = "Cannot submit AJAX request to archived tests.";
        res.sendError(404, err);
        Initializer.log(new NullPointerException(err));
      }
    }
  }
}