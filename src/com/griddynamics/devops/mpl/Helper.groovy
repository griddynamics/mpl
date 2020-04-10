//
// Copyright (c) 2018 Grid Dynamics International, Inc. All Rights Reserved
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

package com.griddynamics.devops.mpl

import java.nio.file.Paths

import com.cloudbees.groovy.cps.NonCPS

import org.jenkinsci.plugins.workflow.cps.CpsGroovyShellFactory
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution
import org.jenkinsci.plugins.workflow.cps.CpsThread
import org.jenkinsci.plugins.workflow.cps.nodes.StepStartNode
import org.jenkinsci.plugins.workflow.cps.nodes.StepEndNode
import org.jenkinsci.plugins.workflow.actions.LabelAction
import org.jenkinsci.plugins.workflow.actions.BodyInvocationAction
import org.jenkinsci.plugins.workflow.cps.actions.ArgumentsActionImpl
import org.jenkinsci.plugins.workflow.libs.LibrariesAction

import hudson.model.Run
import hudson.FilePath
import jenkins.model.Jenkins

import com.griddynamics.devops.mpl.MPLManager
import com.griddynamics.devops.mpl.MPLException

/**
 * Manages all helpers to interact with low-level groovy
 *
 * @author Sergei Parshev <sparshev@griddynamics.com>
 */
abstract class Helper {
  /**
   * Get a new shell with set some specific variables
   *
   * @param vars  Predefined variables to include in the shell
   *
   * @return  cps groovy shell object
   */
  @NonCPS
  static Object getShell(Map vars = [:]) {
    def ex = CpsThread.current().getExecution()
    def shell = new CpsGroovyShellFactory(ex).withParent(ex.getShell()).build()
    vars.each { key, val -> shell.setVariable(key, val) }
    shell
  }

  /**
   * Getting a list of modules for each loaded library with modules data
   * Idea from LibraryAdder.findResource() function
   *
   * @param path  Module resource path
   *
   * @return  list of maps with pairs "module path: module source code"
   *
   * @see org.jenkinsci.plugins.workflow.libs.LibraryAdder#findResources(CpsFlowExecution execution, String name)
   */
  static List getModulesList(String path) {
    def executable = CpsThread.current()?.getExecution()?.getOwner()?.getExecutable()
    if( ! (executable instanceof Run) )
      throw new MPLException('Current executable is not a jenkins Run')

    def action = executable.getAction(LibrariesAction.class)
    if( action == null )
      throw new MPLException('Unable to find LibrariesAction in the current Run')

    def modules = []
    def libs = new FilePath(executable.getRootDir()).child('libs')
    action.getLibraries().each { lib ->
      MPLManager.instance.getModulesLoadPaths().each { modulesPath ->
        def libPath = Paths.get(lib.name, 'resources', modulesPath, path).toString()
        def f = libs.child(libPath)
        if( f.exists() ) modules += [[libPath, f.readToString()]]
      }
    }
    return modules
  }

  /**
   * Helps with merging two maps recursively
   *
   * @param base     map for modification
   * @param overlay  map to override values in the _base_ map
   *
   * @return  modified base map
   */
  static Map mergeMaps(Map base, Map overlay) {
    if( ! (base in Map) ) base = [:]
    if( ! (overlay in Map) ) return overlay
    overlay.each { key, val ->
      base[key] = base[key] in Map ? mergeMaps(base[key], val) : val
    }
    base
  }

  /**
   * Deep copy of the Map or List
   *
   * @param value      value to deep copy
   *
   * @return  value type without any relation to the original value
   */
  @NonCPS
  static cloneValue(value) {
    def out

    if( value in Map )
      out = value.collectEntries { k, v -> [k, cloneValue(v)] }
    else if( value in List )
      out = value.collect { cloneValue(it) }
    else
      out = value

    return out
  }

  /**
   * Silently check the workspace file existence
   * Quiet version of fileExists step
   *
   * @param path  relative path to the file in workspace
   *
   * @return  Boolean if file exists or not or null if there is no workspace available
   */
  @NonCPS
  static Boolean pathExists(String path) {
    return CpsThread.current() ? CpsThread.current().getContextVariable(
      FilePath.class, CpsThread.current().&getExecution, CpsThread.current().head.&get
    )?.child(path)?.exists() : null
  }

  /**
   * Silently read the workspace file
   * Quiet version of readFile step
   *
   * @param path  relative path to the file in workspace
   *
   * @return  String with the file content
   */
  @NonCPS
  static String pathRead(String path) {
    return CpsThread.current() ? CpsThread.current().getContextVariable(
      FilePath.class, CpsThread.current().&getExecution, CpsThread.current().head.&get
    )?.child(path)?.read()?.getText() : null
  }

  /**
   * Returns just last two components of the path without an extension
   *
   * @param path  resource path of the module
   *
   * @return  String simple MPL module name
   */
  @NonCPS
  static String pathToSimpleName(String path) {
    return path.tokenize('./')[-3,-2].join('/')
  }

  /**
   * Starts block for the MPL module
   *
   * @param path  resource path of the module to track it
   *
   * @return  String block id (used for tests)
   */
  @NonCPS
  static String startMPLBlock(String path) {
    def head = CpsThread.current().head
    CpsFlowExecution.maybeAutoPersistNode(head.get())
    def node = new StepStartNode(
      head.getExecution(),
      Jenkins.get().getDescriptor('org.jenkinsci.plugins.workflow.steps.EchoStep'),
      head.get()
    )
    node.addAction(new BodyInvocationAction())
    node.addAction(new LabelAction("MPL: ${pathToSimpleName(path)}"))
    node.addAction(new ArgumentsActionImpl([MPLModule:path]))
    head.getExecution().cacheNode(node)
    head.setNewHead(node)

    return node.getId()
  }

  /**
   * Ends the current MPL module block
   *
   * @param start_id  start node ID to find in the current execution
   */
  @NonCPS
  static void endMPLBlock(String start_id) {
    def start_node = CpsThread.current().getExecution().getNode(start_id)
    def head = CpsThread.current().head
    def node = new StepEndNode(head.getExecution(), start_node, head.get())
    node.addAction(new BodyInvocationAction())
    head.getExecution().cacheNode(node)
    head.setNewHead(node)
    CpsFlowExecution.maybeAutoPersistNode(node)
  }

  /**
   * Returns stack of the running modules
   * The first one - is the current one
   *
   * @return  List with maps, contains module name and id
   */
  @NonCPS
  static List getMPLBlocks() {
    def nodes = [CpsThread.current().head.get()]
    nodes += nodes.first().getEnclosingBlocks()
    return nodes.collect {[
      id: it.getId(),
      module: it.getPersistentAction(ArgumentsActionImpl.class)?.getArgumentValue('MPLModule'),
    ]}.findAll{ it.module }
  }

  /**
   * Helps to run source code in the new shell with predefined vars
   * Also it's overriden by tests to handle the module execution
   *
   * @param src   source code of the module
   * @param path  resource path of the module to track it
   * @param vars  predefined variables for the run
   */
  static void runModule(String src, String path, Map vars = [:]) {
    getShell(vars).evaluate(src, path)
  }

  /**
   * Cut & simplify a stacktrace
   *
   * @param exception  container of the stacktrace
   *
   * @return  List with stack trace elements
   */
  static StackTraceElement[] getModuleStack(Throwable exception) {
    List stack = exception.getStackTrace()

    // For jenkins to remove starting trace items
    if( stack.size() > 0 && stack.last()?.getFileName() == 'Thread.java' ) {
      // Finding the first MPLModule call and cutting the trace
      for( def i = stack.size(); i--; /* inverse for */ ) {
        if( stack[i-1].getFileName()?.endsWith('MPLModule.groovy') )
          break
        else
          stack.remove(i)
      }
    }

    // If the exception are not from the mpl pipeline - need to show at least something
    if( stack.isEmpty() )
      stack = exception.getStackTrace()

    stack as StackTraceElement[]
  }

  /**
   * Looking the latest cause of the module file name and return it's line number
   *
   * @param module_path  MPL module path or module file name
   * @param exception  container of the stacktrace
   *
   * @return  Module line number or null if not found
   */
  static Integer getModuleExceptionLine(String module_path, Throwable exception) {
    List stack = exception.getStackTrace()
    // First try to find the complete module path
    for( def s in stack ) {
      if( s?.getFileName() == module_path )
        return s.getLineNumber()
    }

    // Second try to find at least a module file name
    def module_file = module_path.tokenize('/').last()
    for( def s in stack ) {
      if( s?.getFileName()?.endsWith(module_file) )
        return s.getLineNumber()
    }
    return null
  }

  /**
   * Special function to return exception if someone tries to use MPLConfig in a wrong way
   * Basically used just to be overridden on the unit tests side.
   *
   * @param config  current MPLConfig configuration
   *
   * @return  Set of entries - but only when overridden by unit tests
   */
  static Set configEntrySet(Map config) {
    throw new MPLException('Forbidden to iterate over MPLConfig, please use some specific key with a good self-describable name')
  }
}
