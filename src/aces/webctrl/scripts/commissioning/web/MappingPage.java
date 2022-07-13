package aces.webctrl.scripts.commissioning.web;
import aces.webctrl.scripts.commissioning.core.*;
import javax.servlet.http.*;
import java.util.*;
public class MappingPage extends ServletBase {
  @Override public void exec(final HttpServletRequest req, final HttpServletResponse res) throws Throwable{
    final String cmd = req.getParameter("cmd");
    final String id = req.getParameter("ID");
    if (cmd==null || id==null || !cmd.equals("delete")){
      if (cmd!=null || id!=null){
        res.sendError(400);
      }else{
        final StringBuilder sb = new StringBuilder(1024);
        TreeMap<String,Integer> map = new TreeMap<String,Integer>();
        String str;
        for (Mapping m:Mapping.instances.values()){
          sb.append("add(").append(m.ID).append(",\"");
          sb.append(Utility.escapeJS(m.getName())).append("\",{");
          for (TestingUnit tu:m.equipment.values()){
            str = m.groupNames.get(tu.getGroup());
            if (str!=null){
              map.put(str,1+map.getOrDefault(str,0));
            }
          }
          boolean start = true;
          for (Map.Entry<String,Integer> entry:map.entrySet()){
            if (start){
              start = false;
            }else{
              sb.append(',');
            }
            sb.append('"').append(Utility.escapeJS(entry.getKey())).append("\":").append(entry.getValue());
          }
          map.clear();
          sb.append("},[");
          start = true;
          for (String s:m.tags.keySet()){
            if (start){
              start = false;
            }else{
              sb.append(',');
            }
            sb.append('"').append(Utility.escapeJS(s)).append('"');
          }
          sb.append("]);\n");
        }
        res.setContentType("text/html");
        res.getWriter().print(getHTML(req).replace("//__SCRIPT__",sb.toString()));
      }
    }else{
      final Mapping m = Mapping.instances.get(Integer.parseInt(id));
      if (m==null){
        String err = "Cannot locate a mapping with the given ID.";
        res.sendError(404, err);
        Initializer.log(new NullPointerException(err));
      }else{
        Mapping.instances.remove(m.ID);
      }
    }
  }
}