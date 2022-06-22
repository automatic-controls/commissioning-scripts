package aces.webctrl.scripts.commissioning.core;
import com.controlj.green.addonsupport.access.node.*;
import java.util.regex.*;
/**
 * Maps a tag name to nodes using relative paths with the aid of regular expressions.
 * Tag names cannot be modified after creation because they are used for ordering collections.
 */
public class SemanticTag {
  private volatile String expr;
  private volatile String tag;
  private volatile Pattern pattern = null;
  public void serialize(ByteBuilder b){
    b.write(tag);
    b.write(expr);
  }
  public static SemanticTag deserialize(SerializationStream s) throws PatternSyntaxException {
    return new SemanticTag(s.readString(), s.readString());
  }
  /**
   * Constructs a new semantic tag with the given name and expression.
   */
  public SemanticTag(String tag, String expr) throws PatternSyntaxException {
    tag = Utility.V_SPACE.matcher(tag).replaceAll("");
    this.tag = tag;
    setExpression(expr);
  }
  /**
   * Sets the expression used to match nodes against this semantic tag.
   */
  public void setExpression(String expr) throws PatternSyntaxException {
    expr = Utility.V_SPACE.matcher(expr).replaceAll("");
    this.expr = expr;
    pattern = null;
    pattern = Pattern.compile(expr);
  }
  /**
   * @return the expression used to match nodes against this semantic tag.
   */
  public String getExpression(){
    return expr;
  }
  /**
   * @return the name for this semantic tag.
   */
  public String getTag(){
    return tag;
  }
  /**
   * Attempts to resolve this semantic tag against the provided node.
   * @return the resolved node which corresponds to this semantic tag, or {@code null} if no matches are found.
   */
  public Node resolve(Node n){
    Pattern p = pattern;
    return p==null?null:search(n,p,"");
  }
  /**
   * Utility method which uses partial regex matching to efficiently search a node tree for relative reference paths according to the given pattern.
   */
  private static Node search(Node n, Pattern p, String path){
    if (n==null){
      return null;
    }
    String tmp;
    Matcher matcher;
    Node ret;
    for (Node m:n.getChildren()){
      tmp = path+m.getReferenceName();
      matcher = p.matcher(tmp);
      if (matcher.matches()){
        return m;
      }else if (matcher.hitEnd()){
        tmp+='/';
        matcher = p.matcher(tmp);
        if (matcher.matches()){
          return m;
        }else if (matcher.hitEnd()){
          ret = search(m,p,tmp);
          if (ret!=null){
            return ret;
          }
        }
      }
    }
    return null;
  }
  @Override public int hashCode(){
    return expr.hashCode()^tag.hashCode();
  }
}