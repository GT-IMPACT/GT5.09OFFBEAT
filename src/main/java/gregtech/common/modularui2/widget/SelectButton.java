package gregtech.common.modularui2.widget;

import com.cleanroommc.modularui.widgets.ToggleButton;

/** GTNH radio button: selecting another linked value releases the previously selected button. */
public class SelectButton extends ToggleButton {

    @Override
    public Result onMousePressed(int mouseButton) {
        if ((mouseButton == 0 || mouseButton == 1) && getState() == 0) {
            return super.onMousePressed(mouseButton);
        }
        return Result.IGNORE;
    }
}
