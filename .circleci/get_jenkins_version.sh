#!/bin/sh
#
# Script to find of the lts/latest docker image version of jenkins
#
# Usage:
#   $ ./jenkinsbro_build.sh <VERSION>
#
# Parameters:
#   VERSION - allows to check the version to use, values:
#     * "2.176."  - prefix of the jenkins version, will choose the latest available LTS 2.176.*
#     * "2.176.2" - exact version you want
#     * "lts"     - latest stable version
#     * "latest"  - just latest version
#

VAR_VERSION=$1

if [ -z "${VAR_VERSION}" ]; then
    echo Specify the version you need
    exit 1
fi

ver_url=https://mirrors.jenkins.io/war
ver="[0-9]"
if [ "${VAR_VERSION}" = 'lts' ]; then
    ver_url=${ver_url}-stable
elif [ "${VAR_VERSION}" != 'latest' ]; then
    ver=$(echo "${VAR_VERSION}" | tr -dc '0-9.')
    echo "Using specified version '${ver}'" 1>&2
fi

# Get version-1 to not complicate the dockerhub checks - sometimes images are not here for the fresh versions
jenkins_version=$(curl -s "${ver_url}/" | grep -oE 'href="'${ver}'[^/]*' | head -2 | tail -1 | tr -dc "0-9.\n")
if [ "x$jenkins_version" = "x" ]; then
    echo "Unable to find version for: ${VAR_VERSION} '${jenkins_version}'" 1>&2
    exit 1
fi

echo "${jenkins_version}"
