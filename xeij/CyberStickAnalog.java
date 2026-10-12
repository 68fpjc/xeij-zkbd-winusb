//========================================================================================
//  CyberStickAnalog.java
//    en:CYBER STICK (analog mode)
//    ja:サイバースティック(アナログモード)
//  Copyright (C) 2003-2025 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

//サイバースティック(アナログモード)
//
//  データ
//    +-------+---------------------------------------------------------------+
//    | 出力  |                             入力                              |
//    +-------+-------+-------+-------+-------+-------+-------+-------+-------+
//    | pin8  |       | pin7  | pin6  |       | pin4  | pin3  | pin2  | pin1  |
//    |       +-------+-------+-------+-------+-------+-------+-------+-------+
//    |       |   7   |   6   |   5   |   4   |   3   |   2   |   1   |   0   |
//    +-------+-------+-------+-------+-------+-------+-------+-------+-------+
//    |  REQ  |       |  ACK  |  L/H  |       |  D3   |  D2   |  D1   |  D0   |
//    +-------+-------+-------+-------+-------+-------+-------+-------+-------+
//    | 1→0  |   1   |   0   |   0   |   1   | A+A'  | B+B'  |   C   |   D   |
//    +-------+-------+-------+-------+-------+-------+-------+-------+-------+
//    |       |   1   |   0   |   1   |   1   |  E1   |  E2   | START |SELECT |
//    +-------+-------+-------+-------+-------+-------+-------+-------+-------+
//    |       |   1   |   0   |   0   |   1   |      STICK UP/DOWN UPPER      |
//    +-------+-------+-------+-------+-------+-------------------------------+
//    |       |   1   |   0   |   1   |   1   |    STICK LEFT/RIGHT UPPER     |
//    +-------+-------+-------+-------+-------+-------------------------------+
//    |       |   1   |   0   |   0   |   1   |        THROTTLE UPPER         |
//    +-------+-------+-------+-------+-------+-------------------------------+
//    |       |   1   |   0   |   1   |   1   |         OPTION UPPER          |
//    +-------+-------+-------+-------+-------+-------------------------------+
//    |       |   1   |   0   |   0   |   1   |      STICK UP/DOWN LOWER      |
//    +-------+-------+-------+-------+-------+-------------------------------+
//    |       |   1   |   0   |   1   |   1   |    STICK LEFT/RIGHT LOWER     |
//    +-------+-------+-------+-------+-------+-------------------------------+
//    |       |   1   |   0   |   0   |   1   |        THROTTLE LOWER         |
//    +-------+-------+-------+-------+-------+-------------------------------+
//    |       |   1   |   0   |   1   |   1   |         OPTION LOWER          |
//    +-------+-------+-------+-------+-------+-------+-------+-------+-------+
//    |       |   1   |   0   |   0   |   1   |   A   |   B   |  A'   |  B'   |
//    +-------+-------+-------+-------+-------+-------+-------+-------+-------+
//    |       |   1   |   0   |   1   |   1   |   1   |   1   |   1   |   1   |
//    +-------+-------+-------+-------+-------+-------+-------+-------+-------+
//    REQの立ち下がりから50us毎に12個のデータを出力する
//    スティックとスロットルの上下は0=上,…,127=128=中央,…,255=下
//    左右は0=左,…,127=128=中央,…,255=右
//    ボタンは0=押されている,1=押されていない
//
//  参考
//    http://retropc.net/x68000/software/hardware/analog/ajoy/
//    https://ktjdragon.com/nb/misc/atari_cyberstick

package xeij;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

//class CyberStickAnalog
//  サイバースティック(アナログモード)
public class CyberStickAnalog extends Joystick implements ActionListener, ChangeListener, FocusListener, XInput.GamepadListener, KeyListener {

  protected static final int SUP_BIT           =  0;  //STICK ↑
  protected static final int SLEFT_BIT         =  1;  //STICK ←
  protected static final int TUP_BIT           =  2;  //THROTTLE ↑
  protected static final int OLEFT_BIT         =  3;  //OPTION ←
  protected static final int STICKS            =  4;  //スティックの数
  protected static final int A_BIT             =  4;  //A
  protected static final int APRIME_BIT        =  5;  //A'
  protected static final int B_BIT             =  6;  //B
  protected static final int BPRIME_BIT        =  7;  //B'
  protected static final int C_BIT             =  8;  //C
  protected static final int D_BIT             =  9;  //D
  protected static final int E1_BIT            = 10;  //E1
  protected static final int E2_BIT            = 11;  //E2
  protected static final int SELECT_BIT        = 12;  //SELECT
  protected static final int START_BIT         = 13;  //START
  protected static final int STICK_AND_BUTTONS = 14;  //スティックとボタンの数

  protected static final int SUP_MASK          = (1 << SUP_BIT);
  protected static final int SLEFT_MASK        = (1 << SLEFT_BIT);
  protected static final int TUP_MASK          = (1 << TUP_BIT);
  protected static final int OLEFT_MASK        = (1 << OLEFT_BIT);
  protected static final int A_MASK            = (1 << A_BIT);
  protected static final int APRIME_MASK       = (1 << APRIME_BIT);
  protected static final int B_MASK            = (1 << B_BIT);
  protected static final int BPRIME_MASK       = (1 << BPRIME_BIT);
  protected static final int C_MASK            = (1 << C_BIT);
  protected static final int D_MASK            = (1 << D_BIT);
  protected static final int E1_MASK           = (1 << E1_BIT);
  protected static final int E2_MASK           = (1 << E2_BIT);
  protected static final int SELECT_MASK       = (1 << SELECT_BIT);
  protected static final int START_MASK        = (1 << START_BIT);

  protected static final String[] BIT_TO_TEXT = {
    "STICK ↑",
    "STICK ←",
    "THROTTLE ↑",
    "OPTION ←",
    "A",
    "A'",
    "B",
    "B'",
    "C",
    "D",
    "E1",
    "E2",
    "SELECT",
    "START",
  };

  protected static final boolean[] BIT_TO_REPEATABLE = {
    false,  //STICK ↑
    false,  //STICK ←
    false,  //THROTTLE ↑
    false,  //OPTION ←
    true,   //A
    true,   //A'
    true,   //B
    true,   //B'
    true,   //C
    true,   //D
    true,   //E1
    true,   //E2
    false,  //SELECT
    false,  //START
  };

  protected static final int MAP_CODE     = STICK_AND_BUTTONS * 0;  //XInputコードとキーコード
  protected static final int MAP_REPEAT   = STICK_AND_BUTTONS * 1;  //連射有効
  protected static final int MAP_DELAY    = STICK_AND_BUTTONS * 2;  //連射開始
  protected static final int MAP_INTERVAL = STICK_AND_BUTTONS * 3;  //連射間隔
  protected static final int MAP_LENGTH   = STICK_AND_BUTTONS * 4;
  protected int[] map;

  protected int xinputFocusedButton;  //フォーカスされているXInputテキストフィールドの番号。-1=なし
  protected long[] startTimeOf;  //連射開始時刻。ボタンが押されたとき初期化して押されている間だけ使う
  protected int lastButtons;  //前回押されていたボタン。XInputを含む

  protected int req;  //REQ
  protected long transferStartTime;  //転送開始時刻。REQの立ち下がり
  protected static final int TRANSFER_STEPS = 12;  //転送データの数。必要なデータは11個だがAJOY.Xが12個読んでいる
  protected int[] transferData;  //転送データ

  protected JTextField[] xinputTextFieldOf = new JTextField[STICK_AND_BUTTONS];
  protected JCheckBox[] repeatCheckBoxOf = new JCheckBox[STICK_AND_BUTTONS];
  protected SpinnerNumberModel[] delayModelOf = new SpinnerNumberModel[STICK_AND_BUTTONS];
  protected JSpinner[] delaySpinnerOf = new JSpinner[STICK_AND_BUTTONS];
  protected SpinnerNumberModel[] intervalModelOf = new SpinnerNumberModel[STICK_AND_BUTTONS];
  protected JSpinner[] intervalSpinnerOf = new JSpinner[STICK_AND_BUTTONS];

  //  map[i]=xCode<<16|keyCode
  //  xCode=map[i]>>>16
  //  keyCode=map[i]&65535
  //  xCode=128|xIndex<<5|xBit
  //  xIndex=(xCode>>5)&3
  //  xBit=xCode&31
  protected final int[][] defaultMaps = new int[][] {
    {
      (128 | 0 << 5 | XInput.RSUP_BIT   ) << 16 | KeyEvent.VK_UNDEFINED,  //STICK ↑
      (128 | 0 << 5 | XInput.RSLEFT_BIT ) << 16 | KeyEvent.VK_UNDEFINED,  //STICK ←
      (128 | 0 << 5 | XInput.LSUP_BIT   ) << 16 | KeyEvent.VK_UNDEFINED,  //THROTTLE ↑
      (128 | 0 << 5 | XInput.LSLEFT_BIT ) << 16 | KeyEvent.VK_UNDEFINED,  //OPTION ←
      (128 | 0 << 5 | XInput.RSTICK_BIT ) << 16 | KeyEvent.VK_UNDEFINED,  //A。右スティック
      (128 | 0 << 5 | XInput.A_BIT      ) << 16 | KeyEvent.VK_UNDEFINED,  //A'。台座
      (128 | 0 << 5 | XInput.X_BIT      ) << 16 | KeyEvent.VK_UNDEFINED,  //B。右スティック
      (128 | 0 << 5 | XInput.B_BIT      ) << 16 | KeyEvent.VK_UNDEFINED,  //B'。台座
      (128 | 0 << 5 | XInput.Y_BIT      ) << 16 | KeyEvent.VK_UNDEFINED,  //C。台座
      (128 | 0 << 5 | XInput.LSTICK_BIT ) << 16 | KeyEvent.VK_UNDEFINED,  //D。左スティック
      (128 | 0 << 5 | XInput.LB_BIT     ) << 16 | KeyEvent.VK_UNDEFINED,  //E1。左スティック
      (128 | 0 << 5 | XInput.RB_BIT     ) << 16 | KeyEvent.VK_UNDEFINED,  //E2。左スティック
      (128 | 0 << 5 | XInput.BACK_BIT   ) << 16 | KeyEvent.VK_UNDEFINED,  //SELECT
      (128 | 0 << 5 | XInput.START_BIT  ) << 16 | KeyEvent.VK_UNDEFINED,  //START
      0, 0, 0, 0,   0,   0,   0,   0,   0,   0,   0,   0, 0, 0,  //連射有効
      0, 0, 0, 0,  50,  50,  50,  50,  50,  50,  50,  50, 0, 0,  //連射開始
      0, 0, 0, 0, 100, 100, 100, 100, 100, 100, 100, 100, 0, 0,  //連射間隔
    },
    {
      (128 | 1 << 5 | XInput.RSUP_BIT   ) << 16 | KeyEvent.VK_UNDEFINED,  //STICK ↑
      (128 | 1 << 5 | XInput.RSLEFT_BIT ) << 16 | KeyEvent.VK_UNDEFINED,  //STICK ←
      (128 | 1 << 5 | XInput.LSUP_BIT   ) << 16 | KeyEvent.VK_UNDEFINED,  //THROTTLE ↑
      (128 | 1 << 5 | XInput.LSLEFT_BIT ) << 16 | KeyEvent.VK_UNDEFINED,  //OPTION ←
      (128 | 1 << 5 | XInput.RSTICK_BIT ) << 16 | KeyEvent.VK_UNDEFINED,  //A。右スティック
      (128 | 1 << 5 | XInput.A_BIT      ) << 16 | KeyEvent.VK_UNDEFINED,  //A'。台座
      (128 | 1 << 5 | XInput.X_BIT      ) << 16 | KeyEvent.VK_UNDEFINED,  //B。右スティック
      (128 | 1 << 5 | XInput.B_BIT      ) << 16 | KeyEvent.VK_UNDEFINED,  //B'。台座
      (128 | 1 << 5 | XInput.Y_BIT      ) << 16 | KeyEvent.VK_UNDEFINED,  //C。台座
      (128 | 1 << 5 | XInput.LSTICK_BIT ) << 16 | KeyEvent.VK_UNDEFINED,  //D。左スティック
      (128 | 1 << 5 | XInput.LB_BIT     ) << 16 | KeyEvent.VK_UNDEFINED,  //E1。左スティック
      (128 | 1 << 5 | XInput.RB_BIT     ) << 16 | KeyEvent.VK_UNDEFINED,  //E2。左スティック
      (128 | 1 << 5 | XInput.BACK_BIT   ) << 16 | KeyEvent.VK_UNDEFINED,  //SELECT
      (128 | 1 << 5 | XInput.START_BIT  ) << 16 | KeyEvent.VK_UNDEFINED,  //START
      0, 0, 0, 0,   0,   0,   0,   0,   0,   0,   0,   0, 0, 0,  //連射有効
      0, 0, 0, 0,  50,  50,  50,  50,  50,  50,  50,  50, 0, 0,  //連射開始
      0, 0, 0, 0, 100, 100, 100, 100, 100, 100, 100, 100, 0, 0,  //連射間隔
    },
  };

  //new CyberStickAnalog (number)
  //  コンストラクタ
  //  number  枝番号。1～2
  public CyberStickAnalog (int number) {
    this.number = number;
    id = "cyberstickanalog" + number;
    nameEn = "CYBER STICK (analog) #" + number;
    nameJa = "サイバースティック (アナログ) #" + number;
    map = new int[MAP_LENGTH];
    int[] tempMap = Settings.sgsGetIntArray (id);
    if (0 < tempMap.length && tempMap[0] == -2) {  //新フォーマット
      for (int i = 0; i < MAP_LENGTH; i++) {
        map[i] = i + 1 < tempMap.length ? tempMap[i + 1] : 0;
      }
    } else {  //初期値
      System.arraycopy (defaultMaps[number - 1], 0,  //from
                        map, 0,  //to
                        MAP_LENGTH);  //length
    }
    if (PPI.PPI_XINPUT_ON) {
      xinputFocusedButton = -1;
    }
    startTimeOf = new long[STICK_AND_BUTTONS];
    transferData = new int[TRANSFER_STEPS + 1];
    Arrays.fill (transferData, 0b11111111);
    reset ();
    configurationPanel = null;
  }

  //tini ()
  //  後始末
  @Override public void tini () {
    if (true &&  //false=初期値と同じでも出力して値を確認する
        Arrays.equals (map, defaultMaps[number - 1])) {  //初期値と同じ
      Settings.sgsPutIntArray (id, new int[0]);  //"none"ではなく""にする
    } else {  //初期値と違う
      int[] tempMap = new int[1 + MAP_LENGTH];
      tempMap[0] = -2;  //新フォーマット
      for (int i = 0; i < MAP_LENGTH; i++) {
        tempMap[1 + i] = map[i];
      }
      Settings.sgsPutIntArray (id, tempMap);
    }
  }

  //reset ()
  //  リセット。設定パネルが表示されるとき呼び出される
  @Override public final void reset () {
    lastButtons = 0;
    req = 0;
    transferStartTime = 0L;
  }

  private void updateText () {
    for (int i = 0; i < STICK_AND_BUTTONS; i++) {
      String text;
      int xCode = map[MAP_CODE + i] >>> 16;
      if (xCode == 0) {
        text = Multilingual.mlnJapanese ? "なし" : "none";
      } else {
        int xIndex = (xCode >> 5) & 3;
        int xBit = xCode & 31;
        text = "#" + xIndex + " " + XInput.BIT_TO_TEXT[xBit];
      }
      xinputTextFieldOf[i].setText (text);
    }
  }


  //ボタンのアクションリスナー
  @Override public void actionPerformed (ActionEvent ae) {
    Object source = ae.getSource ();
    String command = ae.getActionCommand ();
    if (command.equals ("Reset to default values")) {  //初期値に戻す
      if (Arrays.equals (map, defaultMaps[number - 1])) {  //初期値と同じ
        JOptionPane.showMessageDialog (
          null,
          Multilingual.mlnJapanese ? nameJa + " の設定は初期値と同じです" : nameEn + " settings are equals to default values",
          Multilingual.mlnJapanese ? "確認" : "Confirmation",
          JOptionPane.PLAIN_MESSAGE);
        return;
      }
      if (JOptionPane.showConfirmDialog (
        null,
        Multilingual.mlnJapanese ? nameJa + " の設定を初期値に戻しますか？" : "Do you want to reset " + nameEn + " settings to default values?",
        Multilingual.mlnJapanese ? "確認" : "Confirmation",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.PLAIN_MESSAGE) != JOptionPane.YES_OPTION) {
        return;
      }
      System.arraycopy (defaultMaps[number - 1], 0,  //from
                        map, 0,  //to
                        MAP_LENGTH);  //length
      updateText ();
      for (int i = 0; i < STICK_AND_BUTTONS; i++) {
        if (BIT_TO_REPEATABLE[i]) {
          repeatCheckBoxOf[i].setSelected (map[MAP_REPEAT + i] != 0);
          delayModelOf[i].setValue (Math.max (DELAY_MIN, Math.min (DELAY_MAX, map[MAP_DELAY + i])));
          intervalModelOf[i].setValue (Math.max (INTERVAL_MIN, Math.min (INTERVAL_MAX, map[MAP_INTERVAL + i])));
        }
      }
    } else if (command.startsWith ("Repeat ")) {
      int i = Integer.parseInt (command.substring (7));
      map[MAP_REPEAT + i] = repeatCheckBoxOf[i].isSelected () ? 1 : 0;
    } else {
      System.out.println ("unknown action command " + command);
    }
  }


  //スピナーのチェンジリスナー
  @Override public void stateChanged (ChangeEvent ce) {
    JSpinner spinner = (JSpinner) ce.getSource ();
    String name = spinner.getName ();
    if (name.startsWith ("Delay ")) {
      int i = Integer.parseInt (name.substring (6));
      map[MAP_DELAY + i] = delayModelOf[i].getNumber ().intValue ();
    } else if (name.startsWith ("Interval ")) {
      int i = Integer.parseInt (name.substring (9));
      map[MAP_INTERVAL + i] = intervalModelOf[i].getNumber ().intValue ();
    } else {
      System.out.println ("unknown spinner name " + name);
    }
  }


  //テキストフィールドのフォーカスリスナー
  @Override public void focusGained (FocusEvent fe) {
    Component component = fe.getComponent ();
    String componentName = component.getName ();
    int type = componentName.charAt (0);  //'x'または'k'
    int i = Integer.parseInt (componentName.substring (1));
    //背景色を変えて目立たさせる
    component.setBackground (new Color (LnF.lnfRGB[6]));
    if (PPI.PPI_XINPUT_ON && type == 'x') {  //XInput
      //Gamepadリスナーを追加する
      xinputFocusedButton = i;
      if (PPI.ppiXInput != null) {
        PPI.ppiXInput.addGamepadListener (this);
      }
    } else if (type == 'k') {  //キー
    } else {
      System.out.println ("unknown component name " + componentName);
    }
  }
  @Override public void focusLost (FocusEvent fe) {
    Component component = fe.getComponent ();
    String componentName = component.getName ();
    int type = componentName.charAt (0);  //'x'または'k'
    int i = Integer.parseInt (componentName.substring (1));
    //背景色を元に戻す
    component.setBackground (null);
    if (PPI.PPI_XINPUT_ON && type == 'x') {  //XInput
      //Gamepadリスナーを削除する
      if (PPI.ppiXInput != null) {
        PPI.ppiXInput.removeGamepadListeners ();
      }
      xinputFocusedButton = -1;
    } else if (type == 'k') {  //キー
    } else {
      System.out.println ("unknown component name " + componentName);
    }
  }


  //Gamepadリスナー
  @Override public void connected (XInput.Gamepad gamepad) {
  }
  @Override public void disconnected (XInput.Gamepad gamepad) {
  }
  @Override public void buttonPressed (XInput.Gamepad gamepad, int buttonMasks) {
    if (buttonMasks == 0) {  //ないはず
      return;
    }
    if (PPI.PPI_XINPUT_ON && 0 <= xinputFocusedButton) {  //フォーカスされているxinputのテキストフィールドがある
      int xIndex = gamepad.getIndex ();
      int xBit = Integer.numberOfTrailingZeros (buttonMasks);
      int xCode = 128 | xIndex << 5 | xBit;
      if (STICKS <= xinputFocusedButton ||  //ボタンはすべて割り当てられる
          ((1 << xBit) & (XInput.LSUP_MASK |
                          XInput.LSDOWN_MASK |
                          XInput.LSLEFT_MASK |
                          XInput.LSRIGHT_MASK |
                          XInput.RSUP_MASK |
                          XInput.RSDOWN_MASK |
                          XInput.RSLEFT_MASK |
                          XInput.RSRIGHT_MASK)) != 0) {  //スティックはスティックのみ割り当てられる
        map[MAP_CODE + xinputFocusedButton] = xCode << 16;
        //すべてのボタンのxIndexを更新する
        //  インデックスを変更したいときボタンを1個割り当て直せば済む
        for (int i = 0; i < STICK_AND_BUTTONS; i++) {
          int xCode2 = map[MAP_CODE + i] >>> 16;
          if (xCode2 != 0) {
            map[MAP_CODE + i] = ((xCode2 & ~(3 << 5)) | xIndex << 5) << 16;
          }
        }
        updateText ();
      }
    }
  }
  @Override public void buttonReleased (XInput.Gamepad gamepad, int buttonMasks) {
  }
  @Override public void leftStickMovedX (XInput.Gamepad gamepad) {
  }
  @Override public void leftStickMovedY (XInput.Gamepad gamepad) {
  }
  @Override public void leftTriggerMoved (XInput.Gamepad gamepad) {
  }
  @Override public void rightStickMovedX (XInput.Gamepad gamepad) {
  }
  @Override public void rightStickMovedY (XInput.Gamepad gamepad) {
  }
  @Override public void rightTriggerMoved (XInput.Gamepad gamepad) {
  }


  //テキストフィールドのキーリスナー
  @Override public void keyPressed (KeyEvent ke) {
    Component component = ke.getComponent ();
    String componentName = component.getName ();
    int type = componentName.charAt (0);  //'x'または'k'
    int i = Integer.parseInt (componentName.substring (1));
    int xCode = map[MAP_CODE + i] >>> 16;
    int keyCode = map[MAP_CODE + i] & 65535;
    if (PPI.PPI_XINPUT_ON && type == 'x') {  //XInput
      if (ke.getKeyCode () == KeyEvent.VK_ESCAPE) {  //Escキーは解除とみなす
        xCode = 0;
      }
    } else if (type == 'k') {  //キー
    } else {
      System.out.println ("unknown component name " + componentName);
    }
    map[MAP_CODE + i] = xCode << 16 | keyCode;
    updateText ();
    ke.consume ();
  }
  @Override public void keyReleased (KeyEvent ke) {
    ke.consume ();
  }
  @Override public void keyTyped (KeyEvent ke) {
    ke.consume ();
  }


  //configurationPanel = getConfigurationPanel ()
  //  設定パネルを返す。初回は作る
  @Override public JComponent getConfigurationPanel () {

    if (configurationPanel != null) {
      return configurationPanel;
    }

    for (int i = 0; i < STICK_AND_BUTTONS; i++) {
      xinputTextFieldOf[i] =
        ComponentFactory.setEnabled (
          ComponentFactory.addListener (
            ComponentFactory.addListener (
              ComponentFactory.setHorizontalAlignment (
                ComponentFactory.setName (
                  ComponentFactory.createTextField ("", 8),
                  "x" + i),
                JTextField.CENTER),
              (KeyListener) this),
            (FocusListener) this),
          PPI.PPI_XINPUT_ON && XEiJ.prgIsWindows);
      if (BIT_TO_REPEATABLE[i]) {
        repeatCheckBoxOf[i] =
          ComponentFactory.setText (
            ComponentFactory.createCheckBox (map[MAP_REPEAT + i] != 0, "Repeat " + i, (ActionListener) this),
            "");
        delayModelOf[i] =
          new SpinnerNumberModel (Math.max (DELAY_MIN, Math.min (DELAY_MAX, map[MAP_DELAY + i])),
                                  DELAY_MIN, DELAY_MAX, DELAY_STEP);
        delaySpinnerOf[i] =
          ComponentFactory.setName (
            ComponentFactory.createNumberSpinner (delayModelOf[i], 4, (ChangeListener) this),
            "Delay " + i);
        intervalModelOf[i] =
          new SpinnerNumberModel (Math.max (INTERVAL_MIN, Math.min (INTERVAL_MAX, map[MAP_INTERVAL + i])),
                                  INTERVAL_MIN, INTERVAL_MAX, INTERVAL_STEP);
        intervalSpinnerOf[i] =
          ComponentFactory.setName (
            ComponentFactory.createNumberSpinner (intervalModelOf[i], 4, (ChangeListener) this),
            "Interval " + i);
      }
    }
    updateText ();

    //    0       1           2      3      4       5
    //       Stick/Button  XInput  Burst  Delay  Interval
    //    -----------------------------------------------
    //    #       *           *      *      *       *
    ArrayList<Object> cellList = new ArrayList<Object> ();
    //
    cellList.add (null);
    cellList.add (Multilingual.mlnText (ComponentFactory.createLabel ("Stick/Button"), "ja", "スティック/ボタン"));
    cellList.add (ComponentFactory.createLabel ("XInput"));
    cellList.add (Multilingual.mlnText (ComponentFactory.createLabel ("Burst"), "ja", "連射"));
    cellList.add (Multilingual.mlnText (ComponentFactory.createLabel ("Delay (ms)"), "ja", "開始 (ms)"));
    cellList.add (Multilingual.mlnText (ComponentFactory.createLabel ("Interval (ms)"), "ja", "間隔 (ms)"));
    //
    cellList.add (ComponentFactory.createHorizontalSeparator ());
    //
    for (int i = 0; i < STICK_AND_BUTTONS; i++) {
      cellList.add (String.valueOf (1 + i));  //#
      cellList.add (BIT_TO_TEXT[i]);  //Button
      cellList.add (xinputTextFieldOf[i]);  //XInput
      cellList.add (BIT_TO_REPEATABLE[i] ? repeatCheckBoxOf[i] : null);  //Burst
      cellList.add (BIT_TO_REPEATABLE[i] ? delaySpinnerOf[i] : null);  //Delay
      cellList.add (BIT_TO_REPEATABLE[i] ? intervalSpinnerOf[i] : null);  //Interval
    }

    configurationPanel =
      ComponentFactory.createVerticalBox (
        Box.createVerticalStrut (5),
        ComponentFactory.createHorizontalBox (
          Box.createHorizontalGlue (),
          Multilingual.mlnText (ComponentFactory.createLabel (getNameEn ()), "ja", getNameJa ()),
          Box.createHorizontalGlue ()),
        Box.createVerticalStrut (5),
        ComponentFactory.createHorizontalBox (
          ComponentFactory.createGridPanel (
            6,  //colCount
            2 + STICK_AND_BUTTONS,  //rowCount
            "paddingLeft=3,paddingRight=3,center",   //gridStyles
            "",  //colStyles
            "italic;colSpan=6,widen",  //rowStyles
            "",  //cellStyles
            cellList.toArray (new Object[0]))),
        Box.createVerticalStrut (5),
        ComponentFactory.createHorizontalBox (
          Box.createHorizontalGlue (),
          Multilingual.mlnText (ComponentFactory.createButton ("Reset to default values", (ActionListener) this), "ja", "初期値に戻す"),
          Box.createHorizontalGlue ()),
        Box.createVerticalStrut (5));

    return configurationPanel;
  }


  //input (ke, pressed)
  //  キー入力イベントを処理する
  //  ke  キーイベント
  //  pressed  false=離された,true=押された
  @Override public boolean input (KeyEvent ke, boolean pressed) {
    return false;
  }


  //setPin8 (pin8)
  //  ピン8を変更する
  //  pin8  ピン8
  @Override public void setPin8 (int pin8) {
    if (!(PPI.PPI_XINPUT_ON && XEiJ.prgIsWindows)) {
      req = pin8;
      return;
    }
    if ((req & ~pin8) == 0) {  //REQの立ち下がりではない
      req = pin8;
      return;
    }
    //REQの立ち下がり
    req = 0;
    //転送開始時刻を記録する
    transferStartTime = XEiJ.mpuClockTime;
    //スティックの位置を確認する
    int outputSticks = 0;  //出力するスティックの位置
    if (PPI.ppiXInput != null) {
      for (int i = 0; i < STICKS; i++) {
        int xCode = map[MAP_CODE + i] >>> 16;
        if (xCode != 0) {
          int xIndex = (xCode >> 5) & 3;
          int xBit = xCode & 31;
          //スティックの位置を-32768～32768で取得する
          int t = (xBit == XInput.LSUP_BIT ? -PPI.ppiXInput.getLeftStickY (xIndex) :  //左スティック。上が0、下が255
                   xBit == XInput.LSDOWN_BIT ? PPI.ppiXInput.getLeftStickY (xIndex) :  //左スティック。下が0、上が255
                   xBit == XInput.LSLEFT_BIT ? PPI.ppiXInput.getLeftStickX (xIndex) :  //左スティック。左が0、右が255
                   xBit == XInput.LSRIGHT_BIT ? -PPI.ppiXInput.getLeftStickX (xIndex) :  //左スティック。右が0、左が255
                   xBit == XInput.RSUP_BIT ? -PPI.ppiXInput.getRightStickY (xIndex) :  //右スティック。上が0、下が255
                   xBit == XInput.RSDOWN_BIT ? PPI.ppiXInput.getRightStickY (xIndex) :  //右スティック。下が0、上が255
                   xBit == XInput.RSLEFT_BIT ? PPI.ppiXInput.getRightStickX (xIndex) :  //右スティック。左が0、右が255
                   xBit == XInput.RSRIGHT_BIT ? -PPI.ppiXInput.getRightStickX (xIndex) :  //右スティック。右が0、左が255
                   0);
          //符号を付けたまま右に8ビットシフトする。-256～-1は-1、0～255は0になる
          t >>= 8;
          //128に加えてクリッピングする。127と128(の境目)が中央になる
          t = Math.max (0, Math.min (255, 128 + t));
          outputSticks |= t << (8 * i);
        }
      }
    }
    //ボタンの状態を確認する
    int currentButtons = 0;  //現在押されているボタン
    int lastIndex = -1;
    int lastMasks = 0;
    for (int i = STICKS; i < STICK_AND_BUTTONS; i++) {
      int xCode = map[MAP_CODE + i] >>> 16;
      if (xCode != 0) {  //割り当てられている
        int xIndex = (xCode >> 5) & 3;
        int xBit = xCode & 31;
        if (lastIndex != xIndex) {
          lastIndex = xIndex;
          lastMasks = PPI.ppiXInput == null ? 0 : PPI.ppiXInput.getButtonMasks (lastIndex);
        }
        if ((lastMasks & (1 << xBit)) != 0) {  //押されている
          currentButtons |= 1 << i;
        }
      }
    }
    int pressedButtons = ~lastButtons & currentButtons;  //今回押されたボタン
    lastButtons = currentButtons;
    int outputButtons = 0;  //出力するボタン
    for (int i = STICKS; i < STICK_AND_BUTTONS; i++) {
      if ((pressedButtons & (1 << i)) != 0 &&  //今回押された
          map[MAP_REPEAT + i] != 0) {  //連射有効
        startTimeOf[i] = XEiJ.mpuClockTime + map[MAP_DELAY + i] * (XEiJ.TMR_FREQ / 1000);  //連射開始時刻
      }
      if ((currentButtons & (1 << i)) != 0 &&  //現在押されている
          (map[MAP_REPEAT + i] == 0 ||  //連射が無効または
           XEiJ.mpuClockTime < startTimeOf[i] ||  //連射開始時刻になっていないまたは
           ((int) ((XEiJ.mpuClockTime - startTimeOf[i]) /  //連射開始時刻を過ぎた時間を
                   ((map[MAP_INTERVAL + i] >> 1) * (XEiJ.TMR_FREQ / 1000)))  //連射間隔の半分で割った商が
            & 1) != 0)) {  //奇数
        outputButtons |= 1 << i;
      }
    }
    //転送データを作る
    transferData[0] = (0b10010000 |
                       ((outputButtons & (A_MASK | APRIME_MASK)) != 0 ? 0 : 0b00001000) |
                       ((outputButtons & (B_MASK | BPRIME_MASK)) != 0 ? 0 : 0b00000100) |
                       ((outputButtons &  C_MASK               ) != 0 ? 0 : 0b00000010) |
                       ((outputButtons &  D_MASK               ) != 0 ? 0 : 0b00000001));
    transferData[1] = (0b10110000 |
                       ((outputButtons & E1_MASK    ) != 0 ? 0 : 0b00001000) |
                       ((outputButtons & E2_MASK    ) != 0 ? 0 : 0b00000100) |
                       ((outputButtons & START_MASK ) != 0 ? 0 : 0b00000010) |
                       ((outputButtons & SELECT_MASK) != 0 ? 0 : 0b00000001));
    transferData[2] = 0b10010000 | ((outputSticks >>> (8 * SUP_BIT   + 4)) & 0b00001111);
    transferData[3] = 0b10110000 | ((outputSticks >>> (8 * SLEFT_BIT + 4)) & 0b00001111);
    transferData[4] = 0b10010000 | ((outputSticks >>> (8 * TUP_BIT   + 4)) & 0b00001111);
    transferData[5] = 0b10110000 | ((outputSticks >>> (8 * OLEFT_BIT + 4)) & 0b00001111);
    transferData[6] = 0b10010000 | ((outputSticks >>> (8 * SUP_BIT      )) & 0b00001111);
    transferData[7] = 0b10110000 | ((outputSticks >>> (8 * SLEFT_BIT    )) & 0b00001111);
    transferData[8] = 0b10010000 | ((outputSticks >>> (8 * TUP_BIT      )) & 0b00001111);
    transferData[9] = 0b10110000 | ((outputSticks >>> (8 * OLEFT_BIT    )) & 0b00001111);
    transferData[10] = (0b10010000 |
                        ((outputButtons & A_MASK     ) != 0 ? 0 : 0b00001000) |
                        ((outputButtons & B_MASK     ) != 0 ? 0 : 0b00000100) |
                        ((outputButtons & APRIME_MASK) != 0 ? 0 : 0b00000010) |
                        ((outputButtons & BPRIME_MASK) != 0 ? 0 : 0b00000001));
    transferData[11] = 0b10111111;
  }  //setPin8

  //d = readByte ()
  //  ポートから読み出す
  //  d  値。0～255
  @Override public int readByte () {
    long t = XEiJ.mpuClockTime - transferStartTime;  //転送開始時刻からの経過時間を
    t /= XEiJ.TMR_FREQ / 20000L;  //50us=1/20000Hzで割る
    int step = (int) Math.max (0L, Math.min ((long) TRANSFER_STEPS, t));
    return transferData[step];
  }  //readByte

}  //class CyberStickAnalog
