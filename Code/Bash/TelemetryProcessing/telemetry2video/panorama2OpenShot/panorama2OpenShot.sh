#! /bin/bash

# $0 - path to script
# $1 - path to data folder
# $2 - path to processed data folder
# $3 - panorama run time

scriptPath=$0
scriptPath=${scriptPath%/*}

workPath=`pwd`

cat "$scriptPath/panorama_template.osp" | sed ''s/#data#/$1/g'' | sed ''s/#processedData#/$2/g'' | sed ''s/#panoramaTime#/$3/g''> "$workPath/$2/media/panorama.osp"
