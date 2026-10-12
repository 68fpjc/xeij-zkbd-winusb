//========================================================================================
//  ADPCM.java
//    en:ADPCM sound source -- It decodes the compressed data written in MSM6258V and calculates interpolated values.
//    ja:ADPCM音源 -- MSM6258Vに書き込まれたデータを展開して補間値を計算します。
//  Copyright (C) 2003-2026 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,Timer,TimerTask,TreeMap

public class ADPCM {

  public static final boolean PCM_ON = true;  //true=PCMを出力する

  //  ADPCMのサンプリング周波数は8000000Hzまたは4000000Hzの原発振周波数を1/1024,1/768,1/512に分周して作られる
  //  同じデータを繰り返す回数を変更することで原発振周波数と分周比の組み合わせを再現する
  //  原発振周波数   分周比   サンプリング周波数
  //     8000000   /  1024  =     7812.5000
  //     8000000   /   768  =    10416.6667
  //     8000000   /   512  =    15625.0000
  //     4000000   /  1024  =     3906.2500
  //     4000000   /   768  =     5208.3333
  //     4000000   /   512  =     7812.5000
  //    16000000   /  1024  =    15625.0000
  //    16000000   /   768  =    20833.3333
  //    16000000   /   512  =    31250.0000
  public static final int PCM_SAMPLE_FREQ = 62500;  //基本サンプリング周波数(Hz)。62500Hzに固定
  public static final long PCM_SAMPLE_TIME = XEiJ.TMR_FREQ / PCM_SAMPLE_FREQ;  //1サンプルの時間(XEiJ.TMR_FREQ単位)
  public static final int PCM_BLOCK_SAMPLES = PCM_SAMPLE_FREQ / SoundSource.SND_BLOCK_FREQ;  //1ブロックのサンプル数。2500

  //原発振周波数、分周比、パン
  //  原発振周波数は0=8MHz/8MHz,1=4MHz/16MHz
  //  分周比は0=1024,1=768,2=512,3=inhibited
  public static final int PCM_MAX_REPEAT = PCM_SAMPLE_FREQ * 1024 / 4000000;  //データ繰り返す回数の最大。16
  public static final int[] PCM_SAMPLE_REPEAT = {  //データを繰り返す回数
    PCM_SAMPLE_FREQ * 1024 /  8000000,  // 8
    PCM_SAMPLE_FREQ *  768 /  8000000,  // 6
    PCM_SAMPLE_FREQ *  512 /  8000000,  // 4
    PCM_SAMPLE_FREQ *  768 /  8000000,  // 6
    PCM_SAMPLE_FREQ * 1024 /  4000000,  //16
    PCM_SAMPLE_FREQ *  768 /  4000000,  //12
    PCM_SAMPLE_FREQ *  512 /  4000000,  // 8
    PCM_SAMPLE_FREQ *  768 /  4000000,  //12
    PCM_SAMPLE_FREQ * 1024 /  8000000,  // 8
    PCM_SAMPLE_FREQ *  768 /  8000000,  // 6
    PCM_SAMPLE_FREQ *  512 /  8000000,  // 4
    PCM_SAMPLE_FREQ *  768 /  8000000,  // 6
    PCM_SAMPLE_FREQ * 1024 / 16000000,  // 4
    PCM_SAMPLE_FREQ *  768 / 16000000,  // 3
    PCM_SAMPLE_FREQ *  512 / 16000000,  // 2
    PCM_SAMPLE_FREQ *  768 / 16000000,  // 3
  };
  public static int pcmOSCFreqRequest;  //pcmOSCFreqModeに設定する値。非同期に変更できないので切りの良いところで転送する。リセットさせる必要はない
  public static int pcmOSCFreqMode;  //原発振周波数の改造。0=8MHz/4MHz,1=8MHz/16MHz
  public static int pcmOscillator;  //原発振周波数の選択。0=8MHz/8MHz,1=4MHz/16MHz
  public static int pcmDivider;  //分周比の選択。0=1/1024,1=1/768,2=1/512,3=inhibited
  public static int pcmRepeat;  //データを繰り返す回数
  public static long pcmInterval;  //1データ(2サンプル)あたりの割り込み間隔(XEiJ.TMR_FREQ単位)

  //ADPCM→PCM変換
  //  予測指標p(0..48)と4bitデータn(0..15)の組み合わせでPCMデータの差分が決まる
  //    x = floor (32768 / pow (1.1, 80 - p))
  //    delta = (1 - (n >> 2 & 2)) * ((n >> 2 & 1) * x + (n >> 1 & 1) * (x >> 1) + (n & 1) * (x >> 2) + (x >> 3))
  //    data = max (-2048, min (2047, data + delta))
  //  データで予測指標の差分が決まる
  //    p = max (0, min (48, p + ((n & 7) - 4 >> 3 | (n & 3) + 1 << 1)))
  //  8bitデータを下位4bit、上位4bitの順にデコードする
  //  8bitまとめてテーブルを参照することで2データ分の予測指標の遷移を1回で済ませる
  //  変換テーブル
  //    pcmDecoderTable[予測指標<<8|8bitデータ] = 下位4bitに対応する差分<<19|(上位4bitに対応する差分&8191)<<6|次の予測指標
  //    データの範囲が12bitのときmin-maxからmax-minまでの差分の範囲は符号を含めて13bit必要
  //      ±(1552+(1552>>1)+(1552>>2)+(1552>>3))=±2910
  //    テーブルを作る段階で12bitにクリッピングすると波形が変わってしまう
  //  ソフトウェアによってテーブルの内容に若干の差異がある
  //    PCM8.X,PCMLIB.a,MAMEなど
  //      zmusic2の組み込みと解除でそれぞれ+70ずつくらいずれる
  //      perl -e "for$p(0..48){$p%10==0 and print'      //    ';print int(16*1.1**$p).',';$p%10==9 and print chr(10);}"
  //      16,17,19,21,23,25,28,31,34,37,
  //      41,45,50,55,60,66,73,80,88,97,
  //      107,118,130,143,157,173,190,209,230,253,
  //      279,307,337,371,408,449,494,544,598,658,
  //      724,796,876,963,1060,1166,1282,1411,1552,
  //    PCM8A.X
  //      zmusic2の組み込みで+34、解除で+32ずれる
  //      log(32768/16)/log(1.1)=79.99795<80なのでPCM8.Xのテーブルと比較すると値がわずかに小さい
  //      perl -e "for$p(0..48){$p%10==0 and print'      //    ';print int(2**15/1.1**(80-$p)).',';$p%10==9 and print chr(10);}"
  //      15,17,19,21,23,25,28,31,34,37,
  //      41,45,50,55,60,66,73,80,88,97,
  //      107,118,130,143,157,173,190,209,230,253,
  //      279,307,337,371,408,449,494,543,598,658,
  //      724,796,876,963,1060,1166,1282,1410,1551,
  public static final int[] pcmDecoderTable = new int[256 * 49];  //ADPCM→PCM変換テーブル
  public static int pcmDecoderPointer;  //予測指標。0..48

  //バッファ
  public static final int[] pcmBuffer = new int[SoundSource.SND_CHANNELS * (PCM_BLOCK_SAMPLES + PCM_MAX_REPEAT * 2)];  //出力バッファ。ステレオのときはleft→rightの順序
  public static int pcmPointer;  //出力バッファの中を遷移するポインタ

  //タイマ
  public static long pcmClock;  //次のADPCMデータを取り込む時刻

  //動作
  public static boolean pcmOutputOn;  //true=PCMを出力する
  public static boolean pcmActive;  //pcmClock!=XEiJ.FAR_FUTURE。true=動作中,false=停止中
  public static int pcmEncodedData;  //次に出力する8ビットのADPCMデータ。-1=データなし
  public static int pcmDecodedData1;  //ADPCM→PCM変換されたPCMデータ<<4。1つ前
  public static int pcmDecodedData2;  //ADPCM→PCM変換されたPCMデータ<<4。2つ前
  public static int pcmDecodedData3;  //ADPCM→PCM変換されたPCMデータ<<4。3つ前

  //ゼロデータ補正
  //  予測指標が大きい状態から0x00や0x88を繰り返して予測指標を下げようとすると変位が+側または-側に振り切ってしまう
  //    perl -e "$s=0;for($p=48;$p>0;$p--){$s+=int(1.1**$p*16)>>3;}print$s*16"
  //    33454
  //    予測指標が48のとき0x00を48回繰り返して予測指標を0にすると変位が33454増える
  //  そのままパンで0に戻されるとプチノイズの原因になる
  //  データが97以上のとき0x00を0x08に、-97以下のとき0x88を0x80に変更することで変位が0から離れないようにする
  //    97は予測指標が48のときの0の増分の半分。予測指標を無視しても絶対値が大きくなってしまわない下限
  //    perl -e "print int(1.1**48*16)>>3>>1"
  //    97
  //! 軽量化。0以上かどうかで分けている
  //! 軽量化。delta1だけ補正している。delta2を加えるときの符号はテーブルを参照する時点で確定できないのでdelta2の補正は負荷が大きくなる
  public static final boolean PCM_MODIFY_ZERO_ZERO = true;  //true=ゼロデータ補正を行う。0x00/0x88を0x08/0x80に読み替える
  public static final boolean PCM_MODIFY_ZERO_TWICE = false;  //true=0x00/0x88が2回以上繰り返されたときだけ補正する
  public static final int[] pcmDecoderTableP = new int[256 * 49];  //0x00を0x08に読み替えたテーブル
  public static final int[] pcmDecoderTableM = new int[256 * 49];  //0x88を0x80に読み替えたテーブル
  public static int pcmZeroPreviousData;  //0x00=直前が0x00、0x88=直前が0x88、-1=その他

  //減衰
  //  データがないとき変位をゆっくり0に近付ける
  //  データによっては変位が0から離れた状態で再生が終わってしまうことがある
  //  そのとき再生終了と同時に変位を0にするとブチッという音が出てしまう
  //  また、変位が偏ってクリッピングが多発するのもノイズの原因になる
  //  データがないとき、変位をゆっくり0に近付けることでノイズを低減する
  //
  //  PCM8A.Xが組み込まれているときは無音でもADPCMはONのままなので、データが間に合わなかったのか再生が終わったのかすぐに判断できない
  //  ADPCMがOFFのときだけでなくADPCMがONでデータがないときも減衰させる必要がある
  //  ゆっくり0に近付ければ、データが間に合わなかったときは変位がわずかにずれるだけで済む
  public static final boolean PCM_DECAY_ON = true;  //true=減衰させる

  //補間
  //  ADPCMの出力周波数を62500Hzに上げるための補間アルゴリズムを選択する
  //  区分定数補間
  //    同じ値を繰り返す
  //    サンプリングデータをコピーするだけなので単純で高速だが、波形が階段状になるためノイズが増える
  //  線形補間
  //    前後2つのサンプリングデータを直線で結んで補間する
  //    前後2つのサンプリングデータの線形結合で求まる
  //  エルミート補間
  //    前後4つのサンプリングデータを用いて前後2つのサンプリングデータを曲線で結んで補間する
  //    前後4つのサンプリングデータの線形結合で求まる
  public static final int PCM_INTERPOLATION_CONSTANT = 0;  //Piecewise-constant interpolation 区分定数補間
  public static final int PCM_INTERPOLATION_LINEAR   = 1;  //Liear interpolation 線形補間
  public static final int PCM_INTERPOLATION_HERMITE  = 2;  //Hermite interpolation エルミート補間
  public static int pcmInterpolationAlgorithm;  //PCM_INTERPOLATION_CONSTANT,PCM_INTERPOLATION_LINEAR,PCM_INTERPOLATION_HERMITE
  //  モノラル
  public static final PIP[][] PCM_INTERPOLATION_MONO = {
    //                    分割数  原発振  分周比     出力
    //                            周波数            周波数
    //区分定数補間
    {
      PIP.PIP_MONO_CONSTANT_8,   // 8MHz  1/1024   7812.50Hz
      PIP.PIP_MONO_CONSTANT_6,   // 8MHz  1/768   10416.67Hz
      PIP.PIP_MONO_CONSTANT_4,   // 8MHz  1/512   15625.00Hz
      PIP.PIP_MONO_CONSTANT_6,   // 8MHz  1/768   10416.67Hz
      PIP.PIP_MONO_CONSTANT_16,  // 4MHz  1/1024   3906.25Hz
      PIP.PIP_MONO_CONSTANT_12,  // 4MHz  1/768    5208.33Hz
      PIP.PIP_MONO_CONSTANT_8,   // 4MHz  1/512    7812.50Hz
      PIP.PIP_MONO_CONSTANT_12,  // 4MHz  1/768    5208.33Hz
      PIP.PIP_MONO_CONSTANT_8,   // 8MHz  1/1024   7812.50Hz
      PIP.PIP_MONO_CONSTANT_6,   // 8MHz  1/768   10416.67Hz
      PIP.PIP_MONO_CONSTANT_4,   // 8MHz  1/512   15625.00Hz
      PIP.PIP_MONO_CONSTANT_6,   // 8MHz  1/768   10416.67Hz
      PIP.PIP_MONO_CONSTANT_4,   //16MHz  1/1024  15625.00Hz
      PIP.PIP_MONO_CONSTANT_3,   //16MHz  1/768   20833.33Hz
      PIP.PIP_MONO_CONSTANT_2,   //16MHz  1/512   31250.00Hz
      PIP.PIP_MONO_CONSTANT_3,   //16MHz  1/768   20833.33Hz
    },
    //線形補間
    {
      PIP.PIP_MONO_LINEAR_8,     // 8MHz  1/1024   7812.50Hz
      PIP.PIP_MONO_LINEAR_6,     // 8MHz  1/768   10416.67Hz
      PIP.PIP_MONO_LINEAR_4,     // 8MHz  1/512   15625.00Hz
      PIP.PIP_MONO_LINEAR_6,     // 8MHz  1/768   10416.67Hz
      PIP.PIP_MONO_LINEAR_16,    // 4MHz  1/1024   3906.25Hz
      PIP.PIP_MONO_LINEAR_12,    // 4MHz  1/768    5208.33Hz
      PIP.PIP_MONO_LINEAR_8,     // 4MHz  1/512    7812.50Hz
      PIP.PIP_MONO_LINEAR_12,    // 4MHz  1/768    5208.33Hz
      PIP.PIP_MONO_LINEAR_8,     // 8MHz  1/1024   7812.50Hz
      PIP.PIP_MONO_LINEAR_6,     // 8MHz  1/768   10416.67Hz
      PIP.PIP_MONO_LINEAR_4,     // 8MHz  1/512   15625.00Hz
      PIP.PIP_MONO_LINEAR_6,     // 8MHz  1/768   10416.67Hz
      PIP.PIP_MONO_LINEAR_4,     //16MHz  1/1024  15625.00Hz
      PIP.PIP_MONO_LINEAR_3,     //16MHz  1/768   20833.33Hz
      PIP.PIP_MONO_LINEAR_2,     //16MHz  1/512   31250.00Hz
      PIP.PIP_MONO_LINEAR_3,     //16MHz  1/768   20833.33Hz
    },
    //エルミート補間
    {
      PIP.PIP_MONO_HERMITE_8,    // 8MHz  1/1024   7812.50Hz
      PIP.PIP_MONO_HERMITE_6,    // 8MHz  1/768   10416.67Hz
      PIP.PIP_MONO_HERMITE_4,    // 8MHz  1/512   15625.00Hz
      PIP.PIP_MONO_HERMITE_6,    // 8MHz  1/768   10416.67Hz
      PIP.PIP_MONO_HERMITE_16,   // 4MHz  1/1024   3906.25Hz
      PIP.PIP_MONO_HERMITE_12,   // 4MHz  1/768    5208.33Hz
      PIP.PIP_MONO_HERMITE_8,    // 4MHz  1/512    7812.50Hz
      PIP.PIP_MONO_HERMITE_12,   // 4MHz  1/768    5208.33Hz
      PIP.PIP_MONO_HERMITE_8,    // 8MHz  1/1024   7812.50Hz
      PIP.PIP_MONO_HERMITE_6,    // 8MHz  1/768   10416.67Hz
      PIP.PIP_MONO_HERMITE_4,    // 8MHz  1/512   15625.00Hz
      PIP.PIP_MONO_HERMITE_6,    // 8MHz  1/768   10416.67Hz
      PIP.PIP_MONO_HERMITE_4,    //16MHz  1/1024  15625.00Hz
      PIP.PIP_MONO_HERMITE_3,    //16MHz  1/768   20833.33Hz
      PIP.PIP_MONO_HERMITE_2,    //16MHz  1/512   31250.00Hz
      PIP.PIP_MONO_HERMITE_3,    //16MHz  1/768   20833.33Hz
    },
  };  //PCM_INTERPOLATION_MONO
  //  ステレオ
  public static final PIP[][] PCM_INTERPOLATION_STEREO = {
    //                      分割数  原発振  分周比     出力
    //                              周波数            周波数
    //区分定数補間
    {
      PIP.PIP_STEREO_CONSTANT_8,   // 8MHz  1/1024   7812.50Hz
      PIP.PIP_STEREO_CONSTANT_6,   // 8MHz  1/768   10416.67Hz
      PIP.PIP_STEREO_CONSTANT_4,   // 8MHz  1/512   15625.00Hz
      PIP.PIP_STEREO_CONSTANT_6,   // 8MHz  1/768   10416.67Hz
      PIP.PIP_STEREO_CONSTANT_16,  // 4MHz  1/1024   3906.25Hz
      PIP.PIP_STEREO_CONSTANT_12,  // 4MHz  1/768    5208.33Hz
      PIP.PIP_STEREO_CONSTANT_8,   // 4MHz  1/512    7812.50Hz
      PIP.PIP_STEREO_CONSTANT_12,  // 4MHz  1/768    5208.33Hz
      PIP.PIP_STEREO_CONSTANT_8,   // 8MHz  1/1024   7812.50Hz
      PIP.PIP_STEREO_CONSTANT_6,   // 8MHz  1/768   10416.67Hz
      PIP.PIP_STEREO_CONSTANT_4,   // 8MHz  1/512   15625.00Hz
      PIP.PIP_STEREO_CONSTANT_6,   // 8MHz  1/768   10416.67Hz
      PIP.PIP_STEREO_CONSTANT_4,   //16MHz  1/1024  15625.00Hz
      PIP.PIP_STEREO_CONSTANT_3,   //16MHz  1/768   20833.33Hz
      PIP.PIP_STEREO_CONSTANT_2,   //16MHz  1/512   31250.00Hz
      PIP.PIP_STEREO_CONSTANT_3,   //16MHz  1/768   20833.33Hz
    },
    //線形補間
    {
      PIP.PIP_STEREO_LINEAR_8,     // 8MHz  1/1024   7812.50Hz
      PIP.PIP_STEREO_LINEAR_6,     // 8MHz  1/768   10416.67Hz
      PIP.PIP_STEREO_LINEAR_4,     // 8MHz  1/512   15625.00Hz
      PIP.PIP_STEREO_LINEAR_6,     // 8MHz  1/768   10416.67Hz
      PIP.PIP_STEREO_LINEAR_16,    // 4MHz  1/1024   3906.25Hz
      PIP.PIP_STEREO_LINEAR_12,    // 4MHz  1/768    5208.33Hz
      PIP.PIP_STEREO_LINEAR_8,     // 4MHz  1/512    7812.50Hz
      PIP.PIP_STEREO_LINEAR_12,    // 4MHz  1/768    5208.33Hz
      PIP.PIP_STEREO_LINEAR_8,     // 8MHz  1/1024   7812.50Hz
      PIP.PIP_STEREO_LINEAR_6,     // 8MHz  1/768   10416.67Hz
      PIP.PIP_STEREO_LINEAR_4,     // 8MHz  1/512   15625.00Hz
      PIP.PIP_STEREO_LINEAR_6,     // 8MHz  1/768   10416.67Hz
      PIP.PIP_STEREO_LINEAR_4,     //16MHz  1/1024  15625.00Hz
      PIP.PIP_STEREO_LINEAR_3,     //16MHz  1/768   20833.33Hz
      PIP.PIP_STEREO_LINEAR_2,     //16MHz  1/512   31250.00Hz
      PIP.PIP_STEREO_LINEAR_3,     //16MHz  1/768   20833.33Hz
    },
    //エルミート補間
    {
      PIP.PIP_STEREO_HERMITE_8,    // 8MHz  1/1024   7812.50Hz
      PIP.PIP_STEREO_HERMITE_6,    // 8MHz  1/768   10416.67Hz
      PIP.PIP_STEREO_HERMITE_4,    // 8MHz  1/512   15625.00Hz
      PIP.PIP_STEREO_HERMITE_6,    // 8MHz  1/768   10416.67Hz
      PIP.PIP_STEREO_HERMITE_16,   // 4MHz  1/1024   3906.25Hz
      PIP.PIP_STEREO_HERMITE_12,   // 4MHz  1/768    5208.33Hz
      PIP.PIP_STEREO_HERMITE_8,    // 4MHz  1/512    7812.50Hz
      PIP.PIP_STEREO_HERMITE_12,   // 4MHz  1/768    5208.33Hz
      PIP.PIP_STEREO_HERMITE_8,    // 8MHz  1/1024   7812.50Hz
      PIP.PIP_STEREO_HERMITE_6,    // 8MHz  1/768   10416.67Hz
      PIP.PIP_STEREO_HERMITE_4,    // 8MHz  1/512   15625.00Hz
      PIP.PIP_STEREO_HERMITE_6,    // 8MHz  1/768   10416.67Hz
      PIP.PIP_STEREO_HERMITE_4,    //16MHz  1/1024  15625.00Hz
      PIP.PIP_STEREO_HERMITE_3,    //16MHz  1/768   20833.33Hz
      PIP.PIP_STEREO_HERMITE_2,    //16MHz  1/512   31250.00Hz
      PIP.PIP_STEREO_HERMITE_3,    //16MHz  1/768   20833.33Hz
    },
  };  //PCM_INTERPOLATION_STEREO
  public static PIP pcmInterpolationEngine;  //補間エンジン

  //アタック
  //  パンのON/OFFでプチノイズが出ないように曲線で補間する
  public static final int PCM_ATTACK_RATE = 10;
  public static final int PCM_ATTACK_SPAN = 1 << PCM_ATTACK_RATE;  //曲線の長さ
  public static final int PCM_ATTACK_MASK = PCM_ATTACK_SPAN - 1;
  public static final int PCM_ATTACK_SHIFT = 14;
  public static final int[] pcmAttackCurve = new int[PCM_ATTACK_SPAN * 4];
  //  0=OFF
  //  1=ON
  //  0x80000000+(0..PCM_ATTACK_SPAN-1)=ON→OFF
  //  0x80000000+(PCM_ATTACK_SPAN*2..PCM_ATTACK_SPAN*3-1)=OFF→ON
  //  (pcmPanLeft==0||pcmPanLeft<0x80000000+PCM_ATTACK_SPAN*2)==OFF
  //  !(pcmPanLeft==0||pcmPanLeft<0x80000000+PCM_ATTACK_SPAN*2)==ON
  public static int pcmPanLeft;  //モノラルのときleftをmiddleとして使う
  public static int pcmPanRight;  //rightはステレオのときだけ使う

  public static int pcmLastPan;

  //pcmInit ()
  //  PCMを初期化する
  public static void pcmInit () {
    //動作
    //pcmOutputOn = true;
    pcmActive = false;
    pcmEncodedData = -1;
    pcmDecodedData1 = 0;
    pcmDecodedData2 = 0;
    pcmDecodedData3 = 0;
    //ADPCM→PCM変換
    //  4bitデータのADPCM→PCM変換テーブルを作る
    //    d4[予測指標<<4|4bitデータ] = 4bitデータに対応する差分
    //    p4[予測指標<<4|4bitデータ] = 次の予測指標
    int[] d4 = new int[16 * 49];
    int[] p4 = new int[16 * 49];
    int[] deltaP = new int[] { -1, -1, -1, -1, 2, 4, 6, 8 };  //PCM8,PCM8A,X68Soundなど
    for (int p = 0; p < 49; p++) {  //予測指標
      int x = (int) Math.floor (16.0 * Math.pow (1.1, p));  //PCM8.X,PCMLIB.a,MAMEなど
      //int x = (int) Math.floor (32768.0 / Math.pow (1.1, (double) (80 - p)));  //PCM8A.X
      for (int n = 0; n < 16; n++) {  //4bitデータ
        int i = p << 4 | n;
        if (true) {
          int t = ((((n & 7) << 1) | 1) * x) >> 3;
          d4[i] = (n & 8) == 0 ? t : -t;  //4bitデータに対応する差分
        } else {  //PCM8
          d4[i] = (1 - (n >> 2 & 2)) * ((n >> 2 & 1) * x + (n >> 1 & 1) * (x >> 1) + (n & 1) * (x >> 2) + (x >> 3));  //4bitデータに対応する差分
        }
        p4[i] = Math.max (0, Math.min (48, p + deltaP[n & 7]));  //次の予測指標
        if (false) {
          System.out.printf ("(%2d,%2d,%5d,%2d),", p, n, d4[i], p4[i]);
          if ((n & 7) == 7) {
            System.out.println ();
          }
        }
      }
    }
    //  8bitデータのADPCM→PCM変換テーブルを作る
    //pcmDecoderTable = new int[256 * 49];
    //pcmDecoderTableP = new int[256 * 49];
    //pcmDecoderTableM = new int[256 * 49];
    for (int p = 0; p < 49; p++) {  //予測指標
      for (int n2 = 0; n2 < 16; n2++) {  //8bitデータの上位4bit
        int i4 = p << 8 | n2 << 4;
        for (int n1 = 0; n1 < 16; n1++) {  //8bitデータの下位4bit
          int i = p << 4 | n1;
          int delta1 = d4[i];  //8bitデータの下位4ビットに対応する差分
          i = p4[i] << 4 | n2;
          int delta2 = d4[i];  //8bitデータの上位4ビットに対応する差分
          int q = p4[i];  //次の予測指標
          int t = delta1 << 19 | (delta2 & 8191) << 6 | q;
          pcmDecoderTable[i4 | n1] = t;
          if (PCM_MODIFY_ZERO_ZERO) {
            //pcmDecoderTablePは0x00の、pcmDecoderTableMは0x88の、delta1を符号反転する
            //  notで-1から引いてから1を加えれば0から引いたことになる
            pcmDecoderTableP[i4 | n1] = n1 == 0 && n2 == 0 ? (t ^ -1 << 19) + (1 << 19) : t;
            pcmDecoderTableM[i4 | n1] = n1 == 8 && n2 == 8 ? (t ^ -1 << 19) + (1 << 19) : t;
          }
        }
      }
    }
    if (PCM_MODIFY_ZERO_TWICE) {
      pcmZeroPreviousData = -1;
    }
    //補間
    //pcmInterpolationAlgorithm = PCM_INTERPOLATION_LINEAR;
    //アタック
    //pcmAttackCurve = new int[PCM_ATTACK_SPAN * 4];
    for (int i = 0; i < PCM_ATTACK_SPAN; i++) {
      pcmAttackCurve[i] =
        (int) Math.round (0.5 * (double) (1 << PCM_ATTACK_SHIFT) *
                          (1.0 + Math.cos (Math.PI / (double) PCM_ATTACK_SPAN * (double) i)));  //(1+cos(0→π))/2=1→0
      pcmAttackCurve[PCM_ATTACK_SPAN * 2 + i] =
        (int) Math.round (0.5 * (double) (1 << PCM_ATTACK_SHIFT) *
                          (1.0 + Math.cos (Math.PI / (double) PCM_ATTACK_SPAN * (double) i)));  //(1-cos(0→π))/2=0→1
    }
    Arrays.fill (pcmAttackCurve, PCM_ATTACK_SPAN, PCM_ATTACK_SPAN * 2, 0);
    Arrays.fill (pcmAttackCurve, PCM_ATTACK_SPAN * 3, PCM_ATTACK_SPAN * 4, 1 << PCM_ATTACK_SHIFT);
    pcmPanLeft = 0;
    if (SoundSource.SND_CHANNELS == 2) {  //ステレオ
      pcmPanRight = 0;
    }
    //バッファ
    //pcmBuffer = new int[SoundSource.SND_CHANNELS * (PCM_BLOCK_SAMPLES + PCM_MAX_REPEAT * 2)];
    //リセット
    pcmReset ();
  }  //pcmInit()

  //リセット
  public static void pcmReset () {
    //原発振周波数、分周比、パン
    //pcmOSCFreqRequest = 0;  //8MHz/4MHz
    pcmOSCFreqMode = 0;  //8MHz/4MHz
    pcmOscillator = 0;  //8MHz/8MHz
    pcmDivider = 2;  //1/512
    pcmUpdateRepeatInterval ();
    pcmLastPan = 0;
    pcmSetPan (pcmLastPan);  //LeftON,RightON
    //ADPCM→PCM変換
    pcmDecoderPointer = 0;
    //バッファ
    Arrays.fill (pcmBuffer, 0);
    pcmPointer = 0;
    //タイマ
    pcmClock = XEiJ.FAR_FUTURE;
    TickerQueue.tkqRemove (SoundSource.sndPcmTicker);
    //動作
    pcmActive = false;  //ADPCM出力停止
    pcmEncodedData = -1;  //ADPCMデータなし
  }  //pcmReset()

  //pcmUpdateRepeatInterval ()
  //  原発振周波数と分周比に従ってADPCMのサンプリング周波数を設定する
  public static void pcmUpdateRepeatInterval () {
    pcmRepeat = PCM_SAMPLE_REPEAT[pcmOSCFreqMode << 3 | pcmOscillator << 2 | pcmDivider];
    pcmInterval = PCM_SAMPLE_TIME * 2 * pcmRepeat;
    pcmSetInterpolationAlgorithm (pcmInterpolationAlgorithm);
  }  //pcmUpdateRepeatInterval()

  //pcmSetInterpolationAlgorithm (algorithm)
  //  補間アルゴリズムを指定する
  public static void pcmSetInterpolationAlgorithm (int algorithm) {
    pcmInterpolationAlgorithm = algorithm;
    pcmInterpolationEngine = (SoundSource.SND_CHANNELS == 1 ? PCM_INTERPOLATION_MONO : PCM_INTERPOLATION_STEREO)
      [algorithm][pcmOSCFreqMode << 3 | pcmOscillator << 2 | pcmDivider];
  }  //pcmSetInterpolationAlgorithm(int)

  //pcmSetOutputOn (on)
  //  PCM出力のON/OFF
  public static void pcmSetOutputOn (boolean on) {
    pcmOutputOn = on;
    pcmSetPan (pcmLastPan);
  }  //pcmSetOutputOn(boolean)

  //pcmSetPan (pan)
  //  発音するチャンネルの設定
  //  bit0  0=RightON,1=RightOFF
  //  bit1  0=LeftON,1=LeftOFF
  //    0/1,Left/Right共にOPMと逆になっていることに注意
  public static void pcmSetPan (int pan) {
    pcmLastPan = pan;
    boolean on0 = !(pcmPanLeft == 0 || pcmPanLeft < 0x80000000 + PCM_ATTACK_SPAN * 2);  //true=ONまたはOFF→ON,false=OFFまたはON→OFF
    boolean on1 = pcmOutputOn && (SoundSource.SND_CHANNELS == 1 ? (pan & 3) != 3 : (pan & 2) == 0);  //true=ON,false=OFF
    if (on0 != on1) {
      if (on1) {  //OFF→ONまたはON→OFF→ON
        if (pcmPanLeft < 0x80000000 + PCM_ATTACK_SPAN) {  //ON→OFF→ONでON→OFFが終了していない
          pcmPanLeft ^= PCM_ATTACK_SPAN * 2 | PCM_ATTACK_MASK;  //ON→OFFを反転してOFF→ONの途中から
        } else {  //ON→OFF→ONでON→OFFが終了しているか、OFF→ON
          pcmPanLeft = 0x80000000 + PCM_ATTACK_SPAN * 2;  //OFF→ONの最初から
        }
      } else {  //ON→OFF
        if (pcmPanLeft < 0x80000000 + PCM_ATTACK_SPAN * 3) {  //OFF→ON→OFFでOFF→ONが終了していない
          pcmPanLeft ^= PCM_ATTACK_SPAN * 2 | PCM_ATTACK_MASK;  //OFF→ONを反転してON→OFFの途中から
        } else {  //ON→OFF→ONでOFF→ONが終了しているか、ON→OFF
          pcmPanLeft = 0x80000000 + PCM_ATTACK_SPAN * 0;  //ON→OFFの最初から
        }
      }
    }
    if (SoundSource.SND_CHANNELS == 2) {  //ステレオ
      on0 = !(pcmPanRight == 0 || pcmPanRight < 0x80000000 + PCM_ATTACK_SPAN * 2);  //true=ONまたはOFF→ON,false=OFFまたはON→OFF
      on1 = pcmOutputOn && (pan & 1) == 0;  //true=ON,false=OFF
      if (on0 != on1) {
        if (on1) {  //OFF→ONまたはON→OFF→ON
          if (pcmPanRight < 0x80000000 + PCM_ATTACK_SPAN) {  //ON→OFF→ONでON→OFFが終了していない
            pcmPanRight ^= PCM_ATTACK_SPAN * 2 | PCM_ATTACK_MASK;  //ON→OFFを反転してOFF→ONの途中から
          } else {  //ON→OFF→ONでON→OFFが終了しているか、OFF→ON
            pcmPanRight = 0x80000000 + PCM_ATTACK_SPAN * 2;  //OFF→ONの最初から
          }
        } else {  //ON→OFF
          if (pcmPanRight < 0x80000000 + PCM_ATTACK_SPAN * 3) {  //OFF→ON→OFFでOFF→ONが終了していない
            pcmPanRight ^= PCM_ATTACK_SPAN * 2 | PCM_ATTACK_MASK;  //OFF→ONを反転してON→OFFの途中から
          } else {  //ON→OFF→ONでOFF→ONが終了しているか、ON→OFF
            pcmPanRight = 0x80000000 + PCM_ATTACK_SPAN * 0;  //ON→OFFの最初から
          }
        }
      }
    }
  }  //pcmSetPan(int)

  //pcmFillBuffer (endPointer)
  public static void pcmFillBuffer (int endPointer) {
    while (pcmPointer < endPointer && (pcmDecodedData1 | pcmDecodedData2 | pcmDecodedData3) != 0) {
      if (PCM_DECAY_ON) {  //減衰させる
        int m = pcmDecodedData1;
        pcmInterpolationEngine.write (m + (m >>> 31) - (-m >>> 31));
      } else {  //減衰させない
        pcmInterpolationEngine.write (0);
      }
    }
    if (pcmPointer < endPointer) {  //残りは無音
      Arrays.fill (pcmBuffer, pcmPointer, endPointer, 0);
      pcmPointer = endPointer;
    } else if (endPointer < pcmPointer) {  //進み過ぎたので戻る
      Arrays.fill (pcmBuffer, endPointer, pcmPointer, 0);
      pcmPointer = endPointer;
    }
  }  //pcmFillBuffer



  //PIP PCM補間エンジン
  public static enum PIP {

    //モノラル 区分定数補間 3906.25Hz
    PIP_MONO_CONSTANT_16 {
      @Override public void write (int m) {
        int i = pcmPointer;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  1] =
            pcmBuffer[i +  2] =
            pcmBuffer[i +  3] =
            pcmBuffer[i +  4] =
            pcmBuffer[i +  5] =
            pcmBuffer[i +  6] =
            pcmBuffer[i +  7] =
            pcmBuffer[i +  8] =
            pcmBuffer[i +  9] =
            pcmBuffer[i + 10] =
            pcmBuffer[i + 11] =
            pcmBuffer[i + 12] =
            pcmBuffer[i + 13] =
            pcmBuffer[i + 14] =
            pcmBuffer[i + 15] = m);
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i     ] = m * pcmAttackCurve[p     ] >> 14;
          pcmBuffer[i +  1] = m * pcmAttackCurve[p +  1] >> 14;
          pcmBuffer[i +  2] = m * pcmAttackCurve[p +  2] >> 14;
          pcmBuffer[i +  3] = m * pcmAttackCurve[p +  3] >> 14;
          pcmBuffer[i +  4] = m * pcmAttackCurve[p +  4] >> 14;
          pcmBuffer[i +  5] = m * pcmAttackCurve[p +  5] >> 14;
          pcmBuffer[i +  6] = m * pcmAttackCurve[p +  6] >> 14;
          pcmBuffer[i +  7] = m * pcmAttackCurve[p +  7] >> 14;
          pcmBuffer[i +  8] = m * pcmAttackCurve[p +  8] >> 14;
          pcmBuffer[i +  9] = m * pcmAttackCurve[p +  9] >> 14;
          pcmBuffer[i + 10] = m * pcmAttackCurve[p + 10] >> 14;
          pcmBuffer[i + 11] = m * pcmAttackCurve[p + 11] >> 14;
          pcmBuffer[i + 12] = m * pcmAttackCurve[p + 12] >> 14;
          pcmBuffer[i + 13] = m * pcmAttackCurve[p + 13] >> 14;
          pcmBuffer[i + 14] = m * pcmAttackCurve[p + 14] >> 14;
          pcmBuffer[i + 15] = m * pcmAttackCurve[p + 15] >> 14;
          p += 16;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  1] =
            pcmBuffer[i +  2] =
            pcmBuffer[i +  3] =
            pcmBuffer[i +  4] =
            pcmBuffer[i +  5] =
            pcmBuffer[i +  6] =
            pcmBuffer[i +  7] =
            pcmBuffer[i +  8] =
            pcmBuffer[i +  9] =
            pcmBuffer[i + 10] =
            pcmBuffer[i + 11] =
            pcmBuffer[i + 12] =
            pcmBuffer[i + 13] =
            pcmBuffer[i + 14] =
            pcmBuffer[i + 15] = 0);
        }
        pcmDecodedData1 = m;
        pcmPointer = i + 16;
      }
    },  //PIP_MONO_CONSTANT_16

    //モノラル 区分定数補間 5208.33Hz
    PIP_MONO_CONSTANT_12 {
      @Override public void write (int m) {
        int i = pcmPointer;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  1] =
            pcmBuffer[i +  2] =
            pcmBuffer[i +  3] =
            pcmBuffer[i +  4] =
            pcmBuffer[i +  5] =
            pcmBuffer[i +  6] =
            pcmBuffer[i +  7] =
            pcmBuffer[i +  8] =
            pcmBuffer[i +  9] =
            pcmBuffer[i + 10] =
            pcmBuffer[i + 11] = m);
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i     ] = m * pcmAttackCurve[p     ] >> 14;
          pcmBuffer[i +  1] = m * pcmAttackCurve[p +  1] >> 14;
          pcmBuffer[i +  2] = m * pcmAttackCurve[p +  2] >> 14;
          pcmBuffer[i +  3] = m * pcmAttackCurve[p +  3] >> 14;
          pcmBuffer[i +  4] = m * pcmAttackCurve[p +  4] >> 14;
          pcmBuffer[i +  5] = m * pcmAttackCurve[p +  5] >> 14;
          pcmBuffer[i +  6] = m * pcmAttackCurve[p +  6] >> 14;
          pcmBuffer[i +  7] = m * pcmAttackCurve[p +  7] >> 14;
          pcmBuffer[i +  8] = m * pcmAttackCurve[p +  8] >> 14;
          pcmBuffer[i +  9] = m * pcmAttackCurve[p +  9] >> 14;
          pcmBuffer[i + 10] = m * pcmAttackCurve[p + 10] >> 14;
          pcmBuffer[i + 11] = m * pcmAttackCurve[p + 11] >> 14;
          p += 12;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  1] =
            pcmBuffer[i +  2] =
            pcmBuffer[i +  3] =
            pcmBuffer[i +  4] =
            pcmBuffer[i +  5] =
            pcmBuffer[i +  6] =
            pcmBuffer[i +  7] =
            pcmBuffer[i +  8] =
            pcmBuffer[i +  9] =
            pcmBuffer[i + 10] =
            pcmBuffer[i + 11] = 0);
        }
        pcmDecodedData1 = m;
        pcmPointer = i + 12;
      }
    },  //PIP_MONO_CONSTANT_12

    //モノラル 区分定数補間 7812.50Hz
    PIP_MONO_CONSTANT_8 {
      @Override public void write (int m) {
        int i = pcmPointer;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  1] =
            pcmBuffer[i +  2] =
            pcmBuffer[i +  3] =
            pcmBuffer[i +  4] =
            pcmBuffer[i +  5] =
            pcmBuffer[i +  6] =
            pcmBuffer[i +  7] = m);
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i     ] = m * pcmAttackCurve[p     ] >> 14;
          pcmBuffer[i +  1] = m * pcmAttackCurve[p +  1] >> 14;
          pcmBuffer[i +  2] = m * pcmAttackCurve[p +  2] >> 14;
          pcmBuffer[i +  3] = m * pcmAttackCurve[p +  3] >> 14;
          pcmBuffer[i +  4] = m * pcmAttackCurve[p +  4] >> 14;
          pcmBuffer[i +  5] = m * pcmAttackCurve[p +  5] >> 14;
          pcmBuffer[i +  6] = m * pcmAttackCurve[p +  6] >> 14;
          pcmBuffer[i +  7] = m * pcmAttackCurve[p +  7] >> 14;
          p += 8;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  1] =
            pcmBuffer[i +  2] =
            pcmBuffer[i +  3] =
            pcmBuffer[i +  4] =
            pcmBuffer[i +  5] =
            pcmBuffer[i +  6] =
            pcmBuffer[i +  7] = 0);
        }
        pcmDecodedData1 = m;
        pcmPointer = i + 8;
      }
    },  //PIP_MONO_CONSTANT_8

    //モノラル 区分定数補間 10416.67Hz
    PIP_MONO_CONSTANT_6 {
      @Override public void write (int m) {
        int i = pcmPointer;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  1] =
            pcmBuffer[i +  2] =
            pcmBuffer[i +  3] =
            pcmBuffer[i +  4] =
            pcmBuffer[i +  5] = m);
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i     ] = m * pcmAttackCurve[p     ] >> 14;
          pcmBuffer[i +  1] = m * pcmAttackCurve[p +  1] >> 14;
          pcmBuffer[i +  2] = m * pcmAttackCurve[p +  2] >> 14;
          pcmBuffer[i +  3] = m * pcmAttackCurve[p +  3] >> 14;
          pcmBuffer[i +  4] = m * pcmAttackCurve[p +  4] >> 14;
          pcmBuffer[i +  5] = m * pcmAttackCurve[p +  5] >> 14;
          p += 6;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  1] =
            pcmBuffer[i +  2] =
            pcmBuffer[i +  3] =
            pcmBuffer[i +  4] =
            pcmBuffer[i +  5] = 0);
        }
        pcmDecodedData1 = m;
        pcmPointer = i + 6;
      }
    },  //PIP_MONO_CONSTANT_6

    //モノラル 区分定数補間 15625.00Hz
    PIP_MONO_CONSTANT_4 {
      @Override public void write (int m) {
        int i = pcmPointer;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  1] =
            pcmBuffer[i +  2] =
            pcmBuffer[i +  3] = m);
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i     ] = m * pcmAttackCurve[p     ] >> 14;
          pcmBuffer[i +  1] = m * pcmAttackCurve[p +  1] >> 14;
          pcmBuffer[i +  2] = m * pcmAttackCurve[p +  2] >> 14;
          pcmBuffer[i +  3] = m * pcmAttackCurve[p +  3] >> 14;
          p += 4;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  1] =
            pcmBuffer[i +  2] =
            pcmBuffer[i +  3] = 0);
        }
        pcmDecodedData1 = m;
        pcmPointer = i + 4;
      }
    },  //PIP_MONO_CONSTANT_4

    //モノラル 区分定数補間 20833.33Hz
    PIP_MONO_CONSTANT_3 {
      @Override public void write (int m) {
        int i = pcmPointer;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  1] =
            pcmBuffer[i +  2] = m);
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i     ] = m * pcmAttackCurve[p     ] >> 14;
          pcmBuffer[i +  1] = m * pcmAttackCurve[p +  1] >> 14;
          pcmBuffer[i +  2] = m * pcmAttackCurve[p +  2] >> 14;
          p += 3;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  1] =
            pcmBuffer[i +  2] = 0);
        }
        pcmDecodedData1 = m;
        pcmPointer = i + 3;
      }
    },  //PIP_MONO_CONSTANT_3

    //モノラル 区分定数補間 31250.00Hz
    PIP_MONO_CONSTANT_2 {
      @Override public void write (int m) {
        int i = pcmPointer;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  1] = m);
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i     ] = m * pcmAttackCurve[p     ] >> 14;
          pcmBuffer[i +  1] = m * pcmAttackCurve[p +  1] >> 14;
          p += 2;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  1] = 0);
        }
        pcmDecodedData1 = m;
        pcmPointer = i + 2;
      }
    },  //PIP_MONO_CONSTANT_2

    //モノラル 線形補間 3906.25Hz
    PIP_MONO_LINEAR_16 {
      @Override public void write (int m1) {
        int i = pcmPointer;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          int m0 = pcmDecodedData1;
          pcmBuffer[i     ] = 15 * m0 +      m1 >> 4;
          pcmBuffer[i +  1] =  7 * m0 +      m1 >> 3;
          pcmBuffer[i +  2] = 13 * m0 +  3 * m1 >> 4;
          pcmBuffer[i +  3] =  3 * m0 +      m1 >> 2;
          pcmBuffer[i +  4] = 11 * m0 +  5 * m1 >> 4;
          pcmBuffer[i +  5] =  5 * m0 +  3 * m1 >> 3;
          pcmBuffer[i +  6] =  9 * m0 +  7 * m1 >> 4;
          pcmBuffer[i +  7] =      m0 +      m1 >> 1;
          pcmBuffer[i +  8] =  7 * m0 +  9 * m1 >> 4;
          pcmBuffer[i +  9] =  3 * m0 +  5 * m1 >> 3;
          pcmBuffer[i + 10] =  5 * m0 + 11 * m1 >> 4;
          pcmBuffer[i + 11] =      m0 +  3 * m1 >> 2;
          pcmBuffer[i + 12] =  3 * m0 + 13 * m1 >> 4;
          pcmBuffer[i + 13] =      m0 +  7 * m1 >> 3;
          pcmBuffer[i + 14] =      m0 + 15 * m1 >> 4;
          pcmBuffer[i + 15] =                m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          int m0 = pcmDecodedData1;
          pcmBuffer[i     ] = (15 * m0 +      m1 >> 4) * pcmAttackCurve[p     ] >> 14;
          pcmBuffer[i +  1] = ( 7 * m0 +      m1 >> 3) * pcmAttackCurve[p +  1] >> 14;
          pcmBuffer[i +  2] = (13 * m0 +  3 * m1 >> 4) * pcmAttackCurve[p +  2] >> 14;
          pcmBuffer[i +  3] = ( 3 * m0 +      m1 >> 2) * pcmAttackCurve[p +  3] >> 14;
          pcmBuffer[i +  4] = (11 * m0 +  5 * m1 >> 4) * pcmAttackCurve[p +  4] >> 14;
          pcmBuffer[i +  5] = ( 5 * m0 +  3 * m1 >> 3) * pcmAttackCurve[p +  5] >> 14;
          pcmBuffer[i +  6] = ( 9 * m0 +  7 * m1 >> 4) * pcmAttackCurve[p +  6] >> 14;
          pcmBuffer[i +  7] = (     m0 +      m1 >> 1) * pcmAttackCurve[p +  7] >> 14;
          pcmBuffer[i +  8] = ( 7 * m0 +  9 * m1 >> 4) * pcmAttackCurve[p +  8] >> 14;
          pcmBuffer[i +  9] = ( 3 * m0 +  5 * m1 >> 3) * pcmAttackCurve[p +  9] >> 14;
          pcmBuffer[i + 10] = ( 5 * m0 + 11 * m1 >> 4) * pcmAttackCurve[p + 10] >> 14;
          pcmBuffer[i + 11] = (     m0 +  3 * m1 >> 2) * pcmAttackCurve[p + 11] >> 14;
          pcmBuffer[i + 12] = ( 3 * m0 + 13 * m1 >> 4) * pcmAttackCurve[p + 12] >> 14;
          pcmBuffer[i + 13] = (     m0 +  7 * m1 >> 3) * pcmAttackCurve[p + 13] >> 14;
          pcmBuffer[i + 14] = (     m0 + 15 * m1 >> 4) * pcmAttackCurve[p + 14] >> 14;
          pcmBuffer[i + 15] = (               m1     ) * pcmAttackCurve[p + 15] >> 14;
          p += 16;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  1] =
            pcmBuffer[i +  2] =
            pcmBuffer[i +  3] =
            pcmBuffer[i +  4] =
            pcmBuffer[i +  5] =
            pcmBuffer[i +  6] =
            pcmBuffer[i +  7] =
            pcmBuffer[i +  8] =
            pcmBuffer[i +  9] =
            pcmBuffer[i + 10] =
            pcmBuffer[i + 11] =
            pcmBuffer[i + 12] =
            pcmBuffer[i + 13] =
            pcmBuffer[i + 14] =
            pcmBuffer[i + 15] = 0);
        }
        pcmDecodedData1 = m1;
        pcmPointer = i + 16;
      }
    },  //PIP_MONO_LINEAR_16

    //モノラル 線形補間 5208.33Hz
    PIP_MONO_LINEAR_12 {
      @Override public void write (int m1) {
        int i = pcmPointer;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          int m0 = pcmDecodedData1;
          pcmBuffer[i     ] = 65536 * 11 / 12 * m0 + 65536 *  1 / 12 * m1 >> 16;
          pcmBuffer[i +  1] = 65536 * 10 / 12 * m0 + 65536 *  2 / 12 * m1 >> 16;
          pcmBuffer[i +  2] =          3      * m0 +                   m1 >>  2;
          pcmBuffer[i +  3] = 65536 *  8 / 12 * m0 + 65536 *  4 / 12 * m1 >> 16;
          pcmBuffer[i +  4] = 65536 *  7 / 12 * m0 + 65536 *  5 / 12 * m1 >> 16;
          pcmBuffer[i +  5] =                   m0 +                   m1 >>  1;
          pcmBuffer[i +  6] = 65536 *  5 / 12 * m0 + 65536 *  7 / 12 * m1 >> 16;
          pcmBuffer[i +  7] = 65536 *  4 / 12 * m0 + 65536 *  8 / 12 * m1 >> 16;
          pcmBuffer[i +  8] =                   m0 +          3      * m1 >>  2;
          pcmBuffer[i +  9] = 65536 *  2 / 12 * m0 + 65536 * 10 / 12 * m1 >> 16;
          pcmBuffer[i + 10] = 65536 *  1 / 12 * m0 + 65536 * 11 / 12 * m1 >> 16;
          pcmBuffer[i + 11] =                                          m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          int m0 = pcmDecodedData1;
          pcmBuffer[i     ] = (65536 * 11 / 12 * m0 + 65536 *  1 / 12 * m1 >> 16) * pcmAttackCurve[p     ] >> 14;
          pcmBuffer[i +  1] = (65536 * 10 / 12 * m0 + 65536 *  2 / 12 * m1 >> 16) * pcmAttackCurve[p +  1] >> 14;
          pcmBuffer[i +  2] = (         3      * m0 +                   m1 >>  2) * pcmAttackCurve[p +  2] >> 14;
          pcmBuffer[i +  3] = (65536 *  8 / 12 * m0 + 65536 *  4 / 12 * m1 >> 16) * pcmAttackCurve[p +  3] >> 14;
          pcmBuffer[i +  4] = (65536 *  7 / 12 * m0 + 65536 *  5 / 12 * m1 >> 16) * pcmAttackCurve[p +  4] >> 14;
          pcmBuffer[i +  5] = (                  m0 +                   m1 >>  1) * pcmAttackCurve[p +  5] >> 14;
          pcmBuffer[i +  6] = (65536 *  5 / 12 * m0 + 65536 *  7 / 12 * m1 >> 16) * pcmAttackCurve[p +  6] >> 14;
          pcmBuffer[i +  7] = (65536 *  4 / 12 * m0 + 65536 *  8 / 12 * m1 >> 16) * pcmAttackCurve[p +  7] >> 14;
          pcmBuffer[i +  8] = (                  m0 +          3      * m1 >>  2) * pcmAttackCurve[p +  8] >> 14;
          pcmBuffer[i +  9] = (65536 *  2 / 12 * m0 + 65536 * 10 / 12 * m1 >> 16) * pcmAttackCurve[p +  9] >> 14;
          pcmBuffer[i + 10] = (65536 *  1 / 12 * m0 + 65536 * 11 / 12 * m1 >> 16) * pcmAttackCurve[p + 10] >> 14;
          pcmBuffer[i + 11] = (                                         m1      ) * pcmAttackCurve[p + 11] >> 14;
          p += 12;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  1] =
            pcmBuffer[i +  2] =
            pcmBuffer[i +  3] =
            pcmBuffer[i +  4] =
            pcmBuffer[i +  5] =
            pcmBuffer[i +  6] =
            pcmBuffer[i +  7] =
            pcmBuffer[i +  8] =
            pcmBuffer[i +  9] =
            pcmBuffer[i + 10] =
            pcmBuffer[i + 11] = 0);
        }
        pcmDecodedData1 = m1;
        pcmPointer = i + 12;
      }
    },  //PIP_MONO_LINEAR_12

    //モノラル 線形補間 7812.50Hz
    PIP_MONO_LINEAR_8 {
      @Override public void write (int m1) {
        int i = pcmPointer;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          int m0 = pcmDecodedData1;
          pcmBuffer[i    ] = 7 * m0 +     m1 >> 3;
          pcmBuffer[i + 1] = 3 * m0 +     m1 >> 2;
          pcmBuffer[i + 2] = 5 * m0 + 3 * m1 >> 3;
          pcmBuffer[i + 3] =     m0 +     m1 >> 1;
          pcmBuffer[i + 4] = 3 * m0 + 5 * m1 >> 3;
          pcmBuffer[i + 5] =     m0 + 3 * m1 >> 2;
          pcmBuffer[i + 6] =     m0 + 7 * m1 >> 3;
          pcmBuffer[i + 7] =              m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          int m0 = pcmDecodedData1;
          pcmBuffer[i    ] = (7 * m0 +     m1 >> 3) * pcmAttackCurve[p    ] >> 14;
          pcmBuffer[i + 1] = (3 * m0 +     m1 >> 2) * pcmAttackCurve[p + 1] >> 14;
          pcmBuffer[i + 2] = (5 * m0 + 3 * m1 >> 3) * pcmAttackCurve[p + 2] >> 14;
          pcmBuffer[i + 3] = (    m0 +     m1 >> 1) * pcmAttackCurve[p + 3] >> 14;
          pcmBuffer[i + 4] = (3 * m0 + 5 * m1 >> 3) * pcmAttackCurve[p + 4] >> 14;
          pcmBuffer[i + 5] = (    m0 + 3 * m1 >> 2) * pcmAttackCurve[p + 5] >> 14;
          pcmBuffer[i + 6] = (    m0 + 7 * m1 >> 3) * pcmAttackCurve[p + 6] >> 14;
          pcmBuffer[i + 7] = (             m1     ) * pcmAttackCurve[p + 7] >> 14;
          p += 8;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i    ] = (
            pcmBuffer[i + 1] =
            pcmBuffer[i + 2] =
            pcmBuffer[i + 3] =
            pcmBuffer[i + 4] =
            pcmBuffer[i + 5] =
            pcmBuffer[i + 6] =
            pcmBuffer[i + 7] = 0);
        }
        pcmDecodedData1 = m1;
        pcmPointer = i + 8;
      }
    },  //PIP_MONO_LINEAR_8

    //モノラル 線形補間 10416.67Hz
    PIP_MONO_LINEAR_6 {
      @Override public void write (int m1) {
        int i = pcmPointer;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          int m0 = pcmDecodedData1;
          pcmBuffer[i    ] = 65536 * 5 / 6 * m0 + 65536 * 1 / 6 * m1 >> 16;
          pcmBuffer[i + 1] = 65536 * 4 / 6 * m0 + 65536 * 2 / 6 * m1 >> 16;
          pcmBuffer[i + 2] =                 m0 +                 m1 >>  1;
          pcmBuffer[i + 3] = 65536 * 2 / 6 * m0 + 65536 * 4 / 6 * m1 >> 16;
          pcmBuffer[i + 4] = 65536 * 1 / 6 * m0 + 65536 * 5 / 6 * m1 >> 16;
          pcmBuffer[i + 5] =                                      m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          int m0 = pcmDecodedData1;
          pcmBuffer[i    ] = (65536 * 5 / 6 * m0 + 65536 * 1 / 6 * m1 >> 16) * pcmAttackCurve[p    ] >> 14;
          pcmBuffer[i + 1] = (65536 * 4 / 6 * m0 + 65536 * 2 / 6 * m1 >> 16) * pcmAttackCurve[p + 1] >> 14;
          pcmBuffer[i + 2] = (                m0 +                 m1 >>  1) * pcmAttackCurve[p + 2] >> 14;
          pcmBuffer[i + 3] = (65536 * 2 / 6 * m0 + 65536 * 4 / 6 * m1 >> 16) * pcmAttackCurve[p + 3] >> 14;
          pcmBuffer[i + 4] = (65536 * 1 / 6 * m0 + 65536 * 5 / 6 * m1 >> 16) * pcmAttackCurve[p + 4] >> 14;
          pcmBuffer[i + 5] = (                                     m1      ) * pcmAttackCurve[p + 5] >> 14;
          p += 6;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i    ] = (
            pcmBuffer[i + 1] =
            pcmBuffer[i + 2] =
            pcmBuffer[i + 3] =
            pcmBuffer[i + 4] =
            pcmBuffer[i + 5] = 0);
        }
        pcmDecodedData1 = m1;
        pcmPointer = i + 6;
      }
    },  //PIP_MONO_LINEAR_6

    //モノラル 線形補間 15625.00Hz
    PIP_MONO_LINEAR_4 {
      @Override public void write (int m1) {
        int i = pcmPointer;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          int m0 = pcmDecodedData1;
          pcmBuffer[i    ] = 3 * m0 +     m1 >> 2;
          pcmBuffer[i + 1] =     m0 +     m1 >> 1;
          pcmBuffer[i + 2] =     m0 + 3 * m1 >> 2;
          pcmBuffer[i + 3] =              m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          int m0 = pcmDecodedData1;
          pcmBuffer[i    ] = (3 * m0 +     m1 >> 2) * pcmAttackCurve[p    ] >> 14;
          pcmBuffer[i + 1] = (    m0 +     m1 >> 1) * pcmAttackCurve[p + 1] >> 14;
          pcmBuffer[i + 2] = (    m0 + 3 * m1 >> 2) * pcmAttackCurve[p + 2] >> 14;
          pcmBuffer[i + 3] = (             m1     ) * pcmAttackCurve[p + 3] >> 14;
          p += 4;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i    ] = (
            pcmBuffer[i + 1] =
            pcmBuffer[i + 2] =
            pcmBuffer[i + 3] = 0);
        }
        pcmDecodedData1 = m1;
        pcmPointer = i + 4;
      }
    },  //PIP_MONO_LINEAR_4

    //モノラル 線形補間 20833.33Hz
    PIP_MONO_LINEAR_3 {
      @Override public void write (int m1) {
        int i = pcmPointer;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          int m0 = pcmDecodedData1;
          pcmBuffer[i    ] = 65536 * 2 / 3 * m0 + 65536 * 1 / 3 * m1 >> 16;
          pcmBuffer[i + 1] = 65536 * 1 / 3 * m0 + 65536 * 2 / 3 * m1 >> 16;
          pcmBuffer[i + 2] =                                      m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          int m0 = pcmDecodedData1;
          pcmBuffer[i    ] = (65536 * 2 / 3 * m0 + 65536 * 1 / 3 * m1 >> 16) * pcmAttackCurve[p    ] >> 14;
          pcmBuffer[i + 1] = (65536 * 1 / 3 * m0 + 65536 * 2 / 3 * m1 >> 16) * pcmAttackCurve[p + 1] >> 14;
          pcmBuffer[i + 2] = (                                     m1      ) * pcmAttackCurve[p + 2] >> 14;
          p += 3;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i    ] = (
            pcmBuffer[i + 1] =
            pcmBuffer[i + 2] = 0);
        }
        pcmDecodedData1 = m1;
        pcmPointer = i + 3;
      }
    },  //PIP_MONO_LINEAR_3

    //モノラル 線形補間 31250.00Hz
    PIP_MONO_LINEAR_2 {
      @Override public void write (int m1) {
        int i = pcmPointer;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          int m0 = pcmDecodedData1;
          pcmBuffer[i    ] = m0 + m1 >> 1;
          pcmBuffer[i + 1] =      m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          int m0 = pcmDecodedData1;
          pcmBuffer[i    ] = (m0 + m1 >> 1) * pcmAttackCurve[p    ] >> 14;
          pcmBuffer[i + 1] = (     m1     ) * pcmAttackCurve[p + 1] >> 14;
          p += 2;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i    ] = (
            pcmBuffer[i + 1] = 0);
        }
        pcmDecodedData1 = m1;
        pcmPointer = i + 2;
      }
    },  //PIP_MONO_LINEAR_2

    //モノラル エルミート補間 3906.25Hz
    PIP_MONO_HERMITE_16 {
      @Override public void write (int m2) {
        //  echo read("hermite.gp");hermite_code(1,16) | gp-2.7 -q
        int i = pcmPointer;
        int mm = pcmDecodedData3;
        int m0 = pcmDecodedData2;
        int m1 = pcmDecodedData1;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          pcmBuffer[i     ] =          -225 * mm + 8115 * m0 +  317 * m1 -   15 * m2 >> 13;
          pcmBuffer[i +  1] =           -49 * mm +  987 * m0 +   93 * m1 -    7 * m2 >> 10;
          pcmBuffer[i +  2] =          -507 * mm + 7553 * m0 + 1263 * m1 -  117 * m2 >> 13;
          pcmBuffer[i +  3] =            -9 * mm +  111 * m0 +   29 * m1 -    3 * m2 >>  7;
          pcmBuffer[i +  4] =          -605 * mm + 6567 * m0 + 2505 * m1 -  275 * m2 >> 13;
          pcmBuffer[i +  5] =           -75 * mm +  745 * m0 +  399 * m1 -   45 * m2 >> 10;
          pcmBuffer[i +  6] =          -567 * mm + 5301 * m0 + 3899 * m1 -  441 * m2 >> 13;
          pcmBuffer[i +  7] =                -mm +    9 *      (m0 + m1) -        m2 >>  4;
          pcmBuffer[i +  8] =          -441 * mm + 3899 * m0 + 5301 * m1 -  567 * m2 >> 13;
          pcmBuffer[i +  9] =           -45 * mm +  399 * m0 +  745 * m1 -   75 * m2 >> 10;
          pcmBuffer[i + 10] =          -275 * mm + 2505 * m0 + 6567 * m1 -  605 * m2 >> 13;
          pcmBuffer[i + 11] =            -3 * mm +   29 * m0 +  111 * m1 -    9 * m2 >>  7;
          pcmBuffer[i + 12] =          -117 * mm + 1263 * m0 + 7553 * m1 -  507 * m2 >> 13;
          pcmBuffer[i + 13] =            -7 * mm +   93 * m0 +  987 * m1 -   49 * m2 >> 10;
          pcmBuffer[i + 14] =           -15 * mm +  317 * m0 + 8115 * m1 -  225 * m2 >> 13;
          pcmBuffer[i + 15] =                                         m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i     ] = (         -225 * mm + 8115 * m0 +  317 * m1 -   15 * m2 >> 13) * pcmAttackCurve[p     ] >> 14;
          pcmBuffer[i +  1] = (          -49 * mm +  987 * m0 +   93 * m1 -    7 * m2 >> 10) * pcmAttackCurve[p +  1] >> 14;
          pcmBuffer[i +  2] = (         -507 * mm + 7553 * m0 + 1263 * m1 -  117 * m2 >> 13) * pcmAttackCurve[p +  2] >> 14;
          pcmBuffer[i +  3] = (           -9 * mm +  111 * m0 +   29 * m1 -    3 * m2 >>  7) * pcmAttackCurve[p +  3] >> 14;
          pcmBuffer[i +  4] = (         -605 * mm + 6567 * m0 + 2505 * m1 -  275 * m2 >> 13) * pcmAttackCurve[p +  4] >> 14;
          pcmBuffer[i +  5] = (          -75 * mm +  745 * m0 +  399 * m1 -   45 * m2 >> 10) * pcmAttackCurve[p +  5] >> 14;
          pcmBuffer[i +  6] = (         -567 * mm + 5301 * m0 + 3899 * m1 -  441 * m2 >> 13) * pcmAttackCurve[p +  6] >> 14;
          pcmBuffer[i +  7] = (               -mm +    9 *      (m0 + m1) -        m2 >>  4) * pcmAttackCurve[p +  7] >> 14;
          pcmBuffer[i +  8] = (         -441 * mm + 3899 * m0 + 5301 * m1 -  567 * m2 >> 13) * pcmAttackCurve[p +  8] >> 14;
          pcmBuffer[i +  9] = (          -45 * mm +  399 * m0 +  745 * m1 -   75 * m2 >> 10) * pcmAttackCurve[p +  9] >> 14;
          pcmBuffer[i + 10] = (         -275 * mm + 2505 * m0 + 6567 * m1 -  605 * m2 >> 13) * pcmAttackCurve[p + 10] >> 14;
          pcmBuffer[i + 11] = (           -3 * mm +   29 * m0 +  111 * m1 -    9 * m2 >>  7) * pcmAttackCurve[p + 11] >> 14;
          pcmBuffer[i + 12] = (         -117 * mm + 1263 * m0 + 7553 * m1 -  507 * m2 >> 13) * pcmAttackCurve[p + 12] >> 14;
          pcmBuffer[i + 13] = (           -7 * mm +   93 * m0 +  987 * m1 -   49 * m2 >> 10) * pcmAttackCurve[p + 13] >> 14;
          pcmBuffer[i + 14] = (          -15 * mm +  317 * m0 + 8115 * m1 -  225 * m2 >> 13) * pcmAttackCurve[p + 14] >> 14;
          pcmBuffer[i + 15] = (                                        m1                  ) * pcmAttackCurve[p + 15] >> 14;
          p += 16;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  1] =
            pcmBuffer[i +  2] =
            pcmBuffer[i +  3] =
            pcmBuffer[i +  4] =
            pcmBuffer[i +  5] =
            pcmBuffer[i +  6] =
            pcmBuffer[i +  7] =
            pcmBuffer[i +  8] =
            pcmBuffer[i +  9] =
            pcmBuffer[i + 10] =
            pcmBuffer[i + 11] =
            pcmBuffer[i + 12] =
            pcmBuffer[i + 13] =
            pcmBuffer[i + 14] =
            pcmBuffer[i + 15] = 0);
        }
        pcmDecodedData3 = m0;
        pcmDecodedData2 = m1;
        pcmDecodedData1 = m2;
        pcmPointer = i + 16;
      }
    },  //PIP_MONO_HERMITE_16

    //モノラル エルミート補間 5208.33Hz
    PIP_MONO_HERMITE_12 {
      @Override public void write (int m2) {
        //  echo read("hermite.gp");hermite_code(1,12) | gp-2.7 -q
        int i = pcmPointer;
        int mm = pcmDecodedData3;
        int m0 = pcmDecodedData2;
        int m1 = pcmDecodedData1;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          pcmBuffer[i     ] = (int) (( -121 * mm + 3399 * m0 +  189 * m1 -   11 * m2) *    1242757L >> 32);
          pcmBuffer[i +  1] = (int) ((  -25 * mm +  405 * m0 +   57 * m1 -    5 * m2) *    9942054L >> 32);
          pcmBuffer[i +  2] =            -9 * mm +  111 * m0 +   29 * m1 -    3 * m2 >>  7;
          pcmBuffer[i +  3] = (int) ((   -2 * mm +   21 * m0 +    9 * m1 -        m2) *  159072863L >> 32);
          pcmBuffer[i +  4] = (int) (( -245 * mm + 2331 * m0 + 1545 * m1 -  175 * m2) *    1242757L >> 32);
          pcmBuffer[i +  5] =                -mm +    9 *      (m0 + m1) -        m2 >>  4;
          pcmBuffer[i +  6] = (int) (( -175 * mm + 1545 * m0 + 2331 * m1 -  245 * m2) *    1242757L >> 32);
          pcmBuffer[i +  7] = (int) ((       -mm +    9 * m0 +   21 * m1 -    2 * m2) *  159072863L >> 32);
          pcmBuffer[i +  8] =            -3 * mm +   29 * m0 +  111 * m1 -    9 * m2 >>  7;
          pcmBuffer[i +  9] = (int) ((   -5 * mm +   57 * m0 +  405 * m1 -   25 * m2) *    9942054L >> 32);
          pcmBuffer[i + 10] = (int) ((  -11 * mm +  189 * m0 + 3399 * m1 -  121 * m2) *    1242757L >> 32);
          pcmBuffer[i + 11] =                                         m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i     ] = (int) (( -121 * mm + 3399 * m0 +  189 * m1 -   11 * m2) *    1242757L >> 32) * pcmAttackCurve[p     ] >> 14;
          pcmBuffer[i +  1] = (int) ((  -25 * mm +  405 * m0 +   57 * m1 -    5 * m2) *    9942054L >> 32) * pcmAttackCurve[p +  1] >> 14;
          pcmBuffer[i +  2] =       (    -9 * mm +  111 * m0 +   29 * m1 -    3 * m2 >>  7               ) * pcmAttackCurve[p +  2] >> 14;
          pcmBuffer[i +  3] = (int) ((   -2 * mm +   21 * m0 +    9 * m1 -        m2) *  159072863L >> 32) * pcmAttackCurve[p +  3] >> 14;
          pcmBuffer[i +  4] = (int) (( -245 * mm + 2331 * m0 + 1545 * m1 -  175 * m2) *    1242757L >> 32) * pcmAttackCurve[p +  4] >> 14;
          pcmBuffer[i +  5] =       (        -mm +    9 *      (m0 + m1) -        m2 >>  4               ) * pcmAttackCurve[p +  5] >> 14;
          pcmBuffer[i +  6] = (int) (( -175 * mm + 1545 * m0 + 2331 * m1 -  245 * m2) *    1242757L >> 32) * pcmAttackCurve[p +  6] >> 14;
          pcmBuffer[i +  7] = (int) ((       -mm +    9 * m0 +   21 * m1 -    2 * m2) *  159072863L >> 32) * pcmAttackCurve[p +  7] >> 14;
          pcmBuffer[i +  8] =       (    -3 * mm +   29 * m0 +  111 * m1 -    9 * m2 >>  7               ) * pcmAttackCurve[p +  8] >> 14;
          pcmBuffer[i +  9] = (int) ((   -5 * mm +   57 * m0 +  405 * m1 -   25 * m2) *    9942054L >> 32) * pcmAttackCurve[p +  9] >> 14;
          pcmBuffer[i + 10] = (int) ((  -11 * mm +  189 * m0 + 3399 * m1 -  121 * m2) *    1242757L >> 32) * pcmAttackCurve[p + 10] >> 14;
          pcmBuffer[i + 11] =       (                                 m1                                 ) * pcmAttackCurve[p + 11] >> 14;
          p += 12;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  1] =
            pcmBuffer[i +  2] =
            pcmBuffer[i +  3] =
            pcmBuffer[i +  4] =
            pcmBuffer[i +  5] =
            pcmBuffer[i +  6] =
            pcmBuffer[i +  7] =
            pcmBuffer[i +  8] =
            pcmBuffer[i +  9] =
            pcmBuffer[i + 10] =
            pcmBuffer[i + 11] = 0);
        }
        pcmDecodedData3 = m0;
        pcmDecodedData2 = m1;
        pcmDecodedData1 = m2;
        pcmPointer = i + 12;
      }
    },  //PIP_MONO_HERMITE_12

    //モノラル エルミート補間 7812.50Hz
    PIP_MONO_HERMITE_8 {
      @Override public void write (int m2) {
        //  echo read("hermite.gp");hermite_code(1,8) | gp-2.7 -q
        int i = pcmPointer;
        int mm = pcmDecodedData3;
        int m0 = pcmDecodedData2;
        int m1 = pcmDecodedData1;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          pcmBuffer[i    ] =           -49 * mm +  987 * m0 +   93 * m1 -    7 * m2 >> 10;
          pcmBuffer[i + 1] =            -9 * mm +  111 * m0 +   29 * m1 -    3 * m2 >>  7;
          pcmBuffer[i + 2] =           -75 * mm +  745 * m0 +  399 * m1 -   45 * m2 >> 10;
          pcmBuffer[i + 3] =                -mm +    9 *      (m0 + m1) -        m2 >>  4;
          pcmBuffer[i + 4] =           -45 * mm +  399 * m0 +  745 * m1 -   75 * m2 >> 10;
          pcmBuffer[i + 5] =            -3 * mm +   29 * m0 +  111 * m1 -    9 * m2 >>  7;
          pcmBuffer[i + 6] =            -7 * mm +   93 * m0 +  987 * m1 -   49 * m2 >> 10;
          pcmBuffer[i + 7] =                                         m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i    ] = (          -49 * mm +  987 * m0 +   93 * m1 -    7 * m2 >> 10) * pcmAttackCurve[p    ] >> 14;
          pcmBuffer[i + 1] = (           -9 * mm +  111 * m0 +   29 * m1 -    3 * m2 >>  7) * pcmAttackCurve[p + 1] >> 14;
          pcmBuffer[i + 2] = (          -75 * mm +  745 * m0 +  399 * m1 -   45 * m2 >> 10) * pcmAttackCurve[p + 2] >> 14;
          pcmBuffer[i + 3] = (               -mm +    9 *      (m0 + m1) -        m2 >>  4) * pcmAttackCurve[p + 3] >> 14;
          pcmBuffer[i + 4] = (          -45 * mm +  399 * m0 +  745 * m1 -   75 * m2 >> 10) * pcmAttackCurve[p + 4] >> 14;
          pcmBuffer[i + 5] = (           -3 * mm +   29 * m0 +  111 * m1 -    9 * m2 >>  7) * pcmAttackCurve[p + 5] >> 14;
          pcmBuffer[i + 6] = (           -7 * mm +   93 * m0 +  987 * m1 -   49 * m2 >> 10) * pcmAttackCurve[p + 6] >> 14;
          pcmBuffer[i + 7] = (                                        m1                  ) * pcmAttackCurve[p + 7] >> 14;
          p += 8;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  1] =
            pcmBuffer[i +  2] =
            pcmBuffer[i +  3] =
            pcmBuffer[i +  4] =
            pcmBuffer[i +  5] =
            pcmBuffer[i +  6] =
            pcmBuffer[i +  7] = 0);
        }
        pcmDecodedData3 = m0;
        pcmDecodedData2 = m1;
        pcmDecodedData1 = m2;
        pcmPointer = i + 8;
      }
    },  //PIP_MONO_HERMITE_8

    //モノラル エルミート補間 10416.67Hz
    PIP_MONO_HERMITE_6 {
      @Override public void write (int m2) {
        //  echo read("hermite.gp");hermite_code(1,6) | gp-2.7 -q
        int i = pcmPointer;
        int mm = pcmDecodedData3;
        int m0 = pcmDecodedData2;
        int m1 = pcmDecodedData1;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          pcmBuffer[i    ] = (int) ((  -25 * mm +  405 * m0 +   57 * m1 -    5 * m2) *    9942054L >> 32);
          pcmBuffer[i + 1] = (int) ((   -2 * mm +   21 * m0 +    9 * m1 -        m2) *  159072863L >> 32);
          pcmBuffer[i + 2] =                -mm +    9 *      (m0 + m1) -        m2 >>  4;
          pcmBuffer[i + 3] = (int) ((       -mm +    9 * m0 +   21 * m1 -    2 * m2) *  159072863L >> 32);
          pcmBuffer[i + 4] = (int) ((   -5 * mm +   57 * m0 +  405 * m1 -   25 * m2) *    9942054L >> 32);
          pcmBuffer[i + 5] =                                         m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i    ] = (int) ((  -25 * mm +  405 * m0 +   57 * m1 -    5 * m2) *    9942054L >> 32) * pcmAttackCurve[p    ] >> 14;
          pcmBuffer[i + 1] = (int) ((   -2 * mm +   21 * m0 +    9 * m1 -        m2) *  159072863L >> 32) * pcmAttackCurve[p + 1] >> 14;
          pcmBuffer[i + 2] =       (        -mm +    9 *      (m0 + m1) -        m2 >>  4               ) * pcmAttackCurve[p + 2] >> 14;
          pcmBuffer[i + 3] = (int) ((       -mm +    9 * m0 +   21 * m1 -    2 * m2) *  159072863L >> 32) * pcmAttackCurve[p + 3] >> 14;
          pcmBuffer[i + 4] = (int) ((   -5 * mm +   57 * m0 +  405 * m1 -   25 * m2) *    9942054L >> 32) * pcmAttackCurve[p + 4] >> 14;
          pcmBuffer[i + 5] =       (                                 m1                                 ) * pcmAttackCurve[p + 5] >> 14;
          p += 6;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  1] =
            pcmBuffer[i +  2] =
            pcmBuffer[i +  3] =
            pcmBuffer[i +  4] =
            pcmBuffer[i +  5] = 0);
        }
        pcmDecodedData3 = m0;
        pcmDecodedData2 = m1;
        pcmDecodedData1 = m2;
        pcmPointer = i + 6;
      }
    },  //PIP_MONO_HERMITE_6

    //モノラル エルミート補間 15625.00Hz
    PIP_MONO_HERMITE_4 {
      @Override public void write (int m2) {
        //  echo read("hermite.gp");hermite_code(1,4) | gp-2.7 -q
        int i = pcmPointer;
        int mm = pcmDecodedData3;
        int m0 = pcmDecodedData2;
        int m1 = pcmDecodedData1;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          pcmBuffer[i    ] =            -9 * mm +  111 * m0 +   29 * m1 -    3 * m2 >>  7;
          pcmBuffer[i + 1] =                -mm +    9 *      (m0 + m1) -        m2 >>  4;
          pcmBuffer[i + 2] =            -3 * mm +   29 * m0 +  111 * m1 -    9 * m2 >>  7;
          pcmBuffer[i + 3] =                                         m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i    ] = (           -9 * mm +  111 * m0 +   29 * m1 -    3 * m2 >>  7) * pcmAttackCurve[p    ] >> 14;
          pcmBuffer[i + 1] = (               -mm +    9 *      (m0 + m1) -        m2 >>  4) * pcmAttackCurve[p + 1] >> 14;
          pcmBuffer[i + 2] = (           -3 * mm +   29 * m0 +  111 * m1 -    9 * m2 >>  7) * pcmAttackCurve[p + 2] >> 14;
          pcmBuffer[i + 3] = (                                        m1                  ) * pcmAttackCurve[p + 3] >> 14;
          p += 4;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  1] =
            pcmBuffer[i +  2] =
            pcmBuffer[i +  3] = 0);
        }
        pcmDecodedData3 = m0;
        pcmDecodedData2 = m1;
        pcmDecodedData1 = m2;
        pcmPointer = i + 4;
      }
    },  //PIP_MONO_HERMITE_4

    //モノラル エルミート補間 20833.33Hz
    PIP_MONO_HERMITE_3 {
      @Override public void write (int m2) {
        //  echo read("hermite.gp");hermite_code(1,3) | gp-2.7 -q
        int i = pcmPointer;
        int mm = pcmDecodedData3;
        int m0 = pcmDecodedData2;
        int m1 = pcmDecodedData1;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          pcmBuffer[i    ] = (int) ((   -2 * mm +   21 * m0 +    9 * m1 -        m2) *  159072863L >> 32);
          pcmBuffer[i + 1] = (int) ((       -mm +    9 * m0 +   21 * m1 -    2 * m2) *  159072863L >> 32);
          pcmBuffer[i + 2] =                                         m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i    ] = (int) ((   -2 * mm +   21 * m0 +    9 * m1 -        m2) *  159072863L >> 32) * pcmAttackCurve[p    ] >> 14;
          pcmBuffer[i + 1] = (int) ((       -mm +    9 * m0 +   21 * m1 -    2 * m2) *  159072863L >> 32) * pcmAttackCurve[p + 1] >> 14;
          pcmBuffer[i + 2] =       (                                 m1                                 ) * pcmAttackCurve[p + 2] >> 14;
          p += 3;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  1] =
            pcmBuffer[i +  2] = 0);
        }
        pcmDecodedData3 = m0;
        pcmDecodedData2 = m1;
        pcmDecodedData1 = m2;
        pcmPointer = i + 3;
      }
    },  //PIP_MONO_HERMITE_3

    //モノラル エルミート補間 31250.00Hz
    PIP_MONO_HERMITE_2 {
      @Override public void write (int m2) {
        //  echo read("hermite.gp");hermite_code(1,2) | gp-2.7 -q
        int i = pcmPointer;
        int mm = pcmDecodedData3;
        int m0 = pcmDecodedData2;
        int m1 = pcmDecodedData1;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          pcmBuffer[i    ] =                -mm +    9 *      (m0 + m1) -        m2 >>  4;
          pcmBuffer[i + 1] =                                         m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i    ] = (               -mm +    9 *      (m0 + m1) -        m2 >>  4) * pcmAttackCurve[p    ] >> 14;
          pcmBuffer[i + 1] = (                                        m1                  ) * pcmAttackCurve[p + 1] >> 14;
          p += 2;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  1] = 0);
        }
        pcmDecodedData3 = m0;
        pcmDecodedData2 = m1;
        pcmDecodedData1 = m2;
        pcmPointer = i + 2;
      }
    },  //PIP_MONO_HERMITE_2

    //ステレオ 区分定数補間 3906.25Hz
    PIP_STEREO_CONSTANT_16 {
      @Override public void write (int m) {
        int i = pcmPointer;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  2] =
            pcmBuffer[i +  4] =
            pcmBuffer[i +  6] =
            pcmBuffer[i +  8] =
            pcmBuffer[i + 10] =
            pcmBuffer[i + 12] =
            pcmBuffer[i + 14] =
            pcmBuffer[i + 16] =
            pcmBuffer[i + 18] =
            pcmBuffer[i + 20] =
            pcmBuffer[i + 22] =
            pcmBuffer[i + 24] =
            pcmBuffer[i + 26] =
            pcmBuffer[i + 28] =
            pcmBuffer[i + 30] = m);
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i     ] = m * pcmAttackCurve[p     ] >> 14;
          pcmBuffer[i +  2] = m * pcmAttackCurve[p +  1] >> 14;
          pcmBuffer[i +  4] = m * pcmAttackCurve[p +  2] >> 14;
          pcmBuffer[i +  6] = m * pcmAttackCurve[p +  3] >> 14;
          pcmBuffer[i +  8] = m * pcmAttackCurve[p +  4] >> 14;
          pcmBuffer[i + 10] = m * pcmAttackCurve[p +  5] >> 14;
          pcmBuffer[i + 12] = m * pcmAttackCurve[p +  6] >> 14;
          pcmBuffer[i + 14] = m * pcmAttackCurve[p +  7] >> 14;
          pcmBuffer[i + 16] = m * pcmAttackCurve[p +  8] >> 14;
          pcmBuffer[i + 18] = m * pcmAttackCurve[p +  9] >> 14;
          pcmBuffer[i + 20] = m * pcmAttackCurve[p + 10] >> 14;
          pcmBuffer[i + 22] = m * pcmAttackCurve[p + 11] >> 14;
          pcmBuffer[i + 24] = m * pcmAttackCurve[p + 12] >> 14;
          pcmBuffer[i + 26] = m * pcmAttackCurve[p + 13] >> 14;
          pcmBuffer[i + 28] = m * pcmAttackCurve[p + 14] >> 14;
          pcmBuffer[i + 30] = m * pcmAttackCurve[p + 15] >> 14;
          p += 16;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  2] =
            pcmBuffer[i +  4] =
            pcmBuffer[i +  6] =
            pcmBuffer[i +  8] =
            pcmBuffer[i + 10] =
            pcmBuffer[i + 12] =
            pcmBuffer[i + 14] =
            pcmBuffer[i + 16] =
            pcmBuffer[i + 18] =
            pcmBuffer[i + 20] =
            pcmBuffer[i + 22] =
            pcmBuffer[i + 24] =
            pcmBuffer[i + 26] =
            pcmBuffer[i + 28] =
            pcmBuffer[i + 30] = 0);
        }
        p = pcmPanRight;
        if (p > 0) {  //ON
          pcmBuffer[  i +  1] = (
            pcmBuffer[i +  3] =
            pcmBuffer[i +  5] =
            pcmBuffer[i +  7] =
            pcmBuffer[i +  9] =
            pcmBuffer[i + 11] =
            pcmBuffer[i + 13] =
            pcmBuffer[i + 15] =
            pcmBuffer[i + 17] =
            pcmBuffer[i + 19] =
            pcmBuffer[i + 21] =
            pcmBuffer[i + 23] =
            pcmBuffer[i + 25] =
            pcmBuffer[i + 27] =
            pcmBuffer[i + 29] =
            pcmBuffer[i + 31] = m);
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i +  1] = m * pcmAttackCurve[p     ] >> 14;
          pcmBuffer[i +  3] = m * pcmAttackCurve[p +  1] >> 14;
          pcmBuffer[i +  5] = m * pcmAttackCurve[p +  2] >> 14;
          pcmBuffer[i +  7] = m * pcmAttackCurve[p +  3] >> 14;
          pcmBuffer[i +  9] = m * pcmAttackCurve[p +  4] >> 14;
          pcmBuffer[i + 11] = m * pcmAttackCurve[p +  5] >> 14;
          pcmBuffer[i + 13] = m * pcmAttackCurve[p +  6] >> 14;
          pcmBuffer[i + 15] = m * pcmAttackCurve[p +  7] >> 14;
          pcmBuffer[i + 17] = m * pcmAttackCurve[p +  8] >> 14;
          pcmBuffer[i + 19] = m * pcmAttackCurve[p +  9] >> 14;
          pcmBuffer[i + 21] = m * pcmAttackCurve[p + 10] >> 14;
          pcmBuffer[i + 23] = m * pcmAttackCurve[p + 11] >> 14;
          pcmBuffer[i + 25] = m * pcmAttackCurve[p + 12] >> 14;
          pcmBuffer[i + 27] = m * pcmAttackCurve[p + 13] >> 14;
          pcmBuffer[i + 29] = m * pcmAttackCurve[p + 14] >> 14;
          pcmBuffer[i + 31] = m * pcmAttackCurve[p + 15] >> 14;
          p += 16;
          pcmPanRight = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i +  1] = (
            pcmBuffer[i +  3] =
            pcmBuffer[i +  5] =
            pcmBuffer[i +  7] =
            pcmBuffer[i +  9] =
            pcmBuffer[i + 11] =
            pcmBuffer[i + 13] =
            pcmBuffer[i + 15] =
            pcmBuffer[i + 17] =
            pcmBuffer[i + 19] =
            pcmBuffer[i + 21] =
            pcmBuffer[i + 23] =
            pcmBuffer[i + 25] =
            pcmBuffer[i + 27] =
            pcmBuffer[i + 29] =
            pcmBuffer[i + 31] = 0);
        }
        pcmDecodedData1 = m;
        pcmPointer = i + 32;
      }
    },  //PIP_STEREO_CONSTANT_16

    //ステレオ 区分定数補間 5208.33Hz
    PIP_STEREO_CONSTANT_12 {
      @Override public void write (int m) {
        int i = pcmPointer;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  2] =
            pcmBuffer[i +  4] =
            pcmBuffer[i +  6] =
            pcmBuffer[i +  8] =
            pcmBuffer[i + 10] =
            pcmBuffer[i + 12] =
            pcmBuffer[i + 14] =
            pcmBuffer[i + 16] =
            pcmBuffer[i + 18] =
            pcmBuffer[i + 20] =
            pcmBuffer[i + 22] = m);
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i     ] = m * pcmAttackCurve[p     ] >> 14;
          pcmBuffer[i +  2] = m * pcmAttackCurve[p +  1] >> 14;
          pcmBuffer[i +  4] = m * pcmAttackCurve[p +  2] >> 14;
          pcmBuffer[i +  6] = m * pcmAttackCurve[p +  3] >> 14;
          pcmBuffer[i +  8] = m * pcmAttackCurve[p +  4] >> 14;
          pcmBuffer[i + 10] = m * pcmAttackCurve[p +  5] >> 14;
          pcmBuffer[i + 12] = m * pcmAttackCurve[p +  6] >> 14;
          pcmBuffer[i + 14] = m * pcmAttackCurve[p +  7] >> 14;
          pcmBuffer[i + 16] = m * pcmAttackCurve[p +  8] >> 14;
          pcmBuffer[i + 18] = m * pcmAttackCurve[p +  9] >> 14;
          pcmBuffer[i + 20] = m * pcmAttackCurve[p + 10] >> 14;
          pcmBuffer[i + 22] = m * pcmAttackCurve[p + 11] >> 14;
          p += 12;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  2] =
            pcmBuffer[i +  4] =
            pcmBuffer[i +  6] =
            pcmBuffer[i +  8] =
            pcmBuffer[i + 10] =
            pcmBuffer[i + 12] =
            pcmBuffer[i + 14] =
            pcmBuffer[i + 16] =
            pcmBuffer[i + 18] =
            pcmBuffer[i + 20] =
            pcmBuffer[i + 22] = 0);
        }
        p = pcmPanRight;
        if (p > 0) {  //ON
          pcmBuffer[  i +  1] = (
            pcmBuffer[i +  3] =
            pcmBuffer[i +  5] =
            pcmBuffer[i +  7] =
            pcmBuffer[i +  9] =
            pcmBuffer[i + 11] =
            pcmBuffer[i + 13] =
            pcmBuffer[i + 15] =
            pcmBuffer[i + 17] =
            pcmBuffer[i + 19] =
            pcmBuffer[i + 21] =
            pcmBuffer[i + 23] = m);
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i +  1] = m * pcmAttackCurve[p     ] >> 14;
          pcmBuffer[i +  3] = m * pcmAttackCurve[p +  1] >> 14;
          pcmBuffer[i +  5] = m * pcmAttackCurve[p +  2] >> 14;
          pcmBuffer[i +  7] = m * pcmAttackCurve[p +  3] >> 14;
          pcmBuffer[i +  9] = m * pcmAttackCurve[p +  4] >> 14;
          pcmBuffer[i + 11] = m * pcmAttackCurve[p +  5] >> 14;
          pcmBuffer[i + 13] = m * pcmAttackCurve[p +  6] >> 14;
          pcmBuffer[i + 15] = m * pcmAttackCurve[p +  7] >> 14;
          pcmBuffer[i + 17] = m * pcmAttackCurve[p +  8] >> 14;
          pcmBuffer[i + 19] = m * pcmAttackCurve[p +  9] >> 14;
          pcmBuffer[i + 21] = m * pcmAttackCurve[p + 10] >> 14;
          pcmBuffer[i + 23] = m * pcmAttackCurve[p + 11] >> 14;
          p += 12;
          pcmPanRight = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i +  1] = (
            pcmBuffer[i +  3] =
            pcmBuffer[i +  5] =
            pcmBuffer[i +  7] =
            pcmBuffer[i +  9] =
            pcmBuffer[i + 11] =
            pcmBuffer[i + 13] =
            pcmBuffer[i + 15] =
            pcmBuffer[i + 17] =
            pcmBuffer[i + 19] =
            pcmBuffer[i + 21] =
            pcmBuffer[i + 23] = 0);
        }
        pcmDecodedData1 = m;
        pcmPointer = i + 24;
      }
    },  //PIP_STEREO_CONSTANT_12

    //ステレオ 区分定数補間 7812.50Hz
    PIP_STEREO_CONSTANT_8 {
      @Override public void write (int m) {
        int i = pcmPointer;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  2] =
            pcmBuffer[i +  4] =
            pcmBuffer[i +  6] =
            pcmBuffer[i +  8] =
            pcmBuffer[i + 10] =
            pcmBuffer[i + 12] =
            pcmBuffer[i + 14] = m);
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i     ] = m * pcmAttackCurve[p     ] >> 14;
          pcmBuffer[i +  2] = m * pcmAttackCurve[p +  1] >> 14;
          pcmBuffer[i +  4] = m * pcmAttackCurve[p +  2] >> 14;
          pcmBuffer[i +  6] = m * pcmAttackCurve[p +  3] >> 14;
          pcmBuffer[i +  8] = m * pcmAttackCurve[p +  4] >> 14;
          pcmBuffer[i + 10] = m * pcmAttackCurve[p +  5] >> 14;
          pcmBuffer[i + 12] = m * pcmAttackCurve[p +  6] >> 14;
          pcmBuffer[i + 14] = m * pcmAttackCurve[p +  7] >> 14;
          p += 8;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  2] =
            pcmBuffer[i +  4] =
            pcmBuffer[i +  6] =
            pcmBuffer[i +  8] =
            pcmBuffer[i + 10] =
            pcmBuffer[i + 12] =
            pcmBuffer[i + 14] = 0);
        }
        p = pcmPanRight;
        if (p > 0) {  //ON
          pcmBuffer[  i +  1] = (
            pcmBuffer[i +  3] =
            pcmBuffer[i +  5] =
            pcmBuffer[i +  7] =
            pcmBuffer[i +  9] =
            pcmBuffer[i + 11] =
            pcmBuffer[i + 13] =
            pcmBuffer[i + 15] = m);
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i +  1] = m * pcmAttackCurve[p     ] >> 14;
          pcmBuffer[i +  3] = m * pcmAttackCurve[p +  1] >> 14;
          pcmBuffer[i +  5] = m * pcmAttackCurve[p +  2] >> 14;
          pcmBuffer[i +  7] = m * pcmAttackCurve[p +  3] >> 14;
          pcmBuffer[i +  9] = m * pcmAttackCurve[p +  4] >> 14;
          pcmBuffer[i + 11] = m * pcmAttackCurve[p +  5] >> 14;
          pcmBuffer[i + 13] = m * pcmAttackCurve[p +  6] >> 14;
          pcmBuffer[i + 15] = m * pcmAttackCurve[p +  7] >> 14;
          p += 8;
          pcmPanRight = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i +  1] = (
            pcmBuffer[i +  3] =
            pcmBuffer[i +  5] =
            pcmBuffer[i +  7] =
            pcmBuffer[i +  9] =
            pcmBuffer[i + 11] =
            pcmBuffer[i + 13] =
            pcmBuffer[i + 15] = 0);
        }
        pcmDecodedData1 = m;
        pcmPointer = i + 16;
      }
    },  //PIP_STEREO_CONSTANT_8

    //ステレオ 区分定数補間 10416.67Hz
    PIP_STEREO_CONSTANT_6 {
      @Override public void write (int m) {
        int i = pcmPointer;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  2] =
            pcmBuffer[i +  4] =
            pcmBuffer[i +  6] =
            pcmBuffer[i +  8] =
            pcmBuffer[i + 10] = m);
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i     ] = m * pcmAttackCurve[p     ] >> 14;
          pcmBuffer[i +  2] = m * pcmAttackCurve[p +  1] >> 14;
          pcmBuffer[i +  4] = m * pcmAttackCurve[p +  2] >> 14;
          pcmBuffer[i +  6] = m * pcmAttackCurve[p +  3] >> 14;
          pcmBuffer[i +  8] = m * pcmAttackCurve[p +  4] >> 14;
          pcmBuffer[i + 10] = m * pcmAttackCurve[p +  5] >> 14;
          p += 6;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  2] =
            pcmBuffer[i +  4] =
            pcmBuffer[i +  6] =
            pcmBuffer[i +  8] =
            pcmBuffer[i + 10] = 0);
        }
        p = pcmPanRight;
        if (p > 0) {  //ON
          pcmBuffer[  i +  1] = (
            pcmBuffer[i +  3] =
            pcmBuffer[i +  5] =
            pcmBuffer[i +  7] =
            pcmBuffer[i +  9] =
            pcmBuffer[i + 11] = m);
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i +  1] = m * pcmAttackCurve[p     ] >> 14;
          pcmBuffer[i +  3] = m * pcmAttackCurve[p +  1] >> 14;
          pcmBuffer[i +  5] = m * pcmAttackCurve[p +  2] >> 14;
          pcmBuffer[i +  7] = m * pcmAttackCurve[p +  3] >> 14;
          pcmBuffer[i +  9] = m * pcmAttackCurve[p +  4] >> 14;
          pcmBuffer[i + 11] = m * pcmAttackCurve[p +  5] >> 14;
          p += 6;
          pcmPanRight = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i +  1] = (
            pcmBuffer[i +  3] =
            pcmBuffer[i +  5] =
            pcmBuffer[i +  7] =
            pcmBuffer[i +  9] =
            pcmBuffer[i + 11] = 0);
        }
        pcmDecodedData1 = m;
        pcmPointer = i + 12;
      }
    },  //PIP_STEREO_CONSTANT_6

    //ステレオ 区分定数補間 15625.00Hz
    PIP_STEREO_CONSTANT_4 {
      @Override public void write (int m) {
        int i = pcmPointer;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  2] =
            pcmBuffer[i +  4] =
            pcmBuffer[i +  6] = m);
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i     ] = m * pcmAttackCurve[p     ] >> 14;
          pcmBuffer[i +  2] = m * pcmAttackCurve[p +  1] >> 14;
          pcmBuffer[i +  4] = m * pcmAttackCurve[p +  2] >> 14;
          pcmBuffer[i +  6] = m * pcmAttackCurve[p +  3] >> 14;
          p += 4;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  2] =
            pcmBuffer[i +  4] =
            pcmBuffer[i +  6] = 0);
        }
        p = pcmPanRight;
        if (p > 0) {  //ON
          pcmBuffer[  i +  1] = (
            pcmBuffer[i +  3] =
            pcmBuffer[i +  5] =
            pcmBuffer[i +  7] = m);
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i +  1] = m * pcmAttackCurve[p     ] >> 14;
          pcmBuffer[i +  3] = m * pcmAttackCurve[p +  1] >> 14;
          pcmBuffer[i +  5] = m * pcmAttackCurve[p +  2] >> 14;
          pcmBuffer[i +  7] = m * pcmAttackCurve[p +  3] >> 14;
          p += 4;
          pcmPanRight = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i +  1] = (
            pcmBuffer[i +  3] =
            pcmBuffer[i +  5] =
            pcmBuffer[i +  7] = 0);
        }
        pcmDecodedData1 = m;
        pcmPointer = i + 8;
      }
    },  //PIP_STEREO_CONSTANT_4

    //ステレオ 区分定数補間 20833.33Hz
    PIP_STEREO_CONSTANT_3 {
      @Override public void write (int m) {
        int i = pcmPointer;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  2] =
            pcmBuffer[i +  4] = m);
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i     ] = m * pcmAttackCurve[p     ] >> 14;
          pcmBuffer[i +  2] = m * pcmAttackCurve[p +  1] >> 14;
          pcmBuffer[i +  4] = m * pcmAttackCurve[p +  2] >> 14;
          p += 3;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  2] =
            pcmBuffer[i +  4] = 0);
        }
        p = pcmPanRight;
        if (p > 0) {  //ON
          pcmBuffer[  i +  1] = (
            pcmBuffer[i +  3] =
            pcmBuffer[i +  5] = m);
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i +  1] = m * pcmAttackCurve[p     ] >> 14;
          pcmBuffer[i +  3] = m * pcmAttackCurve[p +  1] >> 14;
          pcmBuffer[i +  5] = m * pcmAttackCurve[p +  2] >> 14;
          p += 3;
          pcmPanRight = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i +  1] = (
            pcmBuffer[i +  3] =
            pcmBuffer[i +  5] = 0);
        }
        pcmDecodedData1 = m;
        pcmPointer = i + 6;
      }
    },  //PIP_STEREO_CONSTANT_3

    //ステレオ 区分定数補間 31250.00Hz
    PIP_STEREO_CONSTANT_2 {
      @Override public void write (int m) {
        int i = pcmPointer;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  2] = m);
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i     ] = m * pcmAttackCurve[p     ] >> 14;
          pcmBuffer[i +  2] = m * pcmAttackCurve[p +  1] >> 14;
          p += 2;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  2] = 0);
        }
        p = pcmPanRight;
        if (p > 0) {  //ON
          pcmBuffer[  i +  1] = (
            pcmBuffer[i +  3] = m);
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i +  1] = m * pcmAttackCurve[p     ] >> 14;
          pcmBuffer[i +  3] = m * pcmAttackCurve[p +  1] >> 14;
          p += 2;
          pcmPanRight = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i +  1] = (
            pcmBuffer[i +  3] = 0);
        }
        pcmDecodedData1 = m;
        pcmPointer = i + 4;
      }
    },  //PIP_STEREO_CONSTANT_2

    //ステレオ 線形補間 3906.25Hz
    PIP_STEREO_LINEAR_16 {
      @Override public void write (int m1) {
        int i = pcmPointer;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          int m0 = pcmDecodedData1;
          pcmBuffer[i     ] = 15 * m0 +      m1 >> 4;
          pcmBuffer[i +  2] =  7 * m0 +      m1 >> 3;
          pcmBuffer[i +  4] = 13 * m0 +  3 * m1 >> 4;
          pcmBuffer[i +  6] =  3 * m0 +      m1 >> 2;
          pcmBuffer[i +  8] = 11 * m0 +  5 * m1 >> 4;
          pcmBuffer[i + 10] =  5 * m0 +  3 * m1 >> 3;
          pcmBuffer[i + 12] =  9 * m0 +  7 * m1 >> 4;
          pcmBuffer[i + 14] =      m0 +      m1 >> 1;
          pcmBuffer[i + 16] =  7 * m0 +  9 * m1 >> 4;
          pcmBuffer[i + 18] =  3 * m0 +  5 * m1 >> 3;
          pcmBuffer[i + 20] =  5 * m0 + 11 * m1 >> 4;
          pcmBuffer[i + 22] =      m0 +  3 * m1 >> 2;
          pcmBuffer[i + 24] =  3 * m0 + 13 * m1 >> 4;
          pcmBuffer[i + 26] =      m0 +  7 * m1 >> 3;
          pcmBuffer[i + 28] =      m0 + 15 * m1 >> 4;
          pcmBuffer[i + 30] =                m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          int m0 = pcmDecodedData1;
          pcmBuffer[i     ] = (15 * m0 +      m1 >> 4) * pcmAttackCurve[p     ] >> 14;
          pcmBuffer[i +  2] = ( 7 * m0 +      m1 >> 3) * pcmAttackCurve[p +  1] >> 14;
          pcmBuffer[i +  4] = (13 * m0 +  3 * m1 >> 4) * pcmAttackCurve[p +  2] >> 14;
          pcmBuffer[i +  6] = ( 3 * m0 +      m1 >> 2) * pcmAttackCurve[p +  3] >> 14;
          pcmBuffer[i +  8] = (11 * m0 +  5 * m1 >> 4) * pcmAttackCurve[p +  4] >> 14;
          pcmBuffer[i + 10] = ( 5 * m0 +  3 * m1 >> 3) * pcmAttackCurve[p +  5] >> 14;
          pcmBuffer[i + 12] = ( 9 * m0 +  7 * m1 >> 4) * pcmAttackCurve[p +  6] >> 14;
          pcmBuffer[i + 14] = (     m0 +      m1 >> 1) * pcmAttackCurve[p +  7] >> 14;
          pcmBuffer[i + 16] = ( 7 * m0 +  9 * m1 >> 4) * pcmAttackCurve[p +  8] >> 14;
          pcmBuffer[i + 18] = ( 3 * m0 +  5 * m1 >> 3) * pcmAttackCurve[p +  9] >> 14;
          pcmBuffer[i + 20] = ( 5 * m0 + 11 * m1 >> 4) * pcmAttackCurve[p + 10] >> 14;
          pcmBuffer[i + 22] = (     m0 +  3 * m1 >> 2) * pcmAttackCurve[p + 11] >> 14;
          pcmBuffer[i + 24] = ( 3 * m0 + 13 * m1 >> 4) * pcmAttackCurve[p + 12] >> 14;
          pcmBuffer[i + 26] = (     m0 +  7 * m1 >> 3) * pcmAttackCurve[p + 13] >> 14;
          pcmBuffer[i + 28] = (     m0 + 15 * m1 >> 4) * pcmAttackCurve[p + 14] >> 14;
          pcmBuffer[i + 30] = (               m1     ) * pcmAttackCurve[p + 15] >> 14;
          p += 16;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  2] =
            pcmBuffer[i +  4] =
            pcmBuffer[i +  6] =
            pcmBuffer[i +  8] =
            pcmBuffer[i + 10] =
            pcmBuffer[i + 12] =
            pcmBuffer[i + 14] =
            pcmBuffer[i + 16] =
            pcmBuffer[i + 18] =
            pcmBuffer[i + 20] =
            pcmBuffer[i + 22] =
            pcmBuffer[i + 24] =
            pcmBuffer[i + 26] =
            pcmBuffer[i + 28] =
            pcmBuffer[i + 30] = 0);
        }
        p = pcmPanRight;
        if (p > 0) {  //ON
          int m0 = pcmDecodedData1;
          pcmBuffer[i +  1] = 15 * m0 +      m1 >> 4;
          pcmBuffer[i +  3] =  7 * m0 +      m1 >> 3;
          pcmBuffer[i +  5] = 13 * m0 +  3 * m1 >> 4;
          pcmBuffer[i +  7] =  3 * m0 +      m1 >> 2;
          pcmBuffer[i +  9] = 11 * m0 +  5 * m1 >> 4;
          pcmBuffer[i + 11] =  5 * m0 +  3 * m1 >> 3;
          pcmBuffer[i + 13] =  9 * m0 +  7 * m1 >> 4;
          pcmBuffer[i + 15] =      m0 +      m1 >> 1;
          pcmBuffer[i + 17] =  7 * m0 +  9 * m1 >> 4;
          pcmBuffer[i + 19] =  3 * m0 +  5 * m1 >> 3;
          pcmBuffer[i + 21] =  5 * m0 + 11 * m1 >> 4;
          pcmBuffer[i + 23] =      m0 +  3 * m1 >> 2;
          pcmBuffer[i + 25] =  3 * m0 + 13 * m1 >> 4;
          pcmBuffer[i + 27] =      m0 +  7 * m1 >> 3;
          pcmBuffer[i + 29] =      m0 + 15 * m1 >> 4;
          pcmBuffer[i + 31] =                m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          int m0 = pcmDecodedData1;
          pcmBuffer[i +  1] = (15 * m0 +      m1 >> 4) * pcmAttackCurve[p     ] >> 14;
          pcmBuffer[i +  3] = ( 7 * m0 +      m1 >> 3) * pcmAttackCurve[p +  1] >> 14;
          pcmBuffer[i +  5] = (13 * m0 +  3 * m1 >> 4) * pcmAttackCurve[p +  2] >> 14;
          pcmBuffer[i +  7] = ( 3 * m0 +      m1 >> 2) * pcmAttackCurve[p +  3] >> 14;
          pcmBuffer[i +  9] = (11 * m0 +  5 * m1 >> 4) * pcmAttackCurve[p +  4] >> 14;
          pcmBuffer[i + 11] = ( 5 * m0 +  3 * m1 >> 3) * pcmAttackCurve[p +  5] >> 14;
          pcmBuffer[i + 13] = ( 9 * m0 +  7 * m1 >> 4) * pcmAttackCurve[p +  6] >> 14;
          pcmBuffer[i + 15] = (     m0 +      m1 >> 1) * pcmAttackCurve[p +  7] >> 14;
          pcmBuffer[i + 17] = ( 7 * m0 +  9 * m1 >> 4) * pcmAttackCurve[p +  8] >> 14;
          pcmBuffer[i + 19] = ( 3 * m0 +  5 * m1 >> 3) * pcmAttackCurve[p +  9] >> 14;
          pcmBuffer[i + 21] = ( 5 * m0 + 11 * m1 >> 4) * pcmAttackCurve[p + 10] >> 14;
          pcmBuffer[i + 23] = (     m0 +  3 * m1 >> 2) * pcmAttackCurve[p + 11] >> 14;
          pcmBuffer[i + 25] = ( 3 * m0 + 13 * m1 >> 4) * pcmAttackCurve[p + 12] >> 14;
          pcmBuffer[i + 27] = (     m0 +  7 * m1 >> 3) * pcmAttackCurve[p + 13] >> 14;
          pcmBuffer[i + 29] = (     m0 + 15 * m1 >> 4) * pcmAttackCurve[p + 14] >> 14;
          pcmBuffer[i + 31] = (               m1     ) * pcmAttackCurve[p + 15] >> 14;
          p += 16;
          pcmPanRight = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i +  1] = (
            pcmBuffer[i +  3] =
            pcmBuffer[i +  5] =
            pcmBuffer[i +  7] =
            pcmBuffer[i +  9] =
            pcmBuffer[i + 11] =
            pcmBuffer[i + 13] =
            pcmBuffer[i + 15] =
            pcmBuffer[i + 17] =
            pcmBuffer[i + 19] =
            pcmBuffer[i + 21] =
            pcmBuffer[i + 23] =
            pcmBuffer[i + 25] =
            pcmBuffer[i + 27] =
            pcmBuffer[i + 29] =
            pcmBuffer[i + 31] = 0);
        }
        pcmDecodedData1 = m1;
        pcmPointer = i + 32;
      }
    },  //PIP_STEREO_LINEAR_16

    //ステレオ 線形補間 5208.33Hz
    PIP_STEREO_LINEAR_12 {
      @Override public void write (int m1) {
        int i = pcmPointer;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          int m0 = pcmDecodedData1;
          pcmBuffer[i     ] = 65536 * 11 / 12 * m0 + 65536 *  1 / 12 * m1 >> 16;
          pcmBuffer[i +  2] = 65536 * 10 / 12 * m0 + 65536 *  2 / 12 * m1 >> 16;
          pcmBuffer[i +  4] =          3      * m0 +                   m1 >>  2;
          pcmBuffer[i +  6] = 65536 *  8 / 12 * m0 + 65536 *  4 / 12 * m1 >> 16;
          pcmBuffer[i +  8] = 65536 *  7 / 12 * m0 + 65536 *  5 / 12 * m1 >> 16;
          pcmBuffer[i + 10] =                   m0 +                   m1 >>  1;
          pcmBuffer[i + 12] = 65536 *  5 / 12 * m0 + 65536 *  7 / 12 * m1 >> 16;
          pcmBuffer[i + 14] = 65536 *  4 / 12 * m0 + 65536 *  8 / 12 * m1 >> 16;
          pcmBuffer[i + 16] =                   m0 +          3      * m1 >>  2;
          pcmBuffer[i + 18] = 65536 *  2 / 12 * m0 + 65536 * 10 / 12 * m1 >> 16;
          pcmBuffer[i + 20] = 65536 *  1 / 12 * m0 + 65536 * 11 / 12 * m1 >> 16;
          pcmBuffer[i + 22] =                                          m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          int m0 = pcmDecodedData1;
          pcmBuffer[i     ] = (65536 * 11 / 12 * m0 + 65536 *  1 / 12 * m1 >> 16) * pcmAttackCurve[p     ] >> 14;
          pcmBuffer[i +  2] = (65536 * 10 / 12 * m0 + 65536 *  2 / 12 * m1 >> 16) * pcmAttackCurve[p +  1] >> 14;
          pcmBuffer[i +  4] = (         3      * m0 +                   m1 >>  2) * pcmAttackCurve[p +  2] >> 14;
          pcmBuffer[i +  6] = (65536 *  8 / 12 * m0 + 65536 *  4 / 12 * m1 >> 16) * pcmAttackCurve[p +  3] >> 14;
          pcmBuffer[i +  8] = (65536 *  7 / 12 * m0 + 65536 *  5 / 12 * m1 >> 16) * pcmAttackCurve[p +  4] >> 14;
          pcmBuffer[i + 10] = (                  m0 +                   m1 >>  1) * pcmAttackCurve[p +  5] >> 14;
          pcmBuffer[i + 12] = (65536 *  5 / 12 * m0 + 65536 *  7 / 12 * m1 >> 16) * pcmAttackCurve[p +  6] >> 14;
          pcmBuffer[i + 14] = (65536 *  4 / 12 * m0 + 65536 *  8 / 12 * m1 >> 16) * pcmAttackCurve[p +  7] >> 14;
          pcmBuffer[i + 16] = (                  m0 +          3      * m1 >>  2) * pcmAttackCurve[p +  8] >> 14;
          pcmBuffer[i + 18] = (65536 *  2 / 12 * m0 + 65536 * 10 / 12 * m1 >> 16) * pcmAttackCurve[p +  9] >> 14;
          pcmBuffer[i + 20] = (65536 *  1 / 12 * m0 + 65536 * 11 / 12 * m1 >> 16) * pcmAttackCurve[p + 10] >> 14;
          pcmBuffer[i + 22] = (                                         m1      ) * pcmAttackCurve[p + 11] >> 14;
          p += 12;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  2] =
            pcmBuffer[i +  4] =
            pcmBuffer[i +  6] =
            pcmBuffer[i +  8] =
            pcmBuffer[i + 10] =
            pcmBuffer[i + 12] =
            pcmBuffer[i + 14] =
            pcmBuffer[i + 16] =
            pcmBuffer[i + 18] =
            pcmBuffer[i + 20] =
            pcmBuffer[i + 22] = 0);
        }
        p = pcmPanRight;
        if (p > 0) {  //ON
          int m0 = pcmDecodedData1;
          pcmBuffer[i +  1] = 65536 * 11 / 12 * m0 + 65536 *  1 / 12 * m1 >> 16;
          pcmBuffer[i +  3] = 65536 * 10 / 12 * m0 + 65536 *  2 / 12 * m1 >> 16;
          pcmBuffer[i +  5] =          3      * m0 +                   m1 >>  2;
          pcmBuffer[i +  7] = 65536 *  8 / 12 * m0 + 65536 *  4 / 12 * m1 >> 16;
          pcmBuffer[i +  9] = 65536 *  7 / 12 * m0 + 65536 *  5 / 12 * m1 >> 16;
          pcmBuffer[i + 11] =                   m0 +                   m1 >>  1;
          pcmBuffer[i + 13] = 65536 *  5 / 12 * m0 + 65536 *  7 / 12 * m1 >> 16;
          pcmBuffer[i + 15] = 65536 *  4 / 12 * m0 + 65536 *  8 / 12 * m1 >> 16;
          pcmBuffer[i + 17] =                   m0 +          3      * m1 >>  2;
          pcmBuffer[i + 19] = 65536 *  2 / 12 * m0 + 65536 * 10 / 12 * m1 >> 16;
          pcmBuffer[i + 21] = 65536 *  1 / 12 * m0 + 65536 * 11 / 12 * m1 >> 16;
          pcmBuffer[i + 23] =                                          m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          int m0 = pcmDecodedData1;
          pcmBuffer[i +  1] = (65536 * 11 / 12 * m0 + 65536 *  1 / 12 * m1 >> 16) * pcmAttackCurve[p     ] >> 14;
          pcmBuffer[i +  3] = (65536 * 10 / 12 * m0 + 65536 *  2 / 12 * m1 >> 16) * pcmAttackCurve[p +  1] >> 14;
          pcmBuffer[i +  5] = (         3      * m0 +                   m1 >>  2) * pcmAttackCurve[p +  2] >> 14;
          pcmBuffer[i +  7] = (65536 *  8 / 12 * m0 + 65536 *  4 / 12 * m1 >> 16) * pcmAttackCurve[p +  3] >> 14;
          pcmBuffer[i +  9] = (65536 *  7 / 12 * m0 + 65536 *  5 / 12 * m1 >> 16) * pcmAttackCurve[p +  4] >> 14;
          pcmBuffer[i + 11] = (                  m0 +                   m1 >>  1) * pcmAttackCurve[p +  5] >> 14;
          pcmBuffer[i + 13] = (65536 *  5 / 12 * m0 + 65536 *  7 / 12 * m1 >> 16) * pcmAttackCurve[p +  6] >> 14;
          pcmBuffer[i + 15] = (65536 *  4 / 12 * m0 + 65536 *  8 / 12 * m1 >> 16) * pcmAttackCurve[p +  7] >> 14;
          pcmBuffer[i + 17] = (                  m0 +          3      * m1 >>  2) * pcmAttackCurve[p +  8] >> 14;
          pcmBuffer[i + 19] = (65536 *  2 / 12 * m0 + 65536 * 10 / 12 * m1 >> 16) * pcmAttackCurve[p +  9] >> 14;
          pcmBuffer[i + 21] = (65536 *  1 / 12 * m0 + 65536 * 11 / 12 * m1 >> 16) * pcmAttackCurve[p + 10] >> 14;
          pcmBuffer[i + 23] = (                                         m1      ) * pcmAttackCurve[p + 11] >> 14;
          p += 12;
          pcmPanRight = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i +  1] = (
            pcmBuffer[i +  3] =
            pcmBuffer[i +  5] =
            pcmBuffer[i +  7] =
            pcmBuffer[i +  9] =
            pcmBuffer[i + 11] =
            pcmBuffer[i + 13] =
            pcmBuffer[i + 15] =
            pcmBuffer[i + 17] =
            pcmBuffer[i + 19] =
            pcmBuffer[i + 21] =
            pcmBuffer[i + 23] = 0);
        }
        pcmDecodedData1 = m1;
        pcmPointer = i + 24;
      }
    },  //PIP_STEREO_LINEAR_12

    //ステレオ 線形補間 7812.50Hz
    PIP_STEREO_LINEAR_8 {
      @Override public void write (int m1) {
        int i = pcmPointer;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          int m0 = pcmDecodedData1;
          pcmBuffer[i     ] = 7 * m0 +     m1 >> 3;
          pcmBuffer[i +  2] = 3 * m0 +     m1 >> 2;
          pcmBuffer[i +  4] = 5 * m0 + 3 * m1 >> 3;
          pcmBuffer[i +  6] =     m0 +     m1 >> 1;
          pcmBuffer[i +  8] = 3 * m0 + 5 * m1 >> 3;
          pcmBuffer[i + 10] =     m0 + 3 * m1 >> 2;
          pcmBuffer[i + 12] =     m0 + 7 * m1 >> 3;
          pcmBuffer[i + 14] =              m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          int m0 = pcmDecodedData1;
          pcmBuffer[i     ] = (7 * m0 +     m1 >> 3) * pcmAttackCurve[p    ] >> 14;
          pcmBuffer[i +  2] = (3 * m0 +     m1 >> 2) * pcmAttackCurve[p + 1] >> 14;
          pcmBuffer[i +  4] = (5 * m0 + 3 * m1 >> 3) * pcmAttackCurve[p + 2] >> 14;
          pcmBuffer[i +  6] = (    m0 +     m1 >> 1) * pcmAttackCurve[p + 3] >> 14;
          pcmBuffer[i +  8] = (3 * m0 + 5 * m1 >> 3) * pcmAttackCurve[p + 4] >> 14;
          pcmBuffer[i + 10] = (    m0 + 3 * m1 >> 2) * pcmAttackCurve[p + 5] >> 14;
          pcmBuffer[i + 12] = (    m0 + 7 * m1 >> 3) * pcmAttackCurve[p + 6] >> 14;
          pcmBuffer[i + 14] = (             m1     ) * pcmAttackCurve[p + 7] >> 14;
          p += 8;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  2] =
            pcmBuffer[i +  4] =
            pcmBuffer[i +  6] =
            pcmBuffer[i +  8] =
            pcmBuffer[i + 10] =
            pcmBuffer[i + 12] =
            pcmBuffer[i + 14] = 0);
        }
        p = pcmPanRight;
        if (p > 0) {  //ON
          int m0 = pcmDecodedData1;
          pcmBuffer[i +  1] = 7 * m0 +     m1 >> 3;
          pcmBuffer[i +  3] = 3 * m0 +     m1 >> 2;
          pcmBuffer[i +  5] = 5 * m0 + 3 * m1 >> 3;
          pcmBuffer[i +  7] =     m0 +     m1 >> 1;
          pcmBuffer[i +  9] = 3 * m0 + 5 * m1 >> 3;
          pcmBuffer[i + 11] =     m0 + 3 * m1 >> 2;
          pcmBuffer[i + 13] =     m0 + 7 * m1 >> 3;
          pcmBuffer[i + 15] =              m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          int m0 = pcmDecodedData1;
          pcmBuffer[i +  1] = (7 * m0 +     m1 >> 3) * pcmAttackCurve[p    ] >> 14;
          pcmBuffer[i +  3] = (3 * m0 +     m1 >> 2) * pcmAttackCurve[p + 1] >> 14;
          pcmBuffer[i +  5] = (5 * m0 + 3 * m1 >> 3) * pcmAttackCurve[p + 2] >> 14;
          pcmBuffer[i +  7] = (    m0 +     m1 >> 1) * pcmAttackCurve[p + 3] >> 14;
          pcmBuffer[i +  9] = (3 * m0 + 5 * m1 >> 3) * pcmAttackCurve[p + 4] >> 14;
          pcmBuffer[i + 11] = (    m0 + 3 * m1 >> 2) * pcmAttackCurve[p + 5] >> 14;
          pcmBuffer[i + 13] = (    m0 + 7 * m1 >> 3) * pcmAttackCurve[p + 6] >> 14;
          pcmBuffer[i + 15] = (             m1     ) * pcmAttackCurve[p + 7] >> 14;
          p += 8;
          pcmPanRight = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i +  1] = (
            pcmBuffer[i +  3] =
            pcmBuffer[i +  5] =
            pcmBuffer[i +  7] =
            pcmBuffer[i +  9] =
            pcmBuffer[i + 11] =
            pcmBuffer[i + 13] =
            pcmBuffer[i + 15] = 0);
        }
        pcmDecodedData1 = m1;
        pcmPointer = i + 16;
      }
    },  //PIP_STEREO_LINEAR_8

    //ステレオ 線形補間 10416.67Hz
    PIP_STEREO_LINEAR_6 {
      @Override public void write (int m1) {
        int i = pcmPointer;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          int m0 = pcmDecodedData1;
          pcmBuffer[i     ] = 65536 * 5 / 6 * m0 + 65536 * 1 / 6 * m1 >> 16;
          pcmBuffer[i +  2] = 65536 * 4 / 6 * m0 + 65536 * 2 / 6 * m1 >> 16;
          pcmBuffer[i +  4] =                 m0 +                 m1 >>  1;
          pcmBuffer[i +  6] = 65536 * 2 / 6 * m0 + 65536 * 4 / 6 * m1 >> 16;
          pcmBuffer[i +  8] = 65536 * 1 / 6 * m0 + 65536 * 5 / 6 * m1 >> 16;
          pcmBuffer[i + 10] =                                      m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          int m0 = pcmDecodedData1;
          pcmBuffer[i     ] = (65536 * 5 / 6 * m0 + 65536 * 1 / 6 * m1 >> 16) * pcmAttackCurve[p    ] >> 14;
          pcmBuffer[i +  2] = (65536 * 4 / 6 * m0 + 65536 * 2 / 6 * m1 >> 16) * pcmAttackCurve[p + 1] >> 14;
          pcmBuffer[i +  4] = (                m0 +                 m1 >>  1) * pcmAttackCurve[p + 2] >> 14;
          pcmBuffer[i +  6] = (65536 * 2 / 6 * m0 + 65536 * 4 / 6 * m1 >> 16) * pcmAttackCurve[p + 3] >> 14;
          pcmBuffer[i +  8] = (65536 * 1 / 6 * m0 + 65536 * 5 / 6 * m1 >> 16) * pcmAttackCurve[p + 4] >> 14;
          pcmBuffer[i + 10] = (                                     m1      ) * pcmAttackCurve[p + 5] >> 14;
          p += 6;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  2] =
            pcmBuffer[i +  4] =
            pcmBuffer[i +  6] =
            pcmBuffer[i +  8] =
            pcmBuffer[i + 10] = 0);
        }
        p = pcmPanRight;
        if (p > 0) {  //ON
          int m0 = pcmDecodedData1;
          pcmBuffer[i +  1] = 65536 * 5 / 6 * m0 + 65536 * 1 / 6 * m1 >> 16;
          pcmBuffer[i +  3] = 65536 * 4 / 6 * m0 + 65536 * 2 / 6 * m1 >> 16;
          pcmBuffer[i +  5] =                 m0 +                 m1 >>  1;
          pcmBuffer[i +  7] = 65536 * 2 / 6 * m0 + 65536 * 4 / 6 * m1 >> 16;
          pcmBuffer[i +  9] = 65536 * 1 / 6 * m0 + 65536 * 5 / 6 * m1 >> 16;
          pcmBuffer[i + 11] =                                      m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          int m0 = pcmDecodedData1;
          pcmBuffer[i +  1] = (65536 * 5 / 6 * m0 + 65536 * 1 / 6 * m1 >> 16) * pcmAttackCurve[p    ] >> 14;
          pcmBuffer[i +  3] = (65536 * 4 / 6 * m0 + 65536 * 2 / 6 * m1 >> 16) * pcmAttackCurve[p + 1] >> 14;
          pcmBuffer[i +  5] = (                m0 +                 m1 >>  1) * pcmAttackCurve[p + 2] >> 14;
          pcmBuffer[i +  7] = (65536 * 2 / 6 * m0 + 65536 * 4 / 6 * m1 >> 16) * pcmAttackCurve[p + 3] >> 14;
          pcmBuffer[i +  9] = (65536 * 1 / 6 * m0 + 65536 * 5 / 6 * m1 >> 16) * pcmAttackCurve[p + 4] >> 14;
          pcmBuffer[i + 11] = (                                     m1      ) * pcmAttackCurve[p + 5] >> 14;
          p += 6;
          pcmPanRight = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i +  1] = (
            pcmBuffer[i +  3] =
            pcmBuffer[i +  5] =
            pcmBuffer[i +  7] =
            pcmBuffer[i +  9] =
            pcmBuffer[i + 11] = 0);
        }
        pcmDecodedData1 = m1;
        pcmPointer = i + 12;
      }
    },  //PIP_STEREO_LINEAR_6

    //ステレオ 線形補間 15625.00Hz
    PIP_STEREO_LINEAR_4 {
      @Override public void write (int m1) {
        int i = pcmPointer;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          int m0 = pcmDecodedData1;
          pcmBuffer[i    ] = 3 * m0 +     m1 >> 2;
          pcmBuffer[i + 2] =     m0 +     m1 >> 1;
          pcmBuffer[i + 4] =     m0 + 3 * m1 >> 2;
          pcmBuffer[i + 6] =              m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          int m0 = pcmDecodedData1;
          pcmBuffer[i    ] = (3 * m0 +     m1 >> 2) * pcmAttackCurve[p    ] >> 14;
          pcmBuffer[i + 2] = (    m0 +     m1 >> 1) * pcmAttackCurve[p + 1] >> 14;
          pcmBuffer[i + 4] = (    m0 + 3 * m1 >> 2) * pcmAttackCurve[p + 2] >> 14;
          pcmBuffer[i + 6] = (             m1     ) * pcmAttackCurve[p + 3] >> 14;
          p += 4;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i    ] = (
            pcmBuffer[i + 2] =
            pcmBuffer[i + 4] =
            pcmBuffer[i + 6] = 0);
        }
        p = pcmPanRight;
        if (p > 0) {  //ON
          int m0 = pcmDecodedData1;
          pcmBuffer[i + 1] = 3 * m0 +     m1 >> 2;
          pcmBuffer[i + 3] =     m0 +     m1 >> 1;
          pcmBuffer[i + 5] =     m0 + 3 * m1 >> 2;
          pcmBuffer[i + 7] =              m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          int m0 = pcmDecodedData1;
          pcmBuffer[i + 1] = (3 * m0 +     m1 >> 2) * pcmAttackCurve[p    ] >> 14;
          pcmBuffer[i + 3] = (    m0 +     m1 >> 1) * pcmAttackCurve[p + 1] >> 14;
          pcmBuffer[i + 5] = (    m0 + 3 * m1 >> 2) * pcmAttackCurve[p + 2] >> 14;
          pcmBuffer[i + 7] = (             m1     ) * pcmAttackCurve[p + 3] >> 14;
          p += 4;
          pcmPanRight = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i + 1] = (
            pcmBuffer[i + 3] =
            pcmBuffer[i + 5] =
            pcmBuffer[i + 7] = 0);
        }
        pcmDecodedData1 = m1;
        pcmPointer = i + 8;
      }
    },  //PIP_STEREO_LINEAR_4

    //ステレオ 線形補間 20833.33Hz
    PIP_STEREO_LINEAR_3 {
      @Override public void write (int m1) {
        int i = pcmPointer;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          int m0 = pcmDecodedData1;
          pcmBuffer[i    ] = 65536 * 2 / 3 * m0 + 65536 * 1 / 3 * m1 >> 16;
          pcmBuffer[i + 2] = 65536 * 1 / 3 * m0 + 65536 * 2 / 3 * m1 >> 16;
          pcmBuffer[i + 4] =                                      m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          int m0 = pcmDecodedData1;
          pcmBuffer[i    ] = (65536 * 2 / 3 * m0 + 65536 * 1 / 3 * m1 >> 16) * pcmAttackCurve[p    ] >> 14;
          pcmBuffer[i + 2] = (65536 * 1 / 3 * m0 + 65536 * 2 / 3 * m1 >> 16) * pcmAttackCurve[p + 1] >> 14;
          pcmBuffer[i + 4] = (                                     m1      ) * pcmAttackCurve[p + 2] >> 14;
          p += 3;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i    ] = (
            pcmBuffer[i + 2] =
            pcmBuffer[i + 4] = 0);
        }
        p = pcmPanRight;
        if (p > 0) {  //ON
          int m0 = pcmDecodedData1;
          pcmBuffer[i + 1] = 65536 * 2 / 3 * m0 + 65536 * 1 / 3 * m1 >> 16;
          pcmBuffer[i + 3] = 65536 * 1 / 3 * m0 + 65536 * 2 / 3 * m1 >> 16;
          pcmBuffer[i + 5] =                                      m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          int m0 = pcmDecodedData1;
          pcmBuffer[i + 1] = (65536 * 2 / 3 * m0 + 65536 * 1 / 3 * m1 >> 16) * pcmAttackCurve[p    ] >> 14;
          pcmBuffer[i + 3] = (65536 * 1 / 3 * m0 + 65536 * 2 / 3 * m1 >> 16) * pcmAttackCurve[p + 1] >> 14;
          pcmBuffer[i + 5] = (                                     m1      ) * pcmAttackCurve[p + 2] >> 14;
          p += 3;
          pcmPanRight = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i + 1] = (
            pcmBuffer[i + 3] =
            pcmBuffer[i + 5] = 0);
        }
        pcmDecodedData1 = m1;
        pcmPointer = i + 6;
      }
    },  //PIP_STEREO_LINEAR_3

    //ステレオ 線形補間 31250.00Hz
    PIP_STEREO_LINEAR_2 {
      @Override public void write (int m1) {
        int i = pcmPointer;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          int m0 = pcmDecodedData1;
          pcmBuffer[i    ] = m0 + m1 >> 1;
          pcmBuffer[i + 2] =      m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          int m0 = pcmDecodedData1;
          pcmBuffer[i    ] = (m0 + m1 >> 1) * pcmAttackCurve[p    ] >> 14;
          pcmBuffer[i + 2] = (     m1     ) * pcmAttackCurve[p + 1] >> 14;
          p += 2;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i    ] = (
            pcmBuffer[i + 2] = 0);
        }
        p = pcmPanRight;
        if (p > 0) {  //ON
          int m0 = pcmDecodedData1;
          pcmBuffer[i + 1] = m0 + m1 >> 1;
          pcmBuffer[i + 3] =      m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          int m0 = pcmDecodedData1;
          pcmBuffer[i + 1] = (m0 + m1 >> 1) * pcmAttackCurve[p    ] >> 14;
          pcmBuffer[i + 3] = (     m1     ) * pcmAttackCurve[p + 1] >> 14;
          p += 2;
          pcmPanRight = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i + 1] = (
            pcmBuffer[i + 3] = 0);
        }
        pcmDecodedData1 = m1;
        pcmPointer = i + 4;
      }
    },  //PIP_STEREO_LINEAR_2

    //ステレオ エルミート補間 3906.25Hz
    PIP_STEREO_HERMITE_16 {
      @Override public void write (int m2) {
        //  echo read("hermite.gp");hermite_code(2,16) | gp-2.7 -q
        int i = pcmPointer;
        int mm = pcmDecodedData3;
        int m0 = pcmDecodedData2;
        int m1 = pcmDecodedData1;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          pcmBuffer[i     ] =          -225 * mm + 8115 * m0 +  317 * m1 -   15 * m2 >> 13;
          pcmBuffer[i +  2] =           -49 * mm +  987 * m0 +   93 * m1 -    7 * m2 >> 10;
          pcmBuffer[i +  4] =          -507 * mm + 7553 * m0 + 1263 * m1 -  117 * m2 >> 13;
          pcmBuffer[i +  6] =            -9 * mm +  111 * m0 +   29 * m1 -    3 * m2 >>  7;
          pcmBuffer[i +  8] =          -605 * mm + 6567 * m0 + 2505 * m1 -  275 * m2 >> 13;
          pcmBuffer[i + 10] =           -75 * mm +  745 * m0 +  399 * m1 -   45 * m2 >> 10;
          pcmBuffer[i + 12] =          -567 * mm + 5301 * m0 + 3899 * m1 -  441 * m2 >> 13;
          pcmBuffer[i + 14] =                -mm +    9 *      (m0 + m1) -        m2 >>  4;
          pcmBuffer[i + 16] =          -441 * mm + 3899 * m0 + 5301 * m1 -  567 * m2 >> 13;
          pcmBuffer[i + 18] =           -45 * mm +  399 * m0 +  745 * m1 -   75 * m2 >> 10;
          pcmBuffer[i + 20] =          -275 * mm + 2505 * m0 + 6567 * m1 -  605 * m2 >> 13;
          pcmBuffer[i + 22] =            -3 * mm +   29 * m0 +  111 * m1 -    9 * m2 >>  7;
          pcmBuffer[i + 24] =          -117 * mm + 1263 * m0 + 7553 * m1 -  507 * m2 >> 13;
          pcmBuffer[i + 26] =            -7 * mm +   93 * m0 +  987 * m1 -   49 * m2 >> 10;
          pcmBuffer[i + 28] =           -15 * mm +  317 * m0 + 8115 * m1 -  225 * m2 >> 13;
          pcmBuffer[i + 30] =                                         m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i     ] = (         -225 * mm + 8115 * m0 +  317 * m1 -   15 * m2 >> 13) * pcmAttackCurve[p     ] >> 14;
          pcmBuffer[i +  2] = (          -49 * mm +  987 * m0 +   93 * m1 -    7 * m2 >> 10) * pcmAttackCurve[p +  1] >> 14;
          pcmBuffer[i +  4] = (         -507 * mm + 7553 * m0 + 1263 * m1 -  117 * m2 >> 13) * pcmAttackCurve[p +  2] >> 14;
          pcmBuffer[i +  6] = (           -9 * mm +  111 * m0 +   29 * m1 -    3 * m2 >>  7) * pcmAttackCurve[p +  3] >> 14;
          pcmBuffer[i +  8] = (         -605 * mm + 6567 * m0 + 2505 * m1 -  275 * m2 >> 13) * pcmAttackCurve[p +  4] >> 14;
          pcmBuffer[i + 10] = (          -75 * mm +  745 * m0 +  399 * m1 -   45 * m2 >> 10) * pcmAttackCurve[p +  5] >> 14;
          pcmBuffer[i + 12] = (         -567 * mm + 5301 * m0 + 3899 * m1 -  441 * m2 >> 13) * pcmAttackCurve[p +  6] >> 14;
          pcmBuffer[i + 14] = (               -mm +    9 *      (m0 + m1) -        m2 >>  4) * pcmAttackCurve[p +  7] >> 14;
          pcmBuffer[i + 16] = (         -441 * mm + 3899 * m0 + 5301 * m1 -  567 * m2 >> 13) * pcmAttackCurve[p +  8] >> 14;
          pcmBuffer[i + 18] = (          -45 * mm +  399 * m0 +  745 * m1 -   75 * m2 >> 10) * pcmAttackCurve[p +  9] >> 14;
          pcmBuffer[i + 20] = (         -275 * mm + 2505 * m0 + 6567 * m1 -  605 * m2 >> 13) * pcmAttackCurve[p + 10] >> 14;
          pcmBuffer[i + 22] = (           -3 * mm +   29 * m0 +  111 * m1 -    9 * m2 >>  7) * pcmAttackCurve[p + 11] >> 14;
          pcmBuffer[i + 24] = (         -117 * mm + 1263 * m0 + 7553 * m1 -  507 * m2 >> 13) * pcmAttackCurve[p + 12] >> 14;
          pcmBuffer[i + 26] = (           -7 * mm +   93 * m0 +  987 * m1 -   49 * m2 >> 10) * pcmAttackCurve[p + 13] >> 14;
          pcmBuffer[i + 28] = (          -15 * mm +  317 * m0 + 8115 * m1 -  225 * m2 >> 13) * pcmAttackCurve[p + 14] >> 14;
          pcmBuffer[i + 30] = (                                        m1                  ) * pcmAttackCurve[p + 15] >> 14;
          p += 16;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  2] =
            pcmBuffer[i +  4] =
            pcmBuffer[i +  6] =
            pcmBuffer[i +  8] =
            pcmBuffer[i + 10] =
            pcmBuffer[i + 12] =
            pcmBuffer[i + 14] =
            pcmBuffer[i + 16] =
            pcmBuffer[i + 18] =
            pcmBuffer[i + 20] =
            pcmBuffer[i + 22] =
            pcmBuffer[i + 24] =
            pcmBuffer[i + 26] =
            pcmBuffer[i + 28] =
            pcmBuffer[i + 30] = 0);
        }
        p = pcmPanRight;
        if (p > 0) {  //ON
          pcmBuffer[i +  1] =          -225 * mm + 8115 * m0 +  317 * m1 -   15 * m2 >> 13;
          pcmBuffer[i +  3] =           -49 * mm +  987 * m0 +   93 * m1 -    7 * m2 >> 10;
          pcmBuffer[i +  5] =          -507 * mm + 7553 * m0 + 1263 * m1 -  117 * m2 >> 13;
          pcmBuffer[i +  7] =            -9 * mm +  111 * m0 +   29 * m1 -    3 * m2 >>  7;
          pcmBuffer[i +  9] =          -605 * mm + 6567 * m0 + 2505 * m1 -  275 * m2 >> 13;
          pcmBuffer[i + 11] =           -75 * mm +  745 * m0 +  399 * m1 -   45 * m2 >> 10;
          pcmBuffer[i + 13] =          -567 * mm + 5301 * m0 + 3899 * m1 -  441 * m2 >> 13;
          pcmBuffer[i + 15] =                -mm +    9 *      (m0 + m1) -        m2 >>  4;
          pcmBuffer[i + 17] =          -441 * mm + 3899 * m0 + 5301 * m1 -  567 * m2 >> 13;
          pcmBuffer[i + 19] =           -45 * mm +  399 * m0 +  745 * m1 -   75 * m2 >> 10;
          pcmBuffer[i + 21] =          -275 * mm + 2505 * m0 + 6567 * m1 -  605 * m2 >> 13;
          pcmBuffer[i + 23] =            -3 * mm +   29 * m0 +  111 * m1 -    9 * m2 >>  7;
          pcmBuffer[i + 25] =          -117 * mm + 1263 * m0 + 7553 * m1 -  507 * m2 >> 13;
          pcmBuffer[i + 27] =            -7 * mm +   93 * m0 +  987 * m1 -   49 * m2 >> 10;
          pcmBuffer[i + 29] =           -15 * mm +  317 * m0 + 8115 * m1 -  225 * m2 >> 13;
          pcmBuffer[i + 31] =                                         m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i +  1] = (         -225 * mm + 8115 * m0 +  317 * m1 -   15 * m2 >> 13) * pcmAttackCurve[p     ] >> 14;
          pcmBuffer[i +  3] = (          -49 * mm +  987 * m0 +   93 * m1 -    7 * m2 >> 10) * pcmAttackCurve[p +  1] >> 14;
          pcmBuffer[i +  5] = (         -507 * mm + 7553 * m0 + 1263 * m1 -  117 * m2 >> 13) * pcmAttackCurve[p +  2] >> 14;
          pcmBuffer[i +  7] = (           -9 * mm +  111 * m0 +   29 * m1 -    3 * m2 >>  7) * pcmAttackCurve[p +  3] >> 14;
          pcmBuffer[i +  9] = (         -605 * mm + 6567 * m0 + 2505 * m1 -  275 * m2 >> 13) * pcmAttackCurve[p +  4] >> 14;
          pcmBuffer[i + 11] = (          -75 * mm +  745 * m0 +  399 * m1 -   45 * m2 >> 10) * pcmAttackCurve[p +  5] >> 14;
          pcmBuffer[i + 13] = (         -567 * mm + 5301 * m0 + 3899 * m1 -  441 * m2 >> 13) * pcmAttackCurve[p +  6] >> 14;
          pcmBuffer[i + 15] = (               -mm +    9 *      (m0 + m1) -        m2 >>  4) * pcmAttackCurve[p +  7] >> 14;
          pcmBuffer[i + 17] = (         -441 * mm + 3899 * m0 + 5301 * m1 -  567 * m2 >> 13) * pcmAttackCurve[p +  8] >> 14;
          pcmBuffer[i + 19] = (          -45 * mm +  399 * m0 +  745 * m1 -   75 * m2 >> 10) * pcmAttackCurve[p +  9] >> 14;
          pcmBuffer[i + 21] = (         -275 * mm + 2505 * m0 + 6567 * m1 -  605 * m2 >> 13) * pcmAttackCurve[p + 10] >> 14;
          pcmBuffer[i + 23] = (           -3 * mm +   29 * m0 +  111 * m1 -    9 * m2 >>  7) * pcmAttackCurve[p + 11] >> 14;
          pcmBuffer[i + 25] = (         -117 * mm + 1263 * m0 + 7553 * m1 -  507 * m2 >> 13) * pcmAttackCurve[p + 12] >> 14;
          pcmBuffer[i + 27] = (           -7 * mm +   93 * m0 +  987 * m1 -   49 * m2 >> 10) * pcmAttackCurve[p + 13] >> 14;
          pcmBuffer[i + 29] = (          -15 * mm +  317 * m0 + 8115 * m1 -  225 * m2 >> 13) * pcmAttackCurve[p + 14] >> 14;
          pcmBuffer[i + 31] = (                                        m1                  ) * pcmAttackCurve[p + 15] >> 14;
          p += 16;
          pcmPanRight = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i +  1] = (
            pcmBuffer[i +  3] =
            pcmBuffer[i +  5] =
            pcmBuffer[i +  7] =
            pcmBuffer[i +  9] =
            pcmBuffer[i + 11] =
            pcmBuffer[i + 13] =
            pcmBuffer[i + 15] =
            pcmBuffer[i + 17] =
            pcmBuffer[i + 19] =
            pcmBuffer[i + 21] =
            pcmBuffer[i + 23] =
            pcmBuffer[i + 25] =
            pcmBuffer[i + 27] =
            pcmBuffer[i + 29] =
            pcmBuffer[i + 31] = 0);
        }
        pcmDecodedData3 = m0;
        pcmDecodedData2 = m1;
        pcmDecodedData1 = m2;
        pcmPointer = i + 32;
      }
    },  //PIP_STEREO_HERMITE_16

    //ステレオ エルミート補間 5208.33Hz
    PIP_STEREO_HERMITE_12 {
      @Override public void write (int m2) {
        //  echo read("hermite.gp");hermite_code(2,12) | gp-2.7 -q
        int i = pcmPointer;
        int mm = pcmDecodedData3;
        int m0 = pcmDecodedData2;
        int m1 = pcmDecodedData1;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          pcmBuffer[i     ] = (int) (( -121 * mm + 3399 * m0 +  189 * m1 -   11 * m2) *    1242757L >> 32);
          pcmBuffer[i +  2] = (int) ((  -25 * mm +  405 * m0 +   57 * m1 -    5 * m2) *    9942054L >> 32);
          pcmBuffer[i +  4] =            -9 * mm +  111 * m0 +   29 * m1 -    3 * m2 >>  7;
          pcmBuffer[i +  6] = (int) ((   -2 * mm +   21 * m0 +    9 * m1 -        m2) *  159072863L >> 32);
          pcmBuffer[i +  8] = (int) (( -245 * mm + 2331 * m0 + 1545 * m1 -  175 * m2) *    1242757L >> 32);
          pcmBuffer[i + 10] =                -mm +    9 *      (m0 + m1) -        m2 >>  4;
          pcmBuffer[i + 12] = (int) (( -175 * mm + 1545 * m0 + 2331 * m1 -  245 * m2) *    1242757L >> 32);
          pcmBuffer[i + 14] = (int) ((       -mm +    9 * m0 +   21 * m1 -    2 * m2) *  159072863L >> 32);
          pcmBuffer[i + 16] =            -3 * mm +   29 * m0 +  111 * m1 -    9 * m2 >>  7;
          pcmBuffer[i + 18] = (int) ((   -5 * mm +   57 * m0 +  405 * m1 -   25 * m2) *    9942054L >> 32);
          pcmBuffer[i + 20] = (int) ((  -11 * mm +  189 * m0 + 3399 * m1 -  121 * m2) *    1242757L >> 32);
          pcmBuffer[i + 22] =                                         m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i     ] = (int) (( -121 * mm + 3399 * m0 +  189 * m1 -   11 * m2) *    1242757L >> 32) * pcmAttackCurve[p     ] >> 14;
          pcmBuffer[i +  2] = (int) ((  -25 * mm +  405 * m0 +   57 * m1 -    5 * m2) *    9942054L >> 32) * pcmAttackCurve[p +  1] >> 14;
          pcmBuffer[i +  4] =       (    -9 * mm +  111 * m0 +   29 * m1 -    3 * m2 >>  7               ) * pcmAttackCurve[p +  2] >> 14;
          pcmBuffer[i +  6] = (int) ((   -2 * mm +   21 * m0 +    9 * m1 -        m2) *  159072863L >> 32) * pcmAttackCurve[p +  3] >> 14;
          pcmBuffer[i +  8] = (int) (( -245 * mm + 2331 * m0 + 1545 * m1 -  175 * m2) *    1242757L >> 32) * pcmAttackCurve[p +  4] >> 14;
          pcmBuffer[i + 10] =       (        -mm +    9 *      (m0 + m1) -        m2 >>  4               ) * pcmAttackCurve[p +  5] >> 14;
          pcmBuffer[i + 12] = (int) (( -175 * mm + 1545 * m0 + 2331 * m1 -  245 * m2) *    1242757L >> 32) * pcmAttackCurve[p +  6] >> 14;
          pcmBuffer[i + 14] = (int) ((       -mm +    9 * m0 +   21 * m1 -    2 * m2) *  159072863L >> 32) * pcmAttackCurve[p +  7] >> 14;
          pcmBuffer[i + 16] =       (    -3 * mm +   29 * m0 +  111 * m1 -    9 * m2 >>  7               ) * pcmAttackCurve[p +  8] >> 14;
          pcmBuffer[i + 18] = (int) ((   -5 * mm +   57 * m0 +  405 * m1 -   25 * m2) *    9942054L >> 32) * pcmAttackCurve[p +  9] >> 14;
          pcmBuffer[i + 20] = (int) ((  -11 * mm +  189 * m0 + 3399 * m1 -  121 * m2) *    1242757L >> 32) * pcmAttackCurve[p + 10] >> 14;
          pcmBuffer[i + 22] =       (                                 m1                                 ) * pcmAttackCurve[p + 11] >> 14;
          p += 12;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  2] =
            pcmBuffer[i +  4] =
            pcmBuffer[i +  6] =
            pcmBuffer[i +  8] =
            pcmBuffer[i + 10] =
            pcmBuffer[i + 12] =
            pcmBuffer[i + 14] =
            pcmBuffer[i + 16] =
            pcmBuffer[i + 18] =
            pcmBuffer[i + 20] =
            pcmBuffer[i + 22] = 0);
        }
        p = pcmPanRight;
        if (p > 0) {  //ON
          pcmBuffer[i +  1] = (int) (( -121 * mm + 3399 * m0 +  189 * m1 -   11 * m2) *    1242757L >> 32);
          pcmBuffer[i +  3] = (int) ((  -25 * mm +  405 * m0 +   57 * m1 -    5 * m2) *    9942054L >> 32);
          pcmBuffer[i +  5] =            -9 * mm +  111 * m0 +   29 * m1 -    3 * m2 >>  7;
          pcmBuffer[i +  7] = (int) ((   -2 * mm +   21 * m0 +    9 * m1 -        m2) *  159072863L >> 32);
          pcmBuffer[i +  9] = (int) (( -245 * mm + 2331 * m0 + 1545 * m1 -  175 * m2) *    1242757L >> 32);
          pcmBuffer[i + 11] =                -mm +    9 *      (m0 + m1) -        m2 >>  4;
          pcmBuffer[i + 13] = (int) (( -175 * mm + 1545 * m0 + 2331 * m1 -  245 * m2) *    1242757L >> 32);
          pcmBuffer[i + 15] = (int) ((       -mm +    9 * m0 +   21 * m1 -    2 * m2) *  159072863L >> 32);
          pcmBuffer[i + 17] =            -3 * mm +   29 * m0 +  111 * m1 -    9 * m2 >>  7;
          pcmBuffer[i + 19] = (int) ((   -5 * mm +   57 * m0 +  405 * m1 -   25 * m2) *    9942054L >> 32);
          pcmBuffer[i + 21] = (int) ((  -11 * mm +  189 * m0 + 3399 * m1 -  121 * m2) *    1242757L >> 32);
          pcmBuffer[i + 23] =                                         m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i +  1] = (int) (( -121 * mm + 3399 * m0 +  189 * m1 -   11 * m2) *    1242757L >> 32) * pcmAttackCurve[p     ] >> 14;
          pcmBuffer[i +  3] = (int) ((  -25 * mm +  405 * m0 +   57 * m1 -    5 * m2) *    9942054L >> 32) * pcmAttackCurve[p +  1] >> 14;
          pcmBuffer[i +  5] =       (    -9 * mm +  111 * m0 +   29 * m1 -    3 * m2 >>  7               ) * pcmAttackCurve[p +  2] >> 14;
          pcmBuffer[i +  7] = (int) ((   -2 * mm +   21 * m0 +    9 * m1 -        m2) *  159072863L >> 32) * pcmAttackCurve[p +  3] >> 14;
          pcmBuffer[i +  9] = (int) (( -245 * mm + 2331 * m0 + 1545 * m1 -  175 * m2) *    1242757L >> 32) * pcmAttackCurve[p +  4] >> 14;
          pcmBuffer[i + 11] =       (        -mm +    9 *      (m0 + m1) -        m2 >>  4               ) * pcmAttackCurve[p +  5] >> 14;
          pcmBuffer[i + 13] = (int) (( -175 * mm + 1545 * m0 + 2331 * m1 -  245 * m2) *    1242757L >> 32) * pcmAttackCurve[p +  6] >> 14;
          pcmBuffer[i + 15] = (int) ((       -mm +    9 * m0 +   21 * m1 -    2 * m2) *  159072863L >> 32) * pcmAttackCurve[p +  7] >> 14;
          pcmBuffer[i + 17] =       (    -3 * mm +   29 * m0 +  111 * m1 -    9 * m2 >>  7               ) * pcmAttackCurve[p +  8] >> 14;
          pcmBuffer[i + 19] = (int) ((   -5 * mm +   57 * m0 +  405 * m1 -   25 * m2) *    9942054L >> 32) * pcmAttackCurve[p +  9] >> 14;
          pcmBuffer[i + 21] = (int) ((  -11 * mm +  189 * m0 + 3399 * m1 -  121 * m2) *    1242757L >> 32) * pcmAttackCurve[p + 10] >> 14;
          pcmBuffer[i + 23] =       (                                 m1                                 ) * pcmAttackCurve[p + 11] >> 14;
          p += 12;
          pcmPanRight = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i +  1] = (
            pcmBuffer[i +  3] =
            pcmBuffer[i +  5] =
            pcmBuffer[i +  7] =
            pcmBuffer[i +  9] =
            pcmBuffer[i + 11] =
            pcmBuffer[i + 13] =
            pcmBuffer[i + 15] =
            pcmBuffer[i + 17] =
            pcmBuffer[i + 19] =
            pcmBuffer[i + 21] =
            pcmBuffer[i + 23] = 0);
        }
        pcmDecodedData3 = m0;
        pcmDecodedData2 = m1;
        pcmDecodedData1 = m2;
        pcmPointer = i + 24;
      }
    },  //PIP_STEREO_HERMITE_12

    //ステレオ エルミート補間 7812.50Hz
    PIP_STEREO_HERMITE_8 {
      @Override public void write (int m2) {
        //  echo read("hermite.gp");hermite_code(2,8) | gp-2.7 -q
        int i = pcmPointer;
        int mm = pcmDecodedData3;
        int m0 = pcmDecodedData2;
        int m1 = pcmDecodedData1;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          pcmBuffer[i     ] =           -49 * mm +  987 * m0 +   93 * m1 -    7 * m2 >> 10;
          pcmBuffer[i +  2] =            -9 * mm +  111 * m0 +   29 * m1 -    3 * m2 >>  7;
          pcmBuffer[i +  4] =           -75 * mm +  745 * m0 +  399 * m1 -   45 * m2 >> 10;
          pcmBuffer[i +  6] =                -mm +    9 *      (m0 + m1) -        m2 >>  4;
          pcmBuffer[i +  8] =           -45 * mm +  399 * m0 +  745 * m1 -   75 * m2 >> 10;
          pcmBuffer[i + 10] =            -3 * mm +   29 * m0 +  111 * m1 -    9 * m2 >>  7;
          pcmBuffer[i + 12] =            -7 * mm +   93 * m0 +  987 * m1 -   49 * m2 >> 10;
          pcmBuffer[i + 14] =                                         m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i     ] = (          -49 * mm +  987 * m0 +   93 * m1 -    7 * m2 >> 10) * pcmAttackCurve[p    ] >> 14;
          pcmBuffer[i +  2] = (           -9 * mm +  111 * m0 +   29 * m1 -    3 * m2 >>  7) * pcmAttackCurve[p + 1] >> 14;
          pcmBuffer[i +  4] = (          -75 * mm +  745 * m0 +  399 * m1 -   45 * m2 >> 10) * pcmAttackCurve[p + 2] >> 14;
          pcmBuffer[i +  6] = (               -mm +    9 *      (m0 + m1) -        m2 >>  4) * pcmAttackCurve[p + 3] >> 14;
          pcmBuffer[i +  8] = (          -45 * mm +  399 * m0 +  745 * m1 -   75 * m2 >> 10) * pcmAttackCurve[p + 4] >> 14;
          pcmBuffer[i + 10] = (           -3 * mm +   29 * m0 +  111 * m1 -    9 * m2 >>  7) * pcmAttackCurve[p + 5] >> 14;
          pcmBuffer[i + 12] = (           -7 * mm +   93 * m0 +  987 * m1 -   49 * m2 >> 10) * pcmAttackCurve[p + 6] >> 14;
          pcmBuffer[i + 14] = (                                        m1                  ) * pcmAttackCurve[p + 7] >> 14;
          p += 8;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  2] =
            pcmBuffer[i +  4] =
            pcmBuffer[i +  6] =
            pcmBuffer[i +  8] =
            pcmBuffer[i + 10] =
            pcmBuffer[i + 12] =
            pcmBuffer[i + 14] = 0);
        }
        p = pcmPanRight;
        if (p > 0) {  //ON
          pcmBuffer[i +  1] =           -49 * mm +  987 * m0 +   93 * m1 -    7 * m2 >> 10;
          pcmBuffer[i +  3] =            -9 * mm +  111 * m0 +   29 * m1 -    3 * m2 >>  7;
          pcmBuffer[i +  5] =           -75 * mm +  745 * m0 +  399 * m1 -   45 * m2 >> 10;
          pcmBuffer[i +  7] =                -mm +    9 *      (m0 + m1) -        m2 >>  4;
          pcmBuffer[i +  9] =           -45 * mm +  399 * m0 +  745 * m1 -   75 * m2 >> 10;
          pcmBuffer[i + 11] =            -3 * mm +   29 * m0 +  111 * m1 -    9 * m2 >>  7;
          pcmBuffer[i + 13] =            -7 * mm +   93 * m0 +  987 * m1 -   49 * m2 >> 10;
          pcmBuffer[i + 15] =                                         m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i +  1] = (          -49 * mm +  987 * m0 +   93 * m1 -    7 * m2 >> 10) * pcmAttackCurve[p    ] >> 14;
          pcmBuffer[i +  3] = (           -9 * mm +  111 * m0 +   29 * m1 -    3 * m2 >>  7) * pcmAttackCurve[p + 1] >> 14;
          pcmBuffer[i +  5] = (          -75 * mm +  745 * m0 +  399 * m1 -   45 * m2 >> 10) * pcmAttackCurve[p + 2] >> 14;
          pcmBuffer[i +  7] = (               -mm +    9 *      (m0 + m1) -        m2 >>  4) * pcmAttackCurve[p + 3] >> 14;
          pcmBuffer[i +  9] = (          -45 * mm +  399 * m0 +  745 * m1 -   75 * m2 >> 10) * pcmAttackCurve[p + 4] >> 14;
          pcmBuffer[i + 11] = (           -3 * mm +   29 * m0 +  111 * m1 -    9 * m2 >>  7) * pcmAttackCurve[p + 5] >> 14;
          pcmBuffer[i + 13] = (           -7 * mm +   93 * m0 +  987 * m1 -   49 * m2 >> 10) * pcmAttackCurve[p + 6] >> 14;
          pcmBuffer[i + 15] = (                                        m1                  ) * pcmAttackCurve[p + 7] >> 14;
          p += 8;
          pcmPanRight = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i +  1] = (
            pcmBuffer[i +  3] =
            pcmBuffer[i +  5] =
            pcmBuffer[i +  7] =
            pcmBuffer[i +  9] =
            pcmBuffer[i + 11] =
            pcmBuffer[i + 13] =
            pcmBuffer[i + 15] = 0);
        }
        pcmDecodedData3 = m0;
        pcmDecodedData2 = m1;
        pcmDecodedData1 = m2;
        pcmPointer = i + 16;
      }
    },  //PIP_STEREO_HERMITE_8

    //ステレオ エルミート補間 10416.67Hz
    PIP_STEREO_HERMITE_6 {
      @Override public void write (int m2) {
        //  echo read("hermite.gp");hermite_code(2,6) | gp-2.7 -q
        int i = pcmPointer;
        int mm = pcmDecodedData3;
        int m0 = pcmDecodedData2;
        int m1 = pcmDecodedData1;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          pcmBuffer[i     ] = (int) ((  -25 * mm +  405 * m0 +   57 * m1 -    5 * m2) *    9942054L >> 32);
          pcmBuffer[i +  2] = (int) ((   -2 * mm +   21 * m0 +    9 * m1 -        m2) *  159072863L >> 32);
          pcmBuffer[i +  4] =                -mm +    9 *      (m0 + m1) -        m2 >>  4;
          pcmBuffer[i +  6] = (int) ((       -mm +    9 * m0 +   21 * m1 -    2 * m2) *  159072863L >> 32);
          pcmBuffer[i +  8] = (int) ((   -5 * mm +   57 * m0 +  405 * m1 -   25 * m2) *    9942054L >> 32);
          pcmBuffer[i + 10] =                                         m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i     ] = (int) ((  -25 * mm +  405 * m0 +   57 * m1 -    5 * m2) *    9942054L >> 32) * pcmAttackCurve[p    ] >> 14;
          pcmBuffer[i +  2] = (int) ((   -2 * mm +   21 * m0 +    9 * m1 -        m2) *  159072863L >> 32) * pcmAttackCurve[p + 1] >> 14;
          pcmBuffer[i +  4] =       (        -mm +    9 *      (m0 + m1) -        m2 >>  4               ) * pcmAttackCurve[p + 2] >> 14;
          pcmBuffer[i +  6] = (int) ((       -mm +    9 * m0 +   21 * m1 -    2 * m2) *  159072863L >> 32) * pcmAttackCurve[p + 3] >> 14;
          pcmBuffer[i +  8] = (int) ((   -5 * mm +   57 * m0 +  405 * m1 -   25 * m2) *    9942054L >> 32) * pcmAttackCurve[p + 4] >> 14;
          pcmBuffer[i + 10] =       (                                 m1                                 ) * pcmAttackCurve[p + 5] >> 14;
          p += 6;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i     ] = (
            pcmBuffer[i +  2] =
            pcmBuffer[i +  4] =
            pcmBuffer[i +  6] =
            pcmBuffer[i +  8] =
            pcmBuffer[i + 10] = 0);
        }
        p = pcmPanRight;
        if (p > 0) {  //ON
          pcmBuffer[i +  1] = (int) ((  -25 * mm +  405 * m0 +   57 * m1 -    5 * m2) *    9942054L >> 32);
          pcmBuffer[i +  3] = (int) ((   -2 * mm +   21 * m0 +    9 * m1 -        m2) *  159072863L >> 32);
          pcmBuffer[i +  5] =                -mm +    9 *      (m0 + m1) -        m2 >>  4;
          pcmBuffer[i +  7] = (int) ((       -mm +    9 * m0 +   21 * m1 -    2 * m2) *  159072863L >> 32);
          pcmBuffer[i +  9] = (int) ((   -5 * mm +   57 * m0 +  405 * m1 -   25 * m2) *    9942054L >> 32);
          pcmBuffer[i + 11] =                                         m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i +  1] = (int) ((  -25 * mm +  405 * m0 +   57 * m1 -    5 * m2) *    9942054L >> 32) * pcmAttackCurve[p    ] >> 14;
          pcmBuffer[i +  3] = (int) ((   -2 * mm +   21 * m0 +    9 * m1 -        m2) *  159072863L >> 32) * pcmAttackCurve[p + 1] >> 14;
          pcmBuffer[i +  5] =       (        -mm +    9 *      (m0 + m1) -        m2 >>  4               ) * pcmAttackCurve[p + 2] >> 14;
          pcmBuffer[i +  7] = (int) ((       -mm +    9 * m0 +   21 * m1 -    2 * m2) *  159072863L >> 32) * pcmAttackCurve[p + 3] >> 14;
          pcmBuffer[i +  9] = (int) ((   -5 * mm +   57 * m0 +  405 * m1 -   25 * m2) *    9942054L >> 32) * pcmAttackCurve[p + 4] >> 14;
          pcmBuffer[i + 11] =       (                                 m1                                 ) * pcmAttackCurve[p + 5] >> 14;
          p += 6;
          pcmPanRight = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i +  1] = (
            pcmBuffer[i +  3] =
            pcmBuffer[i +  5] =
            pcmBuffer[i +  7] =
            pcmBuffer[i +  9] =
            pcmBuffer[i + 11] = 0);
        }
        pcmDecodedData3 = m0;
        pcmDecodedData2 = m1;
        pcmDecodedData1 = m2;
        pcmPointer = i + 6;
      }
    },  //PIP_STEREO_HERMITE_6

    //ステレオ エルミート補間 15625.00Hz
    PIP_STEREO_HERMITE_4 {
      @Override public void write (int m2) {
        //  echo read("hermite.gp");hermite_code(2,4) | gp-2.7 -q
        int i = pcmPointer;
        int mm = pcmDecodedData3;
        int m0 = pcmDecodedData2;
        int m1 = pcmDecodedData1;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          pcmBuffer[i    ] =            -9 * mm +  111 * m0 +   29 * m1 -    3 * m2 >>  7;
          pcmBuffer[i + 2] =                -mm +    9 *      (m0 + m1) -        m2 >>  4;
          pcmBuffer[i + 4] =            -3 * mm +   29 * m0 +  111 * m1 -    9 * m2 >>  7;
          pcmBuffer[i + 6] =                                         m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i    ] = (           -9 * mm +  111 * m0 +   29 * m1 -    3 * m2 >>  7) * pcmAttackCurve[p    ] >> 14;
          pcmBuffer[i + 2] = (               -mm +    9 *      (m0 + m1) -        m2 >>  4) * pcmAttackCurve[p + 1] >> 14;
          pcmBuffer[i + 4] = (           -3 * mm +   29 * m0 +  111 * m1 -    9 * m2 >>  7) * pcmAttackCurve[p + 2] >> 14;
          pcmBuffer[i + 6] = (                                        m1                  ) * pcmAttackCurve[p + 3] >> 14;
          p += 4;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i    ] = (
            pcmBuffer[i + 2] =
            pcmBuffer[i + 4] =
            pcmBuffer[i + 6] = 0);
        }
        p = pcmPanRight;
        if (p > 0) {  //ON
          pcmBuffer[i + 1] =            -9 * mm +  111 * m0 +   29 * m1 -    3 * m2 >>  7;
          pcmBuffer[i + 3] =                -mm +    9 *      (m0 + m1) -        m2 >>  4;
          pcmBuffer[i + 5] =            -3 * mm +   29 * m0 +  111 * m1 -    9 * m2 >>  7;
          pcmBuffer[i + 7] =                                         m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i + 1] = (           -9 * mm +  111 * m0 +   29 * m1 -    3 * m2 >>  7) * pcmAttackCurve[p    ] >> 14;
          pcmBuffer[i + 3] = (               -mm +    9 *      (m0 + m1) -        m2 >>  4) * pcmAttackCurve[p + 1] >> 14;
          pcmBuffer[i + 5] = (           -3 * mm +   29 * m0 +  111 * m1 -    9 * m2 >>  7) * pcmAttackCurve[p + 2] >> 14;
          pcmBuffer[i + 7] = (                                        m1                  ) * pcmAttackCurve[p + 3] >> 14;
          p += 4;
          pcmPanRight = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i + 1] = (
            pcmBuffer[i + 3] =
            pcmBuffer[i + 5] =
            pcmBuffer[i + 7] = 0);
        }
        pcmDecodedData3 = m0;
        pcmDecodedData2 = m1;
        pcmDecodedData1 = m2;
        pcmPointer = i + 8;
      }
    },  //PIP_STEREO_HERMITE_4

    //ステレオ エルミート補間 20833.33Hz
    PIP_STEREO_HERMITE_3 {
      @Override public void write (int m2) {
        //  echo read("hermite.gp");hermite_code(2,3) | gp-2.7 -q
        int i = pcmPointer;
        int mm = pcmDecodedData3;
        int m0 = pcmDecodedData2;
        int m1 = pcmDecodedData1;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          pcmBuffer[i    ] = (int) ((   -2 * mm +   21 * m0 +    9 * m1 -        m2) *  159072863L >> 32);
          pcmBuffer[i + 2] = (int) ((       -mm +    9 * m0 +   21 * m1 -    2 * m2) *  159072863L >> 32);
          pcmBuffer[i + 4] =                                         m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i    ] = (int) ((   -2 * mm +   21 * m0 +    9 * m1 -        m2) *  159072863L >> 32) * pcmAttackCurve[p    ] >> 14;
          pcmBuffer[i + 2] = (int) ((       -mm +    9 * m0 +   21 * m1 -    2 * m2) *  159072863L >> 32) * pcmAttackCurve[p + 1] >> 14;
          pcmBuffer[i + 4] =       (                                 m1                                 ) * pcmAttackCurve[p + 2] >> 14;
          p += 3;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i    ] = (
            pcmBuffer[i + 2] =
            pcmBuffer[i + 4] = 0);
        }
        p = pcmPanRight;
        if (p > 0) {  //ON
          pcmBuffer[i + 1] = (int) ((   -2 * mm +   21 * m0 +    9 * m1 -        m2) *  159072863L >> 32);
          pcmBuffer[i + 3] = (int) ((       -mm +    9 * m0 +   21 * m1 -    2 * m2) *  159072863L >> 32);
          pcmBuffer[i + 5] =                                         m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i + 1] = (int) ((   -2 * mm +   21 * m0 +    9 * m1 -        m2) *  159072863L >> 32) * pcmAttackCurve[p    ] >> 14;
          pcmBuffer[i + 3] = (int) ((       -mm +    9 * m0 +   21 * m1 -    2 * m2) *  159072863L >> 32) * pcmAttackCurve[p + 1] >> 14;
          pcmBuffer[i + 5] =       (                                 m1                                 ) * pcmAttackCurve[p + 2] >> 14;
          p += 3;
          pcmPanRight = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i + 1] = (
            pcmBuffer[i + 3] =
            pcmBuffer[i + 5] = 0);
        }
        pcmDecodedData3 = m0;
        pcmDecodedData2 = m1;
        pcmDecodedData1 = m2;
        pcmPointer = i + 6;
      }
    },  //PIP_STEREO_HERMITE_3

    //ステレオ エルミート補間 31250.00Hz
    PIP_STEREO_HERMITE_2 {
      @Override public void write (int m2) {
        //  echo read("hermite.gp");hermite_code(2,2) | gp-2.7 -q
        int i = pcmPointer;
        int mm = pcmDecodedData3;
        int m0 = pcmDecodedData2;
        int m1 = pcmDecodedData1;
        int p = pcmPanLeft;
        if (p > 0) {  //ON
          pcmBuffer[i    ] =                -mm +    9 *      (m0 + m1) -        m2 >>  4;
          pcmBuffer[i + 2] =                                         m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i    ] = (               -mm +    9 *      (m0 + m1) -        m2 >>  4) * pcmAttackCurve[p    ] >> 14;
          pcmBuffer[i + 2] = (                                        m1                  ) * pcmAttackCurve[p + 1] >> 14;
          p += 2;
          pcmPanLeft = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i    ] = (
            pcmBuffer[i + 2] = 0);
        }
        p = pcmPanRight;
        if (p > 0) {  //ON
          pcmBuffer[i + 1] =                -mm +    9 *      (m0 + m1) -        m2 >>  4;
          pcmBuffer[i + 3] =                                         m1;
        } else if (p < 0) {  //ON→OFFまたはOFF→ON
          p = (char) p;
          pcmBuffer[i + 1] = (               -mm +    9 *      (m0 + m1) -        m2 >>  4) * pcmAttackCurve[p    ] >> 14;
          pcmBuffer[i + 3] = (                                        m1                  ) * pcmAttackCurve[p + 1] >> 14;
          p += 2;
          pcmPanRight = p << -(PCM_ATTACK_RATE + 1) >= 0 ? 0x80000000 + p : p >> PCM_ATTACK_RATE + 1;
        } else {  //OFF
          pcmBuffer[  i + 1] = (
            pcmBuffer[i + 3] = 0);
        }
        pcmDecodedData3 = m0;
        pcmDecodedData2 = m1;
        pcmDecodedData1 = m2;
        pcmPointer = i + 4;
      }
    };  //PIP_STEREO_HERMITE_2

    public abstract void write (int m);

  }  //enum PIP



  public static int pcmReadByte (int a) {
    return (a & 3) == 1 ? (pcmActive ? 0b10000000 : 0) | 0x40 : 0xff;
  }  //pcmReadByte

  public static void pcmWriteByte (int a, int d) {
    a &= 3;
    if (a == 1) {  //コマンド
      if ((d & 0b00000001) != 0) {  //動作終了
        if (pcmActive) {  //動作中
          pcmClock = XEiJ.FAR_FUTURE;
          TickerQueue.tkqRemove (SoundSource.sndPcmTicker);
          pcmActive = false;
          pcmEncodedData = -1;
          pcmDecoderPointer = 0;
          HD63450.dmaRisePCL (3);
          HD63450.dmaRiseREQ (3);
        }
      } else if ((d & 0b00000010) != 0) {  //動作開始
        if (!pcmActive) {  //停止中
          //現在のブロックの残り時間が1サンプルの時間の倍数になるように切り上げる
          int remainingSamples = Math.max (0, (int) ((double) (SoundSource.sndBlockClock - XEiJ.mpuClockTime) / (double) PCM_SAMPLE_TIME));  //現在のブロックの残りサンプル数
          pcmClock = SoundSource.sndBlockClock - PCM_SAMPLE_TIME * (long) remainingSamples;  //書き込み開始時刻
          TickerQueue.tkqAdd (SoundSource.sndPcmTicker, pcmClock);
          pcmActive = true;
          int newPointer = SoundSource.SND_CHANNELS * (PCM_BLOCK_SAMPLES - remainingSamples);  //書き込み開始位置
          if (pcmPointer < newPointer) {
            pcmFillBuffer (newPointer);
          } else {
            pcmPointer = newPointer;  //少し戻る場合がある
          }
          //DMAに最初のデータを要求する
          HD63450.dmaFallPCL (3);
          HD63450.dmaFallREQ (3);
        }
        //} else if ((d & 0b00000100) != 0) {  //録音開始
        //! 非対応
      }
    } else if (a == 3) {  //データ
      if (pcmActive) {
        pcmEncodedData = d & 0xff;
        HD63450.dmaRisePCL (3);
        HD63450.dmaRiseREQ (3);
      }
    }
  }  //pcmWriteByte



}  //class ADPCM



