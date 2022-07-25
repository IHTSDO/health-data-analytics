# Utilities - SNOMED CT Health Data Analytics

## Clustering / Feature Reduction Utility
This utility can help prepare SNOMED CT encoded data for machine learning by suggesting a smaller set of concepts to use as features to avoid overfitting. 
The utility maps encounter frequencies onto the SNOMED CT hierarchy and summarises low-encounter concepts using ancestor concepts.

The algorithm works bottom-up. Concepts that meet the specified minimum encounter frequency will immediately qualify as a cluster. The algorithm will attempt to group concepts 
with insufficient frequency while walking up the hierarchy using a common ancestor. If there is not enough encounter frequency among the remaining concepts they will not make 
it into a cluster at all.

### Output

The main output of the utility is a map of encounter concept ids to the surviving cluster concept ids (`encounter_to_cluster_map.txt`). 
This can be used in the preprocessing of the encounter data to reduce the number of encounter features before running machine learning. 
Encounters may map to multiple overlapping clusters, this behaviour can be suppressed using the `-exclude-ancestor-clusters` parameter.

Other output files:
- `clusters.txt` Contains details of each cluster with the following columns:
  - `clusterConceptId`: The concept identifier.
  - `FSN`: The concept fully specified name description term.
  - `remainingAggregateFrequency`: Total frequency of this and descendant encounters, excluding those within other clusters.
  - `totalAggregateFrequency`: Total frequency of this and descendant encounters, including those within other clusters.
  - `includesWeakEncounters`: Any descendant concepts that on their own have insufficient frequency so are part of this cluster.
  - `includesSubClusters`: Any descendant concepts that are also features.
- `encounter_cluster_inclusion.txt` Contains details of each encounter concept with the following columns:
  - `encounterConceptId`: The concept identifier.
  - `sufficientFrequency`: Flag to indicate if the encounter had sufficient frequency to create a cluster.
  - `clusterInclusionCount`: The number of clusters this concept was included in.

### Build
Use maven on the command line to build the jar file:
```
cd utilities
mvn clean package
```
The jar is output in the target directory.

### Run
Run the jar on the command line using the java command:
```
java -jar target/utilities-*with-dependencies.jar -term-to-concept-map barts-core-problem-list-map.txt -encounter-frequency-file CriteriaFrequency-problems.txt -force-clusters force-cluster.txt -relationship-file SnomedCT_InternationalRF2_PRODUCTION_20210731T120000Z/Snapshot/Terminology/sct2_Relationship_Snapshot_INT_20210731.txt -description-file SnomedCT_InternationalRF2_PRODUCTION_20210731T120000Z/Snapshot/Terminology/sct2_Description_Snapshot-en_INT_20210731.txt -min-encounter-frequency 200 -exclude-ancestor-features
```
#### Required arguments
- All input and output files use tab separated format.
- `-term-to-concept-map <filename>`
  - TSV file with map of terms and concept ids.
  - Headers: Term, ConceptId
- `-encounter-frequency-file <filename`
  - TSV file with map of terms and frequencies.
  - Headers: Term, Frequency
- `-min-encounter-frequency <number>`
    - The minimum number of encounters a concept must have to be used as a cluster. 
- `-relationship-file <filename`
  - Path to SNOMED CT Relationships file
  - example: sct2_Relationship_Snapshot_INT_20210731.txt
- `-description-file <filename>`
  - Path to SNOMED CT Descriptions file
  - example: sct2_Description_Snapshot-en_INT_20210731.txt
#### Optional arguments
- `-force-clusters <filename>`
  - TSV file with list of concepts that must be clusters.
- `-exclude-ancestor-clusters`
  - Stop clusters including ancestor clusters in the final encounter to cluster map.
  - Although this nesting is semantically correct suppressing nesting in the output may be desirable to stop feature overlap.
