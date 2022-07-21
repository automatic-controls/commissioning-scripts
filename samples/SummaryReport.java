import aces.webctrl.scripts.commissioning.core.*;
import aces.webctrl.scripts.commissioning.web.*;
import java.util.*;
import java.util.concurrent.*;
/** Essentially summarizes a selected {@code Mapping} by retrieving the values of all mapped nodes. */
public class SummaryReport extends Script {
  /** Flag which records whether the {@link #exit()} method has been called. */
  private volatile boolean exited = false;
  /** Whether scheduled report emails should be sent as a CSV attachment instead of the default embedded HTML. */
  private volatile boolean csv = false;
  /** Stores retrieved data from {@link #exec(ResolvedTestingUnit)} to be printed at a later time by {@link #getOutput(boolean)}. */
  private final ArrayList<Tracker> trackers = new ArrayList<Tracker>();
  private final ConcurrentSkipListSet<String> tags = new ConcurrentSkipListSet<String>();
  @Override public String getDescription(){
    return "Generate a report which retrieves values for all mapped nodes.";
  }
  @Override public String[] getParamNames(){
    return new String[]{"CSV Export to Email"};
  }
  @Override public void init(){
    csv = (Boolean)params.getOrDefault("CSV Export to Email",false);
    trackers.ensureCapacity(this.testsTotal);
  }
  @Override public boolean isEmailCSV(){
    return csv;
  }
  @Override public void exec(ResolvedTestingUnit x) throws Throwable {
    tags.addAll(x.getTags());
    Tracker t = new Tracker(x);
    synchronized (trackers){
      trackers.add(t);
      trackers.sort(null);
    }
  }
  @Override public void exit() throws Throwable {
    exited = true;
  }
  @Override public String getOutput(boolean email) throws Throwable {
    final StringBuilder sb = new StringBuilder(4096);
    final Set<String> tags = this.tags.clone();
    final int cols = tags.size()+1;
    if (csv && email){
      sb.append("Control Program");
      for (String tag:tags){
        sb.append(',');
        sb.append(Utility.escapeCSV(tag));
      }
      synchronized (trackers){
        for (Tracker t:trackers){
          sb.append('\n');
          sb.append(Utility.escapeCSV(t.path));
          for (String tag:tags){
            sb.append(',');
            sb.append(Utility.escapeCSV((String)t.values.getOrDefault(tag,"NULL")));
          }
        }
      }
    }else{
      sb.append("<!DOCTYPE html>\n");
      sb.append("<html lang=\"en\">\n");
      sb.append("<head>\n");
      sb.append("<title>SummaryReport</title>\n");
      if (email){
        sb.append("<style>\n");
        sb.append(ProviderCSS.getCSS());
        sb.append("\n</style>\n");
      }else{
        sb.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"main.css\"/>\n");
      }
      sb.append("</head>\n");
      sb.append("<body>\n");
      sb.append("<div class=\"c\">\n");
      sb.append(Utility.format("<h1>SummaryReport: $0</h1>\n",Utility.escapeHTML(this.mapping.getName())));
      if (!exited){
        final int completed = 100*this.testsCompleted.get()/this.testsTotal;
        final int started = 100*this.testsStarted.get()/this.testsTotal;
        sb.append("<div style=\"position:relative;top:0;left:15%;width:70%;height:2em\">\n");
        sb.append("<div class=\"bar\"></div>\n");
        sb.append(Utility.format("<div class=\"bar\" style=\"background-color:indigo;width:$0%\"></div>\n", started));
        sb.append(Utility.format("<div class=\"bar\" style=\"background-color:blue;width:$0%\"></div>\n", completed));
        sb.append("</div>\n");
      }
      sb.append("<table>\n");
      sb.append("<thead>\n");
      sb.append("<tr>\n");
      sb.append("<th>Control Program</th>\n");
      for (String tag:tags){
        sb.append("<th>").append(Utility.escapeHTML(tag)).append("</th>\n");
      }
      sb.append("</tr>\n");
      sb.append("</thead>\n");
      sb.append("<tbody>\n");
      synchronized (trackers){
        int grp = -1;
        for (Tracker t:trackers){
          if (grp!=t.group){
            if (grp!=-1){
              sb.append("</tbody>\n<tbody>\n");
            }
            grp = t.group;
            sb.append(Utility.format("<tr><th colspan=\"$0\">$1</th></tr>", cols, Utility.escapeHTML((String)this.mapping.groupNames.getOrDefault(grp, "(Deleted Group)"))));
          }
          sb.append("<tr>\n");
          sb.append(Utility.format("<td><a target=\"_blank\" href=\"$0\">$1</a></td>\n", t.link, t.path));
          for (String tag:tags){
            sb.append("<td>").append(Utility.escapeHTML((String)t.values.getOrDefault(tag,"NULL"))).append("</td>\n");
          }
          sb.append("</tr>\n");
        }
      }
      sb.append("</tbody>\n");
      sb.append("</table>\n");
      sb.append("</div>\n");
      if (!exited){
        sb.append("<script>\n");
        sb.append("setTimeout(()=>{window.location.reload();}, 1000);");
        sb.append("</script>\n");
      }
      sb.append("</body>\n");
      sb.append("</html>");
    }
    return sb.toString();
  }
}
class Tracker implements Comparable<Object> {
  public volatile String link;
  public volatile String path;
  public volatile TreeMap<String,String> values = new TreeMap<String,String>();
  public volatile int group;
  public Tracker(ResolvedTestingUnit x) throws InterruptedException {
    group = x.getGroup();
    link = x.getPersistentLink();
    path = Utility.escapeHTML(x.getDisplayPath());
    String val;
    for (String tag:x.getTags()){
      val = x.getValue(tag);
      values.put(tag,val==null?"NULL":val);
    }
  }
  public int compareTo(Object obj){
    if (obj instanceof Tracker){
      Tracker t = (Tracker)obj;
      if (group==t.group){
        return path.compareTo(t.path);
      }else{
        return group-t.group;
      }
    }else{
      return -1;
    }
  }
}