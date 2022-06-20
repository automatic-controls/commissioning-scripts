package aces.webctrl.scripts.commissioning.core;
/**
 * User scripts should extend this class and override {@link #init()}, {@link #exec(ResolvedTestingUnit)}, and {@link #exit()}.
 * {@code init()} is invoked before any tests are performed.
 * At a minimum, you should specify required tag mappings by invoking {@link Test#addRequiredTag(String)} on the instance variable {@code test} during this step.
 * After initialization, {@code exec()} will be invoked once for each piece of equipment being tested.
 * Multiple threads are typically used, so be sure {@code exec()} is thread-safe for many concurrent invokations.
 * {@code exit()} is invoked after every test is executed.
 * Once {@code init()} is invoked, you can assume {@code exit()} will be eventually called, even if errors occur elsewhere.
 * <p>
 * Scripts provide output in the form of a text/html document.
 * The prefix and postfix document should be specified within the {@code init()} method using {@link Test#setOutputPrefix(String)} and {@link Test#setOutputSuffix(String)}.
 * {@link Test#appendOutput(CharSequence)} may be used to add content to the middle of the document.
 * Users can preview the current state of the text/html document as the test is executing,
 * so be sure the document is left in a valid state after every change to the output.
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
   * {@link Test#addRequiredTag(String)}, {@link Test#setOutputPrefix(String)}, and {@link Test#setOutputSuffix(String)}.
   * It is generally guaranteed that an invokation of this method will be eventually followed by an invokation of {@link #exit()},
   * so any necessary clean-up code can be placed there.
   */
  public void init() throws Throwable {}
  /**
   * Invoked once for every control program the script should be executed upon.
   * @param ctx is a local context variable which represents one control program with semantic tag mappings.
   */
  public abstract void exec(ResolvedTestingUnit ctx) throws Throwable;
  /**
   * Invoked after every test has been executed.
   */
  public void exit() throws Throwable {}
  /**
   * Resetting a control program helps to ensure changes made during tests are temporary.
   * However, each script must still mark the values it wants to keep for resets using
   * {@link #markAndGetValue(String)} and {@link #markAndSetValue(String, Object)}.
   * By default, this method always returns {@code true}.
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
}