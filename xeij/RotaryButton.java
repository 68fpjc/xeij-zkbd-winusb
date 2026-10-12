//========================================================================================
//  RotaryButton.java
//    en:Rotary button
//    ja:回転式ボタン
//  Copyright (C) 2003-2025 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.awt.event.*;
import java.util.*;
import javax.swing.*;

//class RotaryButton
//  回転式ボタン
//  押す度にテキストまたはアイコンが入れ替わるボタン
@SuppressWarnings ("this-escape")
public class RotaryButton extends JButton implements ActionListener {

  protected int index;  //インデックス。現在のテキストまたはアイコンの番号
  protected String[] texts;  //テキスト
  protected Icon[] icons;  //アイコン
  protected ArrayList<ActionListener> listeners;  //ユーザーのアクションリスナーのリスト

  //button = new RotaryButton (index, text, ...)
  //  テキストが入れ替わる回転式ボタンのコンストラクタ
  //  サイズを調整しない。必要ならば最小サイズを設定すること
  public RotaryButton (int index, String... texts) {
    super (texts[index]);
    this.texts = texts;
    this.index = index;
    listeners = new ArrayList<ActionListener> ();
    super.addActionListener (this);
  }  //new RotaryButton

  //button = new RotaryButton (index, icon, ...)
  //  アイコンが入れ替わる回転式ボタンのコンストラクタ
  public RotaryButton (int index, Icon... icons) {
    super (icons[index]);
    this.icons = icons;
    this.index = index;
    listeners = new ArrayList<ActionListener> ();
    super.addActionListener (this);
  }  //new RotaryButton

  //index = getIndex ()
  //  インデックスを取得する
  //  ユーザーのアクションリスナーから呼び出したとき更新後のインデックスが返る
  public int getIndex () {
    return index;
  }  //getIndex

  //setIndex (index)
  //  インデックスを設定する
  //  アクションイベントは発火しない
  public void setIndex (int index) {
    if (this.index != index) {
      this.index = index;
      if (texts != null) {
        setText (texts[index]);
      } else if (icons != null) {
        setIcon (icons[index]);
      }
    }
  }  //setIndex

  //actionPerformed (ae)
  //  自分のアクションリスナー
  //  インデックスを更新した後にユーザーのアクションリスナーを呼び出す
  @Override public void actionPerformed (ActionEvent ae) {
    if (texts != null) {
      index = index + 1 < texts.length ? index + 1 : 0;
      setText (texts[index]);
    } else if (icons != null) {
      index = index + 1 < icons.length ? index + 1 : 0;
      setIcon (icons[index]);
    }
    for (ActionListener listener : listeners) {
      listener.actionPerformed (ae);
    }
  }  //actionPerformed

  //addActionListener (listener)
  //  ユーザーのアクションリスナーを追加する
  @Override public void addActionListener (ActionListener listener) {
    if (listener != null) {
      listeners.add (listener);
    }
  }  //addActionListener

  //listeners = getActionListeners ()
  //  ユーザーのアクションリスナーの配列を返す
  @Override public ActionListener[] getActionListeners () {
    return listeners.toArray (new ActionListener[listeners.size ()]);
  }  //getActionListeners

  //removeActionListener (listener)
  //  ユーザーのアクションリスナーを削除する
  @Override public void removeActionListener (ActionListener listener) {
    listeners.remove (listener);
  }  //removeActionListener

}  //class RotaryButton
