package edu.umass.ciir.proteus.athena.dates;

import edu.umass.ciir.proteus.athena.Tool;
import edu.umass.ciir.proteus.athena.preprocess.SentenceIO;
import edu.umass.ciir.proteus.athena.utils.IO;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.procedure.TIntIntProcedure;
import org.lemurproject.galago.core.btree.simple.DiskMapBuilder;
import org.lemurproject.galago.utility.ByteUtil;
import org.lemurproject.galago.utility.Parameters;

import java.io.File;
import java.io.IOException;

/**
 * Build a diskmapreader from the "tagged"-sentences output
 * Note that if your sentences aren't sorted, this is going to go poorly for you.
 * @author jfoley.
 */
public class DocDatesBuilder implements Tool {
  private DiskMapBuilder out;
  private TIntIntHashMap dateCounts;
  private String currentId;

  public DocDatesBuilder() {
    out = null;
    dateCounts = new TIntIntHashMap();
    currentId = null;
  }

  @Override
  public String getName() {
    return "doc-dates-builder";
  }

  public void run(Parameters argp) throws Exception {
    File inputFile = new File(argp.getString("input"));
    String buildWhat = argp.getString("what");

    SentenceIO.Handler handler;
    if(buildWhat.equals("pages")) {
      handler = new SentenceIO.Handler() {
        @Override
        public void process(String bookId, int pageNum, int sentenceNum, int year, String sentenceText) {
          String pageId = String.format("%s_%d", bookId, pageNum);
          processDate(pageId, year);
        }
      };
    } else if(buildWhat.equals("books")) {
      handler = new SentenceIO.Handler() {
        @Override
        public void process(String bookId, int pageNum, int sentenceNum, int year, String sentenceText) {
          processDate(bookId, year);
        }
      };
    } else throw new IllegalArgumentException("No such build what="+buildWhat);

    out = new DiskMapBuilder(argp.getString("output"));
    SentenceIO.forEachSentence(inputFile, argp, handler);

    flush();
    IO.close(out);

    System.out.println("Done!");
  }

  private void flush() {
    final Parameters p = Parameters.instance();
    dateCounts.forEachEntry(new TIntIntProcedure() {
      @Override
      public boolean execute(int year, int count) {
        p.put(Integer.toString(year), count);
        return true;
      }
    });
    try {
      assert(currentId != null);
      out.put(ByteUtil.fromString(currentId), ByteUtil.fromString(p.toString()));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void processDate(String thisId, int year) {
    assert(thisId != null);
    if(currentId != null && !currentId.equals(thisId)) {
      flush();
      dateCounts = new TIntIntHashMap();
    }
    currentId = thisId;
    dateCounts.adjustOrPutValue(year, 1, 1);
  }
}
