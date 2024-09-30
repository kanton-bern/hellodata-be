#custom makefile :)
pull:
	docker-compose pull

start-all:
	docker-compose up -d --build

start-infra:
	docker-compose up postgres keycloak redis nats nats-box cloudbeaver hello-data-cloudbeaver-gateway hello-data-dbt-docs hello-data-dbt-docs-sidecar -d --build

start-cloudbeaver: start-infra
	docker-compose up cloudbeaver hello-data-cloudbeaver-gateway hello-data-sidecar-cloudbeaver -d --build

start-dbt-docs: start-infra
	docker-compose up hello-data-dbt-docs hello-data-dbt-docs-sidecar -d --build

start-airflow: start-infra
	docker-compose up airflow-init flower  hello-data-airflow-sidecar airflow-webserver airflow-scheduler airflow-worker airflow-triggerer -d --build

start-portal:
	docker-compose up hello-data-portal-ui hello-data-portal-api hello-data-portal-sidecar -d --build

start-default-dd:
	docker-compose up superset-app-default-data-domain superset-init-default-data-domain superset-worker-default-data-domain superset-worker-beat-default-data-domain hello-data-superset-sidecar-default-data-domain -d --build

start-airflow:
	docker-compose up airflow-webserver airflow-scheduler airflow-worker airflow-triggerer airflow-init flower hello-data-airflow-sidecar -d

stop:
	docker-compose down --volumes --remove-orphans