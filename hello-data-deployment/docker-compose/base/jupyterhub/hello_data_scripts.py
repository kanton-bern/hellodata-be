import os
import requests
import psycopg2
from requests.auth import HTTPBasicAuth

def connect():
    # Read environment variables
    api_url = os.getenv('API_URL')
    api_username = os.getenv('API_USERNAME')
    api_password = os.getenv('API_PASSWORD')

    if not api_url or not api_username or not api_password:
        print("Error: API_URL, API_USERNAME, and API_PASSWORD environment variables must be set.")
        return

    try:
        # Make the GET request with basic authentication
        response = requests.get(api_url, auth=HTTPBasicAuth(api_username, api_password))

        # Check if the request was successful
        if response.status_code == 200:
            # Extract the database connection parameters from the response
            db_params = response.json()

            # Establish a connection to the database
            connection = psycopg2.connect(
                user=db_params['username'],
                password=db_params['password'],
                host=db_params['host'],
                port=db_params['port'],
                database=db_params['databaseName']
            )

            return connection

        else:
            print(f"Failed to get a successful response. Status code: {response.status_code}")
            print(f"Response content: {response.text}")
            return None

    except requests.RequestException as e:
        print(f"An error occurred during the API request: {e}")
        return None

    except psycopg2.Error as e:
        print(f"An error occurred while connecting to the database: {e}")
        return None
