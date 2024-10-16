# Welcome to HelloDATA BE 👋🏻

This is the open documentation about HelloDATA BE. We hope you enjoy it.

!!! info "Contribute"

    In case something is missing or you'd like to add something, below is how you can contribute:

    - [Star our GitHub ⭐](https://github.com/kanton-bern/hellodata-be)
    - Want to discuss, contribute, or need help, create a [GitHub Issue](https://github.com/kanton-bern/hellodata-be/issues), create a [Pull Request](https://github.com/kanton-bern/hellodata-be/pulls) or open a [Discussion](https://github.com/kanton-bern/hellodata-be/discussions/).

## What is HelloDATA BE?

HelloDATA BE is an **enterprise data platform** built on top of open-source tools based on the modern data stack. We use state-of-the-art tools such as dbt for data modeling with SQL and Airflow to run and orchestrate tasks and use Superset to visualize the BI dashboards. The underlying database is Postgres.

Each of these components is carefully chosen and additional tools can be added in a later stage.

## Why do you need an **_Open Enterprise Data Platform_** (HelloDATA BE)?

These days the amount of data grows yearly more than the entire lifetime before. Each fridge, light bulb, or anything really starts to produce data. Meaning there is a growing need to make sense of more data. Usually, not all data is necessary and valid, but due to the nature of growing data, we must be able to collect and store them easily. There is a great need to be able to analyze this data. The result can be used for secondary usage and thus create added value.

That is what this open data enterprise platform is all about. In the old days, you used to have one single solution provided; think of SAP or Oracle. These days that has completely changed. New SaaS products are created daily, specializing in a tiny little niche. There are also many open-source tools to use and get going with minutes freely.

So why would you need a HelloDATA BE? It's simple. You want the best of both worlds. You want **open source** to not be locked-in, to use the strongest, collaboratively created product in the open. People worldwide can fix a security bug in minutes, or you can even go into the source code (as it's available for everyone) and fix it yourself—compared to an extensive vendor where you solely rely on their update cycle.

But let's be honest for a second if we use the latest shiny thing from open source. There are a lot of bugs, missing features, and independent tools. That's precisely where HelloDATA BE comes into play. We are building the **missing platform** that combines the best-of-breed open-source technologies into a **single portal**, making it enterprise-ready by adding features you typically won't get in an open-source product. Or we fix bugs that were encountered during our extensive tests.

Sounds too good to be true? Give it a try. Do you want to knot the best thing? It's open-source as well. Check out our [GitHub HelloDATA BE](https://github.com/kanton-bern/hellodata-be).


## Quick Start for Developers

Want to run HelloDATA BE and test it locally? Run the following command in the docker-compose directory to deploy all components:

```sh
cd hello-data-deployment/docker-compose
docker-compose up -d
```

**Note:** Please refer to our [docker-compose README](https://github.com/kanton-bern/hellodata-be/tree/main/hello-data-deployment/docker-compose/README.md) for more information; there are some must presets you need to configure.
---
