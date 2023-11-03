package rankga;

/**
 * A utility class for converting milliseconds to a formatted time string.
 */
public class ConvertTime {

  /**
   * Converts milliseconds to a formatted time string.
   *
   * @param milliseconds The time in milliseconds to be converted.
   *
   * @return A formatted time string in the format "dd:hh:mm:ss.SSS".
   */
  public static String convertMillisToTimeFormat( long milliseconds ) {
    long seconds = milliseconds / 1000;
    long minutes = seconds / 60;
    long hours = minutes / 60;
    long days = hours / 24;

    long remainingSeconds = seconds % 60;
    long remainingMinutes = minutes % 60;
    long remainingHours = hours % 24;

    long remainingMillis = milliseconds % 1000;

    // Format the time components into a string.
    String formattedTime = String.format( "%02d:%02d:%02d:%02d.%03d",
                                          days,
                                          remainingHours,
                                          remainingMinutes,
                                          remainingSeconds,
                                          remainingMillis );

    return formattedTime;
  }

}
