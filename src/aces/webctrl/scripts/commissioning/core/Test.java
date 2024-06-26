package aces.webctrl.scripts.commissioning.core;
import org.codehaus.janino.*;
import com.controlj.green.addonsupport.access.*;
import java.util.*;
import java.util.jar.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.IntUnaryOperator;
import java.nio.file.*;
import java.io.*;
import java.security.*;
import javax.servlet.http.*;
import java.net.*;
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
  private volatile Set<String> paramNames = null;
  private volatile Script script = null;
  private volatile String cachedOutput = null;
  private volatile int hash = 0;
  public volatile Map<String,Boolean> lastParams = null;
  private volatile boolean jar = false;
  public volatile boolean reserved = false;
  /**
   * Construct a new test using the given parameters.
   */
  public Test(Path scriptFile){
    clearStatus();
    this.scriptFile = scriptFile;
    this.name = scriptFile.getFileName().toString();
    jar = this.name.endsWith(".jar");
    try(
      Script script = getScript();
    ){}catch(Throwable t){
      Initializer.log(t);
    }
    instances.put(ID,this);
  }
  @Override public int hashCode(){
    return hash;
  }
  public boolean delete(){
    if (reserved){
      return false;
    }
    try{
      kill();
      if (!waitForDeath(5000L)){
        return false;
      }
      try{
        Files.deleteIfExists(scriptFile);
      }catch(Throwable t){
        return false;
      }
      instances.remove(ID);
      final Path scripts = Initializer.getScriptFolder();
      Iterator<ScheduledTest> iter = ScheduledTest.instances.values().iterator();
      ScheduledTest st;
      while (iter.hasNext()){
        st = iter.next();
        try{
          if (!Files.exists(scripts.resolve(st.getRelScriptPath()))){
            iter.remove();
          }
        }catch(Throwable t){
          Initializer.log(t);
        }
      }
      return true;
    }catch(Throwable t){
      Initializer.log(t);
    }
    return false;
  }
  /**
   * @return the output of the test, or {@code null} if there is no output.
   */
  public String getOutput(){
    final String cachedOutput = this.cachedOutput;
    return cachedOutput==null?getScriptOutputSafe(false):cachedOutput;
  }
  private String getScriptOutputSafe(boolean email){
    final Script scr = script;
    if (scr==null){
      return null;
    }
    try{
      return scr.getOutput(email);
    }catch(Throwable t){
      Initializer.log(t);
      return Utility.getStackTrace(t);
    }
  }
  public void submitAJAX(HttpServletRequest req, HttpServletResponse res) throws Throwable {
    final Script scr = script;
    if (scr==null){
      String err = "AJAX request submitted to inactive script.";
      res.sendError(404, err);
      //Initializer.log(new NullPointerException(err));
    }else{
      scr.updateAJAX(req,res);
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
    final String str = description;
    return str==null?"No description given.":str;
  }
  /**
   * @return a read-only set of parameter names for this script.
   */
  public Set<String> getParamNames(){
    return paramNames==null?Collections.emptySet():paramNames;
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
   * @param timeout in milliseconds specifies the maximum duration to wait for death. -1 means wait forever.
   * @return whether the test died within the specified timeout
   */
  public boolean waitForDeath(long timeout) throws InterruptedException {
    if (!running.get()){
      return true;
    }
    if (timeout==-1){
      while (running.get()){
        Thread.sleep(500L);
      }
      return true;
    }else{
      final long x = System.currentTimeMillis()+timeout;
      long d;
      while (running.get()){
        d = x-System.currentTimeMillis();
        if (d<=0){
          break;
        }
        Thread.sleep(Math.min(500L,d));
      }
      return !running.get();
    }
  }
  /**
   * Attempts to initiate a new test.
   * @param m specifies semantic tag mappings and equipment groups to use for this test.
   * @param threadCount is the maximum number of threads to activate for this test.
   * @param maxTests is the maximum percentage of program within each group that can be tested concurrently.
   * @param operator specifies the operator who initiated the test (for record keeping purposes).
   * @param schedule provided for hash validation and test completion callback. {@code null} values are acceptable.
   * @return whether test initiation was successful.
   */
  public boolean initiate(Mapping m, int threadCount, double maxTests, String operator, Map<String,Boolean> params, ScheduledTest schedule){
    if (!Initializer.isDying() && running.compareAndSet(false,true)){
      final long startTime = System.currentTimeMillis();
      boolean ret = true;
      boolean invokeTerminate = false;
      script = null;
      cachedOutput = null;
      lastParams = params;
      init:{
        try{
          kill = false;
          status = "Initiating";
          if (threadCount<1){
            threadCount = 1;
          }else if (threadCount>64){
            threadCount = 64;
          }
          if (maxTests<0){
            maxTests = 0;
          }else if (maxTests>1){
            maxTests = 1;
          }
          if (!Files.exists(scriptFile)){
            status = "Initialization error: Script file does not exist";
            ret = false;
            break init;
          }
          script = getScript();
          if (script==null){
            status = "Initialization error: Could not locate implementation of "+Script.class.getName();
            ret = false;
            break init;
          }
          script.test = this;
          script.mapping = m;
          script.params = params;
          script.schedule = schedule;
          if (kill){
            clearStatus();
            ret = false;
            break init;
          }
          if (schedule!=null){
            if (schedule.getScriptHash()!=hash){
              status = "Initialization error: Script hash does not match expected value.";
              ret = false;
              break init;
            }
            if (schedule.getMappingHash()!=m.hashCode()){
              status = "Initialization error: Mapping hash does not match expected value.";
              ret = false;
              break init;
            }
          }
          final Container<Boolean> autoReset = new Container<Boolean>(true);
          final Container<ConcurrentSkipListMap<String,ResolvedTestingUnit>> units = new Container<ConcurrentSkipListMap<String,ResolvedTestingUnit>>();
          {
            final TreeSet<String> requiredTags = new TreeSet<String>();
            autoReset.x = script.autoReset();
            script.requireTags(requiredTags);
            if (kill){
              clearStatus();
              ret = false;
              break init;
            }
            Initializer.getConnection().runReadAction(FieldAccessFactory.newDisabledFieldAccess(), new ReadAction(){
              public void execute(SystemAccess sys){
                units.x = m.resolve(sys.getTree(SystemTree.Geographic), requiredTags);
              }
            });
          }
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
          final GroupTracker[] groups = groupMap.values().toArray(new GroupTracker[]{});
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
          script.units = rtus;
          script.threads = threadCount;
          script.maxTestsPerGroup = mtest;
          script.testsTotal = total;
          script.testsStarted = numStarted;
          script.testsCompleted = numCompleted;
          script.init();
          invokeTerminate = true;
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
                        throw new NullPointerException("Fatal flaw detected in logic.");
                      }else{
                        try{
                          script.exec(rtu);
                        }catch(InterruptedException e){}catch(Throwable t){
                          Initializer.log(t);
                        }
                        if (autoReset.x){
                          while (true){
                            try{
                              rtu.reset(null);
                              break;
                            }catch(InterruptedException e){}
                          }
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
                    boolean doClear = false;
                    try{
                      script.exit();
                      doClear = true;
                    }catch(InterruptedException e){}catch(Throwable t){
                      Initializer.log(t);
                      status = "Termination error occurred.";
                    }
                    try{
                      final boolean pub = script.isArchivePublic();
                      cachedOutput = getScriptOutputSafe(false);
                      final ArchivedTest at = new ArchivedTest(name, operator, startTime, System.currentTimeMillis(), threads.length, mtest, pub, params);
                      threads = null;
                      if (cachedOutput!=null){
                        at.save(cachedOutput);
                      }
                      script.archivedTest = at;
                      if (schedule!=null && schedule.emails.size()>0){
                        String email = getScriptOutputSafe(true);
                        if (email!=null){
                          boolean csv = false;
                          try{
                            csv = script.isEmailCSV();
                          }catch(Throwable t){
                            Initializer.log(t);
                          }
                          schedule.onComplete(ExpansionUtils.nullifyLinks(email),csv);
                        }
                      }
                    }catch(Throwable t){
                      Initializer.log(t);
                    }
                    try{ script.close(); }catch(Throwable t){ Initializer.log(t); }
                    script = null;
                    if (doClear){
                      clearStatus();
                    }
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
          status = "Initialization error occurred.";
          Initializer.log(t);
          ret = false;
          break init;
        }
      }
      if (!ret){
        if (invokeTerminate){
          try{
            script.exit();
            cachedOutput = script.getOutput(false);
          }catch(InterruptedException e){}catch(Throwable t){
            Initializer.log(t);
          }
        }
        if (script!=null){
          try{ script.close(); }catch(Throwable t){ Initializer.log(t); }
          script = null;
        }
        running.set(false);
      }
      return ret;
    }
    return false;
  }
  public Script getScript() throws Throwable {
    if (jar){
      URLClassLoader cl = null;
      try{
        {
          MessageDigest md = MessageDigest.getInstance("MD5");
          byte[] buf = new byte[4096];
          try(
            DigestInputStream in = new DigestInputStream(new FileInputStream(scriptFile.toFile()), md);
          ){
            while (in.read(buf,0,buf.length)!=-1){}
          }
          buf = null;
          hash = Arrays.hashCode(md.digest());
        }
        String scriptClass;
        try(
          JarFile jarFile = new JarFile(scriptFile.toFile(),false);
        ){
          Manifest m = jarFile.getManifest();
          if (m==null){
            description = "Manifest is missing from jar file.";
            paramNames = null;
            return null;
          }
          scriptClass = m.getMainAttributes().getValue("Main-Class");
          if (scriptClass==null){
            description = "\"Main-Class\" header is missing from jar manifest.";
            paramNames = null;
            return null;
          }
          scriptClass = scriptClass.replace('\\','.').replace('/','.');
        }
        try{
          cl = new URLClassLoader(new URL[]{scriptFile.toUri().toURL()}, Test.class.getClassLoader());
        }catch(Throwable t){
          description = "Failed to create URLClassLoader.";
          paramNames = null;
          Initializer.log(t);
          return null;
        }
        Script s;
        try{
          s = cl.loadClass(scriptClass).asSubclass(Script.class).getDeclaredConstructor().newInstance();
        }catch(Throwable t){
          try{ cl.close(); }catch(Throwable tt){ Initializer.log(tt); }
          description = "Could not load main script class: "+Utility.escapeHTML(scriptClass);
          paramNames = null;
          Initializer.log(t);
          return null;
        }
        try{
          description = s.getDescription();
          String[] params = s.getParamNames();
          paramNames = params==null?null:Collections.unmodifiableSet(new TreeSet<String>(Arrays.asList(params)));
        }catch(Throwable t){
          description = "Failed to retrieve description and parameter names.";
          paramNames = null;
          Initializer.log(t);
        }
        return s;
      }catch(Throwable t){
        if (cl!=null){
          try{ cl.close(); }catch(Throwable tt){ Initializer.log(tt); }
        }
        description = "Failed to load script.";
        paramNames = null;
        throw t;
      }
    }else{
      try{
        final SimpleCompiler sc = new SimpleCompiler();
        final MessageDigest md = MessageDigest.getInstance("MD5");
        try(
          BufferedReader r = new BufferedReader(new InputStreamReader(new DigestInputStream(new FileInputStream(scriptFile.toFile()), md), java.nio.charset.StandardCharsets.UTF_8));
        ){
          sc.cook(name,r);
        }
        hash = Arrays.hashCode(md.digest());
        ClassLoader cl = sc.getClassLoader();
        org.codehaus.janino.util.ClassFile[] cfs = sc.getClassFiles();
        for (int i=0;i<cfs.length;++i){
          try{
            Script s = cl.loadClass(cfs[i].getThisClassName()).asSubclass(Script.class).getDeclaredConstructor().newInstance();
            try{
              description = s.getDescription();
              String[] params = s.getParamNames();
              paramNames = params==null?null:Collections.unmodifiableSet(new TreeSet<String>(Arrays.asList(params)));
            }catch(Throwable t){
              description = "Failed to retrieve description and parameter names.";
              paramNames = null;
              Initializer.log(t);
            }
            return s;
          }catch(Throwable t){}
        }
        description = "Could not locate implementation of "+Utility.escapeHTML(Script.class.getName())+" with no-argument constructor.";
        paramNames = null;
        return null;
      }catch(Throwable t){
        description = "Failed to compile script.";
        paramNames = null;
        throw t;
      }
    }
  }
}