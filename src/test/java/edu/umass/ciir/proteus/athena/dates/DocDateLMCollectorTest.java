package edu.umass.ciir.proteus.athena.dates;

import edu.umass.ciir.proteus.athena.Main;
import edu.umass.ciir.proteus.athena.parser.DocDateSketchParser;
import org.junit.Assert;
import org.junit.Test;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.parse.DocumentStreamParser;
import org.lemurproject.galago.core.types.DocumentSplit;
import org.lemurproject.galago.core.util.DocumentSplitFactory;
import org.lemurproject.galago.tupleflow.FileUtility;
import org.lemurproject.galago.utility.Parameters;
import org.lemurproject.galago.tupleflow.Utility;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DocDateLMCollectorTest {
  @Test
  public void testSimpleData() throws Exception {
    File inF = null;
    File outF = null;
    try {
      inF = FileUtility.createTemporary();
      outF = FileUtility.createTemporary();

      Utility.copyStringToFile(
        "doc0\t0\t0\t1982\tThis is the way things are, here in 1982.\n" +
        "doc0\t0\t0\t1981\tThis is the way things were, last year, in 1981.\n" +
        "doc0\t0\t0\t1982\tBut 1982 wasn't always this good.\n" +
        "doc1\t0\t0\t1783\t1783 was a year that I keep using for examples.\n",
        inF
      );

      Main.main(new String[]{
        "--tool=doc-date-lm-collector",
        "--dataset=none",
        "--what=books",
        "--input=" + inF.getAbsolutePath(),
        "--output=" + outF.getAbsolutePath()
      });

      DocumentSplit written = DocumentSplitFactory.file(outF);
      DocumentStreamParser docsSP = new DocDateSketchParser(written, Parameters.instance());

      List<Document> docs = new ArrayList<Document>();
      while(true) {
        Document d = docsSP.nextDocument();
        if(d == null) break;
        docs.add(d);
      }

      Assert.assertEquals(3, docs.size());
      Assert.assertEquals("doc0", docs.get(0).metadata.get("book"));
      Assert.assertEquals("doc0", docs.get(1).metadata.get("book"));
      Assert.assertEquals("doc1", docs.get(2).metadata.get("book"));
      Assert.assertEquals("1783", docs.get(2).metadata.get("year"));
      String year0 = docs.get(0).metadata.get("year");
      String year1 = docs.get(1).metadata.get("year");
      Assert.assertTrue(("1981".equals(year0) && "1982".equals(year1)) || ("1982".equals(year0) && "1981".equals(year1)));


    } finally {
      Assert.assertNotNull(inF);
      Assert.assertTrue(inF.delete());
      Assert.assertNotNull(outF);
      Assert.assertTrue(outF.delete());
    }

  }


  @Test
  public void testByPages() throws Exception {
    File inF = null;
    File outF = null;
    try {
      inF = FileUtility.createTemporary();
      outF = FileUtility.createTemporary();

      Utility.copyStringToFile(
          "doc0\t0\t0\t1982\tThis is the way things are, here in 1982.\n" +
              "doc0\t0\t0\t1981\tThis is the way things were, last year, in 1981.\n" +
              "doc0\t0\t0\t1982\tBut 1982 wasn't always this good.\n" +
              "doc0\t1\t0\t1783\t1783 was a year that I keep using for examples.\n",
          inF
      );

      Main.main(new String[]{
          "--tool=doc-date-lm-collector",
          "--dataset=none",
          "--what=pages",
          "--input=" + inF.getAbsolutePath(),
          "--output=" + outF.getAbsolutePath()
      });

      DocumentSplit written = DocumentSplitFactory.file(outF);
      DocumentStreamParser docsSP = new DocDateSketchParser(written, Parameters.instance());

      List<Document> docs = new ArrayList<Document>();
      while(true) {
        Document d = docsSP.nextDocument();
        if(d == null) break;
        docs.add(d);
      }

      Assert.assertEquals(3, docs.size());
      Assert.assertEquals("doc0_0", docs.get(0).metadata.get("book"));
      Assert.assertEquals("doc0_0", docs.get(1).metadata.get("book"));
      Assert.assertEquals("doc0_1", docs.get(2).metadata.get("book"));
      Assert.assertEquals("1783", docs.get(2).metadata.get("year"));
      String year0 = docs.get(0).metadata.get("year");
      String year1 = docs.get(1).metadata.get("year");
      Assert.assertTrue(("1981".equals(year0) && "1982".equals(year1)) || ("1982".equals(year0) && "1981".equals(year1)));


    } finally {
      Assert.assertNotNull(inF);
      Assert.assertTrue(inF.delete());
      Assert.assertNotNull(outF);
      Assert.assertTrue(outF.delete());
    }

  }

  @Test
  public void testFilterYears() throws Exception {
    File inF = null;
    File outF = null;
    try {
      inF = FileUtility.createTemporary();
      outF = FileUtility.createTemporary();

      Utility.copyStringToFile(
        "doc0\t0\t0\t1982\tThis is the way things are, here in 1982.\n" +
          "doc0\t0\t0\t1981\tThis is the way things were, last year, in 1981.\n" +
          "doc0\t0\t0\t1982\tBut 1982 wasn't always this good.\n" +
          "doc1\t0\t0\t1783\t1783 was a year that I keep using for examples.\n",
        inF
      );

      Main.main(new String[]{
        "--tool=doc-date-lm-collector",
        "--dataset=books",
        "--what=books",
        "--input=" + inF.getAbsolutePath(),
        "--output=" + outF.getAbsolutePath()
      });

      DocumentSplit written = DocumentSplitFactory.file(outF);
      DocumentStreamParser docsSP = new DocDateSketchParser(written, Parameters.instance());

      List<Document> docs = new ArrayList<Document>();
      while(true) {
        Document d = docsSP.nextDocument();
        if(d == null) break;
        docs.add(d);
      }

      Assert.assertEquals(1, docs.size());
      Assert.assertEquals("doc1", docs.get(0).metadata.get("book"));
      Assert.assertEquals("1783", docs.get(0).metadata.get("year"));
    } finally {
      Assert.assertNotNull(inF);
      Assert.assertTrue(inF.delete());
      Assert.assertNotNull(outF);
      Assert.assertTrue(outF.delete());
    }

  }
}