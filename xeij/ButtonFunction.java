//========================================================================================
//  ButtonFunction.java
//    en:F11/F12 and button function assignments
//    ja:F11/F12およびボタン機能割り当て
//  Copyright (C) 2003-2025 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.awt.event.*;  //ActionListener
import java.util.*;  //ArrayList
import javax.swing.*;  //JMenu

//class ButtonFunction
//  F11/F12およびボタン機能割り当て
public class ButtonFunction {



  public static enum Button {
    F11 {
      @Override String en () {
        return "F11 key";
      }
      @Override String ja () {
        return "F11 キー";
      }
      @Override String paramK () {
        return "f11key";
      }
    },
    F12 {
      @Override String en () {
        return "F12 key";
      }
      @Override String ja () {
        return "F12 キー";
      }
      @Override String paramK () {
        return "f12key";
      }
    },
    WHEEL {
      @Override String en () {
        return "Wheel button";
      }
      @Override String ja () {
        return "ホイールボタン";
      }
      @Override String paramK () {
        return "wheel";
      }
    },
    WHEELUP {
      @Override String en () {
        return "Wheel scroll up";
      }
      @Override String ja () {
        return "ホイールスクロールアップ";
      }
      @Override String paramK () {
        return "wheelup";
      }
    },
    WHEELDOWN {
      @Override String en () {
        return "Wheel scroll down";
      }
      @Override String ja () {
        return "ホイールスクロールダウン";
      }
      @Override String paramK () {
        return "wheeldown";
      }
    },
    BUTTON4 {
      @Override String en () {
        return "Button 4";
      }
      @Override String ja () {
        return "ボタン 4";
      }
      @Override String paramK () {
        return "button4";
      }
    },
    BUTTON5 {
      @Override String en () {
        return "Button 5";
      }
      @Override String ja () {
        return "ボタン 5";
      }
      @Override String paramK () {
        return "button5";
      }
    };
    abstract String en ();
    abstract String ja ();
    abstract String paramK ();
  }  //enum Button

  static final Button[] BUTTON_ARRAY = Button.values ();
  static final int BUTTONS = BUTTON_ARRAY.length;



  public static enum Modifier {
    ONLY {
      @Override int mask () {
        return 0;
      }
      @Override String paramK () {
        return "";
      }
      @Override String en () {
        return "Only";
      }
      @Override String ja () {
        return "単独";
      }
    },
    SHIFT {
      @Override int mask () {
        return InputEvent.SHIFT_DOWN_MASK;
      }
      @Override String paramK () {
        return "shift";
      }
      @Override String en () {
        return "Shift";
      }
      @Override String ja () {
        return "Shift";
      }
    },
    CTRL {
      @Override int mask () {
        return InputEvent.CTRL_DOWN_MASK;
      }
      @Override String paramK () {
        return "ctrl";
      }
      @Override String en () {
        return "Ctrl";
      }
      @Override String ja () {
        return "Ctrl";
      }
    },
    ALT {
      @Override int mask () {
        return InputEvent.ALT_DOWN_MASK;
      }
      @Override String paramK () {
        return "alt";
      }
      @Override String en () {
        return "Alt";
      }
      @Override String ja () {
        return "Alt";
      }
    };
    abstract int mask ();
    abstract String paramK ();
    abstract String en ();
    abstract String ja ();
  }  //enum Modifier

  static final Modifier[] MODIFIER_ARRAY = Modifier.values ();
  static final int MODIFIERS = MODIFIER_ARRAY.length;



  public static enum Function {
    FULLSCREEN {
      @Override String en () {
        return "Toggle full screen";
      }
      @Override String ja () {
        return "全画面表示の切り替え";
      }
      @Override String paramV () {
        return "fullscreen";
      }
      @Override boolean execute (boolean pressed) {
        if (pressed) {
          XEiJ.pnlToggleFullScreen ();
        }
        return true;
      }
    },
    MAXIMIZED {
      @Override String en () {
        return "Toggle maximized";
      }
      @Override String ja () {
        return "最大化の切り替え";
      }
      @Override String paramV () {
        return "maximized";
      }
      @Override boolean execute (boolean pressed) {
        if (pressed) {
          XEiJ.pnlToggleMaximized ();
        }
        return true;
      }
    },
    SEAMLESS {
      @Override String en () {
        return "Toggle seamless mouse";
      }
      @Override String ja () {
        return "シームレスマウスの切り替え";
      }
      @Override String paramV () {
        return "seamless";
      }
      @Override boolean execute (boolean pressed) {
        if (pressed) {
          Mouse.musSetSeamlessOn (!Mouse.musSeamlessOn);
        }
        return true;
      }
    },
    SCREENSHOT {
      @Override String en () {
        return "Take a screenshot";
      }
      @Override String ja () {
        return "スクリーンショットを撮る";
      }
      @Override String paramV () {
        return "screenshot";
      }
      @Override boolean execute (boolean pressed) {
        if (pressed) {
          Keyboard.kbdDoCapture ();
        }
        return true;
      }
    },
    TEXTCOPY {
      @Override String en () {
        return "Text screen copy";
      }
      @Override String ja () {
        return "テキスト画面コピー";
      }
      @Override String paramV () {
        return "textcopy";
      }
      @Override boolean execute (boolean pressed) {
        if (pressed) {
          TextCopy.txcCopy ();
        }
        return true;
      }
    },
    GIFANIMATION {
      @Override String en () {
        return "Start recording GIF animation";
      }
      @Override String ja () {
        return "GIF アニメーション録画開始";
      }
      @Override String paramV () {
        return "gifanimation";
      }
      @Override boolean execute (boolean pressed) {
        if (pressed) {
          GIFAnimation.gifStartRecording ();
        }
        return true;
      }
    },
    STOPANDSTART {
      @Override String en () {
        return "Stop and start";
      }
      @Override String ja () {
        return "停止と再開";
      }
      @Override String paramV () {
        return "stopandstart";
      }
      @Override boolean execute (boolean pressed) {
        if (pressed) {
          XEiJ.mpuStopAndStart ();
        }
        return true;
      }
    },
    TRACE1 {
      @Override String en () {
        return "1 trace";
      }
      @Override String ja () {
        return "トレース 1 回";
      }
      @Override String paramV () {
        return "trace1";
      }
      @Override boolean execute (boolean pressed) {
        if (pressed) {
          XEiJ.mpuAdvance (1);
        }
        return true;
      }
    },
    TRACE10 {
      @Override String en () {
        return "10 traces";
      }
      @Override String ja () {
        return "トレース 10 回";
      }
      @Override String paramV () {
        return "trace10";
      }
      @Override boolean execute (boolean pressed) {
        if (pressed) {
          XEiJ.mpuAdvance (10);
        }
        return true;
      }
    },
    TRACE100 {
      @Override String en () {
        return "100 traces";
      }
      @Override String ja () {
        return "トレース 100 回";
      }
      @Override String paramV () {
        return "trace100";
      }
      @Override boolean execute (boolean pressed) {
        if (pressed) {
          XEiJ.mpuAdvance (100);
        }
        return true;
      }
    },
    STEP1 {
      @Override String en () {
        return "1 step";
      }
      @Override String ja () {
        return "ステップ 1 回";
      }
      @Override String paramV () {
        return "step1";
      }
      @Override boolean execute (boolean pressed) {
        if (pressed) {
          XEiJ.mpuStep (1);
        }
        return true;
      }
    },
    STEP10 {
      @Override String en () {
        return "10 steps";
      }
      @Override String ja () {
        return "ステップ 10 回";
      }
      @Override String paramV () {
        return "step10";
      }
      @Override boolean execute (boolean pressed) {
        if (pressed) {
          XEiJ.mpuStep (10);
        }
        return true;
      }
    },
    STEP100 {
      @Override String en () {
        return "100 steps";
      }
      @Override String ja () {
        return "ステップ 100 回";
      }
      @Override String paramV () {
        return "step100";
      }
      @Override boolean execute (boolean pressed) {
        if (pressed) {
          XEiJ.mpuStep (100);
        }
        return true;
      }
    },
    RETURN {
      @Override String en () {
        return "Step until return";
      }
      @Override String ja () {
        return "ステップアンティルリターン";
      }
      @Override String paramV () {
        return "return";
      }
      @Override boolean execute (boolean pressed) {
        if (pressed) {
          XEiJ.mpuStepUntilReturn ();
        }
        return true;
      }
    },
    LEFTCLICK {
      @Override String en () {
        return "Left click";
      }
      @Override String ja () {
        return "左クリック";
      }
      @Override String paramV () {
        return "leftclick";
      }
      @Override boolean execute (boolean pressed) {
        if (pressed) {
          Mouse.musWheelButton = 1;  //左クリック
          Mouse.musWheelReleaseTime = XEiJ.mpuClockTime + XEiJ.TMR_FREQ / 1000 * 100;  //下へ回転。左クリック100ms
        }
        return true;
      }
    },
    RIGHTCLICK {
      @Override String en () {
        return "Right click";
      }
      @Override String ja () {
        return "右クリック";
      }
      @Override String paramV () {
        return "rightclick";
      }
      @Override boolean execute (boolean pressed) {
        if (pressed) {
          Mouse.musWheelButton = 2;  //右クリック
          Mouse.musWheelReleaseTime = XEiJ.mpuClockTime + XEiJ.TMR_FREQ / 1000 * 100;  //上へ回転。右クリック100ms
        }
        return true;
      }
    },
    DONOTHING {
      @Override String en () {
        return "No function";
      }
      @Override String ja () {
        return "機能なし";
      }
      @Override String paramV () {
        return "donothing";
      }
      @Override boolean execute (boolean pressed) {
        return false;
      }
    };
    abstract String en ();
    abstract String ja ();
    abstract String paramV ();
    abstract boolean execute (boolean pressed);
  }  //enum Function

  static final Function[] FUNCTION_ARRAY = Function.values ();
  static final int FUNCTIONS = FUNCTION_ARRAY.length;



  //割り当て
  //  assignment[MODIFIERS * bi + mi] = fi
  final static int[] assignment = new int[MODIFIERS * BUTTONS];

  //ウインドウ
  public static JFrame bfnFrame;

  //bfnInit ()
  //  初期化
  public static void bfnInit () {
    //パラメータを復元する
    for (int bi = 0; bi < BUTTONS; bi++) {
      Button b = BUTTON_ARRAY[bi];
      for (int mi = 0; mi < MODIFIERS; mi++) {
        Modifier m = MODIFIER_ARRAY[mi];
        String k = m.paramK () + b.paramK ();
        String v = Settings.sgsGetString (k);
        int fi = Function.DONOTHING.ordinal ();
        for (int gi = 0; gi < FUNCTIONS; gi++) {
          Function g = FUNCTION_ARRAY[gi];
          if (g.paramV ().equals (v)) {
            fi = gi;
            break;
          }
        }
        assignment[MODIFIERS * bi + mi] = fi;
      }
    }
    switch (Settings.sgsGetString ("mousewheel")) {  //旧パラメータ。保存しない
    case "trace":
      //-wheelup=trace1
      //-shiftwheelup=trace10
      //-ctrlwheelup=trace100
      //-wheeldown=step1
      //-shiftwheeldown=step10
      //-ctrlwheeldown=step100
      //-altwheeldown=return
      assignment[MODIFIERS * Button.WHEELUP.ordinal () + Modifier.ONLY.ordinal ()] = Function.TRACE1.ordinal ();
      assignment[MODIFIERS * Button.WHEELUP.ordinal () + Modifier.SHIFT.ordinal ()] = Function.TRACE10.ordinal ();
      assignment[MODIFIERS * Button.WHEELUP.ordinal () + Modifier.CTRL.ordinal ()] = Function.TRACE100.ordinal ();
      assignment[MODIFIERS * Button.WHEELDOWN.ordinal () + Modifier.ONLY.ordinal ()] = Function.STEP1.ordinal ();
      assignment[MODIFIERS * Button.WHEELDOWN.ordinal () + Modifier.SHIFT.ordinal ()] = Function.STEP10.ordinal ();
      assignment[MODIFIERS * Button.WHEELDOWN.ordinal () + Modifier.CTRL.ordinal ()] = Function.STEP100.ordinal ();
      assignment[MODIFIERS * Button.WHEELDOWN.ordinal () + Modifier.ALT.ordinal ()] = Function.RETURN.ordinal ();
      break;
    case "click":
      //-wheelup=leftclick
      //-wheeldown=rightclick
      assignment[MODIFIERS * Button.WHEELUP.ordinal () + Modifier.ONLY.ordinal ()] = Function.LEFTCLICK.ordinal ();
      assignment[MODIFIERS * Button.WHEELDOWN.ordinal () + Modifier.ONLY.ordinal ()] = Function.RIGHTCLICK.ordinal ();
      break;
    }
  }  //bfnInit

  //bfnTini ()
  //  後始末
  public static void bfnTini () {
    //パラメータを保存する
    for (int bi = 0; bi < BUTTONS; bi++) {
      Button b = BUTTON_ARRAY[bi];
      for (int mi = 0; mi < MODIFIERS; mi++) {
        Modifier m = MODIFIER_ARRAY[mi];
        String k = m.paramK () + b.paramK ();
        int fi = assignment[MODIFIERS * bi + mi];
        Function f = FUNCTION_ARRAY[fi];
        String v = f.paramV ();
        Settings.sgsPutString (k, v);
      }
    }
    Settings.sgsPutString ("mousewheel", "");  //旧パラメータ。保存しない
  }  //bfnTini

  //bfnMakeMenuItem ()
  public static JMenuItem bfnMakeMenuItem () {
    return Multilingual.mlnText (
      ComponentFactory.createMenuItem (
        "F11/F12 and button function assignments",
        new ActionListener () {
          @Override public void actionPerformed (ActionEvent ae) {
            bfnOpen ();
          }
        }),
      "ja", "F11/F12 およびボタン機能割り当て");
  }  //bfnMakeMenuItem

  //bfnStart ()
  public static void bfnStart () {
    if (RestorableFrame.rfmGetOpened (Settings.SGS_BFN_FRAME_KEY)) {
      bfnOpen ();
    }
  }  //bfnStart

  //bfnOpen ()
  //  ウィンドウを開く
  public static void bfnOpen () {
    if (bfnFrame == null) {
      bfnMakeFrame ();
    }
    XEiJ.pnlExitFullScreen (false);
    bfnFrame.setVisible (true);
  }  //bfnOpen()

  //bfnMakeFrame ()
  //  ウィンドウを作る
  //  ここでは開かない
  public static void bfnMakeFrame () {
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        int bmi = Integer.parseInt (ae.getActionCommand ());
        int fi = bmi / (MODIFIERS * BUTTONS);
        bmi -= MODIFIERS * BUTTONS * fi;
        assignment[bmi] = fi;
      }
    };
    //                                                                                                                     MODIFIERS*BUTTONS
    //                     0       1     2        3      4                                                                               |
    //              +-------------+--------------------------+---------+-------+-----------------+-------------------+----------+----------+
    //            0 |             |         F11 key          | F12 key | Wheel | Wheel scroll up | Wheel scroll down | Button 4 | Button 5 |
    //              |             +--+--------+-------+------+-+--+-+--+-+-+-+-+---+----+---+----+----+----+----+----+-+--+--+--+-+--+--+--+
    //            1 |             |  | Shift+ | Ctrl+ | Alt+ | |  | |  | | | | |   |    |   |    |    |    |    |    | |  |  |  | |  |  |  |
    //              +-------------+--+--------+-------+------+-+--+-+--+-+-+-+-+---+----+---+----+----+----+----+----+-+--+--+--+-+--+--+--+
    //            2 | fullscreen  |X |   X    |   X   |  X   |X|X |X|X |X|X|X|X| X | X  | X | X  | X  | X  | X  | X  |X|X |X |X |X|X |X |X |
    //              +-------------+--+--------+-------+------+-+--+-+--+-+-+-+-+---+----+---+----+----+----+----+----+-+--+--+--+-+--+--+--+
    //            :
    //              +-------------+--+--------+-------+------+-+--+-+--+-+-+-+-+---+----+---+----+----+----+----+----+-+--+--+--+-+--+--+--+
    //  1+FUNCTIONS | no function |X |   X    |   X   |  X   |X|X |X|X |X|X|X|X| X | X  | X | X  | X  | X  | X  | X  |X|X |X |X |X|X |X |X |
    //              +-------------+--+--------+-------+------+-+--+-+--+-+-+-+-+---+----+---+----+----+----+----+----+-+--+--+--+-+--+--+--+
    //  ※水平線の位置はfi={0,6,14,16}の上に固定
    //  各ボタンの左側に垂直線を加える
    int colCount = 1 + (1 + MODIFIERS) * BUTTONS;
    ArrayList<Object> cellList = new ArrayList<Object> ();
    cellList.add ("");
    for (int bi = 0; bi < BUTTONS; bi++) {
      Button b = BUTTON_ARRAY[bi];
      cellList.add (ComponentFactory.createVerticalSeparator ());
      cellList.add (Multilingual.mlnText (ComponentFactory.createLabel (b.en ()), "ja", b.ja ()));
    }
    for (int bi = 0; bi < BUTTONS; bi++) {
      for (int mi = 0; mi < MODIFIERS; mi++) {
        Modifier m = MODIFIER_ARRAY[mi];
        cellList.add (Multilingual.mlnText (ComponentFactory.createLabel (m.en ()), "ja", m.ja ()));
      }
    }
    ButtonGroup[] group = new ButtonGroup[MODIFIERS * BUTTONS];
    for (int fi = 0; fi < FUNCTIONS; fi++) {
      Function f = FUNCTION_ARRAY[fi];
      if (fi == 0 || fi == 6 || fi == 14 || fi == 16) {  //※
        cellList.add (ComponentFactory.createHorizontalSeparator ());
      }
      cellList.add (Multilingual.mlnText (ComponentFactory.createLabel (f.en ()), "ja", f.ja ()));
      for (int bi = 0; bi < BUTTONS; bi++) {
        cellList.add (ComponentFactory.createVerticalSeparator ());
        for (int mi = 0; mi < MODIFIERS; mi++) {
          int bmi = MODIFIERS * bi + mi;
          if (fi == 0) {
            group[bmi] = new ButtonGroup ();
          }
          cellList.add (
            ComponentFactory.setEnabled (
              ComponentFactory.setText (
                ComponentFactory.createRadioButton (
                  group[bmi],
                  assignment[bmi] == fi,
                  String.valueOf (MODIFIERS * BUTTONS * fi + bmi),  //commandをfbmiにする
                  listener),
                ""),  //textを""にする
              fi == Function.TEXTCOPY.ordinal () ? XEiJ.clpClipboard != null :  //テキスト画面コピーはクリップボードが必要
              true)
            );
        }
      }
    }
    String colStyles = "italic";
    String cellStyles = "rowSpan=2";
    for (int bi = 0; bi < BUTTONS; bi++) {
      colStyles += ";lengthen";
      for (int mi = 0; mi < MODIFIERS; mi++) {
        colStyles += ";";
      }
      cellStyles += ";rowSpan=2;colSpan=" + MODIFIERS;
    }
    bfnFrame = Multilingual.mlnTitle (
      ComponentFactory.createRestorableSubFrame (
        Settings.SGS_BFN_FRAME_KEY,
        "F11/F12 and button function assignments",
        null,
        ComponentFactory.setPreferredSize (
          new JScrollPane (
            ComponentFactory.setEmptyBorder (
              ComponentFactory.createGridPanel (
                colCount,  //colCount
                2 + FUNCTIONS + 4,  //rowCount
                "paddingLeft=3,paddingRight=3,center",   //gridStyles
                colStyles,  //colStyles
                "italic;italic" +
                ";colSpan=" + colCount + ",widen" +  //0の上
                ";;;;;;;colSpan=" + colCount + ",widen" +  //6の上※
                ";;;;;;;;;colSpan=" + colCount + ",widen" +  //14の上※
                ";;;colSpan=" + colCount + ",widen",  //16の上※ rowStyles
                cellStyles,  //cellStyles
                cellList.toArray (new Object[0])),
              6, 6, 6, 6)),
          600, 380)),
      "ja", "F11/F12 およびボタン機能割り当て");
  }  //bfnMakeFrame()

  //consumed = bfnExecute (b, modifiersEx, pressed, forcedF)
  //  ボタンが押されたまたは離された
  public static boolean bfnExecute (Button b, int modifiersEx, boolean pressed, Function forcedF) {
    int mask = modifiersEx & (InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK);
    for (int mi = 0; mi < MODIFIERS; mi++) {
      Modifier m = MODIFIER_ARRAY[mi];
      if (m.mask () == mask) {
        int bi = b.ordinal ();
        int bmi = MODIFIERS * bi + mi;
        int fi = assignment[bmi];
        Function f = FUNCTION_ARRAY[fi];
        if (forcedF != null) {
          f = forcedF;
        }
        return f.execute (pressed);
      }
    }
    return false;
  }  //bfnExecute

  //bfnFullScreenText ()
  //  全画面表示を解除するボタンを探して文字列で返す。なければnull
  public static String bfnFullScreenText () {
    int fi = Function.FULLSCREEN.ordinal ();  //全画面表示の切り替え
    int gi = Function.MAXIMIZED.ordinal ();  //最大化の切り替え。全画面表示のとき全画面表示を解除する
    for (int mi = 0; mi < MODIFIERS; mi++) {
      for (int bi = 0; bi < BUTTONS; bi++) {
        int a = assignment[MODIFIERS * bi + mi];
        if (a == fi || a == gi) {
          StringBuilder sb = new StringBuilder ();
          if (mi != 0) {
            sb.append (MODIFIER_ARRAY[mi].en ()).append ("+");
          }
          if (Multilingual.mlnJapanese) {
            sb.append (BUTTON_ARRAY[bi].ja ());
          } else {
            sb.append (BUTTON_ARRAY[bi].en ());
          }
          return sb.toString ();
        }
      }
    }
    return null;
  }  //bfnFullScreenText



}  //class ButtonFunction
