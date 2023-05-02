#!/usr/bin/env bash

set -o errexit # abort on nonzero exitstatus
set -o nounset # abort on unbound variable
set -o pipefail # don't hide errors within pipes

source "$(dirname "$0")"/config.sh

az acr create --name "$PAINTBOTS_AZURE_REGISTRY" \
   --resource-group "$PAINTBOTS_AZURE_RG" \
   --sku standard \
   --admin-enabled true

