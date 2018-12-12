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
# if below is set then fabric/ca won't be built.
export FABRIC_NOBUILD=true
export FABRIC_CA_NOBUILD=true
