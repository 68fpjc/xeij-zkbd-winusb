//========================================================================================
//  RootPointerList.java
//    en:Root pointer list -- You can select tasks that should be stopped under the multitasking environment.
//    ja:ルートポインタリスト -- マルチタスク環境で停止させるタスクを選択できます。
//  Copyright (C) 2003-2024 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//  マルチタスク環境では停止ボタンが押された瞬間にコアを止めてしまうとどのタスクが止まるかわからず、
//  止めたいタスクを狙って止めることが難しい
//  動作中のタスクのルートポインタのリストを表示して選択されたタスクだけ停止ボタンで止められるようにする
//  選択されていないタスクの実行中は停止ボタンが押されても無視する
//
//  停止フラグがセットされたルートポインタがルートポインタレジスタに設定された瞬間にコアを止める方法もあるが、
//  ルートポインタレジスタが設定されてからそのタスクのコードに辿り着くまでに毎回多くの命令を実行しなければならず、
//  効率が悪い
//
//  過去3秒間にルートポインタレジスタに設定されたルートポインタを動作中のタスクのルートポインタとみなす
//  ルートポインタレジスタを監視するだけでアドレス変換テーブルにアクセスするわけではないので、
//  既に存在しないタスクが混ざっていても問題ない
//----------------------------------------------------------------------------------------

package xeij;

import java.awt.*;  //BasicStroke,BorderLayout,BoxLayout,Color,Component,Container,Cursor,Desktop,Dimension,Font,Frame,Graphics,Graphics2D,GraphicsDevice,GraphicsEnvironment,GridLayout,Image,Insets,Paint,Point,Rectangle,RenderingHints,Robot,Shape,Stroke,TexturePaint,Toolkit
import java.awt.event.*;  //ActionEvent,ActionListener,ComponentAdapter,ComponentEvent,ComponentListener,FocusAdapter,FocusEvent,FocusListener,InputEvent,KeyAdapter,KeyEvent,KeyListener,MouseAdapter,MouseEvent,MouseListener,MouseMotionAdapter,MouseWheelEvent,WindowAdapter,WindowEvent,WindowListener,WindowStateListener
import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import javax.swing.*;  //AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JApplet,JButton,JCheckBox,JCheckBoxMenuItem,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException

public class RootPointerList {

  public static final boolean RTL_ON = true;

  public static final long RTL_SPAN = XEiJ.TMR_FREQ * 3;  //レコードの寿命。3秒
  public static final int RTL_HASH_BITS = 6;
  public static final int RTL_HASH_SIZE = 1 << RTL_HASH_BITS;
  public static final int RTL_HASH_COEFF = 0x5efc103f;  //ハッシュ関数の係数。アドレス変換キャッシュ用の係数を流用する

  //ルートポインタレコード
  public static class RootPointerRecord {
    public int rbrRootPointer;  //ルートポインタ
    public RootPointerRecord rbrNextRecord;  //同じハッシュコードを持つ次のレコード。空きレコードリストの次のレコード
    public long rbrTimeLimit;  //廃棄時刻。XEiJ.mpuClockTimeが廃棄時刻を過ぎたらレコードを廃棄する
    //public boolean rbrInterruptFlag;  //次にこのルートテーブルがルートポインタレジスタに設定されたらコアを止める
    public boolean rbrThisTaskIsStoppable;  //true=このタスクは停止ボタンで止められる
  }

  //停止フラグ
  public static boolean rtlCurrentSupervisorTaskIsStoppable;  //true=動作中のスーパーバイザタスクは停止ボタンで止められる
  public static boolean rtlCurrentUserTaskIsStoppable;  //true=動作中のユーザタスクは停止ボタンで止められる

  //ハッシュテーブル
  public static final RootPointerRecord[] rtlHashTable = new RootPointerRecord[RTL_HASH_SIZE];  //ハッシュコード→同じハッシュコードを持つレコードのリストの先頭

  //空きレコードリスト
  public static RootPointerRecord rtlFreeRecordList;  //空きレコードのリストの先頭

  //ウインドウ
  public static JFrame rtlFrame;  //ウインドウ
  public static ScrollTextArea rtlBoard;  //スクロールテキストエリア
  public static JTextArea rtlTextArea;  //テキストエリア

  //タイマー
  public static final int RTL_INTERVAL = 10;
  public static int rtlTimer;

  public static void rtlInit () {

    //停止フラグ
    rtlCurrentSupervisorTaskIsStoppable = true;
    rtlCurrentUserTaskIsStoppable = true;

    //ハッシュテーブル
    for (int hashCode = 0; hashCode < RTL_HASH_SIZE; hashCode++) {
      rtlHashTable[hashCode] = null;
    }

    //空きレコードリスト
    rtlFreeRecordList = null;

    //ウインドウ
    rtlFrame = null;

    //タイマー
    rtlTimer = 0;

  }  //rtlInit()

  //rtlStart ()
  public static void rtlStart () {
    if (RestorableFrame.rfmGetOpened (Settings.SGS_RTL_FRAME_KEY)) {
      rtlOpen ();
    }
  }  //rtlStart()

  //rtlOpen ()
  //  ルートポインタリストを開く
  public static void rtlOpen () {
    if (rtlFrame == null) {
      rtlMakeFrame ();
    } else {
      rtlUpdateFrame ();
    }
    XEiJ.dbgVisibleMask |= XEiJ.DBG_RTL_VISIBLE_MASK;
    XEiJ.pnlExitFullScreen (false);
    rtlFrame.setVisible (true);
  }  //rtlOpen()

  //rtlMakeFrame ()
  //  ルートポインタリストを作る
  //  ここでは開かない
  public static void rtlMakeFrame () {

    //スクロールテキストエリア
    rtlBoard = ComponentFactory.setPreferredSize (
      ComponentFactory.setFont (new ScrollTextArea (), LnF.lnfMonospacedFont),
      300, 200);
    rtlBoard.setMargin (new Insets (2, 4, 2, 4));  //グリッドを更新させるためJTextAreaではなくScrollTextAreaに設定する必要がある
    rtlTextArea = rtlBoard.getTextArea ();
    rtlTextArea.setEditable (false);

    //テキストエリアのマウスリスナー
    ComponentFactory.addListener (
      rtlTextArea,
      new MouseAdapter () {
        @Override public void mousePressed (MouseEvent me) {
          String text = rtlTextArea.getText ();
          //int offset = rtlTextArea.viewToModel (new Point (0, me.getY ()));  //viewToModel2Dは9から
          int offset = rtlTextArea.viewToModel2D (new Point (0, me.getY ()));  //viewToModel2Dは9から
          if (offset < 0 || text.length () <= offset) {
            return;
          }
          if (0 < offset) {
            offset = text.lastIndexOf ("\n", offset - 1) + 1;  //行頭
          }
          if (Character.digit (text.charAt (offset), 16) < 0) {
            return;
          }
          int rootPointer = Integer.parseInt (text.substring (offset, offset + 8), 16);
          int hashCode = rootPointer * RTL_HASH_COEFF >>> -RTL_HASH_BITS;  //ハッシュコード
          for (RootPointerRecord record = rtlHashTable[hashCode]; record != null; record = record.rbrNextRecord) {
            if (record.rbrRootPointer == rootPointer) {  //同じルートポインタが見つかった
              //record.rbrInterruptFlag = true;  //コアを止める
              record.rbrThisTaskIsStoppable = !record.rbrThisTaskIsStoppable;
              if (record.rbrRootPointer == MC68060.mmuSRP) {
                rtlCurrentSupervisorTaskIsStoppable = record.rbrThisTaskIsStoppable;
              }
              if (record.rbrRootPointer == MC68060.mmuURP) {
                rtlCurrentUserTaskIsStoppable = record.rbrThisTaskIsStoppable;
              }
              return;
            }
          }
        }
      });

    //ウインドウ
    rtlFrame = Multilingual.mlnTitle (
      ComponentFactory.createRestorableSubFrame (
        Settings.SGS_RTL_FRAME_KEY,
        "Root pointer list",
        null,
        ComponentFactory.createVerticalBox (
          rtlBoard
          )
        ),
      "ja", "ルートポインタリスト");  //Multilingual.mlnTitle

    //  ウインドウリスナー
    ComponentFactory.addListener (
      rtlFrame,
      new WindowAdapter () {
        @Override public void windowClosing (WindowEvent we) {
          XEiJ.dbgVisibleMask &= ~XEiJ.DBG_RTL_VISIBLE_MASK;
        }
      });

  }  //rtlMakeFrame()

  //rtlUpdateFrame ()
  //  ルートポインタリストを更新する
  public static void rtlUpdateFrame () {

    if (rtlFrame == null) {
      return;
    }

    //長時間ルートポインタが更新されないと消えてしまうので表示するときに更新されたことにする
    rtlSetRootPointer (MC68060.mmuURP, false);
    rtlSetRootPointer (MC68060.mmuSRP, true);

    StringBuilder sb = new StringBuilder ();
    for (int hashCode = 0; hashCode < RTL_HASH_SIZE; hashCode++) {
      RootPointerRecord prevRecord = null;
      for (RootPointerRecord record = rtlHashTable[hashCode]; record != null; record = record.rbrNextRecord) {
        if (record.rbrTimeLimit <= XEiJ.mpuClockTime) {  //廃棄時刻を過ぎている
          if (prevRecord != null) {
            prevRecord.rbrNextRecord = null;  //残りを切り捨てる
          } else {
            rtlHashTable[hashCode] = null;  //残りを切り捨てる
          }
          RootPointerRecord lastRecord = record;
          while (lastRecord.rbrNextRecord != null) {
            lastRecord = lastRecord.rbrNextRecord;
          }
          lastRecord.rbrNextRecord = rtlFreeRecordList;  //リストのまま空きレコードのリストの先頭に押し込む
          rtlFreeRecordList = record;
          break;
        }
        XEiJ.fmtHex8 (sb, record.rbrRootPointer).append (record.rbrThisTaskIsStoppable ? Multilingual.mlnJapanese ? "\t停止許可" : "\tstoppable" : Multilingual.mlnJapanese ? "\t停止禁止" : "\tunstoppable").append ('\n');
        prevRecord = record;
      }  //for record
    }  //for hashCode

    if (rtlTextArea != null) {
      rtlTextArea.setText (sb.toString ());
    }

  }  //rtlUpdateFrame()

  //rtlSetRootPointer (rootPointer)
  //  ルートポインタがルートポインタレジスタに設定されたときに呼び出す
  public static void rtlSetRootPointer (int rootPointer, boolean supervisor) {
    int hashCode = rootPointer * RTL_HASH_COEFF >>> -RTL_HASH_BITS;  //ハッシュコード
    RootPointerRecord prevRecord = null;
    for (RootPointerRecord record = rtlHashTable[hashCode]; record != null; record = record.rbrNextRecord) {
      if (record.rbrRootPointer == rootPointer) {  //同じルートポインタが見つかった
        if (prevRecord != null) {  //リストの先頭でないとき
          prevRecord.rbrNextRecord = record.rbrNextRecord;  //リストから切り離す
          record.rbrNextRecord = rtlHashTable[hashCode];  //リストの先頭に押し込む
          rtlHashTable[hashCode] = record;
        }
        record.rbrTimeLimit = XEiJ.mpuClockTime + RTL_SPAN;  //廃棄時刻
        //if (record.rbrInterruptFlag) {  //コアを止める
        //  record.rbrInterruptFlag = false;
        //  XEiJ.mpuStop (null);
        //}
        if (supervisor) {
          rtlCurrentSupervisorTaskIsStoppable = record.rbrThisTaskIsStoppable;
        } else {
          rtlCurrentUserTaskIsStoppable = record.rbrThisTaskIsStoppable;
        }
        return;
      }
      prevRecord = record;
    }  //for record
    //同じルートポインタが見つからなかった
    RootPointerRecord record;
    if (rtlFreeRecordList == null) {  //空きレコードがない
      record = new RootPointerRecord ();  //新しいレコードを作る
    } else {  //空きレコードがある
      record = rtlFreeRecordList;  //空きレコードを再利用する
      rtlFreeRecordList = record.rbrNextRecord;
    }
    record.rbrRootPointer = rootPointer;
    record.rbrNextRecord = rtlHashTable[hashCode];  //リストの先頭に押し込む
    rtlHashTable[hashCode] = record;
    record.rbrTimeLimit = XEiJ.mpuClockTime + RTL_SPAN;  //廃棄時刻
    //record.rbrInterruptFlag = false;
    record.rbrThisTaskIsStoppable = true;
    if (supervisor) {
      rtlCurrentSupervisorTaskIsStoppable = record.rbrThisTaskIsStoppable;
    } else {
      rtlCurrentUserTaskIsStoppable = record.rbrThisTaskIsStoppable;
    }
  }  //rtlSetRootPointer(int,boolean)

}  //class RootPointerList



