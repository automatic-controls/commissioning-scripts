package aces.webctrl.scripts.commissioning.core;
import com.controlj.green.addonsupport.access.*;
import java.util.*;
import java.util.regex.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.io.*;
public class Mapping {
  private final static Pattern newline = Pattern.compile("\\n");
  private final static AtomicInteger nextID = new AtomicInteger();
  public final static ConcurrentSkipListMap<Integer,Mapping> instances = new ConcurrentSkipListMap<Integer,Mapping>();
  public final int ID = nextID.getAndIncrement();
  /**
   * Stores semantic tag mappings.
   */
  public final ConcurrentSkipListMap<String,SemanticTag> tags = new ConcurrentSkipListMap<String,SemanticTag>();
  /**
   * Stores references to individual control programs for testing in groups.
   */
  public final ConcurrentSkipListMap<String,TestingUnit> equipment = new ConcurrentSkipListMap<String,TestingUnit>();
  /**
   * A name for this grouping.
   */
  private volatile String name;
  /**
   * Constructs a new mapping.
   */
  public Mapping(String name){
    instances.put(ID,this);
  }
  /**
   * This method should be invoked within a database read action that has disabled field access.
   * @return a non-empty map of resolved testing units, or {@code null} if no testing units could be resolved.
   */
  public ConcurrentSkipListMap<String,ResolvedTestingUnit> resolve(Tree tree, Collection<String> requiredTags){
    for (String tag:requiredTags){
      if (!tags.containsKey(tag)){
        return null;
      }
    }
    ConcurrentSkipListMap<String,ResolvedTestingUnit> map = new ConcurrentSkipListMap<String,ResolvedTestingUnit>();
    Collection<SemanticTag> sts = tags.values();
    ResolvedTestingUnit rtu;
    for (TestingUnit tu:equipment.values()){
      rtu = new ResolvedTestingUnit(tu, tree, sts, requiredTags);
      if (rtu.isValid()){
        map.put(rtu.getID(), rtu);
      }else if (!tu.resolveSuccess){
        equipment.remove(tu.getID());
      }
    }
    return map.isEmpty()?null:map;
  }
  /**
   * @return the name of this mapping.
   */
  public String getName(){
    return name;
  }
  /**
   * Sets the name of this mapping.
   */
  public void setName(String name){
    this.name = name;
  }
  /**
   * Exports all semantic tag data to the given StringBuilder.
   */
  public void exportTags(StringBuilder sb){
    for (SemanticTag st:tags.values()){
      sb.append(st.getTag()).append('\n').append(st.getExpression()).append('\n');
    }
  }
  /**
   * Imports semantic tag data from the given {@code InputStream}.
   */
  public void importTags(InputStream in) throws IOException, PatternSyntaxException {
    String[] arr = newline.split(new String(Utility.readAllBytes(in),java.nio.charset.StandardCharsets.UTF_8));
    if ((arr.length&1)==1){
      throw new IOException("Cannot parse SemanticTag list data.");
    }
    SemanticTag st;
    for (int i=0;i<arr.length;i+=2){
      st = new SemanticTag(arr[i],arr[i+1]);
      tags.put(st.getTag(), st);
    }
  }
}