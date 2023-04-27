#!/bin/bash

source `dirname $0`/config.sh

az group create --name $PAINTBOTS_AZURE_RG \
   --location $PAINTBOTS_AZURE_REGION \
   --tags Owner=$PAINTBOTS_AZURE_OWNER DueDate=$PAINTBOTS_AZURE_DUEDATE

# az group delete --name $PAINTBOTS_AZURE_RG
