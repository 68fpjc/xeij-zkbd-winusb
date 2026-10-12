//========================================================================================
//  ScreenModeTest.java
//    en:Screen mode test
//    ja:表示モードテスト
//  Copyright (C) 2003-2026 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.awt.event.*;  //ActionEvent,ActionListener,ComponentAdapter,ComponentEvent,ComponentListener,FocusAdapter,FocusEvent,FocusListener,InputEvent,KeyAdapter,KeyEvent,KeyListener,MouseAdapter,MouseEvent,MouseListener,MouseMotionAdapter,MouseWheelEvent,WindowAdapter,WindowEvent,WindowListener,WindowStateListener
import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,Timer,TimerTask,TreeMap
import javax.swing.*;  //AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JApplet,JButton,JCheckBox,JCheckBoxMenuItem,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException
import javax.swing.event.*;  //CaretListener,ChangeEvent,ChangeListener,DocumentEvent,DocumentListener,ListSelectionListener

public class ScreenModeTest {

  public static final boolean SMT_ON = true;  //true=表示モードテストを有効にする

  //ウインドウ
  public static final int SMT_MARGIN = 4;  //周囲のマージン
  public static final int SMT_ITEM_GAP = 4;  //アイテムの間隔
  public static final int SMT_ITEM_GAP_2 = 8;  //アイテムの間隔2
  public static final int SMT_ITEM_GAP_3 = 12;  //アイテムの間隔3
  public static JFrame smtFrame;

  //周波数
  public static JTextField smtFreqTextField;
  public static String smtFreqLastText;  //前回表示した周波数

  //モード
  public static JTextField smtModeTextField;
  public static final int SMT_MODE_LIMIT = 6;  //モードを表示する数の上限
  public static final int SMT_MODE_SPAN  = 5;  //モードを表示する時間(s)
  public static final String[] smtModeName = new String[SMT_MODE_LIMIT];  //モードの名前のリスト。名前昇順
  public static final long[] smtModeTime = new long[SMT_MODE_LIMIT];  //モードの時刻のリスト。名前昇順。時刻昇順ではない
  public static int smtModeCount;  //リストの現在の長さ

  //パターンテスト
  public static boolean smtPatternTestOn;  //true=パターンテスト中
  public static boolean smtPatternTestSpriteOn;  //true=パターンテストでスプライト番号を表示する

  //アイテム
  public static SMTItem[] smtItemArray;

  public static SMTGroup smtCRTItemGroup1;
  public static SMTItem smtItemCrtHFEd;  //$E80000 R00 7-0
  public static SMTItem smtItemCrtHSEd;  //$E80002 R01 7-0
  public static SMTItem smtItemCrtHBEd;  //$E80004 R02 7-0
  public static SMTItem smtItemCrtHDEd;  //$E80006 R03 7-0
  public static SMTItem smtItemCrtVFEd;  //$E80008 R04 9-0
  public static SMTItem smtItemCrtVSEd;  //$E8000A R05 9-0
  public static SMTItem smtItemCrtVBEd;  //$E8000C R06 9-0
  public static SMTItem smtItemCrtVDEd;  //$E8000E R07 9-0

  public static SMTGroup smtCRTItemGroup2;
  public static SMTItem smtItemCrtIRQ;  //$E80012 R09 9-0
  public static SMTItem smtItemCrtTxX;  //$E80014 R10 9-0
  public static SMTItem smtItemCrtTxY;  //$E80016 R11 9-0
  public static SMTItem smtItemCrtGrMM;  //$E80028 R20 10-8
  public static SMTItem smtItemCrtHigh;  //$E80028 R20 4
  public static SMTItem smtItemCrtVRes;  //$E80028 R20 3-2
  public static SMTItem smtItemCrtHRes;  //$E80028 R20 1-0
  public static SMTItem smtItemSysHRL;  //$E8E007 1
  public static SMTItem smtItemSysCont;  //$E8E001 3-0

  public static SMTGroup smtCRTItemGroup3;
  public static SMTItem smtItemCrtGr0X;  //$E80018 R12 9-0
  public static SMTItem smtItemCrtGr0Y;  //$E8001A R13 9-0
  public static SMTItem smtItemCrtGr1X;  //$E8001C R14 8-0
  public static SMTItem smtItemCrtGr1Y;  //$E8001E R15 8-0
  public static SMTItem smtItemCrtGr2X;  //$E80020 R16 8-0
  public static SMTItem smtItemCrtGr2Y;  //$E80022 R17 8-0
  public static SMTItem smtItemCrtGr3X;  //$E80024 R18 8-0
  public static SMTItem smtItemCrtGr3Y;  //$E80026 R19 8-0

  public static SMTGroup smtVcnItemGroup1;
  public static SMTItem smtItemVcnGrMM;  //$E82400 Reg1 2-0
  public static SMTItem smtItemVcnSpPr;  //$E82500 Reg2 13-12
  public static SMTItem smtItemVcnTxPr;  //$E82500 Reg2 11-10
  public static SMTItem smtItemVcnGrPr;  //$E82500 Reg2 9-8
  public static SMTItem smtItemVcnG4th;  //$E82500 Reg2 7-6
  public static SMTItem smtItemVcnG3rd;  //$E82500 Reg2 5-4
  public static SMTItem smtItemVcnG2nd;  //$E82500 Reg2 3-2
  public static SMTItem smtItemVcnG1st;  //$E82500 Reg2 1-0

  public static SMTGroup smtVcnItemGroup2;
  public static SMTItem smtItemVcnAHOn;  //$E82600 Reg3 14
  public static SMTItem smtItemVcnExOn;  //$E82600 Reg3 12
  public static SMTItem smtItemVcnHalf;  //$E82600 Reg3 11
  public static SMTItem smtItemVcnPLSB;  //$E82600 Reg3 10
  public static SMTItem smtItemVcnGrGr;  //$E82600 Reg3 9
  public static SMTItem smtItemVcnGrST;  //$E82600 Reg3 8
  public static SMTItem smtItemVcnSpOn;  //$E82600 Reg3 6
  public static SMTItem smtItemVcnTxOn;  //$E82600 Reg3 5
  public static SMTItem smtItemVcnGxOn;  //$E82600 Reg3 4
  public static SMTItem smtItemVcnG4On;  //$E82600 Reg3 3
  public static SMTItem smtItemVcnG3On;  //$E82600 Reg3 2
  public static SMTItem smtItemVcnG2On;  //$E82600 Reg3 1
  public static SMTItem smtItemVcnG1On;  //$E82600 Reg3 0

  public static SMTGroup smtSprItemGroup1;
  public static SMTItem smtItemSprBg0X;  //$EB0800 Reg0 9-0
  public static SMTItem smtItemSprBg0Y;  //$EB0802 Reg1 9-0
  public static SMTItem smtItemSprBg1X;  //$EB0804 Reg2 9-0
  public static SMTItem smtItemSprBg1Y;  //$EB0806 Reg3 9-0
  public static SMTItem smtItemSprDisp;  //$EB0808 Reg4 9
  public static SMTItem smtItemSprB1Tx;  //$EB0808 Reg4 5-4
  public static SMTItem smtItemSprB1On;  //$EB0808 Reg4 3
  public static SMTItem smtItemSprB0Tx;  //$EB0808 Reg4 2-1
  public static SMTItem smtItemSprB0On;  //$EB0808 Reg4 0

  public static SMTGroup smtSprItemGroup2;
  public static SMTItem smtItemSprHFEd;  //$EB080A Reg5 7-0
  public static SMTItem smtItemSprHBEd;  //$EB080C Reg6 5-0
  public static SMTItem smtItemSprVBEd;  //$EB080E Reg7 7-0
  public static SMTItem smtItemSprHigh;  //$EB0810 Reg8 4
  public static SMTItem smtItemSprVRes;  //$EB0810 Reg8 3-2
  public static SMTItem smtItemSprHRes;  //$EB0810 Reg8 1-0

  //タイマー
  public static final int SMT_INTERVAL = 10;
  public static int smtTimer;

  //smtInit ()
  //  初期化
  public static void smtInit () {

    //ウインドウ
    smtFrame = null;

    //周波数
    smtFreqLastText = "";

    //モード
    smtModeCount = 0;

    //パターンテスト
    smtPatternTestOn = false;
    smtPatternTestSpriteOn = false;

    //タイマー
    smtTimer = 0;

  }  //smtInit()

  //smtStart ()
  public static void smtStart () {
    if (RestorableFrame.rfmGetOpened (Settings.SGS_SMT_FRAME_KEY)) {
      smtOpen ();
    }
  }  //smtStart()

  //smtOpen ()
  //  表示モードテストを開く
  public static void smtOpen () {
    if (smtFrame == null) {
      smtMakeFrame ();
    } else {
      smtUpdateFrame ();
    }
    XEiJ.dbgVisibleMask |= XEiJ.DBG_SMT_VISIBLE_MASK;
    XEiJ.pnlExitFullScreen (false);
    smtFrame.setVisible (true);
  }  //smtOpen()

  //smtMakeFrame ()
  //  表示モードテストを作る
  //  ここでは開かない
  public static void smtMakeFrame () {

    //アイテム
    smtItemArray = new SMTItem[] {

      smtItemCrtHFEd = new SMTItem_CrtHFEd (),
      smtItemCrtHSEd = new SMTItem_CrtHSEd (),
      smtItemCrtHBEd = new SMTItem_CrtHBEd (),
      smtItemCrtHDEd = new SMTItem_CrtHDEd (),
      smtItemCrtVFEd = new SMTItem_CrtVFEd (),
      smtItemCrtVSEd = new SMTItem_CrtVSEd (),
      smtItemCrtVBEd = new SMTItem_CrtVBEd (),
      smtItemCrtVDEd = new SMTItem_CrtVDEd (),

      smtItemCrtIRQ = new SMTItem_CrtIRQ (),
      smtItemCrtTxX = new SMTItem_CrtTxX (),
      smtItemCrtTxY = new SMTItem_CrtTxY (),
      smtItemCrtGrMM = new SMTItem_CrtGrMM (),
      smtItemCrtHigh = new SMTItem_CrtHigh (),
      smtItemCrtVRes = new SMTItem_CrtVRes (),
      smtItemCrtHRes = new SMTItem_CrtHRes (),
      smtItemSysHRL = new SMTItem_SysHRL (),
      smtItemSysCont = new SMTItem_SysCont (),

      smtItemCrtGr0X = new SMTItem_CrtGr0X (),
      smtItemCrtGr0Y = new SMTItem_CrtGr0Y (),
      smtItemCrtGr1X = new SMTItem_CrtGr1X (),
      smtItemCrtGr1Y = new SMTItem_CrtGr1Y (),
      smtItemCrtGr2X = new SMTItem_CrtGr2X (),
      smtItemCrtGr2Y = new SMTItem_CrtGr2Y (),
      smtItemCrtGr3X = new SMTItem_CrtGr3X (),
      smtItemCrtGr3Y = new SMTItem_CrtGr3Y (),

      smtItemVcnGrMM = new SMTItem_VcnGrMM (),
      smtItemVcnSpPr = new SMTItem_VcnSpPr (),
      smtItemVcnTxPr = new SMTItem_VcnTxPr (),
      smtItemVcnGrPr = new SMTItem_VcnGrPr (),
      smtItemVcnG4th = new SMTItem_VcnG4th (),
      smtItemVcnG3rd = new SMTItem_VcnG3rd (),
      smtItemVcnG2nd = new SMTItem_VcnG2nd (),
      smtItemVcnG1st = new SMTItem_VcnG1st (),

      smtItemVcnAHOn = new SMTItem_VcnAHOn (),
      smtItemVcnExOn = new SMTItem_VcnExOn (),
      smtItemVcnHalf = new SMTItem_VcnHalf (),
      smtItemVcnPLSB = new SMTItem_VcnPLSB (),
      smtItemVcnGrGr = new SMTItem_VcnGrGr (),
      smtItemVcnGrST = new SMTItem_VcnGrST (),
      smtItemVcnSpOn = new SMTItem_VcnSpOn (),
      smtItemVcnTxOn = new SMTItem_VcnTxOn (),
      smtItemVcnGxOn = new SMTItem_VcnGxOn (),
      smtItemVcnG4On = new SMTItem_VcnG4On (),
      smtItemVcnG3On = new SMTItem_VcnG3On (),
      smtItemVcnG2On = new SMTItem_VcnG2On (),
      smtItemVcnG1On = new SMTItem_VcnG1On (),

      smtItemSprBg0X = new SMTItem_SprBg0X (),
      smtItemSprBg0Y = new SMTItem_SprBg0Y (),
      smtItemSprBg1X = new SMTItem_SprBg1X (),
      smtItemSprBg1Y = new SMTItem_SprBg1Y (),
      smtItemSprDisp = new SMTItem_SprDisp (),
      smtItemSprB1Tx = new SMTItem_SprB1Tx (),
      smtItemSprB1On = new SMTItem_SprB1On (),
      smtItemSprB0Tx = new SMTItem_SprB0Tx (),
      smtItemSprB0On = new SMTItem_SprB0On (),

      smtItemSprHFEd = new SMTItem_SprHFEd (),
      smtItemSprHBEd = new SMTItem_SprHBEd (),
      smtItemSprVBEd = new SMTItem_SprVBEd (),
      smtItemSprHigh = new SMTItem_SprHigh (),
      smtItemSprVRes = new SMTItem_SprVRes (),
      smtItemSprHRes = new SMTItem_SprHRes (),

    };

    //アイテムグループ
    smtCRTItemGroup1 = new SMTGroup (
      smtItemCrtHFEd,
      smtItemCrtHSEd,
      smtItemCrtHBEd,
      smtItemCrtHDEd,
      smtItemCrtVFEd,
      smtItemCrtVSEd,
      smtItemCrtVBEd,
      smtItemCrtVDEd
      );
    smtCRTItemGroup2 = new SMTGroup (
      smtItemCrtIRQ,
      smtItemCrtTxX,
      smtItemCrtTxY,
      smtItemCrtGrMM,
      smtItemCrtHigh,
      smtItemCrtVRes,
      smtItemCrtHRes,
      smtItemSysHRL,
      smtItemSysCont
      );
    smtCRTItemGroup3 = new SMTGroup (
      smtItemCrtGr0X,
      smtItemCrtGr0Y,
      smtItemCrtGr1X,
      smtItemCrtGr1Y,
      smtItemCrtGr2X,
      smtItemCrtGr2Y,
      smtItemCrtGr3X,
      smtItemCrtGr3Y
      );
    smtVcnItemGroup1 = new SMTGroup (
      smtItemVcnGrMM,
      smtItemVcnSpPr,
      smtItemVcnTxPr,
      smtItemVcnGrPr,
      smtItemVcnG4th,
      smtItemVcnG3rd,
      smtItemVcnG2nd,
      smtItemVcnG1st
      );
    smtVcnItemGroup2 = new SMTGroup (
      smtItemVcnAHOn,
      smtItemVcnExOn,
      smtItemVcnHalf,
      smtItemVcnPLSB,
      smtItemVcnGrGr,
      smtItemVcnGrST,
      smtItemVcnSpOn,
      smtItemVcnTxOn,
      smtItemVcnGxOn,
      smtItemVcnG4On,
      smtItemVcnG3On,
      smtItemVcnG2On,
      smtItemVcnG1On
      );
    smtSprItemGroup1 = new SMTGroup (
      smtItemSprBg0X,
      smtItemSprBg0Y,
      smtItemSprBg1X,
      smtItemSprBg1Y,
      smtItemSprDisp,
      smtItemSprB1Tx,
      smtItemSprB1On,
      smtItemSprB0Tx,
      smtItemSprB0On
      );
    smtSprItemGroup2 = new SMTGroup (
      smtItemSprHFEd,
      smtItemSprHBEd,
      smtItemSprVBEd,
      smtItemSprHigh,
      smtItemSprVRes,
      smtItemSprHRes
      );

    //ウインドウ
    smtFrame = Multilingual.mlnTitle (
      ComponentFactory.createRestorableSubFrame (
        Settings.SGS_SMT_FRAME_KEY,
        "Screen Mode Test",
        null,
        ComponentFactory.createVerticalBox (
          Box.createVerticalStrut (SMT_MARGIN),

          ComponentFactory.createHorizontalBox (
            Box.createHorizontalStrut (SMT_MARGIN),
            Multilingual.mlnTitledBorder (
              ComponentFactory.setTitledLineBorder (
                ComponentFactory.createVerticalBox (
                  ComponentFactory.createHorizontalBox (
                    smtFreqTextField = ComponentFactory.setEditable (
                      ComponentFactory.setHorizontalAlignment (
                        ComponentFactory.createTextField ("", 30),
                        JTextField.CENTER),
                      false)
                    ),
                  ComponentFactory.createHorizontalBox (
                    smtCRTItemGroup1.smgBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP_2),
                    smtItemCrtHFEd.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemCrtHSEd.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemCrtHBEd.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemCrtHDEd.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemCrtVFEd.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemCrtVSEd.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemCrtVBEd.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemCrtVDEd.smiBox,
                    Box.createHorizontalGlue ()
                    ),
                  ComponentFactory.createHorizontalBox (
                    smtCRTItemGroup2.smgBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP_2),
                    smtItemCrtIRQ.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemCrtTxX.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemCrtTxY.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP_3),
                    smtItemCrtGrMM.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemCrtHigh.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemCrtVRes.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemCrtHRes.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemSysHRL.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP_3),
                    smtItemSysCont.smiBox,
                    Box.createHorizontalGlue ()
                    ),
                  ComponentFactory.createHorizontalBox (
                    smtCRTItemGroup3.smgBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP_2),
                    smtItemCrtGr0X.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemCrtGr0Y.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemCrtGr1X.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemCrtGr1Y.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemCrtGr2X.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemCrtGr2Y.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemCrtGr3X.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemCrtGr3Y.smiBox,
                    Box.createHorizontalGlue ()
                    )
                  ),
                "CRTC / System port"),  //setTitledLineBorder
              "ja", "CRTC / システムポート"),  //Multilingual.mlnTitledBorder
            Box.createHorizontalStrut (SMT_MARGIN)
            ),

          ComponentFactory.createHorizontalBox (
            Box.createHorizontalStrut (SMT_MARGIN),
            Multilingual.mlnTitledBorder (
              ComponentFactory.setTitledLineBorder (
                ComponentFactory.createVerticalBox (
                  ComponentFactory.createHorizontalBox (
                    smtModeTextField = ComponentFactory.setEditable (
                      ComponentFactory.setHorizontalAlignment (
                        ComponentFactory.createTextField ("", 30),
                        JTextField.CENTER),
                      false)
                    ),
                  ComponentFactory.createHorizontalBox (
                    smtVcnItemGroup1.smgBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP_2),
                    smtItemVcnGrMM.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP_3),
                    smtItemVcnSpPr.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemVcnTxPr.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemVcnGrPr.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemVcnG4th.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemVcnG3rd.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemVcnG2nd.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemVcnG1st.smiBox,
                    Box.createHorizontalGlue ()
                    ),
                  ComponentFactory.createHorizontalBox (
                    smtVcnItemGroup2.smgBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP_2),
                    smtItemVcnAHOn.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemVcnExOn.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemVcnHalf.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemVcnPLSB.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemVcnGrGr.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemVcnGrST.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemVcnSpOn.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemVcnTxOn.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemVcnGxOn.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemVcnG4On.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemVcnG3On.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemVcnG2On.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemVcnG1On.smiBox,
                    Box.createHorizontalGlue ()
                    )
                  ),
                "Video Controller"),  //setTitledLineBorder
              "ja", "ビデオコントローラ"),  //Multilingual.mlnTitledBorder
            Box.createHorizontalStrut (SMT_MARGIN)
            ),

          ComponentFactory.createHorizontalBox (
            Box.createHorizontalStrut (SMT_MARGIN),
            Multilingual.mlnTitledBorder (
              ComponentFactory.setTitledLineBorder (
                ComponentFactory.createVerticalBox (
                  ComponentFactory.createHorizontalBox (
                    smtSprItemGroup1.smgBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP_2),
                    smtItemSprBg0X.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemSprBg0Y.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemSprBg1X.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemSprBg1Y.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP_3),
                    smtItemSprDisp.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemSprB1Tx.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemSprB1On.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemSprB0Tx.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemSprB0On.smiBox,
                    Box.createHorizontalGlue ()
                    ),
                  ComponentFactory.createHorizontalBox (
                    smtSprItemGroup2.smgBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP_2),
                    smtItemSprHFEd.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemSprHBEd.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemSprVBEd.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP_3),
                    smtItemSprHigh.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemSprVRes.smiBox,
                    Box.createHorizontalStrut (SMT_ITEM_GAP),
                    smtItemSprHRes.smiBox,
                    Box.createHorizontalGlue (),
                    !SpriteScreen.SPR_PATTEST_ON ? null :
                    ComponentFactory.createVerticalBox (
                      Box.createVerticalGlue (),
                      ComponentFactory.createHorizontalBox (
                        Multilingual.mlnText (
                          ComponentFactory.createCheckBox (
                            smtPatternTestOn,
                            "Pattern Test",
                            new ActionListener () {
                              @Override public void actionPerformed (ActionEvent ae) {
                                smtSetPatternTestOn (((JCheckBox) ae.getSource ()).isSelected ());
                              }
                            }),
                          "ja", "パターンテスト")
                        ),
                      ComponentFactory.createHorizontalBox (
                        Multilingual.mlnText (
                          ComponentFactory.createCheckBox (
                            smtPatternTestSpriteOn,
                            "Sprite Number",
                            new ActionListener () {
                              @Override public void actionPerformed (ActionEvent ae) {
                                smtSetPatternTestSpriteOn (((JCheckBox) ae.getSource ()).isSelected ());
                              }
                            }),
                          "ja", "スプライト番号")
                        ),
                      Box.createVerticalGlue ()
                      )
                    )
                  ),
                "Sprite Controller"),  //setTitledLineBorder
              "ja", "スプライトコントローラ"),  //Multilingual.mlnTitledBorder
            Box.createHorizontalStrut (SMT_MARGIN)
            ),

          Box.createVerticalStrut (SMT_MARGIN)
          )
        ),
      "ja", "表示モードテスト");  //Multilingual.mlnTitle

    //  ウインドウリスナー
    ComponentFactory.addListener (
      smtFrame,
      new WindowAdapter () {
        @Override public void windowClosing (WindowEvent we) {
          XEiJ.dbgVisibleMask &= ~XEiJ.DBG_SMT_VISIBLE_MASK;
        }
      });

  }  //smtMakeFrame()

  //smtSetPatternTestOn (on)
  //  パターンテストの設定
  public static void smtSetPatternTestOn (boolean on) {
    if (smtPatternTestOn != on) {
      smtPatternTestOn = on;
      if (on) {
        SpriteScreen.sprPatCurr = SpriteScreen.sprPatTest;
        SpriteScreen.sprColCurr = SpriteScreen.sprColTest;
        SpriteScreen.sprTColCurr = SpriteScreen.sprTColTest;
        VideoController.vcnPal16TS = VideoController.vcnPal16TSTest;
      } else {
        SpriteScreen.sprPatCurr = SpriteScreen.sprPatPort;
        SpriteScreen.sprColCurr = SpriteScreen.sprColPort;
        SpriteScreen.sprTColCurr = SpriteScreen.sprTColPort;
        if (FlashingLights.FLR_ON &&
            FlashingLights.flrPalette) {
          VideoController.vcnPal16TS = FlashingLights.flrGhostTS;
        } else {
          VideoController.vcnPal16TS = VideoController.vcnPal16TSPort;
        }
      }
      for (int i = 0; i < 256; i++) {
        VideoController.vcnPal32TS[i] = VideoController.vcnPalTbl[VideoController.vcnPal16TS[i]];
      }
      CRTC.crtAllStamp += 2;
      if (XEiJ.mpuTask == null) {  //停止中
        CRTC.crtRepaint ();
      }
    }
  }  //smtSetPatternTestOn

  //smtSetPatternTestSpriteOn (on)
  //  パターンテストでスプライト番号を表示するか
  public static void smtSetPatternTestSpriteOn (boolean on) {
    if (smtPatternTestSpriteOn != on) {
      smtPatternTestSpriteOn = on;
      if (on) {
        SpriteScreen.sprNumCurr = SpriteScreen.sprNumTest;
      } else {
        SpriteScreen.sprNumCurr = SpriteScreen.sprNumPort;
      }
      CRTC.crtAllStamp += 2;
      if (XEiJ.mpuTask == null) {  //停止中
        CRTC.crtRepaint ();
      }
    }
  }  //smtSetPatternTestSpriteOn

  //smtUpdateFrame ()
  //  表示モードテストを更新する
  public static void smtUpdateFrame () {

    if (smtFrame == null) {
      return;
    }

    //周波数
    smtUpdateFreq ();

    //モード
    smtUpdateMode ();

    //アイテム
    smtItemCrtHFEd.smiSetPortValue (CRTC.crtR00HFrontEndPort);
    smtItemCrtHSEd.smiSetPortValue (CRTC.crtR01HSyncEndPort);
    smtItemCrtHBEd.smiSetPortValue (CRTC.crtR02HBackEndPort);
    smtItemCrtHDEd.smiSetPortValue (CRTC.crtR03HDispEndPort);
    smtItemCrtVFEd.smiSetPortValue (CRTC.crtR04VFrontEndPort);
    smtItemCrtVSEd.smiSetPortValue (CRTC.crtR05VSyncEndPort);
    smtItemCrtVBEd.smiSetPortValue (CRTC.crtR06VBackEndPort);
    smtItemCrtVDEd.smiSetPortValue (CRTC.crtR07VDispEndPort);

    smtItemCrtIRQ.smiSetPortValue (CRTC.crtR09IRQRasterPort);
    smtItemCrtTxX.smiSetPortValue (CRTC.crtR10TxXPort);
    smtItemCrtTxY.smiSetPortValue (CRTC.crtR11TxYPort);
    smtItemCrtGrMM.smiSetPortValue (CRTC.crtMemoryModePort);
    smtItemCrtHigh.smiSetPortValue (CRTC.crtHighResoPort);
    smtItemCrtVRes.smiSetPortValue (CRTC.crtVResoPort);
    smtItemCrtHRes.smiSetPortValue (CRTC.crtHResoPort);
    smtItemSysHRL.smiSetPortValue (CRTC.crtHRLPort);
    smtItemSysCont.smiSetPortValue (VideoController.vcnTargetContrastPort);

    smtItemCrtGr0X.smiSetPortValue (CRTC.crtR12GrXPort[0]);
    smtItemCrtGr0Y.smiSetPortValue (CRTC.crtR13GrYPort[0]);
    smtItemCrtGr1X.smiSetPortValue (CRTC.crtR12GrXPort[1]);
    smtItemCrtGr1Y.smiSetPortValue (CRTC.crtR13GrYPort[1]);
    smtItemCrtGr2X.smiSetPortValue (CRTC.crtR12GrXPort[2]);
    smtItemCrtGr2Y.smiSetPortValue (CRTC.crtR13GrYPort[2]);
    smtItemCrtGr3X.smiSetPortValue (CRTC.crtR12GrXPort[3]);
    smtItemCrtGr3Y.smiSetPortValue (CRTC.crtR13GrYPort[3]);

    smtItemVcnGrMM.smiSetPortValue (VideoController.vcnReg1Port      );
    smtItemVcnSpPr.smiSetPortValue (VideoController.vcnReg2Port >> 12);
    smtItemVcnTxPr.smiSetPortValue (VideoController.vcnReg2Port >> 10);
    smtItemVcnGrPr.smiSetPortValue (VideoController.vcnReg2Port >>  8);
    smtItemVcnG4th.smiSetPortValue (VideoController.vcnReg2Port >>  6);
    smtItemVcnG3rd.smiSetPortValue (VideoController.vcnReg2Port >>  4);
    smtItemVcnG2nd.smiSetPortValue (VideoController.vcnReg2Port >>  2);
    smtItemVcnG1st.smiSetPortValue (VideoController.vcnReg2Port      );

    smtItemVcnAHOn.smiSetPortValue (VideoController.vcnReg3Port >> 14);
    smtItemVcnExOn.smiSetPortValue (VideoController.vcnReg3Port >> 12);
    smtItemVcnHalf.smiSetPortValue (VideoController.vcnReg3Port >> 11);
    smtItemVcnPLSB.smiSetPortValue (VideoController.vcnReg3Port >> 10);
    smtItemVcnGrGr.smiSetPortValue (VideoController.vcnReg3Port >>  9);
    smtItemVcnGrST.smiSetPortValue (VideoController.vcnReg3Port >>  8);
    smtItemVcnSpOn.smiSetPortValue (VideoController.vcnReg3Port >>  6);
    smtItemVcnTxOn.smiSetPortValue (VideoController.vcnReg3Port >>  5);
    smtItemVcnGxOn.smiSetPortValue (VideoController.vcnReg3Port >>  4);
    smtItemVcnG4On.smiSetPortValue (VideoController.vcnReg3Port >>  3);
    smtItemVcnG3On.smiSetPortValue (VideoController.vcnReg3Port >>  2);
    smtItemVcnG2On.smiSetPortValue (VideoController.vcnReg3Port >>  1);
    smtItemVcnG1On.smiSetPortValue (VideoController.vcnReg3Port      );

    smtItemSprBg0X.smiSetPortValue (SpriteScreen.sprReg0Bg0XPort);
    smtItemSprBg0Y.smiSetPortValue (SpriteScreen.sprReg1Bg0YPort);
    smtItemSprBg1X.smiSetPortValue (SpriteScreen.sprReg2Bg1XPort);
    smtItemSprBg1Y.smiSetPortValue (SpriteScreen.sprReg3Bg1YPort);
    smtItemSprDisp.smiSetPortValue (SpriteScreen.sprReg4BgCtrlPort >> 9);
    smtItemSprB1Tx.smiSetPortValue (SpriteScreen.sprReg4BgCtrlPort >> 4);
    smtItemSprB1On.smiSetPortValue (SpriteScreen.sprReg4BgCtrlPort >> 3);
    smtItemSprB0Tx.smiSetPortValue (SpriteScreen.sprReg4BgCtrlPort >> 1);
    smtItemSprB0On.smiSetPortValue (SpriteScreen.sprReg4BgCtrlPort     );

    smtItemSprHFEd.smiSetPortValue (SpriteScreen.sprReg5HFrontEndPort);
    smtItemSprHBEd.smiSetPortValue (SpriteScreen.sprReg6HBackEndPort);
    smtItemSprVBEd.smiSetPortValue (SpriteScreen.sprReg7VBackEndPort);
    smtItemSprHigh.smiSetPortValue (SpriteScreen.sprReg8ResoPort >> 4);
    smtItemSprVRes.smiSetPortValue (SpriteScreen.sprReg8ResoPort >> 2);
    smtItemSprHRes.smiSetPortValue (SpriteScreen.sprReg8ResoPort     );

    for (SMTItem item : smtItemArray) {
      if (item.smiPending) {
        item.smiPending = false;
        item.smiControlled ();
      }
    }

  }  //smtUpdateFrame()

  //smtUpdateFreq ()
  //  周波数の表示を更新する
  public static void smtUpdateFreq () {
    int htotal = CRTC.crtR00HFrontEndCurr + 1;
    int vtotal = CRTC.crtR04VFrontEndCurr + 1;
    if (0 < htotal && 0 < vtotal) {  //0除算を回避する
      int k = CRTC.crtHRLCurr << 3 | CRTC.crtHighResoCurr << 2 | CRTC.crtHResoCurr;
      double osc = (double) CRTC.crtFreqs[CRTC.CRT_OSCS[k]] * CRTC.crtVsyncMultiplier;
      int ratio = CRTC.CRT_DIVS[k];
      double hfreq = osc / (ratio * htotal << 3);
      double vfreq = hfreq / vtotal;
      int width = CRTC.crtR03HDispEndCurr - CRTC.crtR02HBackEndCurr << 3;
      int height = CRTC.crtR07VDispEndCurr - CRTC.crtR06VBackEndCurr;
      String option = "(normal)";
      if (CRTC.crtDuplication) {  //ラスタ2度読み
        height >>= 1;
        option = "(double-scanning)";
      } else if (CRTC.crtInterlace) {  //インターレース
        height <<= 1;
        option = "(interlace)";
      } else if (CRTC.crtSlit) {  //スリット
        option = "(slit)";
      } else if (CRTC.crtDupExceptSp) {  //ラスタ2度読み(スプライトを除く)
        height >>= 1;
        option = "(double-except-sp)";
      }
      String freqText = String.format ("%dx%d%s, HSYNC:%.3fMHz/%d/8/%d=%.3fkHz, VSYNC:%.3fkHz/%d=%.3fHz",
                                       width, height,
                                       option,
                                       osc * 1e-6, ratio, htotal, hfreq * 1e-3,
                                       hfreq * 1e-3, vtotal, vfreq);
      if (!freqText.equals (smtFreqLastText)) {  //前回と同じときは更新しない。内容が変わっていないのに更新し続けるとコピーできない
        smtFreqLastText = freqText;
        smtFreqTextField.setText (freqText);
      }
    }
  }  //smtUpdateFreq()

  //smtUpdateMode ()
  //  画面モードの表示を更新する
  //  画面モードは頻繁に更新される場合があるが、表示が頻繁に更新されるとコピーできないので、最近使われた複数の画面モードを一定の順序で表示する
  public static void smtUpdateMode () {
    String name1 = VideoController.vcnMode.name ();  //分類
    String name2 = VideoController.vcnGetName (new StringBuilder ()).toString ();  //詳細
    String name = name1.equals (name2) ? name1 : new StringBuilder (name1).append ('(').append (name2).append (')').toString ();  //モードの名前
    long time = XEiJ.mpuClockTime;  //現在の時刻
    boolean update = false;  //true=更新する
    //リストを走査する
    //  今回のモードのとき時刻を更新する
    //  時刻がSMT_MODE_SPAN秒前よりも古いレコードを捨てる
    boolean f = false;  //true=リストに今回のモードがある
    {
      long t = time - XEiJ.TMR_FREQ * SMT_MODE_SPAN;  //SMT_MODE_SPAN秒前の時刻
      int k = 0;  //残ったレコードの数。時刻がSMT_MODE_SPAN秒前よりも新しいレコードの数
      for (int i = 0; i < smtModeCount; i++) {
        if (name.compareTo (smtModeName[i]) == 0) {  //今回のモードのとき
          f = true;  //リストに今回のモードがある
          //レコードを残して時刻を更新する。k<iのとき詰める
          smtModeName[k] = name;
          smtModeTime[k] = time;
          k++;
        } else if (t < smtModeTime[i]) {  //今回のモードではなくて時刻がSMT_MODE_SPAN秒前よりも新しいとき
          //レコードを残す。k<iのとき詰める
          smtModeName[k] = smtModeName[i];
          smtModeTime[k] = smtModeTime[i];
          k++;
        }
      }
      if (k < smtModeCount) {  //時刻がSMT_MODE_SPAN秒以上前のレコードを捨てて詰めたのでリストが短くなった
        smtModeCount = k;
        update = true;
      }
    }
    if (!f) {  //リストに今回のモードがないとき
      //リストが一杯のとき最も古いレコードを捨てる
      if (smtModeCount == SMT_MODE_LIMIT) {
        //最も古いレコードを探す
        int o = -1;  //最も古いレコードのインデックス
        long t = Long.MAX_VALUE;  //最も古いレコードの時刻
        for (int i = 0; i < smtModeCount; i++) {
          if (smtModeTime[i] < t) {
            t = smtModeTime[i];
            o = i;
          }
        }
        //リストを詰める
        for (int i = o + 1; i < SMT_MODE_LIMIT; i++) {
          smtModeName[i - 1] = smtModeName[i];
          smtModeTime[i - 1] = smtModeTime[i];
        }
        smtModeCount--;
      }
      //リストに今回のモードを追加する
      {
        int p = -1;  //今回のモードを挿入する位置。今回のモードよりも名前が大きいレコードの最小のインデックス。-1=今回のモードよりも名前が大きいレコードがない
        for (int i = 0; i < smtModeCount; i++) {
          if (name.compareTo (smtModeName[i]) < 0) {
            p = i;  //今回のモードよりも名前が大きいレコードの最小のインデックス
            break;
          }
        }
        if (p < 0) {  //今回のモードよりも名前が大きいレコードがないとき
          p = smtModeCount;  //末尾に追加する
        }
        for (int i = smtModeCount - 1; p <= i; i--) {
          smtModeName[i + 1] = smtModeName[i];
          smtModeTime[i + 1] = smtModeTime[i];
        }
        smtModeName[p] = name;
        smtModeTime[p] = time;
        smtModeCount++;
      }
      update = true;
    }
    //リストが変化したとき表示する
    if (update) {
      StringBuilder sb = new StringBuilder ();
      for (int i = 0; i < smtModeCount; i++) {
        if (0 < i) {
          sb.append (", ");
        }
        sb.append (smtModeName[i]);
      }
      smtModeTextField.setText (sb.toString ());
    }
  }  //smtUpdateMode()



  //========================================================================================
  //$$SMI 表示モードテストのアイテム
  public static class SMTItem {

    public int smiBitWidth;  //bit幅
    public int smiBitMask;  //bitマスク兼最大値。(1<<smiBitWidth)-1
    public int smiDigits;  //最大値の10進桁数。(smiBitWidth*77>>8)+1

    public int smiPortValue;  //現在Portに表示されている値
    public int smiMaskValue;  //現在Maskで選択されている値。0=Port,-1=Test
    public int smiTestValue;  //現在Testに表示されている値

    public boolean smiPending;  //true=動作中に操作されたのでsmtUpdateFrame()でMaskとTestを取り込む

    public ButtonGroup smiButtonGroup;  //PortとTestを選択するラジオボタンのグループ

    public Box smiBox;  //ボックス
    public JLabel smiNameLabel;  //名前のラベル

    public JRadioButton smiPortRadioButton;  //Portを選択するラジオボタン
    public JCheckBox smiPortCheckBox;  //1bitのときPortで使うチェックボックス
    public JTextField smiPortTextField;  //2bit以上あるときPortで使うテキストフィールド

    public JRadioButton smiTestRadioButton;  //Testを選択するラジオボタン
    public JCheckBox smiTestCheckBox;  //1bitのときTestで使うチェックボックス
    public DecimalSpinner smiTestSpinner;  //2bit以上あるときTestで使うスピナー

    public SMTGroup smiItemGroup;

    //  コンストラクタ
    public SMTItem (String name, int portValue, int bitWidth) {

      smiBitWidth = bitWidth;
      smiBitMask = (1 << bitWidth) - 1;
      smiDigits = (bitWidth * 77 >> 8) + 1;

      portValue &= smiBitMask;
      smiPortValue = portValue;
      smiMaskValue = 0;
      smiTestValue = portValue;

      smiPending = false;

      smiButtonGroup = new ButtonGroup ();

      smiBox = ComponentFactory.setFixedSize (
        ComponentFactory.createVerticalBox (
          Box.createVerticalGlue (),
          ComponentFactory.createHorizontalBox (
            Box.createHorizontalGlue (),
            smiNameLabel = ComponentFactory.setFixedSize (
              ComponentFactory.createLabel (name),
              bitWidth == 1 ? LnF.lnfFontSize * 3 : 14 + 24 + (LnF.lnfFontSize * 2 / 3) * Math.max (2, smiDigits), LnF.lnfFontSize + 4),
            Box.createHorizontalGlue ()
            ),
          ComponentFactory.createHorizontalBox (
            Box.createHorizontalGlue (),
            smiPortRadioButton = ComponentFactory.setFixedSize (ComponentFactory.createRadioButton (smiButtonGroup, smiMaskValue == 0, "", new ActionListener () {
              @Override public void actionPerformed (ActionEvent ae) {
                smiPortSelected ();
              }
            }), 14, LnF.lnfFontSize + 4),  //ae -> this.smiPortSelected ()
            bitWidth == 1 ?
            (smiPortCheckBox = ComponentFactory.setEnabled (
              ComponentFactory.setFixedSize (
                ComponentFactory.createCheckBox (smiPortValue != 0, "", null),
                14, LnF.lnfFontSize + 4),
              false)) :  //操作不可
            (smiPortTextField = ComponentFactory.setFixedSize (
              ComponentFactory.setEditable (
                ComponentFactory.setHorizontalAlignment (
                  new JTextField (String.valueOf (smiPortValue)), //columnを指定すると幅を調節できなくなる
                  JTextField.CENTER),  //中央寄せ
                false),  //編集不可
              24 + (LnF.lnfFontSize * 2 / 3) * smiDigits, LnF.lnfFontSize + 4)),
            Box.createHorizontalGlue ()
            ),
          ComponentFactory.createHorizontalBox (
            Box.createHorizontalGlue (),
            smiTestRadioButton = ComponentFactory.setFixedSize (ComponentFactory.createRadioButton (smiButtonGroup, smiMaskValue != 0, "", new ActionListener () {
              @Override public void actionPerformed (ActionEvent ae) {
                smiTestSelected ();
              }
            }), 14, LnF.lnfFontSize + 4),  //ae -> this.smiTestSelected ()
            bitWidth == 1 ?
            (smiTestCheckBox = ComponentFactory.setFixedSize (ComponentFactory.createCheckBox (smiTestValue != 0, "", new ActionListener () {
              @Override public void actionPerformed (ActionEvent ae) {
                smiTestChanged ();
              }
            }), 14, LnF.lnfFontSize + 4)) :  //ae -> this.smiTestChanged ()
            (smiTestSpinner = ComponentFactory.createDecimalSpinner (smiTestValue, 0, smiBitMask, 1, 0, new ChangeListener () {
              @Override public void stateChanged (ChangeEvent ce) {
                smiTestChanged ();
              }
            })),  //ce -> this.smiTestChanged ()
            Box.createHorizontalGlue ()
            ),
          Box.createVerticalGlue ()
          ),
        bitWidth == 1 ? LnF.lnfFontSize * 3 : 14 + 24 + (LnF.lnfFontSize * 2 / 3) * Math.max (2, smiDigits), LnF.lnfFontSize * 3 + 12);

      smiItemGroup = null;

    }  //new SMTItem(int,int,int)

    //smiSetPortValue (portValue)
    //  Portの値を表示する
    //  smtUpdateFrame()が呼び出す
    public void smiSetPortValue (int portValue) {
      portValue &= smiBitMask;
      if (smiPortValue != portValue) {
        smiPortValue = portValue;
        if (smiBitWidth == 1) {
          smiPortCheckBox.setSelected (portValue != 0);
        } else {
          smiPortTextField.setText (String.valueOf (portValue));
        }
      }
    }  //smiSetPortValue(int)

    //smiPortSelected ()
    //  Portが選択された
    public void smiPortSelected () {
      if (smiMaskValue != 0) {
        smiMaskValue = 0;
        if (XEiJ.mpuTask == null) {  //停止中
          //停止中のときはすぐにsmiControlled()を呼び出す
          smiControlled ();
        } else {  //動作中
          //動作中のときは次回のsmtUpdateFrame()でsmiControlled()を呼び出す
          smiPending = true;
        }
        if (smiItemGroup != null) {
          smiItemGroup.smgUpdate ();
        }
      }
    }  //smiPortSelected()

    //smiTestSelected ()
    //  Testが選択された
    public void smiTestSelected () {
      if (smiMaskValue == 0) {
        smiMaskValue = -1;
        if (XEiJ.mpuTask == null) {  //停止中
          //停止中のときはすぐにsmiControlled()を呼び出す
          smiControlled ();
        } else {  //動作中
          //動作中のときは次回のsmtUpdateFrame()でsmiControlled()を呼び出す
          smiPending = true;
        }
        if (smiItemGroup != null) {
          smiItemGroup.smgUpdate ();
        }
      }
    }  //smiTestSelected()

    //smiTestChanged ()
    //  Testの値が変更された
    public void smiTestChanged () {
      int testValue = (smiBitWidth == 1 ? smiTestCheckBox.isSelected () ? 1 : 0 :
                       smiTestSpinner.getIntValue ());
      if (smiTestValue != testValue) {
        smiTestValue = testValue;
        if (XEiJ.mpuTask == null) {  //停止中
          //停止中のときはすぐにsmiControlled()を呼び出す
          smiControlled ();
        } else {  //動作中
          //動作中のときは次回のsmtUpdateFrame()でsmiControlled()を呼び出す
          smiPending = true;
        }
      }
    }  //smiTestChanged()

    //smiControlled ()
    //  MaskまたはTestが操作された
    public void smiControlled () {
      //オーバーライドする
    }  //smiControlled()

  }  //class SMTItem


  //----------------------------------------------------------------------------------------
  //CRTC

  public static class SMTItem_CrtHFEd extends SMTItem {
    public SMTItem_CrtHFEd () {
      super ("HFEd", CRTC.crtR00HFrontEndPort, 8);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E80000.w 7-0 Horizontal front porch end column",
                      "ja", "$E80000.w 7-0 水平フロントポーチ終了カラム");
    }
    @Override public void smiControlled () {
      CRTC.crtR00HFrontEndMask = smiMaskValue;
      CRTC.crtR00HFrontEndTest = smiTestValue;
      int curr = CRTC.crtR00HFrontEndMask == 0 ? CRTC.crtR00HFrontEndPort : CRTC.crtR00HFrontEndTest;
      if (CRTC.crtR00HFrontEndCurr != curr) {
        CRTC.crtR00HFrontEndCurr = curr;
        if (XEiJ.mpuTask != null) {  //動作中
          CRTC.crtRestart ();
        }
      }
    }
  }  //class SMTItem_CrtHFEd

  public static class SMTItem_CrtHSEd extends SMTItem {
    public SMTItem_CrtHSEd () {
      super ("HSEd", CRTC.crtR01HSyncEndPort, 8);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E80002.w 7-0 Horizontal sync pulse end column",
                      "ja", "$E80002.w 7-0 水平同期パルス終了カラム");
    }
    @Override public void smiControlled () {
      CRTC.crtR01HSyncEndMask = smiMaskValue;
      CRTC.crtR01HSyncEndTest = smiTestValue;
      int curr = CRTC.crtR01HSyncEndMask == 0 ? CRTC.crtR01HSyncEndPort : CRTC.crtR01HSyncEndTest;
      if (CRTC.crtR01HSyncEndCurr != curr) {
        CRTC.crtR01HSyncEndCurr = curr;
        if (XEiJ.mpuTask != null) {  //動作中
          CRTC.crtRestart ();
        }
      }
    }
  }  //class SMTItem_CrtHSEd

  public static class SMTItem_CrtHBEd extends SMTItem {
    public SMTItem_CrtHBEd () {
      super ("HBEd", CRTC.crtR02HBackEndPort, 8);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E80004.w 7-0 Horizontal back porch end column -4",
                      "ja", "$E80004.w 7-0 水平バックポーチ終了カラム -4");
    }
    @Override public void smiControlled () {
      CRTC.crtR02HBackEndMask = smiMaskValue;
      CRTC.crtR02HBackEndTest = smiTestValue;
      int curr = CRTC.crtR02HBackEndMask == 0 ? CRTC.crtR02HBackEndPort : CRTC.crtR02HBackEndTest;
      if (CRTC.crtR02HBackEndCurr != curr) {
        CRTC.crtR02HBackEndCurr = curr;
        if (XEiJ.mpuTask != null) {  //動作中
          CRTC.crtRestart ();
        }
      }
    }
  }  //class SMTItem_CrtHBEd

  public static class SMTItem_CrtHDEd extends SMTItem {
    public SMTItem_CrtHDEd () {
      super ("HDEd", CRTC.crtR03HDispEndPort, 8);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E80006.w 7-0 Horizontal display period end column -4",
                      "ja", "$E80006.w 7-0 水平映像期間終了カラム -4");
    }
    @Override public void smiControlled () {
      CRTC.crtR03HDispEndMask = smiMaskValue;
      CRTC.crtR03HDispEndTest = smiTestValue;
      int curr = CRTC.crtR03HDispEndMask == 0 ? CRTC.crtR03HDispEndPort : CRTC.crtR03HDispEndTest;
      if (CRTC.crtR03HDispEndCurr != curr) {
        CRTC.crtR03HDispEndCurr = curr;
        if (XEiJ.mpuTask != null) {  //動作中
          CRTC.crtRestart ();
        }
      }
    }
  }  //class SMTItem_CrtHDEd

  public static class SMTItem_CrtVFEd extends SMTItem {
    public SMTItem_CrtVFEd () {
      super ("VFEd", CRTC.crtR04VFrontEndPort, 10);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E80008.w 9-0 Vertical front porch end raster",
                      "ja", "$E80008.w 9-0 垂直フロントポーチ終了ラスタ");
    }
    @Override public void smiControlled () {
      CRTC.crtR04VFrontEndMask = smiMaskValue;
      CRTC.crtR04VFrontEndTest = smiTestValue;
      int curr = CRTC.crtR04VFrontEndMask == 0 ? CRTC.crtR04VFrontEndPort : CRTC.crtR04VFrontEndTest;
      if (CRTC.crtR04VFrontEndCurr != curr) {
        CRTC.crtR04VFrontEndCurr = curr;
        if (CRTC.CRT_RASTER_HASH_ON) {
          CRTC.crtUpdateRasterHash ();
        }
        if (XEiJ.mpuTask != null) {  //動作中
          CRTC.crtRestart ();
        }
      }
    }
  }  //class SMTItem_CrtVFEd

  public static class SMTItem_CrtVSEd extends SMTItem {
    public SMTItem_CrtVSEd () {
      super ("VSEd", CRTC.crtR05VSyncEndPort, 10);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E8000A.w 9-0 Vertical sync pulse end raster",
                      "ja", "$E8000A.w 9-0 垂直同期パルス終了ラスタ");
    }
    @Override public void smiControlled () {
      CRTC.crtR05VSyncEndMask = smiMaskValue;
      CRTC.crtR05VSyncEndTest = smiTestValue;
      int curr = CRTC.crtR05VSyncEndMask == 0 ? CRTC.crtR05VSyncEndPort : CRTC.crtR05VSyncEndTest;
      if (CRTC.crtR05VSyncEndCurr != curr) {
        CRTC.crtR05VSyncEndCurr = curr;
        if (XEiJ.mpuTask != null) {  //動作中
          CRTC.crtRestart ();
        }
      }
    }
  }  //class SMTItem_CrtVSEd

  public static class SMTItem_CrtVBEd extends SMTItem {
    public SMTItem_CrtVBEd () {
      super ("VBEd", CRTC.crtR06VBackEndPort, 10);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E8000C.w 9-0 Vertical back porch end raster",
                      "ja", "$E8000C.w 9-0 垂直バックポーチ終了ラスタ");
    }
    @Override public void smiControlled () {
      CRTC.crtR06VBackEndMask = smiMaskValue;
      CRTC.crtR06VBackEndTest = smiTestValue;
      int curr = CRTC.crtR06VBackEndMask == 0 ? CRTC.crtR06VBackEndPort : CRTC.crtR06VBackEndTest;
      if (CRTC.crtR06VBackEndCurr != curr) {
        CRTC.crtR06VBackEndCurr = curr;
        if (XEiJ.mpuTask != null) {  //動作中
          CRTC.crtRestart ();
        }
      }
    }
  }  //class SMTItem_CrtVBEd

  public static class SMTItem_CrtVDEd extends SMTItem {
    public SMTItem_CrtVDEd () {
      super ("VDEd", CRTC.crtR07VDispEndPort, 10);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E8000E.w 9-0 Vertical display period end raster",
                      "ja", "$E8000E.w 9-0 垂直映像期間終了ラスタ");
    }
    @Override public void smiControlled () {
      CRTC.crtR07VDispEndMask = smiMaskValue;
      CRTC.crtR07VDispEndTest = smiTestValue;
      int curr = CRTC.crtR07VDispEndMask == 0 ? CRTC.crtR07VDispEndPort : CRTC.crtR07VDispEndTest;
      if (CRTC.crtR07VDispEndCurr != curr) {
        CRTC.crtR07VDispEndCurr = curr;
        if (XEiJ.mpuTask != null) {  //動作中
          CRTC.crtRestart ();
        }
      }
    }
  }  //class SMTItem_CrtVDEd

  public static class SMTItem_CrtGrMM extends SMTItem {
    public SMTItem_CrtGrMM () {
      super ("GrMM", CRTC.crtMemoryModePort, 3);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E80028.w 10-8 Memory mode of graphic screen",
                      "ja", "$E80028.w 10-8 グラフィック画面のメモリモード");
    }
    @Override public void smiControlled () {
      CRTC.crtMemoryModeMask = smiMaskValue;
      CRTC.crtMemoryModeTest = smiTestValue;
      CRTC.crtSetMemoryMode (CRTC.crtTextStorage, CRTC.crtGraphicStorage, CRTC.crtMemoryModePort);
    }
  }  //class SMTItem_CrtGrMM

  public static class SMTItem_CrtHigh extends SMTItem {
    public SMTItem_CrtHigh () {
      super ("High", CRTC.crtHighResoPort, 1);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E80028.w 4 High-resolution",
                      "ja", "$E80028.w 4 高解像度");
    }
    @Override public void smiControlled () {
      CRTC.crtHighResoMask = smiMaskValue;
      CRTC.crtHighResoTest = smiTestValue;
      int curr = CRTC.crtHighResoMask == 0 ? CRTC.crtHighResoPort : CRTC.crtHighResoTest;
      if (CRTC.crtHighResoCurr != curr) {
        CRTC.crtHighResoCurr = curr;
        if (XEiJ.mpuTask != null) {  //動作中
          CRTC.crtRestart ();
        }
      }
    }
  }  //class SMTItem_CrtHigh

  public static class SMTItem_CrtVRes extends SMTItem {
    public SMTItem_CrtVRes () {
      super ("VRes", CRTC.crtVResoPort, 2);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E80028.w 3-2 Vertical resolution",
                      "ja", "$E80028.w 3-2 垂直解像度");
    }
    @Override public void smiControlled () {
      CRTC.crtVResoMask = smiMaskValue;
      CRTC.crtVResoTest = smiTestValue;
      int curr = CRTC.crtVResoMask == 0 ? CRTC.crtVResoPort : CRTC.crtVResoTest;
      if (CRTC.crtVResoCurr != curr) {
        CRTC.crtVResoCurr = curr;
        if (XEiJ.mpuTask != null) {  //動作中
          CRTC.crtRestart ();
        }
      }
    }
  }  //class SMTItem_CrtVRes

  public static class SMTItem_CrtHRes extends SMTItem {
    public SMTItem_CrtHRes () {
      super ("HRes", CRTC.crtHResoPort, 2);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E80028.w 1-0 Horizontal resolution",
                      "ja", "$E80028.w 1-0 水平解像度");
    }
    @Override public void smiControlled () {
      CRTC.crtHResoMask = smiMaskValue;
      CRTC.crtHResoTest = smiTestValue;
      int curr = CRTC.crtHResoMask == 0 ? CRTC.crtHResoPort : CRTC.crtHResoTest;
      if (CRTC.crtHResoCurr != curr) {
        CRTC.crtHResoCurr = curr;
        if (XEiJ.mpuTask != null) {  //動作中
          CRTC.crtRestart ();
        }
      }
    }
  }  //class SMTItem_CrtHRes

  public static class SMTItem_CrtIRQ extends SMTItem {
    public SMTItem_CrtIRQ () {
      super ("IRQ", CRTC.crtR09IRQRasterPort, 10);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E80012.w 9-0 IRQ raster number",
                      "ja", "$E80012.w 9-0 IRQ ラスタ番号");
    }
    @Override public void smiControlled () {
      CRTC.crtR09IRQRasterMask = smiMaskValue;
      CRTC.crtR09IRQRasterTest = smiTestValue;
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
  }  //class SMTItem_CrtIRQ

  public static class SMTItem_CrtTxX extends SMTItem {
    public SMTItem_CrtTxX () {
      super ("TxX", CRTC.crtR10TxXPort, 10);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E80014.w 9-0 Text screen x-direction scroll",
                      "ja", "$E80014.w 9-0 テキスト画面 x 方向スクロール");
    }
    @Override public void smiControlled () {
      CRTC.crtR10TxXMask = smiMaskValue;
      CRTC.crtR10TxXTest = smiTestValue;
      int curr = CRTC.crtR10TxXMask == 0 ? CRTC.crtR10TxXPort : CRTC.crtR10TxXTest;
      if (CRTC.crtR10TxXCurr != curr) {
        CRTC.crtR10TxXCurr = curr;
        CRTC.crtAllStamp += 2;
        if (XEiJ.mpuTask == null) {  //停止中
          CRTC.crtRepaint ();
        }
      }
    }
  }  //class SMTItem_CrtTxX

  public static class SMTItem_CrtTxY extends SMTItem {
    public SMTItem_CrtTxY () {
      super ("TxY", CRTC.crtR11TxYPort, 10);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E80016.w 9-0 Text screen y-direction scroll",
                      "ja", "$E80016.w 9-0 テキスト画面 y 方向スクロール");
    }
    @Override public void smiControlled () {
      CRTC.crtR11TxYMask = smiMaskValue;
      CRTC.crtR11TxYTest = smiTestValue;
      int curr = CRTC.crtR11TxYMask == 0 ? CRTC.crtR11TxYPort : CRTC.crtR11TxYTest;
      if (CRTC.crtR11TxYCurr != curr) {
        CRTC.crtR11TxYCurr = curr;
        CRTC.crtAllStamp += 2;
        if (XEiJ.mpuTask == null) {  //停止中
          CRTC.crtRepaint ();
        }
      }
    }
  }  //class SMTItem_CrtTxY

  public static class SMTItem_CrtGr0X extends SMTItem {
    public SMTItem_CrtGr0X () {
      super ("Gr0X", CRTC.crtR12GrXPort[0], 10);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E80018.w 9-0 graphic plane 0 x-direction scroll",
                      "ja", "$E80018.w 9-0 グラフィックプレーン 0 x 方向スクロール");
    }
    @Override public void smiControlled () {
      CRTC.crtR12GrXMask[0] = smiMaskValue;
      CRTC.crtR12GrXTest[0] = smiTestValue;
      int curr = CRTC.crtR12GrXMask[0] == 0 ? CRTC.crtR12GrXPort[0] : CRTC.crtR12GrXTest[0];
      if (CRTC.crtR12GrXCurr[0] != curr) {
        CRTC.crtR12GrXCurr[0] = curr;
        CRTC.crtAllStamp += 2;
        if (XEiJ.mpuTask == null) {  //停止中
          CRTC.crtRepaint ();
        }
      }
    }
  }  //class SMTItem_CrtGr0X

  public static class SMTItem_CrtGr0Y extends SMTItem {
    public SMTItem_CrtGr0Y () {
      super ("Gr0Y", CRTC.crtR13GrYPort[0], 10);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E8001A.w 9-0 graphic plane 0 y-direction scroll",
                      "ja", "$E8001A.w 9-0 グラフィックプレーン 0 y 方向スクロール");
    }
    @Override public void smiControlled () {
      CRTC.crtR13GrYMask[0] = smiMaskValue;
      CRTC.crtR13GrYTest[0] = smiTestValue;
      int curr = CRTC.crtR13GrYMask[0] == 0 ? CRTC.crtR13GrYPort[0] : CRTC.crtR13GrYTest[0];
      if (CRTC.crtR13GrYCurr[0] != curr) {
        CRTC.crtR13GrYCurr[0] = curr;
        CRTC.crtAllStamp += 2;
        if (XEiJ.mpuTask == null) {  //停止中
          CRTC.crtRepaint ();
        }
      }
    }
  }  //class SMTItem_CrtGr0Y

  public static class SMTItem_CrtGr1X extends SMTItem {
    public SMTItem_CrtGr1X () {
      super ("Gr1X", CRTC.crtR12GrXPort[1], 9);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E8001C.w 8-0 graphic plane 0 x-direction scroll",
                      "ja", "$E8001C.w 8-0 グラフィックプレーン 0 x 方向スクロール");
    }
    @Override public void smiControlled () {
      CRTC.crtR12GrXMask[1] = smiMaskValue;
      CRTC.crtR12GrXTest[1] = smiTestValue;
      int curr = CRTC.crtR12GrXMask[1] == 0 ? CRTC.crtR12GrXPort[1] : CRTC.crtR12GrXTest[1];
      if (CRTC.crtR12GrXCurr[1] != curr) {
        CRTC.crtR12GrXCurr[1] = curr;
        CRTC.crtAllStamp += 2;
        if (XEiJ.mpuTask == null) {  //停止中
          CRTC.crtRepaint ();
        }
      }
    }
  }  //class SMTItem_CrtGr1X

  public static class SMTItem_CrtGr1Y extends SMTItem {
    public SMTItem_CrtGr1Y () {
      super ("Gr1Y", CRTC.crtR13GrYPort[1], 9);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E8001E.w 8-0 graphic plane 0 y-direction scroll",
                      "ja", "$E8001E.w 8-0 グラフィックプレーン 0 y 方向スクロール");
    }
    @Override public void smiControlled () {
      CRTC.crtR13GrYMask[1] = smiMaskValue;
      CRTC.crtR13GrYTest[1] = smiTestValue;
      int curr = CRTC.crtR13GrYMask[1] == 0 ? CRTC.crtR13GrYPort[1] : CRTC.crtR13GrYTest[1];
      if (CRTC.crtR13GrYCurr[1] != curr) {
        CRTC.crtR13GrYCurr[1] = curr;
        CRTC.crtAllStamp += 2;
        if (XEiJ.mpuTask == null) {  //停止中
          CRTC.crtRepaint ();
        }
      }
    }
  }  //class SMTItem_CrtGr1Y

  public static class SMTItem_CrtGr2X extends SMTItem {
    public SMTItem_CrtGr2X () {
      super ("Gr2X", CRTC.crtR12GrXPort[2], 9);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E80020.w 8-0 graphic plane 0 x-direction scroll",
                      "ja", "$E80020.w 8-0 グラフィックプレーン 0 x 方向スクロール");
    }
    @Override public void smiControlled () {
      CRTC.crtR12GrXMask[2] = smiMaskValue;
      CRTC.crtR12GrXTest[2] = smiTestValue;
      int curr = CRTC.crtR12GrXMask[2] == 0 ? CRTC.crtR12GrXPort[2] : CRTC.crtR12GrXTest[2];
      if (CRTC.crtR12GrXCurr[2] != curr) {
        CRTC.crtR12GrXCurr[2] = curr;
        CRTC.crtAllStamp += 2;
        if (XEiJ.mpuTask == null) {  //停止中
          CRTC.crtRepaint ();
        }
      }
    }
  }  //class SMTItem_CrtGr2X

  public static class SMTItem_CrtGr2Y extends SMTItem {
    public SMTItem_CrtGr2Y () {
      super ("Gr2Y", CRTC.crtR13GrYPort[2], 9);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E80022.w 8-0 graphic plane 0 y-direction scroll",
                      "ja", "$E80022.w 8-0 グラフィックプレーン 0 y 方向スクロール");
    }
    @Override public void smiControlled () {
      CRTC.crtR13GrYMask[2] = smiMaskValue;
      CRTC.crtR13GrYTest[2] = smiTestValue;
      int curr = CRTC.crtR13GrYMask[2] == 0 ? CRTC.crtR13GrYPort[2] : CRTC.crtR13GrYTest[2];
      if (CRTC.crtR13GrYCurr[2] != curr) {
        CRTC.crtR13GrYCurr[2] = curr;
        CRTC.crtAllStamp += 2;
        if (XEiJ.mpuTask == null) {  //停止中
          CRTC.crtRepaint ();
        }
      }
    }
  }  //class SMTItem_CrtGr2Y

  public static class SMTItem_CrtGr3X extends SMTItem {
    public SMTItem_CrtGr3X () {
      super ("Gr3X", CRTC.crtR12GrXPort[3], 9);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E80024.w 8-0 graphic plane 3 x-direction scroll",
                      "ja", "$E80024.w 8-0 グラフィックプレーン 3 x 方向スクロール");
    }
    @Override public void smiControlled () {
      CRTC.crtR12GrXMask[3] = smiMaskValue;
      CRTC.crtR12GrXTest[3] = smiTestValue;
      int curr = CRTC.crtR12GrXMask[3] == 0 ? CRTC.crtR12GrXPort[3] : CRTC.crtR12GrXTest[3];
      if (CRTC.crtR12GrXCurr[3] != curr) {
        CRTC.crtR12GrXCurr[3] = curr;
        CRTC.crtAllStamp += 2;
        if (XEiJ.mpuTask == null) {  //停止中
          CRTC.crtRepaint ();
        }
      }
    }
  }  //class SMTItem_CrtGr3X

  public static class SMTItem_CrtGr3Y extends SMTItem {
    public SMTItem_CrtGr3Y () {
      super ("Gr3Y", CRTC.crtR13GrYPort[3], 9);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E80026.w 8-0 graphic plane 3 y-direction scroll",
                      "ja", "$E80026.w 8-0 グラフィックプレーン 3 y 方向スクロール");
    }
    @Override public void smiControlled () {
      CRTC.crtR13GrYMask[3] = smiMaskValue;
      CRTC.crtR13GrYTest[3] = smiTestValue;
      int curr = CRTC.crtR13GrYMask[3] == 0 ? CRTC.crtR13GrYPort[3] : CRTC.crtR13GrYTest[3];
      if (CRTC.crtR13GrYCurr[3] != curr) {
        CRTC.crtR13GrYCurr[3] = curr;
        CRTC.crtAllStamp += 2;
        if (XEiJ.mpuTask == null) {  //停止中
          CRTC.crtRepaint ();
        }
      }
    }
  }  //class SMTItem_CrtGr3Y


  //----------------------------------------------------------------------------------------
  //ビデオコントローラ

  public static class SMTItem_VcnGrMM extends SMTItem {
    public SMTItem_VcnGrMM () {
      super ("GrMM", VideoController.vcnReg1Port, 3);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E82400.w 2-0 Memory mode of graphic screen",
                      "ja", "$E82400.w 2-0 グラフィック画面のメモリモード");
    }
    @Override public void smiControlled () {
      VideoController.vcnReg1Mask = VideoController.vcnReg1Mask & ~7 | smiMaskValue & 7;
      VideoController.vcnReg1Test = VideoController.vcnReg1Test & ~7 | smiTestValue;
      int curr = VideoController.vcnReg1Port & ~VideoController.vcnReg1Mask | VideoController.vcnReg1Test & VideoController.vcnReg1Mask;
      if (VideoController.vcnReg1Curr != curr) {
        VideoController.vcnReg1Curr = curr;
        VideoController.vcnUpdateMode ();
        if (XEiJ.mpuTask == null) {  //停止中
          CRTC.crtRepaint ();
        }
      }
    }
  }  //class SMTItem_VcnGrMM

  public static class SMTItem_VcnSpPr extends SMTItem {
    public SMTItem_VcnSpPr () {
      super ("SpPr", VideoController.vcnReg2Port >> 12, 2);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E82500.w 13-12 Precedence of sprite screen",
                      "ja", "$E82500.w 13-12 スプライト画面の優先順位");
    }
    @Override public void smiControlled () {
      VideoController.vcnReg2Mask = VideoController.vcnReg2Mask & ~(3 << 12) | smiMaskValue & 3 << 12;
      VideoController.vcnReg2Test = VideoController.vcnReg2Test & ~(3 << 12) | smiTestValue     << 12;
      int curr = VideoController.vcnReg2Port & ~VideoController.vcnReg2Mask | VideoController.vcnReg2Test & VideoController.vcnReg2Mask;
      if (VideoController.vcnReg2Curr != curr) {
        VideoController.vcnReg2Curr = curr;
        VideoController.vcnUpdateMode ();
        if (XEiJ.mpuTask == null) {  //停止中
          CRTC.crtRepaint ();
        }
      }
    }
  }  //class SMTItem_VcnSpPr

  public static class SMTItem_VcnTxPr extends SMTItem {
    public SMTItem_VcnTxPr () {
      super ("TxPr", VideoController.vcnReg2Port >> 10, 2);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E82500.w 11-10 Precedence of text screen",
                      "ja", "$E82500.w 11-10 テキスト画面の優先順位");
    }
    @Override public void smiControlled () {
      VideoController.vcnReg2Mask = VideoController.vcnReg2Mask & ~(3 << 10) | smiMaskValue & 3 << 10;
      VideoController.vcnReg2Test = VideoController.vcnReg2Test & ~(3 << 10) | smiTestValue     << 10;
      int curr = VideoController.vcnReg2Port & ~VideoController.vcnReg2Mask | VideoController.vcnReg2Test & VideoController.vcnReg2Mask;
      if (VideoController.vcnReg2Curr != curr) {
        VideoController.vcnReg2Curr = curr;
        VideoController.vcnUpdateMode ();
        if (XEiJ.mpuTask == null) {  //停止中
          CRTC.crtRepaint ();
        }
      }
    }
  }  //class SMTItem_VcnTxPr

  public static class SMTItem_VcnGrPr extends SMTItem {
    public SMTItem_VcnGrPr () {
      super ("GrPr", VideoController.vcnReg2Port >> 8, 2);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E82500.w 9-8 Precedence of graphic screen",
                      "ja", "$E82500.w 9-8 グラフィック画面の優先順位");
    }
    @Override public void smiControlled () {
      VideoController.vcnReg2Mask = VideoController.vcnReg2Mask & ~(3 << 8) | smiMaskValue & 3 << 8;
      VideoController.vcnReg2Test = VideoController.vcnReg2Test & ~(3 << 8) | smiTestValue     << 8;
      int curr = VideoController.vcnReg2Port & ~VideoController.vcnReg2Mask | VideoController.vcnReg2Test & VideoController.vcnReg2Mask;
      if (VideoController.vcnReg2Curr != curr) {
        VideoController.vcnReg2Curr = curr;
        VideoController.vcnUpdateMode ();
        if (XEiJ.mpuTask == null) {  //停止中
          CRTC.crtRepaint ();
        }
      }
    }
  }  //class SMTItem_VcnGrPr

  public static class SMTItem_VcnG4th extends SMTItem {
    public SMTItem_VcnG4th () {
      super ("G4th", VideoController.vcnReg2Port >> 6, 2);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E82500.w 7-6 The farthest plane number of graphic screen",
                      "ja", "$E82500.w 7-6 グラフィック画面の一番奥のプレーンの番号");
    }
    @Override public void smiControlled () {
      VideoController.vcnReg2Mask = VideoController.vcnReg2Mask & ~(3 << 6) | smiMaskValue & 3 << 6;
      VideoController.vcnReg2Test = VideoController.vcnReg2Test & ~(3 << 6) | smiTestValue     << 6;
      int curr = VideoController.vcnReg2Port & ~VideoController.vcnReg2Mask | VideoController.vcnReg2Test & VideoController.vcnReg2Mask;
      if (VideoController.vcnReg2Curr != curr) {
        VideoController.vcnReg2Curr = curr;
        VideoController.vcnUpdateMode ();
        if (XEiJ.mpuTask == null) {  //停止中
          CRTC.crtRepaint ();
        }
      }
    }
  }  //class SMTItem_VcnG4th

  public static class SMTItem_VcnG3rd extends SMTItem {
    public SMTItem_VcnG3rd () {
      super ("G3rd", VideoController.vcnReg2Port >> 4, 2);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E82500.w 5-4 Second-farthest plane number of graphic screen",
                      "ja", "$E82500.w 5-4 グラフィック画面の奥から 2 番目のプレーンの番号");
    }
    @Override public void smiControlled () {
      VideoController.vcnReg2Mask = VideoController.vcnReg2Mask & ~(3 << 4) | smiMaskValue & 3 << 4;
      VideoController.vcnReg2Test = VideoController.vcnReg2Test & ~(3 << 4) | smiTestValue     << 4;
      int curr = VideoController.vcnReg2Port & ~VideoController.vcnReg2Mask | VideoController.vcnReg2Test & VideoController.vcnReg2Mask;
      if (VideoController.vcnReg2Curr != curr) {
        VideoController.vcnReg2Curr = curr;
        VideoController.vcnUpdateMode ();
        if (XEiJ.mpuTask == null) {  //停止中
          CRTC.crtRepaint ();
        }
      }
    }
  }  //class SMTItem_VcnG3rd

  public static class SMTItem_VcnG2nd extends SMTItem {
    public SMTItem_VcnG2nd () {
      super ("G2nd", VideoController.vcnReg2Port >> 2, 2);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E82500.w 3-2 Second-nearest plane number of graphic screen",
                      "ja", "$E82500.w 3-2 グラフィック画面の手前から 2 番目のプレーンの番号");
    }
    @Override public void smiControlled () {
      VideoController.vcnReg2Mask = VideoController.vcnReg2Mask & ~(3 << 2) | smiMaskValue & 3 << 2;
      VideoController.vcnReg2Test = VideoController.vcnReg2Test & ~(3 << 2) | smiTestValue     << 2;
      int curr = VideoController.vcnReg2Port & ~VideoController.vcnReg2Mask | VideoController.vcnReg2Test & VideoController.vcnReg2Mask;
      if (VideoController.vcnReg2Curr != curr) {
        VideoController.vcnReg2Curr = curr;
        VideoController.vcnUpdateMode ();
        if (XEiJ.mpuTask == null) {  //停止中
          CRTC.crtRepaint ();
        }
      }
    }
  }  //class SMTItem_VcnG2nd

  public static class SMTItem_VcnG1st extends SMTItem {
    public SMTItem_VcnG1st () {
      super ("G1st", VideoController.vcnReg2Port, 2);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E82500.w 1-0 The nearest plane number of graphic screen",
                      "ja", "$E82500.w 1-0 グラフィック画面の一番手前のプレーンの番号");
    }
    @Override public void smiControlled () {
      VideoController.vcnReg2Mask = VideoController.vcnReg2Mask & ~3 | smiMaskValue & 3;
      VideoController.vcnReg2Test = VideoController.vcnReg2Test & ~3 | smiTestValue;
      int curr = VideoController.vcnReg2Port & ~VideoController.vcnReg2Mask | VideoController.vcnReg2Test & VideoController.vcnReg2Mask;
      if (VideoController.vcnReg2Curr != curr) {
        VideoController.vcnReg2Curr = curr;
        VideoController.vcnUpdateMode ();
        if (XEiJ.mpuTask == null) {  //停止中
          CRTC.crtRepaint ();
        }
      }
    }
  }  //class SMTItem_VcnG1st

  public static class SMTItem_VcnAHOn extends SMTItem {
    public SMTItem_VcnAHOn () {
      super ("AHOn", VideoController.vcnReg3Port >> 14, 1);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E82600.w 14 Halftone with text palet 0",
                      "ja", "$E82600.w 14 テキストパレット 0 との半透明");
    }
    @Override public void smiControlled () {
      VideoController.vcnReg3Mask = VideoController.vcnReg3Mask & ~(1 << 14) | smiMaskValue & 1 << 14;
      VideoController.vcnReg3Test = VideoController.vcnReg3Test & ~(1 << 14) | smiTestValue     << 14;
      int curr = VideoController.vcnReg3Port & ~VideoController.vcnReg3Mask | VideoController.vcnReg3Test & VideoController.vcnReg3Mask;
      if (VideoController.vcnReg3Curr != curr) {
        VideoController.vcnReg3Curr = curr;
        VideoController.vcnUpdateMode ();
        if (XEiJ.mpuTask == null) {  //停止中
          CRTC.crtRepaint ();
        }
      }
    }
  }  //class SMTItem_VcnAHOn

  public static class SMTItem_VcnExOn extends SMTItem {
    public SMTItem_VcnExOn () {
      super ("ExOn", VideoController.vcnReg3Port >> 12, 1);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E82600.w 12 Extended (special priority or halftone)",
                      "ja", "$E82600.w 12 拡張 (特殊プライオリティまたは半透明)");
    }
    @Override public void smiControlled () {
      VideoController.vcnReg3Mask = VideoController.vcnReg3Mask & ~(1 << 12) | smiMaskValue & 1 << 12;
      VideoController.vcnReg3Test = VideoController.vcnReg3Test & ~(1 << 12) | smiTestValue     << 12;
      int curr = VideoController.vcnReg3Port & ~VideoController.vcnReg3Mask | VideoController.vcnReg3Test & VideoController.vcnReg3Mask;
      if (VideoController.vcnReg3Curr != curr) {
        VideoController.vcnReg3Curr = curr;
        VideoController.vcnUpdateMode ();
        if (XEiJ.mpuTask == null) {  //停止中
          CRTC.crtRepaint ();
        }
      }
    }
  }  //class SMTItem_VcnExOn

  public static class SMTItem_VcnHalf extends SMTItem {
    public SMTItem_VcnHalf () {
      super ("Half", VideoController.vcnReg3Port >> 11, 1);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E82600.w 11 Halftone",
                      "ja", "$E82600.w 11 半透明");
    }
    @Override public void smiControlled () {
      VideoController.vcnReg3Mask = VideoController.vcnReg3Mask & ~(1 << 11) | smiMaskValue & 1 << 11;
      VideoController.vcnReg3Test = VideoController.vcnReg3Test & ~(1 << 11) | smiTestValue     << 11;
      int curr = VideoController.vcnReg3Port & ~VideoController.vcnReg3Mask | VideoController.vcnReg3Test & VideoController.vcnReg3Mask;
      if (VideoController.vcnReg3Curr != curr) {
        VideoController.vcnReg3Curr = curr;
        VideoController.vcnUpdateMode ();
        if (XEiJ.mpuTask == null) {  //停止中
          CRTC.crtRepaint ();
        }
      }
    }
  }  //class SMTItem_VcnHalf

  public static class SMTItem_VcnPLSB extends SMTItem {
    public SMTItem_VcnPLSB () {
      super ("PLSB", VideoController.vcnReg3Port >> 10, 1);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E82600.w 10 Select area by LSB of palet code",
                      "ja", "$E82600.w 10 パレットコードの LSB で範囲を選択");
    }
    @Override public void smiControlled () {
      VideoController.vcnReg3Mask = VideoController.vcnReg3Mask & ~(1 << 10) | smiMaskValue & 1 << 10;
      VideoController.vcnReg3Test = VideoController.vcnReg3Test & ~(1 << 10) | smiTestValue     << 10;
      int curr = VideoController.vcnReg3Port & ~VideoController.vcnReg3Mask | VideoController.vcnReg3Test & VideoController.vcnReg3Mask;
      if (VideoController.vcnReg3Curr != curr) {
        VideoController.vcnReg3Curr = curr;
        VideoController.vcnUpdateMode ();
        if (XEiJ.mpuTask == null) {  //停止中
          CRTC.crtRepaint ();
        }
      }
    }
  }  //class SMTItem_VcnPLSB

  public static class SMTItem_VcnGrGr extends SMTItem {
    public SMTItem_VcnGrGr () {
      super ("GrGr", VideoController.vcnReg3Port >> 9, 1);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E82600.w 9 Halftone between the nearest graphic page and second-nearest graphic page",
                      "ja", "$E82600.w 9 一番手前のグラフィックページと手前から 2 番目のグラフィックページとの半透明");
    }
    @Override public void smiControlled () {
      VideoController.vcnReg3Mask = VideoController.vcnReg3Mask & ~(1 << 9) | smiMaskValue & 1 << 9;
      VideoController.vcnReg3Test = VideoController.vcnReg3Test & ~(1 << 9) | smiTestValue     << 9;
      int curr = VideoController.vcnReg3Port & ~VideoController.vcnReg3Mask | VideoController.vcnReg3Test & VideoController.vcnReg3Mask;
      if (VideoController.vcnReg3Curr != curr) {
        VideoController.vcnReg3Curr = curr;
        VideoController.vcnUpdateMode ();
        if (XEiJ.mpuTask == null) {  //停止中
          CRTC.crtRepaint ();
        }
      }
    }
  }  //class SMTItem_VcnGrGr

  public static class SMTItem_VcnGrST extends SMTItem {
    public SMTItem_VcnGrST () {
      super ("GrST", VideoController.vcnReg3Port >> 8, 1);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E82600.w 8 Halftone between the nearest graphic page and sprite/text screen in the back of graphic screen",
                      "ja", "$E82600.w 8 一番手前のグラフィックページとグラフィック画面の奥にあるスプライト/テキスト画面との半透明");
    }
    @Override public void smiControlled () {
      VideoController.vcnReg3Mask = VideoController.vcnReg3Mask & ~(1 << 8) | smiMaskValue & 1 << 8;
      VideoController.vcnReg3Test = VideoController.vcnReg3Test & ~(1 << 8) | smiTestValue     << 8;
      int curr = VideoController.vcnReg3Port & ~VideoController.vcnReg3Mask | VideoController.vcnReg3Test & VideoController.vcnReg3Mask;
      if (VideoController.vcnReg3Curr != curr) {
        VideoController.vcnReg3Curr = curr;
        VideoController.vcnUpdateMode ();
        if (XEiJ.mpuTask == null) {  //停止中
          CRTC.crtRepaint ();
        }
      }
    }
  }  //class SMTItem_VcnGrST

  public static class SMTItem_VcnSpOn extends SMTItem {
    public SMTItem_VcnSpOn () {
      super ("SpOn", VideoController.vcnReg3Port >> 6, 1);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E82600.w 6 Sprite screen",
                      "ja", "$E82600.w 6 スプライト画面");
    }
    @Override public void smiControlled () {
      VideoController.vcnReg3Mask = VideoController.vcnReg3Mask & ~(1 << 6) | smiMaskValue & 1 << 6;
      VideoController.vcnReg3Test = VideoController.vcnReg3Test & ~(1 << 6) | smiTestValue     << 6;
      int curr = VideoController.vcnReg3Port & ~VideoController.vcnReg3Mask | VideoController.vcnReg3Test & VideoController.vcnReg3Mask;
      if (VideoController.vcnReg3Curr != curr) {
        VideoController.vcnReg3Curr = curr;
        VideoController.vcnUpdateMode ();
        if (XEiJ.mpuTask == null) {  //停止中
          CRTC.crtRepaint ();
        }
      }
    }
  }  //class SMTItem_VcnSpOn

  public static class SMTItem_VcnTxOn extends SMTItem {
    public SMTItem_VcnTxOn () {
      super ("TxOn", VideoController.vcnReg3Port >> 5, 1);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E82600.w 5 Text screen",
                      "ja", "$E82600.w 5 テキスト画面");
    }
    @Override public void smiControlled () {
      VideoController.vcnReg3Mask = VideoController.vcnReg3Mask & ~(1 << 5) | smiMaskValue & 1 << 5;
      VideoController.vcnReg3Test = VideoController.vcnReg3Test & ~(1 << 5) | smiTestValue     << 5;
      int curr = VideoController.vcnReg3Port & ~VideoController.vcnReg3Mask | VideoController.vcnReg3Test & VideoController.vcnReg3Mask;
      if (VideoController.vcnReg3Curr != curr) {
        VideoController.vcnReg3Curr = curr;
        VideoController.vcnUpdateMode ();
        if (XEiJ.mpuTask == null) {  //停止中
          CRTC.crtRepaint ();
        }
      }
    }
  }  //class SMTItem_VcnTxOn

  public static class SMTItem_VcnGxOn extends SMTItem {
    public SMTItem_VcnGxOn () {
      super ("GxOn", VideoController.vcnReg3Port >> 4, 1);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E82600.w 4 1024x1024 graphic screen",
                      "ja", "$E82600.w 4 1024x1024 グラフィック画面");
    }
    @Override public void smiControlled () {
      VideoController.vcnReg3Mask = VideoController.vcnReg3Mask & ~(1 << 4) | smiMaskValue & 1 << 4;
      VideoController.vcnReg3Test = VideoController.vcnReg3Test & ~(1 << 4) | smiTestValue     << 4;
      int curr = VideoController.vcnReg3Port & ~VideoController.vcnReg3Mask | VideoController.vcnReg3Test & VideoController.vcnReg3Mask;
      if (VideoController.vcnReg3Curr != curr) {
        VideoController.vcnReg3Curr = curr;
        VideoController.vcnUpdateMode ();
        if (XEiJ.mpuTask == null) {  //停止中
          CRTC.crtRepaint ();
        }
      }
    }
  }  //class SMTItem_VcnGxOn

  public static class SMTItem_VcnG4On extends SMTItem {
    public SMTItem_VcnG4On () {
      super ("G4On", VideoController.vcnReg3Port >> 3, 1);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E82600.w 3 512x512 graphic plane 3",
                      "ja", "$E82600.w 3 512x512 グラフィックプレーン 3");
    }
    @Override public void smiControlled () {
      VideoController.vcnReg3Mask = VideoController.vcnReg3Mask & ~(1 << 3) | smiMaskValue & 1 << 3;
      VideoController.vcnReg3Test = VideoController.vcnReg3Test & ~(1 << 3) | smiTestValue     << 3;
      int curr = VideoController.vcnReg3Port & ~VideoController.vcnReg3Mask | VideoController.vcnReg3Test & VideoController.vcnReg3Mask;
      if (VideoController.vcnReg3Curr != curr) {
        VideoController.vcnReg3Curr = curr;
        VideoController.vcnUpdateMode ();
        if (XEiJ.mpuTask == null) {  //停止中
          CRTC.crtRepaint ();
        }
      }
    }
  }  //class SMTItem_VcnG4On

  public static class SMTItem_VcnG3On extends SMTItem {
    public SMTItem_VcnG3On () {
      super ("G3On", VideoController.vcnReg3Port >> 2, 1);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E82600.w 2 512x512 graphic plane 2",
                      "ja", "$E82600.w 2 512x512 グラフィックプレーン 2");
    }
    @Override public void smiControlled () {
      VideoController.vcnReg3Mask = VideoController.vcnReg3Mask & ~(1 << 2) | smiMaskValue & 1 << 2;
      VideoController.vcnReg3Test = VideoController.vcnReg3Test & ~(1 << 2) | smiTestValue     << 2;
      int curr = VideoController.vcnReg3Port & ~VideoController.vcnReg3Mask | VideoController.vcnReg3Test & VideoController.vcnReg3Mask;
      if (VideoController.vcnReg3Curr != curr) {
        VideoController.vcnReg3Curr = curr;
        VideoController.vcnUpdateMode ();
        if (XEiJ.mpuTask == null) {  //停止中
          CRTC.crtRepaint ();
        }
      }
    }
  }  //class SMTItem_VcnG3On

  public static class SMTItem_VcnG2On extends SMTItem {
    public SMTItem_VcnG2On () {
      super ("G2On", VideoController.vcnReg3Port >> 1, 1);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E82600.w 1 512x512 graphic plane 1",
                      "ja", "$E82600.w 1 512x512 グラフィックプレーン 1");
    }
    @Override public void smiControlled () {
      VideoController.vcnReg3Mask = VideoController.vcnReg3Mask & ~(1 << 1) | smiMaskValue & 1 << 1;
      VideoController.vcnReg3Test = VideoController.vcnReg3Test & ~(1 << 1) | smiTestValue     << 1;
      int curr = VideoController.vcnReg3Port & ~VideoController.vcnReg3Mask | VideoController.vcnReg3Test & VideoController.vcnReg3Mask;
      if (VideoController.vcnReg3Curr != curr) {
        VideoController.vcnReg3Curr = curr;
        VideoController.vcnUpdateMode ();
        if (XEiJ.mpuTask == null) {  //停止中
          CRTC.crtRepaint ();
        }
      }
    }
  }  //class SMTItem_VcnG2On

  public static class SMTItem_VcnG1On extends SMTItem {
    public SMTItem_VcnG1On () {
      super ("G1On", VideoController.vcnReg3Port, 1);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E82600.w 0 512x512 graphic plane 0",
                      "ja", "$E82600.w 0 512x512 グラフィックプレーン 0");
    }
    @Override public void smiControlled () {
      VideoController.vcnReg3Mask = VideoController.vcnReg3Mask & ~1 | smiMaskValue & 1;
      VideoController.vcnReg3Test = VideoController.vcnReg3Test & ~1 | smiTestValue;
      int curr = VideoController.vcnReg3Port & ~VideoController.vcnReg3Mask | VideoController.vcnReg3Test & VideoController.vcnReg3Mask;
      if (VideoController.vcnReg3Curr != curr) {
        VideoController.vcnReg3Curr = curr;
        VideoController.vcnUpdateMode ();
        if (XEiJ.mpuTask == null) {  //停止中
          CRTC.crtRepaint ();
        }
      }
    }
  }  //class SMTItem_VcnG1On


  //----------------------------------------------------------------------------------------
  //スプライトコントローラ

  public static class SMTItem_SprBg0X extends SMTItem {
    public SMTItem_SprBg0X () {
      super ("Bg0X", SpriteScreen.sprReg0Bg0XPort, 10);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$EB0800.w 9-0 Background 0 x-direction scroll",
                      "ja", "$EB0800.w 9-0 バックグラウンド 0 x 方向スクロール");
    }
    @Override public void smiControlled () {
      SpriteScreen.sprReg0Bg0XMask = smiMaskValue;
      SpriteScreen.sprReg0Bg0XTest = smiTestValue;
      int curr = SpriteScreen.sprReg0Bg0XMask == 0 ? SpriteScreen.sprReg0Bg0XPort : SpriteScreen.sprReg0Bg0XTest;
      if (SpriteScreen.sprReg0Bg0XCurr != curr) {
        SpriteScreen.sprReg0Bg0XCurr = curr;
        if (VideoController.vcnReg3Curr << 31 - 6 < 0 && SpriteScreen.sprReg4BgCtrlCurr << 31 - 9 < 0) {  //スプライト画面が表示されている
          CRTC.crtAllStamp += 2;
          if (XEiJ.mpuTask == null) {  //停止中
            CRTC.crtRepaint ();
          }
        }
      }
    }
  }  //class SMTItem_SprBg0X

  public static class SMTItem_SprBg0Y extends SMTItem {
    public SMTItem_SprBg0Y () {
      super ("Bg0Y", SpriteScreen.sprReg1Bg0YPort, 10);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$EB0802.w 9-0 Background 0 y-direction scroll",
                      "ja", "$EB0802.w 9-0 バックグラウンド 0 y 方向スクロール");
    }
    @Override public void smiControlled () {
      SpriteScreen.sprReg1Bg0YMask = smiMaskValue;
      SpriteScreen.sprReg1Bg0YTest = smiTestValue;
      int curr = SpriteScreen.sprReg1Bg0YMask == 0 ? SpriteScreen.sprReg1Bg0YPort : SpriteScreen.sprReg1Bg0YTest;
      if (SpriteScreen.sprReg1Bg0YCurr != curr) {
        SpriteScreen.sprReg1Bg0YCurr = curr;
        if (VideoController.vcnReg3Curr << 31 - 6 < 0 && SpriteScreen.sprReg4BgCtrlCurr << 31 - 9 < 0) {  //スプライト画面が表示されている
          CRTC.crtAllStamp += 2;
          if (XEiJ.mpuTask == null) {  //停止中
            CRTC.crtRepaint ();
          }
        }
      }
    }
  }  //class SMTItem_SprBg0Y

  public static class SMTItem_SprBg1X extends SMTItem {
    public SMTItem_SprBg1X () {
      super ("Bg1X", SpriteScreen.sprReg2Bg1XPort, 10);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$EB0804.w 9-0 Background 1 x-direction scroll",
                      "ja", "$EB0804.w 9-0 バックグラウンド 1 x 方向スクロール");
    }
    @Override public void smiControlled () {
      SpriteScreen.sprReg2Bg1XMask = smiMaskValue;
      SpriteScreen.sprReg2Bg1XTest = smiTestValue;
      int curr = SpriteScreen.sprReg2Bg1XMask == 0 ? SpriteScreen.sprReg2Bg1XPort : SpriteScreen.sprReg2Bg1XTest;
      if (SpriteScreen.sprReg2Bg1XCurr != curr) {
        SpriteScreen.sprReg2Bg1XCurr = curr;
        if (VideoController.vcnReg3Curr << 31 - 6 < 0 && SpriteScreen.sprReg4BgCtrlCurr << 31 - 9 < 0) {  //スプライト画面が表示されている
          CRTC.crtAllStamp += 2;
          if (XEiJ.mpuTask == null) {  //停止中
            CRTC.crtRepaint ();
          }
        }
      }
    }
  }  //class SMTItem_SprBg1X

  public static class SMTItem_SprBg1Y extends SMTItem {
    public SMTItem_SprBg1Y () {
      super ("Bg1Y", SpriteScreen.sprReg3Bg1YPort, 10);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$EB0806.w 9-0 Background 1 y-direction scroll",
                      "ja", "$EB0806.w 9-0 バックグラウンド 1 y 方向スクロール");
    }
    @Override public void smiControlled () {
      SpriteScreen.sprReg3Bg1YMask = smiMaskValue;
      SpriteScreen.sprReg3Bg1YTest = smiTestValue;
      int curr = SpriteScreen.sprReg3Bg1YMask == 0 ? SpriteScreen.sprReg3Bg1YPort : SpriteScreen.sprReg3Bg1YTest;
      if (SpriteScreen.sprReg3Bg1YCurr != curr) {
        SpriteScreen.sprReg3Bg1YCurr = curr;
        if (VideoController.vcnReg3Curr << 31 - 6 < 0 && SpriteScreen.sprReg4BgCtrlCurr << 31 - 9 < 0) {  //スプライト画面が表示されている
          CRTC.crtAllStamp += 2;
          if (XEiJ.mpuTask == null) {  //停止中
            CRTC.crtRepaint ();
          }
        }
      }
    }
  }  //class SMTItem_SprBg1Y

  public static class SMTItem_SprDisp extends SMTItem {
    public SMTItem_SprDisp () {
      super ("Disp", SpriteScreen.sprReg4BgCtrlPort >> 9, 1);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$EB0808.w 9 Display sprite and background",
                      "ja", "$EB0808.w 9 スプライトとバックグラウンドを表示");
    }
    @Override public void smiControlled () {
      SpriteScreen.sprReg4BgCtrlMask = SpriteScreen.sprReg4BgCtrlMask & ~(1 << 9) | smiMaskValue & 1 << 9;
      SpriteScreen.sprReg4BgCtrlTest = SpriteScreen.sprReg4BgCtrlTest & ~(1 << 9) | smiTestValue     << 9;
      int curr = SpriteScreen.sprReg4BgCtrlPort & ~SpriteScreen.sprReg4BgCtrlMask | SpriteScreen.sprReg4BgCtrlTest & SpriteScreen.sprReg4BgCtrlMask;
      if (SpriteScreen.sprReg4BgCtrlCurr != curr) {
        SpriteScreen.sprReg4BgCtrlCurr = curr;
        if (VideoController.vcnReg3Curr << 31 - 6 < 0) {  //スプライト画面が表示されている。DispがON→OFFのときは再描画が必要
          CRTC.crtAllStamp += 2;
          if (XEiJ.mpuTask == null) {  //停止中
            CRTC.crtRepaint ();
          }
        }
      }
    }
  }  //class SMTItem_SprDisp

  public static class SMTItem_SprB1Tx extends SMTItem {
    public SMTItem_SprB1Tx () {
      super ("B1Tx", SpriteScreen.sprReg4BgCtrlPort >> 4, 2);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$EB0808.w 5-4 Text page assigned to background 1",
                      "ja", "$EB0808.w 5-4 バックグラウンド 1 に割り当てるテキストページ");
    }
    @Override public void smiControlled () {
      SpriteScreen.sprReg4BgCtrlMask = SpriteScreen.sprReg4BgCtrlMask & ~(3 << 4) | smiMaskValue & 3 << 4;
      SpriteScreen.sprReg4BgCtrlTest = SpriteScreen.sprReg4BgCtrlTest & ~(3 << 4) | smiTestValue     << 4;
      int curr = SpriteScreen.sprReg4BgCtrlPort & ~SpriteScreen.sprReg4BgCtrlMask | SpriteScreen.sprReg4BgCtrlTest & SpriteScreen.sprReg4BgCtrlMask;
      if (SpriteScreen.sprReg4BgCtrlCurr != curr) {
        SpriteScreen.sprReg4BgCtrlCurr = curr;
        if (VideoController.vcnReg3Curr << 31 - 6 < 0 && SpriteScreen.sprReg4BgCtrlCurr << 31 - 9 < 0) {  //スプライト画面が表示されている
          CRTC.crtAllStamp += 2;
          if (XEiJ.mpuTask == null) {  //停止中
            CRTC.crtRepaint ();
          }
        }
      }
    }
  }  //class SMTItem_SprB1Tx

  public static class SMTItem_SprB1On extends SMTItem {
    public SMTItem_SprB1On () {
      super ("B1On", SpriteScreen.sprReg4BgCtrlPort >> 3, 1);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$EB0808.w 3 Background 1",
                      "ja", "$EB0808.w 3 バックグラウンド 1");
    }
    @Override public void smiControlled () {
      SpriteScreen.sprReg4BgCtrlMask = SpriteScreen.sprReg4BgCtrlMask & ~(1 << 3) | smiMaskValue & 1 << 3;
      SpriteScreen.sprReg4BgCtrlTest = SpriteScreen.sprReg4BgCtrlTest & ~(1 << 3) | smiTestValue     << 3;
      int curr = SpriteScreen.sprReg4BgCtrlPort & ~SpriteScreen.sprReg4BgCtrlMask | SpriteScreen.sprReg4BgCtrlTest & SpriteScreen.sprReg4BgCtrlMask;
      if (SpriteScreen.sprReg4BgCtrlCurr != curr) {
        SpriteScreen.sprReg4BgCtrlCurr = curr;
        if (VideoController.vcnReg3Curr << 31 - 6 < 0 && SpriteScreen.sprReg4BgCtrlCurr << 31 - 9 < 0) {  //スプライト画面が表示されている
          CRTC.crtAllStamp += 2;
          if (XEiJ.mpuTask == null) {  //停止中
            CRTC.crtRepaint ();
          }
        }
      }
    }
  }  //class SMTItem_SprB1On

  public static class SMTItem_SprB0Tx extends SMTItem {
    public SMTItem_SprB0Tx () {
      super ("B0Tx", SpriteScreen.sprReg4BgCtrlPort >> 1, 2);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$EB0808.w 2-1 Text page assigned to background 0",
                      "ja", "$EB0808.w 2-1 バックグラウンド 0 に割り当てるテキストページ");
    }
    @Override public void smiControlled () {
      SpriteScreen.sprReg4BgCtrlMask = SpriteScreen.sprReg4BgCtrlMask & ~(3 << 1) | smiMaskValue & 3 << 1;
      SpriteScreen.sprReg4BgCtrlTest = SpriteScreen.sprReg4BgCtrlTest & ~(3 << 1) | smiTestValue     << 1;
      int curr = SpriteScreen.sprReg4BgCtrlPort & ~SpriteScreen.sprReg4BgCtrlMask | SpriteScreen.sprReg4BgCtrlTest & SpriteScreen.sprReg4BgCtrlMask;
      if (SpriteScreen.sprReg4BgCtrlCurr != curr) {
        SpriteScreen.sprReg4BgCtrlCurr = curr;
        if (VideoController.vcnReg3Curr << 31 - 6 < 0 && SpriteScreen.sprReg4BgCtrlCurr << 31 - 9 < 0) {  //スプライト画面が表示されている
          CRTC.crtAllStamp += 2;
          if (XEiJ.mpuTask == null) {  //停止中
            CRTC.crtRepaint ();
          }
        }
      }
    }
  }  //class SMTItem_SprB0Tx

  public static class SMTItem_SprB0On extends SMTItem {
    public SMTItem_SprB0On () {
      super ("B0On", SpriteScreen.sprReg4BgCtrlPort, 1);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$EB0808.w 0 Background 0",
                      "ja", "$EB0808.w 0 バックグラウンド 0");
    }
    @Override public void smiControlled () {
      SpriteScreen.sprReg4BgCtrlMask = SpriteScreen.sprReg4BgCtrlMask & ~1 | smiMaskValue & 1;
      SpriteScreen.sprReg4BgCtrlTest = SpriteScreen.sprReg4BgCtrlTest & ~1 | smiTestValue;
      int curr = SpriteScreen.sprReg4BgCtrlPort & ~SpriteScreen.sprReg4BgCtrlMask | SpriteScreen.sprReg4BgCtrlTest & SpriteScreen.sprReg4BgCtrlMask;
      if (SpriteScreen.sprReg4BgCtrlCurr != curr) {
        SpriteScreen.sprReg4BgCtrlCurr = curr;
        if (VideoController.vcnReg3Curr << 31 - 6 < 0 && SpriteScreen.sprReg4BgCtrlCurr << 31 - 9 < 0) {  //スプライト画面が表示されている
          CRTC.crtAllStamp += 2;
          if (XEiJ.mpuTask == null) {  //停止中
            CRTC.crtRepaint ();
          }
        }
      }
    }
  }  //class SMTItem_SprB0On

  public static class SMTItem_SprHFEd extends SMTItem {
    public SMTItem_SprHFEd () {
      super ("HFEd", SpriteScreen.sprReg5HFrontEndPort, 8);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$EB080A.w 7-0 End character number of horizontal front porch",
                      "ja", "$EB080A.w 7-0 水平フロントポーチの終了キャラクタ番号");
    }
    @Override public void smiControlled () {
      SpriteScreen.sprReg5HFrontEndMask = smiMaskValue;
      SpriteScreen.sprReg5HFrontEndTest = smiTestValue;
      int curr = SpriteScreen.sprReg5HFrontEndMask == 0 ? SpriteScreen.sprReg5HFrontEndPort : SpriteScreen.sprReg5HFrontEndTest;
      if (SpriteScreen.sprReg5HFrontEndCurr != curr) {
        SpriteScreen.sprReg5HFrontEndCurr = curr;
        if (VideoController.vcnReg3Curr << 31 - 6 < 0 && SpriteScreen.sprReg4BgCtrlCurr << 31 - 9 < 0) {  //スプライト画面が表示されている
          CRTC.crtAllStamp += 2;
          if (XEiJ.mpuTask == null) {  //停止中
            CRTC.crtRepaint ();
          }
        }
      }
    }
  }  //class SMTItem_SprHFEd

  public static class SMTItem_SprHBEd extends SMTItem {
    public SMTItem_SprHBEd () {
      super ("HBEd", SpriteScreen.sprReg6HBackEndPort, 6);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$EB080C.w 5-0 End character number of horizontal back porch",
                      "ja", "$EB080C.w 5-0 水平バックポーチの終了キャラクタ番号");
    }
    @Override public void smiControlled () {
      SpriteScreen.sprReg6HBackEndMask = smiMaskValue;
      SpriteScreen.sprReg6HBackEndTest = smiTestValue;
      int curr = SpriteScreen.sprReg6HBackEndMask == 0 ? SpriteScreen.sprReg6HBackEndPort : SpriteScreen.sprReg6HBackEndTest;
      if (SpriteScreen.sprReg6HBackEndCurr != curr) {
        SpriteScreen.sprReg6HBackEndCurr = curr;
        if (VideoController.vcnReg3Curr << 31 - 6 < 0 && SpriteScreen.sprReg4BgCtrlCurr << 31 - 9 < 0) {  //スプライト画面が表示されている
          CRTC.crtAllStamp += 2;
          if (XEiJ.mpuTask == null) {  //停止中
            CRTC.crtRepaint ();
          }
        }
      }
    }
  }  //class SMTItem_SprHBEd

  public static class SMTItem_SprVBEd extends SMTItem {
    public SMTItem_SprVBEd () {
      super ("VBEd", SpriteScreen.sprReg7VBackEndPort, 8);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$EB080E.w 7-0 End raster number of vertical back porch",
                      "ja", "$EB080E.w 7-0 垂直バックポーチの終了ラスタ番号");
    }
    @Override public void smiControlled () {
      SpriteScreen.sprReg7VBackEndMask = smiMaskValue;
      SpriteScreen.sprReg7VBackEndTest = smiTestValue;
      int curr = SpriteScreen.sprReg7VBackEndMask == 0 ? SpriteScreen.sprReg7VBackEndPort : SpriteScreen.sprReg7VBackEndTest;
      if (SpriteScreen.sprReg7VBackEndCurr != curr) {
        SpriteScreen.sprReg7VBackEndCurr = curr;
        if (VideoController.vcnReg3Curr << 31 - 6 < 0 && SpriteScreen.sprReg4BgCtrlCurr << 31 - 9 < 0) {  //スプライト画面が表示されている
          CRTC.crtAllStamp += 2;
          if (XEiJ.mpuTask == null) {  //停止中
            CRTC.crtRepaint ();
          }
        }
      }
    }
  }  //class SMTItem_SprVBEd

  public static class SMTItem_SprHigh extends SMTItem {
    public SMTItem_SprHigh () {
      super ("High", SpriteScreen.sprReg8ResoPort >> 4, 1);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$EB0810.w 4 High-resolution",
                      "ja", "$EB0810.w 4 高解像度");
    }
    @Override public void smiControlled () {
      SpriteScreen.sprReg8ResoMask = SpriteScreen.sprReg8ResoMask & ~(1 << 4) | smiMaskValue & 1 << 4;
      SpriteScreen.sprReg8ResoTest = SpriteScreen.sprReg8ResoTest & ~(1 << 4) | smiTestValue     << 4;
      int curr = SpriteScreen.sprReg8ResoPort & ~SpriteScreen.sprReg8ResoMask | SpriteScreen.sprReg8ResoTest & SpriteScreen.sprReg8ResoMask;
      if (SpriteScreen.sprReg8ResoCurr != curr) {
        SpriteScreen.sprReg8ResoCurr = curr;
        if (VideoController.vcnReg3Curr << 31 - 6 < 0 && SpriteScreen.sprReg4BgCtrlCurr << 31 - 9 < 0) {  //スプライト画面が表示されている
          CRTC.crtAllStamp += 2;
          if (XEiJ.mpuTask == null) {  //停止中
            CRTC.crtRepaint ();
          }
        }
      }
    }
  }  //class SMTItem_SprHigh

  public static class SMTItem_SprVRes extends SMTItem {
    public SMTItem_SprVRes () {
      super ("VRes", SpriteScreen.sprReg8ResoPort >> 2, 2);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$EB0810.w 3-2 Vertical resolution",
                      "ja", "$EB0810.w 3-2 垂直解像度");
    }
    @Override public void smiControlled () {
      SpriteScreen.sprReg8ResoMask = SpriteScreen.sprReg8ResoMask & ~(3 << 2) | smiMaskValue & 3 << 2;
      SpriteScreen.sprReg8ResoTest = SpriteScreen.sprReg8ResoTest & ~(3 << 2) | smiTestValue     << 2;
      int curr = SpriteScreen.sprReg8ResoPort & ~SpriteScreen.sprReg8ResoMask | SpriteScreen.sprReg8ResoTest & SpriteScreen.sprReg8ResoMask;
      if (SpriteScreen.sprReg8ResoCurr != curr) {
        SpriteScreen.sprReg8ResoCurr = curr;
        if (VideoController.vcnReg3Curr << 31 - 6 < 0 && SpriteScreen.sprReg4BgCtrlCurr << 31 - 9 < 0) {  //スプライト画面が表示されている
          CRTC.crtAllStamp += 2;
          if (XEiJ.mpuTask == null) {  //停止中
            CRTC.crtRepaint ();
          }
        }
      }
    }
  }  //class SMTItem_SprVRes

  public static class SMTItem_SprHRes extends SMTItem {
    public SMTItem_SprHRes () {
      super ("HRes", SpriteScreen.sprReg8ResoPort, 2);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$EB0810.w 1-0 Horizontal resolution",
                      "ja", "$EB0810.w 1-0 水平解像度");
    }
    @Override public void smiControlled () {
      SpriteScreen.sprReg8ResoMask = SpriteScreen.sprReg8ResoMask & ~3 | smiMaskValue & 3;
      SpriteScreen.sprReg8ResoTest = SpriteScreen.sprReg8ResoTest & ~3 | smiTestValue;
      int curr = SpriteScreen.sprReg8ResoPort & ~SpriteScreen.sprReg8ResoMask | SpriteScreen.sprReg8ResoTest & SpriteScreen.sprReg8ResoMask;
      if (SpriteScreen.sprReg8ResoCurr != curr) {
        SpriteScreen.sprReg8ResoCurr = curr;
        if (VideoController.vcnReg3Curr << 31 - 6 < 0 && SpriteScreen.sprReg4BgCtrlCurr << 31 - 9 < 0) {  //スプライト画面が表示されている
          CRTC.crtAllStamp += 2;
          if (XEiJ.mpuTask == null) {  //停止中
            CRTC.crtRepaint ();
          }
        }
      }
    }
  }  //class SMTItem_SprHRes


  //----------------------------------------------------------------------------------------
  //システムポート

  public static class SMTItem_SysHRL extends SMTItem {
    public SMTItem_SysHRL () {
      super ("HRL", CRTC.crtHRLPort, 1);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E8E007.b 1 HRL (dot clock selection)",
                      "ja", "$E8E007.b 1 HRL (ドットクロック選択)");
    }
    @Override public void smiControlled () {
      CRTC.crtHRLMask = smiMaskValue;
      CRTC.crtHRLTest = smiTestValue;
      int curr = CRTC.crtHRLMask == 0 ? CRTC.crtHRLPort : CRTC.crtHRLTest;
      if (CRTC.crtHRLCurr != curr) {
        CRTC.crtHRLCurr = curr;
        if (XEiJ.mpuTask != null) {  //動作中
          CRTC.crtRestart ();
        }
      }
    }
  }  //class SMTItem_SysHRL

  public static class SMTItem_SysCont extends SMTItem {
    public SMTItem_SysCont () {
      super ("Cont", VideoController.vcnTargetContrastPort, 4);
      Multilingual.mlnToolTipText (smiNameLabel,
                      "en", "$E8E001.b 3-0 Contrast",
                      "ja", "$E8E001.b 3-0 コントラスト");
    }
    @Override public void smiControlled () {
      VideoController.vcnTargetContrastMask = smiMaskValue;
      VideoController.vcnTargetContrastTest = smiTestValue;
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
    }
  }  //class SMTItem_SysCont



  //========================================================================================
  //$$SMG 表示モードテストのアイテムグループ
  //  コピーボタン
  //    ポートデータをテストデータにまとめてコピーする
  //  ポートラジオボタン
  //    まとめてポートを選択する
  //  テストラジオボタン
  //    まとめてテストを選択する
  public static class SMTGroup {

    public SMTItem[] smgItemArray;
    public ButtonGroup smgButtonGroup;
    public Box smgBox;
    public JRadioButton smgPortRadioButton;
    public JRadioButton smgTestRadioButton;

    //  コンストラクタ
    public SMTGroup (SMTItem... itemArray) {
      smgItemArray = itemArray;
      for (SMTItem item : smgItemArray) {
        item.smiItemGroup = this;
      }
      smgButtonGroup = new ButtonGroup ();
      smgBox = ComponentFactory.setFixedSize (
        ComponentFactory.createVerticalBox (
          Box.createVerticalStrut (LnF.lnfFontSize + 4),
          ComponentFactory.createHorizontalBox (
            ComponentFactory.createVerticalBox (
              Box.createVerticalGlue (),
              ComponentFactory.setFixedSize (
                Multilingual.mlnToolTipText (
                  ComponentFactory.createImageButton (
                    XEiJ.createImage (
                      14, 18,
                      "11111111111111" +
                      "1............1" +
                      "1............1" +
                      "1............1" +
                      "1....1111....1" +
                      "1....1..1....1" +
                      "1....1..1....1" +
                      "1....1..1....1" +
                      "1....1..1....1" +
                      "1.1111..1111.1" +
                      "1..1......1..1" +
                      "1...1....1...1" +
                      "1....1..1....1" +
                      "1.....11.....1" +
                      "1............1" +
                      "1............1" +
                      "1............1" +
                      "11111111111111",
                      LnF.lnfRGB[0],
                      LnF.lnfRGB[12]),
                    "Copy port data to test data", new ActionListener () {
                      @Override public void actionPerformed (ActionEvent ae) {
                        smgCopyClicked ();
                      }
                    }),  //ae -> smgCopyClicked ()
                  "ja", "ポートデータをテストデータにコピーする"),
                16, 20),
              Box.createVerticalGlue ()
              ),
            Box.createHorizontalStrut (2),
            ComponentFactory.createVerticalBox (
              smgPortRadioButton = ComponentFactory.setFixedSize (
                Multilingual.mlnToolTipText (
                  ComponentFactory.createRadioButton (smgButtonGroup, smgIsPortSelected (), "", new ActionListener () {
                    @Override public void actionPerformed (ActionEvent ae) {
                      smgPortSelected ();
                    }
                  }),  //ae -> smgPortSelected ()
                  "en", "Reflect port data", "ja", "ポートデータを反映させる"),
                14, LnF.lnfFontSize + 4),
              smgTestRadioButton = ComponentFactory.setFixedSize (
                Multilingual.mlnToolTipText (
                  ComponentFactory.createRadioButton (smgButtonGroup, smgIsTestSelected (), "", new ActionListener () {
                    @Override public void actionPerformed (ActionEvent ae) {
                      smgTestSelected ();
                    }
                  }),  //ae -> smgTestSelected ()
                  "en", "Reflect test data", "ja", "テストデータを反映させる"),
                14, LnF.lnfFontSize + 4)
              )
            )
          ),
        32, (LnF.lnfFontSize + 4) * 3);
    }  //new SMTGroup(SMTItem...)

    //smgIsPortSelected ()
    //  true=すべてPortが選択されている
    public final boolean smgIsPortSelected () {
      for (SMTItem item : smgItemArray) {
        if (item.smiMaskValue != 0) {  //Testが選択されている
          return false;
        }
      }
      return true;
    }  //smgIsPortSelected()

    //smgIsTestSelected ()
    //  true=すべてTestが選択されている
    public final boolean smgIsTestSelected () {
      for (SMTItem item : smgItemArray) {
        if (item.smiMaskValue == 0) {  //Portが選択されている
          return false;
        }
      }
      return true;
    }  //smgIsTestSelected()

    //smgCopyClicked ()
    //  コピーボタンが押された
    //  すべてのアイテムのPortをTestにコピーする
    public void smgCopyClicked () {
      for (SMTItem item : smgItemArray) {
        if (item.smiBitWidth == 1) {
          item.smiTestCheckBox.setSelected (item.smiPortValue != 0);
        } else {
          item.smiTestSpinner.setIntValue (item.smiPortValue);
        }
        item.smiTestChanged ();
      }
    }  //smgCopyClicked()

    //smgPortSelected ()
    //  Portが選択された
    public void smgPortSelected () {
      for (SMTItem item : smgItemArray) {
        item.smiPortRadioButton.setSelected (true);
        item.smiPortSelected ();
      }
    }  //smgPortSelected()

    //smgTestSelected ()
    //  Testが選択された
    public void smgTestSelected () {
      for (SMTItem item : smgItemArray) {
        item.smiTestRadioButton.setSelected (true);
        item.smiTestSelected ();
      }
    }  //smgTestSelected()

    //smgUpdate ()
    //  アイテム毎のPort/Testの選択が変化したときアイテムグループのPort/Testに反映させるためにアイテムが呼び出す
    public void smgUpdate () {
      if (smgIsPortSelected ()) {
        smgPortRadioButton.setSelected (true);
      } else if (smgIsTestSelected ()) {
        smgTestRadioButton.setSelected (true);
      } else {
        smgButtonGroup.clearSelection ();
      }
    }  //smgUpdate()

  }  //class SMTGroup



}  //class ScreenModeTest



