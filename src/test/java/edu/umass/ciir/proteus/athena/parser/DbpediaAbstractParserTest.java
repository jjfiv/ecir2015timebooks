package edu.umass.ciir.proteus.athena.parser;

import org.junit.Test;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.parse.DocumentStreamParser;
import org.lemurproject.galago.core.util.DocumentSplitFactory;
import org.lemurproject.galago.tupleflow.Utility;
import org.lemurproject.galago.utility.Parameters;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class DbpediaAbstractParserTest {
  @Test
  public void simpleParse() throws IOException {
    String data = "<http://dbpedia.org/resource/Anarchism> <http://dbpedia.org/ontology/abstract> \"\"@en .\n" +
        "<http://dbpedia.org/resource/Autism> <http://dbpedia.org/ontology/abstract> \"Autism is a disorder of neural development characterized by impaired social interaction and communication, and by restricted and repetitive behavior.\"@en .\n" +
        "<http://dbpedia.org/resource/Achilles> <http://dbpedia.org/ontology/abstract> \"In Greek mythology, Achilles was a Greek hero of the Trojan War and the central character and greatest warrior of Homer's Iliad.\"@en .\n";

    File tmp = File.createTempFile("fake-dbpedia-abstracts", ".ttl");
    try {
      Utility.copyStringToFile(data, tmp);
      DocumentStreamParser ps = new DbpediaAbstractParser(DocumentSplitFactory.file(tmp), Parameters.instance());

      Document autism = ps.nextDocument();
      assertNotNull(autism);
      Document achilles = ps.nextDocument();
      assertNotNull(achilles);
      assertNull(ps.nextDocument());
      assertNull(ps.nextDocument());

      assertEquals("Autism", autism.name);
      assertEquals("Achilles", achilles.name);
      assertEquals("<title>Achilles</title>\n<body>In Greek mythology, Achilles was a Greek hero of the Trojan War and the central character and greatest warrior of Homer's Iliad.</body>", achilles.text);

    } finally {
      assertTrue(tmp.delete());
    }

  }

}