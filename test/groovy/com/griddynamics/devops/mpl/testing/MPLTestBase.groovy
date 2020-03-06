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

import org.junit.AfterClass
import org.junit.Before
import com.lesfurets.jenkins.unit.BasePipelineTest

import com.griddynamics.devops.mpl.MPLConfig
import com.griddynamics.devops.mpl.MPLManager
import com.griddynamics.devops.mpl.Helper

import java.security.AccessController
import java.security.PrivilegedAction
import org.codehaus.groovy.runtime.InvokerHelper

/**
 * Reinitialized for each particular test
 */
abstract class MPLTestBase extends BasePipelineTest {
  MPLTestBase() {
    helper = new MPLTestHelper()
  }

  // Contains all the executing blocks {id => [module, parent]}
  Map mpl_blocks = [:]
  int mpl_blocks_id_counter = 0

  @Before
  void setupOverrides() {
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

    // TODO: Not working properly with the parallel run
    Helper.metaClass.static.startMPLBlock = { String path ->
      def id = mpl_blocks_id_counter++
      def level = getMPLModuleLevel()
      mpl_blocks[id] = [
        id: id,
        module: path,
        level: level,
        ended: false,
        parent: level > 1 ? findBlockParents(level-1).first().id : null,
      ]
      return id.toString()
    }
    Helper.metaClass.static.endMPLBlock = { String start_id ->
      mpl_blocks[start_id.toInteger()].ended = true
    }
    Helper.metaClass.static.getMPLBlocks = { ->
      def level = getMPLModuleLevel()
      return findBlockParents(level)
    }
  }

  @AfterClass
  static void cleanOverrides() {
    GroovySystem.metaClassRegistry.removeMetaClass(Helper.class)
  }

  // Using stacktrace to determine the nesting level of the module
  int getMPLModuleLevel() {
    return new Throwable().getStackTrace().findAll {
      it.getClassName() == 'MPLModule' && it.getLineNumber() > -1
    }.size()
  }

  // Get the list of the currently active modules of specified level and it's parents
  List findBlockParents(int level) {
    def curr_heads = mpl_blocks.values().findAll { it.ended == false && it.level == level }

    if( curr_heads.size() == 0 )
      return []
    else if( curr_heads.size() > 1 )
      throw Exception("Invalid number of current heads: ${curr_heads.size()} - unable to determine the current one")

    for( def l = 0; l < level; l++ )
      curr_heads += [mpl_blocks[curr_heads.last().parent]]
    return curr_heads
  }
}
