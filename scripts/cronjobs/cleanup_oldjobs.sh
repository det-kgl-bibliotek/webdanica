#!/usr/bin/env bash

SCRIPT_DIR=$(dirname $(readlink -f $BASH_SOURCE[0]))

[ -e $SCRIPT_DIR/setenv.sh ] && source $SCRIPT_DIR/setenv.sh

NAS_INSTALL=${NAS_INSTALL:-/home/test/WEBDANICA}
OLDJOBS=${OLDJOBS:-}$NAS_INSTALL/oldjobs}

ME=$(readlink -f $BASH_SOURCE[0])

if [ ! -d "$NAS_INSTALL" ]; then
  echo ERROR: The netarchivesuite installdir \"$NAS_INSTALL\" does not exist. Please correct the path in $ME
  exit 1
fi

if [[ -d ${OLDJOBS} ]]; then	
  cd $OLDJOBS
  rm -rfv */**/lib
fi

