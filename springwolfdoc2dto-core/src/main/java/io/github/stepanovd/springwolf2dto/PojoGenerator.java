package io.github.stepanovd.springwolf2dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.codemodel.JCodeModel;
import org.jsonschema2pojo.*;
import org.jsonschema2pojo.rules.RuleFactory;

import java.io.IOException;
import java.util.Iterator;


public class PojoGenerator {
    private PojoGenerator() {
    }

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
