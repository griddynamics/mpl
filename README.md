Modular Pipeline Library
========================

Shared jenkins library with modular structure allow to write a simple pipeline modules, test it properly and use in any kind of pipelines.

## Goals

* Provide core to support flexible modular structure
* Enable support for declarative & scripted pipelines
* Ensure that nested libraries could reuse the MPL features
* Allow module overrides from the project or nested libraries
* Prepare set of simple & minimal basic pipelines
* Make it easy to create tests for modules & pipelines

## Documentation

This readme contains mostly technical information, if you need some overview - please check the next resources:

* [Grid Dynamics Blog](https://blog.griddynamics.com/developing-a-modular-pipeline-library-to-improve-devops-collaboration/)
* Overview & demo videos:
  * [MPL Intro](https://youtu.be/NpnQS3fFlyI)
  * [MPL Overview](https://youtu.be/wwi7oREL02k)
  * [Demo of the MPL Build](https://youtu.be/QZsQThhPk-Y)
  * [Demo of the Nested Library](https://youtu.be/UsThHFze76Y)
  * [Demo of the Petclinic Pipeline](https://youtu.be/GLtvxY1S3Aw)

You also can check [MPL Wiki](https://github.com/griddynamics/mpl/wiki) to find additional info.

## Dependencies

* Jenkins >= 2.74 LTS
* workflow-cps >= 2.44
* workflow-job >= 2.15
* workflow-cps-global-lib >= 2.8

## Setup

Go to: Manage Jenkins --> Configure System --> Global Pipeline Libraries:

* Name: `mpl`
  * Default Version: `<empty>`
  * Load implicitly: `false`
  * Allow default version to be overridden: `true`
  * Include @Library changes in job recent changes: `true`
  * Retrieval method: `Modern SCM`
  * Source Code Management: `Git`
  * Project Repository: `https://github.com/griddynamics/mpl.git`
  * Behaviors: `Discover branches`, `Discover tags`

## Usage

You can use MPL in 3 different ways - it's 3 layers of the library:

* **Pipeline** - Standard pipelines implemented for you. Just use them for your project or as example for your custom pipeline.
* **Modules**  - Common steps you can find in each pipeline. Prepare your pipeline structure and just use the required modules.
* **Features** - Some useful libraries that you can use. Prepare your own pipeline with custom modules and use just features from the library.

### Jenkinsfile / Pipeline script

Just two lines to use default Master pipeline in your project Jenkinsfile or in the Jenkins Pipeline Script:
```
@Library('mpl@release') _
MPLPipeline {}
```

### Pipelines

You can find pipelines in the MPL library interfaces: `{MPL}/vars/MPL{Name}Pipeline.groovy` files.

There is 2 ways to slightly reconfigure the provided standard pipeline:
* Provide some configuration when you using pipeline in your jenkinsfile (see examples)
* Override pipeline modules in the project (see `Creating / Overriding steps modules` section)

If both ways are not helping - just create your own pipeline definition in the Jenkinsfile and use MPL modules or create your own nested library with your standard pipelines.

### Modules

MPL is mostly modules with logic. Basic features:

* Simple groovy sandbox step files with all the pipeline features
* Could be used in declarative & scripted pipelines
* Override system with loop protection and simple nesting

In fact modules could be loaded from a number of places:
* `{ProjectRepo}/.jenkins/modules/{Stage}/{Name}{Stage}.groovy` - custom modules for the project
* `{Library}/{SpecifiedModulesLoadPath}/modules/{Stage}/{Name}{Stage}.groovy` - custom modules load path that added while init
* `{Library}/resources/com/griddynamics/devops/mpl/modules/{Stage}/{Name}{Stage}.groovy` - library modules for everyone

If you will override module Build in your project repo, it will be used first.
If you will try to require Build module from this overridden Build module - original library module will be used.

Check the usage examples & library modules to get some clue about the nesting system.

### Creating / Overriding steps modules

If your project is special - you can override or provide aditional modules just for the project.

What do you need to do:
1. Create a step file: `{ProjectRepo}/.jenkins/modules/{Stage}/{Name}{Stage}.groovy` (name could be empty)
2. Fill the step with your required logic: you can use CFG flatten map variable & MPL functions inside the steps definition
3. Use this step in your custom pipeline (or, if it's override, in standard pipeline) via MPLModule method.
4. Provided custom modules will be available to use after the checkout of your project source only

For example: "Maven Build" steps have path `modules/Build/MavenBuild.groovy` and placed in the library - feel free to check it out.

### Post Steps

MPL supports 2 useful poststep interfaces which allow you to store all the logic of module in the same file.

All the poststeps will be executed in LIFO order and all the exceptions will be collected & displayed in the logs.

#### MPLModulePostStep

Allow to set some actions that need to be executed right after the current module (doesn't matter it fails or not).

Could be useful when you need to collect reports or clean stage agent before it will be killed.

If module post step fails - it's fatal for the module, so the pipeline will fail (unlike general poststeps). All the poststeps
for the module will be executed and errors will be printed, but module will fail.

`{NestedLibModules}/Build/MavenBuild.groovy`:
```
MPLModulePostStep {
  junit 'target/junitReport/*.xml'
}

// Could fail but our poststep will be executed
MPLModule('Maven Build', CFG)
```

#### MPLPostStep

General poststep interface usually used in the pipelines. Requires 2 calls - first one to define poststep in a module and second one
to execute it and usually placed in the pipeline post actions.

When error occurs during poststeps execution - it will be printed in the log, but status of pipeline will not be affected.

1. `{NestedLibModules}/Deploy/OpenshiftDeploy.groovy`:
   ```
   MPLPostStep('always') {
     echo "OpenShift Deploy Decomission poststep"
   }
 
   echo 'Executing Openshift Deploy process'
   ```
2. `{NestedLib}/var/CustomPipeline.groovy`:
   ```
   def call(body) {
     ...
     pipeline {
       ...
       stages {
         ...
         stage( 'Openshift Deploy' ) {
           steps {
             MPLModule()
           }
         }
         ...
       }
       post {
         always {
           MPLPostStepsRun('always')
         }
         success {
           MPLPostStepsRun('success')
         }
         failure {
           MPLPostStepsRun('failure')
         }
       }
     }
   }
   ```

### Enforcing modules

To make sure that some of your stages will be executed for sure - you can add a list of modules that could be overrided on the project side.
Just make sure, that you executing function `MPLEnforce` and provide list of modules that could be overriden in your pipeline script:
Jenkins job script:
```
@Library('mpl@release') _

// Only 'Build Maven' & 'Deploy' modules could be overriden on the project side
MPLEnforce(['Build Maven', 'Deploy'])

... // Your enforced pipeline
```
Notices:
* The function `MPLEnforce` could be executed only once, after that it will ignore any further executions.
* This trick is really working only if you controlling the job pipeline scripts, with Jenkinsfile it's not so secure.

### Nested libraries

MPL supporting the nested libraries to simplify work for a big teams which would like to use MPL but with some modifications.

Basically you just need to provide your `vars` interfaces and specify the mpl library to use it:

* `{NestedLib}/vars/MPLInit.groovy`:
  ```
  def call() {
    // Using the latest release MPL and adding the custom path to find modules
    library('mpl@release')
    MPLModulesPath('com/yourcompany/mpl')
  }
  ```
* `{NestedLib}/vars/NestedPipeline.groovy`:
  ```
  def call(body) {
    MPLInit() // Init the MPL library
    ...       // Specifying the configs / pipelines and using modules etc.
  }
  ```

And after that and configuring the library for your jenkins (just put it after the `mpl` config) - you can use it in the project's Jenkinsfile (see examples).

Also you can override resources of the MPL library - but it's forever. You can't use MPL resource anymore if you will override it in the nested library.

You can cover the nested library with tests as well as MPL library - please check the nested library example on wiki page.

## Release process

Jenkins shared libraries is just a repositories connected to the Jenkins Master instance. So you can use any branch/tag from the MPL or nested lib repo.

* branch:**18.04** - released version of the library. Can be used to pin the version.
* branch:**master** - most fresh functions (and bugs) are here. You can use it for testing purposes.
* branch:**TICKET-1234** - feature branch, could be used for testing purposes.

## Examples

### Wiki page examples

Please check the wiki page to see some MPL examples: [MPL Wiki](https://github.com/griddynamics/mpl/wiki)

### Standard Pipeline usage

If we fine with standard pipeline, but need to slightly modify options.

`{ProjectRepo}/Jenkinsfile`:
```
@Library('mpl@release') _

// Use default master pipeline
MPLPipeline {
  // Pipeline configuration override here
  // Example: (check available options in the pipeline definition)
  agent_label = 'LS'                     // Set agent label
  modules.Build.tool_version = 'Maven 2' // Change tool for build stage
  modules.Test = null                    // Disable Test stage
}
```

### Use Standard Pipeline but with custom module

We fine with standard pipeline, but would like to use different deploy stage.

* `{ProjectRepo}/Jenkinsfile`:
  ```
  @Library('mpl@release') _
  
  // Use default master pipeline
  MPLPipeline {}
  ```
* `{ProjectRepo}/.jenkins/modules/Deploy/Deploy.groovy`:
  ```
  // Any step could be here, config modification, etc.
  echo "Let's begin the deployment process!"
  
  // Run original deployment from the library
  MPLModule('Deploy', CFG)
  
  echo "Deployment process completed!"
  ```

### Custom Declarative Pipeline with mixed steps

`{ProjectRepo}/Jenkinsfile`:
```
@Library('mpl@release') _

pipeline {  // Declarative pipeline
  agent {
    label 'LS'
  }
  stages {
    stage( 'Build' ) {
      parallel {        // Parallel build for 2 subprojects
        stage( 'Build Project A' ) {
          steps {
            dir( 'subProjectA' ) {
              MPLModule('Maven Build', [ // Using common Maven Build with specified configs
                tool_version: 'Maven 2'
              ])
            }
          }
        }
        stage( 'Build Project B' ) {
          steps {
            dir( 'subProjectB' ) {
              // Custom build process (it's better to put it into the project custom module)
              sh 'gradle build'
              sh 'cp -a target my_data'
            }
          }
        }
      }
    }
  }
}
```

### Using nested library (based on MPL)

`{ProjectRepo}/Jenkinsfile`:
```
@Library('nested-mpl@release') _

NestedPipeline {
  // config here
}
```

## Contribution

### Tests

We should be ensure that modules will not be broken accidentally, so tests is mandatory.

MPL supports MPLModule testing via slightly modified [JenkinsPipelineUnit](https://github.com/jenkinsci/JenkinsPipelineUnit).
You can find module tests (as well as modified base test classes & overridden requirements classes) in the `test` directory.

To run tests just execute `mvn clean test` - and it will compile the classes & execute tests for the modules.

### Pipelines

MPL provided pipelines should be really simple, but could be improved with new best practices - so changes are always welcome.

### Modules

If you have some interesting module - you for sure can prepare changes for existing module or a new module, write tests, create
the pull-request, describe it - and after that this module could be approved to be included n the base library. If not - you always
could use (or create) your own nested library to share this fancy module across your projects.
