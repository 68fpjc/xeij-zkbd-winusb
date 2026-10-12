//========================================================================================
//  SpritePatternViewer.java
//    en:Sprite pattern viewer
//    ja:スプライトパターンビュア
//  Copyright (C) 2003-2025 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.awt.*;
import java.awt.event.*;  //MouseEvent
import java.awt.font.*;  //TextLayout
import java.awt.geom.*;  //Rectangle2D
import java.awt.image.*;  //BufferedImage
import java.util.*;  //Arrays
import javax.swing.*;

public class SpritePatternViewer {

  public static final boolean SPV_ON = true;

  //セル
  static final int SPV_CELL_WIDTH = 36;  //セルの幅。偶数
  static final int SPV_CELL_HEIGHT = 36;  //セルの高さ

  //グループ
  //  グループのサイズは4x4セルに固定
  static final int SPV_GROUP_GAP = 4;  //グループの間隔

  //ヘッダ
  static final String SPV_FONT_NAME = "Dialog";  //フォント名
  static final int SPV_FONT_STYLE = Font.BOLD;  //フォントスタイル
  static final int SPV_FONT_SIZE = 16;  //フォントサイズ
  static final int SPV_HEADER_WIDTH = SPV_FONT_SIZE * 3;  //左ヘッダの幅
  static final int SPV_HEADER_HEIGHT = SPV_FONT_SIZE;  //上ヘッダの高さ

  //イメージ
  static int spvImageWidth;  //イメージ幅
  static int spvImageHeight;  //イメージ高さ
  static BufferedImage spvBufferedImage;  //イメージ
  static int[] spvBitmap;  //ビットマップ
  static ScrollCanvas spvCanvas;  //キャンバス

  //色
  static final int SPV_BACKGROUND_RGB = 0xff333333;  //背景色
  static final int SPV_FOREGROUND_RGB = 0xffcccccc;  //文字色
  static final int SPV_GRAY_LINE_RGB = 0xff999999;  //灰色の線の色
  static final int SPV_BLOCK_DOT_RGB = 0xffffffff;  //パレットブロックの点の色

  //バンク
  //  0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16  17  18   19    20
  //  0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 0-3 4-7 8-11 12-15 0-15
  static final int SPV_DEF_BANK = 0;
  static final int SPV_MIN_BANK = 0;
  static final int SPV_MAX_BANK = 20;
  static int spvBankNumber;  //バンク番号
  static JComboBox<String> spvBankComboBox;  //バンクドロップダウンリスト
  static int spvCellOffset;  //開始セル番号
  static int spvColsBit;  //セル桁数のビット
  static int spvRowsBit;  //セル行数のビット
  static int spvColsMask;  //セル桁数のマスク
  static int spvRowsMask;  //セル行数のマスク
  static int spvWidth;  //幅
  static int spvHeight;  //高さ

  //サイズ
  //  -1   0   1
  //  Auto 8x8 16x16
  static final int SPV_DEF_SIZE = -1;
  static final int SPV_MIN_SIZE = -1;
  static final int SPV_MAX_SIZE = 1;
  static final int SPV_LAST_SIZE = 1;  //前回のサイズの初期値
  static int spvSizeNumber;  //サイズ番号
  static JComboBox<String> spvSizeComboBox;  //サイズドロップダウンリスト
  static final int[] spvLastSizeArray = new int[4096];  //前回のサイズ
  static final int[] spvSizeArray = new int[4096];  //サイズ

  //パレットブロック
  //  -1   0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15
  //  Auto 0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15
  static final int SPV_DEF_BLOCK = -1;
  static final int SPV_MIN_BLOCK = -1;
  static final int SPV_MAX_BLOCK = 15;
  static final int SPV_LAST_BLOCK = 1;  //前回のパレットブロックの初期値
  static int spvBlockNumber;  //ブロック番号
  static JComboBox<String> spvBlockComboBox;  //ブロックドロップダウンリスト
  static final int[] spvLastBlockArray = new int[4 * 4096];  //前回のパレットブロック
  static final int[] spvBlockArray = new int[4 * 4096];  //パレットブロック

  //反転
  //  -1   0 1 2 3
  //  Auto - H V H&V
  static final int SPV_DEF_FLIP = -1;
  static final int SPV_MIN_FLIP = -1;
  static final int SPV_MAX_FLIP = 3;
  static final int SPV_LAST_FLIP = 0;  //前回の反転の初期値
  static int spvFlipNumber;  //反転番号
  static JComboBox<String> spvFlipComboBox;  //反転ドロップダウンリスト
  static final int[] spvLastFlipArray = new int[4 * 4096];  //前回の反転
  static final int[] spvFlipArray = new int[4 * 4096];  //反転。bit1=上下反転,bit0=左右反転

  //倍率
  //  -1 0 1 2 3  4
  //  1  2 4 8 16 32
  static final int SPV_DEF_SCALE = 0;
  static final int SPV_MIN_SCALE = -1;
  static final int SPV_MAX_SCALE = 4;
  static int spvScaleNumber;  //倍率番号
  static JComboBox<String> spvScaleComboBox;  //倍率ドロップダウンリスト

  //Hex
  //  off    on
  //  10進数 16進数
  static final boolean SPV_DEF_HEX = false;
  static boolean spvHex;

  //停止
  static boolean spvStopped;  //true=停止中
  static boolean spvStoppedRequest;
  //  コピーされた値
  static final short[] spvCopiedNum = new short[SpriteScreen.SPR_COUNT];
  static final short[] spvCopiedCol = new short[SpriteScreen.SPR_COUNT];
  static final byte[] spvCopiedPrw = new byte[SpriteScreen.SPR_COUNT];
  static final boolean[] spvCopiedH = new boolean[SpriteScreen.SPR_COUNT];
  static final boolean[] spvCopiedV = new boolean[SpriteScreen.SPR_COUNT];
  static final int[] spvCopiedPat = new int[32 * 4096];
  static final short[] spvCopiedTNum = new short[4096 * 4];
  static final short[] spvCopiedTCol = new short[4096 * 4];
  static final boolean[] spvCopiedTH = new boolean[4096 * 4];
  static final boolean[] spvCopiedTV = new boolean[4096 * 4];
  static final int[] spvCopiedPalTbl = new int[65536];
  static final int[] spvCopiedPal16TS = new int[256];
  //  参照する値。停止中はコピーされた値、さもなくば現在の値
  static short[] spvNum;
  static short[] spvCol;
  static byte[] spvPrw;
  static boolean[] spvH;
  static boolean[] spvV;
  static int[] spvPat;
  static short[] spvTNum;
  static short[] spvTCol;
  static boolean[] spvTH;
  static boolean[] spvTV;
  static int[] spvPalTbl;
  static int[] spvPal16TS;
  //
  static final int[] spvPal32TS = new int[256];

  //テキストフィールド
  static JTextField spvTextField;
  static boolean spvTextLocked;

  //ウインドウ
  static JFrame spvFrame;

  //ポップアップメニュー
  static JPopupMenu spvPopupMenu;
  static int spvOffsetToCopy;
  static int spvLengthToCopy;

  //タイマー
  public static final int SPV_INTERVAL = 10;
  public static int spvTimer;

  //spvInit ()
  //  初期化
  public static void spvInit () {
    //パラメータ
    spvBankNumber = Settings.sgsGetInt ("spvbank", SPV_DEF_BANK, SPV_MIN_BANK, SPV_MAX_BANK);
    spvBlockNumber = Settings.sgsGetInt ("spvblock", SPV_DEF_BLOCK, SPV_MIN_BLOCK, SPV_MAX_BLOCK);
    spvFlipNumber = Settings.sgsGetInt ("spvflip", SPV_DEF_FLIP, SPV_MIN_FLIP, SPV_MAX_FLIP);
    spvHex = Settings.sgsGetOnOff ("spvhex", SPV_DEF_HEX);
    spvScaleNumber = Settings.sgsGetInt ("spvscale", SPV_DEF_SCALE, SPV_MIN_SCALE, SPV_MAX_SCALE);
    spvSizeNumber = Settings.sgsGetInt ("spvsize", SPV_DEF_SIZE, SPV_MIN_SIZE, SPV_MAX_SIZE);
    //イメージ
    spvImageWidth = (SPV_HEADER_WIDTH +
                     (SPV_CELL_WIDTH << 6) +
                     (SPV_GROUP_GAP << (6 - 2)) - SPV_GROUP_GAP);
    spvImageHeight = (SPV_HEADER_HEIGHT +
                      (SPV_CELL_HEIGHT << 6) +
                      (SPV_GROUP_GAP << (6 - 2)) - SPV_GROUP_GAP);
    //バンク
    spvSetBankNumber (spvBankNumber);
    //サイズ
    Arrays.fill (spvLastSizeArray, SPV_LAST_SIZE);
    //パレットブロック
    Arrays.fill (spvLastBlockArray, SPV_LAST_BLOCK);
    //反転
    Arrays.fill (spvLastFlipArray, SPV_LAST_FLIP);
    //停止
    spvSetStoppedOff ();
    //ウインドウ
    spvFrame = null;
    //タイマー
    spvTimer = 0;
  }  //spvInit

  //spvTini ()
  //  後始末
  public static void spvTini () {
    //パラメータ
    Settings.sgsPutInt ("spvbank", spvBankNumber);
    Settings.sgsPutInt ("spvblock", spvBlockNumber);
    Settings.sgsPutInt ("spvflip", spvFlipNumber);
    Settings.sgsPutOnOff ("spvhex", spvHex);
    Settings.sgsPutInt ("spvscale", spvScaleNumber);
    Settings.sgsPutInt ("spvsize", spvSizeNumber);
  }  //spvTini

  //spvSetBankNumber (number)
  //  バンク番号を設定する
  static void spvSetBankNumber (int number) {
    spvBankNumber = number;
    spvCellOffset = (spvBankNumber < 16 ? spvBankNumber << 8 :
                     spvBankNumber < 20 ? (spvBankNumber - 16) << 10 :
                     0);  //(spvBankNumber-20)<<12
    spvColsBit =
      spvRowsBit = (spvBankNumber < 16 ? 4 :  //16x16
                    spvBankNumber < 20 ? 5 :  //32x32
                    6);  //64x64
    spvColsMask =
      spvRowsMask = (1 << spvRowsBit) - 1;
    spvWidth = (SPV_HEADER_WIDTH +
                (SPV_CELL_WIDTH << spvColsBit) +
                (SPV_GROUP_GAP << (spvColsBit - 2)) - SPV_GROUP_GAP);
    spvHeight = (SPV_HEADER_HEIGHT +
                 (SPV_CELL_HEIGHT << spvRowsBit) +
                 (SPV_GROUP_GAP << (spvRowsBit - 2)) - SPV_GROUP_GAP);
    if (spvCanvas != null) {
      spvDrawHeader ();
      spvCanvas.setImage (spvWidth, spvHeight);
    }
  }  //spvSetBankNumber

  //x = spvColToX (col)
  //  セル桁からX座標を求める
  static int spvColToX (int col) {
    int g = col >> 2;  //グループ
    col &= 3;  //グループ内セル桁
    return (SPV_HEADER_WIDTH +
            ((SPV_CELL_WIDTH << 2) + SPV_GROUP_GAP) * g +
            SPV_CELL_WIDTH * col);
  }  //spvColToX

  //y = spvRowToY (row)
  //  セル行からY座標を求める
  static int spvRowToY (int row) {
    int g = row >> 2;  //グループ
    row &= 3;  //グループ内セル行
    return (SPV_HEADER_HEIGHT +
            ((SPV_CELL_HEIGHT << 2) + SPV_GROUP_GAP) * g +
            SPV_CELL_HEIGHT * row);
  }  //spvRowToY

  //((x << 16) | col) = spvXToXCol (x)
  //  X座標からセル内X座標とセル桁を求める。-1=範囲外
  static int spvXToXCol (int x) {
    if (x < 0 || spvWidth <= x) {  //表示範囲の外
      return -1;
    }
    if (x < SPV_HEADER_WIDTH) {  //左ヘッダ
      return -1;
    }
    x -= SPV_HEADER_WIDTH;
    int g = x / ((SPV_CELL_WIDTH << 2) + SPV_GROUP_GAP);  //グループ
    x -= ((SPV_CELL_WIDTH << 2) + SPV_GROUP_GAP) * g;  //グループ内X座標
    if ((SPV_CELL_WIDTH << 2) <= x) {  //グループの間隔
      return -1;
    }
    int col = x / SPV_CELL_WIDTH;  //グループ内セル桁
    x -= SPV_CELL_WIDTH * col;  //セル内X座標
    return (x << 16) | ((g << 2) + col);
  }  //spvXToXCol

  //((y << 16) | row) = spvYToYRow (y)
  //  Y座標からセル内Y座標とセル行を求める。-1=範囲外
  static int spvYToYRow (int y) {
    if (y < 0 || spvHeight <= y) {  //表示範囲の外
      return -1;
    }
    if (y < SPV_HEADER_HEIGHT) {  //上ヘッダ
      return -1;
    }
    y -= SPV_HEADER_HEIGHT;
    int g = y / ((SPV_CELL_HEIGHT << 2) + SPV_GROUP_GAP);  //グループ
    y -= ((SPV_CELL_HEIGHT << 2) + SPV_GROUP_GAP) * g;  //グループ内Y座標
    if ((SPV_CELL_HEIGHT << 2) <= y) {  //グループの間隔
      return -1;
    }
    int row = y / SPV_CELL_HEIGHT;  //グループ内セル行
    y -= SPV_CELL_HEIGHT * row;  //セル内Y座標
    return (y << 16) | ((g << 2) + row);
  }  //spvYToYRow

  //spvStart ()
  //  開始
  public static void spvStart () {
    if (RestorableFrame.rfmGetOpened (Settings.SGS_SPV_FRAME_KEY)) {
      spvOpen ();
    }
  }  //spvStart

  //spvOpen ()
  //  開く
  public static void spvOpen () {
    if (spvFrame == null) {
      spvMakeFrame ();
    } else {
      spvUpdateFrame ();
    }
    XEiJ.dbgVisibleMask |= XEiJ.DBG_SPV_VISIBLE_MASK;
    XEiJ.pnlExitFullScreen (false);
    spvFrame.setVisible (true);
  }  //spvOpen

  //spvMakeFrame ()
  //  作る
  //  ここでは開かない
  static void spvMakeFrame () {
    //イメージ
    spvBufferedImage = new BufferedImage (spvImageWidth, spvImageHeight, BufferedImage.TYPE_INT_ARGB);
    spvDrawHeader ();
    //ビットマップ
    spvBitmap = ((DataBufferInt) spvBufferedImage.getRaster ().getDataBuffer ()).getData ();
    //キャンバス
    spvCanvas = new ScrollCanvas (spvBufferedImage, spvWidth, spvHeight);
    spvCanvas.setMatColor (new Color (SPV_BACKGROUND_RGB));
    spvCanvas.setMinScaleShift (SPV_MIN_SCALE);
    spvCanvas.setMaxScaleShift (SPV_MAX_SCALE);
    spvCanvas.setScaleShift (spvScaleNumber);
    //アクションリスナー
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        String command = ae.getActionCommand ();
        switch (command) {
        case "Bank":
          spvSetBankNumber (spvBankComboBox.getSelectedIndex () + SPV_MIN_BANK);
          if (XEiJ.mpuTask == null) {
            spvUpdateFrame ();
          }
          break;
        case "Size":
          spvSizeNumber = spvSizeComboBox.getSelectedIndex () + SPV_MIN_SIZE;
          if (XEiJ.mpuTask == null) {
            spvUpdateFrame ();
          }
          break;
        case "Block":
          spvBlockNumber = spvBlockComboBox.getSelectedIndex () + SPV_MIN_BLOCK;
          if (XEiJ.mpuTask == null) {
            spvUpdateFrame ();
          }
          break;
        case "Flip":
          spvFlipNumber = spvFlipComboBox.getSelectedIndex () + SPV_MIN_FLIP;
          if (XEiJ.mpuTask == null) {
            spvUpdateFrame ();
          }
          break;
        case "Hex":
          spvHex = (((JCheckBox) source).isSelected ());
          spvDrawHeader ();
          spvUpdateFrame ();
          break;
        case "Stop":
          spvStoppedRequest = (((JCheckBox) source).isSelected ());
          if (XEiJ.mpuTask == null) {
            spvUpdateFrame ();
          }
          break;
        case "Scale":
          spvScaleNumber = spvScaleComboBox.getSelectedIndex () + SPV_MIN_SCALE;
          spvCanvas.setScaleShift (spvScaleNumber);
          break;
        case "Copy as hexadecimal":
          spvCopyPattern ();
          break;
        default:
          System.out.println ("unknown action command " + command);
        }
      }
    };
    //テキストフィールド
    spvTextField = new JTextField ();
    spvTextField.setEditable (false);
    spvTextField.setHorizontalAlignment (JTextField.CENTER);
    //ウインドウ
    spvFrame = Multilingual.mlnTitle (
      ComponentFactory.createRestorableSubFrame (
        Settings.SGS_SPV_FRAME_KEY,
        "Sprite Pattern Viewer",
        null,
        ComponentFactory.createBorderPanel (
          0, 0,
          //CENTER
          spvCanvas,
          //NORTH
          ComponentFactory.createVerticalBox (
            //1行目
            ComponentFactory.createHorizontalBox (
              Box.createHorizontalGlue (),
              //
              Multilingual.mlnText (
                ComponentFactory.createLabel ("Bank "),
                "ja", "バンク "),
              spvBankComboBox = ComponentFactory.createComboBox (
                spvBankNumber - SPV_MIN_BANK,
                "Bank",
                listener,
                "0", "1", "2", "3", "4", "5", "6", "7", "8",
                "9", "10", "11", "12", "13", "14", "15",
                "0-3", "4-7", "8-11", "12-15", "0-15"),
              //
              Multilingual.mlnText (
                ComponentFactory.createLabel ("Size "),
                "ja", "サイズ "),
              spvSizeComboBox = ComponentFactory.createComboBox (
                spvSizeNumber - SPV_MIN_SIZE,
                "Size",
                listener,
                4,
                Multilingual.mlnJapanese ? "自動" : "Auto",
                "8x8", "16x16"),
              //
              Multilingual.mlnText (
                ComponentFactory.createLabel ("Block "),
                "ja", "ブロック "),
              spvBlockComboBox = ComponentFactory.createComboBox (
                spvBlockNumber - SPV_MIN_BLOCK,
                "Block",
                listener,
                4,
                Multilingual.mlnJapanese ? "自動" : "Auto",
                "0", "1", "2", "3", "4", "5", "6", "7", "8",
                "9", "10", "11", "12", "13", "14", "15"),
              //
              Multilingual.mlnText (
                ComponentFactory.createLabel ("Flip "),
                "ja", "反転 "),
              spvFlipComboBox = ComponentFactory.createComboBox (
                spvFlipNumber - SPV_MIN_FLIP,
                "Flip",
                listener,
                4,
                Multilingual.mlnJapanese ? "自動" : "Auto",
                Multilingual.mlnJapanese ? "なし" : "-",
                Multilingual.mlnJapanese ? "左右" : "H",
                Multilingual.mlnJapanese ? "上下" : "V",
                Multilingual.mlnJapanese ? "上下左右" : "H&V"),
              //
              Box.createHorizontalStrut (5),
              ComponentFactory.createCheckBox (spvHex, "Hex", listener),
              //
              Box.createHorizontalGlue ()
              ),  //HorizontalBox
            //2行目
            ComponentFactory.createHorizontalBox (
              //
              spvTextField,
              //
              Multilingual.mlnText (
                ComponentFactory.createLabel ("Scale "),
                "ja", "倍率 "),
              spvScaleComboBox = ComponentFactory.createComboBox (
                spvCanvas.getScaleShift () - SPV_MIN_SCALE,
                "Scale",
                listener,
                "1", "2", "4", "8", "16", "32"),
              //
              Box.createHorizontalStrut (5),
              Multilingual.mlnText (
                ComponentFactory.createCheckBox (spvStoppedRequest, "Stop", listener),
                "ja", "停止")
              //
              )  //HorizontalBox
            )  //VerticalBox
          )  //BorderPanel
        ),  //SubFrame
      "ja", "スプライトパターンビュア");
    //スケールシフトリスナー
    spvCanvas.addScaleShiftListener (new ScrollCanvas.ScaleShiftListener () {
      @Override public void scaleShiftChanged (int scaleShift) {
        spvScaleComboBox.setSelectedIndex (scaleShift - SPV_MIN_SCALE);
      }
    });
    //ウインドウリスナー
    ComponentFactory.addListener (
      spvFrame,
      new WindowAdapter () {
        @Override public void windowClosing (WindowEvent we) {
          XEiJ.dbgVisibleMask &= ~XEiJ.DBG_SPV_VISIBLE_MASK;
        }
      });
    //ポップアップメニュー
    spvPopupMenu = ComponentFactory.createPopupMenu (
      Multilingual.mlnText (
        ComponentFactory.createMenuItem ("Copy as hexadecimal", 'C', listener),
        "ja", "16進数でコピー")
      );
    //マウスリスナー
    MouseAdapter ma = new MouseAdapter () {
      @Override public void mouseClicked (MouseEvent me) {
        spvTextLocked = !spvTextLocked;  //テキストフィールドのロックを反転する
      }  //mouseClicked
      @Override public void mouseMoved (MouseEvent me) {
        if (!spvTextLocked) {  //テキストフィールドがロックされていないとき
          MouseEvent2D me2D = (MouseEvent2D) me;
          int x = (int) me2D.getX2D ();
          int y = (int) me2D.getY2D ();
          String s = spvGetPixel (x, y);
          spvTextField.setText (s == null ? "" : s);  //テキストフィールドに表示する
        }
      }  //mouseMoved
      @Override public void mousePressed (MouseEvent me) {
        spvShowPopup (me);
      }  //mousePressed
      @Override public void mouseReleased (MouseEvent me) {
        spvShowPopup (me);
      }  //mouseReleased
    };
    spvCanvas.addMouseListener (ma);
    spvCanvas.addMouseMotionListener (ma);
  }  //spvMakeFrame

  //spvDrawHeader ()
  //  ヘッダを描く
  static void spvDrawHeader () {
    Graphics2D g2 = spvBufferedImage.createGraphics ();
    //背景
    g2.setColor (new Color (SPV_BACKGROUND_RGB));
    g2.fillRect (0, 0, spvWidth, spvHeight);
    //ヘッダ
    g2.setColor (new Color (SPV_FOREGROUND_RGB));
    g2.setFont (new Font (SPV_FONT_NAME, SPV_FONT_STYLE, SPV_FONT_SIZE));
    //  上
    for (int col = 0; col <= spvColsMask; col++) {
      spvDrawString (g2,
                     spvColToX (col) + SPV_CELL_WIDTH / 2,  //X座標
                     spvRowToY (0) - SPV_FONT_SIZE / 12,  //Y座標
                     String.format (spvHex ? "%X" : "%d",
                                    col & spvColsMask),  //文字列
                     SwingConstants.SOUTH);  //アンカー
    }  //for col
    //  左
    for (int row = 0; row <= spvRowsMask; row++) {
      spvDrawString (g2,
                     spvColToX (0) - SPV_FONT_SIZE / 6,  //X座標
                     spvRowToY (row) + SPV_CELL_HEIGHT / 2,  //Y座標
                     String.format (spvHex ? "%X" : "%d",
                                    spvCellOffset + ((row & spvRowsMask) << spvColsBit)),  //文字列
                     SwingConstants.EAST);  //アンカー
    }  //for row
  }  //spvDrawHeader

  //spvDrawString (g, x, y, s, d)
  //  文字列を描く
  static void spvDrawString (Graphics2D g2, int x, int y, String s, int d) {
    Font f = g2.getFont ();
    FontRenderContext flc = g2.getFontRenderContext ();
    TextLayout tl = new TextLayout (s, f, flc);
    Rectangle2D r = tl.getBounds ();
    int rx = (int) Math.round (r.getX ());
    int ry = (int) Math.round (r.getY ());
    int rw = (int) Math.round (r.getWidth ());
    int rh = (int) Math.round (r.getHeight ());
    switch (d) {
    case SwingConstants.NORTH_WEST:
      g2.drawString (s, x - rx, y - ry);
      break;
    case SwingConstants.NORTH:
      g2.drawString (s, x - (rx + (rw >> 1)), y - ry);
      break;
    case SwingConstants.NORTH_EAST:
      g2.drawString (s, x - (rx + rw), y - ry);
      break;
    case SwingConstants.WEST:
      g2.drawString (s, x - rx, y - (ry + (rh >> 1)));
      break;
    case SwingConstants.CENTER:
      g2.drawString (s, x - (rx + (rw >> 1)), y - (ry + (rh >> 1)));
      break;
    case SwingConstants.EAST:
      g2.drawString (s, x - (rx + rw), y - (ry + (rh >> 1)));
      break;
    case SwingConstants.SOUTH_WEST:
      g2.drawString (s, x - rx, y - (ry + rh));
      break;
    case SwingConstants.SOUTH:
      g2.drawString (s, x - (rx + (rw >> 1)), y - (ry + rh));
      break;
    case SwingConstants.SOUTH_EAST:
      g2.drawString (s, x - (rx + rw), y - (ry + rh));
      break;
    }
  }  //spvDrawString

  //spvShowPopup (me)
  //  ポップアップメニューを表示する
  static void spvShowPopup (MouseEvent me) {
    if (me.isPopupTrigger ()) {
      MouseEvent2D me2D = (MouseEvent2D) me;
      int x = (int) me2D.getX2D ();
      int y = (int) me2D.getY2D ();
      if (spvGetPatternToCopy (x, y)) {
        Point p = spvCanvas.getPopupPoint (me2D);
        spvPopupMenu.show (spvCanvas, p.x, p.y);
      }
    }
  }  //spvShowPopup

  //f = spvGetPatternToCopy (x, y)
  //  座標からspvPatのコピー範囲を求めてspvOffsetToCopyとspvLengthToCopyを設定する
  //  1セルの長さは32、1/4セルの長さは8
  static boolean spvGetPatternToCopy (int x, int y) {
    int col = spvXToXCol (x);  //セル内X座標<<16|セル桁
    int row = spvYToYRow (y);  //セル内Y座標<<16|セル行
    if (col < 0 || row < 0) {  //セル範囲外
      spvOffsetToCopy = 0;
      spvLengthToCopy = 0;
      return false;
    }
    x = col >> 16;  //セル内X座標
    y = row >> 16;  //セル内Y座標
    col = (char) col;  //セル桁
    row = (char) row;  //セル行
    int n = (spvCellOffset + col + (row << spvColsBit)) << 2;  //8x8パターン番号
    int size = spvSizeArray[n >> 2];  //サイズ
    if (size < 0) {
      size = spvLastSizeArray[n >> 2];
    }
    if (size == 0) {  //8x8
      int qCol = x / (SPV_CELL_WIDTH / 2);  //1/4セル桁
      int qRow = y / (SPV_CELL_HEIGHT / 2);  //1/4セル行
      n += qRow + (qCol << 1);  //8x8パターン番号。進行方向に注意
      spvLengthToCopy = 1 << 3;
    } else {  //16x16
      spvLengthToCopy = 4 << 3;
    }
    spvOffsetToCopy = n << 3;
    return true;
  }  //spvGetPatternToCopy

  //spvCopyPattern ()
  //  設定された範囲のパターンを16進数でコピーする
  static void spvCopyPattern () {
    if (spvLengthToCopy != 0) {
      StringBuilder sb = new StringBuilder ();
      for (int i = 0; i < spvLengthToCopy; i++) {
        sb.append (String.format ("%08X\n", spvPat[spvOffsetToCopy + i]));
      }
      XEiJ.clpCopy (sb.toString ());
    }
  }  //spvCopyPattern

  //s = spvGetPixel (x, y)
  //  座標からピクセルの情報を求める
  static String spvGetPixel (int x, int y) {
    int col = spvXToXCol (x);  //セル内X座標<<16|セル桁
    int row = spvYToYRow (y);  //セル内Y座標<<16|セル行
    if (col < 0 || row < 0) {  //セル範囲外
      return null;
    }
    x = col >> 16;  //セル内X座標
    y = row >> 16;  //セル内Y座標
    col = (char) col;  //セル桁
    row = (char) row;  //セル行
    int n = (spvCellOffset + col + (row << spvColsBit)) << 2;  //8x8パターン番号
    int size = spvSizeArray[n >> 2];  //サイズ
    if (size < 0) {
      size = spvLastSizeArray[n >> 2];
    }
    if (size == 0) {  //8x8
      int qCol = x / (SPV_CELL_WIDTH / 2);  //1/4セル桁
      int qRow = y / (SPV_CELL_HEIGHT / 2);  //1/4セル行
      n += qRow + (qCol << 1);  //8x8パターン番号。進行方向に注意
      x -= (SPV_CELL_WIDTH / 2) * qCol;  //1/4セル内X座標
      y -= (SPV_CELL_HEIGHT / 2) * qRow;  //1/4セル内Y座標
      x -= 1;
      y -= 1;
      if (x < 0 || 2 * 8 <= x ||
          y < 0 || 2 * 8 <= y) {  //パータン範囲外
        return null;
      }
      x >>= 1;  //8x8パターン内X座標
      y >>= 1;  //8x8パターン内Y座標
      int pb = spvBlockArray[n];  //パレットブロック
      if (pb < 0) {
        pb = spvLastBlockArray[n];
      }
      int vh = spvFlipArray[n];  //反転
      if (vh < 0) {
        vh = spvLastFlipArray[n];
      }
      int v = vh >> 1;
      int h = vh & 1;
      if (h != 0) {  //水平反転
        x ^= 7;
      }
      if (v != 0) {  //垂直反転
        y ^= 7;
      }
      int pc = (spvPat[(n << 3) + y] >> ((7 - x) << 2)) & 15;  //パレットコード
      int p = pb << 4 | pc;
      int c = spvPal16TS[p];
      return String.format ("8x8 n=%d x=%d y=%d p=0x%02X c=0x%04X(%d,%d,%d,%d)",
                            n, x, y, p, c,
                            c >> 11, (c >> 6) & 31, (c >> 1) & 31, c & 1);
    } else {  //16x16
      x -= 2;
      y -= 2;
      if (x < 0 || 2 * 16 <= x ||
          y < 0 || 2 * 16 <= y) {  //パターン範囲外
        return null;
      }
      x >>= 1;  //16x16パターン内X座標
      y >>= 1;  //16x16パターン内Y座標
      int pb = spvBlockArray[n];  //パレットブロック
      if (pb < 0) {
        pb = spvLastBlockArray[n];
      }
      int vh = spvFlipArray[n];  //反転
      if (vh < 0) {
        vh = spvLastFlipArray[n];
      }
      int v = vh >> 1;
      int h = vh & 1;
      if ((vh & 1) != 0) {  //水平反転
        x ^= 15;
      }
      if ((vh & 2) != 0) {  //垂直反転
        y ^= 15;
      }
      int pc = (spvPat[(n << 3) + y + ((x & 8) << 1)] >> ((7 - (x & 7)) << 2)) & 15;  //パレットコード
      int p = pb << 4 | pc;
      int c = spvPal16TS[p];
      return String.format ("16x16 n=%d x=%d y=%d p=0x%02X c=0x%04X(%d,%d,%d,%d)",
                            n >> 2, x, y, p, c,
                            c >> 11, (c >> 6) & 31, (c >> 1) & 31, c & 1);
    }  //if 8x8 else 16x16
  }  //spvGetPixel

  //spvUpdateFrame ()
  //  更新する
  static void spvUpdateFrame () {
    if (spvFrame == null) {  //未初期化
      return;
    }
    //停止/動作
    if (spvStopped != spvStoppedRequest) {
      if (spvStoppedRequest) {  //動作→停止
        spvSetStoppedOn ();
      } else {  //停止→動作
        spvSetStoppedOff ();
      }
    }
    //パレットテーブルを作る
    for (int i = 0; i < 256; i++) {
      spvPal32TS[i] = spvPalTbl[spvPal16TS[i]];
    }
    //情報を集める
    Arrays.fill (spvSizeArray, spvSizeNumber);
    Arrays.fill (spvBlockArray, spvBlockNumber);
    Arrays.fill (spvFlipArray, spvFlipNumber);
    //自動の要素があるときだけ集める
    if (spvSizeNumber < 0 ||
        spvBlockNumber < 0 ||
        spvFlipNumber < 0) {  //自動の要素がある
      //スプライトから集める
      for (int sn = 0; sn < SpriteScreen.sprNumberOfSprites; sn++) {  //スプライト番号
        if ((256 <= sn && sn < 256 + 8) ||  //欠番
            spvPrw[sn] == 0) {  //表示されていない
          continue;
        }
        int n = spvNum[sn] << 2;  //8x8パターン番号
        if (spvSizeArray[n >> 2] < 0) {
          spvSizeArray[n >> 2] =
            spvLastSizeArray[n >> 2] = 1;  //16x16
        }
        int pb = spvCol[sn] >> 4;  //パレットブロック
        int vh = ((spvV[sn] ? 2 : 0) |
                  (spvH[sn] ? 1 : 0));  //反転
        for (int k = 0; k < 4; k++) {
          if (spvBlockArray[n + k] < 0) {
            spvBlockArray[n + k] =
              spvLastBlockArray[n + k] = pb;
          }
          if (spvFlipArray[n + k] < 0) {
            spvFlipArray[n + k] =
              spvLastFlipArray[n + k] = vh;
          }
        }
      }  //for sn
      //バックグラウンドから集める
      int tm = 0;  //表示されているテキストページのマスク
      if ((SpriteScreen.sprReg4BgCtrlPort & 1) != 0) {  //BG0が表示されている
        tm |= 1 << ((SpriteScreen.sprReg4BgCtrlPort >> 1) & 3);  //BG0に割り当てられているテキストページ
      }
      if (((SpriteScreen.sprReg8ResoPort & 3) == 0 ||  //256x256または
           SpriteScreen.spr512bg1) &&  //512x512でBG1を表示かつ
          (SpriteScreen.sprReg4BgCtrlPort & 8) != 0) {  //BG1が表示されている
        tm |= 1 << ((SpriteScreen.sprReg4BgCtrlPort >> 4) & 3);  //BG1に割り当てられているテキストページ
      }
      if ((SpriteScreen.sprReg8ResoPort & 3) == 0) {  //256x256
        for (int tp = 0; tp < 4; tp++) {  //テキストページ
          if ((tm & (1 << tp)) == 0) {  //表示されていない
            continue;
          }
          int o = 4096 * tp;  //テキスト配列インデックス開始位置
          for (int i = 0; i < 4096; i++) {  //テキスト配列インデックス
            int n = spvTNum[o + i] >> 3;  //8x8パターン番号
            if (spvSizeArray[n >> 2] < 0) {
              spvSizeArray[n >> 2] =
                spvLastSizeArray[n >> 2] = 0;  //8x8
            }
            int pb = spvTCol[o + i] >> 4;  //パレットブロック
            int vh = ((spvTV[o + i] ? 2 : 0) |
                      (spvTH[o + i] ? 1 : 0));  //反転
            if (spvBlockArray[n] < 0) {
              spvBlockArray[n] =
                spvLastBlockArray[n] = pb;
            }
            if (spvFlipArray[n] < 0) {
              spvFlipArray[n] =
                spvLastFlipArray[n] = vh;
            }
          }  //for i
        }  //for tp
      } else {  //512x512
        for (int tp = 0; tp < 4; tp++) {  //テキストページ
          if ((tm & (1 << tp)) == 0) {  //表示されていない
            continue;
          }
          int o = 4096 * tp;  //テキスト配列インデックス開始位置
          for (int i = 0; i < 4096; i++) {  //テキスト配列インデックス
            int n = spvTNum[o + i] >> 1;  //8x8パターン番号
            if (spvSizeArray[n >> 2] < 0) {
              spvSizeArray[n >> 2] =
                spvLastSizeArray[n >> 2] = 1;  //16x16
            }
            int pb = spvTCol[o + i] >> 4;  //パレットブロック
            int vh = ((spvTV[o + i] ? 2 : 0) |
                      (spvTH[o + i] ? 1 : 0));  //反転
            for (int k = 0; k < 4; k++) {
              if (spvBlockArray[n + k] < 0) {
                spvBlockArray[n + k] =
                  spvLastBlockArray[n + k] = pb;
              }
              if (spvFlipArray[n + k] < 0) {
                spvFlipArray[n + k] =
                  spvLastFlipArray[n + k] = vh;
              }
            }
          }  //for i
        }  //for tp
      }  //if 256x256 else 512x512
      //集められなかったものは前回と同じにする
      for (int n = 0; n < 4096; n++) {  //16x16パターン番号
        if (spvSizeArray[n] < 0) {
          spvSizeArray[n] = spvLastSizeArray[n];
        }
      }
      for (int n = 0; n < 4 * 4096; n++) {  //8x8パターン番号
        if (spvBlockArray[n] < 0) {
          spvBlockArray[n] = spvLastBlockArray[n];
        }
        if (spvFlipArray[n] < 0) {
          spvFlipArray[n] = spvLastFlipArray[n];
        }
      }
    }  //if 自動の要素がある
    //パターンを表示する
    int a = 32 * spvCellOffset;  //パターン配列インデックス
    for (int cRow = 0; cRow <= spvRowsMask; cRow++) {  //セル行
      int cY = spvRowToY (cRow);  //セル左上(0,0)Y座標
      for (int cCol = 0; cCol <= spvColsMask; cCol++) {  //セル桁
        int cX = spvColToX (cCol);  //セル左上(0,0)X座標
        int cIndex = cX + spvImageWidth * cY;  //セル左上ビットマップインデックス
        //背景色で塗り潰す
        //  サイズが変わるときゴミが残らないようにする
        int i = cIndex;
        for (int v = 0; v < SPV_CELL_HEIGHT; v++) {
          for (int h = 0; h < SPV_CELL_WIDTH; h++) {
            spvBitmap[i + h] = SPV_BACKGROUND_RGB;
          }
          i += spvImageWidth;  //(+0,+1)
        }
        int n = (spvCellOffset + cCol + (cRow << spvColsBit)) << 2;  //8x8パターン番号
        if (spvSizeArray[n >> 2] == 0) {  //8x8
          for (int qCol = 0; qCol < 2; qCol++) {  //1/4セル桁
            int qX = cX + SPV_CELL_WIDTH / 2 * qCol;  //1/4セル左上X座標
            for (int qRow = 0; qRow < 2; qRow++) {  //1/4セル行
              int qY = cY + SPV_CELL_HEIGHT / 2 * qRow;  //1/4セル左上Y座標
              int qIndex = qX + spvImageWidth * qY;  //1/4セル左上ビットマップインデックス
              int pb = spvBlockArray[n];  //パレットブロック
              int vh = spvFlipArray[n];  //反転
              int hMask = (vh & 1) != 0 ? 7 : 0;  //左右反転マスク
              int vMask = (vh & 2) != 0 ? 15 : 0;  //上下反転マスク
              //灰色の線とパレットブロックを表す点
              i = qIndex + (hMask == 0 ? 0 + spvImageWidth * 1 :
                            1 + 2 * 8 + spvImageWidth * 1);  //左上ビットマップインデックス
              for (int v = 0; v <= 15; v++) {  //描画方向。上0→下15
                int vm = v ^ vMask;  //参照方向。上0→下15または下15→上0
                if (vm == pb) {  //パレットブロックの点
                  spvBitmap[i] = SPV_BLOCK_DOT_RGB;
                } else if (vm < 8) {  //灰色の線
                  spvBitmap[i] = SPV_GRAY_LINE_RGB;
                }
                i += spvImageWidth * 1;  //(+0,+1)
              }
              //パターン
              pb <<= 4;  //パレットブロック<<4
              vMask >>= 1;
              i = qIndex + 1 + spvImageWidth * 1;  //左上(1,1)ビットマップインデックス
              for (int v = 0; v <= 7; v++) {  //描画方向。上0→下7
                int vm = v ^ vMask;  //参照方向。上0→下7または下7→上0
                int d = spvPat[a + vm];
                for (int h = 7; 0 <= h; h--) {  //描画方向。左7→右0
                  int hm = h ^ hMask;  //参照方向。左7→右0または右0→左7
                  spvBitmap[i] =
                    spvBitmap[i + 1] =
                      spvBitmap[i + spvImageWidth] =
                        spvBitmap[i + spvImageWidth + 1] = spvPal32TS[pb + ((d >> (hm << 2)) & 15)];
                  i += 2;  //(+2,+0)
                }  //for h
                i += spvImageWidth * 2 - 2 * 8;  //(-2*8,+2)
              }  //for v
              a += 8;
              n++;
            }  //for qRow
          }  //for qCol
        } else {  //16x16
          int pb = spvBlockArray[n];  //パレットブロック
          int vh = spvFlipArray[n];  //反転
          int hMask = (vh & 1) != 0 ? 15 : 0;  //左右反転マスク
          int vMask = (vh & 2) != 0 ? 15 : 0;  //上下反転マスク
          //灰色の線とパレットブロックの点
          i = cIndex + (hMask == 0 ? 0 + spvImageWidth * 2 :
                        2 + 2 * 16 + spvImageWidth * 2);  //左上ビットマップインデックス
          for (int v = 0; v <= 15; v++) {  //描画方向。上0→下15
            int vm = v ^ vMask;  //参照方向。上0→下15または下15→上0
            if (vm == pb) {  //パレットブロックの点
              spvBitmap[i] =
                spvBitmap[i + 1] =
                  spvBitmap[i + spvImageWidth] =
                    spvBitmap[i + spvImageWidth + 1] = SPV_BLOCK_DOT_RGB;
            } else if (vm < 8) {  //灰色の線
              spvBitmap[i] =
                spvBitmap[i + 1] =
                  spvBitmap[i + spvImageWidth] =
                    spvBitmap[i + spvImageWidth + 1] = SPV_GRAY_LINE_RGB;
            }
            i += spvImageWidth * 2;  //(+0,+2)
          }
          //パターン
          pb <<= 4;  //パレットブロック<<4
          i = cIndex + 2 + spvImageWidth * 2;  //左上(2,2)ビットマップインデックス
          for (int v = 0; v <= 15; v++) {  //描画方向。上0→下15
            int vm = v ^ vMask;  //参照方向。上0→下15または下15→上0
            long d = ((long) spvPat[a + vm] << 32 |
                      (long) spvPat[a + vm + 16] & 0xffffffffL);
            for (int h = 15; 0 <= h; h--) {  //描画方向。左15→右0
              int hm = h ^ hMask;  //参照方向。左15→右0または右0→左15
              spvBitmap[i] =
                spvBitmap[i + 1] =
                  spvBitmap[i + spvImageWidth] =
                    spvBitmap[i + spvImageWidth + 1] = spvPal32TS[pb + ((int) (d >> (hm << 2)) & 15)];
              i += 2;  //(+2,+0)
            }  //for h
            i += spvImageWidth * 2 - 2 * 16;  //(-2*16,+2)
          }  //for v
          a += 32;
        }  //if 8x8 else 16x16
      }  //for cCol
    }  //for cRow
    spvCanvas.repaint ();
  }  //spvUpdateFrame

  //spvSetStoppedOn ()
  //  停止する
  static void spvSetStoppedOn () {
    spvStopped = true;
    spvStoppedRequest = true;
    //現在の値をコピーする
    System.arraycopy (SpriteScreen.sprNumPort, 0, spvCopiedNum, 0, SpriteScreen.SPR_COUNT);
    System.arraycopy (SpriteScreen.sprColPort, 0, spvCopiedCol, 0, SpriteScreen.SPR_COUNT);
    System.arraycopy (SpriteScreen.sprPrw, 0, spvCopiedPrw, 0, SpriteScreen.SPR_COUNT);
    System.arraycopy (SpriteScreen.sprH, 0, spvCopiedH, 0, SpriteScreen.SPR_COUNT);
    System.arraycopy (SpriteScreen.sprV, 0, spvCopiedV, 0, SpriteScreen.SPR_COUNT);
    System.arraycopy (SpriteScreen.sprPatPort, 0, spvCopiedPat, 0, 32 * 4096);
    System.arraycopy (SpriteScreen.sprTNum, 0, spvCopiedTNum, 0, 4096 * 4);
    System.arraycopy (SpriteScreen.sprTColPort, 0, spvCopiedTCol, 0, 4096 * 4);
    System.arraycopy (SpriteScreen.sprTH, 0, spvCopiedTH, 0, 4096 * 4);
    System.arraycopy (SpriteScreen.sprTV, 0, spvCopiedTV, 0, 4096 * 4);
    System.arraycopy (VideoController.vcnPalBase[VideoController.VCN_CONTRAST_SCALE * 15], 0, spvCopiedPalTbl, 0, 65536);
    System.arraycopy (VideoController.vcnPal16TSPort, 0, spvCopiedPal16TS, 0, 256);
    //コピーされた値を参照する
    spvNum = spvCopiedNum;
    spvCol = spvCopiedCol;
    spvPrw = spvCopiedPrw;
    spvH = spvCopiedH;
    spvV = spvCopiedV;
    spvPat = spvCopiedPat;
    spvTNum = spvCopiedTNum;
    spvTCol = spvCopiedTCol;
    spvTH = spvCopiedTH;
    spvTV = spvCopiedTV;
    spvPalTbl = spvCopiedPalTbl;
    spvPal16TS = spvCopiedPal16TS;
  }  //spvSetStoppedOn

  //spvSetStoppedOff ()
  //  停止を解除する
  static void spvSetStoppedOff () {
    spvStopped = false;
    spvStoppedRequest = false;
    //現在の値を参照する
    spvNum = SpriteScreen.sprNumPort;
    spvCol = SpriteScreen.sprColPort;
    spvPrw = SpriteScreen.sprPrw;
    spvH = SpriteScreen.sprH;
    spvV = SpriteScreen.sprV;
    spvPat = SpriteScreen.sprPatPort;
    spvTNum = SpriteScreen.sprTNum;
    spvTCol = SpriteScreen.sprTColPort;
    spvTH = SpriteScreen.sprTH;
    spvTV = SpriteScreen.sprTV;
    spvPalTbl = VideoController.vcnPalBase[VideoController.VCN_CONTRAST_SCALE * 15];
    spvPal16TS = VideoController.vcnPal16TSPort;
  }  //spvSetStoppedOff

}  //class SpritePatternViewer
