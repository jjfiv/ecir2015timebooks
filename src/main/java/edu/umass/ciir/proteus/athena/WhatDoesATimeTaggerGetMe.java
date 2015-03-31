package edu.umass.ciir.proteus.athena;

import edu.umass.ciir.galagotools.utils.IO;
import edu.umass.ciir.galagotools.utils.Match;
import edu.umass.ciir.proteus.athena.preprocess.SentenceIO;
import org.lemurproject.galago.utility.Parameters;

import java.util.regex.Pattern;

/**
 * @author jfoley
 */
public class WhatDoesATimeTaggerGetMe implements Tool {
  int obvious = 0;
  int hasFourDigitNumber = 0;
  int total = 0;

  @Override
  public String getName() {
    return "what-does-a-time-tagger-get-me";
  }

  @Override
  public void run(Parameters argp) throws Exception {
    final Pattern yearAsToken = Pattern.compile("\\s\\d{4}(\\s|.)");
    SentenceIO.forEachSentence(IO.file(argp.getString("input")), argp, new SentenceIO.Handler() {

      @Override
      public void process(String bookId, int pageNum, int sentenceNum, int year, String sentenceText) {
        String docId = bookId;
        String yearStr = Integer.toString(year);
        if(sentenceText.contains(yearStr)) {
          obvious++;
        } else if(Match.find(sentenceText, yearAsToken) != null) {
          hasFourDigitNumber++;
        } else if(total - (obvious + hasFourDigitNumber) < 20) {
          System.out.println(yearStr+"\t"+sentenceText);
        }

        total++;
      }
    });

    System.out.println("Obvious mentions: "+obvious);
    System.out.println("Four digit number mentions: "+hasFourDigitNumber);
    System.out.println("Total mentions: "+total);
  }

  public static void main(String[] args) throws Exception {
    Tool exp = new WhatDoesATimeTaggerGetMe();
    exp.run(Parameters.parseArray(
      "input", "data/robust04/u.robust04.sentences.tsv.gz",
      "dataset", "robust04"
    ));

  }
}
