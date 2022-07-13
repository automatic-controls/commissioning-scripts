package aces.webctrl.scripts.commissioning.web;
import aces.webctrl.scripts.commissioning.core.*;
import com.controlj.green.addonsupport.access.*;
import javax.servlet.http.*;
import java.util.*;
import java.util.regex.*;
public class ScheduleEditPage extends ServletBase {
  @Override public void exec(final HttpServletRequest req, final HttpServletResponse res) throws Throwable {
    final String cmd = req.getParameter("cmd");
    if (cmd==null){
      String err = "A command must be specified.";
      res.sendError(400, err);
      Initializer.log(new NullPointerException(err));
    }else if (cmd.equals("new")){
      String html = getHTML(req)
        .replace("__SCHEDULE_ID__","-1")
        .replace("__MAX_TESTS__","0")
        .replace("__THREADS__","1")
        .replace("__CRON_EXPRESSION__","")
        .replace("__NEXT_EXECUTION__","None")
        .replace("__OPERATOR__",Utility.escapeHTML(DirectAccess.getDirectAccess().getUserSystemConnection(req).getOperator().getLoginName()))
        .replace("__HASH_VALIDATION__","N/A")
        .replace("__EMAIL_SUBJECT__","")
        .replace("__EMAIL_TO__","")
        .replace("__EMAIL_CC__","");
      final StringBuilder sb = new StringBuilder(2048);
      String s;
      sb.append("<option value=\"\" disabled selected hidden>--- None ---</option>\n");
      for (Test m:Test.instances.values()){
        s = m.getName();
        if (s.endsWith(".java")){
          s = s.substring(0,s.length()-5);
        }
        sb.append("<option value=\"").append(m.ID).append("\">").append(Utility.escapeHTML(s)).append("</option>\n");
      }
      html = html.replace("__SCRIPT_LIST__",sb.toString());
      sb.setLength(0);
      sb.append("<option value=\"\" disabled selected hidden>--- None ---</option>\n");
      for (Mapping m:Mapping.instances.values()){
        sb.append("<option value=\"").append(m.ID).append("\">").append(Utility.escapeHTML(m.getName())).append("</option>\n");
      }
      html = html.replace("__MAPPING_LIST__",sb.toString());
      res.setContentType("text/html");
      res.getWriter().print(html);
    }else{
      final String id = req.getParameter("ID");
      if (id==null){
        String err = "ID parameter must be specified for command: "+cmd;
        res.sendError(400, err);
        Initializer.log(new NullPointerException(err));
      }else{
        final int ID = Integer.parseInt(id);
        if (ID==-1 && cmd.equals("save")){
          final ScheduledTest newTest = new ScheduledTest();
          if (save(newTest, req, res)){
            ScheduledTest.instances.put(newTest.ID,newTest);
            res.sendRedirect(req.getContextPath()+"/ScheduleEditor?cmd=get&ID="+newTest.ID);
          }else{
            res.sendRedirect(req.getContextPath()+"/ScheduleEditor?cmd=new");
          }
        }else if (cmd.equals("params")){
          Test test = Test.instances.get(ID);
          if (test==null){
            String err = "Cannot locate a script with the given ID.";
            res.sendError(404, err);
            Initializer.log(new NullPointerException(err));
          }else{
            final ScheduledTest st = ScheduledTest.instances.get(Integer.parseInt(req.getParameter("SID")));
            final StringBuilder sb = new StringBuilder(1024);
            sb.append('{');
            boolean first = true;
            Map<String,Boolean> lastParams = st==null?test.lastParams:st.params;
            for (String s:test.getParamNames()){
              if (first){
                first = false;
              }else{
                sb.append(',');
              }
              sb.append('"').append(Utility.escapeJSON(s)).append("\":").append(lastParams!=null && lastParams.getOrDefault(s,false));
            }
            sb.append('}');
            res.setContentType("text/plain");
            res.getWriter().print(sb.toString());
          }
        }else{
          ScheduledTest s = ScheduledTest.instances.get(ID);
          if (s==null){
            String err = "Cannot locate a schedule with the given ID.";
            res.sendError(404, err);
            Initializer.log(new NullPointerException(err));
          }else{
            switch (cmd){
              case "save":{
                save(s, req, res);
                res.sendRedirect(req.getContextPath()+"/ScheduleEditor?cmd=get&ID="+id);
                break;
              }
              case "get":{
                String html = getHTML(req)
                  .replace("__SCHEDULE_ID__",id)
                  .replace("__MAX_TESTS__",String.valueOf(Math.round(s.getMaxTests()*100)))
                  .replace("__THREADS__",String.valueOf(s.getThreads()))
                  .replace("__CRON_EXPRESSION__",Utility.escapeHTML(s.getCronExpression()))
                  .replace("__NEXT_EXECUTION__",Utility.escapeHTML(s.getNextString()))
                  .replace("__OPERATOR__",Utility.escapeHTML(s.getOperator()))
                  .replace("__HASH_VALIDATION__",s.validateHashes()?"Success":"Failure")
                  .replace("__EMAIL_SUBJECT__",Utility.escapeHTML(s.getEmailSubject()));
                final StringBuilder sb = new StringBuilder(1024);
                for (String str:s.emails){
                  sb.append(str).append(';');
                }
                html = html.replace("__EMAIL_TO__",Utility.escapeHTML(sb.toString()));
                sb.setLength(0);
                for (String str:s.emailsCC){
                  sb.append(str).append(';');
                }
                html = html.replace("__EMAIL_CC__",Utility.escapeHTML(sb.toString()));
                sb.setLength(0);
                boolean blank = true;
                {
                  Test selectedTest = s.getScript();
                  String str;
                  for (Test m:Test.instances.values()){
                    str = m.getName();
                    if (str.endsWith(".java")){
                      str = str.substring(0,str.length()-5);
                    }
                    sb.append("<option value=\"").append(m.ID).append('"');
                    if (blank && m==selectedTest){
                      blank = false;
                      sb.append(" selected");
                    }
                    sb.append('>').append(Utility.escapeHTML(str)).append("</option>\n");
                  }
                }
                if (blank){
                  sb.append("<option value=\"\" disabled selected hidden>--- None ---</option>\n");
                }
                html = html.replace("__SCRIPT_LIST__",sb.toString());
                sb.setLength(0);
                blank = true;
                Mapping selectedMapping = s.getMapping();
                for (Mapping m:Mapping.instances.values()){
                  sb.append("<option value=\"").append(m.ID).append('"');
                  if (blank && m==selectedMapping){
                    blank = false;
                    sb.append(" selected");
                  }
                  sb.append('>').append(Utility.escapeHTML(m.getName())).append("</option>\n");
                }
                if (blank){
                  sb.append("<option value=\"\" disabled selected hidden>--- None ---</option>\n");
                }
                html = html.replace("__MAPPING_LIST__",sb.toString());
                res.setContentType("text/html");
                res.getWriter().print(html);
                break;
              }
              case "email":{
                if (!s.onComplete("<!DOCTYPE html><html lang=\"en\"><head><title>Test</title></head><body>This is a test.</body></html>")){
                  res.setStatus(500);
                }
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
  private final static Pattern emailDelimiter = Pattern.compile(";");
  private static boolean save(ScheduledTest t, HttpServletRequest req, HttpServletResponse res){
    try{
      final Test test = Test.instances.get(Integer.parseInt("script"));
      t.setMappingName(Mapping.instances.get(Integer.parseInt("mapping")).getName());
      t.setRelScriptPath(test.getScriptFile());
      t.setMaxTests(Double.parseDouble(req.getParameter("maxTests"))/100);
      t.setThreads(Integer.parseInt(req.getParameter("threads")));
      t.setCronExpression(req.getParameter("cron"));
      t.setEmailSubject(req.getParameter("emailSubject"));
      {
        String[] arr = emailDelimiter.split(req.getParameter("emailTo"));
        t.emails.clear();
        for (String s:arr){
          t.emails.add(s);
        }
        arr = emailDelimiter.split(req.getParameter("emailCc"));
        t.emailsCC.clear();
        for (String s:arr){
          t.emailsCC.add(s);
        }
      }
      t.params.clear();
      for (String s:Utility.decodeList(req.getParameter("params"))){
        t.params.put(s,Boolean.TRUE);
      }
      for (String s:test.getParamNames()){
        t.params.putIfAbsent(s,Boolean.FALSE);
      }
      t.setOperator(DirectAccess.getDirectAccess().getUserSystemConnection(req).getOperator().getLoginName());
      t.recomputeHashes();
      return true;
    }catch(Throwable e){
      Initializer.log(e);
      return false;
    }
  }
}