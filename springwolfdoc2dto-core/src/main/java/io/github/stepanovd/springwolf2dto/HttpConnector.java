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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 *
 * @author Dmitry Stepanov (distep2@gmail.com)
 */
public class HttpConnector implements Connector {
    @Override
    public JsonNode load(Configuration config) throws JsonProcessingException {

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(config.url())).build();
        String body = "";
        try {
            body = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        ObjectMapper objectMapper = new ObjectMapper()
                .enable(JsonParser.Feature.ALLOW_COMMENTS)
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

        JsonNode jsonNode = objectMapper.readTree(body);
        JsonNode baseNode = config.documentationTitle() == null ? jsonNode : jsonNode.get(config.documentationTitle());
        JsonNode schemas = baseNode.get("components").get("schemas");
        return schemas;
    }
}
