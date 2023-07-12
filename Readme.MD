# It's gradle plugin Springwolfdoc2dto

This plugin help you generate DTO classes based SpringWolf docs.
Implementation based on [jsonschema2pojo](https://github.com/joelittlejohn/jsonschema2pojo) librar. 
Support of polymorphism according to AsyncApi standard is added.

## Enable plugin

```groovy
plugins {
    id 'io.github.stepanovd.springwolf2dto' version '1.0.6-alpha'
}
```
## Configuration

### Parameters:

* __String url__ - URL to springwolf docs (default is *http://{domain}/springwolf/docs*)
* __String targetPackage__ - class package for generated classes
* __String documentationTitle__ - title of service used in springwolf documentation. This title define key of documentation in json object. 
You can set this to empty string if you use springwolf version 0.10+
* __String channel__ - title of channel used in springwolf documentation. This title define title of channel from documentation. 
Plugin will generate DTO classes only from this channel if you fill this property.
You can set this to empty string if you want generate DTO classes by all channels.
* __Path targetDirectory__ - directory for save generated classes

### Set parameters:

```groovy
springWolfDoc2DTO{
    url = 'http://localhost:8080/springwolf/docs'
    targetPackage = 'example.package'
    documentationTitle = 'my-service'
    channel = 'kafka-channel'
    targetDirectory = project.layout.getBuildDirectory().dir("generated-sources")
}
```

## Run

```
./gradle -q generateDTO
```

# Examples

## Example of deserialization fields
Representation of source of message is shown below.
```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestEvent implements Serializable {

    private String id;
    private LocalDateTime occuredOn;
    private TestEvent.ValueType valueType;
    private Map<String, Boolean> flags;
    private String value;

    public enum ValueType {

        STRING("STRING"),
        BOOLEAN("BOOLEAN"),
        INTEGER("INTEGER"),
        DOUBLE("DOUBLE");
        
        private final String value;

        public ValueType(String value) {
            this.value = value;
        }
    }
}
```

SpringWolf generate json documentation:

```json5
{
  "service": {
    "serviceVersion": "2.0.0",
    "info": {
      //block with service info
    },
    "servers": {
      "kafka": {
        //describe of kafka connection
      }
    },
    "channels": {
      "kafka-channel": {
        "subscribe": {
          //...
          "message": {
            "oneOf": [
              {
                "name": "pckg.test.TestEvent",
                "title": "TestEvent",
                "payload": {
                  "$ref": "#/components/schemas/TestEvent"
                }
              }
            ]
          }
        },
        //...
      }
    },
    "components": {
      "schemas": {
        "TestEvent": {
          "type": "object",
          "properties": {
            "id": {
              "type": "string",
              "exampleSetFlag": false
            },
            "occuredOn": {
              "type": "string",
              "format": "date-time",
              "exampleSetFlag": false
            },
            "valueType": {
              "type": "string",
              "exampleSetFlag": false,
              "enum": [
                "STRING",
                "BOOLEAN",
                "INTEGER",
                "DOUBLE"
              ]
            },
            "flags": {
              "type": "object",
              "additionalProperties": {
                "type": "boolean",
                "exampleSetFlag": false
              },
              "exampleSetFlag": false
            },
            "value": {
              "type": "string",
              "exampleSetFlag": false
            }
          },
          "example": {
            "id": "string",
            "occuredOn": "2015-07-20T15:49:04",
            "valueType": "STRING",
            "flags": {
              "additionalProp1": true,
              "additionalProp2": true,
              "additionalProp3": true
            }
          },
          "exampleSetFlag": true
        }
      }
    }
  }
}
```

Generated DTO classes is shown below

```java

package pckg.test;

// import

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "occuredOn",
    "valueType",
    "flags",
    "value"
})
@Generated("jsonschema2pojo")
public class TestEvent implements Serializable
{
    @JsonProperty("id")
    private String id;
    @JsonProperty("occuredOn")
    private LocalDateTime occuredOn;
    @JsonProperty("valueType")
    private TestEvent.ValueType valueType;
    @JsonProperty("flags")
    private Flags flags;
    @JsonProperty("value")
    private String value;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();
    private final static long serialVersionUID = 7311052418845777748L;

    // Getters ans Setters

    @Generated("jsonschema2pojo")
    public enum ValueType {

        STRING("STRING"),
        BOOLEAN("BOOLEAN"),
        INTEGER("INTEGER"),
        DOUBLE("DOUBLE");
        private final String value;
        private final static Map<String, TestEvent.ValueType> CONSTANTS = new HashMap<String, TestEvent.ValueType>();

        static {
            for (TestEvent.ValueType c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        ValueType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static TestEvent.ValueType fromValue(String value) {
            TestEvent.ValueType constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }
    }
}
```

Important remark what __Map<String, Boolean> flags__ will be generate as alone class Flags with property **additionalProperties**

```java


package pckg.test;

// import

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({

})
@Generated("jsonschema2pojo")
public class Flags implements Serializable
{

    @JsonIgnore
    private Map<String, Boolean> additionalProperties = new LinkedHashMap<String, Boolean>();
    private final static long serialVersionUID = 7471055390730117740L;

    //getters and setters

}
```

## Polymorphism

If you need add polymorphism of messages you must add annotations to source of message

```java 
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@Setter(AccessLevel.PROTECTED)
@EqualsAndHashCode
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true,
    defaultImpl = ChangedEvent.class
)
@JsonSubTypes(value = {
    @JsonSubTypes.Type(name = ChangedEvent.type, value = ChangedEvent.class),
    @JsonSubTypes.Type(name = DeletedEvent.type, value = DeletedEvent.class)
})
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(oneOf = {ChangedEvent.class, DeletedEvent.class},
discriminatorProperty = "type",
discriminatorMapping = {
    @DiscriminatorMapping(value = ChangedEvent.type, schema = ChangedEvent.class),
    @DiscriminatorMapping(value = DeletedEvent.type, schema = DeletedEvent.class),
})
public abstract class DomainEvent {
    @Schema(required = true, nullable = false)
    private String id;
    
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime occuredOn = LocalDateTime.now();
    
    public abstract String getType();
}

public class ChangedEvent
    extends DomainEvent
    implements Serializable
{
    public static final String type = "CHANGED_EVENT";
    private String valueId;
    private String value;
}

public class DeletedEvent
    extends DomainEvent
    implements Serializable
{
    public static final String type = "DELETED_EVENT";
    private String valueId;
 }
```

Springwolf generate json documentation

```json5
{
    "service": {
        //common info
        "channels": {
            "kafka-channel": {
                "subscribe": {
                    //...
                    "message": {
                        "oneOf": [
                            {
                                "name": "pckg.test.DomainEvent",
                                "title": "DomainEvent",
                                "payload": {
                                    "$ref": "#/components/schemas/DomainEvent"
                                }
                            },
                            {
                                "name": "pckg.test.ChangedEvent",
                                "title": "ChangedEvent",
                                "payload": {
                                    "$ref": "#/components/schemas/ChangedEvent"
                                }
                            },
                            {
                                "name": "pckg.test.DeletedEvent",
                                "title": "DeletedEvent",
                                "payload": {
                                    "$ref": "#/components/schemas/DeletedEvent"
                                }
                            }
                        ]
                    }
                },
                //...
            }
        },
        "components": {
            "schemas": {
                "ChangedEvent": {
                    "type": "object",
                    "properties": {
                        "id": {
                            "type": "string",
                            "exampleSetFlag": false
                        },
                        "occuredOn": {
                            "type": "string",
                            "format": "date-time",
                            "exampleSetFlag": false
                        },
                        "value": {
                            "type": "string",
                            "exampleSetFlag": false
                        },
                        "valueId": {
                            "type": "string",
                            "exampleSetFlag": false
                        },
                        "type": {
                            "type": "string",
                            "exampleSetFlag": false
                        }
                    },
                    "example": {
                        "id": "string",
                        "occuredOn": "2015-07-20T15:49:04",
                        "value": "string",
                        "valueId": "string",
                        "type": "CHANGED_EVENT"
                    },
                    "exampleSetFlag": true
                },
                "DeletedEvent": {
                    "type": "object",
                    "properties": {
                        "id": {
                            "type": "string",
                            "exampleSetFlag": false
                        },
                        "occuredOn": {
                            "type": "string",
                            "format": "date-time",
                            "exampleSetFlag": false
                        },
                        "valueId": {
                            "type": "string",
                            "exampleSetFlag": false
                        },
                        "type": {
                            "type": "string",
                            "exampleSetFlag": false
                        }
                    },
                    "example": {
                        "id": "string",
                        "occuredOn": "2015-07-20T15:49:04",
                        "valueId": "string",
                        "type": "DELETED_EVENT"
                    },
                    "exampleSetFlag": true
                },
                "DomainEvent": {
                    "type": "object",
                    "properties": {
                        "id": {
                            "type": "string",
                            "exampleSetFlag": false
                        },
                        "occuredOn": {
                            "type": "string",
                            "format": "date-time",
                            "exampleSetFlag": false
                        },
                        "type": {
                            "type": "string",
                            "exampleSetFlag": false
                        }
                    },
                    "example": {
                        "id": "string",
                        "occuredOn": "2015-07-20T15:49:04",
                        "type": "string"
                    },
                    "discriminator": {
                        "propertyName": "type",
                        "mapping": {
                            "CHANGED_EVENT": "#/components/schemas/ChangedEvent",
                            "DELETED_EVENT": "#/components/schemas/DeletedEvent"
                        }
                    },
                    "exampleSetFlag": true,
                    "oneOf": [
                        {
                            "$ref": "#/components/schemas/ChangedEvent",
                            "exampleSetFlag": false
                        },
                        {
                            "$ref": "#/components/schemas/DeletedEvent",
                            "exampleSetFlag": false
                        }
                    ]
                }
            }
        }
    }
}
```

Then generated DTO classes is shown below

```java 

package pckg.test;

// import

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "occuredOn",
    "type"
})
@Generated("jsonschema2pojo")
@JsonTypeInfo(property = "type", use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(name = "CHANGED_EVENT", value = ChangedEvent.class),
    @JsonSubTypes.Type(name = "DELETED_EVENT", value = DeletedEvent.class)
})
public class DomainEvent implements Serializable
{

    @JsonProperty("id")
    protected String id;
    @JsonProperty("occuredOn")
    protected LocalDateTime occuredOn;
    @JsonProperty("type")
    protected String type;
    @JsonIgnore
    protected Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();
    protected final static long serialVersionUID = 4691666114019791903L;

    //getters and setters

}

// import

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "occuredOn",
    "valueId",
    "type"
})
@Generated("jsonschema2pojo")
public class DeletedEvent
    extends DomainEvent
    implements Serializable
{

    @JsonProperty("id")
    private String id;
    @JsonProperty("occuredOn")
    private LocalDateTime occuredOn;
    @JsonProperty("valueId")
    private String valueId;
    @JsonProperty("type")
    private String type;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();
    private final static long serialVersionUID = 7326381459761013337L;

    // getters and setters

}


package pckg.test;

//import

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "occuredOn",
    "value",
    "type"
})
@Generated("jsonschema2pojo")
public class ChangedEvent
    extends DomainEvent
    implements Serializable
{
    @JsonProperty("id")
    private String id;
    @JsonProperty("occuredOn")
    private LocalDateTime occuredOn;
    @JsonProperty("value")
    private String value;
    @JsonProperty("type")
    private String type;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();
    private final static long serialVersionUID = 5446866391322866265L;

    //getters and setters

}

```