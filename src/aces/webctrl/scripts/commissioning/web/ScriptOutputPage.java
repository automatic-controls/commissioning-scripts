package aces.webctrl.scripts.commissioning.web;
import aces.webctrl.scripts.commissioning.core.*;
import javax.servlet.http.*;
public class ScriptOutputPage extends ServletBase {
  @Override public void exec(final HttpServletRequest req, final HttpServletResponse res) throws Throwable {
    final String id = req.getParameter("ID");
    if (id==null){
      String err = "HTTP request parameter \"ID\" is missing.";
      res.sendError(400, err);
      Initializer.log(new NullPointerException(err));
    }else{
      final Test t = Test.instances.get(Integer.parseInt(id));
      if (t==null){
        String err = "Requested test does not exist.";
        res.sendError(404, err);
        Initializer.log(new NullPointerException(err));
      }else{
        String output = t.getOutput();
        if (output==null){
          String err = "Output cache is empty.";
          res.sendError(404, err);
          Initializer.log(new NullPointerException(err));
        }else{
          res.setContentType("text/html");
          res.getWriter().print(ExpansionUtils.expandLinks(output, req));
        }
      }
    }
  }
}