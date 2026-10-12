//========================================================================================
//  MemoryDumpList.java
//    en:Memory dump list
//    ja:メモリダンプリスト
//  Copyright (C) 2003-2025 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.awt.*;  //BasicStroke,BorderLayout,BoxLayout,Color,Component,Container,Cursor,Desktop,Dimension,Font,FlowLayout,Frame,Graphics,Graphics2D,GraphicsDevice,GraphicsEnvironment,GridLayout,Image,Insets,Paint,Point,Rectangle,RenderingHints,Robot,Shape,Stroke,TexturePaint,Toolkit
import java.awt.event.*;  //ActionEvent,ActionListener,ComponentAdapter,ComponentEvent,ComponentListener,FocusAdapter,FocusEvent,FocusListener,InputEvent,KeyAdapter,KeyEvent,KeyListener,MouseAdapter,MouseEvent,MouseListener,MouseMotionAdapter,MouseWheelEvent,WindowAdapter,WindowEvent,WindowListener,WindowStateListener
import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,TimeZone,Timer,TimerTask,TreeMap
import javax.swing.*;  //AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JApplet,JButton,JCheckBox,JCheckBoxMenuItem,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException
import javax.swing.event.*;  //CaretEvent,CaretListener,ChangeEvent,ChangeListener,DocumentEvent,DocumentListener,ListSelectionListener
import javax.swing.text.*;  //AbstractDocument,BadLocationException,DefaultCaret,Document,DocumentFilter,JTextComponent,ParagraphView,Style,StyleConstants,StyleContext,StyledDocument

public class MemoryDumpList {

  public static final int DMP_ITEM_SIZE = 0x00000010;  //項目の最小サイズ
  public static final int DMP_PAGE_SIZE = 0x00000400;  //ページのサイズ
  public static final int DMP_ITEM_MASK = -DMP_ITEM_SIZE;
  public static final int DMP_PAGE_MASK = -DMP_PAGE_SIZE;
  public static final int DMP_MAX_ITEMS = DMP_PAGE_SIZE / DMP_ITEM_SIZE + 2;  //ページの最大項目数。先頭と末尾の番兵を含む

  public static final char[] DMP_BASE = (
    //         11111111112222222222333333333344444444445555555555666666666677777777778
    //12345678901234567890123456789012345678901234567890123456789012345678901234567890
    "xxxxxxxx  xx xx xx xx  xx xx xx xx  xx xx xx xx  xx xx xx xx ").toCharArray ();
  public static final int DMP_DATA_START = 10;  //行頭からデータの開始位置までのオフセット
  public static final int[] DMP_DATA_ADDRESS = {
    0,   1, -1,  2,  3, -1,  4,  5, -1,  6,  7, -1, -1,
    8,   9, -1, 10, 11, -1, 12, 13, -1, 14, 15, -1, -1,
    16, 17, -1, 18, 19, -1, 20, 21, -1, 22, 23, -1, -1,
    24, 25, -1, 26, 27, -1, 28, 29, -1, 30, 31,
  };  //データの開始位置からのオフセット→データのアドレス*2+(0=上位,1=下位)。-1=空白
  public static final int[] DMP_DATA_OFFSET = {
    0,   1,  3,  4,  6,  7,  9, 10,
    13, 14, 16, 17, 19, 20, 22, 23,
    26, 27, 29, 30, 32, 33, 35, 36,
    39, 40, 42, 43, 45, 46, 48, 49,
  };  //データのアドレス*2+(0=上位,1=下位)→データの開始位置からのオフセット

  public static int dmpItemCount;  //ページに含まれる項目の数。先頭と末尾の番兵を含む。0=構築前または再構築要求
  public static int dmpItemIndex;  //キャレットがある項目の番号
  public static int dmpPageAddress;  //ページの先頭アドレス
  public static final int[] dmpAddressArray = new int[DMP_MAX_ITEMS];  //項目の先頭アドレスの配列。先頭は前のページの末尾、末尾は次のページの先頭。スピナーのヒント
  public static final int[] dmpSplitArray = new int[DMP_MAX_ITEMS];  //項目を区切る位置の配列。先頭は0
  public static final int[] dmpCaretArray = new int[DMP_MAX_ITEMS];  //項目が選択されたときキャレットを移動させる位置の配列。行の手前にヘッダやラベルなどを挿入しないときはdmpSplitArrayと同じ

  public static JFrame dmpFrame;  //ウインドウ
  public static ScrollTextArea dmpBoard;  //スクロールテキストエリア
  public static JTextArea dmpTextArea;  //テキストエリア
  public static Hex8Spinner dmpSpinner;  //スピナー

  public static int dmpFunctionCode;  //ファンクションコード
  public static DecimalSpinner dmpFCSpinner;  //ファンクションコードスピナー

  public static boolean dmpSecondBridge;  //true=行末からはみ出して書いた文字を行頭にも書く
  public static Color dmpCellophaneColor;  //dmpSecondBridgeではみ出した部分に被せるセロファンの色

  //dmpInit ()
  //  初期化
  public static void dmpInit () {

    dmpItemCount = 0;  //構築前
    dmpItemIndex = 0;
    dmpPageAddress = 0;

    dmpFunctionCode = 5;

    //dmpAddressArray = new int[DMP_MAX_ITEMS];
    //dmpSplitArray = new int[DMP_MAX_ITEMS];
    //dmpCaretArray = new int[DMP_MAX_ITEMS];

    dmpFrame = null;

  }  //dmpInit()

  //dmpStart ()
  public static void dmpStart () {
    if (RestorableFrame.rfmGetOpened (Settings.SGS_DMP_FRAME_KEY)) {
      dmpOpen (-1, -1, true);
    }
  }  //dmpStart()

  //dmpOpen (address, functionCode, forceUpdate)
  //  メモリダンプリストウインドウを開く
  //  既に開いているときは手前に持ってくる
  public static void dmpOpen (int address, int functionCode, boolean forceUpdate) {
    if (dmpFrame == null) {
      dmpMake ();
    }
    dmpUpdate (address, functionCode, forceUpdate);
    XEiJ.pnlExitFullScreen (false);
    dmpFrame.setVisible (true);
    XEiJ.dbgVisibleMask |= XEiJ.DBG_DMP_VISIBLE_MASK;
  }  //dmpOpen(int,int,boolean)

  static class MemoryDumpTextArea extends ScrollTextArea {
    @Override public void paintAfterText (JTextArea textArea, Graphics2D g2) {
      if (MemoryDumpList.dmpSecondBridge) {
        try {
          g2.setPaint (MemoryDumpList.dmpCellophaneColor);
          Rectangle r0 = textArea.modelToView2D (61).getBounds ();  //0の位置
          Rectangle r1 = textArea.modelToView2D (62).getBounds ();  //1の位置
          g2.fillRect (r0.x - (r1.x - r0.x), r0.y,
                       r1.x - r0.x, r0.height * DMP_MAX_ITEMS);  //左側
          g2.fillRect (r0.x + (r1.x - r0.x) * 16, r0.y,
                       r1.x - r0.x, r0.height * DMP_MAX_ITEMS);  //右側
        } catch (BadLocationException ble) {
        }
      }
    }
  }

  //dmpMake ()
  //  メモリダンプリストウインドウを作る
  public static void dmpMake () {

    //スクロールテキストエリア
    dmpBoard = ComponentFactory.setPreferredSize (
      ComponentFactory.setFont (new MemoryDumpTextArea (), LnF.lnfMonospacedFont),
      500, 400);
    dmpBoard.setMargin (new Insets (2, 4, 2, 4));
    dmpBoard.setHighlightCursorOn (true);
    dmpTextArea = dmpBoard.getTextArea ();
    dmpTextArea.setEditable (false);
    dmpTextArea.addMouseWheelListener ((mwe) -> {
      int n = mwe.getWheelRotation ();
      JViewport v = dmpBoard.getViewport ();
      Point p = v.getViewPosition ();
      v.setViewPosition (new Point (p.x,
                                    Math.max (0,
                                              Math.min (dmpTextArea.getSize ().height - v.getExtentSize ().height,
                                                        p.y + n * dmpTextArea.getFont ().getSize () * 5))));
    });

    //スピナー
    dmpSpinner = new Hex8Spinner (dmpPageAddress, DMP_ITEM_MASK, true);

    //スピナーのチェンジリスナー
    //  スピナーが操作されたとき、そのアドレスの行にテキストエリアのキャレットを移動させる
    //  ページの範囲外になったときはテキストエリアを再構築する
    //  ページの構築中に呼び出されたときは何もしない
    dmpSpinner.addChangeListener (new ChangeListener () {
      @Override public void stateChanged (ChangeEvent ce) {
        if (XEiJ.dbgEventMask == 0) {  //テキストは構築済みでsetTextの中ではない
          dmpUpdate (dmpSpinner.getIntValue (), dmpFunctionCode, false);
        }
      }
    });

    //テキストエリアのキャレットリスナー
    //  テキストエリアがクリックされてキャレットが動いたとき、その行のアドレスをスピナーに設定する
    //  クリックでテキストエリアに移ってしまったフォーカスをスピナーに戻す
    //  ページの構築中に呼び出されたときは何もしない
    //    setText→キャレットリスナー→スピナーのチェンジリスナー→setTextとなるとsetTextの二重呼び出しでエラーが出る
    ComponentFactory.addListener (
      dmpTextArea,
      new CaretListener () {
        @Override public void caretUpdate (CaretEvent ce) {
          if (XEiJ.dbgEventMask == 0) {  //テキストは構築済みでsetTextの中ではない
            int p = ce.getDot ();  //キャレットの位置
            if (p == ce.getMark ()) {  //選択範囲がない
              int i = Arrays.binarySearch (dmpSplitArray, 1, dmpItemCount, p + 1);  //項目の先頭のときも次の項目を検索してから1つ戻る
              i = (i >> 31 ^ i) - 1;  //キャレットがある位置を含む項目の番号
              dmpSpinner.setHintIndex (i);
            }
          }
        }
      });

    //テキストエリアのマウスリスナー
    ComponentFactory.addListener (
      dmpTextArea,
      new MouseAdapter () {
        @Override public void mousePressed (MouseEvent me) {
          if (XEiJ.mpuTask == null && me.isPopupTrigger ()) {
            XEiJ.dbgShowPopup (me, dmpTextArea, false);
          }
        }
        @Override public void mouseReleased (MouseEvent me) {
          if (XEiJ.mpuTask == null && me.isPopupTrigger ()) {
            XEiJ.dbgShowPopup (me, dmpTextArea, false);
          }
        }
      });

    //テキストエリアのフォーカスリスナー
    ComponentFactory.addListener (
      dmpBoard,
      new FocusAdapter () {
        @Override public void focusGained (FocusEvent fe) {
          dmpBoard.setCaretVisible (true);
        }
        @Override public void focusLost (FocusEvent fe) {
          dmpBoard.setCaretVisible (false);
        }
      });

    //テキストエリアのキーリスナー
    ComponentFactory.addListener (
      dmpBoard,
      new KeyAdapter () {
        @Override public void keyTyped (KeyEvent ke) {
          if (XEiJ.dbgEventMask == 0) {  //テキストは構築済みでsetTextの中ではない
            int x = Character.digit (ke.getKeyChar (), 16);
            if (x >= 0) {  //16進数のキーが押された
              int p = dmpTextArea.getCaretPosition ();  //キャレットの位置
              int i = Arrays.binarySearch (dmpSplitArray, 1, dmpItemCount, p + 1);
              i = (i >> 31 ^ i) - 1;  //キャレットがある位置を含む項目の番号
              int t = p - dmpCaretArray[i] - DMP_DATA_START;  //最初のデータの位置からのオフセット
              if (t >= 0 && t < DMP_DATA_ADDRESS.length) {
                t = DMP_DATA_ADDRESS[t];
                if (t >= 0) {  //データがある
                  int a = dmpAddressArray[i] + (t >> 1);  //アドレス
                  XEiJ.dbgEventMask++;  //キャレットが動くがキャレットリスナーが反応しないようにする
                  if ((t & 1) == 0) {  //上位
                    try {
                      MC68060.mmuPokeByte (a, x << 4 | Character.digit (dmpTextArea.getText (p + 1, 1).charAt (0), 16), dmpFunctionCode);  //書き込む
                    } catch (BadLocationException ble) {
                    }
                    dmpTextArea.replaceRange (XEiJ.fmtHex2 (MC68060.mmuPeekByteZero (a, dmpFunctionCode)), p, p + 2);  //読み出す
                    dmpTextArea.setCaretPosition (p + 1);  //下位の位置
                  } else {  //下位
                    try {
                      MC68060.mmuPokeByte (a, Character.digit (dmpTextArea.getText (p - 1, 1).charAt (0), 16) << 4 | x, dmpFunctionCode);  //書き込む
                    } catch (BadLocationException ble) {
                    }
                    dmpTextArea.replaceRange (XEiJ.fmtHex2 (MC68060.mmuPeekByteZero (a, dmpFunctionCode)), p - 1, p + 1);  //読み出す
                    if (t < 31) {
                      dmpTextArea.setCaretPosition (dmpCaretArray[i] + DMP_DATA_START + DMP_DATA_OFFSET[t + 1]);  //次のアドレスの上位の位置
                    }
                  }
                  XEiJ.dbgEventMask--;
                }
              }
            }
          }
        }
      });

    //ボタンのアクションリスナー
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        switch (ae.getActionCommand ()) {
        case "Reload":
          dmpItemCount = 0;  //再構築要求
          dmpUpdate (dmpAddressArray[dmpItemIndex], dmpFunctionCode, true);
          break;
        case "Second bridge":  //セカンドブリッジ
          dmpSecondBridge = ((JCheckBox) ae.getSource ()).isSelected ();
          dmpItemCount = 0;  //再構築要求
          dmpUpdate (dmpAddressArray[dmpItemIndex], dmpFunctionCode, true);
          break;
        }
      }
    };

    //再読み込みボタン
    JButton reloadButton =
      Multilingual.mlnToolTipText (
        ComponentFactory.createImageButton (LnF.LNF_RELOAD_IMAGE, "Reload", listener),
        "ja", "再読み込み");

    //ファンクションコードスピナー
    dmpFCSpinner = ComponentFactory.createDecimalSpinner (
      dmpFunctionCode,  //value
      0,  //minimum
      7,  //maximum
      1,  //stepSize
      0,  //option
      new ChangeListener () {
        @Override public void stateChanged (ChangeEvent ce) {
          if (XEiJ.dbgEventMask == 0) {  //テキストは構築済みでsetTextの中ではない
            dmpUpdate (dmpPageAddress, dmpFCSpinner.getIntValue (), false);
          }
        }
      });

    //セカンドブリッジチェックボックス
    dmpSecondBridge = false;
    dmpCellophaneColor = new Color ((LnF.lnfRGB[5] & 0x00ffffff) | 0xcc000000, true);
    JCheckBox secondBridgeCheckBox =
      Multilingual.mlnToolTipText (
        ComponentFactory.createIconCheckBox (
          dmpSecondBridge,
          XEiJ.createImage (
            20, 14,
            "22222222222222222222" +
            "2..................2" +
            "2...........11111..2" +
            "2...........11111..2" +
            "2....111111111111..2" +
            "2...........11111..2" +
            "2...........11111..2" +
            "2..................2" +
            "2..................2" +
            "2....1111111111....2" +
            "2..................2" +
            "2..................2" +
            "2..................2" +
            "22222222222222222222",
            LnF.lnfRGB[0],
            LnF.lnfRGB[12],
            LnF.lnfRGB[12]),
          XEiJ.createImage (
            20, 14,
            "22222222222222222222" +
            "2..................2" +
            "2...........11111..2" +
            "2...........11111..2" +
            "2....111111111111..2" +
            "2...........11111..2" +
            "2...........11111..2" +
            "2..11111...........2" +
            "2..11111...........2" +
            "2..111111111111....2" +
            "2..11111...........2" +
            "2..11111...........2" +
            "2..................2" +
            "22222222222222222222",
            LnF.lnfRGB[0],
            LnF.lnfRGB[12],
            LnF.lnfRGB[12]),
          "Second bridge", listener),
        "ja", "セカンドブリッジ");

    //ウインドウ
    dmpFrame = Multilingual.mlnTitle (
      ComponentFactory.createRestorableSubFrame (
        Settings.SGS_DMP_FRAME_KEY,
        "Memory dump list",
        null,
        ComponentFactory.createBorderPanel (
          dmpBoard,
          ComponentFactory.createHorizontalBox (

            dmpFCSpinner,

            dmpSpinner,
            reloadButton,
            secondBridgeCheckBox,
            Box.createHorizontalGlue ()
            )
          )
        ),
      "ja", "メモリダンプリスト");
    ComponentFactory.addListener (
      dmpFrame,
      new WindowAdapter () {
        @Override public void windowClosing (WindowEvent we) {
          XEiJ.dbgVisibleMask &= ~XEiJ.DBG_DMP_VISIBLE_MASK;
        }
      });

  }  //dmpMake()

  //dmpUpdate (address, functionCode, forceUpdate)
  //  メモリダンプリストウインドウを更新する
  public static void dmpUpdate (int address, int functionCode, boolean forceUpdate) {

    XEiJ.dbgEventMask++;  //構築開始

    if (address == -1) {  //spを表示
      address = XEiJ.regRn[15];
      forceUpdate = true;
    }

    if (functionCode == -1) {  //spのユーザ/スーパーバイザに従う
      functionCode = XEiJ.regSRS != 0 ? 5 : 1;
      forceUpdate = true;
    }

    if (dmpFunctionCode != functionCode) {  //ファンクションコードが一致しない
      dmpFunctionCode = functionCode;
      forceUpdate = true;
      dmpFCSpinner.setIntValue (functionCode);
    }

    if (forceUpdate) {  //再構築要求
      dmpItemCount = 0;
    }

    if (dmpItemCount != 0) {  //構築前または再構築要求のいずれでもない
      int i = Arrays.binarySearch (dmpAddressArray, 1, dmpItemCount, address + 1);  //項目の先頭のときも次の項目を検索してから1つ戻る
      i = (i >> 31 ^ i) - 1;  //目的のアドレスを含む項目の番号
      if (0 < i && i < dmpItemCount - 1 &&  //ページの内側
          dmpAddressArray[i] == address) {  //項目の先頭

        //再構築しない

        if (dmpItemIndex != i) {  //キャレットがある項目を変更する必要がある
          dmpItemIndex = i;
          dmpTextArea.setCaretPosition (dmpCaretArray[i]);
        }

        XEiJ.dbgEventMask--;  //構築終了

        return;
      }
    }

    //再構築する

    //構築前または再構築要求または先頭または末尾の番兵が選択された
    //  0x00000000の境界を跨ぐとき反対側を指すことがあるので先頭と末尾の番兵を区別しない
    address &= DMP_ITEM_MASK;  //目的のアドレスを含む項目の先頭アドレス
    dmpPageAddress = address & DMP_PAGE_MASK;  //ページの先頭アドレス
    int pageEndAddress = dmpPageAddress + DMP_PAGE_SIZE;  //ページの末尾アドレス。0になることがある

    //先頭の番兵
    dmpAddressArray[0] = dmpPageAddress - DMP_ITEM_SIZE;  //昇順を維持するためマスクしない
    dmpSplitArray[0] = 0;
    dmpCaretArray[0] = 0;
    StringBuilder sb = new StringBuilder (
      //         1111111111222222222233333333334444444444555555555566666666667777777777
      //1234567890123456789012345678901234567890123456789012345678901234567890123456789
      //xxxxxxx  xx xx xx xx  xx xx xx xx  xx xx xx xx  xx xx xx xx  ................
      "▲        +0 +1 +2 +3  +4 +5 +6 +7  +8 +9 +A +B  +C +D +E +F  0123456789ABCDEF\n");
    int itemCount = 1;  //項目数
    int itemAddress = dmpPageAddress;  //項目の先頭アドレス
    boolean bridge = false;  //1=先頭はSJISの2バイトコードの2バイト目
    for (int k = 1; k <= 200; k++) {  //最大100文字200バイトまで遡る。長い文章でもSJISの2バイトコードの1バイト目ばかりが続くわけではないので十分だろう
      int h = MC68060.mmuPeekByteZero (itemAddress - k, functionCode);
      if ((0x81 <= h && h <= 0x9f) ||
          (0xe0 <= h && h <= 0xef)) {  //SJISの2バイトコードの1バイト目
        bridge = !bridge;
      } else {
        break;
      }
    }

    do {
      int itemEndAddress = itemAddress + DMP_ITEM_SIZE;  //項目の末尾アドレス
      //項目の開始
      if (itemAddress == address) {
        dmpItemIndex = itemCount;  //目的のアドレスを含む項目の番号
      }
      dmpAddressArray[itemCount] = itemAddress;  //項目の先頭アドレス
      dmpSplitArray[itemCount] = sb.length ();  //項目を区切る位置
      dmpCaretArray[itemCount] = sb.length ();  //項目が選択されたときキャレットを移動させる位置
      //アドレス
      XEiJ.fmtHex8 (DMP_BASE,  0, itemAddress);
      //データ
      XEiJ.fmtHex2 (DMP_BASE, 10, MC68060.mmuPeekByteZero (itemAddress     , functionCode));
      XEiJ.fmtHex2 (DMP_BASE, 13, MC68060.mmuPeekByteZero (itemAddress +  1, functionCode));
      XEiJ.fmtHex2 (DMP_BASE, 16, MC68060.mmuPeekByteZero (itemAddress +  2, functionCode));
      XEiJ.fmtHex2 (DMP_BASE, 19, MC68060.mmuPeekByteZero (itemAddress +  3, functionCode));
      XEiJ.fmtHex2 (DMP_BASE, 23, MC68060.mmuPeekByteZero (itemAddress +  4, functionCode));
      XEiJ.fmtHex2 (DMP_BASE, 26, MC68060.mmuPeekByteZero (itemAddress +  5, functionCode));
      XEiJ.fmtHex2 (DMP_BASE, 29, MC68060.mmuPeekByteZero (itemAddress +  6, functionCode));
      XEiJ.fmtHex2 (DMP_BASE, 32, MC68060.mmuPeekByteZero (itemAddress +  7, functionCode));
      XEiJ.fmtHex2 (DMP_BASE, 36, MC68060.mmuPeekByteZero (itemAddress +  8, functionCode));
      XEiJ.fmtHex2 (DMP_BASE, 39, MC68060.mmuPeekByteZero (itemAddress +  9, functionCode));
      XEiJ.fmtHex2 (DMP_BASE, 42, MC68060.mmuPeekByteZero (itemAddress + 10, functionCode));
      XEiJ.fmtHex2 (DMP_BASE, 45, MC68060.mmuPeekByteZero (itemAddress + 11, functionCode));
      XEiJ.fmtHex2 (DMP_BASE, 49, MC68060.mmuPeekByteZero (itemAddress + 12, functionCode));
      XEiJ.fmtHex2 (DMP_BASE, 52, MC68060.mmuPeekByteZero (itemAddress + 13, functionCode));
      XEiJ.fmtHex2 (DMP_BASE, 55, MC68060.mmuPeekByteZero (itemAddress + 14, functionCode));
      XEiJ.fmtHex2 (DMP_BASE, 58, MC68060.mmuPeekByteZero (itemAddress + 15, functionCode));
      sb.append (DMP_BASE);
      //キャラクタ
      boolean nextBridge = false;
      int a;
      if (!bridge) {  //行末からはみ出して書いた文字はない
        sb.append (' ');
        a = itemAddress;
      } else if (itemAddress == dmpPageAddress ||  //1行目は常に
                 dmpSecondBridge) {  //2行目以降は設定に従い、行末からはみ出して書いた文字を行頭にも書く
        a = itemAddress - 1;
      } else {  //行末からはみ出して書いた文字を行頭には書かない
        sb.append ("  ");
        a = itemAddress + 1;
      }
      for (; a < itemEndAddress; a++) {
        int h = MC68060.mmuPeekByteZero (a, functionCode);
        int c;
        if ((0x81 <= h && h <= 0x9f) ||
            (0xe0 <= h && h <= 0xef)) {  //SJISの2バイトコードの1バイト目
          int l = MC68060.mmuPeekByteZero (a + 1, functionCode);  //これは範囲外になる場合がある
          if (0x40 <= l && l <= 0xfc && l != 0x7f) {  //SJISの2バイトコードの2バイト目
            c = CharacterCode.chrSJISToChar[h << 8 | l];  //2バイトで変換する
            if (c == 0) {  //対応する文字がない
              c = '※';
            } else if (c == 0x3000) {  //全角空白
              c = '\u2b1a';  //U+2B1A Dotted Square
            }
            a++;
            if (a == itemEndAddress) {  //行末からはみ出した
              nextBridge = true;
            }
          } else {  //SJISの2バイトコードの2バイト目ではない
            c = '.';  //SJISの2バイトコードの1バイト目ではなかった
          }
        } else {  //SJISの2バイトコードの1バイト目ではない
          c = CharacterCode.chrSJISToChar[h];  //1バイトで変換する
          if (c < 0x20 || c == 0x7f) {  //対応する文字がないまたは制御コード
            c = '.';
          }
        }
        sb.append ((char) c);
      }  //for a
      sb.append ('\n');
      //項目の終了
      itemCount++;
      itemAddress = itemEndAddress;
      bridge = nextBridge;
    } while (itemAddress < pageEndAddress);

    //末尾の番兵
    dmpAddressArray[itemCount] = itemAddress;  //昇順を維持するためマスクしない
    dmpSplitArray[itemCount] = sb.length ();
    dmpCaretArray[itemCount] = sb.length ();
    sb.append (
      //         1111111111222222222233333333334444444444555555555566666666667777777777
      //1234567890123456789012345678901234567890123456789012345678901234567890123456789
      "▼        +0 +1 +2 +3  +4 +5 +6 +7  +8 +9 +A +B  +C +D +E +F  0123456789ABCDEF");
    itemCount++;
    dmpItemCount = itemCount;

    //テキスト
    dmpTextArea.setText (sb.toString ());
    dmpTextArea.setCaretPosition (dmpCaretArray[dmpItemIndex]);

    //スピナー
    dmpSpinner.setHintArray (dmpAddressArray, itemCount);
    dmpSpinner.setHintIndex (dmpItemIndex);

    XEiJ.dbgEventMask--;  //構築終了

  }  //dmpUpdate(int,int,boolean)

}  //class MemoryDumpList



