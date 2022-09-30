/**
 * MPL build using MPL
 *
 * Env vars:
 *   MPL_CLONE_URL - where to get the sources
 *   MPL_REFSPEC   - refspec mapping
 *   MPL_VERSION   - branch/commit/tag to verify
 */

import hudson.plugins.git.*
import org.jenkinsci.plugins.workflow.job.WorkflowJob

import org.junit.Test

class MPLPipelineTest extends JenkinsTest {
  @Test
  void testJenkinsfileBuild() {
    println("Setup job configuration...")

    def remote_configs = [new UserRemoteConfig(System.getenv('MPL_CLONE_URL'), null, System.getenv('MPL_REFSPEC'), '')]
    def branches = [new BranchSpec(System.getenv('MPL_VERSION') ?: 'master')]
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
