package org.gbif.registry.ws.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.IOException;
import java.util.List;

@JsonSerialize(using = LegacyEndpointListWrapper.LegacyEndpointResponseListWrapperJsonSerializer.class)
@XmlRootElement(name = "legacyEndpointResponses")
public class LegacyEndpointListWrapper {

  private List<LegacyEndpointResponse> legacyEndpointResponses;

  public LegacyEndpointListWrapper() {
  }

  public LegacyEndpointListWrapper(List<LegacyEndpointResponse> legacyEndpointResponses) {
    this.legacyEndpointResponses = legacyEndpointResponses;
  }

  @XmlElement(name = "service")
  public List<LegacyEndpointResponse> getLegacyEndpointResponses() {
    return legacyEndpointResponses;
  }

  public void setLegacyEndpointResponses(List<LegacyEndpointResponse> legacyEndpointResponses) {
    this.legacyEndpointResponses = legacyEndpointResponses;
  }

  public static class LegacyEndpointResponseListWrapperJsonSerializer extends JsonSerializer<LegacyEndpointListWrapper> {

    @Override
    public void serialize(LegacyEndpointListWrapper value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
      if (value == null) {
        jgen.writeNull();
        return;
      }

      jgen.writeStartArray();
      if (value.getLegacyEndpointResponses() != null) {
        for (LegacyEndpointResponse item : value.getLegacyEndpointResponses()) {
          jgen.writeObject(item);
        }
      }
      jgen.writeEndArray();
    }
  }
}
