package io.github.kycguard.generator;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.kycguard.common.util.JsonUtils;
import io.github.kycguard.generator.model.SyntheticCustomer;
import io.github.kycguard.generator.model.WatchlistEntry;
import io.github.kycguard.generator.service.CustomerGenerator;
import io.github.kycguard.generator.service.WatchlistGenerator;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataGeneratorCli {

    private final WatchlistGenerator watchlistGenerator;
    private final CustomerGenerator customerGenerator;

    public DataGeneratorCli() {
        this(new WatchlistGenerator(), new CustomerGenerator());
    }

    public DataGeneratorCli(WatchlistGenerator watchlistGenerator, CustomerGenerator customerGenerator) {
        this.watchlistGenerator = watchlistGenerator;
        this.customerGenerator = customerGenerator;
    }

    public static void main(String[] args) {
        int exitCode = run(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    public static int run(String... args) {
        return new DataGeneratorCli().execute(args);
    }

    public int execute(String... args) {
        if (args == null || args.length == 0 || isHelpRequested(args)) {
            printUsage();
            return (args != null && args.length > 0 && isHelpRequested(args)) ? 0 : 1;
        }

        String command = args[0].toLowerCase();
        Map<String, String> options = parseOptions(args, 1);

        try {
            return switch (command) {
                case "watchlist" -> handleWatchlistCommand(options);
                case "customers" -> handleCustomersCommand(options);
                default -> {
                    System.err.println("Unknown command: '" + command + "'. Available commands: watchlist, customers");
                    printUsage();
                    yield 1;
                }
            };
        } catch (Exception e) {
            System.err.println("Error executing command '" + command + "': " + e.getMessage());
            return 1;
        }
    }

    private int handleWatchlistCommand(Map<String, String> options) throws IOException {
        int count = Integer.parseInt(options.getOrDefault("count", "1000"));
        long seed = Long.parseLong(options.getOrDefault("seed", "42"));
        String outputPath = options.get("output");

        if (count <= 0) {
            System.err.println("Error: --count must be a positive integer.");
            return 1;
        }

        List<WatchlistEntry> entries = watchlistGenerator.generate(count, seed);

        if (outputPath != null && !outputPath.isBlank() && !"-".equals(outputPath)) {
            writeToFile(entries, outputPath);
            System.err.printf("Successfully generated %d watchlist entries to %s (seed: %d)%n", count, outputPath, seed);
        } else {
            System.out.println(JsonUtils.mapper().writerWithDefaultPrettyPrinter().writeValueAsString(entries));
        }

        return 0;
    }

    private int handleCustomersCommand(Map<String, String> options) throws IOException {
        int count = Integer.parseInt(options.getOrDefault("count", "1000"));
        double riskyRatio = Double.parseDouble(options.getOrDefault("risky-ratio", "0.03"));
        long seed = Long.parseLong(options.getOrDefault("seed", "42"));
        String watchlistPath = options.get("watchlist");
        String outputPath = options.get("output");

        if (count <= 0) {
            System.err.println("Error: --count must be a positive integer.");
            return 1;
        }
        if (riskyRatio < 0.0 || riskyRatio > 1.0) {
            System.err.println("Error: --risky-ratio must be between 0.0 and 1.0.");
            return 1;
        }

        List<WatchlistEntry> watchlist = null;
        if (watchlistPath != null && !watchlistPath.isBlank()) {
            File watchlistFile = new File(watchlistPath);
            if (!watchlistFile.exists()) {
                System.err.println("Error: Watchlist file does not exist: " + watchlistPath);
                return 1;
            }
            watchlist = JsonUtils.mapper().readValue(watchlistFile, new TypeReference<List<WatchlistEntry>>() {});
        }

        List<SyntheticCustomer> customers = customerGenerator.generate(count, riskyRatio, watchlist, seed);

        if (outputPath != null && !outputPath.isBlank() && !"-".equals(outputPath)) {
            writeToFile(customers, outputPath);
            System.err.printf("Successfully generated %d customers (%d risky) to %s (seed: %d)%n",
                    count, Math.round(count * riskyRatio), outputPath, seed);
        } else {
            System.out.println(JsonUtils.mapper().writerWithDefaultPrettyPrinter().writeValueAsString(customers));
        }

        return 0;
    }

    private void writeToFile(Object data, String outputPath) throws IOException {
        Path path = Path.of(outputPath);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        JsonUtils.mapper().writerWithDefaultPrettyPrinter().writeValue(path.toFile(), data);
    }

    private static boolean isHelpRequested(String[] args) {
        for (String arg : args) {
            if ("--help".equalsIgnoreCase(arg) || "-h".equalsIgnoreCase(arg) || "help".equalsIgnoreCase(arg)) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, String> parseOptions(String[] args, int startIndex) {
        Map<String, String> options = new HashMap<>();
        for (int i = startIndex; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--")) {
                String key = arg.substring(2);
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    options.put(key, args[i + 1]);
                    i++;
                } else {
                    options.put(key, "true");
                }
            }
        }
        return options;
    }

    private static void printUsage() {
        System.out.println("KYC-Guard Synthetic Data Generator CLI");
        System.out.println("Usage:");
        System.out.println("  watchlist --count <N> [--seed <S>] [--output <FILE>]");
        System.out.println("  customers --count <N> [--risky-ratio <R>] [--seed <S>] [--watchlist <FILE>] [--output <FILE>]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --count <N>         Number of records to generate (default: 1000)");
        System.out.println("  --seed <S>          Fixed random seed for deterministic generation (default: 42)");
        System.out.println("  --risky-ratio <R>   Ratio of risky customers linked to watchlist [0.0 - 1.0] (default: 0.03)");
        System.out.println("  --watchlist <FILE>  Path to existing watchlist JSON file to sample from");
        System.out.println("  --output <FILE>     Output path for formatted JSON (omit for stdout)");
        System.out.println("  --help, -h          Display this help message");
    }
}
