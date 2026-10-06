package art.arcane.wormholes.portal;

import art.arcane.wormholes.util.Direction;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public final class PortalConstruction {
    private PortalConstruction() {
    }

    public static Set<Cell> connectedCells(Cell start, PortalType type, Function<Cell, PortalType> lookup) {
        Set<Cell> connected = new HashSet<>();
        Set<Cell> visited = new HashSet<>();
        ArrayDeque<Cell> search = new ArrayDeque<>();
        search.add(start);
        while (!search.isEmpty()) {
            Cell cell = search.removeFirst();
            if (!visited.add(cell) || lookup.apply(cell) != type) {
                continue;
            }
            connected.add(cell);
            search.addLast(new Cell(cell.x() + 1, cell.y(), cell.z()));
            search.addLast(new Cell(cell.x() - 1, cell.y(), cell.z()));
            search.addLast(new Cell(cell.x(), cell.y() + 1, cell.z()));
            search.addLast(new Cell(cell.x(), cell.y() - 1, cell.z()));
            search.addLast(new Cell(cell.x(), cell.y(), cell.z() + 1));
            search.addLast(new Cell(cell.x(), cell.y(), cell.z() - 1));
        }
        return Set.copyOf(connected);
    }

    public record Cell(int x, int y, int z) {
    }

    /** The axis a flat surface keeps fixed: Y for a floor or ceiling, X or Z for a wall. */
    public enum Plane {
        X, Y, Z
    }

    public record PlanarCells(Set<Cell> cells, boolean overLimit) {
    }

    public static Plane planeFacing(int faceX, int faceY, int faceZ) {
        if (faceY != 0) {
            return Plane.Y;
        }
        return faceX != 0 ? Plane.X : Plane.Z;
    }

    /**
     * Cells joined to {@code start} through their faces without leaving its plane, for turning blocks
     * placed by an editor into runes. Past {@code limit} cells nothing is returned.
     */
    public static PlanarCells planarCells(Cell start, Plane fixed, Predicate<Cell> member, int limit) {
        Set<Cell> connected = new HashSet<>();
        Set<Cell> visited = new HashSet<>();
        ArrayDeque<Cell> search = new ArrayDeque<>();
        search.add(start);
        while (!search.isEmpty()) {
            Cell cell = search.removeFirst();
            if (!visited.add(cell) || !member.test(cell)) {
                continue;
            }
            if (connected.size() >= limit) {
                return new PlanarCells(Set.of(), true);
            }
            connected.add(cell);
            if (fixed != Plane.X) {
                search.addLast(new Cell(cell.x() + 1, cell.y(), cell.z()));
                search.addLast(new Cell(cell.x() - 1, cell.y(), cell.z()));
            }
            if (fixed != Plane.Y) {
                search.addLast(new Cell(cell.x(), cell.y() + 1, cell.z()));
                search.addLast(new Cell(cell.x(), cell.y() - 1, cell.z()));
            }
            if (fixed != Plane.Z) {
                search.addLast(new Cell(cell.x(), cell.y(), cell.z() + 1));
                search.addLast(new Cell(cell.x(), cell.y(), cell.z() - 1));
            }
        }
        return new PlanarCells(Set.copyOf(connected), false);
    }

	public static boolean isCoplanarPortalArea(int xDepth, int yDepth, int zDepth)
	{
		int flatAxes = 0;
		if(xDepth == 0)
		{
			flatAxes++;
		}
		if(yDepth == 0)
		{
			flatAxes++;
		}
		if(zDepth == 0)
		{
			flatAxes++;
		}

		return flatAxes >= 1;
	}

	public static Direction derivePortalNormal(int xDepth, int yDepth, int zDepth, double lookX, double lookY, double lookZ)
	{
		double ax = xDepth == 0 ? Math.abs(lookX) : -1.0D;
		double ay = yDepth == 0 ? Math.abs(lookY) : -1.0D;
		double az = zDepth == 0 ? Math.abs(lookZ) : -1.0D;

		if(ax >= ay && ax >= az)
		{
			return lookX >= 0.0D ? Direction.E : Direction.W;
		}

		if(ay >= az)
		{
			return lookY >= 0.0D ? Direction.U : Direction.D;
		}

		return lookZ >= 0.0D ? Direction.S : Direction.N;
	}

}
