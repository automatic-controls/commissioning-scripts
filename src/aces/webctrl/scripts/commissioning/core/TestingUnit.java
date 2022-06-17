package aces.webctrl.scripts.commissioning.core;
public class TestingUnit {
  private volatile String ID;
  private volatile int groupID;
  public volatile boolean resolveSuccess = true;
  public TestingUnit(String ID, int groupID){
    this.ID = ID;
    this.groupID = groupID;
  }
  public String getID(){
    return ID;
  }
  public int getGroup(){
    return groupID;
  }
}
