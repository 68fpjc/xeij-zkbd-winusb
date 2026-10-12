//========================================================================================
//  MC68901.java
//    en:MFP -- Multi-Function Peripheral
//    ja:MFP -- マルチファンクションペリフェラル
//  Copyright (C) 2003-2026 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System

public class MC68901 {

  public static final boolean MFP_DELAYED_INTERRUPT = true;  //true=MFPの割り込み要求を1命令遅延させる

  //レジスタ
  public static final int MFP_GPIP_DATA = 0x00e88001;  //GPIPデータレジスタ
  public static final int MFP_AER       = 0x00e88003;  //アクティブエッジレジスタ。各ビット0=1→0,1=0→1
  public static final int MFP_DDR       = 0x00e88005;  //データディレクションレジスタ。各ビット0=入力,1=出力。全ビット入力なので0x00に固定
  public static final int MFP_IERA      = 0x00e88007;  //割り込みイネーブルレジスタA。各ビット0=ディセーブル,1=イネーブル
  public static final int MFP_IERB      = 0x00e88009;  //割り込みイネーブルレジスタB。各ビット0=ディセーブル,1=イネーブル
  public static final int MFP_IPRA      = 0x00e8800b;  //割り込みペンディングレジスタA
  public static final int MFP_IPRB      = 0x00e8800d;  //割り込みペンディングレジスタB
  public static final int MFP_ISRA      = 0x00e8800f;  //割り込みインサービスレジスタA
  public static final int MFP_ISRB      = 0x00e88011;  //割り込みインサービスレジスタB
  public static final int MFP_IMRA      = 0x00e88013;  //割り込みマスクレジスタA
  public static final int MFP_IMRB      = 0x00e88015;  //割り込みマスクレジスタB
  public static final int MFP_VECTOR    = 0x00e88017;  //ベクタレジスタ
  public static final int MFP_TACR      = 0x00e88019;  //タイマAコントロールレジスタ
  public static final int MFP_TBCR      = 0x00e8801b;  //タイマBコントロールレジスタ
  public static final int MFP_TCDCR     = 0x00e8801d;  //タイマC,Dコントロールレジスタ
  //  タイマのカウンタに$00を書き込んで1/200プリスケールで開始したとき、カウンタから読み出される値は最初の50μs間は$00、次の50μs間は$FF
  public static final int MFP_TADR      = 0x00e8801f;  //タイマAデータレジスタ
  public static final int MFP_TBDR      = 0x00e88021;  //タイマBデータレジスタ
  public static final int MFP_TCDR      = 0x00e88023;  //タイマCデータレジスタ
  public static final int MFP_TDDR      = 0x00e88025;  //タイマDデータレジスタ
  public static final int MFP_SYNC_CHAR = 0x00e88027;  //同期キャラクタレジスタ
  public static final int MFP_UCR       = 0x00e88029;  //USARTコントロールレジスタ
  public static final int MFP_RSR       = 0x00e8802b;  //受信ステータスレジスタ
  public static final int MFP_TSR       = 0x00e8802d;  //送信ステータスレジスタ
  public static final int MFP_UDR       = 0x00e8802f;  //USARTデータレジスタ

  //GPIP
  //  GPIP7 H-SYNC
  //    1  水平帰線期間
  //    0  水平表示期間(水平バックポーチ／水平映像期間／水平フロントポーチ)
  //  GPIP6 CRTC IRQ
  //    0  指定されたラスタ
  //    1  その他のラスタ
  //    遷移は直前の水平フロントポーチの開始位置付近、V-DISPも遷移するときはその直前
  //    0番は垂直帰線期間の最初のラスタ
  //      CRTC R06+1==CRTC R09のとき、指定されたラスタの開始(CRTC IRQ 1→0)と垂直映像期間の開始(V-DISP 0→1)が同じラスタになる
  //  GPIP5
  //    RTCのCLKOUT(1Hz)が接続されることになっていたが欠番になった
  //  GPIP4 V-DISP
  //    1  垂直映像期間
  //    0  垂直フロントポーチ／垂直帰線期間／垂直バックポーチ
  //    遷移は直前の水平フロントポーチの開始位置付近
  public static final int MFP_GPIP_ALARM_LEVEL  = 0;  //0=ALARMによる電源ON
  public static final int MFP_GPIP_EXPWON_LEVEL = 1;  //0=EXPWONによる電源ON
  public static final int MFP_GPIP_POWER_LEVEL  = 2;  //0=POWERスイッチON
  public static final int MFP_GPIP_OPMIRQ_LEVEL = 3;  //0=OPM割り込み要求あり
  public static final int MFP_GPIP_VDISP_LEVEL  = 4;  //1=垂直映像期間,0=それ以外
  public static final int MFP_GPIP_RINT_LEVEL   = 6;  //0=指定されたラスタ,1=それ以外
  public static final int MFP_GPIP_HSYNC_LEVEL  = 7;  //0=水平表示期間,1=水平帰線期間

  //GPIPマスク
  public static final int MFP_GPIP_ALARM_MASK  = 1 << MFP_GPIP_ALARM_LEVEL;
  public static final int MFP_GPIP_EXPWON_MASK = 1 << MFP_GPIP_EXPWON_LEVEL;
  public static final int MFP_GPIP_POWER_MASK  = 1 << MFP_GPIP_POWER_LEVEL;
  public static final int MFP_GPIP_OPMIRQ_MASK = 1 << MFP_GPIP_OPMIRQ_LEVEL;
  public static final int MFP_GPIP_VDISP_MASK  = 1 << MFP_GPIP_VDISP_LEVEL;
  public static final int MFP_GPIP_RINT_MASK   = 1 << MFP_GPIP_RINT_LEVEL;
  public static final int MFP_GPIP_HSYNC_MASK  = 1 << MFP_GPIP_HSYNC_LEVEL;

  //割り込みレベル
  public static final int MFP_ALARM_LEVEL        =  0;  //40:MFP B0 GPIP0 RTC ALARM
  public static final int MFP_EXPWON_LEVEL       =  1;  //41:MFP B1 GPIP1 EXPWON
  public static final int MFP_POWER_LEVEL        =  2;  //42:MFP B2 GPIP2 POWER
  public static final int MFP_OPMIRQ_LEVEL       =  3;  //43:MFP B3 GPIP3 FM音源
  public static final int MFP_TIMER_D_LEVEL      =  4;  //44:MFP B4 Timer-D バックグラウンドスレッド
  public static final int MFP_TIMER_C_LEVEL      =  5;  //45:MFP B5 Timer-C マウス処理,テキストカーソル,FDDモーターOFF,稼働時間計測
  public static final int MFP_VDISP_LEVEL        =  6;  //46:MFP B6 GPIP4 V-DISP
  public static final int MFP_TIMER_B_LEVEL      =  8;  //48:MFP A0 Timer-B キーボードシリアルクロック
  public static final int MFP_OUTPUT_ERROR_LEVEL =  9;  //49:MFP A1 キーボードシリアル出力エラー
  public static final int MFP_OUTPUT_EMPTY_LEVEL = 10;  //4A:MFP A2 キーボードシリアル出力空
  public static final int MFP_INPUT_ERROR_LEVEL  = 11;  //4B:MFP A3 キーボードシリアル入力エラー
  public static final int MFP_INPUT_FULL_LEVEL   = 12;  //4C:MFP A4 キーボードシリアル入力あり
  public static final int MFP_TIMER_A_LEVEL      = 13;  //4D:MFP A5 Timer-A(V-DISPイベントカウント)
  public static final int MFP_RINT_LEVEL         = 14;  //4E:MFP A6 GPIP6 CRTC IRQ
  public static final int MFP_HSYNC_LEVEL        = 15;  //4F:MFP A7 GPIP7 H-SYNC

  //割り込みマスク
  public static final int MFP_ALARM_MASK        = 1 << MFP_ALARM_LEVEL;
  public static final int MFP_EXPWON_MASK       = 1 << MFP_EXPWON_LEVEL;
  public static final int MFP_POWER_MASK        = 1 << MFP_POWER_LEVEL;
  public static final int MFP_OPMIRQ_MASK       = 1 << MFP_OPMIRQ_LEVEL;
  public static final int MFP_TIMER_D_MASK      = 1 << MFP_TIMER_D_LEVEL;
  public static final int MFP_TIMER_C_MASK      = 1 << MFP_TIMER_C_LEVEL;
  public static final int MFP_VDISP_MASK        = 1 << MFP_VDISP_LEVEL;
  public static final int MFP_TIMER_B_MASK      = 1 << MFP_TIMER_B_LEVEL;
  public static final int MFP_OUTPUT_ERROR_MASK = 1 << MFP_OUTPUT_ERROR_LEVEL;
  public static final int MFP_OUTPUT_EMPTY_MASK = 1 << MFP_OUTPUT_EMPTY_LEVEL;
  public static final int MFP_INPUT_ERROR_MASK  = 1 << MFP_INPUT_ERROR_LEVEL;
  public static final int MFP_INPUT_FULL_MASK   = 1 << MFP_INPUT_FULL_LEVEL;
  public static final int MFP_TIMER_A_MASK      = 1 << MFP_TIMER_A_LEVEL;
  public static final int MFP_RINT_MASK         = 1 << MFP_RINT_LEVEL;
  public static final int MFP_HSYNC_MASK        = 1 << MFP_HSYNC_LEVEL;

  public static final long MFP_OSC_FREQ = 4000000L;  //MFPのオシレータの周波数

  //タイマのプリスケール
  public static final long MFP_DELTA[] = {
    XEiJ.FAR_FUTURE,                     //0:カウント禁止
    4 * XEiJ.TMR_FREQ / MFP_OSC_FREQ,    //1:1/4プリスケール(1μs)
    10 * XEiJ.TMR_FREQ / MFP_OSC_FREQ,   //2:1/10プリスケール(2.5μs)
    16 * XEiJ.TMR_FREQ / MFP_OSC_FREQ,   //3:1/16プリスケール(4μs)
    50 * XEiJ.TMR_FREQ / MFP_OSC_FREQ,   //4:1/50プリスケール(12.5μs)
    64 * XEiJ.TMR_FREQ / MFP_OSC_FREQ,   //5:1/64プリスケール(16μs)
    100 * XEiJ.TMR_FREQ / MFP_OSC_FREQ,  //6:1/100プリスケール(25μs)
    200 * XEiJ.TMR_FREQ / MFP_OSC_FREQ,  //7:1/200プリスケール(50μs)
  };

  //MFP_UDRの入力データのキュー
  public static final int MFP_UDR_QUEUE_BITS = 4;  //キューの長さのビット数
  public static final int MFP_UDR_QUEUE_SIZE = 1 << MFP_UDR_QUEUE_BITS;  //キューの長さ
  public static final int MFP_UDR_QUEUE_MASK = MFP_UDR_QUEUE_SIZE - 1;  //キューの長さのマスク

  //GPIPデータレジスタ
  //  値は0または該当するビットのマスク
  //  ゼロ拡張
  public static int mfpGpipAlarm;  //0またはMFP_GPIP_ALARM_MASK
  public static int mfpGpipExpwon;  //0またはMFP_GPIP_EXPWON_MASK
  public static int mfpGpipPower;  //0またはMFP_GPIP_POWER_MASK
  public static int mfpGpipOpmirq;  //0またはMFP_GPIP_OPMIRQ_MASK
  public static int mfpGpipVdisp;  //0またはMFP_GPIP_VDISP_MASK
  public static int mfpGpipRint;  //0またはMFP_GPIP_RINT_MASK
  public static int mfpGpipHsync;  //0またはMFP_GPIP_HSYNC_MASK

  //レジスタ
  //  ゼロ拡張
  public static int mfpAer;  //アクティブエッジレジスタ
  public static int mfpIer;  //割り込みイネーブルレジスタ(上位バイトがMFP_IERA、下位バイトがMFP_IERB)
  public static int mfpImr;  //割り込みマスクレジスタ(上位バイトがMFP_IMRA、下位バイトがMFP_IMRB)
  public static int mfpVectorHigh;  //ベクタレジスタのビット7～4
  public static int mfpTaPrescale;  //タイマAのプリスケール(0～7、0はカウント禁止)
  public static int mfpTbPrescale;  //タイマBのプリスケール(0～7、0はカウント禁止)
  public static int mfpTcPrescale;  //タイマCのプリスケール(0～7、0はカウント禁止)
  public static int mfpTdPrescale;  //タイマDのプリスケール(0～7、0はカウント禁止)
  public static boolean mfpTaEventcount;  //タイマAイベントカウントモード
  public static boolean mfpTbEventcount;  //タイマBイベントカウントモード
  public static int mfpTaInitial;  //タイマAの初期値(1～256)
  public static int mfpTbInitial;  //タイマBの初期値(1～256)
  public static int mfpTcInitial;  //タイマCの初期値(1～256)
  public static int mfpTdInitial;  //タイマDの初期値(1～256)
  //  タイマの現在の値はイベントカウントモードのときとディレイモードでカウンタが停止しているときだけ有効
  //    ディレイモードでカウンタが動作しているときは読み出した時点の値を計算する
  //    Current=Initial-((ClockTime-Start)/Delta)%Initial
  public static int mfpTaCurrent;  //タイマAの現在の値(1～256)
  public static int mfpTbCurrent;  //タイマBの現在の値(1～256)
  public static int mfpTcCurrent;  //タイマCの現在の値(1～256)
  public static int mfpTdCurrent;  //タイマDの現在の値(1～256)
  public static boolean mfpTbKeyboardOn;  //TBCRに$01、TBDRに$0Dが設定されている。キーボードと通信できる
  //  タイマデータレジスタ
  //    イベントカウントモードまたはディレイモードで動作中のとき
  //      本来は
  //        MFPのDSピンの立ち上がりでカウンタの値をデータレジスタにコピーする
  //        データレジスタを読み出すときそのまま返す
  //      X68000のとき
  //        ┃MPUのLDSがDSに接続されている
  //        ┃命令フェッチでもコピーされるのでカウンタとデータレジスタを区別する必要がない
  //        データレジスタを読み出すとき現在のカウンタの値を求めて返す
  //      X68030のとき
  //        ┃YUKIのIOLDSがDSに接続されている
  //        ┃IOLDSは8bitのI/Oポート以外では立ち上がらないのでコピーが間欠的になる
  //        ┃2回の読み出しの間に8bitのI/Oポートにアクセスしなければ、
  //        ┃前回の読み出しが最後のIOLDSの立ち上がりになり、
  //        ┃前回読み出そうとしたときの値が1回遅れで読み出される
  //        IOLDSの立ち上がりの時刻を記録する
  //        データレジスタを読み出すとき最後に記録された時刻のカウンタの値を求めて返す
  //    停止中のとき
  //      停止したときカウンタの値を求めてデータレジスタにコピーする
  //      データレジスタを読み出すときそのまま返す
  //  IOLDSの立ち上がり
  //    現在の時刻をIOLDSの立ち上がりの時刻として記録する
  //    イベントカウントモードのとき
  //      カウンタの値をデータレジスタにコピーする
  public static int mfpTaData;  //タイマAデータレジスタ
  public static int mfpTbData;  //タイマBデータレジスタ
  public static int mfpTcData;  //タイマCデータレジスタ
  public static int mfpTdData;  //タイマDデータレジスタ
  public static long mfpIoldsTime;  //IOLDSの立ち上がりの時刻

  public static int mfpUCR;  //USARTコントロールレジスタ
  //  |CLK|CL1|CL0|ST1|ST0|PE|E/O|-|
  //   |   |       |       |  |
  //   |   |       |       |  パリティの奇遇
  //   |   |       |       |  1=偶数
  //   |   |       |       |  0=奇数
  //   |   |       |       パリティの有無
  //   |   |       |       1=パリティあり
  //   |   |       |       0=パリティなし
  //   |   |       スタートストップ
  //   |   |       00=同期
  //   |   |       01=非同期 スタート1bit ストップ1bit
  //   |   |       10=非同期 スタート1bit ストップ1.5bit
  //   |   |       11=非同期 スタート1bit ストップ2bit
  //   |   キャラクタ長
  //   |   00=8bit
  //   |   01=7bit
  //   |   10=6bit
  //   |   11=5bit
  //   クロックモード
  //   0  1/16
  //   1  1/1
  public static int mfpRSR;  //受信ステータスレジスタ
  //  |BF|OE|PE|FE|F/S OR B|M/CIP|SS|RE|
  //   |  |  |  |  |        |     |  |
  //   |  |  |  |  |        |     |  受信イネーブル
  //   |  |  |  |  |        |     |  1=受信可
  //   |  |  |  |  |        |     Synchronous Strip Enable
  //   |  |  |  |  |        Match/Character in Progress
  //   |  |  |  |  Found/Search or Break Detect
  //   |  |  |  フレームエラー
  //   |  |  |  1=フレームエラーあり
  //   |  |  パリティエラー
  //   |  |  1=パリティエラーあり
  //   |  オーバーランエラー
  //   |  1=オーバーランエラーあり。UDRがリードされる前に次のデータが来た
  //   バッファフル
  //   1=受信バッファフル。次のデータをUDRからリードできる
  //   0=受信バッファ空
  public static int mfpTSR;  //送信ステータスレジスタ
  //  |BE|UE|AT|END|B|H|L|TE|
  //   |  |  |  |   | |   |
  //   |  |  |  |   | |   送信イネーブル
  //   |  |  |  |   | |   1=送信可
  //   |  |  |  |   | High and Low
  //   |  |  |  |   Break
  //   |  |  |  End of transmission
  //   |  |  Auto-Turnaround
  //   |  アンダーランエラー
  //   バッファエンプティ
  //   1=送信バッファ空。次のデータをUDRへライトできる
  //   0=送信バッファフル。次のデータをUDRへライトできない
  public static long mfpTSRBufferFullTime;  //送信バッファフル時間
  public static long mfpTSRBufferFullEnd;  //送信バッファフル終了時刻

  //割り込み
  //  割り込み要求カウンタと割り込み受付カウンタの値が異なるときMFP_IPRA,MFP_IPRBの該当ビットがONになる
  //  MFP_IERA,MFP_IERBの該当ビットに0が書き込まれたときMFP_IPRA,MFP_IPRBの該当ビットを0にするためrequestをacknowledgedにコピーする
  public static final int[] mfpInnerRequest = new int[16];  //割り込み要求カウンタ
  public static final int[] mfpInnerAcknowledged = new int[16];  //割り込み受付カウンタ
  public static final boolean[] mfpInnerInService = new boolean[16];  //割り込み処理中のときtrue
  public static int mfpInnerLevel;  //割り込み処理中のレベル

  //タイマ
  //  すべてディレイモードでカウンタが動作しているときだけ有効
  public static long mfpTaStart;  //タイマAの初期値からスタートしたときのクロック
  public static long mfpTbStart;  //タイマBの初期値からスタートしたときのクロック
  public static long mfpTcStart;  //タイマCの初期値からスタートしたときのクロック
  public static long mfpTdStart;  //タイマDの初期値からスタートしたときのクロック
  public static long mfpTaDelta;  //タイマAのプリスケールに対応する1カウントあたりの時間
  public static long mfpTbDelta;  //タイマBのプリスケールに対応する1カウントあたりの時間
  public static long mfpTcDelta;  //タイマCのプリスケールに対応する1カウントあたりの時間
  public static long mfpTdDelta;  //タイマDのプリスケールに対応する1カウントあたりの時間
  public static long mfpTaClock;  //タイマAが次に割り込む時刻
  public static long mfpTbClock;  //タイマBが次に割り込む時刻
  public static long mfpTcClock;  //タイマCが次に割り込む時刻
  public static long mfpTdClock;  //タイマDが次に割り込む時刻

  //MFP_UDRの入力データのキュー
  //  入力データはゼロ拡張すること
  //  キー入力を取りこぼさないためにキューを使う
  //  read==writeのときキューは空
  //  write+1==readのときキューは満杯
  public static final int[] mfpUdrQueueArray = new int[MFP_UDR_QUEUE_SIZE];  //入力データ
  public static volatile int mfpUdrQueueRead;  //最後に読み出したデータの位置
  public static volatile int mfpUdrQueueWrite;  //最後に書き込んだデータの位置

  //mfpInit ()
  //  MFPを初期化する
  public static void mfpInit () {
    //mfpInnerRequest = new int[16];
    //mfpInnerAcknowledged = new int[16];
    //mfpInnerInService = new boolean[16];
    //mfpUdrQueueArray = new int[MFP_UDR_QUEUE_SIZE];
    for (int i = 0; i < MFP_UDR_QUEUE_SIZE; i++) {
      mfpUdrQueueArray[i] = 0;
    }
    mfpUdrQueueRead = 0;
    mfpUdrQueueWrite = 0;
    mfpReset ();
  }  //mfpInit()

  //リセット
  public static void mfpReset () {
    mfpGpipAlarm = 0;
    mfpGpipExpwon = MFP_GPIP_EXPWON_MASK;
    mfpGpipPower = 0;
    mfpGpipOpmirq = MFP_GPIP_OPMIRQ_MASK;
    mfpGpipVdisp = 0;
    mfpGpipRint = MFP_GPIP_RINT_MASK;
    mfpGpipHsync = 0;
    mfpAer = 0;
    mfpIer = 0;
    for (int i = 0; i < 16; i++) {
      mfpInnerRequest[i] = 0;
      mfpInnerAcknowledged[i] = 0;
      mfpInnerInService[i] = false;
    }
    mfpImr = 0;
    mfpVectorHigh = 0;
    mfpTaPrescale = 0;
    mfpTbPrescale = 0;
    mfpTcPrescale = 0;
    mfpTdPrescale = 0;
    mfpTaEventcount = false;
    mfpTbEventcount = false;
    mfpTaInitial = 256;
    mfpTbInitial = 256;
    mfpTcInitial = 256;
    mfpTdInitial = 256;
    mfpTaCurrent = 0;
    mfpTbCurrent = 0;
    mfpTcCurrent = 0;
    mfpTdCurrent = 0;
    mfpTaData = 0;
    mfpTbData = 0;
    mfpTcData = 0;
    mfpTdData = 0;
    mfpIoldsTime = 0L;
    mfpTaStart = 0L;
    mfpTbStart = 0L;
    mfpTcStart = 0L;
    mfpTdStart = 0L;
    mfpTaClock = XEiJ.FAR_FUTURE;
    mfpTbClock = XEiJ.FAR_FUTURE;
    mfpTcClock = XEiJ.FAR_FUTURE;
    mfpTdClock = XEiJ.FAR_FUTURE;
    mfpUCR = 0x88;
    mfpRSR = 0x01;
    mfpTSR = 0x01;
    mfpTSRBufferFullTime = 0L;
    mfpTSRBufferFullEnd = 0L;
    if (MFP_KBD_ON) {
      //mfpKbdBuffer = new int[MFP_KBD_LIMIT];
      mfpKbdReadPointer = 0;
      mfpKbdWritePointer = 0;
      mfpKbdLastData = 0;
      mfpTkClock = XEiJ.FAR_FUTURE;
      //mfpTkTime = 0L;
    }
    TickerQueue.tkqRemove (mfpTaTicker);
    TickerQueue.tkqRemove (mfpTbTicker);
    TickerQueue.tkqRemove (mfpTcTicker);
    TickerQueue.tkqRemove (mfpTdTicker);
    TickerQueue.tkqRemove (mfpTkTicker);
  }  //mfpReset()

  //割り込み受付
  //  コアが割り込み要求を受け付けたときに呼び出す
  //  割り込みベクタ番号を返す
  //  割り込み要求を取り下げる場合は0を返す
  //  オートベクタを使用するデバイスはオートベクタの番号を返すこと
  //
  //! 未対応
  //  スプリアス割り込みは発生しない
  //  実機ではデバイスからMFPへの割り込み要求とMPUからMFPへの割り込み禁止指示がほぼ同時に発生するとスプリアス割り込みが通知されることがある
  //    (1) デバイスがMFPに割り込みを要求する
  //    (2) MPUがMFPにデバイスの割り込みの禁止を指示する
  //    (3) MFPがデバイスの要求に従ってMPUに割り込みを要求する
  //    (4) MFPがMPUの指示に従ってデバイスの割り込みを禁止する
  //    (5) MPUがMFPの割り込み要求を受け付けてMFPに割り込みベクタの提出を指示する
  //    (6) MFPがMPUの指示に従って割り込みが許可されていて割り込みを要求しているデバイスを探すが見当たらないので応答しない
  //    (7) MPUがスプリアス割り込みを通知する
  //  ここではデバイスが見つからないとき割り込み要求を取り下げるのでスプリアス割り込みは発生しない
  public static int mfpAcknowledge () {
    for (int i = 15; i >= 0; i--) {
      if ((mfpImr & 1 << i) != 0) {
        int request = mfpInnerRequest[i];
        if (mfpInnerAcknowledged[i] != request) {
          mfpInnerAcknowledged[i] = request;
          mfpInnerInService[mfpInnerLevel = i] = true;
          return mfpVectorHigh + i;
        }
      }
    }
    return 0;
  }  //mfpAcknowledge()

  //割り込み終了
  //  コアが割り込み処理を終了したときに呼び出す
  //  まだ処理されていない割り込みが残っていたら再度割り込み要求を出す
  public static void mfpDone () {
    mfpInnerInService[mfpInnerLevel] = false;
    for (int i = 15; i >= 0; i--) {
      if ((mfpImr & 1 << i) != 0 && mfpInnerAcknowledged[i] != mfpInnerRequest[i]) {
        if (MFP_DELAYED_INTERRUPT) {
          XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
        } else {
          XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
        }
        return;
      }
    }
  }  //mfpDone()

  //mfpKeyboardInput (scanCode)
  //  キー入力
  //  コアのスレッドで呼び出すこと
  public static void mfpKeyboardInput (int scanCode) {
    if (!mfpTbKeyboardOn) {
      return;
    }
    scanCode &= 0xff;
    if (mfpUdrQueueWrite + 1 != mfpUdrQueueRead) {  //キューは満杯ではない
      mfpUdrQueueWrite = mfpUdrQueueWrite + 1;
      mfpUdrQueueArray[mfpUdrQueueWrite & MFP_UDR_QUEUE_MASK] = scanCode;
      if ((mfpIer & MFP_INPUT_FULL_MASK) != 0) {
        mfpInnerRequest[MFP_INPUT_FULL_LEVEL]++;
        if ((mfpImr & MFP_INPUT_FULL_MASK) != 0) {
          if (MFP_DELAYED_INTERRUPT) {
            XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          } else {
            XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          }
        }
      }
    }
  }  //mfpKeyboardInput


  //Timer-A
  public static final TickerQueue.Ticker mfpTaTicker = new TickerQueue.Ticker () {
    @Override protected void tick () {
      if ((mfpIer & MFP_TIMER_A_MASK) != 0) {
        mfpInnerRequest[MFP_TIMER_A_LEVEL]++;
        if ((mfpImr & MFP_TIMER_A_MASK) != 0) {
          if (MFP_DELAYED_INTERRUPT) {
            XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          } else {
            XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          }
        }
      }
      mfpTaClock += mfpTaDelta * mfpTaInitial;
      TickerQueue.tkqAdd (mfpTaTicker, mfpTaClock);
    }
  };

  //Timer-B
  public static final TickerQueue.Ticker mfpTbTicker = new TickerQueue.Ticker () {
    @Override protected void tick () {
      if ((mfpIer & MFP_TIMER_B_MASK) != 0) {
        mfpInnerRequest[MFP_TIMER_B_LEVEL]++;
        if ((mfpImr & MFP_TIMER_B_MASK) != 0) {
          if (MFP_DELAYED_INTERRUPT) {
            XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          } else {
            XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          }
        }
      }
      mfpTbClock += mfpTbDelta * mfpTbInitial;
      TickerQueue.tkqAdd (mfpTbTicker, mfpTbClock);
    }
  };

  //Timer-C
  public static final TickerQueue.Ticker mfpTcTicker = new TickerQueue.Ticker () {
    @Override protected void tick () {
      if ((mfpIer & MFP_TIMER_C_MASK) != 0) {
        mfpInnerRequest[MFP_TIMER_C_LEVEL]++;
        if ((mfpImr & MFP_TIMER_C_MASK) != 0) {
          if (MFP_DELAYED_INTERRUPT) {
            XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          } else {
            XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          }
        }
      }
      mfpTcClock += mfpTcDelta * mfpTcInitial;
      TickerQueue.tkqAdd (mfpTcTicker, mfpTcClock);
    }
  };

  //Timer-D
  public static final TickerQueue.Ticker mfpTdTicker = new TickerQueue.Ticker () {
    @Override protected void tick () {
      if ((mfpIer & MFP_TIMER_D_MASK) != 0) {
        mfpInnerRequest[MFP_TIMER_D_LEVEL]++;
        if ((mfpImr & MFP_TIMER_D_MASK) != 0) {
          if (MFP_DELAYED_INTERRUPT) {
            XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          } else {
            XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          }
        }
      }
      mfpTdClock += mfpTdDelta * mfpTdInitial;
      TickerQueue.tkqAdd (mfpTdTicker, mfpTdClock);
    }
  };

  //キーボード
  public static final TickerQueue.Ticker mfpTkTicker = new TickerQueue.Ticker () {
    @Override protected void tick () {
      if (MFP_KBD_ON) {
        //  XEiJ.mpuClockTimeだけで割り込みのタイミングを決めると、
        //  コアのタスクが詰まっているときキー入力割り込みも詰まってリピートの開始と間隔が短くなってしまう
        long time = System.currentTimeMillis () - 10L;  //10msまでは早すぎてもよいことにする
        if (time < mfpTkTime) {  //早すぎる
          mfpTkClock = XEiJ.mpuClockTime + XEiJ.TMR_FREQ / 1000 * (mfpTkTime - time);
          TickerQueue.tkqAdd (mfpTkTicker, mfpTkClock);
        } else {
          if (mfpTbKeyboardOn) {
            if (mfpKbdReadPointer != mfpKbdWritePointer) {  //バッファが空でないとき
              if ((mfpIer & MFP_INPUT_FULL_MASK) != 0) {
                mfpInnerRequest[MFP_INPUT_FULL_LEVEL]++;
                if ((mfpImr & MFP_INPUT_FULL_MASK) != 0) {
                  //MFPのキー入力割り込みを要求する
                  if (MFP_DELAYED_INTERRUPT) {
                    XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
                  } else {
                    XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
                  }
                }
              }
            }
          }
          mfpTkClock = XEiJ.FAR_FUTURE;
          TickerQueue.tkqRemove (mfpTkTicker);
          //mfpTkTime = 0L;
        }
      }
    }  //tick()
  };


  //GPIP入力
  //  デバイスが呼び出す
  //GPIP0
  public static void mfpAlarmRise () {
    if (mfpGpipAlarm == 0) {  //0→1
      mfpGpipAlarm = MFP_GPIP_ALARM_MASK;
      if ((mfpAer & MFP_GPIP_ALARM_MASK) != 0 && (mfpIer & MFP_ALARM_MASK) != 0) {
        mfpInnerRequest[MFP_ALARM_LEVEL]++;
        if ((mfpImr & MFP_ALARM_MASK) != 0) {
          if (MFP_DELAYED_INTERRUPT) {
            XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          } else {
            XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          }
        }
      }
    }
  }  //mfpAlarmRise()
  public static void mfpAlarmFall () {
    if (mfpGpipAlarm != 0) {  //1→0
      mfpGpipAlarm = 0;
      if ((mfpAer & MFP_GPIP_ALARM_MASK) == 0 && (mfpIer & MFP_ALARM_MASK) != 0) {
        mfpInnerRequest[MFP_ALARM_LEVEL]++;
        if ((mfpImr & MFP_ALARM_MASK) != 0) {
          if (MFP_DELAYED_INTERRUPT) {
            XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          } else {
            XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          }
        }
      }
    }
  }  //mfpAlarmFall()

  //GPIP1
  public static void mfpExpwonRise () {
    if (mfpGpipExpwon == 0) {  //0→1
      mfpGpipExpwon = MFP_GPIP_EXPWON_MASK;
      if ((mfpAer & MFP_GPIP_EXPWON_MASK) != 0 && (mfpIer & MFP_EXPWON_MASK) != 0) {
        mfpInnerRequest[MFP_EXPWON_LEVEL]++;
        if ((mfpImr & MFP_EXPWON_MASK) != 0) {
          if (MFP_DELAYED_INTERRUPT) {
            XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          } else {
            XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          }
        }
      }
    }
  }  //mfpExpwonRise()
  public static void mfpExpwonFall () {
    if (mfpGpipExpwon != 0) {  //1→0
      mfpGpipExpwon = 0;
      if ((mfpAer & MFP_GPIP_EXPWON_MASK) == 0 && (mfpIer & MFP_EXPWON_MASK) != 0) {
        mfpInnerRequest[MFP_EXPWON_LEVEL]++;
        if ((mfpImr & MFP_EXPWON_MASK) != 0) {
          if (MFP_DELAYED_INTERRUPT) {
            XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          } else {
            XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          }
        }
      }
    }
  }  //mfpExpwonFall()

  //GPIP2
  public static void mfpPowerRise () {
    if (mfpGpipPower == 0) {  //0→1
      mfpGpipPower = MFP_GPIP_POWER_MASK;
      if ((mfpAer & MFP_GPIP_POWER_MASK) != 0 && (mfpIer & MFP_POWER_MASK) != 0) {
        mfpInnerRequest[MFP_POWER_LEVEL]++;
        if ((mfpImr & MFP_POWER_MASK) != 0) {
          if (MFP_DELAYED_INTERRUPT) {
            XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          } else {
            XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          }
        }
      }
    }
  }  //mfpPowerRise()
  public static void mfpPowerFall () {
    if (mfpGpipPower != 0) {  //1→0
      mfpGpipPower = 0;
      if ((mfpAer & MFP_GPIP_POWER_MASK) == 0 && (mfpIer & MFP_POWER_MASK) != 0) {
        mfpInnerRequest[MFP_POWER_LEVEL]++;
        if ((mfpImr & MFP_POWER_MASK) != 0) {
          if (MFP_DELAYED_INTERRUPT) {
            XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          } else {
            XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          }
        }
      }
    }
  }  //mfpPowerFall()

  //GPIP3
  public static void mfpOpmirqRise () {
    if (mfpGpipOpmirq == 0) {  //0→1
      mfpGpipOpmirq = MFP_GPIP_OPMIRQ_MASK;
      if ((mfpAer & MFP_GPIP_OPMIRQ_MASK) != 0 && (mfpIer & MFP_OPMIRQ_MASK) != 0) {
        mfpInnerRequest[MFP_OPMIRQ_LEVEL]++;
        if ((mfpImr & MFP_OPMIRQ_MASK) != 0) {
          if (MFP_DELAYED_INTERRUPT) {
            XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          } else {
            XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          }
        }
      }
    }
  }  //mfpOpmirqRise()
  public static void mfpOpmirqFall () {
    if (mfpGpipOpmirq != 0) {  //1→0
      mfpGpipOpmirq = 0;
      if ((mfpAer & MFP_GPIP_OPMIRQ_MASK) == 0 && (mfpIer & MFP_OPMIRQ_MASK) != 0) {
        mfpInnerRequest[MFP_OPMIRQ_LEVEL]++;
        if ((mfpImr & MFP_OPMIRQ_MASK) != 0) {
          if (MFP_DELAYED_INTERRUPT) {
            XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          } else {
            XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          }
        }
      }
    }
  }  //mfpOpmirqFall()

  //GPIP4
  public static void mfpVdispRise () {
    //if (mfpGpipVdisp == 0) {  //0→1
    mfpGpipVdisp = MFP_GPIP_VDISP_MASK;
    if ((mfpAer & MFP_GPIP_VDISP_MASK) != 0) {
      if ((mfpIer & MFP_VDISP_MASK) != 0) {
        mfpInnerRequest[MFP_VDISP_LEVEL]++;
        if ((mfpImr & MFP_VDISP_MASK) != 0) {
          if (MFP_DELAYED_INTERRUPT) {
            XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          } else {
            XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          }
        }
      }
      if (mfpTaEventcount && --mfpTaCurrent <= 0) {
        mfpTaCurrent = mfpTaInitial;
        if ((mfpIer & MFP_TIMER_A_MASK) != 0) {
          mfpInnerRequest[MFP_TIMER_A_LEVEL]++;
          if ((mfpImr & MFP_TIMER_A_MASK) != 0) {
            if (MFP_DELAYED_INTERRUPT) {
              XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
            } else {
              XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
            }
          }
        }
      }
    }
    //}
  }  //mfpVdispRise()
  public static void mfpVdispFall () {
    //if (mfpGpipVdisp != 0) {  //1→0
    mfpGpipVdisp = 0;
    if ((mfpAer & MFP_GPIP_VDISP_MASK) == 0) {
      if ((mfpIer & MFP_VDISP_MASK) != 0) {
        mfpInnerRequest[MFP_VDISP_LEVEL]++;
        if ((mfpImr & MFP_VDISP_MASK) != 0) {
          if (MFP_DELAYED_INTERRUPT) {
            XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          } else {
            XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          }
        }
      }
      if (mfpTaEventcount && --mfpTaCurrent <= 0) {
        mfpTaCurrent = mfpTaInitial;
        if ((mfpIer & MFP_TIMER_A_MASK) != 0) {
          mfpInnerRequest[MFP_TIMER_A_LEVEL]++;
          if ((mfpImr & MFP_TIMER_A_MASK) != 0) {
            if (MFP_DELAYED_INTERRUPT) {
              XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
            } else {
              XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
            }
          }
        }
      }
    }
    //}
  }  //mfpVdispFall()

  //GPIP6
  public static void mfpRintRise () {
    //if (mfpGpipRint == 0) {  //0→1
    mfpGpipRint = MFP_GPIP_RINT_MASK;
    if ((mfpAer & MFP_GPIP_RINT_MASK) != 0 && (mfpIer & MFP_RINT_MASK) != 0) {
      mfpInnerRequest[MFP_RINT_LEVEL]++;
      if ((mfpImr & MFP_RINT_MASK) != 0) {
        if (MFP_DELAYED_INTERRUPT) {
          XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
        } else {
          XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
        }
      }
    }
    //}
  }  //mfpRintRise()
  public static void mfpRintFall () {
    //if (mfpGpipRint != 0) {  //1→0
    mfpGpipRint = 0;
    if ((mfpAer & MFP_GPIP_RINT_MASK) == 0 && (mfpIer & MFP_RINT_MASK) != 0) {
      mfpInnerRequest[MFP_RINT_LEVEL]++;
      if ((mfpImr & MFP_RINT_MASK) != 0) {
        if (MFP_DELAYED_INTERRUPT) {
          XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
        } else {
          XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
        }
      }
    }
    //}
  }  //mfpRintFall()

  //GPIP7
  public static void mfpHsyncRise () {
    //if (mfpGpipHsync == 0) {  //0→1
    mfpGpipHsync = MFP_GPIP_HSYNC_MASK;
    if ((mfpAer & MFP_GPIP_HSYNC_MASK) != 0 && (mfpIer & MFP_HSYNC_MASK) != 0) {
      mfpInnerRequest[MFP_HSYNC_LEVEL]++;
      if ((mfpImr & MFP_HSYNC_MASK) != 0) {
        if (MFP_DELAYED_INTERRUPT) {
          XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
        } else {
          XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
        }
      }
    }
    if (MercuryUnit.MU4_ON && MercuryUnit.mu4OnPositive &&  //HSYNC
        CRTC.crtR05VSyncEndCurr <= CRTC.crtRasterNumber) {  //!VSYNC
      MercuryUnit.mu4HsyncStart (CRTC.crtClock);
    }
    //}
  }  //mfpHsyncRise()
  public static void mfpHsyncFall () {
    //if (mfpGpipHsync != 0) {  //1→0
    mfpGpipHsync = 0;
    if ((mfpAer & MFP_GPIP_HSYNC_MASK) == 0 && (mfpIer & MFP_HSYNC_MASK) != 0) {
      mfpInnerRequest[MFP_HSYNC_LEVEL]++;
      if ((mfpImr & MFP_HSYNC_MASK) != 0) {
        if (MFP_DELAYED_INTERRUPT) {
          XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
        } else {
          XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
        }
      }
    }
    if (MercuryUnit.MU4_ON && MercuryUnit.mu4OnNegative &&  //!HSYNC
        CRTC.crtRasterNumber < CRTC.crtR05VSyncEndCurr) {  //VSYNC
      MercuryUnit.mu4HsyncStart (CRTC.crtClock);
    }
    //}
  }  //mfpHsyncFall()

  //キー入力のリピートの処理をMFPで行う
  //  キー入力バッファ
  //    キー入力データが並んでいるリングバッファ
  //    読み出しポインタ
  //      次に読み出す位置
  //      MFPが更新する
  //    書き込みポインタ
  //      次に書き込む位置
  //      KBDが更新する
  //    読み出しポインタと書き込みポインタが一致しているときバッファは空
  //    読み出しポインタを進めると書き込みポインタと一致するとき読み出しポインタはバッファの末尾
  //    書き込みポインタを進めると読み出しポインタと一致するときはバッファが一杯(なので書き込めない)
  //  キー入力データ
  //    0x0000xxxx  リピートしないデータ。リピートしないキーが押されたかキーが離されたときのデータ
  //    0x0001xxxx  リピート開始前のデータ。リピートするキーが押されてまだ読み出されていないデータ
  //    0xffffxxxx  リピート開始後のデータ。リピートするキーが押されて1回以上読み出されたデータ
  //    0x00+キーコード  キーが押されたデータ
  //    0x80+キーコード  キーが離されたデータ
  //  最後に読み出したデータ
  //    キー入力バッファから最後に読み出したデータ
  //    キー入力バッファが空のときUDRから読み出される
  //  キーが押されたまたは離されたとき
  //    バッファが一杯でないとき
  //      キーが押されたとき
  //        書き込みポインタの位置にリピート開始前のデータまたはリピートしないデータを書き込む
  //      キーが離されたとき
  //        書き込みポインタの位置にリピートしないデータを書き込む
  //      書き込みポインタを進める
  //    MFPのキー入力割り込みを要求する
  //  UDR読み出し
  //    バッファが空でないとき
  //      バッファの末尾でなくて先頭がリピート開始後のデータのとき
  //        読み出しポインタを進める
  //      バッファの先頭のデータを最後に読み出したデータとして保存する
  //      バッファの末尾でないとき
  //        読み出しポインタを進める
  //        MFPのキー入力割り込みを要求する
  //      バッファの末尾でリピートしないデータのとき
  //        読み出しポインタを進める
  //      バッファの末尾でリピート開始前のデータのとき
  //        バッファの末尾のデータをリピート開始後に変更する
  //        現在時刻+リピート開始時間でMFPのTimer-Kをセットする
  //      バッファの末尾でリピート開始後のデータのとき
  //        現在時刻+リピート間隔時間でMFPのTimer-Kをセットする
  //    最後に読み出したデータを返す
  //  MFPのTimer-K
  //    キー入力のリピート処理のためにMFPに追加されたタイマー
  //    バッファが空でないとき
  //      MFPのキー入力割り込みを要求する
  //  UDR読み出しとMFPのTimer-Kはどちらもコアから呼ばれるので同時に呼び出されることはない
  //  キー入力割り込みがIERAで禁止されているとタイマーで割り込みがかからないのでリピートが止まってしまうが、
  //  キー入力割り込みを止めたいときはIMRAでマスクするのが原則なので通常は問題ないはず
  public static final boolean MFP_KBD_ON = false;  //true=キー入力のリピートの処理をMFPで行う
  public static final int MFP_KBD_SIZE = 256;  //キー入力バッファのサイズ。2の累乗にすること
  public static final int MFP_KBD_MASK = MFP_KBD_SIZE - 1;
  public static final int[] mfpKbdBuffer = new int[MFP_KBD_SIZE];  //キー入力バッファ
  public static volatile int mfpKbdReadPointer;  //読み出しポインタ。マスクしない
  public static volatile int mfpKbdWritePointer;  //書き込みポインタ。マスクしない
  public static int mfpKbdLastData;  //最後に読み出したデータ
  public static long mfpTkClock;  //Timer-Kの次の呼び出し時刻
  public static long mfpTkTime;  //次にTimer-Kが呼び出されるべき時刻(ms)。mfpTkClockがXEiJ.FAR_FUTUREでないときだけ有効

  //mfpKbdInput (data, repeat)
  //  キーが押されたまたは離されたとき
  //  data    0x00+キーコード  キーが押されたデータ
  //          0x80+キーコード  キーが離されたデータ
  //  repeat  false  リピートしない
  //          true   リピートする
  public static void mfpKbdInput (int data, boolean repeat) {
    if (!mfpTbKeyboardOn) {
      return;
    }
    int w = mfpKbdWritePointer;
    if (w + 1 != mfpKbdReadPointer) {  //バッファが一杯でないとき
      mfpKbdBuffer[w & MFP_KBD_MASK] = (repeat ? 0x00010000 : 0x00000000) | data;  //書き込みポインタの位置にデータを書き込む
      mfpKbdWritePointer = w + 1;  //書き込みポインタを進める
      if ((mfpIer & MFP_INPUT_FULL_MASK) != 0) {
        mfpInnerRequest[MFP_INPUT_FULL_LEVEL]++;
        if ((mfpImr & MFP_INPUT_FULL_MASK) != 0) {
          //MFPのキー入力割り込みを要求する
          if (MFP_DELAYED_INTERRUPT) {
            XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          } else {
            XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
          }
        }
      }
    }
  }  //mfpKbdInput(int,boolean)

  //data = mfpKbdReadData ()
  //  UDR読み出し
  public static int mfpKbdReadData () {
    int r = mfpKbdReadPointer;
    int w = mfpKbdWritePointer;
    if (r != w) {  //バッファが空でないとき
      int s = mfpKbdBuffer[r & MFP_KBD_MASK];  //バッファの先頭のデータ
      if (r + 1 != w && s < 0) {  //バッファの末尾でなくて先頭がリピート開始後のデータのとき
        mfpKbdReadPointer = r = r + 1;  //読み出しポインタを進める
        s = mfpKbdBuffer[r & MFP_KBD_MASK];  //バッファの先頭のデータ
      }
      mfpKbdLastData = (char) s;  //バッファの先頭のデータを最後に読み出したデータとして保存する
      if (r + 1 != w) {  //バッファの末尾でないとき
        mfpKbdReadPointer = r + 1;  //読み出しポインタを進める
        if ((mfpIer & MFP_INPUT_FULL_MASK) != 0) {
          mfpInnerRequest[MFP_INPUT_FULL_LEVEL]++;
          if ((mfpImr & MFP_INPUT_FULL_MASK) != 0) {
            //MFPのキー入力割り込みを要求する
            if (MFP_DELAYED_INTERRUPT) {
              XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
            } else {
              XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
            }
          }
        }
      } else if ((s >> 16) == 0x0000) {  //バッファの末尾でリピートしないデータのとき
        mfpKbdReadPointer = r + 1;  //読み出しポインタを進める
      } else if ((s >> 16) == 0x0001) {  //バッファの末尾でリピート開始前のデータのとき
        mfpKbdBuffer[r & MFP_KBD_MASK] = 0xffff0000 | s;  //バッファの末尾のデータをリピート開始後に変更する
        if (mfpTkTicker.time == Long.MAX_VALUE &&  //Timer-Kが予約されておらず
            (mfpIer & MFP_INPUT_FULL_MASK) != 0) {  //キー入力割り込みが許可されているとき
          mfpTkClock = XEiJ.mpuClockTime + XEiJ.TMR_FREQ / 1000 * Keyboard.kbdRepeatDelay;  //現在時刻+リピート開始時間でMFPのTimer-Kをセットする
          TickerQueue.tkqAdd (mfpTkTicker, mfpTkClock);
          mfpTkTime = System.currentTimeMillis () + Keyboard.kbdRepeatDelay;  //次にTimer-Kが呼び出されるべき時刻(ms)
        }
      } else {  //バッファの末尾でリピート開始後のデータのとき
        if (mfpTkTicker.time == Long.MAX_VALUE &&  //Timer-Kが予約されておらず
            (mfpIer & MFP_INPUT_FULL_MASK) != 0) {  //キー入力割り込みが許可されているとき
          mfpTkClock = XEiJ.mpuClockTime + XEiJ.TMR_FREQ / 1000 * Keyboard.kbdRepeatInterval;  //現在時刻+リピート間隔時間でMFPのTimer-Kをセットする
          TickerQueue.tkqAdd (mfpTkTicker, mfpTkClock);
          mfpTkTime = System.currentTimeMillis () + Keyboard.kbdRepeatInterval;  //次にTimer-Kが呼び出されるべき時刻(ms)
        }
      }
    }
    return mfpKbdLastData;  //最後に読み出したデータを返す
  }  //mfpKbdReadData()



  //mfpPeekByte_00 (a)
  //mfpPeekByte_30 (a)
  //  ピークバイト
  public static int mfpPeekByte_00 (int a) {
    return ((a & 0x3f) == (MFP_UDR & 0x3f) ?
            mfpUdrQueueArray[mfpUdrQueueRead & MFP_UDR_QUEUE_MASK] :
            mfpReadByte_00 (a));
  }  //mfpPeekByte_00
  public static int mfpPeekByte_30 (int a) {
    return ((a & 0x3f) == (MFP_UDR & 0x3f) ?
            mfpUdrQueueArray[mfpUdrQueueRead & MFP_UDR_QUEUE_MASK] :
            mfpReadByte_30 (a));
  }  //mfpPeekByte_30

  //mfpReadByte_00 (a)
  //mfpReadByte_30 (a)
  //  リードバイト
  public static int mfpReadByte_00 (int a) {
    switch (a & 0x3f) {
    case MFP_GPIP_DATA & 0x3f:
      return mfpReadGPDR ();
    case MFP_AER & 0x3f:
      return mfpAer;
    case MFP_DDR & 0x3f:
      return 0x00;
    case MFP_IERA & 0x3f:
      return mfpIer >>> 8;
    case MFP_IERB & 0x3f:
      return mfpIer & 0xff;
    case MFP_IPRA & 0x3f:
      return mfpReadIPRA ();
    case MFP_IPRB & 0x3f:
      return mfpReadIPRB ();
    case MFP_ISRA & 0x3f:
      return mfpReadISRA ();
    case MFP_ISRB & 0x3f:
      return mfpReadISRB ();
    case MFP_IMRA & 0x3f:
      return mfpImr >>> 8;
    case MFP_IMRB & 0x3f:
      return mfpImr & 0xff;
    case MFP_VECTOR & 0x3f:
      return mfpVectorHigh;
    case MFP_TACR & 0x3f:
      return (mfpTaEventcount ? 0x08 : 0) | mfpTaPrescale;
    case MFP_TBCR & 0x3f:
      return (mfpTbEventcount ? 0x08 : 0) | mfpTbPrescale;
    case MFP_TCDCR & 0x3f:
      return mfpTcPrescale << 4 | mfpTdPrescale;
    case MFP_TADR & 0x3f:
      return mfpReadTADR_00 ();
    case MFP_TBDR & 0x3f:
      return mfpReadTBDR_00 ();
    case MFP_TCDR & 0x3f:
      return mfpReadTCDR_00 ();
    case MFP_TDDR & 0x3f:
      return mfpReadTDDR_00 ();
    case MFP_SYNC_CHAR & 0x3f:
      return 0;
    case MFP_UCR & 0x3f:
      return mfpUCR;
    case MFP_RSR & 0x3f:
      return mfpReadRSR ();
    case MFP_TSR & 0x3f:
      return mfpReadTSR ();
    case MFP_UDR & 0x3f:
      return mfpReadUDR ();
    default:
      return 0xff;
    }
  }  //mfpReadByte_00
  public static int mfpReadByte_30 (int a) {
    switch (a & 0x3f) {
    case MFP_GPIP_DATA & 0x3f:
      return mfpReadGPDR ();
    case MFP_AER & 0x3f:
      return mfpAer;
    case MFP_DDR & 0x3f:
      return 0x00;
    case MFP_IERA & 0x3f:
      return mfpIer >>> 8;
    case MFP_IERB & 0x3f:
      return mfpIer & 0xff;
    case MFP_IPRA & 0x3f:
      return mfpReadIPRA ();
    case MFP_IPRB & 0x3f:
      return mfpReadIPRB ();
    case MFP_ISRA & 0x3f:
      return mfpReadISRA ();
    case MFP_ISRB & 0x3f:
      return mfpReadISRB ();
    case MFP_IMRA & 0x3f:
      return mfpImr >>> 8;
    case MFP_IMRB & 0x3f:
      return mfpImr & 0xff;
    case MFP_VECTOR & 0x3f:
      return mfpVectorHigh;
    case MFP_TACR & 0x3f:
      return (mfpTaEventcount ? 0x08 : 0) | mfpTaPrescale;
    case MFP_TBCR & 0x3f:
      return (mfpTbEventcount ? 0x08 : 0) | mfpTbPrescale;
    case MFP_TCDCR & 0x3f:
      return mfpTcPrescale << 4 | mfpTdPrescale;
    case MFP_TADR & 0x3f:
      return mfpReadTADR_30 ();
    case MFP_TBDR & 0x3f:
      return mfpReadTBDR_30 ();
    case MFP_TCDR & 0x3f:
      return mfpReadTCDR_30 ();
    case MFP_TDDR & 0x3f:
      return mfpReadTDDR_30 ();
    case MFP_SYNC_CHAR & 0x3f:
      return 0;
    case MFP_UCR & 0x3f:
      return mfpUCR;
    case MFP_RSR & 0x3f:
      return mfpReadRSR ();
    case MFP_TSR & 0x3f:
      return mfpReadTSR ();
    case MFP_UDR & 0x3f:
      return mfpReadUDR ();
    default:
      return 0xff;
    }
  }  //mfpReadByte_30

  public static int mfpReadGPDR () {
    return (mfpGpipHsync |
            mfpGpipRint |
            0b00100000 |  //GPIP5は常に1
            mfpGpipVdisp |
            mfpGpipOpmirq |
            mfpGpipPower |
            mfpGpipExpwon |
            mfpGpipAlarm);
  }  //mfpReadGPDR

  public static int mfpReadIPRA () {
    return ((mfpInnerRequest[15] != mfpInnerAcknowledged[15] ? 0b10000000 : 0) |
            (mfpInnerRequest[14] != mfpInnerAcknowledged[14] ? 0b01000000 : 0) |
            (mfpInnerRequest[13] != mfpInnerAcknowledged[13] ? 0b00100000 : 0) |
            (mfpInnerRequest[12] != mfpInnerAcknowledged[12] ? 0b00010000 : 0) |
            (mfpInnerRequest[11] != mfpInnerAcknowledged[11] ? 0b00001000 : 0) |
            (mfpInnerRequest[10] != mfpInnerAcknowledged[10] ? 0b00000100 : 0) |
            (mfpInnerRequest[ 9] != mfpInnerAcknowledged[ 9] ? 0b00000010 : 0) |
            (mfpInnerRequest[ 8] != mfpInnerAcknowledged[ 8] ? 0b00000001 : 0));
  }  //mfpReadIPRA

  public static int mfpReadIPRB () {
    return ((mfpInnerRequest[ 7] != mfpInnerAcknowledged[ 7] ? 0b10000000 : 0) |
            (mfpInnerRequest[ 6] != mfpInnerAcknowledged[ 6] ? 0b01000000 : 0) |
            (mfpInnerRequest[ 5] != mfpInnerAcknowledged[ 5] ? 0b00100000 : 0) |
            (mfpInnerRequest[ 4] != mfpInnerAcknowledged[ 4] ? 0b00010000 : 0) |
            (mfpInnerRequest[ 3] != mfpInnerAcknowledged[ 3] ? 0b00001000 : 0) |
            (mfpInnerRequest[ 2] != mfpInnerAcknowledged[ 2] ? 0b00000100 : 0) |
            (mfpInnerRequest[ 1] != mfpInnerAcknowledged[ 1] ? 0b00000010 : 0) |
            (mfpInnerRequest[ 0] != mfpInnerAcknowledged[ 0] ? 0b00000001 : 0));
  }  //mfpReadIPRB

  public static int mfpReadISRA () {
    return ((mfpInnerInService[15] ? 0b10000000 : 0) |
            (mfpInnerInService[14] ? 0b01000000 : 0) |
            (mfpInnerInService[13] ? 0b00100000 : 0) |
            (mfpInnerInService[12] ? 0b00010000 : 0) |
            (mfpInnerInService[11] ? 0b00001000 : 0) |
            (mfpInnerInService[10] ? 0b00000100 : 0) |
            (mfpInnerInService[ 9] ? 0b00000010 : 0) |
            (mfpInnerInService[ 8] ? 0b00000001 : 0));
  }  //mfpReadISRA

  public static int mfpReadISRB () {
    return ((mfpInnerInService[ 7] ? 0b10000000 : 0) |
            (mfpInnerInService[ 6] ? 0b01000000 : 0) |
            (mfpInnerInService[ 5] ? 0b00100000 : 0) |
            (mfpInnerInService[ 4] ? 0b00010000 : 0) |
            (mfpInnerInService[ 3] ? 0b00001000 : 0) |
            (mfpInnerInService[ 2] ? 0b00000100 : 0) |
            (mfpInnerInService[ 1] ? 0b00000010 : 0) |
            (mfpInnerInService[ 0] ? 0b00000001 : 0));
  }  //mfpReadISRB

  public static int mfpReadTADR_00 () {
    if (!mfpTaEventcount && mfpTaPrescale != 0) {  //ディレイモードで動作中のとき
      mfpTaData = (
        mfpTaInitial -
        (int) (Math.floor ((double) (XEiJ.mpuClockTime - mfpTaStart) / (double) mfpTaDelta)
               % mfpTaInitial)
        ) & 0xff;  //現在の時刻のカウンタの値を求めて返す
    }
    return mfpTaData;
  }  //mfpReadTADR_00
  public static int mfpReadTADR_30 () {
    if (!mfpTaEventcount && mfpTaPrescale != 0) {  //ディレイモードで動作中のとき
      mfpTaData = (
        mfpTaInitial -
        (int) (Math.floor ((double) (mfpIoldsTime - mfpTaStart) / (double) mfpTaDelta)
               % mfpTaInitial)
        ) & 0xff;  //記録された時刻のカウンタの値を求めて返す
    }
    return mfpTaData;
  }  //mfpReadTADR_30

  public static int mfpReadTBDR_00 () {
    if (!mfpTbEventcount && mfpTbPrescale != 0) {  //ディレイモードで動作中のとき
      mfpTbData = (
        mfpTbInitial -
        (int) (Math.floor ((double) (XEiJ.mpuClockTime - mfpTbStart) / (double) mfpTbDelta)
               % mfpTbInitial)
        ) & 0xff;  //現在の時刻のカウンタの値を求めて返す
    }
    return mfpTbData;
  }  //mfpReadTBDR_00
  public static int mfpReadTBDR_30 () {
    if (!mfpTbEventcount && mfpTbPrescale != 0) {  //ディレイモードで動作中のとき
      mfpTbData = (
        mfpTbInitial -
        (int) (Math.floor ((double) (mfpIoldsTime - mfpTbStart) / (double) mfpTbDelta)
               % mfpTbInitial)
        ) & 0xff;  //記録された時刻のカウンタの値を求めて返す
    }
    return mfpTbData;
  }  //mfpReadTBDR_30

  public static int mfpReadTCDR_00 () {
    if (mfpTcPrescale != 0) {  //ディレイモードで動作中のとき
      mfpTcData = (
        mfpTcInitial -
        (int) (Math.floor ((double) (XEiJ.mpuClockTime - mfpTcStart) / (double) mfpTcDelta)
               % mfpTcInitial)
        ) & 0xff;  //現在の時刻のカウンタの値を求めて返す
    }
    return mfpTcData;
  }  //mfpReadTCDR_00
  public static int mfpReadTCDR_30 () {
    if (mfpTcPrescale != 0) {  //ディレイモードで動作中のとき
      mfpTcData = (
        mfpTcInitial -
        (int) (Math.floor ((double) (mfpIoldsTime - mfpTcStart) / (double) mfpTcDelta)
               % mfpTcInitial)
        ) & 0xff;  //記録された時刻のカウンタの値を求めて返す
    }
    return mfpTcData;
  }  //mfpReadTCDR_30

  public static int mfpReadTDDR_00 () {
    if (mfpTdPrescale != 0) {  //ディレイモードで動作中のとき
      mfpTdData = (
        mfpTdInitial -
        (int) (Math.floor ((double) (XEiJ.mpuClockTime - mfpTdStart) / (double) mfpTdDelta)
               % mfpTdInitial)
        ) & 0xff;  //現在の時刻のカウンタの値を求めて返す
    }
    return mfpTdData;
  }  //mfpReadTDDR_00
  public static int mfpReadTDDR_30 () {
    if (mfpTdPrescale != 0) {  //ディレイモードで動作中のとき
      mfpTdData = (
        mfpTdInitial -
        (int) (Math.floor ((double) (mfpIoldsTime - mfpTdStart) / (double) mfpTdDelta)
               % mfpTdInitial)
        ) & 0xff;  //記録された時刻のカウンタの値を求めて返す
    }
    return mfpTdData;
  }  //mfpReadTDDR_30

  public static int mfpReadRSR () {
    if (MFP_KBD_ON) {
      return (mfpRSR |
              (mfpKbdReadPointer == mfpKbdWritePointer ? 0 : 0x80));  //BF
    } else {
      return (mfpRSR |
              (mfpUdrQueueRead == mfpUdrQueueWrite ? 0 : 0x80));  //BF
    }
  }  //mfpReadRSR

  public static int mfpReadTSR () {
    return (mfpTSR |
            (XEiJ.mpuClockTime < mfpTSRBufferFullEnd ? 0 : 0x80));  //BE
  }  //mfpReadTSR

  public static int mfpReadUDR () {
    if (MFP_KBD_ON) {
      return mfpKbdReadData ();
    } else {
      if (mfpUdrQueueRead != mfpUdrQueueWrite) {  //キューは空ではない
        mfpUdrQueueRead = mfpUdrQueueRead + 1;
        if (mfpUdrQueueRead != mfpUdrQueueWrite) {  //キューが空にならなかったので再度割り込み要求を出す
          if ((mfpIer & MFP_INPUT_FULL_MASK) != 0) {
            mfpInnerRequest[MFP_INPUT_FULL_LEVEL]++;
            if ((mfpImr & MFP_INPUT_FULL_MASK) != 0) {
              if (MFP_DELAYED_INTERRUPT) {
                XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
              } else {
                XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
              }
            }
          }
        }
      }
      return mfpUdrQueueArray[mfpUdrQueueRead & MFP_UDR_QUEUE_MASK];  //最後に押されたまたは離されたキー
    }
  }  //mfpReadUDR

  //mfpWriteByte (a, d)
  //  ライトバイト
  public static void mfpWriteByte (int a, int d) {
    switch (a & 0x3f) {
    case MFP_GPIP_DATA & 0x3f:
      return;
    case MFP_AER & 0x3f:
      mfpAer = d & 0xff;
      return;
    case MFP_DDR & 0x3f:
      return;
    case MFP_IERA & 0x3f:
      mfpWriteIERA (d);
      return;
    case MFP_IERB & 0x3f:
      mfpWriteIERB (d);
      return;
    case MFP_IPRA & 0x3f:
      mfpWriteIPRA (d);
      return;
    case MFP_IPRB & 0x3f:
      mfpWriteIPRB (d);
      return;
    case MFP_ISRA & 0x3f:
      mfpWriteISRA (d);
      return;
    case MFP_ISRB & 0x3f:
      mfpWriteISRB (d);
      return;
    case MFP_IMRA & 0x3f:
      mfpWriteIMRA (d);
      return;
    case MFP_IMRB & 0x3f:
      mfpWriteIMRB (d);
      return;
    case MFP_VECTOR & 0x3f:
      mfpVectorHigh = 0xf0 & d;  //ビット3は0(全チャンネル自動割込み終了モード)に固定
      return;
    case MFP_TACR & 0x3f:
      mfpWriteTACR (d);
      return;
    case MFP_TBCR & 0x3f:
      mfpWriteTBCR (d);
      return;
    case MFP_TCDCR & 0x3f:
      mfpWriteTCDCR (d);
      return;
    case MFP_TADR & 0x3f:
      mfpWriteTADR (d);
      return;
    case MFP_TBDR & 0x3f:
      mfpWriteTBDR (d);
      return;
    case MFP_TCDR & 0x3f:
      mfpWriteTCDR (d);
      return;
    case MFP_TDDR & 0x3f:
      mfpWriteTDDR (d);
      return;
    case MFP_SYNC_CHAR & 0x3f:
      return;
    case MFP_UCR & 0x3f:
      mfpWriteUCR (d);
      return;
    case MFP_RSR & 0x3f:
      mfpRSR = d & 0b00000001;
      return;
    case MFP_TSR & 0x3f:
      mfpTSR = d & 0b00000001;
      return;
    case MFP_UDR & 0x3f:
      mfpWriteUDR (d);
      return;
    }
  }  //mfpWriteByte

  public static void mfpWriteIERA (int d) {
    d = (char) (d << 8);
    int oldIera = mfpIer;
    mfpIer = d | (mfpIer & 0xff);
    //MFP_IERAのビットに0を書き込むとMFP_IPRAの該当ビットも0になる
    if ((short) d >= 0) {  //(0b10000000_00000000 & d) == 0
      mfpInnerAcknowledged[15] = mfpInnerRequest[15];
    }
    if (d << 31 - 14 >= 0) {  //(0b01000000_00000000 & d) == 0
      mfpInnerAcknowledged[14] = mfpInnerRequest[14];
    }
    if (d << 31 - 13 >= 0) {  //(0b00100000_00000000 & d) == 0
      mfpInnerAcknowledged[13] = mfpInnerRequest[13];
    }
    if (d << 31 - 12 >= 0) {  //(0b00010000_00000000 & d) == 0
      mfpInnerAcknowledged[12] = mfpInnerRequest[12];
    } else if (oldIera << 31 - 12 >= 0 &&  //MFP_INPUT_FULL_MASKが0→1
               (MFP_KBD_ON ?
                mfpKbdReadPointer != mfpKbdWritePointer :  //バッファが空でないとき
                mfpUdrQueueRead != mfpUdrQueueWrite)) {  //キューが空でないとき
      mfpInnerRequest[MFP_INPUT_FULL_LEVEL]++;
      if ((mfpImr & MFP_INPUT_FULL_MASK) != 0) {
        //MFPのキー入力割り込みを要求する
        if (MFP_DELAYED_INTERRUPT) {
          XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
        } else {
          XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
        }
      }
    }
    if (d << 31 - 11 >= 0) {  //(0b00001000_00000000 & d) == 0
      mfpInnerAcknowledged[11] = mfpInnerRequest[11];
    }
    if (d << 31 - 10 >= 0) {  //(0b00000100_00000000 & d) == 0
      mfpInnerAcknowledged[10] = mfpInnerRequest[10];
    }
    if (d << 31 - 9 >= 0) {  //(0b00000010_00000000 & d) == 0
      mfpInnerAcknowledged[ 9] = mfpInnerRequest[ 9];
    }
    if (d << 31 - 8 >= 0) {  //(0b00000001_00000000 & d) == 0
      mfpInnerAcknowledged[ 8] = mfpInnerRequest[ 8];
    }
  }  //mfpWriteIERA

  public static void mfpWriteIERB (int d) {
    mfpIer = (mfpIer & ~0xff) | (d & 0xff);
    //MFP_IERBのビットに0を書き込むとMFP_IPRBの該当ビットも0になる
    if ((byte) d >= 0) {  //(0b10000000 & d) == 0
      mfpInnerAcknowledged[ 7] = mfpInnerRequest[ 7];
    }
    if (d << 31 - 6 >= 0) {  //(0b01000000 & d) == 0
      mfpInnerAcknowledged[ 6] = mfpInnerRequest[ 6];
    }
    if (d << 31 - 5 >= 0) {  //(0b00100000 & d) == 0
      mfpInnerAcknowledged[ 5] = mfpInnerRequest[ 5];
    }
    if (d << 31 - 4 >= 0) {  //(0b00010000 & d) == 0
      mfpInnerAcknowledged[ 4] = mfpInnerRequest[ 4];
    }
    if (XEiJ.TEST_BIT_3_SHIFT ? d << 31 - 3 >= 0 : (d & 8) == 0) {
      mfpInnerAcknowledged[ 3] = mfpInnerRequest[ 3];
    }
    if (XEiJ.TEST_BIT_2_SHIFT ? d << 31 - 2 >= 0 : (d & 4) == 0) {
      mfpInnerAcknowledged[ 2] = mfpInnerRequest[ 2];
    }
    if (XEiJ.TEST_BIT_1_SHIFT ? d << 31 - 1 >= 0 : (d & 2) == 0) {
      mfpInnerAcknowledged[ 1] = mfpInnerRequest[ 1];
    }
    if (XEiJ.TEST_BIT_0_SHIFT ? d << 31 - 0 >= 0 : (d & 1) == 0) {
      mfpInnerAcknowledged[ 0] = mfpInnerRequest[ 0];
    }
  }  //mfpWriteIERB

  public static void mfpWriteIPRA (int d) {
    //MFP_IPRAのビットは他のすべてのビットに1を書き込むことで直接クリアできる
    switch (d & 0xff) {
    case 0b01111111:
      mfpInnerAcknowledged[15] = mfpInnerRequest[15];
      break;
    case 0b10111111:
      mfpInnerAcknowledged[14] = mfpInnerRequest[14];
      break;
    case 0b11011111:
      mfpInnerAcknowledged[13] = mfpInnerRequest[13];
      break;
    case 0b11101111:
      mfpInnerAcknowledged[12] = mfpInnerRequest[12];
      break;
    case 0b11110111:
      mfpInnerAcknowledged[11] = mfpInnerRequest[11];
      break;
    case 0b11111011:
      mfpInnerAcknowledged[10] = mfpInnerRequest[10];
      break;
    case 0b11111101:
      mfpInnerAcknowledged[ 9] = mfpInnerRequest[ 9];
      break;
    case 0b11111110:
      mfpInnerAcknowledged[ 8] = mfpInnerRequest[ 8];
      break;
    }
  }  //mfpWriteIPRA

  public static void mfpWriteIPRB (int d) {
    //MFP_IPRBのビットは他のすべてのビットに1を書き込むことで直接クリアできる
    switch (d & 0xff) {
    case 0b01111111:
      mfpInnerAcknowledged[ 7] = mfpInnerRequest[ 7];
      break;
    case 0b10111111:
      mfpInnerAcknowledged[ 6] = mfpInnerRequest[ 6];
      break;
    case 0b11011111:
      mfpInnerAcknowledged[ 5] = mfpInnerRequest[ 5];
      break;
    case 0b11101111:
      mfpInnerAcknowledged[ 4] = mfpInnerRequest[ 4];
      break;
    case 0b11110111:
      mfpInnerAcknowledged[ 3] = mfpInnerRequest[ 3];
      break;
    case 0b11111011:
      mfpInnerAcknowledged[ 2] = mfpInnerRequest[ 2];
      break;
    case 0b11111101:
      mfpInnerAcknowledged[ 1] = mfpInnerRequest[ 1];
      break;
    case 0b11111110:
      mfpInnerAcknowledged[ 0] = mfpInnerRequest[ 0];
      break;
    }
  }  //mfpWriteIPRB

  public static void mfpWriteISRA (int d) {
    //MFP_ISRAのビットは他のすべてのビットに1を書き込むことで直接クリアできる
    switch (d & 0xff) {
    case 0b01111111:
      mfpInnerInService[15] = false;
      break;
    case 0b10111111:
      mfpInnerInService[14] = false;
      break;
    case 0b11011111:
      mfpInnerInService[13] = false;
      break;
    case 0b11101111:
      mfpInnerInService[12] = false;
      break;
    case 0b11110111:
      mfpInnerInService[11] = false;
      break;
    case 0b11111011:
      mfpInnerInService[10] = false;
      break;
    case 0b11111101:
      mfpInnerInService[ 9] = false;
      break;
    case 0b11111110:
      mfpInnerInService[ 8] = false;
      break;
    }
  }  //mfpWriteISRA

  public static void mfpWriteISRB (int d) {
    //MFP_ISRBのビットは他のすべてのビットに1を書き込むことで直接クリアできる
    switch (d & 0xff) {
    case 0b01111111:
      mfpInnerInService[ 7] = false;
      break;
    case 0b10111111:
      mfpInnerInService[ 6] = false;
      break;
    case 0b11011111:
      mfpInnerInService[ 5] = false;
      break;
    case 0b11101111:
      mfpInnerInService[ 4] = false;
      break;
    case 0b11110111:
      mfpInnerInService[ 3] = false;
      break;
    case 0b11111011:
      mfpInnerInService[ 2] = false;
      break;
    case 0b11111101:
      mfpInnerInService[ 1] = false;
      break;
    case 0b11111110:
      mfpInnerInService[ 0] = false;
      break;
    }
  }  //mfpWriteISRB

  public static void mfpWriteIMRA (int d) {
    mfpImr = (d & 0xff) << 8 | (mfpImr & 0xff);
    //MFP_IMRAのビットに1を書き込んだときMFP_IPRAの該当ビットが1ならば割り込み発生
    if ((byte) d < 0 && mfpInnerRequest[15] != mfpInnerAcknowledged[15] ||  //(0b10000000 & d) != 0
        d << 31 - 6 < 0 && mfpInnerRequest[14] != mfpInnerAcknowledged[14] ||  //(0b01000000 & d) != 0
        d << 31 - 5 < 0 && mfpInnerRequest[13] != mfpInnerAcknowledged[13] ||  //(0b00100000 & d) != 0
        d << 31 - 4 < 0 && mfpInnerRequest[12] != mfpInnerAcknowledged[12] ||  //(0b00010000 & d) != 0
        (XEiJ.TEST_BIT_3_SHIFT ? d << 31 - 3 < 0 : (d & 8) != 0) && mfpInnerRequest[11] != mfpInnerAcknowledged[11] ||
        (XEiJ.TEST_BIT_2_SHIFT ? d << 31 - 2 < 0 : (d & 4) != 0) && mfpInnerRequest[10] != mfpInnerAcknowledged[10] ||
        (XEiJ.TEST_BIT_1_SHIFT ? d << 31 - 1 < 0 : (d & 2) != 0) && mfpInnerRequest[ 9] != mfpInnerAcknowledged[ 9] ||
        (XEiJ.TEST_BIT_0_SHIFT ? d << 31 - 0 < 0 : (d & 1) != 0) && mfpInnerRequest[ 8] != mfpInnerAcknowledged[ 8]) {
      if (MFP_DELAYED_INTERRUPT) {
        XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
      } else {
        XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
      }
    }
  }  //mfpWriteIMRA

  public static void mfpWriteIMRB (int d) {
    mfpImr = (mfpImr & ~0xff) | (d & 0xff);
    //MFP_IMRBのビットに1を書き込んだときMFP_IPRBの該当ビットが1ならば割り込み発生
    if ((byte) d < 0 && mfpInnerRequest[ 7] != mfpInnerAcknowledged[ 7] ||  //(0b10000000 & d) != 0
        d << 31 - 6 < 0 && mfpInnerRequest[ 6] != mfpInnerAcknowledged[ 6] ||  //(0b01000000 & d) != 0
        d << 31 - 5 < 0 && mfpInnerRequest[ 5] != mfpInnerAcknowledged[ 5] ||  //(0b00100000 & d) != 0
        d << 31 - 4 < 0 && mfpInnerRequest[ 4] != mfpInnerAcknowledged[ 4] ||  //(0b00010000 & d) != 0
        (XEiJ.TEST_BIT_3_SHIFT ? d << 31 - 3 < 0 : (d & 8) != 0) && mfpInnerRequest[ 3] != mfpInnerAcknowledged[ 3] ||
        (XEiJ.TEST_BIT_2_SHIFT ? d << 31 - 2 < 0 : (d & 4) != 0) && mfpInnerRequest[ 2] != mfpInnerAcknowledged[ 2] ||
        (XEiJ.TEST_BIT_1_SHIFT ? d << 31 - 1 < 0 : (d & 2) != 0) && mfpInnerRequest[ 1] != mfpInnerAcknowledged[ 1] ||
        (XEiJ.TEST_BIT_0_SHIFT ? d << 31 - 0 < 0 : (d & 1) != 0) && mfpInnerRequest[ 0] != mfpInnerAcknowledged[ 0]) {
      if (MFP_DELAYED_INTERRUPT) {
        XEiJ.mpuDIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
      } else {
        XEiJ.mpuIRR |= XEiJ.MPU_MFP_INTERRUPT_MASK;
      }
    }
  }  //mfpWriteIMRB

  public static void mfpWriteTACR (int d) {
    boolean prevEventcount = mfpTaEventcount;
    int prevPrescale = mfpTaPrescale;
    mfpTaEventcount = (d & 0x08) != 0;
    mfpTaPrescale = d & 0x07;
    if (mfpTaEventcount && mfpTaPrescale != 0) {  //パルス幅計測モードはキャンセル
      mfpTaEventcount = false;
      mfpTaPrescale = 0;
    }
    if (prevEventcount != mfpTaEventcount || prevPrescale != mfpTaPrescale) {  //変更あり
      if (!prevEventcount && prevPrescale != 0) {  //前回はディレイモード
        mfpTaCurrent = mfpTaInitial - (int) (Math.floor ((double) (XEiJ.mpuClockTime - mfpTaStart) / mfpTaDelta) % mfpTaInitial);  //現在の値
        mfpTaClock = XEiJ.FAR_FUTURE;
        TickerQueue.tkqRemove (mfpTaTicker);  //割り込みを終了する
      }
      if (!mfpTaEventcount && mfpTaPrescale != 0) {  //今回はディレイモード
        mfpTaDelta = MFP_DELTA[mfpTaPrescale];  //今回のデルタ
        if (!prevEventcount && prevPrescale != 0) {  //前回もディレイモード。プリスケールの変更のみ
          long prevDelta = MFP_DELTA[prevPrescale];  //前回のデルタ
          mfpTaStart -= (mfpTaDelta - prevDelta) * (mfpTaInitial - mfpTaCurrent);  //デルタが増えた分×カウンタが進んだ分を開始時刻から引く
        } else {  //今回からディレイモード
          mfpTaStart = XEiJ.mpuClockTime;  //現在時刻から開始する
        }
        mfpTaClock = mfpTaStart + mfpTaDelta * mfpTaInitial;  //オーバーフローする時刻
        TickerQueue.tkqAdd (mfpTaTicker, mfpTaClock);  //割り込みを開始する
      }
      if (!mfpTaEventcount && mfpTaPrescale == 0) {  //停止
        mfpTaData = mfpTaCurrent & 0xff;
      }
    }
  }  //mfpWriteTACR

  public static void mfpWriteTBCR (int d) {
    boolean prevEventcount = mfpTbEventcount;
    int prevPrescale = mfpTbPrescale;
    mfpTbEventcount = (d & 0x08) != 0;
    mfpTbPrescale = d & 0x07;
    mfpTbKeyboardOn = !mfpTbEventcount && mfpTbPrescale == 0x01 && mfpTbInitial == 0x0d;
    if (mfpTbEventcount && mfpTbPrescale != 0) {  //パルス幅計測モードはキャンセル
      mfpTbEventcount = false;
      mfpTbPrescale = 0;
    }
    if (prevEventcount != mfpTbEventcount || prevPrescale != mfpTbPrescale) {  //変更あり
      if (!prevEventcount && prevPrescale != 0) {  //前回はディレイモード
        mfpTbCurrent = mfpTbInitial - (int) (Math.floor ((double) (XEiJ.mpuClockTime - mfpTbStart) / mfpTbDelta) % mfpTbInitial);  //現在の値
        mfpTbClock = XEiJ.FAR_FUTURE;
        TickerQueue.tkqRemove (mfpTbTicker);  //割り込みを終了する
      }
      if (!mfpTbEventcount && mfpTbPrescale != 0) {  //今回はディレイモード
        mfpTbDelta = MFP_DELTA[mfpTbPrescale];  //今回のデルタ
        if (!prevEventcount && prevPrescale != 0) {  //前回もディレイモード。プリスケールの変更のみ
          long prevDelta = MFP_DELTA[prevPrescale];  //前回のデルタ
          mfpTbStart -= (mfpTbDelta - prevDelta) * (mfpTbInitial - mfpTbCurrent);  //デルタが増えた分×カウンタが進んだ分を開始時刻から引く
        } else {  //今回からディレイモード
          mfpTbStart = XEiJ.mpuClockTime;  //現在時刻から開始する
        }
        mfpTbClock = mfpTbStart + mfpTbDelta * mfpTbInitial;  //オーバーフローする時刻
        TickerQueue.tkqAdd (mfpTbTicker, mfpTbClock);  //割り込みを開始する
      }
      if (!mfpTbEventcount && mfpTbPrescale == 0) {  //停止
        mfpTbData = mfpTbCurrent & 0xff;
      }
    }
  }  //mfpWriteTBCR

  public static void mfpWriteTCDCR (int d) {
    int prevPrescale = mfpTcPrescale;
    mfpTcPrescale = d >> 4 & 0x07;
    if (prevPrescale != mfpTcPrescale) {  //変更あり
      if (prevPrescale != 0) {  //前回はディレイモード
        mfpTcCurrent = mfpTcInitial - (int) (Math.floor ((double) (XEiJ.mpuClockTime - mfpTcStart) / mfpTcDelta) % mfpTcInitial);  //現在の値
        mfpTcClock = XEiJ.FAR_FUTURE;
        TickerQueue.tkqRemove (mfpTcTicker);  //割り込みを終了する
      }
      if (mfpTcPrescale != 0) {  //今回はディレイモード
        mfpTcDelta = MFP_DELTA[mfpTcPrescale];  //今回のデルタ
        if (prevPrescale != 0) {  //前回もディレイモード。プリスケールの変更のみ
          long prevDelta = MFP_DELTA[prevPrescale];  //前回のデルタ
          mfpTcStart -= (mfpTcDelta - prevDelta) * (mfpTcInitial - mfpTcCurrent);  //デルタが増えた分×カウンタが進んだ分を開始時刻から引く
        } else {  //今回からディレイモード
          mfpTcStart = XEiJ.mpuClockTime;  //現在時刻から開始する
        }
        mfpTcClock = mfpTcStart + mfpTcDelta * mfpTcInitial;  //オーバーフローする時刻
        TickerQueue.tkqAdd (mfpTcTicker, mfpTcClock);  //割り込みを開始する
      } else {  //停止
        mfpTcData = mfpTcCurrent & 0xff;
      }
    }
    prevPrescale = mfpTdPrescale;
    mfpTdPrescale = d & 0x07;
    if (prevPrescale != mfpTdPrescale) {  //変更あり
      if (prevPrescale != 0) {  //前回はディレイモード
        mfpTdCurrent = mfpTdInitial - (int) (Math.floor ((double) (XEiJ.mpuClockTime - mfpTdStart) / mfpTdDelta) % mfpTdInitial);  //現在の値
        mfpTdClock = XEiJ.FAR_FUTURE;
        TickerQueue.tkqRemove (mfpTdTicker);  //割り込みを終了する
      }
      if (mfpTdPrescale != 0) {  //今回はディレイモード
        mfpTdDelta = MFP_DELTA[mfpTdPrescale];  //今回のデルタ
        if (prevPrescale != 0) {  //前回もディレイモード。プリスケールの変更のみ
          long prevDelta = MFP_DELTA[prevPrescale];  //前回のデルタ
          mfpTdStart -= (mfpTdDelta - prevDelta) * (mfpTdInitial - mfpTdCurrent);  //デルタが増えた分×カウンタが進んだ分を開始時刻から引く
        } else {  //今回からディレイモード
          mfpTdStart = XEiJ.mpuClockTime;  //現在時刻から開始する
        }
        mfpTdClock = mfpTdStart + mfpTdDelta * mfpTdInitial;  //オーバーフローする時刻
        TickerQueue.tkqAdd (mfpTdTicker, mfpTdClock);  //割り込みを開始する
      } else {  //停止
        mfpTdData = mfpTdCurrent & 0xff;
      }
    }
  }  //mfpWriteTCDCR

  public static void mfpWriteTADR (int d) {
    if (mfpTaEventcount || mfpTaPrescale == 0) {  //イベントカウントモードまたは停止中
      mfpTaInitial = ((d - 1) & 0xff) + 1;  //初期値
      mfpTaCurrent = mfpTaInitial;
      if (!mfpTaEventcount) {  //停止中
        mfpTaData = mfpTaInitial & 0xff;
      }
    } else {  //ディレイモード
      int prevInitial = mfpTaInitial;  //前回の初期値
      mfpTaInitial = ((d - 1) & 0xff) + 1;  //今回の初期値
      mfpTaStart -= mfpTaDelta * (mfpTaInitial - prevInitial);  //デルタ×初期値が増えた分を開始時刻から引く
    }
  }  //mfpWriteTADR

  public static void mfpWriteTBDR (int d) {
    if (mfpTbEventcount || mfpTbPrescale == 0) {  //イベントカウントモードまたは停止中
      mfpTbInitial = ((d - 1) & 0xff) + 1;  //初期値
      mfpTbCurrent = mfpTbInitial;
      if (!mfpTbEventcount) {  //停止中
        mfpTbData = mfpTbInitial & 0xff;
      }
    } else {  //ディレイモード
      int prevInitial = mfpTbInitial;  //前回の初期値
      mfpTbInitial = ((d - 1) & 0xff) + 1;  //今回の初期値
      mfpTbStart -= mfpTbDelta * (mfpTbInitial - prevInitial);  //デルタ×初期値が増えた分を開始時刻から引く
    }
    mfpTbKeyboardOn = !mfpTbEventcount && mfpTbPrescale == 0x01 && mfpTbInitial == 0x0d;
  }  //mfpWriteTBDR

  public static void mfpWriteTCDR (int d) {
    if (mfpTcPrescale == 0) {  //停止中
      mfpTcInitial = ((d - 1) & 0xff) + 1;  //初期値
      mfpTcCurrent = mfpTcInitial;
      mfpTcData = mfpTcInitial & 0xff;
    } else {  //ディレイモード
      int prevInitial = mfpTcInitial;  //前回の初期値
      mfpTcInitial = ((d - 1) & 0xff) + 1;  //今回の初期値
      mfpTcStart -= mfpTcDelta * (mfpTcInitial - prevInitial);  //デルタ×初期値が増えた分を開始時刻から引く
    }
  }  //mfpWriteTCDR

  public static void mfpWriteTDDR (int d) {
    if (mfpTdPrescale == 0) {  //停止中
      mfpTdInitial = ((d - 1) & 0xff) + 1;  //初期値
      mfpTdCurrent = mfpTdInitial;
      mfpTdData = mfpTdInitial & 0xff;
    } else {  //ディレイモード
      int prevInitial = mfpTdInitial;  //前回の初期値
      mfpTdInitial = ((d - 1) & 0xff) + 1;  //今回の初期値
      mfpTdStart -= mfpTdDelta * (mfpTdInitial - prevInitial);  //デルタ×初期値が増えた分を開始時刻から引く
    }
  }  //mfpWriteTDDR

  public static void mfpWriteUCR (int d) {
    mfpUCR = d & 0b11111110;
    mfpTSRBufferFullTime = (
      mfpTbDelta * mfpTbInitial *  //Timer-Bの1周期。TBOが反転する間隔
      2 *  //TBOの1周期
      ((mfpUCR & 0x80) != 0 ? 16 : 1) *  //クロックモード
      ((1 * 2) +  //スタートビット
       ((8 - ((mfpUCR >> 5) & 3)) * 2) +  //キャラクタ長
       (((mfpUCR >> 3) & 3) + 1)) / 2);  //ストップビット
  }  //mfpWriteUCR

  public static void mfpWriteUDR (int d) {
    if (mfpTbKeyboardOn) {
      if ((byte) d < 0) {  //LEDの状態
        Keyboard.kbdSetLedStatus (d);
      } else if ((d & 0xf8) == 0x40) {  //MSCTRL
      } else if ((d & 0xf8) == 0x48) {  //ロック
      } else if ((d & 0xfc) == 0x54) {  //LEDの明るさ
        Keyboard.kbdSetLedBrightness (d);
      } else if ((d & 0xfc) == 0x58) {  //テレビコントロール
      } else if ((d & 0xfc) == 0x5c) {  //OPT.2キーによるテレビコントロール
      } else if ((d & 0xf0) == 0x60) {  //リピート開始時間
        Keyboard.kbdSetRepeatDelay (0x0f & d);
      } else if ((d & 0xf0) == 0x70) {  //リピート間隔
        Keyboard.kbdSetRepeatInterval (0x0f & d);
      }
    }
    mfpTSRBufferFullEnd = XEiJ.mpuClockTime + mfpTSRBufferFullTime;
  }  //mfpWriteUDR



  //mfpIoldsTransition ()
  //  IOLDSの立ち上がり
  public static void mfpIoldsTransition () {
    mfpIoldsTime = XEiJ.mpuClockTime;
    if (mfpTaEventcount) {
      mfpTaData = mfpTaCurrent & 0xff;
    }
    if (mfpTbEventcount) {
      mfpTbData = mfpTbCurrent & 0xff;
    }
  }  //mfpIoldsTransition



}  //class MC68901



