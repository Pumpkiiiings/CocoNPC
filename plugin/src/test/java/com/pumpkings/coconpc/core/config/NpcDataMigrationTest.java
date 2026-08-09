package com.pumpkings.coconpc.core.config;

import com.pumpkings.coconpc.CocoNPC;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * The migration runs once, on someone else's server, against data this project has never
 * seen. If it drops a field there is no second chance, so the conversion is pinned here.
 */
@DisplayName("Migration from the two-file layout")
class NpcDataMigrationTest {

    @TempDir
    Path dataFolder;

    private File npcFolder;
    private CocoNPC plugin;

    @BeforeEach
    void setUp() {
        npcFolder = new File(dataFolder.toFile(), "npcs");
        assertThat(npcFolder.mkdirs()).isTrue();

        plugin = mock(CocoNPC.class);
        lenient().when(plugin.getDataFolder()).thenReturn(dataFolder.toFile());
        lenient().when(plugin.getLogger()).thenReturn(Logger.getLogger("CocoNPCTest"));
    }

    private void writeOldData(String contents) throws IOException {
        Files.writeString(dataFolder.resolve("CocoNPC.data"), contents, StandardCharsets.UTF_8);
    }

    private void writeOldActions(String contents) throws IOException {
        Files.writeString(dataFolder.resolve("npcs.yml"), contents, StandardCharsets.UTF_8);
    }

    private YamlConfiguration migrateAndLoad(int id) {
        new NpcDataMigration(plugin, npcFolder).runIfNeeded();
        return YamlConfiguration.loadConfiguration(new File(npcFolder, id + ".yml"));
    }

    private static final String ONE_NPC = """
            id_count: 1
            data:
              3f2b8c1a-1111-2222-3333-444455556666:
                id: 7
                size: 1.25
                hologram:
                - '<gold>GUARD'
                - '<gray>line two'
                hologram_billboard: VERTICAL
                hologram_shadow: false
                hologram_bg: '#80000000'
                location:
                  world: world
                  x: 10.5
                  y: -60.0
                  z: 8.5
                  yaw: 180.0
                  pitch: 0.0
                skin:
                  head: aGVhZA==
                  body1: Ym9keTE=
                parts:
                  right_arm1:
                    pitch: -30.0
                    yaw: 5.0
                    roll: 2.5
                    offset_x: 0.1
                    offset_y: 0.2
                    offset_z: 0.3
                    hidden: true
            """;

    private static final String ONE_ACTION_LIST = """
            npc:
              7:
                uuid: 3f2b8c1a-1111-2222-3333-444455556666
                actions:
                - '[require_money] 500'
                - '[message] hello'
            """;

    @Test
    @DisplayName("writes one file named after the numeric id")
    void writesFileNamedAfterId() throws IOException {
        writeOldData(ONE_NPC);
        writeOldActions(ONE_ACTION_LIST);

        new NpcDataMigration(plugin, npcFolder).runIfNeeded();

        assertThat(new File(npcFolder, "7.yml")).exists();
    }

    @Test
    @DisplayName("keeps identity, size and location")
    void keepsCoreFields() throws IOException {
        writeOldData(ONE_NPC);
        writeOldActions(ONE_ACTION_LIST);
        YamlConfiguration out = migrateAndLoad(7);

        assertThat(out.getString("uuid")).isEqualTo("3f2b8c1a-1111-2222-3333-444455556666");
        assertThat(out.getInt("id")).isEqualTo(7);
        assertThat(out.getDouble("size")).isEqualTo(1.25);
        assertThat(out.getString("location.world")).isEqualTo("world");
        assertThat(out.getDouble("location.x")).isEqualTo(10.5);
        assertThat(out.getDouble("location.yaw")).isEqualTo(180.0);
    }

    @Test
    @DisplayName("keeps every skin texture")
    void keepsSkin() throws IOException {
        writeOldData(ONE_NPC);
        writeOldActions(ONE_ACTION_LIST);
        YamlConfiguration out = migrateAndLoad(7);

        assertThat(out.getString("skin.head")).isEqualTo("aGVhZA==");
        assertThat(out.getString("skin.body1")).isEqualTo("Ym9keTE=");
    }

    @Test
    @DisplayName("keeps part rotations, offsets and visibility")
    void keepsParts() throws IOException {
        writeOldData(ONE_NPC);
        writeOldActions(ONE_ACTION_LIST);
        YamlConfiguration out = migrateAndLoad(7);

        assertThat(out.getDouble("parts.right_arm1.pitch")).isEqualTo(-30.0);
        assertThat(out.getDouble("parts.right_arm1.yaw")).isEqualTo(5.0);
        assertThat(out.getDouble("parts.right_arm1.roll")).isEqualTo(2.5);
        assertThat(out.getDouble("parts.right_arm1.offset_x")).isEqualTo(0.1);
        assertThat(out.getDouble("parts.right_arm1.offset_z")).isEqualTo(0.3);
        assertThat(out.getBoolean("parts.right_arm1.hidden")).isTrue();
    }

    @Test
    @DisplayName("moves the flat hologram keys into a nested block")
    void reshapesHologram() throws IOException {
        writeOldData(ONE_NPC);
        writeOldActions(ONE_ACTION_LIST);
        YamlConfiguration out = migrateAndLoad(7);

        assertThat(out.getStringList("hologram.lines"))
                .containsExactly("<gold>GUARD", "<gray>line two");
        assertThat(out.getString("hologram.billboard")).isEqualTo("VERTICAL");
        assertThat(out.getBoolean("hologram.shadow")).isFalse();
        assertThat(out.getString("hologram.background")).isEqualTo("#80000000");
    }

    @Test
    @DisplayName("pulls actions across from the separate id-keyed file")
    void movesActions() throws IOException {
        writeOldData(ONE_NPC);
        writeOldActions(ONE_ACTION_LIST);
        YamlConfiguration out = migrateAndLoad(7);

        // Actions lived in npcs.yml keyed by id, not in CocoNPC.data keyed by uuid.
        assertThat(out.getStringList("actions"))
                .containsExactly("[require_money] 500", "[message] hello");
    }

    @Test
    @DisplayName("renames the originals to .bak instead of deleting them")
    void backsUpTheOriginals() throws IOException {
        writeOldData(ONE_NPC);
        writeOldActions(ONE_ACTION_LIST);

        new NpcDataMigration(plugin, npcFolder).runIfNeeded();

        assertThat(dataFolder.resolve("CocoNPC.data.bak")).exists();
        assertThat(dataFolder.resolve("npcs.yml.bak")).exists();
        assertThat(dataFolder.resolve("CocoNPC.data")).doesNotExist();
    }

    @Test
    @DisplayName("does nothing on a fresh install with no old files")
    void noOldFilesIsANoOp() {
        new NpcDataMigration(plugin, npcFolder).runIfNeeded();

        assertThat(npcFolder.listFiles()).isEmpty();
    }

    @Test
    @DisplayName("does not run again once npcs/ already holds data")
    void doesNotRunTwice() throws IOException {
        writeOldData(ONE_NPC);
        writeOldActions(ONE_ACTION_LIST);
        new NpcDataMigration(plugin, npcFolder).runIfNeeded();

        // Simulate the old file reappearing next to already-migrated data.
        writeOldData(ONE_NPC);
        Files.writeString(new File(npcFolder, "7.yml").toPath(), "uuid: sentinel\n");

        new NpcDataMigration(plugin, npcFolder).runIfNeeded();

        // The converted file must be left exactly as it was, not overwritten.
        assertThat(Files.readString(new File(npcFolder, "7.yml").toPath()))
                .contains("sentinel");
    }

    @Test
    @DisplayName("skips an NPC with no usable id rather than aborting the whole run")
    void skipsUnusableEntries() throws IOException {
        writeOldData("""
                data:
                  3f2b8c1a-1111-2222-3333-444455556666:
                    size: 1.0
                  4a2b8c1a-1111-2222-3333-444455556666:
                    id: 3
                    size: 2.0
                """);
        writeOldActions("npc: {}\n");

        new NpcDataMigration(plugin, npcFolder).runIfNeeded();

        // The entry without an id is dropped; the valid one still lands.
        assertThat(new File(npcFolder, "3.yml")).exists();
        assertThat(npcFolder.listFiles()).hasSize(1);
    }

    @Test
    @DisplayName("survives an old file that has no NPCs at all")
    void handlesEmptyOldFile() throws IOException {
        writeOldData("id_count: 0\n");
        writeOldActions("npc: {}\n");

        new NpcDataMigration(plugin, npcFolder).runIfNeeded();

        assertThat(npcFolder.listFiles()).isEmpty();
        assertThat(dataFolder.resolve("CocoNPC.data.bak")).exists();
    }
}
