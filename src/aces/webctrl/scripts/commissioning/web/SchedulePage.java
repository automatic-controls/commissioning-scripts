package aces.webctrl.scripts.commissioning.web;
import aces.webctrl.scripts.commissioning.core.*;
import javax.servlet.http.*;
public class SchedulePage extends ServletBase {
  @Override public void exec(final HttpServletRequest req, final HttpServletResponse res) throws Throwable {
    final String cmd = req.getParameter("cmd");
    if (cmd==null){
      final StringBuilder sb = new StringBuilder(1024);
      //TODO
      
      res.setContentType("text/html");
      res.getWriter().print(getHTML(req).replace("//__SCRIPT__",sb.toString()));
    }else{
      final String id = req.getParameter("ID");
      if (id==null){
        String err = "ID parameter must be specified for command: "+cmd;
        res.sendError(400, err);
        Initializer.log(new NullPointerException(err));
      }else{
        final ScheduledTest t = ScheduledTest.instances.get(Integer.parseInt(id));
        if (t==null){
          String err = "Cannot locate a script with the given ID.";
          res.sendError(404, err);
          Initializer.log(new NullPointerException(err));
        }else{
          switch (cmd){
            case "start":{
              //TODO

              break;
            }
            case "delete":{
              //TODO

              break;
            }
            default:{
              String err = "Unrecognized command parameter: "+cmd;
              res.sendError(400, err);
              Initializer.log(new IllegalArgumentException(err));
            }
          }
        }
      }
    }
  }
}