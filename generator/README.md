# Basic Synthetic Patient Data Generator
A synthetic patient population can be generated using this built-in generator module, as an alternative to generating patient data using Synthea. 
This option supports only a small number of scenarios (see below) but is very performant.

## Generating Patient Data
From the root of the project the following command will generate about 1.2 million patients, it takes about one minute.
```bash
java -Xms3g -jar generator/target/generator*.jar
``` 
Command options:
- `--population-size` Optional. Defaults to 1,248,322.

The generated population will be written in native bulk NDJSON format to a directory named `patient-data-for-import`.

## Synthesised Patient Scenarios
In the "concepts used" section "<<" shows that a concept was picked randomly from a set including the specified concept and all its descendants.

### Demographic:
- All patients are over the age of 30 and under the age of 85.
- Male/Female split is 50/50. 

### Rheumatoid Arthritis, COPD
- 12% of patients have COPD
    - No medication prescribed
        - After 1-6 months 2% have Bacterial pneumonia
- 6% of patients have RA
    - 50% of these are given an Anti-TNF agent
        - After 1-6 months 4% have Bacterial pneumonia
    - Remaining 50% - No medication prescribed
        - After 1-6 months 0.5% have Bacterial pneumonia
- 0.12% of patients have both RA and COPD
    - 10% of these are given an Anti-TNF agent
        - After 1-6 months 10% have Bacterial pneumonia
    - Remaining 90% - No medication prescribed
        - After 1-6 months 2% have Bacterial pneumonia
- Concepts used:
  - << 69896004 |Rheumatoid arthritis (disorder)|
  - << 13645005 |Chronic obstructive lung disease (disorder)|
  - << 416897008 |Product containing tumor necrosis factor alpha inhibitor (product)|
  - << 53084003 |Bacterial pneumonia (disorder)|

### Afib, Peptic Ucler
- Patients have either Afib or Peptic Ucler or both
  - Some are prescribed an Antiplatelet agent
    - Increased risk of CVA
    - Possible UGIB
- Concepts used:
  - Afib: << 49436004 |Atrial fibrillation (disorder)|
  - Peptic Ulcer: << 13200003 |Peptic ulcer (disorder)|
  - Antiplatelet Agent: << 108972005 |Medicinal product acting as antiplatelet agent (product)|
  - CVA: 230690007 |Cerebrovascular accident (disorder)|
  - UGIB: 37372002 |Upper gastrointestinal hemorrhage (disorder)|

### Pulm Embolus, GI Ulcer
- Patients have either Pulm Embolus or GI Ulcer or both
  - Some are prescribed an AntiCoagulant agent
    - Affects risk of GI bleed
- Concepts used:
  - Pulm Embolus: << 233935004 |Pulmonary thromboembolism (disorder)|
  - GI Ulcer: << 40845000 |Gastrointestinal ulcer (disorder)|
  - AntiCoag agent: << 48603004 |Product containing warfarin (medicinal product)|
  - GI bleed: << 74474003 |Gastrointestinal hemorrhage (disorder)|

### BRCA1 gene
- 0.2% female patients have BRCA1 gene
  - Some are prescribed a tamoxifen drug
    - Affects stats with Breast cancer
- Concepts used:
  - BRCA1 gene: 412734009 |BRCA1 gene mutation positive (finding)|
  - Tamoxifen drug: 75959001 |Product containing tamoxifen (medicinal product)|
  - Breast cancer: 254837009 |Malignant neoplasm of breast (disorder)|

### Diabetics, Smoking
- Patients have type 2 diabetes
  - Some smoke
    - Affects stats on foot amputation
- Concepts used:
  - type 2 diabetes: << 44054006 |Diabetes mellitus type 2 (disorder)|
  - Smoker: << 77176002 |Smoker (finding)|
  - Foot amputation: << 180030006 |Amputation of the foot (procedure)|

### Myo, Lymph
- Patients have B-cell lymphoma or Myocardial disease or both
  - Some are prescribed anthracycline
    - Affects stats of CHF
- Concepts used:
  - << 109979007 |B-cell lymphoma (disorder)|
  - << 57809008 |Myocardial disease (disorder)|
  - << 108787006 |Medicinal product containing anthracycline and acting as antineoplastic agent (product)|
  - << 42343007 |Congestive heart failure (disorder)|

### Breast Cancer
- 9.7% of Female patients between 60 and 79
- 53% are group A, the rest group B
- Group A have mammography screening once a year
  - Chance of abnormal screening 6.5%
    - Chance of abnormal diagnostic 49.2%
      - Chance of positive biopsy 17%
        - Chance of stage 1 42%
        - Chance of stage 2 36%
        - Otherwise DCIS
- Group B have mammography screening once every two years
  - Chance of abnormal screening 6.5%
    - Chance of abnormal diagnostic 36.9%
      - Chance of positive biopsy 22%
        - Chance of stage 1 39%
        - Chance of stage 2 42%
        - Otherwise DCIS
- Concepts used
  - Screening: 384151000119104 | Screening mammography of bilateral breasts (procedure) |
  - Abnormal screening: << 171176006 | Breast neoplasm screening abnormal (finding) |
  - ... leads to diagnostic: 566571000119105 | Mammography of right breast (procedure) |
  - Abnormal diagnostic: << 274530001 | Abnormal findings on diagnostic imaging of breast (finding) |
  - ... leads to biopsy: 122548005 | Biopsy of breast (procedure) |
  - Positive biopsy: 165325009 | Biopsy result abnormal (finding) |
  - Stage 1: 422399001 | Infiltrating ductal carcinoma of breast, stage 1 (finding) |
  - Stage 2: 422479008 | Infiltrating ductal carcinoma of breast, stage 2 (finding) |
  - DCIS: 397201007 | Microcalcifications present in ductal carcinoma in situ (finding) |
