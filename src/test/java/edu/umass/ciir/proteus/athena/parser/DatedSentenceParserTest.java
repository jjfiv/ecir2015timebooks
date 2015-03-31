package edu.umass.ciir.proteus.athena.parser;

import org.junit.Test;
import org.lemurproject.galago.core.parse.DocumentStreamParser;
import org.lemurproject.galago.core.types.DocumentSplit;
import org.lemurproject.galago.core.util.DocumentSplitFactory;
import org.lemurproject.galago.tupleflow.FileUtility;
import org.lemurproject.galago.utility.Parameters;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DatedSentenceParserTest {

  @Test
  public void selectsDatedSentenceParser() throws IOException {
    File tmp = null;
    try {
      tmp = FileUtility.createTemporary();
      Parameters buildP = Parameters.parseArray("filetype", DatedSentenceParser.class.getName(), "dataset", "none");
      DocumentSplit fakeSplit = DocumentSplitFactory.file(tmp);
      DatedSentenceParser dsp = (DatedSentenceParser) DocumentStreamParser.instance(fakeSplit, buildP);
      assertEquals("none", dsp.conf.getString("dataset"));
    } finally {
      if(tmp != null) assertTrue(tmp.delete());
    }
  }

}