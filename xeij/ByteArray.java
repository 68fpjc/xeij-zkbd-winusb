//========================================================================================
//  ByteArray.java
//    en:Byte array manipulation -- It manipulates byte arrays.
//    ja:byte配列操作 -- byte配列を操作します。
//  Copyright (C) 2003-2023 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.io.*;
import java.util.zip.*;

public class ByteArray {

  //d = byaRbs (bb, a)
  //  リードバイト符号拡張
  //  インライン展開する
  public static byte byaRbs (byte[] bb, int a) {
    return bb[a];
  }  //byaRbs

  //d = byaRbz (bb, a)
  //  リードバイトゼロ拡張
  //  インライン展開する
  public static int byaRbz (byte[] bb, int a) {
    return bb[a] & 255;
  }  //byaRbz

  //d = byaRws (bb, a)
  //  リードワード符号拡張
  public static int byaRws (byte[] bb, int a) {
    return bb[a] << 8 | bb[a + 1] & 255;
  }  //byaRws

  //d = byaRiws (bb, a)
  //  リトルエンディアンリードワード符号拡張
  public static int byaRiws (byte[] bb, int a) {
    return bb[a + 1] << 8 | bb[a] & 255;
  }  //byaRiws

  //d = byaRwz (bb, a)
  //  リードワードゼロ拡張
  public static int byaRwz (byte[] bb, int a) {
    return (char) (bb[a] << 8 | bb[a + 1] & 255);
  }  //byaRwz

  //d = byaRiwz (bb, a)
  //  リトルエンディアンリードワードゼロ拡張
  public static int byaRiwz (byte[] bb, int a) {
    return (char) (bb[a + 1] << 8 | bb[a] & 255);
  }  //byaRiwz

  //d = byaRls (bb, a)
  //  リードロング符号拡張
  public static int byaRls (byte[] bb, int a) {
    return bb[a] << 24 | (bb[a + 1] & 255) << 16 | (char) (bb[a + 2] << 8 | bb[a + 3] & 255);
  }  //byaRls

  //d = byaRils (bb, a)
  //  リトルエンディアンリードロング符号拡張
  public static int byaRils (byte[] bb, int a) {
    return bb[a + 3] << 24 | (bb[a + 2] & 255) << 16 | (char) (bb[a + 1] << 8 | bb[a] & 255);
  }  //byaRils

  //d = byaRqs (bb, a)
  //  リードクワッド符号拡張
  public static long byaRqs (byte[] bb, int a) {
    return (long) byaRls (bb, a) << 32 | byaRls (bb, a + 4) & 0xffffffffL;
  }  //byaRqs

  //sb = byaRstr (sb, bb, a, l)
  //  リードストリング
  //  文字列をSJISから変換しながら読み出す
  //  対応する文字がないときは0xfffdになる
  //  制御コードは'.'になる
  public static StringBuilder byaRstr (StringBuilder sb, byte[] bb, int a, int l) {
    for (int i = 0; i < l; i++) {
      int s = bb[a + i] & 255;
      char c;
      if (0x81 <= s && s <= 0x9f || 0xe0 <= s && s <= 0xef) {  //SJISの2バイトコードの1バイト目
        int t = a + 1 < l ? bb[a + 1] & 255 : 0;
        if (0x40 <= t && t != 0x7f && t <= 0xfc) {  //SJISの2バイトコードの2バイト目
          c = CharacterCode.chrSJISToChar[s << 8 | t];  //2バイトで変換する
          if (c == 0) {  //対応する文字がない
            c = '\ufffd';
          }
          a++;
        } else {  //SJISの2バイトコードの2バイト目ではない
          c = '.';  //SJISの2バイトコードの1バイト目ではなかった
        }
      } else {  //SJISの2バイトコードの1バイト目ではない
        c = CharacterCode.chrSJISToChar[s];  //1バイトで変換する
        if (c == 0) {  //対応する文字がない
          c = '\ufffd';
        }
      }
      sb.append (c);
    }
    return sb;
  }  //byaRstr

  //byaWb (bb, a, d)
  //  ライトバイト
  public static void byaWb (byte[] bb, int a, int d) {
    bb[a    ] = (byte)  d;
  }  //byaWb

  //byaWw (bb, a, d)
  //  ライトワード
  public static void byaWw (byte[] bb, int a, int d) {
    bb[a    ] = (byte) (d >> 8);
    bb[a + 1] = (byte)  d;
  }  //byaWw

  //byaWwArray (bb, a, da)
  //  ライトワードアレイ
  public static void byaWwArray (byte[] bb, int a, int[] da) {
    for (int i = 0, l = da.length; i < l; i++) {
      int d = da[i];
      bb[a    ] = (byte) (d >> 8);
      bb[a + 1] = (byte)  d;
      a += 2;
    }
  }  //byaWwArray

  //byaWiw (bb, a, d)
  //  リトルエンディアンライトワード
  public static void byaWiw (byte[] bb, int a, int d) {
    bb[a + 1] = (byte) (d >> 8);
    bb[a    ] = (byte)  d;
  }  //byaWiw

  //byaWl (bb, a, d)
  //  ライトロング
  public static void byaWl (byte[] bb, int a, int d) {
    bb[a    ] = (byte) (d >> 24);
    bb[a + 1] = (byte) (d >> 16);
    bb[a + 2] = (byte) (d >>  8);
    bb[a + 3] = (byte)  d;
  }  //byaWl

  //byaWil (bb, a, d)
  //  リトルエンディアンライトロング
  public static void byaWil (byte[] bb, int a, int d) {
    bb[a + 3] = (byte) (d >> 24);
    bb[a + 2] = (byte) (d >> 16);
    bb[a + 1] = (byte) (d >>  8);
    bb[a    ] = (byte)  d;
  }  //byaWil

  //a = byaWstr (bb, a, s)
  //  ライトストリング
  //  文字列をSJISに変換しながら書き込む
  //  SJISに変換できない文字は'※'になる
  //  文字列の直後のアドレスを返す
  public static int byaWstr (byte[] bb, int a, String s) {
    int l = s.length ();
    for (int i = 0; i < l; i++) {
      int c = CharacterCode.chrCharToSJIS[s.charAt (i)];
      if (c == 0 && s.charAt (i) != '\0') {
        c = 0x81a6;  //'※'
      }
      if (c >> 8 != 0) {
        bb[a++] = (byte) (c >> 8);
      }
      bb[a++] = (byte) c;
    }
    return a;
  }  //byaWstr

  //byaCmp (bbx, ax, sx, bby, ay, sy)
  //  bbx[ax..ax+sx-1]-bby[ay..ay+sy-1]
  public static int byaCmp (byte[] bbx, int ax, int sx, byte[] bby, int ay, int sy) {
    int i;
    for (i = 0; i < sx && i < sy; i++) {
      int x = 0xff & bbx[ax + i];
      int y = 0xff & bby[ay + i];
      if (x != y) {
        return x - y;
      }
    }
    return sx - sy;
  }  //byaCmp

  //byaDump (bb, st, ed)
  //  バイトバッファをダンプする
  public static void byaDump (byte[] bb, int p, int q) {
    for (int p0 = p & -16, q0 = q + 15 & -16; p0 < q0; p0 += 1024) {
      StringBuilder sb = new StringBuilder ();  //StringBuilderは32MBを超えられないので適当な間隔で作り直す必要がある
      for (int p1 = p0, q1 = Math.min (p0 + 1024, q0); p1 < q1; p1 += 16) {
        XEiJ.fmtHex8 (sb, p1).append (' ');
        for (int p2 = p1, q2 = p1 + 16; p2 < q2; p2++) {
          if (p <= p2 && p2 < q) {
            XEiJ.fmtHex2 (sb.append (' '), bb[p2]);
          } else {
            sb.append (' ').append (' ').append (' ');
          }
        }
        sb.append (' ').append (' ');
        int h = 0;  //繰り越した文字
        for (int p2 = p1, q2 = p1 + 16; p2 < q2 || p2 == q2 && h != 0; p2++) {
          int l = p <= p2 && p2 < q ? bb[p2] & 255 : ' ';  //今回の文字
          if ((0x81 <= h && h <= 0x9f || 0xe0 <= h && h <= 0xef) &&  //繰り越した文字はSJISの1バイト目かつ
              (0x40 <= l && l != 0x7f && l <= 0xfc)) {  //今回の文字はSJISの2バイト目
            l |= h << 8;
            int c = CharacterCode.chrSJISToChar[l];
            if (c != 0) {  //SJISで変換できる
              sb.append ((char) c);
            } else {  //SJISだが変換できない
              sb.append ('※');
            }
            h = 0;
          } else {  //繰り越した文字と今回の文字を合わせてもSJISにならない
            if (h != 0) {  //繰り越した文字を吐き出す
              sb.append ('.');
              h = 0;
            }
            //この時点で繰り越した文字はない
            if (0x81 <= l && l <= 0x9f || 0xe0 <= l && l <= 0xef) {  //繰り越した文字がなく、今回の文字はSJISの1バイト目
              h = l;  //繰り越す
            } else {  //繰り越した文字がなく、今回の文字はSJISの1バイト目ではない
              int c = CharacterCode.chrSJISToChar[l];
              if (0x20 <= c && c != 0x7f) {  //ASCIIまたは半角カナ
                sb.append ((char) c);
              } else {  //SJISの1バイト目ではなく、ASCIIまたは半角カナでもない
                sb.append ('.');
              }
            }
          }
        }
        sb.append ('\n');
      }
      System.out.print (sb.toString ());
    }
  }  //byaDump

/*
  public static final char[] BYA_BASE64_ENCODE_TABLE = {
    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
    'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
    'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
    'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/',
  };
*/
  public static final char[] BYA_BASE64_ENCODE_TABLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray ();

  //s = byaEncodeBase64 (bb)
  //s = byaEncodeBase64 (bb, o, l)
  //  base64変換
  //  https://tools.ietf.org/html/rfc4648
  public static String byaEncodeBase64 (byte[] bb) {
    return byaEncodeBase64 (bb, 0, bb.length);
  }  //byaEncodeBase64(byte[])
  public static String byaEncodeBase64 (byte[] bb, int o, int l) {
    char[] w = new char[(l + 2) / 3 << 2];
    l += o - 2;
    int i, j;
    for (i = o, j = 0; i < l; i += 3, j += 4) {
      int c = bb[i] & 255;
      int d = bb[i + 1] & 255;
      int e = bb[i + 2] & 255;
      //cccccccc dddddddd eeeeeeee
      //cccccc ccdddd ddddee eeeeee
      w[j    ] = BYA_BASE64_ENCODE_TABLE[c >> 2];
      w[j + 1] = BYA_BASE64_ENCODE_TABLE[(c & 3) << 4 | d >> 4];
      w[j + 2] = BYA_BASE64_ENCODE_TABLE[(d & 15) << 2 | e >> 6];
      w[j + 3] = BYA_BASE64_ENCODE_TABLE[e & 63];
    }
    l += 2;
    if (i < l) {
      int c = bb[i] & 255;
      int d = i + 1 < l ? bb[i + 1] & 255 : 0;
      int e = i + 2 < l ? bb[i + 2] & 255 : 0;
      w[j    ] = BYA_BASE64_ENCODE_TABLE[c >> 2];
      w[j + 1] = BYA_BASE64_ENCODE_TABLE[(c & 3) << 4 | d >> 4];
      w[j + 2] = i + 1 < l ? BYA_BASE64_ENCODE_TABLE[(d & 15) << 2 | e >> 6] : '=';
      w[j + 3] = i + 2 < l ? BYA_BASE64_ENCODE_TABLE[e & 63] : '=';
    }
    return new String (w);
  }  //byaEncodeBase64

  static {
    if (false) {
      System.out.println (byaEncodeBase64 ("".getBytes (XEiJ.ISO_8859_1)));  //""
      System.out.println (byaEncodeBase64 ("f".getBytes (XEiJ.ISO_8859_1)));  //"Zg=="
      System.out.println (byaEncodeBase64 ("fo".getBytes (XEiJ.ISO_8859_1)));  //"Zm8="
      System.out.println (byaEncodeBase64 ("foo".getBytes (XEiJ.ISO_8859_1)));  //"Zm9v"
      System.out.println (byaEncodeBase64 ("foob".getBytes (XEiJ.ISO_8859_1)));  //"Zm9vYg=="
      System.out.println (byaEncodeBase64 ("fooba".getBytes (XEiJ.ISO_8859_1)));  //"Zm9vYmE="
      System.out.println (byaEncodeBase64 ("foobar".getBytes (XEiJ.ISO_8859_1)));  //"Zm9vYmFy"
    }
  }

/*
  public static final char[] BYA_BASE64_DECODE_TABLE = {
    //  +1  +2  +3  +4  +5  +6  +7  +8  +9  +a  +b  +c  +d  +e  +f
    64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64,  //00
    64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64,  //10
    64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 62, 64, 64, 64, 63,  //20
    52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 64, 64, 64, 64, 64, 64,  //30
    64,  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14,  //40
    15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 64, 64, 64, 64, 64,  //50
    64, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,  //60
    41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 64, 64, 64, 64, 64,  //70
  };
*/
  public static final char[] BYA_BASE64_DECODE_TABLE = "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@>@@@?456789:;<=@@@@@@@\0\1\2\3\4\5\6\7\b\t\n\13\f\r\16\17\20\21\22\23\24\25\26\27\30\31@@@@@@\32\33\34\35\36\37 !\"#$%&\'()*+,-./0123@@@@@".toCharArray ();

  //bb = byaDecodeBase64 (s)
  //  base64逆変換
  //  https://tools.ietf.org/html/rfc4648
  public static byte[] byaDecodeBase64 (String s) {
    char[] w = s.toCharArray ();
    int ll = s.length ();
    int l = 0;
    for (int i = 0; i < ll; i++) {
      char c = w[i];
      if (c <= 127 && (c = BYA_BASE64_DECODE_TABLE[c]) <= 63) {
        w[l++] = c;
      }
    }
    int l3 = l & 3;
    l -= l3;
    byte[] bb = new byte[(l >> 2) * 3 + (l3 <= 1 ? 0 : l3 - 1)];
    int i, j;
    for (i = 0, j = 0; i < l; i += 4, j += 3) {
      char c = w[i];
      char d = w[i + 1];
      char e = w[i + 2];
      char f = w[i + 3];
      //cccccc dddddd eeeeee ffffff
      //ccccccdd ddddeeee eeffffff
      bb[j] = (byte) (c << 2 | d >> 4);
      bb[j + 1] = (byte) (d << 4 | e >> 2);
      bb[j + 2] = (byte) (e << 6 | f);
    }
    if (l3 >= 2) {
      char c = w[i];
      char d = w[i + 1];
      bb[j] = (byte) (c << 2 | d >> 4);
      if (l3 >= 3) {
        char e = w[i + 2];
        bb[j + 1] = (byte) (d << 4 | e >> 2);
      }
    }
    return bb;
  }  //byaDecodeBase64

  static {
    if (false) {
      try {
        System.out.println (new String (byaDecodeBase64 (""), "ISO_8859_1"));  //""
        System.out.println (new String (byaDecodeBase64 ("Zg=="), "ISO_8859_1"));  //"f"
        System.out.println (new String (byaDecodeBase64 ("Zm8="), "ISO_8859_1"));  //"fo"
        System.out.println (new String (byaDecodeBase64 ("Zm9v"), "ISO_8859_1"));  //"foo"
        System.out.println (new String (byaDecodeBase64 ("Zm9vYg=="), "ISO_8859_1"));  //"foob"
        System.out.println (new String (byaDecodeBase64 ("Zm9vYmE="), "ISO_8859_1"));  //"fooba"
        System.out.println (new String (byaDecodeBase64 ("Zm9vYmFy"), "ISO_8859_1"));  //"foobar"
      } catch (UnsupportedEncodingException uee) {
      }
    }
  }

  //bb = byaEncodeGzip (bb)
  //bb = byaEncodeGzip (bb, o, l)
  //  gzip圧縮
  public static byte[] byaEncodeGzip (byte[] bb) {
    return byaEncodeGzip (bb, 0, bb.length);
  }  //byaEncodeGzip(byte[])
  public static byte[] byaEncodeGzip (byte[] bb, int o, int l) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream ();
    try (GZIPOutputStream gos = new GZIPOutputStream (baos) {
      {
        //def.setLevel (Deflater.BEST_COMPRESSION);  //991467
        def.setLevel (Deflater.DEFAULT_COMPRESSION);  //995563
        //def.setLevel (Deflater.BEST_SPEED);  //1119763
      }
    }) {
      gos.write (bb, o, l);
      gos.flush ();
    } catch (IOException ioe) {
    }
    return baos.toByteArray ();
  }  //byaEncodeGzip

  //bb = byaDecodeGzip (bb)
  //bb = byaDecodeGzip (bb, o, l)
  //  gzip解凍
  public static byte[] byaDecodeGzip (byte[] bb) {
    return byaDecodeGzip (bb, 0, bb.length);
  }  //byaDecodeGzip(byte[])
  public static byte[] byaDecodeGzip (byte[] bb, int o, int l) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream ();
    try (GZIPInputStream gis = new GZIPInputStream (new ByteArrayInputStream (bb, o, l))) {
      int tl = 4096;
      byte[] tbb = new byte[tl];
      for (int k = gis.read (tbb, 0, tl); k >= 0; k = gis.read (tbb, 0, tl)) {
        baos.write (tbb, 0, k);
      }
    } catch (IOException ioe) {
    }
    return baos.toByteArray ();
  }  //byaDecodeGzip

  public static int crc32 (byte[] b) {
    return crc32 (b, 0, b.length);
  }
  public static int crc32 (byte[] b, int off, int len) {
    CRC32 crc32 = new CRC32 ();
    crc32.reset ();
    crc32.update (b, off, len);
    return (int) crc32.getValue ();
  }

}  //class ByteArray



