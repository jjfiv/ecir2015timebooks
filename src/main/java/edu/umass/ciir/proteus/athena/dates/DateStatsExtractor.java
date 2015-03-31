package edu.umass.ciir.proteus.athena.dates;

import edu.umass.ciir.proteus.athena.Tool;
import edu.umass.ciir.galagotools.utils.IO;
import gnu.trove.map.hash.TIntIntHashMap;
import org.lemurproject.galago.utility.Parameters;

import java.io.File;

/**
 * @author jfoley.
 */
public class DateStatsExtractor implements Tool {

  @Override
  public String getName() {
    return "date-stats-extractor";
  }

  @Override
  public void run(Parameters argp) throws Exception {
    final TIntIntHashMap booksDates = new TIntIntHashMap();
    final TIntIntHashMap robustDates = new TIntIntHashMap();

    System.err.println("Find book dates");
    IO.forEachLine(new File("data/dated-sentences/usentences3.tsv.gz"), new IO.StringFunctor() {
      @Override
      public void process(String input) {
        String[] fields = input.split("\t");
        // parse and fit years
        String timexValue = fields[3];
        Integer year = DateRecognition.getYear(timexValue);
        if(year == null) return;

        if (year < 0 || year > 2025) {return;}
        booksDates.adjustOrPutValue(10*(year/10), 1, 1);
      }
    });

    System.err.println("Find robust dates");
    IO.forEachLine(new File("data/robust04/u.robust04.sentences.tsv.gz"), new IO.StringFunctor() {
      @Override
      public void process(String input) {
        String[] fields = input.split("\t");
        // parse and fit years
        String timexValue = fields[2];
        Integer year = DateRecognition.getYear(timexValue);
        if(year == null) return;

        if (year < 0 || year > 2025) {return;}
        robustDates.adjustOrPutValue(10*(year/10), 1, 1);
      }
    });

    System.out.println("Year\tBooks\tRobust");
    for(int yr = -25; yr <= 2025; yr++) {
      System.out.printf("%d\t%d\t%d\n", yr, booksDates.get(yr), robustDates.get(yr));
    }
  }
}
