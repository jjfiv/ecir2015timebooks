package edu.umass.ciir.proteus.athena.experiment;

import edu.umass.ciir.proteus.athena.Tool;
import edu.umass.ciir.proteus.athena.cfg.DataSet;
import edu.umass.ciir.proteus.athena.utils.DateUtil;
import edu.umass.ciir.proteus.athena.utils.IO;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.lemurproject.galago.utility.CmpUtil;
import org.lemurproject.galago.utility.Parameters;

import java.io.File;
import java.util.*;

/**
 * @author jfoley
 */
public class CompareRuns implements Tool {

  private static final class Result {
    public String qid;
    public int year;
    public double score;
    public Result(String qid, int year, double score) {
      this.qid = qid;
      this.year = year;
      this.score = score;
    }

    public static final Comparator<Result> ByYear = new Comparator<Result>() {
      @Override
      public int compare(Result lhs, Result rhs) {
        return CmpUtil.compare(lhs.year, rhs.year);
      }
    };
  }

  public static ArrayList<Result> readRunFromFile(String input, final String whichMetric, final String dataset) {
    //final TObjectDoubleHashMap<String> qdata = new TObjectDoubleHashMap<String>();
    final ArrayList<Result> runData = new ArrayList<Result>();

    IO.forEachLine(new File(input), new IO.StringFunctor() {
			@Override
			public void process(String input) {
				if (!input.startsWith("# query-result"))
					return;

				String[] cols = input.split("\t");
				String metric = cols[3];
				if (!metric.equals(whichMetric)) {
					return;
				}
				String qid = cols[1];
				int year = DateUtil.YearFromString(cols[2]);
				if (!DataSet.yearMatches(year, dataset)) return;
				double score = Double.parseDouble(cols[4]);
				runData.add(new Result(qid, year, score));
			}
		});

    return runData;
  }

  public static Map<String,List<Result>> groupResultByDecade(List<Result> results) {
    Map<String,List<Result>> decadeResults = new HashMap<String,List<Result>>();

    for(Result rl : results) {
      String decade = DateUtil.nearestDecade(rl.year);
      if (!decadeResults.containsKey(decade))
        decadeResults.put(decade, new ArrayList<Result>());
      decadeResults.get(decade).add(rl);
    }

    return decadeResults;
  }

  public static double[] scores(List<Result> results) {
    if(results == null) { return new double[0]; }
    double[] sc = new double[results.size()];
    for(int i=0; i<results.size(); i++) {
      sc[i] = results.get(i).score;
    }
    return sc;
  }

  public static TObjectDoubleHashMap<String> toQidScore(List<Result> results) {
    TObjectDoubleHashMap<String> qdata = new TObjectDoubleHashMap<String>();
    qdata.ensureCapacity(results.size());
    for(Result rs : results) {
      qdata.put(rs.qid, rs.score);
    }
    return qdata;
  }

  @Override
  public String getName() {
    return "compare-runs";
  }

  public static double mean(double[] vals) {
    if(vals.length == 0) return 0;
    double sum = 0;
    for (double val : vals) {
      sum += val;
    }
    return sum / ((double) vals.length);
  }

  public static int numZeroes(double[] vals) {
    int numZ = 0;
    for(double val : vals) {
      if(val < 0.00001) {
        numZ++;
      }
    }
    return numZ;
  }

  public static int numOnes(double[] vals) {
    int count = 0;
    for(double val : vals) {
      if(val == 1.0) {
        count++;
      }
    }
    return count;
  }

  @Override
  public void run(Parameters argp) throws Exception {
    boolean mean = argp.get("mean", true);
    boolean compare = argp.get("compare", false);
    boolean histCompare = argp.get("hist-compare", false);
    boolean decadePerf = argp.get("decade-perf", false);

    if(!(mean || compare || histCompare)) {
      System.err.println("No comparison selected.");
      return;
    }

    String metric = argp.get("metric", "recip_rank");

    String dataset = argp.getString("dataset");
    List<Result> baselineResults = readRunFromFile(argp.getString("baseline"), metric, dataset);
    List<Result> methodResults = readRunFromFile(argp.getString("method"), metric, dataset);

    // convert to qid, score map
    TObjectDoubleHashMap<String> methodScores = toQidScore(methodResults);
    TObjectDoubleHashMap<String> baselineScores = toQidScore(baselineResults);
    assert(baselineScores.keySet().equals(methodScores.keySet()));

    // get score[]
    double[] mv = methodScores.values();
    double[] bv = baselineScores.values();

    System.out.println("measure\tbaseline\tmethod");

    if(mean) {
      System.out.printf("M-%s\t%.5f\t%.5f\n", metric, mean(bv), mean(mv));
      System.out.printf("M-%s-NZ\t%d\t%d\n", metric, numZeroes(bv), numZeroes(mv));
      System.out.printf("M-%s-N1\t%d\t%d\n", metric, numOnes(bv), numOnes(mv));
    }

    if(compare) {
      for(String key : baselineScores.keySet()) {
        double baseScore = baselineScores.get(key);
        double methodScore = methodScores.get(key);

        if(methodScore != baseScore) {
          System.out.printf("%s\t%.5f\t%.5f\n", key, baseScore, methodScore);
        }
      }
    }

    if(histCompare) {
      TObjectIntHashMap<String> bins = new TObjectIntHashMap<String>();
      for(String key : baselineScores.keySet()) {
        double baseScore = baselineScores.get(key);
        double methodScore = methodScores.get(key);

        double diff = methodScore - baseScore;
        bins.adjustOrPutValue(String.format("%+.1f", diff), 1, 1);
      }

      ArrayList<String> allBins = new ArrayList<String>(bins.keySet());
      Collections.sort(allBins);
      for(String bin : allBins) {
        System.out.printf("%s\t%d\n", bin, bins.get(bin));
      }
    }

    if(decadePerf) {
      Map<String, List<Result>> bdr = groupResultByDecade(baselineResults);
      Map<String, List<Result>> mdr = groupResultByDecade(methodResults);

      ArrayList<Integer> decades = new ArrayList<Integer>();
      for(String decadeStr : bdr.keySet()) {
        decades.add(DateUtil.YearFromString(decadeStr));
      }
      Collections.sort(decades);

      for (int decade : decades) {
        double dbv[] = scores(bdr.get(DateUtil.nearestDecade(decade)));
        double dmv[] = scores(mdr.get(DateUtil.nearestDecade(decade)));
        System.out.printf("%s\tmean\t%.5f\t%.5f\n", decade, mean(dbv), mean(dmv));
        System.out.printf("%s\tNZ\t%d\t%d\n", decade, numZeroes(dbv), numZeroes(dmv));
        System.out.printf("%s\tN1\t%d\t%d\n", decade, numOnes(dbv), numOnes(dmv));
      }
    }
  }

}
