package gregtech.api.logic;

import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.util.GT_Utility;
import gregtech.api.util.OverclockCalculator;
import gregtech.api.util.ParallelHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.Supplier;

/** Data-stick recipes retain their ordered bus/hatch addressing. */
public final class AssemblyLineProcessingLogic extends ProcessingLogic {
    private final Supplier<List<ItemStack>> itemPorts;
    private final Iterable<FluidStack[]> fluidPorts;
    private final Supplier<List<ItemStack>> dataSticks;
    private NBTTagCompound lockedRecipe;

    public AssemblyLineProcessingLogic(Supplier<List<ItemStack>> itemPorts, Supplier<List<FluidStack>> fluidPorts,
            Supplier<List<ItemStack>> dataSticks) {
        this(itemPorts, dataSticks, () -> fluidPorts.get().stream().map(fluid -> new FluidStack[] { fluid }).iterator());
    }

    public AssemblyLineProcessingLogic(
        Supplier<List<ItemStack>> itemPorts,
        Supplier<List<ItemStack>> dataSticks,
        Iterable<FluidStack[]> fluidPorts
    ) {
        this.itemPorts = itemPorts;
        this.fluidPorts = fluidPorts;
        this.dataSticks = dataSticks;
    }

    @Override
    public boolean requiresRecipeMap() {
        return false;
    }

    @Override
    public void clearRecipeLock() {
        lockedRecipe = null;
    }

    @Override
    public NBTTagCompound saveRecipeLock() {
        NBTTagCompound tag = new NBTTagCompound();
        if (lockedRecipe != null) {
            tag.setTag("assembly", lockedRecipe.copy());
        }
        return tag;
    }

    @Override
    public void loadRecipeLock(NBTTagCompound tag) {
        lockedRecipe = tag.hasKey("assembly") ? (NBTTagCompound) tag.getCompoundTag("assembly").copy() : null;
    }

    @Override
    public Result process(Request request) {
        if (!request.enabled(LOCK_RECIPE)) {
            clearRecipeLock();
        }
        List<ItemStack> items = new ArrayList<>(itemPorts.get());
        List<FluidStack[]> ports = new ArrayList<>();
        List<FluidStack> fluids = new ArrayList<>();
        for (FluidStack[] port : fluidPorts) {
            FluidStack[] snapshot = port.clone();
            ports.add(snapshot);
            fluids.addAll(Arrays.asList(snapshot));
        }
        List<ItemStack> sticks = new ArrayList<>(dataSticks.get());
        ItemStack[] beforeSticks = ProcessingInputs.copyItems(sticks.toArray(new ItemStack[0]));
        ProcessingInputs source = new ProcessingInputs(items, fluids, () -> sameReferences(items, itemPorts.get())
                && sameFluidPorts(ports) && unchangedData(sticks, beforeSticks));
        int[] itemIndices = indices(items);
        int[] tankIndices = indices(fluids);
        int[][] fluidIndices = new int[ports.size()][];
        int offset = 0;
        for (int port = 0; port < ports.size(); port++) {
            int end = offset + ports.get(port).length;
            fluidIndices[port] = Arrays.copyOfRange(tankIndices, offset, end);
            offset = end;
        }
        CheckRecipeResult failure = lockedRecipe == null ? CheckRecipeResult.NO_RECIPE
                : CheckRecipeResult.RECIPE_LOCK_INVALID;
        try {
            long power = Math.multiplyExact(request.voltage, request.amperage);
            for (ItemStack stick : beforeSticks) {
                if (stick == null || !stick.hasTagCompound()) {
                    continue;
                }
                NBTTagCompound recipe = stick.getTagCompound();
                if (lockedRecipe != null && !lockedRecipe.equals(recipe)) {
                    continue;
                }
                Result result = prepare(recipe, source, itemIndices, fluidIndices, request, power);
                if (!result.status.wasSuccessful()) {
                    failure = result.status;
                    continue;
                }
                NBTTagCompound nextLock = request.enabled(LOCK_RECIPE) ? (NBTTagCompound) recipe.copy() : null;
                if (!result.consumed.commit()) {
                    return Result.failure(CheckRecipeResult.INPUT_CHANGED);
                }
                lockedRecipe = nextLock;
                return result;
            }
        } catch (ArithmeticException exception) {
            return Result.failure(CheckRecipeResult.POWER_OVERFLOW);
        }
        return Result.failure(failure);
    }

    private Result prepare(
        NBTTagCompound recipe,
        ProcessingInputs source,
        int[] items,
        int[][] fluids,
        Request request,
        long power
    ) {
        int eut = recipe.getInteger("eu");
        int duration = recipe.getInteger("time");
        ItemStack output = GT_Utility.loadItem(recipe, "output");
        if (!recipe.hasKey("eu") || eut < 0 || duration <= 0 || !GT_Utility.isStackValid(output)) {
            return Result.failure(CheckRecipeResult.NO_RECIPE);
        }
        if ((eut + 3L) / 4 > request.voltage) {
            return Result.failure(CheckRecipeResult.INSUFFICIENT_VOLTAGE);
        }
        if (eut > power) {
            return Result.failure(CheckRecipeResult.INSUFFICIENT_POWER);
        }
        OverclockCalculator.Result clock = new OverclockCalculator().calculate(eut, duration, power, 1);
        efficiencyAdjustedEUt(clock.eut, request.efficiency);
        int limit = ParallelHelper.batchLimit(1, clock.duration, request.enabled(BATCH));
        ProcessingInputs consumed = source.copy();
        int operations = 0;
        CheckRecipeResult failure = CheckRecipeResult.NO_RECIPE;
        while (operations < limit) {
            ProcessingInputs candidate = consumed.copy();
            if (!consume(recipe, candidate, items, fluids)) {
                break;
            }
            long amount = (long) output.stackSize * (operations + 1);
            if (amount > Integer.MAX_VALUE) {
                failure = CheckRecipeResult.OUTPUT_OVERFLOW;
                break;
            }
            ItemStack product = output.copy();
            product.stackSize = (int) amount;
            CheckRecipeResult fit = request.outputs.canFit(new ItemStack[] { product }, new FluidStack[0],
                    request.enabled(PROTECT_ITEMS), false);
            if (!fit.wasSuccessful()) {
                failure = fit;
                break;
            }
            consumed = candidate;
            operations++;
        }
        if (operations == 0) {
            return Result.failure(failure);
        }
        output.stackSize = Math.multiplyExact(output.stackSize, operations);
        long ticks = (long) clock.duration * operations;
        if (ticks > Integer.MAX_VALUE) {
            return Result.failure(CheckRecipeResult.DURATION_OVERFLOW);
        }
        return Result.cycle(consumed, clock.eut, (int) ticks, 1, operations, new ItemStack[] { output },
                new FluidStack[0]);
    }

    private static boolean consume(NBTTagCompound recipe, ProcessingInputs inputs, int[] items, int[][] fluids) {
        for (int port = 0; port < 15; port++) {
            int alternatives = recipe.getInteger("a" + port);
            if (!recipe.hasKey(Integer.toString(port)) && alternatives == 0) {
                continue;
            }
            if (port >= items.length || items[port] < 0 || alternatives < 0
                    || alternatives > recipe.func_150296_c().size()) {
                return false;
            }
            ItemStack supplied = inputs.items[items[port]];
            ItemStack required = null;
            for (int i = 0; i < alternatives; i++) {
                ItemStack option = GT_Utility.loadItem(recipe, "a" + port + ":" + i);
                if (matches(option, supplied)) {
                    required = option;
                    break;
                }
            }
            if (required == null) {
                required = GT_Utility.loadItem(recipe, Integer.toString(port));
            }
            if (!matches(required, supplied)) {
                return false;
            }
            supplied.stackSize -= required.stackSize;
        }
        for (int port = 0; port < 4; port++) {
            if (!recipe.hasKey("f" + port)) {
                continue;
            }
            FluidStack required = GT_Utility.loadFluid(recipe, "f" + port);
            if (required == null || required.amount < 0 || port >= fluids.length) {
                return false;
            }
            if (!consumeFluidPort(required, inputs, fluids[port])) {
                return false;
            }
        }
        return true;
    }

    private static boolean consumeFluidPort(FluidStack required, ProcessingInputs inputs, int[] tanks) {
        for (int index : tanks) {
            if (index < 0) {
                continue;
            }
            FluidStack supplied = inputs.fluids[index];
            if (GT_Utility.areFluidsEqual(supplied, required, true) && supplied.amount >= required.amount) {
                supplied.amount -= required.amount;
                return true;
            }
        }
        return false;
    }

    private boolean sameFluidPorts(List<FluidStack[]> before) {
        int index = 0;
        for (FluidStack[] current : fluidPorts) {
            if (index >= before.size()) {
                return false;
            }
            FluidStack[] previous = before.get(index++);
            if (previous.length != current.length) {
                return false;
            }
            for (int tank = 0; tank < current.length; tank++) {
                if (previous[tank] != current[tank]) {
                    return false;
                }
            }
        }
        return index == before.size();
    }

    private static boolean matches(ItemStack required, ItemStack supplied) {
        return required != null && required.stackSize >= 0 && supplied != null
                && supplied.stackSize >= required.stackSize && GT_Utility.areStacksEqual(required, supplied, true);
    }

    /**
     * ProcessingInputs removes empty ports and shared references; retain the
     * original addresses.
     */
    private static <T> int[] indices(List<T> ports) {
        IdentityHashMap<T, Integer> indices = new IdentityHashMap<>();
        int[] result = new int[ports.size()];
        for (int i = 0; i < ports.size(); i++) {
            T port = ports.get(i);
            if (port == null) {
                result[i] = -1;
            } else {
                if (!indices.containsKey(port)) {
                    indices.put(port, indices.size());
                }
                result[i] = indices.get(port);
            }
        }
        return result;
    }

    private static <T> boolean sameReferences(List<T> before, List<T> after) {
        if (before.size() != after.size()) {
            return false;
        }
        for (int i = 0; i < before.size(); i++) {
            if (before.get(i) != after.get(i)) {
                return false;
            }
        }
        return true;
    }

    private boolean unchangedData(List<ItemStack> sticks, ItemStack[] before) {
        if (!sameReferences(sticks, dataSticks.get())) {
            return false;
        }
        for (int i = 0; i < before.length; i++) {
            if (!ItemStack.areItemStacksEqual(before[i], sticks.get(i))) {
                return false;
            }
        }
        return true;
    }
}
