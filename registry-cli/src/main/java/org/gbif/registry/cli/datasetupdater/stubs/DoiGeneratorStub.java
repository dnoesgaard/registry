package org.gbif.registry.cli.datasetupdater.stubs;

import org.gbif.api.model.common.DOI;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.service.InvalidMetadataException;
import org.gbif.registry.doi.generator.DoiGenerator;

import java.util.UUID;

/**
 * Stub class used to simplify Guice binding, e.g. when this class must be bound but isn't used by CLI.
 */
public class DoiGeneratorStub implements DoiGenerator {

  @Override
  public DOI newDatasetDOI() {
    return null;
  }

  @Override
  public DOI newDownloadDOI() {
    return null;
  }

  @Override
  public boolean isGbif(DOI doi) {
    return false;
  }

  @Override
  public void failed(DOI doi, InvalidMetadataException e) {

  }

  @Override
  public void registerDataset(DOI doi, DataCiteMetadata metadata, UUID datasetKey) throws InvalidMetadataException {

  }

  @Override
  public void registerDownload(DOI doi, DataCiteMetadata metadata, String downloadKey) throws InvalidMetadataException {

  }

  @Override
  public void delete(DOI doi) {

  }
}