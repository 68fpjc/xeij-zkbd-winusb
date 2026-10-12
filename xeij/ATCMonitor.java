//========================================================================================
//  ATCMonitor.java
//    en:Address translation caches monitor
//    ja:アドレス変換キャッシュモニタ
//  Copyright (C) 2003-2026 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

//      +------+--------------------------------------------------------------------------------------------------------+
//      |      |                                                                                                    1111
//      |      |          1111111111222222222233333333334444444444555555555566666666667777777777888888888899999999990000|
//      |      |01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123|
//      +------+--------------------------------------------------------------------------------------------------------+
//      |     0|                                                                                                        |
//      |     1|                                       ADDRESS TRANSLATION CACHES                                       |
//      |     2|                                                                                                        |
//      |     3|        USER DATA                 USER CODE              SUPERVISOR DATA           SUPERVISOR CODE      |
//      |     4|    dd/64=ddd% IN USE         dd/64=ddd% IN USE         dd/64=ddd% IN USE         dd/64=ddd% IN USE     |
//      |     5|                                                                                                        |
//      |     6|    LOGICAL  PHYSICAL         LOGICAL  PHYSICAL         LOGICAL  PHYSICAL         LOGICAL  PHYSICAL     |
//      |     7|  0 xxxxxxxx xxxxxxxx GCW   0 xxxxxxxx xxxxxxxx GCW   0 xxxxxxxx xxxxxxxx GCW   0 xxxxxxxx xxxxxxxx GCW |
//      |     8|  1 xxxxxxxx xxxxxxxx GCW   1 xxxxxxxx xxxxxxxx GCW   1 xxxxxxxx xxxxxxxx GCW   1 xxxxxxxx xxxxxxxx GCW |
//      |     9|  2 xxxxxxxx xxxxxxxx GCW   2 xxxxxxxx xxxxxxxx GCW   2 xxxxxxxx xxxxxxxx GCW   2 xxxxxxxx xxxxxxxx GCW |
//      |    10|  3 xxxxxxxx xxxxxxxx GCW   3 xxxxxxxx xxxxxxxx GCW   3 xxxxxxxx xxxxxxxx GCW   3 xxxxxxxx xxxxxxxx GCW |
//      |    11|  4 xxxxxxxx xxxxxxxx GCW   4 xxxxxxxx xxxxxxxx GCW   4 xxxxxxxx xxxxxxxx GCW   4 xxxxxxxx xxxxxxxx GCW |
//      |    12|  5 xxxxxxxx xxxxxxxx GCW   5 xxxxxxxx xxxxxxxx GCW   5 xxxxxxxx xxxxxxxx GCW   5 xxxxxxxx xxxxxxxx GCW |
//      |    13|  6 xxxxxxxx xxxxxxxx GCW   6 xxxxxxxx xxxxxxxx GCW   6 xxxxxxxx xxxxxxxx GCW   6 xxxxxxxx xxxxxxxx GCW |
//      |    14|  7 xxxxxxxx xxxxxxxx GCW   7 xxxxxxxx xxxxxxxx GCW   7 xxxxxxxx xxxxxxxx GCW   7 xxxxxxxx xxxxxxxx GCW |
//      |    15|  8 xxxxxxxx xxxxxxxx GCW   8 xxxxxxxx xxxxxxxx GCW   8 xxxxxxxx xxxxxxxx GCW   8 xxxxxxxx xxxxxxxx GCW |
//      |    16|  9 xxxxxxxx xxxxxxxx GCW   9 xxxxxxxx xxxxxxxx GCW   9 xxxxxxxx xxxxxxxx GCW   9 xxxxxxxx xxxxxxxx GCW |
//      |    17| 10 xxxxxxxx xxxxxxxx GCW  10 xxxxxxxx xxxxxxxx GCW  10 xxxxxxxx xxxxxxxx GCW  10 xxxxxxxx xxxxxxxx GCW |
//      |    18| 11 xxxxxxxx xxxxxxxx GCW  11 xxxxxxxx xxxxxxxx GCW  11 xxxxxxxx xxxxxxxx GCW  11 xxxxxxxx xxxxxxxx GCW |
//      |    19| 12 xxxxxxxx xxxxxxxx GCW  12 xxxxxxxx xxxxxxxx GCW  12 xxxxxxxx xxxxxxxx GCW  12 xxxxxxxx xxxxxxxx GCW |
//      |    20| 13 xxxxxxxx xxxxxxxx GCW  13 xxxxxxxx xxxxxxxx GCW  13 xxxxxxxx xxxxxxxx GCW  13 xxxxxxxx xxxxxxxx GCW |
//      |    21| 14 xxxxxxxx xxxxxxxx GCW  14 xxxxxxxx xxxxxxxx GCW  14 xxxxxxxx xxxxxxxx GCW  14 xxxxxxxx xxxxxxxx GCW |
//      |    22| 15 xxxxxxxx xxxxxxxx GCW  15 xxxxxxxx xxxxxxxx GCW  15 xxxxxxxx xxxxxxxx GCW  15 xxxxxxxx xxxxxxxx GCW |
//      |    23| 16 xxxxxxxx xxxxxxxx GCW  16 xxxxxxxx xxxxxxxx GCW  16 xxxxxxxx xxxxxxxx GCW  16 xxxxxxxx xxxxxxxx GCW |
//      |    24| 17 xxxxxxxx xxxxxxxx GCW  17 xxxxxxxx xxxxxxxx GCW  17 xxxxxxxx xxxxxxxx GCW  17 xxxxxxxx xxxxxxxx GCW |
//      |    25| 18 xxxxxxxx xxxxxxxx GCW  18 xxxxxxxx xxxxxxxx GCW  18 xxxxxxxx xxxxxxxx GCW  18 xxxxxxxx xxxxxxxx GCW |
//      |    26| 19 xxxxxxxx xxxxxxxx GCW  19 xxxxxxxx xxxxxxxx GCW  19 xxxxxxxx xxxxxxxx GCW  19 xxxxxxxx xxxxxxxx GCW |
//      |    27| 20 xxxxxxxx xxxxxxxx GCW  20 xxxxxxxx xxxxxxxx GCW  20 xxxxxxxx xxxxxxxx GCW  20 xxxxxxxx xxxxxxxx GCW |
//      |    28| 21 xxxxxxxx xxxxxxxx GCW  21 xxxxxxxx xxxxxxxx GCW  21 xxxxxxxx xxxxxxxx GCW  21 xxxxxxxx xxxxxxxx GCW |
//      |    29| 22 xxxxxxxx xxxxxxxx GCW  22 xxxxxxxx xxxxxxxx GCW  22 xxxxxxxx xxxxxxxx GCW  22 xxxxxxxx xxxxxxxx GCW |
//      |    30| 23 xxxxxxxx xxxxxxxx GCW  23 xxxxxxxx xxxxxxxx GCW  23 xxxxxxxx xxxxxxxx GCW  23 xxxxxxxx xxxxxxxx GCW |
//      |    31| 24 xxxxxxxx xxxxxxxx GCW  24 xxxxxxxx xxxxxxxx GCW  24 xxxxxxxx xxxxxxxx GCW  24 xxxxxxxx xxxxxxxx GCW |
//      |    32| 25 xxxxxxxx xxxxxxxx GCW  25 xxxxxxxx xxxxxxxx GCW  25 xxxxxxxx xxxxxxxx GCW  25 xxxxxxxx xxxxxxxx GCW |
//      |    33| 26 xxxxxxxx xxxxxxxx GCW  26 xxxxxxxx xxxxxxxx GCW  26 xxxxxxxx xxxxxxxx GCW  26 xxxxxxxx xxxxxxxx GCW |
//      |    34| 27 xxxxxxxx xxxxxxxx GCW  27 xxxxxxxx xxxxxxxx GCW  27 xxxxxxxx xxxxxxxx GCW  27 xxxxxxxx xxxxxxxx GCW |
//      |    35| 28 xxxxxxxx xxxxxxxx GCW  28 xxxxxxxx xxxxxxxx GCW  28 xxxxxxxx xxxxxxxx GCW  28 xxxxxxxx xxxxxxxx GCW |
//      |    36| 29 xxxxxxxx xxxxxxxx GCW  29 xxxxxxxx xxxxxxxx GCW  29 xxxxxxxx xxxxxxxx GCW  29 xxxxxxxx xxxxxxxx GCW |
//      |    37| 30 xxxxxxxx xxxxxxxx GCW  30 xxxxxxxx xxxxxxxx GCW  30 xxxxxxxx xxxxxxxx GCW  30 xxxxxxxx xxxxxxxx GCW |
//      |    38| 31 xxxxxxxx xxxxxxxx GCW  31 xxxxxxxx xxxxxxxx GCW  31 xxxxxxxx xxxxxxxx GCW  31 xxxxxxxx xxxxxxxx GCW |
//      |    39| 32 xxxxxxxx xxxxxxxx GCW  32 xxxxxxxx xxxxxxxx GCW  32 xxxxxxxx xxxxxxxx GCW  32 xxxxxxxx xxxxxxxx GCW |
//      |    40| 33 xxxxxxxx xxxxxxxx GCW  33 xxxxxxxx xxxxxxxx GCW  33 xxxxxxxx xxxxxxxx GCW  33 xxxxxxxx xxxxxxxx GCW |
//      |    41| 34 xxxxxxxx xxxxxxxx GCW  34 xxxxxxxx xxxxxxxx GCW  34 xxxxxxxx xxxxxxxx GCW  34 xxxxxxxx xxxxxxxx GCW |
//      |    42| 35 xxxxxxxx xxxxxxxx GCW  35 xxxxxxxx xxxxxxxx GCW  35 xxxxxxxx xxxxxxxx GCW  35 xxxxxxxx xxxxxxxx GCW |
//      |    43| 36 xxxxxxxx xxxxxxxx GCW  36 xxxxxxxx xxxxxxxx GCW  36 xxxxxxxx xxxxxxxx GCW  36 xxxxxxxx xxxxxxxx GCW |
//      |    44| 37 xxxxxxxx xxxxxxxx GCW  37 xxxxxxxx xxxxxxxx GCW  37 xxxxxxxx xxxxxxxx GCW  37 xxxxxxxx xxxxxxxx GCW |
//      |    45| 38 xxxxxxxx xxxxxxxx GCW  38 xxxxxxxx xxxxxxxx GCW  38 xxxxxxxx xxxxxxxx GCW  38 xxxxxxxx xxxxxxxx GCW |
//      |    46| 39 xxxxxxxx xxxxxxxx GCW  39 xxxxxxxx xxxxxxxx GCW  39 xxxxxxxx xxxxxxxx GCW  39 xxxxxxxx xxxxxxxx GCW |
//      |    47| 40 xxxxxxxx xxxxxxxx GCW  40 xxxxxxxx xxxxxxxx GCW  40 xxxxxxxx xxxxxxxx GCW  40 xxxxxxxx xxxxxxxx GCW |
//      |    48| 41 xxxxxxxx xxxxxxxx GCW  41 xxxxxxxx xxxxxxxx GCW  41 xxxxxxxx xxxxxxxx GCW  41 xxxxxxxx xxxxxxxx GCW |
//      |    49| 42 xxxxxxxx xxxxxxxx GCW  42 xxxxxxxx xxxxxxxx GCW  42 xxxxxxxx xxxxxxxx GCW  42 xxxxxxxx xxxxxxxx GCW |
//      |    50| 43 xxxxxxxx xxxxxxxx GCW  43 xxxxxxxx xxxxxxxx GCW  43 xxxxxxxx xxxxxxxx GCW  43 xxxxxxxx xxxxxxxx GCW |
//      |    51| 44 xxxxxxxx xxxxxxxx GCW  44 xxxxxxxx xxxxxxxx GCW  44 xxxxxxxx xxxxxxxx GCW  44 xxxxxxxx xxxxxxxx GCW |
//      |    52| 45 xxxxxxxx xxxxxxxx GCW  45 xxxxxxxx xxxxxxxx GCW  45 xxxxxxxx xxxxxxxx GCW  45 xxxxxxxx xxxxxxxx GCW |
//      |    53| 46 xxxxxxxx xxxxxxxx GCW  46 xxxxxxxx xxxxxxxx GCW  46 xxxxxxxx xxxxxxxx GCW  46 xxxxxxxx xxxxxxxx GCW |
//      |    54| 47 xxxxxxxx xxxxxxxx GCW  47 xxxxxxxx xxxxxxxx GCW  47 xxxxxxxx xxxxxxxx GCW  47 xxxxxxxx xxxxxxxx GCW |
//      |    55| 48 xxxxxxxx xxxxxxxx GCW  48 xxxxxxxx xxxxxxxx GCW  48 xxxxxxxx xxxxxxxx GCW  48 xxxxxxxx xxxxxxxx GCW |
//      |    56| 49 xxxxxxxx xxxxxxxx GCW  49 xxxxxxxx xxxxxxxx GCW  49 xxxxxxxx xxxxxxxx GCW  49 xxxxxxxx xxxxxxxx GCW |
//      |    57| 50 xxxxxxxx xxxxxxxx GCW  50 xxxxxxxx xxxxxxxx GCW  50 xxxxxxxx xxxxxxxx GCW  50 xxxxxxxx xxxxxxxx GCW |
//      |    58| 51 xxxxxxxx xxxxxxxx GCW  51 xxxxxxxx xxxxxxxx GCW  51 xxxxxxxx xxxxxxxx GCW  51 xxxxxxxx xxxxxxxx GCW |
//      |    59| 52 xxxxxxxx xxxxxxxx GCW  52 xxxxxxxx xxxxxxxx GCW  52 xxxxxxxx xxxxxxxx GCW  52 xxxxxxxx xxxxxxxx GCW |
//      |    60| 53 xxxxxxxx xxxxxxxx GCW  53 xxxxxxxx xxxxxxxx GCW  53 xxxxxxxx xxxxxxxx GCW  53 xxxxxxxx xxxxxxxx GCW |
//      |    61| 54 xxxxxxxx xxxxxxxx GCW  54 xxxxxxxx xxxxxxxx GCW  54 xxxxxxxx xxxxxxxx GCW  54 xxxxxxxx xxxxxxxx GCW |
//      |    62| 55 xxxxxxxx xxxxxxxx GCW  55 xxxxxxxx xxxxxxxx GCW  55 xxxxxxxx xxxxxxxx GCW  55 xxxxxxxx xxxxxxxx GCW |
//      |    63| 56 xxxxxxxx xxxxxxxx GCW  56 xxxxxxxx xxxxxxxx GCW  56 xxxxxxxx xxxxxxxx GCW  56 xxxxxxxx xxxxxxxx GCW |
//      |    64| 57 xxxxxxxx xxxxxxxx GCW  57 xxxxxxxx xxxxxxxx GCW  57 xxxxxxxx xxxxxxxx GCW  57 xxxxxxxx xxxxxxxx GCW |
//      |    65| 58 xxxxxxxx xxxxxxxx GCW  58 xxxxxxxx xxxxxxxx GCW  58 xxxxxxxx xxxxxxxx GCW  58 xxxxxxxx xxxxxxxx GCW |
//      |    66| 59 xxxxxxxx xxxxxxxx GCW  59 xxxxxxxx xxxxxxxx GCW  59 xxxxxxxx xxxxxxxx GCW  59 xxxxxxxx xxxxxxxx GCW |
//      |    67| 60 xxxxxxxx xxxxxxxx GCW  60 xxxxxxxx xxxxxxxx GCW  60 xxxxxxxx xxxxxxxx GCW  60 xxxxxxxx xxxxxxxx GCW |
//      |    68| 61 xxxxxxxx xxxxxxxx GCW  61 xxxxxxxx xxxxxxxx GCW  61 xxxxxxxx xxxxxxxx GCW  61 xxxxxxxx xxxxxxxx GCW |
//      |    69| 62 xxxxxxxx xxxxxxxx GCW  62 xxxxxxxx xxxxxxxx GCW  62 xxxxxxxx xxxxxxxx GCW  62 xxxxxxxx xxxxxxxx GCW |
//      |    70| 63 xxxxxxxx xxxxxxxx GCW  63 xxxxxxxx xxxxxxxx GCW  63 xxxxxxxx xxxxxxxx GCW  63 xxxxxxxx xxxxxxxx GCW |
//      |    71|                                       G:GLOBAL W:WRITE-PROTECTED                                       |
//      |    72|                            0:WRITETHROUGH 1:COPYBACK 2:PRECISE 3:IMPRECISE                             |
//      |    73|                                                                                                        |
//      +------+--------------------------------------------------------------------------------------------------------+
//      |      |                                                                                                    1111
//      |      |          1111111111222222222233333333334444444444555555555566666666667777777777888888888899999999990000|
//      |      |01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123|
//      +------+--------------------------------------------------------------------------------------------------------+

package xeij;

import java.awt.*;  //BasicStroke,BorderLayout,BoxLayout,Color,Component,Container,Cursor,Desktop,Dimension,Font,FlowLayout,Frame,Graphics,Graphics2D,GraphicsDevice,GraphicsEnvironment,GridLayout,Image,Insets,Paint,Point,Rectangle,RenderingHints,Robot,Shape,Stroke,TexturePaint,Toolkit
import java.awt.event.*;  //ActionEvent,ActionListener,ComponentAdapter,ComponentEvent,ComponentListener,FocusAdapter,FocusEvent,FocusListener,InputEvent,KeyAdapter,KeyEvent,KeyListener,MouseAdapter,MouseEvent,MouseListener,MouseMotionAdapter,MouseWheelEvent,WindowAdapter,WindowEvent,WindowListener,WindowStateListener
import java.awt.image.*;  //BufferedImage,DataBuffer,DataBufferByte,DataBufferInt,IndexColorModel
import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,Timer,TimerTask,TreeMap
import javax.swing.*;  //AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JApplet,JButton,JCheckBox,JCheckBoxMenuItem,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException

public class ATCMonitor {

  public static final boolean ACM_ON = true;

  //アドレス変換キャッシュ
  public static int[][] acmCaches;

  //配置
  public static final int ACM_GROUP_COLS = 26;
  public static final int ACM_COLS = ACM_GROUP_COLS * 4;
  public static final String ACM_TITLE = "ADDRESS TRANSLATION CACHES";
  public static final String[] ACM_GROUP_TITLE = {
    "USER DATA",
    "USER CODE",
    "SUPERVISOR DATA",
    "SUPERVISOR CODE",
  };
  public static final String ACM_FOOTNOTE_1 = "G:GLOBAL W:WRITE-PROTECTED";
  public static final String ACM_FOOTNOTE_2 = "0:WRITETHROUGH 1:COPYBACK 2:PRECISE 3:IMPRECISE";

  //キャンバス
  public static final int ACM_WIDTH = 4 * ACM_COLS;
  public static final int ACM_HEIGHT = 6 * 74;
  public static final int ACM_OFFSET = ACM_WIDTH + 3 >> 2;
  public static IndexColorModel acmColorModel;
  public static BufferedImage acmImage;
  public static byte[] acmBitmap;

  //パネル
  public static JPanel acmPanel;  //パネル

  //ウインドウ
  public static JFrame acmFrame;  //ウインドウ

  //タイマー
  public static final int ACM_INTERVAL = 10;
  public static int acmTimer;

  //フォント
  public static final byte[] ACM_FONT_1 = new byte[5 * 127];
  public static final byte[] ACM_FONT_2 = new byte[5 * 127];
  public static final byte[] ACM_FONT_3 = new byte[5 * 127];

  //acmInit ()
  //  初期化
  public static void acmInit () {

    //アドレス変換キャッシュ
    acmCaches = new int[][] {
      MC68060.mmuUserDataCache,
      MC68060.mmuUserCodeCache,
      MC68060.mmuSuperDataCache,
      MC68060.mmuSuperCodeCache,
    };

    //フォント
    {
      final long m = 0b01010100_01010000_01000100_01000000_00010100_00010000_00000100_00000000L;
      int k = 0;
      for (int i = 0; i < 127; i++) {
        int t = Indicator.IND_ASCII_3X5[i];
        int d;
        //d = t >> 14 - 6 & 0b01000000 | t >> 13 - 4 & 0b00010000 | t >> 12 - 2 & 0b00000100;
        d = (int) (m >>> (t >>> 12 - 3 & 7 << 3)) & 255;
        ACM_FONT_1[k] = (byte)  d;
        ACM_FONT_2[k] = (byte) (d * 2);
        ACM_FONT_3[k] = (byte) (d * 3);
        k++;
        //d = t >> 11 - 6 & 0b01000000 | t >> 10 - 4 & 0b00010000 | t >>  9 - 2 & 0b00000100;
        d = (int) (m >>> (t >>>  9 - 3 & 7 << 3)) & 255;
        ACM_FONT_1[k] = (byte)  d;
        ACM_FONT_2[k] = (byte) (d * 2);
        ACM_FONT_3[k] = (byte) (d * 3);
        k++;
        //d = t >>  8 - 6 & 0b01000000 | t >>  7 - 4 & 0b00010000 | t >>  6 - 2 & 0b00000100;
        d = (int) (m >>> (t >>>  6 - 3 & 7 << 3)) & 255;
        ACM_FONT_1[k] = (byte)  d;
        ACM_FONT_2[k] = (byte) (d * 2);
        ACM_FONT_3[k] = (byte) (d * 3);
        k++;
        //d = t <<  6 - 5 & 0b01000000 | t           & 0b00010000 | t >>  3 - 2 & 0b00000100;
        d = (int) (m >>> (t            & 7 << 3)) & 255;
        ACM_FONT_1[k] = (byte)  d;
        ACM_FONT_2[k] = (byte) (d * 2);
        ACM_FONT_3[k] = (byte) (d * 3);
        k++;
        //d = t <<  6 - 2 & 0b01000000 | t <<  4 - 1 & 0b00010000 | t <<  2 - 0 & 0b00000100;
        d = (int) (m >>> (t <<   6 - 3 & 7 << 3)) & 255;
        ACM_FONT_1[k] = (byte)  d;
        ACM_FONT_2[k] = (byte) (d * 2);
        ACM_FONT_3[k] = (byte) (d * 3);
        k++;
      }
    }

    //ウインドウ
    acmFrame = null;

    //タイマー
    acmTimer = 0;

  }  //acmInit()

  //acmStart ()
  public static void acmStart () {
    if (RestorableFrame.rfmGetOpened (Settings.SGS_ACM_FRAME_KEY)) {
      acmOpen ();
    }
  }  //acmStart()

  //acmOpen ()
  //  アドレス変換キャッシュモニタを開く
  public static void acmOpen () {
    if (acmFrame == null) {
      acmMakeFrame ();
    } else {
      acmUpdateFrame ();
    }
    XEiJ.dbgVisibleMask |= XEiJ.DBG_ACM_VISIBLE_MASK;
    XEiJ.pnlExitFullScreen (false);
    acmFrame.setVisible (true);
  }  //acmOpen()

  //acmMakeFrame ()
  //  アドレス変換キャッシュモニタを作る
  //  ここでは開かない
  public static void acmMakeFrame () {

    //キャンバス
    acmColorModel = new IndexColorModel (2, 4,
                                         //                   黒           青           橙           白
                                         new byte[] { (byte) 0x00, (byte) 0x20, (byte) 0xff, (byte) 0xff },  //Red
                                         new byte[] { (byte) 0x00, (byte) 0x20, (byte) 0x87, (byte) 0xff },  //Green
                                         new byte[] { (byte) 0x00, (byte) 0xff, (byte) 0x40, (byte) 0xff });  //Blue
    acmImage = new BufferedImage (ACM_WIDTH, ACM_HEIGHT, BufferedImage.TYPE_BYTE_BINARY, acmColorModel);
    acmBitmap = ((DataBufferByte) acmImage.getRaster ().getDataBuffer ()).getData ();
    acmDrawString3 ((ACM_COLS - ACM_TITLE.length ()) >> 1, 1, ACM_TITLE);
    for (int z = 0; z < 4; z++) {
      int x0 = ACM_GROUP_COLS * z;
      acmDrawString2 (x0 + ((ACM_GROUP_COLS - ACM_GROUP_TITLE[z].length ()) >> 1), 3, ACM_GROUP_TITLE[z]);
      acmDrawString3 (x0 + 6, 4, "/64=");
      acmDrawString3 (x0 + 13, 4, "% IN USE");
      acmDrawString2 (x0 + 4, 6, "LOGICAL  PHYSICAL");
    }
    acmDrawString2 ((ACM_COLS - ACM_FOOTNOTE_1.length ()) >> 1, 71, ACM_FOOTNOTE_1);
    acmDrawString2 ((ACM_COLS - ACM_FOOTNOTE_2.length ()) >> 1, 72, ACM_FOOTNOTE_2);

    //パネル
    acmPanel = ComponentFactory.setFixedSize (
      new JPanel () {
        @Override protected void paintComponent (Graphics g) {
          super.paintComponent (g);
          g.drawImage (acmImage, 0, 0, null);
        }
        @Override protected void paintBorder (Graphics g) {
        }
        @Override protected void paintChildren (Graphics g) {
        }
        @Override public void update (Graphics g) {
        }
      }, ACM_WIDTH, ACM_HEIGHT);
    acmPanel.setBackground (Color.black);
    acmPanel.setOpaque (true);

    //ウインドウ
    acmFrame = Multilingual.mlnTitle (
      ComponentFactory.createRestorableSubFrame (
        Settings.SGS_ACM_FRAME_KEY,
        "Address translation caches monitor",
        null,
        acmPanel
        ),
      "ja", "アドレス変換キャッシュモニタ");  //Multilingual.mlnTitle

    //  ウインドウリスナー
    ComponentFactory.addListener (
      acmFrame,
      new WindowAdapter () {
        @Override public void windowClosing (WindowEvent we) {
          XEiJ.dbgVisibleMask &= ~XEiJ.DBG_ACM_VISIBLE_MASK;
        }
      });

  }  //acmMakeFrame()

  //acmUpdateFrame ()
  //  アドレス変換キャッシュモニタを更新する
  public static void acmUpdateFrame () {

    if (acmFrame == null) {
      return;
    }

    for (int z = 0; z < 4; z++) {
      int[] cache = acmCaches[z];
      int x0 = ACM_GROUP_COLS * z;
      int inuse = 0;
      int t;
      for (int l = 0, i = 0; l < 64; l++, i += 4) {  //ライン,インデックス
        t = XEiJ.FMT_BCD4[l];
        int y = 7 + l;
        int r = cache[i];  //リード用の論理ページアドレス
        if (!MC68060.mmuEnabled || r == 1) {  //無効
          acmDrawChar1 (x0 + 1, y, t >> 4 == 0 ? ' ' : (t >> 4) + '0');  //10の位
          acmDrawChar1 (x0 + 2, y, (t & 15) + '0');  //1の位
          acmDrawString1 (x0 + 4, y, "-------- -------- ---");
        } else {  //有効
          inuse++;
          int w = cache[i + 1];  //ライト用の論理ページアドレス
          int p = cache[i + 2];  //物理ページアドレス
          int f = cache[i + 3];  //フラグ。bit10:グローバル,bit6-5:キャッシュモード
          acmDrawChar3 (x0 + 1, y, t >> 4 == 0 ? ' ' : (t >> 4) + '0');  //10の位
          acmDrawChar3 (x0 + 2, y, (t & 15) + '0');  //1の位
          //論理ページアドレス
          t = r >>> 28;
          acmDrawChar3 (x0 + 4, y, (9 - t >> 4 & 7) + t + '0');
          t = r >>> 24 & 15;
          acmDrawChar3 (x0 + 5, y, (9 - t >> 4 & 7) + t + '0');
          t = r >>> 20 & 15;
          acmDrawChar3 (x0 + 6, y, (9 - t >> 4 & 7) + t + '0');
          t = r >>> 16 & 15;
          acmDrawChar3 (x0 + 7, y, (9 - t >> 4 & 7) + t + '0');
          t = (char) r >>> 12;
          acmDrawChar3 (x0 + 8, y, (9 - t >> 4 & 7) + t + '0');
          t = r >>> 8 & 15;
          acmDrawChar3 (x0 + 9, y, (9 - t >> 4 & 7) + t + '0');
          t = r >>> 4 & 15;
          acmDrawChar3 (x0 + 10, y, (9 - t >> 4 & 7) + t + '0');
          t = r & 15;
          acmDrawChar3 (x0 + 11, y, (9 - t >> 4 & 7) + t + '0');
          //物理ページアドレス
          t = p >>> 28;
          acmDrawChar3 (x0 + 13, y, (9 - t >> 4 & 7) + t + '0');
          t = p >>> 24 & 15;
          acmDrawChar3 (x0 + 14, y, (9 - t >> 4 & 7) + t + '0');
          t = p >>> 20 & 15;
          acmDrawChar3 (x0 + 15, y, (9 - t >> 4 & 7) + t + '0');
          t = p >>> 16 & 15;
          acmDrawChar3 (x0 + 16, y, (9 - t >> 4 & 7) + t + '0');
          t = (char) p >>> 12;
          acmDrawChar3 (x0 + 17, y, (9 - t >> 4 & 7) + t + '0');
          t = p >>> 8 & 15;
          acmDrawChar3 (x0 + 18, y, (9 - t >> 4 & 7) + t + '0');
          t = p >>> 4 & 15;
          acmDrawChar3 (x0 + 19, y, (9 - t >> 4 & 7) + t + '0');
          t = p & 15;
          acmDrawChar3 (x0 + 20, y, (9 - t >> 4 & 7) + t + '0');
          //グローバル
          acmDrawChar3 (x0 + 22, y, (f & MC68060.MMU_DESCRIPTOR_GLOBAL) != 0 ? 'G' : '-');
          //キャッシュモード
          acmDrawChar3 (x0 + 23, y, ((f & MC68060.MMU_DESCRIPTOR_CACHE_MODE) >> 5) + '0');
          //ライトプロテクト
          acmDrawChar3 (x0 + 24, y, w == 1 ? 'W' : '-');
        }  //if 無効/有効
      }  //for l,i
      t = XEiJ.FMT_BCD4[inuse];
      acmDrawChar3 (x0 + 4, 4, t >> 4 == 0 ? ' ' : (t >> 4) + '0');  //10の位
      acmDrawChar3 (x0 + 5, 4, (t & 15) + '0');  //1の位
      t = XEiJ.FMT_BCD4[100 * inuse + 32 >> 6];
      acmDrawChar3 (x0 + 10, 4, t >> 8 == 0 ? ' ' : (t >> 8) + '0');  //100の位
      acmDrawChar3 (x0 + 11, 4, t >> 4 == 0 ? ' ' : (t >> 4 & 15) + '0');  //10の位
      acmDrawChar3 (x0 + 12, 4, (t & 15) + '0');  //1の位
    }  //for z

    acmPanel.repaint ();

  }  //acmUpdateFrame()

  //acmDrawChar1 (x, y, c)
  //  パレットコード1で文字を描く
  public static void acmDrawChar1 (int x, int y, int c) {
    byte[] bb = acmBitmap;
    x += ACM_OFFSET * 6 * y;
    c *= 5;
    bb[x                 ] = ACM_FONT_1[c    ];
    bb[x + ACM_OFFSET    ] = ACM_FONT_1[c + 1];
    bb[x + ACM_OFFSET * 2] = ACM_FONT_1[c + 2];
    bb[x + ACM_OFFSET * 3] = ACM_FONT_1[c + 3];
    bb[x + ACM_OFFSET * 4] = ACM_FONT_1[c + 4];
  }  //acmDrawChar1(int,int,int)

  //acmDrawChar2 (x, y, c)
  //  パレットコード2で文字を描く
  public static void acmDrawChar2 (int x, int y, int c) {
    byte[] bb = acmBitmap;
    x += ACM_OFFSET * 6 * y;
    c *= 5;
    bb[x                 ] = ACM_FONT_2[c    ];
    bb[x + ACM_OFFSET    ] = ACM_FONT_2[c + 1];
    bb[x + ACM_OFFSET * 2] = ACM_FONT_2[c + 2];
    bb[x + ACM_OFFSET * 3] = ACM_FONT_2[c + 3];
    bb[x + ACM_OFFSET * 4] = ACM_FONT_2[c + 4];
  }  //acmDrawChar2(int,int,int)

  //acmDrawChar3 (x, y, c)
  //  パレットコード3で文字を描く
  public static void acmDrawChar3 (int x, int y, int c) {
    byte[] bb = acmBitmap;
    x += ACM_OFFSET * 6 * y;
    c *= 5;
    bb[x                 ] = ACM_FONT_3[c    ];
    bb[x + ACM_OFFSET    ] = ACM_FONT_3[c + 1];
    bb[x + ACM_OFFSET * 2] = ACM_FONT_3[c + 2];
    bb[x + ACM_OFFSET * 3] = ACM_FONT_3[c + 3];
    bb[x + ACM_OFFSET * 4] = ACM_FONT_3[c + 4];
  }  //acmDrawChar3(int,int,int)

  //acmDrawString1 (x, y, s)
  //  パレットコード1で文字列を描く
  public static void acmDrawString1 (int x, int y, String s) {
    acmDrawString1 (x, y, s.toCharArray ());
  }  //acmDrawString1(int,int,String)
  public static void acmDrawString1 (int x, int y, char[] s) {
    byte[] bb = acmBitmap;
    x += ACM_OFFSET * 6 * y;
    for (char c : s) {
      c *= 5;
      bb[x                 ] = ACM_FONT_1[c    ];
      bb[x + ACM_OFFSET    ] = ACM_FONT_1[c + 1];
      bb[x + ACM_OFFSET * 2] = ACM_FONT_1[c + 2];
      bb[x + ACM_OFFSET * 3] = ACM_FONT_1[c + 3];
      bb[x + ACM_OFFSET * 4] = ACM_FONT_1[c + 4];
      x++;
    }
  }  //acmDrawString1(int,int,char[])

  //acmDrawString2 (x, y, s)
  //  パレットコード2で文字列を描く
  public static void acmDrawString2 (int x, int y, String s) {
    acmDrawString2 (x, y, s.toCharArray ());
  }  //acmDrawString2(int,int,String)
  public static void acmDrawString2 (int x, int y, char[] s) {
    byte[] bb = acmBitmap;
    x += ACM_OFFSET * 6 * y;
    for (char c : s) {
      c *= 5;
      bb[x                 ] = ACM_FONT_2[c    ];
      bb[x + ACM_OFFSET    ] = ACM_FONT_2[c + 1];
      bb[x + ACM_OFFSET * 2] = ACM_FONT_2[c + 2];
      bb[x + ACM_OFFSET * 3] = ACM_FONT_2[c + 3];
      bb[x + ACM_OFFSET * 4] = ACM_FONT_2[c + 4];
      x++;
    }
  }  //acmDrawString2(int,int,char[])

  //acmDrawString3 (x, y, s)
  //  パレットコード3で文字列を描く
  public static void acmDrawString3 (int x, int y, String s) {
    acmDrawString3 (x, y, s.toCharArray ());
  }  //acmDrawString3(int,int,String)
  public static void acmDrawString3 (int x, int y, char[] s) {
    byte[] bb = acmBitmap;
    x += ACM_OFFSET * 6 * y;
    for (char c : s) {
      c *= 5;
      bb[x                 ] = ACM_FONT_3[c    ];
      bb[x + ACM_OFFSET    ] = ACM_FONT_3[c + 1];
      bb[x + ACM_OFFSET * 2] = ACM_FONT_3[c + 2];
      bb[x + ACM_OFFSET * 3] = ACM_FONT_3[c + 3];
      bb[x + ACM_OFFSET * 4] = ACM_FONT_3[c + 4];
      x++;
    }
  }  //acmDrawString3(int,int,char[])

}  //class ATCMonitor



