/**
 * Common build module
 */

if ( fileExists('build.gradle')) {
  MPLModule('Gradle Build', CFG)
} else {
  MPLModule('Maven Build', CFG)
}

if( fileExists('openshift') ) {
  MPLModule('Openshift Build', CFG)
}
