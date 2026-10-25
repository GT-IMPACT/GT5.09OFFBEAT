package gregtech.api.interfaces.metatileentity;

import gregtech.api.metatileentity.implementations.GT_MEOutputStorage;

/** Settings and cell storage shared by the item and fluid ME outputs. */
public interface IMEOutput {
    GT_MEOutputStorage<?> getMEOutputStorage();
    long getMEOutputAmount();
}
