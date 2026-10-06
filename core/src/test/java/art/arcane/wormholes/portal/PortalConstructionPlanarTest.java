package art.arcane.wormholes.portal;

import art.arcane.wormholes.portal.PortalConstruction.Cell;
import art.arcane.wormholes.portal.PortalConstruction.Plane;
import art.arcane.wormholes.portal.PortalConstruction.PlanarCells;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PortalConstructionPlanarTest {
    private static Set<Cell> disc(int radius, int y) {
        Set<Cell> cells = new HashSet<>();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z <= radius * radius) {
                    cells.add(new Cell(x, y, z));
                }
            }
        }
        return cells;
    }

    @Test
    void floorFillTakesTheWholeDiscAndStaysOnItsLayer() {
        Set<Cell> blocks = disc(11, 1);
        blocks.addAll(disc(3, 2));
        blocks.add(new Cell(0, 0, 0));
        PlanarCells found = PortalConstruction.planarCells(new Cell(5, 1, 5), Plane.Y, blocks::contains, 4096);
        assertFalse(found.overLimit());
        assertEquals(disc(11, 1), found.cells());
    }

    @Test
    void wallFillFollowsTheWallOnly() {
        Set<Cell> blocks = new HashSet<>();
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 3; y++) {
                blocks.add(new Cell(x, y, 7));
                blocks.add(new Cell(x, y, 8));
            }
        }
        PlanarCells found = PortalConstruction.planarCells(new Cell(1, 1, 7), Plane.Z, blocks::contains, 4096);
        assertEquals(12, found.cells().size());
        assertTrue(found.cells().stream().allMatch(cell -> cell.z() == 7));
    }

    @Test
    void fillPastTheLimitReturnsNothing() {
        PlanarCells found = PortalConstruction.planarCells(new Cell(0, 1, 0), Plane.Y, disc(11, 1)::contains, 100);
        assertTrue(found.overLimit());
        assertTrue(found.cells().isEmpty());
    }

    @Test
    void faceNormalPicksTheFixedAxis() {
        assertEquals(Plane.Y, PortalConstruction.planeFacing(0, 1, 0));
        assertEquals(Plane.Y, PortalConstruction.planeFacing(0, -1, 0));
        assertEquals(Plane.X, PortalConstruction.planeFacing(-1, 0, 0));
        assertEquals(Plane.Z, PortalConstruction.planeFacing(0, 0, 1));
    }
}
