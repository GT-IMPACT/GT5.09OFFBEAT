package gregtech.common.items.toolbox.pickblock;

import java.util.List;
import java.lang.reflect.Method;


import com.google.common.collect.ImmutableList;

import gregtech.api.enums.ToolboxSlot;
import mrtjp.projectred.integration.GatePart;

public class ProjectRedAction extends DependentAction {

    public ProjectRedAction() {
        super("ProjRed|Integration", () -> Integration.ACTION);
    }

    // Multipart is an optional runtime dependency of ProjectRed.
    private static final class Integration {

        private static final SimpleAction<?> ACTION = createAction();

        private static SimpleAction<?> createAction() {
            try {
                Class<?> multipart = Class.forName("codechicken.multipart.TileMultipart");
                Method parts = multipart.getMethod("jPartList");
                return new SimpleAction<>(multipart, (tile, side) -> onApply(tile, parts));
            } catch (ReflectiveOperationException | LinkageError ignored) {
                return new SimpleAction<>(Void.class);
            }
        }

        private static List<ToolboxSlot> onApply(Object tile, Method parts) {
            try {
                for (Object part : (Iterable<?>) parts.invoke(tile)) {
                    if (part instanceof GatePart) {
                        return ImmutableList.of(ToolboxSlot.SCREWDRIVER);
                    }
                }
            } catch (ReflectiveOperationException ignored) {
                return ImmutableList.of();
            }
            return ImmutableList.of();
        }
    }
}
