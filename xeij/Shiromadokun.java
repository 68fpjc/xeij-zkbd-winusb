//========================================================================================
//  Shiromadokun.java
//    en:Shiromadokun
//    ja:白窓君
//  Copyright (C) 2003-2023 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

//                                                                                  
//    白窓君 (旧版) の配線図                                                        
//                                  JS                                              
//            ┏━━━━━━━━━━━━━━━━━━━━━┓                        
//            ┃    IOA0    IOA1    IOA2    IOA3    VCC1  ┃                        
//            ┃    1  IOA5 2  IOA6 3  PC4  4  GND  5     ┃                        
//            ┗┓  ○  6   ○  7   ○  8   ○  9   ○  ┏┛                        
//              ┃  │  ○  │  ○  │  ○  │  ○  │  ┃                          
//              ┗━┿━┿━┿━┿━┿━┿━┿━┿━┿━┛                          
//                  黒  青  茶  紫  赤  緑  橙  灰  黄                              
//                  │  │  │  │  │  │  │  桃  │                              
//                  │  │  │  │  │  │  └─┼─┼───────┐              
//                  │  │  │  │  └─┼───┼─┼─────┐  │              
//                  │  │  └─┼───┼───┼─┼───┐  │  │              
//                  └─┼───┼───┼───┼─┼─┐  │  │  │              
//                      └─┐  │  ┌─┘      │  │  │  │  │  │              
//              ┌─────┼─┼─┼─────┘  │  │  │  │  │              
//              灰  ┌───┼─┼─┼───────┘  │  │  │  │              
//              桃  黄      青  紫  緑                  黒  茶  赤  橙              
//          ┏━┿━┿━━━┿━┿━┿━━━━━━━━━┿━┿━┿━┿━━━━┓    
//      ┌─╂─○  ○  ○  ○  ○  ○  ○  ○  ○  ○  ○  ○  ○  ○        ┃    
//      │  ┃  1  2│ 3│  4   5   6   7   8   9   10  11  12  13  14        ┃    
//      ＜  ┃ VSS  │  │  RS  R/W E   DB0 DB1 DB2 DB3 DB4 DB5 DB6 DB7       ┃    
//      ＜←╂───┼─┘                                                    ┃    
//      ＜  ┃      │  VL                                                    ┃    
//      └─╂───┘                     NDM162                             ┃    
//          ┃     VDD                                                        ┃    
//          ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛    
//                                                                                  
//                                                                                  
//                                                                                  
//    白窓君 (復刻版) の配線図                                                      
//                                                                                  
//                            JS                                                    
//      ┏━━━━━━━━━━━━━━━━━━━━━┓                              
//      ┃    IOA0    IOA1    IOA2    IOA3    VCC1  ┃                              
//      ┃    1  IOA5 2  IOA6 3  PC4  4  GND  5     ┃                              
//      ┗┓  ○  6   ○  7   ○  8   ○  9   ○  ┏┛                              
//        ┃  │  ○  │  ○  │  ○  │  ○  │  ┃                                
//        ┗━┿━┿━┿━┿━┿━┿━┿━┿━┿━┛                                
//            黒  青  茶  紫  赤  緑  橙  灰  黄              SC1602B               
//            │  │  │  │  │  │  │  桃  │    ┏━━━━━━━━━━━━┓    
//            │  │  │  │  │  │  └─┼─┼─橙╂○14 DB7    ○13 DB6    ┃    
//            │  │  │  │  └─┼───┼─┼──╂─────赤┘          ┃    
//            │  │  └─┼───┼───┼─┼─茶╂○12 DB5    ○11 DB4    ┃    
//            └─┼───┼───┼───┼─┼──╂─────黒┘          ┃    
//                │      │      │      │  │    ┃○10 DB3    ○9 DB2     ┃    
//                │      │      │      │  │    ┃                        ┃    
//                │      │      │      │  │    ┃○8 DB1     ○7 DB0     ┃    
//                │      └───┼───┼─┼──╂─────紫┐          ┃    
//                │              └───┼─┼─緑╂○6 E       ○5 R/W     ┃    
//                │                      │  │    ┃                        ┃    
//                └───────────┼─┼─青╂○4 RS  ┌─○3 VO      ┃    
//                                        │  └──╂────┼黄┐          ┃    
//                                        └──灰桃╂○2 VSS │  ○1 VDD     ┃    
//                                                  ┗┿━━━┿━┿━━━━━┛    
//                                                    │      ↓  │                
//                                                    └──∨∨∨┘20kΩ           
//                                                                                  
//                                                                                  
//  HD44780
//    CLCDコントローラ
//      R/W  RS
//       0    0    コマンドレジスタ書き込み
//       0    1    データレジスタ書き込み
//       1    0    ステータスレジスタ読み出し
//       1    1    データレジスタ読み出し
//    書き込み手順
//      R/W,RS,DB7,DB6,DB5,DB4をセットしてE=1→0
//      R/W,RS,DB3,DB2,DB1,DB0をセットしてE=1→0
//    コマンド
//      0  0  0  0  0  0  0  1    画面クリア,カーソルアドレス0,ホームアドレス0
//      0  0  0  0  0  0  1  *    カーソルアドレス0,ホームアドレス0
//      0  0  0  0  0  1  0  0    文字を書いたらカーソルアドレスをデクリメント(カーソルを左へ移動),左上0→右下64+39,左下64→右上39
//      0  0  0  0  0  1  0  1    文字を書いたらカーソルアドレスをデクリメント,ホームアドレスをデクリメント(画面を右へシフト),0→39
//      0  0  0  0  0  1  1  0    文字を書いたらカーソルアドレスをインクリメント(カーソルを右へ移動),右上39→左下64,右下64+39→左上0
//      0  0  0  0  0  1  1  1    文字を書いたらカーソルアドレスをインクリメント,ホームアドレスをインクリメント(画面を左へシフト),39→0
//      0  0  0  0  1  0  *  *    表示OFF
//      0  0  0  0  1  1  0  0    表示ON,下線カーソルOFF,四角カーソルOFF
//      0  0  0  0  1  1  0  1    表示ON,下線カーソルOFF,四角カーソルON
//      0  0  0  0  1  1  1  0    表示ON,下線カーソルON,四角カーソルOFF
//      0  0  0  0  1  1  1  1    表示ON,下線カーソルON,四角カーソルON
//      0  0  0  1  0  0  *  *    カーソルアドレスデクリメント,左上0→右下64+39,左下64→右上39
//      0  0  0  1  0  1  *  *    カーソルアドレスインクリメント,右上39→左下64,右下64+39→左上0
//      0  0  0  1  1  0  *  *    ホームアドレスインクリメント(画面を左へシフト),39→0
//      0  0  0  1  1  1  *  *    ホームアドレスデクリメント(画面を右へシフト),0→39
//      0  0  1  0  0  0  *  *    4bit,5x8ドット1行      0011 0011 0011 0010で4bitになる。0x33は下位データ待ちを繰り返して0x32になったら抜ける
//      0  0  1  0  0  1  *  *    4bit,5x10ドット1行
//      0  0  1  0  1  *  *  *    4bit,5x8ドット2行
//      0  0  1  1  0  0  *  *    8bit,5x8ドット1行      0011**** 0011**** 0011****で8bitになる
//      0  0  1  1  0  1  *  *    8bit,5x10ドット1行
//      0  0  1  1  1  *  *  *    8bit,5x8ドット2行
//      0  1  c  c  c  y  y  y    フォントアドレス
//      1  y  x  x  x  x  x  x    カーソルアドレス,40..63→64,64+40..64+63→0
//

package xeij;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;  //BufferedImage,IndexColorModel
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

//class Shiromadokun
public class Shiromadokun extends Joystick {

  private static final boolean DEBUG = false;

  //仕様
  public static final int JAPANESE_SPECIFICATION = 0;
  public static final int EUROPEAN_SPECIFICATION = 1;
  protected int specification;

  protected int pinE;  //1→0で書き込む
  protected int busData;  //E=1→0のとき書き込むデータ
  protected int upper;  //上位4bit。-1=上位4bit待ち
  protected int lower;  //下位4bit。-1=下位4bit待ち
  protected int fontOrScreen;  //0=フォント書き込み,1=画面書き込み
  protected int fontAddress;  //フォントアドレス。次にフォントデータを書き込まれるアドレス。0..63
  protected int cursorAddress;  //カーソルアドレス。カーソルが表示されて次に文字を書き込まれるアドレス。0..39,64..64+39
  protected int homeAddress;  //ホームアドレス。画面の左上に表示されている文字のアドレス。0..39
  protected byte[] screenMemory;  //画面メモリ。0..39は1行目,64..64+39は2行目
  protected byte[] fontMemory;  //フォントメモリ。0..7は外字,8..15は0..7と同じ
  protected int fontBaseAddress;  //フォントベースアドレス。8*256*0は日本仕様、8*256*1は欧州仕様

  protected int entryMode;
  protected boolean displayOn;  //表示OFF/表示ON
  protected boolean underlineCursorOn;  //下線カーソルOFF/下線カーソルON

  protected static final long BLINKING_DELAY = 0L;
  protected static final long BLINKING_INTERVAL = 500L;  //実機は300くらいか
  protected boolean blinkingCursorOn;  //点滅カーソルOFF/点滅カーソルON
  protected int blinkingCursorMask;  //点滅カーソルのマスク
  protected TimerTask blinkingCursorTask;  //点滅カーソルのタスク

  //イメージとビットマップ
  //  NDM162のLCDは16文字x2行、1文字が5x7ドット、隙間が1ドット、パディングは左右が6ドット、上下が4ドットくらい
  protected static final int SCREEN_WIDTH = 16;
  protected static final int SCREEN_HEIGHT = 2;
  protected static final int CHARACTER_WIDTH = 5;
  protected static final int CHARACTER_HEIGHT = 8;
  protected static final int SPACE_WIDTH = 1;
  protected static final int SPACE_HEIGHT = 1;
  protected static final int MARGIN_TOP = 2;
  protected static final int MARGIN_BOTTOM = 2;
  protected static final int MARGIN_LEFT = 4;
  protected static final int MARGIN_RIGHT = 4;
  protected static final int DOT_WIDTH = 2;
  protected static final int DOT_HEIGHT = 2;
  protected static final int IMAGE_WIDTH = DOT_WIDTH * (MARGIN_LEFT + (CHARACTER_WIDTH + SPACE_WIDTH) * SCREEN_WIDTH - 1 + MARGIN_RIGHT);
  protected static final int IMAGE_HEIGHT = DOT_HEIGHT * (MARGIN_TOP + (CHARACTER_HEIGHT + SPACE_HEIGHT) * SCREEN_HEIGHT - 1 + MARGIN_BOTTOM);
  protected BufferedImage image;
  protected byte[] bitmap;
  protected JPanel panel;

  public Shiromadokun (int number) {
    this.number = number;
    id = "shiromadokun" + number;
    nameEn = "Shiromadokun #" + number;
    nameJa = "白窓君 #" + number;
    specification = Settings.sgsGetInt (id);
    if (!(specification == JAPANESE_SPECIFICATION ||
          specification == EUROPEAN_SPECIFICATION)) {
      specification = JAPANESE_SPECIFICATION;
    }
    screenMemory = new byte[64 * 2];
    fontMemory = new byte[8 * 256 * 2];
    System.arraycopy (SMK6X8_FONT, 0,  //from
                      fontMemory, 0,  //to
                      8 * 256 * 2);  //length
    fontBaseAddress = 8 * 256 * specification;
    image = new BufferedImage (IMAGE_WIDTH,
                               IMAGE_HEIGHT,
                               BufferedImage.TYPE_BYTE_BINARY,
                               new IndexColorModel (1, 2,
                                                    new byte[] { (byte) 0x66,        0x00 },
                                                    new byte[] { (byte) 0xcc, (byte) 0x33 },
                                                    new byte[] { (byte) 0x99, (byte) 0x99 }));
    bitmap = ((DataBufferByte) image.getRaster ().getDataBuffer ()).getData ();
    panel = ComponentFactory.setFixedSize (
      new JPanel () {
        @Override protected void paintComponent (Graphics g) {
          if (displayOn) {
            drawBitmap ();
          }
          g.drawImage (image, 0, 0, null);
        }
      },
      IMAGE_WIDTH, IMAGE_HEIGHT);
    reset ();
    configurationPanel = null;
  }

  @Override public void tini () {
    Settings.sgsPutInt (id, specification);
  }

  @Override public final void reset () {
    pinE = 0;
    busData = 0;
    upper = -1;
    lower = -1;
    fontOrScreen = 0;
    fontAddress = 0;
    cursorAddress = 0;
    homeAddress = 0;
    Arrays.fill (screenMemory, (byte) 0x20);
    //Arrays.fill (fontMemory, (byte) 0);
    entryMode = 2;
    displayOn = true;
    underlineCursorOn = false;
    blinkingCursorOn = false;
    blinkingCursorMask = 0x00;
    blinkingCursorTask = null;
    setSpecification (specification);
  }

  protected final void setSpecification (int specification) {
    this.specification = specification;
    fontBaseAddress = 8 * 256 * specification;
  }

  @Override public JComponent getConfigurationPanel () {
    if (configurationPanel != null) {
      return configurationPanel;
    }

    ActionListener actionListener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        String command = ae.getActionCommand ();
        switch (command) {
        case "Japanese specification" :  //日本仕様
          setSpecification (JAPANESE_SPECIFICATION);
          panel.repaint ();
          break;
        case "European specification":  //欧州仕様
          setSpecification (EUROPEAN_SPECIFICATION);
          panel.repaint ();
          break;
        }
      }
    };

    ButtonGroup specificationGroup = new ButtonGroup ();
    //           0        1
    //    0        Title
    //    1          -
    //    2        Panel
    //    3          -
    //    4  日本仕様  欧州仕様
    return configurationPanel = ComponentFactory.createGridPanel (
      2, 5,
      "paddingLeft=3,paddingRight=3,center",   //gridStyles
      "",  //colStyles
      "colSpan=2;colSpan=2;colSpan=2;colSpan=2",  //rowStyles
      "",  //cellStyles
      Multilingual.mlnText (ComponentFactory.createLabel (getNameEn ()), "ja", getNameJa ()),  //(0,0)-(1,0)
      null,  //(0,1)-(1,1)
      panel,  //(0,2)-(1,2)
      null,  //(0,3)-(1,3)
      Multilingual.mlnText (
        ComponentFactory.createRadioButton (
          specificationGroup, specification == JAPANESE_SPECIFICATION, "Japanese specification", actionListener),
        "ja", "日本仕様"),  //(0,4)
      Multilingual.mlnText (
        ComponentFactory.createRadioButton (
          specificationGroup, specification == EUROPEAN_SPECIFICATION, "European specification", actionListener),
        "ja", "欧州仕様"));  //(1,4)
  }

  @Override public void setPin8 (int pin8) {
    if (pinE == 1 && pin8 == 0) {  //E=1→0
      if ((busData & 0x60) == 0x00) {  //コマンドレジスタ書き込み
        if (upper < 0) {  //上位4bit
          upper = busData & 0x0f;
          lower = -1;
        } else {  //下位4bit
          lower = busData & 0x0f;
          if (upper == 0x3 && lower == 0x3) {  //0x33のときは下位を繰り返す
            lower = -1;
          } else {
            int command = upper << 4 | lower;
            upper = -1;
            lower = -1;
            processCommand (command);  //コマンド処理
          }
        }
      } else if ((busData & 0x60) == 0x20) {  //データレジスタ書き込み
        if (upper < 0) {  //上位4bit
          upper = busData & 0x0f;
          lower = -1;
        } else {  //下位4bit
          lower = busData & 0x0f;
          int data = upper << 4 | lower;
          upper = -1;
          lower = -1;
          processData (data);  //データ処理
        }
      }
    }
    pinE = pin8;
  }
  @Override public int readByte () {
    return 0xff;
  }
  @Override public void writeByte (int d) {
    busData = d & 0xff;
  }

  @SuppressWarnings ("fallthrough") protected void processCommand (int command) {
    if (DEBUG) {
      System.out.printf ("command(0x%02x)\n", command);
    }
    if (command <= 0x03) {
      switch (command) {
      case 0x00:
        break;
      case 0x01:  //Clear display
        //画面クリア,カーソルアドレス0,ホームアドレス0
        Arrays.fill (screenMemory, (byte) 0x20);  //0ではない。Thanks @kani7
        //fallthrough
      case 0x02:  //Return home
      case 0x03:
        //カーソルアドレス0,ホームアドレス0
        homeAddress = 0;
        cursorAddress = 0;
        if (DEBUG) {
          System.out.printf ("homeAddress=%d\n", homeAddress);
          System.out.printf ("cursorAddress=%d\n", cursorAddress);
        }
        break;
      }
    } else if (command <= 0x3f) {
      switch (command >> 2) {
      case 0x04 >> 2:  //Entry mode set
        entryMode = command & 3;
        if (DEBUG) {
          System.out.printf ("entryMode=%d\n", entryMode);
        }
        break;
      case 0x08 >> 2:  //Display on/off control
        displayOn = false;  //表示OFF
        if (DEBUG) {
          System.out.printf ("displayOn=%b\n", displayOn);
        }
        Arrays.fill (bitmap, (byte) 0);
        break;
      case 0x0c >> 2:
        displayOn = true;  //表示ON
        underlineCursorOn = (command & 2) != 0;  //下線カーソルOFF/下線カーソルON
        blinkingCursorOn = (command & 1) != 0;  //点滅カーソルOFF/点滅カーソルON
        if (DEBUG) {
          System.out.printf ("displayOn=%b\n", displayOn);
          System.out.printf ("underlineCursorOn=%b\n", underlineCursorOn);
          System.out.printf ("blinkingCursorOn=%b\n", blinkingCursorOn);
        }
        if (blinkingCursorOn) {
          blinkingStart ();
        } else {
          blinkingEnd ();
        }
        break;
      case 0x10 >> 2:  //Cursor or display shift
        //カーソルアドレスデクリメント,左上0→右下64+39,左下64→右上39
        cursorAddress = cursorAddress == 0 ? 64 + 39 : cursorAddress == 64 ? 39 : cursorAddress - 1;
        if (DEBUG) {
          System.out.printf ("cursorAddress=%d\n", cursorAddress);
        }
        break;
      case 0x14 >> 2:
        //カーソルアドレスインクリメント,右上39→左下64,右下64+39→左上0
        cursorAddress = cursorAddress == 39 ? 64 : cursorAddress == 64 + 39 ? 0 : cursorAddress + 1;
        if (DEBUG) {
          System.out.printf ("cursorAddress=%d\n", cursorAddress);
        }
        break;
      case 0x18 >> 2:
        //ホームアドレスインクリメント(画面を左へシフト),39→0
        homeAddress = homeAddress == 39 ? 0 : homeAddress + 1;
        if (DEBUG) {
          System.out.printf ("homeAddress=%d\n", homeAddress);
        }
        break;
      case 0x1c >> 2:
        //ホームアドレスデクリメント(画面を右へシフト),0→39
        homeAddress = homeAddress == 0 ? 39 : homeAddress - 1;
        if (DEBUG) {
          System.out.printf ("homeAddress=%d\n", homeAddress);
        }
        break;
      case 0x20 >> 2:  //Function set
      case 0x24 >> 2:
      case 0x28 >> 2:
      case 0x2c >> 2:
      case 0x30 >> 2:
      case 0x34 >> 2:
      case 0x38 >> 2:
      case 0x3c >> 2:
        break;
      }
    } else if (command <= 0x7f) {  //フォントアドレス
      fontOrScreen = 0;
      fontAddress = command & 0x3f;
      if (DEBUG) {
        System.out.printf ("fontOrScreen=%d\n", fontOrScreen);
        System.out.printf ("fontAddress=%d\n", fontAddress);
      }
    } else {  //カーソルアドレス,40..63→64,64+40..64+63→0
      fontOrScreen = 1;
      int a = command & 0x7f;
      cursorAddress = a < 40 ? a : a < 64 ? 64 : a < 64 + 40 ? a : 0;
      if (DEBUG) {
        System.out.printf ("fontOrScreen=%d\n", fontOrScreen);
        System.out.printf ("cursorAddress=%d\n", cursorAddress);
      }
    }
    panel.repaint ();
  }

  @SuppressWarnings ("fallthrough") protected void processData (int data) {
    if (DEBUG) {
      System.out.printf ("data(0x%02x)\n", data);
    }
    if (fontOrScreen == 0) {  //フォント書き込み
      fontMemory[fontBaseAddress + fontAddress] =
        fontMemory[fontBaseAddress + fontAddress + 64] = (byte) (data << 3);  //上位に寄せる
      if (DEBUG) {
        System.out.printf ("fontMemory[%d]=0x%02x\n",
                           fontBaseAddress + fontAddress, (fontMemory[fontBaseAddress + fontAddress] & 0xff) >> 3);  //下位に寄せる
      }
      fontAddress = fontAddress == 63 ? 0 : fontAddress + 1;
      if (DEBUG) {
        System.out.printf ("fontAddress=%d\n", fontAddress);
      }
    } else {  //画面書き込み
      screenMemory[cursorAddress] = (byte) data;
      if (DEBUG) {
        System.out.printf ("screenMemory[%d]=0x%02x\n", cursorAddress, screenMemory[cursorAddress] & 0xff);
      }
      switch (entryMode) {
      case 1:  //文字を書いたらカーソルアドレスをデクリメント,ホームアドレスをデクリメント(画面を右へシフト),0→39
        homeAddress = homeAddress == 0 ? 39 : homeAddress - 1;
        if (DEBUG) {
          System.out.printf ("homeAddress=%d\n", homeAddress);
        }
        //fallthrough
      case 0:  //文字を書いたらカーソルアドレスをデクリメント(カーソルを左へ移動),左上0→右下64+39,左下64→右上39
        cursorAddress = cursorAddress == 0 ? 64 + 39 : cursorAddress == 64 ? 39 : cursorAddress - 1;
        if (DEBUG) {
          System.out.printf ("cursorAddress=%d\n", cursorAddress);
        }
        break;
      case 3:  //文字を書いたらカーソルアドレスをインクリメント,ホームアドレスをインクリメント(画面を左へシフト),39→0
        homeAddress = homeAddress == 39 ? 0 : homeAddress + 1;
        if (DEBUG) {
          System.out.printf ("homeAddress=%d\n", homeAddress);
        }
        //fallthrough
      case 2:  //文字を書いたらカーソルアドレスをインクリメント(カーソルを右へ移動),右上39→左下64,右下64+39→左上0
        cursorAddress = cursorAddress == 39 ? 64 : cursorAddress == 64 + 39 ? 0 : cursorAddress + 1;
        if (DEBUG) {
          System.out.printf ("cursorAddress=%d\n", cursorAddress);
        }
        break;
      }
    }
    panel.repaint ();
  }

  protected void drawBitmap () {
    int offset = (IMAGE_WIDTH + 7) >> 3;
    for (int screenY = 0; screenY < SCREEN_HEIGHT; screenY++) {
      for (int screenX = 0; screenX < SCREEN_WIDTH; screenX++) {
        int screenAddress = (screenX + homeAddress) % 40 + 64 * screenY;  //(screenX,screenY)の位置に表示する文字のアドレス
        int underlineCursorMask = 0x00;  //下線カーソルマスク。最下ラインにorする
        int blinkingMask = 0x00;  //点滅マスク。すべてのラインにorする
        if (screenAddress == cursorAddress) {
          if (underlineCursorOn) {  //下線カーソルON
            underlineCursorMask = 0x1f;
          }
          blinkingMask = blinkingCursorMask;
        }
        int fontStartAddress = (screenMemory[screenAddress] & 0xff) << 3;
        for (int characterY = 0; characterY < CHARACTER_HEIGHT; characterY++) {
          int y0 = DOT_HEIGHT * (MARGIN_TOP + (CHARACTER_HEIGHT + SPACE_HEIGHT) * screenY + characterY);
          int fontRaster = (fontMemory[fontBaseAddress + fontStartAddress + characterY] & 0xff) >> 3;  //下位に寄せる
          if (characterY == CHARACTER_HEIGHT - 1) {  //最下ライン
            fontRaster |= underlineCursorMask;
          }
          fontRaster |= blinkingMask;
          for (int characterX = 0; characterX < CHARACTER_WIDTH; characterX++) {
            int x0 = DOT_WIDTH * (MARGIN_LEFT + (CHARACTER_WIDTH + SPACE_WIDTH) * screenX + characterX);
            if ((fontRaster >> (CHARACTER_WIDTH - 1 - characterX) & 1) == 0) {
              for (int dotY = 0; dotY < DOT_HEIGHT; dotY++) {
                int y = y0 + dotY;
                for (int dotX = 0; dotX < DOT_WIDTH; dotX++) {
                  int x = x0 + dotX;
                  bitmap[offset * y + (x >> 3)] &= (byte) ~(0x80 >> (x & 7));
                }
              }
            } else {
              for (int dotY = 0; dotY < DOT_HEIGHT; dotY++) {
                int y = y0 + dotY;
                for (int dotX = 0; dotX < DOT_WIDTH; dotX++) {
                  int x = x0 + dotX;
                  bitmap[offset * y + (x >> 3)] |= (byte) (0x80 >> (x & 7));
                }
              }
            }
          }
        }
      }
    }
  }

  //  点滅開始
  protected void blinkingStart () {
    blinkingCursorMask = 0x00;
    if (blinkingCursorTask == null) {
      blinkingCursorTask = new BlinkingCursorTask ();
      XEiJ.tmrTimer.schedule (blinkingCursorTask, BLINKING_DELAY, BLINKING_INTERVAL);
    }
  }

  //  点滅終了
  protected void blinkingEnd () {
    if (blinkingCursorTask != null) {
      blinkingCursorTask.cancel ();
      blinkingCursorTask = null;
    }
    blinkingCursorMask = 0x00;
  }

  //  点滅タスク
  class BlinkingCursorTask extends TimerTask {
    @Override public void run () {
      blinkingCursorMask ^= 0x1f;
      panel.repaint ();
    }
  }

  //  白窓君のフォント
  //  CGROMに合わせて左に寄せてある。取り出すとき右に3ビットシフトする
/*
  public static final byte[] SMK6X8_FONT = {

    //日本標準フォント(日本仕様)
    //0x00
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x01
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x02
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x03
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x04
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x05
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x06
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x07
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x08
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x09
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x0a
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x0b
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x0c
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x0d
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x0e
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x0f
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x10
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x11
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x12
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x13
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x14
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x15
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x16
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x17
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x18
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x19
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x1a
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x1b
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x1c
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x1d
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x1e
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x1f
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x20
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x21 !
    0b00100000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b00000000,
    0b00000000,
    0b00100000,
    0b00000000,
    //0x22 "
    0b01010000,
    0b01010000,
    0b01010000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x23 #
    0b01010000,
    0b01010000,
    0b11111000,
    0b01010000,
    0b11111000,
    0b01010000,
    0b01010000,
    0b00000000,
    //0x24 $
    0b00100000,
    0b01111000,
    0b10100000,
    0b01110000,
    0b00101000,
    0b11110000,
    0b00100000,
    0b00000000,
    //0x25 %
    0b11000000,
    0b11001000,
    0b00010000,
    0b00100000,
    0b01000000,
    0b10011000,
    0b00011000,
    0b00000000,
    //0x26 &
    0b01100000,
    0b10010000,
    0b10100000,
    0b01000000,
    0b10101000,
    0b10010000,
    0b01101000,
    0b00000000,
    //0x27 '
    0b01100000,
    0b00100000,
    0b01000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x28 (
    0b00010000,
    0b00100000,
    0b01000000,
    0b01000000,
    0b01000000,
    0b00100000,
    0b00010000,
    0b00000000,
    //0x29 )
    0b01000000,
    0b00100000,
    0b00010000,
    0b00010000,
    0b00010000,
    0b00100000,
    0b01000000,
    0b00000000,
    //0x2a *
    0b00000000,
    0b00100000,
    0b10101000,
    0b01110000,
    0b10101000,
    0b00100000,
    0b00000000,
    0b00000000,
    //0x2b +
    0b00000000,
    0b00100000,
    0b00100000,
    0b11111000,
    0b00100000,
    0b00100000,
    0b00000000,
    0b00000000,
    //0x2c ,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b01100000,
    0b00100000,
    0b01000000,
    0b00000000,
    //0x2d -
    0b00000000,
    0b00000000,
    0b00000000,
    0b11111000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x2e .
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b01100000,
    0b01100000,
    0b00000000,
    //0x2f /
    0b00000000,
    0b00001000,
    0b00010000,
    0b00100000,
    0b01000000,
    0b10000000,
    0b00000000,
    0b00000000,
    //0x30 0
    0b01110000,
    0b10001000,
    0b10011000,
    0b10101000,
    0b11001000,
    0b10001000,
    0b01110000,
    0b00000000,
    //0x31 1
    0b00100000,
    0b01100000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b01110000,
    0b00000000,
    //0x32 2
    0b01110000,
    0b10001000,
    0b00001000,
    0b00010000,
    0b00100000,
    0b01000000,
    0b11111000,
    0b00000000,
    //0x33 3
    0b11111000,
    0b00010000,
    0b00100000,
    0b00010000,
    0b00001000,
    0b10001000,
    0b01110000,
    0b00000000,
    //0x34 4
    0b00010000,
    0b00110000,
    0b01010000,
    0b10010000,
    0b11111000,
    0b00010000,
    0b00010000,
    0b00000000,
    //0x35 5
    0b11111000,
    0b10000000,
    0b11110000,
    0b00001000,
    0b00001000,
    0b10001000,
    0b01110000,
    0b00000000,
    //0x36 6
    0b00110000,
    0b01000000,
    0b10000000,
    0b11110000,
    0b10001000,
    0b10001000,
    0b01110000,
    0b00000000,
    //0x37 7
    0b11111000,
    0b00001000,
    0b00010000,
    0b00100000,
    0b01000000,
    0b01000000,
    0b01000000,
    0b00000000,
    //0x38 8
    0b01110000,
    0b10001000,
    0b10001000,
    0b01110000,
    0b10001000,
    0b10001000,
    0b01110000,
    0b00000000,
    //0x39 9
    0b01110000,
    0b10001000,
    0b10001000,
    0b01111000,
    0b00001000,
    0b00010000,
    0b01100000,
    0b00000000,
    //0x3a :
    0b00000000,
    0b01100000,
    0b01100000,
    0b00000000,
    0b01100000,
    0b01100000,
    0b00000000,
    0b00000000,
    //0x3b ;
    0b00000000,
    0b01100000,
    0b01100000,
    0b00000000,
    0b01100000,
    0b00100000,
    0b01000000,
    0b00000000,
    //0x3c <
    0b00010000,
    0b00100000,
    0b01000000,
    0b10000000,
    0b01000000,
    0b00100000,
    0b00010000,
    0b00000000,
    //0x3d =
    0b00000000,
    0b00000000,
    0b11111000,
    0b00000000,
    0b11111000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x3e >
    0b01000000,
    0b00100000,
    0b00010000,
    0b00001000,
    0b00010000,
    0b00100000,
    0b01000000,
    0b00000000,
    //0x3f ?
    0b01110000,
    0b10001000,
    0b00001000,
    0b00010000,
    0b00100000,
    0b00000000,
    0b00100000,
    0b00000000,
    //0x40 @
    0b01110000,
    0b10001000,
    0b00001000,
    0b01101000,
    0b10101000,
    0b10101000,
    0b01110000,
    0b00000000,
    //0x41 A
    0b01110000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b11111000,
    0b10001000,
    0b10001000,
    0b00000000,
    //0x42 B
    0b11110000,
    0b10001000,
    0b10001000,
    0b11110000,
    0b10001000,
    0b10001000,
    0b11110000,
    0b00000000,
    //0x43 C
    0b01110000,
    0b10001000,
    0b10000000,
    0b10000000,
    0b10000000,
    0b10001000,
    0b01110000,
    0b00000000,
    //0x44 D
    0b11100000,
    0b10010000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10010000,
    0b11100000,
    0b00000000,
    //0x45 E
    0b11111000,
    0b10000000,
    0b10000000,
    0b11110000,
    0b10000000,
    0b10000000,
    0b11111000,
    0b00000000,
    //0x46 F
    0b11111000,
    0b10000000,
    0b10000000,
    0b11110000,
    0b10000000,
    0b10000000,
    0b10000000,
    0b00000000,
    //0x47 G
    0b01110000,
    0b10001000,
    0b10000000,
    0b10111000,
    0b10001000,
    0b10001000,
    0b01111000,
    0b00000000,
    //0x48 H
    0b10001000,
    0b10001000,
    0b10001000,
    0b11111000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b00000000,
    //0x49 I
    0b01110000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b01110000,
    0b00000000,
    //0x4a J
    0b00111000,
    0b00010000,
    0b00010000,
    0b00010000,
    0b00010000,
    0b10010000,
    0b01100000,
    0b00000000,
    //0x4b K
    0b10001000,
    0b10010000,
    0b10100000,
    0b11000000,
    0b10100000,
    0b10010000,
    0b10001000,
    0b00000000,
    //0x4c L
    0b10000000,
    0b10000000,
    0b10000000,
    0b10000000,
    0b10000000,
    0b10000000,
    0b11111000,
    0b00000000,
    //0x4d M
    0b10001000,
    0b11011000,
    0b10101000,
    0b10101000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b00000000,
    //0x4e N
    0b10001000,
    0b10001000,
    0b11001000,
    0b10101000,
    0b10011000,
    0b10001000,
    0b10001000,
    0b00000000,
    //0x4f O
    0b01110000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b01110000,
    0b00000000,
    //0x50 P
    0b11110000,
    0b10001000,
    0b10001000,
    0b11110000,
    0b10000000,
    0b10000000,
    0b10000000,
    0b00000000,
    //0x51 Q
    0b01110000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10101000,
    0b10010000,
    0b01101000,
    0b00000000,
    //0x52 R
    0b11110000,
    0b10001000,
    0b10001000,
    0b11110000,
    0b10100000,
    0b10010000,
    0b10001000,
    0b00000000,
    //0x53 S
    0b01111000,
    0b10000000,
    0b10000000,
    0b01110000,
    0b00001000,
    0b00001000,
    0b11110000,
    0b00000000,
    //0x54 T
    0b11111000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b00000000,
    //0x55 U
    0b10001000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b01110000,
    0b00000000,
    //0x56 V
    0b10001000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b01010000,
    0b00100000,
    0b00000000,
    //0x57 W
    0b10001000,
    0b10001000,
    0b10001000,
    0b10101000,
    0b10101000,
    0b10101000,
    0b01010000,
    0b00000000,
    //0x58 X
    0b10001000,
    0b10001000,
    0b01010000,
    0b00100000,
    0b01010000,
    0b10001000,
    0b10001000,
    0b00000000,
    //0x59 Y
    0b10001000,
    0b10001000,
    0b10001000,
    0b01010000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b00000000,
    //0x5a Z
    0b11111000,
    0b00001000,
    0b00010000,
    0b00100000,
    0b01000000,
    0b10000000,
    0b11111000,
    0b00000000,
    //0x5b [
    0b01110000,
    0b01000000,
    0b01000000,
    0b01000000,
    0b01000000,
    0b01000000,
    0b01110000,
    0b00000000,
    //0x5c \\
    0b10001000,
    0b01010000,
    0b11111000,
    0b00100000,
    0b11111000,
    0b00100000,
    0b00100000,
    0b00000000,
    //0x5d ]
    0b01110000,
    0b00010000,
    0b00010000,
    0b00010000,
    0b00010000,
    0b00010000,
    0b01110000,
    0b00000000,
    //0x5e ^
    0b00100000,
    0b01010000,
    0b10001000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x5f _
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b11111000,
    0b00000000,
    //0x60 `
    0b01000000,
    0b00100000,
    0b00010000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x61 a
    0b00000000,
    0b00000000,
    0b01110000,
    0b00001000,
    0b01111000,
    0b10001000,
    0b01111000,
    0b00000000,
    //0x62 b
    0b10000000,
    0b10000000,
    0b10110000,
    0b11001000,
    0b10001000,
    0b10001000,
    0b11110000,
    0b00000000,
    //0x63 c
    0b00000000,
    0b00000000,
    0b01110000,
    0b10000000,
    0b10000000,
    0b10001000,
    0b01110000,
    0b00000000,
    //0x64 d
    0b00001000,
    0b00001000,
    0b01101000,
    0b10011000,
    0b10001000,
    0b10001000,
    0b01111000,
    0b00000000,
    //0x65 e
    0b00000000,
    0b00000000,
    0b01110000,
    0b10001000,
    0b11111000,
    0b10000000,
    0b01110000,
    0b00000000,
    //0x66 f
    0b00110000,
    0b01001000,
    0b01000000,
    0b11100000,
    0b01000000,
    0b01000000,
    0b01000000,
    0b00000000,
    //0x67 g
    0b00000000,
    0b01111000,
    0b10001000,
    0b10001000,
    0b01111000,
    0b00001000,
    0b01110000,
    0b00000000,
    //0x68 h
    0b10000000,
    0b10000000,
    0b10110000,
    0b11001000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b00000000,
    //0x69 i
    0b00100000,
    0b00000000,
    0b01100000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b01110000,
    0b00000000,
    //0x6a j
    0b00010000,
    0b00000000,
    0b00110000,
    0b00010000,
    0b00010000,
    0b10010000,
    0b01100000,
    0b00000000,
    //0x6b k
    0b10000000,
    0b10000000,
    0b10010000,
    0b10100000,
    0b11000000,
    0b10100000,
    0b10010000,
    0b00000000,
    //0x6c l
    0b01100000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b01110000,
    0b00000000,
    //0x6d m
    0b00000000,
    0b00000000,
    0b11010000,
    0b10101000,
    0b10101000,
    0b10001000,
    0b10001000,
    0b00000000,
    //0x6e n
    0b00000000,
    0b00000000,
    0b10110000,
    0b11001000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b00000000,
    //0x6f o
    0b00000000,
    0b00000000,
    0b01110000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b01110000,
    0b00000000,
    //0x70 p
    0b00000000,
    0b00000000,
    0b11110000,
    0b10001000,
    0b11110000,
    0b10000000,
    0b10000000,
    0b00000000,
    //0x71 q
    0b00000000,
    0b00000000,
    0b01101000,
    0b10011000,
    0b01111000,
    0b00001000,
    0b00001000,
    0b00000000,
    //0x72 r
    0b00000000,
    0b00000000,
    0b10110000,
    0b11001000,
    0b10000000,
    0b10000000,
    0b10000000,
    0b00000000,
    //0x73 s
    0b00000000,
    0b00000000,
    0b01110000,
    0b10000000,
    0b01110000,
    0b00001000,
    0b11110000,
    0b00000000,
    //0x74 t
    0b01000000,
    0b01000000,
    0b11100000,
    0b01000000,
    0b01000000,
    0b01001000,
    0b00110000,
    0b00000000,
    //0x75 u
    0b00000000,
    0b00000000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10011000,
    0b01101000,
    0b00000000,
    //0x76 v
    0b00000000,
    0b00000000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b01010000,
    0b00100000,
    0b00000000,
    //0x77 w
    0b00000000,
    0b00000000,
    0b10001000,
    0b10001000,
    0b10101000,
    0b10101000,
    0b01010000,
    0b00000000,
    //0x78 x
    0b00000000,
    0b00000000,
    0b10001000,
    0b01010000,
    0b00100000,
    0b01010000,
    0b10001000,
    0b00000000,
    //0x79 y
    0b00000000,
    0b00000000,
    0b10001000,
    0b10001000,
    0b01111000,
    0b00001000,
    0b01110000,
    0b00000000,
    //0x7a z
    0b00000000,
    0b00000000,
    0b11111000,
    0b00010000,
    0b00100000,
    0b01000000,
    0b11111000,
    0b00000000,
    //0x7b {
    0b00010000,
    0b00100000,
    0b00100000,
    0b01000000,
    0b00100000,
    0b00100000,
    0b00010000,
    0b00000000,
    //0x7c |
    0b00100000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b00000000,
    //0x7d }
    0b01000000,
    0b00100000,
    0b00100000,
    0b00010000,
    0b00100000,
    0b00100000,
    0b01000000,
    0b00000000,
    //0x7e →
    0b00000000,
    0b00100000,
    0b00010000,
    0b11111000,
    0b00010000,
    0b00100000,
    0b00000000,
    0b00000000,
    //0x7f ←
    0b00000000,
    0b00100000,
    0b01000000,
    0b11111000,
    0b01000000,
    0b00100000,
    0b00000000,
    0b00000000,
    //0x80
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x81
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x82
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x83
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x84
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x85
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x86
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x87
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x88
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x89
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x8a
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x8b
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x8c
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x8d
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x8e
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x8f
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x90
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x91
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x92
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x93
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x94
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x95
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x96
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x97
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x98
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x99
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x9a
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x9b
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x9c
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x9d
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x9e
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x9f
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0xa0
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0xa1 。
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b11100000,
    0b10100000,
    0b11100000,
    0b00000000,
    //0xa2 「
    0b00111000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0xa3 」
    0b00000000,
    0b00000000,
    0b00000000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b11100000,
    0b00000000,
    //0xa4 、
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b10000000,
    0b01000000,
    0b00100000,
    0b00000000,
    //0xa5 ・
    0b00000000,
    0b00000000,
    0b00000000,
    0b01100000,
    0b01100000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0xa6 ヲ
    0b00000000,
    0b11111000,
    0b00001000,
    0b11111000,
    0b00001000,
    0b00010000,
    0b00100000,
    0b00000000,
    //0xa7 ァ
    0b00000000,
    0b00000000,
    0b11111000,
    0b00001000,
    0b00110000,
    0b00100000,
    0b01000000,
    0b00000000,
    //0xa8 ィ
    0b00000000,
    0b00000000,
    0b00010000,
    0b00100000,
    0b01100000,
    0b10100000,
    0b00100000,
    0b00000000,
    //0xa9 ゥ
    0b00000000,
    0b00000000,
    0b00100000,
    0b11111000,
    0b10001000,
    0b00001000,
    0b00110000,
    0b00000000,
    //0xaa ェ
    0b00000000,
    0b00000000,
    0b00000000,
    0b11111000,
    0b00100000,
    0b00100000,
    0b11111000,
    0b00000000,
    //0xab ォ
    0b00000000,
    0b00000000,
    0b00010000,
    0b11111000,
    0b00110000,
    0b01010000,
    0b10010000,
    0b00000000,
    //0xac ャ
    0b00000000,
    0b00000000,
    0b01000000,
    0b11111000,
    0b01001000,
    0b01010000,
    0b01000000,
    0b00000000,
    //0xad ュ
    0b00000000,
    0b00000000,
    0b00000000,
    0b01110000,
    0b00010000,
    0b00010000,
    0b11111000,
    0b00000000,
    //0xae ョ
    0b00000000,
    0b00000000,
    0b11110000,
    0b00010000,
    0b11110000,
    0b00010000,
    0b11110000,
    0b00000000,
    //0xaf ッ
    0b00000000,
    0b00000000,
    0b00000000,
    0b10101000,
    0b10101000,
    0b00001000,
    0b00110000,
    0b00000000,
    //0xb0 ー
    0b00000000,
    0b00000000,
    0b00000000,
    0b11111000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0xb1 ア
    0b11111000,
    0b00001000,
    0b00101000,
    0b00110000,
    0b00100000,
    0b00100000,
    0b01000000,
    0b00000000,
    //0xb2 イ
    0b00001000,
    0b00010000,
    0b00100000,
    0b01100000,
    0b10100000,
    0b00100000,
    0b00100000,
    0b00000000,
    //0xb3 ウ
    0b00100000,
    0b11111000,
    0b10001000,
    0b10001000,
    0b00001000,
    0b00010000,
    0b00100000,
    0b00000000,
    //0xb4 エ
    0b00000000,
    0b11111000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b11111000,
    0b00000000,
    //0xb5 オ
    0b00010000,
    0b11111000,
    0b00010000,
    0b00110000,
    0b01010000,
    0b10010000,
    0b00010000,
    0b00000000,
    //0xb6 カ
    0b01000000,
    0b11111000,
    0b01001000,
    0b01001000,
    0b01001000,
    0b01001000,
    0b10010000,
    0b00000000,
    //0xb7 キ
    0b00100000,
    0b11111000,
    0b00100000,
    0b11111000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b00000000,
    //0xb8 ク
    0b00000000,
    0b01111000,
    0b01001000,
    0b10001000,
    0b00001000,
    0b00010000,
    0b01100000,
    0b00000000,
    //0xb9 ケ
    0b01000000,
    0b01111000,
    0b10010000,
    0b00010000,
    0b00010000,
    0b00010000,
    0b00100000,
    0b00000000,
    //0xba コ
    0b00000000,
    0b11111000,
    0b00001000,
    0b00001000,
    0b00001000,
    0b00001000,
    0b11111000,
    0b00000000,
    //0xbb サ
    0b01010000,
    0b11111000,
    0b01010000,
    0b01010000,
    0b00010000,
    0b00100000,
    0b01000000,
    0b00000000,
    //0xbc シ
    0b00000000,
    0b11000000,
    0b00001000,
    0b11001000,
    0b00001000,
    0b00010000,
    0b11100000,
    0b00000000,
    //0xbd ス
    0b00000000,
    0b11111000,
    0b00001000,
    0b00010000,
    0b00100000,
    0b01010000,
    0b10001000,
    0b00000000,
    //0xbe セ
    0b01000000,
    0b11111000,
    0b01001000,
    0b01010000,
    0b01000000,
    0b01000000,
    0b00111000,
    0b00000000,
    //0xbf ソ
    0b00000000,
    0b10001000,
    0b10001000,
    0b01001000,
    0b00001000,
    0b00010000,
    0b01100000,
    0b00000000,
    //0xc0 タ
    0b00000000,
    0b01111000,
    0b01001000,
    0b10101000,
    0b00011000,
    0b00010000,
    0b01100000,
    0b00000000,
    //0xc1 チ
    0b00010000,
    0b11100000,
    0b00100000,
    0b11111000,
    0b00100000,
    0b00100000,
    0b01000000,
    0b00000000,
    //0xc2 ツ
    0b00000000,
    0b10101000,
    0b10101000,
    0b10101000,
    0b00001000,
    0b00010000,
    0b00100000,
    0b00000000,
    //0xc3 テ
    0b01110000,
    0b00000000,
    0b11111000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b01000000,
    0b00000000,
    //0xc4 ト
    0b01000000,
    0b01000000,
    0b01000000,
    0b01100000,
    0b01010000,
    0b01000000,
    0b01000000,
    0b00000000,
    //0xc5 ナ
    0b00100000,
    0b00100000,
    0b11111000,
    0b00100000,
    0b00100000,
    0b01000000,
    0b10000000,
    0b00000000,
    //0xc6 ニ
    0b00000000,
    0b01110000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b11111000,
    0b00000000,
    //0xc7 ヌ
    0b00000000,
    0b11111000,
    0b00001000,
    0b01010000,
    0b00100000,
    0b01010000,
    0b10000000,
    0b00000000,
    //0xc8 ネ
    0b00100000,
    0b11111000,
    0b00010000,
    0b00100000,
    0b01110000,
    0b10101000,
    0b00100000,
    0b00000000,
    //0xc9 ノ
    0b00010000,
    0b00010000,
    0b00010000,
    0b00010000,
    0b00010000,
    0b00100000,
    0b01000000,
    0b00000000,
    //0xca ハ
    0b00000000,
    0b00100000,
    0b00010000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b00000000,
    //0xcb ヒ
    0b10000000,
    0b10000000,
    0b11111000,
    0b10000000,
    0b10000000,
    0b10000000,
    0b01111000,
    0b00000000,
    //0xcc フ
    0b00000000,
    0b11111000,
    0b00001000,
    0b00001000,
    0b00001000,
    0b00010000,
    0b01100000,
    0b00000000,
    //0xcd ヘ
    0b00000000,
    0b01000000,
    0b10100000,
    0b00010000,
    0b00001000,
    0b00001000,
    0b00000000,
    0b00000000,
    //0xce ホ
    0b00100000,
    0b11111000,
    0b00100000,
    0b00100000,
    0b10101000,
    0b10101000,
    0b00100000,
    0b00000000,
    //0xcf マ
    0b00000000,
    0b11111000,
    0b00001000,
    0b00001000,
    0b01010000,
    0b00100000,
    0b00010000,
    0b00000000,
    //0xd0 ミ
    0b00000000,
    0b01110000,
    0b00000000,
    0b01110000,
    0b00000000,
    0b01110000,
    0b00001000,
    0b00000000,
    //0xd1 ム
    0b00000000,
    0b00100000,
    0b01000000,
    0b10000000,
    0b10001000,
    0b11111000,
    0b00001000,
    0b00000000,
    //0xd2 メ
    0b00000000,
    0b00001000,
    0b00001000,
    0b01010000,
    0b00100000,
    0b01010000,
    0b10000000,
    0b00000000,
    //0xd3 モ
    0b00000000,
    0b11111000,
    0b01000000,
    0b11111000,
    0b01000000,
    0b01000000,
    0b00111000,
    0b00000000,
    //0xd4 ヤ
    0b01000000,
    0b01000000,
    0b11111000,
    0b01001000,
    0b01010000,
    0b01000000,
    0b01000000,
    0b00000000,
    //0xd5 ユ
    0b00000000,
    0b01110000,
    0b00010000,
    0b00010000,
    0b00010000,
    0b00010000,
    0b11111000,
    0b00000000,
    //0xd6 ヨ
    0b00000000,
    0b11111000,
    0b00001000,
    0b11111000,
    0b00001000,
    0b00001000,
    0b11111000,
    0b00000000,
    //0xd7 ラ
    0b01110000,
    0b00000000,
    0b11111000,
    0b00001000,
    0b00001000,
    0b00010000,
    0b00100000,
    0b00000000,
    //0xd8 リ
    0b10010000,
    0b10010000,
    0b10010000,
    0b10010000,
    0b00010000,
    0b00100000,
    0b01000000,
    0b00000000,
    //0xd9 ル
    0b00000000,
    0b00100000,
    0b10100000,
    0b10100000,
    0b10101000,
    0b10101000,
    0b10110000,
    0b00000000,
    //0xda レ
    0b00000000,
    0b10000000,
    0b10000000,
    0b10001000,
    0b10010000,
    0b10100000,
    0b11000000,
    0b00000000,
    //0xdb ロ
    0b00000000,
    0b11111000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b11111000,
    0b00000000,
    //0xdc ワ
    0b00000000,
    0b11111000,
    0b10001000,
    0b10001000,
    0b00001000,
    0b00010000,
    0b00100000,
    0b00000000,
    //0xdd ン
    0b00000000,
    0b11000000,
    0b00000000,
    0b00001000,
    0b00001000,
    0b00010000,
    0b11100000,
    0b00000000,
    //0xde ゛
    0b00100000,
    0b10010000,
    0b01000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0xdf ゜
    0b11100000,
    0b10100000,
    0b11100000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0xe0 α
    0b00000000,
    0b00000000,
    0b01001000,
    0b10101000,
    0b10010000,
    0b10010000,
    0b01101000,
    0b00000000,
    //0xe1 ä
    0b01010000,
    0b00000000,
    0b01110000,
    0b00001000,
    0b01111000,
    0b10001000,
    0b01111000,
    0b00000000,
    //0xe2 β
    0b00000000,
    0b00000000,
    0b01110000,
    0b10001000,
    0b11110000,
    0b10001000,
    0b11110000,
    0b10000000,
    //0xe3 ε
    0b00000000,
    0b00000000,
    0b01110000,
    0b10000000,
    0b01100000,
    0b10001000,
    0b01110000,
    0b00000000,
    //0xe4 μ
    0b00000000,
    0b00000000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10011000,
    0b11101000,
    0b10000000,
    //0xe5 δ
    0b00000000,
    0b00000000,
    0b01111000,
    0b10100000,
    0b10010000,
    0b10001000,
    0b01110000,
    0b00000000,
    //0xe6 ρ
    0b00000000,
    0b00000000,
    0b00110000,
    0b01001000,
    0b10001000,
    0b10001000,
    0b11110000,
    0b10000000,
    //0xe7 g
    0b00000000,
    0b00000000,
    0b01111000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b01111000,
    0b00001000,
    //0xe8 √
    0b00000000,
    0b00000000,
    0b00111000,
    0b00100000,
    0b00100000,
    0b10100000,
    0b01000000,
    0b00000000,
    //0xe9
    0b00000000,
    0b00010000,
    0b11010000,
    0b00010000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0xea j
    0b00010000,
    0b00000000,
    0b00110000,
    0b00010000,
    0b00010000,
    0b00010000,
    0b00010000,
    0b00010000,
    //0xeb
    0b00000000,
    0b10100000,
    0b01000000,
    0b10100000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0xec ￠
    0b00000000,
    0b00100000,
    0b01110000,
    0b10100000,
    0b10101000,
    0b01110000,
    0b00100000,
    0b00000000,
    //0xed
    0b01000000,
    0b01000000,
    0b11100000,
    0b01000000,
    0b11100000,
    0b01000000,
    0b01111000,
    0b00000000,
    //0xee
    0b01110000,
    0b00000000,
    0b10110000,
    0b11001000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b00000000,
    //0xef
    0b01010000,
    0b00000000,
    0b01110000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b01110000,
    0b00000000,
    //0xf0 p
    0b00000000,
    0b00000000,
    0b10110000,
    0b11001000,
    0b10001000,
    0b10001000,
    0b11110000,
    0b10000000,
    //0xf1 q
    0b00000000,
    0b01100000,
    0b10011000,
    0b10001000,
    0b10001000,
    0b01111000,
    0b00001000,
    0b00001000,
    //0xf2 θ
    0b00000000,
    0b01110000,
    0b10001000,
    0b11111000,
    0b10001000,
    0b10001000,
    0b01110000,
    0b00000000,
    //0xf3 ∞
    0b00000000,
    0b00000000,
    0b00000000,
    0b01011000,
    0b10101000,
    0b11010000,
    0b00000000,
    0b00000000,
    //0xf4 Ω
    0b00000000,
    0b00000000,
    0b01110000,
    0b10001000,
    0b10001000,
    0b01010000,
    0b11011000,
    0b00000000,
    //0xf5
    0b01010000,
    0b00000000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10011000,
    0b01101000,
    //0xf6 Σ
    0b11111000,
    0b10000000,
    0b01000000,
    0b00100000,
    0b01000000,
    0b10000000,
    0b11111000,
    0b00000000,
    //0xf7 π
    0b00000000,
    0b00000000,
    0b11111000,
    0b01010000,
    0b01010000,
    0b01010000,
    0b10011000,
    0b00000000,
    //0xf8
    0b11111000,
    0b00000000,
    0b10001000,
    0b01010000,
    0b00100000,
    0b01010000,
    0b10001000,
    0b00000000,
    //0xf9 y
    0b00000000,
    0b00000000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b01111000,
    0b00001000,
    //0xfa 千
    0b00000000,
    0b00001000,
    0b11110000,
    0b00100000,
    0b11111000,
    0b00100000,
    0b00100000,
    0b00000000,
    //0xfb 万
    0b00000000,
    0b00000000,
    0b11111000,
    0b01000000,
    0b01111000,
    0b01001000,
    0b10001000,
    0b00000000,
    //0xfc 円
    0b00000000,
    0b00000000,
    0b11111000,
    0b10101000,
    0b11111000,
    0b10001000,
    0b10001000,
    0b00000000,
    //0xfd ÷
    0b00000000,
    0b00100000,
    0b00000000,
    0b11111000,
    0b00000000,
    0b00100000,
    0b00000000,
    0b00000000,
    //0xfe
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0xff
    0b11111000,
    0b11111000,
    0b11111000,
    0b11111000,
    0b11111000,
    0b11111000,
    0b11111000,
    0b11111000,

    //欧州標準フォント(欧州仕様)
    //  日本標準フォントと異なり、下に寄っている
    //0x00
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x01
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x02
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x03
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x04
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x05
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x06
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x07
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x08
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x09
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x0a
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x0b
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x0c
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x0d
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x0e
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x0f
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x10
    0b00000000,
    0b01000000,
    0b01100000,
    0b01110000,
    0b01111000,
    0b01110000,
    0b01100000,
    0b01000000,
    //0x11
    0b00000000,
    0b00010000,
    0b00110000,
    0b01110000,
    0b11110000,
    0b01110000,
    0b00110000,
    0b00010000,
    //0x12
    0b00000000,
    0b01001000,
    0b10010000,
    0b11011000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x13
    0b00000000,
    0b11011000,
    0b01001000,
    0b10010000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x14
    0b00000000,
    0b00100000,
    0b01110000,
    0b11111000,
    0b00000000,
    0b00100000,
    0b01110000,
    0b11111000,
    //0x15
    0b00000000,
    0b11111000,
    0b01110000,
    0b00100000,
    0b00000000,
    0b11111000,
    0b01110000,
    0b00100000,
    //0x16
    0b00000000,
    0b00000000,
    0b01110000,
    0b11111000,
    0b11111000,
    0b11111000,
    0b01110000,
    0b00000000,
    //0x17
    0b00000000,
    0b00001000,
    0b00001000,
    0b00101000,
    0b01001000,
    0b11111000,
    0b01000000,
    0b00100000,
    //0x18
    0b00000000,
    0b00100000,
    0b01110000,
    0b10101000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b00100000,
    //0x19
    0b00000000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b10101000,
    0b01110000,
    0b00100000,
    //0x1a
    0b00000000,
    0b00000000,
    0b00100000,
    0b00010000,
    0b11111000,
    0b00010000,
    0b00100000,
    0b00000000,
    //0x1b
    0b00000000,
    0b00000000,
    0b00100000,
    0b01000000,
    0b11111000,
    0b01000000,
    0b00100000,
    0b00000000,
    //0x1c
    0b00000000,
    0b00010000,
    0b00100000,
    0b01000000,
    0b00100000,
    0b00010000,
    0b00000000,
    0b11111000,
    //0x1d
    0b00000000,
    0b01000000,
    0b00100000,
    0b00010000,
    0b00100000,
    0b01000000,
    0b00000000,
    0b11111000,
    //0x1e
    0b00000000,
    0b00000000,
    0b00100000,
    0b00100000,
    0b01110000,
    0b01110000,
    0b11111000,
    0b00000000,
    //0x1f
    0b00000000,
    0b00000000,
    0b11111000,
    0b01110000,
    0b01110000,
    0b00100000,
    0b00100000,
    0b00000000,
    //0x20
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x21
    0b00000000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b00000000,
    0b00000000,
    0b00100000,
    //0x22
    0b00000000,
    0b01010000,
    0b01010000,
    0b01010000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x23
    0b00000000,
    0b01010000,
    0b01010000,
    0b11111000,
    0b01010000,
    0b11111000,
    0b01010000,
    0b01010000,
    //0x24
    0b00000000,
    0b00100000,
    0b01111000,
    0b10100000,
    0b01110000,
    0b00101000,
    0b11110000,
    0b00100000,
    //0x25
    0b00000000,
    0b11000000,
    0b11001000,
    0b00010000,
    0b00100000,
    0b01000000,
    0b10011000,
    0b00011000,
    //0x26
    0b00000000,
    0b01100000,
    0b10010000,
    0b10100000,
    0b01000000,
    0b10101000,
    0b10010000,
    0b01101000,
    //0x27
    0b00000000,
    0b01100000,
    0b00100000,
    0b01000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x28
    0b00000000,
    0b00010000,
    0b00100000,
    0b01000000,
    0b01000000,
    0b01000000,
    0b00100000,
    0b00010000,
    //0x29
    0b00000000,
    0b01000000,
    0b00100000,
    0b00010000,
    0b00010000,
    0b00010000,
    0b00100000,
    0b01000000,
    //0x2a
    0b00000000,
    0b00000000,
    0b00100000,
    0b10101000,
    0b01110000,
    0b10101000,
    0b00100000,
    0b00000000,
    //0x2b
    0b00000000,
    0b00000000,
    0b00100000,
    0b00100000,
    0b11111000,
    0b00100000,
    0b00100000,
    0b00000000,
    //0x2c
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b01100000,
    0b00100000,
    0b01000000,
    //0x2d
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b11111000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x2e
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b01100000,
    0b01100000,
    //0x2f
    0b00000000,
    0b00000000,
    0b00001000,
    0b00010000,
    0b00100000,
    0b01000000,
    0b10000000,
    0b00000000,
    //0x30
    0b00000000,
    0b01110000,
    0b10001000,
    0b10011000,
    0b10101000,
    0b11001000,
    0b10001000,
    0b01110000,
    //0x31
    0b00000000,
    0b00100000,
    0b01100000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b01110000,
    //0x32
    0b00000000,
    0b01110000,
    0b10001000,
    0b00001000,
    0b00010000,
    0b00100000,
    0b01000000,
    0b11111000,
    //0x33
    0b00000000,
    0b11111000,
    0b00010000,
    0b00100000,
    0b00010000,
    0b00001000,
    0b10001000,
    0b01110000,
    //0x34
    0b00000000,
    0b00010000,
    0b00110000,
    0b01010000,
    0b10010000,
    0b11111000,
    0b00010000,
    0b00010000,
    //0x35
    0b00000000,
    0b11111000,
    0b10000000,
    0b11110000,
    0b00001000,
    0b00001000,
    0b10001000,
    0b01110000,
    //0x36
    0b00000000,
    0b00110000,
    0b01000000,
    0b10000000,
    0b11110000,
    0b10001000,
    0b10001000,
    0b01110000,
    //0x37
    0b00000000,
    0b11111000,
    0b10001000,
    0b00001000,
    0b00010000,
    0b00100000,
    0b00100000,
    0b00100000,
    //0x38
    0b00000000,
    0b01110000,
    0b10001000,
    0b10001000,
    0b01110000,
    0b10001000,
    0b10001000,
    0b01110000,
    //0x39
    0b00000000,
    0b01110000,
    0b10001000,
    0b10001000,
    0b01111000,
    0b00001000,
    0b00010000,
    0b01100000,
    //0x3a
    0b00000000,
    0b00000000,
    0b01100000,
    0b01100000,
    0b00000000,
    0b01100000,
    0b01100000,
    0b00000000,
    //0x3b
    0b00000000,
    0b00000000,
    0b01100000,
    0b01100000,
    0b00000000,
    0b01100000,
    0b00100000,
    0b01000000,
    //0x3c
    0b00000000,
    0b00010000,
    0b00100000,
    0b01000000,
    0b10000000,
    0b01000000,
    0b00100000,
    0b00010000,
    //0x3d
    0b00000000,
    0b00000000,
    0b00000000,
    0b11111000,
    0b00000000,
    0b11111000,
    0b00000000,
    0b00000000,
    //0x3e
    0b00000000,
    0b01000000,
    0b00100000,
    0b00010000,
    0b00001000,
    0b00010000,
    0b00100000,
    0b01000000,
    //0x3f
    0b00000000,
    0b01110000,
    0b10001000,
    0b00001000,
    0b00010000,
    0b00100000,
    0b00000000,
    0b00100000,
    //0x40
    0b00000000,
    0b01110000,
    0b10001000,
    0b00001000,
    0b01101000,
    0b10101000,
    0b10101000,
    0b01110000,
    //0x41
    0b00000000,
    0b00100000,
    0b01010000,
    0b10001000,
    0b10001000,
    0b11111000,
    0b10001000,
    0b10001000,
    //0x42
    0b00000000,
    0b11110000,
    0b10001000,
    0b10001000,
    0b11110000,
    0b10001000,
    0b10001000,
    0b11110000,
    //0x43
    0b00000000,
    0b01110000,
    0b10001000,
    0b10000000,
    0b10000000,
    0b10000000,
    0b10001000,
    0b01110000,
    //0x44
    0b00000000,
    0b11100000,
    0b10010000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10010000,
    0b11100000,
    //0x45
    0b00000000,
    0b11111000,
    0b10000000,
    0b10000000,
    0b11110000,
    0b10000000,
    0b10000000,
    0b11111000,
    //0x46
    0b00000000,
    0b11111000,
    0b10000000,
    0b10000000,
    0b11110000,
    0b10000000,
    0b10000000,
    0b10000000,
    //0x47
    0b00000000,
    0b01110000,
    0b10001000,
    0b10000000,
    0b10111000,
    0b10001000,
    0b10001000,
    0b01111000,
    //0x48
    0b00000000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b11111000,
    0b10001000,
    0b10001000,
    0b10001000,
    //0x49
    0b00000000,
    0b01110000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b01110000,
    //0x4a
    0b00000000,
    0b00111000,
    0b00010000,
    0b00010000,
    0b00010000,
    0b00010000,
    0b10010000,
    0b01100000,
    //0x4b
    0b00000000,
    0b10001000,
    0b10010000,
    0b10100000,
    0b11000000,
    0b10100000,
    0b10010000,
    0b10001000,
    //0x4c
    0b00000000,
    0b10000000,
    0b10000000,
    0b10000000,
    0b10000000,
    0b10000000,
    0b10000000,
    0b11111000,
    //0x4d
    0b00000000,
    0b10001000,
    0b11011000,
    0b10101000,
    0b10101000,
    0b10001000,
    0b10001000,
    0b10001000,
    //0x4e
    0b00000000,
    0b10001000,
    0b10001000,
    0b11001000,
    0b10101000,
    0b10011000,
    0b10001000,
    0b10001000,
    //0x4f
    0b00000000,
    0b01110000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b01110000,
    //0x50
    0b00000000,
    0b11110000,
    0b10001000,
    0b10001000,
    0b11110000,
    0b10000000,
    0b10000000,
    0b10000000,
    //0x51
    0b00000000,
    0b01110000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10101000,
    0b10010000,
    0b01101000,
    //0x52
    0b00000000,
    0b11110000,
    0b10001000,
    0b10001000,
    0b11110000,
    0b10100000,
    0b10010000,
    0b10001000,
    //0x53
    0b00000000,
    0b01110000,
    0b10001000,
    0b10000000,
    0b01110000,
    0b00001000,
    0b10001000,
    0b01110000,
    //0x54
    0b00000000,
    0b11111000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b00100000,
    //0x55
    0b00000000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b01110000,
    //0x56
    0b00000000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b01010000,
    0b00100000,
    //0x57
    0b00000000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10101000,
    0b10101000,
    0b10101000,
    0b01010000,
    //0x58
    0b00000000,
    0b10001000,
    0b10001000,
    0b01010000,
    0b00100000,
    0b01010000,
    0b10001000,
    0b10001000,
    //0x59
    0b00000000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b01010000,
    0b00100000,
    0b00100000,
    0b00100000,
    //0x5a
    0b00000000,
    0b11111000,
    0b00001000,
    0b00010000,
    0b00100000,
    0b01000000,
    0b10000000,
    0b11111000,
    //0x5b
    0b00000000,
    0b01110000,
    0b01000000,
    0b01000000,
    0b01000000,
    0b01000000,
    0b01000000,
    0b01110000,
    //0x5c
    0b00000000,
    0b00000000,
    0b10000000,
    0b01000000,
    0b00100000,
    0b00010000,
    0b00001000,
    0b00000000,
    //0x5d
    0b00000000,
    0b01110000,
    0b00010000,
    0b00010000,
    0b00010000,
    0b00010000,
    0b00010000,
    0b01110000,
    //0x5e
    0b00000000,
    0b00100000,
    0b01010000,
    0b10001000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x5f
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b11111000,
    //0x60
    0b00000000,
    0b01000000,
    0b00100000,
    0b00010000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0x61
    0b00000000,
    0b00000000,
    0b00000000,
    0b01110000,
    0b00001000,
    0b01111000,
    0b10001000,
    0b01111000,
    //0x62
    0b00000000,
    0b10000000,
    0b10000000,
    0b10110000,
    0b11001000,
    0b10001000,
    0b10001000,
    0b11110000,
    //0x63
    0b00000000,
    0b00000000,
    0b00000000,
    0b01110000,
    0b10000000,
    0b10000000,
    0b10001000,
    0b01110000,
    //0x64
    0b00000000,
    0b00001000,
    0b00001000,
    0b01101000,
    0b10011000,
    0b10001000,
    0b10001000,
    0b01111000,
    //0x65
    0b00000000,
    0b00000000,
    0b00000000,
    0b01110000,
    0b10001000,
    0b11111000,
    0b10000000,
    0b01110000,
    //0x66
    0b00000000,
    0b00110000,
    0b01001000,
    0b01000000,
    0b11100000,
    0b01000000,
    0b01000000,
    0b01000000,
    //0x67
    0b00000000,
    0b00000000,
    0b00000000,
    0b01111000,
    0b10001000,
    0b01111000,
    0b00001000,
    0b01110000,
    //0x68
    0b00000000,
    0b10000000,
    0b10000000,
    0b10110000,
    0b11001000,
    0b10001000,
    0b10001000,
    0b10001000,
    //0x69
    0b00000000,
    0b00100000,
    0b00000000,
    0b00100000,
    0b01100000,
    0b00100000,
    0b00100000,
    0b01110000,
    //0x6a
    0b00000000,
    0b00010000,
    0b00000000,
    0b00110000,
    0b00010000,
    0b00010000,
    0b10010000,
    0b01100000,
    //0x6b
    0b00000000,
    0b10000000,
    0b10000000,
    0b10010000,
    0b10100000,
    0b11000000,
    0b10100000,
    0b10010000,
    //0x6c
    0b00000000,
    0b01100000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b01110000,
    //0x6d
    0b00000000,
    0b00000000,
    0b00000000,
    0b11010000,
    0b10101000,
    0b10101000,
    0b10101000,
    0b10101000,
    //0x6e
    0b00000000,
    0b00000000,
    0b00000000,
    0b10110000,
    0b11001000,
    0b10001000,
    0b10001000,
    0b10001000,
    //0x6f
    0b00000000,
    0b00000000,
    0b00000000,
    0b01110000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b01110000,
    //0x70
    0b00000000,
    0b00000000,
    0b00000000,
    0b11110000,
    0b10001000,
    0b11110000,
    0b10000000,
    0b10000000,
    //0x71
    0b00000000,
    0b00000000,
    0b00000000,
    0b01101000,
    0b10011000,
    0b01111000,
    0b00001000,
    0b00001000,
    //0x72
    0b00000000,
    0b00000000,
    0b00000000,
    0b10110000,
    0b11001000,
    0b10000000,
    0b10000000,
    0b10000000,
    //0x73
    0b00000000,
    0b00000000,
    0b00000000,
    0b01110000,
    0b10000000,
    0b01110000,
    0b00001000,
    0b11110000,
    //0x74
    0b00000000,
    0b01000000,
    0b01000000,
    0b11100000,
    0b01000000,
    0b01000000,
    0b01001000,
    0b00110000,
    //0x75
    0b00000000,
    0b00000000,
    0b00000000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10011000,
    0b01101000,
    //0x76
    0b00000000,
    0b00000000,
    0b00000000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b01010000,
    0b00100000,
    //0x77
    0b00000000,
    0b00000000,
    0b00000000,
    0b10001000,
    0b10001000,
    0b10101000,
    0b10101000,
    0b01010000,
    //0x78
    0b00000000,
    0b00000000,
    0b00000000,
    0b10001000,
    0b01010000,
    0b00100000,
    0b01010000,
    0b10001000,
    //0x79
    0b00000000,
    0b00000000,
    0b00000000,
    0b10001000,
    0b10001000,
    0b01111000,
    0b00001000,
    0b01110000,
    //0x7a
    0b00000000,
    0b00000000,
    0b00000000,
    0b11111000,
    0b00010000,
    0b00100000,
    0b01000000,
    0b11111000,
    //0x7b
    0b00000000,
    0b00010000,
    0b00100000,
    0b00100000,
    0b01000000,
    0b00100000,
    0b00100000,
    0b00010000,
    //0x7c
    0b00000000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b00100000,
    //0x7d
    0b00000000,
    0b01000000,
    0b00100000,
    0b00100000,
    0b00010000,
    0b00100000,
    0b00100000,
    0b01000000,
    //0x7e
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b01101000,
    0b10010000,
    0b00000000,
    0b00000000,
    //0x7f
    0b00000000,
    0b00100000,
    0b01010000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b11111000,
    0b00000000,
    //0x80
    0b00000000,
    0b11111000,
    0b10001000,
    0b10000000,
    0b11110000,
    0b10001000,
    0b10001000,
    0b11110000,
    //0x81
    0b01111000,
    0b00101000,
    0b00101000,
    0b01001000,
    0b10001000,
    0b11111000,
    0b10001000,
    0b10001000,
    //0x82
    0b00000000,
    0b10101000,
    0b10101000,
    0b10101000,
    0b01110000,
    0b10101000,
    0b10101000,
    0b10101000,
    //0x83
    0b00000000,
    0b11110000,
    0b00001000,
    0b00001000,
    0b00110000,
    0b00001000,
    0b00001000,
    0b11110000,
    //0x84
    0b00000000,
    0b10001000,
    0b10001000,
    0b10011000,
    0b10101000,
    0b11001000,
    0b10001000,
    0b10001000,
    //0x85
    0b01010000,
    0b00100000,
    0b10001000,
    0b10001000,
    0b10011000,
    0b10101000,
    0b11001000,
    0b10001000,
    //0x86
    0b00000000,
    0b01111000,
    0b00101000,
    0b00101000,
    0b00101000,
    0b00101000,
    0b10101000,
    0b01001000,
    //0x87
    0b00000000,
    0b11111000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10001000,
    //0x88
    0b00000000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b01010000,
    0b00100000,
    0b01000000,
    0b10000000,
    //0x89
    0b00000000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b11111000,
    0b00001000,
    //0x8a
    0b00000000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b01111000,
    0b00001000,
    0b00001000,
    0b00001000,
    //0x8b
    0b00000000,
    0b00000000,
    0b10101000,
    0b10101000,
    0b10101000,
    0b10101000,
    0b10101000,
    0b11111000,
    //0x8c
    0b00000000,
    0b10101000,
    0b10101000,
    0b10101000,
    0b10101000,
    0b10101000,
    0b11111000,
    0b00001000,
    //0x8d
    0b00000000,
    0b11000000,
    0b01000000,
    0b01000000,
    0b01110000,
    0b01001000,
    0b01001000,
    0b01110000,
    //0x8e
    0b00000000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b11001000,
    0b10101000,
    0b10101000,
    0b11001000,
    //0x8f
    0b00000000,
    0b01110000,
    0b10001000,
    0b00101000,
    0b01011000,
    0b00001000,
    0b10001000,
    0b01110000,
    //0x90
    0b00000000,
    0b00000000,
    0b00000000,
    0b01001000,
    0b10101000,
    0b10010000,
    0b10010000,
    0b01101000,
    //0x91
    0b00000000,
    0b00100000,
    0b00110000,
    0b00101000,
    0b00101000,
    0b00100000,
    0b11100000,
    0b11100000,
    //0x92
    0b00000000,
    0b11111000,
    0b10001000,
    0b10000000,
    0b10000000,
    0b10000000,
    0b10000000,
    0b10000000,
    //0x93
    0b00000000,
    0b00000000,
    0b00000000,
    0b11111000,
    0b01010000,
    0b01010000,
    0b01010000,
    0b10011000,
    //0x94
    0b00000000,
    0b11111000,
    0b10000000,
    0b01000000,
    0b00100000,
    0b01000000,
    0b10000000,
    0b11111000,
    //0x95
    0b00000000,
    0b00000000,
    0b00000000,
    0b01111000,
    0b10010000,
    0b10010000,
    0b10010000,
    0b01100000,
    //0x96
    0b00110000,
    0b00101000,
    0b00111000,
    0b00101000,
    0b00101000,
    0b11101000,
    0b11011000,
    0b00011000,
    //0x97
    0b00000000,
    0b00000000,
    0b00001000,
    0b01110000,
    0b10100000,
    0b00100000,
    0b00100000,
    0b00010000,
    //0x98
    0b00000000,
    0b00100000,
    0b01110000,
    0b01110000,
    0b01110000,
    0b11111000,
    0b00100000,
    0b00000000,
    //0x99
    0b00000000,
    0b01110000,
    0b10001000,
    0b10001000,
    0b11111000,
    0b10001000,
    0b10001000,
    0b01110000,
    //0x9a
    0b00000000,
    0b00000000,
    0b01110000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b01010000,
    0b11011000,
    //0x9b
    0b00000000,
    0b00110000,
    0b01001000,
    0b00100000,
    0b01010000,
    0b10001000,
    0b10001000,
    0b01110000,
    //0x9c
    0b00000000,
    0b00000000,
    0b00000000,
    0b01011000,
    0b10101000,
    0b11010000,
    0b00000000,
    0b00000000,
    //0x9d
    0b00000000,
    0b00000000,
    0b01010000,
    0b11111000,
    0b11111000,
    0b11111000,
    0b01110000,
    0b00100000,
    //0x9e
    0b00000000,
    0b00000000,
    0b00000000,
    0b01110000,
    0b10000000,
    0b01100000,
    0b10001000,
    0b01110000,
    //0x9f
    0b00000000,
    0b01110000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10001000,
    //0xa0
    0b00000000,
    0b11011000,
    0b11011000,
    0b11011000,
    0b11011000,
    0b11011000,
    0b11011000,
    0b11011000,
    //0xa1
    0b00000000,
    0b00100000,
    0b00000000,
    0b00000000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b00100000,
    //0xa2
    0b00000000,
    0b00100000,
    0b01110000,
    0b10100000,
    0b10100000,
    0b10101000,
    0b01110000,
    0b00100000,
    //0xa3
    0b00000000,
    0b00110000,
    0b01000000,
    0b01000000,
    0b11100000,
    0b01000000,
    0b01001000,
    0b10110000,
    //0xa4
    0b00000000,
    0b00000000,
    0b10001000,
    0b01110000,
    0b01010000,
    0b01110000,
    0b10001000,
    0b00000000,
    //0xa5
    0b00000000,
    0b10001000,
    0b01010000,
    0b11111000,
    0b00100000,
    0b11111000,
    0b00100000,
    0b00100000,
    //0xa6
    0b00000000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b00000000,
    0b00100000,
    0b00100000,
    0b00100000,
    //0xa7
    0b00000000,
    0b00110000,
    0b01001000,
    0b00100000,
    0b01010000,
    0b00100000,
    0b10010000,
    0b01100000,
    //0xa8
    0b00000000,
    0b00010000,
    0b00101000,
    0b00100000,
    0b11111000,
    0b00100000,
    0b10100000,
    0b01000000,
    //0xa9
    0b00000000,
    0b11111000,
    0b10001000,
    0b10101000,
    0b10111000,
    0b10101000,
    0b10001000,
    0b11111000,
    //0xaa
    0b00000000,
    0b01110000,
    0b00001000,
    0b01111000,
    0b10001000,
    0b01111000,
    0b00000000,
    0b11111000,
    //0xab
    0b00000000,
    0b00000000,
    0b00101000,
    0b01010000,
    0b10100000,
    0b01010000,
    0b00101000,
    0b00000000,
    //0xac
    0b00000000,
    0b10010000,
    0b10101000,
    0b10101000,
    0b11101000,
    0b10101000,
    0b10101000,
    0b10010000,
    //0xad
    0b00000000,
    0b01111000,
    0b10001000,
    0b10001000,
    0b01111000,
    0b00101000,
    0b01001000,
    0b10001000,
    //0xae
    0b00000000,
    0b11111000,
    0b10001000,
    0b10101000,
    0b10001000,
    0b10011000,
    0b10101000,
    0b11111000,
    //0xaf
    0b00000000,
    0b00100000,
    0b01000000,
    0b01100000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0xb0
    0b01100000,
    0b10010000,
    0b10010000,
    0b10010000,
    0b01100000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0xb1
    0b00000000,
    0b00100000,
    0b00100000,
    0b11111000,
    0b00100000,
    0b00100000,
    0b00000000,
    0b11111000,
    //0xb2
    0b01100000,
    0b10010000,
    0b00100000,
    0b01000000,
    0b11110000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0xb3
    0b11100000,
    0b00010000,
    0b01100000,
    0b00010000,
    0b11100000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0xb4
    0b11100000,
    0b10010000,
    0b11100000,
    0b10000000,
    0b10010000,
    0b10111000,
    0b10010000,
    0b00011000,
    //0xb5
    0b00000000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10011000,
    0b11101000,
    0b10000000,
    0b10000000,
    //0xb6
    0b00000000,
    0b01111000,
    0b10011000,
    0b10011000,
    0b01111000,
    0b00011000,
    0b00011000,
    0b00011000,
    //0xb7
    0b00000000,
    0b00000000,
    0b00000000,
    0b00000000,
    0b01100000,
    0b01100000,
    0b00000000,
    0b00000000,
    //0xb8
    0b00000000,
    0b00000000,
    0b00000000,
    0b01010000,
    0b10001000,
    0b10101000,
    0b10101000,
    0b01010000,
    //0xb9
    0b01000000,
    0b11000000,
    0b01000000,
    0b01000000,
    0b11100000,
    0b00000000,
    0b00000000,
    0b00000000,
    //0xba
    0b00000000,
    0b01110000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b01110000,
    0b00000000,
    0b11111000,
    //0xbb
    0b00000000,
    0b00000000,
    0b10100000,
    0b01010000,
    0b00101000,
    0b01010000,
    0b10100000,
    0b00000000,
    //0xbc
    0b10001000,
    0b10010000,
    0b10100000,
    0b01010000,
    0b10110000,
    0b01010000,
    0b01111000,
    0b00010000,
    //0xbd
    0b10001000,
    0b10010000,
    0b10100000,
    0b01010000,
    0b10101000,
    0b00001000,
    0b00010000,
    0b00111000,
    //0xbe
    0b11000000,
    0b01000000,
    0b11000000,
    0b01001000,
    0b11011000,
    0b00101000,
    0b00111000,
    0b00001000,
    //0xbf
    0b00000000,
    0b00100000,
    0b00000000,
    0b00100000,
    0b01000000,
    0b10000000,
    0b10001000,
    0b01110000,
    //0xc0
    0b01000000,
    0b00100000,
    0b00100000,
    0b01010000,
    0b10001000,
    0b11111000,
    0b10001000,
    0b10001000,
    //0xc1
    0b00010000,
    0b00100000,
    0b00100000,
    0b01010000,
    0b10001000,
    0b11111000,
    0b10001000,
    0b10001000,
    //0xc2
    0b00100000,
    0b01010000,
    0b00000000,
    0b01110000,
    0b10001000,
    0b11111000,
    0b10001000,
    0b10001000,
    //0xc3
    0b01101000,
    0b10010000,
    0b00000000,
    0b01110000,
    0b10001000,
    0b11111000,
    0b10001000,
    0b10001000,
    //0xc4
    0b01010000,
    0b00000000,
    0b00100000,
    0b01010000,
    0b10001000,
    0b11111000,
    0b10001000,
    0b10001000,
    //0xc5
    0b00100000,
    0b01010000,
    0b00100000,
    0b01110000,
    0b10001000,
    0b11111000,
    0b10001000,
    0b10001000,
    //0xc6
    0b00000000,
    0b00111000,
    0b01100000,
    0b10100000,
    0b10111000,
    0b11100000,
    0b10100000,
    0b10111000,
    //0xc7
    0b01110000,
    0b10001000,
    0b10000000,
    0b10000000,
    0b10001000,
    0b01110000,
    0b00010000,
    0b00110000,
    //0xc8
    0b01000000,
    0b00100000,
    0b00000000,
    0b11111000,
    0b10000000,
    0b11110000,
    0b10000000,
    0b11111000,
    //0xc9
    0b00010000,
    0b00100000,
    0b00000000,
    0b11111000,
    0b10000000,
    0b11110000,
    0b10000000,
    0b11111000,
    //0xca
    0b00100000,
    0b01010000,
    0b00000000,
    0b11111000,
    0b10000000,
    0b11110000,
    0b10000000,
    0b11111000,
    //0xcb
    0b00000000,
    0b01010000,
    0b00000000,
    0b11111000,
    0b10000000,
    0b11110000,
    0b10000000,
    0b11111000,
    //0xcc
    0b01000000,
    0b00100000,
    0b00000000,
    0b01110000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b01110000,
    //0xcd
    0b00010000,
    0b00100000,
    0b00000000,
    0b01110000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b01110000,
    //0xce
    0b00100000,
    0b01010000,
    0b00000000,
    0b01110000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b01110000,
    //0xcf
    0b00000000,
    0b01010000,
    0b00000000,
    0b01110000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b01110000,
    //0xd0
    0b00000000,
    0b01110000,
    0b01001000,
    0b01001000,
    0b11101000,
    0b01001000,
    0b01001000,
    0b01110000,
    //0xd1
    0b01101000,
    0b10010000,
    0b00000000,
    0b10001000,
    0b11001000,
    0b10101000,
    0b10011000,
    0b10001000,
    //0xd2
    0b01000000,
    0b00100000,
    0b01110000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b01110000,
    //0xd3
    0b00010000,
    0b00100000,
    0b01110000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b01110000,
    //0xd4
    0b00100000,
    0b01010000,
    0b00000000,
    0b01110000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b01110000,
    //0xd5
    0b01101000,
    0b10010000,
    0b00000000,
    0b01110000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b01110000,
    //0xd6
    0b01010000,
    0b00000000,
    0b01110000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b01110000,
    //0xd7
    0b00000000,
    0b00000000,
    0b10001000,
    0b01010000,
    0b00100000,
    0b01010000,
    0b10001000,
    0b00000000,
    //0xd8
    0b00000000,
    0b01110000,
    0b00100000,
    0b01110000,
    0b10101000,
    0b01110000,
    0b00100000,
    0b01110000,
    //0xd9
    0b01000000,
    0b00100000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b01110000,
    //0xda
    0b00010000,
    0b00100000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b01110000,
    //0xdb
    0b00100000,
    0b01010000,
    0b00000000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b01110000,
    //0xdc
    0b01010000,
    0b00000000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b01110000,
    //0xdd
    0b00010000,
    0b00100000,
    0b10001000,
    0b01010000,
    0b00100000,
    0b00100000,
    0b00100000,
    0b00100000,
    //0xde
    0b11000000,
    0b01000000,
    0b01110000,
    0b01001000,
    0b01001000,
    0b01110000,
    0b01000000,
    0b11000000,
    //0xdf
    0b00000000,
    0b00110000,
    0b01001000,
    0b01001000,
    0b01110000,
    0b01001000,
    0b01001000,
    0b10110000,
    //0xe0
    0b01000000,
    0b00100000,
    0b00000000,
    0b01110000,
    0b00001000,
    0b01111000,
    0b10001000,
    0b01111000,
    //0xe1
    0b00010000,
    0b00100000,
    0b00000000,
    0b01110000,
    0b00001000,
    0b01111000,
    0b10001000,
    0b01111000,
    //0xe2
    0b00100000,
    0b01010000,
    0b00000000,
    0b01110000,
    0b00001000,
    0b01111000,
    0b10001000,
    0b01111000,
    //0xe3
    0b01101000,
    0b10010000,
    0b00000000,
    0b01110000,
    0b00001000,
    0b01111000,
    0b10001000,
    0b01111000,
    //0xe4
    0b00000000,
    0b01010000,
    0b00000000,
    0b01110000,
    0b00001000,
    0b01111000,
    0b10001000,
    0b01111000,
    //0xe5
    0b00100000,
    0b01010000,
    0b00100000,
    0b01110000,
    0b00001000,
    0b01111000,
    0b10001000,
    0b01111000,
    //0xe6
    0b00000000,
    0b00000000,
    0b11010000,
    0b00101000,
    0b01111000,
    0b10100000,
    0b10101000,
    0b01010000,
    //0xe7
    0b00000000,
    0b00000000,
    0b01110000,
    0b10000000,
    0b10001000,
    0b01110000,
    0b00100000,
    0b01100000,
    //0xe8
    0b01000000,
    0b00100000,
    0b00000000,
    0b01110000,
    0b10001000,
    0b11111000,
    0b10000000,
    0b01110000,
    //0xe9
    0b00010000,
    0b00100000,
    0b00000000,
    0b01110000,
    0b10001000,
    0b11111000,
    0b10000000,
    0b01110000,
    //0xea
    0b00100000,
    0b01010000,
    0b00000000,
    0b01110000,
    0b10001000,
    0b11111000,
    0b10000000,
    0b01110000,
    //0xeb
    0b00000000,
    0b01010000,
    0b00000000,
    0b01110000,
    0b10001000,
    0b11111000,
    0b10000000,
    0b01110000,
    //0xec
    0b01000000,
    0b00100000,
    0b00000000,
    0b00100000,
    0b01100000,
    0b00100000,
    0b00100000,
    0b01110000,
    //0xed
    0b00010000,
    0b00100000,
    0b00000000,
    0b00100000,
    0b01100000,
    0b00100000,
    0b00100000,
    0b01110000,
    //0xee
    0b00100000,
    0b01010000,
    0b00000000,
    0b00100000,
    0b01100000,
    0b00100000,
    0b00100000,
    0b01110000,
    //0xef
    0b00000000,
    0b01010000,
    0b00000000,
    0b00100000,
    0b01100000,
    0b00100000,
    0b00100000,
    0b01110000,
    //0xf0
    0b00000000,
    0b10100000,
    0b01000000,
    0b10100000,
    0b00010000,
    0b01111000,
    0b10001000,
    0b01110000,
    //0xf1
    0b01101000,
    0b10010000,
    0b00000000,
    0b10110000,
    0b11001000,
    0b10001000,
    0b10001000,
    0b10001000,
    //0xf2
    0b01000000,
    0b00100000,
    0b00000000,
    0b01110000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b01110000,
    //0xf3
    0b00010000,
    0b00100000,
    0b00000000,
    0b01110000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b01110000,
    //0xf4
    0b00000000,
    0b00100000,
    0b01010000,
    0b00000000,
    0b01110000,
    0b10001000,
    0b10001000,
    0b01110000,
    //0xf5
    0b00000000,
    0b01101000,
    0b10010000,
    0b00000000,
    0b01110000,
    0b10001000,
    0b10001000,
    0b01110000,
    //0xf6
    0b00000000,
    0b01010000,
    0b00000000,
    0b01110000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b01110000,
    //0xf7
    0b00000000,
    0b00000000,
    0b00100000,
    0b00000000,
    0b11111000,
    0b00000000,
    0b00100000,
    0b00000000,
    //0xf8
    0b00000000,
    0b00010000,
    0b00100000,
    0b01110000,
    0b10101000,
    0b01110000,
    0b00100000,
    0b01000000,
    //0xf9
    0b01000000,
    0b00100000,
    0b00000000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10011000,
    0b01101000,
    //0xfa
    0b00010000,
    0b00100000,
    0b00000000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10011000,
    0b01101000,
    //0xfb
    0b00100000,
    0b01010000,
    0b00000000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10011000,
    0b01101000,
    //0xfc
    0b00000000,
    0b01010000,
    0b00000000,
    0b10001000,
    0b10001000,
    0b10001000,
    0b10011000,
    0b01101000,
    //0xfd
    0b00000000,
    0b00010000,
    0b00100000,
    0b10001000,
    0b10001000,
    0b01111000,
    0b00001000,
    0b01110000,
    //0xfe
    0b00000000,
    0b01100000,
    0b00100000,
    0b00110000,
    0b00101000,
    0b00110000,
    0b00100000,
    0b01110000,
    //0xff
    0b00000000,
    0b01010000,
    0b00000000,
    0b10001000,
    0b10001000,
    0b01111000,
    0b00001000,
    0b01110000,
  };
*/
  //  perl ../misc/itob.pl PPI.java SMK6X8_FONT
  public static final byte[] SMK6X8_FONT = "\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0    \0\0 \0PPP\0\0\0\0\0PP\370P\370PP\0 x\240p(\360 \0\300\310\20 @\230\30\0`\220\240@\250\220h\0` @\0\0\0\0\0\20 @@@ \20\0@ \20\20\20 @\0\0 \250p\250 \0\0\0  \370  \0\0\0\0\0\0` @\0\0\0\0\370\0\0\0\0\0\0\0\0\0``\0\0\b\20 @\200\0\0p\210\230\250\310\210p\0 `    p\0p\210\b\20 @\370\0\370\20 \20\b\210p\0\0200P\220\370\20\20\0\370\200\360\b\b\210p\0000@\200\360\210\210p\0\370\b\20 @@@\0p\210\210p\210\210p\0p\210\210x\b\20`\0\0``\0``\0\0\0``\0` @\0\20 @\200@ \20\0\0\0\370\0\370\0\0\0@ \20\b\20 @\0p\210\b\20 \0 \0p\210\bh\250\250p\0p\210\210\210\370\210\210\0\360\210\210\360\210\210\360\0p\210\200\200\200\210p\0\340\220\210\210\210\220\340\0\370\200\200\360\200\200\370\0\370\200\200\360\200\200\200\0p\210\200\270\210\210x\0\210\210\210\370\210\210\210\0p     p\08\20\20\20\20\220`\0\210\220\240\300\240\220\210\0\200\200\200\200\200\200\370\0\210\330\250\250\210\210\210\0\210\210\310\250\230\210\210\0p\210\210\210\210\210p\0\360\210\210\360\200\200\200\0p\210\210\210\250\220h\0\360\210\210\360\240\220\210\0x\200\200p\b\b\360\0\370      \0\210\210\210\210\210\210p\0\210\210\210\210\210P \0\210\210\210\250\250\250P\0\210\210P P\210\210\0\210\210\210P   \0\370\b\20 @\200\370\0p@@@@@p\0\210P\370 \370  \0p\20\20\20\20\20p\0 P\210\0\0\0\0\0\0\0\0\0\0\0\370\0@ \20\0\0\0\0\0\0\0p\bx\210x\0\200\200\260\310\210\210\360\0\0\0p\200\200\210p\0\b\bh\230\210\210x\0\0\0p\210\370\200p\0000H@\340@@@\0\0x\210\210x\bp\0\200\200\260\310\210\210\210\0 \0`   p\0\20\0000\20\20\220`\0\200\200\220\240\300\240\220\0`     p\0\0\0\320\250\250\210\210\0\0\0\260\310\210\210\210\0\0\0p\210\210\210p\0\0\0\360\210\360\200\200\0\0\0h\230x\b\b\0\0\0\260\310\200\200\200\0\0\0p\200p\b\360\0@@\340@@H0\0\0\0\210\210\210\230h\0\0\0\210\210\210P \0\0\0\210\210\250\250P\0\0\0\210P P\210\0\0\0\210\210x\bp\0\0\0\370\20 @\370\0\20  @  \20\0       \0@  \20  @\0\0 \20\370\20 \0\0\0 @\370@ \0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\340\240\340\08   \0\0\0\0\0\0\0   \340\0\0\0\0\0\200@ \0\0\0\0``\0\0\0\0\370\b\370\b\20 \0\0\0\370\b0 @\0\0\0\20 `\240 \0\0\0 \370\210\b0\0\0\0\0\370  \370\0\0\0\20\3700P\220\0\0\0@\370HP@\0\0\0\0p\20\20\370\0\0\0\360\20\360\20\360\0\0\0\0\250\250\b0\0\0\0\0\370\0\0\0\0\370\b(0  @\0\b\20 `\240  \0 \370\210\210\b\20 \0\0\370    \370\0\20\370\0200P\220\20\0@\370HHHH\220\0 \370 \370   \0\0xH\210\b\20`\0@x\220\20\20\20 \0\0\370\b\b\b\b\370\0P\370PP\20 @\0\0\300\b\310\b\20\340\0\0\370\b\20 P\210\0@\370HP@@8\0\0\210\210H\b\20`\0\0xH\250\30\20`\0\20\340 \370  @\0\0\250\250\250\b\20 \0p\0\370   @\0@@@`P@@\0  \370  @\200\0\0p\0\0\0\0\370\0\0\370\bP P\200\0 \370\20 p\250 \0\20\20\20\20\20 @\0\0 \20\210\210\210\210\0\200\200\370\200\200\200x\0\0\370\b\b\b\20`\0\0@\240\20\b\b\0\0 \370  \250\250 \0\0\370\b\bP \20\0\0p\0p\0p\b\0\0 @\200\210\370\b\0\0\b\bP P\200\0\0\370@\370@@8\0@@\370HP@@\0\0p\20\20\20\20\370\0\0\370\b\370\b\b\370\0p\0\370\b\b\20 \0\220\220\220\220\20 @\0\0 \240\240\250\250\260\0\0\200\200\210\220\240\300\0\0\370\210\210\210\210\370\0\0\370\210\210\b\20 \0\0\300\0\b\b\20\340\0 \220@\0\0\0\0\0\340\240\340\0\0\0\0\0\0\0H\250\220\220h\0P\0p\bx\210x\0\0\0p\210\360\210\360\200\0\0p\200`\210p\0\0\0\210\210\210\230\350\200\0\0x\240\220\210p\0\0\0000H\210\210\360\200\0\0x\210\210\210x\b\0\08  \240@\0\0\20\320\20\0\0\0\0\20\0000\20\20\20\20\20\0\240@\240\0\0\0\0\0 p\240\250p \0@@\340@\340@x\0p\0\260\310\210\210\210\0P\0p\210\210\210p\0\0\0\260\310\210\210\360\200\0`\230\210\210x\b\b\0p\210\370\210\210p\0\0\0\0X\250\320\0\0\0\0p\210\210P\330\0P\0\210\210\210\210\230h\370\200@ @\200\370\0\0\0\370PPP\230\0\370\0\210P P\210\0\0\0\210\210\210\210x\b\0\b\360 \370  \0\0\0\370@xH\210\0\0\0\370\250\370\210\210\0\0 \0\370\0 \0\0\0\0\0\0\0\0\0\0\370\370\370\370\370\370\370\370\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0@`pxp`@\0\0200p\360p0\20\0H\220\330\0\0\0\0\0\330H\220\0\0\0\0\0 p\370\0 p\370\0\370p \0\370p \0\0p\370\370\370p\0\0\b\b(H\370@ \0 p\250    \0    \250p \0\0 \20\370\20 \0\0\0 @\370@ \0\0\20 @ \20\0\370\0@ \20 @\0\370\0\0  pp\370\0\0\0\370pp  \0\0\0\0\0\0\0\0\0\0    \0\0 \0PPP\0\0\0\0\0PP\370P\370PP\0 x\240p(\360 \0\300\310\20 @\230\30\0`\220\240@\250\220h\0` @\0\0\0\0\0\20 @@@ \20\0@ \20\20\20 @\0\0 \250p\250 \0\0\0  \370  \0\0\0\0\0\0` @\0\0\0\0\370\0\0\0\0\0\0\0\0\0``\0\0\b\20 @\200\0\0p\210\230\250\310\210p\0 `    p\0p\210\b\20 @\370\0\370\20 \20\b\210p\0\0200P\220\370\20\20\0\370\200\360\b\b\210p\0000@\200\360\210\210p\0\370\210\b\20   \0p\210\210p\210\210p\0p\210\210x\b\20`\0\0``\0``\0\0\0``\0` @\0\20 @\200@ \20\0\0\0\370\0\370\0\0\0@ \20\b\20 @\0p\210\b\20 \0 \0p\210\bh\250\250p\0 P\210\210\370\210\210\0\360\210\210\360\210\210\360\0p\210\200\200\200\210p\0\340\220\210\210\210\220\340\0\370\200\200\360\200\200\370\0\370\200\200\360\200\200\200\0p\210\200\270\210\210x\0\210\210\210\370\210\210\210\0p     p\08\20\20\20\20\220`\0\210\220\240\300\240\220\210\0\200\200\200\200\200\200\370\0\210\330\250\250\210\210\210\0\210\210\310\250\230\210\210\0p\210\210\210\210\210p\0\360\210\210\360\200\200\200\0p\210\210\210\250\220h\0\360\210\210\360\240\220\210\0p\210\200p\b\210p\0\370      \0\210\210\210\210\210\210p\0\210\210\210\210\210P \0\210\210\210\250\250\250P\0\210\210P P\210\210\0\210\210\210P   \0\370\b\20 @\200\370\0p@@@@@p\0\0\200@ \20\b\0\0p\20\20\20\20\20p\0 P\210\0\0\0\0\0\0\0\0\0\0\0\370\0@ \20\0\0\0\0\0\0\0p\bx\210x\0\200\200\260\310\210\210\360\0\0\0p\200\200\210p\0\b\bh\230\210\210x\0\0\0p\210\370\200p\0000H@\340@@@\0\0\0x\210x\bp\0\200\200\260\310\210\210\210\0 \0 `  p\0\20\0000\20\20\220`\0\200\200\220\240\300\240\220\0`     p\0\0\0\320\250\250\250\250\0\0\0\260\310\210\210\210\0\0\0p\210\210\210p\0\0\0\360\210\360\200\200\0\0\0h\230x\b\b\0\0\0\260\310\200\200\200\0\0\0p\200p\b\360\0@@\340@@H0\0\0\0\210\210\210\230h\0\0\0\210\210\210P \0\0\0\210\210\250\250P\0\0\0\210P P\210\0\0\0\210\210x\bp\0\0\0\370\20 @\370\0\20  @  \20\0       \0@  \20  @\0\0\0\0h\220\0\0\0 P\210\210\210\370\0\0\370\210\200\360\210\210\360x((H\210\370\210\210\0\250\250\250p\250\250\250\0\360\b\b0\b\b\360\0\210\210\230\250\310\210\210P \210\210\230\250\310\210\0x((((\250H\0\370\210\210\210\210\210\210\0\210\210\210P @\200\0\210\210\210\210\210\370\b\0\210\210\210x\b\b\b\0\0\250\250\250\250\250\370\0\250\250\250\250\250\370\b\0\300@@pHHp\0\210\210\210\310\250\250\310\0p\210(X\b\210p\0\0\0H\250\220\220h\0 0(( \340\340\0\370\210\200\200\200\200\200\0\0\0\370PPP\230\0\370\200@ @\200\370\0\0\0x\220\220\220`0(8((\350\330\30\0\0\bp\240  \20\0 ppp\370 \0\0p\210\210\370\210\210p\0\0p\210\210\210P\330\0000H P\210\210p\0\0\0X\250\320\0\0\0\0P\370\370\370p \0\0\0p\200`\210p\0p\210\210\210\210\210\210\0\330\330\330\330\330\330\330\0 \0\0    \0 p\240\240\250p \0000@@\340@H\260\0\0\210pPp\210\0\0\210P\370 \370  \0   \0   \0000H P \220`\0\20( \370 \240@\0\370\210\250\270\250\210\370\0p\bx\210x\0\370\0\0(P\240P(\0\0\220\250\250\350\250\250\220\0x\210\210x(H\210\0\370\210\250\210\230\250\370\0 @`\0\0\0\0`\220\220\220`\0\0\0\0  \370  \0\370`\220 @\360\0\0\0\340\20`\20\340\0\0\0\340\220\340\200\220\270\220\30\0\210\210\210\230\350\200\200\0x\230\230x\30\30\30\0\0\0\0``\0\0\0\0\0P\210\250\250P@\300@@\340\0\0\0\0p\210\210\210p\0\370\0\0\240P(P\240\0\210\220\240P\260Px\20\210\220\240P\250\b\208\300@\300H\330(8\b\0 \0 @\200\210p@  P\210\370\210\210\20  P\210\370\210\210 P\0p\210\370\210\210h\220\0p\210\370\210\210P\0 P\210\370\210\210 P p\210\370\210\210\08`\240\270\340\240\270p\210\200\200\210p\0200@ \0\370\200\360\200\370\20 \0\370\200\360\200\370 P\0\370\200\360\200\370\0P\0\370\200\360\200\370@ \0p   p\20 \0p   p P\0p   p\0P\0p   p\0pHH\350HHph\220\0\210\310\250\230\210@ p\210\210\210\210p\20 p\210\210\210\210p P\0p\210\210\210ph\220\0p\210\210\210pP\0p\210\210\210\210p\0\0\210P P\210\0\0p p\250p p@ \210\210\210\210\210p\20 \210\210\210\210\210p P\0\210\210\210\210pP\0\210\210\210\210\210p\20 \210P    \300@pHHp@\300\0000HHpHH\260@ \0p\bx\210x\20 \0p\bx\210x P\0p\bx\210xh\220\0p\bx\210x\0P\0p\bx\210x P p\bx\210x\0\0\320(x\240\250P\0\0p\200\210p `@ \0p\210\370\200p\20 \0p\210\370\200p P\0p\210\370\200p\0P\0p\210\370\200p@ \0 `  p\20 \0 `  p P\0 `  p\0P\0 `  p\0\240@\240\20x\210ph\220\0\260\310\210\210\210@ \0p\210\210\210p\20 \0p\210\210\210p\0 P\0p\210\210p\0h\220\0p\210\210p\0P\0p\210\210\210p\0\0 \0\370\0 \0\0\20 p\250p @@ \0\210\210\210\230h\20 \0\210\210\210\230h P\0\210\210\210\230h\0P\0\210\210\210\230h\0\20 \210\210x\bp\0` 0(0 p\0P\0\210\210x\bp".getBytes (XEiJ.ISO_8859_1);

}  //class Shiromadokun
