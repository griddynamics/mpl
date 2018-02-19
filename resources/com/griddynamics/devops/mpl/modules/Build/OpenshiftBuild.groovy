/**
 * Build openshift containers
 */

withCredentials([string(credentialsId: CFG.openshift.credential, variable: 'OPENSHIFT_TOKEN')]) {
  echo "${Openshift.readConfig(readFile('BuildConfig.yaml'))}"
  echo "${Openshift.test(CFG.openshift.url, env.OPENSHIFT_TOKEN)}"
}
