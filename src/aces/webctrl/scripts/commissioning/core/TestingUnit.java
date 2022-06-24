package aces.webctrl.scripts.commissioning.core;
public class TestingUnit implements Comparable<TestingUnit> {
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
  public void serialize(ByteBuilder b){
    b.write(ID);
    b.write(groupID);
  }
  public static TestingUnit deserialize(SerializationStream s){
    return new TestingUnit(s.readString(),s.readInt());
  }
  @Override public int hashCode(){
    return groupID+31*ID.hashCode();
  }
  @Override public int compareTo(TestingUnit tu){
    if (groupID==tu.groupID){
      return ID.compareTo(tu.ID);
    }else{
      return groupID-tu.groupID;
    }
  }
}
