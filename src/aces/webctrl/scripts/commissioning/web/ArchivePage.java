package aces.webctrl.scripts.commissioning.web;
import aces.webctrl.scripts.commissioning.core.*;
import javax.servlet.http.*;
public class ArchivePage extends ServletBase {
  @Override public void exec(final HttpServletRequest req, final HttpServletResponse res) throws Throwable {
    final StringBuilder sb = new StringBuilder(ArchivedTest.instances.size()<<7);
    for (ArchivedTest at:ArchivedTest.instances.values()){
      sb.append("<tr>");
      sb.append("\n<td>").append(Utility.escapeHTML(at.getScriptName())).append("</td>");
      sb.append("\n<td>").append(Utility.escapeHTML(at.getOperator())).append("</td>");
      sb.append("\n<td>").append(Utility.escapeHTML(Utility.getDateString(at.getStart()))).append("</td>");
      sb.append("\n<td>").append(Utility.escapeHTML(Utility.getDateString(at.getEnd()))).append("</td>");
      sb.append("\n<td>").append(at.getThreads()).append("</td>");
      sb.append("\n<td>").append((int)(at.getMaxTests()*100)).append("%</td>");
      sb.append("\n<td>\n");
      at.getParams().forEach(new java.util.function.BiConsumer<String,Boolean>(){
        public void accept(String key, Boolean b){
          sb.append(b?"<div class=\"tagYes\">":"<div class=\"tagNo\">").append(Utility.escapeHTML(key)).append("</div>\n");
        }
      });
      sb.append("</td>");
      sb.append("\n<td><a href=\"").append(req.getContextPath()).append("/ArchiveOutput?ID=").append(at.ID).append("\"></a></td>");
      sb.append("\n</tr>\n");
    }
    res.setContentType("text/html");
    res.getWriter().print(getHTML(req).replace("__ROWS__", sb));
  }
}