package art.arcane.wormholes.render.view;

import art.arcane.wormholes.network.view.EntityVisual;
import art.arcane.wormholes.network.view.RemoteViewCache;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class EntityPriorityTest {
    private static EntityVisual visual(String type, double x, double y, double z) {
        return EntityVisual.full(UUID.randomUUID(), type, x, y, z, 1.8D, 0.0D, 0.0D, 1.0D, 0.0F, 0.0F,
            0.0D, 0.0D, 0.0D, true, "", "", "", null, null, new byte[0], new byte[0], 0);
    }

    @Test
    void playersComeFirstThenNearestEntities() {
        EntityVisual far = visual("minecraft:zombie", 10.0D, 0.0D, 0.0D);
        EntityVisual near = visual("minecraft:zombie", 1.0D, 0.0D, 0.0D);
        EntityVisual player = visual("minecraft:player", 20.0D, 0.0D, 0.0D);
        List<EntityVisual> ordered = EntityPriority.sorted(List.of(far, player, near), 0.0D, 0.0D, 0.0D);
        assertEquals(List.of(player, near, far), ordered);
    }

    @Test
    void withinDropsEntitiesOutsideTheRangeCube() {
        EntityVisual inside = visual("minecraft:cow", 4.0D, -4.0D, 4.0D);
        EntityVisual outside = visual("minecraft:cow", 0.0D, 9.0D, 0.0D);
        assertEquals(List.of(inside), EntityPriority.within(List.of(outside, inside), 0.0D, 0.0D, 0.0D, 8.0D));
    }

    @Test
    void remoteViewKeepsAPlayerListedLastWithinTheEntityBudget() {
        List<EntityVisual> received = new ArrayList<>();
        for (int i = 0; i < 63; i++) {
            received.add(visual("minecraft:item_display", 2.0D + (i % 7), 0.0D, 2.0D + (i / 7)));
        }
        EntityVisual player = visual("minecraft:player", 0.5D, -3.5D, 0.4D);
        received.add(player);
        RemoteViewCache.RemoteView<String, Object, Object> remote = mock(RemoteViewCache.RemoteView.class);
        when(remote.getEntities()).thenReturn(received);
        RemoteProjectionView<String, String, Object, Object> view = new RemoteProjectionView<>(remote,
            new RemoteProjectionView.Options<>("minecraft:air", state -> state));
        List<EntityVisual> entities = view.getEntities(0.0D, 0.0D, 0.0D, 48.0D);
        assertEquals(player, entities.get(0));
        assertTrue(entities.subList(0, 24).contains(player));
    }
}
