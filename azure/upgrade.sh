#!/bin/bash

source `dirname $0`/config.sh

az webapp config container set \
   --name $PAINTBOTS_AZURE_APPNAME \
   --resource-group $PAINTBOTS_AZURE_RG \
   --docker-custom-image-name $PAINTBOTS_AZURE_IMAGE:latest
