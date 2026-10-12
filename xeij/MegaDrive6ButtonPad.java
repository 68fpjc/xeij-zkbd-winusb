//========================================================================================
//  MegaDrive6ButtonPad.java
//    en:MEGA DRIVE 6 button pad
//    ja:メガドラ6ボタンパッド
//  Copyright (C) 2003-2025 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

//メガドラパッド
//
//  配線図
//                      JS
//     IOA0    IOA1    IOA2    IOA3    VCC1
//      ○ IOA5 ○ IOA6 ○ PC4  ○ GND  ○
//     1│  ○ 2│  ○ 3│  ○ 4│  ○ 5│
//      │ 6│  │ 7│  │ 8│  │ 9│  │
//      │  │  │  └─┼┐│  │  │  │
//      │  │  │  ┌─┼┼┘  │  │  │
//      │  │  │  │  │└─┐│  │  │
//      │  │  │  │  │  ┌┼┼─┘  │
//      │  │  │  │  │  │└┼─┐  │
//     1│  │ 2│  │ 3│  │ 4│  │ 5│
//      ○ 6│  ○ 7│  ○ 8│  ○ 9│  ○
//      U   ○  D   ○ 0/L  ○ 0/R  ○ VCC
//         A/B     SEL     GND     ST/C
//                      MD
//
//  データ
//    +-------+---------------------------------------------------------------+
//    | 出力  |                             入力                              |
//    +-------+-------+-------+-------+-------+-------+-------+-------+-------+
//    | pin8  |       | pin7  | pin6  |       | pin4  | pin3  | pin2  | pin1  |
//    |       +-------+-------+-------+-------+-------+-------+-------+-------+
//    |       |   7   |   6   |   5   |   4   |   3   |   2   |   1   |   0   |
//    +-------+-------+-------+-------+-------+-------+-------+-------+-------+
//    |   0   |   1   | START |   A   |   1   |   0   |   0   | DOWN  |  UP   |
//    +-------+-------+-------+-------+-------+-------+-------+-------+-------+
//    |   1   |   1   |   C   |   B   |   1   | RIGHT | LEFT  | DOWN  |  UP   |
//    +-------+-------+-------+-------+-------+-------+-------+-------+-------+
//    |   0   |   1   | START |   A   |   1   |   0   |   0   | DOWN  |  UP   |
//    +-------+-------+-------+-------+-------+-------+-------+-------+-------+
//    |   1   |   1   |   C   |   B   |   1   | RIGHT | LEFT  | DOWN  |  UP   |
//    +-------+-------+-------+-------+-------+-------+-------+-------+-------+
//    |   0   |   1   | START |   A   |   1   |   0   |   0   |   0   |   0   |
//    +-------+-------+-------+-------+-------+-------+-------+-------+-------+
//    |   1   |   1   |   C   |   B   |   1   | MODE  |   X   |   Y   |   Z   |
//    +-------+-------+-------+-------+-------+-------+-------+-------+-------+
//    |   0   |   1   | START |   A   |   1   |   1   |   1   |   1   |   1   |
//    +-------+-------+-------+-------+-------+-------+-------+-------+-------+
//    pin8の出力に合わせてpin1-4,6-7の入力が変化する
//
//  参考
//    電脳倶楽部 067/B/MDPAD/M6PAD_AN.DOC
//    電脳倶楽部 077/B/6B/T_EXPAD.DOC
//    https://applause.elfmimi.jp/md6bpad.html

package xeij;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

//class MegaDrive6ButtonPad
//  メガドラ6ボタンパッド
public class MegaDrive6ButtonPad extends Joystick implements ActionListener, ChangeListener, FocusListener, XInput.GamepadListener, KeyListener {

  protected static final int UP_BIT            =  0;  //↑
  protected static final int DOWN_BIT          =  1;  //↓
  protected static final int LEFT_BIT          =  2;  //←
  protected static final int RIGHT_BIT         =  3;  //→
  protected static final int A_BIT             =  4;  //A
  protected static final int B_BIT             =  5;  //B
  protected static final int C_BIT             =  6;  //C
  protected static final int X_BIT             =  7;  //X
  protected static final int Y_BIT             =  8;  //Y
  protected static final int Z_BIT             =  9;  //Z
  protected static final int MODE_BIT          = 10;  //MODE
  protected static final int START_BIT         = 11;  //START
  protected static final int BUTTONS           = 12;  //ボタンの数

  protected static final int UP_MASK           = (1 << UP_BIT);
  protected static final int DOWN_MASK         = (1 << DOWN_BIT);
  protected static final int LEFT_MASK         = (1 << LEFT_BIT);
  protected static final int RIGHT_MASK        = (1 << RIGHT_BIT);
  protected static final int A_MASK            = (1 << A_BIT);
  protected static final int B_MASK            = (1 << B_BIT);
  protected static final int C_MASK            = (1 << C_BIT);
  protected static final int X_MASK            = (1 << X_BIT);
  protected static final int Y_MASK            = (1 << Y_BIT);
  protected static final int Z_MASK            = (1 << Z_BIT);
  protected static final int MODE_MASK         = (1 << MODE_BIT);
  protected static final int START_MASK        = (1 << START_BIT);

  protected static final String[] BIT_TO_TEXT = {
    "↑",
    "↓",
    "←",
    "→",
    "A",
    "B",
    "C",
    "X",
    "Y",
    "Z",
    "MODE",
    "START",
  };

  protected static final boolean[] BIT_TO_REPEATABLE = {
    false,  //↑
    false,  //↓
    false,  //←
    false,  //→
    true,   //A
    true,   //B
    true,   //C
    true,   //X
    true,   //Y
    true,   //Z
    false,  //MODE
    false,  //START
  };

  protected static final int MAP_CODE     = BUTTONS * 0;  //XInputコードとキーコード
  protected static final int MAP_REPEAT   = BUTTONS * 1;  //連射有効
  protected static final int MAP_DELAY    = BUTTONS * 2;  //連射開始
  protected static final int MAP_INTERVAL = BUTTONS * 3;  //連射間隔
  protected static final int MAP_LENGTH   = BUTTONS * 4;
  protected int[] map;

  protected int xinputFocusedButton;  //フォーカスされているXInputテキストフィールドの番号。-1=なし
  protected long[] startTimeOf;  //連射開始時刻。ボタンが押されたとき初期化して押されている間だけ使う
  protected int lastButtons;  //前回押されていたボタン。XInputを含む
  protected int keyButtons;  //キーで押されているボタン

  protected int page;  //ページ
  protected long lastPageTime;  //最後のページ切り替えの時刻
  protected final long PAGE_SPAN = XEiJ.TMR_FREQ / 1000;  //前回のページ切り替えから1ms経っていたらページ0,1に戻す

  protected JTextField[] xinputTextFieldOf = new JTextField[BUTTONS];
  protected JTextField[] keyTextFieldOf = new JTextField[BUTTONS];
  protected JCheckBox[] repeatCheckBoxOf = new JCheckBox[BUTTONS];
  protected SpinnerNumberModel[] delayModelOf = new SpinnerNumberModel[BUTTONS];
  protected JSpinner[] delaySpinnerOf = new JSpinner[BUTTONS];
  protected SpinnerNumberModel[] intervalModelOf = new SpinnerNumberModel[BUTTONS];
  protected JSpinner[] intervalSpinnerOf = new JSpinner[BUTTONS];

  //  map[i]=xCode<<16|keyCode
  //  xCode=map[i]>>>16
  //  keyCode=map[i]&65535
  //  xCode=128|xIndex<<5|xBit
  //  xIndex=(xCode>>5)&3
  //  xBit=xCode&31
  protected final int[][] defaultMaps = new int[][] {
    {
      (128 | 0 << 5 | XInput.UP_BIT     ) << 16 | KeyEvent.VK_UP,         //↑
      (128 | 0 << 5 | XInput.DOWN_BIT   ) << 16 | KeyEvent.VK_DOWN,       //↓
      (128 | 0 << 5 | XInput.LEFT_BIT   ) << 16 | KeyEvent.VK_LEFT,       //←
      (128 | 0 << 5 | XInput.RIGHT_BIT  ) << 16 | KeyEvent.VK_RIGHT,      //→
      (128 | 0 << 5 | XInput.A_BIT      ) << 16 | KeyEvent.VK_Z,          //A
      (128 | 0 << 5 | XInput.B_BIT      ) << 16 | KeyEvent.VK_X,          //B
      (128 | 0 << 5 | XInput.LB_BIT     ) << 16 | KeyEvent.VK_C,          //C
      (128 | 0 << 5 | XInput.X_BIT      ) << 16 | KeyEvent.VK_A,          //X
      (128 | 0 << 5 | XInput.Y_BIT      ) << 16 | KeyEvent.VK_S,          //Y
      (128 | 0 << 5 | XInput.RB_BIT     ) << 16 | KeyEvent.VK_D,          //Z
      (128 | 0 << 5 | XInput.BACK_BIT   ) << 16 | KeyEvent.VK_E,          //MODE
      (128 | 0 << 5 | XInput.START_BIT  ) << 16 | KeyEvent.VK_R,          //START
      0, 0, 0, 0,   0,   0,   0,   0,   0,   0, 0, 0,  //連射有効
      0, 0, 0, 0,  50,  50,  50 , 50,  50,  50, 0, 0,  //連射開始
      0, 0, 0, 0, 100, 100, 100, 100, 100, 100, 0, 0,  //連射間隔
    },
    {
      (128 | 1 << 5 | XInput.UP_BIT     ) << 16 | KeyEvent.VK_UNDEFINED,  //↑
      (128 | 1 << 5 | XInput.DOWN_BIT   ) << 16 | KeyEvent.VK_UNDEFINED,  //↓
      (128 | 1 << 5 | XInput.LEFT_BIT   ) << 16 | KeyEvent.VK_UNDEFINED,  //←
      (128 | 1 << 5 | XInput.RIGHT_BIT  ) << 16 | KeyEvent.VK_UNDEFINED,  //→
      (128 | 1 << 5 | XInput.A_BIT      ) << 16 | KeyEvent.VK_UNDEFINED,  //A
      (128 | 1 << 5 | XInput.B_BIT      ) << 16 | KeyEvent.VK_UNDEFINED,  //B
      (128 | 1 << 5 | XInput.LB_BIT     ) << 16 | KeyEvent.VK_UNDEFINED,  //C
      (128 | 1 << 5 | XInput.X_BIT      ) << 16 | KeyEvent.VK_UNDEFINED,  //X
      (128 | 1 << 5 | XInput.Y_BIT      ) << 16 | KeyEvent.VK_UNDEFINED,  //Y
      (128 | 1 << 5 | XInput.RB_BIT     ) << 16 | KeyEvent.VK_UNDEFINED,  //Z
      (128 | 1 << 5 | XInput.BACK_BIT   ) << 16 | KeyEvent.VK_UNDEFINED,  //MODE
      (128 | 1 << 5 | XInput.START_BIT  ) << 16 | KeyEvent.VK_UNDEFINED,  //START
      0, 0, 0, 0,   0,   0,   0,   0,   0,   0, 0, 0,  //連射有効
      0, 0, 0, 0,  50,  50,  50 , 50,  50,  50, 0, 0,  //連射開始
      0, 0, 0, 0, 100, 100, 100, 100, 100, 100, 0, 0,  //連射間隔
    },
  };

  //new MegaDrive6ButtonPad (number)
  //  コンストラクタ
  //  number  枝番号。1～2
  public MegaDrive6ButtonPad (int number) {
    this.number = number;
    id = "megadrive6button" + number;
    nameEn = "MEGA DRIVE 6 button pad #" + number;
    nameJa = "メガドラ 6 ボタンパッド #" + number;
    map = new int[MAP_LENGTH];
    int[] tempMap = Settings.sgsGetIntArray (id);
    if (0 < tempMap.length && 0 <= tempMap[0]) {  //旧フォーマット
      for (int i = 0; i < MAP_LENGTH; i++) {
        map[i] = i < tempMap.length ? tempMap[i] : 0;
      }
      //間隔を倍にする
      for (int i = 0; i < BUTTONS; i++) {
        map[MAP_INTERVAL + i] = Math.min (INTERVAL_MAX, map[MAP_INTERVAL + i] << 1);
      }
    } else if (0 < tempMap.length && tempMap[0] == -2) {  //新フォーマット
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
    startTimeOf = new long[BUTTONS];
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
    keyButtons = 0;
    page = 0;
    lastPageTime = 0L;
  }

  private void updateText () {
    for (int i = 0; i < BUTTONS; i++) {
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
      int keyCode = map[MAP_CODE + i] & 65535;
      if (keyCode == 0) {
        text = Multilingual.mlnJapanese ? "なし" : "none";
      } else {
        text = KeyEvent.getKeyText (keyCode);
      }
      keyTextFieldOf[i].setText (text);
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
      for (int i = 0; i < BUTTONS; i++) {
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
      int keyCode = map[MAP_CODE + xinputFocusedButton] & 65535;
      map[MAP_CODE + xinputFocusedButton] = xCode << 16 | keyCode;
      //すべてのボタンのxIndexを更新する
      //  インデックスを変更したいときボタンを1個割り当て直せば済む
      for (int i = 0; i < BUTTONS; i++) {
        int xCode2 = map[MAP_CODE + i] >>> 16;
        if (xCode2 != 0) {
          int keyCode2 = map[MAP_CODE + i] & 65535;
          map[MAP_CODE + i] = ((xCode2 & ~(3 << 5)) | xIndex << 5) << 16 | keyCode2;
        }
      }
      updateText ();
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
      if (ke.getKeyCode () == KeyEvent.VK_ESCAPE) {  //Escキーは解除とみなす
        keyCode = KeyEvent.VK_UNDEFINED;
      } else {  //それ以外は割り当てを変更する
        keyCode = ke.getKeyCode ();
      }
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

    for (int i = 0; i < BUTTONS; i++) {
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
      keyTextFieldOf[i] =
        ComponentFactory.addListener (
          ComponentFactory.addListener (
            ComponentFactory.setHorizontalAlignment (
              ComponentFactory.setName (
                ComponentFactory.createTextField ("", 8),
                "k" + i),
              JTextField.CENTER),
            (KeyListener) this),
          (FocusListener) this);
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

    //    0    1       2        3        4      5       6
    //       Button  XInput  Keyboard  Burst  Delay  Interval
    //    ---------------------------------------------------
    //    #    *       *        *        *      *       *
    ArrayList<Object> cellList = new ArrayList<Object> ();
    //
    cellList.add (null);
    cellList.add (Multilingual.mlnText (ComponentFactory.createLabel ("Button"), "ja", "ボタン"));
    cellList.add (ComponentFactory.createLabel ("XInput"));
    cellList.add (Multilingual.mlnText (ComponentFactory.createLabel ("Keyboard"), "ja", "キーボード"));
    cellList.add (Multilingual.mlnText (ComponentFactory.createLabel ("Burst"), "ja", "連射"));
    cellList.add (Multilingual.mlnText (ComponentFactory.createLabel ("Delay (ms)"), "ja", "開始 (ms)"));
    cellList.add (Multilingual.mlnText (ComponentFactory.createLabel ("Interval (ms)"), "ja", "間隔 (ms)"));
    //
    cellList.add (ComponentFactory.createHorizontalSeparator ());
    //
    for (int i = 0; i < BUTTONS; i++) {
      cellList.add (String.valueOf (1 + i));  //#
      cellList.add (BIT_TO_TEXT[i]);  //Button
      cellList.add (xinputTextFieldOf[i]);  //XInput
      cellList.add (keyTextFieldOf[i]);  //Keyboard
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
            7,  //colCount
            2 + BUTTONS,  //rowCount
            "paddingLeft=3,paddingRight=3,center",   //gridStyles
            "",  //colStyles
            "italic;colSpan=7,widen",  //rowStyles
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
    int keyCode = ke.getKeyCode ();
    //キーで押されているボタンを更新する
    for (int i = 0; i < BUTTONS; i++) {
      if (keyCode == (map[MAP_CODE + i] & 65535)) {
        if (pressed) {
          keyButtons |= 1 << i;
        } else {
          keyButtons &= ~(1 << i);
        }
        return true;
      }
    }
    return false;
  }


  //setPin8 (pin8)
  //  ピン8を変更する
  //  pin8  ピン8
  @Override public void setPin8 (int pin8) {
    page += (page ^ pin8) & 1;
    if (page == 7) {
      page = 1;
    }
    long t = XEiJ.mpuClockTime;
    if (lastPageTime + PAGE_SPAN <= t) {
      page &= 1;
    }
    lastPageTime = t;
  }


  //d = readByte ()
  //  ポートから読み出す
  //  d  値。0～255
  @Override public int readByte () {
    //ボタンの状態を確認する
    int currentButtons = keyButtons;  //現在押されているボタン
    int lastIndex = -1;
    int lastMasks = 0;
    for (int i = 0; i < BUTTONS; i++) {
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
    for (int i = 0; i < BUTTONS; i++) {
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
    //データを作る
    return (page == 0 || page == 2 ?
            0b11110011 &
            ((outputButtons & (UP_MASK | DOWN_MASK)) == UP_MASK ? ~0b00000001 : -1) &  //↑が押されていて↓が押されていない
            ((outputButtons & (DOWN_MASK | UP_MASK)) == DOWN_MASK ? ~0b00000010 : -1) &  //↓が押されていて↑が押されていない
            ((outputButtons & A_MASK) != 0 ? ~0b00100000 : -1) &
            ((outputButtons & START_MASK) != 0 ? ~0b01000000 : -1)
            :
            page == 1 || page == 3 ?
            0b11111111 &
            ((outputButtons & (UP_MASK | DOWN_MASK)) == UP_MASK ? ~0b00000001 : -1) &  //↑が押されていて↓が押されていない
            ((outputButtons & (DOWN_MASK | UP_MASK)) == DOWN_MASK ? ~0b00000010 : -1) &  //↓が押されていて↑が押されていない
            ((outputButtons & (LEFT_MASK | RIGHT_MASK)) == LEFT_MASK ? ~0b00000100 : -1) &  //←が押されていて→が押されていない
            ((outputButtons & (RIGHT_MASK | LEFT_MASK)) == RIGHT_MASK ? ~0b00001000 : -1) &  //→が押されていて←が押されていない
            ((outputButtons & B_MASK) != 0 ? ~0b00100000 : -1) &
            ((outputButtons & C_MASK) != 0 ? ~0b01000000 : -1)
            :
            page == 4 ?
            0b11110000 &
            ((outputButtons & A_MASK) != 0 ? ~0b00100000 : -1) &
            ((outputButtons & START_MASK) != 0 ? ~0b01000000 : -1)
            :
            page == 5 ?
            0b11111111 &
            ((outputButtons & Z_MASK) != 0 ? ~0b00000001 : -1) &
            ((outputButtons & Y_MASK) != 0 ? ~0b00000010 : -1) &
            ((outputButtons & X_MASK) != 0 ? ~0b00000100 : -1) &
            ((outputButtons & MODE_MASK) != 0 ? ~0b00001000 : -1) &
            ((outputButtons & B_MASK) != 0 ? ~0b00100000 : -1) &  //互換パッドだけ？
            ((outputButtons & C_MASK) != 0 ? ~0b01000000 : -1)  //　〃
            :
            //page == 6 ?
            0b11111111 &
            ((outputButtons & A_MASK) != 0 ? ~0b00100000 : -1) &
            ((outputButtons & START_MASK) != 0 ? ~0b01000000 : -1)
            );
  }  //readByte

}  //class MegaDrive6ButtonPad
