//========================================================================================
//  GraphicScreen.java
//    en:Graphic screen
//    ja:グラフィック画面
//  Copyright (C) 2003-2026 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

public class GraphicScreen {

  //GVRAM
  public static final byte[] graM4 = new byte[0x400000 + 0x40000];  //GVRAM+ダミー4bitページ

  //初期ノイズ
  public static int graGraphicNoise;
  public static int graTextNoise;

  //graInit ()
  //  初期化
  public static void graInit () {
    //初期ノイズ
    graGraphicNoise = Settings.sgsGetInt ("graphicnoise", 8, 0, 16);
    graTextNoise = Settings.sgsGetInt ("textnoise", 8, 0, 16);
    graWriteNoise ();
  }  //graInit

  //graTini ()
  //  後始末
  public static void graTini () {
    //初期ノイズ
    Settings.sgsPutInt ("graphicnoise", graGraphicNoise);
    Settings.sgsPutInt ("textnoise", graTextNoise);
  }  //graTini

  //graWriteNoise ()
  //  初期ノイズを書き込む
  public static void graWriteNoise () {
    //Malfunction.reset ();
    Malfunction.fill (graM4, 0, graM4.length, graGraphicNoise, 0xf);
    Malfunction.fill (MainMemory.mmrM8, 0x00e00000, 0x00e80000, graTextNoise, -1);
  }  //graWriteNoise

}  //class GraphicScreen
