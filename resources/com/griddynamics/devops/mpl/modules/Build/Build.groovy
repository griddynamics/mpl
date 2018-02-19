/**
 * Common build module
 */

MPLModule('Maven Build', CFG)

if( fileExists('openshift') ) {
  MPLModule('Openshift Build', CFG)
}
