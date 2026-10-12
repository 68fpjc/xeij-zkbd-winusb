//========================================================================================
//  GraphicScreen.java
//    en:Graphic screen
//    ja:グラフィック画面
//  Copyright (C) 2003-2025 Makoto Kamada
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

}  //class GraphicScreen
