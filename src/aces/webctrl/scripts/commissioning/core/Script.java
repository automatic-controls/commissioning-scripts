package aces.webctrl.scripts.commissioning.core;
public interface Script {
  public void run(ResolvedTestingUnit ctx, Test test) throws Throwable;
}