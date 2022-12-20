package aces.webctrl.scripts.commissioning.core;
import com.controlj.green.addonsupport.access.*;
import com.controlj.green.addonsupport.access.node.*;
import java.util.*;
public class LiteralNode implements Node {
  private volatile String value;
  public LiteralNode(String value){
    this.value = value;
  }
  @Override public String eval(String arg0){
    return null;
  }
  @Override public Node evalToNode(String arg0){
    return null;
  }
  @Override public List<Node> getChildren(){
    return null;
  }
  @Override public String getDisplayName(){
    return null;
  }
  @Override public String getDisplayName(Locale arg0){
    return null;
  }
  @Override public String getDisplayValue(){
    return value;
  }
  @Override public Node getParent(){
    return null;
  }
  @Override public String getReferenceName(){
    return null;
  }
  @Override public String getRelativeReferencePath(Node arg0){
    return null;
  }
  @Override public String getValue(){
    return value;
  }
  @Override public boolean hasParent(){
    return false;
  }
  @Override public Location resolveToLocation(Tree arg0){
    return null;
  }
  @Override public void setDisplayValue(String arg0){
    value = arg0;
  }
  @Override public void setValue(String arg0){
    value = arg0;
  }
}