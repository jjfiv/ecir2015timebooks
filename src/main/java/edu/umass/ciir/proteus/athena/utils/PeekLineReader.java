package edu.umass.ciir.proteus.athena.utils;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;

/**
* @author jfoley
*/
public class PeekLineReader implements Closeable {
  private final BufferedReader reader;
  private String current;

  public PeekLineReader(BufferedReader reader) throws IOException {
    this.reader = reader;
    this.current = reader.readLine();
  }

  public String peek() {
    return current;
  }

  public String next() throws IOException {
    if(current == null) return null;
    String last = current;
    current = reader.readLine();
    return last;
  }

  @Override
  public void close() throws IOException {
    IO.close(reader);
  }
}
