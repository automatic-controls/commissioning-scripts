package aces.webctrl.scripts.commissioning.core;
public class GroupTracker {
  public volatile int size = 1;
  private volatile int num = 0;
  private volatile int running = 0;
  private volatile int completed = 0;
  private volatile int maxRunning = 1;
  public GroupTracker(int num){
    this.num = num;
  }
  public int getIndex(){
    return num;
  }
  public int getMaxRunning(){
    return maxRunning;
  }
  /**
   * It is assumed that {@code size} is correctly configured before invoking this method.
   */
  public void init(double maxPercentage){
    maxRunning = Math.max(1, (int)(size*maxPercentage));
  }
  /**
   * Invoked to determine whether it is okay to start another test in this group.
   */
  public synchronized boolean start(){
    if (completed<size && running<maxRunning){
      ++running;
      return true;
    }else{
      return false;
    }
  }
  /**
   * Invoked whenever a test is completed.
   */
  public synchronized void complete(){
    if (running>0){
      ++completed;
      --running;
    }
  }
  /**
   * @return whether any test in this group is currently active.
   */
  public boolean hasRunning(){
    return running>0;
  }
}