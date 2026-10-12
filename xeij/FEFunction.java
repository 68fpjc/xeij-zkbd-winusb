//========================================================================================
//  FEFunction.java
//    en:FE function
//    ja:FEファンクション
//  Copyright (C) 2003-2024 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//  FEファンクションコール(オペコード0xfe00～0xfeff)をMPUの命令として処理することで高速化する
//  line 1111 emulator/privilege violation例外が発生しないのでFLOATn.Xが組み込まれていてもFLOATn.Xのルーチンは使用されない
//----------------------------------------------------------------------------------------

package xeij;

import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System

public class FEFunction {

  public static final boolean FPK_DEBUG_TRACE = false;

  public static final int FPK_CLOCK = 80;  //一律に80サイクルかかることにする

  public static final boolean FPK_FPCP_NAN = true;  //true=JavaのNaN(0x7ff8000000000000L,0x7fc00000)をFPCPのNaN(0x7fffffffffffffffL,0x7fffffff)に変換する

  public static boolean fpkOn;  //true=FEファンクションコールを命令として処理する

  //  Human68kのline 1111 emulator/privilege violation例外処理ルーチンの手前にFLOATn.Xのマジック'FEfn'を押し込む
  //  CONFIG.SYSにFLOATn.Xが書いてあっても「すでに浮動小数点演算パッケージは登録されています」と表示されて組み込まれなくなる
  //  FLOATn.Xを組み込んでもFEファンクションが命令として処理されることに変わりはないが、
  //  FLOATn.Xを組み込まなければFライン例外がFLOATn.Xを通らなくなるのでDOSコールのオーバーヘッドが少なくなる
  public static boolean fpkRejectFloatOn;  //true=FLOATn.Xを組み込まない

  public static void fpkInit () {
    fpkOn = Settings.sgsGetOnOff ("fefunc");  //FEファンクション命令
    fpkRejectFloatOn = Settings.sgsGetOnOff ("rejectfloat");  //FLOATn.Xを組み込まない
  }  //fpkInit()



  public static final int[] fpkRndTable = new int[55];
  public static int fpkRndPointer = -1;

  //fpkRndInit ()
  //  rnd()乱数列の初期化
  public static void fpkRndInit (int t) {
    for (int i = 0; i < 55; i++) {
      t = (char) t * 15625 + 1;  //15625=5^6
      fpkRndTable[i] = t << 16 | (char) t;
    }
    fpkRndPointer = 54;
    fpkRndShuffle ();
    fpkRndShuffle ();
    fpkRndShuffle ();
  }  //fpkRndInit()

  //fpkRndShuffle ()
  //  rnd()乱数列の更新
  public static void fpkRndShuffle () {
    for (int i = 0; i < 24; i++) {
      fpkRndTable[i] -= fpkRndTable[i + 31];
    }
    for (int i = 24; i < 55; i++) {
      fpkRndTable[i] -= fpkRndTable[i - 24];
    }
  }  //fpkRndShuffle()

  //rnd = fpkRndLong ()
  //  rnd()乱数列の取り出し
  public static int fpkRndLong () {
    int i = fpkRndPointer;
    if (i < 0) {
      fpkRndInit (111);
      i = fpkRndPointer;
    }
    if (i == 54) {
      fpkRndShuffle ();
      i = 0;
    } else {
      i++;
    }
    fpkRndPointer = i;
    return fpkRndTable[i];
  }  //fpkRndLong()



  public static final short[] fpkRandTable = new short[55];
  public static int fpkRandPointer = -1;

  //fpkRandInit (t)
  //  rand()乱数列の初期化
  public static void fpkRandInit (int t) {
    for (int i = 0; i < 55; i++) {
      t = (char) t * 15625 + 1;
      fpkRandTable[i] = (short) t;
    }
    fpkRandPointer = 54;
    fpkRandShuffle ();
  }  //fpkRandInit(int)

  //fpkRandShuffle ()
  //  rand()乱数列の更新
  public static void fpkRandShuffle () {
    for (int i = 0; i < 24; i++) {
      fpkRandTable[i] -= fpkRandTable[i + 31];
    }
    for (int i = 24; i < 55; i++) {
      fpkRandTable[i] -= fpkRandTable[i - 24];
    }
  }  //fpkRandShuffle()

  //rand = fpkRandShort ()
  //  rand()乱数列の取り出し
  public static short fpkRandShort () {
    int i = fpkRandPointer;
    if (i < 0) {
      fpkRandInit (51);
      i = fpkRandPointer;
    }
    if (i == 54) {
      fpkRandShuffle ();
      i = 0;
    } else {
      i++;
    }
    fpkRandPointer = i;
    return fpkRandTable[i];
  }  //fpkRandShort()

  //10^(±16^q*r)のテーブル
  //  10進数の指数部を4bitずつ分割して10^(±16^q*r)のテーブルを参照する
  //  4bitずつ分割するのは誤差を減らすため
  //  3bitずつ分割しても3分割に変わりないが非正規化数の最小値4.94e-324を処理するときに1.0e+320が必要になるので不可
  //  一般的には10進数の指数部を1bitずつ分割して10^(±2^q)のテーブルを参照することが多い
  //  誤差を小さくしたければ分割せずに直接10^(±r)のテーブルを参照する方法もある。doubleならば高々600個余りである
  public static final double[] FPK_TEN_P16QR = {
    //  perl -e "for$i(0..33){$q=$i>>4;$r=$i&15;printf'    1.0e+%d,  //10^(16^%d*%d)%c',16**$q*$r,$q,$r,10}"
    1.0e+0,  //10^(16^0*0)
    1.0e+1,  //10^(16^0*1)
    1.0e+2,  //10^(16^0*2)
    1.0e+3,  //10^(16^0*3)
    1.0e+4,  //10^(16^0*4)
    1.0e+5,  //10^(16^0*5)
    1.0e+6,  //10^(16^0*6)
    1.0e+7,  //10^(16^0*7)
    1.0e+8,  //10^(16^0*8)
    1.0e+9,  //10^(16^0*9)
    1.0e+10,  //10^(16^0*10)
    1.0e+11,  //10^(16^0*11)
    1.0e+12,  //10^(16^0*12)
    1.0e+13,  //10^(16^0*13)
    1.0e+14,  //10^(16^0*14)
    1.0e+15,  //10^(16^0*15)
    1.0e+0,  //10^(16^1*0)
    1.0e+16,  //10^(16^1*1)
    1.0e+32,  //10^(16^1*2)
    1.0e+48,  //10^(16^1*3)
    1.0e+64,  //10^(16^1*4)
    1.0e+80,  //10^(16^1*5)
    1.0e+96,  //10^(16^1*6)
    1.0e+112,  //10^(16^1*7)
    1.0e+128,  //10^(16^1*8)
    1.0e+144,  //10^(16^1*9)
    1.0e+160,  //10^(16^1*10)
    1.0e+176,  //10^(16^1*11)
    1.0e+192,  //10^(16^1*12)
    1.0e+208,  //10^(16^1*13)
    1.0e+224,  //10^(16^1*14)
    1.0e+240,  //10^(16^1*15)
    1.0e+0,  //10^(16^2*0)
    1.0e+256,  //10^(16^2*1)
  };
  public static final double[] FPK_TEN_M16QR = {
    //  perl -e "for$i(0..33){$q=$i>>4;$r=$i&15;printf'    1.0e-%d,  //10^(-16^%d*%d)%c',16**$q*$r,$q,$r,10}"
    1.0e-0,  //10^(-16^0*0)
    1.0e-1,  //10^(-16^0*1)
    1.0e-2,  //10^(-16^0*2)
    1.0e-3,  //10^(-16^0*3)
    1.0e-4,  //10^(-16^0*4)
    1.0e-5,  //10^(-16^0*5)
    1.0e-6,  //10^(-16^0*6)
    1.0e-7,  //10^(-16^0*7)
    1.0e-8,  //10^(-16^0*8)
    1.0e-9,  //10^(-16^0*9)
    1.0e-10,  //10^(-16^0*10)
    1.0e-11,  //10^(-16^0*11)
    1.0e-12,  //10^(-16^0*12)
    1.0e-13,  //10^(-16^0*13)
    1.0e-14,  //10^(-16^0*14)
    1.0e-15,  //10^(-16^0*15)
    1.0e-0,  //10^(-16^1*0)
    1.0e-16,  //10^(-16^1*1)
    1.0e-32,  //10^(-16^1*2)
    1.0e-48,  //10^(-16^1*3)
    1.0e-64,  //10^(-16^1*4)
    1.0e-80,  //10^(-16^1*5)
    1.0e-96,  //10^(-16^1*6)
    1.0e-112,  //10^(-16^1*7)
    1.0e-128,  //10^(-16^1*8)
    1.0e-144,  //10^(-16^1*9)
    1.0e-160,  //10^(-16^1*10)
    1.0e-176,  //10^(-16^1*11)
    1.0e-192,  //10^(-16^1*12)
    1.0e-208,  //10^(-16^1*13)
    1.0e-224,  //10^(-16^1*14)
    1.0e-240,  //10^(-16^1*15)
    1.0e-0,  //10^(-16^2*0)
    1.0e-256,  //10^(-16^2*1)
  };



  //fpkLMUL ()
  //  $FE00  __LMUL
  //  32bit符号あり整数乗算
  //  <d0.l:32bit符号あり整数。被乗数x
  //  <d1.l:32bit符号あり整数。乗数y
  //  >d0.l:32bit符号あり整数。積x*y。オーバーフローのとき不定
  //  >ccr:cs=エラーあり。cs,ne,vs=オーバーフロー
  //  メモ
  //    ZフラグとVフラグの変化はCZ-6BP1のマニュアルに書かれていないがX-BASICが使っている
  //    FLOAT2.X 2.02/2.03とFLOAT3.X 2.03はオーバーフローのときZフラグをクリア、Vフラグをセットする
  //    FLOAT4.X 1.02はオーバーフローのときZフラグとVフラグをクリアする
  //      X-BASICで「print 99999*99999」が「オーバーフローしました」ではなく「アンダーフローしました」になる
  public static void fpkLMUL () {
    long xl = (long) XEiJ.regRn[0];
    long yl = (long) XEiJ.regRn[1];
    long zl = xl * yl;
    int zi = (int) zl;
    if ((long) zi != zl) {  //オーバーフロー
      XEiJ.regRn[0] = zi;  //d0.lは不定。ここでは積の下位32bit
      XEiJ.regCCR = XEiJ.REG_CCR_V | XEiJ.REG_CCR_C;
    } else {
      XEiJ.regRn[0] = zi;
      XEiJ.regCCR = 0;
    }
  }  //fpkLMUL()

  //fpkLDIV ()
  //  $FE01  __LDIV
  //  32bit符号あり整数除算
  //  <d0.l:32bit符号あり整数。被除数x
  //  <d1.l:32bit符号あり整数。除数y
  //  >d0.l:32bit符号あり整数。商x/y。ゼロ除算のとき不定。オーバーフローのとき$7FFFFFFF
  //  >ccr:cs=エラーあり。cs,eq,vc=ゼロ除算(0/0を含む)。cs,ne,vs=オーバーフロー(-$80000000/-$00000001=+$80000000)
  //  バグ
  //    FLOAT2.X 2.02/2.03とFLOAT3.X 2.03は0/0のときCフラグをセットしない
  //      X-BASICで「print 0/0」が「０による除算が行われました」ではなく「0」になる
  //    FLOAT4.X 1.02はオーバーフローのときCフラグをセットしない
  //      X-BASICで「print &H80000000/-1」が「オーバーフローしました」ではなく「-2147483648」になる
  //  メモ
  //    ZフラグとVフラグの変化はCZ-6BP1のマニュアルに書かれていないがX-BASICが使っている
  //    FLOAT2.X 2.02/2.03とFLOAT3.X 2.03はゼロ除算(0/0を含まない)のときZフラグをセット、Vフラグをクリアする
  //    FLOAT2.X 2.02/2.03とFLOAT3.X 2.03はオーバーフローのときZフラグをクリア、Vフラグをセットする
  //    FLOAT4.X 1.02はゼロ除算(0/0を含む)のときZフラグとVフラグをクリアする
  //      X-BASICで「print 1/0」や「print 0/0」が「０による除算が行われました」ではなく「アンダーフローしました」になる
  public static void fpkLDIV () {
    long xl = (long) XEiJ.regRn[0];
    long yl = (long) XEiJ.regRn[1];
    if (yl == 0L) {  //ゼロ除算
      //d0.lは不定。ここでは変化しない
      XEiJ.regCCR = XEiJ.REG_CCR_Z | XEiJ.REG_CCR_C;
    } else {
      long zl = xl / yl;
      int zi = (int) zl;
      if ((long) zi != zl) {  //オーバーフロー
        XEiJ.regRn[0] = 0x7fffffff;  //d0.lは$7FFFFFFF
        XEiJ.regCCR = XEiJ.REG_CCR_V | XEiJ.REG_CCR_C;
      } else {
        XEiJ.regRn[0] = zi;
        XEiJ.regCCR = 0;
      }
    }
  }  //fpkLDIV()

  //fpkLMOD ()
  //  $FE02  __LMOD
  //  32bit符号あり整数剰余算
  //  <d0.l:32bit符号あり整数。被除数x
  //  <d1.l:32bit符号あり整数。除数y
  //  >d0.l:32bit符号あり整数。余りx%y。余りの符号は被除数の符号と同じ。ゼロ除算のとき不定
  //  >ccr:cs=エラーあり。cs,eq,vc=ゼロ除算(0%0を含む)。オーバーフローすることはない
  //  バグ
  //    FLOAT2.X 2.02/2.03とFLOAT3.X 2.03は0%0のときCフラグをセットしない
  //      X-BASICで「print 0 mod 0」が「０による除算が行われました」ではなく「0」になる
  //    FLOAT4.X 1.02は-$80000000%-$00000001が-$00000001になる
  //      X-BASICで「print &H80000000 mod -1」が「0」ではなく「-1」になる
  //  メモ
  //    ZフラグとVフラグの変化はCZ-6BP1のマニュアルに書かれていないがX-BASICが使っている
  //    FLOAT2.X 2.02/2.03とFLOAT3.X 2.03はゼロ除算(0%0を含まない)のときZフラグをセット、Vフラグをクリアする
  //    FLOAT4.X 1.02はゼロ除算(0%0を含む)のときZフラグとVフラグをクリアする
  //      X-BASICで「print 1 mod 0」や「print 0 mod 0」が「０による除算が行われました」ではなく「アンダーフローしました」になる
  public static void fpkLMOD () {
    long xl = (long) XEiJ.regRn[0];
    long yl = (long) XEiJ.regRn[1];
    if (yl == 0L) {  //ゼロ除算
      //d0.lは不定。ここでは変化しない
      XEiJ.regCCR = XEiJ.REG_CCR_Z | XEiJ.REG_CCR_C;
    } else {
      long zl = xl % yl;
      int zi = (int) zl;
      XEiJ.regRn[0] = zi;
      XEiJ.regCCR = 0;
    }
  }  //fpkLMOD()

  //fpkUMUL ()
  //  $FE04  __UMUL
  //  32bit符号なし整数乗算
  //  <d0.l:32bit符号なし整数。被乗数x
  //  <d1.l:32bit符号なし整数。乗数y
  //  >d0.l:32bit符号なし整数。積x*y。オーバーフローのとき不定
  //  >ccr:cs=エラーあり。cs,ne,vs=オーバーフロー
  //  メモ
  //    ZフラグとVフラグの変化はCZ-6BP1のマニュアルに書かれていない
  //    FLOAT2.X 2.02/2.03とFLOAT3.X 2.03はオーバーフローのときZフラグをクリア、Vフラグをセットする
  //    FLOAT4.X 1.02はオーバーフローのときZフラグとVフラグをクリアする
  public static void fpkUMUL () {
    long xl = 0xffffffffL & (long) XEiJ.regRn[0];
    long yl = 0xffffffffL & (long) XEiJ.regRn[1];
    long zl = xl * yl;
    int zi = (int) zl;
    if ((0xffffffffL & (long) zi) != zl) {  //オーバーフロー
      XEiJ.regRn[0] = zi;  //d0.lは不定。ここでは積の下位32bit
      XEiJ.regCCR = XEiJ.REG_CCR_V | XEiJ.REG_CCR_C;
    } else {
      XEiJ.regRn[0] = zi;
      XEiJ.regCCR = 0;
    }
  }  //fpkUMUL()

  //fpkUDIV ()
  //  $FE05  __UDIV
  //  32bit符号なし整数除算
  //  <d0.l:32bit符号なし整数。被除数x
  //  <d1.l:32bit符号なし整数。除数y
  //  >d0.l:32bit符号なし整数。商x/y。ゼロ除算のとき不定
  //  >ccr:cs=エラーあり。cs,eq,vc=ゼロ除算(0/0を含む)。オーバーフローすることはない
  //  バグ
  //    FLOAT2.X 2.02/2.03とFLOAT3.X 2.03は0/0のときCフラグをセットしない
  //  メモ
  //    ZフラグとVフラグの変化はCZ-6BP1のマニュアルに書かれていない
  //    FLOAT2.X 2.02/2.03とFLOAT3.X 2.03はゼロ除算(0/0を含まない)のときZフラグをセット、Vフラグをクリアする
  //    FLOAT4.X 1.02はゼロ除算(0/0を含む)のときZフラグとVフラグをクリアする
  public static void fpkUDIV () {
    long xl = 0xffffffffL & (long) XEiJ.regRn[0];
    long yl = 0xffffffffL & (long) XEiJ.regRn[1];
    if (yl == 0L) {  //ゼロ除算
      //d0.lは不定。ここでは変化しない
      XEiJ.regCCR = XEiJ.REG_CCR_Z | XEiJ.REG_CCR_C;
    } else {
      long zl = xl / yl;
      int zi = (int) zl;
      XEiJ.regRn[0] = zi;
      XEiJ.regCCR = 0;
    }
  }  //fpkUDIV()

  //fpkUMOD ()
  //  $FE06  __UMOD
  //  32bit符号なし整数剰余算
  //  <d0.l:32bit符号なし整数。被除数x
  //  <d1.l:32bit符号なし整数。除数y
  //  >d0.l:32bit符号なし整数。余りx%y。ゼロ除算のとき不定
  //  >ccr:cs=エラーあり。cs,eq,vc=ゼロ除算(0%0を含む)。オーバーフローすることはない
  //  バグ
  //    FLOAT2.X 2.02/2.03とFLOAT3.X 2.03は0%0のときCフラグをセットしない
  //  メモ
  //    ZフラグとVフラグの変化はCZ-6BP1のマニュアルに書かれていない
  //    FLOAT2.X 2.02/2.03とFLOAT3.X 2.03はゼロ除算(0%0を含まない)のときZフラグをセット、Vフラグをクリアする
  //    FLOAT4.X 1.02はゼロ除算(0%0を含む)のときZフラグとVフラグをクリアする
  public static void fpkUMOD () {
    long xl = 0xffffffffL & (long) XEiJ.regRn[0];
    long yl = 0xffffffffL & (long) XEiJ.regRn[1];
    if (yl == 0L) {  //ゼロ除算
      //d0.lは不定。ここでは変化しない
      XEiJ.regCCR = XEiJ.REG_CCR_Z | XEiJ.REG_CCR_C;
    } else {
      long zl = xl % yl;
      int zi = (int) zl;
      XEiJ.regRn[0] = zi;
      XEiJ.regCCR = 0;
    }
  }  //fpkUMOD()

  //fpkIMUL ()
  //  $FE08  __IMUL
  //  32bit符号なし整数乗算
  //  <d0.l:32bit符号なし整数。被乗数x
  //  <d1.l:32bit符号なし整数。乗数y
  //  >d0d1.q:64bit符号なし整数。積x*y
  public static void fpkIMUL () {
    long xl = 0xffffffffL & (long) XEiJ.regRn[0];
    long yl = 0xffffffffL & (long) XEiJ.regRn[1];
    long zl = xl * yl;
    XEiJ.regRn[0] = (int) (zl >>> 32);
    XEiJ.regRn[1] = (int) zl;
  }  //fpkIMUL()

  //fpkIDIV ()
  //  $FE09  __IDIV
  //  32bit符号なし整数除算・剰余算
  //  <d0.l:32bit符号なし整数。被除数x
  //  <d1.l:32bit符号なし整数。除数y
  //  >d0.l:32bit符号なし整数。商x/y。ゼロ除算のとき不定
  //  >d1.l:32bit符号なし整数。余りx%y。ゼロ除算のとき不定
  //  >ccr:cs=エラーあり。cs=ゼロ除算(0/0を含む)。オーバーフローすることはない
  public static void fpkIDIV () {
    long xl = 0xffffffffL & (long) XEiJ.regRn[0];
    long yl = 0xffffffffL & (long) XEiJ.regRn[1];
    if (yl == 0L) {  //ゼロ除算
      //d0.l,d1.lは不定。ここでは変化しない
      XEiJ.regCCR = XEiJ.REG_CCR_C;
    } else {
      long zl = xl / yl;
      XEiJ.regRn[0] = (int) zl;
      XEiJ.regRn[1] = (int) (xl - yl * zl);
      XEiJ.regCCR = 0;
    }
  }  //fpkIDIV()

  //fpkRANDOMIZE ()
  //  $FE0C  __RANDOMIZE
  //  rnd()乱数列の初期化
  //  <d0.l:乱数の種x(0～65535)
  //  >d0.l:0
  public static void fpkRANDOMIZE () {
    fpkRndInit (XEiJ.regRn[0]);
    XEiJ.regRn[0] = 0;
  }  //fpkRANDOMIZE()

  //fpkSRAND ()
  //  $FE0D  __SRAND
  //  rand()乱数列の初期化
  //  <d0.l:乱数の種x(0～65535)
  //  >d0.l:0
  public static void fpkSRAND () {
    fpkRandInit (XEiJ.regRn[0]);
    XEiJ.regRn[0] = 0;
  }  //fpkSRAND()

  //fpkRAND ()
  //  $FE0E  __RAND
  //  整数乱数
  //  >d0.l:乱数rand()(0～32767)
  public static void fpkRAND () {
    XEiJ.regRn[0] = fpkRandShort () & 0x7fff;
  }  //fpkRAND()

  //fpkLTOD ()
  //  $FE1A  __LTOD
  //  32bit符号あり整数を64bit浮動小数点数に変換する
  //  <d0.l:32bit符号あり整数。x
  //  >d0d1.d:64bit浮動小数点数。(double)x
  public static void fpkLTOD () {
    //int→double→[long]→[int,int]
    long l = Double.doubleToLongBits ((double) XEiJ.regRn[0]);
    XEiJ.regRn[0] = (int) (l >>> 32);
    XEiJ.regRn[1] = (int) l;
  }  //fpkLTOD()

  //fpkDTOL ()
  //  $FE1B  __DTOL
  //  64bit浮動小数点数を32bit符号あり整数に変換する
  //  <d0d1.d:64bit浮動小数点数。x
  //  >d0.l:32bit符号あり整数。(int)x。オーバーフローのときは不定
  //  >ccr:cs=オーバーフロー。C以外は不定
  public static void fpkDTOL () {
    //[int,int]→[long]→double→int
    double d = Double.longBitsToDouble ((long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1]);
    XEiJ.regRn[0] = (int) d;  //オーバーフローのときは最小値または最大値
    XEiJ.regCCR = (double) Integer.MIN_VALUE - 1.0 < d && d < (double) Integer.MAX_VALUE + 1.0 ? 0 : XEiJ.REG_CCR_C;  //NaN,±Infはエラー
  }  //fpkDTOL()

  //fpkLTOF ()
  //  $FE1C  __LTOF
  //  32bit符号あり整数を32bit浮動小数点数に変換する
  //  <d0.l:32bit符号あり整数。x
  //  >d0.s:32bit浮動小数点数。(float)x
  public static void fpkLTOF () {
    //int→float→[int]
    XEiJ.regRn[0] = Float.floatToIntBits ((float) XEiJ.regRn[0]);
  }  //fpkLTOF()

  //fpkFTOL ()
  //  $FE1D  __FTOL
  //  32bit浮動小数点数を32bit符号あり整数に変換する
  //  <d0.s:32bit浮動小数点数。x
  //  >d0.l:32bit符号あり整数。(int)x。オーバーフローのときは不定
  //  >ccr:cs=オーバーフロー。C以外は不定
  public static void fpkFTOL () {
    //[int]→float→int
    float f = Float.intBitsToFloat (XEiJ.regRn[0]);
    XEiJ.regRn[0] = (int) f;
    XEiJ.regCCR = (float) Integer.MIN_VALUE - 1.0F < f && f < (float) Integer.MAX_VALUE + 1.0F ? 0 : XEiJ.REG_CCR_C;  //NaN,±Infはエラー
  }  //fpkFTOL()

  //fpkFTOD ()
  //  $FE1E  __FTOD
  //  32bit浮動小数点数を64bit浮動小数点数に変換する
  //  バグ
  //    FLOAT2.X 2.02/2.03はfloatの非正規化数を正しく処理できない
  //      (double)0x007fffff=0x380fffffc0000000Lが0x380fffffe0000000Lになる
  //      (double)0x00000001=0x36a0000000000000Lが0x3800000020000000Lになる
  //  <d0.s:32bit浮動小数点数。x
  //  >d0d1.d:64bit浮動小数点数。(double)x
  public static void fpkFTOD () {
    //[int]→float→double→[long]→[int,int]
    long l = Double.doubleToLongBits ((double) Float.intBitsToFloat (XEiJ.regRn[0]));
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.regRn[0] = (int) (l >>> 32);
    XEiJ.regRn[1] = (int) l;
  }  //fpkFTOD()

  //fpkDTOF ()
  //  $FE1F  __DTOF
  //  64bit浮動小数点数を32bit浮動小数点数に変換する
  //  バグ
  //    FLOAT2.X 2.02/2.03はfloatの非正規化数を正しく処理できない
  //      (float)0x380fffffc0000000L=0x007fffffが0x007ffffeになる
  //      (float)0x36a0000000000000L=0x00000001が0x00000000になる
  //    FLOAT2.X 2.02/2.03は(float)0x8010000000000000L=0x80000000が0x00000000になる
  //  <d0d1.d:64bit浮動小数点数。x
  //  >d0.s:32bit浮動小数点数。(float)x
  //  >ccr:cs=オーバーフロー。C以外は不定
  public static void fpkDTOF () {
    //[int,int]→[long]→double→float→[int]
    double d = Double.longBitsToDouble ((long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1]);
    int h = Float.floatToIntBits ((float) d);
    if (FPK_FPCP_NAN && h == 0x7fc00000) {
      h = 0x7fffffff;
    }
    XEiJ.regRn[0] = h;
    XEiJ.regCCR = (Double.isNaN (d) || Double.isInfinite (d) ||
           Math.abs (d) < (double) Float.MAX_VALUE + 0.5 * (double) Math.ulp (Float.MAX_VALUE) ? 0 : XEiJ.REG_CCR_C);  //アンダーフローはエラーなし
  }  //fpkDTOF()

  //fpkDTST ()
  //  $FE28  __DTST
  //  64bit浮動小数点数と0の比較
  //  x<=>0
  //  バグ
  //    FLOAT2.X 2.02/2.03は-0<0となる
  //    FLOAT4.X 1.02は-0<0となる(実機で確認済み)
  //  <d0d1.d:64bit浮動小数点数。x
  //  >ccr:lt=x<0,eq=x==0,gt=x>0
  public static void fpkDTST () {
    if (true) {
      long l = (long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1];
      XEiJ.regCCR = l << 1 == 0L ? XEiJ.REG_CCR_Z : 0L <= l ? 0 : XEiJ.REG_CCR_N;  //NaNのときは0
    } else {
      //([int,int]→[long]→double)<=>0
      double d = Double.longBitsToDouble ((long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1]);
      XEiJ.regCCR = d < 0.0 ? XEiJ.REG_CCR_N : d == 0.0 ? XEiJ.REG_CCR_Z : 0;  //NaNのときは0
    }
  }  //fpkDTST()

  //fpkDCMP ()
  //  $FE29  __DCMP
  //  64bit浮動小数点数の比較
  //  x<=>y
  //  バグ
  //    FLOAT2.X 2.02/2.03はNaNがNaNと等しく、NaN以外のすべての数よりも大きい
  //    FLOAT4.X 1.02はNaNがNaNと等しく、NaN以外のすべての数よりも大きい
  //    FLOAT2.X 2.02/2.03は-0が0よりも小さい
  //    FLOAT4.X 1.02は-0が0よりも小さい
  //  <d0d1.d:64bit浮動小数点数。x
  //  <d2d3.d:64bit浮動小数点数。y
  //  >ccr:lt=x<y,eq=x==y,gt=x>y
  public static void fpkDCMP () {
    //([int,int]→[long]→double)<=>([int,int]→[long]→double)
    double xd = Double.longBitsToDouble ((long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1]);
    double yd = Double.longBitsToDouble ((long) XEiJ.regRn[2] << 32 | 0xffffffffL & XEiJ.regRn[3]);
    XEiJ.regCCR = xd < yd ? XEiJ.REG_CCR_N | XEiJ.REG_CCR_C : xd == yd ? XEiJ.REG_CCR_Z : 0;  //どちらかがNaNのときは0
  }  //fpkDCMP()

  //fpkDNEG ()
  //  $FE2A  __DNEG
  //  64bit浮動小数点数の符号反転
  //  バグ
  //    FLOAT2.X 2.02/2.03は-(+0)が+0になる
  //    FLOAT4.X 1.02は-(+0)が+0になる
  //    FLOAT2.X 2.02/2.03は-NaNが0xffffffffffffffffLになる
  //    FLOAT4.X 1.02は-NaNが0xffffffffffffffffLになる
  //    FLOAT2.X 2.02/2.03は-0x0000000000000001Lが0x0000000000000001Lになる
  //    FLOAT4.X 1.02は-0x0000000000000001Lが0x0000000000000001Lになる
  //  <d0d1.d:64bit浮動小数点数。x
  //  >d0d1.d:64bit浮動小数点数。-x
  public static void fpkDNEG () {
    //-([int,int]→[long]→double)→[long]→[int,int]
    long l = Double.doubleToLongBits (-Double.longBitsToDouble ((long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1]));
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.regRn[0] = (int) (l >>> 32);
    XEiJ.regRn[1] = (int) l;
  }  //fpkDNEG()

  //fpkDADD ()
  //  $FE2B  __DADD
  //  64bit浮動小数点数の加算
  //  バグ
  //    FLOAT2.X 2.02/2.03はNaNを+Infとして計算する。(+Inf)+(-Inf)はNaNになる
  //    FLOAT2.X 2.02/2.03は丸めの処理がおかしい
  //      0x3ff0000000000004+0x4024000000000000=0x4026000000000000が0x4026000000000001になる
  //      0xbff921fb54442d18+0x40c3880000000000=0x40c38736f0255ddfが0x40c38736f0255ddeになる
  //  <d0d1.d:64bit浮動小数点数。被加算数x
  //  <d2d3.d:64bit浮動小数点数。加算数y
  //  >d0d1.d:64bit浮動小数点数。和x+y
  //  >ccr:cs=エラー,vs=オーバーフロー
  public static void fpkDADD () {
    //([int,int]→[long]→double)+([int,int]→[long]→double)→[long]→[int,int]
    double xd = Double.longBitsToDouble ((long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1]);
    double yd = Double.longBitsToDouble ((long) XEiJ.regRn[2] << 32 | 0xffffffffL & XEiJ.regRn[3]);
    double zd = xd + yd;
    long l = Double.doubleToLongBits (zd);
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.regRn[0] = (int) (l >>> 32);
    XEiJ.regRn[1] = (int) l;
    XEiJ.regCCR = (Double.isNaN (xd) || Double.isNaN (yd) ? 0 :  //引数がNaN
           Double.isNaN (zd) ? XEiJ.REG_CCR_C :  //引数がNaNでないのに結果がNaNのときはエラー。(+Inf)+(-Inf)=NaN
           Double.isInfinite (xd) || Double.isInfinite (yd) ? 0 :  //引数が±Inf
           Double.isInfinite (zd) ? XEiJ.REG_CCR_V | XEiJ.REG_CCR_C :  //引数が±Infでないのに結果が±Infのときはオーバーフロー
           0);
  }  //fpkDADD()

  //fpkDSUB ()
  //  $FE2C  __DSUB
  //  64bit浮動小数点数の減算
  //  バグ
  //    FLOAT2.X 2.02/2.03は丸めの処理がおかしい
  //  <d0d1.d:64bit浮動小数点数。被減算数x
  //  <d2d3.d:64bit浮動小数点数。減算数y
  //  >d0d1.d:64bit浮動小数点数。差x-y
  //  >ccr:cs=エラー,vs=オーバーフロー
  public static void fpkDSUB () {
    //([int,int]→[long]→double)-([int,int]→[long]→double)→[long]→[int,int]
    double xd = Double.longBitsToDouble ((long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1]);
    double yd = Double.longBitsToDouble ((long) XEiJ.regRn[2] << 32 | 0xffffffffL & XEiJ.regRn[3]);
    double zd = xd - yd;
    long l = Double.doubleToLongBits (zd);
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.regRn[0] = (int) (l >>> 32);
    XEiJ.regRn[1] = (int) l;
    XEiJ.regCCR = (Double.isNaN (xd) || Double.isNaN (yd) ? 0 :  //引数がNaN
           Double.isNaN (zd) ? XEiJ.REG_CCR_C :  //引数がNaNでないのに結果がNaNのときはエラー。(+Inf)-(+Inf)=NaN
           Double.isInfinite (xd) || Double.isInfinite (yd) ? 0 :  //引数が±Inf
           Double.isInfinite (zd) ? XEiJ.REG_CCR_V | XEiJ.REG_CCR_C :  //引数が±Infでないのに結果が±Infのときはオーバーフロー
           0);
  }  //fpkDSUB()

  //fpkDMUL ()
  //  $FE2D  __DMUL
  //  64bit浮動小数点数の乗算
  //  バグ
  //    FLOAT2.X 2.02/2.03は(+Inf)*(非正規化数)=(+Inf)がNaNになる
  //    FLOAT2.X 2.02/2.03は(+x)*(非正規化数)=(+x)が+0になる
  //    FLOAT2.X 2.02/2.03は(+x)*(-0)=(-0)が+0になる
  //  <d0d1.d:64bit浮動小数点数。被乗数x
  //  <d2d3.d:64bit浮動小数点数。乗数y
  //  >d0d1.d:64bit浮動小数点数。積x*y
  //  >ccr:cs=エラー,vs=オーバーフロー
  public static void fpkDMUL () {
    //([int,int]→[long]→double)*([int,int]→[long]→double)→[long]→[int,int]
    double xd = Double.longBitsToDouble ((long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1]);
    double yd = Double.longBitsToDouble ((long) XEiJ.regRn[2] << 32 | 0xffffffffL & XEiJ.regRn[3]);
    double zd = xd * yd;
    long l = Double.doubleToLongBits (zd);
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.regRn[0] = (int) (l >>> 32);
    XEiJ.regRn[1] = (int) l;
    XEiJ.regCCR = (Double.isNaN (xd) || Double.isNaN (yd) ? 0 :  //引数がNaN
           Double.isNaN (zd) ? XEiJ.REG_CCR_C :  //引数がNaNでないのに結果がNaNのときはエラー。(±0)*(±Inf)=NaN
           Double.isInfinite (xd) || Double.isInfinite (yd) ? 0 :  //引数が±Inf
           Double.isInfinite (zd) ? XEiJ.REG_CCR_V | XEiJ.REG_CCR_C :  //引数が±Infでないのに結果が±Infのときはオーバーフロー
           0);
  }  //fpkDMUL()

  //fpkDDIV ()
  //  $FE2E  __DDIV
  //  64bit浮動小数点数の除算
  //  <d0d1.d:64bit浮動小数点数。被除数x
  //  <d2d3.d:64bit浮動小数点数。除数y
  //  >d0d1.d:64bit浮動小数点数。商x/y。ゼロ除算のときは不定
  //  >ccr:cs=エラー,eq=ゼロ除算,vs=オーバーフロー
  public static void fpkDDIV () {
    //([int,int]→[long]→double)/([int,int]→[long]→double)→[long]→[int,int]
    double xd = Double.longBitsToDouble ((long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1]);
    double yd = Double.longBitsToDouble ((long) XEiJ.regRn[2] << 32 | 0xffffffffL & XEiJ.regRn[3]);
    double zd = xd / yd;
    long l = Double.doubleToLongBits (zd);
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.regRn[0] = (int) (l >>> 32);
    XEiJ.regRn[1] = (int) l;
    XEiJ.regCCR = (Double.isNaN (xd) || Double.isNaN (yd) ? 0 :  //引数がNaN
           Double.isNaN (zd) ? XEiJ.REG_CCR_C :  //引数がNaNでないのに結果がNaNのときはエラー。(±0)/(±0)=NaN
           Double.isInfinite (xd) || Double.isInfinite (yd) ? 0 :  //引数が±Inf。(±Inf)/(±0)=(±Inf)
           yd == 0.0 ? XEiJ.REG_CCR_Z | XEiJ.REG_CCR_C :  //除数が±0のときはゼロ除算
           Double.isInfinite (zd) ? XEiJ.REG_CCR_V | XEiJ.REG_CCR_C :  //引数が±Infでないのに結果が±Infのときはオーバーフロー
           0);
  }  //fpkDDIV()

  //fpkDMOD ()
  //  $FE2F  __DMOD
  //  64bit浮動小数点数の剰余算
  //  バグ
  //    FLOAT4.X 1.02はNaN%0がエラーになる
  //  <d0d1.d:64bit浮動小数点数。被除数x
  //  <d2d3.d:64bit浮動小数点数。除数y
  //  >d0d1.d:64bit浮動小数点数。余りx%y。ゼロ除算のときは不定
  //  >ccr:cs=エラー,eq=ゼロ除算
  public static void fpkDMOD () {
    //([int,int]→[long]→double)%([int,int]→[long]→double)→[long]→[int,int]
    double xd = Double.longBitsToDouble ((long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1]);
    double yd = Double.longBitsToDouble ((long) XEiJ.regRn[2] << 32 | 0xffffffffL & XEiJ.regRn[3]);
    double zd = xd % yd;
    long l = Double.doubleToLongBits (zd);
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.regRn[0] = (int) (l >>> 32);
    XEiJ.regRn[1] = (int) l;
    XEiJ.regCCR = (Double.isNaN (xd) || Double.isNaN (yd) ? 0 :  //引数がNaN
           yd == 0.0 ? XEiJ.REG_CCR_Z | XEiJ.REG_CCR_C :  //除数が0のときはゼロ除算。(±Inf)%(±0)=NaN, x%(±0)=(±Inf)
           Double.isNaN (zd) ? XEiJ.REG_CCR_C :  //引数がNaNでないのに結果がNaNのときはエラー。(±Inf)%y=NaN
           Double.isInfinite (xd) || Double.isInfinite (yd) ? 0 :  //引数が±Inf
           Double.isInfinite (zd) ? XEiJ.REG_CCR_V | XEiJ.REG_CCR_C :  //引数が±Infでないのに結果が±Infのときはオーバーフロー
           0);
  }  //fpkDMOD()

  //fpkDABS ()
  //  $FE30  __DABS
  //  64bit浮動小数点数の絶対値
  //  <d0d1.d:64bit浮動小数点数。x
  //  >d0d1.d:64bit浮動小数点数。abs(x)
  public static void fpkDABS () {
    //abs([int,int]→[long]→double)→[long]→[int,int]
    long l = Double.doubleToLongBits (Math.abs (Double.longBitsToDouble ((long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1])));
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.regRn[0] = (int) (l >>> 32);
    XEiJ.regRn[1] = (int) l;
  }  //fpkDABS()

  //fpkDCEIL ()
  //  $FE31  __DCEIL
  //  64bit浮動小数点数の天井関数(引数を下回らない最小の整数)
  //  バグ
  //    FLOAT2.X 2.02/2.03はCEIL(-0)やCEIL(-0.5)が+0になる
  //  <d0d1.d:64bit浮動小数点数。x
  //  >d0d1.d:64bit浮動小数点数。ceil(x)
  public static void fpkDCEIL () {
    //ceil([int,int]→[long]→double)→[long]→[int,int]
    long l = Double.doubleToLongBits (Math.ceil (Double.longBitsToDouble ((long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1])));
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.regRn[0] = (int) (l >>> 32);
    XEiJ.regRn[1] = (int) l;
  }  //fpkDCEIL()

  //fpkDFIX ()
  //  $FE32  __DFIX
  //  64bit浮動小数点数の切り落とし関数(絶対値について引数を上回らない最大の整数)
  //  バグ
  //    FLOAT2.X 2.02/2.03はFIX(-0)やFIX(-0.5)が+0になる
  //  <d0d1.d:64bit浮動小数点数。x
  //  >d0d1.d:64bit浮動小数点数。trunc(x)
  public static void fpkDFIX () {
    //trunc([int,int]→[long]→double)→[long]→[int,int]
    double d = Double.longBitsToDouble ((long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1]);
    long l = Double.doubleToLongBits (0.0 <= d ? Math.floor (d) : Math.ceil (d));  //0<=-0だがMath.floor(-0)=-0なので問題ない
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.regRn[0] = (int) (l >>> 32);
    XEiJ.regRn[1] = (int) l;
  }  //fpkDFIX()

  //fpkDFLOOR ()
  //  $FE33  __DFLOOR
  //  64bit浮動小数点数の床関数(引数を上回らない最大の整数)
  //  バグ
  //    FLOAT2.X 2.02/2.03はFLOOR(-0)が-1になる
  //  <d0d1.d:64bit浮動小数点数。x
  //  >d0d1.d:64bit浮動小数点数。floor(x)
  public static void fpkDFLOOR () {
    //floor([int,int]→[long]→double)→[long]→[int,int]
    long l = Double.doubleToLongBits (Math.floor (Double.longBitsToDouble ((long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1])));
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.regRn[0] = (int) (l >>> 32);
    XEiJ.regRn[1] = (int) l;
  }  //fpkDFLOOR()

  //fpkDFRAC ()
  //  $FE34  __DFRAC
  //  64bit浮動小数点数の幹小数部
  //  メモ
  //    FLOAT2.X 2.02/2.03はDFRAC(±Inf)が±0になる
  //    同様の機能を持ったライブラリによってはfrac(±Inf)が引数の±Infをそのまま返すものもある
  //    x-trunc(x)だとInf-Inf=NaNになる。trunc(x)+frac(x)がxに戻らないので都合が悪い場合があるかも知れない
  //  バグ
  //    FLOAT2.X 2.02/2.03はDFRAC(NaN)=NaNが0になる
  //    FLOAT4.X 1.02はDFRAC(NaN)=NaNが0になる
  //  <d0d1.d:64bit浮動小数点数。x
  //  >d0d1.d:64bit浮動小数点数。frac(x)=copysign(x-trunc(x),x)
  public static void fpkDFRAC () {
    //frac([int,int]→[long]→double)→[long]→[int,int]
    long l = (long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1];
    double d = Double.longBitsToDouble (l);
    l = (Double.isNaN (d) ? Double.doubleToLongBits (Double.NaN) :  //frac(NaN)=NaN
         Double.isInfinite (d) ? l & 0x8000000000000000L :  //frac(±Inf)=±0
         Double.doubleToLongBits (0L <= l ? d - Math.floor (d) : -(-d - Math.floor (-d))));  //0<=-0なので0<=d?～は不可
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.regRn[0] = (int) (l >>> 32);
    XEiJ.regRn[1] = (int) l;
  }  //fpkDFRAC()

  //fpkDSGN ()
  //  $FE35  __DSGN
  //  64bit浮動小数点数の符号
  //  バグ
  //    FLOAT2.X 2.02/2.03はDSGN(-0)=-0が+0になる
  //    FLOAT4.X 1.02はDSGN(-0)=-0が+0になる
  //    FLOAT4.X 1.02はDSGN(NaN)=NaNが+1になる
  //  <d0d1.d:64bit浮動小数点数。x
  //  >d0d1.d:64bit浮動小数点数。signum(x)
  public static void fpkDSGN () {
    //signum([int,int]→[long]→double)→[long]→[int,int]
    long l = Double.doubleToLongBits (Math.signum (Double.longBitsToDouble ((long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1])));
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.regRn[0] = (int) (l >>> 32);
    XEiJ.regRn[1] = (int) l;
  }  //fpkDSGN()

  //fpkSIN ()
  //  $FE36  __SIN
  //  64bit浮動小数点数の正弦
  //  バグ
  //    FLOAT2.X 2.02/2.03はsin(x)でxの絶対値が大きすぎるとき真の値からかけ離れた結果を返す
  //  <d0d1.d:64bit浮動小数点数。x
  //  >d0d1.d:64bit浮動小数点数。sin(x)
  //  >ccr:x=±Inf。C以外は不定
  public static void fpkSIN () {
    //sin([int,int]→[long]→double)→[long]→[int,int]
    double xd = Double.longBitsToDouble ((long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1]);
    double zd = Math.sin (xd);
    long l = Double.doubleToLongBits (zd);
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.regRn[0] = (int) (l >>> 32);
    XEiJ.regRn[1] = (int) l;
    XEiJ.regCCR = !Double.isNaN (xd) && Double.isNaN (zd) ? XEiJ.REG_CCR_C : 0;  //引数がNaNでないのに結果がNaNのときはエラー。sin(±Inf)=NaN
  }  //fpkSIN()

  //fpkCOS ()
  //  $FE37  __COS
  //  64bit浮動小数点数の余弦
  //  バグ
  //    FLOAT2.X 2.02/2.03はcos(x)でxの絶対値が大きすぎるとき真の値からかけ離れた結果を返す
  //  <d0d1.d:64bit浮動小数点数。x
  //  >d0d1.d:64bit浮動小数点数。cos(x)
  //  >ccr:x=±Inf。C以外は不定
  public static void fpkCOS () {
    //cos([int,int]→[long]→double)→[long]→[int,int]
    double xd = Double.longBitsToDouble ((long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1]);
    double zd = Math.cos (xd);
    long l = Double.doubleToLongBits (zd);
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.regRn[0] = (int) (l >>> 32);
    XEiJ.regRn[1] = (int) l;
    XEiJ.regCCR = !Double.isNaN (xd) && Double.isNaN (zd) ? XEiJ.REG_CCR_C : 0;  //引数がNaNでないのに結果がNaNのときはエラー。cos(±Inf)=NaN
  }  //fpkCOS()

  //fpkTAN ()
  //  $FE38  __TAN
  //  64bit浮動小数点数の正接
  //  バグ
  //    FLOAT2.X 2.02/2.03はtan(x)でxの絶対値が大きすぎるとき真の値からかけ離れた結果を返す
  //  <d0d1.d:64bit浮動小数点数。x
  //  >d0d1.d:64bit浮動小数点数。tan(x)
  //  >ccr:x=±Inf。C以外は不定
  public static void fpkTAN () {
    //tan([int,int]→[long]→double)→[long]→[int,int]
    double xd = Double.longBitsToDouble ((long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1]);
    double zd = Math.tan (xd);
    long l = Double.doubleToLongBits (zd);
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.regRn[0] = (int) (l >>> 32);
    XEiJ.regRn[1] = (int) l;
    XEiJ.regCCR = !Double.isNaN (xd) && Double.isNaN (zd) ? XEiJ.REG_CCR_C : 0;  //引数がNaNでないのに結果がNaNのときはエラー。tan(±Inf)=NaN
  }  //fpkTAN()

  //fpkATAN ()
  //  $FE39  __ATAN
  //  64bit浮動小数点数の逆正接
  //  バグ
  //    FLOAT2.X 2.02/2.03は非正規化数を+0とみなす。__ATAN(0x000fffffffffffffL)が+0になる
  //  <d0d1.d:64bit浮動小数点数。x
  //  >d0d1.d:64bit浮動小数点数。atan(x)
  public static void fpkATAN () {
    //atan([int,int]→[long]→double)→[long]→[int,int]
    long l = Double.doubleToLongBits (Math.atan (Double.longBitsToDouble ((long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1])));
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.regRn[0] = (int) (l >>> 32);
    XEiJ.regRn[1] = (int) l;
  }  //fpkATAN()

  //fpkLOG ()
  //  $FE3A  __LOG
  //  64bit浮動小数点数の自然対数
  //  バグ
  //    FLOAT2.X 2.02/2.03は非正規化数を+0とみなす。__LOG(0x000fffffffffffffL)が-InfになってXEiJ.REG_CCR_Cがセットされる
  //    FLOAT2.X 2.02/2.03は__LOG(±0)でCがセットされるがZをセットしない
  //  <d0d1.d:64bit浮動小数点数。x
  //  >d0d1.d:64bit浮動小数点数。log(x)
  //  >ccr:0=エラーなし,C=x<0,Z|C=x==0
  public static void fpkLOG () {
    //log([int,int]→[long]→double)→[long]→[int,int]
    double xd = Double.longBitsToDouble ((long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1]);
    double zd = Math.log (xd);
    long l = Double.doubleToLongBits (zd);
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.regRn[0] = (int) (l >>> 32);
    XEiJ.regRn[1] = (int) l;
    XEiJ.regCCR = (Double.isNaN (xd) ? 0 :  //引数がNaN
           Double.isNaN (zd) ? XEiJ.REG_CCR_C :  //引数がNaNでないのに結果がNaNのときはエラー。log(-x)=NaN
           Double.isInfinite (xd) ? 0 :  //引数が±Inf
           Double.isInfinite (zd) ? XEiJ.REG_CCR_Z | XEiJ.REG_CCR_C :  //引数が±Infでないのに結果が±Infのときはエラー。log(±0)=-Inf
           0);
  }  //fpkLOG()

  //fpkEXP ()
  //  $FE3B  __EXP
  //  64bit浮動小数点数の指数関数
  //  <d0d1.d:64bit浮動小数点数。x
  //  >d0d1.d:64bit浮動小数点数。exp(x)
  //  >ccr:cs=オーバーフロー
  public static void fpkEXP () {
    //exp([int,int]→[long]→double)→[long]→[int,int]
    int xh = XEiJ.regRn[0];
    long l = Double.doubleToLongBits (Math.exp (Double.longBitsToDouble ((long) xh << 32 | 0xffffffffL & XEiJ.regRn[1])));
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    int zh = (int) (l >>> 32);
    XEiJ.regRn[0] = zh;
    XEiJ.regRn[1] = (int) l;
    XEiJ.regCCR = (zh & 0x7ff00000) == 0x7ff00000 && (xh & 0x7ff00000) != 0x7ff00000 ? XEiJ.REG_CCR_C : 0;  //結果が±Inf,NaNだが引数が±Inf,NaNでなかったときはエラー
  }  //fpkEXP()

  //fpkSQR ()
  //  $FE3C  __SQR
  //  64bit浮動小数点数の平方根
  //  バグ
  //    FLOAT4.X 1.02はSQR(-0)=-0が+0になる(実機で確認済み)
  //    FLOAT4.X 1.02はSQR(0x8000000000000001)=NaNがNaNになるがエラーにならない(実機で確認済み)
  //    FLOAT2.X 2.02/2.03は__SQR(0x0000000000000001)が+0になる
  //      __SQR(0x000fffffffffffff)は0x1fffffffffffffffになるので非正規化数がすべて+0になるというわけではない
  //  <d0d1.d:64bit浮動小数点数。x
  //  >d0d1.d:64bit浮動小数点数。sqrt(x)
  //  >ccr:cs=x<0
  public static void fpkSQR () {
    //sqrt([int,int]→[long]→double)→[long]→[int,int]
    double xd = Double.longBitsToDouble ((long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1]);
    long l = Double.doubleToLongBits (Math.sqrt (xd));
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.regRn[0] = (int) (l >>> 32);
    XEiJ.regRn[1] = (int) l;
    XEiJ.regCCR = xd < 0.0 ? XEiJ.REG_CCR_C : 0;  //NaNはエラーなし
  }  //fpkSQR()

  //fpkPI ()
  //  $FE3D  __PI
  //  64bit浮動小数点数の円周率
  //  >d0d1.d:64bit浮動小数点数。pi
  public static void fpkPI () {
    if (true) {
      XEiJ.regRn[0] = 0x400921fb;
      XEiJ.regRn[1] = 0x54442d18;
    } else {
      //pi→[long]→[int,int]
      long l = Double.doubleToLongBits (Math.PI);
      XEiJ.regRn[0] = (int) (l >>> 32);
      XEiJ.regRn[1] = (int) l;
    }
  }  //fpkPI()

  //fpkNPI ()
  //  $FE3E  __NPI
  //  64bit浮動小数点数の円周率倍
  //  メモ
  //    FLOAT2.Xの__NPIは倍精度の円周率を倍精度で掛ける
  //    FLOAT4.Xの__NPIは拡張精度の円周率を拡張精度で掛けて倍精度に丸める
  //    乗算以前に円周率の値が違うので結果が一致しない場合が想像以上に多い
  //  バグ
  //    FLOAT2.X 2.02/2.03は非正規化数を+0とみなす。__NPI(0x000fffffffffffff)が+0になる
  //  <d0d1.d:64bit浮動小数点数。x
  //  >d0d1.d:64bit浮動小数点数。x*pi
  //  >ccr:cs=オーバーフロー
  public static void fpkNPI () {
    //([int,int]→[long]→double)*pi→[long]→[int,int]
    int xh = XEiJ.regRn[0];
    //long l = Double.doubleToLongBits (Double.longBitsToDouble ((long) xh << 32 | 0xffffffffL & r[1]) * Math.PI);
    //  四倍精度のπを四倍精度で掛けて倍精度に丸める
    long l = Double.doubleToLongBits (new QFP (Double.longBitsToDouble ((long) xh << 32 | 0xffffffffL & XEiJ.regRn[1])).mul (QFP.QFP_PI).getd ());
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    int zh = (int) (l >>> 32);
    XEiJ.regRn[0] = zh;
    XEiJ.regRn[1] = (int) l;
    XEiJ.regCCR = (zh & 0x7ff00000) == 0x7ff00000 && (xh & 0x7ff00000) != 0x7ff00000 ? XEiJ.REG_CCR_C : 0;  //結果が±Inf,NaNだが引数が±Inf,NaNでなかったときはエラー
  }  //fpkNPI()

  //fpkPOWER ()
  //  $FE3F  __POWER
  //  64bit浮動小数点数の累乗
  //  バグ
  //    FLOAT4.X 1.02で(+Inf)^(-Inf)を計算しようとするとハングアップする(実機で確認済み)
  //      movem.l d0-d1,-(sp)でd0d1.dを(sp).dで取り出せるようにした後、
  //      spを復元せずにrtsで復帰しようとして0x00f00000にあるCGROMにジャンプしている
  //  <d0d1.d:64bit浮動小数点数。x
  //  <d2d3.d:64bit浮動小数点数。y
  //  >d0d1.d:64bit浮動小数点数。pow(x,y)
  //  >ccr:cs=オーバーフロー
  public static void fpkPOWER () {
    //pow([int,int]→[long]→double,[int,int]→[long]→double)→[long]→[int,int]
    double xd = Double.longBitsToDouble ((long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1]);
    double yd = Double.longBitsToDouble ((long) XEiJ.regRn[2] << 32 | 0xffffffffL & XEiJ.regRn[3]);
    double zd = Math.pow (xd, yd);
    long l = Double.doubleToLongBits (zd);
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.regRn[0] = (int) (l >>> 32);
    XEiJ.regRn[1] = (int) l;
    XEiJ.regCCR = (Double.isNaN (xd) || Double.isNaN (yd) ? 0 :  //引数がNaN
           Double.isNaN (zd) ? XEiJ.REG_CCR_C :  //引数がNaNでないのに結果がNaNのときはエラー
           Double.isInfinite (xd) || Double.isInfinite (yd) ? 0 :  //引数が±Inf
           Double.isInfinite (zd) ? XEiJ.REG_CCR_V | XEiJ.REG_CCR_C :  //引数が±Infでないのに結果が±Infのときはオーバーフロー
           0);
  }  //fpkPOWER()

  //fpkRND ()
  //  $FE40  __RND
  //  64bit浮動小数点数の乱数
  //  >d0d1.d:64bit浮動小数点数。rnd()。0以上1未満
  public static void fpkRND () {
    long l = 0xffffffffL & fpkRndLong ();  //下位
    l |= (0x001fffffL & fpkRndLong ()) << 32;  //上位。順序に注意
    //  ここでl==0LのときLong.numberOfLeadingZeros(z)==64なので乱数の値は2^-54になる
    //  FLOATn.Xはその場合を考慮しておらず0のときも先頭の1のビットを探し続けて無限ループに陥ると思われる
    //  実際に0が53ビット並ぶことはないかも知れない
    int o = Long.numberOfLeadingZeros (l) - 11;
    l = (long) (0x3fe - o) << 52 | 0x000fffffffffffffL & l << o;
    XEiJ.regRn[0] = (int) (l >>> 32);
    XEiJ.regRn[1] = (int) l;
  }  //fpkRND()

  //fpkSINH ()
  //  $FE41  __SINH
  //  64bit浮動小数点数の双曲線正弦
  //  バグ
  //    FLOAT2.X 2.02/2.03は__SINH(NaN)がエラーになる
  //    FLOAT2.X 2.02/2.03は|x|<1.5/2^53のとき__SINH(x)が0になる
  //      sinh(x)=(exp(x)-1/exp(x))/2だけで計算しており、x≒0のとき情報落ちでexp(x)≒1+x≒1となることが考慮されていない
  //      sinh(0x3ca8000000000000)=0x3ca8000000000000=1.5/2^53
  //      sinh(0x3ca7ffffffffffff)=0x0000000000000000  ここから突然0になる
  //    FLOAT2.X 2.02/2.03はx<-6243314768165359/274877906944=-22713.0468…のときVがセットされない
  //      sinh(0xc0d62e42fefa39ef)はV|C
  //      sinh(0xc0d62e42fefa39f0)はC
  //      exp(x)がアンダーフローした時点でエラー終了してしまいオーバーフローするexp(-x)を計算していない
  //      この場合はアンダーフローした側を0として扱うべき
  //  <d0d1.d:64bit浮動小数点数。x
  //  >d0d1.d:64bit浮動小数点数。sinh(x)
  //  >ccr:cs=エラー,vs=オーバーフロー
  public static void fpkSINH () {
    //sinh([int,int]→[long]→double)→[long]→[int,int]
    double xd = Double.longBitsToDouble ((long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1]);
    double zd = Math.sinh (xd);
    long l = Double.doubleToLongBits (zd);
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.regRn[0] = (int) (l >>> 32);
    XEiJ.regRn[1] = (int) l;
    XEiJ.regCCR = (Double.isNaN (xd) ? 0 :  //引数がNaN
           Double.isNaN (zd) ? XEiJ.REG_CCR_C :  //引数がNaNでないのに結果がNaNのときはエラー
           Double.isInfinite (xd) ? 0 :  //引数が±Inf
           Double.isInfinite (zd) ? XEiJ.REG_CCR_V | XEiJ.REG_CCR_C :  //引数が±Infでないのに結果が±Infのときはオーバーフロー
           0);
  }  //fpkSINH()

  //fpkCOSH ()
  //  $FE42  __COSH
  //  64bit浮動小数点数の双曲線余弦
  //  バグ
  //    FLOAT2.X 2.02/2.03はCOSH(NaN)=NaNが+Infになる
  //  <d0d1.d:64bit浮動小数点数。x
  //  >d0d1.d:64bit浮動小数点数。cosh(x)
  //  >ccr:cs=エラー,vs=オーバーフロー
  public static void fpkCOSH () {
    //cosh([int,int]→[long]→double)→[long]→[int,int]
    double xd = Double.longBitsToDouble ((long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1]);
    double zd = Math.cosh (xd);
    long l = Double.doubleToLongBits (zd);
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.regRn[0] = (int) (l >>> 32);
    XEiJ.regRn[1] = (int) l;
    XEiJ.regCCR = (Double.isNaN (xd) ? 0 :  //引数がNaN
           Double.isNaN (zd) ? XEiJ.REG_CCR_C :  //引数がNaNでないのに結果がNaNのときはエラー
           Double.isInfinite (xd) ? 0 :  //引数が±Inf
           Double.isInfinite (zd) ? XEiJ.REG_CCR_V | XEiJ.REG_CCR_C :  //引数が±Infでないのに結果が±Infのときはオーバーフロー
           0);
  }  //fpkCOSH()

  //fpkTANH ()
  //  $FE43  __TANH
  //  64bit浮動小数点数の双曲線正接
  //  <d0d1.d:64bit浮動小数点数。x
  //  >d0d1.d:64bit浮動小数点数。tanh(x)
  public static void fpkTANH () {
    //tanh([int,int]→[long]→double)→[long]→[int,int]
    long l = Double.doubleToLongBits (Math.tanh (Double.longBitsToDouble ((long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1])));
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.regRn[0] = (int) (l >>> 32);
    XEiJ.regRn[1] = (int) l;
  }  //fpkTANH()

  //fpkATANH ()
  //  $FE44  __ATANH
  //  64bit浮動小数点数の逆双曲線正接
  //  <d0d1.d:64bit浮動小数点数。x
  //  >d0d1.d:64bit浮動小数点数。atanh(x)
  //  >ccr:cs=x<=-1||1<=x
  public static void fpkATANH () {
    //atanh([int,int]→[long]→double)→[long]→[int,int]
    double d = Double.longBitsToDouble ((long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1]);
    if (true) {
      QFP.fpsr = 0;
      d = new QFP (d).atanh ().getd ();
      XEiJ.regCCR = (QFP.fpsr & (QFP.QFP_OE | QFP.QFP_DZ)) != 0 ? XEiJ.REG_CCR_C : 0;
    } else {
      double s = Math.signum (d);
      double a = Math.abs (d);
      if (a < 1.0) {
        d = s * (Math.log1p (a) - Math.log1p (-a)) * 0.5;  //Math.atanh(double)がない
        XEiJ.regCCR = 0;
      } else if (a == 1.0) {
        d = s * Double.POSITIVE_INFINITY;
        XEiJ.regCCR = XEiJ.REG_CCR_C;
      } else {
        d = Double.NaN;
        XEiJ.regCCR = XEiJ.REG_CCR_C;
      }
    }
    long l = Double.doubleToLongBits (d);
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.regRn[0] = (int) (l >>> 32);
    XEiJ.regRn[1] = (int) l;
  }  //fpkATANH()

  //fpkASIN ()
  //  $FE45  __ASIN
  //  64bit浮動小数点数の逆正弦
  //  <d0d1.d:64bit浮動小数点数。x
  //  >d0d1.d:64bit浮動小数点数。asin(x)
  //  >ccr:cs=x<-1||1<x
  public static void fpkASIN () {
    //asin([int,int]→[long]→double)→[long]→[int,int]
    double d = Double.longBitsToDouble ((long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1]);
    if (d < -1.0 || 1.0 < d) {  //定義域の外。±1,NaNを含まない
      d = Double.NaN;
      XEiJ.regCCR = XEiJ.REG_CCR_C;
    } else {  //定義域の中。±1,NaNを含む
      d = Math.asin (d);
      XEiJ.regCCR = 0;
    }
    long l = Double.doubleToLongBits (d);
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.regRn[0] = (int) (l >>> 32);
    XEiJ.regRn[1] = (int) l;
  }  //fpkASIN()

  //fpkACOS ()
  //  $FE46  __ACOS
  //  64bit浮動小数点数の逆余弦
  //  <d0d1.d:64bit浮動小数点数。x
  //  >d0d1.d:64bit浮動小数点数。acos(x)
  //  >ccr:cs=x<-1||1<x
  public static void fpkACOS () {
    //acos([int,int]→[long]→double)→[long]→[int,int]
    double d = Double.longBitsToDouble ((long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1]);
    if (d < -1.0 || 1.0 < d) {  //定義域の外。±1,NaNを含まない
      d = Double.NaN;
      XEiJ.regCCR = XEiJ.REG_CCR_C;
    } else {  //定義域の中。±1,NaNを含む
      d = Math.acos (d);
      XEiJ.regCCR = 0;
    }
    long l = Double.doubleToLongBits (d);
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.regRn[0] = (int) (l >>> 32);
    XEiJ.regRn[1] = (int) l;
  }  //fpkACOS()

  //fpkLOG10 ()
  //  $FE47  __LOG10
  //  64bit浮動小数点数の常用対数
  //  <d0d1.d:64bit浮動小数点数。x
  //  >d0d1.d:64bit浮動小数点数。log10(x)
  //  >ccr:0=エラーなし,C=x<0,Z|C=x==0
  public static void fpkLOG10 () {
    //log10([int,int]→[long]→double)→[long]→[int,int]
    double xd = Double.longBitsToDouble ((long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1]);
    double zd = Math.log10 (xd);
    long l = Double.doubleToLongBits (zd);
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.regRn[0] = (int) (l >>> 32);
    XEiJ.regRn[1] = (int) l;
    XEiJ.regCCR = (Double.isNaN (xd) ? 0 :  //引数がNaN
           Double.isNaN (zd) ? XEiJ.REG_CCR_C :  //引数がNaNでないのに結果がNaNのときはエラー。log10(-x)=NaN
           Double.isInfinite (xd) ? 0 :  //引数が±Inf
           Double.isInfinite (zd) ? XEiJ.REG_CCR_Z | XEiJ.REG_CCR_C :  //引数が±Infでないのに結果が±Infのときはエラー。log10(±0)=-Inf
           0);
  }  //fpkLOG10()

  //fpkLOG2 ()
  //  $FE48  __LOG2
  //  64bit浮動小数点数の二進対数
  //  <d0d1.d:64bit浮動小数点数。x
  //  >d0d1.d:64bit浮動小数点数。log2(x)
  //  >ccr:0=エラーなし,C=x<0,Z|C=x==0
  public static void fpkLOG2 () {
    //log2([int,int]→[long]→double)→[long]→[int,int]
    double xd = Double.longBitsToDouble ((long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1]);
    double zd = Math.log (xd) / 0.69314718055994530941723212146;  //log(2)。Math.log2(double)がない
    long l = Double.doubleToLongBits (zd);
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.regRn[0] = (int) (l >>> 32);
    XEiJ.regRn[1] = (int) l;
    XEiJ.regCCR = (Double.isNaN (xd) ? 0 :  //引数がNaN
           Double.isNaN (zd) ? XEiJ.REG_CCR_C :  //引数がNaNでないのに結果がNaNのときはエラー。log2(-x)=NaN
           Double.isInfinite (xd) ? 0 :  //引数が±Inf
           Double.isInfinite (zd) ? XEiJ.REG_CCR_Z | XEiJ.REG_CCR_C :  //引数が±Infでないのに結果が±Infのときはエラー。log2(±0)=-Inf
           0);
  }  //fpkLOG2()

  //fpkDFREXP ()
  //  $FE49  __DFREXP
  //  64bit浮動小数点数の分解
  //  <d0d1.d:64bit浮動小数点数。x
  //  >d0d1.d:xの指数部を0にした値。x==0のときは0
  //  >d2.l:xの指数部。x==0のときは0
  public static void fpkDFREXP () {
    int h = XEiJ.regRn[0];
    if (h << 1 == 0 && XEiJ.regRn[1] == 0) {  //0のとき
      XEiJ.regRn[0] = 0;
      XEiJ.regRn[2] = 0;
    } else {  //0でないとき
      XEiJ.regRn[0] = (h & 0x800fffff) | 0x3ff00000;  //符号と仮数部を残して指数部を0にする
      XEiJ.regRn[2] = (h >>> 20 & 0x7ff) - 0x3ff;
    }
  }  //fpkDFREXP()

  //fpkDLDEXP ()
  //  $FE4A  __DLDEXP
  //  64bit浮動小数点数の合成
  //  <d0d1.d:64bit浮動小数点数。x
  //  <d2.l:xの指数部に加える値。y
  //  >d0d1.d:xの指数部にyを加えた値。x==0のときは0。エラーのときは変化しない
  //  >ccr:cs=指数部が範囲外
  public static void fpkDLDEXP () {
    int h = XEiJ.regRn[0];
    if (h << 1 == 0 && XEiJ.regRn[1] == 0) {  //0のとき
      XEiJ.regRn[0] = 0;
      XEiJ.regCCR = 0;
    } else {  //0でないとき
      int t = (h >>> 20 & 0x7ff) + XEiJ.regRn[2];
      if ((t & ~0x7ff) != 0) {  //指数部が範囲外
        XEiJ.regCCR = XEiJ.REG_CCR_C;
      } else {
        XEiJ.regRn[0] = (h & 0x800fffff) | t << 20;  //符号と仮数部を残して指数部を交換する
        XEiJ.regCCR = 0;
      }
    }
  }  //fpkDLDEXP()

  //fpkDADDONE ()
  //  $FE4B  __DADDONE
  //  64bit浮動小数点数に1を加える
  //  <d0d1.d:64bit浮動小数点数。x
  //  >d0d1.d:64bit浮動小数点数。x+1
  public static void fpkDADDONE () {
    //([int,int]→[long]→double)+1→[long]→[int,int]
    double xd = Double.longBitsToDouble ((long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1]);
    double zd = xd + 1.0;
    long l = Double.doubleToLongBits (zd);
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.regRn[0] = (int) (l >>> 32);
    XEiJ.regRn[1] = (int) l;
    XEiJ.regCCR = Double.isInfinite (zd) && !Double.isInfinite (xd) ? XEiJ.REG_CCR_V | XEiJ.REG_CCR_C : 0;  //結果が±Infだが引数が±Infでないときはオーバーフロー
  }  //fpkDADDONE()

  //fpkDSUBONE ()
  //  $FE4C  __DSUBONE
  //  64bit浮動小数点数から1を引く
  //  <d0d1.d:64bit浮動小数点数。x
  //  >d0d1.d:64bit浮動小数点数。x-1
  public static void fpkDSUBONE () {
    //([int,int]→[long]→double)-1→[long]→[int,int]
    double xd = Double.longBitsToDouble ((long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1]);
    double zd = xd - 1.0;
    long l = Double.doubleToLongBits (zd);
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.regRn[0] = (int) (l >>> 32);
    XEiJ.regRn[1] = (int) l;
    XEiJ.regCCR = Double.isInfinite (zd) && !Double.isInfinite (xd) ? XEiJ.REG_CCR_V | XEiJ.REG_CCR_C : 0;  //結果が±Infだが引数が±Infでないときはオーバーフロー
  }  //fpkDSUBONE()

  //fpkDDIVTWO ()
  //  $FE4D  __DDIVTWO
  //  64bit浮動小数点数を2で割る
  //  バグ
  //    FLOAT2.X 2.02/2.03は__DDIVTWO(0x0010000000000000L)がエラーになる
  //    FLOAT4.X 1.02は__DDIVTWO(0x0010000000000000L)がエラーになる
  //    FLOAT2.X 2.02/2.03は__DDIVTWO(0x000fffffffffffffL)がエラーになる
  //    FLOAT4.X 1.02は__DDIVTWO(0x000fffffffffffffL)がエラーになる
  //    FLOAT2.X 2.02/2.03は__DDIVTWO(0x0000000000000001L)が0x0000000000000001Lになる
  //    FLOAT4.X 1.02は__DDIVTWO(0x0000000000000001L)が0x0000000000000001Lになる
  //    FLOAT2.X 2.02/2.03は__DDIVTWO(-0)がエラーになる
  //    FLOAT4.X 1.02は__DDIVTWO(-0)がエラーになる
  //    FLOAT2.X 2.02/2.03は__DDIVTWO(0x8000000000000001L)がエラーになる
  //    FLOAT4.X 1.02は__DDIVTWO(0x8000000000000001L)がエラーになる
  //    FLOAT2.X 2.02/2.03は__DDIVTWO(0x800fffffffffffffL)がエラーになる
  //    FLOAT4.X 1.02は__DDIVTWO(0x800fffffffffffffL)がエラーになる
  //    FLOAT2.X 2.02/2.03は__DDIVTWO(0x8010000000000000L)がエラーになる
  //    FLOAT4.X 1.02は__DDIVTWO(0x8010000000000000L)がエラーになる
  //  <d0d1.d:64bit浮動小数点数。x
  //  >d0d1.d:64bit浮動小数点数。x/2
  //  >ccr:cs=アンダーフロー
  public static void fpkDDIVTWO () {
    //([int,int]→[long]→double)/2→[long]→[int,int]
    double xd = Double.longBitsToDouble ((long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1]);
    double zd = xd * 0.5;
    long l = Double.doubleToLongBits (zd);
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.regRn[0] = (int) (l >>> 32);
    XEiJ.regRn[1] = (int) l;
    XEiJ.regCCR = zd == 0.0 && xd != 0.0 ? XEiJ.REG_CCR_C : 0;  //結果が±0だが引数が±0でないときはアンダーフロー
  }  //fpkDDIVTWO()

  //fpkDIEECNV ()
  //  $FE4E  __DIEECNV
  //  64bit浮動小数点数をIEEEフォーマットに変換する(FLOAT1.X以外は何もしない)
  //  <d0d1.d:64bit浮動小数点数。x
  //  >d0d1.d:64bit浮動小数点数。x
  public static void fpkDIEECNV () {
  }  //fpkDIEECNV()

  //fpkIEEDCNV ()
  //  $FE4F  __IEEDCNV
  //  64bit浮動小数点数をIEEEフォーマットから変換する(FLOAT1.X以外は何もしない)
  //  <d0d1.d:64bit浮動小数点数。x
  //  >d0d1.d:64bit浮動小数点数。x
  public static void fpkIEEDCNV () {
  }  //fpkIEEDCNV()

  //fpkFTST ()
  //  $FE58  __FTST
  //  32bit浮動小数点数と0の比較
  //  x<=>0
  //  <d0.s:32bit浮動小数点数。x
  //  >ccr:lt=x<0,eq=x==0,gt=x>0
  public static void fpkFTST () {
    if (true) {
      int h = XEiJ.regRn[0];
      XEiJ.regCCR = h << 1 == 0 ? XEiJ.REG_CCR_Z : 0 <= h ? 0 : XEiJ.REG_CCR_N;  //NaNのときは0
    } else {
      //([int]→float)<=>0
      float f = Float.intBitsToFloat (XEiJ.regRn[0]);
      XEiJ.regCCR = f < 0.0F ? XEiJ.REG_CCR_N : f == 0.0F ? XEiJ.REG_CCR_Z : 0;  //NaNのときは0
    }
  }  //fpkFTST()

  //fpkFCMP ()
  //  $FE59  __FCMP
  //  32bit浮動小数点数の比較
  //  x<=>y
  //  <d0.s:32bit浮動小数点数。x
  //  <d1.s:32bit浮動小数点数。y
  //  >ccr:lt=x<y,eq=x==y,gt=x>y
  public static void fpkFCMP () {
    //([int]→float)<=>([int]→float)
    float xf = Float.intBitsToFloat (XEiJ.regRn[0]);
    float yf = Float.intBitsToFloat (XEiJ.regRn[1]);
    XEiJ.regCCR = xf < yf ? XEiJ.REG_CCR_N | XEiJ.REG_CCR_C : xf == yf ? XEiJ.REG_CCR_Z : 0;  //どちらかがNaNのときは0
  }  //fpkFCMP()

  //fpkFNEG ()
  //  $FE5A  __FNEG
  //  32bit浮動小数点数の符号反転
  //  <d0.s:32bit浮動小数点数。x
  //  >d0.s:32bit浮動小数点数。-x
  public static void fpkFNEG () {
    //-([int]→float)→[int]
    int h = Float.floatToIntBits (-Float.intBitsToFloat (XEiJ.regRn[0]));
    if (FPK_FPCP_NAN && h == 0x7fc00000) {
      h = 0x7fffffff;
    }
    XEiJ.regRn[0] = h;
  }  //fpkFNEG()

  //fpkFADD ()
  //  $FE5B  __FADD
  //  32bit浮動小数点数の加算
  //  <d0.s:32bit浮動小数点数。被加算数x
  //  <d1.s:32bit浮動小数点数。加算数y
  //  >d0.s:32bit浮動小数点数。和x+y
  //  >ccr:cs=エラー,vs=オーバーフロー
  public static void fpkFADD () {
    //([int]→float)+([int]→float)→[int]
    float xf = Float.intBitsToFloat (XEiJ.regRn[0]);
    float yf = Float.intBitsToFloat (XEiJ.regRn[1]);
    float zf = xf + yf;
    int h = Float.floatToIntBits (zf);
    if (FPK_FPCP_NAN && h == 0x7fc00000) {
      h = 0x7fffffff;
    }
    XEiJ.regRn[0] = h;
    XEiJ.regCCR = (Float.isNaN (xf) || Float.isNaN (yf) ? 0 :  //引数がNaN
           Float.isNaN (zf) ? XEiJ.REG_CCR_C :  //引数がNaNでないのに結果がNaNのときはエラー。(+Inf)+(-Inf)=NaN
           Float.isInfinite (xf) || Float.isInfinite (yf) ? 0 :  //引数が±Inf
           Float.isInfinite (zf) ? XEiJ.REG_CCR_V | XEiJ.REG_CCR_C :  //引数が±Infでないのに結果が±Infのときはオーバーフロー
           0);
  }  //fpkFADD()

  //fpkFSUB ()
  //  $FE5C  __FSUB
  //  32bit浮動小数点数の減算
  //  <d0.s:32bit浮動小数点数。被減算数x
  //  <d1.s:32bit浮動小数点数。減算数y
  //  >d0.s:32bit浮動小数点数。差x-y
  //  >ccr:cs=エラー,vs=オーバーフロー
  public static void fpkFSUB () {
    //([int]→float)-([int]→float)→[int]
    float xf = Float.intBitsToFloat (XEiJ.regRn[0]);
    float yf = Float.intBitsToFloat (XEiJ.regRn[1]);
    float zf = xf - yf;
    int h = Float.floatToIntBits (zf);
    if (FPK_FPCP_NAN && h == 0x7fc00000) {
      h = 0x7fffffff;
    }
    XEiJ.regRn[0] = h;
    XEiJ.regCCR = (Float.isNaN (xf) || Float.isNaN (yf) ? 0 :  //引数がNaN
           Float.isNaN (zf) ? XEiJ.REG_CCR_C :  //引数がNaNでないのに結果がNaNのときはエラー。(+Inf)-(+Inf)=NaN
           Float.isInfinite (xf) || Float.isInfinite (yf) ? 0 :  //引数が±Inf
           Float.isInfinite (zf) ? XEiJ.REG_CCR_V | XEiJ.REG_CCR_C :  //引数が±Infでないのに結果が±Infのときはオーバーフロー
           0);
  }  //fpkFSUB()

  //fpkFMUL ()
  //  $FE5D  __FMUL
  //  32bit浮動小数点数の乗算
  //  <d0.s:32bit浮動小数点数。被乗数x
  //  <d1.s:32bit浮動小数点数。乗数y
  //  >d0.s:32bit浮動小数点数。積x*y
  //  >ccr:cs=エラー,vs=オーバーフロー
  public static void fpkFMUL () {
    //([int]→float)*([int]→float)→[int]
    float xf = Float.intBitsToFloat (XEiJ.regRn[0]);
    float yf = Float.intBitsToFloat (XEiJ.regRn[1]);
    float zf = xf * yf;
    int h = Float.floatToIntBits (zf);
    if (FPK_FPCP_NAN && h == 0x7fc00000) {
      h = 0x7fffffff;
    }
    XEiJ.regRn[0] = h;
    XEiJ.regCCR = (Float.isNaN (xf) || Float.isNaN (yf) ? 0 :  //引数がNaN
           Float.isNaN (zf) ? XEiJ.REG_CCR_C :  //引数がNaNでないのに結果がNaNのときはエラー。(±0)*(±Inf)=NaN
           Float.isInfinite (xf) || Float.isInfinite (yf) ? 0 :  //引数が±Inf
           Float.isInfinite (zf) ? XEiJ.REG_CCR_V | XEiJ.REG_CCR_C :  //引数が±Infでないのに結果が±Infのときはオーバーフロー
           0);
  }  //fpkFMUL()

  //fpkFDIV ()
  //  $FE5E  __FDIV
  //  32bit浮動小数点数の除算
  //  <d0.s:32bit浮動小数点数。被除数x
  //  <d1.s:32bit浮動小数点数。除数y
  //  >d0.s:32bit浮動小数点数。商x/y。ゼロ除算のときは不定
  //  >ccr:cs=エラー,eq=ゼロ除算,vs=オーバーフロー
  public static void fpkFDIV () {
    //([int]→float)/([int]→float)→[int]
    float xf = Float.intBitsToFloat (XEiJ.regRn[0]);
    float yf = Float.intBitsToFloat (XEiJ.regRn[1]);
    float zf = xf / yf;
    int h = Float.floatToIntBits (zf);
    if (FPK_FPCP_NAN && h == 0x7fc00000) {
      h = 0x7fffffff;
    }
    XEiJ.regRn[0] = h;
    XEiJ.regCCR = (Float.isNaN (xf) || Float.isNaN (yf) ? 0 :  //引数がNaN
           Float.isNaN (zf) ? XEiJ.REG_CCR_C :  //引数がNaNでないのに結果がNaNのときはエラー。(±0)/(±0)=NaN
           Float.isInfinite (xf) || Float.isInfinite (yf) ? 0 :  //引数が±Inf。(±Inf)/(±0)=(±Inf)
           yf == 0.0F ? XEiJ.REG_CCR_Z | XEiJ.REG_CCR_C :  //除数が±0のときはゼロ除算
           Float.isInfinite (zf) ? XEiJ.REG_CCR_V | XEiJ.REG_CCR_C :  //引数が±Infでないのに結果が±Infのときはオーバーフロー
           0);
  }  //fpkFDIV()

  //fpkFMOD ()
  //  $FE5F  __FMOD
  //  32bit浮動小数点数の剰余算
  //  <d0.s:32bit浮動小数点数。被除数x
  //  <d1.s:32bit浮動小数点数。除数y
  //  >d0.s:32bit浮動小数点数。余りx%y。ゼロ除算のときは不定
  //  >ccr:cs=エラー,eq=ゼロ除算
  public static void fpkFMOD () {
    //([int]→float)%([int]→float)→[int]
    float xf = Float.intBitsToFloat (XEiJ.regRn[0]);
    float yf = Float.intBitsToFloat (XEiJ.regRn[1]);
    float zf = xf % yf;
    int h = Float.floatToIntBits (zf);
    if (FPK_FPCP_NAN && h == 0x7fc00000) {
      h = 0x7fffffff;
    }
    XEiJ.regRn[0] = h;
    XEiJ.regCCR = (Float.isNaN (xf) || Float.isNaN (yf) ? 0 :  //引数がNaN
           yf == 0.0F ? XEiJ.REG_CCR_Z | XEiJ.REG_CCR_C :  //除数が0のときはゼロ除算。(±Inf)%(±0)=NaN, x%(±0)=(±Inf)
           Float.isNaN (zf) ? XEiJ.REG_CCR_C :  //引数がNaNでないのに結果がNaNのときはエラー。(±Inf)%y=NaN
           Float.isInfinite (xf) || Float.isInfinite (yf) ? 0 :  //引数が±Inf
           Float.isInfinite (zf) ? XEiJ.REG_CCR_V | XEiJ.REG_CCR_C :  //引数が±Infでないのに結果が±Infのときはオーバーフロー
           0);
  }  //fpkFMOD()

  //fpkFABS ()
  //  $FE60  __FABS
  //  32bit浮動小数点数の絶対値
  //  <d0.s:32bit浮動小数点数。x
  //  >d0.s:32bit浮動小数点数。abs(x)
  public static void fpkFABS () {
    //abs([int]→float)→[int]
    int h = Float.floatToIntBits (Math.abs (Float.intBitsToFloat (XEiJ.regRn[0])));
    if (FPK_FPCP_NAN && h == 0x7fc00000) {
      h = 0x7fffffff;
    }
    XEiJ.regRn[0] = h;
  }  //fpkFABS()

  //fpkFCEIL ()
  //  $FE61  __FCEIL
  //  32bit浮動小数点数の天井関数(引数を下回らない最小の整数)
  //  <d0.s:32bit浮動小数点数。x
  //  >d0.s:32bit浮動小数点数。ceil(x)
  public static void fpkFCEIL () {
    //ceil([int]→float→double)→float→[int]
    int h = Float.floatToIntBits ((float) Math.ceil ((double) Float.intBitsToFloat (XEiJ.regRn[0])));
    if (FPK_FPCP_NAN && h == 0x7fc00000) {
      h = 0x7fffffff;
    }
    XEiJ.regRn[0] = h;
  }  //fpkFCEIL()

  //fpkFFIX ()
  //  $FE62  __FFIX
  //  32bit浮動小数点数の切り落とし関数(絶対値について引数を上回らない最大の整数)
  //  <d0.s:32bit浮動小数点数。x
  //  >d0.s:32bit浮動小数点数。trunc(x)
  public static void fpkFFIX () {
    //trunc([int]→float→double)→float→[int]
    float f = Float.intBitsToFloat (XEiJ.regRn[0]);
    int h = Float.floatToIntBits (0.0 <= f ? (float) Math.floor ((double) f) : (float) Math.ceil ((double) f));  //0<=-0だがMath.floor(-0)=-0なので問題ない
    if (FPK_FPCP_NAN && h == 0x7fc00000) {
      h = 0x7fffffff;
    }
    XEiJ.regRn[0] = h;
  }  //fpkFFIX()

  //fpkFFLOOR ()
  //  $FE63  __FFLOOR
  //  32bit浮動小数点数の床関数(引数を上回らない最大の整数)
  //  <d0.s:32bit浮動小数点数。x
  //  >d0.s:32bit浮動小数点数。floor(x)
  public static void fpkFFLOOR () {
    //floor([int]→float→double)→float→[int]
    int h = Float.floatToIntBits ((float) Math.floor ((double) Float.intBitsToFloat (XEiJ.regRn[0])));
    if (FPK_FPCP_NAN && h == 0x7fc00000) {
      h = 0x7fffffff;
    }
    XEiJ.regRn[0] = h;
  }  //fpkFFLOOR()

  //fpkFFRAC ()
  //  $FE64  __FFRAC
  //  32bit浮動小数点数の幹小数部
  //  <d0.s:32bit浮動小数点数。x
  //  >d0.s:32bit浮動小数点数。frac(x)=copysign(x-trunc(x),x)
  public static void fpkFFRAC () {
    //frac([int]→float→double)→float→[int]
    int h = XEiJ.regRn[0];
    float f = Float.intBitsToFloat (h);
    h = (Float.isNaN (f) ? Float.floatToIntBits (Float.NaN) :  //frac(NaN)=NaN
         Float.isInfinite (f) ? h & 0x80000000 :  //frac(±Inf)=±0
         Float.floatToIntBits (0 <= h ? f - (float) Math.floor ((double) f) : -(-f - (float) Math.floor ((double) -f))));  //0<=-0なので0<=f?～は不可
    if (FPK_FPCP_NAN && h == 0x7fc00000) {
      h = 0x7fffffff;
    }
    XEiJ.regRn[0] = h;
  }  //fpkFFRAC()

  //fpkFSGN ()
  //  $FE65  __FSGN
  //  32bit浮動小数点数の符号
  //  <d0.s:32bit浮動小数点数。x
  //  >d0.s:32bit浮動小数点数。signum(x)
  public static void fpkFSGN () {
    //signum([int]→float)→[int]
    int h = Float.floatToIntBits (Math.signum (Float.intBitsToFloat (XEiJ.regRn[0])));  //Math.signum(-0)は-0
    if (FPK_FPCP_NAN && h == 0x7fc00000) {
      h = 0x7fffffff;
    }
    XEiJ.regRn[0] = h;
  }  //fpkFSGN()

  //fpkFSIN ()
  //  $FE66  __FSIN
  //  32bit浮動小数点数の正弦
  //  <d0.s:32bit浮動小数点数。x
  //  >d0.s:32bit浮動小数点数。sin(x)
  //  >ccr:x=±Inf。C以外は不定
  public static void fpkFSIN () {
    //sin([int]→float→double)→float→[int]
    float xf = Float.intBitsToFloat (XEiJ.regRn[0]);
    float zf = (float) Math.sin ((double) xf);
    int h = Float.floatToIntBits (zf);
    if (FPK_FPCP_NAN && h == 0x7fc00000) {
      h = 0x7fffffff;
    }
    XEiJ.regRn[0] = h;
    XEiJ.regCCR = !Float.isNaN (xf) && Float.isNaN (zf) ? XEiJ.REG_CCR_C : 0;  //引数がNaNでないのに結果がNaNのときはエラー。sin(±Inf)=NaN
  }  //fpkFSIN()

  //fpkFCOS ()
  //  $FE67  __FCOS
  //  32bit浮動小数点数の余弦
  //  <d0.s:32bit浮動小数点数。x
  //  >d0.s:32bit浮動小数点数。cos(x)
  //  >ccr:x=±Inf。C以外は不定
  public static void fpkFCOS () {
    //cos([int]→float→double)→float→[int]
    float xf = Float.intBitsToFloat (XEiJ.regRn[0]);
    float zf = (float) Math.cos ((double) xf);
    int h = Float.floatToIntBits (zf);
    if (FPK_FPCP_NAN && h == 0x7fc00000) {
      h = 0x7fffffff;
    }
    XEiJ.regRn[0] = h;
    XEiJ.regCCR = !Float.isNaN (xf) && Float.isNaN (zf) ? XEiJ.REG_CCR_C : 0;  //引数がNaNでないのに結果がNaNのときはエラー。cos(±Inf)=NaN
  }  //fpkFCOS()

  //fpkFTAN ()
  //  $FE68  __FTAN
  //  32bit浮動小数点数の正接
  //  <d0.s:32bit浮動小数点数。x
  //  >d0.s:32bit浮動小数点数。tan(x)
  //  >ccr:x=±Inf。C以外は不定
  public static void fpkFTAN () {
    //tan([int]→float→double)→float→[int]
    float xf = Float.intBitsToFloat (XEiJ.regRn[0]);
    float zf = (float) Math.tan ((double) xf);
    int h = Float.floatToIntBits (zf);
    if (FPK_FPCP_NAN && h == 0x7fc00000) {
      h = 0x7fffffff;
    }
    XEiJ.regRn[0] = h;
    XEiJ.regCCR = !Float.isNaN (xf) && Float.isNaN (zf) ? XEiJ.REG_CCR_C : 0;  //引数がNaNでないのに結果がNaNのときはエラー。tan(±Inf)=NaN
  }  //fpkFTAN()

  //fpkFATAN ()
  //  $FE69  __FATAN
  //  32bit浮動小数点数の逆正接
  //  <d0.s:32bit浮動小数点数。x
  //  >d0.s:32bit浮動小数点数。atan(x)
  public static void fpkFATAN () {
    //atan([int]→float→double)→float→[int]
    int h = Float.floatToIntBits ((float) Math.atan ((double) Float.intBitsToFloat (XEiJ.regRn[0])));
    if (FPK_FPCP_NAN && h == 0x7fc00000) {
      h = 0x7fffffff;
    }
    XEiJ.regRn[0] = h;
  }  //fpkFATAN()

  //fpkFLOG ()
  //  $FE6A  __FLOG
  //  32bit浮動小数点数の自然対数
  //  <d0.s:32bit浮動小数点数。x
  //  >d0.s:32bit浮動小数点数。log(x)
  //  >ccr:0=エラーなし,C=x<0,Z|C=x==0
  public static void fpkFLOG () {
    //log([int]→float→double)→float→[int]
    float xf = Float.intBitsToFloat (XEiJ.regRn[0]);
    float zf = (float) Math.log ((double) xf);
    int h = Float.floatToIntBits (zf);
    if (FPK_FPCP_NAN && h == 0x7fc00000) {
      h = 0x7fffffff;
    }
    XEiJ.regRn[0] = h;
    XEiJ.regCCR = (Float.isNaN (xf) ? 0 :  //引数がNaN
           Float.isNaN (zf) ? XEiJ.REG_CCR_C :  //引数がNaNでないのに結果がNaNのときはエラー。log(-x)=NaN
           Float.isInfinite (xf) ? 0 :  //引数が±Inf
           Float.isNaN (zf) ? XEiJ.REG_CCR_Z | XEiJ.REG_CCR_C :  //引数が±Infでないのに結果が±Infのときはエラー。log(±0)=-Inf
           0);
  }  //fpkFLOG()

  //fpkFEXP ()
  //  $FE6B  __FEXP
  //  32bit浮動小数点数の指数関数
  //  <d0.s:32bit浮動小数点数。x
  //  >d0.s:32bit浮動小数点数。exp(x)
  //  >ccr:cs=オーバーフロー
  public static void fpkFEXP () {
    //exp([int]→float→double)→float→[int]
    int xh = XEiJ.regRn[0];
    int zh = Float.floatToIntBits ((float) Math.exp ((double) Float.intBitsToFloat (xh)));
    if (FPK_FPCP_NAN && zh == 0x7fc00000) {
      zh = 0x7fffffff;
    }
    XEiJ.regRn[0] = zh;
    XEiJ.regCCR = (zh & 0x7fc00000) == 0x7fc00000 && (xh & 0x7fc00000) != 0x7fc00000 ? XEiJ.REG_CCR_C : 0;  //結果が±Inf,NaNだが引数が±Inf,NaNでなかったときはエラー
  }  //fpkFEXP()

  //fpkFSQR ()
  //  $FE6C  __FSQR
  //  32bit浮動小数点数の平方根
  //  <d0.s:32bit浮動小数点数。x
  //  >d0.s:32bit浮動小数点数。sqrt(x)
  //  >ccr:cs=x<0
  public static void fpkFSQR () {
    //sqrt([int]→float→double)→float→[int]
    float xf = Float.intBitsToFloat (XEiJ.regRn[0]);
    int h = Float.floatToIntBits ((float) Math.sqrt ((double) xf));
    if (FPK_FPCP_NAN && h == 0x7fc00000) {
      h = 0x7fffffff;
    }
    XEiJ.regRn[0] = h;
    XEiJ.regCCR = xf < 0.0F ? XEiJ.REG_CCR_C : 0;  //NaNはエラーなし
  }  //fpkFSQR()

  //fpkFPI ()
  //  $FE6D  __FPI
  //  32bit浮動小数点数の円周率
  //  >d0.s:32bit浮動小数点数。pi
  public static void fpkFPI () {
    //pi→float→[int]
    if (true) {
      XEiJ.regRn[0] = 0x40490fdb;
    } else {
      XEiJ.regRn[0] = Float.floatToIntBits ((float) Math.PI);
    }
  }  //fpkFPI()

  //fpkFNPI ()
  //  $FE6E  __FNPI
  //  32bit浮動小数点数の円周率倍
  //  <d0.s:32bit浮動小数点数。x
  //  >d0.s:32bit浮動小数点数。x*pi
  //  >ccr:cs=オーバーフロー
  public static void fpkFNPI () {
    //([int]→float→double)*pi→float→[int]
    int xh = XEiJ.regRn[0];
    //  倍精度のπを倍精度で掛けて単精度に丸める
    int zh = Float.floatToIntBits ((float) ((double) Float.intBitsToFloat (xh) * Math.PI));
    if (FPK_FPCP_NAN && zh == 0x7fc00000) {
      zh = 0x7fffffff;
    }
    XEiJ.regRn[0] = zh;
    XEiJ.regCCR = (zh & 0x7fc00000) == 0x7fc00000 && (xh & 0x7fc00000) != 0x7fc00000 ? XEiJ.REG_CCR_C : 0;  //結果が±Inf,NaNだが引数が±Inf,NaNでなかったときはエラー
  }  //fpkFNPI()

  //fpkFPOWER ()
  //  $FE6F  __FPOWER
  //  32bit浮動小数点数の累乗
  //  <d0.s:32bit浮動小数点数。x
  //  <d1.s:32bit浮動小数点数。y
  //  >d0.s:32bit浮動小数点数。pow(x,y)
  //  >ccr:cs=オーバーフロー
  public static void fpkFPOWER () {
    //pow([int]→float→double,[int]→float→double)→float→[int]
    float xf = Float.intBitsToFloat (XEiJ.regRn[0]);
    float yf = Float.intBitsToFloat (XEiJ.regRn[1]);
    float zf = (float) Math.pow ((double) xf, (double) yf);
    int zh = Float.floatToIntBits (zf);
    if (FPK_FPCP_NAN && zh == 0x7fc00000) {
      zh = 0x7fffffff;
    }
    XEiJ.regRn[0] = zh;
    XEiJ.regCCR = (Float.isNaN (xf) || Float.isNaN (yf) ? 0 :  //引数がNaN
           Float.isNaN (zf) ? XEiJ.REG_CCR_C :  //引数がNaNでないのに結果がNaNのときはエラー
           Float.isInfinite (xf) || Float.isInfinite (yf) ? 0 :  //引数が±Inf
           Float.isNaN (zf) ? XEiJ.REG_CCR_V | XEiJ.REG_CCR_C :  //引数が±Infでないのに結果が±Infのときはオーバーフロー
           0);
  }  //fpkFPOWER()

  //fpkFRND ()
  //  $FE70  __FRND
  //  32bit浮動小数点数の乱数
  //  >d0.s:32bit浮動小数点数。rnd()。0以上1未満
  public static void fpkFRND () {
    //FRND()=DTOF(RND())
    long l = 0xffffffffL & fpkRndLong ();  //下位
    l |= (0x001fffffL & fpkRndLong ()) << 32;  //上位。順序に注意
    //  ここでl==0LのときLong.numberOfLeadingZeros(z)==64なので乱数の値は2^-54になる
    //  FLOATn.Xはその場合を考慮しておらず0のときも先頭の1のビットを探し続けて無限ループに陥ると思われる
    //  実際に0が53ビット並ぶことはないかも知れない
    int o = Long.numberOfLeadingZeros (l) - 11;
    l = ((long) (1022 - 1 - o) << 52) + (l << o);  //指数部を1減らしておいてbit52に移動した仮数部の先頭の1を加えている
    XEiJ.regRn[0] = Float.floatToIntBits ((float) Double.longBitsToDouble (l));
  }  //fpkFRND()

  //fpkFSINH ()
  //  $FE71  __FSINH
  //  32bit浮動小数点数の双曲線正弦
  //  <d0.s:32bit浮動小数点数。x
  //  >d0.s:32bit浮動小数点数。sinh(x)
  //  >ccr:cs=エラー,vs=オーバーフロー
  public static void fpkFSINH () {
    //sinh([int]→float→double)→float→[int]
    float xf = Float.intBitsToFloat (XEiJ.regRn[0]);
    float zf = (float) Math.sinh ((double) xf);
    int zh = Float.floatToIntBits (zf);
    if (FPK_FPCP_NAN && zh == 0x7fc00000) {
      zh = 0x7fffffff;
    }
    XEiJ.regRn[0] = zh;
    XEiJ.regCCR = (Float.isNaN (xf) ? 0 :  //引数がNaN
           Float.isNaN (zf) ? XEiJ.REG_CCR_C :  //引数がNaNでないのに結果がNaNのときはエラー
           Float.isInfinite (xf) ? 0 :  //引数が±Inf
           Float.isNaN (zf) ? XEiJ.REG_CCR_V | XEiJ.REG_CCR_C :  //引数が±Infでないのに結果が±Infのときはオーバーフロー
           0);
  }  //fpkFSINH()

  //fpkFCOSH ()
  //  $FE72  __FCOSH
  //  32bit浮動小数点数の双曲線余弦
  //  <d0.s:32bit浮動小数点数。x
  //  >d0.s:32bit浮動小数点数。cosh(x)
  //  >ccr:cs=エラー,vs=オーバーフロー
  public static void fpkFCOSH () {
    //cosh([int]→float→double)→float→[int]
    float xf = Float.intBitsToFloat (XEiJ.regRn[0]);
    float zf = (float) Math.cosh ((double) xf);
    int zh = Float.floatToIntBits (zf);
    if (FPK_FPCP_NAN && zh == 0x7fc00000) {
      zh = 0x7fffffff;
    }
    XEiJ.regRn[0] = zh;
    XEiJ.regCCR = (Float.isNaN (xf) ? 0 :  //引数がNaN
           Float.isNaN (zf) ? XEiJ.REG_CCR_C :  //引数がNaNでないのに結果がNaNのときはエラー
           Float.isInfinite (xf) ? 0 :  //引数が±Inf
           Float.isNaN (zf) ? XEiJ.REG_CCR_V | XEiJ.REG_CCR_C :  //引数が±Infでないのに結果が±Infのときはオーバーフロー
           0);
  }  //fpkFCOSH()

  //fpkFTANH ()
  //  $FE73  __FTANH
  //  32bit浮動小数点数の双曲線正接
  //  <d0.s:32bit浮動小数点数。x
  //  >d0.s:32bit浮動小数点数。tanh(x)
  public static void fpkFTANH () {
    //tanh([int]→float→double)→float→[int]
    int h = Float.floatToIntBits ((float) Math.tanh ((double) Float.intBitsToFloat (XEiJ.regRn[0])));
    if (FPK_FPCP_NAN && h == 0x7fc00000) {
      h = 0x7fffffff;
    }
    XEiJ.regRn[0] = h;
  }  //fpkFTANH()

  //fpkFATANH ()
  //  $FE74  __FATANH
  //  32bit浮動小数点数の逆双曲線正接
  //  <d0.s:32bit浮動小数点数。x
  //  >d0.s:32bit浮動小数点数。atanh(x)
  //  >ccr:cs=x<=-1||1<=x
  public static void fpkFATANH () {
    //atanh([int]→float→double)→float→[int]
    double d = (double) Float.intBitsToFloat (XEiJ.regRn[0]);
    double s = Math.signum (d);
    double a = Math.abs (d);
    if (a < 1.0) {
      d = s * (Math.log1p (a) - Math.log1p (-a)) * 0.5;  //Math.atanh(double)がない
      XEiJ.regCCR = 0;
    } else if (a == 1.0) {
      d = s * Double.POSITIVE_INFINITY;
      XEiJ.regCCR = XEiJ.REG_CCR_C;
    } else {
      d = Double.NaN;
      XEiJ.regCCR = XEiJ.REG_CCR_C;
    }
    int h = Float.floatToIntBits ((float) d);
    if (FPK_FPCP_NAN && h == 0x7fc00000) {
      h = 0x7fffffff;
    }
    XEiJ.regRn[0] = h;
  }  //fpkFATANH()

  //fpkFASIN ()
  //  $FE75  __FASIN
  //  32bit浮動小数点数の逆正弦
  //  <d0.s:32bit浮動小数点数。x
  //  >d0.s:32bit浮動小数点数。asin(x)
  //  >ccr:cs=x<-1||1<x
  public static void fpkFASIN () {
    //asin([int]→float→double)→float→[int]
    double d = (double) Float.intBitsToFloat (XEiJ.regRn[0]);
    if (d < -1.0 || 1.0 < d) {  //定義域の外。±1,NaNを含まない
      d = Double.NaN;
      XEiJ.regCCR = XEiJ.REG_CCR_C;
    } else {  //定義域の中。±1,NaNを含む
      d = Math.asin (d);
      XEiJ.regCCR = 0;
    }
    int h = Float.floatToIntBits ((float) d);
    if (FPK_FPCP_NAN && h == 0x7fc00000) {
      h = 0x7fffffff;
    }
    XEiJ.regRn[0] = h;
  }  //fpkFASIN()

  //fpkFACOS ()
  //  $FE76  __FACOS
  //  32bit浮動小数点数の逆余弦
  //  <d0.s:32bit浮動小数点数。x
  //  >d0.s:32bit浮動小数点数。acos(x)
  //  >ccr:cs=x<-1||1<x
  public static void fpkFACOS () {
    //acos([int]→float→double)→float→[int]
    double d = (double) Float.intBitsToFloat (XEiJ.regRn[0]);
    if (d < -1.0 || 1.0 < d) {  //定義域の外。±1,NaNを含まない
      d = Double.NaN;
      XEiJ.regCCR = XEiJ.REG_CCR_C;
    } else {  //定義域の中。±1,NaNを含む
      d = Math.acos (d);
      XEiJ.regCCR = 0;
    }
    int h = Float.floatToIntBits ((float) d);
    if (FPK_FPCP_NAN && h == 0x7fc00000) {
      h = 0x7fffffff;
    }
    XEiJ.regRn[0] = h;
  }  //fpkFACOS()

  //fpkFLOG10 ()
  //  $FE77  __FLOG10
  //  32bit浮動小数点数の常用対数
  //  <d0.s:32bit浮動小数点数。x
  //  >d0.s:32bit浮動小数点数。log10(x)
  //  >ccr:0=エラーなし,C=x<0,Z|C=x==0
  public static void fpkFLOG10 () {
    //log10([int]→float→double)→float→[int]
    double xd = (double) Float.intBitsToFloat (XEiJ.regRn[0]);
    double zd = Math.log10 (xd);
    int h = Float.floatToIntBits ((float) zd);
    if (FPK_FPCP_NAN && h == 0x7fc00000) {
      h = 0x7fffffff;
    }
    XEiJ.regRn[0] = h;
    XEiJ.regCCR = (Double.isNaN (xd) ? 0 :  //引数がNaN
           Double.isNaN (zd) ? XEiJ.REG_CCR_C :  //引数がNaNでないのに結果がNaNのときはエラー。log10(-x)=NaN
           Double.isInfinite (xd) ? 0 :  //引数が±Inf
           Double.isInfinite (zd) ? XEiJ.REG_CCR_Z | XEiJ.REG_CCR_C :  //引数が±Infでないのに結果が±Infのときはエラー。log10(±0)=-Inf
           0);
  }  //fpkFLOG10()

  //fpkFLOG2 ()
  //  $FE78  __FLOG2
  //  32bit浮動小数点数の二進対数
  //  <d0.s:32bit浮動小数点数。x
  //  >d0.s:32bit浮動小数点数。log2(x)
  //  >ccr:0=エラーなし,C=x<0,Z|C=x==0
  public static void fpkFLOG2 () {
    //log2([int]→float→double)→float→[int]
    double xd = (double) Float.intBitsToFloat (XEiJ.regRn[0]);
    double zd = Math.log (xd) / 0.69314718055994530941723212146;  //log(2)。Math.log2(double)がない
    int h = Float.floatToIntBits ((float) zd);
    if (FPK_FPCP_NAN && h == 0x7fc00000) {
      h = 0x7fffffff;
    }
    XEiJ.regRn[0] = h;
    XEiJ.regCCR = (Double.isNaN (xd) ? 0 :  //引数がNaN
           Double.isNaN (zd) ? XEiJ.REG_CCR_C :  //引数がNaNでないのに結果がNaNのときはエラー。log2(-x)=NaN
           Double.isInfinite (xd) ? 0 :  //引数が±Inf
           Double.isInfinite (zd) ? XEiJ.REG_CCR_Z | XEiJ.REG_CCR_C :  //引数が±Infでないのに結果が±Infのときはエラー。log2(±0)=-Inf
           0);
  }  //fpkFLOG2()

  //fpkFFREXP ()
  //  $FE79  __FFREXP
  //  32bit浮動小数点数の分解
  //  <d0.s:32bit浮動小数点数。x
  //  >d0.s:xの指数部を0にした値。x==0のときは0
  //  >d1.l:xの指数部。x==0のときは0
  public static void fpkFFREXP () {
    int h = XEiJ.regRn[0];
    if (h << 1 == 0) {  //0のとき
      XEiJ.regRn[0] = 0;
      XEiJ.regRn[1] = 0;
    } else {  //0でないとき
      XEiJ.regRn[0] = (h & 0x807fffff) | 0x3f800000;  //符号と仮数部を残して指数部を0にする
      XEiJ.regRn[1] = (h >>> 23 & 255) - 0x7f;
    }
  }  //fpkFFREXP()

  //fpkFLDEXP ()
  //  $FE7A  __FLDEXP
  //  32bit浮動小数点数の合成
  //  <d0.s:32bit浮動小数点数。x
  //  <d1.l:xの指数部に加える値。y
  //  >d0.s:xの指数部にyを加えた値。x==0のときは0。エラーのときは変化しない
  //  >ccr:0=エラーなし,C=指数部が範囲外
  public static void fpkFLDEXP () {
    int h = XEiJ.regRn[0];
    if (h << 1 == 0) {  //0のとき
      XEiJ.regRn[0] = 0;
      XEiJ.regCCR = 0;
    } else {  //0でないとき
      int t = (h >>> 23 & 255) + XEiJ.regRn[2];
      if ((t & ~0xff) != 0) {  //指数部が範囲外
        XEiJ.regCCR = XEiJ.REG_CCR_C;
      } else {
        XEiJ.regRn[0] = (h & 0x807fffff) | t << 23;  //符号と仮数部を残して指数部を交換する
        XEiJ.regCCR = 0;
      }
    }
  }  //fpkFLDEXP()

  //fpkFADDONE ()
  //  $FE7B  __FADDONE
  //  32bit浮動小数点数に1を加える
  //  <d0.s:32bit浮動小数点数。x
  //  >d0.s:32bit浮動小数点数。x+1
  public static void fpkFADDONE () {
    //([int]→float)+1→[int]
    float xf = Float.intBitsToFloat (XEiJ.regRn[0]);
    float zf = xf + 1.0F;
    int h = Float.floatToIntBits (zf);
    if (FPK_FPCP_NAN && h == 0x7fc00000) {
      h = 0x7fffffff;
    }
    XEiJ.regRn[0] = h;
    XEiJ.regCCR = Double.isInfinite (zf) && !Float.isInfinite (xf) ? XEiJ.REG_CCR_V | XEiJ.REG_CCR_C : 0;  //結果が±Infだが引数が±Infでないときはオーバーフロー
  }  //fpkFADDONE()

  //fpkFSUBONE ()
  //  $FE7C  __FSUBONE
  //  32bit浮動小数点数から1を引く
  //  <d0.s:32bit浮動小数点数。x
  //  >d0.s:32bit浮動小数点数。x-1
  public static void fpkFSUBONE () {
    //([int]→float)-1→[int]
    float xf = Float.intBitsToFloat (XEiJ.regRn[0]);
    float zf = xf - 1.0F;
    int h = Float.floatToIntBits (zf);
    if (FPK_FPCP_NAN && h == 0x7fc00000) {
      h = 0x7fffffff;
    }
    XEiJ.regRn[0] = h;
    XEiJ.regCCR = Double.isInfinite (zf) && !Float.isInfinite (xf) ? XEiJ.REG_CCR_V | XEiJ.REG_CCR_C : 0;  //結果が±Infだが引数が±Infでないときはオーバーフロー
  }  //fpkFSUBONE()

  //fpkFDIVTWO ()
  //  $FE7D  __FDIVTWO
  //  32bit浮動小数点数を2で割る
  //  <d0.s:32bit浮動小数点数。x
  //  >d0.s:32bit浮動小数点数。x/2
  //  >ccr:0=エラーなし,C=アンダーフロー
  public static void fpkFDIVTWO () {
    //([int]→float)/2→[int]
    float xf = Float.intBitsToFloat (XEiJ.regRn[0]);
    float zf = xf * 0.5F;
    int h = Float.floatToIntBits (zf);
    if (FPK_FPCP_NAN && h == 0x7fc00000) {
      h = 0x7fffffff;
    }
    XEiJ.regRn[0] = h;
    XEiJ.regCCR = zf == 0.0F && xf != 0.0F ? XEiJ.REG_CCR_C : 0;  //結果が±0だが引数が±0でないときはアンダーフロー
  }  //fpkFDIVTWO()

  //fpkFIEECNV ()
  //  $FE7E  __FIEECNV
  //  32bit浮動小数点数をIEEEフォーマットに変換する(FLOAT1.X以外は何もしない)
  //  <d0.s:32bit浮動小数点数。x
  //  >d0.s:32bit浮動小数点数。x
  public static void fpkFIEECNV () {
  }  //fpkFIEECNV()

  //fpkIEEFCNV ()
  //  $FE7F  __IEEFCNV
  //  32bit浮動小数点数をIEEEフォーマットから変換する(FLOAT1.X以外は何もしない)
  //  <d0.s:32bit浮動小数点数。x
  //  >d0.s:32bit浮動小数点数。x
  public static void fpkIEEFCNV () {
  }  //fpkIEEFCNV()

  //fpkFEVARG ()
  //  $FEFE  __FEVARG
  //  バージョン確認
  //  >d0.l:'HS86'($48533836)=FLOAT1.X,'IEEE'($49454545)=FLOAT2.X/FLOAT3.X/FLOAT4.X
  //  >d1.l:'SOFT'($534F4654)=FLOAT1.X/FLOAT2.X,'FPCP'($46504350)=FLOAT3.X,'FP20'($46503230)=FLOAT4.X
  public static void fpkFEVARG () {
    XEiJ.regRn[0] = 'I' << 24 | 'E' << 16 | 'E' << 8 | 'E';
    XEiJ.regRn[1] = 'X' << 24 | 'E' << 16 | 'i' << 8 | 'J';
  }  //fpkFEVARG()

  //fpkFEVECS ()
  //  $FEFF  __FEVECS
  //  ベクタ設定
  //  メモ
  //    FLOATn.Xに処理させる
  //    登録してもFEファンクション命令をOFFにしなければ既存のFEファンクション命令に上書きすることはできない
  //  <d0.l:FEファンクションコール番号。$FE00～$FEFF
  //  <a0.l:新アドレス
  //  >d0.l:旧アドレス。-1=エラー
  public static void fpkFEVECS () {
    XEiJ.regRn[0] = -1;
  }  //fpkFEVECS()



  //fpkSTOL ()
  //  $FE10  __STOL
  //  10進数の文字列を32bit符号あり整数に変換する
  //  /^[ \t]*[-+]?[0-9]+/
  //  先頭の'\t'と' 'を読み飛ばす
  //  <a0.l:10進数の文字列の先頭
  //  >d0.l:32bit符号あり整数
  //  >a0.l:10進数の文字列の直後('\0'とは限らない)
  //  >ccr:0=エラーなし,N|C=文法エラー,V|C=オーバーフロー
  public static void fpkSTOL () throws M68kException {
    int a = XEiJ.regRn[8];  //a0
    int c = XEiJ.busRbz (a);
    while (c == ' ' || c == '\t') {
      c = XEiJ.busRbz (++a);
    }
    int n = '7';  //'7'=正,'8'=負
    if (c == '-') {  //負
      n = '8';
      c = XEiJ.busRbz (++a);
    } else if (c == '+') {  //正
      c = XEiJ.busRbz (++a);
    }
    if (!('0' <= c && c <= '9')) {  //数字が1つもない
      XEiJ.regRn[8] = a;  //a0
      XEiJ.regCCR = XEiJ.REG_CCR_N | XEiJ.REG_CCR_C;
      return;
    }
    int x = c - '0';  //値
    for (c = XEiJ.busRbz (++a); '0' <= c && c <= '9'; c = XEiJ.busRbz (++a)) {
      if (214748364 < x || x == 214748364 && n < c) {  //正のとき2147483647、負のとき2147483648より大きくなるときオーバーフロー
        XEiJ.regRn[8] = a;  //a0
        XEiJ.regCCR = XEiJ.REG_CCR_V | XEiJ.REG_CCR_C;
        return;
      }
      x = x * 10 + (c - '0');
    }
    if (n != '7') {  //負
      x = -x;
    }
    XEiJ.regRn[0] = x;  //d0
    XEiJ.regRn[8] = a;  //a0
    XEiJ.regCCR = 0;
  }  //fpkSTOL()

  //fpkLTOS ()
  //  $FE11  __LTOS
  //  32bit符号あり整数を10進数の文字列に変換する
  //  /^-?[1-9][0-9]*$/
  //  <d0.l:32bit符号あり整数
  //  <a0.l:文字列バッファの先頭
  //  >a0.l:10進数の文字列の直後('\0'の位置)
  public static void fpkLTOS () throws M68kException {
    int x = XEiJ.regRn[0];  //d0
    int a = XEiJ.regRn[8];  //a0
    if (x < 0) {  //負
      XEiJ.busWb (a++, '-');
      x = -x;
    }
    long t = XEiJ.fmtBcd12 (0xffffffffL & x);  //符号は取り除いてあるがx=0x80000000の場合があるので(long)xは不可
    XEiJ.regRn[8] = a += Math.max (1, 67 - Long.numberOfLeadingZeros (t) >> 2);  //a0
    XEiJ.busWb (a, 0);
    do {
      XEiJ.busWb (--a, '0' | (int) t & 15);
    } while ((t >>>= 4) != 0L);
  }  //fpkLTOS()

  //fpkSTOH ()
  //  $FE12  __STOH
  //  16進数の文字列を32bit符号なし整数に変換する
  //  /^[0-9A-Fa-f]+/
  //  <a0.l:16進数の文字列の先頭
  //  >d0.l:32bit符号なし整数
  //  >a0.l:16進数の文字列の直後('\0'とは限らない)
  //  >ccr:0=エラーなし,N|C=文法エラー,V|C=オーバーフロー
  public static void fpkSTOH () throws M68kException {
    int a = XEiJ.regRn[8];  //a0
    int c = XEiJ.busRbz (a);
    if (!('0' <= c && c <= '9' || 'A' <= c && c <= 'F' || 'a' <= c && c <= 'f')) {  //数字が1つもない
      XEiJ.regRn[8] = a;  //a0
      XEiJ.regCCR = XEiJ.REG_CCR_N | XEiJ.REG_CCR_C;
      return;
    }
    int x = c <= '9' ? c - '0' : c <= 'F' ? c - ('A' - 10) : c - ('a' - 10);  //値
    for (c = XEiJ.busRbz (++a); '0' <= c && c <= '9' || 'A' <= c && c <= 'F' || 'a' <= c && c <= 'f'; c = XEiJ.busRbz (++a)) {
      if (0x0fffffff < x) {  //0xffffffffより大きくなるときオーバーフロー
        XEiJ.regRn[8] = a;  //a0
        XEiJ.regCCR = XEiJ.REG_CCR_V | XEiJ.REG_CCR_C;
        return;
      }
      x = x << 4 | (c <= '9' ? c - '0' : c <= 'F' ? c - ('A' - 10) : c - ('a' - 10));
    }
    XEiJ.regRn[0] = x;  //d0
    XEiJ.regRn[8] = a;  //a0
    XEiJ.regCCR = 0;
  }  //fpkSTOH()

  //fpkHTOS ()
  //  $FE13  __HTOS
  //  32bit符号なし整数を16進数の文字列に変換する
  //  /^[1-9A-F][0-9A-F]*$/
  //  <d0.l:32bit符号なし整数
  //  <a0.l:文字列バッファの先頭
  //  >a0.l:16進数の文字列の直後('\0'の位置)
  public static void fpkHTOS () throws M68kException {
    int x = XEiJ.regRn[0];  //d0
    int a = XEiJ.regRn[8] += Math.max (1, 35 - Integer.numberOfLeadingZeros (x) >> 2);  //a0
    XEiJ.busWb (a, 0);
    do {
      int t = x & 15;
      //     t             00 01 02 03 04 05 06 07 08 09 0a 0b 0c 0d 0e 0f
      //   9-t             09 08 07 06 05 04 03 02 01 00 ff fe fd fc fb fa
      //   9-t>>4          00 00 00 00 00 00 00 00 00 00 ff ff ff ff ff ff
      //   9-t>>4&7        00 00 00 00 00 00 00 00 00 00 07 07 07 07 07 07
      //   9-t>>4&7|48     30 30 30 30 30 30 30 30 30 30 37 37 37 37 37 37
      //  (9-t>>4&7|48)+t  30 31 32 33 34 35 36 37 38 39 41 42 43 44 45 46
      //                    0  1  2  3  4  5  6  7  8  9  A  B  C  D  E  F
      XEiJ.busWb (--a, (9 - t >> 4 & 7 | 48) + t);
    } while ((x >>>= 4) != 0);
  }  //fpkHTOS()

  //fpkSTOO ()
  //  $FE14  __STOO
  //  8進数の文字列を32bit符号なし整数に変換する
  //  /^[0-7]+/
  //  <a0.l:8進数の文字列の先頭
  //  >d0.l:32bit符号なし整数
  //  >a0.l:8進数の文字列の直後('\0'とは限らない)
  //  >ccr:0=エラーなし,N|C=文法エラー,V|C=オーバーフロー
  public static void fpkSTOO () throws M68kException {
    int a = XEiJ.regRn[8];  //a0
    int c = XEiJ.busRbz (a);
    if (!('0' <= c && c <= '7')) {  //数字が1つもない
      XEiJ.regRn[8] = a;  //a0
      XEiJ.regCCR = XEiJ.REG_CCR_N | XEiJ.REG_CCR_C;
      return;
    }
    int x = c - '0';  //値
    for (c = XEiJ.busRbz (++a); '0' <= c && c <= '7'; c = XEiJ.busRbz (++a)) {
      if (0x1fffffff < x) {  //0xffffffffより大きくなるときオーバーフロー
        XEiJ.regRn[8] = a;  //a0
        XEiJ.regCCR = XEiJ.REG_CCR_V | XEiJ.REG_CCR_C;
        return;
      }
      x = x << 3 | c & 7;
    }
    XEiJ.regRn[0] = x;  //d0
    XEiJ.regRn[8] = a;  //a0
    XEiJ.regCCR = 0;
  }  //fpkSTOO()

  //fpkOTOS ()
  //  $FE15  __OTOS
  //  32bit符号なし整数を8進数の文字列に変換する
  //  /^[1-7][0-7]*$/
  //  <d0.l:32bit符号なし整数
  //  <a0.l:文字列バッファの先頭
  //  >a0.l:8進数の文字列の直後('\0'の位置)
  public static void fpkOTOS () throws M68kException {
    int x = XEiJ.regRn[0];  //d0
    //perl optdiv.pl 34 3
    //  x/3==x*43>>>7 (0<=x<=127) [34*43==1462]
    int a = XEiJ.regRn[8] += Math.max (1, (34 - Integer.numberOfLeadingZeros (x)) * 43 >>> 7);  //a0
    XEiJ.busWb (a, 0);
    do {
      XEiJ.busWb (--a, '0' | x & 7);
    } while ((x >>>= 3) != 0);
  }  //fpkOTOS()

  //fpkSTOB ()
  //  $FE16  __STOB
  //  2進数の文字列を32bit符号なし整数に変換する
  //  /^[01]+/
  //  <a0.l:2進数の文字列の先頭
  //  >d0.l:32bit符号なし整数
  //  >a0.l:2進数の文字列の直後('\0'とは限らない)
  //  >ccr:0=エラーなし,N|C=文法エラー,V|C=オーバーフロー
  public static void fpkSTOB () throws M68kException {
    int a = XEiJ.regRn[8];  //a0
    int c = XEiJ.busRbz (a);
    if (!('0' <= c && c <= '1')) {  //数字が1つもない
      XEiJ.regRn[8] = a;  //a0
      XEiJ.regCCR = XEiJ.REG_CCR_N | XEiJ.REG_CCR_C;
      return;
    }
    int x = c - '0';  //値
    for (c = XEiJ.busRbz (++a); '0' <= c && c <= '1'; c = XEiJ.busRbz (++a)) {
      if (x < 0) {  //オーバーフロー
        XEiJ.regRn[8] = a;  //a0
        XEiJ.regCCR = XEiJ.REG_CCR_V | XEiJ.REG_CCR_C;
        return;
      }
      x = x << 1 | c & 1;
    }
    XEiJ.regRn[0] = x;  //d0
    XEiJ.regRn[8] = a;  //a0
    XEiJ.regCCR = 0;
  }  //fpkSTOB()

  //fpkBTOS ()
  //  $FE17  __BTOS
  //  32bit符号なし整数を2進数の文字列に変換する
  //  /^1[01]*$/
  //  <d0.l:32bit符号なし整数
  //  <a0.l:文字列バッファの先頭
  //  >a0.l:2進数の文字列の直後('\0'の位置)
  public static void fpkBTOS () throws M68kException {
    int x = XEiJ.regRn[0];  //d0
    int a = XEiJ.regRn[8] += Math.max (1, 32 - Integer.numberOfLeadingZeros (x));  //a0
    XEiJ.busWb (a, 0);
    do {
      XEiJ.busWb (--a, '0' | x & 1);
    } while ((x >>>= 1) != 0);
  }  //fpkBTOS()

  //fpkIUSING ()
  //  $FE18  __IUSING
  //  32bit符号あり整数を文字数を指定して右詰めで10進数の文字列に変換する
  //  /^ *-?[1-9][0-9]*$/
  //  <d0.l:32bit符号あり整数
  //  <d1.b:文字数
  //  <a0.l:文字列バッファの先頭
  //  >a0.l:10進数の文字列の直後('\0'の位置)
  public static void fpkIUSING () throws M68kException {
    int x = XEiJ.regRn[0];  //d0
    int n = 0;  //符号の文字数
    if (x < 0) {  //負
      n = 1;
      x = -x;
    }
    long t = XEiJ.fmtBcd12 (0xffffffffL & x);  //符号は取り除いてあるがx=0x80000000の場合があるので(long)xは不可
    int l = n + Math.max (1, 67 - Long.numberOfLeadingZeros (t) >> 2);  //符号を含めた文字数
    int a = XEiJ.regRn[8];  //a0
    for (int i = (XEiJ.regRn[1] & 255) - l; i > 0; i--) {
      XEiJ.busWb (a++, ' ');
    }
    XEiJ.regRn[8] = a += l;  //a0
    XEiJ.busWb (a, 0);
    do {
      XEiJ.busWb (--a, '0' | (int) t & 15);
    } while ((t >>>= 4) != 0L);
    if (n != 0) {
      XEiJ.busWb (--a, '-');
    }
  }  //fpkIUSING()

  //fpkVAL ()
  //  $FE20  __VAL
  //  文字列を64bit浮動小数点数に変換する
  //  先頭の'\t'と' 'を読み飛ばす
  //  "&B"または"&b"で始まっているときは続きを2進数とみなして__STOBで32bit符号なし整数に変換してから__LTODで64bit浮動小数点数に変換する
  //  "&O"または"&o"で始まっているときは続きを8進数とみなして__STOOで32bit符号なし整数に変換してから__LTODで64bit浮動小数点数に変換する
  //  "&H"または"&h"で始まっているときは続きを16進数とみなして__STOHで32bit符号なし整数に変換してから__LTODで64bit浮動小数点数に変換する
  //  それ以外は__STODと同じ
  //  <a0.l:文字列の先頭
  //  >d0d1.d:64bit浮動小数点数
  //  >d2.l:(先頭が'&'でないとき)65535=64bit浮動小数点数をオーバーフローなしでintに変換できる,0=それ以外
  //  >d3.l:(先頭が'&'でないとき)d2.l==65535のとき64bit浮動小数点数をintに変換した値
  //  >a0.l:変換された文字列の直後('\0'とは限らない)
  //  >ccr:0=エラーなし,N|C=文法エラー,V|C=オーバーフロー
  public static void fpkVAL () throws M68kException {
    int a = XEiJ.regRn[8];  //a0
    //先頭の空白を読み飛ばす
    int c = XEiJ.busRbs (a++);
    while (c == ' ' || c == '\t') {
      c = XEiJ.busRbs (a++);
    }
    if (c == '&') {  //&B,&O,&H
      c = XEiJ.busRbs (a++) & 0xdf;
      XEiJ.regRn[8] = a;  //&?の直後
      if (c == 'B') {
        fpkSTOB ();
        fpkLTOD ();
      } else if (c == 'O') {
        fpkSTOO ();
        fpkLTOD ();
      } else if (c == 'H') {
        fpkSTOH ();
        fpkLTOD ();
      } else {
        XEiJ.regCCR = XEiJ.REG_CCR_N | XEiJ.REG_CCR_C;  //文法エラー
      }
    } else {  //&B,&O,&H以外
      fpkSTOD ();
    }
  }  //fpkVAL()

  //fpkUSING ()
  //  $FE21  __USING
  //  64bit浮動小数点数をアトリビュートを指定して文字列に変換する
  //  メモ
  //    bit1の'\\'とbit4の'+'を両方指定したときは'\\'が右側。先頭に"+\\"を付ける
  //    bit1の'\\'とbit2の','とbit4の'+'は整数部の桁数が足りないとき数字を右にずらして押し込まれる
  //    bit3で指数形式を指示しなければ指数部が極端に大きくても極端に小さくても指数形式にならない
  //    bit3で指数形式を指定したときbit1の'\\'とbit2の','は無効
  //    bit4とbit5とbit6はbit4>bit5>bit6の順位で1つだけ有効
  //    有効数字は14桁で15桁目以降はすべて0
  //    FLOAT2.Xは整数部の0でない最初の数字から256文字目までで打ち切られてしまう
  //    整数部の桁数に余裕があれば左側の空白は出力されるので文字列の全体が常に256バイトに収まるわけではない
  //      using 1234.5 5 0 0    " 1235."
  //      using 1234.5 5 1 0    " 1234.5"
  //      using 1234.5 5 2 0    " 1234.50"
  //      using 1234.5 6 2 1    "**1234.50"
  //      using 1234.5 6 2 2    " \\1234.50"
  //      using 1234.5 6 2 3    "*\\1234.50"
  //      using 1234.5 6 2 4    " 1,234.50"
  //      using 1234.5 4 2 4    "1,234.50"
  //      using 1234.5 4 2 5    "1,234.50"
  //      using 1234.5 4 2 6    "\\1,234.50"
  //      using 1234.5 4 2 7    "\\1,234.50"
  //      using 1234.5 4 2 16   "+1234.50"
  //      using 1234.5 4 2 22   "+\\1,234.50"
  //      using 1234.5 4 2 32   "1234.50+"
  //      using 1234.5 4 2 48   "+1234.50"
  //      using 1234.5 4 2 64   "1234.50 "
  //      using 1234.5 4 2 80   "+1234.50"
  //      using 1234.5 4 2 96   "1234.50+"
  //      using 12345678901234567890 10 1 0      "12345678901235000000.0"
  //      using 12345678901234567890e+10 10 1 0  "123456789012350000000000000000.0"
  //      using 0.3333 0 0 0    "."
  //      using 0.6666 0 0 0    "1."
  //      using 0.6666 0 3 0    ".667"
  //      using 0.6666 3 0 0    "  1."
  //      using 0.3333 0 0 2    "\\."
  //      using 0.3333 0 0 16   "+."
  //      using 0.3333 0 0 18   "+\\."
  //      using 1e-10 3 3 0     "  0.000"
  //    指数形式の出力は不可解で本来の動作ではないように思えるが、
  //    X-BASICのprint using命令が使っているのでFLOAT2.Xに合わせておいた方がよさそう
  //      print using "###.##";1.23         "  1.23"         整数部の桁数は3
  //      print using "+##.##";1.23         " +1.23"         整数部の桁数は3←
  //      print using "###.##^^^^^";1.23    " 12.30E-001"    整数部の桁数は3
  //      print using "+##.##^^^^^";1.23    "+12.30E-001"    整数部の桁数は2←
  //    FLOAT2.Xでは#NANと#INFは4桁の整数のように出力される。末尾に小数点が付くが小数部には何も出力されない
  //      using -#INF 7 3 23     "*-\\#,INF."
  //    FLOAT2.Xで#NANと#INFを指数形式にするとさらに不可解。これはバグと言ってよいと思う
  //      using #INF 10 10 8      " #INFE-005"
  //    ここでは#NANと#INFは整数部と小数点と小数部と指数部の全体を使って右寄せにする
  //  <d0d1.d:64bit浮動小数点数
  //  <d2.l:整数部の桁数
  //  <d3.l:小数部の桁数。-1=小数点を書かない,0=小数点を書く
  //  <d4.l:アトリビュート
  //    bit0  左側を'*'で埋める
  //    bit1  先頭に'\\'を付ける
  //    bit2  整数部を3桁毎に','で区切る
  //    bit3  指数形式
  //    bit4  先頭に符号('+'または'-')を付ける。指数形式でないとき整数部の桁数が1桁増える
  //    bit5  末尾に符号('+'または'-')を付ける
  //    bit6  末尾に符号(' 'または'-')を付ける
  //  <a0.l:文字列バッファの先頭
  //  a0は変化しない
  public static void fpkUSING () throws M68kException {
    fpkUSINGSub ((long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1]);  //64bit浮動小数点数
  }  //fpkUSING()
  public static void fpkUSINGSub (long l) throws M68kException {
    int attr = XEiJ.regRn[4];  //アトリビュート
    int len1 = ((attr & (16 + 8)) == 16 ? 1 : 0) + Math.max (0, XEiJ.regRn[2]);  //整数部の桁数
    int len2 = Math.max (-1, XEiJ.regRn[3]);  //小数部の桁数
    int a = XEiJ.regRn[8];  //文字列バッファの先頭
    boolean exp = (attr & 8) != 0;  //true=指数形式
    int spc = (attr & 1) != 0 ? '*' : ' ';  //先頭の空白を充填する文字
    int yen = (attr & 2) != 0 ? '\\' : 0;  //先頭の'\\'
    int cmm = !exp && (attr & 4) != 0 ? ',' : 0;  //3桁毎に入れる','
    //符号
    int sgn1 = 0;  //先頭の符号
    int sgn2 = 0;  //末尾の符号
    if (l < 0L) {  //負
      if ((attr & 32 + 64) == 0) {  //末尾に符号を付けない
        sgn1 = '-';  //先頭の符号
      } else {  //末尾に符号を付ける
        sgn2 = '-';  //末尾の符号
      }
      l &= 0x7fffffffffffffffL;  //符号bitを消しておく
    } else {  //正
      if ((attr & 16) != 0) {  //先頭に符号('+'または'-')を付ける
        sgn1 = '+';
      } else if ((attr & 16 + 32) == 32) {  //末尾に符号('+'または'-')を付ける
        sgn2 = '+';
      } else if ((attr & 16 + 32 + 64) == 64) {  //末尾に符号(' 'または'-')を付ける
        sgn2 = ' ';
      }
    }
    double x = Double.longBitsToDouble (l);  //絶対値
    int e = (int) (l >>> 52) - 1023;  //指数部。ゲタ0。符号bitは消してあるのでマスクは不要
    l &= 0x000fffffffffffffL;  //仮数部の小数部。正規化数のとき整数部の1が付いていないことに注意
    //±0,±Inf,NaN
    if (e == -1023) {  //±0,非正規化数
      if (l == 0L) {  //±0
        for (int i = len1 - ((sgn1 != 0 ? 1 : 0) +  //先頭の符号
                             (yen != 0 ? 1 : 0) +  //'\\'
                             1  //数字
                             ); 0 < i; i--) {
          XEiJ.busWb (a++, spc);  //空白
        }
        if (sgn1 != 0) {
          XEiJ.busWb (a++, sgn1);  //先頭の符号
        }
        if (yen != 0) {
          XEiJ.busWb (a++, yen);  //'\\'
        }
        if (0 < len1) {
          XEiJ.busWb (a++, '0');  //整数部
        }
        if (0 <= len2) {
          XEiJ.busWb (a++, '.');  //小数点
          for (; 0 < len2; len2--) {
            XEiJ.busWb (a++, '0');  //小数部
          }
        }
        XEiJ.busWb (a, '\0');
        return;
      }
      e -= Long.numberOfLeadingZeros (l) - 12;  //非正規化数の指数部を補正する
    } else if (e == 1024) {  //±Inf,NaN
      for (int i = len1 + 1 + len2 + (exp ? 5 : 0) -  //整数部と小数点と小数部と指数部の全体を使って右寄せにする
           ((sgn1 != 0 ? 1 : 0) +  //先頭の符号
            (yen != 0 ? 1 : 0) +  //'\\'
            4  //文字
            ); 0 < i; i--) {
        XEiJ.busWb (a++, spc);  //空白
      }
      if (sgn1 != 0) {
        XEiJ.busWb (a++, sgn1);  //先頭の符号
      }
      if (yen != 0) {
        XEiJ.busWb (a++, yen);  //'\\'
      }
      XEiJ.busWb (a++, '#');
      if (l == 0L) {  //±Inf
        XEiJ.busWb (a++, 'I');
        XEiJ.busWb (a++, 'N');
        XEiJ.busWb (a++, 'F');
      } else {  //NaN
        XEiJ.busWb (a++, 'N');
        XEiJ.busWb (a++, 'A');
        XEiJ.busWb (a++, 'N');
      }
      XEiJ.busWb (a, '\0');
      return;
    }
    //10進数で表現したときの指数部を求める
    //  10^e<=x<10^(e+1)となるeを求める
    e = (int) Math.floor ((double) e * 0.30102999566398119521373889472);  //log10(2)
    //10^-eを掛けて1<=x<10にする
    //  非正規化数の最小値から正規化数の最大値まで処理できなければならない
    //  10^-eを計算してからまとめて掛ける方法はxが非正規化数のとき10^-eがオーバーフローしてしまうので不可
    //    doubleは非正規化数の逆数を表現できない
    if (0 < e) {  //10<=x
      x *= FPK_TEN_M16QR[e & 15];
      if (16 <= e) {
        x *= FPK_TEN_M16QR[16 + (e >> 4 & 15)];
        if (256 <= e) {
          x *= FPK_TEN_M16QR[33];  //FPK_TEN_M16QR[32 + (e >> 8)]
        }
      }
    } else if (e < 0) {  //x<1
      x *= FPK_TEN_P16QR[-e & 15];
      if (e <= -16) {
        x *= FPK_TEN_P16QR[16 + (-e >> 4 & 15)];
        if (e <= -256) {
          x *= FPK_TEN_P16QR[33];  //FPK_TEN_P16QR[32 + (-e >> 8)]
        }
      }
    }
    //整数部2桁、小数部16桁の10進数に変換する
    //  1<=x<10なのでw[1]が先頭になるはずだが誤差で前後にずれる可能性がある
    int[] w = new int[18];
    {
      int d = (int) x;
      int t = XEiJ.FMT_BCD4[d];
      w[0] = t >> 4;
      w[1] = t      & 15;
      for (int i = 2; i < 18; i += 4) {
        //xを10000倍して整数部dを引くことで小数部を残すが、このとき情報落ちが発生して誤差が蓄積する
        //Double-Doubleの乗算の要領で10000倍を正確に行い、誤差の蓄積を回避する
        //x = (x - (double) d) * 10000.0;
        double xh = x * 0x8000001p0;
        xh += x - xh;  //xの上半分
        x = (xh - (double) d) * 10000.0 + (x - xh) * 10000.0;
        d = (int) x;
        t = XEiJ.FMT_BCD4[d];
        w[i    ] = t >> 12;
        w[i + 1] = t >>  8 & 15;
        w[i + 2] = t >>  4 & 15;
        w[i + 3] = t       & 15;
      }
    }
    //先頭の位置を確認する
    //  w[h]が先頭(0でない最初の数字)の位置
    int h = w[0] != 0 ? 0 : w[1] != 0 ? 1 : 2;
    //14+1桁目を四捨五入する
    int o = h + 14;  //w[o]は四捨五入する桁の位置。w[]の範囲内
    if (5 <= w[o]) {
      int i = o;
      while (10 <= ++w[--i]) {
        w[i] = 0;
      }
      if (i < h) {  //先頭から繰り上がった。このとき新しい先頭は1でそれ以外はすべて0
        h--;  //先頭を左にずらす
        o--;  //末尾を左にずらす
      }
    }
    //先頭の位置に応じて指数部を更新する
    //  w[h]が整数部、w[h+1..13]が小数部。10^eの小数点はw[h]の右側。整数部の桁数はe+1桁
    e -= h - 1;
    //整数部の桁数を調節する
    int ee = !exp ? e : Math.max (0, sgn1 != 0 || sgn2 != 0 ? len1 : len1 - 1) - 1;  //整数部の桁数-1。整数部の桁数はee+1桁。指数部はe-ee
    //小数点以下len2+1桁目が先頭から14+1桁目よりも左側にあるときその桁で改めて四捨五入する
    //  あらかじめ14+1桁目で四捨五入しておかないと、
    //  1.5の5を四捨五入しなければならないときに誤差で1.499…になったまま4を四捨五入しようとして失敗することがある
    int s = h + ee + 1 + Math.max (0, len2);  //w[s]は小数点以下len2+1桁目の位置。w.length<=sの場合があることに注意
    if (s < o) {
      o = s;  //w[o]は四捨五入する桁の位置。o<0の場合があることに注意
      if (0 <= o && 5 <= w[o]) {
        int i = o;
        while (10 <= ++w[--i]) {
          w[i] = 0;
        }
        if (i < h) {  //先頭から繰り上がった。このとき新しい先頭は1でそれ以外はすべて0
          h--;  //先頭を左にずらす
          if (!exp) {  //指数形式でないとき
            ee++;  //左に1桁伸ばす。全体の桁数が1桁増える
          } else {  //指数形式のとき
            e++;  //指数部を1増やす
            o--;  //末尾を左にずらす。全体の桁数は変わらない
          }
        }
      }
    }
    //文字列に変換する
    if (0 <= ee) {  //1<=x
      for (int i = len1 - ((sgn1 != 0 ? 1 : 0) +  //先頭の符号
                           (yen != 0 ? 1 : 0) +  //'\\'
                           (cmm != 0 ? ee / 3 : 0) +  //','
                           ee + 1  //数字
                           ); 0 < i; i--) {
        XEiJ.busWb (a++, spc);  //空白
      }
      if (sgn1 != 0) {
        XEiJ.busWb (a++, sgn1);  //先頭の符号
      }
      if (yen != 0) {
        XEiJ.busWb (a++, yen);  //'\\'
      }
      for (int i = ee; 0 <= i; i--) {
        XEiJ.busWb (a++, h < o ? '0' + w[h] : '0');  //整数部
        h++;
        if (cmm != 0 && 0 < i && i % 3 == 0) {
          XEiJ.busWb (a++, cmm);  //','
        }
      }
      if (0 <= len2) {
        XEiJ.busWb (a++, '.');  //小数点
        for (; 0 < len2; len2--) {
          XEiJ.busWb (a++, h < o ? '0' + w[h] : '0');  //小数部
          h++;
        }
      }
    } else {  //x<1
      for (int i = len1 - ((sgn1 != 0 ? 1 : 0) +  //先頭の符号
                           (yen != 0 ? 1 : 0) +  //'\\'
                           1  //数字
                           ); 0 < i; i--) {
        XEiJ.busWb (a++, spc);  //空白
      }
      if (sgn1 != 0) {
        XEiJ.busWb (a++, sgn1);  //先頭の符号
      }
      if (yen != 0) {
        XEiJ.busWb (a++, yen);  //'\\'
      }
      if (0 < len1) {
        XEiJ.busWb (a++, '0');  //整数部
      }
      if (0 <= len2) {
        XEiJ.busWb (a++, '.');  //小数点
        for (int i = -1 - ee; 0 < len2 && 0 < i; len2--, i--) {
          XEiJ.busWb (a++, '0');  //小数部の先頭の0の並び
        }
        for (; 0 < len2; len2--) {
          XEiJ.busWb (a++, h < o ? '0' + w[h] : '0');  //小数部
          h++;
        }
      }
    }
    if (exp) {
      e -= ee;
      XEiJ.busWb (a++, 'E');  //指数部の始まり
      if (0 <= e) {
        XEiJ.busWb (a++, '+');  //指数部の正符号。省略しない
      } else {
        XEiJ.busWb (a++, '-');  //指数部の負符号
        e = -e;
      }
      e = XEiJ.FMT_BCD4[e];
      XEiJ.busWb (a++, '0' + (e >> 8     ));  //指数部の100の位。0でも省略しない
      XEiJ.busWb (a++, '0' + (e >> 4 & 15));  //指数部の10の位
      XEiJ.busWb (a++, '0' + (e      & 15));  //指数部の1の位
    }
    if (sgn2 != 0) {
      XEiJ.busWb (a++, sgn2);  //末尾の符号
    }
    XEiJ.busWb (a, '\0');
  }  //fpkUSINGSub0(long)

  //fpkSTOD ()
  //  $FE22  __STOD
  //  文字列を64bit浮動小数点数に変換する
  //  先頭の'\t'と' 'を読み飛ばす
  //  "#INF"は無限大、"#NAN"は非数とみなす
  //  バグ
  //    FLOAT2.X 2.02/2.03は誤差が大きい
  //      "1.7976931348623E+308"=0x7fefffffffffffb0が0x7fefffffffffffb3になる
  //      "1.5707963267949"=0x3ff921fb54442d28が0x3ff921fb54442d26になる
  //      "4.9406564584125E-324"(非正規化数の最小値よりもわずかに大きい)がエラーになる
  //    FLOAT2.X 2.02/2.03は"-0"が+0になる
  //    FLOAT4.X 1.02は"-0"が+0になる(実機で確認済み)
  //    FLOAT2.X 2.02/2.03は"-#INF"が+Infになる
  //      print val("-#INF")で再現できる
  //      '-'を符号として解釈しておきながら結果の無限大に符号を付けるのを忘れている
  //    FLOAT2.X 2.02/2.03は".#INF"が+Infになる
  //      print val(".#INF")で再現できる
  //    FLOAT4.X 1.02は"#NAN","#INF","-#INF"を読み取ったときa0が文字列の直後ではなく最後の文字を指している
  //  <a0.l:文字列の先頭
  //  >d0d1.d:64bit浮動小数点数
  //  >d2.l:65535=64bit浮動小数点数をオーバーフローなしでintに変換できる,0=それ以外
  //  >d3.l:d2.l==65535のとき64bit浮動小数点数をintに変換した値
  //  >a0.l:変換された文字列の直後('\0'とは限らない)
  //  >ccr:0=エラーなし,N|C=文法エラー,V|C=オーバーフロー
  public static void fpkSTOD () throws M68kException {
    long l = Double.doubleToLongBits (fpkSTODSub ());
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.regRn[0] = (int) (l >> 32);  //d0
    XEiJ.regRn[1] = (int) l;  //d1
  }  //fpkSTOD()
  public static double fpkSTODSub () throws M68kException {
    int a = XEiJ.regRn[8];  //a0
    //先頭の空白を読み飛ばす
    int c = XEiJ.busRbs (a);
    while (c == ' ' || c == '\t') {
      c = XEiJ.busRbs (++a);
    }
    //符号を読み取る
    double s = 1.0;  //仮数部の符号
    if (c == '+') {
      c = XEiJ.busRbs (++a);
    } else if (c == '-') {
      s = -s;
      c = XEiJ.busRbs (++a);
    }
    //#NANと#INFを処理する
    if (c == '#') {
      c = XEiJ.busRbs (a + 1);
      if (c == 'N' || c == 'I') {  //小文字は不可
        c = c << 8 | XEiJ.busRbz (a + 2);
        if (c == ('N' << 8 | 'A') || c == ('I' << 8 | 'N')) {
          c = c << 8 | XEiJ.busRbz (a + 3);
          if (c == ('N' << 16 | 'A' << 8 | 'N') || c == ('I' << 16 | 'N' << 8 | 'F')) {
            XEiJ.regRn[2] = 0;  //d2
            XEiJ.regRn[3] = 0;  //d3
            XEiJ.regRn[8] = a + 4;  //a0。"#NAN"または"#INF"のときだけ直後まで進める。それ以外は'#'の位置で止める
            XEiJ.regCCR = 0;  //エラーなし。"#INF"はオーバーフローとみなされない
            return c == ('N' << 16 | 'A' << 8 | 'N') ? Double.NaN : s * Double.POSITIVE_INFINITY;
          }
        }
      }
      XEiJ.regRn[8] = a;  //a0。'#'の位置で止める
      XEiJ.regCCR = XEiJ.REG_CCR_N | XEiJ.REG_CCR_C;  //文法エラー
      return 0.0;
    }  //if c=='#'
    //仮数部を読み取る
    //  数字を1000個並べてからe-1000などと書いてあるとき途中でオーバーフローすると困るので、
    //  多すぎる数字の並びは先頭の有効数字だけ読み取って残りは桁数だけ数えて読み飛ばす
    long u = 0L;  //仮数部
    int n = 0;  //0以外の最初の数字から数えて何桁目か
    int e = 1;  //-小数部の桁数。1=整数部
    if (c == '.') {  //仮数部の先頭が小数点
      e = 0;  //小数部開始
      c = XEiJ.busRbs (++a);
    }
    if (c < '0' || '9' < c) {  //仮数部に数字がない
      XEiJ.regRn[8] = a;  //a0
      XEiJ.regCCR = XEiJ.REG_CCR_N | XEiJ.REG_CCR_C;  //文法エラー
      return 0.0;
    }
    double x = 0.0;
    do {
      if (0 < n || '0' < c) {  //0以外
        n++;  //0以外の最初の数字から数えて何桁目か
      }
      if (e <= 0 && n <= 18) {  //小数部で18桁目まで
        e--;  //-小数部の桁数
      }
      if (0 < n && n <= 18) {  //1桁目から18桁目まで
        u = u * 10L + (long) (c - '0');
      }
      c = XEiJ.busRbs (++a);
      if (0 < e && c == '.') {  //整数部で小数点が出てきた
        e = 0;  //小数部開始
        c = XEiJ.busRbs (++a);
      }
    } while ('0' <= c && c <= '9');
    if (0 < e) {  //小数点が出てこなかった
      e = 18 < n ? n - 18 : 0;  //整数部を読み飛ばした桁数が(-小数部の桁数)
    }
    //  1<=u<10^18  整数なので誤差はない
    //  0<e   小数点がなくて整数部が19桁以上あって末尾を読み飛ばした
    //  e==0  小数点がなくて整数部が18桁以内で末尾を読み飛ばさなかった
    //        小数点があって小数点で終わっていた
    //  e<0   小数点があって小数部が1桁以上あった
    //指数部を読み取る
    if (c == 'E' || c == 'e') {
      c = XEiJ.busRbs (++a);
      int t = 1;  //指数部の符号
      if (c == '+') {
        c = XEiJ.busRbs (++a);
      } else if (c == '-') {
        t = -t;
        c = XEiJ.busRbs (++a);
      }
      if (c < '0' || '9' < c) {  //指数部に数字がない
        XEiJ.regRn[8] = a;  //a0
        XEiJ.regCCR = XEiJ.REG_CCR_N | XEiJ.REG_CCR_C;  //文法エラー
        return 0.0;
      }
      while (c == '0') {  //先頭の0を読み飛ばす
        c = XEiJ.busRbs (++a);
      }
      int p = 0;
      for (int j = 0; '0' <= c && c <= '9' && j < 9; j++) {  //0以外の数字が出てきてから最大で9桁目まで読み取る。Human68kの環境では数字を1GBも並べることはできないのでオーバーフローの判定には9桁あれば十分
        p = p * 10 + (c - '0');
        c = XEiJ.busRbs (++a);
      }
      e += t * p;
    }
    //符号と仮数部と指数部を合わせる
    //  x=s*x*10^e
    //  1<=u<10^18なのでeが範囲を大きく外れている場合を先に除外する
    if (e < -350) {
      XEiJ.regRn[2] = 65535;  //d2。-1ではない
      XEiJ.regRn[3] = 0;  //d3
      XEiJ.regRn[8] = a;  //a0
      XEiJ.regCCR = 0;  //エラーなし。アンダーフローはエラーとみなされない
      return s < 0.0 ? -0.0 : 0.0;
    }
    if (350 < e) {
      XEiJ.regRn[2] = 0;  //d2
      XEiJ.regRn[3] = 0;  //d3
      XEiJ.regRn[8] = a;  //a0
      XEiJ.regCCR = XEiJ.REG_CCR_V | XEiJ.REG_CCR_C;  //オーバーフロー
      return s * Double.POSITIVE_INFINITY;
    }
    if (true) {
      QFP xx = new QFP (s < 0.0 ? -u : u);  //符号と仮数部
      if (0 < e) {
        xx.mul (QFP.QFP_TEN_P16QR[e & 15]);
        if (16 <= e) {
          xx.mul (QFP.QFP_TEN_P16QR[16 + (e >> 4 & 15)]);
          if (256 <= e) {
            xx.mul (QFP.QFP_TEN_P16QR[33]);
          }
        }
      } else if (e < 0) {
        xx.mul (QFP.QFP_TEN_M16QR[-e & 15]);
        if (e <= -16) {
          xx.mul (QFP.QFP_TEN_M16QR[16 + (-e >> 4 & 15)]);
          if (e <= -256) {
            xx.mul (QFP.QFP_TEN_M16QR[33]);
          }
        }
      }
      x = xx.getd ();
    } else {
      x = s * (double) u;  //符号と仮数部
      if (0 < e) {
        x *= FPK_TEN_P16QR[e & 15];
        if (16 <= e) {
          x *= FPK_TEN_P16QR[16 + (e >> 4 & 15)];
          if (256 <= e) {
            x *= FPK_TEN_P16QR[33];  //FPK_TEN_P16QR[32 + (e >> 8)]
          }
        }
      } else if (e < 0) {
        x /= FPK_TEN_P16QR[-e & 15];
        if (e <= -16) {
          x /= FPK_TEN_P16QR[16 + (-e >> 4 & 15)];
          if (e <= -256) {
            x /= FPK_TEN_P16QR[33];  //FPK_TEN_P16QR[32 + (-e >> 8)]
          }
        }
      }
    }
    if (Double.isInfinite (x)) {
      XEiJ.regRn[8] = a;  //a0
      XEiJ.regCCR = XEiJ.REG_CCR_V | XEiJ.REG_CCR_C;  //オーバーフロー
      return x;
    }
    //  アンダーフローで0になっている場合がある
    if (x == (double) ((int) x)) {  //intで表現できる。+0.0==-0.0==0なので±0.0を含む
      XEiJ.regRn[2] = 65535;  //d2。-1ではない
      XEiJ.regRn[3] = (int) x;  //d3
    } else {  //intで表現できない
      XEiJ.regRn[2] = 0;  //d2
      XEiJ.regRn[3] = 0;  //d3
    }
    XEiJ.regRn[8] = a;  //a0
    XEiJ.regCCR = 0;  //エラーなし
    return x;
  }  //fpkSTODSub()

  //fpkDTOS ()
  //  $FE23  __DTOS
  //  64bit浮動小数点数を文字列に変換する
  //  無限大は"#INF"、非数は"#NAN"になる
  //  指数形式の境目
  //    x<10^-4または10^14<=xのとき指数形式にする
  //    FLOAT2.X/FLOAT4.Xの場合
  //      3f2fffffffffff47  2.4414062499999E-004
  //      3f2fffffffffff48  0.000244140625
  //      42d6bcc41e8fffdf  99999999999999
  //      42d6bcc41e8fffe0  1E+014
  //  <d0d1.d:64bit浮動小数点数
  //  <a0.l:文字列バッファの先頭
  //  >a0.l:末尾の'\0'の位置
  public static void fpkDTOS () throws M68kException {
    fpkDTOSSub ((long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1]);  //64bit浮動小数点数
  }  //fpkDTOS()
  public static void fpkDTOSSub (long l) throws M68kException {
    final int len3 = 14;
    int a = XEiJ.regRn[8];  //文字列バッファの先頭
    //符号と指数部の処理
    //  ±0,±Inf,NaNはここで除外する
    if (l < 0L) {
      XEiJ.busWb (a++, '-');  //負符号
      l &= 0x7fffffffffffffffL;  //符号bitを消しておく
    }
    double x = Double.longBitsToDouble (l);  //絶対値
    int e = (int) (l >>> 52) - 1023;  //指数部。ゲタ0。符号bitは消してあるのでマスクは不要
    l &= 0x000fffffffffffffL;  //仮数部の小数部。正規化数のとき整数部の1が付いていないことに注意
    if (e == -1023) {  //±0,非正規化数
      if (l == 0L) {  //±0
        XEiJ.busWb (a++, '0');  //0
        XEiJ.busWb (a, '\0');
        XEiJ.regRn[8] = a;  //末尾の'\0'の位置
        return;
      }
      e -= Long.numberOfLeadingZeros (l) - 12;  //非正規化数の指数部を補正する
    } else if (e == 1024) {  //±Inf,NaN
      XEiJ.busWb (a++, '#');
      if (l == 0L) {  //±Inf
        XEiJ.busWb (a++, 'I');
        XEiJ.busWb (a++, 'N');
        XEiJ.busWb (a++, 'F');
      } else {  //NaN
        XEiJ.busWb (a++, 'N');
        XEiJ.busWb (a++, 'A');
        XEiJ.busWb (a++, 'N');
      }
      XEiJ.busWb (a, '\0');
      XEiJ.regRn[8] = a;  //末尾の'\0'の位置
      return;
    }
    //10進数で表現したときの指数部を求める
    //  10^e<=x<10^(e+1)となるeを求める
    e = (int) Math.floor ((double) e * 0.30102999566398119521373889472);  //log10(2)
    //10^-eを掛けて1<=x<10にする
    //  非正規化数の最小値から正規化数の最大値まで処理できなければならない
    //  10^-eを計算してからまとめて掛ける方法はxが非正規化数のとき10^-eがオーバーフローしてしまうので不可
    //    doubleは非正規化数の逆数を表現できない
    if (0 < e) {  //10<=x
      x *= FPK_TEN_M16QR[e & 15];
      if (16 <= e) {
        x *= FPK_TEN_M16QR[16 + (e >> 4 & 15)];
        if (256 <= e) {
          x *= FPK_TEN_M16QR[33];  //FPK_TEN_M16QR[32 + (e >> 8)]
        }
      }
    } else if (e < 0) {  //x<1
      x *= FPK_TEN_P16QR[-e & 15];
      if (e <= -16) {
        x *= FPK_TEN_P16QR[16 + (-e >> 4 & 15)];
        if (e <= -256) {
          x *= FPK_TEN_P16QR[33];  //FPK_TEN_P16QR[32 + (-e >> 8)]
        }
      }
    }
    //整数部2桁、小数部16桁の10進数に変換する
    //  1<=x<10なのでw[1]が先頭になるはずだが誤差で前後にずれる可能性がある
    int[] w = new int[18];
    {
      int d = (int) x;
      int t = XEiJ.FMT_BCD4[d];
      w[0] = t >> 4;
      w[1] = t      & 15;
      for (int i = 2; i < 18; i += 4) {
        //xを10000倍して整数部dを引くことで小数部を残すが、このとき情報落ちが発生して誤差が蓄積する
        //Double-Doubleの乗算の要領で10000倍を正確に行い、誤差の蓄積を回避する
        //x = (x - (double) d) * 10000.0;
        double xh = x * 0x8000001p0;
        xh += x - xh;  //xの上半分
        x = (xh - (double) d) * 10000.0 + (x - xh) * 10000.0;
        d = (int) x;
        t = XEiJ.FMT_BCD4[d];
        w[i    ] = t >> 12;
        w[i + 1] = t >>  8 & 15;
        w[i + 2] = t >>  4 & 15;
        w[i + 3] = t       & 15;
      }
    }
    //先頭の位置を確認する
    //  w[h]が先頭(0でない最初の数字)の位置
    int h = w[0] != 0 ? 0 : w[1] != 0 ? 1 : 2;
    //14+1桁目を四捨五入する
    int o = h + 14;  //w[o]は四捨五入する桁の位置。w[]の範囲内
    if (5 <= w[o]) {
      int i = o;
      while (10 <= ++w[--i]) {
        w[i] = 0;
      }
      if (i < h) {  //先頭から繰り上がった。このとき新しい先頭は1でそれ以外はすべて0
        h--;  //先頭を左にずらす
        o--;  //末尾を左にずらす
      }
    }
    //先頭の位置に応じて指数部を更新する
    //  w[h]が整数部、w[h+1..o-1]が小数部。10^eの小数点はw[h]の右側。整数部の桁数はe+1桁
    e -= h - 1;
    //末尾の位置を確認する
    //  w[o-1]が末尾(0でない最後の数字)の位置
    while (w[o - 1] == 0) {  //全体は0ではないので必ず止まる。小数点よりも左側で止まる場合があることに注意
      o--;
    }
    //指数形式にするかどうか選択して文字列に変換する
    if (0 <= e && e < len3) {  //1<=x<10^len3。指数形式にしない
      for (; 0 <= e && h < o; e--) {
        XEiJ.busWb (a++, '0' + w[h++]);  //整数部
      }
      for (; 0 <= e; e--) {
        XEiJ.busWb (a++, '0');  //整数部。末尾の位置に関係なく1の位まで書く
      }
      if (h < o) {  //小数部がある
        XEiJ.busWb (a++, '.');  //小数部があるときだけ小数点を書く
        do {
          XEiJ.busWb (a++, '0' + w[h++]);  //小数部
        } while (h < o);
      }
    } else if (-4 <= e && e < 0) {  //10^-4<=x<1。指数形式にしない
      XEiJ.busWb (a++, '0');  //整数部の0
      XEiJ.busWb (a++, '.');  //小数点
      while (++e < 0) {
        XEiJ.busWb (a++, '0');  //小数部の先頭の0の並び
      }
      do {
        XEiJ.busWb (a++, '0' + w[h++]);  //小数部
      } while (h < o);
    } else {  //x<10^-4または10^len3<=x。指数形式にする
      XEiJ.busWb (a++, '0' + w[h++]);  //整数部
      if (h < o) {  //小数部がある
        XEiJ.busWb (a++, '.');  //小数部があるときだけ小数点を書く
        do {
          XEiJ.busWb (a++, '0' + w[h++]);  //小数部
        } while (h < o);
      }
      XEiJ.busWb (a++, 'E');  //指数部の始まり
      if (0 <= e) {
        XEiJ.busWb (a++, '+');  //指数部の正符号。省略しない
      } else {
        XEiJ.busWb (a++, '-');  //指数部の負符号
        e = -e;
      }
      e = XEiJ.FMT_BCD4[e];
      XEiJ.busWb (a++, '0' + (e >> 8     ));  //指数部の100の位。0でも省略しない
      XEiJ.busWb (a++, '0' + (e >> 4 & 15));  //指数部の10の位
      XEiJ.busWb (a++, '0' + (e      & 15));  //指数部の1の位
    }
    XEiJ.busWb (a, '\0');
    XEiJ.regRn[8] = a;  //末尾の'\0'の位置
  }  //fpkDTOSSub0()

  //fpkECVT ()
  //  $FE24  __ECVT
  //  64bit浮動小数点数を全体の桁数を指定して文字列に変換する
  //  文字列に書くのは仮数部の数字のみ
  //  符号と小数点と指数部は文字列に書かず、小数点の位置と符号をレジスタに入れて返す
  //  桁数は255桁まで指定できるが、有効桁数は14桁まで
  //    有効桁数の次の桁で絶対値を四捨五入する
  //    15桁以上を指定しても14桁に丸められ、15桁目以降はすべて'0'になる
  //  無限大は"#INF"、非数は"#NAN"に変換する
  //    "#INF"と"#NAN"のとき小数点の位置は4になる
  //    "#INF"と"#NAN"で3桁以下のときは途中で打ち切る
  //    "#INF"と"#NAN"で5桁以上のときは5桁目以降はすべて'\0'になる
  //  バグ
  //    FLOATn.Xは"#INF"と"#NAN"で1桁～3桁のとき文字列が"$","$0","$00"になってしまう
  //      文字数が少なすぎて"#INF"や"#NAN"が入り切らないのは仕方がないが、
  //      無意味な"$00"という文字列になるのは数字ではない文字列を四捨五入しようとするバグが原因
  //      例えば3桁のときは4桁目の'F'または'N'が'5'以上なので繰り上げて上の位をインクリメントする
  //      'N'+1='O'または'A'+1='B'が'9'よりも大きいので'0'を上書きして繰り上げて上の位をインクリメントする
  //      'I'+1='J'または'N'+1='O'も'9'よりも大きいので'0'を上書きして繰り上げて上の位をインクリメントする
  //      '#'+1='$'は'9'以下なので"$00"になる
  //      X-BASICでint i2,i3:print ecvt(val("#INF"),3,i2,i3)とすると再現できる
  //    FLOATn.Xは"#NAN"と"#INF"で15桁以上のとき5桁目から14桁目までは'\0'だが15桁目以降に'0'が書き込まれる
  //      通常は5桁目の'\0'で文字列は終了していると見なされるので実害はないが気持ち悪い
  //    FLOAT2.X 2.02/2.03は0のとき小数点の位置が0になる
  //      FLOAT4.X 1.02は0のとき小数点の位置が1になる
  //      ここでは1にしている
  //  <d0d1.d:64bit浮動小数点数
  //  <d2.l:全体の桁数
  //  <a0.l:文字列バッファの先頭。末尾に'\0'を書き込むので桁数+1バイト必要
  //  >d0.l:先頭から小数点の位置までのオフセット
  //  >d1.l:符号(0=+,1=-)
  //  a0.lは変化しない
  public static void fpkECVT () throws M68kException {
    fpkECVTSub ((long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1]);  //64bit浮動小数点数
  }  //fpkECVT()
  public static void fpkECVTSub (long l) throws M68kException {
    int len3 = XEiJ.regRn[2];  //全体の桁数
    int a = XEiJ.regRn[8];  //文字列バッファの先頭
    if (len3 <= 0) {  //全体の桁数が0
      XEiJ.busWb (a, '\0');
      return;
    }
    int b = a + len3;  //文字列バッファの末尾+1。'\0'を書き込む位置
    //符号と指数部の処理
    //  ±0,±Inf,NaNはここで除外する
    if (0L <= l) {
      XEiJ.regRn[1] = 0;  //正符号
    } else {
      XEiJ.regRn[1] = 1;  //負符号
      l &= 0x7fffffffffffffffL;  //符号bitを消しておく
    }
    double x = Double.longBitsToDouble (l);  //絶対値
    int e = (int) (l >>> 52) - 1023;  //指数部。ゲタ0。符号bitは消してあるのでマスクは不要
    l &= 0x000fffffffffffffL;  //仮数部の小数部。正規化数のとき整数部の1が付いていないことに注意
    if (e == -1023) {  //±0,非正規化数
      if (l == 0L) {  //±0
        //指定された全体の桁数だけ'0'を並べる
        while (a < b) {
          XEiJ.busWb (a++, '0');
        }
        XEiJ.busWb (a, '\0');
        XEiJ.regRn[0] = 1;  //小数点の位置
        return;
      }
      e -= Long.numberOfLeadingZeros (l) - 12;  //非正規化数の指数部を補正する
    } else if (e == 1024) {  //±Inf,NaN
      for (int s = l != 0L ? '#' | 'N' << 8 | 'A' << 16 | 'N' << 24 : '#' | 'I' << 8 | 'N' << 16 | 'F' << 24; a < b && s != 0; s >>>= 8) {
        XEiJ.busWb (a++, s);
      }
      while (a < b) {
        XEiJ.busWb (a++, '\0');  //残りは'\0'
      }
      XEiJ.busWb (a, '\0');
      XEiJ.regRn[0] = 4;  //小数点の位置
      return;
    }
    //10進数で表現したときの指数部を求める
    //  10^e<=x<10^(e+1)となるeを求める
    e = (int) Math.floor ((double) e * 0.30102999566398119521373889472);  //log10(2)
    //10^-eを掛けて1<=x<10にする
    //  非正規化数の最小値から正規化数の最大値まで処理できなければならない
    //  10^-eを計算してからまとめて掛ける方法はxが非正規化数のとき10^-eがオーバーフローしてしまうので不可
    //    doubleは非正規化数の逆数を表現できない
    if (0 < e) {  //10<=x
      x *= FPK_TEN_M16QR[e & 15];
      if (16 <= e) {
        x *= FPK_TEN_M16QR[16 + (e >> 4 & 15)];
        if (256 <= e) {
          x *= FPK_TEN_M16QR[33];  //FPK_TEN_M16QR[32 + (e >> 8)]
        }
      }
    } else if (e < 0) {  //x<1
      x *= FPK_TEN_P16QR[-e & 15];
      if (e <= -16) {
        x *= FPK_TEN_P16QR[16 + (-e >> 4 & 15)];
        if (e <= -256) {
          x *= FPK_TEN_P16QR[33];  //FPK_TEN_P16QR[32 + (-e >> 8)]
        }
      }
    }
    //整数部2桁、小数部16桁の10進数に変換する
    //  1<=x<10なのでw[1]が先頭になるはずだが誤差で前後にずれる可能性がある
    int[] w = new int[18];
    {
      int d = (int) x;
      int t = XEiJ.FMT_BCD4[d];
      w[0] = t >> 4;
      w[1] = t      & 15;
      for (int i = 2; i < 18; i += 4) {
        //xを10000倍して整数部dを引くことで小数部を残すが、このとき情報落ちが発生して誤差が蓄積する
        //Double-Doubleの乗算の要領で10000倍を正確に行い、誤差の蓄積を回避する
        //x = (x - (double) d) * 10000.0;
        double xh = x * 0x8000001p0;
        xh += x - xh;  //xの上半分
        x = (xh - (double) d) * 10000.0 + (x - xh) * 10000.0;
        d = (int) x;
        t = XEiJ.FMT_BCD4[d];
        w[i    ] = t >> 12;
        w[i + 1] = t >>  8 & 15;
        w[i + 2] = t >>  4 & 15;
        w[i + 3] = t       & 15;
      }
    }
    //先頭の位置を確認する
    //  w[h]が先頭(0でない最初の数字)の位置
    int h = w[0] != 0 ? 0 : w[1] != 0 ? 1 : 2;
    //14+1桁目を四捨五入する
    int o = h + 14;  //w[o]は四捨五入する桁の位置。w[]の範囲内
    if (5 <= w[o]) {
      int i = o;
      while (10 <= ++w[--i]) {
        w[i] = 0;
      }
      if (i < h) {  //先頭から繰り上がった。このとき新しい先頭は1でそれ以外はすべて0
        h--;  //先頭を左にずらす
        o--;  //末尾を左にずらす
      }
    }
    //先頭の位置に応じて指数部を更新する
    //  w[h]が整数部、w[h+1..o-1]が小数部。10^eの小数点はw[h]の右側。整数部の桁数はe+1桁
    e -= h - 1;
    //先頭からlen3+1桁目が先頭から14+1桁目よりも左側にあるときその桁で改めて四捨五入する
    //  あらかじめ14+1桁目で四捨五入しておかないと、
    //  1.5の5を四捨五入しなければならないときに誤差で1.499…になったまま4を四捨五入しようとして失敗することがある
    int s = h + len3;  //w[s]は先頭からlen3+1桁目の位置。w.length<=sの場合があることに注意
    if (s < o) {
      o = s;  //w[o]は四捨五入する桁の位置。o<0の場合があることに注意
      if (0 <= o && 5 <= w[o]) {
        int i = o;
        while (10 <= ++w[--i]) {
          w[i] = 0;
        }
        if (i < h) {  //先頭から繰り上がった。このとき新しい先頭は1でそれ以外はすべて0
          h--;  //先頭を左にずらす
          o--;  //末尾を左にずらす
          e++;  //指数部を1増やす
        }
      }
    }
    //文字列に変換する
    while (a < b && h < o) {
      XEiJ.busWb (a++, '0' + w[h++]);  //有効数字
    }
    while (a < b) {
      XEiJ.busWb (a++, '0');  //残りは'0'
    }
    XEiJ.busWb (a, '\0');
    XEiJ.regRn[0] = e + 1;  //小数点の位置
  }  //fpkECVTSub0()

  //fpkFCVT ()
  //  $FE25  __FCVT
  //  64bit浮動小数点数を小数点以下の桁数を指定して文字列に変換する
  //  小数点の位置がpのとき[p]の左側に小数点がある
  //  全体の桁数が制限されないので指数部が大きいとき整数部が収まるサイズのバッファが必要
  //  0または1以上のとき
  //    整数部と小数点以下の指定された桁数までを小数部の0を省略せずに出力する
  //    整数部と小数点以下の指定された桁数が合わせて14桁を超えるときは15桁目が四捨五入されて15桁目以降は0になる
  //    小数点の位置は整数部の桁数に等しい
  //    print fcvt(0#,4,i2,i3),i2,i3
  //    0000     0       0
  //    print fcvt(2e+12/3#,4,i2,i3),i2,i3
  //    6666666666666700         12      0
  //               ↑
  //  1未満のとき
  //    小数点以下の桁数の範囲内を先頭の0を省略して出力する
  //    小数点以下の桁数の範囲内がすべて0のときは""になる
  //    小数点の位置は指数部+1に等しい
  //    print fcvt(0.01,3,i2,i3),i2,i3                0.010
  //    10      -1       0                              <~~
  //    print fcvt(0.001,3,i2,i3),i2,i3               0.001
  //    1       -2       0                              <<~
  //    print fcvt(0.0001,3,i2,i3),i2,i3              0.0001
  //            -3       0                              <<<
  //    print fcvt(0.00001,3,i2,i3),i2,i3             0.00001
  //            -4       0                              <<<<
  //  #INFと#NAN
  //    小数点以下の桁数の指定に関係なく4文字出力して小数点の位置4を返す
  //    print fcvt(val("#INF"),2,i2,i3),i2,i3
  //    #INF     4       0
  //    print fcvt(val("#INF"),6,i2,i3),i2,i3
  //    #INF     4       0
  //  バグ
  //    FLOAT4.X 1.02は結果が整数部が大きいとき255文字で打ち切られる
  //    FLOAT4.X 1.02はFCVT(±0)の整数部が0桁ではなく1桁になる
  //  <d0d1.d:64bit浮動小数点数
  //  <d2.l:小数点以下の桁数
  //  <a0.l:文字列バッファの先頭
  //  >d0.l:先頭から小数点の位置までのオフセット
  //  >d1.l:符号(0=+,1=-)
  public static void fpkFCVT () throws M68kException {
    fpkFCVTSub ((long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1]);  //64bit浮動小数点数
  }  //fpkFCVT()
  public static void fpkFCVTSub (long l) throws M68kException {
    int len2 = Math.max (0, XEiJ.regRn[2]);  //小数部の桁数
    int a = XEiJ.regRn[8];  //文字列バッファの先頭
    //符号と指数部の処理
    //  ±0,±Inf,NaNはここで除外する
    if (0L <= l) {
      XEiJ.regRn[1] = 0;  //正符号
    } else {
      XEiJ.regRn[1] = 1;  //負符号
      l &= 0x7fffffffffffffffL;  //符号bitを消しておく
    }
    double x = Double.longBitsToDouble (l);  //絶対値
    int e = (int) (l >>> 52) - 1023;  //指数部。ゲタ0。符号bitは消してあるのでマスクは不要
    l &= 0x000fffffffffffffL;  //仮数部の小数部。正規化数のとき整数部の1が付いていないことに注意
    if (e == -1023) {  //±0,非正規化数
      if (l == 0L) {  //±0
        //指定された小数点以下の桁数だけ'0'を並べる
        while (len2-- > 0) {
          XEiJ.busWb (a++, '0');
        }
        XEiJ.busWb (a, '\0');
        XEiJ.regRn[0] = 0;  //小数点の位置
        return;
      }
      e -= Long.numberOfLeadingZeros (l) - 12;  //非正規化数の指数部を補正する
    } else if (e == 1024) {  //±Inf,NaN
      XEiJ.busWb (a++, '#');
      if (l == 0L) {  //±Inf
        XEiJ.busWb (a++, 'I');
        XEiJ.busWb (a++, 'N');
        XEiJ.busWb (a++, 'F');
      } else {  //NaN
        XEiJ.busWb (a++, 'N');
        XEiJ.busWb (a++, 'A');
        XEiJ.busWb (a++, 'N');
      }
      XEiJ.busWb (a, '\0');
      XEiJ.regRn[0] = 4;  //小数点の位置
      return;
    }
    //10進数で表現したときの指数部を求める
    //  10^e<=x<10^(e+1)となるeを求める
    e = (int) Math.floor ((double) e * 0.30102999566398119521373889472);  //log10(2)
    //10^-eを掛けて1<=x<10にする
    //  非正規化数の最小値から正規化数の最大値まで処理できなければならない
    //  10^-eを計算してからまとめて掛ける方法はxが非正規化数のとき10^-eがオーバーフローしてしまうので不可
    //    doubleは非正規化数の逆数を表現できない
    if (0 < e) {  //10<=x
      x *= FPK_TEN_M16QR[e & 15];
      if (16 <= e) {
        x *= FPK_TEN_M16QR[16 + (e >> 4 & 15)];
        if (256 <= e) {
          x *= FPK_TEN_M16QR[33];  //FPK_TEN_M16QR[32 + (e >> 8)]
        }
      }
    } else if (e < 0) {  //x<1
      x *= FPK_TEN_P16QR[-e & 15];
      if (e <= -16) {
        x *= FPK_TEN_P16QR[16 + (-e >> 4 & 15)];
        if (e <= -256) {
          x *= FPK_TEN_P16QR[33];  //FPK_TEN_P16QR[32 + (-e >> 8)]
        }
      }
    }
    //整数部2桁、小数部16桁の10進数に変換する
    //  1<=x<10なのでw[1]が先頭になるはずだが誤差で前後にずれる可能性がある
    int[] w = new int[18];
    {
      int d = (int) x;
      int t = XEiJ.FMT_BCD4[d];
      w[0] = t >> 4;
      w[1] = t      & 15;
      for (int i = 2; i < 18; i += 4) {
        //xを10000倍して整数部dを引くことで小数部を残すが、このとき情報落ちが発生して誤差が蓄積する
        //Double-Doubleの乗算の要領で10000倍を正確に行い、誤差の蓄積を回避する
        //x = (x - (double) d) * 10000.0;
        double xh = x * 0x8000001p0;
        xh += x - xh;  //xの上半分
        x = (xh - (double) d) * 10000.0 + (x - xh) * 10000.0;
        d = (int) x;
        t = XEiJ.FMT_BCD4[d];
        w[i    ] = t >> 12;
        w[i + 1] = t >>  8 & 15;
        w[i + 2] = t >>  4 & 15;
        w[i + 3] = t       & 15;
      }
    }
    //先頭の位置を確認する
    //  w[h]が先頭(0でない最初の数字)の位置
    int h = w[0] != 0 ? 0 : w[1] != 0 ? 1 : 2;
    //14+1桁目を四捨五入する
    int o = h + 14;  //w[o]は四捨五入する桁の位置。w[]の範囲内
    if (5 <= w[o]) {
      int i = o;
      while (10 <= ++w[--i]) {
        w[i] = 0;
      }
      if (i < h) {  //先頭から繰り上がった。このとき新しい先頭は1でそれ以外はすべて0
        h--;  //先頭を左にずらす
        o--;  //末尾を左にずらす
      }
    }
    //先頭の位置に応じて指数部を更新する
    //  w[h]が整数部、w[h+1..o-1]が小数部。10^eの小数点はw[h]の右側。整数部の桁数はe+1桁
    e -= h - 1;
    //小数点以下len2+1桁目が先頭から14+1桁目よりも左側にあるときその桁で改めて四捨五入する
    //  あらかじめ14+1桁目で四捨五入しておかないと、
    //  1.5の5を四捨五入しなければならないときに誤差で1.499…になったまま4を四捨五入しようとして失敗することがある
    int s = h + e + 1 + len2;  //w[s]は小数点以下len2+1桁目の位置。w.length<=sの場合があることに注意
    if (s < o) {
      o = s;  //w[o]は四捨五入する桁の位置。o<0の場合があることに注意
      if (0 <= o && 5 <= w[o]) {
        int i = o;
        while (10 <= ++w[--i]) {
          w[i] = 0;
        }
        if (i < h) {  //先頭から繰り上がった。このとき新しい先頭は1でそれ以外はすべて0
          h--;  //先頭を左にずらす
          o--;  //末尾を左にずらす
          e++;  //指数部を1増やす
        }
      }
    }
    //文字列に変換する
    while (h < o) {
      XEiJ.busWb (a++, '0' + w[h++]);  //有効数字
    }
    while (h++ < s) {
      XEiJ.busWb (a++, '0');  //残りは'0'
    }
    XEiJ.busWb (a, '\0');
    XEiJ.regRn[0] = e + 1;  //小数点の位置
  }  //fpkFCVTSub0()

  //fpkGCVT ()
  //  $FE26  __GCVT
  //  64bit浮動小数点数を全体の桁数を指定して文字列に変換する
  //  指定された桁数で表現できないときは指数表現になる
  //  メモ
  //    print gcvt(1e-1,10)
  //    0.1
  //    print gcvt(1e-8,10)
  //    0.00000001
  //    print gcvt(1.5e-8,10)
  //    1.5E-008
  //    print gcvt(1e-9,10)
  //    1.E-009                 小数点はあるが小数部がない
  //    print gcvt(2e-1/3#,10)
  //    6.666666667E-002
  //    print gcvt(2e+0/3#,10)
  //    0.6666666667
  //    print gcvt(2e+1/3#,10)
  //    6.666666667
  //    print gcvt(2e+9/3#,10)
  //    666666666.7
  //    print gcvt(2e+10/3#,10)
  //    6666666667
  //    print gcvt(2e+11/3#,10)
  //    6.666666667E+010
  //    print gcvt(0#,4)
  //    0.
  //    print gcvt(val("#INF"),4)
  //    #INF
  //    print gcvt(val("#INF"),3)
  //    $.E+003
  //    print gcvt(val("#INF"),2)
  //    $.E+003
  //    print gcvt(val("#INF"),1)
  //    $.E+003
  //    FLOAT2.XのGCVTは小数部がなくても桁数の範囲内であれば小数点を書く
  //    桁数ちょうどのときは小数点も指数部も付かないので、整数でないことを明確にするために小数点を書いているとも言い難い
  //    ここでは#NANと#INF以外は小数部がなくても小数点を書くことにする
  //  バグ
  //    FLOAT2.X 2.02/2.03は#NANと#INFにも小数点を付ける
  //    FLOAT2.X 2.02/2.03は#NANと#INFのとき桁数が足りないと指数形式にしようとして文字列が壊れる
  //    FLOAT4.X 1.02は#NANと#INFにも小数点を付ける
  //    FLOAT4.X 1.02は桁数の少ない整数には小数点を付けて桁数ちょうどの整数には小数点も指数部も付けない
  //  <d0d1.d:64bit浮動小数点数
  //  <d2.b:全体の桁数
  //  <a0.l:文字列バッファの先頭
  //  >a0.l:末尾の'\0'の位置
  public static void fpkGCVT () throws M68kException {
    fpkGCVTSub ((long) XEiJ.regRn[0] << 32 | 0xffffffffL & XEiJ.regRn[1]);  //64bit浮動小数点数
  }  //fpkGCVT()
  public static void fpkGCVTSub (long l) throws M68kException {
    int len3 = XEiJ.regRn[2];  //全体の桁数
    int a = XEiJ.regRn[8];  //文字列バッファの先頭
    if (len3 <= 0) {  //全体の桁数が0
      XEiJ.busWb (a, '\0');
      return;
    }
    //符号と指数部の処理
    //  ±0,±Inf,NaNはここで除外する
    if (l < 0L) {
      XEiJ.busWb (a++, '-');  //負符号
      l &= 0x7fffffffffffffffL;  //符号bitを消しておく
    }
    double x = Double.longBitsToDouble (l);  //絶対値
    int e = (int) (l >>> 52) - 1023;  //指数部。ゲタ0。符号bitは消してあるのでマスクは不要
    l &= 0x000fffffffffffffL;  //仮数部の小数部。正規化数のとき整数部の1が付いていないことに注意
    if (e == -1023) {  //±0,非正規化数
      if (l == 0L) {  //±0
        XEiJ.busWb (a++, '0');  //0
        XEiJ.busWb (a++, '.');  //小数点
        XEiJ.busWb (a, '\0');
        XEiJ.regRn[8] = a;  //末尾の'\0'の位置
        return;
      }
      e -= Long.numberOfLeadingZeros (l) - 12;  //非正規化数の指数部を補正する
    } else if (e == 1024) {  //±Inf,NaN
      XEiJ.busWb (a++, '#');
      if (l == 0L) {  //±Inf
        XEiJ.busWb (a++, 'I');
        XEiJ.busWb (a++, 'N');
        XEiJ.busWb (a++, 'F');
      } else {  //NaN
        XEiJ.busWb (a++, 'N');
        XEiJ.busWb (a++, 'A');
        XEiJ.busWb (a++, 'N');
      }
      XEiJ.busWb (a, '\0');
      XEiJ.regRn[8] = a;  //末尾の'\0'の位置
      return;
    }
    //10進数で表現したときの指数部を求める
    //  10^e<=x<10^(e+1)となるeを求める
    e = (int) Math.floor ((double) e * 0.30102999566398119521373889472);  //log10(2)
    //10^-eを掛けて1<=x<10にする
    //  非正規化数の最小値から正規化数の最大値まで処理できなければならない
    //  10^-eを計算してからまとめて掛ける方法はxが非正規化数のとき10^-eがオーバーフローしてしまうので不可
    //    doubleは非正規化数の逆数を表現できない
    if (0 < e) {  //10<=x
      x *= FPK_TEN_M16QR[e & 15];
      if (16 <= e) {
        x *= FPK_TEN_M16QR[16 + (e >> 4 & 15)];
        if (256 <= e) {
          x *= FPK_TEN_M16QR[33];  //FPK_TEN_M16QR[32 + (e >> 8)]
        }
      }
    } else if (e < 0) {  //x<1
      x *= FPK_TEN_P16QR[-e & 15];
      if (e <= -16) {
        x *= FPK_TEN_P16QR[16 + (-e >> 4 & 15)];
        if (e <= -256) {
          x *= FPK_TEN_P16QR[33];  //FPK_TEN_P16QR[32 + (-e >> 8)]
        }
      }
    }
    //整数部2桁、小数部16桁の10進数に変換する
    //  1<=x<10なのでw[1]が先頭になるはずだが誤差で前後にずれる可能性がある
    int[] w = new int[18];
    {
      int d = (int) x;
      int t = XEiJ.FMT_BCD4[d];
      w[0] = t >> 4;
      w[1] = t      & 15;
      for (int i = 2; i < 18; i += 4) {
        //xを10000倍して整数部dを引くことで小数部を残すが、このとき情報落ちが発生して誤差が蓄積する
        //Double-Doubleの乗算の要領で10000倍を正確に行い、誤差の蓄積を回避する
        //x = (x - (double) d) * 10000.0;
        double xh = x * 0x8000001p0;
        xh += x - xh;  //xの上半分
        x = (xh - (double) d) * 10000.0 + (x - xh) * 10000.0;
        d = (int) x;
        t = XEiJ.FMT_BCD4[d];
        w[i    ] = t >> 12;
        w[i + 1] = t >>  8 & 15;
        w[i + 2] = t >>  4 & 15;
        w[i + 3] = t       & 15;
      }
    }
    //先頭の位置を確認する
    //  w[h]が先頭(0でない最初の数字)の位置
    int h = w[0] != 0 ? 0 : w[1] != 0 ? 1 : 2;
    //14+1桁目を四捨五入する
    int o = h + 14;  //w[o]は四捨五入する桁の位置。w[]の範囲内
    if (5 <= w[o]) {
      int i = o;
      while (10 <= ++w[--i]) {
        w[i] = 0;
      }
      if (i < h) {  //先頭から繰り上がった。このとき新しい先頭は1でそれ以外はすべて0
        h--;  //先頭を左にずらす
        o--;  //末尾を左にずらす
      }
    }
    //先頭の位置に応じて指数部を更新する
    //  w[h]が整数部、w[h+1..o-1]が小数部。10^eの小数点はw[h]の右側。整数部の桁数はe+1桁
    e -= h - 1;
    //先頭からlen3+1桁目が先頭から14+1桁目よりも左側にあるときその桁で改めて四捨五入する
    //  あらかじめ14+1桁目で四捨五入しておかないと、
    //  1.5の5を四捨五入しなければならないときに誤差で1.499…になったまま4を四捨五入しようとして失敗することがある
    int s = h + len3;  //w[s]は先頭からlen3+1桁目の位置。w.length<=sの場合があることに注意
    if (s < o) {
      o = s;  //w[o]は四捨五入する桁の位置。o<0の場合があることに注意
      if (0 <= o && 5 <= w[o]) {
        int i = o;
        while (10 <= ++w[--i]) {
          w[i] = 0;
        }
        if (i < h) {  //先頭から繰り上がった。このとき新しい先頭は1でそれ以外はすべて0
          h--;  //先頭を左にずらす
          o--;  //末尾を左にずらす
          e++;  //指数部を1増やす
        }
      }
    }
    //末尾の位置を確認する
    //  w[o-1]が末尾(0でない最後の数字)の位置
    while (w[o - 1] == 0) {  //全体は0ではないので必ず止まる。小数点よりも左側で止まる場合があることに注意
      o--;
    }
    //指数形式にするかどうか選択して文字列に変換する
    if (0 <= e && e < len3) {  //1<=x<10^len3。指数形式にしない
      for (; 0 <= e && h < o; e--) {
        XEiJ.busWb (a++, '0' + w[h++]);  //整数部
      }
      for (; 0 <= e; e--) {
        XEiJ.busWb (a++, '0');  //整数部。末尾の位置に関係なく1の位まで書く
      }
      XEiJ.busWb (a++, '.');  //小数部がなくても小数点を書く
      while (h < o) {
        XEiJ.busWb (a++, '0' + w[h++]);  //小数部
      }
    } else if (-4 <= e && e < 0) {  //10^-4<=x<1。指数形式にしない
      XEiJ.busWb (a++, '0');  //整数部の0
      XEiJ.busWb (a++, '.');  //小数点
      while (++e < 0) {
        XEiJ.busWb (a++, '0');  //小数部の先頭の0の並び
      }
      while (h < o) {
        XEiJ.busWb (a++, '0' + w[h++]);  //小数部
      }
    } else {  //x<10^-4または10^len3<=x。指数形式にする
      XEiJ.busWb (a++, '0' + w[h++]);  //整数部
      XEiJ.busWb (a++, '.');  //小数部がなくても小数点を書く
      while (h < o) {
        XEiJ.busWb (a++, '0' + w[h++]);  //小数部
      }
      XEiJ.busWb (a++, 'E');  //指数部の始まり
      if (0 <= e) {
        XEiJ.busWb (a++, '+');  //指数部の正符号。省略しない
      } else {
        XEiJ.busWb (a++, '-');  //指数部の負符号
        e = -e;
      }
      e = XEiJ.FMT_BCD4[e];
      XEiJ.busWb (a++, '0' + (e >> 8     ));  //指数部の100の位。0でも省略しない
      XEiJ.busWb (a++, '0' + (e >> 4 & 15));  //指数部の10の位
      XEiJ.busWb (a++, '0' + (e      & 15));  //指数部の1の位
    }
    XEiJ.busWb (a, '\0');
    XEiJ.regRn[8] = a;  //末尾の'\0'の位置
  }  //fpkGCVTSub0()

  //fpkFVAL ()
  //  $FE50  __FVAL
  //  文字列を32bit浮動小数点数に変換する
  //  __VALとほぼ同じ
  //  <a0.l:文字列の先頭
  //  >d0.s:32bit浮動小数点数
  //  >d2.l:(先頭が'&'でないとき)65535=32bit浮動小数点数をオーバーフローなしでintに変換できる,0=それ以外
  //  >d3.l:(先頭が'&'でないとき)d2.l==65535のとき32bit浮動小数点数をintに変換した値
  //  >a0.l:変換された文字列の直後('\0'とは限らない)
  //  >ccr:0=エラーなし,N|C=文法エラー,V|C=オーバーフロー
  public static void fpkFVAL () throws M68kException {
    int a = XEiJ.regRn[8];  //a0
    //先頭の空白を読み飛ばす
    int c = XEiJ.busRbs (a++);
    while (c == ' ' || c == '\t') {
      c = XEiJ.busRbs (a++);
    }
    if (c == '&') {  //&B,&O,&H
      c = XEiJ.busRbs (a++) & 0xdf;
      XEiJ.regRn[8] = a;  //&?の直後
      if (c == 'B') {
        fpkSTOB ();
        fpkLTOF ();
      } else if (c == 'O') {
        fpkSTOO ();
        fpkLTOF ();
      } else if (c == 'H') {
        fpkSTOH ();
        fpkLTOF ();
      } else {
        XEiJ.regCCR = XEiJ.REG_CCR_N | XEiJ.REG_CCR_C;  //文法エラー
      }
    } else {  //&B,&O,&H以外
      fpkSTOF ();
    }
  }  //fpkFVAL()

  //fpkFUSING ()
  //  $FE51  __FUSING
  //  32bit浮動小数点数をアトリビュートを指定して文字列に変換する
  //  __USINGとほぼ同じ
  //  <d0.s:32bit浮動小数点数
  //  <d2.l:整数部の桁数
  //  <d3.l:小数部の桁数。-1=小数点を書かない,0=小数点を書く
  //  <d4.l:アトリビュート
  //    bit0  左側を'*'で埋める
  //    bit1  先頭に'\\'を付ける
  //    bit2  整数部を3桁毎に','で区切る
  //    bit3  指数形式
  //    bit4  先頭に符号('+'または'-')を付ける。指数形式でないとき整数部の桁数が1桁増える
  //    bit5  末尾に符号('+'または'-')を付ける
  //    bit6  末尾に符号(' 'または'-')を付ける
  //  <a0.l:文字列バッファの先頭
  //  a0は変化しない
  public static void fpkFUSING () throws M68kException {
    fpkUSINGSub (Double.doubleToLongBits ((double) Float.intBitsToFloat (XEiJ.regRn[0])));  //32bit浮動小数点数
  }  //fpkFUSING()

  //fpkSTOF ()
  //  $FE52  __STOF
  //  文字列を32bit浮動小数点数に変換する
  //  __STODとほぼ同じ
  //  <a0.l:文字列の先頭
  //  >d0.s:32bit浮動小数点数
  //  >d2.l:65535=32bit浮動小数点数をオーバーフローなしでintに変換できる,0=それ以外
  //  >d3.l:d2.l==65535のとき32bit浮動小数点数をintに変換した値
  //  >a0.l:変換された文字列の直後('\0'とは限らない)
  //  >ccr:0=エラーなし,N|C=文法エラー,V|C=オーバーフロー
  public static void fpkSTOF () throws M68kException {
    int h = Float.floatToIntBits ((float) fpkSTODSub ());  //d0
    if (FPK_FPCP_NAN && h == 0x7fc00000) {
      h = 0x7fffffff;
    }
    XEiJ.regRn[0] = h;
  }  //fpkSTOF()

  //fpkFTOS ()
  //  $FE53  __FTOS
  //  32bit浮動小数点数を文字列に変換する
  //  __DTOSとほぼ同じ
  //  <d0.s:32bit浮動小数点数
  //  <a0.l:文字列バッファの先頭
  //  >a0.l:末尾の'\0'の位置
  public static void fpkFTOS () throws M68kException {
    fpkDTOSSub (Double.doubleToLongBits ((double) Float.intBitsToFloat (XEiJ.regRn[0])));  //32bit浮動小数点数
  }  //fpkFTOS()

  //fpkFECVT ()
  //  $FE54  __FECVT
  //  32bit浮動小数点数を全体の桁数を指定して文字列に変換する
  //  __ECVTとほぼ同じ
  //  <d0.s:32bit浮動小数点数
  //  <d2.b:全体の桁数
  //  <a0.l:文字列バッファの先頭。末尾に'\0'を書き込むので桁数+1バイト必要
  //  >d0.l:先頭から小数点の位置までのオフセット
  //  >d1.l:符号(0=+,1=-)
  //  a0.lは変化しない
  public static void fpkFECVT () throws M68kException {
    fpkECVTSub (Double.doubleToLongBits ((double) Float.intBitsToFloat (XEiJ.regRn[0])));  //32bit浮動小数点数
  }  //fpkFECVT()

  //fpkFFCVT ()
  //  $FE55  __FFCVT
  //  32bit浮動小数点数を小数点以下の桁数を指定して文字列に変換する
  //  __FCVTとほぼ同じ
  //  <d0.s:32bit浮動小数点数
  //  <d2.b:小数点以下の桁数
  //  <a0.l:文字列バッファの先頭
  //  >d0.l:先頭から小数点の位置までのオフセット
  //  >d1.l:符号(0=+,1=-)
  public static void fpkFFCVT () throws M68kException {
    fpkFCVTSub (Double.doubleToLongBits ((double) Float.intBitsToFloat (XEiJ.regRn[0])));  //32bit浮動小数点数
  }  //fpkFFCVT()

  //fpkFGCVT ()
  //  $FE56  __FGCVT
  //  32bit浮動小数点数を全体の桁数を指定して文字列に変換する
  //  __GCVTとほぼ同じ
  //  <d0.s:32bit浮動小数点数
  //  <d2.b:全体の桁数
  //  <a0.l:文字列バッファの先頭
  //  >a0.l:末尾の'\0'の位置
  public static void fpkFGCVT () throws M68kException {
    fpkGCVTSub (Double.doubleToLongBits ((double) Float.intBitsToFloat (XEiJ.regRn[0])));  //32bit浮動小数点数
  }  //fpkFGCVT()

  //fpkCLMUL ()
  //  $FEE0  __CLMUL
  //  32bit符号あり整数乗算
  //  <(a7).l:32bit符号あり整数。被乗数x
  //  <4(a7).l:32bit符号あり整数。乗数y
  //  >(a7).l:32bit符号あり整数。積x*y。オーバーフローのときは不定
  //  >ccr:cs=オーバーフロー。C以外は不定
  public static void fpkCLMUL () throws M68kException {
    int a7 = XEiJ.regRn[15];
    long l = (long) XEiJ.busRls (a7) * (long) XEiJ.busRls (a7 + 4);
    int h = (int) l;
    XEiJ.busWl (a7, h);  //オーバーフローのときは積の下位32bit
    XEiJ.regCCR = (long) h == l ? 0 : XEiJ.REG_CCR_C;
  }  //fpkCLMUL()

  //fpkCLDIV ()
  //  $FEE1  __CLDIV
  //  32bit符号あり整数除算
  //  <(a7).l:32bit符号あり整数。被除数x
  //  <4(a7).l:32bit符号あり整数。除数y
  //  >(a7).l:32bit符号あり整数。商x/y。ゼロ除算のときは不定
  //  >ccr:cs=ゼロ除算。C以外は不定
  public static void fpkCLDIV () throws M68kException {
    int a7 = XEiJ.regRn[15];
    int h = XEiJ.busRls (a7 + 4);
    if (h == 0) {
      //(a7).lは変化しない
      XEiJ.regCCR = XEiJ.REG_CCR_C;
    } else {
      XEiJ.busWl (a7, XEiJ.busRls (a7) / h);
      XEiJ.regCCR = 0;
    }
  }  //fpkCLDIV()

  //fpkCLMOD ()
  //  $FEE2  __CLMOD
  //  32bit符号あり整数剰余算
  //  <(a7).l:32bit符号あり整数。被除数x
  //  <4(a7).l:32bit符号あり整数。除数y
  //  >(a7).l:32bit符号あり整数。余りx%y。ゼロ除算のときは不定
  //  >ccr:cs=ゼロ除算。C以外は不定
  public static void fpkCLMOD () throws M68kException {
    int a7 = XEiJ.regRn[15];
    int h = XEiJ.busRls (a7 + 4);
    if (h == 0) {
      //(a7).lは変化しない
      XEiJ.regCCR = XEiJ.REG_CCR_C;
    } else {
      XEiJ.busWl (a7, XEiJ.busRls (a7) % h);
      XEiJ.regCCR = 0;
    }
  }  //fpkCLMOD()

  //fpkCUMUL ()
  //  $FEE3  __CUMUL
  //  32bit符号なし整数乗算
  //  <(a7).l:32bit符号なし整数。被乗数x
  //  <4(a7).l:32bit符号なし整数。乗数y
  //  >(a7).l:32bit符号なし整数。積x*y。オーバーフローのときは不定
  //  >ccr:cs=オーバーフロー。C以外は不定
  public static void fpkCUMUL () throws M68kException {
    int a7 = XEiJ.regRn[15];
    long l = (0xffffffffL & XEiJ.busRls (a7)) * (0xffffffffL & XEiJ.busRls (a7 + 4));
    int h = (int) l;
    XEiJ.busWl (a7, h);
    XEiJ.regCCR = (0xffffffffL & h) == l ? 0 : XEiJ.REG_CCR_C;
  }  //fpkCUMUL()

  //fpkCUDIV ()
  //  $FEE4  __CUDIV
  //  32bit符号なし整数除算
  //  <(a7).l:32bit符号なし整数。被除数x
  //  <4(a7).l:32bit符号なし整数。除数y
  //  >(a7).l:32bit符号なし整数。商x/y。ゼロ除算のときは不定
  //  >ccr:cs=ゼロ除算。C以外は不定
  public static void fpkCUDIV () throws M68kException {
    int a7 = XEiJ.regRn[15];
    int h = XEiJ.busRls (a7 + 4);
    if (h == 0) {
      //(a7).lは変化しない
      XEiJ.regCCR = XEiJ.REG_CCR_C;
    } else {
      XEiJ.busWl (a7, (int) ((0xffffffffL & XEiJ.busRls (a7)) / (0xffffffffL & h)));
      XEiJ.regCCR = 0;
    }
  }  //fpkCUDIV()

  //fpkCUMOD ()
  //  $FEE5  __CUMOD
  //  32bit符号なし整数剰余算
  //  <(a7).l:32bit符号なし整数。被除数x
  //  <4(a7).l:32bit符号なし整数。除数y
  //  >(a7).l:32bit符号なし整数。余りx%y。ゼロ除算のときは不定
  //  >ccr:cs=ゼロ除算。C以外は不定
  public static void fpkCUMOD () throws M68kException {
    int a7 = XEiJ.regRn[15];
    int h = XEiJ.busRls (a7 + 4);
    if (h == 0) {
      //(a7).lは変化しない
      XEiJ.regCCR = XEiJ.REG_CCR_C;
    } else {
      XEiJ.busWl (a7, (int) ((0xffffffffL & XEiJ.busRls (a7)) % (0xffffffffL & h)));
      XEiJ.regCCR = 0;
    }
  }  //fpkCUMOD()

  //fpkCLTOD ()
  //  $FEE6  __CLTOD
  //  32bit符号あり整数を64bit浮動小数点数に変換する
  //  <(a7).l:32bit符号あり整数。x
  //  >(a7).d:64bit浮動小数点数。(double)x
  public static void fpkCLTOD () throws M68kException {
    //int→double→[long]→[int,int]
    int a7 = XEiJ.regRn[15];
    long l = Double.doubleToLongBits ((double) XEiJ.busRls (a7));
    XEiJ.busWl (a7, (int) (l >>> 32));
    XEiJ.busWl (a7 + 4, (int) l);
  }  //fpkCLTOD()

  //fpkCDTOL ()
  //  $FEE7  __CDTOL
  //  64bit浮動小数点数を32bit符号あり整数に変換する
  //  <(a7).d:64bit浮動小数点数。x
  //  >(a7).l:32bit符号あり整数。(int)x
  //  >ccr:cs=オーバーフロー。C以外は不定
  public static void fpkCDTOL () throws M68kException {
    //[int,int]→[long]→double→int
    int a7 = XEiJ.regRn[15];
    double d = Double.longBitsToDouble ((long) XEiJ.busRls (a7) << 32 | 0xffffffffL & XEiJ.busRls (a7 + 4));
    XEiJ.busWl (a7, (int) d);  //オーバーフローのときは最小値または最大値
    XEiJ.regCCR = (double) Integer.MIN_VALUE - 1.0 < d && d < (double) Integer.MAX_VALUE + 1.0 ? 0 : XEiJ.REG_CCR_C;  //NaN,±Infはエラー
  }  //fpkCDTOL()

  //fpkCLTOF ()
  //  $FEE8  __CLTOF
  //  32bit符号あり整数を32bit浮動小数点数に変換する
  //  <(a7).l:32bit符号あり整数。x
  //  >(a7).s:32bit浮動小数点数。(float)x
  public static void fpkCLTOF () throws M68kException {
    //int→float→[int]
    int a7 = XEiJ.regRn[15];
    XEiJ.busWl (a7, Float.floatToIntBits ((float) XEiJ.busRls (a7)));
  }  //fpkCLTOF()

  //fpkCFTOL ()
  //  $FEE9  __CFTOL
  //  32bit浮動小数点数を32bit符号あり整数に変換する
  //  <(a7).s:32bit浮動小数点数。x
  //  >(a7).l:32bit符号あり整数。(int)x
  //  >ccr:cs=オーバーフロー。C以外は不定
  public static void fpkCFTOL () throws M68kException {
    //[int]→float→int
    int a7 = XEiJ.regRn[15];
    float f = Float.intBitsToFloat (XEiJ.busRls (a7));
    XEiJ.busWl (a7, (int) f);
    XEiJ.regCCR = (float) Integer.MIN_VALUE - 1.0F < f && f < (float) Integer.MAX_VALUE + 1.0F ? 0 : XEiJ.REG_CCR_C;  //NaN,±Infはエラー
  }  //fpkCFTOL()

  //fpkCFTOD ()
  //  $FEEA  __CFTOD
  //  32bit浮動小数点数を64bit浮動小数点数に変換する
  //  <(a7).s:32bit浮動小数点数。x
  //  >(a7).d:64bit浮動小数点数。(double)x
  public static void fpkCFTOD () throws M68kException {
    //[int]→float→double→[long]→[int,int]
    int a7 = XEiJ.regRn[15];
    long l = Double.doubleToLongBits ((double) Float.intBitsToFloat (XEiJ.busRls (a7)));
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.busWl (a7, (int) (l >>> 32));
    XEiJ.busWl (a7 + 4, (int) l);
  }  //fpkCFTOD()

  //fpkCDTOF ()
  //  $FEEB  __CDTOF
  //  64bit浮動小数点数を32bit浮動小数点数に変換する
  //  <(a7).d:64bit浮動小数点数。x
  //  >(a7).s:32bit浮動小数点数。(float)x
  //  >ccr:cs=オーバーフロー。C以外は不定
  public static void fpkCDTOF () throws M68kException {
    //[int,int]→[long]→double→float→[int]
    int a7 = XEiJ.regRn[15];
    double d = Double.longBitsToDouble ((long) XEiJ.busRls (a7) << 32 | 0xffffffffL & XEiJ.busRls (a7 + 4));
    int h = Float.floatToIntBits ((float) d);
    if (FPK_FPCP_NAN && h == 0x7fc00000) {
      h = 0x7fffffff;
    }
    XEiJ.busWl (a7, h);
    XEiJ.regCCR = (Double.isNaN (d) || Double.isInfinite (d) ||
           Math.abs (d) < (double) Float.MAX_VALUE + 0.5 * (double) Math.ulp (Float.MAX_VALUE) ? 0 : XEiJ.REG_CCR_C);  //アンダーフローはエラーなし
  }  //fpkCDTOF()

  //fpkCDCMP ()
  //  $FEEC  __CDCMP
  //  64bit浮動小数点数の比較
  //  x<=>y
  //  <(a7).d:64bit浮動小数点数。x
  //  <8(a7).d:64bit浮動小数点数。y
  //  >ccr:lt=x<y,eq=x==y,gt=x>y
  public static void fpkCDCMP () throws M68kException {
    //([int,int]→[long]→double)<=>([int,int]→[long]→double)
    int a7 = XEiJ.regRn[15];
    double xd = Double.longBitsToDouble ((long) XEiJ.busRls (a7) << 32 | 0xffffffffL & XEiJ.busRls (a7 + 4));
    double yd = Double.longBitsToDouble ((long) XEiJ.busRls (a7 + 8) << 32 | 0xffffffffL & XEiJ.busRls (a7 + 12));
    XEiJ.regCCR = xd < yd ? XEiJ.REG_CCR_N | XEiJ.REG_CCR_C : xd == yd ? XEiJ.REG_CCR_Z : 0;  //どちらかがNaNのときは0
  }  //fpkCDCMP()

  //fpkCDADD ()
  //  $FEED  __CDADD
  //  64bit浮動小数点数の加算
  //  <(a7).d:64bit浮動小数点数。被加算数x
  //  <8(a7).d:64bit浮動小数点数。加算数y
  //  >(a7).d:64bit浮動小数点数。和x+y
  //  >ccr:0=エラーなし,C=アンダーフロー,V|C=オーバーフロー
  public static void fpkCDADD () throws M68kException {
    //([int,int]→[long]→double)+([int,int]→[long]→double)→[long]→[int,int]
    int a7 = XEiJ.regRn[15];
    double xd = Double.longBitsToDouble ((long) XEiJ.busRls (a7) << 32 | 0xffffffffL & XEiJ.busRls (a7 + 4));
    double yd = Double.longBitsToDouble ((long) XEiJ.busRls (a7 + 8) << 32 | 0xffffffffL & XEiJ.busRls (a7 + 12));
    double zd = xd + yd;
    long l = Double.doubleToLongBits (zd);
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.busWl (a7, (int) (l >>> 32));
    XEiJ.busWl (a7 + 4, (int) l);
    XEiJ.regCCR = (Double.isNaN (xd) || Double.isNaN (yd) ? 0 :  //引数がNaN
           Double.isNaN (zd) ? XEiJ.REG_CCR_C :  //引数がNaNでないのに結果がNaNのときはエラー。(+Inf)+(-Inf)=NaN
           Double.isInfinite (xd) || Double.isInfinite (yd) ? 0 :  //引数が±Inf
           Double.isInfinite (zd) ? XEiJ.REG_CCR_V | XEiJ.REG_CCR_C :  //引数が±Infでないのに結果が±Infのときはオーバーフロー
           0);
  }  //fpkCDADD()

  //fpkCDSUB ()
  //  $FEEE  __CDSUB
  //  64bit浮動小数点数の減算
  //  <(a7).d:64bit浮動小数点数。被減算数x
  //  <8(a7).d:64bit浮動小数点数。減算数y
  //  >(a7).d:64bit浮動小数点数。差x-y
  //  >ccr:cs=エラー,vs=オーバーフロー
  public static void fpkCDSUB () throws M68kException {
    //([int,int]→[long]→double)-([int,int]→[long]→double)→[long]→[int,int]
    int a7 = XEiJ.regRn[15];
    double xd = Double.longBitsToDouble ((long) XEiJ.busRls (a7) << 32 | 0xffffffffL & XEiJ.busRls (a7 + 4));
    double yd = Double.longBitsToDouble ((long) XEiJ.busRls (a7 + 8) << 32 | 0xffffffffL & XEiJ.busRls (a7 + 12));
    double zd = xd - yd;
    long l = Double.doubleToLongBits (zd);
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.busWl (a7, (int) (l >>> 32));
    XEiJ.busWl (a7 + 4, (int) l);
    XEiJ.regCCR = (Double.isNaN (xd) || Double.isNaN (yd) ? 0 :  //引数がNaN
           Double.isNaN (zd) ? XEiJ.REG_CCR_C :  //引数がNaNでないのに結果がNaNのときはエラー。(+Inf)-(+Inf)=NaN
           Double.isInfinite (xd) || Double.isInfinite (yd) ? 0 :  //引数が±Inf
           Double.isInfinite (zd) ? XEiJ.REG_CCR_V | XEiJ.REG_CCR_C :  //引数が±Infでないのに結果が±Infのときはオーバーフロー
           0);
  }  //fpkCDSUB()

  //fpkCDMUL ()
  //  $FEEF  __CDMUL
  //  64bit浮動小数点数の乗算
  //  <(a7).d:64bit浮動小数点数。被乗数x
  //  <8(a7).d:64bit浮動小数点数。乗数y
  //  >(a7).d:64bit浮動小数点数。積x*y
  //  >ccr:cs=エラー,vs=オーバーフロー
  public static void fpkCDMUL () throws M68kException {
    //([int,int]→[long]→double)*([int,int]→[long]→double)→[long]→[int,int]
    int a7 = XEiJ.regRn[15];
    double xd = Double.longBitsToDouble ((long) XEiJ.busRls (a7) << 32 | 0xffffffffL & XEiJ.busRls (a7 + 4));
    double yd = Double.longBitsToDouble ((long) XEiJ.busRls (a7 + 8) << 32 | 0xffffffffL & XEiJ.busRls (a7 + 12));
    double zd = xd * yd;
    long l = Double.doubleToLongBits (zd);
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.busWl (a7, (int) (l >>> 32));
    XEiJ.busWl (a7 + 4, (int) l);
    XEiJ.regCCR = (Double.isNaN (xd) || Double.isNaN (yd) ? 0 :  //引数がNaN
           Double.isNaN (zd) ? XEiJ.REG_CCR_C :  //引数がNaNでないのに結果がNaNのときはエラー。(±0)*(±Inf)=NaN
           Double.isInfinite (xd) || Double.isInfinite (yd) ? 0 :  //引数が±Inf
           Double.isInfinite (zd) ? XEiJ.REG_CCR_V | XEiJ.REG_CCR_C :  //引数が±Infでないのに結果が±Infのときはオーバーフロー
           0);
  }  //fpkCDMUL()

  //fpkCDDIV ()
  //  $FEF0  __CDDIV
  //  64bit浮動小数点数の除算
  //  <(a7).d:64bit浮動小数点数。被除数x
  //  <8(a7).d:64bit浮動小数点数。除数y
  //  >(a7).d:64bit浮動小数点数。商x/y。ゼロ除算のときは不定
  //  >ccr:cs=エラー,eq=ゼロ除算,vs=オーバーフロー
  public static void fpkCDDIV () throws M68kException {
    //([int,int]→[long]→double)/([int,int]→[long]→double)→[long]→[int,int]
    int a7 = XEiJ.regRn[15];
    double xd = Double.longBitsToDouble ((long) XEiJ.busRls (a7) << 32 | 0xffffffffL & XEiJ.busRls (a7 + 4));
    double yd = Double.longBitsToDouble ((long) XEiJ.busRls (a7 + 8) << 32 | 0xffffffffL & XEiJ.busRls (a7 + 12));
    double zd = xd / yd;
    long l = Double.doubleToLongBits (zd);
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.busWl (a7, (int) (l >>> 32));
    XEiJ.busWl (a7 + 4, (int) l);
    XEiJ.regCCR = (Double.isNaN (xd) || Double.isNaN (yd) ? 0 :  //引数がNaN
           Double.isNaN (zd) ? XEiJ.REG_CCR_C :  //引数がNaNでないのに結果がNaNのときはエラー。(±0)/(±0)=NaN
           Double.isInfinite (xd) || Double.isInfinite (yd) ? 0 :  //引数が±Inf。(±Inf)/(±0)=(±Inf)
           yd == 0.0 ? XEiJ.REG_CCR_Z | XEiJ.REG_CCR_C :  //除数が±0のときはゼロ除算
           Double.isInfinite (zd) ? XEiJ.REG_CCR_V | XEiJ.REG_CCR_C :  //引数が±Infでないのに結果が±Infのときはオーバーフロー
           0);
  }  //fpkCDDIV()

  //fpkCDMOD ()
  //  $FEF1  __CDMOD
  //  64bit浮動小数点数の剰余算
  //  <(a7).d:64bit浮動小数点数。被除数x
  //  <8(a7).d:64bit浮動小数点数。除数y
  //  >(a7).d:64bit浮動小数点数。余りx%y。ゼロ除算のときは不定
  //  >ccr:cs=エラー,eq=ゼロ除算
  public static void fpkCDMOD () throws M68kException {
    //([int,int]→[long]→double)%([int,int]→[long]→double)→[long]→[int,int]
    int a7 = XEiJ.regRn[15];
    double xd = Double.longBitsToDouble ((long) XEiJ.busRls (a7) << 32 | 0xffffffffL & XEiJ.busRls (a7 + 4));
    double yd = Double.longBitsToDouble ((long) XEiJ.busRls (a7 + 8) << 32 | 0xffffffffL & XEiJ.busRls (a7 + 12));
    double zd = xd % yd;
    long l = Double.doubleToLongBits (zd);
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.busWl (a7, (int) (l >>> 32));
    XEiJ.busWl (a7 + 4, (int) l);
    XEiJ.regCCR = (Double.isNaN (xd) || Double.isNaN (yd) ? 0 :  //引数がNaN
           yd == 0.0 ? XEiJ.REG_CCR_Z | XEiJ.REG_CCR_C :  //除数が0のときはゼロ除算。(±Inf)%(±0)=NaN, x%(±0)=(±Inf)
           Double.isNaN (zd) ? XEiJ.REG_CCR_C :  //引数がNaNでないのに結果がNaNのときはエラー。(±Inf)%y=NaN
           Double.isInfinite (xd) || Double.isInfinite (yd) ? 0 :  //引数が±Inf
           Double.isInfinite (zd) ? XEiJ.REG_CCR_V | XEiJ.REG_CCR_C :  //引数が±Infでないのに結果が±Infのときはオーバーフロー
           0);
  }  //fpkCDMOD()

  //fpkCFCMP ()
  //  $FEF2  __CFCMP
  //  32bit浮動小数点数の比較
  //  x<=>y
  //  <(a7).s:32bit浮動小数点数。x
  //  <4(a7).s:32bit浮動小数点数。y
  //  >ccr:lt=x<y,eq=x==y,gt=x>y
  public static void fpkCFCMP () throws M68kException {
    //([int]→float)<=>([int]→float)
    int a7 = XEiJ.regRn[15];
    float xf = Float.intBitsToFloat (XEiJ.busRls (a7));
    float yf = Float.intBitsToFloat (XEiJ.busRls (a7 + 4));
    XEiJ.regCCR = xf < yf ? XEiJ.REG_CCR_N | XEiJ.REG_CCR_C : xf == yf ? XEiJ.REG_CCR_Z : 0;  //どちらかがNaNのときは0
  }  //fpkCFCMP()

  //fpkCFADD ()
  //  $FEF3  __CFADD
  //  32bit浮動小数点数の加算
  //  <(a7).s:32bit浮動小数点数。被加算数x
  //  <4(a7).s:32bit浮動小数点数。加算数y
  //  >(a7).s:32bit浮動小数点数。和x+y
  //  >ccr:cs=エラー,vs=オーバーフロー
  public static void fpkCFADD () throws M68kException {
    //([int]→float)+([int]→float)→[int]
    int a7 = XEiJ.regRn[15];
    float xf = Float.intBitsToFloat (XEiJ.busRls (a7));
    float yf = Float.intBitsToFloat (XEiJ.busRls (a7 + 4));
    float zf = xf + yf;
    int h = Float.floatToIntBits (zf);
    if (FPK_FPCP_NAN && h == 0x7fc00000) {
      h = 0x7fffffff;
    }
    XEiJ.busWl (a7, h);
    XEiJ.regCCR = (Float.isNaN (xf) || Float.isNaN (yf) ? 0 :  //引数がNaN
           Float.isNaN (zf) ? XEiJ.REG_CCR_C :  //引数がNaNでないのに結果がNaNのときはエラー。(+Inf)+(-Inf)=NaN
           Float.isInfinite (xf) || Float.isInfinite (yf) ? 0 :  //引数が±Inf
           Float.isInfinite (zf) ? XEiJ.REG_CCR_V | XEiJ.REG_CCR_C :  //引数が±Infでないのに結果が±Infのときはオーバーフロー
           0);
  }  //fpkCFADD()

  //fpkCFSUB ()
  //  $FEF4  __CFSUB
  //  32bit浮動小数点数の減算
  //  <(a7).s:32bit浮動小数点数。被減算数x
  //  <4(a7).s:32bit浮動小数点数。減算数y
  //  >(a7).s:32bit浮動小数点数。差x-y
  //  >ccr:cs=エラー,vs=オーバーフロー
  public static void fpkCFSUB () throws M68kException {
    //([int]→float)-([int]→float)→[int]
    int a7 = XEiJ.regRn[15];
    float xf = Float.intBitsToFloat (XEiJ.busRls (a7));
    float yf = Float.intBitsToFloat (XEiJ.busRls (a7 + 4));
    float zf = xf - yf;
    int h = Float.floatToIntBits (zf);
    if (FPK_FPCP_NAN && h == 0x7fc00000) {
      h = 0x7fffffff;
    }
    XEiJ.busWl (a7, h);
    XEiJ.regCCR = (Float.isNaN (xf) || Float.isNaN (yf) ? 0 :  //引数がNaN
           Float.isNaN (zf) ? XEiJ.REG_CCR_C :  //引数がNaNでないのに結果がNaNのときはエラー。(+Inf)-(+Inf)=NaN
           Float.isInfinite (xf) || Float.isInfinite (yf) ? 0 :  //引数が±Inf
           Float.isInfinite (zf) ? XEiJ.REG_CCR_V | XEiJ.REG_CCR_C :  //引数が±Infでないのに結果が±Infのときはオーバーフロー
           0);
  }  //fpkCFSUB()

  //fpkCFMUL ()
  //  $FEF5  __CFMUL
  //  32bit浮動小数点数の乗算
  //  <(a7).s:32bit浮動小数点数。被乗数x
  //  <4(a7).s:32bit浮動小数点数。乗数y
  //  >(a7).s:32bit浮動小数点数。積x*y
  //  >ccr:0=エラーなし,C=アンダーフロー,V|C=オーバーフロー
  public static void fpkCFMUL () throws M68kException {
    //([int]→float)*([int]→float)→[int]
    int a7 = XEiJ.regRn[15];
    float xf = Float.intBitsToFloat (XEiJ.busRls (a7));
    float yf = Float.intBitsToFloat (XEiJ.busRls (a7 + 4));
    float zf = xf * yf;
    int h = Float.floatToIntBits (zf);
    if (FPK_FPCP_NAN && h == 0x7fc00000) {
      h = 0x7fffffff;
    }
    XEiJ.busWl (a7, h);
    XEiJ.regCCR = (Float.isNaN (xf) || Float.isNaN (yf) ? 0 :  //引数がNaN
           Float.isNaN (zf) ? XEiJ.REG_CCR_C :  //引数がNaNでないのに結果がNaNのときはエラー。(±0)*(±Inf)=NaN
           Float.isInfinite (xf) || Float.isInfinite (yf) ? 0 :  //引数が±Inf
           Float.isInfinite (zf) ? XEiJ.REG_CCR_V | XEiJ.REG_CCR_C :  //引数が±Infでないのに結果が±Infのときはオーバーフロー
           0);
  }  //fpkCFMUL()

  //fpkCFDIV ()
  //  $FEF6  __CFDIV
  //  32bit浮動小数点数の除算
  //  <(a7).s:32bit浮動小数点数。被除数x
  //  <4(a7).s:32bit浮動小数点数。除数y
  //  >(a7).s:32bit浮動小数点数。商x/y。ゼロ除算のときは不定
  //  >ccr:cs=エラー,eq=ゼロ除算,vs=オーバーフロー
  public static void fpkCFDIV () throws M68kException {
    //([int]→float)/([int]→float)→[int]
    int a7 = XEiJ.regRn[15];
    float xf = Float.intBitsToFloat (XEiJ.busRls (a7));
    float yf = Float.intBitsToFloat (XEiJ.busRls (a7 + 4));
    float zf = xf / yf;
    int h = Float.floatToIntBits (zf);
    if (FPK_FPCP_NAN && h == 0x7fc00000) {
      h = 0x7fffffff;
    }
    XEiJ.busWl (a7, h);
    XEiJ.regCCR = (Float.isNaN (xf) || Float.isNaN (yf) ? 0 :  //引数がNaN
           Float.isNaN (zf) ? XEiJ.REG_CCR_C :  //引数がNaNでないのに結果がNaNのときはエラー。(±0)/(±0)=NaN
           Float.isInfinite (xf) || Float.isInfinite (yf) ? 0 :  //引数が±Inf。(±Inf)/(±0)=(±Inf)
           yf == 0.0F ? XEiJ.REG_CCR_Z | XEiJ.REG_CCR_C :  //除数が±0のときはゼロ除算
           Float.isInfinite (zf) ? XEiJ.REG_CCR_V | XEiJ.REG_CCR_C :  //引数が±Infでないのに結果が±Infのときはオーバーフロー
           0);
  }  //fpkCFDIV()

  //fpkCFMOD ()
  //  $FEF7  __CFMOD
  //  32bit浮動小数点数の剰余算
  //  <(a7).s:32bit浮動小数点数。被除数x
  //  <4(a7).s:32bit浮動小数点数。除数y
  //  >(a7).s:32bit浮動小数点数。余りx%y。ゼロ除算のときは不定
  //  >ccr:cs=エラー,eq=ゼロ除算
  public static void fpkCFMOD () throws M68kException {
    //([int]→float)%([int]→float)→[int]
    int a7 = XEiJ.regRn[15];
    float xf = Float.intBitsToFloat (XEiJ.busRls (a7));
    float yf = Float.intBitsToFloat (XEiJ.busRls (a7 + 4));
    float zf = xf % yf;
    int h = Float.floatToIntBits (zf);
    if (FPK_FPCP_NAN && h == 0x7fc00000) {
      h = 0x7fffffff;
    }
    XEiJ.busWl (a7, h);
    XEiJ.regCCR = (Float.isNaN (xf) || Float.isNaN (yf) ? 0 :  //引数がNaN
           yf == 0.0F ? XEiJ.REG_CCR_Z | XEiJ.REG_CCR_C :  //除数が0のときはゼロ除算。(±Inf)%(±0)=NaN, x%(±0)=(±Inf)
           Float.isNaN (zf) ? XEiJ.REG_CCR_C :  //引数がNaNでないのに結果がNaNのときはエラー。(±Inf)%y=NaN
           Float.isInfinite (xf) || Float.isInfinite (yf) ? 0 :  //引数が±Inf
           Float.isInfinite (zf) ? XEiJ.REG_CCR_V | XEiJ.REG_CCR_C :  //引数が±Infでないのに結果が±Infのときはオーバーフロー
           0);
  }  //fpkCFMOD()

  //fpkCDTST ()
  //  $FEF8  __CDTST
  //  64bit浮動小数点数と0の比較
  //  x<=>0
  //  <(a7).d:64bit浮動小数点数。x
  //  >ccr:lt=x<0,eq=x==0,gt=x>0
  public static void fpkCDTST () throws M68kException {
    if (true) {
      int a7 = XEiJ.regRn[15];
      long l = (long) XEiJ.busRls (a7) << 32 | 0xffffffffL & XEiJ.busRls (a7 + 4);
      XEiJ.regCCR = l << 1 == 0L ? XEiJ.REG_CCR_Z : 0L <= l ? 0 : XEiJ.REG_CCR_N;  //NaNのときは0
    } else {
      //[int,int]→[long]→double
      int a7 = XEiJ.regRn[15];
      double d = Double.longBitsToDouble ((long) XEiJ.busRls (a7) << 32 | 0xffffffffL & XEiJ.busRls (a7 + 4));
      XEiJ.regCCR = d < 0.0 ? XEiJ.REG_CCR_N : d == 0.0 ? XEiJ.REG_CCR_Z : 0;  //NaNのときは0
    }
  }  //fpkCDTST()

  //fpkCFTST ()
  //  $FEF9  __CFTST
  //  32bit浮動小数点数と0の比較
  //  x<=>0
  //  <(a7).s:32bit浮動小数点数。x
  //  >ccr:lt=x<0,eq=x==0,gt=x>0
  public static void fpkCFTST () throws M68kException {
    //[int]→float
    if (true) {
      int h = XEiJ.busRls (XEiJ.regRn[15]);
      XEiJ.regCCR = h << 1 == 0 ? XEiJ.REG_CCR_Z : 0 <= h ? 0 : XEiJ.REG_CCR_N;  //NaNのときは0
    } else {
      //([int]→float)<=>0
      float f = Float.intBitsToFloat (XEiJ.busRls (XEiJ.regRn[15]));
      XEiJ.regCCR = f < 0.0F ? XEiJ.REG_CCR_N : f == 0.0F ? XEiJ.REG_CCR_Z : 0;  //NaNのときは0
    }
  }  //fpkCFTST()

  //fpkCDINC ()
  //  $FEFA  __CDINC
  //  64bit浮動小数点数に1を加える
  //  <(a7).d:64bit浮動小数点数。x
  //  >(a7).d:64bit浮動小数点数。x+1
  public static void fpkCDINC () throws M68kException {
    //([int,int]→[long]→double)+1→[long]→[int,int]
    int a7 = XEiJ.regRn[15];
    double xd = Double.longBitsToDouble ((long) XEiJ.busRls (a7) << 32 | 0xffffffffL & XEiJ.busRls (a7 + 4));
    double zd = xd + 1.0;
    long l = Double.doubleToLongBits (zd);
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.busWl (a7, (int) (l >>> 32));
    XEiJ.busWl (a7 + 4, (int) l);
    XEiJ.regCCR = Double.isInfinite (zd) && !Double.isInfinite (xd) ? XEiJ.REG_CCR_V | XEiJ.REG_CCR_C : 0;  //結果が±Infだが引数が±Infでないときはオーバーフロー
  }  //fpkCDINC()

  //fpkCFINC ()
  //  $FEFB  __CFINC
  //  32bit浮動小数点数に1を加える
  //  <(a7).s:32bit浮動小数点数。x
  //  >(a7).s:32bit浮動小数点数。x+1
  public static void fpkCFINC () throws M68kException {
    //([int]→float)+1→[int]
    int a7 = XEiJ.regRn[15];
    float xf = Float.intBitsToFloat (XEiJ.busRls (a7));
    float zf = xf + 1.0F;
    int h = Float.floatToIntBits (zf);
    if (FPK_FPCP_NAN && h == 0x7fc00000) {
      h = 0x7fffffff;
    }
    XEiJ.busWl (a7, h);
    XEiJ.regCCR = Double.isInfinite (zf) && !Float.isInfinite (xf) ? XEiJ.REG_CCR_V | XEiJ.REG_CCR_C : 0;  //結果が±Infだが引数が±Infでないときはオーバーフロー
  }  //fpkCFINC()

  //fpkCDDEC ()
  //  $FEFC  __CDDEC
  //  64bit浮動小数点数から1を引く
  //  <(a7).d:64bit浮動小数点数。x
  //  >(a7).d:64bit浮動小数点数。x-1
  public static void fpkCDDEC () throws M68kException {
    //([int,int]→[long]→double)-1→[long]→[int,int]
    int a7 = XEiJ.regRn[15];
    double xd = Double.longBitsToDouble ((long) XEiJ.busRls (a7) << 32 | 0xffffffffL & XEiJ.busRls (a7 + 4));
    double zd = xd - 1.0;
    long l = Double.doubleToLongBits (zd);
    if (FPK_FPCP_NAN && l == 0x7ff8000000000000L) {
      l = 0x7fffffffffffffffL;
    }
    XEiJ.busWl (a7, (int) (l >>> 32));
    XEiJ.busWl (a7 + 4, (int) l);
    XEiJ.regCCR = Double.isInfinite (zd) && !Double.isInfinite (xd) ? XEiJ.REG_CCR_V | XEiJ.REG_CCR_C : 0;  //結果が±Infだが引数が±Infでないときはオーバーフロー
  }  //fpkCDDEC()

  //fpkCFDEC ()
  //  $FEFD  __CFDEC
  //  32bit浮動小数点数から1を引く
  //  <(a7).s:32bit浮動小数点数。x
  //  >(a7).s:32bit浮動小数点数。x-1
  public static void fpkCFDEC () throws M68kException {
    //([int]→float)-1→[int]
    int a7 = XEiJ.regRn[15];
    float xf = Float.intBitsToFloat (XEiJ.busRls (a7));
    float zf = xf - 1.0F;
    int h = Float.floatToIntBits (zf);
    if (FPK_FPCP_NAN && h == 0x7fc00000) {
      h = 0x7fffffff;
    }
    XEiJ.busWl (a7, h);
    XEiJ.regCCR = Double.isInfinite (zf) && !Float.isInfinite (xf) ? XEiJ.REG_CCR_V | XEiJ.REG_CCR_C : 0;  //結果が±Infだが引数が±Infでないときはオーバーフロー
  }  //fpkCFDEC()

}  //class FEFunction



