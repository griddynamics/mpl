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

import org.junit.Before
import org.junit.Test

import java.nio.file.Paths

import static com.lesfurets.jenkins.unit.global.lib.LibraryConfiguration.library
import static com.lesfurets.jenkins.unit.global.lib.LocalSource.localSource

import static org.assertj.core.api.Assertions.assertThat

import com.griddynamics.devops.mpl.Helper
import com.griddynamics.devops.mpl.testing.MPLTestBase

class BuildTest extends MPLTestBase {
  def script = null

  @Override
  @Before
  void setUp() throws Exception {
    String buildFolderBase = this.class.getResource('.').getPath().split('classes')[0]
    String sharedLibsPath = Paths.get(buildFolderBase, 'libs')

    helper.registerSharedLibrary(library()
        .name('mpl')
        .allowOverride(false)
        .retriever(localSource(sharedLibsPath))
        .targetPath(sharedLibsPath)
        .defaultVersion('snapshot')
        .implicit(true)
        .build()
    )

    setScriptRoots([ 'vars' ] as String[])
    setScriptExtension('groovy')

    super.setUp()

    binding.setVariable('env', [:])

    helper.registerAllowedMethod('fileExists', [String.class], null)
    helper.registerAllowedMethod('tool', [String.class], { name -> "${name}_HOME" })
    helper.registerAllowedMethod('withEnv', [List.class, Closure.class], null)

    script = loadScript('MPLModule.groovy')
  }

  @Test
  void default_run() throws Exception {
    script.call('Build')

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
  void change_tool() throws Exception {
    script.call('Build', [
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
  void change_settings() throws Exception {
    script.call('Build', [
      maven: [
        settings_path: '/test-settings.xml',
      ],
    ])

    printCallStack()

    assertThat(helper.callStack)
      .filteredOn { c -> c.methodName == 'sh' }
      .filteredOn { c -> c.argsToString().contains("-s '/test-settings.xml'") }
      .as('Providing setings file should set the maven operation')
      .isNotEmpty()

    assertJobStatusSuccess()
  }
}
