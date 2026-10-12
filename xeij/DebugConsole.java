//========================================================================================
//  DebugConsole.java
//    en:Debug console -- An interactive debugger
//    ja:デバッグコンソール -- 対話型デバッガ
//  Copyright (C) 2003-2025 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.awt.*;  //BasicStroke,BorderLayout,BoxLayout,Color,Component,Container,Cursor,Desktop,Dimension,Font,FlowLayout,Frame,Graphics,Graphics2D,GraphicsDevice,GraphicsEnvironment,GridLayout,Image,Insets,Paint,Point,Rectangle,RenderingHints,Robot,Shape,Stroke,TexturePaint,Toolkit
import java.awt.datatransfer.*;  //Clipboard,DataFlavor,FlavorEvent,FlavorListener,Transferable,UnsupportedFlavorException
import java.awt.event.*;  //ActionEvent,ActionListener,ComponentAdapter,ComponentEvent,ComponentListener,FocusAdapter,FocusEvent,FocusListener,InputEvent,KeyAdapter,KeyEvent,KeyListener,MouseAdapter,MouseEvent,MouseListener,MouseMotionAdapter,MouseWheelEvent,WindowAdapter,WindowEvent,WindowListener,WindowStateListener
import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,LinkedList,TimeZone,Timer,TimerTask,TreeMap
import javax.swing.*;  //AbstractButton,AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JApplet,JButton,JCheckBox,JCheckBoxMenuItem,JComponent,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException
import javax.swing.event.*;  //CaretEvent,CaretListener,ChangeEvent,ChangeListener,DocumentEvent,DocumentListener,ListSelectionListener

public class DebugConsole {

  public static final int DGT_MAX_OUTPUT_LENGTH = 1024 * 1024;  //出力の上限を1MBとする
  public static final int DGT_CUT_OUTPUT_LENGTH = DGT_MAX_OUTPUT_LENGTH + 1024 * 64;  //出力が上限よりも64KB以上長くなったら上限でカットする

  //入力モード
  public static final int DGT_INPUT_MODE_COMMAND   = 1;  //コマンドモード
  public static final int DGT_INPUT_MODE_PAGE      = 2;  //ページモード
  public static final int DGT_INPUT_MODE_ASSEMBLER = 3;  //アセンブラモード
  public static int dgtInputMode;  //入力モード
  public static LinkedList<String> dgtPageList;

  //プロンプト
  public static final String DGT_COMMAND_PROMPT = "> ";
  public static final String DGT_PAGE_PROMPT = "-- more -- [y/n] ";
  public static String dgtCurrentPrompt;  //現在のプロンプト

  //コンポーネント
  public static JFrame dgtFrame;  //ウインドウ
  public static ScrollTextArea dgtBoard;  //テキストエリア
  public static JPopupMenu dgtPopupMenu;  //ポップアップメニュー
  public static JMenuItem dgtPopupCutMenuItem;  //切り取り
  public static JMenuItem dgtPopupCopyMenuItem;  //コピー
  public static JMenuItem dgtPopupPasteMenuItem;  //貼り付け
  public static JMenuItem dgtPopupSelectAllMenuItem;  //すべて選択
  public static int dgtOutputEnd;  //出力された文字列の末尾。リターンキーが押されたらこれ以降に書かれた文字列をまとめて入力する

  //コアが停止したときコンソールに何を表示するか
  //  bit0  整数レジスタを表示する
  //  bit1  浮動小数点レジスタを表示する
  //  bit2  プロンプトを表示する。レジスタの表示を繰り返すときは最終回だけセットする
  public static volatile int dgtRequestRegs;

  //アセンブル
  public static int dgtAssemblePC;  //次にアセンブルするアドレス
  public static int dgtAssembleFC;  //次にアセンブルするファンクションコード

  //逆アセンブル
  public static int dgtDisassembleLastTail;  //前回逆アセンブルした範囲の終了アドレス
  public static int dgtDisassemblePC;  //前回逆アセンブルした範囲の直後のアドレス。0=PCを使う
  public static int dgtDisassembleFC;  //前回逆アセンブルした範囲のファンクションコード

  //ダンプ
  public static int dgtDumpAddress;  //次回のダンプ開始アドレス
  public static int dgtDumpFunctionCode;  //ファンクションコード
  public static int dgtDumpStraddleChar;  //2行に跨る文字

  //dgtInit ()
  //  デバッグコンソールを初期化する
  public static void dgtInit () {
    dgtInputMode = DGT_INPUT_MODE_COMMAND;
    dgtPageList = null;
    dgtCurrentPrompt = DGT_COMMAND_PROMPT;
    dgtFrame = null;
    dgtBoard = null;
    dgtPopupMenu = null;
    dgtPopupCutMenuItem = null;
    dgtPopupCopyMenuItem = null;
    dgtPopupPasteMenuItem = null;
    dgtPopupSelectAllMenuItem = null;
    dgtOutputEnd = dgtCurrentPrompt.length ();
    dgtRequestRegs = 0;
    dgtAssemblePC = 0;
    dgtAssembleFC = 6;
    dgtDisassembleLastTail = 0;
    dgtDisassemblePC = 0;
    dgtDisassembleFC = 6;
    dgtDumpAddress = 0;
    dgtDumpFunctionCode = 5;
    dgtDumpStraddleChar = 0;
  }  //dgtInit()

  //dgtMake ()
  //  デバッグコンソールを作る
  //  ここでは開かない
  public static void dgtMake () {

    //テキストエリア
    String initialText = (Multilingual.mlnJapanese ?
                          "[ h で使用法を表示]\n" :
                          "[enter h to display usage]\n") + dgtCurrentPrompt;
    dgtOutputEnd = initialText.length ();
    dgtBoard = ComponentFactory.createScrollTextArea (initialText, 500, 600, true);
    dgtBoard.setUnderlineCursorOn (true);
    dgtBoard.setLineWrap (true);  //行を折り返す
    dgtBoard.addDocumentListener (new DocumentListener () {
      @Override public void changedUpdate (DocumentEvent de) {
      }
      @Override public void insertUpdate (DocumentEvent de) {
        if (de.getOffset () < dgtOutputEnd) {
          dgtOutputEnd += de.getLength ();  //出力された文字列の末尾を調整する
        }
      }
      @Override public void removeUpdate (DocumentEvent de) {
        if (de.getOffset () < dgtOutputEnd) {
          dgtOutputEnd -= Math.min (de.getLength (), dgtOutputEnd - de.getOffset ());  //出力された文字列の末尾を調整する
        }
      }
    });
    dgtBoard.addKeyListener (new KeyAdapter () {
      @Override public void keyPressed (KeyEvent ke) {
        int code = ke.getKeyCode ();
        int modifiersEx = ke.getModifiersEx ();
        if (code == KeyEvent.VK_ENTER &&  //Enterキーが押された
            (modifiersEx & (InputEvent.ALT_DOWN_MASK |
                            InputEvent.CTRL_DOWN_MASK |
                            InputEvent.META_DOWN_MASK)) == 0) {  //Altキー,Ctrlキー,Metaキーが押されていない
          if ((modifiersEx & InputEvent.SHIFT_DOWN_MASK) == 0) {  //Shiftキーが押されていない
            ke.consume ();  //Enterキーをキャンセルする
            //Enterキーを処理する
            dgtEnter ();
          } else {  //Shiftキーが押されている
            ke.consume ();  //Enterキーをキャンセルする。デフォルトのShift+Enterの機能を無効化する
            //改行を挿入する
            dgtBoard.replaceRange ("\n", dgtBoard.getSelectionStart (), dgtBoard.getSelectionEnd ());
          }
        }
      }
    });

    //ポップアップメニュー
    ActionListener popupActionListener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        switch (ae.getActionCommand ()) {
        case "Cut":
          dgtCut ();
          break;
        case "Copy":
          dgtCopy ();
          break;
        case "Paste":
          dgtPaste ();
          break;
        case "Select All":
          dgtSelectAll ();
          break;
        }
      }
    };
    dgtPopupMenu = ComponentFactory.createPopupMenu (
      dgtPopupCutMenuItem = Multilingual.mlnText (ComponentFactory.createMenuItem ("Cut", 'T', popupActionListener), "ja", "切り取り"),
      dgtPopupCopyMenuItem = Multilingual.mlnText (ComponentFactory.createMenuItem ("Copy", 'C', popupActionListener), "ja", "コピー"),
      dgtPopupPasteMenuItem = Multilingual.mlnText (ComponentFactory.createMenuItem ("Paste", 'P', popupActionListener), "ja", "貼り付け"),
      ComponentFactory.createHorizontalSeparator (),
      dgtPopupSelectAllMenuItem = Multilingual.mlnText (ComponentFactory.createMenuItem ("Select All", 'A', popupActionListener), "ja", "すべて選択")
      );
    dgtBoard.addMouseListener (new MouseAdapter () {
      @Override public void mousePressed (MouseEvent me) {
        dgtShowPopup (me);
      }
      @Override public void mouseReleased (MouseEvent me) {
        dgtShowPopup (me);
      }
    });

    //ウインドウ
    dgtFrame = Multilingual.mlnTitle (
      ComponentFactory.createRestorableSubFrame (
        Settings.SGS_DGT_FRAME_KEY,
        "Console",
        null,
        dgtBoard
        ),
      "ja", "コンソール");

    dgtBoard.setCaretPosition (dgtOutputEnd);

  }  //dgtMake()

  //dgtShowPopup (me)
  //  ポップアップメニューを表示する
  //  テキストエリアのマウスリスナーが呼び出す
  public static void dgtShowPopup (MouseEvent me) {
    if (me.isPopupTrigger ()) {
      //選択範囲があれば切り取りとコピーが有効
      boolean enableCutAndCopy = XEiJ.clpClipboard != null && dgtBoard.getSelectionStart () != dgtBoard.getSelectionEnd ();
      ComponentFactory.setEnabled (dgtPopupCutMenuItem, enableCutAndCopy);
      ComponentFactory.setEnabled (dgtPopupCopyMenuItem, enableCutAndCopy);
      //クリップボードに文字列があれば貼り付けが有効
      ComponentFactory.setEnabled (dgtPopupPasteMenuItem, XEiJ.clpClipboard != null && XEiJ.clpClipboard.isDataFlavorAvailable (DataFlavor.stringFlavor));
      //クリップボードがあればすべて選択が有効
      ComponentFactory.setEnabled (dgtPopupSelectAllMenuItem, XEiJ.clpClipboard != null);
      //ポップアップメニューを表示する
      dgtPopupMenu.show (me.getComponent (), me.getX (), me.getY ());
    }
  }  //dgtShowPopup(MouseEvent)

  //dgtCut ()
  //  切り取り
  public static void dgtCut () {
    if (XEiJ.clpClipboard != null) {
      //選択範囲の文字列をコピーする
      XEiJ.clpClipboardString = dgtBoard.getSelectedText ();
      try {
        XEiJ.clpClipboard.setContents (XEiJ.clpStringContents, XEiJ.clpClipboardOwner);
        XEiJ.clpIsClipboardOwner = true;  //自分がコピーした
      } catch (Exception e) {
        return;
      }
      //選択範囲の文字列を削除する
      dgtBoard.replaceRange ("", dgtBoard.getSelectionStart (), dgtBoard.getSelectionEnd ());
    }
  }  //dgtCut()

  //dgtCopy ()
  //  コピー
  public static void dgtCopy () {
    if (XEiJ.clpClipboard != null) {
      //選択範囲の文字列をコピーする
      String selectedText = dgtBoard.getSelectedText ();
      if (selectedText != null) {
        XEiJ.clpClipboardString = selectedText;
        try {
          XEiJ.clpClipboard.setContents (XEiJ.clpStringContents, XEiJ.clpClipboardOwner);
          XEiJ.clpIsClipboardOwner = true;  //自分がコピーした
        } catch (Exception e) {
          return;
        }
      }
    }
  }  //dgtCopy()

  //dgtPaste ()
  //  貼り付け
  public static void dgtPaste () {
    if (XEiJ.clpClipboard != null) {
      //クリップボードから文字列を取り出す
      String string = null;
      try {
        string = (String) XEiJ.clpClipboard.getData (DataFlavor.stringFlavor);
      } catch (Exception e) {
        return;
      }
      //選択範囲の文字列を置換する
      dgtBoard.replaceRange (string, dgtBoard.getSelectionStart (), dgtBoard.getSelectionEnd ());
    }
  }  //dgtPaste()

  //dgtSelectAll ()
  //  すべて選択
  public static void dgtSelectAll () {
    if (XEiJ.clpClipboard != null) {
      //すべて選択する
      dgtBoard.selectAll ();
    }
  }  //dgtSelectAll()

  //dgtStart ()
  public static void dgtStart () {
    if (RestorableFrame.rfmGetOpened (Settings.SGS_DGT_FRAME_KEY)) {
      dgtOpen ();
    }
  }  //dgtStart()

  //dgtOpen ()
  //  デバッグコンソールを開く
  public static void dgtOpen () {
    if (dgtFrame == null) {
      dgtMake ();
    }
    XEiJ.pnlExitFullScreen (false);
    dgtFrame.setVisible (true);
  }  //dgtOpen()

  //dgtPrintChar (c)
  //  末尾に1文字追加する
  public static void dgtPrintChar (int c) {
    if (c == 0x08) {  //バックスペース
      if (0 < dgtOutputEnd) {
        if (dgtBoard != null) {
          dgtBoard.replaceRange ("", dgtOutputEnd - 1, dgtOutputEnd);  //1文字削除
          dgtOutputEnd--;
          dgtBoard.setCaretPosition (dgtOutputEnd);
        }
      }
    } else if (0x20 <= c && c != 0x7f || c == 0x09 || c == 0x0a) {  //タブと改行以外の制御コードを除く
      if (dgtBoard != null) {
        dgtBoard.insert (String.valueOf ((char) c), dgtOutputEnd);  //1文字追加
        dgtOutputEnd++;
        if (DGT_CUT_OUTPUT_LENGTH <= dgtOutputEnd) {
          dgtBoard.replaceRange ("", 0, dgtOutputEnd - DGT_MAX_OUTPUT_LENGTH);  //先頭を削って短くする
          dgtOutputEnd = DGT_MAX_OUTPUT_LENGTH;
        }
        dgtBoard.setCaretPosition (dgtOutputEnd);
      }
    }
  }  //dgtPrintChar(int)

  //dgtPrint (s)
  //  末尾に文字列を追加する
  //  制御コードを処理しないのでタブと改行以外の制御コードを含めないこと
  public static void dgtPrint (String s) {
    if (s == null) {
      return;
    }
    if (dgtFrame != null) {
      dgtBoard.insert (s, dgtOutputEnd);  //文字列追加
      dgtOutputEnd += s.length ();
      if (DGT_CUT_OUTPUT_LENGTH <= dgtOutputEnd) {
        dgtBoard.replaceRange ("", 0, dgtOutputEnd - DGT_MAX_OUTPUT_LENGTH);  //先頭を削って短くする
        dgtOutputEnd = DGT_MAX_OUTPUT_LENGTH;
      }
      dgtBoard.setCaretPosition (dgtOutputEnd);
    }
  }  //dgtPrint(String)

  //dgtPrintln (s)
  //  末尾に文字列と改行を追加する
  //  制御コードを処理しないのでタブと改行以外の制御コードを含めないこと
  public static void dgtPrintln (String s) {
    dgtPrint (s);
    dgtPrintChar ('\n');
  }  //dgtPrintln(String)

  //dgtEnter ()
  //  Enterキーを処理する
  public static void dgtEnter () {
    String text = dgtBoard.getText ();  //テキスト全体
    int length = text.length ();  //テキスト全体の長さ
    int outputLineStart = text.lastIndexOf ('\n', dgtOutputEnd - 1) + 1;  //出力の末尾の行の先頭。プロンプトの先頭
    int caretLineStart = text.lastIndexOf ('\n', dgtBoard.getCaretPosition () - 1) + 1;  //キャレットがある行の先頭
    if (outputLineStart <= caretLineStart) {  //出力の末尾の行の先頭以降でEnterキーが押された
      dgtBoard.replaceRange ("", dgtOutputEnd, length);  //入力された文字列を一旦削除する
      dgtSend (text.substring (dgtOutputEnd, length));  //入力された文字列を送信する
    } else if (outputLineStart < dgtOutputEnd) {  //出力の末尾の行の先頭よりも手前でEnterキーが押されて、出力の末尾の行にプロンプトがあるとき
      String prompt = text.substring (outputLineStart, dgtOutputEnd);  //出力の末尾の行のプロンプト
      int caretLineEnd = text.indexOf ('\n', caretLineStart);  //キャレットがある行の末尾
      if (caretLineEnd == -1) {
        caretLineEnd = length;
      }
      String line = text.substring (caretLineStart, caretLineEnd);  //キャレットがある行
      int start = line.indexOf (prompt);  //キャレットがある行のプロンプトの先頭
      if (0 <= start) {  //キャレットがある行にプロンプトがあるとき
        dgtOutputEnd = length;  //入力された文字列を無効化する
        if (text.charAt (dgtOutputEnd - 1) != '\n' && !text.endsWith ("\n" + prompt)) {  //改行または改行+プロンプトで終わっていないとき
          dgtBoard.insert ("\n", dgtOutputEnd);  //末尾に改行を追加する
          dgtOutputEnd++;
          if (DGT_CUT_OUTPUT_LENGTH <= dgtOutputEnd) {
            dgtBoard.replaceRange ("", 0, dgtOutputEnd - DGT_MAX_OUTPUT_LENGTH);  //先頭を削って短くする
            dgtOutputEnd = DGT_MAX_OUTPUT_LENGTH;
          }
        }
        dgtBoard.setCaretPosition (dgtOutputEnd);
        dgtSend (line.substring (start + prompt.length ()));  //プロンプトの後ろから行の末尾までを送信する
      }
    }
  }  //dgtEnter()

  //dgtSend (s)
  //  入力された文字列を処理する
  public static void dgtSend (String s) {
    dgtPrintln (s);
    if (dgtInputMode == DGT_INPUT_MODE_COMMAND) {  //コマンドモード
      ExpressionEvaluator.ExpressionElement nodeTree = XEiJ.fpuBox.evxParse (s, ExpressionEvaluator.EVM_COMMAND);
      if (nodeTree != null) {
        nodeTree.exlEval (ExpressionEvaluator.EVM_COMMAND);
        if (nodeTree.exlValueType == ExpressionEvaluator.ElementType.ETY_FLOAT) {
          dgtPrintln (nodeTree.exlFloatValue.toString ());
        } else if (nodeTree.exlValueType == ExpressionEvaluator.ElementType.ETY_STRING) {
          dgtPrintln (nodeTree.exlStringValue);
        }
      }
    } else if (dgtInputMode == DGT_INPUT_MODE_PAGE) {  //ページモード
      if (!(s.equals ("") ||
            s.toLowerCase ().startsWith (" ") ||
            s.toLowerCase ().startsWith ("y"))) {
        dgtPageList = null;
      }
      dgtPrintPage ();
    } else if (dgtInputMode == DGT_INPUT_MODE_ASSEMBLER) {  //アセンブラモード
      if (s.equals (".")) {  //コマンドモードに戻る
        dgtInputMode = DGT_INPUT_MODE_COMMAND;
        dgtCurrentPrompt = DGT_COMMAND_PROMPT;
      } else {  //アセンブルする
        byte[] binary = Assembler.asmAssemble (dgtAssemblePC, s);
        if (binary != null && 0 < binary.length) {  //バイナリがある
          for (int i = 0; i < binary.length; i++) {
            MC68060.mmuPokeByte (dgtAssemblePC + i, binary[i], dgtAssembleFC);
          }
          //逆アセンブルする
          int itemAddress = dgtAssemblePC;
          dgtAssemblePC += binary.length;
          dgtMakeAssemblerPrompt ();
          int supervisor = DebugConsole.dgtAssembleFC & 4;
          StringBuilder sb = new StringBuilder ();
          while (itemAddress < dgtAssemblePC) {
            String code = Disassembler.disDisassemble (new StringBuilder (), itemAddress, supervisor).toString ();
            int itemEndAddress = Disassembler.disPC;
            if (dgtAssemblePC < itemEndAddress) {  //アセンブルした範囲からはみ出すときdc.wに置き換える
              itemEndAddress = dgtAssemblePC;
              StringBuilder sb2 = new StringBuilder ();
              sb2.append ("dc.w    ");
              for (int a = itemAddress; a < itemEndAddress; a += 2) {
                if (itemAddress < a) {
                  sb2.append (',');
                }
                XEiJ.fmtHex4 (sb2.append ('$'), MC68060.mmuPeekWordZeroCode (a, supervisor));
              }
              code = sb2.toString ();
            }
            //1行目
            int lineAddress = itemAddress;  //行の開始アドレス
            int lineEndAddress = Math.min (lineAddress + 10, itemEndAddress);  //行の終了アドレス
            //アドレス
            XEiJ.fmtHex8 (sb, lineAddress).append ("  ");
            //データ
            for (int a = lineAddress; a < lineEndAddress; a += 2) {
              XEiJ.fmtHex4 (sb, MC68060.mmuPeekWordZeroCode (a, supervisor));
            }
            sb.append (XEiJ.DBG_SPACES, 0, 2 * Math.max (0, lineAddress + 10 - lineEndAddress) + 2);
            //逆アセンブル結果
            sb.append (code).append ('\n');
            //2行目以降
            while (lineEndAddress < itemEndAddress) {
              lineAddress = lineEndAddress;  //行の開始アドレス
              lineEndAddress = Math.min (lineAddress + 10, itemEndAddress);  //行の終了アドレス
              //アドレス
              XEiJ.fmtHex8 (sb, lineAddress).append ("  ");
              //データ
              for (int a = lineAddress; a < lineEndAddress; a += 2) {
                XEiJ.fmtHex4 (sb, MC68060.mmuPeekWordZeroCode (a, supervisor));
              }
              sb.append ('\n');
            }  //while
            itemAddress = itemEndAddress;
          }  //while itemAddress<dgtAssemblePC
          dgtPrint (sb.toString ());
        }  //if バイナリがある
      }  //if コマンドモードに戻る/アセンブルする
    }  //if コマンドモード/ページモード/アセンブラモード
    if (dgtRequestRegs == 0) {
      dgtPrintPrompt ();
    }
  }  //dgtSend(String)

  //dgtMakeAssemblerPrompt ()
  //  アセンブラモードのプロンプトを作る
  public static void dgtMakeAssemblerPrompt () {
    StringBuilder sb = XEiJ.fmtHex8 (new StringBuilder (), dgtAssemblePC);
    if (Model.MPU_MC68LC040 <= XEiJ.currentMPU) {
      sb.append ('@').append (dgtAssembleFC);
    }
    dgtCurrentPrompt = sb.append ("  ").toString ();
  }  //dgtMakeAssemblerPrompt()

  //dgtPrintPrompt (s)
  //  プロンプトを表示する
  //  既に表示されているときは何もしない
  public static void dgtPrintPrompt () {
    String text = dgtBoard.getText ();  //テキスト全体
    if (!text.substring (text.lastIndexOf ('\n', dgtOutputEnd - 1) + 1, dgtOutputEnd).equals (dgtCurrentPrompt)) {  //プロンプトが表示されていない
      dgtPrint (text.endsWith ("\n") ? dgtCurrentPrompt : "\n" + dgtCurrentPrompt);
    }
  }  //dgtPrintPrompt()

  //dgtPrintPage ()
  //  ページを表示する
  public static void dgtPrintPage () {
    if (dgtPageList != null && !dgtPageList.isEmpty ()) {  //ページがある
      dgtPrint (dgtPageList.pollFirst ());
      if (!dgtPageList.isEmpty ()) {  //次のページがある
        //ページモードに移行する
        DebugConsole.dgtInputMode = DebugConsole.DGT_INPUT_MODE_PAGE;
        dgtCurrentPrompt = DGT_PAGE_PROMPT;
      }
    }
    if (dgtPageList == null || dgtPageList.isEmpty ()) {  //ページがない
      dgtPageList = null;
      //コマンドモードに戻る
      dgtInputMode = DGT_INPUT_MODE_COMMAND;
      dgtCurrentPrompt = DGT_COMMAND_PROMPT;
    }
    dgtPrintPrompt ();
  }  //dgtPrintPage()

}  //class DebugConsole



