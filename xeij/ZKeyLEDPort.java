//========================================================================================
//  ZKeyLEDPort.java
//    en:Z keyboard LED port
//    ja:ZキーボードLEDポート
//  Copyright (C) 2003-2025 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.io.*;
import java.lang.foreign.*;  //Arena,FunctionDescriptor,Linker,MemorySegment,SymbolLookup,ValueLayout
import java.lang.invoke.*;  //MethodHandle
import java.nio.charset.*;  //StandardCharsets
import java.util.*;  //NoSuchElementException

public final class ZKeyLEDPort implements AutoCloseable {

  private boolean debugFlag;
  public void setDebugFlag (boolean debugFlag) {
    this.debugFlag = debugFlag;
  }

  //構造体

  //GUID構造体
  //  https://learn.microsoft.com/ja-jp/windows/win32/api/guiddef/ns-guiddef-guid
  private static final MemoryLayout GUID = MemoryLayout.structLayout (
    ValueLayout.JAVA_INT.withName ("Data1"),  //0 unsigned long Data1
    ValueLayout.JAVA_SHORT.withName ("Data2"),  //4 unsigned short Data2
    ValueLayout.JAVA_SHORT.withName ("Data3"),  //6 unsigned short Data3
    MemoryLayout.sequenceLayout (
      8,
      ValueLayout.JAVA_BYTE).withName ("Data4")  //8 unsigned char Data4[8]
    //16
    );

  //HIDD_ATTRIBUTES構造体
  //  https://learn.microsoft.com/ja-jp/windows-hardware/drivers/ddi/hidsdi/ns-hidsdi-_hidd_attributes
  private static final MemoryLayout HIDD_ATTRIBUTES = MemoryLayout.structLayout (
    ValueLayout.JAVA_INT.withName ("Size"),  //0 ULONG Size
    ValueLayout.JAVA_SHORT.withName ("VendorID"),  //4 USHORT VendorID
    ValueLayout.JAVA_SHORT.withName ("ProductID"),  //6 USHORT ProductID
    ValueLayout.JAVA_SHORT.withName ("VersionNumber"),  //8 USHORT VersionNumber
    MemoryLayout.paddingLayout (2)  //10
    //12
    );

  //HIDP_CAPS構造体
  //  https://learn.microsoft.com/ja-jp/windows-hardware/drivers/ddi/hidpi/ns-hidpi-_hidp_caps
  private static final MemoryLayout HIDP_CAPS = MemoryLayout.structLayout (
    ValueLayout.JAVA_SHORT.withName ("Usage"),  //0 USAGE Usage  USAGEはUSHORT。hidusage.hで定義されている
    ValueLayout.JAVA_SHORT.withName ("UsagePage"),  //2 USAGE UsagePage
    ValueLayout.JAVA_SHORT.withName ("InputReportByteLength"),  //4 USHORT InputReportByteLength
    ValueLayout.JAVA_SHORT.withName ("OutputReportByteLength"),  //6 USHORT OutputReportByteLength
    ValueLayout.JAVA_SHORT.withName ("FeatureReportByteLength"),  //8 USHORT FeatureReportByteLength
    MemoryLayout.sequenceLayout (
      17,
      ValueLayout.JAVA_SHORT).withName ("Reserved"),  //10 USHORT Reserved[17]
    ValueLayout.JAVA_SHORT.withName ("NumberLinkCollectionNodes"),  //44 USHORT NumberLinkCollectionNodes
    ValueLayout.JAVA_SHORT.withName ("NumberInputButtonCaps"),  //46 USHORT NumberInputButtonCaps
    ValueLayout.JAVA_SHORT.withName ("NumberInputValueCaps"),  //48 USHORT NumberInputValueCaps
    ValueLayout.JAVA_SHORT.withName ("NumberInputDataIndices"),  //50 USHORT NumberInputDataIndices
    ValueLayout.JAVA_SHORT.withName ("NumberOutputButtonCaps"),  //52 USHORT NumberOutputButtonCaps
    ValueLayout.JAVA_SHORT.withName ("NumberOutputValueCaps"),  //54 USHORT NumberOutputValueCaps
    ValueLayout.JAVA_SHORT.withName ("NumberOutputDataIndices"),  //56 USHORT NumberOutputDataIndices
    ValueLayout.JAVA_SHORT.withName ("NumberFeatureButtonCaps"),  //58 USHORT NumberFeatureButtonCaps
    ValueLayout.JAVA_SHORT.withName ("NumberFeatureValueCaps"),  //60 USHORT NumberFeatureValueCaps
    ValueLayout.JAVA_SHORT.withName ("NumberFeatureDataIndices")  //62 USHORT NumberFeatureDataIndices
    //64
    );

  //INPUT構造体
  //  https://learn.microsoft.com/ja-jp/windows/win32/api/winuser/ns-winuser-input
  private static final int INPUT_KEYBOARD = 1;
  private static final int KEYEVENTF_KEYUP = 0x0002;
  private static final MemoryLayout INPUT = MemoryLayout.structLayout (
    ValueLayout.JAVA_INT.withName ("type"),  //0 DWORD type
    MemoryLayout.paddingLayout (4),  //4
    MemoryLayout.unionLayout (
      //MOUSEINPUT構造体
      //  https://learn.microsoft.com/ja-jp/windows/win32/api/winuser/ns-winuser-mouseinput
      MemoryLayout.structLayout (
        ValueLayout.JAVA_INT.withName ("dx"),  //8 LONG dx
        ValueLayout.JAVA_INT.withName ("dy"),  //12 LONG dy
        ValueLayout.JAVA_INT.withName ("mouseData"),  //16 DWORD mouseData
        ValueLayout.JAVA_INT.withName ("dwFlags"),  //20 DWORD dwFlags
        ValueLayout.JAVA_INT.withName ("time"),  //24 DWORD time
        MemoryLayout.paddingLayout (4),  //28
        ValueLayout.ADDRESS.withName ("dwExtraInfo")  //32 ULONG_PTR dwExtraInfo
        //40
        ).withName ("mi"),  //8
      //KEYBDINPUT構造体
      //  https://learn.microsoft.com/ja-jp/windows/win32/api/winuser/ns-winuser-keybdinput
      MemoryLayout.structLayout (
        ValueLayout.JAVA_SHORT.withName ("wVk"),  //8 WORD wVk
        ValueLayout.JAVA_SHORT.withName ("wScan"),  //10 WORD wScan
        ValueLayout.JAVA_INT.withName ("dwFlags"),  //12 DWORD dwFlags
        ValueLayout.JAVA_INT.withName ("time"),  //16 DWORD time
        MemoryLayout.paddingLayout (4),  //20
        ValueLayout.ADDRESS.withName ("dwExtraInfo")  //24 ULONG_PTR dwExtraInfo
        //32
        ).withName ("ki"),  //8
      //HARDWAREINPUT構造体
      //  https://learn.microsoft.com/ja-jp/windows/win32/api/winuser/ns-winuser-hardwareinput
      MemoryLayout.structLayout (
        ValueLayout.JAVA_INT.withName ("uMsg"),  //8 DWORD uMsg
        ValueLayout.JAVA_SHORT.withName ("wParamL"),  //12 WORD wParamL
        ValueLayout.JAVA_SHORT.withName ("wParamH")  //14 WORD wParamH
        //16
        ).withName ("hi")  //8
      ).withName ("DUMMYUNIONNAME")
    //40
    );

  //SP_DEVICE_INTERFACE_DATA構造体
  //  https://learn.microsoft.com/ja-jp/windows/win32/api/setupapi/ns-setupapi-sp_device_interface_data
  private static final MemoryLayout SP_DEVICE_INTERFACE_DATA = MemoryLayout.structLayout (
    ValueLayout.JAVA_INT.withName ("cbSize"),  //0 DWORD cbSize
    GUID.withName ("InterfaceClassGuid"),  //4 GUID InterfaceClassGuid
    ValueLayout.JAVA_INT.withName ("Flags"),  //20 DWORD Flags
    ValueLayout.ADDRESS.withName ("Reserved")  //24 ULONG_PTR Reserved
    //32
    );

  //SP_DEVICE_INTERFACE_DETAIL_DATA_W構造体
  //  https://learn.microsoft.com/ja-jp/windows/win32/api/setupapi/ns-setupapi-sp_device_interface_detail_data_w
  private static final int ANYSIZE_ARRAY = 1;
  private static final MemoryLayout SP_DEVICE_INTERFACE_DETAIL_DATA_W = MemoryLayout.structLayout (
    ValueLayout.JAVA_INT.withName ("cbSize"),  //0 DWORD cbSize
    MemoryLayout.sequenceLayout (
      ANYSIZE_ARRAY,
      ValueLayout.JAVA_SHORT).withName ("DevicePath"),  //4 WCHAR DevicePath[ANYSIZE_ARRAY]
    MemoryLayout.paddingLayout (2)  //6
    //8
    );

  //エラーコード
  //  https://learn.microsoft.com/ja-jp/windows/win32/debug/system-error-codes--0-499-
  private final int ERROR_INSUFFICIENT_BUFFER = 122;
  private final int ERROR_NO_MORE_ITEMS = 259;

  //リンカ
  private Linker linker;
  private MethodHandle downcallHandle (MemorySegment address, FunctionDescriptor function) {
    return linker.downcallHandle (address, function);
  }

  //アリーナ
  private Arena arena;

  //メソッド
  private MethodHandle CloseHandle;
  private static final int FILE_SHARE_READ = 0x00000001;
  private static final int FILE_SHARE_WRITE = 0x00000002;
  private static final int OPEN_EXISTING = 3;
  private static final long INVALID_HANDLE_VALUE = -1L;
  private MethodHandle CreateFileW;
  private MethodHandle GetKeyState;
  private MethodHandle GetLastError;
  private MethodHandle HidD_FreePreparsedData;
  private MethodHandle HidD_GetAttributes;
  private MethodHandle HidD_GetHidGuid;
  private MethodHandle HidD_GetPreparsedData;
  private MethodHandle HidD_SetFeature;
  private static final int HIDP_STATUS_SUCCESS = 0x0 << 28 | 0x11 << 16 | 0;
  private MethodHandle HidP_GetCaps;
  private MethodHandle SendInput;
  private MethodHandle SetupDiDestroyDeviceInfoList;
  private MethodHandle SetupDiEnumDeviceInterfaces;
  private static final int DIGCF_PRESENT = 0x00000002;
  private static final int DIGCF_DEVICEINTERFACE = 0x00000010;
  private MethodHandle SetupDiGetClassDevsA;
  private MethodHandle SetupDiGetDeviceInterfaceDetailW;

  //ハンドル
  private MemorySegment handle;

  //port = new ZKeyLEDPort (debugFlag)
  //  コンストラクタ
  public ZKeyLEDPort (boolean debugFlag) throws IOException {
    this.debugFlag = debugFlag;
    if (debugFlag) {
      System.out.println ("ZKeyLEDPort(\"" + debugFlag + "\")");
    }

    //リンカ
    linker = Linker.nativeLinker ();

    //アリーナ
    arena = Arena.ofAuto ();

    //ライブラリ
    SymbolLookup hid = SymbolLookup.libraryLookup ("hid", arena);
    SymbolLookup kernel32 = SymbolLookup.libraryLookup ("kernel32", arena);
    SymbolLookup setupapi = SymbolLookup.libraryLookup ("setupapi", arena);
    SymbolLookup user32 = SymbolLookup.libraryLookup ("user32", arena);

    //メソッド
    try {

      //CloseHandle関数
      //  https://learn.microsoft.com/ja-jp/windows/win32/api/handleapi/nf-handleapi-closehandle
      CloseHandle = downcallHandle (
        kernel32.findOrThrow ("CloseHandle"),
        FunctionDescriptor.of (
          ValueLayout.JAVA_INT,  //BOOL
          ValueLayout.ADDRESS));  //HANDLE hObject

      //CreateFileW関数
      //  https://learn.microsoft.com/ja-jp/windows/win32/api/fileapi/nf-fileapi-createfilew
      CreateFileW = downcallHandle (
        kernel32.findOrThrow ("CreateFileW"),
        FunctionDescriptor.of (
          ValueLayout.ADDRESS,  //HANDLE
          ValueLayout.ADDRESS,  //LPCWSTR lpFileName
          ValueLayout.JAVA_INT,  //DWORD dwDesiredAccess
          ValueLayout.JAVA_INT,  //DWORD dwShareMode
          ValueLayout.ADDRESS,  //LPSECURITY_ATTRIBUTES lpSecurityAttributes
          ValueLayout.JAVA_INT,  //DWORD dwCreationDisposition
          ValueLayout.JAVA_INT,  //DWORD dwFlagsAndAttributes
          ValueLayout.ADDRESS));  //HANDLE hTemplateFile

      //GetKeyState関数
      //  https://learn.microsoft.com/ja-jp/windows/win32/api/winuser/nf-winuser-getkeystate
      GetKeyState = downcallHandle (
        user32.findOrThrow ("GetKeyState"),
        FunctionDescriptor.of (
          ValueLayout.JAVA_SHORT,  //SHORT
          ValueLayout.JAVA_INT));  //int nVirtKey

      //GetLastError関数
      //  https://learn.microsoft.com/ja-jp/windows/win32/api/errhandlingapi/nf-errhandlingapi-getlasterror
      GetLastError = downcallHandle (
        kernel32.findOrThrow ("GetLastError"),
        FunctionDescriptor.of (
          ValueLayout.JAVA_INT));  //DWORD

      //HidD_FreePreparsedData関数
      //  https://learn.microsoft.com/ja-jp/windows-hardware/drivers/ddi/hidsdi/nf-hidsdi-hidd_freepreparseddata
      HidD_FreePreparsedData = downcallHandle (
        hid.findOrThrow ("HidD_FreePreparsedData"),
        FunctionDescriptor.of (
          ValueLayout.JAVA_INT,  //BOOLEAN
          ValueLayout.ADDRESS));  //PHIDP_PREPARSED_DATA PreparsedData

      //HidD_GetAttributes関数
      //  https://learn.microsoft.com/ja-jp/windows-hardware/drivers/ddi/hidsdi/nf-hidsdi-hidd_getattributes
      HidD_GetAttributes = downcallHandle (
        hid.findOrThrow ("HidD_GetAttributes"),
        FunctionDescriptor.of (
          ValueLayout.JAVA_INT,  //BOOLEAN
          ValueLayout.ADDRESS,  //HANDLE HidDeviceObject
          ValueLayout.ADDRESS));  //PHIDD_ATTRIBUTES Attributes

      //HidD_GetHidGuid関数
      //  https://learn.microsoft.com/ja-jp/windows-hardware/drivers/ddi/hidsdi/nf-hidsdi-hidd_gethidguid
      HidD_GetHidGuid = downcallHandle (
        hid.findOrThrow ("HidD_GetHidGuid"),
        FunctionDescriptor.ofVoid (
          ValueLayout.ADDRESS));  //LPGUID HidGuid

      //HidD_GetPreparsedData関数
      //  https://learn.microsoft.com/ja-jp/windows-hardware/drivers/ddi/hidsdi/nf-hidsdi-hidd_getpreparseddata
      HidD_GetPreparsedData = downcallHandle (
        hid.findOrThrow ("HidD_GetPreparsedData"),
        FunctionDescriptor.of (
          ValueLayout.JAVA_INT,  //BOOLEAN
          ValueLayout.ADDRESS,  //HANDLE HidDeviceObject
          ValueLayout.ADDRESS));  //PHIDP_PREPARSED_DATA *PreparsedData

      //HidD_SetFeature関数
      //  https://learn.microsoft.com/ja-jp/windows-hardware/drivers/ddi/hidsdi/nf-hidsdi-hidd_setfeature
      HidD_SetFeature = downcallHandle (
        hid.findOrThrow ("HidD_SetFeature"),
        FunctionDescriptor.of (
          ValueLayout.JAVA_INT,  //BOOLEAN
          ValueLayout.ADDRESS,  //HANDLE HidDeviceObject
          ValueLayout.ADDRESS,  //PVOID ReportBuffer
          ValueLayout.JAVA_INT));  //ULONG ReportBufferLength

      //HidP_GetCaps関数
      //  https://learn.microsoft.com/ja-jp/windows-hardware/drivers/ddi/hidpi/nf-hidpi-hidp_getcaps
      HidP_GetCaps = downcallHandle (
        hid.findOrThrow ("HidP_GetCaps"),
        FunctionDescriptor.of (
          ValueLayout.JAVA_INT,  //NTSTATUS
          ValueLayout.ADDRESS,  //PHIDP_PREPARSED_DATA PreparsedData
          ValueLayout.ADDRESS));  //PHIDP_CAPS Capabilities

      //SendInput関数
      //  https://learn.microsoft.com/ja-jp/windows/win32/api/winuser/nf-winuser-sendinput
      SendInput = downcallHandle (
        user32.findOrThrow ("SendInput"),
        FunctionDescriptor.of (
          ValueLayout.JAVA_INT,  //UINT
          ValueLayout.JAVA_INT,  //UINT cInputs
          ValueLayout.ADDRESS,  //LPINPUT pInputs
          ValueLayout.JAVA_INT));  //int cbSize

      //SetupDiDestroyDeviceInfoList関数
      //  https://learn.microsoft.com/ja-jp/windows/win32/api/setupapi/nf-setupapi-setupdidestroydeviceinfolist
      SetupDiDestroyDeviceInfoList = downcallHandle (
        setupapi.findOrThrow ("SetupDiDestroyDeviceInfoList"),
        FunctionDescriptor.of (
          ValueLayout.JAVA_INT,  //BOOL
          ValueLayout.ADDRESS));  //HDEVINFO DeviceInfoSet

      //SetupDiEnumDeviceInterfaces関数
      //  https://learn.microsoft.com/ja-jp/windows/win32/api/setupapi/nf-setupapi-setupdienumdeviceinterfaces
      SetupDiEnumDeviceInterfaces = downcallHandle (
        setupapi.findOrThrow ("SetupDiEnumDeviceInterfaces"),
        FunctionDescriptor.of (
          ValueLayout.JAVA_INT,  //BOOL
          ValueLayout.ADDRESS,  //HDEVINFO DeviceInfoSet
          ValueLayout.ADDRESS,  //PSP_DEVINFO_DATA DeviceInfoData
          ValueLayout.ADDRESS,  //GUID *InterfaceClassGuid
          ValueLayout.JAVA_INT,  //DWORD MemberIndex
          ValueLayout.ADDRESS));  //PSP_DEVICE_INTERFACE_DATA DeviceInterfaceData

      //SetupDiGetClassDevsA関数
      //  https://learn.microsoft.com/ja-jp/windows/win32/api/setupapi/nf-setupapi-setupdigetclassdevsa
      SetupDiGetClassDevsA = downcallHandle (
        setupapi.findOrThrow ("SetupDiGetClassDevsA"),  //SetupDiGetClassDevsWは見つからない
        FunctionDescriptor.of (
          ValueLayout.ADDRESS,  //HDEVINFO
          ValueLayout.ADDRESS,  //GUID *ClassGuid
          ValueLayout.ADDRESS,  //PCSTR Enumerator
          ValueLayout.ADDRESS,  //HWND hwndParent
          ValueLayout.JAVA_INT));  //DWORD Flags

      //SetupDiGetDeviceInterfaceDetailW関数
      //  https://learn.microsoft.com/ja-jp/windows/win32/api/setupapi/nf-setupapi-setupdigetdeviceinterfacedetailw
      SetupDiGetDeviceInterfaceDetailW = downcallHandle (
        setupapi.findOrThrow ("SetupDiGetDeviceInterfaceDetailW"),
        FunctionDescriptor.of (
          ValueLayout.JAVA_INT,  //BOOL
          ValueLayout.ADDRESS,  //HDEVINFO DeviceInfoSet
          ValueLayout.ADDRESS,  //PSP_DEVICE_INTERFACE_DATA DeviceInterfaceData
          ValueLayout.ADDRESS,  //PSP_DEVICE_INTERFACE_DETAIL_DATA_W DeviceInterfaceDetailData
          ValueLayout.JAVA_INT,  //DWORD DeviceInterfaceDetailDataSize
          ValueLayout.ADDRESS,  //PDWORD RequiredSize
          ValueLayout.ADDRESS));  //PSP_DEVINFO_DATA DeviceInfoData

    } catch (NoSuchElementException nsee) {
      nsee.printStackTrace ();
    }

    handle = null;
    open ();
  }

  //------------------------------------------------------------------------
  //ZKeyLEDPort
  //  close ()
  //  ポートを閉じる
  @Override public void close () {
    //閉じていたら何もしない
    if (handle == null || handle.address () == INVALID_HANDLE_VALUE) {
      return;
    }
    //ハンドルを閉じる
    try {
      int error;
      //  https://learn.microsoft.com/ja-jp/windows/win32/api/handleapi/nf-handleapi-closehandle
      if ((int) CloseHandle.invoke (
        handle) == 0 &&  //HANDLE hObject
          (error = (int) GetLastError.invoke ()) != -1) {
        if (debugFlag) {
          System.out.printf ("close: CloseHandle failed (%d)\n", error);
        }
        //handle = null;
      }
    } catch (Throwable e) {
      e.printStackTrace ();
      if (debugFlag) {
        System.out.printf ("close: CloseHandle invocation failed\n");
      }
      //handle = null;
    }
    //ハンドルを消す
    handle = null;
  }  //close

  //------------------------------------------------------------------------
  //ZKeyLEDPort
  //  hitKey (vk)
  //  キーを叩く
  //  vk       キー
  public void hitKey (int vk) {
    MemorySegment pInputs = arena.allocate (INPUT.byteSize () * 2,
                                            INPUT.byteAlignment ());
    pInputs.fill ((byte) 0);
    pInputs.set (ValueLayout.JAVA_INT,
                 INPUT.byteOffset (MemoryLayout.PathElement.groupElement ("type")),
                 INPUT_KEYBOARD);
    pInputs.set (ValueLayout.JAVA_SHORT,
                 INPUT.byteOffset (MemoryLayout.PathElement.groupElement ("DUMMYUNIONNAME"),
                                   MemoryLayout.PathElement.groupElement ("ki"),
                                   MemoryLayout.PathElement.groupElement ("wVk")),
                 (short) vk);
    pInputs.set (ValueLayout.JAVA_INT,
                 INPUT.byteSize () +
                 INPUT.byteOffset (MemoryLayout.PathElement.groupElement ("type")),
                 INPUT_KEYBOARD);
    pInputs.set (ValueLayout.JAVA_SHORT,
                 INPUT.byteSize () +
                 INPUT.byteOffset (MemoryLayout.PathElement.groupElement ("DUMMYUNIONNAME"),
                                   MemoryLayout.PathElement.groupElement ("ki"),
                                   MemoryLayout.PathElement.groupElement ("wVk")),
                 (short) vk);
    pInputs.set (ValueLayout.JAVA_INT,
                 INPUT.byteSize () +
                 INPUT.byteOffset (MemoryLayout.PathElement.groupElement ("DUMMYUNIONNAME"),
                                   MemoryLayout.PathElement.groupElement ("ki"),
                                   MemoryLayout.PathElement.groupElement ("dwFlags")),
                 KEYEVENTF_KEYUP);
    try {
      int error;
      //  https://learn.microsoft.com/ja-jp/windows/win32/api/winuser/nf-winuser-sendinput
      if ((int) SendInput.invoke (
        2,  //UINT cInputs
        pInputs,  //LPINPUT pInputs
        (int) INPUT.byteSize ()) == 0 &&  //int cbSize
          (error = (int) GetLastError.invoke ()) != -1) {
        if (debugFlag) {
          System.out.printf ("hitKey: SendInput failed (%d)\n", error);
        }
      }
    } catch (Throwable e) {
      e.printStackTrace ();
      if (debugFlag) {
        System.out.printf ("hitKey: SendInput invocation failed\n");
      }
    }
  }  //hitKey

  //------------------------------------------------------------------------
  //ZKeyLEDPort
  //  pressed = isKeyPressed (vk)
  //  キーは押されているか
  //  vk       キー
  //  pressed  true   キーは押されている
  //           false  キーは離されている
  public boolean isKeyPressed (int vk) {
    try {
      //  https://learn.microsoft.com/ja-jp/windows/win32/api/winuser/nf-winuser-getkeystate
      return ((short) GetKeyState.invoke (
        vk  //int nVirtKey
        ) & 128) != 0;
    } catch (Throwable e) {
      e.printStackTrace ();
      if (debugFlag) {
        System.out.printf ("isKeyPressed: GetKeyState invocation failed\n");
      }
    }
    return false;
  }  //isKeyPressed

  //------------------------------------------------------------------------
  //ZKeyLEDPort
  //  toggled = isKeyToggled (vk)
  //  キーは点灯しているか
  //  vk       キー
  //  toggled  true   キーは点灯している
  //           false  キーは消灯している
  public boolean isKeyToggled (int vk) {
    try {
      //  https://learn.microsoft.com/ja-jp/windows/win32/api/winuser/nf-winuser-getkeystate
      return ((short) GetKeyState.invoke (
        vk  //int nVirtKey
        ) & 1) != 0;
    } catch (Throwable e) {
      e.printStackTrace ();
      if (debugFlag) {
        System.out.printf ("isKeyToggled: GetKeyState invocation failed\n");
      }
    }
    return false;
  }  //isKeyToggled

  private void printCaps (String devicePath) {
    System.out.printf ("--------------------------------------\n");
    System.out.printf ("%s\n", devicePath);
    System.out.printf ("--------------------------------------\n");
    MemorySegment handle;  //HANDLE handle
    try {
      int error;
      //  https://learn.microsoft.com/ja-jp/windows/win32/api/fileapi/nf-fileapi-createfilew
      if ((handle = (MemorySegment) CreateFileW.invoke (
        arena.allocateFrom (devicePath, StandardCharsets.UTF_16LE),  //LPCWSTR lpFileName
        0,  //DWORD dwDesiredAccess
        FILE_SHARE_READ | FILE_SHARE_WRITE,  //DWORD dwShareMode
        MemorySegment.NULL,  //LPSECURITY_ATTRIBUTES lpSecurityAttributes
        OPEN_EXISTING,  //DWORD dwCreationDisposition
        0,  //DWORD dwFlagsAndAttributes
        MemorySegment.NULL)  //HANDLE hTemplateFile
           ).address () == INVALID_HANDLE_VALUE &&
          (error = (int) GetLastError.invoke ()) != -1) {
        System.out.printf ("printCaps: CreateFileW %s failed (%d)\n", devicePath, error);
        return;
      }
    } catch (Throwable e) {
      e.printStackTrace ();
      if (debugFlag) {
        System.out.printf ("printCaps: CreateFileW invocation failed\n");
      }
      return;
    }
    MemorySegment attributes = arena.allocate (HIDD_ATTRIBUTES);
    attributes.fill ((byte) 0);
    try {
      int error;
      //  https://learn.microsoft.com/ja-jp/windows-hardware/drivers/ddi/hidsdi/nf-hidsdi-hidd_getattributes
      if ((int) HidD_GetAttributes.invoke (
        handle,  //HANDLE HidDeviceObject
        attributes) == 0 &&  //PHIDD_ATTRIBUTES Attributes
          (error = (int) GetLastError.invoke ()) != -1) {
        System.out.printf ("printCaps: HidD_GetAttributes failed (%d)\n", error);
      } else {
        System.out.printf ("VendorID\t\t\t0x%04x\n",
                           0xffff & attributes.get (ValueLayout.JAVA_SHORT,
                                                    HIDD_ATTRIBUTES.byteOffset (MemoryLayout.PathElement.groupElement ("VendorID"))));
        System.out.printf ("ProductID\t\t\t0x%04x\n",
                           0xffff & attributes.get (ValueLayout.JAVA_SHORT,
                                                    HIDD_ATTRIBUTES.byteOffset (MemoryLayout.PathElement.groupElement ("ProductID"))));
        System.out.printf ("VersionNumber\t\t\t0x%04x\n",
                           0xffff & attributes.get (ValueLayout.JAVA_SHORT,
                                                    HIDD_ATTRIBUTES.byteOffset (MemoryLayout.PathElement.groupElement ("VersionNumber"))));
        System.out.printf ("--------------------------------------\n");
      }
    } catch (Throwable e) {
      e.printStackTrace ();
      if (debugFlag) {
        System.out.printf ("printCaps: HidD_GetAttributes invocation failed\n");
      }
    }
    MemorySegment preparsedDataHandle = arena.allocate (ValueLayout.ADDRESS);  //PHIDP_PREPARSED_DATA
    preparsedDataHandle.fill ((byte) 0);
    try {
      int error;
      //  https://learn.microsoft.com/ja-jp/windows-hardware/drivers/ddi/hidsdi/nf-hidsdi-hidd_getpreparseddata
      if ((int) HidD_GetPreparsedData.invoke (
        handle,  //HANDLE HidDeviceObject
        preparsedDataHandle) == 0 &&  //PHIDP_PREPARSED_DATA *PreparsedData
          (error = (int) GetLastError.invoke ()) != -1) {
        System.out.printf ("printCaps: HidD_GetPreparsedData failed (%d)\n", error);
        return;
      }
    } catch (Throwable e) {
      e.printStackTrace ();
      if (debugFlag) {
        System.out.printf ("printCaps: HidD_GetPreparsedData invocation failed\n");
      }
      return;
    }
    MemorySegment preparsedData = preparsedDataHandle.get (ValueLayout.ADDRESS, 0L);
    MemorySegment caps = arena.allocate (HIDP_CAPS);
    caps.fill ((byte) 0);
    try {
      int ntstatus;
      //  https://learn.microsoft.com/ja-jp/windows-hardware/drivers/ddi/hidpi/nf-hidpi-hidp_getcaps
      if ((ntstatus = (int) HidP_GetCaps.invoke (
        preparsedData,  //PHIDP_PREPARSED_DATA PreparsedData
        caps)  //PHIDP_CAPS Capabilities
           ) != HIDP_STATUS_SUCCESS) {
        System.out.printf ("printCaps: HidP_GetCaps failed (0x%08x)\n", ntstatus);
      } else {
        System.out.printf ("Usage\t\t\t\t0x%04x\n",
                           0xffff & caps.get (ValueLayout.JAVA_SHORT,
                                              HIDP_CAPS.byteOffset (MemoryLayout.PathElement.groupElement ("Usage"))));
        System.out.printf ("UsagePage\t\t\t0x%04x\n",
                           0xffff & caps.get (ValueLayout.JAVA_SHORT,
                                              HIDP_CAPS.byteOffset (MemoryLayout.PathElement.groupElement ("UsagePage"))));
        System.out.printf ("InputReportByteLength\t\t0x%04x\n",
                           0xffff & caps.get (ValueLayout.JAVA_SHORT,
                                              HIDP_CAPS.byteOffset (MemoryLayout.PathElement.groupElement ("InputReportByteLength"))));
        System.out.printf ("OutputReportByteLength\t\t0x%04x\n",
                           0xffff & caps.get (ValueLayout.JAVA_SHORT,
                                              HIDP_CAPS.byteOffset (MemoryLayout.PathElement.groupElement ("OutputReportByteLength"))));
        System.out.printf ("FeatureReportByteLength\t\t0x%04x\n",
                           0xffff & caps.get (ValueLayout.JAVA_SHORT,
                                              HIDP_CAPS.byteOffset (MemoryLayout.PathElement.groupElement ("FeatureReportByteLength"))));
        System.out.printf ("NumberLinkCollectionNodes\t0x%04x\n",
                           0xffff & caps.get (ValueLayout.JAVA_SHORT,
                                              HIDP_CAPS.byteOffset (MemoryLayout.PathElement.groupElement ("NumberLinkCollectionNodes"))));
        System.out.printf ("NumberInputButtonCaps\t\t0x%04x\n",
                           0xffff & caps.get (ValueLayout.JAVA_SHORT,
                                              HIDP_CAPS.byteOffset (MemoryLayout.PathElement.groupElement ("NumberInputButtonCaps"))));
        System.out.printf ("NumberInputValueCaps\t\t0x%04x\n",
                           0xffff & caps.get (ValueLayout.JAVA_SHORT,
                                              HIDP_CAPS.byteOffset (MemoryLayout.PathElement.groupElement ("NumberInputValueCaps"))));
        System.out.printf ("NumberInputDataIndices\t\t0x%04x\n",
                           0xffff & caps.get (ValueLayout.JAVA_SHORT,
                                              HIDP_CAPS.byteOffset (MemoryLayout.PathElement.groupElement ("NumberInputDataIndices"))));
        System.out.printf ("NumberOutputButtonCaps\t\t0x%04x\n",
                           0xffff & caps.get (ValueLayout.JAVA_SHORT,
                                              HIDP_CAPS.byteOffset (MemoryLayout.PathElement.groupElement ("NumberOutputButtonCaps"))));
        System.out.printf ("NumberOutputValueCaps\t\t0x%04x\n",
                           0xffff & caps.get (ValueLayout.JAVA_SHORT,
                                              HIDP_CAPS.byteOffset (MemoryLayout.PathElement.groupElement ("NumberOutputValueCaps"))));
        System.out.printf ("NumberOutputDataIndices\t\t0x%04x\n",
                           0xffff & caps.get (ValueLayout.JAVA_SHORT,
                                              HIDP_CAPS.byteOffset (MemoryLayout.PathElement.groupElement ("NumberOutputDataIndices"))));
        System.out.printf ("NumberFeatureButtonCaps\t\t0x%04x\n",
                           0xffff & caps.get (ValueLayout.JAVA_SHORT,
                                              HIDP_CAPS.byteOffset (MemoryLayout.PathElement.groupElement ("NumberFeatureButtonCaps"))));
        System.out.printf ("NumberFeatureValueCaps\t\t0x%04x\n",
                           0xffff & caps.get (ValueLayout.JAVA_SHORT,
                                              HIDP_CAPS.byteOffset (MemoryLayout.PathElement.groupElement ("NumberFeatureValueCaps"))));
        System.out.printf ("NumberFeatureDataIndices\t0x%04x\n",
                           0xffff & caps.get (ValueLayout.JAVA_SHORT,
                                              HIDP_CAPS.byteOffset (MemoryLayout.PathElement.groupElement ("NumberFeatureDataIndices"))));
        System.out.printf ("--------------------------------------\n");
      }
    } catch (Throwable e) {
      e.printStackTrace ();
      if (debugFlag) {
        System.out.printf ("printCaps: HidP_GetCaps invocation failed\n");
      }
    }
    try {
      int error;
      //  https://learn.microsoft.com/ja-jp/windows-hardware/drivers/ddi/hidsdi/nf-hidsdi-hidd_freepreparseddata
      if ((int) HidD_FreePreparsedData.invoke (
        preparsedData) == 0 &&  //PHIDP_PREPARSED_DATA PreparsedData
          (error = (int) GetLastError.invoke ()) != -1) {
        System.out.printf ("printCaps: HidD_FreePreparsedData failed (%d)\n", error);
      }
    } catch (Throwable e) {
      e.printStackTrace ();
      if (debugFlag) {
        System.out.printf ("printCaps: HidD_FreePreparsedData invocation failed\n");
      }
    }
    try {
      int error;
      //  https://learn.microsoft.com/ja-jp/windows/win32/api/handleapi/nf-handleapi-closehandle
      if ((int) CloseHandle.invoke (
        handle) == 0 &&  //HANDLE hObject
          (error = (int) GetLastError.invoke ()) != -1) {
        System.out.printf ("printCaps: CloseHandle failed (%d)\n", error);
      }
    } catch (Throwable e) {
      e.printStackTrace ();
      if (debugFlag) {
        System.out.printf ("printCaps: CloseHandle invocation failed\n");
      }
    }
  }

  //------------------------------------------------------------------------
  //ZKeyLEDPort
  //  open ()
  //  ポートを開く
  public void open () throws IOException {
    //開いていたら失敗
    //  Javaは変数にゴミが入っていることはない
    if (handle != null && handle.address () != INVALID_HANDLE_VALUE) {
      if (debugFlag) {
        System.out.printf ("open: already open\n");
      }
      throw new IOException ("already open");
    }
    //デバイス情報セットを作る
    MemorySegment hidGuid = arena.allocate (GUID);
    hidGuid.fill ((byte) 0);
    try {
      //  https://learn.microsoft.com/ja-jp/windows-hardware/drivers/ddi/hidsdi/nf-hidsdi-hidd_gethidguid
      HidD_GetHidGuid.invoke (
        hidGuid);  //LPGUID HidGuid
    } catch (Throwable e) {
      e.printStackTrace ();
      if (debugFlag) {
        System.out.printf ("open: HidD_GetHidGuid invocation failed\n");
      }
    }
    MemorySegment infoSet = arena.allocate (ValueLayout.ADDRESS);  //HDEVINFO
    try {
      int error;
      //  https://learn.microsoft.com/ja-jp/windows/win32/api/setupapi/nf-setupapi-setupdigetclassdevsw
      if ((infoSet = (MemorySegment) SetupDiGetClassDevsA.invoke (
        hidGuid,  //GUID *ClassGuid
        MemorySegment.NULL,  //PCSTR Enumerator
        MemorySegment.NULL,  //HWND hwndParent
        DIGCF_DEVICEINTERFACE | DIGCF_PRESENT)  //DWORD Flags
           ).address () == INVALID_HANDLE_VALUE &&
          (error = (int) GetLastError.invoke ()) != -1) {
        if (debugFlag) {
          System.out.printf ("open: SetupDiGetClassDevsA failed (%d)\n", error);
        }
        throw new IOException ("SetupDiGetClassDevsA failed");
      } else {
        if (debugFlag) {
          System.out.printf ("open: SetupDiGetClassDevsA success\n");
        }
      }
    } catch (IOException ioe) {
      throw ioe;
    } catch (Throwable e) {
      e.printStackTrace ();
      if (debugFlag) {
        System.out.printf ("open: SetupDiGetClassDevsA invocation failed\n");
      }
      return;
    }
    String targetPath = "";
    //デバイスを探す
    MemorySegment interfaceData = arena.allocate (SP_DEVICE_INTERFACE_DATA);
    for (int index = 0; ; index++) {
      //デバイスインターフェイスを得る
      interfaceData.fill ((byte) 0);
      interfaceData.set (ValueLayout.JAVA_INT,
                         SP_DEVICE_INTERFACE_DATA.byteOffset (MemoryLayout.PathElement.groupElement ("cbSize")),
                         (int) SP_DEVICE_INTERFACE_DATA.byteSize ());
      try {
        int error;
        //  https://learn.microsoft.com/ja-jp/windows/win32/api/setupapi/nf-setupapi-setupdienumdeviceinterfaces
        if ((int) SetupDiEnumDeviceInterfaces.invoke (
          infoSet,  //HDEVINFO DeviceInfoSet
          MemorySegment.NULL,  //PSP_DEVINFO_DATA DeviceInfoData
          hidGuid,  //GUID *InterfaceClassGuid
          index,  //DWORD MemberIndex
          interfaceData) == 0 &&  //PSP_DEVICE_INTERFACE_DATA DeviceInterfaceData
            (error = (int) GetLastError.invoke ()) != -1) {
          if (error != 0 &&
              error != ERROR_NO_MORE_ITEMS) {
            if (debugFlag) {
              System.out.printf ("open: index %d, SetupDiEnumDeviceInterfaces failed (%d)\n", index, error);
            }
          } else {
            if (debugFlag) {
              System.out.printf ("open: index %d, no more items\n", index);
            }
          }
          break;
        }
      } catch (Throwable e) {
        e.printStackTrace ();
        if (debugFlag) {
          System.out.printf ("open: index %d, SetupDiEnumDeviceInterfaces invocation failed\n", index);
        }
        break;
      }
      if (debugFlag) {
        System.out.printf ("open: index %d, SetupDiEnumDeviceInterfaces success\n", index);
      }
      //デバイスインターフェイスの詳細のサイズを得る
      MemorySegment detailSizeSegment = arena.allocate (ValueLayout.JAVA_INT);  //DWORD detailSize
      int detailSize;
      try {
        int error;
        //  https://learn.microsoft.com/ja-jp/windows/win32/api/setupapi/nf-setupapi-setupdigetdeviceinterfacedetailw
        if ((int) SetupDiGetDeviceInterfaceDetailW.invoke (
          infoSet,  //HDEVINFO DeviceInfoSet
          interfaceData,  //PSP_DEVICE_INTERFACE_DATA DeviceInterfaceData
          MemorySegment.NULL,  //PSP_DEVICE_INTERFACE_DETAIL_DATA_W DeviceInterfaceDetailData
          0,  //DWORD DeviceInterfaceDetailDataSize
          detailSizeSegment,  //PDWORD RequiredSize
          MemorySegment.NULL) == 0 &&  //PSP_DEVINFO_DATA DeviceInfoData
            (error = (int) GetLastError.invoke ()) != -1) {
          if (error != 0 &&
              error != ERROR_INSUFFICIENT_BUFFER) {
            if (debugFlag) {
              System.out.printf ("open: index %d, SetupDiGetDeviceInterfaceDetail failed (%d)\n", index, error);
            }
            continue;
          } else {
            detailSize = detailSizeSegment.get (ValueLayout.JAVA_INT, 0L);
            if (debugFlag) {
              System.out.printf ("open: index %d, SetupDiGetDeviceInterfaceDetail insufficient buffer, detailSize %d\n", index, detailSize);
            }
          }
        } else {
          if (debugFlag) {
            System.out.printf ("open: index %d, SetupDiGetDeviceInterfaceDetail unexpected success\n", index);
          }
          continue;
        }
      } catch (Throwable e) {
        e.printStackTrace ();
        if (debugFlag) {
          System.out.printf ("open: index %d, SetupDiGetDeviceInterfaceDetailW invocation failed\n", index);
        }
        continue;
      }
      //デバイスインターフェイスの詳細を得る
      MemorySegment detailData = arena.allocate ((long) detailSize, 4L);  //SP_DEVICE_INTERFACE_DETAIL_DATA_W
      detailData.fill ((byte) 0);
      detailData.set (ValueLayout.JAVA_INT,
                      SP_DEVICE_INTERFACE_DETAIL_DATA_W.byteOffset (MemoryLayout.PathElement.groupElement ("cbSize")),
                      (int) SP_DEVICE_INTERFACE_DETAIL_DATA_W.byteSize ());  //detailSizeではない。可変部分のサイズを含まない
      try {
        int error;
        //  https://learn.microsoft.com/ja-jp/windows/win32/api/setupapi/nf-setupapi-setupdigetdeviceinterfacedetailw
        if ((int) SetupDiGetDeviceInterfaceDetailW.invoke (
          infoSet,  //HDEVINFO DeviceInfoSet
          interfaceData,  //PSP_DEVICE_INTERFACE_DATA DeviceInterfaceData
          detailData,  //PSP_DEVICE_INTERFACE_DETAIL_DATA_W DeviceInterfaceDetailData
          detailSize,  //DWORD DeviceInterfaceDetailDataSize
          MemorySegment.NULL,  //PDWORD RequiredSize
          MemorySegment.NULL) == 0 &&  //PSP_DEVINFO_DATA DeviceInfoData
            (error = (int) GetLastError.invoke ()) != -1) {
          if (debugFlag) {
            System.out.printf ("open: index %d, SetupDiGetDeviceInterfaceDetail failed (%d)\n", index, error);
          }
          continue;
        } else {
          if (debugFlag) {
            System.out.printf ("open: index %d, SetupDiGetDeviceInterfaceDetail success\n", index);
          }
        }
      } catch (Throwable e) {
        e.printStackTrace ();
        if (debugFlag) {
          System.out.printf ("open: index %d, SetupDiGetDeviceInterfaceDetailW invocation failed\n", index);
        }
        continue;
      }
      //デバイスパスをコピーする
      String devicePath = detailData.getString (SP_DEVICE_INTERFACE_DETAIL_DATA_W.byteOffset (MemoryLayout.PathElement.groupElement ("DevicePath")),
                                                StandardCharsets.UTF_16LE);
      String lowerPath = devicePath.toLowerCase ();
      //情報を表示する
      if (debugFlag) {
        printCaps (devicePath);
      }
      //デバイスパスを比較する
      //  DevicePathはcase-insensitive
      if (lowerPath.indexOf ("vid_33dd&pid_0011&mi_01&col05") == -1) {
        if (debugFlag) {
          System.out.printf ("open: index %d, mismatch\n", index);
        }
        continue;
      } else {
        if (debugFlag) {
          System.out.printf ("open: index %d, match\n", index);
        }
      }
      targetPath = devicePath;
    }  //for index
    //デバイス情報セットを捨てる
    try {
      int error;
      //  https://learn.microsoft.com/ja-jp/windows/win32/api/setupapi/nf-setupapi-setupdidestroydeviceinfolist
      if ((int) SetupDiDestroyDeviceInfoList.invoke (
        infoSet) == 0 &&  //HDEVINFO DeviceInfoSet
          (error = (int) GetLastError.invoke ()) != -1) {
        if (debugFlag) {
          System.out.printf ("open: SetupDiDestroyDeviceInfoList failed (%d)\n", error);
        }
        throw new IOException ("SetupDiDestroyDeviceInfoList failed");
      } else {
        if (debugFlag) {
          System.out.printf ("open: SetupDiDestroyDeviceInfoList success\n");
        }
      }
    } catch (IOException ioe) {
      throw ioe;
    } catch (Throwable e) {
      e.printStackTrace ();
      if (debugFlag) {
        System.out.printf ("open: SetupDiDestroyDeviceInfoList invocation failed\n");
      }
      return;
    }
    //見つからなかったら失敗
    if (targetPath.equals ("")) {
      if (debugFlag) {
        System.out.printf ("open: device not found\n");
      }
      throw new IOException ("device not found");
    } else {
      if (debugFlag) {
        System.out.printf ("open: device found\n");
      }
    }
    //デバイスを開く
    try {
      int error;
      //  https://learn.microsoft.com/ja-jp/windows/win32/api/fileapi/nf-fileapi-createfilew
      if ((handle = (MemorySegment) CreateFileW.invoke (
        arena.allocateFrom (targetPath, StandardCharsets.UTF_16LE),  //LPCWSTR lpFileName
        0,  //DWORD dwDesiredAccess
        FILE_SHARE_READ | FILE_SHARE_WRITE,  //DWORD dwShareMode
        MemorySegment.NULL,  //LPSECURITY_ATTRIBUTES lpSecurityAttributes
        OPEN_EXISTING,  //DWORD dwCreationDisposition
        0,  //DWORD dwFlagsAndAttributes
        MemorySegment.NULL)  //HANDLE hTemplateFile
           ).address () == INVALID_HANDLE_VALUE &&
          (error = (int) GetLastError.invoke ()) != -1) {
        if (debugFlag) {
          System.out.printf ("open: CreateFile %s failed (%d)\n", targetPath, error);
          System.out.printf ("open: device not available\n");
        }
        throw new IOException ("device not available");
      } else {
        if (debugFlag) {
          System.out.printf ("open: CreateFile %s success\n", targetPath);
        }
      }
    } catch (IOException ioe) {
      throw ioe;
    } catch (Throwable e) {
      e.printStackTrace ();
      if (debugFlag) {
        System.out.printf ("open: CreateFileW invocation failed\n");
      }
      return;
    }
    //成功して終了
  }  //open

  //------------------------------------------------------------------------
  //ZKeyLEDPort
  //  success = send (data)
  //  LEDのデータを送る
  //  success  true   成功
  //           false  失敗
  //  data    LEDのデータ。0=消灯,…,32=暗い,…,64=やや暗い,…,128=やや明るい,…,255=明るい
  //          +----------+----------+----------+----------+----------+----------+----------+----------+
  //          |63      56|55      48|47      40|39      32|31      24|23      16|15       8|7        0|
  //          |          |   全角   | ひらがな |    INS   |   CAPS   |コード入力| ローマ字 |   かな   |
  //          +----------+----------+----------+----------+----------+----------+----------+----------+
  private static final int[] indexes = new int[] { 7, 8, 9, 10, 11, 13, 14 };
  public boolean send (long data) {
    //閉じていたら失敗
    if (handle == null || handle.address () == INVALID_HANDLE_VALUE) {
      return false;
    }
    //機能レポートを作る
    final int length = 65;
    MemorySegment report = arena.allocate (length);
    report.fill ((byte) 0);
    report.set (ValueLayout.JAVA_BYTE, 0L, (byte) 10);
    report.set (ValueLayout.JAVA_BYTE, 1L, (byte) 248);
    for (int i = 0; i < 7; i++) {
      report.set (ValueLayout.JAVA_BYTE, (long) indexes[i], (byte) (data >> (8 * i)));
    }
    //機能レポートを送る
    try {
      int error;
      //  https://learn.microsoft.com/ja-jp/windows-hardware/drivers/ddi/hidsdi/nf-hidsdi-hidd_setfeature
      if ((int) HidD_SetFeature.invoke (
        handle,  //HANDLE HidDeviceObject
        report,  //PVOID ReportBuffer
        length) == 0 &&  //ULONG ReportBufferLength
          (error = (int) GetLastError.invoke ()) != -1) {
        if (debugFlag) {
          System.out.printf ("send: HidD_SetFeature failed (%d)\n", error);
        }
        return false;
      }
    } catch (Throwable e) {
      e.printStackTrace ();
      if (debugFlag) {
        System.out.printf ("send: HidD_SetFeature invocation failed\n");
      }
      return false;
    }
    return true;
  }  //send

}  //class ZKeyLEDPort
