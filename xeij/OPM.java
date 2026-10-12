//========================================================================================
//  OPM.java
//    en:Frequency modulation sound source
//    ja:FM音源
//  Copyright (C) 2003-2026 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.util.*;

public class OPM {

  public static final boolean OPM_ON = true;  //true=OPMを出力する

  public static final int OPM_OSC_FREQ = 4000000;  //クロック周波数(Hz)。4000000Hz
  public static final int OPM_SAMPLE_FREQ = OPM_OSC_FREQ / 64;  //サンプリング周波数(Hz)。62500Hz
  public static final long OPM_SAMPLE_TIME = XEiJ.TMR_FREQ / OPM_SAMPLE_FREQ;  //1サンプルの時間(XEiJ.TMR_FREQ単位)。16000000ps
  public static final int OPM_BLOCK_SAMPLES = OPM_SAMPLE_FREQ / SoundSource.SND_BLOCK_FREQ;  //1ブロックのサンプル数。2500

  public static int opmOutputMask;  //-1=OPMを出力する

  //YM2151
  public static YM2151 opmYM2151;
  public static TickerQueue.Ticker opmTimerATicker;
  public static TickerQueue.Ticker opmTimerBTicker;
  public static int[] opmRegister;  //[address]=data,[256+ch]=slotMask,[264]=AMD,[265]=PMD
  public static int[] opmBuffer;
  public static int opmPointer;
  public static long opmBusyClock;

  //opmInit ()
  //  初期化
  public static void opmInit () {
    //opmOutputMask = -1;  //OPMを出力する
    opmYM2151 = new YM2151 ();
    opmTimerATicker = new TickerQueue.Ticker () {
      @Override protected void tick () {
        opmYM2151.timerAExpired ();
      }
    };
    opmTimerBTicker = new TickerQueue.Ticker () {
      @Override protected void tick () {
        opmYM2151.timerBExpired ();
      }
    };
    opmRegister = new int[266];
    opmYM2151.setListener (new YM2151.Listener () {
      @Override public void timerA (int clocks) {
        if (clocks != -1) {  //始動
          TickerQueue.tkqAdd (opmTimerATicker, XEiJ.mpuClockTime + (XEiJ.TMR_FREQ / (long) OPM_OSC_FREQ) * (long) clocks);
        } else {  //停止
          TickerQueue.tkqRemove (opmTimerATicker);
        }
      }
      @Override public void timerB (int clocks) {
        if (clocks != -1) {  //始動
          TickerQueue.tkqAdd (opmTimerBTicker, XEiJ.mpuClockTime + (XEiJ.TMR_FREQ / (long) OPM_OSC_FREQ) * (long) clocks);
        } else {  //停止
          TickerQueue.tkqRemove (opmTimerBTicker);
        }
      }
      @Override public void busy (int clocks) {
        opmBusyClock = XEiJ.mpuClockTime + (XEiJ.TMR_FREQ / (long) OPM_OSC_FREQ) * (long) clocks;
      }
      @Override public boolean isBusy () {
        return XEiJ.mpuClockTime < opmBusyClock;
      }
      @Override public void irq (boolean asserted) {
        if (asserted) {
          MC68901.mfpOpmirqFall ();
        } else {
          MC68901.mfpOpmirqRise ();
        }
      }
      @Override public void control (int data) {
        FDC.fdcSetEnforcedReady ((data & 1) != 0);  //CT2 強制レディ状態の設定。0=通常動作,1=強制レディ状態
        ADPCM.pcmOscillator = (data >> 1) & 1;  //CT1 ADPCMの原発振周波数の設定。0=8MHz/8MHz,1=4MHz/16MHz
        ADPCM.pcmUpdateRepeatInterval ();
      }
      @Override public void written (int pointer, int address, int data) {
        if (address == 0x08) {  //KON
          opmRegister[256 + (data & 7)] = (data >> 3) & 15;
        } else if (address == 0x19) {  //AMD/PMD
          opmRegister[264 + (data >> 7)] = data & 127;
        } else {
          opmRegister[address] = data;
        }
        if (OPMLog.OLG_ON) {
          OPMLog.olgSetData (address, data);
        }
      }
    });
    opmYM2151.allocate (SoundSource.SND_CHANNELS * (OPM_BLOCK_SAMPLES + 1));
    opmReset ();
  }

  //opmReset ()
  //  リセット
  public static void opmReset () {
    opmYM2151.reset ();
    opmYM2151.clear ();
    opmBuffer = opmYM2151.getBuffer ();
    opmPointer = opmYM2151.getPointer ();
    opmBusyClock = 0L;
  }

  //opmSetOutputOn (on)
  //  OPM出力のON/OFF
  public static void opmSetOutputOn (boolean on) {
    opmOutputMask = on ? -1 : 0;
    opmYM2151.setChannelMask (opmOutputMask & 0xff);
  }



  public static int opmReadByte (int a) {
    return (a & 3) == 3 ? opmYM2151.readStatus () : 0xff;
  }  //opmReadByte

  public static void opmWriteByte (int a, int d) {
    a &= 3;
    d &= 0xff;
    if (a == 3) {  //データレジスタ
      opmYM2151.generate (
        SoundSource.SND_CHANNELS *
        (OPM_BLOCK_SAMPLES -
         Math.max (0,
                   (int) ((double) (SoundSource.sndBlockClock - XEiJ.mpuClockTime) /
                          (double) OPM_SAMPLE_TIME))));
      opmYM2151.writeData (d);
    } else if (a == 1) {  //アドレスレジスタ
      opmYM2151.writeAddress (d);
    }
  }  //opmWriteByte



}
