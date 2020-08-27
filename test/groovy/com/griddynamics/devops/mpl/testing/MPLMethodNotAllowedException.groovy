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

/**
 * Exception to handle module execution errors
 *
 * @author Sergei Parshev <sparshev@griddynamics.com>
 */
class MPLMethodNotAllowedException extends MissingMethodException {
  public MPLMethodNotAllowedException(String method, Class type, Object[] arguments) {
    super(method, type, arguments, false)
  }

  public MPLMethodNotAllowedException(String method, Class type, Object[] arguments, boolean isStatic) {
    super(method, type, arguments, isStatic)
  }

  public String getMessage() {
    return "Method not registred in the list of allowed methods: ${super.getMessage()}"
  }
}
