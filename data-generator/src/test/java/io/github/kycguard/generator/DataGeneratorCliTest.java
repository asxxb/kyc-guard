package io.github.kycguard.generator;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.kycguard.common.util.JsonUtils;
import io.github.kycguard.generator.model.SyntheticCustomer;
import io.github.kycguard.generator.model.WatchlistEntry;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DataGeneratorCliTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("CLI generates watchlist file successfully")
    void testWatchlistCommandWithOutput() throws Exception {
        Path outputFile = tempDir.resolve("test-watchlist.json");

        int exitCode = DataGeneratorCli.run(
                "watchlist",
                "--count", "50",
                "--seed", "123",
                "--output", outputFile.toString()
        );

        assertThat(exitCode).isZero();
        assertThat(outputFile).exists();

        List<WatchlistEntry> entries = JsonUtils.mapper().readValue(
                outputFile.toFile(),
                new TypeReference<List<WatchlistEntry>>() {}
        );

        assertThat(entries).hasSize(50);
        assertThat(entries.get(0).entryId()).isEqualTo("WL-000001");
    }

    @Test
    @DisplayName("CLI generates customer file referencing external watchlist")
    void testCustomersCommandWithWatchlistAndOutput() throws Exception {
        Path watchlistFile = tempDir.resolve("test-watchlist.json");
        Path customerFile = tempDir.resolve("test-customers.json");

        // 1. Generate watchlist
        int wlCode = DataGeneratorCli.run(
                "watchlist",
                "--count", "20",
                "--seed", "42",
                "--output", watchlistFile.toString()
        );
        assertThat(wlCode).isZero();

        // 2. Generate customers
        int custCode = DataGeneratorCli.run(
                "customers",
                "--count", "100",
                "--risky-ratio", "0.1",
                "--seed", "42",
                "--watchlist", watchlistFile.toString(),
                "--output", customerFile.toString()
        );

        assertThat(custCode).isZero();
        assertThat(customerFile).exists();

        List<SyntheticCustomer> customers = JsonUtils.mapper().readValue(
                customerFile.toFile(),
                new TypeReference<List<SyntheticCustomer>>() {}
        );

        assertThat(customers).hasSize(100);
        long riskyCount = customers.stream().filter(SyntheticCustomer::isRisky).count();
        assertThat(riskyCount).isEqualTo(10);
    }

    @Test
    @DisplayName("CLI generates customers without external watchlist (uses fallback)")
    void testCustomersCommandWithoutWatchlistOption() throws Exception {
        Path customerFile = tempDir.resolve("customers-fallback.json");

        int exitCode = DataGeneratorCli.run(
                "customers",
                "--count", "30",
                "--risky-ratio", "0.2",
                "--output", customerFile.toString()
        );

        assertThat(exitCode).isZero();
        assertThat(customerFile).exists();

        List<SyntheticCustomer> customers = JsonUtils.mapper().readValue(
                customerFile.toFile(),
                new TypeReference<List<SyntheticCustomer>>() {}
        );

        assertThat(customers).hasSize(30);
    }

    @Test
    @DisplayName("CLI outputs to stdout when --output is omitted")
    void testOutputToStdoutWhenNoOutputSpecified() {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(outContent));

            int exitCode = DataGeneratorCli.run(
                    "watchlist",
                    "--count", "5",
                    "--seed", "99"
            );

            assertThat(exitCode).isZero();
            String output = outContent.toString();
            assertThat(output).contains("WL-000001");
            assertThat(output).contains("WL-000005");
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    @DisplayName("CLI displays help and handles invalid inputs gracefully")
    void testHelpAndErrorHandling() {
        // No args -> error and usage
        assertThat(DataGeneratorCli.run()).isEqualTo(1);

        // Help flag -> success and usage
        assertThat(DataGeneratorCli.run("--help")).isEqualTo(0);
        assertThat(DataGeneratorCli.run("-h")).isEqualTo(0);
        assertThat(DataGeneratorCli.run("help")).isEqualTo(0);

        // Unknown command
        assertThat(DataGeneratorCli.run("unknown")).isEqualTo(1);

        // Invalid count
        assertThat(DataGeneratorCli.run("watchlist", "--count", "invalid")).isEqualTo(1);
        assertThat(DataGeneratorCli.run("watchlist", "--count", "-5")).isEqualTo(1);

        // Invalid risky-ratio
        assertThat(DataGeneratorCli.run("customers", "--risky-ratio", "2.5")).isEqualTo(1);
        assertThat(DataGeneratorCli.run("customers", "--count", "-10")).isEqualTo(1);

        // Non-existent watchlist file
        assertThat(DataGeneratorCli.run("customers", "--watchlist", "/nonexistent/path/wl.json")).isEqualTo(1);
    }
}
