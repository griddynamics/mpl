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

/**
 * Finding module implementation and executing it with specified configuration
 *
 * Logic:
 *   Module finding: workspace --> nested lib 2 --> nested lib 1 --> MPL library
 *   Loop protection: There is no way to run currently active module again
 *
 * @author Sergei Parshev <sparshev@griddynamics.com>
 * @param name  used to determine the module name, by default it's current stage name (ex. "Maven Build")
 * @param cfg   module configuration to override. Will update the common module configuration
 */
def call(String name = env.STAGE_NAME, Map cfg = null) {
  if( cfg == null )
    cfg = MPLManager.instance.moduleConfig(name)
  
  // Trace of the running modules to find loops
  // Also to make ability to use lib module from overridden one
  def active_modules = Helper.activeModules()

  // Determining the module source file and location
  def base = (cfg.name ?: name).tokenize()
  def module_path = "modules/${base.last()}/${base.join()}.groovy"
  def project_path = "jenkins/${module_path}".toString()

  // Reading module definition from workspace or from the library resources
  def module_src = null
  if( fileExists(project_path) && (! module_path.contains(project_path)) ) {
    module_path = 'project:' + project_path
    module_src = readFile(project_path)
  } else {
    // Searching for the not executed module from the loaded libraries
    // TODO: add ability to add other library paths to search
    def library_path = "com/griddynamics/devops/mpl/${module_path}".toString()
    module_src = Helper.getModulesList(library_path).find { it ->
      module_path = "library:${it.first()}:${library_path}".toString()
      ! active_modules.contains(module_path)
    }?.last()
  }

  if( ! module_src )
    throw new MPLModuleException("Unable to find the not active module to execute: ${(active_modules).join(' --> ')} -X> ${module_path}")

  try {
    Helper.runModule(module_src, module_path, [CFG: Helper.flatten(cfg)])
  }
  catch( ex ) {
    def newex = new MPLModuleException("Found error during execution of the module '${module_path}':\n${ex}")
    newex.setStackTrace(Helper.getModuleStack(ex))
    throw newex
  }
}
