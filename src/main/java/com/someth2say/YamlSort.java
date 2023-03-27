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
import io.github.yamlpath.YamlExpressionParser;
import io.github.yamlpath.YamlPath;
import io.github.yamlpath.utils.SerializationUtils;
import picocli.CommandLine.Option;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;


@Command(name = "yamlsort", mixinStandardHelpOptions = true)
public class YamlSort implements Runnable {

    @Parameters(paramLabel = "<file>", description = "The YAML file to sort documents from.", arity = "1")
    Path path;

    @Option(paramLabel = "<yamlPath>", description = "YamlPath expression to sort documents.", arity = "1..", names = {"-yp", "-yamlpath"}, required = true)
    List<String> yamlPaths;

    @Override
    public void run() {
        try {
            String s = Files.readString(path);
            String[] split = s.split("---" + System.lineSeparator());
            List<YamlExpressionParser> docs = Arrays.stream(split).filter(Predicate.not(String::isBlank)).map(YamlPath::from).collect(Collectors.toList());

            Collections.reverse(yamlPaths);
            yamlPaths.forEach(yamlPath-> docs.sort(Comparator.comparing((YamlExpressionParser o) -> {
                Object value = o.readSingle(yamlPath);
                return value!=null?value.toString():"";
            }))
            );

            System.out.println(docs.stream().map(YamlSort::dumpAsDocuments).collect(Collectors.joining()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String dumpAsDocuments(YamlExpressionParser yamlExpressionParser) {
        try {
            List<Map<Object, Object>> resources = yamlExpressionParser.getResources();
            StringBuilder sb = new StringBuilder();
            for (Map<Object, Object> resource : resources) {
                sb.append(SerializationUtils.yamlMapper().writeValueAsString(resource));
            }
            return sb.toString();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


}
