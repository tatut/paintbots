#!/bin/bash

source config.sh


az group create --name $PAINTBOTS_AZURE_RG \
   --location northeurope \
   --tags Owner=$PAINTBOTS_AZURE_OWNER DueDate=$PAINTBOTS_AZURE_DUEDATE

# az group delete --name $PAINTBOTS_AZURE_RG
