#!/bin/bash

source `dirname $0`/config.sh

az group delete --name $PAINTBOTS_AZURE_RG
