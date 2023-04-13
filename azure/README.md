# Deploy paintbots to Azure

First write your config to `config.sh`. It is used by all the scripts.

Setup:
```bash
# Login to azure
az login

# copy config_sample.sh to config.sh and change your settigns
cp azure/config_sample.sh azure/config.sh

# Create resource group
./azure/create_group.sh

# Create ACR registry
./azure/create_registry.sh

# Build
./azure/build.sh

# Create and configure webapp
./azure/create_webapp.sh

```
