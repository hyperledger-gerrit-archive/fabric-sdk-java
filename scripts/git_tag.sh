#!/bin/sh
#
# Copyright IBM Corp. All Rights Reserved.
#
# SPDX-License-Identifier: Apache-2.0
#

# Exit on first error, print all commands.
set -e
set -o pipefail
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )"

# release name
RELEASE=release-1.4

function abort {
	echo "!! Exiting shell script"
	echo "!!" "$1"
	exit -1
}

# Check user has xq installed
if ! which xq; then
	echo "Install jq and yq to get xq"
	abort "xq not installed, required for parsing the version";
fi

# Grab the version defined in pom.xml and strip quotes and characters
VERSION=$(xq '.project.version' ${DIR}/pom.xml | sed -r "s/\"([0-9]?[0-9]\.[0-9]?[0-9]\.[0-9]?[0-9]).*/\1/")

echo New version string will be v${VERSION}

# do the release notes for this new version exist?
if [[ -f "${DIR}/release_notes/v${VERSION}.txt" ]]; then
   echo "Release notes exist, hope they make sense!"
else
   abort "No releases notes under the file ${DIR}/release_notes/v${NEW_VERSION}.txt exist";
fi

git checkout "${RELEASE}"
git pull
git tag -a "v${VERSION}" `git log -n 1 --pretty=oneline | head -c7` -F release_notes/"v${VERSION}".txt
git push origin v${VERSION} HEAD:refs/heads/${RELEASE}