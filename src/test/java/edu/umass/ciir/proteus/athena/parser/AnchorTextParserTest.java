package edu.umass.ciir.proteus.athena.parser;

import edu.umass.ciir.galagotools.utils.SGML;
import org.junit.Test;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.parse.DocumentStreamParser;
import org.lemurproject.galago.core.util.DocumentSplitFactory;
import org.lemurproject.galago.tupleflow.Utility;
import org.lemurproject.galago.utility.Parameters;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class AnchorTextParserTest {
  @Test
  public void testSimple() throws IOException {
    String data = "B\tA\tabout A\n" +
        "C\tA\tC about A\n" +
        "C\tB\tC about B\n" +
        "D\tList_of_A\tignored\n" +
        "E\tD\n";

    File tmp = File.createTempFile("anchorTextParserTest", "tsv");
    try {
      Utility.copyStringToFile(data, tmp);
      DocumentStreamParser parser = new AnchorTextParser(DocumentSplitFactory.file(tmp), Parameters.instance());

      Document anchorA = parser.nextDocument();
      assertNotNull(anchorA);
      Document anchorB = parser.nextDocument();
      assertNotNull(anchorB);
      Document anchorD = parser.nextDocument();
      assertNotNull(anchorD);
      assertNull(parser.nextDocument());
      assertNull(parser.nextDocument());

      assertEquals("B\nC\n", SGML.getTagContents(anchorA.text, "link-pages"));
      assertEquals("C\n", SGML.getTagContents(anchorB.text, "link-pages"));
      assertEquals("E\n", SGML.getTagContents(anchorD.text, "link-pages"));

      assertEquals("about A\nC about A\n", SGML.getTagContents(anchorA.text, "link-text"));
      assertEquals("C about B\n", SGML.getTagContents(anchorB.text, "link-text"));
      assertEquals("D\n", SGML.getTagContents(anchorD.text, "link-text"));


    } finally {
      assertTrue(tmp.delete());
    }
  }
}