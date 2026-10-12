//========================================================================================
//  DummyPad.java
//    en:Dummy pad
//    ja:ダミーパッド
//  Copyright (C) 2003-2023 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

//class DummyPad
//  ダミーパッド
//  ジョイスティックポートに何も繋がっていない
public class DummyPad extends Joystick {

  //new DummyPad ()
  //  コンストラクタ
  public DummyPad () {
    number = 0;
    id = "";
    nameEn = "None";
    nameJa = "なし";
  }

}  //class DummyPad
