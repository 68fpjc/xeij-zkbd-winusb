//========================================================================================
//  Indicator.java
//    en:Indicator -- Displays MPU, frequency and load factor
//    ja:インジケータ -- MPUと周波数と負荷率を表示します
//  Copyright (C) 2003-2026 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.awt.*;
import java.awt.image.*;
import java.util.*;
import javax.swing.*;

public class Indicator {

  //  |          111111111122222|
  //  |0123456789012345678901234|
  //  |MC68EC030 999.9MHz 100.0%|
  public static final int IND_MPU_X = 0;
  public static final int IND_CLOCK_X = 10;
  public static final int IND_RATIO_X = 19;
  //public static final int IND_TIMER_X = 26;
  public static final int IND_POWER_X = 26;
  public static final int IND_COLS = 27;
  public static final int IND_FONT_WIDTH = 6;
  public static final int IND_FONT_HEIGHT = 8;
  public static final int IND_PADDING_LEFT = 6;
  public static final int IND_PADDING_RIGHT = 6;
  public static final int IND_PADDING_TOP = 3;
  public static final int IND_PADDING_BOTTOM = 2;
  public static final int IND_WIDTH = IND_PADDING_LEFT + IND_FONT_WIDTH * IND_COLS + IND_PADDING_RIGHT;
  public static final int IND_HEIGHT = IND_PADDING_TOP + IND_FONT_HEIGHT + IND_PADDING_BOTTOM;

  public static int indBackGround;  //背景色
  public static int indColor00;  //文字色 白 負荷率0%
  public static int indColor20;  //       水 負荷率20%
  public static int indColor40;  //       緑 負荷率40%
  public static int indColor60;  //       黄 負荷率60%
  public static int indColor80;  //       赤 負荷率80%

  public static int indLastMPU;

  public static BufferedImage indImage;
  public static int[] indBitmap;
  public static Box indBox;

  public static void indInit () {
    indBackGround = LnF.lnfRGB[0];
    indColor00 = LnF.lnfRGB[14];
    indColor20 = 0xff00ffff;
    indColor40 = 0xff00ff00;
    indColor60 = 0xffffff00;
    indColor80 = 0xffff0000;

    indLastMPU = 0;

    indImage = new BufferedImage (IND_WIDTH, IND_HEIGHT, BufferedImage.TYPE_INT_RGB);
    indBitmap = ((DataBufferInt) indImage.getRaster ().getDataBuffer ()).getData ();
    Arrays.fill (indBitmap, indBackGround);
    indBox = ComponentFactory.setFixedSize (
      new Box (BoxLayout.LINE_AXIS) {
        @Override public void paint (Graphics g) {
          g.drawImage (indImage, 0, 0, null);
        }
        @Override protected void paintComponent (Graphics g) {
        }
        @Override protected void paintBorder (Graphics g) {
        }
        @Override protected void paintChildren (Graphics g) {
        }
        @Override public void update (Graphics g) {
        }
      },
      IND_WIDTH, IND_HEIGHT);
  }

  public static void indUpdate (double actualPercent) {
    //  |          1111111111222222222|
    //  |01234567890123456789012345678|
    //  |MC68EC030 999.9MHz 100.0% PTH|
    //MPU
    if (indLastMPU != XEiJ.currentMPU) {
      indLastMPU = XEiJ.currentMPU;
      int color = indColor00;
      String s = Model.mpuNameOf (XEiJ.currentMPU);
      int l = s.length ();
      for (int i = 0; i < 9; i++) {
        indPutChar (IND_MPU_X + i, i < l ? s.charAt (i) : ' ', color);
      }
    }
    //周波数
    {
      int color = indColor00;
      int t = XEiJ.FMT_BCD4[Math.max (0, Math.min (9999, (int) (10.0 * XEiJ.mpuCurrentMHz + 0.5)))];
      indPutChar (IND_CLOCK_X + 0, t < 0x1000 ? ' ' :  (t >> 12)       + '0', color);
      indPutChar (IND_CLOCK_X + 1, t < 0x0100 ? ' ' : ((t >>  8) & 15) + '0', color);
      indPutChar (IND_CLOCK_X + 2,                    ((t >>  4) & 15) + '0', color);
      indPutChar (IND_CLOCK_X + 3, '.', color);
      indPutChar (IND_CLOCK_X + 4,                    ( t        & 15) + '0', color);
      indPutChar (IND_CLOCK_X + 5, 'M', color);
      indPutChar (IND_CLOCK_X + 6, 'H', color);
      indPutChar (IND_CLOCK_X + 7, 'z', color);
    }
    //負荷率
    {
      int t = XEiJ.FMT_BCD4[Math.max (0, Math.min (9999, (int) (10.0 * actualPercent + 0.5)))];
      int color = (t < 0x0200 ? indColor00 :
                   t < 0x0400 ? indColor20 :
                   t < 0x0600 ? indColor40 :
                   t < 0x0800 ? indColor60 :
                   indColor80);
      indPutChar (IND_RATIO_X + 0, t < 0x1000 ? ' ' :  (t >> 12)       + '0', color);
      indPutChar (IND_RATIO_X + 1, t < 0x0100 ? ' ' : ((t >>  8) & 15) + '0', color);
      indPutChar (IND_RATIO_X + 2,                    ((t >>  4) & 15) + '0', color);
      indPutChar (IND_RATIO_X + 3, '.', color);
      indPutChar (IND_RATIO_X + 4,                    ( t        & 15) + '0', color);
      indPutChar (IND_RATIO_X + 5, '%', color);
    }
    indBox.repaint ();
  }

  public static void indUpdateLED () {
    //タイマーランプ
    //  RTC CLKOUTがHighのとき赤
    //  それ以外は黒
    //indPutChar (IND_TIMER_X,
    //            0xff,
    //            RP5C15.rtcProcessClkoutHigh ? indColor80 :  //赤
    //            indBackGround);  //黒
    //電源ランプ
    //  (VCC2がOFFのとき黒)
    //  (VCC2がONかつ)VCC1がOFFのとき赤
    //  電源スイッチがON(Low)またはRTC ALARMがLowのとき緑または黄
    //  それ以外は黒
    if (PowerControl.PWC_ON) {
      indPutChar (IND_POWER_X,
                  0xff,
                  !PowerControl.pwcVcc1On ? indColor80 :  //赤
                  PowerControl.pwcPowerOn ||
                  //!RP5C15.rtcProcessAlarmHigh ?
                  //!RP5C15.rtcProcessInvalid ? indColor40 : indColor60 :  //緑または黄
                  !RP5C15.rtcAlarmHigh ? indColor40 :  //緑
                  indBackGround);  //黒
    }
    indBox.repaint ();
  }

  public static void indPutChar (int col, int c, int color) {
    if (IND_FONT_HEIGHT == 8) {  //6x8
      int src = IND_FONT_HEIGHT * c;
      int dst = IND_PADDING_LEFT + IND_FONT_WIDTH * col + IND_WIDTH * IND_PADDING_TOP;
      for (int v = 0; v < IND_FONT_HEIGHT; v++) {
        int t = FontPage.Lcd.LCD6X8_FONT[src] << (32 - 8);
        for (int u = 0; u < IND_FONT_WIDTH; u++) {
          indBitmap[dst + u] = t < 0 ? color : indBackGround;
          t <<= 1;
        }
        src++;
        dst += IND_WIDTH;
      }
    } else {  //6x12
      int src = 0x00fef400 - 0x00fc0000 + IND_FONT_HEIGHT * c;
      int dst = IND_PADDING_LEFT + IND_FONT_WIDTH * col + IND_WIDTH * IND_PADDING_TOP;
      for (int v = 0; v < IND_FONT_HEIGHT; v++) {
        int t = ROM.iplrom16scsi256[src] << (32 - 8);
        for (int u = 0; u < IND_FONT_WIDTH; u++) {
          indBitmap[dst + u] = t < 0 ? color : indBackGround;
          t <<= 1;
        }
        src++;
        dst += IND_WIDTH;
      }
    }
  }

/*
  public static final short[] IND_ASCII_3X5 = {
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    //20
    (0b000 << 12 |
     0b000 <<  9 |
     0b000 <<  6 |
     0b000 <<  3 |
     0b000),
    //21 !
    (0b010 << 12 |
     0b010 <<  9 |
     0b010 <<  6 |
     0b000 <<  3 |
     0b010),
    //22 "
    (0b101 << 12 |
     0b101 <<  9 |
     0b000 <<  6 |
     0b000 <<  3 |
     0b000),
    //23 #
    (0b101 << 12 |
     0b111 <<  9 |
     0b101 <<  6 |
     0b111 <<  3 |
     0b101),
    //24 $
    (0b011 << 12 |
     0b110 <<  9 |
     0b010 <<  6 |
     0b011 <<  3 |
     0b110),
    //25 %
    (0b101 << 12 |
     0b110 <<  9 |
     0b010 <<  6 |
     0b011 <<  3 |
     0b101),
    //26 &
    (0b110 << 12 |
     0b110 <<  9 |
     0b010 <<  6 |
     0b101 <<  3 |
     0b110),
    //27 '
    (0b010 << 12 |
     0b010 <<  9 |
     0b000 <<  6 |
     0b000 <<  3 |
     0b000),
    //28 (
    (0b001 << 12 |
     0b010 <<  9 |
     0b010 <<  6 |
     0b010 <<  3 |
     0b001),
    //29 )
    (0b100 << 12 |
     0b010 <<  9 |
     0b010 <<  6 |
     0b010 <<  3 |
     0b100),
    //2a *
    (0b000 << 12 |
     0b101 <<  9 |
     0b010 <<  6 |
     0b101 <<  3 |
     0b000),
    //2b +
    (0b000 << 12 |
     0b010 <<  9 |
     0b111 <<  6 |
     0b010 <<  3 |
     0b000),
    //2c ,
    (0b000 << 12 |
     0b000 <<  9 |
     0b000 <<  6 |
     0b010 <<  3 |
     0b010),
    //2d -
    (0b000 << 12 |
     0b000 <<  9 |
     0b111 <<  6 |
     0b000 <<  3 |
     0b000),
    //2e .
    (0b000 << 12 |
     0b000 <<  9 |
     0b000 <<  6 |
     0b000 <<  3 |
     0b010),
    //2f /
    (0b000 << 12 |
     0b001 <<  9 |
     0b010 <<  6 |
     0b100 <<  3 |
     0b000),
    //30 0
    (0b111 << 12 |
     0b101 <<  9 |
     0b101 <<  6 |
     0b101 <<  3 |
     0b111),
    //31 1
    (0b010 << 12 |
     0b010 <<  9 |
     0b010 <<  6 |
     0b010 <<  3 |
     0b010),
    //32 2
    (0b111 << 12 |
     0b001 <<  9 |
     0b111 <<  6 |
     0b100 <<  3 |
     0b111),
    //33 3
    (0b111 << 12 |
     0b001 <<  9 |
     0b111 <<  6 |
     0b001 <<  3 |
     0b111),
    //34 4
    (0b101 << 12 |
     0b101 <<  9 |
     0b111 <<  6 |
     0b001 <<  3 |
     0b001),
    //35 5
    (0b111 << 12 |
     0b100 <<  9 |
     0b111 <<  6 |
     0b001 <<  3 |
     0b111),
    //36 6
    (0b111 << 12 |
     0b100 <<  9 |
     0b111 <<  6 |
     0b101 <<  3 |
     0b111),
    //37 7
    (0b111 << 12 |
     0b001 <<  9 |
     0b001 <<  6 |
     0b001 <<  3 |
     0b001),
    //38 8
    (0b111 << 12 |
     0b101 <<  9 |
     0b111 <<  6 |
     0b101 <<  3 |
     0b111),
    //39 9
    (0b111 << 12 |
     0b101 <<  9 |
     0b111 <<  6 |
     0b001 <<  3 |
     0b111),
    //3a :
    (0b000 << 12 |
     0b010 <<  9 |
     0b000 <<  6 |
     0b010 <<  3 |
     0b000),
    //3b ;
    (0b000 << 12 |
     0b010 <<  9 |
     0b000 <<  6 |
     0b010 <<  3 |
     0b010),
    //3c <
    (0b001 << 12 |
     0b010 <<  9 |
     0b100 <<  6 |
     0b010 <<  3 |
     0b001),
    //3d =
    (0b000 << 12 |
     0b111 <<  9 |
     0b000 <<  6 |
     0b111 <<  3 |
     0b000),
    //3e >
    (0b100 << 12 |
     0b010 <<  9 |
     0b001 <<  6 |
     0b010 <<  3 |
     0b100),
    //3f ?
    (0b110 << 12 |
     0b001 <<  9 |
     0b010 <<  6 |
     0b000 <<  3 |
     0b010),
    //40 @
    (0b010 << 12 |
     0b101 <<  9 |
     0b111 <<  6 |
     0b100 <<  3 |
     0b011),
    //41 A
    (0b010 << 12 |
     0b101 <<  9 |
     0b111 <<  6 |
     0b101 <<  3 |
     0b101),
    //42 B
    (0b110 << 12 |
     0b101 <<  9 |
     0b110 <<  6 |
     0b101 <<  3 |
     0b110),
    //43 C
    (0b011 << 12 |
     0b100 <<  9 |
     0b100 <<  6 |
     0b100 <<  3 |
     0b011),
    //44 D
    (0b110 << 12 |
     0b101 <<  9 |
     0b101 <<  6 |
     0b101 <<  3 |
     0b110),
    //45 E
    (0b111 << 12 |
     0b100 <<  9 |
     0b111 <<  6 |
     0b100 <<  3 |
     0b111),
    //46 F
    (0b111 << 12 |
     0b100 <<  9 |
     0b111 <<  6 |
     0b100 <<  3 |
     0b100),
    //47 G
    (0b011 << 12 |
     0b100 <<  9 |
     0b101 <<  6 |
     0b101 <<  3 |
     0b011),
    //48 H
    (0b101 << 12 |
     0b101 <<  9 |
     0b111 <<  6 |
     0b101 <<  3 |
     0b101),
    //49 I
    (0b111 << 12 |
     0b010 <<  9 |
     0b010 <<  6 |
     0b010 <<  3 |
     0b111),
    //4a J
    (0b001 << 12 |
     0b001 <<  9 |
     0b001 <<  6 |
     0b001 <<  3 |
     0b110),
    //4b K
    (0b101 << 12 |
     0b101 <<  9 |
     0b110 <<  6 |
     0b101 <<  3 |
     0b101),
    //4c L
    (0b100 << 12 |
     0b100 <<  9 |
     0b100 <<  6 |
     0b100 <<  3 |
     0b111),
    //4d M
    (0b101 << 12 |
     0b111 <<  9 |
     0b111 <<  6 |
     0b101 <<  3 |
     0b101),
    //4e N
    (0b110 << 12 |
     0b101 <<  9 |
     0b101 <<  6 |
     0b101 <<  3 |
     0b101),
    //4f O
    (0b010 << 12 |
     0b101 <<  9 |
     0b101 <<  6 |
     0b101 <<  3 |
     0b010),
    //50 P
    (0b110 << 12 |
     0b101 <<  9 |
     0b110 <<  6 |
     0b100 <<  3 |
     0b100),
    //51 Q
    (0b010 << 12 |
     0b101 <<  9 |
     0b101 <<  6 |
     0b111 <<  3 |
     0b011),
    //52 R
    (0b110 << 12 |
     0b101 <<  9 |
     0b110 <<  6 |
     0b101 <<  3 |
     0b101),
    //53 S
    (0b011 << 12 |
     0b100 <<  9 |
     0b010 <<  6 |
     0b001 <<  3 |
     0b110),
    //54 T
    (0b111 << 12 |
     0b010 <<  9 |
     0b010 <<  6 |
     0b010 <<  3 |
     0b010),
    //55 U
    (0b101 << 12 |
     0b101 <<  9 |
     0b101 <<  6 |
     0b101 <<  3 |
     0b111),
    //56 V
    (0b101 << 12 |
     0b101 <<  9 |
     0b101 <<  6 |
     0b101 <<  3 |
     0b010),
    //57 W
    (0b101 << 12 |
     0b101 <<  9 |
     0b111 <<  6 |
     0b111 <<  3 |
     0b101),
    //58 X
    (0b101 << 12 |
     0b101 <<  9 |
     0b010 <<  6 |
     0b101 <<  3 |
     0b101),
    //59 Y
    (0b101 << 12 |
     0b101 <<  9 |
     0b010 <<  6 |
     0b010 <<  3 |
     0b010),
    //5a Z
    (0b111 << 12 |
     0b001 <<  9 |
     0b010 <<  6 |
     0b100 <<  3 |
     0b111),
    //5b [
    (0b011 << 12 |
     0b010 <<  9 |
     0b010 <<  6 |
     0b010 <<  3 |
     0b011),
    //5c \ 
    (0b000 << 12 |
     0b100 <<  9 |
     0b010 <<  6 |
     0b001 <<  3 |
     0b000),
    //5d ]
    (0b110 << 12 |
     0b010 <<  9 |
     0b010 <<  6 |
     0b010 <<  3 |
     0b110),
    //5e ^
    (0b010 << 12 |
     0b101 <<  9 |
     0b000 <<  6 |
     0b000 <<  3 |
     0b000),
    //5f _
    (0b000 << 12 |
     0b000 <<  9 |
     0b000 <<  6 |
     0b000 <<  3 |
     0b111),
    //60 `
    (0b010 << 12 |
     0b001 <<  9 |
     0b000 <<  6 |
     0b000 <<  3 |
     0b000),
    //61 a
    (0b000 << 12 |
     0b011 <<  9 |
     0b101 <<  6 |
     0b101 <<  3 |
     0b011),
    //62 b
    (0b100 << 12 |
     0b110 <<  9 |
     0b101 <<  6 |
     0b101 <<  3 |
     0b110),
    //63 c
    (0b000 << 12 |
     0b011 <<  9 |
     0b100 <<  6 |
     0b100 <<  3 |
     0b011),
    //64 d
    (0b001 << 12 |
     0b011 <<  9 |
     0b101 <<  6 |
     0b101 <<  3 |
     0b011),
    //65 e
    (0b000 << 12 |
     0b011 <<  9 |
     0b101 <<  6 |
     0b110 <<  3 |
     0b011),
    //66 f
    (0b011 << 12 |
     0b111 <<  9 |
     0b010 <<  6 |
     0b010 <<  3 |
     0b010),
    //67 g
    (0b011 << 12 |
     0b101 <<  9 |
     0b111 <<  6 |
     0b001 <<  3 |
     0b110),
    //68 h
    (0b100 << 12 |
     0b110 <<  9 |
     0b101 <<  6 |
     0b101 <<  3 |
     0b101),
    //69 i
    (0b010 << 12 |
     0b000 <<  9 |
     0b010 <<  6 |
     0b010 <<  3 |
     0b010),
    //6a j
    (0b001 << 12 |
     0b000 <<  9 |
     0b001 <<  6 |
     0b001 <<  3 |
     0b110),
    //6b k
    (0b100 << 12 |
     0b101 <<  9 |
     0b110 <<  6 |
     0b110 <<  3 |
     0b101),
    //6c l
    (0b110 << 12 |
     0b010 <<  9 |
     0b010 <<  6 |
     0b010 <<  3 |
     0b010),
    //6d m
    (0b000 << 12 |
     0b110 <<  9 |
     0b111 <<  6 |
     0b111 <<  3 |
     0b101),
    //6e n
    (0b000 << 12 |
     0b110 <<  9 |
     0b101 <<  6 |
     0b101 <<  3 |
     0b101),
    //6f o
    (0b000 << 12 |
     0b010 <<  9 |
     0b101 <<  6 |
     0b101 <<  3 |
     0b010),
    //70 p
    (0b000 << 12 |
     0b110 <<  9 |
     0b101 <<  6 |
     0b110 <<  3 |
     0b100),
    //71 q
    (0b000 << 12 |
     0b011 <<  9 |
     0b101 <<  6 |
     0b011 <<  3 |
     0b001),
    //72 r
    (0b000 << 12 |
     0b011 <<  9 |
     0b100 <<  6 |
     0b100 <<  3 |
     0b100),
    //73 s
    (0b000 << 12 |
     0b011 <<  9 |
     0b010 <<  6 |
     0b001 <<  3 |
     0b110),
    //74 t
    (0b010 << 12 |
     0b111 <<  9 |
     0b010 <<  6 |
     0b010 <<  3 |
     0b001),
    //75 u
    (0b000 << 12 |
     0b101 <<  9 |
     0b101 <<  6 |
     0b101 <<  3 |
     0b011),
    //76 v
    (0b000 << 12 |
     0b101 <<  9 |
     0b101 <<  6 |
     0b101 <<  3 |
     0b010),
    //77 w
    (0b000 << 12 |
     0b101 <<  9 |
     0b111 <<  6 |
     0b111 <<  3 |
     0b011),
    //78 x
    (0b000 << 12 |
     0b101 <<  9 |
     0b010 <<  6 |
     0b010 <<  3 |
     0b101),
    //79 y
    (0b000 << 12 |
     0b101 <<  9 |
     0b101 <<  6 |
     0b010 <<  3 |
     0b100),
    //7a z
    (0b000 << 12 |
     0b111 <<  9 |
     0b001 <<  6 |
     0b010 <<  3 |
     0b111),
    //7b {
    (0b001 << 12 |
     0b010 <<  9 |
     0b110 <<  6 |
     0b010 <<  3 |
     0b001),
    //7c |
    (0b010 << 12 |
     0b010 <<  9 |
     0b000 <<  6 |
     0b010 <<  3 |
     0b010),
    //7d }
    (0b100 << 12 |
     0b010 <<  9 |
     0b011 <<  6 |
     0b010 <<  3 |
     0b100),
    //7e ~
    (0b111 << 12 |
     0b000 <<  9 |
     0b000 <<  6 |
     0b000 <<  3 |
     0b000),
  };
*/
  //  perl misc/itoc.pl xeij/Indicator.java IND_ASCII_3X5
  public static final char[] IND_ASCII_3X5 = "\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\u2482\u5a00\u5f7d\u3c9e\u5c9d\u6cae\u2400\u1491\u4494\u0aa8\u05d0\22\u01c0\2\u02a0\u7b6f\u2492\u73e7\u73cf\u5bc9\u79cf\u79ef\u7249\u7bef\u7bcf\u0410\u0412\u1511\u0e38\u4454\u6282\u2be3\u2bed\u6bae\u3923\u6b6e\u79e7\u79e4\u396b\u5bed\u7497\u124e\u5bad\u4927\u5fed\u6b6d\u2b6a\u6ba4\u2b7b\u6bad\u388e\u7492\u5b6f\u5b6a\u5bfd\u5aad\u5a92\u72a7\u3493\u0888\u6496\u2a00\7\u2200\u076b\u4d6e\u0723\u176b\u0773\u3e92\u3bce\u4d6d\u2092\u104e\u4bb5\u6492\u0dfd\u0d6d\u056a\u0d74\u0759\u0724\u068e\u2e91\u0b6b\u0b6a\u0bfb\u0a95\u0b54\u0e57\u1591\u2412\u44d4\u7000".toCharArray ();

}
