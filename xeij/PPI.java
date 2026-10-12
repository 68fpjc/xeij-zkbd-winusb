//========================================================================================
//  PPI.java
//    en:8255 PPI -- It emulates joystick ports.
//    ja:8255 PPI -- ジョイスティックポートをエミュレートします。
//  Copyright (C) 2003-2025 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

public class PPI {

  //ポート
  //  0x00e9a000-0x00e9bfffは下位3ビットだけデコードされる
  //  偶数アドレスをバイトサイズでリード/ライトするとバスエラーになる
  //  偶数アドレスをワードサイズでリードすると上位バイトは0xffになる
  //
  //  0x00e9a001  PPIポートA  ジョイスティック1
  //         7        6        5        4        3        2        1        0
  //    +--------+--------+--------+--------+--------+--------+--------+--------+
  //    |   PA7  |   PA6  |   PA5  |   PA4  |   PA3  |   PA2  |   PA1  |   PA0  |
  //    |    1   |   JS7  |   JS6  |    1   |   JS4  |   JS3  |   JS2  |   JS1  |
  //    |        |    B   |    A   |        |   →   |   ←   |   ↓   |   ↑   |
  //    +--------+--------+--------+--------+--------+--------+--------+--------+
  //    bit7    PA7  1
  //    bit6    PA6  JS7入力。0=Bが押されている
  //    bit5    PA5  JS6入力。0=Aが押されている
  //    bit4    PA4  1
  //    bit3    PA3  JS4入力。0=→が押されている
  //    bit2    PA2  JS3入力。0=←が押されている
  //    bit1    PA1  JS2入力。0=↓が押されている
  //    bit0    PA0  JS1入力。0=↑が押されている
  //
  //  0x00e9a003  PPIポートB  ジョイスティック2
  //         7        6        5        4        3        2        1        0
  //    +--------+--------+--------+--------+--------+--------+--------+--------+
  //    |   PB7  |   PB6  |   PB5  |   PB4  |   PB3  |   PB2  |   PB1  |   PB0  |
  //    |    1   |   JT7  |   JT6  |    1   |   JT4  |   JT3  |   JT2  |   JT1  |
  //    |        |    B   |    A   |        |   →   |   ←   |   ↓   |   ↑   |
  //    +--------+--------+--------+--------+--------+--------+--------+--------+
  //    bit7    PB7  1
  //    bit6    PB6  JT7入力。0=Bが押されている
  //    bit5    PB5  JT6入力。0=Aが押されている
  //    bit4    PB4  1
  //    bit3    PB3  JT4入力。0=→が押されている
  //    bit2    PB2  JT3入力。0=←が押されている
  //    bit1    PB1  JT2入力。0=↓が押されている
  //    bit0    PB0  JT1入力。0=↑が押されている
  //
  //  0x00e9a005  PPIポートC  ADPCMコントロール。初期値は0x0b
  //         7        6        5        4        3        2        1        0
  //    +--------+--------+--------+--------+--------+--------+--------+--------+
  //    |   PC7  |   PC6  |   PC5  |   PC4  |   PC3  |   PC2  |   PC1  |   PC0  |
  //    |   JS7  |   JS6  |   JT8  |   JS8  |      RATIO      |  LEFT  |  RIGHT |
  //    +--------+--------+--------+--------+--------+--------+--------+--------+
  //    bit7    PC7    JS7出力(負論理)
  //    bit6    PC6    JS6出力(負論理)
  //    bit5    PC5    JT8出力
  //    bit4    PC4    JS8出力
  //    bit3-2  PC3-2  ADPCM分周比。00=1/1024,01=1/768,10=1/512,11=inhibited
  //    bit1    PC1    ADPCM出力LEFT。0=出力する,1=出力しない
  //    bit0    PC0    ADPCM出力RIGHT。0=出力する,1=出力しない
  //
  //  0x00e9a007  PPIコントロール
  //    bit7=0  ポートCで出力に設定されているビットの操作
  //      bit3-1  ビット番号
  //      bit0    設定値
  //    bit7=1  モードの設定。0x92に固定
  //      bit6-5  グループA(ポートAとポートCの上位)のモード(0=モード0,1=モード1,2/3=モード2)。モード0に固定
  //      bit4    ポートAの方向(0=出力,1=入力)。入力に固定
  //      bit3    ポートCの上位の方向(0=出力,1=入力)。出力に固定
  //      bit2    グループB(ポートBとポートCの下位)のモード(0=モード0,1=モード1)。モード0に固定
  //      bit1    ポートBの方向(0=出力,1=入力)。入力に固定
  //      bit0    ポートCの下位の方向(0=出力,1=入力)。出力に固定
  //
  //  ボタンのマスク
  //    上と下または左と右のキーが同時に押された場合は両方キャンセルする
  //    SELECTボタンは上下同時押し、RUNボタンは左右同時押しに割り当てられる
  //
  //  bit4とbit7は入力できない
  //    8255の足がプルアップされているので実機ではどうすることもできない(外付けの回路でどうにかなるものではない)
  //    エミュレータで入力できるようにするのは簡単だか対応しているソフトは存在しないだろう
  //
  //  参考
  //    電脳倶楽部67号  B/MDPAD/M6PAD_AN.DOC
  //    電脳倶楽部77号  B/6B/T_EXPAD.DOC
  //
  public static final int PPI_PORT_A  = 0x00e9a001;   //PPIポートA
  public static final int PPI_PORT_B  = 0x00e9a003;   //PPIポートB
  public static final int PPI_PORT_C  = 0x00e9a005;   //PPIポートC
  public static final int PPI_CONTROL = 0x00e9a007;   //PPIコントロール

  //ジョイスティック
  public static Joystick[] ppiJoysticks;
  public static Joystick ppiJoystick1;
  public static Joystick ppiJoystick2;

  //モード
  public static boolean ppiJoyKey;  //true=キーボードの一部をジョイスティックとみなす
  public static boolean ppiJoyAuto;  //true=ポートが繰り返し読み出されている間だけ有効
  public static boolean ppiJoyBlock;  //true=ジョイスティック操作として処理されたキー入力データを取り除く


  //ポートの値
  public static int ppiPortCData;

  //自動切り替え
  //  PPIのポートが参照されてから一定時間だけJOYKEYを有効にする
  public static final long PPI_CONTINUOUS_ACCESS_SPAN = XEiJ.TMR_FREQ / 10;  //自動切り替えの有効期間(TMR_FREQ単位)。1/10秒
  public static long ppiLastAccessTime;  //最後にPPIのポートがアクセスされた時刻

  //XInput
  public static final boolean PPI_XINPUT_ON = true;  //false=使わない,true=使う
  public static boolean ppiXInputOn;  //false=使わない,true=使う
  public static XInput ppiXInput;  //XInput。nullでなければ動作中
  public static int ppiXInputLastButtons;

  //ウインドウ
  public static JFrame ppiFrame;

  public static JScrollPane ppiConfigurationScrollPane;

  //ppiInit ()
  //  PPIを初期化する
  public static void ppiInit () {
    ppiJoyKey = Settings.sgsGetOnOff ("joykey");
    ppiJoyAuto = Settings.sgsGetOnOff ("joyauto");
    ppiJoyBlock = Settings.sgsGetOnOff ("joyblock");

    ppiJoysticks = new Joystick[] {
      new DummyPad (),
      new Normal2ButtonPad (1),
      new Normal2ButtonPad (2),
      new MegaDrive3ButtonPad (1),
      new MegaDrive3ButtonPad (2),
      new MegaDrive6ButtonPad (1),
      new MegaDrive6ButtonPad (2),
      new CyberStickAnalog (1),
      new CyberStickAnalog (2),
      new CyberStickDigital (1),
      new CyberStickDigital (2),
      new Shiromadokun (1),
      new Shiromadokun (2),
    };

    String id1 = Settings.sgsGetString ("joystick1");  //ジョイスティックポート1に接続するデバイス
    ppiJoystick1 = ppiJoysticks[0];  //DummyPad
    for (Joystick joystick : ppiJoysticks) {
      if (joystick.getId ().equalsIgnoreCase (id1)) {
        ppiJoystick1 = joystick;
      }
    }

    String id2 = Settings.sgsGetString ("joystick2");  //ジョイスティックポート2に接続するデバイス
    ppiJoystick2 = ppiJoysticks[0];  //DummyPad
    for (Joystick joystick : ppiJoysticks) {
      if (joystick.getId ().equalsIgnoreCase (id2)) {
        ppiJoystick2 = joystick;
      }
    }

    if (PPI_XINPUT_ON) {
      ppiXInputOn = XEiJ.prgIsWindows && Settings.sgsGetOnOff ("xinput");
      if (ppiXInputOn) {
        ppiXInputStart ();
      }
      ppiXInputLastButtons = 0;
    }

    ppiReset ();
  }  //ppiInit

  //ppiTini ()
  //  後始末
  public static void ppiTini () {
    Settings.sgsPutOnOff ("joykey", ppiJoyKey);
    Settings.sgsPutOnOff ("joyauto", ppiJoyAuto);
    Settings.sgsPutOnOff ("joyblock", ppiJoyBlock);
    for (Joystick joystick : ppiJoysticks) {
      joystick.tini ();
    }
    Settings.sgsPutString ("joystick1", ppiJoystick1.getId ());
    Settings.sgsPutString ("joystick2", ppiJoystick2.getId ());
    //XInput
    if (PPI_XINPUT_ON) {
      Settings.sgsPutOnOff ("xinput", ppiXInputOn);
      if (ppiXInputOn) {
        ppiXInputOn = false;
        ppiXInputEnd ();
      }
    }
  }  //ppiTini

  //ppiReset ()
  //  リセット
  public static void ppiReset () {
    ppiPortCData = 0;
    ppiLastAccessTime = 0L;
  }


  public static void ppiXInputStart () {
    if (ppiXInput == null) {
      System.out.println (Multilingual.mlnJapanese ?
                          "XInput のポーリングを開始します" :
                          "Starts polling XInput");
      ppiXInput = new XInput ();
    }
  }

  public static void ppiXInputEnd () {
    if (ppiXInput != null) {
      System.out.println (Multilingual.mlnJapanese ?
                          "XInput のポーリングを終了します" :
                          "Ends polling XInput");
      ppiXInput.end ();
      ppiXInput = null;
    }
  }


  //ppiStart ()
  public static void ppiStart () {
    if (RestorableFrame.rfmGetOpened (Settings.SGS_PPI_FRAME_KEY)) {
      ppiOpen ();
    }
  }

  //ppiOpen ()
  //  ジョイスティックの設定ウインドウを開く
  public static void ppiOpen () {
    if (ppiFrame == null) {
      ppiMakeFrame ();
    }
    XEiJ.pnlExitFullScreen (false);
    ppiFrame.setVisible (true);
  }

  //ppiMakeFrame ()
  //  ジョイスティックポートの設定ウインドウを作る
  //  ここでは開かない
  public static void ppiMakeFrame () {

    //アクションリスナー
    ActionListener actionListener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        String command = ae.getActionCommand ();
        switch (command) {
        case "Consider part of keyboard as joystick":
          //キーボードの一部をジョイスティックとみなす
          ppiJoyKey = ((JCheckBox) ae.getSource ()).isSelected ();
          break;
        case "Enabled only while the port is read repeatedly":
          //ポートが繰り返し読み出されている間だけ有効
          ppiJoyAuto = ((JCheckBox) ae.getSource ()).isSelected ();
          break;
        case "Remove key input data processed as a joystick operation":
          //ジョイスティック操作として処理されたキー入力データを取り除く
          ppiJoyBlock = ((JCheckBox) ae.getSource ()).isSelected ();
          break;
          //
        case "XInput":
          if (PPI_XINPUT_ON) {
            if (((JCheckBox) ae.getSource ()).isSelected ()) {  //on
              if (!ppiXInputOn) {  //off→on
                ppiXInputOn = true;
                ppiXInputStart ();
              }
            } else {  //off
              if (ppiXInputOn) {  //on→off
                ppiXInputOn = false;
                ppiXInputEnd ();
              }
            }
          }
        }
      }
    };

    //ジョイスティックポートのメニュー
    //       0   1  2
    //   0  ポート  接続
    //   1   1   2
    //   2  -------------------------
    //   3  RB  RB  なし
    //   4  RB  RB  ノーマル2ボタンパッド#1
    //   5  RB  RB  ノーマル2ボタンパッド#2
    //   6  RB  RB  メガドラ3ボタンパッド#1
    //   7  RB  RB  メガドラ3ボタンパッド#2
    //   8  RB  RB  メガドラ6ボタンパッド#1
    //   9  RB  RB  メガドラ6ボタンパッド#2
    //  10  RB  RB  サイバースティック(デジタルモード)#1
    //  12  RB  RB  サイバースティック(デジタルモード)#2
    //  13  RB  RB  サイバースティック(アナログモード)#1
    //  14  RB  RB  サイバースティック(アナログモード)#2
    //  15  RB  RB  白窓君#1
    //  16  RB  RB  白窓君#2
    ButtonGroup port1Group = new ButtonGroup ();
    ButtonGroup port2Group = new ButtonGroup ();
    ActionListener port1Listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Joystick joyStick = ppiJoysticks[Integer.parseInt (ae.getActionCommand ())];
        if (ppiJoystick1 != joyStick) {
          ppiJoystick1.reset ();
          ppiJoystick1 = joyStick;
        }
        ppiConfigurationScrollPane.setViewportView (joyStick.getConfigurationPanel ());
      }
    };
    ActionListener port2Listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Joystick joyStick = ppiJoysticks[Integer.parseInt (ae.getActionCommand ())];
        if (ppiJoystick2 != joyStick) {
          ppiJoystick2.reset ();
          ppiJoystick2 = joyStick;
        }
        ppiConfigurationScrollPane.setViewportView (joyStick.getConfigurationPanel ());
      }
    };
    ArrayList<Object> cellList = new ArrayList<Object> ();
    cellList.add (Multilingual.mlnText (ComponentFactory.createLabel ("Port"), "ja", "ポート"));  //(0,0)-(1,0)
    cellList.add (Multilingual.mlnText (ComponentFactory.createLabel ("Connect to"), "ja", "接続"));  //(2,0)-(2,1)
    cellList.add ("1");  //(0,1)
    cellList.add ("2");  //(1,1)
    cellList.add (ComponentFactory.createHorizontalSeparator ());  //(0,2)-(2,2)
    for (int i = 0; i < ppiJoysticks.length; i++) {
      Joystick joyStick = ppiJoysticks[i];
      cellList.add (ComponentFactory.setText (ComponentFactory.createRadioButton (
        port1Group, joyStick == ppiJoystick1, String.valueOf (i), port1Listener), ""));  //(0,3+i)
      cellList.add (ComponentFactory.setText (ComponentFactory.createRadioButton (
        port2Group, joyStick == ppiJoystick2, String.valueOf (i), port2Listener), ""));  //(1,3+i)
      cellList.add (Multilingual.mlnText (ComponentFactory.createLabel (joyStick.getNameEn ()), "ja", joyStick.getNameJa ()));  //(3,3)
    }
    JScrollPane portMenuPanel = new JScrollPane (
      ComponentFactory.createGridPanel (
        3, 3 + ppiJoysticks.length,
        "paddingLeft=3,paddingRight=3,center",   //gridStyles
        "",  //colStyles
        "italic;italic;colSpan=3,widen",  //rowStyles
        "colSpan=2;rowSpan=2",  //cellStyles
        cellList.toArray (new Object[0])));

    //個々のジョイスティックの設定パネル
    ppiConfigurationScrollPane = new JScrollPane ((((ppiJoystick1 instanceof DummyPad) &&
                                                    !(ppiJoystick2 instanceof DummyPad)) ||  //2だけ接続されている
                                                   (!(ppiJoystick1 instanceof Shiromadokun) &&
                                                    (ppiJoystick2 instanceof Shiromadokun))  //2だけ白窓君
                                                   ? ppiJoystick2 : ppiJoystick1).getConfigurationPanel ());

    //ウインドウ
    ppiFrame = Multilingual.mlnTitle (
      ComponentFactory.createRestorableSubFrame (
        Settings.SGS_PPI_FRAME_KEY,
        "Joystick port settings",
        null,
        ComponentFactory.setEmptyBorder (
          ComponentFactory.createVerticalBox (
            //
            !(PPI_XINPUT_ON && XEiJ.prgIsWindows) ? null :
            ComponentFactory.createFlowPanel (
              ComponentFactory.createCheckBox (ppiXInputOn, "XInput", actionListener)
              ),
            !(PPI_XINPUT_ON && XEiJ.prgIsWindows) ? null : ComponentFactory.createHorizontalSeparator (),
            //
            ComponentFactory.createFlowPanel (
              Multilingual.mlnText (
                ComponentFactory.createCheckBox (
                  ppiJoyKey,
                  "Consider part of keyboard as joystick",
                  actionListener),
                "ja", "キーボードの一部をジョイスティックとみなす")
              ),
            ComponentFactory.createFlowPanel (
              Multilingual.mlnText (
                ComponentFactory.createCheckBox (
                  ppiJoyAuto,
                  "Enabled only while the port is read repeatedly",
                  actionListener),
                "ja", "ポートが繰り返し読み出されている間だけ有効")
              ),
            ComponentFactory.createFlowPanel (
              Multilingual.mlnText (
                ComponentFactory.createCheckBox (
                  ppiJoyBlock,
                  "Remove key input data processed as a joystick operation",
                  actionListener),
                "ja", "ジョイスティック操作として処理されたキー入力データを取り除く")
              ),
            Box.createVerticalStrut (5),
            ComponentFactory.createHorizontalBox (
              ComponentFactory.createHorizontalSplitPane (
                portMenuPanel,
                ppiConfigurationScrollPane)
              )
            ),
          5, 5, 5, 5)),
      "ja", "ジョイスティックポート設定");
  }  //ppiMakeFrame()

  //consume = ppiInput (ke, pressed)
  //  JOYKEYの処理
  //  consume  true=入力をキーボードに渡さない
  public static boolean ppiInput (KeyEvent ke, boolean pressed) {
    boolean consume = false;
    if (ppiJoyKey && (!ppiJoyAuto || XEiJ.mpuClockTime < ppiLastAccessTime + PPI_CONTINUOUS_ACCESS_SPAN)) {
      if (ppiJoystick1.input (ke, pressed) ||
          ppiJoystick2.input (ke, pressed)) {
        //押されたときだけキーボード入力を取り除く
        //  特に自動有効化のときは、押されている間に有効になって離されたデータだけ取り除かれると、
        //  キーボード側は押されたままになっていると判断してリピートが止まらなくなる
        consume = pressed && ppiJoyBlock;
      }
    }
    return consume;
  }


  //  リード
  //  FM音源レジスタのアクセスウェイトのためのPPIの空読みは、ジョイスティックのデータを得ることが目的ではないので、
  //  ジョイスティックポートが連続的に読み出されているとみなさない
  public static int ppiReadByte (int a) {
    int d;
    //
    switch (a & 7) {
      //
    case PPI_PORT_A & 7:
      if (XEiJ.regOC >> 6 != 0b0100_101_000) {  //TST.B以外
        ppiLastAccessTime = XEiJ.mpuClockTime;
      }
      d = ppiJoystick1.readByte () & 0xff;
      break;
      //
    case PPI_PORT_B & 7:
      if (XEiJ.regOC >> 6 != 0b0100_101_000) {  //TST.B以外
        ppiLastAccessTime = XEiJ.mpuClockTime;
      }
      d = ppiJoystick2.readByte () & 0xff;
      break;
      //
    case PPI_PORT_C & 7:
      d = ppiPortCData;
      break;
    default:
      d = 0xff;
    }  //switch a&7
    //
    return d;
  }  //ppiReadByte

  //  ライト
  public static void ppiWriteByte (int a, int d) {
    d &= 0xff;
    //
    switch (a & 7) {
      //
    case PPI_PORT_A & 7:
      ppiJoystick1.writeByte (d);
      break;
      //
    case PPI_PORT_B & 7:
      ppiJoystick2.writeByte (d);
      break;
      //
    case PPI_PORT_C & 7:
      ppiPortCData = d;
      //下位4ビット
      ADPCM.pcmSetPan (d);  //パン
      ADPCM.pcmDivider = d >> 2 & 3;  //分周比。0=1/1024,1=1/768,2=1/512,3=inhibited
      ADPCM.pcmUpdateRepeatInterval ();
      //上位4ビット
      ppiJoystick1.setPin8 (d >> 4 & 1);
      ppiJoystick2.setPin8 (d >> 5 & 1);
      ppiJoystick1.setPin6 ((d >> 6 & 1) ^ 1);
      ppiJoystick1.setPin7 ((d >> 7 & 1) ^ 1);
      break;
      //
    case PPI_CONTROL & 7:
      if ((d & 0b1000_0000) == 0b0000_0000) {  //0b0..._nnnx
        int n = (d >> 1) & 7;  //ビット番号
        int x = d & 1;  //データ
        ppiPortCData = ppiPortCData & ~(1 << n) | x << n;
        if (n < 4) {  //下位4ビット
          switch (n) {
          case 0:
          case 1:
            ADPCM.pcmSetPan (ppiPortCData & 3);  //パン
            break;
          case 2:
          case 3:
            ADPCM.pcmDivider = ppiPortCData >> 2 & 3;  //分周比。0=1/1024,1=1/768,2=1/512,3=inhibited
            ADPCM.pcmUpdateRepeatInterval ();
            break;
          }
        } else {  //上位4ビット
          switch (n) {
          case 4:
            ppiJoystick1.setPin8 (x);
            break;
          case 5:
            ppiJoystick2.setPin8 (x);
            break;
          case 6:
            ppiJoystick1.setPin6 (x ^ 1);
            break;
          case 7:
            ppiJoystick1.setPin7 (x ^ 1);
            break;
          }
        }  //if 下位/上位
      } else if ((d & 0b1000_0100) == 0b1000_0000){  //0b1..._.0..
        //!!! 未対応
      } else {  //0b1..._.1..
        //!!! 非対応
      }
    }  //switch a&7
    //
  }  //ppiWriteByte

}  //class PPI
