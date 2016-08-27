#!/usr/bin/env sh

set -e
set -u

targetDir="$( dirname "$( readlink -f "$0" )" )"
target="${targetDir}/dist.jar"

exec java -jar "$target" "$@"

