import aces.webctrl.scripts.commissioning.core.*;
import aces.webctrl.scripts.commissioning.web.*;
import java.util.*;
import java.util.function.*;
import java.util.concurrent.atomic.*;
public class VAVBoxTester extends Script {
  private volatile boolean exited = false;
  private volatile Data[] data;
  private final AtomicInteger index = new AtomicInteger();
  private volatile boolean testDampers = false;
  private volatile boolean testFans = false;
  private volatile boolean testValves = false;
  private volatile boolean initialized = false;
  @Override public String getDescription(){
    return "Verify proper operation of fans, dampers, and heating.";
  }
  @Override public String[] getParamNames(){
    return new String[]{"Dampers", "Fans", "HW Valves"};
  }
  @Override public void exit(){
    exited = true;
  }
  @Override public void init(){
    data = new Data[this.testsTotal];
    Arrays.fill(data,null);
    testDampers = (Boolean)this.params.getOrDefault("Dampers",false);
    testFans = (Boolean)this.params.getOrDefault("Fans",false);
    testValves = (Boolean)this.params.getOrDefault("HW Valves",false);
    initialized = true;
  }
  @Override public void exec(ResolvedTestingUnit x) throws Throwable {
    final int index = this.index.getAndIncrement();
    if (index>=data.length){
      Initializer.log(new ArrayIndexOutOfBoundsException("Index exceeded expected capacity: "+index+">="+data.length));
      return;
    }
    data[index] = new Data(x);
  }
  @Override public String getOutput(boolean email) throws Throwable {
    if (!initialized){
      return null;
    }
    final ArrayList<Data> list = new ArrayList<Data>(data.length);
    Data d;
    for (int i=0;i<data.length;++i){
      d = data[i];
      if (d!=null){
        list.add(d);
      }
    }
    list.sort(null);
    final StringBuilder sb = new StringBuilder(2048);
    sb.append("<!DOCTYPE html>\n");
    sb.append("<html lang=\"en\">\n");
    sb.append("<head>\n");
    sb.append("<title>VAV Box Report</title>\n");
    if (email){
      sb.append("<style>\n");
      sb.append(ProviderCSS.getCSS());
      sb.append("\n</style>\n");
    }else{
      sb.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"main.css\"/>\n");
    }
    sb.append("</head>\n");
    sb.append("<body>\n");
    sb.append("<div class=\"c\">\n");

    sb.append("</div>\n");
    if (!exited){
      sb.append("<script>\n");
      sb.append("setTimeout(()=>{window.location.reload();}, 1000);");
      sb.append("</script>\n");
    }
    sb.append("</body>\n");
    sb.append("</html>");
    return sb.toString();
  }
  private final static String tagAirflow = "airflow";
  private final static String tagDamperLockFlag = "damper_lock_flag";
  private final static String tagDamperLockValue = "damper_lock_value";
  private final static String tagDamperPosition = "damper_position";
  private final static String tagEAT = "eat";
  private final static String tagLAT = "lat";
  private final static String tagSFSSLockFlag = "sfss_lock_flag";
  private final static String tagSFSSLockValue = "sfss_lock_value";
  private final static String tagSFST = "sfst";
  private final static String tagValveLockFlag = "hwv_lock_flag";
  private final static String tagValveLockValue = "hwv_lock_value";
  private final static String tagValvePosition = "hwv_position";
  class Data implements Comparable<Object> {
    public volatile ResolvedTestingUnit x;
    public volatile int group;
    public volatile String path;
    public volatile String link;
    public volatile long start = -1;
    public volatile long end = -1;
    public volatile boolean hasFan;
    public volatile boolean hasDamper;
    public volatile boolean hasValve;
    public volatile boolean fanError = false;
    public volatile boolean fanStopTest = false;
    public volatile boolean fanStartTest = false;
    public volatile boolean damperError = false;
    public volatile Stats[] airflow;
    public volatile boolean valveError = false;
    public volatile double[] heating;
    @Override public int compareTo(Object obj){
      if (obj instanceof Data){
        Data t = (Data)obj;
        if (group==t.group){
          return path.compareTo(t.path);
        }else{
          return group-t.group;
        }
      }else{
        return -1;
      }
    }
    public Data(ResolvedTestingUnit x) throws Throwable {
      start = System.currentTimeMillis();
      this.x = x;
      group = x.getGroup();
      path = x.getDisplayPath();
      link = x.getPersistentLink();
      Boolean b;
      String s,t;
      Stats st;
      hasFan = testFans && x.hasMapping(tagSFSSLockFlag) && x.hasMapping(tagSFSSLockValue) && x.hasMapping(tagSFST);
      hasDamper = testDampers && x.hasMapping(tagAirflow) && x.hasMapping(tagDamperLockFlag) && x.hasMapping(tagDamperLockValue) && x.hasMapping(tagDamperPosition);
      hasValve = testValves && x.hasMapping(tagValveLockFlag) && x.hasMapping(tagValveLockValue) && x.hasMapping(tagValvePosition) && x.hasMapping(tagEAT) && x.hasMapping(tagLAT);
      if (hasFan) fanTest: {
        if (x.markAndSetValue(tagSFSSLockValue, 0)==null || x.markAndSetValue(tagSFSSLockFlag, true)==null){
          fanError = true; break fanTest;
        }
        b = waitFor(90000, 5000, tagSFST, new Predicate<String>(){
          public boolean test(String s){
            return s.equals("0");
          }
        });
        if (b==null){
          fanError = true; break fanTest;
        }
        fanStopTest = b;
        if (!x.setValue(tagSFSSLockValue, 1)){
          fanError = true; break fanTest;
        }
        b = waitFor(90000, 5000, tagSFST, new Predicate<String>(){
          public boolean test(String s){
            return s.equals("1");
          }
        });
        if (b==null){
          fanError = true; break fanTest;
        }
        fanStartTest = b;
      }
      if (hasDamper) damperTest: {
        try{
          airflow = new Stats[11];
          for (int i=0;i<airflow.length;++i){
            final int pos = 10*i;
            if (i==0){
              if (x.markAndSetValue(tagDamperLockValue, pos)==null || x.markAndSetValue(tagDamperLockFlag, true)==null){
                damperError = true; break damperTest;
              }
            }else{
              if (!x.setValue(tagDamperLockValue, pos)){
                damperError = true; break damperTest;
              }
            }
            b = waitFor(120000, 5000, tagDamperPosition, new Predicate<String>(){
              public boolean test(String s){
                return Math.abs(Double.parseDouble(s)-pos)<1;
              }
            });
            if ((st = evaluate(10, 3000, tagAirflow))==null){
              damperError = true; break damperTest;
            }
            airflow[i] = st;
          }
        }catch(NumberFormatException e){
          damperError = true;
        }
      }
      if (hasValve) valveTest: {
        try{
          if ((s=x.getValue(tagValvePosition))==null){
            valveError = true; break valveTest;
          }
          final double initialPosition = Double.parseDouble(s);
          if (x.markAndSetValue(tagValveLockValue,0)==null || x.markAndSetValue(tagValveLockFlag,true)==null){
            valveError = true; break valveTest;
          }
          Thread.sleep(10000L+(long)(initialPosition*6000));
          if ((s=x.getValue(tagEAT))==null || (t=x.getValue(tagLAT))==null){
            valveError = true; break valveTest;
          }
          heating = new double[61];
          heating[0] = Double.parseDouble(t)-Double.parseDouble(s);
          if (x.markAndSetValue(tagValveLockValue,100)==null){
            valveError = true; break valveTest;
          }
          for (int i=1;i<heating.length;++i){
            Thread.sleep(10000L);
            if ((s=x.getValue(tagEAT))==null || (t=x.getValue(tagLAT))==null){
              valveError = true; break valveTest;
            }
            heating[i] = Double.parseDouble(t)-Double.parseDouble(s);
          }
        }catch(NumberFormatException e){
          valveError = true;
        }
      }
      end = System.currentTimeMillis();
    }
    private Boolean waitFor(long timeout, long interval, String tag, Predicate<String> test) throws Throwable {
      final long lim = System.currentTimeMillis()+timeout;
      String s;
      do {
        if ((s=x.getValue(tag))==null){
          return null;
        }
        if (test.test(s)){
          return true;
        }
        if (System.currentTimeMillis()>=lim){
          break;
        }
        Thread.sleep(interval);
      } while (System.currentTimeMillis()<lim);
      return false;
    }
    private Stats evaluate(int times, long interval, String tag) throws Throwable {
      final double[] arr = new double[times];
      String s;
      for (int i=0;i<times;++i){
        Thread.sleep(interval);
        if ((s=x.getValue(tag))==null){
          return null;
        }
        arr[i] = Double.parseDouble(s);
      }
      return new Stats(arr);
    }
  }
}
class Stats {
  public volatile double[] arr;
  public volatile double mean = 0;
  public volatile double absoluteDeviation = 0;
  public Stats(double... arr){
    this.arr = arr;
    if (arr.length>0){
      for (double x:arr){
        mean+=x;
      }
      mean/=arr.length;
      for (double x:arr){
        absoluteDeviation+=Math.abs(x-mean);
      }
      absoluteDeviation/=arr.length;
    }
  }
}