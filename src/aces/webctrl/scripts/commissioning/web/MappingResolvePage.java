package aces.webctrl.scripts.commissioning.web;
import aces.webctrl.scripts.commissioning.core.*;
import com.controlj.green.addonsupport.access.*;
import com.controlj.green.addonsupport.web.*;
import javax.servlet.http.*;
import java.util.*;
public class MappingResolvePage extends ServletBase {
  @Override public void exec(final HttpServletRequest req, final HttpServletResponse res) throws Throwable {
    final String id = req.getParameter("ID");
    if (id==null){
      res.sendError(400, "HTTP request parameter \"ID\" is missing.");
    }else{
      final Mapping m = Mapping.instances.get(Integer.parseInt(id));
      if (m==null){
        res.sendError(404, "Requested mapping does not exist.");
      }else{
        final ArrayList<ResolvedInfo> rinfo = new ArrayList<ResolvedInfo>();
        Initializer.getConnection().runReadAction(FieldAccessFactory.newDisabledFieldAccess(), new ReadAction(){
          public void execute(SystemAccess sys){
            Map<String,ResolvedTestingUnit> units = m.resolve(sys.getTree(SystemTree.Geographic), Collections.emptyList());
            if (units!=null){
              rinfo.ensureCapacity(units.size());
              for (ResolvedTestingUnit rtu:units.values()){
                rinfo.add(new ResolvedInfo(rtu));
              }
            }
          }
        });
        for (ResolvedInfo r:rinfo){
          r.link(req);
          r.groupName = m.groupNames.getOrDefault(r.grp, "Unknown");
        }
        rinfo.sort(null);
        final StringBuilder sb = new StringBuilder(rinfo.size()<<7);
        String group = null;
        sb.append("<tbody>\n");
        for (ResolvedInfo r:rinfo){
          if (!r.groupName.equals(group)){
            if (group!=null){
              sb.append("</tbody>\n<tbody>\n");
            }
            group = r.groupName;
            sb.append("<tr><th colspan=\"2\">").append(Utility.escapeHTML(group)).append("</th></tr>");
          }
          sb.append("<tr>\n");
          if (r.link==null){
            sb.append("<td>").append(Utility.escapeHTML(r.path)).append("</td>\n");
          }else{
            sb.append("<td><a href=\"").append(r.link).append("\">").append(Utility.escapeHTML(r.path)).append("</a></td>\n");
          }
          sb.append("<td>\n");
          for (String tag:r.tags){
            sb.append("<div class=\"tagYes\">").append(Utility.escapeHTML(tag)).append("</div>\n");
          }
          sb.append("</td>\n");
          sb.append("</tr>\n");
        }
        sb.append("</tbody>");
        res.setContentType("text/html");
        res.getWriter().print(getHTML(req).replace("__NAME__", m.getName()).replace("__DATA__", sb));
      }
    }
  }
}
class ResolvedInfo implements Comparable<ResolvedInfo> {
  public String path;
  public Set<String> tags;
  public String link;
  public int grp;
  public String groupName;
  private Location loc;
  public ResolvedInfo(ResolvedTestingUnit rtu){
    loc = rtu.getLocation();
    path = loc.getRelativeDisplayPath(null);
    tags = rtu.getTags();
    grp = rtu.getGroup();
  }
  public void link(HttpServletRequest req){
    try{
      link = Link.createLink(UITree.GEO, loc).getURL(req);
    }catch(Throwable t){
      link = null;
    }
    loc = null;
  }
  @Override public int compareTo(ResolvedInfo r){
    return groupName.compareTo(r.groupName);
  }
}