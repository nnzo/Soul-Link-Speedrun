package net.zenzty.soullink.server.run;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.loader.api.FabricLoader;
import net.zenzty.soullink.SoulLink;

/**
 * Manages the speedrun seed pool file (config/soullink_seeds.txt).
 * Allows loading pre-configured seeds and selecting random seeds for runs.
 */
public final class SeedPoolManager {

    private static final String SEED_POOL_FILE = "soullink_seeds.txt";
    private static final SeedPoolManager INSTANCE = new SeedPoolManager();

    private SeedPoolManager() {}

    public static SeedPoolManager getInstance() {
        return INSTANCE;
    }

    /**
     * Resolves the path to the seed pool file, preferring the config folder.
     */
    public Path getSeedPoolPath() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(SEED_POOL_FILE);
        if (Files.isRegularFile(configPath)) {
            return configPath;
        }
        Path rootPath = Path.of(SEED_POOL_FILE);
        if (Files.isRegularFile(rootPath)) {
            return rootPath;
        }
        return configPath;
    }

    /**
     * Creates a template seed pool file if none exists.
     */
    public void ensureFileExists() {
        Path path = getSeedPoolPath();
        if (Files.exists(path)) {
            return;
        }

        try {
            Files.createDirectories(path.getParent());
            String defaultContent =
                    """
                # Soul Link Speedrun - Seed Pool
                # Add one seed per line. Lines starting with # and blank lines are ignored.
                # Seeds can be numbers (e.g. -1234567890) or text (e.g. speedrun).
                # Inline comments starting with # are also supported.
                #
                # Example seeds (remove '#' to activate):
                # 248336465499698767 # Village and ruined portal at spawn
                # -4534752251090623692
                """;
            Files.writeString(path, defaultContent, StandardCharsets.UTF_8);
            SoulLink.LOGGER.info("Created default seed pool file at {}", path);
        } catch (IOException e) {
            SoulLink.LOGGER.warn("Could not create seed pool file {}: {}", path, e.getMessage());
        }
    }

    /**
     * Loads raw seed lines from the seed pool file.
     */
    public List<String> loadRawSeeds() {
        Path path = getSeedPoolPath();
        ensureFileExists();

        List<String> seeds = new ArrayList<>();
        if (!Files.isRegularFile(path)) {
            return seeds;
        }

        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int commentIndex = trimmed.indexOf('#');
                if (commentIndex != -1) {
                    trimmed = trimmed.substring(0, commentIndex).trim();
                }
                if (!trimmed.isEmpty()) {
                    seeds.add(trimmed);
                }
            }
        } catch (IOException e) {
            SoulLink.LOGGER.warn("Could not read seed pool file {}: {}", path, e.getMessage());
        }

        return seeds;
    }

    /**
     * Picks a random seed from the seed pool file and parses it.
     */
    public OptionalLong getRandomSeed() {
        List<String> seeds = loadRawSeeds();
        if (seeds.isEmpty()) {
            return OptionalLong.empty();
        }

        int index = ThreadLocalRandom.current().nextInt(seeds.size());
        String selected = seeds.get(index);
        return OptionalLong.of(parseSeed(selected));
    }

    /**
     * Parses a string representation of a seed into a long value.
     * Supports 64-bit numerical seeds and alphanumeric text seeds.
     */
    public static long parseSeed(String input) {
        if (input == null) {
            return 0L;
        }
        String trimmed = input.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }
        try {
            return Long.parseLong(trimmed);
        } catch (NumberFormatException e) {
            return (long) trimmed.hashCode();
        }
    }
}
