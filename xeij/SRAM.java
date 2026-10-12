//========================================================================================
//  SRAM.java
//    en:SRAM
//    ja:SRAM
//  Copyright (C) 2003-2026 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.awt.event.*;  //ActionEvent,ActionListener
import java.io.*;  //File
import java.util.*;  //Arrays
import javax.swing.*;  //JMenu,JRadioButtonMenuItem

public class SRAM {

  public static int smrSramSizeRequest;  //SRAMのサイズ。16384,32768,65536のいずれか
  public static int smrSramCurrentSize;  //現在のSRAMのサイズ。16384,32768,65536のいずれか

  public static String smrSramName;  //SRAMイメージファイル名

  public static int smrBootDevice;  //起動デバイス。0x00ed0018。0x0000=STD,0x9070～0x9370=FDn,0x8000～0x8f00=HDn,0xa000=ROM,0xb000=RAM,-1=設定しない
  public static int smrROMBootHandle;  //ROM起動ハンドル。0x00ed000c。-1=設定しない
  public static int smrRAMBootAddress;  //RAM起動アドレス。0x00ed0010。-1=設定しない

  public static int smrRepeatDelay;  //リピート開始。0x00ed003a。-1=設定しない,0..15=200+100*n(ms)
  public static int smrRepeatInterval;  //リピート間隔。0x00ed003b。-1=設定しない,0..15=30+5*n^2(ms)
  public static boolean smrWriteEnableOn;  //true=SRAM書き込み可

  //-romdb
  public static int smrRomdbFlag;  //ROMデバッガ起動フラグ。0x00ed0058。-1=設定しない,0=OFF,255=AUX,1=CON(IPLROM 1.6のみ)

  //起動時のキャッシュモード
  public static int smrCacheMode;

  //起動音
  public static int smrStupsndMode;

  //-setmemorysize
  public static boolean smrModifyMemorySizeOn;  //SRAMにあるメモリサイズを修正する

  //-srambuserror
  public static boolean smrSRAMBusErrorOn;  //SRAMへの書き込み時のバスエラー

  public static JMenu smrRepeatDelayMenu;  //リピート開始メニュー
  public static JMenu smrRepeatIntervalMenu;  //リピート間隔メニュー
  public static JMenu smrRomdbMenu;  //ROMデバッガ起動フラグメニュー
  public static JMenu smrCacheMenu;  //キャッシュメニュー
  public static JMenu smrStupsndMenu;  //起動音
  public static JMenuItem smrModifyMemorySizeMenuItem;  //メモリサイズ修正
  public static JMenu smrMenu;  //SRAMメニュー
  public static JMenu smrBootMenu;  //起動デバイスメニュー
  public static JRadioButtonMenuItem smrSTDMenuItem;

  //smrInit ()
  //  SRAMを初期化する
  public static void smrInit () {

    //-sramsize
    int sizeKB = Settings.sgsGetInt ("sramsize");  //SRAMのサイズ
    smrSramSizeRequest = (sizeKB == 16 ||
                          sizeKB == 32 ||
                          sizeKB == 64 ? sizeKB << 10 : 16 << 10);
    smrSramCurrentSize = smrSramSizeRequest;
    System.out.printf (Multilingual.mlnJapanese ?
                       "SRAM のサイズは %dKB です\n" :
                       "SRAM size is %dKB\n",
                       smrSramSizeRequest >> 10);

    boolean initialized = false;

    //-sram
    smrSramName = "";
    if (!initialized) {
      smrSramName = Settings.sgsGetString ("sram");  //SRAMイメージファイル名
      if (smrSramName.length () != 0) {
        byte[] array = XEiJ.rscGetFile (smrSramName, smrSramSizeRequest);
        if (array != null) {
          System.arraycopy (array, 0, MainMemory.mmrM8, 0x00ed0000, smrSramSizeRequest);
          if (smrSramSizeRequest < 64 << 10) {
            Arrays.fill (MainMemory.mmrM8, 0x00ed0000 + smrSramSizeRequest, 0x00ed0000 + (64 << 10), (byte) 0);  //範囲外をゼロクリアする
          }
          initialized = true;
        }
      }
    }

    //-sramdata
    //  マジックの有無に関係なく復元する
    if (!initialized) {
      byte[] array = Settings.sgsGetData ("sramdata");  //SRAMの内容(gzip+base64)
      if (array.length != 0) {  //復元するデータがある
        System.out.println (Multilingual.mlnJapanese ?
                            "SRAM のデータを復元します" :
                            "SRAM data is restored");
        System.arraycopy (array, 0, MainMemory.mmrM8, 0x00ed0000, Math.min (array.length, smrSramSizeRequest));
        if (array.length < smrSramSizeRequest) {
          Arrays.fill (MainMemory.mmrM8, 0x00ed0000 + array.length, 0x00ed0000 + smrSramSizeRequest, (byte) 0);  //復元されなかった部分をゼロクリアする
        }
        if (smrSramSizeRequest < 64 << 10) {
          Arrays.fill (MainMemory.mmrM8, 0x00ed0000 + smrSramSizeRequest, 0x00ed0000 + (64 << 10), (byte) 0);  //範囲外をゼロクリアする
        }
        initialized = true;
      }
    }

    if (!initialized) {
      System.out.println (Multilingual.mlnJapanese ?
                          "SRAM をゼロクリアします" :
                          "SRAM is zero-cleared");
      Arrays.fill (MainMemory.mmrM8, 0x00ed0000, 0x00ed0000 + (64 << 10), (byte) 0);  //ゼロクリアする
      initialized = true;
    }

    //-keydly
    int repeatDelay = Settings.sgsGetInt ("keydly");
    smrRepeatDelay = -1 <= repeatDelay && repeatDelay <= 15 ? repeatDelay : -1;

    //-keyrep
    int repeatInterval = Settings.sgsGetInt ("keyrep");
    smrRepeatInterval = -1 <= repeatInterval && repeatInterval <= 15 ? repeatInterval : -1;

    //-romdb
    {
      String s = Settings.sgsGetString ("romdb").toLowerCase ();
      smrRomdbFlag = (s.equals ("off") ? 0 :
                      s.equals ("on") || s.equals ("aux") ? 255 :
                      s.equals ("con") ? 1 :
                      -1);
    }

    //起動時のキャッシュモード
    smrCacheMode = smrStringToCache (Settings.sgsGetString ("cache"));

    //起動音
    smrStupsndMode = smrStringToStupsnd (Settings.sgsGetString ("stupsnd"));

    //-modifymemorysize
    smrModifyMemorySizeOn = Settings.sgsGetOnOff ("modifymemorysize");

    //-srambuserror
    smrSRAMBusErrorOn = Settings.sgsGetOnOff ("srambuserror");

    //-boot
    smrParseBootDevice (Settings.sgsGetString ("boot"));

  }  //smrInit

  //smrTini ()
  //  SRAMの後始末
  public static void smrTini () {

    //-sramsize
    Settings.sgsPutInt ("sramsize", smrSramSizeRequest >> 10);

    //-sram
    Settings.sgsPutString ("sram", smrSramName);

    //-sramdata
    //  マジックの有無に関係なく保存する
    //  常に64KB保存する
    Settings.sgsPutData ("sramdata", MainMemory.mmrM8, 0x00ed0000, 64 << 10);

    //-keydly
    Settings.sgsPutInt ("keydly", smrRepeatDelay);  //リピート開始

    //-keyrep
    Settings.sgsPutInt ("keyrep", smrRepeatInterval);  //リピート間隔

    //-romdb
    Settings.sgsPutString ("romdb",
                           smrRomdbFlag == 0 ? "off" :
                           smrRomdbFlag == 255 ? "aux" :
                           smrRomdbFlag == 1 ? "con" :
                           "");

    //起動時のキャッシュモード
    Settings.sgsPutString ("cache", smrCacheToString (smrCacheMode));

    //起動音
    Settings.sgsPutString ("stupsnd", smrStupsndToString (smrStupsndMode));

    //-modifymemorysize
    Settings.sgsPutOnOff ("modifymemorysize", smrModifyMemorySizeOn);

    //-srambuserror
    Settings.sgsPutOnOff ("srambuserror", smrSRAMBusErrorOn);

    //-boot
    Settings.sgsPutString ("boot",
                           smrBootDevice == -1 ? "default" :
                           smrBootDevice == 0x0000 ? "std" :
                           (smrBootDevice & 0xf000) == 0x9000 ? "fd" + (smrBootDevice >> 8 & 3) :
                           (smrBootDevice & 0xf000) == 0x8000 ? "hd" + (smrBootDevice >> 8 & 15) :
                           smrBootDevice == 0xa000 ?
                           (smrROMBootHandle & ~(7 << 2)) == SPC.SPC_HANDLE_EX ? "sc" + (smrROMBootHandle >> 2 & 7) :
                           (smrROMBootHandle & ~(7 << 2)) == SPC.SPC_HANDLE_IN ? "sc" + (smrROMBootHandle >> 2 & 7) :
                           smrROMBootHandle == HFS.HFS_BOOT_HANDLE ? "hf" + HFS.hfsBootUnit :
                           String.format ("rom$%08X", smrROMBootHandle) :
                           smrBootDevice == 0xb000 ? String.format ("ram$%08X", smrRAMBootAddress) :
                           "");

  }

  //smrMakeMenu ()
  //  mnbMakeMenuより前
  //  mpuMakeMenuより前
  public static void smrMakeMenu () {

    //アクションリスナー
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        String command = ae.getActionCommand ();
        switch (command) {
          //メモリサイズ修正
        case "Modify the memory size in SRAM":  //SRAM にあるメモリサイズを修正する
          smrModifyMemorySizeOn = ((JCheckBoxMenuItem) source).isSelected ();
          break;
          //SRAMメニュー
        case "Zero-clear":  //SRAM をゼロクリア
          smrClear ();
          break;
        case "Import":  //SRAM をインポート
          smrLoad ();
          break;
        case "Export":  //SRAM をエクスポート
          smrSave ();
          break;
        case "16KB":
          smrSramSizeRequest = 16384;
          break;
        case "32KB":
          smrSramSizeRequest = 32768;
          break;
        case "64KB":
          smrSramSizeRequest = 65536;
          break;
        case "Bus error when writing to SRAM":  //SRAM への書き込み時のバスエラー
          smrSRAMBusErrorOn = ((JCheckBoxMenuItem) source).isSelected ();
          break;
        default:
          System.out.println ("unknown action command " + command);
        }
      }
    };

    //リピート開始メニュー
    ActionListener delayListener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        String command = ae.getActionCommand ();
        switch (command) {
        case "Follow settings in SWITCH.X":  //SWITCH.X の設定に従う
          smrRepeatDelay = -1;
          Keyboard.kbdSetRepeatDelay (MainMemory.mmrRbs (0x00ed003a));
          break;
        case (200 + 100 *  0) + "ms":
        case (200 + 100 *  1) + "ms":
        case (200 + 100 *  2) + "ms":
        case (200 + 100 *  3) + "ms":
        case (200 + 100 *  4) + "ms":
        case (200 + 100 *  5) + "ms":
        case (200 + 100 *  6) + "ms":
        case (200 + 100 *  7) + "ms":
        case (200 + 100 *  8) + "ms":
        case (200 + 100 *  9) + "ms":
        case (200 + 100 * 10) + "ms":
        case (200 + 100 * 11) + "ms":
        case (200 + 100 * 12) + "ms":
        case (200 + 100 * 13) + "ms":
        case (200 + 100 * 14) + "ms":
        case (200 + 100 * 15) + "ms":
          {
            int ms = Integer.parseInt (command.substring (0, command.length () - 2));  //200..1700
            //perl optdiv.pl 1500 100
            //  x/100==x*1311>>>17 (0<=x<=4698) [1500*1311==1966500]
            smrRepeatDelay = (ms - 200) * 1311 >>> 17;  //0..15
            Keyboard.kbdSetRepeatDelay (smrRepeatDelay);
            MainMemory.mmrWb (0x00ed003a, smrRepeatDelay);
          }
          break;
        default:
          System.out.println ("unknown action command " + command);
        }
      }
    };
    ButtonGroup delayGroup = new ButtonGroup ();
    smrRepeatDelayMenu =
      Multilingual.mlnText (
        ComponentFactory.createMenu (
          "Repeat delay",
          Multilingual.mlnText (
            ComponentFactory.createRadioButtonMenuItem (delayGroup, smrRepeatDelay == -1, "Follow settings in SWITCH.X", delayListener),
            "ja", "SWITCH.X の設定に従う"),
          ComponentFactory.createRadioButtonMenuItem (delayGroup, smrRepeatDelay ==  0, (200 + 100 *  0) + "ms", delayListener),
          ComponentFactory.createRadioButtonMenuItem (delayGroup, smrRepeatDelay ==  1, (200 + 100 *  1) + "ms", delayListener),
          ComponentFactory.createRadioButtonMenuItem (delayGroup, smrRepeatDelay ==  2, (200 + 100 *  2) + "ms", delayListener),
          ComponentFactory.createRadioButtonMenuItem (delayGroup, smrRepeatDelay ==  3, (200 + 100 *  3) + "ms", delayListener),
          ComponentFactory.createRadioButtonMenuItem (delayGroup, smrRepeatDelay ==  4, (200 + 100 *  4) + "ms", delayListener),
          ComponentFactory.createRadioButtonMenuItem (delayGroup, smrRepeatDelay ==  5, (200 + 100 *  5) + "ms", delayListener),
          ComponentFactory.createRadioButtonMenuItem (delayGroup, smrRepeatDelay ==  6, (200 + 100 *  6) + "ms", delayListener),
          ComponentFactory.createRadioButtonMenuItem (delayGroup, smrRepeatDelay ==  7, (200 + 100 *  7) + "ms", delayListener),
          ComponentFactory.createRadioButtonMenuItem (delayGroup, smrRepeatDelay ==  8, (200 + 100 *  8) + "ms", delayListener),
          ComponentFactory.createRadioButtonMenuItem (delayGroup, smrRepeatDelay ==  9, (200 + 100 *  9) + "ms", delayListener),
          ComponentFactory.createRadioButtonMenuItem (delayGroup, smrRepeatDelay == 10, (200 + 100 * 10) + "ms", delayListener),
          ComponentFactory.createRadioButtonMenuItem (delayGroup, smrRepeatDelay == 11, (200 + 100 * 11) + "ms", delayListener),
          ComponentFactory.createRadioButtonMenuItem (delayGroup, smrRepeatDelay == 12, (200 + 100 * 12) + "ms", delayListener),
          ComponentFactory.createRadioButtonMenuItem (delayGroup, smrRepeatDelay == 13, (200 + 100 * 13) + "ms", delayListener),
          ComponentFactory.createRadioButtonMenuItem (delayGroup, smrRepeatDelay == 14, (200 + 100 * 14) + "ms", delayListener),
          ComponentFactory.createRadioButtonMenuItem (delayGroup, smrRepeatDelay == 15, (200 + 100 * 15) + "ms", delayListener)
          ),
        "ja", "リピート開始");

    //リピート間隔メニュー
    ActionListener intervalListener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        String command = ae.getActionCommand ();
        switch (command) {
        case "Follow settings in SWITCH.X":  //SWITCH.X の設定に従う
          smrRepeatInterval =  -1;
          Keyboard.kbdSetRepeatInterval (MainMemory.mmrRbs (0x00ed003b));
          break;
        case (30 + 5 *  0 *  0) + "ms":
        case (30 + 5 *  1 *  1) + "ms":
        case (30 + 5 *  2 *  2) + "ms":
        case (30 + 5 *  3 *  3) + "ms":
        case (30 + 5 *  4 *  4) + "ms":
        case (30 + 5 *  5 *  5) + "ms":
        case (30 + 5 *  6 *  6) + "ms":
        case (30 + 5 *  7 *  7) + "ms":
        case (30 + 5 *  8 *  8) + "ms":
        case (30 + 5 *  9 *  9) + "ms":
        case (30 + 5 * 10 * 10) + "ms":
        case (30 + 5 * 11 * 11) + "ms":
        case (30 + 5 * 12 * 12) + "ms":
        case (30 + 5 * 13 * 13) + "ms":
        case (30 + 5 * 14 * 14) + "ms":
        case (30 + 5 * 15 * 15) + "ms":
          {
            int ms = Integer.parseInt (command.substring (0, command.length () - 2));  //30..1155
            //perl optdiv.pl 1125 5
            //  x/5==x*1639>>>13 (0<=x<=2733) [1125*1639==1843875]
            smrRepeatInterval = (int) Math.sqrt ((double) ((ms - 30) * 1639 >>> 13));  //0..15
            Keyboard.kbdSetRepeatInterval (smrRepeatInterval);
            MainMemory.mmrWb (0x00ed003b, smrRepeatInterval);
          }
          break;
        default:
          System.out.println ("unknown action command " + command);
        }
      }
    };
    ButtonGroup intervalGroup = new ButtonGroup ();
    smrRepeatIntervalMenu =
      Multilingual.mlnText (
        ComponentFactory.createMenu (
          "Repeat interval",
          Multilingual.mlnText (
            ComponentFactory.createRadioButtonMenuItem (intervalGroup, smrRepeatInterval == -1, "Follow settings in SWITCH.X", intervalListener),
            "ja", "SWITCH.X の設定に従う"),
          ComponentFactory.createRadioButtonMenuItem (intervalGroup, smrRepeatInterval ==  0, (30 + 5 *  0 *  0) + "ms", intervalListener),
          ComponentFactory.createRadioButtonMenuItem (intervalGroup, smrRepeatInterval ==  1, (30 + 5 *  1 *  1) + "ms", intervalListener),
          ComponentFactory.createRadioButtonMenuItem (intervalGroup, smrRepeatInterval ==  2, (30 + 5 *  2 *  2) + "ms", intervalListener),
          ComponentFactory.createRadioButtonMenuItem (intervalGroup, smrRepeatInterval ==  3, (30 + 5 *  3 *  3) + "ms", intervalListener),
          ComponentFactory.createRadioButtonMenuItem (intervalGroup, smrRepeatInterval ==  4, (30 + 5 *  4 *  4) + "ms", intervalListener),
          ComponentFactory.createRadioButtonMenuItem (intervalGroup, smrRepeatInterval ==  5, (30 + 5 *  5 *  5) + "ms", intervalListener),
          ComponentFactory.createRadioButtonMenuItem (intervalGroup, smrRepeatInterval ==  6, (30 + 5 *  6 *  6) + "ms", intervalListener),
          ComponentFactory.createRadioButtonMenuItem (intervalGroup, smrRepeatInterval ==  7, (30 + 5 *  7 *  7) + "ms", intervalListener),
          ComponentFactory.createRadioButtonMenuItem (intervalGroup, smrRepeatInterval ==  8, (30 + 5 *  8 *  8) + "ms", intervalListener),
          ComponentFactory.createRadioButtonMenuItem (intervalGroup, smrRepeatInterval ==  9, (30 + 5 *  9 *  9) + "ms", intervalListener),
          ComponentFactory.createRadioButtonMenuItem (intervalGroup, smrRepeatInterval == 10, (30 + 5 * 10 * 10) + "ms", intervalListener),
          ComponentFactory.createRadioButtonMenuItem (intervalGroup, smrRepeatInterval == 11, (30 + 5 * 11 * 11) + "ms", intervalListener),
          ComponentFactory.createRadioButtonMenuItem (intervalGroup, smrRepeatInterval == 12, (30 + 5 * 12 * 12) + "ms", intervalListener),
          ComponentFactory.createRadioButtonMenuItem (intervalGroup, smrRepeatInterval == 13, (30 + 5 * 13 * 13) + "ms", intervalListener),
          ComponentFactory.createRadioButtonMenuItem (intervalGroup, smrRepeatInterval == 14, (30 + 5 * 14 * 14) + "ms", intervalListener),
          ComponentFactory.createRadioButtonMenuItem (intervalGroup, smrRepeatInterval == 15, (30 + 5 * 15 * 15) + "ms", intervalListener)
          ),
        "ja", "リピート間隔");

    //ROMデバッガ起動フラグメニュー
    ActionListener romdbListener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        String command = ae.getActionCommand ();
        switch (command) {
        case "Follow settings in SWITCH.X":  //SWITCH.X の設定に従う
          smrRomdbFlag = -1;
          break;
        case "OFF":
          smrRomdbFlag = 0;
          break;
        case "AUX":
          smrRomdbFlag = 255;
          break;
        case "CON (when using IPLROM 1.6)":  //CON (IPLROM 1.6 使用時)
          smrRomdbFlag = 1;
          break;
        default:
          System.out.println ("unknown action command " + command);
        }
      }
    };
    ButtonGroup romdbGroup = new ButtonGroup ();
    smrRomdbMenu =
      Multilingual.mlnText (
        ComponentFactory.createMenu (
          "ROM debugger start flag",
          Multilingual.mlnText (
            ComponentFactory.createRadioButtonMenuItem (romdbGroup, smrRomdbFlag == -1, "Follow settings in SWITCH.X", romdbListener),
            "ja", "SWITCH.X の設定に従う"),
          ComponentFactory.createRadioButtonMenuItem (romdbGroup, smrRomdbFlag == 0, "OFF", romdbListener),
          ComponentFactory.createRadioButtonMenuItem (romdbGroup, smrRomdbFlag == 255, "AUX", romdbListener),
          Multilingual.mlnText (
            ComponentFactory.createRadioButtonMenuItem (romdbGroup, smrRomdbFlag == 1, "CON (when using IPLROM 1.6)", romdbListener),
            "ja", "CON (IPLROM 1.6 使用時)")
          ),
        "ja", "ROM デバッガ起動フラグ");

    //起動時のキャッシュモード
    //  SRAM_CACHE  $00ED0090
    //  cache
    //    -1  none     CACHE.X の設定に従う
    //     0  off-off  データキャッシュと命令キャッシュは無効
    //     1  off-on   データキャッシュは無効、命令キャッシュは有効
    //     2  on-off   データキャッシュは有効、命令キャッシュは無効
    //     3  on-on    データキャッシュと命令キャッシュは有効
    ActionListener cacheListener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        String command = ae.getActionCommand ();
        switch (command) {
        case "Follow settings in CACHE.X":
          smrCacheMode = -1;
          break;
        case "Disabled":
          smrCacheMode = 0;
          break;
        case "Data cache disabled and instruction cache enabled":
          smrCacheMode = 1;
          break;
        case "Data cache enabled and instruction cache disabled":
          smrCacheMode = 2;
          break;
        case "Enabled":
          smrCacheMode = 3;
          break;
        }
      }
    };
    ButtonGroup cacheGroup = new ButtonGroup ();
    smrCacheMenu =
      Multilingual.mlnText (
        ComponentFactory.createMenu (
          "Cache mode at startup",
          Multilingual.mlnText (
            ComponentFactory.createRadioButtonMenuItem (
              cacheGroup,
              smrCacheMode == -1, "Follow settings in CACHE.X",
              cacheListener),
            "ja", "CACHE.X の設定に従う"),
          Multilingual.mlnText (
            ComponentFactory.createRadioButtonMenuItem (
              cacheGroup,
              smrCacheMode == 0, "Disabled",
              cacheListener),
            "ja", "無効"),
          Multilingual.mlnText (
            ComponentFactory.createRadioButtonMenuItem (
              cacheGroup,
              smrCacheMode == 1, "Data cache disabled and instruction cache enabled",
              cacheListener),
            "ja", "データキャッシュは無効、命令キャッシュは有効"),
          Multilingual.mlnText (
            ComponentFactory.createRadioButtonMenuItem (
              cacheGroup,
              smrCacheMode == 2, "Data cache enabled and instruction cache disabled",
              cacheListener),
            "ja", "データキャッシュは有効、命令キャッシュは無効"),
          Multilingual.mlnText (
            ComponentFactory.createRadioButtonMenuItem (
              cacheGroup,
              smrCacheMode == 3, "Enabled",
              cacheListener),
            "ja", "有効")
          ),
        "ja", "起動時のキャッシュモード");

    //起動音
    //  SRAM_STARTUP_SOUND  $00ED0091
    //  stupsnd
    //    -1  none  stupsnd.x の設定に従う
    //     0  off
    //     1  on
    //              12 o1c   28 o2c   44 o3c   60 o4c   76 o5c    92 o6c   108 o7c   124 o8c
    //              13 o1c#  29 o2c#  45 o3c#  61 o4c#  77 o5c#   93 o6c#  109 o7c#  125 o8c#
    //              14 o1d   30 o2d   46 o3d   62 o4d   78 o5d    94 o6d   110 o7d   126 o8d
    //              16 o1d#  32 o2d#  48 o3d#  64 o4d#  80 o5d#   96 o6d#  112 o7d#
    //              17 o1e   33 o2e   49 o3e   65 o4e   81 o5e    97 o6e   113 o7e
    //     2  o0f   18 o1f   34 o2f   50 o3f   66 o4f   82 o5f    98 o6f   114 o7f
    //     4  o0f#  20 o1f#  36 o2f#  52 o3f#  68 o4f#  84 o5f#  100 o6f#  116 o7f#
    //     5  o0g   21 o1g   37 o2g   53 o3g   69 o4g   85 o5g   101 o6g   117 o7g
    //     6  o0g#  22 o1g#  38 o2g#  54 o3g#  70 o4g#  86 o5g#  102 o6g#  118 o7g#
    //     8  o0a   24 o1a   40 o2a   56 o3a   72 o4a   88 o5a   104 o6a   120 o7a
    //     9  o0a#  25 o1a#  41 o2a#  57 o3a#  73 o4a#  89 o5a#  105 o6a#  121 o7a#
    //    10  o0b   26 o1b   42 o2b   58 o3b   74 o4b   90 o5b   106 o6b   122 o7b
    ActionListener stupsndListener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        String command = ae.getActionCommand ();
        switch (command) {
        case "Follow settings in stupsnd.x":
          smrStupsndMode = -1;
          break;
        case "Disabled":
          smrStupsndMode = 0;
          break;
        case "Enabled (o5c)":
          smrStupsndMode = 1;
          break;
        default:
          smrStupsndMode = smrStringToStupsnd (command);
        }
      }
    };
    ButtonGroup stupsndGroup = new ButtonGroup ();
    smrStupsndMenu =
      Multilingual.mlnText (
        ComponentFactory.createMenu (
          "Startup sound",
          Multilingual.mlnText (
            ComponentFactory.createRadioButtonMenuItem (
              stupsndGroup,
              smrStupsndMode == -1, "Follow settings in stupsnd.x",
              stupsndListener),
            "ja", "stupsnd.x の設定に従う"),
          Multilingual.mlnText (
            ComponentFactory.createRadioButtonMenuItem (
              stupsndGroup,
              smrStupsndMode == 0, "Disabled",
              stupsndListener),
            "ja", "無効"),
          Multilingual.mlnText (
            ComponentFactory.createRadioButtonMenuItem (
              stupsndGroup,
              smrStupsndMode == 1, "Enabled (o5c)",
              stupsndListener),
            "ja", "有効 (o5c)")
          ),
        "ja", "起動音");
    JMenu stupsndSubMenu = null;
    for (int mode = 2; mode <= 126; mode++) {  //127は不可
      if (mode % 4 == 3) {
        continue;
      }
      if (mode == 2 || (mode + 4) % 16 == 0) {
        stupsndSubMenu = ComponentFactory.createMenu ("o" + (mode + 4) / 16);
        smrStupsndMenu.add (stupsndSubMenu);
      }
      stupsndSubMenu.add (
        ComponentFactory.createRadioButtonMenuItem (
          stupsndGroup,
          smrStupsndMode == mode, smrStupsndToString (mode),
          stupsndListener)
        );
    }

    //メモリサイズ修正
    smrModifyMemorySizeMenuItem =
      Multilingual.mlnText (
        ComponentFactory.createCheckBoxMenuItem (smrModifyMemorySizeOn, "Modify the memory size in SRAM", listener),
        "ja", "SRAM にあるメモリサイズを修正する");

    //SRAMメニュー
    ButtonGroup sizeGroup = new ButtonGroup ();
    smrMenu =
      ComponentFactory.createMenu (
        "SRAM",
        Multilingual.mlnText (
          ComponentFactory.createMenuItem ("Zero-clear", listener),
          "ja", "ゼロクリア"),
        Multilingual.mlnText (
          ComponentFactory.createMenuItem ("Import", listener),
          "ja", "インポート"),
        Multilingual.mlnText (
          ComponentFactory.createMenuItem ("Export", listener),
          "ja", "エクスポート"),
        ComponentFactory.createHorizontalSeparator (),
        Multilingual.mlnText (
          ComponentFactory.createRadioButtonMenuItem (sizeGroup, smrSramSizeRequest >> 10 == 16, "16KB", listener),
          "ja", "16KB"),
        Multilingual.mlnText (
          ComponentFactory.createRadioButtonMenuItem (sizeGroup, smrSramSizeRequest >> 10 == 32, "32KB", listener),
          "ja", "32KB"),
        Multilingual.mlnText (
          ComponentFactory.createRadioButtonMenuItem (sizeGroup, smrSramSizeRequest >> 10 == 64, "64KB", listener),
          "ja", "64KB"),
        ComponentFactory.createHorizontalSeparator (),
        Multilingual.mlnText (
          ComponentFactory.createCheckBoxMenuItem (smrSRAMBusErrorOn, "Bus error when writing to SRAM", listener),
          "ja", "SRAM への書き込み時のバスエラー")
        );

    //起動デバイスメニュー
    ButtonGroup bootGroup = new ButtonGroup ();
    ActionListener bootListener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        String command = ae.getActionCommand ();
        smrParseBootDevice (
          command.startsWith ("FDD ") ? "fd" + command.substring (4) :
          command.startsWith ("SASI ") ? "hd" + command.substring (5) :
          command.startsWith ("SCSI ") ? "sc" + command.substring (5) :
          command.startsWith ("HFS ") ? "hf" + command.substring (4) :
          command.equals ("STD") ? "std" :
          "default");
        if (smrBootDevice != -1) {  //メニューで起動デバイスが既定以外に設定されたとき
          XEiJ.mpuSavedBootDevice = -1;  //保存されている起動デバイスを消す
          XEiJ.mpuSavedROMBootHandle = -1;
        }
      }
    };
    JMenu bootMenuFDD = ComponentFactory.createMenu ("FDD");
    for (int u = 0; u < FDC.FDC_MAX_UNITS; u++) {
      bootMenuFDD.add (ComponentFactory.createRadioButtonMenuItem (
        bootGroup, smrBootDevice == 0x9070 + (u << 8),
        "FDD " + u, bootListener));
    }
    JMenu bootMenuSASI = ComponentFactory.createMenu ("SASI");
    for (int u = 0; u < 16; u++) {
      bootMenuSASI.add (ComponentFactory.createRadioButtonMenuItem (
        bootGroup, smrBootDevice == 0x8000 + (u << 8),
        "SASI " + u, bootListener));
    }
    JMenu bootMenuSCSI = ComponentFactory.createMenu ("SCSI");
    for (int u = 0; u < 8; u++) {
      bootMenuSCSI.add (ComponentFactory.createRadioButtonMenuItem (
        bootGroup, smrBootDevice == 0xa000 && (smrROMBootHandle == SPC.SPC_HANDLE_EX + (u << 2) ||
                                               smrROMBootHandle == SPC.SPC_HANDLE_IN + (u << 2)), "SCSI " + u, bootListener));
    }
    JMenu bootMenuHFS = ComponentFactory.createMenu ("HFS");
    for (int u = 0; u < HFS.HFS_MAX_UNITS; u++) {
      bootMenuHFS.add (ComponentFactory.createRadioButtonMenuItem (
        bootGroup, smrBootDevice == 0xa000 && smrROMBootHandle == HFS.HFS_BOOT_HANDLE && HFS.hfsBootUnit == u,
        "HFS " + u, bootListener));
    }
    smrBootMenu =
      Multilingual.mlnText (
        ComponentFactory.createMenu (
          "Boot device",
          Multilingual.mlnText (
            ComponentFactory.createRadioButtonMenuItem (bootGroup, smrBootDevice == -1, "Follow settings in SWITCH.X", bootListener),
            "ja", "SWITCH.X の設定に従う"),
          smrSTDMenuItem = ComponentFactory.createRadioButtonMenuItem (bootGroup, smrBootDevice == 0x0000, "STD", bootListener),
          bootMenuFDD,
          bootMenuSASI,
          bootMenuSCSI,
          bootMenuHFS
          ),
        "ja", "起動デバイス");

  }  //smrInit()

  //smrParseBootDevice ()
  //  起動デバイスの設定を読み取る
  public static void smrParseBootDevice (String boot) {
    smrBootDevice = -1;  //起動デバイス
    smrROMBootHandle = -1;  //ROM起動ハンドル
    smrRAMBootAddress = -1;  //RAM起動アドレス
    boot = boot.toLowerCase ();
    if (boot.equals ("std")) {  //std
      smrBootDevice = 0x0000;  //STD 起動
    } else if (boot.startsWith ("fd")) {  //fdN
      int u = XEiJ.fmtParseInt (boot, 2, 0, FDC.FDC_MAX_UNITS - 1, FDC.FDC_MAX_UNITS);  //起動ユニット番号
      if (u < FDC.FDC_MAX_UNITS) {
        smrBootDevice = 0x9070 + (u << 8);  //FDD起動
      }
    } else if (boot.startsWith ("hd")) {  //hdN
      int u = XEiJ.fmtParseInt (boot, 2, 0, 15, 16);  //起動ユニット番号
      if (u < 16) {
        smrBootDevice = 0x8000 + (u << 8);  //SASI起動
      }
    } else if (boot.startsWith ("sc")) {  //scN
      int u = XEiJ.fmtParseInt (boot, 2, 0, 7, 8);
      if (u < 8) {
        smrBootDevice = 0xa000;  //ROM起動
        smrROMBootHandle = SPC.SPC_HANDLE_EX + ((u & 7) << 2);  //仮に拡張SCSI起動にしておく。リセットしたとき拡張SCSIがなければ内蔵SCSIに読み替えられる
      }
    } else if (boot.startsWith ("hf")) {  //hfN
      int u = XEiJ.fmtParseInt (boot, 2, 0, HFS.HFS_MAX_UNITS - 1, HFS.HFS_MAX_UNITS);  //起動ユニット番号
      if (u < HFS.HFS_MAX_UNITS) {
        HFS.hfsBootUnit = u;
        smrBootDevice = 0xa000;  //ROM起動
        smrROMBootHandle = HFS.HFS_BOOT_HANDLE;  //IPL起動ハンドル
      }
    } else if (boot.startsWith ("rom$")) {  //rom$X
      int handle = XEiJ.fmtParseIntRadix (boot, 3, 0, 0x00ffffff, 0x01000000, 16);  //起動ハンドル
      if (handle < 0x01000000) {
        smrBootDevice = 0xa000;  //ROM起動
        smrROMBootHandle = handle;
      }
    } else if (boot.startsWith ("ram$")) {  //ram$X
      int handle = XEiJ.fmtParseIntRadix (boot, 3, 0, 0x00ffffff, 0x01000000, 16);  //起動ハンドル
      if (handle < 0x01000000) {
        smrBootDevice = 0xb000;  //RAM起動
        smrRAMBootAddress = handle;
      }
    }
  }  //smrParseBootDevice(String)

  //smrReset ()
  //  SRAMリセット
  //  ここでROMも上書きする
  //  ROMを初期化してから呼び出すこと
  //  SPC.spcReset()よりも後であること
  public static void smrReset () {
    smrWriteEnableOn = false;

    //SRAMの容量を変更する
    {
      smrSramCurrentSize = smrSramSizeRequest;
      XEiJ.busSuper (MemoryMappedDevice.MMD_SMR, 0x00ed0000, 0x00ed0000 + smrSramSizeRequest);  //SMR SRAM
      if (smrSramSizeRequest < 65536) {
        XEiJ.busSuper (MemoryMappedDevice.MMD_NUL, 0x00ed0000 + smrSramSizeRequest, 0x00ed0000 + 65536);  //空き
      }
      if (false) {
        System.out.printf (Multilingual.mlnJapanese ?
                           "SRAM の容量は %dKB ($%08X-$%08X) です\n" :
                           "Capacity of SRAM is %dKB ($%08X-$%08X)\n",
                           smrSramCurrentSize >> 10, 0x00ed0000, 0x00ed0000 + smrSramCurrentSize - 1);
      }
    }

    //ROM起動ハンドルを調整する
    //  ROM起動ハンドルが内蔵SCSIを指しているが内蔵SCSIがないとき
    //    拡張SCSIがあるとき
    //      ROM起動ハンドルを拡張SCSIにする
    //    拡張SCSIがないとき
    //      ROM起動ハンドルを消す
    //      ROM起動のとき
    //        STD起動にする
    //  ROM起動ハンドルが拡張SCSIを指しているが拡張SCSIがないとき
    //    内蔵SCSIがあるとき
    //      ROM起動ハンドルを内蔵SCSIにする
    //    内蔵SCSIがないとき
    //      ROM起動ハンドルを消す
    //      ROM起動のとき
    //        STD起動にする
    //  ROM起動ハンドルがHFSを指しているがHFSのディレクトリが設定されていないとき
    //    ROM起動ハンドルを消す
    //    ROM起動のとき
    //      STD起動にする
    //起動デバイス
    if ((smrROMBootHandle & ~(7 << 2)) == SPC.SPC_HANDLE_IN && !SPC.spcSCSIINOn) {  //ROM起動ハンドルが内蔵SCSIを指しているが内蔵SCSIがないとき
      if (SPC.spcSCSIEXOn) {  //拡張SCSIがあるとき
        smrROMBootHandle = SPC.SPC_HANDLE_EX + (smrROMBootHandle & (7 << 2));  //ROM起動ハンドルを拡張SCSIにする
        //MainMemory.mmrWb (0x00ed0070, MainMemory.mmrRbs (0x00ed0070) | 0x08);  //拡張フラグをセットする
      } else {  //拡張SCSIがないとき
        smrROMBootHandle = 0x00e80400;  //ROM起動ハンドルを消す
        if (smrBootDevice == 0xa000) {  //ROM起動のとき
          smrBootDevice = 0x0000;  //STD起動にする
          smrSTDMenuItem.setSelected (true);
        }
      }
    }
    if ((smrROMBootHandle & ~(7 << 2)) == SPC.SPC_HANDLE_EX && !SPC.spcSCSIEXOn) {  //ROM起動ハンドルが拡張SCSIを指しているが拡張SCSIがないとき
      if (SPC.spcSCSIINOn) {  //内蔵SCSIがあるとき
        smrROMBootHandle = SPC.SPC_HANDLE_IN + (smrROMBootHandle & (7 << 2));  //ROM起動ハンドルを内蔵SCSIにする
        //MainMemory.mmrWb (0x00ed0070, MainMemory.mmrRbs (0x00ed0070) & ~0x08);  //拡張フラグをクリアする
      } else {  //内蔵SCSIがないとき
        smrROMBootHandle = 0x00e80400;  //ROM起動ハンドルを消す
        if (smrBootDevice == 0xa000) {  //ROM起動のとき
          smrBootDevice = 0x0000;  //STD起動にする
          smrSTDMenuItem.setSelected (true);
        }
      }
    }
    if (smrROMBootHandle == HFS.HFS_BOOT_HANDLE &&  //ROM起動ハンドルがHFSを指しているが
        (!HFS.hfsUnitArray[HFS.hfsBootUnit].abuConnected ||
         !HFS.hfsUnitArray[HFS.hfsBootUnit].abuInserted)) {  //HFSのディレクトリが設定されていないとき
      smrROMBootHandle = 0x00e80400;  //ROM起動ハンドルを消す
      if (smrBootDevice == 0xa000) {  //ROM起動のとき
        smrBootDevice = 0x0000;  //STD起動にする
        smrSTDMenuItem.setSelected (true);
      }
    }
    //ここから再起動
    if ((XEiJ.mpuROMBootHandle & ~(7 << 2)) == SPC.SPC_HANDLE_IN && !SPC.spcSCSIINOn) {  //ROM起動ハンドルが内蔵SCSIを指しているが内蔵SCSIがないとき
      if (SPC.spcSCSIEXOn) {  //拡張SCSIがあるとき
        XEiJ.mpuROMBootHandle = SPC.SPC_HANDLE_EX + (XEiJ.mpuROMBootHandle & (7 << 2));  //ROM起動ハンドルを拡張SCSIにする
        //MainMemory.mmrWb (0x00ed0070, MainMemory.mmrRbs (0x00ed0070) | 0x08);  //拡張フラグをセットする
      } else {  //拡張SCSIがないとき
        XEiJ.mpuROMBootHandle = 0x00e80400;  //ROM起動ハンドルを消す
        if (XEiJ.mpuBootDevice == 0xa000) {  //ROM起動のとき
          XEiJ.mpuBootDevice = 0x0000;  //STD起動にする
        }
      }
    }
    if ((XEiJ.mpuROMBootHandle & ~(7 << 2)) == SPC.SPC_HANDLE_EX && !SPC.spcSCSIEXOn) {  //ROM起動ハンドルが拡張SCSIを指しているが拡張SCSIがないとき
      if (SPC.spcSCSIINOn) {  //内蔵SCSIがあるとき
        XEiJ.mpuROMBootHandle = SPC.SPC_HANDLE_IN + (XEiJ.mpuROMBootHandle & (7 << 2));  //ROM起動ハンドルを内蔵SCSIにする
        //MainMemory.mmrWb (0x00ed0070, MainMemory.mmrRbs (0x00ed0070) & ~0x08);  //拡張フラグをクリアする
      } else {  //内蔵SCSIがないとき
        XEiJ.mpuROMBootHandle = 0x00e80400;  //ROM起動ハンドルを消す
        if (XEiJ.mpuBootDevice == 0xa000) {  //ROM起動のとき
          XEiJ.mpuBootDevice = 0x0000;  //STD起動にする
        }
      }
    }
    if (XEiJ.mpuROMBootHandle == HFS.HFS_BOOT_HANDLE &&  //ROM起動ハンドルがHFSを指しているが
        (!HFS.hfsUnitArray[HFS.hfsBootUnit].abuConnected ||
         !HFS.hfsUnitArray[HFS.hfsBootUnit].abuInserted)) {  //HFSのディレクトリが設定されていないとき
      XEiJ.mpuROMBootHandle = 0x00e80400;  //ROM起動ハンドルを消す
      if (XEiJ.mpuBootDevice == 0xa000) {  //ROM起動のとき
        XEiJ.mpuBootDevice = 0x0000;  //STD起動にする
      }
    }

    smrOverride ();

  }  //smrReset()

  //smrOverride ()
  //  SRAMの設定を上書きする
  //  ここから再起動もSRAMに上書きする
  public static void smrOverride () {
    if (MainMemory.mmrRls (0x00ed0000) == 0x82773638 &&  //Ｘ68
        MainMemory.mmrRls (0x00ed0004) == 0x30303057) {  //000W  初期化されている。初期化されていないときに上書きしても意味がない

      //メモリサイズ
      if (smrModifyMemorySizeOn) {
        int memorySizeAddress = 0x00ed0008;
        int memorySizeOld = MainMemory.mmrRls (memorySizeAddress);
        int memorySizeNew = MainMemory.mmrMemorySizeCurrent;
        if (memorySizeOld != memorySizeNew) {
          MainMemory.mmrWl (memorySizeAddress, memorySizeNew);
          System.out.printf (Multilingual.mlnJapanese ?
                             "SRAM にあるメモリサイズを %dMB から %dMB に変更しました\n" :
                             "Changed the memory size in SRAM from %dMB to %dMB\n",
                             memorySizeOld >> 20,
                             memorySizeNew >> 20);
        }
      }

      //ROM起動ハンドル
      int romHandleAddress = 0x00ed000c;
      int romHandleOld = MainMemory.mmrRls (romHandleAddress);
      int romHandleNew = XEiJ.mpuROMBootHandle != -1 ? XEiJ.mpuROMBootHandle : smrROMBootHandle;
      if (romHandleNew != -1 &&
          romHandleOld != romHandleNew) {
        MainMemory.mmrWl (romHandleAddress, romHandleNew);
        System.out.printf (Multilingual.mlnJapanese ?
                           "SRAM にある ROM 起動ハンドルを $%08X から $%08X に変更しました\n" :
                           "Changed the ROM boot handle in SRAM from $%08X to $%08X\n",
                           romHandleOld,
                           romHandleNew);
      }

      //RAM起動アドレス
      int ramAddressAddress = 0x00ed0010;
      int ramAddressOld = MainMemory.mmrRls (ramAddressAddress);
      int ramAddressNew = smrRAMBootAddress;
      if (ramAddressNew != -1 &&
          ramAddressOld != ramAddressNew) {
        MainMemory.mmrWl (ramAddressAddress, ramAddressNew);
        System.out.printf (Multilingual.mlnJapanese ?
                           "SRAM にある RAM 起動アドレスを $%08X から $%08X に変更しました\n" :
                           "Changed the RAM boot address in SRAM from $%08X to $%08X\n",
                           ramAddressOld,
                           ramAddressNew);
      }

      //起動デバイス
      int deviceAddress = 0x00ed0018;
      int deviceOld = MainMemory.mmrRwz (deviceAddress);
      int deviceNew = XEiJ.mpuBootDevice != -1 ? XEiJ.mpuBootDevice : smrBootDevice;
      if (deviceNew != -1 &&
          deviceOld != deviceNew) {
        MainMemory.mmrWw (deviceAddress, deviceNew);
        System.out.printf (Multilingual.mlnJapanese ?
                           "SRAM にある起動デバイスを %s から %s に変更しました\n" :
                           "Changed the boot device in SRAM from %s to %s\n",
                           smrBootDescription (romHandleOld, ramAddressOld, deviceOld),
                           smrBootDescription (romHandleNew, ramAddressNew, deviceNew));
      }

      //リピート開始
      int repeatDelayAddress = 0x00ed003a;
      int repeatDelayOld = MainMemory.mmrRbz (repeatDelayAddress);
      int repeatDelayNew = smrRepeatDelay;
      if (repeatDelayNew != -1 &&
          repeatDelayOld != repeatDelayNew) {
        MainMemory.mmrWb (repeatDelayAddress, repeatDelayNew);
        System.out.printf (Multilingual.mlnJapanese ?
                           "SRAM にあるリピート開始を %dms から %dms に変更しました\n" :
                           "Changed the repeat delay in SRAM from %dms to %dms\n",
                           200 + 100 * (repeatDelayOld & 15),
                           200 + 100 * (repeatDelayNew & 15));
      }

      //リピート間隔
      int repeatIntervalAddress = 0x00ed003b;
      int repeatIntervalOld = MainMemory.mmrRbz (repeatIntervalAddress);
      int repeatIntervalNew = smrRepeatInterval;
      if (repeatIntervalNew != -1 &&
          repeatIntervalOld != repeatIntervalNew) {
        MainMemory.mmrWb (repeatIntervalAddress, repeatIntervalNew);
        System.out.printf (Multilingual.mlnJapanese ?
                           "SRAM にあるリピート間隔を %dms から %dms に変更しました\n" :
                           "Changed the repeat interval in SRAM from %dms to %dms\n",
                           30 + 5 * (repeatDelayOld & 15),
                           30 + 5 * (repeatDelayNew & 15));
      }

      boolean iplrom16 = (XEiJ.currentAccelerator == XEiJ.ACCELERATOR_HYBRID ||
                          XEiJ.currentAccelerator == XEiJ.ACCELERATOR_060TURBO ||
                          XEiJ.currentAccelerator == XEiJ.ACCELERATOR_060TURBOPRO ||
                          ROM.romIPLROM16On);  //IPLROM 1.6か

      //ROMデバッガ起動フラグ
      if (smrRomdbFlag != -1) {  //設定する
        boolean nonexistence = (XEiJ.currentModel.getIPLROM () == 130 && !iplrom16 &&
                                ROM.romROM30Data == null);  //ROMデバッガがないか。IPLROM 1.3でROM30.DATが指定されていない
        int address = 0x00ed0058;
        int oldData = MainMemory.mmrRbz (address);
        int newData = (nonexistence ? 0 :  //ROMデバッガがないときOFF
                       !iplrom16 && smrRomdbFlag == 1 ? 255 :  //IPLROM 1.6でないときCONをAUXに変更
                       smrRomdbFlag);
        if (oldData != newData) {
          MainMemory.mmrWb (address, newData);
          System.out.printf (Multilingual.mlnJapanese ?
                             "SRAM にある ROM デバッガ起動フラグを %s から %s に変更しました\n" :
                             "ROM debugger startup flag in SRAM changed from %s to %s\n",
                             oldData == 0 ? "OFF" : oldData == 255 ? "AUX" : "CON",
                             newData == 0 ? "OFF" : newData == 255 ? "AUX" : "CON");
        }
      }

      //起動時のキャッシュモード
      if (smrCacheMode != -1) {
        int address = 0x00ed0090;
        int oldData = MainMemory.mmrRbz (address);
        int newData = smrCacheMode;
        if (oldData != newData) {
          MainMemory.mmrWb (address, newData);
          System.out.printf (Multilingual.mlnJapanese ?
                             "SRAM にある起動時のキャッシュモードを %s から %s に変更しました\n" :
                             "Cache mode at startup in SRAM changed from %s to %s\n",
                             smrCacheToString (oldData),
                             smrCacheToString (newData));
        }
      }

      //起動音
      if (smrStupsndMode != -1) {
        int address = 0x00ed0091;
        int oldData = MainMemory.mmrRbz (address);
        int newData = (!iplrom16 && 2 <= smrStupsndMode ? 1 :  //IPLROM 1.6でないとき2-126を1に変更
                       smrStupsndMode);
        if (oldData != newData) {
          MainMemory.mmrWb (address, newData);
          System.out.printf (Multilingual.mlnJapanese ?
                             "SRAM にある起動音の設定を %s から %s に変更しました\n" :
                             "Startup sound setting in SRAM changed from %s to %s\n",
                             smrStupsndToString (oldData),
                             smrStupsndToString (newData));
        }
      }

      //SASIハードディスクの最大数
      int hdMaxAddress = 0x00ed005a;
      int hdMaxOld = MainMemory.mmrRbz (hdMaxAddress);
      int sasiFlag = MainMemory.mmrRbz (0x00ed006f) == 'V' ? MainMemory.mmrRbz (0x00ed0071) : 0;
      int hdMaxNew = SPC.spcSCSIINOn ? 2 * (32 - Integer.numberOfLeadingZeros (sasiFlag)) : HDC.hdcHDMax;
      if (hdMaxOld != hdMaxNew) {
        MainMemory.mmrWb (hdMaxAddress, hdMaxNew);
        System.out.printf (Multilingual.mlnJapanese ?
                           "SRAM にある SASI ハードディスクの最大数を %d から %d に変更しました\n" :
                           "Changed the maximum number of SASI hard disks in SRAM from %d to %d\n",
                           hdMaxOld, hdMaxNew);
      }

    }
    //「ここから再起動」をキャンセルする
    //XEiJ.mpuBootDevice = -1;
    //XEiJ.mpuROMBootHandle = -1;
  }

  //smrCacheToString (mode)
  //  キャッシュモードを文字列に変換する
  public static String smrCacheToString (int mode) {
    return (mode == 0 ? "off-off" :
            mode == 1 ? "off-on" :
            mode == 2 ? "on-off" :
            mode == 3 ? "on-on" :
            "");
  }  //smrCacheToString

  //smrStringToCache (s)
  //  文字列をキャッシュモードに変換する
  public static int smrStringToCache (String s) {
    s = s.toLowerCase ();
    return (s.equals ("off-off") ? 0 :
            s.equals ("off-on") ? 1 :
            s.equals ("on-off") ? 2 :
            s.equals ("on-on") ? 3 :
            -1);
  }  //smrStringToCache

  //smrStupsndToString (mode)
  //  起動音を文字列に変換する
  public static String smrStupsndToString (int mode) {
    String s = "";
    if (mode == 0) {
      s = "off";
    } else if (mode == 1) {
      s = "on";
    } else if (2 <= mode && mode <= 127) {
      int i = mode - (mode >> 2);
      int o = i / 12;
      int n = i % 12;
      if (9 <= n) {
        o++;
      }
      s = "o" + o + (n == 0 ? "d#" :
                     n == 1 ? "e" :
                     n == 2 ? "f" :
                     n == 3 ? "f#" :
                     n == 4 ? "g" :
                     n == 5 ? "g#" :
                     n == 6 ? "a" :
                     n == 7 ? "a#" :
                     n == 8 ? "b" :
                     n == 9 ? "c" :
                     n == 10 ? "c#" :
                     "d");
    }
    return s;
  }  //smrStupsndToString

  //smrStringToStupsnd (s)
  //  文字列を起動音に変換する
  public static int smrStringToStupsnd (String s) {
    s = s.toLowerCase ();
    int mode = -1;
    if (s.equals ("off")) {
      mode = 0;
    } else if (s.equals ("on")) {
      mode = 1;
    } else {
      int l = s.length ();
      int c0 = l < 1 ? -1 : s.charAt (0);
      int c1 = l < 2 ? -1 : s.charAt (1);
      int c2 = l < 3 ? -1 : s.charAt (2);
      int c3 = l < 4 ? -1 : s.charAt (3);
      if (c0 == 'o' &&
          '0' <= c1 && c1 <= '8' &&
          'a' <= c2 && c2 <= 'g') {
        int o = c1 - '0';
        int n = (c2 == 'a' ? 6 :
                 c2 == 'b' ? 8 :
                 c2 == 'c' ? -3 :
                 c2 == 'd' ? -1 :
                 c2 == 'e' ? 1 :
                 c2 == 'f' ? 2 :
                 4);
        if (c3 == '#' || c3 == '+') {
          n++;
        } else if (c3 == '-') {
          n--;
        }
        int i = 12 * o + n;
        if (2 <= i && i <= 95) {
          mode = i + i / 3;
        }
      }
    }
    return mode;
  }  //smrStringToStupsnd

  static {
    if (false) {
      //起動音の文字列変換の確認
      for (int mode = 0; mode <= 126; mode++) {  //127は不可
        if (mode % 4 == 3) {
          continue;
        }
        String s = smrStupsndToString (mode);
        int i = smrStringToStupsnd (s);
        System.out.printf ("mode=%d s=%s i=%d\n", mode, s, i);
      }
    }
  }

  //s = smrBootDescription (code, romHandle, ramAddress)
  //  device      $00ED0018  起動デバイス
  //                           $0000  STD  FD→HD→ROM→RAM
  //                           $8xxx  HD
  //                           $9xxx  FD
  //                           $Axxx  ROM  ROM起動ハンドルからROM起動アドレスを取り出して呼び出す
  //                                       ROM起動ハンドルがバスエラーになるときはSTDで起動する
  //                           $Bxxx  RAM  RAM起動アドレスを呼び出す。先頭が$60でなければならない
  //  romHandle   $00ED000C  ROM起動ハンドル
  //  ramAddress  $00ED0010  RAM起動アドレス
  public static String smrBootDescription (int romHandle, int ramAddress, int device) {
    switch (device & 0xf000) {
    case 0x8000:
      return "HD" + (device >> 8 & 15);
    case 0x9000:
      return "2HD" + (device >> 8 & 3);
    case 0xa000:
      return ((romHandle & ~(7 << 2)) == SPC.SPC_HANDLE_IN ? "SCSI" + ((romHandle >> 2) & 7) :   //内蔵SCSI
              (romHandle & ~(7 << 2)) == SPC.SPC_HANDLE_EX ? "SCSI" + ((romHandle >> 2) & 7) :   //拡張SCSI
              "ROM$" + XEiJ.fmtHex6 (romHandle));
    case 0xb000:
      return "RAM$" + XEiJ.fmtHex6 (ramAddress);
    default:
      return "STD";
    }
  }

  //smrClear ()
  //  SRAMクリア
  public static void smrClear () {
    XEiJ.pnlExitFullScreen (true);
    if (JOptionPane.showConfirmDialog (
      XEiJ.frmFrame,
      Multilingual.mlnJapanese ? "SRAM をクリアしますか？" : "Do you want to clear SRAM?",
      Multilingual.mlnJapanese ? "確認" : "Confirmation",
      JOptionPane.YES_NO_OPTION,
      JOptionPane.PLAIN_MESSAGE) == JOptionPane.YES_OPTION) {
      Arrays.fill (MainMemory.mmrM8, 0x00ed0000, 0x00ed0000 + 65536, (byte) 0x00);
    }
  }  //smrClear()

  //smrLoad ()
  //  SRAM読み込み
  public static void smrLoad () {
    JFileChooser2 fileChooser = new JFileChooser2 ();
    fileChooser.setFileFilter (new javax.swing.filechooser.FileFilter () {  //java.io.FileFilterと紛らわしい
      @Override public boolean accept (File file) {
        String name = file.getName ();
        String upperName = name.toUpperCase ();
        return (file.isDirectory () ||
                (file.isFile () &&
                 upperName.startsWith ("SRAM")));
      }
      @Override public String getDescription () {
        return (Multilingual.mlnJapanese ?
                "SRAM データファイル (SRAM*.*)" :
                "SRAM data files (SRAM*.*)");
      }
    });
    if (fileChooser.showOpenDialog (null) == JFileChooser.APPROVE_OPTION) {
      File file = fileChooser.getSelectedFile ();
      String name = file.getPath ();
      if (!smrLoadData (name)) {  //読み込めなかった
        XEiJ.pnlExitFullScreen (true);
        JOptionPane.showMessageDialog (null,
                                       Multilingual.mlnJapanese ?
                                       name + " のサイズが違います" :
                                       name + " has wrong size");
        return;
      }
    }
  }  //smrLoad()

  //success = smrLoadData (name)
  //  SRAMのイメージファイルを読み込む
  public static boolean smrLoadData (String name) {
    byte[] array = XEiJ.rscGetFile (name, smrSramSizeRequest);
    if (array != null) {  //読み込めた
      System.arraycopy (array, 0, MainMemory.mmrM8, 0x00ed0000, smrSramSizeRequest);  //SRAMにコピーする
      if (smrSramSizeRequest < 65536) {
        Arrays.fill (MainMemory.mmrM8, 0x00ed0000 + smrSramSizeRequest, 0x00ed0000 + 65536, (byte) 0x00);  //空き
      }
      return true;
    }
    return false;
  }

  //smrSave ()
  //  SRAM書き出し
  public static void smrSave () {
    JFileChooser2 fileChooser = new JFileChooser2 ();
    fileChooser.setFileFilter (new javax.swing.filechooser.FileFilter () {  //java.io.FileFilterと紛らわしい
      @Override public boolean accept (File file) {
        String name = file.getName ();
        String upperName = name.toUpperCase ();
        return (file.isDirectory () ||
                (file.isFile () &&
                 upperName.startsWith ("SRAM")));
      }
      @Override public String getDescription () {
        return (Multilingual.mlnJapanese ?
                "SRAM データファイル (SRAM*.*)" :
                "SRAM data files (SRAM*.*)");
      }
    });
    if (fileChooser.showSaveDialog (null) == JFileChooser.APPROVE_OPTION) {
      XEiJ.rscPutFile (fileChooser.getSelectedFile ().getPath (), MainMemory.mmrM8, 0x00ed0000, smrSramSizeRequest);
    }
  }  //smrSave()

}  //class SRAM
