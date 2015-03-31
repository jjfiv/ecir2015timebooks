package edu.umass.ciir.proteus.athena.experiment;

import edu.umass.ciir.proteus.athena.Tool;
import edu.umass.ciir.proteus.athena.cfg.Athena;
import edu.umass.ciir.proteus.athena.cfg.AthenaKind;
import edu.umass.ciir.proteus.athena.cfg.DataSet;
import edu.umass.ciir.proteus.athena.utils.IO;
import edu.umass.ciir.proteus.athena.utils.Util;
import gnu.trove.list.array.TDoubleArrayList;
import org.lemurproject.galago.core.retrieval.ScoredDocument;
import org.lemurproject.galago.utility.Parameters;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author jfoley.
 */
public class EvaluateRun implements Tool {
  @Override
  public String getName() {
    return "evaluate-run";
  }

  @Override
  public void run(final Parameters argp) throws Exception {
    final boolean progress = argp.get("progress", true);
    final DataSet dataset = Athena.init(argp).getDataset();
    String runFile = argp.getString("input");
    final Map<String,Set<Integer>> qrel = dataset.getQRel(argp.getString("qrelKind"));
    final PrintWriter output = IO.printWriter(argp.getString("output"));

    // evaluate differently than run?
    final String runKindId = argp.get("runKind", dataset.currentKind);
    final AthenaKind theKind = dataset.getKind(runKindId);
    final String dateMethod = argp.get("dateMethod", theKind.getDateMethod());

    GenerateFactRun.forEachRecorded(runFile, new GenerateFactRun.IQueryAction() {
      @Override
      public void processQuery(String kindId, String qid, List<ScoredDocument> docs) {
        if (!kindId.equals(runKindId)) {
          throw new IllegalArgumentException("Loaded runKind=" + kindId + " expected runKind=" + runKindId);
        }
        if (progress) System.err.println("qid="+qid);

        // no such qrel (another split?)
        if (!qrel.containsKey(qid))
          return;

        Set<Integer> relDates = qrel.get(qid);

        List<ScoredDate> dateDocuments;
        try {
          dateDocuments = theKind.extractDates(dateMethod, docs);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        for(String rerankerMethod : DateReranker.Methods) {
          List<ScoredDate> rankedDates = DateReranker.process(rerankerMethod, dateDocuments);

          double recip_rank, ap;
          double ndcg, ndcg5, ndcg10, ndcg20;
          double sqErr1, sqErr2, sqErr3, sqErr4, sqErr5;
          double p1, p5, p10;
          double err1, err5, err10;
          if(rankedDates.isEmpty()) {
            recip_rank = 0;
            ap = 0;
            ndcg = ndcg5 = ndcg10 = ndcg20 = 0;
            p1 = p5 = p10 = 0;
            err1 = err5 = err10 = 0;
            double halfSpan = dataset.yearSpan()/2.0;
            sqErr1 = sqErr2 = sqErr3 = sqErr4 = sqErr5 = halfSpan * halfSpan;
          } else {
            recip_rank = calculateRecipRank(rankedDates, relDates);
            ap = calculateAveragePrecision(rankedDates, relDates);
            ndcg = calculateNDCG(rankedDates, relDates);
            ndcg5 = calculateNDCG(Util.take(rankedDates, 5), relDates);
            ndcg10 = calculateNDCG(Util.take(rankedDates, 10), relDates);
            ndcg20 = calculateNDCG(Util.take(rankedDates, 20), relDates);
            sqErr1 = calcSquareError(rankedDates, relDates, 1);
            sqErr2 = calcSquareError(rankedDates, relDates, 2);
            sqErr3 = calcSquareError(rankedDates, relDates, 3);
            sqErr4 = calcSquareError(rankedDates, relDates, 4);
            sqErr5 = calcSquareError(rankedDates, relDates, 5);
            p1 = calcPrecision(rankedDates, relDates, 1);
            p5 = calcPrecision(rankedDates, relDates, 5);
            p10 = calcPrecision(rankedDates, relDates, 10);
            err1 = calcAbsError(rankedDates, relDates, 1);
            err5 = calcAbsError(rankedDates, relDates, 5);
            err10 = calcAbsError(rankedDates, relDates, 10);
          }

          printId(rerankerMethod);
          output.printf("\t%s\trecip_rank\t%.5f\n", qid, recip_rank);
          printId(rerankerMethod);
          output.printf("\t%s\tap\t%.5f\n", qid, ap);
          printId(rerankerMethod);
          output.printf("\t%s\tndcg\t%.5f\n", qid, ndcg);
          printId(rerankerMethod);
          output.printf("\t%s\tndcg@5\t%.5f\n", qid, ndcg5);
          printId(rerankerMethod);
          output.printf("\t%s\tndcg@10\t%.5f\n", qid, ndcg10);
          printId(rerankerMethod);
          output.printf("\t%s\tndcg@20\t%.5f\n", qid, ndcg20);

          printId(rerankerMethod);
          output.printf("\t%s\tsqErr@1\t%.5f\n", qid, sqErr1);
          printId(rerankerMethod);
          output.printf("\t%s\tsqErr@2\t%.5f\n", qid, sqErr2);
          printId(rerankerMethod);
          output.printf("\t%s\tsqErr@3\t%.5f\n", qid, sqErr3);
          printId(rerankerMethod);
          output.printf("\t%s\tsqErr@4\t%.5f\n", qid, sqErr4);
          printId(rerankerMethod);
          output.printf("\t%s\tsqErr@5\t%.5f\n", qid, sqErr5);

          printId(rerankerMethod);
          output.printf("\t%s\tp@1\t%.5f\n", qid, p1);
          printId(rerankerMethod);
          output.printf("\t%s\tp@5\t%.5f\n", qid, p5);
          printId(rerankerMethod);
          output.printf("\t%s\tp@10\t%.5f\n", qid, p10);

          printId(rerankerMethod);
          output.printf("\t%s\terr@1\t%.5f\n", qid, err1);
          printId(rerankerMethod);
          output.printf("\t%s\terr@5\t%.5f\n", qid, err5);
          printId(rerankerMethod);
          output.printf("\t%s\terr@10\t%.5f\n", qid, err10);
        }
      }

      public void printId(String rerankerMethod) {
        // col1: run identifier
        output.printf("%s:%s:%s:%s", dataset.currentKind, dataset.currentSplit, dateMethod, rerankerMethod);
      }
    });
    output.close();
  }

  /**
   * Calculate P@k.
   * blatantly implemented by hand
   */
  private double calcPrecision(List<ScoredDate> rankedDates, Set<Integer> relYears, int rank) {
    int relCount = 0;
    for(int i=0; i<rank && i<rankedDates.size(); i++) {
      final int retrievedYear = rankedDates.get(i).year;
      if(relYears.contains(retrievedYear)) {
        relCount++;
      }
    }

    return (double) relCount / (double) rank;
  }

  /**
   * Calculate RR/MRR.
   * blatantly implemented by hand
   */
  public static double calculateRecipRank(List<ScoredDate> rankedDates, Set<Integer> relYears) {
    int firstRel = 0;
    for (ScoredDate date : rankedDates) {
      if (relYears.contains(date.year)) {
        firstRel = date.rank;
        break;
      }
    }

    if(firstRel > 0) {
      return 1.0 / ((double) firstRel);
    }
    return 0.0;
  }

  /**
   * Calculate AP.
   * blatantly refactored from galago
   */
  public static double calculateAveragePrecision(List<ScoredDate> rankedDates, Set<Integer> relYears) {
    if(relYears.isEmpty()) return 0.0;

    double sumPrecision = 0.0;
    int releventSoFar = 0;

    for (ScoredDate date : rankedDates) {
      if (relYears.contains(date.year)) {
        releventSoFar++;
        sumPrecision += (releventSoFar / (double) date.rank);
      }
    }

    return sumPrecision / relYears.size();
  }

  /**
   * Calculate NDCG.
   * blatantly refactored from galago
   */
  public static double calculateNDCG(List<ScoredDate> rankedDates, Set<Integer> relYears) {
    if(relYears.isEmpty()) return 0.0;

    // parameters
    final double IdealScore = 1.0;
    int numRetrieved = rankedDates.size();

    // compute dcg:
    TDoubleArrayList actualGain = new TDoubleArrayList(numRetrieved);
    for (ScoredDate doc : rankedDates) {
      double value = 0.0;
      if(relYears.contains(doc.year)) { value = IdealScore; }
      actualGain.add(value);
    }
    double dcg = computeDCG(actualGain, numRetrieved);

    TDoubleArrayList idealGain = new TDoubleArrayList(numRetrieved);
    for(int i=0; i<relYears.size(); i++) {
      idealGain.add(IdealScore);
    }
    // put the best ones at the beginning
    idealGain.sort(); idealGain.reverse();

    double max = computeDCG(idealGain, numRetrieved);
    return dcg / max;
  }

  /**
   * Calculate DCG.
   * blatantly refactored from galago
   */
  private static double computeDCG(TDoubleArrayList gains, int numRetrieved) {
    double dcg = 0.0;
    for (int i = 0; i < Math.min(gains.size(), numRetrieved); i++) {
      dcg += (Math.pow(2, gains.get(i)) - 1.0) / Math.log(i + 2);
    }
    return dcg;
  }

  /**
   * MSE
   */
  public static double calcSquareError(List<ScoredDate> rankedDates, Set<Integer> relYears, int rank) {
    double minSqError = Integer.MAX_VALUE;
    for(int i=0; i<rank && i<rankedDates.size(); i++) {
      final int retrievedYear = rankedDates.get(i).year;
      for (int relYear : relYears) {
        final double err = Math.abs(relYear - retrievedYear);
        final double sqErr = err * err;
        if (sqErr < minSqError) {
          minSqError = sqErr;
        }
      }
    }
    return minSqError;
  }

  /**
   * AbsError
   */
  public static double calcAbsError(List<ScoredDate> rankedDates, Set<Integer> relYears, int rank) {
    double minError = Integer.MAX_VALUE;
    for(int i=0; i<rank && i<rankedDates.size(); i++) {
      final int retrievedYear = rankedDates.get(i).year;
      for (int relYear : relYears) {
        final double err = (relYear - retrievedYear);
        if (err < minError) {
          minError = err;
        }
      }
    }
    return minError;
  }
}
