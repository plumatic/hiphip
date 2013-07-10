package hiphip;

public class IndexArrays {
  public static int length(int[] xs) {
    return xs.length;
  }

  public static int[] make(int start, int stop) {
    int len = stop - start;
    int [] ret = new int [len];
    for (int i = 0; i < len; i++) ret[i] = start + i;
    return ret;
  }
  
  public static void swap(int[] arr, int i, int j) {
    int tmp = arr[i];
    arr[i] = arr[j];
    arr[j] = tmp;
  }
}
