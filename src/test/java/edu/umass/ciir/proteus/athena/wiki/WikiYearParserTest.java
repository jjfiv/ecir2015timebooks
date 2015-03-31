package edu.umass.ciir.proteus.athena.wiki;

import edu.umass.ciir.proteus.athena.utils.SGML;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WikiYearParserTest {
  @Test
  public void getTitle() {
    assertEquals("data", SGML.getTagContents("<title>data</title>", "title"));
    assertEquals("1872 BC", SGML.getTagContents("<title>1872 BC</title>", "title"));
  }

  @Test
  public void testEventSection() {
    assertTrue(EventSectionFinder.hasEventSection("== Events =="));
    assertTrue(EventSectionFinder.hasEventSection("== Events. =="));
    assertTrue(EventSectionFinder.hasEventSection("= Events  ="));
    assertTrue(EventSectionFinder.hasEventSection("==Events=="));
    assertTrue(EventSectionFinder.hasEventSection("==Event=="));
    assertTrue(EventSectionFinder.hasEventSection("==event=="));

    // A.D. 774 in our dump doesn't have a event tag, only a by place tag...
    assertTrue(EventSectionFinder.hasEventSection("== by place =="));

    // 1932 just starts with a "January" header
    assertTrue(EventSectionFinder.hasEventSection("== January =="));
  }

  @Test
  public void testNewlines() {
    String data = "* this is a list\n" +
      "* still a list\n" +
      "  part of the second bullet";

    List<WikiYearParser.WikiForm> lines = WikiYearParser.tokenize("fake", data);
    assertEquals(1, lines.get(0).listLevel);
    assertEquals(0, lines.get(0).headerLevel);
    assertEquals("list", lines.get(0).kind);
    assertEquals("this is a list", lines.get(0).text);

    assertEquals(1, lines.get(1).listLevel);
    assertEquals(0, lines.get(1).headerLevel);
    assertEquals("list", lines.get(1).kind);
    assertEquals("still a list part of the second bullet", lines.get(1).text);

    assertEquals(2, lines.size());
  }

  @Test
  public void parseHeader() {
    String data = "=== Header ===\n" +
      "Paragraph\n";
    List<WikiYearParser.WikiForm> lines = WikiYearParser.tokenize("fake", data);

    assertEquals(3, lines.get(0).headerLevel);
    assertEquals(0, lines.get(0).listLevel);
    assertEquals("header", lines.get(0).kind);
    assertEquals("Header", lines.get(0).text);

    assertEquals(3, lines.get(1).headerLevel);
    assertEquals(0, lines.get(1).listLevel);
    assertEquals("paragraph", lines.get(1).kind);
    assertEquals("Paragraph", lines.get(1).text);

    assertEquals(2, lines.size());
  }

  @Test
  public void parseHeaderResetsList() {
    String data = "=== Header ===\n" +
      "* list here\n" +
      "== header two ==\n" +
      "* list here\n";
    List<WikiYearParser.WikiForm> lines = WikiYearParser.tokenize("fake", data);

    assertEquals(3, lines.get(0).headerLevel);
    assertEquals(0, lines.get(0).listLevel);
    assertEquals("header", lines.get(0).kind);
    assertEquals("Header", lines.get(0).text);

    assertEquals(3, lines.get(1).headerLevel);
    assertEquals(1, lines.get(1).listLevel);
    assertEquals("list", lines.get(1).kind);
    assertEquals("list here", lines.get(1).text);

    assertEquals(2, lines.get(2).headerLevel);
    assertEquals(0, lines.get(2).listLevel);
    assertEquals("header", lines.get(2).kind);
    assertEquals("header two", lines.get(2).text);

    assertEquals(2, lines.get(3).headerLevel);
    assertEquals(1, lines.get(3).listLevel);
    assertEquals("list", lines.get(3).kind);
    assertEquals("list here", lines.get(3).text);

    assertEquals(4, lines.size());
  }

  @Test
  public void parseToTree() {
    String data = "=== H ===\n" +
      "P\n" +
      "* L1\n" +
      "** L1.1\n"+
      "** L1.2\n" +
      "* L2\n" +
      "** L2.1\n";

    List<WikiYearParser.WikiForm> lines = WikiYearParser.tokenize("fake", data);
    WikiYearParser.WikiSection section = WikiYearParser.parseTree("fake", lines, "page");

    //System.out.println(section.toString());

    assertEquals(1, section.size());
    assertEquals("fake", section.text);

    WikiYearParser.WikiSection H = section.get(0);

    assertEquals(2, H.size());
    assertEquals("H", H.text);

    WikiYearParser.WikiSection L1 = H.get(0);
    assertEquals("L1", L1.text);
    assertEquals(2, L1.size());
    assertEquals("L1.1", L1.get(0).text);
    assertEquals(0, L1.get(0).size());
    assertEquals("L1.2", L1.get(1).text);
    assertEquals(0, L1.get(1).size());

    WikiYearParser.WikiSection L2 = H.get(1);
    assertEquals("L2", L2.text);
    assertEquals(1, L2.size());
    assertEquals("L2.1", L2.get(0).text);
    assertEquals(0, L2.get(0).size());
  }

  @Test
  public void parseParagraphs() {
    String data = "=== H ===\n" +
      "P0\n" +
      "== H1 ==\n"+
      "P1\n" +
      "== H2 ==\n"+
      "P2\n";

    List<WikiYearParser.WikiForm> lines = WikiYearParser.tokenize("fake", data);
    WikiYearParser.WikiSection section = WikiYearParser.parseTree("fake", lines, "page");

    //System.out.println(section.toString());

    assertEquals(1, section.size());
    assertEquals("fake", section.text);

    WikiYearParser.WikiSection H = section.get(0);

    assertEquals(2, H.size());
    assertEquals("H", H.text);
    assertEquals(1, H.paragraphs.size());
    assertEquals("P0", H.paragraphs.get(0));

    assertEquals("H1", H.get(0).text);
    assertEquals("H2", H.get(1).text);
    assertEquals("P1", H.get(0).paragraphs.get(0));
    assertEquals("P2", H.get(1).paragraphs.get(0));
  }

  @Test
  public void testBulletJoin() {
    assertEquals("2000\tJanuary 17 ...fact.", WikiYearParser.bulletJoin(Arrays.asList("2000", "January"), "January 17 ...fact."));
    assertEquals("2000\tABC January. Something else.", WikiYearParser.bulletJoin(Arrays.asList("2000", "ABC", "January"), "Something else."));
  }

  @Test
  public void parseBlankPage() {
    String data = "==Events==\n" +
      "*\n" +
      "\n" +
      "==Births==\n" +
      "*\n" +
      "\n" +
      "==Deaths==\n" +
      "*\n" +
      "\n";
    List<WikiYearParser.WikiForm> lines = WikiYearParser.tokenize("583 BC", data);
    WikiYearParser.WikiSection section = WikiYearParser.parseTree("583 BC", lines, "page");
    WikiYearParser.cleanSections(section);
    assertTrue(section.isLeaf());
    assertTrue(WikiYearParser.convertToFacts(section).isEmpty());
  }

}
