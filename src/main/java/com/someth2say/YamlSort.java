//usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.quarkus.platform:quarkus-bom:3.0.0.Beta1@pom
//DEPS io.quarkus:quarkus-picocli
//DEPS io.github.yaml-path:yaml-path:0.0.9
//JAVAC_OPTIONS -parameters
//JAVA_OPTIONS -Djava.util.logging.manager=org.jboss.logmanager.LogManager
//JAVA_OPTIONS -Dquarkus.banner.enabled=false
//JAVA_OPTIONS -Dquarkus.log.level=OFF

package com.someth2say;

import static com.fasterxml.jackson.databind.SerializationFeature.*;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.*;
import static java.util.Comparator.comparing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.github.yamlpath.YamlExpressionParser;
import io.github.yamlpath.YamlPath;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import io.quarkus.runtime.util.ExceptionUtil;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "yamlsort", mixinStandardHelpOptions = true, version = "1.0.0-SNAPSHOT")
public class YamlSort implements Runnable {

	public static class YamlSortOptions {

		public YamlSortOptions() { };
		public YamlSortOptions(final List<String> documentComparatorYPaths) {
			this.documentComparatorYPaths = documentComparatorYPaths;
		}

		@Option(paramLabel = "<file>", names = {"-i",
				"--input"}, description = "The YAML file to sort documents from. If not provided, STDIN is used.")
		public YamlSortOptions withPath(Path path) {
			this.path = path;
			return this;
		}
		protected Path path;

		@Option(paramLabel = "<compareYamlPath>", description = "YamlPath expression to sort documents.", arity = "1..", names = {
				"-y", "-yamlpath", "--yamlpath", "-d", "--docs"}, split = ",")
		public YamlSortOptions withDocumentComparatorYPaths(List<String> documentComparatorYPaths) {
			this.documentComparatorYPaths = documentComparatorYPaths;
			return this;
		}
		protected List<String> documentComparatorYPaths;

		@Option(paramLabel = "<selectYamlPath @@ compareYamlPath>", description = """
				Expression to sort lists of Maps.
				This expression must be formatted as two YamlPath expressions, separated by the string `@@`.
				For clarity, you can add a blank space before of after the separator string: ` @@ `.
				""", arity = "1..", names = {"-m", "--map",
				"--maps"}, split = ",", converter = MapListSortOptionConverter.class)
		public YamlSortOptions withMapListSortOptions(List<MapListSortOption> listSortOptions) {
			this.mapListSortOptions = listSortOptions;
			return this;
		}
		protected List<MapListSortOption> mapListSortOptions;

		@Option(paramLabel = "<selectYamlPath>", description = """
				YamlPath expression to select lists of to be sorted in natural order.
				List must be composed of scalar objects (strings, integers, floats...)
				""", arity = "1..", names = {"-l", "--list", "--lists"}, split = ",")
		public YamlSortOptions withScalarListSortOptions(List<String> scalarListSortOptions) {
			this.scalarListSortOptions = scalarListSortOptions;
			return this;
		}
		protected List<String> scalarListSortOptions;

		@Option(description = "Sort document keys.", names = {"--sortKeys", "-k"}, negatable = true)
		public YamlSortOptions withSortKeys(boolean sortKeys) {
			this.sortKeys = sortKeys;
			return this;
		}
		private boolean sortKeys = true;
	}

	@CommandLine.Mixin
	YamlSortOptions yamlSortOptions;

	@Override
	public void run() {
		try {
			// Split the input file in documents
			String result = yamlSortOptions.path != null
					? sortYamlFile(yamlSortOptions.path, yamlSortOptions)
					: sortYamlSystemIn(yamlSortOptions);
			System.out.println(result);
		} catch (RuntimeException|IOException e) {
			System.err.println("Unable to read Yaml Documents. Maybe it is malformed?");
			System.err.println(ExceptionUtil.getRootCause(e).getMessage());
		}
	}

	private String sortYamlSystemIn(YamlSortOptions yamlSortOptions) throws IOException {
		System.err.println("Using StdIn input. Press ^D to finish or ^C to cancel.");
		return sortYamlStream(System.in, yamlSortOptions);
	}

	public static String sortYamlFile(Path path, YamlSortOptions yamlSortOptions) throws IOException {
		String input = Files.readString(path);
		return sortDocuments(input, yamlSortOptions);
	}

	public static String sortYamlStream(InputStream stream, YamlSortOptions yamlSortOptions) throws IOException {
		String input = new String(stream.readAllBytes());
		return sortDocuments(input, yamlSortOptions);
	}

	public static String sortDocuments(String input, YamlSortOptions yamlSortOptions) {
		String[] docs = input.split("[" + System.lineSeparator() + "^]---" + System.lineSeparator());
		List<YamlExpressionParser> yamlDocs = Arrays.stream(docs).map(YamlPath::from).collect(Collectors.toList());

		// Sort documents
		if (yamlSortOptions.documentComparatorYPaths != null) {
			reversedCopy(yamlSortOptions.documentComparatorYPaths)
					.forEach(yamlPath -> yamlDocs.sort(comparing(asFunction(yamlPath))));
		}

		// Sort arrays of maps
		if (yamlSortOptions.mapListSortOptions != null) {
			reversedCopy(yamlSortOptions.mapListSortOptions)
					.forEach(opts -> yamlDocs.forEach(yamlDoc -> sortMapListsInYamlDoc(yamlDoc, opts)));
		}

		// Sort arrays of Comparable
		if (yamlSortOptions.scalarListSortOptions != null) {
			reversedCopy(yamlSortOptions.scalarListSortOptions)
					.forEach(opts -> yamlDocs.forEach(yamlDoc -> sortScalarListsInYamlDoc(yamlDoc, opts)));
		}
		// Sort document keys
		String sortedDocuments = sortDocumentKeys(yamlDocs, yamlSortOptions);

		return sortedDocuments;
	}

	private static <T> Deque<T> reversedCopy(List<T> list) {
		return list.stream().collect(Collector.of(ArrayDeque::new, ArrayDeque::addFirst, (d1, d2) -> {
			d2.addAll(d1);
			return d2;
		}));
	}

	private static String sortDocumentKeys(List<YamlExpressionParser> yamlDocs, YamlSortOptions yamlSortOptions) {
		ObjectMapper yamlMapper = yamlSortOptions.sortKeys ? getSortedYamlMapper() : getYamlMapper();
		String sortedDocuments = yamlDocs.stream().map(yamlDoc -> yalmDocToString(yamlDoc, yamlMapper))
				.collect(Collectors.joining());
		return sortedDocuments;
	}

	private static void sortMapListsInYamlDoc(YamlExpressionParser yamlDoc, MapListSortOption mapListSortOption) {
		Set<Object> selected = yamlDoc.read(mapListSortOption.listSelectorYPath);
		selected.forEach(obj -> {
			if (obj instanceof AbstractList<?> list) {
				if (list.stream().allMatch(Map.class::isInstance)) {
					sortListOfMaps(mapListSortOption, list);
					yamlDoc.write(mapListSortOption.listSelectorYPath, list);
				} else {
					System.err.println("The list selected by " + mapListSortOption.listSelectorYPath
							+ " is contains objects that are not maps.");
				}
			} else {
				// The object selected is not a list, so we can ignore it
				System.err.println("One object selected by " + mapListSortOption.listSelectorYPath + " is not a list.");
			}
		});
	}

	private static void sortScalarListsInYamlDoc(YamlExpressionParser yamlDoc, String scalarSortYPath) {
		Set<Object> selected = yamlDoc.read(scalarSortYPath);
		selected.forEach(obj -> {
			if (obj instanceof AbstractList<?> list) {
				if (list.stream().allMatch(Comparable.class::isInstance)) {
					sortListOfScalars((AbstractList<Comparable<?>>) list);
				} else {
					System.err.println(
							"The list selected by " + scalarSortYPath + " is contains objects that are not scalars.");
				}
			} else {
				// The object selected is not a list, so we can ignore it
				System.err.println("One object selected by " + scalarSortYPath + " is not a list.");
			}
		});
	}

	private static AbstractList<?> sortListOfMaps(MapListSortOption listSortOption, AbstractList<?> list) {
		list.sort(comparing(resource -> {
			List<Map<Object, Object>> resourcesList = List.of((Map<Object, Object>) resource);
			YamlExpressionParser yamlDoc = new YamlExpressionParser(resourcesList);
			return asFunction(listSortOption.listComparatorYPath).apply(yamlDoc);
		}));

		return list;
	}

	private static void sortListOfScalars(AbstractList<? extends Comparable> list) {
		list.sort(Comparator.naturalOrder());
	}

	private static Function<YamlExpressionParser, String> asFunction(String yamlPath) {
		return (YamlExpressionParser yamlDoc) -> {
			Object value = yamlDoc.readSingle(yamlPath);
			return value != null ? value.toString() : "";
		};
	}

	public static String yalmDocToString(YamlExpressionParser yamlDoc, ObjectMapper yamlMapper) {
		try {

			List<Map<Object, Object>> resources = yamlDoc.getResources();
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
			new YAMLGenerator.Feature[]{MINIMIZE_QUOTES, ALWAYS_QUOTE_NUMBERS_AS_STRINGS, INDENT_ARRAYS_WITH_INDICATOR},
			new SerializationFeature[]{INDENT_OUTPUT, ORDER_MAP_ENTRIES_BY_KEYS},
			new SerializationFeature[]{WRITE_NULL_MAP_VALUES, WRITE_EMPTY_JSON_ARRAYS});

	private static final ObjectMapper YAML_MAPPER = createYamlMapper(
			new YAMLGenerator.Feature[]{MINIMIZE_QUOTES, ALWAYS_QUOTE_NUMBERS_AS_STRINGS, INDENT_ARRAYS_WITH_INDICATOR},
			new SerializationFeature[]{INDENT_OUTPUT},
			new SerializationFeature[]{WRITE_NULL_MAP_VALUES, WRITE_EMPTY_JSON_ARRAYS});

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

	public record MapListSortOption(String listSelectorYPath, String listComparatorYPath) {
		public static final String SEPARATOR_REGEX = " ?@@ ?";
	}

	public static class MapListSortOptionConverter implements CommandLine.ITypeConverter<MapListSortOption> {
		public MapListSortOption convert(String string) {
			String[] split = string.split(MapListSortOption.SEPARATOR_REGEX);
			if (split.length == 2) {
				return new MapListSortOption(split[0], split[1]);
			}

			throw new RuntimeException("Unable to parse map list sort option.");
		}
	}
}
