package edu.umass.ciir.proteus.athena.experiment;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class DateRerankerTest {

  private static ScoredDate mk(double score, int year) { return new ScoredDate(0, score, year); }

  private static List<ScoredDate> setRanks(ScoredDate... dates) {
    List<ScoredDate> data = new ArrayList<ScoredDate>(Arrays.asList(dates));
    for (int i = 0; i < data.size(); i++) {
      data.get(i).rank = i+1;
    }
    return data;
  }

  public int[] getYears(List<ScoredDate> dates) {
    int years[] = new int[dates.size()];
    for (int i = 0; i < dates.size(); i++) {
      years[i] = dates.get(i).year;
    }
    return years;
  }

  public String[] getScores(List<ScoredDate> dates) {
    String scores[] = new String[dates.size()];
    for (int i = 0; i < dates.size(); i++) {
      scores[i] = String.format("%.2f",dates.get(i).score);
    }
    return scores;
  }

  private void checkRank(List<ScoredDate> results) {
    for(int i=0; i<results.size(); i++) {
      assertEquals(i+1, results.get(i).rank);
    }
  }

  @Test
  public void testUniform() throws Exception {
    final List<ScoredDate> dates = setRanks(
      mk(10.0, 1802),
      mk(10.0, 1802),
      mk(10.0, 1800),
      mk(10.0, 1800),
      mk(10.0, 1800),
      mk(10.0, 1800),
      mk(10.0, 1801),
      mk(10.0, 1801),
      mk(10.0, 1801),
      mk(10.0, 1803),
      mk(10.0, 1803),
      mk(10.0, 1804),
      mk(10.0, 1804),
      mk(10.0, 1805)
    );
    List<ScoredDate> uniform = DateReranker.process("uniform", dates);
    checkRank(uniform);
    assertArrayEquals(new int[]{1800, 1801, 1802, 1803, 1804, 1805}, getYears(uniform));
  }

  @Test
  public void testTakeFirst() {
    final List<ScoredDate> dates = setRanks(
      mk(10.0, 1802),
      mk(10.0, 1802),
      mk(10.0, 1800),
      mk(10.0, 1800),
      mk(10.0, 1800),
      mk(10.0, 1800),
      mk(10.0, 1801),
      mk(10.0, 1801),
      mk(10.0, 1801),
      mk(10.0, 1803),
      mk(10.0, 1803),
      mk(10.0, 1804),
      mk(10.0, 1804),
      mk(10.0, 1805)
    );

    List<ScoredDate> res = DateReranker.process("takeFirst", dates);
    checkRank(res);
    assertArrayEquals(new int[] {1802,1800,1801,1803,1804,1805}, getYears(res));
  }

  @Test
  public void testRecipRankWeight() throws Exception {
    final List<ScoredDate> dates = setRanks(
      mk(10.0, 1801), //1.0
      mk(10.0, 1802), //0.5
      mk(10.0, 1800), //0.33
      mk(10.0, 1800), //0.25
      mk(10.0, 1800), //0...
      mk(10.0, 1800),
      mk(10.0, 1800),
      mk(10.0, 1800),
      mk(10.0, 1800),
      mk(10.0, 1800)
    );
    List<ScoredDate> results = DateReranker.process("recipRankWeight", dates);
    checkRank(results);
    assertArrayEquals(new String[] {"1.43","1.00","0.50"}, getScores(results));
    assertArrayEquals(new int[] {1800,1801,1802}, getYears(results));
  }

  @Test
  public void testNormalizedWeights() throws Exception {
    final List<ScoredDate> dates = setRanks(
      mk(-5.9, 1801),
      mk(-6.0, 1802),
      mk(-6.0, 1802),
      mk(-6.0, 1802),
      mk(-7.0, 1800)
    );

    List<ScoredDate> results = DateReranker.process("rm", dates);
    checkRank(results);
    assertArrayEquals(new int[] {1802,1801,1800}, getYears(results));
  }

  @Test
  public void testAllMethods() throws Exception {
    final List<ScoredDate> dates = setRanks(
      mk(2, 1801),
      mk(1, 1802)
    );

    for(String method : DateReranker.Methods) {
      List<ScoredDate> results = DateReranker.process(method, dates);
      checkRank(results);
      assertArrayEquals(new int[]{1801, 1802}, getYears(results));
    }

    try {
      DateReranker.process("bogus-method", dates);
      fail("bogus-method should have triggered an exception");
    } catch(IllegalArgumentException iae) {
      assertNotNull(iae);
    }
  }
}