//========================================================================================
//  RS232CTerminal.java
//    en:RS-232C settings and terminal
//    ja:RS-232C設定とターミナル
//  Copyright (C) 2003-2026 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.awt.datatransfer.*;  //DataFlavor
import java.awt.event.*;  //ActionListener
import java.io.*;  //UnsupportedEncodingException
import java.net.*;  //URLEncoder
import java.nio.*;  //ByteBuffer
import java.nio.channels.*;  //AsynchronousServerSocketChannel
import java.nio.charset.*;  //Charset
import java.util.*;  //HashSet
import java.util.concurrent.*;  //ScheduledExecutorService
import java.util.zip.*;  //CRC32
import javax.swing.*;
import javax.swing.event.*;  //DocumentListener

import com.fazecast.jSerialComm.*;  //SerialPort

public class RS232CTerminal {

  public static final int TRM_MAX_OUTPUT_LENGTH = 1024 * 256;  //出力の上限を256KBとする
  public static final int TRM_CUT_OUTPUT_LENGTH = TRM_MAX_OUTPUT_LENGTH + 1024 * 16;  //出力が上限よりも16KB以上長くなったら上限でカットする

  //コンポーネント
  public static JFrame trmFrame;  //ウインドウ
  public static ScrollTextArea trmBoard;  //テキストエリア
  public static JPopupMenu trmPopupMenu;  //ポップアップメニュー
  public static JMenuItem trmPopupCutMenuItem;  //切り取り
  public static JMenuItem trmPopupCopyMenuItem;  //コピー
  public static JMenuItem trmPopupPasteMenuItem;  //貼り付け
  public static JMenuItem trmPopupSelectAllMenuItem;  //すべて選択
  public static JMenuItem trmPopupSendCtrlCMenuItem;  //^C送信
  public static StringBuilder trmOutputBuilder;  //ターミナルを最初に開くまでに出力された文字列を貯めておくバッファ
  public static int trmOutputEnd;  //出力された文字列の末尾。リターンキーが押されたらこれ以降に書かれた文字列をまとめて入力する
  public static int trmOutputSJIS1;  //出力するときに繰り越したSJISの1バイト目

  //SerialPort
  public static SerialPort[] trmPortArray;  //[row-1]=SerialPort。SerialPortの配列
  public static int trmNumberOfPorts;  //SerialPortの数。SerialPortの行の上限
  //TCP/IP
  public static int trmTcpipRow;  //TCP/IPの行。-1=なし
  //PPPサーバー
  public static int trmPppRow;  //PPPサーバーの行。-1=なし
  //行
  //  0  Terminal
  //  1  SerialPort[0]
  //  :
  //  trmNumberOfPorts  SerialPort[trmNumberOfPorts-1]
  //  trmTcpipRow  TCP/IP
  //  trmPppRow  PPPサーバー
  public static int trmRows;  //行数。1+trmNumberOfPorts+(0<=trmTcpipRow?1:0)+(0<=trmPppRow?1:0)
  public static String[] trmRowName;  //行の名前
  public static int[] trmRowToCol;  //行に接続している列。なければ-1
  //AUX*
  public static int trmNumberOfAUXs;  //AUX*の数
  //列
  //  Terminal,AUX,AUX2,…
  public static int trmCols;  //列数。1+trmNumberOfAUXs
  public static String[] trmColName;  //列の名前
  public static int[] trmColToRow;  //列に接続している行。なければ-1



  //接続
  //  Terminalから送信するとき。TerminalでENTERキーが押されてそれまでに入力／貼り付けされた文字列を送信するとき
  //    KeyListenerでENTERキーが押されたとき
  //      それまでに入力／貼り付けされた文字列を取り出す(trmEnter)
  //        SJISに変換してキューに書き込む(trmSendString)
  //  Terminalが受信するとき。入力／貼り付け以外でTerminalに文字列を表示するとき
  //    スレッドでキューをポーリングする
  //      取り出してSJIS逆変換してテキストエリアへ書き込む
  //      テキストエリアが大きくなりすぎたときは先頭を削って短くする
  //  AUXから送信するとき。SCCのデータレジスタへライトされたとき
  //    キューに書き込むだけ
  //    OUT232CはTx Buffer Emptyが1になるのを待ってからデータポートへライトするが、
  //    Tx Buffer Emptyが1になるのを待たずにライトしても送信される
  //    Tx Buffer Emptyの動作についてはtrmAUXSendTickerを参照
  //  AUXが受信するとき。SCCのデータレジスタからリードされたとき
  //    レジスタのデータを返すだけ
  //    ポーリング間隔またはボーレートなどから決めた受信間隔でティッカーを繰り返す
  //    (受信間隔は短くてもよいがデバッガの誤動作をどうにかしなければならない)
  //      キューが空でなくReset Highest IUSコマンド(WR0=$38)によりIUS(Interrupt Under Service)がリセットされているとき
  //        キューからデータを取り出してレジスタを更新して割り込みを要求する
  //  SerialPortから送信するとき。jSerialCommが受信するとき
  //    SerialPortDataListenerでSerialPortからreadBytesしてキューへ書き込む
  //    キューが満杯になることは事実上ないのでreadBytesが長く滞ることはないはず
  //  SerialPortが受信するとき。jSerialCommが送信するとき
  //    スレッドでキューをポーリングする
  //      データがあれば取り出してwriteBytesでjSerialCommに渡す
  //      writeBytesがブロックするとポーリングが止まる
  static class Connection implements SerialPortDataListener {
    int row;  //行。Terminal,SerialPort,SerialPort,…
    int col;  //列。Terminal,AUX,AUX2,…。row==0&&col==0は不可
    int index;  //trmConnectionArrayとtrmConnectionBoxでのインデックス。trmCols*row+col-1
    String text;  //checkBoxのtext兼actionCommand
    boolean connected;  //接続している
    JCheckBox checkBox;  //接続チェックボックス
    Box box;  //checkBoxを入れる箱

    //キュー
    ByteQueue row2colQueue;  //Terminal/SerialPort→Terminal/AUX
    ByteQueue col2rowQueue;  //Terminal/AUX→Terminal/SerialPort
    boolean row2colReset;  //true=row2colQueueをクリアする
    boolean col2rowReset;  //true=col2rowQueueをクリアする

    //CTS/DCD/DSR/RI
    boolean cts;
    boolean dcd;
    boolean dsr;
    boolean ri;

    //ポーリングタスク
    SimpleTask row2colTask;  //Terminal/SerialPort→Terminal/AUX
    SimpleTask col2rowTask;  //Terminal/AUX→Terminal/SerialPort

    //コンストラクタ
    Connection (int row, int col) {
      this.row = row;
      this.col = col;
      index = trmCols * row + col - 1;
      text = trmRowName[row] + " ⇔ " + trmColName[col];
      connected = false;
      checkBox = ComponentFactory.createCheckBox (connected, text, trmConnectionListener);
      box = ComponentFactory.createHorizontalBox (
        checkBox,
        Box.createHorizontalGlue ()
        );
    }  //Connection

    //connectable = isConnectable ()
    //  接続できるか
    boolean isConnectable () {
      if (!connected) {  //自分が接続していないとき
        for (Connection connection2 : trmConnectionArray) {
          if (this != connection2 &&  //自分以外で
              connection2.connected &&  //接続していて
              (((row == 0 || col == 0) &&
                (connection2.row == 0 || connection2.col == 0)) ||  //Terminalが衝突しているまたは
               row == connection2.row ||  //SerialPortが衝突しているまたは
               col == connection2.col)) {  //AUX*が衝突しているとき
            return false;  //接続できない
          }
        }
      }
      return true;  //接続できる
    }  //isConnectable

    //connect ()
    //  接続する
    void connect () {
      //接続しているか接続できないときは何もしない
      if (connected ||  //接続している
          !isConnectable ()) {  //接続できない
        return;
      }
      //接続する
      connected = true;
      checkBox.setSelected (true);
      trmRowToCol[row] = col;
      trmColToRow[col] = row;
      trmUpdateComponents ();
      //キューを作る
      row2colQueue = new ByteQueue ();
      col2rowQueue = new ByteQueue ();
      //CTS/DCD/DSR/RI
      cts = true;  //キューが溢れることは想定しない
      dcd = true;
      dsr = false;
      ri = false;
      if (1 <= row && row <= trmNumberOfPorts) {  //SerialPort→?
        //シリアルポートを開く
        SerialPort port = trmPortArray[row - 1];
        port.openPort ();
        System.out.println (Multilingual.mlnJapanese ?
                            text + " を開きました" :
                            text + " opened");
        port.setComPortTimeouts (SerialPort.TIMEOUT_WRITE_BLOCKING | SerialPort.TIMEOUT_READ_BLOCKING, 0, 0);
        port.setFlowControl (SerialPort.FLOW_CONTROL_DISABLED);
        //通信設定をSerialPortに反映させる
        trmReflectSettings (col);
        //DTR
        if (trmAUXDTR) {
          port.setDTR ();  //openPortがセットしているはずだが念の為
        } else {
          port.clearDTR ();
        }
        //CTS/DCD/DSR/RI
        //cts = true;
        dcd = port.getDCD ();
        dsr = port.getDSR ();
        ri = port.getRI ();
        //シリアルポートデータリスナーを設定する
        port.addDataListener (this);
      } else if (row == trmTcpipRow) {  //TCP/IP→?
        //TCP/IPポートを開く
        if (!trmTcpipOpenPort (row2colQueue)) {
          //開けなかった
          //!!!
        }
        //CTS/DCD/DSR/RI
        //cts = true;
        //dcd = true;
        //dsr = false;
        //ri = false;
      } else if (row == trmPppRow) {  //PPPサーバー→?
        //PPPサーバーを開く
        if (!trmPppOpenPort (row2colQueue)) {
          //開けなかった
          //!!!
        }
        //CTS/DCD/DSR/RI
        //cts = true;
        //dcd = true;
        //dsr = false;
        //ri = false;
      }
      //ポーリングタスクを開始する
      if (col == 0) {  //?→Terminal
        row2colTask = new SimpleTask (XEiJ.tmrScheduler, (n) -> {
          ByteQueue queue = row2colQueue;
          if (row2colReset) {
            row2colReset = false;
            queue.clear ();
          } else {
            int k = queue.used ();
            if (k != 0) {
              byte[] b = new byte[k];
              queue.read (b, 0, k);  //キューから読み出して
              for (int i = 0; i < k; i++) {
                trmPrintSJIS (b[i] & 0xff);  //ターミナルへ書き込む
              }
            }
          }
          return 1;
        }).fixedDelay (0L, 100L);
      } else {  //?→AUX
        //AUXはスレッドではなくティッカーを使う
        row2colTask = null;
      }
      if (row == 0) {  //?→Terminal
        col2rowTask = new SimpleTask (XEiJ.tmrScheduler, (n) -> {
          ByteQueue queue = col2rowQueue;
          if (col2rowReset) {
            col2rowReset = false;
            queue.clear ();
          } else {
            int k = queue.used ();
            if (k != 0) {
              byte[] b = new byte[k];
              queue.read (b, 0, k);  //キューから読み出して
              for (int i = 0; i < k; i++) {
                trmPrintSJIS (b[i] & 0xff);  //ターミナルへ書き込む
              }
            }
          }
          return 1;
        }).fixedDelay (0L, 100L);
      } else if (row == trmTcpipRow) {  //?→TCP/IP
        col2rowTask = new SimpleTask (XEiJ.tmrScheduler, (n) -> {
          ByteQueue queue = col2rowQueue;
          if (col2rowReset) {
            col2rowReset = false;
            queue.clear ();
          } else {
            int k = queue.used ();
            if (k != 0) {
              byte[] b = new byte[k];
              queue.read (b, 0, k);  //キューから読み出して
              trmTcpipWrite (b, 0, k);  //TCP/IPポートへ書き込む。ブロックすることがある
            }
          }
          return 1;
        }).fixedDelay (0L, 10L);
      } else if (row == trmPppRow) {  //?→PPPサーバー
        col2rowTask = new SimpleTask (XEiJ.tmrScheduler, (n) -> {
          ByteQueue queue = col2rowQueue;
          try {
            if (col2rowReset) {
              col2rowReset = false;
              queue.clear ();
            } else {
              int k = queue.used ();
              if (k != 0) {
                byte[] b = new byte[k];
                queue.read (b, 0, k);  //キューから読み出す
                int i = 0;
                while (trmPppModemLayer && i < k) {  //モデム層
                  int c = b[i++] & 0xff;
                  if (c != '\r') {
                    trmPppModemCommand.append ((char) c);
                  } else {
                    String s = trmPppModemCommand.toString ();
                    trmPppModemCommand.delete (0, trmPppModemCommand.length ());
                    byte[] bb;
                    if (false) {
                      bb = "NO CARRIER\r\n".getBytes ();
                    } else if (s.startsWith ("ATDT")) {
                      bb = "CONNECT\r\n".getBytes ();
                      trmPppModemLayer = false;  //モデム層終了
                      if (trmPppDebug) {
                        System.out.println ("modem layer ended");
                      }
                    } else if (s.startsWith ("AT")) {
                      bb = "OK\r\n".getBytes ();
                    } else {
                      bb = "ERROR\r\n".getBytes ();
                    }
                    row2colQueue.write (bb, 0, bb.length);
                  }
                }  //while
                if (i < k) {  //残りを
                  if (trmPppDebug) {
                    System.out.print ("pppd stdin");
                    for (int j = i; j < k; j++) {
                      System.out.printf (" %02X", b[j] & 0xff);
                    }
                    System.out.println ();
                  }
                  trmPppStdinStream.write (b, i, k - i);  //pppdの標準入力に書き込む。ブロックする
                  trmPppStdinStream.flush ();
                }
              }
            }
          } catch (IOException ioe) {
          }
          return 1;
        }).fixedDelay (0L, 10L);
      } else {  //?→SerialPort
        col2rowTask = new SimpleTask (XEiJ.tmrScheduler, (n) -> {
          SerialPort port = trmPortArray[row - 1];
          ByteQueue queue = col2rowQueue;
          if (col2rowReset) {
            col2rowReset = false;
            queue.clear ();
            port.flushIOBuffers ();
          } else {
            int k = queue.used ();
            if (k != 0) {
              byte[] b = new byte[k];
              queue.read (b, 0, k);  //キューから読み出して
              port.writeBytes (b, k);  //シリアルポートへ書き込む。ブロックすることがある
            }
          }
          return 1;
        }).fixedDelay (0L, 10L);
      }
      //ポーリングティッカーを開始する
      if (col == 1) {  //?→AUX
        trmAUXNotReceiving = false;  //受信可
        trmAUXDataBuffer = 0;
        trmAUXDataAvailable = false;
        trmAUXConnection = this;
        TickerQueue.tkqAdd (trmAUXTicker, XEiJ.mpuClockTime + XEiJ.TMR_FREQ / 1000 * 1);  //1ms
        //AUX送信割り込み
        trmAUXSendEmpty = true;  //送信バッファ空フラグをセット
      }
    }  //connect

    //disconnect ()
    //  切断する
    void disconnect () {
      //接続していないときは何もしない
      if (!connected) {
        return;
      }
      //切断する
      connected = false;
      checkBox.setSelected (connected);
      trmRowToCol[row] = -1;
      trmColToRow[col] = -1;
      trmUpdateComponents ();
      //ポーリングティッカーを終了する
      if (trmAUXConnection != null) {
        TickerQueue.tkqRemove (trmAUXTicker);
        trmAUXConnection = null;
      }
      //ポーリングタスクを停止する
      if (row2colTask != null) {
        row2colTask.stop ();
        row2colTask = null;
      }
      if (col2rowTask != null) {
        col2rowTask.stop ();
        col2rowTask = null;
      }
      //AUX送信割り込み
      if (col == 1) {  //?→AUX
        TickerQueue.tkqRemove (trmAUXSendTicker);  //送信ティッカーを消去
      }
      //CTS/DCD/DSR/RI
      cts = false;
      dcd = false;
      dsr = false;
      ri = false;
      if (1 <= row && row <= trmNumberOfPorts) {  //SerialPort→?
        SerialPort port = trmPortArray[row - 1];
        //シリアルポートデータリスナーを削除する
        port.removeDataListener ();
        //シリアルポートを閉じる
        port.closePort ();
        System.out.println (Multilingual.mlnJapanese ?
                            text + " を閉じました" :
                            text + " closed");
      } else if (row == trmTcpipRow) {
        //TCP/IPポートを閉じる
        trmTcpipClosePort ();
      } else if (row == trmPppRow) {
        //PPPサーバーを閉じる
        trmPppClosePort ();
      }
      //キューを消す
      row2colQueue.clear ();
      col2rowQueue.clear ();
      row2colQueue = null;
      col2rowQueue = null;
    }  //disconnect

    //シリアルポートデータリスナー。row2col。SerialPort→?
    @Override public int getListeningEvents () {
      return (SerialPort.LISTENING_EVENT_DATA_AVAILABLE |
              //SerialPort.LISTENING_EVENT_CTS |
              SerialPort.LISTENING_EVENT_CARRIER_DETECT |
              SerialPort.LISTENING_EVENT_DSR |
              SerialPort.LISTENING_EVENT_RING_INDICATOR);
    }  //getListeningEvents
    @Override public void serialEvent (SerialPortEvent spe) {
      SerialPort port = spe.getSerialPort ();
      int type = spe.getEventType ();
      if ((type & SerialPort.LISTENING_EVENT_DATA_AVAILABLE) != 0) {
        for (;;) {
          int k = Math.min (port.bytesAvailable (),  //ポートから読み出せるバイト数と
                            row2colQueue.unused ());  //キューへ書き込めるバイト数の小さい方
          if (k == 0) {
            break;
          }
          byte[] b = new byte[k];
          port.readBytes (b, k);  //ポートから読み出す
          row2colQueue.write (b, 0, k);  //キューへ書き込む
        }
      }
      //キューが溢れることは想定しない
      //if ((type & SerialPort.LISTENING_EVENT_CTS) != 0) {
      //  cts = port.getCTS ();
      //}
      if ((type & SerialPort.LISTENING_EVENT_CARRIER_DETECT) != 0) {
        dcd = port.getDCD ();
      }
      if ((type & SerialPort.LISTENING_EVENT_DSR) != 0) {
        dsr = port.getDSR ();
      }
      if ((type & SerialPort.LISTENING_EVENT_RING_INDICATOR) != 0) {
        ri = port.getRI ();
      }
    }  //serialEvent

  }  //class Connection



  //TCP/IPポート
  //  参考
  //    https://x.com/yunkya2/status/1940779786017951890
  //    https://github.com/yunkya2/xeij/commit/cba358a58197a05b25b64a4ceaa90d110a564aec
  public static final String TRM_TCPIP_DEFAULT_HOST = "0.0.0.0";  //デフォルトのホスト
  public static final int TRM_TCPIP_DEFAULT_PORT = 54321;  //デフォルトのポート番号
  public static final boolean TRM_TCPIP_DEFAULT_UTF8 = false;  //デフォルトのSJIS⇔UTF-8変換
  public static String trmTcpipHost;  //次に開くホスト
  public static int trmTcpipPort;  //次に開くポート番号
  public static boolean trmTcpipUTF8;  //SJIS⇔UTF-8変換
  public static DecimalSpinner trmTcpipSpinner;  //ポート番号スピナー
  public static JCheckBox trmTcpipCheckBox;  //UTF-8チェックボックス
  public static ByteBuffer trmTcpipReadBuffer;  //受信バッファ
  public static CompletionHandler<Integer, Void> trmTcpipReadHander;  //受信ハンドラ
  public static CompletionHandler<AsynchronousSocketChannel, Void> trmTcpipAcceptHandler;  //接続ハンドラ
  public static ByteQueue trmTcpipReadQueue;  //受信キュー
  public static SocketAddress trmTcpipPortAddress;  //ポートアドレス
  public static AsynchronousServerSocketChannel trmTcpipPortChannel;  //ポートチャネル
  public static final int TRM_TCPIP_BUFFER_SIZE = 4096;  //受信バッファのサイズ
  public static AsynchronousSocketChannel trmTcpipSessionChannel;  //セッションチャネル
  public static SocketAddress trmTcpipClientAddress;  //クライアントアドレス
  public static int trmTcpipToUTF8Pool;  //SJIS→UTF-8変換プール
  public static int trmTcpipToSJISPool;  //UTF-8→SJIS変換プール

  //trmTcpipInit ()
  //  初期化。ここでは開かない
  public static void trmTcpipInit () {
    //パラメータを復元する
    trmTcpipHost = Settings.sgsGetString ("tcpiphost", TRM_TCPIP_DEFAULT_HOST);
    trmTcpipPort = Settings.sgsGetInt ("tcpipport", TRM_TCPIP_DEFAULT_PORT, 0, 65535);
    trmTcpipUTF8 = Settings.sgsGetOnOff ("tcpiputf8", TRM_TCPIP_DEFAULT_UTF8);
    //ポート番号スピナーを作る
    trmTcpipSpinner = ComponentFactory.createDecimalSpinner (
      trmTcpipPort, 0, 65535, 1, 0,
      new ChangeListener () {
        @Override public void stateChanged (ChangeEvent ce) {
          trmTcpipPort = trmTcpipSpinner.getIntValue ();
        }
      });
    //UTF-8チェックボックスを作る
    trmTcpipCheckBox = ComponentFactory.createCheckBox (
      trmTcpipUTF8, "UTF-8",
      new ActionListener () {
        @Override public void actionPerformed (ActionEvent ae) {
          trmTcpipUTF8 = trmTcpipCheckBox.isSelected ();
        }
      });
    //受信バッファを作る
    trmTcpipReadBuffer = ByteBuffer.allocate (TRM_TCPIP_BUFFER_SIZE);
    //受信ハンドラを作る
    trmTcpipReadHander = new CompletionHandler<Integer, Void> () {
      //  受信成功
      @Override public void completed (Integer bytesRead, Void a) {
        int l = (int) bytesRead;  //受信したバイト数
        if (l == -1) {  //切断されたとき
          trmTcpipCloseSession (true);  //セッションを閉じて次の接続を待つ
          return;
        }
        byte[] b = new byte[l];
        int o = 0;
        trmTcpipReadBuffer.flip ();
        trmTcpipReadBuffer.get (b, o, l);  //ポートから読み出す
        if (false) {
          for (int i = 0; i < l; i++) {
            System.out.printf ("%02x ", 0xff & b[o + i]);
          }
          System.out.println ();
        }
        byte[] bb = b;
        int ll = l;
        if (trmTcpipUTF8) {
          //UTF-8→SJIS
          int pool = trmTcpipToSJISPool;
          bb = new byte[2 * l];
          ll = 0;
          for (int i = 0; i < l; i++) {
            int u = (pool << 8) | (0xff & b[o + i]);
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
            int s = CharacterCode.chrCharToSJIS[u];  //UTF-16(サロゲートペアなし)→SJIS変換
            if (s == 0 && u != 0) {  //0でないコードが0になった
              s = 0x81a6;  //対応なし'※'
            }
            if (0x00ff < s) {
              bb[ll++] = (byte) (s >> 8);
            }
            bb[ll++] = (byte) s;
          }  //for i
          trmTcpipToSJISPool = pool;
        }
        trmTcpipReadQueue.write (bb, 0, ll);  //キューへ書き込む。キューへ書き込めるバイト数の確認は省略
        //次の受信を開始する
        if (trmTcpipSessionChannel != null) {
          trmTcpipReadBuffer.clear ();
          trmTcpipSessionChannel.read (trmTcpipReadBuffer, null, trmTcpipReadHander);
        }
      }
      //  受信失敗
      @Override public void failed (Throwable t, Void a) {
        String message = t.getMessage ();
        if (message != null) {
          System.err.println (message);
        }
        trmTcpipCloseSession (true);  //セッションを閉じて次の接続を待つ
      }
    };
    //接続ハンドラを作る
    trmTcpipAcceptHandler = new CompletionHandler<AsynchronousSocketChannel, Void> () {
      //  接続成功
      @Override public void completed (AsynchronousSocketChannel sessionChannel, Void a) {
        trmTcpipSessionChannel = sessionChannel;
        //セッションを開く
        trmTcpipToUTF8Pool = 0;
        trmTcpipToSJISPool = 0;
        try {
          trmTcpipClientAddress = trmTcpipSessionChannel.getRemoteAddress ();
        } catch (IOException ioe) {
          ioe.printStackTrace ();
        }
        System.out.println (Multilingual.mlnJapanese ?
                            trmTcpipClientAddress + " と接続しました" :
                            trmTcpipClientAddress + " connected");
        //受信を開始する
        if (trmTcpipSessionChannel != null) {
          trmTcpipReadBuffer.clear ();
          trmTcpipSessionChannel.read (trmTcpipReadBuffer, null, trmTcpipReadHander);
        }
      }
      //  接続失敗
      @Override public void failed (Throwable t, Void a) {
        String message = t.getMessage ();
        if (message != null) {
          System.err.println (message);
        }
        trmTcpipClosePort ();
      }
    };
  }  //trmTcpipInit

  //trmTcpipTini ()
  //  後始末。ここでは閉じない
  public static void trmTcpipTini () {
    //パラメータを保存する
    Settings.sgsPutString ("tcpiphost", trmTcpipHost);
    Settings.sgsPutInt ("tcpipport", trmTcpipPort);
    Settings.sgsPutOnOff ("tcpiputf8", trmTcpipUTF8);
  }  //trmTcpipTini

  //success = trmTcpipOpenPort (readQueue)
  //  ポートを開く
  //  readQueue  受信キュー
  public static boolean trmTcpipOpenPort (ByteQueue readQueue) {
    trmTcpipReadQueue = readQueue;
    trmTcpipPortAddress = new InetSocketAddress (trmTcpipHost, trmTcpipPort);
    //ポートを開く
    try {
      trmTcpipPortChannel = AsynchronousServerSocketChannel.open ().bind (trmTcpipPortAddress);
    } catch (IOException ioe) {
      ioe.printStackTrace ();
      System.out.println (Multilingual.mlnJapanese ?
                          trmTcpipPortAddress + " を開けません" :
                          trmTcpipPortAddress + " not opened");
      return false;
    }
    System.out.println (Multilingual.mlnJapanese ?
                        trmTcpipPortAddress + " を開きました" :
                        trmTcpipPortAddress + " opened");
    //接続を待つ
    if (trmTcpipPortChannel != null) {
      trmTcpipPortChannel.accept (null, trmTcpipAcceptHandler);
    }
    return true;
  }  //trmTcpipOpenPort

  //trmTcpipClosePort ()
  //  ポートを閉じる
  public static void trmTcpipClosePort () {
    if (trmTcpipPortAddress != null) {
      System.out.println (Multilingual.mlnJapanese ?
                          trmTcpipPortAddress + " を閉じました" :
                          trmTcpipPortAddress + " closed");
      trmTcpipPortAddress = null;
    }
    //ポートを閉じる
    if (trmTcpipPortChannel != null &&
        trmTcpipPortChannel.isOpen ()) {
      try {
        trmTcpipPortChannel.close ();
      } catch (IOException ioe) {
        ioe.printStackTrace ();
      }
    }
    trmTcpipPortChannel = null;
    //セッションを閉じる
    trmTcpipCloseSession (false);  //セッションを閉じる。次の接続を待たない
  }  //trmTcpipClosePort

  //trmTcpipWrite (b, o, l)
  //  送信する
  //  b  配列
  //  o  オフセット
  //  l  長さ
  public static void trmTcpipWrite (byte[] b, int o, int l) {
    if (trmTcpipSessionChannel != null) {
      byte[] bb = b;
      int ll = l;
      if (trmTcpipUTF8) {
        //SJIS→UTF-8
        int pool = trmTcpipToUTF8Pool;
        bb = new byte[3 * l];
        ll = 0;
        for (int i = 0; i < l; i++) {
          int s = (pool << 8) | (0xff & b[o + i]);
          if ((0x0080 <= s && s <= 0x009f) || (0x00e0 <= s && s <= 0x00ff)) {  //2バイトコードの1バイト目
            pool = s;
            continue;
          }
          pool = 0;
          int u = CharacterCode.chrSJISToChar[s];  //SJIS→UTF-16(サロゲートペアなし)変換
          if (u == 0 && s != 0) {  //0でないコードが0になった
            u = '\ufffd';  //対応なし
          }
          if (u <= 0x007f) {
            bb[ll++] = (byte) u;
          } else if (u <= 0x07ff) {
            bb[ll++] = (byte) (0xc0 | (u >> 6));
            bb[ll++] = (byte) (0x80 | (u & 0x3f));
          } else {
            bb[ll++] = (byte) (0xe0 | (u >> 12));
            bb[ll++] = (byte) (0x80 | ((u >> 6) & 0x3f));
            bb[ll++] = (byte) (0x80 | (u & 0x3f));
          }
        }  //for i
        trmTcpipToUTF8Pool = pool;
      }
      ByteBuffer buffer = ByteBuffer.allocate (ll);
      buffer.put (bb, 0, ll);
      buffer.flip ();
      try {
        trmTcpipSessionChannel.write (buffer).get ();
      } catch (Throwable t) {
        String message = t.getMessage ();
        if (message != null) {
          System.err.println (message);
        }
        trmTcpipCloseSession (true);  //セッションを閉じて次の接続を待つ
      }
    }
  }  //trmTcpipWrite

  //trmTcpipCloseSession (reconnect)
  //  セッションを閉じる
  //  reconnect  次の接続を待つか
  public static void trmTcpipCloseSession (boolean reconnect) {
    if (trmTcpipSessionChannel == null) {
      return;
    }
    if (trmTcpipClientAddress != null) {
      System.out.println (Multilingual.mlnJapanese ?
                          trmTcpipClientAddress + " との接続を閉じました" :
                          trmTcpipClientAddress + " disconnected");
      trmTcpipClientAddress = null;
    }
    //セッションを閉じる
    try {
      trmTcpipSessionChannel.close ();
    } catch (IOException ioe) {
      ioe.printStackTrace ();
    }
    trmTcpipSessionChannel = null;
    if (reconnect) {
      //次の接続を待つ
      if (trmTcpipPortChannel != null) {
        trmTcpipPortChannel.accept (null, trmTcpipAcceptHandler);
      }
    }
  }  //trmTcpipCloseSession



  //PPPサーバーになりたかった何か
  //  WSLにpppdをインストールしても使えないことを知らずに作りかけたコード
  //  想定された手順
  //  (1)sudo visudoでsudoersに<username> ALL=NOPASSWD: /usr/sbin/pppdを追加する
  //  (2)XEiJにパラメータ-pppserver=onを指定する
  //  (3)RS-232CとターミナルでPPP serverとAUXを接続する
  //  (4)CONFIG.SYSにprocess=32 10 100を書く
  //  (5)ニフティなどにPPP接続していたときと同じ設定でtmsio.x、xip.x、ppp.xを実行する
  public static String trmPppAddress;  //ローカルIPアドレス:リモートIPアドレス
  public static boolean trmPppDebug;  //true=デバッグ表示あり
  public static boolean trmPppDirect;  //true=モデム層を省略する
  public static boolean trmPppLog;  //true=pppdの標準エラー出力をターミナルに出力する
  public static boolean trmPppServer;  //true=PPPサーバー有効
  //
  public static boolean trmPppAvailable;  //true=pppdが使える
  public static ByteQueue trmPppReadQueue;  //pppdの受信キュー
  public static Process trmPppProcess;  //pppdのプロセス
  public static OutputStream trmPppStdinStream;  //pppdの標準入力ストリーム
  public static Thread trmPppStdoutGobbler;  //pppdの標準出力ゴブラー
  public static Thread trmPppStderrGobbler;  //pppdの標準エラー出力ゴブラー
  public static boolean trmPppModemLayer;  //true=モデム層
  public static StringBuilder trmPppModemCommand;  //モデムコマンド

  //trmPppInit ()
  //  初期化。ここでは開かない
  public static void trmPppInit () {
    //パラメータを復元する
    trmPppAddress = Settings.sgsGetString ("pppaddress", "192.168.10.1:192.168.10.2");
    trmPppDebug = Settings.sgsGetOnOff ("pppdebug", false);
    trmPppDirect = Settings.sgsGetOnOff ("pppdirect", false);
    trmPppLog = Settings.sgsGetOnOff ("ppplog", false);
    trmPppServer = Settings.sgsGetOnOff ("pppserver", false);
    //pppdが使えるか
    trmPppAvailable = trmPppServer && new File ("/usr/sbin/pppd").exists ();
  }  //trmPppInit

  //trmPppTini ()
  //  後始末。ここでは閉じない
  public static void trmPppTini () {
    //パラメータを保存する
    Settings.sgsPutString ("pppaddress", trmPppAddress);
    Settings.sgsPutOnOff ("pppdebug", trmPppDebug);
    Settings.sgsPutOnOff ("pppdirect", trmPppDirect);
    Settings.sgsPutOnOff ("ppplog", trmPppLog);
    Settings.sgsPutOnOff ("pppserver", trmPppServer);
  }  //trmPppTini

  //success = trmPppOpenPort (readQueue)
  //  ポートを開く
  //  readQueue  受信キュー
  public static boolean trmPppOpenPort (ByteQueue readQueue) {
    //pppdを開始する
    trmPppReadQueue = readQueue;
    trmPppProcess = null;
    trmPppStdinStream = null;
    trmPppModemLayer = false;
    if (!trmPppDirect) {
      trmPppModemLayer = true;  //モデム層開始
      if (trmPppDebug) {
        System.out.println ("modem layer started");
      }
    }
    trmPppModemCommand = new StringBuilder ();
    try {
      trmPppProcess = new ProcessBuilder (
        "sudo", "pppd",
        trmPppAddress,  //ローカルIPアドレス:リモートIPアドレス
        "asyncmap", "0",
        "default-asyncmap",
        "local",  //モデム制御線を使わない
        "mru", "1500",
        "mtu", "1500",
        "noaccomp",
        "noauth",  //認証を要求しない
        "nobsdcomp",
        "noccp",  //圧縮しない
        "nodeflate",
        "nodeflatedraft",
        "nodetach",  //フォアグラウンドで動かす
        "noipv6",  //IPv6を使わない。指定しないとppp.xがUnknown protocol 0x8057を出す
        "nopcomp",
        "nopredictor1",
        "notty",  //標準入出力を使う
        "novj",
        "novjccomp",
        "passive",  //受信を待つ
        "silent"  //受信するまで送信しない
        ).start ();
      System.out.println ("pppd started");
      trmPppStdinStream = trmPppProcess.getOutputStream ();
    } catch (IOException ioe) {
      ioe.printStackTrace ();
      return false;
    }
    //pppdの標準出力ゴブラーを開始する
    trmPppStdoutGobbler = new Thread (() -> {
      if (trmPppDebug) {
        System.out.println ("pppd stdout gobbler started");
      }
      try (var is = trmPppProcess.getInputStream ()) {
        byte[] b = new byte[1024];
        while (trmPppProcess != null) {
          int k = is.read (b);  //pppdの標準出力から読み出す。ブロックする
          if (0 < k) {
            if (trmPppDebug) {
              System.out.print ("pppd stdout");
              for (int i = 0; i < k; i++) {
                System.out.printf (" %02X", b[i] & 0xff);
              }
              System.out.println ();
            }
            trmPppReadQueue.write (b, 0, k);  //キューに書き込む
          }
        }
      } catch (IOException ioe) {
      }
      if (trmPppDebug) {
        System.out.println ("pppd stdout gobbler ended");
      }
    });
    trmPppStdoutGobbler.start ();
    //pppdの標準エラー出力ゴブラーを開始する
    trmPppStderrGobbler = new Thread (() -> {
      if (trmPppDebug) {
        System.out.println ("pppd stderr gobbler started");
      }
      try (var is = trmPppProcess.getErrorStream ()) {
        byte[] b = new byte[1024];
        while (trmPppProcess != null) {
          int k = is.read (b);  //pppdの標準エラー出力から読み出す。ブロックする
          if (0 < k && trmPppLog) {
            System.out.print ("pppd stderr ");
            for (int i = 0; i < k; i++) {
              System.out.printf ("%c", b[i] & 0xff);
            }
          }
        }
      } catch (IOException ioe) {
      }
      if (trmPppDebug) {
        System.out.println ("pppd stderr gobbler ended");
      }
    });
    trmPppStderrGobbler.start ();
    return true;
  }  //trmPppOpenPort

  //trmPppClosePort ()
  //  ポートを閉じる
  public static void trmPppClosePort () {
    //pppdを終了する
    trmPppProcess.destroy ();
    trmPppProcess = null;
    System.out.println ("pppd destroyed");
    //pppdの標準出力ゴブラーが終了するまで待つ
    try {
      trmPppStdoutGobbler.join (100L);
    } catch (InterruptedException ie) {
    }
    //pppdの標準エラー出力ゴブラーが終了するまで待つ
    try {
      trmPppStderrGobbler.join (100L);
    } catch (InterruptedException ie) {
    }
  }  //trmPppClosePort



  //AUXフロー制御
  //  X68000のRS-232Cのフロー制御は通信ドライバが処理している
  //  プログラムが連続的に動作する実機と違い、間欠的に動作するエミュレータではフロー制御が適切なタイミングで行われない
  //  SerialPort側のフロー制御をjSerialCommに任せ、通信ドライバ側のフロー制御をSCCで完結させることで、取りこぼしを防ぐ
  //  フロー制御がXONでバッファの3/4が埋まって通信ドライバがXOFF$13を送信したとき
  //  フロー制御がRTSでバッファの3/4が埋まって通信ドライバがWR5 bit1 0=/RTS is Highにしたとき
  //    直ちにSCCの受信を停止する。瞬時に止まるのでどんなに速く通信していても通信ドライバの受信バッファは溢れない
  //  フロー制御がXONでバッファの3/4が空いて通信ドライバがXON$11を送信したとき
  //  フロー制御がRTSでバッファの3/4が空いて通信ドライバがWR5 bit1 1=/RTS is Lowにしたとき
  //    直ちにSCCの受信を再開する。
  //!!! 反応が速すぎると誤動作するドライバがあるかも？
  //  フロー制御の設定はどこにも接続されていないときも有効でなければならない
  public static boolean trmAUXFlowControlRTS;  //true=AUXのフロー制御はRTS。接続するときに使うので接続していなくても有効
  public static boolean trmAUXFlowControlXON;  //true=AUXのフロー制御はXON。接続するときに使うので接続していなくても有効
  public static boolean trmAUXNotReceiving;  //false=受信可($11=XONまたは1=/RTS is Low),true=受信不可($13=XOFFまたは0=/RTS is High)

  //AUX受信データバッファ
  public static int trmAUXDataBuffer;  //0xffでマスクしておくこと
  public static boolean trmAUXDataAvailable;  //ReadCommandのbit0:Rx Character Available。DataBufferの更新でセット、ReadDataでクリア

  //data = trmAUXReadData ()
  //  AUXリードデータ
  public static int trmAUXReadData () {
    trmAUXDataAvailable = false;
    return trmAUXDataBuffer;
  }  //trmAUXReadData

  //trmAUXWriteData (data)
  //  AUXライトデータ
  public static void trmAUXWriteData (int data) {
    int col = 1;
    int row = trmColToRow[col];
    if (row < 0) {
      return;
    }
    data &= 0xff;  //!!! ビット長
    if (trmAUXFlowControlXON) {
      if (data == 0x11) {  //$11=XON
        trmAUXNotReceiving = false;  //受信可
        return;
      } else if (data == 0x13) {  //$13=XOFF
        trmAUXNotReceiving = true;  //受信不可
        return;
      }
    }
    trmConnectionArray[trmCols * row + col - 1].col2rowQueue.write (data);  //AUX→Terminal/SerialPort
    //AUX送信割り込み
    trmAUXSendEmpty = false;  //送信バッファ空フラグをクリア
    TickerQueue.tkqAdd (trmAUXSendTicker, XEiJ.mpuClockTime + Z8530.scc1aInterval);  //現在時刻+間隔の時刻に送信ティッカーを設定
  }  //trmAUXWriteData

  //trmAUXSetNotReceiving (notReceiving)
  //  notReceiving  false  WR5 bit1 1=/RTS is Low   受信可
  //                true   WR5 bit1 0=/RTS is High  受信不可
  public static void trmAUXSetNotReceiving (boolean notReceiving) {
    trmAUXNotReceiving = notReceiving;
  }  //trmAUXSetNotReceiving


  //DTR
  public static boolean trmAUXDTR;
  public static void trmAUXSetDTR (boolean dtr) {
    if (trmAUXDTR != dtr) {
      trmAUXDTR = dtr;
      if (trmAUXConnection != null) {
        int row = trmAUXConnection.row;
        SerialPort port = row < 1 || trmNumberOfPorts < row ? null : trmPortArray[row - 1];
        if (port != null) {
          if (dtr) {
            port.setDTR ();
          } else {
            port.clearDTR ();
          }
        }
      }
    }
  }  //trmAUXSetDTR


  //data = trmAUXReadRR0 ()
  //  Z85C30UM 5-21
  //  RR0
  //    bit7  Break/Abort
  //    bit6  Tx Underrun/EOM
  //    bit5  CTS
  //      SCC 21 CTSA ← RS 5 CTS
  //      SCC 25 CTSB ← RS 22 CI Call Indicator (RI Ring Indicator)
  //          相手のRTS
  //          0=/CTS is High  相手が受信バッファに余裕がないから送るのやめてと言っている
  //          1=/CTS is Low   相手が受信バッファに余裕があるから送っていいよと言っている
  //    bit4  Sync/Hunt
  //      SCC 12 SYNCA ← VCC
  //      SCC 25 CTSB ← RS 22 CI Call Indicator (RI Ring Indicator)
  //    bit3  DCD
  //      SCC 22 DCDA ← RS 6 DSR Data Set Ready
  //      SCC 24 DCDB ← RS 8 CD Carrier Detect
  //    bit2  Tx Buffer Empty
  //    bit1  Zero Count
  //    bit0  Rx Character Available
  public static int trmAUXReadRR0 () {
    return (trmAUXConnection == null ? 0 :
            (trmAUXConnection.cts ? 1 << 5 : 0 << 5) |  //CTSA。CTS(相手のRTS)が接続されている
            (trmAUXConnection.dsr ? 1 << 3 : 0 << 3) |  //DCDA。DSR(相手のDTR)が接続されている
            (trmAUXSendEmpty ? 1 << 2 : 0 << 2) |  //Tx Buffer Empty
            (trmAUXDataAvailable ? 1 << 0 : 0 << 0));  //Rx Character Available
  }  //trmAUXReadRR0

  //data = trmMUSReadRR0 ()
  //  RS-232CのCIとCDはCTSBとDCDBに接続されている
  public static int trmMUSReadRR0 () {
    return (trmAUXConnection == null ? 0 :
            (trmAUXConnection.ri ? 1 << 5 : 0 << 5) |  //CTSB。RS-232CのCIが接続されている
            (trmAUXConnection.dcd ? 1 << 3 : 0 << 3));  //DCDB。RS-232CのCDが接続されている
  }  //trmMUSReadRR0

  //AUX送信ティッカー
  //  送信
  //    データをキューに追加
  //    送信ティッカーを消去
  //    送信バッファ空フラグをクリア
  //    現在時刻+間隔の時刻に送信ティッカーを設定
  //  送信ティッカー
  //    送信バッファ空フラグをセット
  //    送信割り込みが許可されているとき
  //      送信割り込み発生
  public static boolean trmAUXSendEmpty;  //true=送信バッファ空
  public static final TickerQueue.Ticker trmAUXSendTicker = new TickerQueue.Ticker () {
    @Override protected void tick () {
      trmAUXSendEmpty = true;  //送信バッファ空フラグをセット
      if ((Z8530.sccMIE & Z8530.scc1aSendMask) != 0) {  //送信割り込みが許可されているとき
        Z8530.scc1aSendRR3 = Z8530.SCC_1A_SEND_MASK;  //送信割り込み発生
        Z8530.scc1aSendRequest = Z8530.SCC_1A_SEND_MASK;
        XEiJ.mpuIRR |= XEiJ.MPU_SCC_INTERRUPT_MASK;
      }
    }  //tick
  };  //trmAUXSendTicker

  //AUXポーリングティッカー
  public static Connection trmAUXConnection;  //?→AUXの接続。null=なし
  public static final TickerQueue.Ticker trmAUXTicker = new TickerQueue.Ticker () {
    @Override protected void tick () {
      long interval = XEiJ.TMR_FREQ / 1000 * 1;  //1ms
      ByteQueue queue = trmAUXConnection.row2colQueue;  //?→AUX キュー
      if (trmAUXConnection.row2colReset) {  //?→AUX リセット
        trmAUXConnection.row2colReset = false;
        queue.clear ();
      } else if (!trmAUXNotReceiving &&  //受信可で
                 queue.used () != 0) {  //キューが空でないとき
        trmAUXDataBuffer = queue.read ();  //キューから読み出してDataBufferへ書き込む
        trmAUXDataAvailable = true;  //DataBufferは有効
        if ((Z8530.sccMIE & Z8530.scc1aReceiveMask) != 0 &&  //割り込みが許可されていて
            Z8530.scc1aReceiveRR3 == 0) {  //割り込み発生からIUSリセットまでではないとき
          Z8530.scc1aReceiveRR3 = Z8530.SCC_1A_RECEIVE_MASK;  //割り込み発生
          Z8530.scc1aReceiveRequest = Z8530.SCC_1A_RECEIVE_MASK;
          XEiJ.mpuIRR |= XEiJ.MPU_SCC_INTERRUPT_MASK;
        }
        interval = Z8530.scc1aInterval;
      }
      TickerQueue.tkqAdd (trmAUXTicker, XEiJ.mpuClockTime + interval);
    }  //tick
  };  //trmAUXTicker

  public static int trmRSDRV202Head;  //RSDRV.SYS 2.02の先頭アドレス
  public static int trmTMSIO031Head;  //tmsio.x 0.31の先頭アドレス
  public static int trmBSIO021Head;  //bsio.x 0.21の先頭アドレス

  //trmAUXFlowControlTicker
  //  フロー制御をSerialPortに反映させる
  //  ボーレートジェネレータが動き始めるとき通信設定をSerialPortに反映させるが、
  //  その時点でワークエリアが更新されていないためフロー制御を反映させることができない
  //  フロー制御だけ少し遅れて反映させることにする
  //  遅らせすぎると最初のデータに間に合わない可能性がある
  //  バッファが一杯になるまではフロー制御は行われないと仮定すれば間に合わなくても問題ないかも知れない
  public static final TickerQueue.Ticker trmAUXFlowControlTicker = new TickerQueue.Ticker () {
    @Override protected void tick () {
      int set232c = MC68060.mmuPeekLongData (0x04c0, 1);  //IOCS _SET232Cベクタ
      int modeAddress = (0x00fc0000 <= set232c && set232c < 0x01000000 ? 0x0926 :  //IPLROM
                         set232c == trmRSDRV202Head + 0x03ba ? trmRSDRV202Head + 0x0ab2 :  //RSDRV.SYS 2.02
                         set232c == trmTMSIO031Head + 0x0210 ? trmTMSIO031Head + 0x0a42 :  //tmsio.x 0.31
                         set232c == trmBSIO021Head + 0x013A ? trmBSIO021Head + 0x074a :  //bsio.x 0.21
                         0);  //不明
      if (modeAddress == 0) {
        return;
      }
      int mode = MC68060.mmuPeekWordZeroData (modeAddress, 1);  //通信設定
      if (mode == 0x0000 || mode == 0xffff) {
        return;
      }
      boolean rts = (mode & 0x0080) != 0;
      boolean xon = !rts && (mode & 0x0200) != 0;
      if (trmAUXFlowControlRTS == rts &&
          trmAUXFlowControlXON == xon) {
        return;
      }
      trmAUXFlowControlRTS = rts;
      trmAUXFlowControlXON = xon;
      if (false) {
        System.out.printf ("flowcontrol=%s\n", rts ? "rts" : xon ? "xon" : "none");
      }
      int row = trmColToRow[1];
      SerialPort port = row < 1 || trmNumberOfPorts < row ? null : trmPortArray[row - 1];
      if (port != null) {
        port.setFlowControl (rts ? (SerialPort.FLOW_CONTROL_RTS_ENABLED |
                                    SerialPort.FLOW_CONTROL_CTS_ENABLED) :
                             xon ? (SerialPort.FLOW_CONTROL_XONXOFF_IN_ENABLED |
                                    SerialPort.FLOW_CONTROL_XONXOFF_OUT_ENABLED) :
                             SerialPort.FLOW_CONTROL_DISABLED);
      }
    }  //tick
  };  //trmAUXFlowControlTicker

  //trmAUXReset ()
  //  AUXリセット
  public static void trmAUXReset () {
    if (trmAUXConnection != null) {  //接続しているとき
      TickerQueue.tkqRemove (trmAUXFlowControlTicker);
      //キューをクリアする
      trmAUXConnection.row2colReset = true;
      trmAUXConnection.col2rowReset = true;
    }
  } //trmAUXReset


  //接続
  public static Connection[] trmConnectionArray;  //Connectionの配列
  public static Box trmConnectionBox;  //Connectionのboxを入れる箱
  public static ActionListener trmConnectionListener;  //ConnectionのcheckBoxのactionListener
  public static boolean trmRefreshEnabled;  //更新ボタンは有効か
  public static JButton trmRefreshButton;  //更新ボタン
  //通信設定
  public static boolean trmSettingsEnabled;  //通信設定は有効か
  public static String[] trmBaudRateArray;  //ボーレートの選択肢
  public static int trmBaudRateIndex;  //ボーレートのインデックス
  public static String[] trmDataBitsArray;  //データビットの選択肢
  public static int trmDataBitsIndex;  //データビットのインデックス
  public static String[] trmParityArray;  //パリティの選択肢
  public static int trmParityIndex;  //パリティのインデックス
  public static String[] trmStopBitsArray;  //ストップビットの選択肢
  public static int trmStopBitsIndex;  //ストップビットのインデックス
  public static String[] trmFlowControlArray;  //フロー制御の選択肢
  public static int trmFlowControlIndex;  //フロー制御のインデックス
  public static JComboBox<String> trmBaudRateComboBox;  //ボーレート
  public static JComboBox<String> trmDataBitsComboBox;  //データビット
  public static JComboBox<String> trmParityComboBox;  //パリティ
  public static JComboBox<String> trmStopBitsComboBox;  //ストップビット
  public static JComboBox<String> trmFlowControlComboBox;  //フロー制御
  //ファイル転送
  public static boolean trmSendEnabled;  //送信ボタンは有効か
  public static JButton trmSendButton;  //送信ボタン
  public static JFileChooser trmSendFileChooser;  //送信ダイアログのファイルチューザー
  public static JDialog trmSendDialog;  //送信ダイアログ
  //時計合わせ
  public static JButton trmSetClockButton;  //時計合わせボタン
  //追加ポート
  public static JTextField trmAdditionalTextField;

  //trmInitConnection ()
  //  接続を初期化する
  public static void trmInitConnection () {
    //通信設定
    trmBaudRateArray = new String[] { "75", "150", "300", "600", "1200", "2400", "4800", "9600", "19200", "31250", "38400", "50000", "57600", "76800", "115200", "230400" };
    trmBaudRateIndex = 10;
    trmDataBitsArray = new String[] { "B5", "B6", "B7", "B8" };
    trmDataBitsIndex = 3;
    trmParityArray = new String[] { "PN", "PO", "PE" };
    trmParityIndex = 0;
    trmStopBitsArray = new String[] { "S1", "S1.5", "S2" };
    trmStopBitsIndex = 0;
    trmFlowControlArray = new String[] { "NONE", "XON", "RTS" };
    trmFlowControlIndex = 2;
    trmBaudRateComboBox = null;
    trmDataBitsComboBox = null;
    trmParityComboBox = null;
    trmStopBitsComboBox = null;
    trmFlowControlComboBox = null;
    //通信設定を復元する
    for (String keyword : Settings.sgsGetString ("terminalsettings").split ("/")) {
      for (int i = 0; i < trmBaudRateArray.length; i++) {
        if (trmBaudRateArray[i].equals (keyword)) {
          trmBaudRateIndex = i;
          break;
        }
      }
      for (int i = 0; i < trmDataBitsArray.length; i++) {
        if (trmDataBitsArray[i].equals (keyword)) {
          trmDataBitsIndex = i;
          break;
        }
      }
      for (int i = 0; i < trmParityArray.length; i++) {
        if (trmParityArray[i].equals (keyword)) {
          trmParityIndex = i;
          break;
        }
      }
      for (int i = 0; i < trmStopBitsArray.length; i++) {
        if (trmStopBitsArray[i].equals (keyword)) {
          trmStopBitsIndex = i;
          break;
        }
      }
      for (int i = 0; i < trmFlowControlArray.length; i++) {
        if (trmFlowControlArray[i].equals (keyword)) {
          trmFlowControlIndex = i;
          break;
        }
      }
    }  //for keyword
    //ファイル転送
    trmSendButton = null;
    trmSendFileChooser = null;
    trmSendDialog = null;
    //時計合わせ
    trmSetClockButton = null;
    //SerialPort
    trmPortArray = new SerialPort[0];
    trmNumberOfPorts = 0;
    //TCP/IP
    trmTcpipRow = -1;
    //PPPサーバー
    trmPppRow = -1;
    //行
    trmRows = 1;
    trmRowName = new String[1];
    trmRowName[0] = "Terminal";
    trmRowToCol = new int[1];
    Arrays.fill (trmRowToCol, -1);
    //AUX*
    trmNumberOfAUXs = 0;
    //列
    trmCols = 1;
    trmColName = new String[1];
    trmColName[0] = "Terminal";
    trmColToRow = new int[1];
    Arrays.fill (trmColToRow, -1);
    //追加ポート
    {
      String text = Settings.sgsGetString ("additionalport");
      try {
        text = URLDecoder.decode (text, "UTF-8");
      } catch (UnsupportedEncodingException uee) {
        text = "";
      }
      trmAdditionalTextField = ComponentFactory.createTextField (text, 15);
    }
    //TCP/IPポート
    trmTcpipInit ();
    //PPPサーバー
    trmPppInit ();
    //接続
    trmConnectionArray = new Connection[0];
    trmConnectionBox = ComponentFactory.createVerticalBox (
      Box.createVerticalGlue (),
      ComponentFactory.createHorizontalBox (
        Multilingual.mlnText (
          ComponentFactory.createLabel ("Additional port "),
          "ja", "追加ポート "
          ),
        trmAdditionalTextField,
        Multilingual.mlnText (
          ComponentFactory.createLabel (" TCP/IP port "),
          "ja", " TCP/IP ポート "
          ),
        trmTcpipSpinner,
        Box.createHorizontalStrut (5),
        trmTcpipCheckBox,
        Box.createHorizontalGlue ()
        )
      );
    trmConnectionListener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        String command = ae.getActionCommand ();
        for (Connection connection : trmConnectionArray) {
          if (connection.text.equals (command)) {
            if (connection.connected) {
              connection.disconnect ();
            } else {
              connection.connect ();
            }
            break;
          }
        }
      }
    };
    trmRefreshEnabled = false;
    trmRefreshButton = null;
    trmSettingsEnabled = false;
    trmSendEnabled = false;
    //接続を更新する
    trmUpdateConnection ();
    //接続を復元する
    HashSet<String> map = new HashSet<String> ();
    for (String encodedText : Settings.sgsGetString ("rs232cconnection").split ("/")) {
      try {
        map.add (URLDecoder.decode (encodedText, "UTF-8"));
      } catch (UnsupportedEncodingException uee) {
      }
    }
    for (Connection connection : trmConnectionArray) {
      if (map.contains (connection.text)) {
        connection.connect ();
      }
    }
  }  //trmInitConnection

  //trmTiniConnection ()
  //  接続を後始末する
  public static void trmTiniConnection () {
    //接続を保存する
    {
      StringBuilder sb = new StringBuilder ();
      for (Connection connection : trmConnectionArray) {
        if (connection.connected) {  //接続している
          if (sb.length () != 0) {
            sb.append ('/');
          }
          try {
            sb.append (URLEncoder.encode (connection.text, "UTF-8"));
          } catch (UnsupportedEncodingException uee) {
          }
        }
      }
      Settings.sgsPutString ("rs232cconnection", sb.toString ());
    }
    //追加ポート
    {
      String text = trmAdditionalTextField.getText ();
      try {
        text = URLEncoder.encode (text, "UTF-8");
      } catch (UnsupportedEncodingException uee) {
        text = "";
      }
      Settings.sgsPutString ("additionalport", text);
    }
    //TCP/IPポート
    trmTcpipTini ();
    //PPPサーバー
    trmPppTini ();
    //通信設定を保存する
    {
      StringBuilder sb = new StringBuilder ();
      sb.append (trmBaudRateArray[trmBaudRateIndex]);
      sb.append ('/');
      sb.append (trmDataBitsArray[trmDataBitsIndex]);
      sb.append ('/');
      sb.append (trmParityArray[trmParityIndex]);
      sb.append ('/');
      sb.append (trmStopBitsArray[trmStopBitsIndex]);
      sb.append ('/');
      sb.append (trmFlowControlArray[trmFlowControlIndex]);
      Settings.sgsPutString ("terminalsettings", sb.toString ());
    }
    //すべて切断する
    for (Connection connection : trmConnectionArray) {
      connection.disconnect ();
    }
  }  //trmTiniConnection

  //trmIsConnectionUpdatable ()
  //  接続を更新できるか
  public static boolean trmIsConnectionUpdatable () {
    for (Connection connection : trmConnectionArray) {
      if (connection.row != 0 &&  //SerialPortに接続している
          connection.connected) {  //接続している
        return false;  //更新できない
      }
    }
    return true;  //更新できる
  }  //trmIsConnectionUpdatable

  //trmUpdateConnection ()
  //  接続を更新する
  public static void trmUpdateConnection () {
    //更新できないときは何もしない
    if (!trmIsConnectionUpdatable ()) {
      return;
    }
    //SerialPort
    ArrayList<SerialPort> portList = new ArrayList<SerialPort> ();
    try {
      for (SerialPort port : SerialPort.getCommPorts ()) {
        portList.add (port);
      }
    } catch (Throwable t) {
      //  https://github.com/Fazecast/jSerialComm/issues/608
    }
    for (String descriptor : trmAdditionalTextField.getText ().split (",")) {  //追加ポート
      descriptor = descriptor.trim ();
      if (!descriptor.equals ("")) {
        try {
          SerialPort port = SerialPort.getCommPort (descriptor);
          if (port != null) {
            if (false) {  //既にリストにあるポートを追加できないようにする。getDescriptivePortName()はおそらく適切でない
              for (SerialPort anotherPort : portList) {
                if (port.getDescriptivePortName ().equals (anotherPort.getDescriptivePortName ())) {  //port.equals(anotherPort)は不可
                  port = null;
                  break;
                }
              }
            }
            if (port != null) {
              portList.add (port);
            }
          } else {
            System.out.println (descriptor + " not found");
          }
        } catch (SerialPortInvalidPortException spipe) {
          System.out.println (spipe.toString ());
        }
      }
    }
    trmNumberOfPorts = portList.size ();
    trmPortArray = portList.toArray (new SerialPort[trmNumberOfPorts]);
    //TCP/IP
    trmTcpipRow = (1 +  //Terminal
                   trmNumberOfPorts);  //SerialPort
    //PPPサーバー
    if (trmPppAvailable) {
      trmPppRow = (1 +  //Terminal
                   trmNumberOfPorts +  //SerialPort
                   (0 <= trmTcpipRow ? 1 : 0));  //TCP/IP
    } else {
      trmPppRow = -1;
    }
    //行
    trmRows = (1 +  //Terminal
               trmNumberOfPorts +  //SerialPort
               (0 <= trmTcpipRow ? 1 : 0) +  //TCP/IP
               (0 <= trmPppRow ? 1 : 0));  //PPPサーバー
    trmRowName = new String[trmRows];
    //Terminal
    trmRowName[0] = "Terminal";
    //SerialPort
    for (int row = 1; row <= trmNumberOfPorts; row++) {
      SerialPort port = trmPortArray[row - 1];
      trmRowName[row] = port.getSystemPortName () + "(" + port.getPortDescription () + ")";
    }
    //TCP/IP
    if (0 <= trmTcpipRow) {
      trmRowName[trmTcpipRow] = "TCP/IP";
    }
    //PPPサーバー
    if (0 <= trmPppRow) {
      trmRowName[trmPppRow] = "PPP server";
    }
    trmRowToCol = new int[trmRows];
    Arrays.fill (trmRowToCol, -1);
    //AUX*
    trmNumberOfAUXs = 1;
    //列
    trmCols = (1 +  //Terminal
               trmNumberOfAUXs);  //AUX
    trmColName = new String[trmCols];
    trmColName[0] = "Terminal";
    for (int col = 1; col < trmCols; col++) {
      trmColName[col] = col == 1 ? "AUX" : "AUX" + col;
    }
    trmColToRow = new int[trmCols];
    Arrays.fill (trmColToRow, -1);
    //接続
    for (int index = trmConnectionArray.length - 1; 0 <= index; index--) {
      trmConnectionBox.remove (index);
    }
    trmConnectionArray = new Connection[trmCols * trmRows - 1];
    for (int row = 0; row < trmRows; row++) {
      for (int col = 0; col < trmCols; col++) {
        if (col == 0 && row == 0) {
          continue;
        }
        Connection connection = new Connection (row, col);
        trmConnectionArray[connection.index] = connection;
        trmConnectionBox.add (connection.box, connection.index);
      }  //for col
    }  //for row
    trmUpdateComponents ();
    trmConnectionBox.validate ();
  }  //trmUpdateConnection

  //trmUpdateComponents ()
  //  接続できないConnectionのcheckBoxを無効にする
  //  SerialPortに接続しているConnectionがあるとき更新ボタンを無効にする
  //  Terminal-SerialPortがないとき通信設定を表示しない
  public static void trmUpdateComponents () {
    boolean updatable = true;  //更新できる
    boolean configurable = false;  //設定できない
    boolean transferable = false;  //転送できない
    for (Connection connection : trmConnectionArray) {
      if (connection.connected) {  //接続している
        connection.checkBox.setEnabled (true);  //切断できる
        if (1 <= connection.row && connection.row <= trmNumberOfPorts) {  //SerialPortを接続している
          updatable = false;  //更新できない
          if (connection.col == 0) {  //TerminalとSerialPortを接続している
            configurable = true;  //設定できる
          }
        }
        if (connection.row == 0 ||
            connection.col == 0) {  //Terminalを接続している
          transferable = true;  //転送できる
        }
      } else {  //接続していない
        connection.checkBox.setEnabled (connection.isConnectable ());  //接続できるときだけ有効
      }
    }
    trmRefreshEnabled = updatable;
    if (trmRefreshButton != null) {
      trmRefreshButton.setEnabled (updatable);
    }
    trmSettingsEnabled = configurable;
    trmSendEnabled = transferable;
    if (trmBaudRateComboBox != null) {
      trmBaudRateComboBox.setEnabled (configurable);
      trmDataBitsComboBox.setEnabled (configurable);
      trmParityComboBox.setEnabled (configurable);
      trmStopBitsComboBox.setEnabled (configurable);
      trmFlowControlComboBox.setEnabled (configurable);
      trmSendButton.setEnabled (transferable);
      trmSetClockButton.setEnabled (transferable);
    }
  }  //trmUpdateComponents

  //trmSetBaudRate (index)
  //  ボーレートを設定する
  public static void trmSetBaudRate (int index) {
    if (0 <= index && index < trmBaudRateArray.length) {
      trmBaudRateIndex = index;
      trmReflectSettings (0);
    }
  }  //trmSetBaudRate

  //trmSetDataBits (index)
  //  データビットを設定する
  public static void trmSetDataBits (int index) {
    if (0 <= index && index < trmDataBitsArray.length) {
      trmDataBitsIndex = index;
      trmReflectSettings (0);
    }
  }  //trmSetDataBits

  //trmSetParity (index)
  //  パリティを設定する
  public static void trmSetParity (int index) {
    if (0 <= index && index < trmParityArray.length) {
      trmParityIndex = index;
      trmReflectSettings (0);
    }
  }  //trmSetParity

  //trmSetStopBits (index)
  //  ストップビットを設定する
  public static void trmSetStopBits (int index) {
    if (0 <= index && index < trmStopBitsArray.length) {
      trmStopBitsIndex = index;
      trmReflectSettings (0);
    }
  }  //trmSetStopBits

  //trmSetFlowControl (index)
  //  フロー制御を設定する
  public static void trmSetFlowControl (int index) {
    if (0 <= index && index < trmFlowControlArray.length) {
      trmFlowControlIndex = index;
      trmReflectSettings (0);
    }
  }  //trmSetFlowControl

  //trmReflectSettings (col)
  //  通信設定をSerialPortに反映させる
  public static void trmReflectSettings (int col) {
    int row = trmColToRow[col];
    SerialPort port = row < 1 || trmNumberOfPorts < row ? null : trmPortArray[row - 1];
    if (col == 0) {  //Terminal
      String baudRate = trmBaudRateArray[trmBaudRateIndex];
      String dataBits = trmDataBitsArray[trmDataBitsIndex];
      String stopBits = trmStopBitsArray[trmStopBitsIndex];
      String parity = trmParityArray[trmParityIndex];
      String flowControl = trmFlowControlArray[trmFlowControlIndex];
      if (port != null) {
        port.setComPortParameters (Integer.parseInt (baudRate, 10),
                                   Integer.parseInt (dataBits.substring (1), 10),
                                   stopBits.equals ("S1.5") ? SerialPort.ONE_POINT_FIVE_STOP_BITS :
                                   stopBits.equals ("S2") ? SerialPort.TWO_STOP_BITS : SerialPort.ONE_STOP_BIT,
                                   parity.equals ("PO") ? SerialPort.ODD_PARITY :
                                   parity.equals ("PE") ? SerialPort.EVEN_PARITY : SerialPort.NO_PARITY);
        port.setFlowControl (flowControl.equals ("RTS") ? (SerialPort.FLOW_CONTROL_RTS_ENABLED |
                                                           SerialPort.FLOW_CONTROL_CTS_ENABLED) :
                             flowControl.equals ("XON") ? (SerialPort.FLOW_CONTROL_XONXOFF_IN_ENABLED |
                                                           SerialPort.FLOW_CONTROL_XONXOFF_OUT_ENABLED) :
                             SerialPort.FLOW_CONTROL_DISABLED);
      }
    } else if (col == 1) {  //AUX
      double rate = Z8530.sccFreq / (double) ((Z8530.scc1aBaudRateGen + 2) << (Z8530.scc1aClockModeShift + 1));
      double bits = (1.0 +  //start
                     (Z8530.scc1aRxBits == 0 ? 5.0 :
                      Z8530.scc1aRxBits == 1 ? 7.0 :
                      Z8530.scc1aRxBits == 2 ? 6.0 : 8.0) +  //data
                     ((Z8530.scc1aParity & 1) == 0 ? 0.0 : 1.0) +  //parity
                     (Z8530.scc1aStop == 0 ? 0.0 :
                      Z8530.scc1aStop == 1 ? 1.0 :
                      Z8530.scc1aStop == 2 ? 1.5 : 2.0));  //stop
      double interval = bits / rate;
      if (false) {
        System.out.printf ("%08x baudrate=%.3fbps interval=%.3fus\n", XEiJ.regPC0, rate, interval * 1e+6);
      }
      Z8530.scc1aInterval = Math.round (interval * (double) XEiJ.TMR_FREQ);
      //
      if (port != null) {
        port.setComPortParameters ((int) Math.round (rate),
                                   Z8530.scc1aRxBits == 0b00 ? 5 :
                                   Z8530.scc1aRxBits == 0b01 ? 7 :
                                   Z8530.scc1aRxBits == 0b10 ? 6 : 8,
                                   Z8530.scc1aStop == 0b10 ? SerialPort.ONE_POINT_FIVE_STOP_BITS :
                                   Z8530.scc1aStop == 0b11 ? SerialPort.TWO_STOP_BITS : SerialPort.ONE_STOP_BIT,
                                   Z8530.scc1aParity == 0b01 ? SerialPort.ODD_PARITY :
                                   Z8530.scc1aParity == 0b11 ? SerialPort.EVEN_PARITY : SerialPort.NO_PARITY);
      }
      //
      //フロー制御をSerialPortに反映させる
      TickerQueue.tkqAdd (trmAUXFlowControlTicker, XEiJ.mpuClockTime + XEiJ.TMR_FREQ * 500 / 1000000);  //500us後
    } else {  //AUX2～
      //!!! 未対応
    }
  }  //trmReflectSettings


  //trmInit ()
  //  ターミナルウインドウを初期化する
  public static void trmInit () {
    trmFrame = null;
    trmBoard = null;
    trmPopupMenu = null;
    trmPopupCutMenuItem = null;
    trmPopupCopyMenuItem = null;
    trmPopupPasteMenuItem = null;
    trmPopupSelectAllMenuItem = null;
    trmPopupSendCtrlCMenuItem = null;
    trmOutputBuilder = new StringBuilder ();
    trmOutputEnd = 0;
    trmOutputSJIS1 = 0;
    //
    trmInitConnection ();
    trmReset ();
  }

  public static void trmReset () {
    trmRSDRV202Head = 0;
    trmTMSIO031Head = 0;
    trmBSIO021Head = 0;
    trmAUXFlowControlXON = false;
    trmAUXFlowControlRTS = false;
  }

  //trmTini ()
  //  後始末
  public static void trmTini () {
    //接続
    trmTiniConnection ();
  }  //trmTini

  //trmMake ()
  //  ターミナルウインドウを作る
  //  ここでは開かない
  public static void trmMake () {

    //テキストエリア
    trmBoard = ComponentFactory.createScrollTextArea (
      trmOutputBuilder.toString (),  //作る前に出力されていた文字列を設定する
      650, 350,
      true);
    trmOutputBuilder = null;  //これはもういらない
    trmBoard.setUnderlineCursorOn (true);
    trmBoard.setLineWrap (true);  //行を折り返す
    trmBoard.addDocumentListener (new DocumentListener () {
      @Override public void changedUpdate (DocumentEvent de) {
      }
      @Override public void insertUpdate (DocumentEvent de) {
        if (de.getOffset () < trmOutputEnd) {
          trmOutputEnd += de.getLength ();  //出力された文字列の末尾を調整する
        }
      }
      @Override public void removeUpdate (DocumentEvent de) {
        if (de.getOffset () < trmOutputEnd) {
          trmOutputEnd -= Math.min (de.getLength (), trmOutputEnd - de.getOffset ());  //出力された文字列の末尾を調整する
        }
      }
    });
    trmBoard.addKeyListener (new KeyAdapter () {
      @Override public void keyPressed (KeyEvent ke) {
        int keyCode = ke.getKeyCode ();
        if (keyCode == KeyEvent.VK_ENTER) {  //ENTERキーが押された
          ke.consume ();  //ENTERキーをキャンセルする
          trmEnter ();  //ENTERキーを処理する
        } else if (keyCode == KeyEvent.VK_PAUSE) {  //Pauseキーが押された
          ke.consume ();  //Pauseキーをキャンセルする
          trmSendString ("\u0003");  //^Cを送信する
        }
      }
    });

    //ポップアップメニュー
    ActionListener popupActionListener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        switch (ae.getActionCommand ()) {
        case "Cut":  //切り取り
          trmCut ();
          break;
        case "Copy":  //コピー
          trmCopy ();
          break;
        case "Paste":  //貼り付け
          trmPaste ();
          break;
        case "Select All":  //すべて選択
          trmSelectAll ();
          break;
        case "Send ^C":  //^C 送信
          trmSendString ("\u0003");  //^Cを送信する
          break;
        }
      }
    };
    trmPopupMenu = ComponentFactory.createPopupMenu (
      trmPopupCutMenuItem = Multilingual.mlnText (
        ComponentFactory.createMenuItem ("Cut", 'T', popupActionListener),
        "ja", "切り取り"),
      trmPopupCopyMenuItem = Multilingual.mlnText (
        ComponentFactory.createMenuItem ("Copy", 'C', popupActionListener),
        "ja", "コピー"),
      trmPopupPasteMenuItem = Multilingual.mlnText (
        ComponentFactory.createMenuItem ("Paste", 'P', popupActionListener),
        "ja", "貼り付け"),
      ComponentFactory.createHorizontalSeparator (),
      trmPopupSelectAllMenuItem = Multilingual.mlnText (
        ComponentFactory.createMenuItem ("Select All", 'A', popupActionListener),
        "ja", "すべて選択"),
      ComponentFactory.createHorizontalSeparator (),
      trmPopupSendCtrlCMenuItem = Multilingual.mlnText (
        ComponentFactory.createMenuItem ("Send ^C", popupActionListener),
        "ja", "^C 送信")
      );
    trmBoard.addMouseListener (new MouseAdapter () {
      @Override public void mousePressed (MouseEvent me) {
        trmShowPopup (me);
      }
      @Override public void mouseReleased (MouseEvent me) {
        trmShowPopup (me);
      }
    });

    //アクションリスナー
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        String command = ae.getActionCommand ();
        switch (command) {
        case "Refresh":
          trmUpdateConnection ();
          break;
        case "Baud rate":
          trmSetBaudRate (((JComboBox) source).getSelectedIndex ());
          break;
        case "Data bits":
          trmSetDataBits (((JComboBox) source).getSelectedIndex ());
          break;
        case "Parity":
          trmSetParity (((JComboBox) source).getSelectedIndex ());
          break;
        case "Stop bits":
          trmSetStopBits (((JComboBox) source).getSelectedIndex ());
          break;
        case "Flow control":
          trmSetFlowControl (((JComboBox) source).getSelectedIndex ());
          break;
        case "Send file":
          trmSendFile ();
          break;
        case "Set clock":
          trmSetClock ();
          break;
        default:
          System.out.println ("unknown action command " + command);
        }
      }
    };

    //ボタンとコンボボックス
    trmRefreshButton =
      ComponentFactory.setEnabled (
        Multilingual.mlnText (
          ComponentFactory.createButton ("Refresh", listener),
          "ja", "更新"),
        trmRefreshEnabled);
    trmBaudRateComboBox =
      ComponentFactory.setEnabled (
        Multilingual.mlnToolTipText (
          ComponentFactory.createComboBox (
            trmBaudRateIndex, "Baud rate", listener, trmBaudRateArray),
          "ja", "ボーレート"),
        trmSettingsEnabled);
    trmDataBitsComboBox =
      ComponentFactory.setEnabled (
        Multilingual.mlnToolTipText (
          ComponentFactory.createComboBox (
            trmDataBitsIndex, "Data bits", listener, trmDataBitsArray),
          "ja", "データビット"),
        trmSettingsEnabled);
    trmParityComboBox =
      ComponentFactory.setEnabled (
        Multilingual.mlnToolTipText (
          ComponentFactory.createComboBox (
            trmParityIndex, "Parity", listener, trmParityArray),
          "ja", "パリティ"),
        trmSettingsEnabled);
    trmStopBitsComboBox =
      ComponentFactory.setEnabled (
        Multilingual.mlnToolTipText (
          ComponentFactory.createComboBox (
            trmStopBitsIndex, "Stop bits", listener, trmStopBitsArray),
          "ja", "ストップビット"),
        trmSettingsEnabled);
    trmFlowControlComboBox =
      ComponentFactory.setEnabled (
        Multilingual.mlnToolTipText (
          ComponentFactory.createComboBox (
            trmFlowControlIndex, "Flow control", listener, trmFlowControlArray),
          "ja", "フロー制御"),
        trmSettingsEnabled);
    trmSendButton =
      ComponentFactory.setEnabled (
        Multilingual.mlnText (
          ComponentFactory.createButton ("Send file", listener),
          "ja", "ファイル送信"),
        trmSendEnabled);
    trmSetClockButton =
      ComponentFactory.setEnabled (
        Multilingual.mlnText (
          ComponentFactory.createButton ("Set clock", listener),
          "ja", "時計合わせ"),
        trmSendEnabled);

    //ウインドウ
    //  ┏━━━━━━━━━━━━━━━━┓
    //  ┃┌接続────────────┐┃
    //  ┃│                            │┃
    //  ┃│                            │┃NORTH
    //  ┃│                            │┃
    //  ┃└──────────────┘┃
    //  ┃                                ┃CENTER
    //  ┣━━━━━━━━━━━━━━━━┫
    //  ┃┌通信設定┐┌コマンド┐        ┃
    //  ┃│        ││        │        ┃NORTH
    //  ┃└────┘└────┘        ┃
    //  ┃┌ターミナル─────────┐┃
    //  ┃│                            │┃
    //  ┃│                            │┃CENTER
    //  ┃│                            │┃
    //  ┃└──────────────┘┃
    //  ┗━━━━━━━━━━━━━━━━┛
    //接続
    Box connectionBox = Multilingual.mlnTitledBorder (
      ComponentFactory.setTitledLineBorder (
        ComponentFactory.createHorizontalBox (
          Box.createHorizontalStrut (5),
          ComponentFactory.createVerticalBox (
            Box.createVerticalGlue (),
            trmRefreshButton,
            Box.createVerticalGlue ()
            ),
          Box.createHorizontalStrut (10),
          trmConnectionBox,
          Box.createHorizontalGlue ()
          ),
        "Connection"),
      "ja", "接続");
    //通信設定
    Box settingsBox = Multilingual.mlnTitledBorder (
      ComponentFactory.setTitledLineBorder (
        ComponentFactory.createHorizontalBox (
          Box.createHorizontalStrut (5),
          trmBaudRateComboBox,
          trmDataBitsComboBox,
          trmParityComboBox,
          trmStopBitsComboBox,
          trmFlowControlComboBox,
          Box.createHorizontalStrut (5)
          ),  //createHorizontalBox
        "Communication Settings"),  //setTitledLineBorder
      "ja", "通信設定");  //mlnTitledBorder
    //コマンド
    Box commandxBox = Multilingual.mlnTitledBorder (
      ComponentFactory.setTitledLineBorder (
        ComponentFactory.createHorizontalBox (
          Box.createHorizontalStrut (5),
          trmSendButton,
          trmSetClockButton,
          Box.createHorizontalStrut (5)
          ),  //createHorizontalBox
        "Command"),  //setTitledLineBorder
      "ja", "コマンド");  //mlnTitledBorder
    //ウインドウ
    trmFrame = Multilingual.mlnTitle (
      ComponentFactory.createRestorableSubFrame (
        Settings.SGS_TRM_FRAME_KEY,
        "RS-232C and terminal",
        null,
        ComponentFactory.createVerticalSplitPane (
          ComponentFactory.createScrollPane (
            ComponentFactory.createBorderPanel (
              ComponentFactory.createHorizontalBox (
                Box.createHorizontalGlue ()
                ),  //CENTER
              connectionBox  //NORTH
              )  //createBorderPanel
            ),  //createScrollPane
          ComponentFactory.createBorderPanel (
            Multilingual.mlnTitledBorder (
              ComponentFactory.setTitledLineBorder (
                trmBoard,
                "Terminal"),
              "ja", "ターミナル"),  //CENTER
            ComponentFactory.createHorizontalBox (
              settingsBox,
              commandxBox,
              Box.createHorizontalGlue ()
              )  //createHorizontalBox  NORTH
            )  //createBorderPanel
          )  //createVerticalSplitPane
        ),  //createRestorableSubFrame
      "ja", "RS-232C とターミナル");

  }  //trmMake()

  //trmShowPopup (me)
  //  ポップアップメニューを表示する
  //  テキストエリアのマウスリスナーが呼び出す
  public static void trmShowPopup (MouseEvent me) {
    if (me.isPopupTrigger ()) {
      //選択範囲があれば切り取りとコピーが有効
      boolean enableCutAndCopy = XEiJ.clpClipboard != null && trmBoard.getSelectionStart () != trmBoard.getSelectionEnd ();
      ComponentFactory.setEnabled (trmPopupCutMenuItem, enableCutAndCopy);
      ComponentFactory.setEnabled (trmPopupCopyMenuItem, enableCutAndCopy);
      //クリップボードに文字列があれば貼り付けが有効
      ComponentFactory.setEnabled (trmPopupPasteMenuItem, XEiJ.clpClipboard != null && XEiJ.clpClipboard.isDataFlavorAvailable (DataFlavor.stringFlavor));
      //クリップボードがあればすべて選択が有効
      ComponentFactory.setEnabled (trmPopupSelectAllMenuItem, XEiJ.clpClipboard != null);
      //Terminalが接続していれば^C送信が有効
      ComponentFactory.setEnabled (trmPopupSendCtrlCMenuItem,
                                   trmRowToCol[0] == 1 ||  //Terminal→AUX
                                   0 < trmColToRow[0]);  //Terminal→SerialPort
      //ポップアップメニューを表示する
      trmPopupMenu.show (me.getComponent (), me.getX (), me.getY ());
    }
  }  //trmShowPopup(MouseEvent)

  //trmCut ()
  //  切り取り
  public static void trmCut () {
    if (XEiJ.clpClipboard != null) {
      //選択範囲の文字列をコピーする
      XEiJ.clpClipboardString = trmBoard.getSelectedText ();
      try {
        XEiJ.clpClipboard.setContents (XEiJ.clpStringContents, XEiJ.clpClipboardOwner);
        XEiJ.clpIsClipboardOwner = true;  //自分がコピーした
      } catch (Exception e) {
        return;
      }
      //選択範囲の文字列を削除する
      trmBoard.replaceRange ("", trmBoard.getSelectionStart (), trmBoard.getSelectionEnd ());
    }
  }  //trmCut()

  //trmCopy ()
  //  コピー
  public static void trmCopy () {
    if (XEiJ.clpClipboard != null) {
      //選択範囲の文字列をコピーする
      String selectedText = trmBoard.getSelectedText ();
      if (selectedText != null) {
        XEiJ.clpClipboardString = selectedText;
        try {
          XEiJ.clpClipboard.setContents (XEiJ.clpStringContents, XEiJ.clpClipboardOwner);
          XEiJ.clpIsClipboardOwner = true;  //自分がコピーした
        } catch (Exception e) {
          return;
        }
      }
    }
  }  //trmCopy()

  //trmPaste ()
  //  貼り付け
  public static void trmPaste () {
    if (XEiJ.clpClipboard != null) {
      //クリップボードから文字列を取り出す
      String string = null;
      try {
        string = (String) XEiJ.clpClipboard.getData (DataFlavor.stringFlavor);
      } catch (Exception e) {
        return;
      }
      //選択範囲の文字列を置換する
      trmBoard.replaceRange (string, trmBoard.getSelectionStart (), trmBoard.getSelectionEnd ());
    }
  }  //trmPaste()

  //trmSelectAll ()
  //  すべて選択
  public static void trmSelectAll () {
    if (XEiJ.clpClipboard != null) {
      //すべて選択する
      trmBoard.selectAll ();
    }
  }  //trmSelectAll()

  //trmStart ()
  public static void trmStart () {
    if (RestorableFrame.rfmGetOpened (Settings.SGS_TRM_FRAME_KEY)) {
      trmOpen ();
    }
  }  //trmStart()

  //trmOpen ()
  //  ターミナルウインドウを開く
  public static void trmOpen () {
    if (trmFrame == null) {
      trmMake ();
    }
    XEiJ.pnlExitFullScreen (false);
    trmFrame.setVisible (true);
  }  //trmOpen()

  //trmPrintSJIS (d)
  //  SJISで1バイト追加する
  //  SJISの1バイト目は繰り越して2バイト目が来たときに表示する
  public static void trmPrintSJIS (int d) {
    d &= 0xff;
    if (trmOutputSJIS1 != 0) {  //前回SJISの1バイト目を繰り越した
      if (0x40 <= d && d != 0x7f && d <= 0xfc) {  //SJISの2バイト目が来た
        int c = CharacterCode.chrSJISToChar[trmOutputSJIS1 << 8 | d];  //2バイトで変換する
        if (c != 0) {  //2バイトで変換できた
          trmPrintChar (c);  //1文字表示する
        } else {  //2バイトで変換できなかった
          //2バイトで変換できなかったがSJISの1バイト目と2バイト目であることはわかっているので2バイト分のコードを表示する
          trmPrintChar ('[');
          trmPrintChar (XEiJ.fmtHexc (trmOutputSJIS1 >> 4));
          trmPrintChar (XEiJ.fmtHexc (trmOutputSJIS1 & 15));
          trmPrintChar (XEiJ.fmtHexc (d >> 4));
          trmPrintChar (XEiJ.fmtHexc (d & 15));
          trmPrintChar (']');
        }
        trmOutputSJIS1 = 0;
        return;
      }
      //SJISの2バイト目が来なかった
      //前回繰り越したSJISの1バイト目を吐き出す
      trmPrintChar ('[');
      trmPrintChar (XEiJ.fmtHexc (trmOutputSJIS1 >> 4));
      trmPrintChar (XEiJ.fmtHexc (trmOutputSJIS1 & 15));
      trmPrintChar (']');
      trmOutputSJIS1 = 0;
    }
    if (0x81 <= d && d <= 0x9f || 0xe0 <= d && d <= 0xef) {  //SJISの1バイト目が来た
      trmOutputSJIS1 = d;  //次回に繰り越す
    } else {  //SJISの1バイト目が来なかった
      int c = CharacterCode.chrSJISToChar[d];  //1バイトで変換する
      if (c != 0) {  //1バイトで変換できた
        trmPrintChar (c);  //1文字表示する
      } else {  //1バイトで変換できなかった
        //1バイトで変換できなかったがSJISの1バイト目でないことはわかっているので1バイト分のコードを表示する
        trmPrintChar ('[');
        trmPrintChar (XEiJ.fmtHexc (d >> 4));
        trmPrintChar (XEiJ.fmtHexc (d & 15));
        trmPrintChar (']');
      }
    }
  }  //trmPrintSJIS(int)

  //trmPrintChar (c)
  //  末尾に1文字追加する
  public static void trmPrintChar (int c) {
    if (c == 0x08) {  //バックスペース
      if (trmOutputEnd > 0) {
        if (trmBoard != null) {
          trmBoard.replaceRange ("", trmOutputEnd - 1, trmOutputEnd);  //1文字削除
          trmOutputEnd--;
          trmBoard.setCaretPosition (trmOutputEnd);
        } else {
          trmOutputBuilder.delete (trmOutputEnd - 1, trmOutputEnd);  //1文字削除
          trmOutputEnd--;
        }
      }
    } else if (c >= 0x20 && c != 0x7f || c == 0x09 || c == 0x0a) {  //タブと改行以外の制御コードを除く
      if (trmBoard != null) {
        trmBoard.insert (String.valueOf ((char) c), trmOutputEnd);  //1文字追加
        trmOutputEnd++;
        if (trmOutputEnd >= TRM_CUT_OUTPUT_LENGTH) {
          trmBoard.replaceRange ("", 0, trmOutputEnd - TRM_MAX_OUTPUT_LENGTH);  //先頭を削って短くする
          trmOutputEnd = TRM_MAX_OUTPUT_LENGTH;
        }
        trmBoard.setCaretPosition (trmOutputEnd);
      } else {
        trmOutputBuilder.append ((char) c);  //1文字追加
        trmOutputEnd++;
        if (trmOutputEnd >= TRM_CUT_OUTPUT_LENGTH) {
          trmOutputBuilder.delete (0, trmOutputEnd - TRM_MAX_OUTPUT_LENGTH);  //先頭を削って短くする
          trmOutputEnd = TRM_MAX_OUTPUT_LENGTH;
        }
      }
    }
  }  //trmPrintChar(int)

  //trmPrint (s)
  //  末尾に文字列を追加する
  //  情報表示用
  //  制御コードを処理しないのでタブと改行以外の制御コードを含めないこと
  public static void trmPrint (String s) {
    if (s == null) {
      return;
    }
    if (trmFrame != null) {
      trmBoard.insert (s, trmOutputEnd);  //文字列追加
      trmOutputEnd += s.length ();
      if (trmOutputEnd >= TRM_CUT_OUTPUT_LENGTH) {
        trmBoard.replaceRange ("", 0, trmOutputEnd - TRM_MAX_OUTPUT_LENGTH);  //先頭を削って短くする
        trmOutputEnd = TRM_MAX_OUTPUT_LENGTH;
      }
      trmBoard.setCaretPosition (trmOutputEnd);
    } else {
      trmOutputBuilder.append (s);  //文字列追加
      trmOutputEnd += s.length ();
      if (trmOutputEnd >= TRM_CUT_OUTPUT_LENGTH) {
        trmOutputBuilder.delete (0, trmOutputEnd - TRM_MAX_OUTPUT_LENGTH);  //先頭を削って短くする
        trmOutputEnd = TRM_MAX_OUTPUT_LENGTH;
      }
    }
  }  //trmPrint(String)

  //trmPrintln (s)
  //  末尾に文字列と改行を追加する
  //  情報表示用
  //  制御コードを処理しないのでタブと改行以外の制御コードを含めないこと
  public static void trmPrintln (String s) {
    trmPrint (s);
    trmPrintChar ('\n');
  }  //trmPrintln(String)

  //trmEnter ()
  //  ENTERキーを処理する
  public static void trmEnter () {
    String text = trmBoard.getText ();  //テキスト全体
    int length = text.length ();  //テキスト全体の長さ
    int outputLineStart = text.lastIndexOf ('\n', trmOutputEnd - 1) + 1;  //出力の末尾の行の先頭。プロンプトの先頭
    int caretLineStart = text.lastIndexOf ('\n', trmBoard.getCaretPosition () - 1) + 1;  //キャレットがある行の先頭
    if (outputLineStart <= caretLineStart) {  //出力の末尾の行の先頭以降でENTERキーが押された
      trmBoard.replaceRange ("", trmOutputEnd, length);  //入力された文字列を一旦削除する
      trmSendString (text.substring (trmOutputEnd, length) + "\r");  //入力された文字列を送信する
    } else if (outputLineStart < trmOutputEnd) {  //出力の末尾の行の先頭よりも手前でENTERキーが押されて、出力の末尾の行にプロンプトがあるとき
      String prompt = text.substring (outputLineStart, trmOutputEnd);  //出力の末尾の行のプロンプト
      int caretLineEnd = text.indexOf ('\n', caretLineStart);  //キャレットがある行の末尾
      if (caretLineEnd == -1) {
        caretLineEnd = length;
      }
      String line = text.substring (caretLineStart, caretLineEnd);  //キャレットがある行
      int start = line.indexOf (prompt);  //キャレットがある行のプロンプトの先頭
      if (start >= 0) {  //キャレットがある行にプロンプトがあるとき
        trmOutputEnd = length;  //入力された文字列を無効化する
        if (text.charAt (trmOutputEnd - 1) != '\n') {  //改行で終わっていないとき
          trmBoard.insert ("\n", trmOutputEnd);  //末尾にENTERを追加する
          trmOutputEnd++;
          if (trmOutputEnd >= TRM_CUT_OUTPUT_LENGTH) {
            trmBoard.replaceRange ("", 0, trmOutputEnd - TRM_MAX_OUTPUT_LENGTH);  //先頭を削って短くする
            trmOutputEnd = TRM_MAX_OUTPUT_LENGTH;
          }
        }
        trmBoard.setCaretPosition (trmOutputEnd);
        trmSendString (line.substring (start + prompt.length ()) + "\r");  //プロンプトの後ろから行の末尾までを送信する
      }
    }
  }  //trmEnter()

  //trmSendString (s)
  //  文字列をSJISに変換してAUXまたはSerialPortへ送信する
  public static void trmSendString (String s) {
    int l = s.length ();
    if (l == 0) {
      return;
    }
    byte[] b = new byte[l * 2];
    int k = 0;
    for (int i = 0; i < l; i++) {
      int c = CharacterCode.chrCharToSJIS[s.charAt (i)];
      if (0x00ff < c) {
        b[k++] = (byte) (c >> 8);
      }
      b[k++] = (byte) c;
    }
    if (trmRowToCol[0] == 1) {  //Terminal→AUX。row2col
      int row = 0;
      int col = 1;
      trmConnectionArray[trmCols * row + col - 1].row2colQueue.write (b, 0, k);
    } else if (0 < trmColToRow[0]) {  //Terminal→SerialPort。col2row
      int row = trmColToRow[0];
      int col = 0;
      trmConnectionArray[trmCols * row + col - 1].col2rowQueue.write (b, 0, k);
    }
  }  //trmSendString



  //trmSetClock ()
  //  時計合わせボタンが押された
  public static void trmSetClock () {
    String[] options = new String[] {
      Multilingual.mlnJapanese ? "送信" : "Send",
      Multilingual.mlnJapanese ? "キャンセル" : "Cancel",
    };
    if (JOptionPane.showOptionDialog (
      trmFrame,
      Multilingual.mlnJapanese ?
      "DATE と TIME を送信して時計を大まかに合わせます。\n" +
      "COMMAND.X を CTTY AUX で準備してください。":
      "Sending DATE and TIME to set clock roughly.\n" +
      "Prepare COMMAND.X with CTTY AUX.",
      Multilingual.mlnJapanese ? "時計合わせ" : "Set clock",
      JOptionPane.OK_CANCEL_OPTION,
      JOptionPane.PLAIN_MESSAGE,
      null,
      options,
      options[0]) == 0) {
      new SimpleTask (XEiJ.tmrScheduler, (n) -> {
        long dttm = DnT.dntDttmCmil (System.currentTimeMillis () + RP5C15.rtcCmilGap);
        trmSendString (
          String.format ("date %04d-%02d-%02d\rtime %02d:%02d:%02d\r",
                         DnT.dntYearDttm (dttm),
                         DnT.dntMontDttm (dttm),
                         DnT.dntMdayDttm (dttm),
                         DnT.dntHourDttm (dttm),
                         DnT.dntMinuDttm (dttm),
                         DnT.dntSecoDttm (dttm))
          );
        return 0;
      }).oneShot (0L);
    }  //if
  }  //trmSetClock



  //trmSendFile ()
  //  ファイル送信ボタンが押された
  public static void trmSendFile () {
    if (trmSendDialog == null) {
      ActionListener listener = new ActionListener () {
        @Override public void actionPerformed (ActionEvent ae) {
          switch (ae.getActionCommand ()) {
          case JFileChooser.APPROVE_SELECTION:
          case "Send":  //送信
            trmSendDialog.setVisible (false);
            File file = trmSendFileChooser.getSelectedFile ();
            if (file != null) {
              new SimpleTask (XEiJ.tmrScheduler, new SendTask (file)).fixedDelay (0L, 1000L);
            }
            break;
          case JFileChooser.CANCEL_SELECTION:
          case "Cancel":  //キャンセル
            trmSendDialog.setVisible (false);
            break;
          }  //switch
        }  //actionPerformed
      };  //ActionListener
      trmSendFileChooser = new JFileChooser (new File ("a.txt"));
      trmSendFileChooser.setMultiSelectionEnabled (false);  //複数選択不可
      trmSendFileChooser.setControlButtonsAreShown (false);  //デフォルトのボタンを消す
      trmSendFileChooser.addActionListener (listener);
      trmSendDialog =
        Multilingual.mlnTitle (
          ComponentFactory.createModalDialog (
            trmFrame,
            "Send file",
            ComponentFactory.createBorderPanel (
              0, 0,
              ComponentFactory.createVerticalBox (
                trmSendFileChooser,
                ComponentFactory.createHorizontalBox (
                  Box.createHorizontalStrut (12),
                  Box.createHorizontalGlue (),
                  Multilingual.mlnText (
                    ComponentFactory.createLabel (
                      "Prepare COMMAND.X with CTTY AUX"),
                    "ja", "COMMAND.X を CTTY AUX で準備して"),
                  Box.createHorizontalStrut (12),
                  Multilingual.mlnText (
                    ComponentFactory.createButton ("Send", KeyEvent.VK_S, listener),
                    "ja", "送信"),
                  Box.createHorizontalStrut (12),
                  Multilingual.mlnText (
                    ComponentFactory.createButton ("Cancel", KeyEvent.VK_C, listener),
                    "ja", "キャンセル"),
                  Box.createHorizontalStrut (12)
                  ),  //createHorizontalBox
                Box.createVerticalStrut (12)
                )  //createVerticalBox
              )  //createBorderPanel
            ),  //createModalDialog
          "ja", "ファイル送信");  //mlnTitle
    }  //if
    XEiJ.pnlExitFullScreen (true);
    trmSendDialog.setVisible (true);
  }  //trmSendFile

  //buf = load (file)
  //  ファイルを読み込む
  static byte[] load (File file) {
    if (!file.isFile ()) {
      return null;
    }
    int len = (int) file.length ();
    if (len == 0) {
      return null;
    }
    byte[] buf = new byte[len];
    try (BufferedInputStream bis = new BufferedInputStream (new FileInputStream (file))) {
      if (bis.read (buf) != len) {
        return null;
      }
    } catch (IOException ioe) {
      return null;
    }
    return buf;
  }  //load

  //class BitWriter
  //  ビットライタ
  static class BitWriter {

    byte[] buffer;  //バッファ
    int byteIndex;  //バイト書き込み位置=現在の長さ
    int bitIndex;  //ビット書き込み位置
    int bitCount;  //[bitIndex]の残りビット数。上位から書く。下位bitCountビットが空いている

    //new BitWriter ()
    //  コンストラクタ
    BitWriter () {
      buffer = new byte[16];
      byteIndex = 0;
      bitIndex = -1;
      bitCount = 0;
    }  //BitWriter

    //writeByte (data)
    //  バイト書き込み
    //  data  データ。下位8ビットだけ使う
    void writeByte (int data) {
      if (byteIndex == buffer.length) {  //bufferが満杯なので長さを2倍にする
        byte[] temporary = new byte[byteIndex * 2];
        System.arraycopy (buffer, 0,  //from
                          temporary, 0,  //to
                          byteIndex);  //length
        buffer = temporary;
      }
      buffer[byteIndex++] = (byte) data;
    }  //writeByte

    //writeBits (width, data)
    //  ビット書き込み
    //  width  ビット数。0～32
    //  data   データ。下位widthビットだけ使う
    void writeBits (int width, int data) {
      while (width != 0) {  //書き込む残りビット数
        if (bitCount == 0) {  //[bitIndex]が満杯なので新しい[bitIndex]を用意する
          if (byteIndex == buffer.length) {  //bufferが満杯なので長さを2倍にする
            byte[] temporary = new byte[byteIndex * 2];
            System.arraycopy (buffer, 0,  //from
                              temporary, 0,  //to
                              byteIndex);  //length
            buffer = temporary;
          }
          bitIndex = byteIndex;
          bitCount = 8;
          buffer[byteIndex++] = 0;
        }
        data &= (1 << width) - 1;  //dataのゴミを消す
        int n = Math.min (bitCount, width);  //今回書き込むビット数
        bitCount -= n;  //今回書き込んだ後の[bitIndex]の残りビット数
        width -= n;  //今回書き込んだ後の書き込む残りビット数
        buffer[bitIndex] |= (byte) ((data >>> width) << bitCount);  //符号なし右シフト
      }  //while
    }  //writeBits

    //getBuffer ()
    //  バッファを返す
    byte[] getBuffer () {
      return buffer;
    }  //getBuffer

    //getLength ()
    //  現在の長さを返す
    int getLength () {
      return byteIndex;
    }  //getLength

  }  //class BitWriter

  static class DIC {
    int ptr;  //開始位置
    int len;  //長さ
  }

  //outbuf = compress (inpbuf, dicbit)
  //  圧縮
  //  inpbuf  入力バッファ
  //  dicbit  辞書のページ数のビット数。1～15
  static byte[] compress (byte[] inpbuf, int dicbit) {
    int dicsiz = 1 << dicbit;  //辞書のページ数
    //辞書を初期化する
    DIC[] dicbuf = new DIC[dicsiz];  //辞書
    for (int pag = 0; pag < dicsiz; pag++) {
      dicbuf[pag] = new DIC ();
      dicbuf[pag].ptr = 0;
      dicbuf[pag].len = 0;
    }
    int dicent = 0;  //辞書エントリ
    //入力長さ
    int inplen = inpbuf.length;
    BitWriter bw = new BitWriter ();
    bw.writeBits (24, inplen);
    //辞書のページ数のビット数
    bw.writeBits (4, dicbit);
    //圧縮ループ
    int inpptr = 0;  //入力位置
    while (inpptr < inplen) {
      //辞書から探す
      int dicpag = -1;  //辞書にある単語のページ番号
      int dicptr = -1;  //辞書にある単語の開始位置
      int diclen = 0;  //辞書にある単語の長さ
      int fstchr = inpbuf[inpptr] & 255;  //1文字目
      for (int pag = 0; pag < dicsiz; pag++) {
        int len = dicbuf[pag].len;  //辞書にある単語の長さ
        if (diclen < len &&  //これまでより長い
            inpptr + len + 1 <= inplen) {  //1文字伸ばしてもはみ出さない
          int ptr = dicbuf[pag].ptr;  //辞書にある単語の開始位置
        cmp:
          if (fstchr == (inpbuf[ptr] & 255)) {
            for (int i = 1; i < len; i++) {
              if (inpbuf[inpptr + i] != inpbuf[ptr + i]) {
                break cmp;
              }
            }
            dicpag = pag;
            dicptr = ptr;
            diclen = len;
          }
        }
      }  //for pag
      if (diclen == 0) {  //辞書にない
        bw.writeBits (1, 0);
      } else {  //辞書にある
        bw.writeBits (1, 1);
        bw.writeBits (dicbit, dicpag);
      }
      int chr = inpbuf[inpptr + diclen] & 255;  //今回の文字
      //文字を出力する
      bw.writeByte (chr);
      //1文字伸ばす
      diclen++;
      //新しい単語を辞書に登録する
      dicbuf[dicent].ptr = inpptr;
      dicbuf[dicent].len = diclen;
      dicent++;
      if (dicent == dicsiz) {
        dicent = 0;
      }
      //次の文字へ
      inpptr += diclen;
    }  //while
    //出力バッファを返す
    byte[] outbuf = bw.getBuffer ();
    int outlen = bw.getLength ();
    if (outbuf.length != outlen) {
      byte[] tmpbuf = new byte[outlen];
      System.arraycopy (outbuf, 0, tmpbuf, 0, outlen);
      outbuf = tmpbuf;
    }
    return outbuf;
  }  //compress

  //dst = encode (src)
  //  エンコード
  //    Pxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
  //    Qxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
  //    Rxxxxxxxxxxxxxxx
  //      ↓
  //    0xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
  //    0xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
  //    0xxxxxxxxxxxxxxxPQR0000000000000
  public static byte[] encode (byte[] src) {
    int r = src.length;
    int q = r / 62;
    r -= 62 * q;
    if (r != 0) {  //62で割り切れないとき
      r = ((r + 2 + 3) & -4) - 2;  //余りを4*n+2に切り上げる
      if (src.length < 62 * q + r) {
        byte[] tmp = new byte[62 * q + r];
        System.arraycopy (src, 0, tmp, 0, src.length);
        src = tmp;
      }
    }
    byte[] dst = new byte[64 * q + (r == 0 ? 0 : r + 2)];
    int[] w = new int[16];
    for (int i = 0; 64 * i < dst.length; i++) {  //ブロックの番号
      int n = Math.min (64, dst.length - 64 * i) / 4;  //ブロックに含まれるintの数
      //intを集める
      for (int k = 0; k < n - 1; k++) {
        w[k] = ((src[62 * i + 4 * k + 0] & 255) << 24 |
                (src[62 * i + 4 * k + 1] & 255) << 16 |
                (src[62 * i + 4 * k + 2] & 255) << 8 |
                (src[62 * i + 4 * k + 3] & 255));
      }
      w[n - 1] = ((src[62 * i + 4 * (n - 1) + 0] & 255) << 24 |
                  (src[62 * i + 4 * (n - 1) + 1] & 255) << 16);
      //intの最上位ビットを最後のintの下位2バイトに移して31ビット整数にする
      for (int k = 0; k < n; k++) {
        if ((w[k] & 0x80000000) != 0) {
          w[k] &= 0x7fffffff;
          w[n - 1] |= 1 << (15 - k);
        }
      }
      //31ビット整数を4桁の224進数に変換して各桁に32を加えて出力する
      for (int k = 0; k < n; k++) {
        int t3 = w[k];
        int t2 = t3 / 224;
        t3 -= 224 * t2;
        int t1 = t2 / 224;
        t2 -= 224 * t1;
        int t0 = t1 / 224;
        t1 -= 224 * t0;
        dst[64 * i + 4 * k + 0] = (byte) (32 + t0);
        dst[64 * i + 4 * k + 1] = (byte) (32 + t1);
        dst[64 * i + 4 * k + 2] = (byte) (32 + t2);
        dst[64 * i + 4 * k + 3] = (byte) (32 + t3);
      }
    }
    return dst;
  }  //encode



  //送信処理
  //  ファイルをa.rに変換する
  //    a.rは実行ファイルだが終端の$1A以外に$00～$1Fを含まない
  //  a.rをcopy aux a.rとa.rで挟んで送信する
  //  X68000でa.rが作られて実行されてファイルが復元される
  static class SendTask implements SimpleTask.Runner {
    File file;
    byte[] step1Data;
    SendTask (File file) {
      this.file = file;
    }
    @Override public int runN (int n) {
      //================================
      if (n == 1) {
        //ファイルを読み込む
        byte[] fileData = load (file);  //本体
        if (fileData == null) {
          System.out.println ("file not found");
          return 0;
        }
        if (0x00ffff00 < fileData.length) {
          System.out.println ("file size error");
          return 0;
        }
        //------------------------------------------------
        //step3_data
        //byte[] adotr3 = load ("adotr3.r");
        byte[] step3Data = new byte[adotr3.length +  //adotr3
                                    24 +  //ファイル名
                                    4 +  //日時
                                    4 +  //本体のCRC32
                                    4 +  //本体の長さ
                                    fileData.length];  //本体
        int index3 = 0;
        //adotr3
        System.arraycopy (adotr3, 0,
                          step3Data, index3,
                          adotr3.length);
        index3 += adotr3.length;
        //ファイル名
        //  ・SJISに変換できない
        //  ・SJISに変換できたが、Human68kの標準のファイル名に使えない文字がある
        //  ・SJISに変換できたが、/^[^\.]{1,18}(?:\.[^\.]{1,3})?$/にマッチしない
        //  はすべてエラー
        String name = file.getName ();
        {
          byte[] b = new byte[2 * name.length ()];
          int k = 0;
          int p = -1;
          for (int i = 0; i < name.length (); i++) {
            int c = CharacterCode.chrCharToSJIS[name.charAt (i)];
            if (c == 0) {  //変換できない文字がある
              System.out.println ("file name error");
              return 0;
            }
            if (c <= ' ' || c == ':' || c == '*' || c == '?' ||
                c == '\\' || c == '/' || (c == '-' && k == 0) ||
                c == '"' || c == '\'' || c == '+' || c == ';' ||
                c == '<' || c == '=' || c == '>' ||
                c == '[' || c == ']' || c == '|') {  //Human68kの標準のファイル名に使えない文字がある
              System.out.println ("file name error");
              return 0;
            }
            if (c == '.') {  //'.'
              if (p < 0) {  //初回
                p = k;
              } else {  //'.'が2つある
                System.out.println ("file name error");
                return 0;
              }
            }
            if (0x00ff < c) {  //2バイト
              b[k++] = (byte) (c >> 8);
            }
            b[k++] = (byte) c;
            if (p < 0 ? 18 < k : p + 1 + 3 < k) {  //18+3文字に収まっていない
              System.out.println ("file name error");
              return 0;
            }
          }
          for (int i = 0; i < k; i++) {
            step3Data[index3++] = b[i];
          }
          for (int i = k; i < 24; i++) {
            step3Data[index3++] = 0;
          }
        }
        //日時
        {
          long dttm = DnT.dntDttmCmil (file.lastModified () + RP5C15.rtcCmilGap);  //西暦年<<42|月<<38|月通日<<32|時<<22|分<<16|秒<<10|ミリ秒。FCBの日時はRTCの日時なのでオフセットを加える
          int date = DnT.dntYearDttm (dttm) - 1980 << 9 | DnT.dntMontDttm (dttm) << 5 | DnT.dntMdayDttm (dttm);  //(西暦年-1980)<<9|月<<5|月通日
          int time = DnT.dntHourDttm (dttm) << 11 | DnT.dntMinuDttm (dttm) << 5 | DnT.dntSecoDttm (dttm) >> 1;  //時<<11|分<<5|秒/2
          step3Data[index3++] = (byte) (date >> 8);
          step3Data[index3++] = (byte) date;
          step3Data[index3++] = (byte) (time >> 8);
          step3Data[index3++] = (byte) time;
        }
        //本体のCRC32
        {
          CRC32 crc32 = new CRC32 ();
          crc32.update (fileData, 0, fileData.length);
          int t = (int) crc32.getValue ();
          step3Data[index3++] = (byte) (t >> 24);
          step3Data[index3++] = (byte) (t >> 16);
          step3Data[index3++] = (byte) (t >> 8);
          step3Data[index3++] = (byte) (t);
        }
        //本体の長さ
        {
          int t = fileData.length;
          step3Data[index3++] = (byte) (t >> 24);
          step3Data[index3++] = (byte) (t >> 16);
          step3Data[index3++] = (byte) (t >> 8);
          step3Data[index3++] = (byte) (t);
        }
        //本体
        System.arraycopy (fileData, 0,
                          step3Data, index3,
                          fileData.length);
        index3 += fileData.length;
        //------------------------------------------------
        //step2_data
        //byte[] adotr2 = load ("adotr2.r");
        byte[] step3DataCompressed = null;
        int dicbit = 0;
        //辞書のサイズのビット数
        //  圧縮しにくいときは小さい方がよいが縮まらず捨てられるビット数を試しても意味がない
        //  圧縮しやすいときは大きい方がよいが圧縮に時間がかかる
        //  9に固定してもよい
        for (int i = 8; i <= 10; i++) {
          byte[] t = compress (step3Data, i);
          if (step3DataCompressed == null ||
              t.length < step3DataCompressed.length) {
            step3DataCompressed = t;
            dicbit = i;
          }
        }
        byte[] step2Data = new byte[adotr2.length + step3DataCompressed.length];
        int index2 = 0;
        //adotr2
        System.arraycopy (adotr2, 0,
                          step2Data, index2,
                          adotr2.length);
        index2 += adotr2.length;
        //本体
        System.arraycopy (step3DataCompressed, 0,
                          step2Data, index2,
                          step3DataCompressed.length);
        index2 += step3DataCompressed.length;
        //------------------------------------------------
        //step1_data
        //byte[] adotr1 = load ("adotr1.r");
        byte[] step3DataEncoded = encode (step3Data);  //圧縮なし
        byte[] step2DataEncoded = encode (step2Data);  //圧縮あり
        //byte[] step1Data;
        if (step3DataEncoded.length <= step2DataEncoded.length) {  //圧縮効果なし
          step1Data = new byte[adotr1.length + step3DataEncoded.length + 1];
          int index1 = 0;
          System.arraycopy (adotr1, 0,
                            step1Data, index1,
                            adotr1.length);
          index1 += adotr1.length;
          System.arraycopy (step3DataEncoded, 0,
                            step1Data, index1,
                            step3DataEncoded.length);
          index1 += step3DataEncoded.length;
          step1Data[index1++] = 0x1a;
          if (false) {
            System.out.printf ("                 original: %d bytes (%.1f%%)\n",
                               fileData.length,
                               100.0);
            System.out.printf ("   generator concatenated: %d bytes (%.1f%%)\n",
                               step3Data.length,
                               100.0 * (double) step3Data.length / (double) fileData.length);
            System.out.printf ("                  encoded: %d bytes (%.1f%%)\n",
                               step3DataEncoded.length,
                               100.0 * (double) step3DataEncoded.length / (double) fileData.length);
            System.out.printf ("     decoder concatenated: %d bytes (%.1f%%)\n",
                               step1Data.length,
                               100.0 * (double) step1Data.length / (double) fileData.length);
          }
        } else {  //圧縮効果あり
          step1Data = new byte[adotr1.length + step2DataEncoded.length + 1];
          int index1 = 0;
          System.arraycopy (adotr1, 0,
                            step1Data, index1,
                            adotr1.length);
          index1 += adotr1.length;
          System.arraycopy (step2DataEncoded, 0,
                            step1Data, index1,
                            step2DataEncoded.length);
          index1 += step2DataEncoded.length;
          step1Data[index1++] = 0x1a;
          if (false) {
            System.out.printf ("                 original: %d bytes (%.1f%%)\n",
                               fileData.length,
                               100.0);
            System.out.printf ("   generator concatenated: %d bytes (%.1f%%)\n",
                               step3Data.length,
                               100.0 * (double) step3Data.length / (double) fileData.length);
            System.out.printf ("               compressed: %d bytes (%.1f%%) (%d bits dictionary)\n",
                               step3DataCompressed.length,
                               100.0 * (double) step3DataCompressed.length / (double) fileData.length,
                               dicbit);
            System.out.printf ("decompressor concatenated: %d bytes (%.1f%%)\n",
                               step2Data.length,
                               100.0 * (double) step2Data.length / (double) fileData.length);
            System.out.printf ("                  encoded: %d bytes (%.1f%%)\n",
                               step2DataEncoded.length,
                               100.0 * (double) step2DataEncoded.length / (double) fileData.length);
            System.out.printf ("     decoder concatenated: %d bytes (%.1f%%)\n",
                               step1Data.length,
                               100.0 * (double) step1Data.length / (double) fileData.length);
          }
        }
        if (false) {
          int l = step1Data.length;
          System.out.printf ("step1Data %d bytes\n", l);
          for (int i = 0; i < l; i++) {
            if ((i & 15) == 0) {
              System.out.printf ("%08x", i);
            }
            System.out.printf (" %02x", step1Data[i] & 255);
            if ((i & 15) == 15) {
              System.out.println ();
            }
          }
          if ((l & 15) != 0) {
            System.out.println ();
          }
        }
        //copy aux a.rとa.rで挟んで送信する
        trmSendString (String.format (
          "rem a.r/%s %d/%d %.1f%%\rcopy aux a.r\r",
          name, step1Data.length, fileData.length,
          100.0 * (double) step1Data.length / (double) fileData.length));
        return 2;
      }
      //================================
      if (n == 2) {
        if (trmRowToCol[0] == 1) {  //Terminal→AUX。row2col
          int row = 0;
          int col = 1;
          trmConnectionArray[trmCols * row + col - 1].row2colQueue.write (step1Data, 0, step1Data.length);
        } else if (0 < trmColToRow[0]) {  //Terminal→SerialPort。col2row
          int row = trmColToRow[0];
          int col = 0;
          trmConnectionArray[trmCols * row + col - 1].col2rowQueue.write (step1Data, 0, step1Data.length);
        }
        return 3;
      }
      //================================
      if (n == 3) {
        trmSendString ("a.r\r");
        System.out.println ("sending file completed");
        return 0;
      }
      //================================
      //if (n == 0) {  //後始末
      //}
      return 0;
    }  //runN
  }  //class SendTask



  //perl misc/ftob.pl adotr1 misc/adotr1.r
  public static final byte[] adotr1 = "B\202G\373 h\324\211\304|\377\374(B\"Kx@\330\213B\200\320\214\220\204\223\201\300\201\330\200$I\"<\337\337\337\340\322\233B\200v( @\347\210\220\210\353\210\341\231\320\201Q\301\220\201\342Kd\354\"\300\267\304e\3322!B\200\322A\342\220\321\232\265\311e\364\267\314e\266A\372\377\370t\370N\360\" BAVAp\254NO".getBytes (XEiJ.ISO_8859_1);
  //perl misc/ftob.pl adotr2 misc/adotr2.r
  public static final byte[] adotr2 = "p\1\320\211\300|\377\376\"@|\0~\0E\372\0\372r\30a\0\0\322&\0\326\211p\1\320\203\300|\377\376*@r\4a\0\0\276g\0\0\246\30\0z\b\351\255\332\215\377\201\"\5\222\200/\1/\0\377JP\217J\200ktp\0 M \300 \300\261\305e\370(I,M\265\311dvr\1a\0\0\206f\b,\314p\1,\300`0\22\4av\347\210 u\b\0 5\b\4gVR\200\"\0\322\214\262\203bL,\314,\300U\200H@H@\30\330Q\310\377\374H@Q\310\377\364\30\332\275\305f\2,M\271\303e\260p\372N\373\2\16r\3p\254NO IN\320\377\t\377\0Hz\0\4`\366out of memory\r\n\0Hz\0\4`\340data error\r\n\0\0p\0J\7f\4\34\32P\7\24\1\264\7c\2\24\7\20\6\345\250\345.\236\2\222\2f\344\340\210Nu".getBytes (XEiJ.ISO_8859_1);
  //perl misc/ftob.pl adotr3 misc/adotr3.r
  public static final byte[] adotr3 = "E\372\1\2&*\0 G\352\0$ \13\320\203R\200\300|\377\376(@K\354\4\0\377\201\"\r\222\200/\1/\0\377JP\217J\200k\0\0\230 Lr\0 \1t\7\342\210d\6\n\200\355\270\203 Q\312\377\364 \300R\1d\350\"Lp\0\"\3 KF\200`\22HAt\0\24\30\261\2\340\210\345J$1(\0\265\200Q\311\377\356HAQ\311\377\346F\200\260\252\0\34f`?<\0 /\n\377<\\\217J\200kd/\3/\13?\0\377@\"\0/j\0\30\0\2\377\207\377>O\357\0\n\262\203g\b/\n\377AX\217`@/\n\377\tHz\0\6\377\t\377\0 created\r\n\0\0Hz\0\4`\352out of memory\r\n\0Hz\0\4`\324crc error\r\n\0Hz\0\4`\302cannot write\r\n\0\0".getBytes (XEiJ.ISO_8859_1);

}  //class RS232CTerminal
