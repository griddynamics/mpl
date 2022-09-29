# Dockerfile to put plugins and other vars to the image
ARG JENKINS_VERSION

FROM jenkins/jenkins:${JENKINS_VERSION}-jdk11

COPY --chown=jenkins:jenkins plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli -f /usr/share/jenkins/ref/plugins.txt

ENV CASC_JENKINS_CONFIG=/usr/local/jenkins-casc \
    CASC_GROOVY_TESTS_PATH=/usr/local/jenkins-tests \
    JENKINS_HOSTNAME=localhost \
    JAVA_OPTS="-Djenkins.install.runSetupWizard=false ${JAVA_OPTS:-}"

COPY --chown=root:jenkins jenkins-casc.yml ${CASC_JENKINS_CONFIG}/

COPY --chown=root:jenkins tests ${CASC_GROOVY_TESTS_PATH}/
