#!/bin/bash

source `dirname $0`/config.sh

az acr create --name $PAINTBOTS_AZURE_REGISTRY \
   --resource-group $PAINTBOTS_AZURE_RG \
   --sku standard \
   --admin-enabled true

