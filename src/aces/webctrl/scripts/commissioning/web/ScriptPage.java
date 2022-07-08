package aces.webctrl.scripts.commissioning.web;
import aces.webctrl.scripts.commissioning.core.*;
import com.controlj.green.addonsupport.access.*;
import javax.servlet.http.*;
import java.util.*;
import java.nio.file.*;
import java.nio.channels.*;
public class ScriptPage extends ServletBase {
  @Override public void exec(final HttpServletRequest req, final HttpServletResponse res) throws Throwable {
    final String cmd = req.getParameter("cmd");
    if (cmd==null){
      final StringBuilder sb = new StringBuilder(1024);
      boolean first;
      for (Test t:Test.instances.values()){
        sb.append("add(").append(t.ID).append(",\"");
        sb.append(Utility.escapeJS(t.getName())).append("\",\"");
        sb.append(Utility.escapeJS(t.getDescription())).append("\",");
        sb.append(t.isRunning()).append(",\"");
        sb.append(Utility.escapeJS(t.getStatus())).append("\",{");
        first = true;
        Map<String,Boolean> lastParams = t.lastParams;
        for (String s:t.getParamNames()){
          if (first){
            first = false;
          }else{
            sb.append(',');
          }
          sb.append('"').append(Utility.escapeJS(s)).append("\":").append(lastParams!=null && lastParams.getOrDefault(s,false));
        }
        sb.append("});\n");
      }
      String html = getHTML(req).replace("//__SCRIPT__", sb.toString());
      sb.setLength(0);
      for (Mapping m:Mapping.instances.values()){
        sb.append("<option value=\"").append(m.ID).append("\">").append(Utility.escapeHTML(m.getName())).append("</option>\n");
      }
      html = html.replace("__MAPPING_LIST__", sb.toString());
      res.setContentType("text/html");
      res.getWriter().print(html);
    }else if (cmd.equals("upload")){
      final Part filePart = req.getPart("file");
      if (filePart==null){
        res.setStatus(400);
        return;
      }
      final String fileName = filePart.getSubmittedFileName();
      if (fileName==null){
        res.setStatus(400);
        return;
      }
      final Path script = Initializer.getScriptFolder().resolve(fileName);
      try(
        ReadableByteChannel in = Channels.newChannel(filePart.getInputStream());
        FileChannel out = FileChannel.open(script, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);        
      ){
        //TODO replace transferTo and transferFrom logic with some simple fucking byte buffers
      }
      //finish the upload response thing
      //{"id":0,"name":"a","desc":"b","active":false,"status":"c","params":{"option1":true, "option2":false}}
    }else{
      final String id = req.getParameter("id");
      if (id==null){
        res.sendError(400, "ID parameter must be specified for command: "+cmd);
      }else{
        final Test t = Test.instances.get(Integer.parseInt(id));
        if (t==null){
          res.sendError(404, "Cannot locate a script with the given ID.");
        }else{
          switch (cmd){
            case "start":{
              Mapping m = Mapping.instances.get(Integer.parseInt(req.getParameter("mapping")));
              int threadCount = Integer.parseInt(req.getParameter("threads"));
              double maxTests = Double.parseDouble(req.getParameter("maxTests"));
              String operator = DirectAccess.getDirectAccess().getUserSystemConnection(req).getOperator().getLoginName();
              TreeMap<String,Boolean> params = new TreeMap<String,Boolean>();
              for (String s:t.getParamNames()){
                params.put(s,false);
              }
              for (String s:Utility.decodeList(req.getParameter("params"))){
                params.put(s,true);
              }
              t.initiate(m,threadCount,maxTests,operator,Collections.unmodifiableMap(params),null);
              res.sendRedirect(req.getContextPath()+"/ScriptOutput?ID="+id);
              break;
            }
            case "stop":{
              t.kill();
              res.sendRedirect(req.getContextPath()+"/ScriptOutput?ID="+id);
              break;
            }
            case "download":{
              res.setContentType("application/octet-stream");
              res.setHeader("Content-Disposition","attachment;filename=\""+t.getName()+"\"");
              try(
                WritableByteChannel out = Channels.newChannel(res.getOutputStream());
                FileChannel in = FileChannel.open(t.getScriptFile(), StandardOpenOption.READ);
              ){
                //TODO - replace transferTo and transferFrom logic with some simple fucking byte buffers
                for (long i=0,j=in.size(),k;j>0;i+=k,j-=k){
                  k = in.transferTo(i,j,out);
                }
              }
              break;
            }
            case "delete":{
              t.delete();
              break;
            }
            default:{
              res.sendError(400, "Unrecognized command parameter.");
            }
          }
        }
      }
    }
  }
}