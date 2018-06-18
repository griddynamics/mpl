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

import com.griddynamics.devops.mpl.MPLManager

/**
 * Run the poststep list in reverse direction
 *
 * @author Sergei Parshev <sparshev@griddynamics.com>
 * @param name  List name
 * @see MPLManager#postStepsRun(String name)
 */
def call(String name) {
  MPLManager.instance.postStepsRun(name)
  def errors = MPLManager.instance.getPostStepsErrors(name)
  for( int e in errors )
    println "PostStep '${name}' error: ${e.module}: ${e.error}"
}
