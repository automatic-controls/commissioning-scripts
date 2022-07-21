package aces.webctrl.scripts.commissioning.core;
import java.nio.*;
import java.nio.file.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
public class ArchivedTest {
  private final static AtomicInteger nextID = new AtomicInteger();
  public final static ConcurrentSkipListMap<Integer,ArchivedTest> instances = new ConcurrentSkipListMap<Integer,ArchivedTest>();
  public final int ID = nextID.getAndIncrement();
  public volatile static Path mainDataFile;
  public volatile static Path dataFolder;
  private volatile Path dataFile = null;
  private volatile String scriptName;
  private volatile String operator;
  private volatile long startTime;
  private volatile long endTime;
  private volatile int threads;
  private volatile double maxTests;
  private volatile Map<String,Boolean> params;
  public String getScriptName(){
    return scriptName;
  }
  public String getOperator(){
    return operator;
  }
  public long getStart(){
    return startTime;
  }
  public long getEnd(){
    return endTime;
  }
  public int getThreads(){
    return threads;
  }
  public double getMaxTests(){
    return maxTests;
  }
  public Map<String,Boolean> getParams(){
    return params;
  }
  public synchronized static boolean saveAll(){
    ByteBuffer buf = ByteBuffer.wrap(serializeAll());
    try(
      FileChannel out = FileChannel.open(mainDataFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
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
  public synchronized static boolean loadAll(){
    try{
      if (Files.exists(mainDataFile)){
        deserializeAll(new SerializationStream(Files.readAllBytes(mainDataFile)));
      }
      return true;
    }catch(Throwable t){
      Initializer.log(t);
      return false;
    }
  }
  public static byte[] serializeAll(){
    ByteBuilder b = new ByteBuilder(instances.size()<<3);
    for (ArchivedTest at:instances.values()){
      at.serialize(b);
    }
    return b.compute();
  }
  public static void deserializeAll(SerializationStream s){
    while (!s.end()){
      deserialize(s);
    }
  }
  public void serialize(ByteBuilder b){
    b.write(scriptName);
    b.write(operator);
    b.write(startTime);
    b.write(endTime);
    b.write(threads);
    b.write(maxTests);
    b.write(params.size());
    params.forEach(new java.util.function.BiConsumer<String,Boolean>(){
      public void accept(String name, Boolean val){
        b.write(name);
        b.write(val);
      }
    });
  }
  public static ArchivedTest deserialize(SerializationStream s){
    final String scriptName = s.readString();
    final String operator = s.readString();
    final long startTime = s.readLong();
    final long endTime = s.readLong();
    final int threads = s.readInt();
    final double maxTests = s.readDouble();
    final int len = s.readInt();
    final TreeMap<String,Boolean> params = new TreeMap<String,Boolean>();
    for (int i=0;i<len;++i){
      params.put(s.readString(), s.readBoolean());
    }
    return new ArchivedTest(scriptName,operator,startTime,endTime,threads,maxTests,params);
  }
  public ArchivedTest(String scriptName, String operator, long startTime, long endTime, int threads, double maxTests, Map<String,Boolean> params){
    try{
      dataFile = dataFolder.resolve(scriptName+'_'+String.valueOf(startTime));
    }catch(Throwable t){
      Initializer.log(t);
    }
    this.scriptName = scriptName;
    this.operator = operator;
    this.startTime = startTime;
    this.endTime = endTime;
    this.threads = threads;
    this.maxTests = maxTests;
    this.params = Collections.unmodifiableMap(params);
    instances.put(ID,this);
  }
  public synchronized String load(){
    if (dataFile==null){
      return "NullPointerException";
    }
    try{
      return Files.exists(dataFile)?new String(Files.readAllBytes(dataFile), java.nio.charset.StandardCharsets.UTF_8):"Could not locate file.";
    }catch(Throwable t){
      Initializer.log(t);
      return "An error occurred while loading the data file.";
    }
  }
  public synchronized boolean save(String document){
    ByteBuffer buf = ByteBuffer.wrap(document.getBytes(java.nio.charset.StandardCharsets.UTF_8));
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
}