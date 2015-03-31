package edu.umass.ciir.proteus.athena.galago;

import org.lemurproject.galago.core.types.KeyValuePair;
import org.lemurproject.galago.tupleflow.Processor;
import org.lemurproject.galago.tupleflow.Reducer;
import org.lemurproject.galago.tupleflow.Utility;
import org.lemurproject.galago.utility.ByteUtil;
import org.lemurproject.galago.utility.CmpUtil;
import org.lemurproject.galago.utility.StreamCreator;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author jfoley
 */
public class DateTextWriter implements Processor<KeyValuePair> {
  final Reducer<KeyValuePair> concatValues = new DateTextCollector();
  final PrintWriter out;
  List<KeyValuePair> current = new ArrayList<>();

  public DateTextWriter(String path) throws IOException {
    out = new PrintWriter(StreamCreator.openOutputStream(path));
  }

  @Override
  public void process(KeyValuePair object) throws IOException {
    // if we have some things to reduce, and this key is new
    if (!current.isEmpty() && !CmpUtil.equals(current.get(current.size() - 1).key, object.key)) {
      flush();
    }
    current.add(object);
  }

  public void flush() throws IOException {
    List<KeyValuePair> reduced = concatValues.reduce(current);
    if(reduced.isEmpty()) return;
    out.println(Utility.toInt(reduced.get(0).key) + "\t" + ByteUtil.toString(reduced.get(0).value));
    current = new ArrayList<>();
  }

  @Override
  public void close() throws IOException {
    flush();
    out.close();
  }
}
