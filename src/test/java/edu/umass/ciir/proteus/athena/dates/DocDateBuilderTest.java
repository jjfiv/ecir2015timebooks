package edu.umass.ciir.proteus.athena.dates;

import edu.umass.ciir.proteus.athena.Main;
import org.junit.Assert;
import org.junit.Test;
import org.lemurproject.galago.core.btree.simple.DiskMapWrapper;
import org.lemurproject.galago.tupleflow.Utility;
import org.lemurproject.galago.utility.Parameters;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class DocDateBuilderTest {
  @Test
  public void testSimpleData() throws Exception {
    File inF = null;
    File outF = null;
    try {
			inF = File.createTempFile("asd", "jkl");
			outF = File.createTempFile("asd", "jkl");

      Utility.copyStringToFile(
          "doc0\t0\t0\t1982\tThis is the way things are, here in 1982.\n" +
          "doc0\t0\t0\t1982\tanother sentence with 1982.\n" +
          "doc0\t0\t0\t1981\tThis is the way things were, last year, in 1981.\n" +

          "doc0\t1\t0\t1982\tBut 1982 wasn't always this good.\n" +

          "doc0\t2\t0\t1783\t1783 was a year that I keep using for examples.\n" +

          "doc1\t0\t0\t783\t783 is another year.\n",
          inF
      );

      Main.main(new String[]{
          "--tool=doc-dates-builder",
          "--dataset=none",
          "--what=pages",
          "--input=" + inF.getAbsolutePath(),
          "--output=" + outF.getAbsolutePath()
      });

      DiskMapWrapper<String,String> dmr = new DiskMapWrapper<String, String>(outF, new DiskMapWrapper.StringCodec(), new DiskMapWrapper.StringCodec());

      assertEquals(4, dmr.size());

      Parameters page0 = Parameters.parseString(dmr.get("doc0_0"));
      Parameters page1 = Parameters.parseString(dmr.get("doc0_1"));
      Parameters page2 = Parameters.parseString(dmr.get("doc0_2"));
      Parameters page3 = Parameters.parseString(dmr.get("doc1_0"));

      assertEquals(2, page0.getLong("1982"));
      assertEquals(1, page0.getLong("1981"));

      assertEquals(1, page1.getLong("1982"));
      assertEquals(1, page2.getLong("1783"));
      assertEquals(1, page3.getLong("783"));

    } finally {
      Assert.assertNotNull(inF);
      Assert.assertTrue(inF.delete());
      Assert.assertNotNull(outF);
      Assert.assertTrue(outF.delete());
    }
  }

  @Test
  public void testBooks() throws Exception {
    File inF = null;
    File outF = null;
    try {
			inF = File.createTempFile("asd", "jkl");
			outF = File.createTempFile("asd", "jkl");

      Utility.copyStringToFile(
          "doc0\t0\t0\t1982\tThis is the way things are, here in 1982.\n" +
              "doc0\t0\t0\t1982\tanother sentence with 1982.\n" +
              "doc0\t0\t0\t1981\tThis is the way things were, last year, in 1981.\n" +

              "doc0\t1\t0\t1982\tBut 1982 wasn't always this good.\n" +

              "doc0\t2\t0\t1783\t1783 was a year that I keep using for examples.\n" +

              "doc1\t0\t0\t783\t783 is another year.\n",
          inF
      );

      Main.main(new String[]{
          "--tool=doc-dates-builder",
          "--dataset=none",
          "--what=books",
          "--input=" + inF.getAbsolutePath(),
          "--output=" + outF.getAbsolutePath()
      });

      DiskMapWrapper<String,String> dmr = new DiskMapWrapper<String, String>(outF, new DiskMapWrapper.StringCodec(), new DiskMapWrapper.StringCodec());

      assertEquals(2, dmr.size());

      Parameters doc0 = Parameters.parseString(dmr.get("doc0"));
      Parameters doc1 = Parameters.parseString(dmr.get("doc1"));

      assertEquals(3, doc0.getLong("1982"));
      assertEquals(1, doc0.getLong("1981"));
      assertEquals(1, doc0.getLong("1783"));
      assertEquals(1, doc1.getLong("783"));

    } finally {
      Assert.assertNotNull(inF);
      Assert.assertTrue(inF.delete());
      Assert.assertNotNull(outF);
      Assert.assertTrue(outF.delete());
    }

  }

}