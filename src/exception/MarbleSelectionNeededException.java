// In package exception;
package exception;

public class MarbleSelectionNeededException extends InvalidMarbleException {

    public MarbleSelectionNeededException() {
        super("Please select a marble for this action. You have playable marbles on the board.");
    }

    public MarbleSelectionNeededException(String message) {
        super(message);
    }
}