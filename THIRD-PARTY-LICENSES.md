<!--toc:start-->

- [Python](#python)
- [Java](#java)
- [Frontend (JavaScript / TypeScript)](#frontend-javascript--typescript)
- [Infrastructure](#infrastructure)
- [Licence SPDX](#licence-spdx)

<!--toc:end-->

This project uses open-source software:

## Python

| Component        | Version | License                    |
|------------------|---------|----------------------------|
| Apache Airflow   | 2.8.1   | Apache-2.0                 |
| Apache Superset  | 5.x     | Apache-2.0                 |
| dbt-core         |         | Apache-2.0                 |
| dbt-postgres     |         | Apache-2.0                 |
| authlib          | 1.6.9   | BSD-2-Clause               |
| celery           |         | BSD-3-Clause               |
| cryptography     |         | Apache-2.0 or BSD-3-Clause |
| flask            |         | BSD-3-Clause               |
| flask-appbuilder |         | BSD-3-Clause               |
| flask-cors       |         | MIT                        |
| flask-login      |         | MIT                        |
| flask-oidc       | 2.4.0   | BSD-2-Clause               |
| flask-openid     | 1.3.1   | BSD-2-Clause               |
| flask-talisman   | 1.1.0   | MIT                        |
| flower           |         | BSD-2-Clause               |
| gevent           |         | MIT                        |
| jmespath         | 1.0.1   | MIT                        |
| jwt (PyJWT)      |         | MIT                        |
| mkdocs-material  |         | MIT                        |
| oracledb         |         | Apache-2.0                 |
| psycopg2-binary  | 2.9.11  | LGPL-2.1+                  |
| pyaxis           |         | Apache-2.0                 |
| pygdaltools      |         | GPL-3.0                    |
| pymssql          |         | LGPL-2.1+                  |
| redis (Python)   | 7.1.0   | MIT                        |
| requests         |         | Apache-2.0                 |
| statsd           |         | MIT                        |
| werkzeug         |         | BSD-3-Clause               |
| pyyaml           |         | MIT                        |

## Java

| Component                      | Version       | License                                    |
|--------------------------------|---------------|--------------------------------------------|
| Apache Commons Collections     | 4.5.0         | Apache-2.0                                 |
| Apache Commons CSV             | 1.14.1        | Apache-2.0                                 |
| Apache Commons IO              | 2.21.0        | Apache-2.0                                 |
| Apache Commons Validator       | 1.10.1        | Apache-2.0                                 |
| Apache HttpClient              | 5.6           | Apache-2.0                                 |
| Apache HttpMime                | 4.5.14        | Apache-2.0                                 |
| Docker Java API                | 3.7.1         | Apache-2.0                                 |
| Jackson Annotations            |               | Apache-2.0                                 |
| Jackson Databind               |               | Apache-2.0                                 |
| Jackson Databind Nullable      |               | Apache-2.0                                 |
| Jakarta Persistence API        |               | EPL-2.0 / GPL-2.0-with-classpath-exception |
| JNATS                          |               | Apache-2.0                                 |
| Keycloak Admin REST Client     | 26.0.8        | Apache-2.0                                 |
| Liquibase                      |               | Apache-2.0                                 |
| Micrometer Prometheus Registry |               | Apache-2.0                                 |
| ModelMapper                    | 3.2.6         | Apache-2.0                                 |
| PostgreSQL JDBC Driver         |               | BSD-2-Clause                               |
| Project Lombok                 | 1.18.44       | MIT                                        |
| REST Assured                   | 6.0.0         | Apache-2.0                                 |
| Spring Boot                    | 4.0.5         | Apache-2.0                                 |
| Spring Cloud                   | 2025.1.0      | Apache-2.0                                 |
| Spring LDAP                    |               | Apache-2.0                                 |
| Spring Security                |               | Apache-2.0                                 |
| Spring Boot Admin              |               | Apache-2.0                                 |
| Spring Cloud Gateway           |               | Apache-2.0                                 |
| Spring Cloud Kubernetes        |               | Apache-2.0                                 |
| Spring WS Test                 | 5.0.1         | Apache-2.0                                 |
| SpringDoc OpenAPI              | 3.0.2         | Apache-2.0                                 |
| Testcontainers                 | 2.0.4         | MIT                                        |
| Testcontainers Keycloak        | 4.1.1         | MIT                                        |
| Thymeleaf                      | 3.1.3.RELEASE | Apache-2.0                                 |
| Thymeleaf Layout Dialect       | 4.0.1         | Apache-2.0                                 |

*The Java report can be generated automatically by BlackDuck.*

## Frontend (JavaScript / TypeScript)

| Component                     | Version | License                 |
|-------------------------------|---------|-------------------------|
| Angular                       | 21      | MIT                     |
| Angular CDK                   | 21      | MIT                     |
| Angular Material              | 21      | MIT                     |
| angular-auth-oidc-client      | 21      | MIT                     |
| Axios                         | 1.x     | MIT                     |
| ESLint                        | 10      | MIT                     |
| Font Awesome                  | 7       | MIT / CC-BY-4.0 (icons) |
| Jest                          | 30      | MIT                     |
| NgRx (Store, Effects, Router) | 21      | MIT                     |
| ngx-matomo-client             | 9       | MIT                     |
| ngx-pipes                     | 3.x     | MIT                     |
| pdfmake                       | 0.3.x   | MIT                     |
| PostCSS                       | 8.x     | MIT                     |
| PrimeNG                       | 21      | MIT                     |
| PrimeIcons                    | 7       | MIT                     |
| Quill                         | 2       | BSD-3-Clause            |
| RxJS                          | 7.x     | Apache-2.0              |
| Tailwind CSS                  | 4       | MIT                     |
| Transloco (@jsverse)          | 8.x     | MIT                     |
| TypeScript                    | 5.x     | Apache-2.0              |
| Vite                          | 8       | MIT                     |
| Zone.js                       | 0.16    | MIT                     |

## Infrastructure

| Component             | Version    | License                          |
|-----------------------|------------|----------------------------------|
| PostgreSQL            | 18         | PostgreSQL License (MIT-like)    |
| Keycloak              | 26.4.4     | Apache-2.0                       |
| Redis                 | latest     | BSD-3-Clause                     |
| NATS                  | alpine3.20 | Apache-2.0                       |
| CloudBeaver           |            | Apache-2.0                       |
| JupyterHub            | latest     | BSD-3-Clause                     |
| SFTPGo                | latest     | AGPL-3.0                         |
| Matomo                | latest     | GPL-3.0                          |
| MinIO                 | latest     | AGPL-3.0                         |
| Nginx                 | alpine     | BSD-2-Clause                     |
| Eclipse Temurin (JRE) | 21         | GPL-2.0-with-classpath-exception |

## Licence SPDX

| Licence                          | Link                                                       |
|----------------------------------|------------------------------------------------------------|
| AGPL-3.0                         | https://opensource.org/license/agpl-v3/                    |
| Apache-2.0                       | https://opensource.org/license/apache-2-0/                 |
| BSD-2-Clause                     | https://opensource.org/license/bsd-2-clause/               |
| BSD-3-Clause                     | https://opensource.org/license/bsd-3-clause/               |
| CC-BY-4.0                        | https://creativecommons.org/licenses/by/4.0/               |
| EPL-2.0                          | https://opensource.org/license/epl-2-0/                    |
| GPL-2.0-with-classpath-exception | https://opensource.org/license/gpl-2-0/                    |
| GPL-3.0                          | https://opensource.org/license/gpl-3-0/                    |
| LGPL-2.1+                        | https://www.gnu.org/licenses/old-licenses/lgpl-2.1.en.html |
| MIT                              | https://opensource.org/license/mit/                        |
| PostgreSQL License               | https://opensource.org/license/postgresql/                 |

