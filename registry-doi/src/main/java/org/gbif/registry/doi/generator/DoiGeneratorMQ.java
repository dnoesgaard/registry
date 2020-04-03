package org.gbif.registry.doi.generator;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.api.model.common.DoiStatus;
import org.gbif.common.messaging.api.Message;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.ChangeDoiMessage;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.service.InvalidMetadataException;
import org.gbif.doi.service.datacite.DataCiteValidator;
import org.gbif.registry.doi.DoiPersistenceService;
import org.gbif.registry.doi.DoiType;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import javax.annotation.Nullable;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.ibatis.exceptions.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Registry internal service that guarantees to issue unique new DOIs and deals with scheduling
 * DOI metadata updates and registration via RabbitMQ.
 */
public class DoiGeneratorMQ implements DoiGenerator {
  private static final Logger LOG = LoggerFactory.getLogger(DoiGeneratorMQ.class);

  private final DoiPersistenceService doiPersistenceService;
  private final URI datasetTarget;
  private final URI downloadTarget;
  private final URI dataPackageTarget;
  private final String prefix;
  private static final int RANDOM_LENGTH = 6;
  private static final String DOI_CHARACTERS = "23456789abcdefghjkmnpqrstuvwxyz"; // Exclude 0o 1il

  /**
   * The messagePublisher can be optional, and optional is not supported in constructor injection.
   */
  @Inject(optional = true)
  private final MessagePublisher messagePublisher = null;

  @Inject
  public DoiGeneratorMQ(DoiPersistenceService doiPersistenceService, @Named("portal.url") URI portal,
                        @Named("doi.prefix") String prefix) {
    checkArgument(prefix.startsWith("10."), "DOI prefix must begin with 10.");
    this.prefix = prefix;
    this.doiPersistenceService = doiPersistenceService;
    checkNotNull(portal, "portal base URL can't be null");
    checkArgument(portal.isAbsolute(), "portal base URL must be absolute");
    datasetTarget = portal.resolve("dataset/");
    downloadTarget = portal.resolve("occurrence/download/");
    dataPackageTarget = portal.resolve("data_package/");
  }

  private DOI newDOI(final String shoulder, DoiType type) {
    // only try for hundred times then fail
    for (int x=0; x<100; x++) {
      DOI doi = random(shoulder);
      try {
        doiPersistenceService.create(doi, type);
        return doi;
      } catch (PersistenceException e) {
        // might have hit a unique constraint, try another doi
        LOG.debug("Random {} DOI {} existed. Try another one", type, doi, e.getMessage());
      }
    }
    throw new IllegalStateException("Tried 100 random DOIs and none worked, Giving up");
  }

  @Override
  public DOI newDatasetDOI() {
    return newDOI("", DoiType.DATASET);
  }

  @Override
  public DOI newDownloadDOI() {
    return newDOI("dl.", DoiType.DOWNLOAD);
  }

  @Override
  public DOI newDataPackageDOI() {
    return newDOI("dp.", DoiType.DATA_PACKAGE);
  }

  @Override
  public boolean isGbif(DOI doi) {
    return doi != null && doi.getPrefix().equalsIgnoreCase(prefix);
  }

  @Override
  public void failed(DOI doi, InvalidMetadataException e) {
    // Updates the doi table to FAILED status and uses the error stacktrace as the xml for debugging.
    doiPersistenceService.update(doi, new DoiData(DoiStatus.FAILED, null), ExceptionUtils.getStackTrace(e));
  }

  /**
   * @return a random DOI with the given prefix. It is not guaranteed to be unique and might exist already
   */
  private DOI random(@Nullable String shoulder) {
    String suffix = Strings.nullToEmpty(shoulder) + RandomStringUtils.random(RANDOM_LENGTH, DOI_CHARACTERS);
    return new DOI(prefix, suffix);
  }

  @Override
  public void registerDataset(DOI doi, DataCiteMetadata metadata, UUID datasetKey) throws InvalidMetadataException {
    checkNotNull(doi, "DOI required");
    checkNotNull(datasetKey, "Dataset key required");
    checkNotNull(messagePublisher,"No message publisher configured to send DoiChangeMessage");

    String xml = DataCiteValidator.toXml(doi, metadata);
    Message message = new ChangeDoiMessage(DoiStatus.REGISTERED, doi, xml, datasetTarget.resolve(datasetKey.toString()));

    try {
      messagePublisher.send(message, true);
    } catch (IOException e) {
      LOG.error("Failed sending DoiChangeMessage for {} and dataset {}", doi, datasetKey, e);
    }

  }

  @Override
  public void registerDownload(DOI doi, DataCiteMetadata metadata, String downloadKey) throws InvalidMetadataException {
    checkNotNull(doi, "DOI required");
    checkNotNull(downloadKey, "Download key required");
    checkNotNull(messagePublisher,"No message publisher configured to send DoiChangeMessage");

    String xml = DataCiteValidator.toXml(doi, metadata);
    Message message = new ChangeDoiMessage(DoiStatus.REGISTERED, doi, xml, downloadTarget.resolve(downloadKey));

    try {
      messagePublisher.send(message, true);
    } catch (IOException e) {
      LOG.error("Failed sending DoiChangeMessage for {} and download {}", doi, downloadKey, e);
    }

  }

  @Override
  public void registerDataPackage(DOI doi, DataCiteMetadata metadata) throws InvalidMetadataException {
    checkNotNull(doi, "DOI required");
    checkNotNull(messagePublisher, "No message publisher configured to send DoiChangeMessage");

    String xml = DataCiteValidator.toXml(doi, metadata);
    Message message = new ChangeDoiMessage(DoiStatus.REGISTERED, doi, xml, dataPackageTarget.resolve(doi.getDoiName()));
    try {
      messagePublisher.send(message, true);
    } catch (IOException e) {
      LOG.error("Failed sending DoiChangeMessage for DataPackage {}", doi, e);
    }

  }

  @Override
  public void delete(DOI doi) {
    checkNotNull(doi, "DOI required");
    checkNotNull(messagePublisher,"No message publisher configured to send DoiChangeMessage");

    Message message = new ChangeDoiMessage(DoiStatus.DELETED, doi, null, null);

    try {
      messagePublisher.send(message, true);
    } catch (IOException e) {
      LOG.error("Failed sending DoiChangeMessage for {}", doi, e);
    }

  }
}
