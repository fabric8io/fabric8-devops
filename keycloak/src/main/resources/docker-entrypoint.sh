#!/bin/bash

set -e

export JBOSS_HOME="/opt/jboss/keycloak"

if [ "$2" = 'ssl' ]; then
    # Configure WildFly to use HTTPS & SSL
    sed -i -e 's/<security-realms>/&\n            <security-realm name="UndertowRealm">\n                <server-identities>\n                    <ssl>\n                        <keystore path="keycloak.jks" relative-to="jboss.server.config.dir" keystore-password="supersecret" \/>\n                    <\/ssl>\n                <\/server-identities>\n            <\/security-realm>/' $JBOSS_HOME/standalone/configuration/standalone.xml
    sed -i -e 's/<server name="default-server">/&\n                <https-listener name="https" socket-binding="https" security-realm="UndertowRealm"\/>/' $JBOSS_HOME/standalone/configuration/standalone.xml
    # Change Own & Group for KeyStore file
    cp /secret/keycloak/keystore $JBOSS_HOME/standalone/configuration/keycloak.jks
    chown jboss:jboss $JBOSS_HOME/standalone/configuration/keycloak.jks
fi

# Drop root privileges if we are running elasticsearch
if [ "$1" = 'keycloak' ]; then
    if [ $KEYCLOAK_USER ] && [ $KEYCLOAK_PASSWORD ]; then
        echo "Admin account created for : ${KEYCLOAK_USER}"
        keycloak/bin/add-user-keycloak.sh --user $KEYCLOAK_USER --password $KEYCLOAK_PASSWORD
    fi
    exec "/opt/jboss/keycloak/bin/standalone.sh" "-b=0.0.0.0" "-bmanagement=0.0.0.0"
fi