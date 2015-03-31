package edu.umass.ciir.proteus.athena.dates;

import edu.umass.ciir.proteus.athena.Tool;
import edu.umass.ciir.proteus.athena.preprocess.SentenceIO;
import edu.umass.ciir.galagotools.utils.IO;
import org.lemurproject.galago.utility.Parameters;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jfoley.
 */
public class DocDateLMCollector implements Tool {
  private String currentDocument;
  private Map<Integer,StringBuilder> docYearData;
  private PrintWriter output;

  public DocDateLMCollector() {
    currentDocument = null;
    docYearData = new HashMap<Integer,StringBuilder>();
  }

  @Override
  public String getName() {
    return "doc-date-lm-collector";
  }

  @Override
  public void run(Parameters argp) throws Exception {
    File inputFile = new File(argp.getString("input"));
    String buildWhat = argp.getString("what");

    SentenceIO.Handler doWhat;
    if(buildWhat.equals("books")) {
      doWhat = new SentenceIO.Handler() {
        @Override
        public void process(String bookId, int pageNum, int sentenceNum, int year, String sentenceText) {
          processNextSentence(bookId, year, sentenceText);
        }
      };
    } else if(buildWhat.equals("pages")) {
      doWhat = new SentenceIO.Handler() {
        @Override
        public void process(String bookId, int pageNum, int sentenceNum, int year, String sentenceText) {
          String pageId = String.format("%s_%d", bookId, pageNum);
          processNextSentence(pageId, year, sentenceText);
        }
      };
    } else throw new IllegalArgumentException("No such build what="+buildWhat);

    this.output = IO.printWriter(argp.getString("output"));
    SentenceIO.forEachSentence(inputFile, argp, doWhat);

    flush();
    output.close();

    System.out.println("Done!");
  }

  public void processNextSentence(String doc, int year, String sentence) {
    if(currentDocument != null && !currentDocument.equals(doc)) {
      flush();
    }
    currentDocument = doc;

    if(!docYearData.containsKey(year)) {
      StringBuilder start = new StringBuilder();
      start.append(sentence);
      docYearData.put(year, start);
    } else {
      docYearData.get(year).append(' ').append(sentence);
    }
  }

  private void flush() {
    assert(currentDocument != null);

    for(Map.Entry<Integer,StringBuilder> kv : docYearData.entrySet()) {
      int year = kv.getKey();
      String text = kv.getValue().toString();
      output.println(currentDocument + "\t" + year + "\t" + text);
    }

    output.flush();
    docYearData = new HashMap<Integer,StringBuilder>();
  }
}
