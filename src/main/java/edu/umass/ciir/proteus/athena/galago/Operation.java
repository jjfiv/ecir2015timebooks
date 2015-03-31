package edu.umass.ciir.proteus.athena.galago;

/**
 * @author jfoley.
 */
public interface Operation<T> {
	public void process(T item);
}
