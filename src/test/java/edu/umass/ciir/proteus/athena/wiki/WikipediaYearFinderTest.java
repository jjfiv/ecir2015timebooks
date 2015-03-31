package edu.umass.ciir.proteus.athena.wiki;

import edu.umass.ciir.proteus.athena.wiki.WikipediaYearFinder;
import org.junit.Test;
import static org.junit.Assert.*;

public class WikipediaYearFinderTest {

  @Test
  public void testWantTitle() throws Exception {
    assertTrue(WikipediaYearFinder.wantTitle("1"));
    assertTrue(WikipediaYearFinder.wantTitle("222 BC"));
    assertFalse(WikipediaYearFinder.wantTitle("222_BC"));
    assertFalse(WikipediaYearFinder.wantTitle("Something"));
    assertFalse(WikipediaYearFinder.wantTitle("12_Somethings"));
  }
}