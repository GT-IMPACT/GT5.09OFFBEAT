package gregtech.common.items.toolbox.pickblock;

import gregtech.api.enums.ToolboxSlot;
import mods.railcraft.common.blocks.tracks.TileTrack;

public class RailcraftAction extends DependentAction {

    public RailcraftAction() {
        super("Railcraft", () -> new SimpleAction<>(TileTrack.class, ToolboxSlot.CROWBAR));
    }
}
