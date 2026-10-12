//========================================================================================
//  Printer.java
//    en:Printer Port -- It emulates the printer CZ-8PC4.
//    ja:プリンタポート -- プリンタCZ-8PC4をエミュレートします。
//  Copyright (C) 2003-2024 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;
import javax.imageio.metadata.*;
import javax.imageio.stream.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;


//----------------------------------------------------------------------------------------
//
//  プリンタポート
//    0x00e8c001  bit7-0  write  DATA    プリンタデータ
//    0x00e8c003  bit0    write  STROBE  0→1=プリンタデータ受け取り指示
//    0x00e9c001  bit0    write  INTEN   0=プリンタ割り込み禁止,1=プリンタ割り込み許可。INTENが1でREADYが0→1のとき割り込みがかかる
//                bit5    read   READY   0=プリンタビジー,1=プリンタレディ。逆に書かれている資料があるので注意
//                                       STROBEの0→1から0.5μs以内にREADYが1→0になる
//                                       プリンタが次のデータを受け取る準備ができるとREADYが0→1になる
//                                       オフラインのときは常にプリンタビジー
//    0x00e9c003  bit7-2  write  VECTOR  割り込みベクタ番号
//
//  プリンタ出力手順
//    準備
//      (1)0x00e9c003(VECTOR)に割り込みベクタ番号0x63&0xfc=0x60を設定する
//      (2)0x00e9c001のbit0(INTEN)を1にする
//    ループ
//      (3)プリンタ割り込みを待つか、0x00e9c001のbit5(READY)が1になるまで待つ
//      (4)0x00e8c001(DATA)にデータをセットする
//      (5)0x00e8c003のbit0(STROBE)を0にする
//      (6)0x00e8c003のbit0(STROBE)を1にする
//      (7)0x00e9c001のbit5(READY)が0になる
//      (8)プリンタがデータを受け取る
//      (9)プリンタが次のデータを受け取る準備ができる
//      (10)0x00e9c001のbit5(READY)が1になる
//      (11)0x00e9c001のbit0(INTEN)が1のときプリンタ割り込みがかかる
//
//----------------------------------------------------------------------------------------
//
//  CZ-8PC4
//    48ドット熱転写カラー漢字プリンタ
//    昔使っていた実機のマニュアルを参考にする
//
//  CZ-8PC5
//    CZ-8PC4の後継機
//    半角書体をローマン体とサンセリフ体から、全角書体を明朝体とゴシック体から選択できる
//    Oh!X 1991年3月号に紹介記事
//    書体に関するディップスイッチと制御コードは以下を参考にした
//      http://software.aufheben.info/contents.html?contents_key=cz8pc5
//
//  電源スイッチ
//
//  操作パネルのランプ
//    電源ランプ
//      緑点灯  電源ON
//      消灯    電源OFF
//    リボン切れランプ
//      赤点灯  リボン片面終了
//    用紙切れランプ
//      赤点灯  用紙切れ
//    ハガキランプ
//      橙点灯  はがきモードON
//      消灯    はがきモードOFF
//    特殊リボンランプ
//      橙点灯  特殊リボンモード
//    高速ランプ
//      橙点灯  高速印字
//      消灯    標準速度
//    微小送りランプ
//      橙点灯  微小送りモード
//    セレクトランプ
//      緑点灯  セレクト状態
//      消灯    ディセレクト状態
//
//  操作パネルのスイッチ
//    ハガキスイッチ
//      はがきモードのON/OFF
//    特殊リボンスイッチ
//      特殊リボンモードのON/OFF
//    高速スイッチ
//      高速印字と標準速度の切り替え
//    微小送りスイッチ
//      微小送りモードのON/OFF
//    改頁スイッチ
//      微小送りモードのとき
//        順方向に1/180[in]微小送りする
//      さもなくば(微小送りモードでないとき)
//        改頁する
//    改行スイッチ
//      微小送りモードのとき
//        逆方向に1/180[in]微小送りする
//      さもなくば(微小送りモードでないとき)
//        改行する
//    セレクトスイッチ
//      セレクト状態とディセレクト状態の切り替え
//
//  ディップスイッチ
//    電源投入時、INIT信号入力時、ESC c 1実行時の初期値を選択する
//      SW1  ゼロ書体      OFF  スラッシュあり(工場出荷時)。パイカ、エリート、縮小、半角に有効
//                         ON   スラッシュなし
//      SW2  印字速度      OFF  標準速度(工場出荷時)
//                         ON   高速印字
//      SW3  はがきモード  OFF  OFF(工場出荷時)
//                         ON   ON
//      SW4  頁長          OFF  11[in](工場出荷時)
//                         ON   12[in]
//      SW5                OFF  (固定)
//                         ON
//      SW6  給紙位置      OFF  22mm(工場出荷時)
//                         ON   8.5mm
//      SW7                OFF  (固定)
//                         ON
//      SW8  書体          OFF  ローマン体／明朝体(工場出荷時)
//                         ON   サンセリフ体／ゴシック体
//
//  分解能
//    360[dot/in]
//
//  用紙ガイド
//    用紙ガイドで水平方向の印字開始位置を調節する
//    8[in]の印字領域は動かせないので、幅の異なる用紙(の中央)に印字するために、用紙ガイドを動かして用紙の左端から印字領域までの幅を調節する
//
//  給紙位置と微小送り
//    給紙位置と微小送りで垂直方向の印字開始位置を調節する
//    給紙すると用紙の上端から22[mm]の位置まで送られる。SW6で8.5[mm]に変更できる
//    微小送りモードで用紙を微小送りして印字開始位置を調節する
//    調節後の位置は記憶され、次の用紙を給紙すると調節後の位置まで送られる
//
//  はがきモード
//    はがきモードにすると左マージンと右マージンが自動的に設定される
//    左マージン
//      1.9[in]=684[dot]=48.26[mm]
//    右マージン
//      7.4[in]=2664[dot]=187.96[mm]
//    幅
//      7.4[in]-1.9[in]=5.5[in]=1980[dot]=139.7[mm]
//!!! はがきモードの左右マージンの設定はなく、通常の用紙と同じ扱いになっている
//
//  文字種
//    パイカ
//      10文字/in
//      36dot/文字
//      floor(2880dot/行/36dot/文字)=80文字/行
//      A4縦
//        (210-14-9)/(25.4/10)=73文字/行
//      B5縦
//        (182-13-9)/(25.4/10)=63文字/行
//    エリート
//      12文字/in
//      30dot/文字
//      floor(2880dot/行/30dot/文字)=96文字/行
//      A4縦
//        (210-14-9)/(25.4/12))=88文字/行
//      B5縦
//        (182-13-9)/(25.4/12)=75文字/行
//    縮小
//      17文字/in
//      21dot/文字
//      floor(2880dot/行/21dot/文字)=137文字/行
//      A4縦
//        126文字/行
//      B5縦
//        108文字/行
//    漢字
//      2+48+6=56dot/文字
//      floor(2880dot/行/56dot/文字)=51文字/行
//    半角
//      0+24+4=28dot/文字
//      floor(2880dot/行/28dot/文字)=102文字/行
//      A4縦
//        94文字/行
//      B5縦
//        81文字/行
//    スーパースクリプト、サブスクリプト
//      15文字/in
//      24dot/文字
//      floor(2880dot/行/24dot/文字)=120文字/行
//
//  漢字モード
//    漢字モードOFFのとき
//      1バイトで1文字
//      ANKとみなしてパイカ、エリート、縮小のいずれかで印字する
//    漢字モードONのとき
//      2バイトで1文字
//      1バイト目が0x00のとき
//        2バイト目をANKとみなして半角で印字する
//      さもなくば(1バイト目が0x00でないとき)
//        1バイト目が区(0x21～0x7e)、2バイト目が点(0x21～0x7e)のJISコードとみなして全角で印字する
//
//  黒リボン
//    黒インクが塗布されたインクリボン
//
//  単色カラーリボン
//    赤、青、緑、金、銀などのインクが塗布された単色のインクリボン
//
//  カラーリボン
//    イエロー、マゼンタ、シアンの3色のインクが繰り返し塗布されたインクリボン
//
//  カラーモード
//    カラーリボンをセットしてESC EMでカラーモードにするとCRを出力する度にリボンが送られてイエロー、マゼンタ、シアンの順に色が切り替わる
//    1行分のデータをイエロー、マゼンタ、シアンの順に最大3回繰り返し出力して重ね合わせることで減法混色で7色印字できる
//    色コードを直接指定するコマンドはない
//
//----------------------------------------------------------------------------------------

public class PrinterPort {


  //プリンタポート
  //  プリンタが繋がっていなくてもプリンタポートは存在する
  //  プリンタが繋がっていないときは常にプリンタビシー
  public static final int PRN_DATA   = 0x00e8c001;
  public static final int PRN_STROBE = 0x00e8c003;
  public static int prnData;  //PRN_DATAにwriteされたプリンタデータ
  public static int prnStrobe;  //PRN_STROBEにwriteされたストローブ。0→1のときprnDataがプリンタまたはプリンタアダプタに出力される


  //プリンタアダプタ
  //  フォントの作成や展開に時間がかかるのでプリンタのスレッドを分ける
  //  プリンタポートとプリンタの間にプリンタアダプタを挟む
  //  データをプリンタに出力する代わりにプリンタアダプタに出力する
  //  プリンタアダプタはプリンタアダプタバッファが一杯になるか一定時間出力がないとき、
  //  プリンタアダプタスレッドでプリンタアダプタタスクを実行する
  //  プリンタアダプタタスクがプリンタにデータを出力する
  //
  //  プリンタアダプタ出力
  //    出力するデータをプリンタアダプタバッファに追加する
  //    プリンタアダプタバッファが一杯にならなかったとき
  //      プリンタアダプタティッカーをキューに入れ(直し)て10ms後に呼び出す
  //    プリンタアダプタバッファが一杯になったとき
  //      プリンタアダプタティッカーがキューにあれば取り除く
  //      プリンタアダプタタスクランチャを呼び出す
  //      プリンタアダプタバッファをクリアする
  //
  //  プリンタアダプタティッカー
  //    プリンタアダプタタスクランチャを呼び出す
  //
  //  プリンタアダプタタスクランチャ
  //    プリンタアダプタバッファのコピーを持ったプリンタアダプタタスクをプリンタアダプタスレッドで実行する
  //
  //  プリンタアダプタタスク
  //    プリンタアダプタバッファのコピーの内容をプリンタに出力する
  //
  public static final boolean PRN_USE_ADAPTER = true;  //プリンタアダプタを使う
  public static final int PRN_ADAPTER_CAPACITY = 1024;  //プリンタアダプタバッファの容量
  public static final long PRN_ADAPTER_DELAY = XEiJ.TMR_FREQ * 10000 / 1000000;  //10ms。プリンタアダプタディレイタイム。プリンタアダプタバッファが一杯にならなくても最後のプリンタアダプタ出力からこの時間が経過したらプリンタアダプタタスクを実行する
  public static byte[] prnAdapterBuffer;  //プリンタアダプタバッファ
  public static int prnAdapterPointer;  //プリンタアダプタバッファの書き込み位置
  public static TickerQueue.Ticker prnAdapterTicker;  //プリンタアダプタティッカー
  public static java.util.Timer prnAdapterTimer;  //プリンタアダプタスレッド。Timerだけだとjavax.swing.Timerと紛らわしい

  //prnAdapterInit ()
  //  プリンタアダプタ初期化
  public static void prnAdapterInit () {
    prnAdapterBuffer = new byte[PRN_ADAPTER_CAPACITY];  //プリンタアダプタバッファ
    prnAdapterPointer = 0;  //プリンタアダプタバッファの書き込み位置
    prnAdapterTicker = new PrinterAdapterTicker ();  //プリンタアダプタティッカー
    prnAdapterTimer = new java.util.Timer ();  //プリンタアダプタスレッド。Timerだけだとjavax.swing.Timerと紛らわしい
  }  //prnAdapterInit

  //prnAdapterOutput (data)
  //  プリンタアダプタ出力
  public static void prnAdapterOutput (int data) {
    //出力するデータをプリンタアダプタバッファに追加する
    prnAdapterBuffer[prnAdapterPointer++] = (byte) data;
    if (prnAdapterPointer < PRN_ADAPTER_CAPACITY) {  //プリンタアダプタバッファが一杯にならなかったとき
      //プリンタアダプタティッカーをキューに入れ(直し)て10ms後に呼び出す
      TickerQueue.tkqAdd (prnAdapterTicker, XEiJ.mpuClockTime + PRN_ADAPTER_DELAY);
    } else {  //プリンタアダプタバッファが一杯になったとき
      //プリンタアダプタティッカーがキューにあれば取り除く
      TickerQueue.tkqRemove (prnAdapterTicker);
      //プリンタアダプタタスクランチャを呼び出す
      prnAdapterTaskLauncher ();
    }
  }  //prnAdapterOutput

  //class PrinterAdapterTicker
  //  プリンタアダプタティッカー
  public static class PrinterAdapterTicker extends TickerQueue.Ticker {
    @Override protected void tick () {
      //プリンタアダプタタスクランチャを呼び出す
      prnAdapterTaskLauncher ();
    }  //tick
  };

  //prnAdapterTaskLauncher ()
  //  プリンタアダプタタスクランチャ
  public static void prnAdapterTaskLauncher () {
    //プリンタアダプタバッファをコピーする
    byte[] copiedBuffer = Arrays.copyOf (prnAdapterBuffer, prnAdapterPointer);
    //プリンタアダプタバッファをクリアする
    prnAdapterPointer = 0;
    //プリンタアダプタバッファのコピーを持ったプリンタアダプタタスクをプリンタアダプタスレッドで実行する
    prnAdapterTimer.schedule (new PrinterAdapterTask (copiedBuffer), 100L);  //0Lだとログが重なってしまい読み難い
  }  //prnAdapterTaskLauncher

  //class PrinterAdapterTask
  //  プリンタアダプタタスク
  public static class PrinterAdapterTask extends TimerTask {
    private byte[] buffer;
    public PrinterAdapterTask (byte[] copiedBuffer) {
      buffer = copiedBuffer;
    }  //PrinterAdapterTask
    @Override public void run () {
      //プリンタアダプタバッファのコピーの内容をプリンタに出力する
      for (int i = 0; i < buffer.length; i++) {
        prnOutput (buffer[i]);
      }
    }  //run
  }  //class PrinterAdapterTask


  //プリンタ

  //カラーモデル
  //  1dotを1byteで表現する。下位3bitだけ使う
  //    0  0xff000000  ブラック
  //    1  0xff0000ff  ブルー
  //    2  0xff00ff00  ライム
  //    3  0xff00ffff  シアン
  //    4  0xffff0000  レッド
  //    5  0xffff00ff  マゼンタ
  //    6  0xffffff00  イエロー
  //    7  0xffffffff  ホワイト
  //  最初に用紙の全体を7(ホワイト)で塗り潰す
  //  黒リボンのときは0(ブラック)、カラーリボンのときは6(イエロー)、5(マゼンタ)、3(シアン)の順にANDで描画する
  //  ダークモードは黒と白だけ入れ替える
  public static IndexColorModel prnImageColorModel;  //ライトモードのカラーモデル
  public static IndexColorModel prnDarkImageColorModel;  //ダークモードのカラーモデル


  //用紙

  //  定数
  public static final int PRN_MAX_WIDTH_DOT = 360 * 48;  //印字範囲の最大幅[dot]。B0縦の幅1030[mm]=40.6[in]が収まるサイズ。CZ-8PC4の8[in]の6倍
  public static final int PRN_MAX_HEIGHT_DOT = 360 * 66;  //印字範囲の最大高さ[dot]。B0縦の高さ1456[mm]=57.3[in]が収まるサイズ。CZ-8PC4の11[in]の6倍
  public static final int PRN_MAX_WIDTH_MM = (int) Math.floor ((double) PRN_MAX_WIDTH_DOT * 25.4 / 360.0);  //印字範囲の最大幅[mm]
  public static final int PRN_MAX_HEIGHT_MM = (int) Math.floor ((double) PRN_MAX_HEIGHT_DOT * 25.4 / 360.0);  //印字範囲の最大高さ[mm]

  //  列
  public static final int PRN_A_SERIES = 0;  //A列
  public static final int PRN_B_SERIES = 1;  //B列
  public static final int PRN_POSTCARD = 2;  //はがき

  //  方向
  public static final int PRN_PORTRAIT  = 0;  //縦
  public static final int PRN_LANDSCAPE = 1;  //横

  //class Paper
  //  用紙クラス
  //  参考
  //    https://ja.wikipedia.org/wiki/%E7%B4%99%E3%81%AE%E5%AF%B8%E6%B3%95
  public static class Paper {

    //サイズ
    public String sizeEn;  //列の英語表記
    public String sizeJa;  //列の日本語表記
    //方向
    public String orientationEn;  //方向の英語表記
    public String orientationJa;  //方向の日本語表記
    //用紙
    public int paperWidthMm;  //用紙の幅[mm]
    public int paperHeightMm;  //用紙の高さ[mm]
    public int paperWidthDot;  //用紙の幅[dot]
    public int paperHeightDot;  //用紙の高さ[dot]
    //印字不可領域の初期値
    //  用紙の端の印字できない部分。ヘッドが移動できない場所
    public int initialDeadTopMm;  //用紙の上端の印字できない部分の高さの初期値[mm]
    public int initialDeadLeftMm;  //用紙の左端の印字できない部分の幅の初期値[mm]
    public int initialDeadRightMm;  //用紙の右端の印字できない部分の幅の初期値[mm]
    public int initialDeadBottomMm;  //用紙の下端の印字できない部分の高さの初期値[mm]
    //名前
    public String nameEn;  //英語名
    public String nameJa;  //日本語名

    //new Paper (series, n, orientation)
    //  コンストラクタ
    //  series       列
    //               PRN_A_SERIES  A列
    //               PRN_B_SERIES  B列
    //               PRN_POSTCARD  はがき
    //  n            サイズ
    //               4  A4またはB4
    //               5  A5またはB5
    //               など
    //  orientation  方向
    //               PRN_PORTRAIT   縦長
    //               PRN_LANDSCAPE  横長
    public Paper (int series, int n, int orientation) {

      int narrowMm;  //短辺[mm]
      int longMm;  //長辺[mm]

      if (series == PRN_A_SERIES) {  //A列
        narrowMm = (int) Math.floor (1000.0 / Math.pow (2.0, (double) (2 * n + 1) * 0.25) + 0.2);
        longMm = (int) Math.floor (1000.0 / Math.pow (2.0, (double) (2 * n - 1) * 0.25) + 0.2);
        initialDeadTopMm = 11;  //A4縦210x297に名刺横91x55がちょうど10枚収まる
        initialDeadLeftMm = 14;
        initialDeadRightMm = 14;
        initialDeadBottomMm = 11;
        sizeEn = n < 0 ? (1 << -n) + "A0" : "A" + n;
        sizeJa = sizeEn;
      } else if (series == PRN_B_SERIES) {  //B列
        narrowMm = (int) Math.floor (1000.0 * Math.sqrt (3.0) / Math.pow (2.0, (double) (2 * n + 3) * 0.25) + 0.2);
        longMm = (int) Math.floor (1000.0 * Math.sqrt (3.0) / Math.pow (2.0, (double) (2 * n + 1) * 0.25) + 0.2);
        initialDeadTopMm = 11;
        initialDeadLeftMm = 24;
        initialDeadRightMm = 24;
        initialDeadBottomMm = 11;
        sizeEn = n < 0 ? (1 << -n) + "B0" : "B" + n;
        sizeJa = sizeEn;
      } else {  //はがき
        narrowMm = 100;
        longMm = 148;
        initialDeadTopMm = 10;
        initialDeadLeftMm = 3;
        initialDeadRightMm = 3;
        initialDeadBottomMm = 10;
        sizeEn = "Postcard";  //Japanese standard
        sizeJa = "はがき";
      }
      if (orientation == PRN_PORTRAIT) {  //縦長
        paperWidthMm = narrowMm;
        paperHeightMm = longMm;
        orientationEn = "portrait";
        orientationJa = "縦";
      } else {  //横長
        paperWidthMm = longMm;
        paperHeightMm = narrowMm;
        orientationEn = "landscape";
        orientationJa = "横";
      }
      paperWidthDot = (int) Math.floor ((double) paperWidthMm * (360.0 / 25.4) + 0.5);
      paperHeightDot = (int) Math.floor ((double) paperHeightMm * (360.0 / 25.4) + 0.5);

      nameEn = sizeEn + " " + orientationEn + " " + paperWidthMm + "x" + paperHeightMm;
      nameJa = sizeJa + " " + orientationJa + " " + paperWidthMm + "x" + paperHeightMm;

    }  //Paper

  }  //class Paper



  //用紙の配列
  public static Paper[] prnPaperArray;

  //用紙
  public static Paper prnNextPaper;  //次に給紙する用紙。nullではない
  public static Paper prnCurrentPaper;  //現在の用紙。null=未給紙

  //回転
  //  0=0°,1=90°,2=180°,3=270°
  public static int prnNextRotation;  //次に給紙する用紙の回転
  public static int prnRotation;  //現在の用紙の回転
  public static int prnRotatedWidthDot;  //現在の用紙の回転後の幅[dot]
  public static int prnRotatedHeightDot;  //現在の用紙の回転後の高さ[dot]
  public static int prnM11;  //用紙座標からビットマップ座標へ変換する変換行列
  public static int prnM12;  //  [m11 m12 m13]   [x]   [m11*x+m12*y+m13]
  public static int prnM13;  //  [m21 m22 m23] * [y] = [m21*x+m22*y+m23]
  public static int prnM21;  //  [ 0   0   1 ]   [1]   [       1       ]
  public static int prnM22;
  public static int prnM23;
  public static int prnIncrementX;  //prnM11+prnRotatedWidthDot*prnM21。用紙座標でxが1増えたときのビットマップインデックスの増分
  public static int prnIncrementY;  //prnM12+prnRotatedWidthDot*prnM22。用紙座標でyが1増えたときのビットマップインデックスの増分

  //ダークモード
  //  白と黒を入れ替える
  public static boolean prnNextDarkMode;  //次に給紙する用紙のダークモード
  public static boolean prnDarkMode;  //現在の用紙のダークモード

  //オンライン
  public static boolean prnOnlineOn;  //false=オフライン,true=オンライン

  //単色インクリボンの色
  //  0=ブラック,1=ブルー,2=ライム,3=シアン,4=レッド,5=マゼンタ,6=イエロー,7=ホワイト
  public static int prnSingleColor;

  //表示倍率
  //  -4=1/16,-3=1/8,-2=1/4,-1=1/2,0=1,1=2,2=4,3=8,4=16
  public static int prnScaleShift;

  //イメージとビットマップ
  //  給紙する度に作り直す
  public static BufferedImage prnImage;  //現在の用紙のイメージ
  public static byte[] prnBitmap;  //現在の用紙のビットマップ

  //印字フラグ
  //  印字ありのとき排紙するとイメージが保存される
  public static boolean prnPrinted;  //false=印字なし,true=印字あり



  //設定

  //  マージン
  public static int prnMarginLeftX;  //左マージンの印字可能範囲座標[dot]。初期値は0
  public static int prnMarginRightX;  //右マージンの印字可能範囲座標[dot]。初期値は2879
  public static int prnMarginBottomHeight;  //下マージンの高さ[dot]。初期値は0

  //改行ピッチ
  //  ESC 6で1/6[in]=60[dot]、ESC 8で1/8[in]=45[dot]になる
  //  ESC % 9 0で1/6[in]または1/8[in]に戻すときに使う
  public static int prnDefaultLineHeight;  //デフォルトの改行ピッチ[dot]
  public static int prnLineHeight;  //改行ピッチ[dot]

  //文字種
  public static final int PRN_PICA  = 0;  //パイカ 1/10[in]=36[dot]
  public static final int PRN_ELITE = 1;  //エリート 1/12[in]=30[dot]
  public static final int PRN_SMALL = 2;  //縮小 1/20[in]=18[dot]。隙間が3[dot]あって1/17[in]=21[dot]になる
  public static int prnCharacterType;  //文字種

  //ひらがなモード
  public static boolean prnHiraganaMode;  //false=ひらがなOFF,true=ひらがなON

  //スクリプトモード
  public static final int PRN_NO_SCRIPT    = 0;  //スクリプト解除
  public static final int PRN_SUPER_SCRIPT = 1;  //スーパースクリプト
  public static final int PRN_SUB_SCRIPT   = 2;  //サブスクリプト
  public static int prnScriptMode;  //スクリプトモード

  //強調モード
  public static boolean prnStrongMode;  //false=強調OFF,true=強調ON

  //アンダーラインモード
  public static boolean prnUnderlineMode;  //false=アンダーラインOFF,true=アンダーラインON

  //文字スタイル
  public static final int PRN_NORMAL_STYLE      = 0;  //標準文字
  public static final int PRN_OPEN_STYLE        = 1;  //袋文字
  public static final int PRN_SHADOW_STYLE      = 2;  //影文字
  public static final int PRN_OPEN_SHADOW_STYLE = 3;  //袋影文字
  public static int prnCharacterStyle;  //文字スタイル

  //漢字モード
  public static boolean prnKanjiMode;  //false=漢字モードOFF,true=漢字モードON

  //外字データ
  //  外字定義エリアは0x7621..0x767eと0x7721..0x7726の100文字
  //  ESC c 1でクリアされる
  public static byte[] prnGaijiData = null;

  //縦書きモード
  public static boolean prnVerticalWritingMode;  //false=縦書きOFF,true=縦書きON

  //左右スペース
  public static int prnFullWidthLeftSpace;  //全角左スペース[dot]
  public static int prnFullWidthRightSpace;  //全角右スペース[dot]
  public static int prnHalfWidthLeftSpace;  //半角左スペース[dot]
  public static int prnHalfWidthRightSpace;  //半角右スペース[dot]

  //横2倍モード
  public static boolean prnHorizontalDoubleSizeMode;  //false=横2倍OFF,true=横2倍ON。漢字モードの全角と半角もビットイメージも拡大する
  public static boolean prnVerticalDoubleSizeMode;  //false=縦2倍OFF,true=縦2倍ON。漢字モードの全角と半角もビットイメージも拡大する
  public static boolean prnKanjiHorizontalDoubleSizeMode;  //false=漢字横2倍OFF,true=漢字横2倍ON。漢字モードの全角と半角だけ拡大する

  //水平タブ
  public static final int PRN_HORIZONTAL_ANCHOR_LIMIT = 16;
  public static final int[] prnHorizontalTabAnchor = new int[PRN_HORIZONTAL_ANCHOR_LIMIT];  //水平タブアンカー[dot]

  //垂直タブ
  public static final int PRN_VERTICAL_ANCHOR_LIMIT = 128;
  public static final int[] prnVerticalTabAnchor = new int[PRN_VERTICAL_ANCHOR_LIMIT];  //垂直タブアンカー

  //カラーモード
  public static boolean prnColorMode;  //false=単色モード,true=カラーモード

  //色
  //  0=ブラック,1=ブルー,2=ライム,3=シアン,4=レッド,5=マゼンタ,6=イエロー,7=ホワイト
  //  単色モードのときprnSingleColorと同じで0～7
  //  カラーモードのときESC EMで6になりCRで6→5→3→6の順に切り替わる
  public static int prnCurrentColor;

  //書体
  public static int prnHalfWidthFont;  //半角書体。0=ローマン体,1=サンセリフ体
  public static int prnFullWidthFont;  //全角書体。0=明朝体,1=ゴシック体


  //印字不可領域
  public static int prnNextDeadTopMm;  //次に給紙する用紙の上端の印字できない部分の高さ[mm]
  public static int prnNextDeadLeftMm;  //次に給紙する用紙の左端の印字できない部分の幅[mm]
  public static int prnNextDeadRightMm;  //次に給紙する用紙の右端の印字できない部分の幅[mm]
  public static int prnNextDeadBottomMm;  //次に給紙する用紙の下端の印字できない部分の高さ[mm]
  public static int prnDeadTopMm;  //現在の用紙の上端の印字できない部分の高さ[mm]
  public static int prnDeadLeftMm;  //現在の用紙の左端の印字できない部分の幅[mm]
  public static int prnDeadRightMm;  //現在の用紙の右端の印字できない部分の幅[mm]
  public static int prnDeadBottomMm;  //現在の用紙の下端の印字できない部分の高さ[mm]

  //印字可能範囲
  //  用紙から印字不可領域を除いた部分。ヘッドが移動できる場所
  //  ヘッドの位置が印字可能範囲の下端からはみ出したら排紙する
  public static int prnAliveTopY;  //印字可能範囲の上端の用紙座標[dot]
  public static int prnAliveLeftX;  //印字可能範囲の左端の用紙座標[dot]
  public static int prnAliveRightX;  //印字可能範囲の右端の用紙座標[dot]
  public static int prnAliveBottomY;  //印字可能範囲の下端の用紙座標[dot]

  //ページ範囲
  //  X方向は印字可能範囲の左端から右端まで
  //  Y方向はページ開始位置からページ開始位置+ページ長まで
  //  ページ範囲の下端
  //    ページ開始位置+ページ長
  //    下マージンの基準の位置
  //    改ページの移動先
  //    ページ範囲の下端が印字可能範囲の内側にあるとき
  //      改ページを行うとヘッドがページ範囲の下端に移動してそこが新たなページ開始位置になる
  //    ページ範囲の下端が印字可能範囲の外側にあるとき
  //      改ページを行うとヘッドが印字可能範囲の下端からはみ出して排紙される
  //      次に給紙された用紙の印刷可能範囲の先頭が新たなページ開始位置になる
  //  ページ長
  //    A4縦は上下の印字不可領域が17[mm]までのとき12[in]、18[mm]以上のとき11[in]
  public static int prnPageStart;  //ページ開始位置の印字可能範囲座標[dot]。給紙したとき0
  public static int prnPageLength;  //ページ長[dot]。給紙したとき印字可能範囲の高さを1[in]=360[dot]の倍数に切り上げた値

  //コンテント範囲
  //  ページ範囲からマージンを除いた部分
  //  ヘッドの位置がコンテント範囲の下端を超えると改ページされてページ範囲の下端に移動する
  //  ページ範囲の下端が印字可能範囲の下端を超えていれば排紙される
  public static int prnContentTopY;  //コンテント範囲の上端の印字可能範囲座標[dot]。給紙したときページ開始位置
  public static int prnContentBottomY;  //コンテント範囲の下端の印字可能範囲座標[dot]。給紙したときページ開始位置+ページ長-下マージン

  //ヘッドの位置
  public static int prnHeadX;  //ヘッドX座標[dot]。-1=未給紙。改行したとき左マージン
  public static int prnHeadY;  //ヘッドY座標[dot]。-1=未給紙。改ページしたとき新しいページのコンテント範囲の上端
  public static int prnHeadLine;  //ヘッド行番号。-1=未給紙。改ページしたとき0、改行でインクリメント。垂直タブで使う


  //コマンドバッファ
  //  先頭の数バイトで長さが確定するコマンド
  //    対応しているコマンドはすべてバッファに収まる
  //  終了コードが出てくるまで長さが分からないコマンド
  //    コマンドがコマンドバッファに収まらないとき
  //      書き込み位置をコマンドバッファの末尾で止めて末尾の1バイト以外のはみ出した部分のデータを無視する
  public static final byte[] prnCommandBuffer = new byte[4 + 6 * PRN_MAX_WIDTH_DOT];  //ESC M n1 n2 d1 d2 … dkの最大幅2880ドット
  public static int prnCommandLength;  //コマンドの長さ。次にコマンドを判別する位置。この位置まではバッファに書き込むだけ。prnCommandBuffer.length以上になることはない
  public static int prnCommandPointer;  //コマンド書き込み位置。prnCommandBufferのサイズを超える場合があるので注意


  //ウインドウ
  public static ScrollCanvas prnCanvas;  //キャンバス
  public static JFrame prnFrame;  //ウインドウ

  //メニュー
  public static JCheckBoxMenuItem prnOnlineMenuItem;  //オンライン
  public static JCheckBoxMenuItem prnAutosaveMenuItem;  //自動保存
  //
  public static int prnSpinnerLocked;
  public static SpinnerNumberModel prnDeadTopModel;  //用紙の上端の印字できない部分の高さ[mm]
  public static SpinnerNumberModel prnDeadLeftModel;  //用紙の左端の印字できない部分の幅[mm]
  public static SpinnerNumberModel prnDeadRightModel;  //用紙の右端の印字できない部分の幅[mm]
  public static SpinnerNumberModel prnDeadBottomModel;  //用紙の下端の印字できない部分の高さ[mm]
  public static NumberSpinner prnDeadTopSpinner;  //用紙の上端の印字できない部分の高さ[mm]
  public static NumberSpinner prnDeadLeftSpinner;  //用紙の左端の印字できない部分の幅[mm]
  public static NumberSpinner prnDeadRightSpinner;  //用紙の右端の印字できない部分の幅[mm]
  public static NumberSpinner prnDeadBottomSpinner;  //用紙の下端の印字できない部分の高さ[mm]
  public static JRadioButtonMenuItem[] prnScaleMenuItem;

  //プリンタ出力
  public static boolean prnAutosaveOn;  //true=プリンタ出力を自動保存する
  public static String prnSavePath;  //プリンタ出力を保存するディレクトリのabsoluteパス
  public static String prnSaveName;  //主ファイル名。フルパスのファイル名はprnSavePath+File.separator+prnSaveName
  public static JDialog prnSaveDialog;  //ダイアログ
  public static JFileChooser2 prnSaveFileChooser;  //ファイルチューザー
  public static String[] prnWriterSuffixes;  //出力できるイメージファイルの拡張子の配列
  public static javax.swing.filechooser.FileFilter prnSaveFileFilter;  //プリンタ出力イメージファイルフィルタ
  public static JCheckBox prnAutosaveCheckBox;  //自動保存チェックボックス

  //ディップスイッチ
  public static final int PRN_DIPSW_ZERO_STYLE       = 0x01;  //ゼロ書体。0=スラッシュあり,1=スラッシュなし
  public static final int PRN_DIPSW_PRINTING_SPEED   = 0x02;  //印字速度。0=標準速度,1=高速印字
  public static final int PRN_DIPSW_POSTCARD_MODE    = 0x04;  //はがきモード。0=OFF,1=ON
  public static final int PRN_DIPSW_PAGE_LENGTH      = 0x08;  //頁長。0=11[in],1=12[in]
  public static final int PRN_DIPSW_FEEDING_POSITION = 0x20;  //給紙位置。0=22mm,1=8.5mm
  public static final int PRN_DIPSW_FONT_STYLE       = 0x80;  //書体。0=ローマン体／明朝体,1=サンセリフ体／ゴシック体
  public static final int PRN_DIPSW_MASK             = 0xaf;
  public static int prnDIPSW;  //ディップスイッチの状態


  //ページの名前
  //  英語
  public static final String FNT_EN_Zen48x48G = "Full Kanji 48x48 Gothic";
  public static final String FNT_EN_Pic36x46S = "CZ-8PC4 Pica 36x46 Sans-Serif";
  public static final String FNT_EN_Eli30x46S = "CZ-8PC4 Elite 30x46 Sans-Serif";
  public static final String FNT_EN_Sma18x46S = "CZ-8PC4 Small 18x46 Sans-Serif";
  public static final String FNT_EN_Scr28x32S = "CZ-8PC4 Script 28x32 Sans-Serif";
  public static final String FNT_EN_Pan24x48S = "CZ-8PC4 Half 24x48 Sans-Serif";
  public static final String FNT_EN_Zen48x48M = "Full Kanji 48x48 Mincho";
  public static final String FNT_EN_Pic36x46R = "CZ-8PC4 Pica 36x46 Roman";
  public static final String FNT_EN_Eli30x46R = "CZ-8PC4 Elite 30x46 Roman";
  public static final String FNT_EN_Sma18x46R = "CZ-8PC4 Small 18x46 Roman";
  public static final String FNT_EN_Scr28x32R = "CZ-8PC4 Script 28x32 Roman";
  public static final String FNT_EN_Pan24x48R = "CZ-8PC4 Half 24x48 Roman";
  //  日本語
  public static final String FNT_JA_Zen48x48G = "全角 漢字 48x48 ゴシック体";
  public static final String FNT_JA_Pic36x46S = "CZ-8PC4 パイカ 36x46 サンセリフ体";
  public static final String FNT_JA_Eli30x46S = "CZ-8PC4 エリート 30x46 サンセリフ体";
  public static final String FNT_JA_Sma18x46S = "CZ-8PC4 縮小 18x46 サンセリフ体";
  public static final String FNT_JA_Scr28x32S = "CZ-8PC4 スクリプト 28x32 サンセリフ体";
  public static final String FNT_JA_Pan24x48S = "CZ-8PC4 半角 24x48 サンセリフ体";
  public static final String FNT_JA_Zen48x48M = "全角 漢字 48x48 明朝体";
  public static final String FNT_JA_Pic36x46R = "CZ-8PC4 パイカ 36x46 ローマン体";
  public static final String FNT_JA_Eli30x46R = "CZ-8PC4 エリート 30x46 ローマン体";
  public static final String FNT_JA_Sma18x46R = "CZ-8PC4 縮小 18x46 ローマン体";
  public static final String FNT_JA_Scr28x32R = "CZ-8PC4 スクリプト 28x32 ローマン体";
  public static final String FNT_JA_Pan24x48R = "CZ-8PC4 半角 24x48 ローマン体";

  //ページ
  public static FontPage.Zen fntPageZen48x48G;
  public static FontPage.Prn fntPagePic36x46S;
  public static FontPage.Prn fntPageEli30x46S;
  public static FontPage.Prn fntPageSma18x46S;
  public static FontPage.Prn fntPageScr28x32S;
  public static FontPage.Prn fntPagePan24x48S;
  public static FontPage.Zen fntPageZen48x48M;
  public static FontPage.Prn fntPagePic36x46R;
  public static FontPage.Prn fntPageEli30x46R;
  public static FontPage.Prn fntPageSma18x46R;
  public static FontPage.Prn fntPageScr28x32R;
  public static FontPage.Prn fntPagePan24x48R;

  //等幅フォントファミリ
  public static final String[] FNT_GOTHIC_FAMILIES = {  //ゴシック
    //"游ゴシック",  //Windows,Mac
    "ＭＳ ゴシック", "MS Gothic",  //Windows
    "ヒラギノ角ゴ ProN W3", "Hiragino Kaku Gothic ProN",  //Mac
    "ヒラギノ角ゴ Pro W3", "Hiragino Kaku Gothic Pro",
    "Osaka－等幅", "Osaka-Mono",
    "VL ゴシック", "VL Gothic",  //Linux
    "Takaoゴシック", "TakaoGothic",
    "IPAゴシック", "IPAGothic",
  };
  public static final String[] FNT_MINCHO_FAMILIES = {  //明朝
    //"游明朝",  //Windows,Mac
    "ＭＳ 明朝", "MS Mincho",  //Windows
    "ヒラギノ明朝 ProN W3", "Hiragino Mincho ProN",  //Mac
    "ヒラギノ明朝 Pro W3", "Hiragino Mincho Pro",
    "さざなみ明朝", "Sazanami Mincho",  //Linux
    "Takao明朝", "TakaoMincho",
    "IPA明朝", "IPAMincho",
  };
  public static String[] fntAvailableFamilies;  //使えるフォントの一覧
  public static String fntGothicFamily;  //ゴシック
  public static String fntMinchoFamily;  //明朝


  //prnInit ()
  //  初期化
  public static void prnInit () {

    //フォント
    fntPageZen48x48G = new FontPage.Zen (48, 48, FNT_EN_Zen48x48G, FNT_JA_Zen48x48G, "./zen48x48g.f48", "./zen48x48g.png");
    fntPagePic36x46S = new FontPage.Prn (36, 46, FNT_EN_Pic36x46S, FNT_JA_Pic36x46S, "./pic36x46s.dat", "./pic36x46s.png");
    fntPageEli30x46S = new FontPage.Prn (30, 46, FNT_EN_Eli30x46S, FNT_JA_Eli30x46S, "./eli30x46s.dat", "./eli30x46s.png");
    fntPageSma18x46S = new FontPage.Prn (18, 46, FNT_EN_Sma18x46S, FNT_JA_Sma18x46S, "./sma18x46s.dat", "./sma18x46s.png");
    fntPageScr28x32S = new FontPage.Prn (28, 32, FNT_EN_Scr28x32S, FNT_JA_Scr28x32S, "./scr28x32s.dat", "./scr28x32s.png");
    fntPagePan24x48S = new FontPage.Prn (24, 48, FNT_EN_Pan24x48S, FNT_JA_Pan24x48S, "./pan24x48s.dat", "./pan24x48s.png");
    fntPageZen48x48M = new FontPage.Zen (48, 48, FNT_EN_Zen48x48M, FNT_JA_Zen48x48M, "./zen48x48m.f48", "./zen48x48m.png");
    fntPagePic36x46R = new FontPage.Prn (36, 46, FNT_EN_Pic36x46R, FNT_JA_Pic36x46R, "./pic36x46r.dat", "./pic36x46r.png");
    fntPageEli30x46R = new FontPage.Prn (30, 46, FNT_EN_Eli30x46R, FNT_JA_Eli30x46R, "./eli30x46r.dat", "./eli30x46r.png");
    fntPageSma18x46R = new FontPage.Prn (18, 46, FNT_EN_Sma18x46R, FNT_JA_Sma18x46R, "./sma18x46r.dat", "./sma18x46r.png");
    fntPageScr28x32R = new FontPage.Prn (28, 32, FNT_EN_Scr28x32R, FNT_JA_Scr28x32R, "./scr28x32r.dat", "./scr28x32r.png");
    fntPagePan24x48R = new FontPage.Prn (24, 48, FNT_EN_Pan24x48R, FNT_JA_Pan24x48R, "./pan24x48r.dat", "./pan24x48r.png");

    //カラーモデル
    prnImageColorModel = new IndexColorModel (
      8, 8,
      new byte[] {  0,  0,  0,  0, -1, -1, -1, -1 },  //red
      new byte[] {  0,  0, -1, -1,  0,  0, -1, -1 },  //green
      new byte[] {  0, -1,  0, -1,  0, -1,  0, -1 }  //blue
      );
    prnDarkImageColorModel = new IndexColorModel (
      8, 8,
      new byte[] { -1,  0,  0,  0, -1, -1, -1,  0 },  //red
      new byte[] { -1,  0, -1, -1,  0,  0, -1,  0 },  //green
      new byte[] { -1, -1,  0, -1,  0, -1,  0,  0 }  //blue
      );

    //用紙の配列
    prnPaperArray = new Paper[] {
      //縦長
      new Paper (PRN_A_SERIES, 3, PRN_PORTRAIT),  //0 A3縦
      new Paper (PRN_A_SERIES, 4, PRN_PORTRAIT),  //1 A4縦
      new Paper (PRN_A_SERIES, 5, PRN_PORTRAIT),  //2 A5縦
      new Paper (PRN_A_SERIES, 6, PRN_PORTRAIT),  //3 A6縦
      new Paper (PRN_B_SERIES, 3, PRN_PORTRAIT),  //4 B3縦
      new Paper (PRN_B_SERIES, 4, PRN_PORTRAIT),  //5 B4縦
      new Paper (PRN_B_SERIES, 5, PRN_PORTRAIT),  //6 B5縦
      new Paper (PRN_B_SERIES, 6, PRN_PORTRAIT),  //7 B6縦
      new Paper (PRN_POSTCARD, 0, PRN_PORTRAIT),  //8 はがき縦
      //横長
      new Paper (PRN_A_SERIES, 3, PRN_LANDSCAPE),  //9 A3横
      new Paper (PRN_A_SERIES, 4, PRN_LANDSCAPE),  //10 A4横
      new Paper (PRN_A_SERIES, 5, PRN_LANDSCAPE),  //11 A5横
      new Paper (PRN_A_SERIES, 6, PRN_LANDSCAPE),  //12 A6横
      new Paper (PRN_B_SERIES, 3, PRN_LANDSCAPE),  //13 B3横
      new Paper (PRN_B_SERIES, 4, PRN_LANDSCAPE),  //14 B4横
      new Paper (PRN_B_SERIES, 5, PRN_LANDSCAPE),  //15 B5横
      new Paper (PRN_B_SERIES, 6, PRN_LANDSCAPE),  //16 B6横
      new Paper (PRN_POSTCARD, 0, PRN_LANDSCAPE),  //17 はがき横
    };

    //パラメータ
    //  自動保存
    prnAutosaveOn = Settings.sgsGetOnOff ("prnauto");
    //  ディレクトリ
    prnSavePath = Settings.sgsGetString ("prnpath");
    //  ディップスイッチ
    prnDIPSW = Settings.sgsGetInt ("prndipsw") & PRN_DIPSW_MASK;
    //  用紙
    String size = Settings.sgsGetString ("prnsize");
    String orientation = Settings.sgsGetString ("prnorientation");
    prnNextPaper = prnPaperArray[1];  //A4縦
    for (Paper paper : prnPaperArray) {
      if (size.equalsIgnoreCase (paper.sizeEn) &&
          orientation.equalsIgnoreCase (paper.orientationEn)) {
        prnNextPaper = paper;
        break;
      }
    }
    prnCurrentPaper = null;
    //  印字不可領域
    prnNextDeadTopMm = Math.max (0, Math.min (Settings.sgsGetInt ("prntopmargin"), prnNextPaper.paperHeightMm - 1));
    prnNextDeadLeftMm = Math.max (0, Math.min (Settings.sgsGetInt ("prnleftmargin"), prnNextPaper.paperWidthMm - 1));
    prnNextDeadRightMm = Math.max (0, Math.min (Settings.sgsGetInt ("prnrightmargin"), prnNextPaper.paperWidthMm - 1 - prnNextDeadLeftMm));
    prnNextDeadBottomMm = Math.max (0, Math.min (Settings.sgsGetInt ("prnbottommargin"), prnNextPaper.paperHeightMm - 1 - prnNextDeadTopMm));
    //  回転
    prnNextRotation = Math.max (0, Math.min (Settings.sgsGetInt ("prnrotation"), 3));
    prnRotation = 0;
    prnRotatedWidthDot = 0;
    prnRotatedHeightDot = 0;
    prnM11 = 0;
    prnM12 = 0;
    prnM13 = 0;
    prnM21 = 0;
    prnM22 = 0;
    prnM23 = 0;
    prnIncrementX = 0;
    prnIncrementY = 0;
    //  ダークモード
    prnNextDarkMode = Settings.sgsGetOnOff ("prndarkmode");
    prnDarkMode = false;
    //  オンライン
    prnOnlineOn = Settings.sgsGetOnOff ("prnonline");
    //  単色インクリボンの色
    prnSingleColor = Math.max (0, Math.min (Settings.sgsGetInt ("prnsinglecolor"), 7));
    //  表示倍率
    prnScaleShift = Math.max (-4, Math.min (Settings.sgsGetInt ("prnscalefactor"), 4));

    //プリンタポート
    prnReset ();

    //プリンタアダプタ
    if (PRN_USE_ADAPTER) {
      prnAdapterInit ();
    }

    //イメージとビットマップ
    prnImage = null;
    prnBitmap = null;

    //印字フラグ
    prnPrinted = false;

    //設定
    prnResetSettings ();

    //出力
    prnSaveName = "1.png";

  }  //prnInit

  //prnTini ()
  public static void prnTini () {

    //パラメータ
    //  自動保存
    Settings.sgsPutOnOff ("prnauto", prnAutosaveOn);
    //  ディレクトリ
    Settings.sgsPutString ("prnpath", prnSavePath);
    //  ディップスイッチ
    Settings.sgsPutInt ("prndipsw", prnDIPSW);
    //  用紙
    Settings.sgsPutString ("prnsize", prnNextPaper.sizeEn);
    Settings.sgsPutString ("prnorientation", prnNextPaper.orientationEn);
    Settings.sgsPutInt ("prntopmargin", prnNextDeadTopMm);
    Settings.sgsPutInt ("prnleftmargin", prnNextDeadLeftMm);
    Settings.sgsPutInt ("prnrightmargin", prnNextDeadRightMm);
    Settings.sgsPutInt ("prnbottommargin", prnNextDeadBottomMm);
    //  回転
    Settings.sgsPutInt ("prnrotation", prnNextRotation);
    //  ダークモード
    Settings.sgsPutOnOff ("prndarkmode", prnNextDarkMode);
    //  オンライン
    Settings.sgsPutOnOff ("prnonline", prnOnlineOn);
    //  単色インクリボンの色
    Settings.sgsPutInt ("prnsinglecolor", prnSingleColor);
    //  表示倍率
    Settings.sgsPutInt ("prnscalefactor", prnScaleShift);

    //プリンタアダプタ
    if (PRN_USE_ADAPTER) {
      prnAdapterTimer.cancel ();
    }

  }


  //fntGetAvailableFamilies ()
  public static String[] fntGetAvailableFamilies () {
    if (fntAvailableFamilies == null) {
      //フォントファミリの一覧を用意する
      fntAvailableFamilies = GraphicsEnvironment.getLocalGraphicsEnvironment ().getAvailableFontFamilyNames ();
      Arrays.sort (fntAvailableFamilies, null);
    }
    return fntAvailableFamilies;
  }  //fntGetAvailableFamilies

  //fntGetGothicFamily ()
  public static String fntGetGothicFamily () {
    if (fntGothicFamily == null) {
      fntGothicFamily = "Monospaced";
      //使えそうなフォントを探す
      String[] availableFamilies = fntGetAvailableFamilies ();
      for (String family : FNT_GOTHIC_FAMILIES) {
        if (0 <= Arrays.binarySearch (availableFamilies, family, null)) {
          fntGothicFamily = family;
          break;
        }
      }
      System.out.println ((Multilingual.mlnJapanese ? "ゴシック体のフォント: " : "Gothic font: ") + fntGothicFamily);
    }
    return fntGothicFamily;
  }  //fntGetGothicFamily

  //fntGetMinchoFamily ()
  public static String fntGetMinchoFamily () {
    if (fntMinchoFamily == null) {
      fntMinchoFamily = "Monospaced";
      //使えそうなフォントを探す
      String[] availableFamilies = fntGetAvailableFamilies ();
      for (String family : FNT_MINCHO_FAMILIES) {
        if (0 <= Arrays.binarySearch (availableFamilies, family, null)) {
          fntMinchoFamily = family;
          break;
        }
      }
      System.out.println ((Multilingual.mlnJapanese ? "明朝体のフォント: " : "Mincho font: ") + fntMinchoFamily);
    }
    return fntMinchoFamily;
  }  //fntGetMinchoFamily


  //gaijiData = prnGetGaijiData ()
  //  外字データ
  public static byte[] prnGetGaijiData () {
    if (prnGaijiData == null) {
      prnGaijiData = new byte[6 * 48 * 100];
    }
    return prnGaijiData;
  }  //prnGetGaijiData

  //prnStart ()
  public static void prnStart () {
    if (RestorableFrame.rfmGetOpened (Settings.SGS_PRN_FRAME_KEY)) {
      prnOpen ();
    }
  }  //prnStart

  //prnOpen ()
  //  ウインドウを開く
  public static void prnOpen () {
    if (prnFrame == null) {
      prnMakeFrame ();
    }
    XEiJ.pnlExitFullScreen (false);
    prnFrame.setVisible (true);
  }  //prnOpen

  //prnMakeFrame ()
  //  ウインドウを作る
  //  ここでは開かない
  public static void prnMakeFrame () {

    //キャンバス
    prnCanvas = new ScrollCanvas (prnImage);
    prnCanvas.setMargin (10, 10);
    prnCanvas.setMatColor (new Color (LnF.lnfRGB[4]));

    //アクションリスナー
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        String command = ae.getActionCommand ();
        switch (command) {
        case "Online":  //オンライン
          prnSetOnlineOn (((JCheckBoxMenuItem) source).isSelected ());
          break;
        case "Eject":  //排紙
          if (PRN_USE_ADAPTER) {
            //!!! 印刷中に操作すると制御コードの途中に改ページコードが混ざることになる
            prnAdapterTimer.schedule (new PrinterAdapterTask (new byte[] { 0x1a, 0x0c }), 0L);
          }
          break;
        case "Destroy":  //破棄
          if (PRN_USE_ADAPTER) {
            //!!! 印刷中に操作すると制御コードの途中に改行コードが混ざることになる
            prnAdapterTimer.schedule (new PrinterAdapterTask (new byte[] { 0x1a, 0x1a, 0x1a, 0x0c }), 0L);
          }
          break;
        case "Reset":  //リセット
          if (PRN_USE_ADAPTER) {
            //!!! 印刷中に操作すると制御コードの途中に改行コードが混ざることになる
            prnAdapterTimer.schedule (new PrinterAdapterTask (new byte[] { 0x1a, 0x1a, 0x1b, 'c', '1' }), 0L);
          }
          break;
        case "Autosave":  //自動保存
          prnSetAutosaveOn (((JCheckBoxMenuItem) source).isSelected ());
          break;
        case "Close":  //閉じる
          prnFrame.setVisible (false);
          break;
          //
        case "No margins":  //余白なし
          prnNextDeadTopMm = 0;
          prnNextDeadLeftMm = 0;
          prnNextDeadRightMm = 0;
          prnNextDeadBottomMm = 0;
          prnSpinnerLocked++;
          prnDeadTopModel.setValue (Integer.valueOf (prnNextDeadTopMm));
          prnDeadLeftModel.setValue (Integer.valueOf (prnNextDeadLeftMm));
          prnDeadRightModel.setValue (Integer.valueOf (prnNextDeadRightMm));
          prnDeadBottomModel.setValue (Integer.valueOf (prnNextDeadBottomMm));
          prnSpinnerLocked--;
          break;
        case "Reset margins":  //初期値に戻す
          prnNextDeadTopMm = prnNextPaper.initialDeadTopMm;
          prnNextDeadLeftMm = prnNextPaper.initialDeadLeftMm;
          prnNextDeadRightMm = prnNextPaper.initialDeadRightMm;
          prnNextDeadBottomMm = prnNextPaper.initialDeadBottomMm;
          prnSpinnerLocked++;
          prnDeadTopModel.setValue (Integer.valueOf (prnNextDeadTopMm));
          prnDeadLeftModel.setValue (Integer.valueOf (prnNextDeadLeftMm));
          prnDeadRightModel.setValue (Integer.valueOf (prnNextDeadRightMm));
          prnDeadBottomModel.setValue (Integer.valueOf (prnNextDeadBottomMm));
          prnSpinnerLocked--;
          break;
        case "0°":
          prnNextRotation = 0;
          break;
        case "90°":
          prnNextRotation = 1;
          break;
        case "180°":
          prnNextRotation = 2;
          break;
        case "270°":
          prnNextRotation = 3;
          break;
        case "Dark mode":
          prnNextDarkMode = ((JCheckBoxMenuItem) source).isSelected ();
          break;
          //
        case "Black ink ribbon":  //黒色インクリボン
          prnSetSingleColor (0);
          break;
        case "Blue ink ribbon":  //青色インクリボン
          prnSetSingleColor (1);
          break;
        case "Lime ink ribbon":  //緑色インクリボン
          prnSetSingleColor (2);
          break;
        case "Cyan ink ribbon":  //水色インクリボン
          prnSetSingleColor (3);
          break;
        case "Red ink ribbon":  //赤色インクリボン
          prnSetSingleColor (4);
          break;
        case "Magenta ink ribbon":  //紫色インクリボン
          prnSetSingleColor (5);
          break;
        case "Yellow ink ribbon":  //黄色インクリボン
          prnSetSingleColor (6);
          break;
        case "White ink ribbon":  //白色インクリボン
          prnSetSingleColor (7);
          break;
          //
        case "6.25%":
          prnCanvas.setScaleShift (-4);
          break;
        case "12.5%":
          prnCanvas.setScaleShift (-3);
          break;
        case "25%":
          prnCanvas.setScaleShift (-2);
          break;
        case "50%":
          prnCanvas.setScaleShift (-1);
          break;
        case "100%":
          prnCanvas.setScaleShift (0);
          break;
        case "200%":
          prnCanvas.setScaleShift (1);
          break;
        case "400%":
          prnCanvas.setScaleShift (2);
          break;
        case "800%":
          prnCanvas.setScaleShift (3);
          break;
        case "1600%":
          prnCanvas.setScaleShift (4);
          break;
          //
        case "Roman / Mincho":
          prnDIPSW &= ~PRN_DIPSW_FONT_STYLE;
          break;
        case "Sans-Serif / Gothic":
          prnDIPSW |= PRN_DIPSW_FONT_STYLE;
          break;
          //
        default:
          for (Paper paper : prnPaperArray) {
            if (paper.nameEn.equals (command)) {
              prnNextPaper = paper;
              prnNextDeadTopMm = Math.min (prnNextDeadTopMm, prnNextPaper.paperHeightMm - 1);
              prnNextDeadLeftMm = Math.min (prnNextDeadLeftMm, prnNextPaper.paperWidthMm - 1);
              prnNextDeadRightMm = Math.min (prnNextDeadRightMm, prnNextPaper.paperWidthMm - 1 - prnNextDeadLeftMm);
              prnNextDeadBottomMm = Math.min (prnNextDeadBottomMm, prnNextPaper.paperHeightMm - 1 - prnNextDeadTopMm);
              prnSpinnerLocked++;
              prnDeadTopModel.setMaximum (Integer.valueOf (prnNextPaper.paperHeightMm - 1));
              prnDeadLeftModel.setMaximum (Integer.valueOf (prnNextPaper.paperWidthMm - 1));
              prnDeadRightModel.setMaximum (Integer.valueOf (prnNextPaper.paperWidthMm - 1 - prnNextDeadLeftMm));
              prnDeadBottomModel.setMaximum (Integer.valueOf (prnNextPaper.paperHeightMm - 1 - prnNextDeadTopMm));
              prnDeadTopModel.setValue (Integer.valueOf (prnNextDeadTopMm));
              prnDeadLeftModel.setValue (Integer.valueOf (prnNextDeadLeftMm));
              prnDeadRightModel.setValue (Integer.valueOf (prnNextDeadRightMm));
              prnDeadBottomModel.setValue (Integer.valueOf (prnNextDeadBottomMm));
              prnSpinnerLocked--;
              break;
            }
          }
        }
      }
    };

    //用紙メニュー
    ButtonGroup paperGroup = new ButtonGroup ();
    JMenu portraitMenu = Multilingual.mlnText (ComponentFactory.createMenu ("Portrait", 'P'), "ja", "縦長");
    JMenu landscapeMenu = Multilingual.mlnText (ComponentFactory.createMenu ("Landscape", 'L'), "ja", "横長");
    for (int i = 0; i < prnPaperArray.length; i++) {
      Paper paper = prnPaperArray[i];
      (i < prnPaperArray.length >> 1 ? portraitMenu : landscapeMenu).add (
        Multilingual.mlnText (ComponentFactory.createRadioButtonMenuItem (
          paperGroup, paper == prnNextPaper, paper.nameEn, listener), "ja", paper.nameJa));
    }
    //  印字不可領域
    prnSpinnerLocked = 0;
    prnDeadTopModel = new SpinnerNumberModel (prnNextDeadTopMm, 0, prnNextPaper.paperHeightMm - 1, 1);
    prnDeadLeftModel = new SpinnerNumberModel (prnNextDeadLeftMm, 0, prnNextPaper.paperWidthMm - 1, 1);
    prnDeadRightModel = new SpinnerNumberModel (prnNextDeadRightMm, 0, prnNextPaper.paperWidthMm - 1 - prnNextDeadLeftMm, 1);
    prnDeadBottomModel = new SpinnerNumberModel (prnNextDeadBottomMm, 0, prnNextPaper.paperHeightMm - 1 - prnNextDeadTopMm, 1);
    prnDeadTopSpinner = ComponentFactory.createNumberSpinner (prnDeadTopModel, 4, new ChangeListener () {
      @Override public void stateChanged (ChangeEvent ce) {
        if (prnSpinnerLocked == 0) {
          prnNextDeadTopMm = prnDeadTopModel.getNumber ().intValue ();
          prnDeadBottomModel.setMaximum (Integer.valueOf (prnNextPaper.paperHeightMm - 1 - prnNextDeadTopMm));
        }
      }  //stateChanged
    });
    prnDeadLeftSpinner = ComponentFactory.createNumberSpinner (prnDeadLeftModel, 4, new ChangeListener () {
      @Override public void stateChanged (ChangeEvent ce) {
        if (prnSpinnerLocked == 0) {
          prnNextDeadLeftMm = prnDeadLeftModel.getNumber ().intValue ();
          prnDeadRightModel.setMaximum (Integer.valueOf (prnNextPaper.paperWidthMm - 1 - prnNextDeadLeftMm));
        }
      }  //stateChanged
    });
    prnDeadRightSpinner = ComponentFactory.createNumberSpinner (prnDeadRightModel, 4, new ChangeListener () {
      @Override public void stateChanged (ChangeEvent ce) {
        if (prnSpinnerLocked == 0) {
          prnNextDeadRightMm = prnDeadRightModel.getNumber ().intValue ();
          prnDeadLeftModel.setMaximum (Integer.valueOf (prnNextPaper.paperWidthMm - 1 - prnNextDeadRightMm));
        }
      }  //stateChanged
    });
    prnDeadBottomSpinner = ComponentFactory.createNumberSpinner (prnDeadBottomModel, 4, new ChangeListener () {
      @Override public void stateChanged (ChangeEvent ce) {
        if (prnSpinnerLocked == 0) {
          prnNextDeadBottomMm = prnDeadBottomModel.getNumber ().intValue ();
          prnDeadTopModel.setMaximum (Integer.valueOf (prnNextPaper.paperHeightMm - 1 - prnNextDeadBottomMm));
        }
      }  //stateChanged
    });

    //インクリボンメニュー
    ButtonGroup ribbonGroup = new ButtonGroup ();

    //表示メニュー
    ButtonGroup zoomGroup = new ButtonGroup ();
    prnScaleMenuItem = new JRadioButtonMenuItem[9];
    ButtonGroup directionGroup = new ButtonGroup ();
    ButtonGroup fontStyleGroup = new ButtonGroup ();

    //メニューバー
    JMenuBar menuBar = ComponentFactory.createMenuBar (

      //ファイルメニュー
      Multilingual.mlnText (
        ComponentFactory.createMenu (
          "File", 'F',
          prnOnlineMenuItem =
          Multilingual.mlnText (ComponentFactory.createCheckBoxMenuItem (prnOnlineOn, "Online", listener), "ja", "オンライン"),
          ComponentFactory.createHorizontalSeparator (),
          !PRN_USE_ADAPTER ? null : Multilingual.mlnText (ComponentFactory.createMenuItem ("Eject", 'E', listener), "ja", "排紙"),
          !PRN_USE_ADAPTER ? null : Multilingual.mlnText (ComponentFactory.createMenuItem ("Destroy", 'D', listener), "ja", "破棄"),
          !PRN_USE_ADAPTER ? null : Multilingual.mlnText (ComponentFactory.createMenuItem ("Reset", 'R', listener), "ja", "リセット"),
          ComponentFactory.createHorizontalSeparator (),
          prnAutosaveMenuItem =
          Multilingual.mlnText (ComponentFactory.createCheckBoxMenuItem (prnAutosaveOn, "Autosave", listener), "ja", "自動保存"),
          ComponentFactory.createHorizontalSeparator (),
          Multilingual.mlnText (ComponentFactory.createMenuItem ("Close", 'C', listener), "ja", "閉じる")
          ),
        "ja", "ファイル"),

      //用紙メニュー
      Multilingual.mlnText (
        ComponentFactory.createMenu (
          "Paper", 'P',
          portraitMenu,
          landscapeMenu,
          ComponentFactory.createHorizontalSeparator (),
          Multilingual.mlnText (
            ComponentFactory.createMenu (
              "Margin", 'M',
              ComponentFactory.createHorizontalBox (
                Box.createHorizontalGlue (),
                Multilingual.mlnText (ComponentFactory.createLabel ("Top "), "ja", "上 "),
                prnDeadTopSpinner,
                ComponentFactory.createLabel (" mm"),
                Box.createHorizontalGlue ()
                ),
              ComponentFactory.createHorizontalBox (
                Box.createHorizontalGlue (),
                Multilingual.mlnText (ComponentFactory.createLabel ("Left "), "ja", "左 "),
                prnDeadLeftSpinner,
                ComponentFactory.createLabel (" mm"),
                Box.createHorizontalStrut (20),
                Multilingual.mlnText (ComponentFactory.createLabel ("Right "), "ja", "右 "),
                prnDeadRightSpinner,
                ComponentFactory.createLabel (" mm"),
                Box.createHorizontalGlue ()
                ),
              ComponentFactory.createHorizontalBox (
                Box.createHorizontalGlue (),
                Multilingual.mlnText (ComponentFactory.createLabel ("Bottom "), "ja", "下 "),
                prnDeadBottomSpinner,
                ComponentFactory.createLabel (" mm"),
                Box.createHorizontalGlue ()
                ),
              ComponentFactory.createHorizontalSeparator (),
              Multilingual.mlnText (ComponentFactory.createMenuItem ("No margins", listener), "ja", "余白なし"),
              ComponentFactory.createHorizontalSeparator (),
              Multilingual.mlnText (ComponentFactory.createMenuItem ("Reset margins", listener), "ja", "初期値に戻す")
              ),
            "ja", "余白"),
          ComponentFactory.createHorizontalSeparator (),
          ComponentFactory.createRadioButtonMenuItem (directionGroup, prnNextRotation == 0, "0°", listener),
          ComponentFactory.createRadioButtonMenuItem (directionGroup, prnNextRotation == 1, "90°", listener),
          ComponentFactory.createRadioButtonMenuItem (directionGroup, prnNextRotation == 2, "180°", listener),
          ComponentFactory.createRadioButtonMenuItem (directionGroup, prnNextRotation == 3, "270°", listener),
          ComponentFactory.createHorizontalSeparator (),
          Multilingual.mlnText (ComponentFactory.createCheckBoxMenuItem (prnNextDarkMode, "Dark mode", listener), "ja", "ダークモード")
          ),
        "ja", "用紙"),

      //インクリボンメニュー
      Multilingual.mlnText (
        ComponentFactory.createMenu (
          "Ink ribbon", 'R',
          Multilingual.mlnText (ComponentFactory.createRadioButtonMenuItem (ribbonGroup, prnSingleColor == 0,
                                                                "Black ink ribbon", '0', listener),
                                "ja", "黒色インクリボン"),
          Multilingual.mlnText (ComponentFactory.createRadioButtonMenuItem (ribbonGroup, prnSingleColor == 1,
                                                                "Blue ink ribbon", '1', listener),
                                "ja", "青色インクリボン"),
          Multilingual.mlnText (ComponentFactory.createRadioButtonMenuItem (ribbonGroup, prnSingleColor == 2,
                                                                "Lime ink ribbon", '2', listener),
                                "ja", "緑色インクリボン"),
          Multilingual.mlnText (ComponentFactory.createRadioButtonMenuItem (ribbonGroup, prnSingleColor == 3,
                                                                "Cyan ink ribbon", '3', listener),
                                "ja", "水色インクリボン"),
          Multilingual.mlnText (ComponentFactory.createRadioButtonMenuItem (ribbonGroup, prnSingleColor == 4,
                                                                "Red ink ribbon", '4', listener),
                                "ja", "赤色インクリボン"),
          Multilingual.mlnText (ComponentFactory.createRadioButtonMenuItem (ribbonGroup, prnSingleColor == 5,
                                                                "Magenta ink ribbon", '5', listener),
                                "ja", "紫色インクリボン"),
          Multilingual.mlnText (ComponentFactory.createRadioButtonMenuItem (ribbonGroup, prnSingleColor == 6,
                                                                "Yellow ink ribbon", '6', listener),
                                "ja", "黄色インクリボン"),
          Multilingual.mlnText (ComponentFactory.createRadioButtonMenuItem (ribbonGroup, prnSingleColor == 7,
                                                                "White ink ribbon", '7', listener),
                                "ja", "白色インクリボン")
          ),
        "ja", "インクリボン"),

      //表示メニュー
      Multilingual.mlnText (
        ComponentFactory.createMenu (
          "Display", 'D',
          prnScaleMenuItem[0] = ComponentFactory.createRadioButtonMenuItem (zoomGroup, prnScaleShift == -4, "6.25%", '1', listener),
          prnScaleMenuItem[1] = ComponentFactory.createRadioButtonMenuItem (zoomGroup, prnScaleShift == -3, "12.5%", '2', listener),
          prnScaleMenuItem[2] = ComponentFactory.createRadioButtonMenuItem (zoomGroup, prnScaleShift == -2, "25%", '3', listener),
          prnScaleMenuItem[3] = ComponentFactory.createRadioButtonMenuItem (zoomGroup, prnScaleShift == -1, "50%", '4', listener),
          prnScaleMenuItem[4] = ComponentFactory.createRadioButtonMenuItem (zoomGroup, prnScaleShift ==  0, "100%", '5', listener),
          prnScaleMenuItem[5] = ComponentFactory.createRadioButtonMenuItem (zoomGroup, prnScaleShift ==  1, "200%", '6', listener),
          prnScaleMenuItem[6] = ComponentFactory.createRadioButtonMenuItem (zoomGroup, prnScaleShift ==  2, "400%", '7', listener),
          prnScaleMenuItem[7] = ComponentFactory.createRadioButtonMenuItem (zoomGroup, prnScaleShift ==  3, "800%", '8', listener),
          prnScaleMenuItem[8] = ComponentFactory.createRadioButtonMenuItem (zoomGroup, prnScaleShift ==  4, "1600%", '9', listener)
          ),
        "ja", "表示"),

      //DIPSWメニュー
      ComponentFactory.createMenu (
        "DIPSW", 'S',
        Multilingual.mlnText (
          ComponentFactory.createRadioButtonMenuItem (
            fontStyleGroup,
            (prnDIPSW & PRN_DIPSW_FONT_STYLE) == 0,
            "Roman / Mincho",
            listener
            ),
          "ja", "ローマン体／明朝体"
          ),
        Multilingual.mlnText (
          ComponentFactory.createRadioButtonMenuItem (
            fontStyleGroup,
            (prnDIPSW & PRN_DIPSW_FONT_STYLE) != 0,
            "Sans-Serif / Gothic",
            listener
            ),
          "ja", "サンセリフ体／ゴシック体"
          )
        )

      );

    //スケールシフトリスナー
    prnCanvas.addScaleShiftListener (new ScrollCanvas.ScaleShiftListener () {
      @Override public void scaleShiftChanged (int scaleShift) {
        if (prnScaleShift != scaleShift &&
            -4 <= scaleShift && scaleShift <= 4) {
          prnScaleShift = scaleShift;
          prnScaleMenuItem[4 + scaleShift].setSelected (true);
        }
      }
    });

    //ウインドウ
    prnFrame = Multilingual.mlnTitle (
      ComponentFactory.createRestorableSubFrame (
        Settings.SGS_PRN_FRAME_KEY,
        "Printer",
        menuBar,
        ComponentFactory.createBorderPanel (
          ComponentFactory.setPreferredSize (prnCanvas, 600, 400),
          null,
          null,
          null
          )
        ),
      "ja", "プリンタ");

  }  //prnMakeFrame

  //prnMakeSaveDialog ()
  //  プリンタ出力イメージ保存ダイアログを作る
  public static void prnMakeSaveDialog () {
    //出力できるイメージファイルの拡張子の配列を作る
    prnWriterSuffixes = ImageIO.getWriterFileSuffixes ();  //出力できるイメージファイルの拡張子の配列
    if (XEiJ.prgCaseIgnored) {  //ファイル名の大文字と小文字が区別されない
      for (int i = 0; i < prnWriterSuffixes.length; i++) {
        prnWriterSuffixes[i] = prnWriterSuffixes[i].toLowerCase ();  //小文字化しておく
      }
    }
    //プリンタ出力イメージファイルフィルタ
    prnSaveFileFilter = new javax.swing.filechooser.FileFilter () {  //java.io.FileFilterと紛らわしい
      @Override public boolean accept (File file) {
        if (file.isDirectory ()) {
          return true;
        }
        String name = file.getName ();
        if (XEiJ.prgCaseIgnored) {
          name = name.toLowerCase ();
        }
        for (String suffix : prnWriterSuffixes) {
          if (name.endsWith ("." + suffix)) {
            return true;
          }
        }
        return false;
      }
      @Override public String getDescription () {
        return Multilingual.mlnJapanese ? "プリンタ出力イメージ" : "Printer Output Image";
      }
    };
    //アクションリスナー
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        switch (ae.getActionCommand ()) {
        case JFileChooser.APPROVE_SELECTION:
        case "Save":  //保存
          {
            File file = prnSaveFileChooser.getSelectedFile ().getAbsoluteFile ();
            prnSavePath = file.getParentFile ().getAbsolutePath ();
            prnSaveName = file.getName ();
          }
          if (prnSave ()) {  //保存できたとき
            //  ダイアログを閉じた瞬間に排紙処理が再開されてイメージが廃棄される(可能性がある)ので、
            //  イメージの保存が終わってからダイアログを閉じる
            prnSaveDialog.setVisible (false);
          }
          break;
        case JFileChooser.CANCEL_SELECTION:
        case "Discard":  //破棄
          prnSaveDialog.setVisible (false);
          break;
        case "Autosave from next time":  //次回から自動保存
          prnSetAutosaveOn (prnAutosaveCheckBox.isSelected ());
          break;
        }
      }
    };
    //ファイルチューザー
    prnSaveFileChooser = new JFileChooser2 (new File (prnSavePath + File.separator + prnSaveName));
    prnSaveFileChooser.setFileFilter (prnSaveFileFilter);
    prnSaveFileChooser.setMultiSelectionEnabled (false);
    prnSaveFileChooser.setControlButtonsAreShown (false);
    prnSaveFileChooser.addActionListener (listener);
    //ダイアログ
    prnSaveDialog = Multilingual.mlnTitle (
      ComponentFactory.createModalDialog (
        XEiJ.frmFrame,
        "Save printer output image",
        ComponentFactory.createBorderPanel (
          0, 0,
          ComponentFactory.createVerticalBox (
            prnSaveFileChooser,
            ComponentFactory.createHorizontalBox (
              Box.createHorizontalStrut (12),
              Box.createHorizontalGlue (),
              prnAutosaveCheckBox =
              Multilingual.mlnText (ComponentFactory.createCheckBox (prnAutosaveOn, "Autosave from next time", listener), "ja", "次回から自動保存"),
              Box.createHorizontalGlue (),
              Box.createHorizontalStrut (12),
              Multilingual.mlnText (ComponentFactory.createButton ("Save", KeyEvent.VK_S, listener), "ja", "保存"),
              Box.createHorizontalStrut (12),
              Multilingual.mlnText (ComponentFactory.createButton ("Discard", KeyEvent.VK_D, listener), "ja", "破棄"),
              Box.createHorizontalStrut (12)
              ),
            Box.createVerticalStrut (12)
            )
          )
        ),
      "ja", "プリンタ出力イメージの保存");
  }  //prnMakeSaveDialog

  //prnSetAutosaveOn (on)
  //  自動保存を設定する
  public static void prnSetAutosaveOn (boolean on) {
    if (prnAutosaveOn != on) {
      prnAutosaveOn = on;
      if (prnAutosaveCheckBox != null &&
          prnAutosaveCheckBox.isSelected () != prnAutosaveOn) {
        prnAutosaveCheckBox.setSelected (prnAutosaveOn);
      }
      if (prnAutosaveMenuItem != null &&
          prnAutosaveMenuItem.isSelected () != prnAutosaveOn) {
        prnAutosaveMenuItem.setSelected (prnAutosaveOn);
      }
    }
  }  //prnSetAutosaveOn


  //プリンタポート

  //prnReset ()
  //  リセット
  //  プリンタポートをリセットする
  public static void prnReset () {
    prnData = 0;
    prnStrobe = 1;
  }  //prnReset

  //prnReadData ()
  public static int prnReadData () {
    return prnData;
  }  //prnReadData

  //prnReadStrobe ()
  public static int prnReadStrobe () {
    return prnStrobe;
  }  //prnReadStrobe

  //prnWriteData (d)
  public static void prnWriteData (int d) {
    prnData = d & 255;
  }  //prnWriteData

  //prnWriteStrobe (d)
  public static void prnWriteStrobe (int d) {
    d &= 1;
    if (prnStrobe != d) {
      prnStrobe = d;
      if (prnOnlineOn) {
        if (d != 0) {  //0→1
          //プリンタビジー
          IOInterrupt.ioiPrnFall ();
          //出力
          if (PRN_USE_ADAPTER) {
            prnAdapterOutput (prnData);
          } else {
            prnOutput (prnData);
          }
          //プリンタレディ
          IOInterrupt.ioiPrnRise ();
        }
      }
    }
  }  //prnWriteStrobe

  //prnSetOnlineOn (online)
  //  オンラインを設定する
  public static void prnSetOnlineOn (boolean on) {
    if (prnOnlineOn != on) {
      prnOnlineOn = on;
      if (prnOnlineMenuItem != null) {
        prnOnlineMenuItem.setSelected (on);
      }
      if (on) {  //off→on
        //プリンタレディ
        IOInterrupt.ioiPrnRise ();
      } else {  //on→off
        //プリンタビジー
        IOInterrupt.ioiPrnFall ();
      }
    }
  }  //prnSetOnlineOn



  //プリンタ

  //prnOutput (x)
  //  プリンタ出力
  //  接続されていること
  public static void prnOutput (int x) {
    prnCommandBuffer[prnCommandPointer++] = (byte) x;
    if (prnCommandPointer < prnCommandLength) {
      return;  //コマンド継続
    }
  command:
    {
      int c = prnCommandBuffer[0] & 255;  //1バイト目
      int d, e, f, g, h, i, j, z, n;
      switch (c) {
      case 0x08:  //BS
        prnPrintBackSpace ();  //バックスペース
        break command;  //コマンド終了
      case 0x09:  //HT
        prnPrintHorizontalTab ();  //水平タブ
        break command;  //コマンド終了
      case 0x0a:  //LF
        prnPrintLineFeed (1);  //1行改行
        break command;  //コマンド終了
      case 0x0b:  //VT
        if (prnCommandLength < 2) {
          prnCommandLength = 2;
          return;  //コマンド継続
        }
        d = prnCommandBuffer[1] & 255;  //2バイト目
        prnPrintVerticalTab (d & 15);  //垂直タブ
        break command;  //コマンド終了
      case 0x0c:  //FF
        prnPrintFormFeed ();  //改ページ
        break command;  //コマンド終了
      case 0x0d:  //CR
        prnPrintCarriageReturn ();  //復帰
        break command;  //コマンド終了
      case 0x0e:  //SO
        prnSetHorizontalDoubleSizeMode (true);  //横2倍ON
        break command;  //コマンド終了
      case 0x0f:  //SI
        prnSetHorizontalDoubleSizeMode (false);  //横2倍OFF
        break command;  //コマンド終了
      case 0x10:  //POS n1 n2 n3
        if (prnCommandLength < 4) {
          prnCommandLength = 4;
          return;  //コマンド継続
        }
        d = prnCommandBuffer[1] & 255;  //2バイト目
        e = prnCommandBuffer[2] & 255;  //3バイト目
        f = prnCommandBuffer[3] & 255;  //4バイト目
        prnSetStartColumn ((d & 15) * 100 + (e & 15) * 10 + (f & 15));  //開始桁位置設定
        break command;  //コマンド終了
      case 0x11:  //DC1
        prnSelect (true);  //セレクト
        break command;  //コマンド終了
      case 0x13:  //DC3
        prnSelect (false);  //ディセレクト
        break command;  //コマンド終了
      case 0x14:  //DC4 … ?
        z = prnCommandBuffer[prnCommandLength - 1] & 255;  //末尾
        if (z != 0x3f) {
          if (prnCommandLength + 1 < prnCommandBuffer.length) {
            prnCommandLength++;
          }
          return;  //コマンド継続
        }
        prnSetVerticalTabAnchor (prnCommandBuffer, 1, prnCommandLength - 1);  //垂直タブアンカー設置
        break command;  //コマンド終了
      case 0x18:  //CAN
        prnCancel ();  //キャンセル
        break command;  //コマンド終了
      case 0x1a:  //SUB
        if (prnCommandLength < 2) {
          prnCommandLength = 2;
          return;  //コマンド継続
        }
        d = prnCommandBuffer[1] & 255;  //2バイト目
        switch (d) {
        case 0x0c:  //SUB FF (拡張)
          prnEjectPaper ();  //排紙
          break command;  //コマンド終了
        case 0x1a:  //SUB SUB (拡張)
          prnErasePaper ();  //消去
          break command;  //コマンド終了
        case 0x56:  //SUB V
          prnSetVerticalDoubleSizeMode (true);  //縦2倍ON
          break command;  //コマンド終了
        case 0x57:  //SUB W
          prnSetVerticalDoubleSizeMode (false);  //縦2倍OFF
          break command;  //コマンド終了
        }
        break;  //コマンド不明
      case 0x1b:  //ESC
        if (prnCommandLength < 2) {
          prnCommandLength = 2;
          return;  //コマンド継続
        }
        d = prnCommandBuffer[1] & 255;  //2バイト目
        switch (d) {
        case 0x00:  //ESC n
        case 0x01:  //ESC n
        case 0x02:  //ESC n
        case 0x03:  //ESC n
        case 0x04:  //ESC n
        case 0x05:  //ESC n
        case 0x06:  //ESC n
          prnHorizontalMove (d);  //水平移動。バイナリ指定
          break command;  //コマンド終了
        case 0x0b:  //ESC VT n1 n2
          if (prnCommandLength < 4) {
            prnCommandLength = 4;
            return;  //コマンド継続
          }
          e = prnCommandBuffer[2] & 255;  //3バイト目
          f = prnCommandBuffer[3] & 255;  //4バイト目
          prnPrintLineFeed ((e & 15) * 10 + (f & 15));  //n行改行
          break command;  //コマンド終了
        case 0x10:  //ESC POS n1 n2 n3 n4
          if (prnCommandLength < 6) {
            prnCommandLength = 6;
            return;  //コマンド継続
          }
          e = prnCommandBuffer[2] & 255;  //3バイト目
          f = prnCommandBuffer[3] & 255;  //4バイト目
          g = prnCommandBuffer[4] & 255;  //5バイト目
          h = prnCommandBuffer[5] & 255;  //6バイト目
          prnSetHorizontalStartPosition ((e & 15) * 1000 + (f & 15) * 100 + (g & 15) * 10 + (h & 15));  //水平開始位置設定
          break command;  //コマンド終了
        case 0x19:  //ESC EM
          prnSetColorMode ();  //カラーモード
          break command;  //コマンド終了
        case 0x21:  //ESC !
          prnSetStrongMode (true);  //強調文字設定
          break command;  //コマンド終了
        case 0x22:  //ESC "
          prnSetStrongMode (false);  //強調文字解除
          break command;  //コマンド終了
        case 0x24:  //ESC $
          prnSetHiraganaMode (false);  //カタカナモード
          break command;  //コマンド終了
        case 0x25:  //ESC %
          if (prnCommandLength < 3) {
            prnCommandLength = 3;
            return;  //コマンド継続
          }
          e = prnCommandBuffer[2] & 255;  //3バイト目
          switch (e) {
          case 0x32:  //ESC % 2 n1 n2 d1 d2 … dk
            if (prnCommandLength < 5) {
              prnCommandLength = 5;
              return;  //コマンド継続
            }
            f = prnCommandBuffer[3] & 255;  //4バイト目
            g = prnCommandBuffer[4] & 255;  //5バイト目
            n = f << 8 | g;  //バイナリ指定
            if (prnCommandLength < 5 + n) {
              prnCommandLength = 5 + n;
              return;  //コマンド継続
            }
            prn8DotBitImage (prnCommandBuffer, 5, n);  //8ドットビットイメージ
            break command;  //コマンド終了
          case 0x39:  //ESC % 9 n
            if (prnCommandLength < 4) {
              prnCommandLength = 4;
              return;  //コマンド継続
            }
            f = prnCommandBuffer[3] & 255;  //4バイト目
            prnSetLineHeight (f);  //1/120[in]紙送り量設定
            break command;  //コマンド終了
          }
          break;  //コマンド不明
        case 0x26:  //ESC &
          prnSetHiraganaMode (true);  //ひらがなモード
          break command;  //コマンド終了
        case 0x28:  //ESC ( … .
          z = prnCommandBuffer[prnCommandLength - 1] & 255;  //末尾
          if (z != 0x2e) {
            if (prnCommandLength + 1 < prnCommandBuffer.length) {
              prnCommandLength++;
            }
            return;  //コマンド継続
          }
          prnSetHorizontalTabAnchor (prnCommandBuffer, 2, prnCommandLength - 2);  //水平タブアンカー設置
          break command;  //コマンド終了
        case 0x29:  //ESC ) … .
          z = prnCommandBuffer[prnCommandLength - 1] & 255;  //末尾
          if (z != 0x2e) {
            if (prnCommandLength + 1 < prnCommandBuffer.length) {
              prnCommandLength++;
            }
            return;  //コマンド継続
          }
          prnClearHorizontalTabAnchor (prnCommandBuffer, 2, prnCommandLength - 2);  //水平タブアンカー除去
          break command;  //コマンド終了
        case 0x2a:  //ESC * n1 n2 d1 d2 … d32
          if (prnCommandLength < 36) {
            prnCommandLength = 36;
            return;  //コマンド継続
          }
          prn16DotExtendedCharacterDefinition (prnCommandBuffer, 2, prnCommandLength - 2);  //16ドット外字定義
          break command;  //コマンド終了
        case 0x2b:  //ESC + n1 n2 d1 d2 … d72
          if (prnCommandLength < 76) {
            prnCommandLength = 76;
            return;  //コマンド継続
          }
          prn24DotExtendedCharacterDefinition (prnCommandBuffer, 2, prnCommandLength - 2);  //24ドット外字定義
          break command;  //コマンド終了
        case 0x2f:  //ESC / n1 n2 n3
          if (prnCommandLength < 5) {
            prnCommandLength = 5;
            return;  //コマンド継続
          }
          e = prnCommandBuffer[2] & 255;  //3バイト目
          f = prnCommandBuffer[3] & 255;  //4バイト目
          g = prnCommandBuffer[4] & 255;  //5バイト目
          prnSetRightMargin ((e & 15) * 100 + (f & 15) * 10 + (g & 15));  //右マージン設定
          break command;  //コマンド終了
        case 0x32:  //ESC 2
          prnClearAllHorizontalTabAnchor ();  //全水平タブアンカー除去
          break command;  //コマンド終了
        case 0x35:  //ESC 5
          prnSetPageStartPosition ();  //ページ先頭設定
          break command;  //コマンド終了
        case 0x36:  //ESC 6
          prnSetOneSixth ();  //1/6[in]改行設定
          break command;  //コマンド終了
        case 0x38:  //ESC 8
          prnSetOneEighth ();  //1/8[in]改行設定
          break command;  //コマンド終了
        case 0x43:  //ESC C n1 n2
          if (prnCommandLength < 4) {
            prnCommandLength = 4;
            return;  //コマンド継続
          }
          e = prnCommandBuffer[2] & 255;  //3バイト目
          f = prnCommandBuffer[3] & 255;  //4バイト目
          prnSetBottomMargin ((e & 15) * 10 + (f & 15));  //下マージン設定
          break command;  //コマンド終了
        case 0x45:  //ESC E
          prnSetEliteCharacterMode ();  //エリート文字設定
          break command;  //コマンド終了
        case 0x46:  //ESC F n1 n2
          if (prnCommandLength < 4) {
            prnCommandLength = 4;
            return;  //コマンド継続
          }
          e = prnCommandBuffer[2] & 255;  //3バイト目
          f = prnCommandBuffer[3] & 255;  //4バイト目
          prnSetPageHeight ((e & 15) * 10 + (f & 15));  //ページ高さ設定
          break command;  //コマンド終了
        case 0x48:  //ESC H
          prnSetKanjiMode (false);  //漢字モードOFF
          break command;  //コマンド終了
        case 0x49:  //ESC I n1 n2 n3 n4 d1 d2 … dk
          if (prnCommandLength < 6) {
            prnCommandLength = 6;
            return;  //コマンド継続
          }
          e = prnCommandBuffer[2] & 255;  //3バイト目
          f = prnCommandBuffer[3] & 255;  //4バイト目
          g = prnCommandBuffer[4] & 255;  //5バイト目
          h = prnCommandBuffer[5] & 255;  //6バイト目
          n = (e & 15) * 1000 + (f & 15) * 100 + (g & 15) * 10 + (h & 15);
          if (prnCommandLength < 6 + 2 * n) {
            prnCommandLength = 6 + 2 * n;
            return;  //コマンド継続
          }
          prn16DotBitImage (prnCommandBuffer, 6, n);  //16ドットビットイメージ
          break command;  //コマンド終了
        case 0x4a:  //ESC J n1 n2 d1 d2 … dk
          if (prnCommandLength < 4) {
            prnCommandLength = 4;
            return;  //コマンド継続
          }
          e = prnCommandBuffer[2] & 255;  //3バイト目
          f = prnCommandBuffer[3] & 255;  //4バイト目
          n = e << 8 | f;  //バイナリ指定
          if (prnCommandLength < 4 + 3 * n) {
            prnCommandLength = 4 + 3 * n;
            return;  //コマンド継続
          }
          prn24DotBitImage (prnCommandBuffer, 4, n);  //24ドットビットイメージ
          break command;  //コマンド終了
        case 0x4b:  //ESC K
          prnSetKanjiMode (true);  //漢字モードON
          break command;  //コマンド終了
        case 0x4c:  //ESC L n1 n2 n3
          if (prnCommandLength < 5) {
            prnCommandLength = 5;
            return;  //コマンド継続
          }
          e = prnCommandBuffer[2] & 255;  //3バイト目
          f = prnCommandBuffer[3] & 255;  //4バイト目
          g = prnCommandBuffer[4] & 255;  //5バイト目
          prnSetLeftMargin ((e & 15) * 100 + (f & 15) * 10 + (g & 15));  //左マージン設定
          break command;  //コマンド終了
        case 0x4d:  //ESC M n1 n2 d1 d2 … dk
          if (prnCommandLength < 4) {
            prnCommandLength = 4;
            return;  //コマンド継続
          }
          e = prnCommandBuffer[2] & 255;  //3バイト目
          f = prnCommandBuffer[3] & 255;  //4バイト目
          n = e << 8 | f;  //バイナリ指定
          if (prnCommandLength < 4 + 6 * n) {
            prnCommandLength = 4 + 6 * n;
            return;  //コマンド継続
          }
          prn48DotBitImage (prnCommandBuffer, 4, n);  //48ドットビットイメージ
          break command;  //コマンド終了
        case 0x4e:  //ESC N n1 n2 n3 d
          if (prnCommandLength < 6) {
            prnCommandLength = 6;
            return;  //コマンド継続
          }
          e = prnCommandBuffer[2] & 255;  //3バイト目
          f = prnCommandBuffer[3] & 255;  //4バイト目
          g = prnCommandBuffer[4] & 255;  //5バイト目
          h = prnCommandBuffer[5] & 255;  //6バイト目
          prnRepeatCharacter ((e & 15) * 100 + (f & 15) * 10 + (g & 15), h);  //連続文字
          break command;  //コマンド終了
        case 0x50:  //ESC P
          prnSetKanjiMode (false);  //漢字モードOFF
          break command;  //コマンド終了
        case 0x51:  //ESC Q
          prnSetSmallCharacterMode ();  //縮小文字設定
          break command;  //コマンド終了
        case 0x52:  //ESC R
          prnSetPicaCharacterMode ();  //パイカ文字設定
          break command;  //コマンド終了
        case 0x55:  //ESC U
          prnSetHorizontalDoubleSizeMode (true);  //横2倍ON
          break command;  //コマンド終了
        case 0x56:  //ESC V n1 n2 n3 n4 d
          if (prnCommandLength < 7) {
            prnCommandLength = 7;
            return;  //コマンド継続
          }
          e = prnCommandBuffer[2] & 255;  //3バイト目
          f = prnCommandBuffer[3] & 255;  //4バイト目
          g = prnCommandBuffer[4] & 255;  //5バイト目
          h = prnCommandBuffer[5] & 255;  //6バイト目
          i = prnCommandBuffer[6] & 255;  //7バイト目
          prnRepeat8DotBitImage ((e & 15) * 1000 + (f & 15) * 100 + (g & 15) * 10 + (h & 15), i);  //連続8ドットビットメージ
          break command;  //コマンド終了
        case 0x57:  //ESC W n1 n2 n3 n4 d1 d2
          if (prnCommandLength < 8) {
            prnCommandLength = 8;
            return;  //コマンド継続
          }
          e = prnCommandBuffer[2] & 255;  //3バイト目
          f = prnCommandBuffer[3] & 255;  //4バイト目
          g = prnCommandBuffer[4] & 255;  //5バイト目
          h = prnCommandBuffer[5] & 255;  //6バイト目
          i = prnCommandBuffer[6] & 255;  //7バイト目
          j = prnCommandBuffer[7] & 255;  //8バイト目
          prnRepeat16DotBitImage ((e & 15) * 1000 + (f & 15) * 100 + (g & 15) * 10 + (h & 15), i << 8 | j);  //連続16ドットビットメージ
          break command;  //コマンド終了
        case 0x58:  //ESC X
          prnSetUnderlineMode (true);  //アンダーラインあり
          break command;  //コマンド終了
        case 0x59:  //ESC Y
          prnSetUnderlineMode (false);  //アンダーラインなし
          break command;  //コマンド終了
        case 0x5c:  //ESC \\ n1 n2
          if (prnCommandLength < 4) {
            prnCommandLength = 4;
            return;  //コマンド継続
          }
          e = prnCommandBuffer[2] & 255;  //3バイト目
          f = prnCommandBuffer[3] & 255;  //4バイト目
          prnHorizontalMove ((short) (f << 8 | e));  //水平移動。バイナリ指定。下位、上位。符号あり
          break command;  //コマンド終了
        case 0x63:  //ESC c
          if (prnCommandLength < 3) {
            prnCommandLength = 3;
            return;  //コマンド継続
          }
          e = prnCommandBuffer[2] & 255;  //3バイト目
          switch (e) {
          case 0x31:  //ESC c 1
            prnResetSettings ();  //設定リセット
            break command;  //コマンド終了
          }
          break;  //コマンド不明
        case 0x6b:  //ESC k
          if (prnCommandLength < 3) {
            prnCommandLength = 3;
            return;  //コマンド継続
          }
          e = prnCommandBuffer[2] & 255;  //3バイト目
          prnSetHalfWidthFont (e);  //半角書体。バイナリ指定
          break command;  //コマンド終了
        case 0x70:  //ESC p
          if (prnCommandLength < 3) {
            prnCommandLength = 3;
            return;  //コマンド継続
          }
          e = prnCommandBuffer[2] & 255;  //3バイト目
          switch (e) {
          case 0x30:  //ESC p 0
            prnSenseOutOfPaper (false);  //用紙切れ検出無効
            break command;  //コマンド終了
          case 0x31:  //ESC p 1
            prnSenseOutOfPaper (true);  //用紙切れ検出有効
            break command;  //コマンド終了
          }
          break;  //コマンド不明
        case 0x71:  //ESC q n
          if (prnCommandLength < 3) {
            prnCommandLength = 3;
            return;  //コマンド継続
          }
          e = prnCommandBuffer[2] & 255;  //3バイト目
          prnSetCharacterStyle (e);  //文字スタイル設定。バイナリ指定
          break command;  //コマンド終了
        case 0x73:  //ESC s
          if (prnCommandLength < 3) {
            prnCommandLength = 3;
            return;  //コマンド継続
          }
          e = prnCommandBuffer[2] & 255;  //3バイト目
          switch (e) {
          case 0x30:  //ESC s 0
            prnSetScriptMode (0);  //スクリプト解除
            break command;  //コマンド終了
          case 0x31:  //ESC s 1
            prnSetScriptMode (1);  //スーパースクリプト設定
            break command;  //コマンド終了
          case 0x32:  //ESC s 2
            prnSetScriptMode (2);  //サブスクリプト設定
            break command;  //コマンド終了
          }
          break;  //コマンド不明
        }
        break;  //コマンド不明
      case 0x1c:  //FS
        if (prnCommandLength < 2) {
          prnCommandLength = 2;
          return;  //コマンド継続
        }
        d = prnCommandBuffer[1] & 255;  //2バイト目
        switch (d) {
        case 0x4a:  //FS J
          prnSetVerticalWritingMode (true);  //縦書き
          break command;  //コマンド終了
        case 0x4b:  //FS K
          prnSetVerticalWritingMode (false);  //横書き
          break command;  //コマンド終了
        case 0x53:  //FS S n1 n2
          if (prnCommandLength < 4) {
            prnCommandLength = 4;
            return;  //コマンド継続
          }
          e = prnCommandBuffer[2] & 255;  //3バイト目
          f = prnCommandBuffer[3] & 255;  //4バイト目
          prnSetFullWidthLeftRightSpace (e, f);  //全角左右スペース。バイナリ指定
          break command;  //コマンド終了
        case 0x54:  //FS T n1 n2
          if (prnCommandLength < 4) {
            prnCommandLength = 4;
            return;  //コマンド継続
          }
          e = prnCommandBuffer[2] & 255;  //3バイト目
          f = prnCommandBuffer[3] & 255;  //4バイト目
          prnSetHalfWidthLeftRightSpace (e, f);  //半角左右スペース。バイナリ指定
          break command;  //コマンド終了
        case 0x6b:  //FS k
          if (prnCommandLength < 3) {
            prnCommandLength = 3;
            return;  //コマンド継続
          }
          e = prnCommandBuffer[2] & 255;  //3バイト目
          prnSetFullWidthFont (e);  //全角書体。バイナリ指定
          break command;  //コマンド終了
        case 0x70:  //FS p
          prnSetKanjiHorizontalDoubleSizeMode (true);  //漢字横2倍ON
          break command;  //コマンド終了
        case 0x71:  //FS q
          prnSetKanjiHorizontalDoubleSizeMode (false);  //漢字横2倍OFF
          break command;  //コマンド終了
        }  //switch d
        break;  //コマンド不明
      default:
        if (prnKanjiMode) {  //漢字モードONのとき
          if (prnCommandLength < 2) {
            prnCommandLength = 2;
            return;  //コマンド継続
          }
          d = prnCommandBuffer[1] & 255;  //2バイト目
          c = c << 8 | d;
        }
        if (c != 0) {  //0のときは何もしない
          prnPrintCharacter (c);  //1文字印字
        }
        break command;  //コマンド終了
      }  //switch c
      //コマンド不明
      for (int k = 0; k < prnCommandLength; k++) {
        prnPrintCharacter (prnCommandBuffer[k] & 255);  //1文字印字
      }
    }  //command
    //コマンド終了
    prnCommandLength = 1;
    prnCommandPointer = 0;
  }  //prnOutput

  //prnFeedPaper ()
  //  給紙
  public static void prnFeedPaper () {
    if (prnCurrentPaper != null) {  //既に給紙されているとき
      return;  //何もしない
    }
    //用紙
    prnCurrentPaper = prnNextPaper;
    //印字不可領域
    prnDeadTopMm = Math.min (prnNextDeadTopMm, prnCurrentPaper.paperHeightMm - 1);
    prnDeadLeftMm = Math.min (prnNextDeadLeftMm, prnCurrentPaper.paperWidthMm - 1);
    prnDeadRightMm = Math.min (prnNextDeadRightMm, prnCurrentPaper.paperWidthMm - 1 - prnDeadLeftMm);
    prnDeadBottomMm = Math.min (prnNextDeadBottomMm, prnCurrentPaper.paperHeightMm - 1 - prnDeadTopMm);
    //印字可能範囲
    prnAliveTopY = (int) Math.floor ((double) prnDeadTopMm * (360.0 / 25.4) + 0.5);
    prnAliveLeftX = (int) Math.floor ((double) prnDeadLeftMm * (360.0 / 25.4) + 0.5);
    prnAliveRightX = (int) Math.floor ((double) (prnCurrentPaper.paperWidthMm - prnDeadRightMm) * (360.0 / 25.4) + 0.5);
    prnAliveBottomY = (int) Math.floor ((double) (prnCurrentPaper.paperHeightMm - prnDeadBottomMm) * (360.0 / 25.4) + 0.5);
    //回転
    prnRotation = prnNextRotation;
    if (prnRotation == 0) {  //0°
      prnRotatedWidthDot = prnCurrentPaper.paperWidthDot;
      prnRotatedHeightDot = prnCurrentPaper.paperHeightDot;
      //  [1 0 0]
      //  [0 1 0]
      //  [0 0 1]
      prnM11 = 1;
      prnM12 = 0;
      prnM13 = 0;
      prnM21 = 0;
      prnM22 = 1;
      prnM23 = 0;
    } else if (prnRotation == 1) {  //90°
      prnRotatedWidthDot = prnCurrentPaper.paperHeightDot;
      prnRotatedHeightDot = prnCurrentPaper.paperWidthDot;
      //  [0 -1 w]
      //  [1  0 0]
      //  [0  0 1]
      prnM11 = 0;
      prnM12 = -1;
      prnM13 = prnRotatedWidthDot;
      prnM21 = 1;
      prnM22 = 0;
      prnM23 = 0;
    } else if (prnRotation == 2) {  //180°
      prnRotatedWidthDot = prnCurrentPaper.paperWidthDot;
      prnRotatedHeightDot = prnCurrentPaper.paperHeightDot;
      //  [-1  0 w]
      //  [ 0 -1 h]
      //  [ 0  0 1]
      prnM11 = -1;
      prnM12 = 0;
      prnM13 = prnRotatedWidthDot;
      prnM21 = 0;
      prnM22 = -1;
      prnM23 = prnRotatedHeightDot;
    } else {  //270°
      prnRotatedWidthDot = prnCurrentPaper.paperHeightDot;
      prnRotatedHeightDot = prnCurrentPaper.paperWidthDot;
      //  [ 0 1 0]
      //  [-1 0 h]
      //  [ 0 0 1]
      prnM11 = 0;
      prnM12 = 1;
      prnM13 = 0;
      prnM21 = -1;
      prnM22 = 0;
      prnM23 = prnRotatedHeightDot;
    }
    prnIncrementX = prnM11 + prnRotatedWidthDot * prnM21;
    prnIncrementY = prnM12 + prnRotatedWidthDot * prnM22;
    //ダークモード
    prnDarkMode = prnNextDarkMode;
    //イメージとビットマップ
    prnImage = new BufferedImage (prnRotatedWidthDot,
                                  prnRotatedHeightDot,
                                  BufferedImage.TYPE_BYTE_INDEXED, prnDarkMode ? prnDarkImageColorModel : prnImageColorModel);
    prnBitmap = ((DataBufferByte) prnImage.getRaster ().getDataBuffer ()).getData ();
    if (prnCanvas != null) {
      prnCanvas.setImage (prnImage);
    }
    //消去
    prnErasePaper ();
  }  //prnFeedPaper

  //prnErasePaper ()
  //  消去
  //  印刷済みの用紙を保存せずに破棄する
  public static void prnErasePaper () {
    if (prnCurrentPaper == null) {  //給紙されていないとき
      prnFeedPaper ();  //給紙する
    } else {  //給紙されているとき
      //ページ範囲
      prnPageStart = 0;
      prnPageLength = ((prnAliveBottomY - prnAliveTopY + (360 - 1)) / 360) * 360;
      //コンテント範囲
      prnContentTopY = prnPageStart;
      prnContentBottomY = prnPageStart + prnPageLength - prnMarginBottomHeight;
      //ヘッドの位置
      prnHeadX = prnMarginLeftX;
      prnHeadY = prnContentTopY;
      prnHeadLine = 0;
      //白で塗り潰す
      Arrays.fill (prnBitmap, 0, prnRotatedWidthDot * prnRotatedHeightDot, (byte) 7);
      //印字なし
      prnPrinted = false;
      //キャンバスを再描画する
      if (prnCanvas != null) {
        prnCanvas.repaint ();
      }
    }
  }  //prnErasePaper

  //prnEjectPaper ()
  //  排紙
  public static void prnEjectPaper () {
    if (prnCurrentPaper == null) {  //既に排紙されているとき
      return;  //何もしない
    }
    if (prnPrinted) {  //印字されているとき
      prnPrinted = false;
      prnSavePaper ();  //保存する
    }
    prnCurrentPaper = null;  //未給紙にする
    prnHeadX = -1;  //ヘッドX座標[dot]
    prnHeadY = -1;  //ヘッドY座標[dot]
    prnHeadLine = -1;  //ヘッド行番号
    if (prnCanvas != null) {
      prnCanvas.setImage (null);
    }
  }  //prnEjectPaper

  //prnSavePaper ()
  //  保存する
  //  ファイル名はprnSavePath+File.separator+prnSaveName
  //  ファイルが既にあるとき
  //    主ファイル名の末尾に数字があるとき
  //      主ファイル名の末尾の数字をインクリメントする
  //    主ファイル名の末尾に数字がないとき
  //      主ファイル名の末尾に"2"を追加する
  public static void prnSavePaper () {
    //存在しないファイル名に書き換える
    while (new File (prnSavePath + File.separator + prnSaveName).isFile ()) {  //ファイルが既にある
      int j = prnSaveName.lastIndexOf ('.');  //主ファイル名の末尾
      if (j < 0) {
        j = prnSaveName.length ();
      }
      int i = j;
      int n = 2;
      if (0 < j && Character.isDigit (prnSaveName.charAt (j - 1))) {  //主ファイル名の末尾に数字がある
        //主ファイル名の末尾の数字を最大8桁取り出してインクリメントする
        i--;
        while (j - 8 < i &&
               0 < i && Character.isDigit (prnSaveName.charAt (i - 1))) {
          i--;
        }
        n = Integer.parseInt (prnSaveName.substring (i, j));
        n = (n + 1) % 100000000;  //1億個すべて使い切ると無限ループに陥るがそんなことはないだろう
      }
      prnSaveName = prnSaveName.substring (0, i) + n + prnSaveName.substring (j);
    }
    //親ディレクトリを掘る
    File file = new File (prnSavePath + File.separator + prnSaveName).getAbsoluteFile ();
    File parent = file.getParentFile ();
    prnSavePath = parent.getAbsolutePath ();  //区切り直す
    prnSaveName = file.getName ();
    parent.mkdirs ();
    //保存する
    if (prnAutosaveOn) {  //自動保存のとき
      if (!prnSave ()) {  //保存できなかったとき
        prnSetAutosaveOn (false);  //手動保存に切り替える
      }
    }
    if (!prnAutosaveOn) {  //手動保存のとき
      if (prnSaveDialog == null) {
        prnMakeSaveDialog ();  //ダイアログを作る
      }
      prnSaveFileChooser.setCurrentDirectory (parent);  //親ディレクトリを設定する
      prnSaveFileChooser.rescanCurrentDirectory ();  //親ディレクトリのファイルのリストを更新する。これをやらないと前回保存したファイルが表示されない
      prnSaveFileChooser.setSelectedFile (file);  //主ファイル名を設定する
      //ダイアログを表示する
      //  モーダルダイアログなのでダイアログを閉じるまでここでブロックされる
      XEiJ.pnlExitFullScreen (false);
      prnSaveDialog.setVisible (true);
    }
  }  //prnSavePaper

  private static final byte[] PRN_DOUBLE_4BIT = {
    0b00_00_00_00,
    0b00_00_00_11,
    0b00_00_11_00,
    0b00_00_11_11,
    0b00_11_00_00,
    0b00_11_00_11,
    0b00_11_11_00,
    0b00_11_11_11,
    (byte) 0b11_00_00_00,
    (byte) 0b11_00_00_11,
    (byte) 0b11_00_11_00,
    (byte) 0b11_00_11_11,
    (byte) 0b11_11_00_00,
    (byte) 0b11_11_00_11,
    (byte) 0b11_11_11_00,
    (byte) 0b11_11_11_11,
  };

  //prnSetSingleColor (color)
  //  単色インクリボンの色を設定する
  public static void prnSetSingleColor (int color) {
    prnSingleColor = color;
    if (!prnColorMode) {  //単色モードのとき
      prnCurrentColor = color;
    }
  }  //prnSetSingleColor

  //prnGetCharacterWidth ()
  //  現在の文字の幅[dot]を返す
  public static int prnGetCharacterWidth () {
    return (prnKanjiMode ?
            (prnHalfWidthLeftSpace + 24 + prnHalfWidthRightSpace) *
            (prnHorizontalDoubleSizeMode || prnKanjiHorizontalDoubleSizeMode ? 2 : 1) :
            (prnCharacterType == PRN_PICA ? 36 :
             prnCharacterType == PRN_ELITE ? 30 :
             prnCharacterType == PRN_SMALL ? 21 : 0) *
            (prnHorizontalDoubleSizeMode ? 2 : 1));
  }  //prnGetCharacterWidth

  //prnPrintCharacter (c)
  //  1文字印字
  //  コントロールコードは処理しない
  public static void prnPrintCharacter (int c) {
    c = (char) c;
    if (prnCurrentPaper == null) {  //未給紙のとき
      prnFeedPaper ();  //給紙する
    }
    //フォントを選ぶ
    FontPage page = null;
    int y0 = 0;  //開始ラスタ。サブスクリプトのとき14、それ以外は0
    int col = 0;  //文字コードの桁番号
    int row = 0;  //文字コードの行番号
    int w;  //フォントの幅
    int h;  //フォントの高さ
    int o;  //フォントの1ラスタのバイト数
    int oh;  //フォントのバイト数
    byte[] b;  //フォントのビットマップ
  gaiji:
    {
      if (c <= 0x00ff) {  //ANK
        col = c & 15;
        row = c >> 4;
        if (prnKanjiMode) {  //漢字モードの半角。ひらがなは無効
          page = prnHalfWidthFont == 0 ? fntPagePan24x48R : fntPagePan24x48S;
        } else {
          if (prnHiraganaMode) {  //ひらがな
            row += 16;
          }
          if (prnCharacterType == PRN_PICA) {  //パイカ
            page = prnHalfWidthFont == 0 ? fntPagePic36x46R : fntPagePic36x46S;
          } else if (prnCharacterType == PRN_ELITE) {  //エリート
            page = prnHalfWidthFont == 0 ? fntPageEli30x46R : fntPageEli30x46S;
          } else if (prnCharacterType == PRN_SMALL) {  //縮小
            page = prnHalfWidthFont == 0 ? fntPageSma18x46R : fntPageSma18x46S;
          } else {  //スクリプト
            page = prnHalfWidthFont == 0 ? fntPageScr28x32R : fntPageScr28x32S;
            if (prnScriptMode == 2) {  //サブスクリプト
              y0 = 14;
            }
          }
        }
      } else {  //漢字
        page = prnFullWidthFont == 0 ? fntPageZen48x48M : fntPageZen48x48G;
        row = c >> 8;
        col = c & 255;
        if (((0x81 <= row && row <= 0x9f) ||
             (0xe0 <= row && row <= 0xef)) &&
            ((0x40 <= col && col <= 0x7e) ||
             (0x80 <= col && col <= 0xfc))) {  //SJIS
          //  SJIS                           JIS
          //  8140-817E 8180-819E 819F-81FC  2121-215F 2160-217E 2221-227E
          //  9F40-9F7E 9F80-9F9E 9F9F-9FFC  5D21-5D5F 5D60-5D7E 5E21-5E7E
          //  E040-E07E E080-E09E E09F-E0FC  5F21-5F5F 5F60-5F7E 6021-607E
          //  EF40-EF7E EF80-EF9E EF9F-EFFC  7D21-7D5F 7D60-7D7E 7E21-7E7E
          if (0xe0 <= row) {
            row -= 0xe0 - 0xa0;
          }
          row -= 0x81;
          if (0x80 <= col) {
            col -= 0x80 - 0x7f;
          }
          col -= 0x40;
          row *= 2;
          if (94 <= col) {
            row += 1;
            col -= 94;
          }
          row += 0x21;
          col += 0x21;
        }
        if ((row == 0x76 && (0x21 <= col && col <= 0x7e)) ||
            (row == 0x77 && (0x21 <= col && col <= 0x26))) {  //外字定義エリア
          w = 48;  //フォントの幅
          h = 48;  //フォントの高さ
          o = 6;  //フォントの1ラスタのバイト数
          oh = o * h;  //フォントのバイト数
          b = new byte[oh];
          System.arraycopy (prnGetGaijiData (), oh * ((col - 0x21) + 94 * (row - 0x76)), b, 0, oh);
          break gaiji;
        }
        if (0x21 <= col && col <= 0x7e) {
          col -= 0x21;
          if (0x21 <= row && row <= 0x28) {
            row -= 0x21;
          } else if (0x30 <= row && row <= 0x74) {
            //row -= 0x30 - 8;  //77区のとき
            row -= 0x21;  //94区のとき
          } else {
            row = 0;
            col = 0;
          }
        } else {
          row = 0;
          col = 0;
        }
      }
      if (!page.fnpReady) {  //フォントの準備ができていない
        page.fnpCreateImage ((c <= 0x00ff ? prnHalfWidthFont : prnFullWidthFont) == 0 ? fntGetMinchoFamily () : fntGetGothicFamily ());  //フォントを作る
      }
      w = page.fnpCharacterWidth;  //フォントの幅
      h = page.fnpCharacterHeight;  //フォントの高さ
      o = page.fnpCharacterHorizontalBytes;  //フォントの1ラスタのバイト数
      oh = o * h;  //フォントのバイト数
      b = new byte[o * y0 + oh];
      System.arraycopy (page.fnpBinaryArray, oh * (col + page.fnpImageCols * row), b, o * y0, oh);
    }  //gaiji
    h += y0;
    oh = o * h;
    //縦書き
    //  漢字モードで全角のとき48x48のパターンを左に90度回転させる
    //  横2倍は回転してから横に拡大するので文字は縦長になる
    //  回転させない文字
    //    2126-2128  ・：；
    //    215d       －
    //    2162-2166  ≠＜＞≦≧
    //    222a-222d  →←↑↓
    //    2821-2840  ─│┌┐┘└├┬┤┴┼━┃┏┓┛┗┣┳┫┻╋┠┯┨┷┿┝┰┥┸╂
    //  縦書き用のパターンが存在する文字
    //    2122-2123  、  句読点。左下1/3を右上へ移動してから回転
    //    2123       。  〃
    //    2131-2132  ￣＿
    //    213c       ー  長音と波ダッシュ。上下反転
    //    213d-213e  ―‐
    //    2141       ～  〃
    //    2142-2145  ∥｜…‥
    //    214a-215b  （）〔〕［］｛｝〈〉《》「」『』【】
    //    2161       ＝
    //    2421       ぁ  小さいひらがな・カタカナ。左下7/8を右上へ移動してから回転
    //    2423       ぃ  〃
    //    2425       ぅ  〃
    //    2427       ぇ  〃
    //    2429       ぉ  〃
    //    2443       っ  〃
    //    2463       ゃ  〃
    //    2465       ゅ  〃
    //    2467       ょ  〃
    //    246e       ゎ  〃
    //    2521       ァ  〃
    //    2523       ィ  〃
    //    2525       ゥ  〃
    //    2527       ェ  〃
    //    2529       ォ  〃
    //    2543       ッ  〃
    //    2563       ャ  〃
    //    2565       ュ  〃
    //    2567       ョ  〃
    //    256e       ヮ  〃
    //    2575       ヵ  〃
    //    2576       ヶ  〃
    //    括弧の多くは回転させないだけでよさそうに思われるが縦書き用のパターンが存在する
    //  参考
    //    http://www.unicode.org/reports/tr50/
    //    http://www.unicode.org/charts/PDF/UFE10.pdf
    if (prnVerticalWritingMode &&  //縦書き
        page != null && page.fnpImageCols == 94 &&  //全角 漢字
        !((row == 0x21 - 0x21 && ((0x26 - 0x21 <= col && col <= 0x28 - 0x21) ||
                                  (col == 0x5d - 0x21) ||
                                  (0x62 - 0x21 <= col && col <= 0x66 - 0x21))) ||
          (row == 0x22 - 0x21 && (0x2a - 0x21 <= col && col <= 0x2d - 0x21)) ||
          (row == 0x28 - 0x21 && (0x21 - 0x21 <= col && col <= 0x40 - 0x21)))) {  //回転させる
      boolean rotate;
      if (row == 0x21 - 0x21 && (0x22 - 0x21 <= col && col <= 0x23 - 0x21)) {
        //句読点。左下1/3を右上へ移動してから回転
        byte[] bb = new byte[6 * 48];
        //Arrays.fill (bb, (byte) 0);
        //!!! 冗長
        for (int yy = 0; yy < 48 / 3; yy++) {
          int y = yy + 48 * 2 / 3;
          for (int xx = 48 * 2 / 3; xx < 48; xx++) {
            int x = xx - 48 * 2 / 3;
            bb[6 * yy + (xx >> 3)] |= (byte) ((b[6 * y + (x >> 3)] >> (~x & 7) & 1) << (~xx & 7));
          }
        }
        b = bb;
        rotate = true;
      } else if (row == 0x21 - 0x21 && (col == 0x3c - 0x21 ||
                                        col == 0x41 - 0x21)) {
        //長音と波ダッシュ。上下反転
        byte[] bb = new byte[6 * 48];
        //Arrays.fill (bb, (byte) 0);
        //!!! 冗長
        for (int yy = 0; yy < 48; yy++) {
          int y = 48 - 1 - yy;
          for (int xx = 0; xx < 48; xx++) {
            int x = xx;
            bb[6 * yy + (xx >> 3)] |= (byte) ((b[6 * y + (x >> 3)] >> (~x & 7) & 1) << (~xx & 7));
          }
        }
        b = bb;
        rotate = false;
      } else if ((row == 0x24 - 0x21 && (col == 0x21 - 0x21 ||
                                         col == 0x23 - 0x21 ||
                                         col == 0x25 - 0x21 ||
                                         col == 0x27 - 0x21 ||
                                         col == 0x29 - 0x21 ||
                                         col == 0x43 - 0x21 ||
                                         col == 0x63 - 0x21 ||
                                         col == 0x65 - 0x21 ||
                                         col == 0x67 - 0x21 ||
                                         col == 0x6e - 0x21)) ||
                 (row == 0x25 - 0x21 && (col == 0x21 - 0x21 ||
                                         col == 0x23 - 0x21 ||
                                         col == 0x25 - 0x21 ||
                                         col == 0x27 - 0x21 ||
                                         col == 0x29 - 0x21 ||
                                         col == 0x43 - 0x21 ||
                                         col == 0x63 - 0x21 ||
                                         col == 0x65 - 0x21 ||
                                         col == 0x67 - 0x21 ||
                                         col == 0x6e - 0x21 ||
                                         col == 0x75 - 0x21 ||
                                         col == 0x76 - 0x21))) {
        //小さいひらがな・カタカナ。左下7/8を右上へ移動してから回転
        byte[] bb = new byte[6 * 48];
        //Arrays.fill (bb, (byte) 0);
        //!!! 冗長
        for (int yy = 0; yy < 48 * 7 / 8; yy++) {
          int y = yy + 48 / 8;
          for (int xx = 48 / 8; xx < 48; xx++) {
            int x = xx - 48 / 8;
            bb[6 * yy + (xx >> 3)] |= (byte) ((b[6 * y + (x >> 3)] >> (~x & 7) & 1) << (~xx & 7));
          }
        }
        b = bb;
        rotate = true;
      } else if (row == 0x21 - 0x21 && ((0x31 - 0x21 <= col && col <= 0x32 - 0x21) ||
                                        (0x3d - 0x21 <= col && col <= 0x3e - 0x21) ||
                                        (0x42 - 0x21 <= col && col <= 0x45 - 0x21) ||
                                        (0x4a - 0x21 <= col && col <= 0x5b - 0x21) ||
                                        col == 0x61 - 0x21)) {
        rotate = false;
      } else {
        rotate = true;
      }
      if (rotate) {
        //左に90度回転させる
        //  デスティネーションを左上から右へ走査する
        //  ソースを右上から下へ走査する
        byte[] bb = new byte[6 * 48];
        //Arrays.fill (bb, (byte) 0);
        //!!! 冗長
        for (int yy = 0; yy < 48; yy++) {
          int x = 48 - 1 - yy;
          for (int xx = 0; xx < 48; xx++) {
            int y = xx;
            bb[6 * yy + (xx >> 3)] |= (byte) ((b[6 * y + (x >> 3)] >> (~x & 7) & 1) << (~xx & 7));
          }
        }
        b = bb;
      }
    }  //if 縦書き
    if (false) {
      //高さを最低48[dot]にする
      //  パイカは46[dot]なので48[dot]にしておくと袋文字、影文字、袋影文字の下端が切れなくなる
      //  マニュアルのサンプルを見ると下端が切れているのでCZ-8PC4は46[dot]のままなのだと思われる
      if (h < 48) {
        int hh = 48;
        int ohh = o * hh;
        byte[] bb = new byte[ohh];
        System.arraycopy (b, 0, bb, 0, oh);
        //Arrays.fill (bb, oh, ohh - oh, (byte) 0);
        h = hh;
        oh = ohh;
        b = bb;
      }
    }
    //左右スペース
    {
      int lw = 0;
      int rw = 0;
      if (prnKanjiMode) {  //漢字モード
        if (w == 48) {  //全角
          lw = prnFullWidthLeftSpace;
          rw = prnFullWidthRightSpace;
        } else {  //半角
          lw = prnHalfWidthLeftSpace;
          rw = prnHalfWidthRightSpace;
        }
      } else if (prnCharacterType == PRN_SMALL) {  //縮小文字
        rw = 3;  //18[dot]のフォントを印字して21[dot]進む
      }
      if (lw != 0 || rw != 0) {
        int ww = lw + w + rw;
        int oo = (ww + 7) >> 3;
        int ooh = oo * h;
        byte[] bb = new byte[ooh];
        Arrays.fill (bb, (byte) 0);
        //右にlwビットずらしながらコピーする
        int lq = lw >> 3;
        int lr = lw & 7;
        if (lr == 0) {
          for (int a = 0, aa = lq; a < oh; a += o, aa += oo) {
            for (int i = o - 1; 0 <= i; i--) {
              bb[aa + i] = b[a + i];
            }
          }
        } else {
          for (int a = 0, aa = lq; a < oh; a += o, aa += oo) {
            bb[aa + o] = (byte) (b[a + o - 1] << (8 - lr));
            for (int i = o - 1; 0 < i; i--) {
              bb[aa + i] = (byte) ((b[a + i - 1] << (8 - lr)) | (b[a + i] & 255) >> lr);
            }
            bb[aa] = (byte) ((b[a] & 255) >> lr);
          }
        }
        w = ww;
        o = oo;
        oh = ooh;
        b = bb;
      }
    }
    //強調
    if (prnStrongMode) {
      //右にずらして重ねる
      //!!! 縦書きのとき下にずらして重ねることになる
      for (int a = 0; a < oh; a += o) {
        for (int i = o - 1; 0 < i; i--) {
          b[a + i] |= (byte) (b[a + i - 1] << 7 | (b[a + i] & 255) >> 1);
        }
        b[a] |= (byte) ((b[a] & 255) >> 1);
      }
    }  //if 強調
    //文字スタイル
    if (prnCharacterStyle != 0) {  //袋文字または影文字または袋影文字
      //  横書き
      //    1  袋文字    左上 上 右上 左 右 左下 下 右下           ずらしたパターンを重ねてから元のパターンをくり抜く
      //    2  影文字                               右下 右右下下  ずらしたパターンを重ねてから元のパターンをくり抜く
      //    3  袋影文字  左上 上 右上 左 右 左下 下 右下 右右下下  ずらしたパターンを重ねてから元のパターンをくり抜く
      //  縦書き
      //    1  袋文字    左下 下 右下 左 右 左上 上 右上           ずらしたパターンを重ねてから元のパターンをくり抜く
      //    2  影文字                               右上 右右上上  ずらしたパターンを重ねてから元のパターンをくり抜く
      //    3  袋影文字  左下 下 右下 左 右 左上 上 右上 右右上上  ずらしたパターンを重ねてから元のパターンをくり抜く
      byte[] bb = new byte[oh];
      Arrays.fill (bb, (byte) 0);
      if (prnVerticalWritingMode) {  //縦書き
        //右上にずらして重ねる
        for (int a = o, aa = 0; a < oh; a += o, aa += o) {
          bb[aa] |= (byte) ((b[a] & 255) >> 1);
          for (int i = 1; i < o; i++) {
            bb[aa + i] |= (byte) (b[a + i - 1] << 7 | (b[a + i] & 255) >> 1);
          }
        }
      } else {  //横書き
        //右下にずらして重ねる
        for (int a = 0, aa = o; aa < oh; a += o, aa += o) {
          bb[aa] |= (byte) ((b[a] & 255) >> 1);
          for (int i = 1; i < o; i++) {
            bb[aa + i] |= (byte) (b[a + i - 1] << 7 | (b[a + i] & 255) >> 1);
          }
        }
      }
      if (prnCharacterStyle == PRN_SHADOW_STYLE ||  //影文字
          prnCharacterStyle == PRN_OPEN_SHADOW_STYLE) {  //袋影文字
        if (prnVerticalWritingMode) {  //縦書き
          //右右上上にずらして重ねる
          for (int a = o * 2, aa = 0; a < oh; a += o, aa += o) {
            bb[aa] |= (byte) ((b[a] & 255) >> 2);
            for (int i = 1; i < o; i++) {
              bb[aa + i] |= (byte) (b[a + i - 1] << 6 | (b[a + i] & 255) >> 2);
            }
          }
        } else {
          //右右下下にずらして重ねる
          for (int a = 0, aa = o * 2; aa < oh; a += o, aa += o) {
            bb[aa] |= (byte) ((b[a] & 255) >> 2);
            for (int i = 1; i < o; i++) {
              bb[aa + i] |= (byte) (b[a + i - 1] << 6 | (b[a + i] & 255) >> 2);
            }
          }
        }
      }
      if (prnCharacterStyle == PRN_OPEN_STYLE ||  //袋文字
          prnCharacterStyle == PRN_OPEN_SHADOW_STYLE) {  //袋影文字
        //左上にずらして重ねる
        for (int a = o, aa = 0; a < oh; a += o, aa += o) {
          for (int i = 0; i < o - 1; i++) {
            bb[aa + i] |= (byte) (b[a + i] << 1 | (b[a + i + 1] & 255) >> 7);
          }
          bb[aa + o - 1] |= (byte) (b[a + o - 1] << 1);
        }
        //上にずらして重ねる
        for (int a = o, aa = 0; a < oh; a += o, aa += o) {
          for (int i = 0; i < o; i++) {
            bb[aa + i] |= b[a + i];
          }
        }
        if (prnVerticalWritingMode) {  //縦書き
          //右下にずらして重ねる
          for (int a = 0, aa = o; aa < oh; a += o, aa += o) {
            bb[aa] |= (byte) ((b[a] & 255) >> 1);
            for (int i = 1; i < o; i++) {
              bb[aa + i] |= (byte) (b[a + i - 1] << 7 | (b[a + i] & 255) >> 1);
            }
          }
        } else {  //横書き
          //右上にずらして重ねる
          for (int a = o, aa = 0; a < oh; a += o, aa += o) {
            bb[aa] |= (byte) ((b[a] & 255) >> 1);
            for (int i = 1; i < o; i++) {
              bb[aa + i] |= (byte) (b[a + i - 1] << 7 | (b[a + i] & 255) >> 1);
            }
          }
        }
        //左にずらして重ねる
        for (int a = 0; a < oh; a += o) {
          for (int i = 0; i < o - 1; i++) {
            bb[a + i] |= (byte) (b[a + i] << 1 | (b[a + i + 1] & 255) >> 7);
          }
          bb[a + o - 1] |= (byte) (b[a + o - 1] << 1);
        }
        //右にずらして重ねる
        for (int a = 0; a < oh; a += o) {
          bb[a] |= (byte) ((b[a] & 255) >> 1);
          for (int i = 1; i < o; i++) {
            bb[a + i] |= (byte) (b[a + i - 1] << 7 | (b[a + i] & 255) >> 1);
          }
        }
        //左下にずらして重ねる
        for (int a = 0, aa = o; aa < oh; a += o, aa += o) {
          for (int i = 0; i < o - 1; i++) {
            bb[aa + i] |= (byte) (b[a + i] << 1 | (b[a + i + 1] & 255) >> 7);
          }
          bb[aa + o - 1] |= (byte) (b[a + o - 1] << 1);
        }
        //下にずらして重ねる
        for (int a = 0, aa = o; aa < oh; a += o, aa += o) {
          for (int i = 0; i < o; i++) {
            bb[aa + i] |= b[a + i];
          }
        }
      }
      //元のパターンをくり抜く
      for (int i = 0; i < oh; i++) {
        bb[i] &= (byte) ~b[i];
      }
      b = bb;
    }  //if 袋文字または影文字または袋影文字
    //アンダーライン
    //  アンダーラインを袋文字や影文字にすることはできない
    if (prnUnderlineMode) {  //アンダーライン
      if (prnVerticalWritingMode) {  //縦書き
        int a = 0;
        for (int i = 0; i < o - 1; i++) {
          b[a + i] = -1;
        }
        b[a + o - 1] |= (byte) (-256 >> (((w - 1) & 7) + 1));
      } else {  //横書き
        int a = o * (h - 1);
        for (int i = 0; i < o - 1; i++) {
          b[a + i] = -1;
        }
        b[a + o - 1] |= (byte) (-256 >> (((w - 1) & 7) + 1));
      }
    }  //if アンダーライン
    //横2倍
    if (prnHorizontalDoubleSizeMode ||
        (prnKanjiMode && prnKanjiHorizontalDoubleSizeMode)) {  //横2倍
      int ww = w * 2;
      int oo = (ww + 7) >> 3;  //o*2またはo*2-1
      int ooh = oo * h;
      byte[] bb = new byte[ooh];
      for (int a = 0, aa = 0; a < oh; a += o, aa += oo) {  //ラスタの先頭
        for (int i = 0, ii = 0; i < o; i++, ii += 2) {  //ラスタ内インデックス
          int d = b[a + i] & 255;
          bb[aa + ii] = PRN_DOUBLE_4BIT[d >> 4];
          if (ii + 1 < oo) {
            bb[aa + ii + 1] = PRN_DOUBLE_4BIT[d & 15];
          }
        }
      }
      w = ww;
      o = oo;
      oh = ooh;
      b = bb;
    }  //if 横2倍
    //縦2倍
    if (prnVerticalDoubleSizeMode) {  //縦2倍
      int hh = h * 2;
      int ohh = o * hh;
      byte[] bb = new byte[ohh];
      for (int a = 0, aa = 0; a < oh; a += o, aa += o * 2) {
        for (int i = 0; i < o; i++) {
          bb[aa + o + i] = bb[aa + i] = b[a + i];
        }
      }
      h = hh;
      oh = ohh;
      b = bb;
    }  //if 縦2倍
    //行に収まるか
    if (prnMarginLeftX < prnHeadX &&  //左端ではなくて
        prnMarginRightX < prnHeadX + w) {  //右からはみ出す
      //改行する
      prnHeadX = prnMarginLeftX;
      prnHeadY += prnLineHeight;
      prnHeadLine++;
    }
    //コンテント範囲に収まるか
    if (prnContentTopY < prnHeadY &&  //上端ではなくて
        prnContentBottomY < prnHeadY + h) {  //下からはみ出す
      //改ページする
      prnPrintFormFeed ();
    }
    //文字を描く
    int bi = prnCheckRect (w, h);
    if (0 <= bi) {
      int ix = prnIncrementX;
      int iy = prnIncrementY;
      for (int y = 0; y < h; y++) {
        int i = bi;
        for (int x = 0; x < w; x++) {
          if ((b[(x >> 3) + o * y] >> (~x & 7) & 1) != 0) {
            prnBitmap[i] &= (byte) prnCurrentColor;  //ANDで描く
          }
          i += ix;
        }
        bi += iy;
      }
    } else {
      for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
          if ((b[(x >> 3) + o * y] >> (~x & 7) & 1) != 0) {
            prnPaintDot (x, y);
          }
        }
      }
    }
    prnHeadX += w;
    //印字あり
    prnPrinted = true;
    //キャンバスを再描画する
    if (prnCanvas != null) {
      prnCanvas.repaint ();
    }
  }  //prnPrintCharacter

  //prnPrintBackSpace ()
  //  BS  バックスペース  (p159)
  //  現在の文字の幅だけ後退する
  //  漢字モードのときは半角の幅
  //  横2倍が有効
  //  左マージンの位置で止まる
  public static void prnPrintBackSpace () {
    if (prnCurrentPaper == null) {  //未給紙のとき
      prnFeedPaper ();  //給紙する
    }
    prnHeadX = Math.max (prnMarginLeftX, prnHeadX - prnGetCharacterWidth ());
  }  //prnPrintBackSpace

  //prnPrintHorizontalTab ()
  //  HT  水平タブ  (p92)
  //  現在のヘッドの位置よりも右側にある最初の水平タブアンカーの位置まで進む
  //  なければ何もしない
  public static void prnPrintHorizontalTab () {
    if (prnCurrentPaper == null) {  //未給紙のとき
      prnFeedPaper ();  //給紙する
    }
    for (int k = 0; k < PRN_HORIZONTAL_ANCHOR_LIMIT; k++) {
      int x = prnHorizontalTabAnchor[k];
      if (x == 0) {  //終わり
        break;
      }
      if (prnHeadX < x) {  //現在のヘッドの位置よりも右側にある
        prnHeadX = Math.min (x, prnMarginRightX - 1);
        break;
      }
    }
  }  //prnPrintHorizontalTab

  //prnSetStartColumn (n)
  //  POS n1 n2 n3  開始桁位置設定  (p97)
  //  0は左マージンの位置
  //  単位は現在の文字の幅
  //  横2倍が有効
  public static void prnSetStartColumn (int n) {
    if (prnCurrentPaper == null) {  //未給紙のとき
      prnFeedPaper ();  //給紙する
    }
    if (0 <= n && n <= 999) {
      int startX = prnGetCharacterWidth () * n;
      if (prnMarginLeftX + startX < prnMarginRightX) {
        prnHeadX = prnMarginLeftX + startX;
      }
    }
  }  //prnSetStartColumn

  //prnPrintLineFeed (n)
  //  LF            1行改行  (p76)
  //  ESC VT n1 n2  n行改行  (p77)
  //  ヘッドのY座標に改行ピッチ*nを加える
  //  縦2倍が有効
  public static void prnPrintLineFeed (int n) {
    if (prnCurrentPaper == null) {  //未給紙のとき
      prnFeedPaper ();  //給紙する
    }
    if (n < 0) {
      n = 0;
    }
    if (prnVerticalDoubleSizeMode) {  //縦2倍
      n *= 2;
    }
    //改行する
    prnHeadY += prnLineHeight * n;
    prnHeadLine += n;
    //コンテント範囲に収まっているか
    if (prnContentBottomY <= prnHeadY) {  //下からはみ出した
      //改ページする
      prnPrintFormFeed ();
    }
    //カラーモードのとき単色モードに戻る
    if (prnColorMode) {  //カラーモードのとき
      prnColorMode = false;
      prnCurrentColor = prnSingleColor;  //現在の色
    }
  }  //prnPrintLineFeed

  //prnPrintVerticalTab (n)
  //  VT n  垂直タブ  (p83)
  //  nはチャンネル番号
  //  現在の行より下にある指定されたチャンネル番号の垂直タブアンカーの行まで改行を繰り返す
  //  なければ何もしない
  public static void prnPrintVerticalTab (int n) {
    if (prnCurrentPaper == null) {  //未給紙のとき
      prnFeedPaper ();  //給紙する
    }
    for (int k = prnHeadLine; k < PRN_VERTICAL_ANCHOR_LIMIT; k++) {
      if (prnVerticalTabAnchor[k] == n) {  //チャンネルが番号が一致した
        int d = k - prnHeadLine;  //進む行数
        //改行する
        prnHeadX = prnMarginLeftX;
        prnHeadY += prnLineHeight * d;
        prnHeadLine += d;
        //コンテント範囲に収まっているか
        if (prnContentBottomY <= prnHeadY) {  //下からはみ出した
          //改ページする
          prnPrintFormFeed ();
        }
        return;
      }
    }
  }  //prnPrintVerticalTab

  //prnPrintFormFeed ()
  //  FF  改ページ  (p80)
  //  ページの先頭でも次のページまで紙送りする
  public static void prnPrintFormFeed () {
    //改ページする
    prnPageStart += prnPageLength;
    //コンテント範囲
    prnContentTopY = prnPageStart;
    prnContentBottomY = Math.min (prnPageStart + prnPageLength - prnMarginBottomHeight,
                                  prnAliveBottomY - prnAliveTopY);
    //ヘッドの位置
    prnHeadX = prnMarginLeftX;
    prnHeadY = prnContentTopY;
    prnHeadLine = 0;
    //印字可能範囲に収まっているか
    if (prnAliveBottomY <= prnAliveTopY + prnHeadY) {  //下からはみ出した
      prnEjectPaper ();  //排紙する
      prnFeedPaper ();  //給紙する
    }
  }  //prnPrintFormFeed

  //prnPrintCarriageReturn ()
  //  CR  復帰  (p72)
  //  印字位置を左マージンまで戻す
  public static void prnPrintCarriageReturn () {
    if (prnCurrentPaper == null) {  //未給紙のとき
      prnFeedPaper ();  //給紙する
    }
    //ヘッドを左端に移動させる
    prnHeadX = prnMarginLeftX;
    //カラーモードのとき色を変更する
    if (prnColorMode) {  //カラーモードのとき
      prnCurrentColor = prnCurrentColor == 6 ? 5 : prnCurrentColor == 5 ? 3 : 6;  //6→5→3→6の順に切り替える
    }
  }  //prnPrintCarriageReturn

  //prnSetHorizontalDoubleSizeMode (b)
  //  b  true   SO     横2倍ON  (p104)
  //            ESC U  横2倍ON  (p105)
  //     false  SI     横2倍OFF  (p106)
  public static void prnSetHorizontalDoubleSizeMode (boolean b) {
    prnHorizontalDoubleSizeMode = b;
  }  //prnSetHorizontalDoubleSizeMode

  //prnSelect (b)
  //  b  true   DC1  セレクト  (p165)
  //     false  DC3  ディセレクト  (p166)
  public static void prnSelect (boolean b) {
    //!!!
  }  //prnSelect

  //prnSetVerticalTabAnchor (a, o, n)
  //  DC4 0 … 0 n1 0 … 0 n2 … nk ?  垂直タブアンカー設置  (p81)
  //  0は垂直タブ位置でない行
  //  1 2 3 4 5 6 7 8 9 : ; < = >は垂直タブ位置である行のチャンネル番号
  //  ?で終了
  //  チャンネル番号は昇順であること
  //  頁長または下マージンが設定されると垂直タブアンカーはクリアされる
  //  現在の印字位置がページの先頭になる
  public static void prnSetVerticalTabAnchor (byte[] a, int o, int n) {
    if (prnCurrentPaper == null) {  //未給紙のとき
      prnFeedPaper ();  //給紙する
    }
    prnSetPageStartPosition ();  //ページ先頭設定
    int pp = '0';  //前回のチャンネル番号
    int k = 0;
    for (; k < PRN_VERTICAL_ANCHOR_LIMIT && k < n; k++) {
      int p = a[o + k];  //チャンネル番号
      if (p == '0') {
        prnVerticalTabAnchor[k] = 0;
      } else if ('1' <= p && p <= '<') {
        if (p <= pp) {  //昇順になっていない
          break;
        }
        prnVerticalTabAnchor[k] = p - '0';
        pp = p;
      } else {
        break;
      }
    }
    for (; k < PRN_VERTICAL_ANCHOR_LIMIT; k++) {
      prnVerticalTabAnchor[k] = 0;
    }
  }  //prnSetVerticalTabAnchor

  //prnCancel ()
  //  CAN  キャンセル  (p158)
  public static void prnCancel () {
    //!!!
  }  //prnCancel

  //prnSetVerticalDoubleSizeMode (b)
  //  b  true   SUB V  縦2倍ON  (p107)
  //     false  SUB W  縦2倍OFF  (p108)
  public static void prnSetVerticalDoubleSizeMode (boolean b) {
    prnVerticalDoubleSizeMode = b;
  }  //prnSetVerticalDoubleSizeMode

  //prnSetHorizontalStartPosition (n)
  //  ESC POS n1 n2 n3 n4  水平開始位置設定  (p98)
  //  水平開始位置をn/180[in]にする
  public static void prnSetHorizontalStartPosition (int n) {
    if (prnCurrentPaper == null) {  //未給紙のとき
      prnFeedPaper ();  //給紙する
    }
    if (0 <= n && n <= 9999) {
      int startX = 2 * n;
      if (prnMarginLeftX + startX < prnMarginRightX) {
        prnHeadX = prnMarginLeftX + startX;
      }
    }
  }  //prnSetHorizontalStartPosition

  //prnSetColorMode ()
  //  ESC EM  カラーモード  (p167)
  public static void prnSetColorMode () {
    prnColorMode = true;  //カラーモード
    prnCurrentColor = 6;  //現在の色。6=イエロー
  }  //prnSetColorMode

  //prnSetStrongMode (b)
  //  b  true   ESC !  強調文字設定  (p109)
  //     false  ESC "  強調文字解除  (p110)
  public static void prnSetStrongMode (boolean b) {
    prnStrongMode = b;
  }  //prnSetStrongMode

  //prnSetHiraganaMode (b)
  //  b  true   ESC &  ひらがなモード  (p164)
  //     false  ESC $  カタカナモード  (p163)
  public static void prnSetHiraganaMode (boolean b) {
    prnHiraganaMode = b;
  }  //prnSetHiraganaMode

  //prnSetLineHeight (n)
  //  ESC % 9 n  1/120[in]紙送り量設定  (p75)
  //  改行ピッチをn/120[in]にする
  //  0<=n<=127
  //  0はESC % 9 nを実行する前の1/6[in]または1/8[in]に戻す
  public static void prnSetLineHeight (int n) {
    if (0 <= n && n <= 127) {
      if (n == 0) {
        prnLineHeight = prnDefaultLineHeight;
      } else {
        prnLineHeight = 3 * n;
      }
    }
  }  //prnSetLineHeight

  //prn8DotBitImage (a, o, n)
  //  ESC % 2 n1 n2 d1 d2 … dk  8ドットビットイメージ  (p142)
  //  パイカのとき
  //    横方向を4.5倍に拡大する
  //    縦方向を4.5倍に拡大する。高さが36[dot]になる
  //    改行ピッチが15/120[in]または16/120[in]のとき12/120[in]にする
  //  エリートのとき
  //    横方向を3倍に拡大する
  //    縦方向を4.5倍に拡大する。高さが36[dot]になる
  //    改行ピッチが15/120[in]または16/120[in]のとき12/120[in]にする
  //  改行ピッチが1/120[in]のとき
  //    縦方向を6倍に拡大して上3bitだけバッファに展開する
  //    LFが来たら先頭に戻って今度は下3bitだけ展開する
  public static void prn8DotBitImage (byte[] a, int o, int n) {
    if (prnCurrentPaper == null) {  //未給紙のとき
      prnFeedPaper ();  //給紙する
    }
    //!!!
  }  //prn8DotBitImage

  //prnSetHorizontalTabAnchor (a, o, n)
  //  ESC ( n1 , n2 , n3 , … , nk .  水平タブアンカー設置  (p90)
  //  nkは3桁の10進数で1個から16個まで
  //  nkに現在の文字幅(漢字モードのときは半角、横2倍が有効)が掛けられて絶対位置で記憶される
  //  0は左マージンの位置。右マージンを超える位置は無視される
  //  HTで次の水平タブ位置まで進む
  //  電源投入時はパイカで8桁毎に設定されている
  //  左右マージンが設定されるとすべての水平タブ位置がクリアされる
  public static void prnSetHorizontalTabAnchor (byte[] a, int o, int n) {
    if (prnCurrentPaper == null) {  //未給紙のとき
      prnFeedPaper ();  //給紙する
    }
    int characterWidth = prnGetCharacterWidth ();
    n += o;
    int k = 0;
    while (k < PRN_HORIZONTAL_ANCHOR_LIMIT &&
           o < n && '0' <= a[o] && a[o] <= '9') {
      int p = a[o] - '0';
      o++;
      while (o < n && '0' <= a[o] && a[o] <= '9') {  //本来は3桁固定
        p = p * 10 + (a[o] - '0');
        o++;
      }
      int x = prnMarginLeftX + characterWidth * p;  //絶対位置
      if ((k == 0 ? prnMarginLeftX :  //左マージンまたは
           prnHorizontalTabAnchor[k - 1]) < x) {  //直前の水平タブアンカーよりも右側になければならない
        prnHorizontalTabAnchor[k] = x;
        k++;
      }
      if (o < n && a[o] == ',') {  //継続
        o++;
      } else {  //終了
        break;
      }
    }
    for (; k < PRN_HORIZONTAL_ANCHOR_LIMIT; k++) {
      prnHorizontalTabAnchor[k] = 0;
    }
  }  //prnSetHorizontalTabAnchor

  //prnClearHorizontalTabAnchor (a, o, n)
  //  ESC ) n1 , n2 , … nk .  水平タブアンカー除去  (p93)
  //  nkは3桁の10進数で1個から16個まで
  //  nkに現在の文字幅(漢字モードのときは半角、横2倍が有効)が掛けられて絶対位置で比較される
  //  設置したときと同じ条件で除去しなければならない
  public static void prnClearHorizontalTabAnchor (byte[] a, int o, int n) {
    if (prnCurrentPaper == null) {  //未給紙のとき
      prnFeedPaper ();  //給紙する
    }
    int characterWidth = prnGetCharacterWidth ();
    n += o;
    while (o < n && '0' <= a[o] && a[o] <= '9') {
      int p = a[o] - '0';
      o++;
      while (o < n && '0' <= a[o] && a[o] <= '9') {  //本来は3桁固定
        p = p * 10 + (a[o] - '0');
        o++;
      }
      int x = prnMarginLeftX + characterWidth * p;  //絶対位置
      for (int k = 0; k < PRN_HORIZONTAL_ANCHOR_LIMIT; k++) {
        int t = prnHorizontalTabAnchor[k];
        if (t == 0 ||  //終わり
            x < t) {  //行き過ぎた。見つからなかった
          break;
        }
        if (t == x) {  //見つかった
          int j = k;
          for (; j < PRN_HORIZONTAL_ANCHOR_LIMIT - 1; j++) {
            prnHorizontalTabAnchor[j] = prnHorizontalTabAnchor[j + 1];  //詰める
          }
          prnHorizontalTabAnchor[j] = 0;
          break;  //1つしかないので終わり
        }
      }
      if (o < n && a[o] == ',') {  //継続
        o++;
      } else {  //終了
        break;
      }
    }
  }  //prnClearHorizontalTabAnchor

  //prn16DotExtendedCharacterDefinition ()
  //  ESC * n1 n2 d1 d2 … d32  16ドット外字定義  (p134)
  //  外字定義エリアは0x7621..0x767eと0x7721..0x7726の100文字
  //  16x16[dot]のパターンが縦横3倍に拡大されて48x48[dot]で保存される
  //  ESC c 1でクリアされる
  //             0         1              14         15
  //        +---------+---------+----+----------+----------+
  //     0  | d1 bit7 | d3 bit7 | .. | d29 bit7 | d31 bit7 |
  //        +---------+---------+----+----------+----------+
  //     1  | d1 bit6 | d3 bit6 | .. | d29 bit6 | d31 bit6 |
  //        +---------+---------+----+----------+----------+
  //        |    :    |    :    |    |    :     |    :     |
  //        +---------+---------+----+----------+----------+
  //     6  | d1 bit1 | d3 bit1 | .. | d29 bit1 | d31 bit1 |
  //        +---------+---------+----+----------+----------+
  //     7  | d1 bit0 | d3 bit0 | .. | d29 bit0 | d31 bit0 |
  //        +---------+---------+----+----------+----------+
  //     8  | d2 bit7 | d4 bit7 | .. | d30 bit7 | d32 bit7 |
  //        +---------+---------+----+----------+----------+
  //     9  | d2 bit6 | d4 bit6 | .. | d30 bit6 | d32 bit6 |
  //        +---------+---------+----+----------+----------+
  //        |    :    |    :    |    |    :     |    :     |
  //        +---------+---------+----+----------+----------+
  //    14  | d2 bit1 | d4 bit1 | .. | d30 bit1 | d32 bit1 |
  //        +---------+---------+----+----------+----------+
  //    15  | d2 bit0 | d4 bit0 | .. | d30 bit0 | d32 bit0 |
  //        +---------+---------+----+----------+----------+
  public static void prn16DotExtendedCharacterDefinition (byte[] a, int o, int n) {
    byte[] gaijiData = prnGetGaijiData ();
    int n1 = a[o] & 255;
    int n2 = a[o + 1] & 255;
    if (!((n1 == 0x76 && (0x21 <= n2 && n2 <= 0x7e)) ||
          (n1 == 0x77 && (0x21 <= n2 && n2 <= 0x26)))) {  //外字定義エリアではない
      return;
    }
    int i = 6 * 48 * ((n2 - 0x21) + 94 * (n1 - 0x76));
    int j = o + 2;
    for (int y = 0; y < 16; y++) {
      int t = 0;
      for (int x = 0; x < 8; x++) {
        t = t << 3 | (a[j + 2 * x + (y >> 3)] >> (~y & 7) & 1) * 7;
      }
      gaijiData[i    ] = gaijiData[i + 6] = gaijiData[i + 12] = (byte) (t >> 16);
      gaijiData[i + 1] = gaijiData[i + 7] = gaijiData[i + 13] = (byte) (t >> 8);
      gaijiData[i + 2] = gaijiData[i + 8] = gaijiData[i + 14] = (byte) t;
      t = 0;
      for (int x = 8; x < 16; x++) {
        t = t << 3 | (a[j + 2 * x + (y >> 3)] >> (~y & 7) & 1) * 7;
      }
      gaijiData[i + 3] = gaijiData[i +  9] = gaijiData[i + 15] = (byte) (t >> 16);
      gaijiData[i + 4] = gaijiData[i + 10] = gaijiData[i + 16] = (byte) (t >> 8);
      gaijiData[i + 5] = gaijiData[i + 11] = gaijiData[i + 17] = (byte) t;
      i += 18;
    }
  }  //prn16DotExtendedCharacterDefinition

  //prn24DotExtendedCharacterDefinition ()
  //  ESC + n1 n2 d1 d2 … d72  24ドット外字定義  (p138)
  //  外字定義エリアは0x7621..0x767eと0x7721..0x7726の100文字
  //  24x24[dot]のパターンが縦横2倍に拡大されて48x48[dot]で保存される
  //  ESC c 1でクリアされる
  //             0         1              22         23
  //        +---------+---------+----+----------+----------+
  //     0  | d1 bit7 | d4 bit7 | .. | d67 bit7 | d70 bit7 |
  //        +---------+---------+----+----------+----------+
  //     1  | d1 bit6 | d4 bit6 | .. | d67 bit6 | d70 bit6 |
  //        +---------+---------+----+----------+----------+
  //        |    :    |    :    |    |    :     |    :     |
  //        +---------+---------+----+----------+----------+
  //     6  | d1 bit1 | d4 bit1 | .. | d67 bit1 | d70 bit1 |
  //        +---------+---------+----+----------+----------+
  //     7  | d1 bit0 | d4 bit0 | .. | d67 bit0 | d70 bit0 |
  //        +---------+---------+----+----------+----------+
  //     8  | d2 bit7 | d5 bit7 | .. | d68 bit7 | d71 bit7 |
  //        +---------+---------+----+----------+----------+
  //     9  | d2 bit6 | d5 bit6 | .. | d68 bit6 | d71 bit6 |
  //        +---------+---------+----+----------+----------+
  //        |    :    |    :    |    |    :     |    :     |
  //        +---------+---------+----+----------+----------+
  //    14  | d2 bit1 | d5 bit1 | .. | d68 bit1 | d71 bit1 |
  //        +---------+---------+----+----------+----------+
  //    15  | d2 bit0 | d5 bit0 | .. | d68 bit0 | d71 bit0 |
  //        +---------+---------+----+----------+----------+
  //    16  | d3 bit7 | d6 bit7 | .. | d69 bit7 | d72 bit7 |
  //        +---------+---------+----+----------+----------+
  //    17  | d3 bit6 | d6 bit6 | .. | d69 bit6 | d72 bit6 |
  //        +---------+---------+----+----------+----------+
  //        |    :    |    :    |    |    :     |    :     |
  //        +---------+---------+----+----------+----------+
  //    22  | d3 bit1 | d6 bit1 | .. | d69 bit1 | d72 bit1 |
  //        +---------+---------+----+----------+----------+
  //    23  | d3 bit0 | d6 bit0 | .. | d69 bit0 | d72 bit0 |
  //        +---------+---------+----+----------+----------+
  public static void prn24DotExtendedCharacterDefinition (byte[] a, int o, int n) {
    byte[] gaijiData = prnGetGaijiData ();
    int n1 = a[o] & 255;
    int n2 = a[o + 1] & 255;
    if (!((n1 == 0x76 && (0x21 <= n2 && n2 <= 0x7e)) ||
          (n1 == 0x77 && (0x21 <= n2 && n2 <= 0x26)))) {  //外字定義エリアではない
      return;
    }
    int i = 6 * 48 * ((n2 - 0x21) + 94 * (n1 - 0x76));
    int j = o + 2;
    for (int y = 0; y < 24; y++) {
      int t = 0;
      for (int x = 0; x < 16; x++) {
        t = t << 2 | (a[j + 3 * x + (y >> 3)] >> (~y & 7) & 1) * 3;
      }
      gaijiData[i    ] = gaijiData[i + 6] = (byte) (t >> 24);
      gaijiData[i + 1] = gaijiData[i + 7] = (byte) (t >> 16);
      gaijiData[i + 2] = gaijiData[i + 8] = (byte) (t >> 8);
      gaijiData[i + 3] = gaijiData[i + 9] = (byte) t;
      t = 0;
      for (int x = 16; x < 24; x++) {
        t = t << 2 | (a[j + 3 * x + (y >> 3)] >> (~y & 7) & 1) * 3;
      }
      gaijiData[i + 4] = gaijiData[i + 10] = (byte) (t >> 8);
      gaijiData[i + 5] = gaijiData[i + 11] = (byte) t;
      i += 12;
    }
  }  //prn24DotExtendedCharacterDefinition

  //prnSetRightMargin (n)
  //  ESC / n1 n2 n3  右マージン設定  (p88)
  //  右マージンをn桁にする
  //  最大数よりも大きいと無視される
  //  水平タブ位置はクリアされる
  //  単位は現在の文字の幅
  //  漢字モードのときは半角の幅
  //  横2倍は無視される
  public static void prnSetRightMargin (int n) {
    if (prnCurrentPaper == null) {  //未給紙のとき
      prnFeedPaper ();  //給紙する
    }
    if (0 <= n && n <= 999) {
      int rightX = prnGetCharacterWidth () * n;
      if (prnMarginLeftX < rightX) {
        prnMarginRightX = rightX;
        //水平タブ位置をクリアする
        for (int k = 0; k < PRN_HORIZONTAL_ANCHOR_LIMIT; k++) {
          prnHorizontalTabAnchor[k] = 0;
        }
      }
    }
  }  //prnSetRightMargin

  //prnClearAllHorizontalTabAnchor ()
  //  ESC 2  全水平タブアンカー除去  (p95)
  public static void prnClearAllHorizontalTabAnchor () {
    if (prnCurrentPaper == null) {  //未給紙のとき
      prnFeedPaper ();  //給紙する
    }
    //水平タブ位置をクリアする
    for (int k = 0; k < PRN_HORIZONTAL_ANCHOR_LIMIT; k++) {
      prnHorizontalTabAnchor[k] = 0;
    }
  }  //prnClearAllHorizontalTabAnchor

  //prnSetPageStartPosition ()
  //  ESC 5  ページ先頭設定  (p79)
  //  現在の印字位置がページの先頭になる
  public static void prnSetPageStartPosition () {
    if (prnCurrentPaper == null) {  //未給紙のとき
      prnFeedPaper ();  //給紙する
    }
    //ページ範囲
    prnPageStart = prnHeadY;  //現在の印字位置がページの先頭になる
    //コンテント範囲
    prnContentTopY = prnPageStart;
    prnContentBottomY = Math.min (prnPageStart + prnPageLength - prnMarginBottomHeight,
                                  prnAliveBottomY - prnAliveTopY);
    //ヘッドの位置
    prnHeadX = prnMarginLeftX;
    prnHeadY = prnContentTopY;
    prnHeadLine = 0;
  }  //prnSetPageStartPosition

  //prnSetOneSixth ()
  //  ESC 6  1/6[in]改行設定  (p73)
  public static void prnSetOneSixth () {
    prnLineHeight = 60;
    prnDefaultLineHeight = 60;
  }  //prnSetOneSixth

  //prnSetOneEighth ()
  //  ESC 8  1/8[in]改行設定  (p74)
  public static void prnSetOneEighth () {
    prnLineHeight = 45;
    prnDefaultLineHeight = 45;
  }  //prnSetOneEighth

  //prnSetBottomMargin (n)
  //  ESC C n1 n2  下マージン設定  (p84)
  //  下マージンをn行にする
  //  頁長が設定されるとクリアされる
  //  頁長よりも長く設定しようとすると無視される
  //  頁長と下マージンの差が1行の改行ピッチより短くても1行は印字される
  public static void prnSetBottomMargin (int n) {
    if (prnCurrentPaper == null) {  //未給紙のとき
      prnFeedPaper ();  //給紙する
    }
    if (0 <= n && n <= 99) {
      int height = prnLineHeight * n;
      if (height < prnPageLength) {
        prnMarginBottomHeight = height;
        //コンテント範囲
        prnContentBottomY = Math.min (prnPageStart + prnPageLength - height,
                                      prnAliveBottomY - prnAliveTopY);
        //コンテント範囲に収まっているか
        if (prnContentBottomY <= prnHeadY) {  //下からはみ出した
          //改ページする
          prnPrintFormFeed ();
        }
        //垂直タブ位置をクリアする
        for (int k = 0; k < PRN_VERTICAL_ANCHOR_LIMIT; k++) {
          prnVerticalTabAnchor[k] = 0;
        }
      }
    }
  }  //prnSetBottomMargin

  //prnSetEliteCharacterMode ()
  //  ESC E  エリート文字設定  (p102)
  //  エリート文字(1/12[in])にする
  //  https://en.wikipedia.org/wiki/Typewriter#Character_sizes
  public static void prnSetEliteCharacterMode () {
    prnCharacterType = PRN_ELITE;  //エリート文字
  }  //prnSetEliteCharacterMode

  //prnSetPageHeight (n)
  //  ESC F n1 n2  ページ高さ設定  (p78)
  //  ページの高さをn/2[in]にする
  //  現在の印字位置がページの先頭になる
  //  n=0は無視される
  //  下マージンはクリアされる
  public static void prnSetPageHeight (int n) {
    if (prnCurrentPaper == null) {  //未給紙のとき
      prnFeedPaper ();  //給紙する
    }
    prnSetPageStartPosition ();  //ページ先頭設定
    if (1 <= n && n <= 99) {
      //ページ長
      prnPageLength = 180 * n;
      //下マージン
      prnMarginBottomHeight = 0;
      //ページ開始位置
      prnSetPageStartPosition ();
    }
  }  //prnSetPageHeight

  //prn16DotBitImage (a, o, n)
  //  ESC I n1 n2 n3 n4 d1 d2 … dk  16ドットビットイメージ  (p148)
  //  横方向を3倍に拡大する
  //  縦方向を3倍に拡大する。高さが48[dot]になる
  //  改行ピッチが15/120[in]のとき16/120[in]にする
  //  横2倍と縦2倍が有効
  //  行からはみ出した部分は無視される
  public static void prn16DotBitImage (byte[] a, int o, int n) {
    if (prnCurrentPaper == null) {  //未給紙のとき
      prnFeedPaper ();  //給紙する
    }
    //改行ピッチが15/120[in]のとき16/120[in]にする
    if (prnLineHeight == 45) {
      prnLineHeight = 48;
    }
    if (!prnHorizontalDoubleSizeMode) {  //横1倍
      if (!prnVerticalDoubleSizeMode) {  //縦1倍
        //横1倍,縦1倍
        int aw = n;  //横nビット→3*nドット
        int ah = 16;  //縦16ビット→48ドット
        aw = Math.min (3 * aw, prnMarginRightX - prnHeadX) / 3;  //横awビット→3*awドット
        ah = Math.min (3 * ah, prnContentBottomY - prnHeadY) / 3;  //縦ahビット→3*ahドット
        int bi = prnCheckRect (3 * aw, 3 * ah);
        if (0 <= bi) {
          int ix = prnIncrementX;
          int iy = prnIncrementY;
          for (int ay = 0; ay < ah; ay++) {  //縦ビット位置
            int i = bi;
            for (int ax = 0; ax < aw; ax++) {  //横ビット位置
              if ((a[o + 2 * ax + (ay >> 3)] >> (~ay & 7) & 1) != 0) {  //ビットがセットされている
                prnBitmap[i                  ] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix         ] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix         ] &= (byte) prnCurrentColor;
                prnBitmap[i          +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i          + 2 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix + 2 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix + 2 * iy] &= (byte) prnCurrentColor;
              }
              i += 3 * ix;
            }
            bi += 3 * iy;
          }
        } else {
          for (int ay = 0; ay < ah; ay++) {  //縦ビット位置
            for (int ax = 0; ax < aw; ax++) {  //横ビット位置
              if ((a[o + 2 * ax + (ay >> 3)] >> (~ay & 7) & 1) != 0) {  //ビットがセットされている
                prnPaintDot (3 * ax    , 3 * ay    );
                prnPaintDot (3 * ax + 1, 3 * ay    );
                prnPaintDot (3 * ax + 2, 3 * ay    );
                prnPaintDot (3 * ax    , 3 * ay + 1);
                prnPaintDot (3 * ax + 1, 3 * ay + 1);
                prnPaintDot (3 * ax + 2, 3 * ay + 1);
                prnPaintDot (3 * ax    , 3 * ay + 2);
                prnPaintDot (3 * ax + 1, 3 * ay + 2);
                prnPaintDot (3 * ax + 2, 3 * ay + 2);
              }
            }
          }
        }
        prnHeadX += 3 * aw;
      } else {  //縦2倍
        //横1倍,縦2倍
        int aw = n;  //横nビット→3*nドット
        int ah = 16;  //縦16ビット→96ドット
        aw = Math.min (3 * aw, prnMarginRightX - prnHeadX) / 3;  //横awビット→3*awドット
        ah = Math.min (6 * ah, prnContentBottomY - prnHeadY) / 6;  //縦ahビット→6*ahドット
        int bi = prnCheckRect (3 * aw, 6 * ah);
        if (0 <= bi) {
          int ix = prnIncrementX;
          int iy = prnIncrementY;
          for (int ay = 0; ay < ah; ay++) {  //縦ビット位置
            int i = bi;
            for (int ax = 0; ax < aw; ax++) {  //横ビット位置
              if ((a[o + 2 * ax + (ay >> 3)] >> (~ay & 7) & 1) != 0) {  //ビットがセットされている
                prnBitmap[i                  ] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix         ] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix         ] &= (byte) prnCurrentColor;
                prnBitmap[i          +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i          + 2 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix + 2 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix + 2 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i          + 3 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix + 3 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix * 3 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i          + 4 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix + 4 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix + 4 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i          + 5 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix + 5 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix + 5 * iy] &= (byte) prnCurrentColor;
              }
              i += 3 * ix;
            }
            bi += 6 * iy;
          }
        } else {
          for (int ay = 0; ay < ah; ay++) {  //縦ビット位置
            for (int ax = 0; ax < aw; ax++) {  //横ビット位置
              if ((a[o + 2 * ax + (ay >> 3)] >> (~ay & 7) & 1) != 0) {  //ビットがセットされている
                prnPaintDot (3 * ax    , 6 * ay    );
                prnPaintDot (3 * ax + 1, 6 * ay    );
                prnPaintDot (3 * ax + 2, 6 * ay    );
                prnPaintDot (3 * ax    , 6 * ay + 1);
                prnPaintDot (3 * ax + 1, 6 * ay + 1);
                prnPaintDot (3 * ax + 2, 6 * ay + 1);
                prnPaintDot (3 * ax    , 6 * ay + 2);
                prnPaintDot (3 * ax + 1, 6 * ay + 2);
                prnPaintDot (3 * ax + 2, 6 * ay + 2);
                prnPaintDot (3 * ax    , 6 * ay + 3);
                prnPaintDot (3 * ax + 1, 6 * ay + 3);
                prnPaintDot (3 * ax + 2, 6 * ay + 3);
                prnPaintDot (3 * ax    , 6 * ay + 4);
                prnPaintDot (3 * ax + 1, 6 * ay + 4);
                prnPaintDot (3 * ax + 2, 6 * ay + 4);
                prnPaintDot (3 * ax    , 6 * ay + 5);
                prnPaintDot (3 * ax + 1, 6 * ay + 5);
                prnPaintDot (3 * ax + 2, 6 * ay + 5);
              }
            }
          }
        }
        prnHeadX += 3 * aw;
      }  //if 縦1倍/縦2倍
    } else {  //横2倍
      if (!prnVerticalDoubleSizeMode) {  //縦1倍
        //横2倍,縦1倍
        int aw = n;  //横nビット→6*nドット
        int ah = 16;  //縦16ビット→48ドット
        aw = Math.min (6 * aw, prnMarginRightX - prnHeadX) / 6;  //横awビット→6*awドット
        ah = Math.min (3 * ah, prnContentBottomY - prnHeadY) / 3;  //縦ahビット→3*ahドット
        int bi = prnCheckRect (6 * aw, 3 * ah);
        if (0 <= bi) {
          int ix = prnIncrementX;
          int iy = prnIncrementY;
          for (int ay = 0; ay < ah; ay++) {  //縦ビット位置
            int i = bi;
            for (int ax = 0; ax < aw; ax++) {  //横ビット位置
              if ((a[o + 2 * ax + (ay >> 3)] >> (~ay & 7) & 1) != 0) {  //ビットがセットされている
                prnBitmap[i                  ] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix         ] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix         ] &= (byte) prnCurrentColor;
                prnBitmap[i + 3 * ix         ] &= (byte) prnCurrentColor;
                prnBitmap[i + 4 * ix         ] &= (byte) prnCurrentColor;
                prnBitmap[i + 5 * ix         ] &= (byte) prnCurrentColor;
                prnBitmap[i          +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 3 * ix +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 4 * ix +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 5 * ix +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i          + 2 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix + 2 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix + 2 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 3 * ix + 2 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 4 * ix + 2 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 5 * ix + 2 * iy] &= (byte) prnCurrentColor;
              }
              i += 6 * ix;
            }
            bi += 3 * iy;
          }
        } else {
          for (int ay = 0; ay < ah; ay++) {  //縦ビット位置
            for (int ax = 0; ax < aw; ax++) {  //横ビット位置
              if ((a[o + 2 * ax + (ay >> 3)] >> (~ay & 7) & 1) != 0) {  //ビットがセットされている
                prnPaintDot (6 * ax    , 3 * ay    );
                prnPaintDot (6 * ax + 1, 3 * ay    );
                prnPaintDot (6 * ax + 2, 3 * ay    );
                prnPaintDot (6 * ax + 3, 3 * ay    );
                prnPaintDot (6 * ax + 4, 3 * ay    );
                prnPaintDot (6 * ax + 5, 3 * ay    );
                prnPaintDot (6 * ax    , 3 * ay + 1);
                prnPaintDot (6 * ax + 1, 3 * ay + 1);
                prnPaintDot (6 * ax + 2, 3 * ay + 1);
                prnPaintDot (6 * ax + 3, 3 * ay + 1);
                prnPaintDot (6 * ax + 4, 3 * ay + 1);
                prnPaintDot (6 * ax + 5, 3 * ay + 1);
                prnPaintDot (6 * ax    , 3 * ay + 2);
                prnPaintDot (6 * ax + 1, 3 * ay + 2);
                prnPaintDot (6 * ax + 2, 3 * ay + 2);
                prnPaintDot (6 * ax + 3, 3 * ay + 2);
                prnPaintDot (6 * ax + 4, 3 * ay + 2);
                prnPaintDot (6 * ax + 5, 3 * ay + 2);
              }
            }
          }
        }
        prnHeadX += 6 * aw;
      } else {  //縦2倍
        //横2倍,縦2倍
        int aw = n;  //横nビット→6*nドット
        int ah = 16;  //縦16ビット→96ドット
        aw = Math.min (6 * aw, prnMarginRightX - prnHeadX) / 6;  //横awビット→6*awドット
        ah = Math.min (6 * ah, prnContentBottomY - prnHeadY) / 6;  //縦ahビット→6*ahドット
        int bi = prnCheckRect (6 * aw, 6 * ah);
        if (0 <= bi) {
          int ix = prnIncrementX;
          int iy = prnIncrementY;
          for (int ay = 0; ay < ah; ay++) {  //縦ビット位置
            int i = bi;
            for (int ax = 0; ax < aw; ax++) {  //横ビット位置
              if ((a[o + 2 * ax + (ay >> 3)] >> (~ay & 7) & 1) != 0) {  //ビットがセットされている
                prnBitmap[i                  ] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix         ] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix         ] &= (byte) prnCurrentColor;
                prnBitmap[i + 3 * ix         ] &= (byte) prnCurrentColor;
                prnBitmap[i + 4 * ix         ] &= (byte) prnCurrentColor;
                prnBitmap[i + 5 * ix         ] &= (byte) prnCurrentColor;
                prnBitmap[i          +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 3 * ix +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 4 * ix +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 5 * ix +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i          + 2 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix + 2 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix + 2 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 3 * ix + 2 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 4 * ix + 2 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 5 * ix + 2 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i          + 3 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix + 3 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix * 3 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 3 * ix * 3 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 4 * ix * 3 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 5 * ix * 3 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i          + 4 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix + 4 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix + 4 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 3 * ix + 4 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 4 * ix + 4 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 5 * ix + 4 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i          + 5 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix + 5 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix + 5 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 3 * ix + 5 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 4 * ix + 5 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 5 * ix + 5 * iy] &= (byte) prnCurrentColor;
              }
              i += 6 * ix;
            }
            bi += 6 * iy;
          }
        } else {
          for (int ay = 0; ay < ah; ay++) {  //縦ビット位置
            for (int ax = 0; ax < aw; ax++) {  //横ビット位置
              if ((a[o + 2 * ax + (ay >> 3)] >> (~ay & 7) & 1) != 0) {  //ビットがセットされている
                prnPaintDot (6 * ax    , 6 * ay    );
                prnPaintDot (6 * ax + 1, 6 * ay    );
                prnPaintDot (6 * ax + 2, 6 * ay    );
                prnPaintDot (6 * ax + 3, 6 * ay    );
                prnPaintDot (6 * ax + 4, 6 * ay    );
                prnPaintDot (6 * ax + 5, 6 * ay    );
                prnPaintDot (6 * ax    , 6 * ay + 1);
                prnPaintDot (6 * ax + 1, 6 * ay + 1);
                prnPaintDot (6 * ax + 2, 6 * ay + 1);
                prnPaintDot (6 * ax + 3, 6 * ay + 1);
                prnPaintDot (6 * ax + 4, 6 * ay + 1);
                prnPaintDot (6 * ax + 5, 6 * ay + 1);
                prnPaintDot (6 * ax    , 6 * ay + 2);
                prnPaintDot (6 * ax + 1, 6 * ay + 2);
                prnPaintDot (6 * ax + 2, 6 * ay + 2);
                prnPaintDot (6 * ax + 3, 6 * ay + 2);
                prnPaintDot (6 * ax + 4, 6 * ay + 2);
                prnPaintDot (6 * ax + 5, 6 * ay + 2);
                prnPaintDot (6 * ax    , 6 * ay + 3);
                prnPaintDot (6 * ax + 1, 6 * ay + 3);
                prnPaintDot (6 * ax + 2, 6 * ay + 3);
                prnPaintDot (6 * ax + 3, 6 * ay + 3);
                prnPaintDot (6 * ax + 4, 6 * ay + 3);
                prnPaintDot (6 * ax + 5, 6 * ay + 3);
                prnPaintDot (6 * ax    , 6 * ay + 4);
                prnPaintDot (6 * ax + 1, 6 * ay + 4);
                prnPaintDot (6 * ax + 2, 6 * ay + 4);
                prnPaintDot (6 * ax + 3, 6 * ay + 4);
                prnPaintDot (6 * ax + 4, 6 * ay + 4);
                prnPaintDot (6 * ax + 5, 6 * ay + 4);
                prnPaintDot (6 * ax    , 6 * ay + 5);
                prnPaintDot (6 * ax + 1, 6 * ay + 5);
                prnPaintDot (6 * ax + 2, 6 * ay + 5);
                prnPaintDot (6 * ax + 3, 6 * ay + 5);
                prnPaintDot (6 * ax + 4, 6 * ay + 5);
                prnPaintDot (6 * ax + 5, 6 * ay + 5);
              }
            }
          }
        }
        prnHeadX += 6 * aw;
      }  //if 縦1倍/縦2倍
    }  //if 横1倍/横2倍
    //印字あり
    prnPrinted = true;
    //キャンバスを再描画する
    if (prnCanvas != null) {
      prnCanvas.repaint ();
    }
  }  //prn16DotBitImage

  //prn24DotBitImage (a, o, n)
  //  ESC J n1 n2 d1 d2 … dk  24ドットビットイメージ  (p151)
  //  横方向を2倍に拡大する
  //  縦方向を2倍に拡大する。高さが48[dot]になる
  //  改行ピッチが15/120[in]のとき16/120[in]にする
  //  横2倍と縦2倍が有効
  //  行からはみ出した部分は無視される
  public static void prn24DotBitImage (byte[] a, int o, int n) {
    if (prnCurrentPaper == null) {  //未給紙のとき
      prnFeedPaper ();  //給紙する
    }
    //改行ピッチが15/120[in]のとき16/120[in]にする
    if (prnLineHeight == 45) {
      prnLineHeight = 48;
    }
    if (!prnHorizontalDoubleSizeMode) {  //横1倍
      if (!prnVerticalDoubleSizeMode) {  //縦1倍
        //横1倍,縦1倍
        int aw = n;  //横nビット→2*nドット
        int ah = 24;  //縦24ビット→48ドット
        aw = Math.min (2 * aw, prnMarginRightX - prnHeadX) >> 1;  //横awビット→2*awドット
        ah = Math.min (2 * ah, prnContentBottomY - prnHeadY) >> 1;  //縦ahビット→2*ahドット
        int bi = prnCheckRect (2 * aw, 2 * ah);
        if (0 <= bi) {
          int ix = prnIncrementX;
          int iy = prnIncrementY;
          for (int ay = 0; ay < ah; ay++) {  //縦ビット位置
            int i = bi;
            for (int ax = 0; ax < aw; ax++) {  //横ビット位置
              if ((a[o + 3 * ax + (ay >> 3)] >> (~ay & 7) & 1) != 0) {  //ビットがセットされている
                prnBitmap[i                  ] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix         ] &= (byte) prnCurrentColor;
                prnBitmap[i          +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix +     iy] &= (byte) prnCurrentColor;
              }
              i += 2 * ix;
            }
            bi += 2 * iy;
          }
        } else {
          for (int ay = 0; ay < ah; ay++) {  //縦ビット位置
            for (int ax = 0; ax < aw; ax++) {  //横ビット位置
              if ((a[o + 3 * ax + (ay >> 3)] >> (~ay & 7) & 1) != 0) {  //ビットがセットされている
                prnPaintDot (2 * ax    , 2 * ay    );
                prnPaintDot (2 * ax + 1, 2 * ay    );
                prnPaintDot (2 * ax    , 2 * ay + 1);
                prnPaintDot (2 * ax + 1, 2 * ay + 1);
              }
            }
          }
        }
        prnHeadX += 2 * aw;
      } else {  //縦2倍
        //横1倍,縦2倍
        int aw = n;  //横nビット→2*nドット
        int ah = 24;  //縦24ビット→96ドット
        aw = Math.min (2 * aw, prnMarginRightX - prnHeadX) >> 1;  //横awビット→2*awドット
        ah = Math.min (4 * ah, prnContentBottomY - prnHeadY) >> 2;  //縦ahビット→4*ahドット
        int bi = prnCheckRect (2 * aw, 4 * ah);
        if (0 <= bi) {
          int ix = prnIncrementX;
          int iy = prnIncrementY;
          for (int ay = 0; ay < ah; ay++) {  //縦ビット位置
            int i = bi;
            for (int ax = 0; ax < aw; ax++) {  //横ビット位置
              if ((a[o + 3 * ax + (ay >> 3)] >> (~ay & 7) & 1) != 0) {  //ビットがセットされている
                prnBitmap[i                  ] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix         ] &= (byte) prnCurrentColor;
                prnBitmap[i          +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i          + 2 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix + 2 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i          + 3 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix + 3 * iy] &= (byte) prnCurrentColor;
              }
              i += 2 * ix;
            }
            bi += 4 * iy;
          }
        } else {
          for (int ay = 0; ay < ah; ay++) {  //縦ビット位置
            for (int ax = 0; ax < aw; ax++) {  //横ビット位置
              if ((a[o + 3 * ax + (ay >> 3)] >> (~ay & 7) & 1) != 0) {  //ビットがセットされている
                prnPaintDot (2 * ax    , 4 * ay    );
                prnPaintDot (2 * ax + 1, 4 * ay    );
                prnPaintDot (2 * ax    , 4 * ay + 1);
                prnPaintDot (2 * ax + 1, 4 * ay + 1);
                prnPaintDot (2 * ax    , 4 * ay + 2);
                prnPaintDot (2 * ax + 1, 4 * ay + 2);
                prnPaintDot (2 * ax    , 4 * ay + 3);
                prnPaintDot (2 * ax + 1, 4 * ay + 3);
              }
            }
          }
        }
        prnHeadX += 2 * aw;
      }  //if 縦1倍/縦2倍
    } else {  //横2倍
      if (!prnVerticalDoubleSizeMode) {  //縦1倍
        //横2倍,縦1倍
        int aw = n;  //横nビット→4*nドット
        int ah = 24;  //縦24ビット→48ドット
        aw = Math.min (4 * aw, prnMarginRightX - prnHeadX) >> 2;  //横awビット→4*awドット
        ah = Math.min (2 * ah, prnContentBottomY - prnHeadY) >> 1;  //縦ahビット→2*ahドット
        int bi = prnCheckRect (4 * aw, 2 * ah);
        if (0 <= bi) {
          int ix = prnIncrementX;
          int iy = prnIncrementY;
          for (int ay = 0; ay < ah; ay++) {  //縦ビット位置
            int i = bi;
            for (int ax = 0; ax < aw; ax++) {  //横ビット位置
              if ((a[o + 3 * ax + (ay >> 3)] >> (~ay & 7) & 1) != 0) {  //ビットがセットされている
                prnBitmap[i                  ] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix         ] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix         ] &= (byte) prnCurrentColor;
                prnBitmap[i + 3 * ix         ] &= (byte) prnCurrentColor;
                prnBitmap[i          +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 3 * ix +     iy] &= (byte) prnCurrentColor;
              }
              i += 4 * ix;
            }
            bi += 2 * iy;
          }
        } else {
          for (int ay = 0; ay < ah; ay++) {  //縦ビット位置
            for (int ax = 0; ax < aw; ax++) {  //横ビット位置
              if ((a[o + 3 * ax + (ay >> 3)] >> (~ay & 7) & 1) != 0) {  //ビットがセットされている
                prnPaintDot (4 * ax    , 2 * ay    );
                prnPaintDot (4 * ax + 1, 2 * ay    );
                prnPaintDot (4 * ax + 2, 2 * ay    );
                prnPaintDot (4 * ax + 3, 2 * ay    );
                prnPaintDot (4 * ax    , 2 * ay + 1);
                prnPaintDot (4 * ax + 1, 2 * ay + 1);
                prnPaintDot (4 * ax + 2, 2 * ay + 1);
                prnPaintDot (4 * ax + 3, 2 * ay + 1);
              }
            }
          }
        }
        prnHeadX += 4 * aw;
      } else {  //縦2倍
        //横2倍,縦2倍
        int aw = n;  //横nビット→4*nドット
        int ah = 24;  //縦24ビット→96ドット
        aw = Math.min (4 * aw, prnMarginRightX - prnHeadX) >> 2;  //横awビット→4*awドット
        ah = Math.min (4 * ah, prnContentBottomY - prnHeadY) >> 2;  //縦ahビット→4*ahドット
        int bi = prnCheckRect (4 * aw, 4 * ah);
        if (0 <= bi) {
          int ix = prnIncrementX;
          int iy = prnIncrementY;
          for (int ay = 0; ay < ah; ay++) {  //縦ビット位置
            int i = bi;
            for (int ax = 0; ax < aw; ax++) {  //横ビット位置
              if ((a[o + 3 * ax + (ay >> 3)] >> (~ay & 7) & 1) != 0) {  //ビットがセットされている
                prnBitmap[i                  ] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix         ] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix         ] &= (byte) prnCurrentColor;
                prnBitmap[i + 3 * ix         ] &= (byte) prnCurrentColor;
                prnBitmap[i          +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 3 * ix +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i          + 2 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix + 2 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix + 2 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 3 * ix + 2 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i          + 3 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix + 3 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix + 3 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 3 * ix + 3 * iy] &= (byte) prnCurrentColor;
              }
              i += 4 * ix;
            }
            bi += 4 * iy;
          }
        } else {
          for (int ay = 0; ay < ah; ay++) {  //縦ビット位置
            for (int ax = 0; ax < aw; ax++) {  //横ビット位置
              if ((a[o + 3 * ax + (ay >> 3)] >> (~ay & 7) & 1) != 0) {  //ビットがセットされている
                prnPaintDot (4 * ax    , 4 * ay    );
                prnPaintDot (4 * ax + 1, 4 * ay    );
                prnPaintDot (4 * ax + 2, 4 * ay    );
                prnPaintDot (4 * ax + 3, 4 * ay    );
                prnPaintDot (4 * ax    , 4 * ay + 1);
                prnPaintDot (4 * ax + 1, 4 * ay + 1);
                prnPaintDot (4 * ax + 2, 4 * ay + 1);
                prnPaintDot (4 * ax + 3, 4 * ay + 1);
                prnPaintDot (4 * ax    , 4 * ay + 2);
                prnPaintDot (4 * ax + 1, 4 * ay + 2);
                prnPaintDot (4 * ax + 2, 4 * ay + 2);
                prnPaintDot (4 * ax + 3, 4 * ay + 2);
                prnPaintDot (4 * ax    , 4 * ay + 3);
                prnPaintDot (4 * ax + 1, 4 * ay + 3);
                prnPaintDot (4 * ax + 2, 4 * ay + 3);
                prnPaintDot (4 * ax + 3, 4 * ay + 3);
              }
            }
          }
        }
        prnHeadX += 4 * aw;
      }  //if 縦1倍/縦2倍
    }  //if 横1倍/横2倍
    //印字あり
    prnPrinted = true;
    //キャンバスを再描画する
    if (prnCanvas != null) {
      prnCanvas.repaint ();
    }
  }  //prn24DotBitImage

  //prnSetKanjiMode (b)
  //  b  true   ESC K  漢字モードON  (p118)
  //     false  ESC H  漢字モードOFF  (p121)
  //            ESC P  漢字モードOFF  (p121)
  public static void prnSetKanjiMode (boolean b) {
    prnKanjiMode = b;
  }  //prnSetKanjiMode

  //prnSetLeftMargin (n)
  //  ESC L n1 n2 n3  左マージン設定  (p86)
  //  左マージンをn桁にする
  //  最大数よりも大きいと無視される
  //  水平タブ位置はクリアされる
  //  単位は現在の文字の幅
  //  漢字モードのときは半角の幅
  //  横2倍は無視される
  public static void prnSetLeftMargin (int n) {
    if (prnCurrentPaper == null) {  //未給紙のとき
      prnFeedPaper ();  //給紙する
    }
    if (0 <= n && n <= 999) {
      int leftX = prnGetCharacterWidth () * n;
      if (leftX < prnMarginRightX) {
        prnMarginLeftX = leftX;
        if (prnHeadX < prnMarginLeftX) {
          prnHeadX = prnMarginLeftX;
        }
        //水平タブ位置をクリアする
        for (int k = 0; k < PRN_HORIZONTAL_ANCHOR_LIMIT; k++) {
          prnHorizontalTabAnchor[k] = 0;
        }
      }
    }
  }  //prnSetLeftMargin

  //prn48DotBitImage (a, o, n)
  //  ESC M n1 n2 d1 d2 … dk  48ドットビットイメージ  (p153)
  //  拡大しない
  //  改行ピッチが15/120[in]のとき47/360[in]にする
  //  横2倍と縦2倍が有効
  //  行からはみ出した部分は無視される
  public static void prn48DotBitImage (byte[] a, int o, int n) {
    if (prnCurrentPaper == null) {  //未給紙のとき
      prnFeedPaper ();  //給紙する
    }
    //改行ピッチが15/120[in]のとき47/360[in]にする
    if (prnLineHeight == 45) {
      prnLineHeight = 47;
    }
    if (!prnHorizontalDoubleSizeMode) {  //横1倍
      if (!prnVerticalDoubleSizeMode) {  //縦1倍
        //横1倍,縦1倍
        int aw = n;  //横nビット→nドット
        int ah = 48;  //縦48ビット→48ドット
        aw = Math.min (aw, prnMarginRightX - prnHeadX);  //横awビット→awドット
        ah = Math.min (ah, prnContentBottomY - prnHeadY);  //縦ahビット→ahドット
        int bi = prnCheckRect (    aw,     ah);
        if (0 <= bi) {
          int ix = prnIncrementX;
          int iy = prnIncrementY;
          for (int ay = 0; ay < ah; ay++) {  //縦ビット位置
            int i = bi;
            for (int ax = 0; ax < aw; ax++) {  //横ビット位置
              if ((a[o + 6 * ax + (ay >> 3)] >> (~ay & 7) & 1) != 0) {  //ビットがセットされている
                prnBitmap[i                  ] &= (byte) prnCurrentColor;
              }
              i +=     ix;
            }
            bi +=     iy;
          }
        } else {
          for (int ay = 0; ay < ah; ay++) {  //縦ビット位置
            for (int ax = 0; ax < aw; ax++) {  //横ビット位置
              if ((a[o + 6 * ax + (ay >> 3)] >> (~ay & 7) & 1) != 0) {  //ビットがセットされている
                prnPaintDot (    ax    ,     ay    );
              }
            }
          }
        }
        prnHeadX += aw;
      } else {  //縦2倍
        //横1倍,縦2倍
        int aw = n;  //横nビット→nドット
        int ah = 48;  //縦48ビット→96ドット
        aw = Math.min (aw, prnMarginRightX - prnHeadX);  //横awビット→awドット
        ah = Math.min (2 * ah, prnContentBottomY - prnHeadY) >> 1;  //縦ahビット→2*ahドット
        int bi = prnCheckRect (    aw, 2 * ah);
        if (0 <= bi) {
          int ix = prnIncrementX;
          int iy = prnIncrementY;
          for (int ay = 0; ay < ah; ay++) {  //縦ビット位置
            int i = bi;
            for (int ax = 0; ax < aw; ax++) {  //横ビット位置
              if ((a[o + 6 * ax + (ay >> 3)] >> (~ay & 7) & 1) != 0) {  //ビットがセットされている
                prnBitmap[i                  ] &= (byte) prnCurrentColor;
                prnBitmap[i          +     iy] &= (byte) prnCurrentColor;
              }
              i +=     ix;
            }
            bi += 2 * iy;
          }
        } else {
          for (int ay = 0; ay < ah; ay++) {  //縦ビット位置
            for (int ax = 0; ax < aw; ax++) {  //横ビット位置
              if ((a[o + 6 * ax + (ay >> 3)] >> (~ay & 7) & 1) != 0) {  //ビットがセットされている
                prnPaintDot (    ax    , 2 * ay    );
                prnPaintDot (    ax    , 2 * ay + 1);
              }
            }
          }
        }
        prnHeadX += aw;
      }  //if 縦1倍/縦2倍
    } else {  //横2倍
      if (!prnVerticalDoubleSizeMode) {  //縦1倍
        //横2倍,縦1倍
        int aw = n;  //横nビット→2*nドット
        int ah = 48;  //縦48ビット→48ドット
        aw = Math.min (2 * aw, prnMarginRightX - prnHeadX) >> 1;  //横awビット→2*awドット
        ah = Math.min (ah, prnContentBottomY - prnHeadY);  //縦ahビット→ahドット
        int bi = prnCheckRect (2 * aw,     ah);
        if (0 <= bi) {
          int ix = prnIncrementX;
          int iy = prnIncrementY;
          for (int ay = 0; ay < ah; ay++) {  //縦ビット位置
            int i = bi;
            for (int ax = 0; ax < aw; ax++) {  //横ビット位置
              if ((a[o + 6 * ax + (ay >> 3)] >> (~ay & 7) & 1) != 0) {  //ビットがセットされている
                prnBitmap[i                  ] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix         ] &= (byte) prnCurrentColor;
              }
              i += 2 * ix;
            }
            bi +=     iy;
          }
        } else {
          for (int ay = 0; ay < ah; ay++) {  //縦ビット位置
            for (int ax = 0; ax < aw; ax++) {  //横ビット位置
              if ((a[o + 6 * ax + (ay >> 3)] >> (~ay & 7) & 1) != 0) {  //ビットがセットされている
                prnPaintDot (2 * ax    ,     ay    );
                prnPaintDot (2 * ax + 1,     ay    );
              }
            }
          }
        }
        prnHeadX += 2 * aw;
      } else {  //縦2倍
        //横2倍,縦2倍
        int aw = n;  //横nビット→2*nドット
        int ah = 24;  //縦48ビット→96ドット
        aw = Math.min (2 * aw, prnMarginRightX - prnHeadX) >> 1;  //横awビット→2*awドット
        ah = Math.min (2 * ah, prnContentBottomY - prnHeadY) >> 1;  //縦ahビット→2*ahドット
        int bi = prnCheckRect (2 * aw, 2 * ah);
        if (0 <= bi) {
          int ix = prnIncrementX;
          int iy = prnIncrementY;
          for (int ay = 0; ay < ah; ay++) {  //縦ビット位置
            int i = bi;
            for (int ax = 0; ax < aw; ax++) {  //横ビット位置
              if ((a[o + 6 * ax + (ay >> 3)] >> (~ay & 7) & 1) != 0) {  //ビットがセットされている
                prnBitmap[i                  ] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix         ] &= (byte) prnCurrentColor;
                prnBitmap[i          +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix +     iy] &= (byte) prnCurrentColor;
              }
              i += 2 * ix;
            }
            bi += 2 * iy;
          }
        } else {
          for (int ay = 0; ay < ah; ay++) {  //縦ビット位置
            for (int ax = 0; ax < aw; ax++) {  //横ビット位置
              if ((a[o + 6 * ax + (ay >> 3)] >> (~ay & 7) & 1) != 0) {  //ビットがセットされている
                prnPaintDot (2 * ax    , 2 * ay    );
                prnPaintDot (2 * ax + 1, 2 * ay    );
                prnPaintDot (2 * ax    , 2 * ay + 1);
                prnPaintDot (2 * ax + 1, 2 * ay + 1);
              }
            }
          }
        }
        prnHeadX += 2 * aw;
      }  //if 縦1倍/縦2倍
    }  //if 横1倍/横2倍
    //印字あり
    prnPrinted = true;
    //キャンバスを再描画する
    if (prnCanvas != null) {
      prnCanvas.repaint ();
    }
  }  //prn48DotBitImage

  //prnRepeatCharacter (n, d)
  //  ESC N n1 n2 n3 d  連続文字  (p162)
  //  文字dをn回印字する
  //  制御コードは無視する
  public static void prnRepeatCharacter (int n, int d) {
    if (prnCurrentPaper == null) {  //未給紙のとき
      prnFeedPaper ();  //給紙する
    }
    if (0x20 <= d) {  //制御コードでない
      for (int i = 0; i < n; i++) {
        prnPrintCharacter (d);  //1文字印字
      }
    }
  }  //prnRepeatCharacter

  //prnSetSmallCharacterMode ()
  //  ESC Q  縮小文字設定  (p103)
  //  縮小文字(1/17[in])にする
  public static void prnSetSmallCharacterMode () {
    prnCharacterType = PRN_SMALL;  //縮小文字
  }  //prnSetSmallCharacterMode

  //prnSetPicaCharacterMode ()
  //  ESC R  パイカ文字設定  (p101)
  //  パイカ文字(1/10[in])にする
  //  https://en.wikipedia.org/wiki/Typewriter#Character_sizes
  public static void prnSetPicaCharacterMode () {
    prnCharacterType = PRN_PICA;  //パイカ文字
  }  //prnSetPicaCharacterMode

  //prnRepeat8DotBitImage (n, d)
  //  ESC V n1 n2 n3 n4 d  連続8ドットビットメージ  (p155)
  public static void prnRepeat8DotBitImage (int n, int d) {
    if (prnCurrentPaper == null) {  //未給紙のとき
      prnFeedPaper ();  //給紙する
    }
    //!!!
  }  //prnRepeat8DotBitImage

  //prnRepeat16DotBitImage (n, d)
  //  ESC W n1 n2 n3 n4 d1 d2  連続16ドットビットメージ  (p156)
  public static void prnRepeat16DotBitImage (int n, int d) {
    if (prnCurrentPaper == null) {  //未給紙のとき
      prnFeedPaper ();  //給紙する
    }
    //改行ピッチが15/120[in]のとき16/120[in]にする
    if (prnLineHeight == 45) {
      prnLineHeight = 48;
    }
    if (!prnHorizontalDoubleSizeMode) {  //横1倍
      if (!prnVerticalDoubleSizeMode) {  //縦1倍
        //横1倍,縦1倍
        int aw = n;  //横nビット→3*nドット
        int ah = 16;  //縦16ビット→48ドット
        aw = Math.min (3 * aw, prnMarginRightX - prnHeadX) / 3;  //横awビット→3*awドット
        ah = Math.min (3 * ah, prnContentBottomY - prnHeadY) / 3;  //縦ahビット→3*ahドット
        int bi = prnCheckRect (3 * aw, 3 * ah);
        if (0 <= bi) {
          int ix = prnIncrementX;
          int iy = prnIncrementY;
          for (int ay = 0; ay < ah; ay++) {  //縦ビット位置
            int i = bi;
            for (int ax = 0; ax < aw; ax++) {  //横ビット位置
              if ((d >> (~ay & 15) & 1) != 0) {  //ビットがセットされている
                prnBitmap[i                  ] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix         ] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix         ] &= (byte) prnCurrentColor;
                prnBitmap[i          +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i          + 2 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix + 2 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix + 2 * iy] &= (byte) prnCurrentColor;
              }
              i += 3 * ix;
            }
            bi += 3 * iy;
          }
        } else {
          for (int ay = 0; ay < ah; ay++) {  //縦ビット位置
            for (int ax = 0; ax < aw; ax++) {  //横ビット位置
              if ((d >> (~ay & 15) & 1) != 0) {  //ビットがセットされている
                prnPaintDot (3 * ax    , 3 * ay    );
                prnPaintDot (3 * ax + 1, 3 * ay    );
                prnPaintDot (3 * ax + 2, 3 * ay    );
                prnPaintDot (3 * ax    , 3 * ay + 1);
                prnPaintDot (3 * ax + 1, 3 * ay + 1);
                prnPaintDot (3 * ax + 2, 3 * ay + 1);
                prnPaintDot (3 * ax    , 3 * ay + 2);
                prnPaintDot (3 * ax + 1, 3 * ay + 2);
                prnPaintDot (3 * ax + 2, 3 * ay + 2);
              }
            }
          }
        }
        prnHeadX += 3 * aw;
      } else {  //縦2倍
        //横1倍,縦2倍
        int aw = n;  //横nビット→3*nドット
        int ah = 16;  //縦16ビット→96ドット
        aw = Math.min (3 * aw, prnMarginRightX - prnHeadX) / 3;  //横awビット→3*awドット
        ah = Math.min (6 * ah, prnContentBottomY - prnHeadY) / 6;  //縦ahビット→6*ahドット
        int bi = prnCheckRect (3 * aw, 6 * ah);
        if (0 <= bi) {
          int ix = prnIncrementX;
          int iy = prnIncrementY;
          for (int ay = 0; ay < ah; ay++) {  //縦ビット位置
            int i = bi;
            for (int ax = 0; ax < aw; ax++) {  //横ビット位置
              if ((d >> (~ay & 15) & 1) != 0) {  //ビットがセットされている
                prnBitmap[i                  ] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix         ] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix         ] &= (byte) prnCurrentColor;
                prnBitmap[i          +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i          + 2 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix + 2 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix + 2 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i          + 3 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix + 3 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix + 3 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i          + 4 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix + 4 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix + 4 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i          + 5 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix + 5 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix + 5 * iy] &= (byte) prnCurrentColor;
              }
              i += 3 * ix;
            }
            bi += 6 * iy;
          }
        } else {
          for (int ay = 0; ay < ah; ay++) {  //縦ビット位置
            for (int ax = 0; ax < aw; ax++) {  //横ビット位置
              if ((d >> (~ay & 15) & 1) != 0) {  //ビットがセットされている
                prnPaintDot (3 * ax    , 6 * ay    );
                prnPaintDot (3 * ax + 1, 6 * ay    );
                prnPaintDot (3 * ax + 2, 6 * ay    );
                prnPaintDot (3 * ax    , 6 * ay + 1);
                prnPaintDot (3 * ax + 1, 6 * ay + 1);
                prnPaintDot (3 * ax + 2, 6 * ay + 1);
                prnPaintDot (3 * ax    , 6 * ay + 2);
                prnPaintDot (3 * ax + 1, 6 * ay + 2);
                prnPaintDot (3 * ax + 2, 6 * ay + 2);
                prnPaintDot (3 * ax    , 6 * ay + 3);
                prnPaintDot (3 * ax + 1, 6 * ay + 3);
                prnPaintDot (3 * ax + 2, 6 * ay + 3);
                prnPaintDot (3 * ax    , 6 * ay + 4);
                prnPaintDot (3 * ax + 1, 6 * ay + 4);
                prnPaintDot (3 * ax + 2, 6 * ay + 4);
                prnPaintDot (3 * ax    , 6 * ay + 5);
                prnPaintDot (3 * ax + 1, 6 * ay + 5);
                prnPaintDot (3 * ax + 2, 6 * ay + 5);
              }
            }
          }
        }
        prnHeadX += 3 * aw;
      }  //if 縦1倍/縦2倍
    } else {  //横2倍
      if (!prnVerticalDoubleSizeMode) {  //縦1倍
        //横2倍,縦1倍
        int aw = n;  //横nビット→6*nドット
        int ah = 16;  //縦16ビット→48ドット
        aw = Math.min (6 * aw, prnMarginRightX - prnHeadX) / 6;  //横awビット→6*awドット
        ah = Math.min (3 * ah, prnContentBottomY - prnHeadY) / 3;  //縦ahビット→3*ahドット
        int bi = prnCheckRect (6 * aw, 3 * ah);
        if (0 <= bi) {
          int ix = prnIncrementX;
          int iy = prnIncrementY;
          for (int ay = 0; ay < ah; ay++) {  //縦ビット位置
            int i = bi;
            for (int ax = 0; ax < aw; ax++) {  //横ビット位置
              if ((d >> (~ay & 15) & 1) != 0) {  //ビットがセットされている
                prnBitmap[i                  ] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix         ] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix         ] &= (byte) prnCurrentColor;
                prnBitmap[i + 3 * ix         ] &= (byte) prnCurrentColor;
                prnBitmap[i + 4 * ix         ] &= (byte) prnCurrentColor;
                prnBitmap[i + 5 * ix         ] &= (byte) prnCurrentColor;
                prnBitmap[i          +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 3 * ix +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 4 * ix +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 5 * ix +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i          + 2 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix + 2 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix + 2 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 3 * ix + 2 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 4 * ix + 2 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 5 * ix + 2 * iy] &= (byte) prnCurrentColor;
              }
              i += 6 * ix;
            }
            bi += 3 * iy;
          }
        } else {
          for (int ay = 0; ay < ah; ay++) {  //縦ビット位置
            for (int ax = 0; ax < aw; ax++) {  //横ビット位置
              if ((d >> (~ay & 15) & 1) != 0) {  //ビットがセットされている
                prnPaintDot (6 * ax    , 3 * ay    );
                prnPaintDot (6 * ax + 1, 3 * ay    );
                prnPaintDot (6 * ax + 2, 3 * ay    );
                prnPaintDot (6 * ax + 3, 3 * ay    );
                prnPaintDot (6 * ax + 4, 3 * ay    );
                prnPaintDot (6 * ax + 5, 3 * ay    );
                prnPaintDot (6 * ax    , 3 * ay + 1);
                prnPaintDot (6 * ax + 1, 3 * ay + 1);
                prnPaintDot (6 * ax + 2, 3 * ay + 1);
                prnPaintDot (6 * ax + 3, 3 * ay + 1);
                prnPaintDot (6 * ax + 4, 3 * ay + 1);
                prnPaintDot (6 * ax + 5, 3 * ay + 1);
                prnPaintDot (6 * ax    , 3 * ay + 2);
                prnPaintDot (6 * ax + 1, 3 * ay + 2);
                prnPaintDot (6 * ax + 2, 3 * ay + 2);
                prnPaintDot (6 * ax + 3, 3 * ay + 2);
                prnPaintDot (6 * ax + 4, 3 * ay + 2);
                prnPaintDot (6 * ax + 5, 3 * ay + 2);
              }
            }
          }
        }
        prnHeadX += 6 * aw;
      } else {  //縦2倍
        //横2倍,縦2倍
        int aw = n;  //横nビット→6*nドット
        int ah = 16;  //縦16ビット→96ドット
        aw = Math.min (6 * aw, prnMarginRightX - prnHeadX) / 6;  //横awビット→6*awドット
        ah = Math.min (6 * ah, prnContentBottomY - prnHeadY) / 6;  //縦ahビット→6*ahドット
        int bi = prnCheckRect (6 * aw, 6 * ah);
        if (0 <= bi) {
          int ix = prnIncrementX;
          int iy = prnIncrementY;
          for (int ay = 0; ay < ah; ay++) {  //縦ビット位置
            int i = bi;
            for (int ax = 0; ax < aw; ax++) {  //横ビット位置
              if ((short) (d << (ay & 15)) < 0) {  //ビットがセットされている
                prnBitmap[i                  ] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix         ] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix         ] &= (byte) prnCurrentColor;
                prnBitmap[i + 3 * ix         ] &= (byte) prnCurrentColor;
                prnBitmap[i + 4 * ix         ] &= (byte) prnCurrentColor;
                prnBitmap[i + 5 * ix         ] &= (byte) prnCurrentColor;
                prnBitmap[i          +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 3 * ix +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 4 * ix +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 5 * ix +     iy] &= (byte) prnCurrentColor;
                prnBitmap[i          + 2 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix + 2 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix + 2 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 3 * ix + 2 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 4 * ix + 2 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 5 * ix + 2 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i          + 3 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix + 3 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix + 3 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 3 * ix + 3 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 4 * ix + 3 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 5 * ix + 3 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i          + 4 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix + 4 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix + 4 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 3 * ix + 4 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 4 * ix + 4 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 5 * ix + 4 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i          + 5 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i +     ix + 5 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 2 * ix + 5 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 3 * ix + 5 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 4 * ix + 5 * iy] &= (byte) prnCurrentColor;
                prnBitmap[i + 5 * ix + 5 * iy] &= (byte) prnCurrentColor;
              }
              i += 6 * ix;
            }
            bi += 6 * iy;
          }
        } else {
          for (int ay = 0; ay < ah; ay++) {  //縦ビット位置
            for (int ax = 0; ax < aw; ax++) {  //横ビット位置
              if ((short) (d << (ay & 15)) < 0) {  //ビットがセットされている
                prnPaintDot (6 * ax    , 6 * ay    );
                prnPaintDot (6 * ax + 1, 6 * ay    );
                prnPaintDot (6 * ax + 2, 6 * ay    );
                prnPaintDot (6 * ax + 3, 6 * ay    );
                prnPaintDot (6 * ax + 4, 6 * ay    );
                prnPaintDot (6 * ax + 5, 6 * ay    );
                prnPaintDot (6 * ax    , 6 * ay + 1);
                prnPaintDot (6 * ax + 1, 6 * ay + 1);
                prnPaintDot (6 * ax + 2, 6 * ay + 1);
                prnPaintDot (6 * ax + 3, 6 * ay + 1);
                prnPaintDot (6 * ax + 4, 6 * ay + 1);
                prnPaintDot (6 * ax + 5, 6 * ay + 1);
                prnPaintDot (6 * ax    , 6 * ay + 2);
                prnPaintDot (6 * ax + 1, 6 * ay + 2);
                prnPaintDot (6 * ax + 2, 6 * ay + 2);
                prnPaintDot (6 * ax + 3, 6 * ay + 2);
                prnPaintDot (6 * ax + 4, 6 * ay + 2);
                prnPaintDot (6 * ax + 5, 6 * ay + 2);
                prnPaintDot (6 * ax    , 6 * ay + 3);
                prnPaintDot (6 * ax + 1, 6 * ay + 3);
                prnPaintDot (6 * ax + 2, 6 * ay + 3);
                prnPaintDot (6 * ax + 3, 6 * ay + 3);
                prnPaintDot (6 * ax + 4, 6 * ay + 3);
                prnPaintDot (6 * ax + 5, 6 * ay + 3);
                prnPaintDot (6 * ax    , 6 * ay + 4);
                prnPaintDot (6 * ax + 1, 6 * ay + 4);
                prnPaintDot (6 * ax + 2, 6 * ay + 4);
                prnPaintDot (6 * ax + 3, 6 * ay + 4);
                prnPaintDot (6 * ax + 4, 6 * ay + 4);
                prnPaintDot (6 * ax + 5, 6 * ay + 4);
                prnPaintDot (6 * ax    , 6 * ay + 5);
                prnPaintDot (6 * ax + 1, 6 * ay + 5);
                prnPaintDot (6 * ax + 2, 6 * ay + 5);
                prnPaintDot (6 * ax + 3, 6 * ay + 5);
                prnPaintDot (6 * ax + 4, 6 * ay + 5);
                prnPaintDot (6 * ax + 5, 6 * ay + 5);
              }
            }
          }
        }
        prnHeadX += 6 * aw;
      }  //if 縦1倍/縦2倍
    }  //if 横1倍/横2倍
    //印字あり
    prnPrinted = true;
    //キャンバスを再描画する
    if (prnCanvas != null) {
      prnCanvas.repaint ();
    }
  }  //prnRepeat16DotBitImage

  //prnSetUnderlineMode (b)
  //  b  true   ESC X  アンダーラインあり  (p114)
  //     false  ESC Y  アンダーラインなし  (p115)
  public static void prnSetUnderlineMode (boolean b) {
    prnUnderlineMode = b;
  }  //prnSetUnderlineMode

  //prnHorizontalMove (n)
  //  ESC \\ n1 n2  水平移動  (p99)
  //  ESC n  ドットスペース  (p123)
  //  水平方向にn/180[in]移動する
  //  -1440<=n<=1440
  public static void prnHorizontalMove (int n) {
    if (prnCurrentPaper == null) {  //未給紙のとき
      prnFeedPaper ();  //給紙する
    }
    if (-1440 <= n && n <= 1440) {
      int headX = prnHeadX + 2 * n;
      if (prnMarginLeftX <= headX && headX < prnMarginRightX) {
        prnHeadX = headX;
      }
    }
  }  //prnHorizontalMove

  //prnResetSettings ()
  //  ESC c 1  設定リセット  (p157)
  public static void prnResetSettings () {
    prnEjectPaper ();  //排紙する
    //マージン
    prnMarginLeftX = 0;
    prnMarginRightX = 2879;
    prnMarginBottomHeight = 0;
    //改行ピッチ
    prnDefaultLineHeight = 60;  //1/6[in]改行
    prnLineHeight = 60;  //1/6[in]改行
    //文字種
    prnCharacterType = PRN_PICA;  //パイカ
    //ひらがなモード
    prnHiraganaMode = false;  //カタカナ
    //スクリプトモード
    prnScriptMode = PRN_NO_SCRIPT;  //スクリプト解除
    //強調モード
    prnStrongMode = false;  //強調OFF
    //アンダーラインモード
    prnUnderlineMode = false;  //アンダーラインOFF
    //文字スタイル
    prnCharacterStyle = PRN_NORMAL_STYLE;  //標準文字
    //漢字モード
    prnKanjiMode = false;  //漢字モードOFF
    //外字データ
    if (prnGaijiData != null) {
      Arrays.fill (prnGaijiData, (byte) 0);
    }
    //縦書きモード
    prnVerticalWritingMode = false;  //横書き
    //左右スペース
    prnFullWidthLeftSpace = 2;  //全角左スペース[dot]
    prnFullWidthRightSpace = 6;  //全角右スペース[dot]
    prnHalfWidthLeftSpace = 0;  //半角左スペース[dot]
    prnHalfWidthRightSpace = 4;  //半角右スペース[dot]
    //横2倍モード
    prnHorizontalDoubleSizeMode = false;  //横2倍OFF
    prnVerticalDoubleSizeMode = false;  //縦2倍OFF
    prnKanjiHorizontalDoubleSizeMode = false;  //漢字横2倍OFF
    //水平タブ
    for (int k = 0; k < PRN_HORIZONTAL_ANCHOR_LIMIT; k++) {
      prnHorizontalTabAnchor[k] = 36 * 8 * (1 + k);
    }
    //垂直タブ
    for (int k = 0; k < PRN_VERTICAL_ANCHOR_LIMIT; k++) {
      prnVerticalTabAnchor[k] = 0;
    }
    //カラーモード
    prnColorMode = false;  //単色モード
    //書体
    prnHalfWidthFont =
      prnFullWidthFont = (prnDIPSW & PRN_DIPSW_FONT_STYLE) == 0 ? 0 : 1;
    //色
    prnCurrentColor = prnSingleColor;
    //コマンドバッファ
    prnCommandLength = 1;
    prnCommandPointer = 0;
  }  //prnResetSettings

  //prnSetHalfWidthFont (n)
  //  半角書体
  //  n  0  ローマン体
  //     1  サンセリフ体
  public static void prnSetHalfWidthFont (int n) {
    if (0 <= n && n <= 1) {
      prnHalfWidthFont = n;
    }
  }  //prnSetHalfWidthFont

  //prnSenseOutOfPaper (b)
  //  b  true   ESC p 1  用紙切れ検出有効  (p161)
  //     false  ESC p 0  用紙切れ検出無効  (p160)
  //  用紙切れ検出無効のときは用紙の下端から約12[mm]、有効のときは約60[mm]でディセレクトになる
  public static void prnSenseOutOfPaper (boolean b) {
    //!!!
  }  //prnSenseOutOfPaper

  //prnSetCharacterStyle (n)
  //  ESC q n  文字スタイル設定  (p116)
  //  n  0  標準文字
  //     1  袋文字
  //     2  影文字
  //     3  袋影文字
  public static void prnSetCharacterStyle (int n) {
    if (0 <= n && n <= 3) {
      prnCharacterStyle = n;
    }
  }  //prnSetCharacterStyle

  //prnSetScriptMode (n)
  //  n  0  ESC s 0  スクリプト解除  (p113)
  //     1  ESC s 1  スーパースクリプト設定  (p111)
  //     2  ESC s 2  サブスクリプト設定  (p112)
  public static void prnSetScriptMode (int n) {
    if (0 <= n && n <= 2) {
      prnScriptMode = n;
    }
  }  //prnSetScriptMode

  //prnSetVerticalWritingMode (b)
  //  b  true   FS J  縦書き  (p128)
  //     false  FS K  横書き  (p131)
  public static void prnSetVerticalWritingMode (boolean b) {
    prnVerticalWritingMode = b;
  }  //prnSetVerticalWritingMode

  //prnSetFullWidthLeftRightSpace (l, r)
  //  FS S n1 n2  全角左右スペース  (p124)
  public static void prnSetFullWidthLeftRightSpace (int l, int r) {
    prnFullWidthLeftSpace = l;
    prnFullWidthRightSpace = r;
  }  //prnSetFullWidthLeftRightSpace

  //prnSetHalfWidthLeftRightSpace (l, r)
  //  FS T n1 n2  半角左右スペース  (p126)
  public static void prnSetHalfWidthLeftRightSpace (int l, int r) {
    prnHalfWidthLeftSpace = l;
    prnHalfWidthRightSpace = r;
  }  //prnSetHalfWidthLeftRightSpace

  //prnSetFullWidthFont (n)
  //  全角書体
  //  n  0  明朝体
  //     1  ゴシック体
  public static void prnSetFullWidthFont (int n) {
    if (0 <= n && n <= 1) {
      prnFullWidthFont = n;
    }
  }  //prnSetFullWidthFont

  //prnSetKanjiHorizontalDoubleSizeMode (b)
  //  b  true   FS p  漢字横2倍ON  (p132)
  //     false  FS q  漢字横2倍OFF  (p133)
  public static void prnSetKanjiHorizontalDoubleSizeMode (boolean b) {
    prnKanjiHorizontalDoubleSizeMode = b;
  }  //prnSetKanjiHorizontalDoubleSizeMode



  //bi = prnCheckRect (rw, rh)
  //  矩形は印字可能範囲内か
  //  bi  印字可能範囲内のとき左上のビットマップインデックス。-1=印字可能範囲内ではない
  //  rw  矩形の幅
  //  rh  矩形の高さ
  public static int prnCheckRect (int rw, int rh) {
    int px0 = prnAliveLeftX + prnHeadX;  //左上の用紙座標
    int py0 = prnAliveTopY + prnHeadY;
    int px1 = px0 + rw - 1;  //右下の用紙座標
    int py1 = py0 + rh - 1;
    int bx0 = prnM11 * px0 + prnM12 * py0 + prnM13;  //左上のビットマップ座標
    int by0 = prnM21 * px0 + prnM22 * py0 + prnM23;
    int bx1 = prnM11 * px1 + prnM12 * py1 + prnM13;  //右下のビットマップ座標
    int by1 = prnM21 * px1 + prnM22 * py1 + prnM23;
    return (0 <= bx0 && bx0 < prnRotatedWidthDot &&
            0 <= by0 && by0 < prnRotatedHeightDot &&
            0 <= bx1 && bx1 < prnRotatedWidthDot &&
            0 <= by1 && by1 < prnRotatedHeightDot ?  //印字可能範囲内
            bx0 + prnRotatedWidthDot * by0 :  //左上のビットマップインデックス
            -1);
  }  //prnCheckRect

  //prnPaintDot (rx, ry)
  //  1ドット塗る
  //  rx  ヘッド座標
  //  ry
  public static void prnPaintDot (int rx, int ry) {
    int px = prnAliveLeftX + prnHeadX + rx;  //用紙座標
    int py = prnAliveTopY + prnHeadY + ry;
    int bx = prnM11 * px + prnM12 * py + prnM13;  //ビットマップ座標
    int by = prnM21 * px + prnM22 * py + prnM23;
    if (0 <= bx && bx < prnRotatedWidthDot &&
        0 <= by && by < prnRotatedHeightDot) {  //印字可能範囲内
      prnBitmap[bx + prnRotatedWidthDot * by] &= (byte) prnCurrentColor;  //ANDで描く
    }
  }  //prnPaintDot



  //success = prnSave ()
  //  保存
  public static boolean prnSave () {
    //ImageWriterを探す
    //  参考
    //    http://stackoverflow.com/questions/321736/how-to-set-dpi-information-in-an-image
    //    http://docs.oracle.com/javase/8/docs/api/javax/imageio/metadata/doc-files/standard_metadata.html
    int index = prnSaveName.lastIndexOf ('.');
    if (index < 0) {  //拡張子がないとき
      prnSaveName += ".png";  //pngにする
      index = prnSaveName.lastIndexOf ('.');
    }
    String full = prnSavePath + File.separator + prnSaveName;  //フルパスのファイル名
    File imageFile = new File (full);
    if (imageFile.isFile ()) {  //既に存在するとき
      //ファイルが既に存在するとき読み取り専用属性が付いていないことを確認する
      //  Windows10のエクスプローラの読み取り専用属性は保護機能ではないので無視して上書きできる
      //  canWrite()は読み取り専用属性が付いているとfalseを返す
      //  ファイルが存在するときcanWrite()がtrueでなければ上書きしてはならない
      //  canWrite()は「存在して書き込める」なので!canWrite()は「存在しないか書き込めない」であることに注意
      if (!imageFile.canWrite ()) {  //既に存在するが書き込めないとき
        XEiJ.pnlExitFullScreen (true);
        JOptionPane.showMessageDialog (
          null,
          full + (Multilingual.mlnJapanese ?
                  "\nは既に存在します。上書きできません。" :
                  "\nalreay exists. You cannot overwrite it."));
        return false;
      }
      //上書きしてよいか確認する
      XEiJ.pnlExitFullScreen (true);
      if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog (
        null,
        full + (Multilingual.mlnJapanese ?
                "\nは既に存在します。上書きしますか？" :
                "\nalreay exists. Do you want to overwrite it?"),
        Multilingual.mlnJapanese ? "ファイルの上書きの確認" : "Confirmation of overwriting file",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.PLAIN_MESSAGE)) {
        return false;
      }
    }
    for (Iterator<ImageWriter> iterator = ImageIO.getImageWritersBySuffix (prnSaveName.substring (index + 1));
         //拡張子に対応するImageWriterがないときは空のIteratorを返すのでiteratorはnullにならない
         iterator.hasNext (); ) {
      ImageWriter imageWriter = iterator.next ();
      ImageWriteParam imageWriteParam = imageWriter.getDefaultWriteParam ();
      if (false) {
        if (imageWriteParam.canWriteCompressed ()) {
          imageWriteParam.setCompressionMode (ImageWriteParam.MODE_EXPLICIT);
          imageWriteParam.setCompressionQuality (0.75F);
        }
      }
      IIOMetadata imageMetadata = imageWriter.getDefaultImageMetadata (
        ImageTypeSpecifier.createFromBufferedImageType (prnImage.getType ()),
        imageWriteParam);
      if (imageMetadata.isStandardMetadataFormatSupported ()) {
        //解像度を設定する
        //  PNGファイルの仕様では解像度の単位は[dot/m]だが
        //  javax_imageio_1.0のHorizontalPixelSizeとVerticalPixelSizeの単位は[dot/mm]なので
        //    360[dot/in] → 360/25.4[dot/mm]
        //  とする
        //  これを[dot/m]にしてしまうとペイントのプロパティで360000 DPIなどと表示されておかしなことになる
        IIOMetadataNode rootNode = new IIOMetadataNode ("javax_imageio_1.0");
        IIOMetadataNode dimensionNode = new IIOMetadataNode ("Dimension");
        IIOMetadataNode horizontalPixelSizeNode = new IIOMetadataNode("HorizontalPixelSize");
        IIOMetadataNode verticalPixelSizeNode = new IIOMetadataNode ("VerticalPixelSize");
        horizontalPixelSizeNode.setAttribute ("value", String.valueOf (360.0 / 25.4));
        verticalPixelSizeNode.setAttribute ("value", String.valueOf (360.0 / 25.4));
        dimensionNode.appendChild (horizontalPixelSizeNode);
        dimensionNode.appendChild (verticalPixelSizeNode);
        rootNode.appendChild (dimensionNode);
        try {
          imageMetadata.mergeTree ("javax_imageio_1.0", rootNode);
        } catch (IIOInvalidTreeException iioite) {
          continue;
        }
        imageFile.delete ();
        try (ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream (imageFile)) {
          imageWriter.setOutput (imageOutputStream);
          imageWriter.write (imageMetadata, new IIOImage (prnImage, null, imageMetadata), imageWriteParam);
        } catch (IOException ioe) {
          continue;
        }
        return true;
      }
    }  //for iterator
    XEiJ.pnlExitFullScreen (true);
    JOptionPane.showMessageDialog (
      null,
      full + (Multilingual.mlnJapanese ?
              "\nを更新できませんでした。" :
              "\nwas not updated")
      );
    return false;
  }  //prnSave



}  //class PrinterPort



