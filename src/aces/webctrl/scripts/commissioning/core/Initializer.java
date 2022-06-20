package aces.webctrl.scripts.commissioning.core;
import com.controlj.green.addonsupport.*;
import com.controlj.green.addonsupport.access.*;
import javax.servlet.*;
import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;
public class Initializer implements ServletContextListener {
  /** Contains basic informatin about this addon. */
  public volatile static AddOnInfo info = null;
  /** The name of this addon */
  private volatile static String name;
  /** Prefix used for constructing relative URL paths */
  private volatile static String prefix;
  /** Used for logging status messages. */
  private volatile static FileLogger logger;
  /** Path to the private data folder for this addon. */
  private volatile static Path data;
  /** Path to the folder containing scripts. */
  private volatile static Path scriptFolder;
  /** Root system connection to the database. */
  private volatile static SystemConnection con;
  /** Queue for code that must be executed within a write action (with field access) */
  private final static ConcurrentLinkedQueue<WriteAction> writeQueries = new ConcurrentLinkedQueue<WriteAction>();
  /** Queue for code that must be executed within a read action (with field access) */
  private final static ConcurrentLinkedQueue<ReadAction> readQueries = new ConcurrentLinkedQueue<ReadAction>();
  /** Used to poke the dataQueryThread along */
  private final static Object queryNotifier = new Object();
  /** Thread used for all database query operations. */
  private volatile static Thread dataQueryThread;
  /** Flag used to terminate the application */
  private volatile static boolean kill = false;
  /** The maximum duration of time to lock the database for read actions. */
  private final static long maxReadLock = 1000L;
  /** The maximum duration of time to lock the database for write actions. */
  private final static long maxWriteLock = 1000L;
  // Temporary workaround until ALC releases a patch for WebCTRL
  static {
    try{
      Class.forName("com.controlj.green.directaccess.DirectAccessInternal").getMethod("getDirectAccessInternal").invoke(null);
      com.controlj.green.addonsupport.access.DirectAccess.getDirectAccess();
    }catch (Throwable t){}
  }
  /**
   * Loads data and starts the database query processing thread.
   */
  @Override public void contextInitialized(ServletContextEvent sce){
    info = AddOnInfo.getAddOnInfo();
    name = info.getName();
    prefix = '/'+name+'/';
    logger = info.getDateStampLogger();
    con = DirectAccess.getDirectAccess().getRootSystemConnection();
    data = info.getPrivateDir().toPath();
    ArchivedTest.dataFolder = data.resolve("test_archive");
    ArchivedTest.mainDataFile = data.resolve("archive_index");
    Mapping.dataFile = data.resolve("mappings");
    scriptFolder = data.resolve("scripts");
    try{
      if (!Files.exists(ArchivedTest.dataFolder)){
        Files.createDirectory(ArchivedTest.dataFolder);
      }
    }catch(Throwable t){
      log(t);
    }
    try{
      if (!Files.exists(scriptFolder)){
        Files.createDirectory(scriptFolder);
      }
    }catch(Throwable t){
      log(t);
    }
    ArchivedTest.loadAll();
    Mapping.loadAll();
    try{
      Files.walkFileTree(scriptFolder, new SimpleFileVisitor<Path>(){
        @Override public FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs) throws java.io.IOException {
          Objects.requireNonNull(file);
          Objects.requireNonNull(attrs);
          new Test(file);
          return FileVisitResult.CONTINUE;
        }
      });
    }catch(Throwable t){
      log(t);
    }
    dataQueryThread = new Thread(){
      public void run(){
        try{
          final FieldAccess fieldAccess = FieldAccessFactory.newFieldAccess();
          final Container<InterruptedException> interrupt = new Container<InterruptedException>();
          while (true){
            if (kill){ return; }
            synchronized (queryNotifier){
              while (writeQueries.isEmpty() && readQueries.isEmpty()){
                queryNotifier.wait(1000);
                if (kill){ return; }
              }
            }
            while (!readQueries.isEmpty()){
              con.runReadAction(fieldAccess, new ReadAction(){
                public void execute(SystemAccess sys){
                  try{
                    long lim = System.currentTimeMillis()+maxReadLock;
                    ReadAction r;
                    do {
                      r = readQueries.poll();
                      if (kill || r==null){
                        return;
                      }else{
                        try{
                          r.execute(sys);
                        }catch(InterruptedException e){
                          interrupt.x = e;
                          return;
                        }catch(Throwable t){
                          log(t);
                        }
                      }
                    } while (System.currentTimeMillis()<lim);
                  }catch(Throwable t){
                    log(t);
                  }
                }
              });
              if (interrupt.x!=null){
                throw interrupt.x;
              }
              if (kill){ return; }
            }
            while (!writeQueries.isEmpty()){
              con.runWriteAction(fieldAccess, "Nodes modified as result of executed commissioning script.", new WriteAction(){
                public void execute(WritableSystemAccess sys){
                  try{
                    long lim = System.currentTimeMillis()+maxWriteLock;
                    WriteAction r;
                    do {
                      r = writeQueries.poll();
                      if (kill || r==null){
                        return;
                      }else{
                        try{
                          r.execute(sys);
                        }catch(InterruptedException e){
                          interrupt.x = e;
                          return;
                        }catch(Throwable t){
                          log(t);
                        }
                      }
                    } while (System.currentTimeMillis()<lim);
                  }catch(Throwable t){
                    log(t);
                  }
                }
              });
              if (interrupt.x!=null){
                throw interrupt.x;
              }
              if (kill){ return; }
            }
          }
        }catch(InterruptedException e){}catch(Throwable t){
          log(t);
        }
      }
    };
    dataQueryThread.start();
  }
  /**
   * Saves data and terminates all spawned threads in an orderly manner.
   */
  @Override public void contextDestroyed(ServletContextEvent sce){
    try{
      kill = true;
      dataQueryThread.interrupt();
      Collection<Test> tests = Test.instances.values();
      for (Test t:tests){
        t.kill();
      }
      dataQueryThread.join();
      for (Test t:tests){
        t.waitForDeath();
      }
      ArchivedTest.saveAll();
      Mapping.saveAll();
    }catch(Throwable t){
      log(t);
    }
  }
  /**
   * Enqueues the given write action to be executed on the main query thread.
   */
  public static void enqueue(WriteAction r){
    synchronized (queryNotifier){
      writeQueries.add(r);
      queryNotifier.notifyAll();
    }
  }
  /**
   * Enqueues the given read action to be executed on the main query thread.
   */
  public static void enqueue(ReadAction r){
    synchronized (queryNotifier){
      readQueries.add(r);
      queryNotifier.notifyAll();
    }
  }
  /**
   * @return the directory where data should be saved.
   */
  public static Path getDataFolder(){
    return data;
  }
  /**
   * @return the root system connection used by this application.
   */
  public static SystemConnection getConnection(){
    return con;
  }
  /**
   * Logs the given message.
   */
  public static void log(String str){
    logger.println(str);
  }
  /**
   * Logs the given error.
   */
  public static void log(Throwable t){
    logger.println(t);
  }
  /** @return the name of this application. */
  public static String getName(){
    return name;
  }
  /**
   * @return the prefix to use for constructing relative URL paths.
   */
  public static String getPrefix(){
    return prefix;
  }
  /**
   * @return whether the kill flag has been set.
   */
  public static boolean isDying(){
    return kill;
  }
}