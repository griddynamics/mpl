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

package com.griddynamics.devops.mpl.testing

import com.lesfurets.jenkins.unit.BasePipelineTest

import com.griddynamics.devops.mpl.MPLConfig
import com.griddynamics.devops.mpl.MPLManager
import com.griddynamics.devops.mpl.Helper

import java.security.AccessController
import java.security.PrivilegedAction
import org.codehaus.groovy.runtime.InvokerHelper

abstract class MPLTestBase extends BasePipelineTest {
  MPLTestBase() {
    helper = new MPLTestHelper()
  }

  void setUp() throws Exception {
    super.setUp()

    // Overriding Helper to find the right resources in the loaded libs
    Helper.metaClass.static.getModulesList = { String path ->
      def modules = []
      def resourcesFolder = helper.getLibraryClassLoader().getResource('.').getFile()
      MPLManager.instance.getModulesLoadPaths().each { modulesPath ->
        def libPath = modulesPath + '/' + path
        helper.getLibraryClassLoader().getResources(libPath).each { res ->
          def libname = res.getFile().substring(resourcesFolder.length())
          libname = libname.substring(0, Math.max(libname.indexOf('@'), 0))
          modules += [[libname + '/' + libPath, res.text]]
        }
      }
      return modules
    }

    // Replacing runModule function to mock it
    Helper.metaClass.static.runModule = { String source, String path, Map vars = [:] ->
      def binding = new Binding()
      this.binding.variables.each { k, v -> binding.setVariable(k, v) }
      vars.each { k, v -> binding.setVariable(k, v) }
      def loader = AccessController.doPrivileged(new PrivilegedAction<GroovyClassLoader>() {
        public GroovyClassLoader run() {
          return new GroovyClassLoader(helper.getLibraryClassLoader(), helper.getLibraryConfig())
        }
      })
      def script = InvokerHelper.createScript(loader.parseClass(new GroovyCodeSource(source, path, '/mpl/modules'), false), binding)
      script.metaClass.invokeMethod = helper.getMethodInterceptor()
      script.metaClass.static.invokeMethod = helper.getMethodInterceptor()
      script.metaClass.methodMissing = helper.getMethodMissingInterceptor()
      script.run()
    }
    
    // Show the dump of the configuration during unit tests execution
    Helper.metaClass.static.configEntrySet = { Map config -> config.entrySet() }
  }
}
