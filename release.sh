#!/bin/bash
#BINARIES="./bin/*"
REPO="gigapaxos"
OWNER="MobilityFirst"
USAGE='Usage: "./release.sh Description for the release notes.".  To change auth token, use: export GNSGitToken="YOUR_GITHUB_AUTH_TOKEN"'


if [ "$1" == "help" ]; then
	echo $USAGE
	exit 0
fi



if [ $# -lt 1 ]; then
	echo $USAGE
	exit 1
fi
#VERSION="$1" #First argument is the version
MAJOR=$(cat build.properties | grep "^build.major" | sed 's/^build.major.number=//' | sed s/$(printf '\r')\$//)
echo $MAJOR.
MINOR=$(cat build.properties | grep "^build.minor" | sed 's/^build.minor.number=//'| sed s/$(printf '\r')\$//)
echo $MINOR.
REVISION=$(cat build.properties | grep "^build.revision" | sed 's/^build.revision.number=//'| sed s/$(printf '\r')\$//)
echo $REVISION.
VERSION=v$MAJOR.$MINOR.$REVISION
echo $VERSION
BINARIES="gigapaxos-$MAJOR.$MINOR.$REVISION"
#DESCRIPTION="${*:2}" #Second argument and on is the description.
DESCRIPTION="$@" #The commandline argument is the description.
# First prompt for an auth token if one is not already stored.
if [ "$GNSGitToken" == "" ]; then
	echo "Please enter a git token that has write access to the GNS repository: "
	read input
	export GNSGitToken="$input"
fi
# Create a new release on github
curl --data '{"tag_name": "'"$VERSION"'","target_commitish": "master","name": "'"$VERSION"'","body": "'"$DESCRIPTION"'","draft": false,"prerelease": false}' https://api.github.com/repos/$OWNER/$REPO/releases?access_token=$GNSGitToken
RELEASEID=$(curl "https://api.github.com/repos/$OWNER/$REPO/releases/tags/$VERSION" | grep id\": | sed 's/.*id": //' | sed 's/,$//' | grep [0-9][0-9]* -m 1 )
#curl "https://api.github.com/repos/$OWNER/$REPO/releases/tags/$VERSION" > testFile2.txt
echo "ID: $RELEASEID"
# Tar the release binaries, and add them to the release.
TARNAME="$BINARIES.tgz"
tar zcf $TARNAME $BINARIES 
echo "https://api.github.com/repos/$OWNER/$REPO/releases/$RELEASEID/assets?name=$TARNAME&access_token=$GNSGitToken"
curl -X POST --header "Content-Type:application/gzip" --data-binary @"$TARNAME" "https://uploads.github.com/repos/$OWNER/$REPO/releases/$RELEASEID/assets?name=$TARNAME&access_token=$GNSGitToken"
