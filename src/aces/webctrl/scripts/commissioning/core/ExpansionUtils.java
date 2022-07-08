package aces.webctrl.scripts.commissioning.core;
import com.controlj.green.addonsupport.web.*;
import java.util.regex.*;
import java.util.function.*;
import javax.servlet.http.*;
public class ExpansionUtils {
  public final static Pattern PERSISTENT_LINK = Pattern.compile("@\\{getPersistentLink\\(([a-zA-Z0-9:]++)\\)\\}");
  public static String nullifyLinks(String s){
    return PERSISTENT_LINK.matcher(s).replaceAll("#");
  }
  public static String expandLinks(String s, HttpServletRequest req){
    if (req==null){
      return nullifyLinks(s);
    }
    return PERSISTENT_LINK.matcher(s).replaceAll(new Function<MatchResult,String>(){
      public String apply(MatchResult m){
        try{
          return Matcher.quoteReplacement(Link.createLink(UITree.GEO, m.group(1)).getURL(req));
        }catch(Throwable t){
          return "#";
        }
      }
    });
  }
}