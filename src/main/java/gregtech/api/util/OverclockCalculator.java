package gregtech.api.util;

/**
 * Pure power/duration calculation adapted from GTNH's OverclockCalculator.
 * https://github.com/GTNewHorizons/GT5-Unofficial/blob/master/src/main/java/gregtech/api/util/OverclockCalculator.java
 * No inventories or machine fields are modified by a calculation.
 */
public final class OverclockCalculator {
    private double timeReduction = 2;
    private double powerIncrease = 4;
    private double euModifier = 1;
    private double durationModifier = 1;
    private int recipeHeat;
    private int machineHeat;
    private boolean heatDiscount;
    private boolean heatOC;

    public OverclockCalculator setOverclock(double timeReduction, double powerIncrease) {
        if (!Double.isFinite(timeReduction) || !Double.isFinite(powerIncrease) || timeReduction <= 1
                || powerIncrease <= 1) {
            throw new IllegalArgumentException("Invalid overclock factors");
        }
        this.timeReduction = timeReduction;
        this.powerIncrease = powerIncrease;
        return this;
    }

    public OverclockCalculator setEUtModifier(double value) {
        if (!Double.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException("Invalid EU modifier");
        }
        euModifier = value;
        return this;
    }

    public OverclockCalculator setDurationModifier(double value) {
        if (!Double.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException("Invalid duration modifier");
        }
        durationModifier = value;
        return this;
    }

    public OverclockCalculator setHeat(int required, int available, boolean discount, boolean perfectOC) {
        recipeHeat = required;
        machineHeat = available;
        heatDiscount = discount;
        heatOC = perfectOC;
        return this;
    }

    public long recipePower(long recipeEUt) {
        return checkedCeil(discountedRecipePower(recipeEUt));
    }

    public Result calculate(long recipeEUt, int duration, long availablePower, int parallel) {
        if (recipeEUt < 0 || duration <= 0 || availablePower < 0 || parallel < 1) {
            throw new IllegalArgumentException("Invalid processing parameters");
        }
        double energy = discountedRecipePower(recipeEUt) * parallel;
        double controlPower = Math.max(checkedCeil(energy), 32);
        double ticks = duration * durationModifier;
        int heatClocks = heatOC
                ? (int) Math.min(Integer.MAX_VALUE, Math.max(0L, (long) machineHeat - recipeHeat) / 1800)
                : 0;
        int clocks = 0;
        while (controlPower <= availablePower / powerIncrease) {
            controlPower *= powerIncrease;
            energy *= powerIncrease;
            ticks /= clocks++ < heatClocks ? 4 : timeReduction;
        }
        long eut = checkedCeil(energy);
        if (!Double.isFinite(ticks) || ticks > Integer.MAX_VALUE) {
            throw new DurationOverflowException();
        }
        return new Result(eut, Math.max(1, (int) ticks), Math.max(1, Math.ceil(1 / ticks)));
    }

    public boolean hasEnoughHeat() {
        return machineHeat >= recipeHeat;
    }

    private double discountedRecipePower(long recipeEUt) {
        double discount = heatDiscount ? Math.pow(0.95, Math.max(0L, (long) machineHeat - recipeHeat) / 900) : 1;
        return recipeEUt * euModifier * discount;
    }

    private static long checkedCeil(double value) {
        if (!Double.isFinite(value) || value < 0 || value >= Long.MAX_VALUE) {
            throw new ArithmeticException("Power overflow");
        }
        return (long) Math.ceil(value);
    }

    public static final class Result {
        public final long eut;
        public final int duration;
        public final double subTickMultiplier;

        private Result(long eut, int duration, double multiplier) {
            this.eut = eut;
            this.duration = duration;
            this.subTickMultiplier = multiplier;
        }
    }

    public static final class DurationOverflowException extends ArithmeticException {
    }
}
