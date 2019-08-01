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
import com.griddynamics.devops.mpl.MPLException

/**
 * MPL pipeline helper to provide the default modules configuration
 *
 * @author Sergei Parshev <sparshev@griddynamics.com>
 *
 * @param body      Configuration for the pipeline
 * @param defaults  Default configuration from the pipeline definition
 * @param overrides Mandatory settings for the pipeline that will override config settings
 *
 * @return MPLManager singleton object
 */
def call(body, Map defaults = [:], Map overrides = [:]) {
  def config = defaults

  // Merging configs
  if( body in Closure ) {
    // This logic allow us to use configuration closures instead of maps
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config

    // Make sure the global variables will be available in the config closure
    config.env = env
    config.params = params
    config.currentBuild = currentBuild

    // Here we executing the closure to update the pipeline defaults with the closure values
    body()

    // Removing the global variables from the config
    config.remove('env')
    config.remove('params')
    config.remove('currentBuild')
  } else if( body in Map ) {
    Helper.mergeMaps(config, body)
  } else
    throw new MPLException("Unsupported MPL pipeline configuration type provided: ${body}")

  Helper.mergeMaps(config, overrides)

  // Init the MPL Pipeline
  MPLManager.instance.init(config)
}
