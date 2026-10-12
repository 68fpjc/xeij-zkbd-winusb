//========================================================================================
//  FFT.java
//    en:Fast Fourier Transform
//    ja:高速フーリエ変換
//  Copyright (C) 2003-2019 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//  フーリエ変換
//    X[m];0<=m<Nがx[n];0<=n<Nのフーリエ変換のとき
//    X[m]=sum[0<=n<N]{ω[N]^(-m*n)*x[n]}
//
//  回転子
//    ω[N]=e^(2*pi*i/N)=cos(2*pi/N)+i*sin(2*pi/N)
//    ω[N]^n=ω[N/n]
//    ω[N]^(-n)=conj(ω[N]^n)=conj(ω[N/n])
//    ω[N]^N=ω[1]=1
//    ω[N]^(N/2)=ω[2]=-1
//    ω[N]^(N/4)=ω[4]=i
//
//  Sande-Tukeyの周波数間引き型FFT(基数K)
//    NはKの倍数とする
//    X[m];0<=m<Nをx[n];0<=n<Nのフーリエ変換とする
//    X[m]=sum[0<=n<N]{ω[N]^(-m*n)*x[n]}
//    周波数のインデックスmをKで割った商をj、余りをuとする
//    m=K*j+u    0<=j<N/K, 0<=u<K
//    時間のインデックスnをN/Kで割った商をv、余りをkとする
//    n=k+N/K*v  0<=k<N/K, 0<=v<K
//    X[m]=sum[0<=n<N]{ω[N]^(-m*n)*x[n]}
//    X[K*j+u]=sum[0<=k<N/K]{sum[0<=v<K]{ω[N]^(-(K*j+u)*(k+N/K*v))*x[k+N/K*v]}}
//            =sum[0<=k<N/K]{sum[0<=v<K]{ω[N]^(-K*j*k-N*j*v-u*k-N/K*u*v)*x[k+N/K*v]}}
//            =sum[0<=k<N/K]{ω[N]^(-K*j*k)*ω[N]^(-u*k)*sum[0<=v<K]{ω[N]^(-N/K*u*v)*x[k+N/K*v]}}
//            =sum[0<=k<N/K]{ω[N/K]^(-j*k)*ω[N]^(-u*k)*sum[0<=v<K]{ω[K]^(-u*v)*x[k+N/K*v]}}
//    ここで
//    X'[j,u]=X[K*j+u]                                         0<=j<N/K, 0<=u<K
//    x'[k,u]=ω[N]^(-u*k)*sum[0<=v<K]{ω[K]^(-u*v)*x[k+N/K*v]}  0<=k<N/K, 0<=u<K
//    とおくと、X'[j,u]はx'[k,u]のフーリエ変換に等しい
//----------------------------------------------------------------------------------------

package xeij;

import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System

public class FFT {

  private int fftN;
  private double[] fftSinCos;  //サインコサインテーブル。0..5*N/4
  private int[] fftSwap;  //スワップリスト。0..N-1の範囲の整数でできるビット反転のペアを小さい順に列挙した配列。ビット反転しても値が変わらない数を含まない

  //コンストラクタ
  //  Nは4以上の2の累乗に限る
  public FFT (int n) {
    fftN = n;
    int n2 = n >> 1;  //N/2
    int n4 = n >> 2;  //N/4
    //サインコサインテーブルを作る
    fftSinCos = new double[5 * n4 + 1];
    double d = 2.0 * Math.PI / (double) n;  //Nは2の累乗なので先に割っても誤差が増えることはない
    for (int i = 1; i < n4; i++) {
      //          |<-------------cos------------->|
      //  |<-------------sin------------->|
      //  +-----*****-----+-------+-------+-----***
      //  |  ***  |  ***  |       |       |  ***  |
      //  |**     |     **|       |       |**     |
      //  *-------+-------*-------+-------*-------+
      //  0      N/4     N/2*   3*N/4   **N     5*N/4
      //  |       |       |  ***  |  ***  |       |
      //  +-------+-------+-----*****-----+-------+
      //   ------> <------ ------> <------ ------>
      //     (1)     (2)     (3)     (4)     (5)
      //         (4)                 (3)                   (2)                (5)              (1)
      fftSinCos[n - i] = fftSinCos[n2 + i] = -(fftSinCos[n2 - i] = fftSinCos[n + i] = fftSinCos[i] = Math.sin (d * (double) i));
    }
    fftSinCos[n] = fftSinCos[n2] = fftSinCos[0] = 0.0;  //代入が2方向から来て重複するのは構わないが0と±1には誤差が入らないようにしたい
    fftSinCos[5 * n4] = fftSinCos[n4] = 1.0;  //[5*N/4]は現在使用していないが入れておく
    fftSinCos[3 * n4] = -1.0;
    //スワップリストを作る
    int b = Integer.numberOfTrailingZeros (n);
    fftSwap = new int[n - (1 << (b + 1 >> 1))];
    for (int i = 0, k = 0; i < n; i++) {
      int j = Integer.reverse (i << -b);
      if (i < j) {
        fftSwap[k] = i;
        fftSwap[k + 1] = j;
        k += 2;
      }
    }
  }  //new FFT(int)

  //fft.fftSandeTukey2 (x, y)
  //  Sande-Tukeyの周波数間引き型FFT(基数2)
  //    X[K*j+u]=sum[0<=k<N/K]{ω[N/K]^(-j*k)*ω[N]^(-u*k)*sum[0<=v<K]{ω[K]^(-u*v)*x[k+N/K*v]}}
  //    X[2*j]=sum[0<=k<N/2]{ω[N/2]^(-j*k)*{x[k]+x[k+N/2]}}
  //    X[2*j+1]=sum[0<=k<N/2]{ω[N/2]^(-j*k)*ω[N]^(-k)*(x[k]-x[k+N/2])}
  public void fftSandeTukey2 (double[] xr, double[] xi) {
    int n4 = fftN >> 2;  //N/4
    int half = fftN >> 1;  //N/2
    for (int step = 1; half >= 1; half >>= 1, step <<= 1) {
      for (int k = 0, theta = 0; k < half; k++, theta += step) {
        double or = fftSinCos[theta + n4];  //ω[N]^(-k)
        double oi = -fftSinCos[theta];
        for (int start = 0; start < fftN; start += half << 1) {
          int k0 = start + k;  //k
          int k1 = k0 + half;  //k+N/2
          double pr = xr[k0];  //x[k]
          double pi = xi[k0];
          double qr = xr[k1];  //x[k+N/2]
          double qi = xi[k1];
          xr[k0] = pr + qr;  //X[2*j]=x[k]+x[k+N/2]
          xi[k0] = pi + qi;
          pr -= qr;  //x[k]-x[k+N/2]
          pi -= qi;
          xr[k1] = or * pr - oi * pi;  //X[2*j+1]=ω[N]^(-k)*(x[k]-x[k+N/2])
          xi[k1] = or * pi + oi * pr;
        }
      }
    }
    //インデックスのビット反転
    for (int k = 0, l = fftSwap.length; k < l; k += 2) {
      int i = fftSwap[k];
      int j = fftSwap[k + 1];
      double t = xr[i];
      xr[i] = xr[j];
      xr[j] = t;
      t = xi[i];
      xi[i] = xi[j];
      xi[j] = t;
    }
  }  //fft.fftSandeTukey2(double[],double[])

  //fft.fftSandeTukey4 (x, y)
  //  Sande-Tukeyの周波数間引き型FFT(基数4)
  //    X[4*j+u]=sum[0<=k<N/4]{ω[N/4]^(-j*k)*ω[N]^(-u*k)*sum[0<=v<4]{i^(-u*v)*x[k+N/4*v]}}
  //    X[4*j]=sum[0<=k<N/4]{ω[N/4]^(-j*k)*((x[k]+x[k+N/2])+(x[k+N/4]+x[k+3*N/4]))}
  //    X[4*j+1]=sum[0<=k<N/4]{ω[N/4]^(-j*k)*ω[N]^(-k)*((x[k]-x[k+N/2])-i*(x[k+N/4]-x[k+3*N/4]))}
  //    X[4*j+2]=sum[0<=k<N/4]{ω[N/4]^(-j*k)*ω[N]^(-2*k)*((x[k]+x[k+N/2])-(x[k+N/4]+x[k+3*N/4]))}
  //    X[4*j+3]=sum[0<=k<N/4]{ω[N/4]^(-j*k)*ω[N]^(-3*k)*((x[k]-x[k+N/2])+i*(x[k+N/4]-x[k+3*N/4]))}
  public void fftSandeTukey4 (double[] xr, double[] xi) {
    int mask = fftN - 1;
    int n4 = fftN >> 2;  //N/4
    int quarter = n4;  //N/4
    for (int step = 1; quarter >= 2; quarter >>= 2, step <<= 2) {
      for (int k = 0, theta1 = 0; k < quarter; k++, theta1 += step) {
        int theta2 = theta1 + theta1 & mask;
        int theta3 = theta2 + theta1 & mask;
        double or1 = fftSinCos[theta1 + n4];  //ω[N]^(-k)
        double oi1 = -fftSinCos[theta1];
        double or2 = fftSinCos[theta2 + n4];  //ω[N]^(-2*k)
        double oi2 = -fftSinCos[theta2];
        double or3 = fftSinCos[theta3 + n4];  //ω[N]^(-3*k)
        double oi3 = -fftSinCos[theta3];
        for (int start = 0; start < fftN; start += quarter << 2) {
          int k0 = start + k;  //k
          int k1 = k0 + quarter;  //k+N/4
          int k2 = k1 + quarter;  //k+N/2
          int k3 = k2 + quarter;  //k+3*N/4
          double xr0 = xr[k0];  //x[k]
          double xi0 = xi[k0];
          double xr1 = xr[k1];  //x[k+N/4]
          double xi1 = xi[k1];
          double xr2 = xr[k2];  //x[k+N/2]
          double xi2 = xi[k2];
          double xr3 = xr[k3];  //x[k+3*N/4]
          double xi3 = xi[k3];
          if (false) {
            xr[k0] = (xr0 + xr2) + (xr1 + xr3);  //X[4*j]=(x[k]+x[k+N/2])+(x[k+N/4]+x[k+3*N/4])
            xi[k0] = (xi0 + xi2) + (xi1 + xi3);
            double tr = (xr0 + xr2) - (xr1 + xr3);  //(x[k]+x[k+N/2])-(x[k+N/4]+x[k+3*N/4])
            double ti = (xi0 + xi2) - (xi1 + xi3);
            xr[k1] = or2 * tr - oi2 * ti;  //X[4*j+2]=ω[N]^(-2*k)*((x[k]+x[k+N/2])-(x[k+N/4]+x[k+3*N/4]))
            xi[k1] = or2 * ti + oi2 * tr;
            tr = (xr0 - xr2) + (xi1 - xi3);  //(x[k]-x[k+N/2])-i*(x[k+N/4]-x[k+3*N/4])
            ti = (xi0 - xi2) - (xr1 - xr3);
            xr[k2] = or1 * tr - oi1 * ti;  //X[4*j+1]=ω[N]^(-k)*((x[k]-x[k+N/2])-i*(x[k+N/4]-x[k+3*N/4]))
            xi[k2] = or1 * ti + oi1 * tr;
            tr = (xr0 - xr2) - (xi1 - xi3);  //(x[k]-x[k+N/2])+i*(x[k+N/4]-x[k+3*N/4])
            ti = (xi0 - xi2) + (xr1 - xr3);
            xr[k3] = or3 * tr - oi3 * ti;  //X[4*j+3]=ω[N]^(-3*k)*((x[k]-x[k+N/2])+i*(x[k+N/4]-x[k+3*N/4]))
            xi[k3] = or3 * ti + oi3 * tr;
          } else {
            double pr, pi, qr, qi;
            xr[k0] = (pr = xr0 + xr2) + (qr = xr1 + xr3);  //X[4*j]=(x[k]+x[k+N/2])+(x[k+N/4]+x[k+3*N/4])
            xi[k0] = (pi = xi0 + xi2) + (qi = xi1 + xi3);
            double tr = pr - qr;  //(x[k]+x[k+N/2])-(x[k+N/4]+x[k+3*N/4])
            double ti = pi - qi;
            xr[k1] = or2 * tr - oi2 * ti;  //X[4*j+2]=ω[N]^(-2*k)*((x[k]+x[k+N/2])-(x[k+N/4]+x[k+3*N/4]))
            xi[k1] = or2 * ti + oi2 * tr;
            tr = (pr = xr0 - xr2) + (qr = xi1 - xi3);  //(x[k]-x[k+N/2])-i*(x[k+N/4]-x[k+3*N/4])
            ti = (pi = xi0 - xi2) - (qi = xr1 - xr3);
            xr[k2] = or1 * tr - oi1 * ti;  //X[4*j+1]=ω[N]^(-k)*((x[k]-x[k+N/2])-i*(x[k+N/4]-x[k+3*N/4]))
            xi[k2] = or1 * ti + oi1 * tr;
            tr = pr - qr;  //(x[k]-x[k+N/2])+i*(x[k+N/4]-x[k+3*N/4])
            ti = pi + qi;
            xr[k3] = or3 * tr - oi3 * ti;  //X[4*j+3]=ω[N]^(-3*k)*((x[k]-x[k+N/2])+i*(x[k+N/4]-x[k+3*N/4]))
            xi[k3] = or3 * ti + oi3 * tr;
          }
        }
      }
    }
    if (quarter == 1) {  //N=4
      for (int k0 = 0; k0 < fftN; k0 += 4) {  //0
        int k1 = k0 + 1;  //1
        int k2 = k0 + 2;  //2
        int k3 = k0 + 3;  //3
        double xr0 = xr[k0];  //x[0]
        double xi0 = xi[k0];
        double xr1 = xr[k1];  //x[1]
        double xi1 = xi[k1];
        double xr2 = xr[k2];  //x[2]
        double xi2 = xi[k2];
        double xr3 = xr[k3];  //x[3]
        double xi3 = xi[k3];
        if (false) {
          xr[k0] = (xr0 + xr2) + (xr1 + xr3);  //X[0]=(x[0]+x[2])+(x[1]+x[3])
          xi[k0] = (xi0 + xi2) + (xi1 + xi3);
          xr[k1] = (xr0 + xr2) - (xr1 + xr3);  //X[2]=(x[0]+x[2])-(x[1]+x[3])
          xi[k1] = (xi0 + xi2) - (xi1 + xi3);
          xr[k2] = (xr0 - xr2) + (xi1 - xi3);  //X[1]=(x[0]-x[2])-i*(x[1]-x[3])
          xi[k2] = (xi0 - xi2) - (xr1 - xr3);
          xr[k3] = (xr0 - xr2) - (xi1 - xi3);  //X[3]=(x[0]-x[2])+i*(x[1]-x[3])
          xi[k3] = (xi0 - xi2) + (xr1 - xr3);
        } else {
          double t0p2r = xr0 + xr2;
          double t0p2i = xi0 + xi2;
          double t0m2r = xr0 - xr2;
          double t0m2i = xi0 - xi2;
          double t1p3r = xr1 + xr3;
          double t1p3i = xi1 + xi3;
          double t1m3r = xr1 - xr3;
          double t1m3i = xi1 - xi3;
          xr[k0] = t0p2r + t1p3r;  //X[0]=(x[0]+x[2])+(x[1]+x[3])
          xi[k0] = t0p2i + t1p3i;
          xr[k1] = t0p2r - t1p3r;  //X[2]=(x[0]+x[2])-(x[1]+x[3])
          xi[k1] = t0p2i - t1p3i;
          xr[k2] = t0m2r + t1m3i;  //X[1]=(x[0]-x[2])-i*(x[1]-x[3])
          xi[k2] = t0m2i - t1m3r;
          xr[k3] = t0m2r - t1m3i;  //X[3]=(x[0]-x[2])+i*(x[1]-x[3])
          xi[k3] = t0m2i + t1m3r;
        }
      }
    } else {  //N=2
      for (int k0 = 0; k0 < fftN; k0 += 2) {  //0
        int k1 = k0 + 1;  //1
        double xr0 = xr[k0];  //x[0]
        double xi0 = xi[k0];
        double xr1 = xr[k1];  //x[1]
        double xi1 = xi[k1];
        xr[k0] = xr0 + xr1;  //X[0]=x[0]+x[1]
        xi[k0] = xi0 + xi1;
        xr[k1] = xr0 - xr1;  //X[1]=x[0]-x[1]
        xi[k1] = xi0 - xi1;
      }
    }
    //インデックスのビット反転
    for (int k = 0, l = fftSwap.length; k < l; k += 2) {
      int i = fftSwap[k];
      int j = fftSwap[k + 1];
      double t = xr[i];
      xr[i] = xr[j];
      xr[j] = t;
      t = xi[i];
      xi[i] = xi[j];
      xi[j] = t;
    }
  }  //fft.fftSandeTukey4(double[],double[])

}  //class FFT



