package gregtech.common.items.toolbox.pickblock;

import java.util.List;
import java.util.function.Supplier;

import net.minecraftforge.common.util.ForgeDirection;

import com.google.common.collect.ImmutableList;

import cpw.mods.fml.common.Loader;
import gregtech.api.enums.ToolboxSlot;

/**
 * An action that requires another mod to function. Used to isolate optional mod related pick block actions.
 */
abstract public class DependentAction implements IDeciderAction {

    private final String requiredMod;
    private final Supplier<SimpleAction<?>> delegateSupplier;

    protected DependentAction(final String requiredMod, final Supplier<SimpleAction<?>> delegateSupplier) {

        this.requiredMod = requiredMod;
        this.delegateSupplier = delegateSupplier;
    }

    @Override
    public boolean isValid(final Object obj) {
        return Loader.isModLoaded(requiredMod) && delegateSupplier.get()
            .isValid(obj);
    }

    @Override
    public List<ToolboxSlot> apply(final Object obj, final ForgeDirection side) {
        return Loader.isModLoaded(requiredMod) ? delegateSupplier.get()
            .apply(obj, side) : ImmutableList.of();
    }
}
