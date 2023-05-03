package io.github.stepanovd.springwolf2dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.codemodel.JType;
import org.jsonschema2pojo.ContentResolver;
import org.jsonschema2pojo.Schema;
import org.jsonschema2pojo.SchemaStore;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;

public class CustomSchemaStore extends SchemaStore {

    public CustomSchemaStore() {
        super();
    }

    public CustomSchemaStore(ContentResolver contentResolver) {
        super(contentResolver);
    }

    public Schema create(String fragmentPath, JsonNode jsonNode, JType javaType) {
        if (fragmentPath.contains("#")) {
            String pathExcludingFragment = substringBefore(fragmentPath, "#");
            String fragment = substringAfter(fragmentPath, "#");
            URI fragmentURI;
            try {
                fragmentURI = new URI(null, null, fragment);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Invalid fragment: " + fragment + " in path: " + fragmentPath);
            }
            fragmentPath = pathExcludingFragment + "#" + fragmentURI.getRawFragment();
        }

        URI id = URI.create(fragmentPath);

        Schema baseSchema = new Schema(id, jsonNode, null);
        baseSchema.setJavaType(javaType);
        if (schemas.containsKey(id)) {
            return schemas.get(id);
        } else {
            schemas.put(id, baseSchema);
            return baseSchema;
        }
    }

    public void invalidateOnPrefix(String checkPrefix, String newPrefix) {
        Set<URI> uris = schemas.keySet().stream().filter(id -> id.toString().startsWith(checkPrefix)).collect(Collectors.toSet());
        for(URI id: uris) {
            String s = id.toString();
            if(s.startsWith("#")) {
                s = newPrefix + s.substring(1);
            }

            URI newId = createId(s);

            Schema schema = schemas.remove(id);
            schemas.put(newId, schema);
        }
    }

    private URI createId(String fragmentPath) {
        if (fragmentPath.contains("#")) {
            String pathExcludingFragment = substringBefore(fragmentPath, "#");
            String fragment = substringAfter(fragmentPath, "#");
            URI fragmentURI;
            try {
                fragmentURI = new URI(null, null, fragment);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Invalid fragment: " + fragment + " in path: " + fragmentPath);
            }
            fragmentPath = pathExcludingFragment + "#" + fragmentURI.getRawFragment();
        }

        URI id = URI.create(fragmentPath);
        return id;
    }
}
