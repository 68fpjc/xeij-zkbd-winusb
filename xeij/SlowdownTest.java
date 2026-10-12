//========================================================================================
//  SlowdownTest.java
//    en:Slowdown test
//    ja:鈍化テスト
//  Copyright (C) 2003-2019 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//  コアのスレッドにダミーのタスクを走らせることでコアのタスクが動ける時間を減らす
//  MPUの動作周波数を上げる負荷テストとは異なり、遅いマシンで動かしたときにしか生じない問題がわかるかも知れない
//----------------------------------------------------------------------------------------

package xeij;

import java.awt.event.*;  //ActionEvent,ActionListener,ComponentAdapter,ComponentEvent,ComponentListener,FocusAdapter,FocusEvent,FocusListener,InputEvent,KeyAdapter,KeyEvent,KeyListener,MouseAdapter,MouseEvent,MouseListener,MouseMotionAdapter,MouseWheelEvent,WindowAdapter,WindowEvent,WindowListener,WindowStateListener
import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,Timer,TimerTask,TreeMap
import javax.swing.*;  //AbstractButton,AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JApplet,JButton,JCheckBox,JCheckBoxMenuItem,JComponent,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException
import javax.swing.event.*;  //CaretListener,ChangeEvent,ChangeListener,DocumentEvent,DocumentListener,ListSelectionListener

public class SlowdownTest {

  public static long sdtNano;
  public static TimerTask sdtTask;

  public static JCheckBoxMenuItem sdtCheckBoxMenuItem;
  public static SpinnerNumberModel sdtModel;
  public static JSpinner sdtSpinner;
  public static Box sdtBox;

  public static void sdtInit () {
    sdtNano = 0L;
    sdtTask = null;

    sdtCheckBoxMenuItem =
      Multilingual.mlnText (
        ComponentFactory.createCheckBoxMenuItem (
          false,
          "Slowdown test",
          new ActionListener () {
            @Override public void actionPerformed (ActionEvent ae) {
              if (((JCheckBoxMenuItem) ae.getSource ()).isSelected ()) {
                sdtStart ();
              } else {
                sdtStop ();
              }
            }
          }),
        "ja", "鈍化テスト");
    sdtModel = new SpinnerNumberModel (0.0, 0.0, 99.9, 0.1);
    sdtSpinner = ComponentFactory.createNumberSpinner (sdtModel, 4, new ChangeListener () {
      @Override public void stateChanged (ChangeEvent ce) {
        sdtCheckBoxMenuItem.setSelected (true);
        sdtStart ();
      }
    });
    sdtBox = ComponentFactory.createHorizontalBox (
      Box.createHorizontalStrut (20),
      sdtSpinner,
      ComponentFactory.createLabel ("%"),
      Box.createHorizontalGlue ()
      );
  }  //sdtInit()

  //sdtStart ()
  //  鈍化テストを開始する
  public static void sdtStart () {
    sdtStop ();
    sdtNano = XEiJ.TMR_INTERVAL * Math.round (10000.0 * sdtModel.getNumber ().doubleValue ());
    sdtTask = new TimerTask () {
      @Override public void run () {
        long limit = System.nanoTime () + sdtNano;
        while (System.nanoTime () < limit) {
        }
      }
    };
    XEiJ.tmrTimer.scheduleAtFixedRate (sdtTask, XEiJ.TMR_DELAY, XEiJ.TMR_INTERVAL);
  }  //sdtStart()

  //sdtStop ()
  //  鈍化テストを終了する
  public static void sdtStop () {
    if (sdtTask != null) {
      sdtTask.cancel ();
      sdtTask = null;
    }
  }  //sdtStop()

}  //class SlowdownTest



