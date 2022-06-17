package aces.webctrl.scripts.commissioning.core;
import com.controlj.green.addonsupport.access.*;
import com.controlj.green.addonsupport.access.node.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
public class ResolvedTestingUnit {
  private volatile Location loc;
  private volatile Node node;
  private volatile int group;
  private volatile String ID;
  private final ConcurrentSkipListMap<String,Node> tags = new ConcurrentSkipListMap<String,Node>();
  private volatile boolean valid = false;
  private final AtomicBoolean started = new AtomicBoolean();
  private volatile boolean completed = false;
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
    final Node n = tags.get(tag);
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
   * @return the value of the node mapped with the given semantic tag, or {@code null} if the semantic tag mapping does not exist or the value cannot be retrieved for any other reason.
   */
  public String getValue(String tag) throws InterruptedException {
    final Node n = tags.get(tag);
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