package edu.umass.ciir.proteus.athena.dates;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.umass.ciir.proteus.athena.Tool;
import edu.umass.ciir.proteus.athena.galago.GalagoUtil;
import edu.umass.ciir.proteus.athena.utils.DateUtil;
import edu.umass.ciir.proteus.athena.utils.NLP;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.parse.DocumentStreamParser;
import org.lemurproject.galago.core.types.DocumentSplit;
import org.lemurproject.galago.tupleflow.Utility;
import org.lemurproject.galago.utility.Parameters;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author jfoley.
 */
public class ExtractWebTimexSentences implements Tool {
  @Override
  public String getName() {
    return "extract-web-timex-sentences";
  }

  @Override
  public void run(Parameters argp) throws Exception {
    boolean robustFixDates = argp.get("robust04Dates", false);

    Set<String> blackList = new HashSet<String>();
    if(argp.isString("recover")) {
      blackList = Utility.readFileToStringSet(new File(argp.getString("recover")));
    }

    List<DocumentSplit> inputs = GalagoUtil.getDocumentSplits(argp.getAsList("input", String.class), argp);
    if(inputs.isEmpty()) {
      throw new IllegalArgumentException("Input set is empty!");
    }
    System.err.println("# found "+inputs.size()+" input documents!");

    StanfordCoreNLP nlp = NLP.instance(argp);

    PrintWriter out = new PrintWriter(argp.getString("output"));
    for(DocumentSplit split : inputs) {
      System.err.println("# "+split.fileName);
      DocumentStreamParser parser = DocumentStreamParser.instance(split, argp);
      while(true) {
        Document doc = parser.nextDocument();
        if(doc == null) break;

        if(blackList.contains(doc.name)) {
          System.err.println("# Skip: "+doc.name);
          continue;
        }

        System.err.println("# "+doc.name);

        try {
          if(robustFixDates) {
            // patch robust weird date formats
            doc.text = DateUtil.fixRobustDates(doc.text);
          }
          String text = doc.text;

          List<ExtractTimexSentences.SentenceInfo> sentences = ExtractTimexSentences.extractFromSinglePage(nlp, text);

          for (ExtractTimexSentences.SentenceInfo sinfo : sentences) {

            out.printf("%s\t%s\t%s\t%s\n",
              doc.name,
              Parameters.parseMap(doc.metadata).toString(),
              sinfo.timexValue,
              sinfo.sentence);
          }
        } catch (Exception ex) {
          System.err.println("# Fail: "+split.fileName+"/"+doc.name);
          ex.printStackTrace(System.err);
        }
      }
      parser.close();
    }
    out.close();
  }
}
