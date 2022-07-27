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
    return "Verify proper operation of fans, dampers, and heating on VAV boxes.";
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
    {
      Data d;
      for (int i=0;i<data.length;++i){
        d = data[i];
        if (d!=null){
          list.add(d);
        }
      }
    }
    list.sort(null);
    final StringBuilder sb = new StringBuilder(8192);
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
    sb.append("<script>\n");
    sb.append("function scatter(parent, w, h, xData, yData, xFit, yFit, xSuffix, ySuffix){\n");
    sb.append("if (xData.length!==yData.length || xFit.length!==yFit.length){\n");
    sb.append("console.log(\"Error: Data length mismatch.\");\n");
    sb.append("return undefined;\n");
    sb.append("}\n");
    sb.append("if (xData.length===0){\n");
    sb.append("console.log(\"Data must be non-empty.\");\n");
    sb.append("return undefined;\n");
    sb.append("}\n");
    sb.append("if (!xSuffix){\n");
    sb.append("xSuffix = \"\";\n");
    sb.append("}\n");
    sb.append("if (!ySuffix){\n");
    sb.append("ySuffix = \"\";\n");
    sb.append("}\n");
    sb.append("w = Math.round(w);\n");
    sb.append("h = Math.round(h);\n");
    sb.append("const r = (w+h)/220;\n");
    sb.append("let xMin = Math.min(0, ...xData, ...xFit);\n");
    sb.append("let xMax = Math.max(0, ...xData, ...xFit);\n");
    sb.append("let xRange = (xMax-xMin)/40;\n");
    sb.append("xMin-=xRange;\n");
    sb.append("xMax+=xRange;\n");
    sb.append("xRange = xMax-xMin;\n");
    sb.append("const xDecimals = Math.max(0,Math.round(2.2-Math.log10(xRange)));\n");
    sb.append("const xFn = function(x){\n");
    sb.append("return Math.round((x-xMin)*w/xRange);\n");
    sb.append("};\n");
    sb.append("const xOrigin = xFn(0);\n");
    sb.append("const xxData = [];\n");
    sb.append("for (const x of xData){\n");
    sb.append("xxData.push(xFn(x));\n");
    sb.append("}\n");
    sb.append("let yMin = Math.min(0, ...yData, ...yFit);\n");
    sb.append("let yMax = Math.max(0, ...yData, ...yFit);\n");
    sb.append("let yRange = (yMax-yMin)/40;\n");
    sb.append("yMin-=yRange;\n");
    sb.append("yMax+=yRange;\n");
    sb.append("yRange = yMax-yMin;\n");
    sb.append("const yDecimals = Math.max(0,Math.round(2.2-Math.log10(yRange)));\n");
    sb.append("const yFn = function(y){\n");
    sb.append("return Math.round(h-(y-yMin)*h/yRange-1);\n");
    sb.append("};\n");
    sb.append("const yOrigin = yFn(0);\n");
    sb.append("const yyData = [];\n");
    sb.append("for (const y of yData){\n");
    sb.append("yyData.push(yFn(y));\n");
    sb.append("}\n");
    sb.append("const canvas = document.createElement(\"CANVAS\");\n");
    sb.append("canvas.onmouseleave = function(e){\n");
    sb.append("popup.style.display = \"none\";\n");
    sb.append("};\n");
    sb.append("canvas.onmousemove = function(e){\n");
    sb.append("if (mouseDown){\n");
    sb.append("canvas.onmousedown(e);\n");
    sb.append("}else{\n");
    sb.append("const rect = canvas.getBoundingClientRect();\n");
    sb.append("popup.innerText = '('+((e.clientX-rect.left)*xRange/w+xMin).toFixed(xDecimals)+xSuffix+\", \"+((h-e.clientY+rect.top-1)*yRange/h+yMin).toFixed(yDecimals)+ySuffix+')';\n");
    sb.append("popup.style.left = String(Math.round(e.clientX-popup.offsetWidth/2))+\"px\";\n");
    sb.append("popup.style.top = String(Math.round(e.clientY+popup.offsetHeight*1.5))+\"px\";\n");
    sb.append("popup.style.display = \"inline-block\";\n");
    sb.append("}\n");
    sb.append("};\n");
    sb.append("canvas.onmouseover = canvas.onmousemove;\n");
    sb.append("canvas.onmouseenter = canvas.onmousemove;\n");
    sb.append("canvas.onmousedown = function(e){\n");
    sb.append("const rect = canvas.getBoundingClientRect();\n");
    sb.append("const x = e.clientX-rect.left;\n");
    sb.append("const y = e.clientY-rect.top;\n");
    sb.append("let j = 0;\n");
    sb.append("let rr = -1;\n");
    sb.append("for (var i=0;i<xData.length;++i){\n");
    sb.append("const rtmp = (x-xxData[i])**2+(y-yyData[i])**2;\n");
    sb.append("if (rr===-1 || rtmp<rr){\n");
    sb.append("rr = rtmp;\n");
    sb.append("j = i;\n");
    sb.append("}\n");
    sb.append("}\n");
    sb.append("popup.innerText = '('+xData[j].toFixed(xDecimals)+xSuffix+\", \"+yData[j].toFixed(yDecimals)+ySuffix+')';\n");
    sb.append("popup.style.left = String(Math.round(rect.left+xxData[j]-popup.offsetWidth/2))+\"px\";\n");
    sb.append("popup.style.top = String(Math.round(rect.top+yyData[j]+popup.offsetHeight/2))+\"px\";\n");
    sb.append("popup.style.display = \"inline-block\";\n");
    sb.append("}\n");
    sb.append("canvas.setAttribute(\"width\", w);\n");
    sb.append("canvas.setAttribute(\"height\", h);\n");
    sb.append("canvas.style.display = \"block\";\n");
    sb.append("canvas.style.userSelect = \"none\";\n");
    sb.append("canvas.style.backgroundColor = \"black\";\n");
    sb.append("const ctx = canvas.getContext(\"2d\");\n");
    sb.append("ctx.globalAlpha = 1;\n");
    sb.append("ctx.lineWidth = 1;\n");
    sb.append("ctx.strokeStyle = \"steelblue\";\n");
    sb.append("ctx.beginPath();\n");
    sb.append("ctx.moveTo(xOrigin, 0);\n");
    sb.append("ctx.lineTo(xOrigin, h);\n");
    sb.append("ctx.moveTo(0, yOrigin);\n");
    sb.append("ctx.lineTo(w, yOrigin);\n");
    sb.append("ctx.stroke();\n");
    sb.append("ctx.lineWidth = 2;\n");
    sb.append("ctx.strokeStyle = \"forestgreen\";\n");
    sb.append("ctx.beginPath();\n");
    sb.append("for (var i=0;i<xFit.length;++i){\n");
    sb.append("const x = xData===xFit?xxData[i]:xFn(xFit[i]);\n");
    sb.append("const y = yData===yFit?yyData[i]:yFn(yFit[i]);\n");
    sb.append("if (i===0){\n");
    sb.append("ctx.moveTo(x,y);\n");
    sb.append("}else{\n");
    sb.append("ctx.lineTo(x,y);\n");
    sb.append("}\n");
    sb.append("}\n");
    sb.append("ctx.stroke();\n");
    sb.append("ctx.fillStyle = \"violet\";\n");
    sb.append("ctx.beginPath();\n");
    sb.append("for (var i=0;i<xData.length;++i){\n");
    sb.append("const x = xxData[i];\n");
    sb.append("const y = yyData[i];\n");
    sb.append("ctx.moveTo(x,y);\n");
    sb.append("ctx.arc(x, y, r, 0, 2*Math.PI);\n");
    sb.append("}\n");
    sb.append("ctx.fill();\n");
    sb.append("parent.appendChild(canvas);\n");
    sb.append("return ctx;\n");
    sb.append("}\n");
    sb.append("function draw(parent, data){\n");
    sb.append("const vw = Math.max(document.documentElement.clientWidth || 0, window.innerWidth || 0)/5;\n");
    sb.append("const vh = Math.max(document.documentElement.clientHeight || 0, window.innerHeight || 0)/4;\n");
    sb.append("for (const x of data){\n");
    sb.append("const table = document.createElement(\"TABLE\");\n");
    sb.append("const thead = document.createElement(\"THEAD\");\n");
    sb.append("const tbody = document.createElement(\"TBODY\");\n");
    sb.append("table.appendChild(thead);\n");
    sb.append("table.appendChild(tbody);\n");
    sb.append("const trTitle = document.createElement(\"TR\");\n");
    sb.append("const trHeaders = document.createElement(\"TR\");\n");
    sb.append("thead.appendChild(trTitle);\n");
    sb.append("thead.appendChild(trHeaders);\n");
    sb.append("const tdTitle = document.createElement(\"TD\");\n");
    sb.append("trTitle.appendChild(tdTitle);\n");
    sb.append("tdTitle.style.fontSize = \"150%\";\n");
    sb.append("tdTitle.style.fontWeight = \"bold\";\n");
    sb.append("tdTitle.style.color = \"aquamarine\";\n");
    sb.append("tdTitle.innerText = x[\"name\"];\n");
    sb.append("tdTitle.setAttribute(\"colspan\",\"7\");\n");
    sb.append("const thPath = document.createElement(\"TH\");\n");
    sb.append("const thStart = document.createElement(\"TH\");\n");
    sb.append("const thStop = document.createElement(\"TH\");\n");
    sb.append("const thFanStart = document.createElement(\"TH\");\n");
    sb.append("const thFanStop = document.createElement(\"TH\");\n");
    sb.append("const thDamper = document.createElement(\"TH\");\n");
    sb.append("const thValve = document.createElement(\"TH\");\n");
    sb.append("trHeaders.appendChild(thPath);\n");
    sb.append("trHeaders.appendChild(thStart);\n");
    sb.append("trHeaders.appendChild(thStop);\n");
    sb.append("trHeaders.appendChild(thFanStart);\n");
    sb.append("trHeaders.appendChild(thFanStop);\n");
    sb.append("trHeaders.appendChild(thDamper);\n");
    sb.append("trHeaders.appendChild(thValve);\n");
    sb.append("thPath.innerText = \"Location\";\n");
    sb.append("thStart.innerText = \"Start Time\";\n");
    sb.append("thStop.innerText = \"Stop Time\";\n");
    sb.append("thFanStart.innerText = \"Fan Start Command\";\n");
    sb.append("thFanStop.innerText = \"Fan Stop Command\";\n");
    sb.append("thDamper.innerText = \"Airflow (CFM) vs. Damper Position (%)\";\n");
    sb.append("thValve.innerText = \"HW Valve Temperature Differential Over Time\";\n");
    sb.append("for (const y of x[\"equipment\"]){\n");
    sb.append("const tr = document.createElement(\"TR\");\n");
    sb.append("const tdPath = document.createElement(\"TD\");\n");
    sb.append("const a = document.createElement(\"A\");\n");
    sb.append("a.innerText = y[\"path\"];\n");
    sb.append("a.href = y[\"link\"];\n");
    sb.append("a.setAttribute(\"target\",\"_blank\");\n");
    sb.append("tdPath.appendChild(a);\n");
    sb.append("tr.appendChild(tdPath);\n");
    sb.append("const tdStart = document.createElement(\"TD\");\n");
    sb.append("tdStart.innerText = y[\"start\"];\n");
    sb.append("tr.appendChild(tdStart);\n");
    sb.append("const tdStop = document.createElement(\"TD\");\n");
    sb.append("tdStop.innerText = y[\"stop\"];\n");
    sb.append("tr.appendChild(tdStop);\n");
    sb.append("const fan = y[\"fan\"];\n");
    sb.append("if (!fan || fan[\"error\"]){\n");
    sb.append("const tdFan = document.createElement(\"TD\");\n");
    sb.append("tdFan.setAttribute(\"colspan\",\"2\");\n");
    sb.append("if (fan){\n");
    sb.append("tdFan.innerText = \"Error\";\n");
    sb.append("}\n");
    sb.append("tr.appendChild(tdFan);\n");
    sb.append("}else{\n");
    sb.append("const tdFanStart = document.createElement(\"TD\");\n");
    sb.append("const tdFanStop = document.createElement(\"TD\");\n");
    sb.append("tdFanStart.innerText = fan[\"start\"]?\"Success\":\"Failure\";\n");
    sb.append("tdFanStop.innerText = fan[\"stop\"]?\"Success\":\"Failure\";\n");
    sb.append("tr.appendChild(tdFanStart);\n");
    sb.append("tr.appendChild(tdFanStop);\n");
    sb.append("}\n");
    sb.append("const damper = y[\"damper\"];\n");
    sb.append("const tdDamper = document.createElement(\"TD\");\n");
    sb.append("tr.appendChild(tdDamper);\n");
    sb.append("if (!damper || damper[\"error\"]){\n");
    sb.append("if (damper){\n");
    sb.append("tdDamper.innerText = \"Error\";\n");
    sb.append("}\n");
    sb.append("}else{\n");
    sb.append("scatter(tdDamper, vw, vh, damper[\"x\"], damper[\"y\"], damper[\"xFit\"], damper[\"yFit\"], \"%\", \" cfm\");\n");
    sb.append("}\n");
    sb.append("const valve = y[\"valve\"];\n");
    sb.append("const tdValve = document.createElement(\"TD\");\n");
    sb.append("tr.appendChild(tdValve);\n");
    sb.append("if (!valve || valve[\"error\"]){\n");
    sb.append("if (valve){\n");
    sb.append("tdValve.innerText = \"Error\";\n");
    sb.append("}\n");
    sb.append("}else{\n");
    sb.append("scatter(tdValve, vw, vh, valve[\"x\"], valve[\"y\"], valve[\"x\"], valve[\"y\"], \" min\", \" \\u{00B0}F\");\n");
    sb.append("}\n");
    sb.append("tbody.appendChild(tr);\n");
    sb.append("}\n");
    sb.append("parent.appendChild(document.createElement(\"BR\"));\n");
    sb.append("parent.appendChild(table);\n");
    sb.append("}\n");
    sb.append("}\n");
    sb.append("</script>\n");
    sb.append("</head>\n");
    sb.append("<body>\n");
    sb.append("<div id=\"popup\" style=\"display:none;position:absolute;cursor:default;color:aqua;pointer-events:none\"></div>\n");
    sb.append("<div id=\"mainDiv\" class=\"c\">\n");
    sb.append("<h1>VAV Box Report</h1>\n");
    sb.append("</div>\n");
    sb.append("<script>\n");
    sb.append("var mouseDown = 0;\n");
    sb.append("document.onmousedown = function(){ \n");
    sb.append("++mouseDown;\n");
    sb.append("};\n");
    sb.append("document.onmouseup = function(){\n");
    sb.append("--mouseDown;\n");
    sb.append("};\n");
    sb.append("draw(mainDiv,\n");
    sb.append("[\n");
    boolean first = true;
    int group = -1;
    for (Data d:list){
      if (d.group==group){
        sb.append(",\n");
      }else{
        group = d.group;
        if (first){
          first = false;
        }else{
          sb.append("\n]\n},\n");
        }
        sb.append(Utility.format("{\n\"name\":\"$0\",\n\"equipment\":[\n", Utility.escapeJS((String)this.mapping.groupNames.getOrDefault(group,"(Deleted Group)"))));
      }
      d.print(sb);
    }
    sb.append("]\n");
    sb.append(");\n");
    if (!exited){
      sb.append("setTimeout(()=>{window.location.reload();}, 60000);\n");
    }
    sb.append("</script>\n");
    sb.append("</body>\n");
    sb.append("</html>");
    return sb.toString();
  }
  class Data implements Comparable<Object> {
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
    public final static int[] airflowX = new int[]{0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
    public volatile Stats[] airflowY;
    private final static int airflowSamples = 10;
    public volatile boolean valveError = false;
    public volatile double[] heatingX;
    public volatile double[] heatingY;
    public void print(final StringBuilder sb){
      boolean first;
      int i,j;
      sb.append("{\n");
      sb.append(Utility.format("\"path\":\"$0\",\n", Utility.escapeJS(path)));
      sb.append(Utility.format("\"link\":\"$0\",\n", link));
      sb.append(Utility.format("\"start\":\"$0\",\n", Utility.escapeJS(Utility.getDateString(start))));
      sb.append(Utility.format("\"stop\":\"$0\"", Utility.escapeJS(Utility.getDateString(end))));
      if (hasFan){
        sb.append(",\n\"fan\":{\n");
        sb.append("\"error\":").append(fanError);
        if (!fanError){
          sb.append(",\n\"start\":").append(fanStartTest);
          sb.append(",\n\"stop\":").append(fanStopTest);
        }
        sb.append("\n}");
      }
      if (hasDamper){
        sb.append(",\n\"damper\":{\n");
        sb.append("\"error\":").append(damperError);
        if (!damperError){
          sb.append(",\n\"x\":[");
          first = true;
          for (j=0;j<airflowX.length;++j){
            for (i=0;i<airflowSamples;++i){
              if (first){
                first = false;
              }else{
                sb.append(',');
              }
              sb.append(airflowX[j]);
            }
          }
          sb.append("],\n\"y\":[");
          first = true;
          for (Stats st:airflowY){
            for (j=0;j<st.arr.length;++j){
              if (first){
                first = false;
              }else{
                sb.append(',');
              }
              sb.append(st.arr[j]);
            }
          }
          sb.append("],\n\"xFit\":[");
          first = true;
          for (j=0;j<airflowX.length;++j){
            if (first){
              first = false;
            }else{
              sb.append(',');
            }
            sb.append(airflowX[j]);
          }
          sb.append("],\n\"yFit\":[");
          first = true;
          for (Stats st:airflowY){
            if (first){
              first = false;
            }else{
              sb.append(',');
            }
            sb.append(st.mean);
          }
          sb.append(']');
        }
        sb.append("\n}");
      }
      if (hasValve){
        sb.append(",\n\"valve\":{\n");
        sb.append("\"error\":").append(valveError);
        if (!valveError){
          sb.append(",\n\"x\":[");
          first = true;
          for (j=0;j<heatingX.length;++j){
            if (first){
              first = false;
            }else{
              sb.append(',');
            }
            sb.append(heatingX[j]);
          }
          sb.append("],\n\"y\":[");
          first = true;
          for (j=0;j<heatingY.length;++j){
            if (first){
              first = false;
            }else{
              sb.append(',');
            }
            sb.append(heatingY[j]);
          }
          sb.append(']');
        }
        sb.append("\n}");
      }
      sb.append("\n}");
    }
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
      String s=null,t=null;
      Stats st;
      hasFan = testFans && x.hasMapping(tagSFSSLockFlag) && x.hasMapping(tagSFSSLockValue) && x.hasMapping(tagSFST);
      hasDamper = testDampers && x.hasMapping(tagAirflow) && x.hasMapping(tagDamperLockFlag) && x.hasMapping(tagDamperLockValue) && x.hasMapping(tagDamperPosition);
      hasValve = testValves && x.hasMapping(tagValveLockFlag) && x.hasMapping(tagValveLockValue) && x.hasMapping(tagValvePosition) && x.hasMapping(tagEAT) && x.hasMapping(tagLAT);
      if (hasFan) fanTest: {
        if (x.markAndSetValue(tagSFSSLockValue, 0)==null || x.markAndSetValue(tagSFSSLockFlag, true)==null){
          fanError = true; break fanTest;
        }
        b = waitFor(90000, 5000, tagSFST, new Predicate<Object>(){
          public boolean test(Object s){
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
        b = waitFor(90000, 5000, tagSFST, new Predicate<Object>(){
          public boolean test(Object s){
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
          airflowY = new Stats[airflowX.length];
          for (int i=0;i<airflowX.length;++i){
            final int pos = airflowX[i];
            if (i==0){
              if (x.markAndSetValue(tagDamperLockValue, pos)==null || x.markAndSetValue(tagDamperLockFlag, true)==null){
                damperError = true; break damperTest;
              }
            }else{
              if (!x.setValue(tagDamperLockValue, pos)){
                damperError = true; break damperTest;
              }
            }
            b = waitFor(120000, 5000, tagDamperPosition, new Predicate<Object>(){
              public boolean test(Object s){
                return Math.abs(Double.parseDouble(s.toString())-pos)<1;
              }
            });
            if ((st = evaluate(airflowSamples, 3000, tagAirflow))==null){
              damperError = true; break damperTest;
            }
            airflowY[i] = st;
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
          Thread.sleep(30000L+(long)(initialPosition*8000));
          if ((s=x.getValue(tagEAT))==null || (t=x.getValue(tagLAT))==null){
            valveError = true; break valveTest;
          }
          final long startTime = System.currentTimeMillis();
          heatingX = new double[61];
          heatingY = new double[heatingX.length];
          heatingX[0] = 0;
          heatingY[0] = Double.parseDouble(t)-Double.parseDouble(s);
          if (x.markAndSetValue(tagValveLockValue,100)==null){
            valveError = true; break valveTest;
          }
          for (int i=1;i<heatingX.length;++i){
            Thread.sleep(10000L);
            if ((s=x.getValue(tagEAT))==null || (t=x.getValue(tagLAT))==null){
              valveError = true; break valveTest;
            }
            heatingX[i] = (System.currentTimeMillis()-startTime)/60000.0;
            heatingY[i] = Double.parseDouble(t)-Double.parseDouble(s);
          }
        }catch(NumberFormatException e){
          valveError = true;
        }
      }
      end = System.currentTimeMillis();
    }
    private Boolean waitFor(long timeout, long interval, String tag, Predicate<Object> test) throws Throwable {
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
  //public volatile double absoluteDeviation = 0;
  public Stats(double... arr){
    this.arr = arr;
    if (arr.length>0){
      for (int i=0;i<arr.length;++i){
        mean+=arr[i];
      }
      mean/=arr.length;
      /*
      for (int i=0;i<arr.length;++i){
        absoluteDeviation+=Math.abs(arr[i]-mean);
      }
      absoluteDeviation/=arr.length;
      */
    }
  }
}