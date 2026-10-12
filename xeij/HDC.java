//========================================================================================
//  HDC.java
//    en:SASI hard disk controller
//    ja:SASIハードディスクコントローラ
//  Copyright (C) 2003-2026 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//  0x00e96020  HDC SASI HDコントローラ
//  0x00e96020  SPC 内蔵SCSIプロトコルコントローラ
//----------------------------------------------------------------------------------------

package xeij;

import java.awt.event.*;  //ActionEvent,ActionListener,ComponentAdapter,ComponentEvent,ComponentListener,FocusAdapter,FocusEvent,FocusListener,InputEvent,KeyAdapter,KeyEvent,KeyListener,MouseAdapter,MouseEvent,MouseListener,MouseMotionAdapter,MouseWheelEvent,WindowAdapter,WindowEvent,WindowListener,WindowStateListener
import java.io.*;  //BufferedInputStream,BufferedOutputStream,BufferedReader,BufferedWriter,File,FileInputStream,FileNotFoundException,FileReader,InputStream,InputStreamReader,IOException,OutputStreamWriter,RandomAccessFile
import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,Timer,TimerTask,TreeMap
import java.util.zip.*;  //CRC32,Deflater,GZIPInputStream,GZIPOutputStream,ZipEntry,ZipInputStream
import javax.swing.*;  //AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JButton,JCheckBox,JCheckBoxMenuItem,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException

public class HDC {

  public static final boolean HDC_DEBUG_TRACE = false;
  public static final boolean HDC_DEBUG_COMMAND = false;
  public static final boolean HDC_DEBUG_UNIMPLEMENTED_COMMAND = true;

  public static final int HDC_BASE           = 0x00e96000;
  public static final int HDC_DATA_PORT      = 0x00e96001;  //データポート
  public static final int HDC_STATUS_PORT    = 0x00e96003;  //ステータスポート
  public static final int HDC_RESET_PORT     = 0x00e96005;  //リセットポート
  public static final int HDC_SELECTION_PORT = 0x00e96007;  //セレクションポート

  public static final int HDC_STATUS_MESSAGE = 0b10000;
  public static final int HDC_STATUS_C_D     = 0b01000;
  public static final int HDC_STATUS_COMMAND = 0b01000;
  public static final int HDC_STATUS_DATA    = 0b00000;
  public static final int HDC_STATUS_I_O     = 0b00100;
  public static final int HDC_STATUS_OUTPUT  = 0b00000;
  public static final int HDC_STATUS_INPUT   = 0b00100;
  public static final int HDC_STATUS_BUSY    = 0b00010;
  public static final int HDC_STATUS_FREE    = 0b00000;
  public static final int HDC_STATUS_REQUEST = 0b00001;

  public static final String[] HDC_COMMAND_NAME = (
    "TestDriveReady"                + "," +  //0x00
    "Recalibrate"                   + "," +  //0x01
    ""                              + "," +  //0x02
    "RequestSenseStatus"            + "," +  //0x03
    "FormatDrive"                   + "," +  //0x04
    "CheckTrackFormat"              + "," +  //0x05
    "FormatBlock"                   + "," +  //0x06
    "BadTrackFormat"                + "," +  //0x07
    "Read"                          + "," +  //0x08
    ""                              + "," +  //0x09
    "Write"                         + "," +  //0x0a
    "Seek"                          + "," +  //0x0b
    "InitializeDriveCharacteristic" + "," +  //0x0c
    "LastCorrectedBurstLength"      + "," +  //0x0d
    "AssignTrack"                   + "," +  //0x0e
    "WriteSectorBuffer"             + "," +  //0x0f
    "ReadSectorBuffer"              + "," +  //0x10
    //123456789abcdef0123456789abcdef
    ",,,,,,,,,,,,,,,"                     +  //0x11..0x1f
    ",,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,"    +  //0x20..0x3f
    ",,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,"    +  //0x40..0x5f
    ",,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,"    +  //0x60..0x7f
    ",,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,"    +  //0x80..0x9f
    ",,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,"    +  //0xa0..0xbf
    ",,"                                  +  //0xc0..0xc1
    "AssignDrive"                   + "," +  //0xc2
    ",,,,,,,,,,,,,,,,,,,,,,,,,,,,,"       +  //0xc3..0xdf
    ",,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,"        //0xe0..0xff
    ).split (",", 256);  //splitで末尾の空要素が削除されるのでHDC_COMMAND_NAME[cmd]が存在しないとき""とみなす処理が必要

  //転送開始遅延
  //  WRITEコマンドを実行するには、コマンドとデータの両方をデータポートに書き込む。
  //  コマンドをMPUで、データをDMACで書き込むには、その間にDMACを始動しなければならない。
  //  コマンドの6バイト目を受け取った瞬間にデータの1バイト目の外部転送要求を出してしまうと、
  //  DMACがREQの立ち下がりを見逃してしまうので、少し間隔をあける必要がある。
  public static final long HDC_DELAY_TIME = XEiJ.TMR_FREQ * 100 / 1000000;  //100us
  public static final TickerQueue.Ticker hdcDelayTicker = new TickerQueue.Ticker () {
    @Override protected void tick () {
      HD63450.dmaFallREQ (1);
    }
  };


  //メニュー
  public static JMenu hdcMenu;

  //ファイルフィルタ
  public static javax.swing.filechooser.FileFilter hdcFileFilter;  //java.io.FileFilterと紛らわしい

  //開くダイアログ
  public static OpenDialog hdcOpenDialog;  //開くダイアログ。null=作る前
  public static int hdcOpenUnit;  //開くユニットの番号
  public static ArrayList<File[]> hdcOpenHistory;  //作る前に追加されたヒストリ

  //フォーマットダイアログ
  public static JDialog hdcFormatDialog;  //ダイアログ
  public static JFileChooser2 hdcFormatFileChooser;  //ファイルチューザー
  public static HDMedia hdcFormatMedia;  //フォーマットするメディアの種類
  public static boolean hdcFormatCopySystemFiles;  //true=システムファイルを転送する

  //ユニット
  public static final HDUnit[] hdcUnitArray = new HDUnit[16];  //ユニットの配列
  public static int hdcHDMax;  //現在挿入されているユニットのユニット番号の最大値+1。0～16。[0x00ed005a].b=SPC.spcSCSIINOn?0:hdcHDMax

  public static int hdcSelectedID;  //セレクションフェーズで選択されたID。0～7=有効,-1=選択されていない
  public static int hdcSelectedLUN;  //コマンドフェーズで選択されたLUN。0～1=有効,-1=選択されていない
  public static HDUnit hdcTargetUnit;  //バスを占有しているターゲットのユニット。hdcUnitArray[hdcSelectedID<<1|hdcSelectedLUN],null=フリー
  public static int hdcBusStatus;  //バスステータス

  public static byte[] hdcReadHandle;  //転送(ターゲット→イニシエータ)の対象の配列。hdcStatusBuffer,hdcMessageBuffer,hdcSenseBuffer,hduImage
  public static byte[] hdcWriteHandle;  //転送(イニシエータ→ターゲット)の対象の配列。hdcCommandBuffer,hduImage
  public static int hdcIndex;  //次に転送する位置
  public static int hdcLimit;  //転送を終了する位置
  public static final byte[] hdcCommandBuffer = new byte[6];  //コマンドバッファ
  public static final byte[] hdcStatusBuffer = new byte[1];  //ステータスバッファ
  public static final byte[] hdcMessageBuffer = new byte[1];  //メッセージバッファ
  public static final byte[] hdcSenseBuffer = new byte[4];  //センスバッファ
  public static final byte[] hdcAssignDriveBuffer = new byte[10];  //ドライブパラメータバッファ

  public static int hdcLastFormatBlodkEnd;  //最後に実行したFormatBlockコマンドの終了位置(バイト)。-1=最後に実行したコマンドはFormatBlockではない

  public static JCheckBoxMenuItem hdcSASIMenuItem;

  //hdcInit ()
  //  HDCを初期化する
  public static void hdcInit () {

    //ファイルフィルタ
    //  SASIハードディスクのイメージファイルかどうかを調べる
    //  ファイルチューザーとドロップターゲットで使う
    hdcFileFilter = new javax.swing.filechooser.FileFilter () {  //java.io.FileFilterと紛らわしい
      @Override public boolean accept (File file) {
        if (file.isDirectory ()) {  //ディレクトリがある
          return true;
        }
        if (!file.isFile ()) {  //ファイルがない
          return false;
        }
        String path = file.getPath ();
        if (hdcIsInserted (path)) {  //既に挿入されている
          return false;
        }
        long longLength = file.length ();
        for (HDMedia media : HDMedia.HDM_ARRAY) {
          if (media.humDiskEndByte == longLength) {  //ファイルサイズが一致
            return true;
          }
        }
        return false;
      }
      @Override public String getDescription () {
        return (Multilingual.mlnJapanese ?
                "SASI ハードディスクのイメージファイル (*.HDF)" :
                "SASI hard disk image files (*.HDF)");
      }
    };

    //開くダイアログ
    hdcOpenDialog = null;
    hdcOpenUnit = 0;
    hdcOpenHistory = new ArrayList<File[]> ();
    for (int i = JFileChooser2.MAXIMUM_HISTORY_COUNT - 1; 0 <= i; i--) {
      hdcAddHistory (JFileChooser2.pathsToFiles (Settings.sgsGetString ("sahistory" + i)));
    }

    //ユニット
    //  ダイアログが書き込み禁止でもパラメータは:Rを付けなければ書き込み禁止にしない
    //hdcUnitArray = new HDUnit[16];
    hdcHDMax = 0;
    for (int u = 0; u < 16; u++) {
      HDUnit unit = hdcUnitArray[u] = new HDUnit (u);
      if (u == 0) {
        unit.connect (false);  //ID 0は内蔵ドライブとするため切り離せない
      }
      String path = Settings.sgsGetString ("sa" + u);
      String hdN = Settings.sgsGetString ("hd" + u);  //SASIまたはSCSI
      if (!(hdN.equals ("") || hdN.equals ("none"))) {  //hdNが指定されている
        String hdNWithoutR = hdN.endsWith (":R") || hdN.endsWith (":r") ? hdN.substring (0, hdN.length () - 2) : hdN;  //":R"を取り除く
        int dotIndex = hdNWithoutR.lastIndexOf ('.');
        String ext = dotIndex < 0 ? "" : hdNWithoutR.substring (dotIndex + 1);  //最後の'.'の後の文字列、なければ""
        if (ext.equalsIgnoreCase ("HDF") ||
            HDMedia.hdmPathToMedia (hdNWithoutR, null) != null) {  //hdNはSASI HDらしい
          path = hdN;
          Settings.sgsPutString ("hd" + u, "");  //消しておく
        }
      }
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
          hdcAddHistory (new File (path).getAbsoluteFile ());
        }
      }
    }

    //フォーマットダイアログ
    hdcFormatDialog = null;
    hdcFormatFileChooser = null;
    hdcFormatMedia = HDMedia.HDM_40MB;
    hdcFormatCopySystemFiles = true;

    hdcSelectedID = -1;
    hdcSelectedLUN = -1;
    hdcTargetUnit = null;
    hdcBusStatus = HDC_STATUS_FREE;

    hdcReadHandle = null;
    hdcWriteHandle = null;
    hdcIndex = 0;
    hdcLimit = 0;
    //hdcCommandBuffer = new byte[6];
    //hdcStatusBuffer = new byte[1];
    //hdcMessageBuffer = new byte[1];
    //hdcSenseBuffer = new byte[4];
    //hdcAssignDriveBuffer = new byte[10];

    hdcLastFormatBlodkEnd = -1;

  }  //hdcInit()

  //hdcTini ()
  //  後始末
  public static void hdcTini () {

    //イメージファイルに書き出す
    for (HDUnit unit : hdcUnitArray) {
      unit.hduTini ();
    }

    //開くダイアログ
    //  開くダイアログを作らなかったときはパラメータを更新しない
    if (hdcOpenDialog != null) {
      Settings.sgsPutOnOff ("sareadonly", hdcOpenDialog.getReadOnly ());
      Settings.sgsPutOnOff ("saappreboot", hdcOpenDialog.getReboot ());
      ArrayList<String> pathsList = hdcOpenDialog.getHistory ();
      int n = pathsList.size ();
      for (int i = 0; i < n; i++) {
        Settings.sgsPutString ("sahistory" + i, pathsList.get (i));
      }
      for (int i = n; i < 16; i++) {
        Settings.sgsPutString ("sahistory" + i, "");
      }
    }

    //ユニット
    for (int u = 0; u < 16; u++) {
      AbstractUnit unit = hdcUnitArray[u];
      Settings.sgsPutString (
        "sa" + u,
        unit.abuConnected && unit.abuInserted ?
        unit.abuWriteProtected ? unit.abuPath + ":R" : unit.abuPath :
        "");
    }

  }  //hdcTini()

  public static void hdcMakeMenu () {

    //アクションリスナー
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        String command = ae.getActionCommand ();
        switch (command) {
        case "Create new SASI hard disk image files":  //SASIハードディスクのイメージファイルの新規作成
          hdcOpenFormatDialog ();
          break;
        }
      }
    };

    //SASIメニュー
    hdcMenu = ComponentFactory.createMenu ("SASI");  //横に長いとサブメニューを開きにくいので短くする
    ComponentFactory.addComponents (
      hdcMenu,
      ComponentFactory.createHorizontalBox (
        Multilingual.mlnText (
          ComponentFactory.createLabel ("SASI hard disk"),
          "ja", "SASI ハードディスク")),
      ComponentFactory.createHorizontalSeparator ()
      );
    for (int u = 0; u < 16; u++) {
      hdcMenu.add (hdcUnitArray[u].getMenuBox ());
    }
    ComponentFactory.addComponents (
      hdcMenu,
      ComponentFactory.createHorizontalSeparator (),
      hdcSASIMenuItem = ComponentFactory.setEnabled (
        Multilingual.mlnText (ComponentFactory.createCheckBoxMenuItem (!XEiJ.currentModel.isSCSI (), "Built-in SASI port", listener), "ja", "内蔵 SASI ポート"),
        false),  //機種の指定で内蔵SASIと内蔵SCSIを切り替えるので操作できないことにする
      ComponentFactory.createHorizontalSeparator (),
      Multilingual.mlnText (ComponentFactory.createMenuItem ("Create new SASI hard disk image files", listener),
                            "ja", "SASI ハードディスクのイメージファイルの新規作成")
      );

  }

  //inserted = hdcIsInserted (path)
  //  パスで指定したファイルが既に挿入されているか調べる
  public static boolean hdcIsInserted (String path) {
    for (HDUnit unit : hdcUnitArray) {
      if (unit != null &&
          unit.abuConnected &&  //接続されている
          unit.abuInserted &&  //挿入されている
          unit.abuPath.equals (path)) {  //パスが一致している
        return true;  //既に挿入されている
      }
    }
    return false;  //まだ挿入されていない
  }  //hdcIsInserted(String)

  static class OpenDialog extends AbstractOpenDialog {
    public OpenDialog () {
      super (XEiJ.frmFrame,
             "Open SASI hard disk image files",
             "SASI ハードディスクのイメージファイルを開く",
             false,  //ファイル
             hdcFileFilter);
    }
    @Override public void openFiles (File[] files, boolean reboot) {
      hdcOpenFiles (files, reboot);
    }
  }  //class OpenDialog

  //hdcOpenFiles (list, reset)
  //  開くダイアログで選択されたファイルを開く
  public static void hdcOpenFiles (File[] list, boolean reset) {
    boolean success = true;
    for (int u = hdcOpenUnit, k = 0; k < list.length; ) {
      if (16 <= u) {  //ユニットが足りない
        success = false;  //失敗
        break;
      }
      HDUnit unit = hdcUnitArray[u];  //ユニット
      if (!unit.abuConnected) {  //接続されていない
        u++;
        continue;
      }
      File file = list[k++];  //イメージファイル
      if (!file.isFile ()) {  //イメージファイルが存在しない
        success = false;  //失敗
        continue;
      }
      if (!unit.insert (file.getPath (),
                        hdcOpenDialog.getReadOnly () || !file.canWrite ())) {  //挿入できない
        success = false;  //失敗
        continue;
      }
      u++;
    }
    if (success) {  //すべて挿入できた
      hdcAddHistory (list);  //ヒストリに追加する
      if (reset) {  //ここから再起動
        XEiJ.mpuReset (0x8000 | hdcOpenUnit << 8, -1);
      }
    }
  }  //hdcOpenFiles(File[],boolean)

  //hdcMakeFormatDialog ()
  //  フォーマットダイアログを作る
  //  コマンドラインのみ
  public static void hdcMakeFormatDialog () {

    //アクションリスナー
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        switch (ae.getActionCommand ()) {
        case JFileChooser.APPROVE_SELECTION:
        case "Start formatting":  //フォーマットを開始する
          {
            File[] list = hdcFormatFileChooser.getSelectedFiles ();
            if (list.length > 0) {
              hdcFormatDialog.setVisible (false);
              if (!hdcFormatFiles (list)) {
                //!!! 失敗
              }
            }
          }
          break;
        case JFileChooser.CANCEL_SELECTION:
        case "Cancel":  //キャンセル
          hdcFormatDialog.setVisible (false);
          break;
        case "10MB":
          hdcFormatMedia = HDMedia.HDM_10MB;
          break;
        case "20MB":
          hdcFormatMedia = HDMedia.HDM_20MB;
          break;
        case "40MB":
          hdcFormatMedia = HDMedia.HDM_40MB;
          break;
        case "Copy system files":  //システムファイルを転送する
          hdcFormatCopySystemFiles = ((JCheckBox) ae.getSource ()).isSelected ();
          break;
        }
      }
    };

    //ファイルチューザー
    hdcMakeFormatFileChooser ();
    hdcFormatFileChooser.setFileFilter (hdcFileFilter);
    hdcFormatFileChooser.addActionListener (listener);

    //ダイアログ
    ButtonGroup mediaGroup = new ButtonGroup ();
    hdcFormatDialog = Multilingual.mlnTitle (
      ComponentFactory.createModalDialog (
        XEiJ.frmFrame,
        "Create new SASI hard disk image files",
        ComponentFactory.createBorderPanel (
          0, 0,
          ComponentFactory.createVerticalBox (
            hdcFormatFileChooser,
            ComponentFactory.createHorizontalBox (
              Box.createHorizontalStrut (12),
              Box.createHorizontalGlue (),
              ComponentFactory.createRadioButtonMenuItem (mediaGroup, hdcFormatMedia == HDMedia.HDM_10MB,  "10MB", listener),
              ComponentFactory.createRadioButtonMenuItem (mediaGroup, hdcFormatMedia == HDMedia.HDM_20MB,  "20MB", listener),
              ComponentFactory.createRadioButtonMenuItem (mediaGroup, hdcFormatMedia == HDMedia.HDM_40MB,  "40MB", listener),
              Box.createHorizontalGlue (),
              Box.createHorizontalStrut (12)
              ),
            Box.createVerticalStrut (12),
            ComponentFactory.createHorizontalBox (
              Box.createHorizontalStrut (12),
              Box.createHorizontalGlue (),
              Multilingual.mlnText (ComponentFactory.createCheckBox (hdcFormatCopySystemFiles, "Copy system files", listener), "ja", "システムファイルを転送する"),
              Box.createHorizontalGlue (),
              Box.createHorizontalStrut (12),
              Multilingual.mlnText (ComponentFactory.createButton ("Start formatting", KeyEvent.VK_F, listener), "ja", "フォーマットを開始する"),
              Box.createHorizontalStrut (12),
              Multilingual.mlnText (ComponentFactory.createButton ("Cancel", KeyEvent.VK_C, listener), "ja", "キャンセル"),
              Box.createHorizontalStrut (12)
              ),
            Box.createVerticalStrut (12)
            )
          )
        ),
      "ja", "SASI ハードディスクのイメージファイルの新規作成");

  }  //hdcMakeFormatDialog()

  //hdcMakeFormatFileChooser ()
  //  フォーマットファイルチューザーを作る
  public static void hdcMakeFormatFileChooser () {
    if (hdcFormatFileChooser == null) {
      hdcFormatFileChooser = new JFileChooser2 ();
      //hdcFormatFileChooser.setMultiSelectionEnabled (true);  //複数選択可能
      hdcFormatFileChooser.setControlButtonsAreShown (false);  //デフォルトのボタンを消す
    }
  }

  //hdcOpenFormatDialog ()
  //  フォーマットダイアログを開く
  public static void hdcOpenFormatDialog () {
    if (hdcFormatDialog == null) {
      hdcMakeFormatDialog ();
    }
    XEiJ.pnlExitFullScreen (true);
    hdcFormatDialog.setVisible (true);
  }  //hdcOpenFormatDialog()

  //success = hdcFormatFiles (list)
  //  SASIディスクをフォーマットする
  //  コマンドラインのみ
  public static boolean hdcFormatFiles (File[] list) {
    boolean success = true;
  format:
    {
      //SASIディスクのフォーマットデータを作る
      byte[] bb = new byte[(int) hdcFormatMedia.humDiskEndByte];  //高々40MBなので丸ごと確保する
      if (!hdcFormatMedia.hdmMakeFormatData (bb, hdcFormatCopySystemFiles)) {
        success = false;  //失敗
        break format;
      }
      //書き出す
      int u = 0;
      for (File file : list) {
        String path = file.getPath ();
        if (true) {
          if (!path.toUpperCase ().endsWith (".HDF")) {  //適切な拡張子が指定されていない
            path += path.endsWith (".") ? "hdf" : ".hdf";  //拡張子を付ける
            file = new File (path);
          }
        }
        if (hdcIsInserted (path)) {  //他のユニットに挿入されている
          success = false;  //失敗
          break format;
        }
        if (!XEiJ.rscPutFile (path, bb, 0, bb.length)) {  //書き出せない
          success = false;  //失敗
          break format;
        }
        //空いているユニットがあれば挿入する
        while (u < 16) {
          HDUnit unit = hdcUnitArray[u++];  //ユニット
          if (unit.abuConnected &&  //接続されていて
              !unit.abuInserted &&  //空いていて
              unit.insert (path,
                           false)) {  //挿入できた
            //フォーマットしたディスクの書き込みを禁止しても意味がないのでここでは書き込みを禁止しない
            break;
          }
        }
      }
    }  //format
    if (success) {  //すべてフォーマットできた
      hdcAddHistory (list);  //ヒストリに追加する
    }
    return success;
  }  //hdcFormatFiles(File[])


  //hdcAddHistory (file)
  //  ファイルをヒストリに追加する
  public static void hdcAddHistory (File file) {
    hdcAddHistory (new File[] { file });
  }

  //hdcAddHistory (files)
  //  複数のファイルをヒストリに追加する
  public static void hdcAddHistory (File[] files) {
    if (hdcOpenDialog == null) {
      hdcOpenHistory.add (files);
    } else {
      hdcOpenDialog.addHistory (files);
    }
    hdcMakeFormatFileChooser ();
    hdcFormatFileChooser.addHistory (files);
    hdcFormatFileChooser.selectLastFiles ();
  }


  //入出力
  //
  //     4   3   2   1   0
  //    MSG C/D I/O BSY REQ | ACK SEL | DATA
  //
  //     0   0   0   0   0  |  0   0  |       バスフリーフェーズ
  //
  //     0   0   0   0   0  |  0   1  |  ID   イニシエータがセレクションフェーズを開始
  //     0   0   0   1   0  |  0   1  |       ターゲットがバスを占有
  //     0   0   0   1   0  |  0   0  |       イニシエータがセレクションフェーズを終了
  //
  //     0   1   0   1   0  |  0   0  |       ターゲットがコマンドフェーズ(イニシエータ→ターゲット)を開始
  //     0   1   0   1   1  |  0   0  |       ターゲットがデータの受信を開始
  //     0   1   0   1   1  |  1   0  |  XX   イニシエータがデータの送信を開始
  //     0   1   0   1   0  |  1   0  |       ターゲットがデータの受信を終了
  //     0   1   0   1   0  |  0   0  |       イニシエータがデータの送信を終了
  //    繰り返す
  //
  //    データ転送フェーズが必要なとき
  //     0   0   0   1   0  |  0   0  |       ターゲットがデータ転送フェーズ(イニシエータ→ターゲット)を開始
  //     0   0   0   1   1  |  0   0  |       ターゲットがデータの受信を開始
  //     0   0   0   1   1  |  1   0  |  XX   イニシエータがデータの送信を開始
  //     0   0   0   1   0  |  1   0  |       ターゲットがデータの受信を終了
  //     0   0   0   1   0  |  0   0  |       イニシエータがデータの送信を終了
  //    繰り返す
  //    または
  //     0   0   1   1   0  |  0   0  |       ターゲットがデータ転送フェーズ(ターゲット→イニシエータ)を開始
  //     0   0   1   1   1  |  0   0  |  XX   ターゲットがデータの送信を開始
  //     0   0   1   1   1  |  1   0  |       イニシエータがデータを受信を開始
  //     0   0   1   1   0  |  1   0  |       ターゲットがデータの送信を終了
  //     0   0   1   1   0  |  0   0  |       イニシエータがデータの受信を終了
  //    繰り返す
  //
  //     0   1   1   1   0  |  0   0  |       ターゲットがステータスフェーズ(ターゲット→イニシエータ)を開始
  //     0   1   1   1   1  |  0   0  |  XX   ターゲットがデータの送信を開始
  //     0   1   1   1   1  |  1   0  |       イニシエータがデータを受信を開始
  //     0   1   1   1   0  |  1   0  |       ターゲットがデータの送信を終了
  //     0   1   1   1   0  |  0   0  |       イニシエータがデータの受信を終了
  //
  //     1   1   1   1   0  |  0   0  |       ターゲットがメッセージフェーズ(ターゲット→イニシエータ)を開始
  //     1   1   1   1   1  |  0   0  |  XX   ターゲットがデータの送信を開始
  //     1   1   1   1   1  |  1   0  |       イニシエータがデータを受信を開始
  //     1   1   1   1   0  |  1   0  |       ターゲットがデータの送信を終了
  //     1   1   1   1   0  |  0   0  |       イニシエータがデータの受信を終了
  //
  //     0   0   0   0   0  |  0   0  |       ターゲットがバスを開放
  //
  //  SELはセレクションポートにIDを書き込むと1になり、ステータスポートに任意のデータを書き込むと0に戻る
  //  ACKはREQ=1のときにデータポートを読み書きすると1になり、REQ=0で0に戻る
  //  ACKはプログラムから見えないので厳密に実装する必要はない
  //  DMAのREQ1はSICILIANのHDREQに接続されている。DMAのACK1はどこにも繋がっておらず、SICILIANがACKを作っている

  //d = hdcPeekStatus ()
  //  pbz (0x00e96003)
  public static int hdcPeekStatus () {
    return hdcBusStatus;
  }  //hdcPeekStatus()

  //d = hdcReadStatus ()
  //  rbz (0x00e96003)
  public static int hdcReadStatus () {
    int d = hdcBusStatus;
    if (HDC_DEBUG_TRACE) {
      System.out.printf ("%08x hdcReadStatus(%d,%d)=%02x\n", XEiJ.regPC0, hdcSelectedID, hdcSelectedLUN, d);
    }
    return d;
  }  //hdcReadStatus()

  //hdcWriteSelect (d)
  //  wb (0x00e96007, d)
  public static void hdcWriteSelect (int d) {
    d &= 255;
    if (HDC_DEBUG_TRACE) {
      System.out.printf ("%08x hdcWriteSelect(%02x)\n", XEiJ.regPC0, d);
    }
    if (d == 0) {  //選択されていない
      return;
    }
    //セレクションフェーズに移行する
    hdcSelectedID = Integer.numberOfTrailingZeros (d);
    hdcSelectedLUN = -1;
    hdcBusStatus = HDC_STATUS_BUSY;
  }  //hdcWriteSelect(int)

  //hdcWriteCommand (d)
  //  wb (0x00e96003, d)
  public static void hdcWriteCommand (int d) {
    d &= 255;
    if (HDC_DEBUG_TRACE) {
      System.out.printf ("%08x hdcWriteCommand(%d,%d,%02x)\n", XEiJ.regPC0, hdcSelectedID, hdcSelectedLUN, d);
    }
    //コマンドフェーズに移行する
    hdcWriteHandle = hdcCommandBuffer;
    hdcIndex = 0;
    hdcLimit = 6;
    hdcBusStatus = HDC_STATUS_COMMAND | HDC_STATUS_OUTPUT | HDC_STATUS_BUSY | HDC_STATUS_REQUEST;
    HD63450.dmaFallREQ (1);
  }  //hdcWriteCommand(int)

  //hdcWriteReset (d)
  //  wb (0x00e96005, d)
  public static void hdcWriteReset (int d) {
    d &= 255;
    if (HDC_DEBUG_TRACE) {
      System.out.printf ("%08x hdcWriteReset(%d,%d,%02x)\n", XEiJ.regPC0, hdcSelectedID, hdcSelectedLUN, d);
    }
    //バスフリーフェーズに移行する
    hdcBusFreePhase ();
  }  //hdcWriteReset(int)

  //d = hdcPeekData ()
  //  pbz (0x00e96001)
  public static int hdcPeekData () {
    return (hdcReadHandle == null ? 0 :  //転送中でなければ無視する
            hdcReadHandle[hdcIndex] & 255);
  }  //hdcPeekData()

  //d = hdcReadData ()
  //  rbz (0x00e96001)
  public static int hdcReadData () {
    if (hdcReadHandle == null) {  //転送中でなければ無視する
      if (HDC_DEBUG_TRACE) {
        System.out.printf ("%08x hdcReadData(%d,%d)=%02x\n", XEiJ.regPC0, hdcSelectedID, hdcSelectedLUN, 0);
      }
      return 0;
    }
    int d = hdcReadHandle[hdcIndex++] & 255;
    HD63450.dmaRiseREQ (1);
    if (HDC_DEBUG_TRACE) {
      System.out.printf ("%08x hdcReadData(%d,%d)=%02x\n", XEiJ.regPC0, hdcSelectedID, hdcSelectedLUN, d);
    }
    if (hdcIndex < hdcLimit) {  //継続
      HD63450.dmaFallREQ (1);
    } else if (hdcBusStatus == (HDC_STATUS_INPUT | HDC_STATUS_BUSY | HDC_STATUS_REQUEST)) {  //データ転送フェーズが終了した
      //ステータスフェーズに移行する
      hdcStatusPhase (0, 0);  //センスデータなし
    } else if (hdcBusStatus == (HDC_STATUS_COMMAND | HDC_STATUS_INPUT | HDC_STATUS_BUSY | HDC_STATUS_REQUEST)) {  //ステータスフェーズが終了した
      //メッセージフェーズに移行する
      hdcReadHandle = hdcMessageBuffer;
      hdcWriteHandle = null;
      hdcIndex = 0;
      hdcLimit = 1;
      hdcBusStatus = HDC_STATUS_MESSAGE | HDC_STATUS_COMMAND | HDC_STATUS_INPUT | HDC_STATUS_BUSY | HDC_STATUS_REQUEST;
      HD63450.dmaFallREQ (1);
    } else {  //メッセージフェーズが終了した
      //バスフリーフェーズに移行する
      hdcBusFreePhase ();
      IOInterrupt.ioiHdcFall ();
      IOInterrupt.ioiHdcRise ();
    }
    return d;
  }  //hdcReadData()

  //hdcWriteData (d)
  //  wb (0x00e96001)
  public static void hdcWriteData (int d) {
    d &= 255;
    if (HDC_DEBUG_TRACE) {
      System.out.printf ("%08x hdcWriteData(%d,%d,%02x)\n", XEiJ.regPC0, hdcSelectedID, hdcSelectedLUN, d);
    }
    if (hdcWriteHandle == null) {  //転送中でなければ無視する
      return;
    }
    hdcWriteHandle[hdcIndex++] = (byte) d;
    HD63450.dmaRiseREQ (1);
    if (hdcIndex < hdcLimit) {  //継続
      HD63450.dmaFallREQ (1);
    } else if (hdcBusStatus == (HDC_STATUS_OUTPUT | HDC_STATUS_BUSY | HDC_STATUS_REQUEST)) {  //データ転送フェーズが終了した
      if (hdcWriteHandle == hdcAssignDriveBuffer) {  //ASSIGN DRIVEのデータ転送フェーズが終了した
        if (HDC_DEBUG_COMMAND) {
          System.out.printf ("%08x SASI AssignDrive[", XEiJ.regPC0);
          for (int i = 0; i < 10; i++) {
            if (i > 0) {
              System.out.print (',');
            }
            System.out.printf ("0x%02x", hdcAssignDriveBuffer[i] & 255);
          }
          System.out.println (']');
        }
        //!!!
      }
      //ステータスフェーズに移行する
      hdcStatusPhase (0, 0);  //センスデータなし
    } else {  //コマンドフェーズが終了した
      int cmd = hdcCommandBuffer[0] & 255;  //コマンド番号
      if (HDC_DEBUG_COMMAND) {
        String name = cmd < HDC_COMMAND_NAME.length ? HDC_COMMAND_NAME[cmd] : "";
        if (name.length () == 0) {
          name = "???";
        }
        System.out.printf ("%08x SASI %s(", XEiJ.regPC0, name);
        for (int i = 0; i < hdcLimit; i++) {
          if (i > 0) {
            System.out.print (',');
          }
          System.out.printf ("0x%02x", hdcCommandBuffer[i] & 255);
        }
        System.out.println (')');
      }
      hdcSelectedLUN = hdcCommandBuffer[1] >> 5 & 7;
      if (hdcSelectedLUN > 1) {
        //ステータスフェーズに移行する
        hdcSenseBuffer[0] = 0x20;  //INVALID COMMAND
        hdcStatusPhase (2, 0);  //センスデータあり
        return;
      }
      hdcTargetUnit = hdcUnitArray[hdcSelectedID << 1 | hdcSelectedLUN];  //ユニット
      if (!hdcTargetUnit.abuInserted) {  //挿入されていない
        //ステータスフェーズに移行する
        hdcSenseBuffer[0] = 0x20;  //INVALID COMMAND
        hdcStatusPhase (2, 0);  //センスデータあり
        return;
      }
      if (hdcLastFormatBlodkEnd >= 0 && cmd != 0x06) {  //一連のFormatBlockコマンドが終了した
        HDMedia media = HDMedia.hdmLengthToMedia (hdcLastFormatBlodkEnd);
        if (media != null) {
          hdcTargetUnit.hduMedia = media;  //SASIハードディスクの容量を変更する
          //!!! FORMAT.XはFormatBlockを行う前に指定されたサイズまでアクセスできるか確認しようとするので、容量を減らすことはできるが増やすことはできない
        }
        hdcLastFormatBlodkEnd = -1;
      }
      switch (cmd) {
      case 0x00:  //TEST DRIVE READY
        hdcDoTestDriveReady ();
        break;
      case 0x01:  //RECALIBRATE
        hdcDoRecalibrate ();
        break;
      case 0x03:  //REQUEST SENSE STATUS
        hdcDoRequestSenseStatus ();
        break;
      case 0x04:  //FORMAT DRIVE
        hdcDoFormatDrive ();
        break;
      //case 0x05:  //CHECK TRACK FORMAT
      case 0x06:  //FORMAT BLOCK
        hdcDoFormatBlock ();
        break;
      //case 0x07:  //BAD TRACK FORMAT
      case 0x08:  //READ
        hdcDoRead ();
        break;
      case 0x0a:  //WRITE
        hdcDoWrite ();
        break;
      case 0x0b:  //SEEK
        hdcDoSeek ();
        break;
      //case 0x0c:  //INITIALIZE DRIVE CHARACTERISTICS
      //case 0x0d:  //LAST CORRECTED BURST LENGTH
      //case 0x0e:  //ASSIGN TRACK
      //case 0x0f:  //WRITE SECTOR BUFFER
      //case 0x10:  //READ SECTOR BUFFER
      case 0xc2:  //ASSIGN DRIVE
        hdcDoAssignDrive ();
        break;
      default:
        if (HDC_DEBUG_UNIMPLEMENTED_COMMAND) {  //未実装コマンドを表示する
          StringBuilder sb = new StringBuilder ();
          String name = cmd < HDC_COMMAND_NAME.length ? HDC_COMMAND_NAME[cmd] : "";
          if (name.length () == 0) {
            name = "???";
          }
          sb.append (String.format ("%08x SASI %s(", XEiJ.regPC0, name));
          for (int i = 0; i < hdcLimit; i++) {
            if (i > 0) {
              sb.append (',');
            }
            sb.append (String.format ("0x%02x", hdcCommandBuffer[i] & 255));
          }
          sb.append (')');
          System.out.println (sb.toString ());
        }
        //ステータスフェーズに移行する
        hdcSenseBuffer[0] = 0x20;  //INVALID COMMAND
        hdcStatusPhase (2, 0);  //センスデータあり
      }
    }
  }  //hdcWriteData(int)

  //hdcDoTestDriveReady ()
  public static void hdcDoTestDriveReady () {
    if (HDC_DEBUG_TRACE) {
      System.out.printf ("%08x hdcDoTestDriveReady(%d,%d)\n", XEiJ.regPC0, hdcSelectedID, hdcSelectedLUN);
    }
    //ステータスフェーズに移行する
    hdcStatusPhase (0, 0);  //センスデータなし
  }  //hdcDoTestDriveReady()

  //hdcDoRecalibrate ()
  public static void hdcDoRecalibrate () {
    if (HDC_DEBUG_TRACE) {
      System.out.printf ("%08x hdcDoRecalibrate(%d,%d)\n", XEiJ.regPC0, hdcSelectedID, hdcSelectedLUN);
    }
    //ステータスフェーズに移行する
    hdcStatusPhase (0, 0);  //センスデータなし
  }  //hdcDoRecalibrate()

  //hdcDoRequestSenseStatus ()
  public static void hdcDoRequestSenseStatus () {
    if (HDC_DEBUG_TRACE) {
      System.out.printf ("%08x hdcDoRequestSenseStatus(%d,%d)\n", XEiJ.regPC0, hdcSelectedID, hdcSelectedLUN);
    }
    //データ転送フェーズ(ターゲット→イニシエータ)に移行する
    hdcReadHandle = hdcSenseBuffer;
    hdcIndex = 0;
    hdcLimit = 4;
    hdcBusStatus = HDC_STATUS_INPUT | HDC_STATUS_BUSY | HDC_STATUS_REQUEST;
    if (false) {
      HD63450.dmaFallREQ (1);
    } else {
      TickerQueue.tkqAdd (hdcDelayTicker, XEiJ.mpuClockTime + HDC_DELAY_TIME);
    }
  }  //hdcDoRequestSenseStatus()

  //hdcDoFormatDrive ()
  public static void hdcDoFormatDrive () {
    if (HDC_DEBUG_TRACE) {
      System.out.printf ("%08x hdcDoFormatDrive(%d,%d)\n", XEiJ.regPC0, hdcSelectedID, hdcSelectedLUN);
    }
    if (!hdcTargetUnit.abuInserted || hdcTargetUnit.hduMedia == null ||  //挿入されていない
        hdcTargetUnit.abuWriteProtected) {  //書き込みが禁止されている
      //ステータスフェーズに移行する
      hdcSenseBuffer[0] = 0x20;  //INVALID COMMAND
      hdcStatusPhase (2, 0);  //センスデータあり
      return;
    }
    hdcTargetUnit.hduWritten = true;
    //ディスクの全体をゼロクリアする
    Arrays.fill (hdcTargetUnit.hduImage, 0, (int) hdcTargetUnit.hduMedia.humDiskEndByte, (byte) 0);
    //ステータスフェーズに移行する
    hdcStatusPhase (0, 0);  //センスデータなし
  }  //hdcDoFormatDrive()

  //hdcDoFormatBlock ()
  //  SASIコマンド$06 FormatBlock
  public static void hdcDoFormatBlock () {
    int i = (hdcCommandBuffer[1] & 31) << 16 | (char) (hdcCommandBuffer[2] << 8 | hdcCommandBuffer[3] & 255);  //開始セクタ
    int n = 33;  //セクタ数
    if (HDC_DEBUG_TRACE) {
      System.out.printf ("%08x hdcDoFormatBlock(%d,%d,%d,%d)\n", XEiJ.regPC0, hdcSelectedID, hdcSelectedLUN, i, n);
    }
    if (!hdcTargetUnit.abuInserted || hdcTargetUnit.hduMedia == null ||  //挿入されていない
        hdcTargetUnit.abuWriteProtected) {  //書き込みが禁止されている
      //ステータスフェーズに移行する
      hdcSenseBuffer[0] = 0x20;  //INVALID COMMAND
      hdcStatusPhase (2, 0);  //センスデータあり
      return;
    }
    int l = i + n;  //終了セクタ+1
    if (hdcTargetUnit.hduMedia.hdmDiskEndRecord < l) {
      //ステータスフェーズに移行する
      hdcSenseBuffer[0] = 0x21;  //INVALID SECTOR ADDRESS
      hdcStatusPhase (2, 0);  //センスデータあり
      return;
    }
    int begin = hdcTargetUnit.hduMedia.hdmBytesPerRecord * i;
    int end = hdcTargetUnit.hduMedia.hdmBytesPerRecord * l;
    hdcLastFormatBlodkEnd = end;
    hdcTargetUnit.hduWritten = true;
    //指定されたセクタをゼロクリアする
    Arrays.fill (hdcTargetUnit.hduImage, begin, end, (byte) 0);
    //ステータスフェーズに移行する
    hdcStatusPhase (0, 0);  //センスデータなし
  }  //hdcDoFormatBlock()

  //hdcDoRead ()
  public static void hdcDoRead () {
    int i = (hdcCommandBuffer[1] & 31) << 16 | (char) (hdcCommandBuffer[2] << 8 | hdcCommandBuffer[3] & 255);  //開始セクタ
    int n = hdcCommandBuffer[4] & 255;  //セクタ数
    if (n == 0) {
      n = 256;
    }
    if (HDC_DEBUG_TRACE) {
      System.out.printf ("%08x hdcDoRead(%d,%d,%d,%d)\n", XEiJ.regPC0, hdcSelectedID, hdcSelectedLUN, i, n);
    }
    if (!hdcTargetUnit.abuInserted || hdcTargetUnit.hduMedia == null) {  //挿入されていない
      //ステータスフェーズに移行する
      hdcSenseBuffer[0] = 0x20;  //INVALID COMMAND
      hdcStatusPhase (2, 0);  //センスデータあり
      return;
    }
    int l = i + n;  //終了セクタ+1
    if (hdcTargetUnit.hduMedia.hdmDiskEndRecord < l) {
      //ステータスフェーズに移行する
      hdcSenseBuffer[0] = 0x21;  //INVALID SECTOR ADDRESS
      hdcStatusPhase (2, 0);  //センスデータあり
      return;
    }
    //データ転送フェーズ(ターゲット→イニシエータ)に移行する
    hdcReadHandle = hdcTargetUnit.hduImage;
    hdcIndex = hdcTargetUnit.hduMedia.hdmBytesPerRecord * i;
    hdcLimit = hdcTargetUnit.hduMedia.hdmBytesPerRecord * l;
    hdcBusStatus = HDC_STATUS_INPUT | HDC_STATUS_BUSY | HDC_STATUS_REQUEST;
    if (false) {
      HD63450.dmaFallREQ (1);
    } else {
      TickerQueue.tkqAdd (hdcDelayTicker, XEiJ.mpuClockTime + HDC_DELAY_TIME);
    }
  }  //hdcDoRead()

  //hdcDoWrite ()
  public static void hdcDoWrite () {
    int i = (hdcCommandBuffer[1] & 31) << 16 | (char) (hdcCommandBuffer[2] << 8 | hdcCommandBuffer[3] & 255);  //開始セクタ
    int n = hdcCommandBuffer[4] & 255;  //セクタ数
    if (n == 0) {
      n = 256;
    }
    if (HDC_DEBUG_TRACE) {
      System.out.printf ("%08x hdcDoWrite(%d,%d,%d,%d)\n", XEiJ.regPC0, hdcSelectedID, hdcSelectedLUN, i, n);
    }
    if (!hdcTargetUnit.abuInserted || hdcTargetUnit.hduMedia == null ||  //挿入されていない
        hdcTargetUnit.abuWriteProtected) {  //書き込みが禁止されている
      //ステータスフェーズに移行する
      hdcSenseBuffer[0] = 0x20;  //INVALID COMMAND
      hdcStatusPhase (2, 0);  //センスデータあり
      return;
    }
    int l = i + n;  //終了セクタ+1
    if (hdcTargetUnit.hduMedia.hdmDiskEndRecord < l) {
      //ステータスフェーズに移行する
      hdcSenseBuffer[0] = 0x21;  //INVALID SECTOR ADDRESS
      hdcStatusPhase (2, 0);  //センスデータあり
      return;
    }
    hdcTargetUnit.hduWritten = true;
    //データ転送フェーズ(イニシエータ→ターゲット)に移行する
    hdcWriteHandle = hdcTargetUnit.hduImage;
    hdcIndex = hdcTargetUnit.hduMedia.hdmBytesPerRecord * i;
    hdcLimit = hdcTargetUnit.hduMedia.hdmBytesPerRecord * l;
    hdcBusStatus = HDC_STATUS_OUTPUT | HDC_STATUS_BUSY | HDC_STATUS_REQUEST;
    if (false) {
      HD63450.dmaFallREQ (1);
    } else {
      TickerQueue.tkqAdd (hdcDelayTicker, XEiJ.mpuClockTime + HDC_DELAY_TIME);
    }
  }  //hdcDoWrite()

  //hdcDoSeek ()
  public static void hdcDoSeek () {
    int i = (hdcCommandBuffer[1] & 31) << 16 | (char) (hdcCommandBuffer[2] << 8 | hdcCommandBuffer[3] & 255);  //開始セクタ
    if (HDC_DEBUG_TRACE) {
      System.out.printf ("%08x hdcDoSeek(%d,%d,%d)\n", XEiJ.regPC0, hdcSelectedID, hdcSelectedLUN, i);
    }
    if (!hdcTargetUnit.abuInserted || hdcTargetUnit.hduMedia == null) {  //挿入されていない
      //ステータスフェーズに移行する
      hdcSenseBuffer[0] = 0x20;  //INVALID COMMAND
      hdcStatusPhase (2, 0);  //センスデータあり
      return;
    }
    if (hdcTargetUnit.hduMedia.hdmDiskEndRecord <= i) {
      //ステータスフェーズに移行する
      hdcSenseBuffer[0] = 0x21;  //INVALID SECTOR ADDRESS
      hdcStatusPhase (2, 0);  //センスデータあり
      return;
    }
    //ステータスフェーズに移行する
    hdcStatusPhase (0, 0);  //センスデータなし
  }  //hdcDoSeek()

  //hdcDoAssignDrive ()
  public static void hdcDoAssignDrive () {
    if (HDC_DEBUG_TRACE) {
      System.out.printf ("%08x hdcDoAssignDrive(%d,%d)\n", XEiJ.regPC0, hdcSelectedID, hdcSelectedLUN);
    }
    //データ転送フェーズ(イニシエータ→ターゲット)に移行する
    hdcWriteHandle = hdcAssignDriveBuffer;
    hdcIndex = 0;
    hdcLimit = 10;
    hdcBusStatus = HDC_STATUS_OUTPUT | HDC_STATUS_BUSY | HDC_STATUS_REQUEST;
    if (false) {
      HD63450.dmaFallREQ (1);
    } else {
      TickerQueue.tkqAdd (hdcDelayTicker, XEiJ.mpuClockTime + HDC_DELAY_TIME);
    }
  }  //hdcDoAssignDrive()

  //hdcStatusPhase (status, message)
  //  ステータスフェーズに移行する
  //  status   2=センスデータあり。hdcSenseBufferを設定しておくこと
  //  message  常に0
  public static void hdcStatusPhase (int status, int message) {
    hdcStatusBuffer[0] = (byte) status;
    hdcMessageBuffer[0] = (byte) message;
    hdcReadHandle = hdcStatusBuffer;
    hdcWriteHandle = null;
    hdcIndex = 0;
    hdcLimit = 1;
    hdcBusStatus = HDC_STATUS_COMMAND | HDC_STATUS_INPUT | HDC_STATUS_BUSY | HDC_STATUS_REQUEST;
    HD63450.dmaFallREQ (1);
  }  //hdcStatusPhase(int,int)

  //hdcBusFreePhase ()
  //  バスフリーフェーズに移行する
  public static void hdcBusFreePhase () {
    hdcSelectedID = -1;
    hdcSelectedLUN = -1;
    hdcTargetUnit = null;
    hdcBusStatus = HDC_STATUS_FREE;
    hdcReadHandle = null;
    hdcWriteHandle = null;
    hdcIndex = 0;
    hdcLimit = 0;
    HD63450.dmaRiseREQ (1);
  }  //hdcBusFreePhase()



  //========================================================================================
  //SASIフォーマットデータ
  //  無償公開されたHuman68k version 3.02のシステムディスクに入っているFORMAT.Xから抽出したデータを使う

  //----------------------------------------------------------------------------------------
  //SASIディスクIPL
/*
  public static final int[] HDC_DISK_IPL = {
    //  perl -e "do'sjdump.pl';$p=0;$m=4;$o=0x8aa;$l=0xcaa-$o;open IN,'HUMAN302.XDF'or die;binmode IN;seek IN,1024*592,0;read IN,$b,64;seek IN,1024*592+vec($b,15,32)+32*$m,0;read IN,$b,32;seek IN,1024*592+vec($b,7,32)+64+$o,0;read IN,$b,$l;close IN;sjdumpcode($b,0,$l,$p)"
    0x60,0x00,0x00,0xca,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00000000  `..ﾊ............
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00000010  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00000020  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00000030  ................
    0x1a,0x1b,0x5b,0x36,0x3b,0x33,0x32,0x48,0x58,0x36,0x38,0x30,0x30,0x30,0x20,0x48,  //00000040  ..[6;32HX68000 H
    0x41,0x52,0x44,0x20,0x44,0x49,0x53,0x4b,0x20,0x49,0x50,0x4c,0x20,0x4d,0x45,0x4e,  //00000050  ARD DISK IPL MEN
    0x55,0x1b,0x5b,0x32,0x35,0x3b,0x32,0x32,0x48,0x83,0x4a,0x81,0x5b,0x83,0x5c,0x83,  //00000060  U.[25;22Hカーソル
    0x8b,0x83,0x4c,0x81,0x5b,0x82,0xc5,0x91,0x49,0x91,0xf0,0x82,0xb5,0x82,0xc4,0x83,  //00000070   キーで選択してリ
    0x8a,0x83,0x5e,0x81,0x5b,0x83,0x93,0x83,0x4c,0x81,0x5b,0x82,0xf0,0x89,0x9f,0x82,  //00000080   ターンキーを押し
    0xb5,0x82,0xc4,0x82,0xad,0x82,0xbe,0x82,0xb3,0x82,0xa2,0x00,0x1b,0x5b,0x32,0x36,  //00000090   てください..[26
    0x3b,0x32,0x38,0x48,0x91,0x49,0x91,0xf0,0x82,0xb5,0x82,0xbd,0x82,0xe0,0x82,0xcc,  //000000a0  ;28H選択したもの
    0x82,0xf0,0x8e,0xa9,0x93,0xae,0x8b,0x4e,0x93,0xae,0x82,0xc6,0x82,0xb5,0x82,0xc4,  //000000b0  を自動起動として
    0x93,0x6f,0x98,0x5e,0x82,0xb5,0x82,0xdc,0x82,0xb7,0x00,0x00,0x4f,0xfa,0xff,0x32,  //000000c0  登録します..O..2
    0x42,0x85,0x20,0x3c,0x00,0x00,0x00,0x8e,0x4e,0x4f,0x1e,0x00,0xe1,0x47,0x74,0x04,  //000000d0  B. <...晒O..瓱t.
    0x26,0x3c,0x00,0x00,0x01,0x00,0x43,0xfa,0x02,0xc6,0x61,0x00,0x02,0x32,0x4a,0x00,  //000000e0  &<....C..ﾆa..2J.
    0x66,0x00,0x01,0x6a,0x43,0xfa,0x02,0xb8,0x47,0xfa,0xff,0x0a,0x0c,0x91,0x58,0x36,  //000000f0  f..jC..ｸG....噌6
    0x38,0x4b,0x66,0x00,0x01,0x6c,0x74,0x0e,0x42,0x43,0x42,0x44,0x42,0x86,0x43,0xe9,  //00000100  8Kf..lt.BCBDB.C.
    0x00,0x10,0x4a,0x11,0x67,0x16,0x52,0x46,0x26,0xc9,0x10,0x29,0x00,0x08,0x08,0x00,  //00000110  ..J.g.RF&ﾉ.)....
    0x00,0x00,0x66,0x08,0x52,0x43,0x4a,0x00,0x66,0x02,0x52,0x44,0x51,0xca,0xff,0xe0,  //00000120  ..f.RCJ.f.RDQﾊ.濳
    0x4a,0x43,0x67,0x00,0x01,0x42,0x72,0x0a,0x70,0x04,0x4e,0x4f,0x08,0x00,0x00,0x04,  //00000130   Cg..Br.p.NO....
    0x66,0x12,0x4a,0x44,0x67,0x0e,0x53,0x44,0x67,0x1c,0x43,0xfa,0xfe,0xf4,0x61,0x00,  //00000140  f.JDg.SDg.C...a.
    0x01,0xc4,0x60,0x28,0x43,0xfa,0xfe,0xea,0x61,0x00,0x01,0xba,0x43,0xfa,0xff,0x3e,  //00000150  .ﾄ`(C..鸞..ｺC..>
    0x61,0x00,0x01,0xb2,0x60,0x14,0x47,0xfa,0xfe,0x9c,0x20,0x5b,0x24,0x28,0x00,0x08,  //00000160  a..ｲ`.G... [$(..
    0x4a,0x28,0x00,0x08,0x66,0xf4,0x60,0x00,0x00,0xbe,0x7a,0x02,0x42,0x43,0x45,0xfa,  //00000170  J(..f.`..ｾz.BCE.
    0xfe,0x84,0x22,0x52,0x10,0x29,0x00,0x08,0x67,0x0a,0xb0,0x05,0x67,0x06,0x72,0x02,  //00000180  .."R.)..g.ｰ.g.r.
    0x61,0x00,0x01,0x76,0x61,0x00,0x01,0x16,0x58,0x8a,0x52,0x43,0xb6,0x46,0x65,0xe2,  //00000190  a..va...X崖CｶFe秡
    0x60,0x2a,0x61,0x00,0x01,0x06,0x61,0x00,0x00,0xf2,0xb0,0x3c,0x00,0x1d,0x67,0x3a,  //000001a0   *a...a...ｰ<..g:
    0xb0,0x3c,0x00,0x35,0x67,0x0c,0xb0,0x3c,0x00,0x3c,0x67,0x1a,0xb0,0x3c,0x00,0x3e,  //000001b0  ｰ<.5g.ｰ<.<g.ｰ<.>
    0x66,0xe4,0x61,0x00,0x00,0xe8,0x52,0x43,0xb6,0x46,0x65,0x02,0x42,0x43,0x61,0x00,  //000001c0  f臑..鏗CｶFe.BCa.
    0x00,0xb2,0x66,0xf2,0x60,0xcc,0x61,0x00,0x00,0xd4,0x53,0x43,0x6a,0x04,0x36,0x06,  //000001d0  .ｲf.`ﾌa..ﾔSCj.6.
    0x53,0x43,0x61,0x00,0x00,0x9e,0x66,0xf2,0x60,0xb8,0x47,0xfa,0xfe,0x18,0xe5,0x43,  //000001e0  SCa..枅.`ｸG...蕕
    0x20,0x73,0x30,0x00,0x24,0x28,0x00,0x08,0x4a,0x05,0x67,0x3a,0x43,0xfa,0x01,0xb8,  //000001f0   s0.$(..J.g:C..ｸ
    0x72,0x0e,0x43,0xe9,0x00,0x10,0x4a,0x29,0xff,0xf8,0x67,0x12,0x20,0x11,0x08,0x00,  //00000200  r.C...J)..g. ...
    0x00,0x18,0x66,0x0a,0x42,0x11,0xb4,0x80,0x67,0x04,0x12,0xbc,0x00,0x02,0x51,0xc9,  //00000210  ..f.B.ｴ.g..ｼ..Qﾉ
    0xff,0xe2,0x2f,0x02,0x74,0x04,0x26,0x3c,0x00,0x00,0x01,0x00,0x43,0xfa,0x01,0x80,  //00000220  ../.t.&<....C...
    0x61,0x00,0x00,0xe8,0x24,0x1f,0xc4,0xbc,0x00,0xff,0xff,0xff,0x26,0x3c,0x00,0x00,  //00000230  a...$.ﾄｼ....&<..
    0x04,0x00,0x43,0xfa,0xfd,0xbc,0xd3,0xfc,0x00,0x00,0x04,0x00,0x61,0x00,0x00,0xd0,  //00000240  ..C..ｼﾓ.....a..ﾐ
    0x4a,0x00,0x66,0x08,0x0c,0x11,0x00,0x60,0x66,0x22,0x4e,0xd1,0x45,0xfa,0x00,0xda,  //00000250  J.f....`f"NﾑE..ﾚ
    0x43,0xfa,0x00,0xcc,0x61,0x00,0x00,0xae,0x22,0x4a,0x61,0x00,0x00,0xa8,0x60,0xfe,  //00000260  C..ﾌa..ｮ"Ja..ｨ`.
    0x45,0xfa,0x00,0xe3,0x60,0xea,0x45,0xfa,0x00,0xfa,0x60,0xe4,0x45,0xfa,0x01,0x11,  //00000270  E..總鵙...`胼...
    0x60,0xde,0x41,0xfa,0xfd,0x80,0x20,0x03,0xe5,0x40,0x20,0x70,0x00,0x00,0x10,0x28,  //00000280  `ﾞA... .蕁 p...(
    0x00,0x08,0xb0,0x05,0x67,0x02,0x4a,0x00,0x4e,0x75,0x42,0x80,0x4e,0x4f,0xe0,0x48,  //00000290  ..ｰ.g.J.NuB.NO潯
    0xb0,0x3c,0x00,0x4e,0x66,0x02,0x70,0x1d,0x4e,0x75,0x61,0x5a,0x43,0xfa,0xfd,0x56,  //000002a0  ｰ<.Nf.p.NuaZC..V
    0x30,0x03,0xe5,0x40,0x43,0xf1,0x00,0x00,0x72,0x24,0x74,0x09,0xd4,0x43,0x70,0x23,  //000002b0  0.蕁C...r$t.ﾔCp#
    0x4e,0x4f,0x22,0x51,0x72,0x28,0x61,0x46,0x24,0x09,0x41,0xfa,0x00,0xe2,0x94,0x88,  //000002c0  NO"Qr(aF$.A..笏郁
    0xe8,0x8a,0x84,0xfc,0x00,0x0a,0xd4,0xbc,0x00,0x30,0x00,0x30,0x72,0x20,0xb4,0x7c,  //000002d0   割...ﾔｼ.0.0r ｴ|
    0x00,0x30,0x67,0x02,0x32,0x02,0x61,0x26,0x48,0x42,0x32,0x02,0x61,0x20,0x72,0x29,  //000002e0  .0g.2.a&HB2.a r)
    0x61,0x1c,0x72,0x20,0x61,0x18,0x74,0x07,0x42,0x41,0x12,0x19,0x61,0x10,0x51,0xca,  //000002f0  a.r a.t.BA..a.Qﾊ
    0xff,0xf8,0x72,0x03,0x60,0x02,0x72,0x0b,0x70,0x22,0x4e,0x4f,0x4e,0x75,0x70,0x20,  //00000300  ..r.`.r.p"NONup 
    0x4e,0x4f,0x4e,0x75,0x70,0x21,0x4e,0x4f,0x4e,0x75,0x70,0x45,0x60,0x02,0x70,0x46,  //00000310  NONup!NONupE`.pF
    0x48,0xe7,0x78,0x40,0x32,0x07,0x4e,0x4f,0x4c,0xdf,0x02,0x1e,0x4e,0x75,0x1a,0x1b,  //00000320  H輾@2.NOLﾟ..Nu..
    0x5b,0x31,0x36,0x3b,0x33,0x33,0x48,0x00,0x20,0x20,0x83,0x6e,0x81,0x5b,0x83,0x68,  //00000330  [16;33H.  ハード
    0x83,0x66,0x83,0x42,0x83,0x58,0x83,0x4e,0x82,0xaa,0x93,0xc7,0x82,0xdf,0x82,0xdc,  //00000340  ディスクが読めま
    0x82,0xb9,0x82,0xf1,0x00,0x20,0x20,0x8a,0xc7,0x97,0x9d,0x83,0x75,0x83,0x8d,0x83,  //00000350  せん.  管理ブロッ
    0x62,0x83,0x4e,0x82,0xaa,0x89,0xf3,0x82,0xea,0x82,0xc4,0x82,0xa2,0x82,0xdc,0x82,  //00000360   クが壊れています
    0xb7,0x00,0x20,0x20,0x8b,0x4e,0x93,0xae,0x89,0xc2,0x94,0x5c,0x82,0xc8,0x97,0xcc,  //00000370   .  起動可能な領
    0x88,0xe6,0x82,0xaa,0x82,0xa0,0x82,0xe8,0x82,0xdc,0x82,0xb9,0x82,0xf1,0x00,0x82,  //00000380  域がありません.Ｉ
    0x68,0x82,0x6f,0x82,0x6b,0x83,0x75,0x83,0x8d,0x83,0x62,0x83,0x4e,0x82,0xcc,0x93,  //00000390   ＰＬブロックの内
    0xe0,0x97,0x65,0x82,0xaa,0x88,0xd9,0x8f,0xed,0x82,0xc5,0x82,0xb7,0x00,0x00,0x00,  //000003a0   容が異常です...
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //000003b0  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //000003c0  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //000003d0  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //000003e0  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //000003f0  ................
  };
*/
  //  perl misc/itob.pl xeij/HDC.java HDC_DISK_IPL
  public static final byte[] HDC_DISK_IPL = "`\0\0\312\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\32\33[6;32HX68000 HARD DISK IPL MENU\33[25;22H\203J\201[\203\\\203\213\203L\201[\202\305\221I\221\360\202\265\202\304\203\212\203^\201[\203\223\203L\201[\202\360\211\237\202\265\202\304\202\255\202\276\202\263\202\242\0\33[26;28H\221I\221\360\202\265\202\275\202\340\202\314\202\360\216\251\223\256\213N\223\256\202\306\202\265\202\304\223o\230^\202\265\202\334\202\267\0\0O\372\3772B\205 <\0\0\0\216NO\36\0\341Gt\4&<\0\0\1\0C\372\2\306a\0\0022J\0f\0\1jC\372\2\270G\372\377\n\f\221X68Kf\0\1lt\16BCBDB\206C\351\0\20J\21g\26RF&\311\20)\0\b\b\0\0\0f\bRCJ\0f\2RDQ\312\377\340JCg\0\1Br\np\4NO\b\0\0\4f\22JDg\16SDg\34C\372\376\364a\0\1\304`(C\372\376\352a\0\1\272C\372\377>a\0\1\262`\24G\372\376\234 [$(\0\bJ(\0\bf\364`\0\0\276z\2BCE\372\376\204\"R\20)\0\bg\n\260\5g\6r\2a\0\1va\0\1\26X\212RC\266Fe\342`*a\0\1\6a\0\0\362\260<\0\35g:\260<\0005g\f\260<\0<g\32\260<\0>f\344a\0\0\350RC\266Fe\2BCa\0\0\262f\362`\314a\0\0\324SCj\0046\6SCa\0\0\236f\362`\270G\372\376\30\345C s0\0$(\0\bJ\5g:C\372\1\270r\16C\351\0\20J)\377\370g\22 \21\b\0\0\30f\nB\21\264\200g\4\22\274\0\2Q\311\377\342/\2t\4&<\0\0\1\0C\372\1\200a\0\0\350$\37\304\274\0\377\377\377&<\0\0\4\0C\372\375\274\323\374\0\0\4\0a\0\0\320J\0f\b\f\21\0`f\"N\321E\372\0\332C\372\0\314a\0\0\256\"Ja\0\0\250`\376E\372\0\343`\352E\372\0\372`\344E\372\1\21`\336A\372\375\200 \3\345@ p\0\0\20(\0\b\260\5g\2J\0NuB\200NO\340H\260<\0Nf\2p\35NuaZC\372\375V0\3\345@C\361\0\0r$t\t\324Cp#NO\"Qr(aF$\tA\372\0\342\224\210\350\212\204\374\0\n\324\274\0000\0000r \264|\0000g\0022\2a&HB2\2a r)a\34r a\30t\7BA\22\31a\20Q\312\377\370r\3`\2r\13p\"NONup NONup!NONupE`\2pFH\347x@2\7NOL\337\2\36Nu\32\33[16;33H\0  \203n\201[\203h\203f\203B\203X\203N\202\252\223\307\202\337\202\334\202\271\202\361\0  \212\307\227\235\203u\203\215\203b\203N\202\252\211\363\202\352\202\304\202\242\202\334\202\267\0  \213N\223\256\211\302\224\\\202\310\227\314\210\346\202\252\202\240\202\350\202\334\202\271\202\361\0\202h\202o\202k\203u\203\215\203b\203N\202\314\223\340\227e\202\252\210\331\217\355\202\305\202\267\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0".getBytes (XEiJ.ISO_8859_1);

  //----------------------------------------------------------------------------------------
  //SASIパーティションIPL
/*
  public static final int[] HDC_PARTITION_IPL = {
    //  perl -e "do'sjdump.pl';$p=0x2100;$m=4;$o=0x1126;$l=0x1526-$o;open IN,'HUMAN302.XDF'or die;binmode IN;seek IN,1024*592,0;read IN,$b,64;seek IN,1024*592+vec($b,15,32)+32*$m,0;read IN,$b,32;seek IN,1024*592+vec($b,7,32)+64+$o,0;read IN,$b,$l;close IN;sjdumpcode($b,0,$l,$p)"
    0x60,0x20,0x48,0x75,0x64,0x73,0x6f,0x6e,0x20,0x73,0x6f,0x66,0x74,0x20,0x32,0x2e,  //00002100  ` Hudson soft 2.
    0x30,0x30,0x04,0x00,0x01,0x02,0x00,0x01,0x02,0x00,0x27,0x72,0xf8,0x14,0x00,0x00,  //00002110  00........'r....
    0x00,0x00,0x4f,0xfa,0xff,0xdc,0x43,0xfa,0x01,0x34,0x61,0x00,0x01,0x2a,0x20,0x3c,  //00002120  ..O..ﾜC..4a..* <
    0x00,0x00,0x00,0x8e,0x4e,0x4f,0x12,0x00,0xe1,0x41,0x42,0x82,0x14,0x3a,0xff,0xdf,  //00002130  ...晒O..瓣B..:.ﾟ
    0x42,0x80,0x10,0x3a,0xff,0xd1,0xc4,0xc0,0x42,0x80,0x30,0x3a,0xff,0xca,0xd4,0x80,  //00002140  B..:.ﾑﾄﾀB.0:.ﾊﾔ.
    0xe5,0x82,0xd4,0xba,0xff,0xca,0x26,0x3c,0x00,0x00,0x04,0x00,0x43,0xfa,0x01,0x9a,  //00002150  蛯ﾔｺ.ﾊ&<....C..啾
    0x61,0x00,0x00,0xc8,0x4a,0x00,0x66,0x00,0x00,0xd0,0x43,0xfa,0x01,0x8c,0x3c,0x3c,  //00002160   ..ﾈJ.f..ﾐC...<<
    0x00,0x1f,0x24,0x49,0x47,0xfa,0x01,0x75,0x7e,0x0a,0x10,0x1a,0x80,0x3c,0x00,0x20,  //00002170  ..$IG..u~....<. 
    0xb0,0x1b,0x66,0x06,0x51,0xcf,0xff,0xf4,0x60,0x14,0xd3,0xfc,0x00,0x00,0x00,0x20,  //00002180  ｰ.f.Qﾏ..`.ﾓ.... 
    0x51,0xce,0xff,0xe0,0x43,0xfa,0x00,0xc8,0x61,0x00,0x00,0xbc,0x60,0xfe,0x42,0x80,  //00002190  Qﾎ.澆..ﾈa..ｼ`.B.
    0x30,0x29,0x00,0x1a,0xe1,0x58,0x55,0x40,0x42,0x87,0x1e,0x3a,0xff,0x68,0xc0,0xc7,  //000021a0  0)..畊U@B..:.hﾀﾇ
    0xe5,0x80,0xd4,0x80,0x42,0x80,0x30,0x3a,0xff,0x60,0xeb,0x80,0xd0,0xbc,0x00,0x00,  //000021b0  蛟ﾔ.B.0:.`..ﾐｼ..
    0x03,0xff,0xe0,0x88,0xe4,0x88,0xe5,0x80,0xd4,0x80,0x48,0xe7,0x60,0x00,0x43,0xfa,  //000021c0  ..煦艾蛟ﾔ.H軈.C.
    0x01,0x28,0x26,0x3c,0x00,0x00,0x01,0x00,0x61,0x50,0x4c,0xdf,0x00,0x06,0x4a,0x00,  //000021d0  .(&<....aPLﾟ..J.
    0x66,0x56,0x43,0xfa,0x01,0x14,0x0c,0x59,0x48,0x55,0x66,0x56,0x54,0x89,0x0c,0x99,  //000021e0  fVC....YHUfVT...
    0x00,0x00,0x68,0x00,0x66,0x56,0x2f,0x19,0x26,0x19,0xd6,0x99,0x2f,0x03,0x2f,0x19,  //000021f0  ..h.fV/.&.ﾖ././.
    0x22,0x7c,0x00,0x00,0x67,0xc0,0xd6,0xbc,0x00,0x00,0x00,0x40,0x61,0x1c,0x22,0x1f,  //00002200  "|..gﾀﾖｼ...@a.".
    0x24,0x1f,0x22,0x5f,0x4a,0x00,0x66,0x20,0x41,0xf9,0x00,0x00,0x68,0x00,0xd1,0xc2,  //00002210  $."_J.f A...h.ﾑﾂ
    0x53,0x81,0x65,0x04,0x42,0x18,0x60,0xf8,0x4e,0xd1,0x48,0xe7,0x78,0x40,0x70,0x46,  //00002220  S‘.B.`.NﾑH輾@pF
    0x4e,0x4f,0x4c,0xdf,0x02,0x1e,0x4e,0x75,0x43,0xfa,0x00,0x48,0x61,0x18,0x60,0x00,  //00002230  NOLﾟ..NuC..Ha.`.
    0xff,0x5c,0x43,0xfa,0x00,0x5d,0x61,0x0e,0x60,0x00,0xff,0x52,0x43,0xfa,0x00,0x75,  //00002240  .\C..]a.`..RC..u
    0x61,0x04,0x60,0x00,0xff,0x48,0x70,0x21,0x4e,0x4f,0x4e,0x75,0x1a,0x00,0x1b,0x5b,  //00002250  a.`..Hp!NONu...[
    0x31,0x36,0x3b,0x33,0x35,0x48,0x48,0x75,0x6d,0x61,0x6e,0x2e,0x73,0x79,0x73,0x20,  //00002260  16;35HHuman.sys 
    0x82,0xaa,0x20,0x8c,0xa9,0x82,0xc2,0x82,0xa9,0x82,0xe8,0x82,0xdc,0x82,0xb9,0x82,  //00002270  が 見つかりません
    0xf1,0x00,0x1b,0x5b,0x31,0x36,0x3b,0x33,0x38,0x48,0x83,0x66,0x83,0x42,0x83,0x58,  //00002280   ..[16;38Hディス
    0x83,0x4e,0x82,0xaa,0x81,0x40,0x93,0xc7,0x82,0xdf,0x82,0xdc,0x82,0xb9,0x82,0xf1,  //00002290  クが　読めません
    0x00,0x1b,0x5b,0x31,0x36,0x3b,0x33,0x36,0x48,0x48,0x75,0x6d,0x61,0x6e,0x2e,0x73,  //000022a0  ..[16;36HHuman.s
    0x79,0x73,0x20,0x82,0xaa,0x20,0x89,0xf3,0x82,0xea,0x82,0xc4,0x82,0xa2,0x82,0xdc,  //000022b0  ys が 壊れていま
    0x82,0xb7,0x00,0x1b,0x5b,0x31,0x36,0x3b,0x33,0x33,0x48,0x48,0x75,0x6d,0x61,0x6e,  //000022c0  す..[16;33HHuman
    0x2e,0x73,0x79,0x73,0x20,0x82,0xcc,0x20,0x83,0x41,0x83,0x68,0x83,0x8c,0x83,0x58,  //000022d0  .sys の アドレス
    0x82,0xaa,0x88,0xd9,0x8f,0xed,0x82,0xc5,0x82,0xb7,0x00,0x68,0x75,0x6d,0x61,0x6e,  //000022e0  が異常です.human
    0x20,0x20,0x20,0x73,0x79,0x73,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //000022f0     sys..........
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00002300  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00002310  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00002320  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00002330  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00002340  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00002350  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00002360  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00002370  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00002380  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00002390  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //000023a0  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //000023b0  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //000023c0  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //000023d0  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //000023e0  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //000023f0  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00002400  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00002410  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00002420  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00002430  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00002440  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00002450  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00002460  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00002470  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00002480  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //00002490  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //000024a0  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //000024b0  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //000024c0  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //000024d0  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //000024e0  ................
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,  //000024f0  ................
  };
*/
  //  perl misc/itob.pl xeij/HDC.java HDC_PARTITION_IPL
  public static final byte[] HDC_PARTITION_IPL = "` Hudson soft 2.00\4\0\1\2\0\1\2\0\'r\370\24\0\0\0\0O\372\377\334C\372\0014a\0\1* <\0\0\0\216NO\22\0\341AB\202\24:\377\337B\200\20:\377\321\304\300B\2000:\377\312\324\200\345\202\324\272\377\312&<\0\0\4\0C\372\1\232a\0\0\310J\0f\0\0\320C\372\1\214<<\0\37$IG\372\1u~\n\20\32\200<\0 \260\33f\6Q\317\377\364`\24\323\374\0\0\0 Q\316\377\340C\372\0\310a\0\0\274`\376B\2000)\0\32\341XU@B\207\36:\377h\300\307\345\200\324\200B\2000:\377`\353\200\320\274\0\0\3\377\340\210\344\210\345\200\324\200H\347`\0C\372\1(&<\0\0\1\0aPL\337\0\6J\0fVC\372\1\24\fYHUfVT\211\f\231\0\0h\0fV/\31&\31\326\231/\3/\31\"|\0\0g\300\326\274\0\0\0@a\34\"\37$\37\"_J\0f A\371\0\0h\0\321\302S\201e\4B\30`\370N\321H\347x@pFNOL\337\2\36NuC\372\0Ha\30`\0\377\\C\372\0]a\16`\0\377RC\372\0ua\4`\0\377Hp!NONu\32\0\33[16;35HHuman.sys \202\252 \214\251\202\302\202\251\202\350\202\334\202\271\202\361\0\33[16;38H\203f\203B\203X\203N\202\252\201@\223\307\202\337\202\334\202\271\202\361\0\33[16;36HHuman.sys \202\252 \211\363\202\352\202\304\202\242\202\334\202\267\0\33[16;33HHuman.sys \202\314 \203A\203h\203\214\203X\202\252\210\331\217\355\202\305\202\267\0human   sys\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0".getBytes (XEiJ.ISO_8859_1);



  //========================================================================================
  //$$HDU SASI HDユニット
  //  SASIハードディスクのユニット
  public static class HDUnit extends AbstractUnit {

    public HDMedia hduMedia;  //メディアの種類
    public byte[] hduImage;  //ディスクイメージ
    public boolean hduWritten;  //true=書き込みがあった

    //new HDUnit (number)
    //  コンストラクタ
    public HDUnit (int number) {
      super (number);
      hduImage = null;
      hduWritten = false;
    }

    //hduTini ()
    //  後始末
    //  イメージファイルに書き出す
    public void hduTini () {
      if (abuInserted) {
        hduFlush ();
      }
    }  //hduTini()

    //success = unit.hduFlush ()
    //  イメージファイルに書き出す
    public boolean hduFlush () {
      if (!hduWritten) {  //書き込みがない
        return true;
      }
      if (abuWriteProtected) {  //書き込みが許可されていない
        return false;
      }
      if (!XEiJ.rscPutFile (abuPath, hduImage, 0, (int) hduMedia.humDiskEndByte)) {
        return false;
      }
      hduWritten = false;
      return true;
    }  //hduFlush()

    //unit.connect (disconnectable)
    //  接続する
    @Override protected void connect (boolean disconnectable) {
      super.connect (disconnectable);
      hduImage = new byte[HDMedia.HDM_MAX_BYTES_PER_DISK];
      hduWritten = false;
    }

    //unit.disconnect ()
    //  切り離す
    @Override protected void disconnect () {
      super.disconnect ();
      hduImage = null;
    }

    //success = unit.eject ()
    //  イジェクトする
    @Override protected boolean eject () {
      if (!hduFlush ()) {  //イメージファイルに書き出す
        return false;
      }
      String path = abuPath;  //イジェクトされたイメージファイルのパス。super.eject()を呼び出す前にコピーすること
      if (!super.eject ()) {  //イジェクトする
        return false;
      }
      if (hduMedia != null) {  //挿入されていたとき
        hdcAddHistory (new File (path).getAbsoluteFile ());
        System.out.println (Multilingual.mlnJapanese ?
                            path + " を sa" + abuNumber + " から切り離しました" :
                            path + " was removed from sa" + abuNumber);
      }
      hduMedia = null;
      //hdcHDMaxを更新する
      if (hdcHDMax == abuNumber - 1) {
        int u = abuNumber - 1;
        while (u >= 0 && !hdcUnitArray[u].abuInserted) {  //挿入されている最後のユニットまで遡る
          u--;
        }
        hdcHDMax = u + 1;
      }
      return true;
    }

    //success = unit.open ()
    //  開くダイアログを開く
    @Override protected boolean open () {
      if (!super.open ()) {
        return false;
      }
      hdcOpenUnit = abuNumber;
      if (hdcOpenDialog == null) {
        hdcOpenDialog = new OpenDialog ();
        hdcOpenDialog.setReadOnly (Settings.sgsGetOnOff ("sareadonly"));
        hdcOpenDialog.setReboot (Settings.sgsGetOnOff ("saappreboot"));
        for (File[] files : hdcOpenHistory) {
          hdcOpenDialog.addHistory (files);
        }
        hdcOpenHistory.clear ();
      }
      hdcOpenDialog.rescanCurrentDirectory ();  //挿入されているファイルが変わると選択できるファイルも変わるのでリストを作り直す
      XEiJ.pnlExitFullScreen (true);
      hdcOpenDialog.setVisible (true);
      return true;
    }  //unit.open()

    //success = unit.insert (path, writeProtected)
    //  挿入する
    @Override protected boolean insert (String path, boolean writeProtected) {
      if (hdcIsInserted (path)) {  //既に挿入されている
        return false;
      }
      if (!super.insert (path, writeProtected)) {  //挿入できなかった
        return false;
      }
      //hdcHDMaxを更新する
      if (hdcHDMax <= abuNumber) {
        hdcHDMax = abuNumber + 1;
      }
      return true;
    }  //unit.insert(String)

    //loaded = unit.load (path)
    //  読み込む
    @Override protected boolean load (String path) {
      hduMedia = HDMedia.hdmPathToMedia (path, hduImage);
      if (hduMedia == null) {  //読み込めない
        return false;
      }
      hduWritten = false;
      hdcAddHistory (new File (path).getAbsoluteFile ());
      System.out.println (Multilingual.mlnJapanese ?
                          path + " を sa" + abuNumber + " に接続しました" :
                          path + " was connected to sa" + abuNumber);
      return true;
    }

  }  //class HDUnit



  public static int hdcPeekByte (int a) {
    if (SPC.spcSCSIINOn) {  //SCSI内蔵機
      if ((a & 0x20) == 0) {  //SASIポート
        return ((a & 0x02) == 0 ? 0x00 : 0xff);
      } else {  //SCSIポート
        return SPC.spcSCSIINChip.spiPeek (a);
      }
    } else {  //SASI内蔵機
      switch (a & 0x3f) {
      case HDC_DATA_PORT & 0x3f:  //0x00e96001
        return hdcPeekData ();
      case HDC_STATUS_PORT & 0x3f:  //0x00e96003
        return hdcPeekStatus ();
      case HDC_RESET_PORT & 0x3f:  //0x00e96005
        return 0xff;
      case HDC_SELECTION_PORT & 0x3f:  //0x00e96007
        return 0xff;
      default:
        return 0xff;
      }
    }
  }  //hdcPeekByte

  public static int hdcReadByte (int a) {
    if (SPC.spcSCSIINOn) {  //SCSI内蔵機
      if ((a & 0x20) == 0) {  //SASIポート
        return ((a & 0x02) == 0 ? 0x00 : 0xff);
      } else {  //SCSIポート
        return SPC.spcSCSIINChip.spiRead (a);
      }
    } else {  //SASI内蔵機
      switch (a & 0x3f) {
      case HDC_DATA_PORT & 0x3f:  //0x00e96001
        return hdcReadData ();
      case HDC_STATUS_PORT & 0x3f:  //0x00e96003
        return hdcReadStatus ();
      case HDC_RESET_PORT & 0x3f:  //0x00e96005
        return 0xff;
      case HDC_SELECTION_PORT & 0x3f:  //0x00e96007
        return 0xff;
      default:
        return 0xff;
      }
    }
  }  //hdcReadByte

  public static void hdcWriteByte (int a, int d) {
    if (SPC.spcSCSIINOn) {  //SCSI内蔵機
      if ((a & 0x20) == 0) {  //SASIポート
        return;
      } else {  //SCSIポート
        SPC.spcSCSIINChip.spiWrite (a, d);
        return;
      }
    } else {  //SASI内蔵機
      switch (a & 0x3f) {
      case HDC_DATA_PORT & 0x3f:  //0x00e96001
        hdcWriteData (d);
        return;
      case HDC_STATUS_PORT & 0x3f:  //0x00e96003
        hdcWriteCommand (d);
        return;
      case HDC_RESET_PORT & 0x3f:  //0x00e96005
        hdcWriteReset (d);
        return;
      case HDC_SELECTION_PORT & 0x3f:  //0x00e96007
        hdcWriteSelect (d);
        return;
      default:
        return;
      }
    }
  }  //hdcWriteByte



}  //class HDC



