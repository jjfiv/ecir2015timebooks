package edu.umass.ciir.proteus.athena.utils;

import org.lemurproject.galago.core.util.FixedSizeMinHeap;
import org.lemurproject.galago.utility.CmpUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * @author jfoley.
 */
public class RandUtil {

  public static class RandomlyWeighted<T>  {
    public final T obj;
    public final int weight;
    public RandomlyWeighted(T obj, Integer weight) {
      this.obj = obj;
      this.weight = weight;
    }
    public static final Comparator<RandomlyWeighted> byWeight = new Comparator<RandomlyWeighted>() {
      @Override
      public int compare(RandomlyWeighted lhs, RandomlyWeighted rhs) {
        return CmpUtil.compare(lhs.weight, rhs.weight);
      }
    };
  }

  /** fill a heap with randomly weighted elements as you go... */
  public static <T> List<T> sampleRandomly(Iterable<T> source, int count, Random rand) {
    FixedSizeMinHeap<RandomlyWeighted> heap = new FixedSizeMinHeap<>(RandomlyWeighted.class, count, RandomlyWeighted.byWeight);

    for(T newObj : source) {
      int weight = rand.nextInt();
      heap.offer(new RandomlyWeighted<>(newObj, weight));
    }

    ArrayList<T> output = new ArrayList<>(count);
    for (RandomlyWeighted rw : heap.getSortedArray()) {
      output.add(Util.<T>cast(rw.obj));
    }

    return output;
  }

  public static String nextString(Random rand, int length) {
    StringBuilder longAndRandom = new StringBuilder();
    for(int i=0; i<length; i++) {
      int code = -1;
      while(code < 0) {
        code = rand.nextInt() & 0xffff;
        if(!Character.isDefined(code))
          code = -1;
      }
      longAndRandom.append((char) code);
    }
    return longAndRandom.toString();
  }


}
