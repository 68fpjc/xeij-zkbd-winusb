//========================================================================================
//  TickerQueue.java
//    en:Sort of a simple task scheduler
//    ja:簡易タスクスケジューラのようなもの
//  Copyright (C) 2003-2026 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

public class TickerQueue {

  //class Ticker
  //  ティッカー
  public static abstract class Ticker {
    Ticker () {
      time = Long.MAX_VALUE;  //キューにない
    }
    long time;  //実行時刻。キューにないときLong.MAX_VALUE
    //  tickが呼び出された時点でthisはキューから削除されている
    //  tickの中でtkqAddを呼び出してthisまたは他のティッカーをキューに追加できる
    //  tickの中でtkqRemoveを呼び出して他のティッカーをキューから削除できる
    //  tickの中でtkqRunを呼び出してはならない
    abstract void tick ();
  }  //class Ticker

  //キュー
  //  キューに近いソート済み集合
  static final int TKQ_S = 256;  //容量。2の累乗
  static final Ticker[] tkqArray = new Ticker[TKQ_S];  //配列。未使用領域は不定
  //  w-r       使用量
  //  r==w      空
  //  s-(w-r)   空き容量
  //  (w-r)==s  満杯
  //  w&(s-1)   末尾+1のインデックス
  //  r&(s-1)   先頭のインデックス
  static int tkqR;  //先頭。読み出した回数の下位32bit
  static int tkqW;  //末尾+1。書き込んだ回数の下位32bit

  static final boolean TKQ_NULL = true;  //true=未使用領域をnullにする

  //tkqInit ()
  //  初期化
  public static void tkqInit () {
    //tkqArray = new Ticker[TKQ_S];
    tkqR = 0;
    tkqW = 0;
    if (false) {
      tkqTest ();
    }
  }  //tkqInit

  //tkqRun (time)
  //  実行
  //  指定された時刻までの実行時刻を持つティッカーをキューから削除して実行する
  public static void tkqRun (long time) {
    Ticker ticker;
    while (tkqR != tkqW &&  //キューが空でなくて
           (ticker = tkqArray[tkqR & (TKQ_S - 1)]).time <= time) {  //先頭のティッカーが実行時刻になっている
      ticker.time = Long.MAX_VALUE;  //キューから削除する
      if (TKQ_NULL) {
        tkqArray[tkqR & (TKQ_S - 1)] = null;  //未使用領域をnullにする
      }
      tkqR++;  //先頭を進める
      ticker.tick ();  //実行する。ここでtkqRやtkqWが動く場合があることに注意
    }
  }  //tkqRun

  //tkqAdd (ticker, time)
  //  追加
  //  指定されたティッカーに指定された時刻を持たせてキューに追加する
  public static void tkqAdd (Ticker ticker, long time) {
    tkqRemove (ticker);  //キューにあれば削除する
    if ((tkqW - tkqR) == TKQ_S) {  //キューが満杯
      throw new Error ("tkqAdd: overflow");
    }
    ticker.time = time;  //キューに追加する
    int w = tkqW;  //後ろから
    Ticker t;
    while (tkqR < w &&
           time < (t = tkqArray[(w - 1) & (TKQ_S - 1)]).time) {  //探す
      tkqArray[w & (TKQ_S - 1)] = t;  //後ろを後ろにずらす
      w--;
    }
    tkqArray[w & (TKQ_S - 1)] = ticker;
    tkqW++;  //末尾を進める
  }  //tkqAdd

  //tkqRemove (ticker)
  //  削除
  //  指定されたティッカーがキューにあればキューから削除する。なければ何もしない
  public static void tkqRemove (Ticker ticker) {
    if (ticker.time == Long.MAX_VALUE) {  //キューにない
      return;
    }
    ticker.time = Long.MAX_VALUE;  //削除する
    int r = tkqR;  //先頭から
    while (tkqArray[r & (TKQ_S - 1)] != ticker) {  //探す。必ずある
      r++;
    }
    while (tkqR < r) {
      tkqArray[r & (TKQ_S - 1)] = tkqArray[(r - 1) & (TKQ_S - 1)];  //手前を後ろにずらす
      r--;
    }
    if (TKQ_NULL) {
      tkqArray[r & (TKQ_S - 1)] = null;  //未使用領域をnullにする
    }
    tkqR++;  //先頭を進める
  }  //tkqRemove

  //tkqTest ()
  //  テスト
  static void tkqTest () {
    //準備
    int n = 200;
    Ticker[] a = new Ticker[n];
    for (int i = 0; i < n; i++) {
      a[i] = new Ticker () {
        @Override void tick () {
        }
      };
    }
    //昇順書き込み
    for (int i = 0; i < n; i++) {
      tkqAdd (a[i], i);
    }
    if (tkqW - tkqR != n) {
      System.out.println ("tkqTest: error 1");
      return;
    }
    for (int i = 0; i < n; i++) {
      if (tkqArray[(tkqR + i) & (TKQ_S - 1)] != a[i]) {
        System.out.println ("tkqTest: error 2");
        return;
      }
    }
    //昇順削除
    for (int i = 0; i < n; i++) {
      tkqRemove (a[i]);
    }
    if (tkqW - tkqR != 0) {
      System.out.println ("tkqTest: error 3");
      return;
    }
    //昇順書き込み
    for (int i = 0; i < n; i++) {
      tkqAdd (a[i], i);
    }
    if (tkqW - tkqR != n) {
      System.out.println ("tkqTest: error 4");
      return;
    }
    for (int i = 0; i < n; i++) {
      if (tkqArray[(tkqR + i) & (TKQ_S - 1)] != a[i]) {
        System.out.println ("tkqTest: error 5");
        return;
      }
    }
    //降順削除
    for (int i = 0; i < n; i++) {
      tkqRemove (a[n - 1 - i]);
    }
    if (tkqW - tkqR != 0) {
      System.out.println ("tkqTest: error 6");
      return;
    }
    //降順書き込み
    for (int i = 0; i < n; i++) {
      tkqAdd (a[n - 1 - i], n - 1 - i);
    }
    if (tkqW - tkqR != n) {
      System.out.println ("tkqTest: error 7");
      return;
    }
    for (int i = 0; i < n; i++) {
      if (tkqArray[(tkqR + i) & (TKQ_S - 1)] != a[i]) {
        System.out.println ("tkqTest: error 8");
        return;
      }
    }
    //昇順削除
    for (int i = 0; i < n; i++) {
      tkqRemove (a[i]);
    }
    if (tkqW - tkqR != 0) {
      System.out.println ("tkqTest: error 9");
      return;
    }
    //降順書き込み
    for (int i = 0; i < n; i++) {
      tkqAdd (a[n - 1 - i], n - 1 - i);
    }
    if (tkqW - tkqR != n) {
      System.out.println ("tkqTest: error 10");
      return;
    }
    for (int i = 0; i < n; i++) {
      if (tkqArray[(tkqR + i) & (TKQ_S - 1)] != a[i]) {
        System.out.println ("tkqTest: error 11");
        return;
      }
    }
    //降順削除
    for (int i = 0; i < n; i++) {
      tkqRemove (a[n - 1 - i]);
    }
    if (tkqW - tkqR != 0) {
      System.out.println ("tkqTest: error 12");
      return;
    }
  }  //tkqTest

}  //class TickerQueue
