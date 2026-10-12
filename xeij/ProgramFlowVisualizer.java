//========================================================================================
//  ProgramFlowVisualizer.java
//    en:Program flow visualizer -- It visualizes the track of the program counter in real time.
//    ja:プログラムフロービジュアライザ -- プログラムカウンタが通った跡をリアルタイムに可視化します。
//  Copyright (C) 2003-2025 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//  パネル
//    address1    bar   funnel address2                                map
//                                     0000    0400    0800    0c00    1000    1400    1800    1c00    2000
//    00000000 +-------+   +-- 40000000 +---------------------------------------------------------------+ mapY0
//             |       |   |            |                                                               |
//    20000000 |       |   |   40010000 |                                                               |
//             |       |   |            |                                                               |
//    40000000 |       | --+   40020000 |                                                               |
//             |       |   |            |                                                               |
//    60000000 |       |   |   40030000 |                                                               |
//             |       |   |            |                                                               |
//    80000000 |       |   |   40040000 |                                                               |
//             |       |   |            |                                                               |
//    A0000000 |       |   |   40050000 |                                                               |
//             |       |   |            |                                                               |
//    C0000000 |       |   |   40060000 |                                                               |
//             |       |   |            |                                                               |
//    E0000000 |       |   |   40070000 |                                                               |
//             +-------+   +--          +---------------------------------------------------------------+ mapY1
//  バー
//    パネルの左側にアドレス空間全体の最近プログラムカウンタが通った範囲を示すバーを表示する
//  漏斗
//    バーとマップの間にアドレス空間全体から見たマップの表示範囲を示す漏斗を表示する
//  マップ
//    パネルの右側に最近プログラムカウンタが通った範囲を示すマップを表示する
//                            1セル  32バイト  16ワード     2x2ピクセル
//                1ライン   256セル  8Kバイト  4Kワード   512x2ピクセル
//    1マップ  512Kライン  128Mセル  4Gバイト  2Gワード  512x1Mピクセル
//  色
//    最近プログラムカウンタが通ったセルを明るい色で表示する
//  更新
//    0.1秒間隔で更新する
//----------------------------------------------------------------------------------------

package xeij;

import java.awt.*;  //BasicStroke,BorderLayout,BoxLayout,Color,Component,Container,Cursor,Desktop,Dimension,Font,FlowLayout,Frame,Graphics,Graphics2D,GraphicsDevice,GraphicsEnvironment,GridLayout,Image,Insets,Paint,Point,Rectangle,RenderingHints,Robot,Shape,Stroke,TexturePaint,Toolkit
import java.awt.event.*;  //ActionEvent,ActionListener,ComponentAdapter,ComponentEvent,ComponentListener,FocusAdapter,FocusEvent,FocusListener,InputEvent,KeyAdapter,KeyEvent,KeyListener,MouseAdapter,MouseEvent,MouseListener,MouseMotionAdapter,MouseWheelEvent,WindowAdapter,WindowEvent,WindowListener,WindowStateListener
import java.awt.image.*;  //BufferedImage,DataBuffer,DataBufferByte,DataBufferInt,IndexColorModel
import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,Timer,TimerTask,TreeMap
import javax.swing.*;  //AbstractButton,AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JApplet,JButton,JCheckBox,JCheckBoxMenuItem,JComponent,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException

public class ProgramFlowVisualizer {

  public static final boolean PFV_ON = true;

  //サイズ
  public static final int PFV_LOG2_BYTES_PER_CELL = 5;
  public static final int PFV_LOG2_CELLS_PER_LINE = 8;
  public static final int PFV_LOG2_BYTES_PER_LINE = PFV_LOG2_BYTES_PER_CELL + PFV_LOG2_CELLS_PER_LINE;
  public static final int PFV_LOG2_TOTAL_LINES = 32 - PFV_LOG2_BYTES_PER_LINE;
  public static final int PFV_BYTES_PER_CELL = 1 << PFV_LOG2_BYTES_PER_CELL;  //1セルあたりのバイト数
  public static final int PFV_CELLS_PER_LINE = 1 << PFV_LOG2_CELLS_PER_LINE;  //1ラインあたりのセル数
  public static final int PFV_BYTES_PER_LINE = 1 << PFV_LOG2_BYTES_PER_LINE;  //1ラインあたりのバイト数
  public static final int PFV_TOTAL_LINES = 1 << PFV_LOG2_TOTAL_LINES;  //総ライン数

  public static final int PFV_MARGIN_LEFT = 2;  //左マージン
  public static final int PFV_ADDRESS1_WIDTH = 34;  //アドレス1の幅
  public static final int PFV_BAR_WIDTH = 10;  //バーの幅
  public static final int PFV_FUNNEL_WIDTH = 8;  //漏斗の幅
  public static final int PFV_ADDRESS2_WIDTH = 34;  //アドレス2の幅
  public static final int PFV_MAP_WIDTH = PFV_CELLS_PER_LINE << 1;  //マップの幅
  public static final int PFV_MARGIN_RIGHT = 2;  //右マージン

  public static final int PFV_ADDRESS1_X0 = PFV_MARGIN_LEFT;  //アドレス1の左端
  public static final int PFV_BAR_X0 = PFV_ADDRESS1_X0 + PFV_ADDRESS1_WIDTH;  //バーの左端
  public static final int PFV_FUNNEL_X0 = PFV_BAR_X0 + PFV_BAR_WIDTH;  //漏斗の左端
  public static final int PFV_ADDRESS2_X0 = PFV_FUNNEL_X0 + PFV_FUNNEL_WIDTH;  //アドレス2の左端
  public static final int PFV_MAP_X0 = PFV_ADDRESS2_X0 + PFV_ADDRESS2_WIDTH;  //マップの左端
  public static final int PFV_PANEL_WIDTH = PFV_MAP_X0 + PFV_MAP_WIDTH + PFV_MARGIN_RIGHT;  //パネルの幅

  public static final int PFV_MARGIN_TOP = 2;  //上マージン
  public static final int PFV_OFFSET_HEIGHT = 6 + 2;  //オフセットの高さ。横に繋がると読み難いので縦に少しずらす
  public static final int PFV_MAP_MIN_HEIGHT = 128;  //マップの最小の高さ
  public static final int PFV_MAP_DEFAULT_HEIGHT = 256;  //マップのデフォルトの高さ
  public static final int PFV_MARGIN_BOTTOM = 2;  //下マージン

  public static final int PFV_OFFSET_Y0 = PFV_MARGIN_TOP;  //オフセットの上端
  public static final int PFV_MAP_Y0 = PFV_OFFSET_Y0 + PFV_OFFSET_HEIGHT;  //マップの上端
  public static final int PFV_PANEL_MIN_HEIGHT = PFV_MAP_Y0 + PFV_MAP_MIN_HEIGHT + PFV_MARGIN_BOTTOM;  //パネルの最小の高さ
  public static final int PFV_PANEL_DEFAULT_HEIGHT = PFV_MAP_Y0 + PFV_MAP_DEFAULT_HEIGHT + PFV_MARGIN_BOTTOM;  //パネルのデフォルトの高さ

  public static final int PFV_PANEL_MAX_HEIGHT = 1024;  //パネルの最大高さ

  public static int pfvPanelHeight;  //パネルの高さ。PFV_PANEL_MIN_HEIGHT<=pfvPanelHeight<=PFV_PANEL_MAX_HEIGHT
  public static int pfvMapHeight;  //マップの高さ。pfvPanelHeight-(PFV_MARGIN_TOP+PFV_MARGIN_BOTTOM)
  public static int pfvMapY1;  //マップの下端。PFV_MAP_Y0-1+pfvMapHeight
  public static int pfvLines;  //ラインの数。pfvMapHeight+1>>1

  //状態
  public static int pfvLine0;  //開始ライン。max(0,min(PFV_TOTAL_LINES-pfvLines,(int)((long)PFV_TOTAL_LINES*(y-PFV_MAP_Y0)/pfvMapHeight)-(pfvLines>>1)))
  public static int pfvLine1;  //終了ライン。pfvLine0+pfvLines-1
  public static int pfvAddress0;  //開始アドレス。pfvLine0<<PFV_LOG2_BYTES_PER_LINE
  public static int pfvAddress1;  //終了アドレス。(pfvLine1+1<<PFV_LOG2_BYTES_PER_LINE)-2
  public static int pfvFunnelY0;  //漏斗の口の上端。PFV_MAP_Y0+(int)((long)pfvMapHeight*pfvLine0/PFV_TOTAL_LINES)
  public static int pfvFunnelY1;  //漏斗の口の下端。PFV_MAP_Y0+(int)((long)pfvMapHeight*pfvLine1/PFV_TOTAL_LINES)
  public static int pfvPressedX;  //マウスのボタンが押された座標
  public static int pfvPressedY;
  public static int pfvPressedLine0;  //マウスのボタンが押されたときのpfvLine0
  public static int pfvSpan;  //表示範囲。0=256,1=512,2=1024,3=2048,4=4096,5=8192,6=16384,7=32768,8=65536

  //色
  //    赤   黄   緑   水   青   紫   赤   黄
  //   0/6  1/6  2/6  3/6  4/6  5/6  6/6  7/6
  public static final float PFV_COLOR_H0 = 4F / 6F;  //青紫赤黄
  public static final float PFV_COLOR_H1 = 7F / 6F;
  public static final float PFV_COLOR_S0 = 1F;
  public static final float PFV_COLOR_S1 = 1F;
  public static final float PFV_COLOR_B0 = 0.2F;
  public static final float PFV_COLOR_B1 = 1F;
  public static final int PFV_ADDRESS_PALET = 240;  //アドレスのパレット
  public static final int PFV_FUNNEL_PALET = 240;  //漏斗のパレット

  //コンポーネント
  public static BufferedImage pfvImage;
  public static byte[] pfvBitmap;
  public static JPanel pfvPanel;
  public static JFrame pfvFrame;

  //タイマー
  public static final int PFV_INTERVAL = 10;
  public static int pfvTimer;

  //pfvInit ()
  //  初期化
  public static void pfvInit () {
    //サイズ
    pfvPanelHeight = PFV_PANEL_DEFAULT_HEIGHT;
    pfvMapHeight = PFV_MAP_DEFAULT_HEIGHT;
    pfvMapY1 = PFV_MAP_Y0 - 1 + pfvMapHeight;
    pfvLines = pfvMapHeight + 1 >>> 1;
    //状態
    pfvLine0 = 0;
    pfvLine1 = pfvLine0 + pfvLines - 1;
    pfvAddress0 = pfvLine0 << PFV_LOG2_BYTES_PER_LINE;
    pfvAddress1 = (pfvLine1 + 1 << PFV_LOG2_BYTES_PER_LINE) - 2;
    pfvFunnelY0 = PFV_MAP_Y0 + (int) ((long) pfvMapHeight * pfvLine0 / PFV_TOTAL_LINES);
    pfvFunnelY1 = PFV_MAP_Y0 + (int) ((long) pfvMapHeight * pfvLine1 / PFV_TOTAL_LINES);
    pfvPressedX = -1;
    //pfvPressedY = -1;
    //pfvPressedLine0 = -1;
    pfvSpan = 8;
    //色
    byte[] red = new byte[256];
    byte[] green = new byte[256];
    byte[] blue = new byte[256];
    for (int i = 0; i < 256; i++) {
      int rgb = Color.HSBtoRGB (PFV_COLOR_H0 + i * (1F / 256F * (PFV_COLOR_H1 - PFV_COLOR_H0)),  //H
                                PFV_COLOR_S0 + i * (1F / 256F * (PFV_COLOR_S1 - PFV_COLOR_S0)),  //S
                                PFV_COLOR_B0 + i * (1F / 256F * (PFV_COLOR_B1 - PFV_COLOR_B0)));  //B
      red[i] = (byte) (rgb >>> 16);
      green[i] = (byte) (rgb >>> 8);
      blue[i] = (byte) rgb;
    }
    IndexColorModel icm = new IndexColorModel (8, 256, red, green, blue);
    //コンポーネント
    pfvImage = new BufferedImage (PFV_PANEL_WIDTH, PFV_PANEL_MAX_HEIGHT, BufferedImage.TYPE_BYTE_INDEXED, icm);
    pfvBitmap = ((DataBufferByte) pfvImage.getRaster ().getDataBuffer ()).getData ();
    pfvPanel = null;
    pfvFrame = null;
    //タイマー
    pfvTimer = 0;
  }  //pfvInit()

  //pfvStart ()
  public static void pfvStart () {
    if (RestorableFrame.rfmGetOpened (Settings.SGS_PFV_FRAME_KEY)) {
      pfvOpen ();
    }
  }  //pfvStart()

  //pfvOpen ()
  //  プログラムフロービジュアライザを開く
  public static void pfvOpen () {
    if (pfvFrame == null) {
      pfvMakeFrame ();
    }
    XEiJ.dbgVisibleMask |= XEiJ.DBG_PFV_VISIBLE_MASK;
    if (XEiJ.mpuTask == null) {
      pfvUpdate ();
    }
    XEiJ.pnlExitFullScreen (false);
    pfvFrame.setVisible (true);
  }  //pfvOpen()

  //pfvMakeFrame ()
  //  プログラムフロービジュアライザを作る
  //  ここでは開かない
  public static void pfvMakeFrame () {
    //パネル
    pfvPanel = ComponentFactory.setMinimumSize (
      ComponentFactory.setPreferredSize (
        ComponentFactory.setMaximumSize (
          new JPanel () {
            @Override public void paint (Graphics g) {
              g.drawImage (pfvImage, 0, 0, null);
            }
            @Override protected void paintComponent (Graphics g) {
            }
            @Override protected void paintBorder (Graphics g) {
            }
            @Override protected void paintChildren (Graphics g) {
            }
            @Override public void update (Graphics g) {
            }
          },
          PFV_PANEL_WIDTH, PFV_PANEL_MAX_HEIGHT),
        PFV_PANEL_WIDTH, PFV_PANEL_DEFAULT_HEIGHT),
      PFV_PANEL_WIDTH, PFV_PANEL_MIN_HEIGHT);
    //  コンポーネントリスナー
    ComponentFactory.addListener (
      pfvPanel,
      new ComponentAdapter () {
        @Override public void componentResized (ComponentEvent ce) {
          pfvPanelHeight = Math.max (PFV_PANEL_MIN_HEIGHT, Math.min (PFV_PANEL_MAX_HEIGHT, pfvPanel.getHeight ()));
          pfvMapHeight = pfvPanelHeight - (PFV_MARGIN_TOP + PFV_MARGIN_BOTTOM);
          pfvMapY1 = PFV_MAP_Y0 - 1 + pfvMapHeight;
          pfvLines = pfvMapHeight + 1 >> 1;
          pfvLine0 = Math.max (0, Math.min (PFV_TOTAL_LINES - pfvLines, pfvLine0 + pfvLine1 + 1 - pfvLines >> 1));
          pfvLine1 = pfvLine0 + pfvLines - 1;
          pfvAddress0 = pfvLine0 << PFV_LOG2_BYTES_PER_LINE;
          pfvAddress1 = (pfvLine1 + 1 << PFV_LOG2_BYTES_PER_LINE) - 2;
          pfvFunnelY0 = PFV_MAP_Y0 + (int) ((long) pfvMapHeight * pfvLine0 / PFV_TOTAL_LINES);
          pfvFunnelY1 = PFV_MAP_Y0 + (int) ((long) pfvMapHeight * pfvLine1 / PFV_TOTAL_LINES);
          if (XEiJ.mpuTask == null) {
            pfvUpdate ();
          }
        }
      });
    //  キーリスナー
    pfvPanel.setFocusable (true);  //キーイベントを受け付ける
    ComponentFactory.addListener (
      pfvPanel,
      new KeyAdapter () {
        @Override public void keyPressed (KeyEvent ke) {
          int line0;
          switch (ke.getKeyCode ()) {
          case KeyEvent.VK_UP:
            line0 = pfvLine0 - 16;
            break;
          case KeyEvent.VK_DOWN:
            line0 = pfvLine0 + 16;
            break;
          case KeyEvent.VK_PAGE_UP:
            line0 = pfvLine0 - (pfvLines * 15 >> 4);
            break;
          case KeyEvent.VK_PAGE_DOWN:
            line0 = pfvLine0 + (pfvLines * 15 >> 4);
            break;
          default:
            return;
          }
          pfvLine0 = Math.max (0, Math.min (PFV_TOTAL_LINES - pfvLines, line0));
          pfvLine1 = pfvLine0 + pfvLines - 1;
          pfvAddress0 = pfvLine0 << PFV_LOG2_BYTES_PER_LINE;
          pfvAddress1 = (pfvLine1 + 1 << PFV_LOG2_BYTES_PER_LINE) - 2;
          pfvFunnelY0 = PFV_MAP_Y0 + (int) ((long) pfvMapHeight * pfvLine0 / PFV_TOTAL_LINES);
          pfvFunnelY1 = PFV_MAP_Y0 + (int) ((long) pfvMapHeight * pfvLine1 / PFV_TOTAL_LINES);
          if (XEiJ.mpuTask == null) {
            pfvUpdate ();
          }
        }
      });
    //  マウスリスナー
    ComponentFactory.addListener (
      pfvPanel,
      new MouseAdapter () {
        @Override public void mouseClicked (MouseEvent me) {
          int x = me.getX ();
          int y = me.getY ();
          if (PFV_MAP_X0 <= x && x < PFV_MAP_X0 + PFV_MAP_WIDTH && PFV_MAP_Y0 <= y && y <= pfvMapY1) {
            //逆アセンブルする
            DisassembleList.ddpOpen (pfvAddress0 + (x - PFV_MAP_X0 >> 1 << PFV_LOG2_BYTES_PER_CELL) + (y - PFV_MAP_Y0 >> 1 << PFV_LOG2_BYTES_PER_LINE), XEiJ.regSRS, false);
          }
        }
        @Override public void mouseExited (MouseEvent me) {
          pfvPressedX = -1;
        }
        @Override public void mousePressed (MouseEvent me) {
          int x = me.getX ();
          int y = me.getY ();
          if (PFV_BAR_X0 <= x && x < PFV_ADDRESS2_X0) {
            pfvLine0 = Math.max (0, Math.min (PFV_TOTAL_LINES - pfvLines, (int) ((long) PFV_TOTAL_LINES * (y - PFV_MAP_Y0) / pfvMapHeight) - (pfvLines >> 1)));
            pfvLine1 = pfvLine0 + pfvLines - 1;
            pfvAddress0 = pfvLine0 << PFV_LOG2_BYTES_PER_LINE;
            pfvAddress1 = (pfvLine1 + 1 << PFV_LOG2_BYTES_PER_LINE) - 2;
            pfvFunnelY0 = PFV_MAP_Y0 + (int) ((long) pfvMapHeight * pfvLine0 / PFV_TOTAL_LINES);
            pfvFunnelY1 = PFV_MAP_Y0 + (int) ((long) pfvMapHeight * pfvLine1 / PFV_TOTAL_LINES);
            if (XEiJ.mpuTask == null) {
              pfvUpdate ();
            }
          }
          pfvPressedX = x;
          pfvPressedY = y;
          pfvPressedLine0 = pfvLine0;
        }
        @Override public void mouseReleased (MouseEvent me) {
          pfvPressedX = -1;
        }
      });
    //  マウスモーションリスナー
    ComponentFactory.addListener (
      pfvPanel,
      new MouseMotionAdapter () {
        @Override public void mouseDragged (MouseEvent me) {
          if (pfvPressedX >= 0) {
            int x = me.getX ();
            int y = me.getY ();
            pfvLine0 = Math.max (0, Math.min (PFV_TOTAL_LINES - pfvLines,
                                              PFV_BAR_X0 <= x && pfvPressedX < PFV_ADDRESS2_X0 ?
                                              (int) ((long) PFV_TOTAL_LINES * (y - PFV_MAP_Y0) / pfvMapHeight) - (pfvLines >> 1) :
                                              pfvPressedLine0 - (y - pfvPressedY >> 1)));
            pfvLine1 = pfvLine0 + pfvLines - 1;
            pfvAddress0 = pfvLine0 << PFV_LOG2_BYTES_PER_LINE;
            pfvAddress1 = (pfvLine1 + 1 << PFV_LOG2_BYTES_PER_LINE) - 2;
            pfvFunnelY0 = PFV_MAP_Y0 + (int) ((long) pfvMapHeight * pfvLine0 / PFV_TOTAL_LINES);
            pfvFunnelY1 = PFV_MAP_Y0 + (int) ((long) pfvMapHeight * pfvLine1 / PFV_TOTAL_LINES);
            if (XEiJ.mpuTask == null) {
              pfvUpdate ();
            }
          }
        }
      });
    //  マウスホイールリスナー
    ComponentFactory.addListener (
      pfvPanel,
      new MouseWheelListener () {
        @Override public void mouseWheelMoved (MouseWheelEvent mwe) {
          pfvLine0 = Math.max (0, Math.min (PFV_TOTAL_LINES - pfvLines, pfvLine0 + (mwe.getWheelRotation () << 4)));
          pfvLine1 = pfvLine0 + pfvLines - 1;
          pfvAddress0 = pfvLine0 << PFV_LOG2_BYTES_PER_LINE;
          pfvAddress1 = (pfvLine1 + 1 << PFV_LOG2_BYTES_PER_LINE) - 2;
          pfvFunnelY0 = PFV_MAP_Y0 + (int) ((long) pfvMapHeight * pfvLine0 / PFV_TOTAL_LINES);
          pfvFunnelY1 = PFV_MAP_Y0 + (int) ((long) pfvMapHeight * pfvLine1 / PFV_TOTAL_LINES);
          if (XEiJ.mpuTask == null) {
            pfvUpdate ();
          }
        }
      });
    //アクションリスナー
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        switch (ae.getActionCommand ()) {
        case "Clear":
          if (XEiJ.mpuTask == null) {
            BranchLog.blgClear ();
            pfvUpdate ();
          }
          break;
        case "256 records":
          pfvSpan = 0;
          if (XEiJ.mpuTask == null) {
            pfvUpdate ();
          }
          break;
        case "512 records":
          pfvSpan = 1;
          if (XEiJ.mpuTask == null) {
            pfvUpdate ();
          }
          break;
        case "1024 records":
          pfvSpan = 2;
          if (XEiJ.mpuTask == null) {
            pfvUpdate ();
          }
          break;
        case "2048 records":
          pfvSpan = 3;
          if (XEiJ.mpuTask == null) {
            pfvUpdate ();
          }
          break;
        case "4096 records":
          pfvSpan = 4;
          if (XEiJ.mpuTask == null) {
            pfvUpdate ();
          }
          break;
        case "8192 records":
          pfvSpan = 5;
          if (XEiJ.mpuTask == null) {
            pfvUpdate ();
          }
          break;
        case "16384 records":
          pfvSpan = 6;
          if (XEiJ.mpuTask == null) {
            pfvUpdate ();
          }
          break;
        case "32768 records":
          pfvSpan = 7;
          if (XEiJ.mpuTask == null) {
            pfvUpdate ();
          }
          break;
        case "65536 records":
          pfvSpan = 8;
          if (XEiJ.mpuTask == null) {
            pfvUpdate ();
          }
          break;
        }
      }
    };
    //ウインドウ
    ButtonGroup spanGroup = new ButtonGroup ();
    pfvFrame = Multilingual.mlnTitle (
      ComponentFactory.createRestorableSubFrame (
        Settings.SGS_PFV_FRAME_KEY,
        "Program flow visualizer",
        null,
        ComponentFactory.createBorderPanel (
          //CENTER
          pfvPanel,
          //NORTH
          ComponentFactory.createHorizontalBox (
            XEiJ.mpuAddButtonStopped (
              Multilingual.mlnToolTipText (
                ComponentFactory.createImageButton (
                  LnF.LNF_CLEAR_IMAGE,
                  LnF.LNF_CLEAR_DISABLED_IMAGE,
                  "Clear", listener),
                "ja", "クリア")
              ),
            Box.createHorizontalGlue (),
            Multilingual.mlnToolTipText (
              ComponentFactory.createIconRadioButton (spanGroup, pfvSpan == 0,
                                          LnF.LNF_NUMBER_IMAGE_ARRAY[0], LnF.LNF_NUMBER_SELECTED_IMAGE_ARRAY[0],
                                          "256 records", listener),
              "ja", "256 レコード"),
            Multilingual.mlnToolTipText (
              ComponentFactory.createIconRadioButton (spanGroup, pfvSpan == 1,
                                          LnF.LNF_NUMBER_IMAGE_ARRAY[1], LnF.LNF_NUMBER_SELECTED_IMAGE_ARRAY[1],
                                          "512 records", listener),
              "ja", "512 レコード"),
            Multilingual.mlnToolTipText (
              ComponentFactory.createIconRadioButton (spanGroup, pfvSpan == 2,
                                          LnF.LNF_NUMBER_IMAGE_ARRAY[2], LnF.LNF_NUMBER_SELECTED_IMAGE_ARRAY[2],
                                          "1024 records", listener),
              "ja", "1024 レコード"),
            Multilingual.mlnToolTipText (
              ComponentFactory.createIconRadioButton (spanGroup, pfvSpan == 3,
                                          LnF.LNF_NUMBER_IMAGE_ARRAY[3], LnF.LNF_NUMBER_SELECTED_IMAGE_ARRAY[3],
                                          "2048 records", listener),
              "ja", "2048 レコード"),
            Multilingual.mlnToolTipText (
              ComponentFactory.createIconRadioButton (spanGroup, pfvSpan == 4,
                                          LnF.LNF_NUMBER_IMAGE_ARRAY[4], LnF.LNF_NUMBER_SELECTED_IMAGE_ARRAY[4],
                                          "4096 records", listener),
              "ja", "4096 レコード"),
            Multilingual.mlnToolTipText (
              ComponentFactory.createIconRadioButton (spanGroup, pfvSpan == 5,
                                          LnF.LNF_NUMBER_IMAGE_ARRAY[5], LnF.LNF_NUMBER_SELECTED_IMAGE_ARRAY[5],
                                          "8192 records", listener),
              "ja", "8192 レコード"),
            Multilingual.mlnToolTipText (
              ComponentFactory.createIconRadioButton (spanGroup, pfvSpan == 6,
                                          LnF.LNF_NUMBER_IMAGE_ARRAY[6], LnF.LNF_NUMBER_SELECTED_IMAGE_ARRAY[6],
                                          "16384 records", listener),
              "ja", "16384 レコード"),
            Multilingual.mlnToolTipText (
              ComponentFactory.createIconRadioButton (spanGroup, pfvSpan == 7,
                                          LnF.LNF_NUMBER_IMAGE_ARRAY[7], LnF.LNF_NUMBER_SELECTED_IMAGE_ARRAY[7],
                                          "32768 records", listener),
              "ja", "32768 レコード"),
            Multilingual.mlnToolTipText (
              ComponentFactory.createIconRadioButton (spanGroup, pfvSpan == 8,
                                          LnF.LNF_NUMBER_IMAGE_ARRAY[8], LnF.LNF_NUMBER_SELECTED_IMAGE_ARRAY[8],
                                          "65536 records", listener),
              "ja", "65536 レコード"),
            Box.createHorizontalGlue (),
            XEiJ.mpuMakeBreakButton (),  //停止ボタン
            XEiJ.mpuMakeTraceButton (),  //トレース実行ボタン
            XEiJ.mpuMakeTrace10Button (),  //トレース10回ボタン
            XEiJ.mpuMakeTrace100Button (),  //トレース100回ボタン
            XEiJ.mpuMakeStepButton (),  //ステップ実行ボタン
            XEiJ.mpuMakeStep10Button (),  //ステップ10回ボタン
            XEiJ.mpuMakeStep100Button (),  //ステップ100回ボタン
            XEiJ.mpuMakeReturnButton (),  //ステップアンティルリターンボタン
            XEiJ.mpuMakeRunButton ()  //実行ボタン
            )
          )
        ),
      "ja", "プログラムフロービジュアライザ");
    //  ウインドウリスナー
    ComponentFactory.addListener (
      pfvFrame,
      new WindowAdapter () {
        @Override public void windowClosing (WindowEvent we) {
          XEiJ.dbgVisibleMask &= ~XEiJ.DBG_PFV_VISIBLE_MASK;
        }
      });
  }  //pfvMakeFrame()

  //pfvUpdate ()
  //  プログラムフロービジュアライザを更新する
  public static void pfvUpdate () {
    if (BranchLog.blgLock) {
      return;
    }
    BranchLog.blgLock = true;
    BranchLog.blgStop ();
    Arrays.fill (pfvBitmap, 0, PFV_PANEL_WIDTH * pfvPanelHeight, (byte) 0);
    //アドレス1
    {
      int sbits = 32 - 27 + Integer.numberOfLeadingZeros (pfvMapHeight);
      int xbasemax = 1 << 32 - sbits;
      for (int xbase = 0; xbase < xbasemax; xbase++) {
        int x = xbase << sbits;
        int i = PFV_PANEL_WIDTH * (PFV_MAP_Y0 - 2) + PFV_ADDRESS1_X0 + PFV_PANEL_WIDTH * (int) ((long) pfvMapHeight * (x & 0xffffffffL) >>> 32);
        int t =    x >>> 28;
        pfvPutc (        i, (9 - t >> 4 & 7 | 48) + t, PFV_ADDRESS_PALET);
        t =        x >>> 24 & 15;
        pfvPutc (4 * 1 + i, (9 - t >> 4 & 7 | 48) + t, PFV_ADDRESS_PALET);
        t =        x >>> 20 & 15;
        pfvPutc (4 * 2 + i, (9 - t >> 4 & 7 | 48) + t, PFV_ADDRESS_PALET);
        t =        x >>> 16 & 15;
        pfvPutc (4 * 3 + i, (9 - t >> 4 & 7 | 48) + t, PFV_ADDRESS_PALET);
        t = (char) x >>> 12;
        pfvPutc (4 * 4 + i, (9 - t >> 4 & 7 | 48) + t, PFV_ADDRESS_PALET);
        t =        x >>>  8 & 15;
        pfvPutc (4 * 5 + i, (9 - t >> 4 & 7 | 48) + t, PFV_ADDRESS_PALET);
        t =        x >>>  4 & 15;
        pfvPutc (4 * 6 + i, (9 - t >> 4 & 7 | 48) + t, PFV_ADDRESS_PALET);
        t =        x        & 15;
        pfvPutc (4 * 7 + i, (9 - t >> 4 & 7 | 48) + t, PFV_ADDRESS_PALET);
      }
    }
    //漏斗
    {
      int i = PFV_PANEL_WIDTH * PFV_MAP_Y0 + PFV_FUNNEL_X0 + (PFV_FUNNEL_WIDTH >>> 1);
      pfvBitmap[i] =
        pfvBitmap[i + 1] =
          pfvBitmap[i + 2] = 
            pfvBitmap[i + 3] = (byte) PFV_FUNNEL_PALET;
      for (int y = PFV_MAP_Y0; y < pfvFunnelY0; y++) {
        pfvBitmap[i] = (byte) PFV_FUNNEL_PALET;
        i += PFV_PANEL_WIDTH;
      }
      pfvBitmap[i - 3] =
        pfvBitmap[i - 2] =
          pfvBitmap[i - 1] = 
            pfvBitmap[i] = (byte) PFV_FUNNEL_PALET;
      i += PFV_PANEL_WIDTH * (pfvFunnelY1 - pfvFunnelY0);
      pfvBitmap[i - 3] =
        pfvBitmap[i - 2] =
          pfvBitmap[i - 1] = 
            pfvBitmap[i] = (byte) PFV_FUNNEL_PALET;
      for (int y = pfvFunnelY1; y < pfvMapY1; y++) {
        pfvBitmap[i] = (byte) PFV_FUNNEL_PALET;
        i += PFV_PANEL_WIDTH;
      }
      pfvBitmap[i] =
        pfvBitmap[i + 1] =
          pfvBitmap[i + 2] = 
            pfvBitmap[i + 3] = (byte) PFV_FUNNEL_PALET;
    }
    //アドレス2
    {
      final int s = 4096 << PFV_LOG2_BYTES_PER_CELL - 1;
      int x0 = pfvAddress0 + s - 1 & -s;
      int xo1 = pfvAddress1 - x0;
      for (int xo = 0; xo <= xo1; xo += s) {
        int x = x0 + xo;
        int i = PFV_PANEL_WIDTH * (PFV_MAP_Y0 - 2) + PFV_ADDRESS2_X0 + PFV_PANEL_WIDTH * (x - pfvAddress0 >>> PFV_LOG2_BYTES_PER_LINE - 1);
        int t =    x >>> 28;
        pfvPutc (        i, (9 - t >> 4 & 7 | 48) + t, PFV_ADDRESS_PALET);
        t =        x >>> 24 & 15;
        pfvPutc (4 * 1 + i, (9 - t >> 4 & 7 | 48) + t, PFV_ADDRESS_PALET);
        t =        x >>> 20 & 15;
        pfvPutc (4 * 2 + i, (9 - t >> 4 & 7 | 48) + t, PFV_ADDRESS_PALET);
        t =        x >>> 16 & 15;
        pfvPutc (4 * 3 + i, (9 - t >> 4 & 7 | 48) + t, PFV_ADDRESS_PALET);
        t = (char) x >>> 12;
        pfvPutc (4 * 4 + i, (9 - t >> 4 & 7 | 48) + t, PFV_ADDRESS_PALET);
        t =        x >>>  8 & 15;
        pfvPutc (4 * 5 + i, (9 - t >> 4 & 7 | 48) + t, PFV_ADDRESS_PALET);
        t =        x >>>  4 & 15;
        pfvPutc (4 * 6 + i, (9 - t >> 4 & 7 | 48) + t, PFV_ADDRESS_PALET);
        t =        x        & 15;
        pfvPutc (4 * 7 + i, (9 - t >> 4 & 7 | 48) + t, PFV_ADDRESS_PALET);
      }
    }
    //オフセット
    {
      final int s = 16 << PFV_LOG2_BYTES_PER_CELL - 1;
      int y = PFV_OFFSET_Y0 + 2;
      for (int x = 0; x < PFV_BYTES_PER_LINE; x += s) {
        y = PFV_OFFSET_Y0 + PFV_OFFSET_Y0 + 2 - y;
        if (x < 0x10) {
          int i = PFV_PANEL_WIDTH * y + PFV_MAP_X0 - 1 + (x >>> PFV_LOG2_BYTES_PER_CELL - 1);
          pfvPutc (        i, (9 - x >> 4 & 7 | 48) + x, PFV_ADDRESS_PALET);
        } else if (x < 0x100) {
          int i = PFV_PANEL_WIDTH * y + PFV_MAP_X0 - 3 + (x >>> PFV_LOG2_BYTES_PER_CELL - 1);
          int t = x >>>  4 & 15;
          pfvPutc (        i, (9 - t >> 4 & 7 | 48) + t, PFV_ADDRESS_PALET);
          t =     x        & 15;
          pfvPutc (4 * 1 + i, (9 - t >> 4 & 7 | 48) + t, PFV_ADDRESS_PALET);
        } else if (x < 0x1000) {
          int i = PFV_PANEL_WIDTH * y + PFV_MAP_X0 - 5 + (x >>> PFV_LOG2_BYTES_PER_CELL - 1);
          int t = x >>>  8 & 15;
          pfvPutc (        i, (9 - t >> 4 & 7 | 48) + t, PFV_ADDRESS_PALET);
          t =     x >>>  4 & 15;
          pfvPutc (4 * 1 + i, (9 - t >> 4 & 7 | 48) + t, PFV_ADDRESS_PALET);
          t =     x        & 15;
          pfvPutc (4 * 2 + i, (9 - t >> 4 & 7 | 48) + t, PFV_ADDRESS_PALET);
        } else {
          int i = PFV_PANEL_WIDTH * y + PFV_MAP_X0 - 7 + (x >>> PFV_LOG2_BYTES_PER_CELL - 1);
          int t = x >>> 12 & 15;
          pfvPutc (        i, (9 - t >> 4 & 7 | 48) + t, PFV_ADDRESS_PALET);
          t =     x >>>  8 & 15;
          pfvPutc (4 * 1 + i, (9 - t >> 4 & 7 | 48) + t, PFV_ADDRESS_PALET);
          t =     x >>>  4 & 15;
          pfvPutc (4 * 2 + i, (9 - t >> 4 & 7 | 48) + t, PFV_ADDRESS_PALET);
          t =     x        & 15;
          pfvPutc (4 * 3 + i, (9 - t >> 4 & 7 | 48) + t, PFV_ADDRESS_PALET);
        }
      }
    }
    //分岐レコード
    long newestRecord = BranchLog.blgNewestRecord;  //最新のレコードの番号
    long oldestRecord = Math.max (0L, BranchLog.blgNewestRecord - 65535L);  //最古のレコードの番号
    oldestRecord = newestRecord - (long) Math.min ((256 << pfvSpan) - 1, (int) (newestRecord - oldestRecord));  //表示範囲を制限する
    for (long record = oldestRecord; record <= newestRecord; record++) {
      int palet = 255 - ((int) (newestRecord - record) >>> pfvSpan);  //1周していない場合があるのでnewestからの距離を使う
      int address0, address1;
      {
        int i = (char) record << BranchLog.BLG_RECORD_SHIFT;
        address0 = BranchLog.blgArray[i] & ~1;
        address1 = BranchLog.blgArray[i + 1] & ~1;
      }
      //バー
      {
        int y0 = (int) ((long) pfvMapHeight * (address0 & 0xffffffffL) >>> 32);
        int y1 = (int) ((long) pfvMapHeight * (address1 & 0xffffffffL) >>> 32);
        int i = PFV_PANEL_WIDTH * PFV_MAP_Y0 + PFV_BAR_X0 + PFV_PANEL_WIDTH * y0;
        for (int y = y0; y <= y1; y++) {
          pfvBitmap[i] =
            pfvBitmap[i + 1] =
              pfvBitmap[i + 2] =
                pfvBitmap[i + 3] =
                  pfvBitmap[i + 4] =
                    pfvBitmap[i + 5] =
                      pfvBitmap[i + 6] =
                        pfvBitmap[i + 7] = (byte) palet;
          i += PFV_PANEL_WIDTH;
        }
      }
      //マップ
      if ((address0 & 0xffffffffL) <= (pfvAddress1 & 0xffffffffL) && (pfvAddress0 & 0xffffffffL) <= (address1 & 0xffffffffL)) {  //分岐レコードの範囲と表示されているマップの範囲が重なっている
        if ((address0 & 0xffffffffL) < (pfvAddress0 & 0xffffffffL)) {
          address0 = pfvAddress0;
        }
        if ((address1 & 0xffffffffL) > (pfvAddress1 & 0xffffffffL)) {
          address1 = pfvAddress1;
        }
        int line = address0 >>> PFV_LOG2_BYTES_PER_LINE;
        int line1 = address1 >>> PFV_LOG2_BYTES_PER_LINE;
        int i0 = PFV_PANEL_WIDTH * PFV_MAP_Y0 + PFV_MAP_X0 + PFV_PANEL_WIDTH * (line - pfvLine0 << 1);
        int i = i0 + ((address0 & PFV_BYTES_PER_LINE - 2) >>> PFV_LOG2_BYTES_PER_CELL - 1 & -2);
        for (; line <= line1; line++) {
          int i1 = i0 + (line < line1 ? PFV_BYTES_PER_LINE - 2 >>> PFV_LOG2_BYTES_PER_CELL - 1 & -2 :
                         (address1 & PFV_BYTES_PER_LINE - 2) >>> PFV_LOG2_BYTES_PER_CELL - 1 & -2);
          for (; i <= i1; i += 2) {
            pfvBitmap[i] =
              pfvBitmap[i + 1] =
                pfvBitmap[i + PFV_PANEL_WIDTH] =
                  pfvBitmap[i + (PFV_PANEL_WIDTH + 1)] = (byte) palet;
          }
          i = i0 += PFV_PANEL_WIDTH << 1;
        }
      }
    }  //for record
    pfvPanel.repaint ();
    pfvTimer = PFV_INTERVAL;
    BranchLog.blgLock = false;
  }  //pfvUpdate()

  //pfvPutc (i, c, p)
  //  1文字表示
  public static void pfvPutc (int i, int c, int p) {
    int t = Indicator.IND_ASCII_3X5[c];
    if (t << 17 < 0) {
      pfvBitmap[                          i] = (byte) p;
    }
    if (t << 18 < 0) {
      pfvBitmap[                      1 + i] = (byte) p;
    }
    if (t << 19 < 0) {
      pfvBitmap[                      2 + i] = (byte) p;
    }
    if (t << 20 < 0) {
      pfvBitmap[PFV_PANEL_WIDTH         + i] = (byte) p;
    }
    if (t << 21 < 0) {
      pfvBitmap[PFV_PANEL_WIDTH     + 1 + i] = (byte) p;
    }
    if (t << 22 < 0) {
      pfvBitmap[PFV_PANEL_WIDTH     + 2 + i] = (byte) p;
    }
    if (t << 23 < 0) {
      pfvBitmap[PFV_PANEL_WIDTH * 2     + i] = (byte) p;
    }
    if ((byte) t < 0) {  //t << 24 < 0
      pfvBitmap[PFV_PANEL_WIDTH * 2 + 1 + i] = (byte) p;
    }
    if (t << 25 < 0) {
      pfvBitmap[PFV_PANEL_WIDTH * 2 + 2 + i] = (byte) p;
    }
    if (t << 26 < 0) {
      pfvBitmap[PFV_PANEL_WIDTH * 3     + i] = (byte) p;
    }
    if (t << 27 < 0) {
      pfvBitmap[PFV_PANEL_WIDTH * 3 + 1 + i] = (byte) p;
    }
    if (XEiJ.TEST_BIT_3_SHIFT ? t << 28 < 0 : (t & 8) != 0) {
      pfvBitmap[PFV_PANEL_WIDTH * 3 + 2 + i] = (byte) p;
    }
    if (XEiJ.TEST_BIT_2_SHIFT ? t << 29 < 0 : (t & 4) != 0) {
      pfvBitmap[PFV_PANEL_WIDTH * 4     + i] = (byte) p;
    }
    if (XEiJ.TEST_BIT_1_SHIFT ? t << 30 < 0 : (t & 2) != 0) {
      pfvBitmap[PFV_PANEL_WIDTH * 4 + 1 + i] = (byte) p;
    }
    if (XEiJ.TEST_BIT_0_SHIFT ? t << 31 != 0 : (t & 1) != 0) {
      pfvBitmap[PFV_PANEL_WIDTH * 4 + 2 + i] = (byte) p;
    }
  }  //pfvPutc(int,int,int)

}  //class ProgramFlowVisualizer



