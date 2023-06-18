package com.someth2say;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import picocli.CommandLine;

class MapListSortOptionConverterTest {

	@org.junit.jupiter.api.Test
	void convertWithoutSpaces() {
		YamlSort.MapListSortOptionConverter converter = new YamlSort.MapListSortOptionConverter();
		YamlSort.MapListSortOption convert = converter.convert("pets@@name");

		assertEquals("pets", convert.listSelectorYPath());
		assertEquals("name", convert.listComparatorYPath());
	}

	@org.junit.jupiter.api.Test
	void convertWithSpaces() {
		YamlSort.MapListSortOptionConverter converter = new YamlSort.MapListSortOptionConverter();
		YamlSort.MapListSortOption convert = converter.convert("pets @@ name");

		assertEquals("pets", convert.listSelectorYPath());
		assertEquals("name", convert.listComparatorYPath());
	}

	@org.junit.jupiter.api.Test
	void picocliParse() {
		YamlSort app = new YamlSort();
		CommandLine cmd = new CommandLine(app);

		cmd.parseArgs("--map", "pets@@name");
		List<YamlSort.MapListSortOption> mapListSortOptions = app.yamlSortOptions.mapListSortOptions;
		assertEquals(1, mapListSortOptions.size());
		assertEquals("pets", mapListSortOptions.get(0).listSelectorYPath());
		assertEquals("name", mapListSortOptions.get(0).listComparatorYPath());

	}

}
