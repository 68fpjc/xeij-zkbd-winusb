//========================================================================================
//  ZKeyPort.java
//    en:Z keyboard port
//    ja:Zキーボードポート
//  Copyright (C) 2003-2025 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

//Zキーボードのポート
//  ZKeyLEDPort  HIDのFeature Reportを使う従来の実装
//  ZKeyUsbPort  libusb(WinUSB)でUSBを直接操作する実装
public interface ZKeyPort extends AutoCloseable {
  void setDebugFlag (boolean debugFlag);
  //LEDのデータを送る。ZKeyLEDPort.sendを参照
  boolean send (long data);
  boolean isKeyToggled (int vk);
  void hitKey (int vk);
  @Override void close ();
}  //interface ZKeyPort
