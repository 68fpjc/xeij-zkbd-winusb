//========================================================================================
//  BranchLog.java
//    en:Branch log -- It records changes of the program counter and disassembles codes in order that were executed.
//    ja:分岐ログ -- プログラムカウンタの変化を記録してコードを実行された順序で逆アセンブルします。
//  Copyright (C) 2003-2025 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//  インクリメント以外の方法でpcが更新されたときに前回飛び込んだ命令の先頭アドレスと今回飛び出した命令の先頭アドレスのペアを記録する
//  リングバッファを用いてMPUが停止する直前の65536回までデバッガで遡れるようにする
//  1命令平均10サイクルで20命令に1回分岐すると仮定すると10MHzのとき1秒間に5万回分岐することになる
//  分岐レコード
//    [0]  bit31-bit1  head   分岐命令や例外処理などで飛び込んだ命令の先頭アドレス。連続して実行された命令の並びの先頭
//               bit0  super  0=ユーザモード,1=スーパーバイザモード
//    [1]  bit31-bit1  tail   分岐命令や例外処理などで飛び出した命令の先頭アドレス。連続して実行された命令の並びの末尾
//               bit0  int    0=ノーマル,1=インタラプト
//    常にhead <= tail (符号なし比較)
//    分岐先の最初の命令で再び分岐したときはheadとtailが同じになる
//    割り込みも記録されるので飛び出した命令は分岐命令とは限らない
//    同じペアを繰り返したときはループとみなして新しいレコードを作らない。ループ回数は記録しない
//    [2-17]  regRn[0-15]のコピー
//  停止レコード
//    MPUが停止したとき最新のレコードに停止レコードを書き込む
//    停止レコードは次にMPUが動き出したときに上書きされる
//    停止レコードのtailはMPUが次に実行する命令の先頭アドレス
//      分岐ログに記録された範囲でこの命令だけがまだ実行されていない
//    命令ブレークポイントで停止したときはheadとtailがどちらもこれから実行する命令の先頭アドレスになる
//      このときはheadもまだ実行されていない
//  レコードの実体
//    レコードの実体はblgArray[(レコード番号の下位16bit)<<BLG_RECORD_SHIFT]から始まる
//    i=(char)record<<BLG_RECORD_SHIFT
//  最古のレコード
//    i=(char)Math.max(0L,blgNewestRecord-65535L)<<BLG_RECORD_SHIFT
//  最新のレコード
//    i=(char)blgNewestRecord<<BLG_RECORD_SHIFT
//----------------------------------------------------------------------------------------

package xeij;

import java.awt.*;  //BasicStroke,BorderLayout,BoxLayout,Color,Component,Container,Cursor,Desktop,Dimension,Font,FlowLayout,Frame,Graphics,Graphics2D,GraphicsDevice,GraphicsEnvironment,GridLayout,Image,Insets,Paint,Point,Rectangle,RenderingHints,Robot,Shape,Stroke,TexturePaint,Toolkit
import java.awt.event.*;  //ActionEvent,ActionListener,ComponentAdapter,ComponentEvent,ComponentListener,FocusAdapter,FocusEvent,FocusListener,InputEvent,KeyAdapter,KeyEvent,KeyListener,MouseAdapter,MouseEvent,MouseListener,MouseMotionAdapter,MouseWheelEvent,WindowAdapter,WindowEvent,WindowListener,WindowStateListener
import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,Timer,TimerTask,TreeMap
import javax.swing.*;  //AbstractButton,AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JApplet,JButton,JCheckBox,JCheckBoxMenuItem,JComponent,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException
import javax.swing.event.*;  //CaretEvent,CaretListener,ChangeEvent,ChangeListener,DocumentEvent,DocumentListener,ListSelectionListener

public class BranchLog {

  public static final boolean BLG_ON = true;  //true=分岐ログを有効にする

  //ログ
  //  原則としてMPUの動作中はblgNewestRecordが動き続けているのでログを参照できないが、
  //  PFVのようにMPUの動作の合間であることが保証されていれば参照することができる
  public static final int BLG_RECORD_SHIFT = 5;  //1つのレコードに32要素使う
  public static final int[] blgArray = new int[65536 << BLG_RECORD_SHIFT];  //head0,tail0,head1,tail1,...
  public static long blgNewestRecord;  //最新のレコードの番号。blgArray[(char)blgNewestRecord<<BLG_RECORD_SHIFT]が次に書き込む位置
  public static int blgPrevHeadSuper;  //前回のレコードの[0]のコピー
  public static int blgPrevTailInt;  //前回のレコードの[1]のコピー
  public static int blgHead;  //分岐命令や例外処理などで飛び込んだ命令の先頭アドレス
  public static int blgSuper;  //0=ユーザモード,1=スーパーバイザモード
  public static int blgInt;  //0=ノーマル,1=インタラプト
  public static final int[] blgRegs = new int[16];  //レジスタ

  //ウインドウ
  public static JFrame blgFrame;  //ウインドウ

  //スピナー
  public static SpinnerNumberModel blgModel;  //レコード番号のスピナーのスピナーモデル
  public static JSpinner blgSpinner;  //レコード番号のスピナー

  //テキストエリア
  public static final int BLG_TEXT_AREA_WIDTH = 400;  //テキストエリアの幅
  public static final int BLG_TEXT_AREA_HEIGHT = 400;  //テキストエリアの高さ
  public static ScrollTextArea blgScrollTextArea;  //スクロールテキストエリア
  public static JTextArea blgTextArea;  //テキストエリア
  public static boolean blgLock;  //true=テキスト更新中。更新中にスピナーやキャレットを動かすのでイベントで更新がネストしないようにする

  //ページ
  public static final long BLG_SELECT_OLDEST = -3L;  //最古のレコードを選択する
  public static final long BLG_SELECT_NEWEST = -2L;  //最新のレコードを選択する
  public static final long BLG_SELECT_NONE = -1L;  //レコードを選択しない
  public static final int BLG_RECORDS_PER_PAGE = 1000;  //1ページに表示するレコードの数
  public static int blgNumberOfRecords;  //現在のページのレコードの数。最新のページと最古のページは1ページに表示するレコードの数よりも少ない場合がある。1<=blgNumberOfRecords<=BLG_RECORDS_PER_PAGE
  public static long blgFirstRecord;  //現在のページの先頭のレコードの番号。対応する項目の番号は1
  public static long blgLastRecord;  //現在のページの末尾のレコードの番号。対応する項目の番号はblgNumberOfRecords
  public static long blgSelectedRecord;  //現在のページの選択されているレコードの番号。blgFirstRecord<=blgSelectedRecord<=blgLastRecord
  public static int blgNumberOfItems;  //現在のページの項目の数。先頭と末尾の番兵を含む。3<=blgNumberOfItems=blgNumberOfRecords+2<=BLG_RECORDS_PER_PAGE+2
  public static int blgSelectedItem;  //現在のページの選択されている項目の番号。blgSelectedItem=(int)(blgSelectedRecord-blgFirstRecord)+1
  public static final long[] blgRecordArray = new long[BLG_RECORDS_PER_PAGE + 2];  //現在のページのアイテムの番号に対応するレコードの番号の配列。[0]=直前のページの末尾のレコードの番号,[1]=blgFirstRecord,[blgSelectedItem]=blgSelectedRecord,[blgNumberOfRecords]=blgLastRecord,[blgNumberOfItems-1]=直後のページの先頭のレコードの番号
  public static final int[] blgPositionArray = new int[BLG_RECORDS_PER_PAGE + 3];  //現在のページのアイテムの番号に対応するテキストの開始位置の配列。[0]=0,[blgNumberOfItems]=テキストの長さ

  //モード
  public static boolean blgShowUser;  //ユーザレコードを表示する
  public static boolean blgShowSupervisor;  //スーパーバイザレコードを表示する
  public static boolean blgShowNormal;  //ノーマルレコードを表示する
  public static boolean blgShowInterrupt;  //インタラプトレコードを表示する
  public static boolean blgShowRegs;  //レジスタを表示する

  //blgInit ()
  //  初期化
  public static void blgInit () {

    //分岐ログ
    //blgArray = new int[65536 << BLG_RECORD_SHIFT];
    blgNewestRecord = 0L;
    blgPrevHeadSuper = 0;
    blgPrevTailInt = 0;
    blgHead = 0;
    blgSuper = 0;
    blgInt = 0;
    //blgRegs = new int[16];

    //ウインドウ
    blgFrame = null;

    //パネル

    //スピナー
    blgModel = null;
    blgSpinner = null;

    //テキストエリア
    blgScrollTextArea = null;
    blgTextArea = null;
    blgLock = false;

    //ページ
    blgNumberOfRecords = 0;
    blgFirstRecord = -1L;
    blgLastRecord = -1L;
    blgSelectedRecord = -1L;
    blgNumberOfItems = 0;
    blgSelectedItem = -1;
    //blgRecordArray = new long[BLG_RECORDS_PER_PAGE + 2];
    //blgPositionArray = new int[BLG_RECORDS_PER_PAGE + 3];

    //モード
    blgShowUser = true;
    blgShowSupervisor = true;
    blgShowNormal = true;
    blgShowInterrupt = true;
    blgShowRegs = false;

  }  //blgInit()

  //blgReset ()
  //  リセット
  //  リセットされる前のログは保存しない
  public static void blgReset () {

    blgNewestRecord = 0L;
    blgPrevHeadSuper = 0;
    blgPrevTailInt = 0;
    blgHead = XEiJ.regPC;
    blgSuper = XEiJ.regSRS >>> 13;
    blgInt = XEiJ.mpuISR == 0 ? 0 : 1;
    System.arraycopy (XEiJ.regRn, 0, blgRegs, 0, 16);

    blgNumberOfRecords = 0;
    blgFirstRecord = -1L;
    blgLastRecord = -1L;
    blgSelectedRecord = -1L;
    blgNumberOfItems = 0;
    blgSelectedItem = -1;

    DisassembleList.ddpBacktraceRecord = -1L;  //未選択

  }  //blgReset()

  //blgStop ()
  //  最新レコードに停止レコードを記録する
  //  tailはpc
  //  最後に実行した命令で分岐したときはheadもpc
  public static void blgStop () {
    int i = (char) blgNewestRecord << BLG_RECORD_SHIFT;
    blgArray[i] = blgHead | blgSuper;
    blgArray[i + 1] = XEiJ.regPC | blgInt;  //次に実行する命令
    System.arraycopy (blgRegs, 0, blgArray, i + 2, 16);
  }  //blgStop()

  //blgJump (a)
  //  最新レコードに分岐レコードを記録して次の分岐レコードに進む
  public static void blgJump (int a) {
    if (blgPrevHeadSuper != (blgHead | blgSuper) ||
        blgPrevTailInt != (XEiJ.regPC0 | blgInt)) {  //前回のレコードと異なるとき
      int i = (char) blgNewestRecord++ << BLG_RECORD_SHIFT;
      blgArray[i] = blgPrevHeadSuper = blgHead | blgSuper;
      blgArray[i + 1] = blgPrevTailInt = XEiJ.regPC0 | blgInt;
      System.arraycopy (blgRegs, 0, blgArray, i + 2, 16);
    }
    blgHead = XEiJ.regPC = a;
    blgSuper = XEiJ.regSRS >>> 13;
    blgInt = XEiJ.mpuISR == 0 ? 0 : 1;
    System.arraycopy (XEiJ.regRn, 0, blgRegs, 0, 16);
  }  //blgJump(int)

  //blgMakeFrame ()
  //  分岐ログウインドウを作る
  //  ここでは開かない
  public static void blgMakeFrame () {

    //スクロールテキストエリア
    blgScrollTextArea = ComponentFactory.setPreferredSize (
      ComponentFactory.setFont (new ScrollTextArea (), LnF.lnfMonospacedFont),
      BLG_TEXT_AREA_WIDTH, BLG_TEXT_AREA_HEIGHT);
    blgScrollTextArea.setMargin (new Insets (2, 4, 2, 4));  //グリッドを更新させるためJTextAreaではなくScrollTextAreaに設定する必要がある
    blgScrollTextArea.setHighlightCursorOn (true);
    blgTextArea = blgScrollTextArea.getTextArea ();
    blgTextArea.setEditable (false);
    blgTextArea.setText (Multilingual.mlnJapanese ? "MPU が動作中です" : "MPU is running");
    blgTextArea.setCaretPosition (0);
    blgTextArea.addMouseWheelListener ((mwe) -> {
      int n = mwe.getWheelRotation ();
      JViewport v = blgScrollTextArea.getViewport ();
      Point p = v.getViewPosition ();
      v.setViewPosition (new Point (p.x,
                                    Math.max (0,
                                              Math.min (blgTextArea.getSize ().height - v.getExtentSize ().height,
                                                        p.y + n * blgTextArea.getFont ().getSize () * 5))));
    });

    //テキストエリアのマウスリスナー
    ComponentFactory.addListener (
      blgTextArea,
      new MouseAdapter () {
        @Override public void mousePressed (MouseEvent me) {
          if (XEiJ.mpuTask == null && me.isPopupTrigger ()) {
            XEiJ.dbgShowPopup (me, blgTextArea, false);
          }
        }
        @Override public void mouseReleased (MouseEvent me) {
          if (XEiJ.mpuTask == null && me.isPopupTrigger ()) {
            XEiJ.dbgShowPopup (me, blgTextArea, false);
          }
        }
      });

    //キャレットリスナー
    //  キー操作またはクリックでキャレットが動いたときキャレットの位置のレコードの番号をスピナーに設定する
    ComponentFactory.addListener (
      blgTextArea,
      new CaretListener () {
        @Override public void caretUpdate (CaretEvent ce) {
          if (blgSelectedRecord >= 0 && !blgLock &&  //テキストは構築済みでsetTextの中ではない
              XEiJ.dbgEventMask == 0) {  //Select Allの中ではない
            int p = ce.getDot ();  //キャレットの位置
            if (p == ce.getMark ()) {  //選択範囲がない
              int item = Arrays.binarySearch (blgPositionArray, 1, blgNumberOfItems, p + 1);  //項目の先頭のときも次の項目を検索してから1つ戻る
              item = (item >> 31 ^ item) - 1;  //キャレットがある位置を含む項目の番号
              if (blgSelectedItem != item) {  //選択されているレコードとは別の項目に移動した
                if (item == 0) {  //先頭の番兵
                  blgUpdate (Math.max (0L, blgFirstRecord - 1L));  //直前のページの末尾へ
                } else if (item <= blgNumberOfRecords) {  //レコード
                  blgLock = true;
                  long record = blgRecordArray[item];
                  blgSelectedRecord = record;
                  blgSelectedItem = item;
                  if (blgModel.getNumber ().longValue () != record) {
                    blgModel.setValue (Long.valueOf (record));  //スピナーの値を設定する
                  }
                  blgLock = false;
                } else {  //末尾の番兵
                  blgUpdate (blgLastRecord + 1L);  //直後のページの先頭へ
                }
              }
            }
          }
        }
      });

    //スピナー
    blgModel = new ReverseLongModel (0L, 0L, Long.MAX_VALUE, 1L);
    blgSpinner = ComponentFactory.createNumberSpinner (blgModel, 10, new ChangeListener () {
      @Override public void stateChanged (ChangeEvent ce) {
        if (!blgLock) {
          blgUpdate (blgModel.getNumber ().longValue ());
        }
      }
    });

    //アクションリスナー
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        switch (ae.getActionCommand ()) {
        case "Clear":  //クリア
          blgClear ();
          blgUpdate (BLG_SELECT_NEWEST);
          break;
        case "Oldest record":  //最古のレコード
          if (blgSelectedRecord >= 0) {
            blgUpdate (BLG_SELECT_OLDEST);  //最古のレコードへ
          }
          break;
        case "Previous page":  //前のページ
          if (blgSelectedRecord >= 0) {
            blgUpdate (blgFirstRecord < blgSelectedRecord ? blgFirstRecord :  //ページの先頭でなければページの先頭へ
                       Math.max (0L, blgFirstRecord - BLG_RECORDS_PER_PAGE));  //ページの先頭のときは直前のページの先頭へ
          }
          break;
        case "Previous record":  //前のレコード
          if (blgSelectedRecord > 0) {
            if (blgTextArea.getCaretPosition () != blgPositionArray[blgSelectedItem]) {  //キャレットが項目の先頭にないとき
              blgTextArea.setCaretPosition (blgPositionArray[blgSelectedItem]);  //キャレットを項目の先頭に移動する
            } else {  //キャレットが項目の先頭にあるとき
              blgUpdate (blgSelectedRecord - 1);  //直前のレコードへ
            }
          }
          break;
        case "Next record":  //次のレコード
          if (blgSelectedRecord >= 0) {
            blgUpdate (blgSelectedRecord + 1);  //直後のレコードへ
          }
          break;
        case "Next page":  //次のページ
          if (blgSelectedRecord >= 0) {
            blgUpdate (blgSelectedRecord < blgLastRecord ? blgLastRecord :  //ページの末尾でなければページの末尾へ
                       blgLastRecord + BLG_RECORDS_PER_PAGE);  //ページの末尾のときは直後のページの末尾へ
          }
          break;
        case "Newest record":  //最新のレコード
          if (blgSelectedRecord >= 0) {
            blgUpdate (BLG_SELECT_NEWEST);  //最新のレコードへ
          }
          break;

        case "User":
          blgShowUser = ((JCheckBox) source).isSelected ();
          blgUpdate (BLG_SELECT_NONE);
          break;
        case "Supervisor":
          blgShowSupervisor = ((JCheckBox) source).isSelected ();
          blgUpdate (BLG_SELECT_NONE);
          break;
        case "Normal":
          blgShowNormal = ((JCheckBox) source).isSelected ();
          blgUpdate (BLG_SELECT_NONE);
          break;
        case "Interrupt":
          blgShowInterrupt = ((JCheckBox) source).isSelected ();
          blgUpdate (BLG_SELECT_NONE);
          break;
        case "Regs":
          blgShowRegs = ((JCheckBox) source).isSelected ();
          blgUpdate (BLG_SELECT_NONE);
          break;

        }
      }  //actionPerformed(ActionEvent)
    };  //listener

    //ボタン
    JButton clearButton = XEiJ.mpuAddButtonStopped (
      Multilingual.mlnToolTipText (
        ComponentFactory.createImageButton (
          LnF.LNF_CLEAR_IMAGE,
          LnF.LNF_CLEAR_DISABLED_IMAGE,
          "Clear", listener),
        "ja", "クリア")
      );
    JButton oldestRecordButton = XEiJ.mpuAddButtonStopped (
      Multilingual.mlnToolTipText (
        ComponentFactory.createImageButton (
          LnF.LNF_OLDEST_IMAGE,
          LnF.LNF_OLDEST_DISABLED_IMAGE,
          "Oldest record", listener),
        "ja", "最古のレコード")
      );
    JButton previousPageButton = XEiJ.mpuAddButtonStopped (
      Multilingual.mlnToolTipText (
        ComponentFactory.createImageButton (
          LnF.LNF_OLDER_IMAGE,
          LnF.LNF_OLDER_DISABLED_IMAGE,
          "Previous page", listener),
        "ja", "前のページ")
      );
    JButton previousRecordButton = XEiJ.mpuAddButtonStopped (
      Multilingual.mlnToolTipText (
        ComponentFactory.createImageButton (
          LnF.LNF_PREVIOUS_IMAGE,
          LnF.LNF_PREVIOUS_DISABLED_IMAGE,
          "Previous record", listener),
        "ja", "前のレコード")
      );
    JButton nextRecordButton = XEiJ.mpuAddButtonStopped (
      Multilingual.mlnToolTipText (
        ComponentFactory.createImageButton (
          LnF.LNF_NEXT_IMAGE,
          LnF.LNF_NEXT_DISABLED_IMAGE,
          "Next record", listener),
        "ja", "次のレコード")
      );
    JButton nextPageButton = XEiJ.mpuAddButtonStopped (
      Multilingual.mlnToolTipText (
        ComponentFactory.createImageButton (
          LnF.LNF_NEWER_IMAGE,
          LnF.LNF_NEWER_DISABLED_IMAGE,
          "Next page", listener),
        "ja", "次のページ")
      );
    JButton newestRecordButton = XEiJ.mpuAddButtonStopped (
      Multilingual.mlnToolTipText (
        ComponentFactory.createImageButton (
          LnF.LNF_NEWEST_IMAGE,
          LnF.LNF_NEWEST_DISABLED_IMAGE,
          "Newest record", listener),
        "ja", "最新のレコード")
      );
    JCheckBox userCheckBox = ComponentFactory.createIconCheckBox (
      blgShowUser,
      LnF.LNF_USER_IMAGE,
      LnF.LNF_USER_SELECTED_IMAGE,
      "User", listener);
    JCheckBox supervisorCheckBox = ComponentFactory.createIconCheckBox (
      blgShowSupervisor,
      LnF.LNF_SUPERVISOR_IMAGE,
      LnF.LNF_SUPERVISOR_SELECTED_IMAGE,
      "Supervisor", listener);
    JCheckBox normalCheckBox = ComponentFactory.createIconCheckBox (
      blgShowNormal,
      LnF.LNF_NORMAL_IMAGE,
      LnF.LNF_NORMAL_SELECTED_IMAGE,
      "Normal", listener);
    JCheckBox interruptCheckBox = ComponentFactory.createIconCheckBox (
      blgShowInterrupt,
      LnF.LNF_INTERRUPT_IMAGE,
      LnF.LNF_INTERRUPT_SELECTED_IMAGE,
      "Interrupt", listener);
    JCheckBox regsCheckBox = ComponentFactory.createIconCheckBox (
      blgShowRegs,
      LnF.LNF_REGS_IMAGE,
      LnF.LNF_REGS_SELECTED_IMAGE,
      "Regs", listener);

    //ウインドウ
    blgFrame = Multilingual.mlnTitle (
      ComponentFactory.createRestorableSubFrame (
        Settings.SGS_BLG_FRAME_KEY,
        "Branch log",
        null,
        ComponentFactory.createBorderPanel (
          //CENTER
          blgScrollTextArea,
          //NORTH
          ComponentFactory.createVerticalBox (
            //1行目
            ComponentFactory.createHorizontalBox (
              clearButton,
              Box.createHorizontalGlue (),
              Box.createHorizontalStrut (12),
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
              ),
            //2行目
            ComponentFactory.createHorizontalBox (
              blgSpinner,
              Box.createHorizontalStrut (12),
              oldestRecordButton,
              previousPageButton,
              previousRecordButton,
              nextRecordButton,
              nextPageButton,
              newestRecordButton,
              Box.createHorizontalStrut (12),
              userCheckBox,
              supervisorCheckBox,
              normalCheckBox,
              interruptCheckBox,
              regsCheckBox,
              Box.createHorizontalGlue ()
              )
            )
          )  //BorderPanel
        ),
      "ja", "分岐ログ");
    //  ウインドウリスナー
    ComponentFactory.addListener (
      blgFrame,
      new WindowAdapter () {
        @Override public void windowClosing (WindowEvent we) {
          XEiJ.dbgVisibleMask &= ~XEiJ.DBG_BLG_VISIBLE_MASK;
        }
      });

  }  //blgMakeFrame()

  //blgClear ()
  //  Clearボタン
  public static void blgClear () {
    blgReset ();
    blgArray[0] = XEiJ.regPC | XEiJ.regSRS >>> 13;
    blgArray[1] = XEiJ.regPC | (XEiJ.mpuISR == 0 ? 0 : 1);
    System.arraycopy (XEiJ.regRn, 0, blgArray, 2, 16);
  }  //blgClear

  //blgStart ()
  public static void blgStart () {
    if (RestorableFrame.rfmGetOpened (Settings.SGS_BLG_FRAME_KEY)) {
      blgOpen (BLG_SELECT_NONE);
    }
  }  //blgStart()

  //blgOpen (selectedRecord)
  //  分岐ログウインドウを開く
  public static void blgOpen (long selectedRecord) {
    if (blgFrame == null) {
      blgMakeFrame ();
    }
    XEiJ.dbgVisibleMask |= XEiJ.DBG_BLG_VISIBLE_MASK;
    blgUpdate (selectedRecord);
    XEiJ.pnlExitFullScreen (false);
    blgFrame.setVisible (true);
  }  //blgOpen(long)

  //blgUpdate (selectedRecord)
  //  分岐ログウインドウを更新する
  //  MPUが止まってから呼び出すこと
  public static void blgUpdate (long selectedRecord) {
    if (XEiJ.mpuTask != null) {  //MPUの動作中は更新しない。タスクの合間であることを保証すればMPUの動作中でも更新できるが重いのでやめておく
      blgLock = true;
      blgTextArea.setText (Multilingual.mlnJapanese ? "MPU が動作中です" : "MPU is running");
      blgTextArea.setCaretPosition (0);
      blgLock = false;
      return;
    }
    if (blgLock) {  //更新中
      return;
    }
    blgLock = true;
    blgStop ();  //念のため停止レコードを書き込む
    long newestRecord = blgNewestRecord;  //最新のレコードの番号
    long oldestRecord = Math.max (0L, newestRecord - 65535);  //最古のレコードの番号
    if (selectedRecord < 0L) {
      if (selectedRecord == BLG_SELECT_NONE) {  //レコードを選択しないとき
        selectedRecord = blgSelectedRecord < 0L ? newestRecord : blgSelectedRecord;  //前回選択されたレコードまたは最新のレコードを選択する
      } else if (selectedRecord == BLG_SELECT_NEWEST) {  //最新のレコードを選択するとき
        selectedRecord = newestRecord;  //最新のレコードを選択する
      } else if (selectedRecord == BLG_SELECT_OLDEST) {  //最古のレコードを選択するとき
        selectedRecord = oldestRecord;  //最古のレコードを選択する
      }
    }
    if (selectedRecord < oldestRecord) {  //選択しようとしたレコードが残っていないとき
      selectedRecord = oldestRecord;  //最古のレコードを選択する
    } else if (selectedRecord > newestRecord) {  //選択しようとしたレコードがまだ存在しないとき
      selectedRecord = newestRecord;  //最新のレコードを選択する
    }
    long firstRecord = selectedRecord / BLG_RECORDS_PER_PAGE * BLG_RECORDS_PER_PAGE;  //選択されたレコードを含むページの先頭のレコードの番号
    long lastRecord = firstRecord + (long) (BLG_RECORDS_PER_PAGE - 1);  //選択されたレコードを含むページの末尾のレコードの番号
    if (firstRecord < oldestRecord) {  //ページの先頭のレコードが残っていないとき
      firstRecord = oldestRecord;  //ページの先頭のレコードは最古のレコード
    }
    if (lastRecord > newestRecord) {  //ページの末尾のレコードがまだ存在しないとき
      lastRecord = newestRecord;  //ページの末尾のレコードは最新のレコード
    }

    if (blgFirstRecord != firstRecord || blgLastRecord != lastRecord ||  //範囲が変わったとき
        blgLastRecord == blgNewestRecord) {  //最新のレコードを含んでいるとき。トレース実行またはステップ実行で分岐しなかったとき

      //!!! 過去に通過した領域にラベルを追加した場合は範囲が変わっていなくても再構築する必要がある

      //再構築する
      blgFirstRecord = firstRecord;  //ページの先頭のレコードの番号
      blgLastRecord = lastRecord;  //ページの末尾のレコードの番号
      blgSelectedRecord = selectedRecord;  //選択されているレコードの番号

      int pcPosition = -1;  //pcの位置

      //ラベルの準備
      //LabeledAddress.lblUpdateProgram ();

      //先頭の番兵
      blgRecordArray[0] = firstRecord - 1;  //直前のページの末尾のレコードの番号
      blgPositionArray[0] = 0;
      StringBuilder sb = new StringBuilder (
        firstRecord == oldestRecord ?
        Multilingual.mlnJapanese ? "───── 分岐ログの先頭 ─────\n" : "───── Top of the branch log ─────\n" :
        Multilingual.mlnJapanese ? "↑↑↑↑↑ 手前のページ ↑↑↑↑↑\n" : "↑↑↑↑↑ Previous page ↑↑↑↑↑\n");

      //項目のループ
      int itemNumber = 1;  //項目の番号
      long itemRecord = firstRecord;  //レコードの番号
      while (itemRecord <= lastRecord) {
        int i = (char) itemRecord << BLG_RECORD_SHIFT;
        int headAddress = blgArray[i] & ~1;
        int supervisor = blgArray[i] & 1;
        int tailAddress = blgArray[i + 1] & ~1;
        int interrupt = blgArray[i + 1] & 1;

        //項目の開始
        if (itemRecord == selectedRecord) {  //選択されているレコード
          blgSelectedItem = itemNumber;  //選択されている項目の番号
        }
        blgRecordArray[itemNumber] = itemRecord;  //項目のレコードの番号
        blgPositionArray[itemNumber] = sb.length ();  //項目の開始位置
        sb.append (itemRecord);  //レコードの番号
        sb.append (supervisor == 0 ? "[U" : "[S");  //ユーザまたはスーパーバイザ
        sb.append (interrupt == 0 ? "N]  " : "I]  ");  //ノーマルまたはインタラプト
        XEiJ.fmtHex8 (sb, headAddress);  //開始アドレス
        LabeledAddress.lblSearch (sb, headAddress);  //ラベル
        sb.append ('\n');

        //逆アセンブルリスト
        if ((supervisor == 0 ? blgShowUser : blgShowSupervisor) &&
            (interrupt == 0 ? blgShowNormal : blgShowInterrupt)) {

          //レジスタ
          if (blgShowRegs) {
            for (int n = 0; n < 16; n++) {
              if (n == 0) {
                sb.append ('D');
              } else if (n == 8) {
                sb.append ('A');
              } else if ((n & 7) == 4) {
                sb.append (' ');
              }
              XEiJ.fmtHex8 (sb.append (' '), blgArray[i + 2 + n]);
              if ((n & 7) == 7) {
                sb.append ('\n');
              }
            }
          }

          for (Disassembler.disPC = headAddress; Disassembler.disPC <= tailAddress; ) {
            if (itemRecord == selectedRecord &&  //選択されているレコード
                Disassembler.disPC == XEiJ.regPC) {
              pcPosition = sb.length ();  //pcの位置
            }
            Disassembler.disDisassemble (XEiJ.fmtHex8 (sb.append ("  "),  //字下げ
                                                       Disassembler.disPC).  //アドレス
                                         append ("  "),
                                         Disassembler.disPC, supervisor).  //逆アセンブル
              append ('\n');
            if ((Disassembler.disStatus & Disassembler.DIS_ALWAYS_BRANCH) != 0) {  //完全分岐命令のとき
              sb.append ('\n');  //隙間を空けて読みやすくする
            }
          }
        }

        //項目の終了
        itemNumber++;
        itemRecord++;

      }  //while

      //末尾の番兵
      blgRecordArray[itemNumber] = lastRecord + 1;  //直後のページの先頭のレコードの番号
      blgPositionArray[itemNumber] = sb.length ();
      sb.append (
        lastRecord == newestRecord ?
        Multilingual.mlnJapanese ? "───── 分岐ログの末尾 ─────" : "───── Bottom of the branch log ─────" :
        Multilingual.mlnJapanese ? "↓↓↓↓↓ 次のページ ↓↓↓↓↓" : "↓↓↓↓↓ Next page ↓↓↓↓↓");
      itemNumber++;
      blgPositionArray[itemNumber] = sb.length ();  //テキストの長さ
      blgNumberOfRecords = itemNumber - 2;
      blgNumberOfItems = itemNumber;

      //テキスト
      blgTextArea.setText (sb.toString ());  //テキストを設定する
      blgTextArea.setCaretPosition (pcPosition >= 0 ? pcPosition :  //pcの位置を含んでいるときはpcの位置へ
                                    blgPositionArray[blgSelectedItem]);  //それ以外は選択されているレコードの先頭へキャレットを動かす

    } else if (blgSelectedRecord != selectedRecord) {  //範囲は変わっていないが選択されているレコードが変わった

      blgSelectedRecord = selectedRecord;  //選択されているレコードの番号
      blgSelectedItem = (int) (blgSelectedRecord - blgFirstRecord) + 1;  //選択されている項目の番号
      blgTextArea.setCaretPosition (blgPositionArray[blgSelectedItem]);  //選択されているレコードの先頭へキャレットを動かす

    }

    //範囲も選択されているレコードも変わっていなければキャレットは動かさない
    //  選択されているレコードの中でキャレットは自由に動くことができる

    if (blgModel.getNumber ().longValue () != selectedRecord) {
      blgModel.setValue (Long.valueOf (selectedRecord));  //スピナーの値を設定する。ロックしてあるのでイベントで更新がネストすることはない
    }

    blgLock = false;
  }  //blgUpdate(long)

}  //class BranchLog



