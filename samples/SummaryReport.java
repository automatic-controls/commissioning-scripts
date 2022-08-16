import aces.webctrl.scripts.commissioning.core.*;
import aces.webctrl.scripts.commissioning.web.*;
import java.util.*;
import java.util.concurrent.*;
/**
 * Essentially summarizes a selected {@code Mapping} by retrieving the values of all mapped nodes.
 * Interpreted at runtime by Janino. http://janino-compiler.github.io/janino/
 */
public class SummaryReport extends Script {
  /**
   * Flag which records whether the {@link #exit()} method has been called.
   * Due to multi-threading, non-final fields should be marked {@code volatile}.
   */
  private volatile boolean exited = false;
  /** Whether scheduled report emails should be sent as a CSV attachment instead of an embedded HTML document. */
  private volatile boolean csv = false;
  /** Stores retrieved data from {@link #exec(ResolvedTestingUnit)} to be printed at a later time by {@link #getOutput(boolean)}. */
  private final ArrayList<Tracker> trackers = new ArrayList<Tracker>();
  /**
   * Cumulative record of mapped tags for each control program.
   * We use {@code ConcurrentSkipListSet} for thread safety, as opposed to an object like {@code TreeSet}, which is not thread-safe.
   */
  private final ConcurrentSkipListSet<String> tags = new ConcurrentSkipListSet<String>();
  /**
   * @return a descriptive {@code String} for this script.
   */
  @Override public String getDescription(){
    return "Generate a report which retrieves values for all mapped nodes.";
  }
  /**
   * Before executing this script, users are presented with a checkbox option labelled as "CSV Export to Email".
   */
  @Override public String[] getParamNames(){
    return new String[]{"CSV Export to Email"};
  }
  /**
   * Handles initialization procedures.
   */
  @Override public void init(){
    /*
      Retrieves whether the user selected the checkbox option specified by getParamNames().
      Note that we must explicitly cast the return value to Boolean due to a Janino limitation on generic type arguments.
      http://janino-compiler.github.io/janino/#limitations
    */
    csv = (Boolean)params.getOrDefault("CSV Export to Email",false);
    //For performance (optional), we adjust the capacity of the trackers ArrayList
    trackers.ensureCapacity(this.testsTotal);
  }
  /**
   * Tells the program whether emailed reports should be sent as a CSV attachment instead of an embedded HTML document.
   */
  @Override public boolean isEmailCSV(){
    return csv;
  }
  /**
   * Invoked once for each control program.
   */
  @Override public void exec(ResolvedTestingUnit x) throws Throwable {
    Tracker t = new Tracker(x);
    tags.addAll(x.getTags());
    /*
      Since this method may be invoked concurrently by multiple threads, we synchronize modifications to the trackers ArrayList.
      An alternative would be to use java.util.concurrent.locks.ReentrantReadWriteLock.
      If you are new to multi-threading, you should also familiarize yourself with java.util.concurrent.atomic.AtomicInteger.
    */
    synchronized (trackers){
      trackers.add(t);
      trackers.sort(null);
    }
  }
  /**
   * Invoked after all mapped control programs have been processed by {@link #exec(ResolvedTestingUnit)}.
   */
  @Override public void exit(){
    exited = true;
  }
  /**
   * May be invoked anytime during the lifetime of this script for users to view results.
   * @param email specified whether the returned {@code String} will be used as an email report.
   */
  @Override public String getOutput(boolean email) throws Throwable {
    //We incrementally build the output String.
    final StringBuilder sb = new StringBuilder(4096);
    /*
      For thread-safety, we clone the tag set.
      To understand the problem, suppose the tag set is modified after printing <th> column headers but before printing <td> cells.
    */
    final Set<String> tags = this.tags.clone();
    if (csv && email){
      //Print all data in the CSV format for an email.
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
            sb.append(Utility.escapeCSV((String)t.values.getOrDefault(tag,"")));
          }
        }
      }
    }else{
      //Number of columns in the printed HTML table.
      final int cols = tags.size()+1;
      //Print all data in a HTML document.
      //You may find Utility.escapeHTML() and Utility.escapeJS() useful for printing HTML documents.
      sb.append("<!DOCTYPE html>\n");
      sb.append("<html lang=\"en\">\n");
      sb.append("<head>\n");
      sb.append("<title>SummaryReport</title>\n");
      if (email){
        //The link to main.css will not resolve when viewed at the other end of an email, so we embed the required CSS directly.
        sb.append("<style>\n");
        sb.append(ProviderCSS.getCSS());
        sb.append("\n</style>\n");
      }else{
        //This links to the CSS document used by other parts of this addon.
        sb.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"main.css\"/>\n");
        //src\aces\webctrl\scripts\commissioning\html\main.css
      }
      sb.append("</head>\n");
      sb.append("<body>\n");
      sb.append("<div class=\"c\">\n");
      sb.append(Utility.format("<h1>SummaryReport: $0</h1>\n",Utility.escapeHTML(this.mapping.getName())));
      if (!exited){
        //If the script has not completed, we provide a progress bar.
        final int completed = 100*this.testsCompleted.get()/this.testsTotal;
        final int started = 100*this.testsStarted.get()/this.testsTotal;
        sb.append("<div style=\"position:relative;top:0;left:15%;width:70%;height:2em\">\n");
        sb.append("<div class=\"bar\"></div>\n");
        sb.append(Utility.format("<div class=\"bar\" style=\"background-color:indigo;width:$0%\"></div>\n", started));
        sb.append(Utility.format("<div class=\"bar\" style=\"background-color:blue;width:$0%\"></div>\n", completed));
        sb.append("</div>\n");
      }else if (this.test.isKilled()){
        sb.append("<h3 style=\"color:crimson\">Foribly Terminated Before Natural Completion</h3>\n");
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
          sb.append(Utility.format("<td><a target=\"_blank\" href=\"$0\">$1</a></td>\n", t.link, Utility.escapeHTML(t.path)));
          for (String tag:tags){
            sb.append("<td>").append(Utility.escapeHTML((String)t.values.getOrDefault(tag,""))).append("</td>\n");
          }
          sb.append("</tr>\n");
        }
      }
      sb.append("</tbody>\n");
      sb.append("</table>\n");
      sb.append("</div>\n");
      if (!exited){
        //If the script has not completed, we refresh the document every second.
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
/**
 * Stores data about each control program.
 * Note we implement {@code Comparable<Object>} instead of {@code Comparable<Tracker>}
 * due to a Janino limitation on generic type parameters.
 */
class Tracker implements Comparable<Object> {
  public volatile String link;
  public volatile String path;
  public volatile TreeMap<String,String> values = new TreeMap<String,String>();
  public volatile int group;
  public Tracker(ResolvedTestingUnit x) throws InterruptedException {
    group = x.getGroup();
    link = x.getPersistentLink();
    path = x.getDisplayPath();
    String val;
    for (String tag:x.getTags()){
      val = x.getValue(tag);
      values.put(tag,val==null?"":val);
    }
  }
  /**
   * Used for sorting alphanumerically within each group.
   */
  @Override public int compareTo(Object obj){
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