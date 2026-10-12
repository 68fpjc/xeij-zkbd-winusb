//========================================================================================
//  RegisterList.java
//    en:Register list
//    ja:レジスタリスト
//  Copyright (C) 2003-2024 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//  実機用のデバッガは030モードのレジスタの表示方法が統一されていない
//              11111111112222222222333333333344444444445555555555666666666677777777778
//    012345678901234567890123456789012345678901234567890123456789012345678901234567890
//  DB.X v3.00 (000モード)
//    PC:xxxxxxxx USP:xxxxxxxx SSP:xxxxxxxx SR:xxxx X:b N:b Z:b V:b C:b
//    HI:b LS:b CC:b CS:b NE:b EQ:b VC:b VS:b PL:b MI:b GE:b LT:b GT:b LE:b HS:b LO:b
//    D  xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx  xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx
//    A  xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx  xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx
//  DB.X v3.00 (030モード)
//    PC:xxxxxxxx USP:xxxxxxxx MSP:xxxxxxxx ISP:xxxxxxxx SR:xxxx
//    X:b N:b Z:b V:b C:b SFC:xx DFC:xx CACR:xxxxxxxx CAAR:xxxxxxxx VBR:xxxxxxxx
//    HI:b LS:b CC:b CS:b NE:b EQ:b VC:b VS:b PL:b MI:b GE:b LT:b GT:b LE:b HS:b LO:b
//    D  xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx  xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx
//    A  xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx  xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx
//  SCD.X v3.00 (000モード)
//    PC=xxxxxxxx USP=xxxxxxxx SSP=xxxxxxxx SR=xxxx X:b  N:b  Z:b  V:b  C:b
//    D  xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx  xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx
//    A  xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx  xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx
//  SCD.X v3.00 (030モード)
//    PC=xxxxxxxx USP=xxxxxxxx ISP=xxxxxxxx MSP=xxxxxxxx SR=xxxx X:b  N:b  Z:b  V:b  C:b
//    SFC=x DFC=x VBR=xxxxxxxx CACR=xxxxxxxx CAAR=xxxxxxxx
//    D  xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx  xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx
//    A  xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx  xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx
//----------------------------------------------------------------------------------------

package xeij;

import java.awt.*;  //BasicStroke,BorderLayout,BoxLayout,Color,Component,Container,Cursor,Desktop,Dimension,Font,FlowLayout,Frame,Graphics,Graphics2D,GraphicsDevice,GraphicsEnvironment,GridLayout,Image,Insets,Paint,Point,Rectangle,RenderingHints,Robot,Shape,Stroke,TexturePaint,Toolkit
import java.awt.event.*;  //ActionEvent,ActionListener,ComponentAdapter,ComponentEvent,ComponentListener,FocusAdapter,FocusEvent,FocusListener,InputEvent,KeyAdapter,KeyEvent,KeyListener,MouseAdapter,MouseEvent,MouseListener,MouseMotionAdapter,MouseWheelEvent,WindowAdapter,WindowEvent,WindowListener,WindowStateListener
import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import javax.swing.*;  //AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JApplet,JButton,JCheckBox,JCheckBoxMenuItem,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException

public class RegisterList {

  public static final char[] DRP_BASE_000 = (
    //         11111111112222222222333333333344444444445555555555666666666677777777778
    //12345678901234567890123456789012345678901234567890123456789012345678901234567890
    "PC:xxxxxxxx USP:xxxxxxxx SSP:xxxxxxxx              SR:xxxx X:b N:b Z:b V:b C:b\n" +
    "HI:b LS:b CC(HS):b CS(LO):b NE:b EQ:b VC:b VS:b PL:b MI:b GE:b LT:b GT:b LE:b\n" +  //DRP_1+
    "D  xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx  xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx\n" +  //DRP_2+
    "A  xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx  xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx\n" +  //DRP_3+
    "                                                                            \n" +  //DRP_4+
    "                                                                            \n" +  //DRP_5+
    "                                                                           \n" +  //DRP_6 +
    "                                                                              \n" +  //DRP_7 +
    "                                                                              \n" +  //DRP_8 +
    "                                                                              \n" +  //DRP_9 +
    "                                                                              ").toCharArray ();  //DRP_10+
  public static final char[] DRP_BASE_030 = (
    //         11111111112222222222333333333344444444445555555555666666666677777777778
    //12345678901234567890123456789012345678901234567890123456789012345678901234567890
    "PC:xxxxxxxx USP:xxxxxxxx ISP:xxxxxxxx MSP:xxxxxxxx SR:xxxx X:b N:b Z:b V:b C:b\n" +
    "HI:b LS:b CC(HS):b CS(LO):b NE:b EQ:b VC:b VS:b PL:b MI:b GE:b LT:b GT:b LE:b\n" +  //DRP_1+
    "D  xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx  xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx\n" +  //DRP_2+
    "A  xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx  xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx\n" +  //DRP_3+
    "SFC:x   VBR:xxxxxxxx CACR:xxxxxxxx                                          \n" +  //DRP_4+
    "DFC:x                                                                       \n" +  //DRP_5+
    "FPCR:xxxxxxxx FPSR:xxxxxxxx M:b Z:b I:b N:b B:b S:b E:b O:b U:b D:b X:b P:b\n" +  //DRP_6 +
    "FP0:+x.xxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx  FP1:+x.xxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx\n" +  //DRP_7 +
    "FP2:+x.xxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx  FP3:+x.xxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx\n" +  //DRP_8 +
    "FP4:+x.xxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx  FP5:+x.xxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx\n" +  //DRP_9 +
    "FP6:+x.xxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx  FP7:+x.xxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx").toCharArray ();  //DRP_10+
  public static final char[] DRP_BASE_060 = (
    //         11111111112222222222333333333344444444445555555555666666666677777777778
    //12345678901234567890123456789012345678901234567890123456789012345678901234567890
    "PC:xxxxxxxx USP:xxxxxxxx SSP:xxxxxxxx              SR:xxxx X:b N:b Z:b V:b C:b\n" +
    "HI:b LS:b CC(HS):b CS(LO):b NE:b EQ:b VC:b VS:b PL:b MI:b GE:b LT:b GT:b LE:b\n" +  //DRP_1+
    "D  xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx  xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx\n" +  //DRP_2+
    "A  xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx  xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx\n" +  //DRP_3+
    "SFC:x   VBR:xxxxxxxx CACR:xxxxxxxx  TCR:xxxxxxxx  URP:xxxxxxxx  SRP:xxxxxxxx\n" +  //DRP_4+
    "DFC:x  ITT0:xxxxxxxx ITT1:xxxxxxxx DTT0:xxxxxxxx DTT1:xxxxxxxx  PCR:xxxxxxxx\n" +  //DRP_5+
    "FPCR:xxxxxxxx FPSR:xxxxxxxx M:b Z:b I:b N:b B:b S:b E:b O:b U:b D:b X:b P:b\n" +  //DRP_6 +
    "FP0:+x.xxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx  FP1:+x.xxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx\n" +  //DRP_7 +
    "FP2:+x.xxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx  FP3:+x.xxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx\n" +  //DRP_8 +
    "FP4:+x.xxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx  FP5:+x.xxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx\n" +  //DRP_9 +
    "FP6:+x.xxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx  FP7:+x.xxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx").toCharArray ();  //DRP_10+
  public static final int DRP_1 = 79;  //行の先頭のポジション
  public static final int DRP_2 = DRP_1 + 78;
  public static final int DRP_3 = DRP_2 + 76;
  public static final int DRP_4 = DRP_3 + 76;
  public static final int DRP_5 = DRP_4 + 77;
  public static final int DRP_6 = DRP_5 + 77;
  public static final int DRP_7 = DRP_6 + 76;
  public static final int DRP_8 = DRP_7 + 79;
  public static final int DRP_9 = DRP_8 + 79;
  public static final int DRP_10 = DRP_9 + 79;
  public static final int DRP_END = DRP_10 + 78;  //テキストの長さ。固定
  public static int[] DRP_FP_POS = { DRP_7 + 4, DRP_7 + 44, DRP_8 + 4, DRP_8 + 44, DRP_9 + 4, DRP_9 + 44, DRP_10 + 4, DRP_10 + 44 };
  public static char[] drpCurrentBase;

  public static JFrame drpFrame;  //ウインドウ
  public static ScrollTextArea drpBoard;  //スクロールテキストエリア
  public static JTextArea drpTextArea;  //テキストエリア

  //drpInit ()
  //  初期化
  public static void drpInit () {

    drpFrame = null;

  }  //drpInit()

  //drpStart ()
  public static void drpStart () {
    if (RestorableFrame.rfmGetOpened (Settings.SGS_DRP_FRAME_KEY)) {
      drpOpen ();
    }
  }  //drpStart()

  //drpOpen ()
  //  レジスタウインドウを開く
  //  既に開いているときは手前に持ってくる
  public static void drpOpen () {
    if (drpFrame == null) {
      drpMake ();
    }
    drpUpdate ();
    XEiJ.pnlExitFullScreen (false);
    drpFrame.setVisible (true);
    XEiJ.dbgVisibleMask |= XEiJ.DBG_DRP_VISIBLE_MASK;
  }  //drpOpen()

  //drpMake ()
  //  レジスタウインドウを作る
  public static void drpMake () {

    //スクロールテキストエリア
    drpBoard = ComponentFactory.setPreferredSize (
      ComponentFactory.setFont (new ScrollTextArea (), LnF.lnfMonospacedFont),
      500, 8 + 17 * 11);
    drpBoard.setMargin (new Insets (2, 4, 2, 4));  //グリッドを更新させるためJTextAreaではなくScrollTextAreaに設定する必要がある
    drpTextArea = drpBoard.getTextArea ();
    drpTextArea.setEditable (false);
    drpSetMPU ();

    //テキストエリアのマウスリスナー
    drpTextArea.addMouseListener (new MouseAdapter () {
      @Override public void mousePressed (MouseEvent me) {
        if (XEiJ.mpuTask == null && me.isPopupTrigger ()) {
          XEiJ.dbgShowPopup (me, drpTextArea, false);
        }
      }
      @Override public void mouseReleased (MouseEvent me) {
        if (XEiJ.mpuTask == null && me.isPopupTrigger ()) {
          XEiJ.dbgShowPopup (me, drpTextArea, false);
        }
      }
    });

    //ウインドウ
    drpFrame = Multilingual.mlnTitle (
      ComponentFactory.createRestorableSubFrame (
        Settings.SGS_DRP_FRAME_KEY,
        "Register list",
        null,
        ComponentFactory.createBorderPanel (
          drpBoard,
          ComponentFactory.createHorizontalBox (
            Box.createHorizontalGlue (),
            ComponentFactory.createFlowPanel (
              FlowLayout.RIGHT,
              XEiJ.mpuMakeOriIllegalCheckBox (),  //ORI.B #$00,D0を不当命令とみなすチェックボックス
              XEiJ.mpuMakeStopOnErrorCheckBox (),  //エラーで停止するチェックボックス
              XEiJ.mpuMakeStopAtStartCheckBox (),  //実行開始位置で停止するチェックボックス
              Box.createHorizontalStrut (12),
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
          )
        ),
      "ja", "レジスタリスト");
    ComponentFactory.addListener (
      drpFrame,
      new WindowAdapter () {
        @Override public void windowClosing (WindowEvent we) {
          XEiJ.dbgVisibleMask &= ~XEiJ.DBG_DRP_VISIBLE_MASK;
        }
      });

  }  //drpMake()

  //drpSetMPU ()
  //  レジスタウインドウのMPUを設定する
  public static void drpSetMPU () {
    drpCurrentBase = (XEiJ.currentMPU < Model.MPU_MC68020 ? DRP_BASE_000 :
                      XEiJ.currentMPU < Model.MPU_MC68LC040 ? DRP_BASE_030 :
                      DRP_BASE_060);
    if (drpTextArea != null) {
      drpTextArea.setText (String.valueOf (drpCurrentBase));
    }
  }  //drpSetMPU()

  //drpUpdate ()
  //  レジスタウインドウを更新する
  //            1111111111222222222233333333334444444444555555555566666666667777777777
  //  01234567890123456789012345678901234567890123456789012345678901234567890123456789
  //  PC:xxxxxxxx USP:xxxxxxxx ISP:xxxxxxxx MSP:xxxxxxxx SR:xxxx X:b N:b Z:b V:b C:b
  //  HI:b LS:b CC(HS):b CS(LO):b NE:b EQ:b VC:b VS:b PL:b MI:b GE:b LT:b GT:b LE:b   DRP_1+
  //  D  xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx  xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx     DRP_2+
  //  A  xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx  xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx     DRP_3+
  //  SFC:x   VBR:xxxxxxxx CACR:xxxxxxxx  TCR:xxxxxxxx  URP:xxxxxxxx  SRP:xxxxxxxx    DRP_4+
  //  DFC:x  ITT0:xxxxxxxx ITT1:xxxxxxxx DTT0:xxxxxxxx DTT1:xxxxxxxx  PCR:xxxxxxxx    DRP_5+
  //  FPCR:xxxxxxxx FPSR:xxxxxxxx M:b Z:b I:b N:b B:b S:b E:b O:b U:b D:b X:b P:b     DRP_6+
  //  FP0:+x.xxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx  FP1:+x.xxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx  DRP_7+
  //  FP2:+x.xxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx  FP3:+x.xxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx  DRP_8+
  //  FP4:+x.xxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx  FP5:+x.xxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx  DRP_9+
  //  FP6:+x.xxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx  FP7:+x.xxxxxxxxxxxxxxxxxxxxxxxxe+xxxxx  DRP_10+
  public static void drpUpdate () {
    XEiJ.fmtHex8 (drpCurrentBase,  3, XEiJ.regPC);  //PC
    XEiJ.fmtHex8 (drpCurrentBase, 16, XEiJ.regSRS != 0 ? XEiJ.mpuUSP : XEiJ.regRn[15]);  //USP
    if (XEiJ.currentMPU < Model.MPU_MC68020) {  //000
      XEiJ.fmtHex8 (drpCurrentBase, 29, XEiJ.regSRS != 0 ? XEiJ.regRn[15] : XEiJ.mpuISP);  //SSP
      XEiJ.fmtHex4 (drpCurrentBase, 54, XEiJ.regSRT1 | XEiJ.regSRS | XEiJ.regSRI | XEiJ.regCCR);  //SR
    } else if (XEiJ.currentMPU < Model.MPU_MC68LC040) {  //030
      XEiJ.fmtHex8 (drpCurrentBase, 29, XEiJ.regSRS == 0 || XEiJ.regSRM != 0 ? XEiJ.mpuISP : XEiJ.regRn[15]);  //ISP
      XEiJ.fmtHex8 (drpCurrentBase, 42, XEiJ.regSRS == 0 || XEiJ.regSRM == 0 ? XEiJ.mpuMSP : XEiJ.regRn[15]);  //MSP
      XEiJ.fmtHex4 (drpCurrentBase, 54, XEiJ.regSRT1 | XEiJ.regSRT0 | XEiJ.regSRS | XEiJ.regSRM | XEiJ.regSRI | XEiJ.regCCR);  //SR
    } else {  //060
      XEiJ.fmtHex8 (drpCurrentBase, 29, XEiJ.regSRS != 0 ? XEiJ.regRn[15] : XEiJ.mpuISP);  //SSP
      XEiJ.fmtHex4 (drpCurrentBase, 54, XEiJ.regSRT1 | XEiJ.regSRS | XEiJ.regSRM | XEiJ.regSRI | XEiJ.regCCR);  //SR
    }
    if (true) {
      drpCurrentBase[61] = XEiJ.REG_CCRXMAP[XEiJ.regCCR];  //X
      drpCurrentBase[65] = XEiJ.REG_CCRNMAP[XEiJ.regCCR];  //N
      drpCurrentBase[69] = XEiJ.REG_CCRZMAP[XEiJ.regCCR];  //Z
      drpCurrentBase[73] = XEiJ.REG_CCRVMAP[XEiJ.regCCR];  //V
      drpCurrentBase[77] = XEiJ.REG_CCRCMAP[XEiJ.regCCR];  //C
    } else {
      drpCurrentBase[61] = (char) ('0' + (XEiJ.regCCR >> 4 & 1));  //X
      drpCurrentBase[65] = (char) ('0' + (XEiJ.regCCR >> 3 & 1));  //N
      drpCurrentBase[69] = (char) ('0' + (XEiJ.regCCR >> 2 & 1));  //Z
      drpCurrentBase[73] = (char) ('0' + (XEiJ.regCCR >> 1 & 1));  //V
      drpCurrentBase[77] = (char) ('0' + (XEiJ.regCCR      & 1));  //C
    }
    drpCurrentBase[DRP_1 +  3] = XEiJ.MPU_CCCMAP[ 2 << 5 | XEiJ.regCCR];  //HI
    drpCurrentBase[DRP_1 +  8] = XEiJ.MPU_CCCMAP[ 3 << 5 | XEiJ.regCCR];  //LS
    drpCurrentBase[DRP_1 + 17] = XEiJ.MPU_CCCMAP[ 4 << 5 | XEiJ.regCCR];  //CC(HS)
    drpCurrentBase[DRP_1 + 26] = XEiJ.MPU_CCCMAP[ 5 << 5 | XEiJ.regCCR];  //CS(LO)
    drpCurrentBase[DRP_1 + 31] = XEiJ.MPU_CCCMAP[ 6 << 5 | XEiJ.regCCR];  //NE
    drpCurrentBase[DRP_1 + 36] = XEiJ.MPU_CCCMAP[ 7 << 5 | XEiJ.regCCR];  //EQ
    drpCurrentBase[DRP_1 + 41] = XEiJ.MPU_CCCMAP[ 8 << 5 | XEiJ.regCCR];  //VC
    drpCurrentBase[DRP_1 + 46] = XEiJ.MPU_CCCMAP[ 9 << 5 | XEiJ.regCCR];  //VS
    drpCurrentBase[DRP_1 + 51] = XEiJ.MPU_CCCMAP[10 << 5 | XEiJ.regCCR];  //PL
    drpCurrentBase[DRP_1 + 56] = XEiJ.MPU_CCCMAP[11 << 5 | XEiJ.regCCR];  //MI
    drpCurrentBase[DRP_1 + 61] = XEiJ.MPU_CCCMAP[12 << 5 | XEiJ.regCCR];  //GE
    drpCurrentBase[DRP_1 + 66] = XEiJ.MPU_CCCMAP[13 << 5 | XEiJ.regCCR];  //LT
    drpCurrentBase[DRP_1 + 71] = XEiJ.MPU_CCCMAP[14 << 5 | XEiJ.regCCR];  //GT
    drpCurrentBase[DRP_1 + 76] = XEiJ.MPU_CCCMAP[15 << 5 | XEiJ.regCCR];  //LE
    XEiJ.fmtHex8 (drpCurrentBase, DRP_2 +  3, XEiJ.regRn[ 0]);  //D0
    XEiJ.fmtHex8 (drpCurrentBase, DRP_2 + 12, XEiJ.regRn[ 1]);  //D1
    XEiJ.fmtHex8 (drpCurrentBase, DRP_2 + 21, XEiJ.regRn[ 2]);  //D2
    XEiJ.fmtHex8 (drpCurrentBase, DRP_2 + 30, XEiJ.regRn[ 3]);  //D3
    XEiJ.fmtHex8 (drpCurrentBase, DRP_2 + 40, XEiJ.regRn[ 4]);  //D4
    XEiJ.fmtHex8 (drpCurrentBase, DRP_2 + 49, XEiJ.regRn[ 5]);  //D5
    XEiJ.fmtHex8 (drpCurrentBase, DRP_2 + 58, XEiJ.regRn[ 6]);  //D6
    XEiJ.fmtHex8 (drpCurrentBase, DRP_2 + 67, XEiJ.regRn[ 7]);  //D7
    XEiJ.fmtHex8 (drpCurrentBase, DRP_3 +  3, XEiJ.regRn[ 8]);  //A0
    XEiJ.fmtHex8 (drpCurrentBase, DRP_3 + 12, XEiJ.regRn[ 9]);  //A1
    XEiJ.fmtHex8 (drpCurrentBase, DRP_3 + 21, XEiJ.regRn[10]);  //A2
    XEiJ.fmtHex8 (drpCurrentBase, DRP_3 + 30, XEiJ.regRn[11]);  //A3
    XEiJ.fmtHex8 (drpCurrentBase, DRP_3 + 40, XEiJ.regRn[12]);  //A4
    XEiJ.fmtHex8 (drpCurrentBase, DRP_3 + 49, XEiJ.regRn[13]);  //A5
    XEiJ.fmtHex8 (drpCurrentBase, DRP_3 + 58, XEiJ.regRn[14]);  //A6
    XEiJ.fmtHex8 (drpCurrentBase, DRP_3 + 67, XEiJ.regRn[15]);  //A7
    if (Model.MPU_MC68020 <= XEiJ.currentMPU) {
      drpCurrentBase[DRP_4 + 4] = (char) ('0' + XEiJ.mpuSFC);  //SFC
      drpCurrentBase[DRP_5 + 4] = (char) ('0' + XEiJ.mpuDFC);  //DFC
      XEiJ.fmtHex8 (drpCurrentBase, DRP_4 + 12, XEiJ.mpuVBR);  //VBR
      XEiJ.fmtHex8 (drpCurrentBase, DRP_4 + 26, XEiJ.mpuCACR);  //CACR
      if (Model.MPU_MC68LC040 <= XEiJ.currentMPU) {
        XEiJ.fmtHex8 (drpCurrentBase, DRP_4 + 40, MC68060.mmuTCR);  //TCR
        XEiJ.fmtHex8 (drpCurrentBase, DRP_4 + 54, MC68060.mmuURP);  //URP
        XEiJ.fmtHex8 (drpCurrentBase, DRP_4 + 68, MC68060.mmuSRP);  //SRP
        XEiJ.fmtHex8 (drpCurrentBase, DRP_5 + 12, MC68060.mmuITT0);  //ITT0
        XEiJ.fmtHex8 (drpCurrentBase, DRP_5 + 26, MC68060.mmuITT1);  //ITT1
        XEiJ.fmtHex8 (drpCurrentBase, DRP_5 + 40, MC68060.mmuDTT0);  //DTT0
        XEiJ.fmtHex8 (drpCurrentBase, DRP_5 + 54, MC68060.mmuDTT1);  //DTT1
        XEiJ.fmtHex8 (drpCurrentBase, DRP_5 + 68, XEiJ.mpuPCR);  //PCR
      }
      XEiJ.fmtHex8 (drpCurrentBase, DRP_6 +  5, XEiJ.fpuBox.epbFpcr);  //FPCR
      XEiJ.fmtHex8 (drpCurrentBase, DRP_6 + 19, XEiJ.fpuBox.epbFpsr);  //FPSR
      drpCurrentBase[DRP_6 + 30] = (char) ('0' + (XEiJ.fpuBox.epbFpsr >> 27 & 1));  //FPSR M
      drpCurrentBase[DRP_6 + 34] = (char) ('0' + (XEiJ.fpuBox.epbFpsr >> 26 & 1));  //FPSR Z
      drpCurrentBase[DRP_6 + 38] = (char) ('0' + (XEiJ.fpuBox.epbFpsr >> 25 & 1));  //FPSR I
      drpCurrentBase[DRP_6 + 42] = (char) ('0' + (XEiJ.fpuBox.epbFpsr >> 24 & 1));  //FPSR N
      drpCurrentBase[DRP_6 + 46] = (char) ('0' + (XEiJ.fpuBox.epbFpsr >> 15 & 1));  //FPSR BSUN
      drpCurrentBase[DRP_6 + 50] = (char) ('0' + (XEiJ.fpuBox.epbFpsr >> 14 & 1));  //FPSR SNAN
      drpCurrentBase[DRP_6 + 54] = (char) ('0' + (XEiJ.fpuBox.epbFpsr >> 13 & 1));  //FPSR OPERR
      drpCurrentBase[DRP_6 + 58] = (char) ('0' + (XEiJ.fpuBox.epbFpsr >> 12 & 1));  //FPSR OVFL
      drpCurrentBase[DRP_6 + 62] = (char) ('0' + (XEiJ.fpuBox.epbFpsr >> 11 & 1));  //FPSR UNFL
      drpCurrentBase[DRP_6 + 66] = (char) ('0' + (XEiJ.fpuBox.epbFpsr >> 10 & 1));  //FPSR DZ
      drpCurrentBase[DRP_6 + 70] = (char) ('0' + (XEiJ.fpuBox.epbFpsr >>  9 & 1));  //FPSR INEX2
      drpCurrentBase[DRP_6 + 74] = (char) ('0' + (XEiJ.fpuBox.epbFpsr >>  8 & 1));  //FPSR INEX1
      for (int n = 0; n <= 7; n++) {
        int pos = DRP_FP_POS[n];
        String s = XEiJ.fpuFPn[n].toString ();
        int i = 0;
        int l = s.length ();
        for (; i < 34 && i < l; i++) {
          drpCurrentBase[pos + i] = s.charAt (i);
        }
        for (; i < 34; i++) {
          drpCurrentBase[pos + i] = ' ';
        }
      }
    }
    SwingUtilities.invokeLater (new Runnable () {
      public void run () {
        //複数回に分けても改行を含まないreplaceRangeが一番速い
        drpTextArea.replaceRange (String.valueOf (drpCurrentBase, 3, DRP_1 - 1 - 3), 3, DRP_1 - 1);
        drpTextArea.replaceRange (String.valueOf (drpCurrentBase, DRP_1 + 3, DRP_2 - 1 - (DRP_1 + 3)), DRP_1 + 3, DRP_2 - 1);
        drpTextArea.replaceRange (String.valueOf (drpCurrentBase, DRP_2 + 3, DRP_3 - 1 - (DRP_2 + 3)), DRP_2 + 3, DRP_3 - 1);
        drpTextArea.replaceRange (String.valueOf (drpCurrentBase, DRP_3 + 3, DRP_4 - 1 - (DRP_3 + 3)), DRP_3 + 3, DRP_4 - 1);
        drpTextArea.replaceRange (String.valueOf (drpCurrentBase, DRP_4 + 4, DRP_5 - 1 - (DRP_4 + 4)), DRP_4 + 4, DRP_5 - 1);
        drpTextArea.replaceRange (String.valueOf (drpCurrentBase, DRP_5 + 4, DRP_6 - 1 - (DRP_5 + 4)), DRP_5 + 4, DRP_6 - 1);
        drpTextArea.replaceRange (String.valueOf (drpCurrentBase, DRP_6 + 5, DRP_7 - 1 - (DRP_6 + 5)), DRP_6 + 5, DRP_7 - 1);
        drpTextArea.replaceRange (String.valueOf (drpCurrentBase, DRP_7 + 4, DRP_8 - 1 - (DRP_7 + 4)), DRP_7 + 4, DRP_8 - 1);
        drpTextArea.replaceRange (String.valueOf (drpCurrentBase, DRP_8 + 4, DRP_9 - 1 - (DRP_8 + 4)), DRP_8 + 4, DRP_9 - 1);
        drpTextArea.replaceRange (String.valueOf (drpCurrentBase, DRP_9 + 4, DRP_10 - 1 - (DRP_9 + 4)), DRP_9 + 4, DRP_10 - 1);
        drpTextArea.replaceRange (String.valueOf (drpCurrentBase, DRP_10 + 4, DRP_END - (DRP_10 + 4)), DRP_10 + 4, DRP_END);
      }
    });
  }  //drpUpdate()

}  //class RegisterList



