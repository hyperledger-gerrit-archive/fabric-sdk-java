#!/usr/bin/env bash
#
# Copyright IBM Corp. All Rights Reserved.
#
# SPDX-License-Identifier: Apache-2.0
#
#file used for automatic integration build test
#This should always match what's in the README.md

export FABRIC_COMMIT=latest
export FABRIC_CA_COMMIT=latest
# if below are set then fabric/ca won't be built.
#export FABRIC_NO_BUILD=true
#export FABRIC_CA_NO_BUILD=true


case "$ORG_HYPERLEDGER_FABRIC_SDKTEST_VERSION" in
"1.0.0" | "1.1.0" )
echo "NOT DOING BUILD ORG_HYPERLEDGER_FABRIC_SDKTEST_VERSION=$ORG_HYPERLEDGER_FABRIC_SDKTEST_VERSION"
export FABRIC_NO_BUILD=true
export FABRIC_CA_NO_BUILD=true
    ;;
*)
unset FABRIC_NO_BUILD
unset FABRIC_CA_NO_BUILD
    ;;
esac
