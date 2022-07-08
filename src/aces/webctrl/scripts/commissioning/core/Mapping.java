package aces.webctrl.scripts.commissioning.core;
import com.controlj.green.addonsupport.access.*;
import java.util.*;
import java.util.regex.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.nio.channels.*;
public class Mapping {
  public volatile static Path dataFile;
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
   * Stores group names corresponding to each group index.
   */
  public final ConcurrentSkipListMap<Integer,String> groupNames = new ConcurrentSkipListMap<Integer,String>();
  /**
   * A name for this grouping.
   */
  private volatile String name;
  @Override public int hashCode(){
    Container<Integer> hash = new Container<Integer>(name.hashCode());
    tags.forEach(new java.util.function.BiConsumer<String,SemanticTag>(){
      public void accept(String str, SemanticTag tag){
        hash.x = hash.x*31+tag.hashCode();
      }
    });
    equipment.forEach(new java.util.function.BiConsumer<String,TestingUnit>(){
      public void accept(String str, TestingUnit tu){
        hash.x = hash.x*31+tu.hashCode();
      }
    });
    groupNames.forEach(new java.util.function.BiConsumer<Integer,String>(){
      public void accept(Integer index, String name){
        hash.x = hash.x*31+(index^name.hashCode());
      }
    });
    return hash.x;
  }
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
   * Serializes all mappings.
   */
  public static byte[] serializeAll(){
    ByteBuilder b = new ByteBuilder(instances.size()<<7);
    for (Mapping m:instances.values()){
      m.serialize(b);
    }
    return b.compute();
  }
  /**
   * Deserializes all mappings.
   */
  public static void deserializeAll(SerializationStream s){
    while (!s.end()){
      deserialize(s);
    }
  }
  /**
   * Serializes a single mapping.
   */
  public void serialize(ByteBuilder b){
    b.write(name);
    {
      final Collection<SemanticTag> tags = new ArrayList<SemanticTag>(this.tags.values());
      b.write(tags.size());
      for (SemanticTag st:tags){
        st.serialize(b);
      }
    }
    {
      final ArrayList<TestingUnit> equipment = new ArrayList<TestingUnit>(this.equipment.values());
      equipment.sort(null);
      final int len = equipment.size();
      b.write(len);
      int i=0, j, bound=0, group=-1;
      for (;;){
        if (i>=bound){
          if (i==len){
            if (bound==len){
              break;
            }
            j = group+1;
          }else{
            j = equipment.get(i).getGroup();
          }
          if (i==bound){
            group = j;
          }else if (group!=j){
            j = bound;
            bound = i;
            i = j;
            b.write(groupNames.get(group));
            b.write(group);
            b.write(bound-i);
            continue;
          }
        }else{
          b.write(equipment.get(i).getID());
        }
        ++i;
      }
    }
  }
  /**
   * Deserializes a single mapping.
   */
  public static Mapping deserialize(SerializationStream s){
    Mapping m = new Mapping(s.readString());
    int i;
    SemanticTag st;
    int len = s.readInt();
    for (i=0;i<len;++i){
      try{
        st = SemanticTag.deserialize(s);
        m.tags.put(st.getTag(),st);
      }catch(PatternSyntaxException e){}
    }
    TestingUnit tu;
    len = s.readInt();
    i = 0;
    int subLen,j,grp;
    String name;
    while (i<len){
      name = s.readString();
      grp = s.readInt();
      subLen = s.readInt();
      m.groupNames.put(grp,name);
      i+=subLen;
      for (j=0;j<subLen;++j){
        tu = new TestingUnit(s.readString(),grp);
        m.equipment.put(tu.getID(),tu);
      }
    }
    return m;
  }
  /**
   * Constructs a new mapping.
   */
  public Mapping(String name){
    instances.put(ID,this);
    setName(name);
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
   * Two mappings cannot share the same name.
   * In the case of duplicates, a suffix _# is appended.
   * @return the new name for this mapping.
   */
  public String setName(final String name){
    if (name==null){ return this.name; }
    int suffix = 1;
    String tryName = name;
    boolean found;
    while (true){
      found = true;
      for (Mapping m:instances.values()){
        if (m!=this && m.getName().equals(tryName)){
          found = false;
          break;
        }
      }
      if (found){
        final String prev = this.name;
        this.name = tryName;
        for (ScheduledTest st:ScheduledTest.instances.values()){
          if (prev.equals(st.getMappingName())){
            st.setMappingName(tryName);
          }
        }
        return tryName;
      }
      tryName = name+'_'+(++suffix);
    }
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