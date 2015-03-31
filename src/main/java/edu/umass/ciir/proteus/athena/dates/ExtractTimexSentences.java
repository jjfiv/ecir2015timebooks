package edu.umass.ciir.proteus.athena.dates;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.time.Timex;
import edu.stanford.nlp.util.CoreMap;
import edu.umass.ciir.proteus.athena.Tool;
import edu.umass.ciir.proteus.athena.parser.MBTEIPageParser;
import edu.umass.ciir.proteus.athena.utils.IO;
import edu.umass.ciir.proteus.athena.utils.NLP;
import edu.umass.ciir.proteus.athena.utils.Util;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.types.DocumentSplit;
import org.lemurproject.galago.utility.Parameters;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static edu.stanford.nlp.time.TimeAnnotations.TimexAnnotation;

public class ExtractTimexSentences implements Tool {

  public static class SentenceInfo {
    public String documentName;
    public int pageNumber;
    public int sentenceNumber;
    public String timexValue;
    public String sentence;

    public String toTSV() {
      return documentName + "\t" + pageNumber + "\t" + sentenceNumber + "\t" + timexValue + "\t" + sentence;
    }
  }

  public static List<SentenceInfo> extractFromSinglePage(StanfordCoreNLP nlp, String text) {
    ArrayList<SentenceInfo> sentencesToKeep = new ArrayList<SentenceInfo>();
    Annotation document = new Annotation(text);

    System.err.println("# begin annotate");
    nlp.annotate(document);
    System.err.println("# end annotate");

    List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

    for (int sentenceId = 0; sentenceId < sentences.size(); sentenceId++) {
      CoreMap sentence = sentences.get(sentenceId);
      int keepTimex = 0;
      Timex sentenceTimex = null;
      StringBuilder sentenceStr = new StringBuilder();
      String lastTid = "";
      for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
        String term = token.get(CoreAnnotations.TextAnnotation.class);
        sentenceStr.append(term).append(' ');
        Timex timex = token.get(TimexAnnotation.class);
        if (timex == null || timex.value() == null)
          continue;

        // only take the first timex
        if (timex.tid().equals(lastTid))
          continue;
        lastTid = timex.tid();
        if (!timex.timexType().equals("DATE"))
          continue;
        if (!timex.value().matches("[\\dX-]+")) {
          continue;
        }
        //System.err.println(timex);
        keepTimex++;
        sentenceTimex = timex;
      }

      // only save it if we have exactly one matching timex
      if (keepTimex != 1)
        continue; // next sentence

      SentenceInfo info = new SentenceInfo();
      info.sentenceNumber = sentenceId;
      info.timexValue = sentenceTimex.value();
      info.sentence = sentenceStr.toString();
      sentencesToKeep.add(info);
    }
    return sentencesToKeep;
  }


  @Override
  public String getName() {
    return "extract-timex-sentences";
  }

  @Override
  public void run(Parameters argp) throws IOException, XMLStreamException {
    PrintWriter out = new PrintWriter(argp.getString("output"));
    List<File> files = Util.checkAndExpandPaths(argp.getAsList("input", String.class));
    boolean isListFile = argp.getBoolean("listFile");

    if(isListFile) {
      files = Util.collectLines(files);
    }

    final Set<String> blacklistNames = new HashSet<String>();
    if(argp.isString("blacklist")) {
      IO.forEachLine(new File(argp.getString("blacklist")), new IO.StringFunctor() {
				@Override
				public void process(String input) {
					if (!input.trim().isEmpty()) {
						blacklistNames.add(input.trim());
					}
				}
			});
    }

    StanfordCoreNLP nlp = NLP.instance(argp);
    System.err.println("# Properties: " + nlp.getProperties());

    for(File file : files) {
      if(blacklistNames.contains(file.getName())) {
        System.err.println("# Skip: " + file.getAbsolutePath());
        continue;
      }

      System.err.println("# File: " + file.getAbsolutePath());

      MBTEIPageParser pageParser = null;
      try {
        DocumentSplit split = new DocumentSplit();
        split.fileName = file.getCanonicalPath();
        pageParser = new MBTEIPageParser(split, argp);
        while (true) {
          Document page = pageParser.nextDocument();
          if (page == null) break;

          for (SentenceInfo info : extractFromSinglePage(nlp, page.text)) {
            info.documentName = page.metadata.get("identifier");
            info.pageNumber = Integer.parseInt(page.metadata.get("pageNumber"));
            out.println(info.toTSV());
          }
        }
      } catch(Exception ex) {
        System.err.println("# Fail: "+file);
        ex.printStackTrace();
      } finally {
        out.flush();
        if(pageParser != null) pageParser.close();
      }
    }
    out.close();
  }

  public static void main(String[] args) throws Exception {
    Parameters argp = Parameters.parseArgs(args);
    Tool tool = new ExtractTimexSentences();
    tool.run(argp);
  }
}
