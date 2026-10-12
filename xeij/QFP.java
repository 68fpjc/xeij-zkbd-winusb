//========================================================================================
//  QFP.java
//    en:Quad-precision floating-point number
//    ja:四倍精度浮動小数点数
//  Copyright (C) 2003-2023 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//  Double-Doubleにフラグと指数部の下駄を加えたもの
//  メモ
//    Double-Doubleの弱点は表現できる数の範囲(指数部の範囲)がdoubleと同じであることだが、doubleと同じ範囲の計算ができるわけではない
//    doubleではオーバーフローしない上限に近い数の計算がDouble-Doubleでは途中でオーバーフローしてしまい完遂できないことがある
//----------------------------------------------------------------------------------------

package xeij;

import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System

public class QFP {

  //定数
  //  flg
  public static final int QFP_P  = 0x00000000;  //+
  public static final int QFP_M  = 0x08000000;  //-
  public static final int QFP_Z  = 0x04000000;  //±0
  public static final int QFP_I  = 0x02000000;  //±Inf
  public static final int QFP_N  = 0x01000000;  //NaN
  public static final int QFP_ZIN = QFP_Z | QFP_I | QFP_N;  //±0,±Inf,NaN
  public static final int QFP_MZIN = QFP_M | QFP_Z | QFP_I | QFP_N;  //-,±0,±Inf,NaN
  //  fpsr
  public static final int QFP_BS = 0x00008000;  //比較不能な分岐/セット
  public static final int QFP_SN = 0x00004000;  //シグナリングNaN
  public static final int QFP_OE = 0x00002000;  //オペランドエラー
  public static final int QFP_OF = 0x00001000;  //オーバーフロー
  public static final int QFP_UF = 0x00000800;  //アンダーフロー
  public static final int QFP_DZ = 0x00000400;  //ゼロ除算
  public static final int QFP_X2 = 0x00000200;  //不正確な結果
  public static final int QFP_X1 = 0x00000100;  //不正確な10進数
  public static final int QFP_EXC = 0x0000ff00;
  //  指数部の下駄
  //  加減算の軽量化のため、dvlの指数部の範囲は1.5倍+αしたときdoubleに収まらなければならない
  //  dvlの指数部の範囲はfloatをカバーできるものとする
  private static final int QFP_GETA_SIZE = 512;  //dvlの指数部の上限-下限+1
  private static final int QFP_GETA_BASE = -QFP_GETA_SIZE >> 1;  //dvlの指数部の下限

  //  echo read("efp.gp");qfppub("QFP_ONE",eval("1"),"1") | gp-2.7 -q
  public static final QFP QFP_ONE = new QFP (QFP_P, 0, 0x1.0p0, 0.0);  //=1=1
  //  echo read("efp.gp");qfppub("QFP_TEN",eval("10"),"10") | gp-2.7 -q
  public static final QFP QFP_TEN = new QFP (QFP_P, 0, 0x1.4p3, 0.0);  //=10=10
  //  echo read("efp.gp");qfppub("QFP_TENTO4",eval("10^4"),"10^4") | gp-2.7 -q
  public static final QFP QFP_TENTO4 = new QFP (QFP_P, 0, 0x1.388p13, 0.0);  //=10^4=10000
  //  echo read("efp.gp");qfppub("QFP_PI",Pi,"pi") | gp-2.7 -q
  public static final QFP QFP_PI = new QFP (QFP_P, 0, 0x1.921fb54442d18p1, 0x1.1a62633145c07p-53);  //>pi=3.14159265358979323846264338328
  //  echo read("efp.gp");qfppub("QFP_LN_2_2",log(2)/2,"log(2)/2") | gp-2.7 -q
  public static final QFP QFP_LN_2_2 = new QFP (QFP_P, 0, 0x1.62e42fefa39efp-2, 0x1.abc9e3b39803fp-57);  //<log(2)/2=0.34657359027997265470861606072909

  //クラスフィールド
  public static int fpsr;

  //インスタンスフィールド
  public int flg;  //フラグ。±0,±Inf,NaN以外のときMはdvlの符号のコピー
  public int epp;  //指数部の下駄。指数部-QFP_GETA_BASE&-QFP_GETA_SIZE
  public double dvl;  //上位。符号を含む。指数部の範囲はQFP_GETA_BASE..QFP_GETA_BASE+QFP_GETA_SIZE-1
  public double cvl;  //下位。符号を含む

  //公開コンストラクタ
  public QFP () {
    this.set0 ();
  }  //new QFP()
  public QFP (double d) {
    this.setd (d);
  }  //new QFP(double)
  public QFP (float f) {
    this.setf (f);
  }  //new QFP(float)
  public QFP (int i) {
    this.seti (i);
  }  //new QFP(int)
  public QFP (long l) {
    this.setl (l);
  }  //new QFP(long)
  public QFP (QFP x) {
    this.flg = x.flg;
    this.epp = x.epp;
    this.dvl = x.dvl;
    this.cvl = x.cvl;
  }  //new QFP(QFP)

  //内部コンストラクタ
  private QFP (int f, int e, double d, double c) {
    this.flg = f;
    this.epp = e;
    this.dvl = d;
    this.cvl = c;
  }  //new QFP(int,int,double,double)

  //qfp = qfp.abs ()
  //qfp = qfp.abs (x)
  //  絶対値
  public QFP abs () {
    if ((this.flg & QFP_M) != 0) {  //-x,-0,-Inf
      this.flg ^= QFP_M;
      this.dvl = -this.dvl;
      this.cvl = -this.cvl;
    }
    return this;
  }  //qfp.abs()
  public QFP abs (QFP x) {
    if ((x.flg & QFP_M) != 0) {  //-x,-0,-Inf
      this.flg = x.flg ^ QFP_M;
      this.epp = x.epp;
      this.dvl = -x.dvl;
      this.cvl = -x.cvl;
    } else {
      this.flg = x.flg;
      this.epp = x.epp;
      this.dvl = x.dvl;
      this.cvl = x.cvl;
    }
    return this;
  }  //qfp.abs(QFP)

  //qfp = qfp.add (y)
  //qfp = qfp.add (x, y)
  //  加算
  //  フラグ
  //    (NaN)+(NaN,+Inf,0<y,+0,-0,y<0,-Inf)=NaN
  //    (NaN,+Inf,0<x,+0,-0,x<0,-Inf)+(NaN)=NaN
  //    (+Inf)+(-Inf)=NaN,OE
  //    (-Inf)+(+Inf)=NaN,OE
  //    (+Inf)+(+Inf,0<y,+0,-0,y<0)=+Inf
  //    (-Inf)+(0<y,+0,-0,y<0,-Inf)=-Inf
  //    (+Inf,0<x,+0,-0,x<0)+(+Inf)=+Inf
  //    (0<x,+0,-0,x<0,-Inf)+(-Inf)=-Inf
  //    (+0)+(+0)=+0
  //    (+0)+(-0)=+0
  //    (-0)+(+0)=+0
  //    (-0)+(-0)=-0
  //    (+0,-0)+(0<y,y<0)=y
  //    (0<x,x<0)+(+0,-0)=x
  //    (0<x,x<0)+(0<y,y<0)=z
  //    +------+------+------+------+------+------+------+
  //    |      | +Inf |  0<y |   +0 |   -0 |  y<0 | -Inf |
  //    +------+------+------+------+------+------+------+
  //    | +Inf | +Inf | +Inf | +Inf | +Inf | +Inf |  NaN |
  //    |  0<x | +Inf |    z |    x |    x |    z | -Inf |
  //    |   +0 | +Inf |    y |   +0 |   +0 |    y | -Inf |
  //    |   -0 | +Inf |    y |   +0 |   -0 |    y | -Inf |
  //    |  x<0 | +Inf |    z |    x |    x |    z | -Inf |
  //    | -Inf |  NaN | -Inf | -Inf | -Inf | -Inf | -Inf |
  //    +------+------+------+------+------+------+------+
  private static final int[] QFP_ADD_FLG = {
    //  perl -e "use strict;my($M,$Z,$I,$N,$OE,$DZ)=(0x08000000,0x04000000,0x02000000,0x01000000,0x00002000,0x00000400);for my$a(0..15){my$x=$a<<24;for my$b(0..15){my$y=$b<<24;($b&7)==0 and print'      ';printf('0x%08x,',0b11101010>>($x>>24&7)&1||0b11101010>>($y>>24&7)&1?$N:($x&$y)&$I&&$x!=$y?$N|$OE:($x|$y)&$I?$x&$I?$x:$y:($x&$y)&$Z?($x&$y)&$M|$Z:$x&$Z?0xc0000000:$y&$Z?0x80000000:0);($b&7)==7 and print chr 10;}}"
    0x00000000,0x01000000,0x02000000,0x01000000,0x80000000,0x01000000,0x01000000,0x01000000,
    0x00000000,0x01000000,0x0a000000,0x01000000,0x80000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x02000000,0x01000000,0x02000000,0x01000000,0x02000000,0x01000000,0x01000000,0x01000000,
    0x02000000,0x01000000,0x01002000,0x01000000,0x02000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0xc0000000,0x01000000,0x02000000,0x01000000,0x04000000,0x01000000,0x01000000,0x01000000,
    0xc0000000,0x01000000,0x0a000000,0x01000000,0x04000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x00000000,0x01000000,0x02000000,0x01000000,0x80000000,0x01000000,0x01000000,0x01000000,
    0x00000000,0x01000000,0x0a000000,0x01000000,0x80000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x0a000000,0x01000000,0x01002000,0x01000000,0x0a000000,0x01000000,0x01000000,0x01000000,
    0x0a000000,0x01000000,0x0a000000,0x01000000,0x0a000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0xc0000000,0x01000000,0x02000000,0x01000000,0x04000000,0x01000000,0x01000000,0x01000000,
    0xc0000000,0x01000000,0x0a000000,0x01000000,0x0c000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
  };
  public QFP add (QFP y) {
    int xf = this.flg;
    int xe = this.epp;
    double xd = this.dvl;
    double xc = this.cvl;
    int yf = y.flg;
    int ye = y.epp;
    double yd = y.dvl;
    double yc = y.cvl;
    //フラグを設定する
    int zf = QFP_ADD_FLG[xf >>> 24 - 4 | yf >>> 24];
    if ((zf & (0xc0000000 | QFP_ZIN)) != 0) {  //x,y,±0,±Inf,NaN
      if (0 <= zf) {  //±0,±Inf,NaN
        QFP.fpsr |= zf & QFP_EXC;
        this.flg = zf & QFP_MZIN;
      } else if (zf << 1 < 0) {  //y
        this.flg = yf;
        this.epp = ye;
        this.dvl = yd;
        this.cvl = yc;
        //} else {  //x
        //  this.flg = xf;
        //  this.epp = xe;
        //  this.dvl = xd;
        //  this.cvl = xc;
      }
      return this;
    }
    //両方±0,±Inf,NaN以外
    //指数部の下駄を合わせる
    if (xe != ye) {
      if (xe + 128 == ye) {
        yd = Math.scalb (yd, 128);
        yc = Math.scalb (yc, 128);
      } else if (xe - 128 == ye) {
        yd = Math.scalb (yd, -128);
        yc = Math.scalb (yc, -128);
      } else {
        if (xe < ye) {  //yが大きすぎるときy
          this.flg = yf;
          this.epp = ye;
          this.dvl = yd;
          this.cvl = yc;
          //} else {  //xが大きすぎるときx
          //  this.flg = xf;
          //  this.epp = xe;
          //  this.dvl = xd;
          //  this.cvl = xc;
        }
        return this;
      }
    }
    //加減算を行う
    {
      double t1 = xd + yd;
      double t2 = xd - t1;
      t2 = (((xd - (t1 + t2)) + (t2 + yd)) + xc) + yc;
      xd = t1 + t2;
      xc = (t1 - xd) + t2;
    }
    //フラグを設定する
    //  両方±0,±Inf,NaN以外の加減算で結果がNaNになることはない
    //  両方絶対値が2^192未満の加減算で結果が±Infになることはない
    if (xd == 0.0) {  //±0
      this.flg = QFP_P | QFP_Z;  //+0
      return this;
    }
    this.flg = (int) (Double.doubleToLongBits (xd) >>> 36) & QFP_M;
    //指数部の下駄を設定する
    int e = Math.getExponent (xd) - QFP_GETA_BASE & -QFP_GETA_SIZE;
    if (e != 0) {
      xe += e;
      xd = Math.scalb (xd, -e);
      xc = Math.scalb (xc, -e);
    }
    //結果を格納する
    this.epp = xe;
    this.dvl = xd;
    this.cvl = xc;
    return this;
  }  //qfp.add(QFP)
  public QFP add (QFP x, QFP y) {
    int xf = x.flg;
    int xe = x.epp;
    double xd = x.dvl;
    double xc = x.cvl;
    int yf = y.flg;
    int ye = y.epp;
    double yd = y.dvl;
    double yc = y.cvl;
    //フラグを設定する
    int zf = QFP_ADD_FLG[xf >>> 24 - 4 | yf >>> 24];
    if ((zf & (0xc0000000 | QFP_ZIN)) != 0) {  //x,y,±0,±Inf,NaN
      if (0 <= zf) {  //±0,±Inf,NaN
        QFP.fpsr |= zf & QFP_EXC;
        this.flg = zf & QFP_MZIN;
      } else if (zf << 1 < 0) {  //y
        this.flg = yf;
        this.epp = ye;
        this.dvl = yd;
        this.cvl = yc;
      } else {  //x
        this.flg = xf;
        this.epp = xe;
        this.dvl = xd;
        this.cvl = xc;
      }
      return this;
    }
    //両方±0,±Inf,NaN以外
    //指数部の下駄を合わせる
    if (xe != ye) {
      if (xe + 128 == ye) {
        yd = Math.scalb (yd, 128);
        yc = Math.scalb (yc, 128);
      } else if (xe - 128 == ye) {
        yd = Math.scalb (yd, -128);
        yc = Math.scalb (yc, -128);
      } else {
        if (xe < ye) {  //yが大きすぎるときy
          this.flg = yf;
          this.epp = ye;
          this.dvl = yd;
          this.cvl = yc;
        } else {  //xが大きすぎるときx
          this.flg = xf;
          this.epp = xe;
          this.dvl = xd;
          this.cvl = xc;
        }
        return this;
      }
    }
    //加減算を行う
    {
      double t1 = xd + yd;
      double t2 = xd - t1;
      t2 = (((xd - (t1 + t2)) + (t2 + yd)) + xc) + yc;
      xd = t1 + t2;
      xc = (t1 - xd) + t2;
    }
    //フラグを設定する
    //  両方±0,±Inf,NaN以外の加減算で結果がNaNになることはない
    //  両方絶対値が2^192未満の加減算で結果が±Infになることはない
    if (xd == 0.0) {  //±0
      this.flg = QFP_P | QFP_Z;  //+0
      return this;
    }
    this.flg = (int) (Double.doubleToLongBits (xd) >>> 36) & QFP_M;
    //指数部の下駄を設定する
    int e = Math.getExponent (xd) - QFP_GETA_BASE & -QFP_GETA_SIZE;
    if (e != 0) {
      xe += e;
      xd = Math.scalb (xd, -e);
      xc = Math.scalb (xc, -e);
    }
    //結果を格納する
    this.epp = xe;
    this.dvl = xd;
    this.cvl = xc;
    return this;
  }  //qfp.add(QFP,QFP)

  //qfp = qfp.atanh (x)
  private static final QFP[] QFP_ATH_T = {
    //  1-2/(2^n*sqrt(2)+1) == (2^(2*n+1)+1-2^(n+1)*sqrt(2))/(2^(2*n+1)-1)
    //  echo read("efp.gp");for(n=0,106,qfpmem(eval("1-2/(2^n*sqrt(2)+1)"),Str("1-2/(2^",n,"*sqrt(2)+1)"))) | gp-2.7 -q
    new QFP (QFP_P, 0, 0x1.5f619980c4337p-3, -0x1.165f626cdd52bp-60),  //<1-2/(2^0*sqrt(2)+1)=0.17157287525380990239662255158060
    new QFP (QFP_P, 0, 0x1.e90df15b89be3p-2, 0x1.1f99b9abc530dp-56),  //>1-2/(2^1*sqrt(2)+1)=0.47759225007251711497046358616589
    new QFP (QFP_P, 0, 0x1.662c704e79f12p-1, 0x1.282af86097e1cp-55),  //>1-2/(2^2*sqrt(2)+1)=0.69955779035533030998666097439750
    new QFP (QFP_P, 0, 0x1.acd734cfa2558p-1, -0x1.d43396def4576p-55),  //<1-2/(2^3*sqrt(2)+1)=0.83757939371677542692262189301295
    new QFP (QFP_P, 0, 0x1.d4a917bee0f8ep-1, 0x1.a848f4c92e287p-56),  //>1-2/(2^4*sqrt(2)+1)=0.91535257535041283451731107793599
    new QFP (QFP_P, 0, 0x1.e9dc9d2d2669p-1, 0x1.1f0d824ddcde5p-58),  //>1-2/(2^5*sqrt(2)+1)=0.95676127601764627106824226753814
    new QFP (QFP_P, 0, 0x1.f4cf57477a9dfp-1, -0x1.58d05e5b8e13cp-57),  //>1-2/(2^6*sqrt(2)+1)=0.97814438579126405002483016033466
    new QFP (QFP_P, 0, 0x1.fa5fcd25fa7dp-1, 0x1.d5e0dfd5720f4p-57),  //>1-2/(2^7*sqrt(2)+1)=0.98901215637783403019827166620693
    new QFP (QFP_P, 0, 0x1.fd2deaca257dap-1, -0x1.b4d133e623a68p-55),  //>1-2/(2^8*sqrt(2)+1)=0.99449094503028873919489082537865
    new QFP (QFP_P, 0, 0x1.fe9675ec66c9dp-1, 0x1.0314f0b167a78p-61),  //<1-2/(2^9*sqrt(2)+1)=0.99724167357216553275215115146172
    new QFP (QFP_P, 0, 0x1.ff4b1b0724de6p-1, -0x1.4f6460da8573bp-55),  //>1-2/(2^10*sqrt(2)+1)=0.99861988508422135618277088368592
    new QFP (QFP_P, 0, 0x1.ffa58585b10e2p-1, -0x1.e812fa2f88614p-56),  //<1-2/(2^11*sqrt(2)+1)=0.99930970437028696214760189421028
    new QFP (QFP_P, 0, 0x1.ffd2c0c31c61fp-1, -0x1.82bc0859369f6p-55),  //>1-2/(2^12*sqrt(2)+1)=0.99965479261135554959522980932001
    new QFP (QFP_P, 0, 0x1.ffe95fe196accp-1, 0x1.991433745dacbp-59),  //<1-2/(2^13*sqrt(2)+1)=0.99982738140837446316402328234885
    new QFP (QFP_P, 0, 0x1.fff4afd0cc65ep-1, 0x1.4035d2da50783p-58),  //>1-2/(2^14*sqrt(2)+1)=0.99991368697937921695563957694829
    new QFP (QFP_P, 0, 0x1.fffa57e06654ep-1, 0x1.9b510deb5e06ep-58),  //<1-2/(2^15*sqrt(2)+1)=0.99995684255842732237712901779679
    new QFP (QFP_P, 0, 0x1.fffd2bee332ebp-1, -0x1.82787a0eb7631p-57),  //<1-2/(2^16*sqrt(2)+1)=0.99997842104639055378876556861730
    new QFP (QFP_P, 0, 0x1.fffe95f69997ep-1, -0x1.fbebab6e35a44p-58),  //>1-2/(2^17*sqrt(2)+1)=0.99998921046498855802453226317115
    new QFP (QFP_P, 0, 0x1.ffff4afb2cccp-1, 0x1.e5d3881a1907dp-59),  //>1-2/(2^18*sqrt(2)+1)=0.99999460521794248154009253151716
    new QFP (QFP_P, 0, 0x1.ffffa57d8e66p-1, 0x1.2de46d4fa2f96p-56),  //>1-2/(2^19*sqrt(2)+1)=0.99999730260533327668252503098688
    new QFP (QFP_P, 0, 0x1.ffffd2bec533p-1, 0x1.71c6418321567p-57),  //<1-2/(2^20*sqrt(2)+1)=0.99999865130175714547943879214761
    new QFP (QFP_P, 0, 0x1.ffffe95f62198p-1, 0x1.82beb77000cdfp-58),  //>1-2/(2^21*sqrt(2)+1)=0.99999932565065119929426999557121
    new QFP (QFP_P, 0, 0x1.fffff4afb0eccp-1, 0x1.86fcd50738a9dp-59),  //>1-2/(2^22*sqrt(2)+1)=0.99999966282526875625702343002444
    new QFP (QFP_P, 0, 0x1.fffffa57d86e6p-1, 0x1.880c5c7086a0ap-60),  //<1-2/(2^23*sqrt(2)+1)=0.99999983141262016727739016874704
    new QFP (QFP_P, 0, 0x1.fffffd2bec353p-1, 0x1.88503e4b4a1e5p-61),  //<1-2/(2^24*sqrt(2)+1)=0.99999991570630653092546549088408
    new QFP (QFP_P, 0, 0x1.fffffe95f61a2p-1, -0x1.fcef3d927beep-55),  //>1-2/(2^25*sqrt(2)+1)=0.99999995785315237728436919619601
    new QFP (QFP_P, 0, 0x1.ffffff4afb0cfp-1, -0x1.fcef3516408b1p-56),  //>1-2/(2^26*sqrt(2)+1)=0.99999997892657596659758669192677
    new QFP (QFP_P, 0, 0x1.ffffffa57d867p-1, -0x1.fcef32f731b1fp-57),  //<1-2/(2^27*sqrt(2)+1)=0.99999998946328792778764299206308
    new QFP (QFP_P, 0, 0x1.ffffffd2bec33p-1, 0x1.406219b212409p-55),  //>1-2/(2^28*sqrt(2)+1)=0.99999999473164395001603379788677
    new QFP (QFP_P, 0, 0x1.ffffffe95f61ap-1, -0x1.7fcef324d7d0ep-55),  //<1-2/(2^29*sqrt(2)+1)=0.99999999736582197153856996069848
    new QFP (QFP_P, 0, 0x1.fffffff4afb0dp-1, -0x1.8fcef324500d3p-56),  //<1-2/(2^30*sqrt(2)+1)=0.99999999868291098490192324407443
    new QFP (QFP_P, 0, 0x1.fffffffa57d86p-1, 0x1.9a0c4336f478fp-55),  //<1-2/(2^31*sqrt(2)+1)=0.99999999934145549223412118775431
    new QFP (QFP_P, 0, 0x1.fffffffd2bec3p-1, 0x1.990c4336f698p-56),  //>1-2/(2^32*sqrt(2)+1)=0.99999999967072774606285048527966
    new QFP (QFP_P, 0, 0x1.fffffffe95f62p-1, -0x1.99dcef3242381p-55),  //<1-2/(2^33*sqrt(2)+1)=0.99999999983536387301787271548711
    new QFP (QFP_P, 0, 0x1.ffffffff4afb1p-1, -0x1.99ecef32422f9p-56),  //>1-2/(2^34*sqrt(2)+1)=0.99999999991768193650554822595495
    new QFP (QFP_P, 0, 0x1.ffffffffa57d8p-1, 0x1.9982c4336f74ap-55),  //<1-2/(2^35*sqrt(2)+1)=0.99999999995884096825192708003027
    new QFP (QFP_P, 0, 0x1.ffffffffd2becp-1, 0x1.9981c4336f74cp-56),  //<1-2/(2^36*sqrt(2)+1)=0.99999999997942048412575178177833
    new QFP (QFP_P, 0, 0x1.ffffffffe95f6p-1, 0x1.998144336f74dp-57),  //>1-2/(2^37*sqrt(2)+1)=0.99999999998971024206282295132996
    new QFP (QFP_P, 0, 0x1.fffffffff4afbp-1, 0x1.998104336f74dp-58),  //>1-2/(2^38*sqrt(2)+1)=0.99999999999485512103139824077518
    new QFP (QFP_P, 0, 0x1.fffffffffa57ep-1, -0x1.e667f1bcc908bp-55),  //>1-2/(2^39*sqrt(2)+1)=0.99999999999742756051569581166514
    new QFP (QFP_P, 0, 0x1.fffffffffd2bfp-1, -0x1.e667f2bcc908bp-56),  //>1-2/(2^40*sqrt(2)+1)=0.99999999999871378025784707865196
    new QFP (QFP_P, 0, 0x1.fffffffffe95fp-1, 0x1.86660330cdbddp-55),  //<1-2/(2^41*sqrt(2)+1)=0.99999999999935689012892333253082
    new QFP (QFP_P, 0, 0x1.ffffffffff4bp-1, -0x1.3cccfe6f99211p-55),  //>1-2/(2^42*sqrt(2)+1)=0.99999999999967844506446161456663
    new QFP (QFP_P, 0, 0x1.ffffffffffa58p-1, -0x1.3cccfe7399211p-56),  //>1-2/(2^43*sqrt(2)+1)=0.99999999999983922253223079435862
    new QFP (QFP_P, 0, 0x1.ffffffffffd2cp-1, -0x1.3cccfe7599211p-57),  //>1-2/(2^44*sqrt(2)+1)=0.99999999999991961126611539394813
    new QFP (QFP_P, 0, 0x1.ffffffffffe96p-1, -0x1.3cccfe7699211p-58),  //>1-2/(2^45*sqrt(2)+1)=0.99999999999995980563305769616627
    new QFP (QFP_P, 0, 0x1.fffffffffff4bp-1, -0x1.3cccfe7719211p-59),  //>1-2/(2^46*sqrt(2)+1)=0.99999999999997990281652884788119
    new QFP (QFP_P, 0, 0x1.fffffffffffa5p-1, 0x1.f619980c4536fp-55),  //<1-2/(2^47*sqrt(2)+1)=0.99999999999998995140826442389010
    new QFP (QFP_P, 0, 0x1.fffffffffffd3p-1, -0x1.04f333f9dde48p-55),  //>1-2/(2^48*sqrt(2)+1)=0.99999999999999497570413221193243
    new QFP (QFP_P, 0, 0x1.fffffffffffe9p-1, 0x1.7d86660310edcp-55),  //>1-2/(2^49*sqrt(2)+1)=0.99999999999999748785206610596306
    new QFP (QFP_P, 0, 0x1.ffffffffffff5p-1, -0x1.413cccfe77912p-55),  //>1-2/(2^50*sqrt(2)+1)=0.99999999999999874392603305298074
    new QFP (QFP_P, 0, 0x1.ffffffffffffap-1, 0x1.5f619980c4357p-55),  //>1-2/(2^51*sqrt(2)+1)=0.99999999999999937196301652649017
    new QFP (QFP_P, 0, 0x1.ffffffffffffdp-1, 0x1.5f619980c4347p-56),  //>1-2/(2^52*sqrt(2)+1)=0.99999999999999968598150826324504
    new QFP (QFP_P, 0, 0x1.fffffffffffffp-1, -0x1.a827999fcef3p-55),  //>1-2/(2^53*sqrt(2)+1)=0.99999999999999984299075413162251
    new QFP (QFP_P, 0, 0x1.fffffffffffffp-1, 0x1.2bec333018867p-55),  //<1-2/(2^54*sqrt(2)+1)=0.99999999999999992149537706581125
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bccp-55),  //>1-2/(2^55*sqrt(2)+1)=0.99999999999999996074768853290563
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-56),  //<1-2/(2^56*sqrt(2)+1)=0.99999999999999998037384426645281
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-57),  //<1-2/(2^57*sqrt(2)+1)=0.99999999999999999018692213322641
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-58),  //<1-2/(2^58*sqrt(2)+1)=0.99999999999999999509346106661320
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-59),  //<1-2/(2^59*sqrt(2)+1)=0.99999999999999999754673053330660
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-60),  //<1-2/(2^60*sqrt(2)+1)=0.99999999999999999877336526665330
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-61),  //<1-2/(2^61*sqrt(2)+1)=0.99999999999999999938668263332665
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-62),  //<1-2/(2^62*sqrt(2)+1)=0.99999999999999999969334131666333
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-63),  //<1-2/(2^63*sqrt(2)+1)=0.99999999999999999984667065833166
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-64),  //<1-2/(2^64*sqrt(2)+1)=0.99999999999999999992333532916583
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-65),  //<1-2/(2^65*sqrt(2)+1)=0.99999999999999999996166766458292
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-66),  //<1-2/(2^66*sqrt(2)+1)=0.99999999999999999998083383229146
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-67),  //<1-2/(2^67*sqrt(2)+1)=0.99999999999999999999041691614573
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-68),  //<1-2/(2^68*sqrt(2)+1)=0.99999999999999999999520845807286
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-69),  //<1-2/(2^69*sqrt(2)+1)=0.99999999999999999999760422903643
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-70),  //<1-2/(2^70*sqrt(2)+1)=0.99999999999999999999880211451822
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-71),  //<1-2/(2^71*sqrt(2)+1)=0.99999999999999999999940105725911
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-72),  //<1-2/(2^72*sqrt(2)+1)=0.99999999999999999999970052862955
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-73),  //<1-2/(2^73*sqrt(2)+1)=0.99999999999999999999985026431478
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-74),  //<1-2/(2^74*sqrt(2)+1)=0.99999999999999999999992513215739
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-75),  //<1-2/(2^75*sqrt(2)+1)=0.99999999999999999999996256607869
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-76),  //<1-2/(2^76*sqrt(2)+1)=0.99999999999999999999998128303935
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-77),  //<1-2/(2^77*sqrt(2)+1)=0.99999999999999999999999064151967
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-78),  //<1-2/(2^78*sqrt(2)+1)=0.99999999999999999999999532075984
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-79),  //<1-2/(2^79*sqrt(2)+1)=0.99999999999999999999999766037992
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-80),  //<1-2/(2^80*sqrt(2)+1)=0.99999999999999999999999883018996
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-81),  //<1-2/(2^81*sqrt(2)+1)=0.99999999999999999999999941509498
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-82),  //<1-2/(2^82*sqrt(2)+1)=0.99999999999999999999999970754749
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-83),  //<1-2/(2^83*sqrt(2)+1)=0.99999999999999999999999985377374
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-84),  //<1-2/(2^84*sqrt(2)+1)=0.99999999999999999999999992688687
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-85),  //<1-2/(2^85*sqrt(2)+1)=0.99999999999999999999999996344344
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-86),  //<1-2/(2^86*sqrt(2)+1)=0.99999999999999999999999998172172
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-87),  //<1-2/(2^87*sqrt(2)+1)=0.99999999999999999999999999086086
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-88),  //<1-2/(2^88*sqrt(2)+1)=0.99999999999999999999999999543043
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-89),  //<1-2/(2^89*sqrt(2)+1)=0.99999999999999999999999999771521
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-90),  //<1-2/(2^90*sqrt(2)+1)=0.99999999999999999999999999885761
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-91),  //<1-2/(2^91*sqrt(2)+1)=0.99999999999999999999999999942880
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-92),  //<1-2/(2^92*sqrt(2)+1)=0.99999999999999999999999999971440
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-93),  //<1-2/(2^93*sqrt(2)+1)=0.99999999999999999999999999985720
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-94),  //<1-2/(2^94*sqrt(2)+1)=0.99999999999999999999999999992860
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-95),  //<1-2/(2^95*sqrt(2)+1)=0.99999999999999999999999999996430
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-96),  //<1-2/(2^96*sqrt(2)+1)=0.99999999999999999999999999998215
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-97),  //<1-2/(2^97*sqrt(2)+1)=0.99999999999999999999999999999108
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-98),  //<1-2/(2^98*sqrt(2)+1)=0.99999999999999999999999999999554
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-99),  //<1-2/(2^99*sqrt(2)+1)=0.99999999999999999999999999999777
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-100),  //<1-2/(2^100*sqrt(2)+1)=0.99999999999999999999999999999888
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-101),  //<1-2/(2^101*sqrt(2)+1)=0.99999999999999999999999999999944
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-102),  //<1-2/(2^102*sqrt(2)+1)=0.99999999999999999999999999999972
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-103),  //<1-2/(2^103*sqrt(2)+1)=0.99999999999999999999999999999986
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-104),  //<1-2/(2^104*sqrt(2)+1)=0.99999999999999999999999999999993
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-105),  //<1-2/(2^105*sqrt(2)+1)=0.99999999999999999999999999999997
    new QFP (QFP_P, 0, 0x1.0p0, -0x1.6a09e667f3bcdp-106),  //<1-2/(2^106*sqrt(2)+1)=0.99999999999999999999999999999998
  };
  //  echo read("efp.gp");eval("f(x)=atanh(x)");a=2*sqrt(2)-3;b=-a;n=31;qfpchebyshev("QFP_ATH_C",f,a,b,n) | gp-2.7 -q
  private static final QFP QFP_ATH_C1 = new QFP (QFP_P, 0, 0x1.0p0, -0x1.f36f641e20d72p-114);  //>c1=1.00000000000000000000000000000
  private static final QFP QFP_ATH_C3 = new QFP (QFP_P, 0, 0x1.5555555555555p-2, 0x1.5555555555611p-56);  //>c3=0.333333333333333333333333333334
  private static final QFP QFP_ATH_C5 = new QFP (QFP_P, 0, 0x1.999999999999ap-3, -0x1.9999999a4175dp-57);  //>c5=0.199999999999999999999999998941
  private static final QFP QFP_ATH_C7 = new QFP (QFP_P, 0, 0x1.2492492492492p-3, 0x1.24924b582faeep-57);  //>c7=0.142857142857142857142858053426
  private static final QFP QFP_ATH_C9 = new QFP (QFP_P, 0, 0x1.c71c71c71c71cp-4, 0x1.c7140426f0f4ap-58);  //<c9=0.111111111111111111110664924582
  private static final QFP QFP_ATH_C11 = new QFP (QFP_P, 0, 0x1.745d1745d1746p-4, -0x1.5fe638aeacf12p-59);  //<c11=0.0909090909090909092295808117708
  private static final QFP QFP_ATH_C13 = new QFP (QFP_P, 0, 0x1.3b13b13b13b12p-4, -0x1.a3d69d8be81fp-58);  //>c13=0.0769230769230768939015654936276
  private static final QFP QFP_ATH_C15 = new QFP (QFP_P, 0, 0x1.1111111111249p-4, 0x1.d1d90451b5d3fp-59);  //<c15=0.0666666666666709987679838034753
  private static final QFP QFP_ATH_C17 = new QFP (QFP_P, 0, 0x1.e1e1e1e1d17b1p-5, -0x1.464b242b5513p-59);  //>c17=0.0588235294112985410236031906597
  private static final QFP QFP_ATH_C19 = new QFP (QFP_P, 0, 0x1.af286bcf2dc59p-5, -0x1.87a381950827p-61);  //<c19=0.0526315789842832289529081789289
  private static final QFP QFP_ATH_C21 = new QFP (QFP_P, 0, 0x1.8618605cae00ep-5, -0x1.338aeff0d76ebp-59);  //<c21=0.0476190454550647741781645777628
  private static final QFP QFP_ATH_C23 = new QFP (QFP_P, 0, 0x1.642cb7cf14af4p-5, 0x1.e3d44dca87f29p-59);  //>c23=0.0434783544557343670267754616356
  private static final QFP QFP_ATH_C25 = new QFP (QFP_P, 0, 0x1.47a7e890f5ccdp-5, 0x1.31e1771122396p-59);  //<c25=0.0399970571813184995379450688298
  private static final QFP QFP_ATH_C27 = new QFP (QFP_P, 0, 0x1.2ff12aaefe92p-5, -0x1.6898bb0e3245dp-60);  //>c27=0.0371023019469751679228447470154
  private static final QFP QFP_ATH_C29 = new QFP (QFP_P, 0, 0x1.129828d81e45cp-5, -0x1.e5836f6efc4bcp-62);  //<c29=0.0335198209513200835791642635031
  private static final QFP QFP_ATH_C31 = new QFP (QFP_P, 0, 0x1.4cd3eb19356dep-5, -0x1.9b438b675ceacp-60);  //>c31=0.0406283942954084167863663627263
  //  52738671016840858519654937824341/1298074214633706907132624082305024*x^31 + 174044861024189167268553365393105/5192296858534827628530496329220096*x^29 + 192646165843689783173132312173475/5192296858534827628530496329220096*x^27 + 51919148588299478172157465924043/1298074214633706907132624082305024*x^25 + 112876261627386639600856890638121/2596148429267413814265248164610048*x^23 + 123626110061379994152690835949845/2596148429267413814265248164610048*x^21 + 34159847777477682956032434436057/649037107316853453566312041152512*x^19 + 9544663342819264156052426631917/162259276829213363391578010288128*x^17 + 173076561951172167729046456327487/2596148429267413814265248164610048*x^15 + 6240741416508203917305250584545/81129638414606681695789005144064*x^13 + 118006746784882446282972010813559/1298074214633706907132624082305024*x^11 + 72115234146317050395967301846949/649037107316853453566312041152512*x^9 + 46359793379775246683308298435959/324518553658426726783156020576256*x^7 + 129807421463370690713262407542947/649037107316853453566312041152512*x^5 + 108172851219475575594385340192273/324518553658426726783156020576256*x^3 + 46768052394588893382517914646921052235912054765895/46768052394588893382517914646921056628989841375232*x
  //  113.0359bit
  public QFP atanh () {
    return this.atanh (this);
  }  //qfp.atanh()
  public QFP atanh (QFP x) {
    int xf = x.flg;
    if ((xf & QFP_ZIN) != 0) {  //±0,±Inf,NaN
      if ((xf & QFP_I) != 0) {  //±Inf
        QFP.fpsr |= QFP_OE;
        this.flg = QFP_N;  //atanh(±Inf)=NaN
      } else {  //±0,NaN
        this.flg = xf;  //atanh(±0)=±0,atanh(NaN)=NaN
      }
      return this;
    }
    //±0,±Inf,NaN以外
    x = new QFP ().abs (x);  //|x|
    {
      int t = x.cmp1 ();
      if (0 <= t) {  //1<=|x|
        if (t == 0) {  //|x|==1
          QFP.fpsr |= QFP_DZ;
          this.flg = xf | QFP_I;
        } else {  //1<|x|
          QFP.fpsr |= QFP_OE;
          this.flg = QFP_N;
        }
        return this;
      }
    }
    //|x|<1
    int n = 0;
    if (QFP_ATH_T[0].lt (x)) {  //3-2*sqrt(2)<|x|
      QFP t1 = new QFP ().negdec (x);  //t1=1-|x|
      n = -Math.getExponent (t1.dvl);  //-floor(log2(1-|x|))。|x|<1なのでt1.eppは0
      if (QFP_ATH_T[n].lt (x)) {  //1-2/(2^n*sqrt(2)+1)<|x|
        n++;
      }
      t1.shl (n);  //t1=2^n*(1-|x|)
      x.inc ();  //x=1+|x|
      QFP t2 = new QFP ().add (x, t1);  //t2=1+|x|+2^n*(1-|x|)
      x.sub (t1)  //x=1+|x|-2^n*(1-|x|)
        .div (t2);  //x=(1+|x|-2^n*(1-|x|))/(1+|x|+2^n*(1-|x|))
    }
    QFP x2 = new QFP ().squ (x);  //x^2
    this.mul (QFP_ATH_C31, x2)
      .add (QFP_ATH_C29).mul (x2)
        .add (QFP_ATH_C27).mul (x2)
          .add (QFP_ATH_C25).mul (x2)
            .add (QFP_ATH_C23).mul (x2)
              .add (QFP_ATH_C21).mul (x2)
                .add (QFP_ATH_C19).mul (x2)
                  .add (QFP_ATH_C17).mul (x2)
                    .add (QFP_ATH_C15).mul (x2)
                      .add (QFP_ATH_C13).mul (x2)
                        .add (QFP_ATH_C11).mul (x2)
                          .add (QFP_ATH_C9).mul (x2)
                            .add (QFP_ATH_C7).mul (x2)
                              .add (QFP_ATH_C5).mul (x2)
                                .add (QFP_ATH_C3).mul (x2)
                                  .add (QFP_ATH_C1).mul (x);
    if (n != 0) {
      this.add (new QFP (n).mul (QFP_LN_2_2));  //+n*log(2)/2
    }
    return this.neg ((xf & QFP_M) != 0);
  }  //qfp.atanh(QFP)

  //s = x.cmp (y)
  public int cmp (QFP y) {
    int xf = this.flg;
    int yf = y.flg;
    if (((xf | yf) & QFP_ZIN) != 0) {  //どちらかが±0,±Inf,NaN
      return EFPBox.EFP_CMP_TABLE[xf >>> 24] << (yf >>> 24 - 1) >> 30;
    }
    //両方±0,±Inf,NaN以外
    int s = (xf & QFP_M) != 0 ? -1 : 1;
    return (xf != yf ? s :
            this.epp != y.epp ? this.epp < y.epp ? -s : s :
            this.dvl != y.dvl ? this.dvl < y.dvl ? -s : s :
            this.cvl != y.cvl ? this.cvl < y.cvl ? -s : s :
            0);
  }  //qfp.cmp(QFP)

  //s = x.cmp1 ()
  //  s=x<=>1
  //  1との比較
  public int cmp1 () {
    return ((this.flg & QFP_MZIN) != 0 ?  //-x,±0,±Inf,NaN
            (this.flg & QFP_N) != 0 ? 0 :  //NaN
            (this.flg & (QFP_M | QFP_Z)) != 0 ? -1 :  //(-Inf,-x,-0,+0)<1
            1  //1<+Inf
            :  //+x
            this.epp != 0 ? this.epp < 0 ? -1 : 1 :
            this.dvl != 1.0 ? this.dvl < 1.0 ? -1 : 1 :
            this.cvl < 0.0 ? -1 : 0.0 < this.cvl ? 1 : 0);
  }  //qfp.cmp1()

  //x = x.dec ()
  //y = y.dec (x)
  //  y=x-1
  //  デクリメント
  public QFP dec () {
    return this.sub (QFP_ONE);
  }  //qfp.dec()
  public QFP dec (QFP x) {
    return this.sub (x, QFP_ONE);
  }  //qfp.dec(QFP)

  //qfp = qfp.div (y)
  //qfp = qfp.div (x, y)
  //  除算
  //  フラグ
  //    (NaN)/(NaN,+Inf,0<y,+0,-0,y<0,-Inf)=NaN
  //    (NaN,+Inf,0<x,+0,-0,x<0,-Inf)/(NaN)=NaN
  //    (+0,-0)/(+0,-0)=NaN,OE
  //    (+Inf,-Inf)/(+Inf,-Inf)=NaN,OE
  //    (+Inf,0<x)/(+0)=+Inf,DZ
  //    (x<0,-Inf)/(+0)=-Inf,DZ
  //    (+Inf,0<x)/(-0)=-Inf,DZ
  //    (x<0,-Inf)/(-0)=+Inf,DZ
  //    (+Inf)/(0<y)=+Inf
  //    (+Inf)/(y<0)=-Inf
  //    (-Inf)/(0<y)=-Inf
  //    (-Inf)/(y<0)=+Inf
  //    (+0)/(0<y)=+0
  //    (-0)/(0<y)=-0
  //    (+0)/(y<0)=-0
  //    (-0)/(y<0)=+0
  //    (0<x,+0)/(+Inf)=+0
  //    (-0,x<0)/(+Inf)=-0
  //    (0<x,+0)/(-Inf)=-0
  //    (-0,x<0)/(-Inf)=+0
  //    (0<x)/(0<y)=+z
  //    (0<x)/(y<0)=-z
  //    (x<0)/(0<y)=-z
  //    (x<0)/(y<0)=+z
  //    +------+------+------+------+------+------+------+
  //    |      | +Inf |  0<y |   +0 |   -0 |  y<0 | -Inf |
  //    +------+------+------+------+------+------+------+
  //    | +Inf |  NaN | +Inf | +Inf | -Inf | -Inf |  NaN |
  //    |  0<x |   +0 |   +z | +Inf | -Inf |   -z |   -0 |
  //    |   +0 |   +0 |   +0 |  NaN |  NaN |   -0 |   -0 |
  //    |   -0 |   -0 |   -0 |  NaN |  NaN |   +0 |   +0 |
  //    |  x<0 |   -0 |   -z | -Inf | +Inf |   +z |   +0 |
  //    | -Inf |  NaN | -Inf | -Inf | +Inf | +Inf |  NaN |
  //    +------+------+------+------+------+------+------+
  private static final int[] QFP_DIV_FLG = {
    //  perl -e "use strict;my($M,$Z,$I,$N,$OE,$DZ)=(0x08000000,0x04000000,0x02000000,0x01000000,0x00002000,0x00000400);for my$a(0..15){my$x=$a<<24;for my$b(0..15){my$y=$b<<24;($b&7)==0 and print'      ';printf('0x%08x,',0b11101010>>($x>>24&7)&1||0b11101010>>($y>>24&7)&1?$N:($x&$y)&($Z|$I)?$N|$OE:($x^$y)&$M|($x&$I&&$y&$Z?$I|$DZ:$x&$I?$I:$x&$Z||$y&$I?$Z:0));($b&7)==7 and print chr 10;}}"
    0x00000000,0x01000000,0x04000000,0x01000000,0x00000000,0x01000000,0x01000000,0x01000000,
    0x08000000,0x01000000,0x0c000000,0x01000000,0x08000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x02000000,0x01000000,0x01002000,0x01000000,0x02000400,0x01000000,0x01000000,0x01000000,
    0x0a000000,0x01000000,0x01002000,0x01000000,0x0a000400,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x04000000,0x01000000,0x04000000,0x01000000,0x01002000,0x01000000,0x01000000,0x01000000,
    0x0c000000,0x01000000,0x0c000000,0x01000000,0x01002000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x08000000,0x01000000,0x0c000000,0x01000000,0x08000000,0x01000000,0x01000000,0x01000000,
    0x00000000,0x01000000,0x04000000,0x01000000,0x00000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x0a000000,0x01000000,0x01002000,0x01000000,0x0a000400,0x01000000,0x01000000,0x01000000,
    0x02000000,0x01000000,0x01002000,0x01000000,0x02000400,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x0c000000,0x01000000,0x0c000000,0x01000000,0x01002000,0x01000000,0x01000000,0x01000000,
    0x04000000,0x01000000,0x04000000,0x01000000,0x01002000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
  };
  public QFP div (QFP y) {
    int zf = QFP_DIV_FLG[this.flg >>> 24 - 4 | y.flg >>> 24];
    if ((zf & QFP_ZIN) != 0) {  //±0,±Inf,NaN
      QFP.fpsr |= zf & QFP_EXC;
      this.flg = zf & QFP_MZIN;
      return this;
    }
    return this.mul (new QFP ().rcp (y));
  }  //qfp.div(QFP)
  public QFP div (QFP x, QFP y) {
    int zf = QFP_DIV_FLG[x.flg >>> 24 - 4 | y.flg >>> 24];
    if ((zf & QFP_ZIN) != 0) {  //±0,±Inf,NaN
      QFP.fpsr |= zf & QFP_EXC;
      this.flg = zf & QFP_MZIN;
      return this;
    }
    return this.mul (x, new QFP ().rcp (y));
  }  //qfp.div(QFP,QFP)

  //qfp = qfp.div2 ()
  //qfp = qfp.div2 (x)
  //  1/2倍
  public QFP div2 () {
    if ((this.flg & QFP_ZIN) != 0) {  //±0,±Inf,NaN
      return this;
    }
    //±0,±Inf,NaN以外
    //1/2倍する
    this.dvl *= 0.5;
    this.cvl *= 0.5;
    //指数部の下駄を設定する
    int e = Math.getExponent (this.dvl) - QFP_GETA_BASE & -QFP_GETA_SIZE;
    if (e != 0) {
      this.epp += e;
      this.dvl = Math.scalb (this.dvl, -e);
      this.cvl = Math.scalb (this.cvl, -e);
    }
    return this;
  }  //qfp.div2()
  public QFP div2 (QFP x) {
    this.flg = x.flg;
    if ((this.flg & QFP_ZIN) != 0) {  //±0,±Inf,NaN
      return this;
    }
    //±0,±Inf,NaN以外
    //1/2倍する
    this.epp = x.epp;
    this.dvl = x.dvl * 0.5;
    this.cvl = x.cvl * 0.5;
    //指数部の下駄を設定する
    int e = Math.getExponent (this.dvl) - QFP_GETA_BASE & -QFP_GETA_SIZE;
    if (e != 0) {
      this.epp += e;
      this.dvl = Math.scalb (this.dvl, -e);
      this.cvl = Math.scalb (this.cvl, -e);
    }
    return this;
  }  //qfp.div2(QFP)

  //qfp.dump ()
  //qfp.dump (value)
  //  ダンプ
  public void dump () {
    this.dump (null, false);
  }  //qfp.dump()
  public void dump (String name) {
    this.dump (name, false);
  }  //qfp.dump(String)
  public void dump (boolean value) {
    this.dump (null, value);
  }  //qfp.dump(boolean)
  public void dump (String name, boolean value) {
    if (name != null) {
      System.out.printf ("%s=", name);
    }
    if ((this.flg & QFP_ZIN) != 0) {  //±0,±Inf,NaN
      System.out.printf ("QFP{flg:0x%08x", this.flg);
      if (value) {
        System.out.printf (",val:%s", this.toString ());
      }
      System.out.println ("}");
    } else {  //±0,±Inf,NaN以外
      System.out.printf ("QFP{flg:0x%08x,epp:%d,dvl:%.16g=0x%016x,cvl:%.16g=0x%016x",
                         this.flg,
                         this.epp,
                         this.dvl, Double.doubleToLongBits (this.dvl),
                         this.cvl, Double.doubleToLongBits (this.cvl));
      if (value) {
        System.out.printf (",val:%s", this.toString ());
      }
      System.out.println ("}");
    }
  }  //qfp.dump()

  //i = qfp.getd ()
  //  double取得
  public double getd () {
    int xf = this.flg;
    if ((xf & QFP_ZIN) != 0) {  //±0,±Inf,NaN
      return ((xf & QFP_Z) != 0 ? (xf & QFP_M) == 0 ? 0.0 : -0.0 :  //±0
              (xf & QFP_I) != 0 ? (xf & QFP_M) == 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY :  //±Inf
              Double.NaN);  //NaN
    }
    //±0,±Inf,NaN以外
    double xd = this.dvl;
    if (this.epp != 0) {
      xd = Math.scalb (xd, this.epp);
      if (Double.isInfinite (xd)) {  //オーバーフローした
        QFP.fpsr |= QFP_OF;
      } else if (xd == 0.0) {  //アンダーフローした
        QFP.fpsr |= QFP_UF;
      }
    }
    return xd;
  }  //qfp.getd()

  //f = qfp.getf ()
  //  float取得
  public float getf () {
    int xf = this.flg;
    if ((xf & QFP_ZIN) != 0) {  //±0,±Inf,NaN
      return ((xf & QFP_Z) != 0 ? (xf & QFP_M) == 0 ? 0.0F : -0.0F :  //±0
              (xf & QFP_I) != 0 ? (xf & QFP_M) == 0 ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY :  //±Inf
              Float.NaN);  //NaN
    }
    //±0,±Inf,NaN以外
    return (float) this.dvl;
  }  //qfp.getf()

  //i = qfp.geti ()
  //  int取得
  public int geti () {
    int xf = this.flg;
    if ((xf & QFP_ZIN) != 0) {  //±0,±Inf,NaN
      if ((xf & QFP_Z) != 0) {  //±0
        return 0;
      }
      QFP.fpsr |= QFP_OE;
      return ((xf & QFP_I) != 0 ? (xf & QFP_M) == 0 ? 0x7fffffff : 0x80000000 :  //±Inf
              0);  //NaN
    }
    //±0,±Inf,NaN以外
    if (this.epp != 0) {
      if (this.epp < 0) {
        return 0;
      }
      QFP.fpsr |= QFP_OE;
      return (xf & QFP_M) == 0 ? 0x7fffffff : 0x80000000;
    }
    double xd = this.dvl;
    double xc = this.cvl;
    //上位と下位の符号を合わせる
    if (xd * xc < 0.0) {
      double u = Math.ulp (xd) * Math.signum (xc);
      xd += u;
      xc -= u;
    }
    if (xd <= (double) Integer.MIN_VALUE - 1.0 || (double) Integer.MAX_VALUE + 1.0 <= xd) {
      QFP.fpsr |= QFP_OE;
      return 0.0 <= xd ? 0x7fffffff : 0x80000000;
    }
    return (int) xd;
  }  //qfp.geti()

  //l = qfp.getl ()
  //  long取得
  public long getl () {
    int xf = this.flg;
    if ((xf & QFP_ZIN) != 0) {  //±0,±Inf,NaN
      if ((xf & QFP_Z) != 0) {  //±0
        return 0L;
      }
      QFP.fpsr |= QFP_OE;
      return ((xf & QFP_I) != 0 ? (xf & QFP_M) == 0 ? 0x7fffffffffffffffL : 0x8000000000000000L :  //±Inf
              0L);  //NaN
    }
    //±0,±Inf,NaN以外
    if (this.epp != 0) {
      if (this.epp < 0) {
        return 0;
      }
      QFP.fpsr |= QFP_OE;
      return (xf & QFP_M) == 0 ? 0x7fffffffffffffffL : 0x8000000000000000L;
    }
    double xd = this.dvl;
    double xc = this.cvl;
    //上位と下位の符号を合わせる
    if (xd * xc < 0.0) {
      double u = Math.ulp (xd) * Math.signum (xc);
      xd += u;
      xc -= u;
    }
    if (xd <= (double) Long.MIN_VALUE - 1.0 || (double) Long.MAX_VALUE + 1.0 <= xd) {
      QFP.fpsr |= QFP_OE;
      return 0.0 <= xd ? 0x7fffffffffffffffL : 0x8000000000000000L;
    }
    return (long) xd + (long) xc;
  }  //qfp.getl()

  //x = x.inc ()
  //  インクリメント
  public QFP inc () {
    return this.add (QFP_ONE);
  }  //qfp.inc()

  //b = x.lt (y)
  public boolean lt (QFP y) {
    int xf = this.flg;
    int yf = y.flg;
    if (((xf | yf) & QFP_ZIN) != 0) {  //どちらかが±0,±Inf,NaN
      return EFPBox.EFP_LT_TABLE[xf >>> 24] << (yf >>> 24 - 1) < 0;
    }
    //両方±0,±Inf,NaN以外
    int s = (xf & QFP_M) != 0 ? -1 : 1;
    return (xf != yf ? s :
            this.epp != y.epp ? this.epp < y.epp ? -s : s :
            this.dvl != y.dvl ? this.dvl < y.dvl ? -s : s :
            this.cvl != y.cvl ? this.cvl < y.cvl ? -s : s :
            0) < 0;
  }  //qfp.lt(QFP)

  //qfp = qfp.mul (y)
  //qfp = qfp.mul (x, y)
  //  乗算
  //  フラグ
  //    (NaN)*(NaN,+Inf,0<y,+0,-0,y<0,-Inf)=NaN
  //    (NaN,+Inf,0<x,+0,-0,x<0,-Inf)*(NaN)=NaN
  //    (+0,-0)*(+Inf,-Inf)=NaN,OE
  //    (+Inf,-Inf)*(+0,-0)=NaN,OE
  //    (+Inf)*(+Inf,0<y)=+Inf
  //    (+Inf)*(y<0,-Inf)=-Inf
  //    (-Inf)*(+Inf,0<y)=-Inf
  //    (-Inf)*(y<0,-Inf)=+Inf
  //    (+Inf,0<x)*(+Inf)=+Inf
  //    (x<0,-Inf)*(+Inf)=-Inf
  //    (+Inf,0<x)*(-Inf)=-Inf
  //    (x<0,-Inf)*(-Inf)=+Inf
  //    (+0)*(0<y,+0)=+0
  //    (+0)*(-0,y<0)=-0
  //    (-0)*(0<y,+0)=-0
  //    (-0)*(-0,y<0)=+0
  //    (0<x,+0)*(+0)=+0
  //    (-0,x<0)*(+0)=-0
  //    (0<x,+0)*(-0)=-0
  //    (-0,x<0)*(-0)=+0
  //    (0<x)*(0<y)=+z
  //    (0<x)*(y<0)=-z
  //    (x<0)*(0<y)=-z
  //    (x<0)*(y<0)=+z
  //    +------+------+------+------+------+------+------+
  //    |      | +Inf |  0<y |   +0 |   -0 |  y<0 | -Inf |
  //    +------+------+------+------+------+------+------+
  //    | +Inf | +Inf | +Inf |  NaN |  NaN | -Inf | -Inf |
  //    |  0<x | +Inf |   +z |   +0 |   -0 |   -z | -Inf |
  //    |   +0 |  NaN |   +0 |   +0 |   -0 |   -0 |  NaN |
  //    |   -0 |  NaN |   -0 |   -0 |   +0 |   +0 |  NaN |
  //    |  x<0 | -Inf |   -z |   -0 |   +0 |   +z | +Inf |
  //    | -Inf | -Inf | -Inf |  NaN |  NaN | +Inf | +Inf |
  //    +------+------+------+------+------+------+------+
  private static final int[] QFP_MUL_FLG = {
    //  perl -e "use strict;my($M,$Z,$I,$N,$OE,$DZ)=(0x08000000,0x04000000,0x02000000,0x01000000,0x00002000,0x00000400);for my$a(0..15){my$x=$a<<24;for my$b(0..15){my$y=$b<<24;($b&7)==0 and print'      ';printf('0x%08x,',0b11101010>>($x>>24&7)&1||0b11101010>>($y>>24&7)&1?$N:(($x|$y)&($Z|$I))==($Z|$I)?$N|$OE:($x^$y)&$M|(($x|$y)&$I?$I:($x|$y)&$Z?$Z:0));($b&7)==7 and print chr 10;}}"
    0x00000000,0x01000000,0x02000000,0x01000000,0x04000000,0x01000000,0x01000000,0x01000000,
    0x08000000,0x01000000,0x0a000000,0x01000000,0x0c000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x02000000,0x01000000,0x02000000,0x01000000,0x01002000,0x01000000,0x01000000,0x01000000,
    0x0a000000,0x01000000,0x0a000000,0x01000000,0x01002000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x04000000,0x01000000,0x01002000,0x01000000,0x04000000,0x01000000,0x01000000,0x01000000,
    0x0c000000,0x01000000,0x01002000,0x01000000,0x0c000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x08000000,0x01000000,0x0a000000,0x01000000,0x0c000000,0x01000000,0x01000000,0x01000000,
    0x00000000,0x01000000,0x02000000,0x01000000,0x04000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x0a000000,0x01000000,0x0a000000,0x01000000,0x01002000,0x01000000,0x01000000,0x01000000,
    0x02000000,0x01000000,0x02000000,0x01000000,0x01002000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x0c000000,0x01000000,0x01002000,0x01000000,0x0c000000,0x01000000,0x01000000,0x01000000,
    0x04000000,0x01000000,0x01002000,0x01000000,0x04000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
  };
  public QFP mul (QFP y) {
    //フラグを設定する
    int zf = QFP_MUL_FLG[this.flg >>> 24 - 4 | y.flg >>> 24];
    if ((zf & QFP_ZIN) != 0) {  //±0,±Inf,NaN
      QFP.fpsr |= zf & QFP_EXC;
      this.flg = zf & QFP_MZIN;
      return this;
    }
    //乗算を行う
    int ze = this.epp + y.epp;
    double xd = this.dvl;
    double xc = this.cvl;
    double yd = y.dvl;
    double yc = y.cvl;
    {
      double t1 = xd * yd;
      double t2 = 0x1.0000002p27 * xd;
      double t3 = 0x1.0000002p27 * yd;
      double t4 = (xd - t2) + t2;
      double t5 = (yd - t3) + t3;
      t2 = xd - t4;
      t3 = yd - t5;
      t2 = ((xd + xc) * yc + xc * yd) + ((((t4 * t5 - t1) + t3 * t4) + t2 * t5) + t2 * t3);
      xd = t1 + t2;
      xc = (t1 - xd) + t2;
    }
    //指数部の下駄を設定する
    int e = Math.getExponent (xd) - QFP_GETA_BASE & -QFP_GETA_SIZE;
    if (e != 0) {
      ze += e;
      xd = Math.scalb (xd, -e);
      xc = Math.scalb (xc, -e);
    }
    //結果を格納する
    this.flg = zf;
    this.epp = ze;
    this.dvl = xd;
    this.cvl = xc;
    return this;
  }  //qfp.mul(QFP)
  public QFP mul (QFP x, QFP y) {
    //フラグを設定する
    int zf = QFP_MUL_FLG[x.flg >>> 24 - 4 | y.flg >>> 24];
    if ((zf & QFP_ZIN) != 0) {  //±0,±Inf,NaN
      QFP.fpsr |= zf & QFP_EXC;
      this.flg = zf & QFP_MZIN;
      return this;
    }
    //乗算を行う
    int ze = x.epp + y.epp;
    double xd = x.dvl;
    double xc = x.cvl;
    double yd = y.dvl;
    double yc = y.cvl;
    {
      double t1 = xd * yd;
      double t2 = 0x1.0000002p27 * xd;
      double t3 = 0x1.0000002p27 * yd;
      double t4 = (xd - t2) + t2;
      double t5 = (yd - t3) + t3;
      t2 = xd - t4;
      t3 = yd - t5;
      t2 = ((xd + xc) * yc + xc * yd) + ((((t4 * t5 - t1) + t3 * t4) + t2 * t5) + t2 * t3);
      xd = t1 + t2;
      xc = (t1 - xd) + t2;
    }
    //指数部の下駄を設定する
    int e = Math.getExponent (xd) - QFP_GETA_BASE & -QFP_GETA_SIZE;
    if (e != 0) {
      ze += e;
      xd = Math.scalb (xd, -e);
      xc = Math.scalb (xc, -e);
    }
    //結果を格納する
    this.flg = zf;
    this.epp = ze;
    this.dvl = xd;
    this.cvl = xc;
    return this;
  }  //qfp.mul(QFP,QFP)

  //qfp = qfp.mul2 ()
  //qfp = qfp.mul2 (x)
  //  2倍
  public QFP mul2 () {
    if ((this.flg & QFP_ZIN) != 0) {  //±0,±Inf,NaN
      return this;
    }
    //±0,±Inf,NaN以外
    //2倍する
    this.dvl *= 2.0;
    this.cvl *= 2.0;
    //指数部の下駄を設定する
    int e = Math.getExponent (this.dvl) - QFP_GETA_BASE & -QFP_GETA_SIZE;
    if (e != 0) {
      this.epp += e;
      this.dvl = Math.scalb (this.dvl, -e);
      this.cvl = Math.scalb (this.cvl, -e);
    }
    return this;
  }  //qfp.mul2()
  public QFP mul2 (QFP x) {
    this.flg = x.flg;
    if ((this.flg & QFP_ZIN) != 0) {  //±0,±Inf,NaN
      return this;
    }
    //±0,±Inf,NaN以外
    //2倍する
    this.epp = x.epp;
    this.dvl = x.dvl * 2.0;
    this.cvl = x.cvl * 2.0;
    //指数部の下駄を設定する
    int e = Math.getExponent (this.dvl) - QFP_GETA_BASE & -QFP_GETA_SIZE;
    if (e != 0) {
      this.epp += e;
      this.dvl = Math.scalb (this.dvl, -e);
      this.cvl = Math.scalb (this.cvl, -e);
    }
    return this;
  }  //qfp.mul2(QFP)

  //qfp = qfp.neg ()
  //qfp = qfp.neg (x)
  //qfp = qfp.neg (b)
  //qfp = qfp.neg (b, x)
  //  符号反転
  public QFP neg () {
    int xf = this.flg;
    if ((xf & QFP_ZIN) != 0) {  //±0,±Inf,NaN
      if ((xf & (QFP_Z | QFP_I)) != 0) {  //±0,±Inf
        this.flg = xf ^ QFP_M;
      }
      return this;
    }
    //±0,±Inf,NaN以外
    this.flg = xf ^ QFP_M;
    this.dvl = -this.dvl;
    this.cvl = -this.cvl;
    return this;
  }  //qfp.neg()
  public QFP neg (QFP x) {
    int xf = x.flg;
    if ((xf & QFP_ZIN) != 0) {  //±0,±Inf,NaN
      if ((xf & (QFP_Z | QFP_I)) != 0) {  //±0,±Inf
        xf ^= QFP_M;
      }
      this.flg = xf;
      return this;
    }
    //±0,±Inf,NaN以外
    this.flg = xf ^ QFP_M;
    this.dvl = -this.dvl;
    this.cvl = -this.cvl;
    return this;
  }  //qfp.neg(QFP)
  public QFP neg (boolean b) {
    return b ? this.neg () : this;
  }  //qfp.neg(boolean)
  public QFP neg (boolean b, QFP x) {
    return b ? this.neg (x) : this.setq (x);
  }  //qfp.neg(boolean,QFP)

  //x = x.negdec ()
  //  x=1-x
  //  逆デクリメント
  public QFP negdec () {
    return this.sub (QFP_ONE, this);
  }  //qfp.dec()
  public QFP negdec (QFP x) {
    return this.sub (QFP_ONE, x);
  }  //qfp.dec(QFP)

  //qfp = qfp.rcp ()
  //qfp = qfp.rcp (x)
  //  逆数
  //  z[n+1]=2*z[n]-x*z[n]^2
  public QFP rcp () {
    int xf = this.flg;
    if ((xf & QFP_ZIN) != 0) {  //±0,±Inf,NaN
      if ((xf & QFP_Z) != 0) {  //±0
        QFP.fpsr |= QFP_DZ;
        xf ^= QFP_Z | QFP_I;
      } else if ((xf & QFP_I) != 0) {  //±Inf
        xf ^= QFP_Z | QFP_I;
      }
      this.flg = xf;
      return this;
    }
    QFP t1 = new QFP (xf, -this.epp, 1.0 / this.dvl, 0.0);
    QFP t2 = new QFP ().squ (t1).mul (this);
    return this.mul2 (t1).sub (t2);
  }  //qfp.rcp()
  public QFP rcp (QFP x) {
    int xf = x.flg;
    if ((xf & QFP_ZIN) != 0) {  //±0,±Inf,NaN
      if ((xf & QFP_Z) != 0) {  //±0
        QFP.fpsr |= QFP_DZ;
        xf ^= QFP_Z | QFP_I;
      } else if ((xf & QFP_I) != 0) {  //±Inf
        xf ^= QFP_Z | QFP_I;
      }
      this.flg = xf;
      return this;
    }
    QFP t1 = new QFP (xf, -x.epp, 1.0 / x.dvl, 0.0);
    QFP t2 = new QFP ().squ (t1).mul (x);
    return this.mul2 (t1).sub (t2);
  }  //qfp.rcp(QFP)

  //qfp = qfp.set0 ()
  //  0代入
  public final QFP set0 () {
    this.flg = QFP_P | QFP_Z;
    return this;
  }  //qfp.set0()

  //qfp = qfp.setd (d)
  //  double代入
  public final QFP setd (double d) {
    int xf = (int) (Double.doubleToLongBits (d) >>> 36) & QFP_M;
    if (d == 0.0) {  //±0
      this.flg = xf | QFP_Z;
      return this;
    }
    if (Double.isInfinite (d)) {  //±Inf
      this.flg = xf | QFP_I;
      return this;
    }
    if (Double.isNaN (d)) {  //NaN
      this.flg = QFP_N;
      return this;
    }
    //±0,±Inf,NaN以外
    this.flg = xf;
    this.epp = Math.getExponent (d) - QFP_GETA_BASE & -QFP_GETA_SIZE;
    this.dvl = Math.scalb (d, -this.epp);
    this.cvl = 0.0;
    return this;
  }  //qfp.setd(double)

  //qfp = qfp.setf (f)
  //  float代入
  public final QFP setf (float f) {
    int xf = Float.floatToIntBits (f) >>> 4 & QFP_M;
    if (f == 0.0) {  //±0
      this.flg = xf | QFP_Z;
      return this;
    }
    if (Float.isInfinite (f)) {  //±Inf
      this.flg = xf | QFP_I;
      return this;
    }
    if (Float.isNaN (f)) {  //NaN
      this.flg = QFP_N;
      return this;
    }
    //±0,±Inf,NaN以外
    this.flg = xf;
    this.epp = 0;  //512<=QFP_GETA_SIZEのときfloatの指数部はepp==0に入り切る
    this.dvl = (double) f;
    this.cvl = 0.0;
    return this;
  }  //qfp.setf(float)

  //qfp = qfp.seti (i)
  //  int代入
  public final QFP seti (int i) {
    if (i == 0) {
      this.flg = QFP_P | QFP_Z;
      return this;
    }
    this.flg = i >>> 4 & QFP_M;
    this.epp = 0;
    this.dvl = (double) i;
    this.cvl = 0.0;
    return this;
  }  //qfp.seti(int)

  //qfp = qfp.setl (l)
  //  long代入
  public final QFP setl (long l) {
    if (l == 0L) {  //+0
      this.flg = QFP_P | QFP_Z;
      return this;
    }
    this.flg = (int) (l >>> 36) & QFP_M;
    this.epp = 0;
    //  Javaのlongからdoubleへのキャストはto nearest evenなので誤差の絶対値はulp/2以下
    //  http://docs.oracle.com/javase/specs/jls/se8/html/jls-5.html#jls-5.1.2
    //  doubleに変換しても整数であることに変わりないのでlongにキャストすればほとんど元に戻るが、
    //  longからdoubleへのキャストで2^63になったときだけdoubleからlongへのキャストが飽和変換で2^63-1になってしまう
    this.dvl = (double) l;  //2^63になる場合がある。longにキャストすると
    this.cvl = (double) (l - (this.dvl == 0x1p63 ? 0x8000000000000000L : (long) this.dvl));
    return this;
  }  //qfp.setl(long)

  //qfp = qfp.setq (x)
  //  QFP代入
  public final QFP setq (QFP x) {
    this.flg = x.flg;
    this.epp = x.epp;
    this.dvl = x.dvl;
    this.cvl = x.cvl;
    return this;
  }  //qfp.setq(QFP)

  //qfp = qfp.shl (n)
  //  左シフト
  public QFP shl (int n) {
    if ((this.flg & QFP_ZIN) != 0) {  //±0,±Inf,NaN
      return this;
    }
    //±0,±Inf,NaN以外
    //2^n倍する
    int e = Math.getExponent (this.dvl);  //元の指数部の下位
    n += this.epp + e;  //新しい指数部の全体
    this.epp = n - QFP_GETA_BASE & -QFP_GETA_SIZE;  //新しい指数部の上位
    n -= this.epp + e;  //新しい指数部の下位-元の指数部の下位
    this.dvl = Math.scalb (this.dvl, n);
    this.cvl = Math.scalb (this.cvl, n);
    return this;
  }  //qfp.shl(int)

  //qfp = qfp.shr (n)
  //  右シフト
  public QFP shr (int n) {
    if ((this.flg & QFP_ZIN) != 0) {  //±0,±Inf,NaN
      return this;
    }
    //±0,±Inf,NaN以外
    //2^-n倍する
    n = -n;
    int e = Math.getExponent (this.dvl);  //元の指数部の下位
    n += this.epp + e;  //新しい指数部の全体
    this.epp = n - QFP_GETA_BASE & -QFP_GETA_SIZE;  //新しい指数部の上位
    n -= this.epp + e;  //新しい指数部の下位-元の指数部の下位
    this.dvl = Math.scalb (this.dvl, n);
    this.cvl = Math.scalb (this.cvl, n);
    return this;
  }  //qfp.shr(int)

  //qfp = qfp.sqrt ()
  //qfp = qfp.sqrt (x)
  //  平方根
  //  z[n+1]=(z[n]+x/z[n])/2
  public QFP sqrt () {
    int xf = this.flg;
    if ((xf & QFP_MZIN) != 0) {  //-x,±0,±Inf,NaN
      if (xf == QFP_M || xf == (QFP_M | QFP_I)) {  //sqrt(-x)=NaN, sqrt(-Inf)=NaN
        QFP.fpsr |= QFP_OE;
        this.flg = QFP_N;
      } else {
        this.flg = xf;  //sqrt(±0)=±0, sqrt(+Inf)=+Inf, sqrt(NaN)=NaN
      }
      return this;
    }
    //-x,±0,±Inf,NaN以外
    QFP t = new QFP (Math.sqrt (this.dvl)).shl (this.epp >> 1);
    return this.div (this, t).add (t).div2 ();
  }  //qfp.sqrt()
  public QFP sqrt (QFP x) {
    int xf = x.flg;
    if ((xf & QFP_MZIN) != 0) {  //-x,±0,±Inf,NaN
      if (xf == QFP_M || xf == (QFP_M | QFP_I)) {  //sqrt(-x)=NaN, sqrt(-Inf)=NaN
        QFP.fpsr |= QFP_OE;
        this.flg = QFP_N;
      } else {
        this.flg = xf;  //sqrt(±0)=±0, sqrt(+Inf)=+Inf, sqrt(NaN)=NaN
      }
      return this;
    }
    //-x,±0,±Inf,NaN以外
    QFP t = new QFP (Math.sqrt (x.dvl)).shl (x.epp >> 1);
    return this.div (x, t).add (t).div2 ();
  }  //qfp.sqrt(QFP)

  //qfp = qfp.squ ()
  //qfp = qfp.squ (x)
  //  2乗
  public QFP squ () {
    return this.mul (this, this);
  }  //qfp.squ()
  public QFP squ (QFP x) {
    return this.mul (x, x);
  }  //qfp.squ(QFP)

  //qfp = qfp.sub (y)
  //qfp = qfp.sub (x, y)
  //  減算
  //  フラグ
  //    (NaN)-(NaN,+Inf,0<y,+0,-0,y<0,-Inf)=NaN
  //    (NaN,+Inf,0<x,+0,-0,x<0,-Inf)-(NaN)=NaN
  //    (+Inf)-(+Inf)=NaN,OE
  //    (-Inf)-(-Inf)=NaN,OE
  //    (+Inf)-(0<y,+0,-0,y<0,-Inf)=+Inf
  //    (-Inf)-(+Inf,0<y,+0,-0,y<0)=-Inf
  //    (0<x,+0,-0,x<0,-Inf)-(+Inf)=-Inf
  //    (+Inf,0<x,+0,-0,x<0)-(-Inf)=+Inf
  //    (+0)-(+0)=+0
  //    (+0)-(-0)=+0
  //    (-0)-(+0)=-0
  //    (-0)-(-0)=+0
  //    (+0,-0)-(0<y,y<0)=-y
  //    (0<x,x<0)-(+0,-0)=x
  //    (0<x,x<0)-(0<y,y<0)=z
  //    +------+------+------+------+------+------+------+
  //    |      | +Inf |  0<y |   +0 |   -0 |  y<0 | -Inf |
  //    +------+------+------+------+------+------+------+
  //    | +Inf |  NaN | +Inf | +Inf | +Inf | +Inf | +Inf |
  //    |  0<x | -Inf |    z |    x |    x |    z | +Inf |
  //    |   +0 | -Inf |   -y |   +0 |   +0 |   -y | +Inf |
  //    |   -0 | -Inf |   -y |   -0 |   +0 |   -y | +Inf |
  //    |  x<0 | -Inf |    z |    x |    x |    z | +Inf |
  //    | -Inf | -Inf | -Inf | -Inf | -Inf | -Inf |  NaN |
  //    +------+------+------+------+------+------+------+
  private static final int[] QFP_SUB_FLG = {
    //  perl -e "use strict;my($M,$Z,$I,$N,$OE,$DZ)=(0x08000000,0x04000000,0x02000000,0x01000000,0x00002000,0x00000400);for my$a(0..15){my$x=$a<<24;for my$b(0..15){my$y=$b<<24;($b&7)==0 and print'      ';printf('0x%08x,',0b11101010>>($x>>24&7)&1||0b11101010>>($y>>24&7)&1?$N:($x&$y)&$I&&$x==$y?$N|$OE:($x|$y)&$I?$x&$I?$x:$y^$M:($x&$y)&$Z?($x&~$y)&$M|$Z:$x&$Z?0xc0000000:$y&$Z?0x80000000:0);($b&7)==7 and print chr 10;}}"
    0x00000000,0x01000000,0x0a000000,0x01000000,0x80000000,0x01000000,0x01000000,0x01000000,
    0x00000000,0x01000000,0x02000000,0x01000000,0x80000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x02000000,0x01000000,0x01002000,0x01000000,0x02000000,0x01000000,0x01000000,0x01000000,
    0x02000000,0x01000000,0x02000000,0x01000000,0x02000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0xc0000000,0x01000000,0x0a000000,0x01000000,0x04000000,0x01000000,0x01000000,0x01000000,
    0xc0000000,0x01000000,0x02000000,0x01000000,0x04000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x00000000,0x01000000,0x0a000000,0x01000000,0x80000000,0x01000000,0x01000000,0x01000000,
    0x00000000,0x01000000,0x02000000,0x01000000,0x80000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x0a000000,0x01000000,0x0a000000,0x01000000,0x0a000000,0x01000000,0x01000000,0x01000000,
    0x0a000000,0x01000000,0x01002000,0x01000000,0x0a000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0xc0000000,0x01000000,0x0a000000,0x01000000,0x0c000000,0x01000000,0x01000000,0x01000000,
    0xc0000000,0x01000000,0x02000000,0x01000000,0x04000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
    0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,0x01000000,
  };
  public QFP sub (QFP y) {
    int xf = this.flg;
    int xe = this.epp;
    double xd = this.dvl;
    double xc = this.cvl;
    int yf = y.flg;
    int ye = y.epp;
    double yd = y.dvl;
    double yc = y.cvl;
    //フラグを設定する
    int zf = QFP_SUB_FLG[xf >>> 24 - 4 | yf >>> 24];
    if ((zf & (0xc0000000 | QFP_ZIN)) != 0) {  //x,y,±0,±Inf,NaN
      if (0 <= zf) {  //±0,±Inf,NaN
        QFP.fpsr |= zf & QFP_EXC;
        this.flg = zf & QFP_MZIN;
      } else if (zf << 1 < 0) {  //-y
        this.flg = yf ^ QFP_M;
        this.epp = ye;
        this.dvl = yd;
        this.cvl = yc;
        //} else {  //x
        //  this.flg = xf;
        //  this.epp = xe;
        //  this.dvl = xd;
        //  this.cvl = xc;
      }
      return this;
    }
    //両方±0,±Inf,NaN以外
    //指数部の下駄を合わせる
    if (xe != ye) {
      if (xe + 128 == ye) {
        yd = Math.scalb (yd, 128);
        yc = Math.scalb (yc, 128);
      } else if (xe - 128 == ye) {
        yd = Math.scalb (yd, -128);
        yc = Math.scalb (yc, -128);
      } else {
        if (xe < ye) {  //yが大きすぎるとき-y
          this.flg = yf ^ QFP_M;
          this.epp = ye;
          this.dvl = yd;
          this.cvl = yc;
          //} else {  //xが大きすぎるときx
          //  this.flg = xf;
          //  this.epp = xe;
          //  this.dvl = xd;
          //  this.cvl = xc;
        }
        return this;
      }
    }
    //加減算を行う
    {
      double t1 = xd - yd;
      double t2 = xd - t1;
      t2 = (((xd - (t1 + t2)) + (t2 - yd)) + xc) - yc;
      xd = t1 + t2;
      xc = (t1 - xd) + t2;
    }
    //フラグを設定する
    //  両方±0,±Inf,NaN以外の加減算で結果がNaNになることはない
    //  両方絶対値が2^192未満の加減算で結果が±Infになることはない
    if (xd == 0.0) {  //±0
      this.flg = QFP_P | QFP_Z;  //+0
      return this;
    }
    this.flg = (int) (Double.doubleToLongBits (xd) >>> 36) & QFP_M;
    //指数部の下駄を設定する
    int e = Math.getExponent (xd) - QFP_GETA_BASE & -QFP_GETA_SIZE;
    if (e != 0) {
      xe += e;
      xd = Math.scalb (xd, -e);
      xc = Math.scalb (xc, -e);
    }
    //結果を格納する
    this.epp = xe;
    this.dvl = xd;
    this.cvl = xc;
    return this;
  }  //qfp.sub(QFP)
  public QFP sub (QFP x, QFP y) {
    int xf = x.flg;
    int xe = x.epp;
    double xd = x.dvl;
    double xc = x.cvl;
    int yf = y.flg;
    int ye = y.epp;
    double yd = y.dvl;
    double yc = y.cvl;
    //フラグを設定する
    int zf = QFP_SUB_FLG[xf >>> 24 - 4 | yf >>> 24];
    if ((zf & (0xc0000000 | QFP_ZIN)) != 0) {  //x,y,±0,±Inf,NaN
      if (0 <= zf) {  //±0,±Inf,NaN
        QFP.fpsr |= zf & QFP_EXC;
        this.flg = zf & QFP_MZIN;
      } else if (zf << 1 < 0) {  //-y
        this.flg = yf ^ QFP_M;
        this.epp = ye;
        this.dvl = yd;
        this.cvl = yc;
      } else {  //x
        this.flg = xf;
        this.epp = xe;
        this.dvl = xd;
        this.cvl = xc;
      }
      return this;
    }
    //両方±0,±Inf,NaN以外
    //指数部の下駄を合わせる
    if (xe != ye) {
      if (xe + 128 == ye) {
        yd = Math.scalb (yd, 128);
        yc = Math.scalb (yc, 128);
      } else if (xe - 128 == ye) {
        yd = Math.scalb (yd, -128);
        yc = Math.scalb (yc, -128);
      } else {
        if (xe < ye) {  //yが大きすぎるとき-y
          this.flg = yf ^ QFP_M;
          this.epp = ye;
          this.dvl = yd;
          this.cvl = yc;
        } else {  //xが大きすぎるときx
          this.flg = xf;
          this.epp = xe;
          this.dvl = xd;
          this.cvl = xc;
        }
        return this;
      }
    }
    //加減算を行う
    {
      double t1 = xd - yd;
      double t2 = xd - t1;
      t2 = (((xd - (t1 + t2)) + (t2 - yd)) + xc) - yc;
      xd = t1 + t2;
      xc = (t1 - xd) + t2;
    }
    //フラグを設定する
    //  両方±0,±Inf,NaN以外の加減算で結果がNaNになることはない
    //  両方絶対値が2^192未満の加減算で結果が±Infになることはない
    if (xd == 0.0) {  //±0
      this.flg = QFP_P | QFP_Z;  //+0
      return this;
    }
    this.flg = (int) (Double.doubleToLongBits (xd) >>> 36) & QFP_M;
    //指数部の下駄を設定する
    int e = Math.getExponent (xd) - QFP_GETA_BASE & -QFP_GETA_SIZE;
    if (e != 0) {
      xe += e;
      xd = Math.scalb (xd, -e);
      xc = Math.scalb (xc, -e);
    }
    //結果を格納する
    this.epp = xe;
    this.dvl = xd;
    this.cvl = xc;
    return this;
  }  //qfp.sub(QFP,QFP)

  //s = qfp.toString ()
  //  10進数文字列化
  public static final QFP[] QFP_TEN_P16QR = {
    //  echo read("efp.gp");for(i=0,50,q=floor(i/16);r=i-16*q;s=Str("10^(16^",q,"*",r,")");qfpmem(eval(s),s)) | gp-2.7 -q
    new QFP (QFP_P, 0, 0x1.0p0, 0.0),  //10^(16^0*0)=1
    new QFP (QFP_P, 0, 0x1.4p3, 0.0),  //10^(16^0*1)=10
    new QFP (QFP_P, 0, 0x1.9p6, 0.0),  //10^(16^0*2)=100
    new QFP (QFP_P, 0, 0x1.f4p9, 0.0),  //10^(16^0*3)=1000
    new QFP (QFP_P, 0, 0x1.388p13, 0.0),  //10^(16^0*4)=10000
    new QFP (QFP_P, 0, 0x1.86ap16, 0.0),  //10^(16^0*5)=100000
    new QFP (QFP_P, 0, 0x1.e848p19, 0.0),  //10^(16^0*6)=1000000
    new QFP (QFP_P, 0, 0x1.312dp23, 0.0),  //10^(16^0*7)=10000000
    new QFP (QFP_P, 0, 0x1.7d784p26, 0.0),  //10^(16^0*8)=100000000
    new QFP (QFP_P, 0, 0x1.dcd65p29, 0.0),  //10^(16^0*9)=1000000000
    new QFP (QFP_P, 0, 0x1.2a05f2p33, 0.0),  //10^(16^0*10)=10000000000
    new QFP (QFP_P, 0, 0x1.74876e8p36, 0.0),  //10^(16^0*11)=100000000000
    new QFP (QFP_P, 0, 0x1.d1a94a2p39, 0.0),  //10^(16^0*12)=1000000000000
    new QFP (QFP_P, 0, 0x1.2309ce54p43, 0.0),  //10^(16^0*13)=10000000000000
    new QFP (QFP_P, 0, 0x1.6bcc41e9p46, 0.0),  //10^(16^0*14)=100000000000000
    new QFP (QFP_P, 0, 0x1.c6bf52634p49, 0.0),  //10^(16^0*15)=1000000000000000
    new QFP (QFP_P, 0, 0x1.0p0, 0.0),  //10^(16^1*0)=1
    new QFP (QFP_P, 0, 0x1.1c37937e08p53, 0.0),  //10^(16^1*1)=10000000000000000
    new QFP (QFP_P, 0, 0x1.3b8b5b5056e17p106, -0x1.3107fp52),  //10^(16^1*2)=1.00000000000000000000000000000 e32
    new QFP (QFP_P, 0, 0x1.5e531a0a1c873p159, -0x1.14b4c7a76a406p105),  //10^(16^1*3)=1.00000000000000000000000000000 e48
    new QFP (QFP_P, 0, 0x1.84f03e93ff9f5p212, -0x1.2ac340948e389p157),  //10^(16^1*4)=1.00000000000000000000000000000 e64
    new QFP (QFP_P, 512, 0x1.afcef51f0fb5fp-247, -0x1.08f322e84da1p-308),  //10^(16^1*5)=1.00000000000000000000000000000 e80
    new QFP (QFP_P, 512, 0x1.df67562d8b363p-194, -0x1.ae9d180b58861p-248),  //10^(16^1*6)=1.00000000000000000000000000000 e96
    new QFP (QFP_P, 512, 0x1.0a1f5b8132466p-140, 0x1.4f01f167b5e3p-194),  //10^(16^1*7)=1.00000000000000000000000000000 e112
    new QFP (QFP_P, 512, 0x1.27748f9301d32p-87, -0x1.901cc86649e4ap-141),  //10^(16^1*8)=1.00000000000000000000000000000 e128
    new QFP (QFP_P, 512, 0x1.4805738b51a75p-34, -0x1.18a0e9df9363ap-89),  //10^(16^1*9)=1.00000000000000000000000000000 e144
    new QFP (QFP_P, 512, 0x1.6c2d4256ffcc3p19, -0x1.56a2119e533adp-38),  //10^(16^1*10)=1.00000000000000000000000000000 e160
    new QFP (QFP_P, 512, 0x1.945145230b378p72, -0x1.b20a11c22bf0cp15),  //10^(16^1*11)=1.00000000000000000000000000000 e176
    new QFP (QFP_P, 512, 0x1.c0e1ef1a724ebp125, -0x1.4abd220ed605cp71),  //10^(16^1*12)=1.00000000000000000000000000000 e192
    new QFP (QFP_P, 512, 0x1.f25c186a6f04cp178, 0x1.45a7709a56ccep123),  //10^(16^1*13)=1.00000000000000000000000000000 e208
    new QFP (QFP_P, 512, 0x1.14a52dffc6799p232, 0x1.2f82bd6b70d9ap177),  //10^(16^1*14)=1.00000000000000000000000000000 e224
    new QFP (QFP_P, 1024, 0x1.33234de7ad7e3p-227, -0x1.34a66b24bc3ebp-283),  //10^(16^1*15)=1.00000000000000000000000000000 e240
    new QFP (QFP_P, 0, 0x1.0p0, 0.0),  //10^(16^2*0)=1
    new QFP (QFP_P, 1024, 0x1.54fdd7f73bf3cp-174, -0x1.7222446fe467p-229),  //10^(16^2*1)=1.00000000000000000000000000000 e256
    new QFP (QFP_P, 1536, 0x1.c633415d4c1d2p164, 0x1.c6cc655c54bc5p109),  //10^(16^2*2)=1.00000000000000000000000000000 e512
    new QFP (QFP_P, 2560, 0x1.2e7f39519a015p-9, -0x1.739372c035fd3p-64),  //10^(16^2*3)=1.00000000000000000000000000000 e768
    new QFP (QFP_P, 3584, 0x1.92eceb0d02ea2p-183, -0x1.f44d79616b874p-237),  //10^(16^2*4)=1.00000000000000000000000000000 e1024
    new QFP (QFP_P, 4096, 0x1.0c59181dd70aep156, -0x1.34f7a4332a3fap101),  //10^(16^2*5)=1.00000000000000000000000000000 e1280
    new QFP (QFP_P, 5120, 0x1.65706a7673275p-18, -0x1.bbda853c4b3e3p-77),  //10^(16^2*6)=1.00000000000000000000000000000 e1536
    new QFP (QFP_P, 6144, 0x1.dc1bbb0924957p-192, 0x1.185a80e1e6764p-248),  //10^(16^2*7)=1.00000000000000000000000000000 e1792
    new QFP (QFP_P, 6656, 0x1.3d1676bb8a7acp147, -0x1.0dac596b98e6bp93),  //10^(16^2*8)=1.00000000000000000000000000000 e2048
    new QFP (QFP_P, 7680, 0x1.a65c406483e9p-27, 0x1.bcfd432008104p-84),  //10^(16^2*9)=1.00000000000000000000000000000 e2304
    new QFP (QFP_P, 8704, 0x1.194aa9804143ep-200, 0x1.4cbbfda13244dp-256),  //10^(16^2*10)=1.00000000000000000000000000000 e2560
    new QFP (QFP_P, 9216, 0x1.76ae153537b2fp138, -0x1.9d9bfbc56226fp80),  //10^(16^2*11)=1.00000000000000000000000000000 e2816
    new QFP (QFP_P, 10240, 0x1.f312ba4bb116bp-36, 0x1.457ee84626d0ap-90),  //10^(16^2*12)=1.00000000000000000000000000000 e3072
    new QFP (QFP_P, 11264, 0x1.4c61defaad34p-209, -0x1.baaf1c99fdcacp-264),  //10^(16^2*13)=1.00000000000000000000000000000 e3328
    new QFP (QFP_P, 11776, 0x1.babb91457e4fep129, 0x1.fbcad508c760fp75),  //10^(16^2*14)=1.00000000000000000000000000000 e3584
    new QFP (QFP_P, 12800, 0x1.26dc0ee6fb8cap-44, -0x1.24e7716893ec1p-100),  //10^(16^2*15)=1.00000000000000000000000000000 e3840
    new QFP (QFP_P, 0, 0x1.0p0, 0.0),  //10^(16^3*0)=1
    new QFP (QFP_P, 13824, 0x1.88c0a40514413p-218, -0x1.94dacfab01fedp-275),  //10^(16^3*1)=1.00000000000000000000000000000 e4096
    new QFP (QFP_P, 27136, 0x1.2d4743a2ff5e4p77, 0x1.1a0c7c2892306p22),  //10^(16^3*2)=1.00000000000000000000000000000 e8192
  };
  public static final QFP[] QFP_TEN_M16QR = {
    //  echo read("efp.gp");for(i=0,50,q=floor(i/16);r=i-16*q;s=Str("10^(-16^",q,"*",r,")");qfpmem(eval(s),s)) | gp-2.7 -q
    new QFP (QFP_P, 0, 0x1.0p0, 0.0),  //10^(-16^0*0)=1
    new QFP (QFP_P, 0, 0x1.999999999999ap-4, -0x1.999999999999ap-58),  //10^(-16^0*1)=0.100000000000000000000000000000
    new QFP (QFP_P, 0, 0x1.47ae147ae147bp-7, -0x1.eb851eb851eb8p-63),  //10^(-16^0*2)=0.0100000000000000000000000000000
    new QFP (QFP_P, 0, 0x1.0624dd2f1a9fcp-10, -0x1.89374bc6a7efap-66),  //10^(-16^0*3)=0.00100000000000000000000000000000
    new QFP (QFP_P, 0, 0x1.a36e2eb1c432dp-14, -0x1.6a161e4f765fep-68),  //10^(-16^0*4)=0.000100000000000000000000000000000
    new QFP (QFP_P, 0, 0x1.4f8b588e368f1p-17, -0x1.ee78183f91e64p-71),  //10^(-16^0*5)=1.00000000000000000000000000000 e-5
    new QFP (QFP_P, 0, 0x1.0c6f7a0b5ed8dp-20, 0x1.b5a63f9a49c2cp-75),  //10^(-16^0*6)=1.00000000000000000000000000000 e-6
    new QFP (QFP_P, 0, 0x1.ad7f29abcaf48p-24, 0x1.5e1e99483b023p-78),  //10^(-16^0*7)=1.00000000000000000000000000000 e-7
    new QFP (QFP_P, 0, 0x1.5798ee2308c3ap-27, -0x1.03023df2d4c94p-82),  //10^(-16^0*8)=1.00000000000000000000000000000 e-8
    new QFP (QFP_P, 0, 0x1.12e0be826d695p-30, -0x1.34674bfabb83bp-84),  //10^(-16^0*9)=1.00000000000000000000000000000 e-9
    new QFP (QFP_P, 0, 0x1.b7cdfd9d7bdbbp-34, -0x1.20a5465df8d2cp-88),  //10^(-16^0*10)=1.00000000000000000000000000000 e-10
    new QFP (QFP_P, 0, 0x1.5fd7fe1796495p-37, 0x1.7f7bc7b4d28aap-91),  //10^(-16^0*11)=1.00000000000000000000000000000 e-11
    new QFP (QFP_P, 0, 0x1.19799812dea11p-40, 0x1.97f27f0f6e886p-96),  //10^(-16^0*12)=1.00000000000000000000000000000 e-12
    new QFP (QFP_P, 0, 0x1.c25c268497682p-44, -0x1.ecd79a5a0df95p-99),  //10^(-16^0*13)=1.00000000000000000000000000000 e-13
    new QFP (QFP_P, 0, 0x1.6849b86a12b9bp-47, 0x1.ea70909833de7p-107),  //10^(-16^0*14)=1.00000000000000000000000000000 e-14
    new QFP (QFP_P, 0, 0x1.203af9ee75616p-50, -0x1.937831647f5ap-104),  //10^(-16^0*15)=1.00000000000000000000000000000 e-15
    new QFP (QFP_P, 0, 0x1.0p0, 0.0),  //10^(-16^1*0)=1
    new QFP (QFP_P, 0, 0x1.cd2b297d889bcp-54, 0x1.5b4c2ebe68799p-109),  //10^(-16^1*1)=1.00000000000000000000000000000 e-16
    new QFP (QFP_P, 0, 0x1.9f623d5a8a733p-107, -0x1.a2cc10f3892d4p-161),  //10^(-16^1*2)=1.00000000000000000000000000000 e-32
    new QFP (QFP_P, 0, 0x1.7624f8a762fd8p-160, 0x1.595560c018581p-215),  //10^(-16^1*3)=1.00000000000000000000000000000 e-48
    new QFP (QFP_P, 0, 0x1.50ffd44f4a73dp-213, 0x1.a53f2398d747bp-268),  //10^(-16^1*4)=1.00000000000000000000000000000 e-64
    new QFP (QFP_P, -512, 0x1.2f8ac174d6123p246, 0x1.a5dccd879fc96p191),  //10^(-16^1*5)=1.00000000000000000000000000000 e-80
    new QFP (QFP_P, -512, 0x1.116805effaeaap193, 0x1.cd88ede5810c7p139),  //10^(-16^1*6)=1.00000000000000000000000000000 e-96
    new QFP (QFP_P, -512, 0x1.ec866b79e0cbap139, 0x1.bea6a30bdaffap85),  //10^(-16^1*7)=1.00000000000000000000000000000 e-112
    new QFP (QFP_P, -512, 0x1.bba08cf8c979dp86, -0x1.afa9c1a60497dp32),  //10^(-16^1*8)=1.00000000000000000000000000000 e-128
    new QFP (QFP_P, -512, 0x1.8f9574dcf8a7p33, 0x1.647f32529774bp-21),  //10^(-16^1*9)=1.00000000000000000000000000000 e-144
    new QFP (QFP_P, -512, 0x1.67e9c127b6e74p-20, 0x1.26b3da42cecadp-76),  //10^(-16^1*10)=1.00000000000000000000000000000 e-160
    new QFP (QFP_P, -512, 0x1.442e4fb67196p-73, 0x1.7dc56d0072d28p-131),  //10^(-16^1*11)=1.00000000000000000000000000000 e-176
    new QFP (QFP_P, -512, 0x1.23ff06eea847ap-126, -0x1.fcc24e7cae5cfp-180),  //10^(-16^1*12)=1.00000000000000000000000000000 e-192
    new QFP (QFP_P, -512, 0x1.0701bd527b498p-179, -0x1.cfdedc1a30d3p-233),  //10^(-16^1*13)=1.00000000000000000000000000000 e-208
    new QFP (QFP_P, -512, 0x1.d9ca79d89462ap-233, -0x1.425b0740a9caep-288),  //10^(-16^1*14)=1.00000000000000000000000000000 e-224
    new QFP (QFP_P, -1024, 0x1.aac0bf9b9e65cp226, 0x1.d6fb1e4a9a909p171),  //10^(-16^1*15)=1.00000000000000000000000000000 e-240
    new QFP (QFP_P, 0, 0x1.0p0, 0.0),  //10^(-16^2*0)=1
    new QFP (QFP_P, -1024, 0x1.8062864ac6f43p173, 0x1.39fa911155ffp118),  //10^(-16^2*1)=1.00000000000000000000000000000 e-256
    new QFP (QFP_P, -1536, 0x1.2093dc65b647ap-165, 0x1.0e3899699972p-219),  //10^(-16^2*2)=1.00000000000000000000000000000 e-512
    new QFP (QFP_P, -2560, 0x1.b14cda94a0bbdp8, 0x1.6b5ae1b259461p-47),  //10^(-16^2*3)=1.00000000000000000000000000000 e-768
    new QFP (QFP_P, -3584, 0x1.454d054bb4af8p182, 0x1.7b0f4c02b0d7ap126),  //10^(-16^2*4)=1.00000000000000000000000000000 e-1024
    new QFP (QFP_P, -4096, 0x1.e870ba12ebdb7p-157, 0x1.5f0fa5fb329e7p-211),  //10^(-16^2*5)=1.00000000000000000000000000000 e-1280
    new QFP (QFP_P, -5120, 0x1.6eb2893ea54e2p17, 0x1.b268e1eb7534p-38),  //10^(-16^2*6)=1.00000000000000000000000000000 e-1536
    new QFP (QFP_P, -6144, 0x1.134c7749892f7p191, -0x1.78c9be2976cf8p137),  //10^(-16^2*7)=1.00000000000000000000000000000 e-1792
    new QFP (QFP_P, -6656, 0x1.9d5ca69e686c6p-148, -0x1.0ddb6d7695869p-202),  //10^(-16^2*8)=1.00000000000000000000000000000 e-2048
    new QFP (QFP_P, -7680, 0x1.3655081e5142cp26, 0x1.c7f1c9d4e0198p-28),  //10^(-16^2*9)=1.00000000000000000000000000000 e-2304
    new QFP (QFP_P, -8704, 0x1.d1f6fb85bd815p199, -0x1.fdd33889c15f4p145),  //10^(-16^2*10)=1.00000000000000000000000000000 e-2560
    new QFP (QFP_P, -9216, 0x1.5dd2e72224515p-139, 0x1.e5718c3e1a28p-193),  //10^(-16^2*11)=1.00000000000000000000000000000 e-2816
    new QFP (QFP_P, -10240, 0x1.06a17e7922aebp35, 0x1.43f3cf11b5fc6p-19),  //10^(-16^2*12)=1.00000000000000000000000000000 e-3072
    new QFP (QFP_P, -11264, 0x1.8a57514d5d62cp208, -0x1.36b0b39a33d88p154),  //10^(-16^2*13)=1.00000000000000000000000000000 e-3328
    new QFP (QFP_P, -11776, 0x1.280d5f1f07facp-130, 0x1.32a5a6f1a7076p-184),  //10^(-16^2*14)=1.00000000000000000000000000000 e-3584
    new QFP (QFP_P, -12800, 0x1.bc85ff1a6f95bp43, 0x1.fc518fbd22355p-14),  //10^(-16^2*15)=1.00000000000000000000000000000 e-3840
    new QFP (QFP_P, 0, 0x1.0p0, 0.0),  //10^(-16^3*0)=1
    new QFP (QFP_P, -13824, 0x1.4dba0991a59d4p217, -0x1.0e90e3f6e2f1ep159),  //10^(-16^3*1)=1.00000000000000000000000000000 e-4096
    new QFP (QFP_P, -27136, 0x1.b30d8416d0db5p-78, 0x1.a574753f616c1p-135),  //10^(-16^3*2)=1.00000000000000000000000000000 e-8192
  };
  public String toString () {
    int xf = this.flg;
    if ((xf & QFP_ZIN) != 0) {  //±0,±Inf,NaN
      return (xf == (QFP_P | QFP_Z) ? "0" :
              xf == (QFP_M | QFP_Z) ? "-0" :
              xf == (QFP_P | QFP_I) ? "Infinity" :
              xf == (QFP_M | QFP_I) ? "-Infinity" :
              "NaN");
    }
    StringBuilder sb = new StringBuilder ();
    QFP x = new QFP (this);
    if ((xf & QFP_M) != 0) {  //-x
      sb.append ('-');
      x.neg ();
    }
    QFP t = new QFP ();
    //10進数で表現したときの指数部を求める
    //  10^e<=x<10^(e+1)となるeを求める
    int e = (int) Math.floor ((double) (x.epp + Math.getExponent (x.dvl)) * 0.30102999566398119521373889472);  //log10(2)
    //10^-eを掛けて1<=x<10にする
    if (0 < e) {
      x.mul (QFP_TEN_M16QR[e & 15]);
      if (16 <= e) {
        x.mul (QFP_TEN_M16QR[16 + (e >> 4 & 15)]);
        if (256 <= e) {
          x.mul (QFP_TEN_M16QR[32 + (e >> 8 & 15)]);
          if (4096 <= e) {
            x.mul (QFP_TEN_M16QR[48 + (e >> 12)]);
          }
        }
      }
    } else if (e < 0) {
      x.mul (QFP_TEN_P16QR[-e & 15]);
      if (e <= -16) {
        x.mul (QFP_TEN_P16QR[16 + (-e >> 4 & 15)]);
        if (e <= -256) {
          x.mul (QFP_TEN_P16QR[32 + (-e >> 8 & 15)]);
          if (e <= -4096) {
            x.mul (QFP_TEN_P16QR[48 + (-e >> 12)]);
          }
        }
      }
    }
    //整数部2桁、小数部32桁の10進数に変換する
    //  1<=x<10なのでw[1]が先頭になるはずだが誤差で前後にずれる可能性がある
    char[] w = new char[34];
    {
      int num = x.geti ();
      int bcd = XEiJ.FMT_BCD4[num];
      w[0] = (char) ('0' | bcd >> 4     );
      w[1] = (char) ('0' | bcd      & 15);
      for (int i = 2; i < 34; i += 4) {
        x.sub (t.seti (num)).mul (QFP_TENTO4);
        num = x.geti ();
        bcd = XEiJ.FMT_BCD4[num];
        w[i    ] = (char) ('0' | bcd >> 12     );
        w[i + 1] = (char) ('0' | bcd >>  8 & 15);
        w[i + 2] = (char) ('0' | bcd >>  4 & 15);
        w[i + 3] = (char) ('0' | bcd       & 15);
      }
    }
    //先頭の位置を確認する
    //  w[h]が先頭(0でない最初の数字)の位置
    int h = w[0] != '0' ? 0 : w[1] != '0' ? 1 : 2;
    //30+1桁目を四捨五入する
    int o = h + 30;  //w[o]は四捨五入する桁の位置。w[]の範囲内
    if ('5' <= w[o]) {
      int i = o;
      while ('9' < ++w[--i]) {
        w[i] = '0';
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
    while (w[o - 1] == '0') {  //全体は0ではないので必ず止まる。小数点よりも左側で止まる場合があることに注意
      o--;
    }
    //指数形式にするかどうか選択して文字列に変換する
    if (0 <= e && e < 30) {  //1<=x<10^30。指数形式にしない
      sb.append (w, h, e + 1);  //整数部。末尾の位置に関係なく1の位まで書く
      h += e + 1;
      if (h < o) {  //小数部がある
        sb.append ('.')  //小数部があるときだけ小数点を書く
          .append (w, h, o - h);  //小数部
      }
    } else if (-4 <= e && e < 0) {  //10^-4<=x<1。指数形式にしない
      sb.append ('0')  //整数部の0
        .append ('.');  //小数点
      while (++e < 0) {
        sb.append ('0');  //小数部の先頭の0の並び
      }
      sb.append (w, h, o - h);  //小数部。全体は0ではないので必ず小数部がある
    } else {  //x<10^-4または10^30<=x。指数形式にする
      sb.append (w[h++]);  //整数部
      if (h < o) {  //小数部がある
        sb.append ('.')  //小数部があるときだけ小数点を書く
          .append (w, h, o - h);  //小数部
      }
      sb.append ('e')  //指数部の始まり
        .append (e);  //指数部。正符号は省略する
    }
    return sb.toString ();
  }  //qfp.toString()

}  //class QFP



