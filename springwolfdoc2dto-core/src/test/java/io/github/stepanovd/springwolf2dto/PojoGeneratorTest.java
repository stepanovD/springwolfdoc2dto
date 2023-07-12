package io.github.stepanovd.springwolf2dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class PojoGeneratorTest {

    @Test
    void convertJsonToJavaClass() throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Path path = Paths.get("src/test/java");
        String packageName = "pckg.test";
        deleteDirectoryRecursive(path.resolve(packageName.split("\\.")[0]));
        Files.createDirectories(path);
        Configuration config = new Configuration("url", null, path, packageName, "service", null);

        Connector connector = new StringConnector(
                """
                          {
                          "service": {
                            "serviceVersion": "2.0.0",
                            "info": {
                              "title": "service",
                              "version": "2.0.1-SNAPSHOT"
                            },
                            "servers": {
                              "kafka": {
                                "url": "kafka0:9093",
                                "protocol": "kafka"
                              }
                            },
                            "channels": {
                              "kafka-channel": {
                                "subscribe": {
                                  "bindings": {
                                    "kafka": {

                                    }
                                  },
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
                                "bindings": {
                                  "kafka": {

                                  }
                                }
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
                                      "type": "object",
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
                        """);

        PojoGenerator.convertJsonToJavaClass(config, connector);

        Path srcPath = path;
        for (String subfolder : packageName.split("\\.")) {
            srcPath = srcPath.resolve(subfolder);
        }

        List<File> files = Files.list(srcPath)
                .map(Path::toFile)
                .collect(Collectors.toList());

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        try(StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {

            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(files);

            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

            JavaCompiler.CompilationTask task = compiler.getTask(
                    null,
                    fileManager,
                    diagnostics,
                    null,
                    null,
                    compilationUnits
            );

            task.call();

            for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
                System.out.format("Error on line %d in %s%n",
                        diagnostic.getLineNumber(),
                        diagnostic.getSource());
            }
        }

        ClassLoader classLoader = PojoGeneratorTest.class.getClassLoader();
        URLClassLoader urlClassLoader = new URLClassLoader(
                new URL[]{path.toUri().toURL()},
                classLoader);
        Class<?> testEventClass = urlClassLoader.loadClass("pckg.test.TestEvent");

        System.out.println("loaded class");

        String exampleObject = """
                     {
                        "id": "qwerty",
                        "occuredOn": "2015-07-20T15:49:04",
                        "valueType": "STRING",
                        "flags": {
                          "additionalProp1": false,
                          "additionalProp2": true,
                          "additionalProp3": true
                        }
                     }
                """;

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        var testObject = objectMapper.readValue(exampleObject, testEventClass);

        Assertions.assertEquals(testEventClass.getMethod("getId").invoke(testObject), "qwerty");
        Assertions.assertEquals(testEventClass.getMethod("getOccuredOn").invoke(testObject), LocalDateTime.of(2015, 7, 20, 15, 49, 4));
        Assertions.assertEquals(testEventClass.getMethod("getValueType").invoke(testObject).getClass().getName(), "pckg.test.TestEvent$ValueType");
        Assertions.assertEquals(testEventClass.getMethod("getFlags").invoke(testObject).getClass().getName(), "pckg.test.Flags");

        var flags = testEventClass.getMethod("getFlags").invoke(testObject);
        Map<String, Boolean> additionalProperties = (Map<String, Boolean>) flags.getClass().getMethod("getAdditionalProperties").invoke(flags);

        Assertions.assertFalse(additionalProperties.get("additionalProp1"));
        Assertions.assertTrue(additionalProperties.get("additionalProp2"));
        Assertions.assertTrue(additionalProperties.get("additionalProp3"));

        System.out.println("deserialized object");
        deleteDirectoryRecursive(path.resolve(packageName.split("\\.")[0]));
    }

    @Test
    void convertJsonToJavaClassWithPolymorphism() throws IOException, ClassNotFoundException {
        Path path = Paths.get("src/test/java");
        String packageName = "pckg.test";
        deleteDirectoryRecursive(path.resolve(packageName.split("\\.")[0]));

        Files.createDirectories(path);
        Configuration config = new Configuration("url", null, path, packageName, "service", null);

        Connector connector = new StringConnector(
                """                   
                        {
                            "service": {
                                "asyncapi": "2.0.0",
                                "info": {
                                    "title": "service",
                                    "version": "2.0.1-SNAPSHOT"
                                },
                                "servers": {
                                    "kafka": {
                                        "url": "localhost:9092",
                                        "protocol": "kafka"
                                    }
                                },
                                "channels": {
                                    "kafka-channel": {
                                        "subscribe": {
                                            "bindings": {
                                                "kafka": {}
                                            },
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
                                        "bindings": {
                                            "kafka": {}
                                        }
                                    }
                                },
                                "components": {
                                    "schemas": {
                                        "ChangedEvent": {
                                            "required": [
                                                "id",
                                                "occuredOn",
                                                "value",
                                                "type"
                                            ],
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
                                                "type": {
                                                    "type": "string",
                                                    "exampleSetFlag": false
                                                }
                                            },
                                            "example": {
                                                "id": "string",
                                                "occuredOn": "2015-07-20T15:49:04",
                                                "value": "string",
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
                                   
                        """
        );

        PojoGenerator.convertJsonToJavaClass(config, connector);

        Path srcPath = path;
        for (String subfolder : packageName.split("\\.")) {
            srcPath = srcPath.resolve(subfolder);
        }

        List<File> files = Files.list(srcPath)
                .map(Path::toFile)
                .collect(Collectors.toList());

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        try(StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {

            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(files);

            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

            JavaCompiler.CompilationTask task = compiler.getTask(
                    null,
                    fileManager,
                    diagnostics,
                    null,
                    null,
                    compilationUnits
            );

            task.call();

            for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
                System.out.format("Error on line %d in %s%n",
                        diagnostic.getLineNumber(),
                        diagnostic.getSource());
            }
        }

        ClassLoader classLoader = PojoGeneratorTest.class.getClassLoader();
        URLClassLoader urlClassLoader = new URLClassLoader(
                new URL[]{path.toUri().toURL()},
                classLoader);
        Class<?> domainEventClass = urlClassLoader.loadClass("pckg.test.DomainEvent");

        System.out.println("loaded class");

        String exampleChangedEvent = """
                     {
                        "id": "string",
                        "occuredOn": "2015-07-20T15:49:04",
                        "value": "string",
                        "type": "CHANGED_EVENT"
                    }
                """;
        String exampleDeletedEvent = """
                     {
                        "id": "string",
                        "occuredOn": "2015-07-20T15:49:04",
                        "valueId": "string",
                        "type": "DELETED_EVENT"
                    }
                """;

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        var testChangedObject = objectMapper.readValue(exampleChangedEvent, domainEventClass);
        var testDeletedObject = objectMapper.readValue(exampleDeletedEvent, domainEventClass);

        Assertions.assertEquals(testChangedObject.getClass().getName(), "pckg.test.ChangedEvent");
        Assertions.assertEquals(testDeletedObject.getClass().getName(), "pckg.test.DeletedEvent");

        System.out.println("deserialized object");

        deleteDirectoryRecursive(path.resolve(packageName.split("\\.")[0]));
    }

    @Test
    void convertJsonToJavaClassWithPolymorphismWithoutTitle() throws IOException, ClassNotFoundException {
        Path path = Paths.get("src/test/java");
        String packageName = "pckg.test";
        deleteDirectoryRecursive(path.resolve(packageName.split("\\.")[0]));

        Files.createDirectories(path);
        Configuration config = new Configuration("url", null, path, packageName, null, "kafka-channel");

        Connector connector = new StringConnector(
                """                   
                        {
                            "asyncapi": "2.0.0",
                            "info": {
                                "title": "service",
                                "version": "2.0.1-SNAPSHOT"
                            },
                            "servers": {
                                "kafka": {
                                    "url": "localhost:9092",
                                    "protocol": "kafka"
                                }
                            },
                            "channels": {
                                "kafka-channel": {
                                    "subscribe": {
                                        "bindings": {
                                            "kafka": {}
                                        },
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
                                    "bindings": {
                                        "kafka": {}
                                    }
                                }
                            },
                            "components": {
                                "schemas": {
                                    "ChangedEvent": {
                                        "required": [
                                            "id",
                                            "occuredOn",
                                            "value",
                                            "type"
                                        ],
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
                                            "type": {
                                                "type": "string",
                                                "exampleSetFlag": false
                                            }
                                        },
                                        "example": {
                                            "id": "string",
                                            "occuredOn": "2015-07-20T15:49:04",
                                            "value": "string",
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
                                   
                        """
        );

        PojoGenerator.convertJsonToJavaClass(config, connector);

        Path srcPath = path;
        for (String subfolder : packageName.split("\\.")) {
            srcPath = srcPath.resolve(subfolder);
        }

        List<File> files = Files.list(srcPath)
                .map(Path::toFile)
                .collect(Collectors.toList());

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        try(StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {

            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(files);

            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

            JavaCompiler.CompilationTask task = compiler.getTask(
                    null,
                    fileManager,
                    diagnostics,
                    null,
                    null,
                    compilationUnits
            );

            task.call();

            for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
                System.out.format("Error on line %d in %s%n",
                        diagnostic.getLineNumber(),
                        diagnostic.getSource());
            }
        }

        ClassLoader classLoader = PojoGeneratorTest.class.getClassLoader();
        URLClassLoader urlClassLoader = new URLClassLoader(
                new URL[]{path.toUri().toURL()},
                classLoader);
        Class<?> domainEventClass = urlClassLoader.loadClass("pckg.test.DomainEvent");

        System.out.println("loaded class");

        String exampleChangedEvent = """
                     {
                        "id": "string",
                        "occuredOn": "2015-07-20T15:49:04",
                        "value": "string",
                        "type": "CHANGED_EVENT"
                    }
                """;
        String exampleDeletedEvent = """
                     {
                        "id": "string",
                        "occuredOn": "2015-07-20T15:49:04",
                        "valueId": "string",
                        "type": "DELETED_EVENT"
                    }
                """;

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        var testChangedObject = objectMapper.readValue(exampleChangedEvent, domainEventClass);
        var testDeletedObject = objectMapper.readValue(exampleDeletedEvent, domainEventClass);

        Assertions.assertEquals(testChangedObject.getClass().getName(), "pckg.test.ChangedEvent");
        Assertions.assertEquals(testDeletedObject.getClass().getName(), "pckg.test.DeletedEvent");

        System.out.println("deserialized object");

//        deleteDirectoryRecursive(path.resolve(packageName.split("\\.")[0]));
    }

    private void deleteDirectoryRecursive(Path path) throws IOException {
        Files.walkFileTree(path, new FileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Test
//    @Disabled
    void convertJsonFromHttpToJavaClass() throws IOException {
        Path path = Paths.get("src/test/java");
        String packageName = "pckg.test";
        deleteDirectoryRecursive(path.resolve(packageName.split("\\.")[0]));

        Files.createDirectories(path);

        String url = "http://localhost:8080/springwolf/docs";
        Configuration config = new Configuration(url, null, path, packageName, "", "goodt-core-orgstructure");

        Connector connector = new HttpConnector();
        PojoGenerator.convertJsonToJavaClass(config, connector);

        Path srcPath = path;
        for (String subfolder : packageName.split("\\.")) {
            srcPath = srcPath.resolve(subfolder);
        }

        List<File> files = Files.list(srcPath)
                .map(Path::toFile)
                .collect(Collectors.toList());

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        try(StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {

            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(files);

            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

            JavaCompiler.CompilationTask task = compiler.getTask(
                    null,
                    fileManager,
                    diagnostics,
                    null,
                    null,
                    compilationUnits
            );

            task.call();

            for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
                System.out.format("Error on line %d in %s%n",
                        diagnostic.getLineNumber(),
                        diagnostic.getSource());
            }
        }
    }
}