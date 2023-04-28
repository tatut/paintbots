#!/bin/bash

source `dirname $0`/config.sh

az appservice plan create \
   --resource-group $PAINTBOTS_AZURE_RG \
   --name $PAINTBOTS_AZURE_PLAN \
   --is-linux

az webapp create \
   --resource-group $PAINTBOTS_AZURE_RG \
   --plan $PAINTBOTS_AZURE_PLAN \
   --name $PAINTBOTS_AZURE_APPNAME \
   --deployment-container-image-name $PAINTBOTS_AZURE_REGISTRY.azurecr.io/$PAINTBOTS_AZURE_IMAGE:latest


az webapp config appsettings set \
   --resource-group $PAINTBOTS_AZURE_RG \
   --name $PAINTBOTS_AZURE_APPNAME \
   --settings WEBSITES_PORT=31173
