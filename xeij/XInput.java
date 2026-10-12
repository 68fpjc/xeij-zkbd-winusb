//========================================================================================
//  XInput.java
//    en:XInput Gamepad
//    ja:XInput Gamepad
//  Copyright (C) 2003-2026 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.lang.foreign.*;  //Arena,FunctionDescriptor,Linker,MemorySegment,SymbolLookup,ValueLayout
import java.lang.invoke.*;  //MethodHandle
import java.util.*;

//class XInput
//  XInputクラス
public final class XInput implements SimpleTask.Runner {



  //公開クラス定数

  //ボタンのビット
  public static final int UP_BIT        = 0;  //上ボタン
  public static final int DOWN_BIT      = 1;  //下ボタン
  public static final int LEFT_BIT      = 2;  //左ボタン
  public static final int RIGHT_BIT     = 3;  //右ボタン
  public static final int START_BIT     = 4;  //STARTボタン
  public static final int BACK_BIT      = 5;  //BACKボタン
  public static final int LSTICK_BIT    = 6;  //左スティックボタン
  public static final int RSTICK_BIT    = 7;  //右スティックボタン
  public static final int LB_BIT        = 8;  //LBボタン
  public static final int RB_BIT        = 9;  //RBボタン
  public static final int A_BIT         = 12;  //Aボタン
  public static final int B_BIT         = 13;  //Bボタン
  public static final int X_BIT         = 14;  //Xボタン
  public static final int Y_BIT         = 15;  //Yボタン
  public static final int LSUP_BIT      = 16;  //左スティック上
  public static final int LSDOWN_BIT    = 17;  //左スティック下
  public static final int LSLEFT_BIT    = 18;  //左スティック左
  public static final int LSRIGHT_BIT   = 19;  //左スティック右
  public static final int RSUP_BIT      = 20;  //右スティック上
  public static final int RSDOWN_BIT    = 21;  //右スティック下
  public static final int RSLEFT_BIT    = 22;  //右スティック左
  public static final int RSRIGHT_BIT   = 23;  //右スティック右
  public static final int LTRIGGER_BIT  = 24;  //左トリガー
  public static final int RTRIGGER_BIT  = 25;  //右トリガー
  public static final int BUTTONS       = 26;  //ボタンの数

  //ボタンのマスク
  public static final int UP_MASK       = 1 << UP_BIT;
  public static final int DOWN_MASK     = 1 << DOWN_BIT;
  public static final int LEFT_MASK     = 1 << LEFT_BIT;
  public static final int RIGHT_MASK    = 1 << RIGHT_BIT;
  public static final int START_MASK    = 1 << START_BIT;
  public static final int BACK_MASK     = 1 << BACK_BIT;
  public static final int LSTICK_MASK   = 1 << LSTICK_BIT;
  public static final int RSTICK_MASK   = 1 << RSTICK_BIT;
  public static final int LB_MASK       = 1 << LB_BIT;
  public static final int RB_MASK       = 1 << RB_BIT;
  public static final int A_MASK        = 1 << A_BIT;
  public static final int B_MASK        = 1 << B_BIT;
  public static final int X_MASK        = 1 << X_BIT;
  public static final int Y_MASK        = 1 << Y_BIT;
  public static final int LSUP_MASK     = 1 << LSUP_BIT;
  public static final int LSDOWN_MASK   = 1 << LSDOWN_BIT;
  public static final int LSLEFT_MASK   = 1 << LSLEFT_BIT;
  public static final int LSRIGHT_MASK  = 1 << LSRIGHT_BIT;
  public static final int RSUP_MASK     = 1 << RSUP_BIT;
  public static final int RSDOWN_MASK   = 1 << RSDOWN_BIT;
  public static final int RSLEFT_MASK   = 1 << RSLEFT_BIT;
  public static final int RSRIGHT_MASK  = 1 << RSRIGHT_BIT;
  public static final int LTRIGGER_MASK = 1 << LTRIGGER_BIT;
  public static final int RTRIGGER_MASK = 1 << RTRIGGER_BIT;

  //ボタンの名前
  public static final String BIT_TO_TEXT[] = new String[] {
    "UP", "DOWN", "LEFT", "RIGHT", "START", "BACK", "LSTICK", "RSTICK",
    "LB", "RB", "(10)", "(11)", "A", "B", "X", "Y",
    "LSUP", "LSDOWN", "LSLEFT", "LSRIGHT", "RSUP", "RSDOWN", "RSLEFT", "RSRIGHT",
    "LTRIGGER", "RTRIGGER",
  };



  //非公開クラス定数

  //型
  protected static final ValueLayout.OfInt DWORD = ValueLayout.JAVA_INT;
  protected static final ValueLayout.OfShort WORD = ValueLayout.JAVA_SHORT;
  protected static final ValueLayout.OfByte BYTE = ValueLayout.JAVA_BYTE;
  protected static final ValueLayout.OfShort SHORT = ValueLayout.JAVA_SHORT;

  //エラーコード
  protected static final int ERROR_SUCCESS = 0;
  //protected static final int ERROR_DEVICE_NOT_CONNECTED = 1167;

  //構造体
  //  XINPUT_GAMEPAD
  //  https://learn.microsoft.com/ja-jp/windows/win32/api/xinput/ns-xinput-xinput_gamepad
  protected static final MemoryLayout GAMEPAD_LAYOUT = MemoryLayout.structLayout (
    WORD.withName ("wButtons"),
    BYTE.withName ("bLeftTrigger"),
    BYTE.withName ("bRightTrigger"),
    SHORT.withName ("sThumbLX"),
    SHORT.withName ("sThumbLY"),
    SHORT.withName ("sThumbRX"),
    SHORT.withName ("sThumbRY"));
  protected static final long OFFSET_wButtons = GAMEPAD_LAYOUT.byteOffset (MemoryLayout.PathElement.groupElement ("wButtons"));
  protected static final long OFFSET_bLeftTrigger = GAMEPAD_LAYOUT.byteOffset (MemoryLayout.PathElement.groupElement ("bLeftTrigger"));
  protected static final long OFFSET_bRightTrigger = GAMEPAD_LAYOUT.byteOffset (MemoryLayout.PathElement.groupElement ("bRightTrigger"));
  protected static final long OFFSET_sThumbLX = GAMEPAD_LAYOUT.byteOffset (MemoryLayout.PathElement.groupElement ("sThumbLX"));
  protected static final long OFFSET_sThumbLY = GAMEPAD_LAYOUT.byteOffset (MemoryLayout.PathElement.groupElement ("sThumbLY"));
  protected static final long OFFSET_sThumbRX = GAMEPAD_LAYOUT.byteOffset (MemoryLayout.PathElement.groupElement ("sThumbRX"));
  protected static final long OFFSET_sThumbRY = GAMEPAD_LAYOUT.byteOffset (MemoryLayout.PathElement.groupElement ("sThumbRY"));
  //  XINPUT_STATE
  //  https://learn.microsoft.com/ja-jp/windows/win32/api/xinput/ns-xinput-xinput_state
  protected static final MemoryLayout STATE_LAYOUT = MemoryLayout.structLayout (
    DWORD.withName ("dwPacketNumber"),
    GAMEPAD_LAYOUT.withName ("Gamepad"));
  protected static final long OFFSET_dwPacketNumber = STATE_LAYOUT.byteOffset (MemoryLayout.PathElement.groupElement ("dwPacketNumber"));



  //非公開インスタンス変数

  //ゲームパッド
  protected Gamepad[] gamepads;

  //リスナー
  protected ArrayList<GamepadListener> gamepadListeners;

  //アリーナ
  protected Arena arena;

  //タスク
  //  使用できるコントローラがあるときは10ms間隔、さもなくば500ms間隔でポーリングする
  protected static final long ACTIVE_DELAY = 10L;
  protected static final long INACTIVE_DELAY = 500L;
  protected SimpleTask task;

  //関数
  protected MethodHandle XInputGetState;

  //構造体
  protected MemorySegment state;
  protected MemorySegment gamepad;



  //公開コンストラクタ

  //new XInput ()
  //  コンストラクタ
  public XInput () {
    //ゲームパッドを開く
    gamepads = new Gamepad[4];
    for (int index = 0; index < 4; index++) {
      gamepads[index] = new Gamepad (index);
    }
    //リスナーを初期化する
    gamepadListeners = new ArrayList<GamepadListener> ();
    //アリーナを開く
    arena = Arena.global ();
    //ライブラリを開く
    SymbolLookup xinputLibrary = SymbolLookup.libraryLookup ("XInput1_4.dll", arena);
    //関数をリンクする
    Linker linker = Linker.nativeLinker ();
    //  XInputGetState
    //  https://learn.microsoft.com/ja-jp/windows/win32/api/xinput/nf-xinput-xinputgetstate
    XInputGetState = linker.downcallHandle (
      xinputLibrary.findOrThrow ("XInputGetState"),
      FunctionDescriptor.of (
        DWORD,  //return
        DWORD,  //dwUserIndex
        ValueLayout.ADDRESS));  //XINPUT_STATE *
    //構造体の領域を確保する
    state = arena.allocate (STATE_LAYOUT);
    gamepad = state.asSlice (STATE_LAYOUT.byteOffset (MemoryLayout.PathElement.groupElement ("Gamepad")));
    //ポーリングを開始する
    task = new SimpleTask (XEiJ.tmrScheduler, this).fixedDelay (INACTIVE_DELAY, INACTIVE_DELAY);
  }



  //スレッド

  //runN (n)
  @Override public int runN (int n) {
    boolean available = false;  //true=使用できるコントローラがある
    for (int index = 0; index < 4; index++) {
      Gamepad gamepad = gamepads[index];
      if (gamepad.getState ()) {  //状態が変化した
        if (gamepad.isConnected ()) {  //接続された
          for (GamepadListener listener : gamepadListeners) {
            listener.connected (gamepad);
          }
        } else if (gamepad.isDisconnected ()) {  //切断された
          for (GamepadListener listener : gamepadListeners) {
            listener.disconnected (gamepad);
          }
        }
        if (gamepad.isAvailable ()) {  //使用できる
          available = true;  //使用できるコントローラがある
          int buttonMasks = gamepad.getPressedButtonMasks ();
          if (buttonMasks != 0) {  //押されたボタンがある
            for (GamepadListener listener : gamepadListeners) {
              listener.buttonPressed (gamepad, buttonMasks);
            }
          }
          buttonMasks = gamepad.getReleasedButtonMasks ();
          if (buttonMasks != 0) {  //離されたボタンがある
            for (GamepadListener listener : gamepadListeners) {
              listener.buttonReleased (gamepad, buttonMasks);
            }
          }
          if (gamepad.isLeftStickMovedX ()) {  //左スティックがX方向に動いた
            for (GamepadListener listener : gamepadListeners) {
              listener.leftStickMovedX (gamepad);
            }
          }
          if (gamepad.isLeftStickMovedY ()) {  //左スティックがY方向に動いた
            for (GamepadListener listener : gamepadListeners) {
              listener.leftStickMovedY (gamepad);
            }
          }
          if (gamepad.isLeftTriggerMoved ()) {  //左トリガーが動いた
            for (GamepadListener listener : gamepadListeners) {
              listener.leftTriggerMoved (gamepad);
            }
          }
          if (gamepad.isRightStickMovedX ()) {  //右スティックがX方向に動いた
            for (GamepadListener listener : gamepadListeners) {
              listener.rightStickMovedX (gamepad);
            }
          }
          if (gamepad.isRightStickMovedY ()) {  //右スティックがY方向に動いた
            for (GamepadListener listener : gamepadListeners) {
              listener.rightStickMovedY (gamepad);
            }
          }
          if (gamepad.isRightTriggerMoved ()) {  //右トリガーが動いた
            for (GamepadListener listener : gamepadListeners) {
              listener.rightTriggerMoved (gamepad);
            }
          }
        }  //if 使用できる
      }  //if 変化したかつリスナーがある
    }  //for index
    task.setDelay (available ? ACTIVE_DELAY :  //使用できるコントローラがある
                   INACTIVE_DELAY);  //使用できるコントローラがない
    return 1;
  }  //runN



  //公開インスタンスメソッド

  //end ()
  //  終了する
  public void end () {
    //ポーリングを終了する
    task.stop ();
    //アリーナを閉じる
    //arena.close ();
    //ゲームパッドを閉じる
    for (int index = 0; index < 4; index++) {
      gamepads[index].close ();
    }
  }

  //buttonMasks = getButtonMasks (index)
  //  すべての押されているボタンのマスクを返す
  //  index  コントローラのインデックス
  //  buttonMasks  すべての押されているボタンのマスク
  public int getButtonMasks (int index) {
    return gamepads[index].getButtonMasks ();
  }

  //leftStickX = getLeftStickX (index)
  //  左スティックのX方向の位置を返す
  //  index  コントローラのインデックス
  //  leftStickX  左スティックのX方向の位置
  public int getLeftStickX (int index) {
    return gamepads[index].getLeftStickX ();
  }

  //leftStickY = getLeftStickY (index)
  //  左スティックのY方向の位置を返す
  //  index  コントローラのインデックス
  //  leftStickY  左スティックのY方向の位置
  public int getLeftStickY (int index) {
    return gamepads[index].getLeftStickY ();
  }

  //leftTrigger = getLeftTrigger (index)
  //  左トリガーの位置を返す
  //  index  コントローラのインデックス
  //  leftTrigger  左トリガーの位置
  public int getLeftTrigger (int index) {
    return gamepads[index].getLeftTrigger ();
  }

  //rightStickX = getRightStickX (index)
  //  右スティックのX方向の位置を返す
  //  index  コントローラのインデックス
  //  rightStickX  右スティックのX方向の位置
  public int getRightStickX (int index) {
    return gamepads[index].getRightStickX ();
  }

  //rightStickY = getRightStickY (index)
  //  右スティックのY方向の位置を返す
  //  index  コントローラのインデックス
  //  rightStickY  右スティックのY方向の位置
  public int getRightStickY (int index) {
    return gamepads[index].getRightStickY ();
  }

  //rightTrigger = getRightTrigger (index)
  //  右トリガーの位置を返す
  //  index  コントローラのインデックス
  //  rightTrigger  右トリガーの位置
  public int getRightTrigger (int index) {
    return gamepads[index].getRightTrigger ();
  }

  //available = isAvailable (index)
  //  使用できるかを返す
  //  index  コントローラのインデックス
  //  available  使用できるか
  public boolean isAvailable (int index) {
    return gamepads[index].isAvailable ();
  }

  //setThresholdOfLeftStick (index, thresholdOfLeftStick)
  //  左スティックの閾値を設定する
  //  index  コントローラのインデックス
  //  thresholdOfLeftStick  左スティックの閾値
  public void setThresholdOfLeftStick (int index, int thresholdOfLeftStick) {
    gamepads[index].setThresholdOfLeftStick (thresholdOfLeftStick);
  }

  //setThresholdOfRightStick (index, thresholdOfRightStick)
  //  右スティックの閾値を設定する
  //  index  コントローラのインデックス
  //  thresholdOfRightStick  右スティックの閾値
  public void setThresholdOfRightStick (int index, int thresholdOfRightStick) {
    gamepads[index].setThresholdOfRightStick (thresholdOfRightStick);
  }

  //setThresholdOfLeftTrigger (index, thresholdOfLeftTrigger)
  //  左トリガーの閾値を設定する
  //  index  コントローラのインデックス
  //  thresholdOfLeftTrigger  左トリガーの閾値
  public void setThresholdOfLeftTrigger (int index, int thresholdOfLeftTrigger) {
    gamepads[index].setThresholdOfLeftTrigger (thresholdOfLeftTrigger);
  }

  //setThresholdOfRightTrigger (index, thresholdOfRightTrigger)
  //  右トリガーの閾値を設定する
  //  index  コントローラのインデックス
  //  thresholdOfRightTrigger  右トリガーの閾値
  public void setThresholdOfRightTrigger (int index, int thresholdOfRightTrigger) {
    gamepads[index].setThresholdOfRightTrigger (thresholdOfRightTrigger);
  }



  //ゲームパッドリスナー

  //interface XInput.GamepadListener
  //  ゲームパッドリスナー
  public interface GamepadListener {
    public void connected (Gamepad gamepad);  //接続された
    public void disconnected (Gamepad gamepad);  //切断された
    public void buttonPressed (Gamepad gamepad, int buttonMasks);  //ボタンが押された
    public void buttonReleased (Gamepad gamepad, int buttonMasks);  //ボタンが離された
    public void leftStickMovedX (Gamepad gamepad);  //左スティックがX方向に動いた
    public void leftStickMovedY (Gamepad gamepad);  //左スティックがY方向に動いた
    public void leftTriggerMoved (Gamepad gamepad);  //左トリガーが動いた
    public void rightStickMovedX (Gamepad gamepad);  //右スティックがX方向に動いた
    public void rightStickMovedY (Gamepad gamepad);  //右スティックがY方向に動いた
    public void rightTriggerMoved (Gamepad gamepad);  //右トリガーが動いた
  }

  //addGamepadListener (listener)
  //  ゲームパッドリスナーを追加する
  //  listener  追加するGamepadリスナー
  public void addGamepadListener (GamepadListener listener) {
    if (listener != null && !gamepadListeners.contains (listener)) {
      gamepadListeners.add (listener);
    }
  }

  //removeGamepadListener (listener)
  //  ゲームパッドリスナーを削除する
  //  listener  削除するGamepadリスナー
  public void removeGamepadListener (GamepadListener listener) {
    gamepadListeners.remove (listener);
  }

  //removeGamepadListeners ()
  //  すべてのゲームパッドリスナーを削除する
  public void removeGamepadListeners () {
    gamepadListeners.clear ();
  }

  //getGamepadListeners ()
  //  すべてのゲームパッドリスナーを取得する
  public GamepadListener[] getGamepadListeners () {
    return gamepadListeners.toArray (new GamepadListener[gamepadListeners.size ()]);
  }



  //ゲームパッドアダプター

  //class XInput.GamepadAdapter
  //  ゲームパッドアダプター
  public static class GamepadAdapter implements GamepadListener {
    @Override public void connected (Gamepad gamepad) {
    }
    @Override public void disconnected (Gamepad gamepad) {
    }
    @Override public void buttonPressed (Gamepad gamepad, int buttonMasks) {
    }
    @Override public void buttonReleased (Gamepad gamepad, int buttonMasks) {
    }
    @Override public void leftStickMovedX (Gamepad gamepad) {
    }
    @Override public void leftStickMovedY (Gamepad gamepad) {
    }
    @Override public void leftTriggerMoved (Gamepad gamepad) {
    }
    @Override public void rightStickMovedX (Gamepad gamepad) {
    }
    @Override public void rightStickMovedY (Gamepad gamepad) {
    }
    @Override public void rightTriggerMoved (Gamepad gamepad) {
    }
  }



  //ゲームパッドクラス

  //class XInput.Gamepad
  //  ゲームパッドクラス
  public class Gamepad implements AutoCloseable {

    //閾値
    //  スティックの閾値を小さくしすぎると初動の方向がブレやすくなる
    //  トリガーは小さくしても押し込まないと入らない印象。コントローラにもよるのだろう
    static final int THRESHOLD_OF_STICK = (32768 / 2);
    static final int THRESHOLD_OF_TRIGGER = (256 / 4);

    //ワークエリア
    int index;  //コントローラのインデックス。0～3
    //  今回の状態
    boolean available;  //true=XInputGetStateがERROR_SUCCESSを返した
    int dwPacketNumber;  //ここからXInputGetStateが出力したXINPUT_STATE構造体から取り出した値
    int wButtons;
    int bLeftTrigger;
    int bRightTrigger;
    int sThumbLX;
    int sThumbLY;
    int sThumbRX;
    int sThumbRY;  //ここまで
    int buttonMasks;  //スティックとトリガーの位置を加えたボタンのマスク
    //  前回の状態
    boolean previous_available;
    int previous_dwPacketNumber;
    int previous_wButtons;
    int previous_bLeftTrigger;
    int previous_bRightTrigger;
    int previous_sThumbLX;
    int previous_sThumbLY;
    int previous_sThumbRX;
    int previous_sThumbRY;
    int previous_buttonMasks;
    //  設定
    long thresholdOfLeftStick2;  //左スティックの閾値の2乗
    long thresholdOfRightStick2;  //右スティックの閾値の2乗
    int thresholdOfLeftTrigger;  //左トリガーの閾値
    int thresholdOfRightTrigger;  //右トリガーの閾値

    //new Gamepad (index)
    //  コンストラクタ
    //  index  コントローラのインデックス。0～3
    public Gamepad (int index) {
      open (index);
    }

    //close ()
    //  閉じる
    @Override public void close () {
      available = false;
      dwPacketNumber = 0;
      wButtons = 0;
      bLeftTrigger = 0;
      bRightTrigger = 0;
      sThumbLX = 0;
      sThumbLY = 0;
      sThumbRX = 0;
      sThumbRY = 0;
      buttonMasks = 0;
      previous_available = false;
      previous_dwPacketNumber = 0;
      previous_wButtons = 0;
      previous_bLeftTrigger = 0;
      previous_bRightTrigger = 0;
      previous_sThumbLX = 0;
      previous_sThumbLY = 0;
      previous_sThumbRX = 0;
      previous_sThumbRY = 0;
      previous_buttonMasks = 0;
    }

    //buttonMasks = getButtonMasks ()
    //  getState()で取得したすべての押されているボタンのマスクを返す
    //  buttonMasks  すべての押されているボタンのマスク
    public int getButtonMasks () {
      return buttonMasks;
    }

    //index = getIndex ()
    //  コントローラのインデックスを返す
    //  index  コントローラのインデックス
    public int getIndex () {
      return index;
    }

    //leftStickX = getLeftStickX ()
    //  getState()で取得した左スティックのX方向の位置を返す
    //  leftStickX  左スティックのX方向の位置
    public int getLeftStickX () {
      return sThumbLX;
    }

    //leftStickY = getLeftStickY ()
    //  getState()で取得した左スティックのY方向の位置を返す
    //  leftStickY  左スティックのY方向の位置
    public int getLeftStickY () {
      return sThumbLY;
    }

    //leftTrigger = getLeftTrigger ()
    //  getState()で取得した左トリガーの位置を返す
    //  leftTrigger  左トリガーの位置
    public int getLeftTrigger () {
      return bLeftTrigger;
    }

    //pressedButtonMasks = getPressedButtonMasks ()
    //  getState()で取得したすべての押されたボタンのマスクを返す
    //  pressedButtonMasks  すべての押されたボタンのマスク
    public int getPressedButtonMasks () {
      return buttonMasks & ~previous_buttonMasks;
    }

    //releasedButtonMasks = getReleasedButtonMasks ()
    //  getState()で取得したすべての離されたボタンのマスクを返す
    //  releasedButtonMasks  すべての離されたボタンのマスク
    public int getReleasedButtonMasks () {
      return ~buttonMasks & previous_buttonMasks;
    }

    //rightStickX = getRightStickX ()
    //  getState()で取得した右スティックのX方向の位置を返す
    //  rightStickX  右スティックのX方向の位置
    public int getRightStickX () {
      return sThumbRX;
    }

    //rightStickY = getRightStickY ()
    //  getState()で取得した右スティックのY方向の位置を返す
    //  rightStickY  右スティックのY方向の位置
    public int getRightStickY () {
      return sThumbRY;
    }

    //rightTrigger = getRightTrigger ()
    //  getState()で取得した右トリガーの位置を返す
    //  rightTrigger  右トリガーの位置
    public int getRightTrigger () {
      return bRightTrigger;
    }

    //changed = getState ()
    //  コントローラの状態を取得する
    //  changed  false=変化しなかった,true=変化した。使用可能とは限らない
    public boolean getState () {
      //前回の状態を保存する
      previous_available = available;
      previous_wButtons = wButtons;
      previous_bLeftTrigger = bLeftTrigger;
      previous_bRightTrigger = bRightTrigger;
      previous_sThumbLX = sThumbLX;
      previous_sThumbLY = sThumbLY;
      previous_sThumbRX = sThumbRX;
      previous_sThumbRY = sThumbRY;
      previous_buttonMasks = buttonMasks;
      //今回の状態を取得する
      available = XInputGetState != null;
      if (available) {
        try {
          int result = (int) XInputGetState.invoke (index, state);
          available = result == ERROR_SUCCESS;
        } catch (Throwable e) {
          //e.printStackTrace ();
        }
      }
      if (available) {  //使用できる
        dwPacketNumber = state.get (DWORD, OFFSET_dwPacketNumber);
        wButtons = (char) gamepad.get (WORD, OFFSET_wButtons);
        bLeftTrigger = 0xff & gamepad.get (BYTE, OFFSET_bLeftTrigger);
        bRightTrigger = 0xff & gamepad.get (BYTE, OFFSET_bRightTrigger);
        sThumbLX = gamepad.get (SHORT, OFFSET_sThumbLX);
        sThumbLY = gamepad.get (SHORT, OFFSET_sThumbLY);
        sThumbRX = gamepad.get (SHORT, OFFSET_sThumbRX);
        sThumbRY = gamepad.get (SHORT, OFFSET_sThumbRY);
      } else {  //使用できない
        dwPacketNumber = 0;
        wButtons = 0;
        bLeftTrigger = 0;
        bRightTrigger = 0;
        sThumbLX = 0;
        sThumbLY = 0;
        sThumbRX = 0;
        sThumbRY = 0;
      }
      //ボタンのマスクにスティックとトリガーの位置を加える
      int masks = wButtons;
      //左スティック
      {
        int x = sThumbLX;
        int y = sThumbLY;
        if (thresholdOfLeftStick2 < ((long) x * (long) x + (long) y * (long) y)) {  //閾値の円盤の外側。オーバーフローに注意
          int px = 5741 * x - 2378 * y;  //+xから+yへpi/8回転させた方向。tan(pi/8)≒2378/5741
          int py = 2378 * x + 5741 * y;
          int qx = 5741 * x + 2378 * y;  //+xから+yへ-pi/8回転させた方向
          int qy = -2378 * x + 5741 * y;
          masks |= ((0 <= (-px | -qx) ? LSLEFT_MASK : 0 <= (px | qx) ? LSRIGHT_MASK : 0) |
                    (0 <= (-py | -qy) ? LSDOWN_MASK : 0 <= (py | qy) ? LSUP_MASK : 0));  //8方向判別
        }
      }
      //右スティック
      {
        int x = sThumbRX;
        int y = sThumbRY;
        if (thresholdOfRightStick2 < ((long) x * (long) x + (long) y * (long) y)) {  //閾値の円盤の外側。オーバーフローに注意
          int px = 5741 * x - 2378 * y;  //+xから+yへpi/8回転させた方向。tan(pi/8)≒2378/5741
          int py = 2378 * x + 5741 * y;
          int qx = 5741 * x + 2378 * y;  //+xから+yへ-pi/8回転させた方向
          int qy = -2378 * x + 5741 * y;
          masks |= ((0 <= (-px | -qx) ? RSLEFT_MASK : 0 <= (px | qx) ? RSRIGHT_MASK : 0) |
                    (0 <= (-py | -qy) ? RSDOWN_MASK : 0 <= (py | qy) ? RSUP_MASK : 0));  //8方向判別
        }
      }
      //左トリガー
      if (thresholdOfLeftTrigger < bLeftTrigger) {  //閾値より大きい
        masks |= LTRIGGER_MASK;
      }
      //右トリガー
      if (thresholdOfRightTrigger < bRightTrigger) {  //閾値より大きい
        masks |= RTRIGGER_MASK;
      }
      buttonMasks = masks;
      //変化したかどうかを返す
      return ((available != previous_available) ||  //使用できるかが変化したか
              (available &&  //使用できて
               dwPacketNumber != previous_dwPacketNumber));  //変化した
    }

    //available = isAvailable ()
    //  使用できるかを返す
    //  available  使用できるか
    public boolean isAvailable () {
      return available;
    }

    //connected = isConnected ()
    //  接続されたかを返す
    //  connected  接続されたか
    public boolean isConnected () {
      return available && !previous_available;
    }

    //disconnected = isDisconnected ()
    //  切断されたかを返す
    //  disconnected  切断されたか
    public boolean isDisconnected () {
      return !available && previous_available;
    }

    //leftStickMovedX = isLeftStickMovedX ()
    //  getState()で取得した左スティックがX方向に動いたかを返す
    //  leftStickMovedX  左スティックがX方向に動いたか
    public boolean isLeftStickMovedX () {
      return sThumbLX != previous_sThumbLX;
    }

    //leftStickMovedY = isLeftStickMovedY ()
    //  getState()で取得した左スティックがY方向に動いたかを返す
    //  leftStickMovedY  左スティックがY方向に動いたか
    public boolean isLeftStickMovedY () {
      return sThumbLY != previous_sThumbLY;
    }

    //leftTriggerMoved = isLeftTriggerMoved ()
    //  getState()で取得した左トリガーが動いたかを返す
    //  leftTriggerMoved  左トリガーが動いたか
    public boolean isLeftTriggerMoved () {
      return bLeftTrigger != previous_bLeftTrigger;
    }

    //rightStickMovedX = isRightStickMovedX ()
    //  getState()で取得した右スティックがX方向に動いたかを返す
    //  rightStickMovedX  右スティックがX方向に動いたか
    public boolean isRightStickMovedX () {
      return sThumbRX != previous_sThumbRX;
    }

    //rightStickMovedY = isRightStickMovedY ()
    //  getState()で取得した右スティックがY方向に動いたかを返す
    //  rightStickMovedY  右スティックがY方向に動いたか
    public boolean isRightStickMovedY () {
      return sThumbRY != previous_sThumbRY;
    }

    //rightTriggerMoved = isRightTriggerMoved ()
    //  getState()で取得した右トリガーが動いたかを返す
    //  rightTriggerMoved  右トリガーが動いたか
    public boolean isRightTriggerMoved () {
      return bRightTrigger != previous_bRightTrigger;
    }

    //open (index)
    //  開く
    //  index  コントローラのインデックス。0～3
    private void open (int index) {
      //コントローラのインデックスを確認する
      if (index < 0 || 3 < index) {
        throw new IllegalArgumentException (String.format ("Controller %d cannot be specified", index));
      }
      //ワークエリアを初期化する
      this.index = index;
      available = false;
      dwPacketNumber = 0;
      wButtons = 0;
      bLeftTrigger = 0;
      bRightTrigger = 0;
      sThumbLX = 0;
      sThumbLY = 0;
      sThumbRX = 0;
      sThumbRY = 0;
      buttonMasks = 0;
      previous_available = false;
      previous_dwPacketNumber = 0;
      previous_wButtons = 0;
      previous_bLeftTrigger = 0;
      previous_bRightTrigger = 0;
      previous_sThumbLX = 0;
      previous_sThumbLY = 0;
      previous_sThumbRX = 0;
      previous_sThumbRY = 0;
      previous_buttonMasks = 0;
      thresholdOfLeftStick2 = (long) THRESHOLD_OF_STICK * (long) THRESHOLD_OF_STICK;
      thresholdOfRightStick2 = (long) THRESHOLD_OF_STICK * (long) THRESHOLD_OF_STICK;
      thresholdOfLeftTrigger = THRESHOLD_OF_TRIGGER;
      thresholdOfRightTrigger = THRESHOLD_OF_TRIGGER;
    }

    //setThresholdOfLeftStick (thresholdOfLeftStick)
    //  左スティックの閾値を設定する
    //  thresholdOfLeftStick  左スティックの閾値
    public void setThresholdOfLeftStick (int thresholdOfLeftStick) {
      thresholdOfLeftStick2 = (long) thresholdOfLeftStick * (long) thresholdOfLeftStick;
    }

    //setThresholdOfRightStick (thresholdOfRightStick)
    //  右スティックの閾値を設定する
    //  thresholdOfRightStick  右スティックの閾値
    public void setThresholdOfRightStick (int thresholdOfRightStick) {
      thresholdOfRightStick2 = (long) thresholdOfRightStick * (long) thresholdOfRightStick;
    }

    //setThresholdOfLeftTrigger (thresholdOfLeftTrigger)
    //  左トリガーの閾値を設定する
    //  thresholdOfLeftTrigger  左トリガーの閾値
    public void setThresholdOfLeftTrigger (int thresholdOfLeftTrigger) {
      this.thresholdOfLeftTrigger = thresholdOfLeftTrigger;
    }

    //setThresholdOfRightTrigger (thresholdOfRightTrigger)
    //  右トリガーの閾値を設定する
    //  thresholdOfRightTrigger  右トリガーの閾値
    public void setThresholdOfRightTrigger (int thresholdOfRightTrigger) {
      this.thresholdOfRightTrigger = thresholdOfRightTrigger;
    }

  }



}  //class XInput
