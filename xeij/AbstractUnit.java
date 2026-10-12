//========================================================================================
//  AbstractUnit.java
//    en:Abstract unit -- It is the super class of the floppy disk unit, hard disk unit, and so on.
//    ja:抽象ユニット -- フロッピーディスクユニットやハードディスクユニットなどのスーパークラスです。
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
import javax.swing.*;  //AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JButton,JCheckBox,JCheckBoxMenuItem,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException

public abstract class AbstractUnit {

  //ユニットの状態
  protected static final int ABU_WRONG_INSERTION = 0b00000001;  //誤挿入
  protected static final int ABU_INSERTED        = 0b00000010;  //メディア挿入
  protected static final int ABU_NOT_READY       = 0b00000100;  //ノットレディ
  protected static final int ABU_WRITE_PROTECTED = 0b00001000;  //プロテクト
  protected static final int ABU_USER_PREVENTED  = 0b00010000;  //ユーザによるイジェクト禁止
  protected static final int ABU_BUFFERED        = 0b00100000;  //バッファあり
  protected static final int ABU_EJECT_PREVENTED = 0b01000000;  //イジェクト禁止
  protected static final int ABU_BLINKING        = 0b10000000;  //LED点滅

  protected int abuNumber;  //ユニット番号
  protected int abuApparentNumber;  //見かけのユニット番号

  protected int abuNumberOfModes;  //モードの数。ユニット毎に定数でなければならない
  protected int abuCurrentMode;  //現在のモード。0～モードの数-1
  protected JButton abuModeButton;  //モードボタン

  protected boolean abuConnected;  //true=接続されている
  protected boolean abuInserted;  //true=挿入されている
  protected boolean abuWriteProtected;  //true=書き込みが禁止されている
  protected boolean abuBuffered;  //true=バッファあり
  protected boolean abuEjectPrevented;  //true=イジェクトが禁止されている

  protected boolean abuDisconnectable;  //true=切り離せる
  protected boolean abuUnprotectable;  //true=書き込みを許可できる

  protected boolean abuEjected;  //true=前回の検査以降にイジェクトされた

  protected String abuPath;  //フルパスファイル名

  protected Box abuMenuBox;
  protected JCheckBox abuConnectCheckBox;
  protected JCheckBox abuProtectCheckBox;
  protected JButton abuOpenButton;
  protected JTextField abuOpenTextField;
  protected JButton abuEjectButton;

  //numberOfModes = unit.abuGetNumberOfModes ()
  //  モードの数を返す
  //  モードボタンを表示するときオーバーライドする
  public int abuGetNumberOfModes () {
    return 0;
  }  //unit.abuGetNumberOfModes()

  //image = unit.abuGetModeIcon (mode, enabled)
  //  モードボタンのアイコンを返す
  //  モードボタンを表示するときオーバーライドする
  public ImageIcon abuGetModeIcon (int mode, boolean enabled) {
    return null;
  }  //unit.abuGetModeIcon(int,boolean)

  //text = unit.abuGetModeTextEn (mode, enabled)
  //  モードボタンの英語のツールチップテキストを返す
  //  モードボタンを表示するときオーバーライドする
  public String abuGetModeTextEn (int mode, boolean enabled) {
    return (enabled ? "Mode " + mode + " / Change mode to " + (mode + 1 < abuNumberOfModes ? mode + 1 : 0) :
            "Mode " + mode);
  }  //unit.abuGetModeTextEn(int,boolean)

  //text = unit.abuGetModeTextJa (mode, enabled)
  //  モードボタンの日本語のツールチップテキストを返す
  //  モードボタンを表示するときオーバーライドする
  public String abuGetModeTextJa (int mode, boolean enabled) {
    return (enabled ? "モード " + mode + " / モード " + (mode + 1 < abuNumberOfModes ? mode + 1 : 0) + " に切り替える" :
            "モード " + mode);
  }  //unit.abuGetModeTextJa(int,boolean)

  //unit.abuSetMode (mode)
  //  モードを切り替える
  //  これは挿入したまま切り替えられる
  public void abuSetMode (int mode) {
    abuCurrentMode = mode;  //モードを切り替える
    abuUpdateModeButton ();  //モードボタンを更新する
  }  //unit.abuSetMode(int)

  //unit.abuUpdateModeButton ()
  //  モードボタンを更新する
  public void abuUpdateModeButton () {
    if (abuNumberOfModes > 0) {  //モードボタンがある
      boolean enabled = abuConnected && !abuInserted;  //接続されていて挿入されていないときモードを切り替えられる
      abuModeButton.setEnabled (enabled);
      abuModeButton.setIcon (abuGetModeIcon (abuCurrentMode, true));
      abuModeButton.setDisabledIcon (abuGetModeIcon (abuCurrentMode, false));
      Multilingual.mlnToolTipText (abuModeButton,
                           "en", abuGetModeTextEn (abuCurrentMode, enabled),
                           "ja", abuGetModeTextJa (abuCurrentMode, enabled));
    }
  }  //unit.abuSetMode(int)


  //box = unit.getMenuBox ()
  //  メニューの箱を返す
  public Box getMenuBox () {
    return abuMenuBox;
  }  //unit.getMenuBox()

  //connected = unit.isConnected ()
  //  ユニットが接続されているかどうか
  public boolean isConnected () {
    return abuConnected;
  }  //unit.isConnected()

  //ejected = unit.hasBeenEjected ()
  //  前回の検査以降にイジェクトされた
  protected boolean hasBeenEjected () {
    boolean t = abuEjected;
    abuEjected = false;
    return t;
  }  //unit.hasBeenEjected()

  //new AbstractUnit (number)
  //new AbstractUnit (number, apparentNumber)
  //  コンストラクタ
  protected AbstractUnit (int number) {
    this (number, number);
  }
  @SuppressWarnings ("this-escape") protected AbstractUnit (int number, int apparentNumber) {
    abuNumber = number;
    abuApparentNumber = apparentNumber;

    abuNumberOfModes = abuGetNumberOfModes ();  //[this-escape]
    abuCurrentMode = 0;

    abuConnected = false;
    abuInserted = false;
    abuWriteProtected = false;
    abuBuffered = false;
    abuEjectPrevented = false;

    abuDisconnectable = true;
    abuUnprotectable = true;

    abuEjected = false;

    abuPath = "";

    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        String command = ae.getActionCommand ();
        switch (command) {
        case "connect":  //接続/切り離し
          if (abuConnectCheckBox.isSelected ()) {
            connect (true);
          } else {
            disconnect ();
          }
          break;
        case "mode":  //モード切り替え
          if (abuConnected && !abuInserted) {  //接続されていて挿入されていない
            abuSetMode (abuCurrentMode + 1 < abuNumberOfModes ? abuCurrentMode + 1 : 0);  //モードを切り替える
          }
          break;
        case "eject":  //イジェクト
          eject ();  //イジェクトする
          break;
        case "open":  //開く
          open ();  //開くダイアログを開く
          break;
        case "protect":  //書き込み禁止/書き込み許可
          if (abuProtectCheckBox.isSelected ()) {
            protect (true);  //開いてから書き込みを禁止した場合は書き込みを許可できる
          } else {
            unprotect ();
          }
          break;
        }
      }
    };

    abuMenuBox = ComponentFactory.createHorizontalBox (
      abuConnectCheckBox = Multilingual.mlnToolTipText (
        ComponentFactory.setEnabled (
          ComponentFactory.createIconCheckBox (false,
                                   LnF.LNF_NUMBER_IMAGE_ARRAY[abuApparentNumber], LnF.LNF_NUMBER_SELECTED_IMAGE_ARRAY[abuApparentNumber],
                                   LnF.LNF_NUMBER_IMAGE_ARRAY[abuApparentNumber], LnF.LNF_NUMBER_SELECTED_IMAGE_ARRAY[abuApparentNumber],
                              "connect", listener),
          true),
        "en", "Connect", "ja", "接続する")  //接続チェックボックス
      );

    //モードボタン
    if (abuNumberOfModes > 0) {
      boolean enabled = abuConnected && !abuInserted;  //接続されていて挿入されていない
      abuModeButton = Multilingual.mlnToolTipText (
        ComponentFactory.setEnabled (
          ComponentFactory.createIconButton (abuGetModeIcon (abuCurrentMode, true),
                                 abuGetModeIcon (abuCurrentMode, false),
                                 "mode",
                                 listener),
          enabled),
        "en", abuGetModeTextEn (abuCurrentMode, enabled),
        "ja", abuGetModeTextJa (abuCurrentMode, enabled)
        );
      ComponentFactory.addComponents (
        abuMenuBox,
        abuModeButton
        );
    }

    //イジェクトボタン
    abuEjectButton = Multilingual.mlnToolTipText (
      ComponentFactory.setEnabled (
        ComponentFactory.createImageButton (LnF.LNF_EJECT_IMAGE, LnF.LNF_EJECT_DISABLED_IMAGE, "eject", listener),
        false),
      "en", null, "ja", null);

    //開くボタン
    abuOpenButton = Multilingual.mlnToolTipText (
      ComponentFactory.setEnabled (
        ComponentFactory.setPreferredSize (
          ComponentFactory.setText (
            ComponentFactory.createButton (LnF.LNF_OPEN_IMAGE, LnF.LNF_OPEN_DISABLED_IMAGE, "open", listener),
            ""),
          LnF.lnfFontSize + 4, LnF.lnfFontSize + 4),
        false),
      "en", null, "ja", null);
    abuOpenTextField = ComponentFactory.addListener (
      ComponentFactory.setEditable (
        ComponentFactory.setPreferredSize (
          ComponentFactory.setHorizontalAlignment (
            ComponentFactory.createTextField ("", 20),
            SwingConstants.CENTER),
          144, LnF.lnfFontSize + 4),
        false),
      new MouseAdapter () {
        @Override public void mouseClicked (MouseEvent me) {
          if (!abuInserted) {  //挿入されていないとき
            abuOpenButton.doClick ();
          }
        }
      });

    //書き込み禁止ボタン
    abuProtectCheckBox = Multilingual.mlnToolTipText (
      ComponentFactory.setEnabled (
        ComponentFactory.createIconCheckBox (true,
                                             LnF.LNF_PROTECT_IMAGE, LnF.LNF_PROTECT_SELECTED_IMAGE,
                                             LnF.LNF_PROTECT_DISABLED_IMAGE, LnF.LNF_PROTECT_DISABLED_SELECTED_IMAGE,
                                             "protect", listener),
        false),
      "en", null, "ja", null);

    ComponentFactory.addComponents (
      abuMenuBox,
      abuEjectButton,
      abuOpenButton,
      abuOpenTextField,
      Box.createHorizontalGlue (),
      abuProtectCheckBox
      );

  }  //new AbstractUnit(int)

  //unit.connect (disconnectable)
  //  接続する
  protected void connect (boolean disconnectable) {
    if (abuConnected) {  //既に接続されている
      return;
    }
    abuConnected = true;  //接続する
    abuDisconnectable = disconnectable;  //切り離せるかどうか
    abuConnectCheckBox.setSelected (true);  //接続されていることを示す
    abuConnectCheckBox.setEnabled (disconnectable);  //接続してまだ開いていないので切り離しが許可されていれば切り離せるようになる
    Multilingual.mlnToolTipText (abuConnectCheckBox, "en", disconnectable ? "Disconnect" : null, "ja", disconnectable ? "切り離す" : null);
    abuSetMode (abuCurrentMode);  //モードボタンを更新する
    abuOpenButton.setEnabled (true);  //接続されたので開けるようになる
    Multilingual.mlnToolTipText (abuOpenButton,
                                 "en", "Open",
                                 "ja", "開く");
  }  //unit.connect(boolean)

  //unit.disconnect ()
  //  切り離す
  protected void disconnect () {
    if (!abuConnected ||  //接続されていない
        abuInserted ||  //挿入されている
        !abuDisconnectable) {  //切り離せない
      return;
    }
    abuConnected = false;  //切り離す
    abuConnectCheckBox.setSelected (false);  //接続されていないことを示す
    Multilingual.mlnToolTipText (abuConnectCheckBox, "en", "Connect", "ja", "接続する");
    abuSetMode (abuCurrentMode);  //モードボタンを更新する
    abuOpenButton.setEnabled (false);  //切り離されたので開けなくなる
    Multilingual.mlnToolTipText (abuOpenButton, "en", null, "ja", null);
  }  //unit.disconnect()

  //unit.protect (unprotectable)
  //  書き込みを禁止する
  protected void protect (boolean unprotectable) {
    if (!abuInserted ||  //挿入されていない
        abuWriteProtected) {  //既に書き込みが禁止されている
      return;
    }
    abuWriteProtected = true;  //書き込みを禁止する
    abuUnprotectable = unprotectable;  //書き込みを許可できるかどうか
    abuProtectCheckBox.setSelected (true);  //書き込みが禁止されていることを示す
    abuProtectCheckBox.setEnabled (unprotectable);  //書き込みを許可できるかどうか
    Multilingual.mlnToolTipText (abuProtectCheckBox, "en", "Write-Protected", "ja", "書き込みが禁止されています");
  }  //unit.protect(boolean)

  //unit.unprotect ()
  //  書き込みを許可する
  protected void unprotect () {
    if (!abuInserted ||  //挿入されていない
        !abuWriteProtected ||  //既に書き込みが許可されている
        !abuUnprotectable) {  //書き込みを許可できない
      return;
    }
    abuWriteProtected = false;  //書き込みを許可する
    abuProtectCheckBox.setSelected (false);  //書き込みが許可されていることを示す
    Multilingual.mlnToolTipText (abuProtectCheckBox, "en", "Write-Enabled", "ja", "書き込みが許可されています");
  }  //unit.unprotect()

  //unit.prevent ()
  //  イジェクトを禁止する
  protected void prevent () {
    if (!abuConnected ||  //接続されていない
        !abuInserted ||  //挿入されていない
        abuEjectPrevented) {  //既にイジェクトが禁止されている
      return;
    }
    abuEjectPrevented = true;  //イジェクトを禁止する
    abuOpenButton.setEnabled (false);  //イジェクトが禁止されたので開けなくなる
    Multilingual.mlnToolTipText (abuOpenButton, "en", null, "ja", null);
    abuEjectButton.setEnabled (false);  //イジェクトが禁止されたのでイジェクトできなくなる
    Multilingual.mlnToolTipText (abuEjectButton, "en", null, "ja", null);
  }  //unit.prevent()

  //unit.allow ()
  //  イジェクトを許可する
  protected void allow () {
    if (!abuConnected ||  //接続されていない
        !abuInserted ||  //挿入されていない
        !abuEjectPrevented) {  //既にイジェクトが許可されている
      return;
    }
    abuEjectPrevented = false;  //イジェクトを許可する
    abuOpenButton.setEnabled (true);  //イジェクトが許可されたので開けるようになる
    Multilingual.mlnToolTipText (abuOpenButton,
                                 "en", "Open",
                                 "ja", "開く");
    abuEjectButton.setEnabled (true);  //イジェクトが許可されたのでイジェクトできるようになる
    Multilingual.mlnToolTipText (abuEjectButton, "en", "Eject", "ja", "イジェクトする");
  }  //unit.allow()

  //success = unit.eject ()
  //  イジェクトする
  protected boolean eject () {
    if (!abuConnected ||  //接続されていない
        abuEjectPrevented) {  //イジェクトが禁止されている
      return false;
    }
    if (!abuInserted) {  //挿入されていない
      return true;  //挿入されていないときイジェクトは常に成功する
    }
    abuInserted = false;  //イジェクトする
    abuWriteProtected = false;  //書き込みを許可する
    abuBuffered = false;
    abuEjected = true;  //前回の検査以降にイジェクトされた
    abuPath = "";
    abuOpenTextField.setText ("");  //パスの表示を更新する
    abuConnectCheckBox.setEnabled (abuDisconnectable);  //イジェクトされたので切り離しが許可されていれば切り離せるようになる
    Multilingual.mlnToolTipText (abuConnectCheckBox, "en", abuDisconnectable ? "Disconnect" : null, "ja", abuDisconnectable ? "切り離す" : null);
    abuSetMode (abuCurrentMode);  //モードボタンを更新する
    abuProtectCheckBox.setSelected (true);  //イジェクトされたので書き込めなくなる
    abuProtectCheckBox.setEnabled (false);  //イジェクトされたので書き込みを許可できなくなる
    Multilingual.mlnToolTipText (abuProtectCheckBox, "en", null, "ja", null);
    abuEjectButton.setEnabled (false);  //イジェクトされたのでイジェクトできなくなる
    Multilingual.mlnToolTipText (abuEjectButton, "en", null, "ja", null);
    return true;
  }  //unit.eject()

  //success = unit.open ()
  //  開くダイアログを開く
  //  super.open()を呼び出して成功したときだけ開くダイアログを開くこと
  protected boolean open () {
    if (!abuConnected ||  //接続されていない
        abuInserted && abuEjectPrevented) {  //挿入されていてイジェクトが禁止されている
      return false;
    }
    return eject ();  //挿入されていたらイジェクトする
  }  //unit.open()

  //success = unit.insert (path, writeProtected)
  //  挿入する
  protected boolean insert (String path, boolean writeProtected) {
    if (path.length () == 0 ||  //パスが指定されていない
        !abuConnected ||  //接続されていない
        abuInserted && abuEjectPrevented) {  //挿入されていてイジェクトが禁止されている
      return false;
    }
    abuInserted = true;  //挿入する
    abuPath = path;
    String tempPath = abuPath;
    while (2 < tempPath.length () &&
           (tempPath.endsWith ("/.") ||
            tempPath.endsWith ("\\."))) {
      tempPath = tempPath.substring (0, tempPath.length () - 2);  //末尾の"/."を隠す
    }
    abuOpenTextField.setText (tempPath);  //パスの表示を更新する
    //!!! setCaretPositionが失敗することがある
    abuOpenTextField.setCaretPosition (tempPath.length ());  //パスの末尾が見えるようにする
    abuConnectCheckBox.setEnabled (false);  //挿入されたので切り離せなくなる
    Multilingual.mlnToolTipText (abuConnectCheckBox, "en", null, "ja", null);
    abuSetMode (abuCurrentMode);  //モードボタンを更新する
    abuProtectCheckBox.setEnabled (true);  //挿入されたので書き込みを禁止できるようになる
    abuProtectCheckBox.setSelected (false);  //書き込みが許可されていることを示す
    Multilingual.mlnToolTipText (abuProtectCheckBox, "en", "Write-Enabled", "ja", "書き込みが許可されています");
    abuEjectButton.setEnabled (true);  //挿入されたのでイジェクトできるようになる
    Multilingual.mlnToolTipText (abuEjectButton, "en", "Eject", "ja", "イジェクトする");
    //挿入された状態にしてから書き込みを禁止する
    if (writeProtected) {
      protect (false);  //開くときに書き込みを禁止した場合はイジェクトするまで書き込みを許可できない
    }
    //挿入された状態にしてから読み込む
    if (!load (path)) {  //読み込めなかった
      eject ();  //イジェクトする
      return false;
    }
    return true;
  }  //unit.insert(String)

  //success = unit.load (path)
  //  読み込む
  protected boolean load (String path) {
    return true;
  }

  //name = getName ()
  //  表示名を返す
  protected String getName () {
    return abuPath;
  }

}  //class AbstractUnit



