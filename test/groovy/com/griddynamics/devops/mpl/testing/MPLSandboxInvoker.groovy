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

import com.cloudbees.groovy.cps.sandbox.SandboxInvoker
import com.griddynamics.devops.mpl.testing.MPLInterceptor

/**
 * Sandbox invoker override for intercepting the methodCall execution
 *
 * @author Sergei Parshev <sparshev@griddynamics.com>
 */
public class MPLSandboxInvoker extends SandboxInvoker {
  @Override
  public Object methodCall(Object receiver, String method, Object[] args) throws Throwable {
    return MPLInterceptor.instance.processMethodCall(super.&doMethodCall, 'Sandbox', receiver, method, args)
  }

  public Object doMethodCall(Object receiver, String method, Object[] args) throws Throwable {
    return super.methodCall(receiver, method, args)
  }
}
