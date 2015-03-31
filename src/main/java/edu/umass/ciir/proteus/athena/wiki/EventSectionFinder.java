package edu.umass.ciir.proteus.athena.wiki;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author jfoley.
 */
public class EventSectionFinder {
  // Find a wikipedia events section we can parse
  private static final String eventHeader = "={1,4} *(E|e)vents?\\.? *={1,4}";
  private static final String placeHeader = "={1,4} *(B|b)y (P|p)lace\\.? *={1,4}";
  private static final String timeHeader = "={1,4} *(J|j)anuary\\.? *={1,4}";
  public static Pattern eventRegex = Pattern.compile(
    "("+eventHeader+"|"+placeHeader+"|"+timeHeader+")"
  );

  public static int findEventSection(String mediawiki) {
    Matcher matcher = eventRegex.matcher(mediawiki);
    if(matcher.find()) {
      return matcher.start();
    }
    return -1;
  }

  public static boolean hasEventSection(String mediawiki) {
    return findEventSection(mediawiki) >= 0;
  }
}
