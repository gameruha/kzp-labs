package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ArgumentParserTest {

    @Test
    void parsesInputAndOutputPaths() {
        CommandLineOptions options = ArgumentParser.parse(
                new String[] {"--input", "a.csv", "--output", "b.txt"});

        assertEquals(Path.of("a.csv"), options.input());
        assertEquals(Path.of("b.txt"), options.output());
        assertFalse(options.help());
        assertFalse(options.version());
    }

    @Test
    void usesDefaultOutputPath() {
        CommandLineOptions options = ArgumentParser.parse(new String[] {"--input", "a.csv"});

        assertEquals(Path.of("out", "report.txt"), options.output());
    }

    @Test
    void rejectsUnknownAndIncompleteArguments() {
        assertThrows(IllegalArgumentException.class, () -> ArgumentParser.parse(new String[] {"--bad"}));
        assertThrows(IllegalArgumentException.class, () -> ArgumentParser.parse(new String[] {"--input"}));
        assertThrows(IllegalArgumentException.class, () -> ArgumentParser.parse(new String[0]));
    }

    @Test
    void helpAndVersionDoNotRequireInput() {
        assertTrue(ArgumentParser.parse(new String[] {"--help"}).help());
        assertTrue(ArgumentParser.parse(new String[] {"--version"}).version());
    }
}
