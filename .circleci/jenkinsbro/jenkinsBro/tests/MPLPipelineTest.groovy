/**
 * MPL build using MPL
 *
 * Parameters:
 *   mpl_build_test.clone_url      - where to get the sources
 *   mpl_build_test.ref_spec       - refspec mapping
 *   mpl_build_test.branch_spec    - branch/commit/tag to build
 *   mpl_build_test.credentials_id - if the creds are required
 */

import hudson.plugins.git.*
import org.jenkinsci.plugins.workflow.job.WorkflowJob

import org.junit.Test

@Module('mpl_build_test')
class MPLPipelineTest extends JenkinsTest {
  @Test
  void testJenkinsfileBuild() {
    println("Setup job configuration: ${MODULE}")

    def remote_configs = [new UserRemoteConfig(MODULE.clone_url, null, MODULE.ref_spec, MODULE.credentials_id ?: '')]
    def branches = [new BranchSpec(MODULE.branch_spec ?: 'master')]
    def scm = new GitSCM(remote_configs, branches, false, [], null, null, [])

    def wfjob = jenkins.createProject(WorkflowJob.class, 'mpl-build')
    def flow_def = org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition.newInstance(scm, 'Jenkinsfile')
    wfjob.setDefinition(flow_def)

    println('Running the job and wait for complete')
    def task_future = wfjob.scheduleBuild2(0)
    def build = task_future.waitForStart()
    task_future.get() // Waiting

    build.getLogText().writeLogTo(0, System.out)

    assert build.getResult().toString() == 'SUCCESS'
  }
}
