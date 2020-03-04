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

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import org.jenkinsci.plugins.workflow.libs.LibraryRetriever

/**
 * Simple global library local retreiver
 *
 * @author Sergei Parshev <sparshev@griddynamics.com>
 */
public class MPLLocalLibraryRetriever extends LibraryRetriever {
  private final File local_directory;

  public MPLLocalLibraryRetriever() {
    this(System.getProperty('user.dir'))
  }

  public MPLLocalLibraryRetriever(String path) {
    local_directory = new File(path)
  }

  @Override
  public void retrieve(String name, String version, boolean changelog, FilePath target, Run<?, ?> run, TaskListener listener) {
    doRetrieve(target, listener, "${name}@${version}")
  }

  @Override
  public void retrieve(String name, String version, FilePath target, Run<?, ?> run, TaskListener listener) {
    doRetrieve(target, listener, "${name}@${version}")
  }

  private void doRetrieve(FilePath target, TaskListener listener, String libversion) {
    final FilePath localFilePath = new FilePath(local_directory.toPath().resolve(libversion).toFile())
    localFilePath.copyRecursiveTo("src/**/*.groovy,vars/*.groovy,vars/*.txt,resources/", null, target)
  }
}
