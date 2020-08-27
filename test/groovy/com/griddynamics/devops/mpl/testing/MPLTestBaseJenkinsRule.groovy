//
// Copyright (c) 2020 Grid Dynamics International, Inc. All Rights Reserved
// https://www.griddynamics.com
//
// Classification level: Public
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// $Id: $
// @Project:     MPL
// @Description: Shared Jenkins Modular Pipeline Library
//

package com.griddynamics.devops.mpl.testing

import org.junit.rules.TemporaryFolder
import org.jvnet.hudson.test.JenkinsRule
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.Rule
import org.junit.ClassRule
import hudson.model.Queue
import hudson.Functions
import java.util.function.Consumer
import java.util.function.Function

import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.flow.FlowExecution

import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution
import org.jenkinsci.plugins.workflow.graph.FlowGraphWalker
import org.jenkinsci.plugins.workflow.graph.FlowNode
import org.jenkinsci.plugins.workflow.actions.ErrorAction

import com.lesfurets.jenkins.unit.MethodCall
import com.lesfurets.jenkins.unit.MethodSignature
import com.griddynamics.devops.mpl.testing.MPLInterceptor

import org.jenkinsci.plugins.workflow.libs.GlobalLibraries
import org.jenkinsci.plugins.workflow.libs.LibraryConfiguration
import com.griddynamics.devops.mpl.testing.MPLLocalLibraryRetriever

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun

import java.util.logging.Logger
import java.util.logging.Level
import java.util.logging.LogManager

import com.griddynamics.devops.mpl.Helper

/**
 * Base class for unit tests using JenkinsRule
 *
 * @author Sergei Parshev <sparshev@griddynamics.com>
 */
abstract class MPLTestBaseJenkinsRule {
  @Rule
  public TemporaryFolder tmp = new TemporaryFolder()

  @ClassRule
  public static JenkinsRule jenkins = new JenkinsRule() {
    @Override
    public void before() throws Throwable {
      // TODO: Not working as expected - there is ton of logs from jenkins...
      println("DEBUG: jenkins: init")
      // Disable ExtensionFinder SSH and JNLP4 exceptions
      def logger = Logger.getLogger('hudson')
      logger.setLevel(Level.OFF)
      LogManager.getLogManager().addLogger(logger)
      super.before()
    }
  }

  /** Currently executing flow. */
  protected CpsFlowExecution exec

  /** Directory to put {@link #exec} in. */
  protected File rootDir

  List<LibraryConfiguration> libraries = []

  String[] scriptRoots = ["src/main/jenkins", "./."]

  String scriptExtension = "jenkins"

  Binding binding = new Binding()

  def workflow_job = null
  def job_result = null

  MPLTestBaseJenkinsRule() {
    helper = this
  }

  public void setUp() throws Exception {
    rootDir = tmp.newFolder()
    workflow_job = jenkins.createProject(WorkflowJob)
  }

  @After
  public void clearTest() {
    MPLInterceptor.instance.clear()
    libraries.clear()
  }

  @Before
  public void setupOverrides() {
    // Show the dump of the configuration during unit tests execution
    Helper.metaClass.static.configEntrySet = { Map config -> config.entrySet() }
    // TODO: fix this
    org.jenkinsci.plugins.workflow.cps.CpsVmExecutorService.FAIL_ON_MISMATCH = false
  }

  @AfterClass
  public static void cleanOverrides() {
    org.jenkinsci.plugins.workflow.cps.CpsVmExecutorService.FAIL_ON_MISMATCH = true
    GroovySystem.metaClassRegistry.removeMetaClass(Helper.class)
  }

  protected String dumpError() {
    StringBuilder msg = new StringBuilder()
    FlowGraphWalker walker = new FlowGraphWalker(exec)
    for (FlowNode n : walker) {
      ErrorAction e = n.getAction(ErrorAction.class)
      if (e != null) {
        msg.append(Functions.printThrowable(e.getError()))
      }
    }
    return msg.toString()
  }

  public static class FlowExecutionOwnerImpl extends FlowExecutionOwner {
    @Override public FlowExecution get() { return helper.exec }
    @Override public File getRootDir() { return helper.rootDir }
    @Override public Queue.Executable getExecutable() { return null }
    @Override public String getUrl() { return "TODO" }
    @Override public boolean equals(Object o) { return this==o }
    @Override public int hashCode() { return 0 }
  }

  protected static MPLTestBaseJenkinsRule helper

  void registerAllowedMethod(String name, List<Class> args = [], Closure closure) {
    MPLInterceptor.instance.registerAllowedMethod(name, args, closure)
  }

  void registerAllowedMethod(MethodSignature methodSignature, Closure closure) {
    MPLInterceptor.instance.registerAllowedMethod(methodSignature, closure)
  }

  void registerAllowedMethod(MethodSignature methodSignature, Function callback) {
    MPLInterceptor.instance.registerAllowedMethod(methodSignature,
        callback != null ? { params -> return callback.apply(params) } : null)
  }

  void registerAllowedMethod(MethodSignature methodSignature, Consumer callback) {
    MPLInterceptor.instance.registerAllowedMethod(methodSignature,
        callback != null ? { params -> return callback.accept(params) } : null)
  }

  void printCallStack() {
    MPLInterceptor.instance.printCallStack()
  }

  List<MethodCall> getCallStack() {
    return MPLInterceptor.instance.call_stack
  }

  void runMPLModule(Object... args) {
    // Here we starting the job and replacing arguments during execution
    workflow_job.setDefinition(new CpsFlowDefinition('''MPLModule('TO_REPLACE_321314')''', true))
    MPLInterceptor.instance.addArgumentsReplace('Sandbox  WorkflowScript.MPLModule(TO_REPLACE_321314)', args)
    job_result = jenkins.buildAndAssertSuccess(workflow_job)
  }

  void assertJobStatusFailure() {
    assertJobStatus('FAILURE')
  }

  void assertJobStatusUnstable() {
    assertJobStatus('UNSTABLE')
  }

  void assertJobStatusSuccess() {
    assertJobStatus('SUCCESS')
  }

  private assertJobStatus(String status) {
    //assertThat(binding.getVariable('currentBuild').result).isEqualTo(status)
  }

  void registerSharedLibrary(Object libraryDescription) {
    //println("DEBUG: ${libraryDescription}")
    Objects.requireNonNull(libraryDescription)
    Objects.requireNonNull(libraryDescription.name)
    LibraryConfiguration lib = new LibraryConfiguration(
      libraryDescription.name,
      new MPLLocalLibraryRetriever(libraryDescription.retriever.sourceURL)
    )
    lib.setDefaultVersion(libraryDescription.defaultVersion)
    lib.setImplicit(libraryDescription.implicit)
    lib.setAllowVersionOverride(libraryDescription.allowOverride)
    //libraries.add(lib)
    GlobalLibraries.get().setLibraries(Collections.singletonList(lib))
  }
}
