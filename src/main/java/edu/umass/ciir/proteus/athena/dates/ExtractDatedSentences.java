package edu.umass.ciir.proteus.athena.dates;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.umass.ciir.galagotools.galago.GalagoUtil;
import edu.umass.ciir.galagotools.utils.IO;
import edu.umass.ciir.galagotools.utils.StrUtil;
import edu.umass.ciir.proteus.athena.Tool;
import edu.umass.ciir.proteus.athena.utils.NLP;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.parse.DocumentStreamParser;
import org.lemurproject.galago.core.types.DocumentSplit;
import org.lemurproject.galago.utility.Parameters;

import java.io.PrintWriter;
import java.util.*;

/**
 * @author jfoley
 */
public class ExtractDatedSentences implements Tool {
  @Override
  public String getName() {
    return "extract-dated-sentences";
  }

  @Override
  public void run(Parameters argp) throws Exception {

    final Set<String> finished = new HashSet<>();
    if(argp.containsKey("previous")) {
      List<String> previousOutput = argp.getAsList("previous", String.class);
      for (String prev : previousOutput) {
        IO.forEachLine(IO.file(prev), new IO.StringFunctor() {
          @Override
          public void process(String input) {
            if(input.startsWith("#")) {
              finished.add(StrUtil.removeFront(input, "#"));
            }
          }
        });
      }
    }

    List<DocumentSplit> inputs = GalagoUtil.getDocumentSplits(argp.getAsList("input", String.class), argp);
    PrintWriter out = new PrintWriter(argp.getString("output"));

    if(inputs.isEmpty()) {
      throw new IllegalArgumentException("Input set is empty!");
    }
    System.err.println("# found "+inputs.size()+" input documents!");

    StanfordCoreNLP nlp = NLP.instance(argp);

    for(DocumentSplit split : inputs) {
      System.err.println("# "+split.fileName);
      try (DocumentStreamParser parser = DocumentStreamParser.instance(split, argp)) {
        for (Document doc : GalagoUtil.documentsStreamIterable(parser)) {
          if (finished.contains(doc.name)) continue;

          System.err.println("# " + doc.name);
          try {
            String text = doc.text;
            // hack for silly \\N documents when dealing with wex index
            if (text.length() <= 3) continue;

            List<ExtractTimexSentences.SentenceInfo> sentences = ExtractTimexSentences.extractFromSinglePage(nlp, text);

            for (ExtractTimexSentences.SentenceInfo sinfo : sentences) {
              out.printf("%s\t%s\t%s\n",
                doc.name,
                sinfo.timexValue,
                sinfo.sentence);
            }
            out.println("#" + doc.name); //finish document mark...
          } catch (Exception ex) {
            System.err.println("# Fail: " + split.fileName + "/" + doc.name);
            ex.printStackTrace(System.err);
          }
        }
      }
    }
    out.close();
  }
}

