package edu.umass.ciir.proteus.athena.wiki;

import org.junit.Test;

public class WikiCitationFinderTest {

  @Test
  public void testCategories() {
    String inputText = "{{DEFAULTSORT:Peano Axioms}}\n"+
      "[[Category:1889 introductions]]\n"+
      "[[Category:Mathematical axioms]]\n";

    System.out.println(WikiCitationFinder.process("Peano axioms", inputText));

  }

}