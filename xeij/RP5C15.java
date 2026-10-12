//========================================================================================
//  RP5C15.java
//    en:RTC -- Real-Time Clock
//    ja:RTC -- リアルタイムクロック
//  Copyright (C) 2003-2026 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//  カウンタ
//    日時をSystem.currentTimeMillis()からのずれとして記憶する
//      ずれの初期値はデフォルトのタイムゾーンのオフセット
//    年カウンタの起点は常に1980年とする
//      X68000のハードウェアが提供する時計なのだからOSによって異なる起点を使用することがあってはならない
//      2079年まで問題なく使用できるのだから起点を変更する必要はない
//    年カウンタの起点が固定されているので閏年カウンタへの書き込みは無効
//      閏年カウンタの値は常に年カウンタの値を4で割った余りに等しい
//      年カウンタに書き込むと閏年カウンタも更新される
//    カウンタとホストの時計の同期はmpuTask.run()で最初にカウンタがアクセスされたときに行う
//      2回目以降はカウンタの値を更新しない
//      mpuTask.run()は10ms間隔で動作するので、ホストの時計の秒針が動いてからRTCの秒カウンタが更新されるまでに最大で10msかかる
//      mpuTask.run()の動作は不連続なので、カウンタが参照される度にホストの時計を読みに行ったとしてもMPUのクロックとRTCの進み方に最大で10msのずれが生じることに変わりはない
//      正規化されていない日時を書き込んだとき、更新後は正規化された日時が読み出される
//! 以下は未対応
//  アラーム
//    アラームが無効のとき、rtcClock=FAR_FUTURE
//    アラームが有効のとき、rtcClock=直近の発動日時(歴通ミリ秒)
//    mpuTask.run()の先頭でrtcClock<=System.currentTimeMillis()ならばアラームを発動する
//----------------------------------------------------------------------------------------

package xeij;

import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,TimeZone,Timer,TimerTask,TreeMap

public class RP5C15 {

  public static final boolean RTC_DEBUG_TRACE = false;
  public static final boolean RTC_DEBUG_WRITE = false;
  public static final boolean RTC_DEBUG_GAP = false;

  //RP5C15のレジスタ

  //レジスタバンク0
  //  秒カウンタ
  //    00..59 (BCD)
  public static final int RTC_0_SECO0  = 0x01;  //0x00e8a001  bit3-0  1秒カウンタ
  public static final int RTC_0_SECO1  = 0x03;  //0x00e8a003  bit2-0  10秒カウンタ
  //  分カウンタ
  //    00..59 (BCD)
  public static final int RTC_0_MINU0  = 0x05;  //0x00e8a005  bit3-0  1分カウンタ
  public static final int RTC_0_MINU1  = 0x07;  //0x00e8a007  bit2-0  10分カウンタ
  //  時カウンタ
  //    00..31 (BCD)
  //        hour   0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19 20 21 22 23
  //    12時間計  00 01 02 03 04 05 06 07 08 09 10 11 20 21 22 23 24 25 26 27 28 29 30 31 (BCD)
  //    24時間計  00 01 02 03 04 05 06 07 08 09 10 11 12 13 14 15 16 17 18 19 20 21 22 23 (BCD)
  public static final int RTC_0_HOUR0  = 0x09;  //0x00e8a009  bit3-0  1時カウンタ
  public static final int RTC_0_HOUR1  = 0x0b;  //0x00e8a00b  bit1    12時間計のとき12時カウンタ(0=AM,1=PM)
  //                                                                  24時間計のとき20時カウンタ
  //                                                          bit0    10時カウンタ
  //  曜日カウンタ
  //    0..6
  //    7日でオーバーフローする日カウンタ
  //    日カウンタと同時に初期化する必要があり、日カウンタと同時にカウントアップする
  //    0..6のどれが日曜日なのかは初期化したときに決まる
  //    X68000では0=日曜日で初期化されるが、RTC側で0=日曜日と決まっているわけではない
  public static final int RTC_0_WDAY   = 0x0d;  //0x00e8a00d  bit2-0  曜日カウンタ
  //  日カウンタ
  //    01..31 (BCD)。01=1日
  //    年カウンタが4の倍数になっているかどうかとは関係なく、閏年カウンタが0のときに2月を29日までカウントする
  //    閏年カウンタ  月カウンタ  01 02 03 04 05 06 07 08 09 10 11 12
  //          0                   31 29 31 30 31 30 31 31 30 31 30 31
  //          1       日カウンタ  31 28 31 30 31 30 31 31 30 31 30 31
  //          2         の上限    31 28 31 30 31 30 31 31 30 31 30 31
  //          3                   31 28 31 30 31 30 31 31 30 31 30 31
  public static final int RTC_0_MDAY0  = 0x0f;  //0x00e8a00f  bit3-0  1日カウンタ
  public static final int RTC_0_MDAY1  = 0x11;  //0x00e8a011  bit1-0  10日カウンタ
  //  月カウンタ
  //    01..12 (BCD)。01=1月
  public static final int RTC_0_MONT0  = 0x13;  //0x00e8a013  bit3-0  1月カウンタ
  public static final int RTC_0_MONT1  = 0x15;  //0x00e8a015  bit0    10月カウンタ
  //  年カウンタ
  //    00..99 (BCD)
  //    ただの12ヶ月カウンタ
  //    閏年カウンタと同時に初期化する必要があり、閏年カウンタと同時にカウントアップする
  //    00が西暦何年なのかは初期化したときに決まる
  //    X68000では00=1980年で初期化されるが、RTC側で00=1980年と決まっているわけではない
  public static final int RTC_0_YEAR0  = 0x17;  //0x00e8a017  bit3-0  1年カウンタ
  public static final int RTC_0_YEAR1  = 0x19;  //0x00e8a019  bit3-0  10年カウンタ

  //レジスタバンク1
  //  CLKOUTセレクタ
  //    0..7。0=点灯,1=16384Hz,2=1024Hz,3=128Hz,4=16Hz,5=1Hz,6=1/64Hz,7=消灯
  //    X68000ではRP5C15のCLKOUT信号がTIMER-LEDに接続されており、このレジスタはTIMER-LEDの状態を制御するために使われている
  //    IOCS _ALARMMODやIOCS _ALARMSETでアラームを許可したとき、0が書き込まれてTIMER-LEDが点灯する
  //    IOCS _ALARMMODでアラームを禁止したとき、7が書き込まれてTIMER-LEDが消灯する
  //    アラームで起動したとき、5が書き込まれてTIMER-LEDが点滅する
  //    KRAMD.SYSは転送開始時と転送終了時に7をEORすることでTIMER-LEDをRAMディスクのアクセスランプにしている
  //    #1000ではCLKOUTがMFPのGPIP5にも接続されることになっていたが、X68000では接続されていない
  public static final int RTC_1_CLKOUT = 0x01;  //0x00e8a001  bit2-0  CLKOUTセレクタ
  //  アジャスト
  //    1=秒を29捨30入する
  public static final int RTC_1_ADJUST = 0x03;  //0x00e8a003  bit0    アジャスト
  //  アラームレジスタ
  //    1分カウンタ～10日カウンタがアラーム1分レジスタ～アラーム10日レジスタとすべて一致したときアラーム出力が始まる
  //    アラームリセット後に一度も書き込まれていないアラームレジスタはdon't careで常に一致していると見なされる
  //    アラームリセットすると全項目がdon't careになりアラーム出力が始まってしまうので、
  //    通常はアラーム出力をOFFにしてからアラームリセットする
  public static final int RTC_1_MINU0  = 0x05;  //0x00e8a005  bit3-0  アラーム1分レジスタ
  public static final int RTC_1_MINU1  = 0x07;  //0x00e8a007  bit2-0  アラーム10分レジスタ
  public static final int RTC_1_HOUR0  = 0x09;  //0x00e8a009  bit3-0  アラーム1時レジスタ
  public static final int RTC_1_HOUR1  = 0x0b;  //0x00e8a00b  bit2-0  アラーム10時レジスタ
  public static final int RTC_1_WDAY   = 0x0d;  //0x00e8a00d  bit2-0  アラーム曜日レジスタ
  public static final int RTC_1_MDAY0  = 0x0f;  //0x00e8a00f  bit3-0  アラーム1日カウンタ
  public static final int RTC_1_MDAY1  = 0x11;  //0x00e8a011  bit1-0  アラーム10日カウンタ
  //  12時間計/24時間計セレクタ
  //    0..1。0=12時間計,1=24時間計
  public static final int RTC_1_RULE   = 0x15;  //0x00e8a015  bit0    12時間計/24時間計セレクタ
  //  閏年カウンタ
  //    0..3。0=今年が閏年,1=3年後が閏年,2=2年後が閏年,3=来年が閏年
  //    4年でオーバーフローする年カウンタ
  //    年カウンタと同時に初期化する必要があり、年カウンタと同時にカウントアップする
  //    曜日カウンタと違ってRTC側で0=閏年と決まっている
  //    年カウンタが4の倍数になっているかどうかとは関係なく、閏年カウンタが0のときに2月を29日までカウントする
  public static final int RTC_1_LEAP   = 0x17;  //0x00e8a017  bit1-0  閏年カウンタ

  //バンク0/1共通
  //  モードレジスタ
  public static final int RTC_MODE     = 0x1b;  //0x00e8a01b  bit3    0=秒以後のカウンタ停止,1=計時開始
  //                                                          bit2    0=アラーム出力OFF,1=アラーム出力ON
  //                                                          bit0    0=バンク0,1=バンク1
  //  テストレジスタ(write-only)
  //    参考
  //      https://twitter.com/kugimoto0715/status/745570562102046728
  public static final int RTC_TEST     = 0x1d;  //0x00e8a01d  bit3    1=1年カウンタに16384Hzを入力する
  //                                                          bit2    1=1日カウンタに16384Hzを入力する
  //                                                          bit1    1=1分カウンタに16384Hzを入力する
  //                                                          bit0    1=1秒カウンタに16384Hzを入力する
  //  リセットレジスタ(write-only)
  public static final int RTC_RESET    = 0x1f;  //0x00e8a01f  bit3    0=1Hz ON
  //                                                          bit2    0=16Hz ON
  //                                                          bit1    1=秒以前の分周段リセット
  //                                                          bit0    1=アラームリセット

  public static int rtcRule;  //0=12時間計,1=24時間計
  public static int rtcMove;  //0=停止中,8=動作中
  public static int rtcAlarm;  //0=アラーム出力OFF,4=アラーム出力ON
  public static int rtcBank;  //0=バンク0,1=バンク1
  public static final byte[] rtcRegBank0 = new byte[32];  //レジスタバンク0
  public static final byte[] rtcRegBank1 = new byte[32];  //レジスタバンク1
  public static byte[] rtcRegCurrent;  //現在選択されているレジスタバンク。rtcBank==0?rtcRegBank0:rtcRegBank1
  public static int rtcTest;  //0=通常動作,1..15=テスト動作のスケール
  public static MemoryMappedDevice rtcFirst;  //rtcTest==0?MemoryMappedDevice.MMD_RTC_FIRST:MemoryMappedDevice.MMD_RTC_TEST
  public static long rtcTestStartCmil;  //現在のスケールでテスト動作を開始したときの日時カウンタの値の歴通ミリ秒
  public static long rtcTestStartTime;  //現在のスケールでテスト動作を開始したときのXEiJ.mpuClockTime

  //  rtcWeekGap
  //    曜日のずれ
  //    0..6
  //    これをcdayに加えて7で割った余りが曜日カウンタの値と一致するように調整する
  //    (cday+4)%7==0のとき日曜日なので初期値は4
  //    曜日カウンタに書き込まれたとき増分を加え、日付カウンタに書き込まれたとき増分を引く
  public static int rtcWeekGap;  //曜日のずれ
  //  rtcCmilGap
  //    日時のずれ
  //    rtcCmil-System.currentTimeMillis()
  //    通常動作で動作中のときだけ有効
  //    初期値はローカルタイムを求めるときUTCに加えるオフセット
  //      夏時間を考慮しない場合はTimeZone.getDefault ().getRawOffset()と等しい
  //      JSTのとき1000*60*60*9=32400000
  //    日時カウンタに書き込まれたとき増分にスケールを掛けた値を加える
  //    通常動作で停止中から動作中に移行するとき日時カウンタの値からホストの日時を引いた値を設定する
  public static long rtcInitialCmilGap;  //日時のずれの初期値
  public static long rtcCmilGap;  //日時のずれ
  public static long rtcCmil;  //日時カウンタの値の歴通ミリ秒
  public static int rtcCday;  //日付カウンタの値の歴通日
  public static int rtcDsec;  //時刻カウンタの値の日通秒

  //rtcInit ()
  //  RTCを初期化する
  public static void rtcInit () {
    if (RTC_DEBUG_TRACE) {
      System.out.printf ("rtcInit()\n");
    }
    rtcRule = 1;  //24時間計
    rtcMove = 8;  //動作
    rtcAlarm = 0;  //アラーム出力OFF
    rtcHzReset ();
    rtcBank = 0;  //バンク0
    rtcRegCurrent = rtcBank == 0 ? rtcRegBank0 : rtcRegBank1;
    rtcTest = 0;  //通常動作
    rtcFirst = (rtcTest == 0 ?
                !XEiJ.currentModel.isX68030 () ?
                MemoryMappedDevice.MMD_RTC_FIRST_00 :
                MemoryMappedDevice.MMD_RTC_FIRST_30 :
                !XEiJ.currentModel.isX68030 () ?
                MemoryMappedDevice.MMD_RTC_TEST_00 :
                MemoryMappedDevice.MMD_RTC_TEST_30);
    XEiJ.busSuper (rtcFirst, 0x00e8a000, 0x00e8c000);
    rtcTestStartCmil = 0;
    rtcTestStartTime = 0;
    if (RTC_DEBUG_TRACE) {
      System.out.printf ("  rtcRule=%d\n", rtcRule);
      System.out.printf ("  rtcMove=%d\n", rtcMove);
      System.out.printf ("  rtcBank=%d\n", rtcBank);
      System.out.printf ("  rtcTest=%d\n", rtcTest);
    }
    //  00:00:00
    rtcRegBank0[RTC_0_SECO0] = 0;
    rtcRegBank0[RTC_0_SECO1] = 0;
    rtcRegBank0[RTC_0_MINU0] = 0;
    rtcRegBank0[RTC_0_MINU1] = 0;
    rtcRegBank0[RTC_0_HOUR0] = 0;
    rtcRegBank0[RTC_0_HOUR1] = 0;
    //  1980-01-01
    rtcRegBank0[RTC_0_WDAY] = 2;  //火曜日
    rtcRegBank0[RTC_0_MDAY0] = 1;
    rtcRegBank0[RTC_0_MDAY1] = 0;
    rtcRegBank0[RTC_0_MONT0] = 1;
    rtcRegBank0[RTC_0_MONT1] = 0;
    rtcRegBank0[RTC_0_YEAR0] = 0;
    rtcRegBank0[RTC_0_YEAR1] = 0;
    rtcRegBank1[RTC_1_CLKOUT] = 0;
    rtcRegBank1[RTC_1_ADJUST] = 0;
    rtcRegBank1[RTC_1_MINU0] = 16;
    rtcRegBank1[RTC_1_MINU1] = 16;
    rtcRegBank1[RTC_1_HOUR0] = 16;
    rtcRegBank1[RTC_1_HOUR1] = 16;
    rtcRegBank1[RTC_1_WDAY] = 16;
    rtcRegBank1[RTC_1_MDAY0] = 16;
    rtcRegBank1[RTC_1_MDAY1] = 16;
    rtcRegBank1[RTC_1_RULE] = (byte) rtcRule;
    rtcRegBank1[RTC_1_LEAP] = 0;
    rtcRegBank1[RTC_MODE] = (byte) (rtcMove + rtcBank);
    rtcRegBank1[RTC_TEST] = (byte) rtcTest;
    rtcRegBank1[RTC_RESET] = 0;
    rtcWeekGap = 4;  //曜日のずれ
    rtcInitialCmilGap = TimeZone.getDefault ().getOffset (System.currentTimeMillis ());
    rtcCmilGap = rtcInitialCmilGap;  //日時のずれ
    if (RTC_DEBUG_GAP) {
      System.out.printf ("rtcInit  rtcCmilGap=%d\n", rtcCmilGap);
    }
    rtcCmil = 315532800000L;
    rtcCday = 3652;
    rtcDsec = 0;
    if (RTC_DEBUG_TRACE) {
      System.out.printf ("  rtcWeekGap=%d\n", rtcWeekGap);
      System.out.printf ("  rtcCmilGap=%d\n", rtcCmilGap);
      System.out.printf ("  rtcCmil=%d (%s)\n", rtcCmil, DnT.dntSdttmCmil (rtcCmil));
      System.out.printf ("  rtcCday=%d (%s)\n", rtcCday, DnT.dntSdtCday (rtcCday));
      System.out.printf ("  rtcDsec=%d (%s)\n", rtcDsec, DnT.dntStmDsec (rtcDsec));
      rtcDumpReg ();
      System.out.printf ("  3652=%d\n", DnT.dntCdayYearMontMday (1980, 1, 1));
      System.out.printf ("  40176=%d\n", DnT.dntCdayYearMontMday (2079, 12, 31));
      System.out.printf ("  315532800000=%d\n", DnT.dntCmilYearMontMdayHourMinuSecoMill (1980, 1, 1, 0, 0, 0, 0));
      System.out.printf ("  3471292799999=%d\n", DnT.dntCmilYearMontMdayHourMinuSecoMill (2079, 12, 31, 23, 59, 59, 999));
      System.out.printf ("  1980-01-01=%s\n", DnT.dntSdtCday (3652));
      System.out.printf ("  2079-12-31=%s\n", DnT.dntSdtCday (40176));
      System.out.printf ("  1980-01-01 00:00:00.999=%s\n", DnT.dntSdttmCmil (315532800000L));
      System.out.printf ("  2079-12-31 23:59:59.999=%s\n", DnT.dntSdttmCmil (3471292799999L));
    }
  }  //rtcInit()

  //リセット
  public static void rtcReset () {
    if (RTC_DEBUG_TRACE) {
      System.out.printf ("rtcReset()\n");
    }
    rtcFirst = (rtcTest == 0 ?
                !XEiJ.currentModel.isX68030 () ?
                MemoryMappedDevice.MMD_RTC_FIRST_00 :
                MemoryMappedDevice.MMD_RTC_FIRST_30 :
                !XEiJ.currentModel.isX68030 () ?
                MemoryMappedDevice.MMD_RTC_TEST_00 :
                MemoryMappedDevice.MMD_RTC_TEST_30);
    XEiJ.busSuper (rtcFirst, 0x00e8a000, 0x00e8c000);
  }  //rtcReset()

  //rtcSetByHost ()
  //  ホストの時計に合わせる
  public static void rtcSetByHost () {
    if (RTC_DEBUG_TRACE) {
      System.out.printf ("rtcSetByHost()\n");
    }
    XEiJ.tmrTimer.schedule (new TimerTask () {
      @Override public void run () {
        if (rtcTest != 0) {
          //テスト動作→通常動作
          rtcTest = 0;
          rtcFirst = (!XEiJ.currentModel.isX68030 () ?
                      MemoryMappedDevice.MMD_RTC_FIRST_00 :
                      MemoryMappedDevice.MMD_RTC_FIRST_30);
          XEiJ.busSuper (rtcFirst, 0x00e8a000, 0x00e8c000);
          if (RTC_DEBUG_TRACE) {
            System.out.printf ("  rtcTest=%d\n", rtcTest);
          }
        }
        if (rtcMove == 0) {
          //停止中→動作中
          rtcMove = 8;
          if (RTC_DEBUG_TRACE) {
            System.out.printf ("  rtcMove=%d\n", rtcMove);
          }
        }
        rtcCmilGap = rtcInitialCmilGap;  //日時のずれ
        if (RTC_DEBUG_GAP) {
          System.out.printf ("rtcSetByHost  rtcCmilGap=%d\n", rtcCmilGap);
        }
        if (RTC_DEBUG_TRACE) {
          System.out.printf ("  rtcCmilGap=%d\n", rtcCmilGap);
        }
        rtcUpdate ();
      }
    }, 10L);
  }  //rtcSetByHost()

  public static final long[] RTC_TEST_FREQ = {
    //                                     年       日    分   秒
    0L,                                                               //$00
    XEiJ.TMR_FREQ      / (16384L * (                           1L)),  //$01  1秒カウンタ以上
    XEiJ.TMR_FREQ      / (16384L * (                      1L     )),  //$02  1分カウンタ以上
    XEiJ.TMR_FREQ      / (16384L * (                     60L + 1L)),  //$03  1秒カウンタ以上
    XEiJ.TMR_FREQ      / (16384L * (                1L           )),  //$04  1日カウンタ以上
    XEiJ.TMR_FREQ      / (16384L * (            86400L +       1L)),  //$05  1秒カウンタ以上
    XEiJ.TMR_FREQ      / (16384L * (             1440L +  1L     )),  //$06  1分カウンタ以上
    XEiJ.TMR_FREQ      / (16384L * (            86400L + 60L + 1L)),  //$07  1秒カウンタ以上
    XEiJ.TMR_FREQ      / (16384L * (       1L                    )),  //$08  1年カウンタ以上
    XEiJ.TMR_FREQ      / (16384L * (31557600L +                1L)),  //$09  1秒カウンタ以上
    XEiJ.TMR_FREQ      / (16384L * (  525960L +           1L     )),  //$0A  1分カウンタ以上
    XEiJ.TMR_FREQ      / (16384L * (31557600L +          60L + 1L)),  //$0B  1秒カウンタ以上
    XEiJ.TMR_FREQ * 4L / (16384L * (    1461L +     4L           )),  //$0C  1日カウンタ以上。365.25+1=1461/4+1=(1461+4)/4
    XEiJ.TMR_FREQ      / (16384L * (31557600L + 86400L +       1L)),  //$0D  1秒カウンタ以上
    XEiJ.TMR_FREQ      / (16384L * (  525960L +  1440L +  1L     )),  //$0E  1分カウンタ以上
    XEiJ.TMR_FREQ      / (16384L * (31557600L + 86400L + 60L + 1L)),  //$0F  1秒カウンタ以上
  };
  public static final long[] RTC_TEST_SCALE = {
    //         年          日       分      秒
    0L,                                           //$00
    (                                    1000L),  //$01  1秒カウンタ以上
    (                           60000L        ),  //$02  1分カウンタ以上
    (                                    1000L),  //$03  1秒カウンタ以上
    (               86400000L                 ),  //$04  1日カウンタ以上
    (                                    1000L),  //$05  1秒カウンタ以上
    (                           60000L        ),  //$06  1分カウンタ以上
    (                                    1000L),  //$07  1秒カウンタ以上
    (31557600000L                             ),  //$08  1年カウンタ以上
    (                                    1000L),  //$09  1秒カウンタ以上
    (                           60000L        ),  //$0A  1分カウンタ以上
    (                                    1000L),  //$0B  1秒カウンタ以上
    (               86400000L                 ),  //$0C  1日カウンタ以上
    (                                    1000L),  //$0D  1秒カウンタ以上
    (                           60000L        ),  //$0E  1分カウンタ以上
    (                                    1000L),  //$0F  1秒カウンタ以上
  };

  //rtcTestUpdate ()
  //  カウンタを更新する(テスト動作)
  public static void rtcTestUpdate () {
    if (RTC_DEBUG_TRACE) {
      System.out.printf ("rtcTestUpdate()\n");
    }
    if (rtcMove != 0) {  //動作中
      rtcSetCmil (rtcTestStartCmil +
                  ((XEiJ.mpuClockTime - rtcTestStartTime) / RTC_TEST_FREQ[rtcTest]) * RTC_TEST_SCALE[rtcTest]);
    }
  }  //rtcTestUpdate()

  //rtcUpdate ()
  //  カウンタを更新する(通常動作)
  //  mpuTask.run()で最初にカウンタがアクセスされたときに呼び出す
  //    00100000  42A7                  clr.l   -(sp)                       Bｧ
  //    00100002  FF20                  DOS     _SUPER                      . 
  //    00100004  4A3900E8A001          tst.b   $00E8A001.l                 J9.陟.
  //    0010000A  60F8                  bra.s   $00100004                   `.
  //  mew 100000 42a7 ff20 4a39 00e8 a001 60f8
  //  g=100000
  public static void rtcUpdate () {
    if (RTC_DEBUG_TRACE) {
      System.out.printf ("rtcUpdate()\n");
      XEiJ.prgPrintStackTrace ();
    }
    if (rtcMove != 0) {  //動作中
      rtcSetCmil (System.currentTimeMillis () + rtcCmilGap);
    }
  }  //rtcUpdate()

  //rtcSetCmil (cmil)
  //  カウンタに歴通ミリ秒を設定する
  public static void rtcSetCmil (long cmil) {
    if (RTC_DEBUG_TRACE) {
      System.out.printf ("rtcSetCmil(%d)\n", cmil);
    }
    //               日時               cday       cmil
    //  1980-01-01 (Tue) 00:00:00.000   3652   315532800000
    //  2079-12-31 (Sun) 23:59:59.999  40176  3471292799999
    //  2080-01-01 (Mon) 00:00:00.000  40177  3471292800000
    long shift = DnT.dntFdiv (cmil - 315532800000L, 3471292800000L - 315532800000L) * (3471292800000L - 315532800000L);
    if (shift != 0L) {
      cmil -= shift;  //315532800000..3471292799999
      rtcCmilGap -= shift;
      if (RTC_DEBUG_GAP) {
        System.out.printf ("rtcSetCmil  rtcCmilGap=%d\n", rtcCmilGap);
      }
      if (RTC_DEBUG_TRACE) {
        System.out.printf ("  rtcCmilGap=%d\n", rtcCmilGap);
      }
    }
    if (rtcCmil != cmil) {
      rtcCmil = cmil;  //315532800000..3471292799999
      if (RTC_DEBUG_TRACE) {
        System.out.printf ("  rtcCmil=%d (%s)\n", rtcCmil, DnT.dntSdttmCmil (rtcCmil));
      }
      int cday = DnT.dntCdayCmil (cmil);
      int dsec = DnT.dntDsecCmil (cmil);
      if (rtcCday != cday) {
        rtcCday = cday;  //3652..40176
        if (RTC_DEBUG_TRACE) {
          System.out.printf ("  rtcCday=%d (%s)\n", rtcCday, DnT.dntSdtCday (rtcCday));
        }
        int date = DnT.dntDateCday (cday);  //1980-01-01..2079-12-31
        int t = XEiJ.FMT_BCD4[DnT.dntYearDate (date) - 1980];  //year=00..99
        rtcRegBank0[RTC_0_YEAR1] = (byte) (t >> 4);
        rtcRegBank0[RTC_0_YEAR0] = (byte) (t & 15);
        rtcRegBank1[RTC_1_LEAP] = (byte) (t & 3);
        t = XEiJ.FMT_BCD4[DnT.dntMontDate (date)];  //mont=01..12
        rtcRegBank0[RTC_0_MONT1] = (byte) (t >> 4);
        rtcRegBank0[RTC_0_MONT0] = (byte) (t & 15);
        t = XEiJ.FMT_BCD4[DnT.dntMdayDate (date)];  //mday=01..31
        rtcRegBank0[RTC_0_MDAY1] = (byte) (t >> 4);
        rtcRegBank0[RTC_0_MDAY0] = (byte) (t & 15);
        rtcRegBank0[RTC_0_WDAY] = (byte) DnT.dntFrem (cday + rtcWeekGap, 7);
        if (RTC_DEBUG_TRACE) {
          rtcDumpReg ();
        }
      }  //if rtcCday!=cday
      if (rtcDsec != dsec) {
        rtcDsec = dsec;  //0..86399
        if (RTC_DEBUG_TRACE) {
          System.out.printf ("  rtcDsec=%d (%s)\n", rtcDsec, DnT.dntStmDsec (rtcDsec));
        }
        int time = DnT.dntTimeDsec (dsec);
        int t = DnT.dntHourTime (time);
        t = XEiJ.FMT_BCD4[rtcRule == 0 && 12 <= t ? t + 8 : t];  //hour=00..31
        rtcRegBank0[RTC_0_HOUR1] = (byte) (t >> 4);
        rtcRegBank0[RTC_0_HOUR0] = (byte) (t & 15);
        t = XEiJ.FMT_BCD4[DnT.dntMinuTime (time)];  //minu=00..63
        rtcRegBank0[RTC_0_MINU1] = (byte) (t >> 4);
        rtcRegBank0[RTC_0_MINU0] = (byte) (t & 15);
        t = XEiJ.FMT_BCD4[DnT.dntSecoTime (time)];  //seco=00..63
        rtcRegBank0[RTC_0_SECO1] = (byte) (t >> 4);
        rtcRegBank0[RTC_0_SECO0] = (byte) (t & 15);
        if (RTC_DEBUG_TRACE) {
          rtcDumpReg ();
        }
      }  //if rtcDsec!=dsec
    }  //if rtcCmil!=cmil
  }  //rtcSetCmil(long)

  //rtcDumpReg ()
  public static void rtcDumpReg () {
    System.out.printf ("  L YY-MM-DD W  R HH:MM:SS\n" +
                       "  %01X %01X%01X-%01X%01X-%01X%01X %01X  %01X %01X%01X:%01X%01X:%01X%01X\n",
                       rtcRegBank1[RTC_1_LEAP],
                       rtcRegBank0[RTC_0_YEAR1], rtcRegBank0[RTC_0_YEAR0],
                       rtcRegBank0[RTC_0_MONT1], rtcRegBank0[RTC_0_MONT0],
                       rtcRegBank0[RTC_0_MDAY1], rtcRegBank0[RTC_0_MDAY0],
                       rtcRegBank0[RTC_0_WDAY],
                       rtcRegBank1[RTC_1_RULE],
                       rtcRegBank0[RTC_0_HOUR1], rtcRegBank0[RTC_0_HOUR0],
                       rtcRegBank0[RTC_0_MINU1], rtcRegBank0[RTC_0_MINU0],
                       rtcRegBank0[RTC_0_SECO1], rtcRegBank0[RTC_0_SECO0]);
  }  //rtcDumpReg()

  //rtcAddSeco (seco)
  public static void rtcAddSeco (int seco) {
    if (RTC_DEBUG_TRACE) {
      System.out.printf ("rtcAddSeco(%d)\n", seco);
    }
    long mill = (long) seco * 1000L;
    rtcCmilGap += mill;
    if (RTC_DEBUG_GAP) {
      System.out.printf ("rtcAddSeco  rtcCmilGap=%d\n", rtcCmilGap);
    }
    rtcCmil += mill;
    rtcCday = DnT.dntCdayCmil (rtcCmil);
    rtcDsec = DnT.dntDsecCmil (rtcCmil);
    if (RTC_DEBUG_TRACE) {
      System.out.printf ("  rtcCmilGap=%d\n", rtcCmilGap);
      System.out.printf ("  rtcCmil=%d (%s)\n", rtcCmil, DnT.dntSdttmCmil (rtcCmil));
      System.out.printf ("  rtcCday=%d (%s)\n", rtcCday, DnT.dntSdtCday (rtcCday));
      System.out.printf ("  rtcDsec=%d (%s)\n", rtcDsec, DnT.dntStmDsec (rtcDsec));
    }
    if (rtcTest != 0) {
      rtcTestStartCmil = rtcCmil;
      rtcTestStartTime = XEiJ.mpuClockTime;
      if (RTC_DEBUG_TRACE) {
        System.out.printf ("  rtcTestStartCmil=%d (%s)\n", rtcTestStartCmil, DnT.dntSdttmCmil (rtcTestStartCmil));
        System.out.printf ("  rtcTestStartTime=%d\n", rtcTestStartTime);
      }
    }
  }  //rtcAddSeco(int)

  //rtcAddMday (mday)
  public static void rtcAddMday (int mday) {
    if (RTC_DEBUG_TRACE) {
      System.out.printf ("rtcAddMday(%d)\n", mday);
    }
    rtcAddSeco (mday * 86400);
    rtcWeekGap = DnT.dntFrem (rtcWeekGap - mday, 7);
    if (RTC_DEBUG_TRACE) {
      System.out.printf ("  rtcWeekGap=%d\n", rtcWeekGap);
    }
  }  //rtcAddMday(int)

  //rtcPeekByte (a)
  //  ピークバイト
  public static int rtcPeekByte (int a) {
    int d = rtcRegCurrent[a & 31] & 15;
    if (RTC_DEBUG_TRACE) {
      System.out.printf ("rtcPeekByte(0x%08x)=0x%02x\n", a, d & 255);
    }
    return d;
  }  //rtcPeekByte(int)

  //rtcReadByte (a)
  //  リードバイト
  public static int rtcReadByte (int a) {
    int d = rtcRegCurrent[a & 31] & 15;
    if (RTC_DEBUG_TRACE) {
      System.out.printf ("rtcReadByte(0x%08x)=0x%02x\n", a, d & 255);
    }
    return d;
  }  //rtcReadByte(int)

  public static final String[] RTC_REG_NAME = (
    "one second," +         //0 $01 1秒カウンタ
    "ten seconds," +        //0 $03 10秒カウンタ
    "one minute," +         //0 $05 1分カウンタ
    "ten minutes," +        //0 $07 10分カウンタ
    "one hour," +           //0 $09 1時カウンタ
    "ten hours," +          //0 $0B 10時カウンタ
    "day of week," +        //0 $0D 曜日カウンタ
    "one day," +            //0 $0F 1日カウンタ
    "ten days," +           //0 $11 10日カウンタ
    "one month," +          //0 $13 1月カウンタ
    "ten months," +         //0 $15 10月カウンタ
    "one year," +           //0 $17 1年カウンタ
    "ten years," +          //0 $19 10年カウンタ
    "mode," +               //0 $1B モードレジスタ
    "test," +               //0 $1D テストレジスタ(write-only)
    "reset," +              //0 $1F リセットレジスタ(write-only)
    "clkout," +             //1 $01 CLKOUTセレクタ
    "adjust," +             //1 $03 アジャスト
    "alarm one minute," +   //1 $05 アラーム1分レジスタ
    "alarm ten minutes," +  //1 $07 アラーム10分レジスタ
    "alarm one hour," +     //1 $09 アラーム1時レジスタ
    "alarm ten hours," +    //1 $0B アラーム10時レジスタ
    "alarm day of week," +  //1 $0D アラーム曜日レジスタ
    "alarm one day," +      //1 $0F アラーム1日レジスタ
    "alarm ten days," +     //1 $11 アラーム10日レジスタ
    "," +                   //1 $13
    "twenty four," +        //1 $15 12時間計/24時間計セレクタ
    "leap year," +          //1 $17 閏年カウンタ
    "," +                   //1 $19
    "mode," +               //1 $1B モードレジスタ
    "test," +               //1 $1D テストレジスタ(write-only)
    "reset"                 //1 $1F リセットレジスタ(write-only)
    ).split (",");

  //rtcWriteByte (a, d)
  //  ライトバイト
  public static void rtcWriteByte (int a, int d) {
    if (RTC_DEBUG_TRACE ||
        (RTC_DEBUG_WRITE &&
         (a & 31) != RTC_MODE && (byte) (d & (8 + 4)) != rtcMove + rtcAlarm)) {  //バンク切り替えを除くすべての書き込み
      System.out.printf ("%08X rtcWriteByte@%d(0x%08x(%s),0x%02x)\n",
                         XEiJ.regPC0, rtcBank, a,
                         (a & 1) == 0 ? "" : RTC_REG_NAME[rtcBank << 4 | ((a & 31) >> 1)],
                         d & 255);
    }
    switch (rtcBank << 5 | a & 31) {
      //バンク0
    case 0 << 5 | RTC_0_SECO0:  //0x00e8a001  bit3-0  1秒カウンタ
      d &= 15;  //0..15
      rtcAddSeco (d - rtcRegBank0[RTC_0_SECO0]);
      rtcRegBank0[RTC_0_SECO0] = (byte) d;
      if (RTC_DEBUG_TRACE) {
        rtcDumpReg ();
      }
      return;
    case 0 << 5 | RTC_0_SECO1:  //0x00e8a003  bit2-0  10秒カウンタ
      d &= 7;  //0..7
      rtcAddSeco ((d - rtcRegBank0[RTC_0_SECO1]) * 10);
      rtcRegBank0[RTC_0_SECO1] = (byte) d;
      if (RTC_DEBUG_TRACE) {
        rtcDumpReg ();
      }
      return;
    case 0 << 5 | RTC_0_MINU0:  //0x00e8a005  bit3-0  1分カウンタ
      d &= 15;  //0..15
      rtcAddSeco ((d - rtcRegBank0[RTC_0_MINU0]) * 60);
      rtcRegBank0[RTC_0_MINU0] = (byte) d;
      if (RTC_DEBUG_TRACE) {
        rtcDumpReg ();
      }
      return;
    case 0 << 5 | RTC_0_MINU1:  //0x00e8a007  bit2-0  10分カウンタ
      d &= 7;  //0..7
      rtcAddSeco ((d - rtcRegBank0[RTC_0_MINU1]) * 600);
      rtcRegBank0[RTC_0_MINU1] = (byte) d;
      if (RTC_DEBUG_TRACE) {
        rtcDumpReg ();
      }
      return;
    case 0 << 5 | RTC_0_HOUR0:  //0x00e8a009  bit3-0  1時カウンタ
      d &= 15;  //0..15
      rtcAddSeco ((d - rtcRegBank0[RTC_0_HOUR0]) * 3600);
      rtcRegBank0[RTC_0_HOUR0] = (byte) d;
      if (RTC_DEBUG_TRACE) {
        rtcDumpReg ();
      }
      return;
    case 0 << 5 | RTC_0_HOUR1:  //0x00e8a00b  bit1-0  10時カウンタ
      d &= 3;  //0..3
      rtcAddSeco (((d >> 1) - (rtcRegBank0[RTC_0_HOUR1] >> 1)) * (rtcRule == 0 ? 43200 : 72000) +
                  ((d & 1) - (rtcRegBank0[RTC_0_HOUR1] & 1)) * 36000);
      rtcRegBank0[RTC_0_HOUR1] = (byte) d;
      if (RTC_DEBUG_TRACE) {
        rtcDumpReg ();
      }
      return;
    case 0 << 5 | RTC_0_WDAY:  //0x00e8a00d  bit2-0  曜日カウンタ
      d &= 7;  //0..7
      {
        int t = rtcWeekGap + d - rtcRegBank0[RTC_0_WDAY] + 7;  //1..20
        //perl optdiv.pl 20 7
        //  x/7==x*19>>>7 (0<=x<=26) [20*19==380]
        rtcWeekGap = t - (t * 19 >>> 7) * 7;  //0..6
        rtcRegBank0[RTC_0_WDAY] = (byte) d;
        if (RTC_DEBUG_TRACE) {
          System.out.printf ("  rtcWeekGap=%d\n", rtcWeekGap);
          rtcDumpReg ();
        }
      }
      return;
    case 0 << 5 | RTC_0_MDAY0:  //0x00e8a00f  bit3-0  1日カウンタ
      d &= 15;  //0..15
      rtcAddMday (d - rtcRegBank0[RTC_0_MDAY0]);
      rtcRegBank0[RTC_0_MDAY0] = (byte) d;
      if (RTC_DEBUG_TRACE) {
        rtcDumpReg ();
      }
      return;
    case 0 << 5 | RTC_0_MDAY1:  //0x00e8a011  bit1-0  10日カウンタ
      d &= 3;  //0..3
      rtcAddMday ((d - rtcRegBank0[RTC_0_MDAY1]) * 10);
      rtcRegBank0[RTC_0_MDAY1] = (byte) d;
      if (RTC_DEBUG_TRACE) {
        rtcDumpReg ();
      }
      return;
    case 0 << 5 | RTC_0_MONT0:  //0x00e8a013  bit3-0  1月カウンタ
      d &= 15;  //0..15
      rtcAddMday (DnT.dntCdayYearMontMday (1980 + rtcRegBank0[RTC_0_YEAR1] * 10 + rtcRegBank0[RTC_0_YEAR0],
                                           rtcRegBank0[RTC_0_MONT1] * 10 + d,
                                           rtcRegBank0[RTC_0_MDAY1] * 10 + rtcRegBank0[RTC_0_MDAY0]) - rtcCday);
      rtcRegBank0[RTC_0_MONT0] = (byte) d;
      if (RTC_DEBUG_TRACE) {
        rtcDumpReg ();
      }
      return;
    case 0 << 5 | RTC_0_MONT1:  //0x00e8a015  bit0  10月カウンタ
      d &= 1;  //0..1
      rtcAddMday (DnT.dntCdayYearMontMday (1980 + rtcRegBank0[RTC_0_YEAR1] * 10 + rtcRegBank0[RTC_0_YEAR0],
                                           d * 10 + rtcRegBank0[RTC_0_MONT0],
                                           rtcRegBank0[RTC_0_MDAY1] * 10 + rtcRegBank0[RTC_0_MDAY0]) - rtcCday);
      rtcRegBank0[RTC_0_MONT1] = (byte) d;
      if (RTC_DEBUG_TRACE) {
        rtcDumpReg ();
      }
      return;
    case 0 << 5 | RTC_0_YEAR0:  //0x00e8a017  bit3-0  1年カウンタ
      d &= 15;  //0..15
      rtcAddMday (DnT.dntCdayYearMontMday (1980 + rtcRegBank0[RTC_0_YEAR1] * 10 + d,
                                           rtcRegBank0[RTC_0_MONT1] * 10 + rtcRegBank0[RTC_0_MONT0],
                                           rtcRegBank0[RTC_0_MDAY1] * 10 + rtcRegBank0[RTC_0_MDAY0]) - rtcCday);
      rtcRegBank0[RTC_0_YEAR0] = (byte) d;
      rtcRegBank1[RTC_1_LEAP] = (byte) (rtcRegBank0[RTC_0_YEAR1] * 10 + d & 3);
      if (RTC_DEBUG_TRACE) {
        rtcDumpReg ();
      }
      return;
    case 0 << 5 | RTC_0_YEAR1:  //0x00e8a019  bit3-0  10年カウンタ
      d &= 15;  //0..15
      rtcAddMday (DnT.dntCdayYearMontMday (1980 + d * 10 + rtcRegBank0[RTC_0_YEAR0],
                                           rtcRegBank0[RTC_0_MONT1] * 10 + rtcRegBank0[RTC_0_MONT0],
                                           rtcRegBank0[RTC_0_MDAY1] * 10 + rtcRegBank0[RTC_0_MDAY0]) - rtcCday);
      rtcRegBank0[RTC_0_YEAR1] = (byte) d;
      rtcRegBank1[RTC_1_LEAP] = (byte) (d * 10 + rtcRegBank0[RTC_0_YEAR0] & 3);
      if (RTC_DEBUG_TRACE) {
        rtcDumpReg ();
      }
      return;
      //バンク1
    case 1 << 5 | RTC_1_CLKOUT:  //0x00e8a001  bit2-0  CLKOUTセレクタ
      //! 未対応。TIMER-LED
      return;
    case 1 << 5 | RTC_1_ADJUST:  //0x00e8a003  bit0    アジャスト
      //! 未対応。アジャスト
      return;
      //case 1 << 5 | RTC_1_MINU0:  //0x00e8a005  bit3-0  アラーム1分レジスタ
      //  return;
      //case 1 << 5 | RTC_1_MINU1:  //0x00e8a007  bit2-0  アラーム10分レジスタ
      //  return;
      //case 1 << 5 | RTC_1_HOUR0:  //0x00e8a009  bit3-0  アラーム1時レジスタ
      //  return;
      //case 1 << 5 | RTC_1_HOUR1:  //0x00e8a00b  bit2-0  アラーム10時レジスタ
      //  return;
      //case 1 << 5 | RTC_1_WDAY:  //0x00e8a00d  bit2-0  アラーム曜日レジスタ
      //  return;
      //case 1 << 5 | RTC_1_MDAY0:  //0x00e8a00f  bit3-0  アラーム1日カウンタ
      //  return;
      //case 1 << 5 | RTC_1_MDAY1:  //0x00e8a011  bit1-0  アラーム10日カウンタ
      //  return;
    case 1 << 5 | RTC_1_RULE:  //0x00e8a015  bit0    12時間計/24時間計セレクタ
      d &= 1;
      if (rtcRule != d) {
        //12時カウンタ/20時カウンタの値を変えずに解釈を変更する
        rtcAddSeco ((rtcRegBank0[RTC_0_HOUR1] >> 1) *
                    ((d == 0 ? 43200 : 72000) - (rtcRule == 0 ? 43200 : 72000)));
        rtcRegBank1[RTC_1_RULE] = (byte) d;
        rtcRule = d;
        if (RTC_DEBUG_TRACE) {
          System.out.printf ("  rtcRule=%d\n", rtcRule);
          rtcDumpReg ();
        }
      }
      return;
    case 1 << 5 | RTC_1_LEAP:  //0x00e8a017  bit1-0  閏年カウンタ
      //何もしない
      return;
      //共通
    case 0 << 5 | RTC_MODE:  //0x00e8a01b  モードレジスタ
    case 1 << 5 | RTC_MODE:  //0x00e8a01b  モードレジスタ
      d &= 8 + 4 + 1;
      rtcBank = d & 1;
      if (RTC_DEBUG_TRACE) {
        System.out.printf ("  rtcBank=%d\n", rtcBank);
      }
      rtcRegCurrent = rtcBank == 0 ? rtcRegBank0 : rtcRegBank1;
      if (rtcMove < (d & 8)) {  //停止中→動作中
        if (rtcTest == 0) {  //通常動作
          rtcCmilGap = rtcCmil - System.currentTimeMillis ();
          rtcCmilGap = rtcCmilGap < 0L ? -(((-rtcCmilGap + 500L) / 1000L) * 1000L) : ((rtcCmilGap + 500L) / 1000L) * 1000L;  //秒単位に丸める
          if (RTC_DEBUG_GAP) {
            System.out.printf ("rtcWriteByte  rtcCmilGap=%d\n", rtcCmilGap);
          }
          if (RTC_DEBUG_TRACE) {
            System.out.printf ("  rtcCmilGap=%d\n", rtcCmilGap);
          }
        } else {  //テスト動作
          rtcTestStartCmil = rtcCmil;
          rtcTestStartTime = XEiJ.mpuClockTime;
          if (RTC_DEBUG_TRACE) {
            System.out.printf ("  rtcTestStartCmil=%d (%s)\n", rtcTestStartCmil, DnT.dntSdttmCmil (rtcTestStartCmil));
            System.out.printf ("  rtcTestStartTime=%d\n", rtcTestStartTime);
          }
        }
      }
      rtcAlarm = d & 4;
      rtcMove = d & 8;
      if (RTC_DEBUG_TRACE) {
        System.out.printf ("  rtcMove=%d\n", rtcMove);
      }
      rtcRegBank0[RTC_MODE] = rtcRegBank1[RTC_MODE] = (byte) d;
      return;
    case 0 << 5 | RTC_TEST:  //0x00e8a01d  テストレジスタ
    case 1 << 5 | RTC_TEST:  //0x00e8a01d  テストレジスタ
      d &= 15;
      if (rtcTest != d) {
        if (d == 0) {  //テスト動作→通常動作
          if (rtcMove != 0) {  //動作中
            rtcCmilGap = rtcCmil - System.currentTimeMillis ();
            rtcCmilGap = rtcCmilGap < 0L ? -(((-rtcCmilGap + 500L) / 1000L) * 1000L) : ((rtcCmilGap + 500L) / 1000L) * 1000L;  //秒単位に丸める
            if (RTC_DEBUG_GAP) {
              System.out.printf ("rtcWriteByte  rtcCmilGap=%d\n", rtcCmilGap);
            }
            if (RTC_DEBUG_TRACE) {
              System.out.printf ("  rtcCmilGap=%d\n", rtcCmilGap);
            }
          }
        } else if (rtcTest == 0) {  //通常動作→テスト動作
          if (rtcMove != 0) {  //動作中
            rtcTestStartCmil = rtcCmil;
            rtcTestStartTime = XEiJ.mpuClockTime;
            if (RTC_DEBUG_TRACE) {
              System.out.printf ("  rtcTestStartCmil=%d (%s)\n", rtcTestStartCmil, DnT.dntSdttmCmil (rtcTestStartCmil));
              System.out.printf ("  rtcTestStartTime=%d\n", rtcTestStartTime);
            }
          }
        } else {  //テスト動作→テスト動作
          if (rtcMove != 0) {  //動作中
            rtcTestStartCmil = rtcCmil;
            rtcTestStartTime = XEiJ.mpuClockTime;
            if (RTC_DEBUG_TRACE) {
              System.out.printf ("  rtcTestStartCmil=%d (%s)\n", rtcTestStartCmil, DnT.dntSdttmCmil (rtcTestStartCmil));
              System.out.printf ("  rtcTestStartTime=%d\n", rtcTestStartTime);
            }
          }
        }
        rtcTest = d;
        rtcFirst = (rtcTest == 0 ?
                    !XEiJ.currentModel.isX68030 () ?
                    MemoryMappedDevice.MMD_RTC_FIRST_00 :
                    MemoryMappedDevice.MMD_RTC_FIRST_30 :
                    !XEiJ.currentModel.isX68030 () ?
                    MemoryMappedDevice.MMD_RTC_TEST_00 :
                    MemoryMappedDevice.MMD_RTC_TEST_30);
        XEiJ.busSuper (rtcFirst, 0x00e8a000, 0x00e8c000);
        if (RTC_DEBUG_TRACE) {
          System.out.printf ("  rtcTest=%d\n", rtcTest);
        }
        rtcRegBank0[RTC_TEST] = rtcRegBank1[RTC_TEST] = (byte) d;
      }
      return;
    case 0 << 5 | RTC_RESET:  //0x00e8a01f  リセットコントローラ
    case 1 << 5 | RTC_RESET:  //0x00e8a01f  リセットコントローラ
      rtcSetHz (d & 12);
      return;
    }
  }  //rtcWriteByte(int,int)


  //ALARM出力
  //  観測
  //    RTCのALARM出力はMFPのGPIP0で観測できる
  //  論理
  //    ALARM出力は正論理のアラームと1Hzと16HzのNOR(ORしてからNOT)である
  //    アラームがON(MODE bit2=%1)でマッチしているとき(アラームリセット後のdon't careを含む)
  //      アラームが1なので1Hzと16Hzに関係なく0が出力される
  //    アラームがON(MODE bit2=%1)でマッチしていないか、アラームがOFF(MODE bit2=%0)のとき
  //      アラームが0なので1Hzと16HzのNORが出力される
  //      1Hzと16Hzはタイマー1秒カウンタの区切りで整列している
  //      1HzがON、16HzがON(RESET bit3-2=%00)のとき
  //        1Hzの1→0と16Hzの1→0のNORが出力される
  //        00000000000000000101010101010101
  //      1HzがON、16HzがOFF(RESET bit3-2=%01)のとき
  //        1Hzの1→0と16Hzの0のNORが出力される
  //        00000000000000001111111111111111
  //      1HzがOFF、16HzがON(RESET bit3-2=%10)のとき
  //        1Hzの0と16Hzの1→0のNORが出力される
  //        01010101010101010101010101010101
  //      1HzがOFF、16HzがOFF(RESET bit3-2=%11)のとき
  //        1Hzの0と16Hzの0のNORが出力される
  //        11111111111111111111111111111111
  //  メモ
  //    アラーム起動を設定するとき、1Hzと16HzをOFF(RESET bit3-2=11)にしておかないと、
  //    マッチしていないのに0が出力されて再起動してしまう
  //!!! アラームは未実装。1Hz/16Hzのみ出力
  //!!! 1Hz/16Hzがタイマー1秒カウンタの区切りで整列していない
  public static final long RTC_HZ_UNIT = XEiJ.TMR_FREQ / 32;  //(1/32)s
  public static int rtcResetHz;  //RESET bit3-2
  public static boolean rtcAlarmHigh;  //false=Low,true=High
  public static int rtcHzStage;  //0～31
  public static long rtcHzTime;  //Tickerの実行時刻
  public static void rtcHzReset () {
    rtcResetHz = 0b1100;  //1Hz OFF,16Hz OFF
    if (!rtcAlarmHigh) {
      rtcAlarmHigh = true;  //High
      Indicator.indUpdateLED ();
      if (!PowerControl.PWC_ON ||
          PowerControl.pwcVcc1On) {
        MC68901.mfpAlarmRise ();
      }
      if (PowerControl.PWC_ON) {
        PowerControl.pwcVcc1Off ();
      }
    }
    rtcHzStage = -1;
    rtcHzTime = XEiJ.FAR_FUTURE;
  }  //rtcHzReset
  public static final TickerQueue.Ticker RTC_HZ_TICKER = new TickerQueue.Ticker () {
    @Override protected void tick () {
      rtcAlarmHigh = !rtcAlarmHigh;
      Indicator.indUpdateLED ();
      if (rtcAlarmHigh) {  //山の始まり
        if (!PowerControl.PWC_ON ||
            PowerControl.pwcVcc1On) {
          MC68901.mfpAlarmRise ();
        }
        if (PowerControl.PWC_ON) {
          PowerControl.pwcVcc1Off ();
        }
      } else {  //谷の始まり
        if (!PowerControl.PWC_ON ||
            PowerControl.pwcVcc1On) {
          MC68901.mfpAlarmFall ();
        }
        if (false &&
            PowerControl.PWC_ON) {
          PowerControl.pwcVcc1On ();
        }
      }
      int length = (rtcResetHz == 0b0000 ? rtcHzStage == 0 ? 17 : 1 :  //1Hz+16Hz
                    rtcResetHz == 0b0100 ? 16 :  //1Hz
                    1);  //16Hz。山または谷の長さ
      rtcHzStage += length;
      if (rtcHzStage == 32) {
        rtcHzStage = 0;
      }
      rtcHzTime += RTC_HZ_UNIT * length;
      TickerQueue.tkqAdd (RTC_HZ_TICKER, rtcHzTime);
    }  //tick
  };  //RTC_HZ_TICKER
  public static void rtcSetHz (int hz) {
    if (rtcResetHz != hz) {
      rtcResetHz = hz;
      if (hz < 0b1100) {  //1Hz/16Hz出力あり
        if (rtcAlarmHigh) {  //Lowから始まる
          rtcAlarmHigh = false;
          Indicator.indUpdateLED ();
          if (!PowerControl.PWC_ON ||
              PowerControl.pwcVcc1On) {
            MC68901.mfpAlarmFall ();
          }
          if (false &&
              PowerControl.PWC_ON) {
            PowerControl.pwcVcc1On ();
          }
        }
        rtcHzStage = (hz == 0b0000 ? 17 :  //1Hz+16Hz
                      hz == 0b0100 ? 16 :  //1Hz
                      1);  //16Hz。最初の谷の長さ=次のステージ
        rtcHzTime = XEiJ.mpuClockTime + RTC_HZ_UNIT * rtcHzStage;
        TickerQueue.tkqAdd (RTC_HZ_TICKER, rtcHzTime);
      } else {  //1Hz/16Hz出力なし
        if (!rtcAlarmHigh) {  //Highに戻す
          rtcAlarmHigh = true;
          Indicator.indUpdateLED ();
          if (!PowerControl.PWC_ON ||
              PowerControl.pwcVcc1On) {
            MC68901.mfpAlarmRise ();
          }
          if (PowerControl.PWC_ON) {
            PowerControl.pwcVcc1Off ();
          }
        }
        rtcHzStage = -1;
        rtcHzTime = XEiJ.FAR_FUTURE;
        TickerQueue.tkqRemove (RTC_HZ_TICKER);
      }
    }
  }  //rtcHzUpdate



  //rtcTurnOn ()
  //  VCC1がONになった
  //  実機のRTCはVCC2とバックアップ電池で動作するので何も変わらない
  //  ここではALARMとCLKOUTをタスクからTickerに移行する
  //!!! 未実装
  public static void rtcTurnOn () {
  }  //rtcTurnOn

  //rtcTurnOff ()
  //  VCC1がOFFになった
  //  実機のRTCはVCC2とバックアップ電池で動作するので何も変わらない
  //  ここではALARMとCLKOUTをTickerからタスクに移行する
  //!!! 未実装
  public static void rtcTurnOff () {
  }  //rtcTurnOff

  //rtcExecuteCore ()
  //  コアの10msの処理の先頭で呼び出される
  public static void rtcExecuteCore () {
    XEiJ.busSuper (rtcFirst, 0x00e8a000, 0x00e8c000);  //RTC RTC
  }  //rtcExecuteCore



}  //class RP5C15



