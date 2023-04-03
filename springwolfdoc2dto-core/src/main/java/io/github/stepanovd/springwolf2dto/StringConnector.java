/**
 * Copyright © 2010-2014 Nokia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.stepanovd.springwolf2dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author Dmitry Stepanov (distep2@gmail.com)
 */
@RequiredArgsConstructor
public class StringConnector implements Connector {
    private final String body;
    @Override
    public JsonNode load(Configuration config) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper()
                .enable(JsonParser.Feature.ALLOW_COMMENTS)
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

        JsonNode jsonNode = objectMapper.readTree(body);
        JsonNode baseNode = config.documentationTitle() == null || config.documentationTitle().isEmpty()? jsonNode : jsonNode.get(config.documentationTitle());
        JsonNode schemas = baseNode.get("components").get("schemas");
        return schemas;
    }
}
