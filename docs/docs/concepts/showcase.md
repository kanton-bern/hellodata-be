# Showcase: Animal Statistics (Switzerland)

## What is the Showcase?
It's the demo cases of HD-BE, it's importing animal data from an external source and loading them with Airflow, modeled with dbt, and visualized in Superset.

It hopefully will show you how the platform works and it comes pre-installed with the [docker-compose](../../../hello-data-deployment/docker-compose/README.md) installation.

## How can I get started and explore it?
Click on the data-domain `showcase` and you can explore pre-defined dashboards with below described Airflow job and dbt models.

## How does it look?
Below the technical details of the showcase are described. How the airflow pipeline is collecting the data from an open API and modeling it with dbt.
### Airflow Pipeline

![](../images/Pasted%20image%2020231130144045.png)

- _**data_download**_  
    The source files, which are in CSV format, are queried via the data_download task and stored in the file system.  
- _**create_tables**_  
    Based on the CSV files, tables are created in the LZN database schema of the project.  
- _**insert_data**_  
    After the tables have been created, in this step, the source data from the CSV file is copied into the corresponding tables in the LZN database schema.  
- _**dbt_run**_  
    After the preceding steps have been executed and the data foundation for the DBT framework has been established, the data processing steps in the database can be initiated using DBT scripts. (described in the DBT section)  
- _**dbt_docs**_  
    Upon completion of generating the tables in the database, a documentation of the tables and their dependencies is generated using DBT.  
- _**dbt_docs_serve**_  
    For the visualization of the generated documentation, it is provided in the form of a website.


### DBT: Data modeling

#### fact_breeds_long 

The fact table fact_breeds_long describes key figures, which are used to derive the stock of registered, living animals, divided by breeds over time.

The following tables from the [tierstatistik_lzn] database schema are selected for the calculation of the key figure:

- cats_breeds
- cattle_breeds
- dogs_breeds
- equids_breeds
- goats_breeds
- sheep_breeds

![](../images/Pasted%20image%2020231130144109.png)  

#### fact_cattle_beefiness_fattissue 

The fact table fact_catle_beefiness_fattissue describes key figures, which are used to derive the number of slaughtered cows by year and month.  
Classification is done according to CH-TAX ([Trading Class Classification CHTAX System | VIEGUT AG](https://www.viegut.ch/de/marktinfo/chtax-tabellen/))

The following tables from the [tierstatistik_lzn] database schema are selected for the calculation of the key figure:

- cattle_evolbeefiness
- cattle_evolfattissue

![](../images/Pasted%20image%2020231130144118.png)

  

#### fact_cattle_popvariations 

The fact table fact_cattle_popvariations describes key figures, which are used to derive the increase and decrease of the cattle population in the Animal Traffic Database ([https://www.agate.ch/](https://www.agate.ch/)) over time (including reports from Liechtenstein).  
The key figures are grouped according to the following types of reports:

- Birth
- Slaughter
- Death  

The following table from the [tierstatistik_lzn] database schema is selected for the calculation of the key figure:

- cattle_popvariations

  
![](../images/Pasted%20image%2020231130144128.png)  

#### fact_cattle_pyr_wide & fact_cattle_pyr_long

The fact table fact_cattle_popvariations describes key figures, which are used to derive the distribution of registered living cattle by age class and gender.

The following table from the [tierstatistik_lzn] database schema is selected for the calculation of the key figure:

- cattle_pyr

The fact table fact_cattle_pyr_long pivots all key figures from fact_cattle_pyr_wide.

![](../images/Pasted%20image%2020231130144139.png)
  


### Superset

#### Database Connection

The data foundation of the Superset visualizations in the form of Datasets, Dashboards, and Charts is realized through a Database Connection.  

In this case, a database connection to a database is established, which refers to a PostgreSQL database in which the above-described DBT scripts were executed.

#### Datasets
Datasets are used to prepare the data foundation in a suitable form, which can then be visualized in charts in an appropriate way.

Essentially, modeled fact tables from the UDM database schema are selected and linked with dimension tables.

This allows facts to be calculated or evaluated at different levels of professional granularity.

![](../images/Pasted%20image%2020231130144155.png)
### Interfaces

# Tierstatistik


|Source|Description|
|---|---|
|[https://tierstatistik.identitas.ch/de/](https://tierstatistik.identitas.ch/de/)|Website of the API provider|
|[https://tierstatistik.identitas.ch/de/docs.html](https://tierstatistik.identitas.ch/de/docs.html)| Documentation of the platform and description of the data basis and API | 
|[tierstatistik.identitas.ch/tierstatistik.rdf](https://tierstatistik.identitas.ch/tierstatistik.rdf)|API and data provided by the website| 
