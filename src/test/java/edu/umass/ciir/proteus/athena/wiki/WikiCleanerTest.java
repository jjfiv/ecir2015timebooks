package edu.umass.ciir.proteus.athena.wiki;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WikiCleanerTest {

  @Test
  public void refTest() {
    String testInput = "<ref>delete this data</ref>keep this data<ref>delete this</ref>";
    assertEquals("keep this data", WikiCleaner.removeReferences(testInput));
    assertEquals("keep this data", WikiCleaner.clean(testInput));

    String selfClosing = "<ref name=delete-me/><ref name=delete-me/ ><ref name=foo / >keep this data<ref>delete this</ref>";
    assertEquals("keep this data", WikiCleaner.removeReferences(selfClosing));
    assertEquals("keep this data", WikiCleaner.clean(selfClosing));
  }

  @Test
  public void links() {
    String testInput = "[[File:image.jpg]]keep this data[[Category:1776]]";
    assertEquals("keep this data<category>1776</category>", WikiCleaner.clean(testInput));
  }

  @Test
  public void tags() {
    String testInput = "<onlyinclude>keep this data</includeonly>";
    assertEquals("keep this data", WikiCleaner.clean(testInput));
  }

  @Test
  public void externalLinks() {
    String testInput = "[http://example.com] [http://example.com example]";
    assertEquals("<a href=\"http://example.com\">link</a> <a href=\"http://example.com\">example</a>", WikiCleaner.clean(testInput));
  }

  @Test
  public void testImage() {
    String testInput = "[[Image:image.jpg|something]]";
    assertEquals("", WikiCleaner.clean(testInput));
  }

  @Test
  public void testNested() {
    String testInput = "[[Image:image.jpg|[[link]]]]";
    assertEquals("", WikiCleaner.clean(testInput));
  }

  @Test
  public void testHeaders() {
    assertEquals("<h2>H2</h2>\n", WikiCleaner.convertHeaders(" == H2"));
    assertEquals("<h2>H 2</h2>\n", WikiCleaner.convertHeaders(" == H 2  "));
    assertEquals("Anything else == 0\n", WikiCleaner.convertHeaders("Anything else == 0"));
  }
}