/**
 * Class to convert test results to junit xml
 */

import junit.framework.Test
import junit.framework.TestResult

import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener
import org.junit.runner.Description
import org.junit.runner.Result

/**
 * Simple groovy implementation of the JUnitResultFormatterAsRunListener
 * It also captures stdout/stderr by intercepting the likes of System#out.
 */
class SimpleJUnitResultFormatterAsRunListener extends RunListener {
  private final File report_dir
  private ByteArrayOutputStream stdout
  private PrintStream old_stdout, old_stderr

  private long test_start_time

  private String suite_name
  private String suite_timestamp
  private int suite_error_count
  private Map suite_results

  SimpleJUnitResultFormatterAsRunListener(File report_dir) {
    this.report_dir = report_dir
  }

  @Override
  public void testRunStarted(Description description) throws Exception {
    suite_name = description.getChildren()[0].toString()
    suite_results = [:]
    suite_timestamp = new Date().format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", TimeZone.getTimeZone('UTC'))
    println("Suite started: ${suite_name}, tests: ${description.testCount()}")
  }

  @Override
  public void testStarted(Description description) throws Exception {
    println('')
    println("Test started: ${description.toString()}")
    suite_results[description.toString()] = [
      failures: [],
      errors: [],
    ]

    test_start_time = System.currentTimeMillis()

    // Capture the stdout/stderr
    old_stdout = System.out
    old_stderr = System.err
    System.setOut(new PrintStream(stdout = new ByteArrayOutputStream()))
    System.setErr(new PrintStream(stdout))
  }

  @Override
  public void testIgnored(Description description) throws Exception {
    println('')
    println("Ignored test: ${description.toString()} - ${description.getAnnotation(org.junit.Ignore.class)?.value()}")
    suite_results[description.toString()] = [
      name: description.toString(),
      skipped: true,
      notice: description.getAnnotation(org.junit.Ignore.class).value(),
      duration: 0,
    ]
  }

  @Override
  public void testFinished(Description description) throws Exception {
    // Stop capture the stdout/stderr
    System.out.flush()
    System.err.flush()
    System.setOut(old_stdout)
    System.setErr(old_stderr)
    println('')
    println("Test finished: ${description.toString()}")

    suite_results[description.toString()] += [
      name: description.toString(),
      stdout: stdout.toString(),
      stderr: '',
      duration: System.currentTimeMillis() - test_start_time,
    ]
  }

  @Override
  public void testFailure(Failure failure) throws Exception {
    testAssumptionFailure(failure)
  }

  @Override
  public void testAssumptionFailure(Failure failure) {
    // Stop capture the stdout/stderr
    System.out.flush()
    System.err.flush()
    System.setOut(old_stdout)
    System.setErr(old_stderr)
    println('')
    println("Error found: ${failure.getDescription()}: ${failure.getException()}")
    def e = failure.getException()
    def key = e instanceof AssertionError ? 'failures' : 'errors'
    suite_results[failure.getDescription().toString()][key] += e
    if( key == 'errors' )
      suite_error_count++

    // Printing stdout on fail
    println('-----8<----- LOG -----8<-----')
    println(stdout.toString())
    println('---->8---- END LOG ---->8----')
  }

  @Override
  public void testRunFinished(Result result) throws Exception {
    println('Suite finished')

    // Write XML data to the report file
    def writer = new StringWriter()
    def xml = new groovy.xml.MarkupBuilder(writer)

    xml.testsuites( disabled: result.getIgnoreCount(),
                    errors: suite_error_count, failures: result.getFailureCount(),
                    tests: result.getRunCount(), time: (result.getRunTime()/1000.0f).round(3) ) {
      testsuite( name: suite_name,
                 id: 0, disabled: result.getIgnoreCount(), skipped: result.getIgnoreCount(),
                 errors: suite_error_count, failures: result.getFailureCount(),
                 tests: result.getRunCount(), time: (result.getRunTime()/1000.0f).round(3), timestamp: suite_timestamp ) {
        /*properties {
          property( name: 'asd', value: 'dsd' )
        }*/
        suite_results.values().each { test ->
          testcase( name: test.name, classname: suite_name, time: (test.duration/1000.0f).round(3) ) {
            if( test.skipped ) {
              skipped()
            } else if( test.errors ) {
              test.errors.each {
                error( message: it.getMessage(), type: it.getClass().toString() )
              }
            } else if( test.failures ) {
              test.failures.each {
                failure( message: it.getMessage(), type: it.getClass().toString() )
              }
            }

            if( test.stdout?.trim() )
              'system-out'( test.stdout.trim() )
            if( test.stderr?.trim() )
              'system-err'( test.stderr.trim() )
          }
        }
      }
    }

    def file = new File(report_dir, "TEST-${suite_name}.xml")
    file.write('<?xml version="1.0" encoding="UTF-8"?>\n')
    file.append(writer.toString())
  }
}
