//========================================================================================
//  Bubble.java
//    en:Bubble
//    ja:バブル
//  Copyright (C) 2003-2025 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.awt.*;  //RenderingHints
import java.awt.font.*;  //TextLayout
import java.awt.geom.*;  //Rectangle2D

//class Bubble
//  バブル
public class Bubble {

  public static final boolean BBL_ON = true;
  public static final int BBL_FONT_SIZE = 24;  //フォントサイズ
  public static final int BBL_Y = BBL_FONT_SIZE * 2;  //Y座標
  public static final int BBL_PADDING_X = BBL_FONT_SIZE * 1;  //X方向パディング
  public static final int BBL_PADDING_Y = BBL_FONT_SIZE / 2;  //Y方向パディング
  public static final Font BBL_FONT = new Font ("SansSerif", Font.PLAIN, BBL_FONT_SIZE);  //フォント

  public static Color bblBackground;  //背景色
  public static Color bblForeground;  //文字色
  public static String bblText;  //テキスト
  public static long bblEndTime;  //終了時刻

  public static void bblInit () {
    bblBackground = new Color (LnF.lnfRGB[14]);
    bblForeground = new Color (LnF.lnfRGB[0]);
    bblText = null;
    bblEndTime = 0L;
  }  //bblInit

  //bblStart (text, time)
  //  開始
  public static void bblStart (String text, long time) {
    bblText = text;
    bblEndTime = System.currentTimeMillis () + time;
  }  //bblStart

  //bblDraw (g2)
  //  表示
  public static void bblDraw (Graphics2D g2) {
    if (bblText != null) {
      g2.setRenderingHint (RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g2.setFont (BBL_FONT);
      TextLayout tl = new TextLayout (bblText, BBL_FONT, g2.getFontRenderContext ());
      Rectangle2D r = tl.getBounds ();
      int x = (XEiJ.pnlWidth >> 1) - ((int) r.getWidth () >> 1);
      int y = BBL_Y;
      g2.setColor (bblBackground);
      g2.fillRect ((int) r.getX () + x - BBL_PADDING_X,
                   (int) r.getY () + y - BBL_PADDING_Y,
                   (int) r.getWidth () + (BBL_PADDING_X << 1),
                   (int) r.getHeight () + (BBL_PADDING_Y << 1));
      g2.setColor (bblForeground);
      tl.draw (g2, x, y);
      if (bblEndTime <= System.currentTimeMillis ()) {  //終了
        bblEnd ();
      }
    }
  }  //bblDraw

  //bblEnd ()
  //  終了
  public static void bblEnd () {
    bblText = null;
    bblEndTime = 0L;
  }  //bblEnd

}  //class Bubble
