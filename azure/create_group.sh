#!/usr/bin/env bash

set -o errexit # abort on nonzero exitstatus
set -o nounset # abort on unbound variable
set -o pipefail # don't hide errors within pipes

source "$(dirname "$0")"/config.sh

az group create --name "$PAINTBOTS_AZURE_RG" \
   --location "$PAINTBOTS_AZURE_REGION" \
   --tags Owner="$PAINTBOTS_AZURE_OWNER" DueDate="$PAINTBOTS_AZURE_DUEDATE"
