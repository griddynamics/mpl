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

import org.junit.Before
import org.junit.Test

import static com.lesfurets.jenkins.unit.global.lib.LibraryConfiguration.library
import static com.lesfurets.jenkins.unit.global.lib.LocalSource.localSource

import static org.assertj.core.api.Assertions.assertThat

import com.griddynamics.devops.mpl.Helper
import com.griddynamics.devops.mpl.testing.MPLTestBaseJenkinsRule

/**
 * Same tests using JenkinsRule
 *
 * @author Sergei Parshev <sparshev@griddynamics.com>
 */
public class BuildJenkinsRuleTest extends MPLTestBaseJenkinsRule {
  @Override
  @Before
  void setUp() throws Exception {
    //setScriptRoots([ 'vars' ] as String[])
    //setScriptExtension('groovy')

    super.setUp()

    String sharedLibs = this.class.getResource('.').getFile()
    helper.registerSharedLibrary(library()
        .name('mpl')
        .allowOverride(false)
        .retriever(localSource(sharedLibs))
        .targetPath(sharedLibs)
        .defaultVersion('snapshot')
        .implicit(true)
        .build()
    )

    binding.setVariable('env', [:])

    // Shared lib requirements
    helper.registerAllowedMethod('MPLModule', [], null)
    helper.registerAllowedMethod('MPLModule', [String.class], null)
    helper.registerAllowedMethod('MPLModule', [String.class, Object.class], null)
    helper.registerAllowedMethod('call', [String.class, Object.class], null)

    // Test requirements
    helper.registerAllowedMethod('fileExists', [String.class], { return false })
    helper.registerAllowedMethod('tool', [String.class], { name -> "${name}_HOME" })
    helper.registerAllowedMethod('withEnv', [List.class, Closure.class], null)
    helper.registerAllowedMethod('sh', [String.class], {})
  }


  @Test
  void default_run() {
    runMPLModule('Build')

    printCallStack()

    assertThat(helper.callStack)
      .filteredOn { c -> c.methodName == 'tool' }
      .filteredOn { c -> c.argsToString().contains('Maven 3') }
      .as('Maven 3 tool used')
      .isNotEmpty()

    assertThat(helper.callStack)
      .filteredOn { c -> c.methodName == 'sh' }
      .filteredOn { c -> c.argsToString().startsWith('mvn') }
      .filteredOn { c -> c.argsToString().contains('clean install') }
      .as('Shell execution should contain mvn command and default clean install')
      .isNotEmpty()

    assertThat(helper.callStack)
      .filteredOn { c -> c.methodName == 'sh' }
      .filteredOn { c -> c.argsToString().startsWith('mvn') }
      .filteredOn { c -> ! c.argsToString().contains('-s ') }
      .as('Default mvn run without settings provided')
      .isNotEmpty()

    assertJobStatusSuccess()
  }

  @Test
  void change_tool() {
    runMPLModule('Build', [
      maven: [
        tool_version: 'Maven 2',
      ],
    ])

    printCallStack()

    assertThat(helper.callStack)
      .filteredOn { c -> c.methodName == 'tool' }
      .filteredOn { c -> c.argsToString().contains('Maven 2') }
      .as('Changing maven tool name')
      .isNotEmpty()

    assertJobStatusSuccess()
  }

  @Test
  void change_settings() {
    runMPLModule('Build', [
      maven: [
        settings_path: '/test-settings.xml',
      ],
    ])

    printCallStack()

    assertThat(helper.callStack)
      .filteredOn { c -> c.methodName == 'sh' }
      .filteredOn { c -> c.argsToString().contains("-s '/test-settings.xml'") }
      .as('Providing settings file should set the maven operation')
      .isNotEmpty()

    assertJobStatusSuccess()
  }
}
