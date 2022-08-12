import aces.webctrl.scripts.commissioning.core.*;
import aces.webctrl.scripts.commissioning.web.*;
import javax.servlet.http.*;
import java.util.*;
import java.util.function.*;
import java.util.concurrent.atomic.*;
public class TerminalUnitTest extends Script {
  private volatile boolean exited = false;
  private volatile Data[] data;
  private final AtomicInteger index = new AtomicInteger();
  private volatile boolean testDampers = false;
  private volatile boolean testFans = false;
  private volatile boolean testHeating = false;
  private volatile boolean initialized = false;
  @Override public String getDescription(){
    return "<a href=\"https://github.com/automatic-controls/commissioning-scripts/tree/main/samples/TerminalUnitTest\" target=\"_blank\" style=\"border:none;\">Terminal Unit Commissioning Script</a> v0.1.0<br>Evaluates performance of fans, dampers, and heating components.";
  }
  @Override public String[] getParamNames(){
    return new String[]{"Dampers", "Fans", "Heating"};
  }
  @Override public void exit(){
    exited = true;
  }
  @Override public void init(){
    data = new Data[this.testsTotal];
    Arrays.fill(data,null);
    testDampers = (Boolean)this.params.getOrDefault("Dampers",false);
    testFans = (Boolean)this.params.getOrDefault("Fans",false);
    testHeating = (Boolean)this.params.getOrDefault("Heating",false);
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
    final StringBuilder sb = new StringBuilder(8192);
    sb.append("<!DOCTYPE html>\n");
    sb.append("<html lang=\"en\">\n");
    sb.append("<head>\n");
    sb.append("<title>Terminal Unit Report</title>\n");
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
    sb.append("trHeaders.appendChild(thPath);\n");
    sb.append("thPath.innerText = \"Location\";\n");
    sb.append("const thDuration = document.createElement(\"TH\");\n");
    sb.append("trHeaders.appendChild(thDuration);\n");
    sb.append("thDuration.innerText = \"Duration\";\n");
    sb.append("if (testFans){\n");
    sb.append("const thFanStart = document.createElement(\"TH\");\n");
    sb.append("const thFanStop = document.createElement(\"TH\");\n");
    sb.append("trHeaders.appendChild(thFanStart);\n");
    sb.append("trHeaders.appendChild(thFanStop);\n");
    sb.append("thFanStart.innerText = \"Fan Start\";\n");
    sb.append("thFanStop.innerText = \"Fan Stop\";\n");
    sb.append("}\n");
    sb.append("if (testDampers){\n");
    sb.append("const thDamper = document.createElement(\"TH\");\n");
    sb.append("trHeaders.appendChild(thDamper);\n");
    sb.append("thDamper.innerText = \"Damper Airflow\";\n");
    sb.append("}\n");
    sb.append("if (testHeatAO){\n");
    sb.append("const thHeatAO = document.createElement(\"TH\");\n");
    sb.append("trHeaders.appendChild(thHeatAO);\n");
    sb.append("thHeatAO.innerText = \"Temperature Differential\";\n");
    sb.append("}\n");
    sb.append("for (const y of x[\"equipment\"]){\n");
    sb.append("const tr = document.createElement(\"TR\");\n");
    sb.append("tr.equipmentIdentifier = y[\"path\"];\n");
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
    sb.append("if (testFans){\n");
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
    sb.append("tdFanStart.innerText = fan[\"start\"]?\"Success\":\"Unresponsive\";\n");
    sb.append("tdFanStop.innerText = fan[\"stop\"]?\"Success\":\"Unresponsive\";\n");
    sb.append("tdFanStart.style.backgroundColor = fan[\"start\"]?\"darkgreen\":\"darkred\";\n");
    sb.append("tdFanStop.style.backgroundColor = fan[\"stop\"]?\"darkgreen\":\"darkred\";\n");
    sb.append("tr.appendChild(tdFanStart);\n");
    sb.append("tr.appendChild(tdFanStop);\n");
    sb.append("}\n");
    sb.append("}\n");
    sb.append("if (testDampers){\n");
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
    sb.append("//const xData = damper[\"xFit\"];\n");
    sb.append("const yData = damper[\"yFit\"];\n");
    sb.append("const yMax = Math.max(...yData);\n");
    sb.append("const yMaxStr = yMax.toFixed(0);\n");
    sb.append("if (useMaxCooling.checked && damper[\"maxCool\"] && yMax<damper[\"maxCool\"]*(100-Number(damperMargin.value))/100){\n");
    sb.append("success = false;\n");
    sb.append("}else{\n");
    sb.append("const margin = yMax*Number(damperMargin.value)/100;\n");
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
    sb.append("tr.damperVisible = canvas.shown;\n");
    sb.append("if (canvas.shown){\n");
    sb.append("tdDamper.replaceChildren(canvas);\n");
    sb.append("tdDamper.title = \"\";\n");
    sb.append("}else{\n");
    sb.append("tdDamper.replaceChildren((success?\"Success\":\"Failure\")+\": \"+yMaxStr+\" cfm\");\n");
    sb.append("if (damper[\"maxCool\"] && damper[\"maxCool\"]>0){\n");
    sb.append("tdDamper.title = \"Max Cooling = \"+damper[\"maxCool\"]+\" cfm\";\n");
    sb.append("}\n");
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
    sb.append("tdDamper.innerText = \"Unresponsive\";\n");
    sb.append("tdDamper.style.backgroundColor = \"darkred\";\n");
    sb.append("}\n");
    sb.append("}\n");
    sb.append("if (testHeatAO){\n");
    sb.append("const heatAO = y[\"heatAO\"];\n");
    sb.append("const tdHeatAO = document.createElement(\"TD\");\n");
    sb.append("tr.appendChild(tdHeatAO);\n");
    sb.append("if (!heatAO || heatAO[\"error\"]){\n");
    sb.append("if (heatAO){\n");
    sb.append("tdHeatAO.innerText = \"Error\";\n");
    sb.append("tdHeatAO.style.backgroundColor = \"purple\";\n");
    sb.append("}\n");
    sb.append("}else if (heatAO[\"failure\"]){\n");
    sb.append("if (heatAO[\"failMsg\"]){\n");
    sb.append("tdHeatAO.innerText = heatAO[\"failMsg\"];\n");
    sb.append("}else{\n");
    sb.append("tdHeatAO.innerText = \"Failure\";\n");
    sb.append("}\n");
    sb.append("tdHeatAO.style.backgroundColor = \"darkred\";\n");
    sb.append("}else{\n");
    sb.append("const cooling = heatAO[\"cooling\"];\n");
    sb.append("let success = true;\n");
    sb.append("const xData = heatAO[\"x\"];\n");
    sb.append("const yData = heatAO[\"y\"];\n");
    sb.append("const yMax = Math.max(...yData);\n");
    sb.append("const yMin = Math.min(...yData);\n");
    sb.append("let yMsg = \"\";\n");
    sb.append("const erraticLim = 6*Number(erraticMargin.value);\n");
    sb.append("{\n");
    sb.append("let yPrev = yData[0];\n");
    sb.append("let xPrev = xData[0];\n");
    sb.append("for (var i=0;i<yData.length;++i){\n");
    sb.append("if (xData[i]!==xPrev && Math.abs((yData[i]-yPrev)/(xData[i]-xPrev))>erraticLim){\n");
    sb.append("success = false;\n");
    sb.append("yMsg+=\"Erratic, \";\n");
    sb.append("break;\n");
    sb.append("}\n");
    sb.append("yPrev = yData[i];\n");
    sb.append("xPrev = xData[i];\n");
    sb.append("}\n");
    sb.append("}\n");
    sb.append("if (yMax<Number(heatAOMargin.value)){\n");
    sb.append("success = false;\n");
    sb.append("yMsg+=\"Heating, \";\n");
    sb.append("}\n");
    sb.append("if (cooling && yMin>-Number(coolMargin.value)){\n");
    sb.append("success = false;\n");
    sb.append("yMsg+=\"Cooling, \";\n");
    sb.append("}\n");
    sb.append("if (success){\n");
    sb.append("yMsg = \"Success: \"+yMsg;\n");
    sb.append("}\n");
    sb.append("yMsg+=(yMax-yMin).toFixed(1)+\" \\u{00B0}F\";\n");
    sb.append("const canvas = scatter(vw, vh, heatAO[\"x\"], heatAO[\"y\"], heatAO[\"x\"], heatAO[\"y\"], \" min\", \" \\u{00B0}F\", success);\n");
    sb.append("tdHeatAO.style.cursor = \"pointer\";\n");
    sb.append("tdHeatAO.style.backgroundColor = success?\"darkgreen\":\"darkred\";\n");
    sb.append("canvas.shown = !graphsVisible;\n");
    sb.append("tdHeatAO.onclick = function(){\n");
    sb.append("canvas.shown = !canvas.shown;\n");
    sb.append("tr.heatAOVisible = canvas.shown;\n");
    sb.append("if (canvas.shown){\n");
    sb.append("tdHeatAO.replaceChildren(canvas);\n");
    sb.append("}else{\n");
    sb.append("tdHeatAO.replaceChildren(yMsg);\n");
    sb.append("canvas.onmouseleave();\n");
    sb.append("}\n");
    sb.append("};\n");
    sb.append("tdHeatAO.onclick();\n");
    sb.append("tr.toggleHeatAOGraph = function(b){\n");
    sb.append("if (b^canvas.shown){\n");
    sb.append("tdHeatAO.onclick();\n");
    sb.append("}\n");
    sb.append("};\n");
    sb.append("}\n");
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
    sb.append("if (tr.toggleHeatAOGraph){\n");
    sb.append("tr.toggleHeatAOGraph(graphsVisible);\n");
    sb.append("}\n");
    sb.append("}\n");
    sb.append("}\n");
    sb.append("function reloadData(){\n");
    sb.append("graphsVisible = false;\n");
    sb.append("const mainDiv = document.getElementById(\"mainDiv\");\n");
    sb.append("const heatAOSet = new Set();\n");
    sb.append("const damperSet = new Set();\n");
    sb.append("for (const tr of mainDiv.getElementsByTagName(\"TR\")){\n");
    sb.append("if (tr.equipmentIdentifier){\n");
    sb.append("if (tr.heatAOVisible){\n");
    sb.append("heatAOSet.add(tr.equipmentIdentifier);\n");
    sb.append("}\n");
    sb.append("if (tr.damperVisible){\n");
    sb.append("damperSet.add(tr.equipmentIdentifier);\n");
    sb.append("}\n");
    sb.append("}\n");
    sb.append("}\n");
    sb.append("let e = mainDiv.lastElementChild;\n");
    sb.append("while (e && e.id!==\"dataAfterBreak\"){\n");
    sb.append("const tmp = e;\n");
    sb.append("e = e.previousElementSibling;\n");
    sb.append("mainDiv.removeChild(tmp);\n");
    sb.append("}\n");
    sb.append("draw(mainDiv,DATA);\n");
    sb.append("for (const tr of mainDiv.getElementsByTagName(\"TR\")){\n");
    sb.append("if (tr.equipmentIdentifier){\n");
    sb.append("if (tr.toggleHeatAOGraph && heatAOSet.has(tr.equipmentIdentifier)){\n");
    sb.append("tr.toggleHeatAOGraph(true);\n");
    sb.append("}\n");
    sb.append("if (tr.toggleDamperGraph && damperSet.has(tr.equipmentIdentifier)){\n");
    sb.append("tr.toggleDamperGraph(true);\n");
    sb.append("}\n");
    sb.append("}\n");
    sb.append("}\n");
    sb.append("}\n");
    sb.append("function prepareDataExport(){\n");
    sb.append("if (exportDataButton.getAttribute(\"href\").length===1 && DATA){\n");
    sb.append("exportDataButton.setAttribute(\"href\", \"data:text/plain;charset=utf-8,\"+encodeURIComponent(JSON.stringify(DATA, undefined, 2)));\n");
    sb.append("}\n");
    sb.append("}\n");
    sb.append("</script>\n");
    sb.append("</head>\n");
    sb.append("<body>\n");
    sb.append("<div id=\"popup\" style=\"display:none;position:absolute;cursor:default;color:yellow;pointer-events:none;\"></div>\n");
    sb.append("<div id=\"mainDiv\" class=\"c\">\n");
    sb.append("<h1>Terminal Unit Report</h1>\n");
    sb.append("<button class=\"e\" onclick=\"reloadData()\">Reevaluate Data Tolerances</button>\n");
    sb.append("<button class=\"e\" onclick=\"toggleGraphs()\">Toggle Graph Visibility</button>\n");
    sb.append("<a class=\"e\" id=\"exportDataButton\" href=\"#\" download=\"data.json\" onclick=\"prepareDataExport()\">Export Data</a>\n");
    sb.append("<br>\n");
    sb.append("<div class=\"divGrouping\">\n");
    sb.append("<input type=\"checkbox\" id=\"useMaxCooling\">\n");
    sb.append("<label for=\"useMaxCooling\" id=\"useMaxCoolingLabel\">Should Damper Airflow Exceed Maximum Cooling Design Parameter?</label>\n");
    sb.append("</div>\n");
    sb.append("<br id=\"damperMarginBreak\">\n");
    sb.append("<div class=\"divGrouping\">\n");
    sb.append("<label for=\"damperMargin\" id=\"damperMarginLabel\"></label>\n");
    sb.append("<input type=\"range\" style=\"width:35vw\" min=\"0\" max=\"15\" step=\"0.1\" value=\"3\" id=\"damperMargin\" oninput=\"damperMarginLabel.innerText='Damper Airflow Tolerance ('+Number(damperMargin.value).toFixed(1)+'%):'\">\n");
    sb.append("</div>\n");
    sb.append("<br id=\"heatAOMarginBreak\">\n");
    sb.append("<div class=\"divGrouping\">\n");
    sb.append("<label for=\"heatAOMargin\" id=\"heatAOMarginLabel\"></label>\n");
    sb.append("<input type=\"range\" style=\"width:35vw\" min=\"0\" max=\"50\" step=\"1\" value=\"15\" id=\"heatAOMargin\" oninput=\"heatAOMarginLabel.innerText='Minimum Heating Differential ('+heatAOMargin.value+' \\u{00B0}F):'\">\n");
    sb.append("</div>\n");
    sb.append("<br id=\"coolMarginBreak\">\n");
    sb.append("<div class=\"divGrouping\">\n");
    sb.append("<label for=\"coolMargin\" id=\"coolMarginLabel\"></label>\n");
    sb.append("<input type=\"range\" style=\"width:35vw\" min=\"0\" max=\"50\" step=\"1\" value=\"10\" id=\"coolMargin\" oninput=\"coolMarginLabel.innerText='Minimum Cooling Differential ('+coolMargin.value+' \\u{00B0}F):'\">\n");
    sb.append("</div>\n");
    sb.append("<br id=\"erraticMarginBreak\">\n");
    sb.append("<div class=\"divGrouping\">\n");
    sb.append("<label for=\"erraticMargin\" id=\"erraticMarginLabel\"></label>\n");
    sb.append("<input type=\"range\" style=\"width:35vw\" min=\"3\" max=\"30\" step=\"0.2\" value=\"10\" id=\"erraticMargin\" oninput=\"erraticMarginLabel.innerText='Erratic Thermostat Threshhold ('+Number(erraticMargin.value).toFixed(1)+' \\u{00B0}F):'\">\n");
    sb.append("</div>\n");
    if (!exited){
      sb.append("<br><br>\n");
      sb.append("<div style=\"position:relative;top:0;left:15%;width:70%;height:1em\">\n");
      sb.append("<div class=\"bar\"></div>\n");
      sb.append(Utility.format("<div id=\"testsStartedBar\" class=\"bar\" style=\"background-color:indigo;width:$0%\"></div>\n", 100*this.testsStarted.get()/this.testsTotal));
      sb.append(Utility.format("<div id=\"testsCompletedBar\" class=\"bar\" style=\"background-color:blue;width:$0%\"></div>\n", 100*this.testsCompleted.get()/this.testsTotal));
      sb.append("</div>\n");
      sb.append("<br id=\"dataAfterBreak\">\n");
    }else if (this.test.isKilled()){
      sb.append("<h3 id=\"dataAfterBreak\" style=\"color:crimson\">Foribly Terminated Before Natural Completion</h3>\n");
    }else{
      sb.append("<br id=\"dataAfterBreak\">\n");
    }
    sb.append("</div>\n");
    sb.append("<br><br>\n");
    sb.append("<script>\n");
    sb.append("damperMargin.oninput();\n");
    sb.append("heatAOMargin.oninput();\n");
    sb.append("coolMargin.oninput();\n");
    sb.append("erraticMargin.oninput();\n");
    sb.append("var graphsVisible = false;\n");
    sb.append(Utility.format("var testFans = $0;\n", testFans));
    sb.append(Utility.format("var testDampers = $0;\n", testDampers));
    sb.append(Utility.format("var testHeatAO = $0;\n", testHeating));
    sb.append("if (!testDampers){\n");
    sb.append("useMaxCooling.style.display = \"none\";\n");
    sb.append("useMaxCoolingLabel.style.display = \"none\";\n");
    sb.append("damperMarginBreak.style.display = \"none\";\n");
    sb.append("damperMargin.style.display = \"none\";\n");
    sb.append("damperMarginLabel.style.display = \"none\";\n");
    sb.append("}\n");
    sb.append("if (!testHeatAO){\n");
    sb.append("heatAOMarginBreak.style.display = \"none\";\n");
    sb.append("heatAOMargin.style.display = \"none\";\n");
    sb.append("heatAOMarginLabel.style.display = \"none\";\n");
    sb.append("coolMarginBreak.style.display = \"none\";\n");
    sb.append("coolMargin.style.display = \"none\";\n");
    sb.append("coolMarginLabel.style.display = \"none\";\n");
    sb.append("erraticMarginBreak.style.display = \"none\";\n");
    sb.append("erraticMargin.style.display = \"none\";\n");
    sb.append("erraticMarginLabel.style.display = \"none\";\n");
    sb.append("}\n");
    sb.append("var DATA=");
    printData(sb,false);
    sb.append(";\ndraw(mainDiv,DATA);\n");
    if (!exited){
      sb.append("const f = function(){\n");
      sb.append("const req = new XMLHttpRequest();\n");
      sb.append("req.open(\"GET\",window.location.href+\"&AJAX\");\n");
      sb.append("req.onreadystatechange = function(){\n");
      sb.append("if (this.readyState===4){\n");
      sb.append("if (this.status===200){\n");
      sb.append("try{\n");
      sb.append("const ret = JSON.parse(this.responseText);\n");
      sb.append("DATA = ret[\"data\"];\n");
      sb.append("if (testsStartedBar){\n");
      sb.append("testsStartedBar.style.width = String(ret[\"started\"])+'%';\n");
      sb.append("}\n");
      sb.append("if (testsCompletedBar){\n");
      sb.append("testsCompletedBar.style.width = String(ret[\"completed\"])+'%';\n");
      sb.append("}\n");
      sb.append("reloadData();\n");
      sb.append("}catch(e){\n");
      sb.append("console.error(e);\n");
      sb.append("}\n");
      sb.append("setTimeout(f, 60000);\n");
      sb.append("}else if (this.status===404){\n");
      sb.append("window.location.reload();\n");
      sb.append("}\n");
      sb.append("}\n");
      sb.append("};\n");
      sb.append("req.send();\n");
      sb.append("};\n");
      sb.append("setTimeout(f, 60000);\n");
    }
    sb.append("</script>\n");
    sb.append("</body>\n");
    sb.append("</html>");
    return sb.toString();
  }
  @Override public void updateAJAX(HttpServletRequest req, HttpServletResponse res) throws Throwable {
    res.setContentType("text/plain");
    final StringBuilder sb = new StringBuilder(4096);
    sb.append(Utility.format("{\"started\":$0,\"completed\":$1,\"data\":", 100*this.testsStarted.get()/this.testsTotal, 100*this.testsCompleted.get()/this.testsTotal));
    printData(sb,true);
    sb.append('}');
    res.getWriter().print(ExpansionUtils.expandLinks(sb.toString(),req));
  }
  private void printData(final StringBuilder sb, final boolean JSON){
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
    if (list.size()==0){
      sb.append("[]");
    }else{
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
          sb.append(Utility.format("{\n\"name\":\"$0\",\n\"equipment\":[\n", escape(JSON,(String)this.mapping.groupNames.getOrDefault(group,"(Deleted Group)"))));
        }
        d.print(sb,JSON);
      }
      sb.append("]}]");
    }
  }
  private static String escape(boolean JSON, String s){
    return JSON?Utility.escapeJSON(s):Utility.escapeJS(s);
  }
  class Data implements Comparable<Object> {
    private final static String tagAirflow = "airflow";
    private final static String tagAirflowMaxHeat = "airflow_max_heat";
    private final static String tagAirflowMaxCool = "airflow_max_cool";
    private final static String tagAirflowLockFlag = "airflow_lock_flag";
    private final static String tagAirflowLockValue = "airflow_lock_value";
    private final static String tagDamperLockFlag = "damper_lock_flag";
    private final static String tagDamperLockValue = "damper_lock_value";
    private final static String tagDamperPosition = "damper_position";
    private final static String tagEAT = "eat";
    private final static String tagLAT = "lat";
    private final static String tagSFSSLockFlag = "sfss_lock_flag";
    private final static String tagSFSSLockValue = "sfss_lock_value";
    private final static String tagSFST = "sfst";
    private final static String tagHeatAOLockFlag = "heating_AO_lock_flag";
    private final static String tagHeatAOLockValue = "heating_AO_lock_value";
    private final static String tagHeatAOPosition = "heating_AO_position";
    private final static String tagPumpStatus = "pump_status";
    private final static String tagPumpCmdLockFlag = "pump_cmd_lock_flag";
    private final static String tagPumpCmdLockValue = "pump_cmd_lock_value";
    private final static String tagPumpRevLockFlag = "pump_rev_lock_flag";
    private final static String tagPumpRevLockValue = "pump_rev_lock_value";
    private final static double lowLimit = 40.0;
    private final static double highLimit = 120.0;
    public volatile ResolvedTestingUnit x;
    public volatile int group;
    public volatile String path;
    public volatile String link;
    public volatile long start = -1;
    public volatile long end = -1;
    public volatile boolean hasFan;
    public volatile boolean hasDamper;
    public volatile boolean hasHeatAO;
    public volatile boolean hasEAT;
    public volatile boolean hasPump;
    public volatile boolean fanError = false;
    public volatile boolean fanStopTest = false;
    public volatile boolean fanStartTest = false;
    public volatile boolean damperError = false;
    public volatile boolean damperResponse = true;
    public final static int[] airflowX = new int[]{0,5,10,15,20,25,30,35,40,45,50,55,60,65,70,75,80,85,90,95,100};
    public volatile Stats[] airflowY;
    private final static int airflowSamples = 5;
    public volatile double airflowMaxCoolValue = 0;
    public volatile double airflowMaxHeatValue = 0;
    public volatile boolean heatingError = false;
    public volatile boolean heatingFailure = false;
    public volatile String heatingFailMsg = "Failure";
    public volatile double[] heatingX;
    public volatile double[] heatingY;
    public volatile int heatingSamples;
    public Data(ResolvedTestingUnit x) throws Throwable {
      start = System.currentTimeMillis();
      this.x = x;
      group = x.getGroup();
      path = x.getDisplayPath();
      link = x.getPersistentLink();
      Boolean b;
      String s=null,t=null;
      Stats st;
      String airflowMaxHeat = null;
      String airflowMaxCool = null;
      hasFan = x.hasMapping(tagSFSSLockFlag)
        && x.hasMapping(tagSFSSLockValue)
        && x.hasMapping(tagSFST);
      hasDamper = x.hasMapping(tagAirflow)
        && x.hasMapping(tagAirflowLockFlag)
        && x.hasMapping(tagAirflowLockValue)
        && x.hasMapping(tagAirflowMaxCool)
        && x.hasMapping(tagAirflowMaxHeat)
        && x.hasMapping(tagDamperLockFlag)
        && x.hasMapping(tagDamperLockValue)
        && x.hasMapping(tagDamperPosition);
      hasHeatAO = x.hasMapping(tagHeatAOLockFlag)
        && x.hasMapping(tagHeatAOLockValue)
        && x.hasMapping(tagHeatAOPosition)
        && x.hasMapping(tagLAT);
      hasPump = x.hasMapping(tagPumpCmdLockFlag)
        && x.hasMapping(tagPumpCmdLockValue)
        && x.hasMapping(tagPumpRevLockFlag)
        && x.hasMapping(tagPumpRevLockValue)
        && x.hasMapping(tagPumpStatus)
        && x.hasMapping(tagLAT);
      hasEAT = x.hasMapping(tagEAT);
      if (hasFan && testFans || hasDamper && testDampers){
        // If we're going to test fans or dampers, first we make an attempt to shut-off all heating and cooling elements
        if (hasHeatAO){
          x.setValueAutoMark(tagHeatAOLockValue,0);
          x.setValueAutoMark(tagHeatAOLockFlag,true);
        }
        if (hasPump){
          x.setValueAutoMark(tagPumpCmdLockValue,0);
          x.setValueAutoMark(tagPumpCmdLockFlag,true);
        }
        if (hasHeatAO){
          Thread.sleep(30000);
        }
        if (hasPump){
          waitFor(180000, 3000, tagPumpStatus, new Predicate<Object>(){
            public boolean test(Object s){
              return s.equals("0");
            }
          });
        }
      }
      if (hasFan && testFans) fanTest: {
        // Test the fan by locking it off and then on
        if (!x.setValueAutoMark(tagSFSSLockValue, 0) || !x.setValueAutoMark(tagSFSSLockFlag, true)){
          fanError = true; break fanTest;
        }
        b = waitFor(180000, 3000, tagSFST, new Predicate<Object>(){
          public boolean test(Object s){
            return s.equals("0");
          }
        });
        if (b==null){
          fanError = true; break fanTest;
        }
        fanStopTest = b;
        if (!x.setValueAutoMark(tagSFSSLockValue, 1)){
          fanError = true; break fanTest;
        }
        b = waitFor(180000, 3000, tagSFST, new Predicate<Object>(){
          public boolean test(Object s){
            return s.equals("1");
          }
        });
        if (b==null){
          fanError = true; break fanTest;
        }
        fanStartTest = b;
      } else if (hasFan && (hasDamper && testDampers || hasHeatAO && testHeating || hasPump && testHeating)){
        // If we're going to test dampers or heating/cooling elements, then we lock the fan on
        if (x.setValueAutoMark(tagSFSSLockValue, 1) && x.setValueAutoMark(tagSFSSLockFlag, true)){
          b = waitFor(180000, 3000, tagSFST, new Predicate<Object>(){
            public boolean test(Object s){
              return s.equals("1");
            }
          });
          if (b!=null && b){
            fanStartTest = true;
          }
        }
      }
      if (hasDamper && testDampers) damperTest: {
        // Test the damper by locking it to 0%, and then measuring the airflow at 5% increment intervals
        try{
          if (airflowMaxCool==null && (airflowMaxCool=x.getValue(tagAirflowMaxCool))==null){
            damperError = true; break damperTest;
          }
          airflowMaxCoolValue = Double.parseDouble(airflowMaxCool);
          airflowY = new Stats[airflowX.length];
          for (int i=0;i<airflowX.length;++i){
            final int pos = airflowX[i];
            if (i==0){
              if (!x.setValueAutoMark(tagDamperLockValue, pos) || !x.setValueAutoMark(tagDamperLockFlag, true)){
                damperError = true; break damperTest;
              }
            }else{
              if (!x.setValueAutoMark(tagDamperLockValue, pos)){
                damperError = true; break damperTest;
              }
            }
            b = waitFor(240000, 3000, tagDamperPosition, new Predicate<Object>(){
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
      if (hasHeatAO && testHeating) heatTest: {
        // Test heating AO by locking to 0%, waiting for stabilization, locking to 100%, and measuring the temperature at 10 second intervals
        // If a damper exists, we lock it to the heating maximum airflow parameter
        // We ensure there is sufficient airflow at every interval of the test (e.g, to prevent fires / overheating)
        try{
          if (!fanStartTest && !hasDamper){
            heatingFailure = true;
            heatingFailMsg = "Loss of Airflow";
            break heatTest;
          }
          if ((s=x.getValue(tagHeatAOPosition))==null){
            heatingError = true; break heatTest;
          }
          final double initialPosition = Double.parseDouble(s);
          if (!x.setValueAutoMark(tagHeatAOLockValue,0) || !x.setValueAutoMark(tagHeatAOLockFlag,true)){
            heatingError = true; break heatTest;
          }
          if (hasDamper){
            final long lim = System.currentTimeMillis()+30000L+(long)(initialPosition*6000);
            if (airflowMaxHeat==null && (airflowMaxHeat=x.getValue(tagAirflowMaxHeat))==null){
              heatingError = true; break heatTest;
            }
            airflowMaxHeatValue = Math.max(Double.parseDouble(airflowMaxHeat),150);
            if (!x.setValueAutoMark(tagAirflowLockValue, airflowMaxHeatValue) || !x.setValueAutoMark(tagDamperLockFlag, false) || !x.setValueAutoMark(tagAirflowLockFlag, true)){
              heatingError = true; break heatTest;
            }
            waitFor(240000, 3000, tagAirflow, new Predicate<Object>(){
              public boolean test(Object s){
                return Math.abs(Double.parseDouble(s.toString())-airflowMaxHeatValue)<50;
              }
            });
            final long dif = lim-System.currentTimeMillis();
            if (dif>0){
              Thread.sleep(dif);
            }
          }else{
            Thread.sleep(30000L+(long)(initialPosition*6000));
          }
          s = "0";
          if (hasEAT && (s=x.getValue(tagEAT))==null || (t=x.getValue(tagLAT))==null){
            heatingError = true; break heatTest;
          }
          final long startTime = System.currentTimeMillis();
          heatingX = new double[61];
          heatingY = new double[heatingX.length];
          heatingSamples = heatingX.length;
          heatingX[0] = 0;
          heatingY[0] = Double.parseDouble(t)-Double.parseDouble(s);
          if (!x.setValueAutoMark(tagHeatAOLockValue,100)){
            heatingError = true; break heatTest;
          }
          double yMax = 0;
          double yAvg = 0;
          double lat = 0;
          long time = 0;
          for (int i=1,j;i<heatingX.length;++i){
            Thread.sleep(10000L);
            time = System.currentTimeMillis();
            if (hasEAT && (s=x.getValue(tagEAT))==null || (t=x.getValue(tagLAT))==null){
              heatingError = true; break heatTest;
            }else if ((b=checkAirflow())==null){
              heatingError = true; break heatTest;
            }else if (!b){
              heatingFailure = true;
              heatingFailMsg = "Loss of Airflow";
              break heatTest;
            }
            lat = Double.parseDouble(t);
            heatingX[i] = (time-startTime)/60000.0;
            heatingY[i] = lat-Double.parseDouble(s)-heatingY[0];
            if (lat>highLimit){
              heatingSamples = i+1;
              break;
            }
            if (heatingY[i]>yMax){
              yMax = heatingY[i];
            }
            if (i>21){
              yAvg = 0;
              for (j=0;j<8;++j){
                yAvg+=heatingY[i-j-3];
              }
              yAvg/=8;
              if (yAvg+yMax/50>(heatingY[i]+heatingY[i-1]+heatingY[i-2])/3){
                heatingSamples = i+1;
                break;
              }
            }
          }
          heatingY[0] = 0;
        }catch(NumberFormatException e){
          Initializer.log(e);
          heatingError = true;
        }finally{
          x.setValueAutoMark(tagHeatAOLockValue,0);
        }
      }
      if (hasPump && testHeating && !hasHeatAO) heatTest: {
        try{
          if (!fanStartTest && !hasDamper){
            heatingFailure = true;
            heatingFailMsg = "Loss of Airflow";
            break heatTest;
          }
          if (!x.setValueAutoMark(tagPumpRevLockValue,0) || !x.setValueAutoMark(tagPumpRevLockFlag,true) || !x.setValueAutoMark(tagPumpCmdLockValue,0) || !x.setValueAutoMark(tagPumpCmdLockFlag,true)){
            heatingError = true; break heatTest;
          }
          b = waitFor(180000, 3000, tagPumpStatus, new Predicate<Object>(){
            public boolean test(Object s){
              return s.equals("0");
            }
          });
          if (b==null){
            heatingError = true; break heatTest;
          }else if (!b){
            heatingFailure = true;
            heatingFailMsg = "Compressor Stop Failure";
            break heatTest;
          }
          Thread.sleep(480000L);
          s = "0";
          heatingX = new double[181];
          heatingY = new double[heatingX.length];
          heatingSamples = heatingX.length;
          heatingX[0] = 0;
          heatingY[0] = 0;
          for (int i=0;i<4;++i){
            Thread.sleep(3000);
            if (hasEAT && (s=x.getValue(tagEAT))==null || (t=x.getValue(tagLAT))==null){
              heatingError = true; break heatTest;
            }
            heatingY[0]+=Double.parseDouble(t)-Double.parseDouble(s);
          }
          heatingY[0]/=4;
          final long startTime = System.currentTimeMillis();
          if (!x.setValueAutoMark(tagPumpCmdLockValue,1)){
            heatingError = true; break heatTest;
          }
          String u = "";
          int pumpStart = -1;
          int swap = -1;
          boolean swapped = false;
          boolean firstCooling = true;
          double yMax=0;
          double yMin=0;
          double sum=0;
          double yAvg=0;
          double yAvg2=0;
          long time = 0;
          double lat = 0;
          for (int i=1,j;i<heatingX.length;++i){
            Thread.sleep(10000L);
            if (pumpStart==-1){
              if ((u=x.getValue(tagPumpStatus))==null){
                heatingError = true;
                break heatTest;
              }else if (u.equals("1")){
                pumpStart = i+30;
                swap = i+((180-i)>>1)-5;
              }else if (i>18){
                heatingFailure = true;
                heatingFailMsg = "Compressor Start Failure";
                break heatTest;
              }
            }
            if (!swapped && i>=swap && swap!=-1){
              swapped = true;
              swap = i+30;
              firstCooling = sum<=0;
              if (!x.setValueAutoMark(tagPumpRevLockValue,1)){
                heatingError = true; break heatTest;
              }
            }
            time = System.currentTimeMillis();
            if (hasEAT && (s=x.getValue(tagEAT))==null || (t=x.getValue(tagLAT))==null){
              heatingError = true; break heatTest;
            }else if ((b=checkAirflow())==null){
              heatingError = true; break heatTest;
            }else if (!b){
              heatingFailure = true;
              heatingFailMsg = "Loss of Airflow";
              break heatTest;
            }
            lat = Double.parseDouble(t);
            heatingX[i] = (time-startTime)/60000.0;
            heatingY[i] = lat-Double.parseDouble(s)-heatingY[0];
            if (pumpStart!=-1 && (lat>highLimit || lat<lowLimit)){
              if (swapped){
                heatingSamples = i+1;
                break;
              }else{
                swapped = true;
                swap = i+30;
                firstCooling = sum<=0;
                if (!x.setValueAutoMark(tagPumpRevLockValue,1)){
                  heatingError = true; break heatTest;
                }
              }
            }
            sum+=heatingY[i];
            if (heatingY[i]>yMax){
              yMax = heatingY[i];
            }
            if (heatingY[i]<yMin){
              yMin = heatingY[i];
            }
            if (!swapped && i>pumpStart && pumpStart!=-1 || swapped && i>swap){
              yAvg = 0;
              for (j=0;j<10;++j){
                yAvg+=heatingY[i-j-5];
              }
              yAvg/=10;
              yAvg2 = (heatingY[i]+heatingY[i-1]+heatingY[i-2]+heatingY[i-3]+heatingY[i-4])/5;
              if (swapped){
                if (firstCooling && yAvg+yMax/50>yAvg2 || !firstCooling && yAvg+yMin/50<yAvg2){
                  heatingSamples = i+1;
                  break;
                }
              }else{
                if (sum>0 && yAvg+yMax/50>yAvg2 || sum<=0 && yAvg+yMin/50<yAvg2){
                  swapped = true;
                  swap = i+30;
                  firstCooling = sum<=0;
                  if (!x.setValueAutoMark(tagPumpRevLockValue,1)){
                    heatingError = true; break heatTest;
                  }
                }
              }
            }
          }
          heatingY[0] = 0;
        }catch(NumberFormatException e){
          Initializer.log(e);
          heatingError = true;
        }finally{
          x.setValueAutoMark(tagPumpCmdLockValue,0);
        }
      }
      end = System.currentTimeMillis();
    }
    private Boolean checkAirflow() throws Throwable {
      if (hasFan || hasDamper){
        String s=null;
        for (int i=0;i<3;++i){
          if (hasFan){
            if ((s=x.getValue(tagSFST))==null){
              return null;
            }
            if (s.equals("1")){
              return true;
            }
          }
          if (hasDamper){
            if ((s=x.getValue(tagAirflow))==null){
              return null;
            }
            if (Double.parseDouble(s)>90){
              return true;
            }
          }
          Thread.sleep(2000);
        }
      }
      return false;
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
    public void print(final StringBuilder sb, final boolean JSON){
      boolean first;
      int i,j;
      sb.append("{\n");
      sb.append(Utility.format("\"path\":\"$0\",\n", escape(JSON,path)));
      sb.append(Utility.format("\"link\":\"$0\",\n", link));
      sb.append(Utility.format("\"start\":\"$0\",\n", escape(JSON,Utility.getDateString(start))));
      sb.append(Utility.format("\"stop\":\"$0\",\n", escape(JSON,Utility.getDateString(end))));
      sb.append("\"duration\":").append((end-start)/60000.0);
      if (hasFan && testFans){
        sb.append(",\n\"fan\":{\n");
        sb.append("\"error\":").append(fanError);
        if (!fanError){
          sb.append(",\n\"start\":").append(fanStartTest);
          sb.append(",\n\"stop\":").append(fanStopTest);
        }
        sb.append("\n}");
      }
      if (hasDamper && testDampers){
        sb.append(",\n\"damper\":{\n");
        sb.append("\"error\":").append(damperError);
        if (!damperError){
          sb.append(",\n\"response\":").append(damperResponse);
          if (damperResponse){
            sb.append(",\n\"maxCool\":").append(airflowMaxCoolValue);
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
      if (hasHeatAO && testHeating || hasPump && testHeating){
        sb.append(",\n\"heatAO\":{\n");
        sb.append("\"error\":").append(heatingError);
        sb.append(",\n\"failure\":").append(heatingFailure);
        sb.append(",\n\"failMsg\":\"").append(escape(JSON,heatingFailMsg)).append('"');
        if (!heatingError && !heatingFailure){
          sb.append(",\n\"cooling\":").append(hasPump && !hasHeatAO);
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