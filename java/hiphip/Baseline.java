package hiphip;

import clojure.lang.IFn;

/*********************************************************************************************
 * Functions equivalent to (specific applications of) hiphip.array macros for benchmarking and
 * testing purposes.
 *********************************************************************************************/
public class Baseline {
  public static double[] make_double_array(int len) {
    return new double[len];
  }

  public static String[] make_string_array(int len) {
    return new String[len];
  }

  public static double[] make_double_array_and_fill(int len) {
    double [] ret = new double[len];
    for(int i = 0; i < len; i++) ret[i] = i;
    return ret;
  }
  
  public static long[] make_long_array_and_fill(int len) {
    long [] ret = new long[len];
    for(int i = 0; i < len; i++) ret[i] = i;
    return ret;
  }


  public static String[] make_string_array_and_fill(int len) {
    String [] ret = new String[len];
    for(int i = 0; i < len; i++) ret[i] = "test";
    return ret;
  }

  public static double areduce_dl(double [] xs, long [] ys) {
    double ret = 0.0;
    for(int i = 0; i < xs.length; i++) 
      ret += xs[i] * ys[i];
    return ret;
  }

  public static double[] multiply_pointwise_dl(double [] xs, long [] ys) {
    double[] os = new double[xs.length];
    for(int i = 0; i < xs.length; i++) 
      os[i] = xs[i] * ys[i];
    return os;
  }

  public static double[] multiply_in_place_pointwise_dl(double [] xs, long [] ys) {
    for(int i = 0; i < xs.length; i++) 
      xs[i] *= ys[i];
    return xs;
  }

  public static String[] fill_string_pointwise_product_dl(String [] os, double [] xs, long [] ys) {
    for(int i = 0; i < xs.length; i++) 
      os[i] = String.valueOf(xs[i] * ys[i]);
    return os;
  }
}
