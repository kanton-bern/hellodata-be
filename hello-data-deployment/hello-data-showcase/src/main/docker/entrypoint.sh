#!/bin/sh

# Check if the destination path is set
if [ -z "$DESTINATION_PATH" ]; then
    echo "Error: DESTINATION_PATH environment variable is not set."
    exit 1
fi

if [ -z "$AIRFLOW_UID" ]; then
    echo "Error: AIRFLOW_UID is not set."
    exit 1
fi

if [ -z "$CONNECTION_ID" ]; then
    echo "Error: CONNECTION_ID is not set."
    exit 1
fi

mkdir -p "$DESTINATION_PATH/files"
mkdir -p "$DESTINATION_PATH/dbt-docs"

cp -R /showcase-src/* "$DESTINATION_PATH"

rm -rf /showcase-src

chmod 777 -R "$DESTINATION_PATH"

sed -i "s/SHOWCASE_DWH_HOST/$SHOWCASE_DWH_HOST/g" "$DESTINATION_PATH/dbt/profiles.yml"
sed -i "s/SHOWCASE_DWH_PORT/$SHOWCASE_DWH_PORT/g" "$DESTINATION_PATH/dbt/profiles.yml"
sed -i "s/DWH_USER_MODELER_PASSWORD/$SHOWCASE_DWH_U_MODELER_PASSWORD/g" "$DESTINATION_PATH/dbt/profiles.yml"
sed -i "s/DWH_MODELER_USER/$SHOWCASE_DWH_U_MODELER/g" "$DESTINATION_PATH/dbt/profiles.yml"
sed -i "s/SHOWCASE_DWH_NAME/$SHOWCASE_DWH_NAME/g" "$DESTINATION_PATH/dbt/profiles.yml"
sed -i "s/CONNECTION_ID/$CONNECTION_ID/g" "$DESTINATION_PATH/dags/tierstatistik_elt_dag.py"
sed -i "s/CONNECTION_ID/$CONNECTION_ID/g" "$DESTINATION_PATH/dags/install_common_data.py"

dags_path="$DESTINATION_PATH/dags/*"
for file in $dags_path; do
    echo "Processing $file"
    sed -i "s/DD_KEY/$DD_KEY/g" "$file"
done

chmod 777 -R "$DESTINATION_PATH"

# Run an infinite loop to keep the container running
while true; do
  sleep 3600 # Sleep for an hour, adjust as needed
done
