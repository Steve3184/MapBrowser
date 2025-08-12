package top.steve3184.mapbrowser;

import me.friwi.jcefmaven.EnumProgress;
import me.friwi.jcefmaven.IProgressHandler;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * A progress handler for the JCEF installation process.
 * It uses a logger for general status updates and System.out for detailed
 * progress bars to provide clear, non-spammy feedback.
 */
public class FancyProgressHandler implements IProgressHandler {
    private final Logger logger;
    private EnumProgress currentState = null;
    private int lastDisplayedPercent = -1;

    /**
     * Constructs a new progress handler.
     * @param logger The logger to use for status updates.
     */
    public FancyProgressHandler(Logger logger) {
        this.logger = logger;
    }

    /**
     * Handles progress updates from the JCEF installation process.
     *
     * @param state The current stage of the process (e.g., DOWNLOADING).
     * @param percent The progress percentage for the current stage (-1 if indeterminate).
     */
    @Override
    public void handleProgress(EnumProgress state, float percent) {
        Objects.requireNonNull(state, "State cannot be null");

        // --- Handle State Changes ---
        if (state != currentState) {
            // If the previous state had a progress bar, print a final "100%" to complete its line.
            if (currentState == EnumProgress.DOWNLOADING || currentState == EnumProgress.EXTRACTING) {
                logger.info("Downloading JCEF bundle: 100%...");
            }

            currentState = state;
            lastDisplayedPercent = -1;
            String message = getHumanReadableState(state);

            // Use System.out for progress bars, otherwise use the logger.
            if (state == EnumProgress.DOWNLOADING || state == EnumProgress.EXTRACTING) {
                // Use System.out.print to allow appending percentages on the same line.
                System.out.print(message + ": ");
            } else if (state == EnumProgress.INITIALIZED) {
                // Log the final completion message.
                logger.info(message + "... Done.");
            } else {
                // Log all other state changes as distinct info messages.
                logger.info(message + "...");
            }
        }

        // --- Handle Percentage Updates ---
        // This block only runs for states that have a visual progress bar.
        if ((state == EnumProgress.DOWNLOADING || state == EnumProgress.EXTRACTING) && percent >= 0) {
            int currentBracket = (int) (percent / 10) * 10;
            // Only print an update for every 10% increment to avoid spam.
            if (currentBracket > lastDisplayedPercent) {
                logger.info("Downloading JCEF bundle: "+ currentBracket + "%...");
                lastDisplayedPercent = currentBracket;
            }
        }
    }

    /**
     * Converts an EnumProgress state into a user-friendly string.
     *
     * @param state The state to convert.
     * @return A human-readable string describing the state.
     */
    private String getHumanReadableState(EnumProgress state) {
        return switch (state) {
            case LOCATING -> "Locating JCEF bundle";
            case DOWNLOADING -> "Downloading JCEF bundle";
            case EXTRACTING -> "Extracting JCEF bundle";
            case INSTALL -> "Installing JCEF";
            case INITIALIZING -> "Initializing JCEF";
            case INITIALIZED -> "JCEF Initialized";
        };
    }
}