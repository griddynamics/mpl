/**
 * Build openshift containers
 */

withCredentials([string(credentialsId: CFG.'openshift.credential', variable: 'OPENSHIFT_TOKEN')]) {
  echo 'TODO: Build openshift container'
}
