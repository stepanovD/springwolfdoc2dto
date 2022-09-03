package io.github.stepanovd.springwolf2dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

public interface Connector {
    JsonNode load(Configuration config) throws JsonProcessingException;
}
