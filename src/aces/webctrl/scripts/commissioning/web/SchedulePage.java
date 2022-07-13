package aces.webctrl.scripts.commissioning.web;
import aces.webctrl.scripts.commissioning.core.*;
import javax.servlet.http.*;
public class SchedulePage extends ServletBase {
  @Override public void exec(final HttpServletRequest req, final HttpServletResponse res) throws Throwable {
    final String cmd = req.getParameter("cmd");
    if (cmd==null){
      final StringBuilder sb = new StringBuilder(1024);
      for (ScheduledTest s:ScheduledTest.instances.values()){
        sb.append("add(").append(s.ID).append(",\"");
        sb.append(Utility.escapeJS(s.getRelScriptPath())).append("\",\"");
        sb.append(Utility.escapeJS(s.getMappingName())).append("\",\"");
        sb.append(Utility.escapeJS(s.getOperator())).append("\",\"");
        sb.append(s.getThreads()).append("\",\"");
        sb.append(Math.round(s.getMaxTests()*100)).append("%\",\"");
        sb.append(Utility.escapeJS(s.getCronExpression())).append("\",\"");
        sb.append(Utility.escapeJS(s.getNextString())).append("\",\"");
        sb.append(s.validateHashes()?"Success":"Failure").append("\");\n");
      }
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
              Test s = t.exec();
              if (s==null){
                res.sendRedirect(req.getContextPath()+"/ScheduledTests");
              }else{
                res.sendRedirect(req.getContextPath()+"/ScriptOutput?ID="+s.ID);
              }
              break;
            }
            case "delete":{
              ScheduledTest.instances.remove(t.ID);
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