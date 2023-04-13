#!/bin/bash

source `dirname $0`/config.sh

az acr build --file Dockerfile --registry $PAINTBOTS_AZURE_REGISTRY --image $PAINTBOTS_AZURE_IMAGE .
