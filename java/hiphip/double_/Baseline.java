package hiphip.double_;

import clojure.lang.IFn;

/*********************************************************************************************
 * Functions equivalent to (specific applications of) hiphip macros for benchmarking and
 * testing purposes.
 *********************************************************************************************/
public class Baseline {
  public static int alength(double [] arr) {
    return arr.length;
  }

  public static double aget(double [] arr, int idx) {
    return arr[idx];
  }

  public static double aset(double [] arr, int idx, double v) {
    arr[idx] = v;
    return v;
  }

  public static double ainc(double [] arr, int idx, int v) {
    return arr[idx]+=v;
  }
  
  public static double[] aclone(double [] arr) {
    return arr.clone();
  }
  
  // tests areduce and dot-product 
  public static double dot_product(double[] arr1, double[] arr2) {
    double s = 0;
    for (int i = 0; i < arr1.length; i++) {
      s += arr1[i] * arr2[i];
    }
    return s;
  }

  
  // tests doarr and afill!
  public static double[] multiply_in_place_pointwise(double[] xs, double[] ys) {
    for(int i = 0; i < xs.length; i++) {
      xs[i] *= ys[i];
    }
    return xs;
  }

  public static double[] multiply_end_in_place_pointwise(double[] xs, double[] ys) {
    for(int i = (xs.length)/2; i < xs.length; i++) {
      xs[i] *= ys[i];
    }
    return xs;
  }


  // tests afill!
  public static double[] multiply_in_place_by_idx(double[] xs) {
    for(int i = 0; i < xs.length; i++) {
      xs[i] *= i;
    }
    return xs;
  }

  // tests amake
  public static double[] acopy_inc(int len, double[] ys) {
    double[] ret = new double[len];
    for(int i = 0; i < len; i++) {
      ret[i] = ys[i] + 1;
    }
    return ret;
  }

  public static double[] amap_inc(double[] arr) {
    double[] ret = new double[arr.length];
    for (int i = 0; i < arr.length; i++) {
      ret[i] = arr[i] + 1;
    }
    return ret;
  }
  
  public static double[] amap_end_inc(double[] arr) {
    int h1 = arr.length/2, h2 = arr.length - h1;
    double[] ret = new double[h2];
    for (int i = 0; i < h2; i++) {
      ret[i] = arr[i+h1] + 1;
    }
    return ret;
  }

  public static double[] amap_plus_idx(double[] arr) {
    double[] newarr = new double[arr.length];
    for (int i = 0; i < arr.length; i++) {
      newarr[i] = arr[i] + i;
    }
    return newarr;
  }

  public static double asum(double[] arr) {
    double s = 0;
    for (double d : arr) {
      s += d;
    }
    return s;
  }

  public static double asum_end(double[] arr) {
    double s = 0;
    for (int i = arr.length/2; i < arr.length; i++) {
      s += arr[i];
    }
    return s;
  }

  public static double asum_square(double[] arr) {
    double s = 0;
    for (double d : arr) {
      s += d * d;
    }
    return s;
  }

  public static double aproduct(double[] arr) {
    double s = 1;
    for (double d : arr) {
      s *= d;
    }
    return s;
  }

  public static double amax(double[] arr) {
    double m = arr[0];
    for (int i = 1; i < arr.length; i++) {
      double v = arr[i];
      if (v > m) m = v;
    }
    return m;
  }

  public static double amin(double[] arr) {
    double m = arr[0];
    for (int i = 1; i < arr.length; i++) {
      double v = arr[i];
      if (v < m) m = v;
    }
    return m;
  }

  public static Double amean(double[] arr) {
    return (1.0 * asum(arr)) / arr.length;
  }
}
