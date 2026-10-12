//========================================================================================
//  DisassembleList.java
//    en:Disassemble list
//    ja:逆アセンブルリスト
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

public class DisassembleList {

  public static final int DDP_ITEM_SIZE = 0x00000002;  //行の最小サイズ
  public static final int DDP_PAGE_SIZE = 0x00000400;  //ページのサイズ
  public static final int DDP_ITEM_MASK = -DDP_ITEM_SIZE;
  public static final int DDP_PAGE_MASK = -DDP_PAGE_SIZE;
  public static final int DDP_MAX_ITEMS = DDP_PAGE_SIZE / DDP_ITEM_SIZE + 2;  //ページの最大項目数。先頭と末尾の番兵を含む

  public static final char[] DDP_MOVEQD0_BASE = (
    //         11111111112222222222333333333344444444445555555555666666666677777777778
    //12345678901234567890123456789012345678901234567890123456789012345678901234567890
    "moveq.l #$xx,d0").toCharArray ();
  public static final char[] DDP_DCW_BASE = (
    //         11111111112222222222333333333344444444445555555555666666666677777777778
    //12345678901234567890123456789012345678901234567890123456789012345678901234567890
    ".dc.w   $xxxx").toCharArray ();

  public static int ddpItemCount;  //ページに含まれる項目の数。先頭と末尾の番兵を含む。0=構築前または再構築要求
  public static int ddpItemIndex;  //キャレットがある項目の番号
  public static int ddpItemAddress;  //キャレットがある項目の先頭アドレス
  public static int ddpPageAddress;  //ページの先頭アドレス
  public static final int[] ddpAddressArray = new int[DDP_MAX_ITEMS];  //項目の先頭アドレスの配列。先頭は前のページの末尾、末尾は次のページの先頭。スピナーのヒント
  public static final int[] ddpSplitArray = new int[DDP_MAX_ITEMS];  //項目を区切る位置の配列。先頭は0
  public static final int[] ddpCaretArray = new int[DDP_MAX_ITEMS];  //項目が選択されたときキャレットを移動させる位置の配列。行の手前にヘッダやラベルなどを挿入しないときはddpSplitArrayと同じ
  public static final boolean[] ddpDCWArray = new boolean[DDP_MAX_ITEMS];  //項目毎の.dc.wで出力したかどうかのフラグの配列。true=数ワード後にある表示しなければならないアドレスを跨がないために逆アセンブルせず.dc.wで出力した。この行がクリックされたとき逆アセンブルし直す

  public static JFrame ddpFrame;  //ウインドウ
  public static ScrollTextArea ddpBoard;  //スクロールテキストエリア
  public static JTextArea ddpTextArea;  //テキストエリア

  public static Hex8Spinner ddpSpinner;  //スピナー

  public static int ddpPopupAddress;  //クリックされた行のアドレス

  public static boolean ddpBacktraceOn;  //true=バックトレース
  public static long ddpBacktraceRecord;  //現在選択されている分岐レコードの通し番号。-1L=未選択
  public static SpinnerNumberModel ddpBacktraceModel;  //バックトレーススピナーのモデル。値はLong
  public static JSpinner ddpBacktraceSpinner;  //バックトレーススピナー
  public static JCheckBox ddpBacktraceCheckBox;  //バックトレースチェックボックス

  public static String ddpStoppedBy;  //停止理由
  public static int ddpStoppedAddress;  //停止位置

  public static int ddpSupervisorMode;  //0=ユーザモード,0以外=スーパーバイザモード
  public static JCheckBox ddpSupervisorCheckBox;  //ユーザ/スーパーバイザチェックボックス

  public static int ddpProgramMode;  //アドレスとオフセット。0=アドレスのみ,1=オフセットのみ,2=両方
  public static RotaryButton ddpProgramButton;  //アドレスとオフセットボタン
  public static JComboBox<String> ddpProgramBox;  //プログラム名コンボボックス
  public static int ddpProgramAge;


  //ddpInit ()
  //  初期化
  public static void ddpInit () {

    ddpItemCount = 0;  //構築前
    ddpItemIndex = 0;
    ddpItemAddress = -1;
    ddpPageAddress = 0;
    ddpSupervisorMode = 1;
    //ddpAddressArray = new int[DDP_MAX_ITEMS];
    //ddpSplitArray = new int[DDP_MAX_ITEMS];
    //ddpCaretArray = new int[DDP_MAX_ITEMS];
    //ddpDCWArray = new boolean[DDP_MAX_ITEMS];

    ddpFrame = null;

  }  //ddpInit()

  //ddpStart ()
  public static void ddpStart () {
    if (RestorableFrame.rfmGetOpened (Settings.SGS_DDP_FRAME_KEY)) {
      ddpOpen (-1, -1, true);
    }
  }  //ddpStart()

  //ddpOpen (address, supervisor, forceUpdate)
  //  逆アセンブルリストウインドウを開く
  //  既に開いているときは手前に持ってくる
  public static void ddpOpen (int address, int supervisor, boolean forceUpdate) {
    if (ddpFrame == null) {
      ddpMake ();
    }
    ddpFrame.setVisible (true);
    ddpBacktraceRecord = -1L;  //分岐レコードの選択を解除する
    LabeledAddress.lblClear ();
    ddpUpdate (address, supervisor, forceUpdate);
    XEiJ.pnlExitFullScreen (false);
    XEiJ.dbgVisibleMask |= XEiJ.DBG_DDP_VISIBLE_MASK;
  }  //ddpOpen(int,int,boolean)

  //ddpMake ()
  //  逆アセンブルリストウインドウを作る
  public static void ddpMake () {

    //スクロールテキストエリア
    ddpBoard = ComponentFactory.setPreferredSize (
      ComponentFactory.setFont (new ScrollTextArea (), LnF.lnfMonospacedFont),
      730, 400);
    ddpBoard.setMargin (new Insets (2, 4, 2, 4));
    ddpBoard.setHighlightCursorOn (true);
    ddpTextArea = ddpBoard.getTextArea ();
    ddpTextArea.setEditable (false);
    ddpTextArea.addMouseWheelListener ((mwe) -> {
      int n = mwe.getWheelRotation ();
      JViewport v = ddpBoard.getViewport ();
      Point p = v.getViewPosition ();
      v.setViewPosition (new Point (p.x,
                                    Math.max (0,
                                              Math.min (ddpTextArea.getSize ().height - v.getExtentSize ().height,
                                                        p.y + n * ddpTextArea.getFont ().getSize () * 5))));
    });

    //スピナー
    ddpSpinner = ComponentFactory.createHex8Spinner (ddpPageAddress, DDP_ITEM_MASK, true, new ChangeListener () {
      //スピナーのチェンジリスナー
      //  スピナーが操作されたとき、そのアドレスの行にテキストエリアのキャレットを移動させる
      //  ページの範囲外になったときはテキストエリアを再構築する
      //  ページの構築中に呼び出されたときは何もしない
      @Override public void stateChanged (ChangeEvent ce) {
        if (XEiJ.dbgEventMask == 0) {  //テキストは構築済みでsetTextの中ではない
          ddpUpdate (ddpSpinner.getAbsoluteValue (), ddpSupervisorMode, false);
        }
      }
    });

    //テキストエリアのキャレットリスナー
    //  テキストエリアがクリックされてキャレットが動いたとき、その行のアドレスをスピナーに設定する
    //  クリックでテキストエリアに移ってしまったフォーカスをスピナーに戻す
    //  ページの構築中に呼び出されたときは何もしない
    //    setText→キャレットリスナー→スピナーのチェンジリスナー→setTextとなるとsetTextの二重呼び出しでエラーが出る
    ComponentFactory.addListener (
      ddpTextArea,
      new CaretListener () {
        @Override public void caretUpdate (CaretEvent ce) {
          if (XEiJ.dbgEventMask == 0) {  //テキストは構築済みでsetTextの中ではない
            int p = ce.getDot ();  //キャレットの位置
            if (p == ce.getMark ()) {  //選択範囲がない
              int i = Arrays.binarySearch (ddpSplitArray, 1, ddpItemCount, p + 1);  //項目の先頭のときも次の項目を検索してから1つ戻る
              i = (i >> 31 ^ i) - 1;  //キャレットがある位置を含む項目の番号
              ddpSpinner.setHintIndex (i);
              //ddpProgramBox.setSelectedIndex (LabeledAddress.lblGetIndex (ddpAddressArray[i]) + 1);
            }
          }
        }
      });

    //テキストエリアのマウスリスナー
    ComponentFactory.addListener (
      ddpTextArea,
      new MouseAdapter () {
        @Override public void mousePressed (MouseEvent me) {
          if (XEiJ.mpuTask == null && me.isPopupTrigger ()) {
            XEiJ.dbgShowPopup (me, ddpTextArea, true);
          }
        }
        @Override public void mouseReleased (MouseEvent me) {
          if (XEiJ.mpuTask == null && me.isPopupTrigger ()) {
            XEiJ.dbgShowPopup (me, ddpTextArea, true);
          }
        }
      });

    //ボタンのアクションリスナー
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        switch (ae.getActionCommand ()) {
        case "Backtrace":
          if (BranchLog.BLG_ON) {
            ddpBacktraceOn = ((JCheckBox) ae.getSource ()).isSelected ();
            if (XEiJ.dbgEventMask == 0) {
              ddpUpdate (ddpAddressArray[ddpItemIndex], ddpSupervisorMode, true);
            }
          }
          break;
        case "User/Supervisor":  //ユーザ/スーパーバイザ
          if (XEiJ.dbgEventMask == 0) {
            ddpUpdate (ddpAddressArray[ddpItemIndex], ((JCheckBox) ae.getSource ()).isSelected () ? 1 : 0, true);
          }
          break;
        case "Address and/or offset":  //アドレスとオフセット
          ddpProgramMode = ddpProgramButton.getIndex ();
          if (XEiJ.dbgEventMask == 0) {
            ddpUpdate (0, ddpSupervisorMode, true);
          }
          break;
        case "Program name":  //プログラム名
          {
            int index = ddpProgramBox.getSelectedIndex ();
            if (0 <= index) {
              if (index == 0) {
                ddpSpinner.setOffset (0);
              } else {
                if (ddpProgramAge == LabeledAddress.lblProgramAge) {  //念の為
                  ddpSpinner.setOffset (LabeledAddress.lblProgramHead[index - 1]);
                }
              }
            }
          }
          break;
        }
      }
    };

    //アドレスとオフセットボタン
    ddpProgramMode = 0;
    ddpProgramButton = Multilingual.mlnToolTipText (
      ComponentFactory.createRotaryButton (
        "Address and/or offset",
        listener,
        ddpProgramMode,
        LnF.LNF_ADDRESS_IMAGE,
        LnF.LNF_OFFSET_IMAGE,
        LnF.LNF_BOTH_IMAGE
        ),
      "ja", "アドレスとオフセット");

    //プログラム名コンボボックス
    ddpProgramBox = Multilingual.mlnToolTipText (
      ComponentFactory.createComboBox (0, "Program name", listener, 16, ""),
      "ja", "プログラム名");
    ddpProgramBox.setMaximumRowCount (16);
    ddpProgramAge = -1;

    //バックトレース
    if (BranchLog.BLG_ON) {
      ddpBacktraceOn = false;
      ddpBacktraceRecord = -1L;  //未選択

      //バックトレーススピナー
      ddpBacktraceModel = new ReverseLongModel (0L, 0L, 0L, 1L);
      ddpBacktraceSpinner = ComponentFactory.createNumberSpinner (ddpBacktraceModel, 10, new ChangeListener () {
        @Override public void stateChanged (ChangeEvent ce) {
          if (XEiJ.dbgEventMask == 0 && XEiJ.mpuTask == null) {  //MPU停止中
            long record = ddpBacktraceModel.getNumber ().longValue ();
            int i = (char) record << BranchLog.BLG_RECORD_SHIFT;
            if (//ddpBacktraceRecord < 0L ||  //現在選択されている分岐レコードがない
                ddpBacktraceRecord < record) {  //後ろへ移動する
              ddpBacktraceRecord = record;
              ddpUpdate (BranchLog.blgArray[i] & ~1, BranchLog.blgArray[i] & 1, false);  //分岐レコードの先頭
            } else if (record < ddpBacktraceRecord) {  //手前へ移動する
              ddpBacktraceRecord = record;
              ddpUpdate (BranchLog.blgArray[i + 1] & ~1, BranchLog.blgArray[i] & 1, false);  //分岐レコードの末尾
            }
          }
        }
      });

      //バックトレースチェックボックス
      ddpBacktraceCheckBox =
        Multilingual.mlnToolTipText (
          ComponentFactory.createIconCheckBox (
            ddpBacktraceOn,
            LnF.LNF_BACK_TRACE_IMAGE,
            LnF.LNF_BACK_TRACE_SELECTED_IMAGE,
            "Backtrace", listener),
          "ja", "バックトレース");
    }

    //スーパーバイザチェックボックス
    ddpSupervisorCheckBox =
      Multilingual.mlnToolTipText (
        ComponentFactory.createIconCheckBox (
          ddpSupervisorMode != 0,
          LnF.LNF_USER_SELECTED_IMAGE,
          LnF.LNF_SUPERVISOR_SELECTED_IMAGE,
          "User/Supervisor", listener),
        "ja", "ユーザ/スーパーバイザ");

    //ウインドウ
    ddpFrame = Multilingual.mlnTitle (
      ComponentFactory.createRestorableSubFrame (
        Settings.SGS_DDP_FRAME_KEY,
        "Disassemble list",
        null,
        ComponentFactory.createBorderPanel (
          //CENTER
          ddpBoard,
          //NORTH
          ComponentFactory.createVerticalBox (
            //1行目
            ComponentFactory.createHorizontalBox (
              Box.createHorizontalGlue (),
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
              ddpProgramButton,
              ddpProgramBox,
              ddpSpinner,
              ddpSupervisorCheckBox,
              Box.createHorizontalStrut (12),
              (BranchLog.BLG_ON ?
               ComponentFactory.createHorizontalBox (
                 ddpBacktraceCheckBox,
                 ddpBacktraceSpinner,
                 Box.createHorizontalStrut (12)) :
               null),
              Box.createHorizontalGlue ()
              )
            )
          )
        ),
      "ja", "逆アセンブルリスト");
    ComponentFactory.addListener (
      ddpFrame,
      new WindowAdapter () {
        @Override public void windowClosing (WindowEvent we) {
          XEiJ.dbgVisibleMask &= ~XEiJ.DBG_DDP_VISIBLE_MASK;
        }
      });

    ddpStoppedBy = null;
    ddpStoppedAddress = -1;

  }  //ddpMake()

  //ddpUpdate (address, supervisor, forceUpdate)
  //  逆アセンブルリストウインドウを更新する
  //  address  -1=pcまたはpc0を表示,0=前回と同じアドレスを表示
  public static void ddpUpdate (int address, int supervisor, boolean forceUpdate) {

    XEiJ.dbgEventMask++;  //構築開始

    if (address == -1) {  //pcまたはpc0を表示
      ddpStoppedAddress = address = ddpStoppedBy == null ? XEiJ.regPC : XEiJ.regPC0;
      forceUpdate = true;
    } else if (address == 0) {  //前回と同じアドレスを表示。同じアドレスで再構築したいときに使う
      address = ddpItemAddress;
    }

    if (supervisor == -1) {
      supervisor = XEiJ.regSRS;
      forceUpdate = true;
    }

    if ((ddpSupervisorMode != 0) != (supervisor != 0)) {  //ユーザ/スーパーバイザが一致しない
      ddpSupervisorMode = supervisor;
      forceUpdate = true;
      if (ddpSupervisorCheckBox.isSelected () != (supervisor != 0)) {
        ddpSupervisorCheckBox.setSelected (supervisor != 0);
      }
    }

    if (forceUpdate) {  //再構築要求
      ddpItemCount = 0;
    }

    address &= DDP_ITEM_MASK;  //目的のアドレスを含む項目の先頭アドレス

    //バックトレース
    if (BranchLog.BLG_ON) {
      if (XEiJ.mpuTask == null) {  //MPU停止中
        long newestRecord = BranchLog.blgNewestRecord;  //最新のレコードの番号
        long oldestRecord = Math.max (0L, newestRecord - 65535);  //最古のレコードの番号
        if (//ddpBacktraceRecord < 0L ||  //レコードが選択されていない
            ddpBacktraceRecord < oldestRecord || newestRecord < ddpBacktraceRecord) {  //選択されているレコードが存在しない
          ddpBacktraceRecord = newestRecord;  //最新のレコードを選択する
          ddpBacktraceModel.setMaximum (Long.valueOf (newestRecord));
          ddpBacktraceModel.setValue (Long.valueOf (newestRecord));
        }
        if (ddpBacktraceOn) {  //バックトレースモードのとき
          int i = (char) ddpBacktraceRecord << BranchLog.BLG_RECORD_SHIFT;
          if (address >>> 1 < BranchLog.blgArray[i] >>> 1) {  //現在選択されているレコードよりも前
            if (oldestRecord < ddpBacktraceRecord) {  //直前にレコードがある
              ddpBacktraceRecord--;  //直前のレコードを選択する
              ddpBacktraceModel.setValue (Long.valueOf (ddpBacktraceRecord));
              address = BranchLog.blgArray[((char) ddpBacktraceRecord << BranchLog.BLG_RECORD_SHIFT) + 1] & ~1;  //直前のレコードの末尾に移動する
            }
          } else if (BranchLog.blgArray[i + 1] >>> 1 < address >>> 1) {  //現在選択されているレコードよりも後
            if (ddpBacktraceRecord < newestRecord) {  //直後にレコードがある
              ddpBacktraceRecord++;  //直後のレコードを選択する
              ddpBacktraceModel.setValue (Long.valueOf (ddpBacktraceRecord));
              address = BranchLog.blgArray[(char) ddpBacktraceRecord << BranchLog.BLG_RECORD_SHIFT] & ~1;  //直後のレコードの先頭に移動する
            }
          }
        }
      }
    }

    if (ddpItemCount != 0) {  //構築前または再構築要求のいずれでもない
      int i = Arrays.binarySearch (ddpAddressArray, 1, ddpItemCount, address + 1);  //項目の先頭のときも次の項目を検索してから1つ戻る
      i = (i >> 31 ^ i) - 1;  //目的のアドレスを含む項目の番号
      if (0 < i && i < ddpItemCount - 1 &&  //ページの内側
          ddpAddressArray[i] == address &&  //項目の先頭
          !ddpDCWArray[i]) {  //.dc.wで出力されていない

        //再構築しない

        ddpItemAddress = address;
        if (ddpItemIndex != i) {  //キャレットがある項目を変更する必要がある
          ddpItemIndex = i;
          ddpTextArea.setCaretPosition (ddpCaretArray[i]);
        }

        //!
        //バックトレースモードのとき分岐レコードの範囲が変わったときはハイライト表示を更新する

        XEiJ.dbgEventMask--;  //構築終了
        return;
      }
    }

    //再構築する
    ddpItemAddress = address;

    //構築前または再構築要求または先頭または末尾の番兵が選択された
    //  0x00000000の境界を跨ぐとき反対側を指すことがあるので先頭と末尾の番兵を区別しない
    ddpPageAddress = address & DDP_PAGE_MASK;  //ページの先頭アドレス
    int pageEndAddress = ddpPageAddress + DDP_PAGE_SIZE;  //ページの末尾アドレス。0になることがある

    //幅を決める
    int dataBytes = 10;  //1行あたりのデータのバイト数
    int addressWidth;  //アドレスの幅
    int codeWidth;  //コードの幅
    if (ddpProgramMode == 0) {  //アドレスのみ
      //          1111111111222222222233333333334444444444555555555566666666667777777777
      //01234567890123456789012345678901234567890123456789012345678901234567890123456789
      //aaaaaaaa  dddddddddddddddddddd  cccccccccccccccccccccccccccccccccccc..........
      //          +0+1+2+3+4+5+6+7+8+9                                              ▲
      //          +0+1+2+3+4+5+6+7+8+9                                              ▼
      addressWidth = 8;
      codeWidth = 36;
    } else if (ddpProgramMode == 1) {  //オフセットのみ
      //          1111111111222222222233333333334444444444555555555566666666667777777777
      //01234567890123456789012345678901234567890123456789012345678901234567890123456789
      //Loooooo  dddddddddddddddddddd  ccccccccccccccccccccccccccccccccccccc..........
      //         +0+1+2+3+4+5+6+7+8+9                                               ▲
      //         +0+1+2+3+4+5+6+7+8+9                                               ▼
      addressWidth = 7;
      codeWidth = 37;
    } else {  //両方
      //          111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999
      //0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789
      //aaaaaaaa:Loooooo  dddddddddddddddddddd  cccccccccccccccccccccccccccccccccccccccccccccccc..........
      //                  +0+1+2+3+4+5+6+7+8+9                                                           ▲
      //                  +0+1+2+3+4+5+6+7+8+9                                                           ▼
      addressWidth = 16;
      codeWidth = 48;
    }
    String header = (new StringBuilder ().
                     append (XEiJ.DBG_SPACES, 0, addressWidth + 2).
                     append ("+0+1+2+3+4+5+6+7+8+9").
                     append (XEiJ.DBG_SPACES, 0, 2 + codeWidth + dataBytes - 2).
                     append ("▲\n")).toString ();
    String footer = (new StringBuilder ().
                     append (XEiJ.DBG_SPACES, 0, addressWidth + 2).
                     append ("+0+1+2+3+4+5+6+7+8+9").
                     append (XEiJ.DBG_SPACES, 0, 2 + codeWidth + dataBytes - 2).
                     append ("▼")).toString ();

    //先頭の番兵
    ddpAddressArray[0] = ddpPageAddress - DDP_ITEM_SIZE;  //昇順を維持するためマスクしない
    ddpSplitArray[0] = 0;
    ddpCaretArray[0] = 0;
    StringBuilder sb = new StringBuilder (header);
    int itemCount = 1;  //項目数
    int itemAddress = ddpPageAddress;  //項目の先頭アドレス
    int dcwAddress = pageEndAddress;  //.dc.wで出力する範囲
    int dcwEndAddress = pageEndAddress;
    boolean prevBranchFlag = false;  //true=直前が完全分岐命令だった

    //ラベル
    if (LabeledAddress.lblProgramCount == 0) {
      LabeledAddress.lblUpdateProgram ();
    }
    if (ddpProgramAge != LabeledAddress.lblProgramAge) {  //更新が必要
      ddpProgramAge = LabeledAddress.lblProgramAge;
      ddpProgramBox.removeAllItems ();
      ddpProgramBox.addItem ("raw address");
      for (int i = 0; i < LabeledAddress.lblProgramCount; i++) {
        ddpProgramBox.addItem (XEiJ.fmtHex8 (new StringBuilder (), LabeledAddress.lblProgramHead[i]).append (' ').append (LabeledAddress.lblProgramName[i]).toString ());
      }
    }

    TreeMap<Integer,InstructionBreakPoint.InstructionBreakRecord> pointTable;
    if (InstructionBreakPoint.IBP_ON) {
      pointTable = InstructionBreakPoint.ibpPointTable;
    }

  itemLoop:
    do {
      int itemEndAddress;  //項目の末尾アドレス
      String code;  //逆アセンブル結果

      //逆アセンブルする
      //  以下のアドレスを跨いでしまうときは逆アセンブルせず1ワードずつ.dc.wまたはmoveqで出力する
      //    目的のアドレス
      //    命令ブレークポイント
      //  途中の行をクリックすることで途中から逆アセンブルし直せるようにするため、複数ワードあっても1行にまとめない
      //  途中に逆アセンブルできる命令があっても、跨いではいけないアドレスまですべて1ワードずつ出力する
      if (dcwAddress <= itemAddress && itemAddress < dcwEndAddress) {  //.dc.wで出力中
        Disassembler.disStatus = 0;  //念のため
        int oc = MC68060.mmuPeekWordZeroCode (itemAddress, supervisor);
        if ((oc & 0xfe00) == 0x7000 && MC68060.mmuPeekWordZeroCode (itemAddress + 2, supervisor) == 0x4e4f) {  //moveq.l #$xx,d0;trap#15
          //pcがIOCSコールのtrap#15を指しているときmoveqが.dc.wになってしまうのを避ける
          XEiJ.fmtHex2 (DDP_MOVEQD0_BASE, 10, oc);
          code = String.valueOf (DDP_MOVEQD0_BASE);
        } else {
          XEiJ.fmtHex4 (DDP_DCW_BASE, 9, oc);
          code = String.valueOf (DDP_DCW_BASE);
        }
        itemEndAddress = itemAddress + 2;
        ddpDCWArray[itemCount] = true;
      } else {  //.dc.wで出力中ではない
        LabeledAddress.lblGetIndex (itemAddress);
        code = Disassembler.disDisassemble (new StringBuilder (), itemAddress, supervisor,
                                            LabeledAddress.lblLastGetHead, LabeledAddress.lblLastGetTail, ddpProgramMode).toString ();  //逆アセンブルする
        for (int t = itemAddress + 2; t < Disassembler.disPC; t += 2) {
          if (t == address ||  //目的のアドレスを跨いでしまった
              InstructionBreakPoint.IBP_ON && pointTable.containsKey (t)) {  //命令ブレークポイントを跨いでしまった
            //!
            //バックトレースモードのとき選択されている分岐レコードの先頭も跨がないようにする
            //  IOCSの_B_READと_B_WRITEでmoveq.l #<data>,d0だけ変更してtrap#15に飛び込んでいるところなど
            dcwAddress = itemAddress;  //.dc.wで出力し直す
            dcwEndAddress = t;
            continue itemLoop;
          }
        }
        itemEndAddress = Disassembler.disPC;
        ddpDCWArray[itemCount] = false;
      }

      //完全分岐命令の下に隙間を空けて読みやすくする
      if (prevBranchFlag) {
        sb.append ('\n');
      }

      //項目の開始
      if (itemAddress == address) {
        ddpItemIndex = itemCount;  //目的のアドレスを含む項目の番号
      }
      ddpAddressArray[itemCount] = itemAddress;  //項目の先頭アドレス
      ddpSplitArray[itemCount] = sb.length ();  //項目を区切る位置

      if (prevBranchFlag) {
        //ラベル
        if (true) {
          int i = sb.length ();
          LabeledAddress.lblSearch (sb, itemAddress);
          if (i < sb.length ()) {
            sb.append ('\n');
          }
        }
      }

      //停止理由
      if (itemAddress == ddpStoppedAddress && ddpStoppedBy != null) {
        sb.append (ddpStoppedBy).append ('\n');
      }

      ddpCaretArray[itemCount] = sb.length ();  //項目が選択されたときキャレットを移動させる位置

      //1行目
      int lineAddress = itemAddress;  //行の開始アドレス
      int lineEndAddress = Math.min (lineAddress + dataBytes, itemEndAddress);  //行の終了アドレス
      {
        //アドレス
        //  アドレスのみまたは両方のとき
        //    アドレスを表示する
        if (ddpProgramMode == 0 ||  //アドレスのみまたは
            ddpProgramMode == 2) {  //両方のとき
          XEiJ.fmtHex8 (sb, lineAddress);  //アドレスを表示する
        }
        //オフセット
        //  オフセットのみまたは両方のとき
        //    プログラムがありかつ先頭が0でないとき
        //      両方のとき
        //        ':'を表示する
        //      'L'とオフセットを表示する
        //    プログラムがないまたは先頭が0のとき
        //      オフセットのみのとき
        //        アドレスを表示する
        //      両方のとき
        //        空白を表示する
        if (ddpProgramMode == 1 ||  //オフセットのみまたは
            ddpProgramMode == 2) {  //両方のとき
          int index = LabeledAddress.lblGetIndex (lineAddress);
          if (index != -1 &&  //プログラムがありかつ
              LabeledAddress.lblLastGetHead != 0) {  //先頭が0でないとき
            if (ddpProgramMode == 2) {  //両方のとき
              sb.append (':');  //':'を表示する
            }
            XEiJ.fmtHex6 (sb.append ('L'), lineAddress - LabeledAddress.lblLastGetHead);  //'L'とオフセットを表示する
          } else {  //プログラムがないまたは先頭が0のとき
            if (ddpProgramMode == 1) {  //オフセットのみのとき
              XEiJ.fmtHex8 (sb, lineAddress);  //アドレスを表示する
            } else {  //両方のとき
              //          Lxxxxxx
              sb.append ("       ");  //空白を表示する
            }
          }
        }
        sb.append ("  ");
      }
      //データ
      for (int a = lineAddress; a < lineEndAddress; a += 2) {
        XEiJ.fmtHex4 (sb, MC68060.mmuPeekWordZeroCode (a, supervisor));
      }
      sb.append (XEiJ.DBG_SPACES, 0, 2 * Math.max (0, lineAddress + dataBytes - lineEndAddress) + 2);
      //逆アセンブル結果
      sb.append (code).append (XEiJ.DBG_SPACES, 0, Math.max (1, codeWidth - code.length ()));
      //キャラクタ
      InstructionBreakPoint.InstructionBreakRecord r = InstructionBreakPoint.IBP_ON ? pointTable.get (itemAddress) : null;
      if (r != null) {  //命令ブレークポイントがある
        if (r.ibrWaitInstruction != null) {  //待機ポイント
          sb.append ("----");
        }
        if (r.ibrThreshold < 0) {  //インスタント
          sb.append ("******");
        } else if (r.ibrThreshold != 0x7fffffff) {
          sb.append (r.ibrValue).append ('/').append (r.ibrThreshold);
        }
      } else {  //命令ブレークポイントがない
        for (int a = lineAddress; a < lineEndAddress; a++) {
          int h = MC68060.mmuPeekByteZeroCode (a, supervisor);
          int c;
          if (0x81 <= h && h <= 0x9f || 0xe0 <= h && h <= 0xef) {  //SJISの2バイトコードの1バイト目
            int l = MC68060.mmuPeekByteZeroCode (a + 1, supervisor);  //これは範囲外になる場合がある
            if (0x40 <= l && l != 0x7f && l <= 0xfc) {  //SJISの2バイトコードの2バイト目
              c = CharacterCode.chrSJISToChar[h << 8 | l];  //2バイトで変換する
              if (c == 0) {  //対応する文字がない
                c = '※';
              }
              a++;
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
      }
      sb.append ('\n');

      //2行目以降
      while (lineEndAddress < itemEndAddress) {
        lineAddress = lineEndAddress;  //行の開始アドレス
        lineEndAddress = Math.min (lineAddress + dataBytes, itemEndAddress);  //行の終了アドレス
        //アドレス
        //  アドレスのみまたは両方のとき
        //    アドレスを表示する
        if (ddpProgramMode == 0 ||  //アドレスのみまたは
            ddpProgramMode == 2) {  //両方のとき
          XEiJ.fmtHex8 (sb, lineAddress);  //アドレスを表示する
        }
        //オフセット
        //  オフセットのみまたは両方のとき
        //    プログラムがありかつ先頭が0でないとき
        //      両方のとき
        //        ':'を表示する
        //      'L'とオフセットを表示する
        //    プログラムがないまたは先頭が0のとき
        //      オフセットのみのとき
        //        アドレスを表示する
        //      両方のとき
        //        空白を表示する
        if (ddpProgramMode == 1 ||  //オフセットのみまたは
            ddpProgramMode == 2) {  //両方のとき
          int index = LabeledAddress.lblGetIndex (lineAddress);
          if (index != -1 &&  //プログラムがありかつ
              LabeledAddress.lblLastGetHead != 0) {  //先頭が0でないとき
            if (ddpProgramMode == 2) {  //両方のとき
              sb.append (':');  //':'を表示する
            }
            XEiJ.fmtHex6 (sb.append ('L'), lineAddress - LabeledAddress.lblLastGetHead);  //'L'とオフセットを表示する
          } else {  //プログラムがないまたは先頭が0のとき
            if (ddpProgramMode == 1) {  //オフセットのみのとき
              XEiJ.fmtHex8 (sb, lineAddress);  //アドレスを表示する
            } else {  //両方のとき
              //          Lxxxxxx
              sb.append ("       ");  //空白を表示する
            }
          }
        }
        sb.append ("  ");
        //データ
        for (int a = lineAddress; a < lineEndAddress; a += 2) {
          XEiJ.fmtHex4 (sb, MC68060.mmuPeekWordZeroCode (a, supervisor));
        }
        sb.append (XEiJ.DBG_SPACES, 0, 2 * Math.max (0, lineAddress + dataBytes - lineEndAddress) + 2 + codeWidth);
        //キャラクタ
        for (int a = lineAddress; a < lineEndAddress; a++) {
          int h = MC68060.mmuPeekByteZeroCode (a, supervisor);
          int c;
          if (0x81 <= h && h <= 0x9f || 0xe0 <= h && h <= 0xef) {  //SJISの2バイトコードの1バイト目
            int l = MC68060.mmuPeekByteZeroCode (a + 1, supervisor);  //これは範囲外になる場合がある
            if (0x40 <= l && l != 0x7f && l <= 0xfc) {  //SJISの2バイトコードの2バイト目
              c = CharacterCode.chrSJISToChar[h << 8 | l];  //2バイトで変換する
              if (c == 0) {  //対応する文字がない
                c = '※';
              }
              a++;
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
      }

      //項目の終了
      itemCount++;
      itemAddress = itemEndAddress;

      //完全分岐命令の下に隙間を空けて読みやすくする
      prevBranchFlag = (Disassembler.disStatus & Disassembler.DIS_ALWAYS_BRANCH) != 0;

    } while (itemAddress < pageEndAddress);

    //末尾の番兵
    ddpAddressArray[itemCount] = itemAddress;  //昇順を維持するためマスクしない
    ddpSplitArray[itemCount] = sb.length ();
    ddpCaretArray[itemCount] = sb.length ();
    sb.append (footer);
    itemCount++;
    ddpItemCount = itemCount;

    //テキスト
    ddpTextArea.setText (sb.toString ());
    ddpTextArea.setCaretPosition (ddpCaretArray[ddpItemIndex]);

    //!
    //バックトレースモードのとき選択されている分岐レコードの範囲をハイライト表示する

    //スピナー
    ddpSpinner.setHintArray (ddpAddressArray, itemCount);
    ddpSpinner.setHintIndex (ddpItemIndex);

    XEiJ.dbgEventMask--;  //構築終了

  }  //ddpUpdate(int,int,boolean)

}  //class DisassembleList



