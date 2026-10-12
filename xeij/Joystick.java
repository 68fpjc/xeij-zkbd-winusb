//========================================================================================
//  Joystick.java
//    en:Joystick
//    ja:ジョイスティック
//  Copyright (C) 2003-2023 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.awt.event.*;  //KeyEvent
import javax.swing.*;  //JComponent

//class Joystick
//  ジョイスティック
public abstract class Joystick {

  protected static final int DELAY_MIN = 10;  //最小連射開始時間(ms)
  protected static final int DELAY_MAX = 1000;  //最大連射開始時間(ms)
  protected static final int DELAY_STEP = 10;  //連射開始時間の増分(ms)
  protected static final int INTERVAL_MIN = 10;  //最小連射間隔(ms)
  protected static final int INTERVAL_MAX = 2000;  //最大連射間隔(ms)
  protected static final int INTERVAL_STEP = 10;  //最大連射の増分(ms)

  protected int number;  //枝番号
  protected String id;  //識別子
  protected String nameEn;  //英語名
  protected String nameJa;  //日本語名
  protected JComponent configurationPanel;  //設定パネル

  //new Joystick ()
  //  コンストラクタ
  public Joystick () {
    configurationPanel = null;
  }

  //configurationPanel = getConfigurationPanel ()
  //  設定パネルを返す。初回は作る
  public JComponent getConfigurationPanel () {
    if (configurationPanel != null) {
      return configurationPanel;
    }
    return configurationPanel = new JPanel ();
  }

  //tini ()
  //  後始末
  public void tini () {
  }

  //reset ()
  //  リセット。設定パネルが表示されるとき呼び出される
  public void reset () {
  }

  //id = getId ()
  //  識別子を返す。パラメータで使う
  public String getId () {
    return id;
  }

  //nameEn = getNameEn ()
  //  英語名を返す。ジョイスティックポート設定ウインドウに表示される
  public String getNameEn () {
    return nameEn;
  }

  //nameJa = getNameEn ()
  //  日本語名を返す。ジョイスティックポート設定ウインドウに表示される
  public String getNameJa () {
    return nameJa;
  }

  //input (ke, pressed)
  //  キー入力イベントを処理する
  //  ke  キーイベント
  //  pressed  false=離された,true=押された
  public boolean input (KeyEvent ke, boolean pressed) {
    return false;
  }

  //setPin6 (pin6)
  //  ピン6を変更する
  //  pin6  ピン6
  public void setPin6 (int pin6) {
  }

  //setPin7 (pin7)
  //  ピン7を変更する
  //  pin7  ピン7
  public void setPin7 (int pin7) {
  }

  //setPin8 (pin8)
  //  ピン8を変更する
  //  pin8  ピン8
  public void setPin8 (int pin8) {
  }

  //d = readByte ()
  //  ポートから読み出す
  //  d  値。0～255
  public int readByte () {
    return 0xff;
  }

  //writeByte (d)
  //  ポートへ書き込む
  //  d  値。0～255
  public void writeByte (int d) {
  }

}  //class Joystick
