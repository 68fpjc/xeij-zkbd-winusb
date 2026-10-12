//========================================================================================
//  TextCopy.java
//    en:Text screen copy
//    ja:テキスト画面コピー
//  Copyright (C) 2003-2024 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.awt.event.*;
import javax.swing.*;

//class TextCopy
//  テキスト画面コピー
public class TextCopy {

  public static final int TXC_AREA_DISPLAY = 0;  //表示範囲
  public static final int TXC_AREA_C_WIDTH = 1;  //C_WIDTH
  public static final int TXC_AREA_VRAM = 2;  //VRAM全体
  public static final int TXC_AREA_ENCLOSED = 3;  //最後に囲んだ範囲
  public static int txcAreaMode;  //範囲モード
  public static boolean txcEncloseEachTime;  //true=マウスで都度囲む

  //class HanPat
  //  半角パターン
  static class HanPat {
    int c;  //文字コード
    int p0, p1, p2, p3;  //フォントパターン
    HanPat next;  //同じハッシュコードを持つ次のフォントパターン
  }

  //class ZenPat
  //  全角パターン
  static class ZenPat {
    int c;  //文字コード
    int p0, p1, p2, p3, p4, p5, p6, p7;  //パターン
    ZenPat next;  //同じハッシュコードを持つ次のパターン
  }

  static HanPat[] hanTable;  //半角ハッシュテーブル
  static ZenPat[] zenTable;  //全角ハッシュテーブル
  static int pressedX, pressedY;  //マウスの左ボタンが押された位置の画面座標。-1=なし
  static int enclosedX1, enclosedY1, enclosedX2, enclosedY2;  //最後に囲んだ範囲

  public static int txcRow1, txcRow2, txcCol1, txcCol2;  //現在選択されている範囲。-1=なし

  //txcInit ()
  //  初期化
  public static void txcInit () {
    String v = Settings.sgsGetString ("textcopyarea").toLowerCase ();
    txcAreaMode = (v.equals ("display") ? TXC_AREA_DISPLAY :
                   v.equals ("c_width") ? TXC_AREA_C_WIDTH :
                   v.equals ("vram") ? TXC_AREA_VRAM :
                   v.equals ("enclosed") ? TXC_AREA_ENCLOSED :
                   TXC_AREA_DISPLAY);
    txcEncloseEachTime = Settings.sgsGetOnOff ("textcopy");
    pressedX = -1;
    pressedY = -1;
    enclosedX1 = 0;
    enclosedY1 = 0;
    enclosedX2 = 0;
    enclosedY2 = 0;
    txcRow1 = -1;
    txcRow2 = -1;
    txcCol1 = -1;
    txcCol2 = -1;
  }  //txcInit

  //txcTini ()
  //  後始末
  public static void txcTini () {
    Settings.sgsPutString ("textcopyarea",
                           txcAreaMode == TXC_AREA_DISPLAY ? "display" :
                           txcAreaMode == TXC_AREA_C_WIDTH ? "c_width" :
                           txcAreaMode == TXC_AREA_VRAM ? "vram" :
                           txcAreaMode == TXC_AREA_ENCLOSED ? "enclosed" :
                           "display");
    Settings.sgsPutOnOff ("textcopy", txcEncloseEachTime);
  }  //txcTini

  //txcMakeMenuItem ()
  //  テキスト画面コピーメニューアイテムを作る
  public static JMenuItem txcMakeMenuItem () {
    return ComponentFactory.setEnabled (
      Multilingual.mlnText (
        ComponentFactory.createMenuItem (
          "Text screen copy", 'C', XEiJ.MNB_MODIFIERS,
          new ActionListener () {
            @Override public void actionPerformed (ActionEvent ae) {
              txcCopy ();
            }
          }),
        "ja", "テキスト画面コピー"),
      XEiJ.clpClipboard != null);
  }  //txcMakeMenuItem

  //txcMakeSettingMenu ()
  //  テキスト画面コピー設定メニューを作る
  public static JMenu txcMakeSettingMenu () {
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        String command = ae.getActionCommand ();
        switch (command) {
        case "Display area":  //表示領域
          txcAreaMode = TXC_AREA_DISPLAY;
          break;
        case "C_WIDTH":
          txcAreaMode = TXC_AREA_C_WIDTH;
          break;
        case "Entire VRAM":  //VRAM全体
          txcAreaMode = TXC_AREA_VRAM;
          break;
        case "Last enclosed area":  //最後に囲んだ範囲
          txcAreaMode = TXC_AREA_ENCLOSED;
          break;
        case "Enclose each time with mouse":  //マウスで都度囲む
          txcEncloseEachTime = ((JCheckBoxMenuItem) source).isSelected ();
          break;
        }
      }
    };
    ButtonGroup areaGroup = new ButtonGroup ();
    return ComponentFactory.setEnabled (
      Multilingual.mlnText (
        ComponentFactory.createMenu (
          "Text screen copy setting",
          Multilingual.mlnText (
            ComponentFactory.createRadioButtonMenuItem (areaGroup, txcAreaMode == TXC_AREA_DISPLAY, "Display area", listener),
            "ja", "表示領域"),
          ComponentFactory.createRadioButtonMenuItem (areaGroup, txcAreaMode == TXC_AREA_C_WIDTH, "C_WIDTH", listener),
          Multilingual.mlnText (
            ComponentFactory.createRadioButtonMenuItem (areaGroup, txcAreaMode == TXC_AREA_VRAM, "Entire VRAM", listener),
            "ja", "VRAM 全体"),
          Multilingual.mlnText (
            ComponentFactory.createRadioButtonMenuItem (areaGroup, txcAreaMode == TXC_AREA_ENCLOSED, "Last enclosed area", listener),
            "ja", "最後に囲んだ範囲"),
          ComponentFactory.createHorizontalSeparator (),
          Multilingual.mlnText (
            ComponentFactory.createCheckBoxMenuItem (txcEncloseEachTime, "Enclose each time with mouse", listener),
            "ja", "マウスで都度囲む")
          ),
        "ja", "テキスト画面コピー設定"),
      XEiJ.clpClipboard != null);
  }  //txcMakeSettingMenu

  //txcReset ()
  //  リセット
  //  ハッシュテーブルを破棄する
  public static void txcReset () {
    hanTable = null;
    zenTable = null;
  }  //txcReset



  //txcMousePressed (screenX, screenY)
  //  マウスの左ボタンが押された
  public static void txcMousePressed (int screenX, int screenY) {
    if ((VideoController.vcnReg3Curr & (1 << 5)) == 0) {  //テキスト画面が表示されていない
      return;  //何もしない
    }
    screenX = Math.max (0, Math.min (XEiJ.pnlScreenWidth - 1, screenX));
    screenY = Math.max (0, Math.min (XEiJ.pnlScreenHeight - 1, screenY));
    pressedX = screenX;  //押された位置の画面座標を保存する
    pressedY = screenY;
  }  //txcMousePressed

  //txcMouseMoved (screenX, screenY)
  //  マウスが動いた
  public static void txcMouseMoved (int screenX, int screenY) {
    if ((VideoController.vcnReg3Curr & (1 << 5)) == 0) {  //テキスト画面が表示されていない
      return;  //何もしない
    }
    if (pressedX < 0) {  //押されていない
      return;  //何もしない
    }
    screenX = Math.max (0, Math.min (XEiJ.pnlScreenWidth - 1, screenX));
    screenY = Math.max (0, Math.min (XEiJ.pnlScreenHeight - 1, screenY));
    calcLocation (screenX, screenY);
    if (0 <= txcRow1) {  //選択あり
      XEiJ.pnlPanel.repaint ();
    }
  }  //txcMouseMoved

  //txcMouseReleased (screenX, screenY)
  //  マウスの左ボタンが離された
  public static void txcMouseReleased (int screenX, int screenY) {
    if ((VideoController.vcnReg3Curr & (1 << 5)) == 0) {  //テキスト画面が表示されていない
      return;  //何もしない
    }
    if (pressedX < 0) {  //押されていない
      return;  //何もしない
    }
    screenX = Math.max (0, Math.min (XEiJ.pnlScreenWidth - 1, screenX));
    screenY = Math.max (0, Math.min (XEiJ.pnlScreenHeight - 1, screenY));
    calcLocation (screenX, screenY);
    if (0 <= txcRow1) {  //選択あり
      XEiJ.clpCopy (getText (txcRow1, txcRow2, txcCol1, txcCol2));  //テキスト画面から文字列を読み取ってクリップボードにコピーする
    }
    pressedX = -1;
    pressedY = -1;
    txcRow1 = -1;
    txcRow2 = -1;
    txcCol1 = -1;
    txcCol2 = -1;
    XEiJ.pnlPanel.repaint ();
  }  //txcMouseReleased

  //calcLocation ()
  //  選択範囲を求める
  static void calcLocation (int screenX, int screenY) {
    int x1 = pressedX;  //押された位置の画面座標
    int y1 = pressedY;
    int x2 = screenX;  //現在位置の画面座標
    int y2 = screenY;
    if (x2 < x1) {  //x1<=x2にする
      int t = x1;
      x1 = x2;
      x2 = t;
    }
    if (y2 < y1) {  //y1<=y2にする
      int t = y1;
      y1 = y2;
      y2 = t;
    }
    x1 += CRTC.crtR10TxXCurr;  //VRAM座標の開始位置
    y1 += CRTC.crtR11TxYCurr;
    x2 += CRTC.crtR10TxXCurr;  //VRAM座標の終了位置。1024x1024を超える場合がある
    y2 += CRTC.crtR11TxYCurr;
    int col1 = (x1 + 4) >> 3;  //開始桁。桁の右半分にあるときその桁を含まない
    int col2 = (x2 - 4) >> 3;  //終了桁。桁の左半分にあるときその桁を含まない
    int row1 = (y1 + 8) >> 4;  //開始行。行の下半分にあるときその行を含まない
    int row2 = (y2 - 8) >> 4;  //終了行。行の上半分にあるときその行を含まない
    if (col1 <= col2 && row1 <= row2) {  //1桁以上かつ1行以上
      enclosedX1 = x1;
      enclosedY1 = y1;
      enclosedX2 = x2;
      enclosedY2 = y2;
      txcCol1 = col1;
      txcCol2 = col2;
      txcRow1 = row1;
      txcRow2 = row2;
    } else {  //さもなくば
      txcCol1 = -1;
      txcCol2 = -1;
      txcRow1 = -1;
      txcRow2 = -1;
    }
  }  //calcLocation



  //txcCopy ()
  //  コピー
  public static void txcCopy () {
    int x1 = CRTC.crtR10TxXCurr;  //左上ドット座標
    int y1 = CRTC.crtR11TxYCurr;
    int x2, y2;  //右下ドット座標
    if (txcAreaMode == TXC_AREA_DISPLAY) {  //表示範囲
      int w = (CRTC.crtR03HDispEndCurr - CRTC.crtR02HBackEndCurr) << 3;
      int h = (CRTC.crtR07VDispEndCurr - CRTC.crtR06VBackEndCurr);
      if (CRTC.crtDuplication || CRTC.crtDupExceptSp) {  //ラスタ2度読み
        h >>= 1;
      } else if (CRTC.crtInterlace) {  //インターレース
        h <<= 1;
      }
      x2 = x1 + w - 1;
      y2 = y1 + h - 1;
    } else if (txcAreaMode == TXC_AREA_C_WIDTH) {  //C_WIDTH
      //CONCTRL(16,n)
      //  0 768x512 text
      //  1 768x512 text+graphic16
      //  2 512x512 text
      //  3 512x512 text+graphic16
      //  4 512x512 text+graphic256
      //  5 512x512 text+graphic65536
      int v = MainMemory.mmrHumanVersion;
      int a = (v == 0x0302 ? 0x6800 + 0xa890 + 0x29b1 :
               v == 0x0301 ? 0x6800 + 0xa77a + 0x29b1 :
               v == 0x020f ? 0x6800 + 0xa822 + 0x29b1 :
               v == 0x0203 ? 0x6800 + 0x96dc + 0x29af :
               v == 0x0202 ? 0x6800 + 0x9950 + 0x29af :
               v == 0x0201 ? 0x6800 + 0x9910 + 0x29af :
               v == 0x0200 ? 0x6800 + 0x9910 + 0x29af :
               //!!! 0x0101 0x0100
               -1);
      if (a < 0) {
        return;
      }
      int n = MC68060.mmuPeekByteZeroData (a, 1);
      if (0 <= n && n <= 1) {  //768x512
        x2 = x1 + 768 - 1;
        y2 = y1 + 512 - 1;
      } else if (2 <= n && n <= 5) {  //512x512
        x2 = x1 + 512 - 1;
        y2 = y1 + 512 - 1;
      } else {
        return;
      }
    } else if (txcAreaMode == TXC_AREA_VRAM) {  //VRAM全体
      x2 = x1 + 1024 - 1;
      y2 = y1 + 1024 - 1;
    } else if (txcAreaMode == TXC_AREA_ENCLOSED) {  //最後に囲んだ範囲
      x1 = enclosedX1;  //初回は0,0,0,0なのでNO DATAになる
      y1 = enclosedY1;
      x2 = enclosedX2;
      y2 = enclosedY2;
    } else {
      return;
    }
    int col1 = (x1 + 4) >> 3;  //開始桁。桁の右半分にあるときその桁を含まない
    int col2 = (x2 - 4) >> 3;  //終了桁。桁の左半分にあるときその桁を含まない
    int row1 = (y1 + 8) >> 4;  //開始行。行の下半分にあるときその行を含まない
    int row2 = (y2 - 8) >> 4;  //終了行。行の上半分にあるときその行を含まない
    //テキスト画面から文字列を読み取ってクリップボードにコピーする
    XEiJ.clpCopy (getText (row1, row2, col1, col2));
  }  //txcCopy



  //makeHash ()
  //  ROMフォントからハッシュテーブルを作る
  static void makeHash () {
    byte[] m = MainMemory.mmrM8;
    hanTable = new HanPat[65536];
    zenTable = new ZenPat[65536];
    //半角
    for (int s = 0xff; 0x20 <= s; s--) {  //SJISコード
      int c = CharacterCode.chrSJISToChar[
        s == 0x80 ? 0x5c :  //＼→￥
        s == 0x81 ? 0x7e :  //～→￣
        s == 0x82 ? 0x7c :  //￤→｜
        (0x86 <= s && s <= 0x9f && s != 0x90) || (0xe0 <= s && s <= 0xfd) ? s ^ 0x20 :  //ひらがな→カタカナ
        s];  //文字コード
      if (c == 0) {
        continue;
      }
      for (int n = 2; 0 <= n; n--) {  //種類。0=半角,1=上付き1/4角,2=下付き1/4角
        int a;  //フォントアドレス
        int p0, p1, p2, p3;  //フォントパターン
        int h;  //ハッシュコード
        if (n == 0) {  //半角
          a = 0x00f3a800 + (s << 4);  //8x16フォントアドレス
        } else {  //1/4角
          a = 0x00f3a000 + (s << 3);  //8x8フォントアドレス
        }
        p0 = ((m[a     ]      ) << 24 |
              (m[a +  1] & 255) << 16 |
              (m[a +  2] & 255) <<  8 |
              (m[a +  3] & 255));
        p1 = ((m[a +  4]      ) << 24 |
              (m[a +  5] & 255) << 16 |
              (m[a +  6] & 255) <<  8 |
              (m[a +  7] & 255));
        if (n == 0) {  //半角
          p2 = ((m[a +  8]      ) << 24 |
                (m[a +  9] & 255) << 16 |
                (m[a + 10] & 255) <<  8 |
                (m[a + 11] & 255));
          p3 = ((m[a + 12]      ) << 24 |
                (m[a + 13] & 255) << 16 |
                (m[a + 14] & 255) <<  8 |
                (m[a + 15] & 255));
        } else if (n == 1) {  //上付き1/4角
          p2 = 0;
          p3 = 0;
        } else {  //下付き1/4角
          p2 = p0;
          p3 = p1;
          p0 = 0;
          p1 = 0;
        }
        if ((p0 | p1 | p2 | p3) == 0 ||
            (p0 & p1 & p2 & p3) == -1) {  //空白
          continue;
        }
        for (int u = 3; 0 <= u; u--) {  //装飾。0=ノーマル,1=太字,2=反転,3=太字反転
          int q0 = p0, q1 = p1, q2 = p2, q3 = p3;  //装飾されたフォントパターン
          if ((u & 1) != 0) {  //太字
            q0 |= (q0 >> 1) & 0x7f7f7f7f;
            q1 |= (q1 >> 1) & 0x7f7f7f7f;
            q2 |= (q2 >> 1) & 0x7f7f7f7f;
            q3 |= (q3 >> 1) & 0x7f7f7f7f;
          }
          if ((u & 2) != 0) {  //反転
            q0 = ~q0;
            q1 = ~q1;
            q2 = ~q2;
            q3 = ~q3;
          }
          h = ((q0 * 31 + q1) * 31 + q2) * 31 + q3;
          h = ((h >> 16) + h) & 65535;  //装飾されたフォントパターンのハッシュコード
          HanPat pat = new HanPat ();
          pat.c = c;  //文字コード
          pat.p0 = q0;  //装飾されたフォントパターン
          pat.p1 = q1;
          pat.p2 = q2;
          pat.p3 = q3;
          pat.next = hanTable[h];  //リストの先頭に挿入する
          hanTable[h] = pat;
        }  //for m
      }  //for n
    }  //for s
    //全角
    for (int row = 77 - 1; 0 <= row; row--) {  //行
      for (int col = 94 - 1; 0 <= col; col--) {  //列
        int kum1 = row < 8 ? row : row + 7;
        int sh = kum1 >> 1;
        int sl = 94 * (kum1 & 1) + col;
        sh += 0x81;
        sl += 0x40;
        if (0xa0 <= sh) {
          sh += 0xe0 - 0xa0;
        }
        if (0x7f <= sl) {
          sl += 0x80 - 0x7f;
        }
        int s = (sh << 8) | sl;  //SJISコード
        int c = CharacterCode.chrSJISToChar[s];  //文字コード
        if (c == 0) {
          continue;
        }
        int a = 0x00f00000 + ((col + 94 * row) << 5);  //16x16フォントアドレス
        int p0 = ((m[a     ]      ) << 24 | (m[a +  1] & 255) << 16 |
                  (m[a +  2] & 255) <<  8 | (m[a +  3] & 255));
        int p1 = ((m[a +  4]      ) << 24 | (m[a +  5] & 255) << 16 |
                  (m[a +  6] & 255) <<  8 | (m[a +  7] & 255));
        int p2 = ((m[a +  8]      ) << 24 | (m[a +  9] & 255) << 16 |
                  (m[a + 10] & 255) <<  8 | (m[a + 11] & 255));
        int p3 = ((m[a + 12]      ) << 24 | (m[a + 13] & 255) << 16 |
                  (m[a + 14] & 255) <<  8 | (m[a + 15] & 255));
        int p4 = ((m[a + 16]      ) << 24 | (m[a + 17] & 255) << 16 |
                  (m[a + 18] & 255) <<  8 | (m[a + 19] & 255));
        int p5 = ((m[a + 20]      ) << 24 | (m[a + 21] & 255) << 16 |
                  (m[a + 22] & 255) <<  8 | (m[a + 23] & 255));
        int p6 = ((m[a + 24]      ) << 24 | (m[a + 25] & 255) << 16 |
                  (m[a + 26] & 255) <<  8 | (m[a + 27] & 255));
        int p7 = ((m[a + 28]      ) << 24 | (m[a + 29] & 255) << 16 |
                  (m[a + 30] & 255) <<  8 | (m[a + 31] & 255));
        if ((p0 | p1 | p2 | p3 | p4 | p5 | p6 | p7) == 0 ||
            (p0 & p1 & p2 & p3 & p4 & p5 & p6 & p7) == -1) {  //空白
          continue;
        }
        for (int u = 3; 0 <= u; u--) {  //装飾。0=ノーマル,1=太字,2=反転,3=太字反転
          int q0 = p0, q1 = p1, q2 = p2, q3 = p3, q4 = p4, q5 = p5, q6 = p6, q7 = p7;  //装飾されたフォントパターン
          if ((u & 1) != 0) {  //太字
            q0 |= (q0 >> 1) & 0x7fff7fff;
            q1 |= (q1 >> 1) & 0x7fff7fff;
            q2 |= (q2 >> 1) & 0x7fff7fff;
            q3 |= (q3 >> 1) & 0x7fff7fff;
            q4 |= (q4 >> 1) & 0x7fff7fff;
            q5 |= (q5 >> 1) & 0x7fff7fff;
            q6 |= (q6 >> 1) & 0x7fff7fff;
            q7 |= (q7 >> 1) & 0x7fff7fff;
          }
          if ((u & 2) != 0) {  //反転
            q0 = ~q0;
            q1 = ~q1;
            q2 = ~q2;
            q3 = ~q3;
            q4 = ~q4;
            q5 = ~q5;
            q6 = ~q6;
            q7 = ~q7;
          }
          int h = ((((((q0 * 31 + q1) * 31 + q2) * 31 + q3) * 31 + q4) * 31 + q5) * 31 + q6) * 31 + q7;
          h = ((h >> 16) + h) & 65535;  //装飾されたフォントパターンのハッシュコード
          ZenPat pat = new ZenPat ();
          pat.c = c;  //文字コード
          pat.p0 = q0;  //装飾されたフォントパターン
          pat.p1 = q1;
          pat.p2 = q2;
          pat.p3 = q3;
          pat.p4 = q4;
          pat.p5 = q5;
          pat.p6 = q6;
          pat.p7 = q7;
          pat.next = zenTable[h];  //リストの先頭に挿入する
          zenTable[h] = pat;
        }  //for m
      }  //for col
    }  //for row
  }  //makeHash

  //getText (row1, row2, col1, col2)
  //  テキスト画面から文字列を読み取る
  //  球面スクロールに対応する。VRAMは128桁64行だが超えていても構わない
  public static String getText (int row1, int row2, int col1, int col2) {
    int cols = col2 - col1 + 1;  //桁数
    int rows = row2 - row1 + 1;  //行数
    if (cols <= 0 && rows <= 0) {  //範囲がないとき
      return "NO DATA";  //何もせず"NO DATA"を返す
    }
    int[] co = new int[cols + 1];  //範囲内のVRAMの水平方向のオフセットの配列
    for (int c = 0; c < co.length; c++) {
      co[c] = (col1 + c) & 127;
    }
    int[] ra = new int[rows + 15];  //範囲内のVRAMの垂直方向のアドレスの配列
    for (int r = 0; r < ra.length; r++) {
      ra[r] = 0x00e00000 + (((row1 + r) & 63) << 11);
    }
    //
    if (hanTable == null) {
      makeHash ();
    }
    byte[] m = MainMemory.mmrM8;
    StringBuilder sb = new StringBuilder ();
    for (int r = 0; r < rows; r++) {  //範囲内の行
      int a = ra[r];
      if (0 < r) {
        sb.append ('\n');
      }
      for (int c = 0; c < cols; c++) {  //範囲内の桁
        //全角
        if (c + 1 < cols) {  //右端でない
          int a0 = a + co[c];
          int a1 = a + co[c + 1];
          int p0 = ((m[a0            ]      ) << 24 | (m[a1            ] & 255) << 16 |
                    (m[a0 + ( 1 << 7)] & 255) <<  8 | (m[a1 + ( 1 << 7)] & 255));
          int p1 = ((m[a0 + ( 2 << 7)]      ) << 24 | (m[a1 + ( 2 << 7)] & 255) << 16 |
                    (m[a0 + ( 3 << 7)] & 255) <<  8 | (m[a1 + ( 3 << 7)] & 255));
          int p2 = ((m[a0 + ( 4 << 7)]      ) << 24 | (m[a1 + ( 4 << 7)] & 255) << 16 |
                    (m[a0 + ( 5 << 7)] & 255) <<  8 | (m[a1 + ( 5 << 7)] & 255));
          int p3 = ((m[a0 + ( 6 << 7)]      ) << 24 | (m[a1 + ( 6 << 7)] & 255) << 16 |
                    (m[a0 + ( 7 << 7)] & 255) <<  8 | (m[a1 + ( 7 << 7)] & 255));
          int p4 = ((m[a0 + ( 8 << 7)]      ) << 24 | (m[a1 + ( 8 << 7)] & 255) << 16 |
                    (m[a0 + ( 9 << 7)] & 255) <<  8 | (m[a1 + ( 9 << 7)] & 255));
          int p5 = ((m[a0 + (10 << 7)]      ) << 24 | (m[a1 + (10 << 7)] & 255) << 16 |
                    (m[a0 + (11 << 7)] & 255) <<  8 | (m[a1 + (11 << 7)] & 255));
          int p6 = ((m[a0 + (12 << 7)]      ) << 24 | (m[a1 + (12 << 7)] & 255) << 16 |
                    (m[a0 + (13 << 7)] & 255) <<  8 | (m[a1 + (13 << 7)] & 255));
          int p7 = ((m[a0 + (14 << 7)]      ) << 24 | (m[a1 + (14 << 7)] & 255) << 16 |
                    (m[a0 + (15 << 7)] & 255) <<  8 | (m[a1 + (15 << 7)] & 255));
          if ((p0 | p1 | p2 | p3 | p4 | p5 | p6 | p7) == 0 ||
              (p0 & p1 & p2 & p3 & p4 & p5 & p6 & p7) == -1) {  //空白
            a0 += 0x00020000;  //ページ1
            a1 += 0x00020000;
            p0 = ((m[a0            ]      ) << 24 | (m[a1            ] & 255) << 16 |
                  (m[a0 + ( 1 << 7)] & 255) <<  8 | (m[a1 + ( 1 << 7)] & 255));
            p1 = ((m[a0 + ( 2 << 7)]      ) << 24 | (m[a1 + ( 2 << 7)] & 255) << 16 |
                  (m[a0 + ( 3 << 7)] & 255) <<  8 | (m[a1 + ( 3 << 7)] & 255));
            p2 = ((m[a0 + ( 4 << 7)]      ) << 24 | (m[a1 + ( 4 << 7)] & 255) << 16 |
                  (m[a0 + ( 5 << 7)] & 255) <<  8 | (m[a1 + ( 5 << 7)] & 255));
            p3 = ((m[a0 + ( 6 << 7)]      ) << 24 | (m[a1 + ( 6 << 7)] & 255) << 16 |
                  (m[a0 + ( 7 << 7)] & 255) <<  8 | (m[a1 + ( 7 << 7)] & 255));
            p4 = ((m[a0 + ( 8 << 7)]      ) << 24 | (m[a1 + ( 8 << 7)] & 255) << 16 |
                  (m[a0 + ( 9 << 7)] & 255) <<  8 | (m[a1 + ( 9 << 7)] & 255));
            p5 = ((m[a0 + (10 << 7)]      ) << 24 | (m[a1 + (10 << 7)] & 255) << 16 |
                  (m[a0 + (11 << 7)] & 255) <<  8 | (m[a1 + (11 << 7)] & 255));
            p6 = ((m[a0 + (12 << 7)]      ) << 24 | (m[a1 + (12 << 7)] & 255) << 16 |
                  (m[a0 + (13 << 7)] & 255) <<  8 | (m[a1 + (13 << 7)] & 255));
            p7 = ((m[a0 + (14 << 7)]      ) << 24 | (m[a1 + (14 << 7)] & 255) << 16 |
                  (m[a0 + (15 << 7)] & 255) <<  8 | (m[a1 + (15 << 7)] & 255));
            if ((p0 | p1 | p2 | p3 | p4 | p5 | p6 | p7) == 0 ||
                (p0 & p1 & p2 & p3 & p4 & p5 & p6 & p7) == -1) {  //空白
              sb.append ("  ");
              c++;
              continue;
            }
          }
          int h = ((((((p0 * 31 + p1) * 31 + p2) * 31 + p3) * 31 + p4) * 31 + p5) * 31 + p6) * 31 + p7;
          h = ((h >> 16) + h) & 65535;  //ハッシュコード
          ZenPat pat = zenTable[h];
          while (pat != null) {  //リストを辿る
            if (pat.p0 == p0 &&
                pat.p1 == p1 &&
                pat.p2 == p2 &&
                pat.p3 == p3 &&
                pat.p4 == p4 &&
                pat.p5 == p5 &&
                pat.p6 == p6 &&
                pat.p7 == p7) {
              break;
            }
            pat = pat.next;
          }
          if (pat != null) {  //見つかった
            sb.append ((char) pat.c);
            c++;
            continue;
          }
        }  //if 右端でない
        //半角
        {
          int a0 = a + co[c];
          int p0 = ((m[a0            ]      ) << 24 |
                    (m[a0 + ( 1 << 7)] & 255) << 16 |
                    (m[a0 + ( 2 << 7)] & 255) <<  8 |
                    (m[a0 + ( 3 << 7)] & 255));
          int p1 = ((m[a0 + ( 4 << 7)]      ) << 24 |
                    (m[a0 + ( 5 << 7)] & 255) << 16 |
                    (m[a0 + ( 6 << 7)] & 255) <<  8 |
                    (m[a0 + ( 7 << 7)] & 255));
          int p2 = ((m[a0 + ( 8 << 7)]      ) << 24 |
                    (m[a0 + ( 9 << 7)] & 255) << 16 |
                    (m[a0 + (10 << 7)] & 255) <<  8 |
                    (m[a0 + (11 << 7)] & 255));
          int p3 = ((m[a0 + (12 << 7)]      ) << 24 |
                    (m[a0 + (13 << 7)] & 255) << 16 |
                    (m[a0 + (14 << 7)] & 255) <<  8 |
                    (m[a0 + (15 << 7)] & 255));
          if ((p0 | p1 | p2 | p3) == 0 ||
              (p0 & p1 & p2 & p3) == -1) {  //空白
            a0 += 0x00020000;  //ページ1
            p0 = ((m[a0            ]      ) << 24 |
                  (m[a0 + ( 1 << 7)] & 255) << 16 |
                  (m[a0 + ( 2 << 7)] & 255) <<  8 |
                  (m[a0 + ( 3 << 7)] & 255));
            p1 = ((m[a0 + ( 4 << 7)]      ) << 24 |
                  (m[a0 + ( 5 << 7)] & 255) << 16 |
                  (m[a0 + ( 6 << 7)] & 255) <<  8 |
                  (m[a0 + ( 7 << 7)] & 255));
            p2 = ((m[a0 + ( 8 << 7)]      ) << 24 |
                  (m[a0 + ( 9 << 7)] & 255) << 16 |
                  (m[a0 + (10 << 7)] & 255) <<  8 |
                  (m[a0 + (11 << 7)] & 255));
            p3 = ((m[a0 + (12 << 7)]      ) << 24 |
                  (m[a0 + (13 << 7)] & 255) << 16 |
                  (m[a0 + (14 << 7)] & 255) <<  8 |
                  (m[a0 + (15 << 7)] & 255));
            if ((p0 | p1 | p2 | p3) == 0 ||
                (p0 & p1 & p2 & p3) == -1) {  //空白
              sb.append (' ');
              continue;
            }
          }
          int h = ((p0 * 31 + p1) * 31 + p2) * 31 + p3;
          h = ((h >> 16) + h) & 65535;  //ハッシュコード
          HanPat pat = hanTable[h];
          while (pat != null) {  //リストを辿る
            if (pat.p0 == p0 &&
                pat.p1 == p1 &&
                pat.p2 == p2 &&
                pat.p3 == p3) {
              break;
            }
            pat = pat.next;
          }
          if (pat != null) {  //見つかった
            sb.append ((char) pat.c);
            continue;
          }
        }
        //不明
        sb.append ('?');
      }  //for col
    }  //for row
    return sb.toString ();
  }  //getText



}  //class TextCopy
