package someth2say;

import com.someth2say.YamlSort;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class YamlSortTest {

    @org.junit.jupiter.api.Test
    void sortYamlString() throws IOException {
        try(InputStream yamlIS = this.getClass().getClassLoader().getResourceAsStream("simple.yml")) {
            assertNotNull(yamlIS);
            String yaml = new Scanner(yamlIS, StandardCharsets.UTF_8).useDelimiter("\\A").next();
            String result = YamlSort.sortYamlString(yaml, List.of("kind"), true);
            System.out.println(result);
        }
    }
}