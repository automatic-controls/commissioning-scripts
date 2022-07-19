package aces.webctrl.scripts.commissioning.web;
import aces.webctrl.scripts.commissioning.core.*;
import com.controlj.green.addonsupport.access.*;
import javax.servlet.http.*;
import java.util.*;
public class MappingEditPage extends ServletBase {
  @Override public void exec(final HttpServletRequest req, final HttpServletResponse res) throws Throwable {
    final String cmd = req.getParameter("cmd");
    if (cmd==null){
      String err = "No command parameter was given.";
      res.sendError(400, err);
      Initializer.log(new NullPointerException(err));
    }else{
      switch (cmd){
        case "new":{
          res.setContentType("text/html");
          res.getWriter().print(getHTML(req).replace("__MAPPING_ID__","-1").replace("__MAPPING_NAME__",""));
          break;
        }
        case "get":{
          final String ID = req.getParameter("ID");
          if (ID==null){
            String err = "The mapping ID request parameter was not specified.";
            res.sendError(400, err);
            Initializer.log(new NullPointerException(err));
          }else{
            final Mapping m = Mapping.instances.get(Integer.parseInt(ID));
            if (m==null){
              String err = "Could not locate mapping.";
              res.sendError(404, err);
              Initializer.log(new NullPointerException(err));
            }else{
              final StringBuilder sb = new StringBuilder(2048);
              for (SemanticTag tag:m.tags.values()){
                sb.append("addTag(\"");
                sb.append(Utility.escapeJS(tag.getTag()));
                sb.append("\",\"");
                sb.append(Utility.escapeJS(tag.getExpression()));
                sb.append("\");\n");
              }
              final TreeMap<String,ArrayList<String>> groups = new TreeMap<String,ArrayList<String>>();
              String name;
              ArrayList<String> arr;
              for (TestingUnit tu:m.equipment.values()){
                name = m.groupNames.get(tu.getGroup());
                arr = groups.get(name);
                if (arr==null){
                  arr = new ArrayList<String>(8);
                  groups.put(name,arr);
                }
                arr.add(tu.getID());
              }
              Initializer.getConnection().runReadAction(FieldAccessFactory.newDisabledFieldAccess(), new ReadAction(){
                public void execute(final SystemAccess sys){
                  try{
                    final Tree tree = sys.getTree(SystemTree.Geographic);
                    for (Map.Entry<String,ArrayList<String>> entry:groups.entrySet()){
                      sb.append("addGroup(\"");
                      sb.append(Utility.escapeJS(entry.getKey()));
                      sb.append("\",{");
                      boolean first = true;
                      String name;
                      for (String s:entry.getValue()){
                        try{
                          name = tree.resolve(s).getRelativeDisplayPath(null);
                        }catch(Throwable t){
                          continue;
                        }
                        if (first){
                          first = false;
                        }else{
                          sb.append(',');
                        }
                        sb.append('"');
                        sb.append(Utility.escapeJS(s));
                        sb.append("\":\"");
                        sb.append(Utility.escapeJS(name));
                        sb.append('"');
                      }
                      sb.append("});\n");
                    }
                  }catch(Throwable t){
                    Initializer.log(t);
                  }
                }
              });
              res.setContentType("text/html");
              res.getWriter().print(getHTML(req)
                .replace("__MAPPING_ID__", String.valueOf(m.ID))
                .replace("__MAPPING_NAME__", Utility.escapeHTML(m.getName()))
                .replace("//__SCRIPT__", sb.toString())
              );
            }
          }
          break;
        }
        case "save":{
          final String ID = req.getParameter("ID");
          final String name = req.getParameter("name");
          final String _tags = req.getParameter("tags");
          final String _groups = req.getParameter("groups");
          if (ID==null || name==null || _tags==null || _groups==null){
            String err = "Required parameters were not specified.";
            res.sendError(400, err);
            Initializer.log(new NullPointerException(err));
          }else{
            final int id = Integer.parseInt(ID);
            final ArrayList<String> tags = Utility.decodeList(_tags);
            final TreeMap<String,SemanticTag> tagMap = new TreeMap<String,SemanticTag>();
            String s;
            for (int i=0,j=tags.size()-1;i<j;++i){
              s = tags.get(i);
              try{
                tagMap.put(s, new SemanticTag(s,tags.get(++i)));
              }catch(java.util.regex.PatternSyntaxException e){
                Initializer.log(e);
              }
            }
            final ArrayList<String> groups = Utility.decodeList(_groups);
            final TreeMap<Integer,String> groupNameMap = new TreeMap<Integer,String>();
            final TreeMap<String,TestingUnit> equipmentMap = new TreeMap<String,TestingUnit>();
            for (int i=0,j=0,k,n,l=groups.size()-1;i<l;++j,++i){
              s = groups.get(i);
              k = Integer.parseInt(groups.get(++i));
              if (i+k<=l){
                groupNameMap.put(j,s);
                for (n=0;n<k;++n){
                  s = groups.get(++i);
                  equipmentMap.put(s,new TestingUnit(s,j));
                }
              }else{
                Initializer.log(new ArrayIndexOutOfBoundsException("Malformed group parameter in AJAX save request."));
                break;
              }
            }
            if (id==-1){
              final Mapping m = new Mapping(name);
              m.tags.putAll(tagMap);
              m.groupNames.putAll(groupNameMap);
              m.equipment.putAll(equipmentMap);
              res.sendRedirect(req.getContextPath()+"/MappingEditor?cmd=get&ID="+m.ID);
            }else{
              final Mapping m = Mapping.instances.get(id);
              if (m==null){
                String err = "Could not locate mapping.";
                res.sendError(404, err);
                Initializer.log(new NullPointerException(err));
              }else{
                m.setName(name);
                m.tags.clear();
                m.tags.putAll(tagMap);
                m.equipment.clear();
                m.groupNames.clear();
                m.groupNames.putAll(groupNameMap);
                m.equipment.putAll(equipmentMap);
                res.sendRedirect(req.getContextPath()+"/MappingEditor?cmd=get&ID="+ID);
              }
            }
          }
          break;
        }
        case "load":{
          final String ID = req.getParameter("ID");
          final LinkedList<Area> scope = new LinkedList<Area>();
          final ArrayList<Area> areas = new ArrayList<Area>();
          final ArrayList<Area> equipment = new ArrayList<Area>();
          Initializer.getConnection().runReadAction(FieldAccessFactory.newDisabledFieldAccess(), new ReadAction(){
            public void execute(SystemAccess sys){
              try{
                final Location root = sys.getGeoRoot();
                Location loc;
                if (ID==null){
                  loc = root;
                }else{
                  try{
                    loc = sys.getTree(SystemTree.Geographic).resolve(ID);
                  }catch(Throwable t){
                    loc = root;
                  }
                }
                for (Location l=loc;;l=l.getParent()){
                  scope.addFirst(new Area(l));
                  if (l==root || !l.hasParent()){
                    break;
                  }
                }
                LocationType type;
                for (Location l:loc.getChildren()){
                  type = l.getType();
                  if (type==LocationType.Equipment){
                    equipment.add(new Area(l));
                  }else if (type==LocationType.Area || type==LocationType.System || type==LocationType.Directory){
                    areas.add(new Area(l));
                  }
                }
              }catch(Throwable t){
                Initializer.log(t);
              }
            }
          });
          final StringBuilder sb = new StringBuilder(2048);
          sb.append("{\"scope\":[");
          boolean first = true;
          for (Area x:scope){
            if (first){
              first = false;
            }else{
              sb.append(',');
            }
            sb.append("{\"name\":\"").append(Utility.escapeJSON(x.name));
            sb.append("\",\"id\":\"").append(Utility.escapeJSON(x.ID));
            sb.append("\"}");
          }
          sb.append("],\"areas\":[");
          first = true;
          for (Area x:areas){
            if (first){
              first = false;
            }else{
              sb.append(',');
            }
            sb.append("{\"name\":\"").append(Utility.escapeJSON(x.name));
            sb.append("\",\"id\":\"").append(Utility.escapeJSON(x.ID));
            sb.append("\"}");
          }
          sb.append("],\"equipment\":[");
          first = true;
          for (Area x:equipment){
            if (first){
              first = false;
            }else{
              sb.append(',');
            }
            sb.append("{\"name\":\"").append(Utility.escapeJSON(x.name));
            sb.append("\",\"id\":\"").append(Utility.escapeJSON(x.ID));
            sb.append("\"}");
          }
          sb.append("]}");
          res.setContentType("text/plain");
          res.getWriter().print(sb.toString());
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
class Area {
  public String ID;
  public String name;
  public Area(){}
  public Area(String ID, String name){
    this.ID = ID;
    this.name = name;
  }
  public Area(Location loc){
    ID = loc.getPersistentLookupString(true);
    name = loc.getDisplayName();
  }
}