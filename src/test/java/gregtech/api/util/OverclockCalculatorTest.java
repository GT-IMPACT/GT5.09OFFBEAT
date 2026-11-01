package gregtech.api.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OverclockCalculatorTest {
    @Test
    public void ordinaryAndPerfectOverclocksRespectPowerBoundaries() {
        long[] power = { 31, 32, 127, 128, 511, 512, 2047, 2048 };
        long[] expectedEnergy = { 24, 24, 24, 96, 96, 384, 384, 1536 };
        int[] ordinary = { 80, 80, 80, 40, 40, 20, 20, 10 };
        int[] perfect = { 80, 80, 80, 20, 20, 5, 5, 1 };
        for (int index = 0; index < power.length; index++) {
            OverclockCalculator.Result normal = new OverclockCalculator().calculate(24, 80, power[index], 1);
            OverclockCalculator.Result ideal = new OverclockCalculator().setOverclock(4, 4).calculate(24, 80,
                    power[index], 1);
            assertEquals(expectedEnergy[index], normal.eut);
            assertEquals(expectedEnergy[index], ideal.eut);
            assertEquals(ordinary[index], normal.duration);
            assertEquals(perfect[index], ideal.duration);
        }
    }

    @Test
    public void heatDiscountsAreRoundedAfterParallelAndOverclockCalculations() {
        int[] heat = { 1800, 2699, 2700, 3599, 3600, 5400 };
        long[] energy = { 384, 384, 365, 365, 347, 313 };
        int[] ticks = { 20, 20, 20, 20, 10, 5 };
        for (int index = 0; index < heat.length; index++) {
            OverclockCalculator calculator = new OverclockCalculator().setHeat(1800, heat[index], true, true);
            OverclockCalculator.Result result = calculator.calculate(24, 80, 512, 1);
            assertEquals(energy[index], result.eut);
            assertEquals(ticks[index], result.duration);
        }
        assertFalse(new OverclockCalculator().setHeat(1800, 1799, true, true).hasEnoughHeat());
        assertTrue(new OverclockCalculator().setHeat(1800, 1800, true, true).hasEnoughHeat());
    }

    @Test
    public void subTickCalculationRetainsPowerAndExposesExtraOperations() {
        OverclockCalculator.Result result = new OverclockCalculator().calculate(24, 1, 512, 1);
        assertEquals(384, result.eut);
        assertEquals(1, result.duration);
        assertEquals(4, result.subTickMultiplier, 0);
    }

    @Test
    public void consumptionCanExceedIntegerRange() {
        OverclockCalculator.Result result = new OverclockCalculator().calculate(1073741824, 80, 17179869184L, 1);
        assertEquals(17179869184L, result.eut);
        assertEquals(20, result.duration);
    }

    @Test(expected = ArithmeticException.class)
    public void powerOverflowIsRejected() {
        new OverclockCalculator().calculate(Long.MAX_VALUE, 80, Long.MAX_VALUE, 2);
    }

    @Test(expected = OverclockCalculator.DurationOverflowException.class)
    public void durationOverflowIsRejected() {
        new OverclockCalculator().setDurationModifier(2).calculate(24, Integer.MAX_VALUE, 32, 1);
    }

    @Test
    public void batchAddsSequentialOperationsWithinBothLimits() {
        assertEquals(1, ParallelHelper.batchLimit(1, 1, false));
        assertEquals(128, ParallelHelper.batchLimit(1, 1, true));
        assertEquals(2048, ParallelHelper.batchLimit(16, 1, true));
        assertEquals(32, ParallelHelper.batchLimit(8, 32, true));
        assertEquals(25, ParallelHelper.batchLimit(16, 80, true));
        assertEquals(8, ParallelHelper.batchLimit(8, 129, true));
    }
}
