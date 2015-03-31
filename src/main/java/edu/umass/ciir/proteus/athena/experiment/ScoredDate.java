package edu.umass.ciir.proteus.athena.experiment;

import edu.umass.ciir.proteus.athena.utils.DateUtil;
import org.lemurproject.galago.core.retrieval.ScoredDocument;
import org.lemurproject.galago.utility.CmpUtil;
import org.lemurproject.galago.utility.lists.Ranked;
import org.lemurproject.galago.utility.lists.Scored;

import java.util.Comparator;

/**
* @author jfoley
*/
public class ScoredDate extends Ranked {
  public final int year;

  public ScoredDate(int rank, double score, int year) {
    super(rank, score);
    this.year = year;
  }

  public ScoredDate(ScoredDocument sdoc) {
    this(sdoc.rank, sdoc.score, DateUtil.YearFromString(sdoc.documentName));
  }

  @Override
  public ScoredDate clone() {
    return new ScoredDate(this.rank, this.score, this.year);
  }

  @Override
  public Scored clone(double score) {
    return new ScoredDate(this.rank, score, this.year);
  }

  @Override
  public int hashCode() {
    return CmpUtil.hash(year) ^ CmpUtil.hash(rank) ^ CmpUtil.hash(score);
  }

  @Override
  public boolean equals(Object other) {
    if(other == null) return false;
    if(!(other instanceof ScoredDate)) return false;

    ScoredDate rhs = (ScoredDate) other;
    return year == rhs.year && rank == rhs.rank && score == rhs.score;
  }

  @Override
  public String toString() {
    return String.format("%d. %d (%.1f)", rank, year, score);
  }

  public static Comparator<ScoredDate> ByScore = new Comparator<ScoredDate>() {
    @Override
    public int compare(ScoredDate lhs, ScoredDate rhs) {
      return -CmpUtil.compare(lhs.score, rhs.score);
    }
  };

  public static Comparator<ScoredDate> ByScoreThenRank = new Comparator<ScoredDate>() {
    @Override
    public int compare(ScoredDate lhs, ScoredDate rhs) {
      int cmp = -CmpUtil.compare(lhs.score, rhs.score);
      if(cmp != 0) return cmp;
      return CmpUtil.compare(lhs.rank, rhs.rank);
    }
  };

  public static Comparator<ScoredDate> ByRankThenScore = new Comparator<ScoredDate>() {
    @Override
    public int compare(ScoredDate lhs, ScoredDate rhs) {
      int cmp = CmpUtil.compare(lhs.rank, rhs.rank);
      if(cmp != 0) return cmp;
      return -CmpUtil.compare(lhs.score, rhs.score);
    }
  };

}
