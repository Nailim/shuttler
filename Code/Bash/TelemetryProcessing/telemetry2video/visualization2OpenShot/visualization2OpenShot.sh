#! /bin/bash

# $0 - path to script
# $1 - path to data folder
# $2 - path to processed data folder
# $3 - panorama run time
# $4 - attitude run time (all generated)

scriptPath=$0
scriptPath=${scriptPath%/*}

workPath=`pwd`

cat "$scriptPath/visualization_template.osp" | sed ''s/#data#/$1/g'' | sed ''s/#processedData#/$2/g'' | sed ''s/#panoramaTime#/$3/g'' | sed ''s/#attitudeTime#/$4/g'' | sed ''s/#pathTime#/$(bc <<< "scale=2; $4+10")/g''> "$workPath/$2/media/visualization.osp"
