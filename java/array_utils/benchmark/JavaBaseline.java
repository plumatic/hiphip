package array_utils.benchmark;

import clojure.lang.IFn;

public class JavaBaseline {
  public static void ainc(double [] arr, int idx) {
    arr[idx]++;
  }
  
  public static double asum_noop(double[] arr) {
    double s = 0;
    for (double d : arr) {
      s += d;
    }
    return s;
  }

  public static double asum_op(double[] arr) {
    double s = 0;
    for (double d : arr) {
      s += 1.0 + (2.0 * d);
    }
    return s;
  }

  public static double[] aclone(double[] arr) {
    double[] clone = new double[arr.length];
    for (int i = 0; i < arr.length; i++) {
      clone[i] = arr[i];
    }
    return clone;
  }

  public static double dot_product(double[] arr1, double[] arr2) {
    double s = 0;
    for (int i = 0; i < arr1.length; i++) {
      s += arr1[i] * arr2[i];
    }
    return s;
  }

  public static double[] afill_index_op(double[] arr) {
    for (int i = 0; i < arr.length; i++) {
      arr[i] = 1.0 + i * 2.0;
    }
    return arr;
  }

  public static double[] afill_inc(double[] arr) {
    for (int i = 0; i < arr.length; i++) {
      arr[i] += 1.0;
    }
    return arr;
  }

  public static double[] afill_value_op(double[] arr) {
    for (int i = 0; i < arr.length; i++) {
      arr[i] = 1.0 + 2.0 * arr[i];
    }
    return arr;
  }


  public static double[] amap_op(double[] arr) {
    double[] newarr = new double[arr.length];
    for (int i = 0; i < arr.length; i++) {
      newarr[i] = 1.0 + 2.0 * arr[i];
    }
    return newarr;
  }

  public static double amean(double[] arr) {
    return asum_noop(arr) / arr.length;
  }
  
  public static double amax(double[] arr) {
    double m = Double.MIN_VALUE;
    for (double d : arr)
      if (d > m) m = d;
    return m;
  }
}
