#!/usr/bin/env bash

SCRIPT_DIR=$(dirname $(readlink -f ${BASH_SOURCE[0]}))

PROG=$(readlink -f ${BASH_SOURCE[0]})

source $SCRIPT_DIR/common.sh

PIGBOOTUP_FILE=$1

if [ ! -f ${PIGBOOTUP_FILE} ]; then
   echo "ERROR: The pig-bootup file '$PIGBOOTUP_FILE' does not exist." 
   exit 1
fi

LIBS=$(grep REGISTER ${PIGBOOTUP_FILE} | grep -v "\-\-" | cut -d ' ' -f2 | xargs -r -i bash -c "eval ls -1 '${SCRIPT_DIR}/{}'")

for L in $LIBS; do
	if [ ! -f "$L" ]; then
	   echo  "MISSING library '$L' in $PIGBOOTUP_FILE"
	   exit 1
	fi
done

METHODS=$(grep DEFINE ${PIGBOOTUP_FILE}  | grep -v "\-\-" | awk '$1=$1' | cut -d ' ' -f3 | tr -d '();')

let FAILURE=0
for M in ${METHODS}; do
	# check if  M is part of the libraries in the LIBS list
	let FOUND=0
	for L in $LIBS; do
		RES=$(grep ${M} "$L")
		if [ "$RES" != "" ]; then
			let FOUND=1
			#echo "Found method $M in library $L"
		fi
	done

	if [ ${FOUND} -eq 0 ]; then
		echo "FAILURE to find method '$M' in the registered libraries."
		let FAILURE=1
	fi
done

if [ ${FAILURE} -eq 1 ]; then
	echo "Missing method definitions in the registered libraries '$LIBS'"
	echo "Please REGISTER the missing library/libraries"
	exit 1
else 
	exit 0
fi
  
 	
