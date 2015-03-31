package edu.umass.ciir.proteus.athena;

import edu.umass.ciir.galagotools.callback.Operation;
import edu.umass.ciir.galagotools.galago.GalagoUtil;
import org.lemurproject.galago.core.index.disk.DiskIndex;
import org.lemurproject.galago.core.index.disk.PositionIndexReader;
import org.lemurproject.galago.core.index.stats.NodeStatistics;
import org.lemurproject.galago.utility.Parameters;

import java.io.IOException;

import static edu.umass.ciir.galagotools.galago.QueryUtil.keepTerm;

/**
 * @author jfoley
 */
public class TermCounts implements Tool {
  @Override
  public String getName() {
    return "term-counts";
  }

  @Override
  public void run(Parameters argp) throws Exception {
    final boolean stop = argp.get("stop", true);
    final boolean ignoreDates = argp.get("ignoreDates", true);

    final DiskIndex index = new DiskIndex(argp.getString("index"));
    PositionIndexReader pir = (PositionIndexReader) index.getIndexPart("postings.krovetz");
    GalagoUtil.forEachKey(pir.getIterator(), new Operation<PositionIndexReader.KeyIterator>() {
      @Override
      public void process(PositionIndexReader.KeyIterator obj) throws IOException {
        String term = obj.getKeyString();
        if(keepTerm(term, stop, ignoreDates)) {
          NodeStatistics ns = obj.getValueCountSource().getStatistics();
          System.out.println(term + " " + ns.maximumCount + " " + ns.nodeDocumentCount + " " + ns.nodeFrequency);
        }
      }
    });

  }
}
