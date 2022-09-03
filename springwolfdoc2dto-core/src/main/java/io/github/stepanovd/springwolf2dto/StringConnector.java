package io.github.stepanovd.springwolf2dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StringConnector implements Connector {
    private final String body;
    @Override
    public JsonNode load(Configuration config) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper()
                .enable(JsonParser.Feature.ALLOW_COMMENTS)
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

        JsonNode jsonNode = objectMapper.readTree(body);
        JsonNode schemas = jsonNode.get(config.documentationTitle()).get("components").get("schemas");
        return schemas;
    }
}
