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

package xeij;

import java.util.*;  //TimeZone

public class RP5C15 {


  //------------------------------------------------
  //Common 共通インタフェイス

  //rtcInit ()
  //  初期化
  public static void rtcInit () {
    //復元
    rtcRegRestore ();
    rtcTimerRestore ();
    rtcAlarmRestore ();
    rtcClkoutRestore ();
    rtcTestRestore ();
    //初期化
    rtcTimerInit ();
    rtcAlarmInit ();
    rtcClkoutInit ();
    rtcTestInit ();
    rtcProcessInit ();
  }  //rtcInit

  //rtcTini ()
  //  後始末
  public static void rtcTini () {
    //後始末
    rtcProcessTini ();
    //保存
    rtcRegSave ();
    rtcTimerSave ();
    rtcAlarmSave ();
    rtcClkoutSave ();
    rtcTestSave ();
  }  //rtcTini

  //rtcReset ()
  //  リセット
  public static void rtcReset () {
    rtcProcessReset ();
    rtcAlarmReset ();
    Indicator.indUpdateLED ();
  }  //rtcReset


  //------------------------------------------------
  //Reg レジスタ

  //一覧
  //  bank num     address  bit
  //     0   0  0x00e8a001  3-0  1秒カウンタ
  //     0   1  0x00e8a003  2-0  10秒カウンタ
  //     0   2  0x00e8a005  3-0  1分カウンタ
  //     0   3  0x00e8a007  2-0  10分カウンタ
  //     0   4  0x00e8a009  3-0  1時カウンタ
  //     0   5  0x00e8a00b  1-0  10時カウンタ
  //     0   6  0x00e8a00d  2-0  曜日カウンタ
  //     0   7  0x00e8a00f  3-0  1日カウンタ
  //     0   8  0x00e8a011  1-0  10日カウンタ
  //     0   9  0x00e8a013  3-0  1月カウンタ
  //     0  10  0x00e8a015    0  10月カウンタ
  //     0  11  0x00e8a017  3-0  1年カウンタ
  //     0  12  0x00e8a019  3-0  10年カウンタ
  //     1   0  0x00e8a001  2-0  CLKOUTセレクタ
  //     1   1  0x00e8a003    0  アジャスト(write-only)
  //     1   2  0x00e8a005  3-0  1分アラームレジスタ
  //     1   3  0x00e8a007  2-0  10分アラームレジスタ
  //     1   4  0x00e8a009  3-0  1時アラームレジスタ
  //     1   5  0x00e8a00b  1-0  10時アラームレジスタ
  //     1   6  0x00e8a00d  2-0  曜日アラームレジスタ
  //     1   7  0x00e8a00f  3-0  1日アラームレジスタ
  //     1   8  0x00e8a011  1-0  10日アラームレジスタ
  //     1   9  0x00e8a013
  //     1  10  0x00e8a015    0  12/24セレクトレジスタ
  //     1  11  0x00e8a017  1-0  閏年カウンタ
  //     1  12  0x00e8a019
  //     *  13  0x00e8a01b  3-0  モードレジスタ
  //     *  14  0x00e8a01d  3-0  テストレジスタ(write-only)
  //     *  15  0x00e8a01f  3-0  リセットレジスタ(write-only)

  //モードレジスタ
  //  bank=* num=13 address=0x1b
  //  bit3  0=タイマーOFF,1=タイマーON
  //  bit2  0=アラームOFF,1=アラームON
  //  bit0  0=バンク0,1=バンク1
  public static boolean rtcBankOne;

  //リセットレジスタ(write-only)
  //  bank=* num=15 address=0x1f
  //  bit3  0=1Hz ON
  //  bit2  0=16Hz ON
  //  bit1  1=秒以前の分周段リセット
  //  bit0  1=アラームリセット
  //  読み出しは常に0

  //rtcRegRestore ()
  //  復元
  public static void rtcRegRestore () {
    rtcBankOne = Settings.sgsGetOnOff ("rtcbankone", false);
  }  //rtcRegRestore

  //rtcRegSave ()
  //  保存
  public static void rtcRegSave () {
    Settings.sgsPutOnOff ("rtcbankone", rtcBankOne);
  }  //rtcRegSave

  //rtcPeekByte (a)
  //  ピークバイト
  public static int rtcPeekByte (int a) {
    switch ((rtcBankOne ? 0x20 : 0x00) + (a & 0x1f)) {
      //
    case 0x00 + 0x01:  //bank=0 num=0  1秒カウンタ
      return 0xf0 | rtcOneSeco;
    case 0x00 + 0x03:  //bank=0 num=1  10秒カウンタ
      return 0xf0 | rtcTenSeco;
    case 0x00 + 0x05:  //bank=0 num=2  1分カウンタ
      return 0xf0 | rtcOneMinu;
    case 0x00 + 0x07:  //bank=0 num=3  10分カウンタ
      return 0xf0 | rtcTenMinu;
    case 0x00 + 0x09:  //bank=0 num=4  1時カウンタ
      return 0xf0 | rtcOneHour;
    case 0x00 + 0x0b:  //bank=0 num=5  10時カウンタ
      return 0xf0 | rtcTenHour;
    case 0x00 + 0x0d:  //bank=0 num=6  曜日カウンタ
      return 0xf0 | rtcWday;
    case 0x00 + 0x0f:  //bank=0 num=7  1日カウンタ
      return 0xf0 | rtcOneMday;
    case 0x00 + 0x11:  //bank=0 num=8  10日カウンタ
      return 0xf0 | rtcTenMday;
    case 0x00 + 0x13:  //bank=0 num=9  1月カウンタ
      return 0xf0 | rtcOneMont;
    case 0x00 + 0x15:  //bank=0 num=10  10月カウンタ
      return 0xf0 | rtcTenMont;
    case 0x00 + 0x17:  //bank=0 num=11  1年カウンタ
      return 0xf0 | rtcOneYear;
    case 0x00 + 0x19:  //bank=0 num=12  10年カウンタ
      return 0xf0 | rtcTenYear;
      //
    case 0x20 + 0x01:  //bank=1 num=0  CLKOUTセレクタ
      return 0xf0 | rtcClkoutMode;
      //
    case 0x20 + 0x05:  //bank=1 num=2  1分アラームレジスタ
      return 0xf0 | rtcAlarmOneMinu;
    case 0x20 + 0x07:  //bank=1 num=3  10分アラームレジスタ
      return 0xf0 | rtcAlarmTenMinu;
    case 0x20 + 0x09:  //bank=1 num=4  1時アラームレジスタ
      return 0xf0 | rtcAlarmOneHour;
    case 0x20 + 0x0b:  //bank=1 num=5  10時アラームレジスタ
      return 0xf0 | rtcAlarmTenHour;
    case 0x20 + 0x0d:  //bank=1 num=6  曜日アラームレジスタ
      return 0xf0 | rtcAlarmWday;
    case 0x20 + 0x0f:  //bank=1 num=7  1日アラームレジスタ
      return 0xf0 | rtcAlarmOneMday;
    case 0x20 + 0x11:  //bank=1 num=8  10日アラームレジスタ
      return 0xf0 | rtcAlarmTenMday;
      //
    case 0x20 + 0x15:  //bank=1 num=10  12/24セレクトレジスタ
      return 0xf0 | rtcSystem;
    case 0x20 + 0x17:  //bank=1 num=11  閏年カウンタ
      return 0xf0 | rtcLeapYear;
      //
    case 0x00 + 0x1b:  //bank=0 num=13  モードレジスタ
    case 0x20 + 0x1b:  //bank=1 num=13  モードレジスタ
      return 0xf0 | (rtcTimerEn ? 8 : 0) | (rtcAlarmEn ? 4 : 0) | (rtcBankOne ? 1 : 0);
      //
    case 0x20 + 0x03:  //bank=1 num=1  アジャスト(write-only)
    case 0x20 + 0x13:  //bank=1 num=9
    case 0x20 + 0x19:  //bank=1 num=12
    case 0x00 + 0x1d:  //bank=0 num=14  テストレジスタ(write-only)
    case 0x20 + 0x1d:  //bank=1 num=14  テストレジスタ(write-only)
    case 0x00 + 0x1f:  //bank=0 num=15  リセットレジスタ(write-only)
    case 0x20 + 0x1f:  //bank=1 num=15  リセットレジスタ(write-only)
      return 0xf0;
      //
    default:  //偶数アドレス
      return 0xff;
    }
  }  //rtcPeekByte

  //rtcReadByte (a)
  //  リードバイト
  public static int rtcReadByte (int a) {
    if (rtcTestMode != 0) {  //テストON
      rtcTestUpdate ();  //テストでカウンタを更新する
    }
    return rtcPeekByte (a);
  }  //rtcReadByte

  //rtcWriteByte (a, d)
  //  ライトバイト
  public static void rtcWriteByte (int a, int d) {
    if (rtcTestMode != 0) {  //テストON
      rtcTestUpdate ();  //テストでカウンタを更新する
    }
    switch ((rtcBankOne ? 0x20 : 0x00) + (a & 0x1f)) {
      //
    case 0x00 + 0x01:  //bank=0 num=0  1秒カウンタ
      rtcSetOneSeco (d);
      return;
    case 0x00 + 0x03:  //bank=0 num=1  10秒カウンタ
      rtcSetTenSeco (d);
      return;
    case 0x00 + 0x05:  //bank=0 num=2  1分カウンタ
      rtcSetOneMinu (d);
      return;
    case 0x00 + 0x07:  //bank=0 num=3  10分カウンタ
      rtcSetTenMinu (d);
      return;
    case 0x00 + 0x09:  //bank=0 num=4  1時カウンタ
      rtcSetOneHour (d);
      return;
    case 0x00 + 0x0b:  //bank=0 num=5  10時カウンタ
      rtcSetTenHour (d);
      return;
    case 0x00 + 0x0d:  //bank=0 num=6  曜日カウンタ
      rtcSetWday (d);
      return;
    case 0x00 + 0x0f:  //bank=0 num=7  1日カウンタ
      rtcSetOneMday (d);
      return;
    case 0x00 + 0x11:  //bank=0 num=8  10日カウンタ
      rtcSetTenMday (d);
      return;
    case 0x00 + 0x13:  //bank=0 num=9  1月カウンタ
      rtcSetOneMont (d);
      return;
    case 0x00 + 0x15:  //bank=0 num=10  10月カウンタ
      rtcSetTenMont (d);
      return;
    case 0x00 + 0x17:  //bank=0 num=11  1年カウンタ
      rtcSetOneYear (d);
      return;
    case 0x00 + 0x19:  //bank=0 num=12  10年カウンタ
      rtcSetTenYear (d);
      return;
      //
    case 0x20 + 0x01:  //bank=1 num=0  CLKOUTセレクタ
      rtcSetClkoutMode (d);  //CLKOUTモードの設定
      return;
    case 0x20 + 0x03:  //bank=1 num=1  アジャスト(write-only)
      if ((d & 1) != 0) {
        rtcAdjust ();  //アジャスト実行
      }
      return;
    case 0x20 + 0x05:  //bank=1 num=2  1分アラームレジスタ
      rtcSetAlarmOneMinu (d);
      return;
    case 0x20 + 0x07:  //bank=1 num=3  10分アラームレジスタ
      rtcSetAlarmTenMinu (d);
      return;
    case 0x20 + 0x09:  //bank=1 num=4  1時アラームレジスタ
      rtcSetAlarmOneHour (d);
      return;
    case 0x20 + 0x0b:  //bank=1 num=5  10時アラームレジスタ
      rtcSetAlarmTenHour (d);
      return;
    case 0x20 + 0x0d:  //bank=1 num=6  曜日アラームレジスタ
      rtcSetAlarmWday (d);
      return;
    case 0x20 + 0x0f:  //bank=1 num=7  1日アラームレジスタ
      rtcSetAlarmOneMday (d);
      return;
    case 0x20 + 0x11:  //bank=1 num=8  10日アラームレジスタ
      rtcSetAlarmTenMday (d);
      return;
      //
    case 0x20 + 0x15:  //bank=1 num=10  12/24セレクトレジスタ
      rtcSetSystem (d);
      return;
    case 0x20 + 0x17:  //bank=1 num=11  閏年カウンタ
      rtcSetLeapYear (d);
      return;
      //
    case 0x00 + 0x1b:  //bank=0 num=13  モードレジスタ
    case 0x20 + 0x1b:  //bank=1 num=13  モードレジスタ
      rtcSetTimerEn ((d & 8) != 0);  //タイマーのON/OFF
      rtcSetAlarmEn ((d & 4) != 0);  //アラームのON/OFF
      rtcBankOne = ((d & 1) != 0);  //バンクの切り替え
      return;
    case 0x00 + 0x1d:  //bank=0 num=14  テストレジスタ(write-only)
    case 0x20 + 0x1d:  //bank=1 num=14  テストレジスタ(write-only)
      rtcSetTestMode (d);
      return;
    case 0x00 + 0x1f:  //bank=0 num=15  リセットレジスタ(write-only)
    case 0x20 + 0x1f:  //bank=1 num=15  リセットレジスタ(write-only)
      rtcSetHzMode ((d & 0b1100) >> 2);
      if ((d & 0b0010) != 0) {
        rtcRoundDown ();  //秒未満の端数を切り捨てる
      }
      if ((d & 0b0001) != 0) {
        rtcWildcardify ();  //すべての項目をワイルドカードにする
      }
      return;
    }
  }  //rtcWriteByte


  //------------------------------------------------
  //Timer タイマー

  //月の日数
  //  dymt  月の日数
  //  [leap][mont-1]=dymt
  public static final int[] RTC_DYMT_L0 = {
    31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31,
  };
  public static final int[] RTC_DYMT_L1 = {
    31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31,
  };
  public static final int[][] RTC_DYMT = {
    RTC_DYMT_L0, RTC_DYMT_L1, RTC_DYMT_L1, RTC_DYMT_L1,
  };

  //カウンタ遷移表
  //  X68030実機で観測した結果
  //  BCDの加算回路に範囲外の値を入力したら加算にならないのは想定内
  //  同じ結果を得られる論理式を推測できるがそれが実際の論理式と同じかどうかは分からない
  //  ここではテーブルにする
  public static final int[] RTC_NINE = {
    // 0     1     2     3     4     5     6     7
    0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
    // 8     9     A     B     C     D     E     F
    0x09, 0x10, 0x0B, 0x16, 0x0D, 0x14, 0x0F, 0x12,
  };
  public static final int[] RTC_FIVE = {
    // 0     1     2     3     4     5     6     7
    0x01, 0x02, 0x03, 0x04, 0x05, 0x10, 0x07, 0x12,
  };
  public static final int[] RTC_HOUR_T0 = {
    // 00     01     02     03     04     05     06     07
    0x001, 0x002, 0x003, 0x004, 0x005, 0x006, 0x007, 0x008,
    // 08     09     0A     0B     0C     0D     0E     0F
    0x009, 0x010, 0x00B, 0x016, 0x00D, 0x014, 0x00F, 0x012,
    // 10     11     12     13     14     15     16     17
    0x011, 0x020, 0x013, 0x026, 0x015, 0x024, 0x017, 0x02A,
    // 18     19     1A     1B     1C     1D     1E     1F
    0x019, 0x020, 0x01B, 0x026, 0x01D, 0x024, 0x01F, 0x022,
    // 20     21     22     23     24     25     26     27
    0x021, 0x022, 0x023, 0x024, 0x025, 0x026, 0x027, 0x028,
    // 28     29     2A     2B     2C     2D     2E     2F
    0x029, 0x030, 0x02B, 0x036, 0x02D, 0x034, 0x02F, 0x032,
    // 30     31     32     33     34     35     36     37
    0x031, 0x100, 0x033, 0x106, 0x035, 0x104, 0x037, 0x10A,
    // 38     39     3A     3B     3C     3D     3E     3F
    0x039, 0x100, 0x03B, 0x106, 0x03D, 0x104, 0x03F, 0x102,
  };
  public static final int[] RTC_HOUR_T1 = {
    // 00     01     02     03     04     05     06     07
    0x001, 0x002, 0x003, 0x004, 0x005, 0x006, 0x007, 0x008,
    // 08     09     0A     0B     0C     0D     0E     0F
    0x009, 0x010, 0x00B, 0x016, 0x00D, 0x014, 0x00F, 0x012,
    // 10     11     12     13     14     15     16     17
    0x011, 0x012, 0x013, 0x014, 0x015, 0x016, 0x017, 0x018,
    // 18     19     1A     1B     1C     1D     1E     1F
    0x019, 0x020, 0x01B, 0x026, 0x01D, 0x024, 0x01F, 0x022,
    // 20     21     22     23     24     25     26     27
    0x021, 0x022, 0x023, 0x100, 0x025, 0x026, 0x027, 0x10C,
    // 28     29     2A     2B     2C     2D     2E     2F
    0x029, 0x030, 0x02B, 0x112, 0x02D, 0x034, 0x02F, 0x116,
    // 30     31     32     33     34     35     36     37
    0x031, 0x032, 0x033, 0x110, 0x035, 0x036, 0x037, 0x11C,
    // 38     39     3A     3B     3C     3D     3E     3F
    0x039, 0x000, 0x03B, 0x102, 0x03D, 0x004, 0x03F, 0x106,
  };
  public static final int[][] RTC_HOUR = {
    //        0            1
    RTC_HOUR_T0, RTC_HOUR_T1,
  };
  public static final int[] RTC_WDAY = {
    //0    1    2    3    4    5    6    7
    0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x0, 0x1,
  };
  public static final int[] RTC_MDAY_28 = {
    // 00     01     02     03     04     05     06     07
    0x001, 0x002, 0x003, 0x004, 0x005, 0x006, 0x007, 0x008,
    // 08     09     0A     0B     0C     0D     0E     0F
    0x009, 0x010, 0x00B, 0x016, 0x00D, 0x014, 0x00F, 0x012,
    // 10     11     12     13     14     15     16     17
    0x011, 0x012, 0x013, 0x014, 0x015, 0x016, 0x017, 0x018,
    // 18     19     1A     1B     1C     1D     1E     1F
    0x019, 0x020, 0x01B, 0x026, 0x01D, 0x024, 0x01F, 0x022,
    // 20     21     22     23     24     25     26     27
    0x021, 0x022, 0x023, 0x024, 0x025, 0x026, 0x027, 0x028,
    // 28     29     2A     2B     2C     2D     2E     2F
    0x101, 0x110, 0x103, 0x116, 0x105, 0x114, 0x107, 0x112,
    // 30     31     32     33     34     35     36     37
    0x031, 0x101, 0x033, 0x107, 0x035, 0x105, 0x037, 0x10B,
    // 38     39     3A     3B     3C     3D     3E     3F
    0x111, 0x101, 0x113, 0x107, 0x115, 0x105, 0x117, 0x103,
  };
  public static final int[] RTC_MDAY_29 = {
    // 00     01     02     03     04     05     06     07
    0x001, 0x002, 0x003, 0x004, 0x005, 0x006, 0x007, 0x008,
    // 08     09     0A     0B     0C     0D     0E     0F
    0x009, 0x010, 0x00B, 0x016, 0x00D, 0x014, 0x00F, 0x012,
    // 10     11     12     13     14     15     16     17
    0x011, 0x012, 0x013, 0x014, 0x015, 0x016, 0x017, 0x018,
    // 18     19     1A     1B     1C     1D     1E     1F
    0x019, 0x020, 0x01B, 0x026, 0x01D, 0x024, 0x01F, 0x022,
    // 20     21     22     23     24     25     26     27
    0x021, 0x022, 0x023, 0x024, 0x025, 0x026, 0x027, 0x028,
    // 28     29     2A     2B     2C     2D     2E     2F
    0x029, 0x101, 0x02B, 0x107, 0x02D, 0x105, 0x02F, 0x103,
    // 30     31     32     33     34     35     36     37
    0x031, 0x101, 0x033, 0x107, 0x035, 0x105, 0x037, 0x10B,
    // 38     39     3A     3B     3C     3D     3E     3F
    0x039, 0x101, 0x03B, 0x107, 0x03D, 0x105, 0x03F, 0x103,
  };
  public static final int[] RTC_MDAY_30 = {
    // 00     01     02     03     04     05     06     07
    0x001, 0x002, 0x003, 0x004, 0x005, 0x006, 0x007, 0x008,
    // 08     09     0A     0B     0C     0D     0E     0F
    0x009, 0x010, 0x00B, 0x016, 0x00D, 0x014, 0x00F, 0x012,
    // 10     11     12     13     14     15     16     17
    0x011, 0x012, 0x013, 0x014, 0x015, 0x016, 0x017, 0x018,
    // 18     19     1A     1B     1C     1D     1E     1F
    0x019, 0x020, 0x01B, 0x026, 0x01D, 0x024, 0x01F, 0x022,
    // 20     21     22     23     24     25     26     27
    0x021, 0x022, 0x023, 0x024, 0x025, 0x026, 0x027, 0x028,
    // 28     29     2A     2B     2C     2D     2E     2F
    0x029, 0x030, 0x02B, 0x036, 0x02D, 0x034, 0x02F, 0x032,
    // 30     31     32     33     34     35     36     37
    0x101, 0x101, 0x103, 0x107, 0x105, 0x105, 0x107, 0x10B,
    // 38     39     3A     3B     3C     3D     3E     3F
    0x109, 0x101, 0x10B, 0x107, 0x10D, 0x105, 0x10F, 0x103,
  };
  public static final int[] RTC_MDAY_31 = {
    // 00     01     02     03     04     05     06     07
    0x001, 0x002, 0x003, 0x004, 0x005, 0x006, 0x007, 0x008,
    // 08     09     0A     0B     0C     0D     0E     0F
    0x009, 0x010, 0x00B, 0x016, 0x00D, 0x014, 0x00F, 0x012,
    // 10     11     12     13     14     15     16     17
    0x011, 0x012, 0x013, 0x014, 0x015, 0x016, 0x017, 0x018,
    // 18     19     1A     1B     1C     1D     1E     1F
    0x019, 0x020, 0x01B, 0x026, 0x01D, 0x024, 0x01F, 0x022,
    // 20     21     22     23     24     25     26     27
    0x021, 0x022, 0x023, 0x024, 0x025, 0x026, 0x027, 0x028,
    // 28     29     2A     2B     2C     2D     2E     2F
    0x029, 0x030, 0x02B, 0x036, 0x02D, 0x034, 0x02F, 0x032,
    // 30     31     32     33     34     35     36     37
    0x031, 0x101, 0x033, 0x107, 0x035, 0x105, 0x037, 0x10B,
    // 38     39     3A     3B     3C     3D     3E     3F
    0x039, 0x101, 0x03B, 0x107, 0x03D, 0x105, 0x03F, 0x103,
  };
  public static final int[][] RTC_MDAY_L0 = {
    //       00           01           02           03           04           05           06           07
    RTC_MDAY_31, RTC_MDAY_31, RTC_MDAY_29, RTC_MDAY_31, RTC_MDAY_30, RTC_MDAY_31, RTC_MDAY_30, RTC_MDAY_31,
    //       08           09           0A           0B           0C           0D           0E           0F
    RTC_MDAY_31, RTC_MDAY_30, RTC_MDAY_31, RTC_MDAY_30, RTC_MDAY_30, RTC_MDAY_30, RTC_MDAY_30, RTC_MDAY_30,
    //       10           11           12           13           14           15           16           17
    RTC_MDAY_31, RTC_MDAY_30, RTC_MDAY_31, RTC_MDAY_30, RTC_MDAY_30, RTC_MDAY_30, RTC_MDAY_30, RTC_MDAY_30,
    //       18           19           1A           1B           1C           1D           1E           1F
    RTC_MDAY_31, RTC_MDAY_30, RTC_MDAY_31, RTC_MDAY_30, RTC_MDAY_30, RTC_MDAY_30, RTC_MDAY_30, RTC_MDAY_30,
  };
  public static final int[][] RTC_MDAY_L1 = {
    //       00           01           02           03           04           05           06           07
    RTC_MDAY_31, RTC_MDAY_31, RTC_MDAY_28, RTC_MDAY_31, RTC_MDAY_30, RTC_MDAY_31, RTC_MDAY_30, RTC_MDAY_31,
    //       08           09           0A           0B           0C           0D           0E           0F
    RTC_MDAY_31, RTC_MDAY_30, RTC_MDAY_31, RTC_MDAY_30, RTC_MDAY_30, RTC_MDAY_30, RTC_MDAY_30, RTC_MDAY_30,
    //       10           11           12           13           14           15           16           17
    RTC_MDAY_31, RTC_MDAY_30, RTC_MDAY_31, RTC_MDAY_30, RTC_MDAY_30, RTC_MDAY_30, RTC_MDAY_30, RTC_MDAY_30,
    //       18           19           1A           1B           1C           1D           1E           1F
    RTC_MDAY_31, RTC_MDAY_30, RTC_MDAY_31, RTC_MDAY_30, RTC_MDAY_30, RTC_MDAY_30, RTC_MDAY_30, RTC_MDAY_30,
  };
  public static final int[][][] RTC_MDAY = {
    //        0            1            2            3
    RTC_MDAY_L0, RTC_MDAY_L1, RTC_MDAY_L1, RTC_MDAY_L1,
  };
  public static final int[] RTC_MONT = {
    // 00     01     02     03     04     05     06     07
    0x001, 0x002, 0x003, 0x004, 0x005, 0x006, 0x007, 0x008,
    // 08     09     0A     0B     0C     0D     0E     0F
    0x009, 0x010, 0x00B, 0x016, 0x00D, 0x014, 0x00F, 0x012,
    // 10     11     12     13     14     15     16     17
    0x011, 0x012, 0x101, 0x104, 0x015, 0x016, 0x105, 0x108,
    // 18     19     1A     1B     1C     1D     1E     1F
    0x019, 0x000, 0x109, 0x104, 0x01D, 0x004, 0x10D, 0x100,
  };
  public static final int[] RTC_LEAP = {
    //0    1    2    3
    0x1, 0x2, 0x3, 0x0,
  };

  //秒カウンタ
  //  bank=0 num=0,1 address=0x01,0x03
  //  00～59 (BCD)
  public static int rtcOneSeco;  //1秒カウンタ
  public static int rtcTenSeco;  //10秒カウンタ

  //分カウンタ
  //  bank=0 num=2,3 address=0x05,0x07
  //  00～59 (BCD)
  public static int rtcOneMinu;  //1分カウンタ
  public static int rtcTenMinu;  //10分カウンタ

  //時カウンタ
  //  bank=0 num=4,5 address=0x09,0x0b
  //  00～31 (BCD)
  //             0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19 20 21 22 23
  //  12時間計  00 01 02 03 04 05 06 07 08 09 10 11 20 21 22 23 24 25 26 27 28 29 30 31
  //  24時間計  00 01 02 03 04 05 06 07 08 09 10 11 12 13 14 15 16 17 18 19 20 21 22 23
  public static int rtcOneHour;  //1時カウンタ
  public static int rtcTenHour;  //10時カウンタ。bit1はAM/PMまたは20時カウンタ

  //曜日カウンタ
  //  bank=0 num=6 address=0x0d
  //  0～6
  //  7日でオーバーフローする日カウンタ
  //  日カウンタと同時に初期化する必要があり、日カウンタと同時にカウントアップする
  //  0～6のどれが日曜日なのかは初期化したときに決まる
  //  X68000では0=日曜日で初期化されるが、RTC側で0=日曜日と決まっているわけではない
  //  曜日カウンタに7を書き込むと、IOCS _DAYASCやCOMMAND.XのDATEの曜日が「？」になる
  //
  //  多くのプログラミング環境で
  //    0=日曜日,1=月曜日,2=火曜日,3=水曜日,4=木曜日,5=金曜日,6=土曜日
  //  だが、ISO 8601／JIS X 0301は
  //    1=月曜日,2=火曜日,3=水曜日,4=木曜日,5=金曜日,6=土曜日,7=日曜日
  //  となっている
  //
  public static int rtcWday;  //曜日カウンタ

  //日カウンタ
  //  bank=0 num=7,8 address=0x0f,0x11
  //  01～31 (BCD)。01=1日
  //  年カウンタが4の倍数になっているかどうかとは関係なく、
  //  閏年カウンタが0のときに2月を29日までカウントする
  //  閏年カウンタ  月カウンタ  01 02 03 04 05 06 07 08 09 10 11 12
  //        0                   31 29 31 30 31 30 31 31 30 31 30 31
  //        1       日カウンタ  31 28 31 30 31 30 31 31 30 31 30 31
  //        2         の上限    31 28 31 30 31 30 31 31 30 31 30 31
  //        3                   31 28 31 30 31 30 31 31 30 31 30 31
  public static int rtcOneMday;  //1日カウンタ
  public static int rtcTenMday;  //10日カウンタ

  //月カウンタ
  //  bank=0 num=9,10 address=0x13,0x15
  //  01～12 (BCD)。01=1月
  public static int rtcOneMont;  //1月カウンタ
  public static int rtcTenMont;  //10月カウンタ

  //年カウンタ
  //  bank=0 num=11,12 address=0x17,0x19
  //  00～99 (BCD)
  //  ただの12ヶ月カウンタ
  //  閏年カウンタと同時に初期化する必要があり、閏年カウンタと同時にカウントアップする
  //  00が西暦何年なのかは初期化したときに決まる
  //  X68000では00=1980年で初期化されるが、RTC側で00=1980年と決まっているわけではない
  //  4年に1回閏年になるので、2000年は問題ないが1900年や2100年を含めることはできない
  public static int rtcOneYear;  //1年カウンタ
  public static int rtcTenYear;  //10年カウンタ

  //アジャスト(write-only)
  //  bank=1 num=1 address=0x03
  //  1=秒カウンタを29捨30入する
  //  読み出しは常に0

  //12/24セレクトレジスタ
  //  bank=1 num=9 address=0x13
  //  0～1。0=12時間計,1=24時間計
  public static int rtcSystem;  //12/24セレクトレジスタ

  //閏年カウンタ
  //  bank=1 num=10 address=0x15
  //  0～3。0=今年が閏年,1=3年後が閏年,2=2年後が閏年,3=来年が閏年
  //  4年でオーバーフローする年カウンタ
  //  年カウンタと同時に初期化する必要があり、年カウンタと同時にカウントアップする
  //  曜日カウンタと違ってRTC側で0=閏年と決まっている
  //  年カウンタが4の倍数になっているかどうかとは関係なく、
  //  閏年カウンタが0のとき2月を29日までカウントする
  public static int rtcLeapYear;

  //モードレジスタ
  //  bank=* num=13 address=0x1b
  //  bit3  0=タイマーOFF,1=タイマーON
  public static boolean rtcTimerEn;

  //デフォルトオフセット
  public static long rtcDefaultOffset;

  //歴通ミリ秒オフセット
  //  (cday,dmil,s32) = System.currentTimeMillis () + offset
  //  ホストの日時(UTC)に対して、日時カウンタがどれだけ進んでいるか
  //  カウンタ書き込み、タイマーリセット、アジャストで連動ON→ONのとき差分を加える
  //  連動OFFのとき無効
  //  連動OFF→ONかつ連動OFFの間にカウンタへの書き込みがあったとき再計算
  public static long rtcOffset;

  //暦通日と日通秒
  //  カウンタの値を暦通日と日通秒で表したもの
  //  それぞれ-1のときカウンタの値が異常であることを意味する
  //  テストOFFのとき有効
  public static int rtcCday;  //歴通日。-1=異常。3652(1980-01-01)～40176(2079-12-31)=正常
  public static int rtcDsec;  //日通秒。-1=異常。0(00:00:00)～86399(23:59:59)=正常

  //1/32秒カウンタ
  //  0～31
  //  常に動き続ける
  //  タイマーOFF→ON、タイマーリセット、アジャストのときゼロクリア
  public static int rtcS32;

  //連動
  //  ホストとカウンタが連動しているときON、さもなくばOFF
  //  自律OFFかつテストOFFかつタイマーONかつカウンタ正常
  public static boolean rtcConnected;
  //  カウンタへの書き込み回数
  public static int rtcWrittenEnd;
  //  連動ON→OFFのときのカウンタへの書き込み回数
  public static int rtcWrittenStart;

  //自律
  //  常に連動OFF
  public static boolean rtcAutonomousRequest;
  public static boolean rtcAutonomous;

  //rtcTimerRestore ()
  //  復元
  public static void rtcTimerRestore () {
    int[] timer = Settings.sgsGetIntArray ("rtctimer", 14, 0);
    rtcOneSeco = timer[0] & 15;
    rtcTenSeco = timer[1] & 7;
    rtcOneMinu = timer[2] & 15;
    rtcTenMinu = timer[3] & 7;
    rtcOneHour = timer[4] & 15;
    rtcTenHour = timer[5] & 3;
    rtcWday = timer[6] & 7;
    rtcOneMday = timer[7] & 15;
    rtcTenMday = timer[8] & 3;
    rtcOneMont = timer[9] & 15;
    rtcTenMont = timer[10] & 1;
    rtcOneYear = timer[11] & 15;
    rtcTenYear = timer[12] & 15;
    rtcLeapYear = timer[13] & 3;
    rtcSystem = Settings.sgsGetInt ("rtcsystem", 1, 0, 1);
    rtcTimerEn = Settings.sgsGetOnOff ("rtctimeren", true);
    rtcDefaultOffset = TimeZone.getDefault ().getRawOffset ();
    rtcOffset = Settings.sgsGetLong ("rtcoffset", rtcDefaultOffset);
    rtcAutonomousRequest = Settings.sgsGetOnOff ("rtcautonomous", false);
  }  //rtcTimerRestore

  //rtcTimerSave ()
  //  保存
  public static void rtcTimerSave () {
    int[] timer = new int[14];
    timer[0] = rtcOneSeco;
    timer[1] = rtcTenSeco;
    timer[2] = rtcOneMinu;
    timer[3] = rtcTenMinu;
    timer[4] = rtcOneHour;
    timer[5] = rtcTenHour;
    timer[6] = rtcWday;
    timer[7] = rtcOneMday;
    timer[8] = rtcTenMday;
    timer[9] = rtcOneMont;
    timer[10] = rtcTenMont;
    timer[11] = rtcOneYear;
    timer[12] = rtcTenYear;
    timer[13] = rtcLeapYear;
    Settings.sgsPutIntArray ("rtctimer", timer);
    Settings.sgsPutInt ("rtcsystem", rtcSystem);
    Settings.sgsPutOnOff ("rtctimeren", rtcTimerEn);
    Settings.sgsPutLong ("rtcoffset", rtcOffset);
    Settings.sgsPutOnOff ("rtcautonomous", rtcAutonomousRequest);
  }  //rtcTimerSave

  //rtcTimerInit ()
  //  初期化
  public static void rtcTimerInit () {
    rtcWrittenEnd = 0;
    rtcWrittenStart = 0;
    rtcAutonomous = rtcAutonomousRequest;
    if (rtcTestMode == 0 && rtcTimerEn) {  //テストOFFかつタイマーONのとき
      rtcWriteHostDateTime ();  //ホストの日時を書き込む
    } else {  //テストONまたはタイマーOFFのとき
      rtcCday = rtcReadCday ();  //カウンタから歴通日を読み出す
      rtcDsec = rtcReadDsec ();  //カウンタから日通秒を読み出す
      rtcS32 = 0;  //秒未満の端数を切り捨てる
    }
  }  //rtcTimerInit

  //offset = rtcGetOffset ()
  //  歴通ミリ秒オフセットを返す
  //  連動OFFのときデフォルトオフセットを返す
  public static long rtcGetOffset () {
    return rtcConnected ? rtcOffset : rtcDefaultOffset;
  }  //rtcGetOffset

  //rtcWriteHostDateTime ()
  //  ホストの日時を書き込む
  //  アラーム比較は行わない
  public static void rtcWriteHostDateTime () {
    long cmil = System.currentTimeMillis () + rtcOffset;
    rtcS32 = (DnT.dntMillCmil (cmil) << 5) / 1000;
    rtcWriteCsec (DnT.dntCsecCmil (cmil));  //歴通秒を書き込む
  }  //rtcWriteHostDateTime

  //rtcWriteCsec (csec)
  //  歴通秒を書き込む
  //  315532800(1980-01-01 00:00:00)～3471292799(2071-12-31 23:59:59)
  //  アラーム比較は行わない
  public static void rtcWriteCsec (long csec) {
    int dsec = DnT.dntDsecCsec (csec);
    if (rtcDsec != dsec) {  //時刻が変わった
      rtcDsec = dsec;
      int time = DnT.dntTimeDsec (dsec);
      int one, ten;
      one = DnT.dntHourTime (time);
      if (rtcSystem == 0) {  //12時間計
        int twelve = (11 * one) >> 7;  //one/12 (0<=one<=34)
        one -= 12 * twelve;
        rtcTenHour = twelve << 1 | (ten = (7 * one) >> 6);  //one/10 (0<=one<=18)
        rtcOneHour = one - 10 * ten;
      } else {  //24時間計
        rtcTenHour = ten = (13 * one) >> 7;  //one/10 (0<=one<=68)
        rtcOneHour = one - 10 * ten;
      }
      one = DnT.dntMinuTime (time);
      rtcTenMinu = ten = (13 * one) >> 7;  //one/10 (0<=one<=68)
      rtcOneMinu = one - 10 * ten;
      one = DnT.dntSecoTime (time);
      rtcTenSeco = ten = (13 * one) >> 7;  //one/10 (0<=one<=68)
      rtcOneSeco = one - 10 * ten;
    }
    int cday = DnT.dntCdayCsec (csec);
    if (rtcCday != cday) {  //日付が変わった
      if (cday < 3652 || 40177 <= cday) {  //歴通日が範囲外のとき
        if (true) {
          //歴通日を40177-3652=36525日単位で巻き戻して1980-01-01～2079-12-31にする
          //存在しない1900-02-29は出現しないが日付がずれる
          cday = DnT.dntFrem (cday - 3652, 36525) + 3652;
        } else {
          //歴通年を2080-1980=100年単位で巻き戻して1980～2079にする
          //日付はずれないが存在しない1900-02-29が出現する
          cday = DnT.dntCdayYearMontMday (
            DnT.dntFrem (DnT.dntYearCday (cday) - 1980, 100) + 1980,
            DnT.dntMontCday (cday),
            DnT.dntMdayCday (cday));
        }
      }
      rtcCday = cday;
      rtcWday = DnT.dntWdayCday (cday);
      int date = DnT.dntDateCday (cday);
      int one, ten;
      one = DnT.dntYearDate (date) - 1980;
      rtcLeapYear = one & 3;
      rtcTenYear = ten = (103 * one) >> 10;  //one/10 (0<=one<=178)
      rtcOneYear = one - 10 * ten;
      one = DnT.dntMontDate (date);
      rtcTenMont = ten = (7 * one) >> 6;  //one/10 (0<=one<=18)
      rtcOneMont = one - 10 * ten;
      one = DnT.dntMdayDate (date);
      rtcTenMday = ten = (13 * one) >> 7;  //one/10 (0<=one<=68)
      rtcOneMday = one - 10 * ten;
    }
  }  //rtcWriteCsec

  //rtcSetAutonomous (on)
  //  自律のON/OFF
  public static void rtcSetAutonomous (boolean on) {
    rtcAutonomousRequest = on;
    CoreTask.add (() -> {
      rtcAutonomous = rtcAutonomousRequest;
      rtcUpdateConnected ();  //連動を更新する
    });
  }  //rtcSetAutonomous

  //rtcSetByHost ()
  //  ホストの時計に合わせる
  public static void rtcSetByHost () {
    CoreTask.add (() -> {
      rtcTestMode = 0;  //テストOFF
      rtcTimerEn = true;  //タイマーON
      rtcOffset = rtcDefaultOffset;  //オフセットをデフォルトに戻す
      rtcWriteHostDateTime ();  //ホストの日時を書き込む
      rtcCompareAndUpdate ();  //アラームを比較してアラーム出力を更新する
    });
  }  //rtcSetByHost

  //rtcSetOneSeco (d)
  //  1秒カウンタの設定
  public static void rtcSetOneSeco (int d) {
    d &= 15;
    if (rtcOneSeco != d) {
      rtcOneSeco = d;
      rtcTimeWritten ();  //時刻が設定された
    }
  }  //rtcSetOneSeco

  //rtcSetTenSeco (d)
  //  10秒カウンタの設定
  public static void rtcSetTenSeco (int d) {
    d &= 7;
    if (rtcTenSeco != d) {
      rtcTenSeco = d;
      rtcTimeWritten ();  //時刻が設定された
    }
  }  //rtcSetTenSeco

  //rtcSetOneMinu (d)
  //  1分カウンタの設定
  public static void rtcSetOneMinu (int d) {
    d &= 15;
    if (rtcOneMinu != d) {
      rtcOneMinu = d;
      rtcTimeWritten ();  //時刻が設定された
    }
  }  //rtcSetOneMinu

  //rtcSetTenMinu (d)
  //  10分カウンタの設定
  public static void rtcSetTenMinu (int d) {
    d &= 7;
    if (rtcTenMinu != d) {
      rtcTenMinu = d;
      rtcTimeWritten ();  //時刻が設定された
    }
  }  //rtcSetTenMinu

  //rtcSetOneHour (d)
  //  1時カウンタの設定
  public static void rtcSetOneHour (int d) {
    d &= 15;
    if (rtcOneHour != d) {
      rtcOneHour = d;
      rtcTimeWritten ();  //時刻が設定された
    }
  }  //rtcSetOneHour

  //rtcSetTenHour (d)
  //  10時カウンタの設定
  public static void rtcSetTenHour (int d) {
    d &= 3;
    if (rtcTenHour != d) {
      rtcTenHour = d;
      rtcTimeWritten ();  //時刻が設定された
    }
  }  //rtcSetTenHour

  //rtcSetSystem (d)
  //  12/24セレクトレジスタの設定
  //  12時カウンタ/20時カウンタの値を変えずに解釈を変更する
  public static void rtcSetSystem (int d) {
    d &= 1;
    if (rtcSystem != d) {
      rtcSystem = d;
      rtcTimeWritten ();  //時刻が設定された
    }
  }  //rtcSetSystem

  //rtcTimeWritten ()
  //  時刻が設定された
  public static void rtcTimeWritten () {
    rtcWrittenEnd++;  //カウンタへの書き込みがあった
    if (rtcConnected) {  //連動ON
      int prevDsec = rtcDsec;
      rtcUpdateDsec ();  //カウンタから日通秒を読み出して連動を更新する
      if (rtcConnected) {  //連動ON→ON
        rtcOffset += 1000L * (long) (rtcDsec - prevDsec);
      }
    } else {  //連動OFF
      rtcUpdateDsec ();  //カウンタから日通秒を読み出して連動を更新する
    }
    rtcCompareAndUpdate ();  //アラームを比較してアラーム出力を更新する
  }  //rtcTimeWritten

  //rtcSetWday (d)
  //  曜日カウンタの設定
  public static void rtcSetWday (int d) {
    d &= 7;
    if (rtcWday != d) {
      rtcWday = d;
      rtcDateWritten ();  //日付が設定された
    }
  }  //rtcSetWday

  //rtcSetOneMday (d)
  //  1日カウンタの設定
  public static void rtcSetOneMday (int d) {
    d &= 15;
    if (rtcOneMday != d) {
      rtcOneMday = d;
      rtcDateWritten ();  //日付が設定された
    }
  }  //rtcSetOneMday

  //rtcSetTenMday (d)
  //  10日カウンタの設定
  public static void rtcSetTenMday (int d) {
    d &= 3;
    if (rtcTenMday != d) {
      rtcTenMday = d;
      rtcDateWritten ();  //日付が設定された
    }
  }  //rtcSetTenMday

  //rtcSetOneMont (d)
  //  1月カウンタの設定
  public static void rtcSetOneMont (int d) {
    d &= 15;
    if (rtcOneMont != d) {
      rtcOneMont = d;
      rtcDateWritten ();  //日付が設定された
    }
  }  //rtcSetOneMont

  //rtcSetTenMont (d)
  //  10月カウンタの設定
  public static void rtcSetTenMont (int d) {
    d &= 1;
    if (rtcTenMont != d) {
      rtcTenMont = d;
      rtcDateWritten ();  //日付が設定された
    }
  }  //rtcSetTenMont

  //rtcSetOneYear (d)
  //  1年カウンタの設定
  public static void rtcSetOneYear (int d) {
    d &= 15;
    if (rtcOneYear != d) {
      rtcOneYear = d;
      rtcDateWritten ();  //日付が設定された
    }
  }  //rtcSetOneYear

  //rtcSetTenYear (d)
  //  10年カウンタの設定
  public static void rtcSetTenYear (int d) {
    d &= 15;
    if (rtcTenYear != d) {
      rtcTenYear = d;
      rtcDateWritten ();  //日付が設定された
    }
  }  //rtcSetTenYear

  //rtcSetLeapYear (d)
  //  閏年カウンタの設定
  public static void rtcSetLeapYear (int d) {
    d &= 3;
    if (rtcLeapYear != d) {
      rtcLeapYear = d;
      rtcDateWritten ();  //日付が設定された
    }
  }  //rtcSetLeapYear

  //rtcDateWritten ()
  //  日付が設定された
  public static void rtcDateWritten () {
    rtcWrittenEnd++;  //カウンタへの書き込みがあった
    if (rtcConnected) {  //連動ON
      int prevCday = rtcCday;
      rtcUpdateCday ();  //カウンタから歴通日を読み出して連動を更新する
      if (rtcConnected) {  //連動ON→ON
        rtcOffset += 86400000L * (long) (rtcCday - prevCday);
      }
    } else {  //連動OFF
      rtcUpdateCday ();  //カウンタから歴通日を読み出して連動を更新する
    }
    rtcCompareAndUpdate ();  //アラームを比較してアラーム出力を更新する
  }  //rtcDateWritten

  //rtcAdjust ()
  //  アジャスト実行
  //  X68030実機で観測したところアジャストが反映されるまで60usくらい待つ必要がある
  //  ここでは即時反映する
  public static void rtcAdjust () {
    //テストONまたはカウンタ異常のときは何もしない
    if (rtcTestMode != 0 ||  //テストONまたは
        rtcCday < 0 || rtcDsec < 0) {  //カウンタ異常のとき
      return;  //何もしない
    }
    //秒未満の端数を切り捨てる
    rtcRoundDown ();  //秒未満の端数を切り捨てる
    //00秒のときは終わり
    int seco = 10 * rtcTenSeco + rtcOneSeco;
    if (seco == 0) {  //00秒
      return;
    }
    //秒カウンタを29捨30入する
    if (rtcTenSeco < 3) {  //29捨
      seco = -seco;
    } else {  //30入
      seco = 60 - seco;
    }
    rtcWriteCsec (DnT.dntCsecCdayDsec (rtcCday, rtcDsec + seco));  //歴通秒を書き込む
    if (rtcConnected) {  //連動ON
      rtcOffset += 1000L * (long) seco;
    }
    rtcCompareAndUpdate ();  //アラームを比較してアラーム出力を更新する
  }  //rtcAdjust

  //rtcSetTimerEn (timerEn)
  //  タイマーのON/OFF
  public static void rtcSetTimerEn (boolean timerEn) {
    if (rtcTimerEn != timerEn) {
      rtcTimerEn = timerEn;
      rtcUpdateConnected ();  //連動を更新する
    }
  }  //rtcSetTimerEn

  //rtcRoundDown ()
  //  タイマーリセット
  //  秒未満の端数を切り捨てる
  public static void rtcRoundDown () {
    if (rtcS32 != 0) {
      if (rtcConnected) {  //連動ON
        rtcOffset -= (long) ((rtcS32 * 1000) >> 5);
      }
      rtcS32 = 0;
    }
  }  //rtcRoundDown

  //rtcIncrementAndUpdate ()
  //  カウンタをインクリメントする
  //  歴通日、日通秒、連動を更新する
  //  アラーム比較は行わない
  public static void rtcIncrementAndUpdate () {
    if (rtcIncrementOnly ()) {  //日付が変わった
      rtcUpdateCday ();  //カウンタから歴通日を読み出して連動を更新する
    }
    rtcUpdateDsec ();  //カウンタから日通秒を読み出して連動を更新する
  }  //rtcIncrementAndUpdate

  //dateChanged = rtcIncrementOnly ()
  //  カウンタをインクリメントする
  public static boolean rtcIncrementOnly () {
    int t = RTC_NINE[rtcOneSeco];
    rtcOneSeco = t & 0xF;
    if ((t & 0x10) != 0) {
      t = RTC_FIVE[rtcTenSeco];
      rtcTenSeco = t & 0xF;
      if ((t & 0x10) != 0) {
        t = RTC_NINE[rtcOneMinu];
        rtcOneMinu = t & 0xF;
        if ((t & 0x10) != 0) {
          t = RTC_FIVE[rtcTenMinu];
          rtcTenMinu = t & 0xF;
          if ((t & 0x10) != 0) {
            t = RTC_HOUR[rtcSystem][rtcTenHour << 4 | rtcOneHour];
            rtcOneHour = t & 0xF;
            rtcTenHour = (t >> 4) & 0xF;
            if ((t & 0x100) != 0) {
              rtcWday = RTC_WDAY[rtcWday];
              int mont = rtcTenMont << 4 | rtcOneMont;
              t = RTC_MDAY[rtcLeapYear][mont][rtcTenMday << 4 | rtcOneMday];
              rtcOneMday = t & 0xF;
              rtcTenMday = (t >> 4) & 0xF;
              if ((t & 0x100) != 0) {
                t = RTC_MONT[mont];
                rtcOneMont = t & 0xF;
                rtcTenMont = (t >> 4) & 0xF;
                if ((t & 0x100) != 0) {
                  rtcLeapYear = RTC_LEAP[rtcLeapYear];
                  t = RTC_NINE[rtcOneYear];
                  rtcOneYear = t & 0xF;
                  if ((t & 0x10) != 0) {
                    t = RTC_NINE[rtcTenYear];
                    rtcTenYear = t & 0xF;
                  }
                }
              }
              return true;
            }
          }
        }
      }
    }
    return false;
  }  //rtcIncrementOnly

  //cday = rtcReadCday ()
  //  カウンタから歴通日を読み出す
  //  異常のとき-1を返す
  public static int rtcReadCday () {
    int cday = -1;
    if (rtcOneMday <= 9 &&
        rtcOneMont <= 9 &&
        rtcOneYear <= 9 &&
        rtcTenYear <= 9) {
      int mday = 10 * rtcTenMday + rtcOneMday;
      int mont = 10 * rtcTenMont + rtcOneMont;
      int year = 1980 + 10 * rtcTenYear + rtcOneYear;
      if (rtcLeapYear == (year & 3) &&
          1 <= mont && mont <= 12 &&
          1 <= mday && mday <= RTC_DYMT[year & 3][mont - 1]) {
        cday = DnT.dntCdayYearMontMday (year, mont, mday);
        if (rtcWday != DnT.dntWdayCday (cday)) {
          cday = -1;
        }
      }
    }
    return cday;
  }  //rtcReadCday

  //rtcUpdateCday ()
  //  カウンタから歴通日を読み出して連動を更新する
  public static void rtcUpdateCday () {
    rtcCday = rtcReadCday ();  //カウンタから歴通日を読み出す
    rtcUpdateConnected ();  //連動を更新する
  }  //rtcUpdateCday

  //dsec = rtcReadDsec ()
  //  カウンタから日通秒を読み出す
  //  異常のとき-1を返す
  public static int rtcReadDsec () {
    int dsec = -1;
    if (rtcOneSeco < 10 &&
        rtcTenSeco < 6 &&
        rtcOneMinu < 10 &&
        rtcTenMinu < 6 &&
        rtcOneHour < 10) {
      int seco = 10 * rtcTenSeco + rtcOneSeco;
      int minu = 10 * rtcTenMinu + rtcOneMinu;
      if (rtcSystem == 0) {
        if (10 * (rtcTenHour & 1) + rtcOneHour < 12) {
          int hour = 6 * (rtcTenHour & 2) + 10 * (rtcTenHour & 1) + rtcOneHour;
          dsec = DnT.dntDsecHourMinuSeco (hour, minu, seco);
        }
      } else {
        if (10 * rtcTenHour + rtcOneHour < 24) {
          int hour = 10 * rtcTenHour + rtcOneHour;
          dsec = DnT.dntDsecHourMinuSeco (hour, minu, seco);
        }
      }
    }
    return dsec;
  }  //rtcReadDsec

  //rtcUpdateDsec ()
  //  カウンタから日通秒を読み出して連動を更新する
  public static void rtcUpdateDsec () {
    rtcDsec = rtcReadDsec ();  //カウンタから日通秒を読み出す
    rtcUpdateConnected ();  //連動を更新する
  }  //rtcUpdateDsec

  //rtcUpdateConnected ()
  //  連動を更新する
  //  rtcAutonomous、rtcTestMode、rtcTimerEn、rtcDsec、rtcCdayのいずれかを更新したとき呼び出す
  //  連動OFF→ONのとき1/32秒カウンタをゼロクリアし歴通ミリ秒オフセットを再計算する
  public static void rtcUpdateConnected () {
    boolean connected = (!rtcAutonomous &&  //自律OFFかつ
                         rtcTestMode == 0 &&  //テストOFFかつ
                         rtcTimerEn &&  //タイマーONかつ
                         0 <= rtcCday && 0 <= rtcDsec);  //カウンタ正常
    if (rtcConnected != connected) {
      rtcConnected = connected;
      if (!connected) {  //連動ON→OFF
        rtcWrittenStart = rtcWrittenEnd;
        if (rtcCday < 0 || rtcDsec < 0) {  //カウンタ異常
          rtcWrittenEnd++;  //復元しない
        }
      } else if (rtcWrittenStart == rtcWrittenEnd) {  //連動OFF→ONかつ連動OFFの間にカウンタへの書き込みがなかった
        //復元する
        rtcWriteHostDateTime ();  //ホストの日時を書き込む
        rtcCompareAndUpdate ();  //アラームを比較してアラーム出力を更新する
      } else {  //連動OFF→ONかつ連動OFFの間にカウンタへの書き込みがあった
        //復元しない
        rtcS32 = 0;
        rtcOffset = DnT.dntCmilCdayDsec (rtcCday, rtcDsec) - System.currentTimeMillis ();
      }
    }
  }  //rtcUpdateConnected


  //------------------------------------------------
  //Alarm アラーム

  //ALARM出力
  //  観測
  //    RTCのALARM出力はMFPのGPIP0で観測できる
  //  論理
  //    ALARM出力はアラーム比較結果と1Hzと16Hz(すべて負論理)のANDである
  //    アラームがON(MODE bit2=%1)でマッチしているとき(アラームリセット後のdon't careを含む)
  //      アラーム比較結果が0なので1Hzと16Hzに関係なく0が出力される
  //        00000000000000000000000000000000
  //    アラームがON(MODE bit2=%1)でマッチしていないか、アラームがOFF(MODE bit2=%0)のとき
  //      アラーム比較結果が1なので1Hzと16Hz(すべて負論理)のANDが出力される
  //      1HzがON、16HzがON(RESET bit3-2=%00)のとき
  //        1Hzの0→1と16Hzの0→1のANDが出力される
  //        00000000000000000101010101010101
  //      1HzがON、16HzがOFF(RESET bit3-2=%01)のとき
  //        1Hzの0→1と16Hzの1のANDが出力される
  //        00000000000000001111111111111111
  //      1HzがOFF、16HzがON(RESET bit3-2=%10)のとき
  //        1Hzの1と16Hzの0→1のANDが出力される
  //        01010101010101010101010101010101
  //      1HzがOFF、16HzがOFF(RESET bit3-2=%11)のとき
  //        1Hzの1と16Hzの1のANDが出力される
  //        11111111111111111111111111111111
  //  メモ
  //    アラーム起動を設定するとき1Hzと16HzをOFF(RESET bit3-2=11)にしておかないと
  //    ALARM出力が頻繁に0になるので電源が切れたとしてもすぐに再起動してしまう

  //アラームレジスタ
  //  bank=1 num=2,3,4,5,6,7,8 address=0x05,0x07,0x09,0x0b,0x0d,0x0f,0x11
  //  1分、10分、1時、10時、曜日、1日、10日カウンタとすべて一致したとき
  //  アラーム比較結果(負論理)が0になる
  //  それぞれアラームリセット後最初の書き込みまでdon't careで常に一致しているとみなされる
  //  アラームリセットで全項目don't careになりアラーム出力が始まってしまうので
  //  通常はアラーム出力をOFFにしてからアラームリセットする
  //  ここでは16=don't careとする
  public static int rtcAlarmOneMinu;  //1分アラームレジスタ
  public static int rtcAlarmTenMinu;  //10分アラームレジスタ
  public static int rtcAlarmOneHour;  //1時アラームレジスタ
  public static int rtcAlarmTenHour;  //10時アラームレジスタ
  public static int rtcAlarmWday;     //曜日アラームレジスタ
  public static int rtcAlarmOneMday;  //1日アラームレジスタ
  public static int rtcAlarmTenMday;  //10日アラームレジスタ

  //モードレジスタ
  //  bank=* num=13 address=0x1b
  //  bit2  0=アラームOFF,1=アラームON
  public static boolean rtcAlarmEn;

  //リセットレジスタ(write-only)
  //  bank=* num=15 address=0x1f
  //  bit3  0=1Hz ON
  //  bit2  0=16Hz ON
  //  読み出しは常に0
  public static int rtcHzMode;  //1Hz/16Hzモード。0～3。bit1=1Hz,bit0=16Hz。0=ON,1=OFF

  //前回アラームを比較したときの日通分
  public static int rtcAlarmDmin;

  //状態
  //  アラーム比較結果
  //  アラームONかつアラームレジスタと日時カウンタが一致
  public static boolean rtcMatcherHigh;  //アラーム比較結果(負論理)。false=一致,true=不一致
  //  1Hz/16Hz出力
  //  00  00000000000000000101010101010101
  //  01  00000000000000001111111111111111
  //  10  01010101010101010101010101010101
  //  11  11111111111111111111111111111111
  public static boolean rtcHzHigh;  //1Hz/16Hz出力(負論理)
  //  ALARM出力
  //  rtcAlarmHigh = rtcMatcherHigh && rtcHzHigh
  public static boolean rtcAlarmHigh;  //ALARM出力(負論理)。false=Low,true=High

  //rtcAlarmRestore ()
  //  復元
  public static void rtcAlarmRestore () {
    int[] alarm = Settings.sgsGetIntArray ("rtcalarm", 7, 16);
    rtcAlarmOneMinu = alarm[0] == 16 ? 16 : alarm[0] & 15;
    rtcAlarmTenMinu = alarm[1] == 16 ? 16 : alarm[1] & 7;
    rtcAlarmOneHour = alarm[2] == 16 ? 16 : alarm[2] & 15;
    rtcAlarmTenHour = alarm[3] == 16 ? 16 : alarm[3] & 3;
    rtcAlarmWday = alarm[4] == 16 ? 16 : alarm[4] & 7;
    rtcAlarmOneMday = alarm[5] == 16 ? 16 : alarm[5] & 15;
    rtcAlarmTenMday = alarm[6] == 16 ? 16 : alarm[6] & 3;
    rtcAlarmEn = Settings.sgsGetOnOff ("rtcalarmen", false);
    rtcHzMode = Settings.sgsGetInt ("rtchzmode", 0, 0, 3);
  }  //rtcAlarmRestore

  //rtcAlarmSave ()
  //  保存
  public static void rtcAlarmSave () {
    int[] alarm = new int[7];
    alarm[0] = rtcAlarmOneMinu;
    alarm[1] = rtcAlarmTenMinu;
    alarm[2] = rtcAlarmOneHour;
    alarm[3] = rtcAlarmTenHour;
    alarm[4] = rtcAlarmWday;
    alarm[5] = rtcAlarmOneMday;
    alarm[6] = rtcAlarmTenMday;
    Settings.sgsPutIntArray ("rtcalarm", alarm);
    Settings.sgsPutOnOff ("rtcalarmen", rtcAlarmEn);
    Settings.sgsPutInt ("rtchzmode", rtcHzMode);
  }  //rtcAlarmSave

  //rtcAlarmInit ()
  //  初期化
  public static void rtcAlarmInit () {
    rtcAlarmDmin = -1;
    rtcMatcherHigh = rtcCompareOnly ();  //アラームを比較する
    rtcHzHigh = true;
    rtcAlarmHigh = rtcMatcherHigh && rtcHzHigh;
  }  //rtcAlarmInit

  //rtcAlarmReset ()
  //  リセット
  public static void rtcAlarmReset () {
    rtcCompareAndUpdate ();  //アラームを比較してアラーム出力を更新する
  }  //rtcAlarmReset

  //rtcSetAlarmOneMinu (d)
  //  1分アラームレジスタの設定
  public static void rtcSetAlarmOneMinu (int d) {
    d &= 15;
    rtcAlarmOneMinu = d;
    rtcCompareAndUpdate ();  //アラームを比較してアラーム出力を更新する
  }  //rtcSetAlarmOneMinu

  //rtcSetAlarmTenMinu (d)
  //  10分アラームレジスタの設定
  public static void rtcSetAlarmTenMinu (int d) {
    d &= 7;
    rtcAlarmTenMinu = d;
    rtcCompareAndUpdate ();  //アラームを比較してアラーム出力を更新する
  }  //rtcSetAlarmTenMinu

  //rtcSetAlarmOneHour (d)
  //  1時アラームレジスタの設定
  public static void rtcSetAlarmOneHour (int d) {
    d &= 15;
    rtcAlarmOneHour = d;
    rtcCompareAndUpdate ();  //アラームを比較してアラーム出力を更新する
  }  //rtcSetAlarmOneHour

  //rtcSetAlarmTenHour (d)
  //  10時アラームレジスタの設定
  public static void rtcSetAlarmTenHour (int d) {
    d &= 3;
    rtcAlarmTenHour = d;
    rtcCompareAndUpdate ();  //アラームを比較してアラーム出力を更新する
  }  //rtcSetAlarmTenHour

  //rtcSetAlarmWday (d)
  //  曜日アラームレジスタの設定
  public static void rtcSetAlarmWday (int d) {
    d &= 7;
    rtcAlarmWday = d;
    rtcCompareAndUpdate ();  //アラームを比較してアラーム出力を更新する
  }  //rtcSetAlarmWday

  //rtcSetAlarmOneMday (d)
  //  1日アラームレジスタの設定
  public static void rtcSetAlarmOneMday (int d) {
    d &= 15;
    rtcAlarmOneMday = d;
    rtcCompareAndUpdate ();  //アラームを比較してアラーム出力を更新する
  }  //rtcSetAlarmOneMday

  //rtcSetAlarmTenMday (d)
  //  10日アラームレジスタの設定
  public static void rtcSetAlarmTenMday (int d) {
    d &= 3;
    rtcAlarmTenMday = d;
    rtcCompareAndUpdate ();  //アラームを比較してアラーム出力を更新する
  }  //rtcSetAlarmTenMday

  //rtcSetAlarmEn (alarmEn)
  //  アラームのON/OFF
  public static void rtcSetAlarmEn (boolean alarmEn) {
    if (rtcAlarmEn != alarmEn) {
      rtcAlarmEn = alarmEn;
      rtcCompareAndUpdate ();  //アラームを比較してアラーム出力を更新する
    }
  }  //rtcSetAlarmEn

  //rtcSetHzMode (hzMode);
  //  1Hz/16Hzの設定
  public static void rtcSetHzMode (int hzMode) {
    if (rtcHzMode != hzMode) {
      rtcHzMode = hzMode;
    }
  }  //rtcSetHzMode

  //rtcWildcardify ()
  //  アラームリセット
  //  すべての項目をワイルドカードにする
  public static void rtcWildcardify () {
    rtcAlarmOneMinu = 16;
    rtcAlarmTenMinu = 16;
    rtcAlarmOneHour = 16;
    rtcAlarmTenHour = 16;
    rtcAlarmWday = 16;
    rtcAlarmOneMday = 16;
    rtcAlarmTenMday = 16;
    rtcCompareAndUpdate ();  //アラームを比較してアラーム出力を更新する
  }  //rtcWildcardify

  //high = rtcCompareOnly ()
  //  アラームを比較する
  //  アラームONかつアラームレジスタと日時カウンタが一致を負論理で返す
  public static boolean rtcCompareOnly () {
    return !(rtcAlarmEn &&
             (rtcAlarmOneMinu == 16 || rtcAlarmOneMinu == rtcOneMinu) &&
             (rtcAlarmTenMinu == 16 || rtcAlarmTenMinu == rtcTenMinu) &&
             (rtcAlarmOneHour == 16 || rtcAlarmOneHour == rtcOneHour) &&
             (rtcAlarmTenHour == 16 || rtcAlarmTenHour == rtcTenHour) &&
             (rtcAlarmWday == 16 || rtcAlarmWday == rtcWday) &&
             (rtcAlarmOneMday == 16 || rtcAlarmOneMday == rtcOneMday) &&
             (rtcAlarmTenMday == 16 || rtcAlarmTenMday == rtcTenMday));
  }  //rtcCompareOnly

  //rtcCompareAndUpdate ()
  //  アラームを比較してアラーム出力を更新する
  public static void rtcCompareAndUpdate () {
    boolean high = rtcCompareOnly ();  //アラームを比較する
    if (rtcMatcherHigh != high) {
      rtcMatcherHigh = high;
      rtcAlarmOutput ();  //アラーム比較結果と1Hz/16Hz出力をまとめてALARM出力を作る
    }
  }  //rtcCompareAndUpdate

  //rtcAlarmOutput ()
  //  アラーム比較結果と1Hz/16Hz出力をまとめてALARM出力を作る
  public static void rtcAlarmOutput () {
    boolean high = rtcMatcherHigh && rtcHzHigh;
    if (rtcAlarmHigh != high) {
      rtcAlarmHigh = high;
      rtcAlarmExport ();  //現在のALARM出力をMFPと電源制御に通知する
    }
  }  //rtcAlarmOutput

  //rtcAlarmExport ()
  //  現在のALARM出力をMFPと電源制御に通知する
  public static void rtcAlarmExport () {
    if (rtcAlarmHigh) {  //ALARM出力がHigh
      if (!PowerControl.PWC_ON ||
          PowerControl.pwcVcc1On) {  //VCC1がON
        MC68901.mfpAlarmRise ();  //MFPに通知
      }
      if (PowerControl.PWC_ON) {
        PowerControl.pwcTurnOff ();  //電源制御に通知
      }
    } else {  //ALARM出力がLow
      if (!PowerControl.PWC_ON ||
          PowerControl.pwcVcc1On) {  //VCC1がON
        MC68901.mfpAlarmFall ();  //MFPに通知
      }
      if (PowerControl.PWC_ON) {
        PowerControl.pwcTurnOn ();  //電源制御に通知
      }
    }
  }  //rtcAlarmExport


  //------------------------------------------------
  //Clkout CLKOUT

  //CLKOUTセレクタ
  //  bank=1 num=0 address=0x01
  //  0  Z        点灯。VCC2でプルアップされている
  //  1  16384Hz  (ここでは点灯)
  //  2  1024Hz   (〃)
  //  3  128Hz    (〃)
  //  4  16Hz
  //  5  1Hz
  //  6  1/64Hz   (ここでは消灯)
  //  7  Low      消灯
  //  秒カウンタと連動して動く
  //
  //  X68000ではRP5C15のCLKOUT信号がTIMER-LEDに接続されており、
  //  このレジスタはTIMER-LEDの状態を制御するために使われている
  //  IOCS _ALARMMODやIOCS _ALARMSETでアラームを許可したとき
  //    0が書き込まれてTIMER-LEDが点灯する
  //  IOCS _ALARMMODでアラームを禁止したとき
  //    7が書き込まれてTIMER-LEDが消灯する
  //  アラームで起動したとき
  //    5が書き込まれてTIMER-LEDが点滅する
  //  KRAMD.SYSなどのRAMディスクはTIMER-LEDをアクセスランプとして使っている
  //  #1000ではCLKOUTがMFPのGPIP5に接続されることになっていたがX68000では接続されていない
  //
  public static int rtcClkoutMode;  //CLKOUTモード

  //遅延点灯
  //  点灯の指示があったとき一瞬でも確実に点灯させる
  public static boolean rtcClkoutLatched;

  //状態
  public static boolean rtcClkoutHigh;  //false=Low,true=High

  //rtcClkoutRestore ()
  //  復元
  public static void rtcClkoutRestore () {
    rtcClkoutMode = Settings.sgsGetInt ("rtcclkout", 7, 0, 7);
  }  //rtcClkoutRestore

  //rtcClkoutSave ()
  //  保存
  public static void rtcClkoutSave () {
    Settings.sgsPutInt ("rtcclkout", rtcClkoutMode);
  }  //rtcClkoutSave

  //rtcClkoutInit ()
  //  初期化
  public static void rtcClkoutInit () {
    rtcClkoutLatched = false;
    rtcClkoutHigh = false;
  }  //rtcClkoutInit

  //rtcSetClkoutMode (d)
  //  CLKOUTモードの設定
  public static void rtcSetClkoutMode (int d) {
    d &= 7;
    if (rtcClkoutMode != d) {
      rtcClkoutMode = d;
      if (d <= 3) {  //点灯
        rtcClkoutLatched = true;
      }
    }
  }  //rtcSetClkoutMode


  //------------------------------------------------
  //Test テスト

  //年通日
  //  yday  年通日。1～366
  //  yda0  毎月0日の年通日。0～
  //  yday=yda0+mday
  //  [leap][mont-1]=yda0
  public static final int[] RTC_YDA0_L0 = {
    //  1   2   3    4    5    6    7    8    9   10   11      12
    // 31  29  31   30   31   30   31   31   30   31   30      31
    0, 31, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335,  //366
  };
  public static final int[] RTC_YDA0_L1 = {
    //  1   2   3    4    5    6    7    8    9   10   11      12
    // 31  28  31   30   31   30   31   31   30   31   30      31
    0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334,  //365
  };
  public static final int[][] RTC_YDA0 = {
    RTC_YDA0_L0, RTC_YDA0_L1, RTC_YDA0_L1, RTC_YDA0_L1,
  };
  //  [leap][yday-1]=mont<<5|mday
  public static final int[] RTC_FROM_YDAY_L0 = new int[366];
  public static final int[] RTC_FROM_YDAY_L1 = new int[365];
  public static final int[][] RTC_FROM_YDAY = {
    RTC_FROM_YDAY_L0, RTC_FROM_YDAY_L1, RTC_FROM_YDAY_L1, RTC_FROM_YDAY_L1,
  };
  static {
    for (int leap = 0; leap < 2; leap++) {
      int yda0 = 0;
      for (int mont = 1; mont <= 12; mont++) {
        int dymt = RTC_DYMT[leap][mont - 1];
        for (int mday = 1; mday <= dymt; mday++) {
          int yday = yda0 + mday;
          RTC_FROM_YDAY[leap][yday - 1] = mont << 5 | mday;
        }
        yda0 += dymt;
      }
    }
  }

  //テストモード
  //  bit0が1秒、bit1が1分、bit2が1日、bit3が1年の各カウンタに対応し、
  //  下位からそのカウンタへの繰り上がりに16384Hzを入力する
  //  X68030実機での観測により、bitとカウンタの対応を確認した
  //  また、20000HzのTimer-Cが1000進む間に約820進むことと、
  //  RTCの入力が32768Hzであることから、16384Hzで間違いないと判断した
  //  1日カウンタへの繰り上がりは曜日カウンタにも反映される
  //  1年カウンタへの繰り上がりは閏年カウンタにも反映される
  //  複数のbitをセットしたとき、上位のカウンタをそれ以上速く回せないため、
  //  上位のテストは下位のテストの繰り上がりの影響を受けない
  //  タイマーOFFでも動作する
  //
  //制約
  //  1msまでを除いて
  //    不正な値から開始できない
  //    閏年が考慮されない
  //  アラームと共存できない
  //
  //手順
  //  増分=(現在ピコ秒時刻-開始時ピコ秒時刻)*16384/1000000000000
  //    開始時から現在までの16384Hzの増分
  //  1msまで
  //    インクリメントを増分回繰り返す
  //  さもなくば
  //    カウンタから取り出す
  //      秒(0～59)
  //      日通分(0～1439)
  //      曜日(0～6)
  //      年通日(1～356)
  //      閏年(0～3)
  //      年(0～99)
  //    秒テストあり
  //      秒+=増分
  //    分テストあり
  //      日通分+=増分
  //    分テストなし
  //      日通分+=秒/60
  //    秒%=60
  //    日テストあり
  //      曜日=(曜日+増分)%7
  //      年通日+=増分
  //    日テストなし
  //      曜日=(曜日+日通分/1440)%7
  //      年通日+=日通分/1440
  //    日通分%=1440
  //    年テストあり
  //      閏年=(閏年+増分)%4
  //      年=(年+増分)%100
  //    年テストなし
  //      閏年=(閏年+(年通日-1)/365)%4
  //      年+=(年通日-1)/365
  //    年通日=(年通日-1)%365+1
  //  開始時ピコ秒時刻+=増分*1000000000000/16384

  //テストレジスタ(write-only)
  //  bank=* num=14 address=0x1d
  //  bit3  年テスト。0=OFF,1=ON
  //  bit2  日テスト。0=OFF,1=ON
  //  bit1  分テスト。0=OFF,1=ON
  //  bit0  秒テスト。0=OFF,1=ON
  //  読み出しは常に0
  public static int rtcTestMode;

  //開始時ピコ秒時刻
  public static long rtcTestTime;

  //rtcTestRestore ()
  //  復元
  public static void rtcTestRestore () {
    rtcTestMode = Settings.sgsGetInt ("rtctestmode", 0, 0, 15);
  }  //rtcTestRestore

  //rtcTestSave ()
  //  保存
  public static void rtcTestSave () {
    Settings.sgsPutInt ("rtctestmode", rtcTestMode);
  }  //rtcTestSave

  //rtcTestInit ()
  //  初期化
  public static void rtcTestInit () {
    rtcTestTime = 0L;
  }  //rtcTestInit

  //rtcTestUpdate ()
  //  テストでカウンタを更新する
  public static void rtcTestUpdate () {
    //開始時から現在までの16384Hzの増分
    //  下位からの繰り上がりの代わりに加える
    //  1000000000000/16384 = 244140625/4
    int delta = (int) (((XEiJ.mpuClockTime - rtcTestTime) << 2) / 244140625L);
    if (delta <= 20) {  //1msまで
      for (int i = 0; i < delta; i++) {
        rtcIncrementOnly ();
      }
    } else {  //さもなくば
      //カウンタから読み出す
      int seco = Math.min (60 - 1, 10 * rtcTenSeco + rtcOneSeco);
      int minu = Math.min (60 - 1, 10 * rtcTenMinu + rtcOneMinu);
      int hour = 10 * rtcTenHour + rtcOneHour;
      if (rtcSystem == 0 && 20 <= hour) {
        hour -= 8;
      }
      hour = Math.min (24 - 1, hour);
      int dmin = 60 * hour + minu;
      int wday = rtcWday;
      int mday = Math.max (1, Math.min (31, 10 * rtcTenMday + rtcOneMday));
      int mont = Math.max (1, Math.min (12, 10 * rtcTenMont + rtcOneMont));
      int yday = RTC_YDA0_L1[mont - 1] + mday;
      int leap = rtcLeapYear;
      int year = Math.min (100 - 1, 10 * rtcTenYear + rtcOneYear);
      //増分を加える
      if ((rtcTestMode & 1) != 0) {  //秒テストあり
        seco += delta;  //増分を秒に加える
      }
      if ((rtcTestMode & 2) != 0) {  //分テストあり
        dmin += delta;  //増分を日通分に加える
        seco %= 60;  //秒からの繰り上がりを捨てる
      } else {  //分テストなし
        int t = seco / 60;  //秒からの繰り上がりを日通分に加える
        dmin += t;
        seco -= 60 * t;
      }
      if ((rtcTestMode & 4) != 0) {  //日テストあり
        wday += delta;  //増分を曜日に加える
        yday += delta;  //増分を年通日に加える
        dmin %= 1440;  //日通分からの繰り上がりを捨てる
      } else {  //日テストなし
        int t = dmin / 1440;  //日通分からの繰り上がりを年通日に加える
        wday += t;
        yday += t;
        dmin -= 1440 * t;
      }
      wday %= 7;  //曜日からの繰り上がりを捨てる
      if ((rtcTestMode & 8) != 0) {  //年テストあり
        leap += delta;  //増分を閏年に加える
        year += delta;  //増分を年に加える
        yday = (yday - 1) % 365 + 1;  //年通日からの繰り上がりを捨てる
      } else {  //年テストなし
        int t = (yday - 1) / 365;  //年通日からの繰り上がりを年に加える
        leap += t;
        year += t;
        yday -= 365 * t;
      }
      leap &= 3;  //閏年からの繰り上がりを捨てる
      year %= 100;  //年からの繰り上がりを捨てる
      //カウンタに書き込む
      int ten;
      rtcOneSeco = ten = (13 * seco) >> 7;  //seco/10 (0<=seco<=68)
      rtcTenSeco = seco - 10 * ten;
      hour = dmin / 60;
      minu = dmin - 60 * hour;
      rtcOneMinu = ten = (13 * minu) >> 7;  //minu/10 (0<=minu<=68)
      rtcTenMinu = minu - 10 * ten;
      if (rtcSystem == 0 && 12 <= hour) {
        hour += 8;
      }
      rtcOneHour = ten = (13 * hour) >> 7;  //hour/10 (0<=hour<=68)
      rtcTenHour = hour - 10 * ten;
      rtcWday = wday;
      mday = RTC_FROM_YDAY_L1[yday - 1];
      mont = mday >> 5;
      mday &= 31;
      rtcOneMday = ten = (13 * mday) >> 7;  //mday/10 (0<=mday<=68)
      rtcTenMday = mday - 10 * ten;
      rtcOneMont = ten = (7 * mont) >> 6;  //mont/10 (0<=mont<=18)
      rtcTenMont = mont - 10 * ten;
      rtcLeapYear = leap;
      rtcOneYear = ten = (103 * year) >> 10;  //year/10 (0<=year<=178)
      rtcTenYear = year - 10 * ten;
    }
    //歴通日と日通秒
    rtcCday = rtcReadCday ();  //カウンタから歴通日を読み出す
    rtcDsec = rtcReadDsec ();  //カウンタから日通秒を読み出す
    //新しい開始時ピコ秒時刻
    rtcTestTime += (244140625L * delta) >> 2;
  }  //rtcTestUpdate

  //rtcSetTestMode (d)
  //  テストモードの設定
  public static void rtcSetTestMode (int d) {
    d &= 15;
    if (rtcTestMode != d) {
      rtcTestMode = d;
      rtcUpdateConnected ();  //連動を更新する
      if (d != 0) {  //テスト開始またはモード変更
        rtcTestTime = XEiJ.mpuClockTime;
      }
    }
  }  //rtcSetTestMode


  //------------------------------------------------
  //Process プロセス

  public static final boolean L = false;
  public static final boolean H = true;

  //1Hz/16Hzの出力パターン
  //  [hzMode<<5|s32]
  public static final boolean[] RTC_HZ_PATTERN = {
    L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,H,L,H,L,H,L,H,L,H,L,H,L,H,L,H,  //%00 1Hz ON,16Hz ON
    L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,  //%01 1Hz ON,16Hz OLF
    L,H,L,H,L,H,L,H,L,H,L,H,L,H,L,H,L,H,L,H,L,H,L,H,L,H,L,H,L,H,L,H,  //%10 1Hz OFF,16Hz ON
    H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,  //%11 1Hz OFF,16Hz OFF
  };

  //CLKOUTの出力パターン
  //  [clkoutMode<<5|s32]
  public static final boolean[] RTC_CLKOUT_PATTERN = {
    H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,  //0 H
    H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,  //1 H
    H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,  //2 H
    H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,  //3 H
    H,L,H,L,H,L,H,L,H,L,H,L,H,L,H,L,H,L,H,L,H,L,H,L,H,L,H,L,H,L,H,L,  //4 16Hz
    H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,H,L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,  //5 1Hz
    L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,  //6 L
    L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,L,  //7 L
  };

  //ティッカー
  public static long rtcNextTime;  //ティッカーの予約時刻(ピコ秒)

  //タスク
  public static int rtcTaskNumber;  //動作中のタスクの番号

  //インジケータ
  public static boolean rtcProcessAlarmHigh;
  public static boolean rtcProcessClkoutHigh;
  public static boolean rtcProcessSync;

  //rtcProcessInit ()
  //  初期化
  public static void rtcProcessInit () {
    rtcNextTime = XEiJ.FAR_FUTURE;
    rtcTaskNumber = 0;
    rtcProcessAlarmHigh = rtcAlarmHigh;
    rtcProcessClkoutHigh = rtcClkoutHigh;
    rtcProcessSync = rtcConnected;
  }  //rtcProcessInit

  //rtcProcessTini ()
  //  後始末
  public static void rtcProcessTini () {
    //ティッカーを終了する
    TickerQueue.tkqRemove (RTC_TICKER);
    //タスクを終了する
    rtcTaskNumber++;
  }  //rtcProcessTini

  //rtcProcessReset ()
  //  リセット
  //  タスクを終了してティッカーを開始する
  public static void rtcProcessReset () {
    //タスクを終了する
    rtcTaskNumber++;
    //ティッカーを開始する
    final long intervalPico = XEiJ.TMR_FREQ >> 5;  //1/32秒
    final long delayPico = intervalPico - XEiJ.mpuClockTime % intervalPico;
    rtcNextTime = XEiJ.mpuClockTime + delayPico;
    TickerQueue.tkqAdd (RTC_TICKER, rtcNextTime);
  }  //rtcProcessReset

  //RTC_TICKER
  //  ティッカー
  public static final TickerQueue.Ticker RTC_TICKER = new TickerQueue.Ticker () {
    @Override protected void tick () {
      rtcProcessMain ();  //プロセス本体
      rtcNextTime += XEiJ.TMR_FREQ >> 5;  //1/32秒
      TickerQueue.tkqAdd (RTC_TICKER, rtcNextTime);
    }
  };  //RTC_TICKER

  //class TaskRunner
  //  タスクのランナー
  public static class TaskRunner implements SimpleTask.Runner {
    int number;  //自分の番号
    TaskRunner (int number) {
      this.number = number;
    }
    @Override public int runN (int n) {
      if (rtcTaskNumber != number) {  //番号が変わった
        return 0;  //終了
      }
      rtcProcessMain ();  //プロセス本体
      return 1;
    }  //runN
  };  //TaskRunner

  //rtcProcessMain ()
  //  プロセス本体
  public static void rtcProcessMain () {
    //1/32秒カウンタをインクリメントする
    rtcS32 = (rtcS32 + 1) & 31;
    if (rtcTestMode == 0 && rtcTimerEn) {  //テストOFFかつタイマーONのとき
      //1秒毎に
      if (rtcS32 == 0) {
        if (rtcConnected) {  //連動ON
          rtcWriteHostDateTime ();  //ホストの日時を書き込む
        } else {  //連動OFF
          rtcIncrementAndUpdate ();  //カウンタをインクリメントする
        }
        //1分毎にアラームを比較する
        int dmin = rtcDsec < 0 ? -1 : rtcDsec / 60;
        if (rtcAlarmDmin != dmin) {  //分の区切りを跨いだ
          rtcAlarmDmin = dmin;
          rtcCompareAndUpdate ();  //アラームを比較してアラーム出力を更新する
        }
      }
    }
    //ALARM出力を更新する
    rtcHzHigh = RTC_HZ_PATTERN[rtcHzMode << 5 | rtcS32];
    rtcAlarmOutput ();  //アラーム比較結果と1Hz/16Hz出力をまとめてALARM出力を作る
    //CLKOUT出力を更新する
    rtcClkoutHigh = rtcClkoutLatched || RTC_CLKOUT_PATTERN[rtcClkoutMode << 5 | rtcS32];
    rtcClkoutLatched = false;
    //ALARM出力とCLKOUT出力をインジケータに反映する
    boolean sync = rtcConnected && rtcOffset == rtcDefaultOffset;
    if (rtcProcessAlarmHigh != rtcAlarmHigh ||
        rtcProcessClkoutHigh != rtcClkoutHigh ||
        rtcProcessSync != sync) {  //変化あり
      rtcProcessAlarmHigh = rtcAlarmHigh;
      rtcProcessClkoutHigh = rtcClkoutHigh;
      rtcProcessSync = sync;
      Indicator.indUpdateLED ();
    }
  }  //rtcProcessMain

  //rtcTurnOn ()
  //  VCC1がONになった
  //  実機のRTCはVCC2とバックアップ電池で動作するので何も変わらない
  //  ここではタスクを終了する
  //  ティッカーはリセットで開始する
  public static void rtcTurnOn () {
    //タスクを終了する
    rtcTaskNumber++;
  }  //rtcVcc1On

  //rtcTurnOff ()
  //  VCC1がOFFになった
  //  実機のRTCはVCC2とバックアップ電池で動作するので何も変わらない
  //  ここではティッカーを終了してタスクを開始する
  public static void rtcTurnOff () {
    //ティッカーを終了する
    TickerQueue.tkqRemove (RTC_TICKER);
    //タスクを開始する
    rtcTaskNumber++;
    final long intervalNano = 1000000000L >> 5;  //1/32秒
    final long delayNano = (intervalNano -
                            ((System.currentTimeMillis () % 1000) * 1000000L) % intervalNano);
    new SimpleTask (
      XEiJ.tmrScheduler,
      new TaskRunner (rtcTaskNumber)
      ).fixedPeriodNano (delayNano, intervalNano);
  }  //rtcVcc1Off


}  //class RP5C15
