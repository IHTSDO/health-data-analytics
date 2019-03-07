# Health Data Analytics Demonstrator

## Capabilities
- Value set generator that works with SNOMED subsumption with inclusion, exclusion and attribute constraints.
- Data analysis tool which finds cohorts of patients with given conditions, procedures and/or prescriptions.
- Statistical testing to measure how a procedure or drug affects the chances of a subsequent disorder.
- Demo data generator which creates a large population of patients including disease associations and conditions.

## Background and strategic fit
SNOMED CT’s distinguishing feature is its logical framework.  Unlike ICD terms which are not based on Description Logic, SNOMED features the ability to do logical subsumption searches to find cohorts of patients, medications, or conditions.

Every kind of quality measure, outcomes measurement or retrospective research involving cohorts of patients depends on defining lists of clinical terms sometimes called “value sets”.

When these types of data analysis are done with ICD or other non-SNOMED terminologies, the creation of value sets requires human experts and is prone to errors. The humans creating the value sets must remember all the possible ways to refer to a condition, and ICD does not allow more than one parent, so Viral Pneumonia much be either a respiratory disease or an infectious disease but can’t be both.

In SNOMED because of its logical structure, you can search hierarchies and do logical role based searches. This allows you to create value sets without arbitrary knowledge of which synonyms are used to describe a term.



These data analysis tasks require three steps that we can define as a High Value Implementation of SNOMED.

First an organization must be able to create value sets by using SNOMED subsumption searches.  This task is independent of any patient data.  If the patient data is linked to SNOMED directly, then the cohort of patients can be found without any further processing.  But if the EMR uses a non-SNOMED interface terminology, then there must be an intermediate step mapping the SNOMED value set to the interface terms.  For the purposes of our demo tool, we will use SNOMED terms directly linked to patient data.



These principles are difficult to explain to clinicians, in order to demonstrate these advantages to them,  it is necessary to have a tool that is populated with SNOMED coded patient data.  With such a tool we can highlight the advantages of SNOMED compared to ICD (or any other terminology).

Real patient data (a large population linked to SNOMED) is practically impossible to get for a variety of reasons.  Real data would contain expected associations between diseases, events, medications and conditions. For example, real data would show that fractured bones were more common after a motor vehicle accident.  Real data would show that infections were more common after an immunosuppressive medication were given.  Real data would also show these and other known links between conditions. Other obvious examples are patients with COPD would have pneumonia and bronchitis more often than patients in general.  Diabetics would have more foot ulcers and more peripheral neuropathy.

## Summary
This demo tool is a real tool. If we were to get some real patient data and stream it into the tool then we could explore for real associations.

## Technical Information

A frontend web application for this API is available [here](https://github.com/IHTSDO/health-data-analytics-frontend).

### Data Store
A standalone Elasticsearch instance is required to hold the patient data. 
There is also a Lucene index within the application to hold a SNOMED CT Edition to support subsumption testing.

### Data Model
The data points are:
- Patient
  - roleId
  - dob
  - dobYear (for optimisation)
  - gender (MALE / FEMALE)
  - encounters
- ClinicalEncounter
  - date
  - conceptId
  - type (FINDING / MEDICATION / PROCEDURE)

### Local API Setup
#### Prerequisites
- Java 8
- Min 3G of memory for the application
- Min 2G of memory for Elasticsearch
- A SNOMED CT RF2 archive

#### Setup
1. Create a folder for this tool.
2. Add the application .jar file by downloading the [latest release](https://github.com/IHTSDO/health-data-analytics/releases/latest).
  1. Alternatively build your own jar file using maven ```mvn clean package```.
3. Unzip the SNOMED RF2 archive into a sub-folder called 'release'.
4. Download and run [Elasticsearch 6.0.1](https://www.elastic.co/downloads/past-releases/elasticsearch-6-0-1).

Now you are ready to run the tool to import SNOMED CT into Lucene and to generate a demonstration population. In the terminal go to the folder for the application and run the following:  
```java -Xmx3g -jar health-data-analytics*.jar --generate-population=10000```

The application will first import SNOMED CT from the release folder. Then it will generate the patients.
The '--generate-population' argument tells the application how many patients to generate. It takes about 45 seconds per 10K patients on a 2.2 GHz Intel Core i7.

The SNOMED data is imported into Lucene indices in the 'data' folder. 
The patient data is stored in Elasticsearch.

The next time you run the application remove the '--generate-population' unless you want to generate a new set.

### Load your own data
You can stream your own patient data into this application by writing an implementation of HealthDataIngestionSource.

We would love to here from you if you are thinking of using this project to load your own data. [Get in touch](mailto:techsupport@snomed.org).
