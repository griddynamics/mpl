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
import org.jenkinsci.plugins.workflow.cps.CpsThread
import org.jenkinsci.plugins.workflow.libs.LibrariesAction

import hudson.model.Run
import hudson.FilePath

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
   * Converts map to a simply flatten map
   *
   * @param data       map to flatten
   * @param separator  keys separator
   * @return  map with flatten keys
   */
  static Map flatten(Map data, String separator = '.') {
    data.collectEntries { k, v ->
      v instanceof Map ? flatten(v, separator).collectEntries { q, r -> [(k + separator + q): r] } : [(k):v]
    }
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
    if( stack.last()?.getFileName() == 'Thread.java' ) {
      // Finding the first MPLModule call and cutting the trace
      for( def i = stack.size(); i--; i > 0 ) {
        if( stack[i-1].getFileName()?.contains('MPLModule.groovy') )
          break
        else
          stack.remove(i)
      }
    }

    // Removing not interesting sources from the output to simplify debug
    for( def i = stack.size(); i--; i > 0 ) {
      if( !stack[i]?.getFileName()?.endsWith('.groovy') ||
          stack[i]?.getFileName() in ['MPLModule.groovy', 'Helper.groovy', 'PipelineTestHelper.groovy', 'MPLTestBase.groovy'] )
        stack.remove(i)
    }

    stack as StackTraceElement[]
  }

  /**
   * Looking the latest cause of the module file name and return it's line number
   *
   * @param module_path  MPL module path or module file name
   * @param exception  container of the stacktrace
   *
   * @return  List with stack trace elements
   */
  static Integer getModuleExceptionLine(String module_path, Throwable exception) {
    List stack = exception.getStackTrace()
    def module_file = module_path.tokenize('/').last()
    for( def s in stack ) {
      if( s?.getFileName() == module_file )
        return s.getLineNumber()
    }
    return null
  }
}
