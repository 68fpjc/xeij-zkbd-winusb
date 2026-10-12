//========================================================================================
//  HostCDROM.java
//    en:Host CD-ROM
//    ja:ホストCD-ROM
//  Copyright (C) 2003-2026 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.awt.event.*;  //ActionListener
import java.lang.foreign.*;
import java.lang.invoke.*;
import java.util.*;  //NoSuchElementException
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.event.*;  //ChangeListener

//class HostCDROM
//  WindowsのときホストマシンのDVD/CD-ROMドライブをSCSI CD-ROMドライブとして使用します
public class HostCDROM {

  //定数

  //設定
  public static final boolean HCD_ENABLED = true;  //true=有効
  static final int HCD_DEFAULT_SCSI_ID = 6;  //デフォルトのSCSI ID(0～15)
  static final int HCD_DEFAULT_VOLUME = 25;  //デフォルトの音量(0～100)
  static final int HCD_PLAY_QUEUE_SIZE = 4;  //再生キューのサイズ
  static final int HCD_PLAY_SECTORS = 30;  //一度に再生するセクタ数。30セクタ
  static final int HCD_PLAY_MILLIS = 1000 * HCD_PLAY_SECTORS / 75;  //一度に再生するミリ秒数。1000*30/75=400ms
  static final int HCD_PLAY_BYTES = 2352 * HCD_PLAY_SECTORS;  //一度に再生するバイト数。2352*30=70560バイト

  //エラーコード
  //  https://learn.microsoft.com/en-us/windows/win32/debug/system-error-codes--0-499-
  static final int ERROR_NOT_READY = 21;
  static final int ERROR_WRONG_DISK = 34;
  static final int ERROR_NO_MORE_ITEMS = 259;

  //ファイル操作
  static final long INVALID_HANDLE_VALUE = -1L;
  //  https://learn.microsoft.com/ja-jp/windows/win32/secauthz/generic-access-rights
  static final int GENERIC_ALL = 0x10000000;
  static final int GENERIC_EXECUTE = 0x20000000;
  static final int GENERIC_WRITE = 0x40000000;
  static final int GENERIC_READ = 0x80000000;
  //  https://learn.microsoft.com/ja-jp/windows/win32/api/fileapi/nf-fileapi-createfilea
  static final int FILE_SHARE_READ = 0x00000001;
  static final int FILE_SHARE_WRITE = 0x00000002;
  static final int FILE_SHARE_DELETE = 0x00000004;
  //  https://learn.microsoft.com/ja-jp/windows/win32/api/fileapi/nf-fileapi-createfilea
  static final int CREATE_NEW = 1;
  static final int CREATE_ALWAYS = 2;
  static final int OPEN_EXISTING = 3;
  static final int OPEN_ALWAYS = 4;
  static final int TRUNCATE_EXISTING = 5;
  //  https://learn.microsoft.com/ja-jp/windows/win32/api/fileapi/nf-fileapi-setfilepointerex
  static final int FILE_BEGIN = 0;
  static final int FILE_CURRENT = 1;
  static final int FILE_END = 2;

  //CD-ROM操作
  //  https://learn.microsoft.com/ja-jp/windows/win32/api/fileapi/nf-fileapi-getdrivetypea
  static final int DRIVE_UNKNOWN = 0;
  static final int DRIVE_NO_ROOT_DIR = 1;
  static final int DRIVE_REMOVABLE = 2;
  static final int DRIVE_FIXED = 3;
  static final int DRIVE_REMOTE = 4;
  static final int DRIVE_CDROM = 5;
  static final int DRIVE_RAMDISK = 6;
  //DeviceIoControl
  static final int IOCTL_CDROM_READ_TOC = 0x00024000;
  static final int IOCTL_CDROM_RAW_READ = 0x0002403e;
  static final int IOCTL_CDROM_READ_TOC_EX = 0x00024054;
  static final int IOCTL_SCSI_PASS_THROUGH_DIRECT = 0x0004d014;
  static final int IOCTL_SCSI_PASS_THROUGH_DIRECT_EX = 0x0004d048;
  static final int IOCTL_STORAGE_CHECK_VERIFY2 = 0x002d0800;
  static final int IOCTL_STORAGE_LOAD_MEDIA2 = 0x002d080c;
  static final int IOCTL_STORAGE_QUERY_PROPERTY = 0x002d1400;
  static final int IOCTL_STORAGE_CHECK_VERIFY = 0x002d4800;
  static final int IOCTL_STORAGE_MEDIA_REMOVAL = 0x002d4804;
  static final int IOCTL_STORAGE_EJECT_MEDIA = 0x002d4808;
  static final int IOCTL_STORAGE_LOAD_MEDIA = 0x002d480c;
  static final int CDROM_READ_TOC_EX_FORMAT_TOC = 0x00000000;
  //CDROM_TOC
  static final int MAXIMUM_NUMBER_TRACKS = 0x00000064;
  //TRACK_MODE_TYPE
  //  https://learn.microsoft.com/ja-jp/windows-hardware/drivers/ddi/ntddcdrm/ne-ntddcdrm-_track_mode_type
  static final int YellowMode2 = 0;
  static final int XAForm2 = 1;
  static final int CDDA = 2;
  static final int RawWithC2AndSubCode = 3;
  static final int RawWithC2 = 4;
  static final int RawWithSubCode = 5;

  //STORAGE_PROPERTY_ID列挙
  //  https://learn.microsoft.com/ja-jp/windows/win32/api/winioctl/ne-winioctl-storage_property_id
  static final int StorageDeviceProperty = 0;

  //STORAGE_QUERY_TYPE列挙
  //  https://learn.microsoft.com/ja-jp/windows/win32/api/winioctl/ne-winioctl-storage_query_type
  static final int PropertyStandardQuery = 0;

  //バスタイプ
  static final int BusTypeUnknown = 0;
  static final int BusTypeScsi = 1;
  static final int BusTypeAtapi = 2;
  static final int BusTypeAta = 3;
  static final int BusType1394 = 4;
  static final int BusTypeSsa = 5;
  static final int BusTypeFibre = 6;
  static final int BusTypeUsb = 7;
  static final int BusTypeRAID = 8;
  static final int BusTypeiScsi = 9;
  static final int BusTypeSas = 10;
  static final int BusTypeSata = 11;
  static final int BusTypeSd = 12;
  static final int BusTypeMmc = 13;
  static final int BusTypeVirtual = 14;
  static final int BusTypeFileBackedVirtual = 15;
  static final int BusTypeSpaces = 16;
  static final int BusTypeNvme = 17;
  static final int BusTypeSCM = 18;
  static final int BusTypeUfs = 19;
  static final int BusTypeNvmeof = 20;
  static final int BusTypeMax = 21;
  static final int BusTypeMaxReserved = 127;

  //変数

  //パラメータ
  static boolean hcdAvailable;  //true=ホストのCD-ROMが利用可能。WindowsでCD-ROMドライブがあり操作できる
  static boolean hcdDebugInfo;  //true=デバッグ情報を出力する
  static boolean hcdConnectNext;  //true=次回は接続する
  static boolean hcdConnected;  //true=接続している
  static int hcdSCSIExNext;  //次回の0=内蔵,8=拡張
  static int hcdSCSIEx;  //0=内蔵,8=拡張
  static int hcdSCSIIdNext;  //次回のSCSI ID
  static int hcdSCSIId;  //SCSI ID
  static int hcdUnitNumberNext;  //次回のユニット番号。hcdSCSIExNext+hcdSCSIIdNext
  public static int hcdUnitNumber;  //ユニット番号。hcdSCSIEx+hcdSCSIId
  static int hcdVolumeInt;  //音量(0～100)
  static float hcdVolumeFloat;  //音量(0.0～1.0)

  //メニュー
  static JSpinner hcdIdSpinner;  //SCSI IDスピナー
  static SpinnerNumberModel hcdIdModel;  //SCSI IDスピナーのスピナーモデル
  static JLabel hcdVolumeLabel;  //音量を表示するラベル
  static JSlider hcdVolumeSlider;  //音量スライダー
  public static JMenu hcdMenu;  //メニュー。hcdAvailableのとき有効

  //構造体
  static MemoryLayout TRACK_DATA;
  static MemoryLayout CDROM_TOC;
  static MemoryLayout CDROM_READ_TOC_EX;
  static MemoryLayout RAW_READ_INFO;
  static MemoryLayout STORAGE_DEVICE_DESCRIPTOR;
  static MemoryLayout STORAGE_PROPERTY_QUERY;

  //リンカ
  static Linker linker;
  static MethodHandle downcallHandle (MemorySegment address, FunctionDescriptor function) {
    return linker.downcallHandle (address, function);
  }

  //アリーナ
  static Arena arena;

  //関数
  static MethodHandle CloseHandle;
  static MethodHandle CreateFileA;
  static MethodHandle DeviceIoControl;
  static MethodHandle GetDiskFreeSpaceA;
  static MethodHandle GetDriveTypeA;
  static MethodHandle GetLastError;
  static MethodHandle GetLogicalDrives;
  static MethodHandle QueryDosDeviceA;
  static MethodHandle ReadFile;
  static MethodHandle SetFilePointerEx;

  //CD-ROM
  static int hcdDriveLetter;  //ドライブレター。'E'など
  static String hcdRootPath;  //ルートパス。"E:\\"など
  static String hcdDevicePath;  //デバイスパス。"\\\\.\\E:"など
  public static String hcdDeviceName;  //デバイス名。"E:"など
  static byte[] hcdVendorProduct;  //ベンダーID[4],プロダクトID[16],プロダクトリビジョン[4]

  //動作中フラグ
  static volatile boolean hcdRunning;  //動作中
  static volatile boolean hcdPlaying;  //再生中
  static volatile boolean hcdPausing;  //中断中
  static volatile int hcdAudioStatus;  //0x11=再生中,0x12=中断中,0x13=正常終了,0x14=エラー終了,0x15=情報なし

  //メモリセグメント
  static MemorySegment hcdReadTocEx;
  static MemorySegment hcdToc;
  static MemorySegment hcdBytesReturned;
  static MemorySegment hcdBufferSegment;
  static MemorySegment hcdReadInfo;

  //ハンドル
  static MemorySegment hcdHandle;

  //ソースデータライン
  static SourceDataLine hcdSourceDataLine;

  //再生キュー
  static byte[][] hcdPlayQueueArray;
  static volatile int hcdPlayQueueWrite;
  static volatile int hcdPlayQueueRead;

  //読み出しスレッド
  static Thread hcdReadThread;
  static volatile int hcdStartSector;
  static volatile int hcdCurrentSector;
  static volatile int hcdEndSector;

  //トラック
  static int[] hcdTOCAddressArray;  //TrackDataのAddressの配列。null=未確認
  static int hcdTOCFirstTrack;
  static int hcdTOCLastTrack;

  //CDXA
  static int hcdDataOffset;  //物理セクタの先頭からデータの先頭までのオフセット。-1=未確認

  //再生スレッド
  static Thread hcdPlayThread;

  //コマンド
  static volatile int hcdRequested;  //要求カウンタ
  static volatile int hcdCompleted;  //完了カウンタ
  static volatile int hcdRetrieved;  //回収カウンタ
  static volatile SPC.SCUnit hcdUnit;  //要求したユニット
  static volatile SPC.SPCChip hcdChip;  //要求したインタフェイス
  static volatile byte[] hcdResultBuffer;  //結果のデータ
  static volatile int hcdResultLength;  //結果の長さ
  static volatile int hcdResultSense0;  //結果のセンスデータ[0]
  static volatile int hcdResultSense2;  //結果のセンスデータ[2]
  static volatile int hcdResultStatus;  //結果のステータス
  static volatile int hcdResultMessage;  //結果のメッセージ
  static volatile int hcdBytesPerSector;  //Readで読み出す1セクタのバイト数



  //メソッド

  //hcdInit ()
  //  初期化
  public static void hcdInit () {

    //パラメータ
    hcdAvailable = HCD_ENABLED && XEiJ.prgIsWindows;
    hcdDebugInfo = Settings.sgsGetOnOff ("hcddebug");
    hcdConnectNext = Settings.sgsGetOnOff ("hcdconnect");
    hcdConnected = hcdConnectNext;
    hcdSCSIExNext = Settings.sgsGetOnOff ("hcdscsiex", false) ? 8 : 0;
    hcdSCSIEx = hcdSCSIExNext;
    hcdSCSIIdNext = Settings.sgsGetInt ("hcdscsiid", HCD_DEFAULT_SCSI_ID, 0, 7);
    hcdSCSIId = hcdSCSIIdNext;
    hcdUnitNumberNext = hcdSCSIExNext + hcdSCSIIdNext;
    hcdUnitNumber = hcdUnitNumberNext;
    hcdVolumeInt = Settings.sgsGetInt ("hcdvolume", HCD_DEFAULT_VOLUME);
    if (hcdVolumeInt < 0 || 100 < hcdVolumeInt) {
      hcdVolumeInt = HCD_DEFAULT_VOLUME;
    }
    if (hcdDebugInfo) {
      System.out.printf ("volume=%d\n", hcdVolumeInt);
    }
    hcdVolumeFloat = (float) hcdVolumeInt / 100F;

    //メニュー
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        String command = ae.getActionCommand ();
        switch (command) {
        case "Connect on next execution":
          hcdConnectNext = ((JCheckBoxMenuItem) source).isSelected ();
          break;
        case "Internal SCSI":  //内蔵 SCSI
          hcdSCSIExNext = 0;
          hcdUnitNumberNext = hcdSCSIExNext + hcdSCSIIdNext;
          break;
        case "Expansion SCSI":  //拡張 SCSI
          hcdSCSIExNext = 8;
          hcdUnitNumberNext = hcdSCSIExNext + hcdSCSIIdNext;
          break;
        case "Debug info":
          hcdDebugInfo = ((JCheckBoxMenuItem) source).isSelected ();
          break;
        default:
          System.out.println ("unknown action command " + command);
        }
      }
    };
    ButtonGroup exGroup = new ButtonGroup ();
    hcdMenu = Multilingual.mlnText (
      ComponentFactory.createMenu (
        "Host CD-ROM",
        Multilingual.mlnText (
          ComponentFactory.createCheckBoxMenuItem (hcdConnectNext, "Connect on next execution", listener),
          "ja", "次回の実行時に接続する"),
        Multilingual.mlnText (
          ComponentFactory.createRadioButtonMenuItem (
            exGroup,
            hcdSCSIExNext == 0,
            "Internal SCSI",
            listener),
          "ja", "内蔵 SCSI"),
        Multilingual.mlnText (
          ComponentFactory.createRadioButtonMenuItem (
            exGroup,
            hcdSCSIExNext == 8,
            "Expansion SCSI",
            listener),
          "ja", "拡張 SCSI"),
        ComponentFactory.createHorizontalBox (
          Box.createHorizontalStrut (20),
          ComponentFactory.createLabel ("SCSI ID "),
          hcdIdSpinner = ComponentFactory.createNumberSpinner (
            hcdIdModel = new SpinnerNumberModel (hcdSCSIIdNext, 0, 7, 1),
            2,
            new ChangeListener () {
              @Override public void stateChanged (ChangeEvent ce) {
                hcdSCSIIdNext = hcdIdModel.getNumber ().intValue ();
                hcdUnitNumberNext = hcdSCSIExNext + hcdSCSIIdNext;
              }
            }
            ),
          Box.createHorizontalGlue ()
          ),
        ComponentFactory.createHorizontalBox (
          Box.createHorizontalGlue (),
          Multilingual.mlnText (ComponentFactory.createLabel ("Volume "), "ja", "音量 "),
          hcdVolumeLabel = ComponentFactory.createLabel (String.valueOf (hcdVolumeInt)),
          Box.createHorizontalGlue ()
          ),
        ComponentFactory.createHorizontalBox (
          hcdVolumeSlider = ComponentFactory.setPreferredSize (
            ComponentFactory.createHorizontalSlider (
              0, 100, hcdVolumeInt, 10, 1,
              new ChangeListener () {
                @Override public void stateChanged (ChangeEvent ce) {
                  hcdVolumeInt = ((JSlider) ce.getSource ()).getValue ();
                  if (hcdDebugInfo) {
                    System.out.printf ("volume=%d\n", hcdVolumeInt);
                  }
                  hcdVolumeFloat = (float) hcdVolumeInt / 100F;
                  hcdVolumeLabel.setText (String.valueOf (hcdVolumeInt));
                }
              }
              ),
            LnF.lnfFontSize * 18, LnF.lnfFontSize * 2 + 28)
          ),
        ComponentFactory.createHorizontalSeparator (),
        Multilingual.mlnText (
          ComponentFactory.createCheckBoxMenuItem (hcdDebugInfo, "Debug info", listener),
          "ja", "デバッグ情報")
        ),
      "ja", "ホスト CD-ROM");
    hcdMenu.setEnabled (false);

    if (!hcdAvailable) {  //有効になっていないかWindowsでない
      hcdConnected = false;
      return;
    }

    //TRACK_DATA構造体
    //  https://learn.microsoft.com/ja-jp/windows-hardware/drivers/ddi/ntddcdrm/ns-ntddcdrm-_track_data
    TRACK_DATA = MemoryLayout.structLayout (
      ValueLayout.JAVA_BYTE.withName ("Reserved"),  //UCHAR Reserved
      ValueLayout.JAVA_BYTE.withName ("Adr_Control"),  //UCHAR Control:4; UCHAR Adr:4
      ValueLayout.JAVA_BYTE.withName ("TrackNumber"),  //UCHAR TrackNumber
      ValueLayout.JAVA_BYTE.withName ("Reserved1"),  //UCHAR Reserved1
      MemoryLayout.sequenceLayout (4, ValueLayout.JAVA_BYTE).withName ("Address"));  //UCHAR Address[4]

    //CDROM_TOC構造体
    //  https://learn.microsoft.com/ja-jp/windows-hardware/drivers/ddi/ntddcdrm/ns-ntddcdrm-_cdrom_toc
    CDROM_TOC = MemoryLayout.structLayout (
      MemoryLayout.sequenceLayout (2, ValueLayout.JAVA_BYTE).withName ("Length"),  //UCHAR Length[2]
      ValueLayout.JAVA_BYTE.withName ("FirstTrack"),  //UCHAR FirstTrack
      ValueLayout.JAVA_BYTE.withName ("LastTrack"),  //UCHAR LastTrack
      MemoryLayout.sequenceLayout (MAXIMUM_NUMBER_TRACKS, TRACK_DATA).withName ("TrackData"));  //TrackData[MAXIMUM_NUMBER_TRACKS]

    //CDROM_READ_TOC_EX構造体
    CDROM_READ_TOC_EX = MemoryLayout.structLayout (
      ValueLayout.JAVA_BYTE.withName ("Msf_Reserved1_Format"),  //UCHAR Format:4; UCHAR Reserved1:3; UCHAR Msf:1
      ValueLayout.JAVA_BYTE.withName ("SessionTrack"),  //UCHAR SessionTrack
      ValueLayout.JAVA_BYTE.withName ("Reserved2"),  //UCHAR Reserved2
      ValueLayout.JAVA_BYTE.withName ("Reserved3"));  //UCHAR Reserved3

    //RAW_READ_INFO構造体
    //  https://learn.microsoft.com/ja-jp/windows-hardware/drivers/ddi/ntddcdrm/ns-ntddcdrm-__raw_read_info
    RAW_READ_INFO = MemoryLayout.structLayout (
      ValueLayout.JAVA_LONG.withName ("DiskOffset"),  //LARGE_INTEGER DiskOffset
      ValueLayout.JAVA_INT.withName ("SectorCount"),  //ULONG SectorCount
      ValueLayout.JAVA_INT.withName ("TrackMode"));  //TRACK_MODE_TYPE TrackMode

    //STORAGE_DEVICE_DESCRIPTOR構造体
    //  https://learn.microsoft.com/ja-jp/windows/win32/api/winioctl/ns-winioctl-storage_device_descriptor
    //  https://learn.microsoft.com/ja-jp/windows/win32/api/winioctl/ne-winioctl-storage_bus_type
    STORAGE_DEVICE_DESCRIPTOR = MemoryLayout.structLayout (
      ValueLayout.JAVA_INT.withName ("Version"),  //0 DWORD Version
      ValueLayout.JAVA_INT.withName ("Size"),  //4 DWORD Size
      ValueLayout.JAVA_BYTE.withName ("DeviceType"),  //8 BYTE DeviceType
      ValueLayout.JAVA_BYTE.withName ("DeviceTypeModifier"),  //9 BYTE DeviceTypeModifier
      ValueLayout.JAVA_BYTE.withName ("RemovableMedia"),  //10 BOOLEAN RemovableMedia
      ValueLayout.JAVA_BYTE.withName ("CommandQueueing"),  //11 BOOLEAN CommandQueueing
      ValueLayout.JAVA_INT.withName ("VendorIdOffset"),  //12 DWORD VendorIdOffset
      ValueLayout.JAVA_INT.withName ("ProductIdOffset"),  //16 DWORD ProductIdOffset
      ValueLayout.JAVA_INT.withName ("ProductRevisionOffset"),  //20 DWORD ProductRevisionOffset
      ValueLayout.JAVA_INT.withName ("SerialNumberOffset"),  //24 DWORD SerialNumberOffset
      ValueLayout.JAVA_INT.withName ("BusType"),  //28 STORAGE_BUS_TYPE BusType
      ValueLayout.JAVA_INT.withName ("RawPropertiesLength"),  //32 DWORD RawPropertiesLength
      MemoryLayout.sequenceLayout (1, ValueLayout.JAVA_BYTE).withName ("RawDeviceProperties"),  //36 BYTE RawDeviceProperties[1]
      MemoryLayout.paddingLayout (3)  //37
      //40
      );

    //STORAGE_PROPERTY_QUERY構造体
    //  https://learn.microsoft.com/ja-jp/windows-hardware/drivers/ddi/ntddstor/ni-ntddstor-ioctl_storage_query_property
    //  https://learn.microsoft.com/ja-jp/windows-hardware/drivers/ddi/ntddstor/ns-ntddstor-_storage_property_query
    //  https://learn.microsoft.com/ja-jp/windows-hardware/drivers/ddi/ntddstor/ne-ntddstor-storage_property_id
    //  https://learn.microsoft.com/ja-jp/windows-hardware/drivers/ddi/ntddstor/ne-ntddstor-_storage_query_type
    STORAGE_PROPERTY_QUERY = MemoryLayout.structLayout (
      ValueLayout.JAVA_INT.withName ("PropertyId"),  //0 STORAGE_PROPERTY_ID PropertyId
      ValueLayout.JAVA_INT.withName ("QueryType"),  //4 STORAGE_QUERY_TYPE QueryType
      MemoryLayout.sequenceLayout (1, ValueLayout.JAVA_BYTE).withName ("AdditionalParameters"),  //8 UCHAR AdditionalParameters[1]
      MemoryLayout.paddingLayout (3)  //9
      //12
      );

    //リンカ
    linker = Linker.nativeLinker ();

    //アリーナ
    arena = Arena.ofAuto ();

    //ライブラリ
    SymbolLookup kernel32 = SymbolLookup.libraryLookup ("kernel32", arena);

    //関数
    try {

      //CloseHandle関数
      //  https://learn.microsoft.com/ja-jp/windows/win32/api/handleapi/nf-handleapi-closehandle
      CloseHandle = downcallHandle (
        kernel32.findOrThrow ("CloseHandle"),
        FunctionDescriptor.of (
          ValueLayout.JAVA_INT,  //BOOL
          ValueLayout.ADDRESS));  //HANDLE hObject

      //CreateFileA関数
      //  https://learn.microsoft.com/ja-jp/windows/win32/api/fileapi/nf-fileapi-createfilea
      CreateFileA = downcallHandle (
        kernel32.findOrThrow ("CreateFileA"),
        FunctionDescriptor.of (
          ValueLayout.ADDRESS,  //HANDLE
          ValueLayout.ADDRESS,  //LPCSTR lpFileName
          ValueLayout.JAVA_INT,  //DWORD dwDesiredAccess
          ValueLayout.JAVA_INT,  //DWORD dwShareMode
          ValueLayout.ADDRESS,  //LPSECURITY_ATTRIBUTES lpSecurityAttributes
          ValueLayout.JAVA_INT,  //DWORD dwCreationDisposition
          ValueLayout.JAVA_INT,  //DWORD dwFlagsAndAttributes
          ValueLayout.ADDRESS));  //HANDLE hTemplateFile

      //DeviceIoControl関数
      //  https://learn.microsoft.com/ja-jp/windows/win32/api/ioapiset/nf-ioapiset-deviceiocontrol
      DeviceIoControl = downcallHandle (
        kernel32.findOrThrow ("DeviceIoControl"),
        FunctionDescriptor.of (
          ValueLayout.JAVA_INT,  //BOOL
          ValueLayout.ADDRESS,  //HANDLE hDevice
          ValueLayout.JAVA_INT,  //DWORD dwIoControlCode
          ValueLayout.ADDRESS,  //LPVOID lpInBuffer
          ValueLayout.JAVA_INT,  //DWORD nInBufferSize
          ValueLayout.ADDRESS,  //LPVOID lpOutBuffer
          ValueLayout.JAVA_INT,  //DWORD nOutBufferSize
          ValueLayout.ADDRESS,  //LPDWORD lpBytesReturned
          ValueLayout.ADDRESS));  //LPOVERLAPPED lpOverlapped

      //GetDiskFreeSpaceA関数
      //  https://learn.microsoft.com/ja-jp/windows/win32/api/fileapi/nf-fileapi-getdiskfreespacea
      GetDiskFreeSpaceA = downcallHandle (
        kernel32.findOrThrow ("GetDiskFreeSpaceA"),
        FunctionDescriptor.of (
          ValueLayout.JAVA_INT,  //BOOL
          ValueLayout.ADDRESS,  //LPCSTR lpRootPathName
          ValueLayout.ADDRESS,  //LPDWORD lpSectorsPerCluster
          ValueLayout.ADDRESS,  //LPDWORD lpBytesPerSector
          ValueLayout.ADDRESS,  //LPDWORD lpNumberOfFreeClusters
          ValueLayout.ADDRESS));  //LPDWORD lpTotalNumberOfClusters

      //GetDriveTypeA関数
      //  https://learn.microsoft.com/ja-jp/windows/win32/api/fileapi/nf-fileapi-getdrivetypea
      GetDriveTypeA = downcallHandle (
        kernel32.findOrThrow ("GetDriveTypeA"),
        FunctionDescriptor.of (
          ValueLayout.JAVA_INT,  //UINT
          ValueLayout.ADDRESS));  //LPCWSTR lpRootPathName

      //GetLastError関数
      //  https://learn.microsoft.com/ja-jp/windows/win32/api/errhandlingapi/nf-errhandlingapi-getlasterror
      GetLastError = downcallHandle (
        kernel32.findOrThrow ("GetLastError"),
        FunctionDescriptor.of (
          ValueLayout.JAVA_INT));  //DWORD

      //GetLogicalDrives関数
      //  https://learn.microsoft.com/ja-jp/windows/win32/api/fileapi/nf-fileapi-getlogicaldrives
      GetLogicalDrives = downcallHandle (
        kernel32.findOrThrow ("GetLogicalDrives"),
        FunctionDescriptor.of (
          ValueLayout.JAVA_INT));  //DWORD

      //QueryDosDeviceA
      //  https://learn.microsoft.com/ja-jp/windows/win32/api/winbase/nf-winbase-querydosdevicea
      QueryDosDeviceA = downcallHandle (
        kernel32.findOrThrow ("QueryDosDeviceA"),
        FunctionDescriptor.of (
          ValueLayout.JAVA_INT,  //DWORD
          ValueLayout.ADDRESS,  //LPCSTR lpDeviceName
          ValueLayout.ADDRESS,  //LPSTR lpTargetPath
          ValueLayout.JAVA_INT));  //DWORD ucchMax

      //ReadFile関数
      //  https://learn.microsoft.com/ja-jp/windows/win32/api/fileapi/nf-fileapi-readfile
      ReadFile = downcallHandle (
        kernel32.findOrThrow ("ReadFile"),
        FunctionDescriptor.of (
          ValueLayout.JAVA_INT,  //BOOL
          ValueLayout.ADDRESS,  //HANDLE hFile
          ValueLayout.ADDRESS,  //LPVOID lpBuffer
          ValueLayout.JAVA_INT,  //DWORD nNumberOfBytesToRead
          ValueLayout.ADDRESS,  //LPDWORD lpNumberOfBytesRead
          ValueLayout.ADDRESS));  //LPOVERLAPPED lpOverlapped

      //SetFilePointerEx関数
      //  https://learn.microsoft.com/ja-jp/windows/win32/api/fileapi/nf-fileapi-setfilepointerex
      SetFilePointerEx = downcallHandle (
        kernel32.findOrThrow ("SetFilePointerEx"),
        FunctionDescriptor.of (
          ValueLayout.JAVA_INT,  //BOOL
          ValueLayout.ADDRESS,  //HANDLE hFile
          ValueLayout.JAVA_LONG,  //LARGE_INTEGER liDistanceToMove
          ValueLayout.ADDRESS,  //PLARGE_INTEGER lpNewFilePointer
          ValueLayout.JAVA_INT));  //DWORD dwMoveMethod

    } catch (NoSuchElementException nsee) {  //操作できない
      if (hcdDebugInfo) {
        nsee.printStackTrace ();
      }
      hcdAvailable = false;
      hcdConnected = false;
      return;
    }

    //CD-ROMドライブを探す
    hcdDriveLetter = 0;
    hcdRootPath = null;
    hcdDevicePath = null;
    hcdDeviceName = null;
    try {
      int error;
      int logicalDrives = 0;
      if ((logicalDrives = (int) GetLogicalDrives.invoke ()) == 0 &&
          (error = (int) GetLastError.invoke ()) != -1) {
        if (hcdDebugInfo) {
          System.out.printf ("GetLogicalDrives returned error %d\n",
                             error);
        }
        hcdAvailable = false;
        hcdConnected = false;
        return;
      }
      for (int driveLetter = 'A'; driveLetter <= 'Z'; driveLetter++) {
        if ((logicalDrives & (1 << (driveLetter - 'A'))) != 0) {
          String rootPath = String.format ("%c:\\", driveLetter);
          if ((int) GetDriveTypeA.invoke (
            arena.allocateFrom (rootPath)) == DRIVE_CDROM) {  //LPCWSTR lpRootPathName
            hcdDriveLetter = driveLetter;
            hcdRootPath = rootPath;
            break;
          }
        }
      }  //for
      if (hcdDriveLetter == 0) {  //CD-ROMドライブが見つからない
        if (hcdDebugInfo) {
          System.out.println ("CD-ROM drive not found");
        }
        hcdAvailable = false;
        hcdConnected = false;
        return;
      }
      hcdDevicePath = String.format ("\\\\.\\%c:", hcdDriveLetter);
      hcdDeviceName = String.format ("%c:", hcdDriveLetter);
      if (hcdDebugInfo) {
        System.out.printf ("CD-ROM drive is %s\n",
                           hcdDeviceName);
      }

      //ベンダー、プロダクト、リビジョンの取得を試みる
      //  メディアが入っていない場合があることに注意する
      MemorySegment handle;
      if ((handle = (MemorySegment) CreateFileA.invoke (
        arena.allocateFrom (hcdDevicePath),  //LPCSTR lpFileName
        0,  //DWORD dwDesiredAccess
        FILE_SHARE_READ | FILE_SHARE_WRITE,  //DWORD dwShareMode
        MemorySegment.NULL,  //LPSECURITY_ATTRIBUTES lpSecurityAttributes
        OPEN_EXISTING,  //DWORD dwCreationDisposition
        0,   //DWORD dwFlagsAndAttributes
        MemorySegment.NULL)).address () == INVALID_HANDLE_VALUE &&  //hcdHandle hTemplateFile
          (error = (int) GetLastError.invoke ()) != -1) {
        if (hcdDebugInfo) {
          System.out.printf ("CreateFileA returned error %d\n",
                             error);
        }
      } else {
        MemorySegment query = arena.allocate (STORAGE_PROPERTY_QUERY);
        MemorySegment descriptor = arena.allocate (1024);  //STORAGE_DEVICE_DESCRIPTOR
        MemorySegment bytesReturned = arena.allocate (ValueLayout.JAVA_INT);
        query.set (ValueLayout.JAVA_INT,
                   STORAGE_PROPERTY_QUERY.byteOffset (MemoryLayout.PathElement.groupElement ("PropertyId")),
                   StorageDeviceProperty);
        query.set (ValueLayout.JAVA_INT,
                   STORAGE_PROPERTY_QUERY.byteOffset (MemoryLayout.PathElement.groupElement ("QueryType")),
                   PropertyStandardQuery);
        if ((int) DeviceIoControl.invoke (
          handle,  //hcdHandle hDevice
          IOCTL_STORAGE_QUERY_PROPERTY,  //DWORD dwIoControlCode
          query,  //LPVOID lpInBuffer
          (int) query.byteSize (),  //DWORD nInBufferSize
          descriptor,  //LPVOID lpOutBuffer
          (int) descriptor.byteSize (),  //DWORD nOutBufferSize
          bytesReturned,  //LPDWORD lpBytesReturned
          MemorySegment.NULL) == 0 &&  //LPOVERLAPPED lpOverlapped
            (error = (int) GetLastError.invoke ()) != -1) {
          if (hcdDebugInfo) {
            System.out.printf ("DeviceIoControl IOCTL_STORAGE_QUERY_PROPERTY returned error %d\n",
                               error);
          }
        } else {
          hcdVendorProduct = new byte[8 + 16 + 4];
          Arrays.fill (hcdVendorProduct, (byte) ' ');
          for (int k = 0; k < 3; k++) {
            int o = descriptor.get (ValueLayout.JAVA_INT,
                                    STORAGE_DEVICE_DESCRIPTOR.byteOffset (MemoryLayout.PathElement.groupElement (
                                      k == 0 ? "VendorIdOffset" :
                                      k == 1 ? "ProductIdOffset" :
                                      "ProductRevisionOffset")));  //入力開始位置
            if (o != 0) {
              int p = k == 0 ? 0 : k == 1 ? 8 : 8 + 16;  //出力開始位置
              int l = k == 0 ? 8 : k == 1 ? 16 : 4;  //長さ
              for (int i = 0; i < l; i++) {
                int c = 0xff & descriptor.get (ValueLayout.JAVA_BYTE, o++);
                if (c == 0) {
                  break;
                }
                hcdVendorProduct[p++] = (byte) (0x20 <= c && c <= 0x7e ? c : '?');
              }
            }
          }
          if (hcdDebugInfo) {
            System.out.print ("VendorProduct is ");
            for (int i = 0; i < 8 + 16 + 4; i++) {
              System.out.printf ("%c", 0xff & hcdVendorProduct[i]);
            }
            System.out.println ();
            int BusType = descriptor.get (ValueLayout.JAVA_INT,
                                          STORAGE_DEVICE_DESCRIPTOR.byteOffset (MemoryLayout.PathElement.groupElement ("BusType")));
            System.out.printf ("BusType is %s\n",
                               BusType == BusTypeUnknown ? "BusTypeUnknown" :
                               BusType == BusTypeScsi ? "BusTypeScsi" :
                               BusType == BusTypeAtapi ? "BusTypeAtapi" :
                               BusType == BusTypeAta ? "BusTypeAta" :
                               BusType == BusType1394 ? "BusType1394" :
                               BusType == BusTypeSsa ? "BusTypeSsa" :
                               BusType == BusTypeFibre ? "BusTypeFibre" :
                               BusType == BusTypeUsb ? "BusTypeUsb" :
                               BusType == BusTypeRAID ? "BusTypeRAID" :
                               BusType == BusTypeiScsi ? "BusTypeiScsi" :
                               BusType == BusTypeSas ? "BusTypeSas" :
                               BusType == BusTypeSata ? "BusTypeSata" :
                               BusType == BusTypeSd ? "BusTypeSd" :
                               BusType == BusTypeMmc ? "BusTypeMmc" :
                               BusType == BusTypeVirtual ? "BusTypeVirtual" :
                               BusType == BusTypeFileBackedVirtual ? "BusTypeFileBackedVirtual" :
                               BusType == BusTypeSpaces ? "BusTypeSpaces" :
                               BusType == BusTypeNvme ? "BusTypeNvme" :
                               BusType == BusTypeSCM ? "BusTypeSCM" :
                               BusType == BusTypeUfs ? "BusTypeUfs" :
                               BusType == BusTypeNvmeof ? "BusTypeNvmeof" :
                               BusType == BusTypeMax ? "BusTypeMax" :
                               BusType == BusTypeMaxReserved ? "BusTypeMaxReserved" :
                               String.valueOf (BusType));
          }
        }
        CloseHandle.invoke (handle);
      }

    } catch (Throwable e) {  //操作できない
      if (hcdDebugInfo) {
        e.printStackTrace ();
      }
      hcdAvailable = false;
      hcdConnected = false;
      return;
    }

    hcdMenu.setEnabled (true);

    //動作中フラグ
    hcdRunning = true;

    //メモリセグメント
    hcdReadTocEx = arena.allocate (CDROM_READ_TOC_EX);
    hcdToc = arena.allocate (CDROM_TOC);
    hcdBytesReturned = arena.allocate (ValueLayout.JAVA_INT);
    hcdBufferSegment = arena.allocate (HCD_PLAY_BYTES);
    hcdReadInfo = arena.allocate (RAW_READ_INFO);

    //ハンドル
    hcdHandle = null;

    //ソースデータライン
    hcdSourceDataLine = null;
    try {
      AudioFormat audioFormat = new AudioFormat (44100F,  //sampleRate
                                                 16,  //sampleSizeInBits
                                                 2,  //channels
                                                 true,  //signed
                                                 false);  //bigEndian
      hcdSourceDataLine = AudioSystem.getSourceDataLine (audioFormat);
      hcdSourceDataLine.open (audioFormat, HCD_PLAY_BYTES * 2);
      hcdSourceDataLine.start ();
    } catch (LineUnavailableException lue) {
      if (hcdDebugInfo) {
        lue.printStackTrace ();
      }
    }

    //再生キュー
    hcdPlayQueueArray = new byte[HCD_PLAY_QUEUE_SIZE][];
    for (int i = 0; i < HCD_PLAY_QUEUE_SIZE; i++) {
      hcdPlayQueueArray[i] = new byte[HCD_PLAY_BYTES];
    }
    hcdPlayQueueWrite = 0;
    hcdPlayQueueRead = 0;

    //読み出しスレッド
    //  動作中のとき繰り返す
    //    コマンドがあるとき
    //      コマンドを実行する
    //    開いていないか再生中でないか中断中か最後まで読み出したかキューが満杯のとき
    //      200ms待つ
    //    さもなくば
    //      400msぶんのデータを読み出してキューに追加する
    hcdReadThread = new Thread (() -> {
      while (hcdRunning) {  //動作中のとき繰り返す
        if (hcdCompleted != hcdRequested) {  //完了カウンタ!=予約カウンタのとき
          hcdCommandComplete_2nd ();  //コマンドを実行する
          hcdCompleted = hcdRequested;
        } else if (hcdHandle == null ||  //開いていないか
                   !hcdPlaying ||  //再生中でないか
                   hcdPausing ||  //中断中か
                   hcdEndSector <= hcdCurrentSector ||  //最後まで読み出したか
                   (hcdPlayQueueWrite - hcdPlayQueueRead) == HCD_PLAY_QUEUE_SIZE) {  //キューが満杯のとき
          try {
            Thread.sleep ((long) (HCD_PLAY_MILLIS / 2));  //200ms待つ
          } catch (InterruptedException ie) {
          }
        } else {  //さもなくば
          //400msぶんのデータを読み出してキューに追加する
          byte[] buffer = hcdPlayQueueArray[hcdPlayQueueWrite & (HCD_PLAY_QUEUE_SIZE - 1)];
          int sectors = Math.min (HCD_PLAY_SECTORS, hcdEndSector - hcdCurrentSector);  //今回読み込むセクタ数
          hcdReadInfo.set (ValueLayout.JAVA_LONG,
                           RAW_READ_INFO.byteOffset (MemoryLayout.PathElement.groupElement ("DiskOffset")),
                           2048L * (long) hcdCurrentSector);
          hcdReadInfo.set (ValueLayout.JAVA_INT,
                           RAW_READ_INFO.byteOffset (MemoryLayout.PathElement.groupElement ("SectorCount")),
                           sectors);
          hcdReadInfo.set (ValueLayout.JAVA_INT,
                           RAW_READ_INFO.byteOffset (MemoryLayout.PathElement.groupElement ("TrackMode")),
                           CDDA);
          try {
            int error;
            if ((int) DeviceIoControl.invoke (
              hcdHandle,  //hcdHandle hDevice
              IOCTL_CDROM_RAW_READ,  //DWORD dwIoControlCode
              hcdReadInfo,  //LPVOID lpInBuffer
              (int) hcdReadInfo.byteSize (),  //DWORD nInBufferSize
              hcdBufferSegment,  //LPVOID lpOutBuffer
              2352 * sectors,  //DWORD nOutBufferSize
              hcdBytesReturned,  //LPDWORD lpBytesReturned
              MemorySegment.NULL) == 0 &&  //LPOVERLAPPED lpOverlapped
                (error = (int) GetLastError.invoke ()) != -1) {  //読めない
              if (hcdDebugInfo) {
                System.out.printf ("DeviceIoControl IOCTL_CDROM_RAW_READ returned error %d\n",
                                   error);
              }
              hcdPlaying = false;
              hcdAudioStatus = 0x14;  //エラー終了
              Arrays.fill (buffer, (byte) 0);
            }
          } catch (Throwable e) {  //操作できない
            if (hcdDebugInfo) {
              e.printStackTrace ();
            }
            hcdPlaying = false;
            hcdAudioStatus = 0x14;  //エラー終了
            Arrays.fill (buffer, (byte) 0);
          }
          //byte[] array = hcdBufferSegment.toArray (ValueLayout.JAVA_BYTE);  //毎回必要。メモリ消費が激しい
          for (int i = 0; i < 588 * sectors; i++) {
            //int l = array[4 * i + 1] << 8 | (0xff & array[4 * i + 0]);
            //int r = array[4 * i + 3] << 8 | (0xff & array[4 * i + 2]);
            int l = ((int) hcdBufferSegment.get (ValueLayout.JAVA_BYTE, 4 * i + 1) << 8 |
                     (0xff & (int) hcdBufferSegment.get (ValueLayout.JAVA_BYTE, 4 * i + 0)));
            int r = ((int) hcdBufferSegment.get (ValueLayout.JAVA_BYTE, 4 * i + 3) << 8 |
                     (0xff & (int) hcdBufferSegment.get (ValueLayout.JAVA_BYTE, 4 * i + 2)));
            l = Math.max (-32768, Math.min (32767, Math.round ((float) l * hcdVolumeFloat)));
            r = Math.max (-32768, Math.min (32767, Math.round ((float) r * hcdVolumeFloat)));
            buffer[4 * i + 0] = (byte) l;
            buffer[4 * i + 1] = (byte) (l >> 8);
            buffer[4 * i + 2] = (byte) r;
            buffer[4 * i + 3] = (byte) (r >> 8);
          }
          if (sectors < HCD_PLAY_SECTORS) {
            Arrays.fill (buffer,
                         2352 * sectors,  //from
                         HCD_PLAY_BYTES,  //to
                         (byte) 0);
          }
          hcdCurrentSector += sectors;
          hcdPlayQueueWrite++;
        }
      }  //while
    });
    hcdReadThread.start ();

    //再生スレッド
    //  終了するまで繰り返す
    //    再生中でないか中断中か4周目以前でキューが空のとき
    //      200ms待つ
    //    5周目以後でキューが空のとき
    //      エラー終了
    //    ソースデータラインが使用できないとき
    //      400msぶんのデータをキューから取り出して400ms待つ
    //    さもなくば
    //      400msぶんのデータをキューから取り出して再生する。ブロックする
    //      最後まで再生したとき
    //        正常終了
    hcdPlayThread = new Thread (() -> {
      while (hcdRunning) {  //終了するまで繰り返す
        if (!hcdPlaying ||  //再生中でないか
            hcdPausing ||  //中断中か
            (((hcdCurrentSector - hcdStartSector) < HCD_PLAY_SECTORS * HCD_PLAY_QUEUE_SIZE * 2) &&  //4周目以前で
             hcdPlayQueueRead == hcdPlayQueueWrite)) {  //キューが空のとき
          try {
            Thread.sleep ((long) (HCD_PLAY_MILLIS / 2));  //200ms待つ
          } catch (InterruptedException ie) {
          }
        } else if (hcdPlayQueueRead == hcdPlayQueueWrite) {  //5周目以後でキューが空のとき
          hcdPlaying = false;
          hcdAudioStatus = 0x14;  //エラー終了
        } else if (hcdSourceDataLine == null) {  //ソースデータラインが使用できないとき
          //400msぶんのデータをキューから取り出して400ms待つ
          hcdPlayQueueRead++;
          try {
            Thread.sleep ((long) (HCD_PLAY_MILLIS));
          } catch (InterruptedException ie) {
          }
        } else {  //さもなくば
          //400msぶんのデータをキューから取り出して再生する。ブロックする
          hcdSourceDataLine.write (hcdPlayQueueArray[hcdPlayQueueRead & (HCD_PLAY_QUEUE_SIZE - 1)], 0, HCD_PLAY_BYTES);
          hcdPlayQueueRead++;
          if (hcdEndSector <= hcdCurrentSector) {  //最後まで再生したとき
            hcdPlaying = false;
            hcdAudioStatus = 0x13;  //正常終了
          }
        }
      }  //while
    });
    hcdPlayThread.start ();

  }  //hcdInit

  //hcdTini ()
  //  後始末
  public static void hcdTini () {

    //動作中フラグ
    hcdRunning = false;

    //読み出しスレッド
    if (hcdReadThread != null) {
      hcdReadThread.interrupt ();
      try {
        hcdReadThread.join ((long) (HCD_PLAY_MILLIS * 2));
      } catch (InterruptedException ie) {
      }
      hcdReadThread = null;
    }

    //再生スレッド
    if (hcdPlayThread != null) {
      hcdPlayThread.interrupt ();
      try {
        hcdPlayThread.join ((long) (HCD_PLAY_MILLIS * 2));
      } catch (InterruptedException ie) {
      }
      hcdPlayThread = null;
    }

    //ソースデータライン
    if (hcdSourceDataLine != null) {
      hcdSourceDataLine.stop ();
      hcdSourceDataLine.close ();
      hcdSourceDataLine = null;
    }

    //パラメータ
    Settings.sgsPutOnOff ("hcddebug", hcdDebugInfo);
    Settings.sgsPutOnOff ("hcdconnect", hcdConnectNext);
    Settings.sgsPutInt ("hcdscsiid", hcdUnitNumberNext);
    Settings.sgsPutInt ("hcdvolume", hcdVolumeInt);

  }  //hcdTini

  //hcdReset ()
  //  リセット
  public static void hcdReset () {
    if (hcdConnected) {
      hcdRequested = 0;
      hcdCompleted = 0;
      hcdRetrieved = 0;
      hcdTOCAddressArray = null;
      hcdDataOffset = -1;
      hcdUnit = null;
      hcdChip = null;
      hcdResultBuffer = null;
      hcdResultLength = 0;
      hcdResultSense0 = 0;
      hcdResultSense2 = 0;
      hcdResultStatus = SPC.SPC_GOOD;
      hcdResultMessage = SPC.SPC_COMMAND_COMPLETE;
      hcdBytesPerSector = 2048;
      hcdPlaying = false;
      hcdPausing = false;
      hcdAudioStatus = 0x15;  //情報なし
    }
  }  //hcdReset

  //success = hcdOpen ()
  //  開く
  static boolean hcdOpen () {
    if (hcdHandle != null) {  //すでに開いている
      return true;
    }
    if (hcdRootPath == null) {  //CD-ROMドライブがない
      return false;
    }
    try {
      int error;
      //挿入されているか
      MemorySegment sectorsPerCluster = arena.allocate (ValueLayout.JAVA_INT);
      MemorySegment bytesPerSector = arena.allocate (ValueLayout.JAVA_INT);
      MemorySegment numberOfFreeClusters = arena.allocate (ValueLayout.JAVA_INT);
      MemorySegment totalNumberOfClusters = arena.allocate (ValueLayout.JAVA_INT);
      if ((int) GetDiskFreeSpaceA.invoke (
        arena.allocateFrom (hcdRootPath),  //LPCSTR lpRootPathName
        sectorsPerCluster,  //LPDWORD lpSectorsPerCluster
        bytesPerSector,  //LPDWORD lpBytesPerSector
        numberOfFreeClusters,  //LPDWORD lpNumberOfFreeClusters
        totalNumberOfClusters) == 0 &&  //LPDWORD lpTotalNumberOfClusters
          (error = (int) GetLastError.invoke ()) != -1) {
        if (hcdDebugInfo) {
          System.out.printf ("GetDiskFreeSpaceA returned error %d\n",
                             error);
        }
        return false;
      }
      if (hcdDebugInfo) {
        System.out.printf ("sectorsPerCluster=%d\n",
                           sectorsPerCluster.get (ValueLayout.JAVA_INT, 0));
        System.out.printf ("bytesPerSector=%d\n",
                           bytesPerSector.get (ValueLayout.JAVA_INT, 0));
        System.out.printf ("numberOfFreeClusters=%d\n",
                           numberOfFreeClusters.get (ValueLayout.JAVA_INT, 0));
        System.out.printf ("totalNumberOfClusters=%d\n",
                           totalNumberOfClusters.get (ValueLayout.JAVA_INT, 0));
      }
      //開く
      if ((hcdHandle = (MemorySegment) CreateFileA.invoke (
        arena.allocateFrom (hcdDevicePath),  //LPCSTR lpFileName
        GENERIC_READ,  //DWORD dwDesiredAccess
        FILE_SHARE_READ | FILE_SHARE_WRITE,  //DWORD dwShareMode
        MemorySegment.NULL,  //LPSECURITY_ATTRIBUTES lpSecurityAttributes
        OPEN_EXISTING,  //DWORD dwCreationDisposition
        0,   //DWORD dwFlagsAndAttributes
        MemorySegment.NULL)).address () == INVALID_HANDLE_VALUE &&  //hcdHandle hTemplateFile
          (error = (int) GetLastError.invoke ()) != -1) {  //開けない
        if (hcdDebugInfo) {
          System.out.printf ("CreateFileA returned error %d\n",
                             error);
        }
        hcdHandle = null;
        return false;
      }
    } catch (Throwable e) {  //操作できない
      if (hcdDebugInfo) {
        e.printStackTrace ();
      }
      hcdHandle = null;
      return false;
    }
    return true;
  }  //hcdOpen

  //hcdClose ()
  //  閉じる
  static void hcdClose () {
    if (hcdHandle == null) {  //開いていない
      return;
    }
    try {
      CloseHandle.invoke (hcdHandle);
    } catch (Throwable e) {  //操作できない
      if (hcdDebugInfo) {
        e.printStackTrace ();
      }
    }
    hcdHandle = null;
    hcdTOCAddressArray = null;
    hcdDataOffset = -1;
  }  //hcdClose

  //buffer = hcdReadTOC (msf)
  //  TOCを読む
  static byte[] hcdReadTOC (boolean msf) {
    if (!hcdOpen ()) {  //開けない
      return null;
    }
    try {
      int error;
      hcdReadTocEx.set (ValueLayout.JAVA_BYTE,
                        CDROM_READ_TOC_EX.byteOffset (MemoryLayout.PathElement.groupElement ("Msf_Reserved1_Format")),
                        (byte) ((msf ? 1 : 0) << 7 |  //Msf
                                0 << 4 |  //Reserved1
                                CDROM_READ_TOC_EX_FORMAT_TOC));  //Format
      hcdReadTocEx.set (ValueLayout.JAVA_BYTE,
                        CDROM_READ_TOC_EX.byteOffset (MemoryLayout.PathElement.groupElement ("SessionTrack")),
                        (byte) 1);
      hcdReadTocEx.set (ValueLayout.JAVA_BYTE,
                        CDROM_READ_TOC_EX.byteOffset (MemoryLayout.PathElement.groupElement ("Reserved2")),
                        (byte) 0);
      hcdReadTocEx.set (ValueLayout.JAVA_BYTE,
                        CDROM_READ_TOC_EX.byteOffset (MemoryLayout.PathElement.groupElement ("Reserved3")),
                        (byte) 0);
      if ((int) DeviceIoControl.invoke (
        hcdHandle,  //hcdHandle hDevice
        IOCTL_CDROM_READ_TOC_EX,  //DWORD dwIoControlCode
        hcdReadTocEx,  //LPVOID lpInBuffer
        (int) hcdReadTocEx.byteSize (),  //DWORD nInBufferSize
        hcdToc,  //LPVOID lpOutBuffer
        (int) hcdToc.byteSize (),  //DWORD nOutBufferSize
        hcdBytesReturned,  //LPDWORD lpBytesReturned
        MemorySegment.NULL) == 0 &&  //LPOVERLAPPED lpOverlapped
          (error = (int) GetLastError.invoke ()) != -1) {  //読めない
        if (hcdDebugInfo) {
          System.out.printf ("DeviceIoControl IOCTL_CDROM_READ_TOC_EX returned error %d\n",
                             error);
        }
        hcdClose ();
        return (error == 0 ||
                error == ERROR_NOT_READY ||
                error == ERROR_WRONG_DISK ? null :
                new byte[0]);
      }
      int Length_0 = (0xff & (int) (hcdToc.get (ValueLayout.JAVA_BYTE,
                                                CDROM_TOC.byteOffset (MemoryLayout.PathElement.groupElement ("Length"),
                                                                      MemoryLayout.PathElement.sequenceElement (0)))));
      int Length_1 = (0xff & (int) (hcdToc.get (ValueLayout.JAVA_BYTE,
                                                CDROM_TOC.byteOffset (MemoryLayout.PathElement.groupElement ("Length"),
                                                                      MemoryLayout.PathElement.sequenceElement (1)))));
      int Length = 256 * Length_0 + Length_1;
      byte[] buffer = new byte[2 + Length];
      for (int i = 0; i < buffer.length; i++) {
        buffer[i] = hcdToc.get (ValueLayout.JAVA_BYTE, (long) i);
      }
      if (false) {
        for (int i = 0; i < buffer.length; i++) {
          System.out.printf ("%02x ", 0xff & buffer[i]);
        }
        System.out.println ();
      }
      return buffer;
    } catch (Throwable e) {  //操作できない
      if (hcdDebugInfo) {
        e.printStackTrace ();
      }
      return null;
    }
  }  //hcdReadTOC

  //hcdPlay (startSector, endSector)
  //  再生を開始する
  static void hcdPlay (int startSector, int endSector) {
    if (!hcdPlaying) {  //再生していない
      if (!hcdOpen ()) {  //開けない
        return;
      }
    }
    //すでに再生しているときは上書きする
    //中断中のデータがキューに残っていると上書き後に再生されてしまうので読み出し位置を進める
    hcdPlayQueueRead = hcdPlayQueueWrite;
    hcdStartSector = startSector;
    hcdCurrentSector = startSector;
    hcdEndSector = endSector;
    hcdPlaying = true;
    hcdPausing = false;
    hcdAudioStatus = 0x11;  //再生中
  }  //hcdPlay

  //hcdPause ()
  //  再生を中断する
  static void hcdPause () {
    if (!hcdPlaying) {  //再生していない
      return;
    }
    hcdPausing = true;
    hcdAudioStatus = 0x12;  //中断中
  }  //hcdPause

  //hcdResume ()
  //  再生を再開する
  static void hcdResume () {
    if (!hcdPlaying) {  //再生していない
      return;
    }
    hcdStartSector = hcdCurrentSector;  //再開した位置を開始時刻にする。1周目からやり直さないとアンダーランで停止してしまう
    hcdPausing = false;
    hcdAudioStatus = 0x11;  //再生中
  }  //hcdResume

  //hcdSetBytesPerSector (bytesPerSector)
  //  セクタの長さを設定する
  static void hcdSetBytesPerSector (int bytesPerSector) {
    if (bytesPerSector == 2048 ||
        bytesPerSector == 2336 ||
        bytesPerSector == 2352) {
      hcdBytesPerSector = bytesPerSector;
      if (hcdDebugInfo) {
        System.out.printf ("bytesPerSector=%d\n", bytesPerSector);
      }
    }
  }  //hcdSetBytesPerSector

  //buffer = hcdRead (startSector, sectors)
  //  セクタを読む
  static byte[] hcdRead (int startSector, int sectors) {
    if (hcdDebugInfo) {
      System.out.printf ("hcdRead(%d,%d)\n", startSector, sectors);
    }
    if (!hcdOpen ()) {  //開けない
      return null;
    }
    byte[] buffer = new byte[hcdBytesPerSector * sectors];
    try {
      int error;
      if (hcdBytesPerSector == 2048) {
        if ((int) SetFilePointerEx.invoke (
          hcdHandle,  //HANDLE hFile
          2048L * (long) startSector,  //LARGE_INTEGER liDistanceToMove
          MemorySegment.NULL,  //PLARGE_INTEGER lpNewFilePointer
          FILE_BEGIN) == 0 &&  //DWORD dwMoveMethod
            (error = (int) GetLastError.invoke ()) != -1) {  //シークできない
          if (hcdDebugInfo) {
            System.out.printf ("SetFilePointerEx returned error %d\n",
                               error);
          }
          hcdClose ();
          return (error == 0 ||
                  error == ERROR_NOT_READY ||
                  error == ERROR_WRONG_DISK ? null :
                  new byte[0]);
        }
      }
      for (int offset = 0; offset < sectors; ) {  //今回読む位置
        int step = Math.min (HCD_PLAY_SECTORS, sectors - offset);  //今回読むセクタ数
        if (hcdBytesPerSector == 2048) {
          if ((int) ReadFile.invoke (
            hcdHandle,  //HANDLE hFile
            hcdBufferSegment,  //LPVOID lpBuffer
            2048 * step,  //DWORD nNumberOfBytesToRead
            hcdBytesReturned,  //LPDWORD lpNumberOfBytesRead
            MemorySegment.NULL) == 0 &&  //LPOVERLAPPED lpOverlapped
              (error = (int) GetLastError.invoke ()) != -1) {  //読めない
            if (hcdDebugInfo) {
              System.out.printf ("ReadFile returned error %d\n",
                                 error);
            }
            hcdClose ();
            return (error == 0 ||
                    error == ERROR_NOT_READY ||
                    error == ERROR_WRONG_DISK ? null :
                    new byte[0]);
          }
        } else {
          hcdReadInfo.set (ValueLayout.JAVA_LONG,
                           RAW_READ_INFO.byteOffset (MemoryLayout.PathElement.groupElement ("DiskOffset")),
                           2048L * (long) (startSector + offset));  //開始位置。2048L以外はエラー87 ERROR_INVALID_PARAMETER
          hcdReadInfo.set (ValueLayout.JAVA_INT,
                           RAW_READ_INFO.byteOffset (MemoryLayout.PathElement.groupElement ("SectorCount")),
                           step);  //セクタ数
          hcdReadInfo.set (ValueLayout.JAVA_INT,
                           RAW_READ_INFO.byteOffset (MemoryLayout.PathElement.groupElement ("TrackMode")),
                           hcdBytesPerSector == 2336 ? YellowMode2 : CDDA);  //モード。手元の環境ではYellowMode2はエラー87 ERROR_INVALID_PARAMETER
          if ((int) DeviceIoControl.invoke (
            hcdHandle,  //hcdHandle hDevice
            IOCTL_CDROM_RAW_READ,  //DWORD dwIoControlCode
            hcdReadInfo,  //LPVOID lpInBuffer
            (int) hcdReadInfo.byteSize (),  //DWORD nInBufferSize
            hcdBufferSegment,  //LPVOID lpOutBuffer
            hcdBytesPerSector * step,  //DWORD nOutBufferSize
            hcdBytesReturned,  //LPDWORD lpBytesReturned
            MemorySegment.NULL) == 0 &&  //LPOVERLAPPED lpOverlapped
              (error = (int) GetLastError.invoke ()) != -1) {  //読めない
            if (hcdDebugInfo) {
              System.out.printf ("DeviceIoControl IOCTL_CDROM_RAW_READ returned error %d\n",
                                 error);
            }
            hcdClose ();
            return (error == 0 ||
                    error == ERROR_NOT_READY ||
                    error == ERROR_WRONG_DISK ? null :
                    new byte[0]);
          }
        }
        for (int i = 0; i < hcdBytesPerSector * step; i++) {
          buffer[hcdBytesPerSector * offset + i] = hcdBufferSegment.get (ValueLayout.JAVA_BYTE, i);
        }
        offset += step;
      }  //for offset
      return buffer;
    } catch (Throwable e) {  //操作できない
      if (hcdDebugInfo) {
        e.printStackTrace ();
      }
      return null;
    }
  }  //hcdRead

  //buffer = hcdReadCapacity ()
  static byte[] hcdReadCapacity () {
    if (!hcdOpen ()) {  //開けない
      return null;
    }
    try {
      int error;
      MemorySegment sectorsPerCluster = arena.allocate (ValueLayout.JAVA_INT);
      MemorySegment bytesPerSector = arena.allocate (ValueLayout.JAVA_INT);
      MemorySegment numberOfFreeClusters = arena.allocate (ValueLayout.JAVA_INT);
      MemorySegment totalNumberOfClusters = arena.allocate (ValueLayout.JAVA_INT);
      if ((int) GetDiskFreeSpaceA.invoke (
        arena.allocateFrom (hcdRootPath),  //LPCSTR lpRootPathName
        sectorsPerCluster,  //LPDWORD lpSectorsPerCluster
        bytesPerSector,  //LPDWORD lpBytesPerSector
        numberOfFreeClusters,  //LPDWORD lpNumberOfFreeClusters
        totalNumberOfClusters) == 0 &&  //LPDWORD lpTotalNumberOfClusters
          (error = (int) GetLastError.invoke ()) != -1) {
        if (hcdDebugInfo) {
          System.out.printf ("GetDiskFreeSpaceA returned error %d\n",
                             error);
        }
        hcdClose ();
        return (error == 0 ||
                error == ERROR_NOT_READY ||
                error == ERROR_WRONG_DISK ? null :
                new byte[0]);
      }
      byte[] buffer = new byte[8];
      ByteArray.byaWl (buffer,
                       0,
                       sectorsPerCluster.get (ValueLayout.JAVA_INT, 0) *
                       totalNumberOfClusters.get (ValueLayout.JAVA_INT, 0) - 1);  //最終論理ブロック
      ByteArray.byaWl (buffer,
                       4,
                       bytesPerSector.get (ValueLayout.JAVA_INT, 0));  //ブロック長
      return buffer;
    } catch (Throwable e) {  //操作できない
      if (hcdDebugInfo) {
        e.printStackTrace ();
      }
      return null;
    }
  }  //hcdReadCapacity

  //hcdEject ()
  //  イジェクトする
  //!!!メディアがないときトレイを出すか出さないか
  static void hcdEject () {
    hcdClose ();
    try {
      int error;
      MemorySegment handle;
      if ((handle = (MemorySegment) CreateFileA.invoke (
        arena.allocateFrom (hcdDevicePath),  //LPCSTR lpFileName
        0,  //DWORD dwDesiredAccess
        FILE_SHARE_READ | FILE_SHARE_WRITE,  //DWORD dwShareMode
        MemorySegment.NULL,  //LPSECURITY_ATTRIBUTES lpSecurityAttributes
        OPEN_EXISTING,  //DWORD dwCreationDisposition
        0,   //DWORD dwFlagsAndAttributes
        MemorySegment.NULL)).address () == INVALID_HANDLE_VALUE &&  //hcdHandle hTemplateFile
          (error = (int) GetLastError.invoke ()) != -1) {
        if (hcdDebugInfo) {
          System.out.printf ("CreateFileA returned error %d\n",
                             error);
        }
        return;
      }
      MemorySegment bytesReturned = arena.allocate (ValueLayout.JAVA_INT);
      if ((int) DeviceIoControl.invoke (
        handle,  //hcdHandle hDevice
        IOCTL_STORAGE_EJECT_MEDIA,  //DWORD dwIoControlCode
        MemorySegment.NULL,  //LPVOID lpInBuffer
        0,  //DWORD nInBufferSize
        MemorySegment.NULL,  //LPVOID lpOutBuffer
        0,  //DWORD nOutBufferSize
        bytesReturned,  //LPDWORD lpBytesReturned
        MemorySegment.NULL) == 0 &&  //LPOVERLAPPED lpOverlapped
          (error = (int) GetLastError.invoke ()) != -1) {
        if (hcdDebugInfo) {
          System.out.printf ("DeviceIoControl IOCTL_STORAGE_EJECT_MEDIA returned error %d\n",
                             error);
        }
      }
      CloseHandle.invoke (handle);
    } catch (Throwable e) {  //操作できない
      if (hcdDebugInfo) {
        e.printStackTrace ();
      }
    }
  }  //hcdEject


  //hcdCommandComplete_1st (unit, oc)
  //  コマンドフェーズの転送が終了した
  public static void hcdCommandComplete_1st (SPC.SCUnit unit, int oc) {
    if (hcdDebugInfo) {
      unit.scuPrintCommand (oc);
    }
    hcdUnit = unit;
    hcdChip = unit.scuChip;
    hcdResultBuffer = null;
    hcdResultLength = 0;
    hcdResultSense0 = 0;
    hcdResultSense2 = 0;
    hcdResultStatus = SPC.SPC_GOOD;
    hcdResultMessage = SPC.SPC_COMMAND_COMPLETE;
    if (!(oc == 0x03 ||  //Request Senseまたは
          oc == 0x12) &&  //Inquiry以外で
        hcdChip.spiLUN != 0) {  //LUNが0でない
      unit.scuNotReady ();
      return;
    }
    switch (oc) {
      //
      //ステータスフェーズへ進むもの
      //
    case 0x01:  //Rezero Unit
      hcdDoRezeroUnit_1st ();
      break;
    case 0x03:  //Request Sense
      unit.scuDoRequestSense ();
      break;
    case 0x12:  //Inquiry
      hcdDoInquiry_1st ();
      break;
    case 0x1a:  //Mode Sense(6)
      hcdDoModeSense6_1st ();
      break;
    case 0x42:  //Read Sub-Channel
      hcdDoReadSubChannel_1st ();
      break;
    case 0x45:  //Play Audio(10)
      hcdDoPlayAudio10_1st ();
      break;
    case 0x47:  //Play Audio MSF
      hcdDoPlayAudioMSF_1st ();
      break;
    case 0x4b:  //Pause Resume
      hcdDoPauseResume_1st ();
      break;
    case 0xa5:  //Play Audio(12)
      hcdDoPlayAudio12_1st ();
      break;
      //
      //データアウトフェーズへ進むもの
      //
    case 0x15:  //Mode Select(6)
      hcdDoModeSelect6_1st ();
      break;
      //
      //読み出しスレッドで処理するもの
      //
    case 0x00:  //Test Unit Ready
    case 0x08:  //Read(6)
    case 0x1b:  //Start-Stop Unit
    case 0x25:  //Read Capacity
    case 0x28:  //Read(10)
    case 0x43:  //Read TOC
    case 0xd8:  //ReadCDDA
      hcdRequested++;
      if (hcdReadThread != null) {
        hcdReadThread.interrupt ();
      }
      break;
      //
      //未実装
      //
    default:
      hcdUnit.scuDoInvalid ();
    }
  }  //hcdCommandComplete_1st


  //hcdCommandComplete_2nd ()
  //  コマンドフェーズの転送が終了した後、読み込みスレッドがコマンドを実行する
  static void hcdCommandComplete_2nd () {
    int oc = hcdChip.spiCommandBuffer[0] & 255;  //オペレーションコード
    switch (oc) {
      //
      //読み出しスレッドで処理するもの
      //
    case 0x00:  //Test Unit Ready
      hcdDoTestUnitReady_2nd ();
      break;
    case 0x08:  //Read(6)
      hcdDoRead6_2nd ();
      break;
    case 0x1b:  //Start-Stop Unit
      hcdDoStartStopUnit_2nd ();
      break;
    case 0x25:  //Read Capacity
      hcdDoReadCapacity_2nd ();
      break;
    case 0x28:  //Read(10)
      hcdDoRead10_2nd ();
      break;
    case 0x43:  //Read TOC
      hcdDoReadTOC_2nd ();
      break;
    case 0xd8:  //ReadCDDA
      hcdDoReadCDDA_2nd ();
      break;
    }
  }  //hcdCommandComplete_2nd


  //hcdDoTestUnitReady_2nd ()
  //  [0]  0x00
  //  [1]  |LUN###|-----|
  //  [5]  |..|----|Flag|Link|
  static void hcdDoTestUnitReady_2nd () {
    if (!hcdOpen ()) {  //開けない
      hcdNotReady ();
      return;
    }
    try {
      int error;
      MemorySegment bytesReturned = arena.allocate (ValueLayout.JAVA_INT);
      if ((int) DeviceIoControl.invoke (
        hcdHandle,  //hcdHandle hDevice
        IOCTL_STORAGE_CHECK_VERIFY,  //DWORD dwIoControlCode
        MemorySegment.NULL,  //LPVOID lpInBuffer
        0,  //DWORD nInBufferSize
        MemorySegment.NULL,  //LPVOID lpOutBuffer
        0,  //DWORD nOutBufferSize
        bytesReturned,  //LPDWORD lpBytesReturned
        MemorySegment.NULL) == 0 &&  //LPOVERLAPPED lpOverlapped
          (error = (int) GetLastError.invoke ()) != -1) {
        if (hcdDebugInfo) {
          System.out.printf ("DeviceIoControl IOCTL_STORAGE_CHECK_VERIFY returned error %d\n",
                             error);
        }
        hcdNotReady ();
        return;
      }
    } catch (Throwable e) {  //操作できない
      if (hcdDebugInfo) {
        e.printStackTrace ();
      }
      hcdNotReady ();
      return;
    }
    hcdGood ();
  }  //hcdDoTestUnitReady_2nd

  //hcdDoRezeroUnit_1st ()
  //  [0]  0x01
  //  [1]  |LUN###|-----|
  //  [5]  |..|----|Flag|Link|
  static void hcdDoRezeroUnit_1st () {
    hcdRequested = 0;
    hcdCompleted = 0;
    hcdRetrieved = 0;
    hcdBytesPerSector = 2048;
    //Rezero Unitは再生を止めない
    hcdChip.spiStatusPhase (SPC.SPC_GOOD, SPC.SPC_COMMAND_COMPLETE);  //エラーなしでステータスフェーズに移行する
  }  //hcdDoRezeroUnit_1st

  //hcdDoRead6_2nd ()
  //  [0]  0x08
  //  [1][2][3]  LUN<<21|論理ブロックアドレス
  //  [4]  論理ブロック数
  //  [5]  |..|----|Flag|Link|
  static void hcdDoRead6_2nd () {
    if (!hcdOpen ()) {  //開けない
      hcdNotReady ();
      return;
    }
    int a = ByteArray.byaRls (hcdChip.spiCommandBuffer, 0) & 0x001fffff;  //論理ブロックアドレス
    int n = hcdChip.spiCommandBuffer[4] & 255;  //論理ブロック数
    if (n == 0) {
      n = 256;
    }
    byte[] buffer = hcdRead (a, n);
    if (buffer == null) {
      hcdNotReady ();
      return;
    }
    if (buffer.length == 0) {
      hcdMediumError ();
      return;
    }
    //データインフェーズに移行する
    hcdResultBuffer = buffer;
    hcdResultLength = buffer.length;
  }  //hcdDoRead6_2nd

  //hcdDoInquiry_1st ()
  //  [0]  0x12
  //  [1]  |LUN###|----|EVPD|
  //  [2]  ページコード
  //  [4]  アロケーション長
  //  [5]  |..|----|Flag|Link|
  static void hcdDoInquiry_1st () {
    boolean evpd = (hcdChip.spiCommandBuffer[1] & 1) != 0;
    if (evpd) {  //VPD情報
      hcdUnit.scuIllegalRequest ();
      return;
    }
    int n = hcdChip.spiCommandBuffer[4] & 255;  //アロケーション長
    byte[] buffer = new byte[36];
    if (hcdChip.spiLUN != 0) {  //LUNが0でない
      buffer[0] = 0x7f;  //指定されたロジカル・ユニットは存在しない
    } else {
      buffer[0] = (byte) (SPC.SPC_CDROM_DEVICE);  //CD-ROMデバイス
      buffer[1] = (byte) (1 << 7);  //0=固定,1=リムーバブル
      buffer[2] = 2;  //ISO/ECMA/ANSIバージョン。SCSI-1/SCSI-2
      buffer[3] = 1;  //レスポンスデータ形式。SCSI-1/SCSI-2
      buffer[4] = 31;  //追加データ長
      buffer[5] = 0;  //予約
      buffer[6] = 0;  //予約
      buffer[7] = 0;  //サポート機能なし
      System.arraycopy (hcdVendorProduct, 0,
                        buffer, 8,
                        8 + 16 + 4);  //ベンダーID[4],プロダクトID[16],プロダクトリビジョン[4]
    }
    if (hcdDebugInfo) {
      hcdDumpBuffer (buffer, buffer.length);
    }
    hcdChip.spiDataInPhase (buffer, 0, Math.min (n, buffer.length), 0);  //データインフェーズに移行する
  }  //hcdDoInquiry_1st

  //hcdDoModeSelect6_1st ()
  //  [0]  0x15
  //  [1]  |LUN###|PF|---|SP|
  //  [4]  パラメータリスト長
  //  [5]  コントロールバイト
  //
  //  パラメータ・リスト
  //    モード・パラメータ・ヘッダ
  //    ブロック・ディスクリプタ
  //      :
  //    パラメータ・ページ
  //      :
  //  モード・パラメータ・ヘッダ
  //    [0]  モード・パラメータ長
  //         Mode Senseのとき(アロケーション長に関係なく)全パラメータリストの長さ。このフィールドを含まない
  //         Mode Selectのとき0x00
  //    [1]  メディア・タイプ
  //    [2]  デバイス固有パラメータ
  //    [3]  ブロック・ディスクリプタ長
  //         ブロック・ディスクリプタのバイト数。0を含む8の倍数
  //  ブロック・ディスクリプタ
  //    [0]  デンシティ・コード
  //    [1][2][3]  ブロック数
  //    [5][6][7]  ブロック長
  //  パラメータ・ページ
  //    [0]  |PS|-|ページ・コード######|
  //    [1]  ページ長n-1
  //    [2]～[n]  モード・パラメータ
  static void hcdDoModeSelect6_1st () {
    int n = hcdChip.spiCommandBuffer[4] & 255;  //パラメータリスト長
    hcdChip.spiDataOutPhase (hcdChip.spiDataOutBuffer, 0, n, 0);  //データアウトフェーズに移行する
  }  //hcdDoModeSelect6_1st

  //hcdDoModeSense6_1st ()
  //  [0]  0x1a
  //  [1]  |LUN###|R|DBD|---|
  //  [2]  |PC##|ページコード######|
  //  [4]  アロケーション長
  //  [5]  |..|----|Flag|Link|
  static void hcdDoModeSense6_1st () {
    boolean dbd = (hcdChip.spiCommandBuffer[1] & 8) != 0;
    int pc = (hcdChip.spiCommandBuffer[2] >> 6) & 3;  //0=カレント値,1=変更可能,2=デフォルト値,3=セーブ値
    if (pc == 3) {  //セーブ値
      hcdUnit.scuIllegalRequest ();
      return;
    }
    int pageCode = hcdChip.spiCommandBuffer[2] & 63;
    int n = hcdChip.spiCommandBuffer[4] & 255;
    //  0x01  リード・エラー・リカバリ・パラメータ p253
    //        [0]  |PS|-|ページコード0x01######|
    //        [1]  ページ長0x06
    //        [2]  エラー・リカバリ・パラメータ
    //             |--|TB|RC|-|PER|DTE|DCR|
    //             TB  回復できなくても転送する
    //             RC  回復が必要でも中断しない
    //             PER  回復したエラーを報告する
    //             DTE  回復した時点で終了する
    //             DCR  ECCによる訂正を禁止する
    //        [3]  リード・リトライ回数
    //        [4][5][6][7]
    //  0x03  フォーマット・パラメータ p199
    //  0x04  ドライブ・パラメータ p200
    //  0x0d  CD-ROMデバイス・パラメータ p255
    //  0x0e  CD-ROMオーディオ・コントロール・パラメータ p255
    //  0x3f  全パラメータ・ページの報告
    if (pageCode != 0x01 &&
        pageCode != 0x03 &&
        pageCode != 0x0e &&
        pageCode != 0x31 &&
        pageCode != 0x3f) {
      hcdUnit.scuIllegalRequest ();
      return;
    }
    byte[] buffer = new byte[256];
    //モード・パラメータ・ヘッダ
    buffer[0] = 3;  //[0]  モード・パラメータ長(Sense時のみ)
    buffer[1] = 0x00;  //[1]  メディア・タイプ
    buffer[2] = 0x00;  //[2]  デバイス固有パラメータ
    buffer[3] = 0;  //[3]  ブロック・ディスクリプタ長
    int length = 4;
    if (!dbd) {
      //ブロック・ディスクリプタ
      if (pc == 0) {  //カレント値
        buffer[length + 6] = (byte) (hcdBytesPerSector >> 8);
        buffer[length + 7] = (byte) hcdBytesPerSector;  //[5][6][7]  ブロック長
      } else if (pc == 1) {  //変更可能
        buffer[length + 6] = -1;
        buffer[length + 7] = -1;  //[5][6][7]  ブロック長
      } else {  //デフォルト値
        buffer[length + 6] = (byte) (2048 >> 8);
        buffer[length + 7] = (byte) 2048;  //[5][6][7]  ブロック長
      }
      buffer[0] += 8;  //[0]  モード・パラメータ長(Sense時のみ)
      buffer[3] += 8;  //[3]  ブロック・ディスクリプタ長
      length += 8;
    }
    if (pageCode == 0x01 ||  //リード・エラー・リカバリ・パラメータ
        pageCode == 0x3f) {  //全パラメータ・ページの報告
      //リード・エラー・リカバリ・パラメータ
      buffer[length + 0] = 0x01;  //[0]  ページ・コード
      buffer[length + 1] = 6;  //[1]  ページ長
      if (pc == 0) {  //カレント値
        buffer[length + 2] = 0x11;  //[2]  リード・エラー・リカバリ・パラメータ。エラー無視、訂正はCIRCのみ
        buffer[length + 3] = 0x00;  //[3]  リード・リトライ回数
      } else if (pc == 1) {  //変更可能
        buffer[length + 2] = 0;  //[2]  リード・エラー・リカバリ・パラメータ。エラー無視、訂正はCIRCのみ
        buffer[length + 3] = 0;  //[3]  リード・リトライ回数
      } else {  //デフォルト値
        buffer[length + 2] = 0x11;  //[2]  リード・エラー・リカバリ・パラメータ。エラー無視、訂正はCIRCのみ
        buffer[length + 3] = 0x00;  //[3]  リード・リトライ回数
      }
      buffer[0] += 8;  //[0]  モード・パラメータ長(Sense時のみ)
      length += 8;
    }
    if (pageCode == 0x03 ||  //フォーマット・パラメータ
        pageCode == 0x3f) {  //全パラメータ・ページの報告
      buffer[length + 0] = 0x03;  //[0]  ページ・コード
      buffer[length + 1] = 22;  //[1]  ページ長
      if (pc == 0) {  //カレント値
        buffer[length + 20] = 0x20;  //[20]  RMB
      } else if (pc == 1) {  //変更可能
        buffer[length + 20] = 0;  //[20]  RMB
      } else {  //デフォルト値
        buffer[length + 20] = 0x20;  //[20]  RMB
      }
      buffer[0] += 24;  //[0]  モード・パラメータ長(Sense時のみ)
      length += 24;
    }
    if (pageCode == 0x0e ||  //CD-ROMオーディオ・コントロール・パラメータ
        pageCode == 0x3f) {  //全パラメータ・ページの報告
      buffer[length + 0] = 0x0e;  //[0]  ページ・コード
      buffer[length + 1] = 14;  //[1]  ページ長
      if (pc == 0) {  //カレント値
        buffer[length + 9] =
          buffer[length + 11] =
            buffer[length + 13] =
              buffer[length + 15] = (byte) (hcdVolumeInt * 255 / 100);  //[9][11][13][15]  ボリューム
      } else if (pc == 1) {  //変更可能
        buffer[length + 9] =
          buffer[length + 11] =
            buffer[length + 13] =
              buffer[length + 15] = -1;
      } else {  //デフォルト値
        buffer[length + 9] =
          buffer[length + 11] =
            buffer[length + 13] =
              buffer[length + 15] = (byte) (HCD_DEFAULT_VOLUME * 255 / 100);  //[9][11][13][15]  ボリューム
      }
      length += 16;
    }
    if (pageCode == 0x31 ||  //転送速度
        pageCode == 0x3f) {  //全パラメータ・ページの報告
      buffer[length + 0] = 0x31;  //[0]  ページ・コード
      buffer[length + 1] = 2;  //[1]  ページ長
      if (pc == 0) {  //カレント値
        buffer[length + 2] = 24;  //[2]  転送速度
      } else if (pc == 1) {  //変更可能
        buffer[length + 2] = 0;  //[2]  転送速度
      } else {  //デフォルト値
        buffer[length + 2] = 24;  //[2]  転送速度
      }
      buffer[0] += 4;  //[0]  モード・パラメータ長(Sense時のみ)
      length += 4;
    }
    if (hcdDebugInfo) {
      hcdDumpBuffer (buffer, length);
    }
    hcdChip.spiDataInPhase (buffer, 0, Math.min (n, length), 0);  //データインフェーズに移行する
  }  //hcdDoModeSense6_1st

  //hcdDoStartStopUnit_2nd ()
  //  [0]  0x1b
  //  [1]  |LUN###|-----|
  //  [4]  |------|LoEj|Start|
  //  [5]  |..|----|Flag|Link|
  static void hcdDoStartStopUnit_2nd () {
    int loejStart = hcdChip.spiCommandBuffer[4] & 3;  //LoEj|Start
    if (loejStart == 2) {  //イジェクト
      //!!!再生終了
      hcdEject ();
    }
    hcdGood ();
  }  //hcdDoStartStopUnit_2nd

  //hcdDoReadCapacity_2nd ()
  //  [0]  0x25
  //  [1]  |LUN###|----|RelAdr|
  //  [2][3][4][5]  論理ブロックアドレス
  //  [8]  |-------|PMI|
  //  [9]  |..|----|Flag|Link|
  static void hcdDoReadCapacity_2nd () {
    if (!hcdOpen ()) {  //開けない
      hcdNotReady ();
      return;
    }
    byte[] buffer = hcdReadCapacity ();
    if (buffer == null) {
      hcdNotReady ();
      return;
    }
    if (buffer.length == 0) {
      hcdMediumError ();
      return;
    }
    //データインフェーズに移行する
    hcdResultBuffer = buffer;
    hcdResultLength = buffer.length;
  }  //hcdDoReadCapacity_2nd

  //hcdDoRead10_2nd ()
  //  [0]  0x28
  //  [1]  |LUN###|DPO|FUA|--|RelAdr|
  //  [2][3][4][5]  論理ブロックアドレス
  //  [7][8]  論理ブロック数
  //  [9]  |..|----|Flag|Link|
  static void hcdDoRead10_2nd () {
    if (!hcdOpen ()) {  //開けない
      hcdNotReady ();
      return;
    }
    int a = ByteArray.byaRls (hcdChip.spiCommandBuffer, 2);  //論理ブロックアドレス
    int n = ByteArray.byaRwz (hcdChip.spiCommandBuffer, 7);  //論理ブロック数
    if (n == 0) {
      hcdGood ();
      return;
    }
    byte[] buffer = hcdRead (a, n);
    if (buffer == null) {
      hcdNotReady ();
      return;
    }
    if (buffer.length == 0) {
      hcdMediumError ();
      return;
    }
    //データインフェーズに移行する
    hcdResultBuffer = buffer;
    hcdResultLength = buffer.length;
  }  //hcdDoRead10_2nd

  //hcdDoReadSubChannel_1st ()
  //  [0]  0x42
  //  [1]  |LUN###|---|MSF|-|
  //  [2]  |-|SubQ|------|
  //  [3]  サブチャネル・データ形式
  //  [6]  トラック番号
  //  [7][8]  アロケーション長
  //  [9]  コントロール・バイト
  static void hcdDoReadSubChannel_1st () {
    boolean msf = (hcdChip.spiCommandBuffer[1] & 2) != 0;
    boolean subQ = (hcdChip.spiCommandBuffer[2] & 64) != 0;
    int type = hcdChip.spiCommandBuffer[3] & 255;
    int n = ByteArray.byaRwz (hcdChip.spiCommandBuffer, 7);
    if (subQ &&
        type == 0x01) {  //カレント・ポジション
      int currentSector = 0;  //現在のセクタ
      int currentTrack = hcdTOCFirstTrack;  //現在のセクタのトラックの番号
      int currentOffset = 0;  //現在のセクタのトラックの先頭からのオフセット
      if (hcdPlaying &&  //再生中
          hcdTOCAddressArray != null) {  //TOC情報あり
        currentSector = hcdCurrentSector;
        while (currentTrack + 1 <= hcdTOCLastTrack &&
               hcdTOCAddressArray[currentTrack + 1 - hcdTOCFirstTrack] <= currentSector) {
          currentTrack++;
        }
        currentOffset = currentSector - hcdTOCAddressArray[currentTrack - hcdTOCFirstTrack];
      }
      //
      byte[] buffer = new byte[16];
      buffer[1] = (byte) hcdAudioStatus;  //オーディオ・ステータス
      if (hcdAudioStatus == 0x13 ||  //正常終了または
          hcdAudioStatus == 0x14) {  //エラー終了のとき
        hcdAudioStatus = 0x15;  //情報なし
      }
      buffer[2] = 0;  //サブチャネル・データ長
      buffer[3] = 12;
      buffer[3] = 0x01;  //サブチャネル・データ形式
      buffer[5] = 0x10;  //ADR<<4|コントロール
      buffer[6] = (byte) ((currentTrack / 10) << 4 |
                          (currentTrack % 10));  //トラック番号。BCD形式
      buffer[7] = 0x01;  //インデックス番号。BCD形式。!!!トラックの先頭のポーズ領域は0x00
      if (msf) {
        int t = currentSector + 75 * 2;
        buffer[8] = 0;  //アブソリュートCD-ROMアドレス
        buffer[9] = (byte) (t / (75 * 60));
        buffer[10] = (byte) ((t / 75) % 60);
        buffer[11] = (byte) (t % 75);
        buffer[12] = 0;  //トラック相対CD-ROMアドレス
        buffer[13] = (byte) (currentOffset / (75 * 60));
        buffer[14] = (byte) ((currentOffset / 75) % 60);
        buffer[15] = (byte) (currentOffset % 75);
      } else {
        buffer[8] = (byte) (currentSector >> 24);  //アブソリュートCD-ROMアドレス
        buffer[9] = (byte) (currentSector >> 16);
        buffer[10] = (byte) (currentSector >> 8);
        buffer[11] = (byte) currentSector;
        buffer[12] = (byte) (currentOffset >> 24);  //トラック相対CD-ROMアドレス
        buffer[13] = (byte) (currentOffset >> 16);
        buffer[14] = (byte) (currentOffset >> 8);
        buffer[15] = (byte) currentOffset;
      }
      if (false) {
        System.out.printf ("currentSector=%d\n", currentSector);
        System.out.printf ("currentTrack=%d\n", currentTrack);
        System.out.printf ("currentOffset=%d\n", currentOffset);
        for (int i = 0; i < buffer.length; i++) {
          System.out.printf ("%02x ", buffer[i] & 255);
        }
        System.out.println ();
      }
      if (hcdDebugInfo) {
        hcdDumpBuffer (buffer, buffer.length);
      }
      hcdChip.spiDataInPhase (buffer, 0, Math.min (n, buffer.length), 0);  //データインフェーズに移行する
    } else {
      hcdIllegalRequest ();
    }
  }  //hcdDoReadSubChannel_1st

  //hcdDoReadTOC_2nd ()
  //  [0]  0x43
  //  [1]  |LUN###|---|MSF|-|
  //  [6]  開始トラック
  //  [7][8]  アロケーション長
  //  [9]  コントロールバイト
  static void hcdDoReadTOC_2nd () {
    boolean msf = (hcdChip.spiCommandBuffer[1] & 2) != 0;  //true=MSFアドレス形式,false=論理ブロックアドレス形式
    int startTrack = hcdChip.spiCommandBuffer[6] & 255;  //開始トラック
    if (startTrack == 0) {
      startTrack = 1;
    }
    if (0xaa < startTrack) {  //開始トラック番号が範囲外
      hcdIllegalRequest ();
      return;
    }
    int allocLength = ByteArray.byaRwz (hcdChip.spiCommandBuffer, 7);  //アロケーション長
    byte[] buffer = hcdReadTOC (msf);
    if (buffer == null) {
      hcdNotReady ();
      return;
    }
    if (buffer.length == 0) {
      hcdMediumError ();
      return;
    }
    //トラックの開始セクタを集める
    //  TrackData[0].TrackNumber==FirstTrack
    //  TrackData[LastTrack-FirstTrack].TrackNumber==LastTrack
    //  TrackData[LastTrack-FirstTrack+1].TrackNumber==170
    hcdTOCAddressArray = new int[(buffer.length - 4) / 8];  //TrackDataのAddressの配列
    hcdTOCFirstTrack = buffer[2] & 255;
    hcdTOCLastTrack = buffer[3] & 255;
    for (int i = 0; i < hcdTOCAddressArray.length; i++) {
      hcdTOCAddressArray[i] = (msf ?
                               -75 * 2 +
                               (buffer[4 + 8 * i + 5] & 255) * (75 * 60) +
                               (buffer[4 + 8 * i + 6] & 255) * 75 +
                               (buffer[4 + 8 * i + 7] & 255) :
                               (buffer[4 + 8 * i + 4] & 255) << 24 |
                               (buffer[4 + 8 * i + 5] & 255) << 16 |
                               (buffer[4 + 8 * i + 6] & 255) << 8 |
                               (buffer[4 + 8 * i + 7] & 255));
    }
    //開始トラックに満たないデータトラックを詰める
    int dataLength = buffer.length;  //全体の長さ
    int p = 4;  //残すデータの開始位置
    while (p < dataLength - 8 &&  //リードアウトトラックでなく
           (0xff & buffer[p + 2]) < startTrack) {  //開始トラックに満たない
      p += 8;
    }
    if (4 < p) {
      for (int i = 0; i < dataLength - p; i++) {
        buffer[4 + i] = buffer[p + i];
      }
      dataLength -= p - 4;
      ByteArray.byaWw (buffer, 0, dataLength - 2);
    }
    //データインフェーズに移行する
    hcdResultBuffer = buffer;
    hcdResultLength = Math.min (dataLength, allocLength);
  }  //hcdDoReadTOC_2nd

  //hcdDoPlayAudio10_1st ()
  //  [0]  0x45
  //  [1]  |LUN###|----|RelAdr|
  //  [2][3][4][5]  論理ブロック・アドレス
  //  [7][8]  転送データ長
  //  [9]  コントロール・バイト
  static void hcdDoPlayAudio10_1st () {
    int start = ByteArray.byaRls (hcdChip.spiCommandBuffer, 2);  //開始位置
    int length = ByteArray.byaRwz (hcdChip.spiCommandBuffer, 7);  //長さ
    hcdPlay (start, start + length);
    hcdChip.spiStatusPhase (SPC.SPC_GOOD, SPC.SPC_COMMAND_COMPLETE);  //エラーなしでステータスフェーズに移行する
  }  //hcdDoPlayAudio10_1st

  //hcdDoPlayAudioMSF_1st ()
  //  [0]  0x47
  //  [1]  |LUN###|-----|
  //  [3]  開始・Mフィールド
  //  [4]  開始・Sフィールド
  //  [5]  開始・Fフィールド
  //  [6]  終了・Mフィールド
  //  [7]  終了・Sフィールド
  //  [8]  終了・Fフィールド
  //  [9]  コントロール・バイト
  static void hcdDoPlayAudioMSF_1st () {
    int start = (-75 * 2 +
                 (hcdChip.spiCommandBuffer[3] & 255) * (75 * 60) +
                 (hcdChip.spiCommandBuffer[4] & 255) * 75 +
                 (hcdChip.spiCommandBuffer[5] & 255));  //開始位置
    int end = (-75 * 2 +
               (hcdChip.spiCommandBuffer[6] & 255) * (75 * 60) +
               (hcdChip.spiCommandBuffer[7] & 255) * 75 +
               (hcdChip.spiCommandBuffer[8] & 255));  //終了位置
    hcdPlay (start, end);
    hcdChip.spiStatusPhase (SPC.SPC_GOOD, SPC.SPC_COMMAND_COMPLETE);  //エラーなしでステータスフェーズに移行する
  }  //hcd.DoPlayAudioMSF

  //hcdDoPauseResume_1st ()
  //  [0]  0x4b
  //  [1]  |LUN###|-----|
  //  [8]  |-------|Resume|
  //  [9]  コントロール・バイト
  static void hcdDoPauseResume_1st () {
    boolean resume = (hcdChip.spiCommandBuffer[8] & 1) != 0;
    if (resume) {
      hcdResume ();
    } else {
      hcdPause ();
    }
    hcdChip.spiStatusPhase (SPC.SPC_GOOD, SPC.SPC_COMMAND_COMPLETE);  //エラーなしでステータスフェーズに移行する
  }  //hcdDoPauseResume_1st

  //hcdDoPlayAudio12_1st ()
  //  [0]  0xa5
  //  [1]  |LUN###|----|RelAdr|
  //  [2][3][4][5]  論理ブロック・アドレス
  //  [6][7][8][9]  転送データ長
  //  [11]  コントロール・バイト
  static void hcdDoPlayAudio12_1st () {
    int start = ByteArray.byaRls (hcdChip.spiCommandBuffer, 2);  //開始位置
    int length = ByteArray.byaRls (hcdChip.spiCommandBuffer, 6);  //長さ
    hcdPlay (start, start + length);
    hcdChip.spiStatusPhase (SPC.SPC_GOOD, SPC.SPC_COMMAND_COMPLETE);  //エラーなしでステータスフェーズに移行する
  }  //hcdDoPlayAudio12_1st

  //hcdDoReadCDDA_2nd ()
  //  [0]  0xd8
  //  [1]  |LUN###|-----|
  //  [2][3][4][5]  論理ブロックアドレス
  //  [8][9]  論理ブロック数
  static void hcdDoReadCDDA_2nd () {
    if (!hcdOpen ()) {  //開けない
      hcdNotReady ();
      return;
    }
    int a = ByteArray.byaRls (hcdChip.spiCommandBuffer, 2);  //論理ブロックアドレス
    int n = ByteArray.byaRwz (hcdChip.spiCommandBuffer, 8);  //論理ブロック数
    if (n == 0) {
      hcdGood ();
      return;
    }
    byte[] buffer = hcdRead (a, n);
    if (buffer == null) {
      hcdNotReady ();
      return;
    }
    if (buffer.length == 0) {
      hcdMediumError ();
      return;
    }
    //データインフェーズに移行する
    hcdResultBuffer = buffer;
    hcdResultLength = buffer.length;
  }  //hcdDoReadCDDA_2nd


  static void hcdGood () {
    hcdResultSense0 = 0;
    hcdResultSense2 = 0;
    hcdResultStatus = SPC.SPC_GOOD;
    hcdResultMessage = SPC.SPC_COMMAND_COMPLETE;
  }  //hcdGood

  static void hcdNotReady () {
    hcdResultSense0 = SPC.SPC_EXTENDED_SENSE;
    hcdResultSense2 = SPC.SPC_NOT_READY;
    hcdResultStatus = SPC.SPC_CHECK_CONDITION;
    hcdResultMessage = SPC.SPC_COMMAND_COMPLETE;
  }  //hcdNotReady

  static void hcdUnitAttention () {
    hcdResultSense0 = SPC.SPC_EXTENDED_SENSE;
    hcdResultSense2 = SPC.SPC_UNIT_ATTENTION;
    hcdResultStatus = SPC.SPC_CHECK_CONDITION;
    hcdResultMessage = SPC.SPC_COMMAND_COMPLETE;
  }  //hcdUnitAttention

  static void hcdDataProtect () {
    hcdResultSense0 = SPC.SPC_EXTENDED_SENSE;
    hcdResultSense2 = SPC.SPC_DATA_PROTECT;
    hcdResultStatus = SPC.SPC_CHECK_CONDITION;
    hcdResultMessage = SPC.SPC_COMMAND_COMPLETE;
  }  //hcdDataProtect

  static void hcdMediumError () {
    hcdResultSense0 = SPC.SPC_EXTENDED_SENSE;
    hcdResultSense2 = SPC.SPC_MEDIUM_ERROR;
    hcdResultStatus = SPC.SPC_CHECK_CONDITION;
    hcdResultMessage = SPC.SPC_COMMAND_COMPLETE;
  }  //hcdMediumError

  static void hcdIllegalRequest () {
    hcdResultSense0 = SPC.SPC_EXTENDED_SENSE;
    hcdResultSense2 = SPC.SPC_ILLEGAL_REQUEST;
    hcdResultStatus = SPC.SPC_CHECK_CONDITION;
    hcdResultMessage = SPC.SPC_COMMAND_COMPLETE;
  }  //hcdIllegalRequest


  //hcdReadINTSPSNS ()
  //  INTSまたはPSNSが読み出された
  public static void hcdReadINTSPSNS () {
    if (hcdRetrieved != hcdCompleted) {  //回収カウンタ!=完了カウンタのとき
      if (hcdResultBuffer != null) {  //結果のデータがあるとき
        hcdChip.spiDataInPhase (hcdResultBuffer, 0, hcdResultLength, 0);  //データインフェーズに移行する
      } else {  //結果のデータがないとき
        hcdChip.spiSenseBuffer[hcdChip.spiLUN][0] = (byte) hcdResultSense0;
        hcdChip.spiSenseBuffer[hcdChip.spiLUN][2] = (byte) hcdResultSense2;
        hcdChip.spiStatusPhase (hcdResultStatus, hcdResultMessage);  //ステータスフェーズに移行する
      }
      hcdChip.spiSetInterruptStatus (SPC.SPC_INTS_CC);
      hcdRetrieved = hcdCompleted;
    }
  }  //hcdReadINTSPSNS


  //hcdDataOutComplete (unit, oc)
  //  データアウトフェーズの転送が終了した
  public static void hcdDataOutComplete (int oc) {
    switch (oc) {
    case 0x15:  //Mode Select(6)
      hcdDoModeSelect6_2nd ();
      break;
    default:
      hcdChip.spiStatusPhase (SPC.SPC_GOOD, SPC.SPC_COMMAND_COMPLETE);  //エラーなしでステータスフェーズに移行する
    }
  }  //hcdDataOutComplete

  //hcdDoModeSelect6_2ns ()
  static void hcdDoModeSelect6_2nd () {
    int oc = hcdChip.spiCommandBuffer[0] & 255;  //オペレーションコード
    if (hcdDebugInfo) {
      hcdUnit.scuPrintDataOut (oc);
    }
    int length = 4;
    if (length + 8 <= hcdChip.spiBufferLimit &&
        (hcdChip.spiDataOutBuffer[3] & 255) == 8) {  //ブロック・ディスクリプタ
      hcdSetBytesPerSector ((hcdChip.spiDataOutBuffer[length + 6] & 255) << 8 |
                            (hcdChip.spiDataOutBuffer[length + 7] & 255));
      length += 8;
    }
    while (length + 1 <= hcdChip.spiBufferLimit) {
      int pageCode = hcdChip.spiDataOutBuffer[length] & 63;  //ページコード
      if (length + 8 <= hcdChip.spiBufferLimit &&
          pageCode == 0x01) {  //リード・エラー・リカバリ・パラメータ
        length += 8;
      } else if (length + 24 <= hcdChip.spiBufferLimit &&
                 pageCode == 0x03) {  //フォーマット・パラメータ
        length += 24;
      } else if (length + 16 <= hcdChip.spiBufferLimit &&
                 pageCode == 0x0e) {  //CD-ROMオーディオ・コントロール・パラメータ
        hcdVolumeInt = (hcdChip.spiDataOutBuffer[length + 9] & 255) * 100 / 255;  //ボリューム(ポート#0)
        if (hcdDebugInfo) {
          System.out.printf ("volume=%d\n", hcdVolumeInt);
        }
        hcdVolumeFloat = (float) hcdVolumeInt / 100F;
        hcdVolumeSlider.setValue (hcdVolumeInt);
        hcdVolumeLabel.setText (String.valueOf (hcdVolumeInt));
        length += 16;
      } else if (length + 4 <= hcdChip.spiBufferLimit &&
                 pageCode == 0x31) {  //転送速度
        length += 4;
      } else {
        break;
      }
    }
    hcdChip.spiStatusPhase (SPC.SPC_GOOD, SPC.SPC_COMMAND_COMPLETE);  //エラーなしでステータスフェーズに移行する
  }  //hcdDoModeSelect6_2nd

  //hcdDumpBuffer (buffer, length)
  static void hcdDumpBuffer (byte[] buffer, int length) {
    System.out.print ("[");
    for (int i = 0; i < length; i++) {
      if (i != 0) {
        System.out.print (",");
      }
      System.out.printf ("0x%02x", buffer[i] & 255);
    }
    System.out.println ("]");
  }  //hcdDumpBuffer

}  //class HostCDROM
