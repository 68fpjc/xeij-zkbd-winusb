//========================================================================================
//  SoundSource.java
//    en:Sound source -- It outputs mixed sound of the frequency modulation sound source and ADPCM sound source while converting the sampling frequency.
//    ja:音源 -- FM音源とADPCM音源を合成してサンプリング周波数を変換しながら出力します。
//  Copyright (C) 2003-2026 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//  内部のサンプリング周波数を62500Hzに固定し、周波数を変換しながら出力する
//  音を出さないまたは出せないときも、タイマやレジスタは音が出ているときと同じように機能しなければならない
//
//  参考
//    Java Sound API プログラマーズガイド
//      http://docs.oracle.com/javase/jp/1.3/guide/sound/prog_guide/title.fm.html
//! 非対応。PCMの入力は実装しない
//----------------------------------------------------------------------------------------

package xeij;

import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.nio.*;  //ByteBuffer,ByteOrder
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,Timer,TimerTask,TreeMap
import javax.sound.sampled.*;  //AudioFormat,AudioSystem,DataLine,LineUnavailableException,SourceDataLine

public class SoundSource {

  //ソースデータライン
  public static final boolean SND_ON = true;  //true=出力する
  public static final int SND_SAMPLE_FREQ = 48000;  //サンプリング周波数(Hz)。22050,44100,48000のいずれか
  public static final int SND_CHANNELS = 2;  //チャンネル数。1=モノラル,2=ステレオ
  public static final int SND_SAMPLE_SHIFT = SND_CHANNELS == 1 ? 1 : 2;  //1サンプルのバイト数のシフトカウント
  public static final int SND_SAMPLE_BYTES = 1 << SND_SAMPLE_SHIFT;  //1サンプルのバイト数
  public static SourceDataLine sndLine;  //出力ライン。null=出力不可。SND_ONのときだけsndLine!=nullになる

  //動作モード
  public static boolean sndPlayOn;  //true=出力する。sndLine!=nullのときだけtrueになる

  //ブロック
  //  動作単位。1/25秒(0.04秒)ずつ出力する
  //  SND_BLOCK_FREQはXEiJ.TMR_FREQとSND_SAMPLE_FREQとOPM.OPM_SAMPLE_FREQとADPCM.PCM_SAMPLE_FREQをすべて割り切らなければならない
  public static final int SND_BLOCK_FREQ = 25;  //ブロック周波数(Hz)
  public static final long SND_BLOCK_TIME = XEiJ.TMR_FREQ / SND_BLOCK_FREQ;  //1ブロックの時間(XEiJ.TMR_FREQ単位)
  public static final int SND_BLOCK_SAMPLES = SND_SAMPLE_FREQ / SND_BLOCK_FREQ;  //1ブロックのサンプル数。882,1764,1920

  //周波数変換
  //  線形補間
  //    62500Hzのサンプリングデータを線形補間したものを22050Hzまたは44100Hzまたは48000Hzのサンプリング間隔でサンプリングし直す
  //
  //    入力周波数=13Hz,出力周波数=10Hzのとき
  //    aaaaaaaaaabbbbbbbbbbccccccccccddddddddddeeeeeeeeeeffffffffffgggggggggghhhhhhhhhhiiiiiiiiiijjjjjjjjjj 入力データ
  //         01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234 サンプリング位置の端数
  //         0            1            2            3            4            5            6            7    サンプリング位置
  //        a*10       b*7+c*3      c*4+d*6      d*1+e*9      f*8+g*2      g*5+h*5      h*2+i*8      j*9+k*1 出力データ
  //
  public static final boolean SND_FREQ_TABLE = true;  //true=規定の周波数で出力するとき周波数変換テーブルを使う
  public static final boolean SND_INTERPOLATION_ON = true;  //true=62500Hzから規定の周波数に変換するとき線形補間を行う
  public static final int SND_INTERPOLATION_BIT = 8;  //線形補間の分解能。前後のデータに掛ける比率の合計のlog2。入力データが符号付き16bitに収まっていれば15以下だが溢れているとき割れてしまうので小さめにしておく
  public static final int[] sndFreqConvIndex = new int[SND_BLOCK_SAMPLES];  //周波数変換のインデックス。0～OPM.OPM_BLOCK_SAMPLES-1(2500-1)
  public static final int[] sndFreqConvFraction = new int[SND_BLOCK_SAMPLES];  //周波数変換の端数。0～(1<<SND_INTERPOLATION_BIT)-1
  public static SNDRateConverter sndRateConverter;  //サンプリング周波数変換アルゴリズムの選択

  //バッファ
  //  バッファを大きくしすぎると音声の遅延(画面とのずれ)が大きくなる
  public static final int SND_BUFFER_SAMPLES = SND_BLOCK_SAMPLES * 3;  //バッファのサンプル数。ブロックの3倍
  public static final int SND_BUFFER_BYTES = SND_BUFFER_SAMPLES << SND_SAMPLE_SHIFT;
  public static final byte[] sndByteBlock = new byte[SND_BUFFER_BYTES];  //ブロックのbyte配列

  //タイマー
  public static long sndBlockClock;  //次のブロックを出力する時刻。再生中はSND_BLOCK_TIMEずつ増える。再生開始時刻はSND_BLOCK_TIMEの倍数とは限らない

  //エンディアン制御
  //  デフォルトはリトルエンディアン
  //  ByteBufferでエンディアンを制御するときはネイティブのエンディアンになる
  public static final boolean SND_BYTE_BUFFER_ENDIAN = false;  //true=ByteBufferでエンディアンを制御する
  public static ByteOrder sndNativeEndian;  //ネイティブのエンディアン
  public static short[] sndShortBlock;  //ブロックのshort配列
  public static ByteBuffer sndByteBuffer;  //sndByteBlockをラップしたByteBuffer
  public static ShortBuffer sndShortBuffer;  //sndByteBlockをラップしたShortBuffer

  //ボリュームコントロール
  //  スピーカに負荷をかけたくないので2倍を上限とする
  //  デフォルトは0.5倍
  //  ボリュームを変更するとき、瞬時に変えるとプチノイズが出るので段階的に変える
  public static final int SND_VOLUME_MAX = 40;  //8倍
  public static final int SND_VOLUME_DEFAULT = 20;  //0.5倍
  public static final int SND_VOLUME_STEP = 5;  //2倍になる差分
  public static int sndVolume;  //ボリュームの設定値。5=1/16倍,10=1/8倍,15=1/4倍,20=1/2倍,25=1倍,30=2倍,35=4倍,40=8倍
  public static int sndCurrentScale;  //ボリュームの現在のスケール。1倍は4096に固定。256=1/16倍,512=1/8倍,1024=1/4倍,2048=1/2倍,4096=1倍,8192=2倍,16384=4倍,32768=8倍
  public static int sndTargetScale;  //ボリュームの変更後のスケール

  //ゼロレベルシフト
  //  OPMとPCMの合成後のデータが範囲外のときゼロレベルをシフトしてなるべく音が割れないようにする
  //  PCMを12ビットでマスクする必要があることに変わりはない
  public static final boolean SND_ZERO_LEVEL_SHIFT = false;
  public static int sndAdaptiveShiftM;
  public static int sndAdaptiveShiftL;
  public static int sndAdaptiveShiftR;

  //ライン出力
  public static final TickerQueue.Ticker sndBlockTicker = new TickerQueue.Ticker () {
    @Override protected void tick () {
      //OPMのバッファを充填する
      OPM.opmYM2151.generate (SND_CHANNELS * OPM.OPM_BLOCK_SAMPLES);
      //PCMのバッファを充填する
      if (ADPCM.pcmPointer < SND_CHANNELS * ADPCM.PCM_BLOCK_SAMPLES) {
        ADPCM.pcmFillBuffer (SND_CHANNELS * ADPCM.PCM_BLOCK_SAMPLES);
      }
      //MU4のバッファを充填する
      if (MercuryUnit.MU4_ON && MercuryUnit.mu4On) {
        MercuryUnit.mu4FillBuffer ();
      }
      if ((sndTargetScale | sndCurrentScale) != 0) {  //フェードインとフェードアウトの両方が必要。0以上なのでまとめてテストする
        int outputSamples = SND_BLOCK_SAMPLES;
        //周波数変換
        (SND_FREQ_TABLE && outputSamples == SND_BLOCK_SAMPLES ? sndRateConverter : SNDRateConverter.ADAPTIVE).convert (outputSamples);
        //ラインに出力する
        //  sndLine.available()はバッファアンダーランが起きたとき0を返すことがある
        //  ラインがダミーのデータで充填されているのかも知れない
        //  その状態で出力しようとすると待たされるのでコアと音声出力の両方が止まってしまう
        //  音声出力が止まるのは避けられないがコアを止めたくないので、
        //  sndLine.available()が0を返したときは出力をキャンセルする
        if (sndLine.available () != 0) {
          try {
            long t = System.nanoTime ();
            sndLine.write (sndByteBlock, 0, outputSamples << SND_SAMPLE_SHIFT);
            XEiJ.mpuTotalNano -= System.nanoTime () - t;  //ラインへの出力にかかった時間をコアが消費した時間から除く
          } catch (Exception e) {
          }
        }
      }
      //音声モニタを更新する
      if (SoundMonitor.smnIsVisible) {
        SoundMonitor.smnUpdate ();
      }
      //OPMの出力ポインタを巻き戻す
      OPM.opmYM2151.clear ();
      //PCMの出力ポインタを巻き戻す
      //  はみ出している部分を先頭にコピーするので周波数変換の前に行えない
      ADPCM.pcmPointer = Math.max (0, ADPCM.pcmPointer - SND_CHANNELS * ADPCM.PCM_BLOCK_SAMPLES);
      for (int i = 0; i < ADPCM.pcmPointer; i++) {
        ADPCM.pcmBuffer[i] = ADPCM.pcmBuffer[i + SND_CHANNELS * ADPCM.PCM_BLOCK_SAMPLES];
      }
      //PCMの原発振周波数の切り替えを行う
      if (ADPCM.pcmOSCFreqMode != ADPCM.pcmOSCFreqRequest) {
        ADPCM.pcmOSCFreqMode = ADPCM.pcmOSCFreqRequest;
        ADPCM.pcmUpdateRepeatInterval ();
      }
      //Arrays.fill (ADPCM.pcmBuffer, ADPCM.pcmPointer, SND_CHANNELS * (ADPCM.PCM_BLOCK_SAMPLES + ADPCM.PCM_MAX_REPEAT * 2 - 1), 0);
      //次の割り込み時刻を設定する
      sndBlockClock += SND_BLOCK_TIME;
      TickerQueue.tkqAdd (sndBlockTicker, sndBlockClock);
    }
  };

  //PCM出力
  public static final TickerQueue.Ticker sndPcmTicker = new TickerQueue.Ticker () {
    @Override protected void tick () {
      if (ADPCM.pcmEncodedData >= 0) {  //有効なPCMデータがあるとき
        //PCMデータを引き取って変換テーブルの中を移動する
        //  補間したときに中間の値を取れるようにするためにデータを4bitシフトする
        int decoded = (ADPCM.PCM_MODIFY_ZERO_ZERO && (!ADPCM.PCM_MODIFY_ZERO_TWICE || ADPCM.pcmEncodedData == ADPCM.pcmZeroPreviousData) ?
                       ADPCM.pcmDecodedData1 >= 0 ? ADPCM.pcmDecoderTableP : ADPCM.pcmDecoderTableM :
                       ADPCM.pcmDecoderTable)[ADPCM.pcmDecoderPointer << 8 | ADPCM.pcmEncodedData];  //下位の差分<<19|(上位の差分&8191)<<6|次の予測指標
        int delta1 = decoded       >> -13 << 4;  //下位の差分(符号あり13bit)
        int delta2 = decoded << 13 >> -13 << 4;  //上位の差分(符号あり13bit)
        ADPCM.pcmDecoderPointer = decoded & 63;  //次の予測指標(符号なし6bit)
        if (ADPCM.PCM_MODIFY_ZERO_ZERO && ADPCM.PCM_MODIFY_ZERO_TWICE) {
          ADPCM.pcmZeroPreviousData = ADPCM.pcmEncodedData == 0x00 || ADPCM.pcmEncodedData == 0x88 ? ADPCM.pcmEncodedData : -1;
        }
        ADPCM.pcmEncodedData = -1;
        if (false) {  //下位4bitの飽和処理を行わない
          //  クリッピングの範囲が0x8000..0x7ff0ではなく0x8000..0x7fffになる
          int m = ADPCM.pcmDecodedData1 + delta1;
          ADPCM.pcmInterpolationEngine.write ((short) m == m ? m : (m = m >> -1 ^ 32767));
          m += delta2;
          ADPCM.pcmInterpolationEngine.write ((short) m == m ? m :      m >> -1 ^ 32767);
        } else {  //本来の範囲でクリッピングを行う
          int m;
          ADPCM.pcmInterpolationEngine.write (m = Math.max (-2048 << 4, Math.min (2047 << 4, ADPCM.pcmDecodedData1 + delta1)));
          ADPCM.pcmInterpolationEngine.write (    Math.max (-2048 << 4, Math.min (2047 << 4, m               + delta2)));
        }
      } else {  //有効なPCMデータがないとき
        int m = ADPCM.pcmDecodedData1;
        if (ADPCM.PCM_DECAY_ON) {  //減衰させる
          ADPCM.pcmInterpolationEngine.write (m += (m >>> 31) - (-m >>> 31));
          ADPCM.pcmInterpolationEngine.write (m +  (m >>> 31) - (-m >>> 31));
        } else {  //同じデータを繰り返す
          ADPCM.pcmInterpolationEngine.write (m);
          ADPCM.pcmInterpolationEngine.write (m);
        }
      }
      //次の割り込み時刻を設定する
      ADPCM.pcmClock += ADPCM.pcmInterval;
      TickerQueue.tkqAdd (sndPcmTicker, ADPCM.pcmClock);
      //DMAに次のデータを要求する
      HD63450.dmaFallPCL (3);
      HD63450.dmaFallREQ (3);
    }
  };

  //sndInit ()
  //  サウンドを初期化する
  public static void sndInit () {
    //ソースデータライン
    sndNativeEndian = ByteOrder.nativeOrder ();
    if (SND_ON) {
      try {
        AudioFormat audioFormat = new AudioFormat ((float) SND_SAMPLE_FREQ,
                                                   16,
                                                   SND_CHANNELS,
                                                   true,
                                                   SND_BYTE_BUFFER_ENDIAN ? sndNativeEndian == ByteOrder.BIG_ENDIAN : false);
        //sndLine = (SourceDataLine) (AudioSystem.getLine (new DataLine.Info (SourceDataLine.class, audioFormat)));
        sndLine = AudioSystem.getSourceDataLine (audioFormat);  //AudioSystem.getSourceDataLine()は1.5から
        sndLine.open (audioFormat, SND_BUFFER_BYTES);
        sndLine.start ();
      } catch (Exception e) {
        sndLine = null;  //出力不可
        sndPlayOn = false;
      }
    } else {
      sndLine = null;  //出力不可
      sndPlayOn = false;
    }
    //ブロック
    //sndByteBlock = new byte[SND_BUFFER_BYTES];
    if (SND_BYTE_BUFFER_ENDIAN) {
      //エンディアン制御
      sndShortBlock = new short[SND_CHANNELS * SND_BUFFER_SAMPLES];
      sndByteBuffer = ByteBuffer.wrap (sndByteBlock);
      sndByteBuffer.order (sndNativeEndian);
      sndShortBuffer = sndByteBuffer.asShortBuffer ();
    }
    //周波数変換
    if (SND_FREQ_TABLE) {
      //sndFreqConvIndex = new int[SND_BLOCK_SAMPLES];
      for (int i = 0; i < SND_BLOCK_SAMPLES; i++) {
        if (!SND_INTERPOLATION_ON) {  //間引き
          sndFreqConvIndex[i] = SND_CHANNELS * i * OPM.OPM_BLOCK_SAMPLES / SND_BLOCK_SAMPLES;
        } else {  //線形補間
          int t = i * OPM.OPM_BLOCK_SAMPLES;  //0～(882-1)*62500,(1764-1)*62500,(1920-1)*62500
          int q = t / SND_BLOCK_SAMPLES;  //サンプリング位置。入力周波数の62500Hzよりも出力周波数の22050Hz,44100Hz,48000Hzの方が小さく、サンプリング位置の最大値が2498になるため、インデックスに1を加えても溢れない
          sndFreqConvIndex[i] = SND_CHANNELS * q;  //インデックス
          sndFreqConvFraction[i] = (t - q * SND_BLOCK_SAMPLES << SND_INTERPOLATION_BIT) / SND_BLOCK_SAMPLES;  //端数
        }
      }
    }
    //sndRateConverter = SND_CHANNELS == 1 ? SNDRateConverter.LINEAR_MONO : SNDRateConverter.LINEAR_STEREO;  //線形補間
    //ボリュームコントロール
    sndCurrentScale = 0;
    sndTargetScale = sndPlayOn && sndVolume > 0 ? (int) (Math.pow (2.0, 12 - 1 + (double) (sndVolume - SND_VOLUME_DEFAULT) / SND_VOLUME_STEP) + 0.5) : 0;
    if (SND_ZERO_LEVEL_SHIFT) {
      //ゼロレベルシフト
      if (SND_CHANNELS == 1) {  //モノラル
        sndAdaptiveShiftM = 0;
      } else {  //ステレオ
        sndAdaptiveShiftL = 0;
        sndAdaptiveShiftR = 0;
      }
    }
  }  //sndInit

  //sndTini ()
  //  サウンドの後始末
  //  ラインを閉じる
  public static void sndTini () {
    if (sndLine != null) {
      sndLine.stop ();
      sndLine.close ();
    }
  }  //sndTini()

  //sndStart ()
  //  サウンドの動作を開始する
  public static void sndStart () {
    sndReset ();
    OPM.opmReset ();
    ADPCM.pcmReset ();
  }  //sndStart

  //リセット
  public static void sndReset () {
    ADPCM.pcmClock = XEiJ.FAR_FUTURE;
    sndBlockClock = XEiJ.mpuClockTime + SND_BLOCK_TIME;
    TickerQueue.tkqRemove (sndPcmTicker);
    TickerQueue.tkqAdd (sndBlockTicker, sndBlockClock);
  }  //sndReset

  //sndSetPlayOn (mode)
  //  音声出力
  public static void sndSetPlayOn (boolean on) {
    if (sndLine != null && sndPlayOn != on) {
      if (on) {
        System.out.println (Multilingual.mlnJapanese ?
                            "音声を出力します" :
                            "Sound is output");
      } else {
        System.out.println (Multilingual.mlnJapanese ?
                            "音声を出力しません" :
                            "Sound is not output");
      }
      sndPlayOn = on;
      sndSetVolume (sndVolume);
      //コアが動いていたら再起動する
      if (XEiJ.mpuTask != null) {
        XEiJ.mpuStart ();
      }
    }
  }  //sndSetPlayOn(boolean)

  //sndSetVolume (volume)
  //  音量
  public static void sndSetVolume (int volume) {
    sndVolume = Math.max (0, Math.min (SND_VOLUME_MAX, volume));
    sndTargetScale = sndPlayOn && sndVolume > 0 ? (int) (Math.pow (2.0, 12 - 1 + (double) (sndVolume - SND_VOLUME_DEFAULT) / SND_VOLUME_STEP) + 0.5) : 0;
    if (XEiJ.mnbVolumeLabel != null) {
      XEiJ.mnbVolumeLabel.setText (String.valueOf (sndVolume));
    }
  }  //sndSetVolume(int)



  //enum SNDRateConverter
  //  サンプリング周波数変換
  //  入力サンプリング周波数と出力サンプリング周波数が両方高いとどの変換も同じように聞こえる
  public static enum SNDRateConverter {

    //規定の周波数で出力しないとき
    ADAPTIVE {
      @Override public void convert (int outputSamples) {
        final int inputSamples = OPM.OPM_BLOCK_SAMPLES;
        int src = 0;
        int dst = 0;
        int balance = (inputSamples >> 1) - outputSamples;
        if (balance >= 0) {
          do {
            src++;
          } while ((balance -= outputSamples) >= 0);
        }
      outputLoop:
        for (;;) {
          if (SND_CHANNELS == 1) {  //モノラル
            int m = (((OPM.OPM_ON ? OPM.opmBuffer[src] : 0) +
                      (ADPCM.PCM_ON ? ADPCM.pcmBuffer[src] : 0) +
                      (MercuryUnit.MU4_ON ? MercuryUnit.mu4Buffer[src] : 0)) * sndCurrentScale) >> 12;
            if (SND_ZERO_LEVEL_SHIFT) {  //ゼロレベルシフトあり
              m += sndAdaptiveShiftM;
              if ((short) m != m) {  //オーバーフローしたとき
                int t = m >> 31 ^ 0x7fff;
                sndAdaptiveShiftM -= m - t;
                m = t;
              }
              sndAdaptiveShiftM += (sndAdaptiveShiftM >>> 31) - (-sndAdaptiveShiftM >>> 31);
            } else {  //ゼロレベルシフトなし
              m = XEiJ.SHORT_SATURATION_CAST ? (short) m == m ? m : m >> 31 ^ 32767 : Math.max (-32768, Math.min (32767, m));
            }
            do {
              if (SND_BYTE_BUFFER_ENDIAN) {  //ByteBufferでエンディアンを制御するとき
                sndShortBlock[dst] = (short) m;
              } else {  //ByteBufferでエンディアンを制御しないとき
                int dst2 = dst << 1;
                sndByteBlock[dst2    ] = (byte) m;
                sndByteBlock[dst2 + 1] = (byte) (m >> 8);
              }
              sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
              dst++;
              if (dst >= outputSamples) {
                break outputLoop;
              }
            } while ((balance += inputSamples) < 0);
          } else {  //ステレオ
            int src2 = src << 1;
            int l = (((OPM.OPM_ON ? OPM.opmBuffer[src2] : 0) +
                      (ADPCM.PCM_ON ? ADPCM.pcmBuffer[src2] : 0) +
                      (MercuryUnit.MU4_ON ? MercuryUnit.mu4Buffer[src2] : 0)) * sndCurrentScale) >> 12;
            int r = (((OPM.OPM_ON ? OPM.opmBuffer[src2 + 1] : 0) +
                      (ADPCM.PCM_ON ? ADPCM.pcmBuffer[src2 + 1] : 0) +
                      (MercuryUnit.MU4_ON ? MercuryUnit.mu4Buffer[src2 + 1] : 0)) * sndCurrentScale) >> 12;
            if (SND_ZERO_LEVEL_SHIFT) {  //ゼロレベルシフトあり
              l += sndAdaptiveShiftL;
              r += sndAdaptiveShiftR;
              if ((short) l != l) {  //オーバーフローしたとき
                int t = l >> 31 ^ 0x7fff;
                sndAdaptiveShiftL -= l - t;
                l = t;
              }
              if ((short) r != r) {  //オーバーフローしたとき
                int t = r >> 31 ^ 0x7fff;
                sndAdaptiveShiftR -= r - t;
                r = t;
              }
              sndAdaptiveShiftL += (sndAdaptiveShiftL >>> 31) - (-sndAdaptiveShiftL >>> 31);
              sndAdaptiveShiftR += (sndAdaptiveShiftR >>> 31) - (-sndAdaptiveShiftR >>> 31);
            } else {  //ゼロレベルシフトなし
              l = XEiJ.SHORT_SATURATION_CAST ? (short) l == l ? l : l >> 31 ^ 32767 : Math.max (-32768, Math.min (32767, l));
              r = XEiJ.SHORT_SATURATION_CAST ? (short) r == r ? r : r >> 31 ^ 32767 : Math.max (-32768, Math.min (32767, r));
            }
            do {
              if (SND_BYTE_BUFFER_ENDIAN) {  //ByteBufferでエンディアンを制御するとき
                int dst2 = dst << 1;
                sndShortBlock[dst2    ] = (short) l;
                sndShortBlock[dst2 + 1] = (short) r;
              } else {  //ByteBufferでエンディアンを制御しないとき
                int dst4 = dst << 2;
                sndByteBlock[dst4    ] = (byte) l;
                sndByteBlock[dst4 + 1] = (byte) (l >> 8);
                sndByteBlock[dst4 + 2] = (byte) r;
                sndByteBlock[dst4 + 3] = (byte) (r >> 8);
              }
              sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
              dst++;
              if (dst >= outputSamples) {
                break outputLoop;
              }
            } while ((balance += inputSamples) < 0);
          }  //if モノラル/ステレオ
          do {
            src++;
          } while ((balance -= outputSamples) >= 0);
        }  //outputLoop
        if (SND_BYTE_BUFFER_ENDIAN) {  //ByteBufferでエンディアンを制御するとき
          //sndByteBlockにsndShortBlockを上書きする
          sndShortBuffer.rewind ();
          sndShortBuffer.put (sndShortBlock, 0, outputSamples << SND_SAMPLE_SHIFT - 1);
        }
      }  //convert(int)
    },  //SNDRateConverter.ADAPTIVE

    //間引き,モノラル
    THINNING_MONO {
      @Override public void convert (int outputSamples) {
        for (int dst = 0; dst < SND_BLOCK_SAMPLES; dst++) {
          int src = sndFreqConvIndex[dst];
          int m = (((OPM.OPM_ON ? OPM.opmBuffer[src] : 0) +
                    (ADPCM.PCM_ON ? ADPCM.pcmBuffer[src] : 0) +
                    (MercuryUnit.MU4_ON ? MercuryUnit.mu4Buffer[src] : 0)) * sndCurrentScale) >> 12;
          if (SND_ZERO_LEVEL_SHIFT) {  //ゼロレベルシフトあり
            m += sndAdaptiveShiftM;
            if ((short) m != m) {  //オーバーフローしたとき
              int t = m >> 31 ^ 0x7fff;
              sndAdaptiveShiftM -= m - t;
              m = t;
            }
            sndAdaptiveShiftM += (sndAdaptiveShiftM >>> 31) - (-sndAdaptiveShiftM >>> 31);
          } else {  //ゼロレベルシフトなし
            m = XEiJ.SHORT_SATURATION_CAST ? (short) m == m ? m : m >> 31 ^ 32767 : Math.max (-32768, Math.min (32767, m));
          }
          if (SND_BYTE_BUFFER_ENDIAN) {  //ByteBufferでエンディアンを制御するとき
            sndShortBlock[dst] = (short) m;
          } else {  //ByteBufferでエンディアンを制御しないとき
            int dst2 = dst << 1;
            sndByteBlock[dst2    ] = (byte) m;
            sndByteBlock[dst2 + 1] = (byte) (m >> 8);
          }
          sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
        }  //for dst
        if (SND_BYTE_BUFFER_ENDIAN) {  //ByteBufferでエンディアンを制御するとき
          //sndByteBlockにsndShortBlockを上書きする
          sndShortBuffer.rewind ();
          sndShortBuffer.put (sndShortBlock, 0, outputSamples << SND_SAMPLE_SHIFT - 1);
        }
      }  //convert(int)
    },  //SNDRateConverter.THINNING_MONO

    //間引き,ステレオ
    THINNING_STEREO {
      @Override public void convert (int outputSamples) {
        for (int dst = 0; dst < SND_BLOCK_SAMPLES; dst++) {
          int src = sndFreqConvIndex[dst];
          int l = (((OPM.OPM_ON ? OPM.opmBuffer[src] : 0) +
                    (ADPCM.PCM_ON ? ADPCM.pcmBuffer[src] : 0) +
                    (MercuryUnit.MU4_ON ? MercuryUnit.mu4Buffer[src] : 0)) * sndCurrentScale) >> 12;
          int r = (((OPM.OPM_ON ? OPM.opmBuffer[src + 1] : 0) +
                    (ADPCM.PCM_ON ? ADPCM.pcmBuffer[src + 1] : 0) +
                    (MercuryUnit.MU4_ON ? MercuryUnit.mu4Buffer[src + 1] : 0)) * sndCurrentScale) >> 12;
          if (SND_ZERO_LEVEL_SHIFT) {  //ゼロレベルシフトあり
            l += sndAdaptiveShiftL;
            r += sndAdaptiveShiftR;
            if ((short) l != l) {  //オーバーフローしたとき
              int t = l >> 31 ^ 0x7fff;
              sndAdaptiveShiftL -= l - t;
              l = t;
            }
            if ((short) r != r) {  //オーバーフローしたとき
              int t = r >> 31 ^ 0x7fff;
              sndAdaptiveShiftR -= r - t;
              r = t;
            }
            sndAdaptiveShiftL += (sndAdaptiveShiftL >>> 31) - (-sndAdaptiveShiftL >>> 31);
            sndAdaptiveShiftR += (sndAdaptiveShiftR >>> 31) - (-sndAdaptiveShiftR >>> 31);
          } else {  //ゼロレベルシフトなし
            l = XEiJ.SHORT_SATURATION_CAST ? (short) l == l ? l : l >> 31 ^ 32767 : Math.max (-32768, Math.min (32767, l));
            r = XEiJ.SHORT_SATURATION_CAST ? (short) r == r ? r : r >> 31 ^ 32767 : Math.max (-32768, Math.min (32767, r));
          }
          if (SND_BYTE_BUFFER_ENDIAN) {  //ByteBufferでエンディアンを制御するとき
            int dst2 = dst << 1;
            sndShortBlock[dst2    ] = (short) l;
            sndShortBlock[dst2 + 1] = (short) r;
          } else {  //ByteBufferでエンディアンを制御しないとき
            int dst4 = dst << 2;
            sndByteBlock[dst4    ] = (byte) l;
            sndByteBlock[dst4 + 1] = (byte) (l >> 8);
            sndByteBlock[dst4 + 2] = (byte) r;
            sndByteBlock[dst4 + 3] = (byte) (r >> 8);
          }
          sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
        }  //for dst
        if (SND_BYTE_BUFFER_ENDIAN) {  //ByteBufferでエンディアンを制御するとき
          //sndByteBlockにsndShortBlockを上書きする
          sndShortBuffer.rewind ();
          sndShortBuffer.put (sndShortBlock, 0, outputSamples << SND_SAMPLE_SHIFT - 1);
        }
      }  //convert(int)
    },  //SNDRateConverter.THINNING_STEREO

    //線形補間,モノラル
    LINEAR_MONO {
      @Override public void convert (int outputSamples) {
        for (int dst = 0; dst < SND_BLOCK_SAMPLES; dst++) {
          int src = sndFreqConvIndex[dst];
          int rat2 = sndFreqConvFraction[dst];
          int rat1 = (1 << SND_INTERPOLATION_BIT) - rat2;
          int m = (((((OPM.OPM_ON ? OPM.opmBuffer[src] : 0) +
                      (ADPCM.PCM_ON ? ADPCM.pcmBuffer[src] : 0) +
                      (MercuryUnit.MU4_ON ? MercuryUnit.mu4Buffer[src] : 0)) * rat1 +
                     ((OPM.OPM_ON ? OPM.opmBuffer[src + 1] : 0) +
                      (ADPCM.PCM_ON ? ADPCM.pcmBuffer[src + 1] : 0) +
                      (MercuryUnit.MU4_ON ? MercuryUnit.mu4Buffer[src + 1] : 0)) * rat2) >> SND_INTERPOLATION_BIT) * sndCurrentScale) >> 12;
          if (SND_ZERO_LEVEL_SHIFT) {  //ゼロレベルシフトあり
            m += sndAdaptiveShiftM;
            if ((short) m != m) {  //オーバーフローしたとき
              int t = m >> 31 ^ 0x7fff;
              sndAdaptiveShiftM -= m - t;
              m = t;
            }
            sndAdaptiveShiftM += (sndAdaptiveShiftM >>> 31) - (-sndAdaptiveShiftM >>> 31);
          } else {  //ゼロレベルシフトなし
            m = XEiJ.SHORT_SATURATION_CAST ? (short) m == m ? m : m >> 31 ^ 32767 : Math.max (-32768, Math.min (32767, m));
          }
          if (SND_BYTE_BUFFER_ENDIAN) {  //ByteBufferでエンディアンを制御するとき
            sndShortBlock[dst] = (short) m;
          } else {  //ByteBufferでエンディアンを制御しないとき
            int dst2 = dst << 1;
            sndByteBlock[dst2    ] = (byte) m;
            sndByteBlock[dst2 + 1] = (byte) (m >> 8);
          }
          sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
        }  //for dst
        if (SND_BYTE_BUFFER_ENDIAN) {  //ByteBufferでエンディアンを制御するとき
          //sndByteBlockにsndShortBlockを上書きする
          sndShortBuffer.rewind ();
          sndShortBuffer.put (sndShortBlock, 0, outputSamples << SND_SAMPLE_SHIFT - 1);
        }
      }  //convert(int)
    },  //SNDRateConverter.LINEAR_MONO

    //線形補間,ステレオ
    LINEAR_STEREO {
      @Override public void convert (int outputSamples) {
        for (int dst = 0; dst < SND_BLOCK_SAMPLES; dst++) {
          int src2 = sndFreqConvIndex[dst];
          int rat2 = sndFreqConvFraction[dst];
          int rat1 = (1 << SND_INTERPOLATION_BIT) - rat2;
          int l = (((((OPM.OPM_ON ? OPM.opmBuffer[src2] : 0) +
                      (ADPCM.PCM_ON ? ADPCM.pcmBuffer[src2] : 0) +
                      (MercuryUnit.MU4_ON ? MercuryUnit.mu4Buffer[src2] : 0)) * rat1 +
                     ((OPM.OPM_ON ? OPM.opmBuffer[src2 + 2] : 0) +
                      (ADPCM.PCM_ON ? ADPCM.pcmBuffer[src2 + 2] : 0) +
                      (MercuryUnit.MU4_ON ? MercuryUnit.mu4Buffer[src2 + 2] : 0)) * rat2) >> SND_INTERPOLATION_BIT) * sndCurrentScale) >> 12;
          int r = (((((OPM.OPM_ON ? OPM.opmBuffer[src2 + 1] : 0) +
                      (ADPCM.PCM_ON ? ADPCM.pcmBuffer[src2 + 1] : 0) +
                      (MercuryUnit.MU4_ON ? MercuryUnit.mu4Buffer[src2 + 1] : 0)) * rat1 +
                     ((OPM.OPM_ON ? OPM.opmBuffer[src2 + 3] : 0) +
                      (ADPCM.PCM_ON ? ADPCM.pcmBuffer[src2 + 3] : 0) +
                      (MercuryUnit.MU4_ON ? MercuryUnit.mu4Buffer[src2 + 3] : 0)) * rat2) >> SND_INTERPOLATION_BIT) * sndCurrentScale) >> 12;
          if (SND_ZERO_LEVEL_SHIFT) {  //ゼロレベルシフトあり
            l += sndAdaptiveShiftL;
            r += sndAdaptiveShiftR;
            if ((short) l != l) {  //オーバーフローしたとき
              int t = l >> 31 ^ 0x7fff;
              sndAdaptiveShiftL -= l - t;
              l = t;
            }
            if ((short) r != r) {  //オーバーフローしたとき
              int t = r >> 31 ^ 0x7fff;
              sndAdaptiveShiftR -= r - t;
              r = t;
            }
            sndAdaptiveShiftL += (sndAdaptiveShiftL >>> 31) - (-sndAdaptiveShiftL >>> 31);
            sndAdaptiveShiftR += (sndAdaptiveShiftR >>> 31) - (-sndAdaptiveShiftR >>> 31);
          } else {  //ゼロレベルシフトなし
            l = XEiJ.SHORT_SATURATION_CAST ? (short) l == l ? l : l >> 31 ^ 32767 : Math.max (-32768, Math.min (32767, l));
            r = XEiJ.SHORT_SATURATION_CAST ? (short) r == r ? r : r >> 31 ^ 32767 : Math.max (-32768, Math.min (32767, r));
          }
          if (SND_BYTE_BUFFER_ENDIAN) {  //ByteBufferでエンディアンを制御するとき
            int dst2 = dst << 1;
            sndShortBlock[dst2    ] = (short) l;
            sndShortBlock[dst2 + 1] = (short) r;
          } else {  //ByteBufferでエンディアンを制御しないとき
            int dst4 = dst << 2;
            sndByteBlock[dst4    ] = (byte) l;
            sndByteBlock[dst4 + 1] = (byte) (l >> 8);
            sndByteBlock[dst4 + 2] = (byte) r;
            sndByteBlock[dst4 + 3] = (byte) (r >> 8);
          }
          sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
        }  //for dst
        if (SND_BYTE_BUFFER_ENDIAN) {  //ByteBufferでエンディアンを制御するとき
          //sndByteBlockにsndShortBlockを上書きする
          sndShortBuffer.rewind ();
          sndShortBuffer.put (sndShortBlock, 0, outputSamples << SND_SAMPLE_SHIFT - 1);
        }
      }  //convert(int)
    },  //SNDRateConverter.LINEAR_STEREO

    //区分定数面積補間
    //  入力データを区分定数補間した波形を出力サンプリング間隔で刻み、各断片の平均の変位(断片の面積に比例する)を出力データとする
    //  出力周波数が入力周波数の1/2倍～1倍のとき、1個の出力データを求めるために2個または3個の入力データが必要になる
    //  7Hz→5Hzの場合
    //    aaaaabbbbbcccccdddddeeeeefffffggggg
    //    AAAAAAABBBBBBBCCCCCCCDDDDDDDEEEEEEE
    //      A=(5*a+2*b)/7
    //      B=(3*b+4*c)/7
    //      C=(1*c+5*d+1*e)/7
    //      D=(4*e+3*f)/7
    //      E=(2*f+5*g)/7
    //    perl -e "$m=7;$n=5;$k=0;for$i(0..$n-1){$j0=int($i*$m/$n);$c0=$n-$i*$m+$j0*$n;$c1=$m-$c0;if($c1>$n){$c2=$c1-$n;$c1=$n}elsif($c0<=$c1){$c2=0}else{$c2=$c1;$c1=$c0;$c0=0;$j0--}printf'    //      d[%2d] = (',$i;if($c0){if($c0==1){print'    '}else{printf'%2d *',$c0};printf' s[%3d] +',$j0}else{print'             '};printf' %2d * s[%3d] ',$c1,$j0+1;if($c2){print'+ ';if($c2==1){print'    '}else{printf'%2d *',$c2};printf' s[%3d]',$j0+2}else{print'             '};printf') / %d;%c',$m,10}"
    //      d[ 0] = (               5 * s[  0] +  2 * s[  1]) / 7;
    //      d[ 1] = ( 3 * s[  1] +  4 * s[  2]              ) / 7;
    //      d[ 2] = (     s[  2] +  5 * s[  3] +      s[  4]) / 7;
    //      d[ 3] = (               4 * s[  4] +  3 * s[  5]) / 7;
    //      d[ 4] = ( 2 * s[  5] +  5 * s[  6]              ) / 7;

    //区分定数面積補間,ステレオ,48kHz
    CONSTANT_AREA_STEREO_48000 {
      @Override public void convert (int outputSamples) {
        //OPMとPCMを合わせてボリュームを掛ける
        for (int src = 0; src < 2 * OPM.OPM_BLOCK_SAMPLES; src += 2) {
          OPM.opmBuffer[src] = (((OPM.OPM_ON ? OPM.opmBuffer[src] : 0) +
                                 (ADPCM.PCM_ON ? ADPCM.pcmBuffer[src] : 0) +
                                 (MercuryUnit.MU4_ON ? MercuryUnit.mu4Buffer[src] : 0)) * sndCurrentScale) >> 12;
          OPM.opmBuffer[src + 1] = (((OPM.OPM_ON ? OPM.opmBuffer[src + 1] : 0) +
                                     (ADPCM.PCM_ON ? ADPCM.pcmBuffer[src + 1] : 0) +
                                     (MercuryUnit.MU4_ON ? MercuryUnit.mu4Buffer[src + 1] : 0)) * sndCurrentScale) >> 12;
        }
        //区分定数面積補間
        //  通分したときの分母は125だが、125≒128=1<<7なので、125で割る代わりに右7bitシフトで済ませている
        //  本来の値よりもわずかに小さい値(125/128倍)が出力される
        for (int src = 0, dst = 0; src < 2 * OPM.OPM_BLOCK_SAMPLES; src += 2 * 125, dst += 4 * 96) {
          int l, r;
                //  perl -e "$m=125;$n=96;$k=0;for$i(0..$n-1){$j0=int($i*$m/$n);$c0=$n-$i*$m+$j0*$n;$c1=$m-$c0;if($c1>$n){$c2=$c1-$n;$c1=$n}elsif($c0<=$c1){$c2=0}else{$c2=$c1;$c1=$c0;$c0=0;$j0--}printf'%16sl = Math.max (-32768, Math.min (32767, ','',$i;if($c0){if($c0==1){print'    '}else{printf'%2d *',$c0};printf' OPM.opmBuffer[src + %3d] +',$j0<<1|0}else{printf'%27s',''};printf' %2d * OPM.opmBuffer[src + %3d] ',$c1,$j0+1<<1|0;if($c2){print'+ ';if($c2==1){print'    '}else{printf'%2d *',$c2};printf' OPM.opmBuffer[src + %3d]',$j0+2<<1|0}else{printf'%27s',''};printf' + 64 >> 7));%c',10;printf'%16sr = Math.max (-32768, Math.min (32767, ','',$i;if($c0){if($c0==1){print'    '}else{printf'%2d *',$c0};printf' OPM.opmBuffer[src + %3d] +',$j0<<1|1}else{printf'%27s',''};printf' %2d * OPM.opmBuffer[src + %3d] ',$c1,$j0+1<<1|1;if($c2){print'+ ';if($c2==1){print'    '}else{printf'%2d *',$c2};printf' OPM.opmBuffer[src + %3d]',$j0+2<<1|1}else{printf'%27s',''};printf' + 64 >> 7));%c',10;printf'%16ssndByteBlock[dst + %2d] = (byte) l;%c','',$i<<2|0,10;printf'%16ssndByteBlock[dst + %2d] = (byte) (l >> 8);%c','',$i<<2|1,10;printf'%16ssndByteBlock[dst + %2d] = (byte) r;%c','',$i<<2|2,10;printf'%16ssndByteBlock[dst + %2d] = (byte) (r >> 8);%c','',$i<<2|3,10;printf'%16ssndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);%c','',10}"
                l = Math.max (-32768, Math.min (32767,                             96 * OPM.opmBuffer[src +   0] + 29 * OPM.opmBuffer[src +   2] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,                             96 * OPM.opmBuffer[src +   1] + 29 * OPM.opmBuffer[src +   3] + 64 >> 7));
                sndByteBlock[dst +  0] = (byte) l;
                sndByteBlock[dst +  1] = (byte) (l >> 8);
                sndByteBlock[dst +  2] = (byte) r;
                sndByteBlock[dst +  3] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,                             67 * OPM.opmBuffer[src +   2] + 58 * OPM.opmBuffer[src +   4] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,                             67 * OPM.opmBuffer[src +   3] + 58 * OPM.opmBuffer[src +   5] + 64 >> 7));
                sndByteBlock[dst +  4] = (byte) l;
                sndByteBlock[dst +  5] = (byte) (l >> 8);
                sndByteBlock[dst +  6] = (byte) r;
                sndByteBlock[dst +  7] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 38 * OPM.opmBuffer[src +   4] + 87 * OPM.opmBuffer[src +   6]                             + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 38 * OPM.opmBuffer[src +   5] + 87 * OPM.opmBuffer[src +   7]                             + 64 >> 7));
                sndByteBlock[dst +  8] = (byte) l;
                sndByteBlock[dst +  9] = (byte) (l >> 8);
                sndByteBlock[dst + 10] = (byte) r;
                sndByteBlock[dst + 11] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,  9 * OPM.opmBuffer[src +   6] + 96 * OPM.opmBuffer[src +   8] + 20 * OPM.opmBuffer[src +  10] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,  9 * OPM.opmBuffer[src +   7] + 96 * OPM.opmBuffer[src +   9] + 20 * OPM.opmBuffer[src +  11] + 64 >> 7));
                sndByteBlock[dst + 12] = (byte) l;
                sndByteBlock[dst + 13] = (byte) (l >> 8);
                sndByteBlock[dst + 14] = (byte) r;
                sndByteBlock[dst + 15] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,                             76 * OPM.opmBuffer[src +  10] + 49 * OPM.opmBuffer[src +  12] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,                             76 * OPM.opmBuffer[src +  11] + 49 * OPM.opmBuffer[src +  13] + 64 >> 7));
                sndByteBlock[dst + 16] = (byte) l;
                sndByteBlock[dst + 17] = (byte) (l >> 8);
                sndByteBlock[dst + 18] = (byte) r;
                sndByteBlock[dst + 19] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 47 * OPM.opmBuffer[src +  12] + 78 * OPM.opmBuffer[src +  14]                             + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 47 * OPM.opmBuffer[src +  13] + 78 * OPM.opmBuffer[src +  15]                             + 64 >> 7));
                sndByteBlock[dst + 20] = (byte) l;
                sndByteBlock[dst + 21] = (byte) (l >> 8);
                sndByteBlock[dst + 22] = (byte) r;
                sndByteBlock[dst + 23] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 18 * OPM.opmBuffer[src +  14] + 96 * OPM.opmBuffer[src +  16] + 11 * OPM.opmBuffer[src +  18] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 18 * OPM.opmBuffer[src +  15] + 96 * OPM.opmBuffer[src +  17] + 11 * OPM.opmBuffer[src +  19] + 64 >> 7));
                sndByteBlock[dst + 24] = (byte) l;
                sndByteBlock[dst + 25] = (byte) (l >> 8);
                sndByteBlock[dst + 26] = (byte) r;
                sndByteBlock[dst + 27] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,                             85 * OPM.opmBuffer[src +  18] + 40 * OPM.opmBuffer[src +  20] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,                             85 * OPM.opmBuffer[src +  19] + 40 * OPM.opmBuffer[src +  21] + 64 >> 7));
                sndByteBlock[dst + 28] = (byte) l;
                sndByteBlock[dst + 29] = (byte) (l >> 8);
                sndByteBlock[dst + 30] = (byte) r;
                sndByteBlock[dst + 31] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 56 * OPM.opmBuffer[src +  20] + 69 * OPM.opmBuffer[src +  22]                             + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 56 * OPM.opmBuffer[src +  21] + 69 * OPM.opmBuffer[src +  23]                             + 64 >> 7));
                sndByteBlock[dst + 32] = (byte) l;
                sndByteBlock[dst + 33] = (byte) (l >> 8);
                sndByteBlock[dst + 34] = (byte) r;
                sndByteBlock[dst + 35] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 27 * OPM.opmBuffer[src +  22] + 96 * OPM.opmBuffer[src +  24] +  2 * OPM.opmBuffer[src +  26] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 27 * OPM.opmBuffer[src +  23] + 96 * OPM.opmBuffer[src +  25] +  2 * OPM.opmBuffer[src +  27] + 64 >> 7));
                sndByteBlock[dst + 36] = (byte) l;
                sndByteBlock[dst + 37] = (byte) (l >> 8);
                sndByteBlock[dst + 38] = (byte) r;
                sndByteBlock[dst + 39] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,                             94 * OPM.opmBuffer[src +  26] + 31 * OPM.opmBuffer[src +  28] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,                             94 * OPM.opmBuffer[src +  27] + 31 * OPM.opmBuffer[src +  29] + 64 >> 7));
                sndByteBlock[dst + 40] = (byte) l;
                sndByteBlock[dst + 41] = (byte) (l >> 8);
                sndByteBlock[dst + 42] = (byte) r;
                sndByteBlock[dst + 43] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,                             65 * OPM.opmBuffer[src +  28] + 60 * OPM.opmBuffer[src +  30] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,                             65 * OPM.opmBuffer[src +  29] + 60 * OPM.opmBuffer[src +  31] + 64 >> 7));
                sndByteBlock[dst + 44] = (byte) l;
                sndByteBlock[dst + 45] = (byte) (l >> 8);
                sndByteBlock[dst + 46] = (byte) r;
                sndByteBlock[dst + 47] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 36 * OPM.opmBuffer[src +  30] + 89 * OPM.opmBuffer[src +  32]                             + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 36 * OPM.opmBuffer[src +  31] + 89 * OPM.opmBuffer[src +  33]                             + 64 >> 7));
                sndByteBlock[dst + 48] = (byte) l;
                sndByteBlock[dst + 49] = (byte) (l >> 8);
                sndByteBlock[dst + 50] = (byte) r;
                sndByteBlock[dst + 51] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,  7 * OPM.opmBuffer[src +  32] + 96 * OPM.opmBuffer[src +  34] + 22 * OPM.opmBuffer[src +  36] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,  7 * OPM.opmBuffer[src +  33] + 96 * OPM.opmBuffer[src +  35] + 22 * OPM.opmBuffer[src +  37] + 64 >> 7));
                sndByteBlock[dst + 52] = (byte) l;
                sndByteBlock[dst + 53] = (byte) (l >> 8);
                sndByteBlock[dst + 54] = (byte) r;
                sndByteBlock[dst + 55] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,                             74 * OPM.opmBuffer[src +  36] + 51 * OPM.opmBuffer[src +  38] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,                             74 * OPM.opmBuffer[src +  37] + 51 * OPM.opmBuffer[src +  39] + 64 >> 7));
                sndByteBlock[dst + 56] = (byte) l;
                sndByteBlock[dst + 57] = (byte) (l >> 8);
                sndByteBlock[dst + 58] = (byte) r;
                sndByteBlock[dst + 59] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 45 * OPM.opmBuffer[src +  38] + 80 * OPM.opmBuffer[src +  40]                             + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 45 * OPM.opmBuffer[src +  39] + 80 * OPM.opmBuffer[src +  41]                             + 64 >> 7));
                sndByteBlock[dst + 60] = (byte) l;
                sndByteBlock[dst + 61] = (byte) (l >> 8);
                sndByteBlock[dst + 62] = (byte) r;
                sndByteBlock[dst + 63] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 16 * OPM.opmBuffer[src +  40] + 96 * OPM.opmBuffer[src +  42] + 13 * OPM.opmBuffer[src +  44] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 16 * OPM.opmBuffer[src +  41] + 96 * OPM.opmBuffer[src +  43] + 13 * OPM.opmBuffer[src +  45] + 64 >> 7));
                sndByteBlock[dst + 64] = (byte) l;
                sndByteBlock[dst + 65] = (byte) (l >> 8);
                sndByteBlock[dst + 66] = (byte) r;
                sndByteBlock[dst + 67] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,                             83 * OPM.opmBuffer[src +  44] + 42 * OPM.opmBuffer[src +  46] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,                             83 * OPM.opmBuffer[src +  45] + 42 * OPM.opmBuffer[src +  47] + 64 >> 7));
                sndByteBlock[dst + 68] = (byte) l;
                sndByteBlock[dst + 69] = (byte) (l >> 8);
                sndByteBlock[dst + 70] = (byte) r;
                sndByteBlock[dst + 71] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 54 * OPM.opmBuffer[src +  46] + 71 * OPM.opmBuffer[src +  48]                             + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 54 * OPM.opmBuffer[src +  47] + 71 * OPM.opmBuffer[src +  49]                             + 64 >> 7));
                sndByteBlock[dst + 72] = (byte) l;
                sndByteBlock[dst + 73] = (byte) (l >> 8);
                sndByteBlock[dst + 74] = (byte) r;
                sndByteBlock[dst + 75] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 25 * OPM.opmBuffer[src +  48] + 96 * OPM.opmBuffer[src +  50] +  4 * OPM.opmBuffer[src +  52] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 25 * OPM.opmBuffer[src +  49] + 96 * OPM.opmBuffer[src +  51] +  4 * OPM.opmBuffer[src +  53] + 64 >> 7));
                sndByteBlock[dst + 76] = (byte) l;
                sndByteBlock[dst + 77] = (byte) (l >> 8);
                sndByteBlock[dst + 78] = (byte) r;
                sndByteBlock[dst + 79] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,                             92 * OPM.opmBuffer[src +  52] + 33 * OPM.opmBuffer[src +  54] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,                             92 * OPM.opmBuffer[src +  53] + 33 * OPM.opmBuffer[src +  55] + 64 >> 7));
                sndByteBlock[dst + 80] = (byte) l;
                sndByteBlock[dst + 81] = (byte) (l >> 8);
                sndByteBlock[dst + 82] = (byte) r;
                sndByteBlock[dst + 83] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,                             63 * OPM.opmBuffer[src +  54] + 62 * OPM.opmBuffer[src +  56] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,                             63 * OPM.opmBuffer[src +  55] + 62 * OPM.opmBuffer[src +  57] + 64 >> 7));
                sndByteBlock[dst + 84] = (byte) l;
                sndByteBlock[dst + 85] = (byte) (l >> 8);
                sndByteBlock[dst + 86] = (byte) r;
                sndByteBlock[dst + 87] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 34 * OPM.opmBuffer[src +  56] + 91 * OPM.opmBuffer[src +  58]                             + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 34 * OPM.opmBuffer[src +  57] + 91 * OPM.opmBuffer[src +  59]                             + 64 >> 7));
                sndByteBlock[dst + 88] = (byte) l;
                sndByteBlock[dst + 89] = (byte) (l >> 8);
                sndByteBlock[dst + 90] = (byte) r;
                sndByteBlock[dst + 91] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,  5 * OPM.opmBuffer[src +  58] + 96 * OPM.opmBuffer[src +  60] + 24 * OPM.opmBuffer[src +  62] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,  5 * OPM.opmBuffer[src +  59] + 96 * OPM.opmBuffer[src +  61] + 24 * OPM.opmBuffer[src +  63] + 64 >> 7));
                sndByteBlock[dst + 92] = (byte) l;
                sndByteBlock[dst + 93] = (byte) (l >> 8);
                sndByteBlock[dst + 94] = (byte) r;
                sndByteBlock[dst + 95] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,                             72 * OPM.opmBuffer[src +  62] + 53 * OPM.opmBuffer[src +  64] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,                             72 * OPM.opmBuffer[src +  63] + 53 * OPM.opmBuffer[src +  65] + 64 >> 7));
                sndByteBlock[dst + 96] = (byte) l;
                sndByteBlock[dst + 97] = (byte) (l >> 8);
                sndByteBlock[dst + 98] = (byte) r;
                sndByteBlock[dst + 99] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 43 * OPM.opmBuffer[src +  64] + 82 * OPM.opmBuffer[src +  66]                             + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 43 * OPM.opmBuffer[src +  65] + 82 * OPM.opmBuffer[src +  67]                             + 64 >> 7));
                sndByteBlock[dst + 100] = (byte) l;
                sndByteBlock[dst + 101] = (byte) (l >> 8);
                sndByteBlock[dst + 102] = (byte) r;
                sndByteBlock[dst + 103] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 14 * OPM.opmBuffer[src +  66] + 96 * OPM.opmBuffer[src +  68] + 15 * OPM.opmBuffer[src +  70] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 14 * OPM.opmBuffer[src +  67] + 96 * OPM.opmBuffer[src +  69] + 15 * OPM.opmBuffer[src +  71] + 64 >> 7));
                sndByteBlock[dst + 104] = (byte) l;
                sndByteBlock[dst + 105] = (byte) (l >> 8);
                sndByteBlock[dst + 106] = (byte) r;
                sndByteBlock[dst + 107] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,                             81 * OPM.opmBuffer[src +  70] + 44 * OPM.opmBuffer[src +  72] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,                             81 * OPM.opmBuffer[src +  71] + 44 * OPM.opmBuffer[src +  73] + 64 >> 7));
                sndByteBlock[dst + 108] = (byte) l;
                sndByteBlock[dst + 109] = (byte) (l >> 8);
                sndByteBlock[dst + 110] = (byte) r;
                sndByteBlock[dst + 111] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 52 * OPM.opmBuffer[src +  72] + 73 * OPM.opmBuffer[src +  74]                             + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 52 * OPM.opmBuffer[src +  73] + 73 * OPM.opmBuffer[src +  75]                             + 64 >> 7));
                sndByteBlock[dst + 112] = (byte) l;
                sndByteBlock[dst + 113] = (byte) (l >> 8);
                sndByteBlock[dst + 114] = (byte) r;
                sndByteBlock[dst + 115] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 23 * OPM.opmBuffer[src +  74] + 96 * OPM.opmBuffer[src +  76] +  6 * OPM.opmBuffer[src +  78] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 23 * OPM.opmBuffer[src +  75] + 96 * OPM.opmBuffer[src +  77] +  6 * OPM.opmBuffer[src +  79] + 64 >> 7));
                sndByteBlock[dst + 116] = (byte) l;
                sndByteBlock[dst + 117] = (byte) (l >> 8);
                sndByteBlock[dst + 118] = (byte) r;
                sndByteBlock[dst + 119] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,                             90 * OPM.opmBuffer[src +  78] + 35 * OPM.opmBuffer[src +  80] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,                             90 * OPM.opmBuffer[src +  79] + 35 * OPM.opmBuffer[src +  81] + 64 >> 7));
                sndByteBlock[dst + 120] = (byte) l;
                sndByteBlock[dst + 121] = (byte) (l >> 8);
                sndByteBlock[dst + 122] = (byte) r;
                sndByteBlock[dst + 123] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 61 * OPM.opmBuffer[src +  80] + 64 * OPM.opmBuffer[src +  82]                             + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 61 * OPM.opmBuffer[src +  81] + 64 * OPM.opmBuffer[src +  83]                             + 64 >> 7));
                sndByteBlock[dst + 124] = (byte) l;
                sndByteBlock[dst + 125] = (byte) (l >> 8);
                sndByteBlock[dst + 126] = (byte) r;
                sndByteBlock[dst + 127] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 32 * OPM.opmBuffer[src +  82] + 93 * OPM.opmBuffer[src +  84]                             + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 32 * OPM.opmBuffer[src +  83] + 93 * OPM.opmBuffer[src +  85]                             + 64 >> 7));
                sndByteBlock[dst + 128] = (byte) l;
                sndByteBlock[dst + 129] = (byte) (l >> 8);
                sndByteBlock[dst + 130] = (byte) r;
                sndByteBlock[dst + 131] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,  3 * OPM.opmBuffer[src +  84] + 96 * OPM.opmBuffer[src +  86] + 26 * OPM.opmBuffer[src +  88] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,  3 * OPM.opmBuffer[src +  85] + 96 * OPM.opmBuffer[src +  87] + 26 * OPM.opmBuffer[src +  89] + 64 >> 7));
                sndByteBlock[dst + 132] = (byte) l;
                sndByteBlock[dst + 133] = (byte) (l >> 8);
                sndByteBlock[dst + 134] = (byte) r;
                sndByteBlock[dst + 135] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,                             70 * OPM.opmBuffer[src +  88] + 55 * OPM.opmBuffer[src +  90] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,                             70 * OPM.opmBuffer[src +  89] + 55 * OPM.opmBuffer[src +  91] + 64 >> 7));
                sndByteBlock[dst + 136] = (byte) l;
                sndByteBlock[dst + 137] = (byte) (l >> 8);
                sndByteBlock[dst + 138] = (byte) r;
                sndByteBlock[dst + 139] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 41 * OPM.opmBuffer[src +  90] + 84 * OPM.opmBuffer[src +  92]                             + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 41 * OPM.opmBuffer[src +  91] + 84 * OPM.opmBuffer[src +  93]                             + 64 >> 7));
                sndByteBlock[dst + 140] = (byte) l;
                sndByteBlock[dst + 141] = (byte) (l >> 8);
                sndByteBlock[dst + 142] = (byte) r;
                sndByteBlock[dst + 143] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 12 * OPM.opmBuffer[src +  92] + 96 * OPM.opmBuffer[src +  94] + 17 * OPM.opmBuffer[src +  96] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 12 * OPM.opmBuffer[src +  93] + 96 * OPM.opmBuffer[src +  95] + 17 * OPM.opmBuffer[src +  97] + 64 >> 7));
                sndByteBlock[dst + 144] = (byte) l;
                sndByteBlock[dst + 145] = (byte) (l >> 8);
                sndByteBlock[dst + 146] = (byte) r;
                sndByteBlock[dst + 147] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,                             79 * OPM.opmBuffer[src +  96] + 46 * OPM.opmBuffer[src +  98] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,                             79 * OPM.opmBuffer[src +  97] + 46 * OPM.opmBuffer[src +  99] + 64 >> 7));
                sndByteBlock[dst + 148] = (byte) l;
                sndByteBlock[dst + 149] = (byte) (l >> 8);
                sndByteBlock[dst + 150] = (byte) r;
                sndByteBlock[dst + 151] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 50 * OPM.opmBuffer[src +  98] + 75 * OPM.opmBuffer[src + 100]                             + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 50 * OPM.opmBuffer[src +  99] + 75 * OPM.opmBuffer[src + 101]                             + 64 >> 7));
                sndByteBlock[dst + 152] = (byte) l;
                sndByteBlock[dst + 153] = (byte) (l >> 8);
                sndByteBlock[dst + 154] = (byte) r;
                sndByteBlock[dst + 155] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 21 * OPM.opmBuffer[src + 100] + 96 * OPM.opmBuffer[src + 102] +  8 * OPM.opmBuffer[src + 104] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 21 * OPM.opmBuffer[src + 101] + 96 * OPM.opmBuffer[src + 103] +  8 * OPM.opmBuffer[src + 105] + 64 >> 7));
                sndByteBlock[dst + 156] = (byte) l;
                sndByteBlock[dst + 157] = (byte) (l >> 8);
                sndByteBlock[dst + 158] = (byte) r;
                sndByteBlock[dst + 159] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,                             88 * OPM.opmBuffer[src + 104] + 37 * OPM.opmBuffer[src + 106] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,                             88 * OPM.opmBuffer[src + 105] + 37 * OPM.opmBuffer[src + 107] + 64 >> 7));
                sndByteBlock[dst + 160] = (byte) l;
                sndByteBlock[dst + 161] = (byte) (l >> 8);
                sndByteBlock[dst + 162] = (byte) r;
                sndByteBlock[dst + 163] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 59 * OPM.opmBuffer[src + 106] + 66 * OPM.opmBuffer[src + 108]                             + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 59 * OPM.opmBuffer[src + 107] + 66 * OPM.opmBuffer[src + 109]                             + 64 >> 7));
                sndByteBlock[dst + 164] = (byte) l;
                sndByteBlock[dst + 165] = (byte) (l >> 8);
                sndByteBlock[dst + 166] = (byte) r;
                sndByteBlock[dst + 167] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 30 * OPM.opmBuffer[src + 108] + 95 * OPM.opmBuffer[src + 110]                             + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 30 * OPM.opmBuffer[src + 109] + 95 * OPM.opmBuffer[src + 111]                             + 64 >> 7));
                sndByteBlock[dst + 168] = (byte) l;
                sndByteBlock[dst + 169] = (byte) (l >> 8);
                sndByteBlock[dst + 170] = (byte) r;
                sndByteBlock[dst + 171] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,      OPM.opmBuffer[src + 110] + 96 * OPM.opmBuffer[src + 112] + 28 * OPM.opmBuffer[src + 114] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,      OPM.opmBuffer[src + 111] + 96 * OPM.opmBuffer[src + 113] + 28 * OPM.opmBuffer[src + 115] + 64 >> 7));
                sndByteBlock[dst + 172] = (byte) l;
                sndByteBlock[dst + 173] = (byte) (l >> 8);
                sndByteBlock[dst + 174] = (byte) r;
                sndByteBlock[dst + 175] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,                             68 * OPM.opmBuffer[src + 114] + 57 * OPM.opmBuffer[src + 116] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,                             68 * OPM.opmBuffer[src + 115] + 57 * OPM.opmBuffer[src + 117] + 64 >> 7));
                sndByteBlock[dst + 176] = (byte) l;
                sndByteBlock[dst + 177] = (byte) (l >> 8);
                sndByteBlock[dst + 178] = (byte) r;
                sndByteBlock[dst + 179] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 39 * OPM.opmBuffer[src + 116] + 86 * OPM.opmBuffer[src + 118]                             + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 39 * OPM.opmBuffer[src + 117] + 86 * OPM.opmBuffer[src + 119]                             + 64 >> 7));
                sndByteBlock[dst + 180] = (byte) l;
                sndByteBlock[dst + 181] = (byte) (l >> 8);
                sndByteBlock[dst + 182] = (byte) r;
                sndByteBlock[dst + 183] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 10 * OPM.opmBuffer[src + 118] + 96 * OPM.opmBuffer[src + 120] + 19 * OPM.opmBuffer[src + 122] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 10 * OPM.opmBuffer[src + 119] + 96 * OPM.opmBuffer[src + 121] + 19 * OPM.opmBuffer[src + 123] + 64 >> 7));
                sndByteBlock[dst + 184] = (byte) l;
                sndByteBlock[dst + 185] = (byte) (l >> 8);
                sndByteBlock[dst + 186] = (byte) r;
                sndByteBlock[dst + 187] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,                             77 * OPM.opmBuffer[src + 122] + 48 * OPM.opmBuffer[src + 124] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,                             77 * OPM.opmBuffer[src + 123] + 48 * OPM.opmBuffer[src + 125] + 64 >> 7));
                sndByteBlock[dst + 188] = (byte) l;
                sndByteBlock[dst + 189] = (byte) (l >> 8);
                sndByteBlock[dst + 190] = (byte) r;
                sndByteBlock[dst + 191] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 48 * OPM.opmBuffer[src + 124] + 77 * OPM.opmBuffer[src + 126]                             + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 48 * OPM.opmBuffer[src + 125] + 77 * OPM.opmBuffer[src + 127]                             + 64 >> 7));
                sndByteBlock[dst + 192] = (byte) l;
                sndByteBlock[dst + 193] = (byte) (l >> 8);
                sndByteBlock[dst + 194] = (byte) r;
                sndByteBlock[dst + 195] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 19 * OPM.opmBuffer[src + 126] + 96 * OPM.opmBuffer[src + 128] + 10 * OPM.opmBuffer[src + 130] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 19 * OPM.opmBuffer[src + 127] + 96 * OPM.opmBuffer[src + 129] + 10 * OPM.opmBuffer[src + 131] + 64 >> 7));
                sndByteBlock[dst + 196] = (byte) l;
                sndByteBlock[dst + 197] = (byte) (l >> 8);
                sndByteBlock[dst + 198] = (byte) r;
                sndByteBlock[dst + 199] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,                             86 * OPM.opmBuffer[src + 130] + 39 * OPM.opmBuffer[src + 132] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,                             86 * OPM.opmBuffer[src + 131] + 39 * OPM.opmBuffer[src + 133] + 64 >> 7));
                sndByteBlock[dst + 200] = (byte) l;
                sndByteBlock[dst + 201] = (byte) (l >> 8);
                sndByteBlock[dst + 202] = (byte) r;
                sndByteBlock[dst + 203] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 57 * OPM.opmBuffer[src + 132] + 68 * OPM.opmBuffer[src + 134]                             + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 57 * OPM.opmBuffer[src + 133] + 68 * OPM.opmBuffer[src + 135]                             + 64 >> 7));
                sndByteBlock[dst + 204] = (byte) l;
                sndByteBlock[dst + 205] = (byte) (l >> 8);
                sndByteBlock[dst + 206] = (byte) r;
                sndByteBlock[dst + 207] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 28 * OPM.opmBuffer[src + 134] + 96 * OPM.opmBuffer[src + 136] +      OPM.opmBuffer[src + 138] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 28 * OPM.opmBuffer[src + 135] + 96 * OPM.opmBuffer[src + 137] +      OPM.opmBuffer[src + 139] + 64 >> 7));
                sndByteBlock[dst + 208] = (byte) l;
                sndByteBlock[dst + 209] = (byte) (l >> 8);
                sndByteBlock[dst + 210] = (byte) r;
                sndByteBlock[dst + 211] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,                             95 * OPM.opmBuffer[src + 138] + 30 * OPM.opmBuffer[src + 140] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,                             95 * OPM.opmBuffer[src + 139] + 30 * OPM.opmBuffer[src + 141] + 64 >> 7));
                sndByteBlock[dst + 212] = (byte) l;
                sndByteBlock[dst + 213] = (byte) (l >> 8);
                sndByteBlock[dst + 214] = (byte) r;
                sndByteBlock[dst + 215] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,                             66 * OPM.opmBuffer[src + 140] + 59 * OPM.opmBuffer[src + 142] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,                             66 * OPM.opmBuffer[src + 141] + 59 * OPM.opmBuffer[src + 143] + 64 >> 7));
                sndByteBlock[dst + 216] = (byte) l;
                sndByteBlock[dst + 217] = (byte) (l >> 8);
                sndByteBlock[dst + 218] = (byte) r;
                sndByteBlock[dst + 219] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 37 * OPM.opmBuffer[src + 142] + 88 * OPM.opmBuffer[src + 144]                             + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 37 * OPM.opmBuffer[src + 143] + 88 * OPM.opmBuffer[src + 145]                             + 64 >> 7));
                sndByteBlock[dst + 220] = (byte) l;
                sndByteBlock[dst + 221] = (byte) (l >> 8);
                sndByteBlock[dst + 222] = (byte) r;
                sndByteBlock[dst + 223] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,  8 * OPM.opmBuffer[src + 144] + 96 * OPM.opmBuffer[src + 146] + 21 * OPM.opmBuffer[src + 148] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,  8 * OPM.opmBuffer[src + 145] + 96 * OPM.opmBuffer[src + 147] + 21 * OPM.opmBuffer[src + 149] + 64 >> 7));
                sndByteBlock[dst + 224] = (byte) l;
                sndByteBlock[dst + 225] = (byte) (l >> 8);
                sndByteBlock[dst + 226] = (byte) r;
                sndByteBlock[dst + 227] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,                             75 * OPM.opmBuffer[src + 148] + 50 * OPM.opmBuffer[src + 150] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,                             75 * OPM.opmBuffer[src + 149] + 50 * OPM.opmBuffer[src + 151] + 64 >> 7));
                sndByteBlock[dst + 228] = (byte) l;
                sndByteBlock[dst + 229] = (byte) (l >> 8);
                sndByteBlock[dst + 230] = (byte) r;
                sndByteBlock[dst + 231] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 46 * OPM.opmBuffer[src + 150] + 79 * OPM.opmBuffer[src + 152]                             + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 46 * OPM.opmBuffer[src + 151] + 79 * OPM.opmBuffer[src + 153]                             + 64 >> 7));
                sndByteBlock[dst + 232] = (byte) l;
                sndByteBlock[dst + 233] = (byte) (l >> 8);
                sndByteBlock[dst + 234] = (byte) r;
                sndByteBlock[dst + 235] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 17 * OPM.opmBuffer[src + 152] + 96 * OPM.opmBuffer[src + 154] + 12 * OPM.opmBuffer[src + 156] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 17 * OPM.opmBuffer[src + 153] + 96 * OPM.opmBuffer[src + 155] + 12 * OPM.opmBuffer[src + 157] + 64 >> 7));
                sndByteBlock[dst + 236] = (byte) l;
                sndByteBlock[dst + 237] = (byte) (l >> 8);
                sndByteBlock[dst + 238] = (byte) r;
                sndByteBlock[dst + 239] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,                             84 * OPM.opmBuffer[src + 156] + 41 * OPM.opmBuffer[src + 158] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,                             84 * OPM.opmBuffer[src + 157] + 41 * OPM.opmBuffer[src + 159] + 64 >> 7));
                sndByteBlock[dst + 240] = (byte) l;
                sndByteBlock[dst + 241] = (byte) (l >> 8);
                sndByteBlock[dst + 242] = (byte) r;
                sndByteBlock[dst + 243] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 55 * OPM.opmBuffer[src + 158] + 70 * OPM.opmBuffer[src + 160]                             + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 55 * OPM.opmBuffer[src + 159] + 70 * OPM.opmBuffer[src + 161]                             + 64 >> 7));
                sndByteBlock[dst + 244] = (byte) l;
                sndByteBlock[dst + 245] = (byte) (l >> 8);
                sndByteBlock[dst + 246] = (byte) r;
                sndByteBlock[dst + 247] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 26 * OPM.opmBuffer[src + 160] + 96 * OPM.opmBuffer[src + 162] +  3 * OPM.opmBuffer[src + 164] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 26 * OPM.opmBuffer[src + 161] + 96 * OPM.opmBuffer[src + 163] +  3 * OPM.opmBuffer[src + 165] + 64 >> 7));
                sndByteBlock[dst + 248] = (byte) l;
                sndByteBlock[dst + 249] = (byte) (l >> 8);
                sndByteBlock[dst + 250] = (byte) r;
                sndByteBlock[dst + 251] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,                             93 * OPM.opmBuffer[src + 164] + 32 * OPM.opmBuffer[src + 166] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,                             93 * OPM.opmBuffer[src + 165] + 32 * OPM.opmBuffer[src + 167] + 64 >> 7));
                sndByteBlock[dst + 252] = (byte) l;
                sndByteBlock[dst + 253] = (byte) (l >> 8);
                sndByteBlock[dst + 254] = (byte) r;
                sndByteBlock[dst + 255] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,                             64 * OPM.opmBuffer[src + 166] + 61 * OPM.opmBuffer[src + 168] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,                             64 * OPM.opmBuffer[src + 167] + 61 * OPM.opmBuffer[src + 169] + 64 >> 7));
                sndByteBlock[dst + 256] = (byte) l;
                sndByteBlock[dst + 257] = (byte) (l >> 8);
                sndByteBlock[dst + 258] = (byte) r;
                sndByteBlock[dst + 259] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 35 * OPM.opmBuffer[src + 168] + 90 * OPM.opmBuffer[src + 170]                             + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 35 * OPM.opmBuffer[src + 169] + 90 * OPM.opmBuffer[src + 171]                             + 64 >> 7));
                sndByteBlock[dst + 260] = (byte) l;
                sndByteBlock[dst + 261] = (byte) (l >> 8);
                sndByteBlock[dst + 262] = (byte) r;
                sndByteBlock[dst + 263] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,  6 * OPM.opmBuffer[src + 170] + 96 * OPM.opmBuffer[src + 172] + 23 * OPM.opmBuffer[src + 174] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,  6 * OPM.opmBuffer[src + 171] + 96 * OPM.opmBuffer[src + 173] + 23 * OPM.opmBuffer[src + 175] + 64 >> 7));
                sndByteBlock[dst + 264] = (byte) l;
                sndByteBlock[dst + 265] = (byte) (l >> 8);
                sndByteBlock[dst + 266] = (byte) r;
                sndByteBlock[dst + 267] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,                             73 * OPM.opmBuffer[src + 174] + 52 * OPM.opmBuffer[src + 176] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,                             73 * OPM.opmBuffer[src + 175] + 52 * OPM.opmBuffer[src + 177] + 64 >> 7));
                sndByteBlock[dst + 268] = (byte) l;
                sndByteBlock[dst + 269] = (byte) (l >> 8);
                sndByteBlock[dst + 270] = (byte) r;
                sndByteBlock[dst + 271] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 44 * OPM.opmBuffer[src + 176] + 81 * OPM.opmBuffer[src + 178]                             + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 44 * OPM.opmBuffer[src + 177] + 81 * OPM.opmBuffer[src + 179]                             + 64 >> 7));
                sndByteBlock[dst + 272] = (byte) l;
                sndByteBlock[dst + 273] = (byte) (l >> 8);
                sndByteBlock[dst + 274] = (byte) r;
                sndByteBlock[dst + 275] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 15 * OPM.opmBuffer[src + 178] + 96 * OPM.opmBuffer[src + 180] + 14 * OPM.opmBuffer[src + 182] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 15 * OPM.opmBuffer[src + 179] + 96 * OPM.opmBuffer[src + 181] + 14 * OPM.opmBuffer[src + 183] + 64 >> 7));
                sndByteBlock[dst + 276] = (byte) l;
                sndByteBlock[dst + 277] = (byte) (l >> 8);
                sndByteBlock[dst + 278] = (byte) r;
                sndByteBlock[dst + 279] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,                             82 * OPM.opmBuffer[src + 182] + 43 * OPM.opmBuffer[src + 184] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,                             82 * OPM.opmBuffer[src + 183] + 43 * OPM.opmBuffer[src + 185] + 64 >> 7));
                sndByteBlock[dst + 280] = (byte) l;
                sndByteBlock[dst + 281] = (byte) (l >> 8);
                sndByteBlock[dst + 282] = (byte) r;
                sndByteBlock[dst + 283] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 53 * OPM.opmBuffer[src + 184] + 72 * OPM.opmBuffer[src + 186]                             + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 53 * OPM.opmBuffer[src + 185] + 72 * OPM.opmBuffer[src + 187]                             + 64 >> 7));
                sndByteBlock[dst + 284] = (byte) l;
                sndByteBlock[dst + 285] = (byte) (l >> 8);
                sndByteBlock[dst + 286] = (byte) r;
                sndByteBlock[dst + 287] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 24 * OPM.opmBuffer[src + 186] + 96 * OPM.opmBuffer[src + 188] +  5 * OPM.opmBuffer[src + 190] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 24 * OPM.opmBuffer[src + 187] + 96 * OPM.opmBuffer[src + 189] +  5 * OPM.opmBuffer[src + 191] + 64 >> 7));
                sndByteBlock[dst + 288] = (byte) l;
                sndByteBlock[dst + 289] = (byte) (l >> 8);
                sndByteBlock[dst + 290] = (byte) r;
                sndByteBlock[dst + 291] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,                             91 * OPM.opmBuffer[src + 190] + 34 * OPM.opmBuffer[src + 192] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,                             91 * OPM.opmBuffer[src + 191] + 34 * OPM.opmBuffer[src + 193] + 64 >> 7));
                sndByteBlock[dst + 292] = (byte) l;
                sndByteBlock[dst + 293] = (byte) (l >> 8);
                sndByteBlock[dst + 294] = (byte) r;
                sndByteBlock[dst + 295] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 62 * OPM.opmBuffer[src + 192] + 63 * OPM.opmBuffer[src + 194]                             + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 62 * OPM.opmBuffer[src + 193] + 63 * OPM.opmBuffer[src + 195]                             + 64 >> 7));
                sndByteBlock[dst + 296] = (byte) l;
                sndByteBlock[dst + 297] = (byte) (l >> 8);
                sndByteBlock[dst + 298] = (byte) r;
                sndByteBlock[dst + 299] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 33 * OPM.opmBuffer[src + 194] + 92 * OPM.opmBuffer[src + 196]                             + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 33 * OPM.opmBuffer[src + 195] + 92 * OPM.opmBuffer[src + 197]                             + 64 >> 7));
                sndByteBlock[dst + 300] = (byte) l;
                sndByteBlock[dst + 301] = (byte) (l >> 8);
                sndByteBlock[dst + 302] = (byte) r;
                sndByteBlock[dst + 303] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,  4 * OPM.opmBuffer[src + 196] + 96 * OPM.opmBuffer[src + 198] + 25 * OPM.opmBuffer[src + 200] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,  4 * OPM.opmBuffer[src + 197] + 96 * OPM.opmBuffer[src + 199] + 25 * OPM.opmBuffer[src + 201] + 64 >> 7));
                sndByteBlock[dst + 304] = (byte) l;
                sndByteBlock[dst + 305] = (byte) (l >> 8);
                sndByteBlock[dst + 306] = (byte) r;
                sndByteBlock[dst + 307] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,                             71 * OPM.opmBuffer[src + 200] + 54 * OPM.opmBuffer[src + 202] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,                             71 * OPM.opmBuffer[src + 201] + 54 * OPM.opmBuffer[src + 203] + 64 >> 7));
                sndByteBlock[dst + 308] = (byte) l;
                sndByteBlock[dst + 309] = (byte) (l >> 8);
                sndByteBlock[dst + 310] = (byte) r;
                sndByteBlock[dst + 311] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 42 * OPM.opmBuffer[src + 202] + 83 * OPM.opmBuffer[src + 204]                             + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 42 * OPM.opmBuffer[src + 203] + 83 * OPM.opmBuffer[src + 205]                             + 64 >> 7));
                sndByteBlock[dst + 312] = (byte) l;
                sndByteBlock[dst + 313] = (byte) (l >> 8);
                sndByteBlock[dst + 314] = (byte) r;
                sndByteBlock[dst + 315] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 13 * OPM.opmBuffer[src + 204] + 96 * OPM.opmBuffer[src + 206] + 16 * OPM.opmBuffer[src + 208] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 13 * OPM.opmBuffer[src + 205] + 96 * OPM.opmBuffer[src + 207] + 16 * OPM.opmBuffer[src + 209] + 64 >> 7));
                sndByteBlock[dst + 316] = (byte) l;
                sndByteBlock[dst + 317] = (byte) (l >> 8);
                sndByteBlock[dst + 318] = (byte) r;
                sndByteBlock[dst + 319] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,                             80 * OPM.opmBuffer[src + 208] + 45 * OPM.opmBuffer[src + 210] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,                             80 * OPM.opmBuffer[src + 209] + 45 * OPM.opmBuffer[src + 211] + 64 >> 7));
                sndByteBlock[dst + 320] = (byte) l;
                sndByteBlock[dst + 321] = (byte) (l >> 8);
                sndByteBlock[dst + 322] = (byte) r;
                sndByteBlock[dst + 323] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 51 * OPM.opmBuffer[src + 210] + 74 * OPM.opmBuffer[src + 212]                             + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 51 * OPM.opmBuffer[src + 211] + 74 * OPM.opmBuffer[src + 213]                             + 64 >> 7));
                sndByteBlock[dst + 324] = (byte) l;
                sndByteBlock[dst + 325] = (byte) (l >> 8);
                sndByteBlock[dst + 326] = (byte) r;
                sndByteBlock[dst + 327] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 22 * OPM.opmBuffer[src + 212] + 96 * OPM.opmBuffer[src + 214] +  7 * OPM.opmBuffer[src + 216] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 22 * OPM.opmBuffer[src + 213] + 96 * OPM.opmBuffer[src + 215] +  7 * OPM.opmBuffer[src + 217] + 64 >> 7));
                sndByteBlock[dst + 328] = (byte) l;
                sndByteBlock[dst + 329] = (byte) (l >> 8);
                sndByteBlock[dst + 330] = (byte) r;
                sndByteBlock[dst + 331] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,                             89 * OPM.opmBuffer[src + 216] + 36 * OPM.opmBuffer[src + 218] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,                             89 * OPM.opmBuffer[src + 217] + 36 * OPM.opmBuffer[src + 219] + 64 >> 7));
                sndByteBlock[dst + 332] = (byte) l;
                sndByteBlock[dst + 333] = (byte) (l >> 8);
                sndByteBlock[dst + 334] = (byte) r;
                sndByteBlock[dst + 335] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 60 * OPM.opmBuffer[src + 218] + 65 * OPM.opmBuffer[src + 220]                             + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 60 * OPM.opmBuffer[src + 219] + 65 * OPM.opmBuffer[src + 221]                             + 64 >> 7));
                sndByteBlock[dst + 336] = (byte) l;
                sndByteBlock[dst + 337] = (byte) (l >> 8);
                sndByteBlock[dst + 338] = (byte) r;
                sndByteBlock[dst + 339] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 31 * OPM.opmBuffer[src + 220] + 94 * OPM.opmBuffer[src + 222]                             + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 31 * OPM.opmBuffer[src + 221] + 94 * OPM.opmBuffer[src + 223]                             + 64 >> 7));
                sndByteBlock[dst + 340] = (byte) l;
                sndByteBlock[dst + 341] = (byte) (l >> 8);
                sndByteBlock[dst + 342] = (byte) r;
                sndByteBlock[dst + 343] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,  2 * OPM.opmBuffer[src + 222] + 96 * OPM.opmBuffer[src + 224] + 27 * OPM.opmBuffer[src + 226] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,  2 * OPM.opmBuffer[src + 223] + 96 * OPM.opmBuffer[src + 225] + 27 * OPM.opmBuffer[src + 227] + 64 >> 7));
                sndByteBlock[dst + 344] = (byte) l;
                sndByteBlock[dst + 345] = (byte) (l >> 8);
                sndByteBlock[dst + 346] = (byte) r;
                sndByteBlock[dst + 347] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,                             69 * OPM.opmBuffer[src + 226] + 56 * OPM.opmBuffer[src + 228] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,                             69 * OPM.opmBuffer[src + 227] + 56 * OPM.opmBuffer[src + 229] + 64 >> 7));
                sndByteBlock[dst + 348] = (byte) l;
                sndByteBlock[dst + 349] = (byte) (l >> 8);
                sndByteBlock[dst + 350] = (byte) r;
                sndByteBlock[dst + 351] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 40 * OPM.opmBuffer[src + 228] + 85 * OPM.opmBuffer[src + 230]                             + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 40 * OPM.opmBuffer[src + 229] + 85 * OPM.opmBuffer[src + 231]                             + 64 >> 7));
                sndByteBlock[dst + 352] = (byte) l;
                sndByteBlock[dst + 353] = (byte) (l >> 8);
                sndByteBlock[dst + 354] = (byte) r;
                sndByteBlock[dst + 355] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 11 * OPM.opmBuffer[src + 230] + 96 * OPM.opmBuffer[src + 232] + 18 * OPM.opmBuffer[src + 234] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 11 * OPM.opmBuffer[src + 231] + 96 * OPM.opmBuffer[src + 233] + 18 * OPM.opmBuffer[src + 235] + 64 >> 7));
                sndByteBlock[dst + 356] = (byte) l;
                sndByteBlock[dst + 357] = (byte) (l >> 8);
                sndByteBlock[dst + 358] = (byte) r;
                sndByteBlock[dst + 359] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,                             78 * OPM.opmBuffer[src + 234] + 47 * OPM.opmBuffer[src + 236] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,                             78 * OPM.opmBuffer[src + 235] + 47 * OPM.opmBuffer[src + 237] + 64 >> 7));
                sndByteBlock[dst + 360] = (byte) l;
                sndByteBlock[dst + 361] = (byte) (l >> 8);
                sndByteBlock[dst + 362] = (byte) r;
                sndByteBlock[dst + 363] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 49 * OPM.opmBuffer[src + 236] + 76 * OPM.opmBuffer[src + 238]                             + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 49 * OPM.opmBuffer[src + 237] + 76 * OPM.opmBuffer[src + 239]                             + 64 >> 7));
                sndByteBlock[dst + 364] = (byte) l;
                sndByteBlock[dst + 365] = (byte) (l >> 8);
                sndByteBlock[dst + 366] = (byte) r;
                sndByteBlock[dst + 367] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 20 * OPM.opmBuffer[src + 238] + 96 * OPM.opmBuffer[src + 240] +  9 * OPM.opmBuffer[src + 242] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 20 * OPM.opmBuffer[src + 239] + 96 * OPM.opmBuffer[src + 241] +  9 * OPM.opmBuffer[src + 243] + 64 >> 7));
                sndByteBlock[dst + 368] = (byte) l;
                sndByteBlock[dst + 369] = (byte) (l >> 8);
                sndByteBlock[dst + 370] = (byte) r;
                sndByteBlock[dst + 371] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767,                             87 * OPM.opmBuffer[src + 242] + 38 * OPM.opmBuffer[src + 244] + 64 >> 7));
                r = Math.max (-32768, Math.min (32767,                             87 * OPM.opmBuffer[src + 243] + 38 * OPM.opmBuffer[src + 245] + 64 >> 7));
                sndByteBlock[dst + 372] = (byte) l;
                sndByteBlock[dst + 373] = (byte) (l >> 8);
                sndByteBlock[dst + 374] = (byte) r;
                sndByteBlock[dst + 375] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 58 * OPM.opmBuffer[src + 244] + 67 * OPM.opmBuffer[src + 246]                             + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 58 * OPM.opmBuffer[src + 245] + 67 * OPM.opmBuffer[src + 247]                             + 64 >> 7));
                sndByteBlock[dst + 376] = (byte) l;
                sndByteBlock[dst + 377] = (byte) (l >> 8);
                sndByteBlock[dst + 378] = (byte) r;
                sndByteBlock[dst + 379] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
                l = Math.max (-32768, Math.min (32767, 29 * OPM.opmBuffer[src + 246] + 96 * OPM.opmBuffer[src + 248]                             + 64 >> 7));
                r = Math.max (-32768, Math.min (32767, 29 * OPM.opmBuffer[src + 247] + 96 * OPM.opmBuffer[src + 249]                             + 64 >> 7));
                sndByteBlock[dst + 380] = (byte) l;
                sndByteBlock[dst + 381] = (byte) (l >> 8);
                sndByteBlock[dst + 382] = (byte) r;
                sndByteBlock[dst + 383] = (byte) (r >> 8);
                sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
        }  //for src,dst
      }  //convert(int)
    },  //SNDRateConverter.CONSTANT_AREA_STEREO_48000

    //線形面積補間
    //  S44PLAY.Xの-sqの方法
    //    S44PLAY.Xで-hqよりも-sqのほうが綺麗に聞こえた(ような気がした)のは線形補間よりも面積補間の方がローパスフィルタとしての性能が高かったからかも知れない
    //  入力データを線形補間した波形を出力サンプリング間隔で刻み、各断片の平均の変位(断片の面積に比例する)を出力データとする
    //  入力サンプリング位置における入力データと出力サンプリング位置における線形補間された入力データを台形の上辺または下辺と見なすと、
    //  線形補間された入力データを出力サンプリング間隔で刻んだ各断片は複数の台形を繋ぎ合わせた形になる
    //  台形の面積は(上辺+下辺)×高さ÷2であるから、断片の面積は近隣の入力データの線形結合で表現できる
    //  出力周波数が入力周波数の1/2倍～1倍のとき、1個の出力データを求めるために3個または4個の入力データが必要になる
    //  末尾のデータを作るために次のブロックの先頭のデータが必要になるが、代わりに前のブロックの末尾のデータをブロックの先頭に加える
    //  OPMとPCMを合成するときにずらせばオーバーヘッドは最小限で済む
    //  7Hz→5Hzの場合
    //    a....b....c....d....e....f....g....h
    //    AAAAAAABBBBBBBCCCCCCCDDDDDDDEEEEEEE
    //      A=(( a + b )* 5 /2+( b + (3*b+2*c)/5 )* 2 /2)/7                           = 5/14*a + 41/70*b + 2/35*c
    //      B=(( (3*b+2*c)/5 + c )* 3 /2+( c + (1*c+4*d)/5 )* 4 /2)/7                 = 9/70*b + 9/14*c + 8/35*d
    //      C=(( (1*c+4*d)/5 + d )* 1 /2+( d + e )* 5 /2+( e + (4*e+1*f)/5 )* 1 /2)/7 = 1/70*c + 17/35*d + 17/35*e + 1/70*f
    //      D=(( (4*e+1*f)/5 + f )* 4 /2+( f + (2*f+3*g)/5 )* 3 /2)/7                 = 8/35*e + 9/14*f + 9/70*g
    //      E=(( (2*f+3*g)/5 + g )* 2 /2+( g + h )* 5 /2)/7                           = 2/35*f + 41/70*g + 5/14*h
    //    perl -e "$m=7;$n=5;@w=();for$i(0..$m){$k=$n*$i;push@w,[$k,'s['.$i.']']}for$j(0..$n-1){$k=$m*$j;$r=$k%$n;$r or next;$q=($k-$r)/$n;push@w,[$k,'('.($n-$r).'*s['.$q.']+'.$r.'*s['.($q+1).'])/'.$n]}@w=sort{$a->[0]<=>$b->[0]}@w;for$j(0..$n-1){$k=$m*$j;@v=grep{$k<=$_->[0]&&$_->[0]<=$k+$m}@w;@d=();for$i(0..@v-2){push@d,'('.$v[$i]->[1].'+'.$v[$i+1]->[1].')*'.($v[$i+1]->[0]-$v[$i]->[0]).'/2'}printf'    //      d[%d]=(%s)/%d;%c',$j,join('+',@d),$m,10}"
    //      d[0]=((s[0]+s[1])*5/2+(s[1]+(3*s[1]+2*s[2])/5)*2/2)/7;
    //      d[1]=(((3*s[1]+2*s[2])/5+s[2])*3/2+(s[2]+(1*s[2]+4*s[3])/5)*4/2)/7;
    //      d[2]=(((1*s[2]+4*s[3])/5+s[3])*1/2+(s[3]+s[4])*5/2+(s[4]+(4*s[4]+1*s[5])/5)*1/2)/7;
    //      d[3]=(((4*s[4]+1*s[5])/5+s[5])*4/2+(s[5]+(2*s[5]+3*s[6])/5)*3/2)/7;
    //      d[4]=(((2*s[5]+3*s[6])/5+s[6])*2/2+(s[6]+s[7])*5/2)/7;
    //    perl -e "$m=7;$n=5;sub str{my($t)=@_;my@s=();for my$k(sort{$a<=>$b}keys%$t){push@s,$t->{$k}.'*s['.$k.']'}join'+',@s}sub mul{my($t,$m)=@_;my$p={};for my$k(keys%$t){$p->{$k}=$t->{$k}*$m}$p}sub add{my($t,$u)=@_;my$s={};for my$k(keys%$t){$s->{$k}=$t->{$k}};for my$k(keys%$u){$s->{$k}=($s->{$k}//0)+$u->{$k}}$s}@w=();for$i(0..$m){$k=$n*$i;$t={};$t->{$i}=1;push@w,[$k,$t]}for$j(0..$n-1){$k=$m*$j;$r=$k%$n;$r or next;$q=($k-$r)/$n;$t={};$t->{$q}=($n-$r)/$n;$t->{$q+1}=$r/$n;push@w,[$k,$t];}@w=sort{$a->[0]<=>$b->[0]}@w;for$j(0..$n-1){$k=$m*$j;@v=grep{$k<=$_->[0]&&$_->[0]<=$k+$m}@w;$d={};for$i(0..@v-2){$d=add($d,mul(add($v[$i]->[1],$v[$i+1]->[1]),($v[$i+1]->[0]-$v[$i]->[0])))}$d=mul($d,1/(2*$m));printf'    //      d[%d]=%s;%c',$j,str($d),10}"
    //      d[0]=0.357142857142857*s[0]+0.585714285714286*s[1]+0.0571428571428571*s[2];
    //      d[1]=0.128571428571429*s[1]+0.642857142857143*s[2]+0.228571428571429*s[3];
    //      d[2]=0.0142857142857143*s[2]+0.485714285714286*s[3]+0.485714285714286*s[4]+0.0142857142857143*s[5];
    //      d[3]=0.228571428571429*s[4]+0.642857142857143*s[5]+0.128571428571429*s[6];
    //      d[4]=0.0571428571428571*s[5]+0.585714285714286*s[6]+0.357142857142857*s[7];

    //線形面積補間,ステレオ,48kHz
    LINEAR_AREA_STEREO_48000 {
      @Override public void convert (int outputSamples) {
        if (false) {  //long
          //OPMとPCMを合わせてボリュームを掛ける。ついでに後ろに1サンプルずらす
          int l = OPM.opmBuffer[2 * OPM.OPM_BLOCK_SAMPLES    ];  //前回の最後のデータ
          int r = OPM.opmBuffer[2 * OPM.OPM_BLOCK_SAMPLES + 1];
          for (int src = 0; src < 2 * OPM.OPM_BLOCK_SAMPLES; src += 2) {
            int t;
            t = (((OPM.OPM_ON ? OPM.opmBuffer[src] : 0) +
                  (ADPCM.PCM_ON ? ADPCM.pcmBuffer[src] : 0) +
                  (MercuryUnit.MU4_ON ? MercuryUnit.mu4Buffer[src] : 0)) * sndCurrentScale) >> 12;
            OPM.opmBuffer[src] = l;
            l = t;
            t = (((OPM.OPM_ON ? OPM.opmBuffer[src + 1] : 0) +
                  (ADPCM.PCM_ON ? ADPCM.pcmBuffer[src + 1] : 0) +
                  (MercuryUnit.MU4_ON ? MercuryUnit.mu4Buffer[src + 1] : 0)) * sndCurrentScale) >> 12;
            OPM.opmBuffer[src + 1] = r;
            r = t;
            sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
          }
          OPM.opmBuffer[2 * OPM.OPM_BLOCK_SAMPLES    ] = l;  //今回の最後のデータ
          OPM.opmBuffer[2 * OPM.OPM_BLOCK_SAMPLES + 1] = r;
          //線形面積補間(long)
          //  通分したときの分母は2*125*96=24000だが、24000*11=264000≒262144=1<<18なので、
          //  24000で割る代わりに係数を11倍して除算を右18bitシフトで済ませている
          //  本来の値よりもわずかに大きい値(264000/242144倍)が出力される
          for (int src = 0, dst = 0; src < 2 * OPM.OPM_BLOCK_SAMPLES; src += 2 * 125, dst += 4 * 96) {
            int t;
            long u, v;
            //  perl -e "$m=125;$n=96;$CH=2;$I=12;sub str{my($t,$lr)=@_;my@s=();for my$k(sort{$a<=>$b}keys%$t){push@s,$t->{$k}.'L * OPM.opmBuffer[src'.($CH*$k+$lr?' + '.($CH*$k+$lr):'').']'}join' + ',@s}sub mul{my($t,$m)=@_;my$p={};for my$k(keys%$t){$p->{$k}=$t->{$k}*$m}$p}sub add{my($t,$u)=@_;my$s={};for my$k(keys%$t){$s->{$k}=$t->{$k}};for my$k(keys%$u){$s->{$k}=($s->{$k}//0)+$u->{$k}}$s}@out=();@w=();for$i(0..$m){$k=$n*$i;$t={};$t->{$i}=1;push@w,[$k,$t]}for$j(0..$n-1){$k=$m*$j;$r=$k%$n;$r or next;$q=($k-$r)/$n;$t={};$t->{$q}=($n-$r)/$n;$t->{$q+1}=$r/$n;push@w,[$k,$t];}@w=sort{$a->[0]<=>$b->[0]}@w;for$lr(0..$CH-1){push@out,sprintf'%*s//%s%c',$I,'',$CH==1?'mid':$lr?'right':'left',10;for$j(0..$n-1){$k=$m*$j;@v=grep{$k<=$_->[0]&&$_->[0]<=$k+$m}@w;$d={};for$i(0..@v-2){$d=add($d,mul(add($v[$i]->[1],$v[$i+1]->[1]),($v[$i+1]->[0]-$v[$i]->[0])))}$d=mul($d,11*$n);push@out,sprintf'%*st = Math.max (-32768, Math.min (32767, (int) (%s + 131072L >> 18)));%c',$I,'',str($d,$lr),10;push@out,sprintf'%*ssndByteBlock[dst'.(2*($CH*$j+$lr)+0?' + '.(2*($CH*$j+$lr)+0):'').'] = (byte) t;%c',$I,'',10;push@out,sprintf'%*ssndByteBlock[dst + '.(2*($CH*$j+$lr)+1).'] = (byte) (t >> 8);%c',$I,'',10;}}$out=join'',@out;$out2='';%flag=();while($out=~/OPM.opmBuffer\[src(?: \+ (\d+))?\]/){$out2.=$`;$out=$';$s=$&;$i=int(($1//0)/$CH);if($i==0||$i==$m){$out2.='(long) '.$s}else{$uv=$i%2?'v':'u';if(exists$flag{$s}){$out2.=$uv}else{$flag{$s}=1;$out2.='('.$uv.' = (long) '.$s.')'}}}$out2.=$out;print$out2"
            //left
            t = Math.max (-32768, Math.min (32767, (int) (101376L * (long) OPM.opmBuffer[src] + 153373L * (v = (long) OPM.opmBuffer[src + 2]) + 9251L * (u = (long) OPM.opmBuffer[src + 4]) + 131072L >> 18)));
            sndByteBlock[dst] = (byte) t;
            sndByteBlock[dst + 1] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (49379L * v + 177617L * u + 37004L * (v = (long) OPM.opmBuffer[src + 6]) + 131072L >> 18)));
            sndByteBlock[dst + 4] = (byte) t;
            sndByteBlock[dst + 5] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (15884L * u + 164857L * v + 83259L * (u = (long) OPM.opmBuffer[src + 8]) + 131072L >> 18)));
            sndByteBlock[dst + 8] = (byte) t;
            sndByteBlock[dst + 9] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (891L * v + 119493L * u + 139216L * (v = (long) OPM.opmBuffer[src + 10]) + 4400L * (u = (long) OPM.opmBuffer[src + 12]) + 131072L >> 18)));
            sndByteBlock[dst + 12] = (byte) t;
            sndByteBlock[dst + 13] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (63536L * v + 174053L * u + 26411L * (v = (long) OPM.opmBuffer[src + 14]) + 131072L >> 18)));
            sndByteBlock[dst + 16] = (byte) t;
            sndByteBlock[dst + 17] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (24299L * u + 172777L * v + 66924L * (u = (long) OPM.opmBuffer[src + 16]) + 131072L >> 18)));
            sndByteBlock[dst + 20] = (byte) t;
            sndByteBlock[dst + 21] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (3564L * v + 135828L * u + 123277L * (v = (long) OPM.opmBuffer[src + 18]) + 1331L * (u = (long) OPM.opmBuffer[src + 20]) + 131072L >> 18)));
            sndByteBlock[dst + 24] = (byte) t;
            sndByteBlock[dst + 25] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (79475L * v + 166925L * u + 17600L * (v = (long) OPM.opmBuffer[src + 22]) + 131072L >> 18)));
            sndByteBlock[dst + 28] = (byte) t;
            sndByteBlock[dst + 29] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (34496L * u + 177133L * v + 52371L * (u = (long) OPM.opmBuffer[src + 24]) + 131072L >> 18)));
            sndByteBlock[dst + 32] = (byte) t;
            sndByteBlock[dst + 33] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (8019L * v + 150381L * u + 105556L * (v = (long) OPM.opmBuffer[src + 26]) + 44L * (u = (long) OPM.opmBuffer[src + 28]) + 131072L >> 18)));
            sndByteBlock[dst + 36] = (byte) t;
            sndByteBlock[dst + 37] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (97196L * v + 156233L * u + 10571L * (v = (long) OPM.opmBuffer[src + 30]) + 131072L >> 18)));
            sndByteBlock[dst + 40] = (byte) t;
            sndByteBlock[dst + 41] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (46475L * u + 177925L * v + 39600L * (u = (long) OPM.opmBuffer[src + 32]) + 131072L >> 18)));
            sndByteBlock[dst + 44] = (byte) t;
            sndByteBlock[dst + 45] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (14256L * v + 162613L * u + 87131L * (v = (long) OPM.opmBuffer[src + 34]) + 131072L >> 18)));
            sndByteBlock[dst + 48] = (byte) t;
            sndByteBlock[dst + 49] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (539L * u + 115621L * v + 142516L * (u = (long) OPM.opmBuffer[src + 36]) + 5324L * (v = (long) OPM.opmBuffer[src + 38]) + 131072L >> 18)));
            sndByteBlock[dst + 52] = (byte) t;
            sndByteBlock[dst + 53] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (60236L * u + 175153L * v + 28611L * (u = (long) OPM.opmBuffer[src + 40]) + 131072L >> 18)));
            sndByteBlock[dst + 56] = (byte) t;
            sndByteBlock[dst + 57] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (22275L * v + 171325L * u + 70400L * (v = (long) OPM.opmBuffer[src + 42]) + 131072L >> 18)));
            sndByteBlock[dst + 60] = (byte) t;
            sndByteBlock[dst + 61] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (2816L * u + 132352L * v + 126973L * (u = (long) OPM.opmBuffer[src + 44]) + 1859L * (v = (long) OPM.opmBuffer[src + 46]) + 131072L >> 18)));
            sndByteBlock[dst + 64] = (byte) t;
            sndByteBlock[dst + 65] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (75779L * u + 168817L * v + 19404L * (u = (long) OPM.opmBuffer[src + 48]) + 131072L >> 18)));
            sndByteBlock[dst + 68] = (byte) t;
            sndByteBlock[dst + 69] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (32076L * v + 176473L * u + 55451L * (v = (long) OPM.opmBuffer[src + 50]) + 131072L >> 18)));
            sndByteBlock[dst + 72] = (byte) t;
            sndByteBlock[dst + 73] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (6875L * u + 147301L * v + 109648L * (u = (long) OPM.opmBuffer[src + 52]) + 176L * (v = (long) OPM.opmBuffer[src + 54]) + 131072L >> 18)));
            sndByteBlock[dst + 76] = (byte) t;
            sndByteBlock[dst + 77] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (93104L * u + 158917L * v + 11979L * (u = (long) OPM.opmBuffer[src + 56]) + 131072L >> 18)));
            sndByteBlock[dst + 80] = (byte) t;
            sndByteBlock[dst + 81] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (43659L * v + 178057L * u + 42284L * (v = (long) OPM.opmBuffer[src + 58]) + 131072L >> 18)));
            sndByteBlock[dst + 84] = (byte) t;
            sndByteBlock[dst + 85] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (12716L * u + 160193L * v + 91091L * (u = (long) OPM.opmBuffer[src + 60]) + 131072L >> 18)));
            sndByteBlock[dst + 88] = (byte) t;
            sndByteBlock[dst + 89] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (275L * v + 111661L * u + 145728L * (v = (long) OPM.opmBuffer[src + 62]) + 6336L * (u = (long) OPM.opmBuffer[src + 64]) + 131072L >> 18)));
            sndByteBlock[dst + 92] = (byte) t;
            sndByteBlock[dst + 93] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (57024L * v + 176077L * u + 30899L * (v = (long) OPM.opmBuffer[src + 66]) + 131072L >> 18)));
            sndByteBlock[dst + 96] = (byte) t;
            sndByteBlock[dst + 97] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (20339L * u + 169697L * v + 73964L * (u = (long) OPM.opmBuffer[src + 68]) + 131072L >> 18)));
            sndByteBlock[dst + 100] = (byte) t;
            sndByteBlock[dst + 101] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (2156L * v + 128788L * u + 130581L * (v = (long) OPM.opmBuffer[src + 70]) + 2475L * (u = (long) OPM.opmBuffer[src + 72]) + 131072L >> 18)));
            sndByteBlock[dst + 104] = (byte) t;
            sndByteBlock[dst + 105] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (72171L * v + 170533L * u + 21296L * (v = (long) OPM.opmBuffer[src + 74]) + 131072L >> 18)));
            sndByteBlock[dst + 108] = (byte) t;
            sndByteBlock[dst + 109] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (29744L * u + 175637L * v + 58619L * (u = (long) OPM.opmBuffer[src + 76]) + 131072L >> 18)));
            sndByteBlock[dst + 112] = (byte) t;
            sndByteBlock[dst + 113] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (5819L * v + 144133L * u + 113652L * (v = (long) OPM.opmBuffer[src + 78]) + 396L * (u = (long) OPM.opmBuffer[src + 80]) + 131072L >> 18)));
            sndByteBlock[dst + 116] = (byte) t;
            sndByteBlock[dst + 117] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (89100L * v + 161425L * u + 13475L * (v = (long) OPM.opmBuffer[src + 82]) + 131072L >> 18)));
            sndByteBlock[dst + 120] = (byte) t;
            sndByteBlock[dst + 121] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (40931L * u + 178013L * v + 45056L * (u = (long) OPM.opmBuffer[src + 84]) + 131072L >> 18)));
            sndByteBlock[dst + 124] = (byte) t;
            sndByteBlock[dst + 125] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (11264L * v + 157597L * u + 95139L * (v = (long) OPM.opmBuffer[src + 86]) + 131072L >> 18)));
            sndByteBlock[dst + 128] = (byte) t;
            sndByteBlock[dst + 129] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (99L * u + 107613L * v + 148852L * (u = (long) OPM.opmBuffer[src + 88]) + 7436L * (v = (long) OPM.opmBuffer[src + 90]) + 131072L >> 18)));
            sndByteBlock[dst + 132] = (byte) t;
            sndByteBlock[dst + 133] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (53900L * u + 176825L * v + 33275L * (u = (long) OPM.opmBuffer[src + 92]) + 131072L >> 18)));
            sndByteBlock[dst + 136] = (byte) t;
            sndByteBlock[dst + 137] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (18491L * v + 167893L * u + 77616L * (v = (long) OPM.opmBuffer[src + 94]) + 131072L >> 18)));
            sndByteBlock[dst + 140] = (byte) t;
            sndByteBlock[dst + 141] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (1584L * u + 125136L * v + 134101L * (u = (long) OPM.opmBuffer[src + 96]) + 3179L * (v = (long) OPM.opmBuffer[src + 98]) + 131072L >> 18)));
            sndByteBlock[dst + 144] = (byte) t;
            sndByteBlock[dst + 145] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (68651L * u + 172073L * v + 23276L * (u = (long) OPM.opmBuffer[src + 100]) + 131072L >> 18)));
            sndByteBlock[dst + 148] = (byte) t;
            sndByteBlock[dst + 149] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (27500L * v + 174625L * u + 61875L * (v = (long) OPM.opmBuffer[src + 102]) + 131072L >> 18)));
            sndByteBlock[dst + 152] = (byte) t;
            sndByteBlock[dst + 153] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (4851L * u + 140877L * v + 117568L * (u = (long) OPM.opmBuffer[src + 104]) + 704L * (v = (long) OPM.opmBuffer[src + 106]) + 131072L >> 18)));
            sndByteBlock[dst + 156] = (byte) t;
            sndByteBlock[dst + 157] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (85184L * u + 163757L * v + 15059L * (u = (long) OPM.opmBuffer[src + 108]) + 131072L >> 18)));
            sndByteBlock[dst + 160] = (byte) t;
            sndByteBlock[dst + 161] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (38291L * v + 177793L * u + 47916L * (v = (long) OPM.opmBuffer[src + 110]) + 131072L >> 18)));
            sndByteBlock[dst + 164] = (byte) t;
            sndByteBlock[dst + 165] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (9900L * u + 154825L * v + 99275L * (u = (long) OPM.opmBuffer[src + 112]) + 131072L >> 18)));
            sndByteBlock[dst + 168] = (byte) t;
            sndByteBlock[dst + 169] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (11L * v + 103477L * u + 151888L * (v = (long) OPM.opmBuffer[src + 114]) + 8624L * (u = (long) OPM.opmBuffer[src + 116]) + 131072L >> 18)));
            sndByteBlock[dst + 172] = (byte) t;
            sndByteBlock[dst + 173] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (50864L * v + 177397L * u + 35739L * (v = (long) OPM.opmBuffer[src + 118]) + 131072L >> 18)));
            sndByteBlock[dst + 176] = (byte) t;
            sndByteBlock[dst + 177] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (16731L * u + 165913L * v + 81356L * (u = (long) OPM.opmBuffer[src + 120]) + 131072L >> 18)));
            sndByteBlock[dst + 180] = (byte) t;
            sndByteBlock[dst + 181] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (1100L * v + 121396L * u + 137533L * (v = (long) OPM.opmBuffer[src + 122]) + 3971L * (u = (long) OPM.opmBuffer[src + 124]) + 131072L >> 18)));
            sndByteBlock[dst + 184] = (byte) t;
            sndByteBlock[dst + 185] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (65219L * v + 173437L * u + 25344L * (v = (long) OPM.opmBuffer[src + 126]) + 131072L >> 18)));
            sndByteBlock[dst + 188] = (byte) t;
            sndByteBlock[dst + 189] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (25344L * u + 173437L * v + 65219L * (u = (long) OPM.opmBuffer[src + 128]) + 131072L >> 18)));
            sndByteBlock[dst + 192] = (byte) t;
            sndByteBlock[dst + 193] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (3971L * v + 137533L * u + 121396L * (v = (long) OPM.opmBuffer[src + 130]) + 1100L * (u = (long) OPM.opmBuffer[src + 132]) + 131072L >> 18)));
            sndByteBlock[dst + 196] = (byte) t;
            sndByteBlock[dst + 197] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (81356L * v + 165913L * u + 16731L * (v = (long) OPM.opmBuffer[src + 134]) + 131072L >> 18)));
            sndByteBlock[dst + 200] = (byte) t;
            sndByteBlock[dst + 201] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (35739L * u + 177397L * v + 50864L * (u = (long) OPM.opmBuffer[src + 136]) + 131072L >> 18)));
            sndByteBlock[dst + 204] = (byte) t;
            sndByteBlock[dst + 205] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (8624L * v + 151888L * u + 103477L * (v = (long) OPM.opmBuffer[src + 138]) + 11L * (u = (long) OPM.opmBuffer[src + 140]) + 131072L >> 18)));
            sndByteBlock[dst + 208] = (byte) t;
            sndByteBlock[dst + 209] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (99275L * v + 154825L * u + 9900L * (v = (long) OPM.opmBuffer[src + 142]) + 131072L >> 18)));
            sndByteBlock[dst + 212] = (byte) t;
            sndByteBlock[dst + 213] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (47916L * u + 177793L * v + 38291L * (u = (long) OPM.opmBuffer[src + 144]) + 131072L >> 18)));
            sndByteBlock[dst + 216] = (byte) t;
            sndByteBlock[dst + 217] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (15059L * v + 163757L * u + 85184L * (v = (long) OPM.opmBuffer[src + 146]) + 131072L >> 18)));
            sndByteBlock[dst + 220] = (byte) t;
            sndByteBlock[dst + 221] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (704L * u + 117568L * v + 140877L * (u = (long) OPM.opmBuffer[src + 148]) + 4851L * (v = (long) OPM.opmBuffer[src + 150]) + 131072L >> 18)));
            sndByteBlock[dst + 224] = (byte) t;
            sndByteBlock[dst + 225] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (61875L * u + 174625L * v + 27500L * (u = (long) OPM.opmBuffer[src + 152]) + 131072L >> 18)));
            sndByteBlock[dst + 228] = (byte) t;
            sndByteBlock[dst + 229] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (23276L * v + 172073L * u + 68651L * (v = (long) OPM.opmBuffer[src + 154]) + 131072L >> 18)));
            sndByteBlock[dst + 232] = (byte) t;
            sndByteBlock[dst + 233] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (3179L * u + 134101L * v + 125136L * (u = (long) OPM.opmBuffer[src + 156]) + 1584L * (v = (long) OPM.opmBuffer[src + 158]) + 131072L >> 18)));
            sndByteBlock[dst + 236] = (byte) t;
            sndByteBlock[dst + 237] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (77616L * u + 167893L * v + 18491L * (u = (long) OPM.opmBuffer[src + 160]) + 131072L >> 18)));
            sndByteBlock[dst + 240] = (byte) t;
            sndByteBlock[dst + 241] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (33275L * v + 176825L * u + 53900L * (v = (long) OPM.opmBuffer[src + 162]) + 131072L >> 18)));
            sndByteBlock[dst + 244] = (byte) t;
            sndByteBlock[dst + 245] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (7436L * u + 148852L * v + 107613L * (u = (long) OPM.opmBuffer[src + 164]) + 99L * (v = (long) OPM.opmBuffer[src + 166]) + 131072L >> 18)));
            sndByteBlock[dst + 248] = (byte) t;
            sndByteBlock[dst + 249] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (95139L * u + 157597L * v + 11264L * (u = (long) OPM.opmBuffer[src + 168]) + 131072L >> 18)));
            sndByteBlock[dst + 252] = (byte) t;
            sndByteBlock[dst + 253] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (45056L * v + 178013L * u + 40931L * (v = (long) OPM.opmBuffer[src + 170]) + 131072L >> 18)));
            sndByteBlock[dst + 256] = (byte) t;
            sndByteBlock[dst + 257] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (13475L * u + 161425L * v + 89100L * (u = (long) OPM.opmBuffer[src + 172]) + 131072L >> 18)));
            sndByteBlock[dst + 260] = (byte) t;
            sndByteBlock[dst + 261] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (396L * v + 113652L * u + 144133L * (v = (long) OPM.opmBuffer[src + 174]) + 5819L * (u = (long) OPM.opmBuffer[src + 176]) + 131072L >> 18)));
            sndByteBlock[dst + 264] = (byte) t;
            sndByteBlock[dst + 265] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (58619L * v + 175637L * u + 29744L * (v = (long) OPM.opmBuffer[src + 178]) + 131072L >> 18)));
            sndByteBlock[dst + 268] = (byte) t;
            sndByteBlock[dst + 269] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (21296L * u + 170533L * v + 72171L * (u = (long) OPM.opmBuffer[src + 180]) + 131072L >> 18)));
            sndByteBlock[dst + 272] = (byte) t;
            sndByteBlock[dst + 273] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (2475L * v + 130581L * u + 128788L * (v = (long) OPM.opmBuffer[src + 182]) + 2156L * (u = (long) OPM.opmBuffer[src + 184]) + 131072L >> 18)));
            sndByteBlock[dst + 276] = (byte) t;
            sndByteBlock[dst + 277] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (73964L * v + 169697L * u + 20339L * (v = (long) OPM.opmBuffer[src + 186]) + 131072L >> 18)));
            sndByteBlock[dst + 280] = (byte) t;
            sndByteBlock[dst + 281] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (30899L * u + 176077L * v + 57024L * (u = (long) OPM.opmBuffer[src + 188]) + 131072L >> 18)));
            sndByteBlock[dst + 284] = (byte) t;
            sndByteBlock[dst + 285] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (6336L * v + 145728L * u + 111661L * (v = (long) OPM.opmBuffer[src + 190]) + 275L * (u = (long) OPM.opmBuffer[src + 192]) + 131072L >> 18)));
            sndByteBlock[dst + 288] = (byte) t;
            sndByteBlock[dst + 289] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (91091L * v + 160193L * u + 12716L * (v = (long) OPM.opmBuffer[src + 194]) + 131072L >> 18)));
            sndByteBlock[dst + 292] = (byte) t;
            sndByteBlock[dst + 293] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (42284L * u + 178057L * v + 43659L * (u = (long) OPM.opmBuffer[src + 196]) + 131072L >> 18)));
            sndByteBlock[dst + 296] = (byte) t;
            sndByteBlock[dst + 297] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (11979L * v + 158917L * u + 93104L * (v = (long) OPM.opmBuffer[src + 198]) + 131072L >> 18)));
            sndByteBlock[dst + 300] = (byte) t;
            sndByteBlock[dst + 301] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (176L * u + 109648L * v + 147301L * (u = (long) OPM.opmBuffer[src + 200]) + 6875L * (v = (long) OPM.opmBuffer[src + 202]) + 131072L >> 18)));
            sndByteBlock[dst + 304] = (byte) t;
            sndByteBlock[dst + 305] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (55451L * u + 176473L * v + 32076L * (u = (long) OPM.opmBuffer[src + 204]) + 131072L >> 18)));
            sndByteBlock[dst + 308] = (byte) t;
            sndByteBlock[dst + 309] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (19404L * v + 168817L * u + 75779L * (v = (long) OPM.opmBuffer[src + 206]) + 131072L >> 18)));
            sndByteBlock[dst + 312] = (byte) t;
            sndByteBlock[dst + 313] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (1859L * u + 126973L * v + 132352L * (u = (long) OPM.opmBuffer[src + 208]) + 2816L * (v = (long) OPM.opmBuffer[src + 210]) + 131072L >> 18)));
            sndByteBlock[dst + 316] = (byte) t;
            sndByteBlock[dst + 317] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (70400L * u + 171325L * v + 22275L * (u = (long) OPM.opmBuffer[src + 212]) + 131072L >> 18)));
            sndByteBlock[dst + 320] = (byte) t;
            sndByteBlock[dst + 321] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (28611L * v + 175153L * u + 60236L * (v = (long) OPM.opmBuffer[src + 214]) + 131072L >> 18)));
            sndByteBlock[dst + 324] = (byte) t;
            sndByteBlock[dst + 325] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (5324L * u + 142516L * v + 115621L * (u = (long) OPM.opmBuffer[src + 216]) + 539L * (v = (long) OPM.opmBuffer[src + 218]) + 131072L >> 18)));
            sndByteBlock[dst + 328] = (byte) t;
            sndByteBlock[dst + 329] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (87131L * u + 162613L * v + 14256L * (u = (long) OPM.opmBuffer[src + 220]) + 131072L >> 18)));
            sndByteBlock[dst + 332] = (byte) t;
            sndByteBlock[dst + 333] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (39600L * v + 177925L * u + 46475L * (v = (long) OPM.opmBuffer[src + 222]) + 131072L >> 18)));
            sndByteBlock[dst + 336] = (byte) t;
            sndByteBlock[dst + 337] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (10571L * u + 156233L * v + 97196L * (u = (long) OPM.opmBuffer[src + 224]) + 131072L >> 18)));
            sndByteBlock[dst + 340] = (byte) t;
            sndByteBlock[dst + 341] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (44L * v + 105556L * u + 150381L * (v = (long) OPM.opmBuffer[src + 226]) + 8019L * (u = (long) OPM.opmBuffer[src + 228]) + 131072L >> 18)));
            sndByteBlock[dst + 344] = (byte) t;
            sndByteBlock[dst + 345] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (52371L * v + 177133L * u + 34496L * (v = (long) OPM.opmBuffer[src + 230]) + 131072L >> 18)));
            sndByteBlock[dst + 348] = (byte) t;
            sndByteBlock[dst + 349] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (17600L * u + 166925L * v + 79475L * (u = (long) OPM.opmBuffer[src + 232]) + 131072L >> 18)));
            sndByteBlock[dst + 352] = (byte) t;
            sndByteBlock[dst + 353] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (1331L * v + 123277L * u + 135828L * (v = (long) OPM.opmBuffer[src + 234]) + 3564L * (u = (long) OPM.opmBuffer[src + 236]) + 131072L >> 18)));
            sndByteBlock[dst + 356] = (byte) t;
            sndByteBlock[dst + 357] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (66924L * v + 172777L * u + 24299L * (v = (long) OPM.opmBuffer[src + 238]) + 131072L >> 18)));
            sndByteBlock[dst + 360] = (byte) t;
            sndByteBlock[dst + 361] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (26411L * u + 174053L * v + 63536L * (u = (long) OPM.opmBuffer[src + 240]) + 131072L >> 18)));
            sndByteBlock[dst + 364] = (byte) t;
            sndByteBlock[dst + 365] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (4400L * v + 139216L * u + 119493L * (v = (long) OPM.opmBuffer[src + 242]) + 891L * (u = (long) OPM.opmBuffer[src + 244]) + 131072L >> 18)));
            sndByteBlock[dst + 368] = (byte) t;
            sndByteBlock[dst + 369] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (83259L * v + 164857L * u + 15884L * (v = (long) OPM.opmBuffer[src + 246]) + 131072L >> 18)));
            sndByteBlock[dst + 372] = (byte) t;
            sndByteBlock[dst + 373] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (37004L * u + 177617L * v + 49379L * (u = (long) OPM.opmBuffer[src + 248]) + 131072L >> 18)));
            sndByteBlock[dst + 376] = (byte) t;
            sndByteBlock[dst + 377] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (9251L * v + 153373L * u + 101376L * (long) OPM.opmBuffer[src + 250] + 131072L >> 18)));
            sndByteBlock[dst + 380] = (byte) t;
            sndByteBlock[dst + 381] = (byte) (t >> 8);
            //right
            t = Math.max (-32768, Math.min (32767, (int) (101376L * (long) OPM.opmBuffer[src + 1] + 153373L * (v = (long) OPM.opmBuffer[src + 3]) + 9251L * (u = (long) OPM.opmBuffer[src + 5]) + 131072L >> 18)));
            sndByteBlock[dst + 2] = (byte) t;
            sndByteBlock[dst + 3] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (49379L * v + 177617L * u + 37004L * (v = (long) OPM.opmBuffer[src + 7]) + 131072L >> 18)));
            sndByteBlock[dst + 6] = (byte) t;
            sndByteBlock[dst + 7] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (15884L * u + 164857L * v + 83259L * (u = (long) OPM.opmBuffer[src + 9]) + 131072L >> 18)));
            sndByteBlock[dst + 10] = (byte) t;
            sndByteBlock[dst + 11] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (891L * v + 119493L * u + 139216L * (v = (long) OPM.opmBuffer[src + 11]) + 4400L * (u = (long) OPM.opmBuffer[src + 13]) + 131072L >> 18)));
            sndByteBlock[dst + 14] = (byte) t;
            sndByteBlock[dst + 15] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (63536L * v + 174053L * u + 26411L * (v = (long) OPM.opmBuffer[src + 15]) + 131072L >> 18)));
            sndByteBlock[dst + 18] = (byte) t;
            sndByteBlock[dst + 19] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (24299L * u + 172777L * v + 66924L * (u = (long) OPM.opmBuffer[src + 17]) + 131072L >> 18)));
            sndByteBlock[dst + 22] = (byte) t;
            sndByteBlock[dst + 23] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (3564L * v + 135828L * u + 123277L * (v = (long) OPM.opmBuffer[src + 19]) + 1331L * (u = (long) OPM.opmBuffer[src + 21]) + 131072L >> 18)));
            sndByteBlock[dst + 26] = (byte) t;
            sndByteBlock[dst + 27] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (79475L * v + 166925L * u + 17600L * (v = (long) OPM.opmBuffer[src + 23]) + 131072L >> 18)));
            sndByteBlock[dst + 30] = (byte) t;
            sndByteBlock[dst + 31] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (34496L * u + 177133L * v + 52371L * (u = (long) OPM.opmBuffer[src + 25]) + 131072L >> 18)));
            sndByteBlock[dst + 34] = (byte) t;
            sndByteBlock[dst + 35] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (8019L * v + 150381L * u + 105556L * (v = (long) OPM.opmBuffer[src + 27]) + 44L * (u = (long) OPM.opmBuffer[src + 29]) + 131072L >> 18)));
            sndByteBlock[dst + 38] = (byte) t;
            sndByteBlock[dst + 39] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (97196L * v + 156233L * u + 10571L * (v = (long) OPM.opmBuffer[src + 31]) + 131072L >> 18)));
            sndByteBlock[dst + 42] = (byte) t;
            sndByteBlock[dst + 43] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (46475L * u + 177925L * v + 39600L * (u = (long) OPM.opmBuffer[src + 33]) + 131072L >> 18)));
            sndByteBlock[dst + 46] = (byte) t;
            sndByteBlock[dst + 47] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (14256L * v + 162613L * u + 87131L * (v = (long) OPM.opmBuffer[src + 35]) + 131072L >> 18)));
            sndByteBlock[dst + 50] = (byte) t;
            sndByteBlock[dst + 51] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (539L * u + 115621L * v + 142516L * (u = (long) OPM.opmBuffer[src + 37]) + 5324L * (v = (long) OPM.opmBuffer[src + 39]) + 131072L >> 18)));
            sndByteBlock[dst + 54] = (byte) t;
            sndByteBlock[dst + 55] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (60236L * u + 175153L * v + 28611L * (u = (long) OPM.opmBuffer[src + 41]) + 131072L >> 18)));
            sndByteBlock[dst + 58] = (byte) t;
            sndByteBlock[dst + 59] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (22275L * v + 171325L * u + 70400L * (v = (long) OPM.opmBuffer[src + 43]) + 131072L >> 18)));
            sndByteBlock[dst + 62] = (byte) t;
            sndByteBlock[dst + 63] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (2816L * u + 132352L * v + 126973L * (u = (long) OPM.opmBuffer[src + 45]) + 1859L * (v = (long) OPM.opmBuffer[src + 47]) + 131072L >> 18)));
            sndByteBlock[dst + 66] = (byte) t;
            sndByteBlock[dst + 67] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (75779L * u + 168817L * v + 19404L * (u = (long) OPM.opmBuffer[src + 49]) + 131072L >> 18)));
            sndByteBlock[dst + 70] = (byte) t;
            sndByteBlock[dst + 71] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (32076L * v + 176473L * u + 55451L * (v = (long) OPM.opmBuffer[src + 51]) + 131072L >> 18)));
            sndByteBlock[dst + 74] = (byte) t;
            sndByteBlock[dst + 75] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (6875L * u + 147301L * v + 109648L * (u = (long) OPM.opmBuffer[src + 53]) + 176L * (v = (long) OPM.opmBuffer[src + 55]) + 131072L >> 18)));
            sndByteBlock[dst + 78] = (byte) t;
            sndByteBlock[dst + 79] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (93104L * u + 158917L * v + 11979L * (u = (long) OPM.opmBuffer[src + 57]) + 131072L >> 18)));
            sndByteBlock[dst + 82] = (byte) t;
            sndByteBlock[dst + 83] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (43659L * v + 178057L * u + 42284L * (v = (long) OPM.opmBuffer[src + 59]) + 131072L >> 18)));
            sndByteBlock[dst + 86] = (byte) t;
            sndByteBlock[dst + 87] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (12716L * u + 160193L * v + 91091L * (u = (long) OPM.opmBuffer[src + 61]) + 131072L >> 18)));
            sndByteBlock[dst + 90] = (byte) t;
            sndByteBlock[dst + 91] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (275L * v + 111661L * u + 145728L * (v = (long) OPM.opmBuffer[src + 63]) + 6336L * (u = (long) OPM.opmBuffer[src + 65]) + 131072L >> 18)));
            sndByteBlock[dst + 94] = (byte) t;
            sndByteBlock[dst + 95] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (57024L * v + 176077L * u + 30899L * (v = (long) OPM.opmBuffer[src + 67]) + 131072L >> 18)));
            sndByteBlock[dst + 98] = (byte) t;
            sndByteBlock[dst + 99] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (20339L * u + 169697L * v + 73964L * (u = (long) OPM.opmBuffer[src + 69]) + 131072L >> 18)));
            sndByteBlock[dst + 102] = (byte) t;
            sndByteBlock[dst + 103] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (2156L * v + 128788L * u + 130581L * (v = (long) OPM.opmBuffer[src + 71]) + 2475L * (u = (long) OPM.opmBuffer[src + 73]) + 131072L >> 18)));
            sndByteBlock[dst + 106] = (byte) t;
            sndByteBlock[dst + 107] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (72171L * v + 170533L * u + 21296L * (v = (long) OPM.opmBuffer[src + 75]) + 131072L >> 18)));
            sndByteBlock[dst + 110] = (byte) t;
            sndByteBlock[dst + 111] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (29744L * u + 175637L * v + 58619L * (u = (long) OPM.opmBuffer[src + 77]) + 131072L >> 18)));
            sndByteBlock[dst + 114] = (byte) t;
            sndByteBlock[dst + 115] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (5819L * v + 144133L * u + 113652L * (v = (long) OPM.opmBuffer[src + 79]) + 396L * (u = (long) OPM.opmBuffer[src + 81]) + 131072L >> 18)));
            sndByteBlock[dst + 118] = (byte) t;
            sndByteBlock[dst + 119] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (89100L * v + 161425L * u + 13475L * (v = (long) OPM.opmBuffer[src + 83]) + 131072L >> 18)));
            sndByteBlock[dst + 122] = (byte) t;
            sndByteBlock[dst + 123] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (40931L * u + 178013L * v + 45056L * (u = (long) OPM.opmBuffer[src + 85]) + 131072L >> 18)));
            sndByteBlock[dst + 126] = (byte) t;
            sndByteBlock[dst + 127] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (11264L * v + 157597L * u + 95139L * (v = (long) OPM.opmBuffer[src + 87]) + 131072L >> 18)));
            sndByteBlock[dst + 130] = (byte) t;
            sndByteBlock[dst + 131] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (99L * u + 107613L * v + 148852L * (u = (long) OPM.opmBuffer[src + 89]) + 7436L * (v = (long) OPM.opmBuffer[src + 91]) + 131072L >> 18)));
            sndByteBlock[dst + 134] = (byte) t;
            sndByteBlock[dst + 135] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (53900L * u + 176825L * v + 33275L * (u = (long) OPM.opmBuffer[src + 93]) + 131072L >> 18)));
            sndByteBlock[dst + 138] = (byte) t;
            sndByteBlock[dst + 139] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (18491L * v + 167893L * u + 77616L * (v = (long) OPM.opmBuffer[src + 95]) + 131072L >> 18)));
            sndByteBlock[dst + 142] = (byte) t;
            sndByteBlock[dst + 143] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (1584L * u + 125136L * v + 134101L * (u = (long) OPM.opmBuffer[src + 97]) + 3179L * (v = (long) OPM.opmBuffer[src + 99]) + 131072L >> 18)));
            sndByteBlock[dst + 146] = (byte) t;
            sndByteBlock[dst + 147] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (68651L * u + 172073L * v + 23276L * (u = (long) OPM.opmBuffer[src + 101]) + 131072L >> 18)));
            sndByteBlock[dst + 150] = (byte) t;
            sndByteBlock[dst + 151] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (27500L * v + 174625L * u + 61875L * (v = (long) OPM.opmBuffer[src + 103]) + 131072L >> 18)));
            sndByteBlock[dst + 154] = (byte) t;
            sndByteBlock[dst + 155] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (4851L * u + 140877L * v + 117568L * (u = (long) OPM.opmBuffer[src + 105]) + 704L * (v = (long) OPM.opmBuffer[src + 107]) + 131072L >> 18)));
            sndByteBlock[dst + 158] = (byte) t;
            sndByteBlock[dst + 159] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (85184L * u + 163757L * v + 15059L * (u = (long) OPM.opmBuffer[src + 109]) + 131072L >> 18)));
            sndByteBlock[dst + 162] = (byte) t;
            sndByteBlock[dst + 163] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (38291L * v + 177793L * u + 47916L * (v = (long) OPM.opmBuffer[src + 111]) + 131072L >> 18)));
            sndByteBlock[dst + 166] = (byte) t;
            sndByteBlock[dst + 167] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (9900L * u + 154825L * v + 99275L * (u = (long) OPM.opmBuffer[src + 113]) + 131072L >> 18)));
            sndByteBlock[dst + 170] = (byte) t;
            sndByteBlock[dst + 171] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (11L * v + 103477L * u + 151888L * (v = (long) OPM.opmBuffer[src + 115]) + 8624L * (u = (long) OPM.opmBuffer[src + 117]) + 131072L >> 18)));
            sndByteBlock[dst + 174] = (byte) t;
            sndByteBlock[dst + 175] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (50864L * v + 177397L * u + 35739L * (v = (long) OPM.opmBuffer[src + 119]) + 131072L >> 18)));
            sndByteBlock[dst + 178] = (byte) t;
            sndByteBlock[dst + 179] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (16731L * u + 165913L * v + 81356L * (u = (long) OPM.opmBuffer[src + 121]) + 131072L >> 18)));
            sndByteBlock[dst + 182] = (byte) t;
            sndByteBlock[dst + 183] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (1100L * v + 121396L * u + 137533L * (v = (long) OPM.opmBuffer[src + 123]) + 3971L * (u = (long) OPM.opmBuffer[src + 125]) + 131072L >> 18)));
            sndByteBlock[dst + 186] = (byte) t;
            sndByteBlock[dst + 187] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (65219L * v + 173437L * u + 25344L * (v = (long) OPM.opmBuffer[src + 127]) + 131072L >> 18)));
            sndByteBlock[dst + 190] = (byte) t;
            sndByteBlock[dst + 191] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (25344L * u + 173437L * v + 65219L * (u = (long) OPM.opmBuffer[src + 129]) + 131072L >> 18)));
            sndByteBlock[dst + 194] = (byte) t;
            sndByteBlock[dst + 195] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (3971L * v + 137533L * u + 121396L * (v = (long) OPM.opmBuffer[src + 131]) + 1100L * (u = (long) OPM.opmBuffer[src + 133]) + 131072L >> 18)));
            sndByteBlock[dst + 198] = (byte) t;
            sndByteBlock[dst + 199] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (81356L * v + 165913L * u + 16731L * (v = (long) OPM.opmBuffer[src + 135]) + 131072L >> 18)));
            sndByteBlock[dst + 202] = (byte) t;
            sndByteBlock[dst + 203] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (35739L * u + 177397L * v + 50864L * (u = (long) OPM.opmBuffer[src + 137]) + 131072L >> 18)));
            sndByteBlock[dst + 206] = (byte) t;
            sndByteBlock[dst + 207] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (8624L * v + 151888L * u + 103477L * (v = (long) OPM.opmBuffer[src + 139]) + 11L * (u = (long) OPM.opmBuffer[src + 141]) + 131072L >> 18)));
            sndByteBlock[dst + 210] = (byte) t;
            sndByteBlock[dst + 211] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (99275L * v + 154825L * u + 9900L * (v = (long) OPM.opmBuffer[src + 143]) + 131072L >> 18)));
            sndByteBlock[dst + 214] = (byte) t;
            sndByteBlock[dst + 215] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (47916L * u + 177793L * v + 38291L * (u = (long) OPM.opmBuffer[src + 145]) + 131072L >> 18)));
            sndByteBlock[dst + 218] = (byte) t;
            sndByteBlock[dst + 219] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (15059L * v + 163757L * u + 85184L * (v = (long) OPM.opmBuffer[src + 147]) + 131072L >> 18)));
            sndByteBlock[dst + 222] = (byte) t;
            sndByteBlock[dst + 223] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (704L * u + 117568L * v + 140877L * (u = (long) OPM.opmBuffer[src + 149]) + 4851L * (v = (long) OPM.opmBuffer[src + 151]) + 131072L >> 18)));
            sndByteBlock[dst + 226] = (byte) t;
            sndByteBlock[dst + 227] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (61875L * u + 174625L * v + 27500L * (u = (long) OPM.opmBuffer[src + 153]) + 131072L >> 18)));
            sndByteBlock[dst + 230] = (byte) t;
            sndByteBlock[dst + 231] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (23276L * v + 172073L * u + 68651L * (v = (long) OPM.opmBuffer[src + 155]) + 131072L >> 18)));
            sndByteBlock[dst + 234] = (byte) t;
            sndByteBlock[dst + 235] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (3179L * u + 134101L * v + 125136L * (u = (long) OPM.opmBuffer[src + 157]) + 1584L * (v = (long) OPM.opmBuffer[src + 159]) + 131072L >> 18)));
            sndByteBlock[dst + 238] = (byte) t;
            sndByteBlock[dst + 239] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (77616L * u + 167893L * v + 18491L * (u = (long) OPM.opmBuffer[src + 161]) + 131072L >> 18)));
            sndByteBlock[dst + 242] = (byte) t;
            sndByteBlock[dst + 243] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (33275L * v + 176825L * u + 53900L * (v = (long) OPM.opmBuffer[src + 163]) + 131072L >> 18)));
            sndByteBlock[dst + 246] = (byte) t;
            sndByteBlock[dst + 247] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (7436L * u + 148852L * v + 107613L * (u = (long) OPM.opmBuffer[src + 165]) + 99L * (v = (long) OPM.opmBuffer[src + 167]) + 131072L >> 18)));
            sndByteBlock[dst + 250] = (byte) t;
            sndByteBlock[dst + 251] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (95139L * u + 157597L * v + 11264L * (u = (long) OPM.opmBuffer[src + 169]) + 131072L >> 18)));
            sndByteBlock[dst + 254] = (byte) t;
            sndByteBlock[dst + 255] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (45056L * v + 178013L * u + 40931L * (v = (long) OPM.opmBuffer[src + 171]) + 131072L >> 18)));
            sndByteBlock[dst + 258] = (byte) t;
            sndByteBlock[dst + 259] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (13475L * u + 161425L * v + 89100L * (u = (long) OPM.opmBuffer[src + 173]) + 131072L >> 18)));
            sndByteBlock[dst + 262] = (byte) t;
            sndByteBlock[dst + 263] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (396L * v + 113652L * u + 144133L * (v = (long) OPM.opmBuffer[src + 175]) + 5819L * (u = (long) OPM.opmBuffer[src + 177]) + 131072L >> 18)));
            sndByteBlock[dst + 266] = (byte) t;
            sndByteBlock[dst + 267] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (58619L * v + 175637L * u + 29744L * (v = (long) OPM.opmBuffer[src + 179]) + 131072L >> 18)));
            sndByteBlock[dst + 270] = (byte) t;
            sndByteBlock[dst + 271] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (21296L * u + 170533L * v + 72171L * (u = (long) OPM.opmBuffer[src + 181]) + 131072L >> 18)));
            sndByteBlock[dst + 274] = (byte) t;
            sndByteBlock[dst + 275] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (2475L * v + 130581L * u + 128788L * (v = (long) OPM.opmBuffer[src + 183]) + 2156L * (u = (long) OPM.opmBuffer[src + 185]) + 131072L >> 18)));
            sndByteBlock[dst + 278] = (byte) t;
            sndByteBlock[dst + 279] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (73964L * v + 169697L * u + 20339L * (v = (long) OPM.opmBuffer[src + 187]) + 131072L >> 18)));
            sndByteBlock[dst + 282] = (byte) t;
            sndByteBlock[dst + 283] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (30899L * u + 176077L * v + 57024L * (u = (long) OPM.opmBuffer[src + 189]) + 131072L >> 18)));
            sndByteBlock[dst + 286] = (byte) t;
            sndByteBlock[dst + 287] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (6336L * v + 145728L * u + 111661L * (v = (long) OPM.opmBuffer[src + 191]) + 275L * (u = (long) OPM.opmBuffer[src + 193]) + 131072L >> 18)));
            sndByteBlock[dst + 290] = (byte) t;
            sndByteBlock[dst + 291] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (91091L * v + 160193L * u + 12716L * (v = (long) OPM.opmBuffer[src + 195]) + 131072L >> 18)));
            sndByteBlock[dst + 294] = (byte) t;
            sndByteBlock[dst + 295] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (42284L * u + 178057L * v + 43659L * (u = (long) OPM.opmBuffer[src + 197]) + 131072L >> 18)));
            sndByteBlock[dst + 298] = (byte) t;
            sndByteBlock[dst + 299] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (11979L * v + 158917L * u + 93104L * (v = (long) OPM.opmBuffer[src + 199]) + 131072L >> 18)));
            sndByteBlock[dst + 302] = (byte) t;
            sndByteBlock[dst + 303] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (176L * u + 109648L * v + 147301L * (u = (long) OPM.opmBuffer[src + 201]) + 6875L * (v = (long) OPM.opmBuffer[src + 203]) + 131072L >> 18)));
            sndByteBlock[dst + 306] = (byte) t;
            sndByteBlock[dst + 307] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (55451L * u + 176473L * v + 32076L * (u = (long) OPM.opmBuffer[src + 205]) + 131072L >> 18)));
            sndByteBlock[dst + 310] = (byte) t;
            sndByteBlock[dst + 311] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (19404L * v + 168817L * u + 75779L * (v = (long) OPM.opmBuffer[src + 207]) + 131072L >> 18)));
            sndByteBlock[dst + 314] = (byte) t;
            sndByteBlock[dst + 315] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (1859L * u + 126973L * v + 132352L * (u = (long) OPM.opmBuffer[src + 209]) + 2816L * (v = (long) OPM.opmBuffer[src + 211]) + 131072L >> 18)));
            sndByteBlock[dst + 318] = (byte) t;
            sndByteBlock[dst + 319] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (70400L * u + 171325L * v + 22275L * (u = (long) OPM.opmBuffer[src + 213]) + 131072L >> 18)));
            sndByteBlock[dst + 322] = (byte) t;
            sndByteBlock[dst + 323] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (28611L * v + 175153L * u + 60236L * (v = (long) OPM.opmBuffer[src + 215]) + 131072L >> 18)));
            sndByteBlock[dst + 326] = (byte) t;
            sndByteBlock[dst + 327] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (5324L * u + 142516L * v + 115621L * (u = (long) OPM.opmBuffer[src + 217]) + 539L * (v = (long) OPM.opmBuffer[src + 219]) + 131072L >> 18)));
            sndByteBlock[dst + 330] = (byte) t;
            sndByteBlock[dst + 331] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (87131L * u + 162613L * v + 14256L * (u = (long) OPM.opmBuffer[src + 221]) + 131072L >> 18)));
            sndByteBlock[dst + 334] = (byte) t;
            sndByteBlock[dst + 335] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (39600L * v + 177925L * u + 46475L * (v = (long) OPM.opmBuffer[src + 223]) + 131072L >> 18)));
            sndByteBlock[dst + 338] = (byte) t;
            sndByteBlock[dst + 339] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (10571L * u + 156233L * v + 97196L * (u = (long) OPM.opmBuffer[src + 225]) + 131072L >> 18)));
            sndByteBlock[dst + 342] = (byte) t;
            sndByteBlock[dst + 343] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (44L * v + 105556L * u + 150381L * (v = (long) OPM.opmBuffer[src + 227]) + 8019L * (u = (long) OPM.opmBuffer[src + 229]) + 131072L >> 18)));
            sndByteBlock[dst + 346] = (byte) t;
            sndByteBlock[dst + 347] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (52371L * v + 177133L * u + 34496L * (v = (long) OPM.opmBuffer[src + 231]) + 131072L >> 18)));
            sndByteBlock[dst + 350] = (byte) t;
            sndByteBlock[dst + 351] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (17600L * u + 166925L * v + 79475L * (u = (long) OPM.opmBuffer[src + 233]) + 131072L >> 18)));
            sndByteBlock[dst + 354] = (byte) t;
            sndByteBlock[dst + 355] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (1331L * v + 123277L * u + 135828L * (v = (long) OPM.opmBuffer[src + 235]) + 3564L * (u = (long) OPM.opmBuffer[src + 237]) + 131072L >> 18)));
            sndByteBlock[dst + 358] = (byte) t;
            sndByteBlock[dst + 359] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (66924L * v + 172777L * u + 24299L * (v = (long) OPM.opmBuffer[src + 239]) + 131072L >> 18)));
            sndByteBlock[dst + 362] = (byte) t;
            sndByteBlock[dst + 363] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (26411L * u + 174053L * v + 63536L * (u = (long) OPM.opmBuffer[src + 241]) + 131072L >> 18)));
            sndByteBlock[dst + 366] = (byte) t;
            sndByteBlock[dst + 367] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (4400L * v + 139216L * u + 119493L * (v = (long) OPM.opmBuffer[src + 243]) + 891L * (u = (long) OPM.opmBuffer[src + 245]) + 131072L >> 18)));
            sndByteBlock[dst + 370] = (byte) t;
            sndByteBlock[dst + 371] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (83259L * v + 164857L * u + 15884L * (v = (long) OPM.opmBuffer[src + 247]) + 131072L >> 18)));
            sndByteBlock[dst + 374] = (byte) t;
            sndByteBlock[dst + 375] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (37004L * u + 177617L * v + 49379L * (u = (long) OPM.opmBuffer[src + 249]) + 131072L >> 18)));
            sndByteBlock[dst + 378] = (byte) t;
            sndByteBlock[dst + 379] = (byte) (t >> 8);
            t = Math.max (-32768, Math.min (32767, (int) (9251L * v + 153373L * u + 101376L * (long) OPM.opmBuffer[src + 251] + 131072L >> 18)));
            sndByteBlock[dst + 382] = (byte) t;
            sndByteBlock[dst + 383] = (byte) (t >> 8);
          }  //for src,dst
        } else {  //float
          //OPMとPCMを合わせてボリュームを掛ける。ついでに後ろに1サンプルずらす
          int l = OPM.opmBuffer[2 * OPM.OPM_BLOCK_SAMPLES    ];  //前回の最後のデータ
          int r = OPM.opmBuffer[2 * OPM.OPM_BLOCK_SAMPLES + 1];
          for (int src = 0; src < 2 * OPM.OPM_BLOCK_SAMPLES; src += 2) {
            int t;
            t = (((OPM.OPM_ON ? OPM.opmBuffer[src] : 0) +
                  (ADPCM.PCM_ON ? ADPCM.pcmBuffer[src] : 0) +
                  (MercuryUnit.MU4_ON ? MercuryUnit.mu4Buffer[src] : 0)) * sndCurrentScale) >> 12;
            OPM.opmBuffer[src] = l;
            l = t;
            t = (((OPM.OPM_ON ? OPM.opmBuffer[src + 1] : 0) +
                  (ADPCM.PCM_ON ? ADPCM.pcmBuffer[src + 1] : 0) +
                  (MercuryUnit.MU4_ON ? MercuryUnit.mu4Buffer[src + 1] : 0)) * sndCurrentScale) >> 12;
            OPM.opmBuffer[src + 1] = r;
            r = t;
            sndCurrentScale += (sndCurrentScale - sndTargetScale >>> 31) - (sndTargetScale - sndCurrentScale >>> 31);
          }
          OPM.opmBuffer[2 * OPM.OPM_BLOCK_SAMPLES    ] = l;  //今回の最後のデータ
          OPM.opmBuffer[2 * OPM.OPM_BLOCK_SAMPLES + 1] = r;
          //線形面積補間
          //  float→intのキャストに飽和処理をさせている
          for (int src = 0, dst = 0; src < 2 * OPM.OPM_BLOCK_SAMPLES; src += 2 * 125, dst += 4 * 96) {
            int t;
            float u, v;
            //  perl -e "$m=125;$n=96;$CH=2;$I=12;sub str{my($t,$lr)=@_;my@s=();for my$k(sort{$a<=>$b}keys%$t){push@s,$t->{$k}.'F * OPM.opmBuffer[src'.($CH*$k+$lr?' + '.($CH*$k+$lr):'').']'}join' + ',@s}sub mul{my($t,$m)=@_;my$p={};for my$k(keys%$t){$p->{$k}=$t->{$k}*$m}$p}sub add{my($t,$u)=@_;my$s={};for my$k(keys%$t){$s->{$k}=$t->{$k}};for my$k(keys%$u){$s->{$k}=($s->{$k}//0)+$u->{$k}}$s}@out=();@w=();for$i(0..$m){$k=$n*$i;$t={};$t->{$i}=1;push@w,[$k,$t]}for$j(0..$n-1){$k=$m*$j;$r=$k%$n;$r or next;$q=($k-$r)/$n;$t={};$t->{$q}=($n-$r)/$n;$t->{$q+1}=$r/$n;push@w,[$k,$t];}@w=sort{$a->[0]<=>$b->[0]}@w;for$lr(0..$CH-1){push@out,sprintf'%*s//%s%c',$I,'',$CH==1?'mid':$lr?'right':'left',10;for$j(0..$n-1){$k=$m*$j;@v=grep{$k<=$_->[0]&&$_->[0]<=$k+$m}@w;$d={};for$i(0..@v-2){$d=add($d,mul(add($v[$i]->[1],$v[$i+1]->[1]),($v[$i+1]->[0]-$v[$i]->[0])))}$d=mul($d,65536/(2*$m));push@out,sprintf'%*st = (int) (%s + 32768F) >> 16;%c',$I,'',str($d,$lr),10;push@out,sprintf'%*ssndByteBlock[dst'.(2*($CH*$j+$lr)+0?' + '.(2*($CH*$j+$lr)+0):'').'] = (byte) t;%c',$I,'',10;push@out,sprintf'%*ssndByteBlock[dst + '.(2*($CH*$j+$lr)+1).'] = (byte) (t >> 8);%c',$I,'',10;}}$out=join'',@out;$out2='';%flag=();while($out=~/OPM.opmBuffer\[src(?: \+ (\d+))?\]/){$out2.=$`;$out=$';$s=$&;$i=int(($1//0)/$CH);if($i==0||$i==$m){$out2.='(float) '.$s}else{$uv=$i%2?'v':'u';if(exists$flag{$s}){$out2.=$uv}else{$flag{$s}=1;$out2.='('.$uv.' = (float) '.$s.')'}}}$out2.=$out;print$out2"
            //left
            t = (int) (25165.824F * (float) OPM.opmBuffer[src] + 38073.6853333333F * (v = (float) OPM.opmBuffer[src + 2]) + 2296.49066666667F * (u = (float) OPM.opmBuffer[src + 4]) + 32768F) >> 16;
            sndByteBlock[dst] = (byte) t;
            sndByteBlock[dst + 1] = (byte) (t >> 8);
            t = (int) (12257.9626666667F * v + 44092.0746666667F * u + 9185.96266666667F * (v = (float) OPM.opmBuffer[src + 6]) + 32768F) >> 16;
            sndByteBlock[dst + 4] = (byte) t;
            sndByteBlock[dst + 5] = (byte) (t >> 8);
            t = (int) (3943.08266666667F * u + 40924.5013333333F * v + 20668.416F * (u = (float) OPM.opmBuffer[src + 8]) + 32768F) >> 16;
            sndByteBlock[dst + 8] = (byte) t;
            sndByteBlock[dst + 9] = (byte) (t >> 8);
            t = (int) (221.184F * v + 29663.232F * u + 34559.3173333333F * (v = (float) OPM.opmBuffer[src + 10]) + 1092.26666666667F * (u = (float) OPM.opmBuffer[src + 12]) + 32768F) >> 16;
            sndByteBlock[dst + 12] = (byte) t;
            sndByteBlock[dst + 13] = (byte) (t >> 8);
            t = (int) (15772.3306666667F * v + 43207.3386666667F * u + 6556.33066666667F * (v = (float) OPM.opmBuffer[src + 14]) + 32768F) >> 16;
            sndByteBlock[dst + 16] = (byte) t;
            sndByteBlock[dst + 17] = (byte) (t >> 8);
            t = (int) (6032.04266666667F * u + 42890.5813333333F * v + 16613.376F * (u = (float) OPM.opmBuffer[src + 16]) + 32768F) >> 16;
            sndByteBlock[dst + 20] = (byte) t;
            sndByteBlock[dst + 21] = (byte) (t >> 8);
            t = (int) (884.736F * v + 33718.272F * u + 30602.5813333333F * (v = (float) OPM.opmBuffer[src + 18]) + 330.410666666667F * (u = (float) OPM.opmBuffer[src + 20]) + 32768F) >> 16;
            sndByteBlock[dst + 24] = (byte) t;
            sndByteBlock[dst + 25] = (byte) (t >> 8);
            t = (int) (19729.0666666667F * v + 41437.8666666667F * u + 4369.06666666667F * (v = (float) OPM.opmBuffer[src + 22]) + 32768F) >> 16;
            sndByteBlock[dst + 28] = (byte) t;
            sndByteBlock[dst + 29] = (byte) (t >> 8);
            t = (int) (8563.37066666667F * u + 43971.9253333333F * v + 13000.704F * (u = (float) OPM.opmBuffer[src + 24]) + 32768F) >> 16;
            sndByteBlock[dst + 32] = (byte) t;
            sndByteBlock[dst + 33] = (byte) (t >> 8);
            t = (int) (1990.656F * v + 37330.944F * u + 26203.4773333333F * (v = (float) OPM.opmBuffer[src + 26]) + 10.9226666666667F * (u = (float) OPM.opmBuffer[src + 28]) + 32768F) >> 16;
            sndByteBlock[dst + 36] = (byte) t;
            sndByteBlock[dst + 37] = (byte) (t >> 8);
            t = (int) (24128.1706666667F * v + 38783.6586666667F * u + 2624.17066666667F * (v = (float) OPM.opmBuffer[src + 30]) + 32768F) >> 16;
            sndByteBlock[dst + 40] = (byte) t;
            sndByteBlock[dst + 41] = (byte) (t >> 8);
            t = (int) (11537.0666666667F * u + 44168.5333333333F * v + 9830.4F * (u = (float) OPM.opmBuffer[src + 32]) + 32768F) >> 16;
            sndByteBlock[dst + 44] = (byte) t;
            sndByteBlock[dst + 45] = (byte) (t >> 8);
            t = (int) (3538.944F * v + 40367.4453333333F * u + 21629.6106666667F * (v = (float) OPM.opmBuffer[src + 34]) + 32768F) >> 16;
            sndByteBlock[dst + 48] = (byte) t;
            sndByteBlock[dst + 49] = (byte) (t >> 8);
            t = (int) (133.802666666667F * u + 28702.0373333333F * v + 35378.5173333333F * (u = (float) OPM.opmBuffer[src + 36]) + 1321.64266666667F * (v = (float) OPM.opmBuffer[src + 38]) + 32768F) >> 16;
            sndByteBlock[dst + 52] = (byte) t;
            sndByteBlock[dst + 53] = (byte) (t >> 8);
            t = (int) (14953.1306666667F * u + 43480.4053333333F * v + 7102.464F * (u = (float) OPM.opmBuffer[src + 40]) + 32768F) >> 16;
            sndByteBlock[dst + 56] = (byte) t;
            sndByteBlock[dst + 57] = (byte) (t >> 8);
            t = (int) (5529.6F * v + 42530.1333333333F * u + 17476.2666666667F * (v = (float) OPM.opmBuffer[src + 42]) + 32768F) >> 16;
            sndByteBlock[dst + 60] = (byte) t;
            sndByteBlock[dst + 61] = (byte) (t >> 8);
            t = (int) (699.050666666667F * u + 32855.3813333333F * v + 31520.0853333333F * (u = (float) OPM.opmBuffer[src + 44]) + 461.482666666667F * (v = (float) OPM.opmBuffer[src + 46]) + 32768F) >> 16;
            sndByteBlock[dst + 64] = (byte) t;
            sndByteBlock[dst + 65] = (byte) (t >> 8);
            t = (int) (18811.5626666667F * u + 41907.5413333333F * v + 4816.896F * (u = (float) OPM.opmBuffer[src + 48]) + 32768F) >> 16;
            sndByteBlock[dst + 68] = (byte) t;
            sndByteBlock[dst + 69] = (byte) (t >> 8);
            t = (int) (7962.624F * v + 43808.0853333333F * u + 13765.2906666667F * (v = (float) OPM.opmBuffer[src + 50]) + 32768F) >> 16;
            sndByteBlock[dst + 72] = (byte) t;
            sndByteBlock[dst + 73] = (byte) (t >> 8);
            t = (int) (1706.66666666667F * u + 36566.3573333333F * v + 27219.2853333333F * (u = (float) OPM.opmBuffer[src + 52]) + 43.6906666666667F * (v = (float) OPM.opmBuffer[src + 54]) + 32768F) >> 16;
            sndByteBlock[dst + 76] = (byte) t;
            sndByteBlock[dst + 77] = (byte) (t >> 8);
            t = (int) (23112.3626666667F * u + 39449.9413333333F * v + 2973.696F * (u = (float) OPM.opmBuffer[src + 56]) + 32768F) >> 16;
            sndByteBlock[dst + 80] = (byte) t;
            sndByteBlock[dst + 81] = (byte) (t >> 8);
            t = (int) (10838.016F * v + 44201.3013333333F * u + 10496.6826666667F * (v = (float) OPM.opmBuffer[src + 58]) + 32768F) >> 16;
            sndByteBlock[dst + 84] = (byte) t;
            sndByteBlock[dst + 85] = (byte) (t >> 8);
            t = (int) (3156.65066666667F * u + 39766.6986666667F * v + 22612.6506666667F * (u = (float) OPM.opmBuffer[src + 60]) + 32768F) >> 16;
            sndByteBlock[dst + 88] = (byte) t;
            sndByteBlock[dst + 89] = (byte) (t >> 8);
            t = (int) (68.2666666666667F * v + 27718.9973333333F * u + 36175.872F * (v = (float) OPM.opmBuffer[src + 62]) + 1572.864F * (u = (float) OPM.opmBuffer[src + 64]) + 32768F) >> 16;
            sndByteBlock[dst + 92] = (byte) t;
            sndByteBlock[dst + 93] = (byte) (t >> 8);
            t = (int) (14155.776F * v + 43709.7813333333F * u + 7670.44266666667F * (v = (float) OPM.opmBuffer[src + 66]) + 32768F) >> 16;
            sndByteBlock[dst + 96] = (byte) t;
            sndByteBlock[dst + 97] = (byte) (t >> 8);
            t = (int) (5049.00266666667F * u + 42125.9946666667F * v + 18361.0026666667F * (u = (float) OPM.opmBuffer[src + 68]) + 32768F) >> 16;
            sndByteBlock[dst + 100] = (byte) t;
            sndByteBlock[dst + 101] = (byte) (t >> 8);
            t = (int) (535.210666666667F * v + 31970.6453333333F * u + 32415.744F * (v = (float) OPM.opmBuffer[src + 70]) + 614.4F * (u = (float) OPM.opmBuffer[src + 72]) + 32768F) >> 16;
            sndByteBlock[dst + 104] = (byte) t;
            sndByteBlock[dst + 105] = (byte) (t >> 8);
            t = (int) (17915.904F * v + 42333.5253333333F * u + 5286.57066666667F * (v = (float) OPM.opmBuffer[src + 74]) + 32768F) >> 16;
            sndByteBlock[dst + 108] = (byte) t;
            sndByteBlock[dst + 109] = (byte) (t >> 8);
            t = (int) (7383.72266666667F * u + 43600.5546666667F * v + 14551.7226666667F * (u = (float) OPM.opmBuffer[src + 76]) + 32768F) >> 16;
            sndByteBlock[dst + 112] = (byte) t;
            sndByteBlock[dst + 113] = (byte) (t >> 8);
            t = (int) (1444.52266666667F * v + 35779.9253333333F * u + 28213.248F * (v = (float) OPM.opmBuffer[src + 78]) + 98.304F * (u = (float) OPM.opmBuffer[src + 80]) + 32768F) >> 16;
            sndByteBlock[dst + 116] = (byte) t;
            sndByteBlock[dst + 117] = (byte) (t >> 8);
            t = (int) (22118.4F * v + 40072.5333333333F * u + 3345.06666666667F * (v = (float) OPM.opmBuffer[src + 82]) + 32768F) >> 16;
            sndByteBlock[dst + 120] = (byte) t;
            sndByteBlock[dst + 121] = (byte) (t >> 8);
            t = (int) (10160.8106666667F * u + 44190.3786666667F * v + 11184.8106666667F * (u = (float) OPM.opmBuffer[src + 84]) + 32768F) >> 16;
            sndByteBlock[dst + 124] = (byte) t;
            sndByteBlock[dst + 125] = (byte) (t >> 8);
            t = (int) (2796.20266666667F * v + 39122.2613333333F * u + 23617.536F * (v = (float) OPM.opmBuffer[src + 86]) + 32768F) >> 16;
            sndByteBlock[dst + 128] = (byte) t;
            sndByteBlock[dst + 129] = (byte) (t >> 8);
            t = (int) (24.576F * u + 26714.112F * v + 36951.3813333333F * (u = (float) OPM.opmBuffer[src + 88]) + 1845.93066666667F * (v = (float) OPM.opmBuffer[src + 90]) + 32768F) >> 16;
            sndByteBlock[dst + 132] = (byte) t;
            sndByteBlock[dst + 133] = (byte) (t >> 8);
            t = (int) (13380.2666666667F * u + 43895.4666666667F * v + 8260.26666666667F * (u = (float) OPM.opmBuffer[src + 92]) + 32768F) >> 16;
            sndByteBlock[dst + 136] = (byte) t;
            sndByteBlock[dst + 137] = (byte) (t >> 8);
            t = (int) (4590.25066666667F * v + 41678.1653333333F * u + 19267.584F * (v = (float) OPM.opmBuffer[src + 94]) + 32768F) >> 16;
            sndByteBlock[dst + 140] = (byte) t;
            sndByteBlock[dst + 141] = (byte) (t >> 8);
            t = (int) (393.216F * u + 31064.064F * v + 33289.5573333333F * (u = (float) OPM.opmBuffer[src + 96]) + 789.162666666667F * (v = (float) OPM.opmBuffer[src + 98]) + 32768F) >> 16;
            sndByteBlock[dst + 144] = (byte) t;
            sndByteBlock[dst + 145] = (byte) (t >> 8);
            t = (int) (17042.0906666667F * u + 42715.8186666667F * v + 5778.09066666667F * (u = (float) OPM.opmBuffer[src + 100]) + 32768F) >> 16;
            sndByteBlock[dst + 148] = (byte) t;
            sndByteBlock[dst + 149] = (byte) (t >> 8);
            t = (int) (6826.66666666667F * v + 43349.3333333333F * u + 15360F * (v = (float) OPM.opmBuffer[src + 102]) + 32768F) >> 16;
            sndByteBlock[dst + 152] = (byte) t;
            sndByteBlock[dst + 153] = (byte) (t >> 8);
            t = (int) (1204.224F * u + 34971.648F * v + 29185.3653333333F * (u = (float) OPM.opmBuffer[src + 104]) + 174.762666666667F * (v = (float) OPM.opmBuffer[src + 106]) + 32768F) >> 16;
            sndByteBlock[dst + 156] = (byte) t;
            sndByteBlock[dst + 157] = (byte) (t >> 8);
            t = (int) (21146.2826666667F * u + 40651.4346666667F * v + 3738.28266666667F * (u = (float) OPM.opmBuffer[src + 108]) + 32768F) >> 16;
            sndByteBlock[dst + 160] = (byte) t;
            sndByteBlock[dst + 161] = (byte) (t >> 8);
            t = (int) (9505.45066666667F * v + 44135.7653333333F * u + 11894.784F * (v = (float) OPM.opmBuffer[src + 110]) + 32768F) >> 16;
            sndByteBlock[dst + 164] = (byte) t;
            sndByteBlock[dst + 165] = (byte) (t >> 8);
            t = (int) (2457.6F * u + 38434.1333333333F * v + 24644.2666666667F * (u = (float) OPM.opmBuffer[src + 112]) + 32768F) >> 16;
            sndByteBlock[dst + 168] = (byte) t;
            sndByteBlock[dst + 169] = (byte) (t >> 8);
            t = (int) (2.73066666666667F * v + 25687.3813333333F * u + 37705.0453333333F * (v = (float) OPM.opmBuffer[src + 114]) + 2140.84266666667F * (u = (float) OPM.opmBuffer[src + 116]) + 32768F) >> 16;
            sndByteBlock[dst + 172] = (byte) t;
            sndByteBlock[dst + 173] = (byte) (t >> 8);
            t = (int) (12626.6026666667F * v + 44037.4613333333F * u + 8871.936F * (v = (float) OPM.opmBuffer[src + 118]) + 32768F) >> 16;
            sndByteBlock[dst + 176] = (byte) t;
            sndByteBlock[dst + 177] = (byte) (t >> 8);
            t = (int) (4153.344F * u + 41186.6453333333F * v + 20196.0106666667F * (u = (float) OPM.opmBuffer[src + 120]) + 32768F) >> 16;
            sndByteBlock[dst + 180] = (byte) t;
            sndByteBlock[dst + 181] = (byte) (t >> 8);
            t = (int) (273.066666666667F * v + 30135.6373333333F * u + 34141.5253333333F * (v = (float) OPM.opmBuffer[src + 122]) + 985.770666666667F * (u = (float) OPM.opmBuffer[src + 124]) + 32768F) >> 16;
            sndByteBlock[dst + 184] = (byte) t;
            sndByteBlock[dst + 185] = (byte) (t >> 8);
            t = (int) (16190.1226666667F * v + 43054.4213333333F * u + 6291.456F * (v = (float) OPM.opmBuffer[src + 126]) + 32768F) >> 16;
            sndByteBlock[dst + 188] = (byte) t;
            sndByteBlock[dst + 189] = (byte) (t >> 8);
            t = (int) (6291.456F * u + 43054.4213333333F * v + 16190.1226666667F * (u = (float) OPM.opmBuffer[src + 128]) + 32768F) >> 16;
            sndByteBlock[dst + 192] = (byte) t;
            sndByteBlock[dst + 193] = (byte) (t >> 8);
            t = (int) (985.770666666667F * v + 34141.5253333333F * u + 30135.6373333333F * (v = (float) OPM.opmBuffer[src + 130]) + 273.066666666667F * (u = (float) OPM.opmBuffer[src + 132]) + 32768F) >> 16;
            sndByteBlock[dst + 196] = (byte) t;
            sndByteBlock[dst + 197] = (byte) (t >> 8);
            t = (int) (20196.0106666667F * v + 41186.6453333333F * u + 4153.344F * (v = (float) OPM.opmBuffer[src + 134]) + 32768F) >> 16;
            sndByteBlock[dst + 200] = (byte) t;
            sndByteBlock[dst + 201] = (byte) (t >> 8);
            t = (int) (8871.936F * u + 44037.4613333333F * v + 12626.6026666667F * (u = (float) OPM.opmBuffer[src + 136]) + 32768F) >> 16;
            sndByteBlock[dst + 204] = (byte) t;
            sndByteBlock[dst + 205] = (byte) (t >> 8);
            t = (int) (2140.84266666667F * v + 37705.0453333333F * u + 25687.3813333333F * (v = (float) OPM.opmBuffer[src + 138]) + 2.73066666666667F * (u = (float) OPM.opmBuffer[src + 140]) + 32768F) >> 16;
            sndByteBlock[dst + 208] = (byte) t;
            sndByteBlock[dst + 209] = (byte) (t >> 8);
            t = (int) (24644.2666666667F * v + 38434.1333333333F * u + 2457.6F * (v = (float) OPM.opmBuffer[src + 142]) + 32768F) >> 16;
            sndByteBlock[dst + 212] = (byte) t;
            sndByteBlock[dst + 213] = (byte) (t >> 8);
            t = (int) (11894.784F * u + 44135.7653333333F * v + 9505.45066666667F * (u = (float) OPM.opmBuffer[src + 144]) + 32768F) >> 16;
            sndByteBlock[dst + 216] = (byte) t;
            sndByteBlock[dst + 217] = (byte) (t >> 8);
            t = (int) (3738.28266666667F * v + 40651.4346666667F * u + 21146.2826666667F * (v = (float) OPM.opmBuffer[src + 146]) + 32768F) >> 16;
            sndByteBlock[dst + 220] = (byte) t;
            sndByteBlock[dst + 221] = (byte) (t >> 8);
            t = (int) (174.762666666667F * u + 29185.3653333333F * v + 34971.648F * (u = (float) OPM.opmBuffer[src + 148]) + 1204.224F * (v = (float) OPM.opmBuffer[src + 150]) + 32768F) >> 16;
            sndByteBlock[dst + 224] = (byte) t;
            sndByteBlock[dst + 225] = (byte) (t >> 8);
            t = (int) (15360F * u + 43349.3333333333F * v + 6826.66666666667F * (u = (float) OPM.opmBuffer[src + 152]) + 32768F) >> 16;
            sndByteBlock[dst + 228] = (byte) t;
            sndByteBlock[dst + 229] = (byte) (t >> 8);
            t = (int) (5778.09066666667F * v + 42715.8186666667F * u + 17042.0906666667F * (v = (float) OPM.opmBuffer[src + 154]) + 32768F) >> 16;
            sndByteBlock[dst + 232] = (byte) t;
            sndByteBlock[dst + 233] = (byte) (t >> 8);
            t = (int) (789.162666666667F * u + 33289.5573333333F * v + 31064.064F * (u = (float) OPM.opmBuffer[src + 156]) + 393.216F * (v = (float) OPM.opmBuffer[src + 158]) + 32768F) >> 16;
            sndByteBlock[dst + 236] = (byte) t;
            sndByteBlock[dst + 237] = (byte) (t >> 8);
            t = (int) (19267.584F * u + 41678.1653333333F * v + 4590.25066666667F * (u = (float) OPM.opmBuffer[src + 160]) + 32768F) >> 16;
            sndByteBlock[dst + 240] = (byte) t;
            sndByteBlock[dst + 241] = (byte) (t >> 8);
            t = (int) (8260.26666666667F * v + 43895.4666666667F * u + 13380.2666666667F * (v = (float) OPM.opmBuffer[src + 162]) + 32768F) >> 16;
            sndByteBlock[dst + 244] = (byte) t;
            sndByteBlock[dst + 245] = (byte) (t >> 8);
            t = (int) (1845.93066666667F * u + 36951.3813333333F * v + 26714.112F * (u = (float) OPM.opmBuffer[src + 164]) + 24.576F * (v = (float) OPM.opmBuffer[src + 166]) + 32768F) >> 16;
            sndByteBlock[dst + 248] = (byte) t;
            sndByteBlock[dst + 249] = (byte) (t >> 8);
            t = (int) (23617.536F * u + 39122.2613333333F * v + 2796.20266666667F * (u = (float) OPM.opmBuffer[src + 168]) + 32768F) >> 16;
            sndByteBlock[dst + 252] = (byte) t;
            sndByteBlock[dst + 253] = (byte) (t >> 8);
            t = (int) (11184.8106666667F * v + 44190.3786666667F * u + 10160.8106666667F * (v = (float) OPM.opmBuffer[src + 170]) + 32768F) >> 16;
            sndByteBlock[dst + 256] = (byte) t;
            sndByteBlock[dst + 257] = (byte) (t >> 8);
            t = (int) (3345.06666666667F * u + 40072.5333333333F * v + 22118.4F * (u = (float) OPM.opmBuffer[src + 172]) + 32768F) >> 16;
            sndByteBlock[dst + 260] = (byte) t;
            sndByteBlock[dst + 261] = (byte) (t >> 8);
            t = (int) (98.304F * v + 28213.248F * u + 35779.9253333333F * (v = (float) OPM.opmBuffer[src + 174]) + 1444.52266666667F * (u = (float) OPM.opmBuffer[src + 176]) + 32768F) >> 16;
            sndByteBlock[dst + 264] = (byte) t;
            sndByteBlock[dst + 265] = (byte) (t >> 8);
            t = (int) (14551.7226666667F * v + 43600.5546666667F * u + 7383.72266666667F * (v = (float) OPM.opmBuffer[src + 178]) + 32768F) >> 16;
            sndByteBlock[dst + 268] = (byte) t;
            sndByteBlock[dst + 269] = (byte) (t >> 8);
            t = (int) (5286.57066666667F * u + 42333.5253333333F * v + 17915.904F * (u = (float) OPM.opmBuffer[src + 180]) + 32768F) >> 16;
            sndByteBlock[dst + 272] = (byte) t;
            sndByteBlock[dst + 273] = (byte) (t >> 8);
            t = (int) (614.4F * v + 32415.744F * u + 31970.6453333333F * (v = (float) OPM.opmBuffer[src + 182]) + 535.210666666667F * (u = (float) OPM.opmBuffer[src + 184]) + 32768F) >> 16;
            sndByteBlock[dst + 276] = (byte) t;
            sndByteBlock[dst + 277] = (byte) (t >> 8);
            t = (int) (18361.0026666667F * v + 42125.9946666667F * u + 5049.00266666667F * (v = (float) OPM.opmBuffer[src + 186]) + 32768F) >> 16;
            sndByteBlock[dst + 280] = (byte) t;
            sndByteBlock[dst + 281] = (byte) (t >> 8);
            t = (int) (7670.44266666667F * u + 43709.7813333333F * v + 14155.776F * (u = (float) OPM.opmBuffer[src + 188]) + 32768F) >> 16;
            sndByteBlock[dst + 284] = (byte) t;
            sndByteBlock[dst + 285] = (byte) (t >> 8);
            t = (int) (1572.864F * v + 36175.872F * u + 27718.9973333333F * (v = (float) OPM.opmBuffer[src + 190]) + 68.2666666666667F * (u = (float) OPM.opmBuffer[src + 192]) + 32768F) >> 16;
            sndByteBlock[dst + 288] = (byte) t;
            sndByteBlock[dst + 289] = (byte) (t >> 8);
            t = (int) (22612.6506666667F * v + 39766.6986666667F * u + 3156.65066666667F * (v = (float) OPM.opmBuffer[src + 194]) + 32768F) >> 16;
            sndByteBlock[dst + 292] = (byte) t;
            sndByteBlock[dst + 293] = (byte) (t >> 8);
            t = (int) (10496.6826666667F * u + 44201.3013333333F * v + 10838.016F * (u = (float) OPM.opmBuffer[src + 196]) + 32768F) >> 16;
            sndByteBlock[dst + 296] = (byte) t;
            sndByteBlock[dst + 297] = (byte) (t >> 8);
            t = (int) (2973.696F * v + 39449.9413333333F * u + 23112.3626666667F * (v = (float) OPM.opmBuffer[src + 198]) + 32768F) >> 16;
            sndByteBlock[dst + 300] = (byte) t;
            sndByteBlock[dst + 301] = (byte) (t >> 8);
            t = (int) (43.6906666666667F * u + 27219.2853333333F * v + 36566.3573333333F * (u = (float) OPM.opmBuffer[src + 200]) + 1706.66666666667F * (v = (float) OPM.opmBuffer[src + 202]) + 32768F) >> 16;
            sndByteBlock[dst + 304] = (byte) t;
            sndByteBlock[dst + 305] = (byte) (t >> 8);
            t = (int) (13765.2906666667F * u + 43808.0853333333F * v + 7962.624F * (u = (float) OPM.opmBuffer[src + 204]) + 32768F) >> 16;
            sndByteBlock[dst + 308] = (byte) t;
            sndByteBlock[dst + 309] = (byte) (t >> 8);
            t = (int) (4816.896F * v + 41907.5413333333F * u + 18811.5626666667F * (v = (float) OPM.opmBuffer[src + 206]) + 32768F) >> 16;
            sndByteBlock[dst + 312] = (byte) t;
            sndByteBlock[dst + 313] = (byte) (t >> 8);
            t = (int) (461.482666666667F * u + 31520.0853333333F * v + 32855.3813333333F * (u = (float) OPM.opmBuffer[src + 208]) + 699.050666666667F * (v = (float) OPM.opmBuffer[src + 210]) + 32768F) >> 16;
            sndByteBlock[dst + 316] = (byte) t;
            sndByteBlock[dst + 317] = (byte) (t >> 8);
            t = (int) (17476.2666666667F * u + 42530.1333333333F * v + 5529.6F * (u = (float) OPM.opmBuffer[src + 212]) + 32768F) >> 16;
            sndByteBlock[dst + 320] = (byte) t;
            sndByteBlock[dst + 321] = (byte) (t >> 8);
            t = (int) (7102.464F * v + 43480.4053333333F * u + 14953.1306666667F * (v = (float) OPM.opmBuffer[src + 214]) + 32768F) >> 16;
            sndByteBlock[dst + 324] = (byte) t;
            sndByteBlock[dst + 325] = (byte) (t >> 8);
            t = (int) (1321.64266666667F * u + 35378.5173333333F * v + 28702.0373333333F * (u = (float) OPM.opmBuffer[src + 216]) + 133.802666666667F * (v = (float) OPM.opmBuffer[src + 218]) + 32768F) >> 16;
            sndByteBlock[dst + 328] = (byte) t;
            sndByteBlock[dst + 329] = (byte) (t >> 8);
            t = (int) (21629.6106666667F * u + 40367.4453333333F * v + 3538.944F * (u = (float) OPM.opmBuffer[src + 220]) + 32768F) >> 16;
            sndByteBlock[dst + 332] = (byte) t;
            sndByteBlock[dst + 333] = (byte) (t >> 8);
            t = (int) (9830.4F * v + 44168.5333333333F * u + 11537.0666666667F * (v = (float) OPM.opmBuffer[src + 222]) + 32768F) >> 16;
            sndByteBlock[dst + 336] = (byte) t;
            sndByteBlock[dst + 337] = (byte) (t >> 8);
            t = (int) (2624.17066666667F * u + 38783.6586666667F * v + 24128.1706666667F * (u = (float) OPM.opmBuffer[src + 224]) + 32768F) >> 16;
            sndByteBlock[dst + 340] = (byte) t;
            sndByteBlock[dst + 341] = (byte) (t >> 8);
            t = (int) (10.9226666666667F * v + 26203.4773333333F * u + 37330.944F * (v = (float) OPM.opmBuffer[src + 226]) + 1990.656F * (u = (float) OPM.opmBuffer[src + 228]) + 32768F) >> 16;
            sndByteBlock[dst + 344] = (byte) t;
            sndByteBlock[dst + 345] = (byte) (t >> 8);
            t = (int) (13000.704F * v + 43971.9253333333F * u + 8563.37066666667F * (v = (float) OPM.opmBuffer[src + 230]) + 32768F) >> 16;
            sndByteBlock[dst + 348] = (byte) t;
            sndByteBlock[dst + 349] = (byte) (t >> 8);
            t = (int) (4369.06666666667F * u + 41437.8666666667F * v + 19729.0666666667F * (u = (float) OPM.opmBuffer[src + 232]) + 32768F) >> 16;
            sndByteBlock[dst + 352] = (byte) t;
            sndByteBlock[dst + 353] = (byte) (t >> 8);
            t = (int) (330.410666666667F * v + 30602.5813333333F * u + 33718.272F * (v = (float) OPM.opmBuffer[src + 234]) + 884.736F * (u = (float) OPM.opmBuffer[src + 236]) + 32768F) >> 16;
            sndByteBlock[dst + 356] = (byte) t;
            sndByteBlock[dst + 357] = (byte) (t >> 8);
            t = (int) (16613.376F * v + 42890.5813333333F * u + 6032.04266666667F * (v = (float) OPM.opmBuffer[src + 238]) + 32768F) >> 16;
            sndByteBlock[dst + 360] = (byte) t;
            sndByteBlock[dst + 361] = (byte) (t >> 8);
            t = (int) (6556.33066666667F * u + 43207.3386666667F * v + 15772.3306666667F * (u = (float) OPM.opmBuffer[src + 240]) + 32768F) >> 16;
            sndByteBlock[dst + 364] = (byte) t;
            sndByteBlock[dst + 365] = (byte) (t >> 8);
            t = (int) (1092.26666666667F * v + 34559.3173333333F * u + 29663.232F * (v = (float) OPM.opmBuffer[src + 242]) + 221.184F * (u = (float) OPM.opmBuffer[src + 244]) + 32768F) >> 16;
            sndByteBlock[dst + 368] = (byte) t;
            sndByteBlock[dst + 369] = (byte) (t >> 8);
            t = (int) (20668.416F * v + 40924.5013333333F * u + 3943.08266666667F * (v = (float) OPM.opmBuffer[src + 246]) + 32768F) >> 16;
            sndByteBlock[dst + 372] = (byte) t;
            sndByteBlock[dst + 373] = (byte) (t >> 8);
            t = (int) (9185.96266666667F * u + 44092.0746666667F * v + 12257.9626666667F * (u = (float) OPM.opmBuffer[src + 248]) + 32768F) >> 16;
            sndByteBlock[dst + 376] = (byte) t;
            sndByteBlock[dst + 377] = (byte) (t >> 8);
            t = (int) (2296.49066666667F * v + 38073.6853333333F * u + 25165.824F * (float) OPM.opmBuffer[src + 250] + 32768F) >> 16;
            sndByteBlock[dst + 380] = (byte) t;
            sndByteBlock[dst + 381] = (byte) (t >> 8);
            //right
            t = (int) (25165.824F * (float) OPM.opmBuffer[src + 1] + 38073.6853333333F * (v = (float) OPM.opmBuffer[src + 3]) + 2296.49066666667F * (u = (float) OPM.opmBuffer[src + 5]) + 32768F) >> 16;
            sndByteBlock[dst + 2] = (byte) t;
            sndByteBlock[dst + 3] = (byte) (t >> 8);
            t = (int) (12257.9626666667F * v + 44092.0746666667F * u + 9185.96266666667F * (v = (float) OPM.opmBuffer[src + 7]) + 32768F) >> 16;
            sndByteBlock[dst + 6] = (byte) t;
            sndByteBlock[dst + 7] = (byte) (t >> 8);
            t = (int) (3943.08266666667F * u + 40924.5013333333F * v + 20668.416F * (u = (float) OPM.opmBuffer[src + 9]) + 32768F) >> 16;
            sndByteBlock[dst + 10] = (byte) t;
            sndByteBlock[dst + 11] = (byte) (t >> 8);
            t = (int) (221.184F * v + 29663.232F * u + 34559.3173333333F * (v = (float) OPM.opmBuffer[src + 11]) + 1092.26666666667F * (u = (float) OPM.opmBuffer[src + 13]) + 32768F) >> 16;
            sndByteBlock[dst + 14] = (byte) t;
            sndByteBlock[dst + 15] = (byte) (t >> 8);
            t = (int) (15772.3306666667F * v + 43207.3386666667F * u + 6556.33066666667F * (v = (float) OPM.opmBuffer[src + 15]) + 32768F) >> 16;
            sndByteBlock[dst + 18] = (byte) t;
            sndByteBlock[dst + 19] = (byte) (t >> 8);
            t = (int) (6032.04266666667F * u + 42890.5813333333F * v + 16613.376F * (u = (float) OPM.opmBuffer[src + 17]) + 32768F) >> 16;
            sndByteBlock[dst + 22] = (byte) t;
            sndByteBlock[dst + 23] = (byte) (t >> 8);
            t = (int) (884.736F * v + 33718.272F * u + 30602.5813333333F * (v = (float) OPM.opmBuffer[src + 19]) + 330.410666666667F * (u = (float) OPM.opmBuffer[src + 21]) + 32768F) >> 16;
            sndByteBlock[dst + 26] = (byte) t;
            sndByteBlock[dst + 27] = (byte) (t >> 8);
            t = (int) (19729.0666666667F * v + 41437.8666666667F * u + 4369.06666666667F * (v = (float) OPM.opmBuffer[src + 23]) + 32768F) >> 16;
            sndByteBlock[dst + 30] = (byte) t;
            sndByteBlock[dst + 31] = (byte) (t >> 8);
            t = (int) (8563.37066666667F * u + 43971.9253333333F * v + 13000.704F * (u = (float) OPM.opmBuffer[src + 25]) + 32768F) >> 16;
            sndByteBlock[dst + 34] = (byte) t;
            sndByteBlock[dst + 35] = (byte) (t >> 8);
            t = (int) (1990.656F * v + 37330.944F * u + 26203.4773333333F * (v = (float) OPM.opmBuffer[src + 27]) + 10.9226666666667F * (u = (float) OPM.opmBuffer[src + 29]) + 32768F) >> 16;
            sndByteBlock[dst + 38] = (byte) t;
            sndByteBlock[dst + 39] = (byte) (t >> 8);
            t = (int) (24128.1706666667F * v + 38783.6586666667F * u + 2624.17066666667F * (v = (float) OPM.opmBuffer[src + 31]) + 32768F) >> 16;
            sndByteBlock[dst + 42] = (byte) t;
            sndByteBlock[dst + 43] = (byte) (t >> 8);
            t = (int) (11537.0666666667F * u + 44168.5333333333F * v + 9830.4F * (u = (float) OPM.opmBuffer[src + 33]) + 32768F) >> 16;
            sndByteBlock[dst + 46] = (byte) t;
            sndByteBlock[dst + 47] = (byte) (t >> 8);
            t = (int) (3538.944F * v + 40367.4453333333F * u + 21629.6106666667F * (v = (float) OPM.opmBuffer[src + 35]) + 32768F) >> 16;
            sndByteBlock[dst + 50] = (byte) t;
            sndByteBlock[dst + 51] = (byte) (t >> 8);
            t = (int) (133.802666666667F * u + 28702.0373333333F * v + 35378.5173333333F * (u = (float) OPM.opmBuffer[src + 37]) + 1321.64266666667F * (v = (float) OPM.opmBuffer[src + 39]) + 32768F) >> 16;
            sndByteBlock[dst + 54] = (byte) t;
            sndByteBlock[dst + 55] = (byte) (t >> 8);
            t = (int) (14953.1306666667F * u + 43480.4053333333F * v + 7102.464F * (u = (float) OPM.opmBuffer[src + 41]) + 32768F) >> 16;
            sndByteBlock[dst + 58] = (byte) t;
            sndByteBlock[dst + 59] = (byte) (t >> 8);
            t = (int) (5529.6F * v + 42530.1333333333F * u + 17476.2666666667F * (v = (float) OPM.opmBuffer[src + 43]) + 32768F) >> 16;
            sndByteBlock[dst + 62] = (byte) t;
            sndByteBlock[dst + 63] = (byte) (t >> 8);
            t = (int) (699.050666666667F * u + 32855.3813333333F * v + 31520.0853333333F * (u = (float) OPM.opmBuffer[src + 45]) + 461.482666666667F * (v = (float) OPM.opmBuffer[src + 47]) + 32768F) >> 16;
            sndByteBlock[dst + 66] = (byte) t;
            sndByteBlock[dst + 67] = (byte) (t >> 8);
            t = (int) (18811.5626666667F * u + 41907.5413333333F * v + 4816.896F * (u = (float) OPM.opmBuffer[src + 49]) + 32768F) >> 16;
            sndByteBlock[dst + 70] = (byte) t;
            sndByteBlock[dst + 71] = (byte) (t >> 8);
            t = (int) (7962.624F * v + 43808.0853333333F * u + 13765.2906666667F * (v = (float) OPM.opmBuffer[src + 51]) + 32768F) >> 16;
            sndByteBlock[dst + 74] = (byte) t;
            sndByteBlock[dst + 75] = (byte) (t >> 8);
            t = (int) (1706.66666666667F * u + 36566.3573333333F * v + 27219.2853333333F * (u = (float) OPM.opmBuffer[src + 53]) + 43.6906666666667F * (v = (float) OPM.opmBuffer[src + 55]) + 32768F) >> 16;
            sndByteBlock[dst + 78] = (byte) t;
            sndByteBlock[dst + 79] = (byte) (t >> 8);
            t = (int) (23112.3626666667F * u + 39449.9413333333F * v + 2973.696F * (u = (float) OPM.opmBuffer[src + 57]) + 32768F) >> 16;
            sndByteBlock[dst + 82] = (byte) t;
            sndByteBlock[dst + 83] = (byte) (t >> 8);
            t = (int) (10838.016F * v + 44201.3013333333F * u + 10496.6826666667F * (v = (float) OPM.opmBuffer[src + 59]) + 32768F) >> 16;
            sndByteBlock[dst + 86] = (byte) t;
            sndByteBlock[dst + 87] = (byte) (t >> 8);
            t = (int) (3156.65066666667F * u + 39766.6986666667F * v + 22612.6506666667F * (u = (float) OPM.opmBuffer[src + 61]) + 32768F) >> 16;
            sndByteBlock[dst + 90] = (byte) t;
            sndByteBlock[dst + 91] = (byte) (t >> 8);
            t = (int) (68.2666666666667F * v + 27718.9973333333F * u + 36175.872F * (v = (float) OPM.opmBuffer[src + 63]) + 1572.864F * (u = (float) OPM.opmBuffer[src + 65]) + 32768F) >> 16;
            sndByteBlock[dst + 94] = (byte) t;
            sndByteBlock[dst + 95] = (byte) (t >> 8);
            t = (int) (14155.776F * v + 43709.7813333333F * u + 7670.44266666667F * (v = (float) OPM.opmBuffer[src + 67]) + 32768F) >> 16;
            sndByteBlock[dst + 98] = (byte) t;
            sndByteBlock[dst + 99] = (byte) (t >> 8);
            t = (int) (5049.00266666667F * u + 42125.9946666667F * v + 18361.0026666667F * (u = (float) OPM.opmBuffer[src + 69]) + 32768F) >> 16;
            sndByteBlock[dst + 102] = (byte) t;
            sndByteBlock[dst + 103] = (byte) (t >> 8);
            t = (int) (535.210666666667F * v + 31970.6453333333F * u + 32415.744F * (v = (float) OPM.opmBuffer[src + 71]) + 614.4F * (u = (float) OPM.opmBuffer[src + 73]) + 32768F) >> 16;
            sndByteBlock[dst + 106] = (byte) t;
            sndByteBlock[dst + 107] = (byte) (t >> 8);
            t = (int) (17915.904F * v + 42333.5253333333F * u + 5286.57066666667F * (v = (float) OPM.opmBuffer[src + 75]) + 32768F) >> 16;
            sndByteBlock[dst + 110] = (byte) t;
            sndByteBlock[dst + 111] = (byte) (t >> 8);
            t = (int) (7383.72266666667F * u + 43600.5546666667F * v + 14551.7226666667F * (u = (float) OPM.opmBuffer[src + 77]) + 32768F) >> 16;
            sndByteBlock[dst + 114] = (byte) t;
            sndByteBlock[dst + 115] = (byte) (t >> 8);
            t = (int) (1444.52266666667F * v + 35779.9253333333F * u + 28213.248F * (v = (float) OPM.opmBuffer[src + 79]) + 98.304F * (u = (float) OPM.opmBuffer[src + 81]) + 32768F) >> 16;
            sndByteBlock[dst + 118] = (byte) t;
            sndByteBlock[dst + 119] = (byte) (t >> 8);
            t = (int) (22118.4F * v + 40072.5333333333F * u + 3345.06666666667F * (v = (float) OPM.opmBuffer[src + 83]) + 32768F) >> 16;
            sndByteBlock[dst + 122] = (byte) t;
            sndByteBlock[dst + 123] = (byte) (t >> 8);
            t = (int) (10160.8106666667F * u + 44190.3786666667F * v + 11184.8106666667F * (u = (float) OPM.opmBuffer[src + 85]) + 32768F) >> 16;
            sndByteBlock[dst + 126] = (byte) t;
            sndByteBlock[dst + 127] = (byte) (t >> 8);
            t = (int) (2796.20266666667F * v + 39122.2613333333F * u + 23617.536F * (v = (float) OPM.opmBuffer[src + 87]) + 32768F) >> 16;
            sndByteBlock[dst + 130] = (byte) t;
            sndByteBlock[dst + 131] = (byte) (t >> 8);
            t = (int) (24.576F * u + 26714.112F * v + 36951.3813333333F * (u = (float) OPM.opmBuffer[src + 89]) + 1845.93066666667F * (v = (float) OPM.opmBuffer[src + 91]) + 32768F) >> 16;
            sndByteBlock[dst + 134] = (byte) t;
            sndByteBlock[dst + 135] = (byte) (t >> 8);
            t = (int) (13380.2666666667F * u + 43895.4666666667F * v + 8260.26666666667F * (u = (float) OPM.opmBuffer[src + 93]) + 32768F) >> 16;
            sndByteBlock[dst + 138] = (byte) t;
            sndByteBlock[dst + 139] = (byte) (t >> 8);
            t = (int) (4590.25066666667F * v + 41678.1653333333F * u + 19267.584F * (v = (float) OPM.opmBuffer[src + 95]) + 32768F) >> 16;
            sndByteBlock[dst + 142] = (byte) t;
            sndByteBlock[dst + 143] = (byte) (t >> 8);
            t = (int) (393.216F * u + 31064.064F * v + 33289.5573333333F * (u = (float) OPM.opmBuffer[src + 97]) + 789.162666666667F * (v = (float) OPM.opmBuffer[src + 99]) + 32768F) >> 16;
            sndByteBlock[dst + 146] = (byte) t;
            sndByteBlock[dst + 147] = (byte) (t >> 8);
            t = (int) (17042.0906666667F * u + 42715.8186666667F * v + 5778.09066666667F * (u = (float) OPM.opmBuffer[src + 101]) + 32768F) >> 16;
            sndByteBlock[dst + 150] = (byte) t;
            sndByteBlock[dst + 151] = (byte) (t >> 8);
            t = (int) (6826.66666666667F * v + 43349.3333333333F * u + 15360F * (v = (float) OPM.opmBuffer[src + 103]) + 32768F) >> 16;
            sndByteBlock[dst + 154] = (byte) t;
            sndByteBlock[dst + 155] = (byte) (t >> 8);
            t = (int) (1204.224F * u + 34971.648F * v + 29185.3653333333F * (u = (float) OPM.opmBuffer[src + 105]) + 174.762666666667F * (v = (float) OPM.opmBuffer[src + 107]) + 32768F) >> 16;
            sndByteBlock[dst + 158] = (byte) t;
            sndByteBlock[dst + 159] = (byte) (t >> 8);
            t = (int) (21146.2826666667F * u + 40651.4346666667F * v + 3738.28266666667F * (u = (float) OPM.opmBuffer[src + 109]) + 32768F) >> 16;
            sndByteBlock[dst + 162] = (byte) t;
            sndByteBlock[dst + 163] = (byte) (t >> 8);
            t = (int) (9505.45066666667F * v + 44135.7653333333F * u + 11894.784F * (v = (float) OPM.opmBuffer[src + 111]) + 32768F) >> 16;
            sndByteBlock[dst + 166] = (byte) t;
            sndByteBlock[dst + 167] = (byte) (t >> 8);
            t = (int) (2457.6F * u + 38434.1333333333F * v + 24644.2666666667F * (u = (float) OPM.opmBuffer[src + 113]) + 32768F) >> 16;
            sndByteBlock[dst + 170] = (byte) t;
            sndByteBlock[dst + 171] = (byte) (t >> 8);
            t = (int) (2.73066666666667F * v + 25687.3813333333F * u + 37705.0453333333F * (v = (float) OPM.opmBuffer[src + 115]) + 2140.84266666667F * (u = (float) OPM.opmBuffer[src + 117]) + 32768F) >> 16;
            sndByteBlock[dst + 174] = (byte) t;
            sndByteBlock[dst + 175] = (byte) (t >> 8);
            t = (int) (12626.6026666667F * v + 44037.4613333333F * u + 8871.936F * (v = (float) OPM.opmBuffer[src + 119]) + 32768F) >> 16;
            sndByteBlock[dst + 178] = (byte) t;
            sndByteBlock[dst + 179] = (byte) (t >> 8);
            t = (int) (4153.344F * u + 41186.6453333333F * v + 20196.0106666667F * (u = (float) OPM.opmBuffer[src + 121]) + 32768F) >> 16;
            sndByteBlock[dst + 182] = (byte) t;
            sndByteBlock[dst + 183] = (byte) (t >> 8);
            t = (int) (273.066666666667F * v + 30135.6373333333F * u + 34141.5253333333F * (v = (float) OPM.opmBuffer[src + 123]) + 985.770666666667F * (u = (float) OPM.opmBuffer[src + 125]) + 32768F) >> 16;
            sndByteBlock[dst + 186] = (byte) t;
            sndByteBlock[dst + 187] = (byte) (t >> 8);
            t = (int) (16190.1226666667F * v + 43054.4213333333F * u + 6291.456F * (v = (float) OPM.opmBuffer[src + 127]) + 32768F) >> 16;
            sndByteBlock[dst + 190] = (byte) t;
            sndByteBlock[dst + 191] = (byte) (t >> 8);
            t = (int) (6291.456F * u + 43054.4213333333F * v + 16190.1226666667F * (u = (float) OPM.opmBuffer[src + 129]) + 32768F) >> 16;
            sndByteBlock[dst + 194] = (byte) t;
            sndByteBlock[dst + 195] = (byte) (t >> 8);
            t = (int) (985.770666666667F * v + 34141.5253333333F * u + 30135.6373333333F * (v = (float) OPM.opmBuffer[src + 131]) + 273.066666666667F * (u = (float) OPM.opmBuffer[src + 133]) + 32768F) >> 16;
            sndByteBlock[dst + 198] = (byte) t;
            sndByteBlock[dst + 199] = (byte) (t >> 8);
            t = (int) (20196.0106666667F * v + 41186.6453333333F * u + 4153.344F * (v = (float) OPM.opmBuffer[src + 135]) + 32768F) >> 16;
            sndByteBlock[dst + 202] = (byte) t;
            sndByteBlock[dst + 203] = (byte) (t >> 8);
            t = (int) (8871.936F * u + 44037.4613333333F * v + 12626.6026666667F * (u = (float) OPM.opmBuffer[src + 137]) + 32768F) >> 16;
            sndByteBlock[dst + 206] = (byte) t;
            sndByteBlock[dst + 207] = (byte) (t >> 8);
            t = (int) (2140.84266666667F * v + 37705.0453333333F * u + 25687.3813333333F * (v = (float) OPM.opmBuffer[src + 139]) + 2.73066666666667F * (u = (float) OPM.opmBuffer[src + 141]) + 32768F) >> 16;
            sndByteBlock[dst + 210] = (byte) t;
            sndByteBlock[dst + 211] = (byte) (t >> 8);
            t = (int) (24644.2666666667F * v + 38434.1333333333F * u + 2457.6F * (v = (float) OPM.opmBuffer[src + 143]) + 32768F) >> 16;
            sndByteBlock[dst + 214] = (byte) t;
            sndByteBlock[dst + 215] = (byte) (t >> 8);
            t = (int) (11894.784F * u + 44135.7653333333F * v + 9505.45066666667F * (u = (float) OPM.opmBuffer[src + 145]) + 32768F) >> 16;
            sndByteBlock[dst + 218] = (byte) t;
            sndByteBlock[dst + 219] = (byte) (t >> 8);
            t = (int) (3738.28266666667F * v + 40651.4346666667F * u + 21146.2826666667F * (v = (float) OPM.opmBuffer[src + 147]) + 32768F) >> 16;
            sndByteBlock[dst + 222] = (byte) t;
            sndByteBlock[dst + 223] = (byte) (t >> 8);
            t = (int) (174.762666666667F * u + 29185.3653333333F * v + 34971.648F * (u = (float) OPM.opmBuffer[src + 149]) + 1204.224F * (v = (float) OPM.opmBuffer[src + 151]) + 32768F) >> 16;
            sndByteBlock[dst + 226] = (byte) t;
            sndByteBlock[dst + 227] = (byte) (t >> 8);
            t = (int) (15360F * u + 43349.3333333333F * v + 6826.66666666667F * (u = (float) OPM.opmBuffer[src + 153]) + 32768F) >> 16;
            sndByteBlock[dst + 230] = (byte) t;
            sndByteBlock[dst + 231] = (byte) (t >> 8);
            t = (int) (5778.09066666667F * v + 42715.8186666667F * u + 17042.0906666667F * (v = (float) OPM.opmBuffer[src + 155]) + 32768F) >> 16;
            sndByteBlock[dst + 234] = (byte) t;
            sndByteBlock[dst + 235] = (byte) (t >> 8);
            t = (int) (789.162666666667F * u + 33289.5573333333F * v + 31064.064F * (u = (float) OPM.opmBuffer[src + 157]) + 393.216F * (v = (float) OPM.opmBuffer[src + 159]) + 32768F) >> 16;
            sndByteBlock[dst + 238] = (byte) t;
            sndByteBlock[dst + 239] = (byte) (t >> 8);
            t = (int) (19267.584F * u + 41678.1653333333F * v + 4590.25066666667F * (u = (float) OPM.opmBuffer[src + 161]) + 32768F) >> 16;
            sndByteBlock[dst + 242] = (byte) t;
            sndByteBlock[dst + 243] = (byte) (t >> 8);
            t = (int) (8260.26666666667F * v + 43895.4666666667F * u + 13380.2666666667F * (v = (float) OPM.opmBuffer[src + 163]) + 32768F) >> 16;
            sndByteBlock[dst + 246] = (byte) t;
            sndByteBlock[dst + 247] = (byte) (t >> 8);
            t = (int) (1845.93066666667F * u + 36951.3813333333F * v + 26714.112F * (u = (float) OPM.opmBuffer[src + 165]) + 24.576F * (v = (float) OPM.opmBuffer[src + 167]) + 32768F) >> 16;
            sndByteBlock[dst + 250] = (byte) t;
            sndByteBlock[dst + 251] = (byte) (t >> 8);
            t = (int) (23617.536F * u + 39122.2613333333F * v + 2796.20266666667F * (u = (float) OPM.opmBuffer[src + 169]) + 32768F) >> 16;
            sndByteBlock[dst + 254] = (byte) t;
            sndByteBlock[dst + 255] = (byte) (t >> 8);
            t = (int) (11184.8106666667F * v + 44190.3786666667F * u + 10160.8106666667F * (v = (float) OPM.opmBuffer[src + 171]) + 32768F) >> 16;
            sndByteBlock[dst + 258] = (byte) t;
            sndByteBlock[dst + 259] = (byte) (t >> 8);
            t = (int) (3345.06666666667F * u + 40072.5333333333F * v + 22118.4F * (u = (float) OPM.opmBuffer[src + 173]) + 32768F) >> 16;
            sndByteBlock[dst + 262] = (byte) t;
            sndByteBlock[dst + 263] = (byte) (t >> 8);
            t = (int) (98.304F * v + 28213.248F * u + 35779.9253333333F * (v = (float) OPM.opmBuffer[src + 175]) + 1444.52266666667F * (u = (float) OPM.opmBuffer[src + 177]) + 32768F) >> 16;
            sndByteBlock[dst + 266] = (byte) t;
            sndByteBlock[dst + 267] = (byte) (t >> 8);
            t = (int) (14551.7226666667F * v + 43600.5546666667F * u + 7383.72266666667F * (v = (float) OPM.opmBuffer[src + 179]) + 32768F) >> 16;
            sndByteBlock[dst + 270] = (byte) t;
            sndByteBlock[dst + 271] = (byte) (t >> 8);
            t = (int) (5286.57066666667F * u + 42333.5253333333F * v + 17915.904F * (u = (float) OPM.opmBuffer[src + 181]) + 32768F) >> 16;
            sndByteBlock[dst + 274] = (byte) t;
            sndByteBlock[dst + 275] = (byte) (t >> 8);
            t = (int) (614.4F * v + 32415.744F * u + 31970.6453333333F * (v = (float) OPM.opmBuffer[src + 183]) + 535.210666666667F * (u = (float) OPM.opmBuffer[src + 185]) + 32768F) >> 16;
            sndByteBlock[dst + 278] = (byte) t;
            sndByteBlock[dst + 279] = (byte) (t >> 8);
            t = (int) (18361.0026666667F * v + 42125.9946666667F * u + 5049.00266666667F * (v = (float) OPM.opmBuffer[src + 187]) + 32768F) >> 16;
            sndByteBlock[dst + 282] = (byte) t;
            sndByteBlock[dst + 283] = (byte) (t >> 8);
            t = (int) (7670.44266666667F * u + 43709.7813333333F * v + 14155.776F * (u = (float) OPM.opmBuffer[src + 189]) + 32768F) >> 16;
            sndByteBlock[dst + 286] = (byte) t;
            sndByteBlock[dst + 287] = (byte) (t >> 8);
            t = (int) (1572.864F * v + 36175.872F * u + 27718.9973333333F * (v = (float) OPM.opmBuffer[src + 191]) + 68.2666666666667F * (u = (float) OPM.opmBuffer[src + 193]) + 32768F) >> 16;
            sndByteBlock[dst + 290] = (byte) t;
            sndByteBlock[dst + 291] = (byte) (t >> 8);
            t = (int) (22612.6506666667F * v + 39766.6986666667F * u + 3156.65066666667F * (v = (float) OPM.opmBuffer[src + 195]) + 32768F) >> 16;
            sndByteBlock[dst + 294] = (byte) t;
            sndByteBlock[dst + 295] = (byte) (t >> 8);
            t = (int) (10496.6826666667F * u + 44201.3013333333F * v + 10838.016F * (u = (float) OPM.opmBuffer[src + 197]) + 32768F) >> 16;
            sndByteBlock[dst + 298] = (byte) t;
            sndByteBlock[dst + 299] = (byte) (t >> 8);
            t = (int) (2973.696F * v + 39449.9413333333F * u + 23112.3626666667F * (v = (float) OPM.opmBuffer[src + 199]) + 32768F) >> 16;
            sndByteBlock[dst + 302] = (byte) t;
            sndByteBlock[dst + 303] = (byte) (t >> 8);
            t = (int) (43.6906666666667F * u + 27219.2853333333F * v + 36566.3573333333F * (u = (float) OPM.opmBuffer[src + 201]) + 1706.66666666667F * (v = (float) OPM.opmBuffer[src + 203]) + 32768F) >> 16;
            sndByteBlock[dst + 306] = (byte) t;
            sndByteBlock[dst + 307] = (byte) (t >> 8);
            t = (int) (13765.2906666667F * u + 43808.0853333333F * v + 7962.624F * (u = (float) OPM.opmBuffer[src + 205]) + 32768F) >> 16;
            sndByteBlock[dst + 310] = (byte) t;
            sndByteBlock[dst + 311] = (byte) (t >> 8);
            t = (int) (4816.896F * v + 41907.5413333333F * u + 18811.5626666667F * (v = (float) OPM.opmBuffer[src + 207]) + 32768F) >> 16;
            sndByteBlock[dst + 314] = (byte) t;
            sndByteBlock[dst + 315] = (byte) (t >> 8);
            t = (int) (461.482666666667F * u + 31520.0853333333F * v + 32855.3813333333F * (u = (float) OPM.opmBuffer[src + 209]) + 699.050666666667F * (v = (float) OPM.opmBuffer[src + 211]) + 32768F) >> 16;
            sndByteBlock[dst + 318] = (byte) t;
            sndByteBlock[dst + 319] = (byte) (t >> 8);
            t = (int) (17476.2666666667F * u + 42530.1333333333F * v + 5529.6F * (u = (float) OPM.opmBuffer[src + 213]) + 32768F) >> 16;
            sndByteBlock[dst + 322] = (byte) t;
            sndByteBlock[dst + 323] = (byte) (t >> 8);
            t = (int) (7102.464F * v + 43480.4053333333F * u + 14953.1306666667F * (v = (float) OPM.opmBuffer[src + 215]) + 32768F) >> 16;
            sndByteBlock[dst + 326] = (byte) t;
            sndByteBlock[dst + 327] = (byte) (t >> 8);
            t = (int) (1321.64266666667F * u + 35378.5173333333F * v + 28702.0373333333F * (u = (float) OPM.opmBuffer[src + 217]) + 133.802666666667F * (v = (float) OPM.opmBuffer[src + 219]) + 32768F) >> 16;
            sndByteBlock[dst + 330] = (byte) t;
            sndByteBlock[dst + 331] = (byte) (t >> 8);
            t = (int) (21629.6106666667F * u + 40367.4453333333F * v + 3538.944F * (u = (float) OPM.opmBuffer[src + 221]) + 32768F) >> 16;
            sndByteBlock[dst + 334] = (byte) t;
            sndByteBlock[dst + 335] = (byte) (t >> 8);
            t = (int) (9830.4F * v + 44168.5333333333F * u + 11537.0666666667F * (v = (float) OPM.opmBuffer[src + 223]) + 32768F) >> 16;
            sndByteBlock[dst + 338] = (byte) t;
            sndByteBlock[dst + 339] = (byte) (t >> 8);
            t = (int) (2624.17066666667F * u + 38783.6586666667F * v + 24128.1706666667F * (u = (float) OPM.opmBuffer[src + 225]) + 32768F) >> 16;
            sndByteBlock[dst + 342] = (byte) t;
            sndByteBlock[dst + 343] = (byte) (t >> 8);
            t = (int) (10.9226666666667F * v + 26203.4773333333F * u + 37330.944F * (v = (float) OPM.opmBuffer[src + 227]) + 1990.656F * (u = (float) OPM.opmBuffer[src + 229]) + 32768F) >> 16;
            sndByteBlock[dst + 346] = (byte) t;
            sndByteBlock[dst + 347] = (byte) (t >> 8);
            t = (int) (13000.704F * v + 43971.9253333333F * u + 8563.37066666667F * (v = (float) OPM.opmBuffer[src + 231]) + 32768F) >> 16;
            sndByteBlock[dst + 350] = (byte) t;
            sndByteBlock[dst + 351] = (byte) (t >> 8);
            t = (int) (4369.06666666667F * u + 41437.8666666667F * v + 19729.0666666667F * (u = (float) OPM.opmBuffer[src + 233]) + 32768F) >> 16;
            sndByteBlock[dst + 354] = (byte) t;
            sndByteBlock[dst + 355] = (byte) (t >> 8);
            t = (int) (330.410666666667F * v + 30602.5813333333F * u + 33718.272F * (v = (float) OPM.opmBuffer[src + 235]) + 884.736F * (u = (float) OPM.opmBuffer[src + 237]) + 32768F) >> 16;
            sndByteBlock[dst + 358] = (byte) t;
            sndByteBlock[dst + 359] = (byte) (t >> 8);
            t = (int) (16613.376F * v + 42890.5813333333F * u + 6032.04266666667F * (v = (float) OPM.opmBuffer[src + 239]) + 32768F) >> 16;
            sndByteBlock[dst + 362] = (byte) t;
            sndByteBlock[dst + 363] = (byte) (t >> 8);
            t = (int) (6556.33066666667F * u + 43207.3386666667F * v + 15772.3306666667F * (u = (float) OPM.opmBuffer[src + 241]) + 32768F) >> 16;
            sndByteBlock[dst + 366] = (byte) t;
            sndByteBlock[dst + 367] = (byte) (t >> 8);
            t = (int) (1092.26666666667F * v + 34559.3173333333F * u + 29663.232F * (v = (float) OPM.opmBuffer[src + 243]) + 221.184F * (u = (float) OPM.opmBuffer[src + 245]) + 32768F) >> 16;
            sndByteBlock[dst + 370] = (byte) t;
            sndByteBlock[dst + 371] = (byte) (t >> 8);
            t = (int) (20668.416F * v + 40924.5013333333F * u + 3943.08266666667F * (v = (float) OPM.opmBuffer[src + 247]) + 32768F) >> 16;
            sndByteBlock[dst + 374] = (byte) t;
            sndByteBlock[dst + 375] = (byte) (t >> 8);
            t = (int) (9185.96266666667F * u + 44092.0746666667F * v + 12257.9626666667F * (u = (float) OPM.opmBuffer[src + 249]) + 32768F) >> 16;
            sndByteBlock[dst + 378] = (byte) t;
            sndByteBlock[dst + 379] = (byte) (t >> 8);
            t = (int) (2296.49066666667F * v + 38073.6853333333F * u + 25165.824F * (float) OPM.opmBuffer[src + 251] + 32768F) >> 16;
            sndByteBlock[dst + 382] = (byte) t;
            sndByteBlock[dst + 383] = (byte) (t >> 8);
          }  //for src,dst
        }  //if long/float
      }  //convert(int)
    };  //SNDRateConverter.LINEAR_AREA_STEREO_48000

    //rateConverter.initialize ()
    //  初めて選択されたときに配列の初期化などの必要な処理があれば行う
    public void initialize () {
    }

    //rateConverter.convert ()
    //  1ブロック分変換する。ADAPTIVE以外はoutputSamplesを使わない
    public abstract void convert (int outputSamples);

  }  //enum SNDRateConverter



}  //class SoundSource



