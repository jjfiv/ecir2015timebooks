package edu.umass.ciir.proteus.athena.preprocess;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.umass.ciir.proteus.athena.Tool;
import edu.umass.ciir.proteus.athena.cfg.DataSet;
import edu.umass.ciir.proteus.athena.dates.DateRecognition;
import edu.umass.ciir.proteus.athena.dates.ExtractTimexSentences;
import edu.umass.ciir.proteus.athena.utils.DateUtil;
import edu.umass.ciir.proteus.athena.utils.NLP;
import edu.umass.ciir.proteus.athena.utils.SGML;
import org.lemurproject.galago.core.btree.simple.DiskMapBuilder;
import org.lemurproject.galago.core.index.corpus.CorpusReader;
import org.lemurproject.galago.core.index.disk.DiskIndex;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.tupleflow.Utility;
import org.lemurproject.galago.utility.ByteUtil;
import org.lemurproject.galago.utility.Parameters;

import java.io.File;
import java.util.List;

/**
 * @author jfoley
 */
public class RobustCollectPubDates implements Tool {

  @Override
  public String getName() {
    return "robust-collect-pub-dates";
  }

  @Override
  public void run(Parameters argp) throws Exception {
    File input = new File(argp.getString("index"));
    DiskIndex index = new DiskIndex(input.getAbsolutePath());
    CorpusReader corpusReader = (CorpusReader) index.getIndexPart("corpus");

    StanfordCoreNLP nlp = NLP.instance(argp);

    File output = new File(argp.getString("output"));
    DiskMapBuilder dmb = new DiskMapBuilder(output.getAbsolutePath());

    for(CorpusReader.KeyIterator iterator = corpusReader.getIterator(); !iterator.isDone(); iterator.nextKey()) {
      Document doc = iterator.getDocument(Document.DocumentComponents.JustText);
      String docName = doc.name;
      String text = doc.text;
      if(text.contains("<DATE>")) {
        String innerDate = DateUtil.fixRobustDates(SGML.getTagContents(text, "DATE"));

        List<ExtractTimexSentences.SentenceInfo> sentences = ExtractTimexSentences.extractFromSinglePage(nlp, innerDate);

        if(sentences.isEmpty()) {
          continue;
        }

        String firstTimex = sentences.get(0).timexValue;
        Integer year = DateRecognition.getYear(firstTimex);
        if(year == null) continue;
        if(!DataSet.yearMatches(year, argp.get("dataset","robust04"))) continue;

        dmb.put(ByteUtil.fromString(docName), Utility.fromInt(year));
      }
    }
    dmb.close();
    System.out.println("## DONE");
  }
}
