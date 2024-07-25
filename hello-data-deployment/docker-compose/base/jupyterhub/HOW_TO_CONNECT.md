## How to connect to the database:

```
from hello_data_scripts import connect # import the function
connection = connect() # use function, it fetches the temp user creds and establishes the connection
```

### Example:

```
import psycopg2
from hello_data_scripts import connect  

# Get the database connection
connection = connect()

if connection is None:
    print("Failed to connect to the database.")
    sys.exit(1)

try:
    # Create a cursor object
    cursor = connection.cursor()
    
    # Example query to check the connection
    cursor.execute("SELECT version();")
    db_version = cursor.fetchone()
    print(f"Connected to database. PostgreSQL version: {db_version}")
    
except psycopg2.Error as e:
    print(f"An error occurred while performing database operations: {e}")
    
finally:
    # Close the cursor and connection
    cursor.close()
    connection.close()
    print("Database connection closed.")
```
