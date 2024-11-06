#custom makefile :)
pull:
	docker-compose pull

start-all:
	docker-compose up -d --build

start-infra:
	docker-compose up -d --build postgres keycloak redis nats nats-box cloudbeaver hello-data-cloudbeaver-gateway hello-data-dbt-docs hello-data-dbt-docs-sidecar

start-cloudbeaver: start-infra
	docker-compose up -d --build  cloudbeaver hello-data-cloudbeaver-gateway hello-data-sidecar-cloudbeaver

start-dbt-docs: start-infra
	docker-compose up -d --build hello-data-dbt-docs hello-data-dbt-docs-sidecar

start-airflow: start-infra
	docker-compose up -d --build airflow-init flower  hello-data-airflow-sidecar airflow-webserver airflow-scheduler airflow-worker airflow-triggerer

start-portal:
	docker-compose up -d --build hello-data-portal-ui hello-data-portal-api hello-data-portal-sidecar

start-default-dd:
	docker-compose up -d --build showcase-files-default-data-domain superset-app-default-data-domain superset-init-default-data-domain superset-worker-default-data-domain superset-worker-beat-default-data-domain hello-data-superset-sidecar-default-data-domain

start-airflow:
	docker-compose up -d --build airflow-webserver airflow-scheduler airflow-worker airflow-triggerer airflow-init flower hello-data-airflow-sidecar

start-jupyterhub: start-portal
	docker-compose up -d --build hello-data-sidecar-jupyterhub-default-data-domain hello-data-jupyterhub-gateway-default-data-domain jupyterhub-notebook-default-data-domain jupyterhub-proxy-default-data-domain jupyterhub-default-data-domain

stop-all:
	docker-compose down --volumes --remove-orphans

#start components for local FE or BE implementation:
start-local-impl: start-portal start-dbt-docs start-cloudbeaver start-default-dd start-airflow

svc ?=

#make start svc=keycloak
start:
	docker-compose up -d $(svc)

stop:
	docker-compose stop $(svc)