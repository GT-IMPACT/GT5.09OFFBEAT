package gregtech.api.interfaces;

public interface IAxeWrenchable {
	default boolean apply() {
		return false;
	}
}