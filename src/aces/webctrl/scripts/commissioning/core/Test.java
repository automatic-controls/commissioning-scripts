package aces.webctrl.scripts.commissioning.core;
import org.codehaus.janino.*;
import com.controlj.green.addonsupport.access.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.IntUnaryOperator;
import java.nio.file.*;
import java.io.*;
public class Test {
  public final static ConcurrentSkipListMap<Integer,Test> instances = new ConcurrentSkipListMap<Integer,Test>();
  private final static AtomicInteger nextID = new AtomicInteger();
  public final int ID = nextID.getAndIncrement();
  private volatile String name;
  private volatile String description;
  private volatile Path scriptFile;
  private final AtomicBoolean running = new AtomicBoolean();
  public volatile String status;
  private volatile boolean kill = false;
  private volatile Thread[] threads = null;
  private volatile TreeSet<String> requiredTags = new TreeSet<String>();
  private volatile String outputPrefix = null;
  private volatile StringBuilder outputBody = new StringBuilder();
  private volatile String outputSuffix = null;
  /**
   * Construct a new test using the given parameters.
   */
  public Test(Path scriptFile){
    clearStatus();
    this.scriptFile = scriptFile;
    this.name = scriptFile.getFileName().toString();
    try{
      Script s = getScript();
      description = s==null?"Could not locate implementation of "+Script.class.getName():s.getDescription();
    }catch(Throwable t){
      Initializer.log(t);
      description = "Failed to compile script.";
    }
    instances.put(ID,this);
  }
  /**
   * Require a new semantic tag for mappings.
   * Intended to be invoked by scripts.
   */
  public void addRequiredTag(String tag){
    requiredTags.add(tag);
  }
  /**
   * Appends some text to the output string.
   * Intended to be invoked by scripts.
   */
  public void appendOutput(CharSequence seq){
    synchronized (outputBody){
      outputBody.append(seq);
    }
  }
  /**
   * Set the output prefix.
   * Intended to be invoked by scripts.
   */
  public void setOutputPrefix(String str){
    outputPrefix = str;
  }
  /**
   * Set the output suffix.
   * Intended to be invoked by scripts.
   */
  public void setOutputSuffix(String str){
    outputSuffix = str;
  }
  /**
   * @return the output of the test.
   */
  public String getOutput(){
    final String pre = outputPrefix;
    final String suf = outputSuffix;
    int len = 0;
    if (pre!=null){
      len+=pre.length();
    }
    if (suf!=null){
      len+=suf.length();
    }
    StringBuilder sb;
    synchronized (outputBody){
      sb = new StringBuilder(len+outputBody.length());
      if (pre!=null){
        sb.append(pre);
      }
      sb.append(outputBody);
    }
    if (suf!=null){
      sb.append(suf);
    }
    return sb.toString();
  }
  /**
   * Erases the output body (does not include the prefix or suffix).
   */
  public void clearOutputBody(){
    synchronized (outputBody){
      outputBody.setLength(0);
    }
  }
  /**
   * Resets the status message of this test.
   */
  public void clearStatus(){
    status = "Idle";
  }
  /**
   * @return the path to this script file.
   */
  public Path getScriptFile(){
    return scriptFile;
  }
  /**
   * @return the status of this test.
   */
  public String getStatus(){
    return status;
  }
  /**
   * @return the name of this test.
   */
  public String getName(){
    return name;
  }
  /**
   * @return a descriptive detail for this test.
   */
  public String getDescription(){
    return description;
  }
  /**
   * Attempts to kill any currently executing test.
   */
  public synchronized void kill(){
    kill = running.get();
    if (kill && threads!=null){
      for (int i=0;i<threads.length;++i){
        threads[i].interrupt();
      }
    }
  }
  /**
   * @return whether the previous test was prematurely terminated.
   */
  public boolean isKilled(){
    return kill;
  }
  /**
   * @return whether this test is currently executing.
   */
  public boolean isRunning(){
    return running.get();
  }
  /**
   * Waits for this test to terminate.
   */
  public void waitForDeath() throws InterruptedException {
    while (running.get()){
      Thread.sleep(1000L);
    }
  }
  /**
   * Attempts to initiate a new test.
   * @return whether test initiation was successful.
   */
  public boolean initiate(Mapping m, int threadCount, double maxTests, String operator){
    if (!Initializer.isDying() && running.compareAndSet(false,true)){
      final long startTime = System.currentTimeMillis();
      boolean ret = true;
      boolean invokeTerminate = false;
      final Container<Script> script = new Container<Script>();
      init:{
        try{
          kill = false;
          status = "Initiating";
          if (threadCount<1){
            threadCount = 1;
          }
          if (threadCount>64){
            threadCount = 64;
          }
          if (maxTests<0){
            maxTests = 0;
          }
          if (maxTests>1){
            maxTests = 1;
          }
          if (!Files.exists(scriptFile)){
            status = "Initialization error: Script file does not exist";
            ret = false;
            break init;
          }
          script.x = getScript();
          if (script.x==null){
            status = "Could not locate implementation of "+Script.class.getName();
            ret = false;
            break init;
          }
          description = script.x.getDescription();
          requiredTags.clear();
          outputPrefix = null;
          outputSuffix = null;
          clearOutputBody();
          if (kill){
            clearStatus();
            ret = false;
            break init;
          }
          script.x.test = this;
          try{
            script.x.init();
            invokeTerminate = true;
          }catch(Throwable t){
            status = "Initialization error occurred: See log file for details";
            Initializer.log(t);
            ret = false;
            break init;
          }
          if (kill){
            clearStatus();
            ret = false;
            break init;
          }
          final Container<ConcurrentSkipListMap<String,ResolvedTestingUnit>> units = new Container<ConcurrentSkipListMap<String,ResolvedTestingUnit>>();
          Initializer.getConnection().runReadAction(FieldAccessFactory.newDisabledFieldAccess(), new ReadAction(){
            public void execute(SystemAccess sys){
              units.x = m.resolve(sys.getTree(SystemTree.Geographic), requiredTags);
            }
          });
          if (units.x==null){
            status = "No testing units could be resolved.";
            ret = false;
            break init;
          }
          if (kill){
            clearStatus();
            ret = false;
            break init;
          }
          final Collection<ResolvedTestingUnit> rtus = units.x.values();
          ConcurrentSkipListMap<Integer,GroupTracker> groupMap = new ConcurrentSkipListMap<Integer,GroupTracker>();
          {
            GroupTracker gt;
            for (ResolvedTestingUnit rtu:rtus){
              gt = groupMap.get(rtu.getGroup());
              if (gt==null){
                groupMap.put(rtu.getGroup(), new GroupTracker(rtu.getGroup()));
              }else{
                ++gt.size;
              }
            }
          }
          final GroupTracker[] groups = (GroupTracker[])groupMap.values().toArray();
          groupMap = null;
          int threadLimit = 0;
          final double mtest = maxTests;
          for (int i=0;i<groups.length;++i){
            groups[i].init(mtest);
            threadLimit+=groups[i].getMaxRunning();
          }
          threadCount = Math.min(threadCount, threadLimit);
          final int total = units.x.size();
          final AtomicInteger numStarted = new AtomicInteger();
          final AtomicInteger numCompleted = new AtomicInteger();
          final AtomicInteger stopped = new AtomicInteger();
          final AtomicInteger groupIndex = new AtomicInteger();
          final IntUnaryOperator groupCycle = new IntUnaryOperator(){
            public int applyAsInt(int x){
              ++x;
              if (x>=groups.length){
                x = 0;
              }
              return x;
            }
          };
          synchronized (this){
            if (kill){
              clearStatus();
              ret = false;
              break init;
            }
            threads = new Thread[threadCount];
            for (int i=0;i<threadCount;++i){
              threads[i] = new Thread(){
                public void run(){
                  try{
                    ResolvedTestingUnit rtu;
                    int i,grp,startIndex;
                    while (numStarted.get()<total && !kill){
                      grp = -1;
                      startIndex = groupIndex.getAndUpdate(groupCycle);
                      i = startIndex;
                      do {
                        if (groups[i].start()){
                          grp = groups[i].getIndex();
                          break;
                        }
                        ++i;
                        if (i==groups.length){
                          i = 0;
                        }
                      } while (i!=startIndex);
                      if (grp==-1){
                        try{
                          Thread.sleep(1000L);
                        }catch(InterruptedException e){}
                        continue;
                      }
                      numStarted.incrementAndGet();
                      rtu = null;
                      for (ResolvedTestingUnit x:rtus){
                        if (x.getGroup()==grp && x.start()){
                          rtu = x;
                          break;
                        }
                      }
                      if (kill){
                        break;
                      }
                      if (rtu==null){
                        //Should never occur
                        throw new Exception("Fatal flaw detected in logic.");
                      }else{
                        boolean autoReset = true;
                        try{
                          script.x.exec(rtu);
                          autoReset = script.x.autoReset();
                        }catch(InterruptedException e){}catch(Throwable t){
                          Initializer.log(t);
                        }
                        if (autoReset){
                          rtu.reset(null);
                        }
                        rtu.complete();
                        groups[i].complete();
                        status = String.valueOf(100*numCompleted.incrementAndGet()/total)+'%';
                      }
                    }
                  }catch(Throwable t){
                    Initializer.log(t);
                  }
                  if (stopped.incrementAndGet()>=threads.length){
                    try{
                      script.x.exit();
                      clearStatus();
                    }catch(InterruptedException e){}catch(Throwable t){
                      Initializer.log(t);
                      status = "Termination error occurred: See log file for details";
                    }
                    ArchivedTest at = new ArchivedTest(name, operator, startTime, System.currentTimeMillis(), threads.length, mtest);
                    threads = null;
                    at.save(getOutput());
                    running.set(false);
                  }
                }
              };
            }
          }
          if (kill){
            clearStatus();
            threads = null;
            ret = false;
            break init;
          }
          status = "0%";
          for (int i=0;i<threadCount;++i){
            threads[i].start();
          }
        }catch(Throwable t){
          status = "Initialization error occurred: See log file for details";
          Initializer.log(t);
          ret = false;
          break init;
        }
      }
      if (!ret){
        if (invokeTerminate){
          try{
            script.x.exit();
          }catch(InterruptedException e){}catch(Throwable t){
            Initializer.log(t);
          }
        }
        running.set(false);
      }
      return ret;
    }
    return false;
  }
  private Script getScript() throws Throwable {
    final SimpleCompiler sc = new SimpleCompiler();
    try(
      FileReader r = new FileReader(scriptFile.toFile());
      BufferedReader rr = new BufferedReader(r);
    ){
      sc.cook(name,rr);
    }
    ClassLoader cl = sc.getClassLoader();
    org.codehaus.janino.util.ClassFile[] cfs = sc.getClassFiles();
    for (int i=0;i<cfs.length;++i){
      try{
        return cl.loadClass(cfs[i].getThisClassName()).asSubclass(Script.class).getDeclaredConstructor().newInstance();
      }catch(Throwable t){}
    }
    return null;
  }
}