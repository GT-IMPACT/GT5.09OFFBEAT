package gregtech.common.render;

import gregtech.api.metatileentity.implementations.GT_ProcessingTestSupport;
import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;

public class GT_FluidDisplayStackRendererTest {

    @Test
    public void amountLabelsKeepTheOriginalPrecisionAndSuffixes() throws Throwable {
        try (GT_ProcessingTestSupport.GameRuntime runtime = new GT_ProcessingTestSupport.GameRuntime()) {
            runtime.run(getClass(), "labels", "");
        }
    }

    public static void runScenario(String scenario, String unused) {
        Locale previousLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.US);
            assertEquals("9999L", GT_FluidDisplayStackRenderer.formatFluidAmount(9999));
            assertEquals("10.0kL", GT_FluidDisplayStackRenderer.formatFluidAmount(10_000));
            assertEquals("16.0kL", GT_FluidDisplayStackRenderer.formatFluidAmount(16_000));
            assertEquals("100kL", GT_FluidDisplayStackRenderer.formatFluidAmount(100_000));
            assertEquals("1.00ML", GT_FluidDisplayStackRenderer.formatFluidAmount(1_000_000));
            assertEquals("16.0ML", GT_FluidDisplayStackRenderer.formatFluidAmount(16_000_000));
            Locale.setDefault(Locale.GERMANY);
            assertEquals("16,0kL", GT_FluidDisplayStackRenderer.formatFluidAmount(16_000));
        } finally {
            Locale.setDefault(previousLocale);
        }
    }
}
