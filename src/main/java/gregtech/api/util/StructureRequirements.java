package gregtech.api.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import gregtech.api.interfaces.ICasing;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_MultiBlockBase;

/**
 * Immutable requirements for positions accepting a casing or a hatch.
 * Counts live in the machine; this descriptor can be shared by every instance and by the tooltip.
 * Hatch counts use IHatchElement's machine-wide lists, including when several groups share a category.
 */
public final class StructureRequirements {
    private final ICasing casing;
    private final int minimumCasings;
    private final Map<IHatchElement, Integer> hatches;

    private StructureRequirements(Builder builder) {
        casing = builder.casing;
        minimumCasings = builder.minimumCasings;
        hatches = Collections.unmodifiableMap(new LinkedHashMap<>(builder.hatches));
    }

    public static Builder builder(ICasing casing, int minimumCasings) {
        return new Builder(casing, minimumCasings);
    }

    public ICasing getCasing() {
        return casing;
    }

    public int getMinimumCasings() {
        return minimumCasings;
    }

    public Map<IHatchElement, Integer> getHatches() {
        return hatches;
    }

    public boolean isSatisfied(GT_MetaTileEntity_MultiBlockBase machine, int casingCount) {
        return casingCount >= minimumCasings && hatches.entrySet().stream()
                .allMatch(entry -> entry.getKey().count(machine) >= entry.getValue());
    }

    public static final class Builder {
        private final ICasing casing;
        private final int minimumCasings;
        private final Map<IHatchElement, Integer> hatches = new LinkedHashMap<>();

        private Builder(ICasing casing, int minimumCasings) {
            this.casing = Objects.requireNonNull(casing);
            if (minimumCasings < 0) throw new IllegalArgumentException("Negative casing minimum");
            this.minimumCasings = minimumCasings;
        }

        /**
         * Repeated categories accumulate, just like HatchElementBuilder.atLeast.
         */
        public Builder atLeast(IHatchElement hatch, int count) {
            if (count <= 0) throw new IllegalArgumentException("Hatch minimum must be positive");
            hatches.merge(Objects.requireNonNull(hatch), count, Math::addExact);
            return this;
        }

        public Builder atLeast(IHatchElement... hatches) {
            for (IHatchElement hatch : hatches) atLeast(hatch, 1);
            return this;
        }

        public StructureRequirements build() {
            return new StructureRequirements(this);
        }
    }
}
