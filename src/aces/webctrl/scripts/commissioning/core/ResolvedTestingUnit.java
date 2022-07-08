package aces.webctrl.scripts.commissioning.core;
import com.controlj.green.addonsupport.access.*;
import com.controlj.green.addonsupport.access.node.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
/**
 * Encapsulates a single control program with semantic tag mappings.
 */
public class ResolvedTestingUnit {
  /** The root location of this control program. */
  private volatile Location loc;
  /** The root node of this control program. */
  private volatile Node node;
  /** The group number to which this testing unit belongs */
  private volatile int group;
  /** The unique ID which resolves to this location on the geographic tree. */
  private volatile String ID;
  /** Contains resolved semantic tag mappings for this control program. */
  private final ConcurrentSkipListMap<String,Node> tags = new ConcurrentSkipListMap<String,Node>();
  /** Contains node-value markings possibly used for resetting values after test completion. */
  private final ConcurrentSkipListMap<String,String> marks = new ConcurrentSkipListMap<String,String>();
  /** Indicates whether testing unit resolution was successful. */
  private volatile boolean valid = false;
  /** Indicates whether this testing unit has been started. */
  private final AtomicBoolean started = new AtomicBoolean();
  /** Indicates whether this test unit has been completed. */
  private volatile boolean completed = false;
  /**
   * Constructs a resolved testing unit based on a few relevant pieces of information.
   * @param tu is the testing unit to resolve.
   * @param tree is the tree to resolve against.
   * @param tagMappings specifies which tag resolutions to attempt to make.
   * @param requiredTags specifies which tags need to be mapped for this procedure to be successful.
   */
  public ResolvedTestingUnit(TestingUnit tu, Tree tree, Collection<SemanticTag> tagMappings, Collection<String> requiredTags){
    try{
      ID = tu.getID();
      try{
        loc = tree.resolve(ID);
      }catch(UnresolvableException e){
        tu.resolveSuccess = false;
        return;
      }
      node = loc.toNode();
      group = tu.getGroup();
      Node n;
      for (SemanticTag st:tagMappings){
        n = st.resolve(node);
        if (n!=null){
          tags.put(st.getTag(), n);
        }
      }
      for (String tag:requiredTags){
        if (!tags.containsKey(tag)){
          return;
        }
      }
    }catch(Throwable t){
      Initializer.log(t);
      return;
    }
    valid = true;
  }
  /**
   * When used to build a return string for {@link Script#getOutput()},
   * this will be dynamically replaced with a link to the location of this testing unit.
   * @return {@code "@&#123;getPersistentLink("+getID()+")&#125;"}
   */
  public String getPersistentLink(){
    return "@{getPersistentLink("+ID+")}";
  }
  /**
   * @return names of all mapped semantic tags for this program.
   */
  public Set<String> getTags(){
    return tags.keySet();
  }
  /**
   * @return whether this testing unit was successfully initialized.
   */
  public boolean isValid(){
    return valid;
  }
  /**
   * Sets the value of the node mapped to the given semantic tag.
   * @return {@code true} if the semantic tag mapping exists and the value was set successfully; {@code false} otherwise.
   */
  public boolean setValue(final String tag, final Object value) throws InterruptedException {
    return setValue(tags.get(tag),value);
  }
  /**
   * Sets the value of the given node.
   * @return {@code true} if the node value was set successfully; {@code false} otherwise.
   */
  public static boolean setValue(final Node n, final Object value) throws InterruptedException {
    if (n==null){
      return false;
    }
    final String str = String.valueOf(value);
    final Result<Boolean> ret = new Result<Boolean>();
    Initializer.enqueue(new WriteAction(){
      public void execute(WritableSystemAccess sys){
        try{
          n.setValue(str);
          ret.setResult(true);
        }catch(Throwable t){
          ret.setResult(false);
        }
      }
    });
    return ret.waitForResult(-1) && ret.getResult();
  }
  /**
   * Sets the value of the given node and marks the previous value internally.
   * Upon test completion, all marked nodes will have their values reset to marked values. See {@link #reset(String)}.
   * @return the previous value of the node, or {@code null} if any part of the operation is unsuccessful.
   */
  public String markAndSetValue(final String tag, final Object value) throws InterruptedException {
    final Node n = tags.get(tag);
    if (n==null){
      return null;
    }
    final String str = String.valueOf(value);
    final Result<String> ret = new Result<String>();
    Initializer.enqueue(new WriteAction(){
      public void execute(WritableSystemAccess sys){
        try{
          String val = n.getValue();
          n.setValue(str);
          ret.setResult(val);
        }catch(Throwable t){
          ret.setResult(null);
        }
      }
    });
    if (ret.waitForResult(-1)){
      String s = ret.getResult();
      marks.put(tag,s);
      return s;
    }else{
      return null;
    }
  }
  /**
   * Gets the value of a node and internally records it.
   * Upon test completion, all marked nodes will have their values reset to marked values. See {@link #reset(String)}.
   * @return the value of the node mapped with the given semantic tag, or {@code null} if the semantic tag mapping does not exist or the value cannot be retrieved for any other reason.
   */
  public String markAndGetValue(String tag) throws InterruptedException {
    final String value = getValue(tag);
    if (value==null){
      return null;
    }else{
      marks.put(tag,value);
    }
    return value;
  }
  /**
   * @return the value of the node mapped with the given semantic tag, or {@code null} if the semantic tag mapping does not exist or the value cannot be retrieved for any other reason.
   */
  public String getValue(String tag) throws InterruptedException {
    return getValue(tags.get(tag));
  }
  /**
   * @return the value of the node, or {@code null} if the value could not be retrieved.
   */
  public static String getValue(Node n) throws InterruptedException {
    if (n==null){
      return null;
    }
    final Result<String> ret = new Result<String>();
    Initializer.enqueue(new ReadAction(){
      public void execute(SystemAccess sys){
        try{
          ret.setResult(n.getValue());
        }catch(Throwable t){
          ret.setResult(null);
        }
      }
    });
    return ret.waitForResult(-1)?ret.getResult():null;
  }
  /**
   * Resets the node corresponding to the given semantic tag per the last marked value.
   * See {@link #markAndGetValue(String)}, {@link #markAndSetValue(String, Object)}, and {@link Script#autoReset()}.
   * If the given semantic tag is {@code null}, then all marks are reset.
   * @return whether the reset operation is successful.
   */
  public boolean reset(String tag) throws InterruptedException {
    if (tag==null){
      int s = marks.size();
      if (s==0){
        return true;
      }
      final ArrayList<Result<Boolean>> rets = new ArrayList<Result<Boolean>>(s);
      String value;
      for (Map.Entry<String,Node> entry:tags.entrySet()){
        final String str = entry.getKey();
        value = marks.remove(str);
        if (value!=null){
          final Node n = entry.getValue();
          final Result<Boolean> ret = new Result<Boolean>();
          rets.add(ret);
          Initializer.enqueue(new WriteAction(){
            public void execute(WritableSystemAccess sys){
              try{
                n.setValue(str);
                ret.setResult(true);
              }catch(Throwable t){
                ret.setResult(false);
              }
            }
          });
        }
      }
      boolean ret = true;
      for (Result<Boolean> r:rets){
        ret&=r.waitForResult(-1)&&r.getResult();
      }
      return ret;
    }else{
      String value = marks.remove(tag);
      if (value==null){
        return false;
      }else{
        return setValue(tag,value);
      }
    }
  }
  /**
   * @return whether the given semantic tag is mapped to some node in this testing unit.
   */
  public boolean hasMapping(String tag){
    return tags.containsKey(tag);
  }
  /**
   * @return the node mapped to the given semantic tag.
   */
  public Node getNode(String tag){
    return tags.get(tag);
  }
  /**
   * @return the root location on the geographic tree for this testing unit.
   */
  public Location getLocation(){
    return loc;
  }
  /**
   * @return the root node on the geographic tree for this testing unit.
   */
  public Node getNode(){
    return node;
  }
  /**
   * @return the number of the group that this testing unit corresponds to.
   */
  public int getGroup(){
    return group;
  }
  /**
   * @return the unique ID which resolves to the root location of this testing unit on the geographic tree.
   */
  public String getID(){
    return ID;
  }
  /**
   * @return whether this test has been completed.
   */
  public boolean isComplete(){
    return completed;
  }
  /**
   * @return whether this test has been started.
   */
  public boolean isStarted(){
    return started.get();
  }
  /**
   * This method will return {@code true} exactly one time, and then {@code false} at all future invokations.
   * @return whether this test should be started.
   */
  public boolean start(){
    return started.compareAndSet(false, true);
  }
  /**
   * Notifies this object that the test has been completed.
   */
  public void complete(){
    completed = true;
  }
}