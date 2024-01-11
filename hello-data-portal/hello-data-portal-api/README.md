User lookup Service
========================

If you want to start the Portal API with the Local-/Mock-UserLookupService for finding users to register:

Start with Spring boot profile 'local-user-search'.


If this profile is ommited, the LdapUserLookupService will be used instead.

Configure ldap-server:
----------------------

application.yaml:

````
hello-data:
  ldap:
    url: ldap://ldap.forumsys.com:389
    base: dc=example,dc=com
    username: cn=read-only-admin,dc=example,dc=com
    password: password
````
Adjust the settings accordingly. 

The configuration above will take the publicly available ldap test server as described on:
https://www.forumsys.com/2022/05/10/online-ldap-test-server/

Configure ldap health actuator:
-------------------------------

application.yaml:

````
management:
    health:
        ldap:
            enabled: false # Disable this when not using ldap connection
````

This should be set to false if using the mock user lookup.