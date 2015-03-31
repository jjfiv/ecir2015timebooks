package edu.umass.ciir.proteus.athena;

import org.lemurproject.galago.utility.Parameters;

/**
 * @author jfoley
 */
public interface Tool {
  public String getName();
  public void run(Parameters argp) throws Exception;
}

