package edu.umass.ciir.proteus.athena.galago;

import org.lemurproject.galago.core.types.KeyValuePair;
import org.lemurproject.galago.tupleflow.Reducer;
import org.lemurproject.galago.tupleflow.Utility;
import org.lemurproject.galago.utility.ByteUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
* @author jfoley
*/
public class DateTextCollector implements Reducer<KeyValuePair> {

  public static KeyValuePair makePair(int date, String text) {
    return new KeyValuePair(Utility.fromInt(date), ByteUtil.fromString(text));
  }

  public static int getDate(KeyValuePair kv) {
    return Utility.toInt(kv.key);
  }

  public static String getText(KeyValuePair kv) {
    return ByteUtil.toString(kv.value);
  }

  @Override
  public ArrayList<KeyValuePair> reduce(List<KeyValuePair> input) throws IOException {
    ArrayList<KeyValuePair> results = new ArrayList<KeyValuePair>(input.size());
    StringBuilder sb = new StringBuilder();

    int lastKey = getDate(input.get(0));
    for(KeyValuePair kv : input) {
      int key = getDate(kv);

      if(key != lastKey) {
        results.add(makePair(lastKey, sb.toString()));

        // reset builder
        sb = new StringBuilder();
        lastKey = key;
      }
      sb.append(getText(kv)).append(' ');
    }

    results.add(makePair(lastKey, sb.toString()));

    return results;
  }
}
