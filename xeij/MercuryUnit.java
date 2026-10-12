//========================================================================================
//  MercuryUnit.java
//    en:Mercury-Unit V4
//    ja:まーきゅりーゆにっとV4
//  Copyright (C) 2003-2026 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.util.*;  //Arrays

public class MercuryUnit {

  public static final boolean MU4_ON = true;
  public static boolean mu4OnRequest;
  public static boolean mu4On;
  public static boolean mu4OnPositive;
  public static boolean mu4OnNegative;
  public static boolean mu4OutputEnable;

  public static final boolean MU4_DEBUG = false;

  //mu4Init ()
  //  初期化
  public static void mu4Init () {
    //パラメータ
    mu4OnRequest = Settings.sgsGetOnOff ("mercury");
    mu4OutputEnable = Settings.sgsGetOnOff ("mercuryoe");
    //エルミート補間
    mu4Hermite640 = new Hermite (640, 2500);
    mu4Hermite882 = new Hermite (882, 2500);
    mu4Hermite960 = new Hermite (960, 2500);
    mu4Hermite1280 = new Hermite (1280, 2500);
    mu4Hermite1764 = new Hermite (1764, 2500);
    mu4Hermite1920 = new Hermite (1920, 2500);
    //ノイズ抑制
    for (int i = 0; i <= MU4_ATTACK_FRAMES; i++) {  //MU4_ATTACK_FRAMESを含む
      mu4AttackArray[i] =
        (float) ((1.0 + Math.sin (Math.PI *
                                  (double) (i - MU4_ATTACK_FRAMES / 2) /
                                  (double) MU4_ATTACK_FRAMES)) / 2.0);
    }
    if (MU4_DEBUG) {
      for (int i = 0; i <= MU4_ATTACK_FRAMES; i++) {
        System.out.printf ("mu4AttackArray[%d]=%.6f\n", i, mu4AttackArray[i]);
      }
    }
  }  //mu4Init

  //mu4Tini ()
  //  後始末
  public static void mu4Tini () {
    //パラメータ
    Settings.sgsPutOnOff ("mercury", mu4OnRequest);
    Settings.sgsPutOnOff ("mercuryoe", mu4OutputEnable);
  }  //mu4Tini

  //mu4Reset ()
  //  リセット
  public static void mu4Reset () {
    mu4On = mu4OnRequest;
    mu4OnPositive = mu4On && !XEiJ.specifiedModel.isShodai ();
    mu4OnNegative = mu4On && XEiJ.specifiedModel.isShodai ();
    //  command=0xfc20
    //  status=0xfc2e
    mu4PortUpper = (MU4_U_RESERVED |
                    MU4_U12_TERR |
                    //MU4_U11_EXPCL |
                    //MU4_U10_EXREQ |
                    MU4_U6_INSEL);
    mu4PortCommand = 0;
    mu4PortStatus = (MU4_S3_N32K |
                     MU4_S2_N44K |
                     MU4_S1_N48K);
    //mu4PortUpper &= ~(MU4_U11_EXPCL | MU4_U10_EXREQ);
    mu4EnablePclReq = true;
    mu4RiseExpcl ();
    mu4RiseExreq ();
    mu4EnablePclReq = false;
    mu4SequenceArray = null;
    Arrays.fill (mu4Buffer, 0);  //出力中にリセットされたとき必要
    TickerQueue.tkqRemove (mu4SequenceTicker);
    mu4PortCommand = -1;  //必ず変化したことにする
    mu4SetCommand (MU4_U_RESERVED |
                   MU4_U12_TERR |
                   MU4_U11_EXPCL |
                   MU4_U10_EXREQ |
                   MU4_U6_INSEL);
  }  //mu4Reset

  //d = mu4ReadByte (a)
  //  バイト読み出し
  public static int mu4ReadByte (int a) {
    int a70 = a & 0x70;
    int d = 0;
    if (a70 == 0x10) {  //0x90-0x9f command
      d = (a & 1) == 0 ? mu4PortUpper >> 8 : (mu4PortUpper | mu4PortCommand) & 0xff;
    } else if (a70 == 0x20) {  //0xa0-0xaf status
      d = (a & 1) == 0 ? mu4PortUpper >> 8 : (mu4PortUpper | mu4PortStatus) & 0xff;
    }
    if (MU4_DEBUG) {
      System.out.printf ("%08x mu4ReadByte(0x%08x)=0x%02x\n", XEiJ.regPC0, a, d);
    }
    return d;
  }  //mu4ReadByte

  //d = mu4ReadWord (a)
  //  ワード読み出し
  public static int mu4ReadWord (int a) {
    int a70 = a & 0x70;
    int d = 0;
    if (a70 == 0x10) {  //0x90-0x9f command
      d = mu4PortUpper | mu4PortCommand;
    } else if (a70 == 0x20) {  //0xa0-0xaf status
      d = mu4PortUpper | mu4PortStatus;
    }
    if (MU4_DEBUG) {
      System.out.printf ("%08x mu4ReadWord(0x%08x)=0x%04x\n", XEiJ.regPC0, a, d);
    }
    return d;
  }  //mu4ReadWord

  //mu4WriteByte (a, d)
  //  バイト書き込み
  public static void mu4WriteByte (int a, int d) {
    d &= 0xff;
    if (MU4_DEBUG) {
      System.out.printf ("%08x mu4WriteByte(0x%08x,0x%02x)\n", XEiJ.regPC0, a, d);
    }
    int a70 = a & 0x70;
    if (a70 == 0x10) {  //0x90-0x9f command
      int t = mu4PortUpper | mu4PortCommand;
      if ((a & 1) == 0) {
        mu4SetCommand ((d << 8) | (t & 0xff));
      } else {
        mu4SetCommand ((t & 0xff00) | d);
      }
    }
  }  //mu4WriteByte

  //mu4WriteWord (a, d)
  //  ワード書き込み
  public static void mu4WriteWord (int a, int d) {
    d &= 0xffff;
    if (MU4_DEBUG) {
      System.out.printf ("%08x mu4WriteWord(0x%08x,0x%04x)\n", XEiJ.regPC0, a, d);
    }
    int a70 = a & 0x70;
    if (a70 == 0x00 &&  //0x80-0x8f data
        mu4Mode == MU4_MODE_OUT &&
        mu4OutputEnable &&  //出力許可
        0 <= mu4WritePointer) {  //リクエストあり
      if ((mu4DataChannels == 1 ||  //mono
           (mu4WritePointer & 1) == 0) &&  //left
          (mu4PortCommand & MU4_C2_LG) != 0) {  //L-on
        mu4NaiKamoLeft = false;
        if (mu4NaiDesuLeft) {
          mu4NaiDesuLeft = false;  //ない→ある
          mu4AttackIndexLeft = 1;  //アタック開始
        }
        mu4DataArray[mu4WritePointer] = Math.round (mu4AttackArray[mu4AttackIndexLeft] * (short) d);
        mu4AttackIndexLeft = Math.min (MU4_ATTACK_FRAMES, mu4AttackIndexLeft + 1);
      }
      if ((mu4DataChannels == 1 ||  //mono
           (mu4WritePointer & 1) != 0) &&  //right
          (mu4PortCommand & MU4_C3_RG) != 0) {  //R-on
        mu4NaiKamoRight = false;
        if (mu4NaiDesuRight) {
          mu4NaiDesuRight = false;  //ない→ある
          mu4AttackIndexRight = 1;  //アタック開始
        }
        mu4DataArray[mu4WritePointer + (2 - mu4DataChannels)] = Math.round (mu4AttackArray[mu4AttackIndexRight] * (short) d);
        mu4AttackIndexRight = Math.min (MU4_ATTACK_FRAMES, mu4AttackIndexRight + 1);
      }
      if (false) {  //1回のEXREQに対して複数回wirteされたときfalse=最後,true=最初を有効にする
        mu4WritePointer = -1;
      }
      mu4DataWritten = true;
    } else if (a70 == 0x10) {  //0x90-0x9f command
      mu4SetCommand (d);
    }
  }  //mu4WriteWord

  //mu4HsyncStart (hsyncTime)
  //  水平同期パルス開始
  //    M256のときシーケンスを開始する
  //  CRTCが呼び出す
  //    垂直同期パルス期間は除いてある
  public static void mu4HsyncStart (long hsyncTime) {
    if (mu4Mode != MU4_MODE_M256) {
      return;
    }
    mu4SequenceArray = (mu4DataChannels == 1 ?  //mono
                        mu4DataRate < 32000 ? MU4_M256_LOW_MONO : MU4_M256_HIGH_MONO
                        :  //stereo
                        mu4DataRate < 32000 ? MU4_M256_LOW_STEREO : MU4_M256_HIGH_STEREO);
    if (mu4SequenceArray == MU4_M256_LOW_STEREO &&
        (mu4PortUpper & MU4_U54_CLKSEL) == MU4_U54_32K) {
      mu4SequenceArray = MU4_M256_LOW_MONO;  //不自然。シミュレータのバグ?
    }
    //水平同期パルス開始時刻
    //  XEiJ.mpuClockTimeでは水平同期パルスが遅延していたときシーケンスが欠落して画面が乱れる
    //  水平同期パルスと一緒に遅延させることで欠落を避ける
    mu4BlockTime = hsyncTime;
    //フレーム番号
    mu4FrameNumber = 0;
    //シーケンス内インデックス
    mu4SequenceIndex = 0;
    //シーケンスティッカーを予約する
    TickerQueue.tkqAdd (
      mu4SequenceTicker,  //シーケンスティッカー
      mu4SequenceTime = mu4BlockTime +  //水平同期パルス開始時刻
      mu4SequenceArray[mu4SequenceIndex++] *  //シーケンス内サイクル数
      XEiJ.TMR_FREQ / mu4DataFrequency);  //サイクル数を時刻に変換する。順序に注意
  }  //mu4HsyncStart

  //mu4ExackStart ()
  //  EXACK開始
  //    ACKIGでないときEXREQを出していたら引っ込める
  //  HD63450が呼び出す
  public static void mu4ExackStart () {
    if ((mu4PortUpper & MU4_U8_ACKIG) != 0) {  //ACKIGでない
      mu4RiseExreq ();
    }
  }  //mu4ExackStart



  //周波数変換後のデータの配列
  public static final int[] mu4Buffer = new int[2 * 2500];

  //mu4FillBuffer ()
  //  周波数変換後のデータを作る
  public static void mu4FillBuffer () {
    if (mu4Mode != MU4_MODE_OUT) {  //OUTでない
      return;
    }
    if (MU4_DEBUG) {
      System.out.printf ("%08x mu4FillBuffer()\n", XEiJ.regPC0);
    }
    //フレーム番号を巻き戻す
    mu4FrameNumber = 0;
    //ブロック開始時刻を進める
    mu4BlockTime += MU4_BLOCK_TIME;
    //シーケンスティッカーを再設定する
    TickerQueue.tkqAdd (
      mu4SequenceTicker,  //シーケンスティッカー
      mu4SequenceTime = mu4BlockTime +  //ブロック開始時刻
      (mu4DataCycles * mu4FrameNumber + mu4SequenceArray[mu4SequenceIndex - 1]) *  //ブロック内サイクル数
      XEiJ.TMR_FREQ / mu4DataFrequency);  //サイクル数を時刻に変換する。順序に注意
    //書き込みがなければゼロを返して終わり
    if (!mu4DataWritten) {
      Arrays.fill (mu4Buffer, 0);
      return;
    }
    mu4DataWritten = false;
    //周波数変換を行う
    switch (mu4DataFrames) {
    case 640:
      mu4Hermite640.convert ();
      break;
    case 882:
      mu4Hermite882.convert ();
      break;
    case 960:
      mu4Hermite960.convert ();
      break;
    case 1280:
      mu4Hermite1280.convert ();
      break;
    case 1764:
      mu4Hermite1764.convert ();
      break;
    case 1920:
      mu4Hermite1920.convert ();
      break;
    }
    //溢れた位置に書き込まれた4フレーム分のデータを先頭に移す
    for (int i = 0; i < 2 * 4; i++) {
      mu4DataArray[i] = mu4DataArray[2 * mu4DataFrames + i];
      mu4DataWritten = mu4DataWritten || mu4DataArray[i] != 0;
    }
    Arrays.fill (mu4DataArray, 2 * 4, 2 * (mu4DataFrames + 4), 0);
  }  //mu4FillBuffer



  //ポート
  //  command/status bit12-4
  static final int MU4_U_RESERVED = 7 << 13;
  static final int MU4_U12_TERR   = 1 << 12;  //0=TERR
  static final int MU4_U11_EXPCL  = 1 << 11;  //0=EXPCL
  static final int MU4_U10_EXREQ  = 1 << 10;  //0=EXREQ
  static final int MU4_U9_M256    = 1 << 9;  //0=M256
  static final int MU4_U8_ACKIG   = 1 << 8;  //0=ACKIG
  static final int MU4_U7_CLKRATE = 1 << 7;  //0=16k/22k/24k,1=32k/44k/48k
  static final int MU4_U6_INSEL   = 1 << 6;  //0=optical,1=coaxial
  static final int MU4_U54_CLKSEL = 3 << 4;  //01=16k/32k,00/10=22k/44k,11=24k/48k
  static final int MU4_U54_32K    = 1 << 4;
  static final int MU4_U54_48K    = 3 << 4;
  //  command bit3-0
  static final int MU4_C3_RG      = 1 << 3;  //0=R-off,1=R-on
  static final int MU4_C2_LG      = 1 << 2;  //0=L-off,1=L-on
  static final int MU4_C1_STEREO  = 1 << 1;  //0=mono,1=stereo
  static final int MU4_C0_OUT     = 1 << 0;  //0=in,1=out
  //  status bit3-0
  static final int MU4_S3_N32K    = 1 << 3;  //0=32k
  static final int MU4_S2_N44K    = 1 << 2;  //0=44k
  static final int MU4_S1_N48K    = 1 << 1;  //0=48k
  static final int MU4_S0_ERR     = 1 << 0;  //1=error
  //
  static int mu4PortUpper;  //command/status bit12-bit4
  static int mu4PortCommand;  //command bit3-0
  static int mu4PortStatus;  //status bit3-0

  //モード
  static final int MU4_MODE_M256 = 0;  //M256
  static final int MU4_MODE_OUT  = 1;  //OUT
  static final int MU4_MODE_IN   = 2;  //IN。未対応
  static int mu4Mode;

  //EXPCLとEXREQの出力許可
  static boolean mu4EnablePclReq;  //false=禁止,true=許可

  //データオシレータ周波数。1秒あたりのデータオシレータサイクル数
  static int mu4DataFrequency;  //12288000=16k/32k,16934400=22k/44k,18432000=24k/48k

  //データフレームサイクル数。1データフレームあたりのデータオシレータサイクル数
  static int mu4DataCycles;  //384=high,768=low

  //データサンプリング周波数。1秒あたりのデータフレーム数
  //  データオシレータ周波数/データフレームサイクル数
  static int mu4DataRate;  //16000,22050,24000,32000,44100,48000

  //データチャンネル数。1データフレームあたりの要素数
  static int mu4DataChannels;  //1=mono,2=stereo

  //ブロック周波数。1秒あたりのブロック数
  //  SoundSource.SND_BLOCK_FREQと同じ
  static final int MU4_BLOCK_RATE = 25;

  //1ブロックあたりのデータフレーム数
  //  データサンプリング周波数/ブロック周波数
  static int mu4DataFrames;

  //ラインサンプリング周波数。1秒あたりのラインフレーム数
  //  OPM.OPM_SAMPLE_FREQと同じ
  static final int MU4_LINE_RATE = 62500;

  //ラインチャンネル数。1ラインフレームあたりの要素数
  //  SoundSource.SND_CHANNELSと同じ
  static final int MU4_LINE_CHANNELS = 2;

  //1ブロックあたりのラインフレーム数
  //  ラインサンプリング周波数/ブロック周波数
  static final int MU4_LINE_FRAMES = MU4_LINE_RATE / MU4_BLOCK_RATE;

  //1ブロックあたりのライン要素数
  //  ラインチャンネル数*ラインフレーム数
  static final int MU4_LINE_ELEMENTS = MU4_LINE_CHANNELS * MU4_LINE_FRAMES;

  //シーケンス
  //  フレームまたは水平同期パルスの先頭を起点として
  //  EXPCLとEXREQをアサートまたはネゲートする時刻を
  //  データオシレータのサイクル数で記述したもの
  //  OUTのときはデータフレームサイクル数を周期として繰り返す
  static final int MU4_FALL_EXPCL = 0;  //EXPCLをアサートする
  static final int MU4_RISE_EXPCL = 1;  //EXPCLをネゲートする
  static final int MU4_FALL_EXREQ = 2;  //EXREQをアサートする
  static final int MU4_RISE_EXREQ = 3;  //EXREQをネゲートする
  //0xfd11 M256 16k mono
  //0xfd13 M256 16k stereo  不自然。シミュレータのバグ?
  //0xfd21 M256 22k mono
  //0xfd31 M256 24k mono
  static final int[] MU4_M256_LOW_MONO = {
    2, MU4_FALL_EXPCL,
    20, MU4_FALL_EXREQ,
    283, MU4_RISE_EXREQ,
    385, MU4_RISE_EXPCL,
  };
  //0xfd23 M256 22k stereo
  //0xfd33 M256 24k stereo
  static final int[] MU4_M256_LOW_STEREO = {
    2, MU4_FALL_EXPCL,
    283, MU4_RISE_EXREQ,
    385, MU4_RISE_EXPCL,
    404, MU4_FALL_EXREQ,
  };
  //0xfd91 M256 32k mono
  //0xfda1 M256 44k mono
  //0xfdb1 M256 48k mono
  static final int[] MU4_M256_HIGH_MONO = {
    2, MU4_FALL_EXPCL,
    10, MU4_FALL_EXREQ,
    142, MU4_RISE_EXREQ,
    193, MU4_RISE_EXPCL,
  };
  //0xfd93 M256 32k stereo
  //0xfda3 M256 44k stereo
  //0xfdb3 M256 48k stereo
  static final int[] MU4_M256_HIGH_STEREO = {
    2, MU4_FALL_EXPCL,
    10, MU4_FALL_EXREQ,
    142, MU4_RISE_EXREQ,
    193, MU4_RISE_EXPCL,
    202, MU4_FALL_EXREQ,
    334, MU4_RISE_EXREQ,
  };
  //0xfe5d OUT 16k mono
  //0xfe6d OUT 22k mono
  //0xfe7d OUT 24k mono
  static final int[] MU4_OUT_LOW_MONO = {
    0, MU4_FALL_EXPCL,
    379, MU4_FALL_EXREQ,
    384, MU4_RISE_EXPCL,
    667, MU4_RISE_EXREQ,
  };
  //0xfe5f OUT 16k stereo
  //0xfe6f OUT 22k stereo
  //0xfe7f OUT 24k stereo
  static final int[] MU4_OUT_LOW_STEREO = {
    0, MU4_FALL_EXPCL,
    283, MU4_RISE_EXREQ,
    379, MU4_FALL_EXREQ,
    384, MU4_RISE_EXPCL,
    667, MU4_RISE_EXREQ,
    763, MU4_FALL_EXREQ,
  };
  //0xfedd OUT 32k mono
  //0xfeed OUT 44k mono
  //0xfefd OUT 48k mono
  static final int[] MU4_OUT_HIGH_MONO = {
    0, MU4_FALL_EXPCL,
    190, MU4_FALL_EXREQ,
    192, MU4_RISE_EXPCL,
    334, MU4_RISE_EXREQ,
  };
  //0xfedf OUT 32k stereo
  //0xfeef OUT 44k stereo
  //0xfeff OUT 48k stereo
  static final int[] MU4_OUT_HIGH_STEREO = {
    0, MU4_FALL_EXPCL,
    142, MU4_RISE_EXREQ,
    190, MU4_FALL_EXREQ,
    192, MU4_RISE_EXPCL,
    334, MU4_RISE_EXREQ,
    382, MU4_FALL_EXREQ,
  };

  //シーケンスの配列
  static int[] mu4SequenceArray;

  //1ブロックの時間
  //  SoundSource.SND_BLOCK_TIMEと同じ
  static final long MU4_BLOCK_TIME = XEiJ.TMR_FREQ / MU4_BLOCK_RATE;

  //ブロック開始時刻
  //  SoundSource.sndBlockClock-MU4_BLOCK_TIME
  //  M256のときは水平同期パルス開始時刻
  static long mu4BlockTime;

  //フレーム番号
  //  現在のシーケンスのブロック内フレーム番号
  //  M256のときは0のみ
  static int mu4FrameNumber;

  //シーケンス内インデックス
  static int mu4SequenceIndex;

  //LRカウンタ
  //  シーケンス側がmu4WritePointerを決めるときに使う
  //  それ以外はmu4DataChannelsとmu4WritePointerの奇遇で判断すること
  static int mu4LRCounter;

  //データ書き込み位置
  //  常にステレオ
  //  -1=リクエストなし
  static int mu4WritePointer;

  //周波数変換前のデータの配列
  //  常にステレオ
  //  モノラルのデータをフレーム単位でパンできるようにするため
  static final int[] mu4DataArray = new int[2 * (1920 + 4)];

  //データ書き込みフラグ
  static boolean mu4DataWritten;

  //シーケンスティッカーの実行時刻
  static long mu4SequenceTime;



  static void mu4DumpVars () {
    System.out.printf ("mu4Mode=%d\n", mu4Mode);
    System.out.printf ("mu4EnablePclReq=%b\n", mu4EnablePclReq);
    System.out.printf ("mu4DataFrequency=%d\n", mu4DataFrequency);
    System.out.printf ("mu4DataCycles=%d\n", mu4DataCycles);
    System.out.printf ("mu4DataRate=%d\n", mu4DataRate);
    System.out.printf ("mu4DataChannels=%d\n", mu4DataChannels);
    System.out.printf ("mu4DataFrames=%d\n", mu4DataFrames);
    System.out.printf ("mu4BlockTime=%d\n", mu4BlockTime);
    System.out.printf ("mu4FrameNumber=%d\n", mu4FrameNumber);
    System.out.printf ("mu4SequenceIndex=%d\n", mu4SequenceIndex);
    System.out.printf ("mu4LRCounter=%d\n", mu4LRCounter);
    System.out.printf ("mu4WritePointer=%d\n", mu4WritePointer);
    System.out.printf ("mu4DataWritten=%b\n", mu4DataWritten);
  }  //mu4DumpVars

  //mu4SetCommand (d)
  //  コマンド書き込み
  static void mu4SetCommand (int d) {
    if (MU4_DEBUG) {
      System.out.printf ("%08x mu4SetCommand(0x%04x)\n", XEiJ.regPC0, d);
    }
    int prevMode = mu4Mode;
    int prevDataRate = mu4DataRate;
    int prevDataChannels = mu4DataChannels;
    int prevUpper = mu4PortUpper;
    int prevCommand = mu4PortCommand;
    mu4PortUpper = ((mu4PortUpper & (MU4_U_RESERVED |
                                     MU4_U12_TERR |
                                     MU4_U11_EXPCL |
                                     MU4_U10_EXREQ)) |
                    (d & (MU4_U9_M256 |
                          MU4_U8_ACKIG |
                          MU4_U7_CLKRATE |
                          MU4_U6_INSEL |
                          MU4_U54_CLKSEL)));
    mu4PortCommand = d & (MU4_C3_RG |
                          MU4_C2_LG |
                          MU4_C1_STEREO |
                          MU4_C0_OUT);
    //
    //EXPCLとEXREQの出力許可
    mu4EnablePclReq = (mu4PortUpper & (MU4_U9_M256 | MU4_U8_ACKIG)) != 0;
    //下位バイトが変化していないときは何もしない
    if ((((mu4PortUpper | mu4PortCommand) ^ (prevUpper | prevCommand)) & 0xff) == 0) {
      return;
    }
    //モード
    mu4Mode = ((mu4PortUpper & MU4_U9_M256) == 0 ? MU4_MODE_M256 :
               (mu4PortCommand & MU4_C0_OUT) != 0 ? MU4_MODE_OUT :
               MU4_MODE_IN);
    //データオシレータ周波数
    mu4DataFrequency = ((mu4PortUpper & MU4_U54_CLKSEL) == MU4_U54_32K ? 12288000 :
                        (mu4PortUpper & MU4_U54_CLKSEL) != MU4_U54_48K ? 16934400 :
                        18432000);
    //データフレームサイクル数
    mu4DataCycles = (mu4PortUpper & MU4_U7_CLKRATE) == 0 ? 768 : 384;
    //データサンプリング周波数
    mu4DataRate = mu4DataFrequency / mu4DataCycles;
    //データチャンネル数
    mu4DataChannels = (mu4PortCommand & MU4_C1_STEREO) == 0 ? 1 : 2;
    //
    if (mu4Mode != prevMode ||  //モードが変わったか
        mu4DataRate != prevDataRate ||  //サンプリング周波数が変わったか
        mu4DataChannels != prevDataChannels) {  //データチャンネル数が変わったとき
      if (mu4Mode == MU4_MODE_M256) {  //M256
        //ここではシーケンスを開始しない
        mu4SequenceArray = null;
      } else if (mu4Mode == MU4_MODE_OUT) {  //OUT
        //1ブロックあたりのデータフレーム数
        mu4DataFrames = mu4DataRate / MU4_BLOCK_RATE;
        //シーケンスの配列
        mu4SequenceArray = (mu4DataChannels == 1 ?  //mono
                            mu4DataRate < 32000 ? MU4_OUT_LOW_MONO : MU4_OUT_HIGH_MONO
                            :  //stereo
                            mu4DataRate < 32000 ? MU4_OUT_LOW_STEREO : MU4_OUT_HIGH_STEREO);
        //ブロック開始時刻
        mu4BlockTime = SoundSource.sndBlockClock - MU4_BLOCK_TIME;
        //フレーム番号
        //  max(0,min(フレーム数,ceil((現在時刻-ブロック開始時刻)*フレーム数/ブロック時間)))
        mu4FrameNumber =
          Math.max (0,
                    Math.min (mu4DataFrames,
                              (int) (((XEiJ.mpuClockTime - mu4BlockTime) * mu4DataFrames +
                                      (MU4_BLOCK_TIME - 1)) / MU4_BLOCK_TIME)));
        //シーケンス内インデックス
        mu4SequenceIndex = 0;
        //LRカウンタ
        mu4LRCounter = 0;
        //データ書き込み位置
        mu4WritePointer = -1;
        if (mu4DataRate != prevDataRate ||  //データサンプリング周波数が変わったか
            mu4DataChannels != prevDataChannels) {  //データチャンネル数が変わったとき
          //データの配列をゼロクリアする
          Arrays.fill (mu4DataArray, 0, 2 * (mu4DataFrames + 4), 0);
        }
        //データ書き込みフラグ
        mu4DataWritten = false;
        if (MU4_DEBUG) {
          mu4DumpVars ();
        }
        //ノイズ抑制
        mu4AttackIndexLeft = 1;
        mu4AttackIndexRight = 1;
        mu4NaiKamoLeft = true;
        mu4NaiKamoRight = true;
        mu4NaiDesuLeft = true;
        mu4NaiDesuRight = true;
        //シーケンスティッカーを予約する
        TickerQueue.tkqAdd (
          mu4SequenceTicker,  //シーケンスティッカー
          mu4SequenceTime = mu4BlockTime +  //ブロック開始時刻
          (mu4DataCycles * mu4FrameNumber + mu4SequenceArray[mu4SequenceIndex++]) *  //ブロック内サイクル数
          XEiJ.TMR_FREQ / mu4DataFrequency);  //サイクル数を時刻に変換する。順序に注意
      } else {  //IN
        //!!! 未対応
        mu4SequenceArray = null;
      }
    }
  }  //mu4SetCommand

  //mu4SequenceTicker
  //  シーケンスティッカー
  static final TickerQueue.Ticker mu4SequenceTicker = new TickerQueue.Ticker () {
    @Override protected void tick () {
      if (mu4SequenceArray == null) {
        return;
      }
      switch (mu4SequenceArray[mu4SequenceIndex++]) {  //コマンド
      case MU4_FALL_EXPCL:
        mu4FallExpcl ();
        break;
      case MU4_RISE_EXPCL:
        mu4RiseExpcl ();
        break;
      case MU4_FALL_EXREQ:
        if (mu4Mode == MU4_MODE_OUT) {
          //データ書き込み位置
          mu4WritePointer = 2 * (mu4FrameNumber + 3) + mu4LRCounter;
          //LRカウンタ
          mu4LRCounter ^= 1;
          //ノイズ抑制
          mu4DataArray[mu4WritePointer] = (int) (mu4DataArray[mu4WritePointer - 2] * MU4_DECAY_RATIO);  //端数は切り捨てる。roundだと1→1で0にならない
          if (mu4DataChannels == 1 ||  //mono
              (mu4WritePointer & 1) == 0) {  //left
            mu4NaiDesuLeft = mu4NaiDesuLeft || mu4NaiKamoLeft;
            mu4NaiKamoLeft = true;
          }
          if (mu4DataChannels == 1 ||  //mono
              (mu4WritePointer & 1) != 0) {  //right
            mu4NaiDesuRight = mu4NaiDesuRight || mu4NaiKamoRight;
            mu4NaiKamoRight = true;
          }
        }
        mu4FallExreq (mu4SequenceTime);
        break;
      case MU4_RISE_EXREQ:
        mu4RiseExreq ();
        break;
      }
      if (mu4SequenceArray.length <= mu4SequenceIndex) {  //シーケンス終了
        if (mu4Mode == MU4_MODE_M256) {  //M256のとき
          mu4SequenceArray = null;  //ここでは次のシーケンスを開始しない
          return;
        }
        //OUT
        mu4FrameNumber = Math.min (mu4DataFrames, mu4FrameNumber + 1);
        mu4SequenceIndex = 0;
        mu4LRCounter = 0;
      }
      //シーケンスティッカーを予約する
      TickerQueue.tkqAdd (
        mu4SequenceTicker,  //シーケンスティッカー
        mu4SequenceTime = mu4BlockTime +  //ブロック開始時刻
        (mu4DataCycles * mu4FrameNumber + mu4SequenceArray[mu4SequenceIndex++]) *  //ブロック内サイクル数
        XEiJ.TMR_FREQ / mu4DataFrequency);  //サイクル数を時刻に変換する。順序に注意
    }  //tick
  };  //mu4Ticker

  //mu4FallExpcl ()
  //  EXPCLをアサートする
  static void mu4FallExpcl () {
    if ((mu4PortUpper & MU4_U11_EXPCL) != 0) {
      mu4PortUpper &= ~MU4_U11_EXPCL;
      if (mu4EnablePclReq) {
        HD63450.dmaFallPCL (2);
      }
    }
  }  //mu4FallExpcl

  //mu4RiseExpcl ()
  //  EXPCLをネゲートする
  static void mu4RiseExpcl () {
    if ((mu4PortUpper & MU4_U11_EXPCL) == 0) {
      mu4PortUpper |= MU4_U11_EXPCL;
      if (mu4EnablePclReq) {
        HD63450.dmaRisePCL (2);
      }
    }
  }  //mu4RiseExpcl

  //mu4FallExreq (time)
  //  EXREQをアサートする
  static void mu4FallExreq (long time) {
    if ((mu4PortUpper & MU4_U10_EXREQ) != 0) {
      mu4PortUpper &= ~MU4_U10_EXREQ;
      if (mu4EnablePclReq) {
        HD63450.dmaFallREQ (2, time);
      }
    }
  }  //mu4FallExreq

  //mu4RiseExreq ()
  //  EXREQをネゲートする
  static void mu4RiseExreq () {
    if ((mu4PortUpper & MU4_U10_EXREQ) == 0) {
      mu4PortUpper |= MU4_U10_EXREQ;
      if (mu4EnablePclReq) {
        HD63450.dmaRiseREQ (2);
      }
    }
  }  //mu4RiseExreq



  //エルミート補間
  static Hermite mu4Hermite640;
  static Hermite mu4Hermite882;
  static Hermite mu4Hermite960;
  static Hermite mu4Hermite1280;
  static Hermite mu4Hermite1764;
  static Hermite mu4Hermite1920;

  //class Hermite
  //  エルミート補間
  static class Hermite {
    int inputFrames, outputFrames;
    int inputFactor, outputFactor;
    float[] coeffArray;
    int[] indexArray;
    Hermite (int inputFrames, int outputFrames) {
      this.inputFrames = inputFrames;
      this.outputFrames = outputFrames;
      int g = gcd (inputFrames, outputFrames);
      inputFactor = inputFrames / g;
      outputFactor = outputFrames / g;
      coeffArray = new float[4 * outputFactor];
      indexArray = new int[outputFactor];
      int inputIndex1 = 0;
      int ratio1 = 0;
      for (int outputIndex = 0; outputIndex < outputFactor; outputIndex++) {
        int inputIndex0 = inputIndex1;
        int ratio0 = ratio1;
        ratio1 += inputFactor;
        while (outputFactor <= ratio1) {
          ratio1 -= outputFactor;
          inputIndex1++;
        }
        //
        //  エルミート補間の係数を求める
        //
        //  f(0)=p0
        //  f(1)=p1
        //  f'(0)=(p1-pm)/2
        //  f'(1)=(p2-p0)/2
        //  を満たす3次関数を作る
        //
        //  f(x)=a*x^3+b*x^2+c*x+d
        //  f'(x)=3*a*x^2+2*b*x+c
        //
        //  f:=sub(first(solve({sub(x=0,a*x^3+b*x^2+c*x+d)=p0,
        //                      sub(x=1,a*x^3+b*x^2+c*x+d)=p1,
        //                      sub(x=0,3*a*x^2+2*b*x+c)=(p1-pm)/2,
        //                      sub(x=1,3*a*x^2+2*b*x+c)=(p2-p0)/2},
        //                     {a,b,c,d})),
        //         a*x^3+b*x^2+c*x+d);
        //  cm:=part(coeff(f,pm),2);
        //  c0:=part(coeff(f,p0),2);
        //  c1:=part(coeff(f,p1),2);
        //  c2:=part(coeff(f,p2),2);
        //  f-(cm*pm+c0*p0+c1*p1+c2*p2);
        //
        //  f(x)=cm*pm+c0*p0+c1*p1+c2*p2
        //  cm=(-x^3+2*x^2-x)/2
        //  c0=(3*x^3-5*x^2+2)/2
        //  c1=(-3*x^3+4*x^2+x)/2
        //  c2=(x^3-x^2)/2
        //
        //  周波数変換ではxは高々4桁通りなのでx毎に{cm,c0,c1,c2}をあらかじめ計算しておける
        //  乗算は4回で済むので(見た目よりは)高速に補間できる
        //
        double x = (double) ratio0 / (double) outputFactor;
        coeffArray[4 * outputIndex] = (float) (((-0.5 * x + 1.0) * x - 0.5) * x);  //cm
        coeffArray[4 * outputIndex + 1] = (float) ((1.5 * x - 2.5) * x * x + 1.0);  //c0
        coeffArray[4 * outputIndex + 2] = (float) (((-1.5 * x + 2.0) * x + 0.5) * x);  //c1
        coeffArray[4 * outputIndex + 3] = (float) ((0.5 * x - 0.5) * x * x);  //c2
        indexArray[outputIndex] = inputIndex0;
      }
    }
    void convert () {
      for (int inputIndex = 0, outputIndex = 0;
           inputIndex < inputFrames;
           inputIndex += inputFactor, outputIndex += outputFactor) {
        for (int od = 0; od < outputFactor; od++) {
          int ii = inputIndex + indexArray[od];
          int oi = outputIndex + od;
          float cm = coeffArray[4 * od];
          float c0 = coeffArray[4 * od + 1];
          float c1 = coeffArray[4 * od + 2];
          float c2 = coeffArray[4 * od + 3];
          mu4Buffer[2 * oi + 0] = Math.round (cm * mu4DataArray[2 * ii + 0] +
                                              c0 * mu4DataArray[2 * ii + 2] +
                                              c1 * mu4DataArray[2 * ii + 4] +
                                              c2 * mu4DataArray[2 * ii + 6]);
          mu4Buffer[2 * oi + 1] = Math.round (cm * mu4DataArray[2 * ii + 1] +
                                              c0 * mu4DataArray[2 * ii + 3] +
                                              c1 * mu4DataArray[2 * ii + 5] +
                                              c2 * mu4DataArray[2 * ii + 7]);
        }
      }
    }  //convert
    int gcd (int x, int y) {
      while (y != 0) {
        int t = x % y;
        x = y;
        y = t;
      }
      return x;
    }  //gcd
  }  //class Hermite



  //ノイズ抑制
  //  アタックとディケイに5ms～10msの時間をかけることでノイズを抑制する
  //  アタックは三角関数、ディケイは指数関数を使う
  //  通常のデータはノイズを抑制する加工がされているはず
  //  ディスクアクセス中にデータが欠落したときなどに効果がある
  static final float MU4_DECAY_RATIO = 0.97F;  //ディケイ率
  static final int MU4_ATTACK_FRAMES = 300;  //アタックにかけるフレーム数
  static final float mu4AttackArray[] = new float[MU4_ATTACK_FRAMES + 1];  //アタックの係数の配列。(1+sin(-pi/2..pi/2))/2
  static int mu4AttackIndexLeft;  //mu4AttackArrayのインデックス
  static int mu4AttackIndexRight;
  static boolean mu4NaiKamoLeft;  //データないかも
  static boolean mu4NaiKamoRight;
  static boolean mu4NaiDesuLeft;  //データないです
  static boolean mu4NaiDesuRight;



}  //class MercuryUnit
