/**
 * Simple Maven Build
 */

withEnv(["PATH+MAVEN=${tool CFG.maven.tool_version}/bin"]) {
  sh """mvn -B -s '${CFG.maven.settings_path}' -DargLine='-Xmx1024m -XX:MaxPermSize=1024m' clean install"""
}
