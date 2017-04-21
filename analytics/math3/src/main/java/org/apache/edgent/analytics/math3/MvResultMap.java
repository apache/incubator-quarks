package org.apache.edgent.analytics.math3;

import java.util.HashMap;

/**
 * Aggregation results for multiple aggregated variables.
 * 
 * <p>The name of the aggregated variable is typically the key for the variable's {@link ResultMap}.
 */
public class MvResultMap extends HashMap<String,ResultMap> {
  private static final long serialVersionUID = 1L;
}