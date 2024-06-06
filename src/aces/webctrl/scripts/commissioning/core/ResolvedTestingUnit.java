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
  /** The display name of the location represented by this object. */
  private volatile String displayName;
  /** The display path of the location represented by this object. */
  private volatile String displayPath;
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
  /** Specifies how many attempts should be made to read and write node values. */
  public volatile int tries = 3;
  /** Specifies how long to wait after failing to read or write a node value. */
  public volatile long failedAttemptTimeout = 300L;
  /** More error messages will be logged when this is true. */
  public volatile boolean verbose = false;
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
      displayName = loc.getDisplayName();
      displayPath = loc.getRelativeDisplayPath(null);
      node = loc.toNode();
      group = tu.getGroup();
      Node n;
      String expr;
      for (SemanticTag st:tagMappings){
        if (st.isLiteral() && (expr=st.getExpression()).charAt(0)=='@'){
          n = new LiteralNode(expr.substring(1));
        }else{
          n = st.resolve(node);
        }
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
   * @return the display name of this location.
   */
  public String getDisplayName(){
    return displayName;
  }
  /**
   * @return the display path of this location.
   */
  public String getDisplayPath(){
    return displayPath;
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
    return setValue(tags.get(tag),value,tries,failedAttemptTimeout);
  }
  /**
   * Sets the value of the given node.
   * @return {@code true} if the node value was set successfully; {@code false} otherwise.
   */
  public boolean setValue(final Node n, final Object value, final int tries, final long failedAttemptTimeout) throws InterruptedException {
    if (n==null){
      return false;
    }
    final String str = String.valueOf(value);
    if (n instanceof LiteralNode){
      ((LiteralNode)n).setValue(str);
      return true;
    }
    final Result<Boolean> ret = new Result<Boolean>();
    for (int i=0;i<tries;++i){
      if (i!=0){
        Thread.sleep(failedAttemptTimeout);
        ret.reset();
      }
      Initializer.enqueue(new WriteAction(){
        public void execute(WritableSystemAccess sys){
          try{
            n.setValue(str);
            ret.setResult(true);
          }catch(Throwable t){
            ret.setResult(false);
            if (verbose){
              Initializer.log(t);
            }
          }
        }
      });
      if (ret.waitForResult(-1) && ret.getResult()){
        return true;
      }
    }
    return false;
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
    if (n instanceof LiteralNode){
      final LiteralNode nn = (LiteralNode)n;
      final String prev = nn.getValue();
      nn.setValue(str);
      marks.put(tag,prev);
      return prev;
    }
    final Result<String> ret = new Result<String>();
    final Container<String> val = new Container<String>(null);
    String s;
    for (int i=0;i<tries;++i){
      if (i!=0){
        Thread.sleep(failedAttemptTimeout);
        ret.reset();
      }
      Initializer.enqueue(new WriteAction(){
        public void execute(WritableSystemAccess sys){
          try{
            if (val.x==null){
              val.x = n.getValue();
            }
            n.setValue(str);
            ret.setResult(val.x);
          }catch(Throwable t){
            ret.setResult(null);
            if (verbose){
              Initializer.log(t);
            }
          }
        }
      });
      if (ret.waitForResult(-1) && (s=ret.getResult())!=null){
        marks.put(tag,s);
        return s;
      }
    }
    return null;
  }
  /**
   * Sets the value of the given node and marks the previous value internally if there does not already exist a recorded mark for {@code tag}.
   * Upon test completion, all marked nodes will have their values reset to marked values. See {@link #reset(String)}.
   * @return {@code true} if the node value was set successfully; {@code false} otherwise.
   */
  public boolean setValueAutoMark(final String tag, final Object val) throws InterruptedException {
    if (marks.containsKey(tag)){
      return setValue(tag,val);
    }else{
      return markAndSetValue(tag,val)!=null;
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
    return getValue(tags.get(tag), tries, failedAttemptTimeout);
  }
  /**
   * @return the value of the node, or {@code null} if the value could not be retrieved.
   */
  public String getValue(Node n, final int tries, final long failedAttemptTimeout) throws InterruptedException {
    if (n==null){
      return null;
    }else if (n instanceof LiteralNode){
      return ((LiteralNode)n).getValue();
    }
    final Result<String> ret = new Result<String>();
    String s;
    for (int i=0;i<tries;++i){
      if (i!=0){
        Thread.sleep(failedAttemptTimeout);
        ret.reset();
      }
      Initializer.enqueue(new ReadAction(){
        public void execute(SystemAccess sys){
          try{
            ret.setResult(n.getValue());
          }catch(Throwable t){
            ret.setResult(null);
            if (verbose){
              Initializer.log(t);
            }
          }
        }
      });
      if (ret.waitForResult(-1) && (s=ret.getResult())!=null){
        return s;
      }
    }
    return null;
  }
  /**
   * @return the display value of the node mapped with the given semantic tag, or {@code null} if the semantic tag mapping does not exist or the value cannot be retrieved for any other reason.
   */
  public String getDisplayValue(String tag) throws InterruptedException {
    return getDisplayValue(tags.get(tag),tries,failedAttemptTimeout);
  }
  /**
   * @return the display value of the node, or {@code null} if the value could not be retrieved.
   */
  public String getDisplayValue(Node n, final int tries, final long failedAttemptTimeout) throws InterruptedException {
    if (n==null){
      return null;
    }else if (n instanceof LiteralNode){
      return ((LiteralNode)n).getDisplayValue();
    }
    final Result<String> ret = new Result<String>();
    String s;
    for (int i=0;i<tries;++i){
      if (i!=0){
        Thread.sleep(failedAttemptTimeout);
        ret.reset();
      }
      Initializer.enqueue(new ReadAction(){
        public void execute(SystemAccess sys){
          try{
            ret.setResult(n.getDisplayValue());
          }catch(Throwable t){
            ret.setResult(null);
            if (verbose){
              Initializer.log(t);
            }
          }
        }
      });
      if (ret.waitForResult(-1) && (s=ret.getResult())!=null){
        return s;
      }
    }
    return null;

  }
  /**
   * Resets the node corresponding to the given semantic tag per the last marked value.
   * See {@link #markAndGetValue(String)}, {@link #markAndSetValue(String, Object)}, and {@link Script#autoReset()}.
   * If the given semantic tag is {@code null}, then all marks are reset.
   * @return whether the reset operation is successful.
   */
  public boolean reset(String tag) throws InterruptedException {
    if (tag==null){
      int s;
      for (int i=0;i<tries;++i){
        if (i!=0){
          Thread.sleep(failedAttemptTimeout);
        }
        s = marks.size();
        if (s==0){
          return true;
        }
        final ArrayList<Result<Boolean>> rets = new ArrayList<Result<Boolean>>(s);
        for (Map.Entry<String,Node> entry:tags.entrySet()){
          final String str = entry.getKey();
          final String value = marks.get(str);
          if (value!=null){
            final Node n = entry.getValue();
            if (n instanceof LiteralNode){
              ((LiteralNode)n).setValue(value);
              marks.remove(str);
              continue;
            }
            final Result<Boolean> ret = new Result<Boolean>();
            rets.add(ret);
            Initializer.enqueue(new WriteAction(){
              public void execute(WritableSystemAccess sys){
                try{
                  n.setValue(value);
                  marks.remove(str);
                  ret.setResult(true);
                }catch(Throwable t){
                  ret.setResult(false);
                  if (verbose){
                    Initializer.log(t);
                  }
                }
              }
            });
          }
        }
        boolean ret = true;
        for (Result<Boolean> r:rets){
          ret&=r.waitForResult(-1)&&r.getResult();
        }
        if (ret){
          return true;
        }
      }
      return false;
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