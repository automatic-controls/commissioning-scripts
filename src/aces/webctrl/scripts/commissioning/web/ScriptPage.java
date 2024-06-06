package aces.webctrl.scripts.commissioning.web;
import aces.webctrl.scripts.commissioning.core.*;
import com.controlj.green.addonsupport.access.*;
import org.apache.tomcat.util.http.parser.HttpParser;
import org.apache.tomcat.util.http.fileupload.ParameterParser;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.*;
import java.util.*;
import java.nio.*;
import java.nio.file.*;
import java.nio.channels.*;
@MultipartConfig
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
      if (filePart==null || filePart.getSize()>67108864){
        Initializer.log(filePart==null?new NullPointerException("File upload is missing."):new OutOfMemoryError("File upload is too large."));
        res.setStatus(400);
        return;
      }
      final String fileName = getSubmittedFileName(filePart);
      if (fileName==null){
        Initializer.log(new NullPointerException("Could not retrieve name of file upload."));
        res.setStatus(400);
        return;
      }
      final Path script = Initializer.getScriptFolder().resolve(fileName);
      Test newTest = null;
      if (Files.exists(script)){
        for (Test te:Test.instances.values()){
          if (Files.isSameFile(script,te.getScriptFile())){
            if (te.reserved){
              Initializer.log(new FileAlreadyExistsException("Cannot overwrite permanent script: "+fileName));
              res.setStatus(400);
              return;
            }
            newTest = te;
            break;
          }
        }
      }
      {
        ByteBuffer buf = ByteBuffer.allocate(8192);
        boolean go = true;
        try(
          ReadableByteChannel in = Channels.newChannel(filePart.getInputStream());
          FileChannel out = FileChannel.open(script, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);        
        ){
          do {
            do {
              go = in.read(buf)!=-1;
            } while (go && buf.hasRemaining());
            buf.flip();
            while (buf.hasRemaining()){
              out.write(buf);
            }
            buf.clear();
          } while (go);
        }
      }
      if (newTest==null){
        newTest = new Test(script);
      }else{
        if (!newTest.isRunning()){
          newTest.clearStatus();
        }
        try(
          Script scriptInstance = newTest.getScript();
        ){}catch(Throwable t){
          Initializer.log(t);
        }
      }
      StringBuilder sb = new StringBuilder(256);
      sb.append("{\"id\":").append(newTest.ID);
      sb.append(",\"name\":\"").append(Utility.escapeJSON(newTest.getName()));
      sb.append("\",\"desc\":\"").append(Utility.escapeJSON(newTest.getDescription()));
      sb.append("\",\"active\":").append(newTest.isRunning());
      sb.append(",\"status\":\"").append(Utility.escapeJSON(newTest.getStatus()));
      sb.append("\",\"params\":{");
      boolean first = true;
      Map<String,Boolean> lastParams = newTest.lastParams;
      for (String s:newTest.getParamNames()){
        if (first){
          first = false;
        }else{
          sb.append(',');
        }
        sb.append('"').append(Utility.escapeJSON(s)).append("\":").append(lastParams!=null && lastParams.getOrDefault(s,false));
      }
      sb.append("}}");
      res.setContentType("text/plain");
      res.getWriter().print(sb.toString());
    }else if (cmd.equals("getData")){
      final StringBuilder sb = new StringBuilder(1024);
      sb.append('[');
      boolean f = true;
      for (Test t:Test.instances.values()){
        if (f){
          f = false;
        }else{
          sb.append(',');
        }
        sb.append("{\"id\":").append(t.ID);
        sb.append(",\"name\":\"").append(Utility.escapeJSON(t.getName()));
        sb.append("\",\"desc\":\"").append(Utility.escapeJSON(t.getDescription()));
        sb.append("\",\"active\":").append(t.isRunning());
        sb.append(",\"status\":\"").append(Utility.escapeJSON(t.getStatus()));
        sb.append("\",\"params\":{");
        boolean first = true;
        Map<String,Boolean> lastParams = t.lastParams;
        for (String s:t.getParamNames()){
          if (first){
            first = false;
          }else{
            sb.append(',');
          }
          sb.append('"').append(Utility.escapeJSON(s)).append("\":").append(lastParams!=null && lastParams.getOrDefault(s,false));
        }
        sb.append("}}");
      }
      sb.append(']');
      res.setContentType("text/plain");
      res.getWriter().print(sb.toString());
    }else{
      final String id = req.getParameter("id");
      if (id==null){
        String err = "ID parameter must be specified for command: "+cmd;
        res.sendError(400, err);
        Initializer.log(new NullPointerException(err));
      }else{
        final Test t = Test.instances.get(Integer.parseInt(id));
        if (t==null){
          String err = "Cannot locate a script with the given ID.";
          res.sendError(404, err);
          Initializer.log(new NullPointerException(err));
        }else{
          switch (cmd){
            case "start":{
              Mapping m = Mapping.instances.get(Integer.parseInt(req.getParameter("mapping")));
              int threadCount = Integer.parseInt(req.getParameter("threads"));
              double maxTests = Double.parseDouble(req.getParameter("maxTests"))/100;
              String operator = DirectAccess.getDirectAccess().getUserSystemConnection(req).getOperator().getLoginName();
              TreeMap<String,Boolean> params = new TreeMap<String,Boolean>();
              for (String s:t.getParamNames()){
                params.put(s,false);
              }
              for (String s:Utility.decodeList(req.getParameter("params"))){
                params.put(s,true);
              }
              if (t.initiate(m,threadCount,maxTests,operator,Collections.unmodifiableMap(params),null)){
                res.sendRedirect(req.getContextPath()+"/ScriptOutput?ID="+id);
              }else{
                res.sendRedirect(req.getContextPath()+"/Scripts");
              }
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
              ByteBuffer buf = ByteBuffer.allocate(8192);
              boolean go = true;
              try(
                WritableByteChannel out = Channels.newChannel(res.getOutputStream());
                FileChannel in = FileChannel.open(t.getScriptFile(), StandardOpenOption.READ);
              ){
                do {
                  do {
                    go = in.read(buf)!=-1;
                  } while (go && buf.hasRemaining());
                  buf.flip();
                  while (buf.hasRemaining()){
                    out.write(buf);
                  }
                  buf.clear();
                } while (go);
              }
              break;
            }
            case "delete":{
              if (t.reserved){
                String err = "Cannot delete permanent script.";
                res.sendError(400, err);
                Initializer.log(new IllegalAccessError(err));
              }else if (!t.delete()){
                String err = "Script could not be deleted. Please try again in a few moments.";
                res.sendError(400, err);
                Initializer.log(new IllegalAccessError(err));
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
  private static String getSubmittedFileName(Part p) {
    String fileName = null;
    String cd = p.getHeader("Content-Disposition");
    if (cd != null) {
        String cdl = cd.toLowerCase(Locale.ENGLISH);
        if (cdl.startsWith("form-data") || cdl.startsWith("attachment")) {
            ParameterParser paramParser = new ParameterParser();
            paramParser.setLowerCaseNames(true);
            // Parameter parser can handle null input
            Map<String,String> params = paramParser.parse(cd, ';');
            if (params.containsKey("filename")) {
                fileName = params.get("filename");
                // The parser will remove surrounding '"' but will not
                // unquote any \x sequences.
                if (fileName != null) {
                    // RFC 6266. This is either a token or a quoted-string
                    if (fileName.indexOf('\\') > -1) {
                        // This is a quoted-string
                        fileName = HttpParser.unquote(fileName.trim());
                    } else {
                        // This is a token
                        fileName = fileName.trim();
                    }
                } else {
                    // Even if there is no value, the parameter is present,
                    // so we return an empty file name rather than no file
                    // name.
                    fileName = "";
                }
            }
        }
    }
    return fileName;
  }
}