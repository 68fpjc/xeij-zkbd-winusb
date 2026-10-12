//========================================================================================
//  HFS.java
//    en:Host file system interface -- It makes an arbitrary directory of the host machine into the boot drive of the Human68k.
//    ja:ホストファイルシステムインタフェイス -- ホストマシンの任意のディレクトリをHuman68kの起動ドライブにします。
//  Copyright (C) 2003-2026 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//  0x00e9f020  HFS ホストファイルシステムインタフェイス
//
//  種類
//    仮想拡張ボード
//
//  機能
//    ホストマシンの任意のディレクトリをHuman68kのリモートデバイスにする
//    ホストマシンの任意のディレクトリからHuman68kを起動する
//
//  組み込み
//    SWITCH BOOT=ROM$E9F020
//
//----------------------------------------------------------------------------------------

package xeij;

import java.awt.event.*;  //ActionEvent,ActionListener,ComponentAdapter,ComponentEvent,ComponentListener,FocusAdapter,FocusEvent,FocusListener,InputEvent,KeyAdapter,KeyEvent,KeyListener,MouseAdapter,MouseEvent,MouseListener,MouseMotionAdapter,MouseWheelEvent,WindowAdapter,WindowEvent,WindowListener,WindowStateListener
import java.io.*;  //BufferedInputStream,BufferedOutputStream,BufferedReader,BufferedWriter,File,FileInputStream,FileNotFoundException,FileReader,InputStream,InputStreamReader,IOException,OutputStreamWriter,RandomAccessFile
import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,InterruptedException,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,Timer,TimerTask,TreeMap
import javax.swing.*;  //AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JButton,JCheckBox,JCheckBoxMenuItem,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException

public class HFS {

  public static final boolean HFS_DEBUG_TRACE = false;
  public static final boolean HFS_DEBUG_FILE_INFO = false;

  public static final boolean HFS_REPORT_INCOMPATIBLE_COMMAND = false;  //非対応のコマンドを報告する

  //コマンドトレース
  public static final boolean HFS_COMMAND_TRACE = false;
  public static boolean hfsCommandTraceOn;

  //スレッド
  public static final boolean HFS_USE_THREAD = true;  //true=ホストマシンのファイルを操作するスレッドをコアから分離する
  public static final long HFS_THREAD_DELAY = 0L;
  public static java.util.Timer hfsTimer;  //Timerだけだとjavax.swing.Timerと紛らわしい
  //  状態
  //
  //                   Call
  //    IDLE  →  X68K  →  DONE  →  IDLE
  //
  //                   Call                Host
  //    IDLE  →  X68K  →  HOST  →  BUSY  →  DONE  →  IDLE
  //
  //                   Call                Host      X68k
  //    IDLE  →  X68K  →  HOST  →  BUSY  →  X68K  →  DONE  →  IDLE
  //
  //                   Call                Host      X68k                Host
  //    IDLE  →  X68K  →  HOST  →  BUSY  →  X68K  →  HOST  →  BUSY  →  DONE  →  IDLE
  //
  //                   Call                Host      X68k                Host      X68k
  //    IDLE  →  X68K  →  HOST  →  BUSY  →  X68K  →  HOST  →  BUSY  →  X68K  →  DONE  →  IDLE
  //
  //  Call,X68k
  //    X68000側の処理。X68Kで呼び出され、HOSTまたはDONEで終了する
  //  Host
  //    ホスト側の処理。BUSYで呼び出され、X68KまたはDONEで終了する
  //
  //  複数のデバイスコマンドが並列に動作することはない
  //  BUSYのとき割り込みを受け付けるがディスクアクセス中はスーパーバイザモードなのでTimer-DでHuman68kのスレッドが切り替わることはない
  //
  //  DOSコールのパラメータに存在しないアドレスを指定するとX68000側の処理中にバスエラーが発生する可能性がある
  //  バスエラーが発生したときはhfsState=HFS_STATE_IDLEとして強制的に次のコマンドを受け付けられるようにする
  //  X68000側の処理中はホスト側のタスクは存在しないか終了直前にhfsStateを書き換えた後なのでhfsStateの書き換えが衝突することはない
  //
  public static final int HFS_STATE_IDLE = 0;  //何もしていない
  public static final int HFS_STATE_X68K = 1;  //X68000側の処理中
  public static final int HFS_STATE_HOST = 2;  //ホスト側のタスクの起動中
  public static final int HFS_STATE_BUSY = 3;  //ホスト側の処理中
  public static final int HFS_STATE_DONE = 4;  //コマンド終了
  public static int hfsState;  //状態

  //先読み・遅延書き込みバッファ
  public static final boolean HFS_BUFFER_TRACE = false;
  public static final int HFS_BUFFER_SIZE = 1024 * 64;  //先読み・遅延書き込みバッファのサイズ
  //バッファ-メモリ間で一度に転送する長さの上限
  //  512以上にすると68060でウエイトサイクルありでPCM8A.Xで再生しながらVRAMに直接読み込むと音がおかしくなる
  //  原発振周波数を16MHzにしてPCM8A.X -M1 -F2にしたら256でも駄目だった
  //  小さくしすぎると転送が遅くなる
  public static final int HFS_BUFFER_STEP = 128;

  //ROM
  //    +0   l  +20      IPL起動ハンドル
  //                       IPL起動ルーチンのアドレス
  //                       IPL起動ハンドルのアドレスでROM起動するとIPLROMがIPL起動ルーチンを呼び出してくれる
  //                       IPL起動ハンドルよりもIPL起動ルーチンのほうが後ろ(大きいアドレス)に配置されていなければならない
  //                       IPLROMはチェックしていないがHuman68kがこの条件でIPL起動ハンドルの有効性を確認している
  //                       (RAMディスクドライバでは使用しない)
  //    +4   l  +24      デバイスドライバ組み込みハンドル
  //                       デバイスドライバ組み込みルーチンのアドレス
  //                       Human68kが(IPL起動ルーチンのアドレス-16).lから取り出す
  //    +8   l  0        デバイスドライバ組み込みパラメータ
  //                       デバイスドライバ組み込みルーチンを呼び出すときにd0に入れておく値
  //                       Human68kがIPL起動ルーチンのアドレス-12から取り出す
  //                       SCSIボードの場合はサービスルーチン(IOCS _SCSIDRV)のベクタが入っている
  //                       (RAMディスクドライバでは使用しない)
  //    +12  l  'Huma'   Human68kマジック
  //         l  'n68k'     'Human68k'
  //                       Human68kが(IPL起動ルーチンのアドレス-8).l[2]を見てデバイスドライバ組み込みルーチンの存在を確認している
  //    +20  w  HFSBOOT    IPL起動ルーチン
  //         w  RTS        ROM起動なので先頭は0x60でなくてよい(RAM起動のときは0x60で始まっていなければならない)
  //                       (RAMディスクドライバでは使用しない)
  //    +24  w  HFSINST  デバイスドライバ組み込みルーチン
  //         w  RTS
  //    +28  l  'JHFS'   HFSマジック
  //    +32
  public static final int HFS_ADDRESS            = 0x00e9f020;
  public static final int HFS_BOOT_HANDLE        = HFS_ADDRESS + 0;  //IPL起動ハンドル
  public static final int HFS_INSTALL_HANDLE     = HFS_ADDRESS + 4;  //デバイスドライバ組み込みハンドル
  public static final int HFS_INSTALL_PARAMETER  = HFS_ADDRESS + 8;  //デバイスドライバ組み込みパラメータ
  public static final int HFS_HUMAN68K_MAGIC     = HFS_ADDRESS + 12;  //Human68kマジック
  public static final int HFS_BOOT_ROUTINE       = HFS_ADDRESS + 20;  //IPL起動ルーチン
  public static final int HFS_INSTALL_ROUTINE    = HFS_ADDRESS + 24;  //デバイスドライバ組み込みルーチン
  public static final int HFS_MAGIC              = HFS_ADDRESS + 28;  //HFSマジック
  public static final int HFS_ROM_SIZE           = 32;  //ROMサイズ

  //デバイスドライバ
  //    +0   l  -1       ネクストデバイスドライバハンドル
  //    +4   w  0x0000   デバイスタイプ
  //    +6   l  +26      ストラテジハンドル
  //    +10  l  +24      インタラプトハンドル
  //    +14  l  '\1HFS'  デバイス名
  //         l  '    '
  //    +22  w  0        ドライブ番号
  //    +24  w  HFSSTR   ストラテジルーチン
  //    +26  w  RTS
  //    +28  w  HFSINT   インタラプトルーチン
  //    +30  w  RTS
  //    +32
  public static final int HFS_NEXT_DEVICE        = 0;  //ネクストデバイスドライバハンドル
  public static final int HFS_DEVICE_TYPE        = 4;  //デバイスタイプ
  public static final int HFS_STRATEGY_HANDLE    = 6;  //ストラテジハンドル
  public static final int HFS_INTERRUPT_HANDLE   = 10;  //インタラプトハンドル
  public static final int HFS_DEVICE_NAME        = 14;  //デバイス名
  public static final int HFS_DRIVE_NUMBER       = 22;  //ドライブ番号
  public static final int HFS_STRATEGY_ROUTINE   = 24;  //ストラテジルーチン
  public static final int HFS_INTERRUPT_ROUTINE  = 28;  //インタラプトルーチン
  public static final int HFS_DEVICE_SIZE        = 32;  //デバイスサイズ

  //ユニット
  public static final int HFS_MIN_UNITS = 1;  //最小ユニット数
  public static final int HFS_MAX_UNITS = 16;  //最大ユニット数
  public static final String HFS_DUMMY_UNIT_NAME = "*HFS*";
  public static final HFUnit[] hfsUnitArray = new HFUnit[HFS_MAX_UNITS];  //ユニットの配列
  public static int hfsBootUnit;  //起動ユニット番号
  public static final int[] hfsDeviceUnitArray = new int[HFS_MAX_UNITS];  //ユニット番号の変換表。リクエストヘッダのユニット番号→本来のユニット番号
  public static int hfsDeviceUnitCount;  //起動時に接続されていたユニットの数

  //メニュー
  public static JMenu hfsMenu;

  //ファイルフィルタ
  public static javax.swing.filechooser.FileFilter hfsFileFilter;  //java.io.FileFilterと紛らわしい

  //開くダイアログ
  public static OpenDialog hfsOpenDialog;  //開くダイアログ。null=作る前
  public static int hfsOpenUnit;  //開くユニットの番号
  public static ArrayList<File[]> hfsOpenHistory;  //作る前に追加されたヒストリ

  //デバイスドライバ
  public static int hfsDeviceHeader;  //デバイスヘッダのアドレス
  public static int hfsRequestHeader;  //a5  実行中のコマンドのリクエストヘッダのアドレス
  public static int hfsRequest1Number;  //<(a5+1).b:リクエストヘッダのユニット番号
  public static int hfsRequest2Command;  //<(a5+2).b:コマンドコード
  public static int hfsRequest13Mode;  //<(a5+13).b:モードなど
  public static int hfsRequest14Namests;  //<(a5+14).l:_NAMESTS形式のファイル名など
  public static int hfsRequest18Param;  //<(a5+18).l:追加のパラメータ
  public static int hfsRequest22Fcb;  //<(a5+22).l:FCBテーブルのアドレスなど
  public static int hfsRequest3Error;  //>(a5+3).w:エラーコード
  public static int hfsRequest18Result;  //>(a5+18).l:リザルトステータス
  public static HFUnit hfsRequestUnit;  //コマンドを実行するユニット

  //TwentyOne.x
  //!!! 工事中
  public static final boolean HFS_USE_TWENTY_ONE = false;  //true=TwentyOne.xのオプションによって動作を変更する
  public static final int HFS_TW_VERBOSE_MODE         = 1 << 31;  //+V バーボーズモード
  public static final int HFS_TW_CASE_SENSITIVE       = 1 << 30;  //+C 大文字と小文字を区別する
  public static final int HFS_TW_SPECIAL_CHARACTER    = 1 << 29;  //+S 特殊文字が使える
  public static final int HFS_TW_MULTI_PERIOD         = 1 << 28;  //+P ピリオドが複数使える
  public static final int HFS_TW_NOT_TWENTY_ONE       = 1 << 27;  //-T 21バイト比較しない
  public static final int HFS_TW_DISABLE_PRINTER_ECHO = 1 << 26;  //+D プリンタエコーを無効化する
  public static final int HFS_TW_USE_SYSROOT          = 1 << 25;  //+R $SYSROOTを使う
  public static final int HFS_TW_WARN_CASE_MISMATCH   = 1 << 24;  //+W +Cのとき大文字と小文字だけが違う名前を警告する
  public static final int HFS_TW_USE_STRONG_SYSROOT   = 1 << 23;  //+r $SYSROOTを使う。'\\'で始まる名前でも使う
  //                                                    bit22-16       予約
  //                                                    bit15-0     -B バッファ数
  public static int hfsTwentyOneOption;  //TwentyOne.xのオプション

  //UTF-8警告
  public static final boolean HFS_UTF8_WARNING = true;
  public static boolean hfsUTF8WarningOn;  //HFSで開いたファイルがUTF-8のとき警告する
  public static HashSet<String> hfsUTF8WarningSet;  //警告したファイル名のセット



  //hfsInit ()
  //  ホストファイルシステムインタフェイスを初期化する
  public static void hfsInit () {

    //コマンドトレース
    if (HFS_COMMAND_TRACE) {
      hfsCommandTraceOn = false;
    }

    //スレッド
    if (HFS_USE_THREAD) {
      hfsTimer = new java.util.Timer ();  //Timerだけだとjavax.swing.Timerと紛らわしい
    }

    //TwentyOne.x
    if (HFS_USE_TWENTY_ONE) {
      hfsTwentyOneOption = 0;
    }

    //ファイルフィルタ
    //  ファイルチューザーとドロップターゲットで使う
    hfsFileFilter = new javax.swing.filechooser.FileFilter () {  //java.io.FileFilterと紛らわしい
      @Override public boolean accept (File file) {
        if (file.isDirectory ()) {
          return true;
        }
        String path = file.getPath ();
        if (hfsIsInserted (path)) {  //既に挿入されている
          return false;
        }
        return true;  //ファイルはあってもなくてもよい
      }
      @Override public String getDescription () {
        return (Multilingual.mlnJapanese ?
                "ルートになるディレクトリまたはそこにあるかも知れないファイル" :
                "Directory to be the root or files that may be found there");
      }
    };

    //開くダイアログ
    hfsOpenDialog = null;
    hfsOpenUnit = 0;
    hfsOpenHistory = new ArrayList<File[]> ();
    for (int i = JFileChooser2.MAXIMUM_HISTORY_COUNT - 1; 0 <= i; i--) {
      hfsAddHistory (JFileChooser2.pathsToFiles (Settings.sgsGetString ("hfhistory" + i)));
    }

    //ユニット
    //hfsUnitArray = new HFUnit[HFS_MAX_UNITS];
    hfsBootUnit = 0;
    //hfsDeviceUnitArray = new int[HFS_MAX_UNITS];
    hfsDeviceUnitCount = 0;
    for (int u = 0; u < HFS_MAX_UNITS; u++) {
      HFUnit unit = hfsUnitArray[u] = new HFUnit (u);
      if (u < HFS_MIN_UNITS) {
        unit.connect (false);  //ドライブ0は最初から接続されていて切り離せない
      }
    }

    //パラメータ
    for (int u = 0; u < HFS_MAX_UNITS; u++) {
      HFUnit unit = hfsUnitArray[u];
      String path = Settings.sgsGetString ("hf" + u);
      boolean userWriteProtect = false;
      if (path.toUpperCase ().endsWith (":R")) {  //書き込み禁止モードで開く
        path = path.substring (0, path.length () - 2);
        userWriteProtect = true;
      }
      boolean hostWriteProtect = !new File (path).canWrite ();
      if (path.length () != 0) {
        unit.connect (true);  //接続されていなければ接続する
        if (unit.insert (path,
                         userWriteProtect || hostWriteProtect)) {  //挿入できた
          hfsAddHistory (new File (path).getAbsoluteFile ());
        }
      }
    }
    if (HFS_UTF8_WARNING) {
      hfsUTF8WarningOn = Settings.sgsGetOnOff ("utf8warning");
      hfsUTF8WarningSet = new HashSet<String> ();
    }

    //HFSメニュー
    hfsMenu = ComponentFactory.createMenu ("HFS");  //横に長いとサブメニューを開きにくいので短くする
    ComponentFactory.addComponents (
      hfsMenu,
      ComponentFactory.createHorizontalBox (
        Multilingual.mlnText (ComponentFactory.createLabel ("Host file system"),
                              "ja", "ホストファイルシステム")),
      ComponentFactory.createHorizontalSeparator ()
      );
    for (HFUnit unit : hfsUnitArray) {
      hfsMenu.add (unit.getMenuBox ());
    }
    if (HFS_UTF8_WARNING) {
      ComponentFactory.addComponents (
        hfsMenu,
        ComponentFactory.createHorizontalSeparator (),
        Multilingual.mlnText (
          ComponentFactory.createCheckBoxMenuItem (hfsCommandTraceOn, "UTF-8 warning", new ActionListener () {
            @Override public void actionPerformed (ActionEvent ae) {
              hfsUTF8WarningOn = ((JCheckBoxMenuItem) ae.getSource ()).isSelected ();
            }
          }),
          "ja", "UTF-8 警告")
        );
    }
    if (HFS_COMMAND_TRACE) {
      ComponentFactory.addComponents (
        hfsMenu,
        ComponentFactory.createHorizontalSeparator (),
        Multilingual.mlnText (
          ComponentFactory.createCheckBoxMenuItem (hfsCommandTraceOn, "HFS command trace", new ActionListener () {
            @Override public void actionPerformed (ActionEvent ae) {
              hfsCommandTraceOn = ((JCheckBoxMenuItem) ae.getSource ()).isSelected ();
            }
          }),
          "ja", "HFS コマンドトレース")
        );
    }

    //ROM
    MainMemory.mmrWl (HFS_BOOT_HANDLE,        HFS_BOOT_ROUTINE);  //IPL起動ハンドル
    MainMemory.mmrWl (HFS_INSTALL_HANDLE,     HFS_INSTALL_ROUTINE);  //デバイスドライバ組み込みハンドル
    MainMemory.mmrWl (HFS_INSTALL_PARAMETER,  0);  //デバイスドライバ組み込みパラメータ
    MainMemory.mmrWl (HFS_HUMAN68K_MAGIC,     'H' << 24 | 'u' << 16 | 'm' << 8 | 'a');  //Human68kマジック
    MainMemory.mmrWl (HFS_HUMAN68K_MAGIC + 4, 'n' << 24 | '6' << 16 | '8' << 8 | 'k');
    MainMemory.mmrWl (HFS_BOOT_ROUTINE,       XEiJ.EMX_OPCODE_HFSBOOT << 16 | 0x4e75);  //IPL起動ルーチン
    MainMemory.mmrWl (HFS_INSTALL_ROUTINE,    XEiJ.EMX_OPCODE_HFSINST << 16 | 0x4e75);  //デバイスドライバ組み込みルーチン
    MainMemory.mmrWl (HFS_MAGIC,              'J' << 24 | 'H' << 16 | 'F' << 8 | 'S');  //HFSマジック

    hfsState = HFS_STATE_IDLE;

  }  //hfsInit()

  //hfsTini ()
  //  ホストファイルシステムインタフェイスの後始末
  //  開いたままのファイルがあれば閉じる
  //  スレッドを停止させる
  public static void hfsTini () {

    if (HFS_USE_THREAD) {
      if (hfsTimer != null) {
        hfsTimer.schedule (new TimerTask () {
          @Override public void run () {
            for (HFUnit unit : hfsUnitArray) {
              unit.hfuTini ();
            }
            hfsTimer.cancel ();
            hfsTimer = null;
          }
        }, HFS_THREAD_DELAY);
        try {
          while (hfsTimer != null) {
            Thread.sleep (10L);
          }
        } catch (InterruptedException ie) {
        }
      }
    } else {
      for (HFUnit unit : hfsUnitArray) {
        unit.hfuTini ();
      }
    }

    //開くダイアログ
    //  開くダイアログを作らなかったときはパラメータを更新しない
    if (hfsOpenDialog != null) {
      Settings.sgsPutOnOff ("hfreadonly", hfsOpenDialog.getReadOnly ());
      Settings.sgsPutOnOff ("hfappreboot", hfsOpenDialog.getReboot ());
      ArrayList<String> pathsList = hfsOpenDialog.getHistory ();
      int n = pathsList.size ();
      for (int i = 0; i < n; i++) {
        Settings.sgsPutString ("hfhistory" + i, pathsList.get (i));
      }
      for (int i = n; i < HFS_MAX_UNITS; i++) {
        Settings.sgsPutString ("hfhistory" + i, "");
      }
    }

    //ユニット
    for (int u = 0; u < HFS_MAX_UNITS; u++) {
      AbstractUnit unit = hfsUnitArray[u];
      Settings.sgsPutString (
        "hf" + u,
        unit.abuConnected && unit.abuInserted ?
        unit.abuWriteProtected ? unit.abuPath + ":R" : unit.abuPath :
        "");
    }

    if (HFS_UTF8_WARNING) {
      Settings.sgsPutOnOff ("utf8warning", hfsUTF8WarningOn);
    }

  }  //hfsTini()

  //inserted = hfsIsInserted (path)
  //  パスで指定したファイルが既に挿入されているか調べる
  public static boolean hfsIsInserted (String path) {
    for (HFUnit unit : hfsUnitArray) {
      if (unit != null &&
          unit.abuConnected &&  //接続されている
          unit.abuInserted &&  //挿入されている
          unit.abuPath.equals (path)) {  //パスが一致している
        return true;  //既に挿入されている
      }
    }
    return false;  //まだ挿入されていない
  }  //hfsIsInserted(String)

  //hfsReset ()
  //  HFSのリセット
  //  開いたままのファイルがあれば閉じる
  public static void hfsReset () {
    for (HFUnit unit : hfsUnitArray) {
      unit.hfuTini ();
    }
  }  //hfsReset()

  //success = hfsIPLBoot ()
  //  IPL起動ルーチン
  public static boolean hfsIPLBoot () {
    return hfsUnitArray[hfsBootUnit].hfuIPLBoot ();
  }  //hfsIPLBoot()

  //hfsInstall ()
  //  デバイスドライバ組み込みルーチン
  //  Human68kがデバイスドライバを組み込むときに呼び出す
  //  メインメモリにデバイスヘッダを構築する
  //  d2=-1を返すまで組み込みルーチンと初期化コマンドが繰り返して呼び出される
  //  1つのインタフェイスで種類の異なる複数のデバイスドライバを組み込むことができる
  //  <d0.l  デバイスドライバ組み込みパラメータ
  //  <d2.l  フラグ。初回は0。2回目以降は前回返したd2の値がそのまま入っている
  //         SCSIドライバの場合
  //           次に接続を確認するSCSI-ID
  //  <a1.l  デバイスヘッダを構築するアドレス
  //  >d2.l  フラグ。-1=終了
  //         SCSIドライバの場合
  //           入力されたd2以上のSCSI-IDを持つ認識可能なSCSI機器が接続されているとき
  //             そのSCSI機器のSCSI-ID+1
  //           入力されたd2以上のSCSI-IDを持つ認識可能なSCSI機器が接続されていないとき
  //             -1
  public static void hfsInstall () throws M68kException {
    if (XEiJ.regRn[2] != 0) {
      XEiJ.regRn[2] = -1;
    } else {
      XEiJ.regRn[2] = 1;
      int a1 = XEiJ.regRn[9];
      hfsDeviceHeader = a1;
      MC68060.mmuWriteLongData (a1 + HFS_NEXT_DEVICE,       -1, XEiJ.regSRS);  //ネクストデバイスドライバハンドル
      MC68060.mmuWriteWordData (a1 + HFS_DEVICE_TYPE,       0x2000, XEiJ.regSRS);  //デバイスタイプ。特殊デバイスドライバ、特殊コントロール不可
      MC68060.mmuWriteLongData (a1 + HFS_STRATEGY_HANDLE,   a1 + HFS_STRATEGY_ROUTINE, XEiJ.regSRS);  //ストラテジハンドル
      MC68060.mmuWriteLongData (a1 + HFS_INTERRUPT_HANDLE,  a1 + HFS_INTERRUPT_ROUTINE, XEiJ.regSRS);  //インタラプトハンドル
      MC68060.mmuWriteLongData (a1 + HFS_DEVICE_NAME,       0x01 << 24 | 'X' << 16 | 'E' << 8 | 'I', XEiJ.regSRS);  //デバイス名
      MC68060.mmuWriteLongData (a1 + HFS_DEVICE_NAME + 4,   'J' << 24 | 'H' << 16 | 'F' << 8 | 'S', XEiJ.regSRS);
      MC68060.mmuWriteLongData (a1 + HFS_STRATEGY_ROUTINE,  XEiJ.EMX_OPCODE_HFSSTR << 16 | 0x4e75, XEiJ.regSRS);  //ストラテジルーチン
      MC68060.mmuWriteLongData (a1 + HFS_INTERRUPT_ROUTINE, XEiJ.EMX_OPCODE_HFSINT << 16 | 0x4e75, XEiJ.regSRS);  //インタラプトルーチン
    }
  }  //hfsInstall()

  //hfsStrategy ()
  //  デバイスドライバのストラテジルーチン
  public static void hfsStrategy () throws M68kException {
    hfsRequestHeader = XEiJ.regRn[13];  //リクエストヘッダのアドレス

    if (false && HFS_DEBUG_TRACE) {
      System.out.printf ("hfsStrategy\n");
      System.out.printf ("  hfsRequestHeader = %08x\n", hfsRequestHeader);
    }

  }  //hfsStrategy()

  //wait = hfsInterrupt ()
  //  デバイスドライバのインタラプトルーチン
  //  trueを返したときWAIT命令を繰り返す
  public static boolean hfsInterrupt () throws M68kException {

    if (false && HFS_DEBUG_TRACE) {
      System.out.printf ("hfsInterrupt\n");
      System.out.printf ("  hfsRequestHeader = %08x\n", hfsRequestHeader);
      System.out.printf ("  hfsState = %d\n", hfsState);
    }

    int a5 = hfsRequestHeader;  //リクエストヘッダのアドレス。インタラプトルーチンが呼び出された時点でa5に入っているとは限らない

    if (hfsState == HFS_STATE_IDLE) {  //新たなコマンド

      if (HFS_DEBUG_TRACE) {
        int number = MC68060.mmuPeekByteZeroData (a5 + 1, XEiJ.regSRS);
        int command = MC68060.mmuPeekByteZeroData (a5 + 2, XEiJ.regSRS);
        int mode = MC68060.mmuPeekByteZeroData (a5 + 13, XEiJ.regSRS);
        int namests = MC68060.mmuPeekLongData (a5 + 14, XEiJ.regSRS);
        int param = MC68060.mmuPeekLongData (a5 + 18, XEiJ.regSRS);
        int fcb = MC68060.mmuPeekLongData (a5 + 22, XEiJ.regSRS);
        if (!(hfsRequest2Command == 0x57 ||  //mediacheck
              hfsRequest2Command == 0x4c && param == 1)) {  //read(1バイト)
          for (int i = 0; i < 26; i++) {
            System.out.printf (" %02x", MC68060.mmuPeekByteZeroData (a5 + i, XEiJ.regSRS));
          }
          System.out.println ();
          System.out.printf ("  Number: %02x\n", number);
          System.out.printf ("  Command: %02x\n", command);
          System.out.printf ("  Mode: %02x\n", mode);
          System.out.printf ("  Namests: %08x:", namests);
          for (int i = 0; i < 88; i++) {
            System.out.printf (" %02x", MC68060.mmuPeekByteZeroData (namests + i, XEiJ.regSRS));
          }
          System.out.println ();
          System.out.printf ("  Param: %08x\n", param);
          System.out.printf ("  Fcb: %08x:", fcb);
          for (int i = 0; i < 96; i++) {
            System.out.printf (" %02x", MC68060.mmuPeekByteZeroData (fcb + i, XEiJ.regSRS));
          }
          System.out.println ();
        }
      }

      //  アドレスを配列のインデックスやマップのキーとして使うときはマスクするのを忘れないこと
      //  主にアドレスとして使うパラメータでもそれ以外の使い方をする場合があるのでここではマスクしない
      hfsRequest1Number = MC68060.mmuReadByteZeroData (a5 + 1, XEiJ.regSRS);  //リクエストヘッダのユニット番号
      hfsRequest2Command = MC68060.mmuReadByteZeroData (a5 + 2, XEiJ.regSRS) & 0x7f;  //コマンドコード。ベリファイフラグは無視する
      hfsRequest13Mode = MC68060.mmuReadByteZeroData (a5 + 13, XEiJ.regSRS);  //モードなど
      hfsRequest14Namests = MC68060.mmuReadLongData (a5 + 14, XEiJ.regSRS);  //_NAMESTS形式のファイル名など
      hfsRequest18Param = MC68060.mmuReadLongData (a5 + 18, XEiJ.regSRS);  //追加のパラメータ
      hfsRequest22Fcb = MC68060.mmuReadLongData (a5 + 22, XEiJ.regSRS);  //FCBテーブルのアドレスなど
      hfsRequest3Error = 0;
      hfsRequest18Result = 0;

      if (hfsRequest2Command == 0x40) {  //初期化コマンド

        //0x40 initialize 初期化
        //  リクエストヘッダ
        //       0.b  i   22
        //       2.b  i   コマンドコード。0x40
        //       3.b  o   エラーコード下位
        //       4.b  o   エラーコード上位
        //      13.b  o   ユニット数
        //      14.l  o   デバイスドライバの末尾
        //      18.l  i   CONFIG.SYSに書かれた引数のアドレス。デバイス名,0,引数1,0,…,引数n,0,0。起動デバイスのときは0,0のみ
        //      22.b  i   内部ドライブ番号(1=A:)
        //            o   起動ユニット番号(1～)
        //    (23バイト)
        //  ユニット数の条件
        //    ユニット数は1以上でなければならない
        //    ユニット数が0になるときはエラーコード下位で0以外の値を返すこと
        //  デバイスドライバの長さの条件
        //    ストラテジルーチンとインタラプトルーチンがデバイスドライバの中に書かれている必要はないが、
        //    デバイスドライバの長さはデバイスヘッダの22バイトを含めて34バイト以上でなければならない
        //  Human68kをリモートデバイスから起動しようとすると暴走するバグの解説と対策
        //    解説
        //      RAMまたはROMから起動したとき、Human68kは起動ドライブのデバイスドライバを初期化した後にDISK2HDを初期化する
        //      Human68kはDISK2HDを初期化するときリクエストヘッダの領域を再利用するが、初期化コマンドを設定し直すことを忘れている
        //      DISK2HDなどのブロックデバイスの初期化コマンドは0x00だが、リモートデバイスの初期化コマンドは0x40である
        //      すなわち、リモートデバイスから起動するとDISK2HDに0x40というブロックデバイスには存在しないコマンドが渡される
        //      DISK2HDはコマンドの範囲をチェックしておらず、範囲外の0x40が渡されるとジャンプテーブルから外れて暴走する
        //      human302の場合は0x000109a4+0x40*4=0x00010aa4にある命令列MOVE.B (A6),D1;BEQ.S *+4のコード0x12166702にジャンプする
        //      2MB以上積んでいれば0x00166702にはRAMがあり、アドレスも偶数なので即エラーとならず、スーパーバイザモードで突っ走る
        //    対策
        //      リモートデバイスを起動可能にするときは初期化コマンドでリクエストヘッダのコマンドを0x00に書き換える
        //      起動デバイスの初期化コマンドは複数回呼ばれる場合があって最後の1回だけ書き換えれば良いのだが、
        //      毎回0x40が設定されてから呼び出されるので毎回0x00に書き換えてしまっても問題ない
        //      最後に呼び出されたときに書き込んだ0x00がDISK2HDデバイスドライバの初期化コマンドとして使用される
        if (HFS_COMMAND_TRACE && hfsCommandTraceOn) {
          int pc = MainMemory.mmrGetLevelZeroPC ();  //DOSコールにレベル0で入ったときのpc
          System.out.println (LabeledAddress.lblSearch (new StringBuilder (), pc).toString ());
          System.out.printf ("%08x initialize(internaldrive=0x%02,param=0x%08x)\n",
                             pc, hfsRequest22Fcb >>> 24, hfsRequest18Param);
        }
        MC68060.mmuWriteByteData (a5 + 2, 0x00, XEiJ.regSRS);  //DISK2HDの初期化コマンド
        //起動時に接続されていたユニットだけをドライブとして登録する
        //  ユニット番号がずれるのでユニット番号の変換表を作る
        //  起動ユニット番号も変換する
        hfsDeviceUnitCount = 0;
        int bootUnit = -1;
        for (int u = 0; u < HFS_MAX_UNITS; u++) {
          if (hfsUnitArray[u].isConnected ()) {
            if (u == hfsBootUnit) {
              bootUnit = hfsDeviceUnitCount;
            }
            hfsDeviceUnitArray[hfsDeviceUnitCount++] = u;
          }
        }
        if (hfsDeviceUnitCount > 0 && bootUnit >= 0) {  //起動ユニットが接続されている
          MC68060.mmuWriteByteData (a5 + 13, hfsDeviceUnitCount, XEiJ.regSRS);  //ユニット数
          MC68060.mmuWriteLongData (a5 + 14, hfsDeviceHeader + 34, XEiJ.regSRS);  //デバイスドライバの末尾
          MC68060.mmuWriteByteData (a5 + 22, 1 + bootUnit, XEiJ.regSRS);  //起動ユニット番号(1～)
        } else {  //起動ユニットが接続されていない
          hfsRequest3Error = HFUnit.DEV_ABORT | HFUnit.DEV_INVALID_UNIT_NUMBER;  //ユニットを増やすにはリセットする必要があるので中止のみ
          hfsRequest18Result = -1;
        }
        hfsState = HFS_STATE_DONE;

      } else {  //初期化コマンド以外のコマンド

        if (hfsRequest1Number < hfsDeviceUnitCount) {  //ユニットがある
          //リクエストヘッダのユニット番号をHFSのユニット番号に変換する
          hfsRequestUnit = hfsUnitArray[hfsDeviceUnitArray[hfsRequest1Number]];
          hfsState = HFS_STATE_X68K;  //X68000側の処理中
          hfsRequestUnit.hfuCall ();
        } else {  //ユニットがない
          hfsRequest3Error = HFUnit.DEV_ABORT | HFUnit.DEV_INVALID_UNIT_NUMBER;  //ユニットを増やすにはリセットする必要があるので中止のみ
          hfsRequest18Result = -1;
          hfsState = HFS_STATE_DONE;
        }

      }

    }  //if hfsState==HFS_STATE_IDLE

    if (HFS_USE_THREAD) {
      while (hfsState != HFS_STATE_DONE) {
        if (hfsState == HFS_STATE_X68K) {  //X68000側の処理中
          hfsRequestUnit.hfuCallX68k ();
          if (hfsState == HFS_STATE_X68K) {  //X68000側の処理を繰り返す
            return true;  //WAITあり
          }
        }
        if (hfsState == HFS_STATE_HOST) {  //ホスト側のタスクの起動中
          hfsState = HFS_STATE_BUSY;  //ホスト側の処理中
          if (hfsTimer != null) {
            hfsTimer.schedule (new TimerTask () {
              @Override public void run () {
                hfsRequestUnit.hfuCallHost ();
              }
            }, HFS_THREAD_DELAY);
          }
        }
        //ここでホスト側の処理が一瞬で終わってhfsState==HFS_STATE_X68Kになっている場合がある
        if (hfsState == HFS_STATE_BUSY) {  //ホスト側の処理中
          return true;  //WAITあり
        }
      }
    } else {
      while (hfsState != HFS_STATE_DONE) {
        while (hfsState == HFS_STATE_X68K) {  //X68000側の処理中
          hfsRequestUnit.hfuCallX68k ();
        }
        if (hfsState == HFS_STATE_HOST) {  //ホスト側のタスクの起動中
          hfsState = HFS_STATE_BUSY;  //ホスト側の処理中
        }
        if (hfsState == HFS_STATE_BUSY) {  //ホスト側の処理中
          hfsRequestUnit.hfuCallHost ();
        }
      }
    }

    if (HFS_COMMAND_TRACE && hfsCommandTraceOn) {
      System.out.printf ("\terror=0x%04x,result=0x%08x\n", hfsRequest3Error, hfsRequest18Result);
    }

    MC68060.mmuWriteByteData (a5 +  3, hfsRequest3Error, XEiJ.regSRS);  //エラーコード下位
    MC68060.mmuWriteByteData (a5 +  4, hfsRequest3Error >> 8, XEiJ.regSRS);  //エラーコード上位
    MC68060.mmuWriteLongData (a5 + 18, hfsRequest18Result, XEiJ.regSRS);  //リザルトステータス

    hfsState = HFS_STATE_IDLE;
    return false;  //WAITなし
  }  //hfsInterrupt()

  static class OpenDialog extends AbstractOpenDialog {
    public OpenDialog () {
      super (XEiJ.frmFrame,
             "Host file system directory to assign Human68k drives",
             "Human68k のドライブを割り当てるホストファイルシステムのディレクトリ",
             true,  //ディレクトリ
             hfsFileFilter);
    }
    @Override public void openFiles (File[] files, boolean reboot) {
      hfsOpenFiles (files, reboot);
    }
  }  //class OpenDialog

  //hfsOpenFiles (list, reset)
  //  開くダイアログで選択されたファイルを開く
  public static void hfsOpenFiles (File[] list, boolean reset) {
    boolean success = true;
    for (int u = hfsOpenUnit, k = 0; k < list.length; ) {
      if (HFS_MAX_UNITS <= u) {  //ユニットが足りない
        success = false;  //失敗
        break;
      }
      HFUnit unit = hfsUnitArray[u];  //ユニット
      if (!unit.abuConnected) {  //接続されていない
        u++;
        continue;
      }
      File file = list[k++];  //ルートになるディレクトリまたはそこにあるかも知れないファイル
      if (!file.isDirectory ()) {  //ディレクトリがない
        file = file.getParentFile ();  //親ディレクトリ
        if (file == null || !file.isDirectory ()) {  //親ディレクトリもない
          success = false;  //失敗
          continue;
        }
      }
      if (!unit.insert (file.getPath (),
                        hfsOpenDialog.getReadOnly () || !file.canWrite ())) {  //挿入できない
        success = false;  //失敗
        continue;
      }
      u++;
    }
    if (success) {  //すべて挿入できた
      hfsAddHistory (list);  //ヒストリに追加する
      if (reset) {  //ここから再起動
        hfsBootUnit = hfsOpenUnit;
        XEiJ.mpuReset (0xa000, HFS_BOOT_HANDLE);
      }
    }
  }  //hfsOpenFiles(File[],boolean)


  //hfsAddHistory (file)
  //  ファイルをヒストリに追加する
  public static void hfsAddHistory (File file) {
    hfsAddHistory (new File[] { file });
  }

  //hfsAddHistory (files)
  //  複数のファイルをヒストリに追加する
  public static void hfsAddHistory (File[] files) {
    if (hfsOpenDialog == null) {
      hfsOpenHistory.add (files);
    } else {
      hfsOpenDialog.addHistory (files);
    }
  }


  //hfsCheckTwentyOneOption ()
  //  TwentyOne.xのオプションを確認する
  //  hfsTwentyOneOptionを更新する
  public void hfsCheckTwentyOneOption () {
    hfsTwentyOneOption = 0;
    if (HFS_USE_TWENTY_ONE) {
      int a = MainMemory.mmrTwentyOneOptionAddress;
      if (0 < a) {
        hfsTwentyOneOption = MC68060.mmuPeekLongData (a, 1);
      }
    }
  }  //hfsCheckTwentyOneOption()



  //========================================================================================
  //$$HFU HFSユニット
  //
  //  _NAMESTS形式のファイル名
  //    0000     0.b      フラグまたはパスの長さ
  //    0001     1.b      内部ドライブ番号(0=A:)
  //    0002     2.b[65]  パス(区切りは'\'または$09)
  //    0043    67.b[8]   ファイル名1
  //    004B    75.b[3]   拡張子
  //    004E    78.b[10]  ファイル名2
  //    0058    88バイト
  //
  //  FCBテーブル
  //                      $0000B520の設定値
  //    0000     0.b      1               FCBテーブルの参照数
  //                                      ハンドラFCB変換テーブルの何箇所から参照されているか(0ならば未使用)
  //    0001     1.b      FCBフラグ       FCBフラグ
  //                                      ブロックデバイス,特殊デバイス    キャラクタデバイス
  //               bit7   0               0                                1
  //               bit6   0               1=_CLOSEしたとき日時を更新する   EOFのON/OFF
  //               bit5   0/1             1=特殊デバイスドライバ           0=COOKEDモード,1=RAWモード
  //               bit4   }               }                                未使用
  //               bit3   }               }                                1=CLOCK
  //               bit2   }               }内部ドライブ番号(0=A:)          1=NUL
  //               bit1   }               }                                1=標準出力
  //               bit0   }               }                                1=標準入力
  //    0002     2.l                      内部DPBテーブル                  デバイスヘッダ
  //    0006     6.l      0               現在のシーク位置
  //    000A    10.l                      シェア管理テーブルのアドレス
  //    000E    14.b      オープンモード  オープンモード
  //                                      bit0～3(アクセスモード)
  //                                              0       読み込み
  //                                              1       書き込み
  //                                              2       読み書き
  //                                              3       _CHMOD,_DELETE,_RENAME
  //                                              4
  //    000F    15バイト
  //    特殊デバイスドライバでは以下の領域を自由に使用してよい
  //    000F    15.b      0               セクタ内で何番目のエントリか。ブロックデバイスのとき$0000C1B4で設定
  //    0010    16.b      0               アクセス中のクラスタ内でのセクタ位置
  //    0011    17.b      0               FAT先頭からのセクタオフセット
  //    0012    18.w      0               FAT先頭からの(現在アクセスしているFATへの)セクタオフセット
  //    0014    20.l      0               現在のデータのセクタ位置
  //    0018    24.l      0               現在のデータのバッファアドレス
  //    001C    28.l      0               ディレクトリのセクタ位置。ブロックデバイスのとき$0000C1B4で設定
  //    0020    32.l      0               次のFCBテーブル
  //    ここからディレクトリエントリ
  //      ブロックデバイスのとき$0000C1B4の中の$0000C1FEでディスクからコピーされる
  //      時刻、日付、先頭クラスタ番号、ファイルサイズはディスク上はリトルエンディアンで、
  //      FCBテーブルにコピーされるときにビッグエンディアンに変換されている
  //    0024    36.b[8]   ファイル名1     デバイス名またはファイル名1
  //    002C    44.b[3]   拡張子          拡張子
  //    002F    47.b      0x20            ファイル属性
  //    0030    48.b[10]  ファイル名2     ファイル名2
  //    003A    58.w      現在時刻        時刻。時<<11|分<<5|秒/2
  //    003C    60.w      現在日付        日付。(西暦年-1980)<<9|月<<5|月通日
  //    003E    62.w      0               このファイルの最初のクラスタ番号
  //    0040    64.l      0               ファイルサイズ
  //    ここまでディレクトリエントリ
  //    0044    68.w                      ディスク内クラスタ番号1
  //    0046    70.w                      ファイル内クラスタ番号1
  //    0048    72.w                      ディスク内クラスタ番号2
  //    004A    74.w                      ファイル内クラスタ番号2
  //    004C    76.w                      ディスク内クラスタ番号3
  //    004E    78.w                      ファイル内クラスタ番号3
  //    0050    80.w                      ディスク内クラスタ番号4
  //    0052    82.w                      ファイル内クラスタ番号4
  //    0054    84.w                      ディスク内クラスタ番号5
  //    0056    86.w                      ファイル内クラスタ番号5
  //    0058    88.w                      ディスク内クラスタ番号6
  //    005A    90.w                      ファイル内クラスタ番号6
  //    005C    92.w                      ディスク内クラスタ番号7
  //    005E    94.w                      ファイル内クラスタ番号7
  //    0060    96バイト
  //
  public static class HFUnit extends AbstractUnit {

    //デバイスエラー
    //  主に装置、メディア、管理領域などの問題
    //  状況によってリトライできる場合がある
    //  上位バイト
    public static final int DEV_IGNORE                 = 0x4000;  //無視(I)
    public static final int DEV_RETRY                  = 0x2000;  //再実行(R)
    public static final int DEV_ABORT                  = 0x1000;  //中止(A)
    //  下位バイト
    public static final int DEV_INVALID_UNIT_NUMBER    = 0x0001;  //無効なユニット番号を指定しました
    public static final int DEV_INSERT_MEDIA           = 0x0002;  //ディスクが入っていません、入れてください
    public static final int DEV_UNKNOWN_COMMAND        = 0x0003;  //デバイスドライバに無効なコマンドを指定しました
    public static final int DEV_CRC_ERROR              = 0x0004;  //ＣＲＣエラー
    public static final int DEV_MANEGEMENT_AREA_BROKEN = 0x0005;  //ディスクの管理領域が破壊されています、使用不能です
    public static final int DEV_SEEK_ERROR             = 0x0006;  //シークエラー
    public static final int DEV_INVALID_MEDIA          = 0x0007;  //無効なメディアを使用しました
    public static final int DEV_SECTOR_NOT_FOUND       = 0x0008;  //セクタが見つかりません
    public static final int DEV_PRINTER_NOT_CONNECTED  = 0x0009;  //プリンタがつながっていません
    public static final int DEV_WRITE_ERROR            = 0x000a;  //書き込みエラー
    public static final int DEV_READ_ERROR             = 0x000b;  //読み込みエラー
    public static final int DEV_MISCELLANEOUS_ERROR    = 0x000c;  //エラーが発生しました
    public static final int DEV_UNPROTECT_MEDIA        = 0x000d;  //プロテクトをはずして、同じディスクを入れてください
    public static final int DEV_CANNOT_WRITE           = 0x000e;  //書き込み不可能です
    public static final int DEV_FILE_SHARING_VIOLATION = 0x000f;  //ファイル共有違反です。現在使用できません。

    //DOSコールエラー
    //  主にファイルシステムの中の問題
    public static final int DOS_INVALID_FUNCTION      =  -1;  //無効なファンクションコード
    public static final int DOS_FILE_NOT_FOUND        =  -2;  //ファイルが見つからない
    public static final int DOS_DIRECTORY_NOT_FOUND   =  -3;  //ディレクトリが見つからない
    public static final int DOS_TOO_MANY_HANDLES      =  -4;  //オープンしているファイルが多すぎる
    public static final int DOS_NOT_A_FILE            =  -5;  //ディレクトリやボリュームラベルをアクセスしようとした
    public static final int DOS_HANDLE_IS_NOT_OPENED  =  -6;  //指定したハンドラがオープンされていない
    public static final int DOS_BROKEN_MEMORY_CHAIN   =  -7;  //メモリ管理領域が壊れている(実際に-7が返されることはない)
    public static final int DOS_NOT_ENOUGH_MEMORY     =  -8;  //メモリが足りない
    public static final int DOS_INVALID_MEMORY_CHAIN  =  -9;  //無効なメモリ管理テーブルを指定した
    public static final int DOS_INVALID_ENVIRONMENT   = -10;  //不正な環境を指定した(実際に-10が返されることはない)
    public static final int DOS_ABNORMAL_X_FILE       = -11;  //実行ファイルのフォーマットが異常
    public static final int DOS_INVALID_ACCESS_MODE   = -12;  //オープンのアクセスモードが異常
    public static final int DOS_ILLEGAL_FILE_NAME     = -13;  //ファイル名の指定が間違っている
    public static final int DOS_INVALID_PARAMETER     = -14;  //パラメータが無効
    public static final int DOS_ILLEGAL_DRIVE_NUMBER  = -15;  //ドライブの指定が間違っている
    public static final int DOS_CURRENT_DIRECTORY     = -16;  //カレントディレクトリを削除しようとした
    public static final int DOS_CANNOT_IOCTRL         = -17;  //_IOCTRLできないデバイス
    public static final int DOS_NO_MORE_FILES         = -18;  //該当するファイルがもうない(_FILES,_NFILES)
    public static final int DOS_CANNOT_WRITE          = -19;  //ファイルに書き込めない(主に属性R,Sのファイルに対する書き込みや削除)
    public static final int DOS_DIRECTORY_EXISTS      = -20;  //同一名のディレクトリを作ろうとした
    public static final int DOS_RM_NONEMPTY_DIRECTORY = -21;  //空でないディレクトリを削除しようとした
    public static final int DOS_MV_NONEMPTY_DIRECTORY = -22;  //空でないディレクトリを移動しようとした
    public static final int DOS_DISK_FULL             = -23;  //ディスクフル
    public static final int DOS_DIRECTORY_FULL        = -24;  //ディレクトリフル
    public static final int DOS_SEEK_OVER_EOF         = -25;  //EOFを越えてシークしようとした
    public static final int DOS_ALREADY_SUPERVISOR    = -26;  //既にスーパーバイザ状態になっている
    public static final int DOS_THREAD_EXISTS         = -27;  //同じスレッド名が存在する
    public static final int DOS_COMMUNICATION_FAILED  = -28;  //スレッド間通信バッファに書き込めない(ビジーまたはオーバーフロー)
    public static final int DOS_TOO_MANY_THREADS      = -29;  //これ以上バックグラウンドでスレッドを起動できない
    public static final int DOS_NOT_ENOUGH_LOCK_AREA  = -32;  //ロック領域が足りない
    public static final int DOS_FILE_IS_LOCKED        = -33;  //ロックされていてアクセスできない
    public static final int DOS_OPENED_HANDLE_EXISTS  = -34;  //指定のドライブはハンドラがオープンされている
    public static final int DOS_FILE_EXISTS           = -80;  //ファイルが存在している(_NEWFILE,_MAKETMP)
    //8200000?  メモリが完全に確保できない(下位4bitは不定)
    //81??????  メモリが確保できない(下位24bitは確保できる最大のサイズ)

    private static final int HFU_FILES_MAGIC = '*' << 24 | 'H' << 16 | 'F' << 8 | 'S';  //_FILESのバッファに書き込むマジック

    public String hfuRootPath;  //ルートディレクトリのパス。末尾の'/'を含まない

    //_FILESのバッファ
    public HashMap<Integer,ArrayDeque<byte[]>> hfuFilesBufferToArrayDeque;  //_FILESのバッファの通し番号→ファイルの一覧のArrayDeque
    public int hfuFilesBufferCounter;  //_FILESのバッファの通し番号

    //ハンドル
    public class HFHandle {
      public int hfhFcb;  //FCBのアドレス
      public File hfhFile;  //ホストにある実体のFile
      public RandomAccessFile hfhRaf;  //ホストにある実体のRandomAccessFile
      public byte[] hfhBuffer;  //先読み・遅延書き込みバッファ
      public long hfhStart;  //バッファの先頭のシーク位置
      public long hfhEnd;  //バッファのデータの末尾のシーク位置
      public boolean hfhDirty;  //true=ダーティデータがある
      public HFHandle (int fcb, File file, RandomAccessFile raf) {
        hfhFcb = fcb;
        hfhFile = file;
        hfhRaf = raf;
        hfhBuffer = new byte[HFS_BUFFER_SIZE];
        hfhStart = 0L;
        hfhEnd = 0L;
        hfhDirty = false;
      }
      @Override public String toString () {
        return String.format ("HFHandle{fcb:0x%08x,file:\"%s\",start:%d,end:%d,dirty:%b}", hfhFcb, hfhFile.toString (), hfhStart, hfhEnd, hfhDirty);
      }
    }
    public HashMap<Integer,HFHandle> hfuFcbToHandle;  //オープンしているファイルのFCBのアドレス→ハンドル
    public LinkedList<HFHandle> hfuClosedHandle;  //クローズされたハンドル。次にオープンするとき再利用する
    public HFHandle hfuNewHandle (int fcb, File file, RandomAccessFile raf) {
      HFHandle handle = hfuClosedHandle.pollFirst ();  //先頭の要素を取り除いて返す。空のときはnull
      if (handle == null) {
        return new HFHandle (fcb, file, raf);
      }
      handle.hfhFcb = fcb;
      handle.hfhFile = file;
      handle.hfhRaf = raf;
      Arrays.fill (handle.hfhBuffer, (byte) 0);
      handle.hfhStart = 0L;
      handle.hfhEnd = 0L;
      handle.hfhDirty = false;
      return handle;
    }
    public void hfuRecycleHandle (HFHandle handle) {
      hfuClosedHandle.push (handle);
    }

    //コマンドのワークエリア
    public final byte[] hfuTargetNameArray1 = new byte[88];  //ワークエリア
    public final byte[] hfuTargetNameArray2 = new byte[88];  //ワークエリア
    public String hfuTargetName1;  //hfuNamestsToPath(hfsRequest14Namests,～)
    public String hfuTargetName2;  //hfuNamestsToPath(hfsRequest18Param,～)
    public long hfuTargetLastModified;  //最終更新時刻
    public int hfuTargetOpenMode;  //<(fcb+14).b:オープンモード。0=読み出し,1=書き込み,2=読み書き
    public HFHandle hfuTargetHandle;  //ハンドル
    public long hfuTargetPosition;  //シーク位置
    public long hfuTargetFileSize;  //ファイルサイズ
    public long hfuTargetLength;  //転送する長さの初期値
    public long hfuTargetAddress;  //転送するアドレスの初期値
    public long hfuTargetTransferred;  //これまでに転送した長さ
    public long hfuTargetTotalSpace;
    public long hfuTargetFreeSpace;

    //unit = new HFUnit (number)
    //  コンストラクタ
    public HFUnit (int number) {
      super (number);
      hfuRootPath = null;
      hfuFilesBufferToArrayDeque = new HashMap<Integer,ArrayDeque<byte[]>> ();
      hfuFilesBufferCounter = 0;
      hfuFcbToHandle = new HashMap<Integer,HFHandle> ();
      hfuClosedHandle = new LinkedList<HFHandle> ();
    }

    //hfuTini ()
    //  HFUnitの後始末
    //  開いたままのファイルがあれば閉じる
    //  _FILESのバッファをクリアする
    public void hfuTini () {
      for (HFHandle handle : hfuFcbToHandle.values ()) {
        RandomAccessFile raf = handle.hfhRaf;
        //バッファをフラッシュする
        if (handle.hfhDirty) {  //ダーティデータが残っている
          if (HFS_BUFFER_TRACE) {
            System.out.printf ("delaywrite(fcb=0x%08x,name=\"%s\",start=0x%08x,end=0x%08x)\n", handle.hfhFcb, handle.hfhFile.toString(), handle.hfhStart, handle.hfhEnd);
          }
          try {
            raf.seek (handle.hfhStart);
          } catch (IOException ioe) {
            System.out.println ((Multilingual.mlnJapanese ? "シークエラー: " : "Seek error: ") + handle.toString ());
          }
          try {
            raf.write (handle.hfhBuffer, 0, (int) (handle.hfhEnd - handle.hfhStart));  //RandomAccessFileのwriteは返却値がない
          } catch (IOException ioe) {
            System.out.println ((Multilingual.mlnJapanese ? "遅延書き込みに失敗しました: " : "Delayed write failed: ") + handle.toString ());
          }
          handle.hfhDirty = false;
        }  //if handle.hfhDirty
        try {
          raf.close ();
        } catch (IOException ioe) {
          System.out.println ((Multilingual.mlnJapanese ? "クローズエラー: " : "Close error: ") + handle.toString ());
        }
      }  //for handle
      hfuFcbToHandle.clear ();
      hfuFilesBufferToArrayDeque.clear ();
      //hfuFilesBufferCounter = 0;
    }

    //success = unit.hfuIPLBoot ()
    //  このユニットからIPL起動する
    //    ルートディレクトリからHUMAN.SYSをロードする
    //    bss+comm+stackをクリアしてから実行開始位置にジャンプする
    //  HUMAN.SYSはX形式実行ファイルでリロケートテーブルも付いているがベースアドレスにロードするのでリロケートする必要はない
    //  参考にしたもの
    //    FORMAT.Xがハードディスクに書き込むIPL起動ルーチン
    public boolean hfuIPLBoot () {
      if (!abuConnected) {  //接続されていないとき
        return false;  //失敗
      }
      //InputStream in = XEiJ.ismOpen (hfuRootPath + "/HUMAN.SYS");  //ルートディレクトリにあるHUMAN.SYSを開く
      byte[] rr = XEiJ.rscGetResource ("HUMAN.SYS");  //HUMAN.SYSをリソースから読み込む
      if (rr == null ||  //読み込めないか
          ByteArray.byaRwz (rr, 0x00) != ('H' << 8 | 'U') ||  //X形式実行ファイルのマジックがないか
          ByteArray.byaRls (rr, 0x04) != 0x00006800 ||  //ベースアドレスが違うか
          ByteArray.byaRls (rr, 0x08) != 0x00006800) {  //実行開始位置が違うとき
        return false;  //失敗
      }
      int textData = ByteArray.byaRls (rr, 0x0c) + ByteArray.byaRls (rr, 0x10);  //text+dataのサイズ
      int bssCommStack = ByteArray.byaRls (rr, 0x14);  //bss+comm+stackのサイズ
      System.arraycopy (rr, 0x40, MainMemory.mmrM8, 0x00006800, textData);  //text+dataをメモリに書き込む
      Arrays.fill (MainMemory.mmrM8, 0x00006800 + textData, 0x00006800 + textData + bssCommStack, (byte) 0);  //bss+comm+stackをクリアする
      return true;  //成功
    }

    //success = unit.eject ()
    //  イジェクトする
    @Override protected boolean eject () {
      String path = abuPath;  //イジェクトされたディレクトリまたはHUMAN.SYSのパス。super.eject()を呼び出す前にコピーすること
      if (!super.eject ()) {  //イジェクトする
        return false;
      }
      if (path.length () != 0) {  //挿入されていたとき
        hfsAddHistory (new File (path).getAbsoluteFile ());
        System.out.println (Multilingual.mlnJapanese ?
                            path + " を hf" + abuNumber + " から切り離しました" :
                            path + " was ejected from hf" + abuNumber);
      }
      return true;
    }

    //unit.open ()
    //  開くダイアログを開く
    @Override protected boolean open () {
      if (!super.open ()) {
        return false;
      }
      hfsOpenUnit = abuNumber;
      if (hfsOpenDialog == null) {
        hfsOpenDialog = new OpenDialog ();
        hfsOpenDialog.setReadOnly (Settings.sgsGetOnOff ("hfreadonly"));
        hfsOpenDialog.setReboot (Settings.sgsGetOnOff ("hfappreboot"));
        for (File[] files : hfsOpenHistory) {
          hfsOpenDialog.addHistory (files);
        }
        hfsOpenHistory.clear ();
      }
      hfsOpenDialog.rescanCurrentDirectory ();  //挿入されているファイルが変わると選択できるファイルも変わるのでリストを作り直す
      XEiJ.pnlExitFullScreen (true);
      hfsOpenDialog.setVisible (true);
      return true;
    }  //unit.open()

    //success = load (path)
    //  読み込む
    @Override protected boolean load (String path) {
      File file = new File (path).getAbsoluteFile ();
      if (!file.isDirectory ()) {  //ディレクトリがない
        file = file.getParentFile ();  //親ディレクトリ
        if (file == null || !file.isDirectory ()) {  //親ディレクトリもない
          return false;
        }
      }
      hfuRootPath = file.getAbsolutePath ();
      hfsAddHistory (new File (path).getAbsoluteFile ());
      System.out.println (Multilingual.mlnJapanese ?
                          hfuRootPath + " を hf" + abuNumber + " に接続しました" :
                          hfuRootPath + " was inserted in hf" + abuNumber);
      return true;
    }

    //unit.hfuCall ()
    //  デバイスコマンド
    public void hfuCall () throws M68kException {
      switch (hfsRequest2Command) {
      case 0x41:
        hfuCallChdir ();
        break;
      case 0x42:
        hfuCallMkdir ();
        break;
      case 0x43:
        hfuCallRmdir ();
        break;
      case 0x44:
        hfuCallRename ();
        break;
      case 0x45:
        hfuCallDelete ();
        break;
      case 0x46:
        hfuCallChmod ();
        break;
      case 0x47:
        hfuCallFiles ();
        break;
      case 0x48:
        hfuCallNfiles ();
        break;
      case 0x49:
        hfuCallCreateNewfile ();
        break;
      case 0x4a:
        hfuCallOpen ();
        break;
      case 0x4b:
        hfuCallClose ();
        break;
      case 0x4c:
        hfuCallRead ();
        break;
      case 0x4d:
        hfuCallWrite ();
        break;
      case 0x4e:
        hfuCallSeek ();
        break;
      case 0x4f:
        hfuCallFiledate ();
        break;
      case 0x50:
        hfuCallDskfre ();
        break;
      case 0x51:
        hfuCallDrvctrl ();
        break;
      case 0x52:
        hfuCallGetdpb ();
        break;
      case 0x53:
        hfuCallDiskred ();
        break;
      case 0x54:
        hfuCallDiskwrt ();
        break;
      case 0x55:
        hfuCallSpecialCtrl ();
        break;
      case 0x56:
        hfuCallFflush ();
        break;
      case 0x57:
        hfuCallMediacheck ();
        break;
      case 0x58:
        hfuCallLock ();
        break;
      default:
        hfsRequest3Error = DEV_ABORT | DEV_UNKNOWN_COMMAND;  //デバイスドライバに無効なコマンドを指定しました
        hfsRequest18Result = -1;
        hfsState = HFS_STATE_DONE;
      }
    }  //unit.hfuCall()

    //unit.hfuCallX68k ()
    //  X68000側の処理
    public void hfuCallX68k () throws M68kException {
      switch (hfsRequest2Command) {
/*
      case 0x41:
        hfuCallChdirX68k ();
        break;
      case 0x42:
        hfuCallMkdirX68k ();
        break;
      case 0x43:
        hfuCallRmdirX68k ();
        break;
      case 0x44:
        hfuCallRenameX68k ();
        break;
      case 0x45:
        hfuCallDeleteX68k ();
        break;
      case 0x46:
        hfuCallChmodX68k ();
        break;
*/
      case 0x47:
        hfuCallFilesX68k ();
        break;
/*
      case 0x48:
        hfuCallNfilesX68k ();
        break;
*/
      case 0x49:
        hfuCallCreateNewfileX68k ();
        break;
      case 0x4a:
        hfuCallOpenX68k ();
        break;
      case 0x4b:
        hfuCallCloseX68k ();
        break;
      case 0x4c:
        hfuCallReadX68k ();
        break;
      case 0x4d:
        hfuCallWriteX68k ();
        break;
/*
      case 0x4e:
        hfuCallSeekX68k ();
        break;
      case 0x4f:
        hfuCallFiledateX68k ();
        break;
*/
      case 0x50:
        hfuCallDskfreX68k ();
        break;
/*
      case 0x51:
        hfuCallDrvctrlX68k ();
        break;
      case 0x52:
        hfuCallGetdpbX68k ();
        break;
      case 0x53:
        hfuCallDiskredX68k ();
        break;
      case 0x54:
        hfuCallDiskwrtX68k ();
        break;
      case 0x55:
        hfuCallSpecialCtrlX68k ();
        break;
      case 0x56:
        hfuCallFflushX68k ();
        break;
      case 0x57:
        hfuCallMediacheckX68k ();
        break;
      case 0x58:
        hfuCallLockX68k ();
        break;
*/
      }
    }  //unit.hfuCallX68k()

    //unit.hfuCallHost ()
    //  ホスト側の処理
    public void hfuCallHost () {
      switch (hfsRequest2Command) {
      case 0x41:
        hfuCallChdirHost ();
        break;
      case 0x42:
        hfuCallMkdirHost ();
        break;
      case 0x43:
        hfuCallRmdirHost ();
        break;
      case 0x44:
        hfuCallRenameHost ();
        break;
      case 0x45:
        hfuCallDeleteHost ();
        break;
      case 0x46:
        hfuCallChmodHost ();
        break;
      case 0x47:
        hfuCallFilesHost ();
        break;
/*
      case 0x48:
        hfuCallNfilesHost ();
        break;
*/
      case 0x49:
        hfuCallCreateNewfileHost ();
        break;
      case 0x4a:
        hfuCallOpenHost ();
        break;
      case 0x4b:
        hfuCallCloseHost ();
        break;
      case 0x4c:
        hfuCallReadHost ();
        break;
      case 0x4d:
        hfuCallWriteHost ();
        break;
/*
      case 0x4e:
        hfuCallSeekHost ();
        break;
      case 0x4f:
        hfuCallFiledateHost ();
        break;
*/
      case 0x50:
        hfuCallDskfreHost ();
        break;
/*
      case 0x51:
        hfuCallDrvctrlHost ();
        break;
      case 0x52:
        hfuCallGetdpbHost ();
        break;
      case 0x53:
        hfuCallDiskredHost ();
        break;
      case 0x54:
        hfuCallDiskwrtHost ();
        break;
      case 0x55:
        hfuCallSpecialCtrlHost ();
        break;
*/
      case 0x56:
        hfuCallFflushHost ();
        break;
/*
      case 0x57:
        hfuCallMediacheckHost ();
        break;
      case 0x58:
        hfuCallLockHost ();
        break;
*/
      }
    }  //unit.hfuCallHost()

    //unit.hfuCallChdir ()
    //  0x41 FF3B _CHDIR カレントディレクトリの設定
    //  カレントディレクトリにしようとしているディレクトリが存在していることを確認する
    //  カレントディレクトリの情報はHumanが管理しているのでドライバは記憶しなくてよい
    //  リクエストヘッダ
    //       0.b  i   26
    //       1.b  i   ユニット番号
    //       2.b  i   コマンドコード。0x41/0xc1
    //       3.b  o   エラーコード下位
    //       4.b  o   エラーコード上位
    //      13.b  i   0
    //      14.l  i   カレントディレクトリにするディレクトリ名。_NAMESTS形式
    //      18.l  i   0
    //            o   リザルトステータス
    //      22.l  i   0
    //    (26バイト)
    public void hfuCallChdir () throws M68kException {
      hfuTargetName1 = hfuNamestsToPath (hfsRequest14Namests, false);  //主ファイル名は使わない
      if (hfuTargetName1 == null) {
        hfsRequest18Result = DOS_ILLEGAL_FILE_NAME;  //ファイル名の指定が間違っている
        hfsState = HFS_STATE_DONE;
        return;
      }
      if (HFS_COMMAND_TRACE && hfsCommandTraceOn) {
        int pc = MainMemory.mmrGetLevelZeroPC ();  //DOSコールにレベル0で入ったときのpc
        System.out.println (LabeledAddress.lblSearch (new StringBuilder (), pc).toString ());
        System.out.printf ("%08x chdir(name=\"%s\")\n",
                           pc, hfuTargetName1);
      }
      if (!abuInserted) {  //挿入されていない
        hfsRequest3Error = DEV_RETRY | DEV_ABORT | DEV_INSERT_MEDIA;  //ディスクが入っていません、入れてください
        hfsRequest18Result = -1;
        hfsState = HFS_STATE_DONE;
        return;
      }
      hfsRequest18Result = 0;
      hfsState = HFS_STATE_HOST;
    }  //unit.hfuCallChdir()

    //unit.hfuCallChdirHost ()
    public void hfuCallChdirHost () {
      File file1 = new File (hfuTargetName1);
      hfsRequest18Result = (!file1.isDirectory () || file1.list() == null ? DOS_DIRECTORY_NOT_FOUND :  //ディレクトリがない
                            0);  //成功
      hfsState = HFS_STATE_DONE;
    }  //unit.hfuCallChdirHost()

    //unit.hfuCallMkdir ()
    //  0x42 FF39 _MKDIR ディレクトリの作成
    //  既にあるときはエラー
    //  リクエストヘッダ
    //       0.b  i   26
    //       1.b  i   ユニット番号
    //       2.b  i   コマンドコード。0x42/0xc2
    //       3.b  o   エラーコード下位
    //       4.b  o   エラーコード上位
    //      13.b  i   0
    //      14.l  i   作成するディレクトリ名。_NAMESTS形式
    //      18.l  i   0
    //            o   リザルトステータス
    //      22.l  i   0
    //    (26バイト)
    public void hfuCallMkdir () throws M68kException {
      hfuTargetName1 = hfuNamestsToPath (hfsRequest14Namests);
      if (hfuTargetName1 == null) {
        hfsRequest18Result = DOS_ILLEGAL_FILE_NAME;  //ファイル名の指定が間違っている
        hfsState = HFS_STATE_DONE;
        return;
      }
      if (HFS_COMMAND_TRACE && hfsCommandTraceOn) {
        int pc = MainMemory.mmrGetLevelZeroPC ();  //DOSコールにレベル0で入ったときのpc
        System.out.println (LabeledAddress.lblSearch (new StringBuilder (), pc).toString ());
        System.out.printf ("%08x mkdir(name=\"%s\")\n",
                           pc, hfuTargetName1);
      }
      if (!abuInserted) {  //挿入されていない
        hfsRequest3Error = DEV_RETRY | DEV_ABORT | DEV_INSERT_MEDIA;  //ディスクが入っていません、入れてください
        hfsRequest18Result = -1;
        hfsState = HFS_STATE_DONE;
        return;
      }
      if (abuWriteProtected) {  //書き込みが禁止されている
        hfsRequest3Error = DEV_IGNORE | DEV_RETRY | DEV_ABORT | DEV_CANNOT_WRITE;  //書き込み不可能です
        hfsRequest18Result = -1;
        hfsState = HFS_STATE_DONE;
        return;
      }
      hfsRequest18Result = 0;
      hfsState = HFS_STATE_HOST;
    }  //unit.hfuCallMkdir ()

    //unit.hfuCallMkdirHost ()
    public void hfuCallMkdirHost () {
      try {
        File file1 = new File (hfuTargetName1);
        hfsRequest18Result = (!file1.mkdir () ? DOS_DIRECTORY_EXISTS :  //ディレクトリが既にある
                              0);  //成功
      } catch (Exception e) {  //セキュリティなどのエラー
        hfsRequest18Result = DOS_CANNOT_WRITE;
      }
      hfsState = HFS_STATE_DONE;
    }  //unit.hfuCallMkdirHost()

    //unit.hfuCallRmdir ()
    //  0x43 FF3A _RMDIR ディレクトリの削除
    //  ディレクトリが空でないときはエラー
    //  リクエストヘッダ
    //       0.b  i   26
    //       1.b  i   ユニット番号
    //       2.b  i   コマンドコード。0x43/0xc3
    //       3.b  o   エラーコード下位
    //       4.b  o   エラーコード上位
    //      13.b  i   0
    //      14.l  i   削除するディレクトリ名。_NAMESTS形式
    //      18.l  i   0
    //            o   リザルトステータス
    //      22.l  i   0
    //    (26バイト)
    public void hfuCallRmdir () throws M68kException {
      hfuTargetName1 = hfuNamestsToPath (hfsRequest14Namests);
      if (hfuTargetName1 == null) {
        hfsRequest18Result = DOS_ILLEGAL_FILE_NAME;  //ファイル名の指定が間違っている
        hfsState = HFS_STATE_DONE;
        return;
      }
      if (HFS_COMMAND_TRACE && hfsCommandTraceOn) {
        int pc = MainMemory.mmrGetLevelZeroPC ();  //DOSコールにレベル0で入ったときのpc
        System.out.println (LabeledAddress.lblSearch (new StringBuilder (), pc).toString ());
        System.out.printf ("%08x rmdir(name=\"%s\")\n",
                           pc, hfuTargetName1);
      }
      if (!abuInserted) {  //挿入されていない
        hfsRequest3Error = DEV_RETRY | DEV_ABORT | DEV_INSERT_MEDIA;  //ディスクが入っていません、入れてください
        hfsRequest18Result = -1;
        hfsState = HFS_STATE_DONE;
        return;
      }
      if (abuWriteProtected) {  //書き込みが禁止されている
        hfsRequest3Error = DEV_IGNORE | DEV_RETRY | DEV_ABORT | DEV_CANNOT_WRITE;  //書き込み不可能です
        hfsRequest18Result = -1;
        hfsState = HFS_STATE_DONE;
        return;
      }
      hfsRequest18Result = 0;
      hfsState = HFS_STATE_HOST;
    }  //unit.hfuCallRmdir ()

    //unit.hfuCallRmdirHost ()
    public void hfuCallRmdirHost () {
      try {
        File file1 = new File (hfuTargetName1);
        hfsRequest18Result = (!file1.isDirectory () ? DOS_DIRECTORY_NOT_FOUND :  //ディレクトリがない
                              !file1.canWrite () ? DOS_CANNOT_WRITE :  //ディレクトリがあるが書き込めない
                              !file1.delete () ? DOS_RM_NONEMPTY_DIRECTORY :  //削除できない
                              0);  //成功
      } catch (Exception e) {  //セキュリティなどのエラー
        hfsRequest18Result = DOS_CANNOT_WRITE;
      }
      hfsState = HFS_STATE_DONE;
    }  //unit.hfuCallRmdirHost()

    //unit.hfuCallRename ()
    //  0x44 FF86 _RENAME ファイル名またはディレクトリ名の変更およびファイルの移動
    //  パスが違うときはファイルを移動する
    //  変更後のファイル名が既にあるときはエラー
    //  リクエストヘッダ
    //       0.b  i   26
    //       1.b  i   ユニット番号
    //       2.b  i   コマンドコード。0x44/0xc4
    //       3.b  o   エラーコード下位
    //       4.b  o   エラーコード上位
    //      13.b  i   0
    //      14.l  i   変更前のファイル名。_NAMESTS形式
    //      18.l  i   変更後のファイル名。_NAMESTS形式
    //            o   リザルトステータス
    //      22.l  i   0
    //    (26バイト)
    public void hfuCallRename () throws M68kException {
      hfuTargetName1 = hfuNamestsToPath (hfsRequest14Namests);
      if (hfuTargetName1 == null) {
        hfsRequest18Result = DOS_ILLEGAL_FILE_NAME;  //ファイル名の指定が間違っている
        hfsState = HFS_STATE_DONE;
        return;
      }
      hfuTargetName2 = hfuNamestsToPath (hfsRequest18Param);
      if (hfuTargetName2 == null) {
        hfsRequest18Result = DOS_ILLEGAL_FILE_NAME;  //ファイル名の指定が間違っている
        hfsState = HFS_STATE_DONE;
        return;
      }
      if (HFS_COMMAND_TRACE && hfsCommandTraceOn) {
        int pc = MainMemory.mmrGetLevelZeroPC ();  //DOSコールにレベル0で入ったときのpc
        System.out.println (LabeledAddress.lblSearch (new StringBuilder (), pc).toString ());
        System.out.printf ("%08x rename(from=\"%s\",to=\"%s\")\n",
                           pc, hfuTargetName1, hfuTargetName2);
      }
      if (!abuInserted) {  //挿入されていない
        hfsRequest3Error = DEV_RETRY | DEV_ABORT | DEV_INSERT_MEDIA;  //ディスクが入っていません、入れてください
        hfsRequest18Result = -1;
        hfsState = HFS_STATE_DONE;
        return;
      }
      if (abuWriteProtected) {  //書き込みが禁止されている
        hfsRequest3Error = DEV_IGNORE | DEV_RETRY | DEV_ABORT | DEV_CANNOT_WRITE;  //書き込み不可能です
        hfsRequest18Result = -1;
        hfsState = HFS_STATE_DONE;
        return;
      }
      hfsRequest18Result = 0;
      hfsState = HFS_STATE_HOST;
    }  //unit.hfuCallRename ()

    //unit.hfuCallRenameHost ()
    public void hfuCallRenameHost () {
      try {
        File file1 = new File (hfuTargetName1);
        File file2 = new File (hfuTargetName2);
        hfsRequest18Result = (!file1.exists () ? DOS_FILE_NOT_FOUND :  //ファイルまたはディレクトリがない
                              !file1.renameTo (file2) ? file1.isFile () ? DOS_CANNOT_WRITE : DOS_MV_NONEMPTY_DIRECTORY :  //変更できない
                              0);  //成功
      } catch (Exception e) {  //セキュリティなどのエラー
        hfsRequest18Result = DOS_CANNOT_WRITE;
      }
      hfsState = HFS_STATE_DONE;
    }  //unit.hfuCallRenameHost()

    //unit.hfuCallDelete ()
    //  0x45 FF41 _DELETE ファイルの削除
    //  リクエストヘッダ
    //       0.b  i   26
    //       1.b  i   ユニット番号
    //       2.b  i   コマンドコード。0x45/0xc5
    //       3.b  o   エラーコード下位
    //       4.b  o   エラーコード上位
    //      13.b  i   0
    //      14.l  i   削除するファイル名。_NAMESTS形式
    //      18.l  i   0
    //      22.l  i   0
    //            o   リザルトステータス
    //    (26バイト)
    public void hfuCallDelete () throws M68kException {
      hfuTargetName1 = hfuNamestsToPath (hfsRequest14Namests);
      if (hfuTargetName1 == null) {
        hfsRequest18Result = DOS_ILLEGAL_FILE_NAME;  //ファイル名の指定が間違っている
        hfsState = HFS_STATE_DONE;
        return;
      }
      if (HFS_COMMAND_TRACE && hfsCommandTraceOn) {
        int pc = MainMemory.mmrGetLevelZeroPC ();  //DOSコールにレベル0で入ったときのpc
        System.out.println (LabeledAddress.lblSearch (new StringBuilder (), pc).toString ());
        System.out.printf ("%08x delete(name=\"%s\")\n",
                           pc, hfuTargetName1);
      }
      if (!abuInserted) {  //挿入されていない
        hfsRequest3Error = DEV_RETRY | DEV_ABORT | DEV_INSERT_MEDIA;  //ディスクが入っていません、入れてください
        hfsRequest18Result = -1;
        hfsState = HFS_STATE_DONE;
        return;
      }
      if (abuWriteProtected) {  //書き込みが禁止されている
        hfsRequest3Error = DEV_IGNORE | DEV_RETRY | DEV_ABORT | DEV_CANNOT_WRITE;  //書き込み不可能です
        hfsRequest18Result = -1;
        hfsState = HFS_STATE_DONE;
        return;
      }
      hfsRequest18Result = 0;
      hfsState = HFS_STATE_HOST;
    }  //unit.hfuCallDelete()

    //unit.hfuCallDeleteHost ()
    public void hfuCallDeleteHost () {
      try {
        File file1 = new File (hfuTargetName1);
        hfsRequest18Result = (!file1.isFile () ? DOS_FILE_NOT_FOUND :  //ファイルがない
                              !file1.canWrite () ? DOS_CANNOT_WRITE :  //ファイルがあるが書き込めない
                              !file1.delete () ? DOS_CANNOT_WRITE :  //削除できない
                              0);  //成功
      } catch (Exception e) {  //セキュリティなどのエラー
        hfsRequest18Result = DOS_CANNOT_WRITE;
      }
      hfsState = HFS_STATE_DONE;
    }  //unit.hfuCallDeleteHost()

    //unit.hfuCallChmod ()
    //  0x46 FF43 _CHMOD ファイルまたはディレクトリの属性の読み込みと設定
    //  リクエストヘッダ
    //       0.b  i   26
    //       1.b  i   ユニット番号
    //       2.b  i   コマンドコード。0x46/0xc6
    //       3.b  o   エラーコード下位
    //       4.b  o   エラーコード上位
    //      13.b  i   新しい属性。-1=読み出し
    //      14.l  i   属性を変更するファイル名。_NAMESTS形式
    //      18.l  i   0
    //            o   属性/リザルトステータス
    //      22.l  i   0
    //    (26バイト)
    public void hfuCallChmod () throws M68kException {
      hfuTargetName1 = hfuNamestsToPath (hfsRequest14Namests);
      if (hfuTargetName1 == null) {
        hfsRequest18Result = DOS_ILLEGAL_FILE_NAME;  //ファイル名の指定が間違っている
        hfsState = HFS_STATE_DONE;
        return;
      }
      if (HFS_COMMAND_TRACE && hfsCommandTraceOn) {
        int pc = MainMemory.mmrGetLevelZeroPC ();  //DOSコールにレベル0で入ったときのpc
        System.out.println (LabeledAddress.lblSearch (new StringBuilder (), pc).toString ());
        System.out.printf ("%08x chmod(name=\"%s\",mode=0x%02x)\n",
                           pc, hfuTargetName1, hfsRequest13Mode);
      }
      if (!abuInserted) {  //挿入されていない
        hfsRequest3Error = DEV_RETRY | DEV_ABORT | DEV_INSERT_MEDIA;  //ディスクが入っていません、入れてください
        hfsRequest18Result = -1;
        hfsState = HFS_STATE_DONE;
        return;
      }
      if (hfsRequest13Mode != 255 && abuWriteProtected) {  //モードが設定で書き込みが禁止されている
        hfsRequest3Error = DEV_IGNORE | DEV_RETRY | DEV_ABORT | DEV_CANNOT_WRITE;  //書き込み不可能です
        hfsRequest18Result = -1;
        hfsState = HFS_STATE_DONE;
        return;
      }
      hfsRequest18Result = 0;
      hfsState = HFS_STATE_HOST;
    }  //unit.hfuCallChmod ()

    //unit.hfuCallChmodHost ()
    public void hfuCallChmodHost () {
      File file1 = new File (hfuTargetName1);
      if (!file1.exists ()) {
        hfsRequest18Result = DOS_FILE_NOT_FOUND;  //ファイルまたはディレクトリがない
      } else if (hfsRequest13Mode == 255) {  //取得
        hfsRequest18Result = ((file1.isFile () ? HumanMedia.HUM_ARCHIVE : 0) |
                              (file1.isDirectory () ? HumanMedia.HUM_DIRECTORY : 0) |
                              (file1.isHidden () ? HumanMedia.HUM_HIDDEN : 0) |
                              (!file1.canWrite () ? HumanMedia.HUM_READONLY : 0));
      } else {  //設定
        hfsRequest18Result = 0;
        boolean oldReadonly = !file1.canWrite ();
        boolean newReadonly = (hfsRequest13Mode & HumanMedia.HUM_READONLY) != 0;
        if (oldReadonly != newReadonly) {  //書き込み不可の変更
          try {
            if (!file1.setWritable (!newReadonly)) {  //失敗
              hfsRequest18Result = DOS_CANNOT_WRITE;
            }
          } catch (Exception e) {  //セキュリティなどのエラー
            hfsRequest18Result = DOS_CANNOT_WRITE;
          }
        }
        if (false) {  //その他の変更は無視する
          boolean oldHidden = file1.isHidden ();
          boolean newHidden = (hfsRequest13Mode & HumanMedia.HUM_HIDDEN) != 0;
          if (oldHidden != newHidden) {  //不可視の変更
            hfsRequest18Result = DOS_CANNOT_WRITE;  //変更できない
          }
          if ((hfsRequest13Mode & HumanMedia.HUM_VOLUME) != 0) {  //ボリューム名の設定
            hfsRequest18Result = DOS_CANNOT_WRITE;  //設定できない
          }
        }
      }
      hfsState = HFS_STATE_DONE;
    }  //unit.unit.hfuCallChmodHost()

    //unit.hfuCallFiles ()
    //  0x47 FF4E _FILES ディレクトリエントリの検索(最初)
    //  リクエストヘッダ
    //       0.b  i   26
    //       1.b  i   ユニット番号
    //       2.b  i   コマンドコード。0x47/0xc7
    //       3.b  o   エラーコード下位
    //       4.b  o   エラーコード上位
    //      13.b  i   検索する属性
    //      14.l  i   検索するファイル名。_NAMESTS形式
    //      18.l  i   _FILESのバッファ
    //                     0.b      i   検索する属性                Humanによって設定済み
    //                     1.b      i   内部ドライブ番号(0=A:)      Humanによって設定済み
    //                     2.l[2]   io  ワークエリア
    //                    10.b[8]   i   検索するファイル名          Humanによって設定済み
    //                    18.b[3]   i   検索する拡張子              Humanによって設定済み
    //                    21.b      o   属性
    //                    22.w      o   時刻
    //                    24.w      o   日付
    //                    26.l      o   ファイルサイズ
    //                    30.b[23]  o   ファイル名
    //                  (53バイト)
    //                  (以降は_FILESのバッファのアドレスのbit31を1にしたとき有効)
    //                    53.b[2]   i   'A:'        内部ドライブ名(A:～)
    //                    55.b[65]  i   '\dir\',0   パス(区切りは'\')
    //                   120.b[8]   i   'file    '  ファイル名1(残りは$20または'?')
    //                   128.b[3]   i   '   '       拡張子(残りは$20または'?')
    //                   131.b[10]  i   0           ファイル名2(残りは0)
    //                  (141バイト)
    //            o   リザルトステータス
    //      22.l  i   0
    //    (26バイト)
    //  ファイル名の大文字と小文字を区別しない
    //    ホストに大文字と小文字だけが異なるファイルが複数あると同じ名前にマッチするファイルが複数出力されることがある
    //  ファイル名にSJISに変換できない文字が含まれるものはマッチしない
    //  ファイル名がSJISに変換したとき18+3バイトに収まらないものはマッチしない
    //
    //  ルートディレクトリのとき
    //    .と..があれば削除する
    //      -d----                               .
    //      -d----                               ..
    //    HUMAN.SYSがあればシステム属性を追加する
    //      a--s--                               HUMAN.SYS
    //! 以下は未対応
    //    ボリューム名を追加する
    //      ボリューム名はSJISで18バイトまで
    //      advshr
    //      --v---    [ホストから取得]        0  [hfuRootPath]  n
    //    HUMAN.SYSとCOMMAND.Xがなければ追加する
    //      a--s--  12:00:00  1993-09-15  58496  HUMAN.SYS      n+1
    //      a-----  12:00:00  1993-02-25  28382  COMMAND.X      n+2
    public void hfuCallFiles () throws M68kException {
      //検索するディレクトリ名をホストのディレクトリ名に変換する
      byte[] w = hfuTargetNameArray1;  //ワークエリア
      MC68060.mmuReadByteArray (hfsRequest14Namests, w, 0, 88, XEiJ.regSRS);
      String dirName = hfuTargetName1 = hfuNamestsToPath (w, false);  //ホストのディレクトリ名
      if (hfuTargetName1 == null) {
        hfsRequest18Result = DOS_ILLEGAL_FILE_NAME;  //ファイル名の指定が間違っている
        hfsState = HFS_STATE_DONE;
        return;
      }
      if (HFS_COMMAND_TRACE && hfsCommandTraceOn) {
        int pc = MainMemory.mmrGetLevelZeroPC ();  //DOSコールにレベル0で入ったときのpc
        System.out.println (LabeledAddress.lblSearch (new StringBuilder (), pc).toString ());
        System.out.printf ("%08x files(name=\"%s\",mode=0x%02x)\n",
                           pc, dirName, hfsRequest13Mode);
      }
      if (!abuInserted) {  //挿入されていない
        hfsRequest3Error = DEV_RETRY | DEV_ABORT | DEV_INSERT_MEDIA;  //ディスクが入っていません、入れてください
        hfsRequest18Result = -1;
        hfsState = HFS_STATE_DONE;
        return;
      }
      hfsRequest18Result = 0;
      hfsState = HFS_STATE_HOST;
    }  //unit.hfuCallFiles()

    //unit.hfuCallFilesHost ()
    public void hfuCallFilesHost () {
      byte[] w = hfuTargetNameArray1;  //ワークエリア
      String dirName = hfuTargetName1;  //ホストのディレクトリ名
      File parent = new File (dirName);  //ホストのディレクトリ
      //検索するディレクトリの一覧を取得する
      String[] children = parent.list ();
      if (children == null) {  //ディレクトリがない
        hfsRequest18Result = DOS_DIRECTORY_NOT_FOUND;
        hfsState = HFS_STATE_DONE;
        return;
      }
      //検索するファイル名の順序を入れ替える
      //  主ファイル名1の末尾が'?'で主ファイル名2の先頭が'\0'のときは主ファイル名2を'?'で充填する
      //    1234567?.Xと1234567*.Xが同じになってしまうのは仕様
      //    TwentyOne.x +Tのときは*.*で主ファイル名2も'?'で充填されている
      //  ソース
      //    w[67..87]  検索するファイル名(_NAMESTS形式)
      //      w[67..74]  主ファイル名1。残りは' '
      //      w[75..77]  拡張子。残りは' '
      //      w[78..87]  主ファイル名2。残りは'\0'
      //  デスティネーション
      //    w[21..41]  検索するファイル名
      //      w[21..28]  主ファイル名1。残りは'\0'
      //      w[29..38]  主ファイル名2。残りは'\0'
      //      w[39..41]  拡張子。残りは'\0'
      for (int i = 21; i <= 28; i++) {  //主ファイル名1
        w[i] = w[67 - 21 + i];
      }
      if (w[74] == '?' && w[78] == '\0') {  //主ファイル名1の末尾が'?'で主ファイル名2の先頭が'\0'
        for (int i = 29; i <= 38; i++) {  //主ファイル名2
          w[i] = '?';
        }
      } else {
        for (int i = 29; i <= 38; i++) {  //主ファイル名2
          w[i] = w[78 - 29 + i];
        }
      }
      for (int i = 38; i >= 21 && (w[i] == '\0' || w[i] == ' '); i--) {  //主ファイル名1+主ファイル名2の空き
        w[i] = '\0';
      }
      for (int i = 39; i <= 41; i++) {  //拡張子
        w[i] = w[75 - 39 + i];
      }
      for (int i = 41; i >= 39 && (w[i] == '\0' || w[i] == ' '); i--) {  //拡張子の空き
        w[i] = '\0';
      }
      //検索するファイル名を小文字化する
      for (int i = 21; i <= 41; i++) {
        int c = w[i] & 255;
        if ('A' <= c && c <= 'Z') {  //大文字
          w[i] = (byte) (c | 0x20);  //小文字化する
        } else if (0x81 <= c && c <= 0x9f || 0xe0 <= c && c <= 0xef) {  //SJISの1バイト目
          i++;
        }
      }
      //ディレクトリの一覧から属性とファイル名の条件に合うものを選ぶ
      boolean isRoot = dirName.equals (hfuRootPath);  //true=ルートディレクトリ
      boolean humansysRequired = isRoot;  //true=HUMAN.SYSを追加する必要がある
      boolean commandxRequired = isRoot;  //true=COMMAND.Xを追加する必要がある
      ArrayDeque<byte[]> deque = new ArrayDeque<byte[]> ();  //リスト
      if (isRoot) {  //ルートディレクトリのとき
        if ((hfsRequest13Mode & HumanMedia.HUM_VOLUME) != 0 &&  //ボリューム名が必要なとき
            w[21] == '?' && w[39] == '?') {  //検索するファイル名が*.*のとき
          //ボリューム名を作る
          int l = dirName.length ();  //UTF-16のボリューム名の文字数
          while (2 <= l &&
                 (dirName.charAt (l - 1) == '/' ||
                  dirName.charAt (l - 1) == '\\' ||  //2文字以上で'/'または'/.'で終わっているとき
                  (dirName.charAt (l - 1) == '.' &&
                   (dirName.charAt (l - 2) == '/' ||
                    dirName.charAt (l - 2) == '\\')))) {
            l--;  //1文字削る
          }
          byte[] b = new byte[32];  //バッファ
          //  b[0]      21.b      属性。eladvshr
          //  b[1..2]   22.w      時刻。時<<11|分<<5|秒/2
          //  b[3..4]   24.w      日付。(西暦年-1980)<<9|月<<5|月通日
          //  b[5..8]   26.l      ファイルサイズ
          //  b[9..31]  30.b[23]  ファイル名
          hfuFileInfo (parent, b);  //ディレクトリの更新日時をボリューム名の更新日時にする
          b[0] = HumanMedia.HUM_VOLUME;  //属性をディレクトリからボリューム名に変更する
          b[5] = b[6] = b[7] = b[8] = 0;  //ファイルサイズは0
          int k = 9;
          for (int i = 0; i < l; i++) {
            int c = CharacterCode.chrCharToSJIS[dirName.charAt (i)];  //UTF-16→SJIS変換
            if (c < 0x0100) {
              if (9 + 21 < k + 1) {  //21バイトに収まらない
                k = 0;
                break;
              }
              b[k++] = (byte) c;
            } else {
              if (9 + 21 < k + 2) {  //21バイトに収まらない
                k = 0;
                break;
              }
              b[k++] = (byte) (c >> 8);
              b[k++] = (byte) c;
            }
          }
          if (k == 0) {  //21バイトに収まらなかった
            k = 9;
            //先頭5バイト
            for (int i = 0; i < l; i++) {
              int c = CharacterCode.chrCharToSJIS[dirName.charAt (i)];  //UTF-16→SJIS変換
              if (c < 0x0100) {
                if (9 + 5 < k + 1) {
                  break;
                }
                b[k++] = (byte) c;
              } else {
                if (9 + 5 < k + 2) {
                  break;
                }
                b[k++] = (byte) (c >> 8);
                b[k++] = (byte) c;
              }
            }
            //隙間1バイト
            b[k++] = (byte) '_';
            //末尾15バイト
            int j = 9 + 21;
            for (int i = l - 1; 0 <= i; i--) {
              int c = CharacterCode.chrCharToSJIS[dirName.charAt (i)];  //UTF-16→SJIS変換
              if (c < 0x0100) {
                if (j - 1 < k) {
                  break;
                }
                b[--j] = (byte) c;
              } else {
                if (j - 2 < k) {
                  break;
                }
                b[--j] = (byte) c;
                b[--j] = (byte) (c >> 8);
              }
            }
            if (k < j) {
              while (j < 9 + 21) {
                b[k++] = b[j++];
              }
            } else {
              k = 9 + 21;
            }
          }
          for (int i = k; i <= 31; i++) {
            b[i] = '\0';
          }
          if (b[27] != '\0') {
            b[30] = b[29];
            b[29] = b[28];
            b[28] = b[27];
            b[27] = (byte) '.';
          }
          if (HFS_DEBUG_FILE_INFO) {
            System.out.print ("FILES   ");
            hfuPrintFileInfo (b);
          }
          //リストに追加する
          deque.addLast (b);
        }  //ボリューム名が必要なとき
      }  //ルートディレクトリのとき
    childrenLoop:
      for (String childName : children) {  //UTF-16のファイル名
        int l = childName.length ();  //UTF-16のファイル名の文字数
        if (l == 0) {  //念のため
          continue childrenLoop;
        }
        //ルートディレクトリの処理
        boolean isHumansys = false;  //true=このエントリはルートディレクトリのHUMAN.SYSである
        boolean isCommandx = false;  //true=このエントリはルートディレクトリのCOMMAND.Xである
        if (isRoot) {  //ルートディレクトリのとき
          if (childName.equals (".") || childName.equals ("..")) {  //.と..を除く
            continue childrenLoop;
          }
          isHumansys = childName.equalsIgnoreCase ("HUMAN.SYS");
          if (isHumansys) {
            humansysRequired = false;  //HUMAN.SYSを追加する必要はない
          }
          isCommandx = childName.equalsIgnoreCase ("COMMAND.X");
          if (isCommandx) {
            commandxRequired = false;  //COMMAND.Xを追加する必要はない
          }
        }
        //ファイル名をSJISに変換する
        //  ソース
        //    childName
        //  デスティネーション
        //    b[9..31]  ファイル名(主ファイル名+'.'+拡張子+'\0')
        byte[] b = new byte[32];  //バッファ
        //  b[0]      21.b      属性。eladvshr
        //  b[1..2]   22.w      時刻。時<<11|分<<5|秒/2
        //  b[3..4]   24.w      日付。(西暦年-1980)<<9|月<<5|月通日
        //  b[5..8]   26.l      ファイルサイズ
        //  b[9..31]  30.b[23]  ファイル名
        int k = 9;
        for (int i = 0; i < l; i++) {
          int c = CharacterCode.chrCharToSJIS[childName.charAt (i)];  //UTF-16→SJIS変換
          if (c <= 0x1f ||  //変換できない文字または制御コード
              c == '/' || c == '\\' ||  //ディレクトリ名の区切り
              (c == '-' && i == 0) ||  //ファイル名の先頭に使えない文字
              c == '"' || c == '\'' ||
              //c == '+' ||  //hlk.rがフォルダ名hlk-3.01+14に使っている
              c == ',' ||
              c == ';' || c == '<' || c == '=' || c == '>' ||
              c == '[' || c == ']' ||
              c == '|') {  //ファイル名に使えない文字
            continue childrenLoop;
          }
          if (c < 0x0100) {
            if (k >= 31) {  //長すぎる
              continue childrenLoop;
            }
            b[k++] = (byte) c;
          } else {
            if (k >= 30) {  //長すぎる
              continue childrenLoop;
            }
            b[k++] = (byte) (c >> 8);
            b[k++] = (byte) c;
          }
        }
        for (int i = k; i <= 31; i++) {
          b[i] = '\0';
        }
        //ファイル名を分解する
        //  ソース
        //    b[9..k-1]  ファイル名(主ファイル名+'.'+拡張子)
        //  デスティネーション
        //    w[0..20]  ファイル名
        //      w[0..7]  主ファイル名1。残りは'\0'
        //      w[8..17]  主ファイル名2。残りは'\0'
        //      w[18..20]  拡張子。残りは'\0'
        int m = (b[k - 1] == '.' ? k :  //name.
                 k >= 9 + 3 && b[k - 2] == '.' ? k - 2 :  //name.e
                 k >= 9 + 4 && b[k - 3] == '.' ? k - 3 :  //name.ex
                 k >= 9 + 5 && b[k - 4] == '.' ? k - 4 :  //name.ext
                 k);  //主ファイル名の直後。拡張子があるときは'.'の位置、ないときはk
        if (m > 9 + 18) {  //主ファイル名が長すぎる
          continue childrenLoop;
        }
        {
          int i = 0;
          for (int j = 9; j < m; j++) {  //主ファイル名
            w[i++] = b[j];
          }
          while (i <= 17) {  //主ファイル名の残り
            w[i++] = '\0';
          }
          for (int j = m + 1; j < k; j++) {  //拡張子
            w[i++] = b[j];
          }
          while (i <= 20) {  //拡張子の残り
            w[i++] = '\0';
          }
        }
        //ファイル名を比較する
        //  ソース
        //    w[0..20]  ファイル名
        //      w[0..7]  主ファイル名1。残りは'\0'
        //      w[8..17]  主ファイル名2。残りは'\0'
        //      w[18..20]  拡張子。残りは'\0'
        //  デスティネーション
        //    w[21..41]  検索するファイル名
        //      w[21..28]  主ファイル名1。残りは'\0'
        //      w[29..38]  主ファイル名2。残りは'\0'
        //      w[39..41]  拡張子。残りは'\0'
        {
          int f = 0x20;  //0x00=次のバイトはSJISの2バイト目,0x20=次のバイトはSJISの2バイト目ではない
          for (int i = 0; i <= 20; i++) {
            int c = w[i] & 255;
            int d = w[21 + i] & 255;
            if (d != '?' && ('A' <= c && c <= 'Z' ? c | f : c) != d) {  //検索するファイル名の'?'以外の部分がマッチしない。SJISの2バイト目でなければ小文字化してから比較する
              continue childrenLoop;
            }
            f = f != 0x00 && (0x81 <= c && c <= 0x9f || 0xe0 <= c && c <= 0xef) ? 0x00 : 0x20;  //このバイトがSJISの2バイト目ではなくてSJISの1バイト目ならば次のバイトはSJISの2バイト目
          }
        }
        //属性、時刻、日付、ファイルサイズを取得する
        File file = new File (parent, childName);
        if (0xffffffffL < file.length ()) {  //4GB以上のファイルは検索できないことにする
          continue childrenLoop;
        }
        hfuFileInfo (file, b);
        if (isHumansys) {  //HUMAN.SYSにシステム属性を追加する
          b[0] |= HumanMedia.HUM_SYSTEM;
        }
        if (HFS_DEBUG_FILE_INFO) {
          System.out.print ("FILES   ");
          hfuPrintFileInfo (b);
        }
        if ((b[0] & hfsRequest13Mode) == 0) {  //属性がマッチしない
          continue childrenLoop;
        }
        //リストに追加する
        deque.addLast (b);
      }
      if (false) {
        if (isRoot) {
          if (humansysRequired) {
            //リストの先頭にHUMAN.SYSを追加する
          }
          if (commandxRequired) {
            //リストの先頭にCOMMAND.Xを追加する
          }
        }
      }
      if (deque.isEmpty ()) {  //1つもなかった
        hfsRequest18Result = DOS_FILE_NOT_FOUND;
        hfsState = HFS_STATE_DONE;
        return;
      }
      hfuFilesBufferCounter++;
      hfuFilesBufferToArrayDeque.put (hfuFilesBufferCounter, deque);
      hfsRequest18Result = 0;
      hfsState = HFS_STATE_X68K;
    }  //unit.hfuCallFilesHost()

    //unit.hfuCallFilesX68k ()
    public void hfuCallFilesX68k () throws M68kException {
      MC68060.mmuWriteLongData (hfsRequest18Param + 2, HFU_FILES_MAGIC, XEiJ.regSRS);
      MC68060.mmuWriteLongData (hfsRequest18Param + 6, hfuFilesBufferCounter, XEiJ.regSRS);
      //hfuCallNfiles ();
      //int key = MC68060.mmuReadLongData (hfsRequest18Param + 6, XEiJ.regSRS);
      int key = hfuFilesBufferCounter;
      ArrayDeque<byte[]> deque = hfuFilesBufferToArrayDeque.get (key);
      if (deque == null) {
        hfsRequest18Result = DOS_NO_MORE_FILES;
        hfsState = HFS_STATE_DONE;
        return;
      }
      byte[] b = deque.pollFirst ();
      MC68060.mmuWriteByteArray (hfsRequest18Param + 21, b, 0, 32, XEiJ.regSRS);
      if (deque.isEmpty ()) {  //終わり
        MC68060.mmuWriteLongData (hfsRequest18Param + 2, 0, XEiJ.regSRS);
        MC68060.mmuWriteLongData (hfsRequest18Param + 6, 0, XEiJ.regSRS);
        hfuFilesBufferToArrayDeque.remove (key);
      }
      hfsRequest18Result = 0;
      hfsState = HFS_STATE_DONE;
    }  //unit.hfuCallFilesX68k()

    //unit.hfuCallNfiles ()
    //  0x48 FF4F _NFILES ディレクトリエントリの検索(次)
    //  リクエストヘッダ
    //       0.b  i   26
    //       1.b  i   ユニット番号
    //       2.b  i   コマンドコード。0x48/0xc8
    //       3.b  o   エラーコード下位
    //       4.b  o   エラーコード上位
    //      13.b  i   0
    //      14.l  i   0
    //      18.l  i   _FILESのバッファ
    //            o   リザルトステータス
    //      22.l  i   0
    //    (26バイト)
    public void hfuCallNfiles () throws M68kException {
      if (HFS_COMMAND_TRACE && hfsCommandTraceOn) {
        int pc = MainMemory.mmrGetLevelZeroPC ();  //DOSコールにレベル0で入ったときのpc
        System.out.println (LabeledAddress.lblSearch (new StringBuilder (), pc).toString ());
        System.out.printf ("%08x nfiles()\n",
                           pc);
      }
      if (!abuInserted) {  //挿入されていない
        hfsRequest3Error = DEV_RETRY | DEV_ABORT | DEV_INSERT_MEDIA;  //ディスクが入っていません、入れてください
        hfsRequest18Result = -1;
        hfsState = HFS_STATE_DONE;
        return;
      }
      if (MC68060.mmuReadLongData (hfsRequest18Param + 2, XEiJ.regSRS) != HFU_FILES_MAGIC) {  //マジックがない
        hfsRequest18Result = DOS_NO_MORE_FILES;
        hfsState = HFS_STATE_DONE;
        return;
      }
      int key = MC68060.mmuReadLongData (hfsRequest18Param + 6, XEiJ.regSRS);
      ArrayDeque<byte[]> deque = hfuFilesBufferToArrayDeque.get (key);
      if (deque == null) {
        hfsRequest18Result = DOS_NO_MORE_FILES;
        hfsState = HFS_STATE_DONE;
        return;
      }
      byte[] b = deque.pollFirst ();
      MC68060.mmuWriteByteArray (hfsRequest18Param + 21, b, 0, 32, XEiJ.regSRS);
      if (deque.isEmpty ()) {  //終わり
        MC68060.mmuWriteLongData (hfsRequest18Param + 2, 0, XEiJ.regSRS);
        MC68060.mmuWriteLongData (hfsRequest18Param + 6, 0, XEiJ.regSRS);
        hfuFilesBufferToArrayDeque.remove (key);
      }
      hfsRequest18Result = 0;
      hfsState = HFS_STATE_DONE;
    }  //unit.hfuCallNfiles()

    //unit.hfuCallCreateNewfile ()
    //  0x49 FF3C _CREATE 新規ファイルの作成
    //       FF8B _NEWFILE 新規ファイルの作成(非破壊)
    //  _CREATEは既にあるファイルを削除してから作成
    //  _NEWFILEは既にファイルがあるときはエラー
    //  リクエストヘッダ
    //       0.b  i   26
    //       1.b  i   ユニット番号
    //       2.b  i   コマンドコード。0x49/0xc9
    //       3.b  o   エラーコード下位
    //       4.b  o   エラーコード上位
    //      13.b  i   作成する属性
    //      14.l  i   作成するファイル名。_NAMESTS形式
    //      18.l  i   0=_NEWFILE,1=_CREATE
    //            o   リザルトステータス
    //      22.l  i   FCBテーブルのアドレス
    //    (26バイト)
    public void hfuCallCreateNewfile () throws M68kException {
      byte[] w = hfuTargetNameArray1;  //ワークエリア
      MC68060.mmuReadByteArray (hfsRequest14Namests, w, 0, 88, XEiJ.regSRS);
      hfuTargetName1 = hfuNamestsToPath (w, true);  //ファイル名
      if (hfuTargetName1 == null) {
        hfsRequest18Result = DOS_ILLEGAL_FILE_NAME;  //ファイル名の指定が間違っている
        hfsState = HFS_STATE_DONE;
        return;
      }
      if (HFS_COMMAND_TRACE && hfsCommandTraceOn) {
        int pc = MainMemory.mmrGetLevelZeroPC ();  //DOSコールにレベル0で入ったときのpc
        System.out.println (LabeledAddress.lblSearch (new StringBuilder (), pc).toString ());
        System.out.printf ("%08x %s(fcb=0x%08x,name=\"%s\",mode=0x%02x)\n",
                           pc, hfsRequest18Param == 0 ? "newfile" : "create", hfsRequest22Fcb, hfuTargetName1, hfsRequest13Mode);
      }
      if (!abuInserted) {  //挿入されていない
        hfsRequest3Error = DEV_RETRY | DEV_ABORT | DEV_INSERT_MEDIA;  //ディスクが入っていません、入れてください
        hfsRequest18Result = -1;
        hfsState = HFS_STATE_DONE;
        return;
      }
      if (abuWriteProtected) {  //書き込みが禁止されている
        hfsRequest3Error = DEV_IGNORE | DEV_RETRY | DEV_ABORT | DEV_CANNOT_WRITE;  //書き込み不可能です
        hfsRequest18Result = -1;
        hfsState = HFS_STATE_DONE;
        return;
      }
      hfsRequest18Result = 0;
      hfsState = HFS_STATE_HOST;
    }  //unit.hfuCallCreateNewfile()

    //unit.hfuCallCreateNewfileHost ()
    public void hfuCallCreateNewfileHost () {
      byte[] b = hfuTargetNameArray2;  //ワークエリア
      File file = new File (hfuTargetName1);
      if (file.exists ()) {  //同名のファイルまたはディレクトリがある
        if (hfsRequest18Param == 0) {  //_NEWFILEで同名のファイルまたはディレクトリがある
          hfsRequest18Result = DOS_FILE_EXISTS;  //ファイルが存在している(_NEWFILE,_MAKETMP)
          hfsState = HFS_STATE_DONE;
          return;
        }
        //_CREATEで同名のファイルまたはディレクトリがある
        //  作成日を更新させるためとファイルサイズを0にするために一旦削除する
        //  file.delete()はディレクトリでも空だと削除しようとするのでディレクトリのときfile.delete()を試みてはならない
        if (file.isDirectory () ||  //同名のディレクトリがある
            !file.delete ()) {  //削除できない
          hfsRequest18Result = DOS_CANNOT_WRITE;
          hfsState = HFS_STATE_DONE;
          return;
        }
      }
      RandomAccessFile raf;
      try {
        raf = new RandomAccessFile (file, "rw");  //RandomAccessFileに"w"というモードはない
      } catch (IOException ioe) {
        hfsRequest18Result = DOS_CANNOT_WRITE;
        hfsState = HFS_STATE_DONE;
        return;
      }
      if (hfuFcbToHandle.isEmpty ()) {  //このユニットでオープンされているファイルがなかった
        prevent ();  //イジェクト禁止
      }
      hfuFileInfo (file, b);
      if (HFS_DEBUG_FILE_INFO) {
        System.out.print ("CREATE  ");
        hfuPrintFileInfo (b);
      }
      int fcb = hfsRequest22Fcb;
      HFHandle handle = hfuTargetHandle = hfuNewHandle (fcb, file, raf);
      hfuFcbToHandle.put (fcb, handle);
      hfsRequest18Result = 0;
      hfsState = HFS_STATE_X68K;
    }  //unit.hfuCallCreateNewfileHost()

    //unit.hfuCallCreateNewfileX68k ()
    public void hfuCallCreateNewfileX68k () throws M68kException {
      HFHandle handle = hfuTargetHandle;
      int fcb = handle.hfhFcb;
      byte[] w = hfuTargetNameArray1;  //ワークエリア
      byte[] b = hfuTargetNameArray2;  //ワークエリア
      //FCBを作る
      //  ソース
      //    b[0]       属性。eladvshr
      //    b[1..2]    時刻。時<<11|分<<5|秒/2
      //    b[3..4]    日付。(西暦年-1980)<<9|月<<5|月通日
      //    b[5..8]    ファイルサイズ
      //    w[67..74]  主ファイル名1。残りは' '
      //    w[75..77]  拡張子。残りは' '
      //    w[78..87]  主ファイル名2。残りは'\0'
      //  デスティネーション
      //    f[36..43]  主ファイル名1
      //    f[44..46]  拡張子
      //    f[47]      属性
      //    f[48..57]  主ファイル名2
      //    f[58..59]  時刻。時<<11|分<<5|秒/2
      //    f[60..61]  日付。(西暦年-1980)<<9|月<<5|月通日
      //    f[62..63]  このファイルの最初のクラスタ番号
      //    f[64..67]  ファイルサイズ
      for (int i = 0; i < 8 + 3; i++) {  //主ファイル名1,拡張子
        MC68060.mmuWriteByteData (fcb + 36 + i, w[67 + i], XEiJ.regSRS);
      }
      MC68060.mmuWriteByteData (fcb + 47, b[0], XEiJ.regSRS);  //属性
      for (int i = 0; i < 10; i++) {  //主ファイル名2
        MC68060.mmuWriteByteData (fcb + 48 + i, w[78 + i], XEiJ.regSRS);
      }
      MC68060.mmuWriteLongData (fcb + 58, ByteArray.byaRls (b, 1), XEiJ.regSRS);  //時刻,日付
      MC68060.mmuWriteWordData (fcb + 62, 0, XEiJ.regSRS);  //クラスタ番号
      MC68060.mmuWriteLongData (fcb + 64, ByteArray.byaRls (b, 5), XEiJ.regSRS);  //ファイルサイズ
      hfsRequest18Result = 0;
      hfsState = HFS_STATE_DONE;
    }  //unit.hfuCallCreateNewfileX68k()

    //unit.hfuCallOpen ()
    //  0x4a FF3D _OPEN 存在するファイルのオープン
    //  リクエストヘッダ
    //       0.b  i   26
    //       1.b  i   ユニット番号
    //       2.b  i   コマンドコード。0x4a/0xca
    //       3.b  o   エラーコード下位
    //       4.b  o   エラーコード上位
    //      13.b  i   0
    //      14.l  i   オープンするファイル名。_NAMESTS形式
    //      18.l  i   0
    //            o   リザルトステータス
    //      22.l  i   FCBテーブルのアドレス
    //    (26バイト)
    public void hfuCallOpen () throws M68kException {
      byte[] w = hfuTargetNameArray1;  //ワークエリア
      MC68060.mmuReadByteArray (hfsRequest14Namests, w, 0, 88, XEiJ.regSRS);
      hfuTargetName1 = hfuNamestsToPath (w, true);  //ファイル名
      if (hfuTargetName1 == null) {
        hfsRequest18Result = DOS_ILLEGAL_FILE_NAME;  //ファイル名の指定が間違っている
        hfsState = HFS_STATE_DONE;
        return;
      }
      int fcb = hfsRequest22Fcb;
      hfuTargetOpenMode = MC68060.mmuReadByteZeroData (fcb + 14, XEiJ.regSRS);  //オープンモード。0=読み出し,1=書き込み,2=読み書き
      if (HFS_COMMAND_TRACE && hfsCommandTraceOn) {
        int pc = MainMemory.mmrGetLevelZeroPC ();  //DOSコールにレベル0で入ったときのpc
        System.out.println (LabeledAddress.lblSearch (new StringBuilder (), pc).toString ());
        System.out.printf ("%08x open(fcb=0x%08x,name=\"%s\",mode=0x%02x)\n",
                           pc, hfsRequest22Fcb, hfuTargetName1, hfuTargetOpenMode);
      }
      if (!abuInserted) {  //挿入されていない
        hfsRequest3Error = DEV_RETRY | DEV_ABORT | DEV_INSERT_MEDIA;  //ディスクが入っていません、入れてください
        hfsRequest18Result = -1;
        hfsState = HFS_STATE_DONE;
        return;
      }
      if (hfuTargetOpenMode != 0 && abuWriteProtected) {  //書き込みが禁止されている
        hfsRequest3Error = DEV_IGNORE | DEV_RETRY | DEV_ABORT | DEV_CANNOT_WRITE;  //書き込み不可能です
        hfsRequest18Result = -1;
        hfsState = HFS_STATE_DONE;
        return;
      }
      hfsRequest18Result = 0;
      hfsState = HFS_STATE_HOST;
    }  //unit.hfuCallOpen()

    //unit.hfuCallOpenHost ()
    public void hfuCallOpenHost () {
      byte[] b = hfuTargetNameArray2;  //ワークエリア
      File file = new File (hfuTargetName1);
      //_OPENはファイルが存在しないときモードに関係なくエラーを返さなければならない
      //  LK.Xはテンポラリファイルを_OPEN(2)してみてエラーが出なければ同名のファイルが存在すると判断し、名前を変えて同じことを繰り返す
      //  RandomAccessFile("rw")はファイルが存在しなければ作成するので、その前にファイルが存在することを確認しなければならない
      //  _OPEN(2)→RandomAccessFile("rw")だけではLK.Xを実行した途端にホストのディレクトリが空のテンポラリファイルで埋め尽くされてしまう
      if (!file.exists ()) {  //ファイルが存在しない
        hfsRequest18Result = DOS_FILE_NOT_FOUND;
        hfsState = HFS_STATE_DONE;
        return;
      }
      if (0xffffffffL < file.length ()) {  //4GB以上のファイルはオープンできないことにする
        hfsRequest18Result = DOS_FILE_NOT_FOUND;
        hfsState = HFS_STATE_DONE;
        return;
      }
      hfuFileInfo (file, b);
      if (HFS_DEBUG_FILE_INFO) {
        System.out.print ("OPEN    ");
        hfuPrintFileInfo (b);
      }
      RandomAccessFile raf;
      try {
        raf = new RandomAccessFile (file, hfuTargetOpenMode == 0 ? "r" : "rw");  //RandomAccessFileに"w"というモードはない
        if (HFS_UTF8_WARNING &&
            hfsUTF8WarningOn &&
            !hfsUTF8WarningSet.contains (hfuTargetName1)) {
          hfsUTF8WarningSet.add (hfuTargetName1);
          //ファイルをオープンするとき先頭4KBがUTF-8にマッチしてかつSJIS(X68000)にマッチしなかったら警告する
          int ll = (int) Math.min (4096, file.length ());  //テストする長さ
          byte[] bb = new byte[ll];
          int kk = 0;  //読み込んだ長さ
          while (kk < ll) {
            int tt = raf.read (bb, kk, ll - kk);
            if (tt < 0) {
              break;
            }
            kk += tt;
          }
          raf.seek (0);
          if (!hfuUTF8WarningTest (bb, kk)) {  //UTF-8にマッチしてかつSJIS(X68000)にマッチしない
            System.out.println ("The character encoding of " + hfuTargetName1 + " is UTF-8, not SJIS.");
          }
        }
      } catch (IOException ioe) {
        hfsRequest18Result = DOS_FILE_NOT_FOUND;
        hfsState = HFS_STATE_DONE;
        return;
      }
      if (hfuFcbToHandle.isEmpty ()) {  //このユニットでオープンされているファイルがなかった
        prevent ();  //イジェクト禁止
      }
      int fcb = hfsRequest22Fcb;
      HFHandle handle = hfuTargetHandle = hfuNewHandle (fcb, file, raf);
      hfuFcbToHandle.put (fcb, handle);
      hfsRequest18Result = 0;
      hfsState = HFS_STATE_X68K;
    }  //unit.hfuCallOpenHost()

    //success = hfuUTF8WarningTest (b, k)
    //  b[0..k-1]がUTF-8にマッチしないかSJIS(X68000)にマッチすれば成功、さもなくば失敗
    private static boolean hfuUTF8WarningTest (byte[] b, int k) {
      //UTF-8にマッチするか
      //  0x00-0x7f
      //  0xc0-0xdf 0x80-0xbf
      //  0xe0-0xef 0x80-0xbf 0x80-0xbf
      //  0xf0-0xf7 0x80-0xbf 0x80-0xbf 0x80-0xbf
      for (int i = 0; i < k; i++) {
        int c = b[i] & 0xff;
        if (c <= 0x7f) {  //0x01-0x7f
          if (c != 0x00) {
            continue;
          }
        } else if (c <= 0xbf) {  //0x80-0xbf
        } else if (c <= 0xdf) {  //0xc0-0xdf
          int d = i + 1 < k ? b[i + 1] & 0xff : -1;
          if (d == -1 || (0x80 <= d && d <= 0xbf)) {
            i++;
            continue;
          }
        } else if (c <= 0xef) {  //0xe0-0xef
          int d = i + 1 < k ? b[i + 1] & 0xff : -1;
          int e = i + 2 < k ? b[i + 2] & 0xff : -1;
          if ((d == -1 || (0x80 <= d && d <= 0xbf)) &&
              (e == -1 || (0x80 <= e && e <= 0xbf))) {
            i += 2;
            continue;
          }
        } else if (c <= 0xf7) {  //0xf0-0xf7
          int d = i + 1 < k ? b[i + 1] & 0xff : -1;
          int e = i + 2 < k ? b[i + 2] & 0xff : -1;
          int f = i + 3 < k ? b[i + 3] & 0xff : -1;
          if ((d == -1 || (0x80 <= d && d <= 0xbf)) &&
              (e == -1 || (0x80 <= e && e <= 0xbf)) &&
              (f == -1 || (0x80 <= f && f <= 0xbf))) {
            i += 3;
            continue;
          }
        } else {  //0xf8-0xff
        }
        return true;  //UTF-8にマッチしない
      }
      //UTF-8にマッチした
      //SJIS(X68000)にマッチするか
      //  0x00-0x7f/0xa0-0xdf
      //  0x80/0xf0-0xf5 0x00-0xff
      //  0x81-0x9f/0xe0-0xef 0x40-0x7e/0x80-0xfc
      for (int i = 0; i < k; i++) {
        int c = b[i] & 0xff;
        if ((0x00 <= c && c <= 0x7f) ||
            (0xa0 <= c && c <= 0xdf)) {  //0x00-0x7f/0xa0-0xdf
          if (c != 0x00) {
            continue;
          }
        } else if (c == 0x80 ||
                   (0xf0 <= c && c <= 0xf5)) {  //0x80/0xf0-0xf5
          if (k <= i + 1 || b[i + 1] != 0x00) {
            i++;
            continue;
          }
        } else if ((0x81 <= c && c <= 0x9f) ||
                   (0xe0 <= c && c <= 0xef)) {  //0x81-0x9f/0xe0-0xef
          int d = i + 1 < k ? b[i + 1] & 0xff : -1;
          if (d == -1 || (0x40 <= d && d <= 0xfc && d != 0x7f)) {
            i++;
            continue;
          }
        } else {  //0xf6-0xff
        }
        return false;  //SJIS(X68000)にマッチしない
      }
      return true;  //SJIS(X68000)にマッチする
    }  //hfuUTF8WarningTest

    //unit.hfuCallOpenX68k ()
    public void hfuCallOpenX68k () throws M68kException {
      HFHandle handle = hfuTargetHandle;
      int fcb = handle.hfhFcb;
      byte[] w = hfuTargetNameArray1;  //ワークエリア
      byte[] b = hfuTargetNameArray2;  //ワークエリア
      //FCBを作る
      //  ソース
      //    b[0]       属性。eladvshr
      //    b[1..2]    時刻。時<<11|分<<5|秒/2
      //    b[3..4]    日付。(西暦年-1980)<<9|月<<5|月通日
      //    b[5..8]    ファイルサイズ
      //    w[67..74]  主ファイル名1。残りは' '
      //    w[75..77]  拡張子。残りは' '
      //    w[78..87]  主ファイル名2。残りは'\0'
      //  デスティネーション
      //    f[36..43]  主ファイル名1
      //    f[44..46]  拡張子
      //    f[47]      属性
      //    f[48..57]  主ファイル名2
      //    f[58..59]  時刻。時<<11|分<<5|秒/2
      //    f[60..61]  日付。(西暦年-1980)<<9|月<<5|月通日
      //    f[62..63]  このファイルの最初のクラスタ番号
      //    f[64..67]  ファイルサイズ
      for (int i = 0; i < 8 + 3; i++) {  //主ファイル名1,拡張子
        MC68060.mmuWriteByteData (fcb + 36 + i, w[67 + i], XEiJ.regSRS);
      }
      MC68060.mmuWriteByteData (fcb + 47, b[0], XEiJ.regSRS);  //属性
      for (int i = 0; i < 10; i++) {  //主ファイル名2
        MC68060.mmuWriteByteData (fcb + 48 + i, w[78 + i], XEiJ.regSRS);
      }
      MC68060.mmuWriteLongData (fcb + 58, ByteArray.byaRls (b, 1), XEiJ.regSRS);  //時刻,日付
      MC68060.mmuWriteWordData (fcb + 62, 0, XEiJ.regSRS);  //クラスタ番号
      MC68060.mmuWriteLongData (fcb + 64, ByteArray.byaRls (b, 5), XEiJ.regSRS);  //ファイルサイズ
      hfsRequest18Result = 0;
      hfsState = HFS_STATE_DONE;
    }  //unit.hfuCallOpenX68k()

    //unit.hfuCallClose ()
    //  0x4b FF3E _CLOSE ハンドラのクローズ
    //  リクエストヘッダ
    //       0.b  i   26
    //       1.b  i   ユニット番号
    //       2.b  i   コマンドコード。0x4b/0xcb
    //       3.b  o   エラーコード下位
    //       4.b  o   エラーコード上位
    //      13.b  i   0
    //      14.l  i   0
    //      18.l  i   0
    //            o   リザルトステータス
    //      22.l  i   FCBテーブルのアドレス
    //    (26バイト)
    public void hfuCallClose () throws M68kException {
      if (HFS_COMMAND_TRACE && hfsCommandTraceOn) {
        int pc = MainMemory.mmrGetLevelZeroPC ();  //DOSコールにレベル0で入ったときのpc
        System.out.println (LabeledAddress.lblSearch (new StringBuilder (), pc).toString ());
        System.out.printf ("%08x close(fcb=0x%08x)\n",
                           pc, hfsRequest22Fcb);
      }
      if (!abuInserted) {  //挿入されていない
        hfsRequest3Error = DEV_RETRY | DEV_ABORT | DEV_INSERT_MEDIA;  //ディスクが入っていません、入れてください
        hfsRequest18Result = -1;
        hfsState = HFS_STATE_DONE;
        return;
      }
      int fcb = hfsRequest22Fcb;
      HFHandle handle = hfuTargetHandle = hfuFcbToHandle.remove (fcb);
      if (handle == null) {  //オープンされていない
        //既にクローズされているファイルをクローズしようとしたときはエラーにせず無視する
        hfsRequest18Result = 0;
        hfsState = HFS_STATE_DONE;
        return;
      }
      hfsRequest18Result = 0;
      hfsState = HFS_STATE_HOST;
    }  //unit.hfuCallClose()

    //unit.hfuCallCloseHost ()
    public void hfuCallCloseHost () {
      HFHandle handle = hfuTargetHandle;
      if (hfsRequest18Result != 0) {  //最終更新日時を設定する
        File file = handle.hfhFile;
        if (!file.setLastModified (hfuTargetLastModified)) {  //最終更新日時を設定する
          System.out.println ((Multilingual.mlnJapanese ? "最終更新日時を設定できません: " : "Could not set last modified date and time: ") + handle.toString ());
        }
        if (hfuFcbToHandle.isEmpty ()) {  //このユニットでオープンされているファイルがなくなった
          allow ();  //イジェクト許可
        }
        hfuRecycleHandle (handle);
        handle = hfuTargetHandle = null;
        hfsRequest18Result = 0;
        hfsState = HFS_STATE_DONE;
        return;
      }
      RandomAccessFile raf = handle.hfhRaf;
      //バッファをフラッシュする
      if (handle.hfhDirty) {  //ダーティデータが残っている
        if (HFS_BUFFER_TRACE) {
          System.out.printf ("delaywrite(fcb=0x%08x,name=\"%s\",start=0x%08x,end=0x%08x)\n", handle.hfhFcb, handle.hfhFile.toString(), handle.hfhStart, handle.hfhEnd);
        }
        try {
          handle.hfhRaf.seek (handle.hfhStart);
        } catch (IOException ioe) {
          System.out.println ((Multilingual.mlnJapanese ? "シークエラー: " : "Seek error: ") + handle.toString ());
        }
        try {
          handle.hfhRaf.write (handle.hfhBuffer, 0, (int) (handle.hfhEnd - handle.hfhStart));  //RandomAccessFileのwriteは返却値がない
        } catch (IOException ioe) {
          System.out.println ((Multilingual.mlnJapanese ? "遅延書き込みに失敗しました: " : "Delayed write failed: ") + handle.toString ());
        }
        handle.hfhDirty = false;
      }  //if handle.hfhDirty
      try {
        raf.close ();
      } catch (IOException ioe) {
        System.out.println ((Multilingual.mlnJapanese ? "クローズエラー: " : "Close error: ") + handle.toString ());
      }
      hfsRequest18Result = 0;
      hfsState = HFS_STATE_X68K;
    }  //unit.hfuCallCloseHost()

    //unit.hfuCallCloseX68k ()
    public void hfuCallCloseX68k () throws M68kException {
      HFHandle handle = hfuTargetHandle;
      int fcb = handle.hfhFcb;
      if ((MC68060.mmuReadByteZeroData (fcb + 14, XEiJ.regSRS) & 15) == 0) {  //アクセスモードが読み出しのときは終了
        if (hfuFcbToHandle.isEmpty ()) {  //このユニットでオープンされているファイルがなくなった
          allow ();  //イジェクト許可
        }
        hfuRecycleHandle (handle);
        handle = hfuTargetHandle = null;
        hfsRequest18Result = 0;
        hfsState = HFS_STATE_DONE;
        return;
      }
      if ((MC68060.mmuReadByteZeroData (fcb + 1, XEiJ.regSRS) & 0x40) != 0) {  //_CLOSEしたとき日時を更新する(クローズした日時を書き込む)
        hfuTargetLastModified = System.currentTimeMillis ();
      } else {  //_CLOSEしたとき日時を更新しない(FCBの日時を書き込む)
        int time = MC68060.mmuReadWordZeroData (fcb + 58, XEiJ.regSRS);  //時刻。時<<11|分<<5|秒/2
        int date = MC68060.mmuReadWordZeroData (fcb + 60, XEiJ.regSRS);  //日付。(西暦年-1980)<<9|月<<5|月通日
        hfuTargetLastModified = DnT.dntCmilYearMontMdayHourMinuSeco (
          (date >> 9) + 1980, date >> 5 & 15, date & 31,
          time >> 11, time >> 5 & 63, (time & 31) << 1) - RP5C15.rtcCmilGap;  //FCBの日時はRTCの日時なのでオフセットを引く
      }
      hfsRequest18Result = 1;  //最終更新日時を設定する
      hfsState = HFS_STATE_HOST;
    }  //unit.hfuCallCloseX68k ()

    //unit.hfuCallRead ()
    //  0x4c FF3F _READ ハンドラから指定されたサイズのデータを読み込む
    //  リクエストヘッダ
    //       0.b  i   26
    //       1.b  i   ユニット番号
    //       2.b  i   コマンドコード。0x4c/0xcc
    //       3.b  o   エラーコード下位
    //       4.b  o   エラーコード上位
    //      13.b  i   0
    //      14.l  i   バッファの先頭アドレス
    //      18.l  i   読み込むバイト数
    //            o   実際に読み込んだバイト数/リザルトステータス
    //      22.l  i   FCBテーブルのアドレス
    //    (26バイト)
    public void hfuCallRead () throws M68kException {
      if (HFS_BUFFER_TRACE || (HFS_COMMAND_TRACE && hfsCommandTraceOn)) {
        int pc = MainMemory.mmrGetLevelZeroPC ();  //DOSコールにレベル0で入ったときのpc
        System.out.println (LabeledAddress.lblSearch (new StringBuilder (), pc).toString ());
        System.out.printf ("%08x read(fcb=0x%08x,address=0x%08x,length=0x%08x)\n",
                           pc, hfsRequest22Fcb, hfsRequest14Namests, hfsRequest18Param);
      }
      if (!abuInserted) {  //挿入されていない。オープンされているファイルがあるときはイジェクト禁止なので挿入されていないということは通常はない。強制イジェクトに対応
        hfsRequest3Error = DEV_RETRY | DEV_ABORT | DEV_INSERT_MEDIA;  //ディスクが入っていません、入れてください
        hfsRequest18Result = -1;
        hfsState = HFS_STATE_DONE;
        return;
      }
      int fcb = hfsRequest22Fcb;
      HFHandle handle = hfuTargetHandle = hfuFcbToHandle.get (fcb);
      if (handle == null) {  //オープンされていない
        hfsRequest18Result = DOS_HANDLE_IS_NOT_OPENED;
        hfsState = HFS_STATE_DONE;
        return;
      }
      hfuTargetLength = hfsRequest18Param & 0xffffffffL;  //読み出す長さ
      if (HFS_BUFFER_TRACE) {
        System.out.printf ("length=%d\n", hfuTargetLength);
      }
      hfuTargetPosition = MC68060.mmuReadLongData (fcb + 6, XEiJ.regSRS) & 0xffffffffL;  //シーク位置
      if (HFS_BUFFER_TRACE) {
        System.out.printf ("position=%d\n", hfuTargetPosition);
      }
      hfuTargetFileSize = MC68060.mmuReadLongData (fcb + 64, XEiJ.regSRS) & 0xffffffffL;  //ファイルサイズ
      if (HFS_BUFFER_TRACE) {
        System.out.printf ("fileSize=%d\n", hfuTargetFileSize);
      }
      hfuTargetLength = Math.min (hfuTargetLength, hfuTargetFileSize - hfuTargetPosition);  //読み出す長さと読み出せる長さの短い方を読み出す長さにする
      if (HFS_BUFFER_TRACE) {
        System.out.printf ("length=%d\n", hfuTargetLength);
      }
      if (hfuTargetLength == 0L) {  //読み出す長さが0のときは何もしない。エラーにもしない
        hfsRequest18Result = 0;
        hfsState = HFS_STATE_DONE;
        return;
      }
      hfuTargetAddress = hfsRequest14Namests & 0xffffffffL;  //アドレス
      if (HFS_BUFFER_TRACE) {
        System.out.printf ("address=0x%08x\n", hfuTargetAddress);
      }
      hfuTargetTransferred = 0L;  //読み出した長さ
      if (HFS_BUFFER_TRACE) {
        System.out.printf ("transferred=%d\n", hfuTargetTransferred);
      }
      //  ファイルサイズが0のとき
      //    バッファは空
      //    バッファを充填する必要がない
      //  ファイルサイズが0でないとき
      //    バッファが空のとき
      //      オープンした直後かバッファをフラッシュした直後
      //      バッファを充填する必要がある
      //    バッファが空でないとき
      //      バッファのデータはバッファの先頭からバッファの末尾またはファイルの末尾まで有効
      //      バッファの途中にファイルの末尾がある場合を含めて、
      //      バッファの範囲内でファイルにあってバッファにないデータは存在しない
      //      シーク位置がバッファの範囲内のとき
      //        バッファに読み書きできる部分がある
      //        バッファを充填する必要がない
      //      シーク位置がバッファの範囲外のとき
      //        バッファに読み書きできる部分がない
      //        バッファを充填する必要がある
      //  ファイルサイズが0またはバッファが空でなくてシーク位置がバッファの範囲内のときバッファを充填する必要がない
      //  ファイルサイズが0でなくてバッファが空またはシーク位置がバッファの範囲外のときバッファを充填する必要がある
      if (hfuTargetFileSize == 0 ||  //ファイルサイズが0または
          (0 < handle.hfhEnd &&  //バッファが空でなくて
           handle.hfhStart <= hfuTargetPosition && hfuTargetPosition < handle.hfhStart + HFS_BUFFER_SIZE)) {  //シーク位置がバッファの範囲内のとき
        //バッファを充填する必要がない
        hfsRequest18Result = 0;
        hfsState = HFS_STATE_X68K;
      } else {  //ファイルサイズが0でなくてバッファが空またはシーク位置がバッファの範囲外のとき
        //バッファを充填する必要がある
        hfsRequest18Result = 0;
        hfsState = HFS_STATE_HOST;
      }
    }  //unit.hfuCallRead()

    //unit.hfuCallReadHost ()
    public void hfuCallReadHost () {
      HFHandle handle = hfuTargetHandle;
      RandomAccessFile raf = handle.hfhRaf;
      //バッファをフラッシュする
      if (handle.hfhDirty) {  //ダーティデータが残っている
        if (HFS_BUFFER_TRACE) {
          System.out.printf ("delaywrite(fcb=0x%08x,name=\"%s\",start=0x%08x,end=0x%08x)\n", handle.hfhFcb, handle.hfhFile.toString(), handle.hfhStart, handle.hfhEnd);
        }
        try {
          raf.seek (handle.hfhStart);
        } catch (IOException ioe) {
          System.out.println ((Multilingual.mlnJapanese ? "シークエラー: " : "Seek error: ") + handle.toString ());
        }
        try {
          raf.write (handle.hfhBuffer, 0, (int) (handle.hfhEnd - handle.hfhStart));  //RandomAccessFileのwriteは返却値がない
        } catch (IOException ioe) {
          System.out.println ((Multilingual.mlnJapanese ? "遅延書き込みに失敗しました: " : "Delayed write failed: ") + handle.toString ());
        }
        handle.hfhDirty = false;
      }  //if handle.hfhDirty
      //バッファを充填する
      int ll = (int) Math.min (HFS_BUFFER_SIZE,  //バッファの長さ
                               hfuTargetFileSize - hfuTargetPosition  //シーク位置からファイルの末尾までの長さ
                               );  //バッファに読み出す長さ
      int kk = 0;  //バッファに読み出した長さ
      Arrays.fill (handle.hfhBuffer, (byte) 0);
      handle.hfhStart = hfuTargetPosition;  //バッファの先頭のシーク位置
      handle.hfhEnd = hfuTargetPosition + ll;  //バッファのデータの末尾のシーク位置
      handle.hfhDirty = false;  //ダーティデータなし
      if (HFS_BUFFER_TRACE) {
        System.out.printf ("preread(fcb=0x%08x,name=\"%s\",start=0x%08x,end=0x%08x)\n", handle.hfhFcb, handle.hfhFile.toString(), handle.hfhStart, handle.hfhEnd);
      }
      if (0 < ll) {
        try {
          raf.seek (hfuTargetPosition);
        } catch (IOException ioe) {
          System.out.println ((Multilingual.mlnJapanese ? "シークエラー: " : "Seek error: ") + handle.toString ());
        }
        try {
          while (kk < ll) {
            int tt = raf.read (handle.hfhBuffer, kk, ll - kk);  //今回読み出した長さ。エラーでなければ1以上
            if (tt < 0) {  //途中でEOFに達した。FCBのファイルサイズが合っていれば起こらないはずだが念のため
              //先読みに失敗した
              hfsRequest18Result = -1;
              hfsState = HFS_STATE_DONE;
              return;
            }
            kk += tt;
          }
        } catch (IOException ioe) {
          System.out.println ((Multilingual.mlnJapanese ? "リードエラー: " : "Read error: ") + handle.toString ());
          hfsRequest18Result = -1;
          hfsState = HFS_STATE_DONE;
          return;
        }
      }
      //バッファから読み出す
      hfsRequest18Result = 0;
      hfsState = HFS_STATE_X68K;
    }  //unit.hfuCallReadHost()

    //unit.hfuCallReadX68k ()
    public void hfuCallReadX68k () throws M68kException {
      HFHandle handle = hfuTargetHandle;
      int fcb = handle.hfhFcb;
      //バッファから読み出す
      long tt = Math.min (hfuTargetLength - hfuTargetTransferred,  //転送する残りの長さ
                          handle.hfhEnd - hfuTargetPosition  //シーク位置からバッファのデータの末尾までの長さ
                          );  //バッファから読み出せる長さ
      long t = Math.min (HFS_BUFFER_STEP, tt);  //バッファから読み出す長さ
      if (HFS_BUFFER_TRACE) {
        System.out.printf ("t=%d\n", t);
      }
      MC68060.mmuWriteByteArray ((int) (hfuTargetAddress + hfuTargetTransferred),
                                 handle.hfhBuffer,
                                 (int) (hfuTargetPosition - handle.hfhStart),  //バッファの先頭からシーク位置までのオフセット
                                 (int) t,  //読み出す長さ
                                 XEiJ.regSRS);  //バッファから読み出す
      hfuTargetPosition += t;  //シーク位置を更新する
      if (HFS_BUFFER_TRACE) {
        System.out.printf ("position=%d\n", hfuTargetPosition);
      }
      MC68060.mmuWriteLongData (fcb + 6, (int) hfuTargetPosition, XEiJ.regSRS);  //FCBのシーク位置を更新する
      hfuTargetTransferred += t;  //読み出した長さを更新する
      if (HFS_BUFFER_TRACE) {
        System.out.printf ("transferred=%d\n", hfuTargetTransferred);
      }
      if (hfuTargetLength <= hfuTargetTransferred) {  //終了
        hfsRequest18Result = (int) hfuTargetTransferred;  //読み出した長さ
        hfsState = HFS_STATE_DONE;
        return;
      }
      if (t < tt) {  //バッファを使い切らなかった
        //バッファを充填する必要がない
        hfsRequest18Result = 0;
        hfsState = HFS_STATE_X68K;
      } else {  //バッファを使い切った
        //バッファを充填する必要がある
        hfsRequest18Result = 0;
        hfsState = HFS_STATE_HOST;
      }
    }  //unit.hfuCallReadX68k()

    //unit.hfuCallWrite ()
    //  0x4d FF40 _WRITE ハンドラへ指定されたサイズのデータを書き込む
    //  リクエストヘッダ
    //       0.b  i   26
    //       1.b  i   ユニット番号
    //       2.b  i   コマンドコード。0x4d/0xcd
    //       3.b  o   エラーコード下位
    //       4.b  o   エラーコード上位
    //      13.b  i   0
    //      14.l  i   バッファの先頭アドレス
    //      18.l  i   書き込むバイト数。0=現在のシーク位置から後ろを切り捨てる
    //            o   実際に書き込んだバイト数/リザルトステータス
    //      22.l  i   FCBテーブルのアドレス
    //    (26バイト)
    public void hfuCallWrite () throws M68kException {
      if (HFS_BUFFER_TRACE || (HFS_COMMAND_TRACE && hfsCommandTraceOn)) {
        int pc = MainMemory.mmrGetLevelZeroPC ();  //DOSコールにレベル0で入ったときのpc
        System.out.println (LabeledAddress.lblSearch (new StringBuilder (), pc).toString ());
        System.out.printf ("%08x write(fcb=0x%08x,address=0x%08x,length=0x%08x)\n",
                           pc, hfsRequest22Fcb, hfsRequest14Namests, hfsRequest18Param);
      }
      if (!abuInserted) {  //挿入されていない。オープンされているファイルがあるときはイジェクト禁止なので挿入されていないということは通常はない。強制イジェクトに対応
        hfsRequest3Error = DEV_RETRY | DEV_ABORT | DEV_INSERT_MEDIA;  //ディスクが入っていません、入れてください
        hfsRequest18Result = -1;
        hfsState = HFS_STATE_DONE;
        return;
      }
      if (abuWriteProtected) {  //書き込みが禁止されている。書き込みモードでオープンできたのだからオープンした後に書き込みが禁止されたということ。本来は書き込みモードでオープンされているときは書き込みを禁止できないようにするべき
        hfsRequest3Error = DEV_IGNORE | DEV_RETRY | DEV_ABORT | DEV_CANNOT_WRITE;  //書き込み不可能です
        hfsRequest18Result = -1;
        hfsState = HFS_STATE_DONE;
        return;
      }
      int fcb = hfsRequest22Fcb;
      HFHandle handle = hfuTargetHandle = hfuFcbToHandle.get (fcb);
      if (handle == null) {  //オープンされていない
        hfsRequest18Result = DOS_HANDLE_IS_NOT_OPENED;
        hfsState = HFS_STATE_DONE;
        return;
      }
      hfuTargetLength = hfsRequest18Param & 0xffffffffL;  //書き込む長さ
      if (HFS_BUFFER_TRACE) {
        System.out.printf ("length=%d\n", hfuTargetLength);
      }
      hfuTargetPosition = MC68060.mmuReadLongData (fcb + 6, XEiJ.regSRS) & 0xffffffffL;  //シーク位置
      if (HFS_BUFFER_TRACE) {
        System.out.printf ("position=%d\n", hfuTargetPosition);
      }
      hfuTargetFileSize = MC68060.mmuReadLongData (fcb + 64, XEiJ.regSRS) & 0xffffffffL;  //ファイルサイズ
      if (HFS_BUFFER_TRACE) {
        System.out.printf ("fileSize=%d\n", hfuTargetFileSize);
      }
      if (hfuTargetLength == 0L) {  //書き込む長さが0のときはシーク位置から後ろを切り捨てる
        if (hfuTargetFileSize <= hfuTargetPosition) {  //シーク位置がファイルの末尾なので何もしない
          hfsRequest18Result = 0;
          hfsState = HFS_STATE_DONE;
          return;
        }
        MC68060.mmuWriteLongData (fcb + 64, (int) hfuTargetPosition, XEiJ.regSRS);  //FCBのファイルサイズを更新する
        hfsRequest18Result = 1;  //シーク位置から後ろを切り捨てる
        hfsState = HFS_STATE_HOST;
        return;
      }
      if (0xffffffffL < hfuTargetPosition + hfuTargetLength) {
        hfuTargetLength = 0xffffffffL - hfuTargetPosition;  //ファイルサイズが4GB以上にならないようにする
        if (HFS_BUFFER_TRACE) {
          System.out.printf ("length=%d\n", hfuTargetLength);
        }
        if (hfuTargetLength == 0L) {
          hfsRequest18Result = 0;
          hfsState = HFS_STATE_DONE;
          return;
        }
      }
      hfuTargetAddress = hfsRequest14Namests & 0xffffffffL;  //アドレス
      if (HFS_BUFFER_TRACE) {
        System.out.printf ("address=0x%08x\n", hfuTargetAddress);
      }
      hfuTargetTransferred = 0L;  //書き込んだ長さ
      if (HFS_BUFFER_TRACE) {
        System.out.printf ("transferred=%d\n", hfuTargetTransferred);
      }
      //  ファイルサイズが0のとき
      //    バッファは空
      //    バッファを充填する必要がない
      //  ファイルサイズが0でないとき
      //    バッファが空のとき
      //      オープンした直後かバッファをフラッシュした直後
      //      バッファを充填する必要がある
      //    バッファが空でないとき
      //      バッファのデータはバッファの先頭からバッファの末尾またはファイルの末尾まで有効
      //      バッファの途中にファイルの末尾がある場合を含めて、
      //      バッファの範囲内でファイルにあってバッファにないデータは存在しない
      //      シーク位置がバッファの範囲内のとき
      //        バッファに読み書きできる部分がある
      //        バッファを充填する必要がない
      //      シーク位置がバッファの範囲外のとき
      //        バッファに読み書きできる部分がない
      //        バッファを充填する必要がある
      //  ファイルサイズが0またはバッファが空でなくてシーク位置がバッファの範囲内のときバッファを充填する必要がない
      //  ファイルサイズが0でなくてバッファが空またはシーク位置がバッファの範囲外のときバッファを充填する必要がある
      if (hfuTargetFileSize == 0 ||  //ファイルサイズが0または
          (0 < handle.hfhEnd &&  //バッファが空でなくて
           handle.hfhStart <= hfuTargetPosition && hfuTargetPosition < handle.hfhStart + HFS_BUFFER_SIZE)) {  //シーク位置がバッファの範囲内のとき
        //バッファを充填する必要がない
        hfsRequest18Result = 0;
        hfsState = HFS_STATE_X68K;
      } else {  //ファイルサイズが0でなくてバッファが空またはシーク位置がバッファの範囲外のとき
        //バッファを充填する必要がある
        hfsRequest18Result = 0;  //シーク位置から後ろを切り捨てない
        hfsState = HFS_STATE_HOST;
      }
    }  //unit.hfuCallWrite()

    //unit.hfuCallWriteHost ()
    public void hfuCallWriteHost () {
      HFHandle handle = hfuTargetHandle;
      RandomAccessFile raf = handle.hfhRaf;
      if (hfsRequest18Result != 0) {  //シーク位置から後ろを切り捨てる
        if (handle.hfhStart <= hfuTargetPosition && hfuTargetPosition < handle.hfhEnd) {  //シーク位置がバッファのデータの範囲内のとき
          //バッファのデータのシーク位置から後ろを切り捨てる
          Arrays.fill (handle.hfhBuffer, (int) (hfuTargetPosition - handle.hfhStart), (int) (handle.hfhEnd - handle.hfhStart), (byte) 0);
          handle.hfhEnd = hfuTargetPosition;
          if (HFS_BUFFER_TRACE) {
            System.out.printf ("truncate(fcb=0x%08x,name=\"%s\",start=0x%08x,end=0x%08x)\n", handle.hfhFcb, handle.hfhFile.toString(), handle.hfhStart, handle.hfhEnd);
          }
        }
        try {
          raf.seek (hfuTargetPosition);  //シーク位置
        } catch (IOException ioe) {
          System.out.println ((Multilingual.mlnJapanese ? "シークエラー: " : "Seek error: ") + handle.toString ());
        }
        //  human302  0x0000c6c8
        try {
          raf.setLength (hfuTargetPosition);  //シーク位置から後ろを切り捨てる
        } catch (IOException ioe) {
          System.out.println ((Multilingual.mlnJapanese ? "ファイルの長さを設定できません: " : "Could not set length of file: ") + handle.toString ());
          hfsRequest18Result = -1;
          hfsState = HFS_STATE_DONE;
          return;
        }
        hfsRequest18Result = 0;
        hfsState = HFS_STATE_DONE;
        return;
      }
      //バッファをフラッシュする
      if (handle.hfhDirty) {  //ダーティデータが残っている
        if (HFS_BUFFER_TRACE) {
          System.out.printf ("delaywrite(fcb=0x%08x,name=\"%s\",start=0x%08x,end=0x%08x)\n", handle.hfhFcb, handle.hfhFile.toString(), handle.hfhStart, handle.hfhEnd);
        }
        try {
          raf.seek (handle.hfhStart);
        } catch (IOException ioe) {
          System.out.println ((Multilingual.mlnJapanese ? "シークエラー: " : "Seek error: ") + handle.toString ());
        }
        try {
          raf.write (handle.hfhBuffer, 0, (int) (handle.hfhEnd - handle.hfhStart));  //RandomAccessFileのwriteは返却値がない
        } catch (IOException ioe) {
          System.out.println ((Multilingual.mlnJapanese ? "遅延書き込みに失敗しました: " : "Delayed write failed: ") + handle.toString ());
        }
        handle.hfhDirty = false;
      }  //if handle.hfhDirty
      //バッファを充填する
      int ll = (int) Math.min (HFS_BUFFER_SIZE,  //バッファの長さ
                               hfuTargetFileSize - hfuTargetPosition  //シーク位置からファイルの末尾までの長さ
                               );  //バッファに読み出す長さ。末尾に書き込むときは0
      int kk = 0;  //バッファに読み出した長さ
      Arrays.fill (handle.hfhBuffer, (byte) 0);
      handle.hfhStart = hfuTargetPosition;  //バッファの先頭のシーク位置
      handle.hfhEnd = hfuTargetPosition + ll;  //バッファのデータの末尾のシーク位置
      handle.hfhDirty = false;  //ダーティデータなし
      if (HFS_BUFFER_TRACE) {
        System.out.printf ("preread(fcb=0x%08x,name=\"%s\",start=0x%08x,end=0x%08x)\n", handle.hfhFcb, handle.hfhFile.toString(), handle.hfhStart, handle.hfhEnd);
      }
      if (0 < ll) {
        try {
          raf.seek (hfuTargetPosition);
        } catch (IOException ioe) {
          System.out.println ((Multilingual.mlnJapanese ? "シークエラー: " : "Seek error: ") + handle.toString ());
        }
        try {
          while (kk < ll) {
            int tt = raf.read (handle.hfhBuffer, kk, ll - kk);  //今回読み出した長さ。エラーでなければ1以上
            if (tt < 0) {  //途中でEOFに達した。FCBのファイルサイズが合っていれば起こらないはずだが念のため
              //先読みに失敗した
              hfsRequest18Result = -1;
              hfsState = HFS_STATE_DONE;
              return;
            }
            kk += tt;
          }
        } catch (IOException ioe) {
          System.out.println ((Multilingual.mlnJapanese ? "リードエラー: " : "Read error: ") + handle.toString ());
          hfsRequest18Result = -1;
          hfsState = HFS_STATE_DONE;
          return;
        }
      }
      //バッファに書き込む
      hfsRequest18Result = 0;
      hfsState = HFS_STATE_X68K;
    }  //unit.hfuCallWriteHost()

    //unit.hfuCallWriteX68k ()
    public void hfuCallWriteX68k () throws M68kException {
      HFHandle handle = hfuTargetHandle;
      int fcb = handle.hfhFcb;
      //バッファに書き込む
      long tt = Math.min (hfuTargetLength - hfuTargetTransferred,  //転送する残りの長さ
                          handle.hfhStart + HFS_BUFFER_SIZE - hfuTargetPosition  //シーク位置からバッファの末尾までの長さ
                          );  //バッファに書き込める長さ
      long t = Math.min (HFS_BUFFER_STEP, tt);  //バッファに書き込む長さ
      if (HFS_BUFFER_TRACE) {
        System.out.printf ("t=%d\n", t);
      }
      MC68060.mmuReadByteArray ((int) (hfuTargetAddress + hfuTargetTransferred),
                                handle.hfhBuffer,
                                (int) (hfuTargetPosition - handle.hfhStart),  //バッファの先頭からシーク位置までのオフセット
                                (int) t,  //書き込む長さ
                                XEiJ.regSRS);  //バッファに書き込む
      handle.hfhEnd = Math.max (handle.hfhEnd, hfuTargetPosition + t);  //バッファのデータが長くなる
      handle.hfhDirty = true;  //ダーティデータあり
      if (HFS_BUFFER_TRACE) {
        System.out.printf ("written(fcb=0x%08x,name=\"%s\",start=0x%08x,end=0x%08x,dirty=%b)\n", handle.hfhFcb, handle.hfhFile.toString(), handle.hfhStart, handle.hfhEnd, handle.hfhDirty);
      }
      hfuTargetPosition += t;  //シーク位置を更新する
      if (HFS_BUFFER_TRACE) {
        System.out.printf ("position=%d\n", hfuTargetPosition);
      }
      MC68060.mmuWriteLongData (fcb + 6, (int) hfuTargetPosition, XEiJ.regSRS);  //FCBのシーク位置を更新する
      if (hfuTargetFileSize < hfuTargetPosition) {  //ファイルが長くなった
        hfuTargetFileSize = hfuTargetPosition;  //ファイルサイズを更新する
        if (HFS_BUFFER_TRACE) {
          System.out.printf ("fileSize=%d\n", hfuTargetFileSize);
        }
        MC68060.mmuWriteLongData (fcb + 64, (int) hfuTargetFileSize, XEiJ.regSRS);  //FCBのファイルサイズを更新する
      }
      hfuTargetTransferred += t;  //書き込んだ長さを更新する
      if (HFS_BUFFER_TRACE) {
        System.out.printf ("transferred=%d\n", hfuTargetTransferred);
      }
      if (hfuTargetLength <= hfuTargetTransferred) {  //終了
        hfsRequest18Result = (int) hfuTargetTransferred;  //書き込んだ長さ
        hfsState = HFS_STATE_DONE;
        return;
      }
      if (t < tt) {  //バッファを使い切らなかった
        //バッファを充填する必要がない
        hfsRequest18Result = 0;  //シーク位置から後ろを切り捨てない
        hfsState = HFS_STATE_X68K;
      } else {  //バッファを使い切った
        //バッファを充填する必要がある
        hfsRequest18Result = 0;  //シーク位置から後ろを切り捨てない
        hfsState = HFS_STATE_HOST;
      }
    }  //unit.hfuCallWriteX68k()

    //unit.hfuCallSeek ()
    //  0x4e FF42 _SEEK ハンドラのシーク位置の変更
    //  リクエストヘッダ
    //       0.b  i   26
    //       1.b  i   ユニット番号
    //       2.b  i   コマンドコード。0x4e/0xce
    //       3.b  o   エラーコード下位
    //       4.b  o   エラーコード上位
    //      13.b  i   シークモード(0=先頭から,1=現在位置から,2=終端から)
    //      14.l  i   0
    //      18.l  i   オフセット
    //            o   現在のシーク位置/リザルトステータス
    //      22.l  i   FCBテーブルのアドレス
    //    (26バイト)
    public void hfuCallSeek () throws M68kException {
      if (HFS_COMMAND_TRACE && hfsCommandTraceOn) {
        int pc = MainMemory.mmrGetLevelZeroPC ();  //DOSコールにレベル0で入ったときのpc
        System.out.println (LabeledAddress.lblSearch (new StringBuilder (), pc).toString ());
        System.out.printf ("%08x seek(fcb=0x%08x,offset=0x%08x,mode=0x%02x)\n",
                           pc, hfsRequest22Fcb, hfsRequest18Param, hfsRequest13Mode);
      }
      if (!abuInserted) {  //挿入されていない。オープンされているファイルがあるときはイジェクト禁止なので挿入されていないということは通常はない。強制イジェクトに対応
        hfsRequest3Error = DEV_RETRY | DEV_ABORT | DEV_INSERT_MEDIA;  //ディスクが入っていません、入れてください
        hfsRequest18Result = -1;
        hfsState = HFS_STATE_DONE;
        return;
      }
      int mode = hfsRequest13Mode;  //モード
      long offset64 = (long) hfsRequest18Param;  //オフセット
      int fcb = hfsRequest22Fcb;  //FCB
      HFHandle handle = hfuFcbToHandle.get (fcb);
      if (handle == null) {  //オープンされていない
        hfsRequest18Result = DOS_HANDLE_IS_NOT_OPENED;
        hfsState = HFS_STATE_DONE;
        return;
      }
      long current64 = (long) MC68060.mmuReadLongData (fcb + 6, XEiJ.regSRS);  //現在位置
      long end64 = (long) MC68060.mmuReadLongData (fcb + 64, XEiJ.regSRS);  //終端位置
      //現在位置をゼロ拡張する
      current64 = current64 & 0x00000000ffffffffL;
      //終端位置をゼロ拡張する
      end64 = end64 & 0x00000000ffffffffL;
      if (mode == 0) {  //SEEK_SET(先頭から)のとき
        //オフセットをゼロ拡張する
        offset64 = offset64 & 0x00000000ffffffffL;
        //オフセットを新しい現在位置とする。シーク可能範囲は先頭位置から先頭位置+4GB-1まで
        current64 = offset64;
      } else if (mode == 1) {  //SEEK_CUR(現在位置から)のとき
        //オフセットを符号拡張する
        offset64 = ((offset64 + 0x0000000080000000L) & 0x00000000ffffffffL) - 0x0000000080000000L;
        //現在位置とオフセットの和を新しい現在位置とする。シーク可能範囲は現在位置-2GBから現在位置+2GB-1まで
        current64 = current64 + offset64;
      } else if (mode == 2) {  //SEEK_END(終端から)のとき
        //オフセットを0はゼロ拡張、0以外はイチ拡張する
        offset64 = ((offset64 - 0x0000000000000001L) & 0x00000000ffffffffL) - 0x00000000ffffffffL;
        //終端位置とオフセットの和を新しい現在位置とする。シーク可能範囲は終端位置-4GB+1から終端位置まで
        current64 = end64 + offset64;
      } else {  //モードがSEEK_SET,SEEK_CUR,SEEK_ENDのいずれでもないとき
        //-14(パラメータが無効)を返却値として終了する
        hfsRequest18Result = DOS_INVALID_PARAMETER;  //パラメータが無効
        hfsState = HFS_STATE_DONE;
        return;
      }
      if (current64 < 0x0000000000000000L || end64 < current64) {  //現在位置が先頭位置または終端位置を越えているとき
        //-25(EOFを越えてシークしようとした)を返却値として終了する
        hfsRequest18Result = DOS_SEEK_OVER_EOF;  //EOFを越えてシークしようとした
        hfsState = HFS_STATE_DONE;
        return;
      }
      //現在位置をシークする
      MC68060.mmuWriteLongData (fcb + 6, (int) current64, XEiJ.regSRS);  //現在位置
      //現在位置を返却値として終了する
      hfsRequest18Result = (int) current64;
      hfsState = HFS_STATE_DONE;
    }  //unit.hfuCallSeek()

    //unit.hfuCallFiledate ()
    //  0x4f FF87 _FILEDATE ハンドラの更新日時の取得と設定
    //  リクエストヘッダ
    //       0.b  i   26
    //       1.b  i   ユニット番号
    //       2.b  i   コマンドコード。0x4f/0xcf
    //       3.b  o   エラーコード下位
    //       4.b  o   エラーコード上位
    //      13.b  i   0
    //      14.l  i   0
    //      18.l  i   日付<<16|時刻,0=読み込み
    //            o   日付<<16|時刻/リザルトステータス
    //      22.l  i   FCBテーブルのアドレス
    //    (26バイト)
    //  ディレクトリはオープンできないので日時を変更できない
    public void hfuCallFiledate () throws M68kException {
      if (HFS_COMMAND_TRACE && hfsCommandTraceOn) {
        int pc = MainMemory.mmrGetLevelZeroPC ();  //DOSコールにレベル0で入ったときのpc
        System.out.println (LabeledAddress.lblSearch (new StringBuilder (), pc).toString ());
        System.out.printf ("%08x filedate(fcb=0x%08x,datetime=0x%08x)\n",
                           pc, hfsRequest22Fcb, hfsRequest18Param);
      }
      if (!abuInserted) {  //挿入されていない。オープンされているファイルがあるときはイジェクト禁止なので挿入されていないということは通常はない。強制イジェクトに対応
        hfsRequest3Error = DEV_RETRY | DEV_ABORT | DEV_INSERT_MEDIA;  //ディスクが入っていません、入れてください
        hfsRequest18Result = -1;
        hfsState = HFS_STATE_DONE;
        return;
      }
      int fcb = hfsRequest22Fcb;
      int datetime = hfsRequest18Param;  //日時
      if (datetime == 0) {  //読み出し
        datetime = (MC68060.mmuReadWordZeroData (fcb + 60, XEiJ.regSRS) << 16 |  //日付。(西暦年-1980)<<9|月<<5|月通日
                    MC68060.mmuReadWordZeroData (fcb + 58, XEiJ.regSRS));  //時刻。時<<11|分<<5|秒/2
      } else {  //設定
        //アクセスモードとの整合はHumanによって確認済み
        //  FCBのアクセスモードが読み出しなのに特殊デバイスの_FILEDATEが書き込みで呼び出されることはない
        //FCBの日時は更新されていないのでここで更新する必要がある
        int time = datetime & 0xffff;  //時刻。時<<11|分<<5|秒/2
        int date = datetime >>> 16;  //日付。(西暦年-1980)<<9|月<<5|月通日
        MC68060.mmuWriteWordData (fcb + 58, time, XEiJ.regSRS);  //FCBの時刻。時<<11|分<<5|秒/2
        MC68060.mmuWriteWordData (fcb + 60, date, XEiJ.regSRS);  //FCBの日付。(西暦年-1980)<<9|月<<5|月通日
        //書き込み中のファイルのタイムスタンプを更新してもクローズしたときにクローズした日時が上書きされてしまうのでクローズした後に設定する
        int type = MC68060.mmuModifyByteSignData (fcb + 1, XEiJ.regSRS);  //デバイスタイプ
        if ((type & 0x40) != 0) {
          MC68060.mmuWriteByteData (fcb + 1, type & ~0x40, XEiJ.regSRS);  //_CLOSEするときクローズした日時ではなくFCBの日時を設定する
        }
      }
      hfsRequest18Result = datetime;
      hfsState = HFS_STATE_DONE;
    }  //unit.hfuCallFiledate()

    //unit.hfuCallDskfre ()
    //  0x50 FF36 _DSKFRE ドライブの空容量の取得
    //  リクエストヘッダ
    //       0.b  i   26
    //       1.b  i   ユニット番号
    //       2.b  i   コマンドコード。0x50/0xd0
    //       3.b  o   エラーコード下位
    //       4.b  o   エラーコード上位
    //      13.b  i   0
    //      14.l  i   バッファのアドレス
    //                     0.w    使用可能なクラスタ数
    //                     2.w    総クラスタ数(データ領域のセクタ数/1クラスタあたりのセクタ数)
    //                     4.w    1クラスタあたりのセクタ数
    //                     6.w    1セクタあたりのバイト数
    //                  (8バイト)
    //      18.l  i   0
    //            o   使用可能なバイト数/リザルトステータス
    //      22.l  i   0
    //    (26バイト)
    public void hfuCallDskfre () throws M68kException {
      if (HFS_COMMAND_TRACE && hfsCommandTraceOn) {
        int pc = MainMemory.mmrGetLevelZeroPC ();  //DOSコールにレベル0で入ったときのpc
        System.out.println (LabeledAddress.lblSearch (new StringBuilder (), pc).toString ());
        System.out.printf ("%08x dskfre(buffer=0x%08x)\n",
                           pc, hfsRequest14Namests);
      }
      if (!abuInserted) {  //挿入されていない
        hfsRequest3Error = DEV_RETRY | DEV_ABORT | DEV_INSERT_MEDIA;  //ディスクが入っていません、入れてください
        hfsRequest18Result = -1;
        hfsState = HFS_STATE_DONE;
        return;
      }
      hfsRequest18Result = 0;
      hfsState = HFS_STATE_HOST;
    }  //unit.hfuCallDskfre()

    //unit.hfuCallDskfreHost ()
    public void hfuCallDskfreHost () {
      File file = new File (hfuRootPath);
      hfuTargetTotalSpace = file.getTotalSpace ();
      hfuTargetFreeSpace = file.getFreeSpace ();
      hfsRequest18Result = 0;
      hfsState = HFS_STATE_X68K;
    }  //unit.hfuCallDskfreHost()

    //unit.hfuCallDskfreX68k ()
    public void hfuCallDskfreX68k () throws M68kException {
      int totalSpace = (int) Math.min (0x7fffffffL, hfuTargetTotalSpace);  //2GBを上限とする
      int freeSpace = (int) Math.min (0x7fffffffL, hfuTargetFreeSpace);  //2GBを上限とする
      int clusterBit = Math.max (0, 7 - Integer.numberOfLeadingZeros (totalSpace));
      MC68060.mmuWriteWordData (hfsRequest14Namests, freeSpace >>> clusterBit + 10, XEiJ.regSRS);  //使用可能なクラスタ数
      MC68060.mmuWriteWordData (hfsRequest14Namests + 2, totalSpace >>> clusterBit + 10, XEiJ.regSRS);  //総クラスタ数
      MC68060.mmuWriteWordData (hfsRequest14Namests + 4, 1 << clusterBit, XEiJ.regSRS);  //1クラスタあたりのセクタ数
      MC68060.mmuWriteWordData (hfsRequest14Namests + 6, 1 << 10, XEiJ.regSRS);  //1セクタあたりのバイト数
      hfsRequest18Result = freeSpace;
      hfsState = HFS_STATE_DONE;
      //COMMAND.XのDIRコマンドの容量表示は最大2GBの返却値に頼っている上に(1クラスタあたりのセクタ数*1セクタあたりのバイト数)の上位9ビットを無視するのでまったくあてにならない
    }  //unit.hfuCallDskfreX68k()

    //unit.hfuCallDrvctrl ()
    //  0x51 FF0F _DRVCTRL ドライブコントロール
    //  リクエストヘッダ
    //       0.b  i   26
    //       1.b  i   ユニット番号
    //       2.b  i   コマンドコード。0x51/0xd1
    //       3.b  o   エラーコード下位
    //       4.b  o   エラーコード上位
    //      13.b  i   モード
    //                   0    センス
    //                   1    イジェクト
    //                   2    イジェクト禁止
    //                   3    イジェクト許可
    //                   4    挿入されていないときLED点滅
    //                   9    カレントディレクトリフラッシュ、サーチFATフラッシュ
    //                  10    サーチFATフラッシュ
    //                  16～  拡張モード
    //            o   センス結果
    //      14.l  i   _DRVCTRLのパラメータのドライブ番号の次のアドレス
    //      18.l  i   0
    //            o   リザルトステータス
    //      22.l  i   0
    //    (26バイト)
    public void hfuCallDrvctrl () throws M68kException {
      if (HFS_COMMAND_TRACE && hfsCommandTraceOn) {
        int pc = MainMemory.mmrGetLevelZeroPC ();  //DOSコールにレベル0で入ったときのpc
        System.out.println (LabeledAddress.lblSearch (new StringBuilder (), pc).toString ());
        System.out.printf ("%08x drvctrl(mode=0x%02x,param=0x%08x)\n",
                           pc, hfsRequest13Mode, hfsRequest14Namests);
      }
      switch (hfsRequest13Mode) {
      case 1:  //イジェクト
        if (hfuFcbToHandle.isEmpty ()) {  //オープンされているファイルがない
          eject ();
        }
        break;
      case 2:  //イジェクト禁止
        prevent ();
        break;
      case 3:  //イジェクト許可
        if (hfuFcbToHandle.isEmpty ()) {  //オープンされているファイルがない
          allow ();
        }
        break;
      }
      MC68060.mmuWriteByteData (hfsRequestHeader + 13,
                                (abuInserted ? ABU_INSERTED : 0) |
                                (abuWriteProtected ? ABU_WRITE_PROTECTED : 0) |
                                (abuBuffered ? ABU_BUFFERED : 0) |
                                (abuEjectPrevented ? ABU_EJECT_PREVENTED : 0), XEiJ.regSRS);
      hfsRequest18Result = 0;
      hfsState = HFS_STATE_DONE;
    }  //unit.hfuCallDrvctrl()

    //unit.hfuCallGetdpb ()
    //  0x52 FF32 _GETDPB DPBの取得
    //  リクエストヘッダ
    //       0.b  i   26
    //       1.b  i   ユニット番号
    //       2.b  i   コマンドコード。0x52/0xd2
    //       3.b  o   エラーコード下位
    //       4.b  o   エラーコード上位
    //      13.b  i   0
    //      14.l  i   バッファのアドレス(_GETDPBのパラメータ+2)
    //                                      +呼び出される前に設定される
    //                   DOS                |  +呼び出された後に上書きされる
    //                  FF32  0x52          |  |
    //                    +0        .b      *  -  内部ドライブ番号(0=A:)
    //                    +1        .b      *  -  ユニット番号
    //                    +2    +0  .w      -  -  1セクタあたりのバイト数(0=特殊デバイスドライバ)
    //                    +4    +2  .b      -  -  1クラスタあたりのセクタ数-1
    //                    +5    +3  .b      -  -  クラスタ数をセクタ数に変換するときのシフトカウント
    //                                      -  -  bit7=1のとき2バイトFATの上下のバイトを入れ換える
    //                    +6    +4  .w      -  -  FATの先頭セクタ番号
    //                    +8    +6  .b      -  -  FAT領域の個数
    //                    +9    +7  .b      -  -  1個のFAT領域に使用するセクタ数
    //                   +10    +8  .w      -  -  ルートディレクトリに入るエントリ数
    //                   +12   +10  .w      -  -  データ部の先頭セクタ番号
    //                   +14   +12  .w      -  -  総クラスタ数+3
    //                   +16   +14  .w      -  -  ルートディレクトリの先頭セクタ番号
    //                        (16バイト)
    //                   +18        .l      *  -  デバイスヘッダのアドレス
    //                   +22        .b      -  *  メディアバイト(特殊デバイスのときは内部ドライブ名('a'～))
    //                   +23        .b      -  0  内部DPBテーブル使用フラグ(-1でアクセスなし)
    //                   +24        .l      *  -  次の内部DPBテーブル(-1=終わり)
    //                   +28        .w      -  0  カレントディレクトリのクラスタ番号(0=ルートディレクトリ)
    //                   +30        .b[64]  -  *  カレントディレクトリ
    //                  (94バイト)
    //      18.l  i   0
    //            o   リザルトステータス
    //      22.l  i   0
    //    (26バイト)
    public void hfuCallGetdpb () throws M68kException {
      if (HFS_REPORT_INCOMPATIBLE_COMMAND ||
          (HFS_COMMAND_TRACE && hfsCommandTraceOn)) {
        int pc = MainMemory.mmrGetLevelZeroPC ();  //DOSコールにレベル0で入ったときのpc
        System.out.println (LabeledAddress.lblSearch (new StringBuilder (), pc).toString ());
        System.out.printf ("%08x getdpb(buffer=0x%08x)\n",
                           pc, hfsRequest14Namests);
      }
      MC68060.mmuWriteLongData (hfsRequest14Namests     , 0, XEiJ.regSRS);
      MC68060.mmuWriteLongData (hfsRequest14Namests +  4, 0, XEiJ.regSRS);
      MC68060.mmuWriteLongData (hfsRequest14Namests +  8, 0, XEiJ.regSRS);
      MC68060.mmuWriteLongData (hfsRequest14Namests + 12, 0, XEiJ.regSRS);
      hfsRequest18Result = 0;
      hfsState = HFS_STATE_DONE;
    }  //unit.hfuCallGetdpb()

    //unit.hfuCallDiskred ()
    //  0x53 FFF3 _DISKRED ハンドラから直接読み込む
    //  リクエストヘッダ
    //       0.b  i   26
    //       1.b  i   ユニット番号
    //       2.b  i   コマンドコード。0x53/0xd3
    //       3.b  o   エラーコード下位
    //       4.b  o   エラーコード上位
    //      13.b  i   メディアバイト
    //      14.l  i   バッファの先頭アドレス
    //      18.l  i   セクタ数
    //            o   リザルトステータス
    //      22.l  i   先頭のセクタ番号
    //    (26バイト)
    public void hfuCallDiskred () throws M68kException {
      if (HFS_REPORT_INCOMPATIBLE_COMMAND ||
          (HFS_COMMAND_TRACE && hfsCommandTraceOn)) {
        int pc = MainMemory.mmrGetLevelZeroPC ();  //DOSコールにレベル0で入ったときのpc
        System.out.println (LabeledAddress.lblSearch (new StringBuilder (), pc).toString ());
        System.out.printf ("%08x diskred(buffer=0x%08x,start=0x%08x,count=0x%08x,mediabyte=0x%02x)\n",
                           pc, hfsRequest14Namests, hfsRequest22Fcb, hfsRequest18Param, hfsRequest13Mode);
      }
      int l = hfsRequest18Param << 10;
      for (int i = 0; i < l; i += 4) {
        MC68060.mmuWriteLongData (hfsRequest14Namests + i, 0, XEiJ.regSRS);
      }
      hfsRequest18Result = 0;
      hfsState = HFS_STATE_DONE;
    }  //unit.hfuCallDiskred()

    //unit.hfuCallDiskwrt ()
    //  0x54 FFF4 _DISKWRT ハンドラに直接書き込む
    //  リクエストヘッダ
    //       0.b  i   26
    //       1.b  i   ユニット番号
    //       2.b  i   コマンドコード。0x54/0xd4
    //       3.b  o   エラーコード下位
    //       4.b  o   エラーコード上位
    //      13.b  i   メディアバイト
    //      14.l  i   バッファの先頭アドレス
    //      18.l  i   セクタ数
    //            o   リザルトステータス
    //      22.l  i   先頭のセクタ番号
    //    (26バイト)
    public void hfuCallDiskwrt () throws M68kException {
      if (HFS_REPORT_INCOMPATIBLE_COMMAND ||
          (HFS_COMMAND_TRACE && hfsCommandTraceOn)) {
        int pc = MainMemory.mmrGetLevelZeroPC ();  //DOSコールにレベル0で入ったときのpc
        System.out.println (LabeledAddress.lblSearch (new StringBuilder (), pc).toString ());
        System.out.printf ("%08x diskwrt(buffer=0x%08x,start=0x%08x,count=0x%08x,mediabyte=0x%02x)\n",
                           pc, hfsRequest14Namests, hfsRequest22Fcb, hfsRequest18Param, hfsRequest13Mode);
      }
      hfsRequest18Result = 0;
      hfsState = HFS_STATE_DONE;
    }  //unit.hfuCallDiskwrt()

    //unit.hfuCallSpecialCtrl ()
    //  0x55 FF44 _IOCTRL デバイスによるハンドラの直接制御
    //                  12  ハンドラ番号による特殊コントロール
    //                  13  ドライブ番号による特殊コントロール
    //  リクエストヘッダ
    //       0.b  i   26
    //       1.b  i   ユニット番号
    //       2.b  i   コマンドコード。0x55/0xd5
    //       3.b  o   エラーコード下位
    //       4.b  o   エラーコード上位
    //      13.b  i   0
    //      14.l  i   ポインタ
    //      18.l  i   上位ワード  コマンド
    //            o   リザルトステータス
    //      22.l  i   0
    //    (26バイト)
    public void hfuCallSpecialCtrl () throws M68kException {
      if (HFS_REPORT_INCOMPATIBLE_COMMAND ||
          (HFS_COMMAND_TRACE && hfsCommandTraceOn)) {
        int pc = MainMemory.mmrGetLevelZeroPC ();  //DOSコールにレベル0で入ったときのpc
        System.out.println (LabeledAddress.lblSearch (new StringBuilder (), pc).toString ());
        System.out.printf ("%08x ioctrl(command=0x%08x,param=0x%08x)\n",
                           pc, hfsRequest18Param, hfsRequest14Namests);
      }
      //特殊コントール不可なのでここには来ない
      hfsRequest18Result = DOS_CANNOT_IOCTRL;
      hfsState = HFS_STATE_DONE;
    }  //unit.hfuCallIoctrl()

    //unit.hfuCallFflush ()
    //  0x56 FF0D _FFLUSH バッファフラッシュ
    //  リクエストヘッダ
    //       0.b  i   26
    //       1.b  i   ユニット番号
    //       2.b  i   コマンドコード。0x56/0xd6
    //       3.b  o   エラーコード下位
    //       4.b  o   エラーコード上位
    //      13.b  i   0
    //      14.l  i   0
    //      18.l  i   0
    //            o   リザルトステータス
    //      22.l  i   0
    //    (26バイト)
    public void hfuCallFflush () throws M68kException {
      if (HFS_COMMAND_TRACE && hfsCommandTraceOn) {
        int pc = MainMemory.mmrGetLevelZeroPC ();  //DOSコールにレベル0で入ったときのpc
        System.out.println (LabeledAddress.lblSearch (new StringBuilder (), pc).toString ());
        System.out.printf ("%08x fflush()\n",
                           pc);
      }
      hfsRequest18Result = 0;
      hfsState = HFS_STATE_HOST;
    }  //unit.hfuCallFflush()

    //unit.hfuCallFflushHost ()
    public void hfuCallFflushHost () {
      for (HFHandle handle : hfuFcbToHandle.values ()) {
        //バッファをフラッシュする
        if (handle.hfhDirty) {  //ダーティデータが残っている
          if (HFS_BUFFER_TRACE) {
            System.out.printf ("delaywrite(fcb=0x%08x,name=\"%s\",start=0x%08x,end=0x%08x)\n", handle.hfhFcb, handle.hfhFile.toString(), handle.hfhStart, handle.hfhEnd);
          }
          RandomAccessFile raf = handle.hfhRaf;
          try {
            raf.seek (handle.hfhStart);
          } catch (IOException ioe) {
            System.out.println ((Multilingual.mlnJapanese ? "シークエラー: " : "Seek error: ") + handle.toString ());
          }
          try {
            raf.write (handle.hfhBuffer, 0, (int) (handle.hfhEnd - handle.hfhStart));  //RandomAccessFileのwriteは返却値がない
          } catch (IOException ioe) {
            System.out.println ((Multilingual.mlnJapanese ? "遅延書き込みに失敗しました: " : "Delayed write failed: ") + handle.toString ());
          }
          handle.hfhDirty = false;
        }  //if handle.hfhDirty
        handle.hfhStart = 0L;
        handle.hfhEnd = 0L;
      }  //for handle
      hfsRequest18Result = 0;
      hfsState = HFS_STATE_DONE;
    }  //unit.hfuCallFflushHost()

    //unit.hfuCallMediacheck ()
    //  0x57 mediacheck メディアチェック
    //  リクエストヘッダ
    //       0.b  i   26
    //       1.b  i   ユニット番号
    //       2.b  i   コマンドコード。0x57/0xd7
    //       3.b  o   エラーコード下位
    //       4.b  o   エラーコード上位
    //      13.b  i   0
    //      14.l  i   0
    //      18.l  i   0
    //            o   最下位バイト  0=メディア交換なし,-1=メディア交換あり
    //      22.l  i   0
    //    (26バイト)
    public void hfuCallMediacheck () throws M68kException {
      if (HFS_COMMAND_TRACE && hfsCommandTraceOn) {
        int pc = MainMemory.mmrGetLevelZeroPC ();  //DOSコールにレベル0で入ったときのpc
        System.out.println (LabeledAddress.lblSearch (new StringBuilder (), pc).toString ());
        System.out.printf ("%08x mediacheck()\n",
                           pc);
      }
      hfsRequest18Result = hasBeenEjected () ? -1 : 0;
      hfsState = HFS_STATE_DONE;
    }  //unit.hfuCallMediacheck()

    //unit.hfuCallLock ()
    //  0x58 FF8C _LOCK ハンドラのロックの制御
    //  リクエストヘッダ
    //       0.b  i   26
    //       1.b  i   ユニット番号
    //       2.b  i   コマンドコード。0x58/0xd8
    //       3.b  o   エラーコード下位
    //       4.b  o   エラーコード上位
    //      13.b  i   0=lock,1=unlock
    //      14.l  i   lock/unlockするバイト数
    //      18.l  i   lock/unlockするシーク位置
    //            o   リザルトステータス
    //      22.l  i   FCBテーブルのアドレス
    //    (26バイト)
    public void hfuCallLock () throws M68kException {
      if (HFS_REPORT_INCOMPATIBLE_COMMAND ||
          (HFS_COMMAND_TRACE && hfsCommandTraceOn)) {
        int pc = MainMemory.mmrGetLevelZeroPC ();  //DOSコールにレベル0で入ったときのpc
        System.out.println (LabeledAddress.lblSearch (new StringBuilder (), pc).toString ());
        System.out.printf ("%08x %s(fcb=0x%08x,offset=0x%08x,length=0x%08x)\n",
                           pc, hfsRequest13Mode == 0 ? "lock" : "unlock", hfsRequest22Fcb, hfsRequest18Param, hfsRequest14Namests);
      }
      if (true) {
        hfsRequest18Result = -1;
      } else {
        hfsRequest18Result = 0;
      }
      hfsState = HFS_STATE_DONE;
    }  //unit.hfuCallLock()

    //hfuFileInfo (file, b)
    //  ファイルの情報の取得(ホスト側)
    //  属性、時刻、日付、ファイルサイズを読み取る
    //  ファイル名は設定済み
    //    b[0]     属性。eladvshr
    //    b[1..2]  時刻。時<<11|分<<5|秒/2
    //    b[3..4]  日付。(西暦年-1980)<<9|月<<5|月通日
    //    b[5..8]  ファイルサイズ
    //    b[9..31]  ファイル名
    private void hfuFileInfo (File file, byte[] b) {
      //属性
      b[0] = (byte) ((file.isFile () ? HumanMedia.HUM_ARCHIVE : 0) |
                     (file.isDirectory () ? HumanMedia.HUM_DIRECTORY : 0) |
                     (file.isHidden () ? HumanMedia.HUM_HIDDEN : 0) |
                     (!file.canWrite () ? HumanMedia.HUM_READONLY : 0));
      //更新日時
      long dttm = DnT.dntDttmCmil (file.lastModified () + RP5C15.rtcCmilGap);  //西暦年<<42|月<<38|月通日<<32|時<<22|分<<16|秒<<10|ミリ秒。FCBの日時はRTCの日時なのでオフセットを加える
      //時刻
      int time = DnT.dntHourDttm (dttm) << 11 | DnT.dntMinuDttm (dttm) << 5 | DnT.dntSecoDttm (dttm) >> 1;  //時<<11|分<<5|秒/2
      b[1] = (byte) (time >> 8);
      b[2] = (byte) time;
      //日付
      int date = DnT.dntYearDttm (dttm) - 1980 << 9 | DnT.dntMontDttm (dttm) << 5 | DnT.dntMdayDttm (dttm);  //(西暦年-1980)<<9|月<<5|月通日
      b[3] = (byte) (date >> 8);
      b[4] = (byte) date;
      //ファイルサイズ
      int size = (int) Math.min (0xffffffffL, file.length ());  //4GB未満。intでマイナスのときは2GB以上
      b[5] = (byte) (size >> 24);
      b[6] = (byte) (size >> 16);
      b[7] = (byte) (size >> 8);
      b[8] = (byte) size;
    }  //hfuFileInfo(File,byte[])

    //hfuPrintFileInfo (b)
    //  ファイルの情報の表示
    //    b[0]     属性。eladvshr
    //    b[1..2]  時刻。時<<11|分<<5|秒/2
    //    b[3..4]  日付。(西暦年-1980)<<9|月<<5|月通日
    //    b[5..8]  ファイルサイズ
    //    b[9..31]  ファイル名
    public void hfuPrintFileInfo (byte[] b) {
      StringBuilder sb = new StringBuilder ();
      //    b[0]     属性。eladvshr
      int attr = b[0] & 255;
      sb.append ((attr & HumanMedia.HUM_EXECUTABLE) != 0 ? 'e' : '-');
      sb.append ((attr & HumanMedia.HUM_LINK      ) != 0 ? 'l' : '-');
      sb.append ((attr & HumanMedia.HUM_ARCHIVE   ) != 0 ? 'a' : '-');
      sb.append ((attr & HumanMedia.HUM_DIRECTORY ) != 0 ? 'd' : '-');
      sb.append ((attr & HumanMedia.HUM_VOLUME    ) != 0 ? 'v' : '-');
      sb.append ((attr & HumanMedia.HUM_SYSTEM    ) != 0 ? 's' : '-');
      sb.append ((attr & HumanMedia.HUM_HIDDEN    ) != 0 ? 'h' : '-');
      sb.append ((attr & HumanMedia.HUM_READONLY  ) != 0 ? 'r' : '-');
      sb.append ("  ");
      //    b[3..4]  日付。(西暦年-1980)<<9|月<<5|月通日
      int date = (char) (b[3] << 8 | b[4] & 255);
      XEiJ.fmtSB02u (XEiJ.fmtSB02u (XEiJ.fmtSB04u (sb, (date >>> 9) + 1980).append ('-'), date >>> 5 & 15).append ('-'), date & 31);
      sb.append ("  ");
      //    b[1..2]  時刻。時<<11|分<<5|秒/2
      int time = (char) (b[1] << 8 | b[2] & 255);
      XEiJ.fmtSB02u (XEiJ.fmtSB02u (XEiJ.fmtSB02u (sb, time >>> 11).append (':'), time >>> 5 & 63).append (':'), time << 1 & 63);
      sb.append ("  ");
      //    b[5..8]  ファイルサイズ
      if ((attr & HumanMedia.HUM_DIRECTORY) != 0) {
        sb.append ("     <dir>");
      } else if ((attr & HumanMedia.HUM_VOLUME) != 0) {
        sb.append ("     <vol>");
      } else {
        int size = (b[5] << 8 | b[6] & 255) << 16 | (char) (b[7] << 8 | b[8] & 255);
        XEiJ.fmtSBnd (sb, 10, size);
      }
      sb.append ("  ");
      //    b[9..31]  ファイル名
      int l = b.length;
      for (int i = 9; i < l; i++) {
        int s = b[i] & 255;
        char c;
        if (0x81 <= s && s <= 0x9f || 0xe0 <= s && s <= 0xef) {  //SJISの2バイトコードの1バイト目
          int t = i + 1 < l ? b[i + 1] & 255 : 0;
          if (0x40 <= t && t != 0x7f && t <= 0xfc) {  //SJISの2バイトコードの2バイト目
            c = CharacterCode.chrSJISToChar[s << 8 | t];  //2バイトで変換する
            if (c == 0) {  //対応する文字がない
              c = '※';
            }
            i++;
          } else {  //SJISの2バイトコードの2バイト目ではない
            c = '.';  //SJISの2バイトコードの1バイト目ではなかった
          }
        } else {  //SJISの2バイトコードの1バイト目ではない
          c = CharacterCode.chrSJISToChar[s];  //1バイトで変換する
          if (c < 0x20 || c == 0x7f) {  //対応する文字がないまたは制御コード
            c = '.';
          }
        }
        sb.append (c);
      }
      System.out.println (sb.toString ());
    }  //hfuPrintFileInfo(byte[]);

    //path = hfuNamestsToPath (namests)
    //path = hfuNamestsToPath (namests, full)
    //path = hfuNamestsToPath (ns, full)
    //  namestsのパスをホストマシンのパスに変換する(X68000側の処理)
    //  namests
    //    +2                                                   +67                   +75            +78                   +88
    //    0x09,ディレクトリ名,0x09,ディレクトリ名,0x09,0x00,…,主ファイル名1,0x20,…,拡張子,0x20,…,主ファイル名2,0x00,…,
    //    0x09,ディレクトリ名,0x09,0x00,…                    ,
    //    0x00,…                                             ,
    //  0x09(の並び)をディレクトリ名の区切りとみなす
    //    0x09が2つ以上連続しているときは1つとみなす
    //    パスの先頭と末尾の0x09はあってもなくても同じ
    //  0x00または+67をディレクトリ名の末尾とみなす
    //    最後のディレクトリ名が+67に達しているときはそこで打ち切る
    //  主ファイル名1と主ファイル名2にSJISの1バイト目と2バイト目が跨る場合がある
    //  ディレクトリ名またはファイル名が'.'の並びのときエラーにする
    //    "."と".."はHuman68kが取り除くのでここには来ない
    //    万が一".."が含まれていると指定されたルートディレクトリから逸脱するおそれがある
    //    "..."や"...."はWindowsで使えないのでエラーにする
    //  エラーのときnullを返すのでDOS_ILLEGAL_FILE_NAMEでエラー終了すること
    private String hfuNamestsToPath (int namests) throws M68kException {
      byte[] w = new byte[88];
      MC68060.mmuReadByteArray (namests, w, 0, 88, XEiJ.regSRS);
      return hfuNamestsToPath (w, true);
    }  //hfuNamestsToPath(int)
    private String hfuNamestsToPath (int namests, boolean full) throws M68kException {
      byte[] w = new byte[88];
      MC68060.mmuReadByteArray (namests, w, 0, 88, XEiJ.regSRS);
      return hfuNamestsToPath (w, full);
    }  //hfuNamestsToPath(int,boolean)
    private String hfuNamestsToPath (byte[] ns, boolean full) throws M68kException {
      if (HFS_COMMAND_TRACE && hfsCommandTraceOn) {
        StringBuilder sb = new StringBuilder ();
        sb.append ("\"");
        for (int i = 0; i < 88; i++) {
          if (i == 2 || i == 67 || i == 75 || i == 78) {
            sb.append ("\"+\"");
          }
          int c = ns[i] & 0xff;
          if (c == '\b') {
            sb.append ("\\b");
          } else if (c == '\f') {
            sb.append ("\\f");
          } else if (c == '\n') {
            sb.append ("\\n");
          } else if (c == '\r') {
            sb.append ("\\r");
          } else if (c == '\t') {
            sb.append ("\\t");
          } else if (c == '\0') {
            sb.append ("\\0");
          } else if (0x20 <= c && c <= 0x7e) {
            sb.append ((char) c);
          } else {
            sb.append (String.format ("\\x%02x", c));
          }
        }
        sb.append ("\"");
        String s = sb.toString ();
        int pc = MainMemory.mmrGetLevelZeroPC ();  //DOSコールにレベル0で入ったときのpc
        System.out.printf ("%08x hfuNamestsToPath(ns=%s,full=%b)\n", pc, s, full);
      }
      byte[] bb = new byte[88];
      int k = 0;
      for (int i = 2; i < 67; ) {
        for (; i < 67 && ns[i] == 0x09; i++) {  //0x09の並びを読み飛ばす
        }
        if (i >= 67 || ns[i] == 0x00) {  //ディレクトリ名がなかった
          break;
        }
        bb[k++] = 0x2f;  //ディレクトリ名の手前の'/'
        for (; i < 67 && ns[i] != 0x00 && ns[i] != 0x09; i++) {
          bb[k++] = ns[i];  //ディレクトリ名
        }
      }
      if (full) {  //主ファイル名を展開する
        bb[k++] = 0x2f;  //主ファイル名の手前の'/'
        for (int i = 67; i < 75; i++) {
          bb[k++] = ns[i];  //主ファイル名1
        }
        for (int i = 78; i < 88; i++) {
          bb[k++] = ns[i];  //主ファイル名2
        }
        for (; k > 0 && bb[k - 1] == 0x00; k--) {  //主ファイル名2の末尾の0x00を切り捨てる
        }
        for (; k > 0 && bb[k - 1] == 0x20; k--) {  //主ファイル名1の末尾の0x20を切り捨てる
        }
        bb[k++] = 0x2e;  //拡張子の手前の'.'
        for (int i = 75; i < 78; i++) {
          bb[k++] = ns[i];  //拡張子
        }
        for (; k > 0 && bb[k - 1] == 0x20; k--) {  //拡張子の末尾の0x20を切り捨てる
        }
        for (; k > 0 && bb[k - 1] == 0x2e; k--) {  //主ファイル名の末尾の0x2eを切り捨てる
        }
      }
      StringBuilder sb = new StringBuilder (hfuRootPath);
      int h = 0x00;  //繰り越した文字。SJISの1バイト目に限る
      int dot = 0;  //'/'でdot=0、'.'でdot|=1、その他でdot|=2。'/'の手前または末尾でdot==1のときディレクトリ名またはファイル名が'.'の並びなのでエラー
      for (int i = 0; i < k; i++) {
        int l = bb[i] & 255;
        if (h != 0x00 && 0x40 <= l && l != 0x7f && l <= 0xfc) {  //SJISの1バイト目が繰り越されており、今回の文字はSJISの2バイト目
          int c = CharacterCode.chrSJISToChar[h << 8 | l];
          if (c != 0x0000) {  //SJISで変換できる
            sb.append ((char) c);
          } else {  //SJISだが変換できない
            XEiJ.fmtHex2 (XEiJ.fmtHex2 (sb.append ('%'), h).append ('%'), l);  //%XX%XX
          }
          h = 0x00;
          dot |= 2;
        } else {  //SJISの1バイト目が繰り越されていないか、今回の文字はSJISの2バイト目ではない
          if (h != 0x00) {  //SJISの1バイト目が繰り越されているが、今回の文字はSJISの2バイト目ではない
            XEiJ.fmtHex2 (sb.append ('%'), h);  //%XX
            h = 0x00;
          }
          if (0x81 <= l && l <= 0x9f || 0xe0 <= l && l <= 0xef) {  //今回の文字はSJISの1バイト目
            h = l;  //繰り越す
          } else {  //今回の文字はSJISの1バイト目ではない
            int c = CharacterCode.chrSJISToChar[l];
            if (0x20 <= c && c != 0x7f) {  //今回の文字はANK
              sb.append ((char) c);
            } else {  //今回の文字はSJISの1バイト目ではなく、ANKでもない
              XEiJ.fmtHex2 (sb.append ('%'), l);  //%XX
            }
            if (c == '/') {
              if (dot == 1) {  //ディレクトリ名が'.'の並び
                return null;  //エラー
              }
              dot = 0;
            } else if (c == '.') {
              dot |= 1;
            } else {
              dot |= 2;
            }
          }
        }
      }
      if (h != 0x00) {  //SJISの1バイト目が繰り越されている
        XEiJ.fmtHex2 (sb.append ('%'), h);  //%XX
      }
      if (dot == 1) {  //ファイル名が'.'の並び
        return null;  //エラー
      }
      return sb.toString ();
    }  //hfuNamestsToPath(byte[],boolean)

  }  //class HFUnit



}  //class HFS



