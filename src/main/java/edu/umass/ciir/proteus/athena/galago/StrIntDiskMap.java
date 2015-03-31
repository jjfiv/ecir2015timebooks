package edu.umass.ciir.proteus.athena.galago;

import org.lemurproject.galago.core.btree.simple.DiskMapWrapper;

import java.io.IOException;

/**
 * @author jfoley
 */
public class StrIntDiskMap extends DiskMapWrapper<String,Integer> {
  public StrIntDiskMap(String path) throws IOException {
    super(path, new DiskMapWrapper.StringCodec(), new DiskMapWrapper.IntCodec());
  }
}
