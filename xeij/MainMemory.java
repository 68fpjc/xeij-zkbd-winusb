//========================================================================================
//  MainMemory.java
//    en:Main memory
//    ja:メインメモリ
//  Copyright (C) 2003-2022 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//データの格納単位
//  配列の1要素に何バイトずつ格納するのが効率的か
//    1バイトずつの場合
//      + インデックスとアドレスが一致するので扱いやすい
//      - ワードアクセスとロングアクセスの配列参照の回数が増えるのでインデックスの範囲チェックのオーバーヘッドが大きくなる
//    2バイトずつの場合
//      + アラインメントの合っているワードアクセスとロングアクセスの配列参照の回数が減る
//      - ライトバイトはリードも必要になるので配列参照の回数が増える
//      - インデックスを求めるときにアドレスをシフトしなければならない
//      ? テキスト画面はアクセスマスクが16ビットだがCRTCのキャラクタが8ビット単位なので端数が生じることに変わりはない
//      ? グラフィックス画面は1ピクセルが16ビットだがパレットが8ビット単位なので8ビットずつ分解しなければならないことに変わりはない
//    4バイトずつの場合
//      + アラインメントの合っているロングアクセスの配列参照の回数が減る
//      - ライトバイトとライドワードはリードも必要になるので配列参照の回数が増える
//      - インデックスを求めるときにアドレスをシフトしなければならない
//----------------------------------------------------------------------------------------

package xeij;

import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.nio.*;  //ByteBuffer,ByteOrder
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,Timer,TimerTask,TreeMap

public class MainMemory {

  public static final boolean MMR_USE_BYTE_BUFFER = false;  //true=ワードとロングのアクセスにバイトバッファを使う。遅くなる

  //メインメモリ
  public static final byte[] mmrM8 = new byte[XEiJ.BUS_ARRAY_SIZE];
  public static ByteBuffer mmrBuffer;  //mmrM8をラップしたバイトバッファ

  public static int mmrHumanVersion;  //Human68kのバージョン。-1=Human68kではない,0=未確認,0x0100/0x0101/0x0200/0x0201/0x0202/0x0203/0x020f/0x025f/0x0301/0x0302=バージョン
  public static boolean mmrFEfuncActivated;  //true=FEファンクション命令が有効になった

  //TwentyOne.xのオプション
  public static int mmrTwentyOneOptionAddress;  //TwentyOne.xのオプションのアドレス,-1=非対応,0=未確認

  //メインメモリのサイズ
  //  0x00100000  1MB
  //  0x00200000  2MB
  //  0x00400000  4MB
  //  0x00600000  6MB
  //  0x00800000  8MB
  //  0x00a00000  10MB
  //  0x00c00000  12MB
  //  のいずれか
  public static int mmrMemorySizeRequest;  //次回のリセット後のメインメモリのサイズ。メニューで設定を変更してからリセットするまでの間、現在のメインメモリのサイズと区別する必要がある
  public static int mmrMemorySizeCurrent;  //現在のメインメモリのサイズ

  //  メインメモリの内容を保存する
  //  レジュームできるわけではないがRAMディスクの内容を保存せずにエミュレータを終了しまったときに役立つ
  //  設定ファイルが大きくなるので起動と終了がややもたつく
  public static boolean mmrMemorySaveOn;  //true=メインメモリの内容を保存する

  //mmrInit ()
  //  初期化
  public static void mmrInit () {

    int mainMemorySizeMB = Settings.sgsGetInt ("memory");  //メインメモリのサイズ
    mmrMemorySizeRequest = (mainMemorySizeMB == 1 ||
                            mainMemorySizeMB == 2 ||
                            mainMemorySizeMB == 4 ||
                            mainMemorySizeMB == 6 ||
                            mainMemorySizeMB == 8 ||
                            mainMemorySizeMB == 10 ||
                            mainMemorySizeMB == 12 ? mainMemorySizeMB << 20 :
                            12 << 20);
    mmrMemorySizeCurrent = mmrMemorySizeRequest;
    System.out.printf (Multilingual.mlnJapanese ?
                       "メインメモリのサイズは %dMB です\n" :
                       "Main memory size is %dMB\n",
                       mmrMemorySizeCurrent >>> 20);

    mmrMemorySaveOn = Settings.sgsGetOnOff ("memorysave");  //メインメモリの内容を保存するか

    byte[] mainMemoryArray = Settings.sgsGetData ("memorydata");  //メインメモリの内容(gzip+base64)
    if (mainMemoryArray.length != 0) {  //復元するデータがある
      System.out.println (Multilingual.mlnJapanese ?
                          "メインメモリのデータを復元します" :
                          "Main memory data is restored");
      System.arraycopy (mainMemoryArray, 0,  //from
                        mmrM8, 0,  //to
                        Math.min (mainMemoryArray.length, mmrMemorySizeCurrent));
      if (mainMemoryArray.length < mmrMemorySizeCurrent) {
        Arrays.fill (mmrM8, mainMemoryArray.length, mmrMemorySizeCurrent, (byte) 0);
      }
    } else {
      System.out.println (Multilingual.mlnJapanese ?
                          "メインメモリをゼロクリアします" :
                          "Main memory is zero-cleared");
      Arrays.fill (mmrM8, 0, mmrMemorySizeCurrent, (byte) 0);
    }

    //mmrM8 = new byte[XEiJ.BUS_MOTHOR_SIZE];
    if (MMR_USE_BYTE_BUFFER) {
      mmrBuffer = ByteBuffer.wrap (mmrM8);
      mmrBuffer.order (ByteOrder.BIG_ENDIAN);
    }

    mmrHumanVersion = 0;
    mmrFEfuncActivated = false;
    if (HFS.HFS_USE_TWENTY_ONE) {
      mmrTwentyOneOptionAddress = 0;
    }

  }  //mmrInit()

  public static void mmrReset () {

    mmrMemorySizeCurrent = mmrMemorySizeRequest;
    mmrHumanVersion = 0;
    mmrFEfuncActivated = false;
    if (HFS.HFS_USE_TWENTY_ONE) {
      mmrTwentyOneOptionAddress = 0;
    }

    XEiJ.busSuper (MemoryMappedDevice.MMD_MMR, 0x00000000, 0x00002000);
    XEiJ.busUser (MemoryMappedDevice.MMD_MMR, 0x00002000, mmrMemorySizeCurrent);
    if (mmrMemorySizeCurrent < 0x00200000) {
      XEiJ.busUser (MemoryMappedDevice.MMD_MM1, mmrMemorySizeCurrent, 0x00200000);
      XEiJ.busSuper (MemoryMappedDevice.MMD_NUL, 0x00200000, 0x00c00000);
    } else {
      XEiJ.busSuper (MemoryMappedDevice.MMD_NUL, mmrMemorySizeCurrent, 0x00c00000);
    }

    mmrSetSupervisorArea (0);

  }  //mmrReset()

  //mmrSetSupervisorArea (d)
  //  スーパーバイザ領域設定
  public static void mmrSetSupervisorArea (int d) {
    int a = ((d & 0xff) + 1) << 13;
    if (mmrMemorySizeCurrent < 0x00200000) {
      //  1MB搭載機
      //    0  a  1     2     3
      //    SSSUUUuuuuuuNNNNNN
      //    0     1  a  2     3
      //    SSSSSSsssuuuNNNNNN
      if (a < 0x00100000) {
        XEiJ.busSuper (MemoryMappedDevice.MMD_MMR, 0x00000000, a);
        XEiJ.busUser (MemoryMappedDevice.MMD_MMR, a, 0x00100000);
        XEiJ.busUser (MemoryMappedDevice.MMD_MM1, 0x00100000, 0x00200000);
      } else {
        XEiJ.busSuper (MemoryMappedDevice.MMD_MMR, 0x00000000, 0x00100000);
        XEiJ.busSuper (MemoryMappedDevice.MMD_MM1, 0x00100000, a);
        XEiJ.busUser (MemoryMappedDevice.MMD_MM1, a, 0x00200000);
      }
    } else {
      //  2MB搭載機
      //    0  a  1     2     3
      //    SSSUUUUUUUUUNNNNNN
      //    0     1  a  2     3
      //    SSSSSSSSSUUUNNNNNN
      XEiJ.busSuper (MemoryMappedDevice.MMD_MMR, 0x00000000, a);
      XEiJ.busUser (MemoryMappedDevice.MMD_MMR, a, 0x00200000);
    }
  }

  //d = mmrRbs (a)
  //  メモリリードバイト符号拡張
  public static byte mmrRbs (int a) {
    //byteの配列からbyteのデータを読み出す
    //       a
    //    +-----+
    //    |  a  |
    //    +-----+
    //    |  d  |
    //    +-----+
    return mmrM8[a & XEiJ.BUS_MOTHER_MASK];
  }  //mmrRbs(int)

  //d = mmrRbz (a)
  //  メモリリードバイトゼロ拡張
  public static int mmrRbz (int a) {
    //byteの配列からbyteのデータを読み出す
    //       a
    //    +-----+
    //    |  a  |
    //    +-----+
    //    |  d  |
    //    +-----+
    return mmrM8[a & XEiJ.BUS_MOTHER_MASK] & 255;
  }  //mmrRbz(int)

  //d = mmrRws (a)
  //  メモリリードワード符号拡張
  public static int mmrRws (int a) {
    if (MMR_USE_BYTE_BUFFER) {
      return mmrBuffer.getShort (a & XEiJ.BUS_MOTHER_MASK);
    } else {
      //byteの配列からshortのデータを読み出す
      //       a    a+1
      //    +-----+-----+
      //    |  a  | a+1 |
      //    +-----+-----+
      //    |     d     |
      //    +-----+-----+
      return mmrM8[a & XEiJ.BUS_MOTHER_MASK] << 8 | mmrM8[a + 1 & XEiJ.BUS_MOTHER_MASK] & 255;
    }
  }  //mmrRws(int)

  //d = mmrRwz (a)
  //  メモリリードワードゼロ拡張
  public static int mmrRwz (int a) {
    if (MMR_USE_BYTE_BUFFER) {
      return mmrBuffer.getChar (a & XEiJ.BUS_MOTHER_MASK);
    } else {
      //byteの配列からunsigned shortのデータを読み出す
      //       a    a+1
      //    +-----+-----+
      //    |  a  | a+1 |
      //    +-----+-----+
      //    |     d     |
      //    +-----+-----+
      return (char) (mmrM8[a & XEiJ.BUS_MOTHER_MASK] << 8 | mmrM8[a + 1 & XEiJ.BUS_MOTHER_MASK] & 255);
    }
  }  //mmrRwz(int)

  //d = mmrRls (a)
  //  メモリリードロング符号拡張
  public static int mmrRls (int a) {
    if (MMR_USE_BYTE_BUFFER) {
      return mmrBuffer.getInt (a & XEiJ.BUS_MOTHER_MASK);
    } else {
      //byteの配列からintのデータを読み出す
      //       a    a+1   a+2   a+3
      //    +-----+-----+-----+-----+
      //    |  a  | a+1 | a+2 | a+3 |
      //    +-----+-----+-----+-----+
      //    |           d           |
      //    +-----+-----+-----+-----+
      return mmrM8[a & XEiJ.BUS_MOTHER_MASK] << 24 | (mmrM8[a + 1 & XEiJ.BUS_MOTHER_MASK] & 255) << 16 | (char) (mmrM8[a + 2 & XEiJ.BUS_MOTHER_MASK] << 8 | mmrM8[a + 3 & XEiJ.BUS_MOTHER_MASK] & 255);
    }
  }  //mmrRls(int)

  //d = mmrRqs (a)
  //  メモリリードクワッド符号拡張
  public static long mmrRqs (int a) {
    if (MMR_USE_BYTE_BUFFER) {
      return mmrBuffer.getLong (a & XEiJ.BUS_MOTHER_MASK);
    } else {
      return ((long) (mmrM8[a     & XEiJ.BUS_MOTHER_MASK] << 24 |
                      mmrM8[a + 1 & XEiJ.BUS_MOTHER_MASK] << 16 & 0x00ff0000 |
                      mmrM8[a + 2 & XEiJ.BUS_MOTHER_MASK] <<  8 & 0x0000ff00 |
                      mmrM8[a + 3 & XEiJ.BUS_MOTHER_MASK]       & 0x000000ff) << 32 |
              (long) (mmrM8[a + 4 & XEiJ.BUS_MOTHER_MASK] << 24 |
                      mmrM8[a + 5 & XEiJ.BUS_MOTHER_MASK] << 16 & 0x00ff0000 |
                      mmrM8[a + 6 & XEiJ.BUS_MOTHER_MASK] <<  8 & 0x0000ff00 |
                      mmrM8[a + 7 & XEiJ.BUS_MOTHER_MASK]       & 0x000000ff) & 0x00000000ffffffffL);
    }
  }  //mmrRqs(int)

  //mmrWb (a, d)
  //  メモリライトバイト
  public static void mmrWb (int a, int d) {
    //byteの配列にbyteのデータを書き込む
    //       a
    //    +-----+
    //    |  d  |
    //    +-----+
    //    |  a  |
    //    +-----+
    mmrM8[a & XEiJ.BUS_MOTHER_MASK] = (byte) d;
  }  //mmrWb(int,int)

  //mmrWw (a, d)
  //  メモリライトワード
  public static void mmrWw (int a, int d) {
    if (MMR_USE_BYTE_BUFFER) {
      mmrBuffer.putShort (a & XEiJ.BUS_MOTHER_MASK, (short) d);
    } else {
      //byteの配列にshortのデータを書き込む
      //       a    a+1
      //    +-----+-----+
      //    |     d     |
      //    +-----+-----+
      //    |  a  | a+1 |
      //    +-----+-----+
      mmrM8[a     & XEiJ.BUS_MOTHER_MASK] = (byte) (d >> 8);
      mmrM8[a + 1 & XEiJ.BUS_MOTHER_MASK] = (byte)  d;
    }
  }  //mmrWw(int,int)

  //a = mmrWl (a, d)
  //  メモリライトロング
  public static void mmrWl (int a, int d) {
    if (MMR_USE_BYTE_BUFFER) {
      mmrBuffer.putInt (a & XEiJ.BUS_MOTHER_MASK, d);
    } else {
      //byteの配列にintのデータを書き込む
      //       a    a+1   a+2   a+3
      //    +-----+-----+-----+-----+
      //    |           d           |
      //    +-----+-----+-----+-----+
      //    |  a  | a+1 | a+2 | a+3 |
      //    +-----+-----+-----+-----+
      mmrM8[a     & XEiJ.BUS_MOTHER_MASK] = (byte) (d >> 24);
      mmrM8[a + 1 & XEiJ.BUS_MOTHER_MASK] = (byte) (d >> 16);
      mmrM8[a + 2 & XEiJ.BUS_MOTHER_MASK] = (byte) (d >> 8);
      mmrM8[a + 3 & XEiJ.BUS_MOTHER_MASK] = (byte)  d;
    }
  }  //mmrWl(int,int)

  //a = mmrWq (a, d)
  //  メモリライトクワッド
  public static void mmrWq (int a, long d) {
    if (MMR_USE_BYTE_BUFFER) {
      mmrBuffer.putLong (a & XEiJ.BUS_MOTHER_MASK, d);
    } else {
      mmrM8[a     & XEiJ.BUS_MOTHER_MASK] = (byte) (d >> 56);
      mmrM8[a + 1 & XEiJ.BUS_MOTHER_MASK] = (byte) (d >> 48);
      mmrM8[a + 2 & XEiJ.BUS_MOTHER_MASK] = (byte) (d >> 40);
      mmrM8[a + 3 & XEiJ.BUS_MOTHER_MASK] = (byte) (d >> 32);
      mmrM8[a + 4 & XEiJ.BUS_MOTHER_MASK] = (byte) (d >> 24);
      mmrM8[a + 5 & XEiJ.BUS_MOTHER_MASK] = (byte) (d >> 16);
      mmrM8[a + 6 & XEiJ.BUS_MOTHER_MASK] = (byte) (d >> 8);
      mmrM8[a + 7 & XEiJ.BUS_MOTHER_MASK] = (byte)  d;
    }
  }  //mmrWq(int,long)

  //mmrWba (a, d, ...)
  //  メモリライトバイトアレイ
  public static void mmrWba (int a, int... da) {
    for (int d : da) {
      mmrWb (a, d);
      a++;
    }
  }  //mmrWba(int,int...)

  //mmrWwa (a, d, ...)
  //  メモリライトワードアレイ
  public static void mmrWwa (int a, int... da) {
    for (int d : da) {
      mmrWw (a, d);
      a += 2;
    }
  }  //mmrWwa(int,int...)

  //a = mmrWla (a, d, ...)
  //  メモリライトロングアレイ
  public static void mmrWla (int a, int... da) {
    for (int d : da) {
      mmrWl (a, d);
      a += 4;
    }
  }  //mmrWla(int,int...)

  //len = mmrStrlen (a, l)
  public static int mmrStrlen (int a, int l) {
    for (int i = 0; i < l; i++) {
      if (mmrM8[a + i] == 0) {
        return i;
      }
    }
    return l;
  }  //mmrStrlen(int,int)

  //s = mmrRstr (a, l)
  //sb = mmrRstr (sb, a, l)
  //  メモリリードストリング
  //  文字列を読み出す
  //  対応する文字がないときは'.'または'※'になる
  //  制御コードは'.'になる
  public static String mmrRstr (int a, int l) {
    return mmrRstr (new StringBuilder (), a, l).toString ();
  }  //mmrRstr(int,int)
  public static StringBuilder mmrRstr (StringBuilder sb, int a, int l) {
    for (int i = 0; i < l; i++) {
      int s = mmrRbz (a + i);
      char c;
      if (0x81 <= s && s <= 0x9f || 0xe0 <= s && s <= 0xef) {  //SJISの2バイトコードの1バイト目
        int t = i + 1 < l ? mmrRbz (a + i + 1) : 0;
        if (0x40 <= t && t != 0x7f && t <= 0xfc) {  //SJISの2バイトコードの2バイト目
          c = CharacterCode.chrSJISToChar[s << 8 | t];  //2バイトで変換する
          if (c == 0) {  //対応する文字がない
            c = '※';
          }
          i++;
        } else {  //SJISの2バイトコードの2バイト目ではない
          c = '.';  //SJISの2バイトコードの1バイト目ではなかった
        }
      } else {  //SJISの2バイトコードの1バイト目ではない
        c = CharacterCode.chrSJISToChar[s];  //1バイトで変換する
        if (c < 0x20 || c == 0x7f) {  //対応する文字がないまたは制御コード
          c = '.';
        }
      }
      sb.append (c);
    }
    return sb;
  }  //mmrRstr(StringBuilder,int,int)

  //a = mmrWstr (a, s)
  //  メモリライトストリング
  //  文字列をSJISに変換しながら書き込む
  //  SJISに変換できない文字は'※'になる
  //  文字列の直後のアドレス(マスク済み)を返す
  public static void mmrWstr (int a, String s) {
    int l = s.length ();
    for (int i = 0; i < l; i++) {
      int c = CharacterCode.chrCharToSJIS[s.charAt (i)];
      if (c == 0 && s.charAt (i) != '\0') {  //SJISに変換できないとき'\0'でない文字が0になる
        mmrWw (a, 0x81a6);  //※
        a += 2;
      } else if (c <= 0x00ff) {
        mmrWb (a, c);
        a++;
      } else {
        mmrWw (a, c);
        a += 2;
      }
    }
  }  //mmrWstr(int,String)

  //top = mmrHumanTop ()
  //  Human68kのメモリ管理の先頭のアドレス(HUMAN.SYSのメモリ管理テーブルのアドレス)を返す
  //  -1  Human68kが読み込まれていないか、未知のバージョン
  //  Human68kのメモリ管理の先頭のアドレス。Human200以降は0x1c04に入っているものと同じ
  //    human100 0x00010a10
  //    human101 0x00010a1a
  //    human200 0x00007dae
  //    human201 0x00007dae
  //    human202 0x00007dce
  //    human203 0x00007d50
  //    human215 0x0000841c
  //    human301 0x000082d0
  //    human302 0x00008372 human295
  public static int mmrHumanTop () {
    return (mmrHumanVersion == 0x0302 ||
            mmrHumanVersion == 0x025f ? 0x00008372 :
            mmrHumanVersion == 0x0301 ? 0x000082d0 :
            mmrHumanVersion == 0x020f ? 0x0000841c :
            mmrHumanVersion == 0x0203 ? 0x00007d50 :
            mmrHumanVersion == 0x0202 ? 0x00007dce :
            mmrHumanVersion == 0x0201 ? 0x00007dae :
            mmrHumanVersion == 0x0200 ? 0x00007dae :
            mmrHumanVersion == 0x0101 ? 0x00010a1a :
            mmrHumanVersion == 0x0100 ? 0x00010a10 :
            -1);
  }  //mmrHumanTop()

  //btm = mmrHumanBtm ()
  //  Human68kのメモリ管理の末尾のアドレスを返す
  //  -1  Human68kが読み込まれていないか、未知のバージョン
  public static int mmrHumanBtm () {
    return mmrHumanVersion > 0 ? MC68060.mmuPeekLongData (0x00001c00, 1) : -1;
  }  //mmrHumanBtm()

  //pmm = mmrHumanPmm ()
  //  Human68kの実行中のプロセスのメモリ管理テーブルのアドレス(GETPDB()-16)を返す
  //  -1  Human68kが読み込まれていないか、未知のバージョン
  //  実行中のプロセスのメモリ管理テーブルのアドレス
  //    human100 [0x00008a98]
  //    human101 [0x00008a92]
  //    human200 [0x00012d88]
  //    human201 [0x00012d88]
  //    human202 [0x00012dc8]
  //    human203 [0x00012b54]
  //    human215 [0x00013c9c]
  //    human301 [0x00013bf4]
  //    human302 [0x00013d0a] human295
  public static int mmrHumanPmm () {
    return (mmrHumanVersion == 0x0302 ||
            mmrHumanVersion == 0x025f ? MC68060.mmuPeekLongData (0x00013d0a, 1) :
            mmrHumanVersion == 0x0301 ? MC68060.mmuPeekLongData (0x00013bf4, 1) :
            mmrHumanVersion == 0x020f ? MC68060.mmuPeekLongData (0x00013c9c, 1) :
            mmrHumanVersion == 0x0203 ? MC68060.mmuPeekLongData (0x00012b54, 1) :
            mmrHumanVersion == 0x0202 ? MC68060.mmuPeekLongData (0x00012dc8, 1) :
            mmrHumanVersion == 0x0201 ? MC68060.mmuPeekLongData (0x00012d88, 1) :
            mmrHumanVersion == 0x0200 ? MC68060.mmuPeekLongData (0x00012d88, 1) :
            mmrHumanVersion == 0x0101 ? MC68060.mmuPeekLongData (0x00008a92, 1) :
            mmrHumanVersion == 0x0100 ? MC68060.mmuPeekLongData (0x00008a98, 1) :
            -1);
  }  //mmrHumanPmm()

  //nul = mmrHumanNul ()
  //  Human68kのNULデバイスドライバのアドレスを返す
  //  -1  Human68kが読み込まれていないか、未知のバージョンか、NULデバイスドライバが見つからない
  //  NULデバイスドライバのアドレス
  //    human100 0x0000b83e
  //    human101 0x0000b84c
  //    human200 0x0000ece6
  //    human201 0x0000ece6
  //    human202 0x0000ed36
  //    human203 0x0000eac2
  //    human215 0x0000fa04
  //    human301 0x0000f93a
  //    human302 0x0000fa50 human295
  public static int mmrHumanNul () {
    int a = (mmrHumanVersion == 0x0302 ||
             mmrHumanVersion == 0x025f ? 0x0000fa50 :
             mmrHumanVersion == 0x0301 ? 0x0000f93a :
             mmrHumanVersion == 0x020f ? 0x0000fa04 :
             mmrHumanVersion == 0x0203 ? 0x0000eac2 :
             mmrHumanVersion == 0x0202 ? 0x0000ed36 :
             mmrHumanVersion == 0x0201 ? 0x0000ece6 :
             mmrHumanVersion == 0x0200 ? 0x0000ece6 :
             mmrHumanVersion == 0x0101 ? 0x0000b84c :
             mmrHumanVersion == 0x0100 ? 0x0000b83e :
             -1);
    return (a >= 0 &&
            MC68060.mmuPeekLongData (a + 14, 1) == ('N' << 24 | 'U' << 16 | 'L' << 8 | ' ') &&
            MC68060.mmuPeekLongData (a + 18, 1) == (' ' << 24 | ' ' << 16 | ' ' << 8 | ' ') ? a : -1);
  }  //mmrHumanNul()

  //dev = mmrHumanDev (name1, name2)
  //  Human68kに組み込まれている指定された名前のデバイスドライバのアドレスを返す
  //  同じ名前のデバイスドライバが複数組み込まれているときは最後に見つかったものを返す
  //  最初の100個までに見つからなかったら諦める
  //  -1  Human68kが読み込まれていないか、未知のバージョンか、指定された名前のデバイスドライバが見つからない
  //  CONデバイスの場合
  //    con = mmrHumanDev ('C' << 24 | 'O' << 16 | 'N' << 8 | ' ', ' ' << 24 | ' ' << 16 | ' ' << 8 | ' ');
  public static int mmrHumanDev (int name1, int name2) {
    int dev = -1;
    for (int a = mmrHumanNul (), i = 0; a >= 0 && i < 100; a = MC68060.mmuPeekLongData (a, 1), i++) {
      //!!! ローカルメモリまで辿れない
      if (MC68060.mmuPeekLongData (a + 14, 1) == name1 && MC68060.mmuPeekLongData (a + 18, 1) == name2) {  //見つかった
        dev = a;
        //最後に見つかったものを返すので見つかっても続ける
      }
    }
    return dev;  //最後に見つかったデバイスドライバ
  }  //mmrHumanDev(int,int)

  //mmrCheckHuman ()
  //  Human68kのバージョンを確認してパッチをあてる
  //  IOCS _BOOTINFで呼び出される
  public static void mmrCheckHuman () {
    if (mmrHumanVersion != 0) {  //確認済み
      return;
    }
    //Human68kが起動デバイスを確認するときに呼び出した_BOOTINFであることを確認する
    if (!(MC68060.mmuPeekLongData (0x00001c00, 1) != 0 &&  //_MALLOCできるメモリ空間の末尾アドレス+1が設定されていて
          MC68060.mmuPeekLongData (0x00001c1c, 1) == 0)) {  //最後のデバイスドライバがまだ設定されていない
      return;
    }
    //Human68kのバージョンを確認する
    //  タイトルメッセージの領域はスタックエリアになって$FFで充填されているのでシェル起動後は確認できない
    //  _VERNUMのコードを直接読み出す
    //    human100  0x00009ae4  0x0100
    //    human101  0x00009aee  0x0101
    //    human200  0x00009ee4  0x0200
    //    human201  0x00009ee4  0x0201
    //    human202  0x00009ed6  0x0202
    //    human203  0x00009d7e  0x0203
    //    human215  0x0000a4fa  0x020f  //0x0215ではない
    //    human301  0x0000a3c6  0x0301
    //    human302  0x0000a4ac  0x0302 human295
    //  0000A4AC  303C3638      move.w  #$3638,d0
    //  0000A4B0  4840          swap.w  d0
    //  0000A4B2  303C0302      move.w  #$0302,d0
    //  0000A4B6  4E75          rts
    mmrHumanVersion = (MC68060.mmuPeekWordZeroData (0x0000a4ac + 8, 1) == 0x0302 ? 0x0302 :
                       MC68060.mmuPeekWordZeroData (0x0000a4ac + 8, 1) == 0x025f ? 0x025f :
                       MC68060.mmuPeekWordZeroData (0x0000a3c6 + 8, 1) == 0x0301 ? 0x0301 :
                       MC68060.mmuPeekWordZeroData (0x0000a4fa + 8, 1) == 0x020f ? 0x020f :  //0x0215ではない
                       MC68060.mmuPeekWordZeroData (0x00009d7e + 8, 1) == 0x0203 ? 0x0203 :
                       MC68060.mmuPeekWordZeroData (0x00009ed6 + 8, 1) == 0x0202 ? 0x0202 :
                       MC68060.mmuPeekWordZeroData (0x00009ee4 + 8, 1) == 0x0201 ? 0x0201 :
                       MC68060.mmuPeekWordZeroData (0x00009ee4 + 8, 1) == 0x0200 ? 0x0200 :
                       MC68060.mmuPeekWordZeroData (0x00009aee + 8, 1) == 0x0101 ? 0x0101 :
                       MC68060.mmuPeekWordZeroData (0x00009ae4 + 8, 1) == 0x0100 ? 0x0100 :
                       -1);  //Human68kが読み込まれていないか、未知のバージョン
    if (mmrHumanVersion < 0) {  //Human68kが読み込まれていないか、未知のバージョン
      return;
    }
    //Human68kにパッチをあてる
    int patched = 0;
    int failed = 0;
    switch (mmrHumanVersion) {

    case 0x0215:

      //RAMまたはROMから起動してDISK2HDを初期化するときリクエストヘッダの初期化コマンドを設定していない(human215,human301,human302)
      //                                                   →                          sf.b    ($0002,a5)
      //  0000802E  61005F74      bsr.w   ~00DEFA
      //  00008032  082900070004  btst.b  #$07,($0004,a1)  →                          tst.b   ($0004,a1)
      //  00008038  6618          bne.s   $00008052        →                          bmi.s   $00008052

      //MOVE from SRをMOVE from CCRに変更した後のキャッシュフラッシュを68010～68060に対応させる
      //  000086EC  4E7A0002      movec.l cacr,d0          →  2F01          move.l  d1,-(sp)
      //                                                       7203          moveq.l #3,d1
      //  000086F0  807C0008      or.w    #$0008,d0        →  70AC4E4F      IOCS    _SYS_STAT
      //  000086F4  4E7B0002      movec.l d0,cacr          →  221F          move.l  (sp)+,d1
      //                                                       2048          movea.l a0,a0
      if (MC68060.mmuPeekLongData (0x000086ec, 1) == 0x4e7a0002) {
        MC68060.mmuPokeLongData (0x000086ec, 0x2f017203, 1);
        MC68060.mmuPokeLongData (0x000086f0, 0x70ac4e4f, 1);
        MC68060.mmuPokeLongData (0x000086f4, 0x221f2048, 1);
        patched++;
      } else {
        failed++;
      }

      //x形式のヘッダのメモリアロケーションモードが必要最小ブロックからかどうかをテストするbit番号が間違っている(human215,human301,human302)
      //  00009A5A  08010001      btst.l  #$01,d1          →  00009A5A  08010000      btst.l  #$00,d1
      if (MC68060.mmuPeekLongData (0x00009a5a, 1) == 0x08010001) {
        MC68060.mmuPokeByteData (0x00009a5a + 3, 0x00, 1);
        patched++;
      } else {
        failed++;
      }

      //ディレクトリを延長するときルートディレクトリかどうかを判断するためにセクタ番号をデータ部の先頭セクタ番号と比較するとき上位ワードを無視している(human215,human301,human302)
      //                                                   →            7000          moveq.l #$00,d0
      //  0000B900  30280014      move.w  ($0014,a0),d0
      //  0000B904  B240          cmp.w   d0,d1            →            B280          cmp.l   d0,d1
      //  0000B906  6406          bcc.s   $0000B90E
      //  0000B908  5241          addq.w  #$01,d1
      //  0000B90A  B240          cmp.w   d0,d1
      //  0000B90C  4E75          rts
      //  0000B90E

      //FILESのバッファのアドレスのbit31がセットされているとき拡張部分をコピーするループのループカウンタのレジスタが間違っている(human215)
      //  0000BC3E  7255          moveq.l #$55,d1
      //  0000BC40  12D8          move.b  (a0)+,(a1)+
      //  0000BC42  51C8FFFC      dbra.w  d0,$0000BC40     →  0000BC42  51C9FFFC      dbra.w  d1,$0000BC40
      if (MC68060.mmuPeekWordZeroData (0x0000bc42, 1) == 0x51c8) {
        MC68060.mmuPokeByteData (0x0000bc42 + 1, 0xc9, 1);
        patched++;
      } else {
        failed++;
      }

      //リモートデバイスに対するchmodコマンドのコマンド番号が間違っている(human215)
      //  0000D848  7057          moveq.l #$57,d0          →  0000D848  7046          moveq.l #$46,d0
      if (MC68060.mmuPeekWordZeroData (0x0000d848, 1) == 0x7057) {
        MC68060.mmuPokeByteData (0x0000d848 + 1, 0x46, 1);
        patched++;
      } else {
        failed++;
      }

      //サブのメモリ空間を削除するときサブの管理下で常駐したブロックをメインのメモリ空間からサブのメモリ空間に入る方向に繋いでいない(human215,human301,human302)

      //スレッドを切り替えるためのTimer-D割り込みルーチンがMC68030のコプロセッサ命令途中割り込みに対応していない(human215,human301,human302)

      //IOCTRL(19,1)でBPBテーブルをコピーする長さとPDAとイジェクトフラグを書き込む位置が間違っている(human215,human301,human302)
      //  00010AE8  700B          moveq.l #$0B,d0          →  00010AE8  700F          moveq.l #$0F,d0
      //  00010AEA  10DE          move.b  (a6)+,(a0)+
      //  00010AEC  51C8FFFC      dbra.w  d0,$00010AEA
      if (MC68060.mmuPeekWordZeroData (0x00010ae8, 1) == 0x700b) {
        MC68060.mmuPokeByteData (0x00010ae8 + 1, 0x0f, 1);
        patched++;
      } else {
        failed++;
      }

      //IOCTRL(19,0)でBPBテーブルのハンドルをBPBテーブルのアドレスとして参照しようとしている(human215)
      //  00FCA520  61000084      bsr.w   $00FCA5A6
      //  00FCA524  206D000E      movea.l $000E(a5),a0
      //  ;BPBテーブルのハンドルを求めるときにd0.w=(d0.w&3)*4を計算しているのでd0.wの上位バイトは既に0になっている
      //  00FCA528  4240          clr.w   d0               →  00FCA528  2C56          movea.l (a6),a6
      //  00FCA52A  102E000A      move.b  $000A(a6),d0
      //  00FCA52E  3080          move.w  d0,(a0)

      break;

    case 0x0301:

      //RAMまたはROMから起動してDISK2HDを初期化するときリクエストヘッダの初期化コマンドを設定していない(human215,human301,human302)
      //
      //                                                   →                          sf.b    ($0002,a5)
      //  00007EE2  61005F74      bsr.w   ~00DEFA
      //  00007EE6  082900070004  btst.b  #$07,($0004,a1)  →                          tst.b   ($0004,a1)
      //  00007EEC  6618          bne.s   $00007F06        →                          bmi.s   $00007F06

      //MOVE from SRをMOVE from CCRに変更した後のキャッシュフラッシュを68010～68060に対応させる
      //  000085B8  4E7A0002      movec.l cacr,d0          →  2F01          move.l  d1,-(sp)
      //                                                       7203          moveq.l #3,d1
      //  000085BC  807C0008      or.w    #$0008,d0        →  70AC4E4F      IOCS    _SYS_STAT
      //  000085C0  4E7B0002      movec.l d0,cacr          →  221F          move.l  (sp)+,d1
      //                                                       2048          movea.l a0,a0
      if (MC68060.mmuPeekLongData (0x000085b8, 1) == 0x4e7a0002) {
        MC68060.mmuPokeLongData (0x000085b8, 0x2f017203, 1);
        MC68060.mmuPokeLongData (0x000085bc, 0x70ac4e4f, 1);
        MC68060.mmuPokeLongData (0x000085c0, 0x221f2048, 1);
        patched++;
      } else {
        failed++;
      }

      //x形式のヘッダのメモリアロケーションモードが必要最小ブロックからかどうかをテストするbit番号が間違っている(human215,human301,human302)
      //
      //  00009926  08010001      btst.l  #$01,d1          →  00009926  08010000      btst.l  #$00,d1
      if (MC68060.mmuPeekWordZeroData (0x00009926 + 2, 1) == 0x0001) {
        MC68060.mmuPokeByteData (0x00009926 + 3, 0x00, 1);
        patched++;
      } else {
        failed++;
      }

      //ディレクトリを延長するときルートディレクトリかどうかを判断するためにセクタ番号をデータ部の先頭セクタ番号と比較するとき上位ワードを無視している(human215,human301,human302)
      //
      //サブのメモリ空間を削除するときサブの管理下で常駐したブロックをメインのメモリ空間からサブのメモリ空間に入る方向に繋いでいない(human215,human301,human302)
      //
      //スレッドを切り替えるためのTimer-D割り込みルーチンがMC68030のコプロセッサ命令途中割り込みに対応していない(human215,human301,human302)
      //
      //IOCTRL(19,1)でBPBテーブルをコピーする長さとPDAとイジェクトフラグを書き込む位置が間違っている(human215,human301,human302)
      //
      //  00010A22  700B          moveq.l #$0B,d0          →  00010A22  700F          moveq.l #$0F,d0
      //  00010A24  10DE          move.b  (a6)+,(a0)+
      //  00010A26  51C8FFFC      dbra.w  d0,$00010A24
      if (MC68060.mmuPeekWordZeroData (0x00010a22, 1) == 0x700b) {
        MC68060.mmuPokeByteData (0x00010a22 + 1, 0x0f, 1);
        patched++;
      } else {
        failed++;
      }

      break;

    case 0x0302:
    case 0x025f:

      //デバイスドライバを初期化する直前と初期化した直後
      //<a1.l:初期化されたデバイスドライバのデバイスヘッダのアドレス
      //  00007140  1B7C00160000  move.b  #$16,$0000(a5)  →  00007140  4E04          emxnop
      //                                                  →  00007142  2209          move.l  a1,d1
      //                                                  →  00007144  1ABC0016      move.b  #$16,(a5)
      //  00007146  1B7C00000002  move.b  #$00,$0002(a5)  →
      //                                                  →  00007148  51ED0002      sf.b    $0002(a5)
      //  0000714C  082900050004  btst.b  #$05,$0004(a1)
      //  00007152  6706          beq.s   $0000715A
      //  00007154  1B7C00400002  move.b  #$40,$0002(a5)
      //  0000715A  2B400012      move.l  d0,$0012(a5)
      //  0000715E  10381C75      move.b  $1C75.w,d0
      //  00007162  5200          addq.b  #$01,d0
      //  00007164  1B400016      move.b  d0,$0016(a5)
      //  00007168  2209          move.l  a1,d1           →  00007168  61006D90      bsr.w   $0000DEFA
      //  0000716A  2241          movea.l d1,a1           →
      //  0000716C  60006D8C      bra.w   $0000DEFA       →  0000716C  4E04          emxnop
      //                                                  →  0000716E  4E75          rts
      if (MC68060.mmuPeekLongData (0x00007140, 1) == 0x1b7c0016) {
        MC68060.mmuPokeLongData (0x00007140, XEiJ.EMX_OPCODE_EMXNOP << 16 | 0x2209, 1);
        MC68060.mmuPokeLongData (0x00007144, 0x1abc0016, 1);
        MC68060.mmuPokeLongData (0x00007148, 0x51ed0002, 1);
        MC68060.mmuPokeLongData (0x00007168, 0x61006d90, 1);
        MC68060.mmuPokeLongData (0x0000716c, XEiJ.EMX_OPCODE_EMXNOP << 16 | 0x4e75, 1);
        patched++;
      } else {
        failed++;
      }

      //ブロックデバイスのユニット数が0の場合に対応する
      //  00007FA4  6B00F152      bmi.w   $000070F8           7FA4  6B00F156  bmi.w   $000070FC
      //  
      //  000070E2  082900070004  btst.b  #$07,$0004(a1)      70E2  4A290004  tst.b   $0004(a1)
      //                                                      70E6  6B28      bmi.s   $00007110
      //  000070E8  6626          bne.s   $00007110           70E8  4A2D000D  tst.b   $000D(a5)
      //  000070EA  204D          movea.l a5,a0
      //  000070EC  610011E2      bsr.w   $000082D0           70EC  6722      beq.s   $00007110
      //                                                      70EE  204D      movea.l a5,a0
      //  000070F0  6B12          bmi.s   $00007104           70F0  610011DE  bsr.w   $000082D0
      //  000070F2  61001154      bsr.w   $00008248
      //                                                      70F4  6B0E      bmi.s   $00007104
      //  000070F6  6A1C          bpl.s   $00007114           70F6  61001150  bsr.w   $00008248
      //  000070F8  41F900007DBD  lea.l   $00007DBD.l,a0
      //                                                      70FA  6A18      bpl.s   $00007114
      //                                                      70FC  41FA0CBF  lea.l   $00007DBD(pc),a0
      //  000070FE  6134          bsr.s   $00007134
      //  00007100  6000F826      bra.w   $00006928           7100  6132      bsr.s   $00007134
      //                                                      7102  6008      bra.s   $0000710C
/*
      if (MC68060.mmuPeekLongData (0x00007fa4, 1) == 0x6b00f152) {
        MC68060.mmuPokeLongData (0x00007fa4, 0x6b00f156, 1);
        MC68060.mmuPokeLongData (0x000070e2, 0x4a290004, 1);
        MC68060.mmuPokeLongData (0x000070e6, 0x6b284a2d, 1);
        MC68060.mmuPokeLongData (0x000070ea, 0x000d6722, 1);
        MC68060.mmuPokeLongData (0x000070ee, 0x204d6100, 1);
        MC68060.mmuPokeLongData (0x000070f2, 0x11de6b0e, 1);
        MC68060.mmuPokeLongData (0x000070f6, 0x61001150, 1);
        MC68060.mmuPokeLongData (0x000070fa, 0x6a1841fa, 1);
        MC68060.mmuPokeLongData (0x000070fe, 0x0cbf6132, 1);
        MC68060.mmuPokeWordData (0x00007102, 0x6008, 1);
        patched++;
      } else {
        failed++;
      }
*/

      //RAMまたはROMから起動してDISK2HDを初期化するときリクエストヘッダの初期化コマンドを設定していない(human215,human301,human302)
      //
      //                                                   →                          sf.b    ($0002,a5)
      //  00007F84  61005F74      bsr.w   ~00DEFA
      //  00007F88  082900070004  btst.b  #$07,($0004,a1)  →                          tst.b   ($0004,a1)
      //  00007F8E  6618          bne.s   $00007FA8        →                          bmi.s   $00007FA8
      //

      //MOVE from SRをMOVE from CCRに変更した後のキャッシュフラッシュを68010～68060に対応させる
      //  0000865A  4E7A0002      movec.l cacr,d0          →  2F01          move.l  d1,-(sp)
      //                                                       7203          moveq.l #3,d1
      //  0000865E  807C0008      or.w    #$0008,d0        →  70AC4E4F      IOCS    _SYS_STAT
      //  00008662  4E7B0002      movec.l d0,cacr          →  221F          move.l  (sp)+,d1
      //                                                       2048          movea.l a0,a0
      if (MC68060.mmuPeekLongData (0x0000865a, 1) == 0x4e7a0002) {
        MC68060.mmuPokeLongData (0x0000865a, 0x2f017203, 1);
        MC68060.mmuPokeLongData (0x0000865e, 0x70ac4e4f, 1);
        MC68060.mmuPokeLongData (0x00008662, 0x221f2048, 1);
        patched++;
      } else {
        failed++;
      }

      //プロセスを起動する直前
      //  0000971E  2B48001C      move.l  a0,$001C(a5)    →  0000971E  48ED1F00001C  movem.l a0-a4,$001C(a5)
      //  00009722  2B490020      move.l  a1,$0020(a5)    →
      //                                                      00009724  200C          move.l  a4,d0
      //  00009726  2B4A0024      move.l  a2,$0024(a5)    →  00009726  4A41          tst.w   d1
      //                                                      00009728  6600FE30      bne.w   $0000955A
      //  0000972A  2B4B0028      move.l  a3,$0028(a5)    →
      //                                                      0000972C  4E04          emxnop
      //  0000972E  2B4C002C      move.l  a4,$002C(a5)    →  0000972E  600A          bra.s   $0000973A
      //  00009732  200C          move.l  a4,d0           →
      //  00009734  4A41          tst.w   d1              →
      //  00009736  6600FE22      bne.w   $0000955A       →
      if (MC68060.mmuPeekLongData (0x0000971e, 1) == 0x2b48001c) {
        MC68060.mmuPokeWordData (0x0000971e, 0x48ed, 1);
        MC68060.mmuPokeLongData (0x00009720, 0x1f00001c, 1);
        MC68060.mmuPokeLongData (0x00009724, 0x200c4a41, 1);
        MC68060.mmuPokeLongData (0x00009728, 0x6600fe30, 1);
        MC68060.mmuPokeLongData (0x0000972c, XEiJ.EMX_OPCODE_EMXNOP << 16 | 0x600a, 1);
        patched++;
      } else {
        failed++;
      }

      //x形式のヘッダのメモリアロケーションモードが必要最小ブロックからかどうかをテストするbit番号が間違っている(human215,human301,human302)
      //
      //  000099C4  08010001      btst.l  #$01,d1          →  000099C4  08010000      btst.l  #$00,d1
      if (XEiJ.currentMPU < Model.MPU_MC68LC060) {  //060turbo.sysのパッチと衝突する
        if (MC68060.mmuPeekWordZeroData (0x000099c4 + 2, 1) == 0x0001) {
          MC68060.mmuPokeByteData (0x000099c4 + 3, 0x00, 1);
          patched++;
        } else {
          failed++;
        }
      }

      //プロセスが常駐した直後
      //<a0.l:常駐したプロセスのメモリ管理テーブルのアドレス
      //  0000A088  4A380CBC      tst.b   $0CBC.w
      //  0000A08C  6704          beq.s   $0000A092       →  0000A08C  6702          beq.s   $0000A090
      //  0000A08E  3F3C0000      move.w  #$0000,-(sp)    →  0000A08E  4267          clr.w   -(sp)
      //                                                  →  0000A090  4E04          emxnop
      //  0000A092
      if (MC68060.mmuPeekWordZeroData (0x0000a08c, 1) == 0x6704 &&
          MC68060.mmuPeekLongData (0x0000a08e, 1) == 0x3f3c0000) {
        MC68060.mmuPokeLongData (0x0000a08c, 0x67024267, 1);
        MC68060.mmuPokeWordData (0x0000a090, XEiJ.EMX_OPCODE_EMXNOP, 1);
        patched++;
      } else {
        failed++;
      }

      //仮想ディレクトリを展開して実体のドライブに移るときドライブ管理テーブルのアドレスを変更する命令のオペレーションサイズが間違っている(human302)
      //
      //  0000B2EA  324C          movea.w a4,a1            →  0000B2EA  224C          movea.l a4,a1
      if (MC68060.mmuPeekWordZeroData (0x0000b2ea, 1) == 0x324c) {
        MC68060.mmuPokeByteData (0x0000b2ea + 0, 0x22, 1);
        patched++;
      } else {
        failed++;
      }

      //ディレクトリを延長するときルートディレクトリかどうかを判断するためにセクタ番号をデータ部の先頭セクタ番号と比較するとき上位ワードを無視している(human215,human301,human302)
      //
      //                                                   →            7000          moveq.l #$00,d0
      //  0000B8E2  30280014      move.w  ($0014,a0),d0
      //  0000B8E6  B240          cmp.w   d0,d1            →            B280          cmp.l   d0,d1
      //  0000B8E8  6406          bcc.s   $0000B8F0
      //  0000B8EA  5241          addq.w  #$01,d1          →            5281          addq.l  #$01,d1
      //  0000B8EC  B240          cmp.w   d0,d1            →            B280          cmp.l   d0,d1
      //  0000B8EE  4E75          rts

      //サブのメモリ空間を削除するときサブの管理下で常駐したブロックをメインのメモリ空間からサブのメモリ空間に入る方向に繋いでいない(human215,human301,human302)

      //スレッドを切り替えるためのTimer-D割り込みルーチンがMC68030のコプロセッサ命令途中割り込みに対応していない(human215,human301,human302)

      //IOCTRL(19,1)でBPBテーブルをコピーする長さとPDAとイジェクトフラグを書き込む位置が間違っている(human215,human301,human302)
      //  00010B38  700B          moveq.l #$0B,d0          →  00010B38  700F          moveq.l #$0F,d0
      //  00010B3A  10DE          move.b  (a6)+,(a0)+
      //  00010B3C  51C8FFFC      dbra.w  d0,$00010B3A
      if (MC68060.mmuPeekWordZeroData (0x00010b38, 1) == 0x700b) {
        MC68060.mmuPokeByteData (0x00010b38 + 1, 0x0f, 1);
        patched++;
      } else {
        failed++;
      }
      //
      break;
    }

    //  Humanのline 1111 emulator/privilege violation例外処理ルーチンの手前にFLOATn.Xのマジック'FEfn'を押し込む
    //    手前にある_EXITVC/_CTRLVC/_ERRJVCのコードを詰めて隙間を作る
    //  Human自身がFLOATn.Xのマジックを持つことでFLOATn.Xを組み込めなくする
    //    シェルが正常終了した
    //      00008518  6140          bsr.s   $0000855A       1行改行
    //      0000851A  4879000111AE  pea.l   $000111AE.l     '終了しました。',$00
    //      00008520  FF09          DOS     _PRINT
    //      00008522  588F          addq.l  #$04,sp
    //      00008524  4879000111BD  pea.l   $000111BD.l     'コマンドを、入力してください',$0D,$0A,'#',$00
    //      0000852A  6006          bra.s   $00008532
    //    コマンド入力ループ
    //      0000852C  4879000111DB  pea.l   $000111DB.l     '#',$00
    //      00008532  FF09          DOS     _PRINT
    //          :
    //    _EXITVC/_CTRLVC/_ERRJVC
    //      00008566  FF81          DOS     _GETPDB         human203まではX68030に対応していないため_GETPDBは$FF51
    //      00008568  B0BC00008382  cmp.l   #$00008382,d0   Humanのプロセス管理テーブル
    //      0000856E  6626          bne.s   $00008596       →  0000856E  6622          bne.s   $00008592
    //    シェルが停止した
    //      00008570  4FF900008372  lea.l   $00008372.l,sp  スタック復元
    //      00008576  42A7          clr.l   -(sp)
    //      00008578  FF20          DOS     _SUPER
    //      0000857A  588F          addq.l  #$04,sp
    //      0000857C  207900011090  movea.l $00011090.l,a0  ユーザスタックエリアのアドレス
    //      00008582  41E800F0      lea.l   $00F0(a0),a0
    //      00008586  4E60          move.l  a0,usp
    //      00008588  61D0          bsr.s   $0000855A       1行改行
    //      0000858A  4879000111DD  pea.l   $000111DD.l     '停止しました。',$00
    //      00008590  FF09          DOS     _PRINT          →  00008590  608E          bra.s   $00008520
    //      00008592  588F          addq.l  #$04,sp         →  00008592  FF00          DOS     _EXIT
    //      00008594  608E          bra.s   $00008524       →  00008594  4645          .dc.w   'FE'
    //    シェル以外のプロセスが停止した
    //      00008596  FF00          DOS     _EXIT           →  00008596  666E          .dc.w   'fn'
    //    line 1111 emulator/privilege violation
    //      00008598  48E78006      movem.l d0/a5-a6,-(sp)  human203まではX68030に対応していないためline 1111 emulatorのコードが異なる
    if (FEFunction.fpkRejectFloatOn) {
      int fline = (mmrHumanVersion == 0x0302 ||
                   mmrHumanVersion == 0x025f ? 0x00008598 :
                   mmrHumanVersion == 0x0301 ? 0x000084f6 :
                   mmrHumanVersion == 0x020f ? 0x00008642 :
                   mmrHumanVersion == 0x0203 ? 0x00007f58 :
                   mmrHumanVersion == 0x0202 ? 0x00007fd6 :
                   mmrHumanVersion == 0x0201 ? 0x00007fb6 :
                   mmrHumanVersion == 0x0200 ? 0x00007fb6 :  //human200はhuman201と同じ
                   //human101とhuman100はコードが異なるのでここでは非対応とする
                   -1);
      if (fline > 0) {
        if (MC68060.mmuPeekWordZeroData (0x0000856e - 0x00008598 + fline, 1) == 0x6626 &&
            MC68060.mmuPeekLongData (0x00008590 - 0x00008598 + fline, 1) == 0xff09588f &&
            MC68060.mmuPeekLongData (0x00008594 - 0x00008598 + fline, 1) == 0x608eff00) {
          MC68060.mmuPokeWordData (0x0000856e - 0x00008598 + fline, 0x6622, 1);
          MC68060.mmuPokeLongData (0x00008590 - 0x00008598 + fline, 0x608eff00, 1);
          MC68060.mmuPokeLongData (0x00008594 - 0x00008598 + fline, 0x4645666e, 1);
          patched++;
        } else {
          failed++;
        }
      }
    }

    System.out.println (new StringBuilder ().
                        append ("Human68k version ").
                        append ((char) ('0' + (mmrHumanVersion >> 8) % 10)).
                        append ('.').
                        append ((char) ('0' + (mmrHumanVersion & 255) / 10)).
                        append ((char) ('0' + (mmrHumanVersion & 255) % 10)).
                        append (Multilingual.mlnJapanese ? " にパッチをあてました (" : " was patched (").
                        append (patched).
                        append ('/').
                        append (patched + failed).
                        append (')').toString ());
    //FEファンクション命令を有効にする
    mmrFEfuncActivated = FEFunction.fpkOn;
    if (mmrFEfuncActivated) {
      System.out.println (Multilingual.mlnJapanese ?
                          "FE ファンクション命令が有効になりました" :
                          "FE function instruction has been activated");
    }
  }  //mmrCheckHuman()

  //pc = mmrGetLevelZeroPC ()
  //  DOSコールにレベル0で入ったときのpcを返す。0=DOSコールの中でないか不明
  public static int mmrGetLevelZeroPC () {
    if (0x020f <= mmrHumanVersion) {  //Human 2.15以上
      int level = MC68060.mmuPeekWordZeroData (0x1c08, XEiJ.regSRS);  //DOSコールのレベル
      if (level != 0) {
        int ssp = MC68060.mmuPeekLongData (0x1c5c, XEiJ.regSRS);  //DOSコールにレベル0で入ったときのssp
        int pc = MC68060.mmuPeekLongData (ssp + 0x3a, XEiJ.regSRS);  //DOSコールにレベル0で入ったときのpc
        return pc;
      }
    }
    return 0;
  }  //mmrGetLevelZeroPC

}  //class MainMemory



