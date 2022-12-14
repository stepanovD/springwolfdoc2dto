/**
 * Copyright © 2010-2014 Nokia
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.stepanovd.springwolf2dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.codemodel.*;
import org.jsonschema2pojo.*;
import org.jsonschema2pojo.rules.RuleFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Dmitry Stepanov (distep2@gmail.com)
 */
public class PojoGenerator {
    private PojoGenerator() {
    }

    /**
     * Generate Java classes based on json schemas
     *
     * @param configuration configuration {@link Configuration}
     * @param connector     implementation of connector for extract json schemas
     * @throws IOException i/o exception
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

            @Override
            public boolean isSerializable() {
                return true;
            }


        };

        Iterator<String> fieldNames = schemas.fieldNames();

        Map<String, String> parentRefs = new HashMap<>();
        Map<String, Map<String, String>> discriminatorsMapping = new HashMap<>();

        while (fieldNames.hasNext()) {
            String componentName = fieldNames.next();

            JsonNode schemaNode = schemas.get(componentName);
            String schema = schemaNode.toPrettyString();


            RuleFactory ruleFactory = new RuleFactory(config, new Jackson2Annotator(config), new SchemaStore());
            SchemaMapper mapper = new SchemaMapper(ruleFactory, new SchemaGenerator());
            JType jType = mapper.generate(jcodeModel, componentName, configuration.packageName(), schema);

            //process references to child classes
            if (schemaNode.has("oneOf")) {
                JsonNode oneOf = schemaNode.get("oneOf");

                Iterator<JsonNode> elements = oneOf.elements();
                while (elements.hasNext()) {
                    JsonNode childNode = elements.next();
                    String childRef = childNode.get("$ref").asText("");
                    childRef = childRef.substring(childRef.lastIndexOf("/") + 1);
                    if (!childRef.isEmpty()) {
                        parentRefs.put(childRef, componentName);
                    }
                }

                ((JDefinedClass) jType).fields().forEach((fieldName, field) -> field.mods().setProtected());
            }

            //process discriminator
            if (schemaNode.has("discriminator")) {
                JsonNode discriminator = schemaNode.get("discriminator");
                JsonNode propertyName = discriminator.get("propertyName");


                tryToAnnotateJsonTypeInfo((JDefinedClass) jType, propertyName.asText());
                if (discriminator.has("mapping")) {
                    JsonNode mappingNode = discriminator.get("mapping");
                    Iterator<String> discriminatorValues = mappingNode.fieldNames();

                    Map<String, String> mapping = new HashMap<>();

                    while (discriminatorValues.hasNext()) {
                        String dValue = discriminatorValues.next();
                        String mappingRef = mappingNode.get(dValue).asText();


                        mapping.put(dValue, mappingRef.substring(mappingRef.lastIndexOf("/") + 1));
                    }

                    discriminatorsMapping.put(componentName, mapping);
                }
            }
        }

        tryToExtendsClasses(parentRefs, jcodeModel, configuration.packageName());
        discriminatorsMapping.forEach((cls, mapping) -> tryToAnnotateJsonSubTypes(cls, mapping, configuration.packageName(), jcodeModel));
        jcodeModel.build(configuration.outputJavaClassDirectory().toFile());
    }

    private static void tryToExtendsClasses(Map<String, String> parentRefs, JCodeModel jcodeModel, String packageName){
        parentRefs.forEach((cls, ref) -> jcodeModel.packages().forEachRemaining(p -> {
            if (p.name().equals(packageName)) {
                AtomicReference<JDefinedClass> clsClass = new AtomicReference<>();
                AtomicReference<JDefinedClass> refClass = new AtomicReference<>();

                p.classes().forEachRemaining(c -> {
                    if (c.name().equals(cls)) {
                        clsClass.set(c);
                    } else if (c.name().equals(ref)) {
                        refClass.set(c);
                    }
                });

                if (clsClass.get() != null && refClass.get() != null) {
                    clsClass.get()._extends(refClass.get());

                }
            }
        }));
    }

    /**
     * Add JsonTypeInfo annotation if discriminator defined
     *
     * @JsonTypeInfo( use = JsonTypeInfo.Id.NAME,
     * include = JsonTypeInfo.As.EXISTING_PROPERTY,
     * property = "type",
     * visible = true,
     * defaultImpl = AttributeChangedEvent.class
     * )
     */
    private static void tryToAnnotateJsonTypeInfo(JDefinedClass jclass, String propertyName) {
        Class<JsonTypeInfo> jti = JsonTypeInfo.class;
        JClass annotationClass = jclass.owner().ref(jti.getName());
        JAnnotationUse generated = jclass.annotate(annotationClass);
        generated.param("property", propertyName);
        generated.param("use", JsonTypeInfo.Id.NAME);
        generated.param("include", JsonTypeInfo.As.EXISTING_PROPERTY);
        generated.param("visible", true);
    }

    /**
     * add JsonSubTypes annotation to superclass if define discriminator and discrimination mapping
     *
     * @JsonSubTypes(value = {
     * @JsonSubTypes.Type(name = "CHANGED_EVENT", value = ChangedEvent.class),
     * @JsonSubTypes.Type(name = "DELETED_EVENT", value = DeletedEvent.class)
     * })
     */
    private static void tryToAnnotateJsonSubTypes(String className, Map<String, String> mapping, String packageName, JCodeModel model) {
        final Map<String, JDefinedClass> classes = new HashMap<>();
        final Collection<String> mappingClasses = mapping.values();

        model.packages().forEachRemaining(p -> {
            if (p.name().equals(packageName)) {

                p.classes().forEachRemaining(c -> {
                    if (c.name().equals(className)) {
                        classes.put(className, c);
                    } else if (mappingClasses.contains(c.name())) {
                        classes.put(c.name(), c);
                    }
                });
            }
        });

        JDefinedClass parentClass = classes.get(className);

        Class<JsonSubTypes> jst = JsonSubTypes.class;
        JClass annotationClass = parentClass.owner().ref(jst.getName());
        JAnnotationUse generated = parentClass.annotate(annotationClass);
        JAnnotationArrayMember values = generated.paramArray("value");

        Class<JsonSubTypes.Type> jstType = JsonSubTypes.Type.class;
        JClass annotationClassType = parentClass.owner().ref(jstType.getName());

        mapping.forEach((descriminator, classRef) -> {
            JAnnotationUse annotate = values.annotate(annotationClassType);
            annotate.param("name", descriminator);
            annotate.param("value", classes.get(classRef));
        });

    }
}
