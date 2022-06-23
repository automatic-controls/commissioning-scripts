package aces.webctrl.scripts.commissioning.core;
import java.util.*;
import java.util.concurrent.atomic.*;
/**
 * User scripts should extend this class and override the relevant methods.
 * One {@code Script} instance is used for each set of tests.
 * The lifetime of a {@code Script} object looks something like:
 * <pre>{@code
 * Script s = new Script();
 * s.test = ...;
 * s.getDescription();
 * s.autoReset();
 * s.requireTags(new TreeSet<String>());
 * s.units = ...;
 * s.threads = ...;
 * s.maxTestsPerGroup = ...;
 * s.testsTotal = ...;
 * s.testsStarted = ...;
 * s.testsCompleted = ...;
 * s.init();
 * for (ResolvedTestingUnit rtu:s.units)&#123;
 *   s.exec(rtu);
 * &#125;
 * s.exit();
 * ArchivedTest.save(s.getOutput());
 * s = null;
 * }</pre>
 * The {@code for} loop over the {@code ResolvedTestingUnit} collection is shown for illustrative purposes only.
 * In actuality, multiple threads will concurrently loop and invoke {@link #exec(ResolvedTestingUnit)} simutaneously.
 * Also, {@link #getOutput()} may be concurrently invoked at any time during a {@code Script} object's lifetime.
 * Refer to individual field and method Javadocs for more information.
 */
public class Script {
  /** Contains a reference to the object which controls this script's execution. */
  public volatile Test test = null;
  /** Contains references to all the control programs and semantic tag mappings which will be tested by this script. */
  public volatile Collection<ResolvedTestingUnit> units = null;
  /** Number of threads being used to execute this script. */
  public volatile int threads = -1;
  /** Maximum percentage of control programs being testing at any given time within each grouping. */
  public volatile double maxTestsPerGroup = -1;
  /** Total number of control programs being tested by this script. */
  public volatile int testsTotal = -1;
  /**
   * Number of control programs whose test has been started.
   * The number of tests currently running is given by {@code testsStarted.get()-testsCompleted.get()}.
   */
  public volatile AtomicInteger testsStarted = null;
  /**
   * Number of control programs whose test has been completed.
   * The number of tests currently running is given by {@code testsStarted.get()-testsCompleted.get()}.
   */
  public volatile AtomicInteger testsCompleted = null;
  /**
   * This method should add required semantic tags to the given set.
   * Control programs with missing required semantic tag mappings will be ignored.
   * @param tags is the {@code Set<String>} where required tags should be added.
   */
  public void requireTags(Set<String> tags) throws Throwable {}
  /**
   * Invoked before any test is started.
   */
  public void init() throws Throwable {}
  /**
   * Invoked once for every control program the script should be executed upon.
   * This method may be concurrently invoked in separate threads, so please be mindful of thread-safety.
   * @param ctx is a context variable representing one control program with semantic tag mappings.
   */
  public void exec(ResolvedTestingUnit ctx) throws Throwable {}
  /**
   * Invoked after all tests are complete.
   */
  public void exit() throws Throwable {}
  /**
   * If this method returns {@code true}, then {@link ResolvedTestingUnit#reset(String) ctx.reset(null)}
   * will be called after each invokation of {@link #exec(ResolvedTestingUnit) exec(ResolvedTestingUnit ctx)}.
   * Resetting a control program helps to ensure changes made during tests are temporary.
   * Each script must mark nodes for auto-reset by using
   * {@link #markAndGetValue(String)} and {@link #markAndSetValue(String, Object)}.
   * By default, this method returns {@code true}.
   * @return whether to automatically reset control program node values at the end of each test.
   */
  public boolean autoReset() throws Throwable {
    return true;
  }
  /**
   * The information returned by this method will be displayed to users when viewing scripts.
   * @return some descriptive detail for this script.
   */
  public String getDescription() throws Throwable {
    return null;
  }
  /**
   * This method should be overridden to provide feedback to users.
   * After {@link #exit()} has been called, this method will be invoked once,
   * and the resulting HTML document will be archived as the final result of this test.
   * If this script is running on a schedule, the final archived document will be emailed to all specified recipients.
   * This method may be concurrently invoked in separate threads at any time during the lifetime of this script, so please be mindful of thread-safety.
   * @return an HTML document to be displayed to the user which includes the status and/or results of this test.
   * {@code null} is also an acceptable return value.
   */
  public String getOutput() throws Throwable {
    return null;
  }
}