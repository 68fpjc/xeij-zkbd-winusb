//========================================================================================
//  ByteQueue.java
//    en:Byte queue
//    ja:バイトキュー
//  Copyright (C) 2003-2026 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.util.*;  //Arrays

//class ByteQueue
//  バイトキュー。要素がbyteのFIFOの実装
//  ブロック(小さいキュー)を連結することで容量を可変にしている。上限は約2GB
//  書き込むスレッドと読み出すスレッドはそれぞれ1つであること
public final class ByteQueue {

  private static final boolean DEBUG = false;
  private static final boolean TEST = false;


  //class Block
  //  ブロック
  private static final class Block {

    private volatile Block newer = null;  //これより新しいブロック。null=最も新しいブロック
    private volatile Block older = null;  //これより古いブロック。null=最も古いブロック

    private static final int size = 65536;  //ブロックの容量。2の累乗
    private final byte[] buff = new byte[size];  //配列
    private volatile int wcnt = 0;  //これまでに書き込んだ長さの下位32bit
    private volatile int rcnt = 0;  //これまでに読み出した長さの下位32bit
    //  buff[wcnt&(size-1)]   次に書き込む位置
    //  buff[rcnt&(size-1)]   次に読み出す位置
    //  wcnt-rcnt             読み出せる長さ
    //  size-(wcnt-rcnt)      書き込める長さ
    //  (wcnt-rcnt)==0        空
    //  (wcnt-rcnt)==size     満杯
    //  0<=(wcnt-rcnt)<=size

    //length = clear ()
    //  空にする。読み飛ばせるだけ読み飛ばす
    //結果
    //  length  読み飛ばせた長さ。0=空で読み飛ばせなかった
    private final int clear () {
      int length = wcnt - rcnt;  //読み飛ばす長さ
      if (0 < length) {  //1バイト以上読み飛ばせて1バイト以上読み飛ばすとき
        rcnt += length;  //これまでに読み出した長さを更新する
      }
      return length;  //読み飛ばせた長さを返す
    }  //clear

    //data = read ()
    //  1バイト読み出す
    //結果
    //  data  読み出したデータ。0～255。-1=空で読み出せなかった
    private final int read () {
      return ((wcnt - rcnt) == 0 ? -1 : //空のとき-1を返す
              buff[(rcnt++) & (size - 1)] & 255);  //1バイト読み出して0～255の範囲で返す
    }  //read

    //length = read (array, offset, length)
    //  配列へ読み出す
    //引数
    //  array   配列
    //  offset  配列の位置
    //  length  読み出す長さ
    //結果
    //  length  読み出せた長さ。0=空で読み出せなかった
    private final int read (byte[] array, int offset, int length) {
      if (length < 0) {
        throw new IllegalArgumentException (String.format ("length=%d", length));
      }
      length = Math.min (length, wcnt - rcnt);  //読み出せる長さに抑える
      if (0 < length) {  //1バイト以上読み出せて1バイト以上読み出すとき
        int index = rcnt & (size - 1);  //キューの位置
        int first = Math.min (length, size - index);  //1回目はキューの末尾まで
        System.arraycopy (buff, index,  //キューから
                          array, offset,  //配列へ
                          first);  //コピーする
        if (first < length) {  //2回目があるとき
          System.arraycopy (buff, 0,  //キューから
                            array, offset + first,  //配列へ
                            length - first);  //コピーする
        }
        rcnt += length;  //これまでに読み出した長さを更新する
      }
      return length;  //読み出せた長さを返す
    }  //read

    //length = skip (length)
    //  読み飛ばす
    //引数
    //  length  読み飛ばす長さ
    //結果
    //  length  読み飛ばせた長さ。0=空で読み飛ばせなかった
    private final int skip (int length) {
      if (length < 0) {
        throw new IllegalArgumentException (String.format ("length=%d", length));
      }
      length = Math.min (length, wcnt - rcnt);  //読み飛ばせる長さに抑える
      if (0 < length) {  //1バイト以上読み飛ばせて1バイト以上読み飛ばすとき
        rcnt += length;  //これまでに読み出した長さを更新する
      }
      return length;  //読み飛ばせた長さを返す
    }  //skip

    //length = unused ()
    //  書き込める長さ
    //結果
    //  length  書き込める長さ。0=満杯で書き込めない
    private final int unused () {
      return size - (wcnt - rcnt);  //書き込める長さ
    }  //unused

    //length = used ()
    //  読み出せる長さ
    //結果
    //  length  読み出せる長さ。0=空で読み出せない
    private final int used () {
      return wcnt - rcnt;  //読み出せる長さ
    }  //used

    //length = write (data)
    //  1バイト書き込む
    //引数
    //  data    書き込むデータ。下位8bitのみ有効
    //結果
    //  length  書き込めた長さ。0=満杯で書き込めなかった。1=書き込めた
    private final int write (int data) {
      if ((wcnt - rcnt) == size) {  //書き込めないとき
        return 0;  //0を返す
      }
      buff[(wcnt++) & (size - 1)] = (byte) data;  //1バイト書き込む
      return 1;  //1を返す
    }  //write

    //length = write (array, offset, length)
    //  配列から書き込む
    //引数
    //  array   配列
    //  offset  配列の位置
    //  length  書き込む長さ
    //結果
    //  length  書き込めた長さ。0=満杯で書き込めなかった
    private final int write (byte[] array, int offset, int length) {
      if (length < 0) {
        throw new IllegalArgumentException (String.format ("length=%d", length));
      }
      length = Math.min (length, size - (wcnt - rcnt));  //書き込める長さに抑える
      if (0 < length) {  //1バイト以上書き込めて1バイト以上書き込むとき
        int index = wcnt & (size - 1);  //キューの位置
        int first = Math.min (length, size - index);  //1回目はキューの末尾まで
        System.arraycopy (array, offset,  //配列から
                          buff, index,  //キューへ
                          first);  //コピーする
        if (first < length) {  //2回目があるとき
          System.arraycopy (array, offset + first,  //配列から
                            buff, 0,  //キューへ
                            length - first);  //コピーする
        }
        wcnt += length;  //これまでに書き込んだ長さを更新する
      }
      return length;  //書き込めた長さを返す
    }  //write

  }  //class Block


  private volatile Block newest = new Block ();  //最も新しいブロック。newest.newer==null
  private volatile Block oldest = newest;  //最も古いブロック。oldest.older==null

  private static final int size = 0x7fffffff;  //キューの容量。ブロックの容量の倍数である必要はない
  private volatile int wcnt = 0;  //これまでに書き込んだ長さの下位32bit
  private volatile int rcnt = 0;  //これまでに読み出した長さの下位32bit
  //  wcnt-rcnt             読み出せる長さ
  //  size-(wcnt-rcnt)      書き込める長さ
  //  (wcnt-rcnt)==0        空
  //  (wcnt-rcnt)==size     満杯
  //  0<=(wcnt-rcnt)<=size

  private final Object lock = new Object ();  //ブロックのためのオブジェクト
  private volatile boolean canceled = false;  //true=キャンセルされた

  //cancel ()
  //  waitAndReadのブロックを解除する
  //  以後キューは動作しない
  public final void cancel () {
    if (canceled) {  //キャンセルされた
      return;
    }
    if (DEBUG) {
      System.out.println ("cancel start");
    }
    canceled = true;  //キャンセルする
    synchronized (lock) {
      lock.notify ();  //waitAndReadのブロックを解除する
    }
    if (DEBUG) {
      System.out.println ("cancel end");
    }
  }  //cancel

  //length = clear ()
  //  空にする。読み飛ばせるだけ読み飛ばす
  //結果
  //  length  読み飛ばせた長さ。0=空で読み飛ばせなかった
  public final int clear () {
    if (canceled) {  //キャンセルされた
      return 0;
    }
    if (DEBUG) {
      System.out.println ("clear start");
    }
    int length = wcnt - rcnt;  //読み飛ばせる長さ
    if (0 < length) {  //1バイト以上読み飛ばせるとき
      int l = length;  //残りの長さ
      int k = oldest.skip (l);  //最も古いブロックから読み飛ばす
      l -= k;
      while (0 < l) {
        //最も古いブロックを削除する
        //  oldest     newer
        //      <-X-older
        //      newer-X->
        //             oldest
        Block newer = oldest.newer;
        newer.older = null;
        oldest.newer = null;
        oldest = newer;
        //
        k = oldest.skip (l);  //最も古いブロックから読み飛ばす
        l -= k;
      }
      rcnt += length;  //これまでに読み出した長さを更新する
    }  //if 0<length
    if (DEBUG) {
      System.out.println ("clear end " + length);
    }
    return length;  //読み飛ばせた長さ
  }  //clear

  //data = read ()
  //  1バイト読み出す
  //結果
  //  data  読み出したデータ。0～255。-1=空で読み出せなかった
  public final int read () {
    if (canceled) {  //キャンセルされた
      return -1;
    }
    if (DEBUG) {
      System.out.println ("read start");
    }
    int data = -1;
    if (0 < (wcnt - rcnt)) {  //1バイト以上読み出せるとき
      data = oldest.read ();  //最も古いブロックから読み出す
      if (data < 0) {  //最も古いブロックが空で読み出せなかったとき
        //最も古いブロックを削除する
        //  oldest     newer
        //      <-X-older
        //      newer-X->
        //             oldest
        Block newer = oldest.newer;
        newer.older = null;
        oldest.newer = null;
        oldest = newer;
        //
        data = oldest.read ();  //最も古いブロックから読み出す
      }
      rcnt++;
    }  //if 0<(wcnt-rcnt)
    if (DEBUG) {
      System.out.println ("read end " + data);
    }
    return data;  //読み出したデータ
  }  //read

  //length = read (array, offset, length)
  //  配列へ読み出す
  //引数
  //  array   配列
  //  offset  配列の位置
  //  length  読み出す長さ
  //結果
  //  length  読み出せた長さ。0=空で読み出せなかった
  public final int read (byte[] array, int offset, int length) {
    if (canceled) {  //キャンセルされた
      return 0;
    }
    if (DEBUG) {
      System.out.println ("read start array," + offset + "," + length);
    }
    if (length < 0) {
      throw new IllegalArgumentException (String.format ("length=%d", length));
    }
    length = Math.min (length, wcnt - rcnt);  //読み出せる長さに抑える
    if (0 < length) {  //1バイト以上読み出せて1バイト以上読み出すとき
      int o = offset;  //次の位置
      int l = length;  //残りの長さ
      int k = oldest.read (array, o, l);  //最も古いブロックから読み出す
      o += k;
      l -= k;
      while (0 < l) {
        //最も古いブロックを削除する
        //  oldest     newer
        //      <-X-older
        //      newer-X->
        //             oldest
        Block newer = oldest.newer;
        newer.older = null;
        oldest.newer = null;
        oldest = newer;
        //
        k = oldest.read (array, o, l);  //最も古いブロックから読み出す
        o += k;
        l -= k;
      }
      rcnt += length;  //これまでに読み出した長さを更新する
    }
    if (DEBUG) {
      System.out.println ("read end " + length);
    }
    return length;  //読み出せた長さ
  }  //read

  //length = skip (length)
  //  読み飛ばす
  //引数
  //  length  読み飛ばす長さ
  //結果
  //  length  読み飛ばせた長さ。0=空で読み飛ばせなかった
  public final int skip (int length) {
    if (canceled) {  //キャンセルされた
      return 0;
    }
    if (DEBUG) {
      System.out.println ("skip start " + length);
    }
    if (length < 0) {
      throw new IllegalArgumentException (String.format ("length=%d", length));
    }
    length = Math.min (length, wcnt - rcnt);  //読み飛ばせる長さに抑える
    if (0 < length) {  //1バイト以上読み飛ばせて1バイト以上読み飛ばすとき
      int l = length;  //残りの長さ
      int k = oldest.skip (l);  //最も古いブロックから読み飛ばす
      l -= k;
      while (0 < l) {
        //最も古いブロックを削除する
        //  oldest     newer
        //      <-X-older
        //      newer-X->
        //             oldest
        Block newer = oldest.newer;
        newer.older = null;
        oldest.newer = null;
        oldest = newer;
        //
        k = oldest.skip (l);  //最も古いブロックから読み飛ばす
        l -= k;
      }
      rcnt += length;  //これまでに読み出した長さを更新する
    }
    if (DEBUG) {
      System.out.println ("skip end " + length);
    }
    return length;  //読み飛ばせた長さ
  }  //skip

  //length = unused ()
  //  書き込める長さ
  //結果
  //  length  書き込める長さ。0=満杯で書き込めない
  public final int unused () {
    if (canceled) {  //キャンセルされた
      return size;
    }
    return size - (wcnt - rcnt);  //書き込める長さ
  }  //unused

  //length = used ()
  //  読み出せる長さ
  //結果
  //  length  読み出せる長さ。0=空で読み出せない
  public final int used () {
    if (canceled) {  //キャンセルされた
      return 0;
    }
    return wcnt - rcnt;  //読み出せる長さ
  }  //used

  //data = waitAndRead ()
  //  1バイト読み出す。1バイト読み出せるまでブロックする
  //結果
  //  data  読み出したデータ。0～255。-1=キャンセルされた
  public final int waitAndRead () {
    if (canceled) {  //キャンセルされた
      return -1;
    }
    if (DEBUG) {
      System.out.println ("waitAndRead start");
    }
    int data = read ();  //1バイト読み出す
    if (data < 0) {  //読み出せない
      try {
        synchronized (lock) {
          do {
            lock.wait ();  //ブロックする
            if (canceled) {  //キャンセルされた
              break;
            }
            data = read ();  //1バイト読み出す
          } while (data < 0);  //読み出せない
        }
      } catch (InterruptedException ie) {
      }
    }
    if (DEBUG) {
      System.out.println ("waitAndRead end " + data);
    }
    return data;  //データを返す
  }  //waitAndRead

  //length = waitAndRead (array, offset, length)
  //  配列へ読み出す。1バイト以上読み出せるまでブロックする
  //引数
  //  array   配列
  //  offset  配列の位置
  //  length  読み出す長さ
  //結果
  //  length  読み出せた長さ。-1=キャンセルされた
  public final int waitAndRead (byte[] array, int offset, int length) {
    if (canceled) {  //キャンセルされた
      return -1;
    }
    if (DEBUG) {
      System.out.println ("waitAndRead start array," + offset + "," + length);
    }
    if (length < 0) {
      throw new IllegalArgumentException (String.format ("length=%d", length));
    }
    if (0 < length) {  //1バイト以上読み出すとき
      int data = waitAndRead ();  //1バイト読み出す。1バイト読み出せるまでブロックする
      if (data < 0) {  //キャンセルされたとき
        length = -1;  //-1を返す
      } else {  //読み出せたとき
        array[offset] = (byte) data;  //1バイト目
        length = 1 + read (array, offset + 1, length - 1);  //2バイト目以降
      }
    }
    if (DEBUG) {
      System.out.println ("waitAndRead end " + length);
    }
    return length;
  }  //waitAndRead

  //length = write (data)
  //  1バイト書き込む
  //引数
  //  data    書き込むデータ。下位8bitのみ有効
  //結果
  //  length  書き込めた長さ。0=満杯で書き込めなかった。1=書き込めた
  public final int write (int data) {
    if (canceled) {  //キャンセルされた
      return 0;
    }
    if (DEBUG) {
      System.out.println ("write start " + data);
    }
    int length = 0;
    if ((wcnt - rcnt) < size) {  //1バイト以上書き込めるとき
      length = newest.write (data);  //最も新しいブロックに書き込む
      if (length == 0) {  //最も新しいブロックが満杯で書き込めなかったとき
        //最も新しいブロックを追加する
        //  newest     newer
        //      <---older
        //      newer--->
        //             newest
        Block newer = new Block ();
        newer.older = newest;
        newest.newer = newer;
        newest = newer;
        //
        length = newest.write (data);  //最も新しいブロックに書き込む
      }
      wcnt += length;  //これまでに書き込んだ長さを更新する
      synchronized (lock) {
        lock.notify ();  //waitAndReadのブロックを解除する
      }
    }
    if (DEBUG) {
      System.out.println ("write end " + length);
    }
    return length;
  }  //write

  //length = write (array, offset, length)
  //  配列から書き込む
  //引数
  //  array   配列
  //  offset  配列の位置
  //  length  書き込む長さ
  //結果
  //  length  書き込めた長さ。0=満杯で書き込めなかった
  public final int write (byte[] array, int offset, int length) {
    if (canceled) {  //キャンセルされた
      return 0;
    }
    if (DEBUG) {
      System.out.println ("write start array," + offset + "," + length);
    }
    length = Math.min (length, size - (wcnt - rcnt));  //書き込める長さに抑える
    if (0 < length) {  //1バイト以上書き込めて1バイト以上書き込むとき
      int o = offset;  //次の位置
      int l = length;  //残りの長さ
      int k = newest.write (array, o, l);  //最も新しいブロックに書き込む
      o += k;
      l -= k;
      while (0 < l) {
        //最も新しいブロックを追加する
        //  newest     newer
        //      <---older
        //      newer--->
        //             newest
        Block newer = new Block ();
        newer.older = newest;
        newest.newer = newer;
        newest = newer;
        //
        k = newest.write (array, o, l);  //最も新しいブロックに書き込む
        o += k;
        l -= k;
      }
      wcnt += length;  //これまでに書き込んだ長さを更新する
      synchronized (lock) {
        lock.notify ();  //waitAndReadのブロックを解除する
      }
    }
    if (DEBUG) {
      System.out.println ("write end " + length);
    }
    return length;  //書き込めた長さ
  }  //write


  //動作確認
  static {
    if (TEST) {
      System.out.println ("ByteQueue test");
      long start = System.currentTimeMillis ();
      final ByteQueue queue = new ByteQueue ();
      final int unit = Math.max (1, Block.size >> 6);
      Thread writer = new Thread () {
        @Override public void run () {
          byte[] a = new byte[unit * 243];
          int x = 0;
          for (int n = 0; n < 243; n++) {
            int l = unit * (x + 1);  //xの並びの長さ
            if ((n & 1) == 0) {  //nが偶数のとき
              for (int i = 0; i < l; i++) {
                queue.write (x);  //1バイトずつ書き込む
              }
            } else {  //nが奇数のとき
              Arrays.fill (a, 0, l, (byte) x);
              queue.write (a, 0, l);  //まとめて書き込む
            }
            x = (x * 7 + 1) % 243;  //次のx
          }
        }
      };
      Thread reader = new Thread () {
        @Override public void run () {
          int session = 0;  //waitAndReadを呼び出した回数
          int error = 0;  //一致しなかったデータの数
          int t = unit * (243 * (243 + 1) / 2);  //全体の長さ
          byte[] a = new byte[t];
          int x = 0;
          int l = unit * (x + 1);  //xの並びの長さ
          for (int n = 0; n < 243; ) {
            session++;
            int k = queue.waitAndRead (a, 0, t);  //読めるだけ読む
            t -= k;
            int i = 0;  //aのインデックス
            while (k != 0) {
              int m = Math.min (k, l);  //xの並びの読めた部分の長さ
              k -= m;
              for (int j = 0; j < m; j++) {
                int data = a[i++] & 255;
                if (data != x) {
                  error++;
                }
              }
              l -= m;
              if (l == 0) {  //xの並びが終わった
                x = (x * 7 + 1) % 243;  //次のx
                l = unit * (x + 1);  //xの並びの長さ
                n++;
              }
            }
          }
          System.out.println (session + " sessions");
          System.out.println (error + " errors");
        }
      };
      reader.start ();
      writer.start ();
      try {
        writer.join ();
      } catch (InterruptedException ie) {
      }
      try {
        reader.join ();
      } catch (InterruptedException ie) {
      }
      long end = System.currentTimeMillis ();
      System.out.println ((end - start) + " ms");
    }  //if
  }  //static


}  //class ByteQueue
