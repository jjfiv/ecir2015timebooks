package edu.umass.ciir.proteus.athena.preprocess;

import edu.umass.ciir.proteus.athena.Tool;
import edu.umass.ciir.proteus.athena.dates.DateRecognition;
import org.lemurproject.galago.core.btree.simple.DiskMapBuilder;
import org.lemurproject.galago.core.index.corpus.CorpusReader;
import org.lemurproject.galago.core.index.disk.DiskIndex;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.tupleflow.Utility;
import org.lemurproject.galago.utility.ByteUtil;
import org.lemurproject.galago.utility.Parameters;

import java.io.File;
import java.util.Map;

/**
 * @author jfoley
 */
public class CollectPubDates implements Tool {
  @Override
  public String getName() {
    return "collect-pub-dates";
  }

  @Override
  public void run(Parameters argp) throws Exception {
    File input = new File(argp.getString("index"));
    DiskIndex index = new DiskIndex(input.getAbsolutePath());
    File output = new File(argp.getString("output"));
    DiskMapBuilder dmb = new DiskMapBuilder(output.getAbsolutePath());

    CorpusReader corpusReader = (CorpusReader) index.getIndexPart("corpus");

    for(CorpusReader.KeyIterator iterator = corpusReader.getIterator(); !iterator.isDone(); iterator.nextKey()) {
      Document doc = iterator.getDocument(Document.DocumentComponents.JustMetadata);
      String docName = doc.name;
      Map<String,String> metadata = doc.metadata;

      String date = metadata.get("date");
      if(date == null || date.trim().isEmpty()) {
        continue;
      }
      Integer year = DateRecognition.tryExtractMetadataYear(date);
      if(year == null) {
        if(date.contains("?")) continue;
        if(date.contains("--")) continue;
        if(date.contains("n.d")) continue;
        if(date.contains("s.d")) continue;
        //System.out.println("# fail: "+docName + "\t" + date);
        continue;
      }
      dmb.put(ByteUtil.fromString(docName), Utility.fromInt(year));
    }
    dmb.close();
    System.out.println("## DONE");
  }
}
