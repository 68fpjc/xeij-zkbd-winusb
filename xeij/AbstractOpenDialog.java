//========================================================================================
//  AbstractOpenDialog.java
//    en:Dialog to open image files
//    ja:イメージファイルを開くダイアログ
//  Copyright (C) 2003-2023 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.awt.*;  //Frame
import java.awt.event.*;  //ActionListener
import java.io.*;  //File
import java.util.*;  //ArrayList
import javax.swing.*;  //JDialog
import javax.swing.filechooser.*;  //FileFilter

public abstract class AbstractOpenDialog extends JDialog implements ActionListener {

  //ファイルチューザー
  protected JFileChooser2 fileChooser;

  //コンポーネント
  protected JCheckBox readOnlyCheckBox;
  protected JRadioButton rebootRadioButton;
  protected JRadioButton openRadioButton;

  //フラグ
  protected boolean directory;  //true=ディレクトリを開く,false=ファイルを開く
  protected boolean readOnly;  //true=書き込み禁止,false=書き込み許可
  protected boolean reboot;  //true=ここから再起動,false=開く

  //super (owner, title, jaTitle, directory, fileFilter)
  //  コンストラクタ
  @SuppressWarnings ("this-escape") public AbstractOpenDialog (
    Frame owner, String title, String jaTitle, boolean directory,
    javax.swing.filechooser.FileFilter fileFilter) {
    super (owner, title, true);  //モーダル
    this.setUndecorated (true);  //[this-escape]ウインドウの枠を消す
    this.getRootPane ().setWindowDecorationStyle (JRootPane.FRAME);  //飾り枠を描く
    this.setAlwaysOnTop (true);  //常に手前に表示
    this.setLocationByPlatform (true);
    this.setDefaultCloseOperation (WindowConstants.HIDE_ON_CLOSE);

    Multilingual.mlnTitle (this, "ja", jaTitle);
    this.directory = directory;

    //ファイルチューザー
    fileChooser = new JFileChooser2 ();
    fileChooser.setMultiSelectionEnabled (true);  //複数選択可能
    fileChooser.setControlButtonsAreShown (false);  //デフォルトのボタンを消す
    fileChooser.setFileFilter (fileFilter);
    fileChooser.addActionListener (this);

    //コンポーネント
    ButtonGroup approveGroup = new ButtonGroup ();
    JComponent component =
      ComponentFactory.createBorderPanel (
        0, 0,
        ComponentFactory.createVerticalBox (
          fileChooser,
          ComponentFactory.createHorizontalBox (
            Box.createHorizontalStrut (12),
            Box.createHorizontalGlue (),
            readOnlyCheckBox =
            Multilingual.mlnText (
              ComponentFactory.createCheckBox (
                readOnly, "Read-only", this),
              "ja", "書き込み禁止"),
            Box.createHorizontalGlue (),
            rebootRadioButton =
            ComponentFactory.setFixedSize (
              ComponentFactory.setText (
                ComponentFactory.createRadioButton (
                  approveGroup, reboot, "approve reboot", this),
                ""),
              20, 20),
            Multilingual.mlnText (
              ComponentFactory.createButton (
                "Reboot from it", KeyEvent.VK_R, this),
              "ja", "ここから再起動"),
            Box.createHorizontalStrut (12),
            openRadioButton =
            ComponentFactory.setFixedSize (
              ComponentFactory.setText (
                ComponentFactory.createRadioButton (
                  approveGroup, !reboot, "approve open", this),
                ""),
              20, 20),
            Multilingual.mlnText (
              ComponentFactory.createButton (
                "Open", KeyEvent.VK_O, this),
              "ja", "開く"),
            Box.createHorizontalStrut (20),
            Multilingual.mlnText (
              ComponentFactory.createButton (
                "Cancel", KeyEvent.VK_C, this),
              "ja", "キャンセル"),
            Box.createHorizontalStrut (12)
            ),
          Box.createVerticalStrut (12)
          )  //createVerticalBox
        );  //createBorderPanel

    this.getContentPane ().add (component, BorderLayout.CENTER);
    this.pack ();
    this.setVisible (false);
  }  //super

  //readOnly = getReadOnly ()
  //  書き込み禁止フラグを取得する
  public boolean getReadOnly () {
    return readOnly;
  }  //getReadOnly

  //setReadOnly (readOnly)
  //  書き込み禁止フラグを設定する
  public void setReadOnly (boolean readOnly) {
    this.readOnly = readOnly;
    if (readOnlyCheckBox != null) {
      readOnlyCheckBox.setSelected (readOnly);
    }
  }  //setReadOnly

  //reboot = getReboot ()
  //  ここから再起動フラグを取得する
  public boolean getReboot () {
    return reboot;
  }  //getReboot

  //setReboot (reboot)
  //  ここから再起動フラグを設定する
  public void setReboot (boolean reboot) {
    this.reboot = reboot;
    if (reboot) {
      if (rebootRadioButton != null) {
        rebootRadioButton.setSelected (true);
      }
    } else {
      if (openRadioButton != null) {
        openRadioButton.setSelected (true);
      }
    }
  }  //setReboot

  //addHistory (files)
  //  ファイルをヒストリに追加する
  public void addHistory (File[] files) {
    fileChooser.addHistory (files);
    fileChooser.selectLastFiles ();
  }  //addHistory

  //pathsList = getHistory ()
  //  ヒストリを取り出す
  public ArrayList<String> getHistory () {
    return fileChooser.getHistory ();
  }  //getHistory

  //rescanCurrentDirectory ()
  //  ファイルの一覧を作り直す
  public void rescanCurrentDirectory () {
    fileChooser.rescanCurrentDirectory ();
  }  //rescanCurrentDirectory

  //actionPerformed (ae)
  //  アクションリスナー
  @Override public void actionPerformed (ActionEvent ae) {
    switch (ae.getActionCommand ()) {
    case JFileChooser.APPROVE_SELECTION:
      approve (reboot);
      break;
    case "Read-only":  //書き込み禁止
      readOnly = ((JCheckBox) ae.getSource ()).isSelected ();
      break;
    case "approve reboot":
      reboot = true;
      break;
    case "Reboot from it":  //ここから再起動
      approve (true);
      break;
    case "approve open":
      reboot = false;
      break;
    case "Open":  //開く
      approve (false);
      break;
    case JFileChooser.CANCEL_SELECTION:
    case "Cancel":  //キャンセル
      this.setVisible (false);  //ダイアログを消す
      break;
    }
  }  //actionPerformed

  //approve (reboot)
  //  ファイルがダブルクリックされた
  //  ここから再起動ボタンが押された
  //  開くボタンが押された
  protected void approve (boolean reboot) {
    File[] files = fileChooser.getSelectedFiles ();
    if (files.length == 0 && directory) {
      files = new File[] { fileChooser.getCurrentDirectory () };
    }
    if (0 < files.length) {  //ファイルが選択されている
      openFiles (files, reboot);
      this.setVisible (false);  //ダイアログを消す。ファイルが選択されていないときは消さない
    }
  }  //approve

  //openFiles (files, reboot)
  //  ファイルを開く
  public abstract void openFiles (File[] files, boolean reboot);

}  //class AbstractOpenDialog
