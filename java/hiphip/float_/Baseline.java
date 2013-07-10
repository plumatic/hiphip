package hiphip.float_;

import clojure.lang.IFn;

/*********************************************************************************************
 * Functions equivalent to (specific applications of) hiphip macros for benchmarking and
 * testing purposes.
 *********************************************************************************************/
public class Baseline {
  public static int alength(float [] arr) {
    return arr.length;
  }

  public static float aget(float [] arr, int idx) {
    return arr[idx];
  }

  public static float aset(float [] arr, int idx, float v) {
    arr[idx] = v;
    return v;
  }

  public static float ainc(float [] arr, int idx, int v) {
    return arr[idx]+=v;
  }
  
  public static float[] aclone(float [] arr) {
    return arr.clone();
  }
  
  // tests areduce and dot-product 
  public static float dot_product(float[] arr1, float[] arr2) {
    float s = 0;
    for (int i = 0; i < arr1.length; i++) {
      s += arr1[i] * arr2[i];
    }
    return s;
  }

  
  // tests doarr and afill!
  public static float[] multiply_in_place_pointwise(float[] xs, float[] ys) {
    for(int i = 0; i < xs.length; i++) {
      xs[i] *= ys[i];
    }
    return xs;
  }

  public static float[] multiply_end_in_place_pointwise(float[] xs, float[] ys) {
    for(int i = (xs.length)/2; i < xs.length; i++) {
      xs[i] *= ys[i];
    }
    return xs;
  }


  // tests afill!
  public static float[] multiply_in_place_by_idx(float[] xs) {
    for(int i = 0; i < xs.length; i++) {
      xs[i] *= i;
    }
    return xs;
  }

  // tests amake
  public static float[] acopy_inc(int len, float[] ys) {
    float[] ret = new float[len];
    for(int i = 0; i < len; i++) {
      ret[i] = ys[i] + 1;
    }
    return ret;
  }

  public static float[] amap_inc(float[] arr) {
    float[] ret = new float[arr.length];
    for (int i = 0; i < arr.length; i++) {
      ret[i] = arr[i] + 1;
    }
    return ret;
  }
  
  public static float[] amap_end_inc(float[] arr) {
    int h1 = arr.length/2, h2 = arr.length - h1;
    float[] ret = new float[h2];
    for (int i = 0; i < h2; i++) {
      ret[i] = arr[i+h1] + 1;
    }
    return ret;
  }

  public static float[] amap_plus_idx(float[] arr) {
    float[] newarr = new float[arr.length];
    for (int i = 0; i < arr.length; i++) {
      newarr[i] = arr[i] + i;
    }
    return newarr;
  }

  public static float asum(float[] arr) {
    float s = 0;
    for (float d : arr) {
      s += d;
    }
    return s;
  }

  public static float asum_end(float[] arr) {
    float s = 0;
    for (int i = arr.length/2; i < arr.length; i++) {
      s += arr[i];
    }
    return s;
  }

  public static float asum_square(float[] arr) {
    float s = 0;
    for (float d : arr) {
      s += d * d;
    }
    return s;
  }

  public static float aproduct(float[] arr) {
    float s = 1;
    for (float d : arr) {
      s *= d;
    }
    return s;
  }

  public static float amax(float[] arr) {
    float m = arr[0];
    for (int i = 1; i < arr.length; i++) {
      float v = arr[i];
      if (v > m) m = v;
    }
    return m;
  }

  public static float amin(float[] arr) {
    float m = arr[0];
    for (int i = 1; i < arr.length; i++) {
      float v = arr[i];
      if (v < m) m = v;
    }
    return m;
  }

  public static Double amean(float[] arr) {
    return (1.0 * asum(arr)) / arr.length;
  }
}
