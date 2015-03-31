package edu.umass.ciir.proteus.athena.cfg;

import edu.umass.ciir.galagotools.galago.GalagoUtil;
import edu.umass.ciir.proteus.athena.experiment.DateReranker;
import edu.umass.ciir.proteus.athena.experiment.ScoredDate;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.junit.Assert;
import org.junit.Test;
import org.lemurproject.galago.core.btree.simple.DiskMapBuilder;
import org.lemurproject.galago.core.btree.simple.DiskMapWrapper;
import org.lemurproject.galago.core.retrieval.ScoredDocument;
import org.lemurproject.galago.tupleflow.FileUtility;
import org.lemurproject.galago.utility.ByteUtil;
import org.lemurproject.galago.utility.Parameters;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExtractDatesTest {

  private static byte[] $(Parameters p) {
    return ByteUtil.fromString(p.toString());
  }
  private static byte[] $(String s) {
    return ByteUtil.fromString(s);
  }
  private static ScoredDocument SD(String name, double score) {
    ScoredDocument sd = new ScoredDocument();
    sd.documentName = name;
    sd.score = score;
    return sd;
  }

  private static List<ScoredDocument> mkRankedList(ScoredDocument... inputs) {
    ArrayList<ScoredDocument> results = new ArrayList<ScoredDocument>(inputs.length);
    for (int i = 0; i < inputs.length; i++) {
      ScoredDocument sdoc = inputs[i];
      sdoc.rank = i;
      results.add(sdoc);
    }
    return results;
  }

  @Test
  public void testCollectPubDates() throws Exception {
    final List<ScoredDocument> fakeData = mkRankedList(
        SD("doc0", 1.0),
        SD("doc-missing", 0.5),
        SD("doc1", 0.3)
    );
    Assert.assertEquals(3, fakeData.size());

    TObjectIntHashMap<String> pubdates = new TObjectIntHashMap<String>();
    pubdates.put("doc17", 1777);
    pubdates.put("doc0", 1888);
    pubdates.put("doc1", 1999);

    List<ScoredDate> dates = ExtractDates.toDates(ExtractDates.collectPubDates(fakeData, pubdates));

    Assert.assertEquals(2, dates.size());

    Assert.assertEquals(1.0, dates.get(0).score, 0.001);
    Assert.assertEquals(0.3, dates.get(1).score, 0.001);

    Assert.assertEquals(1888, dates.get(0).year);
    Assert.assertEquals(1999, dates.get(1).year);
  }

  @Test
  public void testCollectDocDates() throws IOException {
    final List<ScoredDocument> fakeData = mkRankedList(
      SD("doc0", 1.0),
      SD("doc-missing", 0.5),
      SD("doc1", 0.3)
    );
    Assert.assertEquals(3, fakeData.size());

    File tmp = null;
    try {
      tmp = FileUtility.createTemporary();
      DiskMapBuilder dmb = new DiskMapBuilder(tmp.getAbsolutePath());
      dmb.put($("doc0"), $(Parameters.parseArray(
          "1980", 3,
          "1981", 1
      )));
      dmb.put($("doc1"), $(Parameters.parseArray(
          "1980", 3,
          "1981", 11
      )));
      dmb.close();

      DiskMapWrapper<String,String> fromDisk = new DiskMapWrapper<String, String>(tmp, new DiskMapWrapper.StringCodec(), new DiskMapWrapper.StringCodec());

      Assert.assertArrayEquals(new String[]{"doc0", "doc-missing", "doc1"}, GalagoUtil.names(fakeData).toArray());
      Assert.assertArrayEquals(new String[]{"doc0", "doc-missing", "doc1"}, GalagoUtil.names(fakeData).toArray());

      Assert.assertTrue(fromDisk.containsKey("doc0"));
      Assert.assertTrue(fromDisk.containsKey("doc1"));

      Assert.assertEquals(3, GalagoUtil.names(fakeData).size());
      Assert.assertEquals(2, fromDisk.bulkGet(GalagoUtil.names(fakeData)).size());

      List<ScoredDocument> output = ExtractDates.collectDocDates(fakeData, fromDisk);
      Assert.assertEquals(3+1+3+11, output.size());

      List<ScoredDate> results= DateReranker.process("uniform", ExtractDates.toDates(output));
      Assert.assertEquals(1981, results.get(0).year);
      Assert.assertEquals(1, results.get(0).rank);
      Assert.assertEquals(12.0, results.get(0).score, 0.001);
      Assert.assertEquals(1980, results.get(1).year);
      Assert.assertEquals(2, results.get(1).rank);
      Assert.assertEquals(6.0, results.get(1).score, 0.001);

    } finally {
      if(tmp != null) {
        Assert.assertTrue(tmp.delete());
      }
    }
  }
}