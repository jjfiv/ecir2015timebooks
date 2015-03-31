package edu.umass.ciir.proteus.athena.utils;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.lemurproject.galago.utility.Parameters;
import org.lemurproject.galago.tupleflow.Utility;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * @author jfoley
 */
public class NLP {
  public static Properties stanfordFromGalagoConfig(Parameters argp) {
    Properties props = new Properties();

    List<String> annotators = Arrays.asList("tokenize", "cleanxml", "ssplit", "pos", "lemma", "ner");
    if(argp.isList("annotators")) {
      annotators = argp.getList("annotators", String.class);
    }
    props.put("annotators", Utility.join(annotators, ","));

    return props;
  }

  public static StanfordCoreNLP instance(Parameters argp) {
    return new StanfordCoreNLP(stanfordFromGalagoConfig(argp));
  }


}
