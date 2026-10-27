package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MainTest {

    @Test
    void requiresInputPathForRegularCommandLineInvocation() {
        PrintStream originalError = System.err;
        ByteArrayOutputStream capturedError = new ByteArrayOutputStream();
        try (PrintStream replacementError = new PrintStream(capturedError, true, StandardCharsets.UTF_8)) {
            System.setErr(replacementError);
            assertEquals(2, Main.run(new String[0]));
        } finally {
            System.setErr(originalError);
        }

        assertTrue(capturedError.toString(StandardCharsets.UTF_8)
                .contains("потрібно вказати вхідний файл через --input"));
    }

    @Test
    void returnsStatusCodesForHelpVersionAndInputFileErrors() throws Exception {
        assertEquals(0, Main.run(new String[] {"--help"}));
        assertEquals(0, Main.run(new String[] {"--version"}));

        Path missingInput = Files.createTempDirectory("main-test").resolve("missing.csv");
        assertEquals(1, Main.run(new String[] {"--input", missingInput.toString()}));
    }

    @Test
    void processesInputFileAndWritesReport() throws Exception {
        Path directory = Files.createTempDirectory("main-test");
        Path input = directory.resolve("input.csv");
        Path output = directory.resolve("out").resolve("report.txt");
        Files.writeString(input, "Хвойні;Туя;120.5;450.0;7", StandardCharsets.UTF_8);

        assertEquals(0, Main.run(new String[] {"--input", input.toString(), "--output", output.toString()}));
        assertTrue(Files.readString(output, StandardCharsets.UTF_8).contains("Коректних записів: 1"));
    }
}
