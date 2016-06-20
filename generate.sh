#!/bin/sh

GROUP_BY=""
OUTPUT_FILE=$(date +%m%d_%H%M)
INPUT="stacks.txt"

while getopts "g:f:i:" opt; do
	case "$opt" in
	g)
	  GROUP_BY=${OPTARG}
	  ;;
	i)
	  INPUT=${OPTARG}
	  ;;
	f)
	  OUTPUT_FILE=${OPTARG}
	  ;;
	esac
done


java -jar target/java-allocation-instrumenter-3.0-SNAPSHOT.jar ${INPUT}  ${GROUP_BY}
FlameGraph/flamegraph.pl collapsed.txt > ${OUTPUT_FILE}.html
