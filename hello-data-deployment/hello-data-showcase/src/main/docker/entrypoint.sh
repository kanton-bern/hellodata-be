#!/bin/sh

# Check if the destination path is set
if [ -z "$DESTINATION_PATH" ]; then
    echo "Error: DESTINATION_PATH environment variable is not set."
    exit 1
fi

mkdir -p "$DESTINATION_PATH/files"
mkdir -p "$DESTINATION_PATH/dbt-docs"

cp -R /showcase-src/* "$DESTINATION_PATH"

rm -rf /showcase-src

chown -R "${AIRFLOW_UID}:0" "$DESTINATION_PATH"

sed -i "s/DD_KEY/$DD_KEY/g" "$DESTINATION_PATH/dags/tierstatistik_elt_dag.py"
sed -i "s/SHOWCASE_DWH_HOST/$SHOWCASE_DWH_HOST/g" "$DESTINATION_PATH/dbt/profiles.yml"
sed -i "s/SHOWCASE_DWH_PORT/$SHOWCASE_DWH_PORT/g" "$DESTINATION_PATH/dbt/profiles.yml"
sed -i "s/SHOWCASE_DWH_U_MODELER_PASSWORD/$SHOWCASE_DWH_U_MODELER_PASSWORD/g" "$DESTINATION_PATH/dbt/profiles.yml"
sed -i "s/SHOWCASE_DWH_U_MODELER/$SHOWCASE_DWH_U_MODELER/g" "$DESTINATION_PATH/dbt/profiles.yml"
sed -i "s/SHOWCASE_DWH_NAME/$SHOWCASE_DWH_NAME/g" "$DESTINATION_PATH/dbt/profiles.yml"

# Run an infinite loop to keep the container running
while true; do
  sleep 3600 # Sleep for an hour, adjust as needed
done
