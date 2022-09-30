/**
 * Testing module
 * Allows to run unit and integration tests on the working jenkins
 * You can test how configuration was applied, run jobs/pipelines or UI tests
 *
 * Env vars:
 *   CASC_GROOVY_TESTS_PATH - basic tests module dir which contains `tests` dir with test classes
 *   JENKINS_HOME           - path to jenkins home directory
 *
 */

// Need to get junit because jenkins 2.253 doesn't have it
@Grab(group='junit', module='junit', version='4.13')

import hudson.init.InitMilestone
import jenkins.model.Jenkins

/**
 * Common test class
 * You can use it in your tests
 */
class JenkinsTest {
  def jenkins

  @org.junit.Before
  void setUp() {
    jenkins = Jenkins.getInstance()
  }
}

Thread.start('JenkinsBro tests') {
  def tests_dir = "${System.getenv('CASC_GROOVY_TESTS_PATH')}/tests"
  def status_file = new File("${System.getenv('JENKINS_HOME')}/tests_status")
  status_file.write('Started')
  def junit_report_dir = new File("${System.getenv('JENKINS_HOME')}/junit_report")
  junit_report_dir.deleteDir()
  junit_report_dir.mkdir()

  def failed_tests = 0

  println("JenkinsBro tests started, status file: `${status_file}`, reports dir: `${junit_report_dir}`")

  try {
    println('JenkinsBro tests: waiting for Jenkins initialization...')
    def retries = 5 * 60 // 5 minutes
    (1..retries).each {
      if( Jenkins.getInstance().getInitLevel() == InitMilestone.COMPLETED )
        return
      sleep(1000)
    }

    if( Jenkins.getInstance().getInitLevel() != InitMilestone.COMPLETED )
      return println("JenkinsBro tests, Jenkins initialization not completed in ${retries} seconds, exiting...")

    println('JenkinsBro tests: loading required classes')
    def cl = new GroovyClassLoader(this.class.getClassLoader())
    cl.setClassCacheEntry(JenkinsTest)

    println('JenkinsBro tests: preparing junit')
    def core = org.junit.runner.JUnitCore.newInstance()
    def listener = new org.junit.internal.TextListener(System.out)
    core.addListener(listener)

    cl.addClasspath("${System.getenv('CASC_GROOVY_TESTS_PATH')}/lib")
    Class formatterClazz = cl.loadClass('SimpleJUnitResultFormatterAsRunListener')
    core.addListener(formatterClazz.newInstance(junit_report_dir))

    new File(tests_dir).eachFileRecurse(groovy.io.FileType.FILES) {
      if( !it.name.endsWith('Test.groovy') )
        return

      println("Init test suite: ${it.getName()}...")
      Class clazz

      try {
        clazz = cl.parseClass(it)
      } catch( Exception ex ) {
        println("ERROR: Exception while loading test suite ${it}: ${ex}")
        return it.name
      }

      println("Running test suite: ${it.getName()}...")

      try {
        def result = core.run(clazz)
        failed_tests += result.getFailureCount()
      } catch( Exception ex ) {
        println("ERROR: Exception while running test suite ${it}: ${ex}")
        ex.printStackTrace()
        return it.name
      }
    }
  } catch( Exception ex ) {
    println("ERROR: Exception while executing tests: ${ex}")
    ex.printStackTrace()
  }

  status_file.write('Finished')

  println('JenkinsBro tests: All tests are finished')

  println('JenkinsBro tests: Shutdown Jenkins')
  System.exit(failed_tests)
}
