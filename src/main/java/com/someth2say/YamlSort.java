//usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.quarkus.platform:quarkus-bom:3.0.0.Beta1@pom
//DEPS io.quarkus:quarkus-picocli
//DEPS io.github.yaml-path:yaml-path:0.0.7
//JAVAC_OPTIONS -parameters
//JAVA_OPTIONS -Djava.util.logging.manager=org.jboss.logmanager.LogManager
//JAVA_OPTIONS -Dquarkus.banner.enabled=false
//JAVA_OPTIONS -Dquarkus.log.level=OFF

package com.someth2say;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.github.yamlpath.YamlExpressionParser;
import io.github.yamlpath.YamlPath;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.databind.SerializationFeature.*;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.*;
import static java.util.Comparator.comparing;

@Command(name = "yamlsort", mixinStandardHelpOptions = true)
public class YamlSort implements Runnable {

    @Option(paramLabel = "<file>", names = { "-i", "--input" }, description = "The YAML file to sort documents from. If not provided, STDIN is used.")
    Path path;

    @Option(paramLabel = "<yamlPath>", description = "YamlPath expression to sort documents.", arity = "1..", names = { "-y",
            "-yamlpath" }, required = true)
    List<String> yamlPaths;

    @Option(description = "Sort document keys.", names = { "-k", "--sortKeys" }, negatable = true)
    boolean sortKeys=true;

    @Override
    public void run() {
        try {
            // Split the input file in documents
            String result = path!=null?sortYamlFile(path, yamlPaths, sortKeys):sortYamlStream(System.in, yamlPaths, sortKeys);
            System.out.println(result);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String sortYamlFile(Path path, List<String> yamlPaths, boolean sortKeys) throws IOException {
        String input = Files.readString(path);
        return sortYamlString(input, yamlPaths, sortKeys);
    }

    public static String sortYamlStream(InputStream stream, List<String> yamlPaths, boolean sortKeys) throws IOException {
        String input = new String(stream.readAllBytes());
        return sortYamlString(input, yamlPaths, sortKeys);
    }

    public static String sortYamlString(String input, List<String> yamlPaths, boolean sortKeys) {
        String[] docs = input.split("---" + System.lineSeparator());
        List<YamlExpressionParser> parsers = Arrays.stream(docs).map(YamlPath::from)
                .collect(Collectors.toList());

        // Reverse the order of the yamlPath expression to perform stable sort.
        Collections.reverse(yamlPaths);

        // Sort documents
        yamlPaths.forEach(yamlPath -> parsers.sort(comparing((YamlExpressionParser o) -> {
                    Object value = o.readSingle(yamlPath);
                    return value != null ? value.toString() : "";
                }))
        );

        ObjectMapper yamlMapper = sortKeys?getSortedYamlMapper():getYamlMapper();

        return parsers.stream().map(yamlExpressionParser -> dumpAsDocuments(yamlExpressionParser, yamlMapper)).collect(Collectors.joining());
    }

    public static String dumpAsDocuments(YamlExpressionParser yamlExpressionParser, ObjectMapper yamlMapper) {
        try {

            List<Map<Object, Object>> resources = yamlExpressionParser.getResources();
            StringBuilder sb = new StringBuilder();
            for (Map<Object, Object> resource : resources) {
                sb.append(yamlMapper.writeValueAsString(resource));
            }
            return sb.toString();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static final ObjectMapper SORTED_YAML_MAPPER = createYamlMapper(
            new YAMLGenerator.Feature[] { MINIMIZE_QUOTES, ALWAYS_QUOTE_NUMBERS_AS_STRINGS, INDENT_ARRAYS_WITH_INDICATOR },
            new SerializationFeature[] { INDENT_OUTPUT, ORDER_MAP_ENTRIES_BY_KEYS },
            new SerializationFeature[] { WRITE_NULL_MAP_VALUES, WRITE_EMPTY_JSON_ARRAYS });

    private static final ObjectMapper YAML_MAPPER = createYamlMapper(
            new YAMLGenerator.Feature[] { MINIMIZE_QUOTES, ALWAYS_QUOTE_NUMBERS_AS_STRINGS, INDENT_ARRAYS_WITH_INDICATOR },
            new SerializationFeature[] { INDENT_OUTPUT },
            new SerializationFeature[] { WRITE_NULL_MAP_VALUES, WRITE_EMPTY_JSON_ARRAYS });

    private static ObjectMapper createYamlMapper(YAMLGenerator.Feature[] generatorFeatures,
            SerializationFeature[] enabledFeatures, SerializationFeature[] disabledFeatures) {
        YAMLFactory yamlFactory = new YAMLFactory();
        for (YAMLGenerator.Feature feature : generatorFeatures) {
            yamlFactory = yamlFactory.enable(feature);
        }
        ObjectMapper objectMapper = new ObjectMapper(yamlFactory);

        for (SerializationFeature feature : enabledFeatures) {
            objectMapper.configure(feature, true);
        }
        for (SerializationFeature feature : disabledFeatures) {
            objectMapper.configure(feature, false);
        }

        return objectMapper;
    }

    private static ObjectMapper getSortedYamlMapper() {
        return SORTED_YAML_MAPPER;
    }

    private static ObjectMapper getYamlMapper() {
        return YAML_MAPPER;
    }

}
