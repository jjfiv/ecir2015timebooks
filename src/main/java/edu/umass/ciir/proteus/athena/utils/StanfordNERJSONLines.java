package edu.umass.ciir.proteus.athena.utils;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import edu.umass.ciir.proteus.athena.Tool;
import org.lemurproject.galago.utility.Parameters;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author jfoley.
 */
public class StanfordNERJSONLines implements Tool {
  @Override
  public String getName() {
    return "stanford-ner-json-lines";
  }

  @Override
  public void run(Parameters argp) throws Exception {
    File input = new File(argp.getString("input"));
    final StanfordCoreNLP nlp = NLP.instance(argp);

    IO.forEachLine(input, new IO.StringFunctor() {
      @Override
      public void process(String line) {
        try {
          Parameters doc = Parameters.parseString(line);
          String name = doc.getString("name");
          String text = doc.getString("text");

          Annotation document = new Annotation(text);

          System.err.println("#> "+name);
          nlp.annotate(document);
          System.err.println("#< " + name);

          List<Parameters> jsonPerWord = new ArrayList<>();

          List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
          for (CoreMap sentence : sentences) {
            for(CoreMap token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
              String word = token.get(CoreAnnotations.TextAnnotation.class);
              String ner = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
              String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
              Parameters wp = Parameters.instance();

              wp.put("term", word);
              if (ner != null && !ner.equals("O")) {
                wp.put("ner", ner);
              }
              if (pos != null) {
                wp.put("pos", pos);
              }

              jsonPerWord.add(wp);
            }
          }

          Parameters output = Parameters.instance();

          output.put("name", name);
          output.put("text", text);
          output.put("terms", jsonPerWord);

          System.out.println(output);

        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    });
  }
}
