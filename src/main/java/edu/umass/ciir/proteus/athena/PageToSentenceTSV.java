package edu.umass.ciir.proteus.athena;

import ciir.proteus.parse.MBTEIPageParser;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import edu.umass.ciir.galagotools.utils.IO;
import edu.umass.ciir.proteus.athena.utils.NLP;
import edu.umass.ciir.galagotools.utils.Util;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.parse.DocumentStreamParser;
import org.lemurproject.galago.core.types.DocumentSplit;
import org.lemurproject.galago.utility.Parameters;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

/**
 * @author jfoley.
 */
public class PageToSentenceTSV implements Tool {

  @Override
  public String getName() {
    return "page-to-sentences";
  }

  @Override
  public void run(Parameters argp) throws Exception {
    DocumentStreamParser.addExternalParsers(argp);
    final PrintWriter output = IO.printWriter(argp.getString("output"));
    List<File> files = Util.checkAndExpandPaths(argp.getAsList("input", String.class));
    boolean isListFile = argp.getBoolean("listFile");
    if(isListFile) {
      files = Util.collectLines(files);
    }
    final StanfordCoreNLP nlp = NLP.instance(Parameters.parseArray("annotators",
      Arrays.asList("tokenize", "cleanxml", "ssplit")));

    for(File file : files) {
      System.err.println("# File: " + file.getAbsolutePath());

      MBTEIPageParser pageParser = null;
      try {
        DocumentSplit split = new DocumentSplit();
        split.fileName = file.getCanonicalPath();
        pageParser = new MBTEIPageParser(split, argp);
        while (true) {
          Document page = pageParser.nextDocument();
          if (page == null) break;
          handlePage(page, nlp, output);
        }
      } catch(Exception ex) {
        System.err.println("# Fail: "+file);
        ex.printStackTrace();
      } finally {
        output.flush();
        if(pageParser != null) pageParser.close();
      }
    }

    output.close();
  }

  private void handlePage(Document page, StanfordCoreNLP nlp, PrintWriter output) {
    System.out.println(page.name+ ": "+page.text.length());
    int pageNumber = Integer.parseInt(page.metadata.get("pageNumber"));
    String identifier = page.metadata.get("identifier");

    Annotation nlpDoc = new Annotation(page.text);
    nlp.annotate(nlpDoc);
    java.util.List<CoreMap> get = nlpDoc.get(CoreAnnotations.SentencesAnnotation.class);
    for (int sentenceId = 0; sentenceId < get.size(); sentenceId++) {
      CoreMap sentence = get.get(sentenceId);
      StringBuilder sentenceData = new StringBuilder();

      for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
        String term = token.get(CoreAnnotations.TextAnnotation.class);
        sentenceData.append(term).append(' ');
      }

      output.println(String.format("%s\t%d\t%d\t%s",
        identifier, pageNumber, sentenceId, sentenceData.toString()));
    }
  }

  public static void main(String[] args) throws Exception {
    Main.main(new String[] {"--tool=page-to-sentence-index", "--input=/home/jfoley/code/books/inex/indices/pages-corpus", "--output=foo"});
  }
}
