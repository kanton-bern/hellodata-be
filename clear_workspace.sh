!#/bin/bash

echo "Clearing workspace..."

docker rmi -f $(docker images -q)

echo "All images removed."

rm -rf