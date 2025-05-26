#!/bin/sh -e

#    Licensed to the Apache Software Foundation (ASF) under one or more
#    contributor license agreements.  See the NOTICE file distributed with
#    this work for additional information regarding copyright ownership.
#    The ASF licenses this file to You under the Apache License, Version 2.0
#    (the "License"); you may not use this file except in compliance with
#    the License.  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS,
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#    See the License for the specific language governing permissions and
#    limitations under the License.

scripts_dir='/opt/nifi/scripts'

[ -f "${scripts_dir}/common.sh" ] && . "${scripts_dir}/common.sh"

# Override JVM memory settings
if [ ! -z "${NIFI_JVM_HEAP_INIT}" ]; then
    prop_replace 'java.arg.2'       "-Xms${NIFI_JVM_HEAP_INIT}" ${nifi_bootstrap_file}
fi

if [ ! -z "${NIFI_JVM_HEAP_MAX}" ]; then
    prop_replace 'java.arg.3'       "-Xmx${NIFI_JVM_HEAP_MAX}" ${nifi_bootstrap_file}
fi

if [ ! -z "${NIFI_JVM_DEBUGGER}" ]; then
    uncomment "java.arg.debug" ${nifi_bootstrap_file}
fi

if [ -n "${NIFI_SENSITIVE_PROPS_KEY}" ]; then
    prop_replace 'nifi.sensitive.props.key' "${NIFI_SENSITIVE_PROPS_KEY}"
fi

if [ -n "${SINGLE_USER_CREDENTIALS_USERNAME}" ] && [ -n "${SINGLE_USER_CREDENTIALS_PASSWORD}" ]; then
    ${NIFI_HOME}/bin/nifi.sh set-single-user-credentials "${SINGLE_USER_CREDENTIALS_USERNAME}" "${SINGLE_USER_CREDENTIALS_PASSWORD}"
fi

if [ -n "${FULL_HOSTNAME}" ]; then
    prop_replace 'nifi.web.https.host' "${FULL_HOSTNAME}"
    prop_replace 'nifi.cluster.node.address' "${FULL_HOSTNAME}"
    prop_replace 'nifi.remote.input.host' "${FULL_HOSTNAME}"
fi

if [ -n "${KEYSTORE_PASSWORD}" ]; then
    prop_replace 'nifi.security.keystorePasswd' "${KEYSTORE_PASSWORD}"
fi

if [ -n "${TRUSTSTORE_PASSWORD}" ]; then
    prop_replace 'nifi.security.truststorePasswd' "${TRUSTSTORE_PASSWORD}"
fi

if [ -n "${KEY_PASSWORD}" ]; then
    prop_replace 'nifi.security.keyPasswd' "${KEY_PASSWORD}"
fi

# Check if we are secured or unsecured
case ${AUTH} in
    tls)
        echo 'Enabling Two-Way SSL user authentication'
        # . "${scripts_dir}/secure.sh"
        ;;
    ldap)
        echo 'Enabling LDAP user authentication'
        # Reference ldap-provider in properties
        export NIFI_SECURITY_USER_LOGIN_IDENTITY_PROVIDER="ldap-provider"

        # . "${scripts_dir}/secure.sh"
        . "${scripts_dir}/update_login_providers.sh"
        ;;
    oidc)
        echo 'Enabling OIDC user authentication'

        # . "${scripts_dir}/secure.sh"
        . "${scripts_dir}/update_oidc_properties.sh"
        ;;
esac

# Continuously provide logs so that 'docker logs' can produce them
"${NIFI_HOME}/bin/nifi.sh" run &
nifi_pid="$!"
tail -F --pid=${nifi_pid} "${NIFI_HOME}/logs/nifi-app.log" &

trap 'echo Received trapped signal, beginning shutdown...;./bin/nifi.sh stop;exit 0;' TERM HUP INT;
trap ":" EXIT

echo NiFi running with PID ${nifi_pid}.
wait ${nifi_pid}
