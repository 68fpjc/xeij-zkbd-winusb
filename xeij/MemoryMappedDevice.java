//========================================================================================
//  MemoryMappedDevice.java
//    en:Memory mapped device
//    ja:メモリマップトデバイス
//  Copyright (C) 2003-2026 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//  デバイスにアクセスするためのメソッドをenum bodyに記述する
//    mmdPbs,mmdPbz,mmdPws,mmdPwz,mmdPls  ピーク
//    mmdRbs,mmdRbz,mmdRws,mmdRwz,mmdRls  リード
//    mmdWb,mmdWw,mmdWl                   ライト
//  ピーク、リード、ライトの命名規則
//    4文字目  P=ピーク,R=リード,W=ライト
//    5文字目  b=バイト,w=ワード,l=ロング
//    6文字目  s=符号拡張,z=ゼロ拡張
//  ピークとリードの返却値の型はmmdPbsとmmdRbsだけbyte、他はint
//  ピークはSRAMスイッチの読み取りやデバッガなどで使用する
//  ピークはMPUやデバイスの状態を変化させず、例外もスローしない
//  リードとライトはMPUやDMAによる通常のアクセスで使用する
//  リードとライトはバスエラーをスローする場合がある
//  アドレスの未使用ビットはデバイスに渡る前にすべてクリアされていなければならない
//    バスエラーは未使用ビットがクリアされたアドレスで通知されることになる
//  異なるデバイスに跨るアクセスはデバイスに渡る前に分割されていなければならない
//  奇数アドレスに対するワードアクセスはデバイスに渡る前に分割または排除されていなければならない
//  4の倍数でないアドレスに対するロングアクセスはデバイスに渡る前に分割または排除されていなければならない
//  デバイスのメソッドを直接呼び出すときはアドレスのマスクや分割を忘れないこと
//----------------------------------------------------------------------------------------

package xeij;

import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,Timer,TimerTask,TreeMap

public enum MemoryMappedDevice {

  //--------------------------------------------------------------------------------
  //MMD_MMR メインメモリ
  MMD_MMR {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "メインメモリ" : "Main Memory";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a];
    }
    @Override protected int mmdPbz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a] & 0xff;
    }
    @Override protected int mmdPws (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (MainMemory.MMR_USE_BYTE_BUFFER) {
        return MainMemory.mmrBuffer.getShort (a);
      } else {
        return MainMemory.mmrM8[a] << 8 | (MainMemory.mmrM8[a + 1] & 0xff);
      }
    }
    @Override protected int mmdPwz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (MainMemory.MMR_USE_BYTE_BUFFER) {
        return MainMemory.mmrBuffer.getChar (a);
      } else {
        return (char) (MainMemory.mmrM8[a] << 8 | (MainMemory.mmrM8[a + 1] & 0xff));
      }
    }
    @Override protected int mmdPls (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (MainMemory.MMR_USE_BYTE_BUFFER) {
        return MainMemory.mmrBuffer.getInt (a);
      } else {
        return MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 0xff) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | (MainMemory.mmrM8[a + 3] & 0xff));
      }
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.ram;
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a];
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.ram;
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a] & 0xff;
     }
    @Override protected int mmdRws (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.ram;
       a &= XEiJ.BUS_MOTHER_MASK;
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         return MainMemory.mmrBuffer.getShort (a);
       } else {
         return MainMemory.mmrM8[a] << 8 | (MainMemory.mmrM8[a + 1] & 0xff);
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.ram;
       a &= XEiJ.BUS_MOTHER_MASK;
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         return MainMemory.mmrBuffer.getChar (a);
       } else {
         return (char) (MainMemory.mmrM8[a] << 8 | (MainMemory.mmrM8[a + 1] & 0xff));
       }
     }
    @Override protected int mmdRls (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.ramlong;
       a &= XEiJ.BUS_MOTHER_MASK;
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         return MainMemory.mmrBuffer.getInt (a);
       } else {
         return MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 0xff) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | (MainMemory.mmrM8[a + 3] & 0xff));
       }
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.ram;
       a &= XEiJ.BUS_MOTHER_MASK;
       MainMemory.mmrM8[a    ] = (byte)  d;
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.ram;
       a &= XEiJ.BUS_MOTHER_MASK;
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         MainMemory.mmrBuffer.putShort (a, (short) d);
       } else {
         MainMemory.mmrM8[a    ] = (byte) (d >> 8);
         MainMemory.mmrM8[a + 1] = (byte)  d;
       }
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.ramlong;
       a &= XEiJ.BUS_MOTHER_MASK;
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         MainMemory.mmrBuffer.putInt (a, d);
       } else {
         MainMemory.mmrM8[a    ] = (byte) (d >> 24);
         MainMemory.mmrM8[a + 1] = (byte) (d >> 16);
         MainMemory.mmrM8[a + 2] = (byte) (d >> 8);
         MainMemory.mmrM8[a + 3] = (byte)  d;
       }
     }
  },  //MMD_MMR

  MMD_MM6 {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "メインメモリ (68060)" : "Main Memory (68060)";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a];
    }
    @Override protected int mmdPbz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a] & 0xff;
    }
    @Override protected int mmdPws (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (MainMemory.MMR_USE_BYTE_BUFFER) {
        return MainMemory.mmrBuffer.getShort (a);
      } else {
        return MainMemory.mmrM8[a] << 8 | (MainMemory.mmrM8[a + 1] & 0xff);
      }
    }
    @Override protected int mmdPwz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (MainMemory.MMR_USE_BYTE_BUFFER) {
        return MainMemory.mmrBuffer.getChar (a);
      } else {
        return (char) (MainMemory.mmrM8[a] << 8 | (MainMemory.mmrM8[a + 1] & 0xff));
      }
    }
    @Override protected int mmdPls (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (MainMemory.MMR_USE_BYTE_BUFFER) {
        return MainMemory.mmrBuffer.getInt (a);
      } else {
        return MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 0xff) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | (MainMemory.mmrM8[a + 3] & 0xff));
      }
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       if (MC68060.CAT_ON &&
           !XEiJ.busWaitTime.isDma) {
         MC68060.catReadMainROM (a);
       } else {
         XEiJ.mpuClockTime += XEiJ.busWaitTime.ram;
       }
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a];
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       if (MC68060.CAT_ON &&
           !XEiJ.busWaitTime.isDma) {
         MC68060.catReadMainROM (a);
       } else {
         XEiJ.mpuClockTime += XEiJ.busWaitTime.ram;
       }
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a] & 0xff;
     }
    @Override protected int mmdRws (int a) throws M68kException {
       if (MC68060.CAT_ON &&
           !XEiJ.busWaitTime.isDma) {
         MC68060.catReadMainROM (a);
       } else {
         XEiJ.mpuClockTime += XEiJ.busWaitTime.ram;
       }
       a &= XEiJ.BUS_MOTHER_MASK;
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         return MainMemory.mmrBuffer.getShort (a);
       } else {
         return MainMemory.mmrM8[a] << 8 | (MainMemory.mmrM8[a + 1] & 0xff);
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       if (MC68060.CAT_ON &&
           !XEiJ.busWaitTime.isDma) {
         MC68060.catReadMainROM (a);
       } else {
         XEiJ.mpuClockTime += XEiJ.busWaitTime.ram;
       }
       a &= XEiJ.BUS_MOTHER_MASK;
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         return MainMemory.mmrBuffer.getChar (a);
       } else {
         return (char) (MainMemory.mmrM8[a] << 8 | (MainMemory.mmrM8[a + 1] & 0xff));
       }
     }
    @Override protected int mmdRls (int a) throws M68kException {
       if (MC68060.CAT_ON &&
           !XEiJ.busWaitTime.isDma) {
         MC68060.catReadMainROM (a);
       } else {
         XEiJ.mpuClockTime += XEiJ.busWaitTime.ramlong;
       }
       a &= XEiJ.BUS_MOTHER_MASK;
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         return MainMemory.mmrBuffer.getInt (a);
       } else {
         return MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 0xff) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | (MainMemory.mmrM8[a + 3] & 0xff));
       }
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if (MC68060.CAT_ON &&
           !XEiJ.busWaitTime.isDma) {
         MC68060.catWriteMain (a);
       } else {
         XEiJ.mpuClockTime += XEiJ.busWaitTime.ram;
       }
       a &= XEiJ.BUS_MOTHER_MASK;
       MainMemory.mmrM8[a    ] = (byte)  d;
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       if (MC68060.CAT_ON &&
           !XEiJ.busWaitTime.isDma) {
         MC68060.catWriteMain (a);
       } else {
         XEiJ.mpuClockTime += XEiJ.busWaitTime.ram;
       }
       a &= XEiJ.BUS_MOTHER_MASK;
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         MainMemory.mmrBuffer.putShort (a, (short) d);
       } else {
         MainMemory.mmrM8[a    ] = (byte) (d >> 8);
         MainMemory.mmrM8[a + 1] = (byte)  d;
       }
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       if (MC68060.CAT_ON &&
           !XEiJ.busWaitTime.isDma) {
         MC68060.catWriteMain (a);
       } else {
         XEiJ.mpuClockTime += XEiJ.busWaitTime.ramlong;
       }
       a &= XEiJ.BUS_MOTHER_MASK;
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         MainMemory.mmrBuffer.putInt (a, d);
       } else {
         MainMemory.mmrM8[a    ] = (byte) (d >> 24);
         MainMemory.mmrM8[a + 1] = (byte) (d >> 16);
         MainMemory.mmrM8[a + 2] = (byte) (d >> 8);
         MainMemory.mmrM8[a + 3] = (byte)  d;
       }
     }
  },  //MMD_MM6

  //--------------------------------------------------------------------------------
  //MMD_MM1 1MB搭載機の2MB目
  //  $00100000～$001FFFFF
  //  リードはShodaiは$6100、ACE/PRO/PROIIは$FFFFの繰り返し
  //  ライトは無視
  MMD_MM1 {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "1MB 搭載機の 2MB 目" : "2nd MB of machines with 1MB";
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.ram;
      return (byte) ((a & 1) == 0 ? mmdRwz (a) >> 8 : mmdRwz (a));
    }
    @Override protected int mmdRbz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.ram;
      return (a & 1) == 0 ? mmdRwz (a) >> 8 : mmdRwz (a) & 0xff;
    }
    @Override protected int mmdRws (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.ram;
      return (short) mmdRwz (a);
    }
    @Override protected int mmdRwz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.ram;
      return XEiJ.currentModel.isShodai () ? 0x6100 : 0xffff;
    }
    @Override protected int mmdRls (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.ramlong;
      return mmdRwz (a) << 16 | mmdRwz (a + 2);
    }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.ram;
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.ram;
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.ramlong;
     }
  },

  //--------------------------------------------------------------------------------
  //MMD_XMM 拡張メモリ
  MMD_XMM {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "拡張メモリ" : "Expansion Memory";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      a -= XEiJ.busExMemoryStart;
      return XEiJ.busExMemoryArray[a];
    }
    @Override protected int mmdPbz (int a) {
      a -= XEiJ.busExMemoryStart;
      return XEiJ.busExMemoryArray[a] & 0xff;
    }
    @Override protected int mmdPws (int a) {
      a -= XEiJ.busExMemoryStart;
      return XEiJ.busExMemoryArray[a] << 8 | (XEiJ.busExMemoryArray[a + 1] & 0xff);
    }
    @Override protected int mmdPwz (int a) {
      a -= XEiJ.busExMemoryStart;
      return (char) (XEiJ.busExMemoryArray[a] << 8 | (XEiJ.busExMemoryArray[a + 1] & 0xff));
    }
    @Override protected int mmdPls (int a) {
      a -= XEiJ.busExMemoryStart;
      return XEiJ.busExMemoryArray[a] << 24 | (XEiJ.busExMemoryArray[a + 1] & 0xff) << 16 | (char) (XEiJ.busExMemoryArray[a + 2] << 8 | (XEiJ.busExMemoryArray[a + 3] & 0xff));
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       a -= XEiJ.busExMemoryStart;
       return XEiJ.busExMemoryArray[a];
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       a -= XEiJ.busExMemoryStart;
       return XEiJ.busExMemoryArray[a] & 0xff;
     }
    @Override protected int mmdRws (int a) throws M68kException {
       a -= XEiJ.busExMemoryStart;
       return XEiJ.busExMemoryArray[a] << 8 | (XEiJ.busExMemoryArray[a + 1] & 0xff);
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       a -= XEiJ.busExMemoryStart;
       return (char) (XEiJ.busExMemoryArray[a] << 8 | (XEiJ.busExMemoryArray[a + 1] & 0xff));
     }
    @Override protected int mmdRls (int a) throws M68kException {
       a -= XEiJ.busExMemoryStart;
       return XEiJ.busExMemoryArray[a] << 24 | (XEiJ.busExMemoryArray[a + 1] & 0xff) << 16 | (char) (XEiJ.busExMemoryArray[a + 2] << 8 | (XEiJ.busExMemoryArray[a + 3] & 0xff));
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       a -= XEiJ.busExMemoryStart;
       XEiJ.busExMemoryArray[a    ] = (byte)  d;
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       a -= XEiJ.busExMemoryStart;
       XEiJ.busExMemoryArray[a    ] = (byte) (d >> 8);
       XEiJ.busExMemoryArray[a + 1] = (byte)  d;
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       a -= XEiJ.busExMemoryStart;
       XEiJ.busExMemoryArray[a    ] = (byte) (d >> 24);
       XEiJ.busExMemoryArray[a + 1] = (byte) (d >> 16);
       XEiJ.busExMemoryArray[a + 2] = (byte) (d >> 8);
       XEiJ.busExMemoryArray[a + 3] = (byte)  d;
     }
  },  //MMD_XMM

  MMD_XM6 {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "拡張メモリ (68060)" : "Expansion Memory (68060)";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      a -= XEiJ.busExMemoryStart;
      return XEiJ.busExMemoryArray[a];
    }
    @Override protected int mmdPbz (int a) {
      a -= XEiJ.busExMemoryStart;
      return XEiJ.busExMemoryArray[a] & 0xff;
    }
    @Override protected int mmdPws (int a) {
      a -= XEiJ.busExMemoryStart;
      return XEiJ.busExMemoryArray[a] << 8 | (XEiJ.busExMemoryArray[a + 1] & 0xff);
    }
    @Override protected int mmdPwz (int a) {
      a -= XEiJ.busExMemoryStart;
      return (char) (XEiJ.busExMemoryArray[a] << 8 | (XEiJ.busExMemoryArray[a + 1] & 0xff));
    }
    @Override protected int mmdPls (int a) {
      a -= XEiJ.busExMemoryStart;
      return XEiJ.busExMemoryArray[a] << 24 | (XEiJ.busExMemoryArray[a + 1] & 0xff) << 16 | (char) (XEiJ.busExMemoryArray[a + 2] << 8 | (XEiJ.busExMemoryArray[a + 3] & 0xff));
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       if (MC68060.CAT_ON) {
         MC68060.catReadHigh (a);
       }
       a -= XEiJ.busExMemoryStart;
       return XEiJ.busExMemoryArray[a];
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       if (MC68060.CAT_ON) {
         MC68060.catReadHigh (a);
       }
       a -= XEiJ.busExMemoryStart;
       return XEiJ.busExMemoryArray[a] & 0xff;
     }
    @Override protected int mmdRws (int a) throws M68kException {
       if (MC68060.CAT_ON) {
         MC68060.catReadHigh (a);
       }
       a -= XEiJ.busExMemoryStart;
       return XEiJ.busExMemoryArray[a] << 8 | (XEiJ.busExMemoryArray[a + 1] & 0xff);
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       if (MC68060.CAT_ON) {
         MC68060.catReadHigh (a);
       }
       a -= XEiJ.busExMemoryStart;
       return (char) (XEiJ.busExMemoryArray[a] << 8 | (XEiJ.busExMemoryArray[a + 1] & 0xff));
     }
    @Override protected int mmdRls (int a) throws M68kException {
       if (MC68060.CAT_ON) {
         MC68060.catReadHigh (a);
       }
       a -= XEiJ.busExMemoryStart;
       return XEiJ.busExMemoryArray[a] << 24 | (XEiJ.busExMemoryArray[a + 1] & 0xff) << 16 | (char) (XEiJ.busExMemoryArray[a + 2] << 8 | (XEiJ.busExMemoryArray[a + 3] & 0xff));
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if (MC68060.CAT_ON) {
         MC68060.catWriteHigh (a);
       }
       a -= XEiJ.busExMemoryStart;
       XEiJ.busExMemoryArray[a    ] = (byte)  d;
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       if (MC68060.CAT_ON) {
         MC68060.catWriteHigh (a);
       }
       a -= XEiJ.busExMemoryStart;
       XEiJ.busExMemoryArray[a    ] = (byte) (d >> 8);
       XEiJ.busExMemoryArray[a + 1] = (byte)  d;
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       if (MC68060.CAT_ON) {
         MC68060.catWriteHigh (a);
       }
       a -= XEiJ.busExMemoryStart;
       XEiJ.busExMemoryArray[a    ] = (byte) (d >> 24);
       XEiJ.busExMemoryArray[a + 1] = (byte) (d >> 16);
       XEiJ.busExMemoryArray[a + 2] = (byte) (d >> 8);
       XEiJ.busExMemoryArray[a + 3] = (byte)  d;
     }
  },  //MMD_XM6

  //--------------------------------------------------------------------------------
  //MMD_GE0 グラフィックス画面(512ドット16色ページ0)
  //
  //  512ドット16色
  //         ------------------参照------------------        --------------格納--------------
  //    GE0  0x00c00000～0x00c7ffff  ............3210  ──  0x00000000～0x0003ffff  ....3210
  //    参照  00000000 11000yyy yyyyyyxx xxxxxxx1
  //    格納  00000000 000000yy yyyyyyyx xxxxxxxx  i=0x00000|((a>>1)&0x3ffff)
  //
  MMD_GE0 {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "グラフィックス画面 (512 ドット 16 色 ページ 0)" : "Graphics Screen (512 dots 16 colors page 0)";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      return ((a & 1) == 0 ? 0 :
              GraphicScreen.graM4[(a >> 1) & 0x3ffff]);
    }
    @Override protected int mmdPbz (int a) {
      return ((a & 1) == 0 ? 0 :
              GraphicScreen.graM4[(a >> 1) & 0x3ffff]);
    }
    @Override protected int mmdPws (int a) {
      return GraphicScreen.graM4[(a >> 1) & 0x3ffff];
    }
    @Override protected int mmdPwz (int a) {
      return GraphicScreen.graM4[(a >> 1) & 0x3ffff];
    }
    @Override protected int mmdPls (int a) {
      return (GraphicScreen.graM4[( a      >> 1) & 0x3ffff] << 16 |
              GraphicScreen.graM4[((a + 2) >> 1) & 0x3ffff]);
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       return ((a & 1) == 0 ? 0 :
               GraphicScreen.graM4[(a >> 1) & 0x3ffff]);
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       return ((a & 1) == 0 ? 0 :
               GraphicScreen.graM4[(a >> 1) & 0x3ffff]);
     }
    @Override protected int mmdRws (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       return GraphicScreen.graM4[(a >> 1) & 0x3ffff];
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       return GraphicScreen.graM4[(a >> 1) & 0x3ffff];
     }
    @Override protected int mmdRls (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram * 2;
       return (GraphicScreen.graM4[( a      >> 1) & 0x3ffff] << 16 |
               GraphicScreen.graM4[((a + 2) >> 1) & 0x3ffff]);
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       if ((a & 1) != 0) {
         GraphicScreen.graM4[(a >> 1) & 0x3ffff] = (byte) (d & 15);
         int y = (a >> 10) - CRTC.crtR13GrYCurr[0] & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       GraphicScreen.graM4[(a >> 1) & 0x3ffff] = (byte) (d & 15);
       int y = (a >> 10) - CRTC.crtR13GrYCurr[0] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram * 2;
       GraphicScreen.graM4[( a      >> 1) & 0x3ffff] = (byte) ((d >> 16) & 15);
       GraphicScreen.graM4[((a + 2) >> 1) & 0x3ffff] = (byte) ( d        & 15);
       int y = (a >> 10) - CRTC.crtR13GrYCurr[0] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
       if ((a & 1022) == 1022) {
         y = (y + 1) & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
       }
     }
  },  //MMD_GE0

  //--------------------------------------------------------------------------------
  //MMD_GE1 グラフィックス画面(512ドット16色ページ1)
  //
  //  512ドット16色
  //         ------------------参照------------------        --------------格納--------------
  //    GE1  0x00c80000～0x00cfffff  ............7654  ──  0x00040000～0x0007ffff  ....7654
  //    参照  00000000 11001yyy yyyyyyxx xxxxxxx1
  //    格納  00000000 000001yy yyyyyyyx xxxxxxxx  i=0x40000|((a>>1)&0x3ffff)
  //
  MMD_GE1 {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "グラフィックス画面 (512 ドット 16 色 ページ 1)" : "Graphics Screen (512 dots 16 colors page 1)";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      return ((a & 1) == 0 ? 0 :
              GraphicScreen.graM4[0x40000 + ((a >> 1) & 0x3ffff)]);
    }
    @Override protected int mmdPbz (int a) {
      return ((a & 1) == 0 ? 0 :
              GraphicScreen.graM4[0x40000 + ((a >> 1) & 0x3ffff)]);
    }
    @Override protected int mmdPws (int a) {
      return GraphicScreen.graM4[0x40000 + ((a >> 1) & 0x3ffff)];
    }
    @Override protected int mmdPwz (int a) {
      return GraphicScreen.graM4[0x40000 + ((a >> 1) & 0x3ffff)];
    }
    @Override protected int mmdPls (int a) {
      return (GraphicScreen.graM4[0x40000 + (( a      >> 1) & 0x3ffff)] << 16 |
              GraphicScreen.graM4[0x40000 + (((a + 2) >> 1) & 0x3ffff)]);
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       return ((a & 1) == 0 ? 0 :
               GraphicScreen.graM4[0x40000 + ((a >> 1) & 0x3ffff)]);
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       return ((a & 1) == 0 ? 0 :
               GraphicScreen.graM4[0x40000 + ((a >> 1) & 0x3ffff)]);
     }
    @Override protected int mmdRws (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       return GraphicScreen.graM4[0x40000 + ((a >> 1) & 0x3ffff)];
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       return GraphicScreen.graM4[0x40000 + ((a >> 1) & 0x3ffff)];
     }
    @Override protected int mmdRls (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram * 2;
       return (GraphicScreen.graM4[0x40000 + (( a      >> 1) & 0x3ffff)] << 16 |
               GraphicScreen.graM4[0x40000 + (((a + 2) >> 1) & 0x3ffff)]);
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       if ((a & 1) != 0) {
         GraphicScreen.graM4[0x40000 + ((a >> 1) & 0x3ffff)] = (byte) (d & 15);
         int y = (a >> 10) - CRTC.crtR13GrYCurr[1] & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       GraphicScreen.graM4[0x40000 + ((a >> 1) & 0x3ffff)] = (byte) (d & 15);
       int y = (a >> 10) - CRTC.crtR13GrYCurr[1] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram * 2;
       GraphicScreen.graM4[0x40000 + (( a      >> 1) & 0x3ffff)] = (byte) ((d >> 16) & 15);
       GraphicScreen.graM4[0x40000 + (((a + 2) >> 1) & 0x3ffff)] = (byte) ( d        & 15);
       int y = (a >> 10) - CRTC.crtR13GrYCurr[1] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
       if ((a & 1022) == 1022) {
         y = (y + 1) & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
       }
     }
  },  //MMD_GE1

  //--------------------------------------------------------------------------------
  //MMD_GE2 グラフィックス画面(512ドット16色ページ2)
  //
  //  512ドット16色
  //         ------------------参照------------------        --------------格納--------------
  //    GE2  0x00d00000～0x00d7ffff  ............ba98  ──  0x00080000～0x000bffff  ....ba98
  //    参照  00000000 11010yyy yyyyyyxx xxxxxxx1
  //    格納  00000000 000010yy yyyyyyyx xxxxxxxx  i=0x80000|((a>>1)&0x3ffff)
  //
  MMD_GE2 {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "グラフィックス画面 (512 ドット 16 色 ページ 2)" : "Graphics Screen (512 dots 16 colors page 2)";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      return ((a & 1) == 0 ? 0 :
              GraphicScreen.graM4[0x80000 + ((a >> 1) & 0x3ffff)]);
    }
    @Override protected int mmdPbz (int a) {
      return ((a & 1) == 0 ? 0 :
              GraphicScreen.graM4[0x80000 + ((a >> 1) & 0x3ffff)]);
    }
    @Override protected int mmdPws (int a) {
      return GraphicScreen.graM4[0x80000 + ((a >> 1) & 0x3ffff)];
    }
    @Override protected int mmdPwz (int a) {
      return GraphicScreen.graM4[0x80000 + ((a >> 1) & 0x3ffff)];
    }
    @Override protected int mmdPls (int a) {
      return (GraphicScreen.graM4[0x80000 + (( a      >> 1) & 0x3ffff)] << 16 |
              GraphicScreen.graM4[0x80000 + (((a + 2) >> 1) & 0x3ffff)]);
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       return ((a & 1) == 0 ? 0 :
               GraphicScreen.graM4[0x80000 + ((a >> 1) & 0x3ffff)]);
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       return ((a & 1) == 0 ? 0 :
               GraphicScreen.graM4[0x80000 + ((a >> 1) & 0x3ffff)]);
     }
    @Override protected int mmdRws (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       return GraphicScreen.graM4[0x80000 + ((a >> 1) & 0x3ffff)];
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       return GraphicScreen.graM4[0x80000 + ((a >> 1) & 0x3ffff)];
     }
    @Override protected int mmdRls (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram * 2;
       return (GraphicScreen.graM4[0x80000 + (( a      >> 1) & 0x3ffff)] << 16 |
               GraphicScreen.graM4[0x80000 + (((a + 2) >> 1) & 0x3ffff)]);
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       if ((a & 1) != 0) {
         GraphicScreen.graM4[0x80000 + ((a >> 1) & 0x3ffff)] = (byte) (d & 15);
         int y = (a >> 10) - CRTC.crtR13GrYCurr[2] & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       GraphicScreen.graM4[0x80000 + ((a >> 1) & 0x3ffff)] = (byte) (d & 15);
       int y = (a >> 10) - CRTC.crtR13GrYCurr[2] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram * 2;
       GraphicScreen.graM4[0x80000 + (( a      >> 1) & 0x3ffff)] = (byte) ((d >> 16) & 15);
       GraphicScreen.graM4[0x80000 + (((a + 2) >> 1) & 0x3ffff)] = (byte) ( d        & 15);
       int y = (a >> 10) - CRTC.crtR13GrYCurr[2] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
       if ((a & 1022) == 1022) {
         y = (y + 1) & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
       }
     }
  },  //MMD_GE2

  //--------------------------------------------------------------------------------
  //MMD_GE3 グラフィックス画面(512ドット16色ページ3)
  //
  //  512ドット16色
  //         ------------------参照------------------        --------------格納--------------
  //    GE3  0x00d80000～0x00dfffff  ............fedc  ──  0x000c0000～0x000fffff  ....fedc
  //    参照  00000000 11011yyy yyyyyyxx xxxxxxx1
  //    格納  00000000 000011yy yyyyyyyx xxxxxxxx  i=0xc0000|((a>>1)&0x3ffff)
  //
  MMD_GE3 {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "グラフィックス画面 (512 ドット 16 色 ページ 3)" : "Graphics Screen (512 dots 16 colors page 3)";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      return ((a & 1) == 0 ? 0 :
              GraphicScreen.graM4[0xc0000 + ((a >> 1) & 0x3ffff)]);
    }
    @Override protected int mmdPbz (int a) {
      return ((a & 1) == 0 ? 0 :
              GraphicScreen.graM4[0xc0000 + ((a >> 1) & 0x3ffff)]);
    }
    @Override protected int mmdPws (int a) {
      return GraphicScreen.graM4[0xc0000 + ((a >> 1) & 0x3ffff)];
    }
    @Override protected int mmdPwz (int a) {
      return GraphicScreen.graM4[0xc0000 + ((a >> 1) & 0x3ffff)];
    }
    @Override protected int mmdPls (int a) {
      return (GraphicScreen.graM4[0xc0000 + (( a      >> 1) & 0x3ffff)] << 16 |
              GraphicScreen.graM4[0xc0000 + (((a + 2) >> 1) & 0x3ffff)]);
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       return ((a & 1) == 0 ? 0 :
               GraphicScreen.graM4[0xc0000 + ((a >> 1) & 0x3ffff)]);
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       return ((a & 1) == 0 ? 0 :
               GraphicScreen.graM4[0xc0000 + ((a >> 1) & 0x3ffff)]);
     }
    @Override protected int mmdRws (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       return GraphicScreen.graM4[0xc0000 + ((a >> 1) & 0x3ffff)];
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       return GraphicScreen.graM4[0xc0000 + ((a >> 1) & 0x3ffff)];
     }
    @Override protected int mmdRls (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram * 2;
       return (GraphicScreen.graM4[0xc0000 + (( a      >> 1) & 0x3ffff)] << 16 |
               GraphicScreen.graM4[0xc0000 + (((a + 2) >> 1) & 0x3ffff)]);
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       if ((a & 1) != 0) {
         GraphicScreen.graM4[0xc0000 + ((a >> 1) & 0x3ffff)] = (byte) (d & 15);
         int y = (a >> 10) - CRTC.crtR13GrYCurr[3] & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       GraphicScreen.graM4[0xc0000 + ((a >> 1) & 0x3ffff)] = (byte) (d & 15);
       int y = (a >> 10) - CRTC.crtR13GrYCurr[3] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram * 2;
       GraphicScreen.graM4[0xc0000 + (( a      >> 1) & 0x3ffff)] = (byte) ((d >> 16) & 15);
       GraphicScreen.graM4[0xc0000 + (((a + 2) >> 1) & 0x3ffff)] = (byte) ( d        & 15);
       int y = (a >> 10) - CRTC.crtR13GrYCurr[3] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
       if ((a & 1022) == 1022) {
         y = (y + 1) & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
       }
     }
  },  //MMD_GE3

  //--------------------------------------------------------------------------------
  //MMD_GF0 グラフィックス画面(512ドット256色ページ0)
  //
  //  512ドット256色
  //         ------------------参照------------------        --------------格納--------------
  //    GF0  0x00c00000～0x00c7ffff  ........76543210  ─┬  0x00000000～0x0003ffff  ....3210
  //                                                     └  0x00040000～0x0007ffff  ....7654
  //    参照  00000000 11000yyy yyyyyyxx xxxxxxx1
  //    格納  00000000 000000yy yyyyyyyx xxxxxxxx  i=0x00000|((a>>1)&0x3ffff)
  //          00000000 000001yy yyyyyyyx xxxxxxxx  i=0x40000|((a>>1)&0x3ffff)
  //
  MMD_GF0 {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "グラフィックス画面 (512 ドット 256 色 ページ 0)" : "Graphics Screen (512 dots 256 colors page 0)";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      return (byte) ((a & 1) == 0 ? 0 :
                     GraphicScreen.graM4[0x40000 + ((a >> 1) & 0x3ffff)] << 4 |
                     GraphicScreen.graM4[          ((a >> 1) & 0x3ffff)]);
    }
    @Override protected int mmdPbz (int a) {
      return ((a & 1) == 0 ? 0 :
              GraphicScreen.graM4[0x40000 + ((a >> 1) & 0x3ffff)] << 4 |
              GraphicScreen.graM4[          ((a >> 1) & 0x3ffff)]);
    }
    @Override protected int mmdPws (int a) {
      int i = (a >> 1) & 0x3ffff;
      return (GraphicScreen.graM4[0x40000 + i] << 4 |
              GraphicScreen.graM4[          i]);
    }
    @Override protected int mmdPwz (int a) {
      int i = (a >> 1) & 0x3ffff;
      return (GraphicScreen.graM4[0x40000 + i] << 4 |
              GraphicScreen.graM4[          i]);
    }
    @Override protected int mmdPls (int a) {
      int i0 = ( a      >> 1) & 0x3ffff;
      int i1 = ((a + 2) >> 1) & 0x3ffff;
      return (GraphicScreen.graM4[0x40000 + i0] << 20 |
              GraphicScreen.graM4[          i0] << 16 |
              GraphicScreen.graM4[0x40000 + i1] <<  4 |
              GraphicScreen.graM4[          i1]);
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       return (byte) ((a & 1) == 0 ? 0 :
                      GraphicScreen.graM4[0x40000 + ((a >> 1) & 0x3ffff)] << 4 |
                      GraphicScreen.graM4[          ((a >> 1) & 0x3ffff)]);
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       return ((a & 1) == 0 ? 0 :
               GraphicScreen.graM4[0x40000 + ((a >> 1) & 0x3ffff)] << 4 |
               GraphicScreen.graM4[          ((a >> 1) & 0x3ffff)]);
     }
    @Override protected int mmdRws (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       int i = (a >> 1) & 0x3ffff;
       return (GraphicScreen.graM4[0x40000 + i] << 4 |
               GraphicScreen.graM4[          i]);
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       int i = (a >> 1) & 0x3ffff;
       return (GraphicScreen.graM4[0x40000 + i] << 4 |
               GraphicScreen.graM4[          i]);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram * 2;
       int i0 = ( a      >> 1) & 0x3ffff;
       int i1 = ((a + 2) >> 1) & 0x3ffff;
       return (GraphicScreen.graM4[0x40000 + i0] << 20 |
               GraphicScreen.graM4[          i0] << 16 |
               GraphicScreen.graM4[0x40000 + i1] << 4 |
               GraphicScreen.graM4[          i1]);
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       if ((a & 1) != 0) {
         int i = (a >> 1) & 0x3ffff;
         GraphicScreen.graM4[0x40000 + i] = (byte) ((d >> 4) & 15);
         GraphicScreen.graM4[          i] = (byte) ( d       & 15);
         a >>= 10;
         int y = a - CRTC.crtR13GrYCurr[0] & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
         y = a - CRTC.crtR13GrYCurr[1] & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       int i = (a >> 1) & 0x3ffff;
       GraphicScreen.graM4[0x40000 + i] = (byte) ((d >> 4) & 15);
       GraphicScreen.graM4[          i] = (byte) ( d       & 15);
       a >>= 10;
       int y = a - CRTC.crtR13GrYCurr[0] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
       y = a - CRTC.crtR13GrYCurr[1] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram * 2;
       int i0 = ( a      >> 1) & 0x3ffff;
       int i1 = ((a + 2) >> 1) & 0x3ffff;
       GraphicScreen.graM4[0x40000 + i0] = (byte) ((d >> 20) & 15);
       GraphicScreen.graM4[          i0] = (byte) ((d >> 16) & 15);
       GraphicScreen.graM4[0x40000 + i1] = (byte) ((d >>  4) & 15);
       GraphicScreen.graM4[          i1] = (byte) ( d        & 15);
       int b = (a + 2) >>> 10;
       int y = b - CRTC.crtR13GrYCurr[0] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
       y = b - CRTC.crtR13GrYCurr[1] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
       a >>>= 10;
       if (a != b) {
         y = a - CRTC.crtR13GrYCurr[0] & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
         y = a - CRTC.crtR13GrYCurr[1] & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
       }
     }
  },  //MMD_GF0

  //--------------------------------------------------------------------------------
  //MMD_GF1 グラフィックス画面(512ドット256色ページ1)
  //
  //  512ドット256色
  //         ------------------参照------------------        --------------格納--------------
  //    GF1  0x00c80000～0x00cfffff  ........fedcba98  ─┬  0x00080000～0x000bffff  ....ba98
  //                                                     └  0x000c0000～0x000fffff  ....fedc
  //    参照  00000000 11001yyy yyyyyyxx xxxxxxx1
  //    格納  00000000 000010yy yyyyyyyx xxxxxxxx  i=0x80000|((a>>1)&0x3ffff)
  //          00000000 000011yy yyyyyyyx xxxxxxxx  i=0xc0000|((a>>1)&0x3ffff)
  //
  MMD_GF1 {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "グラフィックス画面 (512 ドット 256 色 ページ 1)" : "Graphics Screen (512 dots 256 colors page 1)";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      return (byte) ((a & 1) == 0 ? 0 :
                     GraphicScreen.graM4[0xc0000 + ((a >> 1) & 0x3ffff)] << 4 |
                     GraphicScreen.graM4[0x80000 + ((a >> 1) & 0x3ffff)]);
    }
    @Override protected int mmdPbz (int a) {
      return ((a & 1) == 0 ? 0 :
              GraphicScreen.graM4[0xc0000 + ((a >> 1) & 0x3ffff)] << 4 |
              GraphicScreen.graM4[0x80000 + ((a >> 1) & 0x3ffff)]);
    }
    @Override protected int mmdPws (int a) {
      int i = (a >> 1) & 0x3ffff;
      return (GraphicScreen.graM4[0xc0000 + i] << 4 |
              GraphicScreen.graM4[0x80000 + i]);
    }
    @Override protected int mmdPwz (int a) {
      int i = (a >> 1) & 0x3ffff;
      return (GraphicScreen.graM4[0xc0000 + i] << 4 |
              GraphicScreen.graM4[0x80000 + i]);
    }
    @Override protected int mmdPls (int a) {
      int i0 = ( a      >> 1) & 0x3ffff;
      int i1 = ((a + 2) >> 1) & 0x3ffff;
      return (GraphicScreen.graM4[0xc0000 + i0] << 20 |
              GraphicScreen.graM4[0x80000 + i0] << 16 |
              GraphicScreen.graM4[0xc0000 + i1] <<  4 |
              GraphicScreen.graM4[0x80000 + i1]);
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       return (byte) ((a & 1) == 0 ? 0 :
                      GraphicScreen.graM4[0xc0000 + ((a >> 1) & 0x3ffff)] << 4 |
                      GraphicScreen.graM4[0x80000 + ((a >> 1) & 0x3ffff)]);
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       return ((a & 1) == 0 ? 0 :
               GraphicScreen.graM4[0xc0000 + ((a >> 1) & 0x3ffff)] << 4 |
               GraphicScreen.graM4[0x80000 + ((a >> 1) & 0x3ffff)]);
     }
    @Override protected int mmdRws (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       int i = (a >> 1) & 0x3ffff;
       return (GraphicScreen.graM4[0xc0000 + i] << 4 |
               GraphicScreen.graM4[0x80000 + i]);
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       int i = (a >> 1) & 0x3ffff;
       return (GraphicScreen.graM4[0xc0000 + i] << 4 |
               GraphicScreen.graM4[0x80000 + i]);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram * 2;
       int i0 = ( a      >> 1) & 0x3ffff;
       int i1 = ((a + 2) >> 1) & 0x3ffff;
       return (GraphicScreen.graM4[0xc0000 + i0] << 20 |
               GraphicScreen.graM4[0x80000 + i0] << 16 |
               GraphicScreen.graM4[0xc0000 + i1] << 4 |
               GraphicScreen.graM4[0x80000 + i1]);
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       if ((a & 1) != 0) {
         int i = (a >> 1) & 0x3ffff;
         GraphicScreen.graM4[0xc0000 + i] = (byte) ((d >> 4) & 15);
         GraphicScreen.graM4[0x80000 + i] = (byte) ( d       & 15);
         a >>= 10;
         int y = a - CRTC.crtR13GrYCurr[2] & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
         y = a - CRTC.crtR13GrYCurr[3] & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       int i = (a >> 1) & 0x3ffff;
       GraphicScreen.graM4[0xc0000 + i] = (byte) ((d >> 4) & 15);
       GraphicScreen.graM4[0x80000 + i] = (byte) ( d       & 15);
       a >>= 10;
       int y = a - CRTC.crtR13GrYCurr[2] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
       y = a - CRTC.crtR13GrYCurr[3] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram * 2;
       int i0 = ( a      >> 1) & 0x3ffff;
       int i1 = ((a + 2) >> 1) & 0x3ffff;
       GraphicScreen.graM4[0xc0000 + i0] = (byte) ((d >> 20) & 15);
       GraphicScreen.graM4[0x80000 + i0] = (byte) ((d >> 16) & 15);
       GraphicScreen.graM4[0xc0000 + i1] = (byte) ((d >>  4) & 15);
       GraphicScreen.graM4[0x80000 + i1] = (byte) ( d        & 15);
       int b = (a + 2) >>> 10;
       int y = b - CRTC.crtR13GrYCurr[2] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
       y = b - CRTC.crtR13GrYCurr[3] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
       a >>>= 10;
       if (a != b) {
         y = a - CRTC.crtR13GrYCurr[2] & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
         y = a - CRTC.crtR13GrYCurr[3] & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
       }
     }
  },  //MMD_GF1

  //--------------------------------------------------------------------------------
  //MMD_GM2 グラフィックス画面(メモリモード2)
  //
  //  バイトリード
  //    0x00c00000  0x00が読み出される
  //    0x00c00001  4bitページ1-0が読み出される
  //    0x00c80000  0x00が読み出される
  //    0x00c80001  0x00が読み出される
  //    0x00d00000  0x00が読み出される
  //    0x00d00001  4bitページ1-0が読み出される
  //    0x00d80000  0x00が読み出される
  //    0x00d80001  0x00が読み出される
  //
  //  ワードリード
  //    0x00c00000  上位バイトは0x00で下位バイトに4bitページ1-0が読み出される
  //    0x00c80000  上位バイトは0x00で下位バイトに4bitページ3-2が読み出される
  //    0x00d00000  上位バイトは0x00で下位バイトに4bitページ1-0が読み出される
  //    0x00d80000  上位バイトは0x00で下位バイトに4bitページ3-2が読み出される
  //
  //  バイトライト
  //    0x00c00000  060turboの060モードのときデータは無視され0x00が4bitページ3-2に書き込まれる。それ以外はデータが4bitページ3-2に書き込まれる
  //    0x00c00001  データが4bitページ1-0に書き込まれる
  //    0x00c80000  060turboの060モードのときデータは無視され0x00が4bitページ3-2に書き込まれる。それ以外はデータが4bitページ3-2に書き込まれる
  //    0x00c80001  データが4bitページ1-0に書き込まれる
  //    0x00d00000  060turboの060モードのときデータは無視され0x00が4bitページ3-2に書き込まれる。それ以外はデータが4bitページ3-2に書き込まれる
  //    0x00d00001  データが4bitページ1-0に書き込まれる
  //    0x00d80000  060turboの060モードのときデータは無視され0x00が4bitページ3-2に書き込まれる。それ以外はデータが4bitページ3-2に書き込まれる
  //    0x00d80001  データが4bitページ1-0に書き込まれる
  //
  //  ワードライト
  //    0x00c00000  上位バイトは無視され下位バイトが4bitページ3-2と4bitページ1-0の両方に書き込まれる
  //    0x00c80000  上位バイトは無視され下位バイトが4bitページ3-2と4bitページ1-0の両方に書き込まれる
  //    0x00d00000  上位バイトは無視され下位バイトが4bitページ3-2と4bitページ1-0の両方に書き込まれる
  //    0x00d80000  上位バイトは無視され下位バイトが4bitページ3-2と4bitページ1-0の両方に書き込まれる
  //
  MMD_GM2 {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "グラフィックス画面 (メモリモード 2)" : "Graphics Screen (memory mode 2)";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      if ((a & 0x00080001) == 0x00000001) {  //0x00c00000..0x00c7ffff,0x00d00000..0x00d7ffffかつ下位バイト
        int i = (a >> 1) & 0x3ffff;
        return (byte) (GraphicScreen.graM4[0x40000 + i] << 4 |  //4bitページ1
                       GraphicScreen.graM4[          i]);  //4bitページ0
      } else {  //0x00c80000..0x00cfffff,0x00d80000..0x00dfffffまたは上位バイト
        return 0x00;
      }
    }
    @Override protected int mmdPbz (int a) {
      if ((a & 0x00080001) == 0x00000001) {  //0x00c00000..0x00c7ffff,0x00d00000..0x00d7ffffかつ下位バイト
        int i = (a >> 1) & 0x3ffff;
        return (GraphicScreen.graM4[0x40000 + i] << 4 |  //4bitページ1
                GraphicScreen.graM4[          i]);  //4bitページ0
      } else {  //0x00c80000..0x00cfffff,0x00d80000..0x00dfffffまたは上位バイト
        return 0x00;
      }
    }
    @Override protected int mmdPws (int a) {
      int i = (a >> 1) & 0x3ffff;
      if ((a & 0x00080000) == 0x00000000) {  //0x00c00000..0x00c7ffff,0x00d00000..0x00d7ffff
        return (GraphicScreen.graM4[0x40000 + i] << 4 |  //4bitページ1
                GraphicScreen.graM4[          i]);  //4bitページ0
      } else {  //0x00c80000..0x00cfffff,0x00d80000..0x00dfffff
        return (GraphicScreen.graM4[0xc0000 + i] << 4 |  //4bitページ3
                GraphicScreen.graM4[0x80000 + i]);  //4bitページ2
      }
    }
    @Override protected int mmdPwz (int a) {
      int i = (a >> 1) & 0x3ffff;
      if ((a & 0x00080000) == 0x00000000) {  //0x00c00000..0x00c7ffff,0x00d00000..0x00d7ffff
        return (GraphicScreen.graM4[0x40000 + i] << 4 |  //4bitページ1
                GraphicScreen.graM4[          i]);  //4bitページ0
      } else {  //0x00c80000..0x00cfffff,0x00d80000..0x00dfffff
        return (GraphicScreen.graM4[0xc0000 + i] << 4 |  //4bitページ3
                GraphicScreen.graM4[0x80000 + i]);  //4bitページ2
      }
    }
    @Override protected int mmdPls (int a) {
      return mmdPws (a) << 16 | mmdPwz (a + 2);
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       if ((a & 0x00080001) == 0x00000001) {  //0x00c00000..0x00c7ffff,0x00d00000..0x00d7ffffかつ下位バイト
         int i = (a >> 1) & 0x3ffff;
         return (byte) (GraphicScreen.graM4[0x40000 + i] << 4 |  //4bitページ1
                        GraphicScreen.graM4[          i]);  //4bitページ0
       } else {  //0x00c80000..0x00cfffff,0x00d80000..0x00dfffffまたは上位バイト
         return 0x00;
       }
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       if ((a & 0x00080001) == 0x00000001) {  //0x00c00000..0x00c7ffff,0x00d00000..0x00d7ffffかつ下位バイト
         int i = (a >> 1) & 0x3ffff;
         return (GraphicScreen.graM4[0x40000 + i] << 4 |  //4bitページ1
                 GraphicScreen.graM4[          i]);  //4bitページ0
       } else {  //0x00c80000..0x00cfffff,0x00d80000..0x00dfffffまたは上位バイト
         return 0x00;
       }
     }
    @Override protected int mmdRws (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       int i = (a >> 1) & 0x3ffff;
       if ((a & 0x00080000) == 0x00000000) {  //0x00c00000..0x00c7ffff,0x00d00000..0x00d7ffff
         return (GraphicScreen.graM4[0x40000 + i] << 4 |  //4bitページ1
                 GraphicScreen.graM4[          i]);  //4bitページ0
       } else {  //0x00c80000..0x00cfffff,0x00d80000..0x00dfffff
         return (GraphicScreen.graM4[0xc0000 + i] << 4 |  //4bitページ3
                 GraphicScreen.graM4[0x80000 + i]);  //4bitページ2
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       int i = (a >> 1) & 0x3ffff;
       if ((a & 0x00080000) == 0x00000000) {  //0x00c00000..0x00c7ffff,0x00d00000..0x00d7ffff
         return (GraphicScreen.graM4[0x40000 + i] << 4 |  //4bitページ1
                 GraphicScreen.graM4[          i]);  //4bitページ0
       } else {  //0x00c80000..0x00cfffff,0x00d80000..0x00dfffff
         return (GraphicScreen.graM4[0xc0000 + i] << 4 |  //4bitページ3
                 GraphicScreen.graM4[0x80000 + i]);  //4bitページ2
       }
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return mmdRws (a) << 16 | mmdRwz (a + 2);
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       int i = (a >> 1) & 0x3ffff;
       if ((a & 0x00000001) == 0x00000000) {  //上位バイト
         if (XEiJ.currentMPU < Model.MPU_MC68LC040) {
           GraphicScreen.graM4[0xc0000 + i] = (byte) ((d >> 4) & 15);  //4bitページ3
           GraphicScreen.graM4[0x80000 + i] = (byte) ( d       & 15);  //4bitページ2
         } else {
           GraphicScreen.graM4[0xc0000 + i] = 0;  //4bitページ3
           GraphicScreen.graM4[0x80000 + i] = 0;  //4bitページ2
         }
         a >>>= 10;
         int y = a - CRTC.crtR13GrYCurr[3] & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
         y = a - CRTC.crtR13GrYCurr[2] & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
       } else {  //下位バイト
         GraphicScreen.graM4[0x40000 + i] = (byte) ((d >> 4) & 15);  //4bitページ1
         GraphicScreen.graM4[          i] = (byte) ( d       & 15);  //4bitページ0
         a >>>= 10;
         int y = a - CRTC.crtR13GrYCurr[1] & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
         y = a - CRTC.crtR13GrYCurr[0] & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       int i = (a >> 1) & 0x3ffff;
       GraphicScreen.graM4[0xc0000 + i] = (byte) ((d >>  4) & 15);  //4bitページ3
       GraphicScreen.graM4[0x80000 + i] = (byte) ( d        & 15);  //4bitページ2
       GraphicScreen.graM4[0x40000 + i] = (byte) ((d >>  4) & 15);  //4bitページ1
       GraphicScreen.graM4[          i] = (byte) ( d        & 15);  //4bitページ0
       a >>>= 10;
       int y = a - CRTC.crtR13GrYCurr[3] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
       y = a - CRTC.crtR13GrYCurr[2] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
       y = a - CRTC.crtR13GrYCurr[1] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
       y = a - CRTC.crtR13GrYCurr[0] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWw (a, d >> 16);
       mmdWw (a + 2, d);
     }
  },  //MMD_GM2

  //--------------------------------------------------------------------------------
  //MMD_GG0 グラフィックス画面(512ドット65536色)
  //
  //  512ドット65536色
  //         ------------------参照------------------        --------------格納--------------
  //    GG0  0x00c00000～0x00c7ffff  fedcba9876543210  ─┬  0x00000000～0x0003ffff  ....3210
  //                                                     ├  0x00040000～0x0007ffff  ....7654
  //                                                     ├  0x00080000～0x000bffff  ....ba98
  //                                                     └  0x000c0000～0x000fffff  ....fedc
  //    参照  00000000 11000yyy yyyyyyxx xxxxxxx1
  //    格納  00000000 000000yy yyyyyyyx xxxxxxxx  i=0x00000|((a>>1)&0x3ffff)
  //          00000000 000001yy yyyyyyyx xxxxxxxx  i=0x40000|((a>>1)&0x3ffff)
  //    参照  00000000 11000yyy yyyyyyxx xxxxxxx0
  //    格納  00000000 000010yy yyyyyyyx xxxxxxxx  i=0x80000|((a>>1)&0x3ffff)
  //          00000000 000011yy yyyyyyyx xxxxxxxx  i=0xc0000|((a>>1)&0x3ffff)
  //
  MMD_GG0 {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "グラフィックス画面 (512 ドット 65536 色)" : "Graphics Screen (512 dots 65536 colors)";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      int i = (a >> 1) & 0x3ffff;
      return (byte) ((a & 1) == 0 ?
                     GraphicScreen.graM4[0xc0000 + i] << 4 |
                     GraphicScreen.graM4[0x80000 + i] :
                     GraphicScreen.graM4[0x40000 + i] << 4 |
                     GraphicScreen.graM4[          i]);
    }
    @Override protected int mmdPbz (int a) {
      int i = (a >> 1) & 0x3ffff;
      return ((a & 1) == 0 ?
              GraphicScreen.graM4[0xc0000 + i] << 4 |
              GraphicScreen.graM4[0x80000 + i] :
              GraphicScreen.graM4[0x40000 + i] << 4 |
              GraphicScreen.graM4[          i]);
    }
    @Override protected int mmdPws (int a) {
      int i = (a >> 1) & 0x3ffff;
      return (short) (GraphicScreen.graM4[0xc0000 + i] << 12 |
                      GraphicScreen.graM4[0x80000 + i] <<  8 |
                      GraphicScreen.graM4[0x40000 + i] <<  4 |
                      GraphicScreen.graM4[          i]);
    }
    @Override protected int mmdPwz (int a) {
      int i = (a >> 1) & 0x3ffff;
      return (GraphicScreen.graM4[0xc0000 + i] << 12 |
              GraphicScreen.graM4[0x80000 + i] <<  8 |
              GraphicScreen.graM4[0x40000 + i] <<  4 |
              GraphicScreen.graM4[          i]);
    }
    @Override protected int mmdPls (int a) {
      int i0 = ( a      >> 1) & 0x3ffff;
      int i1 = ((a + 2) >> 1) & 0x3ffff;
      return (GraphicScreen.graM4[0xc0000 + i0] << 28 |
              GraphicScreen.graM4[0x80000 + i0] << 24 |
              GraphicScreen.graM4[0x40000 + i0] << 20 |
              GraphicScreen.graM4[          i0] << 16 |
              GraphicScreen.graM4[0xc0000 + i1] << 12 |
              GraphicScreen.graM4[0x80000 + i1] <<  8 |
              GraphicScreen.graM4[0x40000 + i1] <<  4 |
              GraphicScreen.graM4[          i1]);
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       int i = (a >> 1) & 0x3ffff;
       return (byte) ((a & 1) == 0 ?
                      GraphicScreen.graM4[0xc0000 + i] << 4 |
                      GraphicScreen.graM4[0x80000 + i] :
                      GraphicScreen.graM4[0x40000 + i] << 4 |
                      GraphicScreen.graM4[          i]);
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       int i = (a >> 1) & 0x3ffff;
       return ((a & 1) == 0 ?
               GraphicScreen.graM4[0xc0000 + i] << 4 |
               GraphicScreen.graM4[0x80000 + i] :
               GraphicScreen.graM4[0x40000 + i] << 4 |
               GraphicScreen.graM4[          i]);
     }
    @Override protected int mmdRws (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       int i = (a >> 1) & 0x3ffff;
       return (short) (GraphicScreen.graM4[0xc0000 + i] << 12 |
                       GraphicScreen.graM4[0x80000 + i] <<  8 |
                       GraphicScreen.graM4[0x40000 + i] <<  4 |
                       GraphicScreen.graM4[          i]);
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       int i = (a >> 1) & 0x3ffff;
       return (GraphicScreen.graM4[0xc0000 + i] << 12 |
               GraphicScreen.graM4[0x80000 + i] <<  8 |
               GraphicScreen.graM4[0x40000 + i] <<  4 |
               GraphicScreen.graM4[          i]);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram * 2;
       int i0 = ( a      >> 1) & 0x3ffff;
       int i1 = ((a + 2) >> 1) & 0x3ffff;
       return (GraphicScreen.graM4[0xc0000 + i0] << 28 |
               GraphicScreen.graM4[0x80000 + i0] << 24 |
               GraphicScreen.graM4[0x40000 + i0] << 20 |
               GraphicScreen.graM4[          i0] << 16 |
               GraphicScreen.graM4[0xc0000 + i1] << 12 |
               GraphicScreen.graM4[0x80000 + i1] <<  8 |
               GraphicScreen.graM4[0x40000 + i1] <<  4 |
               GraphicScreen.graM4[          i1]);
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       int i = (a >> 1) & 0x3ffff;
       if ((a & 1) == 0) {
         GraphicScreen.graM4[0xc0000 + i] = (byte) ((d >> 4) & 15);
         GraphicScreen.graM4[0x80000 + i] = (byte) ( d       & 15);
         a >>= 10;
         int y = a - CRTC.crtR13GrYCurr[2] & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
         y = a - CRTC.crtR13GrYCurr[3] & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
       } else {
         GraphicScreen.graM4[0x40000 + i] = (byte) ((d >> 4) & 15);
         GraphicScreen.graM4[          i] = (byte) ( d       & 15);
         a >>= 10;
         int y = a - CRTC.crtR13GrYCurr[0] & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
         y = a - CRTC.crtR13GrYCurr[1] & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       int i = (a >> 1) & 0x3ffff;
       GraphicScreen.graM4[0xc0000 + i] = (byte) ((d >> 12) & 15);
       GraphicScreen.graM4[0x80000 + i] = (byte) ((d >>  8) & 15);
       GraphicScreen.graM4[0x40000 + i] = (byte) ((d >>  4) & 15);
       GraphicScreen.graM4[          i] = (byte) ( d        & 15);
       a >>= 10;
       int y = a - CRTC.crtR13GrYCurr[0] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
       y = a - CRTC.crtR13GrYCurr[1] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
       y = a - CRTC.crtR13GrYCurr[2] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
       y = a - CRTC.crtR13GrYCurr[3] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram * 2;
       int i0 = ( a      >> 1) & 0x3ffff;
       int i1 = ((a + 2) >> 1) & 0x3ffff;
       GraphicScreen.graM4[0xc0000 + i0] = (byte) (d >>> 28);
       GraphicScreen.graM4[0x80000 + i0] = (byte) ((d >> 24) & 15);
       GraphicScreen.graM4[0x40000 + i0] = (byte) ((d >> 20) & 15);
       GraphicScreen.graM4[          i0] = (byte) ((d >> 16) & 15);
       GraphicScreen.graM4[0xc0000 + i1] = (byte) ((char) d >> 12);
       GraphicScreen.graM4[0x80000 + i1] = (byte) ((d >>  8) & 15);
       GraphicScreen.graM4[0x40000 + i1] = (byte) ((d >>  4) & 15);
       GraphicScreen.graM4[          i1] = (byte) ( d        & 15);
       int b = (a + 2) >>> 10;
       int y = b - CRTC.crtR13GrYCurr[0] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
       y = b - CRTC.crtR13GrYCurr[1] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
       y = b - CRTC.crtR13GrYCurr[2] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
       y = b - CRTC.crtR13GrYCurr[3] & 511;
       CRTC.crtRasterStamp[y      ] = 0;
       CRTC.crtRasterStamp[y + 512] = 0;
       a >>>= 10;
       if (a != b) {
         y = a - CRTC.crtR13GrYCurr[0] & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
         y = a - CRTC.crtR13GrYCurr[1] & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
         y = a - CRTC.crtR13GrYCurr[2] & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
         y = a - CRTC.crtR13GrYCurr[3] & 511;
         CRTC.crtRasterStamp[y      ] = 0;
         CRTC.crtRasterStamp[y + 512] = 0;
       }
     }
  },  //MMD_GG0

  //--------------------------------------------------------------------------------
  //MMD_GH0 グラフィックス画面(1024ドット16色)
  //
  //  1024ドット16色
  //         ------------------参照------------------        --------------格納--------------
  //    GH0  0x00c00000～0x00dfffff  ............3210  ──  0x00000000～0x000fffff  ....3210
  //    参照  00000000 110Yyyyy yyyyyXxx xxxxxxx1
  //    格納  00000000 0000YXyy yyyyyyyx xxxxxxxx  i=0x000000|((a>>1)&0x801ff)|((a<<8)&0x40000)|((a>>2)&0x3fe00)
  //
  MMD_GH0 {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "グラフィックス画面 (1024 ドット 16 色)" : "Graphics Screen (1024 dots 16 colors)";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      return ((a & 1) == 0 ? 0 :
              GraphicScreen.graM4[((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00)]);
    }
    @Override protected int mmdPbz (int a) {
      return ((a & 1) == 0 ? 0 :
              GraphicScreen.graM4[((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00)]);
    }
    @Override protected int mmdPws (int a) {
      return GraphicScreen.graM4[((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00)];
    }
    @Override protected int mmdPwz (int a) {
      return GraphicScreen.graM4[((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00)];
    }
    @Override protected int mmdPls (int a) {
      int i0 = ((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00);
      a += 2;
      int i1 = ((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00);
      return (GraphicScreen.graM4[i0] << 16 |
              GraphicScreen.graM4[i1]);
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       return ((a & 1) == 0 ? 0 :
               GraphicScreen.graM4[((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00)]);
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       return ((a & 1) == 0 ? 0 :
               GraphicScreen.graM4[((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00)]);
     }
    @Override protected int mmdRws (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       return GraphicScreen.graM4[((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00)];
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       return GraphicScreen.graM4[((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00)];
     }
    @Override protected int mmdRls (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram * 2;
       int i0 = ((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00);
       a += 2;
       int i1 = ((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00);
       return (GraphicScreen.graM4[i0] << 16 |
               GraphicScreen.graM4[i1]);
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       if ((a & 1) != 0) {
         CRTC.crtRasterStamp[(a >> 11) - CRTC.crtR13GrYCurr[0] & 1023] = 0;
         GraphicScreen.graM4[((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00)] = (byte) (d & 15);
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       CRTC.crtRasterStamp[(a >> 11) - CRTC.crtR13GrYCurr[0] & 1023] = 0;
       GraphicScreen.graM4[((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00)] = (byte) (d & 15);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram * 2;
       CRTC.crtRasterStamp[(a >> 11) - CRTC.crtR13GrYCurr[0] & 1023] = 0;
       GraphicScreen.graM4[((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00)] = (byte) ((d >> 16) & 15);
       a += 2;
       CRTC.crtRasterStamp[(a >> 11) - CRTC.crtR13GrYCurr[0] & 1023] = 0;
       GraphicScreen.graM4[((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00)] = (byte) ( d        & 15);
     }
  },  //MMD_GH0

  //--------------------------------------------------------------------------------
  //MMD_GI0 グラフィックス画面(1024ドット256色)
  //
  //  1024ドット256色
  //         ------------------参照------------------        --------------格納--------------
  //    GI0  0x00c00000～0x00dfffff  ........76543210  ─┬  0x00000000～0x000fffff  ....3210
  //                                                     └  0x00100000～0x001fffff  ....7654
  //    参照  00000000 110Yyyyy yyyyyXxx xxxxxxx1
  //    格納  00000000 0000YXyy yyyyyyyx xxxxxxxx  i=0x000000|((a>>1)&0x801ff)|((a<<8)&0x40000)|((a>>2)&0x3fe00)
  //          00000000 0001YXyy yyyyyyyx xxxxxxxx  i=0x100000|((a>>1)&0x801ff)|((a<<8)&0x40000)|((a>>2)&0x3fe00)
  //
  MMD_GI0 {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "グラフィックス画面 (1024 ドット 256 色)" : "Graphics Screen (1024 dots 256 colors)";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      int i = ((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00);
      return (byte) ((a & 1) == 0 ? 0 :
                     GraphicScreen.graM4[0x100000 + i] << 4 |
                     GraphicScreen.graM4[           i]);
    }
    @Override protected int mmdPbz (int a) {
      int i = ((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00);
      return ((a & 1) == 0 ? 0 :
              GraphicScreen.graM4[0x100000 + i] << 4 |
              GraphicScreen.graM4[           i]);
    }
    @Override protected int mmdPws (int a) {
      int i = ((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00);
      return (GraphicScreen.graM4[0x100000 + i] << 4 |
              GraphicScreen.graM4[           i]);
    }
    @Override protected int mmdPwz (int a) {
      int i = ((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00);
      return (GraphicScreen.graM4[0x100000 + i] << 4 |
              GraphicScreen.graM4[           i]);
    }
    @Override protected int mmdPls (int a) {
      int i0 = ((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00);
      a += 2;
      int i1 = ((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00);
      return (GraphicScreen.graM4[0x100000 + i0] << 20 |
              GraphicScreen.graM4[           i0] << 16 |
              GraphicScreen.graM4[0x100000 + i1] <<  4 |
              GraphicScreen.graM4[           i1]);
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       int i = ((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00);
       return (byte) ((a & 1) == 0 ? 0 :
                      GraphicScreen.graM4[0x100000 + i] << 4 |
                      GraphicScreen.graM4[           i]);
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       int i = ((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00);
       return ((a & 1) == 0 ? 0 :
               GraphicScreen.graM4[0x100000 + i] << 4 |
               GraphicScreen.graM4[           i]);
     }
    @Override protected int mmdRws (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       int i = ((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00);
       return (GraphicScreen.graM4[0x100000 + i] << 4 |
               GraphicScreen.graM4[           i]);
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       int i = ((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00);
       return (GraphicScreen.graM4[0x100000 + i] << 4 |
               GraphicScreen.graM4[           i]);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram * 2;
       int i0 = ((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00);
       a += 2;
       int i1 = ((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00);
       return (GraphicScreen.graM4[0x100000 + i0] << 20 |
               GraphicScreen.graM4[           i0] << 16 |
               GraphicScreen.graM4[0x100000 + i1] <<  4 |
               GraphicScreen.graM4[           i1]);
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       if ((a & 1) != 0) {
         CRTC.crtRasterStamp[(a >> 11) - CRTC.crtR13GrYCurr[0] & 1023] = 0;
         int i = ((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00);
         GraphicScreen.graM4[0x100000 + i] = (byte) ((d >> 4) & 15);
         GraphicScreen.graM4[           i] = (byte) ( d       & 15);
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       CRTC.crtRasterStamp[(a >> 11) - CRTC.crtR13GrYCurr[0] & 1023] = 0;
       int i = ((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00);
       GraphicScreen.graM4[0x100000 + i] = (byte) ((d >> 4) & 15);
       GraphicScreen.graM4[           i] = (byte) ( d       & 15);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram * 2;
       CRTC.crtRasterStamp[(a >> 11) - CRTC.crtR13GrYCurr[0] & 1023] = 0;
       int i0 = ((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00);
       a += 2;
       CRTC.crtRasterStamp[(a >> 11) - CRTC.crtR13GrYCurr[0] & 1023] = 0;
       int i1 = ((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00);
       GraphicScreen.graM4[0x100000 + i0] = (byte) ((d >> 20) & 15);
       GraphicScreen.graM4[           i0] = (byte) ((d >> 16) & 15);
       GraphicScreen.graM4[0x100000 + i1] = (byte) ((d >>  4) & 15);
       GraphicScreen.graM4[           i1] = (byte) ( d        & 15);
     }
  },  //MMD_GI0

  //--------------------------------------------------------------------------------
  //MMD_GJ0 グラフィックス画面(1024ドット65536色)
  //
  //  1024ドット65536色
  //         ------------------参照------------------        --------------格納--------------
  //    GJ0  0x00c00000～0x00dfffff  fedcba9876543210  ─┬  0x00000000～0x000fffff  ....3210
  //                                                     ├  0x00100000～0x001fffff  ....7654
  //                                                     ├  0x00200000～0x002fffff  ....ba98
  //                                                     └  0x00300000～0x003fffff  ....fedc
  //    参照  00000000 110Yyyyy yyyyyXxx xxxxxxx1
  //    格納  00000000 0000YXyy yyyyyyyx xxxxxxxx  i=0x000000|((a>>1)&0x801ff)|((a<<8)&0x40000)|((a>>2)&0x3fe00)
  //          00000000 0001YXyy yyyyyyyx xxxxxxxx  i=0x100000|((a>>1)&0x801ff)|((a<<8)&0x40000)|((a>>2)&0x3fe00)
  //    参照  00000000 110Yyyyy yyyyyXxx xxxxxxx0
  //    格納  00000000 0010YXyy yyyyyyyx xxxxxxxx  i=0x200000|((a>>1)&0x801ff)|((a<<8)&0x40000)|((a>>2)&0x3fe00)
  //          00000000 0011YXyy yyyyyyyx xxxxxxxx  i=0x300000|((a>>1)&0x801ff)|((a<<8)&0x40000)|((a>>2)&0x3fe00)
  //
  MMD_GJ0 {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "グラフィックス画面 (1024 ドット 65536 色)" : "Graphics Screen (1024 dots 65536 colors)";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      int i = ((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00);
      return (byte) ((a & 1) == 0 ?
                     GraphicScreen.graM4[0x300000 + i] << 4 |
                     GraphicScreen.graM4[0x200000 + i] :
                     GraphicScreen.graM4[0x100000 + i] << 4 |
                     GraphicScreen.graM4[           i]);
    }
    @Override protected int mmdPbz (int a) {
      int i = ((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00);
      return ((a & 1) == 0 ?
              GraphicScreen.graM4[0x300000 + i] << 4 |
              GraphicScreen.graM4[0x200000 + i] :
              GraphicScreen.graM4[0x100000 + i] << 4 |
              GraphicScreen.graM4[           i]);
    }
    @Override protected int mmdPws (int a) {
      int i = ((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00);
      return (short) (GraphicScreen.graM4[0x300000 + i] << 12 |
                      GraphicScreen.graM4[0x200000 + i] <<  8 |
                      GraphicScreen.graM4[0x100000 + i] <<  4 |
                      GraphicScreen.graM4[           i]);
    }
    @Override protected int mmdPwz (int a) {
      int i = ((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00);
      return (GraphicScreen.graM4[0x300000 + i] << 12 |
              GraphicScreen.graM4[0x200000 + i] <<  8 |
              GraphicScreen.graM4[0x100000 + i] <<  4 |
              GraphicScreen.graM4[           i]);
    }
    @Override protected int mmdPls (int a) {
      int i0 = ((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00);
      a += 2;
      int i1 = ((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00);
      return (GraphicScreen.graM4[0x300000 + i0] << 28 |
              GraphicScreen.graM4[0x200000 + i0] << 24 |
              GraphicScreen.graM4[0x100000 + i0] << 20 |
              GraphicScreen.graM4[           i0] << 16 |
              GraphicScreen.graM4[0x300000 + i1] << 12 |
              GraphicScreen.graM4[0x200000 + i1] <<  8 |
              GraphicScreen.graM4[0x100000 + i1] <<  4 |
              GraphicScreen.graM4[           i1]);
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       int i = ((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00);
       return (byte) ((a & 1) == 0 ?
                      GraphicScreen.graM4[0x300000 + i] << 4 |
                      GraphicScreen.graM4[0x200000 + i] :
                      GraphicScreen.graM4[0x100000 + i] << 4 |
                      GraphicScreen.graM4[           i]);
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       int i = ((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00);
       return ((a & 1) == 0 ?
               GraphicScreen.graM4[0x300000 + i] << 4 |
               GraphicScreen.graM4[0x200000 + i] :
               GraphicScreen.graM4[0x100000 + i] << 4 |
               GraphicScreen.graM4[           i]);
     }
    @Override protected int mmdRws (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       int i = ((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00);
       return (short) (GraphicScreen.graM4[0x300000 + i] << 12 |
                       GraphicScreen.graM4[0x200000 + i] <<  8 |
                       GraphicScreen.graM4[0x100000 + i] <<  4 |
                       GraphicScreen.graM4[           i]);
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       int i = ((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00);
       return (GraphicScreen.graM4[0x300000 + i] << 12 |
               GraphicScreen.graM4[0x200000 + i] <<  8 |
               GraphicScreen.graM4[0x100000 + i] <<  4 |
               GraphicScreen.graM4[           i]);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram * 2;
       int i0 = ((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00);
       a += 2;
       int i1 = ((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00);
       return (GraphicScreen.graM4[0x300000 + i0] << 28 |
               GraphicScreen.graM4[0x200000 + i0] << 24 |
               GraphicScreen.graM4[0x100000 + i0] << 20 |
               GraphicScreen.graM4[           i0] << 16 |
               GraphicScreen.graM4[0x300000 + i1] << 12 |
               GraphicScreen.graM4[0x200000 + i1] <<  8 |
               GraphicScreen.graM4[0x100000 + i1] <<  4 |
               GraphicScreen.graM4[           i1]);
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       CRTC.crtRasterStamp[(a >> 11) - CRTC.crtR13GrYCurr[0] & 1023] = 0;
       int i = ((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00);
       if ((a & 1) == 0) {
         GraphicScreen.graM4[0x300000 + i] = (byte) ((d >> 4) & 15);
         GraphicScreen.graM4[0x200000 + i] = (byte) ( d       & 15);
       } else {
         GraphicScreen.graM4[0x100000 + i] = (byte) ((d >> 4) & 15);
         GraphicScreen.graM4[           i] = (byte) ( d       & 15);
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram;
       CRTC.crtRasterStamp[(a >> 11) - CRTC.crtR13GrYCurr[0] & 1023] = 0;
       int i = ((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00);
       GraphicScreen.graM4[0x300000 + i] = (byte) ((char) d >> 12);
       GraphicScreen.graM4[0x200000 + i] = (byte) ((d >>  8) & 15);
       GraphicScreen.graM4[0x100000 + i] = (byte) ((d >>  4) & 15);
       GraphicScreen.graM4[           i] = (byte) ( d        & 15);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.gvram * 2;
       CRTC.crtRasterStamp[(a >> 11) - CRTC.crtR13GrYCurr[0] & 1023] = 0;
       int i0 = ((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00);
       a += 2;
       CRTC.crtRasterStamp[(a >> 11) - CRTC.crtR13GrYCurr[0] & 1023] = 0;
       int i1 = ((a >> 1) & 0x801ff) | ((a << 8) & 0x40000) | ((a >> 2) & 0x3fe00);
       GraphicScreen.graM4[0x300000 + i0] = (byte) (d >>> 28);
       GraphicScreen.graM4[0x200000 + i0] = (byte) ((d >> 24) & 15);
       GraphicScreen.graM4[0x100000 + i0] = (byte) ((d >> 20) & 15);
       GraphicScreen.graM4[           i0] = (byte) ((d >> 16) & 15);
       GraphicScreen.graM4[0x300000 + i1] = (byte) ((char) d >> 12);
       GraphicScreen.graM4[0x200000 + i1] = (byte) ((d >>  8) & 15);
       GraphicScreen.graM4[0x100000 + i1] = (byte) ((d >>  4) & 15);
       GraphicScreen.graM4[           i1] = (byte) ( d        & 15);
     }
  },  //MMD_GJ0

  //--------------------------------------------------------------------------------
  //MMD_TXT テキスト画面
  MMD_TXT {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "テキスト画面" : "Text Screen";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a];
    }
    @Override protected int mmdPbz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a] & 0xff;
    }
    @Override protected int mmdPws (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (MainMemory.MMR_USE_BYTE_BUFFER) {
        return MainMemory.mmrBuffer.getShort (a);
      } else {
        return MainMemory.mmrM8[a] << 8 | (MainMemory.mmrM8[a + 1] & 0xff);
      }
    }
    @Override protected int mmdPwz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (MainMemory.MMR_USE_BYTE_BUFFER) {
        return MainMemory.mmrBuffer.getChar (a);
      } else {
        return (char) (MainMemory.mmrM8[a] << 8 | (MainMemory.mmrM8[a + 1] & 0xff));
      }
    }
    @Override protected int mmdPls (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (MainMemory.MMR_USE_BYTE_BUFFER) {
        return MainMemory.mmrBuffer.getInt (a);
      } else {
        return MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 0xff) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | (MainMemory.mmrM8[a + 3] & 0xff));
      }
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.tvram;
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a];
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.tvram;
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a] & 0xff;
     }
    @Override protected int mmdRws (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.tvram;
       a &= XEiJ.BUS_MOTHER_MASK;
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         return MainMemory.mmrBuffer.getShort (a);
       } else {
         return MainMemory.mmrM8[a] << 8 | (MainMemory.mmrM8[a + 1] & 0xff);
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.tvram;
       a &= XEiJ.BUS_MOTHER_MASK;
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         return MainMemory.mmrBuffer.getChar (a);
       } else {
         return (char) (MainMemory.mmrM8[a] << 8 | (MainMemory.mmrM8[a + 1] & 0xff));
       }
     }
    @Override protected int mmdRls (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.tvram * 2;
       a &= XEiJ.BUS_MOTHER_MASK;
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         return MainMemory.mmrBuffer.getInt (a);
       } else {
         return MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 0xff) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | (MainMemory.mmrM8[a + 3] & 0xff));
       }
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.tvram;
       a &= XEiJ.BUS_MOTHER_MASK;
       int x;  //マスク
       if (CRTC.crtSimAccess) {  //同時アクセスあり
         a &= 0x00e1ffff;
         if (CRTC.crtBitMask) {  //同時アクセスあり,ビットマスクあり
           d &= ~(x = CRTC.crtR23Mask >> ((~a & 1) << 3));
           if (CRTC.crtSimPlane0) {
             MainMemory.mmrM8[a             ] = (byte) (MainMemory.mmrM8[a             ] & x | d);
           }
           if (CRTC.crtSimPlane1) {
             MainMemory.mmrM8[a + 0x00020000] = (byte) (MainMemory.mmrM8[a + 0x00020000] & x | d);
           }
           if (CRTC.crtSimPlane2) {
             MainMemory.mmrM8[a + 0x00040000] = (byte) (MainMemory.mmrM8[a + 0x00040000] & x | d);
           }
           if (CRTC.crtSimPlane3) {
             MainMemory.mmrM8[a + 0x00060000] = (byte) (MainMemory.mmrM8[a + 0x00060000] & x | d);
           }
         } else {  //同時アクセスあり,ビットマスクなし
           if (CRTC.crtSimPlane0) {
             MainMemory.mmrM8[a             ] = (byte) d;
           }
           if (CRTC.crtSimPlane1) {
             MainMemory.mmrM8[a + 0x00020000] = (byte) d;
           }
           if (CRTC.crtSimPlane2) {
             MainMemory.mmrM8[a + 0x00040000] = (byte) d;
           }
           if (CRTC.crtSimPlane3) {
             MainMemory.mmrM8[a + 0x00060000] = (byte) d;
           }
         }
       } else if (CRTC.crtBitMask) {  //同時アクセスなし,ビットマスクあり
         x = CRTC.crtR23Mask >> ((~a & 1) << 3);
         MainMemory.mmrM8[a] = (byte) (MainMemory.mmrM8[a] & x | d & ~x);
       } else {  //同時アクセスなし,ビットマスクなし
         MainMemory.mmrM8[a] = (byte) d;
       }
       //同時アクセスやビットマスクで1ピクセルも書き換えなくても更新することになる
       a = ((a & 0x0001ffff) >> 7) - CRTC.crtR11TxYCurr & 1020;
       CRTC.crtRasterStamp[a    ] = 0;
       CRTC.crtRasterStamp[a + 1] = 0;
       CRTC.crtRasterStamp[a + 2] = 0;
       CRTC.crtRasterStamp[a + 3] = 0;
     }  //mmdWb
    @Override protected void mmdWw (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.tvram;
       a &= XEiJ.BUS_MOTHER_MASK;
       int e;  //上位バイトのデータ
       int x;  //下位バイトのマスク
       int y;  //上位バイトのマスク
       if (CRTC.crtSimAccess) {  //同時アクセスあり
         a &= 0x00e1ffff;
         if (CRTC.crtBitMask) {  //同時アクセスあり,ビットマスクあり
           e = d >> 8 & ~(y = (x = CRTC.crtR23Mask) >> 8);
           d &= ~x;
           if (CRTC.crtSimPlane0) {
             MainMemory.mmrM8[a             ] = (byte) (MainMemory.mmrM8[a             ] & y | e);
             MainMemory.mmrM8[a + 0x00000001] = (byte) (MainMemory.mmrM8[a + 0x00000001] & x | d);
           }
           if (CRTC.crtSimPlane1) {
             MainMemory.mmrM8[a + 0x00020000] = (byte) (MainMemory.mmrM8[a + 0x00020000] & y | e);
             MainMemory.mmrM8[a + 0x00020001] = (byte) (MainMemory.mmrM8[a + 0x00020001] & x | d);
           }
           if (CRTC.crtSimPlane2) {
             MainMemory.mmrM8[a + 0x00040000] = (byte) (MainMemory.mmrM8[a + 0x00040000] & y | e);
             MainMemory.mmrM8[a + 0x00040001] = (byte) (MainMemory.mmrM8[a + 0x00040001] & x | d);
           }
           if (CRTC.crtSimPlane3) {
             MainMemory.mmrM8[a + 0x00060000] = (byte) (MainMemory.mmrM8[a + 0x00060000] & y | e);
             MainMemory.mmrM8[a + 0x00060001] = (byte) (MainMemory.mmrM8[a + 0x00060001] & x | d);
           }
         } else {  //同時アクセスあり,ビットマスクなし
           e = d >> 8;
           if (CRTC.crtSimPlane0) {
             MainMemory.mmrM8[a             ] = (byte) e;
             MainMemory.mmrM8[a + 0x00000001] = (byte) d;
           }
           if (CRTC.crtSimPlane1) {
             MainMemory.mmrM8[a + 0x00020000] = (byte) e;
             MainMemory.mmrM8[a + 0x00020001] = (byte) d;
           }
           if (CRTC.crtSimPlane2) {
             MainMemory.mmrM8[a + 0x00040000] = (byte) e;
             MainMemory.mmrM8[a + 0x00040001] = (byte) d;
           }
           if (CRTC.crtSimPlane3) {
             MainMemory.mmrM8[a + 0x00060000] = (byte) e;
             MainMemory.mmrM8[a + 0x00060001] = (byte) d;
           }
         }
       } else if (CRTC.crtBitMask) {  //同時アクセスなし,ビットマスクあり
         y = (x = CRTC.crtR23Mask) >> 8;
         MainMemory.mmrM8[a    ] = (byte) (MainMemory.mmrM8[a    ] & y | (d >> 8) & ~y);
         MainMemory.mmrM8[a + 1] = (byte) (MainMemory.mmrM8[a + 1] & x |  d       & ~x);
       } else {  //同時アクセスなし,ビットマスクなし
         if (MainMemory.MMR_USE_BYTE_BUFFER) {
           MainMemory.mmrBuffer.putShort (a, (short) d);
         } else {
           MainMemory.mmrM8[a    ] = (byte) (d >> 8);
           MainMemory.mmrM8[a + 1] = (byte)  d;
         }
       }
       //同時アクセスやビットマスクで1ピクセルも書き換えなくても更新することになる
       a = ((a & 0x0001ffff) >> 7) - CRTC.crtR11TxYCurr & 1020;
       CRTC.crtRasterStamp[a    ] = 0;
       CRTC.crtRasterStamp[a + 1] = 0;
       CRTC.crtRasterStamp[a + 2] = 0;
       CRTC.crtRasterStamp[a + 3] = 0;
     }  //mmdWw
    @Override protected void mmdWl (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.tvram * 2;
       a &= XEiJ.BUS_MOTHER_MASK;
       int e;  //下位ワードの上位バイトのデータ
       int f;  //上位ワードの下位バイトのデータ
       int g;  //上位ワードの上位バイトのデータ
       int x;  //下位バイトのマスク
       int y;  //上位バイトのマスク
       if (CRTC.crtSimAccess) {  //同時アクセスあり
         a &= 0x00e1ffff;
         if (CRTC.crtBitMask) {  //同時アクセスあり,ビットマスクあり
           g = d >> 24 & ~(y = (x = CRTC.crtR23Mask) >> 8);
           f = d >> 16 & ~x;
           e = d >>  8 & ~y;
           d &= ~x;
           if (CRTC.crtSimPlane0) {
             MainMemory.mmrM8[a             ] = (byte) (MainMemory.mmrM8[a             ] & y | g);
             MainMemory.mmrM8[a + 0x00000001] = (byte) (MainMemory.mmrM8[a + 0x00000001] & x | f);
             MainMemory.mmrM8[a + 0x00000002] = (byte) (MainMemory.mmrM8[a + 0x00000002] & y | e);
             MainMemory.mmrM8[a + 0x00000003] = (byte) (MainMemory.mmrM8[a + 0x00000003] & x | d);
           }
           if (CRTC.crtSimPlane1) {
             MainMemory.mmrM8[a + 0x00020000] = (byte) (MainMemory.mmrM8[a + 0x00020000] & y | g);
             MainMemory.mmrM8[a + 0x00020001] = (byte) (MainMemory.mmrM8[a + 0x00020001] & x | f);
             MainMemory.mmrM8[a + 0x00020002] = (byte) (MainMemory.mmrM8[a + 0x00020002] & y | e);
             MainMemory.mmrM8[a + 0x00020003] = (byte) (MainMemory.mmrM8[a + 0x00020003] & x | d);
           }
           if (CRTC.crtSimPlane2) {
             MainMemory.mmrM8[a + 0x00040000] = (byte) (MainMemory.mmrM8[a + 0x00040000] & y | g);
             MainMemory.mmrM8[a + 0x00040001] = (byte) (MainMemory.mmrM8[a + 0x00040001] & x | f);
             MainMemory.mmrM8[a + 0x00040002] = (byte) (MainMemory.mmrM8[a + 0x00040002] & y | e);
             MainMemory.mmrM8[a + 0x00040003] = (byte) (MainMemory.mmrM8[a + 0x00040003] & x | d);
           }
           if (CRTC.crtSimPlane3) {
             MainMemory.mmrM8[a + 0x00060000] = (byte) (MainMemory.mmrM8[a + 0x00060000] & y | g);
             MainMemory.mmrM8[a + 0x00060001] = (byte) (MainMemory.mmrM8[a + 0x00060001] & x | f);
             MainMemory.mmrM8[a + 0x00060002] = (byte) (MainMemory.mmrM8[a + 0x00060002] & y | e);
             MainMemory.mmrM8[a + 0x00060003] = (byte) (MainMemory.mmrM8[a + 0x00060003] & x | d);
           }
         } else {  //同時アクセスあり,ビットマスクなし
           g = d >> 24;
           f = d >> 16;
           e = d >>  8;
           if (CRTC.crtSimPlane0) {
             MainMemory.mmrM8[a             ] = (byte) g;
             MainMemory.mmrM8[a + 0x00000001] = (byte) f;
             MainMemory.mmrM8[a + 0x00000002] = (byte) e;
             MainMemory.mmrM8[a + 0x00000003] = (byte) d;
           }
           if (CRTC.crtSimPlane1) {
             MainMemory.mmrM8[a + 0x00020000] = (byte) g;
             MainMemory.mmrM8[a + 0x00020001] = (byte) f;
             MainMemory.mmrM8[a + 0x00020002] = (byte) e;
             MainMemory.mmrM8[a + 0x00020003] = (byte) d;
           }
           if (CRTC.crtSimPlane2) {
             MainMemory.mmrM8[a + 0x00040000] = (byte) g;
             MainMemory.mmrM8[a + 0x00040001] = (byte) f;
             MainMemory.mmrM8[a + 0x00040002] = (byte) e;
             MainMemory.mmrM8[a + 0x00040003] = (byte) d;
           }
           if (CRTC.crtSimPlane3) {
             MainMemory.mmrM8[a + 0x00060000] = (byte) g;
             MainMemory.mmrM8[a + 0x00060001] = (byte) f;
             MainMemory.mmrM8[a + 0x00060002] = (byte) e;
             MainMemory.mmrM8[a + 0x00060003] = (byte) d;
           }
         }
       } else if (CRTC.crtBitMask) {  //同時アクセスなし,ビットマスクあり
         y = (x = CRTC.crtR23Mask) >> 8;
         MainMemory.mmrM8[a    ] = (byte) (MainMemory.mmrM8[a    ] & y | (d >> 24) & ~y);
         MainMemory.mmrM8[a + 1] = (byte) (MainMemory.mmrM8[a + 1] & x | (d >> 16) & ~x);
         MainMemory.mmrM8[a + 2] = (byte) (MainMemory.mmrM8[a + 1] & y | (d >>  8) & ~y);
         MainMemory.mmrM8[a + 3] = (byte) (MainMemory.mmrM8[a + 1] & x |  d        & ~x);
       } else {  //同時アクセスなし,ビットマスクなし
         if (MainMemory.MMR_USE_BYTE_BUFFER) {
           MainMemory.mmrBuffer.putInt (a, d);
         } else {
           MainMemory.mmrM8[a    ] = (byte) (d >> 24);
           MainMemory.mmrM8[a + 1] = (byte) (d >> 16);
           MainMemory.mmrM8[a + 2] = (byte) (d >>  8);
           MainMemory.mmrM8[a + 3] = (byte)  d;
         }
       }
       //同時アクセスやビットマスクで1ピクセルも書き換えなくても更新することになる
       int b = ((a     & 0x0001ffff) >> 7) - CRTC.crtR11TxYCurr & 1020;
       CRTC.crtRasterStamp[b    ] = 0;
       CRTC.crtRasterStamp[b + 1] = 0;
       CRTC.crtRasterStamp[b + 2] = 0;
       CRTC.crtRasterStamp[b + 3] = 0;
       a = ((a + 2 & 0x0001ffff) >> 7) - CRTC.crtR11TxYCurr & 1020;
       if (a != b) {
         CRTC.crtRasterStamp[a    ] = 0;
         CRTC.crtRasterStamp[a + 1] = 0;
         CRTC.crtRasterStamp[a + 2] = 0;
         CRTC.crtRasterStamp[a + 3] = 0;
       }
     }  //mmdWl
  },  //MMD_TXT

  //--------------------------------------------------------------------------------
  //MMD_CRT CRTコントローラ
  //
  //  $00E80000～$00E8002F  ワードレジスタ
  //  $00E80030～$00E8003F  $0000
  //  $00E80040～$00E803FF  $00E80000～$00E8003Fの繰り返し
  //  $00E80400～$00E8047F  バスエラー
  //  $00E80480～$00E80481  ワードレジスタ
  //  $00E80482～$00E804FF  $00E80480～$00E80481の繰り返し
  //  $00E80500～$00E807FF  $00E80400～$00E804FFの繰り返し
  //  $00E80800～$00E81FFF  $00E80000～$00E807FFの繰り返し
  //
  MMD_CRT {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "CRT コントローラ" : "CRT Controller";
    }
    //リード
    @Override protected int mmdRbz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.crtc;
       int aa = a & 0x07ff;  //$00E80800～$00E81FFF→$00E80000～$00E807FF
       if (aa < 0x0400) {  //$00E80000～$00E803FF
         aa &= 0x003f;
         switch (aa) {
           //case 0x0001:  //R00の下位
           //  return CRTC.crtR00HFrontEndPort;
           //case 0x0003:  //R01の下位
           //  return CRTC.crtR01HSyncEndPort;
           //case 0x0005:  //R02の下位
           //  return CRTC.crtR02HBackEndPort;
           //case 0x0007:  //R03の下位
           //  return CRTC.crtR03HDispEndPort;
           //case 0x0008:  //R04の上位
           //  return CRTC.crtR04VFrontEndPort >> 8;
           //case 0x0009:  //R04の下位
           //  return CRTC.crtR04VFrontEndPort & 0xff;
           //case 0x000a:  //R05の上位
           //  return CRTC.crtR05VSyncEndPort >> 8;
           //case 0x000b:  //R05の下位
           //  return CRTC.crtR05VSyncEndPort & 0xff;
           //case 0x000c:  //R06の上位
           //  return CRTC.crtR06VBackEndPort >> 8;
           //case 0x000d:  //R06の下位
           //  return CRTC.crtR06VBackEndPort & 0xff;
           //case 0x000e:  //R07の上位
           //  return CRTC.crtR07VDispEndPort >> 8;
           //case 0x000f:  //R07の下位
           //  return CRTC.crtR07VDispEndPort & 0xff;
           //case 0x0011:  //R08の下位
           //  return CRTC.crtR08Adjust;
           //case 0x0012:  //R09の上位
           //  return CRTC.crtR09IRQRasterPort >> 8;
           //case 0x0013:  //R09の下位
           //  return CRTC.crtR09IRQRasterPort & 0xff;
           //case 0x0014:  //R10の上位
           //  return CRTC.crtR10TxXPort >> 8;
           //case 0x0015:  //R10の下位
           //  return CRTC.crtR10TxXPort & 0xff;
           //case 0x0016:  //R11の上位
           //  return CRTC.crtR11TxYCurr >> 8;
           //case 0x0017:  //R11の下位
           //  return CRTC.crtR11TxYCurr & 0xff;
           //case 0x0018:  //R12の上位
           //case 0x001c:  //R14の上位
           //case 0x0020:  //R16の上位
           //case 0x0024:  //R18の上位
           //  return CRTC.crtR12GrXPort[(aa - 0x0018) >> 2] >> 8;
           //case 0x0019:  //R12の下位
           //case 0x001d:  //R14の下位
           //case 0x0021:  //R16の下位
           //case 0x0025:  //R18の下位
           //  return CRTC.crtR12GrXPort[(aa - 0x0018) >> 2] & 0xff;
           //case 0x001a:  //R13の上位
           //case 0x001e:  //R15の上位
           //case 0x0022:  //R17の上位
           //case 0x0026:  //R19の上位
           //  return CRTC.crtR13GrYPort[(aa - 0x0018) >> 2] >> 8;
           //case 0x001b:  //R13の下位
           //case 0x001f:  //R15の下位
           //case 0x0023:  //R17の下位
           //case 0x0027:  //R19の下位
           //  return CRTC.crtR13GrYPort[(aa - 0x0018) >> 2] & 0xff;
         case 0x0028:  //R20の上位
           return (CRTC.crtTextStorage    << 4 |
                   CRTC.crtGraphicStorage << 3 |
                   CRTC.crtMemoryModePort);
         case 0x0029:  //R20の下位
           return (CRTC.crtHighResoPort << 4 |
                   CRTC.crtVResoPort    << 2 |
                   CRTC.crtHResoPort);
         case 0x002a:  //R21の上位
           return ((CRTC.crtBitMask   ? 0b00000010 : 0) |
                   (CRTC.crtSimAccess ? 0b00000001 : 0));
         case 0x002b:  //R21の下位
           return ((CRTC.crtSimPlane3 ? 0b10000000 : 0) |
                   (CRTC.crtSimPlane2 ? 0b01000000 : 0) |
                   (CRTC.crtSimPlane1 ? 0b00100000 : 0) |
                   (CRTC.crtSimPlane0 ? 0b00010000 : 0) |
                   (CRTC.crtCCPlane3  ? 0b00001000 : 0) |
                   (CRTC.crtCCPlane2  ? 0b00000100 : 0) |
                   (CRTC.crtCCPlane1  ? 0b00000010 : 0) |
                   (CRTC.crtCCPlane0  ? 0b00000001 : 0));
           //case 0x002c:  //R22の上位
           //  return CRTC.crtR22SrcBlock;
           //case 0x002d:  //R22の下位
           //  return CRTC.crtR22DstBlock;
           //case 0x002e:  //R23の上位
           //  return CRTC.crtR23Mask >> 8;
           //case 0x002f:  //R23の下位
           //  return CRTC.crtR23Mask & 0xff;
           //case 0x0000:  //R00の上位
           //case 0x0002:  //R01の上位
           //case 0x0004:  //R02の上位
           //case 0x0006:  //R03の上位
           //case 0x0010:  //R08の上位
           //case 0x0030:  //R24の上位
           //case 0x0031:  //R24の下位
           //case 0x0032:  //R25の上位
           //case 0x0033:  //R25の下位
           //case 0x0034:  //R26の上位
           //case 0x0035:  //R26の下位
           //case 0x0036:  //R27の上位
           //case 0x0037:  //R27の下位
           //case 0x0038:  //R28の上位
           //case 0x0039:  //R28の下位
           //case 0x003a:  //R29の上位
           //case 0x003b:  //R29の下位
           //case 0x003c:  //R30の上位
           //case 0x003d:  //R30の下位
           //case 0x003e:  //R31の上位
           //case 0x003f:  //R31の下位
         default:
           return 0x00;
           //return VideoController.vcnMode.ordinal () >> 8;
           //return VideoController.vcnMode.ordinal () & 0xff;
         }
       } else {  //$00E80400～$00E807FF
         aa &= 0xff;  //$00E80500～$00E807FF→$00E80400～$00E804FF
         if (aa < 0x80) {  //$00E80400～$00E8047F
           return super.mmdRbz (a);  //バスエラー
         } else {  //$00E80480～$00E804FF
           aa &= 0x01;  //$00E80482～$00E804FF→$00E80480～$00E80481
           if (aa == 0) {  //動作ポートの上位
             return 0;
           } else {  //動作ポートの下位
             return ((CRTC.crtRasterCopyOn ? 8 : 0) |  //ラスタコピー
                     (CRTC.crtClearFrames != 0 ? 2 : 0));  //高速クリア
           }
         }
       }
     }  //mmdRbz
    @Override protected int mmdRwz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.crtc;
       int aa = a & 0x07ff;  //$00E80800～$00E81FFF→$00E80000～$00E807FF
       if (aa < 0x0400) {  //$00E80000～$00E803FF
         aa &= 0x003f;
         switch (aa) {
           //case 0x0000:  //R00
           //  return CRTC.crtR00HFrontEndPort;
           //case 0x0002:  //R01
           //  return CRTC.crtR01HSyncEndPort;
           //case 0x0004:  //R02
           //  return CRTC.crtR02HBackEndPort;
           //case 0x0006:  //R03
           //  return CRTC.crtR03HDispEndPort;
           //case 0x0008:  //R04
           //  return CRTC.crtR04VFrontEndPort;
           //case 0x000a:  //R05
           //  return CRTC.crtR05VSyncEndPort;
           //case 0x000c:  //R06
           //  return CRTC.crtR06VBackEndPort;
           //case 0x000e:  //R07
           //  return CRTC.crtR07VDispEndPort;
           //case 0x0010:  //R08
           //  return CRTC.crtR08Adjust;
           //case 0x0012:  //R09
           //  return CRTC.crtR09IRQRasterPort;
           //case 0x0014:  //R10
           //  return CRTC.crtR10TxXPort;
           //case 0x0016:  //R11
           //  return CRTC.crtR11TxYPort;
           //case 0x0018:  //R12
           //case 0x001c:  //R14
           //case 0x0020:  //R16
           //case 0x0024:  //R18
           //  return CRTC.crtR12GrXPort[(aa - 0x0018) >> 2];
           //case 0x001a:  //R13
           //case 0x001e:  //R15
           //case 0x0022:  //R17
           //case 0x0026:  //R19
           //  return CRTC.crtR13GrYPort[(aa - 0x0018) >> 2];
         case 0x0028:  //R20
           return (CRTC.crtTextStorage    << 12 |
                   CRTC.crtGraphicStorage << 11 |
                   CRTC.crtMemoryModePort <<  8 |
                   CRTC.crtHighResoPort   <<  4 |
                   CRTC.crtVResoPort      <<  2 |
                   CRTC.crtHResoPort);
         case 0x002a:  //R21
           return ((CRTC.crtBitMask   ? 0b00000010_00000000 : 0) |
                   (CRTC.crtSimAccess ? 0b00000001_00000000 : 0) |
                   (CRTC.crtSimPlane3 ? 0b00000000_10000000 : 0) |
                   (CRTC.crtSimPlane2 ? 0b00000000_01000000 : 0) |
                   (CRTC.crtSimPlane1 ? 0b00000000_00100000 : 0) |
                   (CRTC.crtSimPlane0 ? 0b00000000_00010000 : 0) |
                   (CRTC.crtCCPlane3  ? 0b00000000_00001000 : 0) |
                   (CRTC.crtCCPlane2  ? 0b00000000_00000100 : 0) |
                   (CRTC.crtCCPlane1  ? 0b00000000_00000010 : 0) |
                   (CRTC.crtCCPlane0  ? 0b00000000_00000001 : 0));
           //case 0x002c:  //R22
           //  return CRTC.crtR22SrcBlock << 8 | CRTC.crtR22DstBlock;
           //case 0x002e:  //R23
           //  return CRTC.crtR23Mask;
           //case 0x0030:  //R24
           //case 0x0032:  //R25
           //case 0x0034:  //R26
           //case 0x0036:  //R27
           //case 0x0038:  //R28
           //case 0x003a:  //R29
           //case 0x003c:  //R30
           //case 0x003e:  //R31
         default:
           return 0x0000;
         }
       } else {  //$00E80400～$00E807FF
         aa &= 0xff;  //$00E80500～$00E807FF→$00E80400～$00E804FF
         if (aa < 0x80) {  //$00E80400～$00E8047F
           return super.mmdRbz (a);  //バスエラー
         } else {  //$00E80480～$00E804FF  動作ポート
           return ((CRTC.crtRasterCopyOn ? 8 : 0) |  //ラスタコピー
                   (CRTC.crtClearFrames != 0 ? 2 : 0));  //高速クリア
         }
       }
     }  //mmdRwz
    @Override protected int mmdRls (int a) throws M68kException {
       return mmdRwz (a) << 16 | mmdRwz (a + 2);
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.crtc;
       int aa = a & 0x07ff;  //$00E80800～$00E81FFF→$00E80000～$00E807FF
       if (aa < 0x0400) {  //$00E80000～$00E803FF
         aa &= 0x003f;
         switch (aa) {
         case 0x0001:  //R00の下位
           CRTC.crtR00HFrontEndPort = (d & 0xff) | (CRTC.crtR00Bit0Zero ? 0x00 : 0x01);
           {
             int curr = CRTC.crtR00HFrontEndMask == 0 ? CRTC.crtR00HFrontEndPort : CRTC.crtR00HFrontEndTest;
             if (CRTC.crtR00HFrontEndCurr != curr) {
               CRTC.crtR00HFrontEndCurr = curr;
               CRTC.crtRestart ();
             }
           }
         return;
         case 0x0003:  //R01の下位
           CRTC.crtR01HSyncEndPort = d & 0xff;
           {
             int curr = CRTC.crtR01HSyncEndMask == 0 ? CRTC.crtR01HSyncEndPort : CRTC.crtR01HSyncEndTest;
             if (CRTC.crtR01HSyncEndCurr != curr) {
               CRTC.crtR01HSyncEndCurr = curr;
               CRTC.crtRestart ();
             }
           }
           return;
         case 0x0005:  //R02の下位
           CRTC.crtR02HBackEndPort = d & 0xff;
           {
             int curr = CRTC.crtR02HBackEndMask == 0 ? CRTC.crtR02HBackEndPort : CRTC.crtR02HBackEndTest;
             if (CRTC.crtR02HBackEndCurr != curr) {
               CRTC.crtR02HBackEndCurr = curr;
               CRTC.crtRestart ();
             }
           }
           return;
         case 0x0007:  //R03の下位
           CRTC.crtR03HDispEndPort = d & 0xff;
           {
             int curr = CRTC.crtR03HDispEndMask == 0 ? CRTC.crtR03HDispEndPort : CRTC.crtR03HDispEndTest;
             if (CRTC.crtR03HDispEndCurr != curr) {
               CRTC.crtR03HDispEndCurr = curr;
               CRTC.crtRestart ();
             }
           }
           return;
         case 0x0008:  //R04の上位
           CRTC.crtR04VFrontEndPort = (d & (CRTC.crtVerticalMask >> 8)) << 8 | (CRTC.crtR04VFrontEndPort & 0xff);
           {
             int curr = CRTC.crtR04VFrontEndMask == 0 ? CRTC.crtR04VFrontEndPort : CRTC.crtR04VFrontEndTest;
             if (CRTC.crtR04VFrontEndCurr != curr) {
               CRTC.crtR04VFrontEndCurr = curr;
               if (CRTC.CRT_RASTER_HASH_ON) {
                 CRTC.crtUpdateRasterHash ();
               }
               CRTC.crtRestart ();
             }
           }
           return;
         case 0x0009:  //R04の下位
           CRTC.crtR04VFrontEndPort = (CRTC.crtR04VFrontEndPort & ~0xff) | (d & 0xff);
           {
             int curr = CRTC.crtR04VFrontEndMask == 0 ? CRTC.crtR04VFrontEndPort : CRTC.crtR04VFrontEndTest;
             if (CRTC.crtR04VFrontEndCurr != curr) {
               CRTC.crtR04VFrontEndCurr = curr;
               if (CRTC.CRT_RASTER_HASH_ON) {
                 CRTC.crtUpdateRasterHash ();
               }
               CRTC.crtRestart ();
             }
           }
           return;
         case 0x000a:  //R05の上位
           CRTC.crtR05VSyncEndPort = (d & (CRTC.crtVerticalMask >> 8)) << 8 | (CRTC.crtR05VSyncEndPort & 0xff);
           {
             int curr = CRTC.crtR05VSyncEndMask == 0 ? CRTC.crtR05VSyncEndPort : CRTC.crtR05VSyncEndTest;
             if (CRTC.crtR05VSyncEndCurr != curr) {
               CRTC.crtR05VSyncEndCurr = curr;
               CRTC.crtRestart ();
             }
           }
           return;
         case 0x000b:  //R05の下位
           CRTC.crtR05VSyncEndPort = (CRTC.crtR05VSyncEndPort & ~0xff) | (d & 0xff);
           {
             int curr = CRTC.crtR05VSyncEndMask == 0 ? CRTC.crtR05VSyncEndPort : CRTC.crtR05VSyncEndTest;
             if (CRTC.crtR05VSyncEndCurr != curr) {
               CRTC.crtR05VSyncEndCurr = curr;
               CRTC.crtRestart ();
             }
           }
           return;
         case 0x000c:  //R06の上位
           CRTC.crtR06VBackEndPort = (d & (CRTC.crtVerticalMask >> 8)) << 8 | (CRTC.crtR06VBackEndPort & 0xff);
           {
             int curr = CRTC.crtR06VBackEndMask == 0 ? CRTC.crtR06VBackEndPort : CRTC.crtR06VBackEndTest;
             if (CRTC.crtR06VBackEndCurr != curr) {
               CRTC.crtR06VBackEndCurr = curr;
               CRTC.crtVDispStart = curr + 1;
               if (CRTC.CRT_RASTER_HASH_ON) {
                 CRTC.crtUpdateRasterHash ();
               }
               CRTC.crtRestart ();
             }
           }
           return;
         case 0x000d:  //R06の下位
           CRTC.crtR06VBackEndPort = (CRTC.crtR06VBackEndPort & ~0xff) | (d & 0xff);
           {
             int curr = CRTC.crtR06VBackEndMask == 0 ? CRTC.crtR06VBackEndPort : CRTC.crtR06VBackEndTest;
             if (CRTC.crtR06VBackEndCurr != curr) {
               CRTC.crtR06VBackEndCurr = curr;
               CRTC.crtVDispStart = curr + 1;
               if (CRTC.CRT_RASTER_HASH_ON) {
                 CRTC.crtUpdateRasterHash ();
               }
               CRTC.crtRestart ();
             }
           }
           return;
         case 0x000e:  //R07の上位
           CRTC.crtR07VDispEndPort = (d & (CRTC.crtVerticalMask >> 8)) << 8 | (CRTC.crtR07VDispEndPort & 0xff);
           {
             int curr = CRTC.crtR07VDispEndMask == 0 ? CRTC.crtR07VDispEndPort : CRTC.crtR07VDispEndTest;
             if (CRTC.crtR07VDispEndCurr != curr) {
               CRTC.crtR07VDispEndCurr = curr;
               CRTC.crtVIdleStart = curr + 1;
               if (CRTC.CRT_RASTER_HASH_ON) {
                 CRTC.crtUpdateRasterHash ();
               }
               CRTC.crtRestart ();
             }
           }
           return;
         case 0x000f:  //R07の下位
           CRTC.crtR07VDispEndPort = (CRTC.crtR07VDispEndPort & ~0xff) | (d & 0xff);
           {
             int curr = CRTC.crtR07VDispEndMask == 0 ? CRTC.crtR07VDispEndPort : CRTC.crtR07VDispEndTest;
             if (CRTC.crtR07VDispEndCurr != curr) {
               CRTC.crtR07VDispEndCurr = curr;
               CRTC.crtVIdleStart = curr + 1;
               if (CRTC.CRT_RASTER_HASH_ON) {
                 CRTC.crtUpdateRasterHash ();
               }
               CRTC.crtRestart ();
             }
           }
           return;
         case 0x0011:  //R08の下位
           d &= 0xff;
           if (CRTC.crtR08Adjust != d) {
             CRTC.crtR08Adjust = d;
             CRTC.crtRestart ();
           }
           return;
         case 0x0012:  //R09の上位
           CRTC.crtR09IRQRasterPort = (d & (CRTC.crtVerticalMask >> 8)) << 8 | (CRTC.crtR09IRQRasterPort & 0xff);
           {
             int curr = CRTC.crtR09IRQRasterMask == 0 ? CRTC.crtR09IRQRasterPort : CRTC.crtR09IRQRasterTest;
             if (CRTC.crtR09IRQRasterCurr != curr) {
               CRTC.crtR09IRQRasterCurr = curr;
               if (CRTC.CRT_RASTER_HASH_ON) {
                 CRTC.crtUpdateRasterHash ();
               }
               if (RasterBreakPoint.RBP_ON) {
                 RasterBreakPoint.rbpCheckIRQ ();
               }
             }
           }
           return;
         case 0x0013:  //R09の下位
           CRTC.crtR09IRQRasterPort = (CRTC.crtR09IRQRasterPort & ~0xff) | (d & 0xff);
           {
             int curr = CRTC.crtR09IRQRasterMask == 0 ? CRTC.crtR09IRQRasterPort : CRTC.crtR09IRQRasterTest;
             if (CRTC.crtR09IRQRasterCurr != curr) {
               CRTC.crtR09IRQRasterCurr = curr;
               if (CRTC.CRT_RASTER_HASH_ON) {
                 CRTC.crtUpdateRasterHash ();
               }
               if (RasterBreakPoint.RBP_ON) {
                 RasterBreakPoint.rbpCheckIRQ ();
               }
             }
           }
           return;
         case 0x0014:  //R10の上位
           CRTC.crtR10TxXPort = (d & 0x03) << 8 | (CRTC.crtR10TxXPort & 0xff);
           {
             int curr = CRTC.crtR10TxXMask == 0 ? CRTC.crtR10TxXPort : CRTC.crtR10TxXTest;
             if (CRTC.crtR10TxXCurr != curr) {
               CRTC.crtR10TxXCurr = curr;
               CRTC.crtAllStamp += 2;
             }
           }
           return;
         case 0x0015:  //R10の下位
           CRTC.crtR10TxXPort = (CRTC.crtR10TxXPort & ~0xff) | (d & 0xff);
           {
             int curr = CRTC.crtR10TxXMask == 0 ? CRTC.crtR10TxXPort : CRTC.crtR10TxXTest;
             if (CRTC.crtR10TxXCurr != curr) {
               CRTC.crtR10TxXCurr = curr;
               CRTC.crtAllStamp += 2;
             }
           }
           return;
         case 0x0016:  //R11の上位
           CRTC.crtR11TxYPort = (d & 0x03) << 8 | (CRTC.crtR11TxYPort & 0xff);
           {
             int curr = CRTC.crtR11TxYMask == 0 ? CRTC.crtR11TxYPort : CRTC.crtR11TxYTest;
             if (CRTC.crtR11TxYCurr != curr) {
               CRTC.crtR11TxYCurr = curr;
               //CRTC.crtAllStamp += 2;  //ラッチするとき更新する
             }
           }
           return;
         case 0x0017:  //R11の下位
           CRTC.crtR11TxYPort = (CRTC.crtR11TxYPort & ~0xff) | (d & 0xff);
           {
             int curr = CRTC.crtR11TxYMask == 0 ? CRTC.crtR11TxYPort : CRTC.crtR11TxYTest;
             if (CRTC.crtR11TxYCurr != curr) {
               CRTC.crtR11TxYCurr = curr;
               //CRTC.crtAllStamp += 2;  //ラッチするとき更新する
             }
           }
           return;
         case 0x0018:  //R12の上位
         case 0x001c:  //R14の上位
         case 0x0020:  //R16の上位
         case 0x0024:  //R18の上位
           {
             int p = (aa - 0x0018) >> 2;  //0,1,2,3
             CRTC.crtR12GrXPort[p] = (d & (p == 0 ? 0x03 : 0x01)) << 8 | (CRTC.crtR12GrXPort[p] & 0xff);
             int curr = CRTC.crtR12GrXMask[p] == 0 ? CRTC.crtR12GrXPort[p] : CRTC.crtR12GrXTest[p];
             if (CRTC.crtR12GrXCurr[p] != curr) {
               CRTC.crtR12GrXCurr[p] = curr;
               CRTC.crtAllStamp += 2;
             }
           }
           return;
         case 0x0019:  //R12の下位
         case 0x001d:  //R14の下位
         case 0x0021:  //R16の下位
         case 0x0025:  //R18の下位
           {
             int p = (aa - 0x0018) >> 2;  //0,1,2,3
             CRTC.crtR12GrXPort[p] = (CRTC.crtR12GrXPort[p] & ~0xff) | (d & 0xff);
             int curr = CRTC.crtR12GrXMask[p] == 0 ? CRTC.crtR12GrXPort[p] : CRTC.crtR12GrXTest[p];
             if (CRTC.crtR12GrXCurr[p] != curr) {
               CRTC.crtR12GrXCurr[p] = curr;
               CRTC.crtAllStamp += 2;
             }
           }
           return;
         case 0x001a:  //R13の上位
         case 0x001e:  //R15の上位
         case 0x0022:  //R17の上位
         case 0x0026:  //R19の上位
           {
             int p = (aa - 0x0018) >> 2;  //0,1,2,3
             CRTC.crtR13GrYPort[p] = (d & (p == 0 ? 0x03 : 0x01)) << 8 | (CRTC.crtR13GrYPort[p] & 0xff);
             int curr = CRTC.crtR13GrYMask[p] == 0 ? CRTC.crtR13GrYPort[p] : CRTC.crtR13GrYTest[p];
             if (CRTC.crtR13GrYCurr[p] != curr) {
               CRTC.crtR13GrYCurr[p] = curr;
               //CRTC.crtAllStamp += 2;  //ラッチするとき更新する
             }
           }
           return;
         case 0x001b:  //R13の下位
         case 0x001f:  //R15の下位
         case 0x0023:  //R17の下位
         case 0x0027:  //R19の下位
           {
             int p = (aa - 0x0018) >> 2;  //0,1,2,3
             CRTC.crtR13GrYPort[p] = (CRTC.crtR13GrYPort[p] & ~0xff) | (d & 0xff);
             int curr = CRTC.crtR13GrYMask[p] == 0 ? CRTC.crtR13GrYPort[p] : CRTC.crtR13GrYTest[p];
             if (CRTC.crtR13GrYCurr[p] != curr) {
               CRTC.crtR13GrYCurr[p] = curr;
               //CRTC.crtAllStamp += 2;  //ラッチするとき更新する
             }
           }
           return;
         case 0x0028:  //R20の上位
           CRTC.crtSetMemoryMode (d >> 4, d >> 3, d);
           return;
         case 0x0029:  //R20の下位
           CRTC.crtHighResoPort = d >>> 4 & 1;
           CRTC.crtVResoPort    = d >>> 2 & 3;
           CRTC.crtHResoPort    = d       & 3;
           SpriteScreen.sprAccessible = SpriteScreen.spr768x512 || (d & 0b10010) != 0b10010;
           int highResoCurr = CRTC.crtHighResoMask == 0 ? CRTC.crtHighResoPort : CRTC.crtHighResoTest;
           int vResoCurr = CRTC.crtVResoMask == 0 ? CRTC.crtVResoPort : CRTC.crtVResoTest;
           int hResoCurr = CRTC.crtHResoMask == 0 ? CRTC.crtHResoPort : CRTC.crtHResoTest;
           if (CRTC.crtHighResoCurr != highResoCurr ||
               CRTC.crtVResoCurr != vResoCurr ||
               CRTC.crtHResoCurr != hResoCurr) {
             CRTC.crtHighResoCurr = highResoCurr;
             CRTC.crtVResoCurr = vResoCurr;
             CRTC.crtHResoCurr = hResoCurr;
             CRTC.crtRestart ();
           }
           return;
         case 0x002a:  //R21の上位
           CRTC.crtBitMask   = XEiJ.TEST_BIT_1_SHIFT ? d << 31 - 1 < 0 : (d & 2) != 0;
           CRTC.crtSimAccess = XEiJ.TEST_BIT_0_SHIFT ? d << 31 - 0 < 0 : (d & 1) != 0;
           return;
         case 0x002b:  //R21の下位
           CRTC.crtSimPlane3 = (byte) d < 0;  //(d & 128) != 0。d << 24 < 0
           CRTC.crtSimPlane2 = d << 25 < 0;  //(d & 64) != 0
           CRTC.crtSimPlane1 = d << 26 < 0;  //(d & 32) != 0
           CRTC.crtSimPlane0 = d << 27 < 0;  //(d & 16) != 0
           CRTC.crtCCPlane3  = XEiJ.TEST_BIT_3_SHIFT ? d << 31 - 3 < 0 : (d & 8) != 0;
           CRTC.crtCCPlane2  = XEiJ.TEST_BIT_2_SHIFT ? d << 31 - 2 < 0 : (d & 4) != 0;
           CRTC.crtCCPlane1  = XEiJ.TEST_BIT_1_SHIFT ? d << 31 - 1 < 0 : (d & 2) != 0;
           CRTC.crtCCPlane0  = XEiJ.TEST_BIT_0_SHIFT ? d << 31 - 0 < 0 : (d & 1) != 0;
           return;
         case 0x002c:  //R22の上位
           CRTC.crtR22SrcBlock = d & 0xff;
           return;
         case 0x002d:  //R22の下位
           CRTC.crtR22DstBlock = d & 0xff;
           return;
         case 0x002e:  //R23の上位
           CRTC.crtR23Mask = (d & 0xff) << 8 | (CRTC.crtR23Mask & 0xff);
           return;
         case 0x002f:  //R23の下位
           CRTC.crtR23Mask = (CRTC.crtR23Mask & ~0xff) | (d & 0xff);
           return;
           //case 0x0000:  //R00の上位
           //case 0x0002:  //R01の上位
           //case 0x0004:  //R02の上位
           //case 0x0006:  //R03の上位
           //case 0x0010:  //R08の上位
           //case 0x0030:  //R24の上位
           //case 0x0031:  //R24の下位
           //case 0x0032:  //R25の上位
           //case 0x0033:  //R25の下位
           //case 0x0034:  //R26の上位
           //case 0x0035:  //R26の下位
           //case 0x0036:  //R27の上位
           //case 0x0037:  //R27の下位
           //case 0x0038:  //R28の上位
           //case 0x0039:  //R28の下位
           //case 0x003a:  //R29の上位
           //case 0x003b:  //R29の下位
           //case 0x003c:  //R30の上位
           //case 0x003d:  //R30の下位
           //case 0x003e:  //R31の上位
           //case 0x003f:  //R31の下位
         default:
           return;
         }
       } else {  //$00E80400～$00E807FF
         aa &= 0xff;  //$00E80500～$00E807FF→$00E80400～$00E804FF
         if (aa < 0x80) {  //$00E80400～$00E8047F
           super.mmdWb (a, d);  //バスエラー
         } else {  //$00E80480～$00E804FF
           aa &= 0x01;  //$00E80482～$00E804FF→$00E80480～$00E80481
           if (aa == 0) {  //動作ポートの上位
             return;
           } else {  //動作ポートの下位
             boolean rasterCopyOn = (d & 8) != 0;
             if (CRTC.crtRasterCopyOn != rasterCopyOn) {
               CRTC.crtRasterCopyOn = rasterCopyOn;  //ラスタコピー
               if (CRTC.CRT_RASTER_HASH_ON) {
                 CRTC.crtUpdateRasterHash ();
               }
             }
             if (CRTC.crtClearFrames == 0) {
               CRTC.crtClearStandby = (d & 2) != 0;  //高速クリア
             }
             return;
           }
         }
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.crtc;
       int aa = a & 0x07ff;  //$00E80800～$00E81FFF→$00E80000～$00E807FF
       if (aa < 0x0400) {  //$00E80000～$00E803FF
         aa &= 0x003f;
         switch (aa) {
         case 0x0000:  //R00
           CRTC.crtR00HFrontEndPort = (d & 0xff) | (CRTC.crtR00Bit0Zero ? 0x00 : 0x01);
           {
             int curr = CRTC.crtR00HFrontEndMask == 0 ? CRTC.crtR00HFrontEndPort : CRTC.crtR00HFrontEndTest;
             if (CRTC.crtR00HFrontEndCurr != curr) {
               CRTC.crtR00HFrontEndCurr = curr;
               CRTC.crtRestart ();
             }
           }
           return;
         case 0x0002:  //R01
           CRTC.crtR01HSyncEndPort = d & 0xff;
           {
             int curr = CRTC.crtR01HSyncEndMask == 0 ? CRTC.crtR01HSyncEndPort : CRTC.crtR01HSyncEndTest;
             if (CRTC.crtR01HSyncEndCurr != curr) {
               CRTC.crtR01HSyncEndCurr = curr;
               CRTC.crtRestart ();
             }
           }
           return;
         case 0x0004:  //R02
           CRTC.crtR02HBackEndPort = d & 0xff;
           {
             int curr = CRTC.crtR02HBackEndMask == 0 ? CRTC.crtR02HBackEndPort : CRTC.crtR02HBackEndTest;
             if (CRTC.crtR02HBackEndCurr != curr) {
               CRTC.crtR02HBackEndCurr = curr;
               CRTC.crtRestart ();
             }
           }
           return;
         case 0x0006:  //R03
           CRTC.crtR03HDispEndPort = d & 0xff;
           {
             int curr = CRTC.crtR03HDispEndMask == 0 ? CRTC.crtR03HDispEndPort : CRTC.crtR03HDispEndTest;
             if (CRTC.crtR03HDispEndCurr != curr) {
               CRTC.crtR03HDispEndCurr = curr;
               CRTC.crtRestart ();
             }
           }
           return;
         case 0x0008:  //R04
           CRTC.crtR04VFrontEndPort = d & CRTC.crtVerticalMask;
           {
             int curr = CRTC.crtR04VFrontEndMask == 0 ? CRTC.crtR04VFrontEndPort : CRTC.crtR04VFrontEndTest;
             if (CRTC.crtR04VFrontEndCurr != curr) {
               CRTC.crtR04VFrontEndCurr = curr;
               if (CRTC.CRT_RASTER_HASH_ON) {
                 CRTC.crtUpdateRasterHash ();
               }
               CRTC.crtRestart ();
             }
           }
           return;
         case 0x000a:  //R05
           CRTC.crtR05VSyncEndPort = d & CRTC.crtVerticalMask;
           {
             int curr = CRTC.crtR05VSyncEndMask == 0 ? CRTC.crtR05VSyncEndPort : CRTC.crtR05VSyncEndTest;
             if (CRTC.crtR05VSyncEndCurr != curr) {
               CRTC.crtR05VSyncEndCurr = curr;
               CRTC.crtRestart ();
             }
           }
           return;
         case 0x000c:  //R06
           CRTC.crtR06VBackEndPort = d & CRTC.crtVerticalMask;
           {
             int curr = CRTC.crtR06VBackEndMask == 0 ? CRTC.crtR06VBackEndPort : CRTC.crtR06VBackEndTest;
             if (CRTC.crtR06VBackEndCurr != curr) {
               CRTC.crtR06VBackEndCurr = curr;
               CRTC.crtVDispStart = curr + 1;
               if (CRTC.CRT_RASTER_HASH_ON) {
                 CRTC.crtUpdateRasterHash ();
               }
               CRTC.crtRestart ();
             }
           }
           return;
         case 0x000e:  //R07
           CRTC.crtR07VDispEndPort = d & CRTC.crtVerticalMask;
           {
             int curr = CRTC.crtR07VDispEndMask == 0 ? CRTC.crtR07VDispEndPort : CRTC.crtR07VDispEndTest;
             if (CRTC.crtR07VDispEndCurr != curr) {
               CRTC.crtR07VDispEndCurr = curr;
               CRTC.crtVIdleStart = curr + 1;
               if (CRTC.CRT_RASTER_HASH_ON) {
                 CRTC.crtUpdateRasterHash ();
               }
               CRTC.crtRestart ();
             }
           }
           return;
         case 0x0010:  //R08
           d &= 0xff;
           if (CRTC.crtR08Adjust != d) {
             CRTC.crtR08Adjust = d;
             CRTC.crtRestart ();
           }
           return;
         case 0x0012:  //R09
           CRTC.crtR09IRQRasterPort = d & CRTC.crtVerticalMask;
           {
             int curr = CRTC.crtR09IRQRasterMask == 0 ? CRTC.crtR09IRQRasterPort : CRTC.crtR09IRQRasterTest;
             if (CRTC.crtR09IRQRasterCurr != curr) {
               CRTC.crtR09IRQRasterCurr = curr;
               if (CRTC.CRT_RASTER_HASH_ON) {
                 CRTC.crtUpdateRasterHash ();
               }
               if (RasterBreakPoint.RBP_ON) {
                 RasterBreakPoint.rbpCheckIRQ ();
               }
             }
           }
           return;
         case 0x0014:  //R10
           CRTC.crtR10TxXPort = d & 0x03ff;
           {
             int curr = CRTC.crtR10TxXMask == 0 ? CRTC.crtR10TxXPort : CRTC.crtR10TxXTest;
             if (CRTC.crtR10TxXCurr != curr) {
               CRTC.crtR10TxXCurr = curr;
               CRTC.crtAllStamp += 2;
             }
           }
           return;
         case 0x0016:  //R11
           CRTC.crtR11TxYPort = d & 0x03ff;
           {
             int curr = CRTC.crtR11TxYMask == 0 ? CRTC.crtR11TxYPort : CRTC.crtR11TxYTest;
             if (CRTC.crtR11TxYCurr != curr) {
               CRTC.crtR11TxYCurr = curr;
               //CRTC.crtAllStamp += 2;  //ラッチするとき更新する
             }
           }
           return;
         case 0x0018:  //R12
         case 0x001c:  //R14
         case 0x0020:  //R16
         case 0x0024:  //R18
           {
             int p = (aa - 0x0018) >> 2;  //0,1,2,3
             CRTC.crtR12GrXPort[p] = d & (p == 0 ? 0x03ff : 0x01ff);
             int curr = CRTC.crtR12GrXMask[p] == 0 ? CRTC.crtR12GrXPort[p] : CRTC.crtR12GrXTest[p];
             if (CRTC.crtR12GrXCurr[p] != curr) {
               CRTC.crtR12GrXCurr[p] = curr;
               CRTC.crtAllStamp += 2;
             }
           }
           return;
         case 0x001a:  //R13
         case 0x001e:  //R15
         case 0x0022:  //R17
         case 0x0026:  //R19
           {
             int p = (aa - 0x0018) >> 2;  //0,1,2,3
             CRTC.crtR13GrYPort[p] = d & (p == 0 ? 0x03ff : 0x01ff);
             int curr = CRTC.crtR13GrYMask[p] == 0 ? CRTC.crtR13GrYPort[p] : CRTC.crtR13GrYTest[p];
             if (CRTC.crtR13GrYCurr[p] != curr) {
               CRTC.crtR13GrYCurr[p] = curr;
               //CRTC.crtAllStamp += 2;  //ラッチするとき更新する
             }
           }
           return;
         case 0x0028:  //R20
           CRTC.crtSetMemoryMode (d >> 12, d >> 11, d >> 8);
           CRTC.crtHighResoPort = d >>> 4 & 1;
           CRTC.crtVResoPort    = d >>> 2 & 3;
           CRTC.crtHResoPort    = d       & 3;
           SpriteScreen.sprAccessible = SpriteScreen.spr768x512 || (d & 0b10010) != 0b10010;
           int highResoCurr = CRTC.crtHighResoMask == 0 ? CRTC.crtHighResoPort : CRTC.crtHighResoTest;
           int vResoCurr = CRTC.crtVResoMask == 0 ? CRTC.crtVResoPort : CRTC.crtVResoTest;
           int hResoCurr = CRTC.crtHResoMask == 0 ? CRTC.crtHResoPort : CRTC.crtHResoTest;
           if (CRTC.crtHighResoCurr != highResoCurr ||
               CRTC.crtVResoCurr != vResoCurr ||
               CRTC.crtHResoCurr != hResoCurr) {
             CRTC.crtHighResoCurr = highResoCurr;
             CRTC.crtVResoCurr = vResoCurr;
             CRTC.crtHResoCurr = hResoCurr;
             CRTC.crtRestart ();
           }
           return;
         case 0x002a:  //R21
           CRTC.crtBitMask   = d << 22 < 0;  //(d & 512) != 0
           CRTC.crtSimAccess = d << 23 < 0;  //(d & 256) != 0
           CRTC.crtSimPlane3 = (byte) d < 0;  //(d & 128) != 0。d << 24 < 0
           CRTC.crtSimPlane2 = d << 25 < 0;  //(d & 64) != 0
           CRTC.crtSimPlane1 = d << 26 < 0;  //(d & 32) != 0
           CRTC.crtSimPlane0 = d << 27 < 0;  //(d & 16) != 0
           CRTC.crtCCPlane3  = XEiJ.TEST_BIT_3_SHIFT ? d << 31 - 3 < 0 : (d & 8) != 0;
           CRTC.crtCCPlane2  = XEiJ.TEST_BIT_2_SHIFT ? d << 31 - 2 < 0 : (d & 4) != 0;
           CRTC.crtCCPlane1  = XEiJ.TEST_BIT_1_SHIFT ? d << 31 - 1 < 0 : (d & 2) != 0;
           CRTC.crtCCPlane0  = XEiJ.TEST_BIT_0_SHIFT ? d << 31 - 0 < 0 : (d & 1) != 0;
           return;
         case 0x002c:  //R22
           CRTC.crtR22SrcBlock = d >> 8 & 0xff;
           CRTC.crtR22DstBlock = d      & 0xff;
           return;
         case 0x002e:  //R23
           CRTC.crtR23Mask = (char) d;
           return;
           //case 0x0030:  //R24
           //case 0x0032:  //R25
           //case 0x0034:  //R26
           //case 0x0036:  //R27
           //case 0x0038:  //R28
           //case 0x003a:  //R29
           //case 0x003c:  //R30
           //case 0x003e:  //R31
         default:
           return;
         }
       } else {  //$00E80400～$00E807FF
         aa &= 0xff;  //$00E80500～$00E807FF→$00E80400～$00E804FF
         if (aa < 0x80) {  //$00E80400～$00E8047F
           super.mmdWw (a, d);  //バスエラー
         } else {  //$00E80480～$00E804FF  動作ポート
           boolean rasterCopyOn = (d & 8) != 0;
           if (CRTC.crtRasterCopyOn != rasterCopyOn) {
             CRTC.crtRasterCopyOn = rasterCopyOn;  //ラスタコピー
             if (CRTC.CRT_RASTER_HASH_ON) {
               CRTC.crtUpdateRasterHash ();
             }
           }
           if (CRTC.crtClearFrames == 0) {
             CRTC.crtClearStandby = (d & 2) != 0;  //高速クリア
           }
           return;
         }
       }
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWw (a    , d >> 16);
       mmdWw (a + 2, d      );
     }
  },  //MMD_CRT

  //--------------------------------------------------------------------------------
  //MMD_VCN ビデオコントローラ
  //
  //  $00E82000～$00E821FF  ワードレジスタ
  //  $00E82200～$00E823FF  ワードレジスタ
  //  $00E82400～$00E82401  ワードレジスタ
  //  $00E82402～$00E824FF  $00E82400～$00E82401の繰り返し
  //  $00E82500～$00E82501  ワードレジスタ
  //  $00E82502～$00E825FF  $00E82500～$00E82501の繰り返し
  //  $00E82600～$00E82601  ワードレジスタ
  //  $00E82602～$00E826FF  $00E82600～$00E82601の繰り返し
  //  $00E82700～$00E82FFF  $0000
  //  $00E83000～$00E83FFF  $00E82000～$00E82FFFの繰り返し
  //
  MMD_VCN {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "ビデオコントローラ" : "Video Controller";
    }
    //リード
    @Override protected int mmdRbz (int a) throws M68kException {
       int aa = a & 0x1fff;  //$00E83000～$00E83FFF→$00E82000～$00E82FFF
       if (aa < 0x0400) {  //パレット
         XEiJ.mpuClockTime += XEiJ.busWaitTime.palet;
         int d = (aa < 0x0200 ? VideoController.vcnPal16G8Port[aa >> 1] :  //$00E82000～$00E821FF  グラフィック
                  VideoController.vcnPal16TSPort[(aa - 0x0200) >> 1]);  //$00E82200～$00E823FF  テキストスプライト
         return (aa & 1) == 0 ? d >> 8 : d & 0xff;
       } else {  //レジスタ
         XEiJ.mpuClockTime += XEiJ.busWaitTime.vicon;
         int d = (aa < 0x0500 ? VideoController.vcnReg1Port :  //$00E82400～$00E824FF
                  aa < 0x0600 ? VideoController.vcnReg2Port :  //$00E82500～$00E825FF
                  aa < 0x0700 ? VideoController.vcnReg3Port :  //$00E82600～$00E826FF
                  0);  //$00E82700～$00E82FFF
         return (aa & 1) == 0 ? d >> 8 : d & 0xff;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       int aa = a & 0x1fff;  //$00E83000～$00E83FFF→$00E82000～$00E82FFF
       if (aa < 0x0400) {  //パレット
         XEiJ.mpuClockTime += XEiJ.busWaitTime.palet;
         return (aa < 0x0200 ? VideoController.vcnPal16G8Port[aa >> 1] :  //$00E82000～$00E821FF  グラフィックスパレット
                 VideoController.vcnPal16TSPort[(aa - 0x0200) >> 1]);  //$00E82200～$00E823FF  テキストスプライトパレット
       } else {  //レジスタ
         XEiJ.mpuClockTime += XEiJ.busWaitTime.vicon;
         return (aa < 0x0500 ? VideoController.vcnReg1Port :  //$00E82400～$00E824FF
                 aa < 0x0600 ? VideoController.vcnReg2Port :  //$00E82500～$00E825FF
                 aa < 0x0700 ? VideoController.vcnReg3Port :  //$00E82600～$00E826FF
                 0);  //$00E82700～$00E82FFF
       }
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return mmdRwz (a) << 16 | mmdRwz (a + 2);
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       int aa = a & 0x1fff;  //$00E83000～$00E83FFF→$00E82000～$00E82FFF
       if (aa < 0x0200) {  //$00E82000～$00E821FF  グラフィックスパレット
         XEiJ.mpuClockTime += XEiJ.busWaitTime.palet;
         if (FlashingLights.FLR_ON) {
           FlashingLights.flrWriteByteG8 (a, d);
         } else {
           d &= 0xff;
           int n = aa >> 1;
           if ((aa & 1) == 0) {  //a=0,2,4,6,8,10,12,14
             int t = VideoController.vcnPal16G8Port[n] = d << 8 | (VideoController.vcnPal16G8Port[n] & 0xff);
             VideoController.vcnPal32G8[n] = VideoController.vcnPalTbl[t];
             if ((n & 1) == 0) {  //a=0,4,8,12 n=0,2,4,6
               VideoController.vcnPal8G16LPort[n] = d;
             } else {  //a=2,6,10,14 n-1=0,2,4,6
               VideoController.vcnPal8G16HPort[n - 1] = d << 8;
             }
           } else {  //a=1,3,5,7,9,11,13,15
             int t = VideoController.vcnPal16G8Port[n] = (VideoController.vcnPal16G8Port[n] & ~0xff) | d;
             VideoController.vcnPal32G8[n] = VideoController.vcnPalTbl[t];
             if ((n & 1) == 0) {  //a=1,5,9,13 n+1=1,3,5,7
               VideoController.vcnPal8G16LPort[n + 1] = d;
             } else {  //a=3,7,11,15 n=1,3,5,7
               VideoController.vcnPal8G16HPort[n] = d << 8;
             }
           }
           if ((VideoController.vcnReg3Curr & 0x001f) != 0) {  //グラフィックス画面が表示されている
             CRTC.crtAllStamp += 2;
           }
         }
       } else if (aa < 0x0400) {  //$00E82200～$00E823FF  テキストスプライトパレット
         XEiJ.mpuClockTime += XEiJ.busWaitTime.palet;
         if (FlashingLights.FLR_ON) {
           FlashingLights.flrWriteByteTS (a, d);
         } else {
           d &= 0xff;
           int n = (aa - 0x0200) >> 1;
           d = ((aa & 1) == 0 ?
                d << 8 | (VideoController.vcnPal16TSPort[n] & 0xff) :
                (VideoController.vcnPal16TSPort[n] & ~0xff) | d);
           VideoController.vcnPal16TSPort[n] = d;
           if (!ScreenModeTest.smtPatternTestOn) {
             VideoController.vcnPal32TS[n] = VideoController.vcnPalTbl[d];
             if ((VideoController.vcnReg3Curr << 31 - 6 < 0 && SpriteScreen.sprReg4BgCtrlCurr << 31 - 9 < 0) ||  //スプライト画面が表示されている
                 (VideoController.vcnReg3Curr << 31 - 5 < 0 && n < 16)) {  //テキスト画面が表示されていてテキストパレットが操作された
               CRTC.crtAllStamp += 2;
             }
           }
         }
       } else if (aa < 0x0500) {  //$00E82400～$00E824FF
         XEiJ.mpuClockTime += XEiJ.busWaitTime.vicon;
         d = (aa & 1) == 0 ? 0x00 : d & 0x07;
         if (VideoController.vcnReg1Port != d) {
           VideoController.vcnReg1Port = d;
           VideoController.vcnReg1Curr = ((VideoController.vcnReg1Port & ~VideoController.vcnReg1Mask) |
                                          (VideoController.vcnReg1Test & VideoController.vcnReg1Mask));
           VideoController.vcnUpdateMode ();
         }
       } else if (aa < 0x0600) {  //$00E82500～$00E825FF
         XEiJ.mpuClockTime += XEiJ.busWaitTime.vicon;
         d = ((aa & 1) == 0 ?
              (d & 0x3f) << 8 | (VideoController.vcnReg2Port & 0xff) :
              (VideoController.vcnReg2Port & ~0xff) | (d & 0xff));
         if (VideoController.vcnReg2Port != d) {
           VideoController.vcnReg2Port = d;
           VideoController.vcnReg2Curr = ((VideoController.vcnReg2Port & ~VideoController.vcnReg2Mask) |
                                          (VideoController.vcnReg2Test & VideoController.vcnReg2Mask));
           VideoController.vcnUpdateMode ();
         }
       } else if (aa < 0x0700) {  //$00E82600～$00E826FF
         XEiJ.mpuClockTime += XEiJ.busWaitTime.vicon;
         d = ((aa & 1) == 0 ?
              (d & 0xff) << 8 | (VideoController.vcnReg3Port & 0xff) :
              (VideoController.vcnReg3Port & ~0xff) | (d & 0xff));
         if (VideoController.vcnReg3Port != d) {
           VideoController.vcnReg3Port = d;
           VideoController.vcnReg3Curr = ((VideoController.vcnReg3Port & ~VideoController.vcnReg3Mask) |
                                          (VideoController.vcnReg3Test & VideoController.vcnReg3Mask));
           VideoController.vcnUpdateMode ();
         }
       } else {  //$00E82700～$00E82FFF
         XEiJ.mpuClockTime += XEiJ.busWaitTime.vicon;
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       int aa = a & 0x1fff;  //$00E83000～$00E83FFF→$00E82000～$00E82FFF
       if (aa < 0x0200) {  //$00E82000～$00E821FF  グラフィックスパレット
         XEiJ.mpuClockTime += XEiJ.busWaitTime.palet;
         if (FlashingLights.FLR_ON) {
           FlashingLights.flrWriteWordG8 (a, d);
         } else {
           int n = aa >> 1;  //a=0,2,4,6,8,10,12,14 n=0,1,2,3,4,5,6,7
           int t = VideoController.vcnPal16G8Port[n] = (char) d;
           VideoController.vcnPal32G8[n] = VideoController.vcnPalTbl[t];
           if ((n & 1) == 0) {  //a=0,4,8,12 n=0,2,4,6 n+1=1,3,5,7
             VideoController.vcnPal8G16LPort[n] = d >> 8 & 0xff;
             VideoController.vcnPal8G16LPort[n + 1] = d & 0xff;
           } else {  //a=2,4,6,8 n-1=0,2,4,6 n=1,3,5,7
             VideoController.vcnPal8G16HPort[n - 1] = d & 0xff00;
             VideoController.vcnPal8G16HPort[n] = (d & 0xff) << 8;
           }
           if ((VideoController.vcnReg3Curr & 0x001f) != 0) {  //グラフィックス画面が表示されている
             CRTC.crtAllStamp += 2;
           }
         }
       } else if (aa < 0x0400) {  //$00E82200～$00E823FF  テキストスプライトパレット
         XEiJ.mpuClockTime += XEiJ.busWaitTime.palet;
         if (FlashingLights.FLR_ON) {
           FlashingLights.flrWriteWordTS (a, d);
         } else {
           int n = (aa - 0x0200) >> 1;
           d = (char) d;
           VideoController.vcnPal16TSPort[n] = d;
           if (!ScreenModeTest.smtPatternTestOn) {
             VideoController.vcnPal32TS[n] = VideoController.vcnPalTbl[d];
             if ((VideoController.vcnReg3Curr << 31 - 6 < 0 && SpriteScreen.sprReg4BgCtrlCurr << 31 - 9 < 0) ||  //スプライト画面が表示されている
                 (VideoController.vcnReg3Curr << 31 - 5 < 0 && n < 16)) {  //テキスト画面が表示されていてテキストパレットが操作された
               CRTC.crtAllStamp += 2;
             }
           }
         }
       } else if (aa < 0x0500) {  //$00E82400～$00E824FF
         XEiJ.mpuClockTime += XEiJ.busWaitTime.vicon;
         d &= 0x0007;
         if (VideoController.vcnReg1Port != d) {
           VideoController.vcnReg1Port = d;
           VideoController.vcnReg1Curr = ((VideoController.vcnReg1Port & ~VideoController.vcnReg1Mask) |
                                          (VideoController.vcnReg1Test & VideoController.vcnReg1Mask));
           VideoController.vcnUpdateMode ();
         }
       } else if (aa < 0x0600) {  //$00E82500～$00E825FF
         XEiJ.mpuClockTime += XEiJ.busWaitTime.vicon;
         d &= 0x3fff;
         if (VideoController.vcnReg2Port != d) {
           VideoController.vcnReg2Port = d;
           VideoController.vcnReg2Curr = ((VideoController.vcnReg2Port & ~VideoController.vcnReg2Mask) |
                                          (VideoController.vcnReg2Test & VideoController.vcnReg2Mask));
           VideoController.vcnUpdateMode ();
         }
       } else if (aa < 0x0700) {  //$00E82600～$00E826FF
         XEiJ.mpuClockTime += XEiJ.busWaitTime.vicon;
         d &= 0xffff;
         if (VideoController.vcnReg3Port != d) {
           VideoController.vcnReg3Port = d;
           VideoController.vcnReg3Curr = ((VideoController.vcnReg3Port & ~VideoController.vcnReg3Mask) |
                                          (VideoController.vcnReg3Test & VideoController.vcnReg3Mask));
           VideoController.vcnUpdateMode ();
         }
       } else {  //$00E82700～$00E82FFF
         XEiJ.mpuClockTime += XEiJ.busWaitTime.vicon;
       }
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWw (a    , d >> 16);
       mmdWw (a + 2, d      );
     }
  },  //MMD_VCN

  //--------------------------------------------------------------------------------
  //MMD_DMA DMAコントローラ
  //
  //  $00E84000～$00E8403F  ワードまたはロングワードレジスタ(CH0)
  //  $00E84040～$00E8407F  ワードまたはロングワードレジスタ(CH1)
  //  $00E84080～$00E840BF  ワードまたはロングワードレジスタ(CH2)
  //  $00E840C0～$00E840FF  ワードまたはロングワードレジスタ(CH4)
  //  $00E84100～$00E85FFF  $00E84000～$00E840FFの繰り返し
  //
  MMD_DMA {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "DMA コントローラ" : "DMA Controller";
    }
    //リード
    @Override protected int mmdRbz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.dmac;
       return HD63450.dmaReadByte (a);
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.dmac;
       return HD63450.dmaReadWord (a);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.dmac * 2;
       return HD63450.dmaReadLong (a);
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.dmac;
       HD63450.dmaWriteByte (a, d);
       return;
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.dmac;
       HD63450.dmaWriteWord (a, d);
       return;
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.dmac * 2;
       HD63450.dmaWriteLong (a, d);
       return;
     }
  },  //MMD_DMA

  //--------------------------------------------------------------------------------
  //MMD_SVS スーパーバイザ領域設定
  //
  //  $00E86000  バイトアクセスはバスエラー。ワードリードはバスエラー。ワードライトは$??xx
  //  $00E86001  バイトリードはバスエラー。バイトライトは$xx
  //  $00E86002～$00E87FFF  $00E86000～$00E86001の繰り返し
  //
  MMD_SVS {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "スーパーバイザ領域設定" : "Supervisor Area Setting";
    }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if ((a & 1) == 0) {  //偶数アドレスへのバイトライト
         super.mmdWb (a, d);  //バスエラー
       } else {  //奇数アドレスへのバイトライト
         XEiJ.mpuClockTime += XEiJ.busWaitTime.sysport;  //!!!
         MainMemory.mmrSetSupervisorArea (d);
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       mmdWb (a + 1, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWw (a, d >> 16);
       mmdWw (a + 2, d);
     }
  },  //MMD_SVS

  //--------------------------------------------------------------------------------
  //MMD_MFP MFP
  //
  //  $00E88000～$00E8803F(偶数)  バイトアクセスはバスエラー。ワードリードは$FFxx。ワードライトは$??xx
  //  $00E88000～$00E8803F(奇数)  バイトレジスタ
  //  $00E88040～$00E89FFF  $00E88000～$00E8803Fの繰り返し
  //
  MMD_MFP_00 {  //X68000
    @Override public String toString () {
      return "Multi-Function Peripheral (00)";
    }
    @Override protected int mmdPbz (int a) {
      if (true) {  //X68000
        return MC68901.mfpPeekByte_00 (a);
      } else {  //X68030
        return MC68901.mfpPeekByte_30 (a);
      }
    }
    @Override protected int mmdRbz (int a) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           return super.mmdRbz (a);  //バスエラー
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.mfp;
       if (true) {  //X68000
         return MC68901.mfpReadByte_00 (a);
       } else {  //X68030
         int d = MC68901.mfpReadByte_30 (a);
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
         return d;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       return 0xff00 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return 0xff00ff00 | mmdRbz (a + 1) << 16 | mmdRbz (a + 3);
     }
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           super.mmdWb (a, d);  //バスエラー
           return;
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.mfp;
       MC68901.mfpWriteByte (a, d);
       if (false) {  //X68030
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       mmdWb (a + 1, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWb (a + 1, d >> 16);
       mmdWb (a + 3, d);
     }
  },  //MMD_MFP_00
  MMD_MFP_03 {  //Xellent30の030モード
    @Override public String toString () {
      return "Multi-Function Peripheral (03)";
    }
    @Override protected int mmdPbz (int a) {
      if (true) {  //X68000
        return MC68901.mfpPeekByte_00 (a);
      } else {  //X68030
        return MC68901.mfpPeekByte_30 (a);
      }
    }
    @Override protected int mmdRbz (int a) throws M68kException {
       if (false) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           return super.mmdRbz (a);  //バスエラー
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.mfp;
       if (true) {  //X68000
         return MC68901.mfpReadByte_00 (a);
       } else {  //X68030
         int d = MC68901.mfpReadByte_30 (a);
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
         return d;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       return 0xff00 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return 0xff00ff00 | mmdRbz (a + 1) << 16 | mmdRbz (a + 3);
     }
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if (false) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           super.mmdWb (a, d);  //バスエラー
           return;
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.mfp;
       MC68901.mfpWriteByte (a, d);
       if (false) {  //X68030
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       mmdWb (a + 1, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWb (a + 1, d >> 16);
       mmdWb (a + 3, d);
     }
  },  //MMD_MFP_03
  MMD_MFP_30 {  //X68030
    @Override public String toString () {
      return "Multi-Function Peripheral (30)";
    }
    @Override protected int mmdPbz (int a) {
      if (false) {  //X68000
        return MC68901.mfpPeekByte_00 (a);
      } else {  //X68030
        return MC68901.mfpPeekByte_30 (a);
      }
    }
    @Override protected int mmdRbz (int a) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           return super.mmdRbz (a);  //バスエラー
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.mfp;
       if (false) {  //X68000
         return MC68901.mfpReadByte_00 (a);
       } else {  //X68030
         int d = MC68901.mfpReadByte_30 (a);
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
         return d;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       return 0xff00 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return 0xff00ff00 | mmdRbz (a + 1) << 16 | mmdRbz (a + 3);
     }
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           super.mmdWb (a, d);  //バスエラー
           return;
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.mfp;
       MC68901.mfpWriteByte (a, d);
       if (true) {  //X68030
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       mmdWb (a + 1, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWb (a + 1, d >> 16);
       mmdWb (a + 3, d);
     }
  },  //MMD_MFP_30

  //--------------------------------------------------------------------------------
  //MMD_RTC_FIRST RTC
  //
  //  $00E8A000～$00E8A01F(偶数)  バイトアクセスはバスエラー。ワードリードは$FFxx。ワードライトは$??xx
  //  $00E8A000～$00E8A01F(奇数)  バイトレジスタ
  //  $00E8A020～$00E8AFFF  $00E8A000～$00E8A01Fの繰り返し
  //
  MMD_RTC_FIRST_00 {  //X68000
    @Override public String toString () {
      return "Real-Time Clock (00)";
    }
    @Override protected int mmdPbz (int a) {
      return RP5C15.rtcPeekByte (a);
    }
    @Override protected int mmdRbz (int a) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           return super.mmdRbz (a);  //バスエラー
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.rtc;
       XEiJ.busSuper (MemoryMappedDevice.MMD_RTC_NEXT_00, 0x00e8a000, 0x00e8c000);  //RTC RTC
       RP5C15.rtcUpdate ();
       if (true) {  //X68000
         return RP5C15.rtcReadByte (a);
       } else {  //X68030
         int d = RP5C15.rtcReadByte (a);
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
         return d;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       return 0xff00 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return 0xff00ff00 | mmdRbz (a + 1) << 16 | mmdRbz (a + 3);
     }
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           super.mmdWb (a, d);  //バスエラー
           return;
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.rtc;
       XEiJ.busSuper (MemoryMappedDevice.MMD_RTC_NEXT_00, 0x00e8a000, 0x00e8c000);  //RTC RTC
       RP5C15.rtcUpdate ();
       RP5C15.rtcWriteByte (a, d);
       if (false) {  //X68030
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       mmdWb (a + 1, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWb (a + 1, d >> 16);
       mmdWb (a + 3, d);
     }
  },  //MMD_RTC_FIRST_00
  MMD_RTC_FIRST_03 {  //Xellent30の030モード
    @Override public String toString () {
      return "Real-Time Clock (03)";
    }
    @Override protected int mmdPbz (int a) {
      return RP5C15.rtcPeekByte (a);
    }
    @Override protected int mmdRbz (int a) throws M68kException {
       if (false) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           return super.mmdRbz (a);  //バスエラー
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.rtc;
       XEiJ.busSuper (MemoryMappedDevice.MMD_RTC_NEXT_03, 0x00e8a000, 0x00e8c000);  //RTC RTC
       RP5C15.rtcUpdate ();
       if (true) {  //X68000
         return RP5C15.rtcReadByte (a);
       } else {  //X68030
         int d = RP5C15.rtcReadByte (a);
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
         return d;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       return 0xff00 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return 0xff00ff00 | mmdRbz (a + 1) << 16 | mmdRbz (a + 3);
     }
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if (false) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           super.mmdWb (a, d);  //バスエラー
           return;
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.rtc;
       XEiJ.busSuper (MemoryMappedDevice.MMD_RTC_NEXT_03, 0x00e8a000, 0x00e8c000);  //RTC RTC
       RP5C15.rtcUpdate ();
       RP5C15.rtcWriteByte (a, d);
       if (false) {  //X68030
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       mmdWb (a + 1, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWb (a + 1, d >> 16);
       mmdWb (a + 3, d);
     }
  },  //MMD_RTC_FIRST_03
  MMD_RTC_FIRST_30 {  //X68030
    @Override public String toString () {
      return "Real-Time Clock (30)";
    }
    @Override protected int mmdPbz (int a) {
      return RP5C15.rtcPeekByte (a);
    }
    @Override protected int mmdRbz (int a) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           return super.mmdRbz (a);  //バスエラー
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.rtc;
       XEiJ.busSuper (MemoryMappedDevice.MMD_RTC_NEXT_30, 0x00e8a000, 0x00e8c000);  //RTC RTC
       RP5C15.rtcUpdate ();
       if (false) {  //X68000
         return RP5C15.rtcReadByte (a);
       } else {  //X68030
         int d = RP5C15.rtcReadByte (a);
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
         return d;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       return 0xff00 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return 0xff00ff00 | mmdRbz (a + 1) << 16 | mmdRbz (a + 3);
     }
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           super.mmdWb (a, d);  //バスエラー
           return;
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.rtc;
       XEiJ.busSuper (MemoryMappedDevice.MMD_RTC_NEXT_30, 0x00e8a000, 0x00e8c000);  //RTC RTC
       RP5C15.rtcUpdate ();
       RP5C15.rtcWriteByte (a, d);
       if (true) {  //X68030
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       mmdWb (a + 1, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWb (a + 1, d >> 16);
       mmdWb (a + 3, d);
     }
  },  //MMD_RTC_FIRST_30

  //--------------------------------------------------------------------------------
  //MMD_RTC_NEXT RTC
  MMD_RTC_NEXT_00 {  //X68000
    @Override public String toString () {
      return "Real-Time Clock (00)";
    }
    @Override protected int mmdPbz (int a) {
      return RP5C15.rtcPeekByte (a);
    }
    @Override protected int mmdRbz (int a) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           return super.mmdRbz (a);  //バスエラー
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.rtc;
       if (true) {  //X68000
         return RP5C15.rtcReadByte (a);
       } else {  //X68030
         int d = RP5C15.rtcReadByte (a);
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
         return d;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       return 0xff00 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return 0xff00ff00 | mmdRbz (a + 1) << 16 | mmdRbz (a + 3);
     }
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           super.mmdWb (a, d);  //バスエラー
           return;
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.rtc;
       RP5C15.rtcWriteByte (a, d);
       if (false) {  //X68030
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       mmdWb (a + 1, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWb (a + 1, d >> 16);
       mmdWb (a + 3, d);
     }
  },  //MMD_RTC_NEXT_00
  MMD_RTC_NEXT_03 {  //Xellent30の030モード
    @Override public String toString () {
      return "Real-Time Clock (03)";
    }
    @Override protected int mmdPbz (int a) {
      return RP5C15.rtcPeekByte (a);
    }
    @Override protected int mmdRbz (int a) throws M68kException {
       if (false) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           return super.mmdRbz (a);  //バスエラー
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.rtc;
       if (true) {  //X68000
         return RP5C15.rtcReadByte (a);
       } else {  //X68030
         int d = RP5C15.rtcReadByte (a);
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
         return d;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       return 0xff00 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return 0xff00ff00 | mmdRbz (a + 1) << 16 | mmdRbz (a + 3);
     }
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if (false) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           super.mmdWb (a, d);  //バスエラー
           return;
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.rtc;
       RP5C15.rtcWriteByte (a, d);
       if (false) {  //X68030
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       mmdWb (a + 1, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWb (a + 1, d >> 16);
       mmdWb (a + 3, d);
     }
  },  //MMD_RTC_NEXT_03
  MMD_RTC_NEXT_30 {  //X68030
    @Override public String toString () {
      return "Real-Time Clock (30)";
    }
    @Override protected int mmdPbz (int a) {
      return RP5C15.rtcPeekByte (a);
    }
    @Override protected int mmdRbz (int a) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           return super.mmdRbz (a);  //バスエラー
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.rtc;
       if (false) {  //X68000
         return RP5C15.rtcReadByte (a);
       } else {  //X68030
         int d = RP5C15.rtcReadByte (a);
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
         return d;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       return 0xff00 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return 0xff00ff00 | mmdRbz (a + 1) << 16 | mmdRbz (a + 3);
     }
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           super.mmdWb (a, d);  //バスエラー
           return;
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.rtc;
       RP5C15.rtcWriteByte (a, d);
       if (true) {  //X68030
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       mmdWb (a + 1, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWb (a + 1, d >> 16);
       mmdWb (a + 3, d);
     }
  },  //MMD_RTC_NEXT_30

  //--------------------------------------------------------------------------------
  //MMD_RTC_TEST RTC
  MMD_RTC_TEST_00 {  //X68000
    @Override public String toString () {
      return "Real-Time Clock (00)";
    }
    @Override protected int mmdPbz (int a) {
      return RP5C15.rtcPeekByte (a);
    }
    @Override protected int mmdRbz (int a) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           return super.mmdRbz (a);  //バスエラー
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.rtc;
       RP5C15.rtcTestUpdate ();
       if (true) {  //X68000
         return RP5C15.rtcReadByte (a);
       } else {  //X68030
         int d = RP5C15.rtcReadByte (a);
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
         return d;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       return 0xff00 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return 0xff00ff00 | mmdRbz (a + 1) << 16 | mmdRbz (a + 3);
     }
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           super.mmdWb (a, d);  //バスエラー
           return;
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.rtc;
       RP5C15.rtcTestUpdate ();
       RP5C15.rtcWriteByte (a, d);
       if (false) {  //X68030
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       mmdWb (a + 1, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWb (a + 1, d >> 16);
       mmdWb (a + 3, d);
     }
  },  //MMD_RTC_TEST_00
  MMD_RTC_TEST_03 {  //Xellent30の030モード
    @Override public String toString () {
      return "Real-Time Clock (03)";
    }
    @Override protected int mmdPbz (int a) {
      return RP5C15.rtcPeekByte (a);
    }
    @Override protected int mmdRbz (int a) throws M68kException {
       if (false) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           return super.mmdRbz (a);  //バスエラー
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.rtc;
       RP5C15.rtcTestUpdate ();
       if (true) {  //X68000
         return RP5C15.rtcReadByte (a);
       } else {  //X68030
         int d = RP5C15.rtcReadByte (a);
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
         return d;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       return 0xff00 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return 0xff00ff00 | mmdRbz (a + 1) << 16 | mmdRbz (a + 3);
     }
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if (false) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           super.mmdWb (a, d);  //バスエラー
           return;
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.rtc;
       RP5C15.rtcTestUpdate ();
       RP5C15.rtcWriteByte (a, d);
       if (false) {  //X68030
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       mmdWb (a + 1, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWb (a + 1, d >> 16);
       mmdWb (a + 3, d);
     }
  },  //MMD_RTC_TEST_03
  MMD_RTC_TEST_30 {  //X68030
    @Override public String toString () {
      return "Real-Time Clock (30)";
    }
    @Override protected int mmdPbz (int a) {
      return RP5C15.rtcPeekByte (a);
    }
    @Override protected int mmdRbz (int a) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           return super.mmdRbz (a);  //バスエラー
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.rtc;
       RP5C15.rtcTestUpdate ();
       if (false) {  //X68000
         return RP5C15.rtcReadByte (a);
       } else {  //X68030
         int d = RP5C15.rtcReadByte (a);
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
         return d;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       return 0xff00 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return 0xff00ff00 | mmdRbz (a + 1) << 16 | mmdRbz (a + 3);
     }
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           super.mmdWb (a, d);  //バスエラー
           return;
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.rtc;
       RP5C15.rtcTestUpdate ();
       RP5C15.rtcWriteByte (a, d);
       if (true) {  //X68030
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       mmdWb (a + 1, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWb (a + 1, d >> 16);
       mmdWb (a + 3, d);
     }
  },  //MMD_RTC_TEST_30

  //--------------------------------------------------------------------------------
  //MMD_PRN プリンタポート
  //
  //  $00E8C000～$00E8C003(偶数)  バイトリードは$FF。バイトライトは$??。ワードリードは$FFxx。ワードライトは$??xx
  //  $00E8C000～$00E8C003(奇数)  バイトリードは$FF。バイトライトは$??
  //  $00E8C004～$00E8DFFF  $00E8C000～$00E8C003の繰り返し(?)
  //
  MMD_PRN {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "プリンタポート" : "Printer Port";
    }
    //リード
    @Override protected int mmdRbz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.prnport;
       a &= 3;
       return (a == (PrinterPort.PRN_DATA & 3) ? PrinterPort.prnReadData () :
               a == (PrinterPort.PRN_STROBE & 3) ? PrinterPort.prnReadStrobe () :
               0xff);
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       return 0xff00 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return 0xff00ff00 | mmdRbz (a + 1) << 16 | mmdRbz (a + 3);
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.prnport;
       a &= 3;
       if (a == (PrinterPort.PRN_DATA & 3)) {
         PrinterPort.prnWriteData (d);
       } else if (a == (PrinterPort.PRN_STROBE & 3)) {
         PrinterPort.prnWriteStrobe (d);
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       mmdWb (a + 1, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWb (a + 1, d >> 16);
       mmdWb (a + 3, d);
     }
  },  //MMD_PRN

  //--------------------------------------------------------------------------------
  //MMD_SYS システムポート
  //
  //  $00E8E000～$00E8E00F(偶数)  バイトリードは$FF、バイトライトは$??、ワードリードは$FFxx、ワードライトは$??xx
  //  $00E8E000～$00E8E00F(奇数)  バイトリードは$xx、バイトライトは$xx
  //  $00E8E010～$00E8FFFF  $00E8E000～$00E8E00Fの繰り返し
  //
  MMD_SYS {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "システムポート" : "System Port";
    }
    //リード
    @Override protected int mmdRbz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.sysport;
       switch (a & 15) {
       case 0x01:
         return 0b11110000 | VideoController.vcnTargetContrastPort;
       case 0x03:
         return 0b11111000 | XEiJ.pnlStereoscopicShutter;
       case 0x07:
         return 0b11110100 | (Keyboard.kbdOn ? 8 : 0) | CRTC.crtHRLPort << 1;
       case 0x0b:
         return (XEiJ.currentModel.isX68030 () ? 0xdc :
                 XEiJ.currentMPU < Model.MPU_MC68020 ?
                 XEiJ.mpuClockMHz <= 10.0 ? 0xff : 0xfe :
                 XEiJ.mpuClockMHz <= 20.0 ? 0xff : 0xfe);
       }
       return 0xff;
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       return 0xff00 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return 0xff00ff00 | mmdRbz (a + 1) << 16 | mmdRbz (a + 3);
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.sysport;
       d &= 0xff;
       switch (a & 15) {
       case 0x01:
         VideoController.vcnTargetContrastPort = d & 15;
         if (FlashingLights.FLR_ON) {
           FlashingLights.flrUpdateContrast ();
         } else {
           int curr = VideoController.vcnTargetContrastMask == 0 ? VideoController.vcnTargetContrastPort : VideoController.vcnTargetContrastTest;
           if (VideoController.vcnTargetContrastCurr != curr) {
             VideoController.vcnTargetContrastCurr = curr;
             VideoController.vcnTargetScaledContrast = VideoController.VCN_CONTRAST_SCALE * VideoController.vcnTargetContrastCurr;
             CRTC.crtContrastClock = XEiJ.mpuClockTime;
             CRTC.crtFrameTaskClock = Math.min (CRTC.crtContrastClock, CRTC.crtCaptureClock);
           }
         }
         return;
       case 0x03:
         XEiJ.pnlStereoscopicShutter = d & 3;
         break;
       case 0x07:
         {
           CRTC.crtHRLPort = d >> 1 & 1;
           int curr = CRTC.crtHRLMask == 0 ? CRTC.crtHRLPort : CRTC.crtHRLTest;
           if (CRTC.crtHRLCurr != curr) {
             CRTC.crtHRLCurr = curr;
             CRTC.crtRestart ();
           }
           if ((d & 1 << 2) != 0) {
             XEiJ.sysResetNMI ();  //NMIリセット
           }
         }
         return;
       case 0x09:
         if (XEiJ.currentModel.isX68030 ()) {
           //X68030のとき
           //  d=ROMウェイト設定値<<4|RAMウェイト設定値
           //  ROMウェイト
           //    ROMウェイト設定値+2
           //  RAMウェイト
           //    RAMウェイト設定値が0のとき
           //      スタティックカラムが有効なとき0
           //      スタティックカラムが無効なとき4
           //    RAMウェイト設定値が0でないとき
           //      RAMウェイト設定値+2
           //  ROMとRAMにはキャッシュが効く
           if (XEiJ.currentModel.isX68030 ()) {  //X68030
             XEiJ.mpuROMWaitCycles = (d >> 4 & 15) + 2;
             XEiJ.mpuRAMWaitCycles = (d & 15) == 0 ? 0 : (d & 15) + 2;
             XEiJ.mpuSetWait ();
           }
         }
         return;
       case 0x0d:
         SRAM.smrWriteEnableOn = d == 0x31;
         return;
       case 0x0f:
         if (PowerControl.PWC_ON) {
           PowerControl.pwcWritePort (d);
         }
         return;
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       mmdWb (a + 1, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWb (a + 1, d >> 16);
       mmdWb (a + 3, d);
     }
  },  //MMD_SYS

  //--------------------------------------------------------------------------------
  //MMD_OPM FM音源
  //
  //  $00E90000  バイトリードはバスエラー、バイトライトはバスエラー、ワードリードは$FFFF、ワードライトは$??xx
  //  $00E90001  バイトリードは$FF、バイトライトは$xx
  //  $00E90002  バイトリードはバスエラー、バイトライトはバスエラー、ワードリードは$FFxx、ワードライトは$??xx
  //  $00E90003  バイトリードは$xx、バイトライトは$xx
  //  $00E90004～$00E91FFF  $00E90000～$00E90003の繰り返し
  //
  MMD_OPM_00 {  //X68000
    @Override public String toString () {
      return "FM Sound Generator (00)";
    }
    @Override protected int mmdRbz (int a) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           return super.mmdRbz (a);  //バスエラー
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.opm;
       if (true) {  //X68000
         return OPM.opmReadByte (a);
       } else {  //X68030
         int d = OPM.opmReadByte (a);
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
         return d;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       return 0xff00 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return 0xff00ff00 | mmdRbz (a + 1) << 16 | mmdRbz (a + 3);
     }
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           super.mmdWb (a, d);  //バスエラー
           return;
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.opm;
       OPM.opmWriteByte (a, d);
       if (false) {  //X68030
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       mmdWb (a + 1, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWb (a + 1, d >> 16);
       mmdWb (a + 3, d);
     }
  },  //MMD_OPM_00
  MMD_OPM_03 {  //Xellent30の030モード
    @Override public String toString () {
      return "FM Sound Generator (03)";
    }
    @Override protected int mmdRbz (int a) throws M68kException {
       if (false) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           return super.mmdRbz (a);  //バスエラー
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.opm;
       if (true) {  //X68000
         return OPM.opmReadByte (a);
       } else {  //X68030
         int d = OPM.opmReadByte (a);
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
         return d;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       return 0xff00 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return 0xff00ff00 | mmdRbz (a + 1) << 16 | mmdRbz (a + 3);
     }
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if (false) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           super.mmdWb (a, d);  //バスエラー
           return;
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.opm;
       OPM.opmWriteByte (a, d);
       if (false) {  //X68030
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       mmdWb (a + 1, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWb (a + 1, d >> 16);
       mmdWb (a + 3, d);
     }
  },  //MMD_OPM_03
  MMD_OPM_30 {  //X68030
    @Override public String toString () {
      return "FM Sound Generator (30)";
    }
    @Override protected int mmdRbz (int a) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           return super.mmdRbz (a);  //バスエラー
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.opm;
       if (false) {  //X68000
         return OPM.opmReadByte (a);
       } else {  //X68030
         int d = OPM.opmReadByte (a);
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
         return d;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       return 0xff00 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return 0xff00ff00 | mmdRbz (a + 1) << 16 | mmdRbz (a + 3);
     }
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           super.mmdWb (a, d);  //バスエラー
           return;
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.opm;
       OPM.opmWriteByte (a, d);
       if (true) {  //X68030
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       mmdWb (a + 1, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWb (a + 1, d >> 16);
       mmdWb (a + 3, d);
     }
  },  //MMD_OPM_30

  //--------------------------------------------------------------------------------
  //MMD_PCM ADPCM音源
  //
  //  $00E92000～$00E92003(偶数)  バイトリードはバスエラー、バイトライトはバスエラー、ワードリードは$FFxx、ワードライトは$??xx
  //  $00E92000～$00E92003(奇数)  バイトリードは$xx、バイトライトは$xx
  //  $00E92004～$00E93FFF  $00E92000～$00E92003の繰り返し
  //
  MMD_PCM_00 {  //X68000
    @Override public String toString () {
      return "ADPCM Sound Generator (00)";
    }
    @Override protected int mmdRbz (int a) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           return super.mmdRbz (a);  //バスエラー
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.adpcm;
       if (true) {  //X68000
         return ADPCM.pcmReadByte (a);
       } else {  //X68030
         int d = ADPCM.pcmReadByte (a);
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
         return d;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       return 0xff00 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return 0xff00ff00 | mmdRbz (a + 1) << 16 | mmdRbz (a + 3);
     }
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           super.mmdWb (a, d);  //バスエラー
           return;
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.adpcm;
       ADPCM.pcmWriteByte (a, d);
       if (false) {  //X68030
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       mmdWb (a + 1, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWb (a + 1, d >> 16);
       mmdWb (a + 3, d);
     }
  },  //MMD_PCM_00
  MMD_PCM_03 {  //Xellent30の030モード
    @Override public String toString () {
      return "ADPCM Sound Generator (03)";
    }
    @Override protected int mmdRbz (int a) throws M68kException {
       if (false) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           return super.mmdRbz (a);  //バスエラー
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.adpcm;
       if (true) {  //X68000
         return ADPCM.pcmReadByte (a);
       } else {  //X68030
         int d = ADPCM.pcmReadByte (a);
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
         return d;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       return 0xff00 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return 0xff00ff00 | mmdRbz (a + 1) << 16 | mmdRbz (a + 3);
     }
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if (false) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           super.mmdWb (a, d);  //バスエラー
           return;
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.adpcm;
       ADPCM.pcmWriteByte (a, d);
       if (false) {  //X68030
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       mmdWb (a + 1, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWb (a + 1, d >> 16);
       mmdWb (a + 3, d);
     }
  },  //MMD_PCM_03
  MMD_PCM_30 {  //X68030
    @Override public String toString () {
      return "ADPCM Sound Generator (30)";
    }
    @Override protected int mmdRbz (int a) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           return super.mmdRbz (a);  //バスエラー
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.adpcm;
       if (false) {  //X68000
         return ADPCM.pcmReadByte (a);
       } else {  //X68030
         int d = ADPCM.pcmReadByte (a);
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
         return d;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       return 0xff00 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return 0xff00ff00 | mmdRbz (a + 1) << 16 | mmdRbz (a + 3);
     }
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           super.mmdWb (a, d);  //バスエラー
           return;
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.adpcm;
       ADPCM.pcmWriteByte (a, d);
       if (true) {  //X68030
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       mmdWb (a + 1, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWb (a + 1, d >> 16);
       mmdWb (a + 3, d);
     }
  },  //MMD_PCM_30

  //--------------------------------------------------------------------------------
  //MMD_FDC FDコントローラ
  //
  //  $00E94000～$00E94007(偶数)  バイトリードはバスエラー、バイトライトはバスエラー、ワードリードは$FFxx、ワードライトは$??xx
  //  $00E94000～$00E94007(奇数)  バイトリードは$xx、バイトライトは$xx
  //  $00E94008～$00E95FFF  $00E94000～$00E94007の繰り返し
  //
  MMD_FDC_00 {  //X68000
    @Override public String toString () {
      return "Floppy Disk Controller (00)";
    }
    @Override protected int mmdPbz (int a) {
      return FDC.fdcPeekByte (a);
    }
    @Override protected int mmdRbz (int a) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           return super.mmdRbz (a);  //バスエラー
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.fdc;
       if (true) {  //X68000
         return FDC.fdcReadByte (a);
       } else {  //X68030
         int d = FDC.fdcReadByte (a);
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
         return d;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       return 0xff00 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return 0xff00ff00 | mmdRbz (a + 1) << 16 | mmdRbz (a + 3);
     }
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           super.mmdWb (a, d);  //バスエラー
           return;
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.fdc;
       FDC.fdcWriteByte (a, d);
       if (false) {  //X68030
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       mmdWb (a + 1, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWb (a + 1, d >> 16);
       mmdWb (a + 3, d);
     }
  },  //MMD_FDC_00
  MMD_FDC_03 {  //Xellent30の030モード
    @Override public String toString () {
      return "Floppy Disk Controller (03)";
    }
    @Override protected int mmdPbz (int a) {
      return FDC.fdcPeekByte (a);
    }
    @Override protected int mmdRbz (int a) throws M68kException {
       if (false) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           return super.mmdRbz (a);  //バスエラー
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.fdc;
       if (true) {  //X68000
         return FDC.fdcReadByte (a);
       } else {  //X68030
         int d = FDC.fdcReadByte (a);
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
         return d;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       return 0xff00 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return 0xff00ff00 | mmdRbz (a + 1) << 16 | mmdRbz (a + 3);
     }
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if (false) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           super.mmdWb (a, d);  //バスエラー
           return;
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.fdc;
       FDC.fdcWriteByte (a, d);
       if (false) {  //X68030
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       mmdWb (a + 1, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWb (a + 1, d >> 16);
       mmdWb (a + 3, d);
     }
  },  //MMD_FDC_03
  MMD_FDC_30 {  //X68030
    @Override public String toString () {
      return "Floppy Disk Controller (30)";
    }
    @Override protected int mmdPbz (int a) {
      return FDC.fdcPeekByte (a);
    }
    @Override protected int mmdRbz (int a) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           return super.mmdRbz (a);  //バスエラー
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.fdc;
       if (false) {  //X68000
         return FDC.fdcReadByte (a);
       } else {  //X68030
         int d = FDC.fdcReadByte (a);
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
         return d;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       return 0xff00 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return 0xff00ff00 | mmdRbz (a + 1) << 16 | mmdRbz (a + 3);
     }
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           super.mmdWb (a, d);  //バスエラー
           return;
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.fdc;
       FDC.fdcWriteByte (a, d);
       if (true) {  //X68030
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       mmdWb (a + 1, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWb (a + 1, d >> 16);
       mmdWb (a + 3, d);
     }
  },  //MMD_FDC_30

  //--------------------------------------------------------------------------------
  //MMD_HDC SASI HDコントローラ
  //
  //  $00E96000～$00E9603F(偶数)  バイトリードはバスエラー、バイトライトはバスエラー、ワードリードは$FFxx、ワードライトは$??xx
  //  $00E96000～$00E9603F(奇数)  バイトリードは$xx、バイトライトは$xx
  //                              X68030の$00E96000～$00E9601Fは$00,$00,$FF,$FFの繰り返し。$00E9603Fは$FF
  //  $00E96040～$00E97FFF  $00E96000～$00E9603Fの繰り返し
  //
  MMD_HDC_00 {  //X68000
    @Override public String toString () {
      return "Hard Disk Controller (00)";
    }
    @Override protected int mmdPbz (int a) {
      return HDC.hdcPeekByte (a);
    }
    @Override protected int mmdRbz (int a) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           return super.mmdRbz (a);  //バスエラー
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.hdc;
       if (true) {  //X68000
         return HDC.hdcReadByte (a);
       } else {  //X68030
         int d = HDC.hdcReadByte (a);
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
         return d;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       return 0xff00 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return 0xff00ff00 | mmdRbz (a + 1) << 16 | mmdRbz (a + 3);
     }
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           super.mmdWb (a, d);  //バスエラー
           return;
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.hdc;
       HDC.hdcWriteByte (a, d);
       if (false) {  //X68030
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       mmdWb (a + 1, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWb (a + 1, d >> 16);
       mmdWb (a + 3, d);
     }
  },  //MMD_HDC_00
  MMD_HDC_03 {  //Xellent30の030モード
    @Override public String toString () {
      return "Hard Disk Controller (03)";
    }
    @Override protected int mmdPbz (int a) {
      return HDC.hdcPeekByte (a);
    }
    @Override protected int mmdRbz (int a) throws M68kException {
       if (false) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           return super.mmdRbz (a);  //バスエラー
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.hdc;
       if (true) {  //X68000
         return HDC.hdcReadByte (a);
       } else {  //X68030
         int d = HDC.hdcReadByte (a);
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
         return d;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       return 0xff00 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return 0xff00ff00 | mmdRbz (a + 1) << 16 | mmdRbz (a + 3);
     }
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if (false) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           super.mmdWb (a, d);  //バスエラー
           return;
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.hdc;
       HDC.hdcWriteByte (a, d);
       if (false) {  //X68030
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       mmdWb (a + 1, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWb (a + 1, d >> 16);
       mmdWb (a + 3, d);
     }
  },  //MMD_HDC_03
  MMD_HDC_30 {  //X68030
    @Override public String toString () {
      return "Hard Disk Controller (30)";
    }
    @Override protected int mmdPbz (int a) {
      return HDC.hdcPeekByte (a);
    }
    @Override protected int mmdRbz (int a) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           return super.mmdRbz (a);  //バスエラー
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.hdc;
       if (false) {  //X68000
         return HDC.hdcReadByte (a);
       } else {  //X68030
         int d = HDC.hdcReadByte (a);
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
         return d;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       return 0xff00 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return 0xff00ff00 | mmdRbz (a + 1) << 16 | mmdRbz (a + 3);
     }
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           super.mmdWb (a, d);  //バスエラー
           return;
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.hdc;
       HDC.hdcWriteByte (a, d);
       if (true) {  //X68030
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       mmdWb (a + 1, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWb (a + 1, d >> 16);
       mmdWb (a + 3, d);
     }
  },  //MMD_HDC_30

  //--------------------------------------------------------------------------------
  //MMD_SCC SCC
  //
  //  $00E98000～$00E98007(偶数)  バイトリードはバスエラー、バイトライトはバスエラー、ワードリードは$FFxx、ワードライトは$??xx
  //  $00E98000～$00E98007(奇数)  バイトリードは$xx、バイトライトは$xx
  //  $00E98008～$00E99FFF  $00E98000～$00E98007の繰り返し
  //
  MMD_SCC_00 {  //X68000
    @Override public String toString () {
      return "Serial Communication Controller (00)";
    }
    @Override protected int mmdPbz (int a) {
      return Z8530.sccReadByte (a, true);
    }
    @Override protected int mmdRbz (int a) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           return super.mmdRbz (a);  //バスエラー
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.scc;
       if (true) {  //X68000
         return Z8530.sccReadByte (a, false);
       } else {  //X68030
         int d = Z8530.sccReadByte (a, false);
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
         return d;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       return 0xff00 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return 0xff00ff00 | mmdRbz (a + 1) << 16 | mmdRbz (a + 3);
     }
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           super.mmdWb (a, d);  //バスエラー
           return;
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.scc;
       Z8530.sccWriteByte (a, d);
       if (false) {  //X68030
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       mmdWb (a + 1, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWb (a + 1, d >> 16);
       mmdWb (a + 3, d);
     }
  },  //MMD_SCC_00
  MMD_SCC_03 {  //Xellent30の030モード
    @Override public String toString () {
      return "Serial Communication Controller (03)";
    }
    @Override protected int mmdPbz (int a) {
      return Z8530.sccReadByte (a, true);
    }
    @Override protected int mmdRbz (int a) throws M68kException {
       if (false) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           return super.mmdRbz (a);  //バスエラー
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.scc;
       if (true) {  //X68000
         return Z8530.sccReadByte (a, false);
       } else {  //X68030
         int d = Z8530.sccReadByte (a, false);
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
         return d;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       return 0xff00 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return 0xff00ff00 | mmdRbz (a + 1) << 16 | mmdRbz (a + 3);
     }
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if (false) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           super.mmdWb (a, d);  //バスエラー
           return;
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.scc;
       Z8530.sccWriteByte (a, d);
       if (false) {  //X68030
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       mmdWb (a + 1, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWb (a + 1, d >> 16);
       mmdWb (a + 3, d);
     }
  },  //MMD_SCC_03
  MMD_SCC_30 {  //X68030
    @Override public String toString () {
      return "Serial Communication Controller (30)";
    }
    @Override protected int mmdPbz (int a) {
      return Z8530.sccReadByte (a, true);
    }
    @Override protected int mmdRbz (int a) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           return super.mmdRbz (a);  //バスエラー
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.scc;
       if (false) {  //X68000
         return Z8530.sccReadByte (a, false);
       } else {  //X68030
         int d = Z8530.sccReadByte (a, false);
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
         return d;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       return 0xff00 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return 0xff00ff00 | mmdRbz (a + 1) << 16 | mmdRbz (a + 3);
     }
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           super.mmdWb (a, d);  //バスエラー
           return;
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.scc;
       Z8530.sccWriteByte (a, d);
       if (true) {  //X68030
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       mmdWb (a + 1, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWb (a + 1, d >> 16);
       mmdWb (a + 3, d);
     }
  },  //MMD_SCC_30

  //--------------------------------------------------------------------------------
  //MMD_PPI PPI
  //
  //  $00E9A000～$00E9A007(偶数)  バイトリードはバスエラー、バイトライトはバスエラー、ワードリードは$FFxx、ワードライトは$??xx
  //  $00E9A000～$00E9A007(奇数)  バイトリードは$xx、バイトライトは$xx
  //  $00E9A008～$00E9BFFF  $00E9A000～$00E9A007の繰り返し
  //
  MMD_PPI_00 {  //X68000
    @Override public String toString () {
      return "Programmable Peripheral Interface (00)";
    }
    @Override protected int mmdRbz (int a) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           return super.mmdRbz (a);  //バスエラー
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.ppi;
       if (true) {  //X68000
         return PPI.ppiReadByte (a);
       } else {  //X68030
         int d = PPI.ppiReadByte (a);
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
         return d;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       return 0xff00 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return 0xff00ff00 | mmdRbz (a + 1) << 16 | mmdRbz (a + 3);
     }
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           super.mmdWb (a, d);  //バスエラー
           return;
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.ppi;
       PPI.ppiWriteByte (a, d);
       if (false) {  //X68030
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       mmdWb (a + 1, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWb (a + 1, d >> 16);
       mmdWb (a + 3, d);
     }
  },  //MMD_PPI_00
  MMD_PPI_03 {  //Xellent30の030モード
    @Override public String toString () {
      return "Programmable Peripheral Interface (03)";
    }
    @Override protected int mmdRbz (int a) throws M68kException {
       if (false) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           return super.mmdRbz (a);  //バスエラー
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.ppi;
       if (true) {  //X68000
         return PPI.ppiReadByte (a);
       } else {  //X68030
         int d = PPI.ppiReadByte (a);
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
         return d;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       return 0xff00 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return 0xff00ff00 | mmdRbz (a + 1) << 16 | mmdRbz (a + 3);
     }
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if (false) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           super.mmdWb (a, d);  //バスエラー
           return;
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.ppi;
       PPI.ppiWriteByte (a, d);
       if (false) {  //X68030
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       mmdWb (a + 1, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWb (a + 1, d >> 16);
       mmdWb (a + 3, d);
     }
  },  //MMD_PPI_03
  MMD_PPI_30 {  //X68030
    @Override public String toString () {
      return "Programmable Peripheral Interface (30)";
    }
    @Override protected int mmdRbz (int a) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           return super.mmdRbz (a);  //バスエラー
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.ppi;
       if (false) {  //X68000
         return PPI.ppiReadByte (a);
       } else {  //X68030
         int d = PPI.ppiReadByte (a);
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
         return d;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       return 0xff00 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return 0xff00ff00 | mmdRbz (a + 1) << 16 | mmdRbz (a + 3);
     }
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           super.mmdWb (a, d);  //バスエラー
           return;
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.ppi;
       PPI.ppiWriteByte (a, d);
       if (true) {  //X68030
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       mmdWb (a + 1, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWb (a + 1, d >> 16);
       mmdWb (a + 3, d);
     }
  },  //MMD_PPI_30

  //--------------------------------------------------------------------------------
  //MMD_IOI I/O割り込み
  //
  //  $00E9C000～$00E9C00F(偶数)  バイトリードはバスエラー、バイトライトはバスエラー、ワードリードは$FFxx、ワードライトは$??xx
  //  $00E9C000～$00E9C00F(奇数)  バイトリードは$xx、バイトライトは$xx
  //  $00E9C010～$00E9BFFF  $00E9C000～$00E9C00Fの繰り返し
  //
  MMD_IOI_00 {  //X68000
    @Override public String toString () {
      return "I/O Interrupt (00)";
    }
    @Override protected int mmdRbz (int a) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           return super.mmdRbz (a);  //バスエラー
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.ioi;
       if (true) {  //X68000
         return IOInterrupt.ioiReadByte (a);
       } else {  //X68030
         int d = IOInterrupt.ioiReadByte (a);
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
         return d;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       return 0xff00 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return 0xff00ff00 | mmdRbz (a + 1) << 16 | mmdRbz (a + 3);
     }
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           super.mmdWb (a, d);  //バスエラー
           return;
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.ioi;
       IOInterrupt.ioiWriteByte (a, d);
       if (false) {  //X68030
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       mmdWb (a + 1, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWb (a + 1, d >> 16);
       mmdWb (a + 3, d);
     }
  },  //MMD_IOI_00
  MMD_IOI_03 {  //Xellent30の030モード
    @Override public String toString () {
      return "I/O Interrupt (03)";
    }
    @Override protected int mmdRbz (int a) throws M68kException {
       if (false) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           return super.mmdRbz (a);  //バスエラー
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.ioi;
       if (true) {  //X68000
         return IOInterrupt.ioiReadByte (a);
       } else {  //X68030
         int d = IOInterrupt.ioiReadByte (a);
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
         return d;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       return 0xff00 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return 0xff00ff00 | mmdRbz (a + 1) << 16 | mmdRbz (a + 3);
     }
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if (false) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           super.mmdWb (a, d);  //バスエラー
           return;
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.ioi;
       IOInterrupt.ioiWriteByte (a, d);
       if (false) {  //X68030
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       mmdWb (a + 1, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWb (a + 1, d >> 16);
       mmdWb (a + 3, d);
     }
  },  //MMD_IOI_03
  MMD_IOI_30 {  //X68030
    @Override public String toString () {
      return "I/O Interrupt (30)";
    }
    @Override protected int mmdRbz (int a) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           return super.mmdRbz (a);  //バスエラー
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.ioi;
       if (false) {  //X68000
         return IOInterrupt.ioiReadByte (a);
       } else {  //X68030
         int d = IOInterrupt.ioiReadByte (a);
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
         return d;
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       return 0xff00 | mmdRbz (a + 1);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return 0xff00ff00 | mmdRbz (a + 1) << 16 | mmdRbz (a + 3);
     }
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if (true) {  //Xellent30の030モード以外
         if ((a & 1) == 0) {  //偶数アドレス
           super.mmdWb (a, d);  //バスエラー
           return;
         }
       }
       XEiJ.mpuClockTime += XEiJ.busWaitTime.ioi;
       IOInterrupt.ioiWriteByte (a, d);
       if (true) {  //X68030
         MC68901.mfpIoldsTransition ();  //IOLDSの遷移
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       mmdWb (a + 1, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWb (a + 1, d >> 16);
       mmdWb (a + 3, d);
     }
  },  //MMD_IOI_30

  //--------------------------------------------------------------------------------
  //MMD_XB1 拡張ボード領域1
  MMD_XB1 {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "拡張ボード領域 1" : "Expansion Board Area 1";
    }
    //ピーク
    @Override protected int mmdPbz (int a) {
      if ((a & (XEiJ.BUS_MOTHER_MASK & -0x20)) == 0x00e9e000) {  //数値演算プロセッサボード1
        if ((7 & XEiJ.currentCopro1) != 0) {
          return XEiJ.fpuCoproboard1.cirPeekByteZero (a);
        } else {
          return 0xff;
        }
      }
      if ((a & (XEiJ.BUS_MOTHER_MASK & -0x20)) == 0x00e9e080) {  //数値演算プロセッサボード2
        if ((7 & XEiJ.currentCopro2) != 0) {
          return XEiJ.fpuCoproboard2.cirPeekByteZero (a);
        } else {
          return 0xff;
        }
      }
      if ((a & (XEiJ.BUS_MOTHER_MASK & -HFS.HFS_ROM_SIZE)) == HFS.HFS_ADDRESS) {  //ホストファイルシステムインタフェイス
        a &= XEiJ.BUS_MOTHER_MASK;
        return MainMemory.mmrM8[a] & 0xff;
      }
      if (Keyboard.KBD_ZKEY_ON &&
          (a & (XEiJ.BUS_MOTHER_MASK & -Keyboard.KBD_ZKEY_IO_SIZE)) == Keyboard.KBD_ZKEY_IO_ADDRESS) {  //Zキーボード
        return Keyboard.kbdZKeyIOReadByte (a);
      }
      return 0xff;
    }
    @Override protected int mmdPwz (int a) {
      if ((a & (XEiJ.BUS_MOTHER_MASK & -0x20)) == 0x00e9e000) {  //数値演算プロセッサボード1
        if ((7 & XEiJ.currentCopro1) != 0) {
          return XEiJ.fpuCoproboard1.cirPeekWordZero (a);
        } else {
          return 0xffff;
        }
      }
      if ((a & (XEiJ.BUS_MOTHER_MASK & -0x20)) == 0x00e9e080) {  //数値演算プロセッサボード2
        if ((7 & XEiJ.currentCopro2) != 0) {
          return XEiJ.fpuCoproboard2.cirPeekWordZero (a);
        } else {
          return 0xffff;
        }
      }
      if ((a & (XEiJ.BUS_MOTHER_MASK & -HFS.HFS_ROM_SIZE)) == HFS.HFS_ADDRESS) {  //ホストファイルシステムインタフェイス
        a &= XEiJ.BUS_MOTHER_MASK;
        return (char) (MainMemory.mmrM8[a] << 8 | (MainMemory.mmrM8[a + 1] & 0xff));
      }
      if (Keyboard.KBD_ZKEY_ON &&
          (a & (XEiJ.BUS_MOTHER_MASK & -Keyboard.KBD_ZKEY_IO_SIZE)) == Keyboard.KBD_ZKEY_IO_ADDRESS) {  //Zキーボード
        return Keyboard.kbdZKeyIOReadWord (a);
      }
      return 0xffff;
    }
    @Override protected int mmdPls (int a) {
      if ((a & (XEiJ.BUS_MOTHER_MASK & -0x20)) == 0x00e9e000) {  //数値演算プロセッサボード1
        if ((7 & XEiJ.currentCopro1) != 0) {
          return XEiJ.fpuCoproboard1.cirPeekLong (a);
        } else {
          return -1;
        }
      }
      if ((a & (XEiJ.BUS_MOTHER_MASK & -0x20)) == 0x00e9e080) {  //数値演算プロセッサボード2
        if ((7 & XEiJ.currentCopro2) != 0) {
          return XEiJ.fpuCoproboard2.cirPeekLong (a);
        } else {
          return -1;
        }
      }
      if ((a & (XEiJ.BUS_MOTHER_MASK & -HFS.HFS_ROM_SIZE)) == HFS.HFS_ADDRESS) {  //ホストファイルシステムインタフェイス
        a &= XEiJ.BUS_MOTHER_MASK;
        return MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 0xff) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | (MainMemory.mmrM8[a + 3] & 0xff));
      }
      if (Keyboard.KBD_ZKEY_ON &&
          (a & (XEiJ.BUS_MOTHER_MASK & -Keyboard.KBD_ZKEY_IO_SIZE)) == Keyboard.KBD_ZKEY_IO_ADDRESS) {  //Zキーボード
        return Keyboard.kbdZKeyIOReadLong (a);
      }
      return -1;
    }
    //リード
    @Override protected int mmdRbz (int a) throws M68kException {
       if ((a & (XEiJ.BUS_MOTHER_MASK & -0x20)) == 0x00e9e000) {  //数値演算プロセッサボード1
         if ((7 & XEiJ.currentCopro1) != 0) {
           return XEiJ.fpuCoproboard1.cirReadByteZero (a);
         } else {
           return super.mmdRbz (a);  //バスエラー
         }
       }
       if ((a & (XEiJ.BUS_MOTHER_MASK & -0x20)) == 0x00e9e080) {  //数値演算プロセッサボード2
         if ((7 & XEiJ.currentCopro2) != 0) {
           return XEiJ.fpuCoproboard2.cirReadByteZero (a);
         } else {
           return super.mmdRbz (a);  //バスエラー
         }
       }
       if ((a & (XEiJ.BUS_MOTHER_MASK & -HFS.HFS_ROM_SIZE)) == HFS.HFS_ADDRESS) {  //ホストファイルシステムインタフェイス
         a &= XEiJ.BUS_MOTHER_MASK;
         return MainMemory.mmrM8[a] & 0xff;
       }
       if (Keyboard.KBD_ZKEY_ON &&
           (a & (XEiJ.BUS_MOTHER_MASK & -Keyboard.KBD_ZKEY_IO_SIZE)) == Keyboard.KBD_ZKEY_IO_ADDRESS) {  //Zキーボード
         return Keyboard.kbdZKeyIOReadByte (a);
       }
       return super.mmdRbz (a);  //バスエラー
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       if ((a & (XEiJ.BUS_MOTHER_MASK & -0x20)) == 0x00e9e000) {  //数値演算プロセッサボード1
         if ((7 & XEiJ.currentCopro1) != 0) {
           return XEiJ.fpuCoproboard1.cirReadWordZero (a);
         } else {
           return super.mmdRwz (a);  //バスエラー
         }
       }
       if ((a & (XEiJ.BUS_MOTHER_MASK & -0x20)) == 0x00e9e080) {  //数値演算プロセッサボード2
         if ((7 & XEiJ.currentCopro2) != 0) {
           return XEiJ.fpuCoproboard2.cirReadWordZero (a);
         } else {
           return super.mmdRwz (a);  //バスエラー
         }
       }
       if ((a & (XEiJ.BUS_MOTHER_MASK & -HFS.HFS_ROM_SIZE)) == HFS.HFS_ADDRESS) {  //ホストファイルシステムインタフェイス
         a &= XEiJ.BUS_MOTHER_MASK;
         return (char) (MainMemory.mmrM8[a] << 8 | (MainMemory.mmrM8[a + 1] & 0xff));
       }
       if (Keyboard.KBD_ZKEY_ON &&
           (a & (XEiJ.BUS_MOTHER_MASK & -Keyboard.KBD_ZKEY_IO_SIZE)) == Keyboard.KBD_ZKEY_IO_ADDRESS) {  //Zキーボード
         return Keyboard.kbdZKeyIOReadWord (a);
       }
       return super.mmdRwz (a);  //バスエラー
     }
    @Override protected int mmdRls (int a) throws M68kException {
       if ((a & (XEiJ.BUS_MOTHER_MASK & -0x20)) == 0x00e9e000) {  //数値演算プロセッサボード1
         if ((7 & XEiJ.currentCopro1) != 0) {
           return XEiJ.fpuCoproboard1.cirReadLong (a);
         } else {
           return super.mmdRls (a);  //バスエラー
         }
       }
       if ((a & (XEiJ.BUS_MOTHER_MASK & -0x20)) == 0x00e9e080) {  //数値演算プロセッサボード2
         if ((7 & XEiJ.currentCopro2) != 0) {
           return XEiJ.fpuCoproboard2.cirReadLong (a);
         } else {
           return super.mmdRls (a);  //バスエラー
         }
       }
       if ((a & (XEiJ.BUS_MOTHER_MASK & -HFS.HFS_ROM_SIZE)) == HFS.HFS_ADDRESS) {  //ホストファイルシステムインタフェイス
         a &= XEiJ.BUS_MOTHER_MASK;
         return MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 0xff) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | (MainMemory.mmrM8[a + 3] & 0xff));
       }
       if (Keyboard.KBD_ZKEY_ON &&
           (a & (XEiJ.BUS_MOTHER_MASK & -Keyboard.KBD_ZKEY_IO_SIZE)) == Keyboard.KBD_ZKEY_IO_ADDRESS) {  //Zキーボード
         return Keyboard.kbdZKeyIOReadLong (a);
       }
       return super.mmdRls (a);  //バスエラー
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if ((a & (XEiJ.BUS_MOTHER_MASK & -0x20)) == 0x00e9e000) {  //数値演算プロセッサボード1
         if ((7 & XEiJ.currentCopro1) != 0) {
           XEiJ.fpuCoproboard1.cirWriteByte (a, d);
           return;
         } else {
           super.mmdWb (a, d);  //バスエラー
           return;
         }
       }
       if ((a & (XEiJ.BUS_MOTHER_MASK & -0x20)) == 0x00e9e080) {  //数値演算プロセッサボード2
         if ((7 & XEiJ.currentCopro2) != 0) {
           XEiJ.fpuCoproboard2.cirWriteByte (a, d);
           return;
         } else {
           super.mmdWb (a, d);  //バスエラー
           return;
         }
       }
       if (Keyboard.KBD_ZKEY_ON &&
           (a & (XEiJ.BUS_MOTHER_MASK & -Keyboard.KBD_ZKEY_IO_SIZE)) == Keyboard.KBD_ZKEY_IO_ADDRESS) {  //Zキーボード
         Keyboard.kbdZKeyIOWriteByte (a, d);
         return;
       }
       super.mmdWb (a, d);  //バスエラー
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       if ((a & (XEiJ.BUS_MOTHER_MASK & -0x20)) == 0x00e9e000) {  //数値演算プロセッサボード1
         if ((7 & XEiJ.currentCopro1) != 0) {
           XEiJ.fpuCoproboard1.cirWriteWord (a, d);
           return;
         } else {
           super.mmdWw (a, d);  //バスエラー
           return;
         }
       }
       if ((a & (XEiJ.BUS_MOTHER_MASK & -0x20)) == 0x00e9e080) {  //数値演算プロセッサボード2
         if ((7 & XEiJ.currentCopro2) != 0) {
           XEiJ.fpuCoproboard2.cirWriteWord (a, d);
           return;
         } else {
           super.mmdWw (a, d);  //バスエラー
           return;
         }
       }
       if (Keyboard.KBD_ZKEY_ON &&
           (a & (XEiJ.BUS_MOTHER_MASK & -Keyboard.KBD_ZKEY_IO_SIZE)) == Keyboard.KBD_ZKEY_IO_ADDRESS) {  //Zキーボード
         Keyboard.kbdZKeyIOWriteWord (a, d);
         return;
       }
       super.mmdWw (a, d);  //バスエラー
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       if ((a & (XEiJ.BUS_MOTHER_MASK & -0x20)) == 0x00e9e000) {  //数値演算プロセッサボード1
         if ((7 & XEiJ.currentCopro1) != 0) {
           XEiJ.fpuCoproboard1.cirWriteLong (a, d);
           return;
         } else {
           super.mmdWl (a, d);  //バスエラー
           return;
         }
       }
       if ((a & (XEiJ.BUS_MOTHER_MASK & -0x20)) == 0x00e9e080) {  //数値演算プロセッサボード2
         if ((7 & XEiJ.currentCopro2) != 0) {
           XEiJ.fpuCoproboard2.cirWriteLong (a, d);
           return;
         } else {
           super.mmdWl (a, d);  //バスエラー
           return;
         }
       }
       if (Keyboard.KBD_ZKEY_ON &&
           (a & (XEiJ.BUS_MOTHER_MASK & -Keyboard.KBD_ZKEY_IO_SIZE)) == Keyboard.KBD_ZKEY_IO_ADDRESS) {  //Zキーボード
         Keyboard.kbdZKeyIOWriteLong (a, d);
         return;
       }
       super.mmdWl (a, d);  //バスエラー
     }
  },  //MMD_XB1

  //--------------------------------------------------------------------------------
  //MMD_EXS 拡張SCSI
  //  必要なときだけ接続される
  //  拡張SCSIのROMのサイズは8KBなのでリードのときのバスエラーのチェックは不要
  //  ライトのときはROMには書き込めないのでSPCのレジスタでなければバスエラー
  MMD_EXS {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "拡張 SCSI ポート" : "Expansion SCSI Port";
    }
    //ピーク
    @Override protected int mmdPbz (int a) {
      if ((a & (-32 & XEiJ.BUS_MOTHER_MASK)) == SPC.SPC_BASE_EX) {  //レジスタ
        if ((a & 0x01) == 0) {  //偶数番地
          return 0xff;
        } else {  //奇数番地
          return SPC.spcSCSIEXChip.spiPeek (a);
        }
      } else {  //ROM
        return 0xff & MainMemory.mmrM8[a];
      }
    }
    @Override protected int mmdPwz (int a) {
      XEiJ.mpuClockTime += XEiJ.busWaitTime.hdc;  //!!!
      if ((a & (-32 & XEiJ.BUS_MOTHER_MASK)) == SPC.SPC_BASE_EX) {  //レジスタ
        return 0xff00 | SPC.spcSCSIEXChip.spiPeek (a + 1);
      } else {  //ROM
        return (0xff & MainMemory.mmrM8[a]) << 8 | (0xff & MainMemory.mmrM8[a + 1]);
      }
    }
    @Override protected int mmdPls (int a) {
      return mmdPwz (a) << 16 | mmdPwz (a + 2);
    }
    //リード
    @Override protected int mmdRbz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.hdc;  //!!!
       if ((a & (-32 & XEiJ.BUS_MOTHER_MASK)) == SPC.SPC_BASE_EX) {  //レジスタ
         if ((a & 0x01) == 0) {  //偶数番地
           if (XEiJ.currentIsSecond) {  //Xellent30の030モード
             return 0xff;
           }
           return super.mmdRbz (a);  //バスエラー
         } else {  //奇数番地
           return SPC.spcSCSIEXChip.spiRead (a);
         }
       } else {  //ROM
         return 0xff & MainMemory.mmrM8[a];
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.hdc;  //!!!
       if ((a & (-32 & XEiJ.BUS_MOTHER_MASK)) == SPC.SPC_BASE_EX) {  //レジスタ
         return 0xff00 | SPC.spcSCSIEXChip.spiRead (a + 1);
       } else {  //ROM
         return (0xff & MainMemory.mmrM8[a]) << 8 | (0xff & MainMemory.mmrM8[a + 1]);
       }
     }
    @Override protected int mmdRls (int a) throws M68kException {
       return mmdRwz (a) << 16 | mmdRwz (a + 2);
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.hdc;  //!!!
       if ((a & (-32 & XEiJ.BUS_MOTHER_MASK)) == SPC.SPC_BASE_EX) {  //レジスタ
         if ((a & 1) == 0) {  //偶数番地
           if (XEiJ.currentIsSecond) {  //Xellent30の030モード
             return;
           }
           super.mmdWb (a, d);  //バスエラー
         } else {  //奇数番地
           SPC.spcSCSIEXChip.spiWrite (a, d);
           return;
         }
       } else {  //ROM
         super.mmdWb (a, d);  //バスエラー
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.hdc;  //!!!
       if ((a & (-32 & XEiJ.BUS_MOTHER_MASK)) == SPC.SPC_BASE_EX) {  //レジスタ
         SPC.spcSCSIEXChip.spiWrite (a + 1, d);
         return;
       } else {  //ROM
         super.mmdWw (a, d);  //バスエラー
       }
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       mmdWw (a, d >> 16);
       mmdWw (a + 2, d);
     }
  },  //MMD_EXS

  //--------------------------------------------------------------------------------
  //MMD_XB2 拡張ボード領域2
  //
  //  $00EAFF80  リードはバスエラー、バイトライトはバスエラー、ワードライトは$00200000～$003FFFFFがあるとき$??xx、ないときバスエラー
  //  $00EAFF81  リードはバスエラー、ライトは$00200000～$003FFFFFがあるとき$xx、ないときバスエラー
  //  $00EAFF82  リードはバスエラー、バイトライトはバスエラー、ワードライトは$00400000～$005FFFFFがあるとき$??xx、ないときバスエラー
  //  $00EAFF83  リードはバスエラー、ライトは$00400000～$005FFFFFがあるとき$xx、ないときバスエラー
  //  $00EAFF84  リードはバスエラー、バイトライトはバスエラー、ワードライトは$00600000～$007FFFFFがあるとき$??xx、ないときバスエラー
  //  $00EAFF85  リードはバスエラー、ライトは$00600000～$007FFFFFがあるとき$xx、ないときバスエラー
  //  $00EAFF86  リードはバスエラー、バイトライトはバスエラー、ワードライトは$00800000～$009FFFFFがあるとき$??xx、ないときバスエラー
  //  $00EAFF87  リードはバスエラー、ライトは$00800000～$009FFFFFがあるとき$xx、ないときバスエラー
  //  $00EAFF88  リードはバスエラー、バイトライトはバスエラー、ワードライトは$00A00000～$00BFFFFFがあるとき$??xx、ないときバスエラー
  //  $00EAFF89  リードはバスエラー、ライトは$00A00000～$00BFFFFFがあるとき$xx、ないときバスエラー
  //  $00EAFF8A～$00EAFF8F  バスエラー
  //  $00EAFF90～$00EAFFFF  $00EAFF80～$00EAFF8Fの繰り返し
  //
  //  拡張
  //  $00EAFF7F  バンクメモリのページ番号
  //
  MMD_XB2 {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "拡張ボード領域 2" : "Expansion Board Area 2";
    }
    //リード
    @Override protected int mmdRbz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (0x00eaff81 <= a && a <= 0x00eaff89 && (a & 1) != 0) {  //スーパーバイザエリア設定ポート
         return MainMemory.mmrM8[a] & 0xff;  //読み出せるようにしておく(本来はライトオンリー)
       }
       if (XEiJ.bnkOn && a == 0x00eaff7f) {  //バンクメモリのページ番号
         return XEiJ.bnkPageStart >> 17;
       }
       return super.mmdRbz (a);  //バスエラー
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (0x00eaff81 <= a && a <= 0x00eaff89 && (a & 1) != 0) {  //スーパーバイザエリア設定ポート
         MainMemory.mmrM8[a] = (byte) d;  //読み出せるようにしておく(本来はライトオンリー)
         a = (a & 14) + 2 << 20;  //1,3,5,7,9→2,4,6,8,a
         for (int m = 1; m <= 128; m <<= 1) {
           if ((d & m) == 0) {  //ユーザエリア
             XEiJ.busUser ( MemoryMappedDevice.MMD_MMR, a, a + 0x00040000);
           } else {  //スーパーバイザエリア
             XEiJ.busSuper (MemoryMappedDevice.MMD_MMR, a, a + 0x00040000);
           }
           a += 0x00040000;
         }
         return;
       }
       if (XEiJ.bnkOn && a == 0x00eaff7f) {  //バンクメモリのページ番号
         XEiJ.bnkPageStart = (d & 0xff) << 17;
         return;
       }
       super.mmdWb (a, d);  //バスエラー
     }
  },  //MMD_XB2

  //--------------------------------------------------------------------------------
  //MMD_SPR スプライト画面
  //
  //  CRTCのR20の下位5ビットが%1xx1xでないとき
  //    $00EB0000-$00EB03FF  スプライトスクロールレジスタ(0～127)
  //    $00EB0400-$00EB07FF  $FF。256枚表示のときスプライトスクロールレジスタ(128～255)
  //    $00EB0800-$00EB0811  設定
  //    $00EB0812-$00EB7FFF  $FF
  //    $00EB8000-$00EBFFFF  スプライトPCG・テキストエリア
  //
  //  CRTCのR20の下位5ビットが%1xx1xのとき
  //    $00EB0000-$00EB03FF  バスエラー
  //    $00EB0400-$00EB07FF  バスエラー
  //    $00EB0800-$00EB0811  設定
  //    $00EB0812-$00EB7FFF  $FF
  //    $00EB8000-$00EBFFFF  バスエラー
  //
  //  メモ
  //    スプライトPCG・テキストエリアにバイトサイズで書き込むとデータが破壊される
  //
  MMD_SPR {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "スプライト画面" : "Sprite Screen";
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.sprc;
       return (byte) SpriteScreen.sprReadByte (a);
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.sprc;
       return SpriteScreen.sprReadByte (a);
     }
    @Override protected int mmdRws (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.sprc;
       return (short) SpriteScreen.sprReadWord (a);
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.sprc;
       return SpriteScreen.sprReadWord (a);
     }
    @Override protected int mmdRls (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.sprc * 2;
       return SpriteScreen.sprReadLong (a);
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.sprc;
       SpriteScreen.sprWriteByte (a, d);
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.sprc;
       SpriteScreen.sprWriteWord (a, d);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.sprc * 2;
       SpriteScreen.sprWriteLong (a, d);
     }
  },  //MMD_SPR

  //--------------------------------------------------------------------------------
  //MMD_XB3 拡張ボード領域3
  //  $00EC0000～$00ECFFFF  64KB
  //  0x00ec0000  ユーザI/Oエリア
  //  0x00ec0000  Awesome
  //  0x00ec0000  Xellent30
  //  0x00ecc000  Mercury
  //  0x00ece000  Neptune
  //  0x00ecf000  Venus-X/030
  MMD_XB3 {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "拡張ボード領域 3" : "Expansion Board Area 3";
    }
    //リード
    @Override protected int mmdRbz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (XEiJ.currentAccelerator == XEiJ.ACCELERATOR_XELLENT30 &&
           XEiJ.xt3PortAddress <= a && a < XEiJ.xt3PortAddress + 0x4000) {  //Xellent30設定ポート
         return (a & 1) == 0 ? 0 : XEiJ.xt3PortRead ();
       }
       if (MercuryUnit.MU4_ON && MercuryUnit.mu4On &&
           (a & 0x00ffff80) == 0x00ecc080) {  //Mercury-Unit V4
         return MercuryUnit.mu4ReadByte (a);
       }
       return super.mmdRbz (a);  //バスエラー
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (XEiJ.currentAccelerator == XEiJ.ACCELERATOR_XELLENT30 &&
           XEiJ.xt3PortAddress <= a && a < XEiJ.xt3PortAddress + 0x4000) {  //Xellent30設定ポート
         return XEiJ.xt3PortRead ();
       }
       if (MercuryUnit.MU4_ON && MercuryUnit.mu4On &&
           (a & 0x00ffff80) == 0x00ecc080) {  //Mercury-Unit V4
         return MercuryUnit.mu4ReadWord (a);
       }
       return super.mmdRwz (a);  //バスエラー
     }
    @Override protected int mmdRls (int a) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (XEiJ.currentAccelerator == XEiJ.ACCELERATOR_XELLENT30 &&
           XEiJ.xt3PortAddress <= a && a < XEiJ.xt3PortAddress + 0x4000) {  //Xellent30設定ポート
         return XEiJ.xt3PortRead () << 16 | XEiJ.xt3PortRead ();
       }
       if (MercuryUnit.MU4_ON && MercuryUnit.mu4On &&
           (a & 0x00ffff80) == 0x00ecc080) {  //Mercury-Unit V4
         return MercuryUnit.mu4ReadWord (a) << 16 | MercuryUnit.mu4ReadWord (a + 2);
       }
       return super.mmdRls (a);  //バスエラー
     }
    @Override protected void mmdWb (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (XEiJ.currentAccelerator == XEiJ.ACCELERATOR_XELLENT30 &&
           XEiJ.xt3PortAddress <= a && a < XEiJ.xt3PortAddress + 0x4000) {  //Xellent30設定ポート
         if ((a & 1) == 0) {
         } else {
           XEiJ.xt3PortWrite (d);
         }
         return;
       }
       if (MercuryUnit.MU4_ON && MercuryUnit.mu4On &&
           (a & 0x00ffff80) == 0x00ecc080) {  //Mercury-Unit V4
         MercuryUnit.mu4WriteByte (a, d);
         return;
       }
       super.mmdWb (a, d);  //バスエラー
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (XEiJ.currentAccelerator == XEiJ.ACCELERATOR_XELLENT30 &&
           XEiJ.xt3PortAddress <= a && a < XEiJ.xt3PortAddress + 0x4000) {  //Xellent30設定ポート
         XEiJ.xt3PortWrite (d);
         return;
       }
       if (MercuryUnit.MU4_ON && MercuryUnit.mu4On &&
           (a & 0x00ffff80) == 0x00ecc080) {  //Mercury-Unit V4
         MercuryUnit.mu4WriteWord (a, d);
         return;
       }
       super.mmdWw (a, d);  //バスエラー
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       a &= XEiJ.BUS_MOTHER_MASK;
       if (XEiJ.currentAccelerator == XEiJ.ACCELERATOR_XELLENT30 &&
           XEiJ.xt3PortAddress <= a && a < XEiJ.xt3PortAddress + 0x4000) {  //Xellent30設定ポート
         XEiJ.xt3PortWrite (d >> 16);
         XEiJ.xt3PortWrite (d);
         return;
       }
       if (MercuryUnit.MU4_ON && MercuryUnit.mu4On &&
           (a & 0x00ffff80) == 0x00ecc080) {  //Mercury-Unit V4
         MercuryUnit.mu4WriteWord (a, d >> 16);
         MercuryUnit.mu4WriteWord (a + 2, d);
         return;
       }
       super.mmdWl (a, d);  //バスエラー
     }
  },  //MMD_XB3

  //--------------------------------------------------------------------------------
  //MMD_XTM Xellent30のSRAM
  MMD_XTM {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "Xellent30 の SRAM" : "SRAM on Xellent30";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      a -= XEiJ.xt3MemoryPosition;
      return XEiJ.xt3MemoryArray[a];
    }
    @Override protected int mmdPbz (int a) {
      a -= XEiJ.xt3MemoryPosition;
      return XEiJ.xt3MemoryArray[a] & 0xff;
    }
    @Override protected int mmdPws (int a) {
      a -= XEiJ.xt3MemoryPosition;
      return XEiJ.xt3MemoryArray[a] << 8 | (XEiJ.xt3MemoryArray[a + 1] & 0xff);
    }
    @Override protected int mmdPwz (int a) {
      a -= XEiJ.xt3MemoryPosition;
      return (char) (XEiJ.xt3MemoryArray[a] << 8 | (XEiJ.xt3MemoryArray[a + 1] & 0xff));
    }
    @Override protected int mmdPls (int a) {
      a -= XEiJ.xt3MemoryPosition;
      return XEiJ.xt3MemoryArray[a] << 24 | (XEiJ.xt3MemoryArray[a + 1] & 0xff) << 16 | (char) (XEiJ.xt3MemoryArray[a + 2] << 8 | (XEiJ.xt3MemoryArray[a + 3] & 0xff));
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       a -= XEiJ.xt3MemoryPosition;
       return XEiJ.xt3MemoryArray[a];
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       a -= XEiJ.xt3MemoryPosition;
       return XEiJ.xt3MemoryArray[a] & 0xff;
     }
    @Override protected int mmdRws (int a) throws M68kException {
       a -= XEiJ.xt3MemoryPosition;
       return XEiJ.xt3MemoryArray[a] << 8 | (XEiJ.xt3MemoryArray[a + 1] & 0xff);
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       a -= XEiJ.xt3MemoryPosition;
       return (char) (XEiJ.xt3MemoryArray[a] << 8 | (XEiJ.xt3MemoryArray[a + 1] & 0xff));
     }
    @Override protected int mmdRls (int a) throws M68kException {
       a -= XEiJ.xt3MemoryPosition;
       return XEiJ.xt3MemoryArray[a] << 24 | (XEiJ.xt3MemoryArray[a + 1] & 0xff) << 16 | (char) (XEiJ.xt3MemoryArray[a + 2] << 8 | (XEiJ.xt3MemoryArray[a + 3] & 0xff));
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       a -= XEiJ.xt3MemoryPosition;
       XEiJ.xt3MemoryArray[a    ] = (byte)  d;
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       a -= XEiJ.xt3MemoryPosition;
       XEiJ.xt3MemoryArray[a    ] = (byte) (d >> 8);
       XEiJ.xt3MemoryArray[a + 1] = (byte)  d;
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       a -= XEiJ.xt3MemoryPosition;
       XEiJ.xt3MemoryArray[a    ] = (byte) (d >> 24);
       XEiJ.xt3MemoryArray[a + 1] = (byte) (d >> 16);
       XEiJ.xt3MemoryArray[a + 2] = (byte) (d >> 8);
       XEiJ.xt3MemoryArray[a + 3] = (byte)  d;
     }
  },  //MMD_XTM

  //--------------------------------------------------------------------------------
  //MMD_SMR SRAM
  MMD_SMR {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "SRAM" : "SRAM";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a];
    }
    @Override protected int mmdPbz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a] & 0xff;
    }
    @Override protected int mmdPws (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (MainMemory.MMR_USE_BYTE_BUFFER) {
        return MainMemory.mmrBuffer.getShort (a);
      } else {
        return MainMemory.mmrM8[a] << 8 | (MainMemory.mmrM8[a + 1] & 0xff);
      }
    }
    @Override protected int mmdPwz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (MainMemory.MMR_USE_BYTE_BUFFER) {
        return MainMemory.mmrBuffer.getChar (a);
      } else {
        return (char) (MainMemory.mmrM8[a] << 8 | (MainMemory.mmrM8[a + 1] & 0xff));
      }
    }
    @Override protected int mmdPls (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (MainMemory.MMR_USE_BYTE_BUFFER) {
        return MainMemory.mmrBuffer.getInt (a);
      } else {
        return MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 0xff) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | (MainMemory.mmrM8[a + 3] & 0xff));
      }
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.sram;
       a &= XEiJ.BUS_MOTHER_MASK;
       XEiJ.mpuClockTime += XEiJ.busWaitTime.sram;
       return MainMemory.mmrM8[a];
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.sram;
       a &= XEiJ.BUS_MOTHER_MASK;
       XEiJ.mpuClockTime += XEiJ.busWaitTime.sram;
       return MainMemory.mmrM8[a] & 0xff;
     }
    @Override protected int mmdRws (int a) throws M68kException {
       return (short) mmdRwz (a);
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.sram;
       a &= XEiJ.BUS_MOTHER_MASK;
       XEiJ.mpuClockTime += XEiJ.busWaitTime.sram;
       int d;
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         d = MainMemory.mmrBuffer.getChar (a);
       } else {
         d = (char) (MainMemory.mmrM8[a] << 8 | (MainMemory.mmrM8[a + 1] & 0xff));
       }
       return d;
     }
    @Override protected int mmdRls (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.sram * 2;
       a &= XEiJ.BUS_MOTHER_MASK;
       XEiJ.mpuClockTime += XEiJ.busWaitTime.sram * 2;
       int d;
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         d = MainMemory.mmrBuffer.getInt (a);
       } else {
         d = MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 0xff) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | (MainMemory.mmrM8[a + 3] & 0xff));
       }
       return d;
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.sram;
       if (SRAM.smrWriteEnableOn) {
         a &= XEiJ.BUS_MOTHER_MASK;
         XEiJ.mpuClockTime += XEiJ.busWaitTime.sram;
         MainMemory.mmrM8[a] = (byte) d;
         if (a == 0x00ed002b || a == 0x00ed0059) {  //キーボードの配列または字体が変化した
           Keyboard.kbdRepaint ();  //キーボードが表示されているときkbdImageを作り直して再描画する
         } else if (a == 0x00ed005a) {  //HDMAX
           if (0x00fe0000 <= XEiJ.regPC0 && XEiJ.regPC0 <= 0x00ffffff) {  //IPLROMがSRAMのHDMAXを書き換えようとしている 
             SRAM.smrOverride ();  //SRAMの設定を上書きする
           }
         }
       } else {
         System.out.printf ("%08X writing $%02X to $%08X in a write-protected state\n", XEiJ.regPC0, d & 0xff, a);
         if (SRAM.smrSRAMBusErrorOn) {
           super.mmdWb (a, d);  //バスエラー
         }
       }
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.sram;
       if (SRAM.smrWriteEnableOn) {
         a &= XEiJ.BUS_MOTHER_MASK;
         XEiJ.mpuClockTime += XEiJ.busWaitTime.sram;
         MainMemory.mmrM8[a    ] = (byte) (d >> 8);
         MainMemory.mmrM8[a + 1] = (byte)  d;
         if (a == (0x00ed002b & -2) || a == (0x00ed0059 & -2)) {  //キーボードの配列または字体が変化した
           Keyboard.kbdRepaint ();  //キーボードが表示されているときkbdImageを作り直して再描画する
         }
       } else {
         System.out.printf ("%08X writing $%04X to $%08X in a write-protected state\n", XEiJ.regPC0, d & 0xffff, a);
         if (SRAM.smrSRAMBusErrorOn) {
           super.mmdWw (a, d);  //バスエラー
         }
       }
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       if (SRAM.smrWriteEnableOn) {
         mmdWw (a    , d >> 16);
         mmdWw (a + 2, d      );
       } else {
         System.out.printf ("%08X writing $%08X to $%08X in a write-protected state\n", XEiJ.regPC0, d, a);
         if (SRAM.smrSRAMBusErrorOn) {
           super.mmdWl (a, d);  //バスエラー
         }
       }
     }
  },  //MMD_SMR

  //--------------------------------------------------------------------------------
  //MMD_XB4 拡張ボード領域4
  //  $00EE0000～$00EFFFFF  バンクメモリ
  MMD_XB4 {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "拡張ボード領域 4" : "Expansion Board Area 4";
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       if (XEiJ.bnkOn) {
         a &= XEiJ.BUS_MOTHER_MASK;
         a = XEiJ.bnkPageStart + (a - 0x00ee0000);
         return XEiJ.bnkMemory[a];
       }
       return super.mmdRbs (a);  //バスエラー
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       if (XEiJ.bnkOn) {
         a &= XEiJ.BUS_MOTHER_MASK;
         a = XEiJ.bnkPageStart + (a - 0x00ee0000);
         return XEiJ.bnkMemory[a] & 0xff;
       }
       return super.mmdRbz (a);  //バスエラー
     }
    @Override protected int mmdRws (int a) throws M68kException {
       if (XEiJ.bnkOn) {
         a &= XEiJ.BUS_MOTHER_MASK;
         a = XEiJ.bnkPageStart + (a - 0x00ee0000);
         return ((XEiJ.bnkMemory[a    ]       ) << 8 |
                 (XEiJ.bnkMemory[a + 1] & 0xff));
       }
       return super.mmdRws (a);  //バスエラー
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       if (XEiJ.bnkOn) {
         a &= XEiJ.BUS_MOTHER_MASK;
         a = XEiJ.bnkPageStart + (a - 0x00ee0000);
         return ((XEiJ.bnkMemory[a    ] & 0xff) << 8 |
                 (XEiJ.bnkMemory[a + 1] & 0xff));
       }
       return super.mmdRwz (a);  //バスエラー
     }
    @Override protected int mmdRls (int a) throws M68kException {
       if (XEiJ.bnkOn) {
         a &= XEiJ.BUS_MOTHER_MASK;
         a = XEiJ.bnkPageStart + (a - 0x00ee0000);
         return ((XEiJ.bnkMemory[a    ]       ) << 24 |
                 (XEiJ.bnkMemory[a + 1] & 0xff) << 16 |
                 (XEiJ.bnkMemory[a + 2] & 0xff) <<  8 |
                 (XEiJ.bnkMemory[a + 3] & 0xff));
       }
       return super.mmdRls (a);  //バスエラー
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       if (XEiJ.bnkOn) {
         a &= XEiJ.BUS_MOTHER_MASK;
         a = XEiJ.bnkPageStart + (a - 0x00ee0000);
         XEiJ.bnkMemory[a] = (byte) d;
         return;
       }
       super.mmdWb (a, d);  //バスエラー
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       if (XEiJ.bnkOn) {
         a &= XEiJ.BUS_MOTHER_MASK;
         a = XEiJ.bnkPageStart + (a - 0x00ee0000);
         XEiJ.bnkMemory[a    ] = (byte) (d >> 8);
         XEiJ.bnkMemory[a + 1] = (byte)  d;
         return;
       }
       super.mmdWw (a, d);  //バスエラー
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       if (XEiJ.bnkOn) {
         a &= XEiJ.BUS_MOTHER_MASK;
         a = XEiJ.bnkPageStart + (a - 0x00ee0000);
         XEiJ.bnkMemory[a    ] = (byte) (d >> 24);
         XEiJ.bnkMemory[a + 1] = (byte) (d >> 16);
         XEiJ.bnkMemory[a + 2] = (byte) (d >>  8);
         XEiJ.bnkMemory[a + 3] = (byte)  d;
         return;
       }
       super.mmdWl (a, d);  //バスエラー
     }
  },  //MMD_XB4

  //--------------------------------------------------------------------------------
  //MMD_ROM ROM
  MMD_ROM {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "ROM" : "ROM";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a];
    }
    @Override protected int mmdPbz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a] & 0xff;
    }
    @Override protected int mmdPws (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (MainMemory.MMR_USE_BYTE_BUFFER) {
        return MainMemory.mmrBuffer.getShort (a);
      } else {
        return MainMemory.mmrM8[a] << 8 | (MainMemory.mmrM8[a + 1] & 0xff);
      }
    }
    @Override protected int mmdPwz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (MainMemory.MMR_USE_BYTE_BUFFER) {
        return MainMemory.mmrBuffer.getChar (a);
      } else {
        return (char) (MainMemory.mmrM8[a] << 8 | (MainMemory.mmrM8[a + 1] & 0xff));
      }
    }
    @Override protected int mmdPls (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (MainMemory.MMR_USE_BYTE_BUFFER) {
        return MainMemory.mmrBuffer.getInt (a);
      } else {
        return MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 0xff) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | (MainMemory.mmrM8[a + 3] & 0xff));
      }
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.rom;
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a];
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.rom;
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a] & 0xff;
     }
    @Override protected int mmdRws (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.rom;
       a &= XEiJ.BUS_MOTHER_MASK;
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         return MainMemory.mmrBuffer.getShort (a);
       } else {
         return MainMemory.mmrM8[a] << 8 | (MainMemory.mmrM8[a + 1] & 0xff);
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.rom;
       a &= XEiJ.BUS_MOTHER_MASK;
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         return MainMemory.mmrBuffer.getChar (a);
       } else {
         return (char) (MainMemory.mmrM8[a] << 8 | (MainMemory.mmrM8[a + 1] & 0xff));
       }
     }
    @Override protected int mmdRls (int a) throws M68kException {
       XEiJ.mpuClockTime += XEiJ.busWaitTime.romlong;
       a &= XEiJ.BUS_MOTHER_MASK;
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         return MainMemory.mmrBuffer.getInt (a);
       } else {
         return MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 0xff) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | (MainMemory.mmrM8[a + 3] & 0xff));
       }
     }
  },  //MMD_ROM

  MMD_RO6 {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "ROM (68060)" : "ROM (68060)";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a];
    }
    @Override protected int mmdPbz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      return MainMemory.mmrM8[a] & 0xff;
    }
    @Override protected int mmdPws (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (MainMemory.MMR_USE_BYTE_BUFFER) {
        return MainMemory.mmrBuffer.getShort (a);
      } else {
        return MainMemory.mmrM8[a] << 8 | (MainMemory.mmrM8[a + 1] & 0xff);
      }
    }
    @Override protected int mmdPwz (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (MainMemory.MMR_USE_BYTE_BUFFER) {
        return MainMemory.mmrBuffer.getChar (a);
      } else {
        return (char) (MainMemory.mmrM8[a] << 8 | (MainMemory.mmrM8[a + 1] & 0xff));
      }
    }
    @Override protected int mmdPls (int a) {
      a &= XEiJ.BUS_MOTHER_MASK;
      if (MainMemory.MMR_USE_BYTE_BUFFER) {
        return MainMemory.mmrBuffer.getInt (a);
      } else {
        return MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 0xff) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | (MainMemory.mmrM8[a + 3] & 0xff));
      }
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       if (MC68060.CAT_ON &&
           !XEiJ.busWaitTime.isDma) {
         MC68060.catReadMainROM (a);
       } else {
         XEiJ.mpuClockTime += XEiJ.busWaitTime.rom;
       }
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a];
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       if (MC68060.CAT_ON &&
           !XEiJ.busWaitTime.isDma) {
         MC68060.catReadMainROM (a);
       } else {
         XEiJ.mpuClockTime += XEiJ.busWaitTime.rom;
       }
       a &= XEiJ.BUS_MOTHER_MASK;
       return MainMemory.mmrM8[a] & 0xff;
     }
    @Override protected int mmdRws (int a) throws M68kException {
       if (MC68060.CAT_ON &&
           !XEiJ.busWaitTime.isDma) {
         MC68060.catReadMainROM (a);
       } else {
         XEiJ.mpuClockTime += XEiJ.busWaitTime.rom;
       }
       a &= XEiJ.BUS_MOTHER_MASK;
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         return MainMemory.mmrBuffer.getShort (a);
       } else {
         return MainMemory.mmrM8[a] << 8 | (MainMemory.mmrM8[a + 1] & 0xff);
       }
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       if (MC68060.CAT_ON &&
           !XEiJ.busWaitTime.isDma) {
         MC68060.catReadMainROM (a);
       } else {
         XEiJ.mpuClockTime += XEiJ.busWaitTime.rom;
       }
       a &= XEiJ.BUS_MOTHER_MASK;
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         return MainMemory.mmrBuffer.getChar (a);
       } else {
         return (char) (MainMemory.mmrM8[a] << 8 | (MainMemory.mmrM8[a + 1] & 0xff));
       }
     }
    @Override protected int mmdRls (int a) throws M68kException {
       if (MC68060.CAT_ON &&
           !XEiJ.busWaitTime.isDma) {
         MC68060.catReadMainROM (a);
       } else {
         XEiJ.mpuClockTime += XEiJ.busWaitTime.romlong;
       }
       a &= XEiJ.BUS_MOTHER_MASK;
       if (MainMemory.MMR_USE_BYTE_BUFFER) {
         return MainMemory.mmrBuffer.getInt (a);
       } else {
         return MainMemory.mmrM8[a] << 24 | (MainMemory.mmrM8[a + 1] & 0xff) << 16 | (char) (MainMemory.mmrM8[a + 2] << 8 | (MainMemory.mmrM8[a + 3] & 0xff));
       }
     }
  },  //MMD_RO6

  //--------------------------------------------------------------------------------
  //MMD_IBP 命令ブレークポイント
  MMD_IBP {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "命令ブレークポイント" : "Instruction Break Point";
    }
    @Override protected int mmdRwz (int a) throws M68kException {
       if (InstructionBreakPoint.IBP_ON) {
         InstructionBreakPoint.InstructionBreakRecord[] hashTable = InstructionBreakPoint.ibpHashTable;
         for (InstructionBreakPoint.InstructionBreakRecord r = hashTable[a >> 1 & InstructionBreakPoint.IBP_HASH_MASK];
              r != null;
              r = r.ibrNext) {  //同じ物理ハッシュコードを持つ命令ブレークポイントについて
           if (r.ibrPhysicalAddress == a) {  //命令ブレークポイントが設定されているとき
             if (r.ibrValue == r.ibrTarget) {  //現在値が目標値と一致しているとき
               if (r.ibrThreshold < 0) {  //インスタントのとき
                 InstructionBreakPoint.ibpRemove (r.ibrLogicalAddress, XEiJ.regSRS);  //取り除く
                 XEiJ.mpuContinue = true;  //ステップ実行を継続する
               } else if (r.ibrTarget < r.ibrThreshold) {  //インスタント化しているとき
                 r.ibrTarget = r.ibrThreshold;  //目標値を閾値に戻す
                 XEiJ.mpuContinue = true;  //ステップ実行を継続する
               } else {  //インスタントでなくインスタント化していないとき
                 if (r.ibrScriptElement != null &&  //スクリプトが指定されていて
                     r.ibrScriptElement.exlEval (ExpressionEvaluator.EVM_EXPRESSION).exlFloatValue.iszero ()) {  //条件が成立していないとき
                   break;  //続行する
                 }
                 r.ibrTarget++;  //目標値を増やす
                 XEiJ.mpuContinue = false;  //ステップ実行を継続しない
               }
               M68kException.m6eNumber = M68kException.M6E_INSTRUCTION_BREAK_POINT;  //命令ブレークポイントによる停止。XEiJ.mpuContinueを設定すること
               throw M68kException.m6eSignal;  //停止する
             } else {  //現在値が目標値と一致していないとき
               if (r.ibrWaitInstruction != null) {  //待機ポイントがあるとき
                 WaitInstruction.instruction = r.ibrWaitInstruction;  //待機命令を設定して
                 return XEiJ.EMX_OPCODE_EMXWAIT;  //返す
               }
               r.ibrValue++;  //現在値を増やす
               break;  //続行する
             }
           }
         }
       }
       if (DataBreakPoint.DBP_ON) {
         return DataBreakPoint.dbpMemoryMap[a >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a);
       } else {
         return XEiJ.busMemoryMap[a >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a);
       }
     }
  },  //MMD_IBP

  //--------------------------------------------------------------------------------
  //MMD_DBP データブレークポイント
  MMD_DBP {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "データブレークポイント" : "Data Break Point";
    }
    //ピーク
    @Override protected byte mmdPbs (int a) {
      return (XEiJ.regSRS != 0 ? XEiJ.busSuperMap : XEiJ.busUserMap)[a >>> XEiJ.BUS_PAGE_BITS].mmdPbs (a);
    }
    @Override protected int mmdPbz (int a) {
      return (XEiJ.regSRS != 0 ? XEiJ.busSuperMap : XEiJ.busUserMap)[a >>> XEiJ.BUS_PAGE_BITS].mmdPbz (a);
    }
    @Override protected int mmdPws (int a) {
      return (XEiJ.regSRS != 0 ? XEiJ.busSuperMap : XEiJ.busUserMap)[a >>> XEiJ.BUS_PAGE_BITS].mmdPws (a);
    }
    @Override protected int mmdPwz (int a) {
      return (XEiJ.regSRS != 0 ? XEiJ.busSuperMap : XEiJ.busUserMap)[a >>> XEiJ.BUS_PAGE_BITS].mmdPwz (a);
    }
    @Override protected int mmdPls (int a) {
      return (XEiJ.regSRS != 0 ? XEiJ.busSuperMap : XEiJ.busUserMap)[a >>> XEiJ.BUS_PAGE_BITS].mmdPls (a);
    }
    //リード
    @Override protected byte mmdRbs (int a) throws M68kException {
       int d = (XEiJ.regSRS != 0 ? XEiJ.busSuperMap : XEiJ.busUserMap)[a >>> XEiJ.BUS_PAGE_BITS].mmdRbs (a);
       DataBreakPoint.dbpBreak (DataBreakPoint.DBP_BYTE, a, d, false);
       return (byte) d;
     }
    @Override protected int mmdRbz (int a) throws M68kException {
       int d = (XEiJ.regSRS != 0 ? XEiJ.busSuperMap : XEiJ.busUserMap)[a >>> XEiJ.BUS_PAGE_BITS].mmdRbz (a);
       DataBreakPoint.dbpBreak (DataBreakPoint.DBP_BYTE, a, d, false);
       return d;
     }
    @Override protected int mmdRws (int a) throws M68kException {
       int d = (XEiJ.regSRS != 0 ? XEiJ.busSuperMap : XEiJ.busUserMap)[a >>> XEiJ.BUS_PAGE_BITS].mmdRws (a);
       DataBreakPoint.dbpBreak (DataBreakPoint.DBP_WORD, a, d, false);
       return d;
     }
    @Override protected int mmdRwz (int a) throws M68kException {
       int d = (XEiJ.regSRS != 0 ? XEiJ.busSuperMap : XEiJ.busUserMap)[a >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a);
       DataBreakPoint.dbpBreak (DataBreakPoint.DBP_WORD, a, d, false);
       return d;
     }
    @Override protected int mmdRls (int a) throws M68kException {
       int d = (XEiJ.regSRS != 0 ? XEiJ.busSuperMap : XEiJ.busUserMap)[a >>> XEiJ.BUS_PAGE_BITS].mmdRls (a);
       DataBreakPoint.dbpBreak (DataBreakPoint.DBP_LONG, a, d, false);
       return d;
     }
    //ライト
    @Override protected void mmdWb (int a, int d) throws M68kException {
       (XEiJ.regSRS != 0 ? XEiJ.busSuperMap : XEiJ.busUserMap)[a >>> XEiJ.BUS_PAGE_BITS].mmdWb (a, d);
       DataBreakPoint.dbpBreak (DataBreakPoint.DBP_BYTE, a, d, true);
     }
    @Override protected void mmdWw (int a, int d) throws M68kException {
       (XEiJ.regSRS != 0 ? XEiJ.busSuperMap : XEiJ.busUserMap)[a >>> XEiJ.BUS_PAGE_BITS].mmdWw (a, d);
       DataBreakPoint.dbpBreak (DataBreakPoint.DBP_WORD, a, d, true);
     }
    @Override protected void mmdWl (int a, int d) throws M68kException {
       (XEiJ.regSRS != 0 ? XEiJ.busSuperMap : XEiJ.busUserMap)[a >>> XEiJ.BUS_PAGE_BITS].mmdWl (a, d);
       DataBreakPoint.dbpBreak (DataBreakPoint.DBP_LONG, a, d, true);
     }
  },  //MMD_DBP

  //--------------------------------------------------------------------------------
  //MMD_NUL ヌルデバイス
  MMD_NUL {
    @Override public String toString () {
      return Multilingual.mlnJapanese ? "ヌルデバイス" : "Null Device";
    }
  };  //MMD_NUL

  //--------------------------------------------------------------------------------
  //ピークのデフォルト
  //  エラーや副作用なしでリードする
  //  バスエラーのときは-1をキャストした値を返す
  //  リードがデバイスの状態を変化させる可能性がある場合は個別に処理すること
  protected byte mmdPbs (int a) {
    return (byte) mmdPbz (a);
  }
  protected int mmdPbz (int a) {
    try {
      return mmdRbz (a);
    } catch (M68kException e) {
    }
    return 0xff;
  }
  protected int mmdPws (int a) {
    return (short) mmdPwz (a);
  }
  protected int mmdPwz (int a) {
    try {
      return mmdRwz (a);
    } catch (M68kException e) {
    }
    return 0xffff;
  }
  protected int mmdPls (int a) {
    try {
      return mmdRls (a);
    } catch (M68kException e) {
    }
    return -1;
  }
  //リードのデフォルト
  //  バイトとワードの符号拡張はゼロ拡張を呼び出す
  //  符号なしとロングはバスエラー
  protected byte mmdRbs (int a) throws M68kException {
    return (byte) mmdRbz (a);
  }
  protected int mmdRbz (int a) throws M68kException {
    MC68060.m60BusErrorOnRead ();
    M68kException.m6eNumber = M68kException.M6E_ACCESS_FAULT;
    M68kException.m6eAddress = a;
    M68kException.m6eDirection = XEiJ.MPU_WR_READ;
    M68kException.m6eSize = XEiJ.MPU_SS_BYTE;
    throw M68kException.m6eSignal;
  }
  protected int mmdRws (int a) throws M68kException {
    return (short) mmdRwz (a);
  }
  protected int mmdRwz (int a) throws M68kException {
    MC68060.m60BusErrorOnRead ();
    M68kException.m6eNumber = M68kException.M6E_ACCESS_FAULT;
    M68kException.m6eAddress = a;
    M68kException.m6eDirection = XEiJ.MPU_WR_READ;
    M68kException.m6eSize = XEiJ.MPU_SS_WORD;
    throw M68kException.m6eSignal;
  }
  protected int mmdRls (int a) throws M68kException {
    MC68060.m60BusErrorOnRead ();
    M68kException.m6eNumber = M68kException.M6E_ACCESS_FAULT;
    M68kException.m6eAddress = a;
    M68kException.m6eDirection = XEiJ.MPU_WR_READ;
    M68kException.m6eSize = XEiJ.MPU_SS_LONG;
    throw M68kException.m6eSignal;
  }
  //ポークのデフォルト
  //  エラーや副作用なしでライトする
  //  ライトがデバイスの状態を変化させる可能性がある場合は個別に処理すること
  protected void mmdVb (int a, int d) {
    try {
      mmdWb (a, d);
    } catch (M68kException e) {
    }
  }
  protected void mmdVw (int a, int d) {
    try {
      mmdWw (a, d);
    } catch (M68kException e) {
    }
  }
  protected void mmdVl (int a, int d) {
    try {
      mmdWl (a, d);
    } catch (M68kException e) {
    }
  }
  //ライトのデフォルト
  //  すべてバスエラー
  protected void mmdWb (int a, int d) throws M68kException {
    MC68060.m60BusErrorOnWrite ();
    M68kException.m6eNumber = M68kException.M6E_ACCESS_FAULT;
    M68kException.m6eAddress = a;
    M68kException.m6eDirection = XEiJ.MPU_WR_WRITE;
    M68kException.m6eSize = XEiJ.MPU_SS_BYTE;
    throw M68kException.m6eSignal;
  }
  protected void mmdWw (int a, int d) throws M68kException {
    MC68060.m60BusErrorOnWrite ();
    M68kException.m6eNumber = M68kException.M6E_ACCESS_FAULT;
    M68kException.m6eAddress = a;
    M68kException.m6eDirection = XEiJ.MPU_WR_WRITE;
    M68kException.m6eSize = XEiJ.MPU_SS_WORD;
    throw M68kException.m6eSignal;
  }
  protected void mmdWl (int a, int d) throws M68kException {
    MC68060.m60BusErrorOnWrite ();
    M68kException.m6eNumber = M68kException.M6E_ACCESS_FAULT;
    M68kException.m6eAddress = a;
    M68kException.m6eDirection = XEiJ.MPU_WR_WRITE;
    M68kException.m6eSize = XEiJ.MPU_SS_LONG;
    throw M68kException.m6eSignal;
  }
}  //enum MemoryMappedDevice



