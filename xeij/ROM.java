//========================================================================================
//  ROM.java
//    en:ROM
//    ja:ROM
//  Copyright (C) 2003-2025 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.awt.event.*;  //ActionListener
import java.io.*;  //File
import java.lang.*;
import java.util.*;  //Arrays
import javax.swing.*;  //JFileChooser
import javax.swing.filechooser.*;  //FileFilter

public class ROM {

  //設定
  public static String romROMName;  //-rom  ROMイメージファイル名
  public static String romCGROMName;  //-cgrom  CGROMイメージファイル名
  public static String romROM30Name;  //-rom30  X68030のIPLROMイメージファイル名
  public static String romSCSIINROMName;  //-scsiinrom  SCSIINROMイメージファイル名
  public static String romSCSIEXROMName;  //-scsiexrom  SCSIEXROMイメージファイル名
  public static String romIPLROMName;  //-iplrom  IPLROMイメージファイル名
  public static String romX68000LogoName;  //-x68000logo  X68000ロゴファイル名
  public static int romIPLROM16Style;  //-iplrom16style  IPLROM 1.6のメッセージのスタイル
  public static boolean romIPLROM256KOn;  //-iplrom256k  X68000のIPLROMを256KBに改造する
  public static boolean romIPLROM16On;  //-iplrom16  IPLROM 1.6を使う
  public static boolean romOmusubiOn;  //-omusubi  おむすびフォントを使う

  //設定されたデータ
  public static byte[] romROMData;  //-rom  ROMデータ(1MB)
  public static byte[] romCGROMData;  //-cgrom  CGROMデータ(768KBまたは1MB)。1MBのときも768KBだけ使う
  public static byte[] romROM30Data;  //-rom30  X68030のIPLROMデータ(128KBまたは256KB)。256KBのときは256KB使う
  public static byte[] romSCSIINROMData;  //-scsiinrom  SCSIINROMデータ(8KB)
  public static byte[] romSCSIEXROMData;  //-scsiexrom  SCSIEXROMデータ(8160Bまたは8192B)
  public static byte[] romIPLROMData;  //-iplrom  IPLROMデータ(128KB)
  public static byte[] romX68000LogoData;  //-x68000logo  X68000ロゴデータ(struct FNTBUF形式)

  //リソース
  public static byte[] cgromxeijdat;
  public static byte[] iplromdat;
  public static byte[] iplromxvdat;
  public static byte[] iplromcodat;
  public static byte[] iplrom30dat;
  public static byte[] humansys;
  public static byte[] float2x;
  public static byte[] dbx;

  //ROMイメージ
  public static byte[] scsi16indat;
  public static byte[] scsi16exdat;
  public static byte[] iplrom10sasi128;
  public static byte[] iplrom10scsi128;
  public static byte[] iplrom11sasi128;
  public static byte[] iplrom11scsi128;
  public static byte[] iplrom12sasi128;
  public static byte[] iplrom12scsi128;
  public static byte[] iplrom13scsi128;
  public static byte[] iplrom16sasi128;
  public static byte[] iplrom16scsi128;
  public static byte[] iplrom16sasi256;
  public static byte[] iplrom16scsi256;

  //ROMイメージを保存するファイル
  public static File romLastXEIJROMFile;  //XEIJROM.DAT
  public static File romLastXEIJROM30File;  //XEIJROM30.DAT
  public static File romLastXEIJIPLROM30File;  //XEIJIPLROM30.DAT
  public static File romLastXEIJSCSIINROMFile;  //XEIJSCSIINROM.DAT
  public static File romLastXEIJSCSIEXROMFile;  //XEIJSCSIEXROM.DAT
  public static File romLastEVEN030File;  //EVEN030.BIN
  public static File romLastODD030File;  //ODD030.BIN
  public static File romLastEVENXVIFile;  //EVENXVI.BIN
  public static File romLastODDXVIFile;  //ODDXVI.BIN
  public static File romLastEVENPROFile;  //EVENPRO.BIN
  public static File romLastODDPROFile;  //ODDPRO.BIN

  //メニュー
  public static JMenu romMenu;

  public static int romCurrentIPLROM;  //現在のIPLROMのバージョン。100～160
  public static int romCurrent16Size;  //現在のIPLROM 1.6のサイズ(KB)。0,128,256のいずれか

  //romInit ()
  //  初期化
  public static void romInit () {

    romCurrentIPLROM = 100;
    romCurrent16Size = 0;

    //設定を復元する
    romROMName = Settings.sgsGetString ("rom");
    romCGROMName = Settings.sgsGetString ("cgrom");
    romROM30Name = Settings.sgsGetString ("rom30");
    romSCSIINROMName = Settings.sgsGetString ("scsiinrom");
    romSCSIEXROMName = Settings.sgsGetString ("scsiexrom");
    romIPLROMName = Settings.sgsGetString ("iplrom");
    romX68000LogoName = Settings.sgsGetString ("x68000logo");
    romIPLROM16Style = Settings.sgsGetInt ("iplrom16style");
    romIPLROM256KOn = Settings.sgsGetOnOff ("iplrom256k");
    romIPLROM16On = Settings.sgsGetOnOff ("iplrom16");
    romOmusubiOn = Settings.sgsGetOnOff ("omusubi");

    //設定されたデータを読み込む
    romROMData = romROMName.equals ("") ? null : XEiJ.rscGetFile (romROMName, 1024 * 1024);
    romCGROMData = romCGROMName.equals ("") ? null : XEiJ.rscGetFile (romCGROMName, 1024 * 768, 1024 * 1024);  //768KBまたは1MB
    romROM30Data = romROM30Name.equals ("") ? null : XEiJ.rscGetFile (romROM30Name, 1024 * 128, 1024 * 256);  //128KBまたは256KB
    romSCSIINROMData = romSCSIINROMName.equals ("") ? null : XEiJ.rscGetFile (romSCSIINROMName, 1024 * 8);
    romSCSIEXROMData = romSCSIEXROMName.equals ("") ? null : XEiJ.rscGetFile (romSCSIEXROMName, 8160, 8192);  //8160Bまたは8192B
    romIPLROMData = romIPLROMName.equals ("") ? null : XEiJ.rscGetFile (romIPLROMName, 1024 * 128);
    romX68000LogoData = romX68000LogoName.equals ("") ? null : XEiJ.rscGetFile (romX68000LogoName);

    //X68000ロゴを確認する
    if (romX68000LogoData != null) {
      byte[] b = romX68000LogoData;
      romX68000LogoData = null;
      if (4 <= b.length) {
        int w = ByteArray.byaRwz (b, 0);
        int h = ByteArray.byaRwz (b, 2);
        int l = 4 + ((w + 7) >> 3) * h;
        if (l == b.length && l <= 424) {
          //全体がstruct FNTBUF形式でサイズが範囲内
          romX68000LogoData = b;
        } else {
          //wwww hhhh 03ff f9ff e000にマッチする場所を探す
          for (int i = 0; i <= (b.length & -2) - 10; i += 2) {
            w = ByteArray.byaRwz (b, i);
            h = ByteArray.byaRwz (b, i + 2);
            int d0 = ByteArray.byaRwz (b, i + 4);
            int d1 = ByteArray.byaRwz (b, i + 6);
            int d2 = ByteArray.byaRwz (b, i + 8);
            if (24 <= w && h == 16 && d0 == 0x03ff && d1 == 0xf9ff && d2 == 0xe000) {
              l = 4 + ((w + 7) >> 3) * h;
              if (i + l <= b.length && l <= 424) {
                romX68000LogoData = Arrays.copyOfRange (b, i, i + l);
                break;
              }
            }
          }
        }
      }
    }

    //リソースを読み込む
    romLoadResource ();

    //ROMイメージを作る
    romMakeROMImage ();

    //ROMイメージを保存するファイルを決める
    romLastXEIJROMFile = new File ("XEIJROM.DAT");
    romLastXEIJROM30File = new File ("XEIJROM30.DAT");
    romLastXEIJIPLROM30File = new File ("XEIJIPLROM30.DAT");
    romLastXEIJSCSIINROMFile = new File ("XEIJSCSIINROM.DAT");
    romLastXEIJSCSIEXROMFile = new File ("XEIJSCSIEXROM.DAT");
    romLastEVEN030File = new File ("EVEN030.BIN");
    romLastODD030File = new File ("ODD030.BIN");
    romLastEVENXVIFile = new File ("EVENXVI.BIN");
    romLastODDXVIFile = new File ("ODDXVI.BIN");
    romLastEVENPROFile = new File ("EVENPRO.BIN");
    romLastODDPROFile = new File ("ODDPRO.BIN");

    //メニューを作る
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        switch (ae.getActionCommand ()) {
        case "XEIJROM.DAT ($00F00000-$00FFFFFF;1MB)":
          romOpenXEIJROMDialog ();
          break;
        case "XEIJROM30.DAT ($00FC0000-$00FDFFFF;128KB)":
          romOpenXEIJROM30Dialog ();
          break;
        case "XEIJIPLROM30.DAT ($00FE0000-$00FFFFFF;128KB)":
          romOpenXEIJIPLROM30Dialog ();
          break;
        case "XEIJSCSIINROM.DAT ($00FC0000-$00FC1FFF;8KB)":
          romOpenXEIJSCSIINROMDialog ();
          break;
        case "XEIJSCSIEXROM.DAT ($00EA0000-$00EA1FFF;8KB)":
          romOpenXEIJSCSIEXROMDialog ();
          break;
        case "EVEN030.BIN ($00F00000-$00FFFFFF EVEN;512KB)":
          romOpenEVEN030Dialog ();
          break;
        case "ODD030.BIN ($00F00000-$00FFFFFF ODD;512KB)":
          romOpenODD030Dialog ();
          break;
        case "EVENXVI.BIN ($00FC0000-$00FFFFFF EVEN;128KB)":
          romOpenEVENXVIDialog ();
          break;
        case "ODDXVI.BIN ($00FC0000-$00FFFFFF ODD;128KB)":
          romOpenODDXVIDialog ();
          break;
        case "EVENPRO.BIN ($00FE0000-$00FFFFFF EVEN;64KB)":
          romOpenEVENPRODialog ();
          break;
        case "ODDPRO.BIN ($00FE0000-$00FFFFFF ODD;64KB)":
          romOpenODDPRODialog ();
          break;
        }
      }
    };
    romMenu = Multilingual.mlnText (
      ComponentFactory.createMenu (
        "Create ROM file",
        ComponentFactory.createMenuItem ("XEIJROM.DAT ($00F00000-$00FFFFFF;1MB)", listener),
        ComponentFactory.createMenuItem ("XEIJROM30.DAT ($00FC0000-$00FDFFFF;128KB)", listener),
        ComponentFactory.createMenuItem ("XEIJIPLROM30.DAT ($00FE0000-$00FFFFFF;128KB)", listener),
        ComponentFactory.createMenuItem ("XEIJSCSIINROM.DAT ($00FC0000-$00FC1FFF;8KB)", listener),
        ComponentFactory.createMenuItem ("XEIJSCSIEXROM.DAT ($00EA0000-$00EA1FFF;8KB)", listener),
        ComponentFactory.createHorizontalSeparator (),
        ComponentFactory.createMenuItem ("EVEN030.BIN ($00F00000-$00FFFFFF EVEN;512KB)", listener),
        ComponentFactory.createMenuItem ("ODD030.BIN ($00F00000-$00FFFFFF ODD;512KB)", listener),
        ComponentFactory.createMenuItem ("EVENXVI.BIN ($00FC0000-$00FFFFFF EVEN;128KB)", listener),
        ComponentFactory.createMenuItem ("ODDXVI.BIN ($00FC0000-$00FFFFFF ODD;128KB)", listener),
        ComponentFactory.createMenuItem ("EVENPRO.BIN ($00FE0000-$00FFFFFF EVEN;64KB)", listener),
        ComponentFactory.createMenuItem ("ODDPRO.BIN ($00FE0000-$00FFFFFF ODD;64KB)", listener)
        ),
      "ja", "ROM ファイルの作成");

  }


  //romTini ()
  //  後始末
  public static void romTini () {

    //設定を保存する
    Settings.sgsPutString ("rom", romROMName);
    Settings.sgsPutString ("cgrom", romCGROMName);
    Settings.sgsPutString ("rom30", romROM30Name);
    Settings.sgsPutString ("scsiinrom", romSCSIINROMName);
    Settings.sgsPutString ("scsiexrom", romSCSIEXROMName);
    Settings.sgsPutString ("iplrom", romIPLROMName);
    Settings.sgsPutOnOff ("iplrom256k", romIPLROM256KOn);
    Settings.sgsPutOnOff ("iplrom16", romIPLROM16On);
    Settings.sgsPutOnOff ("omusubi", romOmusubiOn);

  }


  //romReset ()
  //  リセット
  public static void romReset () {
    romCurrentIPLROM = (XEiJ.currentAccelerator == XEiJ.ACCELERATOR_HYBRID ||
                        XEiJ.currentAccelerator == XEiJ.ACCELERATOR_060TURBO ||
                        XEiJ.currentAccelerator == XEiJ.ACCELERATOR_060TURBOPRO ||
                        romIPLROM16On ? 160 :
                        XEiJ.currentModel.getIPLROM ());
    romCurrent16Size = (XEiJ.currentAccelerator == XEiJ.ACCELERATOR_HYBRID ||
                        XEiJ.currentAccelerator == XEiJ.ACCELERATOR_060TURBO ||
                        XEiJ.currentAccelerator == XEiJ.ACCELERATOR_060TURBOPRO ? 256 :
                        romIPLROM16On ? romIPLROM256KOn ? 256 : 128 : 0);
    //----------------------------------------------------------------
    if (romROMData == null &&  //-romが指定されていないかつ
        romCGROMData == null) {  //-cgromが指定されていない
      //CGROM_XEiJ.DATをコピーする
      System.arraycopy (cgromxeijdat, 0,  //from
                        MainMemory.mmrM8, 0x00f00000,  //to
                        0x00fc0000 - 0x00f00000);  //length
    }
    //----------------------------------------------------------------
    if (romROMData == null &&  //-romが指定されていないかつ
        (romROM30Data == null ||  //-rom30が指定されていないまたは
         (romROM30Data.length == 1024 * 128 &&  //-rom30が128KBかつ
          romIPLROMData == null))) {  //-iplromが指定されていない
      //機種に対応するIPLROMをコピーする
      byte[] iplromArray = (XEiJ.currentAccelerator == XEiJ.ACCELERATOR_HYBRID ||
                            XEiJ.currentAccelerator == XEiJ.ACCELERATOR_060TURBO ? iplrom16scsi256 :
                            XEiJ.currentAccelerator == XEiJ.ACCELERATOR_060TURBOPRO ? iplrom16sasi256 :
                            !XEiJ.currentModel.isSCSI () ?
                            //SASI
                            romIPLROM16On ? romIPLROM256KOn ? iplrom16sasi256 : iplrom16sasi128 : iplrom10sasi128 :
                            //SCSI
                            XEiJ.currentModel.getIPLROM () < 110 ?
                            //100,101,102,103
                            romIPLROM16On ? romIPLROM256KOn ? iplrom16scsi256 : iplrom16scsi128 : iplrom10scsi128 :
                            XEiJ.currentModel.getIPLROM () < 120 ?
                            //110
                            romIPLROM16On ? romIPLROM256KOn ? iplrom16scsi256 : iplrom16scsi128 : iplrom11scsi128 :
                            XEiJ.currentModel.getIPLROM () < 130 ?
                            //120
                            romIPLROM16On ? romIPLROM256KOn ? iplrom16scsi256 : iplrom16scsi128 : iplrom12scsi128 :
                            //130
                            romIPLROM16On ?                   iplrom16scsi256                   : iplrom13scsi128);
      System.arraycopy (iplromArray, 0,  //from
                        MainMemory.mmrM8, 0x00fc0000,  //to
                        0x01000000 - 0x00fc0000);  //length
      //IPLROM 1.6のとき情報を書き込む
      //  $00FF09F0  メインメモリのサイズ
      //  $00FFFFE0  'NAME',機種名,$00,$FF,…
      //  $00FFFFFE  エミュレータコード(ここでは設定しない)
      //             $00  実機
      //             $01  XEiJ
      //             $02  XM6 TypeG
      //             $03  X68000_MiSTer
      //             $04  X68000 Z
      //             $FF  設定なし
      //  $00FFFFFF  機種コード
      if (iplromArray == iplrom16sasi128 ||
          iplromArray == iplrom16scsi128 ||
          iplromArray == iplrom16sasi256 ||
          iplromArray == iplrom16scsi256) {
        int size = MainMemory.mmrMemorySizeRequest;  //メインメモリのサイズ
        int hdMax = !XEiJ.currentModel.isSCSI () ? 1 : 0;  //SASIハードディスクの最大数
        String name = (//EEEEEEEEEEEEEEEEFFFFFFFFFFFFFFFF
                       //0123456789ABCDEF0123456789ABCDEF
                       //NAMEX68000 EXPERTII HD (CZ-613C-BK)
                       //"NAME" + XEiJ.currentModel.getName () + " (" + XEiJ.currentModel.getType () + ")" + "\0");
                       "NAME" + XEiJ.currentModel.getName () + "\0");  //機種名
        int code = XEiJ.currentModel.getCode ();  //機種コード
        //
        ByteArray.byaWl (MainMemory.mmrM8, 0x00ff09e8 + 0x08, size);
        MainMemory.mmrM8[0x00ff09e8 + 0x5a] = (byte) hdMax;
        ByteArray.byaWstr (MainMemory.mmrM8, 0x00ffffe0, name);
        MainMemory.mmrM8[0x00ffffff] = (byte) code;
        //  iplrom16sasi128は前半にも書き込む
        //  ROMに焼くとき前半を焼いても構わないようにする
        if (iplromArray == iplrom16sasi128) {
          ByteArray.byaWl (MainMemory.mmrM8, 0x00fd09e8 + 0x08, size);
          MainMemory.mmrM8[0x00fd09e8 + 0x5a] = (byte) hdMax;
          ByteArray.byaWstr (MainMemory.mmrM8, 0x00fdffe0, name);
          MainMemory.mmrM8[0x00fdffff] = (byte) code;
        }
      }
    }
    //----------------------------------------------------------------
    //-rom
    //  ROMデータを上書きする
    if (romROMData != null) {
      System.arraycopy (romROMData, 0,  //from
                        MainMemory.mmrM8, 0x00f00000,  //to
                        0x01000000 - 0x00f00000);  //length
    }
    //----------------------------------------------------------------
    //-cgrom
    //  CGROMデータを上書きする
    //  768KBだけ使う
    if (romCGROMData != null) {
      System.arraycopy (romCGROMData, 0,  //from
                        MainMemory.mmrM8, 0x00f00000,  //to
                        0x00fc0000 - 0x00f00000);  //length
    }
    //----------------------------------------------------------------
    //CGROMにANK6x12がなければIPLROM 1.6からコピーする
    {
      int t = 0;
      for (int y = 0; y < 12; y++) {
        t |= MainMemory.mmrM8[0x00fbf400 + 12 * '!' + y];
      }
      if (t == 0) {  //CGROMにANK6x12がない
        System.arraycopy (iplrom16scsi256, 0x00fef400 - 0x00fc0000,  //from
                          MainMemory.mmrM8, 0x00fbf400,  //to
                          12 * 256);  //length
      }
    }
    //----------------------------------------------------------------
    //-omusubi
    if (romOmusubiOn) {
      System.arraycopy (OMUSUBIFONT, 0,  //from
                        MainMemory.mmrM8, 0x00f3a800,  //to
                        16 * 256);  //length
    }
    //----------------------------------------------------------------
    //-rom30
    //  X68030のIPLROMデータを上書きする
    //  128KBまたは256KB使う
    //  IPLROM 1.6の一部分だけを上書きすることはできない
    if (romROM30Data != null &&
        !(romCurrent16Size == 256 && romROM30Data.length == 1024 * 128 && romIPLROMData == null)) {
      System.arraycopy (romROM30Data, 0,  //from
                        MainMemory.mmrM8, 0x00fc0000,  //to
                        romROM30Data.length);  //length
    }
    //----------------------------------------------------------------
    //-scsiinrom
    //  SCSIINROMデータを上書きする
    //  IPLROM 1.6の一部分だけを上書きすることはできない
    if (romSCSIINROMData != null &&
        !(romCurrent16Size == 256)) {
      System.arraycopy (romSCSIINROMData, 0,  //from
                        MainMemory.mmrM8, 0x00fc0000,  //to
                        0x00fc2000 - 0x00fc0000);  //length
    }
    //----------------------------------------------------------------
    //-iplrom
    //  IPLROMデータを上書きする
    //  IPLROM 1.6の一部分だけを上書きすることはできない
    if (romIPLROMData != null &&
        !(romCurrent16Size == 256 && romROM30Data == null)) {
      System.arraycopy (romIPLROMData, 0,  //from
                        MainMemory.mmrM8, 0x00fe0000,  //to
                        0x01000000 - 0x00fe0000);  //length
    }
    //----------------------------------------------------------------
    //-scsiexrom
    //  SCSIEXROMデータまたはSCSI16EX.DATをコピーする
    //  必要なときだけ接続される
    if (romSCSIEXROMData != null && romSCSIEXROMData.length == 8160) {
      Arrays.fill (MainMemory.mmrM8,  //array
                   0x00ea0000,  //from
                   0x00ea0020,  //to
                   (byte) 0xff);  //value
      System.arraycopy (romSCSIEXROMData, 0,  //from
                        MainMemory.mmrM8, 0x00ea0020,  //to
                        0x00ea2000 - 0x00ea0020);  //length
    } else {
      System.arraycopy (romSCSIEXROMData != null ? romSCSIEXROMData : scsi16exdat, 0,  //from
                        MainMemory.mmrM8, 0x00ea0000,  //to
                        0x00ea2000 - 0x00ea0000);  //length
    }
  }


  //romOpenXEIJROMDialog ()
  //  XEIJROM.DATを保存するダイアログを開く
  public static void romOpenXEIJROMDialog () {
    romLastXEIJROMFile = romOpenDATDialog ("Create XEIJROM.DAT : Entire ROM image file", romLastXEIJROMFile, romMakeXEIJROMArray ());
  }

  //romOpenXEIJROM30Dialog ()
  //  XEIJROM30.DATを保存するダイアログを開く
  public static void romOpenXEIJROM30Dialog () {
    romLastXEIJROM30File = romOpenDATDialog ("Create XEIJROM30.DAT : The first half of IPLROM image file", romLastXEIJROM30File, romMakeXEIJROM30Array ());
  }

  //romOpenXEIJIPLROM30Dialog ()
  //  XEIJIPLROM30.DATを保存するダイアログを開く
  public static void romOpenXEIJIPLROM30Dialog () {
    romLastXEIJIPLROM30File = romOpenDATDialog ("Create XEIJIPLROM30.DAT : The second half of IPLROM image file", romLastXEIJIPLROM30File, romMakeXEIJIPLROM30Array ());
  }

  //romOpenXEIJSCSIINROMDialog ()
  //  XEIJSCSIINROM.DATを保存するダイアログを開く
  public static void romOpenXEIJSCSIINROMDialog () {
    romLastXEIJSCSIINROMFile = romOpenDATDialog ("Create XEIJSCSIINROM.DAT : SCSIINROM image file", romLastXEIJSCSIINROMFile, romMakeXEIJSCSIINROMArray ());
  }

  //romOpenXEIJSCSIEXROMDialog ()
  //  XEIJSCSIEXROM.DATを保存するダイアログを開く
  public static void romOpenXEIJSCSIEXROMDialog () {
    romLastXEIJSCSIEXROMFile = romOpenDATDialog ("Create XEIJSCSIEXROM.DAT : SCSIEXROM image file", romLastXEIJSCSIEXROMFile, romMakeXEIJSCSIEXROMArray ());
  }

  //romOpenEVEN030Dialog ()
  //  EVEN030.BINを保存するダイアログを開く
  public static void romOpenEVEN030Dialog () {
    romLastEVEN030File = romOpenBINDialog ("Create EVEN030.BIN : EVEN ROM binary file for X68030", romLastEVEN030File, romMakeEVEN030Array ());
  }

  //romOpenODD030Dialog ()
  //  ODD030.BINを保存するダイアログを開く
  public static void romOpenODD030Dialog () {
    romLastODD030File = romOpenBINDialog ("Create ODD030.BIN : ODD ROM binary file for X68030", romLastODD030File, romMakeODD030Array ());
  }

  //romOpenEVENXVIDialog ()
  //  EVENXVI.BINを保存するダイアログを開く
  public static void romOpenEVENXVIDialog () {
    romLastEVENXVIFile = romOpenBINDialog ("Create EVENXVI.BIN : EVEN ROM binary file for X68000 XVI", romLastEVENXVIFile, romMakeEVENXVIArray ());
  }

  //romOpenODDXVIDialog ()
  //  ODDXVI.BINを保存するダイアログを開く
  public static void romOpenODDXVIDialog () {
    romLastODDXVIFile = romOpenBINDialog ("Create ODDXVI.BIN : ODD ROM binary file for X68000 XVI", romLastODDXVIFile, romMakeODDXVIArray ());
  }

  //romOpenEVENPRODialog ()
  //  EVENPRO.BINを保存するダイアログを開く
  public static void romOpenEVENPRODialog () {
    romLastEVENPROFile = romOpenBINDialog ("Create EVENPRO.BIN : EVEN ROM binary file for X68000 PRO", romLastEVENPROFile, romMakeEVENPROArray ());
  }

  //romOpenODDPRODialog ()
  //  ODDPRO.BINを保存するダイアログを開く
  public static void romOpenODDPRODialog () {
    romLastODDPROFile = romOpenBINDialog ("Create ODDPRO.BIN : ODD ROM binary file for X68000 PRO", romLastODDPROFile, romMakeODDPROArray ());
  }

  //romOpenDATDialog (datFile, datArray)
  //  データファイルを保存するダイアログを開く
  public static File romOpenDATDialog (String title, File datFile, byte[] datArray) {
    if (datFile == null || datArray == null) {
      return null;
    }
    JFileChooser2 fileChooser = new JFileChooser2 ();
    fileChooser.setDialogTitle (title);
    fileChooser.setFileFilter (new javax.swing.filechooser.FileFilter () {  //java.io.FileFilterと紛らわしい
      @Override public boolean accept (File file) {
        String name = file.getName ();
        String upperName = name.toUpperCase ();
        return (file.isDirectory () ||
                (file.isFile () &&
                 upperName.endsWith (".DAT")));
      }
      @Override public String getDescription () {
        return (Multilingual.mlnJapanese ?
                "データファイル (*.DAT)" :
                "data file (*.DAT)");
      }
    });
    fileChooser.setSelectedFile (datFile);
    if (fileChooser.showSaveDialog (null) == JFileChooser.APPROVE_OPTION) {
      datFile = fileChooser.getSelectedFile ();
      XEiJ.rscPutFile (datFile.getPath (), datArray);
    }
    return datFile;
  }

  //romOpenBINDialog (binFile, binArray)
  //  バイナリファイルを保存するダイアログを開く
  public static File romOpenBINDialog (String title, File binFile, byte[] binArray) {
    if (binFile == null || binArray == null) {
      return null;
    }
    JFileChooser2 fileChooser = new JFileChooser2 ();
    fileChooser.setDialogTitle (title);
    fileChooser.setFileFilter (new javax.swing.filechooser.FileFilter () {  //java.io.FileFilterと紛らわしい
      @Override public boolean accept (File file) {
        String name = file.getName ();
        String upperName = name.toUpperCase ();
        return (file.isDirectory () ||
                (file.isFile () &&
                 upperName.endsWith (".BIN")));
      }
      @Override public String getDescription () {
        return (Multilingual.mlnJapanese ?
                "バイナリファイル (*.BIN)" :
                "binary file (*.BIN)");
      }
    });
    fileChooser.setSelectedFile (binFile);
    if (fileChooser.showSaveDialog (null) == JFileChooser.APPROVE_OPTION) {
      binFile = fileChooser.getSelectedFile ();
      XEiJ.rscPutFile (binFile.getPath (), binArray);
    }
    return binFile;
  }

  //datArray = romMakeXEIJROMArray ()
  //  XEIJROM.DATの配列を作る
  //  1MB
  public static byte[] romMakeXEIJROMArray () {
    byte[] datArray = new byte[1024 * 1024];
    System.arraycopy (MainMemory.mmrM8, 0x00f00000 - 0x00000000,  //コピー元
                      datArray, 0x00f00000 - 0x00f00000,  //コピー先
                      1024 * 1024);  //長さ
    return datArray;
  }

  //datArray = romMakeXEIJROM30Array ()
  //  XEIJROM30.DATの配列を作る
  //  128KB
  public static byte[] romMakeXEIJROM30Array () {
    byte[] datArray = new byte[1024 * 128];
    System.arraycopy (MainMemory.mmrM8, 0x00fc0000 - 0x00000000,  //コピー元
                      datArray, 0x00f00000 - 0x00f00000,  //コピー先
                      1024 * 128);  //長さ
    return datArray;
  }

  //datArray = romMakeXEIJIPLROM30Array ()
  //  XEIJIPLROM30.DATの配列を作る
  //  128KB
  public static byte[] romMakeXEIJIPLROM30Array () {
    byte[] datArray = new byte[1024 * 128];
    System.arraycopy (MainMemory.mmrM8, 0x00fe0000 - 0x00000000,  //コピー元
                      datArray, 0x00f00000 - 0x00f00000,  //コピー先
                      1024 * 128);  //長さ
    return datArray;
  }

  //datArray = romMakeXEIJSCSIINROMArray ()
  //  XEIJSCSIINROM.DATの配列を作る
  //  8KB
  public static byte[] romMakeXEIJSCSIINROMArray () {
    byte[] datArray = new byte[1024 * 8];
    System.arraycopy (scsi16indat, 0,  //コピー元
                      datArray, 0,  //コピー先
                      1024 * 8);  //長さ
    return datArray;
  }

  //datArray = romMakeXEIJSCSIEXROMArray ()
  //  XEIJSCSIEXROM.DATの配列を作る
  //  8KB
  public static byte[] romMakeXEIJSCSIEXROMArray () {
    byte[] datArray = new byte[1024 * 8];
    System.arraycopy (scsi16exdat, 0,  //コピー元
                      datArray, 0,  //コピー先
                      1024 * 8);  //長さ
    return datArray;
  }

  //binArray = romMakeEVEN030Array ()
  //  EVEN030.BINの配列を作る
  //  X68030用EVEN ROM。256Kbit*16=4Mbit(512KB)。リトルエンディアン
  public static byte[] romMakeEVEN030Array () {
    byte[] datArray = romMakeXEIJROMArray ();
    if (datArray == null) {
      return null;
    }
    byte[] binArray = new byte[1024 * 512];
    for (int i = 0; i < 1024 * 1024 / 4; i++) {
      binArray[2 * i + 1] = datArray[4 * i    ];
      binArray[2 * i    ] = datArray[4 * i + 1];
    }
    return binArray;
  }

  //binArray = romMakeODD030Array ()
  //  ODD030.BINの配列を作る
  //  X68030用ODD ROM。256Kbit*16=4Mbit(512KB)。リトルエンディアン
  public static byte[] romMakeODD030Array () {
    byte[] datArray = romMakeXEIJROMArray ();
    if (datArray == null) {
      return null;
    }
    byte[] binArray = new byte[1024 * 512];
    for (int i = 0; i < 1024 * 1024 / 4; i++) {
      binArray[2 * i + 1] = datArray[4 * i + 2];
      binArray[2 * i    ] = datArray[4 * i + 3];
    }
    return binArray;
  }

  //binArray = romMakeEVENXVIArray ()
  //  EVENXVI.BINの配列を作る
  //  XVI用(増設RAMボードCZ-6BE2A用)EVEN ROM。128Kbit*8=1024Mbit(128KB)
  public static byte[] romMakeEVENXVIArray () {
    byte[] datArray = romMakeXEIJROMArray ();
    if (datArray == null) {
      return null;
    }
    byte[] binArray = new byte[1024 * 128];
    for (int i = 0; i < 1024 * 256 / 2; i++) {
      binArray[i] = datArray[1024 * 768 + 2 * i    ];
    }
    return binArray;
  }

  //binArray = romMakeODDXVIArray ()
  //  ODDXVI.BINの配列を作る
  //  XVI用(増設RAMボードCZ-6BE2A用)ODD ROM。128Kbit*8=1024Mbit(128KB)
  public static byte[] romMakeODDXVIArray () {
    byte[] datArray = romMakeXEIJROMArray ();
    if (datArray == null) {
      return null;
    }
    byte[] binArray = new byte[1024 * 128];
    for (int i = 0; i < 1024 * 256 / 2; i++) {
      binArray[i] = datArray[1024 * 768 + 2 * i + 1];
    }
    return binArray;
  }

  //binArray = romMakeEVENPROArray ()
  //  EVENPRO.BINの配列を作る
  //  PRO用EVEN ROM。64Kbit*8=512Mbit=64KB
  public static byte[] romMakeEVENPROArray () {
    byte[] datArray = romMakeXEIJROMArray ();
    if (datArray == null) {
      return null;
    }
    byte[] binArray = new byte[(0x01000000 - 0x00fe0000) / 2];
    for (int i = 0; i < binArray.length; i++) {
      binArray[i] = datArray[0x00fe0000 - 0x00f00000 + 2 * i    ];
    }
    return binArray;
  }

  //binArray = romMakeODDPROArray ()
  //  ODDPRO.BINの配列を作る
  //  PRO用ODD ROM。64Kbit*8=512Mbit=64KB
  public static byte[] romMakeODDPROArray () {
    byte[] datArray = romMakeXEIJROMArray ();
    if (datArray == null) {
      return null;
    }
    byte[] binArray = new byte[(0x01000000 - 0x00fe0000) / 2];
    for (int i = 0; i < binArray.length; i++) {
      binArray[i] = datArray[0x00fe0000 - 0x00f00000 + 2 * i + 1];
    }
    return binArray;
  }


  //romLoadResource ()
  //  リソースをロードする
  public static void romLoadResource () {
    //----------------------------------------------------------------
    //CGROM_XEiJ.DAT
    cgromxeijdat = XEiJ.rscGetResource ("CGROM_XEiJ.DAT");
    if (cgromxeijdat == null ||
        cgromxeijdat.length != 1024 * 768) {
      System.out.println ("CGROM_XEiJ.DAT error");
    }
    //----------------------------------------------------------------
    //IPLROM.DAT
    iplromdat = XEiJ.rscGetResource ("IPLROM.DAT");
    if (iplromdat == null ||
        //iplromdat.length != 1024 * 128 ||
        ByteArray.crc32 (iplromdat) != 0x72bdf532) {
      System.out.println ("IPLROM.DAT error");
    }
    //----------------------------------------------------------------
    //IPLROMXV.DAT
    iplromxvdat = XEiJ.rscGetResource ("IPLROMXV.DAT");
    if (iplromxvdat == null ||
        //iplromxvdat.length != 1024 * 128 ||
        ByteArray.crc32 (iplromxvdat) != 0x00eeb408) {
      System.out.println ("IPLROMXV.DAT error");
    }
    //----------------------------------------------------------------
    //IPLROMCO.DAT
    iplromcodat = XEiJ.rscGetResource ("IPLROMCO.DAT");
    if (iplromcodat == null ||
        //iplromcodat.length != 1024 * 128 ||
        ByteArray.crc32 (iplromcodat) != 0x6c7ef608) {
      System.out.println ("IPLROMCO.DAT error");
    }
    //----------------------------------------------------------------
    //IPLROM30.DAT
    iplrom30dat = XEiJ.rscGetResource ("IPLROM30.DAT");
    if (iplrom30dat == null ||
        //iplrom30dat.length != 1024 * 128 ||
        ByteArray.crc32 (iplrom30dat) != 0xe8f8fdad) {
      System.out.println ("IPLROM30.DAT error");
    }
    //----------------------------------------------------------------
    //HUMAN.SYS
    humansys = XEiJ.rscGetResource ("HUMAN.SYS");
    if (humansys == null ||
        //ByteArray.byaRls (humansys, 12) != 0xa890 ||
        //ByteArray.byaRls (humansys, 16) != 0x33ea ||
        ByteArray.crc32 (humansys) != 0xaf4bcc50) {
      System.out.println ("HUMAN.SYS error");
    }
    //----------------------------------------------------------------
    //FLOAT2.X
    float2x = XEiJ.rscGetResource ("FLOAT2.X");
    if (float2x == null ||
        //ByteArray.byaRws (float2x, 0) != ('H' << 8 | 'U') ||
        ByteArray.crc32 (float2x) != 0x65b19087) {
      System.out.println ("FLOAT2.X error");
    }
    //----------------------------------------------------------------
    //DB.X
    dbx = XEiJ.rscGetResource ("DB.X");
    if (dbx == null ||
        //ByteArray.byaRws (dbx, 0) != ('H' << 8 | 'U') ||
        ByteArray.crc32 (dbx) != 0x7addeb5e) {
      System.out.println ("DB.X error");
    }
  }


  //romMakeROMImage ()
  //  ROMイメージを作る
  public static void romMakeROMImage () {

    //================================================================
    //  scsi16indat
    //    $00FC0000～$00FC1FFF  SCSIINROM 16
    scsi16indat = new byte[1024 * 8];
    //  SCSI16INを確認する
    int scsi16inlength = SCSI16IN.length;
    if (1024 * 8 < scsi16inlength) {
      System.out.println ("SCSI16IN error");
    }
    //  SCSI16INをコピーする
    System.arraycopy (SCSI16IN, 0,  //from
                      scsi16indat, 0,  //to
                      scsi16inlength);  //length
    //  空きを$FFで充填する
    Arrays.fill (scsi16indat,  //array
                 scsi16inlength,  //from
                 1024 * 8,  //to
                 (byte) 0xff);  //value

    //================================================================
    //  scsi16exdat
    //    $00EA0000～$00EA1FFF  SCSIEXROM 16
    scsi16exdat = new byte[1024 * 8];
    //  SCSI16EXを確認する
    if (1024 * 8 < SCSI16EX.length) {
      System.out.println ("SCSI16EX error");
    }
    //  SCSI16EXをコピーする
    System.arraycopy (SCSI16EX, 0,  //from
                      scsi16exdat, 0,  //to
                      SCSI16EX.length);  //length
    //  空きを$FFで充填する
    Arrays.fill (scsi16exdat,  //array
                 SCSI16EX.length,  //from
                 1024 * 8,  //to
                 (byte) 0xff);  //value

    //================================================================
    //  iplrom16scsi256
    //    $00FC0000～$00FC1FFF  SCSIINROM 16
    //    $00FC2000～$00FCFFFF  ROM Human 2.60
    //    $00FD0000～$00FD57FF  ROM FLOAT 2.03
    //    $00FD5800～$00FE9FFF  ROM Debugger 3.60
    //    $00FEA000～$00FEF3FF  IPL/BIOS 1.6 2nd
    //    $00FEF400～$00FEFFFF  ANK6x12
    //    $00FF0000～$00FFFFFF  IPL/BIOS 1.6 1st
    iplrom16scsi256 = new byte[1024 * 256];
    //----------------------------------------------------------------
    //$00FC0000～$00FC1FFF  SCSIINROM 16
    //  SCSI16IN.DATをコピーする
    int scsi16instart = 0x00fc0000;
    System.arraycopy (scsi16indat, 0,  //from
                      iplrom16scsi256, scsi16instart - 0x00fc0000,  //to
                      1024 * 8);  //length
    //----------------------------------------------------------------
    //$00FEA000～$00FEF3FF  IPL/BIOS 1.6 2nd
    //$00FF0000～$00FFFFFF  IPL/BIOS 1.6 1st
    //  パッチデータを確認する
    int ipl16start = ByteArray.byaRls (ROM16PAT_DATA, 0);
    int ipl16length = ROM16PAT_TEXT.length;
    if (ipl16start != 0x00fea000 ||
        0x00fef400 < ipl16start + ipl16length) {
      System.out.println ("ROM16PAT_DATA error");
    }
    //  IPLROM 1.3の$00FF0000～$00FFFFFFをコピーする
    System.arraycopy (iplrom30dat, 0x00ff0000 - 0x00fe0000,  //from
                      iplrom16scsi256, 0x00ff0000 - 0x00fc0000,  //to
                      0x01000000 - 0x00ff0000);  //length
    //  IPLROMCO.DATの$00FF95B6～$00FF9E7Dを$00FFCCB8～にコピーする
    System.arraycopy (iplromcodat, 0x00ff95b6 - 0x00fe0000,  //from
                      iplrom16scsi256, 0x00ffccb8 - 0x00fc0000,  //to
                      0x00ff9e7e - 0x00ff95b6);  //length
    //  SHARPロゴを移動させる
    System.arraycopy (iplrom16scsi256, 0x00ff138c - 0x00fc0000,  //from
                      iplrom16scsi256, 0x00ffd680 - 0x00fc0000,  //to
                      0x00ff1450 - 0x00ff138c);  //length
    //  X68000ロゴを埋め込む
    if (romX68000LogoData != null) {
      byte[] b = romX68000LogoData;
      System.arraycopy (b, 0,  //from
                        iplrom16scsi256, 0x00ff12a8 - 0x00fc0000,  //to
                        b.length);  //length
      int w = ByteArray.byaRwz (b, 0);
      if (112 < w) {  //標準より大きいとき
        //SERIESを表示しない
        byte[] crlfnl = new byte[] { 0x0d, 0x0a, 0x00 };
        System.arraycopy (crlfnl, 0,  //from
                          iplrom16scsi256, 0x00ff11c4 - 0x00fc0000,  //to
                          crlfnl.length);  //length
      }
    }
    //  パッチデータの追加コードをコピーする
    System.arraycopy (ROM16PAT_TEXT, 0,  //from
                      iplrom16scsi256, ipl16start - 0x00fc0000,  //to
                      ipl16length);  //length
    //  パッチデータの挿入コードを展開する
    for (int i = 8; i < ROM16PAT_DATA.length; ) {
      int a = ByteArray.byaRls (ROM16PAT_DATA, i);  //先頭アドレス(0=終了)
      if (a == 0) {
        break;
      }
      int e = ByteArray.byaRls (ROM16PAT_DATA, i + 4);  //末尾アドレス
      int d = ByteArray.byaRls (ROM16PAT_DATA, i + 8);  //元のデータ
      int l = ByteArray.byaRls (ROM16PAT_DATA, i + 12);  //長さ
      if (ByteArray.byaRls (iplrom16scsi256, a - 0x00fc0000) == d) {
        System.arraycopy (ROM16PAT_DATA, i + 16,  //from
                          iplrom16scsi256, a - 0x00fc0000,  //to
                          l);  //length
        for (a += l; a <= e; a += 2) {
          ByteArray.byaWw (iplrom16scsi256, a - 0x00fc0000, 0x2048);  //残りをMOVEA.L A0,A0で充填する。必須
        }
      } else {
        System.out.printf ("ROM16PAT_DATA error at $%08X\n", a);
      }
      i += 16 + l;
    }
    //スタイルを埋め込む
    {
      int a = ipl16start;;
      while (iplrom16scsi256[a - 0x00fc0000] != 0) {
        a++;
      }
      a++;
      iplrom16scsi256[a - 0x00fc0000] = (byte) romIPLROM16Style;
    }
    //----------------------------------------------------------------
    //$00FEF400～$00FEFFFF  ANK6x12
    //  IPLROM 1.2の$00FFD45E～$00FFE045を$00FEF400～にコピーする
    int ank6x12start = 0x00fef400;
    int ank6x12length1 = 12 * 254;
    int ank6x12length = 12 * 256;
    System.arraycopy (iplromcodat, 0x00ffd45e - 0x00fe0000,  //from
                      iplrom16scsi256, ank6x12start - 0x00fc0000,  //to
                      ank6x12length1);  //length
    //  空きを$00で充填する
    Arrays.fill (iplrom16scsi256,  //array
                 ank6x12start + ank6x12length1 - 0x00fc0000,  //from
                 ank6x12start + ank6x12length - 0x00fc0000,  //to
                 (byte) 0x00);  //value
    //----------------------------------------------------------------
    //$00FC2000～$00FCFFFF  ROM Human 2.60
    int humanzstart = 0x00fc2000;
    int humanstart = humanzstart + 0x1c;
    int humantextlength1 = ByteArray.byaRls (humansys, 12);  //HUMAN.SYSのtextの長さ
    int humandatalength1 = ByteArray.byaRls (humansys, 16);  //HUMAN.SYSのdataの長さ
    int humanlength1 = humantextlength1 + humandatalength1;  //HUMAN.SYSのtext+dataの長さ
    int humanlength2 = HU260PAT_TEXT.length;  //追加する長さ
    int humanlength = humanlength1 + humanlength2;  //追加後の長さ
    //  パッチデータを確認する
    if (ByteArray.byaRls (HU260PAT_DATA, 0) != humanstart + humanlength1) {
      System.out.println ("HU260PAT_DATA error");
      return;
    }
    //  Z形式ヘッダを作る
    ByteArray.byaWw (iplrom16scsi256, humanzstart + 0x00 - 0x00fc0000, 0x601a);
    ByteArray.byaWl (iplrom16scsi256, humanzstart + 0x02 - 0x00fc0000, humantextlength1);
    ByteArray.byaWl (iplrom16scsi256, humanzstart + 0x06 - 0x00fc0000, humandatalength1);
    ByteArray.byaWl (iplrom16scsi256, humanzstart + 0x0a - 0x00fc0000, 0x0);
    ByteArray.byaWl (iplrom16scsi256, humanzstart + 0x0e - 0x00fc0000, 0x0);
    ByteArray.byaWl (iplrom16scsi256, humanzstart + 0x12 - 0x00fc0000, 0x0);
    ByteArray.byaWl (iplrom16scsi256, humanzstart + 0x16 - 0x00fc0000, 0x6800);
    ByteArray.byaWw (iplrom16scsi256, humanzstart + 0x1a - 0x00fc0000, 0xffff);
    //  HUMAN.SYSのtext+dataをコピーする
    System.arraycopy (humansys, 64,  //from
                      iplrom16scsi256, humanstart - 0x00fc0000,  //to
                      humanlength1);  //length
    //  パッチデータの追加コードをコピーする
    System.arraycopy (HU260PAT_TEXT, 0,  //from
                      iplrom16scsi256, humanstart + humanlength1 - 0x00fc0000,  //to
                      humanlength2);  //length
    //  パッチデータの挿入コードを展開する
    for (int i = 8; i < HU260PAT_DATA.length; ) {
      int a = ByteArray.byaRls (HU260PAT_DATA, i);  //先頭アドレス(0=終了)
      if (a == 0) {
        break;
      }
      int e = ByteArray.byaRls (HU260PAT_DATA, i + 4);  //末尾アドレス
      int d = ByteArray.byaRls (HU260PAT_DATA, i + 8);  //元のデータ
      int l = ByteArray.byaRls (HU260PAT_DATA, i + 12);  //長さ
      if (ByteArray.byaRls (iplrom16scsi256, a - 0x00fc0000) == d) {
        System.arraycopy (HU260PAT_DATA, i + 16,  //from
                          iplrom16scsi256, a - 0x00fc0000,  //to
                          l);  //length
        for (a += l; a <= e; a += 2) {
          ByteArray.byaWw (iplrom16scsi256, a - 0x00fc0000, 0x2048);  //残りをMOVEA.L A0,A0で充填する。必須
        }
      } else {
        System.out.printf ("HU260PAT_DATA error at $%08X\n", a);
        return;
      }
      i += 16 + l;
    }
    //  空きを$FFで充填する
    Arrays.fill (iplrom16scsi256,  //array
                 humanstart + humanlength - 0x00fc0000,  //from
                 0x00fd0000 - 0x00fc0000,  //to
                 (byte) 0xff);  //value
    //----------------------------------------------------------------
    //$00FD0000～$00FD57FF  ROM FLOAT 2.03
    int floatstart = 0x00fd0000;
    int floatlength = float2x.length;
    //  FLOAT2.Xをコピーする
    System.arraycopy (float2x, 0,  //from
                      iplrom16scsi256, floatstart - 0x00fc0000,  //to
                      floatlength);  //length
    //  空きを$FFで充填する
    Arrays.fill (iplrom16scsi256,  //array
                 floatstart + floatlength - 0x00fc0000,  //from
                 0x00fd5800 - 0x00fc0000,  //to
                 (byte) 0xff);  //value
    //----------------------------------------------------------------
    //$00FD5800～$00FE9FFF  ROM Debugger 3.60
    int debuggerstart = 0x00fd5800;
    int debuggerlength1 = ByteArray.byaRls (dbx, 12) + ByteArray.byaRls (dbx, 16);  //DB.Xのtext+dataの長さ
    //  DB.Xをコピーする
    System.arraycopy (dbx, 64,  //from
                      iplrom16scsi256, debuggerstart - 0x00fc0000,  //to
                      debuggerlength1);  //length
    //  DB.Xをリロケートする
    int displacement = debuggerstart - ByteArray.byaRls (dbx, 4);  //ディスプレースメント=新しいベースアドレス-現在のベースアドレス
    int rtstart = 64 + debuggerlength1;  //リロケートテーブルの開始位置
    int rtend = rtstart + ByteArray.byaRls (dbx, 24);  //リロケートテーブルの終了位置
    int tdindex = debuggerstart;  //リロケート位置
    for (int rtindex = rtstart; rtindex < rtend; ) {
      int tdoffset = ByteArray.byaRwz (dbx, rtindex);  //前回のリロケート位置からのオフセット
      rtindex += 2;
      if (tdoffset == 1) {  //オフセットが1のときはロングで読み直す
        tdoffset = ByteArray.byaRls (dbx, rtindex);
        rtindex += 4;
      }
      if ((tdoffset & 1) == 0) {  //オフセットが偶数のときはロングでリロケートする
        tdindex += tdoffset;  //リロケート位置
        ByteArray.byaWl (iplrom16scsi256, tdindex - 0x00fc0000, ByteArray.byaRls (iplrom16scsi256, tdindex - 0x00fc0000) + displacement);
      } else {  //オフセットが奇数のときはワードでリロケートする
        tdoffset &= ~1;
        tdindex += tdoffset;  //リロケート位置
        ByteArray.byaWw (iplrom16scsi256, tdindex - 0x00fc0000, ByteArray.byaRwz (iplrom16scsi256, tdindex - 0x00fc0000) + displacement);
      }
    }
    //  DB.XをROMデバッガに改造する
    byte[] diffbody = ROMDB_DIFF;
    int difflength = diffbody.length;
    if (ByteArray.byaRls (diffbody, 0) != debuggerstart) {
      return;
    }
    int debuggerlength = ByteArray.byaRls (diffbody, 4);
    int blockstart = debuggerstart;
    for (int diffindex = 8; diffindex < difflength; ) {
      int blockoffset = ByteArray.byaRwz (diffbody, diffindex);
      diffindex += 2;
      if (blockoffset == 1) {
        blockoffset = ByteArray.byaRls (diffbody, diffindex);
        diffindex += 4;
      }
      blockstart += blockoffset;
      int blocklength = ByteArray.byaRwz (diffbody, diffindex);
      diffindex += 2;
      if (blocklength == 1) {
        blocklength = ByteArray.byaRls (diffbody, diffindex);
        diffindex += 4;
      }
      if ((blocklength & 1) == 0) {
        for (int i = 0; i < blocklength; i += 2) {
          ByteArray.byaWw (iplrom16scsi256, blockstart + i - 0x00fc0000, 0x2048);
        }
      } else {
        blocklength--;
        for (int i = 0; i < blocklength; i += 2) {
          ByteArray.byaWw (iplrom16scsi256, blockstart + i - 0x00fc0000, ByteArray.byaRwz (diffbody, diffindex + i));
        }
        diffindex += blocklength;
      }
      blockstart += blocklength;
    }
    //  空きを$FFで充填する
    Arrays.fill (iplrom16scsi256,  //array
                 debuggerstart + debuggerlength - 0x00fc0000,  //from
                 0x00fea000 - 0x00fc0000,  //to
                 (byte) 0xff);  //value
    //  ROM Debuggerはある
    ByteArray.byaWl (iplrom16scsi256, 0x00ff0008 - 0x00fc0000, debuggerstart);
    //  ROM Humanはある
    ByteArray.byaWl (iplrom16scsi256, 0x00ff000c - 0x00fc0000, humanzstart);
    //  ROM FLOATはある
    ByteArray.byaWl (iplrom16scsi256, 0x00ff0010 - 0x00fc0000, floatstart);
    //  SCSIINROMはある
    ByteArray.byaWl (iplrom16scsi256, 0x00ff0020 - 0x00fc0000, ByteArray.byaRls (iplrom16scsi256, 0x00fc0000 - 0x00fc0000));
    ByteArray.byaWl (iplrom16scsi256, 0x00ff0024 - 0x00fc0000, ByteArray.byaRls (iplrom16scsi256, 0x00fc0020 - 0x00fc0000));
    //  範囲を表示する
    System.out.printf ("IPLROM 1.6\n" +
                       "  $%08X-$%08X  SCSIINROM 16\n" +
                       "  $%08X-$%08X  ROM Human 2.60\n" +
                       "  $%08X-$%08X  ROM FLOAT 2.03\n" +
                       "  $%08X-$%08X  ROM Debugger 3.60\n" +
                       "  $%08X-$%08X  IPL/BIOS 1.6 2nd\n" +
                       "  $%08X-$%08X  ANK6x12\n" +
                       "  $%08X-$%08X  IPL/BIOS 1.6 1st\n",
                       scsi16instart, scsi16instart + scsi16inlength - 1,
                       humanzstart, humanstart + humanlength - 1,
                       floatstart, floatstart + floatlength - 1,
                       debuggerstart, debuggerstart + debuggerlength - 1,
                       ipl16start, ipl16start + ipl16length - 1,
                       ank6x12start, ank6x12start + ank6x12length - 1,
                       0x00ff0000, 0x00ffffff);

    //================================================================
    //  iplrom16scsi128
    //    $00FC0000～$00FC1FFF  SCSIINROM 16
    //    $00FC2000～$00FDFFFF  空き
    //    $00FE0000～$00FE4EFF  ROM Debugger 1.0
    //    $00FE4F00～$00FE9FFF  空き
    //    $00FEA000～$00FFFFFF  IPL/BIOS 1.6
    iplrom16scsi128 = new byte[1024 * 256];
    //$00FC0000～$00FC1FFF  SCSIINROM 16
    //  SCSI16IN.DATをコピーする
    System.arraycopy (scsi16indat, 0,  //from
                      iplrom16scsi128, 0x00fc0000 - 0x00fc0000,  //to
                      1024 * 8);  //length
    //$00FC2000～$00FDFFFF  空き
    Arrays.fill (iplrom16scsi128,  //array
                 0x00fc2000 - 0x00fc0000,  //from
                 0x00fe0000 - 0x00fc0000,  //to
                 (byte) 0xff);  //value
    //$00FE0000～$00FE4EFF  ROM Debugger 1.0
    //  IPLROM 1.2からコピーする
    System.arraycopy (iplromcodat, 0x00fe0000 - 0x00fe0000,  //from
                      iplrom16scsi128, 0x00fe0000 - 0x00fc0000,  //to
                      0x00fe4f00 - 0x00fe0000);  //length
    //  実効アドレスの計算で絶対ショートアドレスが符号拡張されない
    //    https://stdkmd.net/bugsx68k/#db_absoluteshort
    ByteArray.byaWw (iplrom16scsi128, 0x00fe0a26 - 0x00fc0000, 0x3015);      //00FE0A26  4240      clr.w d0              →  3015      move.w (a5),d0
    ByteArray.byaWw (iplrom16scsi128, 0x00fe0a28 - 0x00fc0000, 0x48c0);      //00FE0A28  3015      move.w (a5),d0        →  48C0      ext.l d0
    ByteArray.byaWl (iplrom16scsi128, 0x00fe0a40 - 0x00fc0000, 0x322dfffe);  //00FE0A40  4281      clr.l d1              →  322DFFFE  move.w -$0002(a5),d1
    //                                                                         00FE0A42  222DFFFE  move.l -$0002(a5),d1  →
    ByteArray.byaWw (iplrom16scsi128, 0x00fe0a44 - 0x00fc0000, 0x48c1);      //00FE0A44                                  →  48C1      ext.l d1
    //  CCRの未定義ビットを操作している
    //    https://stdkmd.net/bugsx68k/#db_ccrundefbit
    ByteArray.byaWl (iplrom16scsi128, 0x00fe2d10 - 0x00fc0000, 0x023c00fb);  //00FE2D10  023C007B  andi.b #$7B,ccr       →  023C00FB  andi.b #$FB,ccr
    //  リモートターミナルからの入力が1文字置きになる
    //    https://stdkmd.net/bugsx68k/#db_ctrls
    //    ^Sのときだけ空読みする方法もあるがターミナルの仕様に合わないのでここでは^Sで一時停止する機能そのものを削除する
    //    ターミナルはスクロールすれば過去に出力されたメッセージを読むことができるので一時停止する機能は通常は必要ない
    //    1.0と2.32でオフセットが違うことに注意
    ByteArray.byaWw (iplrom16scsi128, 0x00fe4ba6 - 0x00fc0000, 0x600e);      //00FE4BA6  610000EE  bsr.w $00FE4C96       →  600E      bra.s $00FE4BB6
    //$00FE4F00～$00FE9FFF  空き
    Arrays.fill (iplrom16scsi128,  //array
                 0x00fe4f00 - 0x00fc0000,  //from
                 0x00fea000 - 0x00fc0000,  //to
                 (byte) 0xff);  //value
    //$00FEA000～$00FFFFFF  IPL/BIOS 1.6
    System.arraycopy (iplrom16scsi256, 0x00fea000 - 0x00fc0000,  //from
                      iplrom16scsi128, 0x00fea000 - 0x00fc0000,  //to
                      0x01000000 - 0x00fea000);  //length
    //  ROM Debuggerはある
    ByteArray.byaWl (iplrom16scsi128, 0x00ff0008 - 0x00fc0000, 0x00fe0000);
    //  ROM Humanはない
    ByteArray.byaWl (iplrom16scsi128, 0x00ff000c - 0x00fc0000, 0);
    //  ROM FLOATはない
    ByteArray.byaWl (iplrom16scsi128, 0x00ff0010 - 0x00fc0000, 0);
    //  SCSIINROMはある
    //ByteArray.byaWl (iplrom16scsi128, 0x00ff0020 - 0x00fc0000, ByteArray.byaRls (iplrom16scsi128, 0x00fc0000 - 0x00fc0000));
    //ByteArray.byaWl (iplrom16scsi128, 0x00ff0024 - 0x00fc0000, ByteArray.byaRls (iplrom16scsi128, 0x00fc0020 - 0x00fc0000));

    //================================================================
    //  iplrom16sasi256
    //    $00FC0000～$00FC1FFF  空き
    //    $00FC2000～$00FCFFFF  ROM Human 2.60
    //    $00FD0000～$00FD57FF  ROM FLOAT 2.03
    //    $00FD5800～$00FE9FFF  ROM Debugger 3.60
    //    $00FEA000～$00FFFFFF  IPL/BIOS 1.6
    iplrom16sasi256 = new byte[1024 * 256];
    Arrays.fill (iplrom16sasi256,  //array
                 0x00fc0000 - 0x00fc0000,  //from
                 0x00fc2000 - 0x00fc0000,  //to
                 (byte) 0xff);  //value
    System.arraycopy (iplrom16scsi256, 0x00fc2000 - 0x00fc0000,  //from
                      iplrom16sasi256, 0x00fc2000 - 0x00fc0000,  //to
                      0x01000000 - 0x00fc2000);  //length
    //  ROM Debuggerはある
    //ByteArray.byaWl (iplrom16scsi256, 0x00ff0008 - 0x00fc0000, debuggerstart);
    //  ROM Humanはある
    //ByteArray.byaWl (iplrom16scsi256, 0x00ff000c - 0x00fc0000, humanzstart);
    //  ROM FLOATはある
    //ByteArray.byaWl (iplrom16scsi256, 0x00ff0010 - 0x00fc0000, floatstart);
    //  SCSIINROMはない
    ByteArray.byaWl (iplrom16scsi128, 0x00ff0020 - 0x00fc0000, 0);
    ByteArray.byaWl (iplrom16scsi128, 0x00ff0024 - 0x00fc0000, 0);

    //================================================================
    //  iplrom16sasi128
    //    $00FC0000～$00FC4EFF  ROM Debugger 1.0
    //    $00FC4F00～$00FC9FFF  空き
    //    $00FCA000～$00FDFFFF  IPL/BIOS 1.6
    //    $00FE0000～$00FE4EFF  ROM Debugger 1.0
    //    $00FE4F00～$00FE9FFF  空き
    //    $00FEA000～$00FFFFFF  IPL/BIOS 1.6
    iplrom16sasi128 = new byte[1024 * 256];
    System.arraycopy (iplrom16scsi128, 0x00fe0000 - 0x00fc0000,  //from
                      iplrom16sasi128, 0x00fe0000 - 0x00fc0000,  //to
                      0x01000000 - 0x00fe0000);  //length
    //  ROM Debuggerはある
    //ByteArray.byaWl (iplrom16scsi128, 0x00ff0008 - 0x00fc0000, 0x00fe0000);
    //  ROM Humanはない
    //ByteArray.byaWl (iplrom16scsi128, 0x00ff000c - 0x00fc0000, 0);
    //  ROM FLOATはない
    //ByteArray.byaWl (iplrom16scsi128, 0x00ff0010 - 0x00fc0000, 0);
    //  SCSIINROMはない
    ByteArray.byaWl (iplrom16scsi128, 0x00ff0020 - 0x00fc0000, 0);
    ByteArray.byaWl (iplrom16scsi128, 0x00ff0024 - 0x00fc0000, 0);
    //ゴースト
    System.arraycopy (iplrom16sasi128, 0x00fe0000 - 0x00fc0000,  //from
                      iplrom16sasi128, 0x00fc0000 - 0x00fc0000,  //to
                      1024 * 128);  //length

    //================================================================
    //  iplrom10sasi128
    //    $00FC0000～$00FDFFFF  IPLROM 1.0
    //    $00FE0000～$00FFFFFF  IPLROM 1.0
    iplrom10sasi128 = new byte[1024 * 256];
    System.arraycopy (iplromdat, 0,  //from
                      iplrom10sasi128, 0,  //to
                      1024 * 128);  //length
    System.arraycopy (iplromdat, 0,  //from
                      iplrom10sasi128, 1024 * 128,  //to
                      1024 * 128);  //length

    //================================================================
    //  iplrom10scsi128
    //    $00FC0000～$00FC1FFF  SCSIINROM 16
    //    $00FC2000～$00FDFFFF  空き
    //    $00FE0000～$00FFFFFF  IPLROM 1.0
    iplrom10scsi128 = new byte[1024 * 256];
    System.arraycopy (iplrom16scsi128, 0,  //from
                      iplrom10scsi128, 0,  //to
                      1024 * 128);  //length
    System.arraycopy (iplromdat, 0,  //from
                      iplrom10scsi128, 1024 * 128,  //to
                      1024 * 128);  //length

    //================================================================
    //  iplrom11sasi128
    //    $00FC0000～$00FDFFFF  IPLROM 1.1
    //    $00FE0000～$00FFFFFF  IPLROM 1.1
    iplrom11sasi128 = new byte[1024 * 256];
    System.arraycopy (iplromxvdat, 0,  //from
                      iplrom11sasi128, 0,  //to
                      1024 * 128);  //length
    System.arraycopy (iplromxvdat, 0,  //from
                      iplrom11sasi128, 1024 * 128,  //to
                      1024 * 128);  //length

    //================================================================
    //  iplrom11scsi128
    //    $00FC0000～$00FC1FFF  SCSIINROM 16
    //    $00FC2000～$00FDFFFF  空き
    //    $00FE0000～$00FFFFFF  IPLROM 1.1
    iplrom11scsi128 = new byte[1024 * 256];
    System.arraycopy (iplrom16scsi128, 0,  //from
                      iplrom11scsi128, 0,  //to
                      1024 * 128);  //length
    System.arraycopy (iplromxvdat, 0,  //from
                      iplrom11scsi128, 1024 * 128,  //to
                      1024 * 128);  //length

    //================================================================
    //  iplrom12sasi128
    //    $00FC0000～$00FDFFFF  IPLROM 1.2
    //    $00FE0000～$00FFFFFF  IPLROM 1.2
    iplrom12sasi128 = new byte[1024 * 256];
    System.arraycopy (iplromcodat, 0,  //from
                      iplrom12sasi128, 0,  //to
                      1024 * 128);  //length
    System.arraycopy (iplromcodat, 0,  //from
                      iplrom12sasi128, 1024 * 128,  //to
                      1024 * 128);  //length

    //================================================================
    //  iplrom12scsi128
    //    $00FC0000～$00FC1FFF  SCSIINROM 16
    //    $00FC2000～$00FDFFFF  空き
    //    $00FE0000～$00FFFFFF  IPLROM 1.2
    iplrom12scsi128 = new byte[1024 * 256];
    System.arraycopy (iplrom16scsi128, 0,  //from
                      iplrom12scsi128, 0,  //to
                      1024 * 128);  //length
    System.arraycopy (iplromcodat, 0,  //from
                      iplrom12scsi128, 1024 * 128,  //to
                      1024 * 128);  //length

    //================================================================
    //  iplrom13scsi128
    //    $00FC0000～$00FC0029  SCSIINROMヘッダ
    //    $00FC002A～$00FDFFFF  空き
    //    $00FE0000～$00FFFFFF  IPLROM 1.3
    iplrom13scsi128 = new byte[1024 * 256];
    {
      int d1 = ByteArray.byaRls (iplrom30dat, 0x00ff0020 - 0x00fe0000);
      int d2 = ByteArray.byaRls (iplrom30dat, 0x00ff0024 - 0x00fe0000);
      int d3 = ('S' << 24) | ('C' << 16) | ('S' << 8) | 'I';
      int d4 = ('I' << 8) | 'N';
      for (int a = 0x00fc0000; a < 0x00fc0020; a += 4) {
        ByteArray.byaWl (iplrom13scsi128, a - 0x00fc0000, d1);
      }
      ByteArray.byaWl (iplrom13scsi128, 0x00fc0020 - 0x00fc0000, d2);
      ByteArray.byaWl (iplrom13scsi128, 0x00fc0024 - 0x00fc0000, d3);
      ByteArray.byaWw (iplrom13scsi128, 0x00fc0028 - 0x00fc0000, d4);
      Arrays.fill (iplrom13scsi128,  //array
                   0x00fc002a - 0x00fc0000,  //from
                   1024 * 128,  //to
                   (byte) 0xff);  //value
    }
    System.arraycopy (iplrom30dat, 0,  //from
                      iplrom13scsi128, 1024 * 128,  //to
                      1024 * 128);  //length

  }

  //perl misc/ftob.pl OMUSUBIFONT misc/omusubifont.r
  public static final byte[] OMUSUBIFONT = "\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\08@8\4x\0\"\">\"\"\0\0\0\0\08@8\4x\0\"\24\b\24\"\0\0\0\0\0x@x@|\0\"\24\b\24\"\0\0\0\0\0x@x@|\0>\b\b\b\b\0\0\0\0\0x@x@|\0\34\"*$\32\0\0\0\0\08D|DD\0\"$8$\"\0\0\0\0\0xDxDx\0    >\0\0\0\0\0xDxDx\0\34 \34\2<\0\0\0\0\0DD|DD\0>\b\b\b\b\0\0\0\0\0@@@@|\0< <  \0\0\0\0\0DDD(\20\0>\b\b\b\b\0\0\0\0\0x@x@@\0< <  \0\0\0\0\08@@@<\0<\"<$\"\0\0\0\0\08@8\4x\0\34\"\"\"\34\0\0\0\0\08@8\4x\0\34\b\b\b\34\0\0\0\0\0xDDDx\0< < >\0\0\0\0\0xDDDx\0\30\b\b\b\b\0\0\0\0\0xDDDx\0\34\2\34 >\0\0\0\0\0xDDDx\0\34\2\34\2<\0\0\0\0\0xDDDx\0\4\f\24>\4\0\0\0\0\0DdTLD\0\"$8$\"\0\0\0\0\08@8\4x\0\"2*&\"\0\0\0\0\0x@x@|\0<\"<\"<\0\0\0\0\08@@@<\0\"2*&\"\0\0\0\0\0x@x@|\0\"6*\"\"\0\0\0\0\08@8\4x\0<\"<\"<\0\0\0\0\0x@x@|\0\34   \36\0\0\0\0\08\20\20\208\0\4\f\24>\4\0\0\0\0\08\20\20\208\0\34\2\34\2<\0\0\0\0\08\20\20\208\0\34\2\34 >\0\0\0\0\08\20\20\208\0\30\b\b\b\b\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\208888\20\20\0\208\20\0\0\0\0\0\0226lH\0\0\0\0\0\0\0\0\0\0\0\0DDD\376DDD\376DDD\0\0\0\0\0\20\20~\220\220|\22\22\374\20\20\0\0\0\0\0b\222\224h\b\20 ,R\222\214\0\0\0\0\0p\210\210P R\212\204\204\212r\0\0\0\0\0\b\0300 \0\0\0\0\0\0\0\0\0\0\b\20\20\20       \20\20\20\b\0 \20\20\20\b\b\b\b\b\b\b\20\20\20 \0\0\0\0\0\0\20\222|8|\222\20\0\0\0\0\0\0\0\0\20\20\20\376\20\20\20\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\b\0300 \0\0\0\0\0\0\0\0\0\376\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\208\20\0\0\0\0\0\2\2\4\b\b\20  @\200\200\0\0\0\0\08DD\202\202\202\202\202\202\202|\0\0\0\0\0p\20\20\20\20\20\20\20\20\20\20\0\0\0\0\0x\4\4\48@\200\200\200\200\376\0\0\0\0\0x\4\4\4x\4\2\2\2\2\374\0\0\0\0\0\f\24$DD\204\204\204\376\4\4\0\0\0\0\0|@@@x\4\2\2\2\2\374\0\0\0\0\08@@\200\270\304\202\202\202\202|\0\0\0\0\0~\2\4\4\b\b\b\20\20\20\20\0\0\0\0\08DDD8D\202\202\202\202|\0\0\0\0\08D\202\202\202\202~\2\4\48\0\0\0\0\0\0\208\20\0\0\0\208\20\0\0\0\0\0\0\0\208\20\0\0\0\b\0300 \0\0\0\0\0\2\4\b\20 @ \20\b\4\2\0\0\0\0\0\0\0\0~\0\0\0~\0\0\0\0\0\0\0\0@ \20\b\4\2\4\b\20 @\0\0\0\0\08DD\4\b\20\20\0\0\20\20\0\0\0\0\08D\202\202\236\242\242\242\236\200~\0\0\0\0\08DD\202\376\202\202\202\202\202\202\0\0\0\0\0\370\204\204\204\370\204\202\202\202\202\374\0\0\0\0\0<@@\200\200\200\200\200\200\200~\0\0\0\0\0\370\204\204\202\202\202\202\202\202\202\374\0\0\0\0\0\374\200\200\200\374\200\200\200\200\200\376\0\0\0\0\0\374\200\200\200\374\200\200\200\200\200\200\0\0\0\0\0<@@\200\236\202\202\202\202\206z\0\0\0\0\0\202\202\202\202\376\202\202\202\202\202\202\0\0\0\0\08\20\20\20\20\20\20\20\20\208\0\0\0\0\0\2\2\2\2\2\2\2\2\202\202|\0\0\0\0\0\204\210\220\240\340\220\210\204\204\202\202\0\0\0\0\0\200\200\200\200\200\200\200\200\200\200\376\0\0\0\0\0\202\202\306\306\252\252\252\222\222\222\202\0\0\0\0\0\202\302\242\242\222\222\212\212\206\206\202\0\0\0\0\08DD\202\202\202\252\222\252\222|\0\0\0\0\0\370\204\204\204\370\200\200\200\200\200\200\0\0\0\0\08DD\202\202\202\202\262\312\204z\0\0\0\0\0\370\204\204\204\370\220\210\204\204\202\202\0\0\0\0\0<@@@8\4\2\2\2\2\374\0\0\0\0\0\376\20\20\20\20\20\20\20\20\20\20\0\0\0\0\0\202\202\202\202\202\202\202\202\202\202|\0\0\0\0\0\202\202\202\202DDD((\20\20\0\0\0\0\0\202\202\222\222\222\252\252\252DDD\0\0\0\0\0\202\202D(\20(DD\202\202\202\0\0\0\0\0\202\202DD(\20\20\20\20\20\20\0\0\0\0\0|\4\b\b\20  @@\200\376\0\0\0\0<           <\0\0\0\0\202\202D(\20\376\20\376\20\20\20\0\0\0\0x\b\b\b\b\b\b\b\b\b\b\bx\0\0\0\0\20(D\202\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\376\0\0\0\0\0 0\30\b\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0x\4\2~\202\202~\0\0\0\0\0\200\200\200\200\270\304\202\202\202\202\374\0\0\0\0\0\0\0\0\0\36 @@@@>\0\0\0\0\0\2\2\2\2:F\202\202\202\202~\0\0\0\0\0\0\0\0\08D\202\376\200\200~\0\0\0\0\0\16\20\20\20\376\20\20\20\20\20\20\0\0\0\0\0\0\0\0\0:F\202\202\202\206z\2\4x\0\0\200\200\200\200\270\304\202\202\202\202\202\0\0\0\0\0\20\20\0\0\20\20\20\20\20\20\20\0\0\0\0\0\b\b\0\0\b\b\b\b\b\b\b\b\20\340\0\0@@@@BDHxDBB\0\0\0\0\0\20\20\20\20\20\20\20\20\20\20\20\0\0\0\0\0\0\0\0\0\354\222\222\222\222\222\222\0\0\0\0\0\0\0\0\0\270\304\202\202\202\202\202\0\0\0\0\0\0\0\0\08D\202\202\202\202|\0\0\0\0\0\0\0\0\0\270\304\202\202\202\202\374\200\200\200\0\0\0\0\0\0:F\202\202\202\202~\2\2\2\0\0\0\0\0\0^`@@@@@\0\0\0\0\0\0\0\0\0<@@<\2\2|\0\0\0\0\0\0   \374     \34\0\0\0\0\0\0\0\0\0\202\202\202\202\202\206z\0\0\0\0\0\0\0\0\0\202\202\202DD(\20\0\0\0\0\0\0\0\0\0\202\222\222\252\252DD\0\0\0\0\0\0\0\0\0D(\20(D\202\202\0\0\0\0\0\0\0\0\0\202\202\202\202\202F:\2\4x\0\0\0\0\0\0|\4\b\20 @\376\0\0\0\0\30     \300     \30\0\0\0\20\20\20\20\20\20\20\20\20\20\20\20\20\0\0\0000\b\b\b\b\b\6\b\b\b\b\b0\0\0\0\0\376\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\200\200@  \20\b\b\4\2\2\0\0\0\0\0`\222\f\0\0\0\0\0\0\0\0\0\0\0\0\20\20\20\20\20\20\0\20\20\20\20\20\20\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\20\20| p\2164D@>\0\0\0\0\0\0\0\0\0 x |\252\222d\0\0\0\0\0\0\0\0\0\210\204\204\202\202R \0\0\0\0\0\0\0\0\0p\0x\4\4\b0\0\0\0\0\0\0\0\0\08\0|\b\0200L\0\0\0\0\0\0\0\0\0 t\"|\242\242d\0\0\0\0\0\0\0\0\0L |\242$\20\b\0\0\0\0\0\0\0\0\0\20\274\322\222<\20 \0\0\0\0\0\0\0\0\0\20\20\36\20|\222`\0\0\0\0\0\0\0\0\0\0|\2\2\4\30\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0  x |\252\252\252\222d\0\0\0\0\0\0\0\200\210\204\204\202\202\202P \0\0\0\0\0\0`\30\08\304\2\2\4\b0\0\0\0\0\0\0000\f\0>\304\b\0200R\214\0\0\0\0\0\0  t\"\"|\242\242\242d\0\0\0\0\0\0($:\344$$DDD\230\0\0\0\0\0\0\20\34p\168\4~\200\200|\0\0\0\0\0\0\4\4\b\20 @ \20\b\4\0\0\0\0\0\0\b\b\204\204\276\204\204\204\204h\0\0\0\0\0\0\34b\2\4\0\0 @\200~\0\0\0\0\0\0\20\20\34p\b\4~\200\200|\0\0\0\0\0\0\200@@\200\200\200\200\202D8\0\0\0\0\0\0\b\b\376\bx\210\230h\b0\0\0\0\0\0\0DDD^\344DL@@>\0\0\0\0\0\0\34d\b\20&\370\20 @>\0\0\0\0\0\252\0\252\0\252\0\252\0\252\0\252\0\0\0\0\0\0\0\0\0\0\0\0`\220\220`\0\0\0\0\0\0<      \0\0\0\0\0\0\0\0\0\0\0\0\b\b\b\b\b\bx\0\0\0\0\0\0\0\0\0\0\0\0\200@ \20\0\0\0\0\0\0\0\0\0\208\20\0\0\0\0\0\0\0\0\0\0~\2\2~\2\2\4\b\20`\0\0\0\0\0\0\0\0\0~\2\22\34\20 @\0\0\0\0\0\0\0\0\0\2\f\30h\b\b\b\0\0\0\0\0\0\0\0\0\20~BB\2\4\30\0\0\0\0\0\0\0\0\0\0~\20\20\20\20\376\0\0\0\0\0\0\0\0\0\b\b~\30(H\30\0\0\0\0\0\0\0\0\0 .r\22\20\b\b\0\0\0\0\0\0\0\0\0<\4\4\b\b\b~\0\0\0\0\0\0\0\0\0<\4\4<\4\4<\0\0\0\0\0\0\0\0\0RRR\2\4\b0\0\0\0\0\0\0\0\0\0\0@>\0\0\0\0\0\0\0\0\0\0\376\2\2\22\24\30\20\20 \300\0\0\0\0\0\2\2\4\4\b\30(H\210\b\b\0\0\0\0\0\20\20\20\376\202\202\202\2\4\b0\0\0\0\0\0\0\0~\20\20\20\20\20\20\20\376\0\0\0\0\0\b\b\b~\b\30(H\210\b\30\0\0\0\0\0\20\20\20~\22\"\"\"BB\214\0\0\0\0\0   \34\360\20\36\360\b\b\b\0\0\0\0\0  >\"BB\202\4\4\b0\0\0\0\0\0\200\200\200\376\210\210\210\b\20\20 \0\0\0\0\0\0\0~\2\2\2\2\2\2\2\376\0\0\0\0\0DDD\376DDD\b\b\20 \0\0\0\0\0\0\340\20\2\342\22\4\4\b0\300\0\0\0\0\0\0~\2\2\4\4\b\24$B\202\0\0\0\0\0@@@^\342BD@@@>\0\0\0\0\0\0\202B\"\"\4\4\b\b\20 \0\0\0\0\0\0>\"\"R\214\4\n\20 @\0\0\0\0\0\0\4\30p\b\b\376\b\b\20 \0\0\0\0\0\0\"\22\222RBD\4\b\20 \0\0\0\0\0\0|\0\0\376\20\20\20\20 @\0\0\0\0\0\0@@@@pHD@@@\0\0\0\0\0\0\20\20\20\376\20\20\20\20 @\0\0\0\0\0\0\0\0|\0\0\0\0\0\0\376\0\0\0\0\0\0\376\2\2\4D(\20(D\200\0\0\0\0\0\0\20\20\376\2\4\b\0204\322\20\0\0\0\0\0\0\2\2\2\4\4\4\b\20 \300\0\0\0\0\0\0\20\20\bHDDD\202\202\202\0\0\0\0\0\0\200\200\216\360\200\200\200\200\200~\0\0\0\0\0\0\376\2\2\2\2\2\4\4\b0\0\0\0\0\0\0 PH\210\204\4\2\2\2\2\0\0\0\0\0\0\20\20\20\376\20\20T\222\222\20\0\0\0\0\0\0\376\2\2\4\4\210P \20\b\0\0\0\0\0\0\0p\f\2p\f\2\360\f\2\0\0\0\0\0\0\20\20\20  (DD\232\342\0\0\0\0\0\0\4\4\4\b\3100\30$B\200\0\0\0\0\0\0|\20\20\20\376\20\20\20\20\16\0\0\0\0\0\0@@.\362\"\24\20\20\b\b\0\0\0\0\0\0|\4\4\b\b\20\20\20\376\0\0\0\0\0\0\0\376\2\2\2\376\2\2\2\376\2\0\0\0\0\0\0\376\0\0\376\2\2\2\4\b0\0\0\0\0\0\0\202\202\202\202\202\2\4\4\b0\0\0\0\0\0\0PPPPPRR\224\224\230\0\0\0\0\0\0\200\200\200\200\200\202\202\204\230\340\0\0\0\0\0\0\376\202\202\202\202\202\202\202\376\202\0\0\0\0\0\0\376\202\202\202\2\2\4\4\b0\0\0\0\0\0\0\200@ \0\2\2\4\b0\300\0\0\0\0\0\0\220H$\0\0\0\0\0\0\0\0\0\0\0\0\0`\220\220`\0\0\0\0\0\0\0\0\0\0\0\0   \376 NB@\220\216\0\0\0\0\0\0  \374@X\344\202\2\2|\0\0\0\0\0\0\0\370\4\2\2\2\4\30`\0\0\0\0\0\0\0\376\4\b\20    \20\16\0\0\0\0\0\0@@@N0@\200\200\200~\0\0\0\0\0\0($\"\362H\2108LJ0\0\0\0\0\0\0\234\202\204\200\200\240\240\240\236\0\0\0\0\0\0\0\b\210\210|\322\262\226\252\252F\0\0\0\0\0\0  \3542\"bf\252\252&\0\0\0\0\0\0\08T\222\222\222\222\222d\0\0\0\0\0\0\0\204\204\276\204\204\204\234\246\244\230\0\0\0\0\0\0\350,JH\204\204\204\204\210p\0\0\0\0\0\08\b\20TT\212\212\212\2120\0\0\0\0\0\0\0\0\0\20(\304\2\0\0\0\0\0\0\0\0\0\276\204\204\276\204\204\234\246\244\230\0\0\0\0\0\0\20\20\376\20\376\20x\224\222`\0\0\0\0\0\0p\24\24>T\224\244\244D\b\0\0\0\0\0\0  \374 ,b\240\242b\34\0\0\0\0\0\0\4D\\*j\252\252\222\222d\0\0\0\0\0\0  \374 \374  \"\"\34\0\0\0\0\0\0\b\210H\\\342\"$\20\20\20\0\0\0\0\0\0\20\20\210\274\312\212\212\252\34 \0\0\0\0\0\0\20\20\20\36\20\20x\224\222`\0\0\0\0\0\0\3000\0\200\200\274\302\202\48\0\0\0\0\0\0\234\242\302\302\202\202\4\4\b0\0\0\0\0\0\0|\b\20 |\202:FB<\0\0\0\0\0\0  \3542\"b\244\250*$\0\0\0\0\0\0|\b\20 |\202\2\2\48\0\0\0\0\0\0   \3542bb\342\244(\0\0\0\0\0\0\20\20  @@b\222\222\214\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0".getBytes (XEiJ.ISO_8859_1);

  //perl misc/ftob.pl SCSI16IN misc/scsi16in.r
  public static final byte[] SCSI16IN = "\0\374\0\242\0\374\0\242\0\374\0\242\0\374\0\242\0\374\0\242\0\374\0\242\0\374\0\242\0\374\0\242\0\374\0JSCSIIN\0\20SCSIINROM 16 (2025-08-03)\0\0\351` p\200\"<\0\0\1\365C\372\4<NO\260\211f\b\b8\0\0\f\374f(p\365r\0NOJ9\0\355\0qg\32J\270\f\304f\24a\0\25\210p\200r+C\372\3\316NO#\300\0\0\f\356NuSCSI\0\374\2$\0\374\4\220Human68kp\216NO\2\200\0\377\377\377(\0\4\204\0\374\0\0\344\214a\222\t9\0\355\0qfn|\23a\0\3rR\200fdp\365r$NOJ\200g\\\260\274\377\377\377\377gJ\260\274\0\0\0\bgB\260\274\0\0\0\2fBp\365r,v\bC\370 \0NOJ\200f2C\370 \0\20\21\2\0\0p\f\0\0pf\"\20)\0\2g\260\260<\0\1g\252\260<\0\6g\244\260<\0\2g\236`\bQ\316\0\4`\2`\224Nua\0\2\374R\200f\366C\370 \0p\365r v\5NOJ\200f\346\b9\0\6\0\355\0pf\36J\21g\32\f\21\0\4g\24\f\21\0\5g\16\f\21\0\7g\b\f\21\0\204g\2`\276p\365r+NOJ\200f\264p\365r%NOJ\200f\252*)\0\4\340\215\342\215\272\274\0\0\0\4b\232f\2z\3t\0v\b\352\252\352\253C\370 \0r!p\365NOJ\200f\200\f\221X68Sf\0\377x\f\251CSI1\0\4f\0\377l\f)\0`\4\0f C\370 \0000<\0\377\"\351\4\0Q\310\377\3720<\0\377B\231Q\310\377\374t\4\352\252` t\bv\b\352\252\352\253C\370 \0r!p\365NOJ\200f\0\377,\f\21\0`f\0\377$a\0\23\256p\0C\370 \0N\221`\0\377\24\5\370\f\303RB`8H\347\337\376p\216NO\2\200\0\377\377\377\4\200\0\374\0\0e\f\344\210\36\0\f\200\0\0\0\be\26~\377\f\200\0\0\0\bg\fJ\2f\b$Ia\0\375\362\"J\264|\0\bg\0\1\206a\0\1\310R\200f\0\1|x\08\2\t9\0\355\0qf\242\t8\f\303f\234\209\0\355\0p\2\0\0\7\260\4g\216p\365r$NO\260\274\0\0\0\2g\16\260\274\0\0\0\bg\312J\200f\0\377tp\365r,v\bNOJ\200f\0\377fp\365r v$NOJ\200f\0\377X\b9\0\6\0\355\0pf J\21g\34\f\21\0\4g\26\f\21\0\5g\20\f\21\0\7g\n\f\21\0\204g\4`\0\377.p\365r%NOJ\200f\0\377\",\2*)\0\4\340\215\342\215\272\274\0\0\0\4b\0\377\16f\2z\3$It\0v\b\352\252\352\253\"Jr!p\365NO$\6J\200f\0\376\360\f\251X68S\0\0f\0\376\344\f\251CSI1\0\4f\0\376\330t\bv\b\352\252\352\253\"Jr!p\365NO$\6J\200f\0\376\300\f\221X68Kf\0\376\266HB\270\7b\4a|\324CHB,\2\"J0<\0\377\"\351\4\0Q\310\377\372t\20v0\352\252\352\253C\352\4\0r!p\365NO$\6J\200f\0\376\200\"J\f\251\1SCH\0\16f\0\376r\f\251DISK\0\22f\0\376f \t\321\251\0\6\321\251\0\n \21\260\274\377\377\377\377g\b\320\211\"\200\"@`\314HB\23B\0\26HB\5\270\f\303RBL\337\177\373Nut\377`\366H\347`@W\301t\16v\0C\351\0\20J\21g&\f\221Humaf\36\f\251n68k\0\4f\24\20)\0\b\b\0\0\0f\nRCJ\1g\4J\0g\4Q\312\377\320L\337\2\6Nup\377J8\f\302f\30/\4P\370\f\302\309\0\355\0p\2\4\0\7p\365r$NO(\37NuH\347\377\376\b\0\0\0f\03008\n\16\260|\0@e\f\260|\0Pe\b\260|\0\365g\2a\nL\337\177\377/8\f\356Nu2<\200\0a\0\22\362\322|\1\0\262|\220\0e\362NuH\347Pbp\20\262\200e\24\320\200\262\200e:\320\200\262\200e\16pP\262\200e\22`,E\372\0006`\22p \222\200E\372\0l`\bp@\222\200E\372\0\342\345\211,z\373|\"2\20\0\325\301N\222L\337F\nNup\377L\337F\nNup\377Nu\0\0\1\0\0\0\1\364\0\0\1\316\0\0\2\330\0\0\t\24\0\0\b\320\0\0\3\260\0\0\3\356\0\0\4,\0\0\4\230\0\0\4\250\0\0\3p\0\0\0034\0\0\4h\377\377\377\374\377\377\377\374\0\0\t\"\0\0\nR\0\0\n\264\0\0\f\\\0\0\6\376\0\0\7\264\0\0\13\32\0\0\13\210\0\0\f0\0\0\t\232\0\0\t\332\0\0\7\34\0\0\t^\0\0\r(\0\0\7:\0\0\f\302\0\0\f\364\0\0\n\32\0\0\f\220\377\377\377\274\377\377\377\274\377\377\377\274\0\0\r\\\0\0\r\236\0\0\r\326\0\0\16\16\377\377\377\274\377\377\377\274\377\377\377\274\377\377\377\274\377\377\377\274\377\377\377\274\377\377\377<\377\377\377<\377\377\377<\377\377\377<\377\377\377<\377\377\377<\377\377\377<\377\377\377<\377\377\377<\377\377\377<\377\377\377<\377\377\377<\377\377\377<\377\377\377<\377\377\377<\377\377\377<H\347@B,z\372Z\35|\0\220\0\3Q\370\f\302\209\0\355\0o\f\0\0Vg(\23\374\0001\0\350\340\r\23\374\0\7\0\355\0p\23\374\0\0\0\355\0q\23\374\0V\0\355\0o\23\374\0\0\0\350\340\r\209\0\355\0p\2\0\0\7\35@\0\1p\0\35@\0\5\35@\0\21\35@\0\31\35@\0\33\35@\0\35\35@\0\27\35|\0\0\0\0131\374\2\0\f\300rlC\372\0:p\200NO\35|\0\20\0\3p\2a\0\7\26\35|\0\20\0\5p\5a\0\7\n\35|\0\0\0\5 <\0\0\234@a\0\6\372\b\370\0\0\f\374L\337B\2NuH\347\300\2,z\371\242\20.\0\t\35@\0\tL\337@\3NsH\347\t\202,z\371\214A\356\0\t\35|\0\0\0\21\20.\0\r\2\0\0\370f\366\20\220\35|\0`\0\5`\34H\347\t\202,z\371fA\356\0\t\35|\0\0\0\21\20.\0\r\2\0\0\370f\366\2D\0\7p\1\351(\t9\0\355\0qf\f\200.\0\1\b\356\0\4\0\3`\6\b\256\0\4\0\3\35@\0\0270<\t\304\35@\0\33\340H\35@\0\31\35|\0\3\0\35\20\220\35|\0 \0\5p\1a\0\6N\20\20f\16\b.\0\5\0\rf\364J.\0\rj\246\20\20g\374\260<\0\4g\30\20\220\260<\0\20g\fH@\20.\0\13L\337A\220Nup\0`\366\35|\0\0\0\27 <\0\0\2X\35@\0\35\340\210\35@\0\33\340\210\35@\0\31\20\274\0\4p\2a\0\5\364\20\20g\374\20\220\260<\0\4g\b\260<\0\20g\304`\266\b.\0\5\0\rf\370\20\220J.\0\rk\262`\244H\347\20\2,z\370\202\20\21\2\0\0\340g\16\260<\0 g\f\260<\0\240g\n`\nv\6`\6v\n`\2v\f\b.\0\5\0\tf$\20.\0\13j\362\2\0\0\7\f\0\0\2f\16a\0\3\262H@f\6L\337@\bNu\20.\0\13`\364a\0\375\322p\377`\354HV,z\370(\20.\0\t\b\0\0\5f$\b\0\0\3f\30\20.\0\13j\352\2\0\0\7f\fa\0\1TH@f\4,_Nu\20.\0\13`\366a\0\375\226p\377`\356HV,z\367\354\20.\0\t\b\0\0\5f(\b\0\0\3f\34\20.\0\13j\352\2\0\0\7\f\0\0\1f\fa\0\1\334H@f\4,_Nu\20.\0\13`\366a\0\375Vp\377`\356H\347\20\2,z\367\252\b.\0\5\0\tf&\20.\0\13j\362\2\0\0\7\f\0\0\3f\20v\1a\0\3.H@f\6L\337@\bNu\20.\0\13`\364a\0\375\30p\377`\354H\347\20\2,z\367lv\1\b.\0\5\0\tf$\20.\0\13j\362\2\0\0\7\f\0\0\7f\16a\0\2\360H@f\6L\337@\bNu\20.\0\13`\364a\0\374\332p\377`\354H\347\20\2,z\367.v\1\b.\0\5\0\tf\"\20.\0\13j\362\2\0\0\7\f\0\0\6f\fa\\H@f\6L\337@\bNu\20.\0\13`\364a\0\374\236p\377`\354H\347\20\2,z\366\362\20)\0\2g\fU\0e\fg\16S\0g\n`\264v\5`\6v\3`\2v\2\22\274\0\1\23C\0\1T\203`\236HV,z\366\304p\0\20.\0\13,_Nup\20NuH\347xxE\356\0\tG\356\0\rI\356\0\25J\203f\0046<\1\0\35|\0\0\0\27 \3\35@\0\35\340\210\35@\0\33\340\210\35@\0\31\20.\0\13\2\0\0\7\35@\0\21J.\0\13j\372\24\222\35|\0\200\0\5\20\23\2\0\0\360\260<\0pg\6\260<\0\260f\3560\t\342Hd\bp\1\300;\n\373g6Q\203e0J\22f@\b\23\0\0g\366 \31\341\230\30\200\341\230\30\200\341\230\30\200\341\230\30\200 \31\341\230\30\200\341\230\30\200\341\230\30\200\341\230\30\200`\314P\203S\203e\16J\22f\n\b\23\0\1f\366\30\231`\356p\0\20\22g\374\24\200\260<\0\20f\2p\0L\337\36\36NuH\347pxE\356\0\tG\356\0\rI\356\0\25J\203f\0046<\1\0\20.\0\13\2\0\0\7\35@\0\21\35|\0\0\0\27 \3\35@\0\35\340\210\35@\0\33\340\210\35@\0\31\24\222\35|\0\200\0\5\20\23\2\0\0\360\260<\0pg\6\260<\0\260f\3560\t\342Hd\np\1\300;\n\373g\0\0\266J9\0\355\0pjpt\00048\f\300\226\202ed\20\22g\16\326\202\b\0\0\3f\0\0\226`\0\0\266\b\23\0\1g\3502\2\350ISA\20\24\341H\20\24H@\20\24\341H\20\24\"\300\20\24\341H\20\24H@\20\24\341H\20\24\"\300\20\24\341H\20\24H@\20\24\341H\20\24\"\300\20\24\341H\20\24H@\20\24\341H\20\24\"\300Q\311\377\276`\230\326\202Q\203e6\20\22g\nP\203\b\0\0\3f,`N\b\23\0\1g\354\20\24\341H\20\24H@\20\24\341H\20\24\"\300\20\24\341H\20\24H@\20\24\341H\20\24\"\300`\306P\203S\203e\36\20\22g\20\b\0\0\3g\6\b\23\0\0g\4R\203`\f\b\23\0\0f\346\22\324`\336R\203p\0\20\22g\374\24\200r\347\302\0f\6J\203f\2p\0L\337\36\16NuH\347ppE\356\0\13G\356\0\5r\354t\314\20\22\2\0\0\7\35@\0\21J\22j\374\35Y\0\27\26\201J\22k\374\26\202S\203f\342p\0L\337\16\16NuH\347ppE\356\0\13G\356\0\5r\354t\314\20\22\2\0\0\7\35@\0\21J\22j\374\26\201J\22k\374\22\356\0\27\26\202S\203f\342p\0L\337\16\16Nu\0\0\0\0\0\0NU\377\360H\347RpE\372\377\360a\0\1\32L\337\16JN]Nu\1\0\0\0\0\0NU\377\360H\347RpE\372\377\360a\0\0\374L\337\16JN]Nu\b\0\0\0\0\0NU\377\360H\347Rp&IC\355\377\360E\372\377\352r\5\22\332Q\311\377\374C\355\377\360,\2\314\274\0\377\377\377\215\221\23C\0\4a\0\0\216J\200f\0\0\204\341\213p\3\272\200d\2 \5\341\253\"Ka\0\1T\f\200\377\377\377\377gh\f\200\377\377\377\376g\fa\0\0\250L\337\16JN]Nua\0\0\234J\200f\360p\376`\354%\0\0\0\0\0\0\0\0\0NU\377\360H\347Rp&IC\355\377\360E\372\377\346r\t\22\332Q\311\377\374C\355\377\360a\"J\200f\32\"Kv\ba\0\373R\f\200\377\377\377\377g\naRL\337\16JN]Nup\377`\364H\347H\0002<\0\1a\0\371\264J\200W\311\377\370f\30HD\353\f\211)\0\1a\0\372\204J\200f\bp\0L\337\0\22Nup\377`\366C\355\377\360r\5\22\332Q\311\377\374C\355\377\360a\276J\200f$C\355\377\377a\0\373.J\200f\30C\355\377\376a\0\373`J\200f\f\20-\377\376H@\20-\377\377Nup\377NuH\347\340\200A\371\0\350\200#r\0\22\20\22\20t\0\24\20\264\20e\370\222Bd\4\322|\0\310\303B\220\202b\352L\337\1\7NuHV,z\362\214\20.\0\t\b\0\0\5f,\b\0\0\3f \20.\0\13j\352\2\0\0\7f\24a\0\5\234H@f\4,_NuJ@g\4H@`\364\20.\0\13`\356a\0\367\362p\377`\346HV,z\362H\20.\0\t\b\0\0\5f0\b\0\0\3f$\20.\0\13j\352\2\0\0\7\f\0\0\1f\24a\0\6zH@f\4,_NuJ@g\4H@`\364\20.\0\13`\356a\0\367\252p\377`\346\22\0\0\0\0\0NU\377\360H\347Rp&IC\355\377\360E\372\377\352r\5\22\332Q\311\377\374C\355\377\360\23C\0\4a\0\376\260J\200f\0\2\304\"Ka\0\371\336`\0\2\262\3\0\0\0\0\0NU\377\360H\347Rp&IC\355\377\360E\372\377\352r\5\22\332Q\311\377\374C\355\377\360\23C\0\4a\0\376tJ\200f\0\2\210\"Ka\0\371\242`\0\2v\32\0\0\0\0\0NU\377\360H\347Rp&IC\355\377\360E\372\377\352r\5\22\332Q\311\377\374C\355\377\360\23C\0\4\23B\0\2a\0\3764J\200f\0\2H\"Ka\0\371b`\0\0026\25\0\0\0\0\0NU\377\360H\347Rp&IC\355\377\360E\372\377\352r\5\22\332Q\311\377\374C\355\377\360\23C\0\4\23B\0\1a\0\375\364J\200f\0\2\b\"Ka\0\370\346`\0\1\366\7\0\0\0\0\0NU\377\360H\347Rp&IC\355\377\360E\372\377\352r\5\22\332Q\311\377\374C\355\377\360a\0\375\274J\200f\0\1\320\"Ka\0\370\256`\0\1\276\b\0\0\0\0\0NU\377\360H\347Rp&IC\355\377\360E\372\377\352r\5\22\332Q\311\377\374C\355\377\360,\2\314\274\0\377\377\377\215\221\23C\0\4a\0\375vJ\200f\0\1\212\341\213p\3\272\200d\2 \5\341\253\"KA\372\376<\b9\0\4\0\355\0pg\4A\372\370\212N\220`\0\1:\n\0\0\0\0\0NU\377\360H\347Rp&IC\355\377\360E\372\377\352r\5\22\332Q\311\377\374C\355\377\360,\2\314\274\0\377\377\377\215\221\23C\0\4a\0\375\24J\200f\0\1(\341\213p\3\272\200d\2 \5\341\253\"KA\372\375\226\b9\0\4\0\355\0pg\4A\372\367\354N\220`\0\0\330(\0\0\0\0\0\0\0\0\0NU\377\360H\347RpJCg\0\0\352&IC\355\377\360E\372\377\340r\t\22\332Q\311\377\374C\355\377\360,\3#B\0\2\23C\0\b\340\213\23C\0\7a\0\374\246J\200f\0\0\272&\6\341\213p\3\272\200d\2 \5\341\253\"KA\372\375j\b9\0\4\0\355\0pg\4A\372\367\270N\220`h*\0\0\0\0\0\0\0\0\0NU\377\360H\347RpJCg|&IC\355\377\360E\372\377\342r\t\22\332Q\311\377\374C\355\377\360,\3#B\0\2\23C\0\b\340\213\23C\0\7a\0\374:J\200fN&\6\341\213p\3\272\200d\2 \5\341\253\"KA\372\374\274\b9\0\4\0\355\0pg\4A\372\367\22N\220\f\200\377\377\377\377g\"\f\200\377\377\377\376g\36`\4J\200f\24a\0\374>L\337\16JN]Nu\f\200\377\377\377\377f\354p\377`\354a\0\374&J\200f\2p\376L\337\16JN]Nu/\0\0\0\0\0\0\0\0\0NU\377\360H\347RpJCg\324&IC\355\377\360E\372\377\342r\t\22\332Q\311\377\374C\355\377\360`\0\377V\4\0\0\0\0\0NU\377\360H\347RpC\355\377\360E\372\377\354r\5\22\332Q\311\377\374C\355\377\360\23C\0\4\340\213\23C\0\3a\0\373r`\0\377p\36\0\0\0\0\0NU\377\360H\347RpC\355\377\360E\372\377\354r\5\22\332Q\311\377\374C\355\377\360\2\3\0\1\23C\0\4a\0\373@`\0\377>\33\0\0\0\0\0NU\377\360H\347RpC\355\377\360E\372\377\354r\5\22\332Q\311\377\374C\355\377\360\2\3\0\3\23C\0\4a\0\373\16`\0\377\f\301\0\0\0\0\0NU\377\360H\347RpC\355\377\360E\372\377\354r\5\22\332Q\311\377\374C\355\377\360\2\3\0\1\23C\0\4v\6a\0\372\332`\0\376\330\13\0\0\0\0\0NU\377\360H\347RpC\355\377\360E\372\377\354r\5\22\332Q\311\377\374C\355\377\360,\2\314\274\0\377\377\377\215\221a\0\372\246`\0\376\244\302\0\0\0\0\0NU\377\360H\347Rp&IC\355\377\360E\372\377\352r\5\22\332Q\311\377\374C\355\377\360\"\3\23A\0\5v\6a\0\372rJ\200f\0\376\206&\1\"Ka\0\365b`\0\376r\6\0\0\0\0\0NU\377\360H\347RpC\355\377\360E\372\377\354r\5\22\332Q\311\377\374C\355\377\360,\2\314\274\0\377\377\377\215\221\23C\0\4a\0\372,`\0\376*\7\0\0\0\0\0NU\377\360H\347RpC\355\377\360E\372\377\354r\5\22\332Q\311\377\374C\355\377\360,\2\314\274\0\377\377\377\215\221\23C\0\4a\0\371\364`\0\375\362\16\0\0\0\0\0NU\377\360H\347Rp&IC\355\377\360E\372\377\352r\5\22\332Q\311\377\374C\355\377\360,\2\314\274\0\377\377\377\215\221\23C\0\4a\0\371\272J\200f\0\375\316v\4\"Ka\0\364\252`\0\375\272H\347p\320A\371\0\350@@G\356\0\ta\0\0026J\203f\0046<\1\0\20.\0\13\2\0\0\7\35@\0\21 \3\35@\0\35\340\210\35@\0\33\340\210\35@\0\31J.\0\13j\372\26\223\35|\0\200\0\0052<\2001\f9\0\334\0\350\340\13f\16\b9\0\5\0\355\0pg\4\nA\200\3P\320Bh\0\32\21|\0\4\0\0061A\0\4p\25\320\216!@\0\24$<\0\0\1\0J9\0\355\0pj\00649\0\0\f\300\264\203c\2$\3J\23f^\b.\0\0\0\rg\364p\0\20.\0\31H@\20.\0\33\341H\20.\0\35\220\203\"\t\222\200\320\202P\320!A\0\f1@\0\nJ\23f.J.\0\13j\366P\320\21|\0\200\0\7J\23J\23f\32J\20j\370\20(\0\1g\n\260<\0\ng\250p\377`(\323\302\226\202f\230p\0\20\23g\374\26\200\260<\0\20f\2p\0Jh\0\ng\2p\376J\20k\6\21|\0\20\0\7!|\0\351`\1\0\24L\337\13\16NuH\347\20@J\203f\0046<\1\0\20.\0\13\2\0\0\7\35@\0\21 \3\35@\0\35\340\210\35@\0\33\340\210\35@\0\31\35n\0\t\0\t\35|\0\200\0\5a\36J\200f\24\20.\0\tg\372\35n\0\t\0\t\260<\0\20f\2p\0L\337\2\bNuH\347`\2202<\200\261a\0\0\300A\371\0\350@@P\320Bh\0\32\21|\0\4\0\6G\356\0\25!K\0\24t\0004<\1\0J9\0\355\0pj\bt\00049\0\0\f\300\f9\0\334\0\350\340\13f\16\b9\0\5\0\355\0pg\4\nA\200\0031A\0\4G\356\0\t\266\202b\2$\3!I\0\f1B\0\nJ\23f@\b.\0\0\0\rf\364P\320\21|\0\200\0\7J\23J\23f*J\20j\370\20(\0\1f\26\323\302\226\202f\310p\0!|\0\351`\1\0\24L\337\t\6Nu\260<\0\ng\300p\377`\350\20.\0\t\f\0\0\20f\np\0Jh\0\ng\2p\376\21|\0\20\0\7`\314\f8\0\2\f\274e\34\f8\0\4\f\274d\22/\0Nz\0\2\200|\b\bN{\0\2 \37`\2\364\370NuH\347\370B2<\200\0t\17C\370\0\0a\0\3\22\322|\1\0Q\312\377\362L\337B\37NuH\347\370Bp\2002<\1@C\372\0\306NO!\300\f\364p\2002<\1AC\372\2dNO!\300\f\304p\2002<\1CC\372\2\326NO!\300\f\310p\2002<\1DC\372\0\334NO!\300\f\314p\2002<\1EC\372\1\262NO!\300\f\320p\2002<\1FC\372\1\224NO!\300\f\324p\2002<\1GC\372\0tNO!\300\f\330p\2002<\1HC\372\0rNO!\300\f\334p\2002<\1KC\372\0pNO!\300\f\340p\2002<\1MC\372\0nNO!\300\f\344p\2002<\1OC\372\0\306NO!\300\f\3502<\200\0t\17\"|\0\0\0\0a\0\2@\322|\1\0Q\312\377\360L\337B\37Nu/8\f\300H\347H\4K\372\373p`H/8\f\330H\347H\4K\372\365V`:/8\f\334H\347H\4K\372\374:`,/8\f\340H\347H\4K\372\373\364`\36/8\f\344H\347H\4K\372\373\256`\20/8\f\314H\347H\4K\372\365\0`\0\0\2x\08\1\2A\360\0\262|\200\0f6\340L\342Ld\4\b\304\0\20\2D\0\7\t9\0\355\0qg N\225\b\0\0\1f\np\0L\337 \22X\217Nu\0\200\377\377\377\0L\337 \22X\217NuL\337 \22Nu/8\f\350H\347\177Hx\08\1\2A\360\0\262|\200\0fb\"\4\340L\342Ld\4\b\304\0\20\2D\0\7\t9\0\355\0qgJI\370\t\3760\1\340X\300|\0\17\0204\0\0J\0k.g,$<\0\1V`\260<\0\24g\32$<\0\2\254\300\260<\0(g\16$<\0\0\257P\260<\0\ng\2`\6a\0\372np\0L\337\22\376X\217NuL\337\22\376Nu/8\f\324H\347~dK\372\367|`\20/8\f\320H\347~dK\372\367\320`\0\0\2x\08\1\2A\360\0\262|\200\0f^\340L\342Ld\4\b\304\0\20\2D\0\7\t9\0\355\0qgH,\3&\6\326\274\0\0\0\377\340\213\266\274\0\0\1\0c\6&<\0\0\1\0z\0N\225\b\0\0\1f\26\324\203\"\3\341\211\323\301\234\201b\322L\337&~X\217p\0NuL\337&~X\217\0\200\377\377\377\0NuL\337&~NuNT\377\0H\347~`x\08\1\2A\360\0\262|\200\0f`\"\4\340L\342Ld\4\b\304\0\20\2D\0\7\t9\0\355\0qgH$I,\3&\6\266\274\0\0\1\0e\6&<\0\0\1\0C\354\377\0a\0\377(*\3S\205\265\tf\24Q\315\377\372R\202\234\203b\326L\337\6~N\\p\0Nup\376L\337\6~N\\\0\200\377\377\377\0NuL\337\6~N\\/8\f\304NuNT\377\0H\347|dx\08\1\2A\360\0\262|\200\0f@\340L\342Ld\4\b\304\0\20\2D\0\7\t9\0\355\0qg* \tg2v\na\0\371J\b\0\0\1f\favp\0L\337&>N\\Nu\0\200\377\377\377\0L\337&>N\\NuL\337&>N\\/8\f\310Nuv\nC\372\0\264a\0\371\24\b\0\0\1f\326C\354\377\0t\4v\1z\0a\0\365\366\b\0\0\1f\302E\354\377\0\f\232X68Kf\266C\372\0T \22\260\274\0\0\237\331e\220C\351\0\24\260\274\0\1=\35e\204C\351\0\24`\0\377~K\371\0\0\t\376 \1\340X\300\274\0\0\0\17\333\300\20<\0(\f)\0\7\0\3g\20\20<\0\24\f)\0\2\0\4g\4\20<\0\n\32\200B\200Nu\1\1\0\3\0015\200\0\0\0\1\1\0\3\1T\200\0\0\0\1\1\0\3\2f\200\0\0\0\1\1\0\3\2\230\200\0\0\0\1\1\0\7\2f\200\0\0\0\1\1\0\7\2\230\200\0\0\0".getBytes (XEiJ.ISO_8859_1);

  //perl misc/ftob.pl SCSI16EX misc/scsi16ex.r
  public static final byte[] SCSI16EX = "\377\377\377\377\377\377\377\377\377\377\377\377\377\377\377\377\377\377\377\377\377\377\377\377\377\377\377\377\377\377\377\377\0\352\0\302\0\352\0\302\0\352\0\302\0\352\0\302\0\352\0\302\0\352\0\302\0\352\0\302\0\352\0\302\0\352\0jSCSIEX\0\20SCSIEXROM 16 (2025-08-03)\0\0\352\0\0p\200\"<\0\0\1\365C\372\4<NO\260\211f\b\b8\0\1\f\374f(p\365r\0NOJ9\0\355\0qg\32J\270\f\304f\24a\0\25Xp\200r+C\372\3\316NO#\300\0\0\f\356NuSCSI\0\352\2D\0\352\4\260Human68kp\216NO\2\200\0\377\377\377(\0\4\204\0\352\0 \344\214a\222\t9\0\355\0qfn|\23a\0\3rR\200fdp\365r$NOJ\200g\\\260\274\377\377\377\377gJ\260\274\0\0\0\bgB\260\274\0\0\0\2fBp\365r,v\bC\370 \0NOJ\200f2C\370 \0\20\21\2\0\0p\f\0\0pf\"\20)\0\2g\260\260<\0\1g\252\260<\0\6g\244\260<\0\2g\236`\bQ\316\0\4`\2`\224Nua\0\2\374R\200f\366C\370 \0p\365r v\5NOJ\200f\346\b9\0\6\0\355\0pf\36J\21g\32\f\21\0\4g\24\f\21\0\5g\16\f\21\0\7g\b\f\21\0\204g\2`\276p\365r+NOJ\200f\264p\365r%NOJ\200f\252*)\0\4\340\215\342\215\272\274\0\0\0\4b\232f\2z\3t\0v\b\352\252\352\253C\370 \0r!p\365NOJ\200f\200\f\221X68Sf\0\377x\f\251CSI1\0\4f\0\377l\f)\0`\4\0f C\370 \0000<\0\377\"\351\4\0Q\310\377\3720<\0\377B\231Q\310\377\374t\4\352\252` t\bv\b\352\252\352\253C\370 \0r!p\365NOJ\200f\0\377,\f\21\0`f\0\377$a\0\23~p\0C\370 \0N\221`\0\377\24\5\370\f\303RB`8H\347\337\376p\216NO\2\200\0\377\377\377\4\200\0\352\0 e\f\344\210\36\0\f\200\0\0\0\be\26~\377\f\200\0\0\0\bg\fJ\2f\b$Ia\0\375\362\"J\264|\0\bg\0\1\206a\0\1\310R\200f\0\1|x\08\2\t9\0\355\0qf\242\t8\f\303f\234\209\0\355\0p\2\0\0\7\260\4g\216p\365r$NO\260\274\0\0\0\2g\16\260\274\0\0\0\bg\312J\200f\0\377tp\365r,v\bNOJ\200f\0\377fp\365r v$NOJ\200f\0\377X\b9\0\6\0\355\0pf J\21g\34\f\21\0\4g\26\f\21\0\5g\20\f\21\0\7g\n\f\21\0\204g\4`\0\377.p\365r%NOJ\200f\0\377\",\2*)\0\4\340\215\342\215\272\274\0\0\0\4b\0\377\16f\2z\3$It\0v\b\352\252\352\253\"Jr!p\365NO$\6J\200f\0\376\360\f\251X68S\0\0f\0\376\344\f\251CSI1\0\4f\0\376\330t\bv\b\352\252\352\253\"Jr!p\365NO$\6J\200f\0\376\300\f\221X68Kf\0\376\266HB\270\7b\4a|\324CHB,\2\"J0<\0\377\"\351\4\0Q\310\377\372t\20v0\352\252\352\253C\352\4\0r!p\365NO$\6J\200f\0\376\200\"J\f\251\1SCH\0\16f\0\376r\f\251DISK\0\22f\0\376f \t\321\251\0\6\321\251\0\n \21\260\274\377\377\377\377g\b\320\211\"\200\"@`\314HB\23B\0\26HB\5\270\f\303RBL\337\177\373Nut\377`\366H\347`@W\301t\16v\0C\351\0\20J\21g&\f\221Humaf\36\f\251n68k\0\4f\24\20)\0\b\b\0\0\0f\nRCJ\1g\4J\0g\4Q\312\377\320L\337\2\6Nup\377J8\f\302f\30/\4P\370\f\302\309\0\355\0p\2\4\0\7p\365r$NO(\37NuH\347\377\376\b\0\0\0f\03008\n\16\260|\0@e\f\260|\0Pe\b\260|\0\365g\2a\nL\337\177\377/8\f\356Nu2<\200\0a\0\22\302\322|\1\0\262|\220\0e\362NuH\347Pbp\20\262\200e\24\320\200\262\200e:\320\200\262\200e\16pP\262\200e\22`,E\372\0006`\22p \222\200E\372\0l`\bp@\222\200E\372\0\342\345\211,z\373|\"2\20\0\325\301N\222L\337F\nNup\377L\337F\nNup\377Nu\0\0\1\0\0\0\1\364\0\0\1\316\0\0\2\330\0\0\t\24\0\0\b\320\0\0\3\260\0\0\3\356\0\0\4,\0\0\4\230\0\0\4\250\0\0\3p\0\0\0034\0\0\4h\377\377\377\374\377\377\377\374\0\0\t\"\0\0\nR\0\0\n\264\0\0\f\\\0\0\6\376\0\0\7\264\0\0\13\32\0\0\13\210\0\0\f0\0\0\t\232\0\0\t\332\0\0\7\34\0\0\t^\0\0\r(\0\0\7:\0\0\f\302\0\0\f\364\0\0\n\32\0\0\f\220\377\377\377\274\377\377\377\274\377\377\377\274\0\0\r\\\0\0\r\236\0\0\r\326\0\0\16\16\377\377\377\274\377\377\377\274\377\377\377\274\377\377\377\274\377\377\377\274\377\377\377\274\377\377\377<\377\377\377<\377\377\377<\377\377\377<\377\377\377<\377\377\377<\377\377\377<\377\377\377<\377\377\377<\377\377\377<\377\377\377<\377\377\377<\377\377\377<\377\377\377<\377\377\377<\377\377\377<H\347@B,z\372Z\35|\0\220\0\3Q\370\f\302\209\0\355\0o\f\0\0Vg(\23\374\0001\0\350\340\r\23\374\0\17\0\355\0p\23\374\0\0\0\355\0q\23\374\0V\0\355\0o\23\374\0\0\0\350\340\r\209\0\355\0p\2\0\0\7\35@\0\1p\0\35@\0\5\35@\0\21\35@\0\31\35@\0\33\35@\0\35\35@\0\27\35|\0\0\0\0131\374\2\0\f\300r\366C\372\0:p\200NO\35|\0\20\0\3p\2a\0\7\26\35|\0\20\0\5p\5a\0\7\n\35|\0\0\0\5 <\0\0\234@a\0\6\372\b\370\0\1\f\374L\337B\2NuH\347\300\2,z\371\242\20.\0\t\35@\0\tL\337@\3NsH\347\t\202,z\371\214A\356\0\t\35|\0\0\0\21\20.\0\r\2\0\0\370f\366\20\220\35|\0`\0\5`\34H\347\t\202,z\371fA\356\0\t\35|\0\0\0\21\20.\0\r\2\0\0\370f\366\2D\0\7p\1\351(\t9\0\355\0qf\f\200.\0\1\b\356\0\4\0\3`\6\b\256\0\4\0\3\35@\0\0270<\t\304\35@\0\33\340H\35@\0\31\35|\0\3\0\35\20\220\35|\0 \0\5p\1a\0\6N\20\20f\16\b.\0\5\0\rf\364J.\0\rj\246\20\20g\374\260<\0\4g\30\20\220\260<\0\20g\fH@\20.\0\13L\337A\220Nup\0`\366\35|\0\0\0\27 <\0\0\2X\35@\0\35\340\210\35@\0\33\340\210\35@\0\31\20\274\0\4p\2a\0\5\364\20\20g\374\20\220\260<\0\4g\b\260<\0\20g\304`\266\b.\0\5\0\rf\370\20\220J.\0\rk\262`\244H\347\20\2,z\370\202\20\21\2\0\0\340g\16\260<\0 g\f\260<\0\240g\n`\nv\6`\6v\n`\2v\f\b.\0\5\0\tf$\20.\0\13j\362\2\0\0\7\f\0\0\2f\16a\0\3\262H@f\6L\337@\bNu\20.\0\13`\364a\0\375\322p\377`\354HV,z\370(\20.\0\t\b\0\0\5f$\b\0\0\3f\30\20.\0\13j\352\2\0\0\7f\fa\0\1TH@f\4,_Nu\20.\0\13`\366a\0\375\226p\377`\356HV,z\367\354\20.\0\t\b\0\0\5f(\b\0\0\3f\34\20.\0\13j\352\2\0\0\7\f\0\0\1f\fa\0\1\334H@f\4,_Nu\20.\0\13`\366a\0\375Vp\377`\356H\347\20\2,z\367\252\b.\0\5\0\tf&\20.\0\13j\362\2\0\0\7\f\0\0\3f\20v\1a\0\3.H@f\6L\337@\bNu\20.\0\13`\364a\0\375\30p\377`\354H\347\20\2,z\367lv\1\b.\0\5\0\tf$\20.\0\13j\362\2\0\0\7\f\0\0\7f\16a\0\2\360H@f\6L\337@\bNu\20.\0\13`\364a\0\374\332p\377`\354H\347\20\2,z\367.v\1\b.\0\5\0\tf\"\20.\0\13j\362\2\0\0\7\f\0\0\6f\fa\\H@f\6L\337@\bNu\20.\0\13`\364a\0\374\236p\377`\354H\347\20\2,z\366\362\20)\0\2g\fU\0e\fg\16S\0g\n`\264v\5`\6v\3`\2v\2\22\274\0\1\23C\0\1T\203`\236HV,z\366\304p\0\20.\0\13,_Nup\20NuH\347xxE\356\0\tG\356\0\rI\356\0\25J\203f\0046<\1\0\35|\0\0\0\27 \3\35@\0\35\340\210\35@\0\33\340\210\35@\0\31\20.\0\13\2\0\0\7\35@\0\21J.\0\13j\372\24\222\35|\0\200\0\5\20\23\2\0\0\360\260<\0pg\6\260<\0\260f\3560\t\342Hd\bp\1\300;\n\373g6Q\203e0J\22f@\b\23\0\0g\366 \31\341\230\30\200\341\230\30\200\341\230\30\200\341\230\30\200 \31\341\230\30\200\341\230\30\200\341\230\30\200\341\230\30\200`\314P\203S\203e\16J\22f\n\b\23\0\1f\366\30\231`\356p\0\20\22g\374\24\200\260<\0\20f\2p\0L\337\36\36NuH\347pxE\356\0\tG\356\0\rI\356\0\25J\203f\0046<\1\0\20.\0\13\2\0\0\7\35@\0\21\35|\0\0\0\27 \3\35@\0\35\340\210\35@\0\33\340\210\35@\0\31\24\222\35|\0\200\0\5\20\23\2\0\0\360\260<\0pg\6\260<\0\260f\3560\t\342Hd\np\1\300;\n\373g\0\0\266J9\0\355\0pjpt\00048\f\300\226\202ed\20\22g\16\326\202\b\0\0\3f\0\0\226`\0\0\266\b\23\0\1g\3502\2\350ISA\20\24\341H\20\24H@\20\24\341H\20\24\"\300\20\24\341H\20\24H@\20\24\341H\20\24\"\300\20\24\341H\20\24H@\20\24\341H\20\24\"\300\20\24\341H\20\24H@\20\24\341H\20\24\"\300Q\311\377\276`\230\326\202Q\203e6\20\22g\nP\203\b\0\0\3f,`N\b\23\0\1g\354\20\24\341H\20\24H@\20\24\341H\20\24\"\300\20\24\341H\20\24H@\20\24\341H\20\24\"\300`\306P\203S\203e\36\20\22g\20\b\0\0\3g\6\b\23\0\0g\4R\203`\f\b\23\0\0f\346\22\324`\336R\203p\0\20\22g\374\24\200r\347\302\0f\6J\203f\2p\0L\337\36\16NuH\347ppE\356\0\13G\356\0\5r\354t\314\20\22\2\0\0\7\35@\0\21J\22j\374\35Y\0\27\26\201J\22k\374\26\202S\203f\342p\0L\337\16\16NuH\347ppE\356\0\13G\356\0\5r\354t\314\20\22\2\0\0\7\35@\0\21J\22j\374\26\201J\22k\374\22\356\0\27\26\202S\203f\342p\0L\337\16\16Nu\0\0\0\0\0\0NU\377\360H\347RpE\372\377\360a\0\1\32L\337\16JN]Nu\1\0\0\0\0\0NU\377\360H\347RpE\372\377\360a\0\0\374L\337\16JN]Nu\b\0\0\0\0\0NU\377\360H\347Rp&IC\355\377\360E\372\377\352r\5\22\332Q\311\377\374C\355\377\360,\2\314\274\0\377\377\377\215\221\23C\0\4a\0\0\216J\200f\0\0\204\341\213p\3\272\200d\2 \5\341\253\"Ka\0\1T\f\200\377\377\377\377gh\f\200\377\377\377\376g\fa\0\0\250L\337\16JN]Nua\0\0\234J\200f\360p\376`\354%\0\0\0\0\0\0\0\0\0NU\377\360H\347Rp&IC\355\377\360E\372\377\346r\t\22\332Q\311\377\374C\355\377\360a\"J\200f\32\"Kv\ba\0\373R\f\200\377\377\377\377g\naRL\337\16JN]Nup\377`\364H\347H\0002<\0\1a\0\371\264J\200W\311\377\370f\30HD\353\f\211)\0\1a\0\372\204J\200f\bp\0L\337\0\22Nup\377`\366C\355\377\360r\5\22\332Q\311\377\374C\355\377\360a\276J\200f$C\355\377\377a\0\373.J\200f\30C\355\377\376a\0\373`J\200f\f\20-\377\376H@\20-\377\377Nup\377NuH\347\340\200A\371\0\350\200#r\0\22\20\22\20t\0\24\20\264\20e\370\222Bd\4\322|\0\310\303B\220\202b\352L\337\1\7NuHV,z\362\214\20.\0\t\b\0\0\5f,\b\0\0\3f \20.\0\13j\352\2\0\0\7f\24a\0\5\234H@f\4,_NuJ@g\4H@`\364\20.\0\13`\356a\0\367\362p\377`\346HV,z\362H\20.\0\t\b\0\0\5f0\b\0\0\3f$\20.\0\13j\352\2\0\0\7\f\0\0\1f\24a\0\6bH@f\4,_NuJ@g\4H@`\364\20.\0\13`\356a\0\367\252p\377`\346\22\0\0\0\0\0NU\377\360H\347Rp&IC\355\377\360E\372\377\352r\5\22\332Q\311\377\374C\355\377\360\23C\0\4a\0\376\260J\200f\0\2\304\"Ka\0\371\336`\0\2\262\3\0\0\0\0\0NU\377\360H\347Rp&IC\355\377\360E\372\377\352r\5\22\332Q\311\377\374C\355\377\360\23C\0\4a\0\376tJ\200f\0\2\210\"Ka\0\371\242`\0\2v\32\0\0\0\0\0NU\377\360H\347Rp&IC\355\377\360E\372\377\352r\5\22\332Q\311\377\374C\355\377\360\23C\0\4\23B\0\2a\0\3764J\200f\0\2H\"Ka\0\371b`\0\0026\25\0\0\0\0\0NU\377\360H\347Rp&IC\355\377\360E\372\377\352r\5\22\332Q\311\377\374C\355\377\360\23C\0\4\23B\0\1a\0\375\364J\200f\0\2\b\"Ka\0\370\346`\0\1\366\7\0\0\0\0\0NU\377\360H\347Rp&IC\355\377\360E\372\377\352r\5\22\332Q\311\377\374C\355\377\360a\0\375\274J\200f\0\1\320\"Ka\0\370\256`\0\1\276\b\0\0\0\0\0NU\377\360H\347Rp&IC\355\377\360E\372\377\352r\5\22\332Q\311\377\374C\355\377\360,\2\314\274\0\377\377\377\215\221\23C\0\4a\0\375vJ\200f\0\1\212\341\213p\3\272\200d\2 \5\341\253\"KA\372\376<\b9\0\4\0\355\0pg\4A\372\370\212N\220`\0\1:\n\0\0\0\0\0NU\377\360H\347Rp&IC\355\377\360E\372\377\352r\5\22\332Q\311\377\374C\355\377\360,\2\314\274\0\377\377\377\215\221\23C\0\4a\0\375\24J\200f\0\1(\341\213p\3\272\200d\2 \5\341\253\"KA\372\375\226\b9\0\4\0\355\0pg\4A\372\367\354N\220`\0\0\330(\0\0\0\0\0\0\0\0\0NU\377\360H\347RpJCg\0\0\352&IC\355\377\360E\372\377\340r\t\22\332Q\311\377\374C\355\377\360,\3#B\0\2\23C\0\b\340\213\23C\0\7a\0\374\246J\200f\0\0\272&\6\341\213p\3\272\200d\2 \5\341\253\"KA\372\375j\b9\0\4\0\355\0pg\4A\372\367\270N\220`h*\0\0\0\0\0\0\0\0\0NU\377\360H\347RpJCg|&IC\355\377\360E\372\377\342r\t\22\332Q\311\377\374C\355\377\360,\3#B\0\2\23C\0\b\340\213\23C\0\7a\0\374:J\200fN&\6\341\213p\3\272\200d\2 \5\341\253\"KA\372\374\274\b9\0\4\0\355\0pg\4A\372\367\22N\220\f\200\377\377\377\377g\"\f\200\377\377\377\376g\36`\4J\200f\24a\0\374>L\337\16JN]Nu\f\200\377\377\377\377f\354p\377`\354a\0\374&J\200f\2p\376L\337\16JN]Nu/\0\0\0\0\0\0\0\0\0NU\377\360H\347RpJCg\324&IC\355\377\360E\372\377\342r\t\22\332Q\311\377\374C\355\377\360`\0\377V\4\0\0\0\0\0NU\377\360H\347RpC\355\377\360E\372\377\354r\5\22\332Q\311\377\374C\355\377\360\23C\0\4\340\213\23C\0\3a\0\373r`\0\377p\36\0\0\0\0\0NU\377\360H\347RpC\355\377\360E\372\377\354r\5\22\332Q\311\377\374C\355\377\360\2\3\0\1\23C\0\4a\0\373@`\0\377>\33\0\0\0\0\0NU\377\360H\347RpC\355\377\360E\372\377\354r\5\22\332Q\311\377\374C\355\377\360\2\3\0\3\23C\0\4a\0\373\16`\0\377\f\301\0\0\0\0\0NU\377\360H\347RpC\355\377\360E\372\377\354r\5\22\332Q\311\377\374C\355\377\360\2\3\0\1\23C\0\4v\6a\0\372\332`\0\376\330\13\0\0\0\0\0NU\377\360H\347RpC\355\377\360E\372\377\354r\5\22\332Q\311\377\374C\355\377\360,\2\314\274\0\377\377\377\215\221a\0\372\246`\0\376\244\302\0\0\0\0\0NU\377\360H\347Rp&IC\355\377\360E\372\377\352r\5\22\332Q\311\377\374C\355\377\360\"\3\23A\0\5v\6a\0\372rJ\200f\0\376\206&\1\"Ka\0\365b`\0\376r\6\0\0\0\0\0NU\377\360H\347RpC\355\377\360E\372\377\354r\5\22\332Q\311\377\374C\355\377\360,\2\314\274\0\377\377\377\215\221\23C\0\4a\0\372,`\0\376*\7\0\0\0\0\0NU\377\360H\347RpC\355\377\360E\372\377\354r\5\22\332Q\311\377\374C\355\377\360,\2\314\274\0\377\377\377\215\221\23C\0\4a\0\371\364`\0\375\362\16\0\0\0\0\0NU\377\360H\347Rp&IC\355\377\360E\372\377\352r\5\22\332Q\311\377\374C\355\377\360,\2\314\274\0\377\377\377\215\221\23C\0\4a\0\371\272J\200f\0\375\316v\4\"Ka\0\364\252`\0\375\272H\347p\320A\371\0\350@@G\356\0\ta\0\2\6J\203f\0046<\1\0\20.\0\13\2\0\0\7\35@\0\21 \3\35@\0\35\340\210\35@\0\33\340\210\35@\0\31J.\0\13j\372\26\223\35|\0\200\0\0052<\2001P\320Bh\0\32\21|\0\4\0\0061A\0\4p\25\320\216!@\0\24$<\0\0\1\0J9\0\355\0pj\00649\0\0\f\300\264\203c\2$\3J\23f^\b.\0\0\0\rg\364p\0\20.\0\31H@\20.\0\33\341H\20.\0\35\220\203\"\t\222\200\320\202P\320!A\0\f1@\0\nJ\23f.J.\0\13j\366P\320\21|\0\200\0\7J\23J\23f\32J\20j\370\20(\0\1g\n\260<\0\ng\250p\377`(\323\302\226\202f\230p\0\20\23g\374\26\200\260<\0\20f\2p\0Jh\0\ng\2p\376J\20k\6\21|\0\20\0\7!|\0\351`\1\0\24L\337\13\16NuH\347\20@J\203f\0046<\1\0\20.\0\13\2\0\0\7\35@\0\21 \3\35@\0\35\340\210\35@\0\33\340\210\35@\0\31\35n\0\t\0\t\35|\0\200\0\5a\36J\200f\24\20.\0\tg\372\35n\0\t\0\t\260<\0\20f\2p\0L\337\2\bNuH\347`\2202<\200\261a\0\0\250A\371\0\350@@P\320Bh\0\32\21|\0\4\0\6G\356\0\25!K\0\24t\0004<\1\0J9\0\355\0pj\bt\00049\0\0\f\3001A\0\4G\356\0\t\266\202b\2$\3!I\0\f1B\0\nJ\23f@\b.\0\0\0\rf\364P\320\21|\0\200\0\7J\23J\23f*J\20j\370\20(\0\1f\26\323\302\226\202f\310p\0!|\0\351`\1\0\24L\337\t\6Nu\260<\0\ng\300p\377`\350\20.\0\t\f\0\0\20f\np\0Jh\0\ng\2p\376\21|\0\20\0\7`\314\f8\0\2\f\274e\34\f8\0\4\f\274d\22/\0Nz\0\2\200|\b\bN{\0\2 \37`\2\364\370NuH\347\370B2<\200\0t\17C\370\0\0a\0\3\22\322|\1\0Q\312\377\362L\337B\37NuH\347\370Bp\2002<\1@C\372\0\306NO!\300\f\364p\2002<\1AC\372\2dNO!\300\f\304p\2002<\1CC\372\2\326NO!\300\f\310p\2002<\1DC\372\0\334NO!\300\f\314p\2002<\1EC\372\1\262NO!\300\f\320p\2002<\1FC\372\1\224NO!\300\f\324p\2002<\1GC\372\0tNO!\300\f\330p\2002<\1HC\372\0rNO!\300\f\334p\2002<\1KC\372\0pNO!\300\f\340p\2002<\1MC\372\0nNO!\300\f\344p\2002<\1OC\372\0\306NO!\300\f\3502<\200\0t\17\"|\0\0\0\0a\0\2@\322|\1\0Q\312\377\360L\337B\37Nu/8\f\300H\347H\4K\372\373\240`H/8\f\330H\347H\4K\372\365\206`:/8\f\334H\347H\4K\372\374j`,/8\f\340H\347H\4K\372\374$`\36/8\f\344H\347H\4K\372\373\336`\20/8\f\314H\347H\4K\372\3650`\0\0\2x\08\1\2A\360\0\262|\200\0f6\340L\342Ld\4\b\304\0\20\2D\0\7\t9\0\355\0qg N\225\b\0\0\1f\np\0L\337 \22X\217Nu\0\200\377\377\377\0L\337 \22X\217NuL\337 \22Nu/8\f\350H\347\177Hx\08\1\2A\360\0\262|\200\0fb\"\4\340L\342Ld\4\b\304\0\20\2D\0\7\t9\0\355\0qgJI\370\t\3760\1\340X\300|\0\17\0204\0\0J\0k.g,$<\0\1V`\260<\0\24g\32$<\0\2\254\300\260<\0(g\16$<\0\0\257P\260<\0\ng\2`\6a\0\372\236p\0L\337\22\376X\217NuL\337\22\376Nu/8\f\324H\347~dK\372\367\254`\20/8\f\320H\347~dK\372\370\0`\0\0\2x\08\1\2A\360\0\262|\200\0f^\340L\342Ld\4\b\304\0\20\2D\0\7\t9\0\355\0qgH,\3&\6\326\274\0\0\0\377\340\213\266\274\0\0\1\0c\6&<\0\0\1\0z\0N\225\b\0\0\1f\26\324\203\"\3\341\211\323\301\234\201b\322L\337&~X\217p\0NuL\337&~X\217\0\200\377\377\377\0NuL\337&~NuNT\377\0H\347~`x\08\1\2A\360\0\262|\200\0f`\"\4\340L\342Ld\4\b\304\0\20\2D\0\7\t9\0\355\0qgH$I,\3&\6\266\274\0\0\1\0e\6&<\0\0\1\0C\354\377\0a\0\377(*\3S\205\265\tf\24Q\315\377\372R\202\234\203b\326L\337\6~N\\p\0Nup\376L\337\6~N\\\0\200\377\377\377\0NuL\337\6~N\\/8\f\304NuNT\377\0H\347|dx\08\1\2A\360\0\262|\200\0f@\340L\342Ld\4\b\304\0\20\2D\0\7\t9\0\355\0qg* \tg2v\na\0\371z\b\0\0\1f\favp\0L\337&>N\\Nu\0\200\377\377\377\0L\337&>N\\NuL\337&>N\\/8\f\310Nuv\nC\372\0\264a\0\371D\b\0\0\1f\326C\354\377\0t\4v\1z\0a\0\366&\b\0\0\1f\302E\354\377\0\f\232X68Kf\266C\372\0T \22\260\274\0\0\237\331e\220C\351\0\24\260\274\0\1=\35e\204C\351\0\24`\0\377~K\371\0\0\t\376 \1\340X\300\274\0\0\0\17\333\300\20<\0(\f)\0\7\0\3g\20\20<\0\24\f)\0\2\0\4g\4\20<\0\n\32\200B\200Nu\1\1\0\3\0015\200\0\0\0\1\1\0\3\1T\200\0\0\0\1\1\0\3\2f\200\0\0\0\1\1\0\3\2\230\200\0\0\0\1\1\0\7\2f\200\0\0\0\1\1\0\7\2\230\200\0\0\0".getBytes (XEiJ.ISO_8859_1);

  //perl misc/pattobytes.pl ROM16PAT misc/rom16pat.x
  public static final byte[] ROM16PAT_TEXT = "XEiJ IPLROM 1.6 (2025-08-03)\r\nCAUTION: Distribution is prohibited.\r\n\32\0\7\0\"OA\372\0016!\310\0\20A\372\1(~\0p\0N{\b\1A\372\1\"!\310\0\20!\310\0,~\1p\1\300;\2\373g\0\1\bA\372\0004\6\372\0\0\0\32~\2p\bN{\0\2A\372\0\n\362\200\0\0\b\307\0\17`\0\0\346\0\0\0\0\0\376\240\250\0\0\0\0\0\0\0\0\360\0\6\317.IA\372\0@Nz\b\2~\3 <\0\0\b\bN{\0\2/<\0\3304@\360\27@\0.\274\0\0\0\0\360\27\b\0\360\27\f\0X\217A\372\0\n\362\200\0\0\b\307\0\17a\0\0\234`\0\0\216A\372\0<Nz\b\5~\4p\0N{\0\2\364\330p\0N{\0\3\365\30p\0N{\0\4N{\0\5N{\0\6N{\0\7A\372\0\n\362\200\0\0\b\307\0\17a\0\0\316`NA\372\0JNz\30\b~\6 <\0\200\0\0N{\0\2\364\330 <\0\0\2\20N{\0\3\365\30p\0N{\0\4N{\0\5N{\0\6N{\0\7p\1N{\b\bA\372\0\n\362\200\0\0\b\307\0\17a\0\0\202`\2~\0N\371\0\377\0t.IN\320O\357\377\200 O \374\177\377\0\2 \17\350\210T\200\351\210 \200A\370\0\1\"@p\7\"\310\321\374\0 \0\0Q\310\377\366#|\0\360\0\1\377\344\360\27L\0/<\200\3304@\360\0$\0\360\27@\0C\371\0\360\0\0A\371\0 \0\0000<\7\377\261\211V\310\377\374f\4\b\307\0\37\b\227\0\7\360\27@\0\360\0$\0O\357\0\204NuC\370 \0A\351\2\2p\177\"\310Q\310\377\374A\351\2\2p\177\"\310Q\310\377\374p\37\"\374\0\377\4AQ\310\377\370\"|\0\177\300@N{\220\4N{\220\6N{\220\5N{\220\7C\370 \0N{\230\7N{\230\6\"|\0\0\300\0\365\30\364\330N{\220\3A\371\200\377\0\0C\371\200\360\0\0000<\7\377\263\210V\310\377\374f\4\b\307\0\37\276<\0\6d\b\"|\0\0\0\0`\6\"|\0\0\2\0N{\220\3\365\30\364\330\223\311N{\220\4N{\220\6N{\220\5N{\220\7Nu\f\7\0\6f\32\221\310\b\20\0\4g\2B\20X\210\260\374\b\0e\360C\372\0\26!\311\0\364C\372\1\2!\311\0`$<\0\377\0\0NuHo\0\bH\347\377\376t\5\b/\0\5\0@f\bt\1Nh/H\0< o\0B2\0300\1\300|\3618\260|\1\bg\22L\327\0\7 o\0 O\357\0@N\371\0\377\7pp\7\300A\"w\4 \322\330/H\0B0\1\340HA\367\2\376\322\1eDNz\20\0N{ \0k\22\16\21\0\0\341H\16)\0\0\0\0021@\0\2`\36\16\21\0\0\341\210\16)\0\0\0\2\341\210\16)\0\0\0\4\341\210\16)\0\0\0\6 \200N{\20\0L\327\0\377`FNz\20\1N{ \1k\0240(\0\2\341X\16\21\b\0\341X\16)\b\0\0\2`  \20\341\230\16\21\b\0\341\230\16)\b\0\0\2\341\230\16)\b\0\0\4\341\230\16)\b\0\0\6N{\20\1L\327\0\7L\357\3\0\0 O\357\0@J\27j\4\0|\200\0Nsa\6aXalNuH\3470$\225\312&<U\252\252U@\347\0|\7\0/8\0\b*O!\374\0\376\244\f\0\b$\22Nq$\203Nq\266\222Nqf\24Nq$\202Nq\325\374\0\20\0\0\265\374\0\300\0\0e\336.M!\337\0\bF\337!\312\f\370L\337$\fNu\f9\0\377\0\351`-g\6J9\0\351`-V\370\f\355NuH\347\200\360E\371\0\355\0\0G\371\0\377\t\350 J\"K\263\210f\4\263\210gP\23\374\0001\0\350\340\r Jp?B\230Q\310\377\374 J\"KpZ\20\331Q\310\377\374%x\f\370\0\bJ8\f\355f\20%|\0\350\4\0\0\f\25|\0\1\0Z`\f%|\0\374\0\0\0\fB*\0ZBj\1\0B9\0\350\340\rL\337\17\1Nu\b8\0\7\b\7f\20a\0\377\32a\0\377ja\ba\0\0\234p\0NuH\347\300\200A\371\0\374\0$BAa\0\2\262\260\274SCSIf\30a\0\2r\260|INf\16RAa\0\2f\260|\0\20f\2RAJ8\f\355f\fJAg\6C\372\0\36`\24`\f\262|\0\2g\6C\372\0.`\6L\337\1\3Nua\0#v`\376\r\n  SCSIINROM is not required\0\r\n  SCSIINROM 16 is required\0\0H\347\300\300 9\0\355\0\b\260\270\f\370gvO\357\377\200 OC\372\0ta\0\1.r\24\342\250a\0\1@C\372\0\211a\0\1\36 8\f\370\342\250a\0\1.C\372\0~a\0\1\f\"Oa\0\"\362a\0\0\352a\0\0\334a\0\1\2\260<\0Yg\6\260<\0Nf\354\260<\0Yf\26\23\374\0001\0\350\340\r#\370\f\370\0\355\0\bB9\0\350\340\rC\372\0Fa\0\"\266O\357\0\200L\337\3\3Nu\r\n  Modify memory size setting from \0MB to \0MB? (y/n)\0\32\0\20\374\0\r\20\374\0\nB\20NuH\347`\0r\3\351Xt\17\304@\20\373 \16Q\311\377\364B\20L\337\0\6Nu0123456789ABCDEFH\347`\0r\7\351\230t\17\304@\20\373 \16Q\311\377\364B\20L\337\0\6Nu0123456789ABCDEFp\0NOJ\0g\370Nu/\0p\1NOJ\200g\np\0NOp\1NO`\362 \37Nu\20\331f\374S\210Nu\260<\0ae\n\260<\0zb\4\320<\377\340NuH\347\340@J\200f\6\20\374\0000`\32C\372\0 \"\31\260\201e\372t/R\2\220\201d\372\20\302\320\201\"\31f\360B\20L\337\2\7Nu;\232\312\0\5\365\341\0\0\230\226\200\0\17B@\0\1\206\240\0\0\'\20\0\0\3\350\0\0\0d\0\0\0\n\0\0\0\1\0\0\0\0H\347\340@t\n\224\201d\b\20\374\0000R\202f\370C\372\377\300\"\31\260\201T\312\377\372t/R\2\220\201d\372\20\302\320\201\"\31f\360B\20L\337\2\7NuH\347@@@\347\0|\7\0/8\0\bC\372\0\24!\311\0\b\"Op\0r\1Nq0\30Nqr\0.I!\337\0\bF\337D\301L\337\2\2NuH\347@@@\347\0|\7\0/8\0\bC\372\0\24!\311\0\b\"Op\0r\1Nq \30Nqr\0.I!\337\0\bF\337D\301L\337\2\2Nup\0\209\0\350\340\13F\0\351H\350\b1\300\f\266\f8\0\2\f\274e0 <\0\0!\1\f8\0\4\f\274e\30c\22Nz\b\b\b\300\0\0N{\b\b <\0\200\0\0000<\200\0N{\0\2B9\0\350\340\tA\372\1 aF!\300\r \260\274\0\0\377\377c\2p\3771\300\f\270O\357\377\342 \17\320|\0\16\300|\377\360 @L\372\2\7\0\366H\320\2\7a\30O\357\0\36!\300\r$\260\274\0\0\377\377c\2p\3771\300\f\272NuC\371\0\350\200\35t\17SB <\0@\0\0\344\250S\200@\347\0|\7\0\37)\377\354\37)\377\370\37\21\2)\0\317\377\370\2)\0\317\377\354\22\274\0\0J\21f\374\23|\0\0\0\6\23|\0\0\0\b\22\274\0qN\220\22\274\0\0J\21f\374p\0r\0\220)\0\6\222)\0\b\23|\0\310\0\6\23|\0\0\0\b\22\237\23_\377\370\23_\377\354F\337\300\374\0002\260\1c\4\320|\1\0\20\1JBg\b\260|\23\210e\0\377zJ@g4C\372\0Pr\0\228\f\274\262|\0\6b\4\345I\322\301\"\21\344\251\202\300i\0040\1`\24$\1BBHB\204\300H@0\2H@4\1\204\3000\2Nu H H H H H HH@H@Q\310\377\376H@Q\310\377\366Nu\372\0\0\0\372\0\0\0\372\0\0\0\372\0\0\0\246\252\252\253\0\0\0\0)\252\252\253r\0\b8\0\6\b\4g\0062<L\20`&J8\b\5j\0062<C\20`\32\b8\0\5\b\2g\0062<C\3`\f\b8\0\5\b\5g\0042<L\23\b\270\0\0\r\36JAg\36\b\370\0\0\r\36\23\374\0001\0\350\340\r\23\301\0\355\0\35B9\0\350\340\rp\20NONuA\371\0\355\0X\20\20r\1\b8\0\0\b\4f\nD\1\b8\0\3\b\16g\32\262\0f\2B\1\262\0g\20\20\1C\371\0\350\340\r\22\274\0001\20\200B\21D\0\21\300\t\336g\b y\0\377\0\bN\220Nu/\2t\0\249\0\355\0\220\f8\0\4\f\274e\4\304<\0\375a\0\34\372$\37Nu\b8\0\0\r\36gJO\357\377\300 Oa\0\373\370aNC\372\0>a\0\374taJa\0\373\350\"Oa\0\36TO\357\0@\b8\0\6\b\4f\370J8\b\5k\362\b8\0\5\b\2f\352\b8\0\5\b\5f\342a\0\374*a\0\374\34Nu  Hit any key\0C\372\0\26`\4C\372\0\26\b:\0\0\365\304f\0\374\32B\20Nu\33[26m\0\33[50m\0~\0\36\27X\217,OJ8\f\274f<\276<\0\2g\6\276<\0\3f.HG>/\0\6,o\0\n\276Vg\30\276fg\20\276fg\f\276fg\b\276fg\4\276ff\4/N\0\nBGHGM\357\0\b`\26\276<\0\2e\6\276<\0\17c\n>/\0\6\316|\17\377\344ONNp\377NO`\372O\357\377\300 Oa\0\377lC\372\0\26a\0\373\220a\0\377f\"Oa\0\35rO\357\0@`\376  Press the RESET switch\0\0H\347\377\376O\357\377\200A\371\0\355\0\225C\371\0\350\340\rt1\b8\0\0\r\36g\n\22\202\b\220\0\1B\21`\20\b8\0\1\b\2g\b\22\202\b\320\0\1B\21\b\20\0\1f\0\0\204N\271\0\377\20\330N\271\0\377\16\240r\0p\254NO.\0,O N\b:\0\2\364\256g\4a\0\372|a\0\376\322\"Na\0\34\344a\0\0\332a\0\1ra\0\1\240a\0\1\334a\0\3(a\0\2\206a\0\3|a\0\3\332a\0\4\360a\0\0076a\0\b\362a\0\13\306a\0\f\34 N\20\374\0\33\20\374\0=p\32\320x\tr\20\300\20\374\0 a\0\376\204\"Na\0\34\220O\357\0\200L\337\177\377NuH\347\200\300 N\b:\0\1\3644g&\20\374\0\33\20\374\0[p\1\320x\tp\345Ha\0\372\222\20\374\0ra\0\372pC\372\0:a\0\372h`& \t\b:\0\0\364\4g\6C\372\0+`\4C\372\0,a\0\372N\"@a\0\372HC\372\0%a\0\372@\"Na\0\34&L\337\3\1Nu : \1\0\33[320l\0\33[304l\0\1 : \0C\372\0\216a\0\377|p\217NO\"\0B@\222\200H@\351\210\351\211\350H\350I\341\210\341\211\350H\350I\350\b\350\t\200\2740000\202\2740000 N\341\230\20\300\20\374\0.\341\230\20\300\20\374\0 \20\374\0(\341\230\260<\08e\n\20\374\0001\20\374\09`\b\20\374\0002\20\374\0000\20\300\341\230\20\300\20\374\0-\341\231\20\301\341\231\20\301\20\374\0-\341\231\20\301\341\231\20\301\20\374\0)a\0\371\f\"N`\0\33xROM Version\0\f\271NAME\0\377\377\340g\2NuC\372\0\34a\0\376\322 NC\371\0\377\377\344a\0\371^a\0\370\324\"N`\0\33@Model\0C\372\0(a\0\376\256 NC\372\0%\f9\0\334\0\350\340\13f\4C\372\0\36a\0\371.a\0\370\244\"N`\0\33\20Series\0X68000\0X68030\0\0C\372\0\230a\0\376n Nt\377rM\276<\0\6f\22Nz\b\b\340\210t\0\24\0\264<\0\6d\2rX\20\301C\372\0\177a\0\370\336\276<\0\3e\32rE\b\7\0\16g\f\276<\0\3g\fJGk\brL\20\301\20\374\0C\20\374\0000r0\322\7\20\301\20\374\0000 \2k\n\20\374\0-r\3a\0\371\30\20\374\0 \20\374\0( \7B@H@a\0\370\252\20\350\377\377\21|\0.\377\376C\372\0#a\0\370~a\0\367\364\"N`\0\32`Microprocessor\0C68\0MHz)\0JGk\2NuC\372\0B\276<\0\4d\4C\372\0Ra\0\375\254 NC\372\0j\276<\0\4d\30C\372\0h\362\0\\\1\362\'h\0 \37\200\237\200\237g\4C\372\0\\a\0\370\34a\0\367\222\"N`\0\31\376Floating-Point Unit (FPU)\0Floating-Point Coprocessor (FPCP)\0on-chip\0MC68881\0MC68882\0\b\7\0\16f\2NuC\372\0$a\0\375\26 NC\372\0007\276<\0\3d\4C\372\0005a\0\367\232a\0\367\20\"N`\0\31|Memory Management Unit (MMU)\0on-chip\0MC68851\0\0C\372\0Pa\0\374\302p\0\"8\f\370 N\20\374\0$a\0\366\374\20\374\0-\20\374\0$/\0 \1S\200a\0\366\352\20\374\0 \20\374\0( \1\220\237B@H@\350Ha\0\367<C\372\0\34a\0\367\32a\0\366\220\"N`\0\30\374Main Memory\0MB)\0@\347\0|\7\0/8\0\b*O!\374\0\376\257\326\0\b\225\312C\370\"\0A\370!\0002<\0\377\225\374\1\0\0\0S\211P\340Nq\22\222NqQ\320.MQ\311\377\352!\374\0\376\260\2\0\b\225\312A\370!\0002<\0\377\225\374\1\0\0\0J f\fP\320Nq\24\201NqQ\320.MQ\311\377\350!\374\0\376\260,\0\b\225\312A\370!\0002<\0\377\225\374\1\0\0\0J f\nNq\262\22NqV\320.MQ\311\377\352!\374\0\376\260P\0\b\225\312C\370\"\0002<\0\377\225\374\1\0\0\0Nq\24\241Nq.MQ\311\377\360!\337\0\bF\337\225\312G\370 \1J\23f\b \nf\f$K`\b \ng\4a\36\225\312R\213\266\374 \377c\344 \ng\2a\16A\370 \0p\177B\230Q\310\377\374NuC\372\0\26a\0\373p \nH@\341\210\"\13HA\341\211`\0\376\246Extension Memory\0\0\276<\0\4e,a\0\0\342J\200k$C\372\0\200a\0\3738 NC\372\0\300J\200g\4C\372\0\300a\0\365\276a\0\3654\"Na\0\27\240p\0a\0\1~J\200k$C\372\0ja\0\373\n NC\372\0\222J\200g\4C\372\0\222a\0\365\220a\0\365\6\"Na\0\27rp\1a\0\1PJ\200k$C\372\0Ua\0\372\334 NC\372\0dJ\200g\4C\372\0da\0\365ba\0\364\330\"Na\0\27DNuMotherboard Coprocessor\0Extension Coprocessor #1\0Extension Coprocessor #2\0MC68881\0MC68882\0p\377\f8\0\4\f\274d\2NuH\347|\360A\371\0\2 \0G\371\0\350\200#@\302\0|\7\0\26\23\226<\0\24b\4\326<\0\310Nz@\1NzP\0r\7N{\20\1N{\20\0$x\0\bC\372\0n!\311\0\b\"OBA\16h\30\0\0\6\16h\20\0\0\6\266\23gT\16P\20\0\262|\b\2f\3622<\\\1\16h\30\0\0\n\266\23g<\16P\20\0\262|\b\2f\3622<h\0\16h\30\0\0\n\266\23g$\16P\20\0\262|\211\0g\362\16\250\0\0\0\20\16\250\20\0\0\20\200\201\16\250\20\0\0\20\200\201g\2p\1.I!\312\0\bN{P\0N{@\1F\302L\337\17>NuH\347p\360A\371\0\351\340\0J\200g\4A\350\0\200G\371\0\350\200#p\377@\302\0|\7\0\26\23\226<\0\24b\4\326<\0\310$x\0\bC\372\0V!\311\0\b\"O1|\0\0\0\6Jh\0\6\266\23g@2\20\262|\b\2f\3641|\\\1\0\n\266\23g.2\20\262|\b\2f\3641|h\0\0\n\266\23g\0342\20\262|\211\0g\364 (\0\20\"(\0\20\200\201\"(\0\20\200\201g\2p\1.I!\312\0\bF\302L\337\17\16NuC\372\0<a\0\370\374 NC\372\0Ya\0\363\212E\370h\0ara\0\363\232C\372\0]a\0\363xE\371\0\355\0\0a^a\0\363\206C\372\0Ra\0\363d\"N`\0\25JDirect Memory Access Controller (DMAC)\0HD63450 (Main Memory:\0%, SRAM:\0%)\r\n\0\0H\347`\300A\371\0\350@\200C\371\0\350\200\35a\0\23\314$\0@\347\0|\7\0\23\374\0001\0\350\340\r\37(\0\4\37(\0\5\37(\0\6\37(\0)\37(\0-\37(\0001P\320\21|\0\b\0\4\21|\0\21\0\5\21|\0\5\0\6\21|\0\0\0-\21|\0\5\0)\21|\0\5\0001!J\0\f!J\0\0241|\23\210\0\n\37)\377\354\37)\377\370\37\21\2)\0\317\377\354\2)\0\317\377\370\22\274\0\0J\21f\374\23|\0\0\0\6\23|\0\0\0\b\22\274\0q\21|\0\200\0\7NqNqNqNqNqNqNqNqJ\20j\374\22\274\0\0J\21f\374p\0\220)\0\6r\0\222)\0\b\23|\0\310\0\6\23|\0\0\0\b\22\237\23_\377\370\23_\377\354P\320\21_\0001\21_\0-\21_\0)\21_\0\6\21_\0\5\21_\0\4B9\0\350\340\rF\337\300\374\0002\260\1c\4\320|\1\0\20\1S@\"<\0\7\241 \202\300HA\342H\220AHAp\0\321A\"\0a\0\22J \1L\337\3\6NuC\372\0Ba\0\367< NC\372\0Na\0\361\312a\0\1\4k\4a\0\1\220J\200j\bC\372\0Aa\0\361\264aHJ\200j\bC\372\08a\0\361\246C\372\0006a\0\361\236\"N`\0\23\204Real Time Clock (RTC)\0RP5C15 (\0\33[9m\0\33[29m\0)\r\n\0H\347\340\0HA <\0\0\17\377\300Aa\0\361r\20\374\0-\341\231p\0\20\1aZ\20\374\0-\341\231p\0\20\1aN\20\374\0 \20\374\0(\351\231p\17\300\1\260<\0\7c\2p\7\345\bC\373\0Ba\0\361 \20\374\0)\20\374\0 HBp\0\20\2a\36\20\374\0:\341\232p\0\20\2a\22\20\374\0:\341\232p\0\20\2a\6L\337\0\7Nu\260<\0\nd\4\20\374\0000`\0\360\376Sun\0Mon\0Tue\0Wed\0Thu\0Fri\0Sat\0???\0H\347\31\340~\0C\371\0\350\240\33E\371\0\351\240\1x\1p\1\200\21J\22J\22\22\200J\22J\22t\1\304)\377\372S\0J\22J\22\22\200J\22J\22r\7\302)\377\362A\351\377\377v\5p\17\300`\351\211\202\0Q\313\377\366U\210v\5p\17\300`\351\212\204\0Q\313\377\366p\17\300\2V\314\377\262 \1a\0\1$j\2~\377H@\340X\351\b\341X\320|\7\274H@\"\0 \2a\0\1\nj\2~\377$\0 \7L\337\7\230NuH\347\34\200*\1v\0\26\5\340Mx\0\30\5HE\312\274\0\0\17\377\272|\7\274e\0\0\314\272|\b\37b\0\0\304JDg\0\0\276\270|\0\fb\0\0\266p\0\270<\0\2f\bp\3\300\5W\300D\0A\372\0\246\3200@\377JCg\0\0\230\266@b\0\0\222p\217NOa\0\0\232H@\300|\0\377\320|\7l\260|\7\236d\4\320|\0d\272@epf\20H@\340X\270\0eff\6\341X\266\0e^\270|\0\2b\6SE\330|\0\f \5\300\374\1m\344M\320\205RD\310\374\0012\210\374\0\nH\304\320\204\320\203\220\274\0\13\t8\200\374\0\7H@&\1\351\233\306|\0\17\266@f  \2\260<\0;b\30\340H\260<\0;b\20H@\260<\0\27b\bp\0L\337\18Nup\377`\366\37\34\37\36\37\36\37\37\36\37\36\37H\347y\0~\0x\nv\2r\17\302\0\350\b\262\4d\4\260\4e\2~\377\24\0\345\b\320\2\320\0\320\1\340\230Q\313\377\342\340\230J\207L\337\0\236NuC\372\0000a\0\364d NC\372\0C\f9\0\377\0\351`-g\6J9\0\351`-g\4C\372\0002a\0\356\334a\0\356R\"N`\0\20\276Built-in Hard Disk Interface\0SASI\0SCSI\0\0C\372\0014a\0\364\n N09\0\355\0\30f\fC\372\1.a\0\356\220`\0\1\20\260|\200\0e\6\260|\300\0e\fC\372\1\32a\0\355\376`\0\0\370\260|\220\0d\26C\372\1\22a\0\356f\340Hr\17\300\201a\0\356v`\0\0\334\260|\240\0d\26C\372\0\377a\0\356J\340Hr\7\300\201a\0\356Z`\0\0\300\260|\260\0d\0\0\246 9\0\355\0\fr\3\302@\260\274\0\351\360 f\fC\372\0\323a\0\356\32`\0\0\206\260\274\0\352\0 e \260\274\0\352\0@d\30J\201f\24C\372\0\274a\0\355\372\344Hr\7\300\201a\0\356\n`\\\260\274\0\352\220\0e \260\274\0\352\220 d\30J\201f\24C\372\0\244a\0\355\322\344Hr\7\300\201a\0\355\342`4\260\274\0\374\0\0e \260\274\0\374\0 d\30J\201f\24C\372\0\214a\0\355\252\344Hr\7\300\201a\0\355\272`\fC\372\0\207a\0\355\226a\0\355D`\22C\372\0\177a\0\355\210 9\0\355\0\20a\0\3550a\0\354\364\"N`\0\17`Boot Device\0STD\0Unknown $\0SASI HD \0FD \0XEiJ HFS\0Expansion SCSI \0PhantomX VDISK \0Built-in SCSI \0ROM $\0SRAM $\0O\357\377\200 Oa\0\360\322\20\374\0 \20\374\0 p\376\300GU@f\0\0\236\208\f\274f\fp\20\300.\377\371$n\377\372`ZS\0f\fp\1\300.\0\b$n\0\n`JW\0d\fp@\300.\0\13$n\0\20`:f\fp\1\300.\0\f$n\0\24`,\276|\0\2f\36p\1\300.\0\fg\20J.\0\rj\n\b.\0\4\0\17g\2p\0$n\0\b`\bp\1$n\0\bR\212C\372\0\224UGg\4C\351\0\4a\0\354pC\372\0\220a\0\354hC\372\0\223J\0g\4C\351\0\ra\0\354X \na\0\354\4`8\276|\0\fd\24C\371\0\377\307`0\7\320@\322\361\0\370a\0\3548`\36\276|0\37f\nC\372\0wa\0\354(`\16C\372\0qa\0\354\0360\7a\0\353\236\276|p\tg\20C\372\0ea\0\354\n .\0\2a\0\353\264a\0\357\330a\0\353t\"Oa\0\r\340O\357\0\200p\377NO`\372Bus\0Address\0 error on \0writing to $\0reading from $\0NMI\0Error $\0 at $\0\0J8\f\355f\n#|\0\351`\1\0T`\4B\251\0TNuH\347\300\300J8\f\355f<J\271\0\0\t\326f\n\fy\27\f\0\0\t\314d\3562<\200\0\223\311pCNO\fy\13\270\0\0\t\314e\6\260<\0\4g\352J\0f\n\322|\1\0\262|\220\0f\334`$A\371\0\352\0Da\0\354\36\260\274SCSIf\na\0\353\336\260|EXg\b\"y\0\374\0 N\221L\337\3\3NuH\347|\376*<\0\300\0\0E\371\0\350\200\0G\371\0\353\200\0I\371\0\355\200\0p`t\360\304,\200\225\264\0g\16\25|\0001`\r\31@\200\225B*`\r <\26%\6)\262|V\377g\22\262|\26\377f\20\b,\0\0\200\225g\4\b\300\0\37`\0\1\224\262|v\377f\b <\26\0\0I`\356x\0\308\t<HD8\1B\4\222D\270|C\0f\24\25|\0001`\r\b\254\0\0\200\225B*`\rBD`\30\270|L\0f\22\25|\0001`\r\b\354\0\0\200\225B*`\rBD\262|\0\377g\0\18\262|\0Je\6p\377`\0\0012K\372\2\272\b,\0\0\200\225g\4K\372\4\3760\1\347H\332\300M\372\7D\334\355\0\6v\7\306-\0\4\7<\0\240g$\37*\200(\25|\0\7\200( E4\20FP0\0200\202\25_\200(FB\260Bg\6p\376`\0\0\344JDf\0\0\212\21\301\t<Bj\246\0p\37NO5|\0013\200*p\0A\371\0\340\0\0002<?\377 \300 \300Q\311\377\3725|\0003\200*Bx\tda\0\0\260Bj\204\2000\3N\271\0\377\263\350\b\352\0\3\200(p\36NOA\354\200.C\352\242\0\"\330\"\330 \20\"\0H@2\3002\3002\3002\3000\1\"\300\"\300\"\300\"\300\25l\200(`\0017|\0\20\210\b5|\0 \246\0005|\6\344\245\0`N\21\301\t<Bx\tdaPp\37\300j\246\0g<p\370\300*\200(\200\3\25@\200(5C\244\0A\370\t\\ \305 <\0\0\4\0\266|\0\4e\2\320@ \300p\17\7<\0\"g\4P\300`\b\7<\0\214g\2p\3770\200BDHD \4L\337\177>Nu4<\3\0\24\26A\372\7\376p\37\300*\200)r\2\302*`\7D\1\321\0\0200\0\0r\37\302\2\322\1\322.\0\1\0220\20\0\262\0d05B\200(J.\0\1f\b\b\252\0\1`\7`\6\b\352\0\1`\7A\352\200\2C\356\0\0040\331 \331 \331 \3315n\0\2\200\0`(A\352\200\0C\356\0\2 \331 \331 \331 \3315B\200(J.\0\1f\b\b\252\0\1`\7`\6\b\352\0\1`\0075n\0\22\200\20p\0A\352\200\0220\300 \300 \300 \300 \300 \300T\2100\374\0003 \3000\3005|\0\3\244\0r\37\302\26p\4\320n\0\0067@\210\fp\na\0\7\232p\3\300\1f\b7n\0\2\210\n`\0067|\0\377\210\n7n\0\16\210\16\208\t<\260<\0$e$\260<\0(d\4r\20`\32\260<\0,e\24\260<\0000e\f\260<\0<g\6\260<\0If\2r\0257A\210\0200\0252-\0\2\266|\0\4d\0204<\2\0\260Bc\0020\2\262Bc\0022\2S@SAB\270\th1\300\tl1\301\tnp\4\300CX@\341H!\300\t`p\4\346(W\301\220\1\21\300\t=!\374\0\340\0\0\tDB\270\tH0\0252-\0\2\346H\350IS@SA1\300\tp1\301\trB\270\ttB\201$8\tlpwNONu\2\0\2\0\4\0\0\0\2\0\2\0\4\0\0\24\1\0\1\0\4\0\0(\1\0\1\0\4\0\0<\2\0\2\0\0\0\0\0\2\0\2\0\0\0\0\24\1\0\1\0\0\0\0(\1\0\1\0\0\0\0<\2\0\2\0\1\0\0\0\2\0\2\0\1\0\0\24\1\0\1\0\1\0\0(\1\0\1\0\1\0\0<\2\0\2\0\3\0\0\0\2\0\2\0\3\0\0\24\1\0\1\0\3\0\0(\1\0\1\0\3\0\0<\3\0\2\0\4\0\0P\4\0\1\250\4\0\0d\4\0\3P\4\0\0x\2\200\1\340\4\0\0\214\3\0\2\0\1\0\0P\4\0\1\250\1\0\0d\4\0\3P\1\0\0x\2\200\1\340\1\0\0\214\3\0\2\0\3\0\0P\4\0\1\250\3\0\0d\4\0\3P\3\0\0x\2\200\1\340\3\0\0\214\1\200\1\0\4\0\0\240\1\200\1\0\0\0\0\240\1\200\1\0\1\0\0\240\1\200\1\0\3\0\0\240\2\0\2\0\4\0\0\264\2\0\2\0\0\0\0\264\2\0\2\0\1\0\0\264\2\0\2\0\3\0\0\264\1\0\1\0\4\0\0\310\1\0\1\0\0\0\0\310\1\0\1\0\1\0\0\310\1\0\1\0\3\0\0\310\2\0\1\0\4\0\1\270\2\0\1\0\0\0\1\270\2\0\1\0\1\0\1\270\2\0\1\0\3\0\1\270\2\0\1\0\4\0\1\270\2\0\1\0\0\0\1\270\2\0\1\0\1\0\1\270\2\0\1\0\3\0\1\270\2\0\2\0\5\0\0\0\2\0\2\0\5\0\0\24\1\0\1\0\5\0\0(\1\0\1\0\5\0\0<\3\0\2\0\5\0\0P\4\0\1\250\5\0\0d\4\0\3P\5\0\0x\2\200\1\340\5\0\0\214\1\200\1\0\5\0\0\240\2\0\2\0\5\0\0\264\1\0\1\0\5\0\0\310\2\0\1\0\5\0\1\270\2\0\1\0\5\0\1\270\2\0\2\0\7\0\0\0\2\0\2\0\7\0\0\24\1\0\1\0\7\0\0(\1\0\1\0\7\0\0<\3\0\2\0\7\0\0P\4\0\1\250\7\0\0d\4\0\3P\7\0\0x\2\200\1\340\7\0\0\214\1\200\1\0\7\0\0\240\2\0\2\0\7\0\0\264\1\0\1\0\7\0\0\310\2\0\1\0\7\0\1\270\2\0\1\0\7\0\1\270\2\0\2\0\4\0\0\334\2\0\2\0\4\0\0\360\1\0\1\0\4\0\1\4\1\0\1\0\4\0\1\30\2\0\2\0\0\0\0\334\2\0\2\0\0\0\0\360\1\0\1\0\0\0\1\4\1\0\1\0\0\0\1\30\2\0\2\0\1\0\0\334\2\0\2\0\1\0\0\360\1\0\1\0\1\0\1\4\1\0\1\0\1\0\1\30\2\0\2\0\3\0\0\334\2\0\2\0\3\0\0\360\1\0\1\0\3\0\1\4\1\0\1\0\3\0\1\30\3\0\2\0\4\0\1,\3\0\2X\4\0\1@\3\0\4\0\4\0\1T\2\200\1\340\4\0\1h\3\0\2\0\1\0\1,\3\0\2X\1\0\1@\3\0\4\0\1\0\1T\2\200\1\340\1\0\1h\3\0\2\0\3\0\1,\3\0\2X\3\0\1@\3\0\4\0\3\0\1T\2\200\1\340\3\0\1h\1\200\1\0\4\0\1|\1\200\1\0\0\0\1|\1\200\1\0\1\0\1|\1\200\1\0\3\0\1|\2\0\2\0\4\0\1\220\2\0\2\0\0\0\1\220\2\0\2\0\1\0\1\220\2\0\2\0\3\0\1\220\1\0\1\0\4\0\1\244\1\0\1\0\0\0\1\244\1\0\1\0\1\0\1\244\1\0\1\0\3\0\1\244\2\0\1\0\4\0\1\314\2\0\1\0\0\0\1\314\2\0\1\0\1\0\1\314\2\0\1\0\3\0\1\314\2\0\1\0\4\0\1\314\2\0\1\0\0\0\1\314\2\0\1\0\1\0\1\314\2\0\1\0\3\0\1\314\2\0\2\0\5\0\0\334\2\0\2\0\5\0\0\360\1\0\1\0\5\0\1\4\1\0\1\0\5\0\1\30\3\0\2\0\5\0\1,\3\0\2X\5\0\1@\3\0\4\0\5\0\1T\2\200\1\340\5\0\1h\1\200\1\0\5\0\1|\2\0\2\0\5\0\1\220\1\0\1\0\5\0\1\244\2\0\1\0\5\0\1\314\2\0\1\0\5\0\1\314\2\0\2\0\7\0\0\334\2\0\2\0\7\0\0\360\1\0\1\0\7\0\1\4\1\0\1\0\7\0\1\30\3\0\2\0\7\0\1,\3\0\2X\7\0\1@\3\0\4\0\7\0\1T\2\200\1\340\7\0\1h\1\200\1\0\7\0\1|\2\0\2\0\7\0\1\220\1\0\1\0\7\0\1\244\2\0\1\0\7\0\1\314\2\0\1\0\7\0\1\314\25\0\0[\0\t\0\21\0Q\0027\0\5\0(\2(\0\33\5\0\0K\0\3\0\5\0E\1\3\0\2\0\20\1\0\0,\20\0\0-\0\4\0\6\0&\0027\0\5\0(\2(\0\33\0\0\0%\0\1\0\0\0 \1\3\0\2\0\20\1\0\0$\26\0\0\211\0\16\0\34\0|\0027\0\5\0(\2(\0\33\26\0\0\257\0\17\0\37\0\237\1\320\0\7\0 \1\310\0\33\32\0\0\257\0\17\0\37\0\237\1\320\0\7\0 \1\310\0\33\26\0\0\211\0\16\0$\0t\0027\0\5\08\2\30\0\33\21\1\0C\0\6\0\13\0;\0027\0\5\0(\2(\0\33\26\0\0\211\0\16\0,\0l\0027\0\5\0(\2(\0\33\21\1\0C\0\6\0\23\0003\0027\0\5\0(\2(\0\33\25\0\0Q\0\5\0\13\0K\2p\0\1\0C\2C\0\33\25\0\0Q\0\5\0\13\0K\2p\0\1\0S\0023\0\33\20\0\0)\0\2\0\3\0#\2p\0\1\0C\2C\0\33\20\0\0)\0\2\0\3\0#\2p\0\1\0S\0023\0\33\26\0\0{\0\b\0\23\0s\2p\0\1\0C\2C\0\33\26\0\0{\0\b\0\23\0s\2p\0\1\0\27\2o\0\33\32\0\0{\0\b\0\23\0s\2p\0\1\0C\2C\0\33\27\0\0c\0\13\0\r\0]\2\f\0\1\0\"\2\2\0\33\21\1\0C\0\6\0\13\0;\2p\0\1\0C\2C\0\33\26\0\0{\0\b\0#\0c\2p\0\1\0C\2C\0\33\21\1\0C\0\6\0\23\0003\2p\0\1\0C\2C\0\33\21\0\0[\0\t\0\21\0Q\0027\0\5\0(\2(\0\33\21\0\0Q\0\5\0\13\0K\2p\0\1\0C\2C\0\33\0\0\2\2\0\0\0\0\0\0\2\2\0\0\0\0\0\0\2\2\0\0\0\0\0\0\2\2\0\0\0\0\3\1\5\4\7\7\6\6\3\1\5\4\7\7\6\6\3\1\5\4\7\7\6\6\3\1\5\4\7\7\6\6H\347\300\0002\b\344I\341\230\260\1V\300\320\0L\337\0\3NuH\347\360\0S\200e`\"8\r f\00428\f\270\f8\0\4\f\274d\0064<\0\315`\ff\0064<\0013`\0044<\4\315&\1HC\306\302\302\302HCBC\322\203\302|\360\0\351\231HAS\201$\1H@H@\"\2 H HHAHAQ\311\377\376HAQ\311\377\366Q\310\377\352H@Q\310\377\342L\337\0\17Nup\377\f8\0\2\f\274d\4JAf\24\262|\0\5d\16/\1\322A2;\20\nN\273\20\6\"\37Nu\0\n\0D\0n\0~\0\244 8\r /\0\320\200\320\237\320\200\f8\0\2\f\274d\2\320\200\320\274\0\0\0002\200\374\0dH@B@J8\f\276V\300\342XJ8\f\275V\300\342X\208\f\274Nup\0\f8\0\2\f\274e\36Nz\0\2\f8\0\4\f\274d\6\342\230\343\30`\4H@\343X\343\230\300\274\0\0\0\3Nu/\2t\0\249\0\355\0\220a*$\37Nu\f8\0\2\f\274e\34\f8\0\4\f\274d\22/\0Nz\0\2\200|\b\bN{\0\2 \37`\2\364\370Nup\0\f8\0\2\f\274e\\H\347p\0v\3\304\203\342\232Nz\0\2\f8\0\4\f\274d\32DB\304|\20\200\343\232\"\0\302|\336\376\202BN{\20\2\342\230\343\30`$\342ZHB\"\0\302\274\177\377\177\377\202\202N{\20\2F\202\304\200j\2\364xJBj\2\364\230H@\343X\343\230\300\203L\337\0\16Nu/\2t\0a\222$\37Nu/\2t\1a\210$\37NuJ8\t\222g\34Jx\r\fg\6J8\t\221f\20\b9\0\1\0\350\0*f\6a\6F8\t\221NuH\347\340\34008\tvH@B@\352\21028\tt\262x\tpc\00428\tp\320A\320\270\tH\320\274\0\340\0\0$@?9\0\350\0*\b\271\0\0\0\350\0*r\200\208\r1\340)a\24\325\374\0\2\0\0a\f3\337\0\350\0*L\337\7\7Nu48\r\16N\373 \2\263\22 H\263*\0\200\263*\1\0\263*\1\200\263*\2\0\263*\2\200\263*\3\0\263*\3\200\263*\4\0\263*\4\200\263*\5\0\263*\5\200\263*\6\0\263*\6\200\263*\7\0\263*\7\200NuJ8\t\223f\24J8\t\222f\0161\374\0\5\t\274P\370\t\222B8\t\221NuJ8\t\223f\0301\374\0\5\t\274B8\t\222J8\t\221g\ba\0\377,B8\t\221Nua\0\2\346 8\ttNu/\1`\4a\0\2\330r\0\22\31f\366\"\37 8\ttNu/\1p\0\208\t\224\341H\208\t\224\262|\377\377g\24\262|\177\377b\f\21\301\t\224\340I\21\301\r0`\2p\377\"\37Nup\0\208\r1\340\230\200\270\tt\262|\377\377f\4\"\0NuH\347P\0006\1\302|\0\377\340K\262x\tpb\"\264x\trb\34\266|\0\7b\26a\0\377^1\301\tt1\302\tv\21\303\r1a\0\3772`\2p\377L\337\0\nNuH\347`\0a\0\377<08\tv\260x\trd\bR@1\300\tv`\26p\00128\trt\0a\0\33\02408\tr2\0a\0\33xa\0\376\364L\337\0\6p\0NuH\347`\0a\0\377\00008\tvJ@c\bS@1\300\tv`\26p\00028\trSAt\1a\0\32\330p\0r\0a\0\33>a\0\376\272L\337\0\6p\0Nu/\1a\0\376\310\302|\0\377f\2r\00108\tv\220Aj\4r\377`\0061\300\tvr\0a\0\376\216 \1\"\37Nu/\1a\0\376\236\302|\0\377f\2r\00108\tv\320A\260x\trc\00408\tr1\300\tva\0\376b\"\37p\0Nu/\1a\0\376r\302|\0\377f\2r\00108\tt\320A\260x\tpc\00408\tp1\300\ttB8\r1a\0\3762\"\37p\0Nu/\1a\0\376B\302|\0\377f\2r\00108\tt\220Aj\2B@1\300\ttB8\r1a\0\376\b\"\37p\0Nu/\1a\0\376\30S\1d\ba\0\13Br\0`\30f\ba\0\13nr\0`\16U\1d\ba\0\13\250r\0`\2r\377a\0\375\324 \1\"\37Nu/\1a\0\375\344S\1d\ba\0\13\304r\0`\30f\ba\0\13\332r\0`\16U\1d\ba\0\13\354r\0`\2r\377a\0\375\240 \1\"\37Nu/\1a\0\375\260a\0\13\346a\0\375\214p\0\"\37Nu/\1a\0\375\234a\0\f\36a\0\375xp\0\"\37Nua\0\375\212 8\tH\262\274\377\377\377\377g\36\302\274\3\370\3\374 \1H@\346HH\301\357\211\322@ 8\tH!\301\tHB\270\ttr\177\302@\347IHA\356\2102\0 8\tp\264\274\377\377\377\377g\16\304\274\0\177\0?!\302\tpB\270\tt$\0a\0\375\30p\0Nu1\370\t\272\t\274@\347\0|\7\0J8\t\223f\6J8\t\221f\ba\0\374XP\370\t\221P\370\t\222Q\370\t\223F\337Nua\0\375\0P\370\t\223NuH\347\300\0\208\t\220f\0\0\204\262|\0\37b\6a\0\1\364`t\262|\0\177bF\262|\0\\f\20\b9\0\0\0\355\0Yg\0042<\0\200`*\262|\0~f\20\b9\0\1\0\355\0Yg\0042<\0\201`\24\262|\0|f\16\b9\0\2\0\355\0Yg\0042<\0\202a\0\17\322`(\262|\0\237b\6\21\301\t\220`\34\262|\0\337b\6a\0\17\272`\20\262|\0\377b\6\21\301\t\220`\4a\0\17\250`\32\260<\0\33f\6a\0\3\372`\16B8\t\220\341H\20\0012\0a\0\17\214L\337\0\3NuH\347\376\30008\r4g\0\1J28\r6 x\r8C\370\r<Bx\r4Bx\r6!\311\r8\220Ab\0162\31a\0\17X\262\310e\366`\0\1 \b\270\0\3\r\274gD2\31a\0\17B\262\310e\366x\0\308\r1\320DS@z\7\312@\346H48\tt\320B6\00008\tv2\0a\0\30\\RE\272|\0\be\4QERC1\303\tt\21\305\r1`\0\0\324\b\270\0\1\r\274gDx\0\308\r1\320DS@z\7\312@\346H48\tt\320B6\00008\tv2\0a\0\30\32RE\272|\0\be\4QERC1\303\tt\21\305\r12\31a\0\16\300\262\310e\366`\0\0\210\b\270\0\2\r\274g~<\0\342H\234@x\0\308\r1\320DS@z\7\312@\346H48\tt\320B6\00008\tv2\0a\0\27\310RE\272|\0\be\4QERC1\303\tt\21\305\r12\31a\0\16n\262\310e\3660\6x\0\308\r1\320DS@z\7\312@\346H48\tt\320B6\00008\tv2\0a\0\27\206RE\272|\0\be\4QERC1\303\tt\21\305\r1L\337\3\177NuH\347\300\0Jx\r4g\4a\0\376\23208\tt\260x\tpc6\262|\0\bg008\tv\260x\trd\bR@1\300\tv`\26p\00128\trt\0a\0\26\25408\tr2\0a\0\27\20Bx\ttB8\r1\322A2;\20\fN\273\20\bL\337\0\3Nu\0@\0B\0D\0F\0H\0J\0L\0N\0z\0\260\1\f\1F\1`\1\270\1\316\1\320\1\322\1\324\1\326\1\330\1\332\1\334\1\336\1\340\1\342\1\344\1\346\2\6\2\26\2\30\2\32\2(NuNuNuNuNuNuNuH\347\340\300 8\txr\377\260\201f\b x\r\22N\220`\0202<\4\3t\00048\t|\"@p`NOL\337\3\7Nu/\0a\0\372\02408\ttg\fS@1\300\ttB8\r1`\02608\tvg\20S@1\300\tv1\370\tp\ttB8\r1a\0\371\314 \37NuH\347\340\0a\0\371\33408\ttP@\300|\377\370\260x\tpb\n1\300\ttB8\r1`008\tv\260x\trd\bR@1\300\tv`\26p\00128\trt\0a\0\25\23208\tr2\0a\0\25\376Bx\ttB8\r1a\0\371rL\337\0\7NuH\347\340\0a\0\371\20008\tv\260x\trd\bR@1\300\tv`\26p\00128\trt\0a\0\25X08\tr2\0a\0\25\274a\0\3718L\337\0\7Nu/\0a\0\371H08\tvc\6S@1\300\tva\0\371\34 \37NuH\347\340\0a\0\371,08\tt\260x\tpd\fR@1\300\ttB8\r1`008\tv\260x\trd\bR@1\300\tv`\26p\00128\trt\0a\0\24\35608\tr2\0a\0\25RBx\ttB8\r1a\0\370\306L\337\0\7Nu/\0a\0\370\326Bx\ttB8\r1a\0\370\256 \37NuNuNuNuNuNuNuNuNuNuNuNuNuH\347\300\0a\0\370\246B@28\tra\0\25\0B\270\tta\0\370xL\337\0\3Nu\21\374\0\33\t\220!\374\0\0\t\206\t\202NuNuNua\0\370vB\270\tta\0\370RNuNuH\347\200\200 x\t\202\20\301\261\374\0\0\t\220d\4!\310\t\202\208\t\206\260<\0[f\26p \200\1\260<\0`e\n\260<\0zb\4a\0\1r`D\260<\0*f\4aB`:\260<\0=f\f\261\374\0\0\t\211f\2aT`(\260<\0Df\6a\0\0\212`\34\260<\0Ef\6a\0\0\274`\20\260<\0Mf\6a\0\0\366`\4B8\t\220L\337\1\1NuH\347\300\0a\0\367\342B@28\tra\0\24<B\270\ttB8\t\220a\0\367\260L\337\0\3NuH\347\340\0a\0\367\276r\0t\0\228\t\210\248\t\207p \222@\224@\262x\tpb\22\264x\trb\f1\301\tt1\302\tvB8\r1B8\t\220a\0\367pL\337\0\7NuH\347\340\0a\0\367~08\tv\260x\trd\bR@1\300\tv`\26p\00128\trt\0a\0\23V08\tr2\0a\0\23\272B8\t\220a\0\3672L\337\0\7NuH\347\340\0a\0\367@08\tv\260x\trd\bR@1\300\tv`\26p\00128\trt\0a\0\23\03008\tr2\0a\0\23|Bx\ttB8\r1B8\t\220a\0\366\354L\337\0\7NuH\347\340\0a\0\366\37208\tvJ@c\bS@1\300\tv`\26p\00028\trSAt\1a\0\22\322p\0r\0a\0\238B8\t\220a\0\366\260L\337\0\7NuH\347\360\200a\0\366\276 8\r\26g\16A\370\t\206Hy\0\376\321\272/\0NuA\370\t\2060\30\260|[>f\"0\30\260|5lf\6Q\370\t\223`\20\260|5hf\6P\370\t\223`\4a\0\1\366`\0\1\344\260|[?f$0\30\260|4lf\6Bx\r\n`\22\260|4hf\b1\374\0\2\r\n`\4a\0\1\314`\0\1\272A\370\t\207p\0r\377t\377v\377\20\30\260<\0 g\370\260<\0000e\0\0\214\260<\09b\0\0\204r\0\220<\0000\302\374\0\n\322@\20\30\260<\0000e\6\260<\09c\350\260<\0;fb\20\30\260<\0 g\370\260<\0000eT\260<\09bNt\0\220<\0000\304\374\0\n\324@\20\30\260<\0000e\6\260<\09c\350\260<\0;f.\20\30\260<\0 g\370\260<\0000e \260<\09b\32v\0\220<\0000\306\374\0\n\326@\20\30\260<\0000e\6\260<\09c\350\260<\0@f\ba\0\0016`\0\1\6\260<\0Af\ba\0\1\274`\0\0\370\260<\0Bf\ba\0\1\320`\0\0\352\260<\0Cf\ba\0\1\354`\0\0\334\260<\0Df\ba\0\2\f`\0\0\316\260<\0Hf\ba\0\2&`\0\0\300\260<\0Jf\ba\0\2^`\0\0\262\260<\0Kf\ba\0\3\6`\0\0\244\260<\0Lf\ba\0\3l`\0\0\226\260<\0Mf\ba\0\3\252`\0\0\210\260<\0Pf\6a\0\3\350`z\260<\0Rf\6a\0\4l`n\260<\0Xf\6a\0\4b`b\260<\0cf\6a\0\4\222`V\260<\0ff\6a\0\1\260`J\260<\0lf\6a\0\4\262`>\260<\0mf\6a\0\4\336`2\260<\0nf\6a\0\6\336`&\260<\0rf\6a\0\7l`\32\260<\0sf\6a\0\7\230`\16\260<\0uf\6a\0\7\262`\2a\16B8\t\220a\0\364vL\337\1\17NuH\347\200\200 8\t~g\16A\370\t\206Hy\0\376\323\360/\0NuL\337\1\1NuH\347\374\360\262|\377\377g\4JAf\2r\188\tt\330A68\tp\226Dd\6a\0\0024`j08\tvH@B@\352\210\320\270\tH\320\274\0\340\0\0$@&J\324\370\tt\324\303\326\370\tpt\17A\352\0\1C\353\0\0012\3\23 Q\311\377\374E\352\0\200G\353\0\200Q\312\377\346\325\374\0\1\370\0\327\374\0\1\370\0\265\374\0\344\0\0e\31608\tv2\00048\tt6\4SCx\0z\7a\0\20ZL\337\17?NuH\347\300\0\262|\377\377g\4JAf\2r\00108\tv\220Ae\0041\300\tvL\337\0\3NuH\347\300\0\262|\377\377g\4JAf\2r\00108\tv\320A\260x\trc\00408\tr1\300\tvL\337\0\3NuH\347\300\0\262|\377\377g\4JAf\2r\00108\tt\320A\260x\tpc\00408\tp1\300\ttB8\r1L\337\0\3NuH\347\300\0\262|\377\377g\4JAf\2r\00108\tt\220Ad\2p\0001\300\ttB8\r1L\337\0\3NuH\347`\0\262|\377\377g\4JAf\2r\1SA\262x\trc\00428\tr\264|\377\377g\4JBf\2t\1SB\264x\tpc\00448\tp1\302\tt1\301\tvB8\r1L\337\0\6Nu\262|\377\377g\4JAf\4a\26`\22\262|\0\1f\4aB`\b\262|\0\2f\2a~NuH\347\374\00008\tv28\tr\260Ad\6R@a\0\17\03608\tv2\00048\tt68\tpx\0\308\r1z\7a\0\17\32L\337\0?NuH\347\374\00028\tvJAc\bB@SAa\0\16\35208\tv2\0t\00068\ttx\0z\7\3328\r1\272|\0\7c\16\266x\tpd\6RCQE`\2z\7a\0\16\324L\337\0?NuH\347\300\0p\00028\tra\0\16\252B\270\ttL\337\0\3Nu\262|\377\377g\4JAf\4a\26`\22\262|\0\1f\4a,`\b\262|\0\2f\2a@NuH\347\374\00008\tv2\00048\tt68\tpx\0z\7a\0\16zL\337\0?NuH\347\374\00008\tv2\0t\00068\ttx\0z\7a\0\16\\L\337\0?NuH\347\300\00008\tv2\0a\0\0162L\337\0\3NuH\347\340\0\262|\377\377g\4JAf\2r\00108\tv48\trRB\224@\224Ab\n28\tra\0\16\4`\24\324@SB\322@\303Ba\0\r\210\303BSAa\0\r\356Bx\ttB8\r1L\337\0\7NuH\347\360\0\262|\377\377g\4JAf\2r\00108\tv68\trRC\226@\226Ab\n28\tra\0\r\270`\0244\0\320A28\tra\0\r<0\2\320Ca\0\r\242Bx\ttB8\r1L\337\0\17NuH\347\374\360\262|\377\377g\4JAf\2r\188\tt\330A68\tp\226Dd\6a\0\376\366`f08\tvH@B@\352\210\320\270\tH\320\274\0\340\0\0$@&J\324\304\326\370\ttt\17 J\"K2\3\22\330Q\311\377\374E\352\0\200G\353\0\200Q\312\377\352\325\374\0\1\370\0\327\374\0\1\370\0\265\374\0\344\0\0e\32208\tv2\00048\tt\324CRB68\tpx\0z\7a\0\r L\337\17?NuNuH\347\374\0\262|\377\377g\4JAf\2r\00148\tt6\2\326ASC\266x\tpc\00468\tp08\tv2\0x\7\3108\r1z\7a\0\f\342L\337\0?NuH\347\300\0\262|\377\377g\4JAf\2r\00108\tpR@\347H\262@b\26\b\370\0\2\r\274!\374\0\0\r<\r8Bx\r61\301\r4L\337\0\3NuH\347\300\0\262|\377\377g\4JAf\2r\00108\tpR@\347H\262@b\26\b\370\0\3\r\274!\374\0\0\r<\r8Bx\r61\301\r4L\337\0\3Nu/\1a\30\264|\377\377g\0042\2a\16\266|\377\377g\0042\3a\4\"\37Nu\262|\377\377g\4JAf\16\21\374\0\3\t\224B8\r0`\0\1\330\262|\0\1f\22\bx\0\2\t\224f\6\b\270\0\4\t\224`\0\1\300\262|\0\2f\20\b\270\0\2\t\224\b\370\0\4\t\224`\0\1\252\262|\0\3f\n\b\370\0\5\t\224`\0\1\232\262|\0\4f\20\b\370\0\6\t\224\b\270\0\0\r0`\0\1\204\262|\0\7f\n\bx\0\3\t\224`\0\1t\262|\0\tf\n\b\370\0\7\t\224`\0\1d\262|\0\25f\20\b\270\0\6\t\224\b\370\0\0\r0`\0\1N\262|\0\26f\20\b\270\0\2\t\224\b\270\0\4\t\224`\0\18\262|\0\27f\n\b\270\0\5\t\224`\0\1(\262|\0\30f\20\b\270\0\6\t\224\b\270\0\0\r0`\0\1\22\262|\0\32f\n\b\370\0\1\r0`\0\1\2\262|\0\33f\n\b\270\0\3\t\224`\0\0\362\262|\0\35f\n\b\270\0\7\t\224`\0\0\342\262|\0\36e\30\262|\0%b\22\222|\0\36\28\0\360\t\224\2038\t\224`\0\0\304\262|\0(e\32\262|\0/b\24\222|\0(P\1\28\0\360\t\224\2038\t\224`\0\0\244\262|\0002f\n\b\270\0\1\r0`\0\0\224\262|\0003f\16\b\370\0\2\r0\b\270\0\3\r0`~\262|\0004f\16\b\270\0\2\r0\b\370\0\3\r0`j\262|\0005f\b\b\370\0\4\r0`\\\262|\0006f\16\b\270\0\2\r0\b\270\0\3\r0`H\262|\0007f\b\b\270\0\4\r0`:\262|\0If\16\b\370\0\6\r0\b\270\0\5\r0`&\262|\0Jf\16\b\270\0\6\r0\b\370\0\5\r0`\22\262|\0Kf\f\b\270\0\6\r0\b\270\0\5\r0NuH\347\340\200\262|\0\6f$4<\1\33a^4<\34[aX08\tva\0304<\';aL08\tta\f4<\24Ra@L\337\1\7NuR\0\"<\1\nd\0\340\211\260\1e\372t\376TB\220\1d\3724; \16a\36\320\1\340\211J\1f\352Nu\0130\0021\0032\0043\0054\0065\0076\b7\t8\n9@\347\0|\7\0\fx\0@\b\22d\32 x\b\24T\210\260\374\b\234e\4A\370\b\0340\202!\310\b\24Rx\b\22F\337NuH\347\300\0\262|\377\377g\4JAf\2r\00108\tpR@\347H\262@b\26\b\370\0\1\r\274!\374\0\0\r<\r8Bx\r61\301\r4L\337\0\3Nu\262|\377\377f\0361\370\tv\t\2301\370\tt\t\226\21\370\r1\r3\21\370\t\224\t\225\21\370\r0\r2Nu\262|\377\377f\0361\370\t\230\tv1\370\t\226\tt\21\370\r3\r1\21\370\t\225\t\224\21\370\r2\r0NuH\347\377\340O\357\377\200>\1\b8\0\1\r0g \262|\0 e\32\262|\0\202b\24A\372\n\24\222|\0 \302\374\0\"\321\301<\30t\1`\16t\bp\26NO @4\1HA<\1Jx\r4gR x\r8\261\374\0\0\r\274d\0\6$0\307!\310\r8\b8\0\1\r0g.\b8\0\2\t\224g\2RF\b8\0\5\t\224g\26VF\b\370\0\0\r\274g\nWx\r6d\4Bx\r6`\6\b\270\0\0\r\274\335x\r6`\0\5\340a\0\354,\"Op\0JBf\16v\17\22\330\22\3002\300Q\313\377\370`\nv\0172\3302\300Q\313\377\372\b8\0\2\t\224g6\"O\b8\0\1\r0g\24v\17 \21\"\0\342\210\200\201\"\300Q\313\377\364RF`\30t\1\354\272D\202v\17 \21\"\0\342\210\200\201\300\202\"\300Q\313\377\362\b8\0\4\t\224g8\"O$<\252\252\252\252p\1\3008\r1\341\272v\17 \21\"\0\342\211\343\210\200\201\263\317g\4\200\251\377\374JCg\4\200\251\0\4\300\202F\200\301\231\343\232Q\313\377\334\b8\0\6\t\224g\f\"Op\1\354\270D\200\201\251\0<\b8\0\7\t\224g\f\"Op\1\354\270D\200\201\251\0 \b8\0\0\r0g4\"Ot\1\354\272D\202p\3\3008\r1\"<\210\210\210\210&<UUUU\341\271\341\273 \1\345\230\300\202\302\202\306\202\201\251\0004\207\251\08\203\251\0<\b8\0\2\r0g\0\0\314r\20\222Fe\22\342I\"Ov\17 \21\342\250\"\300Q\313\377\370|\20E\372\4\276A\327C\327p\0v\3\20\20\0222\0\0\355I\20(\0\1\0222\0\0\345I\20(\0\4\0242\0\0\355J\20(\0\5\0242\0\0\345J\202B2\201P\210X\211\20\20\0222\0\0\355I\20(\0\1\0222\0\0\345I2\201X\210X\211\20\20\0222\0\0\355I\20(\0\1\0222\0\0\345I2\201X\210X\211Q\313\377\240B\257\0000B\257\0004B\257\08B\257\0<A\357\0000C\357\08v\13  \344\210#\0Q\313\377\370B\227B\257\0\4 O\0\230\377\377\0\0v\r\0\230\200\1\0\0Q\313\377\370\0\230\377\377\0\0\b8\0\3\r0g\0\1vr\20\222Fe\22\342I\"Ov\17 \21\342\250\"\300Q\313\377\370|\20E\372\3\352A\327C\327p\0v\3\20\20\0222\0\0\355I\20(\0\1\0222\0\0\345I\20(\0\4\0242\0\0\355J\20(\0\5\0242\0\0\345J\202B2\201P\210X\211\20\20\0222\0\0\355I\20(\0\1\0222\0\0\345I2\201X\210X\211\20\20\0222\0\0\355I\20(\0\1\0222\0\0\345I2\201X\210X\211Q\313\377\240B\257\0000B\257\0004B\257\08B\257\0<A\357\0000C\357\08v\13  \344\210#\0Q\313\377\370B\227B\257\0\4 O\2\230\7\340\0\0\2\230\37\370\0\0\2\230?\374\0\0\2\230\177\376\0\0\2\230\177\376\0\0\2\230\377\377\0\0\2\230\377\377\0\0\2\230\377\377\0\0\2\230\377\377\0\0\2\230\377\377\0\0\2\230\377\377\0\0\2\230\177\376\0\0\2\230\177\376\0\0\2\230?\374\0\0\2\230\37\370\0\0\2\230\7\340\0\0 O\0\230\7\340\0\0\0\230\30\30\0\0\0\2300\f\0\0\0\230`\6\0\0\0\230@\2\0\0\0\230\200\1\0\0\0\230\200\1\0\0\0\230\200\1\0\0\0\230\200\1\0\0\0\230\200\1\0\0\0\230\200\1\0\0\0\230@\2\0\0\0\230`\6\0\0\0\2300\f\0\0\0\230\30\30\0\0\0\230\7\340\0\0\b8\0\4\r0g\n\"Op\1\354\270D\200\201\221\b8\0\5\r0g\0\0\220E\372\2rA\357\0@C\357\0<p\0v\3Q\210Y\211\20\20\0222\0\0\355I\20(\0\1\0222\0\0\345I\20(\0\4\0242\0\0\355J\20(\0\5\0242\0\0\345J\202B2\201Y\210Y\211\20\20\0222\0\0\355I\20(\0\1\0222\0\0\345I2\201Y\210Y\211\20\20\0222\0\0\355I\20(\0\1\0222\0\0\345I2\201Q\313\377\240B\227B\257\0\4B\257\0\bB\257\0<p\3\300F\344N2\6\334F\334A\334@\b8\0\6\r0g\0\0\216E\372\1\332A\327C\327p\0v\3\20\20\0222\0\0\355I\20(\0\1\0222\0\0\345I\20(\0\4\0242\0\0\355J\20(\0\5\0242\0\0\345J\202B2\201P\210X\211\20\20\0222\0\0\355I\20(\0\1\0222\0\0\345I2\201X\210X\211\20\20\0222\0\0\355I\20(\0\1\0222\0\0\345I2\201X\210X\211Q\313\377\240B\257\0000B\257\0004B\257\08B\257\0<p\3\300F\344N2\6\334F\334A\334@\b8\0\3\t\224g\20\"Op\1\354\270D\200v\17\261\231Q\313\377\374C\357\0@p\1\354\270D\200v\17\"\300Q\313\377\374\b8\0\5\t\224gB OC\357\0@v\3t\3 \20\346\250 \300 \21\346\250\"\300Q\312\377\362SCf\352VF\b\370\0\0\r\274g\30W8\r1d\22P8\r1Sx\ttd\bBx\ttB8\r1`\6\b\270\0\0\r\27408\tpR@\347H28\tt\347I\3228\r1\220An208\tv\260x\trd\bR@1\300\tv`\26p\00128\trt\0a\0\2\34008\tr2\0a\0\3DBx\ttB8\r1`N\260FlJ08\tv2\00048\tt68\tpx\7\3108\r1z\7a\0\003208\tv\260x\trd\bR@1\300\tv`\26p\00128\trt\0a\0\2\22008\tr2\0a\0\2\364Bx\ttB8\r108\tv28\ttt\7\3048\r16\6x\3\3108\t\224 OC\357\0@a\0\1\"\3348\r1p\7\300F\21\300\r1\346N\335x\tta\0\3466O\357\0\200L\337\7\377Nu\0\4\b\f\20\24\30\34\20\24\30\34\20\24\30\34 $(,048<048<048<@DHLPTX\\PTX\\PTX\\`dhlptx|ptx|ptx|\200\204\210\214\220\224\230\234\220\224\230\234\220\224\230\234\240\244\250\254\260\264\270\274\260\264\270\274\260\264\270\274\300\304\310\314\320\324\330\334\320\324\330\334\320\324\330\334\340\344\350\354\360\364\370\374\360\364\370\374\360\364\370\374\200\204\210\214\220\224\230\234\220\224\230\234\220\224\230\234\240\244\250\254\260\264\270\274\260\264\270\274\260\264\270\274\300\304\310\314\320\324\330\334\320\324\330\334\320\324\330\334\340\344\350\354\360\364\370\374\360\364\370\374\360\364\370\374\200\204\210\214\220\224\230\234\220\224\230\234\220\224\230\234\240\244\250\254\260\264\270\274\260\264\270\274\260\264\270\274\300\304\310\314\320\324\330\334\320\324\330\334\320\324\330\334\340\344\350\354\360\364\370\374\360\364\370\374\360\364\370\374H\347\374\370H@B@\352\210\320A\320\270\tH\320\274\0\340\0\0\b\200\0\0g\2PB(@\326B\266|\0\20bP\342\fd$$H&Iz\0170\0322\33\344h\344iFA\302T\200A8\300T\212T\213I\354\0~Q\315\377\346`\26&Iz\0172\33\344iFA\303\\T\213I\354\0~Q\315\377\360\331\374\0\1\370\0\271\374\0\344\0\0e\264`\0\0\300\266|\0 bH\342\fd $H&Iz\17 \32\"\33\344\250\344\251F\201\302\224\200\201(\300I\354\0|Q\315\377\352`\24&Iz\17\"\33\344\251F\201\303\234I\354\0|Q\315\377\362\331\374\0\1\370\0\271\374\0\344\0\0e\272`p\342\fd<$H&Iz\17 \32\"\33\344\250\344\251F\201\302\224\200\201(\3000*\377\3762+\377\376H@HAB@BA\344\250\344\251FA\302T\200A8\300I\354\0zQ\315\377\316`\"&Iz\17\"\33\344\251F\201\303\2342+\377\376HABA\344\251FA\303\\I\354\0zQ\315\377\344\331\374\0\1\370\0\271\374\0\344\0\0e\220L\337\37?NuH\347\360\200&8\tH\356\213\344K\222@\345IVA\345H\320C\345J\324C\264@b\0066<\1\1`\b\320A\324A6<\376\377\341H\20\2A\371\0\350\200\1@\302J\20k\374\0|\7\0J\20j\3741@\200+1|\0\b\204\177F\302\320CQ\311\377\344J\20k\374\0|\7\0J\20j\374F\302Bh\204\177L\337\1\17NuH\347<\0t\00068\tpx\0z\7a\6L\337\0<NuH\347\377\360\222@RA\351ISA6AH@B@\352\210\320\270\tH\320\274\0\340\0\0H\302H\303\324\200\326\200\b\202\0\0g\2PD\b\203\0\0g\2PE$B\226B\342K|\377><\200\0\350n\352gp\17\3008\t\2244<\314\0\1\2V\302H\202HB4<\252\0\1\2V\302H\202JCf2\314G\304FHB\304FHBFF J2\0130\20\300F\200B0\200A\350\0\200Q\311\377\362HB\325\374\0\2\0\0\265\374\0\344\0\0e\334`VSC(\2*\2\310F\312GHDHE\310F\312GHDHEFFFG\"J2\13 I0\20\300F\200D0\3000\3`\0020\302Q\310\377\3740\20\300G\200E0\300C\351\0\200Q\311\377\336HBHDHE\325\374\0\2\0\0\265\374\0\344\0\0e\304L\337\17\377Nu\0\6\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\6\0\0 \0p\0p\0p\0p\0 \0 \0 \0\0\0\0\0 \0\0\0\0\0\0\0\0\0\0\t\0\0f\0f\0\"\0\"\0D\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\t\0\0\22\0\22\0\22\0\177\0$\0$\0$\0\376\0H\0H\0H\0\0\0\0\0\0\0\0\0\0\b\20\0\20\0|\0\222\0\320\0x\0<\0\26\0\222\0|\0\20\0\20\0\0\0\0\0\0\0\0\0\0\n\0\0`\0\220\200\221\0\222\0d\0\b\0\23\0$\200D\200\204\200\3\0\0\0\0\0\0\0\0\0\0\n\0\0p\0\210\0\210\0\210\0P\0000\0I\0\205\0\202\0\205\0x\200\0\0\0\0\0\0\0\0\0\5\0\0`\0`\0 \0 \0@\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\5\0\0\20\0 \0@\0@\0\200\0\200\0\200\0\200\0\200\0@\0@\0 \0\20\0\0\0\0\0\0\5\0\0\200\0@\0 \0 \0\20\0\20\0\20\0\20\0\20\0 \0 \0@\0\200\0\0\0\0\0\0\b\0\0\0\0\0\0\0\0\20\0\222\0T\08\0T\0\222\0\20\0\0\0\0\0\0\0\0\0\0\0\0\b\0\0\0\0\0\0\0\0\20\0\20\0\20\0\376\0\20\0\20\0\20\0\0\0\0\0\0\0\0\0\0\0\0\5\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0`\0`\0 \0 \0@\0\0\0\0\6\0\0\0\0\0\0\0\0\0\0\0\0\0\0\370\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\6\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0000\0000\0\0\0\0\0\0\0\0\0\0\6\0\0\b\0\b\0\b\0\20\0\20\0 \0 \0 \0@\0@\0\200\0\200\0\200\0\0\0\0\0\0\t\0\0<\0f\0\303\0\303\0\303\0\303\0\303\0\303\0\303\0f\0<\0\0\0\0\0\0\0\0\0\0\t\0\0\b\0\30\08\0x\0\30\0\30\0\30\0\30\0\30\0\30\0~\0\0\0\0\0\0\0\0\0\0\t\0\0<\0f\0\303\0\3\0\6\0\f\0\30\0000\0`\0\300\0\377\0\0\0\0\0\0\0\0\0\0\t\0\0<\0f\0\303\0\3\0\6\0\34\0\6\0\3\0\303\0f\0<\0\0\0\0\0\0\0\0\0\0\t\0\0\2\0\6\0\16\0\36\0006\0f\0\306\0\377\0\6\0\6\0\6\0\0\0\0\0\0\0\0\0\0\t\0\0~\0`\0`\0`\0|\0\6\0\3\0\3\0\303\0f\0<\0\0\0\0\0\0\0\0\0\0\t\0\0\f\0\30\0000\0`\0|\0\346\0\303\0\303\0\303\0f\0<\0\0\0\0\0\0\0\0\0\0\t\0\0\377\0\303\0\306\0\6\0\f\0\f\0\f\0\30\0\30\0\30\0\30\0\0\0\0\0\0\0\0\0\0\t\0\0<\0f\0\303\0\303\0f\0<\0f\0\303\0\303\0f\0<\0\0\0\0\0\0\0\0\0\0\t\0\0<\0f\0\303\0\303\0\303\0g\0>\0\6\0\f\0\30\0000\0\0\0\0\0\0\0\0\0\0\5\0\0\0\0\0\0\0\0`\0`\0\0\0\0\0\0\0`\0`\0\0\0\0\0\0\0\0\0\0\0\0\5\0\0\0\0\0\0\0\0`\0`\0\0\0\0\0\0\0`\0`\0 \0 \0@\0\0\0\0\0\0\6\0\0\0\0\0\0\b\0\20\0 \0@\0\200\0@\0 \0\20\0\b\0\0\0\0\0\0\0\0\0\0\6\0\0\0\0\0\0\0\0\0\0\370\0\0\0\0\0\0\0\370\0\0\0\0\0\0\0\0\0\0\0\0\0\0\6\0\0\0\0\0\0\200\0@\0 \0\20\0\b\0\20\0 \0@\0\200\0\0\0\0\0\0\0\0\0\0\6\0\0\360\08\0\30\0\30\0\20\0 \0@\0@\0\0\0\0\0@\0\0\0\0\0\0\0\0\0\0\13\0\0\37\0 \200@@\216@\222@\222@\222@\235\200\200\0@\0?\200\0\0\0\0\0\0\0\0\0\13\0\0\f\0\f\0\26\0\26\0\26\0#\0#\0?\0A\200A\200\343\300\0\0\0\0\0\0\0\0\0\n\0\0\374\0g\0c\0c\0f\0~\0c\0a\200a\200c\0\374\0\0\0\0\0\0\0\0\0\0\n\0\0\36\200a\200\300\200\300\0\300\0\300\0\300\0\300\0\300\200a\0\36\0\0\0\0\0\0\0\0\0\0\n\0\0\374\0c\0a\200a\200a\200a\200a\200a\200a\200c\0\374\0\0\0\0\0\0\0\0\0\0\n\0\0\377\200a\200`\200`\0b\0~\0b\0`\0`\200a\200\377\200\0\0\0\0\0\0\0\0\0\n\0\0\377\200a\200`\200`\0b\0~\0b\0`\0`\0`\0\360\0\0\0\0\0\0\0\0\0\0\13\0\0\36\200a\200\300\200\300\0\300\0\300\0\303\300\300\200\300\200a\0\36\0\0\0\0\0\0\0\0\0\0\13\0\0\363\300a\200a\200a\200a\200\177\200a\200a\200a\200a\200\363\300\0\0\0\0\0\0\0\0\0\7\0\0\374\0000\0000\0000\0000\0000\0000\0000\0000\0000\0\374\0\0\0\0\0\0\0\0\0\0\b\0\0\36\0\f\0\f\0\f\0\f\0\f\0\f\0\f\0\f\0\f\0\f\0\314\0000\0\0\0\0\0\0\n\0\0\363\200a\0b\0d\0h\0|\0f\0f\0c\0c\0\367\200\0\0\0\0\0\0\0\0\0\n\0\0\360\0`\0`\0`\0`\0`\0`\0`\0`\200a\200\377\200\0\0\0\0\0\0\0\0\0\r\0\0\340p``p`p\340x\340Y`]`N`N`D`\344\360\0\0\0\0\0\0\0\0\0\f\0\0\300\340`@p@x@\\@N@G@C\300A\300@\300\340@\0\0\0\0\0\0\0\0\0\13\0\0\36\0a\200\300\300\300\300\300\300\300\300\300\300\300\300\300\300a\200\36\0\0\0\0\0\0\0\0\0\0\13\0\0\376\0a\200`\300`\300`\300a\200~\0`\0`\0`\0\360\0\0\0\0\0\0\0\0\0\0\13\0\0\36\0a\200\300\300\300\300\300\300\300\300\300\300\334\300\367\200c\0\37\0\1\200\0\300\0\0\0\0\0\13\0\0\376\0a\200`\300`\300`\300a\200~\0n\0g\0c\200\361\300\0\0\0\0\0\0\0\0\0\t\0\0\35\0c\0\301\0\340\0p\0<\0\16\0\7\0\203\0\306\0\270\0\0\0\0\0\0\0\0\0\0\13\0\0\377\300\314\300\214@\f\0\f\0\f\0\f\0\f\0\f\0\f\0?\0\0\0\0\0\0\0\0\0\0\13\0\0\361\300`\200`\200`\200`\200`\200`\200`\200`\2001\0\16\0\0\0\0\0\0\0\0\0\0\13\0\0\361\300`\200`\2001\0001\0001\0\32\0\32\0\32\0\f\0\f\0\0\0\0\0\0\0\0\0\0\20\0\0\361\216a\2041\3041\3042\310\32h\32h\32h\f0\f0\f0\0\0\0\0\0\0\0\0\0\13\0\0\361\300`\2001\0002\0\34\0\f\0\16\0\23\0#\0A\200\343\300\0\0\0\0\0\0\0\0\0\13\0\0\361\300`\2001\0001\0\32\0\32\0\f\0\f\0\f\0\f\0\36\0\0\0\0\0\0\0\0\0\0\13\0\0\377\300\300\300\201\200\3\0\6\0\f\0\30\0000\0`@\300\300\377\300\0\0\0\0\0\0\0\0\0\6\0\0p\0@\0@\0@\0@\0@\0@\0@\0@\0@\0@\0@\0p\0\0\0\0\0\0\b\0\0\202\0\202\0D\0D\0(\0(\0|\0\20\0|\0\20\0\20\0\0\0\0\0\0\0\0\0\0\6\0\0p\0\20\0\20\0\20\0\20\0\20\0\20\0\20\0\20\0\20\0\20\0\20\0p\0\0\0\0\0\0\b\0\0\20\0(\0D\0\202\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\6\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\370\0\0\0\0\0\0\5\0\0 \0@\0@\0`\0`\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\t\0\0\0\0\0\0\0\0|\0\316\0\6\0>\0f\0\306\0\316\0w\0\0\0\0\0\0\0\0\0\0\n\0\0\340\0`\0`\0n\0s\0a\200a\200a\200a\200s\0\336\0\0\0\0\0\0\0\0\0\0\b\0\0\0\0\0\0\0\0<\0f\0\300\0\300\0\300\0\300\0f\0<\0\0\0\0\0\0\0\0\0\0\n\0\0\7\0\3\0\3\0;\0g\0\303\0\303\0\303\0\303\0g\0;\200\0\0\0\0\0\0\0\0\0\b\0\0\0\0\0\0\0\0<\0f\0\302\0\376\0\300\0\300\0f\0<\0\0\0\0\0\0\0\0\0\0\7\0\08\0l\0`\0`\0\370\0`\0`\0`\0`\0`\0\360\0\0\0\0\0\0\0\0\0\0\t\0\0\0\0\0\0\0\0}\0\306\0\306\0\306\0|\0\300\0~\0\303\0\303\0\303\0~\0\0\0\0\n\0\0\340\0`\0`\0n\0w\0c\0c\0c\0c\0c\0\367\200\0\0\0\0\0\0\0\0\0\5\0\0`\0`\0\0\0\340\0`\0`\0`\0`\0`\0`\0\360\0\0\0\0\0\0\0\0\0\0\6\0\0\30\0\30\0\0\08\0\30\0\30\0\30\0\30\0\30\0\30\0\30\0\30\0\330\0p\0\0\0\0\t\0\0\340\0`\0`\0n\0d\0h\0p\0x\0l\0f\0\367\0\0\0\0\0\0\0\0\0\0\5\0\0\340\0`\0`\0`\0`\0`\0`\0`\0`\0`\0\360\0\0\0\0\0\0\0\0\0\0\17\0\0\0\0\0\0\0\0\336\360w\270c\30c\30c\30c\30c\30\367\274\0\0\0\0\0\0\0\0\0\n\0\0\0\0\0\0\0\0\356\0w\0c\0c\0c\0c\0c\0\367\200\0\0\0\0\0\0\0\0\0\t\0\0\0\0\0\0\0\0<\0f\0\303\0\303\0\303\0\303\0f\0<\0\0\0\0\0\0\0\0\0\0\t\0\0\0\0\0\0\0\0\356\0w\0c\0c\0c\0c\0w\0n\0`\0`\0\360\0\0\0\0\t\0\0\0\0\0\0\0\0w\0\356\0\306\0\306\0\306\0\306\0\356\0v\0\6\0\6\0\17\0\0\0\0\t\0\0\0\0\0\0\0\0\356\0s\0`\0`\0`\0`\0`\0\360\0\0\0\0\0\0\0\0\0\0\b\0\0\0\0\0\0\0\0z\0\306\0\302\0x\0<\0\206\0\306\0\274\0\0\0\0\0\0\0\0\0\0\7\0\0\0\0`\0`\0\370\0`\0`\0`\0`\0`\0l\08\0\0\0\0\0\0\0\0\0\0\n\0\0\0\0\0\0\0\0\347\0c\0c\0c\0c\0c\0g\0=\200\0\0\0\0\0\0\0\0\0\t\0\0\0\0\0\0\0\0\367\0b\0d\0004\08\08\0\20\0\20\0\0\0\0\0\0\0\0\0\0\16\0\0\0\0\0\0\0\0\3628c\20c\0205\2405\240\34\340\30\300\b@\0\0\0\0\0\0\0\0\0\n\0\0\0\0\0\0\0\0\367\0b\0004\0\30\0\34\0&\0C\0\347\200\0\0\0\0\0\0\0\0\0\n\0\0\0\0\0\0\0\0\363\200a\0a\0002\0002\0\34\0\34\0\b\0\b\0\20\0`\0\0\0\0\b\0\0\0\0\0\0\0\0\376\0\206\0\f\0\30\0000\0`\0\302\0\376\0\0\0\0\0\0\0\0\0\0\6\0\0\30\0 \0 \0 \0 \0@\0\200\0@\0 \0 \0 \0 \0\30\0\0\0\0\0\0\6\0\0 \0 \0 \0 \0 \0 \0 \0 \0 \0 \0 \0 \0 \0\0\0\0\0\0\6\0\0\300\0 \0 \0 \0 \0\20\0\b\0\20\0 \0 \0 \0 \0\300\0\0\0\0\0\0\6\0\0\370\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\b\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\6\0\0\200\0\200\0\200\0@\0@\0 \0 \0 \0\20\0\20\0\b\0\b\0\b\0\0\0\0\0\0\b\0\0`\0\222\0\f\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\6\0\0 \0 \0 \0 \0 \0\0\0\0\0\0\0 \0 \0 \0 \0 \0\0\0\0\0H\347\b\302O\357\374\0p\0\263\374\0\360\0\0e\b\263\374\0\377\377\377c:,I O8\3SD\20\331Q\314\377\374\"NN\271\0\377\317(J\0f\22p\0 O8\3SD\261\tV\314\377\374g\20p\376\"O N8\3SD\20\331Q\314\377\374O\357\4\0L\337C\20Nu08\f\270\300\374\0\3Nu08\f\270\300\374\0\6Nu:8\f\270\312\374\27pNu:8\f\270\312\374\0\226Nu88\f\270\310\374\1,Nu48\f\270\304\374\1,Nu48\f\270\304\374\1,Nu48\f\270\304\374\1,`\0\323<88\f\270\310\374\1,Nu:8\f\270\312\374\27pNu".getBytes (XEiJ.ISO_8859_1);
  public static final byte[] ROM16PAT_DATA = "\0\376\240\0\0\376\363\377\0\377\0000\0\377\0005 <\23\222\0\0\0\6 <\26%\b\3\0\377\0Z\0\377\0sa\0\f\342\0\0\0\6N\371\0\376\240H\0\377\0\216\0\377\0\223A\372\377p\0\0\0\6N\271\0\376\242\234\0\377\0\304\0\377\0\315 9\0\377\0\0\0\2`\b\0\377\0\316\0\377\1\21A\371\0\355\0\0\0\bN\271\0\376\243\302`<\0\377\1\324\0\377\1\331\b8\0\7\0\0\0\6N\271\0\376\244\246\0\377\1<\0\377\1\233\f8\0\1\0\0\0\bN\271\0\376\247\264`X\0\377\1\262\0\377\1\267B9\0\350\0\0\0\6\21|\0\0\300\1\0\377\2R\0\377\2\201r\23C\370\0\0\0\bN\271\0\376\251L`(\0\377\2\206\0\377\2\247B8\t\336\0\0\0\bN\271\0\376\251\254`\32\0\377\2\250\0\377\2\255r\2p\254\0\0\0\6N\271\0\376\251\364\0\377\2\262\0\377\2\271C\372\b\324\0\0\0\6N\271\0\376\252\22\0\377\0036\0\377\3;p\0N{\0\0\0\6N\271\0\376\307n\0\377\3\200\0\377\3\205p\0N{\0\0\0\6N\271\0\376\307n\0\377\7p\0\377\7ua\0\0\2\0\0\0\6N\271\0\376\252\230\0\377\7\276\0\377\7\313a(C\371\0\0\0\6N\271\0\376\253\0\0\377\16v\0\377\16\237H\347\360\340\0\0\0\6N\371\0\376\253<\0\377\16\300\0\377\16\307C\372\4\312\0\0\0\bC\371\0\377\326\200BB\0\377\20\330\0\377\21\217J9\0\355\0\0\0\212H\347\370\200\269\0\355\0\221gHr\bt\0aHA\372\0Lr x\33\24\30\262<\0(f\b\266<\0\2m\2\24\3a.P\1Q\314\377\352x\5\22\30\24\30a Q\314\377\370r\btha\26p\0000\30\320\200N\271\0\376\305\320t\0a\6L\337\1\37NuphNONu\313L\24\0\0\3\1\1\35\34(\2\6C\237_\213\202\206\225\304G\300\6\0042\"$\30\0\31\0\31\200\33\0\1\2\1\0\0\31\0\377\24X\0\377\24_A\372\363d\0\0\0\b!\374\0\376\271\232\0\270\0\377\27\210\0\377\27\215#z{\262\0\0\0\6N\271\0\376\272\376\0\377\27\326\0\377\27\333p\365r\0\0\0\0\6N\271\0\376\273\24\0\377;\374\0\377;\377H\347`\0\0\0\0\4H\347p\0\0\377<\26\0\377<\31L\337\0\6\0\0\0\4L\337\0\16\0\377<\34\0\377<\37L\337\0\6\0\0\0\4L\337\0\16\0\377<Z\0\377<]H\347`\0\0\0\0\4H\347p\0\0\377<v\0\377<yL\337\0\6\0\0\0\4L\337\0\16\0\377DD\0\377D\215?\0?<\0\0\0H?\0/<\0\270\0\20/8\13\374?<\0\2a\0#nBo\0\nRWa\0#dO\357\0\f0\37g\4a\0e\360L\337\177\376NuRA\262|\0 e\16\262x\trc\b28\trg\2SANu\0\377D\246\0\377D\247RA\351A\0\0\0\2a\314\0\377Z\250\0\377Z\251+\200\n\0\0\0\0\2;\200\0\377j\332\0\377k\27p\377H\347\0\0\0>H\347`\300$\1HB x\4XN\220 @p\377\261\374\0\360\0\0e\b\261\374\1\0\0\0e\26\260\374\fFg\20RARB\302\302SA\20\331Q\311\377\374p\0L\337\3\6Nu\0\377k\214\0\377k\221A\371\0\350\0\0\0\6N\371\0\376\273\204\0\377s\366\0\377s\373 <\0\373\0\0\0\6 <\0\376\364\0\0\377z\36\0\377z1\260<\0\t\0\0\0\24 A\302<\0\177\262<\0\13e\2r\4a\0\0\264\"\b\0\377z\342\0\377z\355\b!\4\20\0\0\0\f0<\b#\342hr\376\321ANu\0\377\202\204\0\377\202\245/\0\f8\0\0\0\6N\371\0\376\306\340\0\377\205\342\0\377\205\347\f\200\1\0\0\0\0\6N\271\0\376\305\272\0\377\253\244\0\377\253\247\262|\3\360\0\0\0\4\262|\4\0\0\377\253\264\0\377\253\267\264|\3\360\0\0\0\4\264|\4\0\0\377\254r\0\377\254\205\f8\0\1\0\0\0\nN\271\0\376\307x/\0`\n\0\377\254\350\0\377\254\365\f8\0\1\0\0\0\n$\37N\271\0\376\307\6`\4\0\377\263&\0\377\266aH\347`@\0\0\1pH\347\364\254*<\0\300\0\0E\371\0\350\200\0I\371\0\355\200\0r\0\228\t<\262|\0Je\f\22,\200\35\262|\0Je\2r\20K\371\0\376\276\372\b,\0\0\200\225g\6K\371\0\376\301J0\1\347H\332\300v\7\306-\0\0045|\0 \246\0\25C\200(5C\244\0\b\352\0\3\200( Ep\0t\0\7<\0\240g\2t\3r\377 \300 \300Q\311\377\372Q\312\377\364\b\252\0\3\200(A\370\t\\ \305 <\0\0\4\0\266|\0\4e\2\320@ \300p\17\7<\0\"g\4P\300`\b\7<\0\214g\2p\3770\2000\3a\0165|\0?\246\0p\0L\3375/NuH\347\370\300A\371\0\350 \0J@g\f\260|\0\4g\6\260|\0\6f\16C\372\0rp\7 \331Q\310\377\374``\260|\0\1g\6\260|\0\5f>\"<\0\22\0\22v\0t\bx\7 \3\300\274\373\377\373\377\320\202\300\274\377\337\377\337 \300\320\201 \300\320\201 \300\320\201 \300\324\274\1 \1 Q\314\377\332\326\274T\0T\0d\314`\26 <\0\1\0\1$<\2\2\2\2r\177 \300\320\202Q\311\377\372L\337\3\37Nu\0\0R\224\0 \0>\4\0\7\300\4 \7\376\200\0\370\0\200 \370>\204\0\377\300\255j\377\376\0\377\267@\0\377\267C\0260\0\0\0\0\0\4\0260\0\2\0\377\307Z\0\377\310\27H\347`\0\0\0\0\230N\371\0\376\306>\0\20\0$\0000\0@\0R\0f\0l\0\177Illegal instruction\0Zero divide\0CHK instruction\0TRAPV instruction\0Privilege violation\0Trace\0Line 1010 emulator\0Line 1111 emulator\0\0\377\314\236\0\377\314\26709\0\350\0\0\0\30p\22\300y\0\350\0(\260|\0\22f\6p\377X\217`\2p\0Nu\0\377\334\352\0\377\334\355\260|\377\377\0\0\0\4\262|\377\377\0\377\314\300\0\377\314\305I\371\0\377\0\0\0\6I\371\0\377\3252\0\377\315R\0\377\315WI\371\0\377\0\0\0\6I\371\0\377\325>\0\377\315`\0\377\315eI\371\0\377\0\0\0\6I\371\0\377\325b\0\377\315\206\0\377\315\213C\371\0\377\0\0\0\6C\371\0\377\325D\0\377\315\226\0\377\315\233C\371\0\377\0\0\0\6C\371\0\377\325D\0\377\315\312\0\377\315\317I\371\0\377\0\0\0\6I\371\0\377\325>\0\377\316*\0\377\316/I\371\0\377\0\0\0\6I\371\0\377\325\16\0\377\316\262\0\377\316\267I\371\0\377\0\0\0\6I\371\0\377\325,\0\377\317V\0\377\317[I\371\0\377\0\0\0\6I\371\0\377\325&\0\377\317\250\0\377\317\255I\371\0\377\0\0\0\6I\371\0\377\3258\0\377\317\330\0\377\317\335I\371\0\377\0\0\0\6I\371\0\377\325 \0\377\320\0\0\377\320\5I\371\0\377\0\0\0\6I\371\0\377\325\32\0\377\320<\0\377\320AC\371\0\377\0\0\0\6C\371\0\377\325X\0\377\320H\0\377\320MC\371\0\377\0\0\0\6C\371\0\377\325l\0\377\320T\0\377\320YC\371\0\377\0\0\0\6C\371\0\377\325D\0\377\320t\0\377\320yC\371\0\377\0\0\0\6C\371\0\377\325D\0\377\320\264\0\377\320\271I\371\0\377\0\0\0\6I\371\0\377\325>\0\377\321\0\0\377\321\5C\371\0\377\0\0\0\6C\371\0\377\325b\0\377\321\22\0\377\321\27C\371\0\377\0\0\0\6C\371\0\377\325v\0\377\321$\0\377\321)C\371\0\377\0\0\0\6C\371\0\377\325N\0\377\3210\0\377\3215I\371\0\377\0\0\0\6I\371\0\377\325>\0\377\321J\0\377\321OI\371\0\377\0\0\0\6I\371\0\377\3252\0\377\324x\0\377\324}I\371\0\377\0\0\0\6I\371\0\377\325\24\0\377\315\32\0\377\315AO\357\374\0\0\0\0\6N\371\0\376\362\366\0\377\321\246\0\377\321\253 <\0\0\0\0\0\00608\f\270\344H\0\377\322&\0\377\322+ <\0\0\0\0\0\6N\271\0\376\363T\0\377\3226\0\377\3229Q\310\377\364\0\0\0\4S\200d\362\0\377\322f\0\377\322k <\0\0\0\0\0\6N\271\0\376\363^\0\377\322\226\0\377\322\233*<\0L\0\0\0\6N\271\0\376\363h\0\377\322\252\0\377\322\257*<\0\1\0\0\0\6N\271\0\376\363r\0\377\323@\0\377\323E(<\0\3\0\0\0\6N\271\0\376\363|\0\377\323\310\0\377\323\315$<\0\3\0\0\0\6N\271\0\376\363\206\0\377\324\26\0\377\324\33$<\0\3\0\0\0\6N\271\0\376\363\220\0\377\324B\0\377\324G$<\0\3\0\0\0\6N\271\0\376\363\232\0\377\324\242\0\377\324\247(<\0\3\0\0\0\6N\271\0\376\363\246\0\377\324\204\0\377\324\211*<\0L\0\0\0\6N\271\0\376\363\260\0\377\207\4\0\377\207\7g\0\t\262\0\0\0\4g\0Nz\0\377\207J\0\377\207Mg\0\n\234\0\0\0\4g\0N>\0\377\210L\0\377\210Og\0\n\2\0\0\0\4g\0MF\0\377\210\220\0\377\210\223g\0\bD\0\0\0\4g\0M\f\0\377\210\320\0\377\210\323g\0\b\260\0\0\0\4g\0L\326\0\377\211P\0\377\211Sg\0\b*\0\0\0\4g\0L`\0\377\211\306\0\377\211\311g\0\6\366\0\0\0\4g\0K\364\0\377\212\226\0\377\212\231g\0\6,\0\0\0\4g\0K.\0\377\213.\0\377\2131g\0\5\232\0\0\0\4g\0J\240\0\377\213\200\0\377\213\203g\0\5N\0\0\0\4g\0JX\0\377\215L\0\377\215Og\0\3\306\0\0\0\4g\0H\226\0\377\325\200\0\377\326\177\20.\0\t\0\0\0rJ8\f\355fh`\0\3670J8\f\355f^`\0\367JJ8\f\355fT`\0\367\246J8\f\355fJ`\0\370|J8\f\355f@`\0\370\226J8\f\355f6`\0\3710J8\f\355f,`\0\371\312J8\f\355f\"`\0\371\322J8\f\355f\30`\0\371\370J8\f\355f\16`\0\372\26J8\f\355f\4`\0\372\346p\377Nu\0\0\0\0".getBytes (XEiJ.ISO_8859_1);

  //perl misc/pattobytes.pl HU260PAT misc/hu260pat.x
  public static final byte[] HU260PAT_TEXT = "H\347x\3404\0O\357\377\250/\17/\b\377)P\217J\200k\0\0\336 /\0J\341\210\200\274    \260\274x   f\0\0\310 /\0D\"/\0H\20/\0C\22/\0G\340\230\340\231\200\274    \202\274    \260\274iocsf\16\262\274    f\6p\0`\0\0\256\302\274\377   \260\274floaf\0\0\202\262\274t   fx y\0\377\0\20 (\0\f\320\250\0\20\"(\0\24$\0\324\201&\t\226\250\0\4((\0\30$IA\350\0@`\2\"\330Y\200d\372X@`\2\22\330Q\310\377\374p\0`\2\"\300Y\201d\372XA`\2\22\300Q\311\377\374J\203g$\330\210`\0340\30k\34\260|\0\1f\4 \30k\22\b\200\0\0\325\300f\4\327\222`\2\327R\261\304e\340 \2`\0340\2\20<\0\3\321\374\3\0\0\0/8\34\0/\t/\b?\0\377KO\357\0\16O\357\0XL\337\7\36J\200Nu".getBytes (XEiJ.ISO_8859_1);
  public static final byte[] HU260PAT_DATA = "\0\374\374\226\0\374\377\377\0\374 \6\0\374 \t\0\0003\352\0\0\0\4\0\0005\6\0\374 4\0\374 7\1\33*\33\0\0\0\4\0\33*\33\0\374 Z\0\374 ]3.02\0\0\0\0042.60\0\374)\214\0\374)\221\200<\0\3\0\0\0\6N\371\0\1Dz\0\3741\232\0\3741\241!\374\0\1\0\0\0\b!\374\0\1E\226\34$\0\374;\226\0\374;\231\0\1@z\0\0\0\4\0\1E\226\0\374\\\316\0\374\\\3210<\3\2\0\0\0\0040<\2<\0\0\0\0".getBytes (XEiJ.ISO_8859_1);

  public static final byte[] ROMDB_DIFF = "\0\375X\0\0\1E\364\0\0\0//\bA\371\0\0\20\0000<\3\377BXQ\310\377\374A\371\0\0(\0000<\7\377BXQ\310\377\374 _M\371\0\0\20\0-O\1r\0\0\0P\0\2\0\5\0\0\26\222\0\b\0\5\0\0\26\220\0\n\0\5\0\0\26\222\0\0\0\30\0\24\0\3\5\234\0\20\0\3-|\0\2\0\35\24\216\0FJn\6\220f\n-|\0\375\265\206\1v`\b-|\0\376D\340\1v\0\6\0\3NH\0\2\0\5\0\0*\212\0 \0\5\0\0\20\0\0\0\0\6\0\6\0\3\6\220\0\32\0\3~\23\0(\0\5\0\0-\16\0\24\0\3\3\b\0\6\0\3\6\210\0\2\0\3\32L\0\2\0\3\32N\0\2\0\3\32R\0\2\0\3\32T\0\2\0\3\32X\0\2\0\3\6\220\0\2\0\7N\271\0\376|\212\0\0\0\2\0\2\0\r\0\0000\0\6\350N\271\0\376\177\306\0\0\0*\0\4\0\3\6\354\0\2\0\3\6\222\0\6\0\5\0\0(\260\0\4\0\5\0\0\26\264\0\4\0\3\6\364\0\4\0\3\6\366\0\4\0\3\6\370\0\4\0\3\6\372\0\2\0\3\32@\0\2\0\3\32B\0\2\0\5\0\0*\212\0\2\0\5\0\0*\214\0\2\0\3\3\n\0\0\0\6\0\0\0\33=|\0\1\5\2403\374\377\377\0\0*\212A\371\0\375ZvN\271\0\375\300>\0\2\0s\0\375f@N\271\0\375f\204Nu\r\nROM Debugger version 3.60 (2025-03-23)\r\nCopyright 1987-1993 SHARP/Hudson, 2021-2025 Makoto Kamada\r\n\0\0\0\1x\0\2\0\5\0\08\0\0\2\0\5\0\0\20\0\0\16\0\3\5\240\0\0\0\f\0\2\0\5\0\0\20\0\0\2\0\5\0\0*\214\0\2\0\5\0\0*\212\0\2\0\3\5\242\0\2\0\3\32@\0\2\0\3\32B\0\2\0\5\0\08\0\0\2\0\3\6\220\0\2\0\3N\271\0\2\0\3|l\0\0\0\4\0\0\0\7N\271\0\376y\212\0\0\0\4\0\2\0\5\0\0\20\0\0\22\0\210\0t\0\r\0\0\0\0\0\0\0\0\0\0\0\0\0\22\0\r\0\0\0\0\0\0\0\0\0\0\0\0\0 \0\3\3.\0\322\0\5\0\0*|\0\2\0\5\0\0*z\0\4\0\3\7\374\0\22\0\3\7\374\0\4\0\3\7\374\0:\0\3\7\374\0\2\0\5\0\0*z\0$\0\3\5\242\0H\0\5\0\0*|\0\"\0,\0\2\0\2\0\22\0\256\0\4\0\3\3\f\0\16\0\5\0\0*\270\0~\0\5\0\0*\270\0\"\0h\0$\0\3\3\n\0\34\0\3\2\340\0z\0\3\2\340\0\26\0\3\1\322\0\20\0\3\1\322\0\20\0\3\1\322\0\22\0\3\1\322\0004\0\21p\1J8\t\336k\2D\0\21\300\t\336Nu\0\0\0$\0\4\0\3\1\322\0\26\0\3\1\322\0\20\0\3\1\322\0*\0\3\1\322\0\30\0\3\1\322\0 \0\3\1\322\0\26\0\3\1\322\0\20\0\3\1\322\0.\0\3\1\322\0*\0\3\1\322\0006\0\3\1\322\0\242\0\3\3.\0\2\0\5`\0\370\346\0\0\0\304\0\4\0\3N\371\0\2\0\3\201\b\0\0\0>\0\0\0\3N\371\0\2\0\3\201\312\0\0\0(\0\22\0\3Sn\0000\0\13*n\0006Jn\0\36f\6\0\20\0\3\4\\\0\n\0\3\4[\0\\\0\3\6\364\0\234\0\3\200\316\0\240\0\t\0\376\224p\0\376\224\330\0\f\0\5\0\376\224\360\0\34\0\21\0\376\225d\0\376\225\332\0\376\225\372\0\376\223n\0\6\0\3\4\\\0(\0\3\263\225\0\"\0\3\6\300\0\220\0\3\6\300\0\n\0\5\0\376\231T\0\4\0\3p\23\0B\0N\0\n\0\3\6\302\0\2\0\3\6\304\0\2\0\3\6\306\1@\0\3\6\302\0\f\0\3\6\302\0\16\0\3\6\302\1\202\0\3\6\304\0\2\0\3\6\310\0\260\0\3\6\310\0\2\0\3\6\304\0\22\0\3\6\304\0n\0\3\6\306\0\312\0\3\6\302\0\f\0\3\6\302\0\16\0\3\6\302\0002\0\3\6\306\0\34\0\3\6\306\0\2\0\3\6\314\1>\0\3\6\364\0`\0\3\30`\0\2\0\3\30\0\0\36\0\3\30\0\0\16\0\3\30\0\0000\0\3\30\0\3D\0\3\6\302\0&\0\3\6\302\0 \0\3\6\304\0\24\0\3\6\304\0\b\0\3\6\310\0\"\0\3\6\310\0\32\0\3\6\302\0 \0\3\6\304\0\20\0\3\6\306\0\30\0\3\6\304\0\b\0\3\6\310\0\"\0\3\6\310\0\4\0\3\6\306\0\b\0\3\6\314\0\36\0\3\6\314\0\32\0\3\6\302\0 \0\3\6\304\0\20\0\3\6\306\0\16\0\3\6\304\0\b\0\3\6\310\0\"\0\3\6\310\0\4\0\3\6\306\0\b\0\3\6\314\0\36\0\3\6\314\0\200\0\3\6\364\0\30\0\3\30\0\0\16\0\3\30\0\0\6\0\3\30\0\0\16\0\3\30\0\0\16\0\3\30\0\bt\0\35p\177\262\200o\4a\0\363\232p\200\262\200l\4a\0\363\220\35A\1+a\0\360\n\0\2\0\21\3700`\0\363FJEg\b\272|\0\2f\0\1\36\0\3\0\0\3V\0\5\0\376\231\254\0\4\0\3\0\21\0\16\0 \1B\0\25a\0\0\276\f\0\0{f\0\3420a\0\0\254\300<\0\337\0F\0\7a^\300<\0\337\0@\0!p \262\200b\0\341\224f\2BA _\203Pa\16\f\0\0}f\0\341\202N\371\0\376?D\0\4\0\3?V\0\230\0\3\20\31\0\6\0\3\260\30\1\374\0\3\6\364\0H\0\0314\37*_?\0Bn\6\364a\0\340\0160\37\fB\0\1f\6S@\0J\0\3\6\364\0\246\0\3\6\364\0p\0\3\201,\2\316\0\3\353@\0D\0\3Y\201\t\264\0\3Y\201\0$\0\5`\0\356\270\0\0\0(\31z\1\r FDABS\0004\0\\FDADD\0004\0fFDDIV\0004\0dFDMOVE\0004\0DFDMUL\0004\0gFDNEG\0004\0^FDSQRT\0004\0EFDSUB\0004\0lFSABS\0004\0XFSADD\0004\0bFSDIV\0004\0`FSMOVE\0004\0@FSMUL\0004\0cFSNEG\0004\0ZFSSQRT\0004\0AFSSUB\0004\0hCINVL\0M\364\bCINVP\0M\364\20CINVA\0M\364\30CPUSHL\0M\364(CPUSHP\0M\3640CPUSHA\0M\3648PFLUSHN\0N\365\0PFLUSHAN\0N\365\20PLPAW\0N\365\210PLPAR\0N\365\310MOVE16\0O\366\0LPSTOP\0O\370\0\0\0\6\0\3\6\364\0000\0\3\6\364\0\f\0\3\30\0\0\22\0\3\30\0\0\b\0\3\6\364\0\b\0\3\6\364\0\f\0\3\30\0\0\2\0\3\30\4\0\22\0\3\30\0\0\2\0\3\30\4\0\b\0\3\6\364\0\b\0\3\6\364\0\16\0\3\30\0\0\2\0\3\30\4\0\2\0\3\30\b\0\22\0\3\30\0\0\2\0\3\30\4\0\2\0\3\30\b\0\b\0\3\6\364\0\b\0\3\6\364\0\f\0\3\30\0\0\2\0\3\30\4\0\2\0\3\30\b\0\22\0\3\30\0\0\2\0\3\30\4\0\2\0\3\30\b\0\b\0\3\6\364\0\346\0\5\0\0\20\0\0\n\0\r\f\257\0\375X\0\0Je6\f\257\0\2\0/\235\364\0Jd,L\337\177\377N\271\0\375\272(N\271\0\375\272( WN\271\0\375\300>A\371\0\375\270\bN\271\0\375\300>`\0\1T\0\0\0\4\0\b\0\5\0\0\20\0\0\272\0\7N\271\0\376\232P\0p\0\3\3$\0\4\0\3\3$\0\b\0\3\3$\0\24\0\5\0\0\20\0\0\32\0\20\1*\0\3\4\\\0\32\0\3\0+\0\6\0\3\3.\0\32\0\"\0\232\0\16\0\4\0\f\0\26\0\f\0&\0\16\0002\0\b\0\16\0\4\0\32\0\3\4\300\0\332\2\20\0\2\0\357J8\t\336j\0\374@p\0NOJ\0g\370\300\274\0\0\0\377NuJ8\t\336j\0\373\200Y\217\378\t\223g\4p\256NO\378\t\222f\4p\36NOa\0\373f/@\0\4J\37f\4p\37NOJ\37g\4p\257NO \37NuJ8\t\336j\0\373\376a@\260<\0\3g\0\372\300\260<\0\33g\0\372\270`\4p\0NOp\1NOJ\200g\4J\0g\360\260<\0\3g\6\260<\0\33f\bp\0NO`\0\372\222NuJ8\t\336j\0\373\336/\1r\0\22\0p NO\262<\0\rf\6Bn\0\"`\n\262<\0\ng\4Rn\0\"\"\37`\4p\0NOp\1NOJ\200g J\0g\360\260<\0\23g\4p\0`\22p\0NOp\0NOJ\0g\370\300\274\0\0\0\377Nu\0\0\1\256\0\2\0\3\375b\0\6\0\3\372R\0\6\0\3\375j\0\6\0\3\375\234\0\6\0\3\375\326\0\6\0\3\372 \0\360\0\3\30\260\0\f\0\3\30\274\0\f\0\3\30\310\0\f\0\3\30\324\0\f\0\3\30\340\0\f\0\3\30\354\0\f\0\3\30\370\0\f\0\3\31\4\0(\0\3\6\230\0006\0\3\6\240\0\f\0\3\6\244\0\f\0\3\6\250\0\f\0\3\6\254\0\f\0\3\6\260\0\f\0\3\6\264\0\f\0\3\6\270\0\f\0\3\6\274\0\32\0\3\2\340\0\f\0\3\2\344\0\f\0\3\2\350\0\f\0\3\2\354\0\f\0\3\2\360\0\f\0\3\2\364\0\f\0\3\2\370\0\f\0\3\2\374\0\f\0\3\3\0\0\f\0\3\3\4\1H\0\5\0\376\211\324\0,\0\3\f\21\0N\0\5\0\376\206\232\0^\0\5\0\376\206\232\0x\0\3\6\220\0\f\0\3\6\230\0\210\0\3\4\\\0\2\0\3\3(\0\2\0\3\3&\0\30\0\3\328\0\32\0\3\3&\0\22\0\3\4\\\0,\0\3\328\0~\0\3\6\366\0\20\0\3\6\374\0\2\0\3\7|\0\32\0\3\6\366\0\26\0\3\6\366\0\4\0\3\30`\0\16\0\3\6\366\1R\0\7N\371\0\376\202Z\0\0\0Z\0>\0\5\0\376\206\232\0\20\0\3p=\0\232\0\7N\271\0\376\213,\0\0\0\2\0>\0\3\6\220\0\f\0\3\6\230\0\26\0\3\6\372\0006\0\3\272\350\2\0\0\3\6\220\0\4\0\3\6\222\0p\0\3\3$\0\4\0\3\3&\0 \0\3\3(\1@\0\3\3$\0R\0\3\6\366\08\0\3\6\220\0&\0\3\6\222\0\30\0\3\6\220\0\34\0\3\6\370\0<\0\3\6\372\0@\0\3\6\372\0\"\0\3\30\0\0&\0\3\30`\0\34\0\3\30`\0\b\0\3\6\374\0\2\0\3\7|\0x\0\3\32Z\0\b\0\3\32Z\1,\0\5\0\0*|\1\26\0\3\7\376\0\4\0\3\7\376\0&\0\3\7\376\0 \0\3\7\376\0\6\0\3\7\376\0022\0\3\3&\0\b\0\3\4\\\0\32\0\3\4\\\0F\0\5\0\376\227\30\1\202\0\3\6\224\0\26\0\3\6\224\0\b\0\3\1\24\0\f\0\3\214\262\0\250\0\3\3$\0\b\0\3\6\224\0\210\0\3\3&\0\6\0\3\3&\0\4\0\3\3(\0(\0\3\6\220\0\f\0\3\6\230\0\4\0\3\6\234\0\20\0\3\3&\0\2\0\31\30;@\fH\204\320\304-H\3(Nu\376\377\376\374\370\374\370\364\364\0\0\2\0\3\3&\0\6\0\3\3(\0\4\0\3\3&\0\0\0\b\0\26\0\3\3(\0\4\0\3\3&\0\f\0\7N\271\0\376\235\250\0\22\0\3\3(\0\"\0\3\3&\0$\0\7N\271\0\376\235\250\0\22\0\3\3(\0\36\0\3\3&\0\f\0\7N\271\0\376\235\250\0*\0\3\3(\0\32\0\3\3&\0\6\0\3\3(\0\4\0\3\3&\0\2\0\3\3(\0\b\0\3\3&\0\6\0\3\3(\0\4\0\3\3&\0\0\0\b\0\24\0\3\3(\0\4\0\3\3&\0\n\0\7N\271\0\376\235\250\0\22\0\3\3(\0\"\0\3\3&\0\"\0\7N\271\0\376\235\250\0\22\0\3\3(\0\36\0\3\3&\0\32\0\23N\271\0\376\235\250a\0\1\304f\0\3752p\3\300U\0\f\0\3\3(\0\"\0\3\343\250\0\6\0\5\370\0\355X\1$\0\3\6\220\0\f\0\3\6\232\0\6\0\3\6\236\0\36\0\3\6\220\0\f\0\3\6\230\0\4\0\3\6\234\0\34\0\3\328\0(\0\3\328\0\22\0\3\0d\0\n\0\3\0a\0\322\0\3\0z\0\20\0\5\26\374\0z\0\0\0\2\0\206\0\3\0z\0\310\0\3\0z\0\20\0\5\26\374\0z\1\306\0\3\0z\0\2\0\3\0p\0\2\0\3\0c\0\f\0\5\26\374\0z\0\0\0\2\0\276\0\3\0z\0\2\0\3\0p\0\2\0\3\0c\0\370\0\3\0z\0\2\0\3\0p\0\2\0\3\0c\0\16\0\3\0z\0\372\0\3\6\366\0\20\0\3\6\374\0\2\0\3\7|\0\32\0\3\6\366\0\26\0\3\6\366\0\4\0\3\30`\0\16\0\3\6\366\0\276\0\3\0e\0\244\0\13ccr\0sr\0usp\0\2\0\3pc\0\4\0\3\0d\0\22\0\3\0a\0\4\0\3\0a\0(\0\3\0d\6\316\0\7WCf\32z\2\0\30\0\3XC\0006\0\7WCf\34z\2\0\34\0\3XC\0\20\0\7WCf\34z\2\0\36\0\3XE\1\340\0\5\0\376\231\320\1\360\0\3\3&\5\332\0\3\6\320\0\30\0\3\6\320\0T\0\3\0c\0\2\0\3\0p\2\244\0\3z\3\0.\0\3\0t\0\2\0\3\0t\0\n\0\3\0t\0\2\0\3\0t\0\26\0\3\0t\0\2\0\3\0t\0\20\0\3\0t\0\2\0\3\0t\0F\0\3\0v\0\2\0\3\0a\0\2\0\3\0l\0\204\0\3\0s\0\2\0\3\0f\0\2\0\3\0c\0(\0\3\0d\0\2\0\3\0f\0\2\0\3\0c\1(\0001tc\0drp\0srp\0crp\0cal\0val\0scc\0ac\0pcsr\0mmusr\0tt0\0tt1\0r\0\3\0b\0\2\0\3\0a\0\b\0\3\0d\0\4\0\3\0c\0\30\0\3\0b\0\2\0\3\0a\0\b\0\3\0d\0\4\0\3\0c\0B\0\3\0p\0\2\0\3\0c\0\2\0\3\0s\0\2\0\3\0r\0\6\0\3\0m\0\2\0\3\0m\0\2\0\3\0u\0\2\0\3\0s\0\2\0\3\0r\0\22\0\3\0p\0\2\0\3\0c\0\2\0\3\0s\0\2\0\3\0r\0\f\0\3\0m\0\2\0\3\0m\0\2\0\3\0u\0\2\0\3\0s\0\2\0\3\0r\1\234\0\3U\201\0\350\0\3\0f\0\2\0\3\0p\0p\0\21fpiar\0fpsr\0fpcr\0\0 \0\3\0f\0\2\0\3\0p\0*\0\3\0f\0\2\0\3\0p\0\b\0\3\0f\0\2\0\3\0p\1>\0\7N\271\0\376\227\2\0\0\0\2\0\304\0\3\0\200\0\244\0\3\0\200\3*\0\3U\201\2h\0\3\1\322\0\2\0\3\5\242\0\20\0\3\5\242\0\n\0\3\1\322\0\22\0\3\1\322\0\30\0\3\5\242\0X\0\3\3\n\0\b\0\3\1\322\0\206\0\3\5\242\0\4\0\3\3\n\0\270\0\3\5\240\0\264\0\n\0\16\0\5\0\0\26\224\0\2\0\5\0\0\26\220\0\2\0\7N\271\0\376y\212\0\0\0\4\0\0\0\3N\271\0\2\0\3z\304\0\0\0006\0\2\0\5\0\0\26\222\0\6\0\5\0\0(\260\0\4\0\5\0\0\26\264\0\2\0\5\0\0\26\224\0\2\0\5\0\0\26\220\0\36\0\5\0\0*\212\0\4\0\3\1\324\0\6\0\3\1\324\0\2\0\3\6\220\0\22\0\3\32@\0\226\0\5\0\0*\212\0\30\0\3\32L\0\20\0\3\1\324\0\260\0\5\0\0*\212\0h\0\n\0\16\0\5\0\0\26\224\0\2\0\5\0\0\26\220\0\2\0\7N\271\0\376y\212\0\0\0\4\0\0\0\3N\271\0\2\0\3z\304\0\0\0006\0\2\0\5\0\0\26\222\0\6\0\5\0\0(\260\0\4\0\5\0\0\26\264\0\2\0\5\0\0\26\224\0\f\0\5\0\0\26\220\0\26\0\3\32@\0 \0\3\235\364\0\20\0\5\0\0*\214\0\6\0\5\0\0*\212\0\4\0\5\0\0\20\0\0\b\0\5\0\0\26\224\0\2\0\5\0\0\26\220\0\2\0\7N\271\0\376|\246\0\0\0006\0\0\0\7N\271\0\376y\212\0\0\0\4\0\2\0\5\0\0\26\222\0\6\0\5\0\0\26\264\0\4\0\5\0\0(\260\0\2\0\5\0\0\26\224\0\16\0\5\0\0\26\220\0\16\0\20\0\16\0\3\32L\0`\0\3\32L\0$\0\3p\22\0<\0\b\0&\0\b\0D\0\b\0\22\0\b\0\20\0\3\32R\0000\0\3\0\373\0\6\0\3\32R\0006\0\3\235\364\0\20\0\5\0\0*\212\0\b\0\5\0\0\20\0\0\f\0\5\0\0\26\224\0\2\0\5\0\0\26\220\0\2\0\7N\271\0\376|\246\0\0\0006\0\0\0\7N\271\0\376y\212\0\0\0\4\0\2\0\5\0\0\26\222\0\6\0\5\0\0\26\264\0\4\0\5\0\0(\260\0\2\0\5\0\0\26\224\0\b\0\5\0\0\26\220\0\22\0\20\0p\0\n\0\2\0\5\0\0\26\220\0\24\0\5\0\0\26\224\0\2\0\5\0\0\26\220\0\2\0\7N\271\0\376y\212\0\0\0\4\0\0\0\3N\271\0\2\0\3z\304\0\0\0006\0\2\0\5\0\0\26\222\0\6\0\5\0\0(\260\0\4\0\5\0\0\26\264\0\2\0\5\0\0\26\224\0\n\0\5\0\0*@\0\16\0\5\0\0*\212\0\4\0\5\0\0*\212\0\6\0\5\0\0\20\0\0\"\0\5\0\0*\212\0\30\0\3\235\364\0\22\0\5\0\0*\212\0\b\0\5\0\0*\214\0\4\0\5\0\0*\214\0\2\0\5\0\0*\212\0\4\0\5\0\0\20\0\0\f\0\5\0\0\26\224\0\2\0\5\0\0\26\220\0\2\0\7N\271\0\376|\246\0\0\0006\0\0\0\7N\271\0\376y\212\0\0\0\4\0\2\0\5\0\0\26\222\0\6\0\5\0\0\26\264\0\4\0\5\0\0(\260\0\2\0\5\0\0\26\224\0\n\0\5\0\0\26\220\0 \0\5\0\0*\214\0\2\0\5\0\0*\212\0\4\0\5\0\0\20\0\0\f\0\5\0\0\26\224\0\2\0\5\0\0\26\220\0\2\0\7N\271\0\376|\246\0\0\0006\0\0\0\7N\271\0\376y\212\0\0\0\4\0\2\0\5\0\0\26\222\0\6\0\5\0\0\26\264\0\4\0\5\0\0(\260\0\2\0\5\0\0\26\224\0\n\0\5\0\0\26\220\0\32\0\20\0>\0\3\32X\08\0\3\32T\0\30\0\3\32T\0b\0\b\0>\0\3\32N\0\4\0\3\32X\0\6\0\3\32X\0\6\0\3\32N\0\4\0\3\32R\0006\0\5\0\0*\212\0006\0\3\5\240\0\26\0\5\0\0\20\0\0`\0\3\1\326\0\22\0\3\1\326\1\254\0\3\1\326\0\\\0\5\0\0*\270\0D\0\5\0\376\211\324\0\2\0\7N\271\0\376\235\274\0\0\0\32\0\16\0\5\0\376\206\232\0\30\0\7N\371\0\376\213\252\0\0\0\2\0:\0\3\6\220\0\f\0\3\6\230\08\0\3\1\326\1\314\0\5\0\0*\270\0000\0\3\6\224\0\6\0\3\0324\0\274\0\3\1\326\0>\0\3\6\224\0\6\0\3\0324\0\2\0\3\328\0\2\0\3\32D\0\30\0\3\6\224\0\6\0\3\0324\0\b\0\3\6\224\0\6\0\3\0324\0\6\0\3\328\0\4\0\3\32D\0\6\0\3\6\224\0\6\0\3\0324\0\6\0\3\328\0\4\0\3\32D\0\254\5N\0\30\0\3~\23\0\"\0\3~\23\1\4\0\3\3\b\1\214\0\3,\231\0\2\0\3,\300\0\2\0\3,\343\0\2\0\3-\32\0\2\0\3-=\0\2\0\3-Y\0\2\0\3-u\0\2\0\3-\230\0\2\0\3-\305\0\2\0\3-\367\0\2\0\3.\35\0\2\0\3.E\0\2\0\3.j\0\2\0\3.\251\0\2\0\3.\322\0\6\0\3.\373\0\2\0\3/$\0\2\0\3/\213\0\2\0\3/\254\0\2\0\3/\314\0\2\0\3/\356\0\2\0\0030\5\0\2\0\00301\0\2\0\0030h\0\2\0\0030\203\0\2\0\0030\250\0\2\0\0034\13\0\2\0\00345\0\2\0\0030\321\0\2\0\0030\356\0\2\0\0031\26\0\2\0\0031L\0\2\0\0031g\0\2\0\0034g\0\2\0\0034\210\0\2\0\0034\252\0\2\0\0034\317\0\2\0\0034\371\0\6\0\0035\34\0\2\0\0031\206\0\2\0\0031\227\0\2\0\0031\262\0\2\0\0031\324\0\2\0\0031\353\0\2\0\0032\3\0\2\0\0035?\0\2\0\3/=\0\2\0\3/]\0\2\0\3/g\0\2\0\3/\200\0\2\0\0032\344\0\2\0\0033\26\0\2\0\0033:\0\2\0\0033\177\0\2\0\0073\277\0\0\0\0\0\0\0\30\t\374\0\3\1\322\0\20\0\3\1\322\0\22\0\3\1\322\0$\0\3\4\\\0\f\0\3\1\322\0\256\0\3\4\\\0\34\0\3\1\322\0000\0\3\235\346\0*\0\3\235\346\0 \0\3\235\346\0\n\0\3\1\322\0\4\0\3\1\322\1\0\0\3\5\244\0J\0\3\0\376\0\16\0\5\0\0*t\0\4\0\3\1\322\0(\0\3\1\322\0\22\0\3\1\322\0\6\0\3\32^\0\f\0\3\32^\0006\0\5\0\0*t\0\20\0\5\0\0*v\0\f\0\3\5\244\0002\0\5\0\0*|\0$\0\3\5\244\0\4\0\5\0\0*p\0\2\0\5\0\0*b\0\16\0\3\1\322\0000\0\5\0\0*z\0\f\0\5\0\0*v\0\6\0\5\0\0*p\0T\0\5\0\0*p\0,\0\5Bn\32\266\0\0\0\2\0\b\0\3\5\244\0\6\0\5Rn\32\266\0\0\0\2\0\340\0\3\3&\0\2\0\3\3&\0(\0\7Jn\32\266f\16\0\0\0\2\0V\0\3\7\376\0\4\0\3\7\376\0*\0\3\7\376\0\f\0\3\5\244\0004\0\3\5\244\0\22\0\3\7\376\0\b\0\3\5\244\0\30\0\3\0\376\0\264\0\3\1\324\0\4\0\3\1\322\0\20\0\3\1\324\0\20\0\3\1\322\0\22\0\3\1\322\0\f\0\3\1\322\0L\0\3\1\322\0\6\0\3\1\322\0 \0\3\1\324\0\n\0\3\1\322\0\6\0\3\1\322\0n\0\3\4\\\0\32\0\3\1\322\0\6\0\3\1\322\0\22\0\3\1\322\0\34\0\3\1\322\0\32\0\3\4\\\0\6\0\3\1\322\0\6\0\3\1\322\1t\0\3\3\20\0\2\0\3\3\24\0\36\0\3\3\20\0\2\0\3\3\24\0\b\0\3\3\f\0\36\0\3\3\f\0000\0\3\3\f\08\0\3\3\f\0\254\0\5\0\0\26\224\0\2\0\5\0\0\26\220\0\4\0\7N\271\0\376~p\0\0\0\"\0\2\0\5\0\0\26\224\0\b\0\5\0\0*0\0\2\0\13\0\0\26\224N\271\0\376~2\0\0\0\4\0\0\0\7N\271\0\376y\212\0\0\0\4\0\0\0\7N\271\0\376~H\0\0\0\16\0\2\0\5\0\0\26\224\0\2\0\5\0\0.\16\0\30\0\5\0\0*0\0\2\0\13\0\0\26\224N\271\0\376~2\0\0\0\4\0\0\0\7N\271\0\376y\212\0\0\0\4\0\0\0\7N\271\0\376~H\0\0\0\16\0\2\0\5\0\0\26\224\0\2\0\5\0\0.\16\0\34\0\5\0\0*0\0\2\0\13\0\0\26\224N\271\0\376~2\0\0\0\4\0\0\0\7N\271\0\376y\212\0\0\0\4\0\0\0\7N\271\0\376~H\0\0\0\16\0\2\0\5\0\0\26\224\0\2\0\5\0\0.\16\0$\0\5\0\0*0\0\2\0\13\0\0\26\224N\271\0\376~2\0\0\0\4\0\0\0\7N\271\0\376y\212\0\0\0\4\0\0\0\7N\271\0\376~H\0\0\0\16\0\2\0\5\0\0\26\224\0\2\0\5\0\0.\16\0\32\0\5\0\0*0\0\2\0\13\0\0\26\224N\271\0\376~2\0\0\0\4\0\0\0\7N\271\0\376y\212\0\0\0\4\0\0\0\7N\271\0\376~H\0\0\0\16\0\2\0\5\0\0\26\224\0\2\0\5\0\0.\16\0&\0\5\0\0*0\0\2\0\13\0\0\26\224N\271\0\376~2\0\0\0\4\0\0\0\7N\271\0\376y\212\0\0\0\4\0\0\0\7N\271\0\376~H\0\0\0\16\0\2\0\5\0\0\26\224\0\2\0\5\0\0.\16\0004\0\5\0\0*0\0\2\0\13\0\0\26\224N\271\0\376~2\0\0\0\4\0\0\0\7N\271\0\376y\212\0\0\0\4\0\0\0\7N\271\0\376~H\0\0\0\16\0\2\0\5\0\0\26\224\0\2\0\5\0\0.\16\0\"\0\5\0\0*0\0\2\0\13\0\0\26\224N\271\0\376~2\0\0\0\4\0\0\0\7N\271\0\376y\212\0\0\0\4\0\0\0\7N\271\0\376~H\0\0\0\16\0\2\0\5\0\0\26\224\0\2\0\5\0\0.\16\0\24\0\5\0\0*0\0\2\0\13\0\0\26\224N\271\0\376~2\0\0\0\4\0\0\0\7N\271\0\376y\212\0\0\0\4\0\0\0\7N\271\0\376~H\0\0\0\16\0\2\0\5\0\0\26\224\0\2\0\5\0\0.\16\0\"\0\5\0\0*0\0\2\0\13\0\0\26\224N\271\0\376~2\0\0\0\4\0\0\0\7N\271\0\376y\212\0\0\0\4\0\0\0\7N\271\0\376~H\0\0\0\16\0\2\0\5\0\0\26\224\0\2\0\5\0\0.\16\0\"\0\5\0\0*0\0\2\0\13\0\0\26\224N\271\0\376~2\0\0\0\4\0\0\0\7N\271\0\376y\212\0\0\0\4\0\0\0\7N\271\0\376~H\0\0\0\16\0\2\0\5\0\0\26\224\0\2\0\5\0\0.\16\0$\0\5\0\0*0\0\2\0\13\0\0\26\224N\271\0\376~2\0\0\0\4\0\0\0\7N\271\0\376y\212\0\0\0\4\0\0\0\7N\271\0\376~H\0\0\0\16\0\2\0\5\0\0\26\224\0\2\0\5\0\0.\16\0.\0\5\0\0*0\0\2\0\13\0\0\26\224N\271\0\376~2\0\0\0\4\0\0\0\7N\271\0\376y\212\0\0\0\4\0\0\0\7N\271\0\376~H\0\0\0\16\0\2\0\5\0\0\26\224\0\2\0\5\0\0.\16\0\34\0\5\0\0*0\0\2\0\13\0\0\26\224N\271\0\376~2\0\0\0\4\0\0\0\7N\271\0\376y\212\0\0\0\4\0\0\0\7N\271\0\376~H\0\0\0\16\0\2\0\5\0\0\26\224\0\2\0\5\0\0.\16\0.\0\5\0\0*0\0\2\0\13\0\0\26\224N\271\0\376~2\0\0\0\4\0\0\0\7N\271\0\376y\212\0\0\0\4\0\0\0\7N\271\0\376~H\0\0\0\16\0\2\0\5\0\0\26\224\0\2\0\5\0\0.\16\0\"\0\5\0\0*0\0\2\0\13\0\0\26\224N\271\0\376~2\0\0\0\4\0\0\0\7N\271\0\376y\212\0\0\0\4\0\0\0\7N\271\0\376~H\0\0\0\16\0\2\0\5\0\0\26\224\0\2\0\5\0\0.\16\0\"\0\5\0\0*0\0\2\0\13\0\0\26\224N\271\0\376~2\0\0\0\4\0\0\0\7N\271\0\376y\212\0\0\0\4\0\0\0\7N\271\0\376~H\0\0\0\16\0\2\0\5\0\0\26\224\0\2\0\5\0\0.\16\0\"\0\5\0\0*0\0\2\0\13\0\0\26\224N\271\0\376~2\0\0\0\4\0\0\0\7N\271\0\376y\212\0\0\0\4\0\0\0\7N\271\0\376~H\0\0\0\16\0\2\0\5\0\0\26\224\0\2\0\5\0\0.\16\0\"\0\5\0\0*0\0\2\0\13\0\0\26\224N\271\0\376~2\0\0\0\4\0\0\0\7N\271\0\376y\212\0\0\0\4\0\0\0\7N\271\0\376~H\0\0\0\16\0\2\0\5\0\0\26\224\0\2\0\5\0\0.\16\0\"\0\5\0\0*0\0\2\0\13\0\0\26\224N\271\0\376~2\0\0\0\4\0\0\0\7N\271\0\376y\212\0\0\0\4\0\0\0\7N\271\0\376~H\0\0\0\16\0\2\0\5\0\0\26\224\0\2\0\5\0\0.\16\0\"\0\5\0\0*0\0\2\0\13\0\0\26\224N\271\0\376~2\0\0\0\4\0\0\0\7N\271\0\376y\212\0\0\0\4\0\0\0\7N\271\0\376~H\0\0\0\16\0\2\0\5\0\0\26\224\0\2\0\5\0\0.\16\0\"\0\5\0\0*0\0\2\0\13\0\0\26\224N\271\0\376~2\0\0\0\4\0\0\0\7N\271\0\376y\212\0\0\0\4\0\0\0\7N\271\0\376~H\0\0\0\16\0\2\0\5\0\0\26\224\0\2\0\5\0\0.\16\0000\0\5\0\0*0\0\2\0\13\0\0\26\224N\271\0\376~2\0\0\0\4\0\0\0\7N\271\0\376y\212\0\0\0\4\0\0\0\7N\271\0\376~H\0\0\0\16\0\2\0\5\0\0\26\224\0\2\0\5\0\0.\16\0 \0\5\0\0*0\0\2\0\13\0\0\26\224N\271\0\376~2\0\0\0\4\0\0\0\7N\271\0\376y\212\0\0\0\4\0\0\0\7N\271\0\376~H\0\0\0\16\0\2\0\5\0\0\26\224\0\2\0\5\0\0.\16\08\0\5\0\0*0\0\2\0\13\0\0\26\224N\271\0\376~2\0\0\0\4\0\0\0\7N\271\0\376y\212\0\0\0\4\0\0\0\7N\271\0\376~H\0\0\0\16\0\2\0\5\0\0\26\224\0\2\0\5\0\0.\16\0\"\0\5\0\0*0\0\2\0\13\0\0\26\224N\271\0\376~2\0\0\0\4\0\0\0\7N\271\0\376y\212\0\0\0\4\0\0\0\7N\271\0\376~H\0\0\0\16\0\2\0\5\0\0\26\224\0\2\0\5\0\0.\16\0\"\0\5\0\0*0\0\2\0\13\0\0\26\224N\271\0\376~2\0\0\0\4\0\0\0\7N\271\0\376y\212\0\0\0\4\0\0\0\7N\271\0\376~H\0\0\0\16\0\2\0\5\0\0\26\224\0\2\0\5\0\0.\16\0\36\0\5\0\0*0\0\2\0\13\0\0\26\224N\271\0\376~2\0\0\0\4\0\0\0\7N\271\0\376y\212\0\0\0\4\0\0\0\7N\271\0\376~H\0\0\0\16\0\2\0\5\0\0\26\224\0\2\0\5\0\0.\16\0\"\0\5\0\0*0\0\2\0\13\0\0\26\224N\271\0\376~2\0\0\0\4\0\0\0\7N\271\0\376y\212\0\0\0\4\0\0\0\7N\271\0\376~H\0\0\0\16\0\2\0\5\0\0\26\224\0\2\0\5\0\0.\16\0\34\0\5\0\0*0\0\2\0\13\0\0\26\224N\271\0\376~2\0\0\0\4\0\0\0\7N\271\0\376y\212\0\0\0\4\0\0\0\7N\271\0\376~H\0\0\0\16\0\2\0\5\0\0\26\224\0\2\0\5\0\0.\16\0\"\0\5\0\0*0\0\2\0\13\0\0\26\224N\271\0\376~2\0\0\0\4\0\0\0\7N\271\0\376y\212\0\0\0\4\0\0\0\7N\271\0\376~H\0\0\0\16\0\2\0\5\0\0\26\224\0\2\0\5\0\0.\16\0\"\0\5\0\0*0\0\2\0\13\0\0\26\224N\271\0\376~2\0\0\0\4\0\0\0\7N\271\0\376y\212\0\0\0\4\0\0\0\7N\271\0\376~H\0\0\0\16\0\2\0\5\0\0\26\224\0\2\0\5\0\0.\16\0&\0\5\0\0*0\0\2\0\13\0\0\26\224N\271\0\376~2\0\0\0\4\0\0\0\7N\271\0\376y\212\0\0\0\4\0\0\0\7N\271\0\376~H\0\0\0\16\0\2\0\5\0\0\26\224\0\2\0\5\0\0.\16\0(\0\3\6\260\0\6\0\3\6\260\0\22\0\3\6\260\0>\0CM\370\20\0N\271\0\376\23\316 x*0 h\0\2\261\374\0\375X\0eD\261\374\0\376\235\364d<L\337\177\377M\370\20\0N\271\0\375\272(N\271\0\375\272( WN\271\0\375\300>A\372*\20\0\f\0\5\0\0*D\0\2\0\5\0\0*H\0\6\0\6\0\b\0\5\0\0\20V\0\2\0\5\0\0\26\222\0\4\0\5\0\0*D\0\6\0\5\0\0*H\0\n\0\2\0\6\0\5\0\0\26\264\0\4\0\5\0\0(\260\0\0\0\2\0\0\0\3\363_\0\2\0\5\0\0*H\0\6\0\5\0\0\20\0\0\b\0\5\32<\6\250\0\2\0\3\0320\0\4\0\3\0320\0\2\0\7N\271\0\376\232^\0\0\0\6\0\22\0\3\231>\0\b\0\5\0\0.\n\0(\0\7N\271\0\376\232t\0\0\0\2\0\24\0\3\231 \0J\0\5\6\372\6\366\0d\0\5\6\372\6\366\0b\0\5\6\372\6\366\0\234\0\5\6\372\6\366\1\4\0\7N\271\0\376|P\0\0\0\2\0\"\0\5\0\0\20V\0\2\0\5\0\0\20\0\0\n\0\3\231>\0`\0\7N\271\0\376|P\0\0\0\2\0\f\0\3\30@\0\2\0\3\30P\0\2\0\3\30`\0`\0\3\30\200\0*\0\3\30\200\0@\0\3\30\200\0\30\0\3\30\200\0\36\0\3\30\200\0 \0\3\30@\0\2\0\3\30P\0\2\0\3\30`\1t\0\3\30\200\0\f\0\3\30\200\0\332\0\3\30\0\0\b\0\3\30\200\0\2\0\3\30\0\0\214\0\3\30\200\0>\0\3\30`\0\36\0\3\30`\0006\0\3\30P\0\2\0\3\30@\0\2\0\3\30`\0000\0\3\30@\0\2\0\3\30`\0\b\0\3\30P\0*\0\3\30@\0\2\0\3\30`\0\b\0\3\30P\0*\0\3\30@\0\2\0\3\30`\0\b\0\3\30P\0(\0\3\30@\0\2\0\3\30`\0\n\0\3\30P\0*\0\3\30@\0\2\0\3\30`\0\b\0\3\30P\0*\0\3\30@\0\2\0\3\30`\0\b\0\3\30P\0*\0\3\30@\0\2\0\3\30`\0\b\0\3\30P\0*\0\3\30@\0\2\0\3\30`\0\b\0\3\30P\0*\0\3\30@\0\2\0\3\30`\0\24\0\3\30P\0002\0\3\30@\0\2\0\3\30`\0\b\0\3\30P\0&\0\3\30@\0\2\0\3\30`\0\b\0\3\30P\0&\0\3\30@\0\2\0\3\30`\0\b\0\3\30P\0&\0\3\30@\0\2\0\3\30`\0\b\0\3\30P\0&\0\3\30@\0\2\0\3\30`\0\b\0\3\30P\0&\0\3\30@\0\2\0\3\30`\0\b\0\3\30P\0&\0\3\30@\0\2\0\3\30`\0\b\0\3\30P\0&\0\3\30@\0\2\0\3\30`\0\b\0\3\30P\0&\0\3\30@\0\2\0\3\30`\0\b\0\3\30P\0$\0\3\30@\0\2\0\3\30`\0\22\0\3\30P\0\32\0\3\30`\0\36\0\3\30`\2>\0\3\30`\0\f\0\3\30`\0<\0\3\30`\0\26\0\3\30`\0&\0\3\30@\0\2\0\3\30P\0\36\0\3\30`\0&\0\3\30@\0\2\0\3\30P\0\36\0\3\30`\0&\0\3\30@\0\2\0\3\30P\0\36\0\3\30`\0&\0\3\30@\0\2\0\3\30P\0\36\0\3\30`\0&\0\3\30@\0\2\0\3\30P\0\36\0\3\30`\0&\0\3\30@\0\2\0\3\30P\0\36\0\3\30`\0&\0\3\30@\0\2\0\3\30P\0\36\0\3\30`\0&\0\3\30@\0\2\0\3\30P\0\36\0\3\30`\0&\0\3\30@\0\2\0\3\30P\0.\0\3\30`\0\30\0\3\30@\0\2\0\3\30P\0\2\0\3\30`\0\"\0\3\30`\0&\0\3\30@\0\2\0\3\30P\0\36\0\3\30`\0&\0\3\30@\0\2\0\3\30P\0\36\0\3\30`\0&\0\3\30@\0\2\0\3\30P\0\36\0\3\30`\0&\0\3\30@\0\2\0\3\30P\0\36\0\3\30`\0&\0\3\30@\0\2\0\3\30P\0\36\0\3\30`\0&\0\3\30@\0\2\0\3\30P\0\36\0\3\30`\0&\0\3\30@\0\2\0\3\30P\0\36\0\3\30`\0&\0\3\30@\0\2\0\3\30P\0\36\0\3\30`\0&\0\3\30@\0\2\0\3\30P\0.\0\3\30`\0\32\0\3\30P\0\b\0\3\30@\0\2\0\3\30`\0\30\0\3\0\0\0\n\0\3\0\0\0\6\0\7\347*\0\0\0\0\0\n\0\3\0\0\0\n\0\3\0\0\0\6\0\7\371\0\0\0\0\0\0\n\0\3\0\0\0\n\0\3\0\0\0\6\0\7\200\0\0\0\0\0\0\n\0\3\0\0\0\n\0\3\0\0\0\4\0\t\234@\0\0\0\0\0\0\0\n\0\3\0\0\0\n\0\3\0\0\0.\0\3\327\n\0\"\0\3\204#\0\n\0\3i\266\0\n\0\3B\274\0\n\0\3\316\375\0\n\0\3\245\227\0\n\0\3\325\277\0\n\0\3\252\377\0\n\0\3\210\314\0\n\0\3\16\23\0\n\0\3\330\17\0\n\0\3\254\331\0\n\0\3\341[\0L\0\3\30`\0\256\0\3\30`\0\n\0\3\30`\0X\0\3\30`\0\226\0\3\30`\0d\0\3\30`\0d\0\3\30`\0h\0\3\30`\0v\2@\0\306\0\7N\371\0\376\214(\0\0\0|\0\0\0YAccess fault or floating-point disabled stack frame\r\n\0  Fault or effective address is \0M\0x\0\3ar\0\36\6\235Fault Status Long Word (FSLW) is \0  Fault was caused by: \0                       \0PBE (Push Buffer Bus Error)\0SBE (Store Buffer Bus Error)\0PTA (Pointer A Fault)\0PTB (Pointer B Fault)\0IL (Indirect Level Fault)\0PF (Page Fault)\0SP (Supervisor Protect)\0WP (Write Protect)\0TWE (Bus Error on Table Search)\0RE (Bus Error on Read)\0WE (Bus Error on Write)\0TTR (TTR Hit)\0BPE (Branch Prediction Error)\0\0SEE (Software Emulation Error)\0  Fault occurred on: \0IO=0,MA=0 (Aligned or misaligned first operand access)\0IO=0,MA=1 (Misaligned second or later operand access)\0IO=1,MA=0 (Instruction opword fetch access)\0IO=1,MA=1 (Extension word fetch access)\0LK=0 (Not locked)\0LK=1 (Locked)\0, RW=0 (Undefined, reserved)\0, RW=1 (Write)\0, RW=2 (Read)\0, RW=3 (Read-Modify-Write)\0, SIZE=0 (Long)\0, SIZE=1 (Byte)\0, SIZE=2 (Word)\0, SIZE=3 (Double Precision or MOVE16)\0                     \0TT=0 (Normal Access)\0TT=1 (MOVE16 Access)\0TT=2 (Alternate or Debug Access)\0TT=3 (Acknowledge or LPSTOP Broadcast)\0, TM=0 (Data Cache Push Access)\0, TM=1 (User Data or MOVE16 Access)\0, TM=2 (User Code Access)\0, TM=3 (MMU Table Search Data Access)\0, TM=4 (MMU Table Search Code Access)\0, TM=5 (Supervisor Data Access)\0, TM=6 (Supervisor Code Access)\0, TM=7 (Reserved)\0, TM=0 (Logical Function Code 0)\0, TM=1 (Debug Access)\0, TM=2 (Reserved)\0, TM=3 (Logical Function Code 3)\0, TM=4 (Logical Function Code 4)\0, TM=5 (Debug Pipe Control Mode Access)\0, TM=6 (Debug Pipe Control Mode Access)\0, TM=7 (Logical Function Code 7)\0Access error stack frame\r\n\0  Fault address is \0  Effective address is \0Special status word (SSW) is \0ATC=0 (Bus error)\0ATC=1 (ATC fault)\0, RW=0 (Write)\0, RW=1 (Read)\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\304\0Y\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\2T$k\f8\0\2\f\274d\2`\34\f8\0\4\f\274d\22/\0Nz\0\2\200|\b\bN{\0\2 \37`\2\364\370Nu\f8\0\1\f\274d\4`\0\1\6f\24 n\6\240N{\200\0 n\6\244N{\200\1`\0\0\360\f8\0\3\f\274b$ n\6\240N{\200\0 n\6\244N{\200\1 n\6\250N{\200\2 n\6\254N{\210\2`\0\0\304\f8\0\6\f\274dZ n\6\240N{\200\0 n\6\244N{\200\1 n\6\250N{\200\2 n\32\216N{\200\3 n\32\222N{\200\4 n\32\226N{\200\5 n\32\232N{\200\6 n\32\236N{\200\7 n\6\260N{\210\5 n\32\252N{\210\6 n\32\256N{\210\7`` n\6\240N{\200\0 n\6\244N{\200\1 n\6\250N{\200\2 n\32\216N{\200\3 n\32\222N{\200\4 n\32\226N{\200\5 n\32\232N{\200\6 n\32\236N{\200\7 n\32\242N{\200\b n\32\252N{\210\6 n\32\256N{\210\7 n\32\262N{\210\bNu\f8\0\1\f\274d\4`\0\1\200f\" y\0\0\26\240N{\200\0 y\0\0\26\244N{\200\1 y\0\0\26\260N{\210\1`\0\1\\\f8\0\3\f\274b@ y\0\0\26\240N{\200\0 y\0\0\26\244N{\200\1 y\0\0\26\250N{\200\2 y\0\0\26\260N{\210\1 y\0\0\26\254N{\210\2 y\0\0\26\230N{\210\3`\0\1\24\f8\0\6\f\274d\0\0\210 y\0\0\26\240N{\200\0 y\0\0\26\244N{\200\1 y\0\0\26\250N{\200\2 y\0\0*\216N{\200\3 y\0\0*\222N{\200\4 y\0\0*\226N{\200\5 y\0\0*\232N{\200\6 y\0\0*\236N{\200\7 y\0\0\26\260N{\210\1 y\0\0\26\230N{\210\3 y\0\0*\246N{\210\5 y\0\0*\252N{\210\6 y\0\0*\256N{\210\7`\0\0\204 y\0\0\26\240N{\200\0 y\0\0\26\244N{\200\1 y\0\0\26\250N{\200\2 y\0\0*\216N{\200\3 y\0\0*\222N{\200\4 y\0\0*\226N{\200\5 y\0\0*\232N{\200\6 y\0\0*\236N{\200\7 y\0\0*\242N{\200\b y\0\0\26\260N{\210\1 y\0\0*\252N{\210\6 y\0\0*\256N{\210\7 y\0\0*\262N{\210\bNu\f8\0\1\f\274b\2`\20\f8\0\4\f\274b\b n\6\350N{\210\3Nu\f8\0\1\f\274b\2`\22\f8\0\4\f\274b\nA\371\0\0000\0N{\210\3Nu\f8\0\1\f\274b\2`\20\f8\0\4\f\274b\bNz\210\3-H\6\230Nu\f8\0\1\f\274d\4`\0\1\200f\"Nz\200\0#\310\0\0\26\240Nz\200\1#\310\0\0\26\244Nz\210\1#\310\0\0\26\260`\0\1\\\f8\0\3\f\274b@Nz\200\0#\310\0\0\26\240Nz\200\1#\310\0\0\26\244Nz\200\2#\310\0\0\26\250Nz\210\1#\310\0\0\26\260Nz\210\2#\310\0\0\26\254Nz\210\3#\310\0\0\26\230`\0\1\24\f8\0\6\f\274d\0\0\210Nz\200\0#\310\0\0\26\240Nz\200\1#\310\0\0\26\244Nz\200\2#\310\0\0\26\250Nz\200\3#\310\0\0*\216Nz\200\4#\310\0\0*\222Nz\200\5#\310\0\0*\226Nz\200\6#\310\0\0*\232Nz\200\7#\310\0\0*\236Nz\210\1#\310\0\0\26\260Nz\210\3#\310\0\0\26\230Nz\210\5#\310\0\0*\246Nz\210\6#\310\0\0*\252Nz\210\7#\310\0\0*\256`\0\0\204Nz\200\0#\310\0\0\26\240Nz\200\1#\310\0\0\26\244Nz\200\2#\310\0\0\26\250Nz\200\3#\310\0\0*\216Nz\200\4#\310\0\0*\222Nz\200\5#\310\0\0*\226Nz\200\6#\310\0\0*\232Nz\200\7#\310\0\0*\236Nz\200\b#\310\0\0*\242Nz\210\1#\310\0\0\26\260Nz\210\6#\310\0\0*\252Nz\210\7#\310\0\0*\256Nz\210\b#\310\0\0*\262Nu\f8\0\1\f\274b\2`\nNz\200\2#\310\0\0*<Nu\f8\0\1\f\274b\2`\34\f8\0\4\f\274b\24Nz\210\3#\310\0\0*4Nz\210\4#\310\0\0*8Nu\f8\0\1\f\274d\4`\0\1Jf\"Nz\200\0#\310\0\0\26\240Nz\200\1#\310\0\0\26\244Nz\210\1#\310\0\0\26\260`\0\1&\f8\0\3\f\274b,Nz\200\0#\310\0\0\26\240Nz\200\1#\310\0\0\26\244Nz\210\1#\310\0\0\26\260Nz\210\2#\310\0\0\26\254`\0\0\362\f8\0\6\f\274dpNz\200\0#\310\0\0\26\240Nz\200\1#\310\0\0\26\244Nz\200\3#\310\0\0*\216Nz\200\4#\310\0\0*\222Nz\200\5#\310\0\0*\226Nz\200\6#\310\0\0*\232Nz\200\7#\310\0\0*\236Nz\210\1#\310\0\0\26\260Nz\210\5#\310\0\0*\246Nz\210\6#\310\0\0*\252Nz\210\7#\310\0\0*\256`xNz\200\0#\310\0\0\26\240Nz\200\1#\310\0\0\26\244Nz\200\3#\310\0\0*\216Nz\200\4#\310\0\0*\222Nz\200\5#\310\0\0*\226Nz\200\6#\310\0\0*\232Nz\200\7#\310\0\0*\236Nz\200\b#\310\0\0*\242Nz\210\1#\310\0\0\26\260Nz\210\6#\310\0\0*\252Nz\210\7#\310\0\0*\256Nz\210\b#\310\0\0*\262Nu\f8\0\1\f\274d\4`\0\0016f\34Nz\200\0-H\6\240Nz\200\1-H\6\244Nz\210\1-H\6\260`\0\1\30\f8\0\3\f\274b4Nz\200\0-H\6\240Nz\200\1-H\6\244Nz\200\2-H\6\250Nz\210\1-H\6\260Nz\210\2-H\6\254Nz\210\4-H\6\234`\0\0\334\f8\0\6\f\274djNz\200\0-H\6\240Nz\200\1-H\6\244Nz\200\2-H\6\250Nz\200\3-H\32\216Nz\200\4-H\32\222Nz\200\5-H\32\226Nz\200\6-H\32\232Nz\200\7-H\32\236Nz\210\1-H\6\260Nz\210\4-H\6\234Nz\210\5-H\32\246Nz\210\6-H\32\252Nz\210\7-H\32\256`hNz\200\0-H\6\240Nz\200\1-H\6\244Nz\200\2-H\6\250Nz\200\3-H\32\216Nz\200\4-H\32\222Nz\200\5-H\32\226Nz\200\6-H\32\232Nz\200\7-H\32\236Nz\200\b-H\32\242Nz\210\1-H\6\260Nz\210\6-H\32\252Nz\210\7-H\32\256Nz\210\b-H\32\262NuA\372\0\200N\271\0\375\300>\f8\0\3\f\274e\22J8\f\276f\fA\372\0}N\271\0\375\300>`\30\f8\0\4\f\274e\20J8\f\275f\nA\372\0fN\271\0\375\300>\f8\0\1\f\274d\6A\372\0W`0f\6A\372\0U`(\f8\0\3\f\274d\6A\372\0M`\32f\6A\372\0K`\22\f8\0\5\f\274d\6A\372\0C`\4A\372\0CN\271\0\375\300>NuMicroprocessor: MC68\0EC\0LC\000000\r\n\000010\r\n\000020\r\n\000030\r\n\000040\r\n\000060\r\n\0\0\f8\0\3\f\274f>A\372\0>N\271\0\375\300>J8\f\275f\6A\372\0L`\"\362\'h\0\362\0\\\1\362\'h\0 \37\200\237\200\237\362\37H\0f\6A\372\0C`\4A\372\0GN\271\0\375\300>NuFloating-point co-processor: \0Software emulation\r\n\0MC68881\r\n\0MC68882\r\n\0\0H\347\377\372\f8\0\1\f\274d\6E\372\0R`0f\6E\372\0Z`(\f8\0\3\f\274d\6E\372\0\\`\32f\6E\372\0d`\22\f8\0\5\f\274d\6E\372\0f`\4E\372\0pJn\3\ng\2P\212J8\f\275g\bJn\6\370g\2X\212$RN\371\0\375\311F\0\375\313\32\0\375\313\32\0\375\313\351\0\375\313\351\0\376\203\32\0\376\203\32\0\376\203R\0\376\203R\0\375\313L\0\375\313\211\0\375\314\37\0\375\314c\0\375\313L\0\375\313\211\0\375\314\37\0\375\314c\0\376\203\216\0\376\203\332\0\376\204I\0\376\204\227\0\376\205\22\0\376\205^\0\376\205\315\0\376\206\35\234\376\230\376\231\376\235\376&\377\371\236\376\237\376\242\377\375\376\0\376\1\376\2\376\3\376\376\4\376\5\376\6\376\7\377\374\376\b\376\t\376\n\376\13\376\376\f\376\r\376\16\376\17\377\373\234\376\230\376\231\377\235\376&\377\371\236\376\237\376\242\377\375\376\0\376\1\376\2\376\3\377\376\376\376\4\376\5\376\6\376\7\377\374\376\b\376\t\376\n\376\13\377\376\376\376\f\376\r\376\16\376\17\377\373\234\376\230\376\232\376\233\376\235\376&\377\371\236\376\237\376\242\376\240\377\261\376\270\376\271\376\267\377\262\376\263\376\264\376\265\377\375\376\0\376\1\376\2\376\3\376\376\4\376\5\376\6\376\7\377\374\376\b\376\t\376\n\376\13\376\376\f\376\r\376\16\376\17\377\373\234\376\230\376\232\376\233\376\235\376&\377\371\236\376\237\376\242\376\240\377\261\376\270\376\271\376\267\377\262\376\263\376\264\376\265\377\375\376\0\376\1\376\2\376\3\376\376\4\376\5\376\6\376\7\377\374\376\b\376\t\376\n\376\13\376\376\f\376\r\376\16\376\17\377\372\376\20\376\21\377\376\376\376\22\376\23\376\376\243\377\376\376\376\24\376\25\376\376\244\377\376\376\376\26\376\27\376\245\377\373\234\376\235\376&\377\371\230\376\232\376\233\377\236\376\237\376\242\376\240\377\261\376\270\376\271\377\262\376\263\376\264\376\265\377\375\376\0\376\1\376\2\376\3\377\376\376\376\4\376\5\376\6\376\7\377\374\376\b\376\t\376\n\376\13\377\376\376\376\f\376\r\376\16\376\17\377\373\234\376\235\376&\377\371\230\376\232\376\233\377\236\376\237\376\242\376\240\377\261\376\270\376\271\377\262\376\263\376\264\376\265\377\375\376\0\376\1\376\2\376\3\377\376\376\376\4\376\5\376\6\376\7\377\374\376\b\376\t\376\n\376\13\377\376\376\376\f\376\r\376\16\376\17\377\372\376\20\377\376\376\376\21\377\376\376\376\22\377\376\376\376\23\377\376\376\376\24\377\376\376\376\25\377\376\376\376\26\377\376\376\376\27\377\243\376\244\376\245\377\373\234\376\230\376\231\376\235\376&\377\371\236\376\237\376\242\376\240\376\272\376\266\377\261\376\270\376\271\377\262\376\263\376\264\376\265\377\375\376\0\376\1\376\2\376\3\376\376\4\376\5\376\6\376\7\377\374\376\b\376\t\376\n\376\13\376\376\f\376\r\376\16\376\17\377\373\234\376\230\376\231\376\235\376&\377\371\236\376\237\376\242\376\240\376\272\376\266\377\261\376\270\376\271\377\262\376\263\376\264\376\265\377\375\376\0\376\1\376\2\376\3\376\376\4\376\5\376\6\376\7\377\374\376\b\376\t\376\n\376\13\376\376\f\376\r\376\16\376\17\377\372\376\20\376\21\377\376\376\376\22\376\23\376\376\243\377\376\376\376\24\376\25\376\376\244\377\376\376\376\26\376\27\376\245\377\373\234\376\235\376&\377\371\230\376\231\376\236\376\237\376\242\377\240\376\272\376\266\377\261\376\270\376\271\377\262\376\263\376\264\376\265\377\375\376\0\376\1\376\2\376\3\377\376\376\376\4\376\5\376\6\376\7\377\374\376\b\376\t\376\n\376\13\377\376\376\376\f\376\r\376\16\376\17\377\373\234\376\235\376&\377\371\230\376\231\376\236\376\237\376\242\377\240\376\272\376\266\377\261\376\270\376\271\377\262\376\263\376\264\376\265\377\375\376\0\376\1\376\2\376\3\377\376\376\376\4\376\5\376\6\376\7\377\374\376\b\376\t\376\n\376\13\377\376\376\376\f\376\r\376\16\376\17\377\372\376\20\377\376\376\376\21\377\376\376\376\22\377\376\376\376\23\377\376\376\376\24\377\376\376\376\25\377\376\376\376\26\377\376\376\376\27\377\243\376\244\376\245\377\373\0\376\211\324\0\0\0\0\0\0\0\0\0V\0\376\211\330\0\0\0\0\0\0\0\0\0Z\0\376\211\334\0\0\0\0\0\0\0\0\0^\0\376\211\340\0\0\0\0\0\0\0\0\0b\0\376\211\344\0\0\0\0\0\0\0\0\0f\0\376\211\350\0\0\0\0\0\0\0\0\0j\0\376\211\354\0\0\0\0\0\0\0\0\0n\0\376\211\360\0\0\0\0\0\0\0\0\0r\0\376\211\364\0\0\0\0\0\0\0\1\0v\0\376\211\370\0\0\0\0\0\0\0\1\0z\0\376\211\374\0\0\0\0\0\0\0\1\0~\0\376\212\0\0\0\0\0\0\0\0\1\0\202\0\376\212\4\0\0\0\0\0\0\0\1\0\206\0\376\212\b\0\0\0\0\0\0\0\1\0\212\0\376\212\f\0\0\0\0\0\0\0\1\0\216\0\376\212\20\0\0\0\0\0\0\0\2\0\222\0\376\212\24\0\0\0\0\0\0\3\3\30\260\0\376\212\31\0\0\0\0\0\0\3\3\30\274\0\376\212\36\0\0\0\0\0\0\3\3\30\310\0\376\212#\0\0\0\0\0\0\3\3\30\324\0\376\212(\0\0\0\0\0\0\3\3\30\340\0\376\212-\0\0\0\0\0\0\3\3\30\354\0\376\2122\0\0\0\0\0\0\3\3\30\370\0\376\2127\0\0\0\0\0\0\3\3\31\4\0\376\212<\0\0\0\0\0\0\0\4\0\232\0\376\212A\0\0\0\0\0\0\0\5\0\222\0\376\212F\0\0\0\0\0\0\1\6\6\230\0\376\212K\0\0\0\0\0\0\1\7\0\222\0\376\212P\0\0\0\0\0\0\0\22\0\226\0\376\212T\0\0\377\377\0\0\0\b\0\236\0\376\212X\0\0\0\7\0\0\1\t\6\240\0\376\212]\0\0\0\7\0\0\1\n\6\244\0\376\212b\0\0\0\0\0\0\1\13\6\250\0\376\212h\0\0\0\0\0\0\1\f\6\254\0\376\212n\0\0\0\0\0\0\1\r\6\260\0\376\212s\0\0\0\0\0\0\3\16\6\264\0\376\212y\0\0\0\0\0\0\3\17\6\270\0\376\212\177\0\0\0\0\0\0\3\20\6\274\0\376\212\353\0\0\0\0\0\0\0\21\0\236\0\376\212\303\0\0\0\0\0\0\0\0\2\340\0\376\212\307\0\0\0\0\0\0\0\0\2\344\0\376\212\313\0\0\0\0\0\0\0\0\2\350\0\376\212\317\0\0\0\0\0\0\0\0\2\354\0\376\212\323\0\0\0\0\0\0\0\0\2\360\0\376\212\327\0\0\0\0\0\0\0\0\2\364\0\376\212\333\0\0\0\0\0\0\0\0\2\370\0\376\212\337\0\0\0\0\0\0\0\0\2\374\0\376\212\343\0\0\0\0\0\0\0\0\3\0\0\376\212\347\0\0\0\0\0\0\0\0\3\4\0\376\212\360\0\0\0\0\0\0\1\23\32\216\0\376\212\364\0\0\0\0\0\0\1\24\32\222\0\376\212\372\0\0\0\0\0\0\1\25\32\226\0\376\213\0\0\0\0\0\0\0\1\26\32\232\0\376\213\6\0\0\0\0\0\0\1\27\32\236\0\376\213\f\0\0\0\0\0\0\1\30\32\242\0\376\213\23\0\0\0\0\0\0\1\31\32\246\0\376\213\32\0\0\0\0\0\0\1\32\32\252\0\376\213\37\0\0\0\0\0\0\1\33\32\256\0\376\213$\0\0\0\0\0\0\1\34\32\262D0\0\0D1\0\1D2\0\2D3\0\3D4\0\4D5\0\5D6\0\6D7\0\7A0\0\bA1\0\tA2\0\nA3\0\13A4\0\fA5\0\rA6\0\16A7\0\17FP0\0\20FP1\0\21FP2\0\22FP3\0\23FP4\0\24FP5\0\25FP6\0\26FP7\0\27USP\0\30SSP\0\31MSP\0\32ISP\0\33PC\0\34SR\0\35SFC\0\36DFC\0\37CACR\0 CAAR\0!VBR\0\"FPCR\0#FPSR\0$FPIAR\0%SP\0\17R0\0\0R1\0\1R2\0\2R3\0\3R4\0\4R5\0\5R6\0\6R7\0\7I\0\377M\0\376DIS\0\375FPCP\0\374FP\0\373FX\0\372Z0\0\'Z1\0(Z2\0)Z3\0*Z4\0+Z5\0,Z6\0-Z7\0.Z8\0/Z9\0000CCR\0&TC\0001ITT0\0002ITT1\0003DTT0\0004DTT1\0005BUSCR\0006MMUSR\0007URP\08SRP\09PCR\0:\0\0\0\320@ ;\0\6N\373\b\2\377\377?2\377\377?2\377\377?:\377\377?h\377\377?2\377\377?2\377\377?2\377\377?2\377\377?\222\377\377?\232\377\377?\232\377\377?2\377\377?2\377\377?2\377\377?2\377\377?2\377\377?2\377\377?\246\377\377?2\377\377?2\377\377?2\377\377?2\377\377?2\377\377?2\377\377?2\377\377?2\377\377?2\377\377?2\377\377?2\320@ ;\0\6N\373\b\2\377\377\221\376\377\377\221\376\377\377\222\b\377\377\222P\377\377\221\376\377\377\221\376\377\377\221\376\377\377\221\376\377\377\2226\377\377\222>\377\377\222>\377\377\221\376\377\377\221\376\377\377\221\376\377\377\221\376\377\377\221\376\377\377\221\376\377\377\222F\377\377\221\376\377\377\221\376\377\377\221\376\377\377\221\376\377\377\221\376\377\377\221\376\377\377\221\376\377\377\221\376\377\377\221\376\377\377\221\376\377\377\221\376H\347\340\0a\0\341\0304\0C\372\0J\20\31N\271\0\375\300.\20\31f\366p:N\271\0\375\300.p\1\300\2\320<\0000N\271\0\375\300.p N\271\0\375\300.Jn\3\ng\f\f\21\0Pf\6N\271\0\375\272(\342J\20\31f\300L\337\0\7NuHI\0LS\0CC(HS)\0CS(LO)\0NE\0EQ\0VC\0VS\0PL\0MI\0GE\0LT\0GT\0LE\0\0\0\2(\2)\2.\0023\28\2=\2C\2G\2L\2Q\2(\2V\2[\2`\2e\2k\2o\2s\2y\2}\2\202\2(\2\206\2\212\2\220\2\224\2\231\2\236\2\243\2\247\2\253\2\260\2\264\2\270\2\274\2\301\2\305\2\312\2\320\2\324\2\330\2\334\2\341\2\346\2\354\2\361\2\366\2\373\2\376\3\3\3\b\3\f\3\37\3#\3(\3-\0031\0035\3:\3?\3D\3H\3M\3R\3V\3Z\3^\3b\3f\3k\3p\3t\3x\3\213\3\223\3\231\3\235\3\242\3\247\3\253\3\260\3\266\3\273\3\300\3\304\3\312\3\320\3\326\3\333\3\340\3\346\3\355\3\363\3\372\4\0\4\6\4\f\4\22\4\32\4!\4\'\4.\0044\4:\4>\4A\4G\4J\4P\4Y\2(\4a\4e\4i\4n\4u\4y\4~\2(\2(\2(\4\206\4\214\4\221\4\227\4\236\2(\4\244\2(\4\254\4\264\4\272\2(\4\300\4\306\4\315\4\322\4\327\4\335\4\345\2(\4\355\4\363\4\372\2(\5\0\5\5\5\13\2(\5\20\5\26\5\33\5#\5+\0050\0055\5:\5?\5G\5L\5S\5[\2(\2(\2(\2(\2(\2(\2(\5`\5h\5p\5x\5\200\5\210\5\220\5\230\5\240\2(\5\245\2(\2(\2(\2(\2(\5\252\5\262\5\270\5\277\5\303\5\306\5\314\5\321\5\324\5\332\5\343\5\350\5\357\2(\2(\5\366\5\375\2(\2(\2(\2(\2(\2(\2(\2(\2(\2(\2(\2(\2(\2(\2(\2(\2(\2(\6\4\2(\6\n\2(\6\20\2(\6\26\2(\6\34\2(\6\"\6(\6.\2(\0064\6:\6@\2(\6F\2(\2(\2(\2(\2(\2(\2(\2(\2(\2(\2(\2(\2(\2(\2(\2(\2(\2(\2(\2(\2(\6L\6R\6X\6^\6e\6l\6s\6{\6\202\6\213\6\223\6\232\6\241\6\247\6\255\6\264\0btst\0bchg\0bclr\0bset\0movep\0ori\0andi\0subi\0addi\0eori\0cmpi\0move\0movea\0chk\0lea\0reset\0nop\0stop\0rte\0rts\0trapv\0rtr\0trap\0link\0unlk\0jsr\0jmp\0negx\0clr\0neg\0not\0nbcd\0pea\0swap\0movem\0ext\0tst\0tas\0addq\0subq\0moveq\0sbcd\0divu\0divs\0or\0subx\0suba\0sub\0line 1010 emulator\0cmp\0cmpa\0cmpm\0eor\0exg\0abcd\0mulu\0muls\0and\0addx\0adda\0add\0asl\0asr\0lsl\0lsr\0roxl\0roxr\0rol\0ror\0line 1111 emulator\0illegal\0callm\0rtm\0cmp2\0chk2\0cas\0cas2\0moves\0bkpt\0extb\0rtd\0movec\0divul\0divsl\0pack\0unpk\0bftst\0bfextu\0bfchg\0bfexts\0bfclr\0bfffo\0bfset\0bfins\0pflushr\0pvalid\0pload\0pflush\0pmove\0ptest\0pdb\0ps\0ptrap\0pb\0psave\0prestore\0pmovefd\0GEN\0Scc\0DBcc\0TRAPcc\0Bcc\0SAVE\0RESTORE\0fmove\0fint\0fsinh\0fintrz\0fsqrt\0flognp1\0fetoxm1\0ftanh\0fatan\0fasin\0fatanh\0fsin\0ftan\0fetox\0ftwotox\0ftentox\0flogn\0flog10\0flog2\0fabs\0fcosh\0fneg\0facos\0fcos\0fgetexp\0fgetman\0fdiv\0fmod\0fadd\0fmul\0fsgldiv\0frem\0fscale\0fsglmul\0fsub\0fsincos\0fsincos\0fsincos\0fsincos\0fsincos\0fsincos\0fsincos\0fsincos\0fcmp\0ftst\0fmovecr\0fmove\0fmovem\0fdb\0fs\0ftrap\0fnop\0fb\0fsave\0frestore\0fbra\0fsmove\0fssqrt\0fdmove\0fdsqrt\0fsabs\0fsneg\0fdabs\0fdneg\0fsdiv\0fsadd\0fsmul\0fddiv\0fdadd\0fdmul\0fssub\0fdsub\0cinvl\0cinvp\0cinva\0cpushl\0cpushp\0cpusha\0pflushn\0pflush\0pflushan\0pflusha\0ptestw\0ptestr\0plpaw\0plpar\0move16\0lpstop\0\0UL0\5\320@\320@/;\0\4Nu\0\376\2246\0\376\223\240\0\376\2246\0\375\263\226\0\375\263\304\0\375\263\310\0\375\263\376\0\375\264<\0\375\264\2040<\0?Bn\6\364aLf\22N\271\0\375\264\312S@j\6N\371\0\375i\2\30\301\f\25\0,f\34a&f\22N\271\0\375\264\324S@j\6N\371\0\375i\2\30\301\f\25\0,g\3440\f\2@\0\1g\2B\34NuR\215a\6f\2S\215NuH\347x\0006\0a\0\253F\f\25\0\"g\20\f\25\0\'g\n0\3r\0L\337\0\36Nu\30\35\20\35g\20\260\4g\f\30\300Q\313\377\364N\371\0\375i\0020\3r\377L\337\0\36Nu0<\0?Bn\6\364N\271\0\375\264\312U@d\6N\371\0\375i\28\301\f\25\0,f\30N\271\0\375\264\324U@d\6N\371\0\375i\28\301\f\25\0,g\350NuH\347\200\4a\0\252\316p\0\f\35\0(fN\20\35\260<\0Ag\6\260<\0af\16\20\35\220<\0000\260<\0\7b4`\34\260<\0Sg\6\260<\0sf&\20\35\260<\0Pg\6\260<\0pf\30p\7\f\35\0)f\20<<\365\b\214@=F\1*L\337 \1NuL\337 \1N\371\0\375\224\214\f8\0\4\f\274e\n<<\365\30=F\1*NuN\371\0\375\225bH\347\200\4a\0\252Np\0\f\35\0(fZ\20\35\260<\0Ag\6\260<\0af\16\20\35\220<\0000\260<\0\7b@`\34\260<\0Sg\6\260<\0sf2\20\35\260<\0Pg\6\260<\0pf$p\7\f\35\0)f\34\274|\200\0f\6<<\365H`\4<<\365h\214@=F\1*L\337 \1NuL\337 \1N\371\0\375\226\324N\271\0\376?D\20\35N\271\0\376@j\260<\0Nf\2`*\260<\0Df\6\214|\0@`\36\260<\0If\6\214|\0\200`\22\260<\0Bf\6\214|\0\300`\6N\371\0\375i\2\20\35N\271\0\376@j\260<\0Cg\6N\371\0\375i\2p\347\200FR@e\24N\271\0\375j\212\264|\0\2g\6N\371\0\375i\2\214C=F\1*Nu\274|\365\20g\24N\271\0\375j\220\fB\0\2g\6N\371\0\375i\2\214C=F\1*Nu\274|\370\0f JEg\f\fE\0\2g\6N\371\0\375i\28\374\1\300N\271\0\375t\48\301`\0\0\322JEg\6N\371\0\375i\2N\271\0\375j\220\fB\0\2f.\214CN\271\0\375j\212\fB\0\ng\6\fB\0\13f\20\fB\0\nf\2H\301(\301\214|\0\20`\6N\371\0\375i\2`\0\0\212\fB\0\3f<\214CN\271\0\375j\212\fB\0\ng\6\fB\0\13f\f\fB\0\nf\2H\301(\301`\32\fB\0\3f\16\350[\206|\200\08\303\214|\0 `\6N\371\0\375i\2`F\fB\0\ng\6\fB\0\13f4\fB\0\nf\2H\301(\301N\271\0\375j\212\fB\0\2f\b\214C\214|\0\30`\24\fB\0\3f\b\214C\214|\0\b`\6N\371\0\375i\2`\6N\371\0\375i\2=F\1*a\0\250R\f\25\0,f\6N\371\0\375i\2Nu\314|\0\177\274|\0@d\6\334|\0x`\4\334|\0\203Nu-O\6\3200\7B\0\260|\364\0fjp8\300Gg\0\1\356\346H\320|\1\2\260|\1\6g\0\1\340c\2S@<\0BEN\271\0\375\327\350\26\374\0\t\20\7\354\bf\6\26\374\0n`\26U\0d\6\26\374\0d`\ff\6\26\374\0i`\4\26\374\0b\26\374\0cp\347\200GR@g\20\26\374\0,v\7\306Gt\2N\271\0\375\330\362B\33Nup\370\300G\260|\365\0f\6<<\1\t`Z\260|\365\bf\6<<\1\n`N\260|\365\20f\6<<\1\13`\n\260|\365\30f\16<<\1\fBEN\271\0\375\327\350Nu\260|\365Hf\6<<\1\r`\"\260|\365hf\6<<\1\16`\26\260|\365\210f\6<<\1\17`\n\260|\365\310f <<\1\20BEN\271\0\375\327\350\26\374\0\tv\7\306Gt\2N\271\0\375\330\362B\33Nu\260|\366\0f\4t\3`\22\260|\366\bf\4t\3`D\260|\366\20f6t\2<<\1\21BEN\271\0\375\327\350\26\374\0\tv\7\306GN\271\0\375\330\362\26\374\0,\26\374\0$\"\35N\271\0\375\273\2N\271\0\375\325fB\33Nu\260|\366\30f6t\2<<\1\21BEN\271\0\375\327\350\26\374\0\t\26\374\0$\"\35N\271\0\375\273\2N\271\0\375\325f\26\374\0,v\7\306GN\271\0\375\330\362B\33Nu\260|\366 f@0<\217\377\300U\260|\200\0ff<<\1\21BEN\271\0\375\327\350\26\374\0\tv\7\306Gt\3N\271\0\375\330\362\26\374\0,6\35\351[\306|\0\7t\3N\271\0\375\330\362B\33Nu\260|\370\0f,\fU\1\300f&T\215<<\1\22BEN\271\0\375\327\350\26\374\0\t\26\374\0#\26\374\0$2\35N\271\0\375\273\bB\33NuN\371\0\375\370\226Illegal stack format number\r\n\0Exceptional abort by \0CCR\0SR\0USP\0SFC\0DFC\0CACR\0VBR\0CAAR\0MSP\0ISP\0TC\0ITT0\0ITT1\0DTT0\0DTT1\0MMUSR\0URP\0SRP\0BUSCR\0PCR\0\b\0\0\0\0\1\0\2\b\1\b\2\b\3\b\4\0\3\0\4\0\5\0\6\0\7\b\5\b\6\b\7\0\b\b\b\0\0sfc\0\0\1dfc\0\0\2cacr\0\0\0\3tc\0\0\0\4itt0\0\0\0\5itt1\0\0\0\6dtt0\0\0\0\7dtt1\0\0\b\0usp\0\b\1vbr\0\b\2caar\0\0\b\3msp\0\b\4isp\0\b\5mmusr\0\b\6urp\0\b\7srp\0\0\bbuscr\0\b\bpcr\0\377\377\377\377N\271\0\375\264\336A\371\0\375\270mNu\f8\0\6\f\274d\f-n\0324\6\230-n\328\6\234Nu\f@\0\4e(g\0\0\234\f@\0\7e\0\2<\f@\0\tg\0\266\256NuN\371\0\375\300>N\371\0\375\272(N\371\0\375\272\310p\f\321\256\0\222A\372\0@a\342A\372\334\206a\334\",\0\ba\342A\372\334\177a\3202\24N\271\0\375\272\316a\314*l\0\b/\f=n\6\372\6\366N\271\0\375\325\334(_a\266N\271\0\376A\236`\0\270:Floating-point post-instruction stack frame\r\n\0p\20\321\256\0\222A\372\323na\0\377lA\372\323\234a\0\377d\",\0\ba\0\377ha\0\377^A\372\333\372a\0\377PA\370\0\ba\0\246\270*l\0\2/\f=n\6\372\6\366N\271\0\375\325\334(_a\0\3776A\372\324\32a\0\377(\",\0\fa\0\377,a\0\377\"A\372\324(a\0\377\24A\372\324Pp\0002,\0\16t\16\322Aj&J\20g\"H\347`\200J\0g\bA\372\324\34a\0\376\360 o\0\ba\0\376\350a\0\376\352L\337\1\6p\1J\30f\374Q\312\377\320A\372\325ga\0\376\316A\372\325uJl\0\16j\4A\372\325\330\b,\0\3\0\fg\4J\30f\374a\0\376\260a\0\376\262A\372\326\337a\0\376\244A\372\326\f\b,\0\1\0\fg\4A\372\326\22a\0\376\220A\372\326\30\b,\0\0\0\fg\4A\372\3268J,\0\rj\4J\30f\374a\0\376rA\372\326O\b,\0\6\0\rg\4A\372\326c\b,\0\5\0\rg\4J\30f\374a\0\376Ra\0\376TA\372\326\201a\0\376FA\372\326\217\b,\0\4\0\rg\4A\372\326\255\b,\0\3\0\rg\4J\30f\374a\0\376&A\372\326\341\b,\0\4\0\rg\f\b,\0\3\0\rf<A\372\327\311\b,\0\2\0\rg\20J\30f\374J\30f\374J\30f\374J\30f\374\b,\0\1\0\rg\bJ\30f\374J\30f\374\b,\0\0\0\rg\4J\30f\374a\0\375\326`\0\376 p<\321\256\0\222A\372\330\177a\0\375\304A\372\330\222a\0\375\274\",\0\24a\0\375\300a\0\375\266A\372\330\222a\0\375\250\",\0\ba\0\375\254a\0\375\242A\372\332>a\0\375\224A\370\0\ba\0\244\374*l\0\2/\f=n\6\372\6\366N\271\0\375\325\334(_a\0\375zA\372\330na\0\375l2,\0\fN\271\0\375\272\316a\0\375dA\372\322ja\0\375VA\372\330n\b,\0\2\0\fg\4A\372\330ta\0\375Ba\0\375DA\372\323\317a\0\3756A\372\323\342\b,\0\3\0\fg\4A\372\324\ra\0\375\"a\0\375$A\372\325Qa\0\375\26A\372\324~\b,\0\1\0\fg\4A\372\324\204a\0\375\2A\372\330>\b,\0\0\0\fg\4A\372\330Aa\0\374\356`\0\376zp\0\b-\0\6\0\1f\6N\271\0\375\334T\321\300Nu o\0\4`\2R\211\20\30f\6J\21f\nNua\0\242\232\260\21g\354J\31f\374R\211J\21f\334X\217`\0\200\36a\0\241\\\260<\0:f\2B\0Nu".getBytes (XEiJ.ISO_8859_1);



}  //class ROM



