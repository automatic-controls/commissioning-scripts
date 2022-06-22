package aces.webctrl.scripts.commissioning.core;
/**
 * User scripts should extend this class and override the relevant methods.
 * {@code init()} is invoked before any tests are performed.
 * At a minimum, you should specify required tag mappings by invoking {@link Test#addRequiredTag(String)} on the instance variable {@code test} during this step.
 * After initialization, {@code exec()} will be invoked once for each piece of equipment being tested.
 * Multiple threads are typically used, so be sure {@code exec()} is thread-safe for many concurrent invokations.
 * {@code exit()} is invoked after all tests are complete.
 * Once {@code init()} is invoked, you can assume {@code exit()} will be eventually called, even if errors occur elsewhere.
 * <p>
 * Scripts provide output in the form of a text/html document with {@link #getOutput()}.
 * Users can preview the current state of the text/html document as the test is executing.
 * After all tests are complete, the final output document is archived for future reference.
 */
public abstract class Script {
  /**
   * Global context used to encapsulate all control programs this script will be invoked upon.
   * This variable will be populated after instance construction but before {@link #init()} is invoked.
   */
  public volatile Test test = null;
  /**
   * Invoked before any tests are executed.
   * It is suggested that this method makes a few invokations of
   * {@link Test#addRequiredTag(String)}.
   * It is generally guaranteed that an invokation of this method will be eventually followed by an invokation of {@link #exit()},
   * so any necessary clean-up code can be placed there.
   */
  public void init() throws Throwable {}
  /**
   * Invoked once for every control program the script should be executed upon.
   * This method may be concurrently invoked in separate threads, so please be mindful of thread-safety.
   * @param ctx is a local context variable which represents one control program with semantic tag mappings.
   */
  public abstract void exec(ResolvedTestingUnit ctx) throws Throwable;
  /**
   * Invoked after all tests are complete.
   */
  public void exit() throws Throwable {}
  /**
   * Resetting a control program helps to ensure changes made during tests are temporary.
   * However, each script must still mark the values it wants to keep for resets using
   * {@link #markAndGetValue(String)} and {@link #markAndSetValue(String, Object)}.
   * By default, this method returns {@code true}.
   * @return whether to automatically invoke {@link ResolvedTestingUnit#reset(String)} for every tag at the end of each test.
   */
  public boolean autoReset(){
    return true;
  }
  /**
   * The information returned by this method will be displayed to users when choosing a script to run.
   * @return some descriptive detail for this script.
   */
  public abstract String getDescription();
  /**
   * This method should be overridden to provide feedback to users.
   * Expect this method to be invoked concurrently by multiple threads while the test is executing.
   * As such, please be mindful of thread-safety and synchronize appropriately.
   * After all tests are completed and {@link #exit()} has been called, this method will be invoked once,
   * and the resulting HTML document will be archived as the final result of this test.
   * @return an HTML document to be displayed to the user which describes the status of this test.
   * {@code null} is also an acceptable return value.
   */
  public String getOutput(){
    return null;
  }
}