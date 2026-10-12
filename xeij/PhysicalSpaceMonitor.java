//========================================================================================
//  PhysicalSpaceMonitor.java
//    en:Physical space monitor
//    ja:物理空間モニタ
//  Copyright (C) 2003-2024 Makoto Kamada
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
import javax.swing.*;  //AbstractButton,AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JApplet,JButton,JCheckBox,JCheckBoxMenuItem,JComponent,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException

public class PhysicalSpaceMonitor {

  public static JFrame paaFrame;  //ウインドウ
  public static ScrollTextArea paaBoard;  //スクロールテキストエリア
  public static JTextArea paaTextArea;  //テキストエリア

  //paaInit ()
  //  初期化
  public static void paaInit () {
    paaFrame = null;
  }  //paaInit()

  //paaStart ()
  public static void paaStart () {
    if (RestorableFrame.rfmGetOpened (Settings.SGS_PAA_FRAME_KEY)) {
      paaOpen ();
    }
  }  //paaStart()

  //paaOpen ()
  //  物理アドレス空間ウインドウを開く
  //  既に開いているときは手前に持ってくる
  public static void paaOpen () {
    if (paaFrame == null) {
      paaMake ();
    }
    paaUpdate ();
    XEiJ.pnlExitFullScreen (false);
    paaFrame.setVisible (true);
    XEiJ.dbgVisibleMask |= XEiJ.DBG_PAA_VISIBLE_MASK;
  }  //paaOpen()

  //paaMake ()
  //  アドレス変換ウインドウを作る
  public static void paaMake () {

    //スクロールテキストエリア
    paaBoard = ComponentFactory.setPreferredSize (
      ComponentFactory.setFont (new ScrollTextArea (), LnF.lnfMonospacedFont),
      550, 300);
    paaBoard.setMargin (new Insets (2, 4, 2, 4));
    paaBoard.setHighlightCursorOn (true);
    paaTextArea = paaBoard.getTextArea ();
    paaTextArea.setEditable (false);

    //テキストエリアのマウスリスナー
    ComponentFactory.addListener (
      paaTextArea,
      new MouseAdapter () {
        @Override public void mousePressed (MouseEvent me) {
          if (XEiJ.mpuTask == null && me.isPopupTrigger ()) {
            XEiJ.dbgShowPopup (me, paaTextArea, false);
          }
        }
        @Override public void mouseReleased (MouseEvent me) {
          if (XEiJ.mpuTask == null && me.isPopupTrigger ()) {
            XEiJ.dbgShowPopup (me, paaTextArea, false);
          }
        }
      });

    //ボタンのアクションリスナー
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        switch (ae.getActionCommand ()) {
        case "Update":
          paaUpdate ();
          break;
        }
      }
    };

    //ウインドウ
    paaFrame = Multilingual.mlnTitle (
      ComponentFactory.createRestorableSubFrame (
        Settings.SGS_PAA_FRAME_KEY,
        "Physical space monitor",
        null,
        ComponentFactory.createBorderPanel (
          paaBoard,
          ComponentFactory.createHorizontalBox (
            Multilingual.mlnToolTipText (
              ComponentFactory.createImageButton (
                XEiJ.createImage (
                  20, 14,
                  "11111111111111111111" +
                  "1..................1" +
                  "1.......1111.......1" +
                  "1......111111.1....1" +
                  "1.....11....111....1" +
                  "1....11.....111....1" +
                  "1....11....1111....1" +
                  "1....11............1" +
                  "1....11............1" +
                  "1.....11....11.....1" +
                  "1......111111......1" +
                  "1.......1111.......1" +
                  "1..................1" +
                  "11111111111111111111",
                  LnF.lnfRGB[0],
                  LnF.lnfRGB[12]),
                "Update", listener),
              "ja", "更新"),
            Box.createHorizontalGlue ()
            )
          )
        ),
      "ja", "物理空間モニタ");
    ComponentFactory.addListener (
      paaFrame,
      new WindowAdapter () {
        @Override public void windowClosing (WindowEvent we) {
          XEiJ.dbgVisibleMask &= ~XEiJ.DBG_PAA_VISIBLE_MASK;
        }
      });

  }  //paaMake()

  //paaUpdate ()
  //  物理アドレス空間ウインドウを更新する
  public static void paaUpdate () {
    StringBuilder sb = new StringBuilder ();
    sb.append (Multilingual.mlnJapanese ?
               //xxxxxxx-xxxxxxxx  xxxxxxxxxxxxxxxxxxxxxxxxxx
               "  物理アドレス       メモリマップトデバイス\n" :
               "Physical address      Memory mapped device\n");
    int a0 = 0;
    MemoryMappedDevice mmd0Super = MemoryMappedDevice.MMD_NUL;
    MemoryMappedDevice mmd0User = MemoryMappedDevice.MMD_NUL;
    int page = 0;
    for (; page < XEiJ.BUS_PAGE_COUNT; page++) {
      MemoryMappedDevice mmd1Super = XEiJ.busSuperMap[page];
      MemoryMappedDevice mmd1User = XEiJ.busUserMap[page];
      if (mmd0Super != mmd1Super || mmd0User != mmd1User) {
        int a1 = page << XEiJ.BUS_PAGE_BITS;
        if (mmd0Super != MemoryMappedDevice.MMD_NUL) {
          XEiJ.fmtHex8 (XEiJ.fmtHex8 (sb, a0).append ('-'), a1 - 1).append ("  ").append (mmd0Super.toString ());
          if (mmd0User == MemoryMappedDevice.MMD_NUL) {
            sb.append (Multilingual.mlnJapanese ? " [スーパーバイザ]" : " [Supervisor]");
          }
          sb.append ('\n');
        }
        a0 = a1;
        mmd0Super = mmd1Super;
        mmd0User = mmd1User;
      }
    }
    if (mmd0Super != MemoryMappedDevice.MMD_NUL) {
      XEiJ.fmtHex8 (XEiJ.fmtHex8 (sb, a0).append ('-'), 0 - 1).append ("  ").append (mmd0Super.toString ());
      if (mmd0User == MemoryMappedDevice.MMD_NUL) {
        sb.append (Multilingual.mlnJapanese ? " [スーパーバイザ]" : " [Supervisor]");
      }
      sb.append ('\n');
    }
    paaTextArea.setText (sb.toString ());
    paaTextArea.setCaretPosition (0);
  }  //paaUpdate()

}  //class PhysicalSpaceMonitor



