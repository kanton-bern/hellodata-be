# Data Publisher
With the Data Publisher, OGD data (Open Government Data) can be made available on the public Internet as CSV, XML, or JSON. This data can then be downloaded via a URL on a public website. 

## Benefits of Data Publisher

The Data Publisher can be used to create greater transparency and make public data easily available. This fulfills the growing need for Open Government Data (OGD). 

## Data Publisher architecture

The architecture of the Data Publisher is structured as follows: 

![](../images/Pasted%20image%2020240828142609.png)
 
- **Synchroniser**: The Synchroniser reads the data from the Datamart and converts it into a structured format (CSV, XLM, JSON). In a further step, this is stored on a filestorage.  
- **Filestorage**:  The filestorage serves as storage for various structured file formats. The filestorage is created within a Kubernetes cluster.  
- **Web server**:  The web server accesses the data stored on the filestorage. It also ensures that the data can be accessed via the https format and displayed/downloaded via a browser.  

## Data Publisher process 

The end-to-end process of the Data Publisher is as follows: 

![](../images/Pasted%20image%2020240828142731.png)
 

The blue boxes represent the current process that is already provided by HelloData BE with the help of DAGs. The orange boxes represent the tasks that are performed by the Data Publisher. 

1. **Read out data**: This process is done via Airflow and Python. 
2. **Transforming data**: Data transformation is carried out via DBT, and the data is integrated into a predefined target schema (e.g., UDM layer). 
3. **Extract data from the datamart**: All data is extracted from the defined target schema. 
4. **Transform data into a file forma**t: The data is transformed into one or more file formats (e.g. JSON, XML). 
5. **Save data to a public repository**: The data is stored on a publicly accessible repository on the Internet and a URL is provided for downloads. 


## Configure the data publisher

To configure the data publisher, the data owner must define the required tables and columns to be made available as OGD data. Once these have been defined, the data engineer or data analyst must configure the DAG so that the defined OGD data is written to a separate schema (usually the UDM layer). Once this has been set up, the Synchroniser can be configured as to which schema the tables should be exported from. This process with the corresponding responsibilities is shown in the illustration:  


![](../images/Pasted%20image%2020240828143526.png)

## Technical process

In the first step, the DAG is executed either manually or in a planned manner. This process takes place on the HelloDATA platform, whereby the OGD data is written to a defined target schema, usually the UDM layer.  

The Data Publisher process runs in parallel and is executed automatically at defined intervals. The timing of these queries can be customised by the administrator. In this step, the Data Publisher reads the data from the target schema, transforms it into one or more formats such as XML, JSON, or CSV, and saves it to the file store. The export formats can also be defined by the administrator.  

Using a web server, the data can then be made available on the file repository via HTTPS. This allows the end user to be provided with a URL via which they can download the data. 


![](../images/Pasted%20image%2020240828144917.png)

 