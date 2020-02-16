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

import com.griddynamics.devops.mpl.Helper
import com.griddynamics.devops.mpl.MPLManager
import com.griddynamics.devops.mpl.MPLModuleException
import com.griddynamics.devops.mpl.MPLConfig

import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException

/**
 * Finding module implementation and executing it with specified configuration
 *
 * Logic:
 *   Module finding: workspace --> nested lib 2 --> nested lib 1 --> MPL library
 *   Loop protection: There is no way to run currently active module again
 *
 * @author Sergei Parshev <sparshev@griddynamics.com>
 *
 * @param name  used to determine the module name, by default it's current stage name (ex. "Maven Build")
 * @param cfg   module configuration to override. Will update the common module configuration
 */
def call(String name = env.STAGE_NAME, cfg = null) {
  if( cfg == null )
    cfg = MPLManager.instance.moduleConfig(name)
  else if( cfg instanceof MPLConfig )
    cfg = cfg.clone()
  else
    cfg = MPLConfig.create(cfg)
  
  // Trace of the running modules to find loops
  // Also to make ability to use lib module from overridden one
  // TODO: replace getActiveModules() to Helper.getMPLBlocks()
  def active_modules = MPLManager.instance.getActiveModules()

  // Determining the module source file and location
  def base = (cfg.name ?: name).tokenize()
  def module_path = "modules/${base.last()}/${base.join()}.groovy"
  def project_path = ".jenkins/${module_path}".toString()

  // Reading module definition from workspace or from the library resources
  def module_src = null
  if( MPLManager.instance.checkEnforcedModule(name)
      && env.NODE_NAME != null
      && Helper.pathExists(project_path)
      && (! active_modules.contains(project_path)) ) {
    module_path = project_path
    module_src = Helper.pathRead(project_path)
  } else {
    // Searching for the not executed module from the loaded libraries
    module_src = Helper.getModulesList(module_path).find { it ->
      module_path = "library:${it.first()}".toString()
      ! active_modules.contains(module_path)
    }?.last()
  }

  if( ! module_src )
    throw new MPLModuleException("Unable to find not active module to execute: ${(active_modules).join(' --> ')} -X> ${module_path}")

  // Basically block_id is needed only for tests
  String block_id = MPLManager.instance.pushActiveModule(module_path)
  try {
    Helper.runModule(module_src, module_path, [CFG: cfg])
  }
  catch( FlowInterruptedException ex ) {
    // The exception is used by Jenkins to abort a running build and consequently
    // does not constitute an execution error of the running MPL module.
    throw ex
  }
  catch( ex ) {
    def newex = new MPLModuleException("Found error during execution of the module '${module_path}#${Helper.getModuleExceptionLine(module_path, ex)}':\n${ex}")
    newex.setStackTrace(Helper.getModuleStack(ex))
    throw newex
  }
  finally {
    MPLManager.instance.modulePostStepsRun()
    def errors = MPLManager.instance.getPostStepsErrors()
    if( errors ) {
      for( def e in errors )
        println "Module '${name}' got error during execution of poststep from module '${e.module}': ${e.error}"
      def newex = new MPLModuleException("Found error during execution poststeps for the module '${module_path}'")
      newex.setStackTrace(Helper.getModuleStack(newex))
      throw newex
    }
    MPLManager.instance.popActiveModule(block_id)
  }
}
