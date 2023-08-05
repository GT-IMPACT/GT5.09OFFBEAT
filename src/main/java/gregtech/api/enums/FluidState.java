package gregtech.api.enums;

import java.util.Arrays;

public enum FluidState implements IFluidState {

    GAS(2),
    LIQUID(1),
    MOLTEN(4),
    PLASMA(3),
    SLURRY(0);

    int value;

    FluidState(int value) {
        this.value = value;
    }

    public static IFluidState[] VALID_STATES = new IFluidState[]{SLURRY, LIQUID, GAS, PLASMA, MOLTEN};

    public static IFluidState fromValue(int stateValue) {
        return Arrays.stream(VALID_STATES).filter(state -> state.getValue() == stateValue).findFirst().orElse(LIQUID);
    }

    @Override
    public String getState() {
        return name();
    }

    @Override
    public int getValue() {
        return ordinal();
    }
}

