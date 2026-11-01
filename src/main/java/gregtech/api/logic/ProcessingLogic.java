package gregtech.api.logic;

import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.OverclockCalculator;
import gregtech.api.util.ParallelHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;

/**
 * GTNH-style processing stages with a transactional boundary before input
 * consumption.
 * https://github.com/GTNewHorizons/GT5-Unofficial/blob/master/src/main/java/gregtech/api/logic/ProcessingLogic.java
 * Machine-specific behavior belongs in suppliers/validators, never class-name
 * switches.
 */
public class ProcessingLogic {
    public static final int BATCH = 1;
    public static final int PROTECT_ITEMS = 2;
    public static final int PROTECT_FLUIDS = 4;
    public static final int LOCK_RECIPE = 8;
    public static final int SEPARATE_INPUTS = 16;
    public static final int ALL_MODES = BATCH | PROTECT_ITEMS | PROTECT_FLUIDS | LOCK_RECIPE | SEPARATE_INPUTS;

    private Supplier<GT_Recipe.GT_Recipe_Map> recipeMapSupplier;
    private Supplier<Integer> maxParallelSupplier = () -> 1;
    private Function<GT_Recipe, CheckRecipeResult> validator = recipe -> CheckRecipeResult.SUCCESS;
    private Function<GT_Recipe, OverclockCalculator> calculatorFactory = recipe -> new OverclockCalculator();
    private double timeReduction = 2;
    private double powerIncrease = 4;
    private boolean subTickParallelBypassesLimit = false;
    private int itemOutputLimit = Integer.MAX_VALUE;
    private GT_Recipe.GT_Recipe_Map lastMap;
    private GT_Recipe cachedRecipe;
    private NBTTagCompound lockedRecipe;
    private int availableParallel = 1;

    public ProcessingLogic setRecipeMapSupplier(Supplier<GT_Recipe.GT_Recipe_Map> supplier) {
        recipeMapSupplier = supplier;
        return this;
    }

    public ProcessingLogic setMaxParallelSupplier(Supplier<Integer> supplier) {
        maxParallelSupplier = supplier;
        return this;
    }

    public ProcessingLogic setSubTickParallelBypassesLimit(boolean enabled) {
        subTickParallelBypassesLimit = enabled;
        return this;
    }

    public ProcessingLogic setItemOutputLimit(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("Negative output limit");
        }
        itemOutputLimit = limit;
        return this;
    }

    public ProcessingLogic setOverclock(double time, double power) {
        new OverclockCalculator().setOverclock(time, power);
        timeReduction = time;
        powerIncrease = power;
        return this;
    }

    public ProcessingLogic setRecipeValidator(Function<GT_Recipe, CheckRecipeResult> check) {
        validator = check;
        return this;
    }

    public ProcessingLogic setCalculatorSupplier(Supplier<OverclockCalculator> supplier) {
        return setCalculatorFactory(recipe -> supplier.get());
    }

    public ProcessingLogic setCalculatorFactory(Function<GT_Recipe, OverclockCalculator> factory) {
        calculatorFactory = factory;
        return this;
    }

    public int getAvailableParallel() {
        return availableParallel;
    }

    public boolean requiresRecipeMap() {
        return true;
    }

    public void clearRecipeLock() {
        lockedRecipe = null;
    }

    public void clearRecipeCache() {
        cachedRecipe = null;
        lastMap = null;
    }

    public NBTTagCompound saveRecipeLock() {
        return lockedRecipe == null ? new NBTTagCompound() : (NBTTagCompound) lockedRecipe.copy();
    }

    public void loadRecipeLock(NBTTagCompound tag) {
        lockedRecipe = tag.hasKey("map") ? (NBTTagCompound) tag.copy() : null;
    }

    public Result process(Request request) {
        availableParallel = Math.max(0, maxParallelSupplier.get());
        if (availableParallel == 0) {
            return Result.failure(CheckRecipeResult.MACHINE_CONDITION);
        }
        GT_Recipe.GT_Recipe_Map map = refreshRecipeMap();
        if (map == null) {
            return Result.failure(CheckRecipeResult.NO_RECIPE);
        }
        if (!request.enabled(LOCK_RECIPE)) {
            clearRecipeLock();
        }
        if (!isRecipeLockValid(map)) {
            return Result.failure(CheckRecipeResult.RECIPE_LOCK_INVALID);
        }
        final long power;
        try {
            power = Math.multiplyExact(request.voltage, request.amperage);
        } catch (ArithmeticException e) {
            return Result.failure(CheckRecipeResult.POWER_OVERFLOW);
        }
        CheckRecipeResult failure = CheckRecipeResult.NO_RECIPE;
        for (ProcessingInputs inputs : request.inputs) {
            for (GT_Recipe recipe : findCandidates(map, inputs)) {
                if (!matchesRecipe(map, recipe, inputs)) {
                    continue;
                }
                Result result = prepareRecipe(recipe, inputs, request, power);
                if (!result.status.wasSuccessful()) {
                    failure = result.status;
                    continue;
                }
                NBTTagCompound nextLock = request.enabled(LOCK_RECIPE)
                        ? ProcessingPersistence.recipeIdentity(map, recipe)
                        : null;
                // Everything that can reject the recipe has completed. Commit once.
                if (recipeMapSupplier.get() != map || !result.consumed.commit()) {
                    return Result.failure(CheckRecipeResult.INPUT_CHANGED);
                }
                cachedRecipe = recipe.mCanBeBuffered ? recipe : null;
                lockedRecipe = nextLock;
                return result;
            }
        }
        return Result.failure(failure);
    }

    private GT_Recipe.GT_Recipe_Map refreshRecipeMap() {
        GT_Recipe.GT_Recipe_Map map = recipeMapSupplier == null ? null : recipeMapSupplier.get();
        if (map != lastMap) {
            cachedRecipe = null;
            lastMap = map;
        } else if (map != null && cachedRecipe != null && !map.mRecipeList.contains(cachedRecipe)) {
            cachedRecipe = null;
        }
        return map;
    }

    private boolean isRecipeLockValid(GT_Recipe.GT_Recipe_Map map) {
        if (lockedRecipe == null) {
            return true;
        }
        if (!map.mUnlocalizedName.equals(lockedRecipe.getString("map"))) {
            return false;
        }
        for (GT_Recipe recipe : map.mRecipeList) {
            if (lockedRecipe.equals(ProcessingPersistence.recipeIdentity(map, recipe))) {
                return true;
            }
        }
        return false;
    }

    private Set<GT_Recipe> findCandidates(GT_Recipe.GT_Recipe_Map map, ProcessingInputs inputs) {
        Set<GT_Recipe> candidates = new LinkedHashSet<>();
        if (cachedRecipe != null) {
            candidates.add(cachedRecipe);
        }
        candidates.addAll(map.findProcessingCandidates(inputs.fluids, inputs.items));
        return candidates;
    }

    private boolean matchesRecipe(GT_Recipe.GT_Recipe_Map map, GT_Recipe recipe, ProcessingInputs inputs) {
        if (!recipe.mEnabled || recipe.mFakeRecipe || recipe.mEUt < 0 || recipe.mDuration <= 0) {
            return false;
        }
        if (lockedRecipe != null && !lockedRecipe.equals(ProcessingPersistence.recipeIdentity(map, recipe))) {
            return false;
        }
        return inputs.copy().consume(recipe, 1);
    }

    private Result prepareRecipe(GT_Recipe recipe, ProcessingInputs inputs, Request request, long power) {
        CheckRecipeResult validation = validator.apply(recipe);
        if (!validation.wasSuccessful()) {
            return Result.failure(validation);
        }
        if (request.voltage <= 0 || (request.voltage <= Long.MAX_VALUE / 4 && recipe.mEUt > request.voltage * 4)) {
            return Result.failure(CheckRecipeResult.INSUFFICIENT_VOLTAGE);
        }
        try {
            return calculate(recipe, inputs, request, power);
        } catch (OverclockCalculator.DurationOverflowException e) {
            return Result.failure(CheckRecipeResult.DURATION_OVERFLOW);
        } catch (OutputOverflowException e) {
            return Result.failure(CheckRecipeResult.OUTPUT_OVERFLOW);
        } catch (ArithmeticException e) {
            return Result.failure(CheckRecipeResult.POWER_OVERFLOW);
        }
    }

    private Result calculate(GT_Recipe recipe, ProcessingInputs inputs, Request request, long power) {
        OverclockCalculator calculator = calculatorFactory.apply(recipe).setOverclock(timeReduction, powerIncrease);
        if (!calculator.hasEnoughHeat()) {
            return Result.failure(CheckRecipeResult.INSUFFICIENT_HEAT);
        }
        long recipePower = calculator.recipePower(recipe.mEUt);
        if (recipePower > power || power <= 0) {
            return Result.failure(CheckRecipeResult.INSUFFICIENT_POWER);
        }
        int normalLimit = (int) Math.min(availableParallel, recipePower == 0 ? Integer.MAX_VALUE : power / recipePower);
        if (normalLimit <= 0) {
            return Result.failure(CheckRecipeResult.INSUFFICIENT_POWER);
        }
        int limit = normalLimit;
        if (subTickParallelBypassesLimit) {
            double multiplier = calculator.calculate(recipe.mEUt, recipe.mDuration, power,
                    normalLimit).subTickMultiplier;
            limit = (int) Math.min(Integer.MAX_VALUE, normalLimit * multiplier);
            if (recipePower > 0) {
                limit = (int) Math.min(limit, power / recipePower);
            }
        }
        int parallel = ParallelHelper.byInputs(recipe, inputs, limit);
        parallel = fitOutputs(recipe, parallel, request);
        if (parallel <= 0) {
            CheckRecipeResult status = outputStatus(recipe, 1, request);
            return Result.failure(status.wasSuccessful() ? CheckRecipeResult.NO_RECIPE : status);
        }
        // Sub-tick operations reuse the overclocked cycle; they do not add another
        // power multiplier.
        int poweredParallel = Math.min(normalLimit, parallel);
        OverclockCalculator.Result clock = calculator.calculate(recipe.mEUt, recipe.mDuration, power, poweredParallel);
        efficiencyAdjustedEUt(clock.eut, request.efficiency);
        int count = ParallelHelper.byInputs(recipe, inputs,
                ParallelHelper.batchLimit(parallel, clock.duration, request.enabled(BATCH)));
        count = fitOutputs(recipe, count, request);
        count = Math.max(parallel, count);
        long duration = ((long) clock.duration * count + parallel - 1) / parallel;
        if (duration > Integer.MAX_VALUE) {
            throw new OverclockCalculator.DurationOverflowException();
        }
        ProcessingInputs consumed = inputs.copy();
        if (!consumed.consume(recipe, count)) {
            return Result.failure(CheckRecipeResult.INPUT_CHANGED);
        }
        ItemStack[] itemOutputs = itemOutputs(recipe, count, request.random);
        FluidStack[] fluidOutputs = request.outputs.prepareFluids(fluidOutputs(recipe, count));
        return new Result(CheckRecipeResult.SUCCESS, consumed, recipe, clock.eut, (int) duration, parallel, count,
                itemOutputs, fluidOutputs);
    }

    private int fitOutputs(GT_Recipe recipe, int limit, Request request) {
        return ParallelHelper.maximum(limit, count -> outputStatus(recipe, count, request).wasSuccessful());
    }

    private CheckRecipeResult outputStatus(GT_Recipe recipe, int count, Request request) {
        ItemStack[] items = itemOutputs(recipe, count, null);
        FluidStack[] fluids = request.outputs.prepareFluids(fluidOutputs(recipe, count));
        return request.outputs.canFit(items, fluids, request.enabled(PROTECT_ITEMS), request.enabled(PROTECT_FLUIDS));
    }

    private ItemStack[] itemOutputs(GT_Recipe recipe, int count, IntUnaryOperator random) {
        ItemStack[] outputs = new ItemStack[Math.min(itemOutputLimit, recipe.mOutputs.length)];
        for (int i = 0; i < outputs.length; i++) {
            if (recipe.mOutputs[i] == null || recipe.getOutputChance(i) <= 0) {
                continue;
            }
            int successes = count;
            if (random != null && recipe.getOutputChance(i) < 10000) {
                successes = 0;
                for (int j = 0; j < count; j++) {
                    if (random.applyAsInt(10000) < recipe.getOutputChance(i)) {
                        successes++;
                    }
                }
            }
            if (successes == 0) {
                continue;
            }
            outputs[i] = recipe.mOutputs[i].copy();
            outputs[i].stackSize = outputAmount(outputs[i].stackSize, successes);
        }
        return outputs;
    }

    private static FluidStack[] fluidOutputs(GT_Recipe recipe, int count) {
        FluidStack[] outputs = ProcessingInputs.copyFluids(recipe.mFluidOutputs);
        for (FluidStack output : outputs) {
            if (output != null) {
                output.amount = outputAmount(output.amount, count);
            }
        }
        return outputs;
    }

    private static int outputAmount(int amount, int count) {
        long result = (long) amount * count;
        if (result < 0 || result > Integer.MAX_VALUE) {
            throw new OutputOverflowException();
        }
        return (int) result;
    }

    public static long efficiencyAdjustedEUt(long eut, int efficiency) {
        int divisor = Math.max(1000, efficiency);
        return Math.addExact(Math.multiplyExact(eut / divisor, 10000), eut % divisor * 10000 / divisor);
    }

    public static final class OutputOverflowException extends ArithmeticException {
    }

    public static final class Request {
        public final List<ProcessingInputs> inputs;
        public final long voltage;
        public final long amperage;
        public final int modes;
        public final int efficiency;
        public final ProcessingOutputs outputs;
        public final IntUnaryOperator random;

        public Request(List<ProcessingInputs> inputs, long voltage, long amperage, int modes, ProcessingOutputs outputs,
                IntUnaryOperator random) {
            this(inputs, voltage, amperage, modes, outputs, random, 10000);
        }

        public Request(List<ProcessingInputs> inputs, long voltage, long amperage, int modes, ProcessingOutputs outputs,
                IntUnaryOperator random, int efficiency) {
            this.inputs = inputs;
            this.voltage = voltage;
            this.amperage = amperage;
            this.modes = modes;
            this.outputs = outputs;
            this.random = random;
            this.efficiency = efficiency;
        }

        public boolean enabled(int flag) {
            return (modes & flag) != 0;
        }
    }

    public static final class Result {
        public final CheckRecipeResult status;
        public final ProcessingInputs consumed;
        public final GT_Recipe recipe;
        public final long eut;
        public final int duration;
        public final int parallel;
        public final int operations;
        public final ItemStack[] items;
        public final FluidStack[] fluids;

        private Result(CheckRecipeResult status, ProcessingInputs consumed, GT_Recipe recipe, long eut, int duration,
                int parallel, int operations, ItemStack[] items, FluidStack[] fluids) {
            this.status = status;
            this.consumed = consumed;
            this.recipe = recipe;
            this.eut = eut;
            this.duration = duration;
            this.parallel = parallel;
            this.operations = operations;
            this.items = items;
            this.fluids = fluids;
        }

        public static Result failure(CheckRecipeResult status) {
            return new Result(status, null, null, 0, 0, 0, 0, null, null);
        }

        /** A committed fuel burn; the machine handles its thermal output each tick. */
        public static Result fuelBurn(ProcessingInputs consumed, int duration, ItemStack[] containers) {
            if (duration <= 0) {
                throw new IllegalArgumentException("Burn duration must be positive");
            }
            return new Result(CheckRecipeResult.SUCCESS, consumed, null, 0, duration, 1, 1,
                    containers, new FluidStack[0]);
        }

        /** Fuel has already been committed; negative EU/t denotes generation. */
        public static Result generation(ProcessingInputs consumed, long eut, FluidStack[] fluids) {
            if (eut <= 0) {
                throw new IllegalArgumentException("Generation must be positive");
            }
            return new Result(CheckRecipeResult.SUCCESS, consumed, null, -eut, 1, 1, 1,
                    new ItemStack[0], fluids);
        }
    }
}
