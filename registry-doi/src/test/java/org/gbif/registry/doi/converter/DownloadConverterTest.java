package org.gbif.registry.doi.converter;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.service.datacite.DataCiteValidator;
import org.gbif.occurrence.query.TitleLookup;
import org.gbif.utils.file.FileUtils;
import org.junit.Test;
import org.xmlunit.matchers.CompareMatcher;

import java.net.URI;

import static org.gbif.registry.doi.converter.DownloadTestDataProvider.prepareDatasetOccurrenceDownloadUsage1;
import static org.gbif.registry.doi.converter.DownloadTestDataProvider.prepareDatasetOccurrenceDownloadUsage2;
import static org.gbif.registry.doi.converter.DownloadTestDataProvider.prepareDownload;
import static org.gbif.registry.doi.converter.DownloadTestDataProvider.prepareUser;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DownloadConverterTest {

  @Test
  public void testConvertDownload() throws Exception {
    // given
    DatasetOccurrenceDownloadUsage du1 = prepareDatasetOccurrenceDownloadUsage1();
    DatasetOccurrenceDownloadUsage du2 = prepareDatasetOccurrenceDownloadUsage2();
    Download download = prepareDownload();
    GbifUser user = prepareUser();
    // mock title lookup API
    TitleLookup tl = mock(TitleLookup.class);
    when(tl.getSpeciesName(anyString())).thenReturn("Abies alba Mill.");
    final DataCiteMetadata expectedMetadata = DataCiteValidator
      .fromXml(FileUtils.classpathStream("metadata/metadata-download.xml"));
    final String expected = DataCiteValidator.toXml(expectedMetadata, true);

    // when
    DataCiteMetadata metadata = DownloadConverter.convert(download, user, Lists.newArrayList(du1, du2), tl);
    String actualXmlMetadata = DataCiteValidator.toXml(download.getDoi(), metadata);

    // then
    assertThat(actualXmlMetadata,
      CompareMatcher.isIdenticalTo(expected).normalizeWhitespace().ignoreWhitespace());
    verify(tl, atLeastOnce()).getSpeciesName(anyString());
  }

  @Test
  public void testTruncateDescription() throws Exception {
    // given
    DOI doi = new DOI("10.15468/dl.v8zc57");
    String sourceXml = Resources.toString(
      Resources.getResource("metadata/datacite-large.xml"), Charsets.UTF_8);

    // when
    String truncatedXml = DownloadConverter.truncateDescription(doi, sourceXml, URI.create("http://gbif.org"));

    // then
    DataCiteValidator.validateMetadata(truncatedXml);
    assertTrue(truncatedXml.contains("for full list of all constituents"));
    assertFalse(truncatedXml.contains("University of Ghent"));
    assertTrue(truncatedXml.contains("10.15468/siye1z"));
    assertEquals(3690, truncatedXml.length());
  }

  @Test
  public void testTruncateConstituents() throws Exception {
    // given
    DOI doi = new DOI("10.15468/dl.v8zc57");
    String sourceXml = Resources.toString(
      Resources.getResource("metadata/datacite-large.xml"), Charsets.UTF_8);

    // when
    String truncatedXml = DownloadConverter.truncateConstituents(doi, sourceXml, URI.create("http://gbif.org"));

    // then
    DataCiteValidator.validateMetadata(truncatedXml);
    assertTrue(truncatedXml.contains("for full list of all constituents"));
    assertFalse(truncatedXml.contains("University of Ghent"));
    assertFalse(truncatedXml.contains("10.15468/siye1z"));
    assertEquals(2352, truncatedXml.length());
  }
}