package aces.webctrl.scripts.commissioning.core;
import javax.servlet.http.*;
import java.util.*;
import java.util.concurrent.atomic.*;
/**
 * User scripts should extend this class and override the relevant methods.
 * One {@code Script} instance is used for each set of tests.
 * The lifetime of a {@code Script} object looks something like:
 * <pre>{@code
 * Script s = new Script();
 * s.getDescription();
 * s.getParamNames();
 * s.test = ...;
 * s.mapping = ...;
 * s.params = ...;
 * s.schedule = ...;
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
 * ArchivedTest.save(s.getOutput(false));
 * ScheduledTest.onComplete(s.getOutput(true), s.isEmailCSV());
 * s.close();
 * s = null;
 * }</pre>
 * The {@code for} loop over the {@code ResolvedTestingUnit} collection is shown for illustrative purposes only.
 * In actuality, multiple threads will concurrently loop and invoke {@link #exec(ResolvedTestingUnit)} simutaneously.
 * Also, {@link #getOutput(boolean) getOutput(false)} and {@link #updateAJAX(HttpServletRequest, HttpServletResponse)} may be concurrently invoked at any time during a {@code Script} object's lifetime.
 * Refer to individual field and method Javadocs for more information.
 */
public class Script implements AutoCloseable {
  /** Contains a reference to the object which controls this script's execution. */
  public volatile Test test = null;
  /** Contains a reference to the mapping used to resolve testing units for this script. */
  public volatile Mapping mapping = null;
  /** Contains references to all the control programs and semantic tag mappings which will be tested by this script. */
  public volatile Collection<ResolvedTestingUnit> units = null;
  /** The schedule which triggered this script to execute, or {@code null} if this script was manually triggered. */
  public volatile ScheduledTest schedule = null;
  /**
   * Contains user-supplied parameters given at initialization (read-only).
   * Parameter names (mapping keys) are specified by {@link #getParamNames()}.
   */
  public volatile Map<String,Boolean> params = null;
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
   * @return an array of names for parameters that should be passed to this script.
   * These parameters will be displayed to users as a list of checkboxes.
   */
  public String[] getParamNames() throws Throwable { return null; }
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
   * This method should periodically check {@link Test#isKilled() test.isKilled()} inbetween expensive operations
   * to determine whether a user is attempting to forcibly stop this test (in which case, please {@code return} immediately).
   * The current thread's interrupted status will also be set in such a case.
   * @param ctx is a context variable representing one control program with semantic tag mappings.
   * @see Thread#interrupted()
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
   * {@link ResolvedTestingUnit#markAndGetValue(String) ctx.markAndGetValue(tag)} and {@link ResolvedTestingUnit#markAndSetValue(String, Object) ctx.markAndSetValue(tag, value)}.
   * By default, this method returns {@code true}.
   * @return whether to automatically reset control program node values at the end of each test.
   */
  public boolean autoReset() throws Throwable {
    return true;
  }
  /**
   * The information returned by this method will be displayed to users when viewing scripts.
   * You may include HTML tags in the returned {@code String}.
   * @return some descriptive detail for this script.
   */
  public String getDescription() throws Throwable {
    return null;
  }
  /**
   * This method should be overridden to provide feedback to users.
   * After {@link #exit()} has been called, this method will be invoked,
   * and the resulting HTML document will be archived as the final result of this test.
   * If this script is running on a schedule, the finished document will be emailed to all specified recipients.
   * This method may be concurrently invoked in separate threads at any time during the lifetime of this script, so please be mindful of thread-safety.
   * @param email whether the returned {@code String} will be emailed from a schedule.
   * @return an HTML document to be displayed to the user which includes the status and/or results of this test.
   * {@code null} is also an acceptable return value.
   * @see #isEmailCSV()
   * @see ResolvedTestingUnit#getPersistentLink()
   */
  public String getOutput(boolean email) throws Throwable {
    return null;
  }
  /**
   * This method may be overridden to provide asynchronous updates to the output of actively executing scripts.
   * AJAX requests submitted to {@code window.location.href+"&AJAX"} from within the HTML output will be handled by this method.
   * Response status errors 400, 401, 404 and 500 may be expected in addition to whatever response is provided here.
   * When a script becomes inactive, this method will no longer be invoked, and you should expect to receive a status of 404.
   */
  public void updateAJAX(HttpServletRequest req, HttpServletResponse res) throws Throwable {}
  /**
   * @return whether emailed reports should be sent as a CSV attachment.
   */
  public boolean isEmailCSV() throws Throwable {
    return false;
  }
  /**
   * Closes the {@code ClassLoader} associated with this {@code Script} when necessary.
   */
  @Override public void close() throws Exception {
    ClassLoader cl = Script.class.getClassLoader();
    if (cl instanceof AutoCloseable){
      // In particular, we're looking at java.net.URLClassLoader
      ((AutoCloseable)cl).close();
    }
  }
}