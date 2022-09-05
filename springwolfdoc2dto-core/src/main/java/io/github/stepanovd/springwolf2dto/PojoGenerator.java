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

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.codemodel.JCodeModel;
import org.jsonschema2pojo.*;
import org.jsonschema2pojo.rules.RuleFactory;

import java.io.IOException;
import java.util.Iterator;

/**
 *
 * @author Dmitry Stepanov (distep2@gmail.com)
 */
public class PojoGenerator {
    private PojoGenerator() {
    }

    /**
     * Generate Java classes based on json schemas
     * @param configuration configuration {@link Configuration}
     * @param connector implementation of connector for extract json schemas
     * @throws IOException i/o exception
     *
     * @see HttpConnector
     * @see StringConnector
     */
    public static void convertJsonToJavaClass(Configuration configuration, Connector connector)
            throws IOException {
        JsonNode schemas = connector.load(configuration);

        JCodeModel jcodeModel = new JCodeModel();

        GenerationConfig config = new DefaultGenerationConfig() {
            @Override
            public boolean isGenerateBuilders() {
                return true;
            }

            @Override
            public SourceType getSourceType() {
                return SourceType.JSONSCHEMA;
            }

            @Override
            public AnnotationStyle getAnnotationStyle() {
                return AnnotationStyle.JACKSON;
            }

            @Override
            public String getDateTimeType() {
                return "java.time.LocalDateTime";
            }

            @Override
            public String getDateType() {
                return "java.time.LocalDate";
            }



            @Override
            public boolean isIncludeHashcodeAndEquals() {
                return false;
            }

            @Override
            public boolean isIncludeToString() {
                return false;
            }
            @Override
            public boolean isIncludeGeneratedAnnotation() {
                return true;
            }
        };

        Iterator<String> fieldNames = schemas.fieldNames();
        while(fieldNames.hasNext()) {
            String next = fieldNames.next();

            String schema = schemas.get(next).toPrettyString();

            SchemaMapper mapper = new SchemaMapper(new RuleFactory(config, new Jackson2Annotator(config), new SchemaStore()), new SchemaGenerator());
            mapper.generate(jcodeModel, next, configuration.packageName(), schema);

            jcodeModel.build(configuration.outputJavaClassDirectory().toFile());
        }
    }
}
