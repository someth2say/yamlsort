package com.someth2say;

import static com.someth2say.YamlSort.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;

class YamlSortTest {

	@org.junit.jupiter.api.Test
	void sortKeys() throws IOException {
		String result = sortDocuments("""
				---
				one: 1
				two: 2
				three: 3
				""", new YamlSortOptions());
		assertEquals("""
				---
				one: 1
				three: 3
				two: 2
				""", result);
	}

	@org.junit.jupiter.api.Test
	void sortKeysInMaps() throws IOException {
		String result = sortDocuments("""
				---
				numbers:
				  one: 1
				  two: 2
				  three: 3
				""", new YamlSortOptions());
		assertEquals("""
				---
				numbers:
				  one: 1
				  three: 3
				  two: 2
				""", result);
	}

	@org.junit.jupiter.api.Test
	void noSortKeys() throws IOException {
		String result = sortDocuments("""
				---
				one: 1
				two: 2
				three: 3
				""", new YamlSortOptions().withSortKeys(false));
		assertEquals("""
				---
				one: 1
				two: 2
				three: 3
				""", result);
	}

	@org.junit.jupiter.api.Test
	void sortDocumentsByAttribute() throws IOException {
		String result = sortDocuments("""
				---
				kind: pets
				description: Animals in the house
				pets:
				  - name: Sharona
				    species: cat
				  - species: cat
				    name: Azabache
				---
				kind: people
				description: Humans in the house
				people:
				  - name: Jordi
				    gender: male
				  - name: Cristina
				    gender: female
				""", new YamlSortOptions(List.of("kind")));
		assertEquals("""
				---
				description: Humans in the house
				kind: people
				people:
				  - gender: male
				    name: Jordi
				  - gender: female
				    name: Cristina
				---
				description: Animals in the house
				kind: pets
				pets:
				  - name: Sharona
				    species: cat
				  - name: Azabache
				    species: cat
				            """, result);
	}

	@org.junit.jupiter.api.Test
	void sortArrayOfString() throws IOException {
		String result = sortDocuments("""
				---
				numbers:
				  - one
				  - two
				  - three
				""", new YamlSortOptions().withScalarListSortOptions(List.of("numbers")));
		assertEquals("""
				---
				numbers:
				  - one
				  - three
				  - two
				""", result);
	}

	@org.junit.jupiter.api.Test
	void sortArrayOfIntegers() throws IOException {
		String result = sortDocuments("""
				---
				numbers:
				  - 3
				  - 1
				  - 2
				""", new YamlSortOptions().withScalarListSortOptions(List.of("numbers")));
		assertEquals("""
				---
				numbers:
				  - 1
				  - 2
				  - 3
				""", result);
	}

	@org.junit.jupiter.api.Test
	void sortArrayOfFloats() throws IOException {
		String result = sortDocuments("""
				---
				numbers:
				  - 0.3
				  - 0.1
				  - 0.2
				""", new YamlSortOptions().withScalarListSortOptions(List.of("numbers")));
		assertEquals("""
				---
				numbers:
				  - 0.1
				  - 0.2
				  - 0.3
				""", result);
	}

	@org.junit.jupiter.api.Test
	void sortArrayOfMaps() throws IOException {
		String result = sortDocuments("""
				---
				pets:
				  - name: Sharona
				    species: cat
				  - name: Azabache
				    species: cat
				""", new YamlSortOptions().withMapListSortOptions(List.of(new MapListSortOption("pets", "name"))));
		assertEquals("""
				---
				pets:
				  - name: Azabache
				    species: cat
				  - name: Sharona
				    species: cat
				""", result);
	}

	@org.junit.jupiter.api.Test
	void sortArrayOfMapsDeepNested() throws IOException {
		String result = sortDocuments("""
				---
				house:
				  pets:
				    - cats:
				        - name: Sharona
				        - name: Azabache
				""", new YamlSortOptions().withMapListSortOptions(List.of(new MapListSortOption(".*.cats", "name"))));
		assertEquals("""
				---
				house:
				  pets:
				    - cats:
				        - name: Azabache
				        - name: Sharona
				""", result);
	}

	@org.junit.jupiter.api.Test
	void stableSortOfDocuments() throws IOException {
		String result = sortDocuments("""
				---
				name: a
				surname: a
				---
				name: b
				surname: a
				---
				name: a
				surname: b
				---
				name: b
				surname: b
				""", new YamlSortOptions(List.of("name")));
		assertEquals("""
				---
				name: a
				surname: a
				---
				name: a
				surname: b
				---
				name: b
				surname: a
				---
				name: b
				surname: b
				            """, result);
	}

	@org.junit.jupiter.api.Test
	void stableSortOfDocumentsWithMultipleComparators() throws IOException {
		String result = sortDocuments("""
				---
				name: a
				surname: a
				---
				name: b
				surname: b
				---
				name: a
				surname: b
				---
				name: b
				surname: a
				""", new YamlSortOptions(List.of("name", "surname")));
		assertEquals("""
				---
				name: a
				surname: a
				---
				name: a
				surname: b
				---
				name: b
				surname: a
				---
				name: b
				surname: b
				            """, result);
	}

	@org.junit.jupiter.api.Test
	void stableSortOfListOfMaps() throws IOException {
		String result = sortDocuments("""
				---
				numbers:
				  - Name: Two
				    Chars: 3
				  - Name: Three
				    Chars: 5
				  - Name: One
				    Chars: 3
				""", new YamlSortOptions().withSortKeys(false)
				.withMapListSortOptions(List.of(new YamlSort.MapListSortOption("numbers", "Chars"))));
		assertEquals("""
				---
				numbers:
				  - Name: Two
				    Chars: 3
				  - Name: One
				    Chars: 3
				  - Name: Three
				    Chars: 5
				            """, result);
	}

}