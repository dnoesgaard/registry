package org.gbif.registry.ws.client;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.api.service.registry.DatasetOccurrenceDownloadUsageService;
import org.gbif.registry.ws.client.retrofit.DatasetOccurrenceDownloadUsageRetrofitClient;
import retrofit2.Response;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.UUID;

public class DatasetOccurrenceDownloadUsageWsClient
    implements DatasetOccurrenceDownloadUsageService {

  private final DatasetOccurrenceDownloadUsageRetrofitClient client;

  public DatasetOccurrenceDownloadUsageWsClient(DatasetOccurrenceDownloadUsageRetrofitClient client) {
    this.client = client;
  }

  @Override
  public PagingResponse<DatasetOccurrenceDownloadUsage> listByDataset(@NotNull UUID uuid, @Nullable Pageable pageable) {
    final Response<PagingResponse<DatasetOccurrenceDownloadUsage>> response =
        SyncCall.syncCallWithResponse(client.listByDataset(uuid, pageable.getLimit(), pageable.getOffset()));
    return response.body();
  }
}
