package gregtech.api.logic;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraftforge.common.util.BlockSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * A small, reversible set of block writes prepared without modifying the world.
 */
public final class ProcessingWorldChanges {
    private final World world;
    private final List<Change> changes = new ArrayList<>();
    private final List<BlockSnapshot> applied = new ArrayList<>();

    public ProcessingWorldChanges(World world) {
        this.world = world;
    }

    public void observe(int x, int y, int z) {
        for (Change change : changes) {
            if (change.before.x == x && change.before.y == y && change.before.z == z) {
                return;
            }
        }
        BlockSnapshot before = BlockSnapshot.getBlockSnapshot(world, x, y, z);
        changes.add(new Change(before, before.getReplacedBlock(), before.meta));
    }

    public void setBlock(int x, int y, int z, Block block, int meta) {
        for (Change change : changes) {
            if (change.before.x == x && change.before.y == y && change.before.z == z) {
                change.block = block;
                change.meta = meta;
                return;
            }
        }
        changes.add(new Change(BlockSnapshot.getBlockSnapshot(world, x, y, z), block, meta));
    }

    public boolean isUnchanged() {
        for (Change change : changes) {
            BlockSnapshot before = change.before;
            if (!world.blockExists(before.x, before.y, before.z)
                    || !before.equals(BlockSnapshot.getBlockSnapshot(world, before.x, before.y, before.z))) {
                return false;
            }
        }
        return true;
    }

    public boolean commit() {
        if (!isUnchanged()) {
            return false;
        }
        for (Change change : changes) {
            BlockSnapshot before = change.before;
            if (before.getReplacedBlock() == change.block && before.meta == change.meta) {
                continue;
            }
            applied.add(before);
            if (!world.setBlock(before.x, before.y, before.z, change.block, change.meta, 3)) {
                rollback();
                return false;
            }
        }
        return true;
    }

    public void rollback() {
        while (!applied.isEmpty()) {
            applied.remove(applied.size() - 1).restore(true, false);
        }
    }

    private static final class Change {
        final BlockSnapshot before;
        Block block;
        int meta;

        Change(BlockSnapshot before, Block block, int meta) {
            this.before = before;
            this.block = block;
            this.meta = meta;
        }
    }
}
