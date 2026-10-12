//========================================================================================
//  CONDevice.java
//    en:CON device control -- Paste a string from the platform clipboard or named pipe into the Human68k console.
//    ja:CONデバイス制御 -- プラットフォームのクリップボードまたは名前付きパイプにある文字列をHuman68kのコンソールに貼り付けます。
//  Copyright (C) 2003-2025 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;

public class CONDevice {

  //貼り付けタスクの開始遅延(ms)
  public static final long CON_PASTE_DELAY = 10L;

  //貼り付けタスクの動作間隔(ms)
  //  長すぎると大量に貼り付けたとき時間がかかる
  public static final long CON_PASTE_INTERVAL = 10L;

  //貼り付けパイプの名前
  public static final String CON_PASTE_PIPE_NAME = "XEiJPaste";
  //制御パイプの名前
  public static final String CON_CONTROL_PIPE_NAME = "XEiJControl";

  //パイプの文字コード
  public static final int CON_PIPE_SJIS = 0;
  public static final int CON_PIPE_UTF8 = 1;
  public static int conPipeEncoding;

  //パイプインスタンスの数
  //  NamedPipeInputStreamは相手の切断を検出すると直ちに次の接続を待つが、その間に相手が接続をしようとしたとき失敗する可能性がある
  //  Windowsは同じ名前で複数のパイプを開ける。パイプの数を増やすことで失敗する確率を減らせる
  //  mkfifoは一度に1つの接続しか作れない。連続することがわかっているときは1つにまとめるかsleep 0.1などを挟んで間隔をあける必要がある
  //  パイプインスタンスの数は偶数個。偶数番を貼り付けパイプ、奇数番を制御パイプとする
  public static final int CON_PIPE_INSTANCES_WIN = 4;
  public static final int CON_PIPE_INSTANCES_GEN = 2;
  public static int conPipeInstances;

  //キー
  public static final String[] CON_KEY_BASE = (
    "esc,1," +
    "1,2," +
    "2,3," +
    "3,4," +
    "4,5," +
    "5,6," +
    "6,7," +
    "7,8," +
    "8,9," +
    "9,10," +
    "0,11," +
    "minus,12," +
    "caret,13," +
    "yen,14," +
    "bs,15," +
    "tab,16," +
    "q,17," +
    "w,18," +
    "e,19," +
    "r,20," +
    "t,21," +
    "y,22," +
    "u,23," +
    "i,24," +
    "o,25," +
    "p,26," +
    "at,27," +
    "leftbracket,28," +
    "return,29," +
    "a,30," +
    "s,31," +
    "d,32," +
    "f,33," +
    "g,34," +
    "h,35," +
    "j,36," +
    "k,37," +
    "l,38," +
    "semicolon,39," +
    "colon,40," +
    "rightbracket,41," +
    "z,42," +
    "x,43," +
    "c,44," +
    "v,45," +
    "b,46," +
    "n,47," +
    "m,48," +
    "comma,49," +
    "period,50," +
    "slash,51," +
    "underline,52," +
    "space,53," +
    "home,54," +
    "del,55," +
    "rollup,56," +
    "rolldown,57," +
    "undo,58," +
    "left,59," +
    "up,60," +
    "right,61," +
    "down,62," +
    "clr,63," +
    "tenkeyslash,64," +
    "tenkeyasterisk,65," +
    "tenkeyminus,66," +
    "tenkey7,67," +
    "tenkey8,68," +
    "tenkey9,69," +
    "tenkeyplus,70," +
    "tenkey4,71," +
    "tenkey5,72," +
    "tenkey6,73," +
    "tenkeyequal,74," +
    "tenkey1,75," +
    "tenkey2,76," +
    "tenkey3,77," +
    "enter,78," +
    "tenkey0,79," +
    "tenkeycomma,80," +
    "tenkeyperiod,81," +
    "kigou,82," +
    "touroku,83," +
    "help,84," +
    "xf1,85," +
    "xf2,86," +
    "xf3,87," +
    "xf4,88," +
    "xf5,89," +
    "kana,90," +
    "roma,91," +
    "code,92," +
    "caps,93," +
    "ins,94," +
    "hiragana,95," +
    "zenkaku,96," +
    "break,97," +
    "copy,98," +
    "f1,99," +
    "f2,100," +
    "f3,101," +
    "f4,102," +
    "f5,103," +
    "f6,104," +
    "f7,105," +
    "f8,106," +
    "f9,107," +
    "f10,108," +
    //
    "shift,112," +
    "ctrl,113," +
    "opt1,114," +
    "opt2,115," +
    "num,116," +
    "").split (",");

  //貼り付けパイプを使うか
  public static boolean conPipeOn;
  //貼り付け設定メニュー
  public static JMenu conSettingsMenu;
  //貼り付けパイプチェックボックス
  public static JCheckBoxMenuItem conPipeCheckBox;

  //後始末フラグ
  public static boolean conCleanupFlag;
  //CONデバイス
  public static int conCON;

  //貼り付けタスク
  public static CONPasteTask conPasteTask;
  //貼り付けキュー
  public static LinkedBlockingQueue<String> conPasteQueue;
  //貼り付けキュースレッド
  public static CONPasteQueueThread conPasteQueueThread;

  //キーマップ
  public static HashMap<String,Integer> conKeyMap;
  //制御キュー
  public static LinkedBlockingQueue<String> conControlQueue;
  //制御キュースレッド
  public static CONControlQueueThread conControlQueueThread;

  //パイプスレッドの配列
  public static CONPipeThread[] conPipeThreadArray;

  //conInit ()
  //  初期化
  //    パラメータを復元する
  //    メニューを作る
  //    後始末フラグをクリアする
  //    CONデバイスは探していない
  //    貼り付けタスクはない
  //    貼り付けキューを作る
  //    貼り付けキュースレッドを作る
  //    貼り付けキュースレッドを開始する
  //    制御コマンドマップを作る
  //    制御キューを作る
  //    制御キュースレッドを作る
  //    パイプスレッドの配列を作る
  //    すべてのパイプスレッドについて
  //      パイプスレッドを作る
  //      パイプストリームがあるとき
  //        パイプスレッドを開始する
  public static void conInit () {
    //パラメータを復元する
    conPipeOn = Settings.sgsGetOnOff ("pastepipe");
    {
      String s = Settings.sgsGetString ("pipeencoding").toLowerCase ();
      conPipeEncoding = (s.equals ("utf8") ? CON_PIPE_UTF8 :
                        CON_PIPE_SJIS);
    }
    conPipeInstances = XEiJ.prgIsWindows ? CON_PIPE_INSTANCES_WIN : CON_PIPE_INSTANCES_GEN;
    //メニューを作る
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        String command = ae.getActionCommand ();
        switch (command) {
        case "Stop paste":  //貼り付け中止
          conStopPaste ();
          break;
        case "Paste and control pipe":  //貼り付けおよび制御パイプ
          conSetPipeOn (((JCheckBoxMenuItem) source).isSelected ());
          break;
        case "SJIS":
          conPipeEncoding = CON_PIPE_SJIS;
          break;
        case "UTF-8":
          conPipeEncoding = CON_PIPE_UTF8;
          break;
        }
      }
    };
    ButtonGroup encodingGroup = new ButtonGroup ();
    conSettingsMenu = Multilingual.mlnText (
      ComponentFactory.createMenu (
        "Pipe settings",
        Multilingual.mlnText (
          ComponentFactory.createMenuItem ("Stop paste", listener),
          "ja", "貼り付け中止"),
        ComponentFactory.createHorizontalSeparator (),
        conPipeCheckBox = Multilingual.mlnText (
          ComponentFactory.createCheckBoxMenuItem (conPipeOn, "Paste and control pipe", listener),
          "ja", "貼り付けおよび制御パイプ"),
        ComponentFactory.createRadioButtonMenuItem (encodingGroup, conPipeEncoding == CON_PIPE_SJIS, "SJIS", listener),
        ComponentFactory.createRadioButtonMenuItem (encodingGroup, conPipeEncoding == CON_PIPE_UTF8, "UTF-8", listener)
        ),
      "ja", "パイプ設定");
    //
    //後始末フラグをクリアする
    conCleanupFlag = false;
    //CONデバイスは探していない
    conCON = 0;
    //
    //貼り付けタスクはない
    conPasteTask = null;
    //貼り付けキューを作る
    conPasteQueue = new LinkedBlockingQueue<String> ();
    //貼り付けキュースレッドを作る
    conPasteQueueThread = new CONPasteQueueThread ();
    //貼り付けキュースレッドを開始する
    conPasteQueueThread.start ();
    //
    //キーマップを作る
    conKeyMap = new HashMap<String,Integer> ();
    for (int i = 0; i + 1 < CON_KEY_BASE.length; i += 2) {
      conKeyMap.put (CON_KEY_BASE[i], Integer.parseInt (CON_KEY_BASE[i + 1], 10));
    }
    //制御キューを作る
    conControlQueue = new LinkedBlockingQueue<String> ();
    //制御キュースレッドを作る
    conControlQueueThread = new CONControlQueueThread ();
    //制御キュースレッドを開始する
    conControlQueueThread.start ();
    //
    //パイプスレッドの配列を作る
    conPipeThreadArray = new CONPipeThread[conPipeInstances];
    //すべてのパイプスレッドについて
    for (int i = 0; i < conPipeInstances; i++) {
      if (conPipeOn) {
        //パイプスレッドを作る
        conPipeThreadArray[i] = new CONPipeThread ((i & 1) != 0);  //偶数番を貼り付けパイプ(false)、奇数番を制御パイプ(true)とする
        //パイプストリームがあるとき
        if (conPipeThreadArray[i].getPipeStream () != null) {
          //パイプスレッドを開始する
          conPipeThreadArray[i].start ();
        }
      } else {
        //パイプスレッドはない
        conPipeThreadArray[i] = null;
      }
    }
  }  //conInit

  //conTini ()
  //  後始末
  //    パラメータを保存する
  //    後始末フラグをセットする
  //    貼り付けキュースレッドがあるとき
  //      貼り付けキュースレッドに割り込む
  //      貼り付けキュースレッドが終了するまで待つ
  //      貼り付けキュースレッドはない
  //    貼り付けタスクがあるとき
  //      貼り付けタスクをキャンセルする
  //      貼り付けタスクはない
  //    制御キュースレッドがあるとき
  //      制御キュースレッドに割り込む
  //      制御キュースレッドが終了するまで待つ
  //      制御キュースレッドはない
  //    すべてのパイプスレッドについて
  //      パイプスレッドがあるとき
  //        パイプスレッドを終了させる
  //        パイプスレッドが終了するまで待つ
  //        パイプスレッドはない
  public static void conTini () {
    //パラメータを保存する
    Settings.sgsPutOnOff ("pastepipe", conPipeOn);
    Settings.sgsPutString ("pipeencoding",
                           conPipeEncoding == CON_PIPE_UTF8 ? "utf8" :
                           "sjis");
    //後始末フラグをセットする
    conCleanupFlag = true;
    //貼り付けキュースレッドがあるとき
    if (conPasteQueueThread != null) {
      //貼り付けキュースレッドに割り込む
      conPasteQueueThread.interrupt ();
      //貼り付けキュースレッドが終了するまで待つ
      try {
        conPasteQueueThread.join (100);
      } catch (InterruptedException ie) {
      }
      //貼り付けキュースレッドはない
      conPasteQueueThread = null;
    }
    //貼り付けタスクがあるとき
    CONPasteTask t = conPasteTask;
    if (t != null) {
      //貼り付けタスクをキャンセルする
      t.cancel ();
      //貼り付けタスクはない
      conPasteTask = null;
    }
    //制御キュースレッドがあるとき
    if (conControlQueueThread != null) {
      //制御キュースレッドに割り込む
      conControlQueueThread.interrupt ();
      //制御キュースレッドが終了するまで待つ
      try {
        conControlQueueThread.join (100);
      } catch (InterruptedException ie) {
      }
      //制御キュースレッドはない
      conControlQueueThread = null;
    }
    //すべてのパイプスレッドについて
    for (int i = 0; i < conPipeInstances; i++) {
      //パイプスレッドがあるとき
      if (conPipeThreadArray[i] != null) {
        //パイプを閉じる
        conPipeThreadArray[i].closePipe ();
        //パイプスレッドが終了するまで待つ
        try {
          conPipeThreadArray[i].join (100);
        } catch (InterruptedException ie) {
        }
        //パイプスレッドはない
        conPipeThreadArray[i] = null;
      }
    }
  }  //conTini

  //conReset ()
  //  リセット
  //    CONデバイスは探していない
  public static void conReset () {
    //CONデバイスは探していない
    conCON = 0;
  }  //conReset

  //conDoPaste ()
  //  貼り付け
  //    クリップボードから文字列を取り出す
  //    貼り付けキューに文字列を追加する
  public static void conDoPaste () {
    //クリップボードから文字列を取り出す
    if (XEiJ.clpClipboard == null) {
      return;
    }
    String string = null;
    try {
      string = (String) XEiJ.clpClipboard.getData (DataFlavor.stringFlavor);
    } catch (Exception e) {
      return;
    }
    if (string == null || string.equals ("")) {
      return;
    }
    //貼り付けキューに文字列を追加する
    conPasteQueue.add (string);
  }  //conDoPaste

  //conStopPaste ()
  //  貼り付け中止
  //    貼り付けキューを空にする
  //    貼り付けタスクがあるとき
  //      貼り付けタスクに貼り付けを中止させる
  public static void conStopPaste () {
    //貼り付けキューを空にする
    conPasteQueue.clear ();
    //貼り付けタスクがあるとき
    CONPasteTask t = conPasteTask;
    if (t != null) {
      //貼り付けタスクに貼り付けを中止させる
      t.stopPaste ();
    }
  }  //conStopPaste

  //conSetPipeOn (on)
  //  貼り付けパイプを使うか設定する
  //    off→on
  //      すべてのパイプスレッドについて
  //        パイプスレッドがないとき
  //          パイプスレッドを作る
  //          パイプストリームがあるとき
  //            パイプスレッドを開始する
  //    on→off
  //      すべてのパイプスレッドについて
  //        パイプスレッドがあるとき
  //          パイプを閉じる
  //          パイプスレッドが終了するまで待つ
  //          パイプスレッドはない
  public static void conSetPipeOn (boolean on) {
    if (conPipeOn != on) {
      conPipeOn = on;
      conPipeCheckBox.setSelected (on);
      if (on) {  //off→on
        //すべてのパイプスレッドについて
        for (int i = 0; i < conPipeInstances; i++) {
          //パイプスレッドがないとき
          if (conPipeThreadArray[i] == null) {
            //パイプスレッドを作る
            conPipeThreadArray[i] = new CONPipeThread ((i & 1) != 0);  //偶数番を貼り付けパイプ(false)、奇数番を制御パイプ(true)とする
            //パイプストリームがあるとき
            if (conPipeThreadArray[i].getPipeStream () != null) {
              //パイプスレッドを開始する
              conPipeThreadArray[i].start ();
            }
          }
        }
      } else {  //on→off
        //すべてのパイプスレッドについて
        for (int i = 0; i < conPipeInstances; i++) {
          //パイプスレッドがあるとき
          if (conPipeThreadArray[i] != null) {
            //パイプを閉じる
            conPipeThreadArray[i].closePipe ();
            //パイプスレッドが終了するまで待つ
            try {
              conPipeThreadArray[i].join (100);
            } catch (InterruptedException ie) {
            }
            //パイプスレッドはない
            conPipeThreadArray[i] = null;
          }
        }
      }
    }
  }  //conSetPipeOn

  //class CONPasteQueueThread
  //  貼り付けキュースレッド
  //    以下を繰り返す
  //      後始末フラグがセットされているとき
  //        終了する
  //      貼り付けタスクがないとき
  //        貼り付けキューから文字列を取り出す
  //        (貼り付けキューに文字列が追加されるか割り込まれるまでブロックする)
  //        後始末フラグがセットされているとき
  //          終了する
  //        文字列が有効なとき
  //          貼り付けタスクを作る
  //          貼り付けタスクを開始する
  //      200msスリープする
  public static class CONPasteQueueThread extends Thread {
    @Override public void run () {
      //以下を繰り返す
      for (;;) {
        //後始末フラグがセットされているとき
        if (conCleanupFlag) {
          //終了する
          return;
        }
        //貼り付けタスクがないとき
        if (conPasteTask == null) {
          //貼り付けキューから文字列を取り出す
          //(貼り付けキューに文字列が追加されるか割り込まれるまでブロックする)
          String string = null;
          try {
            string = conPasteQueue.take ();
          } catch (InterruptedException ie) {
          }
          //後始末フラグがセットされているとき
          if (conCleanupFlag) {
            //終了する
            return;
          }
          //文字列が有効なとき
          if (string != null && !string.equals ("")) {
            //貼り付けタスクを作る
            conPasteTask = new CONPasteTask (string);
            //貼り付けタスクを開始する
            conPasteTask.start ();
          }
        }
        //200msスリープする
        try {
          Thread.sleep (200);
        } catch (InterruptedException ie) {
        }
      }  //for
    }  //run
  }  //CONPasteQueueThread

  //class CONControlQueueThread
  //  制御キュースレッド
  //    以下を繰り返す
  //      後始末フラグがセットされているとき
  //        終了する
  //      制御キューから文字列を取り出す
  //      (制御キューに文字列が追加されるか割り込まれるまでブロックする)
  //      後始末フラグがセットされているとき
  //        終了する
  //      文字列が有効なとき
  //        コマンドを切り取って実行する
  //      200msスリープする
  public static class CONControlQueueThread extends Thread {
    @Override public void run () {
      StringBuilder controlBuilder = new StringBuilder ();
      //以下を繰り返す
      for (;;) {
        //後始末フラグがセットされているとき
        if (conCleanupFlag) {
          //終了する
          return;
        }
        //制御キューから文字列を取り出す
        //(制御キューに文字列が追加されるか割り込まれるまでブロックする)
        String string = null;
        try {
          string = conControlQueue.take ();
        } catch (InterruptedException ie) {
        }
        //後始末フラグがセットされているとき
        if (conCleanupFlag) {
          //終了する
          return;
        }
        //文字列が有効なとき
        if (string != null && !string.equals ("")) {
          //コマンドを切り取って実行する
          controlBuilder.append (string);
          for (;;) {
            int l = controlBuilder.length ();
            //改行またはコロンを読み飛ばす
            int i = 0;
            for (; i < l; i++) {
              int c = controlBuilder.charAt (i);
              if (!(c == '\n' || c == '\r' || c == ':')) {
                break;
              }
            }
            if (l <= i) {
              break;
            }
            //改行またはコロンを探す
            int j = i;
            for (; j < l; j++) {
              int c = controlBuilder.charAt (j);
              if (c == '\n' || c == '\r' || c == ':') {
                break;
              }
            }
            if (l <= j) {
              break;
            }
            //コマンドを切り取る
            String s = controlBuilder.substring (i, j);
            controlBuilder.delete (0, j);
            //コマンドを実行する
            conCommand (s);
          }
        }
        //200msスリープする
        try {
          Thread.sleep (200);
        } catch (InterruptedException ie) {
        }
      }  //for
    }  //run
  }  //CONControlQueueThread

  //conCommand (command)
  //  コマンドを実行する
  //  interrupt
  //  presskey opt1
  //  releasekey opt1
  //  reset
  //  typekey ctrl c
  public static void conCommand (String command) {
    //trimして小文字にする
    command = command.trim ().toLowerCase (Locale.ROOT);
    //空のとき何もしない
    if (command.length () == 0) {
      return;
    }
    //空白の並びで分割する
    String[] args = command.split ("[\\x00-\\x20]+");
    //コマンドで分岐する
    switch (args[0]) {
    case "interrupt":  //インタラプト
      XEiJ.sysInterrupt ();
      return;
    case "presskey":  //キーを押す
      conPressKey (args);
      return;
    case "releasekey":  //キーを離す
      conReleaseKey (args);
      return;
    case "typekey":  //キーを押して離す
      conPressKey (args);
      conReleaseKey (args);
      return;
    case "opt1reset":  //OPT.1キーを押しながらリセット
      XEiJ.mpuReset (0, -1);
      return;
    case "reset":  //リセット
      XEiJ.mpuReset (-1, -1);
      return;
    default:
      System.out.println ("unknown command " + args[0]);
      return;
    }
  }  //conCommand

  //conPressKey (args)
  //  キーを押す
  public static void conPressKey (String[] args) {
    for (int k = 1; k < args.length; k++) {  //昇順に押す
      String key = args[k];
      if (conKeyMap.containsKey (key)) {
        Keyboard.kbdCommandPress (conKeyMap.get (key).intValue ());
      } else {
        System.out.println ("unknown key " + key);
        return;
      }
    }
  }  //conPressKey

  //conReleaseKey (args)
  //  キーを離す
  public static void conReleaseKey (String[] args) {
    for (int k = args.length - 1; 1 <= k; k--) {  //降順に離す
      String key = args[k];
      if (conKeyMap.containsKey (key)) {
        Keyboard.kbdCommandRelease (conKeyMap.get (key).intValue ());
      } else {
        System.out.println ("unknown key " + key);
        return;
      }
    }
  }  //conReleaseKey

  //class CONPipeThread
  //  パイプスレッド
  public static class CONPipeThread extends Thread {

    //制御パイプか
    public boolean isControlPipe;
    //パイプストリーム
    public NamedPipeInputStream pipeStream;
    //閉じるフラグ
    public boolean closeFlag;

    //new CONPipeThread
    //  コンストラクタ
    //    制御パイプか
    //    パイプストリームはない
    //    閉じるフラグをクリアする
    //    パイプストリームを開く
    public CONPipeThread (boolean isControlPipe) {
      //制御パイプか
      this.isControlPipe = isControlPipe;
      //パイプストリームはない
      pipeStream = null;
      //閉じるフラグをクリアする
      closeFlag = false;
      //パイプストリームを開く
      try {
        pipeStream = NamedPipeInputStream.create (isControlPipe ? CON_CONTROL_PIPE_NAME : CON_PASTE_PIPE_NAME);
      } catch (IOException ioe) {
        ioe.printStackTrace ();
      }
    }  //CONPipeThread

    //pipeStream = getPipeStream ()
    //  パイプストリームを返す
    //    パイプストリームを返す
    //    (コンストラクタの直後にnullを返したらパイプ使用不可)
    public NamedPipeInputStream getPipeStream () {
      //パイプストリームを返す
      return pipeStream;
    }  //getInputStream

    //closePipe ()
    //  パイプを閉じる
    //    閉じるフラグをセットする
    //    パイプを閉じる
    public void closePipe () {
      //閉じるフラグをセットする
      closeFlag = true;
      //パイプを閉じる。readをキャンセルする
      if (pipeStream != null) {
        pipeStream.close ();
      }
    }  //closePipe

    //run ()
    //  パイプタスク
    //    後始末フラグがセットされておらず閉じるフラグがセットされておらずパイプストリームがあるとき
    //      パイプストリームから入力する。1バイト以上入力するまでブロックする
    //      キャンセルされたとき終了する
    //      文字コードをデコードする
    //      制御パイプのとき制御キューにさもなくば貼り付けキューに追加する
    @Override public void run () {
      byte[] b = new byte[1024];
      int pool = 0;
      try {
        while (!conCleanupFlag &&  //後始末フラグがセットされておらず
               !closeFlag &&  //閉じるフラグがセットされておらず
               pipeStream != null) {  //パイプストリームがあるとき以下を繰り返す
          //パイプストリームから入力する。1バイト以上入力するまでブロックする
          int l = pipeStream.read (b);
          //キャンセルされたとき終了する
          if (l == -1) {
            break;
          }
          //文字コードをデコードする
          StringBuilder sb = new StringBuilder ();
          if (conPipeEncoding == CON_PIPE_UTF8) {  //UTF-8
            for (int i = 0; i < l; i++) {
              int u = (pool << 8) | (0xff & b[i]);
              if ((u & 0xffffffe0) == 0x000000c0 ||  //2バイトコードの1バイト目
                  (u & 0xfffffff0) == 0x000000e0 ||  //3バイトコードの1バイト目
                  (u & 0xfffff0c0) == 0x0000e080 ||  //3バイトコードの2バイト目
                  (u & 0xfffffff8) == 0x000000f0 ||  //4バイトコードの1バイト目
                  (u & 0xfffff8c0) == 0x0000f080 ||  //4バイトコードの2バイト目
                  (u & 0xfff8c0c0) == 0x00f08080) {  //4バイトコードの3バイト目
                pool = u;
                continue;
              }
              pool = 0;
              if ((u & 0xffffff80) == 0x00000000) {  //1バイトコード 7bit
              } else if ((u & 0xffffe0c0) == 0x0000c080) {  //2バイトコード
                u = ((u & 0x00001f00) >> 2) | (u & 0x0000003f);  //5+6=11bit
              } else if ((u & 0xfff0c0c0) == 0x00e08080) {  //3バイトコード
                u = ((u & 0x000f0000) >> 4) | ((u & 0x00003f00) >> 2) | (u & 0x0000003f);  //4+6+6=16bit
              } else if ((u & 0xf8c0c0c0) == 0xf0808080) {  //4バイトコード
                u = ((u & 0x07000000) >> 6) | ((u & 0x003f0000) >> 4) | ((u & 0x00003f00) >> 2) | (u & 0x0000003f);  //3+6+6+6=21bit
                if (0x10ffff < u) {  //不正なコード
                  u = '※';
                } else if (0x00ffff < u) {  //サロゲートペア
                  //u -= 0x10000;
                  //int v = u & 0x3ff;
                  //u >>= 10;
                  //u += 0xd800;
                  //v += 0xdc00;
                  u = '※';
                }
              } else {  //不正なコード
                u = '※';
              }
              sb.append ((char) u);
            }  //for i
          } else {  //SJIS
            for (int i = 0; i < l; i++) {
              int s = (pool << 8) | (0xff & b[i]);
              if ((0x0080 <= s && s <= 0x009f) || (0x00e0 <= s && s <= 0x00ff)) {  //2バイトコードの1バイト目
                pool = s;
                continue;
              }
              pool = 0;
              int u = CharacterCode.chrSJISToChar[s];  //SJIS→UTF-16(サロゲートペアなし)変換
              if (u == 0 && s != 0) {  //0でないコードが0になった
                u = '\ufffd';  //対応なし
              }
              sb.append ((char) u);
            }  //for i
          }
          //制御パイプのとき制御キューにさもなくば貼り付けキューに追加する
          (isControlPipe ? conControlQueue : conPasteQueue).add (sb.toString ());
        }  //while
      } catch (IOException ioe) {
        ioe.printStackTrace ();
      }
    }  //run

  }  //class PipeThread

  //class CONPasteTask
  public static class CONPasteTask extends TimerTask {

    //貼り付ける文字列
    public String string;
    //貼り付ける文字列の長さ
    public int length;
    //貼り付ける文字列の次に貼り付ける文字の位置
    public int index;
    //貼り付け中止フラグ
    public boolean stopFlag;

    //new CONPasteTask ()
    //  コンストラクタ
    //    貼り付け中止フラグをクリアする
    public CONPasteTask (String string) {
      this.string = string;
      length = string.length ();
      index = 0;
      //貼り付け中止フラグをクリアする
      stopFlag = false;
    }  //PasteTask

    //start ()
    //  開始
    //    コアスレッドで貼り付けタスクを固定遅延実行で開始する
    public void start () {
      //コアスレッドで貼り付けタスクを固定遅延実行で開始する
      XEiJ.tmrTimer.schedule (conPasteTask, CON_PASTE_DELAY, CON_PASTE_INTERVAL);
    }  //start

    //stopPaste ()
    //  貼り付け中止
    //    貼り付け中止フラグをセットする
    public void stopPaste () {
      //貼り付け中止フラグをセットする
      stopFlag = true;
    }  //stopPaste

    //run ()
    //  貼り付けタスク(固定遅延実行)
    //    CONデバイスを探していないとき
    //      CONデバイスを探す
    //    CONデバイスが見つからないか貼り付け中止フラグがセットされているとき
    //      貼り付ける文字列を空にする
    //      貼り付けキューを空にする
    //      貼り付けタスクをキャンセルする
    //      貼り付けタスクはない
    //      終了する
    //    コンソール入力バッファが空でないとき
    //      終了する
    //    貼り付ける文字列の先頭を切り取ってコンソール入力バッファへ書き込む
    //    貼り付ける文字列が空のとき
    //      貼り付けタスクをキャンセルする
    //      貼り付けタスクはない
    //      終了する
    //    (ASK68K 3.02のコンソール入力バッファは200バイトしかないので、貼り付ける文字列がなくなるまで繰り返し呼び出される)
    //
    //!!! 入力と貼り付けが競合するとデータが混ざったり欠落したりする可能性がある
    //  コアスレッドを用いるので命令の実行中にコンソール入力バッファを書き換えることはないが、
    //  割り込みルーチンでコンソール入力バッファを書き換えた場合と同様の壊れ方をする可能性はある
    @Override public void run () {
      //CONデバイスを探していないとき
      int con = conCON;
      if (con == 0) {
        //CONデバイスを探す
        con = MainMemory.mmrHumanDev ('C' << 24 | 'O' << 16 | 'N' << 8 | ' ',
                                      ' ' << 24 | ' ' << 16 | ' ' << 8 | ' ');
        if (0 <= con &&  //CONデバイスが見つかった
            (//MC68060.mmuPeekLongData (con + 0x000168, 1) == 0x93fa967b &&  //"日本"
             //MC68060.mmuPeekLongData (con + 0x00016c, 1) == 0x8cea8374 &&  //"語フ"
             //MC68060.mmuPeekLongData (con + 0x000170, 1) == 0x838d8393 &&  //"ロン"
             //MC68060.mmuPeekLongData (con + 0x000174, 1) == 0x83678376 &&  //"トプ"
             //MC68060.mmuPeekLongData (con + 0x000178, 1) == 0x838d835a &&  //"ロセ"
             //MC68060.mmuPeekLongData (con + 0x00017c, 1) == 0x83628354 &&  //"ッサ"
             MC68060.mmuPeekLongData (con + 0x000180, 1) == 0x20826082 &&  //" ＡＳ"
             MC68060.mmuPeekLongData (con + 0x000184, 1) == 0x72826a82 &&  //"ＳＫ６"
             MC68060.mmuPeekLongData (con + 0x000188, 1) == 0x55825782 &&  //"６８Ｋ"
             MC68060.mmuPeekLongData (con + 0x00018c, 1) == 0x6a20666f &&  //"Ｋ fo"
             MC68060.mmuPeekLongData (con + 0x000190, 1) == 0x72205836 &&  //"r X6"
             MC68060.mmuPeekLongData (con + 0x000194, 1) == 0x38303030 &&  //"8000"
             MC68060.mmuPeekLongData (con + 0x000198, 1) == 0x20766572 &&  //" ver"
             MC68060.mmuPeekLongData (con + 0x00019c, 1) == 0x73696f6e &&  //"sion"
             MC68060.mmuPeekLongData (con + 0x0001a0, 1) == 0x20332e30 &&  //" 3.0"
             MC68060.mmuPeekLongData (con + 0x0001a4, 1) == 0x320d0a43// &&  //"2\r\nC"
             //MC68060.mmuPeekLongData (con + 0x0001a8, 1) == 0x6f707972 &&  //"opyr"
             //MC68060.mmuPeekLongData (con + 0x0001ac, 1) == 0x69676874 &&  //"ight"
             //MC68060.mmuPeekLongData (con + 0x0001b0, 1) == 0x20313938 &&  //" 198"
             //MC68060.mmuPeekLongData (con + 0x0001b4, 1) == 0x372d3934 &&  //"7-94"
             //MC68060.mmuPeekLongData (con + 0x0001b8, 1) == 0x20534841 &&  //" SHA"
             //MC68060.mmuPeekLongData (con + 0x0001bc, 1) == 0x52502043 &&  //"RP C"
             //MC68060.mmuPeekLongData (con + 0x0001c0, 1) == 0x6f72702e &&  //"orp."
             //MC68060.mmuPeekLongData (con + 0x0001c4, 1) == 0x2f414343 &&  //"/ACC"
             //MC68060.mmuPeekLongData (con + 0x0001c8, 1) == 0x45535320 &&  //"ESS "
             //MC68060.mmuPeekLongData (con + 0x0001cc, 1) == 0x434f2e2c &&  //"CO.,"
             //MC68060.mmuPeekLongData (con + 0x0001d0, 1) == 0x4c54442e &&  //"LTD."
             //MC68060.mmuPeekLongData (con + 0x0001d4, 1) == 0x0d0a0000  //"\r\n\0\0"
             )) {  //ASK68K 3.02
          conCON = con;
        }
      }
      //CONデバイスが見つからないか貼り付け中止フラグがセットされているとき
      if (con == 0 || stopFlag) {
        //貼り付けキューを空にする
        conPasteQueue.clear ();
        //貼り付けタスクをキャンセルする
        cancel ();
        //貼り付けタスクはない
        conPasteTask = null;
        //終了する
        return;
      }
      //コンソール入力バッファが空でないとき
      int read = MC68060.mmuPeekLongData (con + 0x00e460, 1);  //コンソール入力バッファから最後に読み出した位置、または、これから読み出そうとしている位置
      int write = MC68060.mmuPeekLongData (con + 0x00e464, 1);  //コンソール入力バッファへ最後に書き込んだ位置。入力中はこの後に書き込み始めている場合がある
      if (write != read) {
        //終了する
        return;
      }
      //貼り付ける文字列の先頭を切り取ってコンソール入力バッファへ書き込む
      int head = con + 0x010504;  //コンソール入力バッファの先頭
      int tail = head + 200;  //コンソール入力バッファの末尾
      for (; index < length; index++) {
        int c = CharacterCode.chrCharToSJIS[string.charAt (index)];  //UTF16→SJIS変換
        if (c == 0) {  //変換できない
          continue;  //無視する
        }
        if (c == '\r' && index + 1 < length && string.charAt (index + 1) == '\n') {  //CRLF
          index++;  //CRにする
        } else if (c == '\n') {  //LF
          c = '\r';  //CRにする
        }
        if (!(c >= ' ' || c == '\t' || c == '\r' || c == 0x1b)) {  //タブと改行とエスケープ以外の制御コード
          continue;  //無視する
        }
        int write1 = write + 1 == tail ? head : write + 1;  //1バイト目を書き込む位置
        int write2 = write1 + 1 == tail ? head : write1 + 1;  //2バイト目を書き込む位置
        int write3 = write2 + 1 == tail ? head : write2 + 1;  //3バイト目を書き込む位置。予備
        if (write1 == read || write2 == read || write3 == read || write3 == read) {  //コンソール入力バッファフル。readの位置はまだ読み出されていない場合がある
          break;  //書き込みを延期する
        }
        if (c < 0x0100) {  //1バイトのとき
          MC68060.mmuPokeByteData (write1, c, 1);
          write = write1;
        } else {  //2バイトのとき
          MC68060.mmuPokeByteData (write1, c >> 8, 1);
          MC68060.mmuPokeByteData (write2, c, 1);
          write = write2;
        }
      }
      MC68060.mmuPokeLongData (con + 0x00e464, write, 1);  //コンソール入力バッファへ最後に書き込んだ位置
      //貼り付ける文字列が空のとき
      if (index == length) {
        //貼り付けタスクをキャンセルする
        cancel ();
        //貼り付けタスクはない
        conPasteTask = null;
        //終了する
        return;
      }
      //(ASK68K 3.02のコンソール入力バッファは200バイトしかないので、貼り付ける文字列がなくなるまで繰り返し呼び出される)
    }  //run

  }  //class PasteTask

}  //class CONDevice
