package edu.umass.ciir.proteus.athena.dates;

import edu.umass.ciir.proteus.athena.Tool;
import edu.umass.ciir.proteus.athena.cfg.Athena;
import edu.umass.ciir.proteus.athena.cfg.DataSet;
import edu.umass.ciir.proteus.athena.preprocess.SentenceIO;
import edu.umass.ciir.proteus.athena.galago.DateTextCollector;
import edu.umass.ciir.proteus.athena.galago.DateTextWriter;
import org.lemurproject.galago.core.types.KeyValuePair;
import org.lemurproject.galago.utility.Parameters;
import org.lemurproject.galago.tupleflow.Sorter;

import java.io.File;
import java.io.IOException;

/**
 * @author jfoley
 */
public class SentenceCollector implements Tool {

  @Override
  public String getName() {
    return "sentence-collector";
  }

  @Override
  public void run(Parameters argp) throws Exception {
    final DataSet dataset = Athena.init(argp).getDataset();
    String outPath = argp.getString("output");
    File inputFile = new File(argp.getString("input"));

    final boolean pubdate = argp.get("pubdate", false);
    final boolean sentenceTags = argp.get("sentenceTags", false);

    // hand off tuples to this short Tupleflow pipeline
    final Sorter<KeyValuePair> pipeline =
      new Sorter<KeyValuePair>(new KeyValuePair.KeyOrder(),
        new DateTextCollector(),  // merge the same keys
        new DateTextWriter(outPath)); // merge the same keys, write to file

    SentenceIO.forEachSentence(inputFile, argp, new SentenceIO.Handler() {
      @Override
      public void process(String bookId, int pageNum, int sentenceNum, int year, String sentenceText) {
        if(pubdate) {
          if (dataset.getPubDateMap().containsKey(bookId)) {
            year = dataset.getPubDateMap().get(bookId);
            if(!DataSet.yearMatches(year, dataset.id))
              return;
          } else {
            return;
          }
        }
        String taggedSentence;
        if(sentenceTags) {
          taggedSentence = String.format("<s d=\"%s\" p=\"%s\" n=\"%s\">%s</s>", bookId, pageNum, sentenceNum, sentenceText);
        } else {
          taggedSentence = sentenceText;
        }
        try {
          pipeline.process(DateTextCollector.makePair(year, taggedSentence));
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    });

    // close whole pipeline
    pipeline.close();
    System.out.println("Done!");
  }
}
