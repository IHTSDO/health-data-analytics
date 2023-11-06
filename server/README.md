# Snolytical Server

## Technical Notes
This module provides the data analytics server API. The server jar file also hosts the user interface which is packaged into the jar during the build process.

### Terminology Server Requirements
#### FHIR Terminology Server
The majority of features in Snolytical can be supported by any FHIR Terminology Server that features [subset expansion using ECL](http://hl7.org/fhir/R4/snomedct.html#implicit).
The FHIR Terminology Server used can be configured under `fhir-terminology-server-url` in the configuration. 

#### Subset Builder
The subset building feature requires the [Snowstorm terminology server](https://github.com/IHTSDO/snowstorm). 
This is because Snowstorm has good support for building [ECL expressions](http://snomed.org/ecl) that adhere to the [MRCM rules](https://snomed.org/mrcm), this is not included in FHIR.

There is a Snowstorm proxy where requests from the frontend to `/api/snowstorm` will be directed to the Snowstorm instance configured under `snowstorm.url`.

If you require the subset builder Snowstorm can of course be used to provide the MRCM and FHIR support. 
