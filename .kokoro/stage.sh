#!/bin/bash
# Copyright 2023 Google LLC
#
# This library is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 2.1 of the License, or (at your option) any later version.
#
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with this library; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA

set -eov pipefail

dir=$(dirname "$0")

source "${dir}"/common.sh

pushd "${dir}"/../

MAVEN_SETTINGS_FILE=$(realpath .)/settings.xml

setup_environment_secrets
create_settings_xml_file "${MAVEN_SETTINGS_FILE}"

# Use GCP Maven Mirror
mkdir -p "${HOME}"/.m2
cp settings.xml "${HOME}"/.m2

# AOSS 1P authentication verification (go/aoss1p:oss-binaries#usage-instructions)
echo "gcloud config get-value core/account:"
gcloud config get-value core/account
echo "-------- end of gcloud --------"
echo "AOSS authentication test:"
wget --header="Authorization: Bearer $(gcloud auth print-access-token)" https://us-maven.pkg.dev/cloud-aoss-1p/cloud-aoss-1p-java/log4j/log4j/1.2.17/log4j-1.2.17.pom
echo "-------- end of AOSS authentication test ------"

# gcloud components install app-engine-java --quiet
# apt-get install -y google-cloud-cli-app-engine-java

echo "Staging a release"
# stage release
./mvnw clean deploy \
  -Dorg.slf4j.simpleLogger.showDateTime=true \
  -Dorg.slf4j.simpleLogger.dateTimeFormat=HH:mm:ss:SSS \
  --show-version \
  --batch-mode \
  --settings "${MAVEN_SETTINGS_FILE}" \
  -Paoss \
  -DskipTests=true \
  -DperformRelease=true \
  -Dgpg.executable=gpg \
  -Dgpg.passphrase="${GPG_PASSPHRASE}" \
  -Dgpg.homedir="${GPG_HOMEDIR}"

echo "Successfully finished 'mvn deploy'"

# promote release
if [[ -n "${AUTORELEASE_PR}" ]]; then
  echo "Promoting the staged repository"
  ./mvnw nexus-staging:release \
    --batch-mode \
    --settings "${MAVEN_SETTINGS_FILE}" \
    --activate-profiles release-staging-repository \
    -DperformRelease=true
  echo "Successfully finished 'mvn nexus-staging:release'"
else
  echo "AUTORELEASE_PR environment variable is not set (probably testing something). Not promoting the staged repository."
fi

# release app-gradle-plugin
GRADLE_SETTING_FILE=$(realpath .)/app-gradle-plugin/gradle.properties
create_gradle_properties_file "${GRADLE_SETTING_FILE}"
pushd app-gradle-plugin
if [[ -n "${AUTORELEASE_PR}" ]]; then
  ./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository
  echo "Successfully finished './gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository'"
else
  ./gradlew publishToSonatype
fi
popd # app-gradle-plugin
popd # repository root
