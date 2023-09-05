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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.codemodel.*;
import org.apache.commons.lang3.StringUtils;
import org.jsonschema2pojo.GenerationConfig;
import org.jsonschema2pojo.Jackson2Annotator;
import org.jsonschema2pojo.SchemaGenerator;
import org.jsonschema2pojo.SchemaMapper;
import org.jsonschema2pojo.rules.RuleFactory;

import java.io.IOException;
import java.util.*;
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
        final JsonNode baseNode = connector.load(configuration);

        final Set<String> channelComponents = extractMessageTypesFromChannel(configuration, baseNode);

        JsonNode schemas = baseNode.get("components").get("schemas");
        JCodeModel jcodeModel = new JCodeModel();

        GenerationConfig config = new CustomGenerationConfig();

        //get ordered list of components and add referals to channelComponents
        List<String> componentList = sortComponents(schemas, channelComponents);

        Iterator<String> fieldNames = componentList.iterator();//schemas.fieldNames();

        Map<String, String> parentRefs = new HashMap<>();
        Map<String, Map<String, String>> discriminatorsMapping = new HashMap<>();

        CustomSchemaStore schemaStore = new CustomSchemaStore();

        while (fieldNames.hasNext()) {
            String componentName = fieldNames.next();

            if (skip(componentName, channelComponents)) {
                continue;
            }

            JsonNode schemaNode = schemas.get(componentName);
            String schema = schemaNode.toPrettyString();

            RuleFactory ruleFactory = new RuleFactory(config, new Jackson2Annotator(config), schemaStore);
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
                tryToAnnotateJsonIgnoreProperties((JDefinedClass) jType);
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

            String fragmentPath = "#/components/schemas/%s".formatted(componentName);
            schemaStore.create(fragmentPath, schemaNode, jType);

            schemaStore.invalidateOnPrefix("#/properties", fragmentPath);
        }

        tryToExtendsClasses(parentRefs, jcodeModel, configuration.packageName());
        discriminatorsMapping.forEach((cls, mapping) -> tryToAnnotateJsonSubTypes(cls, mapping, configuration.packageName(), jcodeModel));
        jcodeModel.build(configuration.outputJavaClassDirectory().toFile());
    }

    private static List<String> sortComponents(JsonNode schemas, Set<String> channelComponents) {
        Iterator<String> fieldNames = schemas.fieldNames();
        LinkedList<String> orderedList = new LinkedList<>();

        Map<String, Set<String>> refsChildToParents = new HashMap<>();
        Map<String, Set<String>> refsParentToChild = new HashMap<>();

        while (fieldNames.hasNext()) {
            String componentName = fieldNames.next();

            JsonNode schemaNode = schemas.get(componentName);
            if (schemaNode.has("properties")) {
                Iterator<Map.Entry<String, JsonNode>> properties = schemaNode.get("properties").fields();
                while (properties.hasNext()) {
                    Map.Entry<String, JsonNode> prop = properties.next();

                    if (prop.getValue().has("$ref")) {
                        String mappingRef = prop.getValue().get("$ref").asText();
                        String ref = mappingRef.substring(mappingRef.lastIndexOf("/") + 1);

                        Set<String> set = refsChildToParents.getOrDefault(ref, new HashSet<>());
                        set.add(componentName);
                        refsChildToParents.put(ref, set);

                        Set<String> setChilds = refsParentToChild.getOrDefault(componentName, new HashSet<>());
                        setChilds.add(ref);
                        refsParentToChild.put(componentName, setChilds);
                    }
                }

                if (refsChildToParents.containsKey(componentName)) {
                    Set<String> referals = refsChildToParents.get(componentName);

                    int it = orderedList.size();
                    int pos = orderedList.size();

                    Iterator<String> descendingIterator = orderedList.descendingIterator();

                    while (descendingIterator.hasNext()) {
                        it--;
                        String component = descendingIterator.next();
                        if (referals.contains(component)) {
                            pos = it;
                        }
                    }

                    orderedList.add(pos, componentName);
                } else {
                    orderedList.addLast(componentName);
                }
            } else {
                orderedList.addFirst(componentName);
            }
        }

        Iterator<String> descendingIterator = orderedList.descendingIterator();
        if (channelComponents != null && !channelComponents.isEmpty()) {
            while (descendingIterator.hasNext()) {
                String component = descendingIterator.next();
                if (channelComponents.contains(component) && refsParentToChild.containsKey(component)) {
                    channelComponents.addAll(refsParentToChild.get(component));
                }
            }
        }

        return orderedList;
    }

    private static boolean skip(String componentName, Set<String> channelComponents) {
        boolean excludeByChannel = (channelComponents != null && !channelComponents.isEmpty() && !channelComponents.contains(componentName));
        return excludeByChannel || componentName.startsWith("SpringKafkaDefaultHeaders") || "HeadersNotDocumented".equals(componentName);
    }

    private static void tryToExtendsClasses(Map<String, String> parentRefs, JCodeModel jcodeModel, String packageName) {
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
        generated.param("defaultImpl", jclass);
    }

    /**
     * Add JsonIgnoreProperties(ignoreUnknown = true) annotation for backward
     *
     * @JsonIgnoreProperties(ignoreUnknown = true)
     */
    private static void tryToAnnotateJsonIgnoreProperties(JDefinedClass jclass) {
        Class<JsonIgnoreProperties> jti = JsonIgnoreProperties.class;
        JClass annotationClass = jclass.owner().ref(jti.getName());
        JAnnotationUse generated = jclass.annotate(annotationClass);
        generated.param("ignoreUnknown", true);
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

    private static Set<String> extractMessageTypesFromChannel(Configuration configuration, JsonNode baseNode) {

        final Set<String> channelComponents = new HashSet<>();

        if (StringUtils.isNoneBlank(configuration.channel())) {
            JsonNode channel = baseNode.get("channels").get(configuration.channel());

            if (channel.has("subscribe")) {
                Set<String> types = extractMessageTypesFromChannelStream(channel.get("subscribe"));
                channelComponents.addAll(types);
            }

            if (channel.has("publish")) {
                Set<String> types = extractMessageTypesFromChannelStream(channel.get("publish"));
                channelComponents.addAll(types);
            }
        }

        return channelComponents;
    }

    private static Set<String> extractMessageTypesFromChannelStream(JsonNode channelStream) {
        Set<String> messageTypes = new HashSet<>();
        if (channelStream == null) {
            return messageTypes;
        }

        JsonNode messages = channelStream.get("message");

        if (messages.has("oneOf")) {
            JsonNode oneOf = messages.get("oneOf");

            Iterator<JsonNode> elements = oneOf.elements();
            while (elements.hasNext()) {
                JsonNode messageType = elements.next();
                String typeOfMessage = messageType.get("title").asText("");
                if (!typeOfMessage.isEmpty()) {
                    messageTypes.add(typeOfMessage);
                }
            }
        }

        return messageTypes;
    }
}
