//========================================================================================
//  LogicalSpaceMonitor.java
//    en:Logical space monitor
//    ja:論理空間モニタ
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

public class LogicalSpaceMonitor {

  public static JFrame atwFrame;  //ウインドウ
  public static final ScrollTextArea[] atwBoard = new ScrollTextArea[2];  //スクロールテキストエリア
  public static final JTextArea[] atwTextArea = new JTextArea[2];  //テキストエリア

  //atwInit ()
  //  初期化
  public static void atwInit () {
    atwFrame = null;
  }  //atwInit()

  //atwStart ()
  public static void atwStart () {
    if (RestorableFrame.rfmGetOpened (Settings.SGS_ATW_FRAME_KEY)) {
      atwOpen ();
    }
  }  //atwStart()

  //atwOpen ()
  //  アドレス変換ウインドウを開く
  //  既に開いているときは手前に持ってくる
  public static void atwOpen () {
    if (atwFrame == null) {
      atwMake ();
    }
    atwUpdate ();
    XEiJ.pnlExitFullScreen (false);
    atwFrame.setVisible (true);
    XEiJ.dbgVisibleMask |= XEiJ.DBG_ATW_VISIBLE_MASK;
  }  //atwOpen()

  //atwMake ()
  //  アドレス変換ウインドウを作る
  public static void atwMake () {

    for (int m = 0; m < 2; m++) {
      //スクロールテキストエリア
      atwBoard[m] = ComponentFactory.setPreferredSize (
        ComponentFactory.setFont (new ScrollTextArea (), LnF.lnfMonospacedFont),
        550, 250);
      atwBoard[m].setMargin (new Insets (2, 4, 2, 4));
      atwBoard[m].setHighlightCursorOn (true);
      atwTextArea[m] = atwBoard[m].getTextArea ();
      atwTextArea[m].setEditable (false);
    }

    //テキストエリアのマウスリスナー
    ComponentFactory.addListener (
      atwTextArea[0],
      new MouseAdapter () {
        @Override public void mousePressed (MouseEvent me) {
          if (XEiJ.mpuTask == null && me.isPopupTrigger ()) {
            XEiJ.dbgShowPopup (me, atwTextArea[0], false);
          }
        }
        @Override public void mouseReleased (MouseEvent me) {
          if (XEiJ.mpuTask == null && me.isPopupTrigger ()) {
            XEiJ.dbgShowPopup (me, atwTextArea[0], false);
          }
        }
      });
    ComponentFactory.addListener (
      atwTextArea[1],
      new MouseAdapter () {
        @Override public void mousePressed (MouseEvent me) {
          if (XEiJ.mpuTask == null && me.isPopupTrigger ()) {
            XEiJ.dbgShowPopup (me, atwTextArea[1], false);
          }
        }
        @Override public void mouseReleased (MouseEvent me) {
          if (XEiJ.mpuTask == null && me.isPopupTrigger ()) {
            XEiJ.dbgShowPopup (me, atwTextArea[1], false);
          }
        }
      });

    //ボタンのアクションリスナー
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        switch (ae.getActionCommand ()) {
        case "Update":
          atwUpdate ();
          break;
        }
      }
    };

    //ウインドウ
    atwFrame = Multilingual.mlnTitle (
      ComponentFactory.createRestorableSubFrame (
        Settings.SGS_ATW_FRAME_KEY,
        "Logical space monitor",
        null,
        ComponentFactory.createBorderPanel (
          ComponentFactory.createVerticalSplitPane (
            ComponentFactory.createVerticalBox (
              ComponentFactory.createHorizontalBox (
                Multilingual.mlnText (ComponentFactory.createLabel ("User mode"), "ja", "ユーザモード"),
                Box.createHorizontalGlue ()
                ),
              atwBoard[0]
              ),
            ComponentFactory.createVerticalBox (
              ComponentFactory.createHorizontalBox (
                Multilingual.mlnText (ComponentFactory.createLabel ("Supervisor mode"), "ja", "スーパーバイザモード"),
                Box.createHorizontalGlue ()
                ),
              atwBoard[1]
              )
            ),
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
      "ja", "論理空間モニタ");
    ComponentFactory.addListener (
      atwFrame,
      new WindowAdapter () {
        @Override public void windowClosing (WindowEvent we) {
          XEiJ.dbgVisibleMask &= ~XEiJ.DBG_ATW_VISIBLE_MASK;
        }
      });

  }  //atwMake()

  //atwUpdate ()
  //  アドレス変換ウインドウを更新する
  public static void atwUpdate () {
    for (int m = 0; m < 2; m++) {
      StringBuilder sb = new StringBuilder ();
      sb.append (Multilingual.mlnJapanese ?
                 //xxxxxxx-xxxxxxxx  xxxxxxxx-xxxxxxxx
                 "  論理アドレス       物理アドレス\n" :
                 " Logical address   Physical address\n");
      int l0 = 0x00000000;
      int p0 = MC68060.mmuTranslatePeek (l0, m, 0);
      int f0 = MC68060.mmuPeekFlags;
      if ((p0 ^ l0) == 1) {
        p0 = 1;
      }
      int l1 = MC68060.mmuPageSize;
      while (l1 != 0x00000000) {
        int p1 = MC68060.mmuTranslatePeek (l1, m, 0);
        int f1 = MC68060.mmuPeekFlags;
        if ((p1 ^ l1) == 1) {
          p1 = 1;
        }
        if (!(p0 == 1 && p1 == 1 ||
              p0 != 1 && p1 != 1 && p1 - p0 == l1 - l0 && f0 == f1)) {
          if (p0 != 1) {
            XEiJ.fmtHex8 (XEiJ.fmtHex8 (XEiJ.fmtHex8 (XEiJ.fmtHex8 (sb, l0).append ('-'), l1 - 1).append ("  "), p0).append ('-'), p0 + (l1 - l0) - 1).append (' ');
            if ((f0 & MC68060.MMU_DESCRIPTOR_SUPERVISOR_PROTECTED) != 0) {
              sb.append (Multilingual.mlnJapanese ? " [スーパーバイザ]" : " [Supervisor]");
            }
            if ((f0 & MC68060.MMU_DESCRIPTOR_MODIFIED) != 0) {
              sb.append (Multilingual.mlnJapanese ? " [修正済み]" : " [Modified]");
            }
            if ((f0 & MC68060.MMU_DESCRIPTOR_USED) != 0) {
              sb.append (Multilingual.mlnJapanese ? " [使用済み]" : " [Used]");
            }
            if ((f0 & MC68060.MMU_DESCRIPTOR_WRITE_PROTECTED) != 0) {
              sb.append (Multilingual.mlnJapanese ? " [書き込み禁止]" : " [Write-protected]");
            }
            sb.append ('\n');
          }
          l0 = l1;
          p0 = p1;
          f0 = f1;
        }
        l1 += MC68060.mmuPageSize;
      }
      if (p0 != 1) {
        XEiJ.fmtHex8 (XEiJ.fmtHex8 (XEiJ.fmtHex8 (XEiJ.fmtHex8 (sb, l0).append ('-'), l1 - 1).append ("  "), p0).append ('-'), p0 + (l1 - l0) - 1).append (' ');
        if ((f0 & MC68060.MMU_DESCRIPTOR_SUPERVISOR_PROTECTED) != 0) {
          sb.append (Multilingual.mlnJapanese ? " [スーパーバイザ]" : " [Supervisor]");
        }
        if ((f0 & MC68060.MMU_DESCRIPTOR_MODIFIED) != 0) {
          sb.append (Multilingual.mlnJapanese ? " [修正済み]" : " [Modified]");
        }
        if ((f0 & MC68060.MMU_DESCRIPTOR_USED) != 0) {
          sb.append (Multilingual.mlnJapanese ? " [使用済み]" : " [Used]");
        }
        if ((f0 & MC68060.MMU_DESCRIPTOR_WRITE_PROTECTED) != 0) {
          sb.append (Multilingual.mlnJapanese ? " [書き込み禁止]" : " [Write-protected]");
        }
        sb.append ('\n');
      }
      atwTextArea[m].setText (sb.toString ());
      atwTextArea[m].setCaretPosition (0);
    }
  }  //atwUpdate()

}  //class LogicalSpaceMonitor



