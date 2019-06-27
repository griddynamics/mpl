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

/**
 * Object to help with MPL pipelines configuration & poststeps
 *
 * @author Sergei Parshev <sparshev@griddynamics.com>
 */
@Singleton
class MPLManager implements Serializable {
  /** List of paths which is used to find modules in libraries */
  private List modulesLoadPaths = ['com/griddynamics/devops/mpl']

  /** Pipeline configuration */
  private Map config = [:]

  /** Poststep lists container */
  private Map postSteps = [:]

  /** Module poststep lists container */
  private Map modulePostSteps = [:]

  /** Poststeps errors store */
  private Map postStepsErrors = [:]

  /** Flag to enable enforcement of the modules on project side */
  private Boolean enforced = false

  /** List of modules available on project side while enforcement */
  private List enforcedModules = []

  /** List of currently executed modules */
  private List activeModules = []

  /**
   * Initialization for the MPL manager
   *
   * @param pipelineConfig  Map with common configuration and specific modules configs
   * @return  MPLManager singleton object
   */
  public init(pipelineConfig = null) {
    if( pipelineConfig in Map ) this.config = pipelineConfig
    this
  }

  /**
   * Get agent label from the specific config option
   *
   * @return  Agent label taken from the agent_label config property
   */
  public String getAgentLabel() {
    config.agent_label
  }

  /**
   * Get a module configuration
   * Module config is a pipeline config without modules section and with overrided values from the module itself.
   *
   * @param name  module name
   * @return  Overriden configuration for the specified module
   */
  public Map moduleConfig(String name) {
    config.modules ? Helper.mergeMaps(config.subMap(config.keySet()-'modules'), (config.modules[name] ?: [:])) : config
  }

  /**
   * Determine is module exists in the configuration or not
   *
   * @param name  module name
   * @return  Boolean about existing the module
   */
  public Boolean moduleEnabled(String name) {
    config.modules ? config.modules[name] != null : false
  }

  /**
   * Add post step to the array with specific name
   *
   * @param name  Poststeps list name
   *              Usual poststeps list names:
   *                * "always"  - used to run poststeps anyway (ex: decomission of the dynamic environment)
   *                * "success" - poststeps to run on pipeline success (ex: email with congratulations or ask for promotion)
   *                * "failure" - poststeps to run on pipeline failure (ex: pipeline failed message)
   * @param body  Definition of steps to include in the list
   */
  public void postStep(String name, Closure body) {
    // TODO: Parallel execution - could be dangerous
    if( ! postSteps[name] ) postSteps[name] = []
    postSteps[name] << [module: getActiveModules()?.last(), body: body]
  }

  /**
   * Add module post step to the list
   *
   * @param name  Module poststeps list name
   * @param body  Definition of steps to include in the list
   */
  public void modulePostStep(String name, Closure body) {
    // TODO: Parallel execution - could be dangerous
    if( ! modulePostSteps[name] ) modulePostSteps[name] = []
    modulePostSteps[name] << [module: getActiveModules()?.last(), body: body]
  }

  /**
   * Execute post steps filled by modules in reverse order
   *
   * @param name  Poststeps list name
   */
  public void postStepsRun(String name = 'always') {
    if( postSteps[name] ) {
      for( def i = postSteps[name].size()-1; i >= 0 ; i-- ) {
        try {
          postSteps[name][i]['body']()
        }
        catch( ex ) {
          postStepError(name, postSteps[name][i]['module'], ex)
        }
      }
    }
  }

  /**
   * Execute module post steps filled by module in reverse order
   *
   * @param name  Module poststeps list name
   */
  public void modulePostStepsRun(String name) {
    if( modulePostSteps[name] ) {
      for( def i = modulePostSteps[name].size()-1; i >= 0 ; i-- ) {
        try {
          modulePostSteps[name][i]['body']()
        }
        catch( ex ) {
          postStepError(name, modulePostSteps[name][i]['module'], ex)
        }
      }
    }
  }

  /**
   * Post steps could end with errors - and it will be stored to get it later
   *
   * @param name  Poststeps list name
   * @param module  Name of the module
   * @param exception  Exception object with error
   */
  public void postStepError(String name, String module, Exception exception) {
    if( ! postStepsErrors[name] ) postStepsErrors[name] = []
    postStepsErrors[name] << [module: module, error: exception]
  }
  
  /**
   * Get the list of errors become while poststeps execution
   *
   * @param name  Poststeps list name
   * @return  List of errors
   */
  public List getPostStepsErrors(String name) {
    postStepsErrors[name] ?: []
  }


  /**
   * Get the modules load paths in reverse order to make sure that defined last will be listed first
   *
   * @return  List of paths
   */
  public List getModulesLoadPaths() {
    modulesLoadPaths.reverse()
  }

  /**
   * Add path to the modules load paths list
   *
   * @param path  string with resource path to the parent folder of modules
   */
  public void addModulesLoadPath(String path) {
    modulesLoadPaths += path
  }

  /**
   * Enforce modules override on project side - could be set just once while execution
   *
   * @param modules  List of modules available to be overriden on the project level
   */
  public void enforce(List modules) {
    if( enforced == true ) return // Execute function only once while initialization
    enforced = true
    enforcedModules = modules
  }

  /**
   * Check module in the enforced list
   *
   * @param module  Module name
   * @return  Boolean module in the list, will always return true if not enforced
   */
  public Boolean checkEnforcedModule(String module) {
    ! enforced ?: enforcedModules.contains(module)
  }

  /**
   * Get list of currently executing modules
   *
   * @return  List of modules paths
   */
  public getActiveModules() {
    activeModules
  }

  /**
   * Add active module to the stack-list
   *
   * @param path  Path to the module (including library if it's the library)
   */
  public pushActiveModule(String path) {
    activeModules += path
  }

  /**
   * Removing the latest active module from the list
   */
  public popActiveModule() {
    activeModules.pop()
  }
}
