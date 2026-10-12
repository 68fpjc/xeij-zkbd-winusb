//========================================================================================
//  Mouse.java
//    en:Mouse
//    ja:マウス
//  Copyright (C) 2003-2025 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.awt.*;  //Cursor
import java.awt.event.*;  //MouseEvent
import java.awt.image.*;  //BufferedImage
import javax.swing.*;
import javax.swing.event.*;  //ChangeListener

public class Mouse {

  //  パネルのマウスカーソルを変更する
  //    X68000のマウスカーソルが表示されているときはホストのマウスカーソルを消す
  //    X68000のマウスカーソルが表示されていないときはホストのマウスカーソルをX68000と同じサイズにする
  //  SCCから要求があったときマウスデータを作る
  //    シームレスモードのときは逆アクセラレーションを行う
  //      X68000のマウスカーソルがホストのマウスカーソルの真下に来るようにマウスの移動データを作る
  //  キーボードが操作されたときはキーボードにデータを渡す

  //逆アクセラレーション
  //  ROMのアクセラレーション処理のコード
  //    ;<d0.w:移動量
  //    ;>d0.w:移動量
  //    ;?d1
  //    accelerate:
  //        clr.w   -(sp)
  //        tst.w   d0
  //        bgt.s   1f
  //        addq.w  #1,(sp)
  //        neg.w   d0
  //    1:  move.w  d0,d1   ;  0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25
  //        lsr.w   #3,d1   ;  0  0  0  0  0  0  0  0  1  1  1  1  1  1  1  1  2  2  2  2  2  2  2  2  3  3
  //        bne.s   2f
  //        move.w  #1,d1   ;  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  2  2  2  2  2  2  2  2  3  3
  //    2:  mulu.w  d1,d0   ;  0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 32 34 36 38 40 42 44 46 72 75
  //        move.w  d0,d1
  //        lsr.w   #2,d1   ;  0  0  0  0  1  1  1  1  2  2  2  2  3  3  3  3  8  8  9  9 10 10 11 11 18 18
  //        add.w   d1,d0   ;  0  1  2  3  5  6  7  8 10 11 12 13 15 16 17 18 40 42 45 47 50 52 55 57 90 93
  //        tst.w   (sp)+
  //        beq.s   3f
  //        neg.w   d0
  //    3:  rts
  //  アクセラレーションテーブル
  //    変位:移動距離
  //      0:   0   1:   1   2:   2   3:   3   4:   5   5:   6   6:   7   7:   8   8:  10   9:  11
  //     10:  12  11:  13  12:  15  13:  16  14:  17  15:  18  16:  40  17:  42  18:  45  19:  47
  //     20:  50  21:  52  22:  55  23:  57  24:  90  25:  93  26:  97  27: 101  28: 105  29: 108
  //     30: 112  31: 116  32: 160  33: 165  34: 170  35: 175  36: 180  37: 185  38: 190  39: 195
  //     40: 250  41: 256  42: 262  43: 268  44: 275  45: 281  46: 287  47: 293  48: 360  49: 367
  //     50: 375  51: 382  52: 390  53: 397  54: 405  55: 412  56: 490  57: 498  58: 507  59: 516
  //     60: 525  61: 533  62: 542  63: 551  64: 640  65: 650  66: 660  67: 670  68: 680  69: 690
  //     70: 700  71: 710  72: 810  73: 821  74: 832  75: 843  76: 855  77: 866  78: 877  79: 888
  //     80:1000  81:1012 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  //    ~~~~~~~~~~~~~~~~~~ 82:1025  83:1037  84:1050  85:1062  86:1075  87:1087  88:1210  89:1223
  //     90:1237  91:1251  92:1265  93:1278  94:1292  95:1306  96:1440  97:1455  98:1470  99:1485
  //    100:1500 101:1515 102:1530 103:1545 104:1690 105:1706 106:1722 107:1738 108:1755 109:1771
  //    110:1787 111:1803 112:1960 113:1977 114:1995 115:2012 116:2030 117:2047 118:2065 119:2082
  //    120:2250 121:2268 122:2287 123:2306 124:2325 125:2343 126:2362 127:2381 128:2560
  //  逆アクセラレーション
  //    現在のマウスカーソルの位置とX68000のマウス座標の距離を超えない最大の移動距離をアクセラレーションテーブルから探してその変位に符号を付けて返す
  //    例えば距離が639ピクセルのとき63(639-551=88),23(88-57=31),15(31-18=13),11(13-13=0)の4回で移動が完了する
  public static final int[] MUS_DEACCELERATION_TABLE = new int[1025];  //逆アクセラレーションテーブル

  //マウスカーソル
  public static final String[][] MUS_CURSOR_PATTERN = {
    {
    },
    {
      "00.........",
      "010........",
      "0110.......",
      "01110......",
      "011110.....",
      "0111110....",
      "01111110...",
      "011111110..",
      "0111111110.",
      "01111100000",
      "0110110....",
      "010.0110...",
      "00..0110...",
      ".....0110..",
      ".....0110..",
      "......00...",
    },
  };

  //モード
  //  シームレス
  //    X68000のマウスカーソルが動作環境のマウスポインタを追いかける
  //  エクスクルーシブ
  //    動作環境のマウスポインタを独占して相対座標を利用できるようにする
  public static boolean musSeamlessOn;  //true=シームレス,false=エクスクルーシブ
  public static boolean musFollowScrollOn;  //true=シームレスのときテキスト画面のスクロールに追従する
  public static boolean musExclusiveStart;  //true=エクスクルーシブに切り替えた直後
  public static boolean musEdgeAccelerationOn;  //true=シームレスのとき縁部加速を行う
  public static boolean musHostsPixelUnitsOn;  //true=エクスクルーシブのときマウスはホストの画素単位で動く,false=X68000の画素単位で動く

  //マウスの状態
  public static boolean musButtonLeft;  //マウスの左ボタンの状態。true=押されている。マウスカーソルがスクリーン上になくても有効
  public static boolean musButtonRight;  //マウスの右ボタンの状態。true=押されている。マウスカーソルがスクリーン上になくても有効
  public static int musData;  //マウスのボタンのデータ。0=ボタンが押されていない,1=左ボタンだけ押されている,2=右ボタンだけ押されている,3=左右のボタンが押されている。マウスカーソルがスクリーン上にあるときだけセットされる
  public static int musExtraData;  //マウスのボタンの延長データ。0=ボタンが押されていない,1=左ボタンだけ押されている,2=右ボタンだけ押されている,3=左右のボタンが押されている。マウスカーソルがスクリーン上にあるときだけセットされる。ボタンが押されてからSCCのポートBが読み出されるまで押されたままになる
  public static int musPanelX;  //パネル座標
  public static int musPanelY;
  public static int musScreenX;  //スクリーン座標。スクリーン上にあるとは限らない
  public static int musScreenY;
  public static boolean musOnScreen;  //true=マウスカーソルがスクリーン上にある
  public static boolean musOnKeyboard;  //true=マウスカーソルがキーボード上にある

  //ホイール
  public static final int MUS_WHEEL_TRACE      = 0;  //トレースとステップ
  public static final int MUS_WHEEL_CLICK      = 1;  //左クリックと右クリック
  public static final int MUS_WHEEL_DO_NOTHING = 2;  //何もしない
  public static int musWheelButton;  //ホイールで押されたボタン。1=左ボタン,2=右ボタン
  public static long musWheelReleaseTime;  //ホイールで押されたボタンが離される時刻

  //マウスカーソル
  public static boolean musCursorAvailable;  //true=カスタムカーソルを利用できる
  public static int musCursorNumber;  //表示されているマウスカーソルの番号
  public static Cursor[] musCursorArray;  //マウスカーソルの配列

  //マウスのボタン
  public static boolean musOutputButtonStatus;  //true=ボタンの状態を出力する
  public static int musNumberOfButtons;  //ボタンの数
  public static int musLastModifiersEx;  //前回のmodifiersEx
  public static boolean musCtrlRightOn;  //Ctrlキー＋左ボタンを右ボタンとみなす

  public static final int SHIFT_MASK          = 0x00000001;
  public static final int CTRL_MASK           = 0x00000002;
  public static final int META_MASK           = 0x00000004;
  public static final int BUTTON3_MASK        = 0x00000004;
  public static final int ALT_MASK            = 0x00000008;
  public static final int BUTTON2_MASK        = 0x00000008;
  public static final int BUTTON1_MASK        = 0x00000010;
  public static final int ALT_GRAPH_MASK      = 0x00000020;

  public static final int SHIFT_DOWN_MASK     = 0x00000040;
  public static final int CTRL_DOWN_MASK      = 0x00000080;
  public static final int META_DOWN_MASK      = 0x00000100;
  public static final int ALT_DOWN_MASK       = 0x00000200;
  public static final int BUTTON1_DOWN_MASK   = 0x00000400;
  public static final int BUTTON2_DOWN_MASK   = 0x00000800;
  public static final int BUTTON3_DOWN_MASK   = 0x00001000;
  public static final int ALT_GRAPH_DOWN_MASK = 0x00002000;
  public static final int BUTTON4_DOWN_MASK   = 0x00004000;
  public static final int BUTTON5_DOWN_MASK   = 0x00008000;

  //マウスリスナー、マウスモーションリスナー、マウスホイールリスナー
  //
  //  マウスの操作とイベント
  //                                  modifiers   modifiersEx
  //    button1  left        pressed  0x00000010  0x00000400
  //                        released  0x00000010  0x00000000
  //                         clicked  0x00000010  0x00000000
  //    button2  wheel       pressed  0x00000008  0x00000800
  //                        released  0x00000008  0x00000200  Altがセットされる。clickedは発生しない
  //    button3  right       pressed  0x00000004  0x00001000
  //                        released  0x00000004  0x00000100  Metaがセットされる
  //                         clicked  0x00000004  0x00000100  Metaがセットされる
  //    button4  back        pressed  0x00000000  0x00004000
  //                        released  0x00000000  0x00000000  clickedは発生しない
  //    button5  forward     pressed  0x00000000  0x00008000
  //                        released  0x00000000  0x00000000  clickedは発生しない
  //    wheel    pull     wheelmoved  preciseWheelRotation = 1.000000
  //                                  scrollAmount = 3
  //                                  scrollType = 0
  //                                  unitsToScroll = 3
  //                                  wheelRotation = 1
  //             push     wheelmoved  preciseWheelRotation = -1.000000
  //                                  scrollAmount = 3
  //                                  scrollType = 0
  //                                  unitsToScroll = -3
  //                                  wheelRotation = -1
  //             left     左スクロールではイベントが発生しない
  //             right    右スクロールではイベントが発生しない
  //    ダブルクリックはleftを2回クリックしているだけ
  //    ズームインとズームアウトはkeyCode=0x11=VK_CONTROL,keyLocation=2=KEY_LOCATION_LEFTをpressしてホイールを回している
  //
  //  Macbookのマルチタッチトラックパッドでクリックする方法
  //    左クリック
  //      「リンゴ」→「システム環境設定」→「トラックパッド」の「タップでクリック」をONにする
  //      1本指でタップする
  //    右クリック
  //      「リンゴ」→「システム環境設定」→「トラックパッド」の「副ボタンのクリック」をONにする
  //      「2本指でクリックまたはタップ」を選択する
  //      2本指でタップする
  //    メモ
  //      control+タップはCtrlがセットされた左クリックでしかない
  //        個々のアプリケーションがこの操作に右クリックと同じ機能を割り当てなければ右クリックの代わりにならない
  //      有効になっていない2本指タップやcontrol+タップでもmousePressedとmouseReleasedが発生する
  //        右クリックとして機能しないのはmousePressedとmouseReleasedの間隔が短すぎるためかも知れない
  //
  //        Button1  左ボタン
  //   Ctrl+Button1  右ボタン
  //        Button2  マウスモード切り替え。mouseClickedは発生しない。modifiersExにAltがセットされる。Alt+Button2は無効
  //        Button3  右ボタン。modifiersExにMetaがセットされるでMeta+Button3は無効
  //        Button4  停止・再開
  //        Button5  トレース1回
  //  Shift+Button5  ステップ1回
  //

  //移動速度
  public static final int MUS_SPEED_SCALE_MIN = 0;
  public static final int MUS_SPEED_SCALE_MAX = 40;
  public static final int MUS_SPEED_SCALE_MID = (MUS_SPEED_SCALE_MAX - MUS_SPEED_SCALE_MIN) >> 1;
  public static int musSpeedScaleIndex;  //マウスの移動速度のスケール。MUS_SPEED_SCALE_MIN～MUS_SPEED_SCALE_MAX
  public static int musSpeedRatioX;  //マウスの移動速度の係数*65536
  public static int musSpeedRatioY;
  public static final String[] musSpeedTexts = new String[MUS_SPEED_SCALE_MAX - MUS_SPEED_SCALE_MIN + 1];  //スケールのテキストの配列
  public static JLabel musSpeedLabel;  //スケールのラベル
  public static JSlider musSpeedSlider;  //スケールを指定するスライダー

  //メニュー
  public static JCheckBoxMenuItem musSeamlessMouseCheckBox;
  public static JCheckBoxMenuItem musFollowScrollCheckBox;
  public static JCheckBoxMenuItem musCtrlRightCheckBox;
  public static JCheckBoxMenuItem musEdgeAccelerationCheckBox;
  public static Box musMouseCursorSpeedBox;
  public static JCheckBoxMenuItem musHostsPixelUnitsCheckBox;

  //musInit ()
  //  初期化
  public static void musInit () {
    musSeamlessOn = Settings.sgsGetOnOff ("seamless");
    musFollowScrollOn = Settings.sgsGetOnOff ("followscroll");
    musCtrlRightOn = Settings.sgsGetOnOff ("ctrlright");
    musEdgeAccelerationOn = Settings.sgsGetOnOff ("edgeaccel");
    musHostsPixelUnitsOn = Settings.sgsGetOnOff ("hostspixelunits");
    musSpeedScaleIndex = Math.max (MUS_SPEED_SCALE_MIN, Math.min (MUS_SPEED_SCALE_MAX, Settings.sgsGetInt ("mousespeed", MUS_SPEED_SCALE_MID)));
    //
    musExclusiveStart = false;
    musButtonLeft = false;
    musButtonRight = false;
    musData = 0;
    musExtraData = 0;
    musPanelX = 0;
    musPanelY = 0;
    musScreenX = 0;
    musScreenY = 0;
    musOnScreen = false;
    musOnKeyboard = false;
    musWheelButton = 0;
    musWheelReleaseTime = 0L;
    //逆アクセラレーション
    {
      int index = 0;
      for (int delta = 0; delta <= 81; delta++) {
        int next = delta + 1;
        if (next >= 8) {
          next *= next >> 3;
        }
        next += next >> 2;
        while (index < next) {  //delta==81のときnext==1025
          MUS_DEACCELERATION_TABLE[index++] = delta;
        }
      }
    }
    //マウスカーソル
    musCursorAvailable = false;
    try {
      Toolkit toolkit = Toolkit.getDefaultToolkit ();
      Dimension bestCursorSize = toolkit.getBestCursorSize (16, 16);
      int width = bestCursorSize.width;
      int height = bestCursorSize.height;
      if (width >= 16 && height >= 16) {  //カスタムカーソルを利用できるとき
        BufferedImage cursorImage = new BufferedImage (width, height, BufferedImage.TYPE_INT_ARGB);
        int[] cursorBitmap = ((DataBufferInt) cursorImage.getRaster ().getDataBuffer ()).getData ();
        Point point = new Point (0, 0);
        musCursorArray = new Cursor[MUS_CURSOR_PATTERN.length];
        for (int i = 0; i < MUS_CURSOR_PATTERN.length; i++) {
          String[] ss = MUS_CURSOR_PATTERN[i];
          int h = ss.length;
          for (int y = 0; y < height; y++) {
            String s = y < h ? ss[y] : "";
            int w = s.length ();
            for (int x = 0; x < width; x++) {
              char c = x < w ? s.charAt (x) : '.';
              cursorBitmap[x + width * y] = 0xff000000 & ('.' - c) | -(c & 1);
            }
          }
          musCursorArray[i] = toolkit.createCustomCursor (cursorImage, point, "XEiJ_" + i);
        }
        musCursorAvailable = true;
        musCursorNumber = 1;
      }
    } catch (Exception e) {
    }
    //マウスのボタン
    musOutputButtonStatus = false;
    musNumberOfButtons = -1;
    try {
      musNumberOfButtons = MouseInfo.getNumberOfButtons ();  //手元では5だった
    } catch (Exception e) {
    }
    musLastModifiersEx = 0;
    //musCtrlRightOn = false;

    //ラベル
    //musSpeedTexts = new String[MUS_SPEED_SCALE_MAX - MUS_SPEED_SCALE_MIN + 1];
    for (int i = MUS_SPEED_SCALE_MIN; i <= MUS_SPEED_SCALE_MAX; i++) {
      musSpeedTexts[i - MUS_SPEED_SCALE_MIN] = String.format ("%4.2f", Math.pow (4.0, (double) (i - MUS_SPEED_SCALE_MID) / (double) MUS_SPEED_SCALE_MID));
    }
    musSpeedLabel = ComponentFactory.createLabel (musSpeedTexts[MUS_SPEED_SCALE_MID]);
    //スライダー
    musSpeedSlider = ComponentFactory.setEnabled (
      ComponentFactory.setPreferredSize (
        ComponentFactory.createHorizontalSlider (
          MUS_SPEED_SCALE_MIN,
          MUS_SPEED_SCALE_MAX,
          musSpeedScaleIndex,
          (MUS_SPEED_SCALE_MAX - MUS_SPEED_SCALE_MIN) / 4,
          1,
          musSpeedTexts,
          new ChangeListener () {
            @Override public void stateChanged (ChangeEvent ce) {
              musSetSpeedScaleIndex (((JSlider) ce.getSource ()).getValue ());
            }
          }),
        LnF.lnfFontSize * 18, LnF.lnfFontSize * 2 + 28),
      XEiJ.rbtRobot != null);
    musSetSpeedScaleIndex (musSpeedScaleIndex);

    //メニュー
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        String command = ae.getActionCommand ();
        switch (command) {
        case "Seamless mouse":  //シームレスマウス
          musSetSeamlessOn (((JCheckBoxMenuItem) source).isSelected ());
          break;
        case "Follow scroll":  //スクロールに追従する
          musFollowScrollOn = ((JCheckBoxMenuItem) source).isSelected ();
          break;
        case "Ctrl-key + left-button = right-button":  //Ctrl キー＋左ボタン＝右ボタン
          musCtrlRightOn = ((JCheckBoxMenuItem) source).isSelected ();
          break;
        case "Edge acceleration":  //縁部加速
          musSetEdgeAccelerationOn (((JCheckBoxMenuItem) source).isSelected ());
          break;
        case "Use host's pixel units":  //ホストの画素単位を使う
          musSetHostsPixelUnitsOn (((JCheckBoxMenuItem) source).isSelected ());
          break;
        default:
          System.out.println ("unknown action command " + command);
        }
      }
    };  //ActionListener
    //
    musSeamlessMouseCheckBox =
      ComponentFactory.setEnabled (
        Multilingual.mlnText (
          ComponentFactory.createCheckBoxMenuItem (musSeamlessOn, "Seamless mouse", listener),
          "ja", "シームレスマウス"),
        XEiJ.rbtRobot != null);
    musFollowScrollCheckBox =
      ComponentFactory.setEnabled (
        Multilingual.mlnText (
          ComponentFactory.createCheckBoxMenuItem (musFollowScrollOn, "Follow scroll", listener),
          "ja", "スクロールに追従する"),
        XEiJ.rbtRobot != null);
    //
    musCtrlRightCheckBox =
      Multilingual.mlnText (
        ComponentFactory.createCheckBoxMenuItem (
          musCtrlRightOn,
          "Ctrl-key + left-button = right-button",
          listener),
        "ja", "Ctrl キー＋左ボタン＝右ボタン");
    musEdgeAccelerationCheckBox =
      Multilingual.mlnText (
        ComponentFactory.createCheckBoxMenuItem (musEdgeAccelerationOn, "Edge acceleration", listener),
        "ja", "縁部加速");
    musMouseCursorSpeedBox =
      ComponentFactory.createHorizontalBox (
        Box.createHorizontalGlue (),
        Multilingual.mlnText (
          ComponentFactory.createLabel ("Mouse cursor speed "),
          "ja", "マウスカーソルの速度 "),
        musSpeedLabel,
        Box.createHorizontalGlue ()
        );
    musHostsPixelUnitsCheckBox =
      ComponentFactory.setEnabled (
        Multilingual.mlnText (
          ComponentFactory.createCheckBoxMenuItem (musHostsPixelUnitsOn, "Use host's pixel units", listener),
          "ja", "ホストの画素単位を使う"),
        XEiJ.rbtRobot != null);

  }  //musInit()

  //musTini ()
  //  後始末
  public static void musTini () {
    Settings.sgsPutOnOff ("seamless", musSeamlessOn);
    Settings.sgsPutOnOff ("followscroll", musFollowScrollOn);
    Settings.sgsPutOnOff ("ctrlright", musCtrlRightOn);
    Settings.sgsPutOnOff ("edgeaccel", musEdgeAccelerationOn);
    Settings.sgsPutOnOff ("hostspixelunits", musHostsPixelUnitsOn);
    Settings.sgsPutInt ("mousespeed", musSpeedScaleIndex, MUS_SPEED_SCALE_MID);
  }  //musTini

  public static void musSetSpeedScaleIndex (int i) {
    musSpeedScaleIndex = i;
    musSpeedLabel.setText (musSpeedTexts[i]);
    musUpdateSpeedRatio ();
  }  //musSetSpeedScaleIndex(int)

  public static void musUpdateSpeedRatio () {
    double scale = Math.pow (4.0, (double) (musSpeedScaleIndex - MUS_SPEED_SCALE_MID) / (double) MUS_SPEED_SCALE_MID);
    if (musHostsPixelUnitsOn) {
      //musSpeedRatioX = (int) Math.round (65536.0 * scale * (double) XEiJ.pnlScreenWidth / (double) XEiJ.pnlZoomWidth);
      //musSpeedRatioY = (int) Math.round (65536.0 * scale * (double) XEiJ.pnlScreenHeight / (double) XEiJ.pnlZoomHeight);
      musSpeedRatioX = (int) (65536.0 * scale * (double) XEiJ.pnlScreenWidth / (double) XEiJ.pnlZoomWidth);
      musSpeedRatioY = (int) (65536.0 * scale * (double) XEiJ.pnlScreenHeight / (double) XEiJ.pnlZoomHeight);
    } else {
      //musSpeedRatioX = (int) Math.round (65536.0 * scale);
      //musSpeedRatioY = (int) Math.round (65536.0 * scale);
      musSpeedRatioX = (int) (65536.0 * scale);
      musSpeedRatioY = (int) (65536.0 * scale);
    }
  }  //musUpdateSpeedRatio()

  //musStart ()
  //  マウスの動作を開始する
  public static void musStart () {

    ComponentFactory.addListener (
      XEiJ.pnlCanvasOrPanel,
      new MouseAdapter () {
        @Override public void mouseClicked (MouseEvent me) {
          if (musOutputButtonStatus) {
            int modifiersEx = me.getModifiersEx ();
            if ((modifiersEx & BUTTON1_DOWN_MASK) != 0) {
              System.out.println (String.format ("mouse button %d/%d was clicked. (0x%08x)",
                                                 1, musNumberOfButtons, modifiersEx));
            }
            if ((modifiersEx & BUTTON2_DOWN_MASK) != 0) {
              System.out.println (String.format ("mouse button %d/%d was clicked. (0x%08x)",
                                                 2, musNumberOfButtons, modifiersEx));
            }
            if ((modifiersEx & BUTTON3_DOWN_MASK) != 0) {
              System.out.println (String.format ("mouse button %d/%d was clicked. (0x%08x)",
                                                 3, musNumberOfButtons, modifiersEx));
            }
            if ((modifiersEx & BUTTON4_DOWN_MASK) != 0) {
              System.out.println (String.format ("mouse button %d/%d was clicked. (0x%08x)",
                                                 4, musNumberOfButtons, modifiersEx));
            }
            if ((modifiersEx & BUTTON5_DOWN_MASK) != 0) {
              System.out.println (String.format ("mouse button %d/%d was clicked. (0x%08x)",
                                                 5, musNumberOfButtons, modifiersEx));
            }
            if ((modifiersEx & (BUTTON1_DOWN_MASK |
                                BUTTON2_DOWN_MASK |
                                BUTTON3_DOWN_MASK |
                                BUTTON4_DOWN_MASK |
                                BUTTON5_DOWN_MASK)) == 0) {
              System.out.println (String.format ("mouse button ?/%d was clicked. (0x%08x)",
                                                 musNumberOfButtons, modifiersEx));
            }
          }
          if (!XEiJ.pnlCanvasOrPanel.isFocusOwner ()) {
            XEiJ.pnlCanvasOrPanel.requestFocusInWindow ();
          }
        }
        //@Override public void mouseEntered (MouseEvent me) {
        //}
        @Override public void mouseExited (MouseEvent me) {
          if (musOnScreen) {  //スクリーンから出た
            musOnScreen = false;
          }
          if (musOnKeyboard) {  //キーボードから出た
            musOnKeyboard = false;
            if (Keyboard.kbdPointedIndex >= 0) {  //ポイントされているキーがある
              Keyboard.kbdHover (0, 0);  //ポイントを解除する
            }
          }
        }
        @Override public void mousePressed (MouseEvent me) {
          musPressedOrReleased (me, true);  //マウスのボタンが操作された
        }
        @Override public void mouseReleased (MouseEvent me) {
          musPressedOrReleased (me, false);  //マウスのボタンが操作された
        }
        @Override public void mouseDragged (MouseEvent me) {
          musDraggedOrMoved (me);  //マウスが動いた
        }
        @Override public void mouseMoved (MouseEvent me) {
          musDraggedOrMoved (me);  //マウスが動いた
        }
        @Override public void mouseWheelMoved (MouseWheelEvent mwe) {
          int modifiersEx = mwe.getModifiersEx ();
          if (musOutputButtonStatus) {
            double preciseWheelRotation = mwe.getPreciseWheelRotation ();
            int scrollAmount = mwe.getScrollAmount ();
            int scrollType = mwe.getScrollType ();
            int unitsToScroll = mwe.getUnitsToScroll ();
            int wheelRotation = mwe.getWheelRotation ();
            System.out.println (String.format ("mouse wheel moved (0x%08x)", modifiersEx));
            System.out.println (String.format ("  preciseWheelRotation = %f", preciseWheelRotation));
            System.out.println (String.format ("  scrollAmount = %d", scrollAmount));
            System.out.println (String.format ("  scrollType = %d", scrollType));
            System.out.println (String.format ("  unitsToScroll = %d", unitsToScroll));
            System.out.println (String.format ("  wheelRotation = %d", wheelRotation));
          }
          int wheelRotation = mwe.getWheelRotation ();  //高解像度マウスは端数が蓄積するまで0が報告される
          if (0 < wheelRotation) {  //スクロールアップ
            ButtonFunction.bfnExecute (ButtonFunction.Button.WHEELUP, modifiersEx, true, null);
          } else if (wheelRotation < 0) {  //スクロールダウン
            ButtonFunction.bfnExecute (ButtonFunction.Button.WHEELDOWN, modifiersEx, true, null);
          }
          //マウスホイールイベントを消費する
          mwe.consume ();
        }
      });

  }  //musStart()

  //musPressedOrReleased (me, pressed)
  //  マウスのボタンが操作された
  public static void musPressedOrReleased (MouseEvent me, boolean pressed) {
    //  InputEvent.getModifiers()は変化したものだけ返す
    //  InputEvent.getModifiersEx()は変化していないものも含めて現在の状態を返す
    int modifiersEx = me.getModifiersEx ();
    int pressedMask = ~musLastModifiersEx & modifiersEx;  //0→1
    int releasedMask = musLastModifiersEx & ~modifiersEx;  //1→0
    musLastModifiersEx = modifiersEx;
    if (musOutputButtonStatus) {
      if ((pressedMask & BUTTON1_DOWN_MASK) != 0) {
        System.out.println (String.format ("mouse button %d/%d was pressed. (0x%08x)",
                                           1, musNumberOfButtons, modifiersEx));
      } else if ((releasedMask & BUTTON1_DOWN_MASK) != 0) {
        System.out.println (String.format ("mouse button %d/%d was released. (0x%08x)",
                                           1, musNumberOfButtons, modifiersEx));
      }
      if ((pressedMask & BUTTON2_DOWN_MASK) != 0) {
        System.out.println (String.format ("mouse button %d/%d was pressed. (0x%08x)",
                                           2, musNumberOfButtons, modifiersEx));
      } else if ((releasedMask & BUTTON2_DOWN_MASK) != 0) {
        System.out.println (String.format ("mouse button %d/%d was released. (0x%08x)",
                                           2, musNumberOfButtons, modifiersEx));
      }
      if ((pressedMask & BUTTON3_DOWN_MASK) != 0) {
        System.out.println (String.format ("mouse button %d/%d was pressed. (0x%08x)",
                                           3, musNumberOfButtons, modifiersEx));
      } else if ((releasedMask & BUTTON3_DOWN_MASK) != 0) {
        System.out.println (String.format ("mouse button %d/%d was released. (0x%08x)",
                                           3, musNumberOfButtons, modifiersEx));
      }
      if ((pressedMask & BUTTON4_DOWN_MASK) != 0) {
        System.out.println (String.format ("mouse button %d/%d was pressed. (0x%08x)",
                                           4, musNumberOfButtons, modifiersEx));
      } else if ((releasedMask & BUTTON4_DOWN_MASK) != 0) {
        System.out.println (String.format ("mouse button %d/%d was released. (0x%08x)",
                                           4, musNumberOfButtons, modifiersEx));
      }
      if ((pressedMask & BUTTON5_DOWN_MASK) != 0) {
        System.out.println (String.format ("mouse button %d/%d was pressed. (0x%08x)",
                                           5, musNumberOfButtons, modifiersEx));
      } else if ((releasedMask & BUTTON5_DOWN_MASK) != 0) {
        System.out.println (String.format ("mouse button %d/%d was released. (0x%08x)",
                                           5, musNumberOfButtons, modifiersEx));
      }
      if (((pressedMask | releasedMask) & (BUTTON1_DOWN_MASK |
                                           BUTTON2_DOWN_MASK |
                                           BUTTON3_DOWN_MASK |
                                           BUTTON4_DOWN_MASK |
                                           BUTTON5_DOWN_MASK)) == 0) {
        System.out.println (String.format ("mouse button ?/%d was %s. (0x%08x)",
                                           musNumberOfButtons, pressed ? "pressed" : "released", modifiersEx));
      }
    }
    if (musCtrlRightOn &&  //Ctrlキー＋左ボタンを右ボタンとみなす
        (modifiersEx & MouseEvent.CTRL_DOWN_MASK) != 0) {  //Ctrlキーが押されている
      if ((pressedMask & MouseEvent.BUTTON1_DOWN_MASK) != 0) {  //左ボタンが押された
        pressedMask = ((pressedMask & ~MouseEvent.BUTTON1_DOWN_MASK) |  //左ボタンをクリアして
                       MouseEvent.BUTTON3_DOWN_MASK);  //右ボタンをセットする
      }
      if ((releasedMask & MouseEvent.BUTTON1_DOWN_MASK) != 0) {  //左ボタンが離された
        releasedMask = ((releasedMask & ~MouseEvent.BUTTON1_DOWN_MASK) |  //左ボタンをクリアして
                        MouseEvent.BUTTON3_DOWN_MASK);  //右ボタンをセットする
      }
    }
    if ((pressedMask & MouseEvent.BUTTON1_DOWN_MASK) != 0) {  //左ボタンが押された
      musButtonLeft = true;
      //if (musOnScreen) {  //マウスデータはスクリーン上で押されたときだけON
      if (musOnScreen && (musSeamlessOn || XEiJ.frmIsActive)) {  //マウスデータはスクリーン上で押されたとき、エクスクルーシブのときは更にフォーカスがあるとき、だけON
        musData |= 1;
        musExtraData |= 1;
      } else {
        musData &= ~1;
      }
    } else if ((releasedMask & MouseEvent.BUTTON1_DOWN_MASK) != 0) {  //左ボタンが離された
      musButtonLeft = false;
      musData &= ~1;
    }
    if (musNumberOfButtons < 3) {  //2ボタンマウスのとき
      if ((pressedMask & (MouseEvent.BUTTON2_DOWN_MASK |
                          MouseEvent.BUTTON3_DOWN_MASK)) != 0) {  //右ボタンが押された
        if ((modifiersEx & MouseEvent.ALT_DOWN_MASK) != 0) {  //Altキーが押されている
          musSetSeamlessOn (!musSeamlessOn);  //シームレス/エクスクルーシブを切り替える
        } else {  //Altキーが押されていない
          musButtonRight = true;
          //if (musOnScreen) {  //マウスデータはスクリーン上で押されたときだけON
          if (musOnScreen && (musSeamlessOn || XEiJ.frmIsActive)) {  //マウスデータはスクリーン上で押されたとき、エクスクルーシブのときは更にフォーカスがあるとき、だけON
            musData |= 2;
            musExtraData |= 2;
          } else {
            musData &= ~2;
          }
        }
      } else if ((releasedMask & (MouseEvent.BUTTON2_DOWN_MASK |
                                  MouseEvent.BUTTON3_DOWN_MASK)) != 0) {  //右ボタンが離された
        musButtonRight = false;
        musData &= ~2;
      }
    } else {  //3ボタンマウスのとき
      if ((pressedMask & MouseEvent.BUTTON2_DOWN_MASK) != 0) {  //ホイールが押された
        ButtonFunction.bfnExecute (ButtonFunction.Button.WHEEL, modifiersEx, true, null);
      } else if ((releasedMask & MouseEvent.BUTTON2_DOWN_MASK) != 0) {  //ホイールが離された
        ButtonFunction.bfnExecute (ButtonFunction.Button.WHEEL, modifiersEx, false, null);
      }
      if ((pressedMask & MouseEvent.BUTTON3_DOWN_MASK) != 0) {  //右ボタンが押された
        musButtonRight = true;
        //if (musOnScreen) {  //マウスデータはスクリーン上で押されたときだけON
        if (musOnScreen && (musSeamlessOn || XEiJ.frmIsActive)) {  //マウスデータはスクリーン上で押されたとき、エクスクルーシブのときは更にフォーカスがあるとき、だけON
          musData |= 2;
          musExtraData |= 2;
        } else {
          musData &= ~2;
        }
      } else if ((releasedMask & MouseEvent.BUTTON3_DOWN_MASK) != 0) {  //右ボタンが離された
        musButtonRight = false;
        musData &= ~2;
      }
      if (4 <= musNumberOfButtons) {  //4ボタンマウスのとき
        if ((pressedMask & BUTTON4_DOWN_MASK) != 0) {  //ボタン4が押された
          ButtonFunction.bfnExecute (ButtonFunction.Button.BUTTON4, modifiersEx, true, null);
        } else if ((releasedMask & BUTTON4_DOWN_MASK) != 0) {  //ボタン4が離された
          ButtonFunction.bfnExecute (ButtonFunction.Button.BUTTON4, modifiersEx, false, null);
        }
        if (5 <= musNumberOfButtons) {  //5ボタンマウスのとき
          if ((pressedMask & BUTTON5_DOWN_MASK) != 0) {  //ボタン5が押された
            ButtonFunction.bfnExecute (ButtonFunction.Button.BUTTON5, modifiersEx, true, null);
          } else if ((releasedMask & BUTTON5_DOWN_MASK) != 0) {  //ボタン5が離された
            ButtonFunction.bfnExecute (ButtonFunction.Button.BUTTON5, modifiersEx, false, null);
          }
        }
      }
    }
    musDraggedOrMovedSub (me);
    if (TextCopy.txcEncloseEachTime && musOnScreen) {
      if ((pressedMask & MouseEvent.BUTTON1_DOWN_MASK) != 0) {  //左ボタンが押された
        TextCopy.txcMousePressed (musScreenX, musScreenY);
      } else if ((releasedMask & MouseEvent.BUTTON1_DOWN_MASK) != 0) {  //左ボタンが離された
        TextCopy.txcMouseReleased (musScreenX, musScreenY);
      }
    }
  }  //musPressedOrReleased(MouseEvent,boolean)

  //musDraggedOrMoved (me)
  //  マウスが動いた
  public static void musDraggedOrMoved (MouseEvent me) {
    musDraggedOrMovedSub (me);
    if (TextCopy.txcEncloseEachTime && musOnScreen) {
      TextCopy.txcMouseMoved (musScreenX, musScreenY);
    }
  }
  public static void musDraggedOrMovedSub (MouseEvent me) {
    int x = musPanelX = me.getX ();
    int y = musPanelY = me.getY ();
    if (XEiJ.PNL_ROTATION_ON) {
      if (XEiJ.pnlScreenX1 <= x && x < XEiJ.pnlScreenX2 &&
          XEiJ.pnlScreenY1 <= y && y < XEiJ.pnlScreenY2) {  //左にある
        musOnScreen = true;
        musScreenX = (int) Math.round (XEiJ.pnlInverseL00 * (double) x + XEiJ.pnlInverseL01 * (double) y + XEiJ.pnlInverseL02);
        musScreenY = (int) Math.round (XEiJ.pnlInverseL10 * (double) x + XEiJ.pnlInverseL11 * (double) y + XEiJ.pnlInverseL12);
      } else if (XEiJ.pnlScreenX3 <= x && x < XEiJ.pnlScreenX4 &&
                 XEiJ.pnlScreenY3 <= y && y < XEiJ.pnlScreenY4) {  //右にある
        musOnScreen = true;
        musScreenX = (int) Math.round (XEiJ.pnlInverseR00 * (double) x + XEiJ.pnlInverseR01 * (double) y + XEiJ.pnlInverseR02);
        musScreenY = (int) Math.round (XEiJ.pnlInverseR10 * (double) x + XEiJ.pnlInverseR11 * (double) y + XEiJ.pnlInverseR12);
      } else {
        musOnScreen = false;
      }
    } else {
      if (XEiJ.pnlScreenX1 <= x && x < XEiJ.pnlScreenX1 + XEiJ.pnlZoomWidth &&
          XEiJ.pnlScreenY1 <= y && y < XEiJ.pnlScreenY1 + XEiJ.pnlZoomHeight) {  //スクリーン上にある
        musOnScreen = true;  //スクリーンに入った
        musScreenX = (x - XEiJ.pnlScreenX1) * XEiJ.pnlZoomRatioInX >> 16;  //端数は切り捨てる
        musScreenY = (y - XEiJ.pnlScreenY1) * XEiJ.pnlZoomRatioInY >> 16;
        if (CRTC.crtDuplication) {  //ラスタ2度読み
          musScreenY >>= 1;
        }
      } else {  //スクリーン上にない
        musOnScreen = false;  //スクリーンから出た
      }
    }
    if (XEiJ.pnlKeyboardX <= x && x < XEiJ.pnlKeyboardX + Keyboard.kbdWidth &&
        XEiJ.pnlKeyboardY <= y && y < XEiJ.pnlKeyboardY + Keyboard.kbdHeight) {  //キーボード上にある
      musOnKeyboard = true;  //キーボードに入った
      Keyboard.kbdHover (x - XEiJ.pnlKeyboardX, y - XEiJ.pnlKeyboardY);
    } else {  //キーボード上にない
      if (musOnKeyboard) {  //キーボードから出た
        musOnKeyboard = false;
        if (Keyboard.kbdPointedIndex >= 0) {  //ポイントされているキーがあった
          Keyboard.kbdHover (0, 0);  //ポイントを解除する
        }
      }
    }
  }  //musDraggedOrMoved(MouseEvent)

  //musSetSeamlessOn (on)
  //  シームレス/エクスクルーシブを切り替える
  public static void musSetSeamlessOn (boolean on) {
    if (XEiJ.rbtRobot == null) {  //ロボットが使えないときは切り替えない(シームレスのみ)
      return;
    }
    if (musSeamlessOn != on) {
      musSeamlessOn = on;
      if (on) {  //エクスクルーシブ→シームレス
        musShow ();
        //ホストのマウスカーソルをX68000のマウスカーソルの位置に移動させる
        int x, y;
        if (XEiJ.currentMPU < Model.MPU_MC68LC040) {  //MMUなし
          if (Z8530.SCC_FSX_MOUSE &&
              Z8530.sccFSXMouseHook != 0 &&  //FSX.Xが常駐している
              MainMemory.mmrRls (0x0938) == Z8530.sccFSXMouseHook) {  //マウス受信データ処理ルーチンがFSX.Xを指している。SX-Windowが動作中
            int xy = MainMemory.mmrRls (Z8530.sccFSXMouseWork + 0x0a);
            x = (xy >> 16) - CRTC.crtR10TxXPort;  //SX-Windowのマウスカーソルの見かけのX座標
            y = (short) xy - CRTC.crtR11TxYPort;  //SX-Windowのマウスカーソルの見かけのY座標
          } else {  //SX-Windowが動作中ではない
            int xy = MainMemory.mmrRls (0x0ace);
            x = xy >> 16;  //IOCSのマウスカーソルのX座標
            y = (short) xy;  //IOCSのマウスカーソルのY座標
            if (musFollowScrollOn) {
              x -= CRTC.crtR10TxXPort;
              y -= CRTC.crtR11TxYPort;
            }
          }
        } else {  //MMUあり
          if (Z8530.SCC_FSX_MOUSE &&
              Z8530.sccFSXMouseHook != 0 &&  //FSX.Xが常駐している
              MC68060.mmuPeekLongData (0x0938, 1) == Z8530.sccFSXMouseHook) {  //マウス受信データ処理ルーチンがFSX.Xを指している。SX-Windowが動作中
            int xy = MC68060.mmuPeekLongData (Z8530.sccFSXMouseWork + 0x0a, 1);
            x = (xy >> 16) - CRTC.crtR10TxXPort;  //SX-Windowのマウスカーソルの見かけのX座標
            y = (short) xy - CRTC.crtR11TxYPort;  //SX-Windowのマウスカーソルの見かけのY座標
          } else {  //SX-Windowが動作中ではない
            int xy = MC68060.mmuPeekLongData (0x0ace, 1);
            x = xy >> 16;  //IOCSのマウスカーソルのX座標
            y = (short) xy;  //IOCSのマウスカーソルのY座標
            if (musFollowScrollOn) {
              x -= CRTC.crtR10TxXPort;
              y -= CRTC.crtR11TxYPort;
            }
          }
        }
        if (XEiJ.PNL_ROTATION_ON) {
          XEiJ.rbtRobot.mouseMove (XEiJ.pnlGlobalX + (int) Math.round (XEiJ.pnlMatrixL00 * (double) x + XEiJ.pnlMatrixL01 * (double) y + XEiJ.pnlMatrixL02),
                                   XEiJ.pnlGlobalY + (int) Math.round (XEiJ.pnlMatrixL10 * (double) x + XEiJ.pnlMatrixL11 * (double) y + XEiJ.pnlMatrixL12));
        } else {
          XEiJ.rbtRobot.mouseMove (x * XEiJ.pnlZoomWidth / XEiJ.pnlScreenWidth + XEiJ.pnlScreenX1 + XEiJ.pnlGlobalX,
                                   y * XEiJ.pnlZoomHeight / XEiJ.pnlScreenHeight + XEiJ.pnlScreenY1 + XEiJ.pnlGlobalY);
        }
      } else {  //シームレス→エクスクルーシブ
        musHide ();
        Point point = XEiJ.pnlCanvasOrPanel.getLocationOnScreen ();
        XEiJ.pnlGlobalX = point.x;
        XEiJ.pnlGlobalY = point.y;
        musExclusiveStart = true;  //エクスクルーシブに切り替えた直後
      }
    }
    if (musSeamlessMouseCheckBox.isSelected () != on) {
      musSeamlessMouseCheckBox.setSelected (on);
    }
  }  //musSetSeamlessOn(boolean)

  //musHide ()
  //  マウスカーソルを消す
  public static void musHide () {
    if (musCursorNumber != 0 && musCursorAvailable) {
      musCursorNumber = 0;
      XEiJ.pnlPanel.setCursor (musCursorArray[0]);  //pnlCanvasOrPanelは不可
    }
  }  //musHide()

  //musShow ()
  //  マウスカーソルを表示する
  public static void musShow () {
    if (musCursorNumber != 1 && musCursorAvailable) {
      musCursorNumber = 1;
      XEiJ.pnlPanel.setCursor (musCursorArray[1]);  //pnlCanvasOrPanelは不可
    }
  }  //musShow()

  //musSetEdgeAccelerationOn (on)
  //  縁部加速
  public static void musSetEdgeAccelerationOn (boolean on) {
    musEdgeAccelerationOn = on;
  }  //musSetEdgeAccelerationOn(boolean)

  //musSetHostsPixelUnitsOn (on)
  //  true=エクスクルーシブのときマウスはホストの画素単位で動く,false=X68000の画素単位で動く
  public static void musSetHostsPixelUnitsOn (boolean on) {
    musHostsPixelUnitsOn = on;
    musUpdateSpeedRatio ();
  }  //musSetHostsPixelUnitsOn(boolean)

}  //class Mouse
