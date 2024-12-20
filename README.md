# HelloDATA BE: Open-Source Enterprise Data Platform

> The Open Enterprise Data Platform in a Single Portal

HelloDATA BE integrates the prowess of open-source tools into a unified, enterprise-grade data platform. It simplifies end-to-end data engineering by converging tools like dbt, Airflow, and Superset, anchored on a robust Postgres database. Our platform encapsulates the agility of open source with the dependability of enterprise solutions, streamlining your data workflow in one accessible portal.

<!-- <img src="docs/docs/images/portal-superset.jpg" style="width:800px;"/> -->
![](docs/docs/images/hello-data-superset.png)


## Vision and Goal

Agile and transparent data platforms are vital in a rapidly evolving digital landscape. HelloDATA BE tries to help by offering an open-source solution tailored to enterprise needs. Our mission is to democratize data and provide end-to-end- innovation-driven data handling. [Learn More](docs/docs/vision/vision-and-goal.md).


## Quick Start

**Essential Setup**
1. **Docker Desktop Settings**: For Mac users, adjust Docker Desktop settings for multi-platform support (especially for arm64-chip users). [Details](hello-data-deployment/docker-compose/README.md#mac).
2. **Hosts File Entry**: Ensure `127.0.0.1 host.docker.internal` is added to `/etc/hosts`. For Windows enable in Docker Desktop WSL settings [How to](hello-data-deployment/docker-compose/README.md#prepare-environment).

**Start-Up Instructions**

Change directory to `hello-data-deployment/docker-compose` and run:
```sh
docker-compose up -d
```

Pulling all images and starting up the containers will take a while. Once completed, access the web portal at [localhost:8080](http://localhost:8080) (default: admin/admin).

> **Note:**
> - Detailed start-up instructions, troubleshooting, and FAQs are in the [docker-compose README](hello-data-deployment/docker-compose/README.md).
> - Specific setup information for [Windows users](hello-data-deployment/docker-compose/README.md#windows).

## Key Features

HelloDATA BE is more than a platform; it's a holistic data solution:

- Unified enterprise data platform with a seamless open-source toolkit.
- Domain-centric architecture for effective data governance.
- Robust data modeling with lineage tracking.
- Real-time insights into runs and orchestrations.
- Company-wide dashboard creation and sharing.

## Architecture and Components

HelloDATA BE is built with modularity and extensibility architecture in mind, supported by [NATS](https://nats.io/), [Keycloak](https://www.keycloak.org/), and more, ensuring secure, efficient, and adaptable data handling. [Explore the Architecture](docs/docs/architecture/architecture.md).

### [Domain View](docs/docs/architecture/architecture.md): Business and Data Domains
The different views are a vital aspect of the HelloDATA BE. Adding multiple business domains with n data domains makes it enterprise-ready out of the box.

- **Business Domain**: Core services, including portal, orchestration, and monitoring.
- **Data Domain**: The heart of data storage, encompassing tables/schemas, dbt data models, data marts, DWH environments, and more.

Multiple Data Domains coexist in each business domain, each with dedicated storage and specialized data models. [Dive Deeper](docs/docs/architecture/architecture.md). Also, check the [User Manual](docs/docs/manuals/user-manual.md) for a detailed functional overview.

> **[Docs Website](https://kanton-bern.github.io/hellodata-be):** All the reference and developer documentation here on GitHub is also found as a website for more comfort.

## Contributing to HelloDATA BE

Join our development journey:

- [Developer Guidelines](CONTRIBUTING.md)
- HelloDATA BE Architecture
    - [Used Data Stack](docs/docs/architecture/data-stack.md)
    - [Architecture Overview](docs/docs/architecture/architecture.md)
    - [Infrastructure Insights](docs/docs/architecture/infrastructure.md)

## Development Status

Stay updated with our progress and plans on our [Roadmap](docs/docs/vision/roadmap.md), or get in contact with Discussion or PR/Issue.

## Contributors

- [Adrian Berger](https://github.com/adberger) (Developer)
- Andreea Hansel (Tester)
- [Dario Bagatto](https://github.com/bedag-bad) (Developer)
- [Lorin Reber](https://github.com/lreber) (Product-Owner, Value-Lead)
- [Micha Eichmann](https://github.com/michadavid) (Developer)
- [Michael Disteli](https://www.linkedin.com/in/michael-disteli-0044311b7/) (Product-Manager)
- [Nicolas Schmid](https://github.com/nschmid) (Initial developer & Architect)
- [Rajib Mitra](https://github.com/ramich) (Developer)
- [Simon Späti](https://github.com/sspaeti) (Developer)
- [Slawomir Wieczorek](https://github.com/wieczorslawo) (Developer)
- [Thomas Amlang](https://github.com/Thomas-Amlang) (Developer)
  
(in alphabetical order)

## License

HelloDATA BE is released under the [BSD 3-Clause License](LICENSE).
Copyright (c) 2024 Kanton Bern.

## Get in Touch

We recommend you start a discussion here on GitHub or create an issue. Alternatively, you can reach us at [info-hd-be@bedag.ch](mailto:info-hd-be@bedag.ch).


-----------------------------------------------------


# HelloDATA BE: Open-Source Enterprise Data Platform

> Die offene Enterprise-Datenplattform in einem einzigen Portal

HelloDATA BE integriert die Leistungsfähigkeit von Open-Source-Tools in eine einheitliche, unternehmensgerechte Datenplattform. Sie vereinfacht das End-to-End-Datenmanagement durch die Kombination von Tools wie dbt, Airflow und Superset, basierend auf einer robusten Postgres-Datenbank. Unsere Plattform verbindet die Flexibilität von Open Source mit der Zuverlässigkeit von Unternehmenslösungen und optimiert Ihren Datenworkflow in einem zugänglichen Portal.

<!-- <img src="docs/docs/images/portal-superset.jpg" style="width:800px;"/> -->
![](docs/docs/images/hello-data-superset.png)

## Vision und Ziel

Agile und transparente Datenplattformen sind in einer sich schnell entwickelnden digitalen Welt unerlässlich. HelloDATA BE bietet eine Open-Source-Lösung, die speziell auf die Anforderungen von Unternehmen zugeschnitten ist. Unsere Mission ist es, Daten zu demokratisieren und Innovation im Datenmanagement von Anfang bis Ende zu fördern. [Mehr erfahren](docs/docs/vision/vision-and-goal.md).

## Schnellstart

**Grundlegende Einrichtung**
1. **Einstellungen für Docker Desktop**: Mac-Benutzer sollten die Einstellungen von Docker Desktop für Multi-Plattform-Unterstützung anpassen (insbesondere für Benutzer mit einem Arm64-Chip). [Details](hello-data-deployment/docker-compose/README.md#mac).
2. **Hosts-Datei-Eintrag**: Fügen Sie sicher, dass `127.0.0.1 host.docker.internal` in `/etc/hosts` hinzugefügt wurde. Unter Windows muss dies in den Docker Desktop WSL-Einstellungen aktiviert werden. [Anleitung](hello-data-deployment/docker-compose/README.md#prepare-environment).

**Startanleitung**

Wechseln Sie in das Verzeichnis `hello-data-deployment/docker-compose` und führen Sie aus:
```sh
docker-compose up -d
```

Das Herunterladen aller Images und das Starten der Container kann einige Zeit in Anspruch nehmen. Nach Abschluss können Sie das Webportal unter [localhost:8080](http://localhost:8080) (Standard: admin/admin) aufrufen.

> **Hinweis:**
> - Ausführliche Startanleitungen, Fehlerbehebung und FAQs finden Sie in der [docker-compose README](hello-data-deployment/docker-compose/README.md).
> - Spezielle Einrichtungsinformationen für [Windows-Benutzer](hello-data-deployment/docker-compose/README.md#windows).

## Hauptfunktionen

HelloDATA BE ist mehr als nur eine Plattform; es ist eine umfassende Datenlösung:

- Einheitliche Enterprise-Datenplattform mit nahtlos integrierten Open-Source-Tools.
- Domänenspezifische Architektur für effektive Datenverwaltung.
- Leistungsfähige Datenmodellierung mit Verfolgung der Datenherkunft.
- Echtzeit-Einblicke in Ausführungen und Orchestrierungen.
- Erstellung und gemeinsame Nutzung von Dashboards für das gesamte Unternehmen.

## Architektur und Komponenten

HelloDATA BE basiert auf einer modularen und erweiterbaren Architektur, unterstützt durch [NATS](https://nats.io/), [Keycloak](https://www.keycloak.org/) und weitere Technologien, die eine sichere, effiziente und flexible Datenverarbeitung gewährleisten. [Entdecken Sie die Architektur](docs/docs/architecture/architecture.md).

### [Domänenansicht](docs/docs/architecture/architecture.md): Geschäfts- und Daten-Domänen
Die verschiedenen Ansichten sind ein zentraler Bestandteil von HelloDATA BE. Die Hinzufügung mehrerer Geschäftsdomänen mit n Daten-Domänen macht es von Haus aus unternehmensfähig.

- **Geschäftsdomäne**: Kernservices, einschließlich Portal, Orchestrierung und Überwachung.
- **Daten-Domäne**: Der Kern der Datenspeicherung, einschließlich Tabellen/Schemas, dbt-Datenmodellen, Data Marts, DWH-Umgebungen und mehr.

In jeder Geschäftsdomäne koexistieren mehrere Daten-Domänen, die jeweils über dedizierten Speicher und spezialisierte Datenmodelle verfügen. [Erfahren Sie mehr](docs/docs/architecture/architecture.md). Weitere Details finden Sie im [Benutzerhandbuch](docs/docs/manuals/user-manual.md).

> **[Dokumentationswebsite](https://kanton-bern.github.io/hellodata-be):** Alle Referenzen und Entwicklerdokumentationen, die hier auf GitHub zu finden sind, sind auch als Website für mehr Komfort verfügbar.

## Beitragen zu HelloDATA BE

Werden Sie Teil unserer Entwicklungsreise:

- [Entwickler-Richtlinien](CONTRIBUTING.md)
- HelloDATA BE Architektur
    - [Verwendeter Datenstack](docs/docs/architecture/data-stack.md)
    - [Architekturübersicht](docs/docs/architecture/architecture.md)
    - [Infrastruktur-Einblicke](docs/docs/architecture/infrastructure.md)

## Entwicklungsstatus

Bleiben Sie mit unseren Fortschritten und Plänen auf dem Laufenden, indem Sie unsere [Roadmap](docs/docs/vision/roadmap.md) verfolgen, oder nehmen Sie über Diskussionen oder PR/Issue Kontakt mit uns auf.

## Mitwirkende

- [Adrian Berger](https://github.com/adberger) (Entwickler)
- Andreea Hansel (Tester)
- [Dario Bagatto](https://github.com/bedag-bad) (Entwickler)
- [Lorin Reber](https://github.com/lreber) (Produktinhaber, Value-Lead)
- [Micha Eichmann](https://github.com/michadavid) (Entwickler)
- [Michael Disteli](https://www.linkedin.com/in/michael-disteli-0044311b7/) (Produktmanager)
- [Nicolas Schmid](https://github.com/nschmid) (Initialentwickler & Architekt)
- [Rajib Mitra](https://github.com/ramich) (Entwickler)
- [Simon Späti](https://github.com/sspaeti) (Entwickler)
- [Slawomir Wieczorek](https://github.com/wieczorslawo) (Entwickler)
- [Thomas Amlang](https://github.com/Thomas-Amlang) (Entwickler)

(in alphabetischer Reihenfolge)

## Lizenz

HelloDATA BE wird unter der [BSD 3-Clause Lizenz](LICENSE) veröffentlicht.
Copyright (c) 2024 Kanton Bern.

## Kontakt

Wir empfehlen, hier auf GitHub eine Diskussion zu starten oder ein Issue zu erstellen. Alternativ können Sie uns unter [info-hd-be@bedag.ch](mailto:info-hd-be@bedag.ch) erreichen.
