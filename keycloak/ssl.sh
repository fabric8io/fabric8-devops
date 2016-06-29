#!/bin/bash

# This script is called by maven to deploy the secret after the maven validate phase.
# Note that for this script to succeed you must oc login first

oc whoami > /dev/null
if [ $? == 1 ]; then
  echo "[ERROR] Not logged in, no 'keycloak-keystore' secret is created"
  exit 0;
fi

# deleting existing secret if there
oc get secret keycloak-keystore &> /dev/null
if [ $? == 0 ]; then
  echo "[INFO] Delete existing secret/keycloak-keystore"
  oc delete secret keycloak-keystore
fi
# create the new secret
echo Deploying secret keycloak-keystore
oc secrets new keycloak-keystore keystore=target/secret/keycloak/keystore keystore.password=target/secret/keycloak/keystore.password
