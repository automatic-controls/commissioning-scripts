package aces.webctrl.scripts.commissioning.core;
import com.controlj.green.core.email.*;
import org.springframework.scheduling.support.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.nio.*;
import java.nio.file.*;
import java.nio.channels.*;
import javax.activation.*;
import java.io.*;
/**
 * Uses cron expressions to control scheduled execution of scripts.
 * Upon test completion, reports may be automatically emailed to a list of addresses using the default WebCTRL mail server settings.
 */
public class ScheduledTest {
  public volatile static Path dataFile = null;
  public final static ConcurrentSkipListMap<Integer,ScheduledTest> instances = new ConcurrentSkipListMap<Integer,ScheduledTest>();
  private final static AtomicInteger nextID = new AtomicInteger();
  public final int ID = nextID.getAndIncrement();
  private volatile int mappingHash = 0;
  private volatile int scriptHash = 0;
  private volatile String relScriptPath = null;
  private volatile String operator = "UNKNOWN";
  private volatile int threads = 1;
  private volatile double maxTests = 0;
  private volatile String mappingName = "";
  private volatile String emailSubject = "";
  /** List of emails to send completed reports to. */
  public final ConcurrentSkipListSet<String> emails = new ConcurrentSkipListSet<String>();
  /** List of emails to CC completed reports to. */
  public final ConcurrentSkipListSet<String> emailsCC = new ConcurrentSkipListSet<String>();
  /** Map of script initialization parameters. */
  public final ConcurrentSkipListMap<String,Boolean> params = new ConcurrentSkipListMap<String,Boolean>();
  private volatile String expr = null;
  private volatile CronSequenceGenerator cron = null;
  private volatile long nextRunTime = -1L;
  /**
   * Creates a new scheduled test using the given relative script path.
   */
  public ScheduledTest(Path script) throws IllegalArgumentException {
    setRelScriptPath(script);
    instances.put(ID,this);
  }
  /**
   * Creates a new scheduled test using the given relative script path.
   */
  public ScheduledTest(String relScriptPath){
    setRelScriptPath(relScriptPath);
    instances.put(ID,this);
  }
  /**
   * Used when creating a temporary instance that may be deleted soon after.
   */
  public ScheduledTest(){
    relScriptPath = "";
  }
  /**
   * Saves all scheduled test to the data file.
   */
  public synchronized static boolean saveAll(){
    ByteBuffer buf = ByteBuffer.wrap(serializeAll());
    try(
      FileChannel out = FileChannel.open(dataFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    ){
      while (buf.hasRemaining()){
        out.write(buf);
      }
      return true;
    }catch(Throwable t){
      Initializer.log(t);
      return false;
    }
  }
  /**
   * Load all scheduled tests from the data file.
   */
  public synchronized static boolean loadAll(){
    try{
      if (Files.exists(dataFile)){
        deserializeAll(new SerializationStream(Files.readAllBytes(dataFile)));
      }
      return true;
    }catch(Throwable t){
      Initializer.log(t);
      return false;
    }
  }
  /**
   * Serializes all scheduled tests into a byte array.
   */
  public static byte[] serializeAll(){
    ByteBuilder bb = new ByteBuilder(instances.size()<<5);
    for (ScheduledTest st:instances.values()){
      st.serialize(bb);
    }
    return bb.compute();
  }
  /**
   * Reads all data from the given stream, and resolves the data into various scheduled test objects.
   */
  public static void deserializeAll(SerializationStream s){
    while (!s.end()){
      deserialize(s);
    }
  }
  /**
   * Serializes this scheduled test into the given byte builder.
   */
  public void serialize(final ByteBuilder bb){
    bb.write(relScriptPath);
    bb.write(scriptHash);
    bb.write(mappingName);
    bb.write(mappingHash);
    bb.write(operator);
    bb.write(threads);
    bb.write(maxTests);
    bb.write(expr);
    bb.write(emailSubject);
    Set<String> emails = this.emails.clone();
    bb.write(emails.size());
    for (String str:emails){
      bb.write(str);
    }
    emails = this.emailsCC.clone();
    bb.write(emails.size());
    for (String str:emails){
      bb.write(str);
    }
    Map<String,Boolean> params = this.params.clone();
    bb.write(params.size());
    params.forEach(new java.util.function.BiConsumer<String,Boolean>(){
      public void accept(String name, Boolean val){
        bb.write(name);
        bb.write(val);
      }
    });
  }
  /**
   * Deserializes a single scheduled test from the given stream.
   */
  public static ScheduledTest deserialize(SerializationStream s){
    final ScheduledTest st = new ScheduledTest(s.readString());
    st.scriptHash = s.readInt();
    st.setMappingName(s.readString());
    st.mappingHash = s.readInt();
    st.setOperator(s.readString());
    st.setThreads(s.readInt());
    st.setMaxTests(s.readDouble());
    st.setCronExpression(s.readString());
    st.setEmailSubject(s.readString());
    int i;
    int len = s.readInt();
    for (i=0;i<len;++i){
      st.emails.add(s.readString());
    }
    len = s.readInt();
    for (i=0;i<len;++i){
      st.emailsCC.add(s.readString());
    }
    len = s.readInt();
    for (i=0;i<len;++i){
      st.params.put(s.readString(), s.readBoolean());
    }
    return st;
  }
  /**
   * Resets the next run time of this scheduled test.
   */
  public void reset(){
    CronSequenceGenerator cron = this.cron;
    if (cron==null){
      nextRunTime = -1;
    }else{
      try{
        nextRunTime = cron.next(new Date()).getTime();
      }catch(Throwable t){
        nextRunTime = -1;
      }
    }
  }
  /**
   * @return the cron expression used for scheduling purposes.
   */
  public String getCronExpression(){
    return expr;
  }
  /**
   * Sets the cron expression used for scheduling purposes.
   * @return {@code true} on success; {@code false} if the given expression cannot be parsed.
   */
  public boolean setCronExpression(String expr){
    if (expr.equals(this.expr)){
      return true;
    }else{
      this.expr = expr;
      try{
        cron = new CronSequenceGenerator(expr);
        return true;
      }catch(Throwable t){
        cron = null;
        return false;
      }finally{
        reset();
      }
    }
  }
  /**
   * @return the next run time of this test. If {@code -1}, then this test should not ever be auto-executed.
   */
  public long getNext(){
    return nextRunTime;
  }
  /**
   * @return the next run time of this test as a {@code String}.
   */
  public String getNextString(){
    long nextRunTime = this.nextRunTime;
    return nextRunTime==-1?"None":Utility.getDateString(nextRunTime);
  }
  /**
   * @return the relative script path from the root script folder.
   */
  public String getRelScriptPath(){
    return relScriptPath;
  }
  /**
   * Sets the relative script path.
   */
  public void setRelScriptPath(String relScriptPath){
    this.relScriptPath = relScriptPath==null?"":relScriptPath;
  }
  /**
   * Sets the relative script path.
   */
  public void setRelScriptPath(Path script) throws IllegalArgumentException {
    relScriptPath = Initializer.getScriptFolder().relativize(script).toString();
  }
  /**
   * @return the name used to look-up {@code Mapping} objects.
   */
  public String getMappingName(){
    return mappingName;
  }
  /**
   * Sets the name used to look-up {@code Mapping} objects.
   */
  public void setMappingName(String mappingName){
    this.mappingName = mappingName;
  }
  /**
   * @return the email subject for invokations of {@link #onComplete(String)}.
   */
  public String getEmailSubject(){
    return emailSubject;
  }
  /**
   * Sets the email subject for invokations of {@link #onComplete(String)}.
   */
  public void setEmailSubject(String str){
    emailSubject = str==null?"":str;
  }
  /**
   * @return the operator string for who last modified this scheduled test.
   */
  public String getOperator(){
    return operator;
  }
  /**
   * Sets the operator string for who last modified this scheduled test.
   * Used for record-keeping purposes.
   */
  public void setOperator(String operator){
    this.operator = operator;
  }
  /**
   * @return the maximum number of threads to use for this test.
   */
  public int getThreads(){
    return threads;
  }
  /**
   * Sets the maximum number of threads to use for this test.
   */
  public void setThreads(int threads){
    if (threads<1){
      threads = 1;
    }
    this.threads = threads;
  }
  /**
   * @return the maximum percentage of simutaneous test to be running at any given time within an equipment group.
   */
  public double getMaxTests(){
    return maxTests;
  }
  /**
   * Sets the maximum percentage of simutaneous test to be running at any given time within an equipment group.
   */
  public void setMaxTests(double maxTests){
    if (maxTests<0){
      maxTests = 0;
    }else if (maxTests>1){
      maxTests = 1;
    }
    this.maxTests = maxTests;
  }
  /**
   * Initiates all scheduled test that are ready to execute.
   */
  public static void execAll(){
    for (ScheduledTest st:instances.values()){
      st.execIfReady();
    }
  }
  /**
   * Initiates this scheduled test if it is ready according to the cron expression scheduler.
   * @return the started {@code Test}, or {@code null} if a test could not be started.
   */
  public Test execIfReady(){
    final long nextRunTime = this.nextRunTime;
    if (nextRunTime!=-1 && nextRunTime<=System.currentTimeMillis()){
      return exec();
    }else{
      return null;
    }
  }
  /**
   * Initiates this scheduled test.
   * @return the started {@code Test}, or {@code null} if a test could not be started.
   */
  public Test exec(){
    reset();
    Mapping m = getMapping();
    if (m==null){ return null; }
    Test s = getScript();
    if (s==null){ return null; }
    return s.initiate(m,threads,maxTests,operator+" (Scheduled)",Collections.unmodifiableMap(params.clone()),this)?s:null;
  }
  /**
   * Sends an email to people listed for this scheduled test.
   * @param data specifies the email contents.
   * @return whether an email was sent successfully.
   */
  public boolean onComplete(final String data, boolean csv){
    try{
      String[] emails = this.emails.toArray(new String[]{});
      if (emails.length==0){
        return false;
      }
      String[] emailsCC = this.emailsCC.toArray(new String[]{});
      EmailParametersBuilder pb = EmailServiceFactory.createParametersBuilder();
      pb.withSubject(emailSubject);
      pb.withToRecipients(emails);
      if (emailsCC.length>0){
        pb.withCcRecipients(emailsCC);
      }
      if (csv){
        pb.withMessageContents("This is an automated report. Refer to the attached CSV file.");
        pb.withMessageMimeType("text/plain");
      }else{
        pb.withMessageContents(data);
        pb.withMessageMimeType("text/html");
      }
      EmailParameters ep = pb.build();
      if (csv){
        AttachmentInfo info = new AttachmentInfo();
        info.setAttachment(new DataSource(){
          private final byte[] arr = data.getBytes(java.nio.charset.StandardCharsets.UTF_8);
          public String getContentType(){
            return "text/csv";
          }
          public String getName(){
            return "report.csv";
          }
          public OutputStream getOutputStream() throws IOException {
            throw new IOException("An OutputStream could not be created for this DataSource.");
          }
          public InputStream getInputStream(){
            return new ByteArrayInputStream(arr);
          }
        });
        info.setMimeType("csv");
        info.setName("report.csv");
        ep.addAttachment(info);
      }
      EmailServiceFactory.getService().sendEmail(ep);
      return true;
    }catch(Throwable t){
      Initializer.log(t);
      return false;
    }
  }
  /**
   * Recomputes mapping and script hashes used for validation.
   */
  public void recomputeHashes(){
    Mapping m = getMapping();
    mappingHash = m==null?0:m.hashCode();
    Test t = getScript();
    if (t==null){
      scriptHash = 0;
    }else{
      try(
        Script script = t.getScript(); 
      ){
        scriptHash = script==null?0:t.hashCode();
      }catch(Throwable e){
        scriptHash = 0;
      }
    }
  }
  /**
   * @return whether the script and mapping hashes were successfully validated.
   */
  public boolean validateHashes(){
    Mapping m = getMapping();
    if (m==null || mappingHash!=m.hashCode()){
      return false;
    }
    Test t = getScript();
    if (t==null){
      return false;
    }else{
      try(
        Script script = t.getScript();
      ){
        if (script==null || scriptHash!=t.hashCode()){
          return false;
        }
      }catch(Throwable e){
        return false;
      }
    }
    return true;
  }
  /**
   * @return the {@code Mapping} object corresponding to this scheduled test; or {@code null} upon search failure.
   */
  public Mapping getMapping(){
    final String str = mappingName;
    String name;
    for (Mapping m:Mapping.instances.values()){
      name = m.getName();
      if (name!=null && name.equals(str)){
        return m;
      }
    }
    return null;
  }
  /**
   * @return the {@code Test} object corresponding to the script used in this scheduled test; or {@code null} upon search failure.
   */
  public Test getScript(){
    try{
      final Path p = Initializer.getScriptFolder().resolve(relScriptPath);
      for (Test t:Test.instances.values()){
        if (Files.isSameFile(p,t.getScriptFile())){
          Set<String> names = t.getParamNames();
          NavigableSet<String> set = params.keySet();
          Iterator<String> iter = set.iterator();
          while (iter.hasNext()){
            if (!names.contains(iter.next())){
              iter.remove();
            }
          }
          for (String name:names){
            if (!set.contains(name)){
              params.put(name,false);
            }
          }
          return t;
        }
      }
    }catch(Throwable t){
      Initializer.log(t);
    }
    return null;
  }
  /**
   * @return a hash used to verify semantic tag mappings before running this scheduled test.
   */
  public int getMappingHash(){
    return mappingHash;
  }
  /**
   * @return a hash used to verify script contents before running this scheduled test.
   */
  public int getScriptHash(){
    return scriptHash;
  }
}