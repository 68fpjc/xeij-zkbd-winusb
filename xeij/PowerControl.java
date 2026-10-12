//========================================================================================
//  PowerControl.java
//    en:Power control
//    ja:電源制御
//  Copyright (C) 2003-2026 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.awt.event.*;  //ActionEvent
import java.util.*;  //TimerTask
import javax.swing.*;  //JCheckBoxMenuItem
import javax.swing.event.*;  //ChangeEvent

//class PowerControl
//  電源制御
//  VCC1は以下のいずれかがLowのときON、すべてHighのときOFF
//    電源スイッチ
//      Low=ON,High=OFF
//      VCC2でプルアップされている
//    RTCのALARM
//      ALARM出力は正論理のアラームと1Hzと16HzのNOR(ORしてからNOT)
//      電源スイッチをOFFにしたときのPOWER LED緑の点滅もこれ
//    (POWER LED緑はここまで)
//    拡張スロットA40のEXPWON
//      VCC2でプルアップされている
//      Compactと030CompactにはEXPWONがない
//    REMOTE端子
//      Low=ショート,High=オープン
//      EXPWONに合流している
//      CompactとX68030と030CompactにはREMOTE端子がない
//    システムコントローラのPOWER OFF
//      Low=システムポート$E8E00Fに$0,$F,$Fが書き込まれた,High=その他
//      初代はBUDDHA、XVIはDOSA、X68030はSAKIなど
//    (POWER LED赤はVCC1がOFF)
public class PowerControl {

  //定数
  public static final boolean PWC_ON = true;
  public static final boolean PWC_DEBUG = false;

  //設定
  public static boolean pwcPowerOn;  //電源スイッチの状態。false=OFF,true=ON
  public static boolean pwcExternalOn;  //外部電源スイッチの状態。false=OFF,true=ON
  public static boolean pwcOnAtStartup;  //true=開始時に電源を入れる
  public static boolean pwcOffAtQuit;  //true=終了時に電源を切る
  public static int pwcQuitWait;  //終了時待機時間(秒)。1～300

  //状態
  public static boolean pwcVcc1On;  //VCC1の状態。false=OFF,true=ON
  public static int pwcSystemOffStep;  //システムコントローラのPOWER OFFの状態。0～2=Low,3=High
  public static volatile int pwcQuitStep;  //終了時待機の状態。0=待機前,1=待機中,2=待機後
  public static volatile javax.swing.Timer pwcQuitTimer;  //終了時待機タイマー

  //コンポーネント
  public static JCheckBoxMenuItem pwcOnAtStartupCheckBox;
  public static JCheckBoxMenuItem pwcPowerCheckBox;
  public static JCheckBoxMenuItem pwcExternalCheckBox;
  public static JCheckBoxMenuItem pwcQuitCheckBox;
  public static SpinnerNumberModel pwcQuitModel;
  public static JSpinner pwcQuitSpinner;  //終了時待機時間スピナー

  //pwcInit ()
  //  初期化
  public static void pwcInit () {
    //設定
    pwcPowerOn = Settings.sgsGetOnOff ("powerswitch", false);
    pwcExternalOn = Settings.sgsGetOnOff ("externalswitch", false);
    pwcOnAtStartup = Settings.sgsGetOnOff ("onatstartup", true);
    pwcOffAtQuit = Settings.sgsGetOnOff ("offatquit", true);
    pwcQuitWait = Settings.sgsGetInt ("quitwait", 15, 1, 300);
    //状態
    pwcVcc1On = false;
    pwcSystemOffStep = 3;
    pwcQuitStep = 0;  //終了時待機前
    pwcQuitTimer = null;
  }  //pwcInit

  //pwcTini ()
  //  後始末
  public static void pwcTini () {
    //設定
    Settings.sgsPutOnOff ("powerswitch", pwcPowerOn);
    Settings.sgsPutOnOff ("externalswitch", pwcExternalOn);
    Settings.sgsPutOnOff ("onatstartup", pwcOnAtStartup);
    Settings.sgsPutOnOff ("offatquit", pwcOffAtQuit);
    Settings.sgsPutInt ("quitwait", pwcQuitWait);
  }  //pwcTini

  //pwcMakeMenu ()
  //  電源制御メニューを作る
  public static JMenu pwcMakeMenu () {
    pwcQuitModel = new SpinnerNumberModel (pwcQuitWait, 1, 300, 1);
    pwcQuitSpinner = ComponentFactory.createNumberSpinner (
      pwcQuitModel,
      2,
      (ce) -> {
        pwcQuitWait = pwcQuitModel.getNumber ().intValue ();
      });
    ActionListener listener = (ae) -> {
      Object source = ae.getSource ();
      String command = ae.getActionCommand ();
      switch (command) {
      case "Power switch":
        pwcSetPower (pwcPowerCheckBox.isSelected ());
        break;
      case "External power switch":
        pwcSetRemote (pwcExternalCheckBox.isSelected ());
        break;
      case "Power on at startup":
        pwcOnAtStartup = pwcOnAtStartupCheckBox.isSelected ();
        break;
      case "Power off at quit":
        pwcOffAtQuit = pwcQuitCheckBox.isSelected ();
        break;
      default:
        System.out.println ("unknown action command " + command);
      }
    };
    return Multilingual.mlnText (
      ComponentFactory.createMenu (
        "Power control",
        pwcPowerCheckBox =
        Multilingual.mlnText (
          ComponentFactory.createCheckBoxMenuItem (
            pwcPowerOn,
            "Power switch",
            listener),
          "ja", "電源スイッチ"),
        pwcExternalCheckBox =
        Multilingual.mlnText (
          ComponentFactory.createCheckBoxMenuItem (
            pwcExternalOn,
            "External power switch",
            listener),
          "ja", "外部電源スイッチ"),
        ComponentFactory.createHorizontalSeparator (),
        pwcOnAtStartupCheckBox =
        Multilingual.mlnText (
          ComponentFactory.createCheckBoxMenuItem (
            pwcOnAtStartup,
            "Power on at startup",
            listener),
          "ja", "開始時に電源を入れる"),
        pwcQuitCheckBox =
        Multilingual.mlnText (
          ComponentFactory.createCheckBoxMenuItem (
            pwcOffAtQuit,
            "Power off at quit",
            listener),
          "ja", "終了時に電源を切る"),
        ComponentFactory.createHorizontalBox (
          Box.createHorizontalStrut (20),
          Multilingual.mlnText (
            ComponentFactory.createLabel ("Wait time "),
            "ja", "待機時間 "),
          pwcQuitSpinner,
          Multilingual.mlnText (
            ComponentFactory.createLabel (" s"),
            "ja", " 秒"),
          Box.createHorizontalGlue ()
          )
        ),
      "ja", "電源制御");
  }  //pwcMakeMenu

  //pwcReset ()
  //  RESET命令が実行された
  public static void pwcReset () {
    pwcSystemOffStep = 0;
    if (pwcPowerOn) {
      if (PWC_DEBUG) {
        System.out.println ("power switch is on");
      }
      MC68901.mfpPowerFall ();
      pwcVcc1On ();
    } else {
      if (PWC_DEBUG) {
        System.out.println ("power switch is off");
      }
      MC68901.mfpPowerRise ();
      pwcVcc1Off ();
    }
    if (pwcExternalOn) {
      if (PWC_DEBUG) {
        System.out.println ("external power switch is on");
      }
      MC68901.mfpExpwonFall ();
      pwcVcc1On ();
    } else {
      if (PWC_DEBUG) {
        System.out.println ("external power switch is off");
      }
      MC68901.mfpExpwonRise ();
      pwcVcc1Off ();
    }
  }  //pwcReset

  //pwcSetPower (on)
  //  電源スイッチの状態を設定する
  public static void pwcSetPower (boolean on) {
    if (pwcPowerOn != on) {
      pwcPowerOn = on;
      if (on) {  //OFF→ON
        if (PWC_DEBUG) {
          System.out.println ("power switch turned on");
        }
        MC68901.mfpPowerFall ();
        pwcVcc1On ();
      } else {  //ON→OFF
        if (PWC_DEBUG) {
          System.out.println ("power switch turned off");
        }
        MC68901.mfpPowerRise ();
        pwcVcc1Off ();
      }
    }
  }  //pwcSetPower

  //pwcSetRemote (on)
  //  外部電源スイッチの状態を設定する
  public static void pwcSetRemote (boolean on) {
    if (pwcExternalOn != on) {
      pwcExternalOn = on;
      if (on) {  //OFF→ON
        if (PWC_DEBUG) {
          System.out.println ("external power switch turned on");
        }
        MC68901.mfpExpwonFall ();
        pwcVcc1On ();
      } else {  //ON→OFF
        if (PWC_DEBUG) {
          System.out.println ("external power switch turned off");
        }
        MC68901.mfpExpwonRise ();
        pwcVcc1Off ();
      }
    }
  }  //pwcSetRemote

  //pwcWritePort (d)
  //  システムポート$00E8E00Fに書き込まれた
  public static void pwcWritePort (int d) {
    d &= 0x0f;
    if (pwcSystemOffStep == 0 && d == 0x00) {
      pwcSystemOffStep = 1;
    } else if (pwcSystemOffStep == 1 && d == 0x0f) {
      pwcSystemOffStep = 2;
    } else if (pwcSystemOffStep == 2 && d == 0x0f) {
      pwcSystemOffStep = 3;
      if (PWC_DEBUG) {
        System.out.println ("system port received 0-F-F");
      }
      pwcVcc1Off ();
    } else {
      pwcSystemOffStep = 0;
    }
  }  //pwcWritePort

  //pwcVcc1On ()
  //  条件を満たしていればVCC1をONにする
  public static void pwcVcc1On () {
    //条件を確認する
    if (pwcQuitStep != 0 ||  //終了時待機前でない
        pwcVcc1On ||  //既にON
        (!pwcPowerOn &&  //電源スイッチがOFF(High)
         RP5C15.rtcAlarmHigh &&  //RTCのALARMがHigh
         !pwcExternalOn &&  //外部電源スイッチがOFF(High)
         pwcSystemOffStep == 3)) {  //システムコントローラのPOWER OFFがHigh。すべてHighでOFFの条件が満たされている
      return;
    }
    //VCC1をONにする
    if (PWC_DEBUG) {
      System.out.println ("start turning on");
    }
    pwcVcc1On = true;  //VCC1をON
    Indicator.indUpdateLED ();
    //RP5C15に通知する
    RP5C15.rtcTurnOn ();
    //ハードウェアリセットをかけてMPUを始動する
    XEiJ.mpuReset (-1, -1);
  }  //pwcVcc1On

  //pwcVcc1Off ()
  //  条件を満たしていればVCC1をOFFにする
  public static void pwcVcc1Off () {
    //条件を確認する
    if (!pwcVcc1On ||  //既にOFF
        (pwcPowerOn ||  //電源スイッチがON(Low)
         !RP5C15.rtcAlarmHigh ||  //RTCのALARMがLow
         pwcExternalOn ||  //外部電源スイッチがON(Low)
         pwcSystemOffStep < 3)) {  //システムコントローラのPOWER OFFがLow。いずれかがLowでONの条件を満たしている
      return;
    }
    //VCC1をOFFにする
    if (PWC_DEBUG) {
      System.out.println ("start turning off");
    }
    pwcVcc1On = false;  //VCC1をOFF
    Indicator.indUpdateLED ();
    //RP5C15に通知する
    RP5C15.rtcTurnOff ();
    //MPUを停止させる
    //  VCC1がOFFなのでMPUが停止しているとき有効なボタンも無効になる
    XEiJ.mpuStop (null);
    //画面を暗くする
    CoreTask.add (() -> {
      VideoController.vcnTargetScaledContrast = 0;
      VideoController.vcnCurrentScaledContrast = 0;
      VideoController.vcnSetContrast (0);
      CRTC.crtRepaint ();
      //終了時待機中ならば直ちに終了する
      if (pwcQuitStep == 1) {  //終了時待機中
        pwcQuitCall (0);
      }
    });
  }  //pwcVcc1Off

  //pwcStartup ()
  //  開始処理
  public static void pwcStartup () {
    //開始時に電源を入れる
    if (pwcOnAtStartup) {
      pwcSystemOffStep = 0;
      if (!pwcPowerOn) {
        pwcPowerOn = true;
        pwcPowerCheckBox.setSelected (true);
      }
    }
    //電源が入る条件が揃っていたら電源を入れる
    pwcVcc1On ();
  }  //pwcStartup

  //pwcQuit ()
  //  終了処理
  //  ウインドウのクローズボタンが押された
  //  ファイルメニューの終了が選択された
  public static void pwcQuit () {
    //終了時待機なしのとき直ちに終了する
    if (!pwcOffAtQuit) {  //終了時待機なし
      pwcQuitEnd ();  //終了する
      return;
    }
    //終了時待機前でないとき何もしない
    if (pwcQuitStep != 0) {  //終了時待機前でない
      return;
    }
    //VCC1がOFFのとき直ちに終了する
    if (!pwcVcc1On) {  //VCC1がOFF
      pwcQuitEnd ();  //終了する
      return;
    }
    //終了時待機中に移行する
    if (PWC_DEBUG) {
      System.out.println ("start quit");
    }
    pwcQuitStep = 1;  //終了時待機中
    //電源スイッチをOFFにする
    //  MFPのI2ピンがHighになり電源OFF割り込みが発生する
    if (pwcPowerOn &&
        pwcPowerCheckBox != null &&
        pwcPowerCheckBox.isSelected ()) {  //電源スイッチがON
      pwcPowerCheckBox.doClick ();  //電源スイッチをOFFにする
    }
    //外部電源スイッチをOFFにする
    //  MFPのI1ピンがHighになり外部電源OFF割り込みが発生する
    if (pwcExternalOn &&
        pwcExternalCheckBox != null &&
        pwcExternalCheckBox.isSelected ()) {  //外部電源スイッチがON
      pwcExternalCheckBox.doClick ();  //外部電源スイッチをOFFにする
    }
    //終了時待機時間後に終了する
    pwcQuitCall (1000 * pwcQuitWait);
  }  //pwcQuit

  //pwcQuitCall (delay)
  //  時間が経ったら終了する
  public static void pwcQuitCall (int delay) {
    if (pwcQuitTimer != null) {
      pwcQuitTimer.stop ();
      //pwcQuitTimer = null;
    }
    pwcQuitTimer = new javax.swing.Timer (
      delay,
      (ae) -> {
        pwcQuitEnd ();  //終了する
      });
    pwcQuitTimer.start ();
  }  //pwcQuitCall

  //pwcQuitEnd ()
  //  終了する
  public static void pwcQuitEnd () {
    //終了時待機タイマーを停止する
    if (pwcQuitTimer != null) {
      pwcQuitTimer.stop ();
      pwcQuitTimer = null;
    }
    //終了時待機後でなければ終了する
    if (pwcQuitStep != 2) {
      pwcQuitStep = 2;  //終了時待機後
      XEiJ.prgTini ();  //終了する
    }
  }  //pwcQuitEnd

}  //class PowerControl
