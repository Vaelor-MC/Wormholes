package art.arcane.wormholes.config.toml;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ClientViewConfigTest {
    @Test
    void meshDepthFollowsTheClientUnderTheCap() {
        ClientViewConfig config = new ClientViewConfig();
        assertEquals(8, config.meshMaxDistanceChunks);
        assertEquals(128, config.meshDistanceBlocks(32));
        assertEquals(128, config.meshDistanceBlocks(8));
        assertEquals(64, config.meshDistanceBlocks(4));
        assertEquals(32, config.meshDistanceBlocks(1));
    }

    @Test
    void capIsClampedToTheSupportedRange() {
        ClientViewConfig config = new ClientViewConfig();
        config.meshMaxDistanceChunks = 64;
        config.normalizeRuntimeBounds();
        assertEquals(ClientViewConfig.MAX_MESH_DISTANCE_CHUNKS, config.meshMaxDistanceChunks);
        assertEquals(512, config.meshDistanceBlocks(32));

        config.meshMaxDistanceChunks = 0;
        assertEquals(32, config.meshDistanceBlocks(32));
        config.normalizeRuntimeBounds();
        assertEquals(ClientViewConfig.MIN_MESH_DISTANCE_CHUNKS, config.meshMaxDistanceChunks);
    }
}
