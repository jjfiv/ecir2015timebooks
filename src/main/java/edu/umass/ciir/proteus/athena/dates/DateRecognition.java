package edu.umass.ciir.proteus.athena.dates;

import edu.umass.ciir.proteus.athena.utils.DateUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author jfoley
 */
public class DateRecognition {

  public static Pattern absDate = Pattern.compile("^(-\\d{1,10}|\\d{4})-\\d{2}-\\d{2}");
  public static Pattern monthDayOnly = Pattern.compile("^X{4}-\\d{2}-\\d{2}");
  public static Pattern monthYearOnly = Pattern.compile("^(-\\d{1,10}|\\d{4})-\\d{2}$");
  public static Pattern monthOnly = Pattern.compile("X{4}-\\d{2}");
  public static Pattern vagueYear = Pattern.compile("X{2}\\d{2}");
  public static Pattern vagueDecade = Pattern.compile("X{2}\\d{1}X");
  public static Pattern specificDecade = Pattern.compile("-?\\d{3}X");
  public static Pattern specificCentury = Pattern.compile("^-?\\d{2}X{2}");
  public static Pattern yearAD = Pattern.compile("^\\d{1,4}$");
  public static Pattern yearBC = Pattern.compile("^-\\d{1,10}$");

  public static Integer getYear(String timex) {
    if(absDate.matcher(timex).matches()) {
      return DateUtil.getFirstInteger(timex);
    } else if(monthYearOnly.matcher(timex).matches()) {
      return DateUtil.getFirstInteger(timex);
    } else if(yearAD.matcher(timex).matches()) {
      return DateUtil.getFirstInteger(timex);
    } else if(yearBC.matcher(timex).matches()) {
      return DateUtil.getFirstInteger(timex);
    }
    return null;
  }

  // heuristics built while looking at metadata from inex books
  // see associated test case
  public static Integer tryExtractMetadataYear(String expr) {
    if(expr == null) return null;

    expr = expr.replaceAll("[\\[|\\]|\\(|\\)|<|>|\\.]", " ").trim();
    // common OCR error:
    expr = expr.replace("l", "1");

    // replace circa:
    expr = expr.replaceAll("[A-Za-z]", "");

    if(expr.length() < 4) return null;

    // is this a list of dates? they're common.
    if(expr.contains(",") || expr.contains(" ") || expr.contains("-")) {
      String[] candidates = expr.split("(\\s|,|-)");
      List<Integer> intCandidates = new ArrayList<Integer>();
      for(String c : candidates) {
        Integer x = tryExtractMetadataYear(c);
        if(x != null) {
          intCandidates.add(x);
        }
      }
      if(!intCandidates.isEmpty()) {
        return Collections.min(intCandidates);
      }
    }

    //System.err.println("-> "+expr);

    try {
      if(expr.matches("^\\d{4}")) {
        return Integer.parseInt(expr.substring(0,4));
      }
      if(expr.matches("^-\\d{4}")) {
        return Integer.parseInt(expr.substring(1,5));
      }
      if(expr.matches("^\\d{3}-")) {
        return Integer.parseInt(expr.substring(0,3)+"0");
      }
    } catch (NumberFormatException nfe) {
    }

    return null;
  }
}
