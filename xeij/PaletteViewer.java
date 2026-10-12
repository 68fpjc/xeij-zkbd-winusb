//========================================================================================
//  PaletteViewer.java
//    en:Palette viewer
//    ja:パレットビュア
//  Copyright (C) 2003-2026 Makoto Kamada
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
import javax.swing.*;

public class PaletteViewer {

  public static final boolean PLV_ON = true;

  //                group
  //  +-------+-------+-------+-------+
  //  |       |       |       |       |
  //  | cell  | cell  | cell  | cell  |
  //  |       |       |       |       |
  //  +-------+-------+-------+-------+
  //  |       |       |       |       |
  //  | cell  | cell  | cell  | cell  |
  //  |       |       |       |       |
  //  +-------+-------+-------+-------+
  //  |       |       |       |       |
  //  | cell  | cell  | cell  | cell  |
  //  |       |       |       |       |
  //  +-------+-------+-------+-------+
  //  |       |       |       |       |
  //  | cell  | cell  | cell  | cell  |
  //  |       |       |       |       |
  //  +-------+-------+-------+-------+
  //                      page
  //  +-------------------------------------------+
  //  |                  header                   |
  //  |     +-------+ +-------+ +-------+ +-------+
  //  |     |       | |       | |       | |       |
  //  |     | group | | group | | group | | group |
  //  |     |       | |       | |       | |       |
  //  |     +-------+ +-------+ +-------+ +-------+
  //  |     +-------+ +-------+ +-------+ +-------+
  //  |     |       | |       | |       | |       |
  //  |  h  | group | | group | | group | | group |
  //  |  e  |       | |       | |       | |       |
  //  |  a  +-------+-+-------+ +-------+ +-------+
  //  |  d  +-------+ +-------+ +-------+ +-------+
  //  |  e  |       | |       | |       | |       |
  //  |  r  | group | | group | | group | | group |
  //  |     |       | |       | |       | |       |
  //  |     +-------+ +-------+ +-------+ +-------+
  //  |     +-------+ +-------+ +-------+ +-------+
  //  |     |       | |       | |       | |       |
  //  |     | group | | group | | group | | group |
  //  |     |       | |       | |       | |       |
  //  +-----+-------+-+-------+ +-------+ +-------+
  //                gap
  //                          arrgt
  //        0          1              2              3
  //    +-------+  +-------+  +-------+-------+  +-------+
  //    |       |  |       |  |       |       |  |       |
  //    |   G   |  |  TS   |  |   G   |  TS   |  |   G   |
  //    |       |  |       |  |       |       |  |       |
  //    +-------+  +-------+  +-------+-------+  +-------+
  //                                             |       |
  //                                             |  TS   |
  //                                             |       |
  //                                             +-------+

  //セル
  static final int PLV_CELL_WIDTH = 18;  //セルの幅。偶数
  static final int PLV_CELL_HEIGHT = 18;  //セルの高さ

  //グループ
  //  グループのサイズは4x4セルに固定
  static final int PLV_GROUP_GAP = 2;  //グループの間隔

  //ヘッダ
  static final String PLV_FONT_NAME = "Dialog";  //フォント名
  static final int PLV_FONT_STYLE = Font.BOLD;  //フォントスタイル
  static final int PLV_FONT_SIZE = 16;  //フォントサイズ
  static final int PLV_HEADER_WIDTH = PLV_FONT_SIZE * 2;  //左ヘッダの幅
  static final int PLV_HEADER_HEIGHT = PLV_FONT_SIZE * 2;  //上ヘッダの高さ

  //イメージ
  static int plvImageWidth;  //イメージの幅
  static int plvImageHeight;  //イメージの高さ
  static BufferedImage plvBufferedImage;  //イメージ
  static int[] plvBitmap;  //ビットマップ
  static ScrollCanvas plvCanvas;  //キャンバス

  //色
  static final int PLV_BACKGROUND_RGB = 0xff333333;  //背景色
  static final int PLV_FOREGROUND_RGB = 0xffcccccc;  //文字色

  //ページ
  static int plvCellOffset;  //開始セル番号
  static int plvColsBit;  //セル桁数のビット
  static int plvRowsBit;  //セル行数のビット
  static int plvColsMask;  //セル桁数のマスク
  static int plvRowsMask;  //セル行数のマスク
  static int plvWidth;  //幅
  static int plvHeight;  //高さ

  //配置
  //  0 1  2 3
  //  G TS H V
  static final int PLV_DEF_ARRGT = 2;
  static final int PLV_MIN_ARRGT = 0;
  static final int PLV_MAX_ARRGT = 3;
  static int plvArrgtNumber;  //配置番号
  static JComboBox<String> plvArrgtComboBox;  //配置ドロップダウンリスト
  static int plvArrgtLand;  //配置後の横方向のページ数。plvArrgtNumber==2?2:1
  static int plvArrgtPort;  //配置後の縦方向のページ数。plvArrgtNumber==3?2:1
  static int plvArrgtCols;  //配置後のセル桁数。plvArrgtLand<<plvColsBit
  static int plvArrgtRows;  //配置後のセル行数。plvArrgtPort<<plvRowsBit
  static int plvArrgtWidth;  //配置後の幅。plvWidth*plvArrgtLand
  static int plvArrgtHeight;  //配置後の高さ。plvHeight*plvArrgtPort

  //倍率
  //  -1  0 1 2 3 4
  //  1/2 1 2 4 8 16
  static final int PLV_DEF_SCALE = 0;
  static final int PLV_MIN_SCALE = -1;
  static final int PLV_MAX_SCALE = 4;
  static int plvScaleNumber;  //倍率番号
  static JComboBox<String> plvScaleComboBox;  //倍率ドロップダウンリスト

  //Hex
  //  off    on
  //  10進数 16進数
  static final boolean PLV_DEF_HEX = false;
  static boolean plvHex;

  //停止
  static boolean plvStopped;  //true=停止中
  static boolean plvStoppedRequest;
  //  コピーされた値
  static final int[] plvCopiedPalTbl = new int[65536];
  static final int[] plvCopiedPal16G8 = new int[256];
  static final int[] plvCopiedPal16TS = new int[256];
  static final int[] plvCopiedPal8G16L = new int[256];
  static final int[] plvCopiedPal8G16H = new int[256];
  //  参照する値。停止中はコピーされた値、さもなくば現在の値
  static int[] plvPalTbl;
  static int[] plvPal16G8;
  static int[] plvPal16TS;
  static int[] plvPal8G16L;
  static int[] plvPal8G16H;
  //
  static boolean plvG65536On;  //true=65536色表示あり

  //テキストフィールド
  static JTextField plvTextField;
  static boolean plvTextLocked;

  //ウインドウ
  static JFrame plvFrame;

  //ポップアップメニュー
  static JPopupMenu plvPopupMenu;
  static int plvPageToCopy;
  static int plvRowToCopy;

  //タイマー
  public static final int PLV_INTERVAL = 10;
  public static int plvTimer;

  //plvInit ()
  //  初期化
  public static void plvInit () {
    //パラメータ
    plvArrgtNumber = Settings.sgsGetInt ("plvarrgt", PLV_DEF_ARRGT, PLV_MIN_ARRGT, PLV_MAX_ARRGT);
    plvHex = Settings.sgsGetOnOff ("plvhex", PLV_DEF_HEX);
    plvScaleNumber = Settings.sgsGetInt ("plvscale", PLV_DEF_SCALE, PLV_MIN_SCALE, PLV_MAX_SCALE);
    //ページ
    plvCellOffset = 0;
    plvColsBit =
      plvRowsBit = 4;
    plvColsMask = (1 << plvColsBit) - 1;
    plvRowsMask = (1 << plvRowsBit) - 1;
    plvWidth = (PLV_HEADER_WIDTH +
                (PLV_CELL_WIDTH << plvColsBit) +
                (PLV_GROUP_GAP << (plvColsBit - 2)) - PLV_GROUP_GAP);
    plvHeight = (PLV_HEADER_HEIGHT +
                 (PLV_CELL_HEIGHT << plvRowsBit) +
                 (PLV_GROUP_GAP << (plvRowsBit - 2)) - PLV_GROUP_GAP);
    //イメージ
    plvImageWidth = plvWidth * 2;
    plvImageHeight = plvHeight * 2;
    //配置
    plvSetArrgt (plvArrgtNumber);
    //停止
    plvSetStoppedOff ();
    //ウインドウ
    plvFrame = null;
    //タイマー
    plvTimer = 0;
  }  //plvInit

  //plvTini ()
  //  後始末
  public static void plvTini () {
    //パラメータ
    Settings.sgsPutInt ("plvarrgt", plvArrgtNumber);
    Settings.sgsPutOnOff ("plvhex", plvHex);
    Settings.sgsPutInt ("plvscale", plvScaleNumber);
  }  //plvTini

  //plvSetArrgt (number)
  //  配置を設定する
  static void plvSetArrgt (int number) {
    plvArrgtNumber = number;
    plvArrgtLand = plvArrgtNumber == 2 ? 2 : 1;
    plvArrgtPort = plvArrgtNumber == 3 ? 2 : 1;
    plvArrgtCols = plvArrgtLand << plvColsBit;
    plvArrgtRows = plvArrgtPort << plvRowsBit;
    plvArrgtWidth = plvWidth * plvArrgtLand;
    plvArrgtHeight = plvHeight * plvArrgtPort;
    if (plvCanvas != null) {
      plvDrawHeader ();
      plvCanvas.setImage (plvArrgtWidth, plvArrgtHeight);
    }
  }  //plvSetArrgt

  //x = plvColToX (col)
  //  セル桁からX座標を求める
  static int plvColToX (int col) {
    int p = col >> plvColsBit;  //ページ
    col &= plvColsMask;  //ページ内セル桁
    int g = col >> 2;  //グループ
    col &= 3;  //グループ内セル桁
    return (plvWidth * p +
            PLV_HEADER_WIDTH +
            ((PLV_CELL_WIDTH << 2) + PLV_GROUP_GAP) * g +
            PLV_CELL_WIDTH * col);
  }  //plvColToX

  //y = plvRowToY (row)
  //  セル行からY座標を求める
  static int plvRowToY (int row) {
    int p = row >> plvRowsBit;  //ページ
    row &= plvRowsMask;  //ページ内セル行
    int g = row >> 2;  //グループ
    row &= 3;  //グループ内セル行
    return (plvHeight * p +
            PLV_HEADER_HEIGHT +
            ((PLV_CELL_HEIGHT << 2) + PLV_GROUP_GAP) * g +
            PLV_CELL_HEIGHT * row);
  }  //plvRowToY

  //((x << 16) | col) = plvXToXCol (x)
  //  X座標からセル内X座標とセル桁を求める。-1=範囲外
  static int plvXToXCol (int x) {
    if (x < 0 || plvArrgtWidth <= x) {  //表示範囲の外
      return -1;
    }
    int p = x / plvWidth;  //ページ
    x -= plvWidth * p;  //ページ内X座標
    if (x < PLV_HEADER_WIDTH) {  //左ヘッダ
      return -1;
    }
    x -= PLV_HEADER_WIDTH;
    int g = x / ((PLV_CELL_WIDTH << 2) + PLV_GROUP_GAP);  //グループ
    x -= ((PLV_CELL_WIDTH << 2) + PLV_GROUP_GAP) * g;  //グループ内X座標
    if ((PLV_CELL_WIDTH << 2) <= x) {  //グループの間隔
      return -1;
    }
    int col = x / PLV_CELL_WIDTH;  //グループ内セル桁
    x -= PLV_CELL_WIDTH * col;  //セル内X座標
    return (x << 16) | ((p << plvColsBit) + (g << 2) + col);
  }  //plvXToXCol

  //((y << 16) | row) = plvYToYRow (y)
  //  Y座標からセル内Y座標とセル行を求める。-1=範囲外
  static int plvYToYRow (int y) {
    if (y < 0 || plvArrgtHeight <= y) {  //表示範囲の外
      return -1;
    }
    int p = y / plvHeight;  //ページ
    y -= plvHeight * p;  //ページ内Y座標
    if (y < PLV_HEADER_HEIGHT) {  //上ヘッダ
      return -1;
    }
    y -= PLV_HEADER_HEIGHT;
    int g = y / ((PLV_CELL_HEIGHT << 2) + PLV_GROUP_GAP);  //グループ
    y -= ((PLV_CELL_HEIGHT << 2) + PLV_GROUP_GAP) * g;  //グループ内Y座標
    if ((PLV_CELL_HEIGHT << 2) <= y) {  //グループの間隔
      return -1;
    }
    int row = y / PLV_CELL_HEIGHT;  //グループ内セル行
    y -= PLV_CELL_HEIGHT * row;  //セル内Y座標
    return (y << 16) | ((p << plvRowsBit) + (g << 2) + row);
  }  //plvYToYRow

  //plvStart ()
  //  開始
  public static void plvStart () {
    if (RestorableFrame.rfmGetOpened (Settings.SGS_PLV_FRAME_KEY)) {
      plvOpen ();
    }
  }  //plvStart

  //plvOpen ()
  //  開く
  public static void plvOpen () {
    if (plvFrame == null) {
      plvMakeFrame ();
    } else {
      plvUpdateFrame ();
    }
    XEiJ.dbgVisibleMask |= XEiJ.DBG_PLV_VISIBLE_MASK;
    XEiJ.pnlExitFullScreen (false);
    plvFrame.setVisible (true);
  }  //plvOpen

  //plvMakeFrame ()
  //  作る
  //  ここでは開かない
  static void plvMakeFrame () {
    //イメージ
    plvBufferedImage = new BufferedImage (plvImageWidth, plvImageHeight, BufferedImage.TYPE_INT_ARGB);
    plvDrawHeader ();
    //ビットマップ
    plvBitmap = ((DataBufferInt) plvBufferedImage.getRaster ().getDataBuffer ()).getData ();
    //キャンバス
    plvCanvas = new ScrollCanvas (plvBufferedImage, plvArrgtWidth, plvArrgtHeight);
    plvCanvas.setMatColor (new Color (PLV_BACKGROUND_RGB));
    plvCanvas.setMinScaleShift (PLV_MIN_SCALE);
    plvCanvas.setMaxScaleShift (PLV_MAX_SCALE);
    plvCanvas.setScaleShift (plvScaleNumber);
    //アクションリスナー
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        String command = ae.getActionCommand ();
        switch (command) {
        case "Arrgt":
          plvSetArrgt (plvArrgtComboBox.getSelectedIndex () + PLV_MIN_ARRGT);
          if (XEiJ.mpuTask == null) {
            plvUpdateFrame ();
          }
          break;
        case "Scale":
          plvScaleNumber = plvScaleComboBox.getSelectedIndex () + PLV_MIN_SCALE;
          plvCanvas.setScaleShift (plvScaleNumber);
          break;
        case "Hex":
          plvHex = (((JCheckBox) source).isSelected ());
          plvDrawHeader ();
          plvUpdateFrame ();
          break;
        case "Stop":
          plvStoppedRequest = (((JCheckBox) source).isSelected ());
          if (XEiJ.mpuTask == null) {
            plvUpdateFrame ();
          }
          break;
        case "Copy as hexadecimal":
          if (0 <= plvPageToCopy) {
            StringBuilder sb = new StringBuilder ();
            for (int col = 0; col <= plvColsMask; col++) {
              if (col != 0) {
                sb.append (' ');
              }
              int p = col + (plvRowToCopy << plvColsBit);
              int c = (plvPageToCopy == 0 ? plvPal16G8 : plvPal16TS)[p];
              sb.append (String.format ("%04X", c));
            }
            XEiJ.clpCopy (sb.toString ());
            plvPageToCopy = -1;
          }
          break;
        default:
          System.out.println ("unknown action command " + command);
        }
      }
    };
    //テキストフィールド
    plvTextField = new JTextField ();
    plvTextField.setEditable (false);
    plvTextField.setHorizontalAlignment (JTextField.CENTER);
    //ウインドウ
    plvFrame = Multilingual.mlnTitle (
      ComponentFactory.createRestorableSubFrame (
        Settings.SGS_PLV_FRAME_KEY,
        "Palette Viewer",
        null,
        ComponentFactory.createBorderPanel (
          0, 0,
          //CENTER
          plvCanvas,
          //NORTH
          ComponentFactory.createVerticalBox (
            //1行目
            ComponentFactory.createHorizontalBox (
              Box.createHorizontalGlue (),
              //
              Multilingual.mlnText (
                ComponentFactory.createLabel ("Arrgt"),
                "ja", "配置"),
              plvArrgtComboBox = ComponentFactory.createComboBox (
                plvArrgtNumber - PLV_MIN_ARRGT,
                "Arrgt",
                listener,
                "G", "TS", "H", "V"),
              //
              Multilingual.mlnText (
                ComponentFactory.createLabel ("Scale"),
                "ja", "倍率"),
              plvScaleComboBox = ComponentFactory.createComboBox (
                plvScaleNumber - PLV_MIN_SCALE,
                "Scale",
                listener,
                "1/2", "1", "2", "4", "8", "16"),
              //
              Box.createHorizontalStrut (5),
              ComponentFactory.createCheckBox (plvHex, "Hex", listener),
              //
              Box.createHorizontalGlue ()
              ),  //HorizontalBox
            //2行目
            ComponentFactory.createHorizontalBox (
              Box.createHorizontalGlue (),
              //
              plvTextField,
              //
              Box.createHorizontalStrut (5),
              Multilingual.mlnText (
                ComponentFactory.createCheckBox (plvStoppedRequest, "Stop", listener),
                "ja", "停止"),
              //
              Box.createHorizontalGlue ()
              )  //HorizontalBox
            )  //VerticalBox
          )  //BorderPanel
        ),  //SubFrame
      "ja", "パレットビュア");
    //スケールシフトリスナー
    plvCanvas.addScaleShiftListener (new ScrollCanvas.ScaleShiftListener () {
      @Override public void scaleShiftChanged (int scaleShift) {
        plvScaleNumber = scaleShift;
        plvScaleComboBox.setSelectedIndex (plvScaleNumber - PLV_MIN_SCALE);
      }
    });
    //ウインドウリスナー
    ComponentFactory.addListener (
      plvFrame,
      new WindowAdapter () {
        @Override public void windowClosing (WindowEvent we) {
          XEiJ.dbgVisibleMask &= ~XEiJ.DBG_PLV_VISIBLE_MASK;
        }
      });
    //ポップアップメニュー
    plvPopupMenu = ComponentFactory.createPopupMenu (
      Multilingual.mlnText (
        ComponentFactory.createMenuItem ("Copy as hexadecimal", 'C', listener),
        "ja", "16進数でコピー")
      );
    plvPageToCopy = -1;
    //マウスリスナー
    MouseAdapter ma = new MouseAdapter () {
      @Override public void mouseClicked (MouseEvent me) {
        plvTextLocked = !plvTextLocked;  //テキストフィールドのロックを反転する
      }  //mouseClicked
      @Override public void mouseMoved (MouseEvent me) {
        if (!plvTextLocked) {  //テキストフィールドがロックされていないとき
          MouseEvent2D me2D = (MouseEvent2D) me;
          int x = (int) me2D.getX2D ();
          int y = (int) me2D.getY2D ();
          String s = plvGetPixel (x, y);
          plvTextField.setText (s == null ? "" : s);  //テキストフィールドに表示する
        }
      }  //mouseMoved
      @Override public void mousePressed (MouseEvent me) {
        plvShowPopup (me);
      }  //mousePressed
      @Override public void mouseReleased (MouseEvent me) {
        plvShowPopup (me);
      }  //mouseReleased
    };
    plvCanvas.addMouseListener (ma);
    plvCanvas.addMouseMotionListener (ma);
  }  //plvMakeFrame

  //plvShowPopup (me)
  //  ポップアップメニューを表示する
  static void plvShowPopup (MouseEvent me) {
    if (!me.isPopupTrigger ()) {
      return;
    }
    MouseEvent2D me2D = (MouseEvent2D) me;
    int x = (int) me2D.getX2D ();
    int y = (int) me2D.getY2D ();
    int col = plvXToXCol (x);  //セル内X座標<<16|セル桁
    int row = plvYToYRow (y);  //セル内Y座標<<16|セル行
    if (col < 0 || row < 0) {
      return;
    }
    col = (char) col;  //セル桁
    row = (char) row;  //セル行
    if (col <= plvColsMask && row <= plvRowsMask && plvArrgtNumber != 1) {  //グラフィック
      plvPageToCopy = 0;
    } else {  //テキストとスプライト
      plvPageToCopy = 1;
    }
    plvRowToCopy = row & plvRowsMask;
    Point p = plvCanvas.getPopupPoint (me2D);
    plvPopupMenu.show (plvCanvas, p.x, p.y);
  }  //plvShowPopup

  //plvDrawHeader ()
  //  ヘッダを描く
  static void plvDrawHeader () {
    Graphics2D g2 = plvBufferedImage.createGraphics ();
    //背景
    g2.setColor (new Color (PLV_BACKGROUND_RGB));
    g2.fillRect (0, 0, plvArrgtWidth, plvArrgtHeight);
    //ヘッダ
    g2.setColor (new Color (PLV_FOREGROUND_RGB));
    g2.setFont (new Font (PLV_FONT_NAME, PLV_FONT_STYLE, PLV_FONT_SIZE));
    //  タイトル
    if (plvArrgtNumber != 1) {
      plvDrawString (g2,
                     (PLV_HEADER_WIDTH + plvWidth) / 2,  //X座標
                     plvRowToY (0) - PLV_FONT_SIZE * 13 / 12,  //Y座標
                     "Graphic Palette",  //文字列
                     SwingConstants.SOUTH);  //アンカー
    }
    if (plvArrgtNumber != 0) {
      plvDrawString (g2,
                     plvArrgtWidth - plvWidth + (PLV_HEADER_WIDTH + plvWidth) / 2,  //X座標
                     plvArrgtHeight - plvHeight + plvRowToY (0) - PLV_FONT_SIZE * 13 / 12,  //Y座標
                     "Text and Sprite Palette",  //文字列
                     SwingConstants.SOUTH);  //アンカー
    }
    //  上
    for (int row = 0; row < plvArrgtRows; row += (plvRowsMask + 1)) {
      for (int col = 0; col < plvArrgtCols; col++) {
        plvDrawString (g2,
                       plvColToX (col) + PLV_CELL_WIDTH / 2,  //X座標
                       plvRowToY (row) - PLV_FONT_SIZE / 12,  //Y座標
                       String.format (plvHex ? "%X" : "%d", col & plvColsMask),  //文字列
                       SwingConstants.SOUTH);  //アンカー
      }  //for col
    }  //for row
    //  左
    for (int col = 0; col < plvArrgtCols; col += (plvColsMask + 1)) {
      for (int row = 0; row < plvArrgtRows; row++) {
        plvDrawString (g2,
                       plvColToX (col) - PLV_FONT_SIZE / 6,  //X座標
                       plvRowToY (row) + PLV_CELL_HEIGHT / 2,  //Y座標
                       String.format (plvHex ? "%X" : "%d",
                                      plvCellOffset + ((row & plvRowsMask) << plvColsBit)),  //文字列
                       SwingConstants.EAST);  //アンカー
      }  //for row
    }  //for col
  }  //plvDrawHeader

  //plvDrawString (g, x, y, s, d)
  //  文字列を描く
  static void plvDrawString (Graphics2D g2, int x, int y, String s, int d) {
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
  }  //plvDrawString

  //s = plvGetPixel (x, y)
  //  座標からピクセルの情報を求める
  static String plvGetPixel (int x, int y) {
    int col = plvXToXCol (x);  //セル内X座標<<16|セル桁
    int row = plvYToYRow (y);  //セル内Y座標<<16|セル行
    if (col < 0 || row < 0) {  //セル範囲外
      return null;
    }
    x = col >> 16;  //セル内X座標
    y = row >> 16;  //セル内Y座標
    col = (char) col;  //セル桁
    row = (char) row;  //セル行
    int p = (col & plvColsMask) + ((row & plvRowsMask) << plvColsBit);
    if (col <= plvColsMask && row <= plvRowsMask && plvArrgtNumber != 1) {  //グラフィック
      if (plvG65536On) {  //65536色表示あり
        if (x == 0 || x == PLV_CELL_WIDTH - 1 ||
            y == 0 || y == PLV_CELL_HEIGHT - 1 ||
            x == PLV_CELL_WIDTH / 2 - 1 ||
            x == PLV_CELL_WIDTH / 2) {  //パレット範囲外
          return null;
        }
        int h = x < PLV_CELL_WIDTH / 2 ? 0 : 1;  //左半分と右半分
        if ((p & 1) == 0) {  //下位
          p += h;
          int c = plvPal8G16L[p];
          return String.format ("G p=0x??%02X c=0x??%02X(%d,%d,%d,%d)",
                                p, c,
                                c >> 11, (c >> 6) & 31, (c >> 1) & 31, c & 1);
        } else {  //上位
          p = (p & -2) + h;
          int c = plvPal8G16H[p];
          return String.format ("G p=0x%02X?? c=0x%02X??(%d,%d,%d,%d)",
                                p, c >> 8,
                                c >> 11, (c >> 6) & 31, (c >> 1) & 31, c & 1);
        }
      } else {  //16色、256色、65536色表示なし
        if (x == 0 || x == PLV_CELL_WIDTH - 1 ||
            y == 0 || y == PLV_CELL_HEIGHT - 1) {  //パレット範囲外
          return null;
        }
        int c = plvPal16G8[p];
        return String.format ("G p=0x%02X c=0x%04X(%d,%d,%d,%d)",
                              p, c,
                              c >> 11, (c >> 6) & 31, (c >> 1) & 31, c & 1);
      }
    } else {  //テキストとスプライト
      if (x == 0 || x == PLV_CELL_WIDTH - 1 ||
          y == 0 || y == PLV_CELL_HEIGHT - 1) {  //パレット範囲外
        return null;
      }
      int c = plvPal16TS[p];
      return String.format ("TS p=0x%02X c=0x%04X(%d,%d,%d,%d)",
                            p, c,
                            c >> 11, (c >> 6) & 31, (c >> 1) & 31, c & 1);
    }
  }  //plvGetPixel

  //plvUpdateFrame ()
  //  更新する
  static void plvUpdateFrame () {
    if (plvFrame == null) {  //未初期化
      return;
    }
    //停止/動作
    if (plvStopped != plvStoppedRequest) {
      if (plvStoppedRequest) {  //動作→停止
        plvSetStoppedOn ();
      } else {  //停止→動作
        plvSetStoppedOff ();
      }
    }
    if (!plvStopped) {
      plvG65536On = ((VideoController.vcnReg1Port & 2) != 0 &&  //65536色
                     (VideoController.vcnReg3Port & 15) != 0);  //表示あり
    }
    //パレットを表示する
    for (int row = 0; row < plvArrgtRows; row++) {
      int y = plvRowToY (row) + 1;
      for (int col = 0; col < plvArrgtCols; col++) {
        int x = plvColToX (col) + 1;
        int i = x + plvImageWidth * y;
        int p = (col & plvColsMask) + ((row & plvRowsMask) << plvColsBit);
        if (col <= plvColsMask && row <= plvRowsMask && plvArrgtNumber != 1) {  //グラフィック
          if (plvG65536On) {  //65536色表示あり
            for (int v = 0; v < PLV_CELL_HEIGHT - 2; v++) {  //中央の壁
              plvBitmap[i + PLV_CELL_WIDTH / 2 - 2] = PLV_BACKGROUND_RGB;
              plvBitmap[i + PLV_CELL_WIDTH / 2 - 1] = PLV_BACKGROUND_RGB;
              i += plvImageWidth;
            }
            i += -plvImageWidth * (PLV_CELL_HEIGHT - 2);
            for (int h = 0; h < 2; h++) {  //左半分と右半分
              int c = ((p & 1) == 0 ? plvPal8G16L : plvPal8G16H)[(p & -2) + h];
              int d = plvPalTbl[c];
              for (int v = 0; v < PLV_CELL_HEIGHT - 2; v++) {
                for (int u = 0; u < PLV_CELL_WIDTH / 2 - 2; u++) {
                  plvBitmap[i + u] = d;
                }
                i += plvImageWidth;
              }
              i += PLV_CELL_WIDTH / 2 - plvImageWidth * (PLV_CELL_HEIGHT - 2);
            }
          } else {  //16色、256色、65536色表示なし
            int c = plvPal16G8[p];
            int d = plvPalTbl[c];
            for (int v = 0; v < PLV_CELL_HEIGHT - 2; v++) {
              for (int u = 0; u < PLV_CELL_WIDTH - 2; u++) {
                plvBitmap[i + u] = d;
              }
              i += plvImageWidth;
            }
          }
        } else {  //テキストとスプライト
          int c = plvPal16TS[p];
          int d = plvPalTbl[c];
          for (int v = 0; v < PLV_CELL_HEIGHT - 2; v++) {
            for (int u = 0; u < PLV_CELL_WIDTH - 2; u++) {
              plvBitmap[i + u] = d;
            }
            i += plvImageWidth;
          }
        }
      }
    }
    plvCanvas.repaint ();
  }  //plvUpdateFrame

  //plvSetStoppedOn ()
  //  停止する
  static void plvSetStoppedOn () {
    plvStopped = true;
    plvStoppedRequest = true;
    //現在の値をコピーする
    System.arraycopy (VideoController.vcnPalBase[VideoController.VCN_CONTRAST_SCALE * 15], 0, plvCopiedPalTbl, 0, 65536);
    System.arraycopy (VideoController.vcnPal16G8Port, 0, plvCopiedPal16G8, 0, 256);
    System.arraycopy (VideoController.vcnPal16TSPort, 0, plvCopiedPal16TS, 0, 256);
    System.arraycopy (VideoController.vcnPal8G16LPort, 0, plvCopiedPal8G16L, 0, 256);
    System.arraycopy (VideoController.vcnPal8G16HPort, 0, plvCopiedPal8G16H, 0, 256);
    //コピーされた値を参照する
    plvPalTbl = plvCopiedPalTbl;
    plvPal16G8 = plvCopiedPal16G8;
    plvPal16TS = plvCopiedPal16TS;
    plvPal8G16L = plvCopiedPal8G16L;
    plvPal8G16H = plvCopiedPal8G16H;
  }  //plvSetStoppedOn

  //plvSetStoppedOff ()
  //  停止を解除する
  static void plvSetStoppedOff () {
    plvStopped = false;
    //現在の値を参照する
    plvPalTbl = VideoController.vcnPalBase[VideoController.VCN_CONTRAST_SCALE * 15];
    plvPal16G8 = VideoController.vcnPal16G8Port;
    plvPal16TS = VideoController.vcnPal16TSPort;
    plvPal8G16L = VideoController.vcnPal8G16LPort;
    plvPal8G16H = VideoController.vcnPal8G16HPort;
  }  //plvSetStoppedOff

}  //class PaletteViewer
