package edu.umass.ciir.proteus.athena.preprocess;

import edu.umass.ciir.proteus.athena.cfg.DataSet;
import edu.umass.ciir.proteus.athena.dates.DateRecognition;
import edu.umass.ciir.galagotools.utils.IO;
import org.lemurproject.galago.utility.Parameters;

import java.io.File;

/**
 * @author jfoley
 */
public class SentenceIO {
  public static boolean lengthAcceptable(String sentence) {
    String[] terms = sentence.split("\\s+");
    return terms.length >= 3 && terms.length <= 500;
  }

  /** Hack; just set sentence=page=0 */
  public static Data parseWebTSV(String[] fields, final String dataset) {
    // parse and fit years
    String timexValue = fields[2];
    Integer year = DateRecognition.getYear(timexValue);
    if(year == null) return null;
    if(!DataSet.yearMatches(year, dataset)) return null;

    int pageNum = 0;
    int sentenceNum = 0;

    // parse and fit sentence
    String sentence = fields[3];
    if(!SentenceIO.lengthAcceptable(sentence)) return null;

    // hack around Stanford's parentheses replacement
    sentence = sentence.replaceAll("-LRB-", "(").replaceAll("-RRB-", ")").replaceAll("-LSB-", "[").replaceAll("-RSB-", "]");

    // grab document name
    String documentName = fields[0];

    return new Data(documentName, pageNum, sentenceNum, year, timexValue, sentence);
  }

  public static Data parseTSV(String tsv, final String dataset) {
    String[] fields = tsv.split("\t");
    // hack
    if(dataset.equals("robust04"))
      return parseWebTSV(fields, dataset);
    if(fields.length != 5) return null;

    // parse and fit years
    String timexValue = fields[3];
    Integer year = DateRecognition.getYear(timexValue);
    if(year == null) return null;
    if(!DataSet.yearMatches(year, dataset)) return null;

    // parse and fit numbers
    int pageNum;
    int sentenceNum;
    try {
      pageNum = Integer.parseInt(fields[1]);
      sentenceNum = Integer.parseInt(fields[2]);
    } catch (NumberFormatException nfe) {
      return null;
    }

    // parse and fit sentence
    String sentence = fields[4];
    if(!SentenceIO.lengthAcceptable(sentence)) return null;

    // grab document name
    String documentName = fields[0];

    return new Data(documentName, pageNum, sentenceNum, year, timexValue, sentence);
  }

  public static void forEachSentence(File input, Parameters argp, final Handler callback) {
    final String dataset = argp.getString("dataset");

    IO.forEachLine(input, new IO.StringFunctor() {
      @Override
      public void process(String input) {
        Data data = parseTSV(input, dataset);
        if(data == null) return;
        callback.process(data.bookId, data.pageNum, data.sentenceNum, data.year, data.sentenceText);
      }
    });
  }

  public static class Data {
    public final String bookId;
    public final int pageNum;
    public final int sentenceNum;
    public final int year;
    public final String timex;
    public final String sentenceText;

    public Data(String bookId, int pageNum, int sentenceNum, int year, String timex, String sentenceText) {
      this.bookId = bookId;
      this.pageNum = pageNum;
      this.sentenceNum = sentenceNum;
      this.year = year;
      this.timex = timex;
      this.sentenceText = sentenceText;
    }
  }

  public static interface Handler {
    public void process(String bookId, int pageNum, int sentenceNum, int year, String sentenceText);
  }
}
