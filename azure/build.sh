#!/usr/bin/env bash

set -o errexit # abort on nonzero exitstatus
set -o nounset # abort on unbound variable
set -o pipefail # don't hide errors within pipes

source "$(dirname "$0")"/config.sh

az acr build --file Dockerfile --registry "$PAINTBOTS_AZURE_REGISTRY" --image "$PAINTBOTS_AZURE_IMAGE" .
