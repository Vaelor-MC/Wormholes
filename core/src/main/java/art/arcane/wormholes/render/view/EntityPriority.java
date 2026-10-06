package art.arcane.wormholes.render.view;

import art.arcane.wormholes.network.view.EntityVisual;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Order in which projected entities claim the per-view entity budget: players first, then the entities
 * nearest to the portal. Callers cap the ordered list, so an unordered source must not decide which
 * entities survive the cap.
 */
public final class EntityPriority {
    private EntityPriority() {
    }

    public static Comparator<EntityVisual> order(double centerX, double centerY, double centerZ) {
        return Comparator.comparing((EntityVisual visual) -> !visual.isPlayer())
            .thenComparingDouble(visual -> distanceSquared(visual.x(), visual.y(), visual.z(), centerX, centerY, centerZ));
    }

    public static List<EntityVisual> sorted(Collection<EntityVisual> visuals, double centerX, double centerY, double centerZ) {
        List<EntityVisual> ordered = new ArrayList<>(visuals);
        ordered.sort(order(centerX, centerY, centerZ));
        return ordered;
    }

    public static List<EntityVisual> within(Collection<EntityVisual> visuals, double centerX, double centerY, double centerZ, double range) {
        List<EntityVisual> selected = new ArrayList<>(visuals.size());
        for (EntityVisual visual : visuals) {
            if (Math.abs(visual.x() - centerX) <= range
                && Math.abs(visual.y() - centerY) <= range
                && Math.abs(visual.z() - centerZ) <= range) {
                selected.add(visual);
            }
        }
        selected.sort(order(centerX, centerY, centerZ));
        return selected;
    }

    public static double distanceSquared(double x, double y, double z, double centerX, double centerY, double centerZ) {
        double dx = x - centerX;
        double dy = y - centerY;
        double dz = z - centerZ;
        return (dx * dx) + (dy * dy) + (dz * dz);
    }
}
