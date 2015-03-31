package edu.umass.ciir.proteus.athena.experiment;

import edu.umass.ciir.galagotools.utils.IO;
import edu.umass.ciir.galagotools.utils.io.PeekLineReader;
import edu.umass.ciir.proteus.athena.Tool;
import edu.umass.ciir.proteus.athena.cfg.Athena;
import edu.umass.ciir.proteus.athena.cfg.DataSet;
import edu.umass.ciir.proteus.athena.facts.FactQuery;
import org.lemurproject.galago.core.retrieval.Results;
import org.lemurproject.galago.core.retrieval.Retrieval;
import org.lemurproject.galago.core.retrieval.ScoredDocument;
import org.lemurproject.galago.core.retrieval.query.Node;
import org.lemurproject.galago.utility.Parameters;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author jfoley
 */
public class GenerateFactRun implements Tool {
  @Override
  public String getName() {
    return "generate-fact-run";
  }

  @Override
  public void run(final Parameters argp) throws Exception {
    final boolean progress = argp.get("progress", true);
    final String kind = argp.getString("kind");
    DataSet dataset = Athena.init(argp).getDataset();
    Retrieval ret = dataset.getRetrieval(kind);
    List<FactQuery> factQueries = dataset.getFacts(dataset.currentSplit);

    if(!argp.containsKey("requested")) {
      argp.put("requested", 1000);
    }
    final int cutoff = (int) argp.getLong("requested");
    int queryLimit = (int) argp.get("limit", 0);

    // open output after validating input
    final PrintWriter output = IO.printWriter(argp.getString("output"));
    // output run parameters for posterity
    output.println("###\t"+argp);

    final Set<String> recovered = new HashSet<String>();
    if(argp.isString("recover")) {
      GenerateFactRun.forEachRecorded(argp.getString("recover"), new IQueryAction() {
        @Override
        public void processQuery(String recoverKind, String qid, List<ScoredDocument> docs) {
          if (!recoverKind.equals(kind))
            throw new IllegalArgumentException("mismatched recover kind=" + recoverKind + " new kind=" + kind);
          if (progress) {
            System.err.println("# recover " + qid);
          }
          if (docs.size() == cutoff) {
            recovered.add(qid);
          }
          saveRun(output, kind, qid, docs);
        }
      });
      output.flush();
    }

    int numQueries = factQueries.size();
    if(queryLimit > 0) {
      numQueries = Math.min(numQueries, queryLimit);
    }
    for (int i = 0; i < numQueries; i++) {
      FactQuery query = factQueries.get(i);
      if(progress) {
        System.err.println("# query "+(i+1)+"/"+factQueries.size());
      }
      if(recovered.contains(query.id))
        continue;

      Node gq = query.getQuery(argp);
      if (gq == null) {
        System.err.println("# empty query after stopping: " + query.id);
        continue;
      }

      Node finalQuery = ret.transformQuery(gq, argp);
      Results res = ret.executeQuery(finalQuery, argp);
      List<ScoredDocument> docs = res.scoredDocuments;

      saveRun(output, kind, query.id, docs);
    }

    output.close();
    System.err.println("# done "+getName());
  }

  public static void saveRun(PrintWriter output, String kind, String qid, List<ScoredDocument> docs) {
    for (ScoredDocument doc : docs) {
      output.printf("%s\t%s\t%s\t%d\t%.5f\n", kind, qid, doc.documentName, doc.rank, doc.score);
    }
  }

  public static interface IQueryAction {
    public void processQuery(String kind, String qid, List<ScoredDocument> docs);
  }
  public static void forEachRecorded(String input, IQueryAction action) {
    PeekLineReader reader = null;
    try {
      reader = new PeekLineReader(IO.fileReader(input));

      while(true) {
        if (reader.peek() == null) {
          return;
        }
        if(reader.peek().startsWith("#")) {
          reader.next();
          continue;
        }

        String[] firstCols = reader.peek().split("\t");
        String kind = firstCols[0];
        String qid = firstCols[1];
        List<ScoredDocument> data = new ArrayList<ScoredDocument>(1000);
        while (true) {
          String nextLine = reader.peek();
          if(nextLine == null) break;
          String[] cols = nextLine.split("\t");
          if (!cols[1].equals(qid)) {
            break;
          }
          reader.next();
          String docName = cols[2];
          int rank = Integer.parseInt(cols[3]);
          double score = Double.parseDouble(cols[4]);
          data.add(new ScoredDocument(docName, rank, score));
        }

        action.processQuery(kind, qid, data);
      }
    } catch (IOException e) {
      e.printStackTrace(System.err);
    } catch (ArrayIndexOutOfBoundsException e) {
      e.printStackTrace(System.err);
    } finally {
      if(reader != null) try {
        reader.close();
      } catch (IOException e) {
        e.printStackTrace(System.err);
      }
    }
  }
}

