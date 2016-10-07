#!/bin/sh
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

USAGE=`basename $0`

# Generate the PEM format keys and certificates for the CA, server and client
# and JKS trust store and key stores for java clients.
#
# Consistent with the test's expectations,
# "passw0rd" is used in all cases where a pass phrase is used.

# hints:
# - to see the textual contents of a PEM certificate file:
#   openssl x509 -text -in server.crt
#
# - to see the textual contents of a PEM RSA key file:
#   openssl rsa -text -in server.key  # will prompt for a pass phrase if encrypted
#

set -e
set -x

# ================================================================
# Generating CA, server, and client keys/certificates for use with mosquitto
# https://mosquitto.org/man/mosquitto-tls-7.html
#
# Pay heed to: "It is important to use different certificate subject parameters for your CA, server and clients"

echo;echo =====  Generating CA, server, and client keys/certificates

echo;echo =====  Generate encrypted RSA CA key and certificate
openssl req -new -x509 -days 7300 -passout pass:passw0rd -extensions v3_ca \
    -keyout ca.key -out ca.crt <<xxEOFxx
US
MA
Littleton
IBM
Edgent
My Test Mosquitto CA
foo@test.org
xxEOFxx

echo;echo =====  Generate unencrypted RSA Server key
openssl genrsa -out server.key 2048

echo;echo =====  Generate an encrypted server certificate signing request to send to the CA.
openssl req -new -passout pass:passw0rd -out server.csr -key server.key <<xxEOFxx
US
MA
Littleton
IBM
Edgent
My Test Mosquitto Server
foo@test.org


xxEOFxx

echo;echo =====  Generate the Server cert by signing the certificate request with the CA key
openssl x509 -req -passin pass:passw0rd -in server.csr -out server.crt \
    -days 7300 -CA ca.crt -CAkey ca.key -CAcreateserial

echo;echo =====  Generate an unencrypted RSA Client key
openssl genrsa -out client.key 2048

echo;echo =====  Generate an encrypted client certificate signing request to send to the CA.
openssl req -new -passout pass:passw0rd -out client.csr -key client.key <<xxEOFxx
US
MA
Littleton
IBM
Edgent
My Test Mosquitto Client
foo@test.org


xxEOFxx

echo;echo =====  Generate the Client cert by signing the certificate request with the CA key.
openssl x509 -req -passin pass:passw0rd -in client.csr -out client.crt \
    -days 7300 -CA ca.crt -CAkey ca.key -CAcreateserial 

# ================================================================
# Create the JKS stores for java clients
# https://docs.oracle.com/cd/E35976_01/server.740/es_admin/src/tadm_ssl_convert_pem_to_jks.html

echo;echo =====  Create JKS stores for MqttStreams '(paho client)'  

# Create an empty clientTrustStore.jks
echo;echo =====  Create clientTrustStore.jks  
echo;echo =====  Create an empty clientTrustStore.jks  
keytool -genkey -keyalg RSA -alias xyzzy \
    -dname 'CN=foo.example.com,L=Melbourne,ST=Victoria,C=AU' \
    -keypass passw0rd -keystore clientTrustStore.jks -storepass passw0rd 
keytool -delete -alias xyzzy \
    -keystore clientTrustStore.jks -storepass passw0rd 

echo;echo =====  Add the CA cert to the clientTrustStore.jks  
keytool -import -v -trustcacerts -alias my-mosquitto-ca -file ca.crt \
    -keystore clientTrustStore.jks -storepass passw0rd <<xxEOFxx
yes
xxEOFxx

echo;echo =====  Add the mosquitto.org cert to clientTrustStore.jks
keytool -import -v -trustcacerts -alias mosquitto.org -file mosquitto.org.crt \
    -keystore clientTrustStore.jks -storepass passw0rd <<xxEOFxx
yes
xxEOFxx

echo;echo =====  Create clientKeyStore.jks  
echo;echo =====  Create an empty clientKeyStore.jks  
keytool -genkey -keyalg RSA -alias xyzzy \
    -dname 'CN=foo.example.com,L=Melbourne,ST=Victoria,C=AU' \
    -keypass passw0rd -keystore clientKeyStore.jks -storepass passw0rd 
keytool -delete -alias xyzzy \
    -keystore clientKeyStore.jks -storepass passw0rd 

echo;echo  =====  Add the client key to clientKeyStore.jks '(PEM => pkcs12 => jks)'
openssl pkcs12 -export -out client.p12 -passout pass:passw0rd \
    -passin pass:passw0rd -inkey client.key -in client.crt -certfile ca.crt
keytool -v -importkeystore \
    -srckeystore client.p12 -srcstorepass passw0rd -srcstoretype PKCS12 \
    -destkeystore clientKeyStore.jks -deststorepass passw0rd -deststoretype JKS

# show store contents
echo;echo =====  clientTrustStore
keytool -list -storepass passw0rd -keystore clientTrustStore.jks
echo;echo =====  clientKeyStore
keytool -list -storepass passw0rd -keystore clientKeyStore.jks
