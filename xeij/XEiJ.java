//========================================================================================
//  XEiJ.java
//    en:Main class
//    ja:メインクラス
//  Copyright (C) 2003-2026 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;  //ScheduledExecutorService
import java.util.regex.*;
import java.util.stream.*;
import java.util.zip.*;
import javax.imageio.*;
import javax.imageio.stream.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

public class XEiJ {

  //システムプロパティ
  //  mainクラスの先頭に書く
  //  これでも駄目(間に合わない)ならjavaのオプションに書く
  //    java -Dsun.java2d.d3d=false -Dsun.java2d.xrender=false -jar XEiJ.jar ...
  static {
    System.setProperty ("sun.java2d.d3d", "false");
    System.setProperty ("sun.java2d.xrender", "false");
  }

  //名前とバージョン
  public static final String PRG_TITLE = "XEiJ (X68000 Emulator in Java)";  //タイトル
  public static final String PRG_VERSION = "0.26.10.08";  //バージョン
  public static final String PRG_AUTHOR = "Makoto Kamada";  //作者
  public static final String PRG_WEBPAGE = "https://stdkmd.net/xeij/";  //ウェブページ

  public static final String PRG_JAVA_VENDOR = "Oracle Corporation";  //動作を確認しているJavaのベンダー
  public static final String PRG_JAVA_VERSION = "26.0.2.1";  //動作を確認しているJavaのバージョン
  public static final String PRG_OS_ARCH = "amd64";  //動作を確認しているOSのアーキテクチャ
  public static final String PRG_OS_NAME = "Windows 11";  //動作を確認しているOSの名前
  public static final String PRG_JAVA_MINIMUM_REQUIRED = "26";  //Javaの最低要件

  //全体の設定
  //  bit0..3のテストにシフトを使う
  //    TEST_BIT_0_SHIFT ? a << 31 != 0 : (a & 1) != 0
  //    TEST_BIT_1_SHIFT ? a << 30 < 0 : (a & 2) != 0
  //    TEST_BIT_2_SHIFT ? a << 29 < 0 : (a & 4) != 0
  //    TEST_BIT_3_SHIFT ? a << 28 < 0 : (a & 8) != 0
  public static final boolean TEST_BIT_0_SHIFT = false;  //true=bit0のテストにシフトを使う
  public static final boolean TEST_BIT_1_SHIFT = false;  //true=bit1のテストにシフトを使う
  public static final boolean TEST_BIT_2_SHIFT = true;  //true=bit2のテストにシフトを使う
  public static final boolean TEST_BIT_3_SHIFT = true;  //true=bit3のテストにシフトを使う
  //  shortの飽和処理にキャストを使う
  //    x = SHORT_SATURATION_CAST ? (short) x == x ? x : x >> 31 ^ 32767 : Math.max (-32768, Math.min (32767, x));
  //    m = SHORT_SATURATION_CAST ? (short) m == m ? m : m >> 31 ^ 32767 : Math.max (-32768, Math.min (32767, m));
  //    l = SHORT_SATURATION_CAST ? (short) l == l ? l : l >> 31 ^ 32767 : Math.max (-32768, Math.min (32767, l));
  //    r = SHORT_SATURATION_CAST ? (short) r == r ? r : r >> 31 ^ 32767 : Math.max (-32768, Math.min (32767, r));
  //  または
  //    if (SHORT_SATURATION_CAST) {
  //      if ((short) x != x) {
  //        x = x >> 31 ^ 32767;
  //      }
  //    } else {
  //      if (x < -32768) {
  //        x = -32768;
  //      } else if (x > 32767) {
  //        x = 32767;
  //      }
  //    }
  public static final boolean SHORT_SATURATION_CAST = false;  //shortの飽和処理にキャストを使う

  //バイナリデータの埋め込み
  //  byte[]の場合
  //    Javaはbyteの定数配列をstatic final byte[] XXX={～}で直接記述しにくい
  //      bit7がセットされているデータをいちいち(byte)でキャストしなければならない
  //      初期化コードが巨大化してコンパイラを通らなくなる
  //    Stringに詰め込んで起動時にString.getBytes(Charset)を使ってbyte[]に展開する
  //    ISO-8859-1はすべてのJava実行環境で実装しなければならないことになっているので環境依存にはならない
  //    static final int[] XXX={～}で書いておいてPerlスクリプトで文字列に変換する
  //    final int[]をbyte[]に変更すると動作が遅くなる場合があることに注意する
  //    perl misc/itob.pl xeij/???.java XXX
  public static final Charset ISO_8859_1 = Charset.forName ("ISO-8859-1");
  static {
    if (false) {
      //ISO-8859-1が8bitバイナリデータを素通りさせるかどうかのテスト
      StringBuilder sb = new StringBuilder ();
      for (int i = 0; i < 256; i++) {
        sb.append ((char) i);
      }
      byte[] bb = sb.toString ().getBytes (ISO_8859_1);
      for (int i = 0; i < 256; i++) {
        System.out.printf ("%02x %02x %s\n", i, bb[i] & 255, i == (bb[i] & 255) ? "OK" : "ERROR");
      }
    }
  }
  //  char[]の場合
  //    byte[]の場合と同様にStringに詰め込んで起動時にString.toCharArray()を使ってchar[]に展開する
  //    static final int[] XXX={～}で書いておいてPerlスクリプトで文字列に変換する
  //    final int[]をchar[]に変更すると動作が遅くなる場合があることに注意する
  //    perl misc/itoc.pl xeij/???.java XXX



  //========================================================================================
  //$$PRG プログラムの入り口と出口

  //動作環境
  public static String prgJavaVendor;  //動作環境のJavaのベンダー
  public static String prgJavaVersion;  //動作環境のJavaのバージョン
  public static String prgOsArch;  //動作環境のアーキテクチャ
  public static String prgOsName;  //OSの名前
  public static boolean prgIsLinux;  //true=Linux
  public static boolean prgIsMac;  //true=Mac
  public static boolean prgIsWindows;  //true=Windows

  public static boolean prgCaseIgnored;  //true=ファイル名の大文字と小文字が区別されない

  public static boolean prgVerbose;
  public static boolean prgTiniLog;

  public static String[] prgArgs;

  //main (args)
  //  開始
  public static void main (String[] args) {

    //jSerialCommの不具合対策
    //  jSerialCommはキャッシュされたdllファイルの扱いが雑で問題を引き起こすことがある
    //  XEiJの起動時にキャッシュディレクトリを削除して毎回dllファイルを作り直させる
    if (true) {
      try {
        String def = System.getProperty ("os.name").toLowerCase ().contains ("win") ? "C:\\Temp" : "/tmp";
        Stream.of (new File (System.getProperty ("java.io.tmpdir", def),
                             "jSerialComm").getCanonicalFile ().toPath (),
                   new File (System.getProperty ("user.home", def),
                             ".jSerialComm").getCanonicalFile ().toPath ())
          .filter (Files::exists)
            .forEach (dir -> {
              try {
                Files.walk (dir)
                  .sorted (Comparator.reverseOrder ())
                    .forEach (path -> {
                      if (Files.exists (path)) {
                        try {
                          Files.delete (path);
                          //System.out.println ("Deleted: " + path);
                        } catch (IOException ioe) {
                          System.err.println ("Failed to delete: " + path + " (" + ioe.getMessage () + ")");
                        }
                      }
                    });
              } catch (IOException ioe) {
                System.err.println ("Failed to walk: " + dir + " (" + ioe.getMessage () + ")");
              }
            });
      } catch (IOException ioe) {
      }
    }

    prgArgs = args;

    //起動する
    SwingUtilities.invokeLater (new Runnable () {
      @Override public void run () {
        new XEiJ ();
      }
    });

  }  //main(String[])

  //XEiJ ()
  //  コンストラクタ
  public XEiJ () {

    prgJavaVendor = System.getProperty ("java.vendor");
    prgJavaVersion = System.getProperty ("java.version");
    prgOsArch = System.getProperty ("os.arch");
    prgOsName = System.getProperty ("os.name");
    prgIsLinux = 0 <= prgOsName.indexOf ("Linux");
    prgIsMac = 0 <= prgOsName.indexOf ("Mac");
    prgIsWindows = 0 <= prgOsName.indexOf ("Windows");

    System.out.print ("\n" +
                      "-------------------------------------------------\n" +
                      PRG_TITLE + " version " + PRG_VERSION + "\n" +
                      "-------------------------------------------------\n");

    //初期化
    //  この段階でコンポーネントを参照してはならない
    //  メニューの初期値に必要な情報があればここで作っておく
    prgCaseIgnored = new File ("A").equals (new File ("a"));  //ファイル名の大文字と小文字が区別されるか
    fmtInit ();  //BCD 10進数変換
    Multilingual.mlnInit ();  //MLN 多言語化  sgsInit()よりも前

    System.out.println (Multilingual.mlnJapanese ? "java.vendor は " + prgJavaVendor + " です" :
                        "java.vendor is " + prgJavaVendor);
    System.out.println (Multilingual.mlnJapanese ? "java.version は " + prgJavaVersion + " です" :
                        "java.version is " + prgJavaVersion);
    System.out.println (Multilingual.mlnJapanese ? "os.arch は " + prgOsArch + " です" :
                        "os.arch is " + prgOsArch);
    System.out.println (Multilingual.mlnJapanese ? "os.name は " + prgOsName + " です" :
                        "os.name is " + prgOsName);

    rbtInit ();  //RBT ロボット

    Settings.sgsInit ();  //SGS 設定  mlnInit()よりも後、他の～Init()よりも前
    LnF.lnfInit ();  //Look&Feel
    Bubble.bblInit ();  //BBL バブル lnfInit()よりも後

    CharacterCode.chrInit ();  //CHR 文字コード

    TickerQueue.tkqInit ();  //TKQ ティッカーキュー

    tmrStart ();  //TMR タイマー trmInitより前

    RS232CTerminal.trmInit ();  //TRM RS-232C設定とターミナル。tkqInitよりも後
    if (Malfunction.MLF_ON) {
      Malfunction.mlfInit ();
    }

    if (MercuryUnit.MU4_ON) {
      MercuryUnit.mu4Init ();
    }
    xt3Init ();  //XT3 Xellent30
    mdlInit ();  //MDL 機種。sgsInit()よりも後。spcInitより前

    if (InstructionBreakPoint.IBP_ON) {
      InstructionBreakPoint.ibpInit ();  //IBP 命令ブレークポイント
    }
    if (DataBreakPoint.DBP_ON) {
      DataBreakPoint.dbpInit ();  //DBP データブレークポイント
    }
    busInit ();  //BUS バスコントローラ  ibpInit()とdbpInit()よりも後
    MainMemory.mmrInit ();  //MMR メインメモリ。romInitよりも前
    ROM.romInit ();  //ROM ROM。mpuInit()よりも前。mmrInitよりも後
    CRTC.crtInit ();  //CRT CRTコントローラ
    VideoController.vcnInit ();  //VCN ビデオコントローラ
    HD63450.dmaInit ();  //DMA DMAコントローラ
    svsInit ();  //SVS スーパーバイザ領域設定
    MC68901.mfpInit ();  //MFP MFP
    sysInit ();  //SYS システムポート
    if (OPMLog.OLG_ON) {
      OPMLog.olgInit ();  //OLG OPMログ。opmInitよりも前
    }
    OPM.opmInit ();  //OPM FM音源
    ADPCM.pcmInit ();  //PCM ADPCM音源
    FDC.fdcInit ();  //FDC FDコントローラ
    HDC.hdcInit ();  //HDC SASI HDコントローラ
    if (HostCDROM.HCD_ENABLED) {  //HCD ホストCD-ROM  spcInitよりも前
      HostCDROM.hcdInit ();
    }
    SPC.spcInit ();  //SPC SCSIプロトコルコントローラ  hcdInitよりも後。mdlInitより後
    Z8530.sccInit ();  //SCC SCC  rbtInit()よりも後
    IOInterrupt.ioiInit ();  //IOI I/O割り込み
    SpriteScreen.sprInit ();  //SPR スプライト画面
    bnkInit ();  //BNK バンクメモリ
    SRAM.smrInit ();  //SMR SRAM

    PPI.ppiInit ();  //PPI PPI
    PrinterPort.prnInit ();  //PRN プリンタポート
    Indicator.indInit ();  //IND インジケータ rtcInitより前,pwcInitより前
    RP5C15.rtcInit ();  //RTC RTC indInitより後
    if (PowerControl.PWC_ON) {
      PowerControl.pwcInit ();  //PWC 電源制御 indInitより後
    }

    SlowdownTest.sdtInit ();  //SDT 鈍化テスト
    Keyboard.kbdInit ();  //KBD キーボード  keydlyとkeyrepはSRAMが先なのでsmrInit()よりも後
    CONDevice.conInit ();  //CON CONデバイス制御
    Mouse.musInit ();  //MUS マウス
    pnlInit ();  //PNL パネル
    frmInit ();  //FRM フレーム

    dbgInit ();  //DBG デバッガ共通コンポーネント
    RegisterList.drpInit ();  //DRP レジスタ
    DisassembleList.ddpInit ();  //DDP 逆アセンブルリスト
    MemoryDumpList.dmpInit ();  //DMP メモリダンプリスト
    LogicalSpaceMonitor.atwInit ();  //ATW アドレス変換ウインドウ
    PhysicalSpaceMonitor.paaInit ();  //PAA 物理アドレス空間ウインドウ
    DebugConsole.dgtInit ();  //DGT デバッグコンソール
    if (BranchLog.BLG_ON) {
      BranchLog.blgInit ();  //BLG 分岐ログ
    }
    if (ProgramFlowVisualizer.PFV_ON) {
      ProgramFlowVisualizer.pfvInit ();  //PFV プログラムフロービジュアライザ
    }
    if (RasterBreakPoint.RBP_ON) {
      RasterBreakPoint.rbpInit ();  //RBP ラスタブレークポイント
    }
    if (ScreenModeTest.SMT_ON) {
      ScreenModeTest.smtInit ();  //SMT 表示モードテスト
    }
    if (RootPointerList.RTL_ON) {
      RootPointerList.rtlInit ();  //RTL ルートポインタリスト
    }
    if (SpritePatternViewer.SPV_ON) {
      SpritePatternViewer.spvInit ();  //SPV スプライトパターンビュア
    }
    if (PaletteViewer.PLV_ON) {
      PaletteViewer.plvInit ();  //PLV パレットビュア
    }
    if (ATCMonitor.ACM_ON) {
      ATCMonitor.acmInit ();  //ACM アドレス変換キャッシュモニタ
    }

    SoundSource.sndInit ();  //SND サウンド
    FEFunction.fpkInit ();  //FPK FEファンクション
    mpuInit ();  //MPU MPU。romInit()よりも後
    MC68060.mmuInit ();  //MMU メモリ管理ユニット
    SoundMonitor.smnInit ();  //SMN 音声モニタ
    HFS.hfsInit ();  //HFS ホストファイルシステムインタフェイス

    GIFAnimation.gifInit ();  //GIF GIFアニメーション
    if (FlashingLights.FLR_ON) {
      FlashingLights.flrInit ();  //FLR 点滅光の軽減  pnlInitより後
    }
    TextCopy.txcInit ();  //TXC テキスト画面コピー
    ButtonFunction.bfnInit ();  //BFN ボタン機能割り当て

    //コンポーネントを作る
    //  他のコンポーネントを参照するときは順序に注意する
    Settings.sgsMakeMenu ();  //SGS 設定  mnbMakeMenu()よりも前
    mdlMakeMenu ();  //MDL 機種  mnbMakeMenu(),mpuMakeMenu()よりも前
    FDC.fdcMakeMenu ();  //FDC FDコントローラ  mnbMakeMenu()よりも前
    HDC.hdcMakeMenu ();  //HDC SASI HDコントローラ  mdlMakeMenu()よりも後,mnbMakeMenu()よりも前
    SPC.spcMakeMenu ();  //SPC SCSIプロトコルコントローラ  mdlMakeMenu()よりも後,mnbMakeMenu()よりも前
    SRAM.smrMakeMenu ();  //SMR SRAM  mnbMakeMenuより前、mpuMakeMenuより前
    mpuMakeMenu ();  //MPU MPU  mdlMakeMenuより後、smrMakeMenuより後、mnbMakeMenuより前
    clpMake ();  //CLP クリップボード  mnbMakeMenu()よりも前
    pnlMake ();  //PNL パネル  mnbMakeMenu()よりも前
    mnbMakeMenu ();  //MNB メニューバー  pnlMake()よりも後
    frmMake ();  //FRM フレーム
    dbgMakePopup ();  //DBG デバッガ共通コンポーネント

    if (FlashingLights.FLR_ON) {
      FlashingLights.flrInform ();  //mnbMakeMenuより後
    }

    //デバッグフラグを消し忘れないようにする
    final String flags = (
      "" +
      (EFPBox.CIR_DEBUG_TRACE ? " EFPBox.CIR_DEBUG_TRACE" : "") +
      (FDC.FDC_DEBUG_TRACE ? " FDC.FDC_DEBUG_TRACE" : "") +
      (FEFunction.FPK_DEBUG_TRACE ? " FEFunction.FPK_DEBUG_TRACE" : "") +
      (HD63450.DMA_DEBUG_TRACE != 0 ? " HD63450.DMA_DEBUG_TRACE" : "") +
      (HDC.HDC_DEBUG_TRACE ? " HDC.HDC_DEBUG_TRACE" : "") +
      (HDC.HDC_DEBUG_COMMAND ? " HDC.HDC_DEBUG_COMMAND" : "") +
      (HFS.HFS_DEBUG_TRACE ? " HFS.HFS_DEBUG_TRACE" : "") +
      (HFS.HFS_DEBUG_FILE_INFO ? " HFS.HFS_DEBUG_FILE_INFO" : "") +
      (HFS.HFS_COMMAND_TRACE ? " HFS.HFS_COMMAND_TRACE" : "") +
      (HFS.HFS_BUFFER_TRACE ? " HFS.HFS_BUFFER_TRACE" : "") +
      (IOInterrupt.IOI_DEBUG_TRACE ? " IOInterrupt.IOI_DEBUG_TRACE" : "") +
      (Keyboard.KBD_DEBUG_LED ? " Keyboard.KBD_DEBUG_LED" : "") +
      (MC68060.MMU_DEBUG_COMMAND ? " MC68060.MMU_DEBUG_COMMAND" : "") +
      (MC68060.MMU_DEBUG_TRANSLATION ? " MC68060.MMU_DEBUG_TRANSLATION" : "") +
      (MC68060.MMU_NOT_ALLOCATE_CACHE ? " MC68060.MMU_NOT_ALLOCATE_CACHE" : "") +
      (RP5C15.RTC_DEBUG_TRACE ? " RP5C15.RTC_DEBUG_TRACE" : "") +
      (SPC.SPC_DEBUG_ON ? " SPC.SPC_DEBUG_ON" : "") +
      (Z8530.SCC_DEBUG_ON ? " Z8530.SCC_DEBUG_ON" : "")
      );
    if (!"".equals (flags)) {
      pnlExitFullScreen (true);
      JOptionPane.showMessageDialog (null, "debug flags:" + flags);
    }

    //動作を開始する
    //  イベントリスナーを設定する
    //  タイマーを起動する

    Keyboard.kbdStart ();  //KBD キーボード
    Mouse.musStart ();  //MUS マウス
    pnlStart ();  //PNL パネル
    frmStart ();  //FRM フレーム
    SoundSource.sndStart ();  //SND サウンド

    if (DataBreakPoint.DBP_ON) {
      DataBreakPoint.dbpStart ();  //DBP データブレークポイント
    }
    if (RasterBreakPoint.RBP_ON) {
      RasterBreakPoint.rbpStart ();  //RBP ラスタブレークポイント
    }
    if (ScreenModeTest.SMT_ON) {
      ScreenModeTest.smtStart ();  //SMT 表示モードテスト
    }
    if (OPMLog.OLG_ON) {
      OPMLog.olgStart ();  //OLG OPMログ
    }
    SoundMonitor.smnStart ();  //SMN 音声モニタ
    RS232CTerminal.trmStart ();  //TRM ターミナルウインドウ
    if (Malfunction.MLF_ON) {
      Malfunction.mlfStart ();
    }
    PPI.ppiStart ();  //PPI PPI
    PrinterPort.prnStart ();  //PRN プリンタポート
    if (BranchLog.BLG_ON) {
      BranchLog.blgStart ();  //BLG 分岐ログ
    }
    if (ProgramFlowVisualizer.PFV_ON) {
      ProgramFlowVisualizer.pfvStart ();  //PFV プログラムフロービジュアライザ
    }
    RegisterList.drpStart ();  //DRP レジスタ
    DisassembleList.ddpStart ();  //DDP 逆アセンブルリスト
    MemoryDumpList.dmpStart ();  //DMP メモリダンプリスト
    LogicalSpaceMonitor.atwStart ();  //ATW アドレス変換ウインドウ
    PhysicalSpaceMonitor.paaStart ();  //PAA 物理アドレス空間ウインドウ
    DebugConsole.dgtStart ();  //DGT コンソール
    if (RootPointerList.RTL_ON) {
      RootPointerList.rtlStart ();  //RTL ルートポインタリスト
    }
    if (SpritePatternViewer.SPV_ON) {
      SpritePatternViewer.spvStart ();  //SPV スプライトパターンビュア
    }
    if (PaletteViewer.PLV_ON) {
      PaletteViewer.plvStart ();  //PLV パレットビュア
    }
    if (ATCMonitor.ACM_ON) {
      ATCMonitor.acmStart ();  //ACM アドレス変換キャッシュモニタ
    }
    ButtonFunction.bfnStart ();  //BFN ボタン機能割り当て

    if (Settings.sgsSaveiconValue != null) {
      String[] a = Settings.sgsSaveiconValue.split (",");
      if (0 < a.length) {
        saveIcon (a[0], LnF.LNF_ICON_IMAGES);
        if (1 < a.length) {
          saveImage (LnF.LNF_ICON_IMAGE_16, a[1]);
          if (2 < a.length) {
            saveImage (LnF.LNF_ICON_IMAGE_32, a[2]);
            if (3 < a.length) {
              saveImage (LnF.LNF_ICON_IMAGE_48, a[3]);
            }
          }
        }
      }
      prgTini ();
      return;
    }

    //コアを起動する
    if (PowerControl.PWC_ON) {
      PowerControl.pwcStartup ();
    } else {
      mpuReset (-1, -1);
    }

    pnlBoot2 ();

  }  //コンストラクタ



  //prgTini ()
  //  プログラムの後始末
  public static void prgTini () {
    if (prgTiniLog) {
      System.out.println ("prgTini: call mpuStop");
    }
    mpuStop (null);
    try {
      if (OPMLog.OLG_ON) {
        if (prgTiniLog) {
          System.out.println ("prgTini: call orgTini");
        }
        OPMLog.olgTini ();  //OLG OPMログ
      }
      if (prgTiniLog) {
        System.out.println ("prgTini: call bfnTini");
      }
      ButtonFunction.bfnTini ();  //BFN ボタン機能割り当て
      if (prgTiniLog) {
        System.out.println ("prgTini: call txcTini");
      }
      TextCopy.txcTini ();  //TXC テキスト画面コピー
      if (prgTiniLog) {
        System.out.println ("prgTini: call gifTini");
      }
      GIFAnimation.gifTini ();  //GIFアニメーション
      if (FlashingLights.FLR_ON) {
        if (prgTiniLog) {
          System.out.println ("prgTini: call flrTini");
        }
        FlashingLights.flrTini ();  //FLR 点滅光の軽減
      }
      if (prgTiniLog) {
        System.out.println ("prgTini: call sndTini");
      }
      SoundSource.sndTini ();  //SND サウンド
      if (prgTiniLog) {
        System.out.println ("prgTini: call kbdTini");
      }
      Keyboard.kbdTini ();  //KBD キーボード
      if (prgTiniLog) {
        System.out.println ("prgTini: call musTini");
      }
      Mouse.musTini ();  //MUS マウス
      if (prgTiniLog) {
        System.out.println ("prgTini: call conTini");
      }
      CONDevice.conTini ();  //CON CONデバイス制御
      if (prgTiniLog) {
        System.out.println ("prgTini: call ppiTini");
      }
      PPI.ppiTini ();  //PPI PPI
      if (prgTiniLog) {
        System.out.println ("prgTini: call prnTini");
      }
      PrinterPort.prnTini ();  //PRN プリンタポート
      if (PowerControl.PWC_ON) {
        if (prgTiniLog) {
          System.out.println ("prgTini: call pwcTini");
        }
        PowerControl.pwcTini ();  //PWC 電源制御
      }
      if (prgTiniLog) {
        System.out.println ("prgTini: call fdcTini");
      }
      FDC.fdcTini ();  //FDC FDコントローラ
      if (prgTiniLog) {
        System.out.println ("prgTini: call hdcTini");
      }
      HDC.hdcTini ();  //HDC SASI HDコントローラ
      if (HostCDROM.HCD_ENABLED) {  //HCD ホストCD-ROM
        if (prgTiniLog) {
          System.out.println ("prgTini: call hcdTini");
        }
        HostCDROM.hcdTini ();
      }
      if (prgTiniLog) {
        System.out.println ("prgTini: call spcTini");
      }
      SPC.spcTini ();  //SPC SCSIプロトコルコントローラ
      if (prgTiniLog) {
        System.out.println ("prgTini: call hfsTini");
      }
      HFS.hfsTini ();  //HFS ホストファイルシステムインタフェイス
      if (prgTiniLog) {
        System.out.println ("prgTini: call sccTini");
      }
      Z8530.sccTini ();  //SCC SCC
      if (prgTiniLog) {
        System.out.println ("prgTini: call crtTini");
      }
      CRTC.crtTini ();  //CRT CRTコントローラ
      if (prgTiniLog) {
        System.out.println ("prgTini: call sprTini");
      }
      SpriteScreen.sprTini ();  //SPR スプライト画面
      if (prgTiniLog) {
        System.out.println ("prgTini: call pnlTini");
      }
      pnlTini ();  //PNL パネル
      if (prgTiniLog) {
        System.out.println ("prgTini: call bnkTini");
      }
      bnkTini ();  //BNK バンクメモリ
      if (prgTiniLog) {
        System.out.println ("prgTini: call romTini");
      }
      ROM.romTini ();  //ROM
      if (MercuryUnit.MU4_ON) {
        if (prgTiniLog) {
          System.out.println ("prgTini: call mu4Tini");
        }
        MercuryUnit.mu4Tini ();
      }
      if (prgTiniLog) {
        System.out.println ("prgTini: call xt3Tini");
      }
      xt3Tini ();  //XT3 Xellent30
      if (prgTiniLog) {
        System.out.println ("prgTini: call mdlTini");
      }
      mdlTini ();  //MDL 機種
      if (prgTiniLog) {
        System.out.println ("prgTini: call smrTini");
      }
      SRAM.smrTini ();  //SMR SRAM
      if (prgTiniLog) {
        System.out.println ("prgTini: call busTini");
      }
      busTini ();  //BUS バスコントローラ
      if (SpritePatternViewer.SPV_ON) {
        if (prgTiniLog) {
          System.out.println ("prgTini: call spvTini");
        }
        SpritePatternViewer.spvTini ();  //SPV スプライトパターンビュア
      }
      if (PaletteViewer.PLV_ON) {
        if (prgTiniLog) {
          System.out.println ("prgTini: call plvTini");
        }
        PaletteViewer.plvTini ();  //PLV パレットビュア
      }
      if (prgTiniLog) {
        System.out.println ("prgTini: call trmTini");
      }
      RS232CTerminal.trmTini ();  //TRM RS-232C設定とターミナル
      if (Malfunction.MLF_ON) {
        Malfunction.mlfTini ();
      }
      if (prgTiniLog) {
        System.out.println ("prgTini: call lnfTini");
      }
      LnF.lnfTini ();
      if (prgTiniLog) {
        System.out.println ("prgTini: call tmrTini");
      }
      tmrTini ();  //TMR タイマー
      if (prgTiniLog) {
        System.out.println ("prgTini: call sgsTini");
      }
      Settings.sgsTini ();  //SGS 設定
    } catch (Exception e) {  //終了時に予期しないエラーが発生すると終了できなくなってしまうので、すべてのExceptionをcatchする
      e.printStackTrace ();
    }
    System.exit (0);
  }  //prgTini()

  //prgOpenJavaDialog ()
  //  Java実行環境の情報
  public static void prgOpenJavaDialog () {
    pnlExitFullScreen (true);
    JOptionPane.showMessageDialog (
      frmFrame,
      ComponentFactory.createGridPanel (
        4,  //colCount
        6,  //rowCount
        "paddingLeft=6,paddingRight=6",  //gridStyles
        "italic,right;left;left",  //colStyless
        "italic,center;colSpan=4,widen",  //rowStyless
        "",  //cellStyless
        //
        null,  //(0,0)
        Multilingual.mlnJapanese ? "実行中" : "Current",  //(1,0)
        Multilingual.mlnJapanese ? "推奨" : "Recommended",  //(2,0)
        Multilingual.mlnJapanese ? "最低要件" : "Minimum required",  //(3,0)
        //
        ComponentFactory.createHorizontalSeparator (),  //(0,1)
        //
        Multilingual.mlnJapanese ? "Java のベンダー" : "Java vendor",  //(0,2)
        prgJavaVendor,  //(1,2)
        PRG_JAVA_VENDOR,  //(2,2)
        null,  //(3,2)
        //
        Multilingual.mlnJapanese ? "Java のバージョン" : "Java version",  //(0,3)
        prgJavaVersion,  //(1,3)
        PRG_JAVA_VERSION,  //(2,3)
        PRG_JAVA_MINIMUM_REQUIRED,  //(3,3)
        //
        Multilingual.mlnJapanese ? "OS のアーキテクチャ" : "OS architecture",  //(0,4)
        prgOsArch,  //(1,4)
        PRG_OS_ARCH,  //(2,4)
        null,  //(3,4)
        //
        Multilingual.mlnJapanese ? "OS の名前" : "OS name",  //(0,5)
        prgOsName,  //(1,5)
        PRG_OS_NAME,  //(2,5)
        null  //(3,5)
        ),
      Multilingual.mlnJapanese ? "Java 実行環境の情報" : "Java runtime environment information",
      JOptionPane.PLAIN_MESSAGE);
  }  //prgOpenJavaDialog()

  //prgOpenAboutDialog ()
  //  バージョン情報
  public static void prgOpenAboutDialog () {
    pnlExitFullScreen (true);
    JOptionPane.showMessageDialog (
      frmFrame,
      ComponentFactory.createGridPanel (
        2, 4, "paddingLeft=6,paddingRight=6", "italic,right;left", "", "",
        Multilingual.mlnJapanese ? "タイトル" : "Title"  ,  //(0,0)
        PRG_TITLE,  //(1,0)
        Multilingual.mlnJapanese ? "バージョン" : "Version",  //(0,1)
        PRG_VERSION,  //(1,1)
        Multilingual.mlnJapanese ? "作者" : "Author" ,  //(0,2)
        PRG_AUTHOR,  //(1,2)
        Multilingual.mlnJapanese ? "ウェブページ" : "Webpage",  //(0,3)
        PRG_WEBPAGE  //(1,3)
        ),
      Multilingual.mlnJapanese ? "バージョン情報" : "Version information",
      JOptionPane.PLAIN_MESSAGE);
  }  //prgOpenAboutDialog()

  //prgOpenXEiJLicenseDialog ()
  //  XEiJライセンスダイアログ
  public static void prgOpenXEiJLicenseDialog () {
    pnlExitFullScreen (true);
    JOptionPane.showMessageDialog (
      frmFrame,
      ComponentFactory.createScrollTextPane (rscGetResourceText ("license_XEiJ.txt"), 550, 300),
      Multilingual.mlnJapanese ? "XEiJ 使用許諾条件" : "XEiJ License",
      JOptionPane.PLAIN_MESSAGE);
  }  //prgOpenXEiJLicenseDialog()

  //prgOpenSHARPLicenseDialog ()
  //  FSHARPライセンスダイアログ
  public static void prgOpenSHARPLicenseDialog () {
    pnlExitFullScreen (true);
    JOptionPane.showMessageDialog (
      frmFrame,
      ComponentFactory.createScrollTextPane (rscGetResourceText ("license_FSHARP.txt", "Shift_JIS"), 550, 300),
      Multilingual.mlnJapanese ? "無償公開された X68000 の基本ソフトウェア製品の許諾条件" : "License of the basic software products for X68000 that were distributed free of charge",
      JOptionPane.PLAIN_MESSAGE);
  }  //prgOpenSHARPLicenseDialog()

  //prgOpenYmfmLicenseDialog ()
  //  ymfmライセンスダイアログ
  public static void prgOpenYmfmLicenseDialog () {
    pnlExitFullScreen (true);
    JOptionPane.showMessageDialog (
      frmFrame,
      ComponentFactory.createScrollTextPane (rscGetResourceText ("license_ymfm.txt"), 550, 300),
      "ymfm License",
      JOptionPane.PLAIN_MESSAGE);
  }  //prgOpenYmfmLicenseDialog()

  //prgOpenJSerialCommLicenseDialog ()
  //  jSerialCommライセンスダイアログ
  public static void prgOpenJSerialCommLicenseDialog () {
    pnlExitFullScreen (true);
    JOptionPane.showMessageDialog (
      frmFrame,
      ComponentFactory.createVerticalSplitPane (
        ComponentFactory.createScrollTextPane (rscGetResourceText ("LICENSE-APACHE-2.0"), 550, 300),
        ComponentFactory.createScrollTextPane (rscGetResourceText ("LICENSE-LGPL-3.0"), 550, 300)
        ),
      "jSerialComm License",
      JOptionPane.PLAIN_MESSAGE);
  }  //prgOpenJSerialCommLicenseDialog()

  //prgPrintClass (o)
  //  オブジェクトを表示する
  public static void prgPrintClass (Object o) {
    System.out.println (o.toString ());
    //スーパークラスを遡る
    try {
      Stack<Class<?>> s = new Stack<Class<?>> ();
      for (Class<?> c = o.getClass (); c != null; c = c.getSuperclass ()) {
        s.push (c);
      }
      for (int i = 0; !s.empty (); i++) {
        for (int j = 0; j < i; j++) {
          System.out.print ("  ");
        }
        System.out.println (s.pop ().getName ());
      }
    } catch (Exception e) {
    }
  }  //prgPrintClass(Object)

  //prgPrintStackTrace ()
  //  スタックトレースを表示する
  //  メソッドがどのような経路で呼び出されたか確認したいときに使う
  public static void prgPrintStackTrace () {
    Exception e = new Exception ();
    e.fillInStackTrace ();
    prgPrintStackTraceOf (e);
  }  //prgPrintStackTrace()
  public static void prgPrintStackTraceOf (Exception e) {
    //e.printStackTrace ();
    System.out.println ("------------------------------------------------");
    System.out.println (e.toString ());
    System.out.println ("\t" + e.getMessage ());
    for (StackTraceElement ste : e.getStackTrace ()) {
      System.out.println ("\tat " + ste.toString ());
    }
    System.out.println ("------------------------------------------------");
  }  //prgPrintStackTraceOf()

  //prgStopOnce ()
  //  1回目だけ停止する。2回目以降は何もしない
  //  特定の条件で止めて近くにブレークポイントを仕掛けたいときに使う
  public static boolean prgStopDone = false;
  public static void prgStopOnce () {
    if (!prgStopDone) {
      prgStopDone = true;
      mpuStop (null);
    }
  }  //prgStopOnce()



  //========================================================================================
  //$$TMR タイマ
  //  tmrTimerは1つだけ存在する
  //  1つのタイマにスケジュールされたタスクはオーバーラップしない
  //  固定遅延実行
  //    tmrTimer.schedule (task, delay, interval)
  //    次回の実行開始予定時刻=max(今回の実行終了時刻,今回の実行開始時刻+interval)
  //  固定頻度実行
  //    tmrTimer.scheduleAtFixedRate (task, delay, interval)
  //    次回の実行開始予定時刻=max(今回の実行終了時刻,初回の実行開始時刻+interval*今回までの実行回数)

  //時刻の周波数
  //  mpuClockTimeなどのカウンタが1秒間に進む数
  //  10^10のとき
  //    1周期   0.1nanosecond
  //    2^31-1  0.2second
  //    2^53-1  10day
  //    2^63-1  29year
  //  10^11のとき
  //    1周期   10picosecond
  //    2^31-1  21millisecond
  //    2^53-1  1day
  //    2^63-1  2.9year
  //  10^12のとき
  //    1周期   1picosecond
  //    2^31-1  2.1millisecond
  //    2^53-1  2.5hour
  //    2^63-1  3.5month
  public static final long TMR_FREQ = 1000000000000L;  //10^12Hz。1ps

  //メインタイマ
  public static final long TMR_DELAY = 10L;  //ms
  public static final long TMR_INTERVAL = 10L;  //ms

  //タイマ
  public static java.util.Timer tmrTimer;  //Timerだけだとjavax.swing.Timerと紛らわしい

  //スケジューラ
  public static ScheduledExecutorService tmrScheduler;

  //tmrStart ()
  //  タイマを開始する
  public static void tmrStart () {
    tmrTimer = new java.util.Timer ();  //Timerだけだとjavax.swing.Timerと紛らわしい
    //スケジューラを開始する
    tmrScheduler = Executors.newScheduledThreadPool (3);
  }  //tmrStart()

  //tmrTini ()
  //  タイマの後始末
  public static void tmrTini () {
    if (tmrTimer != null) {
      tmrTimer.cancel ();
    }
    //スケジューラを終了する
    tmrScheduler.shutdown ();
    boolean terminated = false;
    try {
      terminated = tmrScheduler.awaitTermination (10L, TimeUnit.SECONDS);
    } catch (InterruptedException ie) {
    }
    if (!terminated) {
      tmrScheduler.shutdownNow ();
    }
  }  //tmrTini()



  //========================================================================================
  //$$PNL パネル
  //
  //  固定倍率のとき
  //    パネルの最小サイズはスクリーンのサイズに固定倍率を掛けて切り上げた値
  //    スクリーンの表示サイズはスクリーンのサイズに固定倍率を掛けて丸めた値
  //  ウインドウに合わせるとき
  //    倍率は
  //      パネルの幅をスクリーンの幅で割った結果
  //      パネルの高さからキーボードの高さを引いてスクリーンの高さで割った結果
  //    のどちらか小さい方
  //
  //  スクリーンの大きさに固定倍率を掛けて丸めた値から倍率を逆算すると固定倍率よりも小さくなってしまう場合がある
  //
  //  全画面表示
  //    キーボードと合わせてパネルにちょうど入り切るようにスクリーンの拡大率と表示位置を計算する
  //  ウインドウに合わせる
  //    可能ならばユーザがパネルの大きさを変更できるようにする
  //    パネルの大きさが最小倍率で入り切らないとき
  //      プログラムがパネルの大きさを変更できるとき
  //        パネルの大きさを最小倍率でちょうど入り切る大きさに変更する
  //      パネルの中央に表示する
  //    パネルの大きさが最小倍率で入り切るとき
  //      パネルの大きさに合わせてスクリーンを拡大縮小する
  //  固定倍率
  //    可能ならばユーザがパネルの大きさを変更できないようにする
  //    プログラムがパネルの大きさを変更できるとき
  //      パネルの大きさを固定倍率でちょうど入り切る大きさに変更する
  //    スクリーンを拡大縮小してキーボードと一緒にパネルの中央に表示する


  //ビットマップのサイズ
  public static final int PNL_BM_OFFSET_BITS = 10;
  public static final int PNL_BM_WIDTH = 1 << PNL_BM_OFFSET_BITS;
  public static final int PNL_BM_HEIGHT = 1024;

  //アスペクト比
  //  パラメータはaspectratio解像度名=画面比名
  //  解像度番号→解像度名
  //    [resolutionNumber]=resolutionName
  public static final String[] PNL_ASPECT_RESOLUTION_NAME = {
    "256x256",   //0  crtmodtest -l0 -m2
    "384x256",   //1  crtmodtest -l0 -m28
    "512x512",   //2  crtmodtest -l0 -m0
    "640x480",   //3  crtmodtest -l1 -m19
    "768x512",   //4  crtmodtest -l0 -m16
    "1024x848",  //5  crtmodtest -l0 -m18
  };
  //  解像度数
  public static final int PNL_ASPECT_RESOLUTION_COUNT = PNL_ASPECT_RESOLUTION_NAME.length;
  //  画面比番号→画面比名
  //    [ratioNumber]=screenAspectRatioName
  public static final String[] PNL_ASPECT_SCREEN_NAME = {
    "1:1",    //0  1.0
    "64:53",  //1  1.207547
    "4:3",    //2  1.333333
    "7:5",    //3  1.4
    "13:9",   //4  1.444444
    "3:2",    //5  1.5
  };
  //  画面比数
  public static final int PNL_ASPECT_RATIO_COUNT = PNL_ASPECT_SCREEN_NAME.length;
  //  解像度番号→デフォルトの画面比番号
  //    [resolutionNumber]=defaultRatioNumber
  //    Settings.SGS_DEFAULT_PARAMETERSと合わせること
  public static final int[] PNL_ASPECT_DEFAULT_RATIO = {
    2,  //0   256x256  4:3
    2,  //1   384x256  4:3
    2,  //2   512x512  4:3
    2,  //3   640x480  4:3
    5,  //4   768x512  3:2
    1,  //5  1024x848  64:53
  };
  //  画面比番号→ピクセル比名(256x256/512x512)
  //    [ratioNumber]=pixelAspectRatioName
  public static final String[] PNL_ASPECT_PIXEL_NAME_02 = {
    "1:1",    //0  1.0
    null,     //1
    "4:3",    //2  1.333333
    "7:5",    //3  1.4
    "13:9",   //4  1.444444
    "3:2",    //5  1.5
  };
  //  画面比番号→ピクセル比名(384x256/768x512)
  //    [ratioNumber]=pixelAspectRatioName
  public static final String[] PNL_ASPECT_PIXEL_NAME_14 = {
    null,       //0
    null,       //1
    "8:9",      //2  0.8888889  4/768:3/512
    "14:15",    //3  0.9333333  5/768:5/512
    "26:27",    //4  0.962963   13/768:9/512
    "1:1",      //5  1.0        3/768:2/512
  };
  //  画面比番号→ピクセル比名(640x480)
  //    [ratioNumber]=pixelAspectRatioName
  public static final String[] PNL_ASPECT_PIXEL_NAME_3 = {
    null,     //0
    null,     //1
    "1:1",    //2  1.0        4/640:3/480
    "21:20",  //3  1.05       7/640:5/480
    "13:12",  //4  1.083333   13/640:9/480
    "9:8",    //5  1.125      3/640:2/480
  };
  //  画面比番号→ピクセル比名(1024x848)
  //    [ratioNumber]=pixelAspectRatioName
  public static final String[] PNL_ASPECT_PIXEL_NAME_5 = {
    null,       //0
    "1:1",      //1  1.0        64/1024:53/848
    "53:48",    //2  1.104167   4/1024:3/848
    "371:320",  //3  1.159375   7/1024:5/848
    "689:576",  //4  1.196181   13/1024:9/848
    "159:128",  //5  1.242188   3/1024:2/848
  };
  //  画面比番号→画面比
  //    [ratioNumber]=screenAspectRatio
  public static final float[] PNL_ASPECT_SCREEN_RATIO = {
    1.0F / 1.0F,    //0  1.0
    64.0F / 53.0F,  //1  1.207547
    4.0F / 3.0F,    //2  1.333333
    7.0F / 5.0F,    //3  1.4
    13.0F / 9.0F,   //4  1.444444
    3.0F / 2.0F,    //5  1.5
  };
  //  画面比番号→ピクセル比(256x256/512x512)
  //    [ratioNumber]=pixelAspectRatio
  public static final float[] PNL_ASPECT_PIXEL_RATIO_02 = {
    1.0F / 1.0F,    //0  1.0
    0.0F,           //1
    4.0F / 3.0F,    //2  1.333333
    7.0F / 5.0F,    //3  1.4
    13.0F / 9.0F,   //4  1.444444
    3.0F / 2.0F,    //5  1.5
  };
  //  画面比番号→ピクセル比(384x256/768x512)
  //    [ratioNumber]=pixelAspectRatio
  public static final float[] PNL_ASPECT_PIXEL_RATIO_14 = {
    0.0F,             //0
    0.0F,             //1
    8.0F / 9.0F,      //2  0.8888889  4/768:3/512
    14.0F / 15.0F,    //3  0.9333333  5/768:5/512
    26.0F / 27.0F,    //4  0.962963   13/768:9/512
    1.0F / 1.0F,      //5  1.0        3/768:2/512
  };
  //  画面比番号→ピクセル比(640x480)
  //    [ratioNumber]=pixelAspectRatio
  public static final float[] PNL_ASPECT_PIXEL_RATIO_3 = {
    0.0F,           //0
    0.0F,           //1
    1.0F / 1.0F,    //2  1.0        4/640:3/480
    21.0F / 20.0F,  //3  1.05       7/640:5/480
    13.0F / 12.0F,  //4  1.083333   13/640:9/480
    9.0F / 8.0F,    //5  1.125      3/640:2/480
  };
  //  画面比番号→ピクセル比(1024x848)
  //    [ratioNumber]=pixelAspectRatio
  public static final float[] PNL_ASPECT_PIXEL_RATIO_5 = {
    0.0F,             //0
    1.0F / 1.0F,      //1  1.0        64/1024:53/848
    53.0F / 48.0F,    //2  1.104167   4/1024:3/848
    371.0F / 320.0F,  //3  1.159375   7/1024:5/848
    689.0F / 576.0F,  //4  1.196181   13/1024:9/848
    159.0F / 128.0F,  //5  1.242188   3/1024:2/848
  };
  //  解像度番号→画面比番号→ピクセル比名
  //    [resolutionNumber][ratioNumber]=pixelAspectName
  public static final String[][] PNL_ASPECT_NAME_MATRIX = {
    PNL_ASPECT_PIXEL_NAME_02,  //0   256x256
    PNL_ASPECT_PIXEL_NAME_14,  //1   384x256
    PNL_ASPECT_PIXEL_NAME_02,  //2   512x512
    PNL_ASPECT_PIXEL_NAME_3,   //3   640x480
    PNL_ASPECT_PIXEL_NAME_14,  //4   768x512
    PNL_ASPECT_PIXEL_NAME_5,   //5  1024x848
  };
  //  解像度番号→画面比番号→ピクセル比
  //    [resolutionNumber][ratioNumber]=pixelAspectRatio
  public static final float[][] PNL_ASPECT_RATIO_MATRIX = {
    PNL_ASPECT_PIXEL_RATIO_02,  //0   256x256
    PNL_ASPECT_PIXEL_RATIO_14,  //1   384x256
    PNL_ASPECT_PIXEL_RATIO_02,  //2   512x512
    PNL_ASPECT_PIXEL_RATIO_3,   //3   640x480
    PNL_ASPECT_PIXEL_RATIO_14,  //4   768x512
    PNL_ASPECT_PIXEL_RATIO_5,   //5  1024x848
  };
  //  解像度番号→画面比番号
  //    [resolutionNumber]=ratioNumber
  public static int[] pnlAspectMap;
  //  (24k,HRL,VReso,HReso)→(水平方向の拡大率,垂直方向の拡大率)
  //    [CRTC.crtHRLCurr<<4|CRTC.crtVResoCurr<<2|CRTC.crtHResoCurr]
  public static float[] pnlAspectTableX;
  public static float[] pnlAspectTableY;

  //サイズと位置
  public static final int PNL_MIN_WIDTH = 64;  //画面の最小幅
  public static final int PNL_MIN_HEIGHT = 64;  //画面の最小高さ
  public static int pnlScreenWidth;  //X68000から見た表示領域のサイズ。幅は常に8の倍数
  public static int pnlScreenHeight;
  public static float pnlStretchModeX;  //水平方向の拡大率。pnlAspectTableX[key]
  public static float pnlStretchModeY;  //垂直方向の拡大率。pnlAspectTableY[key]
  public static int pnlStretchWidth;  //ピクセルの縦横比に合わせて伸縮された表示領域の幅。Math.round((float)pnlScreenWidth*pnlStretchModeX)
  public static int pnlStretchHeight;  //ピクセルの縦横比に合わせて伸縮された表示領域の高さ。Math.round((float)pnlScreenHeight*pnlStretchModeY)
  //  回転
  public static boolean PNL_ROTATION_ON = true;
  public static int pnlRotationMode;  //反時計回り0=0度,1=90度,2=180度,3=270度
  public static AffineTransform pnlRotationTransformLeft;  //アフィン変換
  public static AffineTransform pnlRotationTransformRight;
  public static double pnlMatrixL00, pnlMatrixL10, pnlMatrixL01, pnlMatrixL11, pnlMatrixL02, pnlMatrixL12;  //変換
  public static double pnlMatrixR00, pnlMatrixR10, pnlMatrixR01, pnlMatrixR11, pnlMatrixR02, pnlMatrixR12;
  public static double pnlInverseL00, pnlInverseL10, pnlInverseL01, pnlInverseL11, pnlInverseL02, pnlInverseL12;  //逆変換
  public static double pnlInverseR00, pnlInverseR10, pnlInverseR01, pnlInverseR11, pnlInverseR02, pnlInverseR12;
  public static int pnlRotatedWidth;  //pnlStretchWidth,pnlStretchHeightを回転させた幅と高さ
  public static int pnlRotatedHeight;
  //  拡大
  public static int pnlZoomWidth;  //描画サイズ。pnlRotatedWidth,pnlRotatedHeightを同じ比率で拡大
  public static int pnlZoomHeight;
  public static int pnlZoomRatioOutX;  //65536*pnlZoomWidth/pnlScreenWidth
  public static int pnlZoomRatioOutY;  //65536*pnlZoomHeight/pnlScreenHeight
  public static int pnlZoomRatioInX;  //65536*pnlScreenWidth/pnlZoomWidth
  public static int pnlZoomRatioInY;  //65536*pnlScreenHeight/pnlZoomHeight
  public static int pnlWidth;  //パネルのサイズ
  public static int pnlHeight;
  public static Dimension pnlSize;  //パネルの推奨サイズ。pnlWidth,pnlHeight
  public static int pnlScreenX1;  //スクリーンのX座標。1=左開始,2=左終了,3=右開始,4=右終了
  public static int pnlScreenX2;
  public static int pnlScreenX3;
  public static int pnlScreenX4;
  public static int pnlScreenY1;  //スクリーンのY座標。1=上開始,2=上終了,3=下開始,4=下終了
  public static int pnlScreenY2;
  public static int pnlScreenY3;
  public static int pnlScreenY4;
  public static int pnlKeyboardX;  //キーボードの表示位置。pnlUpdateArrangement()が設定する
  public static int pnlKeyboardY;
  public static int pnlMinimumWidth;  //パネルの最小サイズ
  public static int pnlMinimumHeight;
  public static int pnlGlobalX;  //画面上の表示位置
  public static int pnlGlobalY;

  //モード
  public static final boolean PNL_FILL_BACKGROUND = true;  //true=常に背景を塗り潰してから描画する
  public static boolean pnlFillBackgroundRequest;  //true=次回のpaintで背景を塗り潰す
  public static boolean pnlIsFullScreenSupported;  //true=全画面表示に移行できる
  public static boolean pnlPrevKeyboardOn;  //true=全画面表示に移行する前はキーボードを表示していた
  public static boolean pnlHideKeyboard;  //true=全画面表示のときキーボードを隠す

  //補間アルゴリズム
  //  RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR  軽い
  //  RenderingHints.VALUE_INTERPOLATION_BILINEAR          頑張る
  //  RenderingHints.VALUE_INTERPOLATION_BICUBIC           重すぎる？
  public static Object pnlInterpolation;  //補間アルゴリズム

  //ホストのリフレッシュレートに合わせる
  public static final double PNL_MIN_RATE = 1.0;
  public static final double PNL_MAX_RATE = 1000.0;
  public static final double PNL_DEFAULT_RATE = 59.94;  //デフォルトのリフレッシュレート(Hz)。指定がなく取得もできなかったときの値
  public static double pnlRefreshRate;  //指定されたリフレッシュレート(Hz)。0.0=指定なし
  public static double pnlFixedRate;  //指定されたか取得できたかデフォルトを採用して確定したリフレッシュレート(Hz)。0.0=未確定(指定がなくて未取得)
  public static boolean pnlAdjustVsync;  //true=ホストのリフレッシュレートに合わせる

  //イメージとビットマップ
  //
  //  立体視OFFのとき
  //    左に描く
  //    左を表示する
  //
  //  立体視ONのとき
  //    垂直映像開始で
  //      0=3=左右OPENまたは2=左OPENのとき
  //        左に描く
  //      1=右OPENのとき
  //        右に描く
  //    drawRasterの後で
  //      0=3=左右OPENのとき
  //        左から右へコピー
  //    左右を表示する
  //
  //  シャッターの左右CLOSEは左右OPENとみなす
  //    TVコントロール信号の送信で$00E8E003に$08や$00が書き込まれて左右CLOSEされてしまう
  //    左右CLOSEすると何も表示されないので止まってしまったように見える
  //    接続ケーブル配線図によると~3DRだけで左OPENと右OPENを切り替えており左右CLOSEは存在しない
  //
  //  X68000－ファミコン3Dシステム接続ケーブル配線図
  //            X68000 STEREOSCOPIC端子側
  //            6pin Mini-DIN オス 半田面
  //                ┌─┐    ┌─┐
  //              ┌┘  └──┘  └┐
  //            ┌┘   GND    GND   └┐
  //            │      ○6  5○────┐
  //            │  VCC1              ││
  //          ┌───○4      3○~3DL││
  //          ││                    ││
  //          ││  ~3DR○2  1○      ││
  //          │└┐    │ VSYNC(3D)┌┘│
  //          │  └┐  │        ┌┘  │
  //          │    └─│────┘    │
  //          │        └──┐        │
  //    ┌──│───────│────│─┐
  //    │  ○│○  ○  ○  ○│○  ○  ○  │
  //    │   8│ 7   6   5   4│ 3   2   1  │
  //    └┐  │              │          ┌┘
  //      │  ○  ○  ○  ○  ○  ○  ○  │
  //      │  15  14  13  12  11  10   9  │
  //      └───────────────┘
  //           D-Sub 15pin オス 半田面
  //           ファミコン3Dシステム側
  //      出典:電脳倶楽部17号(1989年10月号)
  //
  public static final boolean PNL_STEREOSCOPIC_ON = true;
  //!!! XEiJ.PNL_USE_THREADとCRTC.CRT_ENABLE_INTERMITTENTを同時にtrueにしないこと
  public static final boolean PNL_USE_THREAD = true;  //true=パネルの表示にスレッドを使う
  //if PNL_USE_THREAD
  public static BufferedImage[] pnlScreenImageLeftArray;  //イメージ左
  public static BufferedImage[] pnlScreenImageRightArray;  //イメージ右
  //  if PNL_ROTATION_ON
  public static BufferedImage[] pnlScreenSubImageLeftArray;  //イメージ左の表示範囲のサブイメージ
  public static BufferedImage[] pnlScreenSubImageRightArray;  //イメージ右の表示範囲のサブイメージ
  //  endif
  public static int[][] pnlBMLeftArray;  //ビットマップ左
  public static int[][] pnlBMRightArray;  //ビットマップ右
  public static volatile int pnlBMWrite;  //書き込むビットマップ番号。コアスレッドが更新する
  public static volatile int pnlBMRead;  //読み出すビットマップ番号。描画スレッドが更新する
  //else
  public static BufferedImage pnlScreenImageLeft;  //イメージ左
  public static BufferedImage pnlScreenImageRight;  //イメージ右
  //  if PNL_ROTATION_ON
  public static BufferedImage pnlScreenSubImageLeft;  //イメージ左の表示範囲のサブイメージ
  public static BufferedImage pnlScreenSubImageRight;  //イメージ右の表示範囲のサブイメージ
  //  endif
  public static int[] pnlBMLeft;  //ビットマップ左
  public static int[] pnlBMRight;  //ビットマップ右
  //endif
  public static int[] pnlBM;  //描画するビットマップ
  public static boolean pnlStereoscopicOn;  //true=立体視
  public static final int PNL_NAKED_EYE_CROSSING = 0;  //裸眼交差法
  public static final int PNL_NAKED_EYE_PARALLEL = 1;  //裸眼平行法
  public static final int PNL_SIDE_BY_SIDE       = 2;  //サイドバイサイド
  public static final int PNL_TOP_AND_BOTTOM     = 3;  //トップアンドボトム
  public static int pnlStereoscopicMethod;  //0=裸眼交差法,1=裸眼平行法,2=サイドバイサイド,3=トップアンドボトム
  public static int pnlStereoscopicFactor;  //pnlStereoscopicOn&&(pnlStereoscopicMethod==PNL_NAKED_EYE_CROSSING||pnlStereoscopicMethod==PNL_NAKED_EYE_PARALLEL)?2:1
  public static int pnlStereoscopicShutter;  //$00E8E003のbit1-0。0=3=左右OPEN,1=右OPEN,2=左OPEN

  //パネル
  public static JPanel pnlPanel;  //パネル
  //if PNL_USE_THREAD
  public static Thread pnlThread;  //スレッド
  public static boolean pnlInterrupted;  //pnlThread.isInterrupted()の代わり
  public static long pnlWakeupTime;  //スレッドを起こす時刻(ミリ)
  public static long pnlWakeupTimeMNP;  //スレッドを起こす時刻(マイクロナノピコ)
  public static final boolean PNL_USE_CANVAS = PNL_USE_THREAD && true;  //true=パネルの表示にキャンバスを使う
  //  if PNL_USE_CANVAS
  public static boolean pnlUseCanvasRequest;
  public static boolean pnlUseCanvas;  //true=パネルの表示にキャンバスを使う
  public static Canvas pnlCanvas;  //キャンバス
  public static Component pnlCanvasOrPanel;  //PNL_USE_CANVAS&&pnlUseCanvas?pnlCanvas:pnlPanel
  //  endif
  //endif

  //メニュー
  public static int pnlFixedScale;
  public static SpinnerNumberModel pnlFixedModel;
  public static JSpinner pnlFixedSpinner;

  //pnlInit ()
  //  パネルのフィールドを初期化する
  public static void pnlInit () {
    pnlInit2 ();

    //設定

    //  固定倍率
    pnlFixedScale = Math.max (10, Math.min (1000, Settings.sgsGetInt ("fixedscale")));

    //  アスペクト比
    pnlAspectMap = new int[PNL_ASPECT_RESOLUTION_COUNT];
    for (int resolutionNumber = 0; resolutionNumber < PNL_ASPECT_RESOLUTION_COUNT; resolutionNumber++) {
      String resolutionName = PNL_ASPECT_RESOLUTION_NAME[resolutionNumber];
      String screenName = Settings.sgsGetString ("aspectratio" + resolutionName);
      int ratioNumber = PNL_ASPECT_DEFAULT_RATIO[resolutionNumber];
      for (int tempRatioNumber = 0; tempRatioNumber < PNL_ASPECT_RATIO_COUNT; tempRatioNumber++) {
        if (PNL_ASPECT_RATIO_MATRIX[resolutionNumber][tempRatioNumber] == 0.0F) {
          continue;
        }
        if (PNL_ASPECT_SCREEN_NAME[tempRatioNumber].equals (screenName)) {
          ratioNumber = tempRatioNumber;
          break;
        }
      }
      pnlAspectMap[resolutionNumber] = ratioNumber;
    }
    pnlAspectTableX = new float[64];
    pnlAspectTableY = new float[64];
    pnlUpdateAspectTable ();

    //  補間アルゴリズム
    switch (Settings.sgsGetString ("interpolation").toLowerCase ()) {
    case "nearest":  //最近傍補間
      pnlInterpolation = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
      break;
    case "bilinear":  //線形補間
      pnlInterpolation = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
      break;
    case "bicubic":  //三次補間
      pnlInterpolation = RenderingHints.VALUE_INTERPOLATION_BICUBIC;
      break;
    default:
      pnlInterpolation = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
    }

    //  ホストのリフレッシュレートに合わせる
    pnlRefreshRate = 0.0;
    {
      String s = Settings.sgsGetString ("refreshrate");
      if (!s.equals ("")) {
        try {
          double rate = Double.parseDouble (s);
          if (PNL_MIN_RATE <= rate && rate <= PNL_MAX_RATE) {
            pnlRefreshRate = rate;
          }
        } catch (NumberFormatException nfe) {
        }
      }
    }
    pnlFixedRate = pnlRefreshRate;
    pnlAdjustVsync = Settings.sgsGetOnOff ("adjustvsync");

    pnlPrevKeyboardOn = true;

    pnlHideKeyboard = Settings.sgsGetOnOff ("hidekeyboard");

    //イメージとビットマップ
    if (PNL_USE_THREAD) {
      pnlScreenImageLeftArray = new BufferedImage[4];
      pnlScreenImageRightArray = new BufferedImage[4];
      if (PNL_ROTATION_ON) {
        pnlScreenSubImageLeftArray = new BufferedImage[4];
        pnlScreenSubImageRightArray = new BufferedImage[4];
      }
      pnlBMLeftArray = new int[4][];
      pnlBMRightArray = new int[4][];
      for (int n = 0; n < 4; n++) {
        pnlScreenImageLeftArray[n] = new BufferedImage (PNL_BM_WIDTH, PNL_BM_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        pnlScreenImageRightArray[n] = new BufferedImage (PNL_BM_WIDTH, PNL_BM_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        if (PNL_ROTATION_ON) {
          pnlScreenSubImageLeftArray[n] = null;
          pnlScreenSubImageRightArray[n] = null;
        }
        pnlBMLeftArray[n] = ((DataBufferInt) pnlScreenImageLeftArray[n].getRaster ().getDataBuffer ()).getData ();
        pnlBMRightArray[n] = ((DataBufferInt) pnlScreenImageRightArray[n].getRaster ().getDataBuffer ()).getData ();
      }
      pnlBMWrite = 0;
      pnlBM = pnlBMLeftArray[pnlBMWrite & 3];  //左に描く
      pnlBMRead = 0;
      pnlThread = null;
      pnlInterrupted = false;
      pnlWakeupTime = 0L;
      pnlWakeupTimeMNP = 0L;
      if (PNL_USE_CANVAS) {
        pnlUseCanvasRequest = Settings.sgsGetOnOff ("usecanvas");
        pnlUseCanvas = pnlUseCanvasRequest;
        pnlCanvas = null;
      }
    } else {
      pnlScreenImageLeft = new BufferedImage (PNL_BM_WIDTH, PNL_BM_HEIGHT, BufferedImage.TYPE_INT_ARGB);
      pnlScreenImageRight = new BufferedImage (PNL_BM_WIDTH, PNL_BM_HEIGHT, BufferedImage.TYPE_INT_ARGB);
      if (PNL_ROTATION_ON) {
        pnlScreenSubImageLeft = null;
        pnlScreenSubImageRight = null;
      }
      pnlBMLeft = ((DataBufferInt) pnlScreenImageLeft.getRaster ().getDataBuffer ()).getData ();
      pnlBMRight = ((DataBufferInt) pnlScreenImageRight.getRaster ().getDataBuffer ()).getData ();
      pnlBM = pnlBMLeft;  //左に描く
    }
    pnlStereoscopicOn = Settings.sgsGetOnOff ("stereoscopic");
    switch (Settings.sgsGetString ("stereoscopicmethod").toLowerCase ()) {
    case "nakedeyecrossing":
      pnlStereoscopicMethod = PNL_NAKED_EYE_CROSSING;
      break;
    case "nakedeyeparallel":
      pnlStereoscopicMethod = PNL_NAKED_EYE_PARALLEL;
      break;
    case "sidebyside":
      pnlStereoscopicMethod = PNL_SIDE_BY_SIDE;
      break;
    case "topandbottom":
      pnlStereoscopicMethod = PNL_TOP_AND_BOTTOM;
      break;
    default:
      pnlStereoscopicMethod = PNL_NAKED_EYE_CROSSING;
    }
    pnlStereoscopicFactor = pnlStereoscopicOn && (pnlStereoscopicMethod == PNL_NAKED_EYE_CROSSING ||
                                                  pnlStereoscopicMethod == PNL_NAKED_EYE_PARALLEL) ? 2 : 1;
    pnlStereoscopicShutter = 0;  //左右OPEN

    //サイズと位置
    pnlScreenWidth = 768;
    pnlScreenHeight = 512;
    pnlStretchModeX = 1.0F;
    pnlStretchModeY = 1.0F;
    pnlStretchWidth = Math.round ((float) pnlScreenWidth * pnlStretchModeX);
    pnlStretchHeight = Math.round ((float) pnlScreenHeight * pnlStretchModeY);
    //  回転
    if (PNL_ROTATION_ON) {
      pnlRotationMode = Settings.sgsGetInt ("rotation", 0);
      if (pnlRotationMode < 0 || 3 < pnlRotationMode) {
        pnlRotationMode = 0;
      }
      pnlRotationTransformLeft = new AffineTransform ();
      pnlRotationTransformRight = new AffineTransform ();
    }
    pnlRotatedWidth = pnlStretchWidth;
    pnlRotatedHeight = pnlStretchHeight;
    if (PNL_ROTATION_ON && ((pnlRotationMode & 1) != 0)) {  //90度または270度のとき
      pnlRotatedWidth = pnlStretchHeight;  //幅と高さを入れ替える
      pnlRotatedHeight = pnlStretchWidth;
    }
    //  拡大
    pnlZoomWidth = pnlRotatedWidth;
    pnlZoomHeight = pnlRotatedHeight;
    pnlWidth = Math.max (pnlZoomWidth * pnlStereoscopicFactor, Keyboard.kbdWidth);
    pnlHeight = pnlZoomHeight + Keyboard.kbdHeight;
    pnlSize = new Dimension (pnlWidth, pnlHeight);
    pnlScreenX1 = (pnlWidth - pnlZoomWidth * pnlStereoscopicFactor) >> 1;
    pnlScreenY1 = 0;
    pnlArrangementCommon ();
    pnlMinimumWidth = Math.max (PNL_MIN_WIDTH, Keyboard.kbdWidth);
    pnlMinimumHeight = PNL_MIN_HEIGHT + Keyboard.kbdHeight;
    pnlGlobalX = 0;
    pnlGlobalY = 0;

    //モード
    if (!PNL_FILL_BACKGROUND) {
      pnlFillBackgroundRequest = true;
    }

    //メニュー
    pnlFixedModel = new SpinnerNumberModel (pnlFixedScale, 10, 1000, 1);
    pnlFixedSpinner = ComponentFactory.createNumberSpinner (pnlFixedModel, 4, new ChangeListener () {
      @Override public void stateChanged (ChangeEvent ce) {
        if (pnlMode != PNL_FIXEDSCALE) {  //固定倍率になっていない
          pnlSetMode (PNL_FIXEDSCALE);  //固定倍率に切り替える
        } else {  //固定倍率になっている
          pnlUpdateArrangement ();  //再配置のみ
        }
      }
    });

    if (PNL_ROUNDED_CORNER_ON) {
      pnlRoundedCornerModel = new SpinnerNumberModel (pnlRoundedCornerRatio, 0, 50, 1);
      pnlRoundedCornerSpinner = ComponentFactory.createNumberSpinner (pnlRoundedCornerModel, 2, new ChangeListener () {
        @Override public void stateChanged (ChangeEvent ce) {
          pnlRoundedCornerRatio = pnlRoundedCornerModel.getNumber ().intValue ();
          if (PNL_ROUNDED_CORNER_TEST || pnlMode == PNL_FULLSCREEN) {
            pnlUpdateArrangement ();
          }
        }
      });
    }

  }  //pnlInit()

  //rate = pnlGetRefreshRate ()
  //  ホストのリフレッシュレートを取得する
  public static double pnlGetRefreshRate () {
    double rate = 0.0;
    GraphicsConfiguration gc = frmFrame.getGraphicsConfiguration ();
    if (gc != null) {
      GraphicsDevice gd = gc.getDevice ();
      DisplayMode dm = gd.getDisplayMode ();
      int i = dm.getRefreshRate ();  //リフレッシュレート
      if (i != DisplayMode.REFRESH_RATE_UNKNOWN) {
        rate = (i == 23 ? 23.98 :
                i == 29 ? 29.97 :
                i == 59 ? 59.94 :
                i == 119 ? 119.88 :
                i == 239 ? 239.76 :
                (double) i);
        if (rate < PNL_MIN_RATE || PNL_MAX_RATE < rate) {  //念の為
          rate = 0.0;
        }
      }
    }
    if (rate == 0.0) {
      rate = PNL_DEFAULT_RATE;
      System.out.printf (Multilingual.mlnJapanese ?
                         "ホストのリフレッシュレートを取得できません。デフォルトの %.2f Hz を使います\n" :
                         "Cannot get host refresh rate. Use default %.2f Hz\n", rate);
    } else {
      System.out.printf (Multilingual.mlnJapanese ?
                         "ホストのリフレッシュレートは %.2f Hz です\n" :
                         "Host refresh rate is %.2f Hz\n", rate);
    }
    return rate;
  }  //nlGetRefreshRate

  //  立体視をon/offする
  public static void pnlSetStereoscopic (boolean on, int method) {
    if (pnlStereoscopicOn != on || pnlStereoscopicMethod != method) {
      pnlStereoscopicMethod = method;
      pnlStereoscopicFactor = on && (pnlStereoscopicMethod == PNL_NAKED_EYE_CROSSING ||
                                     pnlStereoscopicMethod == PNL_NAKED_EYE_PARALLEL) ? 2 : 1;
      if (!pnlStereoscopicOn && on) {  //off→on
        if (PNL_USE_THREAD) {
          for (int n = 0; n < 4; n++) {
            System.arraycopy (pnlBMLeftArray[n], 0, pnlBMRightArray[n], 0, 1024 * 1024);  //左から右へコピー
          }
        } else {
          System.arraycopy (pnlBMLeft, 0, pnlBMRight, 0, 1024 * 1024);  //左から右へコピー
        }
      } else if (pnlStereoscopicOn && !on) {  //on→off
        if (PNL_USE_THREAD) {
          pnlBM = pnlBMLeftArray[pnlBMWrite & 3];  //左に描く
        } else {
          pnlBM = pnlBMLeft;  //左に描く
        }
      }
      pnlStereoscopicOn = on;
      pnlUpdateArrangement ();
    }
  }

  //  後始末
  public static void pnlTini () {
    pnlTini2 ();
    if (PNL_USE_THREAD) {
      if (pnlThread != null) {
        pnlInterrupted = true;
        try {
          pnlThread.join (100L);
        } catch (InterruptedException ie) {
        }
        pnlThread = null;
      }
    }
    //設定
    //  固定倍率
    Settings.sgsPutInt ("fixedscale", pnlFixedScale);

    //  アスペクト比
    for (int resolutionNumber = 0; resolutionNumber < PNL_ASPECT_RESOLUTION_COUNT; resolutionNumber++) {
      String resolutionName = PNL_ASPECT_RESOLUTION_NAME[resolutionNumber];
      int ratioNumber = pnlAspectMap[resolutionNumber];
      String screenName = PNL_ASPECT_SCREEN_NAME[ratioNumber];
      Settings.sgsPutString ("aspectratio" + resolutionName, screenName);
    }

    //  補間アルゴリズム
    Settings.sgsPutString ("interpolation",
                           pnlInterpolation == RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR ? "nearest" :
                           pnlInterpolation == RenderingHints.VALUE_INTERPOLATION_BILINEAR ? "bilinear" :
                           pnlInterpolation == RenderingHints.VALUE_INTERPOLATION_BICUBIC ? "bicubic" :
                           "bilinear");
    //  ホストのリフレッシュレートに合わせる
    if (pnlRefreshRate != PNL_DEFAULT_RATE) {
      Settings.sgsPutString ("refreshrate",
                             pnlRefreshRate == 0.0 ? "" : String.valueOf (pnlRefreshRate));
    }
    Settings.sgsPutOnOff ("adjustvsync", pnlAdjustVsync);

    Settings.sgsPutOnOff ("hidekeyboard", pnlHideKeyboard);

    if (PNL_USE_CANVAS) {
      Settings.sgsPutOnOff ("usecanvas", pnlUseCanvasRequest);
    }

    //  立体視
    Settings.sgsPutOnOff ("stereoscopic", pnlStereoscopicOn);
    Settings.sgsPutString ("stereoscopicmethod",
                           pnlStereoscopicMethod == PNL_NAKED_EYE_CROSSING ? "nakedeyecrossing" :
                           pnlStereoscopicMethod == PNL_NAKED_EYE_PARALLEL ? "nakedeyeparallel" :
                           pnlStereoscopicMethod == PNL_SIDE_BY_SIDE ? "sidebyside" :
                           pnlStereoscopicMethod == PNL_TOP_AND_BOTTOM ? "topandbottom" :
                           "nakedeyecrossing");

    //  回転
    if (PNL_ROTATION_ON) {
      Settings.sgsPutInt ("rotation", pnlRotationMode);
    }

  }  //pnlTini

  //pnlUpdateAspectTable ()
  //  ピクセルアスペクト比テーブルを更新する
  public static void pnlUpdateAspectTable () {
    float[] ratio = new float[PNL_ASPECT_RESOLUTION_COUNT];  //[resolutionNumber]=pixelAspectRatio
    for (int resolutionNumber = 0; resolutionNumber < PNL_ASPECT_RESOLUTION_COUNT; resolutionNumber++) {
      int ratioNumber = pnlAspectMap[resolutionNumber];
      ratio[resolutionNumber] = PNL_ASPECT_RATIO_MATRIX[resolutionNumber][ratioNumber];
    }
    Arrays.fill (pnlAspectTableY, 1.0F);
    //              HRL V  H
    pnlAspectTableX[0b0_00_00] = ratio[0] * 2.0F;  //69/6 256x256
    pnlAspectTableX[0b0_00_01] = ratio[2];         //69/3 512x256
    pnlAspectTableX[0b0_00_10] = ratio[4];         //69/2 768x256
    pnlAspectTableX[0b0_00_11] = ratio[3];         //50/2 640x240
    pnlAspectTableX[0b0_01_00] = ratio[0] * 2.0F;  //69/6 256x512
    pnlAspectTableX[0b0_01_01] = ratio[2];         //69/3 512x512
    pnlAspectTableX[0b0_01_10] = ratio[4];         //69/2 768x512
    pnlAspectTableX[0b0_01_11] = ratio[3];         //50/2 640x480
    pnlAspectTableX[0b0_10_00] = ratio[0] * 4.0F;  //69/6 256x1024
    pnlAspectTableX[0b0_10_01] = ratio[2] * 2.0F;  //69/3 512x1024
    pnlAspectTableX[0b0_10_10] = ratio[4] * 2.0F;  //69/2 768x1024
    pnlAspectTableX[0b0_10_11] = ratio[3] * 2.0F;  //50/2 640x960
    pnlAspectTableX[0b0_11_00] = ratio[0] * 4.0F;
    pnlAspectTableX[0b0_11_01] = ratio[2] * 2.0F;
    pnlAspectTableX[0b0_11_10] = ratio[4] * 2.0F;
    pnlAspectTableX[0b0_11_11] = ratio[3] * 2.0F;
    pnlAspectTableX[0b1_00_00] = ratio[0] * 2.0F;  //69/6 256x256
    pnlAspectTableX[0b1_00_01] = ratio[1] * 2.0F;  //69/4 384x256
    pnlAspectTableX[0b1_00_10] = ratio[4];         //69/2 768x256
    pnlAspectTableX[0b1_00_11] = ratio[3];         //50/2 640x240
    pnlAspectTableX[0b1_01_00] = ratio[0] * 2.0F;  //69/6 256x512
    pnlAspectTableX[0b1_01_01] = ratio[1] * 2.0F;  //69/4 384x512
    pnlAspectTableX[0b1_01_10] = ratio[4];         //69/2 768x512
    pnlAspectTableX[0b1_01_11] = ratio[3];         //50/2 640x480
    pnlAspectTableX[0b1_10_00] = ratio[0] * 4.0F;  //69/6 256x1024
    pnlAspectTableX[0b1_10_01] = ratio[1] * 2.0F;  //69/4 384x1024
    pnlAspectTableX[0b1_10_10] = ratio[4] * 2.0F;  //69/2 768x1024
    pnlAspectTableX[0b1_10_11] = ratio[3] * 2.0F;  //50/2 640x960
    pnlAspectTableX[0b1_11_00] = ratio[0] * 4.0F;
    pnlAspectTableX[0b1_11_01] = ratio[1] * 2.0F;
    pnlAspectTableX[0b1_11_10] = ratio[4] * 2.0F;
    pnlAspectTableX[0b1_11_11] = ratio[3] * 2.0F;
    //24k
    for (int i = 0; i < 32; i++) {
      pnlAspectTableX[32 + i] = pnlAspectTableX[i];
    }
    //                   HRL V  H
    pnlAspectTableX[32 + 0b0_00_10] = ratio[5];         //69/2 1024x212
    pnlAspectTableY[32 + 0b0_00_10] = 2.0F;
    pnlAspectTableX[32 + 0b0_01_10] = ratio[5];         //69/2 1024x424
    pnlAspectTableY[32 + 0b0_01_10] = 2.0F;
    pnlAspectTableX[32 + 0b0_10_10] = ratio[5];         //69/2 1024x848
    pnlAspectTableX[32 + 0b0_11_10] = ratio[5];
    pnlAspectTableX[32 + 0b1_00_01] = ratio[5];         //69/4 512x212
    pnlAspectTableX[32 + 0b1_01_01] = ratio[5];         //69/4 512x424
    pnlAspectTableX[32 + 0b1_10_01] = ratio[5] * 2.0F;  //69/4 512x848
    pnlAspectTableX[32 + 0b1_11_01] = ratio[5] * 2.0F;
  }  //pnlUpdateAspectTable

  //pnlMake ()
  //  パネルを作る
  public static void pnlMake () {
    pnlMake2 ();

    //パネル
    if (PNL_USE_CANVAS && pnlUseCanvas) {
      pnlCanvas = new Canvas ();
      pnlPanel = new JPanel (new BorderLayout (0, 0));
      pnlPanel.add (pnlCanvas, BorderLayout.CENTER);
      pnlCanvasOrPanel = pnlCanvas;
    } else {
      pnlPanel = new JPanel () {
        @Override protected void paintComponent (Graphics g) {
          pnlPaintCommon (g);
        }
        @Override protected void paintBorder (Graphics g) {
        }
        @Override protected void paintChildren (Graphics g) {
        }
      };
      pnlCanvasOrPanel = pnlPanel;
    }
    pnlPanel.setBackground (Color.black);
    pnlPanel.setOpaque (true);
    pnlPanel.setPreferredSize (pnlSize);
    //矢印カーソルを表示する
    if (Mouse.musCursorAvailable) {
      pnlPanel.setCursor (Mouse.musCursorArray[1]);  //pnlCanvasOrPanelは不可
    }

  }  //pnlMake()

  //pnlPaintCommon (g)
  //  描画共通
  public static void pnlPaintCommon (Graphics g) {
    Graphics2D g2 = (Graphics2D) g;
    if (PNL_FILL_BACKGROUND || pnlFillBackgroundRequest) {  //背景を塗り潰す
      if (!PNL_FILL_BACKGROUND) {
        pnlFillBackgroundRequest = false;
      }
      g2.setRenderingHint (RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
      g2.setColor (Color.black);
      g2.fillRect (0, 0, pnlWidth, pnlHeight);
    }
    g2.setRenderingHint (RenderingHints.KEY_INTERPOLATION, pnlInterpolation);
    boolean compositeSet = false;
    if (FlashingLights.FLR_ON &&
        FlashingLights.flrScreen) {
      float brightness = FlashingLights.flrBrightness[pnlBMRead & 3];
      if (brightness < 1.0F) {
        g2.setComposite (
          AlphaComposite.getInstance (AlphaComposite.SRC_OVER, brightness));
        compositeSet = true;
      }
    }
    if (PNL_USE_THREAD) {
      int d = pnlBMWrite - pnlBMRead;
      if (false) {
        System.out.print (d);
      }
      if (d < 1) {  //足りない
        pnlBMRead += d - 1;  //繰り返す
      } else if (3 < d) {  //多すぎる
        pnlBMRead += d - 3;  //読み飛ばす
      }
      int n = pnlBMRead++ & 3;
      if (PNL_STEREOSCOPIC_ON && pnlStereoscopicOn) {  //立体視ON
        BufferedImage leftImage;
        BufferedImage rightImage;
        if (pnlStereoscopicMethod == PNL_NAKED_EYE_CROSSING) {
          leftImage = pnlScreenImageRightArray[n];
          rightImage = pnlScreenImageLeftArray[n];
        } else {
          //pnlStereoscopicMethod == PNL_NAKED_EYE_PARALLEL
          //pnlStereoscopicMethod == PNL_SIDE_BY_SIDE
          //pnlStereoscopicMethod == PNL_TOP_AND_BOTTOM
          leftImage = pnlScreenImageLeftArray[n];
          rightImage = pnlScreenImageRightArray[n];
        }
        if (PNL_ROTATION_ON && pnlRotationMode != 0) {
          g2.drawImage (leftImage, pnlRotationTransformLeft, null);
          g2.drawImage (rightImage, pnlRotationTransformRight, null);
        } else {
          g2.drawImage (leftImage,
                        pnlScreenX1, pnlScreenY1,
                        pnlScreenX2, pnlScreenY2,
                        0, 0, pnlScreenWidth, pnlScreenHeight,
                        null);
          g2.drawImage (rightImage,
                        pnlScreenX3, pnlScreenY3,
                        pnlScreenX4, pnlScreenY4,
                        0, 0, pnlScreenWidth, pnlScreenHeight,
                        null);
        }
      } else {  //立体視OFF
        if (PNL_ROTATION_ON && pnlRotationMode != 0) {
          g2.drawImage (pnlScreenSubImageLeftArray[n], pnlRotationTransformLeft, null);
        } else {
          g2.drawImage (pnlScreenImageLeftArray[n],
                        pnlScreenX1, pnlScreenY1,
                        pnlScreenX2, pnlScreenY2,
                        0, 0, pnlScreenWidth, pnlScreenHeight,
                        null);
        }
      }
    } else {  //!PNL_USE_THREAD
      if (PNL_STEREOSCOPIC_ON && pnlStereoscopicOn) {  //立体視ON
        BufferedImage leftImage;
        BufferedImage rightImage;
        if (pnlStereoscopicMethod == PNL_NAKED_EYE_CROSSING) {
          leftImage = pnlScreenImageRight;
          rightImage = pnlScreenImageLeft;
        } else {
          //pnlStereoscopicMethod == PNL_NAKED_EYE_PARALLEL
          //pnlStereoscopicMethod == PNL_SIDE_BY_SIDE
          //pnlStereoscopicMethod == PNL_TOP_AND_BOTTOM
          leftImage = pnlScreenImageLeft;
          rightImage = pnlScreenImageRight;
        }
        if (PNL_ROTATION_ON && pnlRotationMode != 0) {
          g2.drawImage (leftImage, pnlRotationTransformLeft, null);
          g2.drawImage (rightImage, pnlRotationTransformRight, null);
        } else {
          g2.drawImage (leftImage,
                        pnlScreenX1, pnlScreenY1,
                        pnlScreenX2, pnlScreenY2,
                        0, 0, pnlScreenWidth, pnlScreenHeight,
                        null);
          g2.drawImage (rightImage,
                        pnlScreenX3, pnlScreenY3,
                        pnlScreenX4, pnlScreenY4,
                        0, 0, pnlScreenWidth, pnlScreenHeight,
                        null);
        }
      } else {  //立体視OFF
        if (PNL_ROTATION_ON && pnlRotationMode != 0) {
          g2.drawImage (pnlScreenImageLeft, pnlRotationTransformLeft, null);
        } else {
          g2.drawImage (pnlScreenImageLeft,
                        pnlScreenX1, pnlScreenY1,
                        pnlScreenX2, pnlScreenY2,
                        0, 0, pnlScreenWidth, pnlScreenHeight,
                        null);
        }
      }
    }
    if (compositeSet) {
      g2.setComposite (
        AlphaComposite.getInstance (
          AlphaComposite.SRC_OVER,
          1.0F));
    }
    g2.setRenderingHint (RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    g2.drawImage (Keyboard.kbdImage, pnlKeyboardX, pnlKeyboardY, null);  //Graphics.drawImage()はimage==nullのとき何もしない
    //
    if (TextCopy.txcEncloseEachTime && 0 <= TextCopy.txcRow1) {
      int x = TextCopy.txcCol1 << 3;
      int w = (TextCopy.txcCol2 - TextCopy.txcCol1 + 1) << 3;
      int y = TextCopy.txcRow1 << 4;
      int h = (TextCopy.txcRow2 - TextCopy.txcRow1 + 1) << 4;
      x -= CRTC.crtR10TxXCurr;
      y -= CRTC.crtR11TxYCurr;
      g2.setColor (Color.red);
      if (PNL_ROTATION_ON) {
        AffineTransform savedTransform = g2.getTransform ();
        g2.transform (pnlRotationTransformLeft);
        g2.drawRect (x, y, w, h);
        g2.setTransform (savedTransform);
      } else {
        g2.drawRect (pnlScreenX1 + ((x * pnlZoomRatioOutX) >> 16),
                     pnlScreenY1 + ((y * pnlZoomRatioOutY) >> 16),
                     ((w * pnlZoomRatioOutX) >> 16) - 1,
                     ((h * pnlZoomRatioOutY) >> 16) - 1);
      }
    }
    if (Bubble.BBL_ON) {
      Bubble.bblDraw (g2);
    }
  }  //pnlPaintCommon

  //pnlStart ()
  //  パネルの動作を開始する
  //  イベントリスナーを設定する
  public static void pnlStart () {
    pnlStart2 ();

    //コンポーネントリスナー
    ComponentFactory.addListener (
      pnlPanel,
      new ComponentAdapter () {
        @Override public void componentResized (ComponentEvent ce) {
          pnlUpdateArrangement ();
        }
      });

    if (PNL_USE_THREAD) {
      if (PNL_USE_CANVAS && pnlUseCanvas) {
        pnlCanvas.createBufferStrategy (2);
        pnlThread = new Thread () {
          @Override public void run () {
            do {
              BufferStrategy bs = pnlCanvas.getBufferStrategy ();
              if (bs != null) {
                Graphics g = bs.getDrawGraphics ();
                pnlPaintCommon (g);
                g.dispose ();
                bs.show ();
              }
            } while (!pnlInterrupted && pnlWakeupCommon ());
          }
        };
      } else {
        pnlThread = new Thread () {
          @Override public void run () {
            do {
              pnlPanel.repaint ();
            } while (!pnlInterrupted && pnlWakeupCommon ());
          }
        };
      }
      pnlWakeupTime = System.currentTimeMillis ();
      pnlWakeupTimeMNP = 0L;
      pnlThread.start ();
    }

  }  //pnlStart()

  public static boolean pnlWakeupCommon () {
    long t = System.currentTimeMillis ();
    if (CRTC.crtTotalLength == 0L) {  //未確定
      pnlWakeupTime += 40L;
    } else {
      pnlWakeupTime += CRTC.crtTotalLength;
      pnlWakeupTimeMNP += CRTC.crtTotalLengthMNP;
      if (1000000000L <= pnlWakeupTimeMNP) {
        pnlWakeupTime++;
        pnlWakeupTimeMNP -= 1000000000L;
      }
    }
    pnlWakeupTime = Math.max (pnlWakeupTime, t + 4L);
    try {
      Thread.sleep (pnlWakeupTime - t);
    } catch (InterruptedException ie) {
      return false;
    }
    return true;
  }

  //pnlExitFullScreen (dialog)
  //  ダイアログやサブウインドウを開く前に全画面表示を終了する
  //  macOSのとき全画面表示のままモーダルダイアログを開くと操作できなくなる
  //  Windowsでも全画面表示のままサブウインドウを開くとサブウインドウが見えないので全画面表示を終了しなければならなくなる
  public static void pnlExitFullScreen (boolean dialog) {
    if (prgIsMac || !dialog) {
      pnlSetFullScreenOn (false);
    }
  }

  //pnlToggleFullScreen ()
  //  全画面表示の切り替え
  public static void pnlToggleFullScreen () {
    if (pnlMode == PNL_FIXEDSCALE || pnlMode == PNL_FITINWINDOW) {
      pnlSetMode (PNL_FULLSCREEN);
    } else {
      pnlSetMode (pnlPrevMode);  //全画面表示だけでなく最大化も終了させる
    }
  }  //pnlToggleFullScreen

  //pnlToggleMaximized ()
  //  最大化の切り替え
  public static void pnlToggleMaximized () {
    if (pnlMode == PNL_FIXEDSCALE || pnlMode == PNL_FITINWINDOW) {
      pnlSetMode (PNL_MAXIMIZED);
    } else {
      pnlSetMode (pnlPrevMode);  //最大化だけでなく全画面表示も終了させる
    }
  }  //pnlToggleMaximized

  //pnlSetFullScreenOn (on)
  //  全画面表示を設定する
  public static void pnlSetFullScreenOn (boolean on) {
    if (on) {
      pnlSetMode (PNL_FULLSCREEN);
    } else if (pnlMode == PNL_FULLSCREEN) {
      pnlSetMode (pnlPrevMode);
    }
  }  //pnlSetFullScreenOn

  //pnlSetFitInWindowOn (on)
  //  ウインドウに合わせるモードを設定する
  //  ウインドウに合わせるモードには全画面表示が含まれる
  public static void pnlSetFitInWindowOn (boolean on) {
    if (!on) {
      pnlSetMode (PNL_FIXEDSCALE);
    } else if (pnlMode == PNL_FIXEDSCALE) {
      pnlSetMode (PNL_FITINWINDOW);
    }
  }  //pnlSetFitInWindowOn

  //pnlUpdateArrangement ()
  //  スクリーンとキーボードの配置を再計算する
  //    リサイズ、最大化、全画面表示などの操作でパネルの大きさが変わったとき
  //    ウインドウに合わせるかどうかが変わったとき
  //    ウインドウに合わせないが固定倍率が変わったとき
  //    キーボードの有無または種類が変わったとき
  //    X68000の画面モードが変更されてスクリーンの大きさが変わったとき
  //    立体視のon/offが変わったとき
  //    アスペクト比が変わったとき
  public static void pnlUpdateArrangement () {
    pnlWidth = pnlPanel.getWidth ();
    pnlHeight = pnlPanel.getHeight ();
    frmMarginWidth = frmFrame.getWidth () - pnlWidth;
    frmMarginHeight = frmFrame.getHeight () - pnlHeight;
    int stretchIndex = CRTC.crtHRLCurr << 4 | CRTC.crtVResoCurr << 2 | CRTC.crtHResoCurr;
    {
      int htotal = CRTC.crtR00HFrontEndCurr + 1;
      int vtotal = CRTC.crtR04VFrontEndCurr + 1;
      if (0 < htotal && 0 < vtotal) {  //0除算を回避する
        int k = CRTC.crtHRLCurr << 3 | CRTC.crtHighResoCurr << 2 | CRTC.crtHResoCurr;
        double osc = (double) CRTC.crtFreqs[CRTC.CRT_OSCS[k]] * CRTC.crtVsyncMultiplier;
        int ratio = CRTC.CRT_DIVS[k];
        double hfreq = osc / (ratio * htotal << 3);
        if (24000.0 <= hfreq && hfreq < 25000.0) {  //24k
          stretchIndex += 32;
        }
      }
    }
    pnlStretchModeX = pnlAspectTableX[stretchIndex];
    pnlStretchModeY = pnlAspectTableY[stretchIndex];
    pnlScreenWidth = Math.max (PNL_MIN_WIDTH, (CRTC.crtR03HDispEndCurr - CRTC.crtR02HBackEndCurr) << 3);
    pnlScreenHeight = Math.max (PNL_MIN_HEIGHT, (CRTC.crtR07VDispEndCurr - CRTC.crtR06VBackEndCurr) << (CRTC.crtInterlace || CRTC.crtSlit ? 1 : 0));
    pnlStretchWidth = Math.round ((float) pnlScreenWidth * pnlStretchModeX);
    pnlStretchHeight = Math.round ((float) pnlScreenHeight * pnlStretchModeY);
    if (RasterBreakPoint.RBP_ON) {
      //ラスタブレークポイントウインドウが開いていたら更新する
      if ((dbgVisibleMask & DBG_RBP_VISIBLE_MASK) != 0) {
        RasterBreakPoint.rbpUpdateFrame ();
      }
    }
    //
    pnlFixedScale = pnlFixedModel.getNumber ().intValue ();  //固定倍率
    //スクリーンとキーボードの配置を決める
    pnlRotatedWidth = pnlStretchWidth;
    pnlRotatedHeight = pnlStretchHeight;
    if (PNL_ROTATION_ON && ((pnlRotationMode & 1) != 0)) {  //90度または270度のとき
      pnlRotatedWidth = pnlStretchHeight;  //幅と高さを入れ替える
      pnlRotatedHeight = pnlStretchWidth;
    }
    if (pnlMode == PNL_FIXEDSCALE) {  //固定倍率のとき
      //配置の計算
      //perl optdiv.pl 32768 100
      //  x/100==x*5243>>>19 (0<=x<=43698) [32768*5243==171802624]
      //pnlZoomWidth = (pnlRotatedWidth * pnlFixedScale + 50) / 100;
      //pnlZoomHeight = (pnlRotatedHeight * pnlFixedScale + 50) / 100;
      pnlZoomWidth = (pnlRotatedWidth * pnlFixedScale + 50) * 5243 >>> 19;
      pnlZoomHeight = (pnlRotatedHeight * pnlFixedScale + 50) * 5243 >>> 19;
      int width = Math.max (Math.max (PNL_MIN_WIDTH, pnlZoomWidth * pnlStereoscopicFactor), Keyboard.kbdWidth);
      int height = Math.max (PNL_MIN_HEIGHT, pnlZoomHeight) + Keyboard.kbdHeight;
      pnlScreenX1 = (width - pnlZoomWidth * pnlStereoscopicFactor) >> 1;
      pnlScreenY1 = (height - pnlZoomHeight - Keyboard.kbdHeight) >> 1;
      if (pnlWidth != width || pnlHeight != height) {  //パネルの大きさが合っていないとき
        pnlWidth = width;
        pnlHeight = height;
        pnlMinimumWidth = width;  //固定倍率では使わないがウインドウに合わせるモードに移行したとき最小サイズが変化したことを検知できるようにする
        pnlMinimumHeight = height;
        pnlSize.setSize (width, height);
        pnlPanel.setMinimumSize (pnlSize);
        pnlPanel.setMaximumSize (pnlSize);
        pnlPanel.setPreferredSize (pnlSize);
      }
      frmMinimumSize.setSize (pnlMinimumWidth + frmMarginWidth, pnlMinimumHeight + frmMarginHeight);
      frmFrame.setMinimumSize (frmMinimumSize);
      frmFrame.setMaximumSize (frmMinimumSize);
      frmFrame.setPreferredSize (frmMinimumSize);
      frmFrame.setResizable (false);
      frmFrame.pack ();
    } else {  //ウインドウに合わせるとき
      //配置の計算
      if (PNL_ROUNDED_CORNER_ON) {
        //画面幅=回転後幅*立体視係数
        int screenWidth = pnlRotatedWidth * pnlStereoscopicFactor;
        //画面高さ=回転後高さ
        int screenHeight = pnlRotatedHeight;
        //最大幅=パネル幅
        int maximumWidth = pnlWidth;
        //最大高さ=パネル高さ-キーボード高さ
        int maximumHeight = pnlHeight - Keyboard.kbdHeight;
        //角丸半径=min(パネル幅,パネル高さ)*角丸率
        //  全画面表示でないか、サイドバイサイドまたはトップアンドボトムのとき0
        //  短辺の長さが奇数で50%のとき切り上げないこと
        double cornerRadius = (!(PNL_ROUNDED_CORNER_TEST || pnlMode == PNL_FULLSCREEN) ||
                               (pnlStereoscopicOn &&
                                (pnlStereoscopicMethod == PNL_SIDE_BY_SIDE ||
                                 pnlStereoscopicMethod == PNL_TOP_AND_BOTTOM)) ? 0 :
                               Math.min (pnlWidth, pnlHeight) * (double) pnlRoundedCornerRatio / 100.0);
        //角丸幅=角丸半径*2
        double cornerWidth = cornerRadius * 2.0;
        //角丸高さ=角丸半径*(キーボードあり?1:2)
        double cornerHeight = cornerRadius * (Keyboard.kbdHeight != 0 ? 1.0 : 2.0);
        //直線幅=最大幅-角丸幅
        double straightWidth = maximumWidth - cornerWidth;
        //直線高さ=最大高さ-角丸高さ
        double straightHeight = maximumHeight - cornerHeight;
        if (maximumHeight * screenWidth <= straightWidth * screenHeight) {
          //最大高さ*画面幅<=直線幅*画面高さ
          //直線幅が十分に大きい。画面が上下の壁に接する
          //拡大高さ=最大高さ
          pnlZoomHeight = maximumHeight;
          //拡大幅=round(拡大高さ*画面幅/画面高さ)
          pnlZoomWidth = (pnlZoomHeight * screenWidth + (screenHeight >> 1)) / screenHeight;
          //画面座標
          if (pnlStereoscopicOn && pnlStereoscopicMethod == PNL_SIDE_BY_SIDE) {
            //サイドバイサイド。左半分の中央に半分の幅で配置する
            pnlScreenX1 = ((maximumWidth >> 1) - (pnlZoomWidth >> 1)) >> 1;
          } else {
            pnlScreenX1 = (maximumWidth - pnlZoomWidth * pnlStereoscopicFactor) >> 1;
          }
          pnlScreenY1 = 0;
        } else if (maximumWidth * screenHeight <= straightHeight * screenWidth) {
          //最大幅*画面高さ<=直線高さ*画面幅
          //直線高さが十分に大きい。画面が左右の壁に接する
          //拡大幅=最大幅
          pnlZoomWidth = maximumWidth;
          //拡大高さ=round(拡大幅*画面高さ/画面幅)
          pnlZoomHeight = (pnlZoomWidth * screenHeight + (screenWidth >> 1)) / screenWidth;
          //画面座標
          pnlScreenX1 = 0;
          if (pnlStereoscopicOn && pnlStereoscopicMethod == PNL_TOP_AND_BOTTOM) {
            //トップアンドボトム。上半分の中央に半分の高さで配置する
            pnlScreenY1 = ((maximumHeight >> 1) - (pnlZoomHeight >> 1)) >> 1;
          } else {
            pnlScreenY1 = (maximumHeight - pnlZoomHeight) >> 1;
          }
        } else {
          //縦横比が近い。画面が角丸に接する
          //  角丸上の点について縦横比が一致する位置を求める
          //  曲線0<=x,0<=y,x^n+y^n=1上の点A,Bからそれらの間にある点Mを求める
          //    A+Bの方向にある曲線上の点M
          //      mx=(ax+bx)/sqrt[n]((ax+bx)^n+(ay+by)^n)
          //      my=(ay+by)/sqrt[n]((ax+bx)^n+(ay+by)^n)
          //    n=2の場合
          //      加法定理と半角の公式を使うと式が簡単になる
          //      加法定理
          //        cos(a+b)=cos(a)*cos(b)-sin(a)*sin(b)
          //        sin(a+b)=sin(a)*cos(b)+cos(a)*sin(b)
          //      半角の公式
          //        cos(t/2)^2=(1+cos(t))/2
          //        sin(t/2)^2=(1-cos(t))/2
          //      角の2等分(0<=a,b<=pi/2)
          //        cos((a+b)/2)=sqrt((1+cos(a+b))/2)
          //        sin((a+b)/2)=sqrt((1-cos(a+b))/2)
          //      mx=sqrt(1/2+(ax*bx-ay*by)/2)
          //      my=sqrt(1/2-(ax*bx-ay*by)/2)
          //    n=3の場合
          //      mx=(ax+bx)/cbrt(2+3*(ax*bx*(ax+bx)+ay*by*(ay+by)))
          //      my=(ay+by)/cbrt(2+3*(ax*bx*(ax+bx)+ay*by*(ay+by)))
          //  曲線のままでは式が複雑になるのでここでは多角形を用いる
          double ax = 1.0;
          double ay = 0.0;
          double bx = 0.0;
          double by = 1.0;
          for (int i = 0; i < 4; i++) {  //16分割。64角形
            double mx;
            double my;
            if (false) {  //n=2
              double t = (ax * bx - ay * by) * 0.5;
              mx = Math.sqrt (0.5 + t);
              my = Math.sqrt (0.5 - t);
            } else {  //n=3
              double sx = ax + bx;
              double sy = ay + by;
              double t = Math.cbrt (2.0 + 3.0 * (ax * bx * sx + ay * by * sy));
              mx = sx / t;
              my = sy / t;
            }
            //仮幅=直線幅+mx*角丸幅
            double temporaryWidth = straightWidth + mx * cornerWidth;
            //仮高さ=直線高さ+my*角丸高さ
            double temporaryHeight = straightHeight + my * cornerHeight;
            if (temporaryHeight * screenWidth <= temporaryWidth * screenHeight) {
              //仮高さ*画面幅<=仮幅*画面高さ
              //横に長過ぎる。M-Bに狭める
              ax = mx;
              ay = my;
            } else {
              //縦に長過ぎる。A-Mに狭める
              bx = mx;
              by = my;
            }
          }
          //直線AB上の点について縦横比が一致する位置を求める
          //  仮幅=直線幅+(ax+(bx-ax)*t)*角丸幅
          //  仮高さ=直線高さ+(ay+(by-ay)*t)*角丸高さ
          //  仮幅/仮高さ=画面幅/画面高さ
          //
          //  仮幅*画面高さ=仮高さ*画面幅
          //  (直線幅+(ax+(bx-ax)*t)*角丸幅)*画面高さ=(直線高さ+(ay+(by-ay)*t)*角丸高さ)*画面幅
          //  直線幅*画面高さ+(ax+(bx-ax)*t)*角丸幅*画面高さ=直線高さ*画面幅+(ay+(by-ay)*t)*角丸高さ*画面幅
          //  直線幅*画面高さ+ax*角丸幅*画面高さ+(bx-ax)*t*角丸幅*画面高さ=直線高さ*画面幅+ay*角丸高さ*画面幅+(by-ay)*t*角丸高さ*画面幅
          //  (bx-ax)*t*角丸幅*画面高さ-(by-ay)*t*角丸高さ*画面幅=直線高さ*画面幅+ay*角丸高さ*画面幅-直線幅*画面高さ-ax*角丸幅*画面高さ
          //  ((bx-ax)*角丸幅*画面高さ-(by-ay)*角丸高さ*画面幅)*t=(直線高さ+ay*角丸高さ)*画面幅-(直線幅+ax*角丸幅)*画面高さ
          //  t=((直線高さ+ay*角丸高さ)*画面幅-(直線幅+ax*角丸幅)*画面高さ)/((bx-ax)*角丸幅*画面高さ-(by-ay)*角丸高さ*画面幅)
          double t = (((straightHeight + ay * cornerHeight) * screenWidth
                       - (straightWidth + ax * cornerWidth) * screenHeight)
                      / ((bx - ax) * cornerWidth * screenHeight
                         - (by - ay) * cornerHeight * screenWidth));
          //拡大幅=直線幅+(ax+(bx-ax)*t)*角丸幅
          pnlZoomWidth = (int) Math.round (straightWidth + (ax + (bx - ax) * t) * cornerWidth);
          if (false) {
            //拡大高さ=直線高さ+(ay+(by-ay)*t)*角丸高さ
            pnlZoomHeight = (int) Math.round (straightHeight + (ay + (by - ay) * t) * cornerHeight);
          } else {
            //拡大高さ=round(拡大幅*画面高さ/画面幅)
            pnlZoomHeight = (pnlZoomWidth * screenHeight + (screenWidth >> 1)) / screenWidth;
          }
          //画面座標
          pnlScreenX1 = (maximumWidth - pnlZoomWidth * pnlStereoscopicFactor) >> 1;
          pnlScreenY1 = (maximumHeight - pnlZoomHeight) >> 1;
        }
      } else {
        if (pnlWidth * pnlRotatedHeight >= (pnlHeight - Keyboard.kbdHeight) * (pnlRotatedWidth * pnlStereoscopicFactor)) {  //ウインドウに合わせると上下に隙間ができないとき
          //パネルの下端にキーボード配置して残った部分にスクリーンを目一杯拡大する
          //    pnlScreenX1                                          pnlScreenX1            pnlScreenX1
          //    |pnlZoomWidth|    |pnlZoomWidth|  |pnlZoomWidth|    |pnlZoomWidth|        |pnlZoomWidth|
          //  +-+------------+-+  +------------+  +------------+  +-+------------+-+  +---+------------+---+ --
          //  | |            | |  |            |  |            |  | |            | |  |   |            |   |
          //  | |   screen   | |  |   screen   |  |   screen   |  | |   screen   | |  |   |   screen   |   | pnlZoomHeight
          //  | |            | |  |            |  |            |  | |            | |  |   |            |   |
          //  | +-+--------+-+ |  +-+--------+-+  +------------+  +-+------------+-+  | +-+------------+-+ | -- pnlKeyboardY
          //  |   |keyboard|   |  | |keyboard| |  |  keyboard  |  |    keyboard    |  | |    keyboard    | | kbdHeight
          //  +---+--------+---+  +-+--------+-+  +------------+  +----------------+  +-+----------------+-+ --
          //      |                 |                             |    kbdWidth    |    |    kbdWidth    |
          //      pnlKeyboardX      pnlKeyboardX                                        pnlKeyboardX
          pnlZoomHeight = pnlHeight - Keyboard.kbdHeight;
          pnlZoomWidth = (pnlZoomHeight * pnlRotatedWidth + (pnlRotatedHeight >> 1)) / pnlRotatedHeight;
          if (pnlStereoscopicOn && pnlStereoscopicMethod == PNL_SIDE_BY_SIDE) {
            pnlScreenX1 = ((pnlWidth >> 1) - (pnlZoomWidth >> 1)) >> 1;
          } else {
            pnlScreenX1 = (pnlWidth - pnlZoomWidth * pnlStereoscopicFactor) >> 1;
          }
          pnlScreenY1 = 0;
        } else {  //ウインドウに合わせると上下に隙間ができるとき
          //左右が先につっかえたのだからスクリーンの幅がキーボードの幅よりも狭いということはない
          //  スクリーンの幅がキーボードの幅よりも狭かったらスクリーンの上と左右の両方に隙間があることになってしまうのでウインドウに合っていない
          //  |pnlZoomWidth|  |pnlZoomWidth|
          //  +------------+  +------------+
          //  |            |  |            |
          //  +------------+  +------------+ -- pnlScreenY1
          //  |            |  |            |
          //  |   screen   |  |   screen   | pnlZoomHeight
          //  |            |  |            |
          //  +-+--------+-+  +------------+ -- pnlKeyboardY
          //  | |keyboard| |  |  keyboard  | kbdHeight
          //  | +--------+ |  +------------+ --
          //  |            |  |            |
          //  +------------+  +------------+
          //    |kbdWidth|    |  kbdWidth  |
          //    pnlKeyboardX
          pnlZoomWidth = pnlWidth / pnlStereoscopicFactor;
          pnlZoomHeight = (pnlZoomWidth * pnlStereoscopicFactor * pnlRotatedHeight + (pnlRotatedWidth * pnlStereoscopicFactor >> 1)) / (pnlRotatedWidth * pnlStereoscopicFactor);
          pnlScreenX1 = 0;
          if (pnlStereoscopicOn && pnlStereoscopicMethod == PNL_TOP_AND_BOTTOM) {
            pnlScreenY1 = (((pnlHeight - Keyboard.kbdHeight) >> 1) - (pnlZoomHeight >> 1)) >> 1;
          } else {
            pnlScreenY1 = (pnlHeight - (pnlZoomHeight + Keyboard.kbdHeight)) >> 1;
          }
        }
      }  //PNL_ROUNDED_CORNER_ON
      //最小サイズと最大サイズ
      int minimumWidth = Math.max (PNL_MIN_WIDTH, Keyboard.kbdWidth);
      int minimumHeight = PNL_MIN_HEIGHT + Keyboard.kbdHeight;
      if (pnlMinimumWidth != minimumWidth || pnlMinimumHeight != minimumHeight) {  //最小サイズが変化した。ウインドウに合わせるモードに移行したかキーボードの有無または種類が変わった
        pnlMinimumWidth = minimumWidth;
        pnlMinimumHeight = minimumHeight;
      }
      frmMinimumSize.setSize (pnlMinimumWidth + frmMarginWidth, pnlMinimumHeight + frmMarginHeight);
      frmFrame.setMinimumSize (frmMinimumSize);
      frmFrame.setMaximumSize (null);
      frmFrame.setResizable (true);
    }
    //
    pnlArrangementCommon ();
    Mouse.musUpdateSpeedRatio ();
    if (!PNL_FILL_BACKGROUND) {
      pnlFillBackgroundRequest = true;
    }
    if (FlashingLights.FLR_ON &&
        FlashingLights.flrEnabled) {
      FlashingLights.flrMakeIndex ();
    }
  }  //pnlUpdateArrangement

  public static void pnlArrangementCommon () {
    if (PNL_STEREOSCOPIC_ON && pnlStereoscopicOn) {
      if (pnlStereoscopicMethod == PNL_NAKED_EYE_CROSSING ||
          pnlStereoscopicMethod == PNL_NAKED_EYE_PARALLEL) {
        pnlScreenX2 = pnlScreenX1 + pnlZoomWidth;
        pnlScreenX3 = pnlScreenX2;
        pnlScreenX4 = pnlScreenX3 + pnlZoomWidth;
        pnlScreenY2 = pnlScreenY1 + pnlZoomHeight;
        pnlScreenY3 = pnlScreenY1;
        pnlScreenY4 = pnlScreenY2;
      } else if (pnlStereoscopicMethod == PNL_SIDE_BY_SIDE) {
        pnlScreenX2 = pnlScreenX1 + (pnlZoomWidth >> 1);
        pnlScreenX3 = pnlScreenX2;
        pnlScreenX4 = pnlScreenX3 + (pnlZoomWidth >> 1);
        pnlScreenY2 = pnlScreenY1 + pnlZoomHeight;
        pnlScreenY3 = pnlScreenY1;
        pnlScreenY4 = pnlScreenY2;
      } else {  //pnlStereoscopicMethod == PNL_TOP_AND_BOTTOM
        pnlScreenX2 = pnlScreenX1 + pnlZoomWidth;
        pnlScreenX3 = pnlScreenX1;
        pnlScreenX4 = pnlScreenX2;
        pnlScreenY2 = pnlScreenY1 + (pnlZoomHeight >> 1);
        pnlScreenY3 = pnlScreenY2;
        pnlScreenY4 = pnlScreenY3 + (pnlZoomHeight >> 1);
      }
    } else {
      pnlScreenX2 = pnlScreenX1 + pnlZoomWidth;
      pnlScreenX3 = pnlScreenX1;
      pnlScreenX4 = pnlScreenX2;
      pnlScreenY2 = pnlScreenY1 + pnlZoomHeight;
      pnlScreenY3 = pnlScreenY1;
      pnlScreenY4 = pnlScreenY2;
    }
    pnlKeyboardX = (pnlWidth - Keyboard.kbdWidth) >> 1;
    pnlKeyboardY = pnlScreenY4;
    pnlZoomRatioOutX = ((pnlZoomWidth * pnlStereoscopicFactor) << 16) / pnlScreenWidth;
    pnlZoomRatioOutY = (pnlZoomHeight << 16) / pnlScreenHeight;
    pnlZoomRatioInX = (pnlScreenWidth << 16) / (pnlZoomWidth * pnlStereoscopicFactor);
    pnlZoomRatioInY = (pnlScreenHeight << 16) / pnlZoomHeight;
    if (PNL_ROTATION_ON) {
      //サブイメージを作る
      if (PNL_USE_THREAD) {
        for (int n = 0; n < 4; n++) {
          pnlScreenSubImageLeftArray[n] = pnlScreenImageLeftArray[n].getSubimage (0, 0, pnlScreenWidth, pnlScreenHeight);
          pnlScreenSubImageRightArray[n] = pnlScreenImageRightArray[n].getSubimage (0, 0, pnlScreenWidth, pnlScreenHeight);
        }
      } else {
        pnlScreenSubImageLeft = pnlScreenImageLeft.getSubimage (0, 0, pnlScreenWidth, pnlScreenHeight);
        pnlScreenSubImageRight = pnlScreenImageRight.getSubimage (0, 0, pnlScreenWidth, pnlScreenHeight);
      }
      //アフィン変換を作る
      //  レクタングル(ax,ay)-(bx,by)を
      //  反時計回りに0度,90度,180度,270度回転させて
      //  レクタングル(cx,cy)-(dx,dy)に収める
      double ax = 0.0;
      double ay = 0.0;
      double bx = (double) pnlScreenWidth;
      double by = (double) pnlScreenHeight;
      double l00, l10, l01, l11, l02, l12;
      double r00, r10, r01, r11, r02, r12;
      if (pnlRotationMode == 0) {  //0度
        double cx = (double) pnlScreenX1;
        double cy = (double) pnlScreenY1;
        double dx = (double) pnlScreenX2;
        double dy = (double) pnlScreenY2;
        l00 = (cx - dx) / (ax - bx);
        l10 = 0.0;
        l01 = 0.0;
        l11 = (cy - dy) / (ay - by);
        l02 = (ax * dx - bx * cx) / (ax - bx);
        l12 = (ay * dy - by * cy) / (ay - by);
        cx = (double) pnlScreenX3;
        cy = (double) pnlScreenY3;
        dx = (double) pnlScreenX4;
        dy = (double) pnlScreenY4;
        r00 = (cx - dx) / (ax - bx);
        r10 = 0.0;
        r01 = 0.0;
        r11 = (cy - dy) / (ay - by);
        r02 = (ax * dx - bx * cx) / (ax - bx);
        r12 = (ay * dy - by * cy) / (ay - by);
      } else if (pnlRotationMode == 1) {  //90度
        double cx = (double) pnlScreenX1;
        double cy = (double) pnlScreenY1;
        double dx = (double) pnlScreenX2;
        double dy = (double) pnlScreenY2;
        l00 = 0.0;
        l10 = (dy - cy) / (ax - bx);
        l01 = (cx - dx) / (ay - by);
        l11 = 0.0;
        l02 = (ay * dx - by * cx) / (ay - by);
        l12 = (ax * cy - bx * dy) / (ax - bx);
        cx = (double) pnlScreenX3;
        cy = (double) pnlScreenY3;
        dx = (double) pnlScreenX4;
        dy = (double) pnlScreenY4;
        r00 = 0.0;
        r10 = (dy - cy) / (ax - bx);
        r01 = (cx - dx) / (ay - by);
        r11 = 0.0;
        r02 = (ay * dx - by * cx) / (ay - by);
        r12 = (ax * cy - bx * dy) / (ax - bx);
      } else if (pnlRotationMode == 2) {  //180度
        double cx = (double) pnlScreenX1;
        double cy = (double) pnlScreenY1;
        double dx = (double) pnlScreenX2;
        double dy = (double) pnlScreenY2;
        l00 = (dx - cx) / (ax - bx);
        l10 = 0.0;
        l01 = 0.0;
        l11 = (dy - cy) / (ay - by);
        l02 = (ax * cx - bx * dx) / (ax - bx);
        l12 = (ay * cy - by * dy) / (ay - by);
        cx = (double) pnlScreenX3;
        cy = (double) pnlScreenY3;
        dx = (double) pnlScreenX4;
        dy = (double) pnlScreenY4;
        r00 = (dx - cx) / (ax - bx);
        r10 = 0.0;
        r01 = 0.0;
        r11 = (dy - cy) / (ay - by);
        r02 = (ax * cx - bx * dx) / (ax - bx);
        r12 = (ay * cy - by * dy) / (ay - by);
      } else {  //270度
        double cx = (double) pnlScreenX1;
        double cy = (double) pnlScreenY1;
        double dx = (double) pnlScreenX2;
        double dy = (double) pnlScreenY2;
        l00 = 0.0;
        l10 = (cy - dy) / (ax - bx);
        l01 = (dx - cx) / (ay - by);
        l11 = 0.0;
        l02 = (ay * cx - by * dx) / (ay - by);
        l12 = (ax * dy - bx * cy) / (ax - bx);
        cx = (double) pnlScreenX3;
        cy = (double) pnlScreenY3;
        dx = (double) pnlScreenX4;
        dy = (double) pnlScreenY4;
        r00 = 0.0;
        r10 = (cy - dy) / (ax - bx);
        r01 = (dx - cx) / (ay - by);
        r11 = 0.0;
        r02 = (ay * cx - by * dx) / (ay - by);
        r12 = (ax * dy - bx * cy) / (ax - bx);
      }
      pnlRotationTransformLeft.setTransform (l00, l10, l01, l11, l02, l12);
      pnlRotationTransformRight.setTransform (r00, r10, r01, r11, r02, r12);
      pnlMatrixL00 = l00;
      pnlMatrixL10 = l10;
      pnlMatrixL01 = l01;
      pnlMatrixL11 = l11;
      pnlMatrixL02 = l02;
      pnlMatrixL12 = l12;
      pnlMatrixR00 = r00;
      pnlMatrixR10 = r10;
      pnlMatrixR01 = r01;
      pnlMatrixR11 = r11;
      pnlMatrixR02 = r02;
      pnlMatrixR12 = r12;
      //逆変換を作る
      //  d=m00*m11-m01*m10
      //  [ m00 m01 m02 ](-1)   [  m11/d -m01/d (m01*m12-m02*m11)/d ]
      //  [ m10 m11 m12 ]     = [ -m10/d  m00/d (m02*m10-m00*m12)/d ]
      //  [  0   0   1  ]       [    0      0            1          ]
      //  マウスが(x1,y1)-(x2,y2)にあるときinverseL、(x3,y3)-(x4,y4)にあるときinverseRを使って画面座標を求める
      double d = l00 * l11 - l01 * l10;
      pnlInverseL00 = l11 / d;
      pnlInverseL10 = -l10 / d;
      pnlInverseL01 = -l01 / d;
      pnlInverseL11 = l00 / d;
      pnlInverseL02 = (l01 * l12 - l02 * l11) / d;
      pnlInverseL12 = (l02 * l10 - l00 * l12) / d;
      d = r00 * r11 - r01 * r10;
      pnlInverseR00 = r11 / d;
      pnlInverseR10 = -r10 / d;
      pnlInverseR01 = -r01 / d;
      pnlInverseR11 = r00 / d;
      pnlInverseR02 = (r01 * r12 - r02 * r11) / d;
      pnlInverseR12 = (r02 * r10 - r00 * r12) / d;
    }
  }  //pnlArrangementCommon



  //モード
  public static final int PNL_UNDEFINED   = 0;  //未定義
  public static final int PNL_FIXEDSCALE  = 1;  //固定倍率
  public static final int PNL_FITINWINDOW = 2;  //ウインドウに合わせる
  public static final int PNL_FULLSCREEN  = 3;  //全画面表示
  public static final int PNL_MAXIMIZED   = 4;  //最大化
  public static int pnlModeRequest;  //起動時、全画面表示と最大化は遅延して切り替える。最大化はRestorableFrameで復元されている可能性がある
  public static int pnlMode;  //モード
  public static int pnlPrevMode;  //全画面表示または最大化に移行する前のモード

  //メニューアイテム
  public static JRadioButtonMenuItem mnbFullScreenMenuItem;  //全画面表示
  public static JRadioButtonMenuItem mnbMaximizedMenuItem;  //最大化
  public static JRadioButtonMenuItem mnbFitInWindowMenuItem;  //ウインドウに合わせる
  public static JRadioButtonMenuItem mnbFixedScaleMenuItem;  //固定倍率

  //角丸調整
  //  モニタの角が丸く、モニタとX68000の画面の縦横比が近いとき、
  //  全画面表示にすると画面の角が欠ける問題を回避する
  //  https://x.com/veryhotcurry/status/2035331642034925809
  public static boolean PNL_ROUNDED_CORNER_ON = true;
  public static boolean PNL_ROUNDED_CORNER_TEST = false;  //全画面表示でなくても角丸を適用する
  public static int pnlRoundedCornerRatio;  //角丸率。0～50。短辺の長さに対するパーセント
  public static SpinnerNumberModel pnlRoundedCornerModel;
  public static JSpinner pnlRoundedCornerSpinner;

  //遅延切り替え
  public static int PNL_BOOT_DELAY = 500;
  public static javax.swing.Timer pnlBootTimer;

  //pnlInit2
  //  初期化2
  public static void pnlInit2 () {
    pnlModeRequest = PNL_UNDEFINED;
    pnlMode = PNL_FITINWINDOW;
    pnlPrevMode = PNL_FITINWINDOW;
    switch (Settings.sgsGetString ("scaling").toLowerCase ()) {
    case "fullscreen":  //全画面表示
      pnlModeRequest = PNL_FULLSCREEN;
      break;
    case "maximized":  //最大化
      pnlModeRequest = PNL_MAXIMIZED;
      break;
    case "fitinwindow":  //ウインドウに合わせる
      break;
    case "fixedscale":  //固定倍率
      pnlMode = PNL_FIXEDSCALE;
      break;
    }
    if (PNL_ROUNDED_CORNER_ON) {
      pnlRoundedCornerRatio = Settings.sgsGetInt ("roundedcornerratio", 0, 0, 50);
    }
  }  //pnlInit2

  //pnlTini2
  //  後始末2
  public static void pnlTini2 () {
    Settings.sgsPutString ("scaling",
                           pnlMode == PNL_FULLSCREEN ? "fullscreen" :
                           pnlMode == PNL_MAXIMIZED ? "maximized" :
                           pnlMode == PNL_FITINWINDOW ? "fitinwindow" :
                           "fixedscale");
    if (PNL_ROUNDED_CORNER_ON) {
      Settings.sgsPutInt ("roundedcornerratio", pnlRoundedCornerRatio);
    }
  }  //pnlTini2

  //pnlMake2
  //  構築2
  public static void pnlMake2 () {
    //メニュー
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        String command = ae.getActionCommand ();
        switch (command) {
        case "Full screen":  //全画面表示
          pnlSetMode (PNL_FULLSCREEN);
          break;
        case "Maximized":  //最大化
          pnlSetMode (PNL_MAXIMIZED);
          break;
        case "Resize the X68000 screen to fit the window":  //Fit in window ウインドウに合わせる
          pnlSetMode (PNL_FITINWINDOW);
          break;
        case "Resize the window to fit the X68000 screen":  //Fixed scale 固定倍率
          pnlSetMode (PNL_FIXEDSCALE);
          break;
        }
      }
    };
    ButtonGroup group = new ButtonGroup ();
    mnbFullScreenMenuItem = ComponentFactory.setEnabled (
      Multilingual.mlnText (
        ComponentFactory.createRadioButtonMenuItem (
          group,
          pnlMode == PNL_FULLSCREEN,
          "Full screen",
          listener),
        "ja", "全画面表示"),
      pnlIsFullScreenSupported);
    mnbMaximizedMenuItem = Multilingual.mlnText (
      ComponentFactory.createRadioButtonMenuItem (
        group,
        pnlMode == PNL_MAXIMIZED,
        "Maximized",
        listener),
      "ja", "最大化");
    mnbFitInWindowMenuItem = Multilingual.mlnText (
      ComponentFactory.createRadioButtonMenuItem (
        group,
        pnlMode == PNL_FITINWINDOW,
        "Resize the X68000 screen to fit the window",  //Fit in window
        'W',
        MNB_MODIFIERS,
        listener),
      "ja", "X68000の画面をリサイズしてウインドウに合わせる");  //ウインドウに合わせる
    mnbFixedScaleMenuItem = Multilingual.mlnText (
      ComponentFactory.createRadioButtonMenuItem (
        group,
        pnlMode == PNL_FIXEDSCALE,
        "Resize the window to fit the X68000 screen",  //Fixed scale
        'X',
        MNB_MODIFIERS,
        listener),
      "ja", "ウインドウをリサイズしてX68000の画面に合わせる");  //固定倍率
  }  //pnlMake2

  //pnlStart2 ()
  //  開始2
  public static void pnlStart2 () {
    //ウインドウステートリスナー
    frmFrame.addWindowStateListener (new WindowStateListener () {
      @Override public void windowStateChanged (WindowEvent we) {
        int state = frmFrame.getExtendedState ();
        if (pnlMode != PNL_MAXIMIZED &&
            (state & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) {  //最大化した
          pnlSetMode (PNL_MAXIMIZED);
        } else if (pnlMode == PNL_MAXIMIZED &&
                   (state & Frame.MAXIMIZED_BOTH) != Frame.MAXIMIZED_BOTH) {  //最大化が終了した
          pnlSetMode (pnlPrevMode);
        }
      }
    });
  }  //pnlStart2

  //pnlBoot2 ()
  //  起動2
  public static void pnlBoot2 () {
    if (pnlModeRequest != PNL_UNDEFINED) {
      pnlBootTimer = new javax.swing.Timer (PNL_BOOT_DELAY, new ActionListener () {
        public void actionPerformed (ActionEvent ae) {
          if (pnlModeRequest == PNL_FULLSCREEN) {
            mnbFullScreenMenuItem.doClick ();
          } else if (pnlModeRequest == PNL_MAXIMIZED) {
            mnbMaximizedMenuItem.doClick ();
          }
          pnlBootTimer.stop ();
          pnlBootTimer = null;
        }
      });
      pnlBootTimer.start ();
    }
  }  //pnlBoot2

  //pnlSetMode (mode)
  //  モードの変更
  public static void pnlSetMode (int mode) {
    do {
      //変更がなければラジオボタンの確認だけ行う
      if (pnlMode == mode) {
        break;
      }
      //全画面表示に移行できなければ諦めてラジオボタンを修正する
      String text = null;
      if (mode == PNL_FULLSCREEN) {  //変更後が全画面表示
        GraphicsDevice screenDevice = frmFrame.getGraphicsConfiguration ().getDevice ();
        if (!screenDevice.isFullScreenSupported ()) {  //全画面表示に対応していない
          JOptionPane.showMessageDialog (
            frmFrame,
            Multilingual.mlnJapanese ?
            "全画面表示に対応していません" :
            "Full screen is not supported");
          break;
        }
        if (Bubble.BBL_ON) {
          text = ButtonFunction.bfnFullScreenText ();
          if (text == null) {  //全画面表示を終了する方法がない
            JOptionPane.showMessageDialog (
              frmFrame,
              Multilingual.mlnJapanese ?
              "全画面表示を終了するキーまたはボタンがありません" :
              "No key or button to exit full screen");
            break;
          }
        }
        frmScreenDevice = screenDevice;
      }
      //変更前が全画面表示または最大化のとき
      if (pnlMode == PNL_FULLSCREEN) {  //変更前が全画面表示
        pnlMode = pnlPrevMode;
        if (Bubble.BBL_ON) {
          Bubble.bblEnd ();
        }
        if (frmScreenDevice.getFullScreenWindow () == frmFrame) {  //全画面表示している
          frmScreenDevice.setFullScreenWindow (null);  //全画面表示を解除する
          frmFrame.getRootPane().setWindowDecorationStyle (JRootPane.FRAME);  //飾り枠を描く
        }
        frmFrame.setJMenuBar (mnbMenuBar);  //メニューバーを表示する
        if (pnlHideKeyboard) {  //全画面表示のときキーボードを隠す
          if (pnlPrevKeyboardOn) {  //全画面表示に移行する前はキーボードを表示していた
            Keyboard.kbdSetOn (true);  //キーボードを表示する
          }
        }
      } else if (pnlMode == PNL_MAXIMIZED) {  //変更前が最大化
        pnlMode = pnlPrevMode;
        if ((frmFrame.getExtendedState () & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) {  //最大化している
          frmFrame.setExtendedState (Frame.NORMAL);  //最大化を解除する
        }
      }
      //変更後が全画面表示または最大化のとき
      if (mode == PNL_FULLSCREEN) {  //変更後が全画面表示
        pnlPrevMode = pnlMode;
        if (pnlHideKeyboard) {  //全画面表示のときキーボードを隠す
          pnlPrevKeyboardOn = Keyboard.kbdImage != null;  //全画面表示に移行する前はキーボードを表示していたか
          if (pnlPrevKeyboardOn) {
            Keyboard.kbdSetOn (false);  //キーボードを表示しない
          }
        }
        frmFrame.setJMenuBar (null);  //メニューバーを表示しない
        if (frmScreenDevice.getFullScreenWindow () != frmFrame) {  //全画面表示していない
          frmFrame.getRootPane().setWindowDecorationStyle (JRootPane.NONE);  //飾り枠を消す
          frmScreenDevice.setFullScreenWindow (frmFrame);  //全画面表示する
        }
        if (Bubble.BBL_ON) {
          if (text != null) {
            Bubble.bblStart (text + (Multilingual.mlnJapanese ? "で全画面表示を終了" : " to exit full screen"), 5000L);
          }
        }
      } else if (mode == PNL_MAXIMIZED) {  //変更後が最大化
        pnlPrevMode = pnlMode;
        frmFrame.setExtendedState (Frame.MAXIMIZED_BOTH);  //最大化する
      }
      pnlMode = mode;
      //再配置する
      //  固定倍率のときここでフレームのサイズが変わることがある
      pnlUpdateArrangement ();
    } while (false);
    //ラジオボタンを確認して合っていなければ修正する
    if (pnlMode == PNL_FIXEDSCALE) {
      if (!mnbFixedScaleMenuItem.isSelected ()) {
        mnbFixedScaleMenuItem.setSelected (true);
      }
    } else if (pnlMode == PNL_FITINWINDOW) {
      if (!mnbFitInWindowMenuItem.isSelected ()) {
        mnbFitInWindowMenuItem.setSelected (true);
      }
    } else if (pnlMode == PNL_FULLSCREEN) {
      if (!mnbFullScreenMenuItem.isSelected ()) {
        mnbFullScreenMenuItem.setSelected (true);
      }
    } else if (pnlMode == PNL_MAXIMIZED) {
      if (!mnbMaximizedMenuItem.isSelected ()) {
        mnbMaximizedMenuItem.setSelected (true);
      }
    }
  }  //pnlSetMode



  //========================================================================================
  //$$RBT ロボット

  public static Robot rbtRobot;  //ロボット

  //rbtInit ()
  public static void rbtInit () {

    //ロボット
    rbtRobot = null;
    try {
      rbtRobot = new Robot ();  //プライマリスクリーンのみを対象とする
    } catch (Exception e) {
    }

  }  //rbtInit()



  //========================================================================================
  //$$MNB メニューバー
  //  メニューバーの高さは23px

  public static final int MNB_MODIFIERS = InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK;  //アクセラレータのモディファイヤ

  //メニューバー
  public static JMenuBar mnbMenuBar;  //メニューバー

  //メニュー
  public static JMenu mnbFileMenu;  //ファイル
  public static JMenu mnbDisplayMenu;  //画面
  public static JMenu mnbSoundMenu;  //音声
  public static JMenu mnbInputMenu;  //入力
  public static JMenu mnbConfigMenu;  //設定
  public static JMenu mnbLanguageMenu;  //言語

  //メニューアイテム
  //  チェックボックスなどの変更内容はアクションイベントから取り出せるので個々のメニューアイテムに名前を付ける必要はない
  //  メニュー以外の方法で変更できるアイテムと一時的に変更できなくなるアイテムに名前をつけておく
  //  最初から最後まで選択できないメニューアイテムはメニューバーを作る時点で無効化するか、表示しない
  public static JMenuItem mnbQuitMenuItem;  //終了
  public static JCheckBoxMenuItem mnbStereoscopicMenuItem;  //立体視
  public static JCheckBoxMenuItem mnbPlayMenuItem;  //音声出力
  public static JMenuItem mnbPasteMenuItem;  //貼り付け
  public static JRadioButtonMenuItem mnbStandardKeyboardMenuItem;  //標準キーボード
  public static JRadioButtonMenuItem mnbCompactKeyboardMenuItem;  //コンパクトキーボード
  public static JRadioButtonMenuItem mnbNoKeyboardMenuItem;  //キーボードなし
  public static JLabel mnbVolumeLabel;  //音量


  //フォントサイズメニュー

  //mnbMakeFontSizeMenu ()
  //  フォントサイズメニューを作る
  public static JMenu mnbMakeFontSizeMenu () {
    //  アクションリスナー
    ActionListener actionListener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        String command = ae.getActionCommand ();
        switch (command) {
        case "Very small":  //極小
          LnF.lnfFontSizeRequest = 10;
          break;
        case "Small":  //小
          LnF.lnfFontSizeRequest = 12;
          break;
        case "Medium":  //中
          LnF.lnfFontSizeRequest = 14;
          break;
        case "Large":  //大
          LnF.lnfFontSizeRequest = 16;
          break;
        case "Very large":  //極大
          LnF.lnfFontSizeRequest = 18;
          break;
        default:
          System.out.println ("unknown action command " + command);
        }
      }
    };
    //  ボタングループ
    ButtonGroup fontSizeGroup = new ButtonGroup ();
    //  メニュー
    return Multilingual.mlnText (
      ComponentFactory.createMenu (
        "Font size",
        Multilingual.mlnText (
          ComponentFactory.pointSize (
            ComponentFactory.createRadioButtonMenuItem (fontSizeGroup, LnF.lnfFontSizeRequest == 10, "Very small", actionListener),
            10),
          "ja", "極小"),
        Multilingual.mlnText (
          ComponentFactory.pointSize (
            ComponentFactory.createRadioButtonMenuItem (fontSizeGroup, LnF.lnfFontSizeRequest == 12, "Small", actionListener),
            12),
          "ja", "小"),
        Multilingual.mlnText (
          ComponentFactory.pointSize (
            ComponentFactory.createRadioButtonMenuItem (fontSizeGroup, LnF.lnfFontSizeRequest == 14, "Medium", actionListener),
            14),
          "ja", "中"),
        Multilingual.mlnText (
          ComponentFactory.pointSize (
            ComponentFactory.createRadioButtonMenuItem (fontSizeGroup, LnF.lnfFontSizeRequest == 16, "Large", actionListener),
            16),
          "ja", "大"),
        Multilingual.mlnText (
          ComponentFactory.pointSize (
            ComponentFactory.createRadioButtonMenuItem (fontSizeGroup, LnF.lnfFontSizeRequest == 18, "Very large", actionListener),
            18),
          "ja", "極大")),
      "ja", "フォントサイズ");
  }  //mnbMakeFontSizeMenu


  //色メニュー
  public static final DecimalSpinner[] mnbColorSpinners = new DecimalSpinner[9];  //スピナー
  public static final int[] mnbColorRGB = new int[15];
  public static JPanel mnbColorPanel;

  //mnbColorHSBToRGB ()
  //  LnF.lnfHSBからmnbColorRGBを作る
  public static void mnbColorHSBToRGB () {
    for (int i = 0; i <= 14; i++) {
      int[] t = LnF.LNF_HSB_INTERPOLATION_TABLE[i];
      float h = (float) (t[0] * LnF.lnfHSB[0] + t[1] * LnF.lnfHSB[1] + t[2] * LnF.lnfHSB[2]) / (49.0F * 360.0F);
      float s = (float) (t[0] * LnF.lnfHSB[3] + t[1] * LnF.lnfHSB[4] + t[2] * LnF.lnfHSB[5]) / (49.0F * 100.0F);
      float b = (float) (t[0] * LnF.lnfHSB[6] + t[1] * LnF.lnfHSB[7] + t[2] * LnF.lnfHSB[8]) / (49.0F * 100.0F);
      mnbColorRGB[i] = Color.HSBtoRGB (h,
                                       Math.max (0.0F, Math.min (1.0F, s)),
                                       Math.max (0.0F, Math.min (1.0F, b)));
    }
  }  //mnbColorHSBToRGB

  //mnbMakeColorMenu ()
  //  色メニューを作る
  public static JMenu mnbMakeColorMenu () {
    mnbColorHSBToRGB ();
    //  パネル
    mnbColorPanel = ComponentFactory.setColor (
      ComponentFactory.setFixedSize (
        new JPanel () {
          @Override protected void paintComponent (Graphics g) {
            super.paintComponent (g);
            for (int i = 0; i <= 14; i++) {
              g.setColor (new Color (mnbColorRGB[i]));
              g.fillRect (LnF.lnfFontSize * i, 0, LnF.lnfFontSize, LnF.lnfFontSize * 5);
            }
          }
        },
        LnF.lnfFontSize * 15, LnF.lnfFontSize * 5),
      Color.white, Color.black);
    //  チェンジリスナー
    ChangeListener changeListener = new ChangeListener () {
      @Override public void stateChanged (ChangeEvent ce) {
        DecimalSpinner spinner = (DecimalSpinner) ce.getSource ();
        LnF.lnfHSB[spinner.getOption ()] = spinner.getIntValue ();
        mnbColorHSBToRGB ();
        mnbColorPanel.repaint ();
      }
    };
    //  アクションリスナー
    ActionListener actionListener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        String command = ae.getActionCommand ();
        switch (command) {
        case "Reset to default values":  //初期値に戻す
          for (int i = 0; i < 9; i++) {
            LnF.lnfHSB[i] = LnF.LNF_DEFAULT_HSB[i];
            mnbColorSpinners[i].setIntValue (LnF.lnfHSB[i]);
          }
          mnbColorHSBToRGB ();
          mnbColorPanel.repaint ();
          break;
        default:
          System.out.println ("unknown action command " + command);
        }
      }
    };
    //  スピナー
    for (int i = 0; i < 9; i++) {
      mnbColorSpinners[i] = ComponentFactory.createDecimalSpinner (
        LnF.lnfHSB[i], 0, i < 3 ? 720 : 100, 1, i, changeListener);
    }
    //  メニュー
    return Multilingual.mlnText (
      ComponentFactory.createMenu (
        "Color",
        ComponentFactory.createHorizontalBox (
          mnbColorSpinners[0],
          mnbColorSpinners[1],
          mnbColorSpinners[2],
          ComponentFactory.createLabel ("H °"),
          Box.createHorizontalGlue ()
          ),
        ComponentFactory.createHorizontalBox (
          mnbColorSpinners[3],
          mnbColorSpinners[4],
          mnbColorSpinners[5],
          ComponentFactory.createLabel ("S%"),
          Box.createHorizontalGlue ()
          ),
        ComponentFactory.createHorizontalBox (
          mnbColorSpinners[6],
          mnbColorSpinners[7],
          mnbColorSpinners[8],
          ComponentFactory.createLabel ("B%"),
          Box.createHorizontalGlue ()
          ),
        ComponentFactory.createHorizontalBox (
          ComponentFactory.setLineBorder (mnbColorPanel),
          Box.createHorizontalGlue ()
          ),
        Multilingual.mlnText (ComponentFactory.createMenuItem ("Reset to default values", actionListener), "ja", "初期値に戻す")
        ),
      "ja", "色");
  }  //mnbMakeColorMenu


  //言語メニュー
  //  テキストで書く
  //    国旗アイコンは対象とするマーケットを選択させるときに使うものであり、表示言語を切り替えるだけのメニューに国旗を用いるのは不適切
  public static JMenu mnbMakeLanguageMenu () {
    //  アクションリスナー
    ActionListener actionListener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        String command = ae.getActionCommand ();
        switch (command) {
        case "English":
          Multilingual.mlnChange ("en");
          break;
        case "日本語":
          Multilingual.mlnChange ("ja");
          break;
        default:
          System.out.println ("unknown action command " + command);
        }
      }
    };
    //  ボタングループ
    ButtonGroup languageGroup = new ButtonGroup ();
    //  メニュー
    return mnbLanguageMenu = Multilingual.mlnText (
      ComponentFactory.createMenu (
        "Language", 'L',
        ComponentFactory.createRadioButtonMenuItem (languageGroup, Multilingual.mlnEnglish, "English", actionListener),
        ComponentFactory.createRadioButtonMenuItem (languageGroup, Multilingual.mlnJapanese, "日本語", actionListener)
        ),
      "ja", "言語");
  }  //mnbMakeLanguageMenu


  //mnbMakeMenu ()
  //  メニューバーを作る
  //  メニューバーの幅は狭くしたいがメニューの幅が狭すぎると隣のメニューに流れやすくなって操作しにくいのでメニューの数を必要最小限にする
  public static void mnbMakeMenu () {

    //アクションリスナー
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        String command = ae.getActionCommand ();
        switch (command) {

          //ファイルメニュー
        case "Quit":  //終了
          if (PowerControl.PWC_ON) {
            PowerControl.pwcQuit ();
          } else {
            prgTini ();
          }
          break;

          //画面メニュー
        case "50%":
        case "75%":
        case "100%":
        case "150%":
        case "200%":
          pnlFixedModel.setValue (Integer.valueOf (Integer.parseInt (command.substring (0, command.length () - 1))));  //pnlFixedSpinnerのChangeEventが発動する
          break;
        case "Nearest neighbor":  //最近傍補間
          pnlInterpolation = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
          break;
        case "Bilinear":  //線形補間
          pnlInterpolation = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
          break;
        case "Bicubic":  //三次補間
          pnlInterpolation = RenderingHints.VALUE_INTERPOLATION_BICUBIC;
          break;
          //
        case "Use canvas":  //キャンバスを使う
          pnlUseCanvasRequest = ((JCheckBoxMenuItem) source).isSelected ();
          break;
          //
        case "Draw all changed pictures":  //変化した画像をすべて描画する
          if (CRTC.CRT_ENABLE_INTERMITTENT) {
            CRTC.crtIntermittentInterval = 0;
          }
          break;
        case "Draw a changed picture once every two times":  //変化した画像を 2 回に 1 回描画する
          if (CRTC.CRT_ENABLE_INTERMITTENT) {
            CRTC.crtIntermittentInterval = 1;
          }
          break;
        case "Draw a changed picture once every three times":  //変化した画像を 3 回に 1 回描画する
          if (CRTC.CRT_ENABLE_INTERMITTENT) {
            CRTC.crtIntermittentInterval = 2;
          }
          break;
        case "Draw a changed picture once every four times":  //変化した画像を 4 回に 1 回描画する
          if (CRTC.CRT_ENABLE_INTERMITTENT) {
            CRTC.crtIntermittentInterval = 3;
          }
          break;
        case "Draw a changed picture once every five times":  //変化した画像を 5 回に 1 回描画する
          if (CRTC.CRT_ENABLE_INTERMITTENT) {
            CRTC.crtIntermittentInterval = 4;
          }
          break;
          //
        case "Stereoscopic viewing":  //立体視
          pnlSetStereoscopic (((JCheckBoxMenuItem) source).isSelected (), pnlStereoscopicMethod);
          break;
        case "Naked-eye crossing":  //裸眼交差法
          pnlSetStereoscopic (pnlStereoscopicOn, PNL_NAKED_EYE_CROSSING);
          break;
        case "Naked-eye parallel":  //裸眼平行法
          pnlSetStereoscopic (pnlStereoscopicOn, PNL_NAKED_EYE_PARALLEL);
          break;
        case "Side-by-side":  //サイドバイサイド
          pnlSetStereoscopic (pnlStereoscopicOn, PNL_SIDE_BY_SIDE);
          break;
        case "Top-and-bottom":  //トップアンドボトム
          pnlSetStereoscopic (pnlStereoscopicOn, PNL_TOP_AND_BOTTOM);
          break;
          //
        case "Sprite pattern viewer":  //スプライトパターンビュア
          if (SpritePatternViewer.SPV_ON) {
            SpritePatternViewer.spvOpen ();
          }
          break;
        case "Palette viewer":  //パレットビュア
          if (PaletteViewer.PLV_ON) {
            PaletteViewer.plvOpen ();
          }
          break;
        case "Screen mode test":  //表示モードテスト
          if (ScreenModeTest.SMT_ON) {
            ScreenModeTest.smtOpen ();
          }
          break;

          //音声メニュー
        case "Play":  //音声出力
          SoundSource.sndSetPlayOn (((JCheckBoxMenuItem) source).isSelected ());
          break;
        case "OPM output":  //OPM出力
          OPM.opmSetOutputOn (((JCheckBoxMenuItem) source).isSelected ());
          break;
        case "OPM log":  //OPM ログ
          OPMLog.olgOpen ();
          break;
          //
        case "PCM output":  //PCM出力
          ADPCM.pcmSetOutputOn (((JCheckBoxMenuItem) source).isSelected ());
          break;
        case "Sound thinning":  //音声 間引き
          SoundSource.sndRateConverter = SoundSource.SND_CHANNELS == 1 ? SoundSource.SNDRateConverter.THINNING_MONO : SoundSource.SNDRateConverter.THINNING_STEREO;
          break;
        case "Sound linear interpolation":  //音声 線形補間
          SoundSource.sndRateConverter = SoundSource.SND_CHANNELS == 1 ? SoundSource.SNDRateConverter.LINEAR_MONO : SoundSource.SNDRateConverter.LINEAR_STEREO;
          break;
        case "Sound piecewise-constant area interpolation":  //音声 区分定数面積補間
          SoundSource.sndRateConverter = SoundSource.SNDRateConverter.CONSTANT_AREA_STEREO_48000;
          break;
        case "Sound linear area interpolation":  //音声 線形面積補間
          SoundSource.sndRateConverter = SoundSource.SNDRateConverter.LINEAR_AREA_STEREO_48000;
          break;
        case "Sound monitor":  //音声モニタ
          SoundMonitor.smnOpen ();
          break;
        case "PCM piecewise-constant interpolation":  //PCM 区分定数補間
          ADPCM.pcmSetInterpolationAlgorithm (ADPCM.PCM_INTERPOLATION_CONSTANT);
          break;
        case "PCM linear interpolation":  //PCM 線形補間
          ADPCM.pcmSetInterpolationAlgorithm (ADPCM.PCM_INTERPOLATION_LINEAR);
          break;
        case "PCM hermite interpolation":  //PCM エルミート補間
          ADPCM.pcmSetInterpolationAlgorithm (ADPCM.PCM_INTERPOLATION_HERMITE);
          break;
        case "PCM 8MHz/4MHz":
          ADPCM.pcmOSCFreqRequest = 0;
          break;
        case "PCM 8MHz/16MHz":
          ADPCM.pcmOSCFreqRequest = 1;
          break;
        case "Mercury-Unit V4 (MK-MU1)":
          MercuryUnit.mu4OnRequest = ((JCheckBoxMenuItem) source).isSelected ();
          break;
        case "MU4 output":
          MercuryUnit.mu4OutputEnable = ((JCheckBoxMenuItem) source).isSelected ();
          break;

          //入力メニュー
        case "Paste":  //貼り付け
          CONDevice.conDoPaste ();
          break;
        case "No keyboard":  //キーボードなし
          Keyboard.kbdSetOn (false);
          pnlUpdateArrangement ();
          break;
        case "Standard keyboard":  //標準キーボード
          Keyboard.kbdSetType (Keyboard.KBD_STANDARD_TYPE);
          Keyboard.kbdSetOn (true);
          pnlUpdateArrangement ();
          break;
        case "Compact keyboard":  //コンパクトキーボード
          Keyboard.kbdSetType (Keyboard.KBD_COMPACT_TYPE);
          Keyboard.kbdSetOn (true);
          pnlUpdateArrangement ();
          break;
        case "Hide keyboard in full screen":  //全画面表示のときキーボードを隠す
          pnlHideKeyboard = ((JCheckBoxMenuItem) source).isSelected ();
          if (pnlMode == PNL_FULLSCREEN) {
            pnlUpdateArrangement ();
          }
          break;
        case "Key assignments":  //キー割り当て
          Keyboard.kbdOpen ();
          break;
        case "Joystick port settings":  //ジョイスティックポート設定
          PPI.ppiOpen ();
          break;

          //設定メニュー
        case "RS-232C and terminal":  //RS-232C とターミナル
          RS232CTerminal.trmOpen ();
          break;
        case "Malfunction screen (experimental)":  //故障画面 (実験中)
          if (Malfunction.MLF_ON) {
            Malfunction.mlfOpen ();
          }
          break;
          //  デバッグ
        case "Console":  //コンソール
          DebugConsole.dgtOpen ();
          break;
        case "Register list":  //レジスタ
          RegisterList.drpOpen ();
          break;
        case "Disassemble list":  //逆アセンブルリスト
          DisassembleList.ddpOpen (-1, -1, true);
          break;
        case "Memory dump list":  //メモリダンプリスト
          MemoryDumpList.dmpOpen (-1, -1, true);
          break;
        case "Logical space monitor":  //論理空間モニタ
          LogicalSpaceMonitor.atwOpen ();
          break;
        case "Physical space monitor":  //物理空間モニタ
          PhysicalSpaceMonitor.paaOpen ();
          break;
        case "Address translation caches monitor":  //アドレス変換キャッシュモニタ
          if (ATCMonitor.ACM_ON) {
            ATCMonitor.acmOpen ();
          }
          break;
        case "Branch log":  //分岐ログ
          if (BranchLog.BLG_ON) {
            BranchLog.blgOpen (BranchLog.BLG_SELECT_NONE);
          }
          break;
        case "Program flow visualizer":  //プログラムフロービジュアライザ
          if (ProgramFlowVisualizer.PFV_ON) {
            ProgramFlowVisualizer.pfvOpen ();
          }
          break;
        case "Raster break point":  //ラスタブレークポイント
          if (RasterBreakPoint.RBP_ON) {
            RasterBreakPoint.rbpOpen ();
          }
          break;
        case "Data break point":  //データブレークポイント
          if (DataBreakPoint.DBP_ON) {
            DataBreakPoint.dbpOpen ();
          }
          break;
        case "Root pointer list":  //ルートポインタリスト
          if (RootPointerList.RTL_ON) {
            RootPointerList.rtlOpen ();
          }
          break;
          //  起動デバイス
          //  RTC
        case "Adjust clock to host":  //時計をホストに合わせる
          RP5C15.rtcSetByHost ();
          break;
          //  SRAM
          //  設定ファイル
          //
        case "Printer":  //プリンタ
          PrinterPort.prnOpen ();
          break;
          //
        case "Print key and mouse button events":  //キーとマウスのボタンのイベントを表示
          Mouse.musOutputButtonStatus = ((JCheckBoxMenuItem) source).isSelected ();
          break;
          //
        case "Java runtime environment information":
          prgOpenJavaDialog ();
          break;
        case "Version information":
          prgOpenAboutDialog ();
          break;
        case "XEiJ License":
          prgOpenXEiJLicenseDialog ();
          break;
        case "FSHARP License":
          prgOpenSHARPLicenseDialog ();
          break;
        case "ymfm License":
          prgOpenYmfmLicenseDialog ();
          break;
        case "jSerialComm License":
          prgOpenJSerialCommLicenseDialog ();
          break;

        default:
          System.out.println ("unknown action command " + command);

        }
      }
    };

    //  メインメモリ
    ActionListener mainMemoryListener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        String command = ae.getActionCommand ();
        switch (command) {
        case "1MB":
          MainMemory.mmrMemorySizeRequest = 0x00100000;
          break;
        case "2MB":
          MainMemory.mmrMemorySizeRequest = 0x00200000;
          break;
        case "4MB":
          MainMemory.mmrMemorySizeRequest = 0x00400000;
          break;
        case "6MB":
          MainMemory.mmrMemorySizeRequest = 0x00600000;
          break;
        case "8MB":
          MainMemory.mmrMemorySizeRequest = 0x00800000;
          break;
        case "10MB":
          MainMemory.mmrMemorySizeRequest = 0x00a00000;
          break;
        case "12MB":
          MainMemory.mmrMemorySizeRequest = 0x00c00000;
          break;
        case "Save contents on exit":  //終了時に内容を保存する
          MainMemory.mmrMemorySaveOn = ((JCheckBoxMenuItem) source).isSelected ();
          break;
        }
      }
    };
    ButtonGroup mainMemoryGroup = new ButtonGroup ();
    JMenu mainMemoryMenu = Multilingual.mlnText (
      ComponentFactory.createMenu (
        "Main memory",
        Multilingual.mlnText (
          ComponentFactory.createRadioButtonMenuItem (mainMemoryGroup, MainMemory.mmrMemorySizeRequest == 0x00100000, "1MB", mainMemoryListener),
          "ja", "1MB"),
        Multilingual.mlnText (
          ComponentFactory.createRadioButtonMenuItem (mainMemoryGroup, MainMemory.mmrMemorySizeRequest == 0x00200000, "2MB", mainMemoryListener),
          "ja", "2MB"),
        Multilingual.mlnText (
          ComponentFactory.createRadioButtonMenuItem (mainMemoryGroup, MainMemory.mmrMemorySizeRequest == 0x00400000, "4MB", mainMemoryListener),
          "ja", "4MB"),
        Multilingual.mlnText (
          ComponentFactory.createRadioButtonMenuItem (mainMemoryGroup, MainMemory.mmrMemorySizeRequest == 0x00600000, "6MB", mainMemoryListener),
          "ja", "6MB"),
        Multilingual.mlnText (
          ComponentFactory.createRadioButtonMenuItem (mainMemoryGroup, MainMemory.mmrMemorySizeRequest == 0x00800000, "8MB", mainMemoryListener),
          "ja", "8MB"),
        Multilingual.mlnText (
          ComponentFactory.createRadioButtonMenuItem (mainMemoryGroup, MainMemory.mmrMemorySizeRequest == 0x00a00000, "10MB", mainMemoryListener),
          "ja", "10MB"),
        Multilingual.mlnText (
          ComponentFactory.createRadioButtonMenuItem (mainMemoryGroup, MainMemory.mmrMemorySizeRequest == 0x00c00000, "12MB", mainMemoryListener),
          "ja", "12MB"),
        ComponentFactory.createHorizontalSeparator (),
        Multilingual.mlnText (
          ComponentFactory.createCheckBoxMenuItem (MainMemory.mmrMemorySaveOn, "Save contents on exit", mainMemoryListener),
          "ja", "終了時に内容を保存する"),
        SRAM.smrModifyMemorySizeMenuItem
        ),
      "ja", "メインメモリ");

    //  X68030/Xellent30 のハイメモリ
    ActionListener highMemoryListener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        String command = ae.getActionCommand ();
        switch (command) {
        case "None":  //なし
          busHighMemorySize = 0 << 20;
          break;
        case "16MB":
          busHighMemorySize = 16 << 20;
          break;
        case "Save contents on exit":  //終了時に内容を保存する
          busHighMemorySaveOn = ((JCheckBoxMenuItem) source).isSelected ();
          break;
        }
      }
    };
    ButtonGroup highMemoryGroup = new ButtonGroup ();
    JMenu highMemoryMenu = Multilingual.mlnText (
      ComponentFactory.createMenu (
        "High memory on X68030/Xellent30",
        Multilingual.mlnText (
          ComponentFactory.createRadioButtonMenuItem (highMemoryGroup, busHighMemorySize == 0 << 20, "None", highMemoryListener),
          "ja", "なし"),
        Multilingual.mlnText (
          ComponentFactory.createRadioButtonMenuItem (highMemoryGroup, busHighMemorySize == 16 << 20, "16MB", highMemoryListener),
          "ja", "16MB"),
        ComponentFactory.createHorizontalSeparator (),
        Multilingual.mlnText (
          ComponentFactory.createCheckBoxMenuItem (busHighMemorySaveOn, "Save contents on exit", highMemoryListener),
          "ja", "終了時に内容を保存する")
        ),
      "ja", "X68030/Xellent30 のハイメモリ");

    //  060turbo のハイメモリ
    ActionListener localMemoryListener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        String command = ae.getActionCommand ();
        switch (command) {
        case "None":  //なし
          busLocalMemorySize = 0 << 20;
          break;
        case "16MB":
          busLocalMemorySize = 16 << 20;
          break;
        case "32MB":
          busLocalMemorySize = 32 << 20;
          break;
        case "64MB":
          busLocalMemorySize = 64 << 20;
          break;
        case "128MB":
          busLocalMemorySize = 128 << 20;
          break;
        case "256MB":
          busLocalMemorySize = 256 << 20;
          break;
        case "384MB":
          busLocalMemorySize = 384 << 20;
          break;
        case "512MB":
          busLocalMemorySize = 512 << 20;
          break;
        case "768MB":
          busLocalMemorySize = 768 << 20;
          break;
        case "Save contents on exit":  //終了時に内容を保存する
          busLocalMemorySaveOn = ((JCheckBoxMenuItem) source).isSelected ();
          break;
        case "Available on X68000":  //X68000 でも有効
          busHimem68000 = ((JCheckBoxMenuItem) source).isSelected ();
          break;
        case "Available on X68030/Xellent30":  //X68030/Xellent30 でも有効
          busHighMemory060turboOn = ((JCheckBoxMenuItem) source).isSelected ();
          break;
        }
      }
    };
    ButtonGroup localMenoryGroup = new ButtonGroup ();
    JMenu localMemoryMenu = Multilingual.mlnText (
      ComponentFactory.createMenu (
        "High memory on 060turbo",
        Multilingual.mlnText (
          ComponentFactory.createRadioButtonMenuItem (localMenoryGroup, busLocalMemorySize == 0 << 20, "None", localMemoryListener),
          "ja", "なし"),
        ComponentFactory.createRadioButtonMenuItem (localMenoryGroup, busLocalMemorySize == 16 << 20, "16MB", localMemoryListener),
        ComponentFactory.createRadioButtonMenuItem (localMenoryGroup, busLocalMemorySize == 32 << 20, "32MB", localMemoryListener),
        ComponentFactory.createRadioButtonMenuItem (localMenoryGroup, busLocalMemorySize == 64 << 20, "64MB", localMemoryListener),
        ComponentFactory.createRadioButtonMenuItem (localMenoryGroup, busLocalMemorySize == 128 << 20, "128MB", localMemoryListener),
        ComponentFactory.createRadioButtonMenuItem (localMenoryGroup, busLocalMemorySize == 256 << 20, "256MB", localMemoryListener),
        ComponentFactory.createRadioButtonMenuItem (localMenoryGroup, busLocalMemorySize == 384 << 20, "384MB", localMemoryListener),
        ComponentFactory.createRadioButtonMenuItem (localMenoryGroup, busLocalMemorySize == 512 << 20, "512MB", localMemoryListener),
        ComponentFactory.createRadioButtonMenuItem (localMenoryGroup, busLocalMemorySize == 768 << 20, "768MB", localMemoryListener),
        ComponentFactory.createHorizontalSeparator (),
        Multilingual.mlnText (
          ComponentFactory.createCheckBoxMenuItem (busLocalMemorySaveOn, "Save contents on exit", localMemoryListener),
          "ja", "終了時に内容を保存する"),
        ComponentFactory.createHorizontalSeparator (),
        Multilingual.mlnText (
          ComponentFactory.createCheckBoxMenuItem (busHimem68000, "Available on X68000", localMemoryListener),
          "ja", "X68000 でも有効"),
        Multilingual.mlnText (
          ComponentFactory.createCheckBoxMenuItem (busHighMemory060turboOn, "Available on X68030/Xellent30", localMemoryListener),
          "ja", "X68030/Xellent30 でも有効")
        ),
      "ja", "060turbo のハイメモリ");

    //Xellent30
    ActionListener xellent30Listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        String command = ae.getActionCommand ();
        switch (command) {
        case "$00EC0000-$00EC3FFF":
          xt3DIPSW = 0;
          break;
        case "$00EC4000-$00EC7FFF":
          xt3DIPSW = 1;
          break;
        case "$00EC8000-$00ECBFFF":
          xt3DIPSW = 2;
          break;
        case "$00ECC000-$00ECFFFF":
          xt3DIPSW = 3;
          break;
        case "256KB":
          xt3MemorySizeRequest = 1 << 18;
          break;
        case "1MB":
          xt3MemorySizeRequest = 1 << 20;
          break;
        case "Save contents on exit":  //終了時に内容を保存する
          xt3MemorySave = ((JCheckBoxMenuItem) source).isSelected ();
          break;
        }
      }
    };
    ButtonGroup xellent30PortGroup = new ButtonGroup ();
    ButtonGroup xellent30SizeGroup = new ButtonGroup ();
    JMenu xellent30Menu = ComponentFactory.createMenu (
      "Xellent30",
      ComponentFactory.createRadioButtonMenuItem (
        xellent30PortGroup,
        xt3DIPSW == 0,
        "$00EC0000-$00EC3FFF",
        xellent30Listener),
      ComponentFactory.createRadioButtonMenuItem (
        xellent30PortGroup,
        xt3DIPSW == 1,
        "$00EC4000-$00EC7FFF",
        xellent30Listener),
      ComponentFactory.createRadioButtonMenuItem (
        xellent30PortGroup,
        xt3DIPSW == 2,
        "$00EC8000-$00ECBFFF",
        xellent30Listener),
      ComponentFactory.createRadioButtonMenuItem (
        xellent30PortGroup,
        xt3DIPSW == 3,
        "$00ECC000-$00ECFFFF",
        xellent30Listener),
      ComponentFactory.createHorizontalSeparator (),
      ComponentFactory.createRadioButtonMenuItem (
        xellent30SizeGroup,
        xt3MemorySizeRequest == 1 << 18,
        "256KB",
        xellent30Listener),
      ComponentFactory.createRadioButtonMenuItem (
        xellent30SizeGroup,
        xt3MemorySizeRequest == 1 << 20,
        "1MB",
        xellent30Listener),
      ComponentFactory.createHorizontalSeparator (),
      Multilingual.mlnText (
        ComponentFactory.createCheckBoxMenuItem (xt3MemorySave, "Save contents on exit", xellent30Listener),
        "ja", "終了時に内容を保存する")
      );

    //回転
    JMenu rotationMenu = null;
    if (PNL_ROTATION_ON) {
      ActionListener rotationListener = new ActionListener () {
        @Override public void actionPerformed (ActionEvent ae) {
          String command = ae.getActionCommand ();
          switch (command) {
          case "No rotation (landscape)":  //回転なし (横画面)
            pnlRotationMode = 0;
            pnlUpdateArrangement ();
            break;
          case "90-degree rotation (portrait)":  //90 度回転 (縦画面)
            pnlRotationMode = 1;
            pnlUpdateArrangement ();
            break;
          case "180-degree rotation":  //180 度回転
            pnlRotationMode = 2;
            pnlUpdateArrangement ();
            break;
          case "270-degree rotation":  //270 度回転
            pnlRotationMode = 3;
            pnlUpdateArrangement ();
            break;
          default:
            System.out.println ("unknown action command " + command);
          }
        }
      };
      ButtonGroup rotationGroup = new ButtonGroup ();
      rotationMenu = Multilingual.mlnText (
        ComponentFactory.createMenu (
          "Rotation",
          Multilingual.mlnText (
            ComponentFactory.createRadioButtonMenuItem (rotationGroup, pnlRotationMode == 0, "No rotation (landscape)", rotationListener),
            "ja", "回転なし (横画面)"),
          Multilingual.mlnText (
            ComponentFactory.createRadioButtonMenuItem (rotationGroup, pnlRotationMode == 1, "90-degree rotation (portrait)", rotationListener),
            "ja", "90 度回転 (縦画面)"),
          Multilingual.mlnText (
            ComponentFactory.createRadioButtonMenuItem (rotationGroup, pnlRotationMode == 2, "180-degree rotation", rotationListener),
            "ja", "180 度回転"),
          Multilingual.mlnText (
            ComponentFactory.createRadioButtonMenuItem (rotationGroup, pnlRotationMode == 3, "270-degree rotation", rotationListener),
            "ja", "270 度回転")
          ),
        "ja", "回転");
    }

    //アスペクト比
    ActionListener aspectListener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        String command = ae.getActionCommand ();
        int i = command.indexOf (',');
        int resolutionNumber = Integer.parseInt (command.substring (0, i));
        int ratioNumber = Integer.parseInt (command.substring (i + 1));
        pnlAspectMap[resolutionNumber] = ratioNumber;
        pnlUpdateAspectTable ();
        pnlUpdateArrangement ();
      }  //actionPerformed
    };  //new ActionListener
    JMenu aspectMenu = ComponentFactory.createMenu ("Aspect ratio");
    for (int resolutionNumber = 0; resolutionNumber < PNL_ASPECT_RESOLUTION_COUNT; resolutionNumber++) {
      JMenu menu = ComponentFactory.createMenu (PNL_ASPECT_RESOLUTION_NAME[resolutionNumber]);
      ButtonGroup group = new ButtonGroup ();
      for (int ratioNumber = 0; ratioNumber < PNL_ASPECT_RATIO_COUNT; ratioNumber++) {
        if (PNL_ASPECT_RATIO_MATRIX[resolutionNumber][ratioNumber] == 0.0F) {
          continue;
        }
        menu.add (
          ComponentFactory.setText (
            ComponentFactory.createRadioButtonMenuItem (
              group,  //buttonGroup
              pnlAspectMap[resolutionNumber] == ratioNumber,  //selected
              resolutionNumber + "," + ratioNumber,  //actionCommand
              aspectListener  //actionListener
              ),
            (PNL_ASPECT_RATIO_MATRIX[resolutionNumber] == PNL_ASPECT_PIXEL_RATIO_02 ?
             String.format ("%s (%.3f)",
                            PNL_ASPECT_SCREEN_NAME[ratioNumber],
                            PNL_ASPECT_SCREEN_RATIO[ratioNumber]) :
             String.format ("%s (%.3f) @ %s (%.3f)",
                            PNL_ASPECT_SCREEN_NAME[ratioNumber],
                            PNL_ASPECT_SCREEN_RATIO[ratioNumber],
                            PNL_ASPECT_NAME_MATRIX[resolutionNumber][ratioNumber],
                            PNL_ASPECT_RATIO_MATRIX[resolutionNumber][ratioNumber]))  //text
            )  //setText
          );  //add
      }  //for ratioNumber
      aspectMenu.add (menu);
    }  //for resolutionNumber
    aspectMenu = Multilingual.mlnText (aspectMenu, "ja", "アスペクト比");

    //走査線エフェクト
    ActionListener scanlineListener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        //Object source = ae.getSource ();
        String command = ae.getActionCommand ();
        switch (command) {
        case "Off":  //なし
          CRTC.crtScanlineEffect = CRTC.ScanlineEffect.OFF;
          CRTC.crtAllStamp += 2;
          break;
        case "Weak":  //弱
          CRTC.crtScanlineEffect = CRTC.ScanlineEffect.WEAK;
          CRTC.crtAllStamp += 2;
          break;
        case "Medium":  //中
          CRTC.crtScanlineEffect = CRTC.ScanlineEffect.MEDIUM;
          CRTC.crtAllStamp += 2;
          break;
        case "Strong":  //強
          CRTC.crtScanlineEffect = CRTC.ScanlineEffect.STRONG;
          CRTC.crtAllStamp += 2;
          break;
        case "Black":  //黒
          CRTC.crtScanlineEffect = CRTC.ScanlineEffect.BLACK;
          CRTC.crtAllStamp += 2;
          break;
        }
      }
    };
    ButtonGroup scanlineGroup = new ButtonGroup ();
    JMenu scanlineMenu =
      Multilingual.mlnText (
        ComponentFactory.createMenu (
          "Scanline effect",
          Multilingual.mlnText (
            ComponentFactory.createRadioButtonMenuItem (scanlineGroup, CRTC.crtScanlineEffect == CRTC.ScanlineEffect.OFF, "Off", scanlineListener),
            "ja", "なし"),
          Multilingual.mlnText (
            ComponentFactory.createRadioButtonMenuItem (scanlineGroup, CRTC.crtScanlineEffect == CRTC.ScanlineEffect.WEAK, "Weak", scanlineListener),
            "ja", "弱"),
          Multilingual.mlnText (
            ComponentFactory.createRadioButtonMenuItem (scanlineGroup, CRTC.crtScanlineEffect == CRTC.ScanlineEffect.MEDIUM, "Medium", scanlineListener),
            "ja", "中"),
          Multilingual.mlnText (
            ComponentFactory.createRadioButtonMenuItem (scanlineGroup, CRTC.crtScanlineEffect == CRTC.ScanlineEffect.STRONG, "Strong", scanlineListener),
            "ja", "強"),
          Multilingual.mlnText (
            ComponentFactory.createRadioButtonMenuItem (scanlineGroup, CRTC.crtScanlineEffect == CRTC.ScanlineEffect.BLACK, "Black", scanlineListener),
            "ja", "黒")
          ),
        "ja", "走査線エフェクト");

    //リフレッシュレート
    JTextField refreshRateTextField = ComponentFactory.createNumberField (pnlRefreshRate == 0.0 ? "" : String.valueOf (pnlRefreshRate), 8);
    refreshRateTextField.addActionListener (
      new ActionListener () {
        @Override public void actionPerformed (ActionEvent ae) {
          JTextField textField = (JTextField) ae.getSource ();
          pnlRefreshRate = 0.0;
          String s = textField.getText ();
          if (!s.equals ("")) {
            double rate = 0.0;
            try {
              rate = Double.parseDouble (s);
            } catch (NumberFormatException nfe) {
            }
            if (PNL_MIN_RATE <= rate && rate <= PNL_MAX_RATE) {
              pnlRefreshRate = rate;
            } else {
              textField.setText ("");  //""でないがパースできないかできても範囲外のとき""に戻す
            }
          }
          pnlFixedRate = pnlRefreshRate;  //指定がないか無効のとき0.0
          if (pnlAdjustVsync && pnlFixedRate == 0.0) {
            pnlFixedRate = pnlGetRefreshRate ();  //なければデフォルトになるので0.0ではなくなる
          }
          CRTC.crtUpdateLength ();
        }
      });

    ButtonGroup unitGroup = new ButtonGroup ();
    ButtonGroup frameGroup = new ButtonGroup ();
    ButtonGroup hintGroup = new ButtonGroup ();
    ButtonGroup vgaGroup = new ButtonGroup ();
    ButtonGroup intermittentGroup = new ButtonGroup ();
    ButtonGroup sterescopicGroup = new ButtonGroup ();
    ButtonGroup soundInterpolationGroup = new ButtonGroup ();
    ButtonGroup adpcmInterpolationGroup = new ButtonGroup ();
    ButtonGroup adpcmOSCFreqGroup = new ButtonGroup ();
    ButtonGroup keyboardGroup = new ButtonGroup ();
    ButtonGroup spritesGroup = new ButtonGroup ();

    //改造
    DecimalSpinner[] freqSpinner = new DecimalSpinner[3];
    ChangeListener freqListener = new ChangeListener () {
      @Override public void stateChanged (ChangeEvent ce) {
        DecimalSpinner spinner = (DecimalSpinner) ce.getSource ();
        int i = spinner.getOption ();
        CRTC.crtFreqsRequest[i] = spinner.getIntValue ();
      }
    };
    for (int i = 0; i < 3; i++) {
      freqSpinner[i] = ComponentFactory.createDecimalSpinner (
        CRTC.crtFreqsRequest[i], CRTC.CRT_MIN_FREQ, CRTC.CRT_MAX_FREQ, 1000000, i, freqListener
        );
    }
    DecimalSpinner sprrasSpinner = ComponentFactory.createDecimalSpinner (
      SpriteScreen.sprSpritesPerRaster, 0, /*2040*/1016, 1, 0,
      new ChangeListener () {
        @Override public void stateChanged (ChangeEvent ce) {
          SpriteScreen.sprSpritesPerRaster = ((DecimalSpinner) ce.getSource ()).getIntValue ();
        }
      });
    ActionListener modificationListener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        String command = ae.getActionCommand ();
        switch (command) {
          //ドットクロック
        case "Adjust to host refresh rate":  //ホストのリフレッシュレートに合わせる
          pnlAdjustVsync = ((JCheckBoxMenuItem) source).isSelected ();
          if (pnlAdjustVsync && pnlFixedRate == 0.0) {  //必要だが指定がなくて未取得
            pnlFixedRate = pnlGetRefreshRate ();
          }
          CRTC.crtUpdateLength ();
          break;
        case "* Reset to default values":  // * 初期値に戻す
          for (int i = 0; i < 3; i++) {
            if (CRTC.crtFreqsRequest[i] != CRTC.CRT_DEFAULT_FREQS[i]) {
              CRTC.crtFreqsRequest[i] = CRTC.CRT_DEFAULT_FREQS[i];
              freqSpinner[i].setIntValue (CRTC.crtFreqsRequest[i]);
            }
          }
          break;
        case "1024-dot non-interlaced":  //1024 ドットノンインターレース
          CRTC.crtEleventhBitRequest = ((JCheckBoxMenuItem) source).isSelected ();
          break;
        case "Can write 0 to bit 0 of CRTC R00":  //CRTC R00 のビット 0 に 0 を書き込める
          CRTC.crtR00Bit0Zero = ((JCheckBoxMenuItem) source).isSelected ();
          break;
          //グラフィック画面
        case "Extended graphic screen":  //拡張グラフィック画面
          CRTC.crtExtendedGraphicRequest = ((JCheckBoxMenuItem) source).isSelected ();
          break;
          //テキスト画面
        case "Spherical scrolling of text screen":  //テキスト画面の球面スクロール
          CRTC.crtSetSphericalScrolling (((JCheckBoxMenuItem) source).isSelected ());
          break;
          //スプライト画面
        case "128 sprites":  //128 枚のスプライト
          SpriteScreen.sprNumberOfSpritesRequest = 128;
          break;
        case "256 sprites":  //256 枚のスプライト
          SpriteScreen.sprNumberOfSpritesRequest = 256;
          break;
        case "504 sprites":  //504 枚のスプライト
          SpriteScreen.sprNumberOfSpritesRequest = 512;
          break;
        case "1016 sprites":  //1016 枚のスプライト
          SpriteScreen.sprNumberOfSpritesRequest = 1024;
          break;
          //case "2040 sprites":  //2040 枚のスプライト
          //  SpriteScreen.sprNumberOfSpritesRequest = 2048;
          //  break;
        case "Full pattern memory":  //フルパターンメモリ
          SpriteScreen.sprFPMRequest = ((JCheckBoxMenuItem) source).isSelected ();
          break;
        case "4096 patterns":  //4096 個のパターン
          SpriteScreen.sprBankOnRequest = ((JCheckBoxMenuItem) source).isSelected ();
          break;
        case "Sprites displayed in 768x512":  //768x512 でスプライトを表示
          SpriteScreen.spr768x512Request = ((JCheckBoxMenuItem) source).isSelected ();
          break;
        case "BG1 displayed in 512x512":  //512x512 で BG1 を表示
          SpriteScreen.spr512bg1Request = ((JCheckBoxMenuItem) source).isSelected ();
          break;
        case "Row and column scroll":  //行スクロールと列スクロール
          if (SpriteScreen.SPR_RC_SCROLL_ON) {
            SpriteScreen.sprRcScrollRequest = ((JCheckBoxMenuItem) source).isSelected ();
          }
          break;
        case "** Reset to default values":  // ** 初期値に戻す
          if (SpriteScreen.sprSpritesPerRaster != 32) {
            SpriteScreen.sprSpritesPerRaster = 32;
            sprrasSpinner.setIntValue (SpriteScreen.sprSpritesPerRaster);
          }
          break;
        }
      }
    };
    JMenu modificationMenu =
      Multilingual.mlnText (
        ComponentFactory.createMenu (
          "Modification",
          Multilingual.mlnText (
            ComponentFactory.createMenu (
              "Dot clock",
              Multilingual.mlnText (
                ComponentFactory.createCheckBoxMenuItem (pnlAdjustVsync, "Adjust to host refresh rate", modificationListener),
                "ja", "ホストのリフレッシュレートに合わせる"),
              ComponentFactory.createHorizontalBox (
                Box.createHorizontalStrut (20),
                refreshRateTextField,
                ComponentFactory.createLabel (" Hz"),
                Box.createHorizontalGlue ()
                ),
              ComponentFactory.createHorizontalSeparator (),
              ComponentFactory.createHorizontalBox (
                Box.createHorizontalStrut (20),
                Multilingual.mlnText (ComponentFactory.createLabel ("Dot clock oscillattor"), "ja", "ドットクロックオシレータ"),
                Box.createHorizontalGlue ()
                ),
              ComponentFactory.createHorizontalBox (
                Box.createHorizontalStrut (20),
                freqSpinner[0],
                ComponentFactory.createLabel (" Hz *"),
                Box.createHorizontalGlue ()
                ),
              ComponentFactory.createHorizontalBox (
                Box.createHorizontalStrut (20),
                freqSpinner[1],
                ComponentFactory.createLabel (" Hz *"),
                Box.createHorizontalGlue ()
                ),
              ComponentFactory.createHorizontalBox (
                Box.createHorizontalStrut (20),
                freqSpinner[2],
                ComponentFactory.createLabel (" Hz *"),
                Box.createHorizontalGlue ()
                ),
              Multilingual.mlnText (
                ComponentFactory.createMenuItem ("* Reset to default values", modificationListener),
                "ja", "* 初期値に戻す"),
              ComponentFactory.createHorizontalSeparator (),
              Multilingual.mlnText (
                ComponentFactory.createCheckBoxMenuItem (CRTC.crtEleventhBitRequest, "1024-dot non-interlaced", modificationListener),
                "ja", "1024 ドットノンインターレース"),
              Multilingual.mlnText (
                ComponentFactory.createCheckBoxMenuItem (CRTC.crtR00Bit0Zero, "Can write 0 to bit 0 of CRTC R00", modificationListener),
                "ja", "CRTC R00 のビット 0 に 0 を書き込める")
              ),  //CRTC
            "ja", "ドットクロック"),
          Multilingual.mlnText (
            ComponentFactory.createMenu (
              "Graphic screen",
              !CRTC.CRT_EXTENDED_GRAPHIC ? null : Multilingual.mlnText (
                ComponentFactory.createCheckBoxMenuItem (CRTC.crtExtendedGraphicRequest, "Extended graphic screen", modificationListener),
                "ja", "拡張グラフィック画面")
              ),  //Graphic screen
            "ja", "グラフィック画面"),
          Multilingual.mlnText (
            ComponentFactory.createMenu (
              "Text screen",
              Multilingual.mlnText (
                ComponentFactory.createCheckBoxMenuItem (CRTC.crtSphericalScrolling, "Spherical scrolling of text screen", modificationListener),
                "ja", "テキスト画面の球面スクロール")
              ),  //Text screen
            "ja", "テキスト画面"),
          Multilingual.mlnText (
            ComponentFactory.createMenu (
              "Sprite screen",
              Multilingual.mlnText (
                ComponentFactory.createRadioButtonMenuItem (
                  spritesGroup, SpriteScreen.sprNumberOfSpritesRequest == 128, "128 sprites", modificationListener),
                "ja", "128 枚のスプライト"),
              Multilingual.mlnText (
                ComponentFactory.createRadioButtonMenuItem (
                  spritesGroup, SpriteScreen.sprNumberOfSpritesRequest == 256, "256 sprites", modificationListener),
                "ja", "256 枚のスプライト"),
              Multilingual.mlnText (
                ComponentFactory.createRadioButtonMenuItem (
                  spritesGroup, SpriteScreen.sprNumberOfSpritesRequest == 512, "504 sprites", modificationListener),
                "ja", "504 枚のスプライト"),
              Multilingual.mlnText (
                ComponentFactory.createRadioButtonMenuItem (
                  spritesGroup, SpriteScreen.sprNumberOfSpritesRequest == 1024, "1016 sprites", modificationListener),
                "ja", "1016 枚のスプライト"),
              //Multilingual.mlnText (
              //  ComponentFactory.createRadioButtonMenuItem (
              //    spritesGroup, SpriteScreen.sprNumberOfSpritesRequest == 2048, "2040 sprites", modificationListener),
              //  "ja", "2040 枚のスプライト"),
              ComponentFactory.createHorizontalSeparator (),
              Multilingual.mlnText (
                ComponentFactory.createCheckBoxMenuItem (SpriteScreen.sprFPMRequest, "Full pattern memory", modificationListener),
                "ja", "フルパターンメモリ"),
              ComponentFactory.createHorizontalSeparator (),
              Multilingual.mlnText (
                ComponentFactory.createCheckBoxMenuItem (SpriteScreen.sprBankOnRequest, "4096 patterns", modificationListener),
                "ja", "4096 個のパターン"),
              Multilingual.mlnText (
                ComponentFactory.createCheckBoxMenuItem (SpriteScreen.spr768x512Request, "Sprites displayed in 768x512", modificationListener),
                "ja", "768x512 でスプライトを表示"),
              Multilingual.mlnText (
                ComponentFactory.createCheckBoxMenuItem (SpriteScreen.spr512bg1Request, "BG1 displayed in 512x512", modificationListener),
                "ja", "512x512 で BG1 を表示"),
              !SpriteScreen.SPR_RC_SCROLL_ON ? null : Multilingual.mlnText (
                ComponentFactory.createCheckBoxMenuItem (SpriteScreen.sprRcScrollRequest, "Row and column scroll", modificationListener),
                "ja", "行スクロールと列スクロール"),
              ComponentFactory.createHorizontalSeparator (),
              ComponentFactory.createHorizontalBox (
                Box.createHorizontalStrut (20),
                Multilingual.mlnText (ComponentFactory.createLabel ("Number of sprites per raster"), "ja", "ラスタあたりのスプライトの枚数"),
                Box.createHorizontalGlue ()
                ),
              ComponentFactory.createHorizontalBox (
                Box.createHorizontalStrut (20),
                sprrasSpinner,
                ComponentFactory.createLabel (" **"),
                Box.createHorizontalGlue ()
                ),
              Multilingual.mlnText (
                ComponentFactory.createMenuItem ("** Reset to default values", modificationListener),
              "ja", "** 初期値に戻す")
              ),  //Sprite screen
            "ja", "スプライト画面")
          ),  //Modification
        "ja", "改造");

    //メニューバー
    mnbMenuBar = ComponentFactory.createMenuBar (

      //ファイルメニュー
      mnbFileMenu = Multilingual.mlnText (
        ComponentFactory.createMenu (
          "File", 'F',
          //FDDメニュー
          FDC.fdcMenu,
          //SASI HDDメニュー
          HDC.hdcMenu,
          //内蔵 SCSI HDDメニュー
          SPC.spcMenu,
          //HFSメニュー
          HFS.hfsMenu,
          ComponentFactory.createHorizontalSeparator (),
          //電源
          !PowerControl.PWC_ON ? null : PowerControl.pwcMakeMenu (),
          //終了
          mnbQuitMenuItem = Multilingual.mlnText (ComponentFactory.createMenuItem ("Quit", 'Q', MNB_MODIFIERS, listener), "ja", "終了")
          ),
        "ja", "ファイル"),

      //MPUメニュー
      mpuMenu,

      //画面メニュー
      mnbDisplayMenu = Multilingual.mlnText (
        ComponentFactory.createMenu (
          "Display", 'D',
          mnbFullScreenMenuItem,
          !PNL_ROUNDED_CORNER_ON ? null : ComponentFactory.createHorizontalBox (
            Box.createHorizontalStrut (20),
            Multilingual.mlnText (
              ComponentFactory.createLabel ("Rounded corner ratio "),
              "ja", "角丸率 "),
            pnlRoundedCornerSpinner,
            ComponentFactory.createLabel ("%"),
            Box.createHorizontalGlue ()
            ),
          mnbMaximizedMenuItem,
          mnbFitInWindowMenuItem,
          mnbFixedScaleMenuItem,
          ComponentFactory.createHorizontalBox (
            Box.createHorizontalStrut (20),
            pnlFixedSpinner,
            Multilingual.mlnText (
              ComponentFactory.createLabel ("% Scale factor"),
              "ja", "% 倍率"),
            Box.createHorizontalGlue ()
            ),
          ComponentFactory.createMenuItem ("50%", listener),
          ComponentFactory.createMenuItem ("75%", listener),
          ComponentFactory.createMenuItem ("100%", listener),
          ComponentFactory.createMenuItem ("150%", listener),
          ComponentFactory.createMenuItem ("200%", listener),
          ComponentFactory.createHorizontalSeparator (),
          //回転
          rotationMenu,
          //アスペクト比
          aspectMenu,
          //補間アルゴリズム
          Multilingual.mlnText (
            ComponentFactory.createMenu (
              "Interpolation algorithm",
              Multilingual.mlnText (
                ComponentFactory.createRadioButtonMenuItem (
                  hintGroup, pnlInterpolation == RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR,
                  "Nearest neighbor", listener),
                "ja", "最近傍補間"),
              Multilingual.mlnText (
                ComponentFactory.createRadioButtonMenuItem (
                  hintGroup, pnlInterpolation == RenderingHints.VALUE_INTERPOLATION_BILINEAR,
                  "Bilinear", listener),
                "ja", "線形補間"),
              Multilingual.mlnText (
                ComponentFactory.createRadioButtonMenuItem (
                  hintGroup, pnlInterpolation == RenderingHints.VALUE_INTERPOLATION_BICUBIC,
                  "Bicubic", listener),
                "ja", "三次補間")
              ),
            "ja", "補間アルゴリズム"),
          //走査線エフェクト
          scanlineMenu,
          !PNL_USE_CANVAS ? null : Multilingual.mlnText (
            ComponentFactory.createCheckBoxMenuItem (pnlUseCanvasRequest, "Use canvas", listener),
            "ja", "キャンバスを使う"),
          //間欠描画
          !CRTC.CRT_ENABLE_INTERMITTENT ? null : Multilingual.mlnText (
            ComponentFactory.createMenu (
              "Intermittent drawing",
              Multilingual.mlnText (
                ComponentFactory.createRadioButtonMenuItem (
                  intermittentGroup, CRTC.crtIntermittentInterval == 0, "Draw all changed pictures", listener),
                "ja", "変化した画像をすべて描画する"),
              Multilingual.mlnText (
                ComponentFactory.createRadioButtonMenuItem (
                  intermittentGroup, CRTC.crtIntermittentInterval == 1, "Draw a changed picture once every two times", listener),
                "ja", "変化した画像を 2 回に 1 回描画する"),
              Multilingual.mlnText (
                ComponentFactory.createRadioButtonMenuItem (
                  intermittentGroup, CRTC.crtIntermittentInterval == 2, "Draw a changed picture once every three times", listener),
                "ja", "変化した画像を 3 回に 1 回描画する"),
              Multilingual.mlnText (
                ComponentFactory.createRadioButtonMenuItem (
                  intermittentGroup, CRTC.crtIntermittentInterval == 3, "Draw a changed picture once every four times", listener),
                "ja", "変化した画像を 4 回に 1 回描画する"),
              Multilingual.mlnText (
                ComponentFactory.createRadioButtonMenuItem (
                  intermittentGroup, CRTC.crtIntermittentInterval == 4, "Draw a changed picture once every five times", listener),
                "ja", "変化した画像を 5 回に 1 回描画する")
              ),
            "ja", "間欠描画"),
          //立体視
          !PNL_STEREOSCOPIC_ON ? null : ComponentFactory.createHorizontalSeparator (),
          mnbStereoscopicMenuItem = !PNL_STEREOSCOPIC_ON ? null : Multilingual.mlnText (
            ComponentFactory.createCheckBoxMenuItem (pnlStereoscopicOn, "Stereoscopic viewing", 'T', listener),
            "ja", "立体視"),
          !PNL_STEREOSCOPIC_ON ? null : Multilingual.mlnText (
            ComponentFactory.createMenu (
              "Stereoscopic settings",
              !PNL_STEREOSCOPIC_ON ? null : Multilingual.mlnText (
                ComponentFactory.createRadioButtonMenuItem (sterescopicGroup,
                                                            pnlStereoscopicMethod == PNL_NAKED_EYE_CROSSING,
                                                            "Naked-eye crossing", listener),
                "ja", "裸眼交差法"),
              !PNL_STEREOSCOPIC_ON ? null : Multilingual.mlnText (
                ComponentFactory.createRadioButtonMenuItem (sterescopicGroup,
                                                            pnlStereoscopicMethod == PNL_NAKED_EYE_PARALLEL,
                                                            "Naked-eye parallel", listener),
                "ja", "裸眼平行法"),
              !PNL_STEREOSCOPIC_ON ? null : Multilingual.mlnText (
                ComponentFactory.createRadioButtonMenuItem (sterescopicGroup,
                                                            pnlStereoscopicMethod == PNL_SIDE_BY_SIDE,
                                                            "Side-by-side", listener),
                "ja", "サイドバイサイド"),
              !PNL_STEREOSCOPIC_ON ? null : Multilingual.mlnText (
                ComponentFactory.createRadioButtonMenuItem (sterescopicGroup,
                                                            pnlStereoscopicMethod == PNL_TOP_AND_BOTTOM,
                                                            "Top-and-bottom", listener),
                "ja", "トップアンドボトム")
              ),
            "ja", "立体視設定"),
          //GIFアニメーション
          ComponentFactory.createHorizontalSeparator (),
          GIFAnimation.gifStartRecordingMenuItem,
          GIFAnimation.gifSettingsMenu,
          //改造
          ComponentFactory.createHorizontalSeparator (),
          modificationMenu,
          //
          //故障画面
          !Malfunction.MLF_ON ? null :
          Multilingual.mlnText (
            ComponentFactory.createMenuItem (
              "Malfunction screen (experimental)",
              listener),
            "ja", "故障画面 (実験中)"),
          !Malfunction.MLF_ON ? null :
          ComponentFactory.createHorizontalSeparator (),
          //
          SpritePatternViewer.SPV_ON ? Multilingual.mlnText (ComponentFactory.createMenuItem ("Sprite pattern viewer", listener), "ja", "スプライトパターンビュア") : null,
          PaletteViewer.PLV_ON ? Multilingual.mlnText (ComponentFactory.createMenuItem ("Palette viewer", listener), "ja", "パレットビュア") : null,
          ScreenModeTest.SMT_ON ? Multilingual.mlnText (ComponentFactory.createMenuItem ("Screen mode test", listener), "ja", "表示モードテスト") : null
          ),
        "ja", "画面"),

      //音声メニュー
      mnbSoundMenu = ComponentFactory.setEnabled (
        Multilingual.mlnText (
          ComponentFactory.createMenu (
            "Sound", 'S',
            mnbPlayMenuItem = ComponentFactory.setEnabled (
              Multilingual.mlnText (ComponentFactory.createCheckBoxMenuItem (SoundSource.sndPlayOn, "Play", 'P', MNB_MODIFIERS, listener), "ja", "音声出力"),
              SoundSource.sndLine != null),
            //ボリュームのラベル
            //  JLabelのalignmentでセンタリングしようとするとチェックボックスのサイズの分だけ右に寄ってしまう
            //  Boxで囲み、左右にglueを置くことでセンタリングする
            ComponentFactory.createHorizontalBox (
              Box.createHorizontalGlue (),
              Multilingual.mlnText (ComponentFactory.createLabel ("Volume "), "ja", "音量 "),
              mnbVolumeLabel = ComponentFactory.createLabel (String.valueOf (SoundSource.sndVolume)),
              Box.createHorizontalGlue ()
              ),
            //ボリュームスライダ
            //  デフォルトのサイズだと間延びした感じになるので幅を狭くする
            ComponentFactory.setPreferredSize (
              ComponentFactory.createHorizontalSlider (
                0,
                SoundSource.SND_VOLUME_MAX,
                SoundSource.sndVolume,
                SoundSource.SND_VOLUME_STEP,
                1,
                new ChangeListener () {
                  @Override public void stateChanged (ChangeEvent ce) {
                    SoundSource.sndSetVolume (((JSlider) ce.getSource ()).getValue ());
                  }
                }),
              LnF.lnfFontSize * 18, LnF.lnfFontSize * 2 + 28),
            Multilingual.mlnText (
              ComponentFactory.createMenu (
                "Sound interpolation",
                Multilingual.mlnText (
                  ComponentFactory.createRadioButtonMenuItem (
                    soundInterpolationGroup, SoundSource.sndRateConverter == SoundSource.SNDRateConverter.THINNING_STEREO,
                    "Sound thinning", listener),
                  "ja", "音声 間引き"),
                Multilingual.mlnText (
                  ComponentFactory.createRadioButtonMenuItem (
                    soundInterpolationGroup, SoundSource.sndRateConverter == SoundSource.SNDRateConverter.LINEAR_STEREO,
                    "Sound linear interpolation", listener),
                  "ja", "音声 線形補間"),
                ComponentFactory.setEnabled (
                  Multilingual.mlnText (
                    ComponentFactory.createRadioButtonMenuItem (
                      soundInterpolationGroup, SoundSource.sndRateConverter == SoundSource.SNDRateConverter.CONSTANT_AREA_STEREO_48000,
                      "Sound piecewise-constant area interpolation", listener),
                    "ja", "音声 区分定数面積補間"),
                  SoundSource.SND_CHANNELS == 2 && SoundSource.SND_SAMPLE_FREQ == 48000),
                ComponentFactory.setEnabled (
                  Multilingual.mlnText (
                    ComponentFactory.createRadioButtonMenuItem (
                      soundInterpolationGroup, SoundSource.sndRateConverter == SoundSource.SNDRateConverter.LINEAR_AREA_STEREO_48000,
                      "Sound linear area interpolation", listener),
                    "ja", "音声 線形面積補間"),
                  SoundSource.SND_CHANNELS == 2 && SoundSource.SND_SAMPLE_FREQ == 48000)
                ),
              "ja", "音声補間"),
            Multilingual.mlnText (ComponentFactory.createMenuItem ("Sound monitor", listener), "ja", "音声モニタ"),
            //
            ComponentFactory.createHorizontalSeparator (),
            //
            ComponentFactory.setEnabled (
              Multilingual.mlnText (ComponentFactory.createCheckBoxMenuItem (OPM.opmOutputMask != 0, "OPM output", listener), "ja", "OPM 出力"),
              SoundSource.sndLine != null),
            !OPMLog.OLG_ON ? null : Multilingual.mlnText (ComponentFactory.createMenuItem ("OPM log", listener), "ja", "OPM ログ"),
            //
            ComponentFactory.createHorizontalSeparator (),
            //
            ComponentFactory.setEnabled (
              Multilingual.mlnText (ComponentFactory.createCheckBoxMenuItem (ADPCM.pcmOutputOn, "PCM output", listener), "ja", "PCM 出力"),
              SoundSource.sndLine != null),
            Multilingual.mlnText (
              ComponentFactory.createMenu (
                "PCM interpolation",
                Multilingual.mlnText (
                  ComponentFactory.createRadioButtonMenuItem (
                    adpcmInterpolationGroup, ADPCM.pcmInterpolationAlgorithm == ADPCM.PCM_INTERPOLATION_CONSTANT,
                    "PCM piecewise-constant interpolation", listener),
                  "ja", "PCM 区分定数補間"),
                Multilingual.mlnText (
                  ComponentFactory.createRadioButtonMenuItem (
                    adpcmInterpolationGroup, ADPCM.pcmInterpolationAlgorithm == ADPCM.PCM_INTERPOLATION_LINEAR,
                    "PCM linear interpolation", listener),
                  "ja", "PCM 線形補間"),
                Multilingual.mlnText (
                  ComponentFactory.createRadioButtonMenuItem (
                    adpcmInterpolationGroup, ADPCM.pcmInterpolationAlgorithm == ADPCM.PCM_INTERPOLATION_HERMITE,
                    "PCM hermite interpolation", listener),
                  "ja", "PCM エルミート補間")
                ),
              "ja", "PCM 補間"),
            Multilingual.mlnText (
              ComponentFactory.createMenu (
                "PCM source oscillator frequency",
                ComponentFactory.createRadioButtonMenuItem (adpcmOSCFreqGroup, ADPCM.pcmOSCFreqRequest == 0, "PCM 8MHz/4MHz", listener),
                ComponentFactory.createRadioButtonMenuItem (adpcmOSCFreqGroup, ADPCM.pcmOSCFreqRequest == 1, "PCM 8MHz/16MHz", listener)
                ),
              "ja", "PCM 原発振周波数"),
            //
            !MercuryUnit.MU4_ON ? null :
            ComponentFactory.createHorizontalSeparator (),
            !MercuryUnit.MU4_ON ? null :
            ComponentFactory.createCheckBoxMenuItem (
              MercuryUnit.mu4OnRequest,
              "Mercury-Unit V4 (MK-MU1)",
              listener),
            !MercuryUnit.MU4_ON ? null :
            Multilingual.mlnText (
              ComponentFactory.createCheckBoxMenuItem (
                MercuryUnit.mu4OutputEnable,
                "MU4 output",
                listener),
              "ja", "MU4 出力"),
            ComponentFactory.createHorizontalSeparator (),
            SRAM.smrStupsndMenu
            ),
          "ja", "音声"),
        SoundSource.sndLine != null),

      //入力メニュー
      mnbInputMenu = Multilingual.mlnText (
        ComponentFactory.createMenu (
          "Input", 'I',
          mnbPasteMenuItem = Multilingual.mlnText (ComponentFactory.createMenuItem ("Paste", 'V', MNB_MODIFIERS, listener), "ja", "貼り付け"),
          CONDevice.conSettingsMenu,
          TextCopy.txcMakeMenuItem (),
          TextCopy.txcMakeSettingMenu (),
          ComponentFactory.createHorizontalSeparator (),
          mnbNoKeyboardMenuItem = Multilingual.mlnText (
            ComponentFactory.createRadioButtonMenuItem (keyboardGroup, !Keyboard.kbdOn, "No keyboard", 'K', MNB_MODIFIERS, listener),
            "ja", "キーボードなし"),
          mnbStandardKeyboardMenuItem = Multilingual.mlnText (
            ComponentFactory.createRadioButtonMenuItem (keyboardGroup, Keyboard.kbdOn && Keyboard.kbdType == Keyboard.KBD_STANDARD_TYPE, "Standard keyboard", listener),
            "ja", "標準キーボード"),
          mnbCompactKeyboardMenuItem = Multilingual.mlnText (
            ComponentFactory.createRadioButtonMenuItem (keyboardGroup, Keyboard.kbdOn && Keyboard.kbdType == Keyboard.KBD_COMPACT_TYPE, "Compact keyboard", listener),
            "ja", "コンパクトキーボード"),
          Multilingual.mlnText (
            ComponentFactory.createCheckBoxMenuItem (pnlHideKeyboard, "Hide keyboard in full screen", listener),
          "ja", "全画面表示のときキーボードを隠す"),
          Multilingual.mlnText (ComponentFactory.createMenuItem ("Key assignments", listener), "ja", "キー割り当て"),
          ButtonFunction.bfnMakeMenuItem (),
          SRAM.smrRepeatDelayMenu,
          SRAM.smrRepeatIntervalMenu,
          !Keyboard.KBD_ZKEY_ON ? null : Keyboard.kbdZKeyMenu,
          ComponentFactory.createHorizontalSeparator (),
          Mouse.musSeamlessMouseCheckBox,
          Mouse.musFollowScrollCheckBox,
          Mouse.musCtrlRightCheckBox,
          Mouse.musEdgeAccelerationCheckBox,
          Mouse.musMouseCursorSpeedBox,
          Mouse.musSpeedSlider,
          Mouse.musHostsPixelUnitsCheckBox,
          ComponentFactory.createHorizontalSeparator (),
          Multilingual.mlnText (ComponentFactory.createMenuItem ("Joystick port settings", listener), "ja", "ジョイスティックポート設定")
          ),
        "ja", "入力"),

      //設定メニュー
      mnbConfigMenu = Multilingual.mlnText (
        ComponentFactory.createMenu (
          "Config", 'G',
          Multilingual.mlnText (ComponentFactory.createMenuItem ("RS-232C and terminal", listener), "ja", "RS-232C とターミナル"),
          Multilingual.mlnText (
            ComponentFactory.createMenu (
              "Debug",
              Multilingual.mlnText (ComponentFactory.createMenuItem ("Console", listener), "ja", "コンソール"),
              Multilingual.mlnText (ComponentFactory.createMenuItem ("Register list", listener), "ja", "レジスタリスト"),
              Multilingual.mlnText (ComponentFactory.createMenuItem ("Disassemble list", listener), "ja", "逆アセンブルリスト"),
              Multilingual.mlnText (ComponentFactory.createMenuItem ("Memory dump list", listener), "ja", "メモリダンプリスト"),
              Multilingual.mlnText (ComponentFactory.createMenuItem ("Logical space monitor", listener), "ja", "論理空間モニタ"),
              Multilingual.mlnText (ComponentFactory.createMenuItem ("Physical space monitor", listener), "ja", "物理空間モニタ"),
              ATCMonitor.ACM_ON ? Multilingual.mlnText (ComponentFactory.createMenuItem ("Address translation caches monitor", listener), "ja", "アドレス変換キャッシュモニタ") : null,
              BranchLog.BLG_ON ? Multilingual.mlnText (ComponentFactory.createMenuItem ("Branch log", listener), "ja", "分岐ログ") : null,
              ProgramFlowVisualizer.PFV_ON ? Multilingual.mlnText (ComponentFactory.createMenuItem ("Program flow visualizer", listener), "ja", "プログラムフロービジュアライザ") : null,
              RasterBreakPoint.RBP_ON ? Multilingual.mlnText (ComponentFactory.createMenuItem ("Raster break point", listener), "ja", "ラスタブレークポイント") : null,
              DataBreakPoint.DBP_ON ? Multilingual.mlnText (ComponentFactory.createMenuItem ("Data break point", listener), "ja", "データブレークポイント") : null,
              RootPointerList.RTL_ON ? Multilingual.mlnText (ComponentFactory.createMenuItem ("Root pointer list", listener), "ja", "ルートポインタリスト") : null,
              ComponentFactory.createHorizontalSeparator (),
              SRAM.smrRomdbMenu
              ),
            "ja", "デバッグ"),
          SRAM.smrBootMenu,
          mainMemoryMenu,
          highMemoryMenu,
          localMemoryMenu,
          xellent30Menu,
          ComponentFactory.createHorizontalSeparator (),
          ComponentFactory.createMenu (
            "RTC",
            Multilingual.mlnText (
              ComponentFactory.createMenuItem ("Adjust clock to host", listener),
              "ja", "時計をホストに合わせる")
            ),
          Z8530.sccMakeMenu (),
          SRAM.smrMenu,
          Settings.sgsMenu,
          ComponentFactory.createHorizontalSeparator (),
          Multilingual.mlnText (ComponentFactory.createMenuItem ("Printer", listener), "ja", "プリンタ"),
          ROM.romMenu,
          Multilingual.mlnText (
            ComponentFactory.createMenu (
              "Miscellaneous",
              SlowdownTest.sdtCheckBoxMenuItem,
              SlowdownTest.sdtBox,
              Multilingual.mlnText (
                ComponentFactory.createCheckBoxMenuItem (Mouse.musOutputButtonStatus, "Print key and mouse button events", listener),
                "ja", "キーとマウスのボタンのイベントを表示")
              ),
            "ja", "その他"),
          ComponentFactory.createHorizontalSeparator (),
          Multilingual.mlnText (
            ComponentFactory.createMenu (
              "Accessibility",
              //点滅光の軽減
              !FlashingLights.FLR_ON ? null :
              FlashingLights.flrMakeMenu (),
              !FlashingLights.FLR_ON ? null :
              ComponentFactory.createHorizontalSeparator (),
              //フォントサイズ
              mnbMakeFontSizeMenu (),
              //色
              mnbMakeColorMenu ()
              ),
            "ja", "アクセシビリティ"),
          ComponentFactory.createHorizontalSeparator (),
          Multilingual.mlnText (
            ComponentFactory.createMenuItem ("Java runtime environment information", listener),
            "ja", "Java 実行環境の情報"),
          Multilingual.mlnText (
            ComponentFactory.createMenuItem ("Version information", listener),
            "ja", "バージョン情報"),
          Multilingual.mlnText (
            ComponentFactory.createMenu (
              "License",
              Multilingual.mlnText (ComponentFactory.createMenuItem ("XEiJ License", listener), "ja", "XEiJ 使用許諾条件"),
              Multilingual.mlnText (ComponentFactory.createMenuItem ("FSHARP License", listener), "ja", "FSHARP 許諾条件"),
              Multilingual.mlnText (ComponentFactory.createMenuItem ("ymfm License", listener), "ja", "ymfm License"),
              Multilingual.mlnText (ComponentFactory.createMenuItem ("jSerialComm License", listener), "ja", "jSerialComm License")
              ),
            "ja", "使用許諾条件")
          ),
        "ja", "設定"),

      mnbMakeLanguageMenu (),  //言語メニュー

      //インジケータ
      Box.createHorizontalGlue (),  //インジケータをセンタリングする
      ComponentFactory.createVerticalBox (
        Box.createVerticalGlue (),
        Indicator.indBox,
        Box.createVerticalGlue ()
        ),
      Box.createHorizontalGlue ()

      );
  }  //mnbMakeMenu()



  //========================================================================================
  //$$FRM フレーム

  //モード
  public static boolean frmIsActive;  //true=フォーカスがある

  //フレーム
  public static JFrame frmFrame;  //フレーム
  public static int frmMarginWidth;  //パネルからフレームまでのマージン
  public static int frmMarginHeight;
  public static Dimension frmMinimumSize;  //pnlMinimumWidth+frmMarginWidth,pnlMinimumHeight+frmMarginHeight フレームの最小サイズ

  //スクリーンデバイス
  public static GraphicsDevice frmScreenDevice;  //スクリーンデバイス

  //ドラッグアンドドロップ
  public static DropTarget frmDropTarget;

  //frmInit ()
  //  フレームを初期化する
  public static void frmInit () {
    frmIsActive = false;
    GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment ();
    frmScreenDevice = environment.getDefaultScreenDevice ();  //スクリーンデバイス
    pnlIsFullScreenSupported = false;
    for (GraphicsDevice screenDevice : environment.getScreenDevices ()) {
      if (screenDevice.isFullScreenSupported ()) {
        pnlIsFullScreenSupported = true;
        break;
      }
    }
  }  //frmInit()

  //frmMake ()
  //  フレームを作る
  public static void frmMake () {

    //フレーム
    frmFrame = ComponentFactory.createRestorableFrame (
      Settings.SGS_FRM_FRAME_KEY,
      PRG_TITLE + " version " + PRG_VERSION,
      mnbMenuBar,
      pnlPanel);
    frmUpdateTitle ();
    frmFrame.setIconImage (LnF.LNF_ICON_IMAGE_48);  //タスクバーのアイコンを変更する
    if (PowerControl.PWC_ON) {
      frmFrame.setDefaultCloseOperation (WindowConstants.DO_NOTHING_ON_CLOSE);
    } else {
      frmFrame.setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
    }
    //frmFrame.setResizable (false);  //リサイズ不可

    //パネルからフレームまでのマージンを確認する
    //!!! 最大化された状態で起動したときこの時点でfrmFrameとpnlPanelのサイズが合っていないためfrmMinimumSizeが不適切な値になる
    frmMarginWidth = frmFrame.getWidth () - pnlPanel.getWidth ();
    frmMarginHeight = frmFrame.getHeight () - pnlPanel.getHeight ();
    frmMinimumSize = new Dimension (pnlMinimumWidth + frmMarginWidth, pnlMinimumHeight + frmMarginHeight);
    frmFrame.setMinimumSize (frmMinimumSize);

    //ドラッグアンドドロップ
    //  FDイメージが放り込まれたらそこから再起動する
    frmDropTarget = new DropTarget (pnlPanel, DnDConstants.ACTION_COPY, new DropTargetAdapter () {
      @Override public void dragOver (DropTargetDragEvent dtde) {
        if (dtde.isDataFlavorSupported (DataFlavor.javaFileListFlavor)) {
          dtde.acceptDrag (DnDConstants.ACTION_COPY);
          return;
        }
        dtde.rejectDrag ();
      }
      @Override public void drop (DropTargetDropEvent dtde) {
        try {
          if (dtde.isDataFlavorSupported (DataFlavor.javaFileListFlavor)) {
            dtde.acceptDrop (DnDConstants.ACTION_COPY);
            boolean reset = false;  //true=ここから再起動,false=開く
            int fdu0 = -1;
            int fdu = 0;
            int hdu0 = -1;
            int hdu = 0;
            int scu0 = -1;
            int scu = 0;
            int hfu0 = -1;
            int hfu = 0;
            for (Object o : (java.util.List) dtde.getTransferable ().getTransferData (DataFlavor.javaFileListFlavor)) {
              if (o instanceof File) {
                File file = (File) o;
                if (file.isFile ()) {  //ファイルのとき。HFS以外のフィルタがディレクトリを受け入れてしまうのでディレクトリを除外する
                  if (FDC.fdcFileFilter.accept (file)) {  //FDのイメージファイルのとき
                    if (fdu < FDC.FDC_MAX_UNITS &&
                        FDC.fdcUnitArray[fdu].insert (file.getPath (), false)) {  //挿入できた
                      if (fdu0 < 0) {
                        fdu0 = fdu;
                      }
                      fdu++;
                      continue;
                    }
                  }
                  if (HDC.hdcFileFilter.accept (file)) {  //SASIハードディスクのイメージファイルのとき
                    if (hdu < 16 &&
                        HDC.hdcUnitArray[hdu].insert (file.getPath (), false)) {  //挿入できた
                      if (hdu0 < 0) {
                        hdu0 = hdu;
                      }
                      hdu++;
                      continue;
                    }
                  }
                  if (SPC.spcFileFilter.accept (file)) {  //SCSIハードディスク/CD-ROMのイメージファイルのとき
                    if (scu < 16 &&
                        SPC.spcUnitArray[scu].insert (file.getPath (), false)) {  //挿入できた
                      if (scu0 < 0) {
                        scu0 = scu;
                      }
                      scu++;
                      continue;
                    }
                  }
                }
                if (HFS.hfsFileFilter.accept (file)) {  //ディレクトリまたはHUMAN.SYSのとき
                  if (hfu < HFS.HFS_MAX_UNITS &&
                      HFS.hfsUnitArray[hfu].insert (file.getPath (), false)) {  //挿入できた
                    if (hfu0 < 0) {
                      hfu0 = hfu;
                    }
                    hfu++;
                    continue;
                  }
                }
              }
              reset = false;  //挿入できないファイルがあったときはリセットをキャンセルする
            }
            dtde.dropComplete (true);
            if (reset) {
              if (fdu0 >= 0) {
                mpuReset (0x9070 | fdu0 << 8, -1);
              } else if (hdu0 >= 0) {
                mpuReset (0x8000 | hdu0 << 8, -1);
              } else if (scu0 >= 0) {
                mpuReset (0xa000, SPC.SPC_HANDLE_EX + (scu0 << 2));  //拡張SCSIがなければ内蔵SCSIに読み替えられる
              } else if (hfu0 >= 0) {
                HFS.hfsBootUnit = hfu0;
                mpuReset (0xa000, HFS.HFS_BOOT_HANDLE);
              }
            }
            return;
          }
        } catch (UnsupportedFlavorException ufe) {
          //ufe.printStackTrace ();
        } catch (IOException ioe) {
          //ioe.printStackTrace ();
        }
        dtde.rejectDrop();
      }
    });

  }  //frmMake()

  //frmUpdateTitle ()
  //  フレームのタイトルを更新する
  public static void frmUpdateTitle () {
    frmFrame.setTitle ((currentAccelerator == ACCELERATOR_HYBRID ? "X68000 Hybrid" :
                        currentModel.getName () +
                        (currentAccelerator == ACCELERATOR_XELLENT30 ? " with Xellent30" :
                         currentAccelerator == ACCELERATOR_060TURBO ? " with 060turbo" :
                         currentAccelerator == ACCELERATOR_060TURBOPRO ? " with 060turboPRO" : "")) +
                       " - " + PRG_TITLE + " version " + PRG_VERSION);
  }

  //frmStart ()
  //  フレームのイベントリスナーを設定して動作を開始する
  public static void frmStart () {

    //ウインドウリスナー
    //  ウインドウを開いたとき  activated,opened
    //  フォーカスを失ったとき  deactivated
    //  フォーカスを取得したとき  activated
    //  ウインドウをアイコン化したとき  iconified,[deactivated]
    //  ウインドウを元のサイズに戻したとき  deiconified,activated
    //  ウインドウを閉じたとき  closing,[deactivated],closed
    ComponentFactory.addListener (
      frmFrame,
      new WindowAdapter () {
        @Override public void windowActivated (WindowEvent we) {
          frmIsActive = true;
        }
        @Override public void windowClosing (WindowEvent we) {
          if (PowerControl.PWC_ON) {
            PowerControl.pwcQuit ();
          } else {
            prgTini ();
          }
        }
        @Override public void windowDeactivated (WindowEvent we) {
          frmIsActive = false;
          //Mouse.musSetSeamlessOn (true);  //フォーカスを失ったときはシームレスに切り替える
        }
        @Override public void windowOpened (WindowEvent we) {
          if (pnlAdjustVsync && pnlFixedRate == 0.0) {  //必要だが指定がなくて未取得
            pnlFixedRate = pnlGetRefreshRate ();
          }
        }
      });

    //コンポーネントリスナー
    //  エクスクルーシブマウスモードのときに使うパネルの座標を得る
    //  全画面表示のON/OFFを行ったときcomponentMovedが呼ばれないことがあるのでcomponentResizedでもパネルの座標を得る
    ComponentFactory.addListener (
      frmFrame,
      new ComponentAdapter () {
        private double lastScaleX = 0.0;
        private double lastScaleY = 0.0;
        private javax.swing.Timer rebuildTimer;
        @Override public void componentMoved (ComponentEvent ce) {
          Point p = pnlPanel.getLocationOnScreen ();
          pnlGlobalX = p.x;
          pnlGlobalY = p.y;
          checkScaleChange ();
        }
        @Override public void componentResized (ComponentEvent ce) {
          Point p = pnlPanel.getLocationOnScreen ();
          pnlGlobalX = p.x;
          pnlGlobalY = p.y;
          checkScaleChange ();
        }
        //DPIの異なるディスプレイへ移動したとき、移動中の中途半端なサイズで余白を計算して
        //タイトルバーやメニューバーが欠けることがあるので、移動が落ち着いてから再構成する
        private void checkScaleChange () {
          GraphicsConfiguration gc = frmFrame.getGraphicsConfiguration ();
          if (gc == null) {
            return;
          }
          java.awt.geom.AffineTransform at = gc.getDefaultTransform ();
          double sx = at.getScaleX ();
          double sy = at.getScaleY ();
          if (lastScaleX != 0.0 && (sx != lastScaleX || sy != lastScaleY) && pnlMode != PNL_FULLSCREEN) {
            if (rebuildTimer == null) {
              rebuildTimer = new javax.swing.Timer (300, new ActionListener () {
                @Override public void actionPerformed (ActionEvent ae) {
                  if (pnlMode == PNL_FULLSCREEN) {
                    return;
                  }
                  //装飾とメニューバーを張り直し、サイズ制約を外してpackし直してから再配置する
                  JRootPane rp = frmFrame.getRootPane ();
                  frmFrame.setJMenuBar (null);
                  rp.setWindowDecorationStyle (JRootPane.NONE);
                  rp.setWindowDecorationStyle (JRootPane.FRAME);
                  frmFrame.setJMenuBar (mnbMenuBar);
                  if (pnlMode == PNL_FIXEDSCALE) {
                    frmFrame.setMinimumSize (null);
                    frmFrame.setMaximumSize (null);
                    frmFrame.setPreferredSize (null);
                    frmFrame.setResizable (true);
                    frmFrame.pack ();
                    //同じサイズだとネイティブウインドウにリサイズが伝わらないので1pxずらして戻す
                    Dimension d = frmFrame.getSize ();
                    frmFrame.setSize (d.width + 1, d.height + 1);
                  }
                  pnlUpdateArrangement ();
                  frmFrame.invalidate ();
                  frmFrame.validate ();
                  rp.repaint ();
                }
              });
              rebuildTimer.setRepeats (false);
            }
            rebuildTimer.restart ();
          }
          lastScaleX = sx;
          lastScaleY = sy;
        }
      });

  }  //frmStart()



  //========================================================================================
  //$$CLP クリップボード

  public static BufferedImage clpClipboardImage;  //コピーされるイメージ
  public static String clpClipboardString;  //コピーされる文字列
  public static Clipboard clpClipboard;  //クリップボード
  public static Transferable clpImageContents;  //イメージをコピーするときに渡すデータ
  public static Transferable clpStringContents;  //文字列をコピーするときに渡すデータ
  public static ClipboardOwner clpClipboardOwner;  //クリップボードオーナー。コピーするときにデータに付ける情報
  public static boolean clpIsClipboardOwner;  //true=クリップボードに入っているデータは自分がコピーした

  //clpMake ()
  //  クリップボードを作る
  //  mnbMake()より前に呼び出すこと
  public static void clpMake () {
    Toolkit toolkit = Toolkit.getDefaultToolkit ();
    clpClipboard = null;
    try {
      clpClipboard = toolkit.getSystemClipboard ();  //クリップボード
    } catch (Exception e) {
      return;
    }
    clpClipboardImage = null;  //コピーされるイメージ
    clpClipboardString = null;  //コピーされる文字列
    clpImageContents = new Transferable () {
      public Object getTransferData (DataFlavor flavor) throws UnsupportedFlavorException {
        if (flavor == DataFlavor.imageFlavor) {
          return clpClipboardImage;
        } else {
          throw new UnsupportedFlavorException (flavor);
        }
      }
      public DataFlavor[] getTransferDataFlavors () {
        return new DataFlavor[] { DataFlavor.imageFlavor };
      }
      public boolean isDataFlavorSupported (DataFlavor flavor) {
        return flavor == DataFlavor.imageFlavor;
      }
    };
    clpStringContents = new Transferable () {
      public Object getTransferData (DataFlavor flavor) throws UnsupportedFlavorException {
        if (flavor == DataFlavor.stringFlavor) {
          return clpClipboardString;
        } else {
          throw new UnsupportedFlavorException (flavor);
        }
      }
      public DataFlavor[] getTransferDataFlavors () {
        return new DataFlavor[] { DataFlavor.stringFlavor };
      }
      public boolean isDataFlavorSupported (DataFlavor flavor) {
        return flavor == DataFlavor.stringFlavor;
      }
    };
    clpIsClipboardOwner = false;  //自分はまだコピーしていない
    //クリップボードオーナー
    //  lostOwnership   クリップボードの所有者でなくなったとき
    clpClipboardOwner = new ClipboardOwner () {
      @Override public void lostOwnership (Clipboard clipboard, Transferable contents) {
        clpIsClipboardOwner = false;
      }
    };
    //フレーバーリスナー
    //  flavorsChanged  クリップボードのDataFlavorが変化したとき
    clpClipboard.addFlavorListener (new FlavorListener () {
      @Override public void flavorsChanged (FlavorEvent fe) {
        boolean available = false;
        try {
          available = clpClipboard.isDataFlavorAvailable (DataFlavor.stringFlavor);
        } catch (IllegalStateException ise) {
        }
        if (mnbPasteMenuItem != null) {
          mnbPasteMenuItem.setEnabled (available);  //文字列ならば貼り付けできる
        }
      }
    });
    if (!clpClipboard.isDataFlavorAvailable (DataFlavor.stringFlavor)) {  //文字列がコピーされていない
      if (mnbPasteMenuItem != null) {
        mnbPasteMenuItem.setEnabled (false);  //貼り付け選択不可
      }
    }
  }  //clpMake

  //clpCopy (s)
  //  クリップボードに文字列をコピーする
  public static void clpCopy (String s) {
    if (clpClipboard != null && s != null) {
      clpClipboardString = s;
      try {
        clpClipboard.setContents (clpStringContents, clpClipboardOwner);
        clpIsClipboardOwner = true;  //自分がコピーした
      } catch (Exception e) {
        return;
      }
    }
  }  //clpCopy



  //========================================================================================
  //$$XT3 Xellent30
  //
  //  DIPSW
  //        1    2     ポートのアドレス
  //    0  OFF  OFF  $00EC0000～$00EC3FFF
  //    1  OFF  ON   $00EC4000～$00EC7FFF
  //    2  ON   OFF  $00EC8000～$00ECBFFF
  //    3  ON   ON   $00ECC000～$00ECFFFF
  //
  //  ポート
  //    FEDCBA9876543210
  //                 |||
  //                 ||bit0:メモリの位置。0=$00Bxxxxx,1=$00Fxxxxx
  //                 |bit1:メモリの有無。0=なし,1=あり
  //                 bit2:MPU。0=MC68000,1=MC68030
  //    +$3FFFまで同じワードが繰り返される
  //

  //DIPSW
  public static int xt3DIPSWRequest;  //DIPSW。0～3
  public static int xt3DIPSW;
  public static int xt3PortAddress;  //ポートのアドレス。0x00ec0000+(xt3DIPSW<<14)

  //メモリ
  public static int xt3MemorySizeRequest;  //メモリのサイズ。1<<18(256KB)または1<<20(1MB)
  public static int xt3MemorySize;
  public static boolean xt3MemoryEnabled;  //メモリの有無。false=なし,true=あり
  public static int xt3MemoryPosition;  //メモリの位置。xt3MemoryArray[0]のアドレス。11<<20または15<<20
  public static int xt3MemoryStart;  //メモリの開始アドレス。xt3MemoryPosition+(1<<20)-xt3MemorySize
  public static final byte[] xt3MemoryArray = new byte[1 << 20];  //メモリの内容の配列。常に1MB
  public static boolean xt3MemorySave;  //メモリの保存フラグ。true=保存する。常に1MBを保存・復元する

  //MPU
  public static int xt3SavedPC;  //MC68000のPC
  public static final int[] xt3SavedRn = new int[16];  //MC68000のRn

  //初期化
  public static void xt3Init () {

    //DIPSW
    xt3DIPSWRequest = Math.max (0, Math.min (3, Settings.sgsGetInt ("xt3dipsw")));  //メニューで変更できる
    xt3DIPSW = xt3DIPSWRequest;  //リセットで更新する

    //ポートのアドレス
    xt3PortAddress = 0x00ec0000 + (xt3DIPSW << 14);  //リセットで更新する

    //メモリのサイズ
    int memoryKB = Settings.sgsGetInt ("xt3memorykb");
    if (!(memoryKB == 1 << 8 || memoryKB == 1 << 10)) {
      memoryKB = 1 << 8;
    }
    xt3MemorySizeRequest = memoryKB << 10;  //メニューで変更できる
    xt3MemorySize = xt3MemorySizeRequest;  //リセットで更新する

    //メモリの有無
    xt3MemoryEnabled = false;  //リセットで更新する。ポートで変更できる

    //メモリの位置
    xt3MemoryPosition = 11 << 20;  //リセットで更新する。ポートで変更できる

    //メモリの開始アドレス
    xt3MemoryStart = xt3MemoryPosition + (1 << 20) - xt3MemorySize;  //リセットで更新する。ポートで変更できる

    //メモリの内容の配列
    //xt3MemoryArray = new byte[1 << 20];
    byte[] memoryArray = Settings.sgsGetData ("xt3memorydata");  //リセットでは更新しない。動作中に変化する
    Arrays.fill (xt3MemoryArray,  //array
                 (byte) 0);  //data
    if (memoryArray.length != 0) {  //復元するデータがある
      System.arraycopy (memoryArray, 0,  //from
                        xt3MemoryArray, 0,  //to
                        Math.min (memoryArray.length, xt3MemoryArray.length));  //length
      if (memoryArray.length < xt3MemoryArray.length) {
        Arrays.fill (xt3MemoryArray,  //array
                     memoryArray.length,  //from
                     xt3MemoryArray.length,  //to
                     (byte) 0);  //data
      }
    }

    //メモリの保存フラグ
    xt3MemorySave = Settings.sgsGetOnOff ("xt3memorysave");  //メニューで変更できる

    //MPU
    xt3SavedPC = 0;  //リセットで更新する
    //xt3SavedRn = new int[16];
    Arrays.fill (xt3SavedRn, 0);  //リセットで更新する

    xt3Reset ();
  }

  //後始末
  public static void xt3Tini () {

    //DIPSW
    Settings.sgsPutInt ("xt3dipsw", xt3DIPSW);  //メニューで変更できる

    //メモリのサイズ
    Settings.sgsPutInt ("xt3memorykb", xt3MemorySizeRequest >> 10);  //メニューで変更できる

    //メモリの内容の配列
    boolean zero = true;
    if (xt3MemorySave) {
      for (int i = 0; i < 1 << 20; i++) {
        if (xt3MemoryArray[i] != 0) {
          zero = false;
          break;
        }
      }
    }
    if (zero) {
      Settings.sgsCurrentMap.put ("xt3memorydata", "");
    } else {
      if (false) {
        Settings.sgsCurrentMap.put ("xt3memorydata",
                                    ByteArray.byaEncodeBase64 (ByteArray.byaEncodeGzip (xt3MemoryArray, 0, 1 << 20)));  //動作中に変化する
      } else {
        Settings.sgsPutData ("xt3memorydata",
                             Arrays.copyOf (xt3MemoryArray, 1 << 20));
      }
    }
    //メモリの保存フラグ
    Settings.sgsPutOnOff ("xt3memorysave", xt3MemorySave);  //メニューで変更できる

  }

  //リセット
  public static void xt3Reset () {

    //ポートのアドレス
    xt3PortAddress = 0x00ec0000 + (xt3DIPSW << 14);  //リセットで更新する

    //メモリのサイズ
    xt3MemorySize = xt3MemorySizeRequest;  //リセットで更新する

    //メモリの有無
    xt3MemoryEnabled = false;  //リセットで更新する。ポートで変更できる

    //メモリの位置
    xt3MemoryPosition = 11 << 20;  //リセットで更新する。ポートで変更できる

    //メモリの開始アドレス
    xt3MemoryStart = xt3MemoryPosition + (1 << 20) - xt3MemorySize;  //リセットで更新する。ポートで変更できる

    //MPU
    xt3SavedPC = 0;  //リセットで更新する
    Arrays.fill (xt3SavedRn, 0);  //リセットで更新する

  }

  //リードワード
  public static int xt3PortRead () {
    return (currentIsSecond ? 4 : 0) | (xt3MemoryEnabled ? 2 : 0) | (xt3MemoryPosition == 11 << 20 ? 0 : 1);
  }

  //ライトワード
  public static void xt3PortWrite (int d) {
    boolean nextIsSecond = (d & 4) != 0;
    boolean memoryEnabled = (d & 2) != 0;
    int memoryPosition = (d & 1) == 0 ? 11 << 20 : 15 << 20;

    if (xt3MemoryEnabled != memoryEnabled ||
        xt3MemoryPosition != memoryPosition) {  //メモリの有無または位置が変更された
      if (xt3MemoryEnabled) {  //元の位置から取り除く
        if (xt3MemoryPosition == 11 << 20) {  //$00Bxxxxx
          if (MainMemory.mmrMemorySizeCurrent < 12 << 20) {  //メインメモリが12MBない
            busSuper (MemoryMappedDevice.MMD_NUL, (12 << 20) - xt3MemorySize, 12 << 20);  //空きに戻す
          } else {  //メインメモリが12MBある
            busUser (MemoryMappedDevice.MMD_MMR, (12 << 20) - xt3MemorySize, 12 << 20);  //メインメモリに戻す
          }
        } else {  //$00Fxxxxx
          busSuper (MemoryMappedDevice.MMD_ROM, (16 << 20) - xt3MemorySize, 16 << 20);  //ROMに戻す
        }
      }
      xt3MemoryEnabled = memoryEnabled;  //メモリの有無
      xt3MemoryPosition = memoryPosition;  //メモリの位置
      if (xt3MemoryEnabled) {  //新しい位置に配置する
        if (xt3MemoryPosition == 11 << 20) {  //$00Bxxxxx
          busUser (MemoryMappedDevice.MMD_XTM, (12 << 20) - xt3MemorySize, 12 << 20);  //メモリを配置する
        } else {  //$00Fxxxxx
          busUser (MemoryMappedDevice.MMD_XTM, (16 << 20) - xt3MemorySize, 16 << 20);  //メモリを配置する
        }
      }
    }

    if (currentIsSecond != nextIsSecond) {  //MPUが変わる

      if (nextIsSecond) {  //MC68000→MC68EC030

        //HALTしたときのPCとRnを保存する
        xt3SavedPC = regPC;
        System.arraycopy (regRn, 0, xt3SavedRn, 0, 16);

        //MC68000を停止する
        if (mpuTask != null) {
          mpuClockLimit = 0L;
          System.out.println (Multilingual.mlnJapanese ?
                              Model.MPU_NAMES[currentFirstMPU] + " を停止します" :
                              Model.MPU_NAMES[currentFirstMPU] + " stops");
          mpuTask.cancel ();
          mpuTask = null;
        }

        //MC68EC030を起動する
        tmrTimer.schedule (new TimerTask () {
          @Override public void run () {

            //8bit I/Oポートのメモリマップの設定(Xellent30の030モード)
            busSetPortMap_03 ();

            //MPU
            currentIsSecond = true;
            currentMPU = currentSecondMPU;
            mpuSetCurrentClock (specifiedSecondClock);

            if (MC68EC030.M30_DIV_ZERO_V_FLAG) {
              MC68EC030.m30DivZeroVFlag = false;
            }

            RegisterList.drpSetMPU ();
            mpuSFC = mpuDFC = mpuCACR = mpuBUSCR = mpuUSP = mpuVBR = mpuCAAR = mpuMSP = mpuISP = 0;
            mpuPCR = 0x04300500 | MPU_060_REV << 8;
            MC68060.mmuReset ();  //TCR,ITT0,ITT1,DTT0,DTT1,URP,SRP。060→000/030のときアドレス変換をOFFにする必要がある
            //MC68EC030
            mpuIgnoreAddressError = true;
            fpuBox = fpuMotherboardCoprocessor;
            fpuBox.epbReset ();
            fpuFPn = fpuBox.epbFPn;
            mpuCacheOn = (mpuCACR & 0x00000101) != 0;
            mpuSetWait ();
            //
            regSRT1 = regSRT0 = 0;
            regSRS = REG_SR_S;
            regSRM = 0;
            regSRI = REG_SR_I;
            regCCR = 0;
            Arrays.fill (regRn, 0);
            //PCとSSPをRAMから読み出す
            regRn[15] = MainMemory.mmrRls (0x00000000);
            regPC = MainMemory.mmrRls (0x00000004);
            //割り込み
            mpuIMR = 0;
            mpuIRR = 0;
            if (MC68901.MFP_DELAYED_INTERRUPT) {
              mpuDIRR = 0;
            }
            mpuISR = 0;
            //動作開始
            mpuStart ();
          }
        }, TMR_DELAY);

      } else {  //MC68EC030→MC68000

        //MC68EC030を停止する
        if (mpuTask != null) {
          mpuClockLimit = 0L;
          System.out.println (Multilingual.mlnJapanese ? "MC68EC030 を停止します" : "MC68EC030 stops");
          mpuTask.cancel ();
          mpuTask = null;
        }

        //MC68000を起動する
        tmrTimer.schedule (new TimerTask () {
          @Override public void run () {

            //8bit I/Oポートのメモリマップの設定(X68000)
            busSetPortMap_00 ();

            //MPU
            currentIsSecond = false;
            currentMPU = currentFirstMPU;
            mpuSetCurrentClock (specifiedFirstClock);

            RegisterList.drpSetMPU ();
            mpuSFC = mpuDFC = mpuCACR = mpuBUSCR = mpuUSP = mpuVBR = mpuCAAR = mpuMSP = mpuISP = 0;
            mpuPCR = 0x04300500 | MPU_060_REV << 8;
            MC68060.mmuReset ();  //TCR,ITT0,ITT1,DTT0,DTT1,URP,SRP。060→000/030のときアドレス変換をOFFにする必要がある
            //MC68000
            mpuIgnoreAddressError = false;
            mpuCacheOn = false;
            mpuSetWait ();
            //
            regSRT1 = regSRT0 = 0;
            regSRS = REG_SR_S;
            regSRM = 0;
            regSRI = REG_SR_I;
            regCCR = 0;
            //HALTしたときのPCとRnを復元する
            regPC = xt3SavedPC;
            System.arraycopy (xt3SavedRn, 0, regRn, 0, 16);
            //割り込み
            mpuIMR = 0;
            mpuIRR = 0;
            if (MC68901.MFP_DELAYED_INTERRUPT) {
              mpuDIRR = 0;
            }
            mpuISR = 0;
            //動作開始
            mpuStart ();
          }
        }, TMR_DELAY);

      }
    }
  }



  //========================================================================================
  //$$MDL 機種

  public static JMenu mdlMenu;  //メニュー

  public static JRadioButtonMenuItem mdlShodaiMenuItem;
  public static JRadioButtonMenuItem mdlACEMenuItem;
  public static JRadioButtonMenuItem mdlEXPERTMenuItem;
  public static JRadioButtonMenuItem mdlPROMenuItem;
  public static JRadioButtonMenuItem mdlSUPERMenuItem;
  public static JRadioButtonMenuItem mdlXVIMenuItem;
  public static JRadioButtonMenuItem mdlXellent30MenuItem;
  public static JRadioButtonMenuItem mdlCompactMenuItem;
  public static JRadioButtonMenuItem mdlHybridMenuItem;
  public static JRadioButtonMenuItem mdl060turboPROMenuItem;
  public static JRadioButtonMenuItem mdlX68030MenuItem;
  public static JRadioButtonMenuItem mdl030CompactMenuItem;
  public static JRadioButtonMenuItem mdl060turboMenuItem;
  public static JCheckBoxMenuItem mdlMC68010MenuItem;

  public static JMenu coproFPUMenu;

  //アクセラレータ
  public static final int ACCELERATOR_HYBRID      = 1;  //Compact+33.3MHz+IPLROM 1.6
  public static final int ACCELERATOR_XELLENT30   = 2;  //XVI+secondMPU
  public static final int ACCELERATOR_060TURBO    = 3;  //X68030+MC68060
  public static final int ACCELERATOR_060TURBOPRO = 4;  //PRO+MC68060
  public static final double MHZ_HYBRID_VALUE = 100.0 / 3.0;  //Hybridの動作周波数。33.3MHz
  public static final String MHZ_HYBRID_STRING = "33.3";
  public static final double MHZ_060TURBO_VALUE = 50.0;  //060turbo/060turboPROの動作周波数
  public static final String MHZ_060TURBO_STRING = "50";

  //指定された値
  //  機種とアクセラレータ
  public static Model specifiedModel;  //指定された機種
  public static int specifiedAccelerator;  //指定されたアクセラレータ
  public static boolean mpu010;  //false=68000を使う,true=68010を使う
  //  MPU
  public static boolean specifiedIsSecond;  //指定されたMPUは2番目か
  public static int specifiedFirstMPU;  //指定された1番目のMPU。Model.MPU_MC68000～Model.MPU_MC68060
  public static int specifiedSecondMPU;  //指定された2番目のMPU。Model.MPU_MC68000～Model.MPU_MC68060
  public static int specifiedMPU;  //指定されたMPU。specifiedIsSecond?specifiedSecondMPU:specifiedFirstMPU
  //  クロック(MHz)
  public static double specifiedFirstClock;  //指定された1番目のクロック(MHz)
  public static double specifiedSecondClock;  //指定された2番目のクロック(MHz)
  public static double specifiedClock;  //指定されたクロック(MHz)。specifiedIsSecond?specifiedSecondClock:specifiedFirstClock
  //  コプロセッサとオンチップFPU
  public static int specifiedCopro0;  //マザーボードコプロセッサ。0=なし,1=MC68881,2=MC68882,7=フルスペック,+8=三倍精度
  public static int specifiedCopro1;  //拡張コプロセッサ#1。0=なし,1=MC68881,2=MC68882,7=フルスペック,+8=三倍精度
  public static int specifiedCopro2;  //拡張コプロセッサ#2。0=なし,1=MC68881,2=MC68882,7=フルスペック,+8=三倍精度
  public static int specifiedOnchipFPU;  //オンチップFPU。0=なし,6=MC68060,7=フルスペック,+8=三倍精度

  //現在の値
  //  機種とアクセラレータ
  public static Model currentModel;  //現在の機種
  public static int currentAccelerator;  //現在のアクセラレータ
  //  MPU
  public static boolean currentIsSecond;  //現在のMPUは2番目か
  public static int currentFirstMPU;  //現在の1番目のMPU。Model.MPU_MC68000～Model.MPU_MC68060
  public static int currentSecondMPU;  //現在の2番目のMPU。Model.MPU_MC68000～Model.MPU_MC68060
  public static int currentMPU;  //現在のMPU。currentIsSecond?currentSecondMPU:currentFirstMPU
  //  クロック(MHz)は指定された値を用いる
  //  コプロセッサとオンチップFPU
  public static int currentCopro0;  //マザーボードコプロセッサ。0=なし,1=MC68881,2=MC68882,7=フルスペック,+8=三倍精度
  public static int currentCopro1;  //拡張コプロセッサ#1。0=なし,1=MC68881,2=MC68882,7=フルスペック,+8=三倍精度
  public static int currentCopro2;  //拡張コプロセッサ#2。0=なし,1=MC68881,2=MC68882,7=フルスペック,+8=三倍精度
  public static int currentOnchipFPU;  //オンチップFPU。0=なし,6=MC68060,7=フルスペック,+8=三倍精度

  //mdlInit ()
  //  機種の指定を読み取る
  public static void mdlInit () {

    //機種とアクセラレータ
    specifiedModel = Model.COMPACT;
    specifiedAccelerator = ACCELERATOR_HYBRID;
    mpu010 = Settings.sgsGetOnOff ("mpu010");
    {
      String paramModel = Settings.sgsGetString ("model");
      switch (paramModel.toLowerCase ()) {
      case "":
      case "none":
      case "hybrid":
        specifiedModel = Model.COMPACT;
        specifiedAccelerator = ACCELERATOR_HYBRID;
        break;
      case "xellent30":
        specifiedModel = Model.XVI;
        specifiedAccelerator = ACCELERATOR_XELLENT30;
        break;
      case "060turbo":
        specifiedModel = Model.X68030;
        specifiedAccelerator = ACCELERATOR_060TURBO;
        break;
      case "060turbopro":
        specifiedModel = Model.PRO;
        specifiedAccelerator = ACCELERATOR_060TURBOPRO;
        break;
      default:
        Model model = Model.fromTypeOrSynonym (paramModel);
        if (model != null) {
          specifiedModel = model;
          specifiedAccelerator = 0;
        } else {
          System.out.println (Multilingual.mlnJapanese ?
                              paramModel + " は不明な機種です" :
                              paramModel + " is unknown model");
          specifiedModel = Model.COMPACT;
          specifiedAccelerator = ACCELERATOR_HYBRID;
        }
      }
    }
    //MPU
    specifiedIsSecond = false;
    specifiedFirstMPU = specifiedModel.getMPU ();
    specifiedSecondMPU = Model.MPU_MC68EC030;
    {
      String[] paramMPUs = Settings.sgsGetString ("mpu").split (",");
      for (int i = 0; i < 2; i++) {
        int mpu = 0;
        String paramMPU = i < paramMPUs.length ? paramMPUs[i] : "";
        switch (paramMPU) {
        case "":
        case "none":
        case "-1":
          mpu = (i == 0 ?
                 (specifiedAccelerator == ACCELERATOR_060TURBO ||
                  specifiedAccelerator == ACCELERATOR_060TURBOPRO ? Model.MPU_MC68060 :
                  specifiedModel.getMPU ()) :
                 Model.MPU_MC68EC030);
          break;
        case "0":
        case "68000":
        case "mc68000":
          mpu = Model.MPU_MC68000;
          break;
        case "1":
        case "68010":
        case "mc68010":
          mpu = Model.MPU_MC68010;
          break;
          //case "2":
          //case "68020":
          //case "mc68020":
          //  specifiedFirstMPU = Model.MPU_MC68020;
          //  break;
        case "3":
        case "68ec030":
        case "mc68ec030":
          mpu = Model.MPU_MC68EC030;
          break;
          //case "68030":
          //case "mc68030":
          //  mpu = Model.MPU_MC68030;
          //  break;
          //case "68lc040":
          //case "mc68lc040":
          //  mpu = Model.MPU_MC68LC040;
          //  break;
          //case "4":
          //case "68040":
          //case "mc68040":
          //  mpu = Model.MPU_MC68040;
          //  break;
          //case "68lc060":
          //case "mc68lc060":
          //  mpu = Model.MPU_MC68LC060;
          //  break;
        case "6":
        case "68060":
        case "mc68060":
          mpu = Model.MPU_MC68060;
          break;
        default:
          Model model = Model.fromTypeOrSynonym (paramMPU);
          if (model != null) {
            mpu = model.getMPU ();
          } else {
            System.out.println (Multilingual.mlnJapanese ?
                                paramMPU + " は不明な MPU です" :
                                paramMPU + " is unknown MPU");
            mpu = specifiedModel.getMPU ();
          }
        }  //switch
        if (i == 0) {
          specifiedFirstMPU = mpu;
        } else {
          specifiedSecondMPU = mpu;
        }
      }  //for i
    }
    specifiedMPU = specifiedIsSecond ? specifiedSecondMPU : specifiedFirstMPU;
    //クロック(MHz)
    specifiedFirstClock = specifiedModel.getClock ();
    specifiedSecondClock = specifiedFirstClock * 2.0;
    {
      String[] paramClocks = Settings.sgsGetString ("clock").split (",");
      for (int i = 0; i < 2; i++) {
        double clock = 0.0;
        String paramClock = i < paramClocks.length ? paramClocks[i] : "";
        switch (paramClock.toLowerCase ()) {
        case "":
        case "none":
        case "-1":
          clock = (i == 0 ?
                   (specifiedAccelerator == ACCELERATOR_HYBRID ? MHZ_HYBRID_VALUE :  //33.3MHz
                    specifiedAccelerator == ACCELERATOR_060TURBO ||
                    specifiedAccelerator == ACCELERATOR_060TURBOPRO ? MHZ_060TURBO_VALUE :
                    specifiedModel.getClock ()) :
                   specifiedFirstClock * 2.0);
          break;
        case "hybrid":
          clock = MHZ_HYBRID_VALUE;  //33.3MHz
          break;
        case "060turbo":
        case "060turbopro":
          clock = MHZ_060TURBO_VALUE;
          break;
        case "16.7":
        case "xellent30":
          clock = 50.0 / 3.0;  //16.7MHz
          break;
        case "33.3":
          clock = 100.0 / 3.0;  //33.3MHz
          break;
        case "66.7":
          clock = 200.0 / 3.0;  //66.7MHz
          break;
        default:
          if (paramClock.matches ("^(?:" +
                                  "[-+]?" +  //符号
                                  "(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)" +  //仮数部
                                  "(?:[Ee][-+]?[0-9]+)?" +  //指数部
                                  ")$")) {
            double d = Double.parseDouble (paramClock);
            if (1.0 <= d && d <= 1000.0) {
              clock = d;
            }
          } else {
            System.out.println (Multilingual.mlnJapanese ?
                                paramClock + " は不明な動作周波数です" :
                                paramClock + " is unknown clock frequency");
            clock = specifiedModel.getClock ();
          }
        }  //switch
        if (i == 0) {
          specifiedFirstClock = clock;
        } else {
          specifiedSecondClock = clock;
        }
      }  //for i
    }
    specifiedClock = specifiedIsSecond ? specifiedSecondClock : specifiedFirstClock;
    //コプロセッサとオンチップFPU
    specifiedCopro0 = 15 & Settings.sgsGetInt ("copro0", 2);
    if (!((7 & specifiedCopro0) == 0 ||
          (7 & specifiedCopro0) == 1 ||
          (7 & specifiedCopro0) == 2 ||
          (7 & specifiedCopro0) == 7)) {
      specifiedCopro0 = (8 & specifiedCopro0) | 2;
    }
    specifiedCopro1 = 15 & Settings.sgsGetInt ("copro1", 2);
    if (!((7 & specifiedCopro1) == 0 ||
          (7 & specifiedCopro1) == 1 ||
          (7 & specifiedCopro1) == 2 ||
          (7 & specifiedCopro1) == 7)) {
      specifiedCopro1 = (8 & specifiedCopro1) | 2;
    }
    specifiedCopro2 = 15 & Settings.sgsGetInt ("copro2", 2);
    if (!((7 & specifiedCopro2) == 0 ||
          (7 & specifiedCopro2) == 1 ||
          (7 & specifiedCopro2) == 2 ||
          (7 & specifiedCopro2) == 7)) {
      specifiedCopro2 = (8 & specifiedCopro2) | 2;
    }
    specifiedOnchipFPU = 15 & Settings.sgsGetInt ("onchipfpu", 7);
    if (!((7 & specifiedOnchipFPU) == 0 ||
          (7 & specifiedOnchipFPU) == 6 ||
          (7 & specifiedOnchipFPU) == 7)) {
      specifiedOnchipFPU = (8 & specifiedOnchipFPU) | 7;
    }
    //現在の値にコピーしておく
    //  機種とアクセラレータ
    currentModel = specifiedModel;
    currentAccelerator = specifiedAccelerator;
    //  MPU
    currentIsSecond = specifiedIsSecond;
    currentFirstMPU = specifiedFirstMPU;
    currentSecondMPU = specifiedSecondMPU;
    currentMPU = specifiedMPU;
    //  クロック(MHz)
    //  コプロセッサとオンチップFPU
    currentCopro0 = specifiedCopro0;
    currentCopro1 = specifiedCopro1;
    currentCopro2 = specifiedCopro2;
    currentOnchipFPU = specifiedOnchipFPU;

    //任意の負荷率
    mpuUtilOn = Settings.sgsGetOnOff ("util");  //任意の負荷率
    mpuUtilRatio = fmtParseInt (Settings.sgsGetString ("ratio"), 0, 1, 100, 100);  //任意の負荷率
    //任意の周波数
    mpuArbFreqMHz = fmtParseInt (Settings.sgsGetString ("mhz"), 0, 1, 1000, 100);
    if (mpuUtilOn) {
      mpuArbFreqOn = false;
    } else {
      mpuArbFreqOn = !(specifiedClock == 10.0 ||
                       specifiedClock == 50.0 / 3.0 ||  //16.7MHz
                       specifiedClock == 25.0 ||
                       specifiedClock == 100.0 / 3.0 ||  //33.3MHz
                       specifiedClock == 50.0 ||
                       specifiedClock == 200.0 / 3.0 ||  //66.7MHz
                       specifiedClock == 75.0 ||
                       specifiedClock == 100.0);
      if (mpuArbFreqOn) {
        mpuArbFreqMHz = (int) specifiedClock;
      }
    }

    //ウェイトサイクル
    mpuROMWaitCycles = 0;
    mpuRAMWaitCycles = 0;
    mpuCacheOn = false;
    //mpuNoWaitTime = new WaitTime ();
    mpuNoWaitTime.ram = 0;
    mpuNoWaitTime.gvram = 0;
    mpuNoWaitTime.tvram = 0;
    mpuNoWaitTime.crtc = 0;
    mpuNoWaitTime.palet = 0;
    mpuNoWaitTime.vicon = 0;
    mpuNoWaitTime.dmac = 0;
    mpuNoWaitTime.mfp = 0;
    mpuNoWaitTime.rtc = 0;
    mpuNoWaitTime.prnport = 0;
    mpuNoWaitTime.sysport = 0;
    mpuNoWaitTime.opm = 0;
    mpuNoWaitTime.adpcm = 0;
    mpuNoWaitTime.fdc = 0;
    mpuNoWaitTime.fdd = 0;
    mpuNoWaitTime.hdc = 0;
    mpuNoWaitTime.scc = 0;
    mpuNoWaitTime.ppi = 0;
    mpuNoWaitTime.ioi = 0;
    mpuNoWaitTime.sprc = 0;
    mpuNoWaitTime.sram = 0;
    mpuNoWaitTime.rom = 0;
    mpuNoWaitTime.ramlong = mpuNoWaitTime.ram << 1;
    mpuNoWaitTime.romlong = mpuNoWaitTime.rom << 1;
    //dmaNoWaitTime = new WaitTime ();
    dmaNoWaitTime.ram = 0;
    dmaNoWaitTime.gvram = 0;
    dmaNoWaitTime.tvram = 0;
    dmaNoWaitTime.crtc = 0;
    dmaNoWaitTime.palet = 0;
    dmaNoWaitTime.vicon = 0;
    dmaNoWaitTime.dmac = 0;
    dmaNoWaitTime.mfp = 0;
    dmaNoWaitTime.rtc = 0;
    dmaNoWaitTime.prnport = 0;
    dmaNoWaitTime.sysport = 0;
    dmaNoWaitTime.opm = 0;
    dmaNoWaitTime.adpcm = 0;
    dmaNoWaitTime.fdc = 0;
    dmaNoWaitTime.fdd = 0;
    dmaNoWaitTime.hdc = 0;
    dmaNoWaitTime.scc = 0;
    dmaNoWaitTime.ppi = 0;
    dmaNoWaitTime.ioi = 0;
    dmaNoWaitTime.sprc = 0;
    dmaNoWaitTime.sram = 0;
    dmaNoWaitTime.rom = 0;
    dmaNoWaitTime.ramlong = dmaNoWaitTime.ram << 1;
    dmaNoWaitTime.romlong = dmaNoWaitTime.rom << 1;
    //mpuWaitTime = new WaitTime ();
    //dmaWaitTime = new WaitTime ();
    busWaitCyclesRequest = Settings.sgsGetOnOff ("waitcycles");
    busWaitCycles = busWaitCyclesRequest;
    busWaitTime = busWaitCycles ? mpuWaitTime : mpuNoWaitTime;

  }  //mdlInit()

  public static void mdlTini () {
    //機種とアクセラレータ
    Settings.sgsPutString ("model",
                           specifiedAccelerator == ACCELERATOR_HYBRID ? "Hybrid" :
                           specifiedAccelerator == ACCELERATOR_XELLENT30 ? "Xellent30" :
                           specifiedAccelerator == ACCELERATOR_060TURBO ? "060turbo" :
                           specifiedAccelerator == ACCELERATOR_060TURBOPRO ? "060turboPRO" :
                           specifiedModel.getSynonym () != null ? specifiedModel.getSynonym () :
                           specifiedModel.getType ());
    Settings.sgsPutOnOff ("mpu010", mpu010);
    //MPU
    //  機種のデフォルトと同じときは省略する
    int defaultFirstMPU = (specifiedAccelerator == ACCELERATOR_060TURBO ||
                           specifiedAccelerator == ACCELERATOR_060TURBOPRO ? Model.MPU_MC68060 :
                           specifiedModel.getMPU ());
    int defaultSecondMPU = Model.MPU_MC68EC030;
    Settings.sgsPutString ("mpu",
                           (specifiedFirstMPU == defaultFirstMPU ? "" :
                            Model.mpuNameOf (specifiedFirstMPU)) +
                           (specifiedSecondMPU == defaultSecondMPU ? "" :
                            "," + Model.mpuNameOf (specifiedSecondMPU)));
    //クロック
    //  機種のデフォルトと同じときは省略する
    //  16.7MHzと33.3MHzは機種名で書くと正確に保存できる
    //  現在のクロックを保存する
    double defaultFirstClock = (specifiedAccelerator == ACCELERATOR_HYBRID ? MHZ_HYBRID_VALUE :  //33.3MHz
                                specifiedAccelerator == ACCELERATOR_060TURBO ||
                                specifiedAccelerator == ACCELERATOR_060TURBOPRO ? MHZ_060TURBO_VALUE :
                                specifiedModel.getClock ());
    double defaultSecondClock = defaultFirstClock * 2.0;
    Settings.sgsPutString ("clock",
                           (specifiedFirstClock == defaultFirstClock ? "" :
                            specifiedFirstClock == 50.0 / 3.0 ? "16.7" :  //16.7MHz
                            specifiedFirstClock == 100.0 / 3.0 ? "33.3" :  //33.3MHz
                            specifiedFirstClock == 200.0 / 3.0 ? "66.7" :  //66.7MHz
                            String.valueOf ((int) specifiedFirstClock)) +
                           (specifiedSecondClock == defaultSecondClock ? "" :
                            "," + (specifiedSecondClock == 50.0 / 3.0 ? "16.7" :  //16.7MHz
                                   specifiedSecondClock == 100.0 / 3.0 ? "33.3" :  //33.3MHz
                                   specifiedSecondClock == 200.0 / 3.0 ? "66.7" :  //66.7MHz
                                   String.valueOf ((int) specifiedSecondClock))));
    //コプロセッサとオンチップFPU
    Settings.sgsPutInt ("copro0", specifiedCopro0);
    Settings.sgsPutInt ("copro1", specifiedCopro1);
    Settings.sgsPutInt ("copro2", specifiedCopro2);
    Settings.sgsPutInt ("onchipfpu", specifiedOnchipFPU);
    //任意の負荷率
    Settings.sgsPutOnOff ("util",
                          mpuUtilOn);
    Settings.sgsPutString ("ratio",
                           String.valueOf (mpuUtilRatio));
    //任意の周波数
    Settings.sgsPutString ("mhz",
                           String.valueOf (mpuArbFreqMHz));
    //ウェイトサイクル
    Settings.sgsPutOnOff ("waitcycles", busWaitCyclesRequest);
  }

  public static void mdlMakeMenu () {

    //アクションリスナー
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        switch (ae.getActionCommand ()) {
        case "X68000 (10MHz)":
          mdlRequestModel (Model.SHODAI, 0);
          mpuReset (-1, -1);
          break;
        case "X68000 ACE (10MHz)":
          mdlRequestModel (Model.ACE, 0);
          mpuReset (-1, -1);
          break;
        case "X68000 EXPERT (10MHz)":
          mdlRequestModel (Model.EXPERT, 0);
          mpuReset (-1, -1);
          break;
        case "X68000 PRO (10MHz)":
          mdlRequestModel (Model.PRO, 0);
          mpuReset (-1, -1);
          break;
        case "X68000 SUPER (10MHz)":
          mdlRequestModel (Model.SUPER, 0);
          mpuReset (-1, -1);
          break;
        case "X68000 XVI (16.7MHz)":
          mdlRequestModel (Model.XVI, 0);
          mpuReset (-1, -1);
          break;
        case "X68000 Compact (16.7MHz)":
          mdlRequestModel (Model.COMPACT, 0);
          mpuReset (-1, -1);
          break;
          //
        case "X68030 (25MHz)":
          mdlRequestModel (Model.X68030, 0);
          mpuReset (-1, -1);
          break;
        case "X68030 Compact (25MHz)":
          mdlRequestModel (Model.X68030COMPACT, 0);
          mpuReset (-1, -1);
          break;
          //
        case "X68000 Hybrid (" + MHZ_HYBRID_STRING + "MHz)":
          mdlRequestModel (Model.COMPACT, ACCELERATOR_HYBRID);
          mpuReset (-1, -1);
          break;
        case "Xellent30 (33.3MHz)":
          mdlRequestModel (Model.XVI, ACCELERATOR_XELLENT30);
          mpuReset (-1, -1);
          break;
        case "060turbo (" + MHZ_060TURBO_STRING + "MHz)":
          mdlRequestModel (Model.X68030, ACCELERATOR_060TURBO);
          mpuReset (-1, -1);
          break;
        case "060turboPRO (" + MHZ_060TURBO_STRING + "MHz)":
          mdlRequestModel (Model.PRO, ACCELERATOR_060TURBOPRO);
          mpuReset (-1, -1);
          break;
          //
        case "MC68010":
          mpu010 = ((JCheckBoxMenuItem) source).isSelected ();
          break;

        }
      }
    };

    //メニュー
    ButtonGroup modelGroup = new ButtonGroup ();
    mdlMenu = Multilingual.mlnText (
      ComponentFactory.createMenu (
        "Change the model and reset",
        mdlShodaiMenuItem = ComponentFactory.createRadioButtonMenuItem (
          modelGroup,
          specifiedModel == Model.SHODAI,
          "X68000 (10MHz)",
          listener),
        mdlACEMenuItem = ComponentFactory.createRadioButtonMenuItem (
          modelGroup,
          specifiedModel == Model.ACE,
          "X68000 ACE (10MHz)",
          listener),
        mdlEXPERTMenuItem = ComponentFactory.createRadioButtonMenuItem (
          modelGroup,
          specifiedModel == Model.EXPERT,
          "X68000 EXPERT (10MHz)",
          listener),
        mdlPROMenuItem = ComponentFactory.createRadioButtonMenuItem (
          modelGroup,
          specifiedModel == Model.PRO && specifiedAccelerator == 0,
          "X68000 PRO (10MHz)",
          listener),
        mdlSUPERMenuItem = ComponentFactory.createRadioButtonMenuItem (
          modelGroup,
          specifiedModel == Model.SUPER,
          "X68000 SUPER (10MHz)",
          listener),
        mdlXVIMenuItem = ComponentFactory.createRadioButtonMenuItem (
          modelGroup,
          specifiedModel == Model.XVI && specifiedAccelerator == 0,
          "X68000 XVI (16.7MHz)",
          listener),
        mdlCompactMenuItem = ComponentFactory.createRadioButtonMenuItem (
          modelGroup,
          specifiedModel == Model.COMPACT && specifiedAccelerator == 0,
          "X68000 Compact (16.7MHz)",
          listener),
        //
        ComponentFactory.createHorizontalSeparator (),
        //
        mdlX68030MenuItem = ComponentFactory.createRadioButtonMenuItem (
          modelGroup,
          specifiedModel == Model.X68030 && specifiedAccelerator == 0,
          "X68030 (25MHz)",
          listener),
        mdl030CompactMenuItem = ComponentFactory.createRadioButtonMenuItem (
          modelGroup,
          specifiedModel == Model.X68030COMPACT,
          "X68030 Compact (25MHz)",
          listener),
        //
        ComponentFactory.createHorizontalSeparator (),
        //
        mdlHybridMenuItem = ComponentFactory.createRadioButtonMenuItem (
          modelGroup,
          specifiedModel == Model.COMPACT && specifiedAccelerator == ACCELERATOR_HYBRID,
          "X68000 Hybrid (" + MHZ_HYBRID_STRING + "MHz)",
          listener),
        mdlXellent30MenuItem = ComponentFactory.createRadioButtonMenuItem (
          modelGroup,
          specifiedModel == Model.XVI && specifiedAccelerator == ACCELERATOR_XELLENT30,
          "Xellent30 (33.3MHz)",
          listener),
        mdl060turboMenuItem = ComponentFactory.createRadioButtonMenuItem (
          modelGroup,
          specifiedModel == Model.X68030 && specifiedAccelerator == ACCELERATOR_060TURBO,
          "060turbo (" + MHZ_060TURBO_STRING + "MHz)",
          listener),
        mdl060turboPROMenuItem = ComponentFactory.createRadioButtonMenuItem (
          modelGroup,
          specifiedModel == Model.PRO && specifiedAccelerator == ACCELERATOR_060TURBOPRO,
          "060turboPRO (" + MHZ_060TURBO_STRING + "MHz)",
          listener),
        //
        ComponentFactory.createHorizontalSeparator (),
        //
        mdlMC68010MenuItem = ComponentFactory.createCheckBoxMenuItem (
          mpu010,
          "MC68010",
          listener)
        ),
      "ja", "機種を変更してリセット");

    //コプロセッサとオンチップFPU
    ActionListener copro0Listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        String command = ae.getActionCommand ();
        switch (command) {
        case "Not installed":
          specifiedCopro0 = (8 & specifiedCopro0) | 0;
          break;
        case "MC68881":
          specifiedCopro0 = (8 & specifiedCopro0) | 1;
          break;
        case "MC68882":
          specifiedCopro0 = (8 & specifiedCopro0) | 2;
          break;
        case "Full specification":
          specifiedCopro0 = (8 & specifiedCopro0) | 7;
          break;
        case "Extended precision (19 digits)":
          specifiedCopro0 = 0 | (7 & specifiedCopro0);
          break;
        case "Triple precision (24 digits)":
          specifiedCopro0 = 8 | (7 & specifiedCopro0);
          break;
        default:
          System.out.println ("unknown action command " + command);
        }
      }
    };
    //
    ActionListener copro1Listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        String command = ae.getActionCommand ();
        switch (command) {
        case "Not installed":
          specifiedCopro1 = (8 & specifiedCopro1) | 0;
          break;
        case "MC68881":
          specifiedCopro1 = (8 & specifiedCopro1) | 1;
          break;
        case "MC68882":
          specifiedCopro1 = (8 & specifiedCopro1) | 2;
          break;
        case "Full specification":
          specifiedCopro1 = (8 & specifiedCopro1) | 7;
          break;
        case "Extended precision (19 digits)":
          specifiedCopro1 = 0 | (7 & specifiedCopro1);
          break;
        case "Triple precision (24 digits)":
          specifiedCopro1 = 8 | (7 & specifiedCopro1);
          break;
        default:
          System.out.println ("unknown action command " + command);
        }
      }
    };
    //
    ActionListener copro2Listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        String command = ae.getActionCommand ();
        switch (command) {
        case "Not installed":
          specifiedCopro2 = (8 & specifiedCopro2) | 0;
          break;
        case "MC68881":
          specifiedCopro2 = (8 & specifiedCopro2) | 1;
          break;
        case "MC68882":
          specifiedCopro2 = (8 & specifiedCopro2) | 2;
          break;
        case "Full specification":
          specifiedCopro2 = (8 & specifiedCopro2) | 7;
          break;
        case "Extended precision (19 digits)":
          specifiedCopro2 = 0 | (7 & specifiedCopro2);
          break;
        case "Triple precision (24 digits)":
          specifiedCopro2 = 8 | (7 & specifiedCopro2);
          break;
        default:
          System.out.println ("unknown action command " + command);
        }
      }
    };
    //
    ActionListener onchipFPUListener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        String command = ae.getActionCommand ();
        switch (command) {
        case "Not installed":
          specifiedOnchipFPU = (8 & specifiedOnchipFPU) | 0;
          break;
        case "MC68060":
          specifiedOnchipFPU = (8 & specifiedOnchipFPU) | 6;
          break;
        case "Full specification":
          specifiedOnchipFPU = (8 & specifiedOnchipFPU) | 7;
          break;
        case "Extended precision (19 digits)":
          specifiedOnchipFPU = 0 | (7 & specifiedOnchipFPU);
          break;
        case "Triple precision (24 digits)":
          specifiedOnchipFPU = 8 | (7 & specifiedOnchipFPU);
          break;
        default:
          System.out.println ("unknown action command " + command);
        }
      }
    };
    //
    ButtonGroup copro00Group = new ButtonGroup ();
    ButtonGroup copro01Group = new ButtonGroup ();
    ButtonGroup copro10Group = new ButtonGroup ();
    ButtonGroup copro11Group = new ButtonGroup ();
    ButtonGroup copro20Group = new ButtonGroup ();
    ButtonGroup copro21Group = new ButtonGroup ();
    ButtonGroup onchipFPU0Group = new ButtonGroup ();
    ButtonGroup onchipFPU1Group = new ButtonGroup ();
    //
    coproFPUMenu = Multilingual.mlnText (
      ComponentFactory.createMenu (
        "Coprocessor and on-chip FPU",
        //
        Multilingual.mlnText (
          ComponentFactory.createMenu (
            "Motherboard coprocessor",
            Multilingual.mlnText (
              ComponentFactory.createRadioButtonMenuItem (copro00Group, (7 & specifiedCopro0) == 0, "Not installed", copro0Listener),
              "ja", "なし"),
            ComponentFactory.createRadioButtonMenuItem (copro00Group, (7 & specifiedCopro0) == 1, "MC68881", copro0Listener),
            ComponentFactory.createRadioButtonMenuItem (copro00Group, (7 & specifiedCopro0) == 2, "MC68882", copro0Listener),
            Multilingual.mlnText (
              ComponentFactory.createRadioButtonMenuItem (copro00Group, (7 & specifiedCopro0) == 7, "Full specification", copro0Listener),
              "ja", "フルスペック"),
            ComponentFactory.createHorizontalSeparator (),
            Multilingual.mlnText (
              ComponentFactory.createRadioButtonMenuItem (copro01Group, (8 & specifiedCopro0) == 0, "Extended precision (19 digits)", copro0Listener),
              "ja", "拡張精度 (19 桁)"),
            Multilingual.mlnText (
              ComponentFactory.createRadioButtonMenuItem (copro01Group, (8 & specifiedCopro0) != 0, "Triple precision (24 digits)", copro0Listener),
              "ja", "三倍精度 (24 桁)")),
          "ja", "マザーボードコプロセッサ"),
        //
        Multilingual.mlnText (
          ComponentFactory.createMenu (
            "Extension coprocessor #1",
            Multilingual.mlnText (
              ComponentFactory.createRadioButtonMenuItem (copro10Group, (7 & specifiedCopro1) == 0, "Not installed", copro1Listener),
              "ja", "なし"),
            ComponentFactory.createRadioButtonMenuItem (copro10Group, (7 & specifiedCopro1) == 1, "MC68881", copro1Listener),
            ComponentFactory.createRadioButtonMenuItem (copro10Group, (7 & specifiedCopro1) == 2, "MC68882", copro1Listener),
            Multilingual.mlnText (
              ComponentFactory.createRadioButtonMenuItem (copro10Group, (7 & specifiedCopro1) == 7, "Full specification", copro1Listener),
              "ja", "フルスペック"),
            ComponentFactory.createHorizontalSeparator (),
            Multilingual.mlnText (
              ComponentFactory.createRadioButtonMenuItem (copro11Group, (8 & specifiedCopro1) == 0, "Extended precision (19 digits)", copro1Listener),
              "ja", "拡張精度 (19 桁)"),
            Multilingual.mlnText (
              ComponentFactory.createRadioButtonMenuItem (copro11Group, (8 & specifiedCopro1) != 0, "Triple precision (24 digits)", copro1Listener),
              "ja", "三倍精度 (24 桁)")),
          "ja", "拡張コプロセッサ #1"),
        //
        Multilingual.mlnText (
          ComponentFactory.createMenu (
            "Extension coprocessor #2",
            Multilingual.mlnText (
              ComponentFactory.createRadioButtonMenuItem (copro20Group, (7 & specifiedCopro2) == 0, "Not installed", copro2Listener),
              "ja", "なし"),
            ComponentFactory.createRadioButtonMenuItem (copro20Group, (7 & specifiedCopro2) == 1, "MC68881", copro2Listener),
            ComponentFactory.createRadioButtonMenuItem (copro20Group, (7 & specifiedCopro2) == 2, "MC68882", copro2Listener),
            Multilingual.mlnText (
              ComponentFactory.createRadioButtonMenuItem (copro20Group, (7 & specifiedCopro2) == 7, "Full specification", copro2Listener),
              "ja", "フルスペック"),
            ComponentFactory.createHorizontalSeparator (),
            Multilingual.mlnText (
              ComponentFactory.createRadioButtonMenuItem (copro21Group, (8 & specifiedCopro2) == 0, "Extended precision (19 digits)", copro2Listener),
              "ja", "拡張精度 (19 桁)"),
            Multilingual.mlnText (
              ComponentFactory.createRadioButtonMenuItem (copro21Group, (8 & specifiedCopro2) != 0, "Triple precision (24 digits)", copro2Listener),
              "ja", "三倍精度 (24 桁)")),
          "ja", "拡張コプロセッサ #2"),
        //
        Multilingual.mlnText (
          ComponentFactory.createMenu (
            "On-chip FPU",
            Multilingual.mlnText (
              ComponentFactory.createRadioButtonMenuItem (onchipFPU0Group, (7 & specifiedOnchipFPU) == 0, "Not installed", onchipFPUListener),
              "ja", "なし"),
            ComponentFactory.createRadioButtonMenuItem (onchipFPU0Group, (7 & specifiedOnchipFPU) == 6, "MC68060", onchipFPUListener),
            Multilingual.mlnText (
              ComponentFactory.createRadioButtonMenuItem (onchipFPU0Group, (7 & specifiedOnchipFPU) == 7, "Full specification", onchipFPUListener),
              "ja", "フルスペック"),
            ComponentFactory.createHorizontalSeparator (),
            Multilingual.mlnText (
              ComponentFactory.createRadioButtonMenuItem (onchipFPU1Group, (8 & specifiedOnchipFPU) == 0, "Extended precision (19 digits)", onchipFPUListener),
              "ja", "拡張精度 (19 桁)"),
            Multilingual.mlnText (
              ComponentFactory.createRadioButtonMenuItem (onchipFPU1Group, (8 & specifiedOnchipFPU) != 0, "Triple precision (24 digits)", onchipFPUListener),
              "ja", "三倍精度 (24 桁)")),
          "ja", "オンチップ FPU")),
      "ja", "コプロセッサとオンチップ FPU");

  }  //mdlMakeMenu

  public static void mdlRequestModel (Model model, int accelerator) {
    specifiedModel = model;
    specifiedAccelerator = accelerator;
    //MPU
    specifiedIsSecond = false;
    specifiedFirstMPU = specifiedModel.getMPU ();
    specifiedSecondMPU = Model.MPU_MC68EC030;
    specifiedMPU = specifiedIsSecond ? specifiedSecondMPU : specifiedFirstMPU;
    specifiedFirstClock = specifiedModel.getClock ();
    specifiedSecondClock = specifiedFirstClock * 2.0;
    specifiedClock = specifiedIsSecond ? specifiedSecondClock : specifiedFirstClock;
    //アクセラレータ
    switch (accelerator) {
    case ACCELERATOR_HYBRID:
      specifiedFirstClock = MHZ_HYBRID_VALUE;  //33.3MHz
      specifiedClock = specifiedIsSecond ? specifiedSecondClock : specifiedFirstClock;
      break;
    case ACCELERATOR_XELLENT30:
      break;
    case ACCELERATOR_060TURBO:
    case ACCELERATOR_060TURBOPRO:
      specifiedFirstMPU = Model.MPU_MC68060;
      specifiedFirstClock = MHZ_060TURBO_VALUE;
      specifiedClock = specifiedIsSecond ? specifiedSecondClock : specifiedFirstClock;
    }
    //任意の負荷率と任意の周波数を無効化
    mpuUtilOn = false;
    mpuArbFreqOn = false;
    mpuSetCurrentClock (specifiedClock);
    //機種を変更してリセットのラジオボタンをセット
    if (accelerator == ACCELERATOR_HYBRID) {
      mdlHybridMenuItem.setSelected (true);
    } else if (accelerator == ACCELERATOR_XELLENT30) {
      mdlXellent30MenuItem.setSelected (true);
    } else if (accelerator == ACCELERATOR_060TURBO) {
      mdl060turboMenuItem.setSelected (true);
    } else if (accelerator == ACCELERATOR_060TURBOPRO) {
      mdl060turboPROMenuItem.setSelected (true);
    } else if (specifiedModel == Model.SHODAI) {
      mdlShodaiMenuItem.setSelected (true);
    } else if (specifiedModel == Model.ACE) {
      mdlACEMenuItem.setSelected (true);
    } else if (specifiedModel == Model.EXPERT) {
      mdlEXPERTMenuItem.setSelected (true);
    } else if (specifiedModel == Model.PRO) {
      mdlPROMenuItem.setSelected (true);
    } else if (specifiedModel == Model.SUPER) {
      mdlSUPERMenuItem.setSelected (true);
    } else if (specifiedModel == Model.XVI) {
      mdlXVIMenuItem.setSelected (true);
    } else if (specifiedModel == Model.COMPACT) {
      mdlCompactMenuItem.setSelected (true);
    } else if (specifiedModel == Model.X68030) {
      mdlX68030MenuItem.setSelected (true);
    } else if (specifiedModel == Model.X68030COMPACT) {
      mdl030CompactMenuItem.setSelected (true);
    }
    //FPU
    //SCSI
    HDC.hdcSASIMenuItem.setSelected (!currentModel.isSCSI ());  //Built-in SASI
    SPC.spcSCSIINMenuItem.setSelected (currentModel.isSCSI ());  //Built-in SCSI
  }



  //========================================================================================
  //$$MPU MPU

  //コンパイルスイッチ
  public static final boolean MPU_INLINE_EXCEPTION = true;  //true=例外処理をインライン展開する。速くなる
  public static final boolean MPU_COMPOUND_POSTINCREMENT = false;  //true=(pc)+をbusRbs((pc+=2)-1),busRws((pc+=2)-2),busRls((pc+=4)-4)のように書く。見た目はスマートだが最適化しにくくなる？

  public static final boolean MPU_SWITCH_MISC_OPCODE = false;  //true=RTSなどのswitchのキーはオペコード全体,false=下位6bit
  public static final boolean MPU_SWITCH_BCC_CONDITION = false;  //true=オペコードのswitchでBccをccで分類する
  public static final boolean MPU_SWITCH_BCC_OFFSET = false;  //true=オペコードのswitchでBRA/BSR/Bccを8bitオフセットの上位2bitで分類する
  public static final boolean MPU_SWITCH_SCC_CONDITION = true;  //true=オペコードのswitchでScc/DBRA/DBcc/TRAPccをccで分類する

  public static final boolean MPU_OMIT_EXTRA_READ = false;  //true=余分なリードを省略する。MC68000のBRA.S/BSR.S/Bcc.S/CLR/SF
  public static final boolean MPU_OMIT_OFFSET_READ = false;  //true=条件分岐命令で分岐しないときオフセットのリードを省略する

  //TMR_FREQ単位のタイマカウンタで到達し得ない時刻を表す定数
  //  TMR_FREQ=10^12のとき到達するのに3.5ヶ月かかる
  //  3.5ヶ月間動かしっぱなしにすると破綻する
  public static final long FAR_FUTURE = 0x7fffffffffffffffL;

  //ステータスレジスタ
  //  トレース
  //     srT1    srT0
  //    0x0000  0x0000  トレースなし
  //    0x0000  0x4000  フローの変化をトレース
  //    0x8000  0x0000  すべての命令をトレース
  //    0x8000  0x4000  未定義
  public static final int REG_SR_T1  = 0b10000000_00000000;
  public static final int REG_SR_T0  = 0b01000000_00000000;  //(020/030/040)
  //  モード
  //      srS     srM
  //    0x0000  0x0000  ユーザモード(USPを使用)
  //    0x0000  0x1000  ユーザモード(USPを使用)
  //    0x2000  0x0000  スーパーバイザ割り込みモード(ISPを使用)
  //    0x2000  0x1000  スーパーバイザマスタモード(MSPを使用)
  public static final int REG_SR_S   = 0b00100000_00000000;
  public static final int REG_SR_M   = 0b00010000_00000000;  //(020/030/040/060)
  //  割り込み
  public static final int REG_SR_I   = 0b00000111_00000000;

  //コンディションコードレジスタ
  public static final int REG_CCR_X  = 0b00000000_00010000;
  public static final int REG_CCR_N  = 0b00000000_00001000;
  public static final int REG_CCR_Z  = 0b00000000_00000100;
  public static final int REG_CCR_V  = 0b00000000_00000010;
  public static final int REG_CCR_C  = 0b00000000_00000001;
  public static final int REG_CCR_MASK = REG_CCR_X | REG_CCR_N | REG_CCR_Z | REG_CCR_V | REG_CCR_C;  //CCRの有効なビット

  public static char[] REG_CCRXMAP = "00000000000000001111111111111111".toCharArray ();
  public static char[] REG_CCRNMAP = "00000000111111110000000011111111".toCharArray ();
  public static char[] REG_CCRZMAP = "00001111000011110000111100001111".toCharArray ();
  public static char[] REG_CCRVMAP = "00110011001100110011001100110011".toCharArray ();
  public static char[] REG_CCRCMAP = "01010101010101010101010101010101".toCharArray ();

  //割り込みレベル
  //  順序の変更やレベルの追加はコードの変更が必要
  public static final int MPU_IOI_INTERRUPT_LEVEL = 1;
  public static final int MPU_EB2_INTERRUPT_LEVEL = 2;
  public static final int MPU_DMA_INTERRUPT_LEVEL = 3;
  public static final int MPU_SCC_INTERRUPT_LEVEL = 5;
  public static final int MPU_MFP_INTERRUPT_LEVEL = 6;
  public static final int MPU_SYS_INTERRUPT_LEVEL = 7;
  public static final int MPU_IOI_INTERRUPT_MASK = 0x80 >> MPU_IOI_INTERRUPT_LEVEL;  //0x40
  public static final int MPU_EB2_INTERRUPT_MASK = 0x80 >> MPU_EB2_INTERRUPT_LEVEL;  //0x20
  public static final int MPU_DMA_INTERRUPT_MASK = 0x80 >> MPU_DMA_INTERRUPT_LEVEL;  //0x10
  public static final int MPU_SCC_INTERRUPT_MASK = 0x80 >> MPU_SCC_INTERRUPT_LEVEL;  //0x04
  public static final int MPU_MFP_INTERRUPT_MASK = 0x80 >> MPU_MFP_INTERRUPT_LEVEL;  //0x02
  public static final int MPU_SYS_INTERRUPT_MASK = 0x80 >> MPU_SYS_INTERRUPT_LEVEL;  //0x01

  public static final boolean MPU_INTERRUPT_SWITCH = true;  //true=最上位の割り込みをswitchで判別する

  //コンディションコード
  public static final boolean T = true;
  public static final boolean F = false;
  //  cccc==CCCC_cc                               cccc  cc
  public static final int CCCC_T  = 0b0000;  //0000  T       1                always true
  public static final int CCCC_F  = 0b0001;  //0001  F       0                always false
  public static final int CCCC_HI = 0b0010;  //0010  HI      ~C&~Z            high
  public static final int CCCC_LS = 0b0011;  //0011  LS      C|Z              low or same
  public static final int CCCC_CC = 0b0100;  //0100  CC(HS)  ~C               carry clear (high or same)
  public static final int CCCC_CS = 0b0101;  //0101  CS(LO)  C                carry set (low)
  public static final int CCCC_NE = 0b0110;  //0110  NE      ~Z               not equal
  public static final int CCCC_EQ = 0b0111;  //0111  EQ      Z                equal
  public static final int CCCC_VC = 0b1000;  //1000  VC      ~V               overflow clear
  public static final int CCCC_VS = 0b1001;  //1001  VS      V                overflow set
  public static final int CCCC_PL = 0b1010;  //1010  PL      ~N               plus
  public static final int CCCC_MI = 0b1011;  //1011  MI      N                minus
  public static final int CCCC_GE = 0b1100;  //1100  GE      N&V|~N&~V        greater or equal
  public static final int CCCC_LT = 0b1101;  //1101  LT      N&~V|~N&V        less than
  public static final int CCCC_GT = 0b1110;  //1110  GT      N&V&~Z|~N&~V&~Z  greater than
  public static final int CCCC_LE = 0b1111;  //1111  LE      Z|N&~V|~N&V      less or equal
  //F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,  //X
  //F,F,F,F,F,F,F,F,T,T,T,T,T,T,T,T,F,F,F,F,F,F,F,F,T,T,T,T,T,T,T,T,  //N
  //F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,  //Z
  //F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,  //V
  //F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,  //C
  //  BCCMAP[CCCC_cc<<5|ccr]==trueならば条件成立
  public static final boolean[] BCCMAP = {
    T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,  //T       NF
    F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,  //F       NT
    T,F,T,F,F,F,F,F,T,F,T,F,F,F,F,F,T,F,T,F,F,F,F,F,T,F,T,F,F,F,F,F,  //HI      NLS
    F,T,F,T,T,T,T,T,F,T,F,T,T,T,T,T,F,T,F,T,T,T,T,T,F,T,F,T,T,T,T,T,  //LS      NHI
    T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,  //CC(HS)  NCS(NLO)
    F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,  //CS(LO)  NCC(NHS)
    T,T,T,T,F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,F,F,F,F,  //NE(NZ)  NEQ(NZE)
    F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,  //EQ(ZE)  NNE(NNZ)
    T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,  //VC      NVS
    F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,F,F,T,T,  //VS      NVC
    T,T,T,T,T,T,T,T,F,F,F,F,F,F,F,F,T,T,T,T,T,T,T,T,F,F,F,F,F,F,F,F,  //PL      NMI
    F,F,F,F,F,F,F,F,T,T,T,T,T,T,T,T,F,F,F,F,F,F,F,F,T,T,T,T,T,T,T,T,  //MI      NPL
    T,T,F,F,T,T,F,F,F,F,T,T,F,F,T,T,T,T,F,F,T,T,F,F,F,F,T,T,F,F,T,T,  //GE      NLT
    F,F,T,T,F,F,T,T,T,T,F,F,T,T,F,F,F,F,T,T,F,F,T,T,T,T,F,F,T,T,F,F,  //LT      NGE
    T,T,F,F,F,F,F,F,F,F,T,T,F,F,F,F,T,T,F,F,F,F,F,F,F,F,T,T,F,F,F,F,  //GT      NLE
    F,F,T,T,T,T,T,T,T,T,F,F,T,T,T,T,F,F,T,T,T,T,T,T,T,T,F,F,T,T,T,T,  //LE      NGT
  };

  //  MPU_CCCMAP[cccc<<5|ccr]==(BCCMAP[cccc<<5|ccr]?'1':'0')
  public static final char[] MPU_CCCMAP = (
    "11111111111111111111111111111111" +
    "00000000000000000000000000000000" +
    "10100000101000001010000010100000" +
    "01011111010111110101111101011111" +
    "10101010101010101010101010101010" +
    "01010101010101010101010101010101" +
    "11110000111100001111000011110000" +
    "00001111000011110000111100001111" +
    "11001100110011001100110011001100" +
    "00110011001100110011001100110011" +
    "11111111000000001111111100000000" +
    "00000000111111110000000011111111" +
    "11001100001100111100110000110011" +
    "00110011110011000011001111001100" +
    "11000000001100001100000000110000" +
    "00111111110011110011111111001111").toCharArray ();

  //  (MPU_CC_cc<<ccr<0)==trueならば条件成立
  //  (MPU_CC_cc<<ccr<0)==BCCMAP[CCCC_cc<<5|ccr]
  public static final int MPU_CC_T  = 0b11111111111111111111111111111111;  //T
  public static final int MPU_CC_F  = 0b00000000000000000000000000000000;  //F
  public static final int MPU_CC_HI = 0b10100000101000001010000010100000;  //HI
  public static final int MPU_CC_LS = 0b01011111010111110101111101011111;  //LS
  public static final int MPU_CC_HS = 0b10101010101010101010101010101010;  //HS
  public static final int MPU_CC_LO = 0b01010101010101010101010101010101;  //LO
  public static final int MPU_CC_NE = 0b11110000111100001111000011110000;  //NE
  public static final int MPU_CC_EQ = 0b00001111000011110000111100001111;  //EQ
  public static final int MPU_CC_VC = 0b11001100110011001100110011001100;  //VC
  public static final int MPU_CC_VS = 0b00110011001100110011001100110011;  //VS
  public static final int MPU_CC_PL = 0b11111111000000001111111100000000;  //PL
  public static final int MPU_CC_MI = 0b00000000111111110000000011111111;  //MI
  public static final int MPU_CC_GE = 0b11001100001100111100110000110011;  //GE
  public static final int MPU_CC_LT = 0b00110011110011000011001111001100;  //LT
  public static final int MPU_CC_GT = 0b11000000001100001100000000110000;  //GT
  public static final int MPU_CC_LE = 0b00111111110011110011111111001111;  //LE

  //TST.Bのテーブル
  //  z=255&(～);ccr=ccr&CCR_X|MPU_TSTB_TABLE[z]をz=～;ccr=ccr&CCR_X|MPU_TSTB_TABLE[255&z]にすると速くなることがある
  //  インデックスが明示的にマスクされていると最適化しやすいのだろう
/*
  public static final byte[] MPU_TSTB_TABLE = new byte[256];
  static {
    for (int z = 0; z < 256; z++) {
      MPU_TSTB_TABLE[z] = (byte) (z >> 7 << 3 | z - 1 >> 6 & CCR_Z);
    }
  }  //static
*/
/*
  public static final byte[] MPU_TSTB_TABLE = {
    REG_CCR_Z, 0,         0,         0,         0,         0,         0,         0,
    0,         0,         0,         0,         0,         0,         0,         0,
    0,         0,         0,         0,         0,         0,         0,         0,
    0,         0,         0,         0,         0,         0,         0,         0,
    0,         0,         0,         0,         0,         0,         0,         0,
    0,         0,         0,         0,         0,         0,         0,         0,
    0,         0,         0,         0,         0,         0,         0,         0,
    0,         0,         0,         0,         0,         0,         0,         0,
    0,         0,         0,         0,         0,         0,         0,         0,
    0,         0,         0,         0,         0,         0,         0,         0,
    0,         0,         0,         0,         0,         0,         0,         0,
    0,         0,         0,         0,         0,         0,         0,         0,
    0,         0,         0,         0,         0,         0,         0,         0,
    0,         0,         0,         0,         0,         0,         0,         0,
    0,         0,         0,         0,         0,         0,         0,         0,
    0,         0,         0,         0,         0,         0,         0,         0,
    REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N,
    REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N,
    REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N,
    REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N,
    REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N,
    REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N,
    REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N,
    REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N,
    REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N,
    REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N,
    REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N,
    REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N,
    REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N,
    REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N,
    REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N,
    REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N, REG_CCR_N,
  };
*/
  //  perl misc/itob.pl xeij/XEiJ.java MPU_TSTB_TABLE
  public static final byte[] MPU_TSTB_TABLE = "\4\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b".getBytes (XEiJ.ISO_8859_1);

  //BITREVのテーブル
  //  初期化コードが大きくなりすぎるので最初から展開しておくにはクラスを分ける必要がある
  public static final int[] MPU_BITREV_TABLE_0 = new int[2048];
  public static final int[] MPU_BITREV_TABLE_1 = new int[2048];
  public static final int[] MPU_BITREV_TABLE_2 = new int[2048];
  static {
    for (int i = 0; i < 2048; i++) {
      MPU_BITREV_TABLE_2[i] = (MPU_BITREV_TABLE_1[i] = (MPU_BITREV_TABLE_0[i] = Integer.reverse (i)) >>> 11) >>> 11;
    }
  }

  //アドレッシングモード
  //                                                              data  memory  control  alterable
  public static final int EA_DR = 0b000_000;  //D  Dr             x                      x
  public static final int EA_AR = 0b001_000;  //A  Ar                                    x
  public static final int EA_MM = 0b010_000;  //M  (Ar)           x     x       x        x
  public static final int EA_MP = 0b011_000;  //+  (Ar)+          x     x                x
  public static final int EA_MN = 0b100_000;  //-  -(Ar)          x     x                x
  public static final int EA_MW = 0b101_000;  //W  (d16,Ar)       x     x       x        x
  public static final int EA_MX = 0b110_000;  //X  (d8,Ar,Rn.wl)  x     x       x        x
  public static final int EA_ZW = 0b111_000;  //Z  (xxx).W        x     x       x        x
  public static final int EA_ZL = 0b111_001;  //Z  (xxx).L        x     x       x        x
  public static final int EA_PW = 0b111_010;  //P  (d16,PC)       x     x       x
  public static final int EA_PX = 0b111_011;  //P  (d8,PC,Rn.wl)  x     x       x
  public static final int EA_IM = 0b111_100;  //I  #<data>        x
  public static final int MMM_DR = EA_DR >> 3;
  public static final int MMM_AR = EA_AR >> 3;
  public static final int MMM_MM = EA_MM >> 3;
  public static final int MMM_MP = EA_MP >> 3;
  public static final int MMM_MN = EA_MN >> 3;
  public static final int MMM_MW = EA_MW >> 3;
  public static final int MMM_MX = EA_MX >> 3;
  public static final long EAM_DR = 0xff00000000000000L >>> EA_DR;
  public static final long EAM_AR = 0xff00000000000000L >>> EA_AR;
  public static final long EAM_MM = 0xff00000000000000L >>> EA_MM;
  public static final long EAM_MP = 0xff00000000000000L >>> EA_MP;
  public static final long EAM_MN = 0xff00000000000000L >>> EA_MN;
  public static final long EAM_MW = 0xff00000000000000L >>> EA_MW;
  public static final long EAM_MX = 0xff00000000000000L >>> EA_MX;
  public static final long EAM_ZW = 0x8000000000000000L >>> EA_ZW;
  public static final long EAM_ZL = 0x8000000000000000L >>> EA_ZL;
  public static final long EAM_PW = 0x8000000000000000L >>> EA_PW;
  public static final long EAM_PX = 0x8000000000000000L >>> EA_PX;
  public static final long EAM_IM = 0x8000000000000000L >>> EA_IM;
  public static final long EAM_ALL = EAM_DR|EAM_AR|EAM_MM|EAM_MP|EAM_MN|EAM_MW|EAM_MX|EAM_ZW|EAM_ZL|EAM_PW|EAM_PX|EAM_IM;  //|DAM+-WXZPI|すべて
  public static final long EAM_ALT = EAM_DR|EAM_AR|EAM_MM|EAM_MP|EAM_MN|EAM_MW|EAM_MX|EAM_ZW|EAM_ZL                     ;  //|DAM+-WXZ  |可変
  public static final long EAM_DAT = EAM_DR       |EAM_MM|EAM_MP|EAM_MN|EAM_MW|EAM_MX|EAM_ZW|EAM_ZL|EAM_PW|EAM_PX|EAM_IM;  //|D M+-WXZPI|データ
  public static final long EAM_DME = EAM_DR       |EAM_MM|EAM_MP|EAM_MN|EAM_MW|EAM_MX|EAM_ZW|EAM_ZL|EAM_PW|EAM_PX       ;  //|D M+-WXZP |データレジスタ直接またはメモリ
  public static final long EAM_DLT = EAM_DR       |EAM_MM|EAM_MP|EAM_MN|EAM_MW|EAM_MX|EAM_ZW|EAM_ZL                     ;  //|D M+-WXZ  |データ可変
  public static final long EAM_DCN = EAM_DR       |EAM_MM              |EAM_MW|EAM_MX|EAM_ZW|EAM_ZL|EAM_PW|EAM_PX       ;  //|D M  WXZP |データレジスタ直接または制御
  public static final long EAM_DCL = EAM_DR       |EAM_MM              |EAM_MW|EAM_MX|EAM_ZW|EAM_ZL                     ;  //|D M  WXZ  |データレジスタ直接または制御可変
  public static final long EAM_ANY =               EAM_MM|EAM_MP|EAM_MN|EAM_MW|EAM_MX|EAM_ZW|EAM_ZL|EAM_PW|EAM_PX|EAM_IM;  //|  M+-WXZPI|レジスタ以外
  public static final long EAM_MEM =               EAM_MM|EAM_MP|EAM_MN|EAM_MW|EAM_MX|EAM_ZW|EAM_ZL|EAM_PW|EAM_PX       ;  //|  M+-WXZP |メモリ
  public static final long EAM_MLT =               EAM_MM|EAM_MP|EAM_MN|EAM_MW|EAM_MX|EAM_ZW|EAM_ZL                     ;  //|  M+-WXZ  |メモリ可変
  public static final long EAM_RDL =               EAM_MM|EAM_MP       |EAM_MW|EAM_MX|EAM_ZW|EAM_ZL|EAM_PW|EAM_PX       ;  //|  M+ WXZP |リードリスト
  public static final long EAM_WTL =               EAM_MM       |EAM_MN|EAM_MW|EAM_MX|EAM_ZW|EAM_ZL                     ;  //|  M -WXZ  |ライトリスト
  public static final long EAM_CNT =               EAM_MM              |EAM_MW|EAM_MX|EAM_ZW|EAM_ZL|EAM_PW|EAM_PX       ;  //|  M  WXZP |制御
  public static final long EAM_CLT =               EAM_MM              |EAM_MW|EAM_MX|EAM_ZW|EAM_ZL                     ;  //|  M  WXZ  |制御可変

  //汎用レジスタ
  //  r[15]は現在のスタックポインタ
  //                              ユーザモード      スーパーバイザモード
  //                                            マスタモード  割り込みモード
  //     ユーザスタックポインタ     regRn[15]       mpuUSP         mpuUSP
  //     マスタスタックポインタ      mpuMSP       regRn[15]        mpuMSP
  //    割り込みスタックポインタ     mpuISP        mpuISP        regRn[15]
  //  モードによってmpuUSP,mpuMSP,mpuISPのいずれかをregRn[15]にコピーして使う
  //  他のモードに切り替えるときはregRn[15]をコピー元のmpuUSP,mpuMSP,mpuISPのいずれかに書き戻してから切り替える
  //  例えばユーザモードから割り込みモードへ移行するときはregRn[15]→mpuUSP,mpuISP→regRn[15]とする
  public static final int[] regRn = new int[16 + 1];  //汎用レジスタ。regRn[16]はテンポラリ。レジスタの更新を回避するとき条件分岐する代わりにregRn[16]に書き込む

  //プログラムカウンタ
  public static int regPC;  //プログラムカウンタ
  public static int regPC0;  //実行中の命令の先頭アドレス

  //オペコードレジスタ
  public static int regOC;  //オペコード。16bit、0拡張

  //ステータスレジスタ
  public static int regSRT1;  //ステータスレジスタのT1。0=トレースなし,REG_SR_T1=すべての命令をトレース
  public static int regSRT0;  //ステータスレジスタのT0(020/030/040)。0=トレースなし,REG_SR_T0=フローの変化をトレース
  public static int mpuTraceFlag;  //トレースフラグ。命令実行前にregSRT1をコピーし、分岐命令を実行するときregSRT0をorする。例外が発生したとき0にする。命令実行後に0でなければトレース例外を発動する
  public static int regSRS;  //ステータスレジスタのS。0=ユーザモード,REG_SR_S=スーパーバイザモード
  public static int regSRM;  //ステータスレジスタのM(020/030/040/060)。0=割り込みモード,REG_SR_M=マスタモード。割り込みを開始するときクリアされる
  public static int regSRI;  //ステータスレジスタのI2,I1,I0

  //コンディションコードレジスタ
  public static int regCCR;  //コンディションコードレジスタ。000XNZVC。0のビットは0に固定

  //mpuIMR
  //  インタラプトマスクレジスタ
  //  ビットの並び順は01234567
  //  現在の割り込みマスクレベルよりも高いビットをセットしたマスク
  //    割り込みマスクレベルがnのときレベル1..nの割り込みが禁止されてレベルn+1..7の割り込みが許可される
  //    ただし、割り込みマスクレベルが7のときはレベル7割り込みの処理中でなければレベル7割り込みが許可される
  //  mpuIMR&mpuIRRで最下位の1のビットの割り込みを受け付ける
  //  irpSetSRでmpuIMR&mpuISRで1のビットの割り込みを終了する
  //  mpuIMR=0x7f>>(srI>>8)|~mpuISR&1
  //      srI     mpuIMR    受け付ける割り込みのレベルと順序
  //    0x0700  0b00000000  なし  レベル7割り込みの処理中のとき
  //            0b00000001  7     レベル7割り込みの処理中ではないとき
  //    0x0600  0b00000001  7
  //    0x0500  0b00000011  7 6
  //    0x0400  0b00000111  7 6 5
  //    0x0300  0b00001111  7 6 5 4
  //    0x0200  0b00011111  7 6 5 4 3
  //    0x0100  0b00111111  7 6 5 4 3 2
  //    0x0000  0b01111111  7 6 5 4 3 2 1
  public static int mpuIMR;

  //mpuIRR
  //  インタラプトリクエストレジスタ
  //  ビットの並び順は01234567
  //  デバイスが割り込みを要求するときレベルに対応するビットをセットする
  //  コアが割り込みを受け付けるときレベルに対応するビットをクリアしてデバイスのacknowledge()を呼び出す
  public static int mpuIRR;
  public static int mpuDIRR;  //遅延割り込み要求

  //mpuISR
  //  インタラプトインサービスレジスタ
  //  ビットの並び順は01234567
  //  コアが割り込み処理を開始するときレベルに対応するビットをセットする
  //  割り込みマスクレベルが下がったとき新しいレベルよりも高いレベルの割り込み処理が終了したものとみなし、
  //  レベルに対応するビットをクリアしてデバイスのdone()を呼び出す
  //  done()が呼び出された時点でまだ処理されていない割り込みが残っているデバイスは再度割り込みを要求する
  public static int mpuISR;

  //制御レジスタ
  public static int mpuSFC;    //000  -12346  SFC
  public static int mpuDFC;    //001  -12346  DFC
  public static int mpuCACR;   //002  --2346  CACR
  //protected static int mpuTC;     //003  ----46  TC                        030MMUのTC
  //protected static int mpuITT0;   //004  ----46  ITT0   IACR0 @ MC68EC040  030MMUのTT0
  //protected static int mpuITT1;   //005  ----46  ITT1   IACR1 @ MC68EC040  030MMUのTT1
  //protected static int mpuDTT0;   //006  ----46  DTT0   DACR0 @ MC68EC040
  //protected static int mpuDTT1;   //007  ----46  DTT1   DACR1 @ MC68EC040
  public static int mpuBUSCR;  //008  -----6  BUSCR
  public static int mpuUSP;    //800  -12346  USP    隠れたユーザスタックポインタ。ユーザモードのときはr[15]を参照すること
  public static int mpuVBR;    //801  -12346  VBR    ベクタベースレジスタ
  public static int mpuCAAR;   //802  --23--  CAAR
  public static int mpuMSP;    //803  --234-  MSP    隠れたマスタスタックポインタ。マスタモードのときはr[15]を参照すること
  public static int mpuISP;    //804  --234-  ISP    隠れた割り込みスタックポインタ。割り込みモードのときはr[15]を参照すること
  //protected static int mpuMMUSR;  //805  ----4-  MMUSR                     030MMUのMMUSR
  //protected static int mpuURP;    //806  ----46  URP                       030MMUのCRPの下位32bit
  //protected static int mpuSRP;    //807  ----46  SRP                       030MMUのSRPの下位32bit
  public static int mpuPCR;    //808  -----6  PCR
  //protected static int mpuHCRP;   //                                       030MMUのCRPの上位32bit
  //protected static int mpuHSRP;   //                                       030MMUのSRPの上位32bit

  public static final int MPU_060_REV = 7;  //MC68060のリビジョンナンバー。1=F43G,5=G65V,6=E41J

  //クロック
  //  時刻は開始からの経過時間
  public static long mpuClockTime;  //時刻(TMR_FREQ単位)
  public static long mpuClockLimit;  //タスクの終了時刻
  public static double mpuClockMHz;  //動作周波数の設定値(MHz)
  public static double mpuCurrentMHz;  //動作周波数の現在値(MHz)
  public static int mpuCycleCount;  //命令のサイクル数。実行アドレス計算のサイクル数を含む
  public static long mpuCycleUnit;  //周波数の表示に使用する1サイクルあたりの時間(TMR_FREQ単位)
  public static long mpuModifiedUnit;  //mpuCycleCountの1サイクルあたりの時間(TMR_FREQ単位)。MC68030のときmpuCycleUnit*3/5
  public static long dmaCycleUnit;  //DMAの1サイクルの時間(TMR_FREQ単位)

  //タイマ
  //  mpuTaskはコアをスケジュールしなおすときに毎回作り直す
  public static TimerTask mpuTask;  //null=停止中。null以外=動作中

  //その他
  public static int mpuBootDevice;  //起動デバイス。-1=指定なし
  public static int mpuROMBootHandle;  //ROM起動ハンドル。-1=指定なし
  public static int mpuSavedBootDevice;  //保存されている起動デバイス。-1=保存されていない
  public static int mpuSavedROMBootHandle;  //保存されているROM起動ハンドル。-1=保存されていない

  //コア
  public static boolean mpuIgnoreAddressError;  //true=アドレスエラーを無視する

  //ウェイトサイクル
  public static int mpuROMWaitCycles;  //MPUからROMにアクセスしたときのウェイトのサイクル数。X68000は1。X68030は$00E8E009の上位4bit+2
  public static int mpuRAMWaitCycles;  //MPUからRAMにアクセスしたときのウェイトのサイクル数。X68000は0。X68030は$00E8E009の下位4bit+2ただし0は0
  public static boolean mpuCacheOn;  //true=キャッシュON。MPUからRAMとROMにアクセスしたときのウェイトを0にすることで擬似的にキャッシュが効いているように見せる
  public static final class WaitTime {
    public long ram;
    public long gvram;
    public long tvram;
    public long crtc;
    public long palet;
    public long vicon;
    public long dmac;
    public long mfp;
    public long rtc;
    public long prnport;
    public long sysport;
    public long opm;
    public long adpcm;
    public long fdc;
    public long fdd;
    public long hdc;
    public long scc;
    public long ppi;
    public long ioi;
    public long sprc;
    public long sram;
    public long rom;
    public long ramlong;
    public long romlong;
    public boolean isDma;
    public WaitTime (boolean isDma) {
      this.isDma = isDma;
    }
  }
  public static final WaitTime mpuNoWaitTime = new WaitTime (false);  //MPUウェイトなし
  public static final WaitTime dmaNoWaitTime = new WaitTime (true);  //DMACウェイトなし
  public static final WaitTime mpuWaitTime = new WaitTime (false);  //現在のMPUのウェイト
  public static final WaitTime dmaWaitTime = new WaitTime (true);  //現在のDMACのウェイト
  public static boolean busWaitCyclesRequest;
  public static boolean busWaitCycles;  //true=ウェイトサイクルなし。mpuNoWaitTime/dmaNoWaitTimeを使う,false=ウェイトあり。mpuWaitTime/dmaWaitTimeを使う
  public static WaitTime busWaitTime;  //busWaitCycles?mpuWaitTime:mpuNoWaitTimeまたはbusWaitCycles?dmaWaitTime:dmaNoWaitTime

  //任意の周波数の指定
  public static boolean mpuArbFreqOn;  //true=任意の周波数の指定がある。mpuArbFreqOn&&mpuUtilOnは不可
  public static int mpuArbFreqMHz;  //任意の周波数の指定(MHz)。1～1000
  public static SpinnerNumberModel mpuArbFreqModel;
  public static JSpinner mpuArbFreqSpinner;
  public static JRadioButtonMenuItem mpuArbFreqRadioButtonMenuItem;

  //任意の負荷率の指定
  public static boolean mpuUtilOn;  //true=任意の負荷率の指定がある。mpuArbFreqOn&&mpuUtilOnは不可
  public static int mpuUtilRatio;  //任意の負荷率の指定(%)。1～100
  public static SpinnerNumberModel mpuUtilModel;
  public static JSpinner mpuUtilSpinner;
  public static JRadioButtonMenuItem mpuUtilRadioButtonMenuItem;

  //周波数と負荷率の調整
  public static final int MPU_ADJUSTMENT_INTERVAL = 100;  //間隔。TMR_INTERVAL*MPU_ADJUSTMENT_INTERVAL毎に表示する
  public static int mpuAdjustmentCounter;
  public static long mpuTotalNano;  //コアのスレッドの動作時間(ns)。本来の動作時間の1/10ならば負荷率10%
  public static long mpuLastNano;  //前回の時刻(ns)
  public static double mpuCoreNano1;
  public static double mpuCoreNano2;

  //メニュー
  public static JMenu mpuMenu;
  public static JMenuItem mpuResetMenuItem;
  public static JMenuItem mpuOpt1ResetMenuItem;
  public static JRadioButtonMenuItem mpuClock10MenuItem;
  public static JRadioButtonMenuItem mpuClock16MenuItem;
  public static JRadioButtonMenuItem mpuClock25MenuItem;
  public static JRadioButtonMenuItem mpuClock33MenuItem;
  public static JRadioButtonMenuItem mpuClock50MenuItem;
  public static JRadioButtonMenuItem mpuClock66MenuItem;
  public static JRadioButtonMenuItem mpuClock75MenuItem;
  public static JRadioButtonMenuItem mpuClock100MenuItem;

  //デバッグ
  public static ActionListener mpuDebugActionListener;  //デバッグアクションリスナー
  public static ArrayList<AbstractButton> mpuButtonsRunning;  //MPUが動作中のときだけ有効なボタン
  public static ArrayList<AbstractButton> mpuButtonsStopped;  //MPUが停止中のときだけ有効なボタン
  public static ArrayList<JCheckBox> mpuOriIllegalCheckBoxList;  //ORI.B #$00,D0を不当命令とみなすチェックボックスのリスト
  public static ArrayList<JCheckBox> mpuStopOnErrorCheckBoxList;  //エラーで停止するチェックボックスのリスト
  public static ArrayList<JCheckBox> mpuStopAtStartCheckBoxList;  //実行開始位置で停止するチェックボックスのリスト

  public static int mpuAdvanceCount;  //トレース実行のカウンタ。0以外=トレース実行中
  public static int mpuStepCount;  //ステップ実行のカウンタ。1以上=ステップ実行中。-1=ステップアンティルリターン実行中
  public static boolean mpuContinue;  //自然に終了したときは変化しない。インスタント命令ブレークポイントで停止したときはtrue、それ以外で停止したときはfalse
  public static int mpuUntilReturnSRS;  //ステップアンティルリターン開始時のregSRS
  public static int mpuUntilReturnRP;  //ステップアンティルリターン開始時のmmuSRPまたはmmuURP
  public static int mpuUntilReturnPC0;  //ステップアンティルリターン開始時のregPC0
  public static int mpuUntilReturnSP;  //ステップアンティルリターン開始時のregRn[15]

  //SX-Window ver3.1のバグ対策
  //
  //  無償公開されたSXWIN311.XDFから起動したときリソースファイルから読み込まれるコードに問題がある
  //  マウスカーソルが指しているメニューの項目の番号を返すサブルーチンでd1レジスタの上位ワードが不定のままdivu.w #$0010,d1を実行している
  //  このサブルーチンの引数ではないd1レジスタの上位ワードを0にしてから呼び出さないとメニューを選択できない
  //  実機で露見しないのは直前に呼び出されるサブルーチンが使っているmovem.w <ea>,<list>命令がd1レジスタの上位ワードをほぼ0にしているため
  //  XEiJで露見したのはこの命令の処理が間違っていてd1レジスタが符号拡張されず上位ワードが0になっていなかったため
  //
  //  問題のサブルーチン
  //    00BFCC74  48E77000              movem.l d1-d3,-(sp)                 H輛.
  //    00BFCC78  2600                  move.l  d0,d3                       &.
  //    00BFCC7A  2F00                  move.l  d0,-(sp)                    /.
  //    00BFCC7C  486DFF80              pea.l   $FF80(a5)                   Hm..
  //    00BFCC80  A156                  SXCALL  __GMPtInRect                ｡V
  //    00BFCC82  508F                  addq.l  #$08,sp                     P夙
  //    00BFCC84  6738                  beq.s   $00BFCCBE                   g8
  //    00BFCC86  2003                  move.l  d3,d0                        .
  //    00BFCC88  3200                  move.w  d0,d1                       2.
  //    00BFCC8A  4840                  swap.w  d0                          H@
  //    00BFCC8C  906DFF80              sub.w   $FF80(a5),d0                仁..
  //    00BFCC90  6B2C                  bmi.s   $00BFCCBE                   k,
  //    00BFCC92  B06DFF92              cmp.w   $FF92(a5),d0                ｰm.地
  //    00BFCC96  6E26                  bgt.s   $00BFCCBE                   n&
  //    00BFCC98  926DFF82              sub.w   $FF82(a5),d1                知.Ｌ
  //    00BFCC9C  6B20                  bmi.s   $00BFCCBE                   k 
  //    00BFCC9E  342DFF9C              move.w  $FF9C(a5),d2                4-.愼
  //    00BFCCA2  C4FC0010              mulu.w  #$0010,d2                   ﾄ...
  //    00BFCCA6  B242                  cmp.w   d2,d1                       ｲB
  //    00BFCCA8  6E14                  bgt.s   $00BFCCBE                   n.
  //    00BFCCAA  82FC0010              divu.w  #$0010,d1                   ※..    ←d1レジスタの上位ワードが不定のままdivu.wを実行している
  //    00BFCCAE  3001                  move.w  d1,d0                       0.
  //    00BFCCB0  4841                  swap.w  d1                          HA
  //    00BFCCB2  4A41                  tst.w   d1                          JA
  //    00BFCCB4  6702                  beq.s   $00BFCCB8                   g.
  //    00BFCCB6  5240                  addq.w  #$01,d0                     R@
  //    00BFCCB8  4CDF000E              movem.l (sp)+,d1-d3                 Lﾟ..
  //    00BFCCBC  4E75                  rts                                 Nu
  //    00BFCCBE  7000                  moveq.l #$00,d0                     p.
  //    00BFCCC0  60F6                  bra.s   $00BFCCB8                   `.
  //
  //  問題のサブルーチンの呼び出し元
  //    00BFC9D6  4E56FFF0              link.w  a6,#$FFF0                   NV..
  //    00BFC9DA  6100FEF4              bsr.w   $00BFC8D0                   a...    ←直前に呼び出されるサブルーチン
  //    00BFC9DE  202C000A              move.l  $000A(a4),d0                 ,..
  //    00BFC9E2  61000290              bsr.w   $00BFCC74                   a...    ←問題のサブルーチン
  //    00BFC9E6  3200                  move.w  d0,d1                       2.
  //    00BFC9E8  3600                  move.w  d0,d3                       6.
  //    (以下略)
  //
  //  直前に呼び出されるサブルーチン
  //    00BFC8D0  426DFFA2              clr.w   $FFA2(a5)                   Bm.｢
  //    00BFC8D4  206C0006              movea.l $0006(a4),a0                 l..
  //    00BFC8D8  4C90000F              movem.w (a0),d0-d3                  L...
  //    00BFC8DC  5240                  addq.w  #$01,d0                     R@
  //    00BFC8DE  5241                  addq.w  #$01,d1                     RA
  //    00BFC8E0  5342                  subq.w  #$01,d2                     SB
  //    00BFC8E2  5343                  subq.w  #$01,d3                     SC
  //    00BFC8E4  48AD000FFF80          movem.w d0-d3,$FF80(a5)             0/0
  //    00BFC8EA  206C0002              movea.l $0002(a4),a0                 l..
  //    00BFC8EE  2050                  movea.l (a0),a0                      P
  //    00BFC8F0  30280012              move.w  $0012(a0),d0                0(..
  //    00BFC8F4  5240                  addq.w  #$01,d0                     R@
  //    00BFC8F6  3B40FF90              move.w  d0,$FF90(a5)                ;@.食
  //    00BFC8FA  486DFFA4              pea.l   $FFA4(a5)                   Hm.､
  //    00BFC8FE  A432                  SXCALL  __SXGetDispRect             ､2
  //    00BFC900  588F                  addq.l  #$04,sp                     X臭
  //    00BFC902  4CAD000FFFA4          movem.w $FFA4(a5),d0-d3             Lｭ...､  ←ここでd1レジスタの上位ワードがほぼ0になる
  //    00BFC908  9641                  sub.w   d1,d3                       泡
  //    00BFC90A  48C3                  ext.l   d3                          Hﾃ      ←このサブルーチンはext.lで符号拡張してからdivu.wを使っているが、符号拡張しておいて符号なし除算というのもおかしい。movem.wでd3も符号拡張されているのでこのext.lは不要で、0<=d1.w<=d3.wでないときもext.lは役に立たない
  //    00BFC90C  86FC0010              divu.w  #$0010,d3                   ※..
  //    (以下略)
  //
  public static final boolean MPU_SXMENU = false;  //true=対策を施す。movem.w <ea>,<list>を修正して実害がなくなったので外しておく

  //mpuInit ()
  //  MPUを初期化する
  public static void mpuInit () {
    //コア
    mpuIgnoreAddressError = false;
    //レジスタ
    //r = new int[16];
    //FPU
    fpuInit ();  //FPU FPU
    //クロック
    mpuClockTime = 0L;
    mpuClockLimit = 0L;
    mpuCycleCount = 0;
    //タイマ
    mpuTask = null;
    //例外処理
    M68kException.m6eSignal = new M68kException ();
    M68kException.m6eNumber = 0;
    M68kException.m6eAddress = 0;
    M68kException.m6eDirection = MPU_WR_WRITE;
    M68kException.m6eSize = MPU_SS_BYTE;
    //その他
    mpuBootDevice = -1;
    mpuROMBootHandle = -1;
    mpuSavedBootDevice = -1;
    mpuSavedROMBootHandle = -1;
    //任意の周波数の指定
    //mpuArbFreqOn = !(clockMHz == 10.0 ||
    //                 clockMHz == 50.0 / 3.0 ||  //16.7MHz
    //                 clockMHz == 25.0 ||
    //                 clockMHz == 100.0 / 3.0 ||  //33.3MHz
    //                 clockMHz == 50.0 ||
    //                 clockMHz == 200.0 / 3.0 ||  //66.7MHz
    //                 clockMHz == 75.0 ||
    //                 clockMHz == 100.0);
    //mpuArbFreqMHz = mpuArbFreqOn ? (int) clockMHz : 100;
    //任意の負荷率の指定
    //mpuUtilOn = false;
    //mpuUtilRatio = 100;

    //カウンタ
    mpuAdjustmentCounter = MPU_ADJUSTMENT_INTERVAL;
    mpuTotalNano = 0L;
    mpuLastNano = System.nanoTime ();
    mpuCoreNano1 = mpuCoreNano2 = 0.5 * 1e+6 * (double) (TMR_INTERVAL * MPU_ADJUSTMENT_INTERVAL);  //負荷率50%

    mpuButtonsRunning = new ArrayList<AbstractButton> ();
    mpuButtonsStopped = new ArrayList<AbstractButton> ();

    mpuOriIllegalCheckBoxList = new ArrayList<JCheckBox> ();
    mpuStopOnErrorCheckBoxList = new ArrayList<JCheckBox> ();
    mpuStopAtStartCheckBoxList = new ArrayList<JCheckBox> ();

    mpuAdvanceCount = 0;
    mpuStepCount = 0;
    mpuContinue = false;
    mpuUntilReturnSRS = 0;
    mpuUntilReturnRP = 0;
    mpuUntilReturnPC0 = 0;
    mpuUntilReturnSP = 0;

    //デバッグアクションリスナー
    mpuDebugActionListener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        switch (ae.getActionCommand ()) {
        case "Stop":  //停止
          if (RootPointerList.RTL_ON) {
            if (RootPointerList.rtlCurrentSupervisorTaskIsStoppable ||
                RootPointerList.rtlCurrentUserTaskIsStoppable) {
              mpuStop (null);  //"Stop Button"
            }
          } else {
            mpuStop (null);  //"Stop Button"
          }
          break;
        case "Trace":  //トレース
          mpuAdvance (1);
          break;
        case "Trace 10 times":  //トレース 10 回
          mpuAdvance (10);
          break;
        case "Trace 100 times":  //トレース 100 回
          mpuAdvance (100);
          break;
        case "Step":  //ステップ
          mpuStep (1);
          break;
        case "Step 10 times":  //ステップ 10 回
          mpuStep (10);
          break;
        case "Step 100 times":  //ステップ 100 回
          mpuStep (100);
          break;
        case "Step until return":  //ステップアンティルリターン
          mpuStepUntilReturn ();
          break;
        case "Run":  //実行
          mpuStart ();
          break;
          //
        case "Consider ORI.B #$00,D0 as an illegal instruction" :
          if (DBG_ORI_BYTE_ZERO_D0) {
            dbgOriByteZeroD0 = ((JCheckBox) source).isSelected ();
            for (JCheckBox checkBox : mpuOriIllegalCheckBoxList) {
              if (checkBox.isSelected () != dbgOriByteZeroD0) {
                checkBox.setSelected (dbgOriByteZeroD0);
              }
            }
          }
          break;
        case "Stop on error":
          dbgStopOnError = ((JCheckBox) source).isSelected ();
          for (JCheckBox checkBox : mpuStopOnErrorCheckBoxList) {
            if (checkBox.isSelected () != dbgStopOnError) {
              checkBox.setSelected (dbgStopOnError);
            }
          }
          break;
        case "Stop at execution start position":
          dbgStopAtStart = ((JCheckBox) source).isSelected ();
          for (JCheckBox checkBox : mpuStopAtStartCheckBoxList) {
            if (checkBox.isSelected () != dbgStopAtStart) {
              checkBox.setSelected (dbgStopAtStart);
            }
          }
          break;
        }
      }  //actionPerformed(ActionEvent)
    };  //mpuDebugActionListener

  }  //mpuInit()

  //mpuMakeOriIllegalCheckBox ()
  //  ORI.B #$00,D0を不当命令とみなすチェックボックスを作る
  public static JCheckBox mpuMakeOriIllegalCheckBox () {
    JCheckBox checkBox = Multilingual.mlnToolTipText (
      ComponentFactory.createIconCheckBox (
        DBG_ORI_BYTE_ZERO_D0 ? dbgOriByteZeroD0 : null,
        LnF.LNF_ORI_BYTE_ZERO_D0_IMAGE,
        LnF.LNF_ORI_BYTE_ZERO_D0_SELECTED_IMAGE,
        "Consider ORI.B #$00,D0 as an illegal instruction", mpuDebugActionListener),
      "ja", "ORI.B #$00,D0 を不当命令とみなす");
    mpuOriIllegalCheckBoxList.add (checkBox);
    return checkBox;
  }  //mpuMakeOriIllegalCheckBox

  //mpuMakeStopOnErrorCheckBox ()
  //  エラーで停止するチェックボックスを作る
  public static JCheckBox mpuMakeStopOnErrorCheckBox () {
    JCheckBox checkBox = Multilingual.mlnToolTipText (
      ComponentFactory.createIconCheckBox (
        dbgStopOnError,
        LnF.LNF_STOP_ON_ERROR_IMAGE,
        LnF.LNF_STOP_ON_ERROR_SELECTED_IMAGE,
        "Stop on error", mpuDebugActionListener),
      "ja", "エラーで停止する");
    mpuStopOnErrorCheckBoxList.add (checkBox);
    return checkBox;
  }  //mpuMakeStopOnErrorCheckBox

  //mpuMakeStopAtStartCheckBox ()
  //  実行開始位置で停止するチェックボックスを作る
  public static JCheckBox mpuMakeStopAtStartCheckBox () {
    JCheckBox checkBox = Multilingual.mlnToolTipText (
      ComponentFactory.createIconCheckBox (
        dbgStopAtStart,
        LnF.LNF_STOP_AT_START_IMAGE,
        LnF.LNF_STOP_AT_START_SELECTED_IMAGE,
        "Stop at execution start position", mpuDebugActionListener),
      "ja", "実行開始位置で停止する");
    mpuStopAtStartCheckBoxList.add (checkBox);
    return checkBox;
  }  //mpuMakeStopAtStartCheckBox

  //mpuMakeMenu ()
  //  mdlMakeMenuより後
  //  smrMakeMenuより後
  //  mnbMakeMenuより前
  public static void mpuMakeMenu () {
    //メニュー
    ButtonGroup unitGroup = new ButtonGroup ();
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        switch (ae.getActionCommand ()) {
        case "Reset":  //リセット
          mpuReset (-1, -1);
          break;
        case "Hold down OPT.1 and reset":  //OPT.1 を押しながらリセット
          mpuReset (0, -1);
          break;
        case "Interrupt":  //インタラプト
          sysInterrupt ();  //インタラプトスイッチが押された
          break;
        case "10MHz":
          mpuArbFreqOn = false;
          mpuUtilOn = false;
          mpuSetCurrentClock (10.0);
          break;
        case "16.7MHz":
          mpuArbFreqOn = false;
          mpuUtilOn = false;
          mpuSetCurrentClock (50.0 / 3.0);  //16.7MHz
          break;
        case "25MHz":
          mpuArbFreqOn = false;
          mpuUtilOn = false;
          mpuSetCurrentClock (25.0);
          break;
        case "33.3MHz":
          mpuArbFreqOn = false;
          mpuUtilOn = false;
          mpuSetCurrentClock (100.0 / 3.0);  //33.3MHz
          break;
        case "50MHz":
          mpuArbFreqOn = false;
          mpuUtilOn = false;
          mpuSetCurrentClock (50.0);
          break;
        case "66.7MHz":
          mpuArbFreqOn = false;
          mpuUtilOn = false;
          mpuSetCurrentClock (200.0 / 3.0);  //66.7MHz
          break;
        case "75MHz":
          mpuArbFreqOn = false;
          mpuUtilOn = false;
          mpuSetCurrentClock (75.0);
          break;
        case "100MHz":
          mpuArbFreqOn = false;
          mpuUtilOn = false;
          mpuSetCurrentClock (100.0);
          break;
        case "Arbitrary frequency":  //任意の周波数
          mpuArbFreqOn = true;
          mpuUtilOn = false;
          mpuSetCurrentClock ((double) mpuArbFreqMHz);
          break;
        case "Arbitrary load factor":  //任意の負荷率
          mpuArbFreqOn = false;
          mpuUtilOn = true;
          break;
        case "FE function instruction":  //FE ファンクション命令
          FEFunction.fpkOn = ((JCheckBoxMenuItem) source).isSelected ();
          break;
        case "Reject FLOATn.X":  //FLOATn.X を組み込まない
          FEFunction.fpkRejectFloatOn = ((JCheckBoxMenuItem) source).isSelected ();
          break;
        case "Cut FC2 pin":  //FC2 ピンをカットする
          busRequestCutFC2Pin = ((JCheckBoxMenuItem) source).isSelected ();
          break;
        case "Wait cycles":  //ウェイトサイクル
          busWaitCyclesRequest = ((JCheckBoxMenuItem) source).isSelected ();
          break;
        case "Use IPLROM 1.6":  //IPLROM 1.6 を使う
          ROM.romIPLROM16On = ((JCheckBoxMenuItem) source).isSelected ();
          break;
        case "Increase IPLROM to 256KB":  //IPLROM を 256KB に増やす
          ROM.romIPLROM256KOn = ((JCheckBoxMenuItem) source).isSelected ();
          break;

        case "Run / Stop":  //実行／停止
          if (((JCheckBox) source).isSelected ()) {  //Run
            mpuStart ();
          } else {  //Stop
            if (RootPointerList.RTL_ON) {
              if (RootPointerList.rtlCurrentSupervisorTaskIsStoppable ||
                  RootPointerList.rtlCurrentUserTaskIsStoppable) {
                mpuStop (null);  //"Stop Button"
              }
            } else {
              mpuStop (null);  //"Stop Button"
            }
          }
          pnlPanel.requestFocusInWindow ();  //パネルにフォーカスを戻す。戻さないとキー入力が入らない
          break;
        }
      }
    };
    mpuMenu = ComponentFactory.createMenu (
      "MPU", 'M',
      mpuResetMenuItem = Multilingual.mlnText (ComponentFactory.createMenuItem ("Reset", 'R', MNB_MODIFIERS, listener), "ja", "リセット"),
      mpuOpt1ResetMenuItem = Multilingual.mlnText (ComponentFactory.createMenuItem ("Hold down OPT.1 and reset", 'O', MNB_MODIFIERS, listener), "ja", "OPT.1 を押しながらリセット"),
      Multilingual.mlnText (ComponentFactory.createMenuItem ("Interrupt", listener), "ja", "インタラプト"),
      ComponentFactory.createHorizontalSeparator (),
      mdlMenu,
      ComponentFactory.createHorizontalSeparator (),
      mpuClock10MenuItem = ComponentFactory.createRadioButtonMenuItem (
        unitGroup,
        !mpuArbFreqOn && !mpuUtilOn && specifiedClock == 10.0,
        "10MHz",
        listener),
      mpuClock16MenuItem = ComponentFactory.createRadioButtonMenuItem (
        unitGroup,
        !mpuArbFreqOn && !mpuUtilOn && specifiedClock == 50.0 / 3.0,
        "16.7MHz",
        listener),
      mpuClock25MenuItem = ComponentFactory.createRadioButtonMenuItem (
        unitGroup,
        !mpuArbFreqOn && !mpuUtilOn && specifiedClock == 25.0,
        "25MHz",
        listener),
      mpuClock33MenuItem = ComponentFactory.createRadioButtonMenuItem (
        unitGroup,
        !mpuArbFreqOn && !mpuUtilOn && specifiedClock == 100.0 / 3.0,
        "33.3MHz",
        listener),
      mpuClock50MenuItem = ComponentFactory.createRadioButtonMenuItem (
        unitGroup,
        !mpuArbFreqOn && !mpuUtilOn && specifiedClock == 50.0,
        "50MHz",
        listener),
      mpuClock66MenuItem = ComponentFactory.createRadioButtonMenuItem (
        unitGroup,
        !mpuArbFreqOn && !mpuUtilOn && specifiedClock == 200.0 / 3.0,
        "66.7MHz",
        listener),
      mpuClock75MenuItem = ComponentFactory.createRadioButtonMenuItem (
        unitGroup,
        !mpuArbFreqOn && !mpuUtilOn && specifiedClock == 75.0,
        "75MHz",
        listener),
      mpuClock100MenuItem = ComponentFactory.createRadioButtonMenuItem (
        unitGroup,
        !mpuArbFreqOn && !mpuUtilOn && specifiedClock == 100.0,
        "100MHz",
        listener),
      mpuArbFreqRadioButtonMenuItem = Multilingual.mlnText (
        ComponentFactory.createRadioButtonMenuItem (
          unitGroup,
          mpuArbFreqOn,
          "Arbitrary frequency",
          listener),
        "ja", "任意の周波数"),
      ComponentFactory.createHorizontalBox (
        Box.createHorizontalStrut (20),
        mpuArbFreqSpinner = ComponentFactory.createNumberSpinner (
          mpuArbFreqModel = new SpinnerNumberModel (mpuArbFreqMHz, 1, 1000, 1),
          4,
          new ChangeListener () {
            @Override public void stateChanged (ChangeEvent ce) {
              //mpuArbFreqRadioButtonMenuItem.setSelected (true);
              mpuArbFreqMHz = mpuArbFreqModel.getNumber ().intValue ();
              if (mpuArbFreqOn) {
                mpuSetCurrentClock ((double) mpuArbFreqMHz);
              }
            }
          }
          ),
        ComponentFactory.createLabel ("MHz"),
        Box.createHorizontalGlue ()
        ),
      mpuUtilRadioButtonMenuItem = Multilingual.mlnText (
        ComponentFactory.createRadioButtonMenuItem (
          unitGroup,
          mpuUtilOn,
          "Arbitrary load factor",
          listener),
        "ja", "任意の負荷率"),
      ComponentFactory.createHorizontalBox (
        Box.createHorizontalStrut (20),
        mpuUtilSpinner = ComponentFactory.createNumberSpinner (
          mpuUtilModel = new SpinnerNumberModel (mpuUtilRatio, 1, 100, 1),
          4,
          new ChangeListener () {
            @Override public void stateChanged (ChangeEvent ce) {
              //mpuUtilRadioButtonMenuItem.setSelected (true);
              mpuUtilRatio = mpuUtilModel.getNumber ().intValue ();
            }
          }
          ),
        ComponentFactory.createLabel ("%"),
        Box.createHorizontalGlue ()
        ),
      //
      ComponentFactory.createHorizontalSeparator (),
      coproFPUMenu,
      Multilingual.mlnText (ComponentFactory.createCheckBoxMenuItem (FEFunction.fpkOn, "FE function instruction", listener), "ja", "FE ファンクション命令"),
      Multilingual.mlnText (ComponentFactory.createCheckBoxMenuItem (FEFunction.fpkRejectFloatOn, "Reject FLOATn.X", listener), "ja", "FLOATn.X を組み込まない"),
      //
      ComponentFactory.createHorizontalSeparator (),
      Multilingual.mlnText (ComponentFactory.createCheckBoxMenuItem (busRequestCutFC2Pin, "Cut FC2 pin", listener), "ja", "FC2 ピンをカットする"),
      Multilingual.mlnText (ComponentFactory.createCheckBoxMenuItem (busWaitCyclesRequest, "Wait cycles", listener), "ja", "ウェイトサイクル"),
      ComponentFactory.createHorizontalSeparator (),
      Multilingual.mlnText (ComponentFactory.createCheckBoxMenuItem (ROM.romIPLROM16On, "Use IPLROM 1.6", listener), "ja", "IPLROM 1.6 を使う"),
      Multilingual.mlnText (ComponentFactory.createCheckBoxMenuItem (ROM.romIPLROM256KOn, "Increase IPLROM to 256KB", listener), "ja", "IPLROM を 256KB に増やす"),
      //
      ComponentFactory.createHorizontalSeparator (),
      SRAM.smrCacheMenu
      );
  }  //mpuMakeMenu()

  //  クロック(MHz)を設定する
  public static void mpuSetCurrentClock (double clock) {
    specifiedClock = clock;
    if (currentIsSecond) {
      specifiedSecondClock = clock;
    } else {
      specifiedFirstClock = clock;
    }
    if (!mpuArbFreqOn && !mpuUtilOn) {
      if (specifiedClock == 10.0) {
        mpuClock10MenuItem.setSelected (true);
      } else if (specifiedClock == 50.0 / 3.0) {  //16.7MHz
        mpuClock16MenuItem.setSelected (true);
      } else if (specifiedClock == 25.0) {
        mpuClock25MenuItem.setSelected (true);
      } else if (specifiedClock == 100.0 / 3.0) {  //33.3MHz
        mpuClock33MenuItem.setSelected (true);
      } else if (specifiedClock == 50.0) {
        mpuClock50MenuItem.setSelected (true);
      } else if (specifiedClock == 200.0 / 3.0) {  //66.7MHz
        mpuClock66MenuItem.setSelected (true);
      } else if (specifiedClock == 75.0) {
        mpuClock75MenuItem.setSelected (true);
      } else if (specifiedClock == 100.0) {
        mpuClock100MenuItem.setSelected (true);
      }
    }
    mpuClockMHz = specifiedClock;
    mpuSetClockMHz (mpuClockMHz);  //currentMPUが変更されたときは再計算が必要なのでクロックが変わっていなくても省略できない
  }

  //mpuSetClockMHz (mhz)
  //  mpuCurrentMHz,mpuCycleUnit,mpuModifiedUnitを設定する
  //  TMR_FREQ=10^12のとき
  //               mpuModifiedUnitと1サイクルの時間
  //  mpuClockMHz     000/060         030
  //    10.0MHz    100000(100ns)  60000(60ns)
  //    16.7MHz     60000 (60ns)  36000(36ns)
  //    25.0MHz     40000 (40ns)  24000(24ns)
  //    33.3MHz     30000 (30ns)  18000(18ns)
  //    50.0MHz     20000 (20ns)  12000(12ns)
  public static void mpuSetClockMHz (double mhz) {
    mhz = Math.max (1.0, Math.min (1000.0, mhz));
    double lastMHz = mpuCurrentMHz;
    mpuCurrentMHz = mhz;
    mpuCycleUnit = (long) (((double) TMR_FREQ / 1000000.0) / mhz + 0.5);
    //  DBRA命令で比較するとMC68030は3/5、MC68040は2/5、MC68060は1/5くらい？
    mpuModifiedUnit = (currentMPU == Model.MPU_MC68EC030 ||
                       currentMPU == Model.MPU_MC68030 ?
                       (long) (((double) TMR_FREQ * 3.0 / (5.0 * 1000000.0)) / mhz + 0.5) :
                       currentMPU == Model.MPU_MC68LC040 ||
                       currentMPU == Model.MPU_MC68040 ?
                       (long) (((double) TMR_FREQ * 2.0 / (5.0 * 1000000.0)) / mhz + 0.5) :
                       mpuCycleUnit);
    if (lastMHz != mhz) {
      mpuSetWait ();
    }
  }  //mpuSetClockMHz(double)

  //mpuSetWait ()
  //  ウェイトを設定する
  //    currentMPU
  //    mpuCycleUnit
  //    mpuCacheOn
  //    mpuROMWaitCycles
  //    mpuRAMWaitCycles
  //    dmaCycleUnit
  //  から
  //    mpuWaitTime
  //    dmaWaitTime
  //  のすべての項目と
  //    mpuNoWaitTime
  //  のsramとromを作る
  //!!! 機種や個体による違いは考慮されていない。DMACのウェイトの機種による違いはDMAC側で調整している
  //!!! 状態やアクセス方法による違いは考慮されていない
  //!!! MPUで分類しておりXellent30は考慮されていない
  //!!! X68030のramとromはロングワード境界を跨がなければ1回でアクセスできるが考慮されていない
  //!!! 060turboはI/Oポートの書き込みウェイトの間に多数の命令を実行できるが考慮されていない
  //  参考
  //                         Execution time of instructions testing I/O ports
  //    +---------------+-------------------+----------------------------------------------------+
  //    |               |                   |                execution time (us)                 |
  //    |   I/O port    |    instruction    |           X68000 10MHz            | X68030 060turbo|
  //    |               |                   | Shodai  ACE  ACE 2nd  PRO    XVI  | 25MHz   50MHz  |
  //    +---------------+-------------------+-----------------------------------+----------------+
  //    | main memory   | tst.w $00000000.l | 1.642  1.642  1.643  1.663  1.650 | 0.2400  0.0399 |
  //    | GVRAM         | tst.w $00C00000.l | 1.752  1.750  1.750  1.770  1.734 | 0.4951  0.4700 |
  //    | TVRAM         | tst.w $00E00000.l | 1.829  1.849  1.850  1.840  1.848 | 0.5049  0.4800 |
  //    | CRTC R00      | tst.w $00E80000.l | 1.740  1.724  1.726  1.770  1.733 | 0.4800  0.4599 |
  //    | CRTC action   | tst.w $00E80480.l | 1.741  1.725  1.725  1.771  1.733 | 0.4800  0.4600 |
  //    | VCN gpalet    | tst.w $00E82000.l | 1.875  1.909  1.910  1.912  1.919 | 0.6778  0.6962 |
  //    | VCN mode      | tst.w $00E82400.l | 1.641  1.640  1.642  1.770  1.733 | 0.4800  0.4601 |
  //    | DMAC ch0 CSR  | tst.b $00E84000.l | 3.124  3.114  3.115  3.127  3.124 | 1.599   1.501  |
  //    | MFP GPIP      | tst.b $00E88001.l | 2.088  2.031  2.031  2.082  2.080 | 1.013   0.9801 |
  //    | RTC onesec    | tst.b $00E8A001.l | 1.776  1.850  1.851  1.773  1.836 | 1.359   1.580  |
  //    | PRN data      | tst.b $00E8C001.l | 1.742  1.725  1.725  1.771  1.734 | 0.4001  0.3800 |
  //    | SYS contrast  | tst.b $00E8E001.l | 1.741  1.725  1.726  1.771  1.734 | 0.4000  0.3800 |
  //    | OPM status    | tst.b $00E90003.l | 1.776  1.852  1.850  1.772  1.835 | 0.8268  0.7801 |
  //    | PCM status    | tst.b $00E92001.l | 1.776  1.850  1.850  1.771  1.836 | 0.8268  0.7800 |
  //    | FDC status    | tst.b $00E94001.l | 1.776  1.852  1.850  1.771  1.836 | 0.8266  0.7800 |
  //    | FDD status    | tst.b $00E94005.l | 1.777  1.850  1.850  1.770  1.836 | 0.8268  0.7801 |
  //    | HDC status    | tst.b $00E96003.l | 1.776  1.852  1.850  1.770  1.836 | 0.8267  0.7801 |
  //    | SCC chB RR0   | tst.b $00E98001.l | 2.231  2.234  2.236  2.248  2.244 | 1.760   1.980  |
  //    | PPI portA     | tst.b $00E9A001.l | 1.777  1.851  1.852  1.771  1.835 | 0.8268  0.7800 |
  //    | IOI status    | tst.b $00E9C001.l | 1.776  1.851  1.851  1.770  1.835 | 0.8267  0.7800 |
  //    | SPRC bg0 scrX | tst.w $00EB0800.l | 1.740  1.725  1.726  1.769  1.734 | 0.4800  0.4600 |
  //    | SRAM          | tst.w $00ED0000.l | 1.741  1.725  1.725  1.772  1.733 | 0.3199  0.3001 |
  //    | CGROM         | tst.w $00F00000.l | 1.741  1.725  1.725  1.771  1.733 | 0.2399  0.0400 |
  //    | IPLROM        | tst.w $00FF0000.l | 1.742  1.726  1.725  1.770  1.734 | 0.2400  0.0400 |
  //    +---------------+-------------------+-----------------------------------+----------------+
  //    thanks to uchopon, tnb and ita
  //
  //                    X68000   X68030 060turbo
  //                     10MHz    25MHz    50MHz
  //    main memory      0.123    0.000    0.000
  //    GVRAM            1.145    6.377   21.500
  //    TVRAM            2.046    6.623   22.000
  //    CRTC R00         1.050    6.000   20.995
  //    CRTC action      1.055    6.000   21.000
  //    VCN gpalet       2.668   10.945   32.810
  //    VCN mode         0.592    6.000   21.005
  //    DMAC ch0 CSR    14.853   33.975   73.050
  //    MFP GPIP         4.332   19.325   47.005
  //    RTC onesec       1.718   27.975   77.000
  //    PRN data         1.060    4.002   17.000
  //    SYS contrast     1.058    4.000   17.000
  //    OPM status       1.715   14.670   37.005
  //    PCM status       1.712   14.670   37.000
  //    FDC status       1.715   14.665   37.000
  //    FDD status       1.712   14.670   37.005
  //    HDC status       1.712   14.668   37.005
  //    SCC chB RR0      6.025   38.000   97.000
  //    PPI portA        1.716   14.670   37.000
  //    IOI status       1.710   14.668   37.000
  //    SPRC bg0 scrX    1.051    6.000   21.000
  //    SRAM             1.057    1.998   13.005
  //    CGROM            1.055   -0.002    0.000
  //    IPLROM           1.058    0.000    0.000
  //
  public static void mpuSetWait () {
    //MPU
    if (currentMPU <= Model.MPU_MC68010) {  //68000/68010
      mpuWaitTime.ram = mpuCycleUnit >> 3;  //DRAMリフレッシュウェイトを1/8サイクル/ワードとする
      mpuWaitTime.vicon = (long) (mpuCycleUnit * 0.6);
      mpuWaitTime.crtc =
        mpuWaitTime.prnport =
          mpuWaitTime.sysport =
            mpuWaitTime.sprc =
              mpuWaitTime.sram =
                mpuWaitTime.rom = mpuCycleUnit;
      mpuWaitTime.gvram = (long) (mpuCycleUnit * 1.1);
      mpuWaitTime.rtc =
        mpuWaitTime.opm =
          mpuWaitTime.adpcm =
            mpuWaitTime.fdc =
              mpuWaitTime.fdd =
                mpuWaitTime.hdc =
                  mpuWaitTime.ppi =
                    mpuWaitTime.ioi = (long) (mpuCycleUnit * 1.7);
      mpuWaitTime.tvram = mpuCycleUnit * 2;
      mpuWaitTime.palet = (long) (mpuCycleUnit * 2.6);
      mpuWaitTime.mfp = (long) (mpuCycleUnit * 4.3);
      mpuWaitTime.scc = mpuCycleUnit * 6;
      mpuWaitTime.dmac = mpuCycleUnit * 15;
      mpuWaitTime.ramlong = mpuWaitTime.ram << 1;
      mpuWaitTime.romlong = mpuWaitTime.rom << 1;
    } else if (currentMPU <= Model.MPU_MC68030) {  //68020/68030
      mpuWaitTime.ram = mpuCacheOn ? 0 : mpuCycleUnit * mpuRAMWaitCycles + (mpuCycleUnit >> 3);
      mpuWaitTime.rom = mpuCacheOn ? 0 : mpuCycleUnit * mpuROMWaitCycles;
      mpuWaitTime.sram = mpuCycleUnit * 2;
      mpuWaitTime.prnport =
        mpuWaitTime.sysport = mpuCycleUnit * 4;
      mpuWaitTime.gvram =
        mpuWaitTime.crtc =
          mpuWaitTime.vicon =
            mpuWaitTime.sprc = mpuCycleUnit * 6;
      mpuWaitTime.tvram = mpuCycleUnit * 7;
      mpuWaitTime.palet = mpuCycleUnit * 11;
      mpuWaitTime.opm =
        mpuWaitTime.adpcm =
          mpuWaitTime.fdc =
            mpuWaitTime.fdd =
              mpuWaitTime.hdc =
                mpuWaitTime.ppi =
                  mpuWaitTime.ioi = mpuCycleUnit * 15;
      mpuWaitTime.mfp = mpuCycleUnit * 19;
      mpuWaitTime.rtc = mpuCycleUnit * 28;
      mpuWaitTime.dmac = mpuCycleUnit * 34;
      mpuWaitTime.scc = mpuCycleUnit * 38;
      mpuWaitTime.ramlong = mpuWaitTime.ram;
      mpuWaitTime.romlong = mpuWaitTime.rom;
    } else {  //68040/68060
      mpuWaitTime.ram = mpuCacheOn ? 0 : mpuCycleUnit * mpuRAMWaitCycles + (mpuCycleUnit >> 3);
      mpuWaitTime.rom = mpuCacheOn ? 0 : mpuCycleUnit * mpuROMWaitCycles;
      mpuWaitTime.sram = mpuCycleUnit * 13;
      mpuWaitTime.prnport =
        mpuWaitTime.sysport = mpuCycleUnit * 17;
      mpuWaitTime.gvram =
        mpuWaitTime.crtc =
          mpuWaitTime.vicon =
            mpuWaitTime.sprc = mpuCycleUnit * 21;
      mpuWaitTime.tvram = mpuCycleUnit * 22;
      mpuWaitTime.palet = mpuCycleUnit * 33;
      mpuWaitTime.opm =
        mpuWaitTime.adpcm =
          mpuWaitTime.fdc =
            mpuWaitTime.fdd =
              mpuWaitTime.hdc =
                mpuWaitTime.ppi =
                  mpuWaitTime.ioi = mpuCycleUnit * 37;
      mpuWaitTime.mfp = mpuCycleUnit * 47;
      mpuWaitTime.dmac = mpuCycleUnit * 73;
      mpuWaitTime.rtc = mpuCycleUnit * 77;
      mpuWaitTime.scc = mpuCycleUnit * 97;
      mpuWaitTime.ramlong = mpuWaitTime.ram;
      mpuWaitTime.romlong = mpuWaitTime.rom;
    }
    if (true) {
      //SRAMとROMはウェイトサイクルなしでも0にしない。クロック計測用
      mpuNoWaitTime.sram = mpuWaitTime.sram;
      mpuNoWaitTime.rom = mpuWaitTime.rom;
      mpuNoWaitTime.romlong = mpuWaitTime.romlong;
    }
    //DMAC
    dmaWaitTime.ram = dmaCycleUnit >> 3;  //DRAMリフレッシュウェイトを1/8サイクル/ワードとする
    dmaWaitTime.sram = 0;
    dmaWaitTime.rom = 0;
    dmaWaitTime.gvram =
      dmaWaitTime.crtc =
        dmaWaitTime.vicon =
          dmaWaitTime.prnport =
            dmaWaitTime.sysport =
              dmaWaitTime.sprc = dmaCycleUnit;
    dmaWaitTime.tvram =
      dmaWaitTime.rtc =
        dmaWaitTime.opm =
          dmaWaitTime.adpcm =
            dmaWaitTime.fdc =
              dmaWaitTime.fdd =
                dmaWaitTime.hdc =
                  dmaWaitTime.ppi =
                    dmaWaitTime.ioi = dmaCycleUnit * 2;
    dmaWaitTime.palet = dmaCycleUnit * 3;
    dmaWaitTime.mfp = dmaCycleUnit * 4;
    dmaWaitTime.scc = dmaCycleUnit * 6;
    dmaWaitTime.dmac = dmaCycleUnit * 15;
    dmaWaitTime.ramlong = dmaWaitTime.ram << 1;
    dmaWaitTime.romlong = dmaWaitTime.rom << 1;
    if (MC68060.CAT_ON) {
      if (Model.MPU_MC68LC060 <= currentMPU) {  //68LC060,68060
        MC68060.catMainLongTime = mpuCycleUnit * 13;
        MC68060.catMainLineTime = mpuCycleUnit * 45;
        MC68060.catHighLongTime = mpuCycleUnit * 9;
        MC68060.catHighLineTime = mpuCycleUnit * 15;
      }
    }
  }  //mpuSetWait

  //mpuReset (device, romHandle)
  //  MPUをリセットしてからコアのタスクを起動する
  //  コアのタスクが動いているときはそれを中断する
  //  動いていたタスクが完全に止まってから再起動する
  public static void mpuReset (int device, int romHandle) {

    if (PowerControl.PWC_ON &&
        !PowerControl.pwcVcc1On) {
      return;
    }

    mpuBootDevice = device;
    mpuROMBootHandle = romHandle;

    //mpuBootDevice
    //  -1=リセット,0=OPT.1キーを押しながらリセット、その他=ここから再起動
    //  リセットのとき
    //    保存されている起動デバイスがあるとき
    //      起動デバイスを保存されている起動デバイスに変更する
    //      保存されている起動デバイスを消す
    //  リセット以外のとき
    //    保存されている起動デバイスがないとき
    //      起動デバイスを保存する
    //  メニューで起動デバイスが既定以外に設定されたとき
    //    保存されている起動デバイスを消す
    if (mpuBootDevice == -1) {  //リセットのとき
      if (mpuSavedBootDevice != -1) {  //保存されている起動デバイスがあるとき
        mpuBootDevice = mpuSavedBootDevice;  //起動デバイスを保存されている起動デバイスに変更する
        mpuROMBootHandle = mpuSavedROMBootHandle;
        mpuSavedBootDevice = -1;  //保存されている起動デバイスを消す
        mpuSavedROMBootHandle = -1;
      }
    } else {  //リセット以外のとき
      if (mpuSavedBootDevice == -1) {  //保存されている起動デバイスがないとき
        mpuSavedBootDevice = MainMemory.mmrRwz (0x00ed0018);  //起動デバイスを保存する
        mpuSavedROMBootHandle = MainMemory.mmrRls (0x00ed000c);
      }
    }

    //68000と68010のどちらを使うか選択する
    if (mpu010) {  //68010を使う
      if (specifiedFirstMPU == Model.MPU_MC68000) {  //68000のとき
        specifiedFirstMPU = Model.MPU_MC68010;  //68010に変更する
      }
      if (specifiedSecondMPU == Model.MPU_MC68000) {  //68000のとき
        specifiedSecondMPU = Model.MPU_MC68010;  //68010に変更する
      }
    } else {  //68000を使う
      if (specifiedFirstMPU == Model.MPU_MC68010) {  //68010のとき
        specifiedFirstMPU = Model.MPU_MC68000;  //68000に変更する
      }
      if (specifiedSecondMPU == Model.MPU_MC68010) {  //68010のとき
        specifiedSecondMPU = Model.MPU_MC68000;  //68000に変更する
      }
    }

    //1番目のMPUを選択する
    specifiedIsSecond = false;
    specifiedMPU = specifiedIsSecond ? specifiedSecondMPU : specifiedFirstMPU;
    specifiedClock = specifiedIsSecond ? specifiedSecondClock : specifiedFirstClock;

    if (MC68EC030.M30_DIV_ZERO_V_FLAG) {
      MC68EC030.m30DivZeroVFlag = false;
    }

    if (mpuTask != null) {
      mpuClockLimit = 0L;
      System.out.println (Multilingual.mlnJapanese ?
                          "MPU を停止します" :
                          "MPU stops");
      mpuTask.cancel ();
      mpuTask = null;
    }

    tmrTimer.schedule (new TimerTask () {
      @Override public void run () {

        //リセット後の値を現在の値にする
        //  機種とアクセラレータ
        currentModel = specifiedModel;
        currentAccelerator = specifiedAccelerator;
        frmUpdateTitle ();
        //  MPU
        currentIsSecond = specifiedIsSecond;
        currentFirstMPU = specifiedFirstMPU;
        currentSecondMPU = specifiedSecondMPU;
        currentMPU = specifiedMPU;
        //  クロック(MHz)
        mpuSetCurrentClock (specifiedClock);
        //  コプロセッサとオンチップFPU
        currentCopro0 = specifiedCopro0;
        currentCopro1 = specifiedCopro1;
        currentCopro2 = specifiedCopro2;
        currentOnchipFPU = specifiedOnchipFPU;

        //メモリマップを再構築する
        if (currentMPU < Model.MPU_MC68020) {
          if (busHimem68000) {
            busRequestExMemoryStart = 0x10000000;
            busRequestExMemorySize = busLocalMemorySize;
            busRequestExMemoryArray = busLocalMemoryArray;
          } else {
            busRequestExMemoryStart = 0x10000000;
            busRequestExMemorySize = 0 << 20;
            busRequestExMemoryArray = BUS_DUMMY_MEMORY_ARRAY;
          }
        } else if (currentMPU < Model.MPU_MC68LC040) {
          if (busHighMemory060turboOn) {
            busRequestExMemoryStart = 0x10000000;
            busRequestExMemorySize = busLocalMemorySize;
            busRequestExMemoryArray = busLocalMemoryArray;
          } else {
            busRequestExMemoryStart = 0x01000000;
            busRequestExMemorySize = busHighMemorySize;
            busRequestExMemoryArray = busHighMemoryArray;
          }
        } else {
          busRequestExMemoryStart = 0x10000000;
          busRequestExMemorySize = busLocalMemorySize;
          busRequestExMemoryArray = busLocalMemoryArray;
        }
        busUpdateMemoryMap ();

        //ROMを構築する
        ROM.romReset ();

        RegisterList.drpSetMPU ();

        mpuSFC = mpuDFC = mpuCACR = mpuBUSCR = mpuUSP = mpuVBR = mpuCAAR = mpuMSP = mpuISP = 0;
        mpuPCR = 0x04300500 | MPU_060_REV << 8;
        MC68060.mmuReset ();  //TCR,ITT0,ITT1,DTT0,DTT1,URP,SRP。060→000/030のときアドレス変換をOFFにする必要がある

        //マザーボードコプロセッサ
        if (Model.MPU_MC68020 <= currentMPU) {
          if ((7 & currentCopro0) == 1) {
            fpuMotherboardCoprocessor.epbSetMC68881 ();
          } else if ((7 & currentCopro0) == 2) {
            fpuMotherboardCoprocessor.epbSetMC68882 ();
          } else {  //7
            fpuMotherboardCoprocessor.epbSetFullSpec ();
          }
          if ((8 & currentCopro0) == 0) {
            fpuMotherboardCoprocessor.epbSetExtended ();
          } else {  //8
            fpuMotherboardCoprocessor.epbSetTriple ();
          }
          if (MC68EC030.CPV_ON &&
              (7 & currentCopro0) != 0) {  //マザーボードコプロセッサがある
            //リセット
            MC68EC030.cpvReset ();
          }
        }
        //拡張コプロセッサ#1
        if ((7 & currentCopro1) == 1) {
          fpuCoproboard1.epbSetMC68881 ();
        } else if ((7 & currentCopro1) == 2) {
          fpuCoproboard1.epbSetMC68882 ();
        } else {  //7
          fpuCoproboard1.epbSetFullSpec ();
        }
        if ((8 & currentCopro1) == 0) {
          fpuCoproboard1.epbSetExtended ();
        } else {  //8
          fpuCoproboard1.epbSetTriple ();
        }
        //拡張コプロセッサ#2
        if ((7 & currentCopro2) == 1) {
          fpuCoproboard2.epbSetMC68881 ();
        } else if ((7 & currentCopro2) == 2) {
          fpuCoproboard2.epbSetMC68882 ();
        } else {  //7
          fpuCoproboard2.epbSetFullSpec ();
        }
        if ((8 & currentCopro2) == 0) {
          fpuCoproboard2.epbSetExtended ();
        } else {  //8
          fpuCoproboard2.epbSetTriple ();
        }
        //オンチップFPU
        if (Model.MPU_MC68040 <= currentMPU) {
          if ((7 & currentOnchipFPU) == 6) {
            fpuOnChipFPU.epbSetMC68060 ();
          } else {  //7
            fpuOnChipFPU.epbSetFullSpec ();
          }
          if ((8 & currentOnchipFPU) == 0) {
            fpuOnChipFPU.epbSetExtended ();
          } else {
            fpuOnChipFPU.epbSetTriple ();
          }
        }

        if (!currentModel.isX68030 ()) {  //X68000
          dmaCycleUnit = TMR_FREQ / 10000000L;  //DMACは10MHz
          HD63450.dmaBurstInterval = dmaCycleUnit << 4 + (HD63450.dmaBT >> 2);
          HD63450.dmaBurstSpan = HD63450.dmaBurstInterval >> 1 + (HD63450.dmaBR & 3);
          mpuROMWaitCycles = 1;
          mpuRAMWaitCycles = 0;
        } else {  //X68030
          dmaCycleUnit = TMR_FREQ / 12500000L;  //DMACは12.5MHz
          HD63450.dmaBurstInterval = dmaCycleUnit << 4 + (HD63450.dmaBT >> 2);
          HD63450.dmaBurstSpan = HD63450.dmaBurstInterval >> 1 + (HD63450.dmaBR & 3);
          mpuROMWaitCycles = 0;
          mpuRAMWaitCycles = 0;
        }

        busWaitCycles = busWaitCyclesRequest;
        busWaitTime = busWaitCycles ? mpuWaitTime : mpuNoWaitTime;

        HD63450.dmaReadCycles = (currentModel.isPRO () ? 6 :  //PROは6
                                 currentModel.isCompact () ? 4 :  //Compactは4
                                 5);  //その他は5
        HD63450.dmaWriteCycles = (currentModel.isPRO () ? 6 :  //PROは6
                                  5);  //その他は5

        if (currentMPU < Model.MPU_MC68020) {  //68000/68010

          mpuIgnoreAddressError = false;
          //mpuSFC = mpuDFC = mpuVBR = mpuISP = 0;
          mpuCacheOn = false;

        } else if (currentMPU < Model.MPU_MC68040) {  //68030

          mpuIgnoreAddressError = true;
          fpuBox = fpuMotherboardCoprocessor;
          fpuBox.epbReset ();
          fpuFPn = fpuBox.epbFPn;
          //mpuCACR = mpuUSP = mpuVBR = mpuCAAR = mpuMSP = mpuISP = 0;
          mpuCacheOn = (mpuCACR & 0x00000101) != 0;

        } else {  //68040/68060

          mpuIgnoreAddressError = true;
          fpuBox = fpuOnChipFPU;
          fpuBox.epbReset ();
          fpuFPn = fpuBox.epbFPn;
          //mpuSFC = mpuDFC = mpuCACR = mpuBUSCR = mpuUSP = mpuVBR = mpuURP = mpuSRP = 0;
          mpuPCR = 0x04300500 | MPU_060_REV << 8;
          mpuCacheOn = (mpuCACR & 0x80008000) != 0;
          if (MC68060.CAT_ON) {
            if (Model.MPU_MC68LC060 <= currentMPU) {  //68LC060,68060
              MC68060.catReset ();
            }
          }

        }

        mpuSetWait ();

        //! SSPとPCをROMのアドレスから直接読み出している
        regSRT1 = regSRT0 = 0;
        regSRS = REG_SR_S;
        regSRM = 0;
        regSRI = REG_SR_I;
        regCCR = 0;
        Arrays.fill (regRn, 0);
        //r[14] = 0x00001000;  //ROMDB2.32のバグ対策。コードにパッチをあてることにしたので不要
        regRn[15] = MainMemory.mmrRls (0x00ff0000);
        regPC = MainMemory.mmrRls (0x00ff0004);
        //メインメモリ
        MainMemory.mmrReset ();
        //バスコントローラ
        busReset ();
        if (InstructionBreakPoint.IBP_ON) {
          InstructionBreakPoint.ibpOp1MemoryMap = InstructionBreakPoint.ibpOp1SuperMap;
          InstructionBreakPoint.ibpReset ();
        }
        if (BranchLog.BLG_ON) {
          BranchLog.blgReset ();
        }
        //割り込み
        mpuIMR = 0;
        mpuIRR = 0;
        if (MC68901.MFP_DELAYED_INTERRUPT) {
          mpuDIRR = 0;
        }
        mpuISR = 0;
        //これでリセット命令が実行されるまでメインメモリとROM以外のデバイスは動かないはず
        //動作開始
        mpuStart ();
      }
    }, TMR_DELAY);

  }  //mpuReset(int,int)

  //mpuStopAndStart ()
  //  停止と再開
  public static void mpuStopAndStart () {
    if (mpuTask == null) {  //Run
      mpuStart ();
    } else {  //Stop
      if (RootPointerList.RTL_ON) {
        if (RootPointerList.rtlCurrentSupervisorTaskIsStoppable ||
            RootPointerList.rtlCurrentUserTaskIsStoppable) {
          mpuStop (null);
        }
      } else {
        mpuStop (null);
      }
    }
  }  //mpuStopAndStart

  //mpuStart ()
  //  コアのタスクを起動する
  //  コアのタスクが動いているときはそれを中断する
  //  動いていたタスクが完全に止まってから再起動する
  public static void mpuStart () {
    if (mpuTask != null) {
      mpuClockLimit = 0L;
      System.out.println (Multilingual.mlnJapanese ?
                          "MPU を停止します" :
                          "MPU stops");
      mpuTask.cancel ();
      mpuTask = null;
    }
    //停止中だけ有効なボタンを無効にする
    for (AbstractButton button : mpuButtonsStopped) {
      button.setEnabled (false);
    }
    DisassembleList.ddpStoppedBy = null;
    System.out.println (Model.mpuNameOf (currentMPU) + (Multilingual.mlnJapanese ? " を起動します" : " starts up"));
    mpuTask = new TimerTask () {
      @Override public void run () {
        mpuContinue = true;
        mpuClockLimit = mpuClockTime + TMR_FREQ * TMR_INTERVAL / 1000;
        mpuExecuteCore ();
      }
    };
    tmrTimer.scheduleAtFixedRate (mpuTask, TMR_DELAY, TMR_INTERVAL);  //固定頻度実行
    //動作中だけ有効なボタンを有効にする
    for (AbstractButton button : mpuButtonsRunning) {
      button.setEnabled (true);
    }
  }  //mpuStart()

  //mpuExecuteCore ()
  //  コアを実行する
  public static void mpuExecuteCore () {
    //コアメーター計測開始
    long nanoStart = System.nanoTime ();
    //コアタスクをロック
    CoreTask.lock ();
    //RTCを準備する
    RP5C15.rtcExecuteCore ();
    //コアを呼び出す
    if (currentMPU < Model.MPU_MC68010) {
      MC68000.mpuCore ();
    } else if (currentMPU < Model.MPU_MC68020) {
      MC68010.mpuCore ();
    } else if (currentMPU < Model.MPU_MC68LC040) {
      MC68EC030.mpuCore ();
    } else {
      MC68060.mpuCore ();
    }
    //デバッグウインドウを更新する
    if (dbgVisibleMask != 0) {  //デバッグ関連ウインドウが表示されている
      dbgUpdate ();
    }
    //コアタスクをアンロック
    CoreTask.unlock ();
    //コアメーター計測終了
    long nanoEnd = System.nanoTime ();
    mpuTotalNano += nanoEnd - nanoStart;
    if (--mpuAdjustmentCounter == 0) {
      //本来の経過時間(ns)
      final double expectedNano = 1e+6 * (double) (TMR_INTERVAL * MPU_ADJUSTMENT_INTERVAL);
      //コアの所要時間(ns)
      double coreNano0 = (double) mpuTotalNano;
      mpuTotalNano = 0L;
      double coreNanoA = (coreNano0 * 2.0 + mpuCoreNano1 + mpuCoreNano2) * 0.25;  //コアの所要時間(ns)
      mpuCoreNano2 = mpuCoreNano1;
      mpuCoreNano1 = coreNano0;
      //現在の負荷率(%)
      //  現在の負荷率(%) = 100.0 * コアの所要時間(ns) / 本来の経過時間(ns)
      //  処理が間に合っていないとき100%よりも大きくなる
      double actualPercent = Math.max (1.0, 100.0 * coreNanoA / expectedNano);
      //負荷率の上限(%)
      double maxPercent = SoundSource.sndPlayOn ? 90.0 : 100.0;  //音声出力がONのとき90%、さもなくば100%
      //目標の動作周波数(MHz)
      //double targetMHz = mpuClockMHz;
      //現在の動作周波数(MHz)
      //double currentMHz = mpuCurrentMHz;
      if (mpuUtilOn) {  //目標の負荷率に近付ける
        //目標の負荷率(%)
        double targetPercent = Math.min (maxPercent, (double) mpuUtilRatio);
        mpuSetClockMHz ((1.2 - 0.2 * actualPercent / targetPercent) * mpuCurrentMHz);
      } else {  //目標の周波数に近づける
        mpuSetClockMHz (Math.min (maxPercent / actualPercent,
                                  1.2 - 0.2 * mpuCurrentMHz / mpuClockMHz) * mpuCurrentMHz);
      }
      Indicator.indUpdate (actualPercent);
      mpuAdjustmentCounter = MPU_ADJUSTMENT_INTERVAL;
    }
  }

  //mpuStop (message)
  //  コアのタスクが動いているとき、それを止める
  //  完全に止まってからデバッグダイアログを更新する
  public static void mpuStop (String message) {
    //トレース実行とステップ実行を中止する
    mpuAdvanceCount = 0;
    mpuStepCount = 0;
    mpuContinue = false;
    mpuStop1 (message);
  }  //mpuStop(String)
  public static void mpuStop1 (String message) {
    if (mpuTask == null) {  //既に停止しているか停止処理が始まっている
      return;
    }
    DisassembleList.ddpStoppedBy = message;  //停止理由
    mpuClockLimit = 0L;
    System.out.println (Multilingual.mlnJapanese ?
                        "MPU を停止します" :
                        "MPU stops");
    mpuTask.cancel ();
    mpuTask = null;
    //ステップ実行を継続する
    if (mpuStepCount != 0 && mpuContinue) {
      if (mpuStepCount == -1 || --mpuStepCount != 0) {
        mpuStep (mpuStepCount);
        return;
      }
    }
    mpuAdvanceCount = 0;
    mpuStepCount = 0;
    mpuContinue = false;
    //動作中だけ有効なボタンを無効にする
    for (AbstractButton button : mpuButtonsRunning) {
      button.setEnabled (false);
    }
    tmrTimer.schedule (new TimerTask () {
      @Override public void run () {
        mpuUpdateWindow ();
      }
    }, TMR_DELAY);
  }  //mpuStop1(message)

  //mpuAdvance (n)
  //  n回トレース実行する。0回は不可
  //  1ターンで実行するのであまり大きな値を指定しないこと
  //  終わってからデバッグダイアログを更新する
  //  コアが止まっていないときは何もしない
  public static void mpuAdvance (int n) {
    if (mpuTask != null) {  //コアが止まっていない
      return;
    }
    mpuAdvanceCount = n;
    DisassembleList.ddpStoppedBy = null;
    mpuTask = new TimerTask () {
      @Override public void run () {
        mpuContinue = true;
        do {
          mpuClockLimit = mpuClockTime + 1L;
          mpuExecuteCore ();
        } while (mpuContinue && --mpuAdvanceCount != 0);
        mpuClockLimit = 0L;
        if (mpuTask != null) {  //最初の命令のエラーで停止したときnullになっている場合がある
          mpuTask.cancel ();
          mpuTask = null;
        }
        if (mpuStepCount != 0 && mpuContinue) {  //ステップ実行中に分岐命令を1命令実行して自然に終了した
          if (mpuStepCount == -1 || --mpuStepCount != 0) {
            mpuStep (mpuStepCount);
            return;
          }
        }
        mpuAdvanceCount = 0;
        mpuStepCount = 0;
        mpuContinue = false;
        mpuUpdateWindow ();
      }
    };
    tmrTimer.schedule (mpuTask, TMR_DELAY);
  }  //mpuAdvance(int)

  //mpuStep (n)
  //  n回ステップ実行する。0回は不可。-1はステップアンティルリターン
  //  次の命令が分岐命令のときはトレース実行と同じ
  //  次の命令が分岐命令でないときはその直後まで実行する
  //  コアが止まっていないときは何もしない
  public static void mpuStep (int n) {
    if (mpuTask != null) {  //コアが止まっていない
      return;
    }
    mpuStepCount = n;
    Disassembler.disDisassemble (new StringBuilder (), regPC, regSRS);
    if ((Disassembler.disStatus & (Disassembler.DIS_ALWAYS_BRANCH | Disassembler.DIS_SOMETIMES_BRANCH)) != 0) {
      if (mpuStepCount == -1 &&  //ステップアンティルリターン
          (Disassembler.disOC == 0x4e73 ||  //RTE
           Disassembler.disOC == 0x4e74 ||  //RTD
           Disassembler.disOC == 0x4e75 ||  //RTR
           Disassembler.disOC == 0x4e77) &&  //RTS
          mpuUntilReturnSRS == regSRS &&  //ユーザモード/スーパーバイザモードが同じ
          (currentMPU < Model.MPU_MC68LC040 ||
           mpuUntilReturnRP == (regSRS != 0 ? MC68060.mmuSRP : MC68060.mmuURP)) &&  //MC68060のときルートポインタが同じ
          mpuUntilReturnPC0 != regPC0 &&  //プログラムカウンタが違う
          Integer.compareUnsigned (mpuUntilReturnSP, regRn[15]) <= 0) {  //スタックポインタが減っていない
        mpuAdvanceCount = 0;
        mpuStepCount = 0;
        mpuContinue = false;
        mpuUpdateWindow ();
        return;
      }
      mpuAdvance (1);
    } else {
      if (InstructionBreakPoint.IBP_ON) {
        InstructionBreakPoint.ibpInstant (Disassembler.disPC, DisassembleList.ddpSupervisorMode);
        mpuStart ();
      }
    }
  }  //mpuStep(int)

  //mpuStepUntilReturn ()
  //  ステップアンティルリターン
  //  RTD,RTE,RTR,RTSを実行する直前で
  //    ステップアンティルリターン開始時と
  //      ユーザモード/スーパーバイザモードが同じ
  //      MC68060のときルートポインタが同じ
  //      プログラムカウンタが違う
  //      スタックポインタが減っていない
  //    ならば停止する
  //  コアが止まっていないときは何もしない
  public static void mpuStepUntilReturn () {
    if (mpuTask != null) {  //コアが止まっていない
      return;
    }
    mpuUntilReturnSRS = regSRS;
    mpuUntilReturnRP = regSRS != 0 ? MC68060.mmuSRP : MC68060.mmuURP;
    mpuUntilReturnPC0 = regPC0;
    mpuUntilReturnSP = regRn[15];
    mpuStep (-1);
  }  //mpuStepUntilReturn()

  //mpuUpdateWindow ()
  //  停止したときデバッグ関連ウインドウやボタンを更新する
  public static void mpuUpdateWindow () {
    if (dbgVisibleMask != 0) {  //デバッグ関連ウインドウが表示されている
      if ((dbgVisibleMask & DBG_DDP_VISIBLE_MASK) != 0) {
        DisassembleList.ddpBacktraceRecord = -1L;  //分岐レコードの選択を解除する
        DisassembleList.ddpUpdate (-1, -1, false);
      }
      if (BranchLog.BLG_ON) {
        if ((dbgVisibleMask & DBG_BLG_VISIBLE_MASK) != 0) {
          BranchLog.blgUpdate (BranchLog.BLG_SELECT_NEWEST);
        }
      }
      if (ProgramFlowVisualizer.PFV_ON) {
        if ((dbgVisibleMask & DBG_PFV_VISIBLE_MASK) != 0) {
          ProgramFlowVisualizer.pfvUpdate ();
        }
      }
      if (RasterBreakPoint.RBP_ON) {
        if ((dbgVisibleMask & DBG_RBP_VISIBLE_MASK) != 0) {
          RasterBreakPoint.rbpUpdateFrame ();
        }
      }
      if (ScreenModeTest.SMT_ON) {
        if ((dbgVisibleMask & DBG_SMT_VISIBLE_MASK) != 0) {
          ScreenModeTest.smtUpdateFrame ();
        }
      }
      if (RootPointerList.RTL_ON) {
        if ((dbgVisibleMask & DBG_RTL_VISIBLE_MASK) != 0) {
          RootPointerList.rtlUpdateFrame ();
        }
      }
      if (SpritePatternViewer.SPV_ON) {
        if ((dbgVisibleMask & DBG_SPV_VISIBLE_MASK) != 0) {
          SpritePatternViewer.spvUpdateFrame ();
        }
      }
      if (PaletteViewer.PLV_ON) {
        if ((dbgVisibleMask & DBG_PLV_VISIBLE_MASK) != 0) {
          PaletteViewer.plvUpdateFrame ();
        }
      }
      if (ATCMonitor.ACM_ON) {
        if ((dbgVisibleMask & DBG_ACM_VISIBLE_MASK) != 0) {
          ATCMonitor.acmUpdateFrame ();
        }
      }
    }
    //コンソールにレジスタ一覧を表示する
    if (DebugConsole.dgtRequestRegs != 0) {
      if ((DebugConsole.dgtRequestRegs & 1) != 0) {  //整数レジスタを表示する
        ExpressionEvaluator.ElementType.ETY_COMMAND_REGS.etyEval (null, ExpressionEvaluator.EVM_COMMAND);
      }
      if ((DebugConsole.dgtRequestRegs & 2) != 0) {  //浮動小数点レジスタを表示する
        ExpressionEvaluator.ElementType.ETY_COMMAND_FLOAT_REGS.etyEval (null, ExpressionEvaluator.EVM_COMMAND);
      }
      if ((DebugConsole.dgtRequestRegs & 4) != 0) {  //プロンプトを表示する
        DebugConsole.dgtPrintPrompt ();
      }
      DebugConsole.dgtRequestRegs = 0;  //表示が完了してから0にする
    }
    //動作中だけ有効なボタンを無効にする
    for (AbstractButton button : mpuButtonsRunning) {
      button.setEnabled (false);
    }
    //停止中だけ有効なボタンを(電源がONのとき)有効にする
    for (AbstractButton button : mpuButtonsStopped) {
      button.setEnabled (!PowerControl.PWC_ON ||
                         PowerControl.pwcVcc1On);
    }
  }  //mpuUpdateWindow()

  //button = mpuMakeBreakButton ()
  //  停止ボタンを作る
  public static JButton mpuMakeBreakButton () {
    return mpuAddButtonRunning (
      Multilingual.mlnToolTipText (
        ComponentFactory.createImageButton (
          LnF.LNF_BREAK_IMAGE,
          LnF.LNF_BREAK_DISABLED_IMAGE,
          "Stop", mpuDebugActionListener),
        "ja", "停止")
      );
  }  //mpuMakeBreakButton()

  //button = mpuMakeTraceButton ()
  //  トレース実行ボタンを作る
  public static JButton mpuMakeTraceButton () {
    return mpuAddButtonStopped (
      Multilingual.mlnToolTipText (
        ComponentFactory.createImageButton (
          LnF.LNF_TRACE_IMAGE,
          LnF.LNF_TRACE_DISABLED_IMAGE,
          "Trace", mpuDebugActionListener),
        "ja", "トレース")
      );
  }  //mpuMakeTraceButton()

  //button = mpuMakeTrace10Button ()
  //  トレース10回ボタンを作る
  public static JButton mpuMakeTrace10Button () {
    return mpuAddButtonStopped (
      Multilingual.mlnToolTipText (
        ComponentFactory.createImageButton (
          LnF.LNF_TRACE_10_IMAGE,
          LnF.LNF_TRACE_10_DISABLED_IMAGE,
          "Trace 10 times", mpuDebugActionListener),
        "ja", "トレース 10 回")
      );
  }  //mpuMakeTrace10Button()

  //button = mpuMakeTrace100Button ()
  //  トレース100回ボタンを作る
  public static JButton mpuMakeTrace100Button () {
    return mpuAddButtonStopped (
      Multilingual.mlnToolTipText (
        ComponentFactory.createImageButton (
          LnF.LNF_TRACE_100_IMAGE,
          LnF.LNF_TRACE_100_DISABLED_IMAGE,
          "Trace 100 times", mpuDebugActionListener),
        "ja", "トレース 100 回")
      );
  }  //mpuMakeTrace100Button()

  //button = mpuMakeStepButton ()
  //  ステップ実行ボタンを作る
  public static JButton mpuMakeStepButton () {
    return mpuAddButtonStopped (
      Multilingual.mlnToolTipText (
        ComponentFactory.createImageButton (
          LnF.LNF_STEP_IMAGE,
          LnF.LNF_STEP_DISABLED_IMAGE,
          "Step", mpuDebugActionListener),
        "ja", "ステップ")
      );
  }  //mpuMakeStepButton()

  //button = mpuMakeStep10Button ()
  //  ステップ10回ボタンを作る
  public static JButton mpuMakeStep10Button () {
    return mpuAddButtonStopped (
      Multilingual.mlnToolTipText (
        ComponentFactory.createImageButton (
          LnF.LNF_STEP_10_IMAGE,
          LnF.LNF_STEP_10_DISABLED_IMAGE,
          "Step 10 times", mpuDebugActionListener),
        "ja", "ステップ 10 回")
      );
  }  //mpuMakeStep10Button()

  //button = mpuMakeStep100Button ()
  //  ステップ100回ボタンを作る
  public static JButton mpuMakeStep100Button () {
    return mpuAddButtonStopped (
      Multilingual.mlnToolTipText (
        ComponentFactory.createImageButton (
          LnF.LNF_STEP_100_IMAGE,
          LnF.LNF_STEP_100_DISABLED_IMAGE,
          "Step 100 times", mpuDebugActionListener),
        "ja", "ステップ 100 回")
      );
  }  //mpuMakeStep100Button()

  //button = mpuMakeReturnButton ()
  //  ステップアンティルリターンボタンを作る
  public static JButton mpuMakeReturnButton () {
    return mpuAddButtonStopped (
      Multilingual.mlnToolTipText (
        ComponentFactory.createImageButton (
          LnF.LNF_STEP_UNTIL_RETURN_IMAGE,
          LnF.LNF_STEP_UNTIL_RETURN_DISABLED_IMAGE,
          "Step until return", mpuDebugActionListener),
        "ja", "ステップアンティルリターン")
      );
  }  //mpuMakeReturnButton()

  //button = mpuMakeRunButton ()
  //  実行ボタンを作る
  public static JButton mpuMakeRunButton () {
    return mpuAddButtonStopped (
      Multilingual.mlnToolTipText (
        ComponentFactory.createImageButton (
          LnF.LNF_RUN_IMAGE,
          LnF.LNF_RUN_DISABLED_IMAGE,
          "Run", mpuDebugActionListener),
        "ja", "実行")
      );
  }  //mpuMakeRunButton()

  //button = mpuAddButtonRunning (button)
  //  MPUが動作中のときだけ有効なボタンを追加する
  public static <T extends AbstractButton> T mpuAddButtonRunning (T button) {
    button.setEnabled (mpuTask != null);
    mpuButtonsRunning.add (button);
    return button;
  }

  //button = mpuAddButtonStopped (button)
  //  MPUが停止中のときだけ有効なボタンを追加する
  public static <T extends AbstractButton> T mpuAddButtonStopped (T button) {
    button.setEnabled (mpuTask == null);
    mpuButtonsStopped.add (button);
    return button;
  }



  //========================================================================================
  //$$EMX エミュレータ拡張命令
  //  下位6bitの組み合わせで64個まで追加できる
  //  オペコードが変更される場合があるので原則としてユーザは使用禁止
  //  オペコード開始位置の上位10bitを変更するときはジャンプテーブルも修正すること
  //
  //  WAIT命令
  //    現在の割り込みマスクレベルで割り込みを受け付ける以外に何もしない
  //    やっていることはBRA *相当の無限ループだが、分岐ログは変化しない
  //    1回あたりの所要時間はMPUの種類や動作周波数に関係なく0.01msとする
  //    ホストのファイルにアクセスするといった時間のかかる処理を行うとき、
  //    コアを止めてしまうと本来ならば割り込みを用いて続けられる音楽やアニメーションが止まってしまうので、
  //    時間のかかる処理は別スレッドで行い、それが終わるまでコアはWAIT命令を繰り返して待つ
  public static final int EMX_OPCODE_BASE    = 0x4e00;  //オペコード開始位置
  public static final int EMX_OPCODE_HFSBOOT = EMX_OPCODE_BASE + 0x00;
  public static final int EMX_OPCODE_HFSINST = EMX_OPCODE_BASE + 0x01;
  public static final int EMX_OPCODE_HFSSTR  = EMX_OPCODE_BASE + 0x02;
  public static final int EMX_OPCODE_HFSINT  = EMX_OPCODE_BASE + 0x03;
  public static final int EMX_OPCODE_EMXNOP  = EMX_OPCODE_BASE + 0x04;
  public static final int EMX_OPCODE_EMXWAIT = EMX_OPCODE_BASE + 0x05;

  public static final String[] EMX_MNEMONIC_ARRAY = {
    "hfsboot",
    "hfsinst",
    "hfsstr",
    "hfsint",
    "emxnop",
    "emxwait",
  };

  //emxNop ()
  //  命令としては何もしない
  //  コードに埋め込んでパッチをあてたりするのに使う
  public static void emxNop () {
    if (MainMemory.mmrHumanVersion == 0x0302 && regPC0 == 0x00007140) {  //デバイスドライバを初期化する直前
      int head = regRn[9];  //初期化するデバイスドライバのデバイスヘッダのアドレス
      int tail = MC68060.mmuPeekLongData (0x00001c00, 1);  //デバイスドライバが使える末尾のアドレス
      //----------------------------------------------------------------
      //PCM8Aにパッチをあてる
      emxPatchPCM8A (head, tail);
      //----------------------------------------------------------------
      //RSDRV.SYS 2.02の先頭アドレスを保存する
      emxCheckRSDRV202 (head, tail);
    } else if (MainMemory.mmrHumanVersion == 0x0302 && regPC0 == 0x0000716c) {  //デバイスドライバを初期化した直後
      int head = regRn[9];  //初期化されたデバイスドライバのデバイスヘッダのアドレス
      int tail = MC68060.mmuPeekLongData (0x00001c00, 1);  //デバイスドライバが使える末尾のアドレス
      //----------------------------------------------------------------
      //060turbo.sysにパッチをあてる
      emxPatch060turbosys (head, tail);
      //----------------------------------------------------------------
      //FSX.Xのマウス受信データ処理ルーチンとマウスワークのアドレスを保存する
      if (Z8530.SCC_FSX_MOUSE) {
        emxCheckFSX (head, tail);
      }
      //----------------------------------------------------------------
      //TwentyOne.xのオプションのアドレスを保存する
      if (HFS.HFS_USE_TWENTY_ONE) {
        emxCheckTwentyOne (head, tail);
      }
      //----------------------------------------------------------------
      //ラベルをクリアする
      LabeledAddress.lblClear ();
    } else if (MainMemory.mmrHumanVersion == 0x0302 && regPC0 == 0x0000972c) {  //プロセスを起動する直前
      int head = regRn[8] + 256;  //起動するプロセスのメモリ管理テーブルのアドレス+256=プロセスの先頭
      //int tail = regRn[9];  //bssの末尾
      int tail = MC68060.mmuPeekLong (head - 208, 1);  //bssの先頭
      //----------------------------------------------------------------
      //bsio.x 0.21の先頭アドレスを保存する
      emxCheckBSIO021 (head, tail);
      //----------------------------------------------------------------
      //PCM8Aにパッチをあてる
      emxPatchPCM8A (head, tail);
      //----------------------------------------------------------------
      //tmsio.x 0.31の先頭アドレスを保存する
      emxCheckTMSIO031 (head, tail);
      //----------------------------------------------------------------
      //ラベルをクリアする
      LabeledAddress.lblClear ();
      //----------------------------------------------------------------
      //実行開始位置で停止する
      if (dbgStopAtStart) {
        InstructionBreakPoint.ibpInstant (regRn[12], 0);  //a4
      }
    } else if (MainMemory.mmrHumanVersion == 0x0302 && regPC0 == 0x0000a090) {  //プロセスが常駐した直後
      int head = regRn[8] + 256;  //常駐したプロセスのメモリ管理テーブルのアドレス+256=プロセスの先頭
      int tail = MC68060.mmuPeekLongData (regRn[8] + 8, 1);  //メモリブロックが使用している末尾
      String name = MC68060.mmuPeekStringZ (head - 60, 1);  //実行ファイル名
      if (name.equalsIgnoreCase ("fsx.x")) {
        //----------------------------------------------------------------
        //FSX.Xのマウス受信データ処理ルーチンとマウスワークのアドレスを保存する
        if (Z8530.SCC_FSX_MOUSE) {
          emxCheckFSX (head, tail);
        }
      }
      if (name.equalsIgnoreCase ("TwentyOne.x")) {
        //----------------------------------------------------------------
        //TwentyOne.xのオプションのアドレスを保存する
        if (HFS.HFS_USE_TWENTY_ONE) {
          emxCheckTwentyOne (head, tail);
        }
      }
    }
  }  //emxNop()

  public static final int[] emxPCM8AFFMap = {
    0x00000138, 0x000001f6, 0x00000394, 0x000011ec, 0x0000120a, 0x00001400, 0x00001814, 0x00001870, 0x00001882, 0x0000188a,
    0x00001892, 0x000018a2, 0x000018a8, 0x000018ca, 0x000018d4, 0x000018e0, 0x000018e8, 0x00001908, 0x000019e4, 0x00001afa,
    0x00001b58, 0x00001b7c, 0x00001bac, 0x00001c38, 0x00001ccc, 0x000021f8, 0x00002250, 0x00002258, 0x00002290, 0x000022a6,
    0x000022b0, 0x000022c0, 0x000022c8, 0x000022de, 0x000022ea, 0x000030c8, 0x000030de, 0x000030e6, 0x000030ea, 0x000030f6,
    0x00003112, 0x00003188, 0x0000334c, 0x0000338a, 0x000033a2, 0x000033c4, 0x000033d0, 0x0000341a, 0x00003428, 0x00003496,
    0x000034a6, 0x000034d6, 0x0000fe0e, 0x0000fec8, 0x0000feec, 0x0000ff46, 0x0000ff4e,
  };

  //emxPatchPCM8A (head, tail)
  //  headから始まるデバイスドライバまたはプロセスがPCM8A.X v1.02ならばパッチをあてる
  public static void emxPatchPCM8A (int head, int tail) {
    if (head + 0x0000ff60 <= tail &&
        MC68060.mmuPeekLongData (head + 0x10f8, 1) == 0x50434d38 &&  //PCM8
        MC68060.mmuPeekLongData (head + 0x10fc, 1) == 0x41313032) {  //A102
      System.out.println (Multilingual.mlnJapanese ?
                          "PCM8A.X 1.02 があります" :
                          "PCM8A.X 1.02 exists");
      int patched = 0;
      int failed = 0;
      //  I/Oポートのアドレスの上位8ビットが$FFになっているところを$00に修正します。(57箇所)
      for (int offset : emxPCM8AFFMap) {
        if (MC68060.mmuPeekByteZeroData (head + offset, 1) == 0xff) {
          MC68060.mmuPokeByteData (head + offset, 0x00, 1);
          patched++;
        } else {
          failed++;
        }
      }
      if (patched != 0) {
        System.out.printf (Multilingual.mlnJapanese ?
                           "PCM8A.X 1.02 にパッチをあてました (%d/%d)\n" :
                           "PCM8A.X 1.02 was patched (%d/%d)\n",
                           patched, patched + failed);
      }
    }
  }  //emxPatchPCM8A

  //emxPatch060turbosys (head, tail)
  //  060turbo.sysにパッチをあてる
  public static void emxPatch060turbosys (int head, int tail) {
    //  sysStat_8000::
    //  00000EC0  203C302E3536  move.l  #'0.56',d0
    //  00000EC6  227C30363054  movea.l #'060T',a1
    //  00000ECC  4E75          rts
    if (head + 0x00002000 <= tail &&
        MC68060.mmuPeekLongData (head + 0x00000ec0, 1) == 0x203c302e &&
        MC68060.mmuPeekLongData (head + 0x00000ec4, 1) == 0x3536227c &&
        MC68060.mmuPeekLongData (head + 0x00000ec8, 1) == 0x30363054) {  //060turbo.sys version 0.56
      System.out.println (Multilingual.mlnJapanese ?
                          "060turbo.sys 0.56 があります" :
                          "060turbo.sys 0.56 exists");
      //SCSIコールでバスエラーが出ることがある
      //  SRAMのソフト転送フラグを確認する命令がbtstではなくbsetになっている
      //  000021E6  08F9000400ED0070  bset.b  #4,$00ED0070  →  000021E6  0839000400ED0070  btst.b  #4,$00ED0070
      int patched = 0;
      int failed = 0;
      if (MC68060.mmuPeekLongData (head + 0x000021e6, 1) == 0x08f90004 &&
          MC68060.mmuPeekLongData (head + 0x000021ea, 1) == 0x00ed0070) {
        MC68060.mmuPokeWordData (head + 0x000021e6, 0x0839, 1);
        patched++;
      } else {
        failed++;
      }
      System.out.printf (Multilingual.mlnJapanese ?
                         "060turbo.sys 0.56 にパッチをあてました (%d/%d)\n" :
                         "060turbo.sys 0.56 was patched (%d/%d)\n",
                         patched, patched + failed);
    }
  }  //emxPatch060turbosys

  //emxCheckFSX (head, tail)
  //  headから始まるデバイスドライバまたはプロセスがFSX.Xならばマウス受信データ処理ルーチンとマウスワークのアドレスを保存する
  public static void emxCheckFSX (int head, int tail) {
    if (Z8530.SCC_FSX_MOUSE) {
      if (head + 0x00063200 <= tail &&
          "\r\nSX SYSTEM for X68000  version 3.10\r\nCopyright 1990,91,92,93,94 SHARP/First Class Technology\r\n".equals (MC68060.mmuPeekStringZ (head + 0x0001ae, 5))) {
        System.out.println (Multilingual.mlnJapanese ?
                            "FSX.X 3.10 があります" :
                            "FSX.X 3.10 exists");
        Z8530.sccFSXMouseHook = head + 0x04f82a;  //マウス受信データ処理ルーチン
        Z8530.sccFSXMouseWork = head + 0x063184;  //マウスワーク
      }
    }
  }  //emxCheckFSX

  //emxCheckRSDRV202 (head, tail)
  //  RSDRV.SYS 2.02の先頭アドレスを保存する
  //  あるからといって使われているとは限らない
  //  タイトル部分はバッファになるので実行前に呼び出すこと
  //  ボーレート11=76800bpsを指定できるようにパッチをあてる
  public static void emxCheckRSDRV202 (int head, int tail) {
    if (head + 0x000ea6 <= tail &&
        MC68060.mmuPeekEquals (head + 0x000e4e, "RS-232C DRIVER for X68000 version 2.02")) {
      if (RS232CTerminal.trmRSDRV202Head != head) {
        RS232CTerminal.trmRSDRV202Head = head;
        int[] patchData = {
          //                                 変更前                         変更後
          //                           05F6  B03C  cmp.b   #10,d0           B03C  cmp.b   #11,d0
          0x05f8, 0x000a, 0x000b,  //  05F8  000A                           000B
          //
          0x0600, 0xd040, 0x2048,  //  0600  D040  add.w   d0,d0            2048  movea.l a0,a0
          //
          0x060e, 0x3030, 0x4e90,  //  060E  3030  move.w  $00(a0,d0.w),d0  4E90  jsr     (a0)
          0x0610, 0x0000, 0x2048,  //  0610  0000                           2048  movea.l a0,a0
          //
          0x074e, 0x0821, 0x2041,  //  074E  0821  .dc.w   2081             2041  movea.l d1,a0
          0x0750, 0x0410, 0x3200,  //  0750  0410  .dc.w   1040             3200  move.w  d0,d1
          0x0752, 0x0207, 0x303c,  //  0752  0207  .dc.w   519              303C  move.w  #2083,d0
          0x0754, 0x0102, 0x0823,  //  0754  0102  .dc.w   258              0823
          0x0756, 0x0080, 0xe268,  //  0756  0080  .dc.w   128              E268  lsr.w   d1,d0
          0x0758, 0x003f, 0x72fe,  //  0758  003F  .dc.w   63               72FE  moveq.l #-2,d1
          0x075a, 0x001f, 0xd141,  //  075A  001F  .dc.w   31               D141  addx.w  d1,d0
          0x075c, 0x000e, 0x2208,  //  075C  000E  .dc.w   14               2208  move.l  a0,d1
          0x075e, 0x0006, 0x4e75,  //  075E  0006  .dc.w   6                4E75  rts
          //                           0760  0002  .dc.w   2
          //
          0x0ab0, 0x0040, 0x0400,  //  0AB0  0040  .dc.w   64               0400  .dc.w   1024
          0x0ad2, 0x0040, 0x0400,  //  0AD2  0040  .dc.w   64               0400  .dc.w   1024
          0x0af4, 0x0040, 0x0400,  //  0AF4  0040  .dc.w   64               0400  .dc.w   1024
          0x0b16, 0x0040, 0x0400,  //  0B16  0040  .dc.w   64               0400  .dc.w   1024
          0x0b38, 0x0040, 0x0400,  //  0B38  0040  .dc.w   64               0400  .dc.w   1024
          //
          //                           0CAC  B23C  cmp.b   #9,d1            B23C  cmp.b   #11,d1
          0x0cae, 0x0009, 0x000b,  //  0CAE  0009                           000B
        };
        int patched = 0;
        int failed = 0;
        for (int i = 0; i < patchData.length; i += 3) {
          int a = head + patchData[i];
          int b = patchData[i + 1];
          int c = patchData[i + 2];
          int d = MC68060.mmuPeekWordZeroData (a, 1);
          if (d == b) {
            MC68060.mmuPokeWordData (a, c, 1);
            patched++;
          } else if (d != c) {
            failed++;
          }
        }
        System.out.printf ("RSDRV.SYS 2.02 found at %08X and patched (%d/%d)\n", head, patched, patched + failed);
      }
    }
  }

  //emxCheckTMSIO031 (head, tail)
  //  tmsio.x 0.31の先頭アドレスを保存する
  //  あるからといって使われているとは限らない
  //  タイトル部分はバッファになるので実行前に呼び出すこと
  public static void emxCheckTMSIO031 (int head, int tail) {
    if (head + 0x000fc4 <= tail &&
        MC68060.mmuPeekEquals (head + 0x000d1c, "TMSIO version 0.31 Copyright (C) 1990-93 by Miki Hoshino")) {
      if (RS232CTerminal.trmTMSIO031Head != head) {
        RS232CTerminal.trmTMSIO031Head = head;
        System.out.printf ("TMSIO 0.31 found at %08X\n", head);
      }
    }
  }

  //emxCheckBSIO021 (head, tail)
  //  bsio.x 0.21の先頭アドレスを保存する
  //  あるからといって使われているとは限らない
  //  タイトル部分はバッファになるので実行前に呼び出すこと
  public static void emxCheckBSIO021 (int head, int tail) {
    if (head + 0x001c2c <= tail &&
        MC68060.mmuPeekEquals (head + 0x001a66, "BSIO  version 0.21 Copyright (C) 1994    By BAZU")) {
      if (RS232CTerminal.trmBSIO021Head != head) {
        RS232CTerminal.trmBSIO021Head = head;
        System.out.printf ("BSIO 0.21 found at %08X\n", head);
      }
    }
  }

  //emxCheckTwentyOne (head, tail)
  //  headから始まるデバイスドライバまたはプロセスがTwentyOne.xならばオプションのアドレスを保存する
  public static void emxCheckTwentyOne (int head, int tail) {
    if (HFS.HFS_USE_TWENTY_ONE &&
        head + 64 <= tail) {
      if (MainMemory.mmrTwentyOneOptionAddress != 0 ||  //TwentyOne.xのオプションのアドレスは確認済みまたは非対応のバージョン
          MainMemory.mmrHumanVersion <= 0) {  //Human68kのバージョンが未確認または未知のバージョン
        return;
      }
      int name1 = MC68060.mmuPeekLongData (head + 14, 1);
      if (name1 == ('*' << 24 | 'T' << 16 | 'w' << 8 | 'e')) {
        int name2 = MC68060.mmuPeekLongData (head + 18, 1);
        if (name2 == ('n' << 24 | 't' << 16 | 'y' << 8 | '*')) {  //TwentyOne.x v1.10まで
          MainMemory.mmrTwentyOneOptionAddress = -1;  //非対応
        }
      } else if (name1 == ('?' << 24 | 'T' << 16 | 'w' << 8 | 'e')) {
        int name2 = MC68060.mmuPeekLongData (head + 18, 1);
        if (name2 == ('n' << 24 | 't' << 16 | 'y' << 8 | '?') ||
            name2 == ('n' << 24 | 't' << 16 | 'y' << 8 | 'E')) {  //TwentyOne.x v1.11から
          System.out.println (Multilingual.mlnJapanese ?
                              "TwentyOne.x があります" :
                              "TwentyOne.x exists");
          MainMemory.mmrTwentyOneOptionAddress = head + 22;
        }
      }
    }
  }  //emxCheckTwentyOne



  //========================================================================================
  //$$IRP 命令の処理
  //
  //  変数名
  //    op                                   オペコード。iiii_qqq_nnn_mmm_rrr
  //    iiii  op >> 12                       命令の種類。ここでは定数
  //    qqq   (op >> 9) - (iiii << 3)        クイックイミディエイトまたはデータレジスタDqの番号
  //    aqq   (op >> 9) - ((iiii << 3) - 8)  アドレスレジスタAqの番号
  //    nnn   op >> 6 & 7                    デスティネーションの実効アドレスのモード
  //    ea    op & 63                        実効アドレスのモードとレジスタ
  //    mmm   ea >> 3                        実効アドレスのモード
  //    rrr   op & 7                         実効アドレスのレジスタ。DrまたはRrのときr[rrr]はr[ea]で代用できる
  //    cccc  op >> 8 & 15                   コンディションコード
  //    a                                    実効アドレス
  //    s                                    テンポラリ
  //    t                                    テンポラリ
  //    w                                    拡張ワード
  //    x                                    被演算数
  //    y                                    演算数
  //    z                                    結果
  //
  //  サイクル数
  //    mpuCycleCountにMC68000のサイクル数を加算する
  //      MC68030のサイクル数はMC68000のサイクル数の0.6倍とみなして計算する
  //      これはROM1.3で起動したときDBRA命令で計測される動作周波数の表示の辻褄を合わせるための係数であり、
  //      MC68000とMC68030のサイクル数の比の平均値ではない
  //        10MHzのMC68000と25MHzのMC68030の速度の比が25/10/0.6=4.17倍となるので何倍も外れてはいないと思われる
  //    MC68000に存在しない命令のサイクル数はMC68000に存在すると仮定した場合の推定値を用いる
  //      オペコードを含むリードとライトを1ワードあたり4サイクルとする
  //
  //  拡張命令
  //    差し障りのない範囲でいくつかの命令を追加してある
  //    エミュレータ拡張命令
  //      HFSBOOT                                         |-|012346|-|-----|-----|          |0100_111_000_000_000
  //      HFSINST                                         |-|012346|-|-----|-----|          |0100_111_000_000_001
  //      HFSSTR                                          |-|012346|-|-----|-----|          |0100_111_000_000_010
  //      HFSINT                                          |-|012346|-|-----|-----|          |0100_111_000_000_011
  //      EMXNOP                                          |-|012346|-|-----|-----|          |0100_111_000_000_100
  //    MC68000で欠番になっているオペコードに割り当てられているColdFireの命令
  //      BITREV.L Dr                                     |-|------|-|-----|-----|D         |0000_000_011_000_rrr (ISA_C)
  //      BYTEREV.L Dr                                    |-|------|-|-----|-----|D         |0000_001_011_000_rrr (ISA_C)
  //      FF1.L Dr                                        |-|------|-|-UUUU|-**00|D         |0000_010_011_000_rrr (ISA_C)
  //      MVS.B <ea>,Dq                                   |-|------|-|-UUUU|-**00|D M+-WXZPI|0111_qqq_100_mmm_rrr (ISA_B)
  //      MVS.W <ea>,Dq                                   |-|------|-|-UUUU|-**00|D M+-WXZPI|0111_qqq_101_mmm_rrr (ISA_B)
  //      MVZ.B <ea>,Dq                                   |-|------|-|-UUUU|-0*00|D M+-WXZPI|0111_qqq_110_mmm_rrr (ISA_B)
  //      MVZ.W <ea>,Dq                                   |-|------|-|-UUUU|-0*00|D M+-WXZPI|0111_qqq_111_mmm_rrr (ISA_B)
  //      SATS.L Dr                                       |-|------|-|-UUUU|-**00|D         |0100_110_010_000_rrr (ISA_B)

  public static final boolean IRP_BITREV_REVERSE = false;  //true=BITREVでInteger.reverseを使う
  public static final boolean IRP_BITREV_SHIFT = false;  //true=BITREVでシフト演算子を使う
  public static final boolean IRP_BITREV_TABLE = true;  //true=BITREVでテーブルを使う

  public static final boolean IRP_MOVEM_MAINMEMORY = true;  //true=000のときMOVEMでメインメモリを特別扱いにする
  public static final boolean IRP_MOVEM_EXPAND = false;  //true=MOVEMで16回展開する。遅くなる
  public static final boolean IRP_MOVEM_LOOP = false;  //true=MOVEMで16回ループする。コンパイラが展開する
  public static final boolean IRP_MOVEM_SHIFT_LEFT = false;  //true=MOVEMで0になるまで左にシフトする。reverseが入る分遅い
  public static final boolean IRP_MOVEM_SHIFT_RIGHT = true;  //true=MOVEMで0になるまで右にシフトする
  public static final boolean IRP_MOVEM_ZEROS = false;  //true=MOVEMでInteger.numberOfTrailingZerosを使う。ループ回数は少ないがスキップする処理が冗長になるので最速ではない

  //  リセット命令
  public static void irpReset () {
    long t = System.nanoTime ();
    //メインメモリのmmrResetとバスコントローラのbusResetはmpuResetへ
    CRTC.crtReset ();  //CRT CRTコントローラ
    VideoController.vcnReset ();  //VCN ビデオコントローラ
    HD63450.dmaReset ();  //DMA DMAコントローラ
    MC68901.mfpReset ();  //MFP MFP  rtcReset,pwcResetより前
    Keyboard.kbdReset ();  //KBD キーボード
    RP5C15.rtcReset ();  //RTC RTC  mfpResetより後
    if (PowerControl.PWC_ON) {
      PowerControl.pwcReset ();  //PWC 電源制御  mfpResetより後
    }
    PrinterPort.prnReset ();  //PRN プリンタポート
    SoundSource.sndReset ();  //SND サウンド
    OPM.opmReset ();  //OPM FM音源
    ADPCM.pcmReset ();  //PCM ADPCM音源
    FDC.fdcReset ();  //FDC FDコントローラ
    IOInterrupt.ioiReset ();  //IOI I/O割り込み
    eb2Reset ();  //EB2 拡張ボードレベル2割り込み
    if (HostCDROM.HCD_ENABLED) {  //HCD ホストCD-ROM
      HostCDROM.hcdReset ();
    }
    SPC.spcReset ();  //SPC SCSIプロトコルコントローラ
    Z8530.sccReset ();  //SCC SCC
    RS232CTerminal.trmReset ();  //TRM RS-232C設定とターミナル
    PPI.ppiReset ();  //PPI PPI
    HFS.hfsReset ();  //HFS ホストファイルシステムインタフェイス
    SpriteScreen.sprReset ();  //SPR スプライト画面
    //smrReset()はspcSCSIEXOnとspcSCSIINOnを使うのでSPC.spcReset()よりも後であること
    if (MercuryUnit.MU4_ON) {
      MercuryUnit.mu4Reset ();
    }
    xt3Reset ();  //XT3 Xellent30
    SRAM.smrReset ();  //SMR SRAM
    CONDevice.conReset ();  //CON CONデバイス制御
    TextCopy.txcReset ();  //TXC テキストコピー。romReset()より後
    //9msを上限として所要時間を仮想時間に加える
    mpuClockTime += Math.min (TMR_FREQ * 9 / 1000, (System.nanoTime () - t) * 1000L);
  }  //irpReset()

  //右シフト・ローテート命令
  //
  //  ASR
  //    X  countが0のとき変化しない。他は最後に押し出されたビット
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときクリア。他は最後に押し出されたビット
  //    ASR.B
  //         ........................ｱｲｳｴｵｶｷｸ X C
  //       0 ........................ｱｲｳｴｵｶｷｸ X 0
  //       1 ........................ｱｱｲｳｴｵｶｷ ｸ ｸ
  //       :
  //       7 ........................ｱｱｱｱｱｱｱｱ ｲ ｲ
  //       8 ........................ｱｱｱｱｱｱｱｱ ｱ ｱ
  //    ASR.W
  //         ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X C
  //       0 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X 0
  //       1 ................ｱｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿ ﾀ ﾀ
  //       :
  //      15 ................ｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱ ｲ ｲ
  //      16 ................ｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱ ｱ ｱ
  //    ASR.L
  //         ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X C
  //       0 ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X 0
  //       1 ｱｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏ ﾐ ﾐ
  //       :
  //      31 ｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱ ｲ ｲ
  //      32 ｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱｱ ｱ ｱ
  //
  //  LSR
  //    X  countが0のとき変化しない。他は最後に押し出されたビット
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときクリア。他は最後に押し出されたビット
  //    LSR.B
  //         ........................ｱｲｳｴｵｶｷｸ X C
  //       0 ........................ｱｲｳｴｵｶｷｸ X 0
  //       1 ........................0ｱｲｳｴｵｶｷ ｸ ｸ
  //       :
  //       7 ........................0000000ｱ ｲ ｲ
  //       8 ........................00000000 ｱ ｱ
  //       9 ........................00000000 0 0
  //    LSR.W
  //         ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X C
  //       0 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X 0
  //       1 ................0ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿ ﾀ ﾀ
  //       :
  //      15 ................000000000000000ｱ ｲ ｲ
  //      16 ................0000000000000000 ｱ ｱ
  //      17 ................0000000000000000 0 0
  //    LSR.L
  //         ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X C
  //       0 ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X 0
  //       1 0ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏ ﾐ ﾐ
  //       :
  //      31 0000000000000000000000000000000ｱ ｲ ｲ
  //      32 00000000000000000000000000000000 ｱ ｱ
  //      33 00000000000000000000000000000000 0 0
  //
  //  ROXR
  //    X  countが0のとき変化しない。他は最後に押し出されたビット
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときXのコピー。他は最後に押し出されたビット
  //    ROXR.B
  //         ........................ｱｲｳｴｵｶｷｸ X C
  //       0 ........................ｱｲｳｴｵｶｷｸ X X
  //       1 ........................Xｱｲｳｴｵｶｷ ｸ ｸ
  //       2 ........................ｸXｱｲｳｴｵｶ ｷ ｷ
  //       :
  //       7 ........................ｳｴｵｶｷｸXｱ ｲ ｲ
  //       8 ........................ｲｳｴｵｶｷｸX ｱ ｱ
  //       9 ........................ｱｲｳｴｵｶｷｸ X X
  //    ROXR.W
  //         ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X C
  //       0 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X X
  //       1 ................Xｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿ ﾀ ﾀ
  //       2 ................ﾀXｱｲｳｴｵｶｷｸｹｺｻｼｽｾ ｿ ｿ
  //       :
  //      15 ................ｳｴｵｶｷｸｹｺｻｼｽｾｿﾀXｱ ｲ ｲ
  //      16 ................ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀX ｱ ｱ
  //      17 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X X
  //    ROXR.L
  //         ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X C
  //       0 ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X X
  //       1 Xｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏ ﾐ ﾐ
  //       2 ﾐXｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎ ﾏ ﾏ
  //       :
  //      31 ｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐXｱ ｲ ｲ
  //      32 ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐX ｱ ｱ
  //      33 ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X X
  //
  //  ROR
  //    X  常に変化しない
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときクリア。他は結果の最上位ビット
  //    ROR.B
  //         ........................ｱｲｳｴｵｶｷｸ X C
  //       0 ........................ｱｲｳｴｵｶｷｸ X 0
  //       1 ........................ｸｱｲｳｴｵｶｷ X ｸ
  //       :
  //       7 ........................ｲｳｴｵｶｷｸｱ X ｲ
  //       8 ........................ｱｲｳｴｵｶｷｸ X ｱ
  //    ROR.W
  //         ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X C
  //       0 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X 0
  //       1 ................ﾀｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿ X ﾀ
  //       :
  //      15 ................ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀｱ X ｲ
  //      16 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X ｱ
  //    ROR.L
  //         ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X C
  //       0 ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X 0
  //       1 ﾐｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏ X ﾐ
  //       :
  //      31 ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐｱ X ｲ
  //      32 ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X ｱ

  //左シフト・ローテート命令
  //
  //  ASL
  //    X  countが0のとき変化しない。他は最後に押し出されたビット
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  ASRで元に戻せないときセット。他はクリア
  //    C  countが0のときクリア。他は最後に押し出されたビット
  //    ASL.B
  //         ........................ｱｲｳｴｵｶｷｸ X C
  //       0 ........................ｱｲｳｴｵｶｷｸ X 0
  //       1 ........................ｲｳｴｵｶｷｸ0 ｱ ｱ
  //       :
  //       7 ........................ｸ0000000 ｷ ｷ
  //       8 ........................00000000 ｸ ｸ
  //       9 ........................00000000 0 0
  //    ASL.W
  //         ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X C
  //       0 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X 0
  //       1 ................ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ0 ｱ ｱ
  //       :
  //      15 ................ﾀ000000000000000 ｿ ｿ
  //      16 ................0000000000000000 ﾀ ﾀ
  //      17 ................0000000000000000 0 0
  //    ASL.L
  //         ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X C
  //       0 ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X 0
  //       1 ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ0 ｱ ｱ
  //       :
  //      31 ﾐ0000000000000000000000000000000 ﾏ ﾏ
  //      32 00000000000000000000000000000000 ﾐ ﾐ
  //      33 00000000000000000000000000000000 0 0
  //
  //  LSL
  //    X  countが0のとき変化しない。他は最後に押し出されたビット
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときクリア。他は最後に押し出されたビット
  //    LSL.B
  //         ........................ｱｲｳｴｵｶｷｸ X C
  //       0 ........................ｱｲｳｴｵｶｷｸ X 0
  //       1 ........................ｲｳｴｵｶｷｸ0 ｱ ｱ
  //       :
  //       7 ........................ｸ0000000 ｷ ｷ
  //       8 ........................00000000 ｸ ｸ
  //       9 ........................00000000 0 0
  //    LSL.W
  //         ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X C
  //       0 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X 0
  //       1 ................ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ0 ｱ ｱ
  //       :
  //      15 ................ﾀ000000000000000 ｿ ｿ
  //      16 ................0000000000000000 ﾀ ﾀ
  //      17 ................0000000000000000 0 0
  //    LSL.L
  //         ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X C
  //       0 ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X 0
  //       1 ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ0 ｱ ｱ
  //       :
  //      31 ﾐ0000000000000000000000000000000 ﾏ ﾏ
  //      32 00000000000000000000000000000000 ﾐ ﾐ
  //      33 00000000000000000000000000000000 0 0
  //
  //  ROXL
  //    X  countが0のとき変化しない。他は最後に押し出されたビット
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときXのコピー。他は最後に押し出されたビット
  //    ROXL.B
  //         ........................ｱｲｳｴｵｶｷｸ X C
  //       0 ........................ｱｲｳｴｵｶｷｸ X X
  //       1 ........................ｲｳｴｵｶｷｸX ｱ ｱ
  //       2 ........................ｳｴｵｶｷｸXｱ ｲ ｲ
  //       :
  //       7 ........................ｸXｱｲｳｴｵｶ ｷ ｷ
  //       8 ........................Xｱｲｳｴｵｶｷ ｸ ｸ
  //       9 ........................ｱｲｳｴｵｶｷｸ X X
  //    ROXL.W
  //         ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X C
  //       0 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X X
  //       1 ................ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀX ｱ ｱ
  //       2 ................ｳｴｵｶｷｸｹｺｻｼｽｾｿﾀXｱ ｲ ｲ
  //       :
  //      15 ................ﾀXｱｲｳｴｵｶｷｸｹｺｻｼｽｾ ｿ ｿ
  //      16 ................Xｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿ ﾀ ﾀ
  //      17 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X X
  //    ROXL.L
  //         ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X C
  //       0 ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X X
  //       1 ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐX ｱ ｱ
  //       2 ｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐXｱ ｲ ｲ
  //       :
  //      31 ﾐXｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎ ﾏ ﾏ
  //      32 Xｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏ ﾐ ﾐ
  //      33 ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X X
  //
  //  ROL
  //    X  常に変化しない
  //    N  結果の最上位ビット
  //    Z  結果が0のときセット。他はクリア
  //    V  常にクリア
  //    C  countが0のときクリア。他は結果の最下位ビット
  //    ROL.B
  //         ........................ｱｲｳｴｵｶｷｸ X C
  //       0 ........................ｱｲｳｴｵｶｷｸ X 0
  //       1 ........................ｲｳｴｵｶｷｸｱ X ｱ
  //       :
  //       7 ........................ｸｱｲｳｴｵｶｷ X ｷ
  //       8 ........................ｱｲｳｴｵｶｷｸ X ｸ
  //    ROL.W
  //         ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X C
  //       0 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X 0
  //       1 ................ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀｱ X ｱ
  //       :
  //      15 ................ﾀｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿ X ｿ
  //      16 ................ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀ X ﾀ
  //    ROL.L
  //         ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X C
  //       0 ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X 0
  //       1 ｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐｱ X ｱ
  //       :
  //      31 ﾐｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏ X ﾏ
  //      32 ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐ X ﾐ



  //========================================================================================
  //以下のメソッドはインライン展開する
/*
  public static void pushb (int x) throws M68kException {
    wb (r[15] -= 2, x);  //ワードの上位側
  }  //pushb(int)
  public static void pushw (int x) throws M68kException {
    ww (r[15] -= 2, x);
  }  //pushw(int)
  public static void pushl (int x) throws M68kException {
    wl (r[15] -= 4, x);
  }  //pushl(int)

  public static int popbs () throws M68kException {
    return rbs ((r[15] += 2) - 2);  //ワードの上位側
  }  //popbs()
  public static int popbz () throws M68kException {
    return rbz ((r[15] += 2) - 2);  //ワードの上位側
  }  //popbz()
  public static int popws () throws M68kException {
    return rws ((r[15] += 2) - 2);
  }  //popws()
  public static int popwz () throws M68kException {
    return rwz ((r[15] += 2) - 2);
  }  //popwz()
  public static int popls () throws M68kException {
    return rls ((r[15] += 4) - 4);
  }  //popls()

  public static int pcbs () throws M68kException {
    return rbs ((pc += 2) - 1);  //ワードの下位側
  }  //pcbs()
  public static int pcbz () throws M68kException {
    return rbz ((pc += 2) - 1);  //ワードの下位側
  }  //pcbz()
  public static int pcws () throws M68kException {
    return rwse ((pc += 2) - 2);
  }  //pcws()
  public static int pcwz () throws M68kException {
    return rwze ((pc += 2) - 2);
  }  //pcwz()
  public static int pcls () throws M68kException {
    return rlse ((pc += 4) - 4);
  }  //pcls()

  public static void ccr_tst (int z) {  //Xは変化しない。VとCはクリア
    ccr = z >> 28 & CCR_N | (z == 0 ? ccr & CCR_X | CCR_Z : ccr & CCR_X);  //ccr_tst
  }  //ccr_tst(int)
  public static void ccr_btst (int z) {  //Z以外は変化しない
    ccr = (ccr & (CCR_X | CCR_N | CCR_V | CCR_C) | (z == 0 ? CCR_Z : 0));
  }  //ccr_btst(int)
  public static void ccr_clr () {  //Xは変化しない。Zはセット。NとVとCはクリア
    ccr = ccr & CCR_X | CCR_Z;  //ccr_clr
  }  //ccr_clr(int)

  //                  x-y V                                  x-y C
  //  x  y|  0   1   2   3  -4  -3  -2  -1   x  y|  0   1   2   3   4   5   6   7
  //  ----+--------------------------------  ----+--------------------------------
  //    0 |  0  -1  -2  -3   4*  3   2   1     0 |  0  -1* -2* -3* -4* -5* -6* -7*
  //    1 |  1   0  -1  -2   5*  4*  3   2     1 |  1   0  -1* -2* -3* -4* -5* -6*
  //    2 |  2   1   0  -1   6*  5*  4*  3     2 |  2   1   0  -1* -2* -3* -4* -5*
  //    3 |  3   2   1   0   7*  6*  5*  4*    3 |  3   2   1   0  -1* -2* -3* -4*
  //   -4 | -4  -5* -6* -7*  0  -1  -2  -3     4 |  4   3   2   1   0  -1* -2* -3*
  //   -3 | -3  -4  -5* -6*  1   0  -1  -2     5 |  5   4   3   2   1   0  -1* -2*
  //   -2 | -2  -3  -4  -5*  2   1   0  -1     6 |  6   5   4   3   2   1   0  -1*
  //   -1 | -1  -2  -3  -4   3   2   1   0     7 |  7   6   5   4   3   2   1   0
  //                 x-y-1 V                                x-y-1 C
  //  x  y|  0   1   2   3  -4  -3  -2  -1   x  y|  0   1   2   3   4   5   6   7
  //  ----+--------------------------------  ----+--------------------------------
  //    0 | -1  -2  -3  -4   3   2   1   0     0 | -1* -2* -3* -4* -5* -6* -7* -8*
  //    1 |  0  -1  -2  -3   4*  3   2   1     1 |  0  -1* -2* -3* -4* -5* -6* -7*
  //    2 |  1   0  -1  -2   5*  4*  3   2     2 |  1   0  -1* -2* -3* -4* -5* -6*
  //    3 |  2   1   0  -1   6*  5*  4*  3     3 |  2   1   0  -1* -2* -3* -4* -5*
  //   -4 | -5* -6* -7* -8* -1  -2  -3  -4     4 |  3   2   1   0  -1* -2* -3* -4*
  //   -3 | -4  -5* -6* -7*  0  -1  -2  -3     5 |  4   3   2   1   0  -1* -2* -3*
  //   -2 | -3  -4  -5* -6*  1   0  -1  -2     6 |  5   4   3   2   1   0  -1* -2*
  //   -1 | -2  -3  -4  -5*  2   1   0  -1     7 |  6   5   4   3   2   1   0  -1*
  //  x        y         z=x-y    v        c         z=x-y-1  v        c
  //  00000000 00001111  01111000 00001000 01111111  11110000 00000000 11111111
  //  00000000 00001111  00111100 00001100 00111111  01111000 00001000 01111111
  //  00000000 00001111  00011110 00001110 00011111  00111100 00001100 00111111
  //  00000000 00001111  00001111 00001111 00001111  00011110 00001110 00011111
  //  11111111 00001111  10000111 01110000 00000111  00001111 11110000 00001111
  //  11111111 00001111  11000011 00110000 00000011  10000111 01110000 00000111
  //  11111111 00001111  11100001 00010000 00000001  11000011 00110000 00000011
  //  11111111 00001111  11110000 00000000 00000000  11100001 00010000 00000001
  //  Vは右上と左下でxとzが異なる部分
  //    V = ((x ^ y) & (x ^ z)) < 0
  //  Cは右上全部および左上と右下でzがある部分
  //    C = (~x & y | ~(x ^ y) & z) < 0
  //    ~を使わずに書けるおそらく最短の等価式
  //    C = (x & (y ^ z) ^ (y | z)) < 0
  //      perl -e "for$x(0..1){for$y(0..1){for$z(0..1){print join(',',$x,$y,$z,(1^$x)&$y|(1^($x^$y))&$z,$x&($y^$z)^($y|$z)).chr(10);}}}"
  //      0,0,0,0,0
  //      0,0,1,1,1
  //      0,1,0,1,1
  //      0,1,1,1,1
  //      1,0,0,0,0
  //      1,0,1,0,0
  //      1,1,0,0,0
  //      1,1,1,1,1
  public static void ccr_sub (int x, int y, int z) {
    ccr = (z >> 28 & CCR_N | (z == 0 ? CCR_Z : 0) |
           ((x ^ y) & (x ^ z)) >>> 31 << 1 |
           (x & (y ^ z) ^ (y | z)) >> 31 & (CCR_X | CCR_C));
  }  //ccr_sub(int,int,int)
  public static void ccr_subx (int x, int y, int z) {  //Zは結果が0のとき変化しない
    ccr = (z >> 28 & CCR_N | (z == 0 ? ccr & CCR_Z : 0) |
           ((x ^ y) & (x ^ z)) >>> 31 << 1 |
           (x & (y ^ z) ^ (y | z)) >> 31 & (CCR_X | CCR_C));
  }  //ccr_subx(int,int,int)
  public static void ccr_subq (int x, int y, int z) {  //ccr_subを常にy>0としたもの。Vは負→正のとき1。Cは正→負のとき1
    ccr = (z >> 28 & CCR_N | (z == 0 ? CCR_Z : 0) |
           (x & ~z) >>> 31 << 1 |
           (~x & z) >> 31 & (CCR_X | CCR_C));
  }  //ccr_subq(int,int,int)
  public static void ccr_neg (int y, int z) {  //ccr_subを常にx==0としたもの。Vは-MAX→-MAXのみ1。Cは0→0以外1
    ccr = (z >> 28 & CCR_N | (z == 0 ? CCR_Z : 0) |
           (y & z) >>> 31 << 1 |
           (y | z) >> 31 & (CCR_X | CCR_C));
  }  //ccr_neg(int,int)
  public static void ccr_negx (int y, int z) {  //ccr_subxを常にx==0としたもの。Zは結果が0のとき変化しない
    ccr = (z >> 28 & CCR_N | (z == 0 ? ccr & CCR_Z : 0) |
           (y & z) >>> 31 << 1 |
           (y | z) >> 31 & (CCR_X | CCR_C));
  }  //ccr_negx(int,int)
  public static void ccr_cmp (int x, int y, int z) {  //Xは変化しない
    ccr = (z >> 28 & CCR_N | (z == 0 ? ccr & CCR_X | CCR_Z : ccr & CCR_X) |
           ((x ^ y) & (x ^ z)) >>> 31 << 1 |
           (x & (y ^ z) ^ (y | z)) >>> 31);
  }  //ccr_cmp(int,int,int)

  //                  x+y V                                  x+y C
  //  x  y|  0   1   2   3  -4  -3  -2  -1   x  y|  0   1   2   3   4   5   6   7
  //  ----+--------------------------------  ----+--------------------------------
  //    0 |  0   1   2   3  -4  -3  -2  -1     0 |  0   1   2   3   4   5   6   7
  //    1 |  1   2   3   4* -3  -2  -1   0     1 |  1   2   3   4   5   6   7   8*
  //    2 |  2   3   4*  5* -2  -1   0   1     2 |  2   3   4   5   6   7   8*  9*
  //    3 |  3   4*  5*  6* -1   0   1   2     3 |  3   4   5   6   7   8*  9* 10*
  //   -4 | -4  -3  -2  -1  -8* -7* -6* -5*    4 |  4   5   6   7   8*  9* 10* 11*
  //   -3 | -3  -2  -1   0  -7* -6* -5* -4     5 |  5   6   7   8*  9* 10* 11* 12*
  //   -2 | -2  -1   0   1  -6* -5* -4  -3     6 |  6   7   8*  9* 10* 11* 12* 13*
  //   -1 | -1   0   1   2  -5* -4  -3  -2     7 |  7   8*  9* 10* 11* 12* 13* 14*
  //                 x+y+1 V                                x+y+1 C
  //  x  y|  0   1   2   3  -4  -3  -2  -1   x  y|  0   1   2   3   4   5   6   7
  //  ----+--------------------------------  ----+--------------------------------
  //    0 |  1   2   3   4* -3  -2  -1   0     0 |  1   2   3   4   5   6   7   8*
  //    1 |  2   3   4*  5* -2  -1   0   1     1 |  2   3   4   5   6   7   8*  9*
  //    2 |  3   4*  5*  6* -1   0   1   2     2 |  3   4   5   6   7   8*  9* 10*
  //    3 |  4*  5*  6*  7*  0   1   2   3     3 |  4   5   6   7   8*  9* 10* 11*
  //   -4 | -3  -2  -1   0  -7* -6* -5* -4     4 |  5   6   7   8*  9* 10* 11* 12*
  //   -3 | -2  -1   0   1  -6* -5* -4  -3     5 |  6   7   8*  9* 10* 11* 12* 13*
  //   -2 | -1   0   1   2  -5* -4  -3  -2     6 |  7   8*  9* 10* 11* 12* 13* 14*
  //   -1 |  0   1   2   3  -4  -3  -2  -1     7 |  8*  9* 10* 11* 12* 13* 14* 15*
  //  x        y         z=x+y    v        c         z=x+y+1  v        c
  //  00000000 00001111  00001111 00000000 00000000  00011110 00010000 00000001
  //  00000000 00001111  00011110 00010000 00000001  00111100 00110000 00000011
  //  00000000 00001111  00111100 00110000 00000011  01111000 01110000 00000111
  //  00000000 00001111  01111000 01110000 00000111  11110000 11110000 00001111
  //  11111111 00001111  11110000 00001111 00001111  11100001 00001110 00011111
  //  11111111 00001111  11100001 00001110 00011111  11000011 00001100 00111111
  //  11111111 00001111  11000011 00001100 00111111  10000111 00001000 01111111
  //  11111111 00001111  10000111 00001000 01111111  00001111 00000000 11111111
  //  Vは左上と右下でxとzが異なる部分
  //    V = (~(x ^ y) & (x ^ z)) < 0
  //    ~を使わずに書けるおそらく最短の等価式
  //    V = ((x ^ z) & (y ^ z)) < 0
  //      perl -e "for$x(0..1){for$y(0..1){for$z(0..1){print join(',',$x,$y,$z,(1^($x^$y))&($x^$z),($x^$z)&($y^$z)).chr(10);}}}"
  //      0,0,0,0,0
  //      0,0,1,1,1
  //      0,1,0,0,0
  //      0,1,1,0,0
  //      1,0,0,0,0
  //      1,0,1,0,0
  //      1,1,0,1,1
  //      1,1,1,0,0
  //  Cは右下全部および右上と左下でzがない部分
  //    C = (x & y | (x ^ y) & ~z) < 0
  //    ~を使わずに書けるおそらく最短の等価式
  //    C = ((x | y) ^ (x ^ y) & z) < 0
  //      perl -e "for$x(0..1){for$y(0..1){for$z(0..1){print join(',',$x,$y,$z,$x&$y|($x^$y)&(1^$z),($x|$y)^($x^$y)&$z).chr(10);}}}"
  //      0,0,0,0,0
  //      0,0,1,0,0
  //      0,1,0,1,1
  //      0,1,1,0,0
  //      1,0,0,1,1
  //      1,0,1,0,0
  //      1,1,0,1,1
  //      1,1,1,1,1
  public static void ccr_add (int x, int y, int z) {
    ccr = (z >> 28 & CCR_N | (z == 0 ? CCR_Z : 0) |
           ((x ^ z) & (y ^ z)) >>> 31 << 1 |
           ((x | y) ^ (x ^ y) & z) >> 31 & (CCR_X | CCR_C));
  }  //ccr_add(int,int,int)
  public static void ccr_addx (int x, int y, int z) {  //Zは結果が0のとき変化しない
    ccr = (z >> 28 & CCR_N | (z == 0 ? ccr & CCR_Z : 0) |
           ((x ^ z) & (y ^ z)) >>> 31 << 1 |
           ((x | y) ^ (x ^ y) & z) >> 31 & (CCR_X | CCR_C));
  }  //ccr_addx(int,int,int)
  public static void ccr_addq (int x, int y, int z) {  //ccr_addを常にy>0としたもの。Vは正→負のとき1。Cは負→正のとき1
    ccr = (z >> 28 & CCR_N | (z == 0 ? CCR_Z : 0) |
           (~x & z) >>> 31 << 1 |
           (x & ~z) >> 31 & (CCR_X | CCR_C));
  }  //ccr_addq(int,int,int)
*/



  //========================================================================================
  //$$EFA 実効アドレス計算
  //
  //  アドレッシングモードとサイクル数
  //    DrまたはArを指定すると不当命令になる
  //    例えばArを指定できない命令ならDrだけを特別扱いにして残りをefa～に渡せばArは自動的に不当命令になる
  //    実効アドレス計算とオペランドのアクセス1回分のサイクル数がmpuCycleCountに加算される
  //                        mmm rrr  Any Mem Mlt Cnt Clt  Byte Word Long Quad Extd  LeaPea JmpJsr
  //         -------------  --- ---  --- --- --- --- ---  ---- ---- ---- ---- ----  ------ ------
  //      M  (Ar)           010 rrr    *   *   *   *   *     4    4    8   16   24       4      8
  //      +  (Ar)+          011 rrr    *   *   *             4    4    8   16   24
  //      -  -(Ar)          100 rrr    *   *   *             6    6   10   18   26
  //      W  (d16,Ar)       101 rrr    *   *   *   *   *     8    8   12   20   28       8     10
  //      X  (d8,Ar,Rn.wl)  110 rrr    *   *   *   *   *    10   10   14   22   30      12     14
  //      Z  (xxx).W        111 000    *   *   *   *   *     8    8   12   20   28       8     10
  //      Z  (xxx).L        111 001    *   *   *   *   *    12   12   16   24   32      12     12
  //      P  (d16,PC)       111 010    *   *       *         8    8   12   20   28       8     10
  //      P  (d8,PC,Rn.wl)  111 011    *   *       *        10   10   14   22   30      12     14
  //      I  #<data>        111 100    *                     4    4    8   16   24
  //    MoveToMemByte/MoveToMemWord/MoveToMemLongはデスティネーションが-(Aq)のとき2減らす
  //    AddToRegLong/AddaLong/AndToRegLong/OrToRegLong/SubToRegLong/SubaLongはソースがDr/Ar/#<data>のとき2増やす
  //
  //  フルフォーマットの拡張ワードの処理の冗長表現
  //      t = r[ea - (0b110_000 - 8)];  //ベースレジスタ
  //      w = rwze ((pc += 2) - 2);  //pcwz。拡張ワード
  //      x = r[w >> 12];  //インデックスレジスタ
  //      if ((w & 0x0800) == 0) {  //ワードインデックス
  //        x = (short) x;
  //      }
  //      x <<= w >> 9 & 3;  //スケールファクタ。ワードインデックスのときは符号拡張してから掛ける
  //      if ((w & 0x0100) == 0) {  //短縮フォーマット
  //        t += (byte) w + x;  //8ビットディスプレースメント
  //      } else {  //フルフォーマット
  //        if ((w & 0x0080) != 0) {  //ベースサプレス
  //          t = 0;
  //        }
  //        if ((w & 0x0040) != 0) {  //インデックスサプレス
  //          x = 0;
  //        }
  //        if ((w & 0x0020) != 0) {  //ベースディスプレースメントあり
  //          if ((w & 0x0010) == 0) {  //ワードベースディスプレースメント
  //            t += rwse ((pc += 2) - 2);  //pcws
  //          } else {  //ロングベースディスプレースメント
  //            t += rlse ((pc += 4) - 4);  //pcls
  //          }
  //        }
  //        if ((w & 0x0003) == 0) {  //メモリ間接なし
  //          t += x;
  //        } else {  //メモリ間接あり
  //          if ((w & 0x0004) == 0) {  //プリインデックス
  //            t = rls (t + x);
  //          } else {  //ポストインデックス
  //            t = rls (t) + x;
  //          }
  //          if ((w & 0x0002) != 0) {  //アウタディスプレースメントあり
  //            if ((w & 0x0001) == 0) {  //ワードアウタディスプレースメント
  //              t += rwse ((pc += 2) - 2);  //pcws
  //            } else {  //ロングアウタディスプレースメント
  //              t += rlse ((pc += 4) - 4);  //pcls
  //            }
  //          }
  //        }
  //      }
  //      return t;
  //
  //  フルフォーマットの拡張ワードのサイクル数
  //    ベースディスプレースメントとメモリ間接とアウターディスプレースメントのリード回数に応じてサイクル数を加算する
  //    ベースレジスタとインデックスレジスタとスケールファクタの有無はサイクル数に影響しないものとする
  //      fedcba9876543210  bd  []  od  計
  //      .......0........               0  (d8,～)
  //      .......1..01..00               0  (～)
  //      .......1..01..01       8       8  ([～])
  //      .......1..01..10       8   4  12  ([～],od.W)
  //      .......1..01..11       8   8  16  ([～],od.L)
  //      .......1..10..00   4           4  (bd.W,～)
  //      .......1..10..01   4   8      12  ([bd.W,～])
  //      .......1..10..10   4   8   4  16  ([bd.W,～],od.W)
  //      .......1..10..11   4   8   8  20  ([bd.W,～],od.L)
  //      .......1..11..00   8           8  (bd.L,～)
  //      .......1..11..01   8   8      16  ([bd.L,～])
  //      .......1..11..10   8   8   4  20  ([bd.L,～],od.W)
  //      .......1..11..11   8   8   8  24  ([bd.L,～],od.L)
  //    1つの式で書くこともできるが冗長になるのでテーブル参照にする
  //
  //  MC68060のサイクル数
  //    ブリーフフォーマットは0、フルフォーマットのメモリ間接なしは1、フルフォーマットのメモリ間接ありは3
  //

  //拡張ワードのサイクル数
/*
  public static final int[] EFA_EXTENSION_CLK = {                  //fedcba9876543210
    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  //.......0........
    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  //.......0........
    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  //.......0........
    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  //.......0........
    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  //.......0........
    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  //.......0........
    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  //.......0........
    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  //.......0........
    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  //.......0........
    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  //.......0........
    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  //.......0........
    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  //.......0........
    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  //.......0........
    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  //.......0........
    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  //.......0........
    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  //.......0........
    0,  8, 12, 16,  0,  8, 12, 16,  0,  8, 12, 16,  0,  8, 12, 16,  //.......1..00....
    0,  8, 12, 16,  0,  8, 12, 16,  0,  8, 12, 16,  0,  8, 12, 16,  //.......1..01....
    4, 12, 16, 20,  4, 12, 16, 20,  4, 12, 16, 20,  4, 12, 16, 20,  //.......1..10....
    8, 16, 20, 24,  8, 16, 20, 24,  8, 16, 20, 24,  8, 16, 20, 24,  //.......1..11....
    0,  8, 12, 16,  0,  8, 12, 16,  0,  8, 12, 16,  0,  8, 12, 16,  //.......1..00....
    0,  8, 12, 16,  0,  8, 12, 16,  0,  8, 12, 16,  0,  8, 12, 16,  //.......1..01....
    4, 12, 16, 20,  4, 12, 16, 20,  4, 12, 16, 20,  4, 12, 16, 20,  //.......1..10....
    8, 16, 20, 24,  8, 16, 20, 24,  8, 16, 20, 24,  8, 16, 20, 24,  //.......1..11....
    0,  8, 12, 16,  0,  8, 12, 16,  0,  8, 12, 16,  0,  8, 12, 16,  //.......1..00....
    0,  8, 12, 16,  0,  8, 12, 16,  0,  8, 12, 16,  0,  8, 12, 16,  //.......1..01....
    4, 12, 16, 20,  4, 12, 16, 20,  4, 12, 16, 20,  4, 12, 16, 20,  //.......1..10....
    8, 16, 20, 24,  8, 16, 20, 24,  8, 16, 20, 24,  8, 16, 20, 24,  //.......1..11....
    0,  8, 12, 16,  0,  8, 12, 16,  0,  8, 12, 16,  0,  8, 12, 16,  //.......1..00....
    0,  8, 12, 16,  0,  8, 12, 16,  0,  8, 12, 16,  0,  8, 12, 16,  //.......1..01....
    4, 12, 16, 20,  4, 12, 16, 20,  4, 12, 16, 20,  4, 12, 16, 20,  //.......1..10....
    8, 16, 20, 24,  8, 16, 20, 24,  8, 16, 20, 24,  8, 16, 20, 24,  //.......1..11....
  };
*/
  //  perl misc/itob.pl xeij/XEiJ.java EFA_EXTENSION_CLK
  public static final byte[] EFA_EXTENSION_CLK = "\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\4\f\20\24\4\f\20\24\4\f\20\24\4\f\20\24\b\20\24\30\b\20\24\30\b\20\24\30\b\20\24\30\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\4\f\20\24\4\f\20\24\4\f\20\24\4\f\20\24\b\20\24\30\b\20\24\30\b\20\24\30\b\20\24\30\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\4\f\20\24\4\f\20\24\4\f\20\24\4\f\20\24\b\20\24\30\b\20\24\30\b\20\24\30\b\20\24\30\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\0\b\f\20\4\f\20\24\4\f\20\24\4\f\20\24\4\f\20\24\b\20\24\30\b\20\24\30\b\20\24\30\b\20\24\30".getBytes (XEiJ.ISO_8859_1);

  public static final boolean EFA_SEPARATE_AR = false;  //true=(Ar)を(A0)～(A7)に分ける



  //========================================================================================
  //$$BUS バスコントローラ

  public static final boolean BUS_SPLIT_UNALIGNED_LONG = false;  //true=4の倍数ではない偶数アドレスへのロングワードアクセスを常に分割する

  //マザーボードのアドレス空間
  public static final int BUS_MOTHER_BITS = 24;  //マザーボードのアドレス空間のビット数
  public static final int BUS_MOTHER_SIZE = BUS_MOTHER_BITS < 32 ? 1 << BUS_MOTHER_BITS : 0;  //マザーボードのアドレス空間のサイズ。1<<32は1が押し出されて0になるのではなくてシフトカウントが5bitでマスクされて1<<0=1になることに注意
  public static final int BUS_MOTHER_MASK = BUS_MOTHER_SIZE - 1;  //マザーボードのアドレスのマスク。a&BUS_MOTHER_MASK
  //合計
  public static final int BUS_ARRAY_SIZE = BUS_MOTHER_SIZE;

  //ページ
  public static final int BUS_PAGE_BITS = 12;  //ページのビット数。a>>>BUS_PAGE_BITS
  public static final int BUS_PAGE_SIZE = 1 << BUS_PAGE_BITS;  //ページのサイズ
  public static final int BUS_PAGE_COUNT = 1 << (32 - BUS_PAGE_BITS);  //ページの数

  //ss
  public static final int MPU_SS_BYTE = 0;
  public static final int MPU_SS_WORD = 1;
  public static final int MPU_SS_LONG = 2;

  //wr
  public static final int MPU_WR_WRITE = 0;
  public static final int MPU_WR_READ = 1;

  //us
  public static final int MPU_US_USER = 0;
  public static final int MPU_US_SUPERVISOR = 1;

  //メモリマップ
  public static final MemoryMappedDevice[] busUserMap = new MemoryMappedDevice[BUS_PAGE_COUNT];  //ユーザモード用のメモリマップ
  public static final MemoryMappedDevice[] busSuperMap = new MemoryMappedDevice[BUS_PAGE_COUNT];  //スーパーバイザモード用のメモリマップ
  public static MemoryMappedDevice[] busMemoryMap;  //現在のメモリマップ。srS==0?um:sm。DataBreakPoint.DBP_ON==trueのときは使わない

  //X68000のハイメモリ
  public static boolean busHimem68000;  //true=060turboのハイメモリはX68000でも有効

  //X68030/Xellent30のハイメモリ
  public static final int BUS_HIGH_MEMORY_START = 0x01000000;  //X68030/Xellent30のハイメモリの開始アドレス
  public static int busHighMemorySize;  //X68030/Xellent30のハイメモリのサイズ
  public static byte[] busHighMemoryArray;  //X68030/Xellent30のハイメモリの配列
  public static boolean busHighMemorySaveOn;  //true=X68030/Xellent30のハイメモリの内容を保存する
  public static boolean busHighMemory060turboOn;  //true=060turboのハイメモリを使う

  //060turboのハイメモリ
  public static final int BUS_LOCAL_MEMORY_START = 0x10000000;  //060turboのハイメモリの開始アドレス
  public static int busLocalMemorySize;  //060turboのハイメモリのサイズ
  public static byte[] busLocalMemoryArray;  //060turboのハイメモリの配列
  public static boolean busLocalMemorySaveOn;  //true=060turboのハイメモリの内容を保存する

  //拡張メモリ
  public static final byte[] BUS_DUMMY_MEMORY_ARRAY = new byte[0];  //X68030と060turbo以外の拡張メモリの配列
  public static int busRequestExMemoryStart;  //次回起動時の拡張メモリの開始アドレス
  public static int busRequestExMemorySize;  //次回起動時の拡張メモリの長さ
  public static byte[] busRequestExMemoryArray;  //次回起動時の拡張メモリの配列。BUS_DUMMY_MEMORY_ARRAY,busHighMemoryArray,busLocalMemoryArrayのいずれか
  public static int busExMemoryStart;  //拡張メモリの開始アドレス
  public static int busExMemorySize;  //拡張メモリの長さ
  public static byte[] busExMemoryArray;  //拡張メモリの配列。BUS_DUMMY_MEMORY_ARRAY,busHighMemoryArray,busLocalMemoryArrayのいずれか

  //FC2ピンをカットする
  public static boolean busRequestCutFC2Pin;
  public static boolean busCutFC2Pin;

  //busInit ()
  //  バスコントローラを初期化する
  public static void busInit () {
    //um = new MMD[BUS_PAGE_COUNT];
    //sm = new MMD[BUS_PAGE_COUNT];
    if (!DataBreakPoint.DBP_ON) {
      busMemoryMap = busSuperMap;
    }

    //X68030/Xellent30のハイメモリ
    int highMemorySizeMB = Settings.sgsGetInt ("highmemory");  //X68030/Xellent30のハイメモリのサイズ
    busHighMemorySize = highMemorySizeMB == 16 ? highMemorySizeMB << 20 : 0 << 20;
    if (busHighMemorySize == 0) {
        System.out.println (Multilingual.mlnJapanese ?
                            "X68030/Xellent30 のハイメモリはありません" :
                            "X68030/Xellent30 high memory does not exists");
    } else {
      System.out.printf (Multilingual.mlnJapanese ?
                         "X68030/Xellent30 のハイメモリのサイズは %dMB です\n" :
                         "X68030/Xellent30 high memory size is %dMB\n",
                         busHighMemorySize >> 20);
    }
    busHighMemoryArray = new byte[busHighMemorySize];

    busHimem68000 = Settings.sgsGetOnOff ("himem68000");

    busHighMemorySaveOn = Settings.sgsGetOnOff ("highmemorysave");  //X68030/Xellent30のハイメモリの内容を保存するか
    busHighMemory060turboOn = Settings.sgsGetOnOff ("highmemory060turbo");

    byte[] highMemoryArray = Settings.sgsGetData ("highmemorydata");  //X68030/Xellent30のハイメモリの内容(gzip+base64)
    if (busHighMemorySize != 0) {  //X68030/Xellent30のハイメモリがある
      if (highMemoryArray.length != 0) {  //復元するデータがある
        System.out.println (Multilingual.mlnJapanese ?
                            "X68030/Xellent30 のハイメモリのデータを復元します" :
                            "X68030/Xellent30 high memory data is restored");
        System.arraycopy (highMemoryArray, 0, busHighMemoryArray, 0, Math.min (highMemoryArray.length, busHighMemorySize));
      } else {
        System.out.println (Multilingual.mlnJapanese ?
                            "X68030/Xellent30 のハイメモリをゼロクリアします" :
                            "X68030/Xellent30 high memory is zero-cleared");
      }
      if (highMemoryArray.length < busHighMemorySize) {
        Arrays.fill (busHighMemoryArray, highMemoryArray.length, busHighMemorySize, (byte) 0);  //復元されなかった部分をゼロクリアする
      }
    }

    //060turboのハイメモリ
    int localMemorySizeMB = Settings.sgsGetInt ("localmemory");  //060turboのハイメモリのサイズ
    busLocalMemorySize = (localMemorySizeMB == 16 ||
                          localMemorySizeMB == 32 ||
                          localMemorySizeMB == 64 ||
                          localMemorySizeMB == 128 ||
                          localMemorySizeMB == 256 ||
                          localMemorySizeMB == 384 ||
                          localMemorySizeMB == 512 ||
                          localMemorySizeMB == 768 ?
                          localMemorySizeMB << 20 :
                          128 << 20);
    if (busLocalMemorySize == 0) {
        System.out.println (Multilingual.mlnJapanese ?
                            "060turbo のハイメモリはありません" :
                            "060turbo high memory does not exists");
    } else {
      System.out.printf (Multilingual.mlnJapanese ?
                         "060turbo のハイメモリのサイズは %dMB です\n" :
                         "060turbo high memory size is %dMB\n",
                         busLocalMemorySize >> 20);
    }
    busLocalMemoryArray = new byte[busLocalMemorySize];

    busLocalMemorySaveOn = Settings.sgsGetOnOff ("localmemorysave");  //060turboのハイメモリの内容を保存するか

    byte[] localMemoryArray = Settings.sgsGetData ("localmemorydata");  //060turboのハイメモリの内容(gzip+base64)
    if (busLocalMemorySize != 0) {  //060turboのハイメモリがある
      if (localMemoryArray.length != 0) {  //復元するデータがある
        System.out.println (Multilingual.mlnJapanese ?
                            "060turbo のハイメモリのデータを復元します" :
                            "060turbo high memory data is restored");
        System.arraycopy (localMemoryArray, 0, busLocalMemoryArray, 0, Math.min (localMemoryArray.length, busLocalMemorySize));
      } else {
        System.out.println (Multilingual.mlnJapanese ?
                            "060turbo のハイメモリをゼロクリアします" :
                            "060turbo high memory is zero-cleared");
      }
      if (localMemoryArray.length < busLocalMemorySize) {
        Arrays.fill (busLocalMemoryArray, localMemoryArray.length, busLocalMemorySize, (byte) 0);  //復元されなかった部分をゼロクリアする
      }
    }

    //現在の拡張メモリ
    busExMemoryStart = busRequestExMemoryStart = 0x10000000;
    busExMemorySize = busRequestExMemorySize = 0 << 20;
    busExMemoryArray = busRequestExMemoryArray = BUS_DUMMY_MEMORY_ARRAY;

    //FC2ピンをカットする
    busRequestCutFC2Pin = Settings.sgsGetOnOff ("cutfc2pin");  //FC2ピンをカットする(on/off)
    busCutFC2Pin = !busRequestCutFC2Pin;

    busUpdateMemoryMap ();

  }  //busInit()

  //busTini ()
  //  後始末
  public static void busTini () {
    Settings.sgsPutOnOff ("himem68000", busHimem68000);
    Settings.sgsPutInt ("highmemory", busHighMemorySize >>> 20);
    Settings.sgsPutOnOff ("highmemorysave", busHighMemorySaveOn);
    Settings.sgsPutOnOff ("highmemory060turbo", busHighMemory060turboOn);
    Settings.sgsPutData ("highmemorydata",
                         busHighMemorySaveOn ?
                         Arrays.copyOf (busHighMemoryArray, busHighMemoryArray.length) :
                         new byte[0]);
    Settings.sgsPutInt ("localmemory", busLocalMemorySize >>> 20);
    Settings.sgsPutOnOff ("localmemorysave", busLocalMemorySaveOn);
    Settings.sgsPutData ("localmemorydata",
                         busLocalMemorySaveOn ?
                         Arrays.copyOf (busLocalMemoryArray, busLocalMemoryArray.length) :
                         new byte[0]);
    Settings.sgsPutOnOff ("cutfc2pin", busRequestCutFC2Pin);
  }  //busTini

  public static void busUpdateMemoryMap () {

    if (busExMemoryStart != busRequestExMemoryStart ||
        busExMemorySize != busRequestExMemorySize ||
        busExMemoryArray != busRequestExMemoryArray ||
        busExMemoryArray.length != busExMemorySize ||
        busCutFC2Pin != busRequestCutFC2Pin) {
      //拡張メモリ
      busExMemoryStart = busRequestExMemoryStart;
      busExMemorySize = busRequestExMemorySize;
      busExMemoryArray = busRequestExMemoryArray;
      if (busExMemoryArray.length != busExMemorySize) {
        byte[] newArray = new byte[busExMemorySize];
        int copySize = Math.min (busExMemoryArray.length, busExMemorySize);
        if (copySize > 0) {
          System.arraycopy (busExMemoryArray, 0, newArray, 0, copySize);
        }
        if (busExMemoryArray == busHighMemoryArray) {
          busHighMemoryArray = newArray;
        } else if (busExMemoryArray == busLocalMemoryArray) {
          busLocalMemoryArray = newArray;
        }
        busExMemoryArray = newArray;
      }
      //FC2ピンをカットする
      busCutFC2Pin = busRequestCutFC2Pin;
    }

    //メモリマップを作る
    //  すべてのページにデバイスを割り当てること

    //  MMR  メインメモリ
    //  $00000000～$000FFFFF  1MB
    //  $00000000～$001FFFFF  2MB
    //  $00000000～$003FFFFF  4MB
    //  $00000000～$005FFFFF  6MB
    //  $00000000～$007FFFFF  8MB
    //  $00000000～$009FFFFF  10MB
    //  $00000000～$00BFFFFF  12MB
    //  $00000000  メインメモリ
    busSuper (MemoryMappedDevice.MMD_MMR, 0x00000000, 0x00002000);  //MMR メインメモリ
    busUser ( MemoryMappedDevice.MMD_MMR, 0x00002000, 0x00c00000);  //MMR メインメモリ

    //  GE0  グラフィック画面(512ドット16色ページ0)
    //  $00C00000～$00C7FFFF  512KB
    //  GE1  グラフィック画面(512ドット16色ページ1)
    //  $00C80000～$00CFFFFF  512KB
    //  GE2  グラフィック画面(512ドット16色ページ2)
    //  $00D00000～$00D7FFFF  512KB
    //  GE3  グラフィック画面(512ドット16色ページ3)
    //  $00D80000～$00DFFFFF  512KB
    //  GF0  グラフィック画面(512ドット256色ページ0)
    //  $00C00000～$00C7FFFF  512KB
    //  GF1  グラフィック画面(512ドット256色ページ1)
    //  $00C80000～$00CFFFFF  512KB
    //  GM2  グラフィック画面(メモリモード2)
    //  $00C00000～$00DFFFFF  2MB
    //  GG0  グラフィック画面(512ドット65536色)
    //  $00C00000～$00C7FFFF  512KB
    //  GH0  グラフィック画面(1024ドット16色)
    //  $00C00000～$00DFFFFF  2MB
    //  GI0  グラフィック画面(1024ドット256色)
    //  $00C00000～$00DFFFFF  2MB
    //  GJ0  グラフィック画面(1024ドット65536色)
    //  $00C00000～$00DFFFFF  2MB
    //  $00C00000  グラフィックVRAM
    busSuper (MemoryMappedDevice.MMD_GE0, 0x00c00000, 0x00c80000);  //GE0 グラフィック画面(512ドット16色ページ0)
    busSuper (MemoryMappedDevice.MMD_GE1, 0x00c80000, 0x00d00000);  //GE1 グラフィック画面(512ドット16色ページ1)
    busSuper (MemoryMappedDevice.MMD_GE2, 0x00d00000, 0x00d80000);  //GE2 グラフィック画面(512ドット16色ページ2)
    busSuper (MemoryMappedDevice.MMD_GE3, 0x00d80000, 0x00e00000);  //GE3 グラフィック画面(512ドット16色ページ3)

    //  TXT  テキスト画面
    //  $00E00000～$00E7FFFF  512KB
    //  $00E00000  テキストVRAM
    busSuper (MemoryMappedDevice.MMD_TXT, 0x00e00000, 0x00e80000);  //TXT テキスト画面

    //  CRT  CRTコントローラ
    //  $00E80000～$00E81FFF  8KB
    //  $00E80000  CRTコントローラ
    busSuper (MemoryMappedDevice.MMD_CRT, 0x00e80000, 0x00e82000);  //CRT CRTコントローラ

    //  VCN  ビデオコントローラ
    //  $00E82000～$00E83FFF  8KB
    //  $00E82000  パレットレジスタ
    //  $00E82400  ビデオコントローラ
    busSuper (MemoryMappedDevice.MMD_VCN, 0x00e82000, 0x00e84000);  //VCN ビデオコントローラ

    //  DMA  DMAコントローラ
    //  $00E84000～$00E85FFF  8KB
    //  $00E84000  DMAコントローラ
    busSuper (MemoryMappedDevice.MMD_DMA, 0x00e84000, 0x00e86000);  //DMA DMAコントローラ

    //  SVS  スーパーバイザ領域設定
    //  $00E86000～$00E87FFF  8KB
    //  $00E86000  スーパーバイザ領域設定
    busSuper (MemoryMappedDevice.MMD_SVS, 0x00e86000, 0x00e88000);  //SVS スーパーバイザ領域設定

    //  MFP  マルチファンクションペリフェラル
    //  $00E88000～$00E89FFF  8KB
    //  $00E88000  MFP
    //  RTC  リアルタイムクロック
    //  $00E8A000～$00E8BFFF  8KB
    //  $00E8A000  RTC
    //  OPM  FM音源
    //  $00E90000～$00E91FFF  8KB
    //  $00E90000  FM音源
    //  PCM  ADPCM音源
    //  $00E92000～$00E93FFF  8KB
    //  $00E92000  ADPCM音源
    //  FDC  FDコントローラ
    //  $00E94000～$00E95FFF  8KB
    //  $00E94000  FDC
    //  HDC  SASI HDコントローラ
    //  $00E96000～$00E97FFF  8KB
    //  $00E96000  HDC SASI HDコントローラ
    //  $00E96020  SPC 内蔵SCSIプロトコルコントローラ
    //  SCC  シリアルコミュニケーションコントローラ
    //  $00E98000～$00E99FFF  8KB
    //  $00E98000  SCC
    //  PPI  プログラマブルペリフェラルインタフェイス
    //  $00E9A000～$00E9BFFF  8KB
    //  $00E9A000  PPI
    //  IOI  I/O割り込み
    //  $00E9C000～$00E9DFFF  8KB
    //  $00E9C000  I/O割り込み
    if (currentModel.isX68030 ()) {  //X68030
      busSetPortMap_30 ();
    } else if (currentIsSecond) {  //Xellent30の030モード
      busSetPortMap_03 ();
    } else {  //X68000
      busSetPortMap_00 ();
    }

    //  PRT  プリンタポート
    //  $00E8C000～$00E8DFFF  8KB
    //  $00E8C000  プリンタポート
    busSuper (MemoryMappedDevice.MMD_PRN, 0x00e8c000, 0x00e8e000);  //PRN プリンタポート

    //  SYP  システムポート
    //  $00E8E000～$00E8FFFF  8KB
    //  $00E8E000  システムポート
    busSuper (MemoryMappedDevice.MMD_SYS, 0x00e8e000, 0x00e90000);  //SYS システムポート

    //  XB1  拡張ボード領域1
    //  $00E9E000～$00E9FFFF  8KB
    //  $00E9E000  数値演算プロセッサボード(CZ-6BP1/CZ-6BP1A)
    //  $00E9E200  ツクモグラフィックアクセラレータPCMボード(TS-6BGA)
    //  $00E9F000  WINDRV
    //  $00E9F000  040Excel
    //  $00E9F020  HFS ホストファイルシステムインタフェイス
    //  $00E9F040～$00E9F047  Zキーボード
    busSuper (MemoryMappedDevice.MMD_XB1, 0x00e9e000, 0x00ea0000);  //XB1 拡張ボード領域1

    //  XSC  拡張SCSI
    //  $00EA0000～$00EA1FFF  8KB
    //  $00EA0000  拡張SCSI(SCSIボードCZ-6BS1/Mach-2)
    //  $00EA1FF0  TS-6BS1
    busSuper (MemoryMappedDevice.MMD_NUL, 0x00ea0000, 0x00eae000);  //MemoryMappedDevice.MMD_EXSはSPC.spcReset()で必要なときだけ接続する

    //  XB2  拡張ボード領域2
    //  $00EA2000～$00EAFFFF  56KB
    //  $00EA8000～$00EA9FFF  PhantomX
    //  $00EAA000～$00EAAFFF  HaumeaX I/O
    //  $00EAB000～$00EABFFF  HaumeaX ROM
    //  $00EAF900  FAXボード(CZ-6BC1)
    //  $00EAFA00  MIDIボード(CZ-6BM1)
    //  $00EAFB00  パラレルボード(CZ-6BN1)
    //  $00EAFC00  RS-232Cボード(CZ-6BF1)
    //  $00EAFD00  ユニバーサルI/Oボード(CZ-6BU1)
    //  $00EAFE00  GP-IBボード(CZ-6BG1)
    //  $00EAFF00  スーパーバイザエリア設定
    //  拡張
    //  $00EAFF7F  バンクメモリのページ番号
    busSuper (MemoryMappedDevice.MMD_XB2, 0x00eae000, 0x00eb0000);  //拡張ボード領域2

    //  SPR  スプライト画面
    //  $00EB0000～$00EBFFFF  64KB
    //  $00EB0000  スプライトレジスタ
    //  $00EB0800  スプライトコントローラ
    //  $00EB8000  スプライトPCGエリア
    //  $00EBC000  スプライトテキストエリア0
    //  $00EBE000  スプライトテキストエリア1
    busSuper (MemoryMappedDevice.MMD_SPR, 0x00eb0000, 0x00ec0000);  //SPR スプライト画面

    //  XB3  拡張ボード領域3
    //  $00EC0000～$00ECFFFF  64KB
    //  $00EC0000  ユーザI/Oエリア
    //  $00EC0000  Awesome
    //  $00EC0000～$00EC3FFF  Xellent30 #0
    //  $00EC0000～$00EC7FFF  ZUSB
    //  $00EC4000～$00EC7FFF  Xellent30 #1
    //  $00EC8000～$00ECBFFF  Xellent30 #2
    //  $00ECC000～$00ECFFFF  Xellent30 #3
    //  $00ECC080～$00ECC0FF  MercuryUnit
    //  $00ECE000  Neptune
    //  $00ECF000  Venus-X/030
    busSuper (MemoryMappedDevice.MMD_XB3, 0x00ec0000, 0x00ed0000);  //拡張ボード領域3

    //  SMR  SRAM
    //  $00ED0000～$00ED3FFF  16KB
    //  $00ED0000～$00ED7FFF  32KB。改造したとき
    //  $00ED0000～$00EDFFFF  64KB。改造したとき
    //  $00ED0000  SRAM
    busSuper (MemoryMappedDevice.MMD_SMR, 0x00ed0000, 0x00ed0000 + 16384);  //SMR SRAM
    busSuper (MemoryMappedDevice.MMD_NUL, 0x00ed0000 + 16384, 0x00ed0000 + 65536);  //空き

    //  XB4  拡張ボード領域4
    //  $00EE0000～$00EFFFFF  128KB
    //  $00EE0000  GAフレームバッファウインドウ(サブ)
    //  $00EF0000  GAフレームバッファウインドウ(メイン)
    //  $00EFFF00  PSX16550
    //  拡張
    //  $00EE0000～$00EFFFFF  バンクメモリ
    busSuper (MemoryMappedDevice.MMD_XB4, 0x00ee0000, 0x00f00000);  //拡張ボード領域4

    //  CG1  CGROM1
    //  $00F00000～$00F3FFFF  256KB。CGROM作成前
    //  $00F00000  KNJ16x16フォント(1～8区,非漢字752文字)
    //  $00F05E00  KNJ16x16フォント(16～47区,第1水準漢字3008文字)
    //  $00F1D600  KNJ16x16フォント(48～84区,第2水準漢字3478文字)
    //  $00F3A000  ANK8x8フォント(256文字)
    //  $00F3A800  ANK8x16フォント(256文字)
    //  $00F3B800  ANK12x12フォント(256文字)
    //  $00F3D000  ANK12x24フォント(256文字)
    busSuper (MemoryMappedDevice.MMD_ROM, 0x00f00000, 0x00f40000);

    //  CG2  CGROM2
    //  $00F40000～$00FBFFFF  512KB。CGROM作成前
    //  $00F40000  KNJ24x24フォント(1～8区,非漢字752文字)
    //  $00F4D380  KNJ24x24フォント(16～47区,第1水準漢字3008文字)
    //  $00F82180  KNJ24x24フォント(48～84区,第2水準漢字3478文字)
    //  $00FBF400  [13]ANK6x12フォント(256文字)
    busSuper (MemoryMappedDevice.MMD_ROM, 0x00f40000, 0x00fc0000);

    //  ROM  ROM
    //  $00FC0000～$00FFFFFF  256KB
    //  $00FC0000  [11,12]内蔵SCSI BIOS,[13]内蔵SCSIハンドル
    //  $00FC0200  [13]ROM Human
    //  $00FCE000  [13]ROM Float
    //  $00FD3800  [13]ROMデバッガ
    //  $00FE0000  [10,11,12]ROMデバッガ
    //  $00FE5000  [10,11,12]ROM Human
    //  $00FF0000  IPLROM
    //  $00FFD018  [10]ANK6x12フォント(254文字)
    //  $00FFD344  [11]ANK6x12フォント(254文字)
    //  $00FFD45E  [12]ANK6x12フォント(254文字)
    //  $00FFDC00  [10]ROMディスク
    busSuper (MemoryMappedDevice.MMD_ROM, 0x00fc0000, 0x01000000);  //ROM ROM

  }  //busUpdateMemoryMap()

  //busSetPortMap_00 ()
  //  8bit I/Oポートのメモリマップの設定(X68000)
  //  8bit I/Oポートの偶数アドレスのバイトアクセスがバスエラーになる
  public static void busSetPortMap_00 () {
    busSuper (MemoryMappedDevice.MMD_MFP_00, 0x00e88000, 0x00e8a000);  //MFP MFP
    busSuper (MemoryMappedDevice.MMD_RTC_FIRST_00, 0x00e8a000, 0x00e8c000);  //RTC RTC
    busSuper (MemoryMappedDevice.MMD_OPM_00, 0x00e90000, 0x00e92000);  //OPM FM音源
    busSuper (MemoryMappedDevice.MMD_PCM_00, 0x00e92000, 0x00e94000);  //PCM ADPCM音源
    busSuper (MemoryMappedDevice.MMD_FDC_00, 0x00e94000, 0x00e96000);  //FDC FDコントローラ
    busSuper (MemoryMappedDevice.MMD_HDC_00, 0x00e96000, 0x00e98000);  //HDC SASI HDコントローラ
    busSuper (MemoryMappedDevice.MMD_SCC_00, 0x00e98000, 0x00e9a000);  //SCC SCC
    busSuper (MemoryMappedDevice.MMD_PPI_00, 0x00e9a000, 0x00e9c000);  //PPI PPI
    busSuper (MemoryMappedDevice.MMD_IOI_00, 0x00e9c000, 0x00e9e000);  //IOI I/O割り込み
  }  //busSetPortMap_00

  //busSetPortMap_03 ()
  //  8bit I/Oポートのメモリマップの設定(Xellent30の030モード)
  //  8bit I/Oポートの偶数アドレスのバイトアクセスがバスエラーにならない
  public static void busSetPortMap_03 () {
    busSuper (MemoryMappedDevice.MMD_MFP_03, 0x00e88000, 0x00e8a000);  //MFP MFP
    busSuper (MemoryMappedDevice.MMD_RTC_FIRST_03, 0x00e8a000, 0x00e8c000);  //RTC RTC
    busSuper (MemoryMappedDevice.MMD_OPM_03, 0x00e90000, 0x00e92000);  //OPM FM音源
    busSuper (MemoryMappedDevice.MMD_PCM_03, 0x00e92000, 0x00e94000);  //PCM ADPCM音源
    busSuper (MemoryMappedDevice.MMD_FDC_03, 0x00e94000, 0x00e96000);  //FDC FDコントローラ
    busSuper (MemoryMappedDevice.MMD_HDC_03, 0x00e96000, 0x00e98000);  //HDC SASI HDコントローラ
    busSuper (MemoryMappedDevice.MMD_SCC_03, 0x00e98000, 0x00e9a000);  //SCC SCC
    busSuper (MemoryMappedDevice.MMD_PPI_03, 0x00e9a000, 0x00e9c000);  //PPI PPI
    busSuper (MemoryMappedDevice.MMD_IOI_03, 0x00e9c000, 0x00e9e000);  //IOI I/O割り込み
  }  //busSetPortMap_03

  //busSetPortMap_30 ()
  //  8bit I/Oポートのメモリマップの設定(X68030)
  //  8bit I/Oポートの偶数アドレスのバイトアクセスがバスエラーになる
  public static void busSetPortMap_30 () {
    busSuper (MemoryMappedDevice.MMD_MFP_30, 0x00e88000, 0x00e8a000);  //MFP MFP
    busSuper (MemoryMappedDevice.MMD_RTC_FIRST_30, 0x00e8a000, 0x00e8c000);  //RTC RTC
    busSuper (MemoryMappedDevice.MMD_OPM_30, 0x00e90000, 0x00e92000);  //OPM FM音源
    busSuper (MemoryMappedDevice.MMD_PCM_30, 0x00e92000, 0x00e94000);  //PCM ADPCM音源
    busSuper (MemoryMappedDevice.MMD_FDC_30, 0x00e94000, 0x00e96000);  //FDC FDコントローラ
    busSuper (MemoryMappedDevice.MMD_HDC_30, 0x00e96000, 0x00e98000);  //HDC SASI HDコントローラ
    busSuper (MemoryMappedDevice.MMD_SCC_30, 0x00e98000, 0x00e9a000);  //SCC SCC
    busSuper (MemoryMappedDevice.MMD_PPI_30, 0x00e9a000, 0x00e9c000);  //PPI PPI
    busSuper (MemoryMappedDevice.MMD_IOI_30, 0x00e9c000, 0x00e9e000);  //IOI I/O割り込み
  }  //busSetPortMap_30

  public static void busReset () {
    if (regSRS != 0) {  //スーパーバイザモード
      if (DataBreakPoint.DBP_ON) {
        DataBreakPoint.dbpMemoryMap = DataBreakPoint.dbpSuperMap;
      } else {
        busMemoryMap = busSuperMap;
      }
    } else {  //ユーザモード
      if (DataBreakPoint.DBP_ON) {
        DataBreakPoint.dbpMemoryMap = DataBreakPoint.dbpUserMap;
      } else {
        busMemoryMap = busUserMap;
      }
    }
  }  //busReset()

  //busUser (mmd, motherStartAddress, motherEndAddress)
  //  ユーザ領域にデバイスを割り当てる
  //  motherStartAddress  マザーボード開始アドレス。BUS_PAGE_SIZEで割り切れること
  //  motherEndAddress    マザーボード終了アドレス。BUS_PAGE_SIZEで割り切れること
  public static void busUser (MemoryMappedDevice mmd, int motherStartAddress, int motherEndAddress) {
    if (MC68060.CAT_ON) {
      if (Model.MPU_MC68LC060 <= currentMPU) {
        mmd = (mmd == MemoryMappedDevice.MMD_MMR ? MemoryMappedDevice.MMD_MM6 :
               mmd == MemoryMappedDevice.MMD_ROM ? MemoryMappedDevice.MMD_RO6 :
               mmd);
      }
    }
    int motherStartPage = motherStartAddress >>> BUS_PAGE_BITS;  //マザーボード開始ページ
    int motherEndPage = motherEndAddress >>> BUS_PAGE_BITS;  //マザーボード終了ページ
    if (false &&
        (motherStartPage << BUS_PAGE_BITS != motherStartAddress ||
         motherEndPage << BUS_PAGE_BITS != motherEndAddress)) {  //開始アドレスまたは終了アドレスがページサイズで割り切れない
      System.out.printf ("ERROR: busUser (\"%s\", 0x%08x, 0x%08x)\n", mmd.toString (), motherStartAddress, motherEndAddress);
    }
    int exMemoryStartPage = busExMemoryStart >>> BUS_PAGE_BITS;  //拡張メモリ開始ページ
    int exMemoryEndPage = exMemoryStartPage + (busExMemorySize >>> BUS_PAGE_BITS);  //拡張メモリ終了ページ
    for (int block = 0; block < 1 << 32 - BUS_MOTHER_BITS; block++) {  //ブロック
      int blockStartPage = block << BUS_MOTHER_BITS - BUS_PAGE_BITS;  //ブロック開始ページ
      int startPage = blockStartPage + motherStartPage;  //デバイス開始ページ
      int endPage = blockStartPage + motherEndPage;  //デバイス終了ページ
      for (int page = startPage; page < endPage; page++) {  //デバイスページ
        boolean isExMemory = exMemoryStartPage <= page && page < exMemoryEndPage;
        MemoryMappedDevice superMmd = isExMemory ? MemoryMappedDevice.MMD_XMM : mmd;
        if (MC68060.CAT_ON) {
          if (Model.MPU_MC68LC060 <= currentMPU &&  //68060で
              isExMemory) {  //拡張メモリのとき
            superMmd = MemoryMappedDevice.MMD_XM6;  //拡張メモリ(68060)に変更する
          }
        }
        busUserMap[page] = busSuperMap[page] = superMmd;
        if (InstructionBreakPoint.IBP_ON) {
          if (InstructionBreakPoint.ibpUserMap[page] != MemoryMappedDevice.MMD_IBP) {  //命令ブレークポイントがない
            InstructionBreakPoint.ibpUserMap[page] = superMmd;
          }
          if (InstructionBreakPoint.ibpSuperMap[page] != MemoryMappedDevice.MMD_IBP) {  //命令ブレークポイントがない
            InstructionBreakPoint.ibpSuperMap[page] = superMmd;
          }
        }
        if (DataBreakPoint.DBP_ON) {
          if (DataBreakPoint.dbpUserMap[page] != MemoryMappedDevice.MMD_DBP) {  //データブレークポイントがない
            DataBreakPoint.dbpUserMap[page] = superMmd;
          }
          if (DataBreakPoint.dbpSuperMap[page] != MemoryMappedDevice.MMD_DBP) {  //データブレークポイントがない
            DataBreakPoint.dbpSuperMap[page] = superMmd;
          }
        }
      }
    }
  }  //busUser(MMD,int,int)

  //busSuper (mmd, motherStartAddress, motherEndAddress)
  //  スーパーバイザ領域にデバイスを割り当てる
  //  motherStartAddress  マザーボード開始アドレス。BUS_PAGE_SIZEで割り切れること
  //  motherEndAddress    マザーボード終了アドレス。BUS_PAGE_SIZEで割り切れること
  public static void busSuper (MemoryMappedDevice mmd, int motherStartAddress, int motherEndAddress) {
    if (MC68060.CAT_ON) {
      if (Model.MPU_MC68LC060 <= currentMPU) {
        mmd = (mmd == MemoryMappedDevice.MMD_MMR ? MemoryMappedDevice.MMD_MM6 :
               mmd == MemoryMappedDevice.MMD_ROM ? MemoryMappedDevice.MMD_RO6 :
               mmd);
      }
    }
    int motherStartPage = motherStartAddress >>> BUS_PAGE_BITS;  //マザーボード開始ページ
    int motherEndPage = motherEndAddress >>> BUS_PAGE_BITS;  //マザーボード終了ページ
    if (false &&
        (motherStartPage << BUS_PAGE_BITS != motherStartAddress ||
         motherEndPage << BUS_PAGE_BITS != motherEndAddress)) {  //開始アドレスまたは終了アドレスがページサイズで割り切れない
      System.out.printf ("ERROR: busSuper (\"%s\", 0x%08x, 0x%08x)\n", mmd.toString (), motherStartAddress, motherEndAddress);
    }
    int exMemoryStartPage = busExMemoryStart >>> BUS_PAGE_BITS;  //拡張メモリ開始ページ
    int exMemoryEndPage = exMemoryStartPage + (busExMemorySize >>> BUS_PAGE_BITS);  //拡張メモリ終了ページ
    for (int block = 0; block < 1 << 32 - BUS_MOTHER_BITS; block++) {  //ブロック
      int blockStartPage = block << BUS_MOTHER_BITS - BUS_PAGE_BITS;  //ブロック開始ページ
      int startPage = blockStartPage + motherStartPage;  //デバイス開始ページ
      int endPage = blockStartPage + motherEndPage;  //デバイス終了ページ
      for (int page = startPage; page < endPage; page++) {  //デバイスページ
        boolean isExMemory = exMemoryStartPage <= page && page < exMemoryEndPage;
        MemoryMappedDevice userMmd = isExMemory ? MemoryMappedDevice.MMD_XMM : busCutFC2Pin ? mmd : MemoryMappedDevice.MMD_NUL;
        MemoryMappedDevice superMmd = isExMemory ? MemoryMappedDevice.MMD_XMM : mmd;
        if (MC68060.CAT_ON) {
          if (Model.MPU_MC68LC060 <= currentMPU &&  //68060で
              isExMemory) {  //拡張メモリのとき
            userMmd = MemoryMappedDevice.MMD_XM6;  //拡張メモリ(68060)に変更する
            superMmd = MemoryMappedDevice.MMD_XM6;  //拡張メモリ(68060)に変更する
          }
        }
        busUserMap[page] = userMmd;
        busSuperMap[page] = superMmd;
        if (InstructionBreakPoint.IBP_ON) {
          if (InstructionBreakPoint.ibpUserMap[page] != MemoryMappedDevice.MMD_IBP) {  //命令ブレークポイントがない
            InstructionBreakPoint.ibpUserMap[page] = userMmd;
          }
          if (InstructionBreakPoint.ibpSuperMap[page] != MemoryMappedDevice.MMD_IBP) {  //命令ブレークポイントがない
            InstructionBreakPoint.ibpSuperMap[page] = superMmd;
          }
        }
        if (DataBreakPoint.DBP_ON) {
          if (DataBreakPoint.dbpUserMap[page] != MemoryMappedDevice.MMD_DBP) {  //データブレークポイントがない
            DataBreakPoint.dbpUserMap[page] = userMmd;
          }
          if (DataBreakPoint.dbpSuperMap[page] != MemoryMappedDevice.MMD_DBP) {  //データブレークポイントがない
            DataBreakPoint.dbpSuperMap[page] = superMmd;
          }
        }
      }
    }
  }  //busSuper(MMD,int,int)

  //d = busPbs (a)
  //  ピークバイト符号拡張
  public static byte busPbs (int a) {
    return busSuperMap[a >>> BUS_PAGE_BITS].mmdPbs (a);
  }  //busPbs(int)

  //d = busPbz (a)
  //  ピークバイトゼロ拡張
  public static int busPbz (int a) {
    return busSuperMap[a >>> BUS_PAGE_BITS].mmdPbz (a);
  }  //busPbz(int)

  //d = busPws (a)
  //  ピークワード符号拡張
  public static int busPws (int a) {
    if (TEST_BIT_0_SHIFT ? a << 31 - 0 >= 0 : (a & 1) == 0) {  //偶数
      return busSuperMap[a >>> BUS_PAGE_BITS].mmdPws (a);
    } else {  //奇数
      int a1 = a + 1;
      return busSuperMap[a >>> BUS_PAGE_BITS].mmdPbs (a) << 8 | busSuperMap[a1 >>> BUS_PAGE_BITS].mmdPbz (a1);
    }
  }  //busPws(int)

  //d = busPwse (a)
  //  ピークワード符号拡張(偶数)
  public static int busPwse (int a) {
    return busSuperMap[a >>> BUS_PAGE_BITS].mmdPws (a);
  }  //busPwse(int)

  //d = busPwz (a)
  //  ピークワードゼロ拡張
  public static int busPwz (int a) {
    if (TEST_BIT_0_SHIFT ? a << 31 - 0 >= 0 : (a & 1) == 0) {  //偶数
      return busSuperMap[a >>> BUS_PAGE_BITS].mmdPwz (a);
    } else {  //奇数
      int a1 = a + 1;
      return busSuperMap[a >>> BUS_PAGE_BITS].mmdPbz (a) << 8 | busSuperMap[a1 >>> BUS_PAGE_BITS].mmdPbz (a1);
    }
  }  //busPwz(int)

  //d = busPwze (a)
  //  ピークワードゼロ拡張(偶数)
  public static int busPwze (int a) {
    return busSuperMap[a >>> BUS_PAGE_BITS].mmdPwz (a);
  }  //busPwze(int)

  //d = busPls (a)
  //  ピークロング符号拡張
  public static int busPls (int a) {
    if (TEST_BIT_0_SHIFT && TEST_BIT_1_SHIFT ? a << 30 == 0 : (a & 3) == 0) {  //4の倍数
      return busSuperMap[a >>> BUS_PAGE_BITS].mmdPls (a);
    } else if (TEST_BIT_0_SHIFT ? a << 31 - 0 >= 0 : (a & 1) == 0) {  //4の倍数ではない偶数
      int a2 = a + 2;
      return busSuperMap[a >>> BUS_PAGE_BITS].mmdPws (a) << 16 | busSuperMap[a2 >>> BUS_PAGE_BITS].mmdPwz (a2);
    } else {  //奇数
      int a1 = a + 1;
      int a3 = a + 3;
      return busSuperMap[a >>> BUS_PAGE_BITS].mmdPbs (a) << 24 | busSuperMap[a1 >>> BUS_PAGE_BITS].mmdPwz (a1) << 8 | busSuperMap[a3 >>> BUS_PAGE_BITS].mmdPbz (a3);
    }
  }  //busPls(int)

  //d = busPlsf (a)
  //  ピークロング符号拡張(4の倍数)
  public static int busPlsf (int a) {
    return busSuperMap[a >>> BUS_PAGE_BITS].mmdPls (a);
  }  //busPlsf(int)

  //d = busPqs (a)
  //  ピーククワッド符号拡張
  public static long busPqs (int a) {
    return (long) busPls (a) << 32 | busPls (a + 4) & 0xffffffffL;
  }  //busPqs(int)


  //検索
  public static int busSearchByte (int start, int end, int c) {
    for (int a = start; a < end; a++) {
      if (busSuperMap[a >>> BUS_PAGE_BITS].mmdPbz (a) == c) {
        return a;
      }
    }
    return -1;
  }
  public static int busSearchWord (int start, int end, int c) {
    for (int a = start; a < end; a += 2) {
      if (busSuperMap[a >>> BUS_PAGE_BITS].mmdPwz (a) == c) {
        return a;
      }
    }
    return -1;
  }
  public static int busSearchByteArray (int start, int end, int[] array) {
    int l = array.length;
    end -= l;
    int c = array[0];
  a:
    for (int a = start; a <= end; a++) {
      if (busSuperMap[a >>> BUS_PAGE_BITS].mmdPbz (a) != c) {
        continue a;
      }
      for (int i = 1, b = a + 1; i < l; i++, b++) {
        if (busSuperMap[b >>> BUS_PAGE_BITS].mmdPbz (b) != array[i]) {
          continue a;
        }
      }
      return a;
    }
    return -1;
  }
  public static int busSearchWordArray (int start, int end, int[] array) {
    int l = array.length;
    end -= l;
    int c = array[0];
  a:
    for (int a = start; a <= end; a += 2) {
      if (busSuperMap[a >>> BUS_PAGE_BITS].mmdPwz (a) != c) {
          continue a;
      }
      for (int i = 1, b = a + 2; i < l; i++, b += 2) {
        if (busSuperMap[b >>> BUS_PAGE_BITS].mmdPwz (b) != array[i]) {
          continue a;
        }
      }
      return a;
    }
    return -1;
  }


  //d = busRbs (a)
  //  リードバイト符号拡張
  public static byte busRbs (int a) throws M68kException {
    if (DataBreakPoint.DBP_ON) {
      return DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdRbs (a);
    } else {
      return busMemoryMap[a >>> BUS_PAGE_BITS].mmdRbs (a);
    }
  }  //busRbs(int)

  //d = busRbz (a)
  //  リードバイトゼロ拡張
  public static int busRbz (int a) throws M68kException {
    if (DataBreakPoint.DBP_ON) {
      return DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdRbz (a);
    } else {
      return busMemoryMap[a >>> BUS_PAGE_BITS].mmdRbz (a);
    }
  }  //busRbz(int)

  //d = busRws (a)
  //  リードワード符号拡張
  public static int busRws (int a) throws M68kException {
    if (TEST_BIT_0_SHIFT ? a << 31 - 0 >= 0 : (a & 1) == 0) {  //偶数
      if (DataBreakPoint.DBP_ON) {
        return DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdRws (a);
      } else {
        return busMemoryMap[a >>> BUS_PAGE_BITS].mmdRws (a);
      }
    } else if (mpuIgnoreAddressError) {  //奇数でアドレスエラーを検出しない
      int a1 = a + 1;
      if (DataBreakPoint.DBP_ON) {
        return DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdRbs (a) << 8 | DataBreakPoint.dbpMemoryMap[a1 >>> BUS_PAGE_BITS].mmdRbz (a1);
      } else {
        return busMemoryMap[a >>> BUS_PAGE_BITS].mmdRbs (a) << 8 | busMemoryMap[a1 >>> BUS_PAGE_BITS].mmdRbz (a1);
      }
    } else {  //奇数でアドレスエラーを検出する
      M68kException.m6eNumber = M68kException.M6E_ADDRESS_ERROR;
      M68kException.m6eAddress = a;
      M68kException.m6eDirection = MPU_WR_READ;
      M68kException.m6eSize = MPU_SS_WORD;
      throw M68kException.m6eSignal;
    }
  }  //busRws(int)

  //d = busRwse (a)
  //  リードワード符号拡張(偶数限定)
  //  MOVEM命令の2ワード目以降など、アドレスが偶数であることが分かっている場合
  public static int busRwse (int a) throws M68kException {
    if (DataBreakPoint.DBP_ON) {
      return DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdRws (a);
    } else {
      return busMemoryMap[a >>> BUS_PAGE_BITS].mmdRws (a);
    }
  }  //busRwse(int)

  //d = busRwz (a)
  //  リードワードゼロ拡張
  public static int busRwz (int a) throws M68kException {
    if (TEST_BIT_0_SHIFT ? a << 31 - 0 >= 0 : (a & 1) == 0) {  //偶数
      if (DataBreakPoint.DBP_ON) {
        return DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdRwz (a);
      } else {
        return busMemoryMap[a >>> BUS_PAGE_BITS].mmdRwz (a);
      }
    } else if (mpuIgnoreAddressError) {  //奇数でアドレスエラーを検出しない
      int a1 = a + 1;
      if (DataBreakPoint.DBP_ON) {
        return DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdRbz (a) << 8 | DataBreakPoint.dbpMemoryMap[a1 >>> BUS_PAGE_BITS].mmdRbz (a1);
      } else {
        return busMemoryMap[a >>> BUS_PAGE_BITS].mmdRbz (a) << 8 | busMemoryMap[a1 >>> BUS_PAGE_BITS].mmdRbz (a1);
      }
    } else {  //奇数でアドレスエラーを検出する
      M68kException.m6eNumber = M68kException.M6E_ADDRESS_ERROR;
      M68kException.m6eAddress = a;
      M68kException.m6eDirection = MPU_WR_READ;
      M68kException.m6eSize = MPU_SS_WORD;
      throw M68kException.m6eSignal;
    }
  }  //busRwz(int)

  //d = busRwze (a)
  //  リードワードゼロ拡張(偶数限定)
  //  MOVEM命令の2ワード目以降など、アドレスが偶数であることが分かっている場合
  public static int busRwze (int a) throws M68kException {
    if (DataBreakPoint.DBP_ON) {
      return DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdRwz (a);
    } else {
      return busMemoryMap[a >>> BUS_PAGE_BITS].mmdRwz (a);
    }
  }  //busRwze(int)

  //d = busRls (a)
  //  リードロング符号拡張
  public static int busRls (int a) throws M68kException {
    if (TEST_BIT_0_SHIFT && TEST_BIT_1_SHIFT ? a << 30 == 0 : (a & 3) == 0) {  //4の倍数
      if (DataBreakPoint.DBP_ON) {
        return DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdRls (a);
      } else {
        return busMemoryMap[a >>> BUS_PAGE_BITS].mmdRls (a);
      }
    } else if (TEST_BIT_0_SHIFT ? a << 31 - 0 >= 0 : (a & 1) == 0) {  //4の倍数ではない偶数
      int a2 = a + 2;
      if (BUS_SPLIT_UNALIGNED_LONG) {  //常に分割する
        if (DataBreakPoint.DBP_ON) {
          return DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdRws (a) << 16 | DataBreakPoint.dbpMemoryMap[a2 >>> BUS_PAGE_BITS].mmdRwz (a2);
        } else {
          return busMemoryMap[a >>> BUS_PAGE_BITS].mmdRws (a) << 16 | busMemoryMap[a2 >>> BUS_PAGE_BITS].mmdRwz (a2);
        }
      } else {  //デバイスを跨がないとき分割しない
        MemoryMappedDevice mmd;
        MemoryMappedDevice mmd2;
        if (DataBreakPoint.DBP_ON) {
          mmd = DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS];
          mmd2 = DataBreakPoint.dbpMemoryMap[a2 >>> BUS_PAGE_BITS];
        } else {
          mmd = busMemoryMap[a >>> BUS_PAGE_BITS];
          mmd2 = busMemoryMap[a2 >>> BUS_PAGE_BITS];
        }
        return mmd == mmd2 ? mmd.mmdRls (a) : mmd.mmdRws (a) << 16 | mmd2.mmdRwz (a2);  //デバイスを跨がない/デバイスを跨ぐ
      }
    } else if (mpuIgnoreAddressError) {  //奇数でアドレスエラーを検出しない
      int a1 = a + 1;
      int a3 = a + 3;
      if (DataBreakPoint.DBP_ON) {
        return DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdRbs (a) << 24 | DataBreakPoint.dbpMemoryMap[a1 >>> BUS_PAGE_BITS].mmdRwz (a1) << 8 | DataBreakPoint.dbpMemoryMap[a3 >>> BUS_PAGE_BITS].mmdRbz (a3);
      } else {
        return busMemoryMap[a >>> BUS_PAGE_BITS].mmdRbs (a) << 24 | busMemoryMap[a1 >>> BUS_PAGE_BITS].mmdRwz (a1) << 8 | busMemoryMap[a3 >>> BUS_PAGE_BITS].mmdRbz (a3);
      }
    } else {  //奇数でアドレスエラーを検出する
      M68kException.m6eNumber = M68kException.M6E_ADDRESS_ERROR;
      M68kException.m6eAddress = a;
      M68kException.m6eDirection = MPU_WR_READ;
      M68kException.m6eSize = MPU_SS_LONG;
      throw M68kException.m6eSignal;
    }
  }  //busRls(int)

  //d = busRlse (a)
  //  リードロング符号拡張(偶数限定)
  //  MOVEM命令の2ワード目以降など、アドレスが偶数であることが分かっている場合
  public static int busRlse (int a) throws M68kException {
    if (TEST_BIT_0_SHIFT && TEST_BIT_1_SHIFT ? a << 30 == 0 : (a & 3) == 0) {  //4の倍数
      if (DataBreakPoint.DBP_ON) {
        return DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdRls (a);
      } else {
        return busMemoryMap[a >>> BUS_PAGE_BITS].mmdRls (a);
      }
    } else {  //4の倍数ではない偶数
      int a2 = a + 2;
      if (BUS_SPLIT_UNALIGNED_LONG) {  //常に分割する
        if (DataBreakPoint.DBP_ON) {
          return DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdRws (a) << 16 | DataBreakPoint.dbpMemoryMap[a2 >>> BUS_PAGE_BITS].mmdRwz (a2);
        } else {
          return busMemoryMap[a >>> BUS_PAGE_BITS].mmdRws (a) << 16 | busMemoryMap[a2 >>> BUS_PAGE_BITS].mmdRwz (a2);
        }
      } else {  //デバイスを跨がないとき分割しない
        MemoryMappedDevice mmd;
        MemoryMappedDevice mmd2;
        if (DataBreakPoint.DBP_ON) {
          mmd = DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS];
          mmd2 = DataBreakPoint.dbpMemoryMap[a2 >>> BUS_PAGE_BITS];
        } else {
          mmd = busMemoryMap[a >>> BUS_PAGE_BITS];
          mmd2 = busMemoryMap[a2 >>> BUS_PAGE_BITS];
        }
        return mmd == mmd2 ? mmd.mmdRls (a) : mmd.mmdRws (a) << 16 | mmd2.mmdRwz (a2);  //デバイスを跨がない/デバイスを跨ぐ
      }
    }
  }  //busRlse(int)

  //d = busRlsf (a)
  //  リードロング符号拡張(4の倍数限定)
  //  例外ベクタテーブルなど、アドレスが4の倍数であることが分かっている場合
  public static int busRlsf (int a) throws M68kException {
    if (DataBreakPoint.DBP_ON) {
      return DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdRls (a);
    } else {
      return busMemoryMap[a >>> BUS_PAGE_BITS].mmdRls (a);
    }
  }  //busRlsf(int)

  //d = busRqs (a)
  //  リードクワッド符号拡張
  public static long busRqs (int a) throws M68kException {
    return (long) busRls (a) << 32 | busRls (a + 4) & 0xffffffffL;
  }  //busRqs(int)

  //busWb (a, d)
  //  ライトバイト
  public static void busWb (int a, int d) throws M68kException {
    if (DataBreakPoint.DBP_ON) {
      DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdWb (a, d);
    } else {
      busMemoryMap[a >>> BUS_PAGE_BITS].mmdWb (a, d);
    }
  }  //busWb(int,int)

  //busWw (a, d)
  //  ライトワード
  public static void busWw (int a, int d) throws M68kException {
    if (TEST_BIT_0_SHIFT ? a << 31 - 0 >= 0 : (a & 1) == 0) {  //偶数
      if (DataBreakPoint.DBP_ON) {
        DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdWw (a, d);
      } else {
        busMemoryMap[a >>> BUS_PAGE_BITS].mmdWw (a, d);
      }
    } else if (mpuIgnoreAddressError) {  //奇数でアドレスエラーを検出しない
      int a1 = a + 1;
      if (DataBreakPoint.DBP_ON) {
        DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdWb (a, d >> 8);
        DataBreakPoint.dbpMemoryMap[a1 >>> BUS_PAGE_BITS].mmdWb (a1, d);
      } else {
        busMemoryMap[a >>> BUS_PAGE_BITS].mmdWb (a, d >> 8);
        busMemoryMap[a1 >>> BUS_PAGE_BITS].mmdWb (a1, d);
      }
    } else {  //奇数でアドレスエラーを検出する
      M68kException.m6eNumber = M68kException.M6E_ADDRESS_ERROR;
      M68kException.m6eAddress = a;
      M68kException.m6eDirection = MPU_WR_WRITE;
      M68kException.m6eSize = MPU_SS_WORD;
      throw M68kException.m6eSignal;
    }
  }  //busWw(int,int)

  //busWwe (a, d)
  //  ライトワード(偶数限定)
  //  MOVEM命令の2ワード目以降など、アドレスが偶数であることが分かっている場合
  public static void busWwe (int a, int d) throws M68kException {
    if (DataBreakPoint.DBP_ON) {
      DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdWw (a, d);
    } else {
      busMemoryMap[a >>> BUS_PAGE_BITS].mmdWw (a, d);
    }
  }  //busWwe(int,int)

  //busWl (a, d)
  //  ライトロング
  public static void busWl (int a, int d) throws M68kException {
    if (TEST_BIT_0_SHIFT && TEST_BIT_1_SHIFT ? a << 30 == 0 : (a & 3) == 0) {  //4の倍数
      if (DataBreakPoint.DBP_ON) {
        DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdWl (a, d);
      } else {
        busMemoryMap[a >>> BUS_PAGE_BITS].mmdWl (a, d);
      }
    } else if (TEST_BIT_0_SHIFT ? a << 31 - 0 >= 0 : (a & 1) == 0) {  //4の倍数ではない偶数
      int a2 = a + 2;
      if (BUS_SPLIT_UNALIGNED_LONG) {  //常に分割する
        if (DataBreakPoint.DBP_ON) {
          DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdWw (a, d >> 16);
          DataBreakPoint.dbpMemoryMap[a2 >>> BUS_PAGE_BITS].mmdWw (a2, d);
        } else {
          busMemoryMap[a >>> BUS_PAGE_BITS].mmdWw (a, d >> 16);
          busMemoryMap[a2 >>> BUS_PAGE_BITS].mmdWw (a2, d);
        }
      } else {  //デバイスを跨がないとき分割しない
        MemoryMappedDevice mmd;
        MemoryMappedDevice mmd2;
        if (DataBreakPoint.DBP_ON) {
          mmd = DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS];
          mmd2 = DataBreakPoint.dbpMemoryMap[a2 >>> BUS_PAGE_BITS];
        } else {
          mmd = busMemoryMap[a >>> BUS_PAGE_BITS];
          mmd2 = busMemoryMap[a2 >>> BUS_PAGE_BITS];
        }
        if (mmd == mmd2) {  //デバイスを跨がない
          mmd.mmdWl (a, d);
        } else {  //デバイスを跨ぐ
          mmd.mmdWw (a, d >> 16);
          mmd2.mmdWw (a2, d);
        }
      }
    } else if (mpuIgnoreAddressError) {  //奇数でアドレスエラーを検出しない
      int a1 = a + 1;
      int a3 = a + 3;
      if (DataBreakPoint.DBP_ON) {
        DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdWb (a, d >> 24);
        DataBreakPoint.dbpMemoryMap[a1 >>> BUS_PAGE_BITS].mmdWw (a1, d >> 8);
        DataBreakPoint.dbpMemoryMap[a3 >>> BUS_PAGE_BITS].mmdWb (a3, d);
      } else {
        busMemoryMap[a >>> BUS_PAGE_BITS].mmdWb (a, d >> 24);
        busMemoryMap[a1 >>> BUS_PAGE_BITS].mmdWw (a1, d >> 8);
        busMemoryMap[a3 >>> BUS_PAGE_BITS].mmdWb (a3, d);
      }
    } else {  //奇数でアドレスエラーを検出する
      M68kException.m6eNumber = M68kException.M6E_ADDRESS_ERROR;
      M68kException.m6eAddress = a;
      M68kException.m6eDirection = MPU_WR_WRITE;
      M68kException.m6eSize = MPU_SS_LONG;
      throw M68kException.m6eSignal;
    }
  }  //busWl(int,int)

  //busWlf (a, d)
  //  ライトロング(4の倍数限定)
  //  例外ベクタテーブルなど、アドレスが4の倍数であることが分かっている場合
  public static void busWlf (int a, int d) throws M68kException {
    if (DataBreakPoint.DBP_ON) {
      DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdWl (a, d);
    } else {
      busMemoryMap[a >>> BUS_PAGE_BITS].mmdWl (a, d);
    }
  }  //busWlf(int,int)

  //busWle (a, d)
  //  ライトロング(偶数限定)
  //  MOVEM命令の2ワード目以降など、アドレスが偶数であることが分かっている場合
  public static void busWle (int a, int d) throws M68kException {
    if (TEST_BIT_0_SHIFT && TEST_BIT_1_SHIFT ? a << 30 == 0 : (a & 3) == 0) {  //4の倍数
      if (DataBreakPoint.DBP_ON) {
        DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdWl (a, d);
      } else {
        busMemoryMap[a >>> BUS_PAGE_BITS].mmdWl (a, d);
      }
    } else {  //4の倍数ではない偶数
      int a2 = a + 2;
      if (BUS_SPLIT_UNALIGNED_LONG) {  //常に分割する
        if (DataBreakPoint.DBP_ON) {
          DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS].mmdWw (a, d >> 16);
          DataBreakPoint.dbpMemoryMap[a2 >>> BUS_PAGE_BITS].mmdWw (a2, d);
        } else {
          busMemoryMap[a >>> BUS_PAGE_BITS].mmdWw (a, d >> 16);
          busMemoryMap[a2 >>> BUS_PAGE_BITS].mmdWw (a2, d);
        }
      } else {  //デバイスを跨がないとき分割しない
        MemoryMappedDevice mmd;
        MemoryMappedDevice mmd2;
        if (DataBreakPoint.DBP_ON) {
          mmd = DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS];
          mmd2 = DataBreakPoint.dbpMemoryMap[a2 >>> BUS_PAGE_BITS];
        } else {
          mmd = busMemoryMap[a >>> BUS_PAGE_BITS];
          mmd2 = busMemoryMap[a2 >>> BUS_PAGE_BITS];
        }
        if (mmd == mmd2) {  //デバイスを跨がない
          mmd.mmdWl (a, d);
        } else {  //デバイスを跨ぐ
          mmd.mmdWw (a, d >> 16);
          mmd2.mmdWw (a2, d);
        }
      }
    }
  }  //busWle(int,int)

  //busWq (a, d)
  //  ライトクワッド
  public static void busWq (int a, long d) throws M68kException {
    busWl (a, (int) (d >>> 32));
    busWl (a + 4, (int) d);
  }  //busWq(int,long)

  //以下は拡張

  //busRbb (a, bb, o, l)
  //  リードバイトバッファ
  public static void busRbb (int a, byte[] bb, int o, int l) throws M68kException {
    if (false) {
      for (int i = 0; i < l; i++) {
        int ai = a + i;
        if (DataBreakPoint.DBP_ON) {
          bb[o + i] = DataBreakPoint.dbpMemoryMap[ai >>> BUS_PAGE_BITS].mmdRbs (ai);
        } else {
          bb[o + i] = busMemoryMap[ai >>> BUS_PAGE_BITS].mmdRbs (ai);
        }
      }
    } else {
      int r = (~a & BUS_PAGE_SIZE - 1) + 1;  //最初のページの残りの長さ。1～BUS_PAGE_SIZE
      while (l > 0) {
        MemoryMappedDevice mmd;
        if (DataBreakPoint.DBP_ON) {
          mmd = DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS];  //ページのデバイス
        } else {
          mmd = busMemoryMap[a >>> BUS_PAGE_BITS];  //ページのデバイス
        }
        int s = l <= r ? l : r;  //このページで転送する長さ
        l -= s;
        if (true) {
          for (s -= 16; s >= 0; s -= 16) {
            bb[o     ] = mmd.mmdRbs (a     );
            bb[o +  1] = mmd.mmdRbs (a +  1);
            bb[o +  2] = mmd.mmdRbs (a +  2);
            bb[o +  3] = mmd.mmdRbs (a +  3);
            bb[o +  4] = mmd.mmdRbs (a +  4);
            bb[o +  5] = mmd.mmdRbs (a +  5);
            bb[o +  6] = mmd.mmdRbs (a +  6);
            bb[o +  7] = mmd.mmdRbs (a +  7);
            bb[o +  8] = mmd.mmdRbs (a +  8);
            bb[o +  9] = mmd.mmdRbs (a +  9);
            bb[o + 10] = mmd.mmdRbs (a + 10);
            bb[o + 11] = mmd.mmdRbs (a + 11);
            bb[o + 12] = mmd.mmdRbs (a + 12);
            bb[o + 13] = mmd.mmdRbs (a + 13);
            bb[o + 14] = mmd.mmdRbs (a + 14);
            bb[o + 15] = mmd.mmdRbs (a + 15);
            a += 16;
            o += 16;
          }
          s += 16;
        }
        for (int i = 0; i < s; i++) {
          bb[o + i] = mmd.mmdRbs (a + i);
        }
        a += s;
        o += s;
        r = BUS_PAGE_SIZE;
      }
    }
  }  //busRbb(int,byte[],int,int)

  //busWbb (a, bb, o, l)
  //  ライトバイトバッファ
  public static void busWbb (int a, byte[] bb, int o, int l) throws M68kException {
    if (false) {
      for (int i = 0; i < l; i++) {
        int ai = a + i;
        if (DataBreakPoint.DBP_ON) {
          DataBreakPoint.dbpMemoryMap[ai >>> BUS_PAGE_BITS].mmdWb (ai, bb[o + i]);
        } else {
          busMemoryMap[ai >>> BUS_PAGE_BITS].mmdWb (ai, bb[o + i]);
        }
      }
    } else {
      int r = (~a & BUS_PAGE_SIZE - 1) + 1;  //最初のページの残りの長さ。1～BUS_PAGE_SIZE
      while (l > 0) {
        MemoryMappedDevice mmd;
        if (DataBreakPoint.DBP_ON) {
          mmd = DataBreakPoint.dbpMemoryMap[a >>> BUS_PAGE_BITS];  //ページのデバイス
        } else {
          mmd = busMemoryMap[a >>> BUS_PAGE_BITS];  //ページのデバイス
        }
        int s = l <= r ? l : r;  //このページで転送する長さ
        l -= s;
        if (true) {
          for (s -= 16; s >= 0; s -= 16) {
            mmd.mmdWb (a     , bb[o     ]);
            mmd.mmdWb (a +  1, bb[o +  1]);
            mmd.mmdWb (a +  2, bb[o +  2]);
            mmd.mmdWb (a +  3, bb[o +  3]);
            mmd.mmdWb (a +  4, bb[o +  4]);
            mmd.mmdWb (a +  5, bb[o +  5]);
            mmd.mmdWb (a +  6, bb[o +  6]);
            mmd.mmdWb (a +  7, bb[o +  7]);
            mmd.mmdWb (a +  8, bb[o +  8]);
            mmd.mmdWb (a +  9, bb[o +  9]);
            mmd.mmdWb (a + 10, bb[o + 10]);
            mmd.mmdWb (a + 11, bb[o + 11]);
            mmd.mmdWb (a + 12, bb[o + 12]);
            mmd.mmdWb (a + 13, bb[o + 13]);
            mmd.mmdWb (a + 14, bb[o + 14]);
            mmd.mmdWb (a + 15, bb[o + 15]);
            a += 16;
            o += 16;
          }
          s += 16;
        }
        for (int i = 0; i < s; i++) {
          mmd.mmdWb (a + i, bb[o + i]);
        }
        a += s;
        o += s;
        r = BUS_PAGE_SIZE;
      }
    }
  }  //busWbb(int,byte[],int,int)

  //busVb (a, d)
  //  ライトバイト(エラーなし)
  public static void busVb (int a, int d) {
    try {
      if (DataBreakPoint.DBP_ON) {
        (regSRS != 0 ? busSuperMap : busUserMap)[a >>> BUS_PAGE_BITS].mmdWb (a, d);
      } else {
        busMemoryMap[a >>> BUS_PAGE_BITS].mmdWb (a, d);
      }
    } catch (M68kException e) {
    }
  }  //busVb(int,int)

  //busVw (a, d)
  //  ライトワード(エラーなし)
  public static void busVw (int a, int d) {
    try {
      if (TEST_BIT_0_SHIFT ? a << 31 - 0 >= 0 : (a & 1) == 0) {  //偶数
        if (DataBreakPoint.DBP_ON) {
          (regSRS != 0 ? busSuperMap : busUserMap)[a >>> BUS_PAGE_BITS].mmdWw (a, d);
        } else {
          busMemoryMap[a >>> BUS_PAGE_BITS].mmdWw (a, d);
        }
      }
    } catch (M68kException e) {
    }
  }  //busVw(int,int)

  //busVl (a, d)
  //  ライトロング(エラーなし)
  public static void busVl (int a, int d) {
    try {
      if (TEST_BIT_0_SHIFT && TEST_BIT_1_SHIFT ? a << 30 == 0 : (a & 3) == 0) {  //4の倍数
        if (DataBreakPoint.DBP_ON) {
          (regSRS != 0 ? busSuperMap : busUserMap)[a >>> BUS_PAGE_BITS].mmdWl (a, d);
        } else {
          busMemoryMap[a >>> BUS_PAGE_BITS].mmdWl (a, d);
        }
      } else if (TEST_BIT_0_SHIFT ? a << 31 - 0 >= 0 : (a & 1) == 0) {  //4の倍数ではない偶数
        int a2 = a + 2;
        MemoryMappedDevice mmd;
        MemoryMappedDevice mmd2;
        if (DataBreakPoint.DBP_ON) {
          mmd = (regSRS != 0 ? busSuperMap : busUserMap)[a >>> BUS_PAGE_BITS];
          mmd2 = (regSRS != 0 ? busSuperMap : busUserMap)[a2 >>> BUS_PAGE_BITS];
        } else {
          mmd = busMemoryMap[a >>> BUS_PAGE_BITS];
          mmd2 = busMemoryMap[a2 >>> BUS_PAGE_BITS];
        }
        if (mmd == mmd2) {  //4の倍数ではない偶数でデバイスを跨がない
          mmd.mmdWl (a, d);
        } else {  //4の倍数ではない偶数でデバイスを跨ぐ
          mmd.mmdWw (a, d >> 16);
          mmd2.mmdWw (a2, d);
        }
      }
    } catch (M68kException e) {
    }
  }  //busVl(int,int)



  //========================================================================================
  //$$SVS スーパーバイザ領域設定
  public static final int SVS_AREASET = 0x00e86001;  //0x00 0x00000000～0x00002000
  //                                                      0x01 0x00000000～0x00004000
  //                                                      0x02 0x00000000～0x00008000
  //                                                      0x04 0x00000000～0x00010000
  //                                                      0x08 0x00000000～0x00020000
  //                                                      0x10 0x00000000～0x00040000
  //                                                      0x20 0x00000000～0x00080000
  //                                                      0x40 0x00000000～0x00100000
  //                                                      0x80 0x00000000～0x00200000

  public static void svsInit () {
  }



  //========================================================================================
  //$$SYS システムポート
  //
  //     アドレス   bit  RW  名前   X68030
  //    0x00e8e001  0-3  RW           13    コントラスト(0=最も暗い,15=最も明るい)
  //    0x00e8e003   3   R             1    TV ON/OFFステータス(0=ON,1=OFF)
  //                      W                 TVリモコン信号
  //                 2   R   FIELD     0
  //                 1   RW  3D-L      0    (3Dスコープ)シャッター左(0=CLOSE,1=OPEN)
  //                 0   RW  3D-R      0    (3Dスコープ)シャッター右(0=CLOSE,1=OPEN)
  //    0x00e8e005  4-0   W                 (カラーイメージユニット(デジタイズテロッパ))画像入力コントロール
  //                                          bit4  IMAGE IN bit17
  //                                          bit3  IMAGE IN bit18
  //                                          bit2  IMAGE IN bit19
  //                                          bit1  IMAGE IN bit20
  //                                          bit0  IMAGE IN bit21
  //    0x00e8e007   3   R             1    キージャックステータス(0=抜かれている,1=差し込まれている)
  //                      W                 キーレディ(0=キーデータ送信禁止,1=キーデータ送信許可)
  //                 2    W            1    1=NMIリセット
  //                 1   RW            0    HRL。詳細はCRTCを参照
  //                 0   RW            0    (現在は使用されていない)解像度LED(0=消灯,1=点灯)
  //    0x00e8e009  7-4   W                 (X68030のみ)ROMアクセスウェイト
  //                3-0   W                 (X68030のみ)RAMアクセスウェイト(0=25MHz,4=16MHz相当,10=10MHz相当)
  //    0x00e8e00b  7-4  R            13    機種(13=MC68030,15=MC68000)
  //                3-0  R            12    動作周波数(12=25MHz,14=16MHz,15=10MHz)
  //    0x00e8e00d  7-0   W                 SRAM WRITE ENABLE(49=SRAM書き込み可)
  //    0x00e8e00f  3-0   W                 フロント電源スイッチがOFFになっているとき0→15→15で電源OFF
  //
  //    未定義のbitはリードすると1、ライトしてもバスエラーは発生しない
  //    アドレスのbit12-4はデコードされない。0x00e8e000～0x00e8ffffの範囲に16バイトのポートが512回繰り返し現れる
  //

  public static boolean sysNMIFlag;  //true=INTERRUPTスイッチが押された

  //sysInit ()
  //  初期化
  public static void sysInit () {
    sysNMIFlag = false;
  }  //sysInit()

  //割り込み受付
  //  コアが割り込み要求を受け付けたときに呼び出す
  //  割り込みベクタ番号を返す
  //  割り込み要求を取り下げる場合は0を返す
  //  オートベクタを使用するデバイスはオートベクタの番号を返すこと
  public static int sysAcknowledge () {
    return M68kException.M6E_LEVEL_7_INTERRUPT_AUTOVECTOR;
  }  //sysAcknowledge()

  //割り込み終了
  //  コアが割り込み処理を終了したときに呼び出す
  //  まだ処理されていない割り込みが残っていたら再度割り込み要求を出す
  public static void sysDone () {
    if (sysNMIFlag) {  //NMIリセットされていない
      mpuIRR |= MPU_SYS_INTERRUPT_MASK;
    }
  }  //sysDone()

  //sysInterrupt ()
  //  INTERRUPTスイッチが押された
  public static void sysInterrupt () {
    sysNMIFlag = true;
    mpuIRR |= MPU_SYS_INTERRUPT_MASK;
  }  //sysInterrupt()

  //sysResetNMI ()
  //  NMIリセット
  public static void sysResetNMI () {
    sysNMIFlag = false;
  }  //sysResetNMI()



  //========================================================================================
  //$$EB2 拡張ボードレベル2割り込み

  public static final int EB2_SPC_REQUEST = 0x4000;  //拡張SCSI
  public static final int EB2_SPC_VECTOR = 0xf6;  //拡張SCSI

  //割り込み要求
  //  0b00010000_00000000  SPC割り込み要求あり
  public static int eb2Request;  //割り込み要求。デバイスは操作しないこと

  //eb2Reset ()
  //  拡張ボードレベル2割り込みをリセットする
  public static void eb2Reset () {
    eb2Request = 0;
  }  //eb2Reset()

  //eb2Interrupt (mask)
  //  割り込み要求
  //  デバイスが割り込みを要求するときに呼び出す
  //  mask  EB2_SPC_VECTOR  拡張SCSI割り込みを要求する
  public static void eb2Interrupt (int mask) {
    eb2Request |= mask;
    mpuIRR |= MPU_EB2_INTERRUPT_MASK;
  }  //eb2Interrupt(int)

  //vector = eb2Acknowledge ()
  //  割り込み受付
  //  コアが割り込み要求を受け付けたときに呼び出す
  //  割り込みベクタ番号を返す
  //  割り込み要求を取り下げる場合は0を返す
  //  オートベクタを使用するデバイスはオートベクタの番号を返すこと
  public static int eb2Acknowledge () {
    if ((eb2Request & EB2_SPC_REQUEST) != 0) {
      eb2Request &= ~EB2_SPC_REQUEST;
      return EB2_SPC_VECTOR;
    }
    return 0;
  }  //eb2Acknowledge()

  //eb2Done ()
  //  割り込み終了
  //  コアが割り込み処理を終了したときに呼び出す
  //  まだ処理されていない割り込みが残っていたら再度割り込み要求を出す
  public static void eb2Done () {
    if (eb2Request != 0) {
      mpuIRR |= MPU_EB2_INTERRUPT_MASK;
    }
  }  //eb2Done()



  //========================================================================================
  //$$XB2 拡張ボード領域2
  //  0x00eaf900  FAXボード(CZ-6BC1)
  //  0x00eafa00  MIDIボード(CZ-6BM1)
  //  0x00eafb00  パラレルボード(CZ-6BN1)
  //  0x00eafc00  RS-232Cボード(CZ-6BF1)
  //  0x00eafd00  ユニバーサルI/Oボード(CZ-6BU1)
  //  0x00eafe00  GP-IBボード(CZ-6BG1)
  //  0x00eaff00  スーパーバイザエリア設定
  //
  //  スーパーバイザエリア設定のみ対応

  //スーパーバイザエリア設定ポート
  //  0x00eaff81  bit0  0x00200000～0x0023ffff
  //              bit1  0x00240000～0x0027ffff
  //              bit2  0x00280000～0x002bffff
  //              bit3  0x002c0000～0x002fffff
  //              bit4  0x00300000～0x0033ffff
  //              bit5  0x00340000～0x0037ffff
  //              bit6  0x00380000～0x003bffff
  //              bit7  0x003c0000～0x003fffff
  //  0x00eaff83  bit0  0x00400000～0x0043ffff
  //              bit1  0x00440000～0x0047ffff
  //              bit2  0x00480000～0x004bffff
  //              bit3  0x004c0000～0x004fffff
  //              bit4  0x00500000～0x0053ffff
  //              bit5  0x00540000～0x0057ffff
  //              bit6  0x00580000～0x005bffff
  //              bit7  0x005c0000～0x005fffff
  //  0x00eaff85  bit0  0x00600000～0x0063ffff
  //              bit1  0x00640000～0x0067ffff
  //              bit2  0x00680000～0x006bffff
  //              bit3  0x006c0000～0x006fffff
  //              bit4  0x00700000～0x0073ffff
  //              bit5  0x00740000～0x0077ffff
  //              bit6  0x00780000～0x007bffff
  //              bit7  0x007c0000～0x007fffff
  //  0x00eaff87  bit0  0x00800000～0x0083ffff
  //              bit1  0x00840000～0x0087ffff
  //              bit2  0x00880000～0x008bffff
  //              bit3  0x008c0000～0x008fffff
  //              bit4  0x00900000～0x0093ffff
  //              bit5  0x00940000～0x0097ffff
  //              bit6  0x00980000～0x009bffff
  //              bit7  0x009c0000～0x009fffff
  //  0x00eaff89  bit0  0x00a00000～0x00a3ffff
  //              bit1  0x00a40000～0x00a7ffff
  //              bit2  0x00a80000～0x00abffff
  //              bit3  0x00ac0000～0x00afffff
  //              bit4  0x00b00000～0x00b3ffff
  //              bit5  0x00b40000～0x00b7ffff
  //              bit6  0x00b80000～0x00bbffff
  //              bit7  0x00bc0000～0x00bfffff



  //========================================================================================
  //$$BNK バンクメモリ
  //  $00EAFF7F  バンクメモリのページ番号。0～255
  //  $00EE0000～$00EFFFFF  バンクメモリ

  public static final int BNK_SIZE = 1024 * 1024 * 32;  //サイズ
  public static byte[] bnkMemory;  //メモリの配列
  public static int bnkPageStart;  //ページの先頭のインデックス。ページ番号<<17
  public static boolean bnkOn;  //true=バンクメモリが有効

  public static void bnkInit () {
    bnkMemory = new byte[BNK_SIZE];
    byte[] array = Settings.sgsGetData ("bankdata");
    if (array.length != 0) {
      System.arraycopy (array, 0, bnkMemory, 0, Math.min (array.length, BNK_SIZE));
    }
    bnkPageStart = 0;
    //bnkOn = true;
    bnkOn = false;
  }

  public static void bnkTini () {
    Settings.sgsPutData ("bankdata",
                         Arrays.copyOf (bnkMemory, BNK_SIZE));
  }



  //========================================================================================
  //$$FPU FPU

  //FPU/FPCP
  public static ExpressionEvaluator fpuMotherboardCoprocessor;  //マザーボードコプロセッサ
  public static ExpressionEvaluator fpuOnChipFPU;  //on-chip FPU
  public static ExpressionEvaluator fpuBox;  //浮動小数点命令を実行するFPU/FPCP

  //数値演算プロセッサボード
  public static EFPBox fpuCoproboard1;  //数値演算プロセッサボード1
  public static EFPBox fpuCoproboard2;  //数値演算プロセッサボード2

  //浮動小数点レジスタ
  public static EFPBox.EFP[] fpuFPn;

  //FPCR control register
  //  exception enable byte
  public static final int FPU_FPCR_BSUN   = 0b00000000_00000000_10000000_00000000;  //branch/set on unordered
  public static final int FPU_FPCR_SNAN   = 0b00000000_00000000_01000000_00000000;  //signaling not a number
  public static final int FPU_FPCR_OPERR  = 0b00000000_00000000_00100000_00000000;  //operand error
  public static final int FPU_FPCR_OVFL   = 0b00000000_00000000_00010000_00000000;  //overflow
  public static final int FPU_FPCR_UNFL   = 0b00000000_00000000_00001000_00000000;  //underflow
  public static final int FPU_FPCR_DZ     = 0b00000000_00000000_00000100_00000000;  //divide by zero
  public static final int FPU_FPCR_INEX2  = 0b00000000_00000000_00000010_00000000;  //inexact operation
  public static final int FPU_FPCR_INEX1  = 0b00000000_00000000_00000001_00000000;  //inexact decimal input
  //  mode control byte
  //    rounding precision
  public static final int FPU_FPCR_PE     = 0b00000000_00000000_00000000_00000000;  //extended
  public static final int FPU_FPCR_PS     = 0b00000000_00000000_00000000_01000000;  //single
  public static final int FPU_FPCR_PD     = 0b00000000_00000000_00000000_10000000;  //double
  //    rounding mode
  public static final int FPU_FPCR_RN     = 0b00000000_00000000_00000000_00000000;  //to nearest
  public static final int FPU_FPCR_RZ     = 0b00000000_00000000_00000000_00010000;  //toward zero
  public static final int FPU_FPCR_RM     = 0b00000000_00000000_00000000_00100000;  //toward minus infinity
  public static final int FPU_FPCR_RP     = 0b00000000_00000000_00000000_00110000;  //toward plus infinity

  //FPSR status register
  //  condition code byte
  public static final int FPU_FPSR_N         = 0b00001000_00000000_00000000_00000000;  //negative
  public static final int FPU_FPSR_Z         = 0b00000100_00000000_00000000_00000000;  //zero
  public static final int FPU_FPSR_I         = 0b00000010_00000000_00000000_00000000;  //infinity
  public static final int FPU_FPSR_NAN       = 0b00000001_00000000_00000000_00000000;  //not a number or unordered
  //  quotient byte
  public static final int FPU_FPSR_S         = 0b00000000_10000000_00000000_00000000;  //sign of quotient
  public static final int FPU_FPSR_QUOTIENT  = 0b00000000_01111111_00000000_00000000;  //quotient
  //  exception status byte
  public static final int FPU_FPSR_EXC_BSUN  = 0b00000000_00000000_10000000_00000000;  //branch/set on unordered
  public static final int FPU_FPSR_EXC_SNAN  = 0b00000000_00000000_01000000_00000000;  //signaling not a number
  public static final int FPU_FPSR_EXC_OPERR = 0b00000000_00000000_00100000_00000000;  //operand error
  public static final int FPU_FPSR_EXC_OVFL  = 0b00000000_00000000_00010000_00000000;  //overflow
  public static final int FPU_FPSR_EXC_UNFL  = 0b00000000_00000000_00001000_00000000;  //underflow
  public static final int FPU_FPSR_EXC_DZ    = 0b00000000_00000000_00000100_00000000;  //divide by zero
  public static final int FPU_FPSR_EXC_INEX2 = 0b00000000_00000000_00000010_00000000;  //inexact operation
  public static final int FPU_FPSR_EXC_INEX1 = 0b00000000_00000000_00000001_00000000;  //inexact decimal input
  //  accrued exception byte
  public static final int FPU_FPSR_AEXC_IOP  = 0b00000000_00000000_00000000_10000000;  //invalid operation
  public static final int FPU_FPSR_AEXC_OVFL = 0b00000000_00000000_00000000_01000000;  //overflow
  public static final int FPU_FPSR_AEXC_UNFL = 0b00000000_00000000_00000000_00100000;  //underflow
  public static final int FPU_FPSR_AEXC_DZ   = 0b00000000_00000000_00000000_00010000;  //divide by zero
  public static final int FPU_FPSR_AEXC_INEX = 0b00000000_00000000_00000000_00001000;  //inexact

  //  EXCからAEXCへの変換
  //    AEXC_IOP |= EXC_BSUN | EXC_SNAN | EXC_OPERR
  //    AEXC_OVFL |= EXC_OVFL
  //    AEXC_UNFL |= EXC_UNFL & EXC_INEX2
  //    AEXC_DZ |= EXC_DZ
  //    AEXC_INEX |= EXC_OVFL | EXC_INEX2 | EXC_INEX1
  public static final int[] FPU_FPSR_EXC_TO_AEXC = new int[256];

  //コンディション
  //
  //  fpsrのbit27-24
  //    MZIN
  //    0000  0<x
  //    0001  NaN
  //    0010  +Inf
  //    0100  +0
  //    1000  x<0
  //    1010  -Inf
  //    1100  -0
  //
  //  FPU_CCMAP_882[(オペコードまたは拡張ワード&63)<<4|fpsr>>24&15]==trueならば条件成立
  //  FPU_CCMAP_060[(オペコードまたは拡張ワード&63)<<4|fpsr>>24&15]==trueならば条件成立
  //
  //  MC68882とMC68060ではOR,NE,GLE,SNEの条件が異なる

  //MC68882
  //  perl -e "@a=();for$a(0..15){$m=$a>>3&1;$z=$a>>2&1;$n=$a&1;@b=map{$_&1}(0,$z,~($n|$z|$m),$z|~($n|$m),$m&~($n|$z),$z|($m&~$n),~($n|$z),$z|~$n,$n,$n|$z,$n|~($m|$z),$n|($z|~$m),$n|($m&~$z),$n|$z|$m,$n|~$z,1);push@a,@b,@b,@b,@b}for$y(0..63){print'    ';$t=0;for$x(0..15){$c=$a[$x<<6|$y];$t=($t<<1)+$c;print(($c?'T':'F').',');}printf'  //%04x %06b%c',$t,$y,10;}
  //
  //F,F,F,F,F,F,F,F,T,T,T,T,T,T,T,T,  //N
  //F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,  //Z
  //F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,  //NAN
  public static final boolean[] FPU_CCMAP_882 = {
    //                                       cccccc  cc    等式          意味
    //IEEEアウェアテスト
    F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,  //0000 000000  F     0             偽
    F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,  //0f0f 000001  EQ    Z             等しい
    T,F,T,F,F,F,F,F,F,F,F,F,F,F,F,F,  //a000 000010  OGT   ~(NAN|Z|N)    比較可能でより大きい
    T,F,T,F,T,T,T,T,F,F,F,F,T,T,T,T,  //af0f 000011  OGE   Z|~(NAN|N)    等しいか比較可能でより大きい
    F,F,F,F,F,F,F,F,T,F,T,F,F,F,F,F,  //00a0 000100  OLT   N&~(NAN|Z)    比較可能でより小さい
    F,F,F,F,T,T,T,T,T,F,T,F,T,T,T,T,  //0faf 000101  OLE   Z|(N&~NAN)    等しいか比較可能でより小さい
    T,F,T,F,F,F,F,F,T,F,T,F,F,F,F,F,  //a0a0 000110  OGL   ~(NAN|Z)      比較可能で等しくない
    T,F,T,F,T,T,T,T,T,F,T,F,T,T,T,T,  //afaf 000111  OR    Z|~NAN        等しいか比較可能
    F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,  //5555 001000  UN    NAN           比較不能
    F,T,F,T,T,T,T,T,F,T,F,T,T,T,T,T,  //5f5f 001001  UEQ   NAN|Z         比較不能か等しい
    T,T,T,T,F,T,F,T,F,T,F,T,F,T,F,T,  //f555 001010  UGT   NAN|~(N|Z)    比較不能かより大きい
    T,T,T,T,T,T,T,T,F,T,F,T,T,T,T,T,  //ff5f 001011  UGE   NAN|(Z|~N)    比較不能かより大きいか等しい
    F,T,F,T,F,T,F,T,T,T,T,T,F,T,F,T,  //55f5 001100  ULT   NAN|(N&~Z)    比較不能かより小さい
    F,T,F,T,T,T,T,T,T,T,T,T,T,T,T,T,  //5fff 001101  ULE   NAN|Z|N       比較不能かより小さいか等しい
    T,T,T,T,F,T,F,T,T,T,T,T,F,T,F,T,  //f5f5 001110  NE    NAN|~Z        比較不能か等しくない
    T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,  //ffff 001111  T     1             真
    //IEEEノンアウェアテスト
    //  NANがセットされているとき、FPSRのBSUNがセットされ、許可されていれば例外が発生する
    F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,  //0000 010000  SF    0             偽(シグナリング)
    F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,  //0f0f 010001  SEQ   Z             等しい(シグナリング)
    T,F,T,F,F,F,F,F,F,F,F,F,F,F,F,F,  //a000 010010  GT    ~(NAN|Z|N)    より大きい
    T,F,T,F,T,T,T,T,F,F,F,F,T,T,T,T,  //af0f 010011  GE    Z|~(NAN|N)    より大きいか等しい
    F,F,F,F,F,F,F,F,T,F,T,F,F,F,F,F,  //00a0 010100  LT    N&~(NAN|Z)    より小さい
    F,F,F,F,T,T,T,T,T,F,T,F,T,T,T,T,  //0faf 010101  LE    Z|(N&~NAN)    より小さいか等しい
    T,F,T,F,F,F,F,F,T,F,T,F,F,F,F,F,  //a0a0 010110  GL    ~(NAN|Z)      等しくない
    T,F,T,F,T,T,T,T,T,F,T,F,T,T,T,T,  //afaf 010111  GLE   Z|~NAN        より大きいか小さいか等しい
    F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,  //5555 011000  NGLE  NAN           GLEでない
    F,T,F,T,T,T,T,T,F,T,F,T,T,T,T,T,  //5f5f 011001  NGL   NAN|Z         GLでない
    T,T,T,T,F,T,F,T,F,T,F,T,F,T,F,T,  //f555 011010  NLE   NAN|~(N|Z)    LEでない
    T,T,T,T,T,T,T,T,F,T,F,T,T,T,T,T,  //ff5f 011011  NLT   NAN|(Z|~N)    LTでない
    F,T,F,T,F,T,F,T,T,T,T,T,F,T,F,T,  //55f5 011100  NGE   NAN|(N&~Z)    GEでない
    F,T,F,T,T,T,T,T,T,T,T,T,T,T,T,T,  //5fff 011101  NGT   NAN|Z|N       GTでない
    T,T,T,T,F,T,F,T,T,T,T,T,F,T,F,T,  //f5f5 011110  SNE   NAN|~Z        等しくない(シグナリング)
    T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,  //ffff 011111  ST    1             真(シグナリング)
    //
    F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,  //0000 100000
    F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,  //0f0f 100001
    T,F,T,F,F,F,F,F,F,F,F,F,F,F,F,F,  //a000 100010
    T,F,T,F,T,T,T,T,F,F,F,F,T,T,T,T,  //af0f 100011
    F,F,F,F,F,F,F,F,T,F,T,F,F,F,F,F,  //00a0 100100
    F,F,F,F,T,T,T,T,T,F,T,F,T,T,T,T,  //0faf 100101
    T,F,T,F,F,F,F,F,T,F,T,F,F,F,F,F,  //a0a0 100110
    T,F,T,F,T,T,T,T,T,F,T,F,T,T,T,T,  //afaf 100111
    F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,  //5555 101000
    F,T,F,T,T,T,T,T,F,T,F,T,T,T,T,T,  //5f5f 101001
    T,T,T,T,F,T,F,T,F,T,F,T,F,T,F,T,  //f555 101010
    T,T,T,T,T,T,T,T,F,T,F,T,T,T,T,T,  //ff5f 101011
    F,T,F,T,F,T,F,T,T,T,T,T,F,T,F,T,  //55f5 101100
    F,T,F,T,T,T,T,T,T,T,T,T,T,T,T,T,  //5fff 101101
    T,T,T,T,F,T,F,T,T,T,T,T,F,T,F,T,  //f5f5 101110
    T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,  //ffff 101111
    //
    F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,  //0000 110000
    F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,  //0f0f 110001
    T,F,T,F,F,F,F,F,F,F,F,F,F,F,F,F,  //a000 110010
    T,F,T,F,T,T,T,T,F,F,F,F,T,T,T,T,  //af0f 110011
    F,F,F,F,F,F,F,F,T,F,T,F,F,F,F,F,  //00a0 110100
    F,F,F,F,T,T,T,T,T,F,T,F,T,T,T,T,  //0faf 110101
    T,F,T,F,F,F,F,F,T,F,T,F,F,F,F,F,  //a0a0 110110
    T,F,T,F,T,T,T,T,T,F,T,F,T,T,T,T,  //afaf 110111
    F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,  //5555 111000
    F,T,F,T,T,T,T,T,F,T,F,T,T,T,T,T,  //5f5f 111001
    T,T,T,T,F,T,F,T,F,T,F,T,F,T,F,T,  //f555 111010
    T,T,T,T,T,T,T,T,F,T,F,T,T,T,T,T,  //ff5f 111011
    F,T,F,T,F,T,F,T,T,T,T,T,F,T,F,T,  //55f5 111100
    F,T,F,T,T,T,T,T,T,T,T,T,T,T,T,T,  //5fff 111101
    T,T,T,T,F,T,F,T,T,T,T,T,F,T,F,T,  //f5f5 111110
    T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,  //ffff 111111
  };

  //MC68060
  //  perl -e "@a=();for$a(0..15){$m=$a>>3&1;$z=$a>>2&1;$n=$a&1;@b=map{$_&1}(0,$z,~($n|$z|$m),$z|~($n|$m),$m&~($n|$z),$z|($m&~$n),~($n|$z),~$n,$n,$n|$z,$n|~($m|$z),$n|($z|~$m),$n|($m&~$z),$n|$z|$m,~$z,1);push@a,@b,@b,@b,@b}for$y(0..63){print'    ';$t=0;for$x(0..15){$c=$a[$x<<6|$y];$t=($t<<1)+$c;print(($c?'T':'F').',');}printf'  //%04x %06b%c',$t,$y,10;}
  //
  //F,F,F,F,F,F,F,F,T,T,T,T,T,T,T,T,  //N
  //F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,  //Z
  //F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,  //NAN
  public static final boolean[] FPU_CCMAP_060 = {
    //                                       cccccc  cc    等式          意味
    //IEEEアウェアテスト
    F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,  //0000 000000  F     0             偽
    F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,  //0f0f 000001  EQ    Z             等しい
    T,F,T,F,F,F,F,F,F,F,F,F,F,F,F,F,  //a000 000010  OGT   ~(NAN|Z|N)    比較可能でより大きい
    T,F,T,F,T,T,T,T,F,F,F,F,T,T,T,T,  //af0f 000011  OGE   Z|~(NAN|N)    等しいか比較可能でより大きい
    F,F,F,F,F,F,F,F,T,F,T,F,F,F,F,F,  //00a0 000100  OLT   N&~(NAN|Z)    比較可能でより小さい
    F,F,F,F,T,T,T,T,T,F,T,F,T,T,T,T,  //0faf 000101  OLE   Z|(N&~NAN)    等しいか比較可能でより小さい
    T,F,T,F,F,F,F,F,T,F,T,F,F,F,F,F,  //a0a0 000110  OGL   ~(NAN|Z)      比較可能で等しくない
    T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,  //aaaa 000111  OR    ~NAN          比較可能
    F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,  //5555 001000  UN    NAN           比較不能
    F,T,F,T,T,T,T,T,F,T,F,T,T,T,T,T,  //5f5f 001001  UEQ   NAN|Z         比較不能か等しい
    T,T,T,T,F,T,F,T,F,T,F,T,F,T,F,T,  //f555 001010  UGT   NAN|~(N|Z)    比較不能かより大きい
    T,T,T,T,T,T,T,T,F,T,F,T,T,T,T,T,  //ff5f 001011  UGE   NAN|(Z|~N)    比較不能かより大きいか等しい
    F,T,F,T,F,T,F,T,T,T,T,T,F,T,F,T,  //55f5 001100  ULT   NAN|(N&~Z)    比較不能かより小さい
    F,T,F,T,T,T,T,T,T,T,T,T,T,T,T,T,  //5fff 001101  ULE   NAN|Z|N       比較不能かより小さいか等しい
    T,T,T,T,F,F,F,F,T,T,T,T,F,F,F,F,  //f0f0 001110  NE    ~Z            等しくない
    T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,  //ffff 001111  T     1             真
    //IEEEノンアウェアテスト
    //  NANがセットされているとき、FPSRのBSUNがセットされ、許可されていれば例外が発生する
    F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,  //0000 010000  SF    0             偽(シグナリング)
    F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,  //0f0f 010001  SEQ   Z             等しい(シグナリング)
    T,F,T,F,F,F,F,F,F,F,F,F,F,F,F,F,  //a000 010010  GT    ~(NAN|Z|N)    より大きい
    T,F,T,F,T,T,T,T,F,F,F,F,T,T,T,T,  //af0f 010011  GE    Z|~(NAN|N)    より大きいか等しい
    F,F,F,F,F,F,F,F,T,F,T,F,F,F,F,F,  //00a0 010100  LT    N&~(NAN|Z)    より小さい
    F,F,F,F,T,T,T,T,T,F,T,F,T,T,T,T,  //0faf 010101  LE    Z|(N&~NAN)    より小さいか等しい
    T,F,T,F,F,F,F,F,T,F,T,F,F,F,F,F,  //a0a0 010110  GL    ~(NAN|Z)      等しくない
    T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,  //aaaa 010111  GLE   ~NAN          より大きいか小さいか等しい
    F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,  //5555 011000  NGLE  NAN           GLEでない
    F,T,F,T,T,T,T,T,F,T,F,T,T,T,T,T,  //5f5f 011001  NGL   NAN|Z         GLでない
    T,T,T,T,F,T,F,T,F,T,F,T,F,T,F,T,  //f555 011010  NLE   NAN|~(N|Z)    LEでない
    T,T,T,T,T,T,T,T,F,T,F,T,T,T,T,T,  //ff5f 011011  NLT   NAN|(Z|~N)    LTでない
    F,T,F,T,F,T,F,T,T,T,T,T,F,T,F,T,  //55f5 011100  NGE   NAN|(N&~Z)    GEでない
    F,T,F,T,T,T,T,T,T,T,T,T,T,T,T,T,  //5fff 011101  NGT   NAN|Z|N       GTでない
    T,T,T,T,F,F,F,F,T,T,T,T,F,F,F,F,  //f0f0 011110  SNE   ~Z            等しくない(シグナリング)
    T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,  //ffff 011111  ST    1             真(シグナリング)
    //
    F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,  //0000 100000
    F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,  //0f0f 100001
    T,F,T,F,F,F,F,F,F,F,F,F,F,F,F,F,  //a000 100010
    T,F,T,F,T,T,T,T,F,F,F,F,T,T,T,T,  //af0f 100011
    F,F,F,F,F,F,F,F,T,F,T,F,F,F,F,F,  //00a0 100100
    F,F,F,F,T,T,T,T,T,F,T,F,T,T,T,T,  //0faf 100101
    T,F,T,F,F,F,F,F,T,F,T,F,F,F,F,F,  //a0a0 100110
    T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,  //aaaa 100111
    F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,  //5555 101000
    F,T,F,T,T,T,T,T,F,T,F,T,T,T,T,T,  //5f5f 101001
    T,T,T,T,F,T,F,T,F,T,F,T,F,T,F,T,  //f555 101010
    T,T,T,T,T,T,T,T,F,T,F,T,T,T,T,T,  //ff5f 101011
    F,T,F,T,F,T,F,T,T,T,T,T,F,T,F,T,  //55f5 101100
    F,T,F,T,T,T,T,T,T,T,T,T,T,T,T,T,  //5fff 101101
    T,T,T,T,F,F,F,F,T,T,T,T,F,F,F,F,  //f0f0 101110
    T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,  //ffff 101111
    //
    F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,F,  //0000 110000
    F,F,F,F,T,T,T,T,F,F,F,F,T,T,T,T,  //0f0f 110001
    T,F,T,F,F,F,F,F,F,F,F,F,F,F,F,F,  //a000 110010
    T,F,T,F,T,T,T,T,F,F,F,F,T,T,T,T,  //af0f 110011
    F,F,F,F,F,F,F,F,T,F,T,F,F,F,F,F,  //00a0 110100
    F,F,F,F,T,T,T,T,T,F,T,F,T,T,T,T,  //0faf 110101
    T,F,T,F,F,F,F,F,T,F,T,F,F,F,F,F,  //a0a0 110110
    T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,  //aaaa 110111
    F,T,F,T,F,T,F,T,F,T,F,T,F,T,F,T,  //5555 111000
    F,T,F,T,T,T,T,T,F,T,F,T,T,T,T,T,  //5f5f 111001
    T,T,T,T,F,T,F,T,F,T,F,T,F,T,F,T,  //f555 111010
    T,T,T,T,T,T,T,T,F,T,F,T,T,T,T,T,  //ff5f 111011
    F,T,F,T,F,T,F,T,T,T,T,T,F,T,F,T,  //55f5 111100
    F,T,F,T,T,T,T,T,T,T,T,T,T,T,T,T,  //5fff 111101
    T,T,T,T,F,F,F,F,T,T,T,T,F,F,F,F,  //f0f0 111110
    T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,  //ffff 111111
  };

  //fpuInit ()
  //  FPUを初期化する
  //  これはmpuInit()から呼ばれる
  public static void fpuInit () {
    for (int i = 0; i < 256; i++) {
      FPU_FPSR_EXC_TO_AEXC[i] = (((i << 8 & (FPU_FPSR_EXC_BSUN | FPU_FPSR_EXC_SNAN | FPU_FPSR_EXC_OPERR)) != 0 ? FPU_FPSR_AEXC_IOP : 0) |
                                 ((i << 8 & FPU_FPSR_EXC_OVFL) != 0 ? FPU_FPSR_AEXC_OVFL : 0) |
                                 ((i << 8 & (FPU_FPSR_EXC_UNFL | FPU_FPSR_EXC_INEX2)) == (FPU_FPSR_EXC_UNFL | FPU_FPSR_EXC_INEX2) ? FPU_FPSR_AEXC_UNFL : 0) |
                                 ((i << 8 & FPU_FPSR_EXC_DZ) != 0 ? FPU_FPSR_AEXC_DZ : 0) |
                                 ((i << 8 & (FPU_FPSR_EXC_OVFL | FPU_FPSR_EXC_INEX2 | FPU_FPSR_EXC_INEX1)) != 0 ? FPU_FPSR_AEXC_INEX : 0));
    }
    //マザーボードコプロセッサ
    fpuMotherboardCoprocessor = new ExpressionEvaluator ();
    //on-chip FPU
    fpuOnChipFPU = new ExpressionEvaluator ();
    //浮動小数点命令を実行するFPU/FPCP
    fpuBox = currentMPU < Model.MPU_MC68LC040 ? fpuMotherboardCoprocessor : fpuOnChipFPU;
    //浮動小数点レジスタ
    fpuFPn = fpuBox.epbFPn;
    //数値演算プロセッサボード
    fpuCoproboard1 = new EFPBox ();
    fpuCoproboard2 = new EFPBox ();
  }  //fpuInit()



  //========================================================================================
  //$$DBG デバッガ共通コンポーネント

  public static final boolean DBG_ORI_BYTE_ZERO_D0 = true;  //true=ORI.B #$00,D0(オペコード0x0000)を不当命令とみなす機能を有効にする。暴走をなるべく早く検出することで暴走の原因を特定しやすくする

  public static boolean dbgHexSelected;  //true=16進数が選択されている
  public static int dbgHexValue;  //選択されている16進数の値
  public static int dbgSupervisorMode;  //0=ユーザモード,0以外=スーパーバイザモード
  public static JPopupMenu dbgPopupMenu;  //ポップアップメニュー
  public static JMenu dbgPopupIBPMenu;  //命令ブレークポイントメニュー
  public static SpinnerNumberModel dbgPopupIBPCurrentModel;  //現在値のスピナーモデル
  public static int dbgPopupIBPCurrentValue;  //現在値
  public static SpinnerNumberModel dbgPopupIBPThresholdModel;  //閾値のスピナーモデル
  public static int dbgPopupIBPThresholdValue;  //閾値
  public static JMenuItem dbgPopupIBPClearMenuItem;  //解除
  public static JMenu dbgPopupHexMenu;  //16進数メニュー
  public static JMenuItem dbgPopupDisMenuItem;  //逆アセンブル
  public static JMenuItem dbgPopupMemMenuItem;  //メモリダンプ
  public static JMenuItem dbgPopupCopyMenuItem;  //コピー
  public static JMenuItem dbgPopupSelectAllMenuItem;  //すべて選択
  public static JTextArea dbgPopupTextArea;  //ポップアップメニューを表示したテキストエリア
  public static int dbgEventMask;  //イベントマスク。0でないときチェンジリスナーとキャレットリスナーを無効化
  public static boolean dbgStopOnError;  //true=エラーが発生したときコアを止める
  public static boolean dbgOriByteZeroD0;  //true=ORI.B #$00,D0を不当命令とみなす。普段はOFFにしておくこと
  public static boolean dbgStopAtStart;  //true=実行開始位置で停止する

  //共通
  //  sb.append(DBG_SPACES,0,length)でStringBuilderに連続する空白を追加するための配列
  public static final char[] DBG_SPACES = (
    //         11111111112222222222333333333344444444445555555555666666666677777777778
    //12345678901234567890123456789012345678901234567890123456789012345678901234567890
    "                                                                                ").toCharArray ();

  public static final int DBG_DRP_VISIBLE_MASK = 1;  //レジスタウインドウが表示されている
  public static final int DBG_DDP_VISIBLE_MASK = 2;  //逆アセンブルリストウインドウが表示されている
  public static final int DBG_DMP_VISIBLE_MASK = 4;  //メモリダンプウインドウが表示されている
  public static final int DBG_BLG_VISIBLE_MASK = 8;  //分岐ログが表示されている
  public static final int DBG_PFV_VISIBLE_MASK = 16;  //プログラムフロービジュアライザが表示されている
  public static final int DBG_RBP_VISIBLE_MASK = 32;  //ラスタブレークポイントウインドウが表示されている
  public static final int DBG_DBP_VISIBLE_MASK = 64;  //データブレークポイントウインドウが表示されている
  public static final int DBG_SMT_VISIBLE_MASK = 128;  //表示モードテストが表示されている
  public static final int DBG_ATW_VISIBLE_MASK = 256;  //アドレス変換ウインドウが表示されている
  public static final int DBG_PAA_VISIBLE_MASK = 512;  //物理アドレス空間ウインドウが表示されている
  public static final int DBG_RTL_VISIBLE_MASK = 1024;  //ルートポインタリストが表示されている
  public static final int DBG_SPV_VISIBLE_MASK = 2048;  //スプライトパターンビュアが表示されている
  public static final int DBG_PLV_VISIBLE_MASK = 4096;  //スプライトパターンビュアが表示されている
  public static final int DBG_ACM_VISIBLE_MASK = 8192;  //アドレス変換キャッシュモニタが表示されている
  public static int dbgVisibleMask;  //表示されているデバッグ関連ウインドウのマスク

  //dbgInit ()
  //  初期化
  public static void dbgInit () {
    dbgVisibleMask = 0;
    dbgHexSelected = false;
    dbgHexValue = 0;
    dbgSupervisorMode = 1;
    dbgPopupMenu = null;
    dbgPopupDisMenuItem = null;
    dbgPopupMemMenuItem = null;
    dbgPopupCopyMenuItem = null;
    dbgPopupSelectAllMenuItem = null;
    dbgPopupIBPMenu = null;
    dbgPopupIBPCurrentModel = null;
    dbgPopupIBPCurrentValue = 0;
    dbgPopupIBPThresholdModel = null;
    dbgPopupIBPThresholdValue = 0;
    dbgPopupHexMenu = null;
    dbgPopupTextArea = null;
    dbgEventMask = 0;
    dbgStopOnError = false;  //ウインドウを表示する前にも必要なのでここで初期化すること
    if (DBG_ORI_BYTE_ZERO_D0) {
      dbgOriByteZeroD0 = false;
    }
    dbgStopAtStart = false;
  }  //dbgInit()

  //dbgMakePopup ()
  //  デバッグ関連ウインドウの共通コンポーネントを作る
  public static void dbgMakePopup () {

    //ポップアップメニュー
    ActionListener popupActionListener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        switch (ae.getActionCommand ()) {
        case "Disassemble":
          DisassembleList.ddpBacktraceRecord = -1L;  //分岐レコードの選択を解除する
          DisassembleList.ddpOpen (dbgHexValue, dbgSupervisorMode, false);
          break;
        case "Memory Dump":
          MemoryDumpList.dmpOpen (dbgHexValue, dbgSupervisorMode != 0 ? 5 : 1, false);
          break;
        case "Run to Here":
          if (InstructionBreakPoint.IBP_ON) {
            if (mpuTask == null) {
              InstructionBreakPoint.ibpInstant (DisassembleList.ddpPopupAddress, DisassembleList.ddpSupervisorMode);
              mpuStart ();
            }
          }
          break;
        case "Set Breakpoint":
          if (InstructionBreakPoint.IBP_ON) {
            InstructionBreakPoint.ibpPut (DisassembleList.ddpPopupAddress, DisassembleList.ddpSupervisorMode, dbgPopupIBPCurrentValue, dbgPopupIBPThresholdValue, null);
            DisassembleList.ddpOpen (0, DisassembleList.ddpSupervisorMode, true);
          }
          break;
        case "Clear Breakpoint":
          if (InstructionBreakPoint.IBP_ON) {
            InstructionBreakPoint.ibpRemove (DisassembleList.ddpPopupAddress, DisassembleList.ddpSupervisorMode);
            DisassembleList.ddpOpen (0, DisassembleList.ddpSupervisorMode, true);
          }
          break;
        case "Copy":
          dbgCopy ();
          break;
        case "Select All":
          dbgSelectAll ();
          break;
        }
      }
    };
    dbgPopupMenu = ComponentFactory.createPopupMenu (
      dbgPopupIBPMenu =
      InstructionBreakPoint.IBP_ON ?
      ComponentFactory.createMenu (
        "XXXXXXXX", KeyEvent.VK_UNDEFINED,
        Multilingual.mlnText (ComponentFactory.createMenuItem ("Run to Here", 'R', popupActionListener), "ja", "ここまで実行"),
        ComponentFactory.createHorizontalSeparator (),
        Multilingual.mlnText (ComponentFactory.createMenuItem ("Set Breakpoint", 'S', popupActionListener), "ja", "ブレークポイントを設定"),
        ComponentFactory.createHorizontalBox (
          Box.createHorizontalStrut (7),
          Box.createHorizontalGlue (),
          ComponentFactory.setPreferredSize (
            Multilingual.mlnText (ComponentFactory.createLabel ("current"), "ja", "現在値"),
            60, 16),
          ComponentFactory.createNumberSpinner (dbgPopupIBPCurrentModel = new SpinnerNumberModel (0, 0, 0x7fffffff, 1), 10, new ChangeListener () {
            @Override public void stateChanged (ChangeEvent ce) {
              dbgPopupIBPCurrentValue = dbgPopupIBPCurrentModel.getNumber ().intValue ();
            }
          }),
          Box.createHorizontalGlue ()
          ),
        ComponentFactory.createHorizontalBox (
          Box.createHorizontalStrut (7),
          Box.createHorizontalGlue (),
          ComponentFactory.setPreferredSize (
            Multilingual.mlnText (ComponentFactory.createLabel ("threshold"), "ja", "閾値"),
            60, 16),
          ComponentFactory.createNumberSpinner (dbgPopupIBPThresholdModel = new SpinnerNumberModel (0, 0, 0x7fffffff, 1), 10, new ChangeListener () {
            @Override public void stateChanged (ChangeEvent ce) {
              dbgPopupIBPThresholdValue = dbgPopupIBPThresholdModel.getNumber ().intValue ();
            }
          }),
          Box.createHorizontalGlue ()
          ),
        dbgPopupIBPClearMenuItem =
        Multilingual.mlnText (ComponentFactory.createMenuItem ("Clear Breakpoint", 'C', popupActionListener), "ja", "ブレークポイントを消去")
        ) :
      null,
      dbgPopupHexMenu =
      ComponentFactory.createMenu (
        "XXXXXXXX", KeyEvent.VK_UNDEFINED,
        dbgPopupDisMenuItem = Multilingual.mlnText (ComponentFactory.createMenuItem ("Disassemble", 'D', popupActionListener), "ja", "逆アセンブル"),
        dbgPopupMemMenuItem = Multilingual.mlnText (ComponentFactory.createMenuItem ("Memory Dump", 'M', popupActionListener), "ja", "メモリダンプ")
        ),
      dbgPopupCopyMenuItem = Multilingual.mlnText (ComponentFactory.createMenuItem ("Copy", 'C', popupActionListener), "ja", "コピー"),
      dbgPopupSelectAllMenuItem = Multilingual.mlnText (ComponentFactory.createMenuItem ("Select All", 'A', popupActionListener), "ja", "すべて選択")
      );

  }  //dbgMakePopup()

  //dbgShowPopup (me, textArea, dis)
  //  ポップアップメニューを表示する
  public static void dbgShowPopup (MouseEvent me, JTextArea textArea, boolean dis) {
    dbgEventMask++;
    int x = me.getX ();
    int y = me.getY ();
    //int p = textArea.viewToModel (me.getPoint ());  //クリックされた位置。viewToModel2Dは9から
    int p = textArea.viewToModel2D (me.getPoint ());  //クリックされた位置。viewToModel2Dは9から
    DisassembleList.ddpPopupAddress = -1;  //クリックされた行のアドレス
    if (dis) {
      int i = Arrays.binarySearch (DisassembleList.ddpSplitArray, 1, DisassembleList.ddpItemCount, p + 1);
      i = (i >> 31 ^ i) - 1;  //クリックされた項目の番号
      DisassembleList.ddpPopupAddress = DisassembleList.ddpAddressArray[i];  //クリックされた項目の先頭アドレス
    }
    int start = textArea.getSelectionStart ();  //選択範囲の開始位置
    int end = textArea.getSelectionEnd ();  //選択範囲の終了位置
    String text = textArea.getText ();  //テキスト全体
    int length = text.length ();  //テキスト全体の長さ
    if ((start == end ||  //選択範囲がないか
         p < start || end <= p) &&  //選択範囲の外側がクリックされて
        0 <= p && p < length && isWord (text.charAt (p))) {  //クリックされた位置に単語があるとき
      //クリックされた位置にある単語を選択する
      for (start = p; 0 < start && isWord (text.charAt (start - 1)); start--) {
      }
      for (end = p + 1; end < length && isWord (text.charAt (end)); end++) {
      }
      textArea.select (start, end);
    }
    dbgHexSelected = false;
    if (start < end) {  //選択範囲があるとき
      textArea.requestFocusInWindow ();  //フォーカスがないと選択範囲が見えない
      //選択範囲にある16進数の文字を取り出す
      //  以下の条件を加える
      //    選択範囲に16進数以外の単語の文字がないこと
      //    選択範囲に16進数の文字が9文字以上ないこと
      //    16進数の文字が偶数文字ずつの塊になっていること
      dbgHexValue = 0;
      int n = 0;
      for (int i = start; i < end; i++) {
        int t;
        if ((t = Character.digit (text.charAt (i), 16)) >= 0) {  //16進数の文字
          dbgHexValue = dbgHexValue << 4 | t;
          if (n >= 8 ||  //選択範囲に16進数の文字が9文字以上ある
              i + 1 >= end || (t = Character.digit (text.charAt (i + 1), 16)) < 0) {  //16進数の文字が偶数文字ずつの塊になっていない
            n = 0;
            break;
          }
          dbgHexValue = dbgHexValue << 4 | t;
          n += 2;
          i++;
        } else if (isWord (text.charAt (i))) {  //16進数以外の単語の文字
          n = 0;
          break;
        }
      }
      dbgHexSelected = n > 0;
      try {
        //Rectangle r = textArea.modelToView (start).getBounds ();  //modelToView2Dは9から
        Rectangle r = textArea.modelToView2D (start).getBounds ();  //modelToView2Dは9から
        //Rectangle s = textArea.modelToView (end - 1).getBounds ();  //modelToView2Dは9から
        Rectangle s = textArea.modelToView2D (end - 1).getBounds ();  //modelToView2Dは9から
        if (r.y == s.y) {  //選択範囲が1行だけのとき
          //選択範囲を隠してしまわないようにポップアップを選択範囲の下側に表示する
          y = r.y + r.height;
        }
      } catch (BadLocationException ble) {
      }
    }
    //逆アセンブルリストでコアが止まっていて選択範囲がなくてクリックされた行のアドレスがわかるとき命令ブレークポイントメニューが有効
    if (InstructionBreakPoint.IBP_ON) {
      if (dis && mpuTask == null && DisassembleList.ddpPopupAddress != -1) {
        ComponentFactory.setText (dbgPopupIBPMenu, fmtHex8 (DisassembleList.ddpPopupAddress));
        TreeMap<Integer,InstructionBreakPoint.InstructionBreakRecord> pointTable = InstructionBreakPoint.ibpPointTable;
        InstructionBreakPoint.InstructionBreakRecord r = pointTable.get (DisassembleList.ddpPopupAddress);
        if (r != null) {  //命令ブレークポイントがあるとき
          dbgPopupIBPCurrentModel.setValue (Integer.valueOf (dbgPopupIBPCurrentValue = r.ibrValue));  //現在値
          dbgPopupIBPThresholdModel.setValue (Integer.valueOf (dbgPopupIBPThresholdValue = r.ibrThreshold));  //閾値
          dbgPopupIBPClearMenuItem.setEnabled (true);  //消去できる
        } else {  //命令ブレークポイントがないとき
          dbgPopupIBPCurrentModel.setValue (Integer.valueOf (dbgPopupIBPCurrentValue = 0));  //現在値
          dbgPopupIBPThresholdModel.setValue (Integer.valueOf (dbgPopupIBPThresholdValue = 0));  //閾値
          dbgPopupIBPClearMenuItem.setEnabled (false);  //消去できない
        }
        ComponentFactory.setVisible (dbgPopupIBPMenu, true);
      } else {
        ComponentFactory.setVisible (dbgPopupIBPMenu, false);
      }
    }
    //16進数が選択されていれば16進数メニューが有効
    if (dbgHexSelected) {
      ComponentFactory.setText (dbgPopupHexMenu, fmtHex8 (dbgHexValue));
      ComponentFactory.setVisible (dbgPopupHexMenu, true);
    } else {
      ComponentFactory.setVisible (dbgPopupHexMenu, false);
    }
    //選択範囲があればコピーが有効
    ComponentFactory.setEnabled (dbgPopupCopyMenuItem, clpClipboard != null && start < end);
    //クリップボードがあればすべて選択が有効
    ComponentFactory.setEnabled (dbgPopupSelectAllMenuItem, clpClipboard != null);
    //ポップアップメニューを表示する
    dbgPopupTextArea = textArea;
    dbgPopupMenu.show (textArea, x, y);
    dbgEventMask--;
  }  //dbgShowPopup(MouseEvent,JTextArea,boolean)

  public static boolean isWord (char c) {
    return '0' <= c && c <= '9' || 'A' <= c && c <= 'Z' || 'a' <= c && c <= 'z' || c == '_';
  }  //isWord(char)

  //dbgCopy ()
  //  コピー
  public static void dbgCopy () {
    clpCopy (dbgPopupTextArea.getSelectedText ());
  }  //dbgCopy()

  //dbgSelectAll ()
  //  すべて選択
  public static void dbgSelectAll () {
    if (clpClipboard != null) {
      //すべて選択する
      dbgEventMask++;
      dbgPopupTextArea.selectAll ();
      dbgPopupTextArea.requestFocusInWindow ();
      dbgEventMask--;
    }
  }  //dbgSelectAll()

  //dbgUpdate ()
  //  デバッグウインドウを更新する
  //  コアのrun()の末尾でdbgVisibleMask!=0のとき呼び出す
  public static void dbgUpdate () {
    if ((dbgVisibleMask & DBG_DRP_VISIBLE_MASK) != 0) {
      RegisterList.drpUpdate ();  //レジスタウインドウを更新する
    }
    if (ProgramFlowVisualizer.PFV_ON) {
      if ((dbgVisibleMask & DBG_PFV_VISIBLE_MASK) != 0) {
        if (ProgramFlowVisualizer.pfvTimer == 0) {
          ProgramFlowVisualizer.pfvUpdate ();  //プログラムフロービジュアライザを更新する
        } else {
          ProgramFlowVisualizer.pfvTimer--;
        }
      }
    }
    if (RasterBreakPoint.RBP_ON) {
      if ((dbgVisibleMask & DBG_RBP_VISIBLE_MASK) != 0) {
        if (RasterBreakPoint.rbpTimer == 0) {
          RasterBreakPoint.rbpUpdateFrame ();  //ラスタブレークポイントウインドウを更新する
        } else {
          RasterBreakPoint.rbpTimer--;
        }
      }
    }
    if (ScreenModeTest.SMT_ON) {
      if ((dbgVisibleMask & DBG_SMT_VISIBLE_MASK) != 0) {
        if (ScreenModeTest.smtTimer == 0) {
          ScreenModeTest.smtUpdateFrame ();  //表示モードテストウインドウを更新する
        } else {
          ScreenModeTest.smtTimer--;
        }
      }
    }
    if (RootPointerList.RTL_ON) {
      if ((dbgVisibleMask & DBG_RTL_VISIBLE_MASK) != 0) {
        if (RootPointerList.rtlTimer == 0) {
          RootPointerList.rtlTimer = RootPointerList.RTL_INTERVAL - 1;
          RootPointerList.rtlUpdateFrame ();  //ルートポインタリストを更新する
        } else {
          RootPointerList.rtlTimer--;
        }
      }
    }
    if (SpritePatternViewer.SPV_ON) {
      if ((dbgVisibleMask & DBG_SPV_VISIBLE_MASK) != 0) {
        if (SpritePatternViewer.spvTimer == 0) {
          SpritePatternViewer.spvTimer = SpritePatternViewer.SPV_INTERVAL - 1;
          SpritePatternViewer.spvUpdateFrame ();  //スプライトパターンビュアを更新する
        } else {
          SpritePatternViewer.spvTimer--;
        }
      }
    }
    if (PaletteViewer.PLV_ON) {
      if ((dbgVisibleMask & DBG_PLV_VISIBLE_MASK) != 0) {
        if (PaletteViewer.plvTimer == 0) {
          PaletteViewer.plvTimer = PaletteViewer.PLV_INTERVAL - 1;
          PaletteViewer.plvUpdateFrame ();  //スプライトパターンビュアを更新する
        } else {
          PaletteViewer.plvTimer--;
        }
      }
    }
    if (ATCMonitor.ACM_ON) {
      if ((dbgVisibleMask & DBG_ACM_VISIBLE_MASK) != 0) {
        if (ATCMonitor.acmTimer == 0) {
          ATCMonitor.acmTimer = ATCMonitor.ACM_INTERVAL - 1;
          ATCMonitor.acmUpdateFrame ();  //アドレス変換キャッシュモニタを更新する
        } else {
          ATCMonitor.acmTimer--;
        }
      }
    }
  }  //dbgUpdate()

  //dbgDoStopOnError ()
  //  エラーで停止する
  //  エラーを検出して例外スタックフレームを構築した後にdbgStopOnErrorならば呼び出してコアを停止させる
  //
  //  Human68kの_BUS_ERRの中では停止させない
  //  human302のシステムディスクで起動した場合、
  //  SCSIボード、RS-232Cボード、MIDIボードのテストで_BUS_ERRが呼び出されてバスエラーが発生する
  //    bus error on reading from 00EA0044 at 0000E2F4
  //    bus error on reading from 00EAFC04 at 0002D04A
  //    bus error on reading from 00EAFC14 at 0002D04A
  //    bus error on reading from 00EAFA01 at 0005CD54
  //  _BUS_ERRはレベル0で入ったDOSコールの番号を更新しないので、_BUS_ERRの中かどうかはpcで判断する
  //    0x0000e342 <= pc0 && pc0 < 0x0000e3b6  human200/human201の_BUS_ERR
  //    0x0000e3c8 <= pc0 && pc0 < 0x0000e43c  human202の_BUS_ERR
  //    0x0000e1a8 <= pc0 && pc0 < 0x0000e21c  human203の_BUS_ERR
  //    0x0000e256 <= pc0 && pc0 < 0x0000e2ca  human215の_BUS_ERR
  //    0x0000e174 <= pc0 && pc0 < 0x0000e1e8  human301の_BUS_ERR
  //    0x0000e28a <= pc0 && pc0 < 0x0000e2fe  human302の_BUS_ERR
  //
  //  DOSコールで発生した特権違反では停止させない
  //
  public static boolean dbgDoStopOnError () {
    if (MainMemory.mmrHumanVersion <= 0) {  //Human68kでないか未確認
      return true;
    }
    if ((regOC & 0xff00) == 0xff00 &&  //DOSコールで発生した
        M68kException.m6eNumber == M68kException.M6E_PRIVILEGE_VIOLATION) {  //特権違反
      return false;
    }
    String message = (
      M68kException.m6eNumber < 0 ?
      fmtHex8 (new StringBuilder ("breaked").append (" at "), regPC0).toString () :
      M68kException.m6eNumber <= M68kException.M6E_ADDRESS_ERROR ?
      fmtHex8 (fmtHex8 (new StringBuilder ("ERROR: ").append (M68kException.M6E_ERROR_NAME[M68kException.m6eNumber])
                        .append (M68kException.m6eDirection == 0 ? " on writing to " : " on reading from "), M68kException.m6eAddress)
               .append (" at "), regPC0).toString () :
      fmtHex8 (new StringBuilder (M68kException.M6E_ERROR_NAME[M68kException.m6eNumber])
               .append (" at "), regPC0).toString ()
      );
    System.out.println (message);
    if (!(M68kException.m6eNumber == M68kException.M6E_ACCESS_FAULT &&
          0x0000e100 <= regPC0 && regPC0 < 0x0000e500)) {  //_BUS_ERRの中で発生したバスエラーでないとき
      mpuStop (message);
      return true;
    }
    return false;
  }  //dbgDoStopOnError()

  //dbgDoubleBusFault ()
  //  ダブルバスフォルト
  public static void dbgDoubleBusFault () {
    String message =
      fmtHex8 (fmtHex8 (new StringBuilder ("FATAL ERROR: ").append (M68kException.M6E_ERROR_NAME[M68kException.m6eNumber])
                        .append (M68kException.m6eDirection == 0 ? " on writing to " : " on reading from "), M68kException.m6eAddress)
               .append (" at "), regPC0).toString ();
    System.out.println (message);
    mpuStop (message);
  }  //dbgDoubleBusFault()



  //========================================================================================
  //$$RSC ResourceFile

  public static final HashMap<String,byte[]> rscResourceCache = new HashMap<String,byte[]> ();

  //array = rscGetResource (name, sizes...)
  //  リソースファイルを読み込む。null=読み込めなかった
  //  ファイル名をコンマで区切る機能はない。XEiJ.jarの中なので必要ない
  //  *.zipや*.gzは非対応。XEiJ.jarの中なので必要ない
  //  キャッシュした配列をそのまま返すので書き換えないように注意すること
  public static byte[] rscGetResource (String name, int... sizes) {
    byte[] array = rscResourceCache.get (name);  //キャッシュから出す
    if (array != null) {  //読み込み済み
      return array;
    }
    array = new byte[1024 * 64];  //最初は64KB
    int size = 0;  //これまでに読み込んだ長さ
    try (BufferedInputStream bis = new BufferedInputStream (XEiJ.class.getResourceAsStream ("../data/" + name))) {
      for (;;) {
        if (size == array.length) {  //配列が一杯
          byte[] newArray = new byte[array.length * 2];  //2倍に伸ばした配列を作る
          System.arraycopy (array, 0, newArray, 0, size);  //読み込んだ部分を新しい配列に移す
          array = newArray;  //新しい配列に移行する
        }
        int step = bis.read (array, size, array.length - size);  //続きを読み込む。今回読み込んだ長さ
        if (step == -1) {  //終わり
          break;
        }
        size += step;  //これまでに読み込んだ長さ
      }
      if (size < array.length) {  //配列が長すぎる
        byte[] newArray = new byte[size];  //切り詰めた配列を作る
        System.arraycopy (array, 0, newArray, 0, size);  //読み込んだ部分を新しい配列に移す
        array = newArray;  //新しい配列に移行する
      }
      boolean fit = sizes.length == 0;
      if (!fit) {
        for (int i = 0; i < sizes.length; i++) {
          if (size == sizes[i]) {  //サイズが合っている
            fit = true;
            break;
          }
        }
      }
      if (fit) {
        System.out.println (Multilingual.mlnJapanese ?
                            name + " を読み込みました" :
                            name + " was read");
        rscResourceCache.put (name, array);  //キャッシュに入れる
        return array;  //try-with-resourcesなのでここでクローズされる
      }
      System.out.println (Multilingual.mlnJapanese ?
                          name + " のサイズが違います" :
                          name + " has wrong size");
      return null;  //try-with-resourcesなのでここでクローズされる
    } catch (IOException ioe) {
    }
    //try-with-resourcesなのでここでクローズされる
    System.out.println (Multilingual.mlnJapanese ?
                        name + " を読み込めません" :
                        name + " cannot be read");
    return null;
  }

  //string = rscGetResourceText (name, charset)
  //  リソースファイルからテキストを読み込む
  public static String rscGetResourceText (String name) {
    return rscGetResourceText (name, "UTF-8");
  }
  public static String rscGetResourceText (String name, String charset) {
    byte[] array = rscGetResource (name);
    if (name != null) {
      try {
        return new String (array, charset);
      } catch (UnsupportedEncodingException uee) {
      }
    }
    return "";
  }

  public static final Pattern RSC_ZIP_SEPARATOR = Pattern.compile ("(?<=\\.(?:jar|zip))(?:/|\\\\)(?=.)", Pattern.CASE_INSENSITIVE);
  public static String rscLastFileName = null;  //最後に読み込んだファイル名

  //array = rscGetFile (names, sizes...)
  //  ファイルを読み込む。null=読み込めなかった
  //  コンマで区切られたファイルを順に探して最初に読み込めたものを返す
  //  *.zip/entryを指定するとエントリを読み込む
  //  *.gzを指定すると解凍する
  public static byte[] rscGetFile (String names, int... sizes) {
    for (String name : names.split (",")) {  //コンマで区切る
      name = name.trim ();  //前後の空白を削除する
      if (name.length () == 0 || name.equalsIgnoreCase ("none")) {
        continue;
      }
      String[] zipSplittedName = RSC_ZIP_SEPARATOR.split (name, 2);  // *.zip/entryを*.zipとentryに分ける
      InputStream is = null;
      if (zipSplittedName.length < 2) {  // *.zip/entryではない
        File file = new File (name);
        if (file.isFile ()) {  //ファイルがある
          try {
            is = new FileInputStream (file);  //ファイルを開く
          } catch (IOException ioe) {
          }
        } else {  //ファイルがない
          System.out.println (Multilingual.mlnJapanese ?
                              name + " がありません" :
                              name + " does not exist");
          continue;
        }
      } else {  // *.zip/entry
        String zipName = zipSplittedName[0];  // *.zip
        String entryName = zipSplittedName[1];  //entry
        if (new File (zipName).isFile ()) {  //ファイルがある
          try {
            ZipFile zipFile = new ZipFile (zipName);  //ファイル
            ZipEntry zipEntry = zipFile.getEntry (entryName);  //エントリ
            if (zipEntry != null) {  //エントリがある
              is = zipFile.getInputStream (zipEntry);  //エントリを開く
            } else {  //エントリがない
              System.out.println (Multilingual.mlnJapanese ?
                                  zipName + " に " + zipEntry + " がありません" :
                                  zipName + " does not include " + zipEntry);
            }
          } catch (IOException ioe) {
          }
        } else {  //ファイルがない
          System.out.println (Multilingual.mlnJapanese ?
                              zipName + " がありません" :
                              zipName + " does not exist");
          continue;
        }
      }
      if (is != null) {  //開けた
        try {
          is = new BufferedInputStream (is);
          if (name.toLowerCase ().endsWith (".gz")) {  // *.gz
            is = new GZIPInputStream (is);  //展開する
          }
          byte[] array = new byte[1024 * 64];  //最初は64KB
          int size = 0;  //これまでに読み込んだ長さ
          for (;;) {
            if (size == array.length) {  //配列が一杯
              byte[] newArray = new byte[array.length * 2];  //2倍に伸ばした配列を作る
              System.arraycopy (array, 0, newArray, 0, size);  //読み込んだ部分を新しい配列に移す
              array = newArray;  //新しい配列に移行する
            }
            int step = is.read (array, size, array.length - size);  //続きを読み込む。今回読み込んだ長さ
            if (step == -1) {  //終わり
              break;
            }
            size += step;  //これまでに読み込んだ長さ
          }
          is.close ();  //ここはtry-with-resourcesではないので明示的にクローズすること
          is = null;
          if (size < array.length) {  //配列が長すぎる
            byte[] newArray = new byte[size];  //切り詰めた配列を作る
            System.arraycopy (array, 0, newArray, 0, size);  //読み込んだ部分を新しい配列に移す
            array = newArray;  //新しい配列に移行する
          }
          boolean fit = sizes.length == 0;
          if (!fit) {
            for (int i = 0; i < sizes.length; i++) {
              if (size == sizes[i]) {  //サイズが合っている
                fit = true;
                break;
              }
            }
          }
          if (fit) {
            System.out.println (Multilingual.mlnJapanese ?
                                name + " を読み込みました" :
                                name + " was read");
            rscLastFileName = name;  //最後に読み込んだファイル名
            return array;  //配列を返す
          }
          System.out.println (Multilingual.mlnJapanese ?
                              name + " のサイズが違います" :
                              name + " has wrong size");
          continue;
        } catch (IOException ioe) {
        }
        if (is != null) {
          try {
            is.close ();  //ここはtry-with-resourcesではないので明示的にクローズすること
            is = null;
          } catch (IOException ioe) {
          }
        }
      }  //if 開けた
      System.out.println (Multilingual.mlnJapanese ?
                          name + " を読み込めません" :
                          name + " cannot be read");
    }  //for name
    //読み込めなかった
    //ファイル名が指定されたときはメッセージが表示される
    //ファイル名が指定されなかったときはエラーメッセージも表示されない
    return null;
  }

  //string = rscGetTextFile (name)
  //string = rscGetTextFile (name, charset)
  //  テキストファイルを読み込む
  public static String rscGetTextFile (String name) {
    return rscGetTextFile (name, "UTF-8");
  }
  public static String rscGetTextFile (String name, String charset) {
    byte[] array = rscGetFile (name);
    if (array != null) {
      try {
        return new String (array, charset);
      } catch (UnsupportedEncodingException uee) {
      }
    }
    return "";
  }

  //mask = rscShowError (message, mask)
  //  ファイル操作に失敗したときに表示するダイアログ
  public static final int RSC_A_MASK = 1;  //中止
  public static final int RSC_R_MASK = 2;  //再実行
  public static final int RSC_I_MASK = 4;  //無視
  public static final String RSC_A_EN = "Abort";
  public static final String RSC_R_EN = "Retry";
  public static final String RSC_I_EN = "Ignore";
  public static final String RSC_A_JA = "中止";
  public static final String RSC_R_JA = "再実行";
  public static final String RSC_I_JA = "無視";
  public static final String[][] RSC_EN_OPTIONS = {
    { RSC_A_EN                     },
    { RSC_A_EN                     },
    {           RSC_R_EN           },
    { RSC_A_EN, RSC_R_EN           },
    {                     RSC_I_EN },
    { RSC_A_EN,           RSC_I_EN },
    {           RSC_R_EN, RSC_I_EN },
    { RSC_A_EN, RSC_R_EN, RSC_I_EN },
  };
  public static final String[][] RSC_JA_OPTIONS = {
    { RSC_A_JA                     },
    { RSC_A_JA                     },
    {           RSC_R_JA           },
    { RSC_A_JA, RSC_R_JA           },
    {                     RSC_I_JA },
    { RSC_A_JA,           RSC_I_JA },
    {           RSC_R_JA, RSC_I_JA },
    { RSC_A_JA, RSC_R_JA, RSC_I_JA },
  };
  public static int rscShowError (String message, int mask) {
    System.out.println (message);
    mask &= RSC_A_MASK | RSC_R_MASK | RSC_I_MASK;
    if (mask == 0) {
      mask = RSC_A_MASK;
    }
    String[] options = (Multilingual.mlnJapanese ? RSC_JA_OPTIONS : RSC_EN_OPTIONS)[mask];
    int def = Integer.numberOfTrailingZeros (mask);  //デフォルトの選択肢。0,1,2。中止、再実行、無視の順で最初に見つかったもの
    pnlExitFullScreen (true);
    int bit = JOptionPane.showOptionDialog (
      null,  //parentComponent
      message,  //message
      Multilingual.mlnJapanese ? "ファイル操作エラー" : "File operation error",  //title
      JOptionPane.YES_NO_CANCEL_OPTION,  //optionType
      JOptionPane.ERROR_MESSAGE,  //messageType
      null,  //icon
      options,  //options
      options[def]);  //initialValue
    if (bit == JOptionPane.CLOSED_OPTION) {  //閉じた。-1
      bit = def;
    }
    return 1 << bit;
  }

  //success = rscPutTextFile (name, string)
  //success = rscPutTextFile (name, strings)
  //success = rscPutTextFile (name, string, charset)
  //success = rscPutTextFile (name, strings, charset)
  //  テキストファイルに書き出す
  public static boolean rscPutTextFile (String name, String string) {
    return rscPutTextFile (name, string, "UTF-8");
  }
  public static boolean rscPutTextFile (String name, ArrayList<String> strings) {
    return rscPutTextFile (name, strings, "UTF-8");
  }
  public static boolean rscPutTextFile (String name, String string, String charset) {
    ArrayList<String> strings = new ArrayList<String> ();
    strings.add (string);
    return rscPutTextFile (name, strings, charset);
  }
  public static boolean rscPutTextFile (String name, ArrayList<String> strings, String charset) {
    String nameTmp = name + ".tmp";
    String nameBak = name + ".bak";
    File file = new File (name);
    File fileTmp = new File (nameTmp);
    File fileBak = new File (nameBak);
    //親ディレクトリがなければ作る
    File parentDirectory = file.getParentFile ();  //親ディレクトリ
    if (parentDirectory != null && !parentDirectory.isDirectory ()) {  //親ディレクトリがない
      if (!parentDirectory.mkdirs ()) {  //親ディレクトリを作る。必要ならば遡って作る。作れなかった
        System.out.println (parentDirectory.getPath () + (Multilingual.mlnJapanese ? " を作れません" : " cannot be created"));
        return false;
      }
    }
    //name.tmpがあればname.tmpを削除する
    if (fileTmp.exists ()) {  //name.tmpがある
      if (!fileTmp.delete ()) {  //name.tmpを削除する。削除できなかった
        System.out.println (nameTmp + (Multilingual.mlnJapanese ? " を削除できません" : " cannot be deleted"));
        return false;
      }
    }
    //name.tmpに出力する
    try (BufferedWriter bw = new BufferedWriter (new FileWriter (nameTmp, Charset.forName (charset)))) {
      for (String string : strings) {
        bw.write (string);
      }
    } catch (IOException ioe) {
      ioe.printStackTrace ();
      System.out.println (nameTmp + (Multilingual.mlnJapanese ? " に書き出せません" : " cannot be written"));
      return false;
    }
    //nameがあればnameをname.bakにリネームする
    boolean fileExists = file.exists ();
    if (fileExists) {  //nameがある
      //name.bakがあればname.bakを削除する
      if (fileBak.exists ()) {  //name.bakがある
        if (!fileBak.delete ()) {  //name.bakを削除する。削除できなかった
          System.out.println (nameBak + (Multilingual.mlnJapanese ? " を削除できません" : " cannot be deleted"));
          return false;
        }
      }
      //nameをname.bakにリネームする
      if (!file.renameTo (fileBak)) {  //nameをname.bakにリネームする。リネームできなかった
        System.out.println (name + (Multilingual.mlnJapanese ? " を " : " cannot be renamed to ") + nameBak + (Multilingual.mlnJapanese ? " にリネームできません" : ""));
        return false;
      }
    }
    //name.tmpをnameにリネームする
    if (!fileTmp.renameTo (file)) {  //name.tmpをnameにリネームする。リネームできなかった
      System.out.println (nameTmp + (Multilingual.mlnJapanese ? " を " : " cannot be renamed to ") + name + (Multilingual.mlnJapanese ? " にリネームできません" : ""));
      return false;
    }
    if (fileExists) {  //nameがあった
      System.out.println (name + (Multilingual.mlnJapanese ? " を更新しました" : " was updated"));
    } else {  //nameがあった
      System.out.println (name + (Multilingual.mlnJapanese ? " を作りました" : " was created"));
    }
    return true;
  }

  //success = rscPutFile (name, array)
  //success = rscPutFile (name, array, offset, length)
  //success = rscPutFile (name, array, offset, length, longLength2)
  //  ファイルに書き出す
  //  nameにarray[offset..offset+length-1]とlongLength2-length個の0を書き出す
  //  親ディレクトリがなければ作る
  //  同じ内容のファイルが既にあるときは更新しない
  //  *.zip/entryは非対応
  public static boolean rscPutFile (String name, byte[] array) {
    return rscPutFile (name, array, 0, array.length, (long) array.length);
  }
  public static boolean rscPutFile (String name, byte[] array, int offset, int length) {
    return rscPutFile (name, array, offset, length, (long) length);
  }
  public static boolean rscPutFile (String name, byte[] array, int offset, int length, long longLength2) {
    if (RSC_ZIP_SEPARATOR.matcher (name).matches ()) {  // *.zip/entry
      // *.zip/entryのとき確実に弾かないとファイルを破壊するおそれがある
      return false;
    }
    File file = new File (name);
    boolean fileExists = file.isFile ();  //true=同じ名前のファイルがある
    if (fileExists && file.length () == longLength2) {  //同じ名前で同じ長さのファイルがある
    comparison:
      {
        try (BufferedInputStream bis = new BufferedInputStream (new FileInputStream (file))) {
          byte[] buffer = new byte[(int) Math.min (Math.max ((long) length, longLength2 - (long) length), (long) (1024 * 1024))];  //最大1MBずつ読み込んで比較する
          int position = 0;
          while (position < length) {
            int step = bis.read (buffer, 0, Math.min (buffer.length, length - position));
            if (step == -1) {  //足りない。長さを確認してから始めたのだから途中で終わるはずがない
              break comparison;
            }
            int offsetPosition = offset + position;
            for (int i = 0; i < step; i++) {
              if (buffer[i] != array[offsetPosition + i]) {  //一致しない
                break comparison;
              }
            }
            position += step;
          }
          long longPosition2 = (long) length;
          while (longPosition2 < longLength2) {
            int step = bis.read (buffer, 0, (int) Math.min ((long) buffer.length, longLength2 - longPosition2));
            if (step == -1) {  //足りない。長さを確認してから始めたのだから途中で終わるはずがない
              break comparison;
            }
            for (int i = 0; i < step; i++) {
              if (buffer[i] != 0) {  //一致しない
                break comparison;
              }
            }
            longPosition2 += (long) step;
          }
          return true;  //一致した
        } catch (IOException ioe) {
        }
      }  //match
    }  //if 同じ名前で同じ長さのファイルがある
    String nameTmp = name + ".tmp";
    File fileTmp = new File (nameTmp);
    String nameBak = name + ".bak";
    File fileBak = new File (nameBak);
  retry:
    for (;;) {
      File parentDirectory = file.getParentFile ();  //親ディレクトリ
      if (parentDirectory != null && !parentDirectory.isDirectory ()) {  //親ディレクトリがない
        String parentName = parentDirectory.getPath ();
        if (parentDirectory.mkdirs ()) {  //親ディレクトリを作る。必要ならば遡って作る。作れた
          System.out.println (Multilingual.mlnJapanese ?
                              parentName + " を作りました" :
                              parentName + " was created");
        } else {  //作れなかった
          switch (rscShowError (Multilingual.mlnJapanese ?
                                parentName + " を作れません" :
                                parentName + " cannot be created",
                                RSC_A_MASK | RSC_R_MASK | RSC_I_MASK)) {
          case RSC_A_MASK:
            break retry;
          case RSC_R_MASK:
            continue retry;
          }
        }
      }
      if (fileTmp.isFile ()) {  //name.tmpがある。前回異常終了して残骸が残っている場合など
        if (!fileTmp.delete ()) {  //name.tmpを削除する。削除できない
          switch (rscShowError (Multilingual.mlnJapanese ?
                                nameTmp + " を削除できません" :
                                nameTmp + " cannot be deleted",
                                RSC_A_MASK | RSC_R_MASK | RSC_I_MASK)) {
          case RSC_A_MASK:
            break retry;
          case RSC_R_MASK:
            continue retry;
          }
        }
      }
      try (OutputStream os = name.toLowerCase ().endsWith (".gz") ?
           new GZIPOutputStream (new BufferedOutputStream (new FileOutputStream (fileTmp))) {
             {
               //def.setLevel (Deflater.BEST_COMPRESSION);
               def.setLevel (Deflater.DEFAULT_COMPRESSION);
               //def.setLevel (Deflater.BEST_SPEED);
             }
           } :
           new BufferedOutputStream (new FileOutputStream (fileTmp))) {  //name.tmpに書き出す
        //array[offset..offset+length-1]を書き出す
        os.write (array, offset, length);
        //longLength2-length個の0を書き出す
        //  RandomAccessFile.setLength()は拡張部分の内容が定義されていないので使えない
        if ((long) length < longLength2) {
          byte[] buffer = new byte[(int) Math.min (longLength2 - (long) length, (long) (1024 * 1024))];  //最大1MBずつ書き出す
          Arrays.fill (buffer, 0, buffer.length, (byte) 0);  //念の為
          long longPosition2 = (long) length;
          while (longPosition2 < longLength2) {
            int step = (int) Math.min ((long) buffer.length, longLength2 - longPosition2);
            os.write (buffer, 0, step);
            longPosition2 += (long) step;
          }
        }
      } catch (IOException ioe) {
        switch (rscShowError (Multilingual.mlnJapanese ?
                              nameTmp + " に書き出せません" :
                              nameTmp + " cannot be written",
                              RSC_A_MASK | RSC_R_MASK | RSC_I_MASK)) {
        case RSC_A_MASK:
          break retry;
        case RSC_R_MASK:
          continue retry;
        }
      }
      if (fileExists && file.isFile ()) {  //同じ名前で内容が異なるファイルがある。リトライでなくなった可能性があるので確認し直す
        if (fileBak.isFile ()) {  //name.bakがある
          if (!fileBak.delete ()) {  //name.bakを削除する。削除できない
            switch (rscShowError (Multilingual.mlnJapanese ?
                                  nameBak + " を削除できません" :
                                  nameBak + " cannot be deleted",
                                  RSC_A_MASK | RSC_R_MASK | RSC_I_MASK)) {
            case RSC_A_MASK:
              break retry;
            case RSC_R_MASK:
              continue retry;
            }
          }
        }
        if (!file.renameTo (fileBak)) {  //nameをname.bakにリネームする。リネームできない
          switch (rscShowError (Multilingual.mlnJapanese ?
                                name + " を " + nameBak + " にリネームできません" :
                                name + " cannot be renamed to " + nameBak,
                                RSC_A_MASK | RSC_R_MASK | RSC_I_MASK)) {
          case RSC_A_MASK:
            break retry;
          case RSC_R_MASK:
            continue retry;
          }
        }
      }
      if (fileTmp.renameTo (file)) {  //name.tmpをnameにリネームする。リネームできた
        if (fileExists) {  //同じ名前のファイルがあった
          System.out.println (Multilingual.mlnJapanese ?
                              name + " を更新しました" :
                              name + " was updated");
        } else {  //同じ名前のファイルがなかった
          System.out.println (Multilingual.mlnJapanese ?
                              name + " を作りました" :
                              name + " was created");
        }
        return true;  //成功
      } else {  //リネームできない
        switch (rscShowError (Multilingual.mlnJapanese ?
                              nameTmp + " を " + name + " にリネームできません" :
                              nameTmp + " cannot be renamed to " + name,
                              RSC_A_MASK | RSC_R_MASK | RSC_I_MASK)) {
        case RSC_A_MASK:
          break retry;
        case RSC_R_MASK:
          continue retry;
        }
      }
      break;
    }  //retry
    if (fileExists) {  //同じ名前のファイルがあった
      System.out.println (Multilingual.mlnJapanese ?
                          name + " を更新できません" :
                          name + " cannot be updated");
    } else {  //同じ名前のファイルがなかった
      System.out.println (Multilingual.mlnJapanese ?
                          name + " を作れません" :
                          name + " cannot be created");
    }
    return false;  //失敗
  }



  //========================================================================================
  //$$ISM InputStream

  public static final Pattern ISM_ZIP_SEPARATOR = Pattern.compile ("(?<=\\.(?:jar|zip))(?:/|\\\\)(?=.)", Pattern.CASE_INSENSITIVE);

  //in = ismOpen (name)
  //  InputStreamを開く
  //  InputStreamを返す
  //    失敗したときはnullを返す
  //  ZIPファイルの中のファイルを指定できる
  //    ZIPファイルの中のファイルはZipInputStreamで開く
  //    ZIPファイルの中のファイル名は{ZIPファイル名}/{ZIPファイルの中のファイル名}で指定する
  //    JARファイルもZIPファイルとして開くことができる
  //  GZIPで圧縮されているファイルを指定できる
  //    GZIPで圧縮されているファイルはGZIPInputStreamで開く
  public static InputStream ismOpen (String name) {
    InputStream in = null;
    in = ismOpen (name, false);  //ファイルを開く
    if (in == null && name.indexOf ('/') < 0 && name.indexOf ('\\') < 0) {  //ファイルがないとき
      in = ismOpen (name, true);  //リソースを開く
    }
    return in;
  }  //ismOpen(String)
  public static InputStream ismOpen (String name, boolean useGetResource) {
    boolean gzipped = name.toLowerCase ().endsWith (".gz");  //true=GZIPファイルが指定された
    String[] zipSplittedName = ISM_ZIP_SEPARATOR.split (name, 2);  //ZIPファイル名とZIPファイルの中のファイル名に分ける
    String fileName = zipSplittedName[0];  //通常のファイル名またはZIPファイル名
    String zipEntryName = zipSplittedName.length < 2 ? null : zipSplittedName[1];  //ZIPファイルの中のファイル名
    InputStream in = null;
    try {
      if (useGetResource) {  //getResourceを使うとき
        if (false) {
          URL url = XEiJ.class.getResource (fileName);
          if (url != null) {  //ファイルがある
            in = url.openStream ();
          }
        } else {
          in = XEiJ.class.getResourceAsStream (fileName);
        }
      } else {
        File file = new File (fileName);
        if (file.exists ()) {  //ファイルがある
          in = new FileInputStream (file);
        }
      }
      if (in != null && zipEntryName != null) {  //ZIPファイルの中のファイルが指定されたとき
        ZipInputStream zin = new ZipInputStream (in);
        in = null;
        ZipEntry entry;
        while ((entry = zin.getNextEntry ()) != null) {  //指定されたファイル名のエントリを探す
          if (zipEntryName.equals (entry.getName ())) {  //エントリが見つかった
            in = zin;
            break;
          }
        }
        if (in == null) {
          System.out.println (Multilingual.mlnJapanese ? fileName + " の中に " + zipEntryName + " がありません" :
                              zipEntryName + " does not exist in " + fileName);
        }
      }
      if (in != null && gzipped) {  //GZIPで圧縮されたファイルが指定されたとき
        in = new GZIPInputStream (in);
      }
      if (in != null) {
        System.out.println (Multilingual.mlnJapanese ? (useGetResource ? "リソースファイル " : "ファイル ") + name + " を読み込みます" :
                            (useGetResource ? "Reading resource file " : "Reading file ") + name);
        return new BufferedInputStream (in);
      }
    } catch (Exception ioe) {
      if (prgVerbose) {
        prgPrintStackTraceOf (ioe);
      }
    }
    System.out.println (Multilingual.mlnJapanese ? (useGetResource ? "リソースファイル " : "ファイル ") + name + " が見つかりません" :
                        (useGetResource ? "Resource file " : "File ") + name + " is not found");
    return null;  //失敗
  }  //ismOpen(String,boolean)

  //k = ismRead (in, bb, o, l)
  //  InputStreamからバイトバッファに読み込む
  //  読み込んだ長さを返す
  //    エラーのときは-1を返す
  //  指定されたサイズまたはファイルの末尾まで読み込む
  //    k=in.read(bb,o,l)は1回で指定されたサイズを読み込めるとは限らない
  //  ブロックされる可能性があるのでコアの動作中にコアのスレッドから呼ばないほうがよい
  public static int ismRead (InputStream in, byte[] bb, int o, int l) {
    try {
      int k = 0;
      while (k < l) {
        int t = in.read (bb, o + k, l - k);
        if (t < 0) {
          break;
        }
        k += t;
      }
      return k;
    } catch (IOException ioe) {
      if (prgVerbose) {
        prgPrintStackTraceOf (ioe);
      }
    }
    return -1;
  }  //ismRead(InputStream,byte[],int,int)

  //k = ismSkip (in, l)
  //  InputStreamを読み飛ばす
  //  読み飛ばした長さを返す
  //    エラーのときは-1を返す
  //  指定されたサイズまたはファイルの末尾まで読み飛ばす
  //    k=in.skip(l)は1回で指定されたサイズを読み飛ばせるとは限らない
  //  ブロックされる可能性があるのでコアの動作中にコアのスレッドから呼ばないほうがよい
  public static int ismSkip (InputStream in, int l) {
    try {
      int k = 0;
      while (k < l) {
        //skip(long)はファイルの末尾でなくても0を返す可能性があるのでファイルの末尾の判定はread()で行う
        //skip(long)する前に毎回read()しないとskip()がファイルの末尾で止まらなくなるらしい
        if (in.read () < 0) {
          break;
        }
        k++;
        if (k < l) {
          int t = (int) in.skip ((long) (l - k));
          if (t < 0) {
            break;
          }
          k += t;
        }
      }
      return k;
    } catch (IOException ioe) {
      if (prgVerbose) {
        prgPrintStackTraceOf (ioe);
      }
    }
    return -1;
  }  //ismSkip(InputStream,int)

  //ismClose (in)
  //  InputStreamを閉じる
  //  in==nullのときは何もしない
  //  in.close()でIOExceptionを無視するだけ
  public static void ismClose (InputStream in) {
    try {
      if (in != null) {
        in.close ();
      }
    } catch (IOException ioe) {
      if (prgVerbose) {
        prgPrintStackTraceOf (ioe);
      }
    }
  }  //ismClose(InputStream)

  //length = ismLength (name, maxLength)
  //  ファイルの長さを数える
  //  ZIPファイルの中のファイルを指定できる
  //  GZIPで圧縮されているファイルを指定できる
  //  -1  ファイルがない
  public static int ismLength (String name, int maxLength) {
    int length;
    InputStream in = ismOpen (name);
    if (in == null) {  //ファイルがない
      length = -1;
    } else {  //ファイルがある
      length = ismSkip (in, maxLength);
      ismClose (in);
    }
    return length;
  }  //ismLength(String,int)

  //success = ismLoad (bb, o, l, names)
  //  ファイルからバイトバッファに読み込む
  //  ファイル名を,で区切って複数指定できる
  //    先頭から順に指定されたサイズまで読み込めるファイルを探す
  //    1つでも読み込むことができればその時点で成功、1つも読み込めなければ失敗
  //  成功したときtrueを返す
  //  ZIPファイルの中のファイルを指定できる
  //    ZIPファイルの中のファイルはZipInputStreamで開く
  //    ZIPファイルの中のファイル名は{ZIPファイル名}/{ZIPファイルの中のファイル名}で指定する
  //    JARファイルもZIPファイルとして開くことができる
  //  GZIPで圧縮されているファイルを指定できる
  //    GZIPで圧縮されているファイルはGZIPInputStreamで開く
  //  ブロックされることがあるのでコアの動作中にコアのスレッドから呼ばないほうがよい
  public static boolean ismLoad (byte[] bb, int o, int l, String names) {
    for (String name : names.split (",")) {  //先頭から順に
      if (name.length () != 0) {  //ファイル名が指定されているとき
        InputStream in = ismOpen (name);  //開く
        if (in != null) {  //開けたら
          int k = ismRead (in, bb, o, l);  //読み込んで
          ismClose (in);  //閉じる
          if (k == l) {  //指定されたサイズまで読み込めたら
            return true;  //成功
          }
        }
      }
    }
    return false;  //1つも読み込めなかったので失敗
  }  //ismLoad(byte[],int,int,String)

  //success = ismSave (bb, offset, length, path, verbose)
  //  バイトバッファからファイルに書き出す
  //  出力範囲がバッファに収まっているとき
  //    ファイルが既に存在していてファイルサイズと内容が一致しているときは更新しない
  //  出力範囲がバッファに収まっていないとき
  //    バッファからはみ出した部分をゼロクリアする
  //    RandomAccessFileのsetLengthを使うとファイルサイズを簡単に変更できるが、ファイルを伸ばしたときに書き込まれる内容が仕様で定義されていない
  //    同じ手順で同じ内容のファイルができない可能性があるのは困るので明示的に0を書き込んでクリアする
  public static boolean ismSave (byte[] bb, int offset, long length, String path, boolean verbose) {
    if (ISM_ZIP_SEPARATOR.split (path, 2).length != 1) {  //ZIPファイルの中のファイル
      if (verbose) {
        pnlExitFullScreen (true);
        JOptionPane.showMessageDialog (null, Multilingual.mlnJapanese ? path + " に書き出せません" : "Cannot write " + path);
      }
      return false;
    }
    long step = 0;  //一度にゼロクリアする長さ。0=バッファに収まっている
    byte[] zz = null;  //ゼロクリア用の配列
    long pointer = (long) (bb.length - offset);  //バッファから出力できる長さ
    if (pointer < length) {  //バッファに収まっていない
      step = Math.min (1024L * 512, length - pointer);  //一度にゼロクリアする長さ。最大512KB
      zz = new byte[(int) step];  //ゼロクリア用の配列
      Arrays.fill (zz, (byte) 0);
    }
    //ファイル
    File file = new File (path);  //ファイル
    //ファイルが既に存在しているときはファイルサイズと内容が一致しているかどうか確認する
    if (step == 0 &&  //バッファに収まっている
        file.exists () && file.length () == length) {  //ファイルサイズが一致している
      //ファイルを読み込む
      if (length == 0L) {  //ファイルサイズが0で一致しているので成功
        return true;
      }
      InputStream in = ismOpen (path);
      if (in != null) {
        int l = (int) length;  //バッファに収まっているのだからintの範囲内
        byte[] tt = new byte[l];
        int k = ismRead (in, tt, 0, l);
        ismClose (in);
        if (k == l &&
            Arrays.equals (tt, bb.length == l ? bb : Arrays.copyOfRange (bb, offset, offset + l))) {  //内容が一致している
          return true;  //更新する必要がないので成功
        }
      }
    }  //check
    // *.tmpと*.bak
    String pathTmp = path + ".tmp";  // *.tmp
    String pathBak = path + ".bak";  // *.bak
    File fileTmp = new File (pathTmp);  // *.tmp
    File fileBak = new File (pathBak);  // *.bak
    // *.tmpを削除する
    if (fileTmp.exists ()) {  // *.tmpがあるとき
      if (!fileTmp.delete ()) {  // *.tmpを削除する
        if (verbose) {
          pnlExitFullScreen (true);
          JOptionPane.showMessageDialog (null, Multilingual.mlnJapanese ? pathTmp + " を削除できません" : "Cannot delete " + pathTmp);
        }
        return false;
      }
    }
    // *.tmpに書き出す
    try (OutputStream out = path.toLowerCase ().endsWith (".gz") ?  //pathの末尾が".gz"のときpathTmpの末尾は".gz.tmp"であることに注意
         new GZIPOutputStream (new BufferedOutputStream (new FileOutputStream (fileTmp))) {
           {
             //def.setLevel (Deflater.BEST_COMPRESSION);
             def.setLevel (Deflater.DEFAULT_COMPRESSION);
             //def.setLevel (Deflater.BEST_SPEED);
           }
         } :
         new BufferedOutputStream (new FileOutputStream (fileTmp))) {  //try-with-resourcesは1.7から
      if (step == 0) {  //バッファに収まっている
        out.write (bb, offset, (int) length);  //OutputStreamのwriteの返り値はvoid。エラーが出なければ1回で最後まで書き出される
      } else {  //バッファに収まっていない
        out.write (bb, offset, (int) pointer);  //バッファから出力できる範囲
        for (; pointer < length; pointer += step) {
          out.write (zz, 0, (int) Math.min (step, length - pointer));  //バッファから出力できない範囲
        }
      }
    } catch (IOException ioe) {
      if (verbose) {
        prgPrintStackTraceOf (ioe);
        pnlExitFullScreen (true);
        JOptionPane.showMessageDialog (null, Multilingual.mlnJapanese ? pathTmp + " に書き出せません" : "Cannot write " + pathTmp);
      }
      return false;
    }
    //ファイルを*.bakにリネームする
    //  javaのFileのrenameToはPerlと違って上書きしてくれないので明示的に削除またはリネームする必要がある
    if (file.exists ()) {  //ファイルがあるとき
      if (fileBak.exists ()) {  // *.bakがあるとき
        if (!fileBak.delete ()) {  // *.bakを削除する
          if (verbose) {
            pnlExitFullScreen (true);
            JOptionPane.showMessageDialog (null, Multilingual.mlnJapanese ? pathBak + " を削除できません" : "Cannot delete " + pathBak);
          }
          return false;
        }
      }
      if (!file.renameTo (fileBak)) {  //ファイルを*.bakにリネームする
        if (verbose) {
          pnlExitFullScreen (true);
          JOptionPane.showMessageDialog (
            null, Multilingual.mlnJapanese ? path + " を " + pathBak + " にリネームできません" : "Cannot rename " + path + " to " + pathBak);
        }
        return false;
      }
    }
    // *.tmpをファイルにリネームする
    //  javaのFileのrenameToはPerlと違って上書きしてくれないので明示的に削除またはリネームする必要がある
    if (!fileTmp.renameTo (file)) {  // *.tmpをファイルにリネームする
      if (verbose) {
        pnlExitFullScreen (true);
        JOptionPane.showMessageDialog (
          null, Multilingual.mlnJapanese ? pathTmp + " を " + path + " にリネームできません" : "Cannot rename " + pathTmp + " to " + path);
      }
      return false;
    }
    return true;
  }  //ismSave(byte[],int,long,String,boolean)



  //========================================================================================
  //$$FMT フォーマット変換
  //  Formatterは遅いので自前で展開する

  public static final char[] FMT_TEMP = new char[32];

  //--------------------------------------------------------------------------------
  //2進数変換
  //  ainNは'.'と'*'、binNは'0'と'1'に変換する
  //
  //  x          00 01
  //  x<<2       00 04
  //  x<<2&4     00 04
  //  x<<2&4^46  2e 2a
  //              .  *

  public static final char[] FMT_AIN4_BASE = ".......*..*...**.*...*.*.**..****...*..**.*.*.****..**.****.****".toCharArray ();
  public static final char[] FMT_BIN4_BASE = "0000000100100011010001010110011110001001101010111100110111101111".toCharArray ();

  //fmtAin4 (a, o, x)
  //fmtBin4 (a, o, x)
  //s = fmtAin4 (x)
  //s = fmtBin4 (x)
  //sb = fmtAin4 (sb, x)
  //sb = fmtBin4 (sb, x)
  //  4桁2進数変換
  public static void fmtAin4 (char[] a, int o, int x) {
    a[o     ] = (char) (x >>  1 & 4 ^ 46);
    a[o +  1] = (char) (x       & 4 ^ 46);
    a[o +  2] = (char) (x <<  1 & 4 ^ 46);
    a[o +  3] = (char) (x <<  2 & 4 ^ 46);
  }  //fmtAin4(char[],int,int)
  public static void fmtBin4 (char[] a, int o, int x) {
    a[o     ] = (char) (x >>>  3 & 1 | 48);
    a[o +  1] = (char) (x >>>  2 & 1 | 48);
    a[o +  2] = (char) (x >>>  1 & 1 | 48);
    a[o +  3] = (char) (x        & 1 | 48);
  }  //fmtBin4(char[],int,int)
  public static String fmtAin4 (int x) {
    return String.valueOf (FMT_AIN4_BASE, (x & 15) << 2, 4);
  }  //fmtAin4(int)
  public static String fmtBin4 (int x) {
    return String.valueOf (FMT_BIN4_BASE, (x & 15) << 2, 4);
  }  //fmtBin4(int)
  public static StringBuilder fmtAin4 (StringBuilder sb, int x) {
    return sb.append (FMT_AIN4_BASE, (x & 15) << 2, 6);
  }  //fmtAin4(StringBuilder,int)
  public static StringBuilder fmtBin4 (StringBuilder sb, int x) {
    return sb.append (FMT_BIN4_BASE, (x & 15) << 2, 6);
  }  //fmtBin4(StringBuilder,int)

  //fmtAin6 (a, o, x)
  //fmtBin6 (a, o, x)
  //s = fmtAin6 (x)
  //s = fmtBin6 (x)
  //sb = fmtAin6 (sb, x)
  //sb = fmtBin6 (sb, x)
  //  6桁2進数変換
  public static void fmtAin6 (char[] a, int o, int x) {
    a[o     ] = (char) (x >>  3 & 4 ^ 46);
    a[o +  1] = (char) (x >>  2 & 4 ^ 46);
    a[o +  2] = (char) (x >>  1 & 4 ^ 46);
    a[o +  3] = (char) (x       & 4 ^ 46);
    a[o +  4] = (char) (x <<  1 & 4 ^ 46);
    a[o +  5] = (char) (x <<  2 & 4 ^ 46);
  }  //fmtAin6(char[],int,int)
  public static void fmtBin6 (char[] a, int o, int x) {
    a[o     ] = (char) (x >>>  5 & 1 | 48);
    a[o +  1] = (char) (x >>>  4 & 1 | 48);
    a[o +  2] = (char) (x >>>  3 & 1 | 48);
    a[o +  3] = (char) (x >>>  2 & 1 | 48);
    a[o +  4] = (char) (x >>>  1 & 1 | 48);
    a[o +  5] = (char) (x        & 1 | 48);
  }  //fmtBin6(char[],int,int)
  public static String fmtAin6 (int x) {
    FMT_TEMP[ 0] = (char) (x >>  3 & 4 ^ 46);
    FMT_TEMP[ 1] = (char) (x >>  2 & 4 ^ 46);
    FMT_TEMP[ 2] = (char) (x >>  1 & 4 ^ 46);
    FMT_TEMP[ 3] = (char) (x       & 4 ^ 46);
    FMT_TEMP[ 4] = (char) (x <<  1 & 4 ^ 46);
    FMT_TEMP[ 5] = (char) (x <<  2 & 4 ^ 46);
    return String.valueOf (FMT_TEMP, 0, 6);
  }  //fmtAin6(int)
  public static String fmtBin6 (int x) {
    FMT_TEMP[ 0] = (char) (x >>>  5 & 1 | 48);
    FMT_TEMP[ 1] = (char) (x >>>  4 & 1 | 48);
    FMT_TEMP[ 2] = (char) (x >>>  3 & 1 | 48);
    FMT_TEMP[ 3] = (char) (x >>>  2 & 1 | 48);
    FMT_TEMP[ 4] = (char) (x >>>  1 & 1 | 48);
    FMT_TEMP[ 5] = (char) (x        & 1 | 48);
    return String.valueOf (FMT_TEMP, 0, 6);
  }  //fmtBin6(int)
  public static StringBuilder fmtAin6 (StringBuilder sb, int x) {
    FMT_TEMP[ 0] = (char) (x >>  3 & 4 ^ 46);
    FMT_TEMP[ 1] = (char) (x >>  2 & 4 ^ 46);
    FMT_TEMP[ 2] = (char) (x >>  1 & 4 ^ 46);
    FMT_TEMP[ 3] = (char) (x       & 4 ^ 46);
    FMT_TEMP[ 4] = (char) (x <<  1 & 4 ^ 46);
    FMT_TEMP[ 5] = (char) (x <<  2 & 4 ^ 46);
    return sb.append (FMT_TEMP, 0, 6);
  }  //fmtAin6(StringBuilder,int)
  public static StringBuilder fmtBin6 (StringBuilder sb, int x) {
    FMT_TEMP[ 0] = (char) (x >>>  5 & 1 | 48);
    FMT_TEMP[ 1] = (char) (x >>>  4 & 1 | 48);
    FMT_TEMP[ 2] = (char) (x >>>  3 & 1 | 48);
    FMT_TEMP[ 3] = (char) (x >>>  2 & 1 | 48);
    FMT_TEMP[ 4] = (char) (x >>>  1 & 1 | 48);
    FMT_TEMP[ 5] = (char) (x        & 1 | 48);
    return sb.append (FMT_TEMP, 0, 6);
  }  //fmtBin6(StringBuilder,int)

  //fmtAin8 (a, o, x)
  //fmtBin8 (a, o, x)
  //s = fmtAin8 (x)
  //s = fmtBin8 (x)
  //sb = fmtAin8 (sb, x)
  //sb = fmtBin8 (sb, x)
  //  8桁2進数変換
  public static void fmtAin8 (char[] a, int o, int x) {
    a[o     ] = (char) (x >>  5 & 4 ^ 46);
    a[o +  1] = (char) (x >>  4 & 4 ^ 46);
    a[o +  2] = (char) (x >>  3 & 4 ^ 46);
    a[o +  3] = (char) (x >>  2 & 4 ^ 46);
    a[o +  4] = (char) (x >>  1 & 4 ^ 46);
    a[o +  5] = (char) (x       & 4 ^ 46);
    a[o +  6] = (char) (x <<  1 & 4 ^ 46);
    a[o +  7] = (char) (x <<  2 & 4 ^ 46);
  }  //fmtAin8(char[],int,int)
  public static void fmtBin8 (char[] a, int o, int x) {
    a[o     ] = (char) (x >>>  7 & 1 | 48);
    a[o +  1] = (char) (x >>>  6 & 1 | 48);
    a[o +  2] = (char) (x >>>  5 & 1 | 48);
    a[o +  3] = (char) (x >>>  4 & 1 | 48);
    a[o +  4] = (char) (x >>>  3 & 1 | 48);
    a[o +  5] = (char) (x >>>  2 & 1 | 48);
    a[o +  6] = (char) (x >>>  1 & 1 | 48);
    a[o +  7] = (char) (x        & 1 | 48);
  }  //fmtBin8(char[],int,int)
  public static String fmtAin8 (int x) {
    FMT_TEMP[ 0] = (char) (x >>  5 & 4 ^ 46);
    FMT_TEMP[ 1] = (char) (x >>  4 & 4 ^ 46);
    FMT_TEMP[ 2] = (char) (x >>  3 & 4 ^ 46);
    FMT_TEMP[ 3] = (char) (x >>  2 & 4 ^ 46);
    FMT_TEMP[ 4] = (char) (x >>  1 & 4 ^ 46);
    FMT_TEMP[ 5] = (char) (x       & 4 ^ 46);
    FMT_TEMP[ 6] = (char) (x <<  1 & 4 ^ 46);
    FMT_TEMP[ 7] = (char) (x <<  2 & 4 ^ 46);
    return String.valueOf (FMT_TEMP, 0, 8);
  }  //fmtAin8(int)
  public static String fmtBin8 (int x) {
    FMT_TEMP[ 0] = (char) (x >>>  7 & 1 | 48);
    FMT_TEMP[ 1] = (char) (x >>>  6 & 1 | 48);
    FMT_TEMP[ 2] = (char) (x >>>  5 & 1 | 48);
    FMT_TEMP[ 3] = (char) (x >>>  4 & 1 | 48);
    FMT_TEMP[ 4] = (char) (x >>>  3 & 1 | 48);
    FMT_TEMP[ 5] = (char) (x >>>  2 & 1 | 48);
    FMT_TEMP[ 6] = (char) (x >>>  1 & 1 | 48);
    FMT_TEMP[ 7] = (char) (x        & 1 | 48);
    return String.valueOf (FMT_TEMP, 0, 8);
  }  //fmtBin8(int)
  public static StringBuilder fmtAin8 (StringBuilder sb, int x) {
    FMT_TEMP[ 0] = (char) (x >>  5 & 4 ^ 46);
    FMT_TEMP[ 1] = (char) (x >>  4 & 4 ^ 46);
    FMT_TEMP[ 2] = (char) (x >>  3 & 4 ^ 46);
    FMT_TEMP[ 3] = (char) (x >>  2 & 4 ^ 46);
    FMT_TEMP[ 4] = (char) (x >>  1 & 4 ^ 46);
    FMT_TEMP[ 5] = (char) (x       & 4 ^ 46);
    FMT_TEMP[ 6] = (char) (x <<  1 & 4 ^ 46);
    FMT_TEMP[ 7] = (char) (x <<  2 & 4 ^ 46);
    return sb.append (FMT_TEMP, 0, 8);
  }  //fmtAin8(StringBuilder,int)
  public static StringBuilder fmtBin8 (StringBuilder sb, int x) {
    FMT_TEMP[ 0] = (char) (x >>>  7 & 1 | 48);
    FMT_TEMP[ 1] = (char) (x >>>  6 & 1 | 48);
    FMT_TEMP[ 2] = (char) (x >>>  5 & 1 | 48);
    FMT_TEMP[ 3] = (char) (x >>>  4 & 1 | 48);
    FMT_TEMP[ 4] = (char) (x >>>  3 & 1 | 48);
    FMT_TEMP[ 5] = (char) (x >>>  2 & 1 | 48);
    FMT_TEMP[ 6] = (char) (x >>>  1 & 1 | 48);
    FMT_TEMP[ 7] = (char) (x        & 1 | 48);
    return sb.append (FMT_TEMP, 0, 8);
  }  //fmtBin8(StringBuilder,int)

  //fmtAin12 (a, o, x)
  //fmtBin12 (a, o, x)
  //s = fmtAin12 (x)
  //s = fmtBin12 (x)
  //sb = fmtAin12 (sb, x)
  //sb = fmtBin12 (sb, x)
  //  12桁2進数変換
  public static void fmtAin12 (char[] a, int o, int x) {
    a[o     ] = (char) (x >>  9 & 4 ^ 46);
    a[o +  1] = (char) (x >>  8 & 4 ^ 46);
    a[o +  2] = (char) (x >>  7 & 4 ^ 46);
    a[o +  3] = (char) (x >>  6 & 4 ^ 46);
    a[o +  4] = (char) (x >>  5 & 4 ^ 46);
    a[o +  5] = (char) (x >>  4 & 4 ^ 46);
    a[o +  6] = (char) (x >>  3 & 4 ^ 46);
    a[o +  7] = (char) (x >>  2 & 4 ^ 46);
    a[o +  8] = (char) (x >>  1 & 4 ^ 46);
    a[o +  9] = (char) (x       & 4 ^ 46);
    a[o + 10] = (char) (x <<  1 & 4 ^ 46);
    a[o + 11] = (char) (x <<  2 & 4 ^ 46);
  }  //fmtAin12(char[],int,int)
  public static void fmtBin12 (char[] a, int o, int x) {
    a[o     ] = (char) (x >>> 11 & 1 | 48);
    a[o +  1] = (char) (x >>> 10 & 1 | 48);
    a[o +  2] = (char) (x >>>  9 & 1 | 48);
    a[o +  3] = (char) (x >>>  8 & 1 | 48);
    a[o +  4] = (char) (x >>>  7 & 1 | 48);
    a[o +  5] = (char) (x >>>  6 & 1 | 48);
    a[o +  6] = (char) (x >>>  5 & 1 | 48);
    a[o +  7] = (char) (x >>>  4 & 1 | 48);
    a[o +  8] = (char) (x >>>  3 & 1 | 48);
    a[o +  9] = (char) (x >>>  2 & 1 | 48);
    a[o + 10] = (char) (x >>>  1 & 1 | 48);
    a[o + 11] = (char) (x        & 1 | 48);
  }  //fmtBin12(char[],int,int)
  public static String fmtAin12 (int x) {
    FMT_TEMP[ 0] = (char) (x >>  9 & 4 ^ 46);
    FMT_TEMP[ 1] = (char) (x >>  8 & 4 ^ 46);
    FMT_TEMP[ 2] = (char) (x >>  7 & 4 ^ 46);
    FMT_TEMP[ 3] = (char) (x >>  6 & 4 ^ 46);
    FMT_TEMP[ 4] = (char) (x >>  5 & 4 ^ 46);
    FMT_TEMP[ 5] = (char) (x >>  4 & 4 ^ 46);
    FMT_TEMP[ 6] = (char) (x >>  3 & 4 ^ 46);
    FMT_TEMP[ 7] = (char) (x >>  2 & 4 ^ 46);
    FMT_TEMP[ 8] = (char) (x >>  1 & 4 ^ 46);
    FMT_TEMP[ 9] = (char) (x       & 4 ^ 46);
    FMT_TEMP[10] = (char) (x <<  1 & 4 ^ 46);
    FMT_TEMP[11] = (char) (x <<  2 & 4 ^ 46);
    return String.valueOf (FMT_TEMP, 0, 12);
  }  //fmtAin12(int)
  public static String fmtBin12 (int x) {
    FMT_TEMP[ 0] = (char) (x >>> 11 & 1 | 48);
    FMT_TEMP[ 1] = (char) (x >>> 10 & 1 | 48);
    FMT_TEMP[ 2] = (char) (x >>>  9 & 1 | 48);
    FMT_TEMP[ 3] = (char) (x >>>  8 & 1 | 48);
    FMT_TEMP[ 4] = (char) (x >>>  7 & 1 | 48);
    FMT_TEMP[ 5] = (char) (x >>>  6 & 1 | 48);
    FMT_TEMP[ 6] = (char) (x >>>  5 & 1 | 48);
    FMT_TEMP[ 7] = (char) (x >>>  4 & 1 | 48);
    FMT_TEMP[ 8] = (char) (x >>>  3 & 1 | 48);
    FMT_TEMP[ 9] = (char) (x >>>  2 & 1 | 48);
    FMT_TEMP[10] = (char) (x >>>  1 & 1 | 48);
    FMT_TEMP[11] = (char) (x        & 1 | 48);
    return String.valueOf (FMT_TEMP, 0, 12);
  }  //fmtBin12(int)
  public static StringBuilder fmtAin12 (StringBuilder sb, int x) {
    FMT_TEMP[ 0] = (char) (x >>  9 & 4 ^ 46);
    FMT_TEMP[ 1] = (char) (x >>  8 & 4 ^ 46);
    FMT_TEMP[ 2] = (char) (x >>  7 & 4 ^ 46);
    FMT_TEMP[ 3] = (char) (x >>  6 & 4 ^ 46);
    FMT_TEMP[ 4] = (char) (x >>  5 & 4 ^ 46);
    FMT_TEMP[ 5] = (char) (x >>  4 & 4 ^ 46);
    FMT_TEMP[ 6] = (char) (x >>  3 & 4 ^ 46);
    FMT_TEMP[ 7] = (char) (x >>  2 & 4 ^ 46);
    FMT_TEMP[ 8] = (char) (x >>  1 & 4 ^ 46);
    FMT_TEMP[ 9] = (char) (x       & 4 ^ 46);
    FMT_TEMP[10] = (char) (x <<  1 & 4 ^ 46);
    FMT_TEMP[11] = (char) (x <<  2 & 4 ^ 46);
    return sb.append (FMT_TEMP, 0, 12);
  }  //fmtAin12(StringBuilder,int)
  public static StringBuilder fmtBin12 (StringBuilder sb, int x) {
    FMT_TEMP[ 0] = (char) (x >>> 11 & 1 | 48);
    FMT_TEMP[ 1] = (char) (x >>> 10 & 1 | 48);
    FMT_TEMP[ 2] = (char) (x >>>  9 & 1 | 48);
    FMT_TEMP[ 3] = (char) (x >>>  8 & 1 | 48);
    FMT_TEMP[ 4] = (char) (x >>>  7 & 1 | 48);
    FMT_TEMP[ 5] = (char) (x >>>  6 & 1 | 48);
    FMT_TEMP[ 6] = (char) (x >>>  5 & 1 | 48);
    FMT_TEMP[ 7] = (char) (x >>>  4 & 1 | 48);
    FMT_TEMP[ 8] = (char) (x >>>  3 & 1 | 48);
    FMT_TEMP[ 9] = (char) (x >>>  2 & 1 | 48);
    FMT_TEMP[10] = (char) (x >>>  1 & 1 | 48);
    FMT_TEMP[11] = (char) (x        & 1 | 48);
    return sb.append (FMT_TEMP, 0, 12);
  }  //fmtBin12(StringBuilder,int)

  //fmtAin16 (a, o, x)
  //fmtBin16 (a, o, x)
  //s = fmtAin16 (x)
  //s = fmtBin16 (x)
  //sb = fmtAin16 (sb, x)
  //sb = fmtBin16 (sb, x)
  //  16桁2進数変換
  public static void fmtAin16 (char[] a, int o, int x) {
    a[o     ] = (char) (x >> 13 & 4 ^ 46);
    a[o +  1] = (char) (x >> 12 & 4 ^ 46);
    a[o +  2] = (char) (x >> 11 & 4 ^ 46);
    a[o +  3] = (char) (x >> 10 & 4 ^ 46);
    a[o +  4] = (char) (x >>  9 & 4 ^ 46);
    a[o +  5] = (char) (x >>  8 & 4 ^ 46);
    a[o +  6] = (char) (x >>  7 & 4 ^ 46);
    a[o +  7] = (char) (x >>  6 & 4 ^ 46);
    a[o +  8] = (char) (x >>  5 & 4 ^ 46);
    a[o +  9] = (char) (x >>  4 & 4 ^ 46);
    a[o + 10] = (char) (x >>  3 & 4 ^ 46);
    a[o + 11] = (char) (x >>  2 & 4 ^ 46);
    a[o + 12] = (char) (x >>  1 & 4 ^ 46);
    a[o + 13] = (char) (x       & 4 ^ 46);
    a[o + 14] = (char) (x <<  1 & 4 ^ 46);
    a[o + 15] = (char) (x <<  2 & 4 ^ 46);
  }  //fmtAin16(char[],int,int)
  public static void fmtBin16 (char[] a, int o, int x) {
    a[o     ] = (char) (x >>> 15 & 1 | 48);
    a[o +  1] = (char) (x >>> 14 & 1 | 48);
    a[o +  2] = (char) (x >>> 13 & 1 | 48);
    a[o +  3] = (char) (x >>> 12 & 1 | 48);
    a[o +  4] = (char) (x >>> 11 & 1 | 48);
    a[o +  5] = (char) (x >>> 10 & 1 | 48);
    a[o +  6] = (char) (x >>>  9 & 1 | 48);
    a[o +  7] = (char) (x >>>  8 & 1 | 48);
    a[o +  8] = (char) (x >>>  7 & 1 | 48);
    a[o +  9] = (char) (x >>>  6 & 1 | 48);
    a[o + 10] = (char) (x >>>  5 & 1 | 48);
    a[o + 11] = (char) (x >>>  4 & 1 | 48);
    a[o + 12] = (char) (x >>>  3 & 1 | 48);
    a[o + 13] = (char) (x >>>  2 & 1 | 48);
    a[o + 14] = (char) (x >>>  1 & 1 | 48);
    a[o + 15] = (char) (x        & 1 | 48);
  }  //fmtBin16(char[],int,int)
  public static String fmtAin16 (int x) {
    FMT_TEMP[ 0] = (char) (x >> 13 & 4 ^ 46);
    FMT_TEMP[ 1] = (char) (x >> 12 & 4 ^ 46);
    FMT_TEMP[ 2] = (char) (x >> 11 & 4 ^ 46);
    FMT_TEMP[ 3] = (char) (x >> 10 & 4 ^ 46);
    FMT_TEMP[ 4] = (char) (x >>  9 & 4 ^ 46);
    FMT_TEMP[ 5] = (char) (x >>  8 & 4 ^ 46);
    FMT_TEMP[ 6] = (char) (x >>  7 & 4 ^ 46);
    FMT_TEMP[ 7] = (char) (x >>  6 & 4 ^ 46);
    FMT_TEMP[ 8] = (char) (x >>  5 & 4 ^ 46);
    FMT_TEMP[ 9] = (char) (x >>  4 & 4 ^ 46);
    FMT_TEMP[10] = (char) (x >>  3 & 4 ^ 46);
    FMT_TEMP[11] = (char) (x >>  2 & 4 ^ 46);
    FMT_TEMP[12] = (char) (x >>  1 & 4 ^ 46);
    FMT_TEMP[13] = (char) (x       & 4 ^ 46);
    FMT_TEMP[14] = (char) (x <<  1 & 4 ^ 46);
    FMT_TEMP[15] = (char) (x <<  2 & 4 ^ 46);
    return String.valueOf (FMT_TEMP, 0, 16);
  }  //fmtAin16(int)
  public static String fmtBin16 (int x) {
    FMT_TEMP[ 0] = (char) (x >>> 15 & 1 | 48);
    FMT_TEMP[ 1] = (char) (x >>> 14 & 1 | 48);
    FMT_TEMP[ 2] = (char) (x >>> 13 & 1 | 48);
    FMT_TEMP[ 3] = (char) (x >>> 12 & 1 | 48);
    FMT_TEMP[ 4] = (char) (x >>> 11 & 1 | 48);
    FMT_TEMP[ 5] = (char) (x >>> 10 & 1 | 48);
    FMT_TEMP[ 6] = (char) (x >>>  9 & 1 | 48);
    FMT_TEMP[ 7] = (char) (x >>>  8 & 1 | 48);
    FMT_TEMP[ 8] = (char) (x >>>  7 & 1 | 48);
    FMT_TEMP[ 9] = (char) (x >>>  6 & 1 | 48);
    FMT_TEMP[10] = (char) (x >>>  5 & 1 | 48);
    FMT_TEMP[11] = (char) (x >>>  4 & 1 | 48);
    FMT_TEMP[12] = (char) (x >>>  3 & 1 | 48);
    FMT_TEMP[13] = (char) (x >>>  2 & 1 | 48);
    FMT_TEMP[14] = (char) (x >>>  1 & 1 | 48);
    FMT_TEMP[15] = (char) (x        & 1 | 48);
    return String.valueOf (FMT_TEMP, 0, 16);
  }  //fmtBin16(int)
  public static StringBuilder fmtAin16 (StringBuilder sb, int x) {
    FMT_TEMP[ 0] = (char) (x >> 13 & 4 ^ 46);
    FMT_TEMP[ 1] = (char) (x >> 12 & 4 ^ 46);
    FMT_TEMP[ 2] = (char) (x >> 11 & 4 ^ 46);
    FMT_TEMP[ 3] = (char) (x >> 10 & 4 ^ 46);
    FMT_TEMP[ 4] = (char) (x >>  9 & 4 ^ 46);
    FMT_TEMP[ 5] = (char) (x >>  8 & 4 ^ 46);
    FMT_TEMP[ 6] = (char) (x >>  7 & 4 ^ 46);
    FMT_TEMP[ 7] = (char) (x >>  6 & 4 ^ 46);
    FMT_TEMP[ 8] = (char) (x >>  5 & 4 ^ 46);
    FMT_TEMP[ 9] = (char) (x >>  4 & 4 ^ 46);
    FMT_TEMP[10] = (char) (x >>  3 & 4 ^ 46);
    FMT_TEMP[11] = (char) (x >>  2 & 4 ^ 46);
    FMT_TEMP[12] = (char) (x >>  1 & 4 ^ 46);
    FMT_TEMP[13] = (char) (x       & 4 ^ 46);
    FMT_TEMP[14] = (char) (x <<  1 & 4 ^ 46);
    FMT_TEMP[15] = (char) (x <<  2 & 4 ^ 46);
    return sb.append (FMT_TEMP, 0, 16);
  }  //fmtAin16(StringBuilder,int)
  public static StringBuilder fmtBin16 (StringBuilder sb, int x) {
    FMT_TEMP[ 0] = (char) (x >>> 15 & 1 | 48);
    FMT_TEMP[ 1] = (char) (x >>> 14 & 1 | 48);
    FMT_TEMP[ 2] = (char) (x >>> 13 & 1 | 48);
    FMT_TEMP[ 3] = (char) (x >>> 12 & 1 | 48);
    FMT_TEMP[ 4] = (char) (x >>> 11 & 1 | 48);
    FMT_TEMP[ 5] = (char) (x >>> 10 & 1 | 48);
    FMT_TEMP[ 6] = (char) (x >>>  9 & 1 | 48);
    FMT_TEMP[ 7] = (char) (x >>>  8 & 1 | 48);
    FMT_TEMP[ 8] = (char) (x >>>  7 & 1 | 48);
    FMT_TEMP[ 9] = (char) (x >>>  6 & 1 | 48);
    FMT_TEMP[10] = (char) (x >>>  5 & 1 | 48);
    FMT_TEMP[11] = (char) (x >>>  4 & 1 | 48);
    FMT_TEMP[12] = (char) (x >>>  3 & 1 | 48);
    FMT_TEMP[13] = (char) (x >>>  2 & 1 | 48);
    FMT_TEMP[14] = (char) (x >>>  1 & 1 | 48);
    FMT_TEMP[15] = (char) (x        & 1 | 48);
    return sb.append (FMT_TEMP, 0, 16);
  }  //fmtBin16(StringBuilder,int)

  //fmtAin24 (a, o, x)
  //fmtBin24 (a, o, x)
  //s = fmtAin24 (x)
  //s = fmtBin24 (x)
  //sb = fmtAin24 (sb, x)
  //sb = fmtBin24 (sb, x)
  //  24桁2進数変換
  public static void fmtAin24 (char[] a, int o, int x) {
    a[o     ] = (char) (x >> 21 & 4 ^ 46);
    a[o +  1] = (char) (x >> 20 & 4 ^ 46);
    a[o +  2] = (char) (x >> 19 & 4 ^ 46);
    a[o +  3] = (char) (x >> 18 & 4 ^ 46);
    a[o +  4] = (char) (x >> 17 & 4 ^ 46);
    a[o +  5] = (char) (x >> 16 & 4 ^ 46);
    a[o +  6] = (char) (x >> 15 & 4 ^ 46);
    a[o +  7] = (char) (x >> 14 & 4 ^ 46);
    a[o +  8] = (char) (x >> 13 & 4 ^ 46);
    a[o +  9] = (char) (x >> 12 & 4 ^ 46);
    a[o + 10] = (char) (x >> 11 & 4 ^ 46);
    a[o + 11] = (char) (x >> 10 & 4 ^ 46);
    a[o + 12] = (char) (x >>  9 & 4 ^ 46);
    a[o + 13] = (char) (x >>  8 & 4 ^ 46);
    a[o + 14] = (char) (x >>  7 & 4 ^ 46);
    a[o + 15] = (char) (x >>  6 & 4 ^ 46);
    a[o + 16] = (char) (x >>  5 & 4 ^ 46);
    a[o + 17] = (char) (x >>  4 & 4 ^ 46);
    a[o + 18] = (char) (x >>  3 & 4 ^ 46);
    a[o + 19] = (char) (x >>  2 & 4 ^ 46);
    a[o + 20] = (char) (x >>  1 & 4 ^ 46);
    a[o + 21] = (char) (x       & 4 ^ 46);
    a[o + 22] = (char) (x <<  1 & 4 ^ 46);
    a[o + 23] = (char) (x <<  2 & 4 ^ 46);
  }  //fmtAin24(char[],int,int)
  public static void fmtBin24 (char[] a, int o, int x) {
    a[o     ] = (char) (x >>> 23 & 1 | 48);
    a[o +  1] = (char) (x >>> 22 & 1 | 48);
    a[o +  2] = (char) (x >>> 21 & 1 | 48);
    a[o +  3] = (char) (x >>> 20 & 1 | 48);
    a[o +  4] = (char) (x >>> 19 & 1 | 48);
    a[o +  5] = (char) (x >>> 18 & 1 | 48);
    a[o +  6] = (char) (x >>> 17 & 1 | 48);
    a[o +  7] = (char) (x >>> 16 & 1 | 48);
    a[o +  8] = (char) (x >>> 15 & 1 | 48);
    a[o +  9] = (char) (x >>> 14 & 1 | 48);
    a[o + 10] = (char) (x >>> 13 & 1 | 48);
    a[o + 11] = (char) (x >>> 12 & 1 | 48);
    a[o + 12] = (char) (x >>> 11 & 1 | 48);
    a[o + 13] = (char) (x >>> 10 & 1 | 48);
    a[o + 14] = (char) (x >>>  9 & 1 | 48);
    a[o + 15] = (char) (x >>>  8 & 1 | 48);
    a[o + 16] = (char) (x >>>  7 & 1 | 48);
    a[o + 17] = (char) (x >>>  6 & 1 | 48);
    a[o + 18] = (char) (x >>>  5 & 1 | 48);
    a[o + 19] = (char) (x >>>  4 & 1 | 48);
    a[o + 20] = (char) (x >>>  3 & 1 | 48);
    a[o + 21] = (char) (x >>>  2 & 1 | 48);
    a[o + 22] = (char) (x >>>  1 & 1 | 48);
    a[o + 23] = (char) (x        & 1 | 48);
  }  //fmtBin24(char[],int,int)
  public static String fmtAin24 (int x) {
    FMT_TEMP[ 0] = (char) (x >> 21 & 4 ^ 46);
    FMT_TEMP[ 1] = (char) (x >> 20 & 4 ^ 46);
    FMT_TEMP[ 2] = (char) (x >> 19 & 4 ^ 46);
    FMT_TEMP[ 3] = (char) (x >> 18 & 4 ^ 46);
    FMT_TEMP[ 4] = (char) (x >> 17 & 4 ^ 46);
    FMT_TEMP[ 5] = (char) (x >> 16 & 4 ^ 46);
    FMT_TEMP[ 6] = (char) (x >> 15 & 4 ^ 46);
    FMT_TEMP[ 7] = (char) (x >> 14 & 4 ^ 46);
    FMT_TEMP[ 8] = (char) (x >> 13 & 4 ^ 46);
    FMT_TEMP[ 9] = (char) (x >> 12 & 4 ^ 46);
    FMT_TEMP[10] = (char) (x >> 11 & 4 ^ 46);
    FMT_TEMP[11] = (char) (x >> 10 & 4 ^ 46);
    FMT_TEMP[12] = (char) (x >>  9 & 4 ^ 46);
    FMT_TEMP[13] = (char) (x >>  8 & 4 ^ 46);
    FMT_TEMP[14] = (char) (x >>  7 & 4 ^ 46);
    FMT_TEMP[15] = (char) (x >>  6 & 4 ^ 46);
    FMT_TEMP[16] = (char) (x >>  5 & 4 ^ 46);
    FMT_TEMP[17] = (char) (x >>  4 & 4 ^ 46);
    FMT_TEMP[18] = (char) (x >>  3 & 4 ^ 46);
    FMT_TEMP[19] = (char) (x >>  2 & 4 ^ 46);
    FMT_TEMP[20] = (char) (x >>  1 & 4 ^ 46);
    FMT_TEMP[21] = (char) (x       & 4 ^ 46);
    FMT_TEMP[22] = (char) (x <<  1 & 4 ^ 46);
    FMT_TEMP[23] = (char) (x <<  2 & 4 ^ 46);
    return String.valueOf (FMT_TEMP, 0, 24);
  }  //fmtAin24(int)
  public static String fmtBin24 (int x) {
    FMT_TEMP[ 0] = (char) (x >>> 23 & 1 | 48);
    FMT_TEMP[ 1] = (char) (x >>> 22 & 1 | 48);
    FMT_TEMP[ 2] = (char) (x >>> 21 & 1 | 48);
    FMT_TEMP[ 3] = (char) (x >>> 20 & 1 | 48);
    FMT_TEMP[ 4] = (char) (x >>> 19 & 1 | 48);
    FMT_TEMP[ 5] = (char) (x >>> 18 & 1 | 48);
    FMT_TEMP[ 6] = (char) (x >>> 17 & 1 | 48);
    FMT_TEMP[ 7] = (char) (x >>> 16 & 1 | 48);
    FMT_TEMP[ 8] = (char) (x >>> 15 & 1 | 48);
    FMT_TEMP[ 9] = (char) (x >>> 14 & 1 | 48);
    FMT_TEMP[10] = (char) (x >>> 13 & 1 | 48);
    FMT_TEMP[11] = (char) (x >>> 12 & 1 | 48);
    FMT_TEMP[12] = (char) (x >>> 11 & 1 | 48);
    FMT_TEMP[13] = (char) (x >>> 10 & 1 | 48);
    FMT_TEMP[14] = (char) (x >>>  9 & 1 | 48);
    FMT_TEMP[15] = (char) (x >>>  8 & 1 | 48);
    FMT_TEMP[16] = (char) (x >>>  7 & 1 | 48);
    FMT_TEMP[17] = (char) (x >>>  6 & 1 | 48);
    FMT_TEMP[18] = (char) (x >>>  5 & 1 | 48);
    FMT_TEMP[19] = (char) (x >>>  4 & 1 | 48);
    FMT_TEMP[20] = (char) (x >>>  3 & 1 | 48);
    FMT_TEMP[21] = (char) (x >>>  2 & 1 | 48);
    FMT_TEMP[22] = (char) (x >>>  1 & 1 | 48);
    FMT_TEMP[23] = (char) (x        & 1 | 48);
    return String.valueOf (FMT_TEMP, 0, 24);
  }  //fmtBin24(int)
  public static StringBuilder fmtAin24 (StringBuilder sb, int x) {
    FMT_TEMP[ 0] = (char) (x >> 21 & 4 ^ 46);
    FMT_TEMP[ 1] = (char) (x >> 20 & 4 ^ 46);
    FMT_TEMP[ 2] = (char) (x >> 19 & 4 ^ 46);
    FMT_TEMP[ 3] = (char) (x >> 18 & 4 ^ 46);
    FMT_TEMP[ 4] = (char) (x >> 17 & 4 ^ 46);
    FMT_TEMP[ 5] = (char) (x >> 16 & 4 ^ 46);
    FMT_TEMP[ 6] = (char) (x >> 15 & 4 ^ 46);
    FMT_TEMP[ 7] = (char) (x >> 14 & 4 ^ 46);
    FMT_TEMP[ 8] = (char) (x >> 13 & 4 ^ 46);
    FMT_TEMP[ 9] = (char) (x >> 12 & 4 ^ 46);
    FMT_TEMP[10] = (char) (x >> 11 & 4 ^ 46);
    FMT_TEMP[11] = (char) (x >> 10 & 4 ^ 46);
    FMT_TEMP[12] = (char) (x >>  9 & 4 ^ 46);
    FMT_TEMP[13] = (char) (x >>  8 & 4 ^ 46);
    FMT_TEMP[14] = (char) (x >>  7 & 4 ^ 46);
    FMT_TEMP[15] = (char) (x >>  6 & 4 ^ 46);
    FMT_TEMP[16] = (char) (x >>  5 & 4 ^ 46);
    FMT_TEMP[17] = (char) (x >>  4 & 4 ^ 46);
    FMT_TEMP[18] = (char) (x >>  3 & 4 ^ 46);
    FMT_TEMP[19] = (char) (x >>  2 & 4 ^ 46);
    FMT_TEMP[20] = (char) (x >>  1 & 4 ^ 46);
    FMT_TEMP[21] = (char) (x       & 4 ^ 46);
    FMT_TEMP[22] = (char) (x <<  1 & 4 ^ 46);
    FMT_TEMP[23] = (char) (x <<  2 & 4 ^ 46);
    return sb.append (FMT_TEMP, 0, 24);
  }  //fmtAin24(StringBuilder,int)
  public static StringBuilder fmtBin24 (StringBuilder sb, int x) {
    FMT_TEMP[ 0] = (char) (x >>> 23 & 1 | 48);
    FMT_TEMP[ 1] = (char) (x >>> 22 & 1 | 48);
    FMT_TEMP[ 2] = (char) (x >>> 21 & 1 | 48);
    FMT_TEMP[ 3] = (char) (x >>> 20 & 1 | 48);
    FMT_TEMP[ 4] = (char) (x >>> 19 & 1 | 48);
    FMT_TEMP[ 5] = (char) (x >>> 18 & 1 | 48);
    FMT_TEMP[ 6] = (char) (x >>> 17 & 1 | 48);
    FMT_TEMP[ 7] = (char) (x >>> 16 & 1 | 48);
    FMT_TEMP[ 8] = (char) (x >>> 15 & 1 | 48);
    FMT_TEMP[ 9] = (char) (x >>> 14 & 1 | 48);
    FMT_TEMP[10] = (char) (x >>> 13 & 1 | 48);
    FMT_TEMP[11] = (char) (x >>> 12 & 1 | 48);
    FMT_TEMP[12] = (char) (x >>> 11 & 1 | 48);
    FMT_TEMP[13] = (char) (x >>> 10 & 1 | 48);
    FMT_TEMP[14] = (char) (x >>>  9 & 1 | 48);
    FMT_TEMP[15] = (char) (x >>>  8 & 1 | 48);
    FMT_TEMP[16] = (char) (x >>>  7 & 1 | 48);
    FMT_TEMP[17] = (char) (x >>>  6 & 1 | 48);
    FMT_TEMP[18] = (char) (x >>>  5 & 1 | 48);
    FMT_TEMP[19] = (char) (x >>>  4 & 1 | 48);
    FMT_TEMP[20] = (char) (x >>>  3 & 1 | 48);
    FMT_TEMP[21] = (char) (x >>>  2 & 1 | 48);
    FMT_TEMP[22] = (char) (x >>>  1 & 1 | 48);
    FMT_TEMP[23] = (char) (x        & 1 | 48);
    return sb.append (FMT_TEMP, 0, 24);
  }  //fmtBin24(StringBuilder,int)

  //--------------------------------------------------------------------------------
  //16進数変換
  //
  //       x               00 01 02 03 04 05 06 07 08 09 0a 0b 0c 0d 0e 0f
  //     9-x               09 08 07 06 05 04 03 02 01 00 ff fe fd fc fb fa
  //    (9-x)>>4           00 00 00 00 00 00 00 00 00 00 ff ff ff ff ff ff
  //   ((9-x)>>4)&7        00 00 00 00 00 00 00 00 00 00 07 07 07 07 07 07
  //  (((9-x)>>4)&7)+48    30 30 30 30 30 30 30 30 30 30 37 37 37 37 37 37
  //  (((9-x)>>4)&7)+48+x  30 31 32 33 34 35 36 37 38 39 41 42 43 44 45 46
  //                        0  1  2  3  4  5  6  7  8  9  A  B  C  D  E  F
  //
  //       x                00 01 02 03 04 05 06 07 08 09 0a 0b 0c 0d 0e 0f
  //     9-x                09 08 07 06 05 04 03 02 01 00 ff fe fd fc fb fa
  //    (9-x)>>4            00 00 00 00 00 00 00 00 00 00 ff ff ff ff ff ff
  //   ((9-x)>>4)&39        00 00 00 00 00 00 00 00 00 00 27 27 27 27 27 27
  //  (((9-x)>>4)&39)+48    30 30 30 30 30 30 30 30 30 30 57 57 57 57 57 57
  //  (((9-x)>>4)&39)+48+x  30 31 32 33 34 35 36 37 38 39 61 62 63 64 65 66
  //                         0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f
  //
  //                c               30 31 32 33 34 35 36 37 38 39 41 42 43 44 45 46 61 62 63 64 65 66
  //             64-c               10 0f 0e 0d 0c 0b 0a 09 08 07 ff fe fd fc fb fa df de dd dc db da
  //            (64-c)>>8           00 00 00 00 00 00 00 00 00 00 ff ff ff ff ff ff ff ff ff ff ff ff
  //           ((64-c)>>8)&39       00 00 00 00 00 00 00 00 00 00 27 27 27 27 27 27 27 27 27 27 27 27
  //          (((64-c)>>8)&39)+48   30 30 30 30 30 30 30 30 30 30 57 57 57 57 57 57 57 57 57 57 57 57
  //   c|32                         30 31 32 33 34 35 36 37 38 39 61 62 63 64 65 66 61 62 63 64 65 66
  //  (c|32)-((((64-c)>>8)&39)+48)  00 01 02 03 04 05 06 07 08 09 0a 0b 0c 0d 0e 0f 0a 0b 0c 0d 0e 0f

  //c = fmtHexc (x)
  //a = fmtHex1 (a, o, x)
  //s = fmtHex1 (x)
  //sb = fmtHex1 (sb, x)
  //  1桁16進数変換
  public static char fmtHexc (int x) {
    x &= 15;
    return (char) ((((9 - x) >> 4) & 7) + 48 + x);
  }  //fmtHexc(int)
  public static void fmtHex1 (char[] a, int o, int x) {
    x &= 15;
    a[o] = (char) ((((9 - x) >> 4) & 7) + 48 + x);
  }  //fmtHex1(char[],int,int)
  public static String fmtHex1 (int x) {
    x &= 15;
    return Character.toString ((char) ((((9 - x) >> 4) & 7) + 48 + x));
  }  //fmtHex1(int)
  public static StringBuilder fmtHex1 (StringBuilder sb, int x) {
    x &= 15;
    return sb.append ((char) ((((9 - x) >> 4) & 7) + 48 + x));
  }  //fmtHex1(StringBuilder,int)

  //fmtHex2 (a, o, x)
  //s = fmtHex2 (x)
  //sb = fmtHex2 (sb, x)
  //  2桁16進数変換
  //  byte用
  public static void fmtHex2 (char[] a, int o, int x) {
    int x0 = x        & 15;
    int x1 = x >>>  4 & 15;
    a[o    ] = (char) ((((9 - x1) >> 4) & 7) + 48 + x1);
    a[o + 1] = (char) ((((9 - x0) >> 4) & 7) + 48 + x0);
  }  //fmtHex2(char[],int,int)
  public static String fmtHex2 (int x) {
    //fmtHex2 (FMT_TEMP, 0, x);
    int x0 = x        & 15;
    int x1 = x >>>  4 & 15;
    FMT_TEMP[0] = (char) ((((9 - x1) >> 4) & 7) + 48 + x1);
    FMT_TEMP[1] = (char) ((((9 - x0) >> 4) & 7) + 48 + x0);
    return String.valueOf (FMT_TEMP, 0, 2);
  }  //fmtHex2(int)
  public static StringBuilder fmtHex2 (StringBuilder sb, int x) {
    int x0 = x        & 15;
    int x1 = x >>>  4 & 15;
    return (sb.
            append ((char) ((((9 - x1) >> 4) & 7) + 48 + x1)).
            append ((char) ((((9 - x0) >> 4) & 7) + 48 + x0)));
  }  //fmtHex2(StringBuilder,int)

  //fmtHex4 (a, o, x)
  //s = fmtHex4 (x)
  //sb = fmtHex4 (sb, x)
  //  4桁16進数変換
  //  word用
  public static void fmtHex4 (char[] a, int o, int x) {
    int t;
    t = (char) x >>> 12;
    a[o    ] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        x >>>  8 & 15;
    a[o + 1] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        x >>>  4 & 15;
    a[o + 2] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        x        & 15;
    a[o + 3] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
  }  //fmtHex4(char[],int,int)
  public static String fmtHex4 (int x) {
    //fmtHex4 (FMT_TEMP, 0, x);
    int t;
    t = (char) x >>> 12;
    FMT_TEMP[0] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        x >>>  8 & 15;
    FMT_TEMP[1] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        x >>>  4 & 15;
    FMT_TEMP[2] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        x        & 15;
    FMT_TEMP[3] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    return String.valueOf (FMT_TEMP, 0, 4);
  }  //fmtHex4(int)
  public static StringBuilder fmtHex4 (StringBuilder sb, int x) {
    //fmtHex4 (FMT_TEMP, 0, x);
    int t;
    t = (char) x >>> 12;
    FMT_TEMP[0] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        x >>>  8 & 15;
    FMT_TEMP[1] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        x >>>  4 & 15;
    FMT_TEMP[2] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        x        & 15;
    FMT_TEMP[3] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    return sb.append (FMT_TEMP, 0, 4);
  }  //fmtHex4(StringBuilder,int)

  //fmtHex6 (a, o, x)
  //s = fmtHex6 (x)
  //sb = fmtHex6 (sb, x)
  //  6桁16進数変換
  //  rgb用
  public static void fmtHex6 (char[] a, int o, int x) {
    int t;
    t =        x >>> 20 & 15;
    a[o    ] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        x >>> 16 & 15;
    a[o + 1] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t = (char) x >>> 12;
    a[o + 2] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        x >>>  8 & 15;
    a[o + 3] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        x >>>  4 & 15;
    a[o + 4] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        x        & 15;
    a[o + 5] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
  }  //fmtHex6(char[],int,int)
  public static String fmtHex6 (int x) {
    //fmtHex6 (FMT_TEMP, 0, x);
    int t;
    t =        x >>> 20 & 15;
    FMT_TEMP[0] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        x >>> 16 & 15;
    FMT_TEMP[1] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t = (char) x >>> 12;
    FMT_TEMP[2] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        x >>>  8 & 15;
    FMT_TEMP[3] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        x >>>  4 & 15;
    FMT_TEMP[4] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        x        & 15;
    FMT_TEMP[5] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    return String.valueOf (FMT_TEMP, 0, 6);
  }  //fmtHex6(int)
  public static StringBuilder fmtHex6 (StringBuilder sb, int x) {
    //fmtHex6 (FMT_TEMP, 0, x);
    int t;
    t =        x >>> 20 & 15;
    FMT_TEMP[0] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        x >>> 16 & 15;
    FMT_TEMP[1] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t = (char) x >>> 12;
    FMT_TEMP[2] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        x >>>  8 & 15;
    FMT_TEMP[3] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        x >>>  4 & 15;
    FMT_TEMP[4] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        x        & 15;
    FMT_TEMP[5] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    return sb.append (FMT_TEMP, 0, 6);
  }  //fmtHex6(StringBuilder,int)

  //fmtHex8 (a, o, x)
  //s = fmtHex8 (x)
  //sb = fmtHex8 (sb, x)
  //  8桁16進数変換
  //  argb,long用
  public static void fmtHex8 (char[] a, int o, int x) {
    int t;
    t =        x >>> 28;
    a[o    ] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        x >>> 24 & 15;
    a[o + 1] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        x >>> 20 & 15;
    a[o + 2] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        x >>> 16 & 15;
    a[o + 3] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t = (char) x >>> 12;
    a[o + 4] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        x >>>  8 & 15;
    a[o + 5] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        x >>>  4 & 15;
    a[o + 6] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        x        & 15;
    a[o + 7] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
  }  //fmtHex8(char[],int,int)
  public static String fmtHex8 (int x) {
    //fmtHex8 (FMT_TEMP, 0, x);
    int t;
    t =        x >>> 28;
    FMT_TEMP[0] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        x >>> 24 & 15;
    FMT_TEMP[1] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        x >>> 20 & 15;
    FMT_TEMP[2] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        x >>> 16 & 15;
    FMT_TEMP[3] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t = (char) x >>> 12;
    FMT_TEMP[4] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        x >>>  8 & 15;
    FMT_TEMP[5] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        x >>>  4 & 15;
    FMT_TEMP[6] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        x        & 15;
    FMT_TEMP[7] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    return String.valueOf (FMT_TEMP, 0, 8);
  }  //fmtHex8(int)
  public static StringBuilder fmtHex8 (StringBuilder sb, int x) {
    //fmtHex8 (FMT_TEMP, 0, x);
    int t;
    t =        x >>> 28;
    FMT_TEMP[0] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        x >>> 24 & 15;
    FMT_TEMP[1] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        x >>> 20 & 15;
    FMT_TEMP[2] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        x >>> 16 & 15;
    FMT_TEMP[3] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t = (char) x >>> 12;
    FMT_TEMP[4] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        x >>>  8 & 15;
    FMT_TEMP[5] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        x >>>  4 & 15;
    FMT_TEMP[6] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        x        & 15;
    FMT_TEMP[7] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    return sb.append (FMT_TEMP, 0, 8);
  }  //fmtHex8(StringBuilder,int)

  public static StringBuilder fmtHex16 (StringBuilder sb, long x) {
    //fmtHex16 (FMT_TEMP, 0, x);
    int s, t;
    s = (int) (x >>> 32);
    t =        s >>> 28;
    FMT_TEMP[ 0] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        s >>> 24 & 15;
    FMT_TEMP[ 1] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        s >>> 20 & 15;
    FMT_TEMP[ 2] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        s >>> 16 & 15;
    FMT_TEMP[ 3] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t = (char) s >>> 12;
    FMT_TEMP[ 4] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        s >>>  8 & 15;
    FMT_TEMP[ 5] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        s >>>  4 & 15;
    FMT_TEMP[ 6] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        s        & 15;
    FMT_TEMP[ 7] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    s = (int)  x;
    t =        s >>> 28;
    FMT_TEMP[ 8] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        s >>> 24 & 15;
    FMT_TEMP[ 9] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        s >>> 20 & 15;
    FMT_TEMP[10] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        s >>> 16 & 15;
    FMT_TEMP[11] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t = (char) s >>> 12;
    FMT_TEMP[12] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        s >>>  8 & 15;
    FMT_TEMP[13] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        s >>>  4 & 15;
    FMT_TEMP[14] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    t =        s        & 15;
    FMT_TEMP[15] = (char) ((((9 - t) >> 4) & 7) + 48 + t);
    return sb.append (FMT_TEMP, 0, 16);
  }  //fmtHex16(StringBuilder,long)

  //--------------------------------------------------------------------------------
  //10進数変換
  //  除算は遅いので逆数乗算で商と余りを求めて下位から充填する
  //  x/yを計算する代わりにceil(pow(2,n)/y)を掛けてから右にnビットシフトする
  //  m=((1<<n)+y-1)/yとおくと0<=x<=((((m-1)/(m*y-(1<<n))+1)<<n)-1)/mの範囲でx/yはm*x>>nに置き換えられる
  //    >perl -e "use GMP::Mpz qw(:all);$y=mpz(10);for($n=mpz(0);$n<=31;$n++){$m=((1<<$n)+$y-1)/$y;$l=(((($m-1)/($m*$y-(1<<$n))+1)<<$n)-1)/$m;printf'x*%s>>%s (0<=x<=%s)%c',$m,$n,$l,10;}"
  //    x*1>>0 (0<=x<=0)
  //    x*1>>1 (0<=x<=1)
  //    x*1>>2 (0<=x<=3)
  //    x*1>>3 (0<=x<=7)
  //    x*2>>4 (0<=x<=7)
  //    x*4>>5 (0<=x<=7)
  //    x*7>>6 (0<=x<=18)
  //    x*13>>7 (0<=x<=68)
  //    x*26>>8 (0<=x<=68)
  //    x*52>>9 (0<=x<=68)
  //    x*103>>10 (0<=x<=178)  2桁
  //    x*205>>11 (0<=x<=1028)  3桁
  //    x*410>>12 (0<=x<=1028)
  //    x*820>>13 (0<=x<=1028)
  //    x*1639>>14 (0<=x<=2738)
  //    x*3277>>15 (0<=x<=16388)  4桁
  //    x*6554>>16 (0<=x<=16388)
  //    x*13108>>17 (0<=x<=16388)
  //    x*26215>>18 (0<=x<=43698)
  //    x*52429>>19 (0<=x<=262148)  5桁。ここからlong
  //    x*104858>>20 (0<=x<=262148)
  //    x*209716>>21 (0<=x<=262148)
  //    x*419431>>22 (0<=x<=699058)
  //    x*838861>>23 (0<=x<=4194308)  6桁
  //    x*1677722>>24 (0<=x<=4194308)
  //    x*3355444>>25 (0<=x<=4194308)
  //    x*6710887>>26 (0<=x<=11184818)  7桁
  //    x*13421773>>27 (0<=x<=67108868)
  //    x*26843546>>28 (0<=x<=67108868)
  //    x*53687092>>29 (0<=x<=67108868)
  //    x*107374183>>30 (0<=x<=178956978)  8桁
  //    x*214748365>>31 (0<=x<=1073741828)  9桁
  //
  //  検算
  //    >perl -e "use GMP::Mpz qw(:all);$y=mpz(10);for($n=mpz(0);$n<=23;$n++){$m=((1<<$n)+$y-1)/$y;$l=(((($m-1)/($m*$y-(1<<$n))+1)<<$n)-1)/$m;for($x=mpz(0);$x<=$l+1;$x++){$t=$m*$x>>$n;$z=$x/$y;if($z!=$t){printf'n=%d,m=%d,l=%d,x=%d,y=%d,z=%d,t=%d%c',$n,$m,$l,$x,$y,$z,$t,10;}}}"
  //    n=0,m=1,l=0,x=1,y=10,z=0,t=1
  //    n=1,m=1,l=1,x=2,y=10,z=0,t=1
  //    n=2,m=1,l=3,x=4,y=10,z=0,t=1
  //    n=3,m=1,l=7,x=8,y=10,z=0,t=1
  //    n=4,m=2,l=7,x=8,y=10,z=0,t=1
  //    n=5,m=4,l=7,x=8,y=10,z=0,t=1
  //    n=6,m=7,l=18,x=19,y=10,z=1,t=2
  //    n=7,m=13,l=68,x=69,y=10,z=6,t=7
  //    n=8,m=26,l=68,x=69,y=10,z=6,t=7
  //    n=9,m=52,l=68,x=69,y=10,z=6,t=7
  //    n=10,m=103,l=178,x=179,y=10,z=17,t=18
  //    n=11,m=205,l=1028,x=1029,y=10,z=102,t=103
  //    n=12,m=410,l=1028,x=1029,y=10,z=102,t=103
  //    n=13,m=820,l=1028,x=1029,y=10,z=102,t=103
  //    n=14,m=1639,l=2738,x=2739,y=10,z=273,t=274
  //    n=15,m=3277,l=16388,x=16389,y=10,z=1638,t=1639
  //    n=16,m=6554,l=16388,x=16389,y=10,z=1638,t=1639
  //    n=17,m=13108,l=16388,x=16389,y=10,z=1638,t=1639
  //    n=18,m=26215,l=43698,x=43699,y=10,z=4369,t=4370
  //    n=19,m=52429,l=262148,x=262149,y=10,z=26214,t=26215
  //    n=20,m=104858,l=262148,x=262149,y=10,z=26214,t=26215
  //    n=21,m=209716,l=262148,x=262149,y=10,z=26214,t=26215
  //    n=22,m=419431,l=699058,x=699059,y=10,z=69905,t=69906
  //    n=23,m=838861,l=4194308,x=4194309,y=10,z=419430,t=419431

  //  4桁まではあらかじめテーブルに展開しておく
  public static final int[] FMT_BCD4 = new int[10000];
  public static final int[] FMT_DCB4 = new int[65536];

  //--------------------------------------------------------------------------------
  //fmtInit ()
  //  初期化
  public static void fmtInit () {
    Arrays.fill (FMT_DCB4, -1);
    int i = 0;
    int x = 0;
    for (int a = 0; a < 10; a++) {
      for (int b = 0; b < 10; b++) {
        for (int c = 0; c < 10; c++) {
          FMT_DCB4[FMT_BCD4[i    ] = x    ] = i;
          FMT_DCB4[FMT_BCD4[i + 1] = x + 1] = i + 1;
          FMT_DCB4[FMT_BCD4[i + 2] = x + 2] = i + 2;
          FMT_DCB4[FMT_BCD4[i + 3] = x + 3] = i + 3;
          FMT_DCB4[FMT_BCD4[i + 4] = x + 4] = i + 4;
          FMT_DCB4[FMT_BCD4[i + 5] = x + 5] = i + 5;
          FMT_DCB4[FMT_BCD4[i + 6] = x + 6] = i + 6;
          FMT_DCB4[FMT_BCD4[i + 7] = x + 7] = i + 7;
          FMT_DCB4[FMT_BCD4[i + 8] = x + 8] = i + 8;
          FMT_DCB4[FMT_BCD4[i + 9] = x + 9] = i + 9;
          i += 10;
          x += 1 << 4;
        }
        x += 6 << 4;
      }
      x += 6 << 8;
    }
  }  //fmtInit()

  //y = fmtBcd4 (x)
  //  xを0～9999にクリッピングしてから4桁のBCDに変換する
  public static int fmtBcd4 (int x) {
    //x = Math.max (0, Math.min (9999, x));
    //perl optdiv.pl 9999 10
    //  x/10==x*3277>>>15 (0<=x<=16388) [9999*3277==32766723]
    //int t = x * 3277 >> 15;  //x/10
    //int y = x - t * 10;  //1の位
    //x = t * 3277 >> 15;  //x/100
    //y |= t - x * 10 << 4;  //10の位
    //t = x * 3277 >> 15;  //x/1000
    //return t << 12 | x - t * 10 << 8 | y;  //1000の位,100の位
    return FMT_BCD4[Math.max (0, Math.min (9999, x))];
  }  //fmtBcd4(int)

  //y = fmtBcd8 (x)
  //  xを0～99999999にクリッピングしてから8桁のBCDに変換する
  public static int fmtBcd8 (int x) {
    x = Math.max (0, Math.min (99999999, x));
    //perl optdiv.pl 99999999 10000
    //  x/10000==x*109951163>>>40 (0<=x<=494389998) [99999999*109951163==10995116190048837]
    int q = (int) ((long) x * 109951163L >>> 40);  //x/10000。1.6ns@2000000000
    //int q = x / 10000;  //2.0ns@2000000000
    return FMT_BCD4[q] << 16 | FMT_BCD4[x - 10000 * q];
  }  //fmtBcd8(int)

  //y = fmtBcd12 (x)
  //  xを0～999999999999Lにクリッピングしてから12桁のBCDに変換する
  public static long fmtBcd12 (long x) {
    x = Math.max (0L, Math.min (999999999999L, x));
    int q = (int) ((double) x / 100000000.0);  //(int) (x / 100000000L);
    int r = (int) (x - 100000000L * q);
    //perl optdiv.pl 99999999 10000
    //  x/10000==x*109951163>>>40 (0<=x<=494389998) [99999999*109951163==10995116190048837]
    int rq = (int) ((long) r * 109951163L >>> 40);  //r/10000
    //int rq = r / 10000;
    return (long) FMT_BCD4[q] << 32 | 0xffffffffL & (FMT_BCD4[rq] << 16 | FMT_BCD4[r - 10000 * rq]);
  }  //fmtBcd12(long)

  //y = fmtBcd16 (x)
  //  xを0～9999999999999999Lにクリッピングしてから16桁のBCDに変換する
  public static long fmtBcd16 (long x) {
    x = Math.max (0L, Math.min (9999999999999999L, x));
    int q = x <= (1L << 53) ? (int) ((double) x / 100000000.0) : (int) (x / 100000000L);
    int r = (int) (x - 100000000L * q);
    //perl optdiv.pl 99999999 10000
    //  x/10000==x*109951163>>>40 (0<=x<=494389998) [99999999*109951163==10995116190048837]
    int qq = (int) ((long) q * 109951163L >>> 40);  //q/10000
    //int qq = q / 10000;
    //perl optdiv.pl 99999999 10000
    //  x/10000==x*109951163>>>40 (0<=x<=494389998) [99999999*109951163==10995116190048837]
    int rq = (int) ((long) r * 109951163L >>> 40);  //r/10000
    //int rq = r / 10000;
    return (long) (FMT_BCD4[qq] << 16 | FMT_BCD4[q - 10000 * qq]) << 32 | 0xffffffffL & (FMT_BCD4[rq] << 16 | FMT_BCD4[r - 10000 * rq]);
  }  //fmtBcd16(long)

  //--------------------------------------------------------------------------------
  //o = fmtCA02u (a, o, x)
  //sb = fmtSB02u (sb, x)
  //  %02u
  //  2桁10進数変換(符号なし,ゼロサプレスなし)
  public static int fmtCA02u (char[] a, int o, int x) {
    if (x < 0 || 99 < x) {
      x = 99;
    }
    x = FMT_BCD4[x];
    a[o    ] = (char) ('0' | x >>> 4);
    a[o + 1] = (char) ('0' | x        & 15);
    return o + 2;
  }  //fmtCA02u(char[],int,int)
  public static StringBuilder fmtSB02u (StringBuilder sb, int x) {
    return sb.append (FMT_TEMP, 0, fmtCA02u (FMT_TEMP, 0, x));
  }  //fmtSB02u(StringBuilder,int)

  //o = fmtCA2u (a, o, x)
  //sb = fmtSB2u (sb, x)
  //  %2u
  //  2桁10進数変換(符号なし,ゼロサプレスあり)
  public static int fmtCA2u (char[] a, int o, int x) {
    if (x < 0 || 99 < x) {
      x = 99;
    }
    x = FMT_BCD4[x];
    if (x <= 0x000f) {  //1桁
      a[o++] = (char) ('0' | x);
    } else {  //2桁
      a[o++] = (char) ('0' | x >>>  4);
      a[o++] = (char) ('0' | x        & 15);
    }
    return o;
  }  //fmtCA2u(char[],int,int)
  public static StringBuilder fmtSB2u (StringBuilder sb, int x) {
    return sb.append (FMT_TEMP, 0, fmtCA2u (FMT_TEMP, 0, x));
  }  //fmtSB2u(StringBuilder,int)

  //o = fmtCA04u (a, o, x)
  //sb = fmtSB04u (sb, x)
  //  %04u
  //  4桁10進数変換(符号なし,ゼロサプレスなし)
  public static int fmtCA04u (char[] a, int o, int x) {
    if (x < 0 || 9999 < x) {
      x = 9999;
    }
    x = FMT_BCD4[x];
    a[o    ] = (char) ('0' | x >>> 12);
    a[o + 1] = (char) ('0' | x >>>  8 & 15);
    a[o + 2] = (char) ('0' | x >>>  4 & 15);
    a[o + 3] = (char) ('0' | x        & 15);
    return o + 4;
  }  //fmtCA04u(char[],int,int)
  public static StringBuilder fmtSB04u (StringBuilder sb, int x) {
    return sb.append (FMT_TEMP, 0, fmtCA04u (FMT_TEMP, 0, x));
  }  //fmtSB04u(StringBuilder,int)

  //o = fmtCA4u (a, o, x)
  //sb = fmtSB4u (sb, x)
  //  %4u
  //  4桁10進数変換(符号なし,ゼロサプレスあり)
  public static int fmtCA4u (char[] a, int o, int x) {
    if (x < 0 || 9999 < x) {
      x = 9999;
    }
    x = FMT_BCD4[x];
    if (x <= 0x000f) {  //1桁
      a[o++] = (char) ('0' | x);
    } else if (x <= 0x00ff) {  //2桁
      a[o++] = (char) ('0' | x >>>  4);
      a[o++] = (char) ('0' | x        & 15);
    } else if (x <= 0x0fff) {  //3桁
      a[o++] = (char) ('0' | x >>>  8);
      a[o++] = (char) ('0' | x >>>  4 & 15);
      a[o++] = (char) ('0' | x        & 15);
    } else {  //4桁
      a[o++] = (char) ('0' | x >>> 12);
      a[o++] = (char) ('0' | x >>>  8 & 15);
      a[o++] = (char) ('0' | x >>>  4 & 15);
      a[o++] = (char) ('0' | x        & 15);
    }
    return o;
  }  //fmtCA4u(char[],int,int)
  public static StringBuilder fmtSB4u (StringBuilder sb, int x) {
    return sb.append (FMT_TEMP, 0, fmtCA4u (FMT_TEMP, 0, x));
  }  //fmtSB4u(StringBuilder,int)

  //o = fmtCA08u (a, o, x)
  //sb = fmtSB08u (sb, x)
  //  %08u
  //  8桁10進数変換(符号なし,ゼロサプレスなし)
  public static int fmtCA08u (char[] a, int o, int x) {
    if (x < 0 || 99999999 < x) {
      x = 99999999;
    }
    //perl optdiv.pl 99999999 10000
    //  x/10000==x*109951163>>>40 (0<=x<=494389998) [99999999*109951163==10995116190048837]
    int h = (int) ((long) x * 109951163L >>> 40);  //x/10000
    return fmtCA04u (a, fmtCA04u (a, o, h), x - h * 10000);
  }  //fmtCA08u(char[],int,int)
  public static StringBuilder fmtSB08u (StringBuilder sb, int x) {
    return sb.append (FMT_TEMP, 0, fmtCA08u (FMT_TEMP, 0, x));
  }  //fmtSB08u(StringBuilder,int)

  //o = fmtCA8u (a, o, x)
  //sb = fmtSB8u (sb, x)
  //  %8u
  //  8桁10進数変換(符号なし,ゼロサプレスあり)
  public static int fmtCA8u (char[] a, int o, int x) {
    if (x < 0 || 99999999 < x) {
      x = 99999999;
    }
    if (x <= 9999) {  //1～4桁
      return fmtCA4u (a, o, x);
    } else {  //5～8桁
      //perl optdiv.pl 99999999 10000
      //  x/10000==x*109951163>>>40 (0<=x<=494389998) [99999999*109951163==10995116190048837]
      int h = (int) ((long) x * 109951163L >>> 40);  //x/10000
      return fmtCA04u (a, fmtCA4u (a, o, h), x - h * 10000);
    }
  }  //fmtCA8u(char[],int,int)
  public static StringBuilder fmtSB8u (StringBuilder sb, int x) {
    return sb.append (FMT_TEMP, 0, fmtCA8u (FMT_TEMP, 0, x));
  }  //fmtSB8u(StringBuilder,int)

  //o = fmtCAd (a, o, x)
  //sb = fmtSBd (sb, x)
  //  %d
  //  10進数変換(符号あり,ゼロサプレスあり)
  public static int fmtCAd (char[] a, int o, long x) {
    if (x < 0L) {
      x = -x;
      a[o++] = '-';
    }
    if (x <= 99999999L) {  //1～8桁
      return fmtCA8u (a, o, (int) x);
    } else if (x <= 9999999999999999L) {  //9～16桁
      long h = x / 100000000L;
      return fmtCA08u (a, fmtCA8u (a, o, (int) h), (int) (x - h * 100000000L));
    } else {  //17～19桁
      long hh = x / 10000000000000000L;
      x -= hh * 10000000000000000L;
      long h = x / 100000000L;
      return fmtCA08u (a, fmtCA08u (a, fmtCA4u (a, o, (int) hh), (int) h), (int) (x - h * 100000000L));
    }
  }  //fmtCAd(char[],int,long)
  public static StringBuilder fmtSBd (StringBuilder sb, long x) {
    return sb.append (FMT_TEMP, 0, fmtCAd (FMT_TEMP, 0, x));
  }  //fmtSBd(StringBuilder,long)

  //o = fmtCAnd (a, o, n, x)
  //sb = fmtSBnd (sb, n, x)
  //  %*d
  //  n桁10進数変換(符号あり,ゼロサプレスあり)
  //  n桁に収まらないとき右側にはみ出すのでバッファのサイズに注意
  public static int fmtCAnd (char[] a, int o, int n, long x) {
    int t = fmtCAd (a, o, x);  //現在の末尾
    n += o;  //必要な末尾
    if (t < n) {  //余っている
      int i = n;
      while (o < t) {  //右から順に右にずらす
        a[--i] = a[--t];
      }
      while (o < i) {  //左にできた隙間を' 'で埋める
        a[--i] = ' ';
      }
      t = n;
    }
    return t;
  }  //fmtnu(char[],int,int,long)
  public static StringBuilder fmtSBnd (StringBuilder sb, int n, int x) {
    return sb.append (FMT_TEMP, 0, fmtCAnd (FMT_TEMP, 0, n, x));
  }  //fmtSBnu(StringBuilder,int,long)

  //--------------------------------------------------------------------------------
  //10進数文字列解析

  //x = fmtParseInt (s, i, min, max, err)
  //  文字列sのインデックスiから基数10で整数xを読み取る
  //x = fmtParseIntRadix (s, i, min, max, err, radix)
  //  文字列sのインデックスiから基数radixで整数xを読み取る
  //  基数radixは2,8,10,16のいずれかに限る
  //  1文字も読み取れないかmin<=x&&x<=maxでないときはerrを返す
  //  先頭の空白を読み飛ばす
  //  数値の先頭の'$'は16進数の強制指定とみなす
  //  数値の後のゴミは無視する
  public static int fmtParseInt (String s, int i, int min, int max, int err) {
    return fmtParseIntRadix (s, i, min, max, err, 10);
  }  //fmtParseInt(String,int,int,int,int)
  public static int fmtParseIntRadix (String s, int i, int min, int max, int err, int radix) {
    if (s == null) {
      return err;
    }
    int l = s.length ();
    int c = i < l ? s.charAt (i++) : -1;
    //空白を読み飛ばす
    while (c == ' ' || c == '\t') {
      c = i < l ? s.charAt (i++) : -1;
    }
    //符号を読み取る
    int n = 0;
    if (c == '+') {
      c = i < l ? s.charAt (i++) : -1;
    } else if (c == '-') {
      n = 1;
      c = i < l ? s.charAt (i++) : -1;
    }
    //基数を読み取る
    //        2進数の範囲        8進数の範囲       10進数の範囲        16進数の範囲
    //  +    0x3fffffff*2+1     0x0fffffff*8+7     214748364*10+7     0x07ffffff*16+15
    //  -  -(0x40000000*2+0)  -(0x10000000*8+0)  -(214748364*10+8)  -(0x08000000*16+ 0)
    int o;
    int p;
    if (c == '$') {  //16進数
      o = 0x07ffffff + n;
      p = 15 + n & 15;
      radix = 16;
      c = i < l ? s.charAt (i++) : -1;
    } else if (radix == 16) {  //16進数
      o = 0x07ffffff + n;
      p = 15 + n & 15;
    } else if (radix == 8) {  //8進数
      o = 0x0fffffff + n;
      p = 7 + n & 7;
    } else if (radix == 2) {  //2進数
      o = 0x3fffffff + n;
      p = 1 + n & 1;
    } else {  //10進数
      o = 214748364;
      p = 7 + n;
      radix = 10;
    }
    //数値を読み取る
    int x = Character.digit (c, radix);
    if (x < 0) {
      return err;
    }
    c = i < l ? Character.digit (s.charAt (i++), radix) : -1;
    while (c >= 0) {
      int t = x - o;
      if (t > 0 || t == 0 && c > p) {
        return err;
      }
      x = x * radix + c;
      c = i < l ? Character.digit (s.charAt (i++), radix) : -1;
    }
    if (n != 0) {
      x = -x;
    }
    return min <= x && x <= max ? x : err;
  }  //fmtParseIntRadix(String,int,int,int,int,int)



  //========================================================================================
  //$$MAT 数学関数

  //x = matMax3 (x1, x2, x3)
  //x = matMax4 (x1, x2, x3, x4)
  //x = matMax5 (x1, x2, x3, x4, x5)
  //  最大値
  public static long matMax3 (long x1, long x2, long x3) {
    return Math.max (Math.max (x1, x2), x3);
  }  //matMax3(long,long,long)
  public static long matMax4 (long x1, long x2, long x3, long x4) {
    return Math.max (Math.max (x1, x2), Math.max (x3, x4));
  }  //matMax4(long,long,long,long)
  public static long matMax5 (long x1, long x2, long x3, long x4, long x5) {
    return Math.max (Math.max (Math.max (x1, x2), Math.max (x3, x4)), x5);
  }  //matMax5(long,long,long,long,long)

  //x = matMin3 (x1, x2, x3)
  //x = matMin4 (x1, x2, x3, x4)
  //x = matMin5 (x1, x2, x3, x4, x5)
  //  最小値
  public static long matMin3 (long x1, long x2, long x3) {
    return Math.min (Math.min (x1, x2), x3);
  }  //matMin3(long,long,long)
  public static long matMin4 (long x1, long x2, long x3, long x4) {
    return Math.min (Math.min (x1, x2), Math.min (x3, x4));
  }  //matMin4(long,long,long,long)
  public static long matMin5 (long x1, long x2, long x3, long x4, long x5) {
    return Math.min (Math.min (Math.min (x1, x2), Math.min (x3, x4)), x5);
  }  //matMin5(long,long,long,long,long)



  //========================================================================================
  //$$STR 文字列

  //s = encodeUTF8 (s)
  //  UTF-8変換
  //  00000000 00000000 00000000 0xxxxxxx => 00000000 00000000 00000000 0xxxxxxx
  //  00000000 00000000 00000xxx xxyyyyyy => 00000000 00000000 110xxxxx 10yyyyyy
  //  00000000 00000000 xxxxyyyy yyzzzzzz => 00000000 1110xxxx 10yyyyyy 10zzzzzz
  //  00000000 000xxxyy yyyyzzzz zzxxxxxx => 11110xxx 10yyyyyy 10zzzzzz 10xxxxxx
  public static String strEncodeUTF8 (String s) {
    StringBuilder sb = new StringBuilder ();
    int l = s.length ();
    for (int i = 0; i < l; i++) {
      int u = s.charAt (i);
      if (0xd800 <= u && u <= 0xdbff && i + 1 < l) {
        int v = s.charAt (i + 1);
        if (0xdc00 <= v && v <= 0xdfff) {  //surrogate pair
          u = 0x10000 + ((u & 0x3ff) << 10) + (v & 0x3ff);
          i++;
        }
      }
      if ((u & 0xffffff80) == 0) {  //7bit
        sb.append ((char) u);
      } else if ((u & 0xfffff800) == 0) {  //11bit
        u = (0x0000c080 |
             (u & 0x000007c0) << 2 |
             (u & 0x0000003f));
        sb.append ((char) (u >> 8)).append ((char) (u & 0xff));
      } else if ((u & 0xffff0000) == 0 && !(0xd800 <= u && u <= 0xdfff)) {  //16bit except broken surrogate pair
        u = (0x00e08080 |
             (u & 0x0000f000) << 4 |
             (u & 0x00000fc0) << 2 |
             (u & 0x0000003f));
        sb.append ((char) (u >> 16)).append ((char) ((u >> 8) & 0xff)).append ((char) (u & 0xff));
      } else if ((u & 0xffe00000) == 0) {  //21bit
        u = (0xf0808080 |
             (u & 0x001c0000) << 6 |
             (u & 0x0003f000) << 4 |
             (u & 0x00000fc0) << 2 |
             (u & 0x0000003f));
        sb.append ((char) ((u >> 24) & 0xff)).append ((char) ((u >> 16) & 0xff)).append ((char) ((u >> 8) & 0xff)).append ((char) (u & 0xff));
      } else {  //out of range or broken surrogate pair
        sb.append ((char) 0xef).append ((char) 0xbf).append ((char) 0xbd);  //U+FFFD REPLACEMENT CHARACTER
      }
    }
    return sb.toString ();
  }  //encodeUTF8(String)

  //s = decodeUTF8 (s)
  //  UTF-8逆変換
  //  00000000 00000000 00000000 0xxxxxxx => 00000000 00000000 00000000 0xxxxxxx
  //  00000000 00000000 110xxxxx 10yyyyyy => 00000000 00000000 00000xxx xxyyyyyy
  //  00000000 1110xxxx 10yyyyyy 10zzzzzz => 00000000 00000000 xxxxyyyy yyzzzzzz
  //  11110xxx 10yyyyyy 10zzzzzz 10xxxxxx => 00000000 000xxxyy yyyyzzzz zzxxxxxx
  public static String strDecodeUTF8 (String s) {
    StringBuilder sb = new StringBuilder ();
    int l = s.length ();
    for (int i = 0; i < l; i++) {
      int c = s.charAt (i) & 0xff;
      for (int k = ((c & 0x80) == 0x00 ? 0 :  //0xxxxxxx 7bit
                    (c & 0xe0) == 0xc0 ? 1 :  //110xxxxx 11bit
                    (c & 0xf0) == 0xe0 ? 2 :  //1110xxxx 16bit
                    (c & 0xf8) == 0xf0 ? 3 :  //11110xxx 21bit
                    -1);  //not supported
           --k >= 0; ) {
        c = c << 8 | (i + 1 < l ? s.charAt (++i) & 0xff : 0);
      }
      int u = ((c & 0xffffff80) == 0x00000000 ? c :
               (c & 0xffffe0c0) == 0x0000c080 ? ((c & 0x00001f00) >> 2 |
                                                 (c & 0x0000003f)) :
               (c & 0xfff0c0c0) == 0x00e08080 ? ((c & 0x000f0000) >> 4 |
                                                 (c & 0x00003f00) >> 2 |
                                                 (c & 0x0000003f)) :
               (c & 0xf8c0c0c0) == 0xf0808080 ? ((c & 0x07000000) >> 6 |
                                                 (c & 0x003f0000) >> 4 |
                                                 (c & 0x00003f00) >> 2 |
                                                 (c & 0x0000003f)) :
               0xfffd);  //U+FFFD REPLACEMENT CHARACTER
      if (u <= 0x0000ffff) {
        sb.append (0xd800 <= u && u <= 0xdfff ? '\ufffd' :  //U+FFFD REPLACEMENT CHARACTER
                   (char) u);
      } else if (u <= 0x0010ffff) {
        u -= 0x000010000;
        sb.append ((char) (0xd800 + ((u >> 10) & 0x3ff))).append ((char) (0xdc00 + (u & 0x3ff)));
      }
    }
    return sb.toString ();
  }  //decodeUTF8(String)

  //uri = strEncodeURI (s)
  //  URI変換
  //  UTF-8変換を行ってからRFC3986のPercent-Encodingを行う
  //  フォームの送信に使用されるapplication/x-www-form-urlencodedではない。" "は"+"ではなく"%20"に変換される
  public static final int[] IsURIChar = {  //URIに使える文字。RFC3986のUnreserved Characters。[-.0-9A-Z_a-z~]
    //00000000 00000000 11111111 11111111
    //01234567 89abcdef 01234567 89abcdef
    0b00000000_00000000_00000000_00000000,  //0x00..0x1f
    0b00000000_00000110_11111111_11000000,  //0x20..0x3f [-.0-9]
    0b01111111_11111111_11111111_11100001,  //0x40..0x5f [A-Z_]
    0b01111111_11111111_11111111_11100010,  //0x60..0x7f [a-z~]
  };
  public static String strEncodeURI (String s) {
    s = strEncodeUTF8 (s);  //UTF-8変換
    StringBuilder sb = new StringBuilder ();
    int l = s.length ();
    for (int i = 0; i < l; i++) {
      int c = s.charAt (i);
      if (c < 0x80 && IsURIChar[c >> 5] << c < 0) {  //URIに使える文字
        sb.append ((char) c);
      } else {
        fmtHex2 (sb.append ('%'), c);
      }
    }
    return sb.toString ();
  }  //encodeURI(String)

  //s = strDecodeURI (s)
  //  URI逆変換
  //  RFC3986のPercent-Encodingの逆変換を行ってからUTF-8逆変換を行う
  //  フォームの送信に使用されるapplication/x-www-form-urlencodedではない。"+"は" "に変換されない
  public static final byte[] strIsHexChar = {  //16進数に使えるASCII文字。[0-9A-Fa-f]
    // 01 02 03 04 05 06 07 08 09 0a 0b 0c 0d 0e 0f 10 11 12 13 14 15 16 17 18 19 1a 1b 1c 1d 1e 1f
    -1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,  //0x00..0x1f
    -1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9,-1,-1,-1,-1,-1,-1,  //0x20..0x3f [0-9]
    -1,10,11,12,13,14,15,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,  //0x40..0x5f [A-F]
    -1,10,11,12,13,14,15,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,  //0x60..0x7f [a-f]
  };
  public static String strDecodeURI (String s) {
    StringBuilder sb = new StringBuilder ();
    int l = s.length ();
    for (int i = 0; i < l; i++) {
      int c = s.charAt (i);
      if (c == '%' && i + 2 < l) {
        int d = s.charAt (i + 1);
        int e = s.charAt (i + 2);
        if (d < 0x80 && (d = strIsHexChar[d]) >= 0 &&
            e < 0x80 && (e = strIsHexChar[e]) >= 0) {
          sb.append ((char) (d << 4 | e));
        } else {
          sb.append ((char) c);
        }
      } else {
        sb.append ((char) c);
      }
    }
    return sb.toString ();
  }  //decodeURI(String)



  //========================================================================================
  //$$IMG イメージ

  //image = createImage (width, height, pattern, rgb, ...)
  //  イメージを作る
  public static BufferedImage createImage (int width, int height, String pattern, int... rgbs) {
    BufferedImage image = new BufferedImage (width, height, BufferedImage.TYPE_INT_RGB);
    int[] bitmap = ((DataBufferInt) image.getRaster ().getDataBuffer ()).getData ();
    int length = width * height;
    for (int i = 0; i < length; i++) {
      char c = pattern.charAt (i);
      bitmap[i] = rgbs[c < '0' ? 0 : Character.digit (c, 16)];
    }
    return image;
  }  //createImage(int,int,String,int...)

  //icon = createImageIcon (width, height, pattern, rgb, ...)
  //  イメージアイコンを作る
  public static ImageIcon createImageIcon (int width, int height, String pattern, int... rgbs) {
    return new ImageIcon (createImage (width, height, pattern, rgbs));
  }  //createImageIcon(int,int,String,int...)

  //paint = createTexturePaint (width, height, pattern, rgb, ...)
  //  テクスチャペイントを作る
  public static TexturePaint createTexturePaint (int width, int height, String pattern, int... rgbs) {
    return new TexturePaint (createImage (width, height, pattern, rgbs), new Rectangle (0, 0, width, height));
  }  //createTexturePaint(int,int,String,int...)

  //image = loadImage (name)
  //  イメージを読み込む
  public static BufferedImage loadImage (String name) {
    BufferedImage image = null;
    try {
      image = ImageIO.read (new File (name));
    } catch (Exception e) {
    }
    return image;
  }  //loadImage(String)

  //sucess = saveImage (image, name)
  //sucess = saveImage (image, name, quality)
  //  イメージを書き出す
  public static boolean saveImage (BufferedImage image, String name) {
    return saveImage (image, name, 0.75F);
  }  //saveImage(BufferedImage,String)
  public static boolean saveImage (BufferedImage image, String name, float quality) {
    int index = name.lastIndexOf (".");
    if (index < 0) {  //拡張子がない
      return false;
    }
    if (name.substring (index).equalsIgnoreCase (".ico")) {  //アイコンファイルの作成
      return saveIcon (name, image);
    }
    Iterator<ImageWriter> iterator = ImageIO.getImageWritersBySuffix (name.substring (index + 1));  //拡張子に対応するImageWriterがないときは空のIteratorを返す
    if (!iterator.hasNext ()) {  //拡張子に対応するImageWriterがない
      return false;
    }
    ImageWriter imageWriter = iterator.next ();
    ImageWriteParam imageWriteParam = imageWriter.getDefaultWriteParam ();
    if (imageWriteParam.canWriteCompressed ()) {
      imageWriteParam.setCompressionMode (ImageWriteParam.MODE_EXPLICIT);
      imageWriteParam.setCompressionQuality (quality);
    }
    try {
      File file = new File (name);
      ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream (file);
      imageWriter.setOutput (imageOutputStream);
      imageWriter.write (null, new IIOImage (image, null, null), imageWriteParam);
      imageOutputStream.close ();
    } catch (Exception e) {
      //e.printStackTrace ();
      return false;
    }
    return true;
  }  //saveImage(BufferedImage,String,float)



  //========================================================================================
  //$$ICO アイコンファイル
  //  サイズの異なる複数のアイコンを格納することができる
  //  ワードサイズとロングサイズのデータはすべてリトルエンディアン
  //
  //  アイコンファイル(.ico)
  //    ファイルヘッダ
  //    エントリデータ0
  //           :
  //    エントリデータn-1
  //    イメージデータ0
  //           :
  //    イメージデータn-1
  //
  //  ファイルヘッダ
  //    0000  .iw  0=予約
  //    0002  .iw  1=アイコン
  //    0004  .iw  n=アイコン数
  //    0006
  //
  //  エントリデータ
  //    0000  .b   幅
  //    0001  .b   高さ
  //    0002  .b   色数(0=256以上)
  //    0003  .b   0=予約
  //    0004  .iw  1=プレーン数
  //    0006  .iw  ピクセル毎のビット数
  //    0008  .il  イメージデータの長さ
  //    000c  .il  ファイルヘッダからイメージデータまでのオフセット
  //    0010
  //
  //  イメージデータ
  //    イメージヘッダ
  //    パレットテーブル
  //    パターンデータ
  //    マスクデータ
  //
  //  イメージヘッダ
  //    0000  .il  40=イメージヘッダの長さ
  //    0004  .il  幅
  //    0008  .il  高さ*2
  //    000c  .iw  1=プレーン数
  //    000e  .iw  ピクセル毎のビット数
  //    0010  .il  0=無圧縮
  //    0014  .il  0=画像データサイズ(省略)
  //    0018  .il  0=横解像度(省略)
  //    001c  .il  0=縦解像度(省略)
  //    0020  .il  p=パレット数
  //    0024  .il  0=重要なパレットのインデックス(省略)
  //    0028
  //
  //  パレットテーブル
  //    0000  .il  パレット0(BGR0)
  //                   :
  //          .il  パレットp-1(BGR0)
  //    4*p
  //
  //  パターンデータ
  //    ピクセルの順序は左下から右上
  //    バイト内のビットの順序は上位から下位
  //    1ラインのデータ長は4の倍数
  //
  //  マスクデータ
  //    0=描画,1=透過
  //    ピクセルの順序は左下から右上
  //    バイト内のビットの順序は上位から下位
  //    1ラインのデータ長は4の倍数

  //success = saveIcon (name, image, ...);
  //  アイコンファイルを出力する
  public static boolean saveIcon (String fileName, BufferedImage... arrayImage) {
    int iconCount = arrayImage.length;
    int[][] arrayPaletTable = new int[iconCount][];  //パレットテーブル
    int[] arrayPaletCount = new int[iconCount];  //パレット数。0=パレットを使わない
    int[] arrayPixelBits = new int[iconCount];  //ピクセル毎のビット数。24,8,4,2,1のいずれか
    int[] arrayPatternLineSize = new int[iconCount];  //パターンデータの1ラインのバイト数
    int[] arrayMaskLineSize = new int[iconCount];  //マスクデータの1ラインのバイト数
    int[] arrayImageSize = new int[iconCount];  //イメージデータの長さ
    int[] arrayImageOffset = new int[iconCount];  //ファイルヘッダからイメージデータまでのオフセット
    int fileSize = 6 + 16 * iconCount;
    for (int iconNumber = 0; iconNumber < iconCount; iconNumber++) {
      BufferedImage image = arrayImage[iconNumber];
      int width = image.getWidth ();
      int height = image.getHeight ();
      //パレットテーブルを作る
      int[] paletTable = new int[256];
      int paletCount = 0;
    countPalet:
      for (int y = height - 1; y >= 0; y--) {
        for (int x = 0; x < width; x++) {
          int rgb = image.getRGB (x, y);
          if (rgb >>> 24 != 0xff) {  //alphaが0xffでなければred,green,blueを無視して透過色とみなす
            continue;
          }
          int l = 0;
          int r = paletCount;
          while (l < r) {
            int m = l + r >> 1;
            if (paletTable[m] < rgb) {
              l = m + 1;
            } else {
              r = m;
            }
          }
          if (l == paletCount || paletTable[l] != rgb) {  //新しい色
            if (paletCount == 256) {  //色数が多すぎる
              paletCount = 0;
              break countPalet;
            }
            for (int i = paletCount; i > l; i--) {
              paletTable[i] = paletTable[i - 1];
            }
            paletTable[l] = rgb;
            paletCount++;
          }
        }  //for x
      }  //for y
      int pixelBits = (paletCount == 0 ? 24 :
                       paletCount > 16 ? 8 :
                       paletCount > 4 ? 4 :
                       paletCount > 2 ? 2 :
                       1);
      int patternLineSize = pixelBits * width + 31 >> 5 << 2;
      int maskLineSize = width + 31 >> 5 << 2;
      int imageSize = 40 + 4 * paletCount + patternLineSize * height + maskLineSize * height;
      arrayPaletTable[iconNumber] = paletTable;
      arrayPaletCount[iconNumber] = paletCount;
      arrayPixelBits[iconNumber] = pixelBits;
      arrayPatternLineSize[iconNumber] = patternLineSize;
      arrayMaskLineSize[iconNumber] = maskLineSize;
      arrayImageSize[iconNumber] = imageSize;
      arrayImageOffset[iconNumber] = fileSize;
      fileSize += imageSize;
    }  //for iconNumber
    byte[] bb = new byte[fileSize];
    //ファイルヘッダ
    ByteArray.byaWiw (bb, 0, 0);
    ByteArray.byaWiw (bb, 2, 1);
    ByteArray.byaWiw (bb, 4, iconCount);
    for (int iconNumber = 0; iconNumber < iconCount; iconNumber++) {
      BufferedImage image = arrayImage[iconNumber];
      int width = image.getWidth ();
      int height = image.getHeight ();
      int[] paletTable = arrayPaletTable[iconNumber];
      int paletCount = arrayPaletCount[iconNumber];
      int pixelBits = arrayPixelBits[iconNumber];
      int patternLineSize = arrayPatternLineSize[iconNumber];
      int maskLineSize = arrayMaskLineSize[iconNumber];
      int imageSize = arrayImageSize[iconNumber];
      int imageOffset = arrayImageOffset[iconNumber];
      //エントリデータ
      int o = 6 + 16 * iconNumber;
      ByteArray.byaWb (bb, o, width);
      ByteArray.byaWb (bb, o + 1, height);
      ByteArray.byaWb (bb, o + 2, paletCount);
      ByteArray.byaWb (bb, o + 3, 0);
      ByteArray.byaWiw (bb, o + 4, 1);
      ByteArray.byaWiw (bb, o + 6, pixelBits);
      ByteArray.byaWil (bb, o + 8, imageSize);
      ByteArray.byaWil (bb, o + 12, imageOffset);
      //イメージヘッダ
      o = imageOffset;
      ByteArray.byaWil (bb, o, 40);
      ByteArray.byaWil (bb, o + 4, width);
      ByteArray.byaWil (bb, o + 8, height * 2);
      ByteArray.byaWiw (bb, o + 12, 1);
      ByteArray.byaWiw (bb, o + 14, pixelBits);
      ByteArray.byaWil (bb, o + 16, 0);
      ByteArray.byaWil (bb, o + 20, 0);
      ByteArray.byaWil (bb, o + 24, 0);
      ByteArray.byaWil (bb, o + 28, 0);
      ByteArray.byaWil (bb, o + 32, paletCount);
      ByteArray.byaWil (bb, o + 36, 0);
      //パレットテーブル
      o += 40;
      for (int i = 0; i < paletCount; i++) {
        ByteArray.byaWil (bb, o, paletTable[i] & 0x00ffffff);
        o += 4;
      }
      //パターンデータ
      for (int y = height - 1; y >= 0; y--) {
        for (int x = 0; x < width; x++) {
          int rgb = image.getRGB (x, y);
          if (rgb >>> 24 != 0xff) {  //alphaが0xffでなければred,green,blueを無視して透過色とみなす
            continue;
          }
          if (pixelBits == 24) {  //パレットなし
            bb[o + 3 * x] = (byte) rgb;  //blue
            bb[o + 3 * x + 1] = (byte) (rgb >> 8);  //green
            bb[o + 3 * x + 2] = (byte) (rgb >> 16);  //red
            continue;
          }
          int l = 0;
          int r = paletCount;
          while (l < r) {
            int m = l + r >> 1;
            if (paletTable[m] < rgb) {
              l = m + 1;
            } else {
              r = m;
            }
          }
          if (l != 0) {
            if (pixelBits == 8) {
              bb[o + x] = (byte) l;
            } else if (pixelBits == 4) {
              bb[o + (x >> 1)] |= (byte) (l << ((~x & 1) << 2));
            } else if (pixelBits == 2) {
              bb[o + (x >> 2)] |= (byte) (l << ((~x & 3) << 1));
            } else {
              bb[o + (x >> 3)] |= (byte) (l << (~x & 7));
            }
          }
        }  //for x
        o += patternLineSize;
      }  //for y
      //マスクデータ
      for (int y = height - 1; y >= 0; y--) {
        for (int x = 0; x < width; x++) {
          int rgb = image.getRGB (x, y);
          if (rgb >>> 24 != 0xff) {  //alphaが0xffでなければred,green,blueを無視して透過色とみなす
            bb[o + (x >> 3)] |= (byte) (1 << (~x & 7));
          }
        }
        o += maskLineSize;
      }
    }  //for iconNumber
    return rscPutFile (fileName, bb, 0, fileSize);
  }  //saveIcon(String,BufferedImage...)



}  //class XEiJ



