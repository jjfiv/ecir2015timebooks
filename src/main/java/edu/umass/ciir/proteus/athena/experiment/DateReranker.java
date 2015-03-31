package edu.umass.ciir.proteus.athena.experiment;

import org.lemurproject.galago.utility.MathUtils;

import java.util.*;

/**
 * @author jfoley
 */
public class DateReranker {
  public static String[] Methods = new String[] {
    "uniform",
    "takeFirst",
    "recipRankWeight",
    "rm"
  };

  public static List<ScoredDate> rank(Map<Integer,ScoredDate> scores) {
    final List<ScoredDate> dates = new ArrayList<ScoredDate>(scores.size());

    // put in a list
    for (ScoredDate date : scores.values()) {
      dates.add(date);
    }

    // sort and reassign rank
    Collections.sort(dates, ScoredDate.ByScoreThenRank);
    int rank = 1;
    for(ScoredDate x : dates) {
      x.rank = rank++;
    }

    return dates;
  }

  public static List<ScoredDate> process(String method, List<ScoredDate> inputDates) {
    // don't bother reranking an empty list (only happens for pubdate)
    if(inputDates.isEmpty())
      return Collections.emptyList();

    // just for my sanity, make sure we're in the right order
    Collections.sort(inputDates, ScoredDate.ByRankThenScore);

    // copy them so we can modify score
    ArrayList<ScoredDate> dates = new ArrayList<ScoredDate>(inputDates.size());
    for(ScoredDate sdate : inputDates) {
      dates.add(sdate.clone());
    }

    if(method.equals("uniform")) {
      return collectUniformWeighting(dates);
    } else if(method.equals("none") || method.equals("takeFirst")) {
      return takeFirst(dates);
    } else if(method.equals("recipRankWeight")) {
      return reciprocalRankWeighting(dates);
    } else if(method.equals("rm")) {
      return rmWeighting(dates);
    } else {
      throw new IllegalArgumentException("No such rerankMethod="+method);
    }
  }

  // Implementation here is based on the Relevance Model unigram normaliztion in Indri & Galago.
  private static List<ScoredDate> rmWeighting(ArrayList<ScoredDate> dates) {
    Map<Integer, ScoredDate> scores = new HashMap<Integer, ScoredDate>(dates.size());

    double[] values = new double[dates.size()];
    for (int i = 0; i < dates.size(); i++) {
      ScoredDate date = dates.get(i);
      values[i] = date.score;
    }
    double logSumExp = MathUtils.logSumExp(values);

    for(ScoredDate date : dates) {
      if(!scores.containsKey(date.year)) {
        scores.put(date.year, date);
        date.score = 0;
      }
      double weight = Math.exp(date.score-logSumExp);
      scores.get(date.year).score += weight;
    }

    return rank(scores);
  }

  private static List<ScoredDate> reciprocalRankWeighting(ArrayList<ScoredDate> dates) {
    Map<Integer, ScoredDate> scores = new HashMap<Integer, ScoredDate>(dates.size());
    for(ScoredDate date : dates) {
      if(!scores.containsKey(date.year)) {
        scores.put(date.year, date);
        date.score = 0;
      }
      double weight = 1.0 / (double) date.rank;
      scores.get(date.year).score += weight;
    }

    return rank(scores);
  }

  private static List<ScoredDate> collectUniformWeighting(List<ScoredDate> dates) {
    Map<Integer, ScoredDate> scores = new HashMap<Integer, ScoredDate>(dates.size());
    for(ScoredDate date : dates) {
      if(!scores.containsKey(date.year)) {
        scores.put(date.year, date);
        date.score = 0;
      }
      scores.get(date.year).score += 1;
    }

    return rank(scores);
  }

  private static List<ScoredDate> takeFirst(List<ScoredDate> dates) {
    Map<Integer, ScoredDate> scores = new HashMap<Integer, ScoredDate>(dates.size());
    for(ScoredDate date : dates) {
      if(!scores.containsKey(date.year)) {
        scores.put(date.year, date);
      }
    }

    return rank(scores);
  }
}
