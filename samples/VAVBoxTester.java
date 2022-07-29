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
    sb.append("function scatter(w, h, xData, yData, xFit, yFit, xSuffix, ySuffix, success){\n");
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
    sb.append("if (w===0){\n");
    sb.append("w = 10;\n");
    sb.append("}\n");
    sb.append("if (h===0){\n");
    sb.append("h = 10;\n");
    sb.append("}\n");
    sb.append("const r = (w+h)/220;\n");
    sb.append("let xMin = Math.min(0, ...xData, ...xFit);\n");
    sb.append("let xMax = Math.max(0, ...xData, ...xFit);\n");
    sb.append("if (xMin===xMax){\n");
    sb.append("xMax = 1;\n");
    sb.append("}\n");
    sb.append("let xRange = (xMax-xMin)/40;\n");
    sb.append("xMin-=xRange;\n");
    sb.append("xMax+=xRange;\n");
    sb.append("xRange = xMax-xMin;\n");
    sb.append("const xDecimals = Math.max(0,Math.round(2.2-Math.log10(xRange)));\n");
    sb.append("if (xDecimals<0){\n");
    sb.append("xDecimals = 0;\n");
    sb.append("}else if (xDecimals>99){\n");
    sb.append("xDecimals = 99;\n");
    sb.append("}\n");
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
    sb.append("if (yMin===yMax){\n");
    sb.append("yMax = 1;\n");
    sb.append("}\n");
    sb.append("let yRange = (yMax-yMin)/40;\n");
    sb.append("yMin-=yRange;\n");
    sb.append("yMax+=yRange;\n");
    sb.append("yRange = yMax-yMin;\n");
    sb.append("const yDecimals = Math.max(0,Math.round(2.2-Math.log10(yRange)));\n");
    sb.append("if (yDecimals<0){\n");
    sb.append("yDecimals = 0;\n");
    sb.append("}else if (yDecimals>99){\n");
    sb.append("yDecimals = 99;\n");
    sb.append("}\n");
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
    sb.append("if (e.ctrlKey || e.shiftKey){\n");
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
    sb.append("popup.style.left = String(Math.round(rect.left+xxData[j]-popup.offsetWidth/2+window.scrollX))+\"px\";\n");
    sb.append("popup.style.top = String(Math.round(rect.top+yyData[j]+popup.offsetHeight/2+window.scrollY))+\"px\";\n");
    sb.append("popup.style.display = \"inline-block\";\n");
    sb.append("}else{\n");
    sb.append("const rect = canvas.getBoundingClientRect();\n");
    sb.append("popup.innerText = '('+((e.clientX-rect.left)*xRange/w+xMin).toFixed(xDecimals)+xSuffix+\", \"+((h-e.clientY+rect.top-1)*yRange/h+yMin).toFixed(yDecimals)+ySuffix+')';\n");
    sb.append("popup.style.left = String(Math.round(e.clientX-popup.offsetWidth/2+window.scrollX))+\"px\";\n");
    sb.append("popup.style.top = String(Math.round(e.clientY+popup.offsetHeight+window.scrollY))+\"px\";\n");
    sb.append("popup.style.display = \"inline-block\";\n");
    sb.append("}\n");
    sb.append("};\n");
    sb.append("canvas.onmouseover = canvas.onmousemove;\n");
    sb.append("canvas.onmouseenter = canvas.onmousemove;\n");
    sb.append("canvas.setAttribute(\"width\", w);\n");
    sb.append("canvas.setAttribute(\"height\", h);\n");
    sb.append("canvas.style.display = \"block\";\n");
    sb.append("canvas.style.userSelect = \"none\";\n");
    sb.append("canvas.style.backgroundColor = \"black\";\n");
    sb.append("canvas.style.cursor = \"crosshair\";\n");
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
    sb.append("ctx.strokeStyle = success?\"forestgreen\":\"crimson\";\n");
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
    sb.append("ctx.fillStyle = success?\"deepskyblue\":\"lightgrey\";\n");
    sb.append("ctx.beginPath();\n");
    sb.append("for (var i=0;i<xData.length;++i){\n");
    sb.append("const x = xxData[i];\n");
    sb.append("const y = yyData[i];\n");
    sb.append("ctx.moveTo(x,y);\n");
    sb.append("ctx.arc(x, y, r, 0, 2*Math.PI);\n");
    sb.append("}\n");
    sb.append("ctx.fill();\n");
    sb.append("return canvas;\n");
    sb.append("}\n");
    sb.append("function draw(parent, data){\n");
    sb.append("const vw = Math.max(document.documentElement.clientWidth || 0, window.innerWidth || 0)/5;\n");
    sb.append("const vh = Math.max(document.documentElement.clientHeight || 0, window.innerHeight || 0)/4;\n");
    sb.append("for (const x of data){\n");
    sb.append("const table = document.createElement(\"TABLE\");\n");
    sb.append("table.style.userSelect = \"none\";\n");
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
    sb.append("tdTitle.setAttribute(\"colspan\",\"6\");\n");
    sb.append("const thPath = document.createElement(\"TH\");\n");
    sb.append("const thDuration = document.createElement(\"TH\");\n");
    sb.append("const thFanStart = document.createElement(\"TH\");\n");
    sb.append("const thFanStop = document.createElement(\"TH\");\n");
    sb.append("const thDamper = document.createElement(\"TH\");\n");
    sb.append("const thValve = document.createElement(\"TH\");\n");
    sb.append("trHeaders.appendChild(thPath);\n");
    sb.append("trHeaders.appendChild(thDuration);\n");
    sb.append("trHeaders.appendChild(thFanStart);\n");
    sb.append("trHeaders.appendChild(thFanStop);\n");
    sb.append("trHeaders.appendChild(thDamper);\n");
    sb.append("trHeaders.appendChild(thValve);\n");
    sb.append("thPath.innerText = \"Location\";\n");
    sb.append("thDuration.innerText = \"Duration\";\n");
    sb.append("thFanStart.innerText = \"Fan Start\";\n");
    sb.append("thFanStop.innerText = \"Fan Stop\";\n");
    sb.append("thDamper.innerText = \"Damper Airflow\";\n");
    sb.append("thValve.innerText = \"Hot Water Valve\";\n");
    sb.append("for (const y of x[\"equipment\"]){\n");
    sb.append("const tr = document.createElement(\"TR\");\n");
    sb.append("const tdPath = document.createElement(\"TD\");\n");
    sb.append("const a = document.createElement(\"A\");\n");
    sb.append("a.innerText = y[\"path\"];\n");
    sb.append("a.href = y[\"link\"];\n");
    sb.append("a.style.border = \"none\";\n");
    sb.append("a.setAttribute(\"target\",\"_blank\");\n");
    sb.append("tdPath.appendChild(a);\n");
    sb.append("tr.appendChild(tdPath);\n");
    sb.append("const tdDuration = document.createElement(\"TD\");\n");
    sb.append("tdDuration.innerText = y[\"duration\"].toFixed(1)+\" min\";\n");
    sb.append("tdDuration.title = y[\"start\"]+\"  -  \"+y[\"stop\"];\n");
    sb.append("tr.appendChild(tdDuration);\n");
    sb.append("const fan = y[\"fan\"];\n");
    sb.append("if (!fan || fan[\"error\"]){\n");
    sb.append("const tdFan = document.createElement(\"TD\");\n");
    sb.append("tdFan.setAttribute(\"colspan\",\"2\");\n");
    sb.append("if (fan){\n");
    sb.append("tdFan.innerText = \"Error\";\n");
    sb.append("tdFan.style.backgroundColor = \"purple\";\n");
    sb.append("}\n");
    sb.append("tr.appendChild(tdFan);\n");
    sb.append("}else{\n");
    sb.append("const tdFanStart = document.createElement(\"TD\");\n");
    sb.append("const tdFanStop = document.createElement(\"TD\");\n");
    sb.append("tdFanStart.innerText = fan[\"start\"]?\"Success\":\"Failure\";\n");
    sb.append("tdFanStop.innerText = fan[\"stop\"]?\"Success\":\"Failure\";\n");
    sb.append("tdFanStart.style.backgroundColor = fan[\"start\"]?\"darkgreen\":\"darkred\";\n");
    sb.append("tdFanStop.style.backgroundColor = fan[\"stop\"]?\"darkgreen\":\"darkred\";\n");
    sb.append("tr.appendChild(tdFanStart);\n");
    sb.append("tr.appendChild(tdFanStop);\n");
    sb.append("}\n");
    sb.append("const damper = y[\"damper\"];\n");
    sb.append("const tdDamper = document.createElement(\"TD\");\n");
    sb.append("tr.appendChild(tdDamper);\n");
    sb.append("if (!damper || damper[\"error\"]){\n");
    sb.append("if (damper){\n");
    sb.append("tdDamper.innerText = \"Error\";\n");
    sb.append("tdDamper.style.backgroundColor = \"purple\";\n");
    sb.append("}\n");
    sb.append("}else if (damper[\"response\"]){\n");
    sb.append("let success = true;\n");
    sb.append("{\n");
    sb.append("//const xData = damper[\"xFit\"];\n");
    sb.append("const yData = damper[\"yFit\"];\n");
    sb.append("const margin = Math.max(...yData)*Number(damperMargin.value)/100;\n");
    sb.append("if (Math.abs(yData[0])<margin){\n");
    sb.append("let prev = yData[0];\n");
    sb.append("for (const cur of yData){\n");
    sb.append("if (prev>cur+margin){\n");
    sb.append("success = false;\n");
    sb.append("break;\n");
    sb.append("}\n");
    sb.append("prev = cur;\n");
    sb.append("}\n");
    sb.append("}else{\n");
    sb.append("success = false;\n");
    sb.append("}\n");
    sb.append("}\n");
    sb.append("const canvas = scatter(vw, vh, damper[\"x\"], damper[\"y\"], damper[\"xFit\"], damper[\"yFit\"], \"%\", \" cfm\", success);\n");
    sb.append("tdDamper.style.cursor = \"pointer\";\n");
    sb.append("tdDamper.style.backgroundColor = success?\"darkgreen\":\"darkred\";\n");
    sb.append("canvas.shown = !graphsVisible;\n");
    sb.append("tdDamper.onclick = function(){\n");
    sb.append("canvas.shown = !canvas.shown;\n");
    sb.append("if (canvas.shown){\n");
    sb.append("tdDamper.replaceChildren(canvas);\n");
    sb.append("}else{\n");
    sb.append("tdDamper.replaceChildren(success?\"Success\":\"Failure\");\n");
    sb.append("canvas.onmouseleave();\n");
    sb.append("}\n");
    sb.append("};\n");
    sb.append("tdDamper.onclick();\n");
    sb.append("tr.toggleDamperGraph = function(b){\n");
    sb.append("if (b^canvas.shown){\n");
    sb.append("tdDamper.onclick();\n");
    sb.append("}\n");
    sb.append("};\n");
    sb.append("}else{\n");
    sb.append("tdDamper.innerText = \"Failure\";\n");
    sb.append("tdDamper.style.backgroundColor = \"darkred\";\n");
    sb.append("}\n");
    sb.append("const valve = y[\"valve\"];\n");
    sb.append("const tdValve = document.createElement(\"TD\");\n");
    sb.append("tr.appendChild(tdValve);\n");
    sb.append("if (!valve || valve[\"error\"]){\n");
    sb.append("if (valve){\n");
    sb.append("tdValve.innerText = \"Error\";\n");
    sb.append("tdValve.style.backgroundColor = \"purple\";\n");
    sb.append("}\n");
    sb.append("}else{\n");
    sb.append("let success = true;\n");
    sb.append("{\n");
    sb.append("//const xData = valve[\"x\"];\n");
    sb.append("const yData = valve[\"y\"];\n");
    sb.append("if (Math.max(...yData)<Number(hwMargin.value)){\n");
    sb.append("success = false;\n");
    sb.append("}\n");
    sb.append("}\n");
    sb.append("const canvas = scatter(vw, vh, valve[\"x\"], valve[\"y\"], valve[\"x\"], valve[\"y\"], \" min\", \" \\u{00B0}F\", success);\n");
    sb.append("tdValve.style.cursor = \"pointer\";\n");
    sb.append("tdValve.style.backgroundColor = success?\"darkgreen\":\"darkred\";\n");
    sb.append("canvas.shown = !graphsVisible;\n");
    sb.append("tdValve.onclick = function(){\n");
    sb.append("canvas.shown = !canvas.shown;\n");
    sb.append("if (canvas.shown){\n");
    sb.append("tdValve.replaceChildren(canvas);\n");
    sb.append("}else{\n");
    sb.append("tdValve.replaceChildren(success?\"Success\":\"Failure\");\n");
    sb.append("canvas.onmouseleave();\n");
    sb.append("}\n");
    sb.append("};\n");
    sb.append("tdValve.onclick();\n");
    sb.append("tr.toggleValveGraph = function(b){\n");
    sb.append("if (b^canvas.shown){\n");
    sb.append("tdValve.onclick();\n");
    sb.append("}\n");
    sb.append("};\n");
    sb.append("}\n");
    sb.append("tbody.appendChild(tr);\n");
    sb.append("}\n");
    sb.append("parent.appendChild(document.createElement(\"BR\"));\n");
    sb.append("parent.appendChild(table);\n");
    sb.append("}\n");
    sb.append("}\n");
    sb.append("function toggleGraphs(){\n");
    sb.append("graphsVisible = !graphsVisible;\n");
    sb.append("for (const tr of document.getElementById(\"mainDiv\").getElementsByTagName(\"TR\")){\n");
    sb.append("if (tr.toggleDamperGraph){\n");
    sb.append("tr.toggleDamperGraph(graphsVisible);\n");
    sb.append("}\n");
    sb.append("if (tr.toggleValveGraph){\n");
    sb.append("tr.toggleValveGraph(graphsVisible);\n");
    sb.append("}\n");
    sb.append("}\n");
    sb.append("}\n");
    sb.append("function reloadData(){\n");
    sb.append("const mainDiv = document.getElementById(\"mainDiv\");\n");
    sb.append("let e = mainDiv.lastElementChild;\n");
    sb.append("while (e && e.id!==\"dataAfterBreak\"){\n");
    sb.append("const tmp = e;\n");
    sb.append("e = e.previousElementSibling;\n");
    sb.append("mainDiv.removeChild(tmp);\n");
    sb.append("}\n");
    sb.append("draw(mainDiv,DATA);\n");
    sb.append("}\n");
    sb.append("</script>\n");
    sb.append("</head>\n");
    sb.append("<body>\n");
    sb.append("<div id=\"popup\" style=\"display:none;position:absolute;cursor:default;color:yellow;pointer-events:none;\"></div>\n");
    sb.append("<div id=\"mainDiv\" class=\"c\">\n");
    sb.append("<h1>VAV Box Report</h1>\n");
    sb.append("<button class=\"e\" onclick=\"reloadData()\">Reevaluate Data Tolerances</button>\n");
    sb.append("<button class=\"e\" onclick=\"toggleGraphs()\">Toggle Graph Visibility</button>\n");
    sb.append("<br>\n");
    sb.append("<div class=\"divGrouping\">\n");
    sb.append("<label for=\"damperMargin\" id=\"damperMarginLabel\"></label>\n");
    sb.append("<input type=\"range\" style=\"width:20vw\" min=\"0\" max=\"10\" step=\"0.1\" value=\"3\" id=\"damperMargin\" oninput=\"damperMarginLabel.innerText='Damper Airflow Tolerance ('+Number(damperMargin.value).toFixed(1)+'%):'\">\n");
    sb.append("</div>\n");
    sb.append("<div class=\"divGrouping\">\n");
    sb.append("<label for=\"hwMargin\" id=\"hwMarginLabel\"></label>\n");
    sb.append("<input type=\"range\" style=\"width:20vw\" min=\"0\" max=\"50\" step=\"1\" value=\"15\" id=\"hwMargin\" oninput=\"hwMarginLabel.innerText='Minimum HW Differential ('+hwMargin.value+' \\u{00B0}F):'\">\n");
    sb.append("</div>\n");
    if (!exited){
      sb.append("<br><br>\n");
      sb.append("<div style=\"position:relative;top:0;left:15%;width:70%;height:1em\">\n");
      sb.append("<div class=\"bar\"></div>\n");
      sb.append(Utility.format("<div class=\"bar\" style=\"background-color:indigo;width:$0%\"></div>\n", 100*this.testsStarted.get()/this.testsTotal));
      sb.append(Utility.format("<div class=\"bar\" style=\"background-color:blue;width:$0%\"></div>\n", 100*this.testsCompleted.get()/this.testsTotal));
      sb.append("</div>\n");
      sb.append("<br id=\"dataAfterBreak\">\n");
    }else if (this.test.isKilled()){
      sb.append("<h3 id=\"dataAfterBreak\" style=\"color:crimson\">Foribly Terminated Before Natural Completion</h3>\n");
    }else{
      sb.append("<br id=\"dataAfterBreak\">\n");
    }
    sb.append("</div>\n");
    sb.append("<script>\n");
    sb.append("damperMargin.oninput();\n");
    sb.append("hwMargin.oninput();\n");
    sb.append("var graphsVisible = false;\n");
    if (list.size()>0){
      sb.append("var DATA=[\n");
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
      sb.append("]}];\n");
    }else{
      sb.append("var DATA=[];\n");
    }
    sb.append("draw(mainDiv,DATA);\n");
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
    public volatile boolean damperResponse = true;
    public final static int[] airflowX = new int[]{0,5,10,15,20,25,30,35,40,45,50,55,60,65,70,75,80,85,90,95,100};
    public volatile Stats[] airflowY;
    private final static int airflowSamples = 5;
    public volatile boolean valveError = false;
    public volatile double[] heatingX;
    public volatile double[] heatingY;
    public volatile int heatingSamples;
    public void print(final StringBuilder sb){
      boolean first;
      int i,j;
      sb.append("{\n");
      sb.append(Utility.format("\"path\":\"$0\",\n", Utility.escapeJS(path)));
      sb.append(Utility.format("\"link\":\"$0\",\n", link));
      sb.append(Utility.format("\"start\":\"$0\",\n", Utility.escapeJS(Utility.getDateString(start))));
      sb.append(Utility.format("\"stop\":\"$0\",\n", Utility.escapeJS(Utility.getDateString(end))));
      sb.append("\"duration\":").append((end-start)/60000.0);
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
          sb.append(",\n\"response\":").append(damperResponse);
          if (damperResponse){
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
        }
        sb.append("\n}");
      }
      if (hasValve){
        sb.append(",\n\"valve\":{\n");
        sb.append("\"error\":").append(valveError);
        if (!valveError){
          sb.append(",\n\"x\":[");
          first = true;
          for (j=0;j<heatingSamples;++j){
            if (first){
              first = false;
            }else{
              sb.append(',');
            }
            sb.append(heatingX[j]);
          }
          sb.append("],\n\"y\":[");
          first = true;
          for (j=0;j<heatingSamples;++j){
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
        b = waitFor(160000, 3000, tagSFST, new Predicate<Object>(){
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
        b = waitFor(160000, 3000, tagSFST, new Predicate<Object>(){
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
            b = waitFor(160000, 3000, tagDamperPosition, new Predicate<Object>(){
              public boolean test(Object s){
                return Math.abs(Double.parseDouble(s.toString())-pos)<1;
              }
            });
            if (b==null){
              damperError = true; break damperTest;
            }else if (!b){
              damperResponse = false; break damperTest;
            }
            if ((st = evaluate(airflowSamples, 2000, tagAirflow))==null){
              damperError = true; break damperTest;
            }
            airflowY[i] = st;
          }
        }catch(NumberFormatException e){
          Initializer.log(e);
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
          Thread.sleep(30000L+(long)(initialPosition*6000));
          if ((s=x.getValue(tagEAT))==null || (t=x.getValue(tagLAT))==null){
            valveError = true; break valveTest;
          }
          final long startTime = System.currentTimeMillis();
          heatingX = new double[61];
          heatingY = new double[heatingX.length];
          heatingSamples = heatingX.length;
          heatingX[0] = 0;
          heatingY[0] = Double.parseDouble(t)-Double.parseDouble(s);
          if (!x.setValue(tagValveLockValue,100)){
            valveError = true; break valveTest;
          }
          double yMax = 0;
          double yAvg;
          for (int i=1,j;i<heatingX.length;++i){
            Thread.sleep(10000L);
            if ((s=x.getValue(tagEAT))==null || (t=x.getValue(tagLAT))==null){
              valveError = true; break valveTest;
            }
            heatingX[i] = (System.currentTimeMillis()-startTime)/60000.0;
            heatingY[i] = Double.parseDouble(t)-Double.parseDouble(s)-heatingY[0];
            if (heatingY[i]>yMax){
              yMax = heatingY[i];
            }
            if (i>18){
              yAvg = 0;
              for (j=0;j<8;++j){
                yAvg+=heatingY[i-j-3];
              }
              yAvg/=8;
              if (yAvg+yMax/40>(heatingY[i]+heatingY[i-1]+heatingY[i-2])/3){
                heatingSamples = i+1;
                break;
              }
            }
          }
          heatingY[0] = 0;
        }catch(NumberFormatException e){
          Initializer.log(e);
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