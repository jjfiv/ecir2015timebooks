package edu.umass.ciir.proteus.athena.utils;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author jfoley
 */
public class DateUtil {
  public static String nearestDecade(int year) {
    if(year <= 0) {
      //System.err.println("Input: "+year);
      year = -1*(year-1);
      //System.err.println("Flip: "+year);
      year = (year / 10) * 10;
      //System.err.println("Scale Down: "+year);
      return Integer.toString(year)+"BC";
    } else {
      return Integer.toString((year / 10) * 10);
    }
  }

  private static String[] months = {
    "january",
    "february",
    "march",
    "april",
    "may",
    "june",
    "july",
    "august",
    "september",
    "october",
    "november",
    "december",
  };
  private static final Set<String> monthSet = new TreeSet<>(Arrays.asList(months));

  public static boolean isMonthDay(String text) {
    if(!text.contains(" "))
      return false;

    String[] parts = text.split("\\s");
    if(parts.length != 2)
      return false;

    try {
      if(!StrUtil.looksLikeInt(parts[1], 2)) {
        return false;
      }
      if (!isMonth(parts[0])) {
        return false;
      }

      int day = Integer.parseInt(parts[1]);
      if(day <= 31)
        return true;
    } catch (NumberFormatException nfe) {
      return false;
    }
    return false;
  }

  public static boolean isMonth(String text) {
    return monthSet.contains(text.toLowerCase());
  }

  public static boolean isYear(String term) {
    return StrUtil.looksLikeInt(term, 4);
  }
  public static int YearFromString(String rel) {
    int year = getFirstInteger(rel);
    if(rel.contains("BC"))
      return 1-year;
    return year;
  }

  public static Integer getFirstInteger(String expr) {
    Matcher matcher = Pattern.compile("^(-?\\d*)").matcher(expr);
    if(matcher.find()) {
      return Integer.parseInt(matcher.group(0));
    }
    return null;
  }

  public static String YearToString(int year) {
    if(year <= 0) {
      year *=-1;
      year ++;
      return String.format("%dBC", year);
    }
    return Integer.toString(year);
  }

  // look for the year and month day slammed together, because StanfordNLP timex can't do that one.
  public static Pattern sixDigitDate = Pattern.compile("(91|92|93|94)(\\d{2})(\\d{2})");
  public static String fixRobustDates(String text) {
    StringBuilder sb = new StringBuilder();

    Matcher match = sixDigitDate.matcher(text);
    int prevEnd = 0;
    while(match.find()) {
      int matchStart = match.start();
      int matchEnd = match.end();
      String YY = match.group(1);
      String MM = match.group(2);
      String DD = match.group(3);
      sb.append(text.substring(prevEnd, matchStart));
      sb.append("19").append(YY).append('-').append(MM).append('-').append(DD);
      prevEnd = matchEnd;
    }
    sb.append(text.substring(prevEnd));

    return sb.toString();
  }
}
