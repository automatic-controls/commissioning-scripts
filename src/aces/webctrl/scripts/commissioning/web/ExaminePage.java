package aces.webctrl.scripts.commissioning.web;
import aces.webctrl.scripts.commissioning.core.*;
import com.controlj.green.addonsupport.access.*;
import com.controlj.green.addonsupport.access.node.*;
import javax.servlet.http.*;
import java.util.*;
import java.util.regex.*;
public class ExaminePage extends ServletBase {
  private final static Pattern quote = Pattern.compile("[\\\\\\[\\]\\.\\{\\}\\^\\$\\?\\*\\+\\|\\(\\)]");
  public final static String quote(CharSequence s){
    return quote.matcher(s).replaceAll("\\\\$0");
  }
  @Override public void exec(final HttpServletRequest req, final HttpServletResponse res) throws Throwable {
    final String cmd = req.getParameter("cmd");
    if (cmd==null){
      String err = "No command parameter was given.";
      res.sendError(400, err);
      Initializer.log(new NullPointerException(err));
    }else{
      final String ID = req.getParameter("ID");
      if (ID==null){
        String err = "No ID parameter was given.";
        res.sendError(400, err);
        Initializer.log(new NullPointerException(err));
      }else{
        switch (cmd){
          case "get":{
            final String[] arr = Initializer.getConnection().runReadAction(FieldAccessFactory.newDisabledFieldAccess(), new ReadActionResult<String[]>(){
              public String[] execute(final SystemAccess sys){
                try{
                  final Location loc = sys.getTree(SystemTree.Geographic).resolve(ID);
                  return new String[]{loc.toNode().getRelativeReferencePath(sys.getGeoRoot().toNode()), loc.getRelativeDisplayPath(null)};
                }catch(Throwable t){
                  return null;
                }
              }
            });
            if (arr==null){
              String err = "Could not resolve location.";
              res.sendError(500, err);
              Initializer.log(new NullPointerException(err));
            }else{
              res.setContentType("text/html");
              res.getWriter().print(getHTML(req).replace("__EQUIPMENT_ID__",Utility.escapeJS(arr[0])).replace("__EQUIPMENT_PATH__",Utility.escapeHTML(arr[1])));
            }
            break;
          }
          case "eval":{
            final String[] values = Initializer.getConnection().runReadAction(FieldAccessFactory.newFieldAccess(), new ReadActionResult<String[]>(){
              public String[] execute(final SystemAccess sys){
                try{
                  final Node n = sys.getGeoRoot().toNode().evalToNode(ID);
                  return new String[]{n.getValue(),n.getDisplayValue()};
                }catch(Throwable t){
                  return new String[]{"NULL","NULL"};
                }
              }
            });
            res.setContentType("text/plain");
            res.getWriter().print("{\"value\":\""+Utility.escapeJSON(values[0])+"\",\"displayValue\":\""+Utility.escapeJSON(values[1])+"\"}");
            break;
          }
          case "load":{
            final StringBuilder sb = new StringBuilder(2048);
            Initializer.getConnection().runReadAction(FieldAccessFactory.newDisabledFieldAccess(), new ReadAction(){
              public void execute(final SystemAccess sys){
                try{
                  final Tree tree = sys.getTree(SystemTree.Geographic);
                  final Node root = sys.getGeoRoot().toNode();
                  final Node n = root.evalToNode(ID);
                  LinkedList<Node> list = new LinkedList<Node>();
                  for (Node m=n;;m=m.getParent()){
                    try{
                      if (m.resolveToLocation(tree).getType()==LocationType.Equipment){
                        break;
                      }
                    }catch(UnresolvableException e){}
                    list.addFirst(m);
                    if (!m.hasParent()){
                      break;
                    }
                  }
                  boolean first = true;
                  String refPath,refName;
                  sb.append("{\"scope\":[");
                  {
                    final StringBuilder pathBuilder = new StringBuilder(128);
                    for (Node m:list){
                      if (first){
                        first = false;
                      }else{
                        sb.append(',');
                      }
                      refName = m.getReferenceName();
                      pathBuilder.append(quote(refName));
                      pathBuilder.append('/');
                      sb.append("{\"refName\":\"");
                      sb.append(Utility.escapeJSON(refName));
                      sb.append("\",\"ID\":\"");
                      sb.append(Utility.escapeJSON(m.getRelativeReferencePath(root)));
                      sb.append("\"}");
                    }
                    refName = null;
                    list = null;
                    refPath = Utility.escapeJSON(pathBuilder.toString());
                  }
                  sb.append("],\"nodes\":[");
                  {
                    List<Node> children = n.getChildren();
                    boolean hasChildren = true;
                    if (children.size()==0){
                      children = Collections.singletonList(n);
                      hasChildren = false;
                    }
                    first = true;
                    for (Node m:children){
                      if (first){
                        first = false;
                      }else{
                        sb.append(',');
                      }
                      refName = m.getReferenceName();
                      sb.append("{\"ID\":\"");
                      sb.append(Utility.escapeJSON(m.getRelativeReferencePath(root)));
                      sb.append("\",\"refName\":\"");
                      sb.append(Utility.escapeJSON(refName));
                      sb.append("\",\"displayName\":\"");
                      sb.append(Utility.escapeJSON(m.getDisplayName()));
                      sb.append("\",\"expr\":\"");
                      if (hasChildren){
                        sb.append(refPath).append(Utility.escapeJSON(quote(refName)));
                      }else{
                        sb.append(refPath,0,refPath.length()-2);
                      }
                      sb.append("\",\"hasChildren\":");
                      sb.append(hasChildren);
                      sb.append('}');
                    }
                  }
                  sb.append("]}");
                }catch(Throwable t){
                  Initializer.log(t);
                  sb.setLength(0);
                }
              }
            });
            if (sb.length()==0){
              res.setStatus(500);
            }else{
              res.setContentType("text/plain");
              res.getWriter().print(sb.toString());
            }
            break;
          }
          default:{
            String err = "The specified command is unrecognized: "+cmd;
            res.sendError(400, err);
            Initializer.log(new NullPointerException(err));
          }
        }
      }
    }
  }
}