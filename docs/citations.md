# How citations are generated

In order to offer a consistent citations, all citations are auto-generated for all datasets.
 * Based on discussion from [Issue #4](https://github.com/gbif/registry/issues/4).
 * Source Code: [CitationGenerator.java](https://github.com/gbif/registry/blob/master/registry-metadata/src/main/java/org/gbif/registry/metadata/CitationGenerator.java)

## General Formula
```
{dataset.authors} ({dataset.pubDate})
{dataset.title}. [Version {dataset.version}]. {organization.title}.
{dataset.type} Dataset {dataset.doi}, accessed via GBIF.org on {YYYY-MM-DD}.
```

Example:

```
Smirnov I, Golikov A, Khalikov R (2017).
Ophiuroidea collections of the Zoological Institute Russian Academy of Sciences. Version 1.32.
Zoological Institute, Russian Academy of Sciences, St. Petersburg.
Occurrence Dataset https://doi.org/10.15468/ej3i4f accessed via GBIF.org on 2017-08-21.
```

### Author List Generation

From the dataset contacts, in order, take all (unique person based on the first and last name)
`ContactType.ORIGINATOR` and `ContactType.METADATA_AUTHOR`.

For each contacts, use the form `lastName` and first letter of `firstNames` (e.g. Smirnov I).
In case a contact doesn't represent a person (absence of first or last name), the name of the organization that is publishing
the dataset will be used.