//========================================================================================
//  Keyboard.java
//    en:Keyboard
//    ja:キーボード
//  Copyright (C) 2003-2026 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//! SHIFTとCTRLは左ボタンで押されただけでロックするようになっているが、
//  これをプレフィックスにして次に他のキーが離された後にSHIFTとCTRLも自動的に離されるモードが欲しい
//  右ボタンで押されたときのロックは従来通り、ロックしたキーをクリックするまで押されっぱなしにする
//----------------------------------------------------------------------------------------

package xeij;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.im.*;  //InputContext
import java.io.*;
import java.util.*;
import java.util.concurrent.*;  //ArrayBlockingQueue
import javax.swing.*;

public class Keyboard {

  //デバッグ
  public static final boolean KBD_DEBUG_LED = false;  //LEDキーが押されたらLEDを反転する

  //種類
  public static final int KBD_STANDARD_TYPE =  0;  //Standard Keyboard
  public static final int KBD_COMPACT_TYPE  =  1;  //Compact Keyboard
  public static final int KBD_TYPES         =  2;  //キーボードの種類の数。Standard KeyboardとCompact Keyboardの2種類

  //サイズ
  //  6+6*94+6=576,2+4*25+2=104  Standard Keyboard
  //  6+6*76+6=468,2+4*25+2=104  Compact Keyboard
  public static final int KBD_COL_WIDTH      =  6;  //列の幅(px)。可変キーの幅の1/4
  public static final int KBD_ROW_HEIGHT     =  4;  //行の高さ(px)。可変キーの高さの1/4
  public static final int KBD_LED_WIDTH      = 11;  //LEDの幅
  public static final int KBD_LED_HEIGHT     =  4;  //LEDのキートップ側の高さ
  public static final int KBD_PADDING_TOP    =  2;  //パディング(px)
  public static final int KBD_PADDING_RIGHT  =  6;
  public static final int KBD_PADDING_BOTTOM =  2;
  public static final int KBD_PADDING_LEFT   =  6;
  public static final int KBD_TOUCHABLE_AREA =  1;  //ポイントできる範囲。0=1段目から,1=2段目から,2=3段目から,3=キートップのみ
  public static final int[] KBD_TYPE_COLS    = { 94, 76 };  //種類→キーボードの幅(列数)
  public static final int[] KBD_TYPE_ROWS    = { 25, 25 };  //種類→キーボードの高さ(行数)
  public static final int KBD_KEY_WIDTH      = KBD_COL_WIDTH * 4 - 1;  //可変キーとLEDキーの幅
  public static final int KBD_KEY_HEIGHT     = KBD_ROW_HEIGHT * 4 - 1;  //可変キーとLEDキーの高さ

  //スタイル
  public static final int KBD_NONE_STYLE     = 0;  //キーなし
  public static final int KBD_NORMAL_STYLE   = 1;  //ノーマル
  public static final int KBD_FUNCTION_STYLE = 2;  //キーの上端を1/2行削る。63～6C:F1～F10
  public static final int KBD_NARROWER_STYLE = 3;  //キートップの左右を1/2列ずつ削る。55:XF1,56:XF2,57:XF3(Compact),58:XF4(Standard),59:XF5(Standard)
  public static final int KBD_NARROW_STYLE   = 4;  //キートップの左右を1列ずつ削る。10:TAB,57:XF3(Standard),71:CTRL,72:OPT.1,73:OPT.2
  public static final int KBD_SHORTER_STYLE  = 5;  //キートップの上下を1行ずつ削る。4E:ENTER(Standard)
  public static final int KBD_SHORT_STYLE    = 6;  //キートップの上下を2行ずつ削る。3B:LEFT(Standard),3D:RIGHT(Standard)
  public static final int KBD_RETURN_STYLE   = 7;  //レクタングルの左下を1列x4行削る。キートップの左右を1列ずつ削る。1D:RETURN
  public static final int KBD_SHIFT_STYLE    = 8;  //レクタングルの左右を9列ずつ残して中央を削る。70:SHIFT

  //文字
  public static final boolean KBD_USE_ROM_FONT = false;  //可変キーの文字をROMフォントにする。ROMフォントが生成済みでなければならない

  //色
  public static final int KBD_LETTER_COLOR       = 14;  //文字
  public static final int KBD_TOP_LEFT_COLOR     = 10;  //ボーダー左上
  public static final int KBD_TOP_COLOR          =  8;  //ボーダー上
  public static final int KBD_LEFT_COLOR         =  8;  //ボーダー左
  public static final int KBD_POINTED_COLOR      =  8;  //キートップ(pointed)
  public static final int KBD_TOP_RIGHT_COLOR    =  6;  //ボーダー右上
  public static final int KBD_BOTTOM_LEFT_COLOR  =  6;  //ボーダー左下
  public static final int KBD_KEYTOP_COLOR       =  6;  //キートップ
  public static final int KBD_RIGHT_COLOR        =  4;  //ボーダー右
  public static final int KBD_BOTTOM_COLOR       =  4;  //ボーダー下
  public static final int KBD_BOTTOM_RIGHT_COLOR =  2;  //ボーダー右下
  public static final int KBD_SHADOW_COLOR       =  2;  //文字の影
  public static final int KBD_BACKGROUND_COLOR   =  0;  //背景
  public static final int KBD_HOLE_COLOR         =  0;  //押し下げられたキーの周囲の隙間
  //
  public static int kbdLetterRGB;
  public static int kbdTopLeftRGB;
  public static int kbdTopRGB;
  public static int kbdLeftRGB;
  public static int kbdPointedRGB;
  public static int kbdTopRightRGB;
  public static int kbdBottomLeftRGB;
  public static int kbdKeytopRGB;
  public static int kbdRightRGB;
  public static int kbdBottomRGB;
  public static int kbdBottomRightRGB;
  public static int kbdShadowRGB;
  public static int kbdBackgroundRGB;
  public static int kbdHoleRGB;
  //
  public static final int KBD_RED_OFF_KEYTOP_COLOR    = Color.HSBtoRGB (0.99F, 0.50F, 0.40F);  //赤色LED、消灯、キートップ側
  public static final int KBD_RED_OFF_BORDER_COLOR    = Color.HSBtoRGB (0.99F, 0.50F, 0.20F);  //赤色LED、消灯、ボーダー側
  public static final int KBD_RED_ON_KEYTOP_COLOR_0   = Color.HSBtoRGB (0.99F, 1.00F, 1.00F - 0.150F * 0);  //赤色LED、明るい、キートップ側
  public static final int KBD_RED_ON_BORDER_COLOR_0   = Color.HSBtoRGB (0.99F, 1.00F, 0.50F - 0.075F * 0);  //赤色LED、明るい、ボーダー側
  public static final int KBD_RED_ON_KEYTOP_COLOR_1   = Color.HSBtoRGB (0.99F, 1.00F, 1.00F - 0.150F * 1);  //赤色LED、やや明るい、キートップ側
  public static final int KBD_RED_ON_BORDER_COLOR_1   = Color.HSBtoRGB (0.99F, 1.00F, 0.50F - 0.075F * 1);  //赤色LED、やや明るい、ボーダー側
  public static final int KBD_RED_ON_KEYTOP_COLOR_2   = Color.HSBtoRGB (0.99F, 1.00F, 1.00F - 0.150F * 2);  //赤色LED、やや暗い、キートップ側
  public static final int KBD_RED_ON_BORDER_COLOR_2   = Color.HSBtoRGB (0.99F, 1.00F, 0.50F - 0.075F * 2);  //赤色LED、やや暗い、ボーダー側
  public static final int KBD_RED_ON_KEYTOP_COLOR_3   = Color.HSBtoRGB (0.99F, 1.00F, 1.00F - 0.150F * 3);  //赤色LED、暗い、キートップ側
  public static final int KBD_RED_ON_BORDER_COLOR_3   = Color.HSBtoRGB (0.99F, 1.00F, 0.50F - 0.075F * 3);  //赤色LED、暗い、ボーダー側
  public static final int KBD_GREEN_OFF_KEYTOP_COLOR  = Color.HSBtoRGB (0.24F, 0.50F, 0.40F);  //緑色LED、消灯、キートップ側
  public static final int KBD_GREEN_OFF_BORDER_COLOR  = Color.HSBtoRGB (0.24F, 0.50F, 0.20F);  //緑色LED、消灯、ボーダー側
  public static final int KBD_GREEN_ON_KEYTOP_COLOR_0 = Color.HSBtoRGB (0.24F, 1.00F, 1.00F - 0.150F * 0);  //緑色LED、明るい、キートップ側
  public static final int KBD_GREEN_ON_BORDER_COLOR_0 = Color.HSBtoRGB (0.24F, 1.00F, 0.50F - 0.075F * 0);  //緑色LED、明るい、ボーダー側
  public static final int KBD_GREEN_ON_KEYTOP_COLOR_1 = Color.HSBtoRGB (0.24F, 1.00F, 1.00F - 0.150F * 1);  //緑色LED、やや明るい、キートップ側
  public static final int KBD_GREEN_ON_BORDER_COLOR_1 = Color.HSBtoRGB (0.24F, 1.00F, 0.50F - 0.075F * 1);  //緑色LED、やや明るい、ボーダー側
  public static final int KBD_GREEN_ON_KEYTOP_COLOR_2 = Color.HSBtoRGB (0.24F, 1.00F, 1.00F - 0.150F * 2);  //緑色LED、やや暗い、キートップ側
  public static final int KBD_GREEN_ON_BORDER_COLOR_2 = Color.HSBtoRGB (0.24F, 1.00F, 0.50F - 0.075F * 2);  //緑色LED、やや暗い、ボーダー側
  public static final int KBD_GREEN_ON_KEYTOP_COLOR_3 = Color.HSBtoRGB (0.24F, 1.00F, 1.00F - 0.150F * 3);  //緑色LED、暗い、キートップ側
  public static final int KBD_GREEN_ON_BORDER_COLOR_3 = Color.HSBtoRGB (0.24F, 1.00F, 0.50F - 0.075F * 3);  //緑色LED、暗い、ボーダー側

  //状態
  //  stat = KBD_LOCKED_MASK | KBD_KEYDOWN_MASK | KBD_MOUSEDOWN_MASK | KBD_BRIGHTNESS_MASK | KBD_LIGHTED_MASK | KBD_PRESSED_MASK | KBD_POINTED_MASK
  public static final int KBD_POINTED_BIT     =          0;  //bit0 1=ポイントされている。ポイントされているキーは1つしか存在しない
  public static final int KBD_POINTED_MASK    = 0b00000001;
  public static final int KBD_PRESSED_BIT     =         1;  //bit1 1=押されている。ロックされているか、キーで押されているか、マウスで押されている
  public static final int KBD_PRESSED_MASK    = 0b00000010;
  public static final int KBD_LIGHTED_BIT     =        2;  //bit2 (LEDキーのとき)1=LEDが点灯している
  public static final int KBD_LIGHTED_MASK    = 0b00000100;
  public static final int KBD_BRIGHTNESS_BIT  =       3;  //bit4-3 (LEDキーかつLEDが点灯しているとき)0=明るい,1=やや明るい,2=やや暗い,3=暗い
  public static final int KBD_BRIGHTNESS_MASK = 0b00011000;
  //  △ここまで描画に使う。ビット位置に依存していることに注意
  //  ▽ここから描画に使わない
  public static final int KBD_MOUSEDOWN_BIT   =     5;  //bit3 1=マウスで押されている
  public static final int KBD_MOUSEDOWN_MASK  = 0b00100000;
  public static final int KBD_KEYDOWN_BIT     =    6;  //bit4 1=キーで押されている
  public static final int KBD_KEYDOWN_MASK    = 0b01000000;
  public static final int KBD_LOCKED_BIT      =   7;  //bit5 1=ロックされている
  public static final int KBD_LOCKED_MASK     = 0b10000000;
  //  LEDキー
  //    lighted||pointedかどうかで可変キーに表示する文字を変更する
  //    KBD_LED_TEST << (stat & (kbdStat[KBD_NUM_INDEX] & (KBD_LIGHTED_MASK | KBD_PRESSED_MASK | KBD_POINTED_MASK)) < 0
  //  その他のモードキー
  //    pressed||pointedかどうかで可変キーに表示する文字を変更する
  //    KBD_MODE_TEST << (stat & (kbdStat[KBD_NUM_INDEX] & (KBD_LIGHTED_MASK | KBD_PRESSED_MASK | KBD_POINTED_MASK)) < 0
  public static final int KBD_LED_TEST  = 0b01011010_01011010_01011010_01011010;
  public static final int KBD_MODE_TEST = 0b01100110_01100110_01100110_01100110;

  //数
  public static final int KBD_VAR_KEYS = 48;  //可変キーの数
  public static final int KBD_LED_KEYS =  8;  //LEDキーの数
  public static final int KBD_FIX_KEYS = 57;  //固定キーの数
  public static final int KBD_ALL_KEYS = KBD_VAR_KEYS + KBD_LED_KEYS + KBD_FIX_KEYS;  //キーの数

  //番号
  //  可変キー,LEDキー,固定キーの順序でスキャンコード順に割り当てた番号
  //  RETURNキーの上下とSHIFTキーの左右は形を特別扱いにすることで1個の番号で扱う
  public static final int KBD_KANA_INDEX   =  48;  //かなキーの番号
  public static final int KBD_ROMA_INDEX   =  49;  //ローマ字キーの番号
  public static final int KBD_CODE_INDEX   =  50;  //コード入力キーの番号
  public static final int KBD_CAPS_INDEX   =  51;  //CAPSキーの番号
  public static final int KBD_HIRA_INDEX   =  53;  //ひらがなキーの番号
  public static final int KBD_ZEN_INDEX    =  54;  //全角キーの番号
  public static final int KBD_NUM_INDEX    =  55;  //NUMキーの番号
  public static final int KBD_RETURN_INDEX =  59;  //RETURNキーの番号
  public static final int KBD_SHIFT_INDEX  = 109;  //SHIFTキーの番号
  public static final int KBD_CTRL_INDEX   = 110;  //CTRLキーの番号
  public static final int KBD_OPT1_INDEX   = 111;  //OPT.1キーの番号

  //番号→スキャンコード(NUM OFF)
/*
  public static final byte[] KBD_NORMAL_CODE = {
    //              |1| | | | | | | | | | | | |NUM
    //              | |1| | | | | | | | | | | |CTRL
    //              | | |1| | | | | | | | | | |コード入力
    //              | | | |0|0|0|0|0|0| | | | |OPT.1
    //              | | | |1|1|1|1| | | | | | |かな。ひらがな+全角→ひらがな
    //              | | | | | | | |1|1| | | | |ローマ字
    //              | | | |0|0|1|1| | | | | | |五十音
    //              | | | | | | | | | |0|0|1|1|CAPS
    //              | | | |0|1|0|1|0|1|0|1|0|1|SHIFT
    0x02,  //  0  0 | | |1|ﾇ|ﾇ|ｱ|ｧ|1|!|1|!|1|!|
    0x03,  //  1  1 | | |2|ﾌ|ﾌ|ｲ|ｨ|2|"|2|"|2|"|
    0x04,  //  2  2 | | |3|ｱ|ｧ|ｳ|ｩ|3|#|3|#|3|#|
    0x05,  //  3  3 | | |4|ｳ|ｩ|ｴ|ｪ|4|$|4|$|4|$|
    0x06,  //  4  4 | | |5|ｴ|ｪ|ｵ|ｫ|5|%|5|%|5|%|
    0x07,  //  5  5 | | |6|ｵ|ｫ|ﾅ|ﾅ|6|&|6|&|6|&|
    0x08,  //  6  6 |7| |7|ﾔ|ｬ|ﾆ|ﾆ|7|'|7|'|7|'|
    0x09,  //  7  7 |8| |8|ﾕ|ｭ|ﾇ|ﾇ|8|(|8|(|8|(|
    0x0a,  //  8  8 |9| |9|ﾖ|ｮ|ﾈ|ﾈ|9|)|9|)|9|)|
    0x0b,  //  9  9 |/| |0|ﾜ|ｦ|ﾉ|ｦ|0| |0| |0| |
    0x0c,  // 10 10 |*| | |ﾎ|ﾎ|ﾗ|ﾗ|ｰ|=|-|=|-|=|
    0x0d,  // 11 11 | |^| |ﾍ|ﾍ|ﾘ|ﾘ|^|~|^|~|^|~|
    0x0e,  // 12 12 | |\| |ｰ|ｰ|ﾙ|ﾙ|\|||\|||\|||
    0x11,  // 13 13 | |Q| |ﾀ|ﾀ|ｶ|ｶ|Q|Q|q|Q|Q|q|
    0x12,  // 14 14 | |W| |ﾃ|ﾃ|ｷ|ｷ|W|W|w|W|W|w|
    0x13,  // 15 15 | |E|e|ｲ|ｨ|ｸ|ｸ|E|e|e|E|E|e|
    0x14,  // 16 16 | |R| |ｽ|ｽ|ｹ|ｹ|R|R|r|R|R|r|
    0x15,  // 17 17 | |T| |ｶ|ｶ|ｺ|ｺ|T|T|t|T|T|t|
    0x16,  // 18 18 | |Y| |ﾝ|ﾝ|ﾊ|ﾊ|Y|Y|y|Y|Y|y|
    0x17,  // 19 19 |4|U| |ﾅ|ﾅ|ﾋ|ﾋ|U|u|u|U|U|u|
    0x18,  // 20 20 |5|I| |ﾆ|ﾆ|ﾌ|ﾌ|I|i|i|I|I|i|
    0x19,  // 21 21 |6|O| |ﾗ|ﾗ|ﾍ|ﾍ|O|o|o|O|O|o|
    0x1a,  // 22 22 |-|P| |ｾ|ｾ|ﾎ|ﾎ|P|P|p|P|P|p|
    0x1b,  // 23 23 | | | |ﾞ|ﾞ|ﾚ|ﾚ|@|`|@|`|@|`|
    0x1c,  // 24 24 | |[| |ﾟ|｢|ﾛ|｢|｢|{|[|{|[|{|
    0x1e,  // 25 25 | |A|a|ﾁ|ﾁ|ｻ| |A|a|a|A|A|a| かな+五十音+SHIFT→0xa0
    0x1f,  // 26 26 | |S| |ﾄ|ﾄ|ｼ|ｼ|S|S|s|S|S|s|
    0x20,  // 27 27 | |D|d|ｼ|ｼ|ｽ|ｽ|D|D|d|D|D|d|
    0x21,  // 28 28 | |F|f|ﾊ|ﾊ|ｾ|ｾ|F|F|f|F|F|f|
    0x22,  // 29 29 | |G| |ｷ|ｷ|ｿ|ｿ|G|G|g|G|G|g|
    0x23,  // 30 30 | |H| |ｸ|ｸ|ﾏ|ﾏ|H|H|h|H|H|h|
    0x24,  // 31 31 |1|J| |ﾏ|ﾏ|ﾐ|ﾐ|J|J|j|J|J|j|
    0x25,  // 32 32 |2|K| |ﾉ|ﾉ|ﾑ|ﾑ|K|K|k|K|K|k|
    0x26,  // 33 33 |3|L| |ﾘ|ﾘ|ﾒ|ﾒ|L|L|l|L|L|l|
    0x27,  // 34 34 |+| | |ﾚ|ﾚ|ﾓ|ﾓ|;|+|;|+|;|+|
    0x28,  // 35 35 | | | |ｹ|ｹ|ﾞ|ｰ|:|*|:|*|:|*|
    0x29,  // 36 36 | |]| |ﾑ|｣|ﾟ|｣|｣|}|]|}|]|}|
    0x2a,  // 37 37 | |Z| |ﾂ|ｯ|ﾀ|ﾀ|Z|z|z|Z|Z|z|
    0x2b,  // 38 38 | |X| |ｻ|ｻ|ﾁ|ﾁ|X|X|x|X|X|x|
    0x2c,  // 39 39 | |C|c|ｿ|ｿ|ﾂ|ｯ|C|C|c|C|C|c|
    0x2d,  // 40 40 | |V| |ﾋ|ﾋ|ﾃ|ﾃ|V|V|v|V|V|v|
    0x2e,  // 41 41 | |B|b|ｺ|ｺ|ﾄ|ﾄ|B|B|b|B|B|b|
    0x2f,  // 42 42 | |N| |ﾐ|ﾐ|ﾔ|ｬ|N|N|n|N|N|n|
    0x30,  // 43 43 |0|M| |ﾓ|ﾓ|ﾕ|ｭ|M|M|m|M|M|m|
    0x31,  // 44 44 |,| | |ﾈ|､|ﾖ|ｮ|､|<|,|<|,|<|
    0x32,  // 45 45 |.| | |ﾙ|｡|ﾜ|､|｡|>|.|>|.|>|
    0x33,  // 46 46 |=| | |ﾒ|･|ｦ|｡|･|?|/|?|/|?|
    0x34,  // 47 47 |E|_| |ﾛ| |ﾝ|･| |_| |_| |_| NUM→ENTER、かな+SHIFT→0xa0
    0x5a,  // 48  0 かな
    0x5b,  // 49  1 ローマ字
    0x5c,  // 50  2 コード入力
    0x5d,  // 51  3 CAPS
    0x5e,  // 52  4 INS
    0x5f,  // 53  5 ひらがな
    0x60,  // 54  6 全角
    0x74,  // 55  7 NUM
    0x01,  // 56  0 ESC
    0x0f,  // 57  1 BS
    0x10,  // 58  2 TAB
    0x1d,  // 59  3 RETURN
    0x35,  // 60  4 Space
    0x36,  // 61  5 HOME
    0x37,  // 62  6 DEL
    0x38,  // 63  7 ROLLUP
    0x39,  // 64  8 ROLLDOWN
    0x3a,  // 65  9 UNDO
    0x3b,  // 66 10 ←
    0x3c,  // 67 11 ↑
    0x3d,  // 68 12 →
    0x3e,  // 69 13 ↓
    0x3f,  // 70 14 CLR
    0x40,  // 71 15 /
    0x41,  // 72 16 *
    0x42,  // 73 17 -
    0x43,  // 74 18 7
    0x44,  // 75 19 8
    0x45,  // 76 20 9
    0x46,  // 77 21 +
    0x47,  // 78 22 4
    0x48,  // 79 23 5
    0x49,  // 80 24 6
    0x4a,  // 81 25 =
    0x4b,  // 82 26 1
    0x4c,  // 83 27 2
    0x4d,  // 84 28 3
    0x4e,  // 85 29 ENTER
    0x4f,  // 86 30 0
    0x50,  // 87 31 ,
    0x51,  // 88 32 .
    0x52,  // 89 33 記号入力
    0x53,  // 90 34 登録
    0x54,  // 91 35 HELP
    0x55,  // 92 36 XF1
    0x56,  // 93 37 XF2
    0x57,  // 94 38 XF3
    0x58,  // 95 39 XF4
    0x59,  // 96 40 XF5
    0x61,  // 97 41 BREAK
    0x62,  // 98 42 COPY
    0x63,  // 99 43 F1
    0x64,  //100 44 F2
    0x65,  //101 45 F3
    0x66,  //102 46 F4
    0x67,  //103 47 F5
    0x68,  //104 48 F6
    0x69,  //105 49 F7
    0x6a,  //106 50 F8
    0x6b,  //107 51 F9
    0x6c,  //108 52 F10
    0x70,  //109 53 SHIFT
    0x71,  //110 54 CTRL
    0x72,  //111 55 OPT.1
    0x73,  //112 56 OPT.2
  };
*/
  //  perl misc/itob.pl xeij/Keyboard.java KBD_NORMAL_CODE
  public static final byte[] KBD_NORMAL_CODE = "\2\3\4\5\6\7\b\t\n\13\f\r\16\21\22\23\24\25\26\27\30\31\32\33\34\36\37 !\"#$%&\'()*+,-./01234Z[\\]^_`t\1\17\20\03556789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYabcdefghijklpqrs".getBytes (XEiJ.ISO_8859_1);

  //番号→スキャンコード(NUM ON)
/*
  public static final byte[] KBD_NUM_CODE = {
    //              |1| | | | | | | | | | | | |NUM
    //              | |1| | | | | | | | | | | |CTRL
    //              | | |1| | | | | | | | | | |コード入力
    //              | | | |0|0|0|0|0|0| | | | |OPT.1
    //              | | | |1|1|1|1| | | | | | |かな。ひらがな+全角→ひらがな
    //              | | | | | | | |1|1| | | | |ローマ字
    //              | | | |0|0|1|1| | | | | | |五十音
    //              | | | | | | | | | |0|0|1|1|CAPS
    //              | | | |0|1|0|1|0|1|0|1|0|1|SHIFT
    0x02,  //  0  0 | | |1|ﾇ|ﾇ|ｱ|ｧ|1|!|1|!|1|!|
    0x03,  //  1  1 | | |2|ﾌ|ﾌ|ｲ|ｨ|2|"|2|"|2|"|
    0x04,  //  2  2 | | |3|ｱ|ｧ|ｳ|ｩ|3|#|3|#|3|#|
    0x05,  //  3  3 | | |4|ｳ|ｩ|ｴ|ｪ|4|$|4|$|4|$|
    0x06,  //  4  4 | | |5|ｴ|ｪ|ｵ|ｫ|5|%|5|%|5|%|
    0x07,  //  5  5 | | |6|ｵ|ｫ|ﾅ|ﾅ|6|&|6|&|6|&|
    0x43,  //  6  6 |7| |7|ﾔ|ｬ|ﾆ|ﾆ|7|'|7|'|7|'|
    0x44,  //  7  7 |8| |8|ﾕ|ｭ|ﾇ|ﾇ|8|(|8|(|8|(|
    0x45,  //  8  8 |9| |9|ﾖ|ｮ|ﾈ|ﾈ|9|)|9|)|9|)|
    0x40,  //  9  9 |/| |0|ﾜ|ｦ|ﾉ|ｦ|0| |0| |0| |
    0x41,  // 10 10 |*| | |ﾎ|ﾎ|ﾗ|ﾗ|ｰ|=|-|=|-|=|
    0x0d,  // 11 11 | |^| |ﾍ|ﾍ|ﾘ|ﾘ|^|~|^|~|^|~|
    0x0e,  // 12 12 | |\| |ｰ|ｰ|ﾙ|ﾙ|\|||\|||\|||
    0x11,  // 13 13 | |Q| |ﾀ|ﾀ|ｶ|ｶ|Q|Q|q|Q|Q|q|
    0x12,  // 14 14 | |W| |ﾃ|ﾃ|ｷ|ｷ|W|W|w|W|W|w|
    0x13,  // 15 15 | |E|e|ｲ|ｨ|ｸ|ｸ|E|e|e|E|E|e|
    0x14,  // 16 16 | |R| |ｽ|ｽ|ｹ|ｹ|R|R|r|R|R|r|
    0x15,  // 17 17 | |T| |ｶ|ｶ|ｺ|ｺ|T|T|t|T|T|t|
    0x16,  // 18 18 | |Y| |ﾝ|ﾝ|ﾊ|ﾊ|Y|Y|y|Y|Y|y|
    0x47,  // 19 19 |4|U| |ﾅ|ﾅ|ﾋ|ﾋ|U|u|u|U|U|u|
    0x48,  // 20 20 |5|I| |ﾆ|ﾆ|ﾌ|ﾌ|I|i|i|I|I|i|
    0x49,  // 21 21 |6|O| |ﾗ|ﾗ|ﾍ|ﾍ|O|o|o|O|O|o|
    0x42,  // 22 22 |-|P| |ｾ|ｾ|ﾎ|ﾎ|P|P|p|P|P|p|
    0x1b,  // 23 23 | | | |ﾞ|ﾞ|ﾚ|ﾚ|@|`|@|`|@|`|
    0x1c,  // 24 24 | |[| |ﾟ|｢|ﾛ|｢|｢|{|[|{|[|{|
    0x1e,  // 25 25 | |A|a|ﾁ|ﾁ|ｻ| |A|a|a|A|A|a| かな+五十音+SHIFT→0xa0
    0x1f,  // 26 26 | |S| |ﾄ|ﾄ|ｼ|ｼ|S|S|s|S|S|s|
    0x20,  // 27 27 | |D|d|ｼ|ｼ|ｽ|ｽ|D|D|d|D|D|d|
    0x21,  // 28 28 | |F|f|ﾊ|ﾊ|ｾ|ｾ|F|F|f|F|F|f|
    0x22,  // 29 29 | |G| |ｷ|ｷ|ｿ|ｿ|G|G|g|G|G|g|
    0x23,  // 30 30 | |H| |ｸ|ｸ|ﾏ|ﾏ|H|H|h|H|H|h|
    0x4b,  // 31 31 |1|J| |ﾏ|ﾏ|ﾐ|ﾐ|J|J|j|J|J|j|
    0x4c,  // 32 32 |2|K| |ﾉ|ﾉ|ﾑ|ﾑ|K|K|k|K|K|k|
    0x4d,  // 33 33 |3|L| |ﾘ|ﾘ|ﾒ|ﾒ|L|L|l|L|L|l|
    0x46,  // 34 34 |+| | |ﾚ|ﾚ|ﾓ|ﾓ|;|+|;|+|;|+|
    0x28,  // 35 35 | | | |ｹ|ｹ|ﾞ|ｰ|:|*|:|*|:|*|
    0x29,  // 36 36 | |]| |ﾑ|｣|ﾟ|｣|｣|}|]|}|]|}|
    0x2a,  // 37 37 | |Z| |ﾂ|ｯ|ﾀ|ﾀ|Z|z|z|Z|Z|z|
    0x2b,  // 38 38 | |X| |ｻ|ｻ|ﾁ|ﾁ|X|X|x|X|X|x|
    0x2c,  // 39 39 | |C|c|ｿ|ｿ|ﾂ|ｯ|C|C|c|C|C|c|
    0x2d,  // 40 40 | |V| |ﾋ|ﾋ|ﾃ|ﾃ|V|V|v|V|V|v|
    0x2e,  // 41 41 | |B|b|ｺ|ｺ|ﾄ|ﾄ|B|B|b|B|B|b|
    0x2f,  // 42 42 | |N| |ﾐ|ﾐ|ﾔ|ｬ|N|N|n|N|N|n|
    0x4f,  // 43 43 |0|M| |ﾓ|ﾓ|ﾕ|ｭ|M|M|m|M|M|m|
    0x50,  // 44 44 |,| | |ﾈ|､|ﾖ|ｮ|､|<|,|<|,|<|
    0x51,  // 45 45 |.| | |ﾙ|｡|ﾜ|､|｡|>|.|>|.|>|
    0x4a,  // 46 46 |=| | |ﾒ|･|ｦ|｡|･|?|/|?|/|?|
    0x4e,  // 47 47 |E|_| |ﾛ| |ﾝ|･| |_| |_| |_| NUM→ENTER、かな+SHIFT→0xa0
    0x5a,  // 48  0 かな
    0x5b,  // 49  1 ローマ字
    0x5c,  // 50  2 コード入力
    0x5d,  // 51  3 CAPS
    0x5e,  // 52  4 INS
    0x5f,  // 53  5 ひらがな
    0x60,  // 54  6 全角
    0x74,  // 55  7 NUM
    0x01,  // 56  0 ESC
    0x0f,  // 57  1 BS
    0x10,  // 58  2 TAB
    0x1d,  // 59  3 RETURN
    0x35,  // 60  4 Space
    0x36,  // 61  5 HOME
    0x37,  // 62  6 DEL
    0x38,  // 63  7 ROLLUP
    0x39,  // 64  8 ROLLDOWN
    0x3a,  // 65  9 UNDO
    0x3b,  // 66 10 ←
    0x3c,  // 67 11 ↑
    0x3d,  // 68 12 →
    0x3e,  // 69 13 ↓
    0x3f,  // 70 14 CLR
    0x40,  // 71 15 /
    0x41,  // 72 16 *
    0x42,  // 73 17 -
    0x43,  // 74 18 7
    0x44,  // 75 19 8
    0x45,  // 76 20 9
    0x46,  // 77 21 +
    0x47,  // 78 22 4
    0x48,  // 79 23 5
    0x49,  // 80 24 6
    0x4a,  // 81 25 =
    0x4b,  // 82 26 1
    0x4c,  // 83 27 2
    0x4d,  // 84 28 3
    0x4e,  // 85 29 ENTER
    0x4f,  // 86 30 0
    0x50,  // 87 31 ,
    0x51,  // 88 32 .
    0x52,  // 89 33 記号入力
    0x53,  // 90 34 登録
    0x54,  // 91 35 HELP
    0x55,  // 92 36 XF1
    0x56,  // 93 37 XF2
    0x57,  // 94 38 XF3
    0x58,  // 95 39 XF4
    0x59,  // 96 40 XF5
    0x61,  // 97 41 BREAK
    0x62,  // 98 42 COPY
    0x63,  // 99 43 F1
    0x64,  //100 44 F2
    0x65,  //101 45 F3
    0x66,  //102 46 F4
    0x67,  //103 47 F5
    0x68,  //104 48 F6
    0x69,  //105 49 F7
    0x6a,  //106 50 F8
    0x6b,  //107 51 F9
    0x6c,  //108 52 F10
    0x70,  //109 53 SHIFT
    0x71,  //110 54 CTRL
    0x72,  //111 55 OPT.1
    0x73,  //112 56 OPT.2
  };
*/
  //  perl misc/itob.pl xeij/Keyboard.java KBD_NUM_CODE
  public static final byte[] KBD_NUM_CODE = "\2\3\4\5\6\7CDE@A\r\16\21\22\23\24\25\26GHIB\33\34\36\37 !\"#KLMF()*+,-./OPQJNZ[\\]^_`t\1\17\20\03556789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYabcdefghijklpqrs".getBytes (XEiJ.ISO_8859_1);

  //種類→固定キーの番号→固定キーのスタイル
  //  Standard KeyboardとCompact KeyboardではXFnキーやOPT.nキーの形が異なるので種類毎にスタイルデータを用意する
  public static final int[][] KBD_TYPE_FIX_STYLE = {
    //Standard Keyboard
    {
      KBD_NORMAL_STYLE  ,  //0x01  0 ESC
      KBD_NARROW_STYLE  ,  //0x0f  1 BS
      KBD_NARROW_STYLE  ,  //0x10  2 TAB
      KBD_RETURN_STYLE  ,  //0x1d  3 RETURN
      KBD_NORMAL_STYLE  ,  //0x35  4 Space
      KBD_NORMAL_STYLE  ,  //0x36  5 HOME
      KBD_NORMAL_STYLE  ,  //0x37  6 DEL
      KBD_NORMAL_STYLE  ,  //0x38  7 ROLLUP
      KBD_NORMAL_STYLE  ,  //0x39  8 ROLLDOWN
      KBD_NORMAL_STYLE  ,  //0x3a  9 UNDO
      KBD_SHORT_STYLE   ,  //0x3b 10 ←
      KBD_NORMAL_STYLE  ,  //0x3c 11 ↑
      KBD_SHORT_STYLE   ,  //0x3d 12 →
      KBD_NORMAL_STYLE  ,  //0x3e 13 ↓
      KBD_NORMAL_STYLE  ,  //0x3f 14 CLR
      KBD_NORMAL_STYLE  ,  //0x40 15 /
      KBD_NORMAL_STYLE  ,  //0x41 16 *
      KBD_NORMAL_STYLE  ,  //0x42 17 -
      KBD_NORMAL_STYLE  ,  //0x43 18 7
      KBD_NORMAL_STYLE  ,  //0x44 19 8
      KBD_NORMAL_STYLE  ,  //0x45 20 9
      KBD_NORMAL_STYLE  ,  //0x46 21 +
      KBD_NORMAL_STYLE  ,  //0x47 22 4
      KBD_NORMAL_STYLE  ,  //0x48 23 5
      KBD_NORMAL_STYLE  ,  //0x49 24 6
      KBD_NORMAL_STYLE  ,  //0x4a 25 =
      KBD_NORMAL_STYLE  ,  //0x4b 26 1
      KBD_NORMAL_STYLE  ,  //0x4c 27 2
      KBD_NORMAL_STYLE  ,  //0x4d 28 3
      KBD_SHORTER_STYLE ,  //0x4e 29 ENTER
      KBD_NORMAL_STYLE  ,  //0x4f 30 0
      KBD_NORMAL_STYLE  ,  //0x50 31 ,
      KBD_NORMAL_STYLE  ,  //0x51 32 .
      KBD_NORMAL_STYLE  ,  //0x52 33 記号入力
      KBD_NORMAL_STYLE  ,  //0x53 34 登録
      KBD_NORMAL_STYLE  ,  //0x54 35 HELP
      KBD_NARROWER_STYLE,  //0x55 36 XF1
      KBD_NARROWER_STYLE,  //0x56 37 XF2
      KBD_NARROW_STYLE  ,  //0x57 38 XF3
      KBD_NARROWER_STYLE,  //0x58 39 XF4
      KBD_NARROWER_STYLE,  //0x59 40 XF5
      KBD_NORMAL_STYLE  ,  //0x61 41 BREAK
      KBD_NORMAL_STYLE  ,  //0x62 42 COPY
      KBD_FUNCTION_STYLE,  //0x63 43 F1
      KBD_FUNCTION_STYLE,  //0x64 44 F2
      KBD_FUNCTION_STYLE,  //0x65 45 F3
      KBD_FUNCTION_STYLE,  //0x66 46 F4
      KBD_FUNCTION_STYLE,  //0x67 47 F5
      KBD_FUNCTION_STYLE,  //0x68 48 F6
      KBD_FUNCTION_STYLE,  //0x69 49 F7
      KBD_FUNCTION_STYLE,  //0x6a 50 F8
      KBD_FUNCTION_STYLE,  //0x6b 51 F9
      KBD_FUNCTION_STYLE,  //0x6c 52 F10
      KBD_SHIFT_STYLE   ,  //0x70 53 SHIFT
      KBD_NARROW_STYLE  ,  //0x71 54 CTRL
      KBD_NARROW_STYLE  ,  //0x72 55 OPT.1
      KBD_NARROW_STYLE  ,  //0x73 56 OPT.2
    },
    //Compact Keyboard
    {
      KBD_NORMAL_STYLE  ,  //0x01  0 ESC
      KBD_NARROW_STYLE  ,  //0x0f  1 BS
      KBD_NARROW_STYLE  ,  //0x10  2 TAB
      KBD_RETURN_STYLE  ,  //0x1d  3 RETURN
      KBD_NORMAL_STYLE  ,  //0x35  4 Space
      KBD_NORMAL_STYLE  ,  //0x36  5 HOME
      KBD_NORMAL_STYLE  ,  //0x37  6 DEL
      KBD_NORMAL_STYLE  ,  //0x38  7 ROLLUP
      KBD_NORMAL_STYLE  ,  //0x39  8 ROLLDOWN
      KBD_NORMAL_STYLE  ,  //0x3a  9 UNDO
      KBD_NORMAL_STYLE  ,  //0x3b 10 ←
      KBD_NORMAL_STYLE  ,  //0x3c 11 ↑
      KBD_NORMAL_STYLE  ,  //0x3d 12 →
      KBD_NORMAL_STYLE  ,  //0x3e 13 ↓
      KBD_NORMAL_STYLE  ,  //0x3f 14 CLR
      KBD_NONE_STYLE    ,  //0x40 15 /
      KBD_NONE_STYLE    ,  //0x41 16 *
      KBD_NONE_STYLE    ,  //0x42 17 -
      KBD_NONE_STYLE    ,  //0x43 18 7
      KBD_NONE_STYLE    ,  //0x44 19 8
      KBD_NONE_STYLE    ,  //0x45 20 9
      KBD_NONE_STYLE    ,  //0x46 21 +
      KBD_NONE_STYLE    ,  //0x47 22 4
      KBD_NONE_STYLE    ,  //0x48 23 5
      KBD_NONE_STYLE    ,  //0x49 24 6
      KBD_NONE_STYLE    ,  //0x4a 25 =
      KBD_NONE_STYLE    ,  //0x4b 26 1
      KBD_NONE_STYLE    ,  //0x4c 27 2
      KBD_NONE_STYLE    ,  //0x4d 28 3
      KBD_NONE_STYLE    ,  //0x4e 29 ENTER
      KBD_NONE_STYLE    ,  //0x4f 30 0
      KBD_NONE_STYLE    ,  //0x50 31 ,
      KBD_NONE_STYLE    ,  //0x51 32 .
      KBD_NORMAL_STYLE  ,  //0x52 33 記号入力
      KBD_NORMAL_STYLE  ,  //0x53 34 登録
      KBD_NORMAL_STYLE  ,  //0x54 35 HELP
      KBD_NARROWER_STYLE,  //0x55 36 XF1
      KBD_NARROWER_STYLE,  //0x56 37 XF2
      KBD_NARROWER_STYLE,  //0x57 38 XF3
      KBD_NORMAL_STYLE  ,  //0x58 39 XF4
      KBD_NORMAL_STYLE  ,  //0x59 40 XF5
      KBD_NORMAL_STYLE  ,  //0x61 41 BREAK
      KBD_NORMAL_STYLE  ,  //0x62 42 COPY
      KBD_FUNCTION_STYLE,  //0x63 43 F1
      KBD_FUNCTION_STYLE,  //0x64 44 F2
      KBD_FUNCTION_STYLE,  //0x65 45 F3
      KBD_FUNCTION_STYLE,  //0x66 46 F4
      KBD_FUNCTION_STYLE,  //0x67 47 F5
      KBD_FUNCTION_STYLE,  //0x68 48 F6
      KBD_FUNCTION_STYLE,  //0x69 49 F7
      KBD_FUNCTION_STYLE,  //0x6a 50 F8
      KBD_FUNCTION_STYLE,  //0x6b 51 F9
      KBD_FUNCTION_STYLE,  //0x6c 52 F10
      KBD_SHIFT_STYLE   ,  //0x70 53 SHIFT
      KBD_NARROW_STYLE  ,  //0x71 54 CTRL
      KBD_NORMAL_STYLE  ,  //0x72 55 OPT.1
      KBD_NORMAL_STYLE  ,  //0x73 56 OPT.2
    },
  };  //KBD_TYPE_FIX_STYLE

  //赤色LEDの色
  //  5A:かな,5B:ローマ字,5C:コード入力,5D:CAPS,5E:INS,74:NUM
  public static final int[][] KBD_RED_LED_COLOR = {
    { KBD_RED_OFF_KEYTOP_COLOR, KBD_RED_OFF_BORDER_COLOR },    //000xx 消灯
    { KBD_RED_ON_KEYTOP_COLOR_0, KBD_RED_ON_BORDER_COLOR_0 },  //001xx 明るい
    { KBD_RED_OFF_KEYTOP_COLOR, KBD_RED_OFF_BORDER_COLOR },    //010xx
    { KBD_RED_ON_KEYTOP_COLOR_1, KBD_RED_ON_BORDER_COLOR_1 },  //011xx やや明るい
    { KBD_RED_OFF_KEYTOP_COLOR, KBD_RED_OFF_BORDER_COLOR },    //100xx
    { KBD_RED_ON_KEYTOP_COLOR_2, KBD_RED_ON_BORDER_COLOR_2 },  //101xx やや暗い
    { KBD_RED_OFF_KEYTOP_COLOR, KBD_RED_OFF_BORDER_COLOR },    //110xx
    { KBD_RED_ON_KEYTOP_COLOR_3, KBD_RED_ON_BORDER_COLOR_3 },  //111xx 暗い
  };

  //緑色LEDの色
  //  5F:ひらがな,60:全角
  public static final int[][] KBD_GREEN_LED_COLOR = {
    { KBD_GREEN_OFF_KEYTOP_COLOR, KBD_GREEN_OFF_BORDER_COLOR },    //000xx 消灯
    { KBD_GREEN_ON_KEYTOP_COLOR_0, KBD_GREEN_ON_BORDER_COLOR_0 },  //001xx 明るい
    { KBD_GREEN_OFF_KEYTOP_COLOR, KBD_GREEN_OFF_BORDER_COLOR },    //010xx
    { KBD_GREEN_ON_KEYTOP_COLOR_1, KBD_GREEN_ON_BORDER_COLOR_1 },  //011xx やや明るい
    { KBD_GREEN_OFF_KEYTOP_COLOR, KBD_GREEN_OFF_BORDER_COLOR },    //100xx
    { KBD_GREEN_ON_KEYTOP_COLOR_2, KBD_GREEN_ON_BORDER_COLOR_2 },  //101xx やや暗い
    { KBD_GREEN_OFF_KEYTOP_COLOR, KBD_GREEN_OFF_BORDER_COLOR },    //110xx
    { KBD_GREEN_ON_KEYTOP_COLOR_3, KBD_GREEN_ON_BORDER_COLOR_3 },  //111xx 暗い
  };

  //LEDキーの番号→LEDの色
  public static final int[][][] KBD_LED_COLOR = {
    KBD_RED_LED_COLOR  ,  //0x5a  0 かな
    KBD_RED_LED_COLOR  ,  //0x5b  1 ローマ字
    KBD_RED_LED_COLOR  ,  //0x5c  2 コード入力
    KBD_RED_LED_COLOR  ,  //0x5d  3 CAPS
    KBD_RED_LED_COLOR  ,  //0x5e  4 INS
    KBD_GREEN_LED_COLOR,  //0x5f  5 ひらがな
    KBD_GREEN_LED_COLOR,  //0x60  6 全角
    KBD_RED_LED_COLOR  ,  //0x74  7 NUM
  };

  //Standard Keyboard
  //                       1 1 1 1 1 1 1 1 1 1 2 2 2 2 2 2 2 2 2 2 3 3 3 3 3 3 3 3 3 3 4 4 4 4 4 4 4 4 4 4 5 5 5 5 5 5 5 5 5 5 6 6 6 6 6 6 6 6 6 6 7 7 7 7 7 7 7 7 7 7 8 8 8 8 8 8 8 8 8 8 9 9 9 9 9
  //   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4
  // 0 +-------+ +-------+ . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . +-------+-------+-------+ . +-------+-------+-------+-------+  0
  // 1 |61     | |62     | . +---------+---------+---------+---------+---------+ +---------+---------+---------+---------+---------+ . |5A     |5B     |5C     | . |5D     |52     |53     |54     |  1
  // 2 | BREAK | |  COPY | . |63       |64       |65       |66       |67       | |68       |69       |6A       |6B       |6C       | . | KANA  | ROMA  | CODE  | . | CAPS  | KIGOU |TOUROKU| HELP  |  2
  // 3 |       | |       | . |   F1    |   F2    |   F3    |   F4    |   F5    | |   F6    |   F7    |   F8    |   F9    |  F10    | . |  rrr  |  rrr  |  rrr  | . |  rrr  |       |       |       |  3
  // 4 +-------+ +-------+ . +---------+---------+---------+---------+---------+ +---------+---------+---------+---------+---------+ . +--rrr--+--rrr--+--rrr--+ . +--rrr--+-------+-------+-------+  4
  // 5 +-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+-----------+ . +-------+-------+-------+ . +-------+-------+-------+-------+  5
  // 6 |01     |02     |03     |04     |05     |06     |07     |08     |09     |0A     |0B     |0C     |0D     |0E     | !0F     ! | . |36     |5E     |37     | . |3F     |40     |41     |42     |  6
  // 7 |  ESC  |  1!   |  2"   |  3#   |  4$   |  5%   |  6&   |  7'   |  8(   |  9)   |   0   |  -=   |  ^~   |  \|   | !  BS   ! | . | HOME  |  INS  |  DEL  | . |  CLR  |   /   |   *   |   -   |  7
  // 8 |       |       |       |       |       |       |       |       |       |       |       |       |       |       | !       ! | . |       |  rrr  |       | . |       | ~~~~~ | ~~~~~ | ~~~~~ |  8
  // 9 +-------+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+-----------+ . +-------+--rrr--+-------+ . +-------+-------+-------+-------+  9
  //10 | !10     ! |11     |12     |13     |14     |15     |16     |17     |18     |19     |1A     |1B     |1C     | !1D         ! | . |38     |39     |3A     | . |43     |44     |45     |46     | 10
  //11 | !  TAB  ! |   Q   |   W   |   E   |   R   |   T   |   Y   |   U   |   I   |   O   |   P   |  @`   |  [{   | !           ! | . | R-UP  |R-DOWN | UNDO  | . |   7   |   8   |   9   |   +   | 11
  //12 | !       ! |       |       |       |       |       |       |       |       |       |       |       |       | !           ! | . |       |       |       | . | ~~~~~ | ~~~~~ | ~~~~~ | ~~~~~ | 12
  //13 +-----------+-+-----+-+-----+-+-----+-+-----+-+-----+-+-----+-+-----+-+-----+-+-----+-+-----+-+-----+-+-----+-+ ! RETURN  ! | . +-------+-------+-------+ . +-------+-------+-------+-------+ 13
  //14 | !71       ! |1E     |1F     |20     |21     |22     |23     |24     |25     |26     |27     |28     |29     | !         ! | . |       |3C     |       | . |47     |48     |49     |4A     | 14
  //15 | !  CTRL   ! |   A   |   S   |   D   |   F   |   G   |   H   |   J   |   K   |   L   |  ;+   |  :*   |  ]}   | !         ! | . | - - - |  UP   | - - - | . |   4   |   5   |   6   |   =   | 15
  //16 | !         ! |       |       |       |       |       |       |       |       |       |       |       |       | !         ! | . | 3B    |       |3D     | . | ~~~~~ | ~~~~~ | ~~~~~ | ~~~~~ | 16
  //17 +-------------+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+-------------+ . | LEFT  +-------+ RIGHT | . +-------+-------+-------+-------+ 17
  //18 | !70           ! |2A     |2B     |2C     |2D     |2E     |2F     |30     |31     |32     |33     |34     | !70           ! | . |       |3E     |       | . |4B     |4C     |4D     | - - - | 18
  //19 | !    SHIFT    ! |   Z   |   X   |   C   |   V   |   B   |   M   |   N   |  ,<   |  .>   |  /?   |   _   | !    SHIFT    ! | . | - - - | DOWN  | - - - | . |   1   |   2   |   3   |4E     | 19
  //20 | !             ! |       |       |       |       |       |       |       |       |       |       |       | !             ! | . |       |       |       | . | ~~~~~ | ~~~~~ | ~~~~~ |       | 20
  //21 +-------------+---+---+---+-----+-+-------+-------+-------+-------+---+---+-------+-------+-+-----+---+---+---+-------------+ . +-------+---+---+-------+ . +-------+-------+-------+ ENTER | 21
  //22 . . . . . . . |5F     |!55     !|!56     !|35                         | !57     ! |!58     !|!59     !|60     | . . . . . . . . | !72     ! | !73     ! | . |4F     |50     |51     | ~~~~~ | 22
  //23 . . . . . . . | HIRA  |!  XF1  !|!  XF2  !|           SPACE           | !  XF3  ! |!  XF4  !|!  XF5  !|ZENKAKU| . . . . . . . . | ! OPT.1 ! | ! OPT.2 ! | . |   0   |   ,   |   .   |       | 23
  //24 . . . . . . . |  ggg  |!       !|!       !|                           | !       ! |!       !|!       !|  ggg  | . . . . . . . . | !       ! | !       ! | . | ~~~~~ | ~~~~~ | ~~~~~ | - - - | 24
  //25 . . . . . . . +--ggg--+---------+---------+---------------------------+-----------+---------+---------+--ggg--+ . . . . . . . . +-----------+-----------+ . +-------+-------+-------+-------+ 25
  //                       1 1 1 1 1 1 1 1 1 1 2 2 2 2 2 2 2 2 2 2 3 3 3 3 3 3 3 3 3 3 4 4 4 4 4 4 4 4 4 4 5 5 5 5 5 5 5 5 5 5 6 6 6 6 6 6 6 6 6 6 7 7 7 7 7 7 7 7 7 7 8 8 8 8 8 8 8 8 8 8 9 9 9 9 9
  //   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4

  //Compact Keyboard
  //                       1 1 1 1 1 1 1 1 1 1 2 2 2 2 2 2 2 2 2 2 3 3 3 3 3 3 3 3 3 3 4 4 4 4 4 4 4 4 4 4 5 5 5 5 5 5 5 5 5 5 6 6 6 6 6 6 6 6 6 6 7 7 7 7 7 7 7
  //   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6
  // 0 +-------+ +-------+ . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . +-------+-------+-------+  0
  // 1 |61     | |62     | . +---------+---------+---------+---------+---------+ +---------+---------+---------+---------+---------+ . |52     |53     |54     |  1
  // 2 | BREAK | |  COPY | . |63       |64       |65       |66       |67       | |68       |69       |6A       |6B       |6C       | . | KIGOU |TOUROKU| HELP  |  2
  // 3 |       | |       | . |   F1    |   F2    |   F3    |   F4    |   F5    | |   F6    |   F7    |   F8    |   F9    |  F10    | . |       |       |       |  3
  // 4 +-------+ +-------+ . +---------+---------+---------+---------+---------+ +---------+---------+---------+---------+---------+ . +-------+-------+-------+  4
  // 5 +-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+-----------+ . |60     |5F     |5C     |  5
  // 6 |01     |02     |03     |04     |05     |06     |07     |08  /  |09  /  |0A  /  |0B  /  |0C  /  |0D     |0E     | !0F     ! | . |ZENKAKU| HIRA  | CODE  |  6
  // 7 |  ESC  |  1!   |  2"   |  3#   |  4$   |  5%   |  6&   | 7'/ 7 | 8(/ 8 | 9)/ 9 | 0 / / | -=/ * |  ^~   |  \|   | !  BS   ! | . |  ggg  |  ggg  |  rrr  |  7
  // 8 |       |       |       |       |       |       |       |  /  43|  /  44|  /  45|  /  40|  /  41|       |       | !       ! | . +--ggg--+--ggg--+--rrr--+  8
  // 9 +-------+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+-----------+ . +-------+--rrr--+-------+  9
  //10 | !10     ! |11     |12     |13     |14     |15     |16     |17  /  |18  /  |19  /  |1A  /  |1B     |1C     | !1D         ! | . |36     |3F     |37     | 10
  //11 | !  TAB  ! |   Q   |   W   |   E   |   R   |   T   |   Y   | U / 4 | I / 5 | O / 6 | P / - |  @`   |  [{   | !           ! | . | HOME  |  CLR  |  DEL  | 11
  //12 | !       ! |       |       |       |       |       |       |  /  47|  /  48|  /  49|  /  42|       |       | !           ! | . |       |       |       | 12
  //13 +-----------+-+-----+-+-----+-+-----+-+-----+-+-----+-+-----+-+-----+-+-----+-+-----+-+-----+-+-----+-+-----+-+ ! RETURN  ! | . +-------+-------+-------+ 13
  //14 | !71       ! |1E     |1F     |20     |21     |22     |23     |24  /  |25  /  |26  /  |27  /  |28     |29     | !         ! | . |74     |5E     |3A     | 14
  //15 | !  CTRL   ! |   A   |   S   |   D   |   F   |   G   |   H   | J / 1 | K / 2 | L / 3 | ;+/ + |  :*   |  ]}   | !         ! | . |  NUM  |  INS  | UNDO  | 15
  //16 | !         ! |       |       |       |       |       |       |  /  4B|  /  4C|  /  4D|  /  46|       |       | !         ! | . |  rrr  |  rrr  |       | 16
  //17 +-------------+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+-------------+ . +--rrr--+--rrr--+-------+ 17
  //18 | !70           ! |2A     |2B     |2C     |2D     |2E     |2F     |30  /  |31  /  |32  /  |33  /  |34  /  | !70           ! | . |38     |3C     |39     | 18
  //19 | !    SHIFT    ! |   Z   |   X   |   C   |   V   |   B   |   M   | N / 0 | ,</ , | .>/ . | /?/ = | _ /ENT| !    SHIFT    ! | . | R-UP  |  UP   |R-DOWN | 19
  //20 | !             ! |       |       |       |       |       |       |  /  4F|  /  50|  /  51|  /  4A|  /  4E| !             ! | . |       |       |       | 20
  //21 +-----+-------+---+---+---+-----+-+-------+-------+-------+-------+---+---+-----+-+-----+-+-----+-+-----+-+-----+-------+---+ . +-------+-------+-------+ 21
  //22 . . . |72     |5D     |!55     !|!56     !|35                         |! 57    !|58     |59     |5B     |5A     |73     | . . . |3B     |3E     |3D     | 22
  //23 . . . | OPT.1 | CAPS  |!  XF1  !|!  XF2  !|           SPACE           |!  XF3  !|  XF4  |  XF5  | ROMA  | KANA  | OPT.2 | . . . | LEFT  | DOWN  | RIGHT | 23
  //24 . . . |       |  rrr  |!       !|!       !|                           |!       !|       |       |  rrr  |  rrr  |       | . . . |       |       |       | 24
  //25 . . . +-------+--rrr--+---------+---------+---------------------------+---------+-------+-------+--rrr--+--rrr--+-------+ . . . +-------+-------+-------+ 25
  //                       1 1 1 1 1 1 1 1 1 1 2 2 2 2 2 2 2 2 2 2 3 3 3 3 3 3 3 3 3 3 4 4 4 4 4 4 4 4 4 4 5 5 5 5 5 5 5 5 5 5 6 6 6 6 6 6 6 6 6 6 7 7 7 7 7 7 7
  //   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6

  //可変キーの番号→ボックス
  //  Standard KeyboardとCompact Keyboardで共通
  //  欠番なし
  public static final int[][] KBD_VAR_BOX = {
    {  4,  5,  4, 4 },  //0x02  0 1
    {  8,  5,  4, 4 },  //0x03  1 2
    { 12,  5,  4, 4 },  //0x04  2 3
    { 16,  5,  4, 4 },  //0x05  3 4
    { 20,  5,  4, 4 },  //0x06  4 5
    { 24,  5,  4, 4 },  //0x07  5 6
    { 28,  5,  4, 4 },  //0x08  6 7
    { 32,  5,  4, 4 },  //0x09  7 8
    { 36,  5,  4, 4 },  //0x0a  8 9
    { 40,  5,  4, 4 },  //0x0b  9 0
    { 44,  5,  4, 4 },  //0x0c 10 -
    { 48,  5,  4, 4 },  //0x0d 11 ^
    { 52,  5,  4, 4 },  //0x0e 12 \
    {  6,  9,  4, 4 },  //0x11 13 q
    { 10,  9,  4, 4 },  //0x12 14 w
    { 14,  9,  4, 4 },  //0x13 15 e
    { 18,  9,  4, 4 },  //0x14 16 r
    { 22,  9,  4, 4 },  //0x15 17 t
    { 26,  9,  4, 4 },  //0x16 18 y
    { 30,  9,  4, 4 },  //0x17 19 u
    { 34,  9,  4, 4 },  //0x18 20 i
    { 38,  9,  4, 4 },  //0x19 21 o
    { 42,  9,  4, 4 },  //0x1a 22 p
    { 46,  9,  4, 4 },  //0x1b 23 @
    { 50,  9,  4, 4 },  //0x1c 24 [
    {  7, 13,  4, 4 },  //0x1e 25 a
    { 11, 13,  4, 4 },  //0x1f 26 s
    { 15, 13,  4, 4 },  //0x20 27 d
    { 19, 13,  4, 4 },  //0x21 28 f
    { 23, 13,  4, 4 },  //0x22 29 g
    { 27, 13,  4, 4 },  //0x23 30 h
    { 31, 13,  4, 4 },  //0x24 31 j
    { 35, 13,  4, 4 },  //0x25 32 k
    { 39, 13,  4, 4 },  //0x26 33 l
    { 43, 13,  4, 4 },  //0x27 34 ;
    { 47, 13,  4, 4 },  //0x28 35 :
    { 51, 13,  4, 4 },  //0x29 36 ]
    {  9, 17,  4, 4 },  //0x2a 37 z
    { 13, 17,  4, 4 },  //0x2b 38 x
    { 17, 17,  4, 4 },  //0x2c 39 c
    { 21, 17,  4, 4 },  //0x2d 40 v
    { 25, 17,  4, 4 },  //0x2e 41 b
    { 29, 17,  4, 4 },  //0x2f 42 n
    { 33, 17,  4, 4 },  //0x30 43 m
    { 37, 17,  4, 4 },  //0x31 44 ,
    { 41, 17,  4, 4 },  //0x32 45 .
    { 45, 17,  4, 4 },  //0x33 46 /
    { 49, 17,  4, 4 },  //0x34 47
  };  //KBD_VAR_BOX

  //種類→LEDキーの番号→ボックス
  //  欠番あり
  //    Standard KeyboardのNUMキー
  public static final int[][][] KBD_TYPE_LED_BOX = {
    //Standard Keyboard
    {
      { 64,  0,  4, 4 },  //0x5a  0 かな
      { 68,  0,  4, 4 },  //0x5b  1 ローマ字
      { 72,  0,  4, 4 },  //0x5c  2 コード入力
      { 78,  0,  4, 4 },  //0x5d  3 CAPS
      { 68,  5,  4, 4 },  //0x5e  4 INS
      {  7, 21,  4, 4 },  //0x5f  5 ひらがな
      { 51, 21,  4, 4 },  //0x60  6 全角
      null             ,  //0x74  7 NUM
    },
    //Compact Keyboard
    {
      { 52, 21,  4, 4 },  //0x5a  0 かな
      { 48, 21,  4, 4 },  //0x5b  1 ローマ字
      { 72,  4,  4, 4 },  //0x5c  2 コード入力
      {  7, 21,  4, 4 },  //0x5d  3 CAPS
      { 68, 13,  4, 4 },  //0x5e  4 INS
      { 68,  4,  4, 4 },  //0x5f  5 ひらがな
      { 64,  4,  4, 4 },  //0x60  6 全角
      { 64, 13,  4, 4 },  //0x74  7 NUM
    },
  };  //KBD_TYPE_LED_BOX

  //種類→固定キーのインデックス→ボックス
  //  欠番あり
  //    Compact Keyboardのテンキー
  public static final int[][][] KBD_TYPE_FIX_BOX = {
    //Standard Keyboard
    {
      {  0,  5,  4, 4 },  //0x01  0 ESC
      { 56,  5,  6, 4 },  //0x0f  1 BS
      {  0,  9,  6, 4 },  //0x10  2 TAB
      { 54,  9,  8, 8 },  //0x1d  3 RETURN
      { 21, 21, 14, 4 },  //0x35  4 Space
      { 64,  5,  4, 4 },  //0x36  5 HOME
      { 72,  5,  4, 4 },  //0x37  6 DEL
      { 64,  9,  4, 4 },  //0x38  7 ROLLUP
      { 68,  9,  4, 4 },  //0x39  8 ROLLDOWN
      { 72,  9,  4, 4 },  //0x3a  9 UNDO
      { 64, 13,  4, 8 },  //0x3b 10 ←
      { 68, 13,  4, 4 },  //0x3c 11 ↑
      { 72, 13,  4, 8 },  //0x3d 12 →
      { 68, 17,  4, 4 },  //0x3e 13 ↓
      { 78,  5,  4, 4 },  //0x3f 14 CLR
      { 82,  5,  4, 4 },  //0x40 15 /
      { 86,  5,  4, 4 },  //0x41 16 *
      { 90,  5,  4, 4 },  //0x42 17 -
      { 78,  9,  4, 4 },  //0x43 18 7
      { 82,  9,  4, 4 },  //0x44 19 8
      { 86,  9,  4, 4 },  //0x45 20 9
      { 90,  9,  4, 4 },  //0x46 21 +
      { 78, 13,  4, 4 },  //0x47 22 4
      { 82, 13,  4, 4 },  //0x48 23 5
      { 86, 13,  4, 4 },  //0x49 24 6
      { 90, 13,  4, 4 },  //0x4a 25 =
      { 78, 17,  4, 4 },  //0x4b 26 1
      { 82, 17,  4, 4 },  //0x4c 27 2
      { 86, 17,  4, 4 },  //0x4d 28 3
      { 90, 17,  4, 8 },  //0x4e 29 ENTER
      { 78, 21,  4, 4 },  //0x4f 30 0
      { 82, 21,  4, 4 },  //0x50 31 ,
      { 86, 21,  4, 4 },  //0x51 32 .
      { 82,  0,  4, 4 },  //0x52 33 記号入力
      { 86,  0,  4, 4 },  //0x53 34 登録
      { 90,  0,  4, 4 },  //0x54 35 HELP
      { 11, 21,  5, 4 },  //0x55 36 XF1
      { 16, 21,  5, 4 },  //0x56 37 XF2
      { 35, 21,  6, 4 },  //0x57 38 XF3
      { 41, 21,  5, 4 },  //0x58 39 XF4
      { 46, 21,  5, 4 },  //0x59 40 XF5
      {  0,  0,  4, 4 },  //0x61 41 BREAK
      {  5,  0,  4, 4 },  //0x62 42 COPY
      { 11,  0,  5, 4 },  //0x63 43 F1
      { 16,  0,  5, 4 },  //0x64 44 F2
      { 21,  0,  5, 4 },  //0x65 45 F3
      { 26,  0,  5, 4 },  //0x66 46 F4
      { 31,  0,  5, 4 },  //0x67 47 F5
      { 37,  0,  5, 4 },  //0x68 48 F6
      { 42,  0,  5, 4 },  //0x69 49 F7
      { 47,  0,  5, 4 },  //0x6a 50 F8
      { 52,  0,  5, 4 },  //0x6b 51 F9
      { 57,  0,  5, 4 },  //0x6c 52 F10
      {  0, 17, 62, 4 },  //0x70 53 SHIFT
      {  0, 13,  7, 4 },  //0x71 54 CTRL
      { 64, 21,  6, 4 },  //0x72 55 OPT.1
      { 70, 21,  6, 4 },  //0x73 56 OPT.2
    },
    //Compact Keyboard
    {
      {  0,  5,  4, 4 },  //0x01  0 ESC
      { 56,  5,  6, 4 },  //0x0f  1 BS
      {  0,  9,  6, 4 },  //0x10  2 TAB
      { 54,  9,  8, 8 },  //0x1d  3 RETURN
      { 21, 21, 14, 4 },  //0x35  4 Space
      { 64,  9,  4, 4 },  //0x36  5 HOME
      { 72,  9,  4, 4 },  //0x37  6 DEL
      { 64, 17,  4, 4 },  //0x38  7 ROLLUP
      { 72, 17,  4, 4 },  //0x39  8 ROLLDOWN
      { 72, 13,  4, 4 },  //0x3a  9 UNDO
      { 64, 21,  4, 4 },  //0x3b 10 ←
      { 68, 17,  4, 4 },  //0x3c 11 ↑
      { 72, 21,  4, 4 },  //0x3d 12 →
      { 68, 21,  4, 4 },  //0x3e 13 ↓
      { 68,  9,  4, 4 },  //0x3f 14 CLR
      null             ,  //0x40 15 /
      null             ,  //0x41 16 *
      null             ,  //0x42 17 -
      null             ,  //0x43 18 7
      null             ,  //0x44 19 8
      null             ,  //0x45 20 9
      null             ,  //0x46 21 +
      null             ,  //0x47 22 4
      null             ,  //0x48 23 5
      null             ,  //0x49 24 6
      null             ,  //0x4a 25 =
      null             ,  //0x4b 26 1
      null             ,  //0x4c 27 2
      null             ,  //0x4d 28 3
      null             ,  //0x4e 29 ENTER
      null             ,  //0x4f 30 0
      null             ,  //0x50 31 ,
      null             ,  //0x51 32 .
      { 64,  0,  4, 4 },  //0x52 33 記号入力
      { 68,  0,  4, 4 },  //0x53 34 登録
      { 72,  0,  4, 4 },  //0x54 35 HELP
      { 11, 21,  5, 4 },  //0x55 36 XF1
      { 16, 21,  5, 4 },  //0x56 37 XF2
      { 35, 21,  5, 4 },  //0x57 38 XF3
      { 40, 21,  4, 4 },  //0x58 39 XF4
      { 44, 21,  4, 4 },  //0x59 40 XF5
      {  0,  0,  4, 4 },  //0x61 41 BREAK
      {  5,  0,  4, 4 },  //0x62 42 COPY
      { 11,  0,  5, 4 },  //0x63 43 F1
      { 16,  0,  5, 4 },  //0x64 44 F2
      { 21,  0,  5, 4 },  //0x65 45 F3
      { 26,  0,  5, 4 },  //0x66 46 F4
      { 31,  0,  5, 4 },  //0x67 47 F5
      { 37,  0,  5, 4 },  //0x68 48 F6
      { 42,  0,  5, 4 },  //0x69 49 F7
      { 47,  0,  5, 4 },  //0x6a 50 F8
      { 52,  0,  5, 4 },  //0x6b 51 F9
      { 57,  0,  5, 4 },  //0x6c 52 F10
      {  0, 17, 62, 4 },  //0x70 53 SHIFT
      {  0, 13,  7, 4 },  //0x71 54 CTRL
      {  3, 21,  4, 4 },  //0x72 55 OPT.1
      { 56, 21,  4, 4 },  //0x73 56 OPT.2
    },
  };  //KBD_TYPE_FIX_BOX

  //可変キーの番号→可変キーに表示する文字の文字コード
  //  可変キーに表示する文字はモードキーの状態によって変化する
  //  かなの配列はSRAMの設定によって変化する
  //    0x00ed002b  キーボードのかなの配列。0=旧JIS,0以外=五十音
  //  文字コード0x5c,0x7e,0x7cの字体はSRAMの設定によって変化する
  //    0x00ed0059  bit0  文字コード0x5cの字体。0=YEN SIGN(0x5c,U+00A5),1=REVERSE SOLIDUS(0x80,U+005C)
  //                bit1  文字コード0x7eの字体。0=MACRON(0x7e,U+00AF),1=TILDE(0x81,U+007E)
  //                bit2  文字コード0x7cの字体。0=VERTICAL LINE(0x7c,U+007C),1=BROKEN BAR(0x82,U+00A6)
  //  文字の字体は半角と全角を区別しない
  //  文字の字体はカタカナとひらがなを区別する。ひらがな+全角のときだけひらがなにする
  //  テレビコントロール(SHIFT|OPT.2+←|↑|→|↓|CLR|/|*|-|7|8|9|+|4|5|6|=|1|2|3|0|,|.)は区別しない
  public static final char[] KBD_CAPS_BASE           = "1234567890-^\\QWERTYUIOP@[ASDFGHJKL;:]ZXCVBNM,./\u0000".toCharArray ();
  public static final char[] KBD_CAPS_SHIFT_BASE     = "!\"#$%&'()\u0000=~|qwertyuiop`{asdfghjkl+*}zxcvbnm<>?_".toCharArray ();
  public static final char[] KBD_KANA_BASE           = "ﾇﾌｱｳｴｵﾔﾕﾖﾜﾎﾍｰﾀﾃｲｽｶﾝﾅﾆﾗｾﾞﾟﾁﾄｼﾊｷｸﾏﾉﾘﾚｹﾑﾂｻｿﾋｺﾐﾓﾈﾙﾒﾛ".toCharArray ();
  public static final char[] KBD_KANA_SHIFT_BASE     = "ﾇﾌｧｩｪｫｬｭｮｦﾎﾍｰﾀﾃｨｽｶﾝﾅﾆﾗｾﾞ｢ﾁﾄｼﾊｷｸﾏﾉﾘﾚｹ｣ｯｻｿﾋｺﾐﾓ､｡･\u00a0".toCharArray ();
  public static final char[] KBD_KANA_AIU_BASE       = "ｱｲｳｴｵﾅﾆﾇﾈﾉﾗﾘﾙｶｷｸｹｺﾊﾋﾌﾍﾎﾚﾛｻｼｽｾｿﾏﾐﾑﾒﾓﾞﾟﾀﾁﾂﾃﾄﾔﾕﾖﾜｦﾝ".toCharArray ();
  public static final char[] KBD_KANA_AIU_SHIFT_BASE = "ｧｨｩｪｫﾅﾆﾇﾈｦﾗﾘﾙｶｷｸｹｺﾊﾋﾌﾍﾎﾚ｢\u00a0ｼｽｾｿﾏﾐﾑﾒﾓｰ｣ﾀﾁｯﾃﾄｬｭｮ､｡･".toCharArray ();
  public static final char[] KBD_ROMA_BASE           = "1234567890ｰ^\\QWERTYUIOP@｢ASDFGHJKL;:｣ZXCVBNM､｡･\u0000".toCharArray ();
  public static final char[] KBD_ROMA_SHIFT_BASE     = "!\"#$%&'()\u0000=~|QWeRTYuioP`{aSDFGHJKL+*}zXCVBNM<>?_".toCharArray ();
  public static final char[] KBD_NUM_BASE            = ("      789/" + "*        456-        123+        0,.=\u0084").toCharArray ();
  public static final byte[] KBD_CAPS_CHR = new byte[KBD_VAR_KEYS];
  public static final byte[] KBD_NORMAL_CHR = new byte[KBD_VAR_KEYS];
  public static final byte[] KBD_CODE_CHR = new byte[KBD_VAR_KEYS];
  public static final byte[] KBD_CAPS_SHIFT_CHR = new byte[KBD_VAR_KEYS];
  public static final byte[] KBD_SHIFT_CHR = new byte[KBD_VAR_KEYS];
  public static final byte[] KBD_CTRL_CHR = new byte[KBD_VAR_KEYS];
  public static final byte[] KBD_KANA_CHR = new byte[KBD_VAR_KEYS];
  public static final byte[] KBD_KANA_SHIFT_CHR = new byte[KBD_VAR_KEYS];
  public static final byte[] KBD_KANA_AIU_CHR = new byte[KBD_VAR_KEYS];
  public static final byte[] KBD_KANA_AIU_SHIFT_CHR = new byte[KBD_VAR_KEYS];
  public static final byte[] KBD_ROMA_CHR = new byte[KBD_VAR_KEYS];
  public static final byte[] KBD_ROMA_SHIFT_CHR = new byte[KBD_VAR_KEYS];
  public static final byte[] KBD_NUM_CAPS_CHR = new byte[KBD_VAR_KEYS];
  public static final byte[] KBD_NUM_CHR = new byte[KBD_VAR_KEYS];
  public static final byte[] KBD_NUM_CODE_CHR = new byte[KBD_VAR_KEYS];
  public static final byte[] KBD_NUM_CAPS_SHIFT_CHR = new byte[KBD_VAR_KEYS];
  public static final byte[] KBD_NUM_SHIFT_CHR = new byte[KBD_VAR_KEYS];
  public static final byte[] KBD_NUM_CTRL_CHR = new byte[KBD_VAR_KEYS];
  public static final byte[] KBD_NUM_KANA_CHR = new byte[KBD_VAR_KEYS];
  public static final byte[] KBD_NUM_KANA_SHIFT_CHR = new byte[KBD_VAR_KEYS];
  public static final byte[] KBD_NUM_KANA_AIU_CHR = new byte[KBD_VAR_KEYS];
  public static final byte[] KBD_NUM_KANA_AIU_SHIFT_CHR = new byte[KBD_VAR_KEYS];
  public static final byte[] KBD_NUM_ROMA_CHR = new byte[KBD_VAR_KEYS];
  public static final byte[] KBD_NUM_ROMA_SHIFT_CHR = new byte[KBD_VAR_KEYS];

  //文字コード→可変キーに表示する文字のパターン
  //
  //       +0 +1 +2 +3 +4 +5 +6 +7 +8 +9 +a +b +c +d +e +f
  //  0x00 　 ^A ^B ^C ^D ^E ^F ^G ^H ^I ^J ^K ^L ^M ^N ^O
  //  0x10 ^P ^Q ^R ^S ^T ^U ^V ^W ^X ^Y ^Z ^[ ^¥ ^] ^^ ^_
  //  0x20 　 ！ ” ＃ ＄ ％ ＆ ’ （ ） ＊ ＋ ， － ． ／
  //  0x30 ０ １ ２ ３ ４ ５ ６ ７ ８ ９ ： ； ＜ ＝ ＞ ？
  //  0x40 ＠ Ａ Ｂ Ｃ Ｄ Ｅ Ｆ Ｇ Ｈ Ｉ Ｊ Ｋ Ｌ Ｍ Ｎ Ｏ
  //  0x50 Ｐ Ｑ Ｒ Ｓ Ｔ Ｕ Ｖ Ｗ Ｘ Ｙ Ｚ ［ ￥ ］ ＾ ＿
  //  0x60 ｀ ａ ｂ ｃ ｄ ｅ ｆ ｇ ｈ ｉ ｊ ｋ ｌ ｍ ｎ ｏ
  //  0x70 ｐ ｑ ｒ ｓ ｔ ｕ ｖ ｗ ｘ ｙ ｚ ｛ ｜ ｝ ￣ ^?
  //  0x80 ＼ ～ ￤ ^\ EN 　 を ぁ ぃ ぅ ぇ ぉ ゃ ゅ ょ っ
  //  0x90 　 あ い う え お か き く け こ さ し す せ そ
  //  0xa0 □ 。 「 」 、 ・ ヲ ァ ィ ゥ ェ ォ ャ ュ ョ ッ
  //  0xb0 ー ア イ ウ エ オ カ キ ク ケ コ サ シ ス セ ソ
  //  0xc0 タ チ ツ テ ト ナ ニ ヌ ネ ノ ハ ヒ フ ヘ ホ マ
  //  0xd0 ミ ム メ モ ヤ ユ ヨ ラ リ ル レ ロ ワ ン ゛ ゜
  //  0xe0 た ち つ て と な に ぬ ね の は ひ ふ へ ほ ま
  //  0xf0 み む め も や ゆ よ ら り る れ ろ わ ん 　 　
  //
  //    0x00         U+0020         SPACE。文字が割り当てられていないキーを表示するときに使うので^@ではなくて空白
  //    0x1c  ^¥     U+005E,U+00A5  CIRCUMFLEX ACCENT,YEN SIGN
  //    0x5c  ¥      U+00A5         YEN SIGN
  //    0x7c  |      U+007C         VERTICAL LINE
  //    0x7e  ¯      U+00AF         MACRON
  //    0x7f  ^?     U+005E,U+003F  CIRCUMFLEX ACCENT,QUESTION MARK
  //    0x80  \      U+005C         REVERSE SOLIDUS。0x5cの代わり
  //    0x81  ~      U+007E         TILDE。0x7eの代わり
  //    0x82  ¦      U+00A6         BROKEN BAR。0x7cの代わり
  //    0x83  ^\     U+005E,U+005C  CIRCUMFLEX ACCENT,REVERSE SOLIDUS。0x1cの代わり
  //    0x84  ENTER                 NUM ONのときに使う
  //    0xa0  □     U+00A0         NO-BREAK SPACE。空白と見分けがつかないと困るのでキーボードには市松模様で表示する
  //
  public static final int[][] KBD_VAR_LETTER = {
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x00
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000100000001000000,
      0b000001010000010100000,
      0b000010001000010100000,
      0b000100000100100010000,
      0b000000000000100010000,
      0b000000000001000001000,
      0b000000000001111111000,
      0b000000000001000001000,
      0b000000000001000001000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x01 ^A
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000100001111100000,
      0b000001010001000010000,
      0b000010001001000001000,
      0b000100000101000010000,
      0b000000000001111100000,
      0b000000000001000010000,
      0b000000000001000001000,
      0b000000000001000010000,
      0b000000000001111100000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x02 ^B
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000100000011100000,
      0b000001010000100010000,
      0b000010001001000001000,
      0b000100000101000000000,
      0b000000000001000000000,
      0b000000000001000000000,
      0b000000000001000001000,
      0b000000000000100010000,
      0b000000000000011100000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x03 ^C
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000100001111100000,
      0b000001010001000010000,
      0b000010001001000001000,
      0b000100000101000001000,
      0b000000000001000001000,
      0b000000000001000001000,
      0b000000000001000001000,
      0b000000000001000010000,
      0b000000000001111100000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x04 ^D
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000100001111111000,
      0b000001010001000000000,
      0b000010001001000000000,
      0b000100000101000000000,
      0b000000000001111110000,
      0b000000000001000000000,
      0b000000000001000000000,
      0b000000000001000000000,
      0b000000000001111111000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x05 ^E
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000100001111111000,
      0b000001010001000000000,
      0b000010001001000000000,
      0b000100000101000000000,
      0b000000000001111110000,
      0b000000000001000000000,
      0b000000000001000000000,
      0b000000000001000000000,
      0b000000000001000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x06 ^F
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000100000011110000,
      0b000001010000100000000,
      0b000010001001000000000,
      0b000100000101000000000,
      0b000000000001001111000,
      0b000000000001000010000,
      0b000000000001000010000,
      0b000000000000100010000,
      0b000000000000011100000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x07 ^G
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000100001000001000,
      0b000001010001000001000,
      0b000010001001000001000,
      0b000100000101000001000,
      0b000000000001111111000,
      0b000000000001000001000,
      0b000000000001000001000,
      0b000000000001000001000,
      0b000000000001000001000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x08 ^H
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000100000011100000,
      0b000001010000001000000,
      0b000010001000001000000,
      0b000100000100001000000,
      0b000000000000001000000,
      0b000000000000001000000,
      0b000000000000001000000,
      0b000000000000001000000,
      0b000000000000011100000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x09 ^I
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000100000000001000,
      0b000001010000000001000,
      0b000010001000000001000,
      0b000100000100000001000,
      0b000000000000000001000,
      0b000000000000000001000,
      0b000000000001000001000,
      0b000000000000100010000,
      0b000000000000011100000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x0a ^J
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000100001000001000,
      0b000001010001000010000,
      0b000010001001000100000,
      0b000100000101001000000,
      0b000000000001010000000,
      0b000000000001101000000,
      0b000000000001000100000,
      0b000000000001000010000,
      0b000000000001000001000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x0b ^K
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000100001000000000,
      0b000001010001000000000,
      0b000010001001000000000,
      0b000100000101000000000,
      0b000000000001000000000,
      0b000000000001000000000,
      0b000000000001000000000,
      0b000000000001000000000,
      0b000000000001111111000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x0c ^L
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000100001000001000,
      0b000001010001100011000,
      0b000010001001010101000,
      0b000100000101010101000,
      0b000000000001001001000,
      0b000000000001001001000,
      0b000000000001000001000,
      0b000000000001000001000,
      0b000000000001000001000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x0d ^M
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000100001000001000,
      0b000001010001000001000,
      0b000010001001100001000,
      0b000100000101010001000,
      0b000000000001001001000,
      0b000000000001000101000,
      0b000000000001000011000,
      0b000000000001000001000,
      0b000000000001000001000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x0e ^N
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000100000011100000,
      0b000001010000100010000,
      0b000010001001000001000,
      0b000100000101000001000,
      0b000000000001000001000,
      0b000000000001000001000,
      0b000000000001000001000,
      0b000000000000100010000,
      0b000000000000011100000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x0f ^O
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000100001111100000,
      0b000001010001000010000,
      0b000010001001000001000,
      0b000100000101000001000,
      0b000000000001000010000,
      0b000000000001111100000,
      0b000000000001000000000,
      0b000000000001000000000,
      0b000000000001000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x10 ^P
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000100000011100000,
      0b000001010000100010000,
      0b000010001001000001000,
      0b000100000101000001000,
      0b000000000001000001000,
      0b000000000001001001000,
      0b000000000001000101000,
      0b000000000000100010000,
      0b000000000000011101000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x11 ^Q
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000100001111100000,
      0b000001010001000010000,
      0b000010001001000001000,
      0b000100000101000001000,
      0b000000000001000010000,
      0b000000000001111100000,
      0b000000000001000010000,
      0b000000000001000001000,
      0b000000000001000001000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x12 ^R
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000100000011110000,
      0b000001010000100000000,
      0b000010001001000000000,
      0b000100000100100000000,
      0b000000000000011100000,
      0b000000000000000010000,
      0b000000000000000001000,
      0b000000000000000010000,
      0b000000000000111100000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x13 ^S
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000100001111111000,
      0b000001010000001000000,
      0b000010001000001000000,
      0b000100000100001000000,
      0b000000000000001000000,
      0b000000000000001000000,
      0b000000000000001000000,
      0b000000000000001000000,
      0b000000000000001000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x14 ^T
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000100001000001000,
      0b000001010001000001000,
      0b000010001001000001000,
      0b000100000101000001000,
      0b000000000001000001000,
      0b000000000001000001000,
      0b000000000001000001000,
      0b000000000000100010000,
      0b000000000000011100000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x15 ^U
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000100001000001000,
      0b000001010001000001000,
      0b000010001001000001000,
      0b000100000101000001000,
      0b000000000000100010000,
      0b000000000000100010000,
      0b000000000000010100000,
      0b000000000000010100000,
      0b000000000000001000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x16 ^V
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000100010001000100,
      0b000001010010001000100,
      0b000010001010001000100,
      0b000100000101010101000,
      0b000000000001010101000,
      0b000000000001010101000,
      0b000000000000100010000,
      0b000000000000100010000,
      0b000000000000100010000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x17 ^W
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000100001000001000,
      0b000001010001000001000,
      0b000010001000100010000,
      0b000100000100010100000,
      0b000000000000001000000,
      0b000000000000010100000,
      0b000000000000100010000,
      0b000000000001000001000,
      0b000000000001000001000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x18 ^X
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000100001000001000,
      0b000001010001000001000,
      0b000010001001000001000,
      0b000100000100100010000,
      0b000000000000010100000,
      0b000000000000001000000,
      0b000000000000001000000,
      0b000000000000001000000,
      0b000000000000001000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x19 ^Y
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000100001111111000,
      0b000001010000000001000,
      0b000010001000000010000,
      0b000100000100000100000,
      0b000000000000001000000,
      0b000000000000010000000,
      0b000000000000100000000,
      0b000000000001000000000,
      0b000000000001111111000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x1a ^Z
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000100000011100000,
      0b000001010000010000000,
      0b000010001000010000000,
      0b000100000100010000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000000000011100000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x1b ^[
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000100001000001000,
      0b000001010000100010000,
      0b000010001000010100000,
      0b000100000100001000000,
      0b000000000001111111000,
      0b000000000000001000000,
      0b000000000001111111000,
      0b000000000000001000000,
      0b000000000000001000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x1c ^¥
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000100000011100000,
      0b000001010000000100000,
      0b000010001000000100000,
      0b000100000100000100000,
      0b000000000000000100000,
      0b000000000000000100000,
      0b000000000000000100000,
      0b000000000000000100000,
      0b000000000000011100000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x1d ^]
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000100000001000000,
      0b000001010000010100000,
      0b000010001000100010000,
      0b000100000101000001000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x1e ^^
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000100000000000000,
      0b000001010000000000000,
      0b000010001000000000000,
      0b000100000100000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000001111111000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x1f ^_
    null,  //0x20
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000010000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x21 !
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000101000000000,
      0b000000000101000000000,
      0b000000000101000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x22 "
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000101000000000,
      0b000000000101000000000,
      0b000000011111110000000,
      0b000000000101000000000,
      0b000000000101000000000,
      0b000000000101000000000,
      0b000000011111110000000,
      0b000000000101000000000,
      0b000000000101000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x23 #
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000010000000000,
      0b000000001111110000000,
      0b000000010010000000000,
      0b000000010010000000000,
      0b000000001111100000000,
      0b000000000010010000000,
      0b000000000010010000000,
      0b000000011111100000000,
      0b000000000010000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x24 $
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000011100000000000,
      0b000000010100010000000,
      0b000000011100100000000,
      0b000000000001000000000,
      0b000000000010000000000,
      0b000000000100000000000,
      0b000000001001110000000,
      0b000000010001010000000,
      0b000000000001110000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x25 %
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001110000000000,
      0b000000010001000000000,
      0b000000010001000000000,
      0b000000001010000000000,
      0b000000000100000000000,
      0b000000001010000000000,
      0b000000010001010000000,
      0b000000010000100000000,
      0b000000001111010000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x26 &
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000001000000000,
      0b000000000001000000000,
      0b000000000010000000000,
      0b000000000100000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x27 '
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000001000000000,
      0b000000000010000000000,
      0b000000000100000000000,
      0b000000000100000000000,
      0b000000000100000000000,
      0b000000000100000000000,
      0b000000000100000000000,
      0b000000000010000000000,
      0b000000000001000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x28 (
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000100000000000,
      0b000000000010000000000,
      0b000000000001000000000,
      0b000000000001000000000,
      0b000000000001000000000,
      0b000000000001000000000,
      0b000000000001000000000,
      0b000000000010000000000,
      0b000000000100000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x29 )
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000010000000000,
      0b000000010010010000000,
      0b000000001010100000000,
      0b000000000111000000000,
      0b000000001010100000000,
      0b000000010010010000000,
      0b000000000010000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x2a *
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000011111110000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x2b +
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000110000000000,
      0b000000000110000000000,
      0b000000000010000000000,
      0b000000000100000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x2c ,
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000011111110000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x2d -
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000110000000000,
      0b000000000110000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x2e .
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000010000000,
      0b000000000000100000000,
      0b000000000001000000000,
      0b000000000010000000000,
      0b000000000100000000000,
      0b000000001000000000000,
      0b000000010000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x2f /
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001111100000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000001111100000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x30 0
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000001000000000,
      0b000000000011000000000,
      0b000000000101000000000,
      0b000000000001000000000,
      0b000000000001000000000,
      0b000000000001000000000,
      0b000000000001000000000,
      0b000000000001000000000,
      0b000000000001000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x31 1
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001111100000000,
      0b000000010000010000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000000001100000000,
      0b000000000110000000000,
      0b000000001000000000000,
      0b000000010000000000000,
      0b000000011111110000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x32 2
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001111100000000,
      0b000000010000010000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000000011100000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000010000010000000,
      0b000000001111100000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x33 3
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000001100000000,
      0b000000000010100000000,
      0b000000000100100000000,
      0b000000001000100000000,
      0b000000010000100000000,
      0b000000010000100000000,
      0b000000011111110000000,
      0b000000000000100000000,
      0b000000000000100000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x34 4
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000011111110000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000011111100000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000010000010000000,
      0b000000001111100000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x35 5
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000111100000000,
      0b000000001000000000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000011111100000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000001111100000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x36 6
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000011111110000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000000000100000000,
      0b000000000001000000000,
      0b000000000001000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x37 7
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001111100000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000001111100000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000001111100000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x38 8
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001111100000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000001111110000000,
      0b000000000000010000000,
      0b000000000000100000000,
      0b000000000001000000000,
      0b000000001110000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x39 9
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000110000000000,
      0b000000000110000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000110000000000,
      0b000000000110000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x3a :
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000110000000000,
      0b000000000110000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000110000000000,
      0b000000000110000000000,
      0b000000000010000000000,
      0b000000000100000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x3b ;
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000100000000,
      0b000000000001000000000,
      0b000000000010000000000,
      0b000000000100000000000,
      0b000000001000000000000,
      0b000000000100000000000,
      0b000000000010000000000,
      0b000000000001000000000,
      0b000000000000100000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x3c <
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000011111110000000,
      0b000000000000000000000,
      0b000000011111110000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x3d =
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001000000000000,
      0b000000000100000000000,
      0b000000000010000000000,
      0b000000000001000000000,
      0b000000000000100000000,
      0b000000000001000000000,
      0b000000000010000000000,
      0b000000000100000000000,
      0b000000001000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x3e >
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000111000000000,
      0b000000001000100000000,
      0b000000010000010000000,
      0b000000000000010000000,
      0b000000000000100000000,
      0b000000000001000000000,
      0b000000000010000000000,
      0b000000000000000000000,
      0b000000000010000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x3f ?
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000111000000000,
      0b000000001000100000000,
      0b000000010010010000000,
      0b000000010101010000000,
      0b000000010101010000000,
      0b000000010101010000000,
      0b000000010011110000000,
      0b000000001000000000000,
      0b000000000111100000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x40 @
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000010000000000,
      0b000000000101000000000,
      0b000000000101000000000,
      0b000000001000100000000,
      0b000000001000100000000,
      0b000000010000010000000,
      0b000000011111110000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x41 A
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000011111000000000,
      0b000000010000100000000,
      0b000000010000010000000,
      0b000000010000100000000,
      0b000000011111000000000,
      0b000000010000100000000,
      0b000000010000010000000,
      0b000000010000100000000,
      0b000000011111000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x42 B
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000111000000000,
      0b000000001000100000000,
      0b000000010000010000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000010000010000000,
      0b000000001000100000000,
      0b000000000111000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x43 C
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000011111000000000,
      0b000000010000100000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000100000000,
      0b000000011111000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x44 D
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000011111110000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000011111100000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000011111110000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x45 E
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000011111110000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000011111100000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x46 F
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000111100000000,
      0b000000001000000000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000010011110000000,
      0b000000010000100000000,
      0b000000010000100000000,
      0b000000001000100000000,
      0b000000000111000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x47 G
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000011111110000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x48 H
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000111000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000111000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x49 I
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000010000010000000,
      0b000000001000100000000,
      0b000000000111000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x4a J
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000010000010000000,
      0b000000010000100000000,
      0b000000010001000000000,
      0b000000010010000000000,
      0b000000010100000000000,
      0b000000011010000000000,
      0b000000010001000000000,
      0b000000010000100000000,
      0b000000010000010000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x4b K
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000011111110000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x4c L
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000010000010000000,
      0b000000011000110000000,
      0b000000010101010000000,
      0b000000010101010000000,
      0b000000010010010000000,
      0b000000010010010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x4d M
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000011000010000000,
      0b000000010100010000000,
      0b000000010010010000000,
      0b000000010001010000000,
      0b000000010000110000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x4e N
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000111000000000,
      0b000000001000100000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000001000100000000,
      0b000000000111000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x4f O
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000011111000000000,
      0b000000010000100000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000100000000,
      0b000000011111000000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x50 P
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000111000000000,
      0b000000001000100000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010010010000000,
      0b000000010001010000000,
      0b000000001000100000000,
      0b000000000111010000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x51 Q
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000011111000000000,
      0b000000010000100000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000100000000,
      0b000000011111000000000,
      0b000000010000100000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x52 R
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000111100000000,
      0b000000001000000000000,
      0b000000010000000000000,
      0b000000001000000000000,
      0b000000000111000000000,
      0b000000000000100000000,
      0b000000000000010000000,
      0b000000000000100000000,
      0b000000001111000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x53 S
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000011111110000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x54 T
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000001000100000000,
      0b000000000111000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x55 U
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000001000100000000,
      0b000000001000100000000,
      0b000000000101000000000,
      0b000000000101000000000,
      0b000000000010000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x56 V
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000100010001000000,
      0b000000100010001000000,
      0b000000100010001000000,
      0b000000010101010000000,
      0b000000010101010000000,
      0b000000010101010000000,
      0b000000001000100000000,
      0b000000001000100000000,
      0b000000001000100000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x57 W
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000001000100000000,
      0b000000000101000000000,
      0b000000000010000000000,
      0b000000000101000000000,
      0b000000001000100000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x58 X
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000001000100000000,
      0b000000000101000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x59 Y
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000011111110000000,
      0b000000000000010000000,
      0b000000000000100000000,
      0b000000000001000000000,
      0b000000000010000000000,
      0b000000000100000000000,
      0b000000001000000000000,
      0b000000010000000000000,
      0b000000011111110000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x5a Z
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000111000000000,
      0b000000000100000000000,
      0b000000000100000000000,
      0b000000000100000000000,
      0b000000000100000000000,
      0b000000000100000000000,
      0b000000000100000000000,
      0b000000000100000000000,
      0b000000000111000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x5b [
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000010000010000000,
      0b000000001000100000000,
      0b000000000101000000000,
      0b000000000010000000000,
      0b000000011111110000000,
      0b000000000010000000000,
      0b000000011111110000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x5c ¥
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000111000000000,
      0b000000000001000000000,
      0b000000000001000000000,
      0b000000000001000000000,
      0b000000000001000000000,
      0b000000000001000000000,
      0b000000000001000000000,
      0b000000000001000000000,
      0b000000000111000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x5d ]
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000010000000000,
      0b000000000101000000000,
      0b000000001000100000000,
      0b000000010000010000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x5e ^
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000011111110000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x5f _
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000100000000000,
      0b000000000100000000000,
      0b000000000010000000000,
      0b000000000001000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x60 `
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000111100000000,
      0b000000001000100000000,
      0b000000001000100000000,
      0b000000001001100000000,
      0b000000000110100000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x61 a
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001000000000000,
      0b000000001000000000000,
      0b000000001000000000000,
      0b000000001011000000000,
      0b000000001100100000000,
      0b000000001000100000000,
      0b000000001000100000000,
      0b000000001111000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x62 b
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000111000000000,
      0b000000001000000000000,
      0b000000001000000000000,
      0b000000001000100000000,
      0b000000000111000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x63 c
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000100000000,
      0b000000000000100000000,
      0b000000000000100000000,
      0b000000000111100000000,
      0b000000001000100000000,
      0b000000001000100000000,
      0b000000001001100000000,
      0b000000000110100000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x64 d
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000111000000000,
      0b000000001000100000000,
      0b000000001111100000000,
      0b000000001000000000000,
      0b000000000111000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x65 e
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000001100000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000001111100000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x66 f
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000111100000000,
      0b000000001000100000000,
      0b000000001000100000000,
      0b000000001000100000000,
      0b000000000111100000000,
      0b000000000000100000000,
      0b000000000111000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x67 g
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001000000000000,
      0b000000001000000000000,
      0b000000001000000000000,
      0b000000001111000000000,
      0b000000001000100000000,
      0b000000001000100000000,
      0b000000001000100000000,
      0b000000001000100000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x68 h
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000010000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x69 i
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000100000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000100000000,
      0b000000000000100000000,
      0b000000000000100000000,
      0b000000000000100000000,
      0b000000001000100000000,
      0b000000000111000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x6a j
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001000000000000,
      0b000000001000000000000,
      0b000000001000000000000,
      0b000000001001100000000,
      0b000000001010000000000,
      0b000000001110000000000,
      0b000000001001000000000,
      0b000000001000100000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x6b k
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x6c l
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000011101100000000,
      0b000000010010010000000,
      0b000000010010010000000,
      0b000000010010010000000,
      0b000000010010010000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x6d m
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001011000000000,
      0b000000001100100000000,
      0b000000001000100000000,
      0b000000001000100000000,
      0b000000001000100000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x6e n
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000111000000000,
      0b000000001000100000000,
      0b000000001000100000000,
      0b000000001000100000000,
      0b000000000111000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x6f o
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001011000000000,
      0b000000001100100000000,
      0b000000001000100000000,
      0b000000001000100000000,
      0b000000001111000000000,
      0b000000001000000000000,
      0b000000001000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x70 p
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000110100000000,
      0b000000001001100000000,
      0b000000001000100000000,
      0b000000001000100000000,
      0b000000000111100000000,
      0b000000000000100000000,
      0b000000000000100000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x71 q
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001011000000000,
      0b000000001100100000000,
      0b000000001000000000000,
      0b000000001000000000000,
      0b000000001000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x72 r
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000111100000000,
      0b000000001000000000000,
      0b000000000111000000000,
      0b000000000000100000000,
      0b000000001111000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x73 s
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000001111100000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000001100000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x74 t
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001000100000000,
      0b000000001000100000000,
      0b000000001000100000000,
      0b000000001001100000000,
      0b000000000110100000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x75 u
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001000100000000,
      0b000000001000100000000,
      0b000000000101000000000,
      0b000000000101000000000,
      0b000000000010000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x76 v
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000010010010000000,
      0b000000010010010000000,
      0b000000010010010000000,
      0b000000010010010000000,
      0b000000001101110000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x77 w
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001000100000000,
      0b000000000101000000000,
      0b000000000010000000000,
      0b000000000101000000000,
      0b000000001000100000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x78 x
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001000100000000,
      0b000000001000100000000,
      0b000000001000100000000,
      0b000000001000100000000,
      0b000000000111100000000,
      0b000000000000100000000,
      0b000000000111000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x79 y
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001111100000000,
      0b000000000001000000000,
      0b000000000010000000000,
      0b000000000100000000000,
      0b000000001111100000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x7a z
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000001000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000100000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000001000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x7b {
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x7c |
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000100000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000001000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000100000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x7d }
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000011111110000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x7e ¯
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000100000011100000,
      0b000001010000100010000,
      0b000010001001000001000,
      0b000100000100000001000,
      0b000000000000000010000,
      0b000000000000000100000,
      0b000000000000001000000,
      0b000000000000000000000,
      0b000000000000001000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x7f ^?
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000010000000000000,
      0b000000001000000000000,
      0b000000000100000000000,
      0b000000000100000000000,
      0b000000000010000000000,
      0b000000000001000000000,
      0b000000000001000000000,
      0b000000000000100000000,
      0b000000000000010000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x80 \
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001100000000000,
      0b000000010010010000000,
      0b000000000001100000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x81 ~
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000000000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x82 ¦
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000100001000000000,
      0b000001010000100000000,
      0b000010001000010000000,
      0b000100000100010000000,
      0b000000000000001000000,
      0b000000000000000100000,
      0b000000000000000100000,
      0b000000000000000010000,
      0b000000000000000001000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x83 ^\
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b011101010111011101100,
      0b010001010010010001010,
      0b010001110010010001010,
      0b011101110010011101100,
      0b010001110010010001010,
      0b010001010010010001010,
      0b011101010010011101010,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x84 ENTER
    null,  //0x85
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000010000000000,
      0b000000011111100000000,
      0b000000000100000000000,
      0b000000001110000000000,
      0b000000010011100000000,
      0b000000000101000000000,
      0b000000001001000000000,
      0b000000001000000000000,
      0b000000000111100000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x86 を
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000100000000000,
      0b000000011111110000000,
      0b000000000100000000000,
      0b000000001111100000000,
      0b000000010101010000000,
      0b000000010010010000000,
      0b000000001100100000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x87 ぁ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000010001000000000,
      0b000000010000100000000,
      0b000000010000100000000,
      0b000000010000010000000,
      0b000000010100010000000,
      0b000000001000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x88 ぃ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001110000000000,
      0b000000000000000000000,
      0b000000001111000000000,
      0b000000000000100000000,
      0b000000000000100000000,
      0b000000000001000000000,
      0b000000000110000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x89 ぅ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000111000000000,
      0b000000000000000000000,
      0b000000001111100000000,
      0b000000000001000000000,
      0b000000000010000000000,
      0b000000000110000000000,
      0b000000001001100000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x8a ぇ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000100000000000,
      0b000000011110100000000,
      0b000000000100100000000,
      0b000000001111000000000,
      0b000000010100100000000,
      0b000000010100100000000,
      0b000000001101000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x8b ぉ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001010000000000,
      0b000000001111100000000,
      0b000000011001010000000,
      0b000000000101010000000,
      0b000000000100000000000,
      0b000000000010000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x8c ゃ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000010010000000000,
      0b000000010111000000000,
      0b000000011010100000000,
      0b000000010010100000000,
      0b000000010100100000000,
      0b000000000101000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x8d ゅ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000011100000000,
      0b000000000010000000000,
      0b000000000111000000000,
      0b000000001010100000000,
      0b000000000100000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x8e ょ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000011111000000000,
      0b000000000000100000000,
      0b000000000000100000000,
      0b000000000001000000000,
      0b000000000110000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x8f っ
    null,  //0x90
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000100000000000,
      0b000000011111110000000,
      0b000000000100000000000,
      0b000000001111100000000,
      0b000000010101010000000,
      0b000000100101001000000,
      0b000000100101001000000,
      0b000000100010001000000,
      0b000000011100010000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x91 あ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000100000100000000,
      0b000000100000010000000,
      0b000000100000010000000,
      0b000000100000010000000,
      0b000000100000001000000,
      0b000000100000001000000,
      0b000000101000001000000,
      0b000000010000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x92 い
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001111000000000,
      0b000000000000000000000,
      0b000000001111000000000,
      0b000000110000100000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000000000100000000,
      0b000000000001000000000,
      0b000000000110000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x93 う
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000111000000000,
      0b000000000000000000000,
      0b000000011111110000000,
      0b000000000000100000000,
      0b000000000001000000000,
      0b000000000010000000000,
      0b000000000101000000000,
      0b000000001001000000000,
      0b000000010000110000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x94 え
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001000000000000,
      0b000000111110100000000,
      0b000000001000010000000,
      0b000000001111001000000,
      0b000000011000100000000,
      0b000000101000010000000,
      0b000001001000010000000,
      0b000000101000100000000,
      0b000000011001000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x95 お
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000100100000000,
      0b000000000100010000000,
      0b000000111110010000000,
      0b000000001001001000000,
      0b000000001001001000000,
      0b000000010001000000000,
      0b000000010001000000000,
      0b000000100001000000000,
      0b000000100110000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x96 か
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000100000000000,
      0b000000011111110000000,
      0b000000000010000000000,
      0b000000011111110000000,
      0b000000000001000000000,
      0b000000001111100000000,
      0b000000010000010000000,
      0b000000010000000000000,
      0b000000001111110000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x97 き
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000100000000,
      0b000000000001000000000,
      0b000000000010000000000,
      0b000000000100000000000,
      0b000000001000000000000,
      0b000000000100000000000,
      0b000000000010000000000,
      0b000000000001000000000,
      0b000000000000100000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x98 く
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000100001000000000,
      0b000000100001000000000,
      0b000000101111110000000,
      0b000000100001000000000,
      0b000000100001000000000,
      0b000000100001000000000,
      0b000000100001000000000,
      0b000000100010000000000,
      0b000000100100000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x99 け
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001111100000000,
      0b000000000000010000000,
      0b000000000000100000000,
      0b000000000001000000000,
      0b000000000000000000000,
      0b000000001000000000000,
      0b000000010000000000000,
      0b000000001111110000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x9a こ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001000000000000,
      0b000000000100100000000,
      0b000000011111000000000,
      0b000000000010000000000,
      0b000000000001000000000,
      0b000000001111100000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000001111110000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x9b さ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000001000100000000,
      0b000000000111000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x9c し
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000001000000000,
      0b000000111111110000000,
      0b000000000001000000000,
      0b000000001101000000000,
      0b000000010011000000000,
      0b000000010011000000000,
      0b000000001101000000000,
      0b000000000001000000000,
      0b000000000110000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x9d す
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001000100000000,
      0b000000001000100000000,
      0b000000111111111000000,
      0b000000001000100000000,
      0b000000001001100000000,
      0b000000001000000000000,
      0b000000001000000000000,
      0b000000001000000000000,
      0b000000000111110000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x9e せ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001111100000000,
      0b000000000001000000000,
      0b000000000010000000000,
      0b000000001100000000000,
      0b000000111111111000000,
      0b000000000110000000000,
      0b000000001000000000000,
      0b000000001000000000000,
      0b000000000111110000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x9f そ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000010101010000000,
      0b000000000000000000000,
      0b000000010101010000000,
      0b000000000000000000000,
      0b000000010101010000000,
      0b000000000000000000000,
      0b000000010101010000000,
      0b000000000000000000000,
      0b000000010101010000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xa0 □
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000110000000000,
      0b000000001001000000000,
      0b000000001001000000000,
      0b000000000110000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xa1 。
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001111100000000,
      0b000000001000000000000,
      0b000000001000000000000,
      0b000000001000000000000,
      0b000000001000000000000,
      0b000000001000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xa2 「
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000100000000,
      0b000000000000100000000,
      0b000000000000100000000,
      0b000000000000100000000,
      0b000000000000100000000,
      0b000000001111100000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xa3 」
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000100000000000,
      0b000000000010000000000,
      0b000000000001000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xa4 、
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000110000000000,
      0b000000000110000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xa5 ・
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000011111110000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000011111110000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000000000100000000,
      0b000000000001000000000,
      0b000000000110000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xa6 ヲ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001111100000000,
      0b000000000000100000000,
      0b000000000101000000000,
      0b000000000110000000000,
      0b000000000100000000000,
      0b000000001000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xa7 ァ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000100000000,
      0b000000000001000000000,
      0b000000000011000000000,
      0b000000000101000000000,
      0b000000001001000000000,
      0b000000000001000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xa8 ィ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000010000000000,
      0b000000001111100000000,
      0b000000001000100000000,
      0b000000000000100000000,
      0b000000000001000000000,
      0b000000000010000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xa9 ゥ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001111100000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000001111100000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xaa ェ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000001000000000,
      0b000000001111100000000,
      0b000000000001000000000,
      0b000000000011000000000,
      0b000000000101000000000,
      0b000000001001000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xab ォ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000100000000000,
      0b000000001111100000000,
      0b000000000100100000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000001000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xac ャ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000111000000000,
      0b000000000001000000000,
      0b000000000001000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000001111100000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xad ュ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001111100000000,
      0b000000000000100000000,
      0b000000001111100000000,
      0b000000000000100000000,
      0b000000001111100000000,
      0b000000000000100000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xae ョ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001010100000000,
      0b000000001010100000000,
      0b000000000000100000000,
      0b000000000001000000000,
      0b000000000010000000000,
      0b000000000100000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xaf ッ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000010000000000000,
      0b000000001111110000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xb0 ー
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000011111110000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000000100100000000,
      0b000000000111000000000,
      0b000000000100000000000,
      0b000000000100000000000,
      0b000000001000000000000,
      0b000000010000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xb1 ア
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000000000100000000,
      0b000000000001000000000,
      0b000000000011000000000,
      0b000000000101000000000,
      0b000000001001000000000,
      0b000000010001000000000,
      0b000000000001000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xb2 イ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000011111110000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000000000010000000,
      0b000000000000100000000,
      0b000000000001000000000,
      0b000000000110000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xb3 ウ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001111100000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000011111110000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xb4 エ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000001000000000,
      0b000000000001000000000,
      0b000000011111110000000,
      0b000000000001000000000,
      0b000000000011000000000,
      0b000000000101000000000,
      0b000000001001000000000,
      0b000000010001000000000,
      0b000000000001000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xb5 オ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000011111110000000,
      0b000000000010010000000,
      0b000000000100010000000,
      0b000000000100010000000,
      0b000000001000010000000,
      0b000000001000010000000,
      0b000000010001100000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xb6 カ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000100100000000,
      0b000000000101000000000,
      0b000000011110010000000,
      0b000000000010100000000,
      0b000000000011000000000,
      0b000000011101000000000,
      0b000000000001000000000,
      0b000000000000100000000,
      0b000000000000100000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xb7 キ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000010000000000,
      0b000000000011110000000,
      0b000000000100010000000,
      0b000000000100010000000,
      0b000000001000100000000,
      0b000000010000100000000,
      0b000000000001000000000,
      0b000000000010000000000,
      0b000000000100000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xb8 ク
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000100000000000,
      0b000000000100000000000,
      0b000000000111110000000,
      0b000000001001000000000,
      0b000000010001000000000,
      0b000000000001000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000100000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xb9 ケ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000011111110000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000011111110000000,
      0b000000000000010000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xba コ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001000100000000,
      0b000000001000100000000,
      0b000000011111110000000,
      0b000000001000100000000,
      0b000000001000100000000,
      0b000000000000100000000,
      0b000000000000100000000,
      0b000000000001000000000,
      0b000000000110000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xbb サ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000011000000000000,
      0b000000000100000000000,
      0b000000000000000000000,
      0b000000011000010000000,
      0b000000000100010000000,
      0b000000000000100000000,
      0b000000000001000000000,
      0b000000000010000000000,
      0b000000011100000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xbc シ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000011111110000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000000000100000000,
      0b000000000001000000000,
      0b000000000010000000000,
      0b000000000101000000000,
      0b000000001000100000000,
      0b000000010000010000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xbd ス
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001000000000000,
      0b000000001000000000000,
      0b000000001011110000000,
      0b000000011100010000000,
      0b000000001000100000000,
      0b000000001000000000000,
      0b000000001000000000000,
      0b000000001000000000000,
      0b000000000111110000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xbe セ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000010000000,
      0b000000010000010000000,
      0b000000001000010000000,
      0b000000000100010000000,
      0b000000000000100000000,
      0b000000000000100000000,
      0b000000000001000000000,
      0b000000000010000000000,
      0b000000000100000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xbf ソ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000010000000000,
      0b000000000011110000000,
      0b000000000100010000000,
      0b000000000110010000000,
      0b000000001001100000000,
      0b000000010000100000000,
      0b000000000001010000000,
      0b000000000010000000000,
      0b000000000100000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xc0 タ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000100000000,
      0b000000000011000000000,
      0b000000001110000000000,
      0b000000000001000000000,
      0b000000011111110000000,
      0b000000000001000000000,
      0b000000000001000000000,
      0b000000000010000000000,
      0b000000000100000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xc1 チ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000100010000000,
      0b000000000010010000000,
      0b000000010010010000000,
      0b000000001000010000000,
      0b000000001000100000000,
      0b000000000000100000000,
      0b000000000001000000000,
      0b000000000010000000000,
      0b000000000100000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xc2 ツ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001111100000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000011111110000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000100000000000,
      0b000000001000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xc3 テ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001000000000000,
      0b000000001000000000000,
      0b000000001000000000000,
      0b000000001000000000000,
      0b000000001110000000000,
      0b000000001001000000000,
      0b000000001000100000000,
      0b000000001000000000000,
      0b000000001000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xc4 ト
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000011111110000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000100000000000,
      0b000000001000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xc5 ナ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001111100000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000011111110000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xc6 ニ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000011111110000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000001000100000000,
      0b000000000101000000000,
      0b000000000010000000000,
      0b000000000101000000000,
      0b000000001000100000000,
      0b000000010000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xc7 ヌ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000010000000000,
      0b000000011111110000000,
      0b000000000000010000000,
      0b000000000000100000000,
      0b000000000001000000000,
      0b000000000010000000000,
      0b000000000110100000000,
      0b000000011010010000000,
      0b000000000010000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xc8 ネ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000000000100000000,
      0b000000000000100000000,
      0b000000000001000000000,
      0b000000000010000000000,
      0b000000000100000000000,
      0b000000011000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xc9 ノ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000010000000000,
      0b000000000001000000000,
      0b000000001001000000000,
      0b000000001000100000000,
      0b000000001000100000000,
      0b000000001000100000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xca ハ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000010000110000000,
      0b000000011111000000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000001111110000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xcb ヒ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000011111110000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000000000100000000,
      0b000000000001000000000,
      0b000000000110000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xcc フ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000100000000000,
      0b000000001010000000000,
      0b000000001001000000000,
      0b000000010001000000000,
      0b000000010000100000000,
      0b000000000000100000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xcd ヘ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000011111110000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000001010100000000,
      0b000000010010010000000,
      0b000000010010010000000,
      0b000000000010000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xce ホ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000011111110000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000000000100000000,
      0b000000010001000000000,
      0b000000001010000000000,
      0b000000000100000000000,
      0b000000000010000000000,
      0b000000000001000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xcf マ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001110000000000,
      0b000000000001100000000,
      0b000000000000010000000,
      0b000000001110000000000,
      0b000000000001100000000,
      0b000000000000010000000,
      0b000000011110000000000,
      0b000000000001100000000,
      0b000000000000010000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xd0 ミ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000100000000000,
      0b000000000100000000000,
      0b000000001000100000000,
      0b000000001000100000000,
      0b000000010011010000000,
      0b000000011100010000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xd1 ム
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000100000000,
      0b000000000000100000000,
      0b000000000001000000000,
      0b000000011001000000000,
      0b000000000110000000000,
      0b000000000011000000000,
      0b000000000100100000000,
      0b000000001000010000000,
      0b000000010000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xd2 メ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001111100000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000011111110000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000001110000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xd3 モ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001000000000000,
      0b000000001001110000000,
      0b000000000110010000000,
      0b000000011100100000000,
      0b000000000100000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000001000000000,
      0b000000000000100000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xd4 ヤ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001111100000000,
      0b000000000000100000000,
      0b000000000000100000000,
      0b000000000001000000000,
      0b000000000001000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000011111110000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xd5 ユ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000011111110000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000011111110000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000011111110000000,
      0b000000000000010000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xd6 ヨ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000011111110000000,
      0b000000000000000000000,
      0b000000011111110000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000000000100000000,
      0b000000000001000000000,
      0b000000000110000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xd7 ラ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000000000010000000,
      0b000000000000100000000,
      0b000000000000100000000,
      0b000000000001000000000,
      0b000000000110000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xd8 リ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001010000000000,
      0b000000001010000000000,
      0b000000001010000000000,
      0b000000001010000000000,
      0b000000001010000000000,
      0b000000001010010000000,
      0b000000001010010000000,
      0b000000010010100000000,
      0b000000010011000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xd9 ル
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000100000000,
      0b000000010011000000000,
      0b000000011100000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xda レ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000011111110000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000011111110000000,
      0b000000010000010000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xdb ロ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000011111110000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000000000100000000,
      0b000000000001000000000,
      0b000000000110000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xdc ワ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000010000000000000,
      0b000000001000000000000,
      0b000000000100000000000,
      0b000000000000000000000,
      0b000000000000010000000,
      0b000000000000100000000,
      0b000000000001000000000,
      0b000000000110000000000,
      0b000000011000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xdd ン
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000010010000000000,
      0b000000001001000000000,
      0b000000000100100000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xde ゛
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000110000000000,
      0b000000001001000000000,
      0b000000001001000000000,
      0b000000000110000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xdf ゜
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000100000000000,
      0b000000000100000000000,
      0b000000011111110000000,
      0b000000000100000000000,
      0b000000001001110000000,
      0b000000001000010000000,
      0b000000001000000000000,
      0b000000010010000000000,
      0b000000010001110000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xe0 た
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000100000000000,
      0b000000111111111000000,
      0b000000001000000000000,
      0b000000001011100000000,
      0b000000011100010000000,
      0b000000010000001000000,
      0b000000000000001000000,
      0b000000000000010000000,
      0b000000000111100000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xe1 ち
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001111100000000,
      0b000000110000010000000,
      0b000000000000001000000,
      0b000000000000001000000,
      0b000000000000010000000,
      0b000000000001100000000,
      0b000000000110000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xe2 つ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000111111110000000,
      0b000000000001100000000,
      0b000000000010000000000,
      0b000000000100000000000,
      0b000000001000000000000,
      0b000000001000000000000,
      0b000000001000000000000,
      0b000000000100000000000,
      0b000000000011100000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xe3 て
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001000000000000,
      0b000000001000000000000,
      0b000000001001110000000,
      0b000000000110000000000,
      0b000000001000000000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000001111110000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xe4 と
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001001000000000,
      0b000000001000100000000,
      0b000000111100010000000,
      0b000000010001010000000,
      0b000000010001000000000,
      0b000000100111000000000,
      0b000000001001100000000,
      0b000000001001010000000,
      0b000000000110000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xe5 な
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000010011100000000,
      0b000000010000010000000,
      0b000000010000100000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000010100000000000,
      0b000000010100000000000,
      0b000000010011110000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xe6 に
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000001000000000,
      0b000000010001000000000,
      0b000000010111110000000,
      0b000000001010001000000,
      0b000000011010001000000,
      0b000000101010001000000,
      0b000000100100111000000,
      0b000000100101001100000,
      0b000000011000110000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xe7 ぬ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001000000000000,
      0b000000111011100000000,
      0b000000001100010000000,
      0b000000001000010000000,
      0b000000011000010000000,
      0b000000011001110000000,
      0b000000101010011000000,
      0b000000101010010000000,
      0b000000001001100000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xe8 ね
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001111100000000,
      0b000000010010010000000,
      0b000000100010001000000,
      0b000000100010001000000,
      0b000000100010001000000,
      0b000000100100001000000,
      0b000000011000010000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xe9 の
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000100001000000000,
      0b000000100001000000000,
      0b000000101111110000000,
      0b000000100001000000000,
      0b000000100001000000000,
      0b000000100111000000000,
      0b000000101001100000000,
      0b000000101001010000000,
      0b000000100110000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xea は
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000111001000000000,
      0b000000001001100000000,
      0b000000010001010000000,
      0b000000100001000000000,
      0b000000100001000000000,
      0b000000100001000000000,
      0b000000100010000000000,
      0b000000011100000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xeb ひ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000011111000000000,
      0b000000000001000000000,
      0b000000000010000000000,
      0b000000100100010000000,
      0b000000100010010000000,
      0b000001000001001000000,
      0b000001000001001000000,
      0b000001010001001000000,
      0b000000001110000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xec ふ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000110000000000,
      0b000000001001000000000,
      0b000000010000100000000,
      0b000000100000010000000,
      0b000000000000001000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xed へ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000101111110000000,
      0b000000100001000000000,
      0b000000101111110000000,
      0b000000100001000000000,
      0b000000100001000000000,
      0b000000100111100000000,
      0b000000101001010000000,
      0b000000101001000000000,
      0b000000100110000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xee ほ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000010000000000,
      0b000000011111110000000,
      0b000000000010000000000,
      0b000000011111110000000,
      0b000000000010000000000,
      0b000000001111000000000,
      0b000000010010100000000,
      0b000000010010010000000,
      0b000000001100000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xef ま
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000011110000000000,
      0b000000000010000000000,
      0b000000000010010000000,
      0b000000001111110000000,
      0b000000010100011000000,
      0b000000100100010000000,
      0b000000100100010000000,
      0b000000011001100000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xf0 み
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000100000000000,
      0b000000111111110000000,
      0b000000000100000000000,
      0b000000000100110000000,
      0b000000011100001000000,
      0b000000100100000000000,
      0b000000100100010000000,
      0b000000011100010000000,
      0b000000000011100000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xf1 む
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000001000000000,
      0b000000010001000000000,
      0b000000010111100000000,
      0b000000001010010000000,
      0b000000011010001000000,
      0b000000101010001000000,
      0b000000100100001000000,
      0b000000100100001000000,
      0b000000011000110000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xf2 め
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000100000000000,
      0b000000011111100000000,
      0b000000000100000000000,
      0b000000011111100000000,
      0b000000000100000000000,
      0b000000000100000000000,
      0b000000000100010000000,
      0b000000000100010000000,
      0b000000000011100000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xf3 も
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000010000000000,
      0b000000010010000000000,
      0b000000010011110000000,
      0b000000001101001000000,
      0b000000011001001000000,
      0b000000101000010000000,
      0b000000000100000000000,
      0b000000000100000000000,
      0b000000000010000000000,
      0b000000000001000000000,
      0b000000000000000000000,
    },  //0xf4 や
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000010000000000,
      0b000000100001000000000,
      0b000000100111110000000,
      0b000000101001001000000,
      0b000000010001001000000,
      0b000000010001001000000,
      0b000000010010001000000,
      0b000000000010010000000,
      0b000000000100000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xf5 ゆ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000011110000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000001111000000000,
      0b000000010010100000000,
      0b000000010010010000000,
      0b000000001100000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xf6 よ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000011000000000000,
      0b000000000110000000000,
      0b000000000000000000000,
      0b000000010000000000000,
      0b000000010111100000000,
      0b000000011000010000000,
      0b000000010000010000000,
      0b000000000000100000000,
      0b000000000111000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xf7 ら
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000010011100000000,
      0b000000010100010000000,
      0b000000011000010000000,
      0b000000011000010000000,
      0b000000010000010000000,
      0b000000010000100000000,
      0b000000000000100000000,
      0b000000000001000000000,
      0b000000000110000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xf8 り
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001111100000000,
      0b000000000001000000000,
      0b000000000010000000000,
      0b000000000100000000000,
      0b000000001111100000000,
      0b000000010000010000000,
      0b000000000111010000000,
      0b000000001000110000000,
      0b000000000111100000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xf9 る
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001000000000000,
      0b000000111011100000000,
      0b000000001100010000000,
      0b000000001000010000000,
      0b000000011000010000000,
      0b000000011000100000000,
      0b000000101000100000000,
      0b000000101001000000000,
      0b000000001001110000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xfa れ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001111100000000,
      0b000000000001000000000,
      0b000000000010000000000,
      0b000000000100000000000,
      0b000000001111100000000,
      0b000000010000010000000,
      0b000000000000010000000,
      0b000000000000100000000,
      0b000000000111000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xfb ろ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001000000000000,
      0b000000001000000000000,
      0b000000111011100000000,
      0b000000001100010000000,
      0b000000011000010000000,
      0b000000011000010000000,
      0b000000111000010000000,
      0b000000101000100000000,
      0b000000001001000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xfc わ
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000100000000000,
      0b000000000100000000000,
      0b000000000100000000000,
      0b000000001000000000000,
      0b000000001000000000000,
      0b000000011100000000000,
      0b000000010010010000000,
      0b000000100010010000000,
      0b000000100001100000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0xfd ん
    null,  //0xfe
    null,  //0xff
  };

  //LEDキーの番号→LEDキーに表示する文字のパターン
  public static final int[][] KBD_LED_LETTER = {
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000001001000001001000,
      0b000001000100001000100,
      0b001111100100111100010,
      0b000010010010010001010,
      0b000010010010010001000,
      0b000100010000100111000,
      0b000100010000001001100,
      0b001000010000001001010,
      0b001001100000000110000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x5a  0 かな
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b001111100000011111000,
      0b001000100000000001000,
      0b001000100000000001000,
      0b001000101111010010000,
      0b001000100000001100000,
      0b001000100000000100000,
      0b001111100000000010000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x5b  1 ローマ字
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000010100,
      0b001111100000001010100,
      0b000000100000001000000,
      0b000000100000001000000,
      0b000000101111001110000,
      0b000000100000001001000,
      0b000000100000001000000,
      0b001111100000001000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x5c  2 コード入力
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b001100011001110001100,
      0b010010100101001010010,
      0b010000100101001010000,
      0b010000111101110001100,
      0b010000100101000000010,
      0b010010100101000010010,
      0b001100100101000001100,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x5d  3 CAPS
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000111001000100111000,
      0b000010001000101000100,
      0b000010001100101000000,
      0b000010001010100111000,
      0b000010001001100000100,
      0b000010001000101000100,
      0b000111001000100111000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x5e  4 INS
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000110000000,
      0b000111001000001100000,
      0b000001001100000000000,
      0b000010001010100000000,
      0b000100001000101111000,
      0b000100001000110000100,
      0b000100001000100000100,
      0b000100010000000001000,
      0b000011100000001110000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x5f  5 ひらがな
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000001000000011110000,
      0b000010100000100010000,
      0b000100010001111111100,
      0b011000001100100100100,
      0b000111110000111111100,
      0b000001000000100100100,
      0b000111110000111111100,
      0b000001000000100000100,
      0b001111111001000001100,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x60  6 全角
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b001000101000101000100,
      0b001000101000101101100,
      0b001100101000101010100,
      0b001010101000101000100,
      0b001001101000101000100,
      0b001000101000101000100,
      0b001000100111001000100,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x74  7 NUM
  };

  //固定キーの番号→固定キーに表示する文字のパターン
  public static final int[][] KBD_FIX_LETTER = {
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b001111100111000111000,
      0b001000001000101000100,
      0b001000001000001000000,
      0b001111000111001000000,
      0b001000000000101000000,
      0b001000001000101000100,
      0b001111100111000111000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x01  0 ESC
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000001111000111000000,
      0b000001000101000100000,
      0b000001000101000000000,
      0b000001111000111000000,
      0b000001000100000100000,
      0b000001000101000100000,
      0b000001111000111000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x0f  1 BS
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b001111100010001111000,
      0b000010000101001000100,
      0b000010000101001000100,
      0b000010001000101111000,
      0b000010001111101000100,
      0b000010001000101000100,
      0b000010001000101111000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x10  2 TAB
    {
      0b0,0b00000000000000000000000000000000,
      0b0,0b00000000000000000000000000000000,
      0b0,0b00000000000000000000000000000000,
      0b0,0b00000000000000000000000000000000,
      0b0,0b00000000000000000000000000000000,
      0b0,0b00000000000000000000000000000000,
      0b0,0b00000000000000000000000000000000,
      0b0,0b00000000000000000001110000000000,
      0b0,0b00000000000000000001010000000000,
      0b0,0b00000000000000000001010000000000,
      0b0,0b00000000000000000001010000000000,
      0b0,0b00000000000000000001010000000000,
      0b0,0b00000000000000000001010000000000,
      0b0,0b00000000000000000001010000000000,
      0b0,0b00000000000000000001010000000000,
      0b0,0b00000000000000000001010000000000,
      0b0,0b00000000000000000001010000000000,
      0b0,0b00000000000000001001010000000000,
      0b0,0b00000000000000011001010000000000,
      0b0,0b00000000000000101111010000000000,
      0b0,0b00000000000001000000010000000000,
      0b0,0b00000000000000101111110000000000,
      0b0,0b00000000000000011000000000000000,
      0b0,0b00000000000000001000000000000000,
      0b0,0b00000000000000000000000000000000,
      0b0,0b00000000000000000000000000000000,
      0b0,0b00000000000000000000000000000000,
      0b0,0b00000000000000000000000000000000,
      0b0,0b00000000000000000000000000000000,
    },  //0x1d  3 RETURN
    null,  //0x35  4 Space
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b010010011001001011110,
      0b010010100101111010000,
      0b010010100101111010000,
      0b011110100101001011100,
      0b010010100101001010000,
      0b010010100101001010000,
      0b010010011001001011110,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x36  5 HOME
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b001110001111101000000,
      0b001001001000001000000,
      0b001000101000001000000,
      0b001000101111001000000,
      0b001000101000001000000,
      0b001001001000001000000,
      0b001110001111101111100,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x37  6 DEL
    {
      0b000000000000000000000,
      0b011100011001000010000,
      0b010010100101000010000,
      0b011100100101000010000,
      0b010010100101000010000,
      0b010010011001111011110,
      0b000000000000000000000,
      0b000000100101110000000,
      0b000000100101001000000,
      0b000000100101110000000,
      0b000000100101000000000,
      0b000000011001000000000,
      0b000000000000000000000,
    },  //0x38  7 ROLLUP
    {
      0b000000000000000000000,
      0b011100011001000010000,
      0b010010100101000010000,
      0b011100100101000010000,
      0b010010100101000010000,
      0b010010011001111011110,
      0b000000000000000000000,
      0b011100011001001010010,
      0b010010100101001011010,
      0b010010100101111011110,
      0b010010100101111010110,
      0b011100011001001010010,
      0b000000000000000000000,
    },  //0x39  8 ROLLDOWN
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b010010100101100001100,
      0b010010100101010010010,
      0b010010110101001010010,
      0b010010111101001010010,
      0b010010101101001010010,
      0b010010100101010010010,
      0b001100100101100001100,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x3a  9 UNDO
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000100000000000,
      0b000000001100000000000,
      0b000000010111111000000,
      0b000000100000001000000,
      0b000000010111111000000,
      0b000000001100000000000,
      0b000000000100000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x3b 10 ←
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000010000000000,
      0b000000000101000000000,
      0b000000001000100000000,
      0b000000011101110000000,
      0b000000000101000000000,
      0b000000000101000000000,
      0b000000000101000000000,
      0b000000000101000000000,
      0b000000000111000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x3c 11 ↑
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000001000000000,
      0b000000000001100000000,
      0b000000111111010000000,
      0b000000100000001000000,
      0b000000111111010000000,
      0b000000000001100000000,
      0b000000000001000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x3d 12 →
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000111000000000,
      0b000000000101000000000,
      0b000000000101000000000,
      0b000000000101000000000,
      0b000000000101000000000,
      0b000000011101110000000,
      0b000000001000100000000,
      0b000000000101000000000,
      0b000000000010000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x3e 13 ↓
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000111001000001111000,
      0b001000101000001000100,
      0b001000001000001000100,
      0b001000001000001111000,
      0b001000001000001000100,
      0b001000101000001000100,
      0b000111001111101000100,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x3f 14 CLR
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000010000000,
      0b000000000000100000000,
      0b000000000001000000000,
      0b000000000010000000000,
      0b000000000100000000000,
      0b000000001000000000000,
      0b000000010000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x40 15 /
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000010000000000,
      0b000000010010010000000,
      0b000000001010100000000,
      0b000000000111000000000,
      0b000000001010100000000,
      0b000000010010010000000,
      0b000000000010000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x41 16 *
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000011111110000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x42 17 -
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000011111110000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000000000100000000,
      0b000000000001000000000,
      0b000000000001000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x43 18 7
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001111100000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000001111100000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000001111100000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x44 19 8
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001111100000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000001111110000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000000000100000000,
      0b000000001111000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x45 20 9
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000011111110000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x46 21 +
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000001100000000,
      0b000000000010100000000,
      0b000000000100100000000,
      0b000000001000100000000,
      0b000000010000100000000,
      0b000000010000100000000,
      0b000000011111110000000,
      0b000000000000100000000,
      0b000000000000100000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x47 22 4
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000011111110000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000011111100000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000010000010000000,
      0b000000001111100000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x48 23 5
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000111100000000,
      0b000000001000000000000,
      0b000000010000000000000,
      0b000000010000000000000,
      0b000000011111100000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000001111100000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x49 24 6
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000011111110000000,
      0b000000000000000000000,
      0b000000011111110000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x4a 25 =
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000010000000000,
      0b000000000110000000000,
      0b000000001010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000010000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x4b 26 1
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001111100000000,
      0b000000010000010000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000000001100000000,
      0b000000000110000000000,
      0b000000001000000000000,
      0b000000010000000000000,
      0b000000011111110000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x4c 27 2
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001111100000000,
      0b000000010000010000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000000011100000000,
      0b000000000000010000000,
      0b000000000000010000000,
      0b000000010000010000000,
      0b000000001111100000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x4d 28 3
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b011101010111011101100,
      0b010001010010010001010,
      0b010001110010010001010,
      0b011101110010011101100,
      0b010001110010010001010,
      0b010001010010010001010,
      0b011101010010011101010,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x4e 29 ENTER
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000001111100000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000010000010000000,
      0b000000001111100000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x4f 30 0
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000110000000000,
      0b000000000110000000000,
      0b000000000010000000000,
      0b000000000100000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x50 31 ,
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000110000000000,
      0b000000000110000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x51 32 .
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000111011110011111000,
      0b000000000010010001000,
      0b001111000010011111000,
      0b000000011110000000000,
      0b000111010000111111100,
      0b000000010000001000000,
      0b000111010000001111000,
      0b000101010010000001000,
      0b000111011110011111000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x52 33 記号入力
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b010111000100110011100,
      0b001010101001001000100,
      0b000111110010111011100,
      0b001000001001100000100,
      0b010111110100110111110,
      0b000100010001100001000,
      0b000111110010101011110,
      0b000010100001110011100,
      0b001111111011000101010,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x53 34 登録
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b010010111101000011100,
      0b010010100001000010010,
      0b010010100001000010010,
      0b011110111001000011100,
      0b010010100001000010000,
      0b010010100001000010000,
      0b010010111101111010000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x54 35 HELP
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b001000101111100010000,
      0b001000101000000110000,
      0b000101001000000010000,
      0b000010001111000010000,
      0b000101001000000010000,
      0b001000101000000010000,
      0b001000101000000010000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x55 36 XF1
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b001000101111100111000,
      0b001000101000001000100,
      0b000101001000000000100,
      0b000010001111000011000,
      0b000101001000000100000,
      0b001000101000001000000,
      0b001000101000001111100,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x56 37 XF2
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b001000101111100111000,
      0b001000101000001000100,
      0b000101001000000000100,
      0b000010001111000011000,
      0b000101001000000000100,
      0b001000101000001000100,
      0b001000101000000111000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x57 38 XF3
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b001000101111100001000,
      0b001000101000000011000,
      0b000101001000000101000,
      0b000010001111001001000,
      0b000101001000001111100,
      0b001000101000000001000,
      0b001000101000000001000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x58 39 XF4
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b001000101111101111100,
      0b001000101000001000000,
      0b000101001000001111000,
      0b000010001111000000100,
      0b000101001000000000100,
      0b001000101000001000100,
      0b001000101000000111000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x59 40 XF5
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b011001100111001001010,
      0b010101010100010101010,
      0b010101010100010101010,
      0b011001100111011101100,
      0b010101010100010101010,
      0b010101010100010101010,
      0b011001010111010101010,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x61 41 BREAK
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b001100011001110100010,
      0b010010100101001100010,
      0b010000100101001010100,
      0b010000100101110001000,
      0b010000100101000001000,
      0b010010100101000001000,
      0b001100011001000001000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x62 42 COPY
    {
      0b000000000000000000000000000,
      0b000000000000000000000000000,
      0b000000001111100010000000000,
      0b000000001000000110000000000,
      0b000000001000000010000000000,
      0b000000001111000010000000000,
      0b000000001000000010000000000,
      0b000000001000000010000000000,
      0b000000001000000010000000000,
      0b000000000000000000000000000,
      0b000000000000000000000000000,
    },  //0x63 43 F1
    {
      0b000000000000000000000000000,
      0b000000000000000000000000000,
      0b000000001111100111000000000,
      0b000000001000001000100000000,
      0b000000001000000000100000000,
      0b000000001111000011000000000,
      0b000000001000000100000000000,
      0b000000001000001000000000000,
      0b000000001000001111100000000,
      0b000000000000000000000000000,
      0b000000000000000000000000000,
    },  //0x64 44 F2
    {
      0b000000000000000000000000000,
      0b000000000000000000000000000,
      0b000000001111100111000000000,
      0b000000001000001000100000000,
      0b000000001000000000100000000,
      0b000000001111000011000000000,
      0b000000001000000000100000000,
      0b000000001000001000100000000,
      0b000000001000000111000000000,
      0b000000000000000000000000000,
      0b000000000000000000000000000,
    },  //0x65 45 F3
    {
      0b000000000000000000000000000,
      0b000000000000000000000000000,
      0b000000001111100001000000000,
      0b000000001000000011000000000,
      0b000000001000000101000000000,
      0b000000001111001001000000000,
      0b000000001000001111100000000,
      0b000000001000000001000000000,
      0b000000001000000001000000000,
      0b000000000000000000000000000,
      0b000000000000000000000000000,
    },  //0x66 46 F4
    {
      0b000000000000000000000000000,
      0b000000000000000000000000000,
      0b000000001111101111100000000,
      0b000000001000001000000000000,
      0b000000001000001111000000000,
      0b000000001111000000100000000,
      0b000000001000000000100000000,
      0b000000001000001000100000000,
      0b000000001000000111000000000,
      0b000000000000000000000000000,
      0b000000000000000000000000000,
    },  //0x67 47 F5
    {
      0b000000000000000000000000000,
      0b000000000000000000000000000,
      0b000000001111100011000000000,
      0b000000001000000100000000000,
      0b000000001000001000000000000,
      0b000000001111001111000000000,
      0b000000001000001000100000000,
      0b000000001000001000100000000,
      0b000000001000000111000000000,
      0b000000000000000000000000000,
      0b000000000000000000000000000,
    },  //0x68 48 F6
    {
      0b000000000000000000000000000,
      0b000000000000000000000000000,
      0b000000001111101111100000000,
      0b000000001000000000100000000,
      0b000000001000000000100000000,
      0b000000001111000001000000000,
      0b000000001000000010000000000,
      0b000000001000000010000000000,
      0b000000001000000010000000000,
      0b000000000000000000000000000,
      0b000000000000000000000000000,
    },  //0x69 49 F7
    {
      0b000000000000000000000000000,
      0b000000000000000000000000000,
      0b000000001111100111000000000,
      0b000000001000001000100000000,
      0b000000001000001000100000000,
      0b000000001111000111000000000,
      0b000000001000001000100000000,
      0b000000001000001000100000000,
      0b000000001000000111000000000,
      0b000000000000000000000000000,
      0b000000000000000000000000000,
    },  //0x6a 50 F8
    {
      0b000000000000000000000000000,
      0b000000000000000000000000000,
      0b000000001111100111000000000,
      0b000000001000001000100000000,
      0b000000001000001000100000000,
      0b000000001111000111100000000,
      0b000000001000000000100000000,
      0b000000001000000001000000000,
      0b000000001000000110000000000,
      0b000000000000000000000000000,
      0b000000000000000000000000000,
    },  //0x6b 51 F9
    {
      0b000000000000000000000000000,
      0b000000000000000000000000000,
      0b000001111100010000111000000,
      0b000001000000110001000100000,
      0b000001000000010001000100000,
      0b000001111000010001000100000,
      0b000001000000010001000100000,
      0b000001000000010001000100000,
      0b000001000000010000111000000,
      0b000000000000000000000000000,
      0b000000000000000000000000000,
    },  //0x6c 52 F10
    {
      0b0000000,0b00000000000000000000000000000000,
      0b0000000,0b00000000000000000000000000000000,
      0b0000000,0b00000000000000000000000000000000,
      0b0000001,0b11001000100111001111101111100000,
      0b0000010,0b00101000100010001000000010000000,
      0b0000010,0b00001000100010001000000010000000,
      0b0000001,0b11001111100010001111000010000000,
      0b0000000,0b00101000100010001000000010000000,
      0b0000010,0b00101000100010001000000010000000,
      0b0000001,0b11001000100111001000000010000000,
      0b0000000,0b00000000000000000000000000000000,
      0b0000000,0b00000000000000000000000000000000,
      0b0000000,0b00000000000000000000000000000000,
    },  //0x70 53 SHIFT
    {
      0b000000000000000000000000000,
      0b000000000000000000000000000,
      0b000000000000000000000000000,
      0b000111001111101111001000000,
      0b001000100010001000101000000,
      0b001000000010001000101000000,
      0b001000000010001111001000000,
      0b001000000010001000101000000,
      0b001000100010001000101000000,
      0b000111000010001000101111100,
      0b000000000000000000000000000,
      0b000000000000000000000000000,
      0b000000000000000000000000000,
    },  //0x71 54 CTRL
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000110011100111000100,
      0b001001010010010001100,
      0b001001010010010000100,
      0b001001011100010000100,
      0b001001010000010000100,
      0b001001010000010000100,
      0b000110010000010100100,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x72 55 OPT.1
    {
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
      0b001100111001110001100,
      0b010010100100100010010,
      0b010010100100100000010,
      0b010010111000100000100,
      0b010010100000100001000,
      0b010010100000100010000,
      0b001100100000101011110,
      0b000000000000000000000,
      0b000000000000000000000,
      0b000000000000000000000,
    },  //0x73 56 OPT.2
  };

  //キーのビットマップ
  //  可変キーとLEDキーはキー毎にビットマップを用意する
  //  可変キーのビットマップの幅はキーの幅
  public static final int[][][] kbdVarStatBitmap = new int[256][][];  //文字コード→状態→可変キーのビットマップ
  public static final int[][][] kbdLedStatBitmap = new int[KBD_LED_KEYS][][];  //LEDキーの番号→状態→LEDキーのビットマップ

  //種類毎に別々に用意するもの
  //  ビットマップ
  //    ピクセルオフセット→RGBコード
  //    固定キーのビットマップの幅はキーボードの幅
  //  番号マップ
  //    ピクセルオフセット→番号。-1=キーがない
  //    マウスの座標にどのキーがあるか瞬時に判断するための配列
  //  レクタングル
  //    {}または{x,y,w,h}または{x1,y1,w1,h1,x2,y2,w2,h2}
  //    1段目を囲む0個～2個の長方形。欠番は0個、RETURNキーとSHIFTキーは2個、その他は1個
  //    キーを描き変えるときに使う
  public static final BufferedImage[] kbdDataTypeImage = new BufferedImage[KBD_TYPES];  //種類→イメージ
  public static final int[] kbdDataTypeWidth = new int[KBD_TYPES];  //種類→キーボードの幅
  public static final int[] kbdDataTypeHeight = new int[KBD_TYPES];  //種類→キーボードの高さ
  public static final int[][] kbdDataTypeBitmap = new int[KBD_TYPES][];  //種類→ビットマップ
  public static final byte[][] kbdDataTypeIndexMap = new byte[KBD_TYPES][];  //種類→番号マップ
  public static final int[][][] kbdDataTypeStatFixBitmap = new int[KBD_TYPES][][];  //種類→状態→固定キーのビットマップ
  public static final int[][][] kbdDataTypeIndexRect = new int[KBD_TYPES][][];  //種類→番号→レクタングル
  public static final int[][] kbdDataTypeIndexCharacterStat = new int[KBD_TYPES][];  //種類→番号→最後に描画したときの文字<<8|状態。-1=まだ描画されていない

  //設定
  public static boolean kbdUSLayoutOn;  //true=USレイアウト。初期値を選択するときに使う
  public static int[] kbdDefaultMapNonZ;  //Zキーボード以外のデフォルトのキーマップ
  public static int[] kbdCurrentMapNonZ;  //(Zキーボードのとき)Zキーボード以外のキーマップのバックアップ
  public static int[] kbdCurrentMap;  //現在のキーマップ

  //現在の状態
  //  表示位置のXEiJ.pnlKeyboardX,XEiJ.pnlKeyboardYはパネル側で制御するのでここにはない
  public static boolean kbdOn;  //true=表示する,false=表示しない
  public static int kbdType;  //キーボードの種類。KBD_STANDARD_TYPEまたはKBD_COMPACT_TYPE
  public static int kbdWidth;  //キーボードの幅
  public static int kbdHeight;  //キーボードの高さ
  public static BufferedImage kbdImage;  //イメージ
  public static int[] kbdBitmap;  //ビットマップ
  public static byte[] kbdIndexMap;  //番号マップ
  public static final int[] kbdStat = new int[KBD_ALL_KEYS];  //番号→状態
  public static int kbdPointedIndex;  //ポイントされているキーの番号。-1=どのキーもポイントされていない
  public static volatile TimerTask kbdRepeatTask;  //リピート中のキーコードをMFPに転送するタスク。null=リピート中ではない
  public static int kbdRepeatDelay;  //リピートディレイ(ms)
  public static int kbdRepeatInterval;  //リピートインターバル(ms)
  public static int kbdLedBrightness;  //LEDの明るさ。0=明るい,1=やや明るい,2=やや暗い,3=暗い

  //Zキーボード
  public static final boolean KBD_ZKEY_ON = true;
  public static boolean kbdZKeyOnRequest;  //true=Zキーボードを制御する
  public static boolean kbdZKeyOn;  //true=Zキーボードを制御する
  public static int kbdZKeyStatus;  //LEDの状態。bit0=かな,…,bit7=全角,0=消灯,1=点灯。ポートが開いていなくても更新する
  public static ZKeyPort kbdZKeyPort;  //ポート
  public static int kbdZKeyMode;  //0=HIDのみ,1=自動(WinUSB優先),2=WinUSBのみ
  public static boolean kbdZKeyIsUsb;  //true=ポートはWinUSB(libusb)。キーマップの入れ替えやNumLock制御は行わない
  public static boolean kbdZKeyDebugFlag;  //デバッグフラグ
  //  I/Oポート
  //  $00E9F040  かな
  //  $00E9F041  ローマ字
  //  $00E9F042  コード入力
  //  $00E9F043  CAPS
  //  $00E9F044  INS
  //  $00E9F045  ひらがな
  //  $00E9F046  全角
  //  $00E9F047  reserved
  //  $00E9F048  bit0    programmable  0=ノーマルモード,1=プログラマブルモード
  //             bit1    send          1=送信
  //             bit2-7  reserved
  //  $00E9F049  reserved
  //      :         :
  //  $00E9F04F  reserved
  public static final int KBD_ZKEY_IO_ADDRESS = 0x00e9f040;
  public static final int KBD_ZKEY_IO_SIZE = 16;
  public static long kbdZKeyIOData;  //I/Oポートのデータ
  public static boolean kbdZKeyIOProgrammable;  //false=ノーマルモード,true=プログラマブルモード
  //  入力
  //    BREAKの半角(VK 244)と全角(VK 243)は押されたときreleasedが発火していつ離されたのか分からない
  //      半角(VK 244)と全角(VK 243)のpressedを無視する。releasedで入力してすぐ離す
  //    CAPSの英数字(VK 240)→F17は押されたときreleasedが発火するがXF4でも発火するので区別がつかずいつ離されたのかも分からない
  //      英数字(VK 240)→F17のreleasedを無視する。pressedで入力してすぐ離す
  //    XF4のひらがな(VK 242)→F16は押されたときreleasedが発火していつ離されたのか分からない
  //      ひらがな(VK 242)→F16のreleasedを無視する。pressedで入力して他のキーが離されるまで押されたままにする
  //    NumLockが点灯していないとテンキーの5と=を区別できない
  //      NumLockが離されたときNumLockが消灯していたら叩いて点灯させる。次のNumLockを入力に使わない
  public static KeyEvent kbdXF4Locked;  //押されたままのXF4
  public static boolean kbdSavedNumLock;  //保存したNumLock
  public static boolean kbdIgnoreNumLock;  //NumLockを1回無視する
  //  キーマップ
  public static int[] kbdDefaultMapZ;  //Zキーボードのデフォルトのキーマップ
  public static int[] kbdCurrentMapZ;  //(Zキーボードでないとき)Zキーボードのキーマップのバックアップ
  //  メニュー
  public static JMenu kbdZKeyMenu;
  public static JCheckBoxMenuItem kbdZKeyConnectCheckBox;
  public static JCheckBoxMenuItem kbdZKeyDemoCheckBox;

  //キーの番号
  //  eo
  //    emulator order
  //    可変キー48個、LEDキー8個、固定キー57個の順で合計113個
  //  xo
  //    X68000 order
  //    X68000(NUM OFF)のキーコードの昇順。113個

  //キーマップ
  //  keyCode
  //    ke.getKeyCode()
  //    Windowsの「￥」と「ろ」のキーコードは92。キーコードだけでは見分けられない
  //  keyLocation
  //    ke.getKeyLocation()
  //    1  STANDARD
  //    2  LEFT
  //    3  RIGHT
  //    4  NUMPAD
  //    Macの「ろ」のキーロケーションは4。意味不明。
  //  extendedKeyCode
  //    ke.getExtendedKeyCode()
  //    Macの「￥」と「］」の拡張キーコードは92。拡張キーコードだけでは見分けられない
  //    MacのShift+5の拡張キーコードは0。ユニークIDどころかIDの用もなしていない
  //  rawCode
  //    ke.paramString()から取り出す
  //    Java15までrawCodeを直接参照できた。Java16から直接参照できなくなった
  //  intCode
  //    KeyEventのデータをintに詰め込む
  //    Mac
  //      (keyCode&0xfff)<<16|(extendedKeyCode&0x0f000fff)<<4|(keyLocation&0xf)
  //    Windows
  //      (keyCode&0xfff)<<16|(rawCode&0xfff)<<4|(keyLocation&0xf)
  //  kbdCurrentMap
  //    kbdCurrentMap[3*xo]=intCode1
  //    kbdCurrentMap[3*xo+1]=intCode2
  //    kbdCurrentMap[3*xo+2]=intCode3
  //  メモ
  //    半角全角漢字、変換、カタカナひらがなはKeyEventが発生しない
  //    窓はconsumeしてもスタートメニューが開く
  //    NumLock、ScrollLockはconsumeしてもNumLock、ScrollLockが切り替わる
  //

  //Thanks to saku39 for providing the data for the Linux.
  public static final int[] KBD_DEFAULT_MAP_LINUX = {
    //(keyCode&0xffff)<<16|(rawCode&0xfff)<<4|(keyLocation&0xf)
    //cccc_rrr_l    cccc_rrr_l    cccc_rrr_l        xo   hex  dec  text
    0x001b_01b_1, 0x001b_009_1, 0x0000_000_0,  //    0  0x01    1  ESC
    0x0031_031_1, 0x0031_00a_1, 0x0000_000_0,  //    1  0x02    2  １！ぬ　
    0x0032_032_1, 0x0032_00b_1, 0x0000_000_0,  //    2  0x03    3  ２＂ふ　
    0x0033_033_1, 0x0033_00c_1, 0x0000_000_0,  //    3  0x04    4  ３＃あぁ
    0x0034_034_1, 0x0034_00d_1, 0x0000_000_0,  //    4  0x05    5  ４＄うぅ
    0x0035_035_1, 0x0035_00e_1, 0x0000_000_0,  //    5  0x06    6  ５％えぇ
    0x0036_036_1, 0x0036_00f_1, 0x0000_000_0,  //    6  0x07    7  ６＆おぉ
    0x0037_037_1, 0x0037_010_1, 0x0000_000_0,  //    7  0x08    8  ７＇やゃ
    0x0038_038_1, 0x0038_011_1, 0x0000_000_0,  //    8  0x09    9  ８（ゆゅ
    0x0039_039_1, 0x0039_012_1, 0x0000_000_0,  //    9  0x0a   10  ９）よょ
    0x0030_030_1, 0x0030_013_1, 0x0000_000_0,  //   10  0x0b   11  ０　わを
    0x002d_0bd_1, 0x002d_014_1, 0x0000_000_0,  //   11  0x0c   12  －＝ほ　
    0x0202_0de_1, 0x0202_015_1, 0x0000_000_0,  //   12  0x0d   13  ＾～へ　
    0x005c_0dc_1, 0x005c_084_1, 0x0000_000_0,  //   13  0x0e   14  ￥｜ー　
    0x0008_008_1, 0x0008_016_1, 0x0000_000_0,  //   14  0x0f   15  BS
    0x0009_009_1, 0x0009_017_1, 0x0000_000_0,  //   15  0x10   16  TAB
    0x0051_051_1, 0x0051_018_1, 0x0000_000_0,  //   16  0x11   17  Ｑ　た　
    0x0057_057_1, 0x0057_019_1, 0x0000_000_0,  //   17  0x12   18  Ｗ　て　
    0x0045_045_1, 0x0045_01a_1, 0x0000_000_0,  //   18  0x13   19  Ｅ　いぃ
    0x0052_052_1, 0x0052_01b_1, 0x0000_000_0,  //   19  0x14   20  Ｒ　す　
    0x0054_054_1, 0x0054_01c_1, 0x0000_000_0,  //   20  0x15   21  Ｔ　か　
    0x0059_059_1, 0x0059_01d_1, 0x0000_000_0,  //   21  0x16   22  Ｙ　ん　
    0x0055_055_1, 0x0055_01e_1, 0x0000_000_0,  //   22  0x17   23  Ｕ　な　
    0x0049_049_1, 0x0049_01f_1, 0x0000_000_0,  //   23  0x18   24  Ｉ　に　
    0x004f_04f_1, 0x004f_020_1, 0x0000_000_0,  //   24  0x19   25  Ｏ　ら　
    0x0050_050_1, 0x0050_021_1, 0x0000_000_0,  //   25  0x1a   26  Ｐ　せ　
    0x0200_0c0_1, 0x0200_022_1, 0x0000_000_0,  //   26  0x1b   27  ＠｀゛　
    0x005b_0db_1, 0x005b_023_1, 0x0000_000_0,  //   27  0x1c   28  ［｛゜「
    0x000a_00d_1, 0x000a_024_1, 0x0000_000_0,  //   28  0x1d   29  リターン
    0x0041_041_1, 0x0041_026_1, 0x0000_000_0,  //   29  0x1e   30  Ａ　ち　
    0x0053_053_1, 0x0053_027_1, 0x0000_000_0,  //   30  0x1f   31  Ｓ　と　
    0x0044_044_1, 0x0044_028_1, 0x0000_000_0,  //   31  0x20   32  Ｄ　し　
    0x0046_046_1, 0x0046_029_1, 0x0000_000_0,  //   32  0x21   33  Ｆ　は　
    0x0047_047_1, 0x0047_02a_1, 0x0000_000_0,  //   33  0x22   34  Ｇ　き　
    0x0048_048_1, 0x0048_02b_1, 0x0000_000_0,  //   34  0x23   35  Ｈ　く　
    0x004a_04a_1, 0x004a_02c_1, 0x0000_000_0,  //   35  0x24   36  Ｊ　ま　
    0x004b_04b_1, 0x004b_02d_1, 0x0000_000_0,  //   36  0x25   37  Ｋ　の　
    0x004c_04c_1, 0x004c_02e_1, 0x0000_000_0,  //   37  0x26   38  Ｌ　り　
    0x003b_0bb_1, 0x003b_02f_1, 0x0000_000_0,  //   38  0x27   39  ；＋れ　
    0x0201_0ba_1, 0x0201_030_1, 0x0000_000_0,  //   39  0x28   40  ：＊け　
    0x005d_0dd_1, 0x005d_033_1, 0x0000_000_0,  //   40  0x29   41  ］｝む」
    0x005a_05a_1, 0x005a_034_1, 0x0000_000_0,  //   41  0x2a   42  Ｚ　つっ
    0x0058_058_1, 0x0058_035_1, 0x0000_000_0,  //   42  0x2b   43  Ｘ　さ　
    0x0043_043_1, 0x0043_036_1, 0x0000_000_0,  //   43  0x2c   44  Ｃ　そ　
    0x0056_056_1, 0x0056_037_1, 0x0000_000_0,  //   44  0x2d   45  Ｖ　ひ　
    0x0042_042_1, 0x0042_038_1, 0x0000_000_0,  //   45  0x2e   46  Ｂ　こ　
    0x004e_04e_1, 0x004e_039_1, 0x0000_000_0,  //   46  0x2f   47  Ｎ　み　
    0x004d_04d_1, 0x004d_03a_1, 0x0000_000_0,  //   47  0x30   48  Ｍ　も　
    0x002c_0bc_1, 0x002c_03b_1, 0x0000_000_0,  //   48  0x31   49  ，＜ね、
    0x002e_0be_1, 0x002e_03c_1, 0x0000_000_0,  //   49  0x32   50  ．＞る。
    0x002f_0bf_1, 0x002f_03d_1, 0x0000_000_0,  //   50  0x33   51  ／？め・
    0x005c_0e2_1, 0x005c_061_1, 0x0000_000_0,  //   51  0x34   52  　＿ろ□
    0x0020_020_1, 0x0020_041_1, 0x0000_000_0,  //   52  0x35   53  スペース
    0x0024_024_1, 0x0024_06e_1, 0x0000_000_0,  //   53  0x36   54  HOME
    0x007f_02e_1, 0x007f_077_1, 0x0000_000_0,  //   54  0x37   55  DEL
    0x0022_022_1, 0x0022_075_1, 0x0000_000_0,  //   55  0x38   56  ROLLUP
    0x0021_021_1, 0x0021_070_1, 0x0000_000_0,  //   56  0x39   57  ROLLDOWN
    0x0023_023_1, 0x0023_073_1, 0x0000_000_0,  //   57  0x3a   58  UNDO
    0x0025_025_1, 0x0025_071_1, 0x0000_000_0,  //   58  0x3b   59  ←
    0x0026_026_1, 0x0026_06f_1, 0x0000_000_0,  //   59  0x3c   60  ↑
    0x0027_027_1, 0x0027_072_1, 0x0000_000_0,  //   60  0x3d   61  →
    0x0028_028_1, 0x0028_074_1, 0x0000_000_0,  //   61  0x3e   62  ↓
    0x0090_090_4, 0x0000_000_0, 0x0000_000_0,  //   62  0x3f   63  CLR
    0x006f_06f_4, 0x0000_000_0, 0x0000_000_0,  //   63  0x40   64  ／
    0x006a_06a_4, 0x0000_000_0, 0x0000_000_0,  //   64  0x41   65  ＊
    0x006d_06d_4, 0x0000_000_0, 0x0000_000_0,  //   65  0x42   66  －
    0x0024_024_4, 0x0067_067_4, 0x0000_000_0,  //   66  0x43   67  ７
    0x0026_026_4, 0x0068_068_4, 0x0000_000_0,  //   67  0x44   68  ８
    0x0021_021_4, 0x0069_069_4, 0x0000_000_0,  //   68  0x45   69  ９
    0x006b_06b_4, 0x0000_000_0, 0x0000_000_0,  //   69  0x46   70  ＋
    0x0025_025_4, 0x0064_064_4, 0x0000_000_0,  //   70  0x47   71  ４
    0x000c_00c_4, 0x0065_065_4, 0x0000_000_0,  //   71  0x48   72  ５
    0x0027_027_4, 0x0066_066_4, 0x0000_000_0,  //   72  0x49   73  ６
    0x0000_000_0, 0x0000_000_0, 0x0000_000_0,  //   73  0x4a   74  ＝
    0x0023_023_4, 0x0061_061_4, 0x0000_000_0,  //   74  0x4b   75  １
    0x0028_028_4, 0x0062_062_4, 0x0000_000_0,  //   75  0x4c   76  ２
    0x0022_022_4, 0x0063_063_4, 0x0000_000_0,  //   76  0x4d   77  ３
    0x000a_00d_4, 0x0000_000_0, 0x0000_000_0,  //   77  0x4e   78  ENTER
    0x009b_02d_4, 0x0060_060_4, 0x0000_000_0,  //   78  0x4f   79  ０
    0x0000_000_0, 0x0000_000_0, 0x0000_000_0,  //   79  0x50   80  ，
    0x007f_02e_4, 0x006e_06e_4, 0x0000_000_0,  //   80  0x51   81  ．
    0x0000_000_0, 0x0000_000_0, 0x0000_000_0,  //   81  0x52   82  記号入力
    0x0000_000_0, 0x0000_000_0, 0x0000_000_0,  //   82  0x53   83  登録
    0x0000_000_0, 0x0000_000_0, 0x0000_000_0,  //   83  0x54   84  HELP
    0x0012_012_2, 0x0012_040_2, 0x0000_000_0,  //   84  0x55   85  XF1
    0x001d_01d_1, 0x001d_066_1, 0x0000_000_0,  //   85  0x56   86  XF2
    0x0012_012_3, 0x0012_06c_3, 0x0000_000_0,  //   86  0x57   87  XF3
    0x020d_05d_1, 0x0000_000_0, 0x0000_000_0,  //   87  0x58   88  XF4
    0x0011_011_3, 0x0000_000_0, 0x0000_000_0,  //   88  0x59   89  XF5
    0x0000_000_0, 0x0000_000_0, 0x0000_000_0,  //   89  0x5a   90  かな
    0x0000_000_0, 0x0000_000_0, 0x0000_000_0,  //   90  0x5b   91  ローマ字
    0x0000_000_0, 0x0000_000_0, 0x0000_000_0,  //   91  0x5c   92  コード入力
    0x0014_014_1, 0x0000_000_0, 0x0000_000_0,  //   92  0x5d   93  CAPS
    0x009b_02d_1, 0x009b_076_1, 0x0000_000_0,  //   93  0x5e   94  INS
    0x020c_05b_2, 0x0000_000_0, 0x0000_000_0,  //   94  0x5f   95  ひらがな
    0x020c_05c_3, 0x0000_000_0, 0x0000_000_0,  //   95  0x60   96  全角
    0x0013_013_1, 0x0000_000_0, 0x0000_000_0,  //   96  0x61   97  BREAK
    0x0000_000_0, 0x0000_000_0, 0x0000_000_0,  //   97  0x62   98  COPY
    0x0070_070_1, 0x0070_043_1, 0x0000_000_0,  //   98  0x63   99  F1
    0x0071_071_1, 0x0071_044_1, 0x0000_000_0,  //   99  0x64  100  F2
    0x0072_072_1, 0x0072_045_1, 0x0000_000_0,  //  100  0x65  101  F3
    0x0073_073_1, 0x0073_046_1, 0x0000_000_0,  //  101  0x66  102  F4
    0x0074_074_1, 0x0074_047_1, 0x0000_000_0,  //  102  0x67  103  F5
    0x0075_075_1, 0x0075_048_1, 0x0000_000_0,  //  103  0x68  104  F6
    0x0076_076_1, 0x0076_049_1, 0x0000_000_0,  //  104  0x69  105  F7
    0x0077_077_1, 0x0077_04a_1, 0x0000_000_0,  //  105  0x6a  106  F8
    0x0078_078_1, 0x0078_04b_1, 0x0000_000_0,  //  106  0x6b  107  F9
    0x0079_079_1, 0x0079_04c_1, 0x0000_000_0,  //  107  0x6c  108  F10
    0x0010_010_3, 0x0010_032_2, 0x0010_03e_3,  //  108  0x70  112  SHIFT
    0x0011_011_2, 0x0011_042_2, 0x0000_000_0,  //  109  0x71  113  CTRL
    0x007a_05f_1, 0x0000_000_0, 0x0000_000_0,  //  110  0x72  114  OPT.1
    0x007b_060_1, 0x0000_000_0, 0x0000_000_0,  //  111  0x73  115  OPT.2
    0x0000_000_0, 0x0000_000_0, 0x0000_000_0,  //  112  0x74  116  NUM
  };

  //Thanks to tantan for providing the data for the macOS US layout.
  //
  //  KBD_DEFAULT_MAP_MAC_US
  //    macOS + USキーボード 用 XEiJ デフォルトキーマッピング
  //
  //  macOS + USキーボード で XEiJ を使用する場合、KBD_DEFAULT_MAP_MAC のマッピングでは
  //  主要記号を含む一部の文字が入力できません。これは一部のキーが KeyEvent#getExtendedKeyCode() で
  //  JISキーボードとは異なる値を返すためです。
  //
  //  このマッピングを使うことでUSキーボードでもすべての記号文字が入力可能となります。
  //  逆にこのマッピングを使うとJISキーボードでは正しく入力できなくなるので注意してください。
  //
  //  また、M.Kamadaさん作 KeyWitch.X を使用して Human68K 側をUS(ASCII)配列にすると、
  //  SHIFTキーと組み合わせた記号入力も含め、XEiJ上で完全なUS配列となります。(推奨)
  //
  //  KeyWitch.X を組み込まない場合は、XEiJ上でほぼJISキーボードの配列になります。
  //
  //  KeyWitch.Xと68エミュレータについては以下を参考にさせて頂きました。
  //  https://yatte-mita.hateblo.jp/entry/2019/06/15/130053
  //
  //  KeyWitch.X は CONFIG.SYS に以下のように追加してください。
  //  DEVICE=\path\to\KeyWitch.x -e \path\to\ascii.env
  //
  public static final int[] KBD_DEFAULT_MAP_MAC_US = {
    //(keyCode&0xfff)<<16|(extendedKeyCode&0x0f000fff)<<4|(keyLocation&0xf)
    //e_ccc_eee_l    e_ccc_eee_l    e_ccc_eee_l        xo   hex  dec  text
    0x0_01b_01b_1, 0x0_000_000_0, 0x0_000_000_0,  //    0  0x01    1  ESC
    0x0_031_031_1, 0x0_031_205_1, 0x0_000_000_0,  //    1  0x02    2  １！ぬ　
    0x0_032_032_1, 0x0_032_200_1, 0x0_000_000_0,  //    2  0x03    3  ２＂ふ　      // 2番目を変更(@)
    0x0_033_033_1, 0x0_033_208_1, 0x0_000_000_0,  //    3  0x04    4  ３＃あぁ
    0x0_034_034_1, 0x0_034_203_1, 0x0_000_000_0,  //    4  0x05    5  ４＄うぅ
    0x0_035_035_1, 0x0_035_000_1, 0x0_000_000_0,  //    5  0x06    6  ５％えぇ
    0x0_036_036_1, 0x0_036_202_1, 0x0_000_000_0,  //    6  0x07    7  ６＆おぉ      // 2番目を変更(^)
    0x0_037_037_1, 0x0_037_096_1, 0x0_000_000_0,  //    7  0x08    8  ７＇やゃ      // 2番目を変更(&)
    0x0_038_038_1, 0x0_038_097_1, 0x0_000_000_0,  //    8  0x09    9  ８（ゆゅ      // 2番目を変更(*)
    0x0_039_039_1, 0x0_039_207_1, 0x0_000_000_0,  //    9  0x0a   10  ９）よょ      // 2番目を変更(()
    0x0_030_030_1, 0x0_030_20a_1, 0x0_000_000_0,  //   10  0x0b   11  ０　わを      // 2番目を変更())
    0x0_02d_02d_1, 0x0_02d_20b_1, 0x0_02d_000_1,  //   11  0x0c   12  －＝ほ　      // 2番目を変更(_)
    0x0_03d_03d_1, 0x0_03d_209_1, 0x0_03d_000_1,  //   12  0x0d   13  ＾～へ　      // 1,2番目を変更(=,+)　
    0x0_05c_05c_1, 0x1_05c_07c_1, 0x0_000_000_0,  //   13  0x0e   14  ￥｜ー　      // 1,2番目を変更(\,|)
    0x0_008_008_1, 0x0_000_000_0, 0x0_000_000_0,  //   14  0x0f   15  BS
    0x0_009_009_1, 0x0_000_000_0, 0x0_000_000_0,  //   15  0x10   16  TAB
    0x0_051_051_1, 0x0_051_000_1, 0x0_000_000_0,  //   16  0x11   17  Ｑ　た　
    0x0_057_057_1, 0x0_057_000_1, 0x0_000_000_0,  //   17  0x12   18  Ｗ　て　
    0x0_045_045_1, 0x0_045_000_1, 0x0_000_000_0,  //   18  0x13   19  Ｅ　いぃ
    0x0_052_052_1, 0x0_052_000_1, 0x0_000_000_0,  //   19  0x14   20  Ｒ　す　
    0x0_054_054_1, 0x0_054_000_1, 0x0_000_000_0,  //   20  0x15   21  Ｔ　か　
    0x0_059_059_1, 0x0_059_000_1, 0x0_000_000_0,  //   21  0x16   22  Ｙ　ん　
    0x0_055_055_1, 0x0_055_000_1, 0x0_000_000_0,  //   22  0x17   23  Ｕ　な　
    0x0_049_049_1, 0x0_049_000_1, 0x0_000_000_0,  //   23  0x18   24  Ｉ　に　
    0x0_04f_04f_1, 0x0_04f_000_1, 0x0_000_000_0,  //   24  0x19   25  Ｏ　ら　
    0x0_050_050_1, 0x0_050_000_1, 0x0_000_000_0,  //   25  0x1a   26  Ｐ　せ　
    0x0_05b_05b_1, 0x0_05b_0a1_1, 0x0_000_000_0,  //   26  0x1b   27  ＠｀゛　      // 1,2番目を変更([,{)
    0x0_05d_05d_1, 0x0_05d_0a2_1, 0x0_05d_01b_1,  //   27  0x1c   28  ［｛゜「      // 1,2番目を変更(],})
    0x0_00a_00a_1, 0x0_00a_000_1, 0x0_000_000_0,  //   28  0x1d   29  リターン      // 2番目を追加(SHIFT/CTRL同時押し)
    0x0_041_041_1, 0x0_041_000_1, 0x0_000_000_0,  //   29  0x1e   30  Ａ　ち　
    0x0_053_053_1, 0x0_053_000_1, 0x0_000_000_0,  //   30  0x1f   31  Ｓ　と　
    0x0_044_044_1, 0x0_044_000_1, 0x0_000_000_0,  //   31  0x20   32  Ｄ　し　
    0x0_046_046_1, 0x0_046_000_1, 0x0_000_000_0,  //   32  0x21   33  Ｆ　は　
    0x0_047_047_1, 0x0_047_000_1, 0x0_000_000_0,  //   33  0x22   34  Ｇ　き　
    0x0_048_048_1, 0x0_048_008_1, 0x0_000_000_0,  //   34  0x23   35  Ｈ　く　
    0x0_04a_04a_1, 0x0_04a_00a_1, 0x0_000_000_0,  //   35  0x24   36  Ｊ　ま　
    0x0_04b_04b_1, 0x0_04b_000_1, 0x0_000_000_0,  //   36  0x25   37  Ｋ　の　
    0x0_04c_04c_1, 0x0_04c_000_1, 0x0_000_000_0,  //   37  0x26   38  Ｌ　り　
    0x0_03b_03b_1, 0x0_03b_201_1, 0x0_000_000_0,  //   38  0x27   39  ；＋れ　      // 2番目を変更(:)
    0x0_0de_0de_1, 0x0_0de_098_1, 0x0_000_000_0,  //   39  0x28   40  ：＊け　      // 1,2番目を変更(',"),3番目削除
    0x0_0c0_0c0_1, 0x1_0c0_07e_1, 0x0_05c_000_1,  //   40  0x29   41  ］｝む」      // 1,2番目を変更(`,~)
    0x0_05a_05a_1, 0x0_05a_000_1, 0x0_000_000_0,  //   41  0x2a   42  Ｚ　つっ
    0x0_058_058_1, 0x0_058_000_1, 0x0_000_000_0,  //   42  0x2b   43  Ｘ　さ　
    0x0_043_043_1, 0x0_043_000_1, 0x0_000_000_0,  //   43  0x2c   44  Ｃ　そ　
    0x0_056_056_1, 0x0_056_000_1, 0x0_000_000_0,  //   44  0x2d   45  Ｖ　ひ　
    0x0_042_042_1, 0x0_042_000_1, 0x0_000_000_0,  //   45  0x2e   46  Ｂ　こ　
    0x0_04e_04e_1, 0x0_04e_000_1, 0x0_000_000_0,  //   46  0x2f   47  Ｎ　み　
    0x0_04d_04d_1, 0x0_04d_000_1, 0x0_000_000_0,  //   47  0x30   48  Ｍ　も　
    0x0_02c_02c_1, 0x0_02c_099_1, 0x0_000_000_0,  //   48  0x31   49  ，＜ね、
    0x0_02e_02e_1, 0x0_02e_0a0_1, 0x0_000_000_0,  //   49  0x32   50  ．＞る。
    0x0_02f_02f_1, 0x0_02f_000_1, 0x0_000_000_0,  //   50  0x33   51  ／？め・
    0x0_02d_20b_1, 0x0_20b_000_4, 0x0_000_000_0,  //   51  0x34   52  　＿ろ□        // 1番目を変更(_)
    0x0_020_020_1, 0x0_000_000_0, 0x0_000_000_0,  //   52  0x35   53  スペース
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   53  0x36   54  HOME
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   54  0x37   55  DEL
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   55  0x38   56  ROLLUP
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   56  0x39   57  ROLLDOWN
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   57  0x3a   58  UNDO
    0x0_025_025_1, 0x0_000_000_0, 0x0_000_000_0,  //   58  0x3b   59  ←
    0x0_026_026_1, 0x0_000_000_0, 0x0_000_000_0,  //   59  0x3c   60  ↑
    0x0_027_027_1, 0x0_000_000_0, 0x0_000_000_0,  //   60  0x3d   61  →
    0x0_028_028_1, 0x0_000_000_0, 0x0_000_000_0,  //   61  0x3e   62  ↓
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   62  0x3f   63  CLR
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   63  0x40   64  ／
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   64  0x41   65  ＊
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   65  0x42   66  －
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   66  0x43   67  ７
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   67  0x44   68  ８
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   68  0x45   69  ９
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   69  0x46   70  ＋
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   70  0x47   71  ４
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   71  0x48   72  ５
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   72  0x49   73  ６
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   73  0x4a   74  ＝
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   74  0x4b   75  １
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   75  0x4c   76  ２
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   76  0x4d   77  ３
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   77  0x4e   78  ENTER
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   78  0x4f   79  ０
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   79  0x50   80  ，
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   80  0x51   81  ．
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   81  0x52   82  記号入力
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   82  0x53   83  登録
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   83  0x54   84  HELP
    0x0_012_012_2, 0x0_000_000_0, 0x0_000_000_0,  //   84  0x55   85  XF1
    0x0_09d_09d_2, 0x0_000_000_0, 0x0_000_000_0,  //   85  0x56   86  XF2
    0x0_09d_09d_3, 0x0_000_000_0, 0x0_000_000_0,  //   86  0x57   87  XF3
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   87  0x58   88  XF4
    0x0_07b_07b_1, 0x0_000_000_0, 0x0_000_000_0,  //   88  0x59   89  XF5
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   89  0x5a   90  かな
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   90  0x5b   91  ローマ字
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   91  0x5c   92  コード入力
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   92  0x5d   93  CAPS
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   93  0x5e   94  INS
    0x0_014_014_1, 0x0_000_000_0, 0x0_000_000_0,  //   94  0x5f   95  ひらがな
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   95  0x60   96  全角
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   96  0x61   97  BREAK
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   97  0x62   98  COPY
    0x0_070_070_1, 0x0_000_000_0, 0x0_000_000_0,  //   98  0x63   99  F1
    0x0_071_071_1, 0x0_000_000_0, 0x0_000_000_0,  //   99  0x64  100  F2
    0x0_072_072_1, 0x0_000_000_0, 0x0_000_000_0,  //  100  0x65  101  F3
    0x0_073_073_1, 0x0_000_000_0, 0x0_000_000_0,  //  101  0x66  102  F4
    0x0_074_074_1, 0x0_000_000_0, 0x0_000_000_0,  //  102  0x67  103  F5
    0x0_075_075_1, 0x0_000_000_0, 0x0_000_000_0,  //  103  0x68  104  F6
    0x0_076_076_1, 0x0_000_000_0, 0x0_000_000_0,  //  104  0x69  105  F7
    0x0_077_077_1, 0x0_000_000_0, 0x0_000_000_0,  //  105  0x6a  106  F8
    0x0_078_078_1, 0x0_000_000_0, 0x0_000_000_0,  //  106  0x6b  107  F9
    0x0_079_079_1, 0x0_000_000_0, 0x0_000_000_0,  //  107  0x6c  108  F10
    0x0_010_010_2, 0x0_010_010_3, 0x0_000_000_0,  //  108  0x70  112  SHIFT
    0x0_011_011_2, 0x0_000_000_0, 0x0_000_000_0,  //  109  0x71  113  CTRL
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //  110  0x72  114  OPT.1
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //  111  0x73  115  OPT.2
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //  112  0x74  116  NUM
  };

  //Thanks to yuduki for providing the data for the macOS.
  public static final int[] KBD_DEFAULT_MAP_MAC = {
    //(keyCode&0xfff)<<16|(extendedKeyCode&0x0f000fff)<<4|(keyLocation&0xf)
    //e_ccc_eee_l    e_ccc_eee_l    e_ccc_eee_l        xo   hex  dec  text
    0x0_01b_01b_1, 0x0_000_000_0, 0x0_000_000_0,  //    0  0x01    1  ESC
    0x0_031_031_1, 0x0_031_205_1, 0x0_000_000_0,  //    1  0x02    2  １！ぬ　
    0x0_032_032_1, 0x0_032_098_1, 0x0_000_000_0,  //    2  0x03    3  ２＂ふ　
    0x0_033_033_1, 0x0_033_208_1, 0x0_000_000_0,  //    3  0x04    4  ３＃あぁ
    0x0_034_034_1, 0x0_034_203_1, 0x0_000_000_0,  //    4  0x05    5  ４＄うぅ
    0x0_035_035_1, 0x0_035_000_1, 0x0_000_000_0,  //    5  0x06    6  ５％えぇ
    0x0_036_036_1, 0x0_036_096_1, 0x0_000_000_0,  //    6  0x07    7  ６＆おぉ
    0x0_037_037_1, 0x0_037_0de_1, 0x0_000_000_0,  //    7  0x08    8  ７＇やゃ
    0x0_038_038_1, 0x0_038_207_1, 0x0_000_000_0,  //    8  0x09    9  ８（ゆゅ
    0x0_039_039_1, 0x0_039_20a_1, 0x0_000_000_0,  //    9  0x0a   10  ９）よょ
    0x0_030_030_1, 0x0_000_000_0, 0x0_000_000_0,  //   10  0x0b   11  ０　わを
    0x0_02d_02d_1, 0x0_02d_03d_1, 0x0_02d_000_1,  //   11  0x0c   12  －＝ほ　
    0x0_03d_202_1, 0x1_03d_07e_1, 0x0_03d_000_1,  //   12  0x0d   13  ＾～へ　
    0x1_05c_0a5_1, 0x1_05c_07c_1, 0x0_000_000_0,  //   13  0x0e   14  ￥｜ー　
    0x0_008_008_1, 0x0_000_000_0, 0x0_000_000_0,  //   14  0x0f   15  BS
    0x0_009_009_1, 0x0_000_000_0, 0x0_000_000_0,  //   15  0x10   16  TAB
    0x0_051_051_1, 0x0_051_000_1, 0x0_000_000_0,  //   16  0x11   17  Ｑ　た　
    0x0_057_057_1, 0x0_057_000_1, 0x0_000_000_0,  //   17  0x12   18  Ｗ　て　
    0x0_045_045_1, 0x0_045_000_1, 0x0_000_000_0,  //   18  0x13   19  Ｅ　いぃ
    0x0_052_052_1, 0x0_052_000_1, 0x0_000_000_0,  //   19  0x14   20  Ｒ　す　
    0x0_054_054_1, 0x0_054_000_1, 0x0_000_000_0,  //   20  0x15   21  Ｔ　か　
    0x0_059_059_1, 0x0_059_000_1, 0x0_000_000_0,  //   21  0x16   22  Ｙ　ん　
    0x0_055_055_1, 0x0_055_000_1, 0x0_000_000_0,  //   22  0x17   23  Ｕ　な　
    0x0_049_049_1, 0x0_049_000_1, 0x0_000_000_0,  //   23  0x18   24  Ｉ　に　
    0x0_04f_04f_1, 0x0_04f_000_1, 0x0_000_000_0,  //   24  0x19   25  Ｏ　ら　
    0x0_050_050_1, 0x0_050_000_1, 0x0_000_000_0,  //   25  0x1a   26  Ｐ　せ　
    0x0_05b_200_1, 0x0_05b_0c0_1, 0x0_05b_020_1,  //   26  0x1b   27  ＠｀゛　
    0x0_05d_05b_1, 0x0_05d_0a1_1, 0x0_05d_01b_1,  //   27  0x1c   28  ［｛゜「
    0x0_00a_00a_1, 0x0_000_000_0, 0x0_000_000_0,  //   28  0x1d   29  リターン
    0x0_041_041_1, 0x0_041_000_1, 0x0_000_000_0,  //   29  0x1e   30  Ａ　ち　
    0x0_053_053_1, 0x0_053_000_1, 0x0_000_000_0,  //   30  0x1f   31  Ｓ　と　
    0x0_044_044_1, 0x0_044_000_1, 0x0_000_000_0,  //   31  0x20   32  Ｄ　し　
    0x0_046_046_1, 0x0_046_000_1, 0x0_000_000_0,  //   32  0x21   33  Ｆ　は　
    0x0_047_047_1, 0x0_047_000_1, 0x0_000_000_0,  //   33  0x22   34  Ｇ　き　
    0x0_048_048_1, 0x0_048_008_1, 0x0_000_000_0,  //   34  0x23   35  Ｈ　く　
    0x0_04a_04a_1, 0x0_04a_00a_1, 0x0_000_000_0,  //   35  0x24   36  Ｊ　ま　
    0x0_04b_04b_1, 0x0_04b_000_1, 0x0_000_000_0,  //   36  0x25   37  Ｋ　の　
    0x0_04c_04c_1, 0x0_04c_000_1, 0x0_000_000_0,  //   37  0x26   38  Ｌ　り　
    0x0_03b_03b_1, 0x0_03b_209_1, 0x0_000_000_0,  //   38  0x27   39  ；＋れ　
    0x0_0de_201_1, 0x0_0de_097_1, 0x0_0de_0de_1,  //   39  0x28   40  ：＊け　
    0x0_05c_05d_1, 0x0_05c_0a2_1, 0x0_05c_000_1,  //   40  0x29   41  ］｝む」
    0x0_05a_05a_1, 0x0_05a_000_1, 0x0_000_000_0,  //   41  0x2a   42  Ｚ　つっ
    0x0_058_058_1, 0x0_058_000_1, 0x0_000_000_0,  //   42  0x2b   43  Ｘ　さ　
    0x0_043_043_1, 0x0_043_000_1, 0x0_000_000_0,  //   43  0x2c   44  Ｃ　そ　
    0x0_056_056_1, 0x0_056_000_1, 0x0_000_000_0,  //   44  0x2d   45  Ｖ　ひ　
    0x0_042_042_1, 0x0_042_000_1, 0x0_000_000_0,  //   45  0x2e   46  Ｂ　こ　
    0x0_04e_04e_1, 0x0_04e_000_1, 0x0_000_000_0,  //   46  0x2f   47  Ｎ　み　
    0x0_04d_04d_1, 0x0_04d_000_1, 0x0_000_000_0,  //   47  0x30   48  Ｍ　も　
    0x0_02c_02c_1, 0x0_02c_099_1, 0x0_000_000_0,  //   48  0x31   49  ，＜ね、
    0x0_02e_02e_1, 0x0_02e_0a0_1, 0x0_000_000_0,  //   49  0x32   50  ．＞る。
    0x0_02f_02f_1, 0x0_02f_000_1, 0x0_000_000_0,  //   50  0x33   51  ／？め・
    0x0_20b_20b_4, 0x0_20b_000_4, 0x0_000_000_0,  //   51  0x34   52  　＿ろ□
    0x0_020_020_1, 0x0_000_000_0, 0x0_000_000_0,  //   52  0x35   53  スペース
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   53  0x36   54  HOME
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   54  0x37   55  DEL
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   55  0x38   56  ROLLUP
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   56  0x39   57  ROLLDOWN
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   57  0x3a   58  UNDO
    0x0_025_025_1, 0x0_000_000_0, 0x0_000_000_0,  //   58  0x3b   59  ←
    0x0_026_026_1, 0x0_000_000_0, 0x0_000_000_0,  //   59  0x3c   60  ↑
    0x0_027_027_1, 0x0_000_000_0, 0x0_000_000_0,  //   60  0x3d   61  →
    0x0_028_028_1, 0x0_000_000_0, 0x0_000_000_0,  //   61  0x3e   62  ↓
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   62  0x3f   63  CLR
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   63  0x40   64  ／
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   64  0x41   65  ＊
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   65  0x42   66  －
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   66  0x43   67  ７
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   67  0x44   68  ８
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   68  0x45   69  ９
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   69  0x46   70  ＋
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   70  0x47   71  ４
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   71  0x48   72  ５
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   72  0x49   73  ６
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   73  0x4a   74  ＝
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   74  0x4b   75  １
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   75  0x4c   76  ２
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   76  0x4d   77  ３
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   77  0x4e   78  ENTER
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   78  0x4f   79  ０
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   79  0x50   80  ，
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   80  0x51   81  ．
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   81  0x52   82  記号入力
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   82  0x53   83  登録
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   83  0x54   84  HELP
    0x0_012_012_2, 0x0_000_000_0, 0x0_000_000_0,  //   84  0x55   85  XF1
    0x0_09d_09d_2, 0x0_000_000_0, 0x0_000_000_0,  //   85  0x56   86  XF2
    0x0_09d_09d_3, 0x0_000_000_0, 0x0_000_000_0,  //   86  0x57   87  XF3
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   87  0x58   88  XF4
    0x0_07b_07b_1, 0x0_000_000_0, 0x0_000_000_0,  //   88  0x59   89  XF5
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   89  0x5a   90  かな
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   90  0x5b   91  ローマ字
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   91  0x5c   92  コード入力
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   92  0x5d   93  CAPS
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   93  0x5e   94  INS
    0x0_014_014_1, 0x0_000_000_0, 0x0_000_000_0,  //   94  0x5f   95  ひらがな
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   95  0x60   96  全角
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   96  0x61   97  BREAK
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //   97  0x62   98  COPY
    0x0_070_070_1, 0x0_000_000_0, 0x0_000_000_0,  //   98  0x63   99  F1
    0x0_071_071_1, 0x0_000_000_0, 0x0_000_000_0,  //   99  0x64  100  F2
    0x0_072_072_1, 0x0_000_000_0, 0x0_000_000_0,  //  100  0x65  101  F3
    0x0_073_073_1, 0x0_000_000_0, 0x0_000_000_0,  //  101  0x66  102  F4
    0x0_074_074_1, 0x0_000_000_0, 0x0_000_000_0,  //  102  0x67  103  F5
    0x0_075_075_1, 0x0_000_000_0, 0x0_000_000_0,  //  103  0x68  104  F6
    0x0_076_076_1, 0x0_000_000_0, 0x0_000_000_0,  //  104  0x69  105  F7
    0x0_077_077_1, 0x0_000_000_0, 0x0_000_000_0,  //  105  0x6a  106  F8
    0x0_078_078_1, 0x0_000_000_0, 0x0_000_000_0,  //  106  0x6b  107  F9
    0x0_079_079_1, 0x0_000_000_0, 0x0_000_000_0,  //  107  0x6c  108  F10
    0x0_010_010_2, 0x0_010_010_3, 0x0_000_000_0,  //  108  0x70  112  SHIFT
    0x0_011_011_2, 0x0_000_000_0, 0x0_000_000_0,  //  109  0x71  113  CTRL
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //  110  0x72  114  OPT.1
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //  111  0x73  115  OPT.2
    0x0_000_000_0, 0x0_000_000_0, 0x0_000_000_0,  //  112  0x74  116  NUM
  };

  public static final int[] KBD_DEFAULT_MAP_WIN_US = {
    //(keyCode&0xffff)<<16|(rawCode&0xfff)<<4|(keyLocation&0xf)
    //cccc_rrr_l    cccc_rrr_l    cccc_rrr_l        xo   hex  dec  text
    0x001b_01b_1, 0x0000_000_0, 0x0000_000_0,  //    0  0x01    1  ESC
    0x0031_031_1, 0x0000_000_0, 0x0000_000_0,  //    1  0x02    2  １！ぬ　
    0x0032_032_1, 0x0000_000_0, 0x0000_000_0,  //    2  0x03    3  ２＂ふ　
    0x0033_033_1, 0x0000_000_0, 0x0000_000_0,  //    3  0x04    4  ３＃あぁ
    0x0034_034_1, 0x0000_000_0, 0x0000_000_0,  //    4  0x05    5  ４＄うぅ
    0x0035_035_1, 0x0000_000_0, 0x0000_000_0,  //    5  0x06    6  ５％えぇ
    0x0036_036_1, 0x0000_000_0, 0x0000_000_0,  //    6  0x07    7  ６＆おぉ
    0x0037_037_1, 0x0000_000_0, 0x0000_000_0,  //    7  0x08    8  ７＇やゃ
    0x0038_038_1, 0x0000_000_0, 0x0000_000_0,  //    8  0x09    9  ８（ゆゅ
    0x0039_039_1, 0x0000_000_0, 0x0000_000_0,  //    9  0x0a   10  ９）よょ
    0x0030_030_1, 0x0000_000_0, 0x0000_000_0,  //   10  0x0b   11  ０　わを
    0x002d_0bd_1, 0x0000_000_0, 0x0000_000_0,  //   11  0x0c   12  －＝ほ　
    0x003d_0bb_1, 0x0000_000_0, 0x0000_000_0,  //   12  0x0d   13  ＾～へ　
    0x00c0_0c0_1, 0x0000_0ff_1, 0x0000_000_0,  //   13  0x0e   14  ￥｜ー　
    0x0008_008_1, 0x0000_000_0, 0x0000_000_0,  //   14  0x0f   15  BS
    0x0009_009_1, 0x0000_000_0, 0x0000_000_0,  //   15  0x10   16  TAB
    0x0051_051_1, 0x0000_000_0, 0x0000_000_0,  //   16  0x11   17  Ｑ　た　
    0x0057_057_1, 0x0000_000_0, 0x0000_000_0,  //   17  0x12   18  Ｗ　て　
    0x0045_045_1, 0x0000_000_0, 0x0000_000_0,  //   18  0x13   19  Ｅ　いぃ
    0x0052_052_1, 0x0000_000_0, 0x0000_000_0,  //   19  0x14   20  Ｒ　す　
    0x0054_054_1, 0x0000_000_0, 0x0000_000_0,  //   20  0x15   21  Ｔ　か　
    0x0059_059_1, 0x0000_000_0, 0x0000_000_0,  //   21  0x16   22  Ｙ　ん　
    0x0055_055_1, 0x0000_000_0, 0x0000_000_0,  //   22  0x17   23  Ｕ　な　
    0x0049_049_1, 0x0000_000_0, 0x0000_000_0,  //   23  0x18   24  Ｉ　に　
    0x004f_04f_1, 0x0000_000_0, 0x0000_000_0,  //   24  0x19   25  Ｏ　ら　
    0x0050_050_1, 0x0000_000_0, 0x0000_000_0,  //   25  0x1a   26  Ｐ　せ　
    0x005b_0db_1, 0x0000_000_0, 0x0000_000_0,  //   26  0x1b   27  ＠｀゛　
    0x005d_0dd_1, 0x0000_000_0, 0x0000_000_0,  //   27  0x1c   28  ［｛゜「
    0x000a_00d_1, 0x0000_000_0, 0x0000_000_0,  //   28  0x1d   29  リターン
    0x0041_041_1, 0x0000_000_0, 0x0000_000_0,  //   29  0x1e   30  Ａ　ち　
    0x0053_053_1, 0x0000_000_0, 0x0000_000_0,  //   30  0x1f   31  Ｓ　と　
    0x0044_044_1, 0x0000_000_0, 0x0000_000_0,  //   31  0x20   32  Ｄ　し　
    0x0046_046_1, 0x0000_000_0, 0x0000_000_0,  //   32  0x21   33  Ｆ　は　
    0x0047_047_1, 0x0000_000_0, 0x0000_000_0,  //   33  0x22   34  Ｇ　き　
    0x0048_048_1, 0x0000_000_0, 0x0000_000_0,  //   34  0x23   35  Ｈ　く　
    0x004a_04a_1, 0x0000_000_0, 0x0000_000_0,  //   35  0x24   36  Ｊ　ま　
    0x004b_04b_1, 0x0000_000_0, 0x0000_000_0,  //   36  0x25   37  Ｋ　の　
    0x004c_04c_1, 0x0000_000_0, 0x0000_000_0,  //   37  0x26   38  Ｌ　り　
    0x003b_0ba_1, 0x0000_000_0, 0x0000_000_0,  //   38  0x27   39  ；＋れ　
    0x00de_0de_1, 0x0000_000_0, 0x0000_000_0,  //   39  0x28   40  ：＊け　
    0x005c_0dc_1, 0x0000_000_0, 0x0000_000_0,  //   40  0x29   41  ］｝む」
    0x005a_05a_1, 0x0000_000_0, 0x0000_000_0,  //   41  0x2a   42  Ｚ　つっ
    0x0058_058_1, 0x0000_000_0, 0x0000_000_0,  //   42  0x2b   43  Ｘ　さ　
    0x0043_043_1, 0x0000_000_0, 0x0000_000_0,  //   43  0x2c   44  Ｃ　そ　
    0x0056_056_1, 0x0000_000_0, 0x0000_000_0,  //   44  0x2d   45  Ｖ　ひ　
    0x0042_042_1, 0x0000_000_0, 0x0000_000_0,  //   45  0x2e   46  Ｂ　こ　
    0x004e_04e_1, 0x0000_000_0, 0x0000_000_0,  //   46  0x2f   47  Ｎ　み　
    0x004d_04d_1, 0x0000_000_0, 0x0000_000_0,  //   47  0x30   48  Ｍ　も　
    0x002c_0bc_1, 0x0000_000_0, 0x0000_000_0,  //   48  0x31   49  ，＜ね、
    0x002e_0be_1, 0x0000_000_0, 0x0000_000_0,  //   49  0x32   50  ．＞る。
    0x002f_0bf_1, 0x0000_000_0, 0x0000_000_0,  //   50  0x33   51  ／？め・
    0x0000_0c1_1, 0x0000_000_0, 0x0000_000_0,  //   51  0x34   52  　＿ろ□
    0x0020_020_1, 0x0000_000_0, 0x0000_000_0,  //   52  0x35   53  スペース
    0x0024_024_1, 0x0000_000_0, 0x0000_000_0,  //   53  0x36   54  HOME
    0x007f_02e_1, 0x0000_000_0, 0x0000_000_0,  //   54  0x37   55  DEL
    0x0022_022_1, 0x0000_000_0, 0x0000_000_0,  //   55  0x38   56  ROLLUP
    0x0021_021_1, 0x0000_000_0, 0x0000_000_0,  //   56  0x39   57  ROLLDOWN
    0x0023_023_1, 0x0000_000_0, 0x0000_000_0,  //   57  0x3a   58  UNDO
    0x0025_025_1, 0x0000_000_0, 0x0000_000_0,  //   58  0x3b   59  ←
    0x0026_026_1, 0x0000_000_0, 0x0000_000_0,  //   59  0x3c   60  ↑
    0x0027_027_1, 0x0000_000_0, 0x0000_000_0,  //   60  0x3d   61  →
    0x0028_028_1, 0x0000_000_0, 0x0000_000_0,  //   61  0x3e   62  ↓
    0x0090_090_4, 0x0000_000_0, 0x0000_000_0,  //   62  0x3f   63  CLR
    0x006f_06f_4, 0x0000_000_0, 0x0000_000_0,  //   63  0x40   64  ／
    0x006a_06a_4, 0x0000_000_0, 0x0000_000_0,  //   64  0x41   65  ＊
    0x006d_06d_4, 0x0000_000_0, 0x0000_000_0,  //   65  0x42   66  －
    0x0024_024_4, 0x0067_067_4, 0x0000_000_0,  //   66  0x43   67  ７
    0x0026_026_4, 0x0068_068_4, 0x0000_000_0,  //   67  0x44   68  ８
    0x0021_021_4, 0x0069_069_4, 0x0000_000_0,  //   68  0x45   69  ９
    0x006b_06b_4, 0x0000_000_0, 0x0000_000_0,  //   69  0x46   70  ＋
    0x0025_025_4, 0x0064_064_4, 0x0000_000_0,  //   70  0x47   71  ４
    0x000c_00c_4, 0x0065_065_4, 0x0000_000_0,  //   71  0x48   72  ５
    0x0027_027_4, 0x0066_066_4, 0x0000_000_0,  //   72  0x49   73  ６
    0x0000_000_0, 0x0000_000_0, 0x0000_000_0,  //   73  0x4a   74  ＝
    0x0023_023_4, 0x0061_061_4, 0x0000_000_0,  //   74  0x4b   75  １
    0x0028_028_4, 0x0062_062_4, 0x0000_000_0,  //   75  0x4c   76  ２
    0x0022_022_4, 0x0063_063_4, 0x0000_000_0,  //   76  0x4d   77  ３
    0x000a_00d_4, 0x0000_000_0, 0x0000_000_0,  //   77  0x4e   78  ENTER
    0x009b_02d_4, 0x0060_060_4, 0x0000_000_0,  //   78  0x4f   79  ０
    0x0000_000_0, 0x0000_000_0, 0x0000_000_0,  //   79  0x50   80  ，
    0x007f_02e_4, 0x006e_06e_4, 0x0000_000_0,  //   80  0x51   81  ．
    0x0000_000_0, 0x0000_000_0, 0x0000_000_0,  //   81  0x52   82  記号入力
    0x0000_000_0, 0x0000_000_0, 0x0000_000_0,  //   82  0x53   83  登録
    0x0000_000_0, 0x0000_000_0, 0x0000_000_0,  //   83  0x54   84  HELP
    0x0012_012_2, 0x0000_000_0, 0x0000_000_0,  //   84  0x55   85  XF1
    0x0000_0eb_1, 0x0000_000_0, 0x0000_000_0,  //   85  0x56   86  XF2
    0x0012_012_3, 0x0000_000_0, 0x0000_000_0,  //   86  0x57   87  XF3
    0x020d_05d_1, 0x0000_000_0, 0x0000_000_0,  //   87  0x58   88  XF4
    0x0011_011_3, 0x0000_000_0, 0x0000_000_0,  //   88  0x59   89  XF5
    0x0000_000_0, 0x0000_000_0, 0x0000_000_0,  //   89  0x5a   90  かな
    0x0000_000_0, 0x0000_000_0, 0x0000_000_0,  //   90  0x5b   91  ローマ字
    0x0000_000_0, 0x0000_000_0, 0x0000_000_0,  //   91  0x5c   92  コード入力
    0x0014_014_1, 0x0000_000_0, 0x0000_000_0,  //   92  0x5d   93  CAPS
    0x009b_02d_1, 0x0000_000_0, 0x0000_000_0,  //   93  0x5e   94  INS
    0x020c_05b_2, 0x0000_000_0, 0x0000_000_0,  //   94  0x5f   95  ひらがな
    0x020c_05c_3, 0x0000_000_0, 0x0000_000_0,  //   95  0x60   96  全角
    0x0013_013_1, 0x0000_000_0, 0x0000_000_0,  //   96  0x61   97  BREAK
    0x0000_000_0, 0x0000_000_0, 0x0000_000_0,  //   97  0x62   98  COPY
    0x0070_070_1, 0x0000_000_0, 0x0000_000_0,  //   98  0x63   99  F1
    0x0071_071_1, 0x0000_000_0, 0x0000_000_0,  //   99  0x64  100  F2
    0x0072_072_1, 0x0000_000_0, 0x0000_000_0,  //  100  0x65  101  F3
    0x0073_073_1, 0x0000_000_0, 0x0000_000_0,  //  101  0x66  102  F4
    0x0074_074_1, 0x0000_000_0, 0x0000_000_0,  //  102  0x67  103  F5
    0x0075_075_1, 0x0000_000_0, 0x0000_000_0,  //  103  0x68  104  F6
    0x0076_076_1, 0x0000_000_0, 0x0000_000_0,  //  104  0x69  105  F7
    0x0077_077_1, 0x0000_000_0, 0x0000_000_0,  //  105  0x6a  106  F8
    0x0078_078_1, 0x0000_000_0, 0x0000_000_0,  //  106  0x6b  107  F9
    0x0079_079_1, 0x0000_000_0, 0x0000_000_0,  //  107  0x6c  108  F10
    0x0010_010_2, 0x0010_010_3, 0x0000_000_0,  //  108  0x70  112  SHIFT
    0x0011_011_2, 0x0000_000_0, 0x0000_000_0,  //  109  0x71  113  CTRL
    0x0000_000_0, 0x0000_000_0, 0x0000_000_0,  //  110  0x72  114  OPT.1
    0x0000_000_0, 0x0000_000_0, 0x0000_000_0,  //  111  0x73  115  OPT.2
    0x0000_000_0, 0x0000_000_0, 0x0000_000_0,  //  112  0x74  116  NUM
  };

  public static final int[] KBD_DEFAULT_MAP_WIN = {
    //(keyCode&0xffff)<<16|(rawCode&0xfff)<<4|(keyLocation&0xf)
    //cccc_rrr_l    cccc_rrr_l    cccc_rrr_l        xo   hex  dec  text
    0x001b_01b_1, 0x0000_000_0, 0x0000_000_0,  //    0  0x01    1  ESC
    0x0031_031_1, 0x0000_000_0, 0x0000_000_0,  //    1  0x02    2  １！ぬ　
    0x0032_032_1, 0x0000_000_0, 0x0000_000_0,  //    2  0x03    3  ２＂ふ　
    0x0033_033_1, 0x0000_000_0, 0x0000_000_0,  //    3  0x04    4  ３＃あぁ
    0x0034_034_1, 0x0000_000_0, 0x0000_000_0,  //    4  0x05    5  ４＄うぅ
    0x0035_035_1, 0x0000_000_0, 0x0000_000_0,  //    5  0x06    6  ５％えぇ
    0x0036_036_1, 0x0000_000_0, 0x0000_000_0,  //    6  0x07    7  ６＆おぉ
    0x0037_037_1, 0x0000_000_0, 0x0000_000_0,  //    7  0x08    8  ７＇やゃ
    0x0038_038_1, 0x0000_000_0, 0x0000_000_0,  //    8  0x09    9  ８（ゆゅ
    0x0039_039_1, 0x0000_000_0, 0x0000_000_0,  //    9  0x0a   10  ９）よょ
    0x0030_030_1, 0x0000_000_0, 0x0000_000_0,  //   10  0x0b   11  ０　わを
    0x002d_0bd_1, 0x0000_000_0, 0x0000_000_0,  //   11  0x0c   12  －＝ほ　
    0x0202_0de_1, 0x0000_000_0, 0x0000_000_0,  //   12  0x0d   13  ＾～へ　
    0x005c_0dc_1, 0x0000_000_0, 0x0000_000_0,  //   13  0x0e   14  ￥｜ー　
    0x0008_008_1, 0x0000_000_0, 0x0000_000_0,  //   14  0x0f   15  BS
    0x0009_009_1, 0x0000_000_0, 0x0000_000_0,  //   15  0x10   16  TAB
    0x0051_051_1, 0x0000_000_0, 0x0000_000_0,  //   16  0x11   17  Ｑ　た　
    0x0057_057_1, 0x0000_000_0, 0x0000_000_0,  //   17  0x12   18  Ｗ　て　
    0x0045_045_1, 0x0000_000_0, 0x0000_000_0,  //   18  0x13   19  Ｅ　いぃ
    0x0052_052_1, 0x0000_000_0, 0x0000_000_0,  //   19  0x14   20  Ｒ　す　
    0x0054_054_1, 0x0000_000_0, 0x0000_000_0,  //   20  0x15   21  Ｔ　か　
    0x0059_059_1, 0x0000_000_0, 0x0000_000_0,  //   21  0x16   22  Ｙ　ん　
    0x0055_055_1, 0x0000_000_0, 0x0000_000_0,  //   22  0x17   23  Ｕ　な　
    0x0049_049_1, 0x0000_000_0, 0x0000_000_0,  //   23  0x18   24  Ｉ　に　
    0x004f_04f_1, 0x0000_000_0, 0x0000_000_0,  //   24  0x19   25  Ｏ　ら　
    0x0050_050_1, 0x0000_000_0, 0x0000_000_0,  //   25  0x1a   26  Ｐ　せ　
    0x0200_0c0_1, 0x0000_000_0, 0x0000_000_0,  //   26  0x1b   27  ＠｀゛　
    0x005b_0db_1, 0x0000_000_0, 0x0000_000_0,  //   27  0x1c   28  ［｛゜「
    0x000a_00d_1, 0x0000_000_0, 0x0000_000_0,  //   28  0x1d   29  リターン
    0x0041_041_1, 0x0000_000_0, 0x0000_000_0,  //   29  0x1e   30  Ａ　ち　
    0x0053_053_1, 0x0000_000_0, 0x0000_000_0,  //   30  0x1f   31  Ｓ　と　
    0x0044_044_1, 0x0000_000_0, 0x0000_000_0,  //   31  0x20   32  Ｄ　し　
    0x0046_046_1, 0x0000_000_0, 0x0000_000_0,  //   32  0x21   33  Ｆ　は　
    0x0047_047_1, 0x0000_000_0, 0x0000_000_0,  //   33  0x22   34  Ｇ　き　
    0x0048_048_1, 0x0000_000_0, 0x0000_000_0,  //   34  0x23   35  Ｈ　く　
    0x004a_04a_1, 0x0000_000_0, 0x0000_000_0,  //   35  0x24   36  Ｊ　ま　
    0x004b_04b_1, 0x0000_000_0, 0x0000_000_0,  //   36  0x25   37  Ｋ　の　
    0x004c_04c_1, 0x0000_000_0, 0x0000_000_0,  //   37  0x26   38  Ｌ　り　
    0x003b_0bb_1, 0x0000_000_0, 0x0000_000_0,  //   38  0x27   39  ；＋れ　
    0x0201_0ba_1, 0x0000_000_0, 0x0000_000_0,  //   39  0x28   40  ：＊け　
    0x005d_0dd_1, 0x0000_000_0, 0x0000_000_0,  //   40  0x29   41  ］｝む」
    0x005a_05a_1, 0x0000_000_0, 0x0000_000_0,  //   41  0x2a   42  Ｚ　つっ
    0x0058_058_1, 0x0000_000_0, 0x0000_000_0,  //   42  0x2b   43  Ｘ　さ　
    0x0043_043_1, 0x0000_000_0, 0x0000_000_0,  //   43  0x2c   44  Ｃ　そ　
    0x0056_056_1, 0x0000_000_0, 0x0000_000_0,  //   44  0x2d   45  Ｖ　ひ　
    0x0042_042_1, 0x0000_000_0, 0x0000_000_0,  //   45  0x2e   46  Ｂ　こ　
    0x004e_04e_1, 0x0000_000_0, 0x0000_000_0,  //   46  0x2f   47  Ｎ　み　
    0x004d_04d_1, 0x0000_000_0, 0x0000_000_0,  //   47  0x30   48  Ｍ　も　
    0x002c_0bc_1, 0x0000_000_0, 0x0000_000_0,  //   48  0x31   49  ，＜ね、
    0x002e_0be_1, 0x0000_000_0, 0x0000_000_0,  //   49  0x32   50  ．＞る。
    0x002f_0bf_1, 0x0000_000_0, 0x0000_000_0,  //   50  0x33   51  ／？め・
    0x005c_0e2_1, 0x0000_000_0, 0x0000_000_0,  //   51  0x34   52  　＿ろ□
    0x0020_020_1, 0x0000_000_0, 0x0000_000_0,  //   52  0x35   53  スペース
    0x0024_024_1, 0x0000_000_0, 0x0000_000_0,  //   53  0x36   54  HOME
    0x007f_02e_1, 0x0000_000_0, 0x0000_000_0,  //   54  0x37   55  DEL
    0x0022_022_1, 0x0000_000_0, 0x0000_000_0,  //   55  0x38   56  ROLLUP
    0x0021_021_1, 0x0000_000_0, 0x0000_000_0,  //   56  0x39   57  ROLLDOWN
    0x0023_023_1, 0x0000_000_0, 0x0000_000_0,  //   57  0x3a   58  UNDO
    0x0025_025_1, 0x0000_000_0, 0x0000_000_0,  //   58  0x3b   59  ←
    0x0026_026_1, 0x0000_000_0, 0x0000_000_0,  //   59  0x3c   60  ↑
    0x0027_027_1, 0x0000_000_0, 0x0000_000_0,  //   60  0x3d   61  →
    0x0028_028_1, 0x0000_000_0, 0x0000_000_0,  //   61  0x3e   62  ↓
    0x0090_090_4, 0x0000_000_0, 0x0000_000_0,  //   62  0x3f   63  CLR
    0x006f_06f_4, 0x0000_000_0, 0x0000_000_0,  //   63  0x40   64  ／
    0x006a_06a_4, 0x0000_000_0, 0x0000_000_0,  //   64  0x41   65  ＊
    0x006d_06d_4, 0x0000_000_0, 0x0000_000_0,  //   65  0x42   66  －
    0x0024_024_4, 0x0067_067_4, 0x0000_000_0,  //   66  0x43   67  ７
    0x0026_026_4, 0x0068_068_4, 0x0000_000_0,  //   67  0x44   68  ８
    0x0021_021_4, 0x0069_069_4, 0x0000_000_0,  //   68  0x45   69  ９
    0x006b_06b_4, 0x0000_000_0, 0x0000_000_0,  //   69  0x46   70  ＋
    0x0025_025_4, 0x0064_064_4, 0x0000_000_0,  //   70  0x47   71  ４
    0x000c_00c_4, 0x0065_065_4, 0x0000_000_0,  //   71  0x48   72  ５
    0x0027_027_4, 0x0066_066_4, 0x0000_000_0,  //   72  0x49   73  ６
    0x0000_000_0, 0x0000_000_0, 0x0000_000_0,  //   73  0x4a   74  ＝
    0x0023_023_4, 0x0061_061_4, 0x0000_000_0,  //   74  0x4b   75  １
    0x0028_028_4, 0x0062_062_4, 0x0000_000_0,  //   75  0x4c   76  ２
    0x0022_022_4, 0x0063_063_4, 0x0000_000_0,  //   76  0x4d   77  ３
    0x000a_00d_4, 0x0000_000_0, 0x0000_000_0,  //   77  0x4e   78  ENTER
    0x009b_02d_4, 0x0060_060_4, 0x0000_000_0,  //   78  0x4f   79  ０
    0x0000_000_0, 0x0000_000_0, 0x0000_000_0,  //   79  0x50   80  ，
    0x007f_02e_4, 0x006e_06e_4, 0x0000_000_0,  //   80  0x51   81  ．
    0x0000_000_0, 0x0000_000_0, 0x0000_000_0,  //   81  0x52   82  記号入力
    0x0000_000_0, 0x0000_000_0, 0x0000_000_0,  //   82  0x53   83  登録
    0x0000_000_0, 0x0000_000_0, 0x0000_000_0,  //   83  0x54   84  HELP
    0x0012_012_2, 0x0000_000_0, 0x0000_000_0,  //   84  0x55   85  XF1          左Alt
    0x001d_01d_1, 0x0000_000_0, 0x0000_000_0,  //   85  0x56   86  XF2          無変換
    0x0012_012_3, 0x0000_000_0, 0x0000_000_0,  //   86  0x57   87  XF3          右Alt
    0x020d_05d_1, 0x0000_000_0, 0x0000_000_0,  //   87  0x58   88  XF4          メニュー。Fn+右Ctrl
    0x0011_011_3, 0x0000_000_0, 0x0000_000_0,  //   88  0x59   89  XF5          右Ctrl
    0x0000_000_0, 0x0000_000_0, 0x0000_000_0,  //   89  0x5a   90  かな
    0x0000_000_0, 0x0000_000_0, 0x0000_000_0,  //   90  0x5b   91  ローマ字
    0x0000_000_0, 0x0000_000_0, 0x0000_000_0,  //   91  0x5c   92  コード入力
    0x0014_014_1, 0x0000_000_0, 0x0000_000_0,  //   92  0x5d   93  CAPS
    0x009b_02d_1, 0x0000_000_0, 0x0000_000_0,  //   93  0x5e   94  INS
    0x020c_05b_2, 0x0000_000_0, 0x0000_000_0,  //   94  0x5f   95  ひらがな
    0x020c_05c_3, 0x0000_000_0, 0x0000_000_0,  //   95  0x60   96  全角
    0x0013_013_1, 0x0000_000_0, 0x0000_000_0,  //   96  0x61   97  BREAK
    0x0000_000_0, 0x0000_000_0, 0x0000_000_0,  //   97  0x62   98  COPY
    0x0070_070_1, 0x0000_000_0, 0x0000_000_0,  //   98  0x63   99  F1
    0x0071_071_1, 0x0000_000_0, 0x0000_000_0,  //   99  0x64  100  F2
    0x0072_072_1, 0x0000_000_0, 0x0000_000_0,  //  100  0x65  101  F3
    0x0073_073_1, 0x0000_000_0, 0x0000_000_0,  //  101  0x66  102  F4
    0x0074_074_1, 0x0000_000_0, 0x0000_000_0,  //  102  0x67  103  F5
    0x0075_075_1, 0x0000_000_0, 0x0000_000_0,  //  103  0x68  104  F6
    0x0076_076_1, 0x0000_000_0, 0x0000_000_0,  //  104  0x69  105  F7
    0x0077_077_1, 0x0000_000_0, 0x0000_000_0,  //  105  0x6a  106  F8
    0x0078_078_1, 0x0000_000_0, 0x0000_000_0,  //  106  0x6b  107  F9
    0x0079_079_1, 0x0000_000_0, 0x0000_000_0,  //  107  0x6c  108  F10
    0x0010_010_2, 0x0010_010_3, 0x0000_000_0,  //  108  0x70  112  SHIFT
    0x0011_011_2, 0x0000_000_0, 0x0000_000_0,  //  109  0x71  113  CTRL
    0x0000_000_0, 0x0000_000_0, 0x0000_000_0,  //  110  0x72  114  OPT.1
    0x0000_000_0, 0x0000_000_0, 0x0000_000_0,  //  111  0x73  115  OPT.2
    0x0000_000_0, 0x0000_000_0, 0x0000_000_0,  //  112  0x74  116  NUM
  };

  public static final int[] KBD_DEFAULT_MAP_ZKEY = {
    //(keyCode&0xffff)<<16|(rawCode&0xfff)<<4|(keyLocation&0xf)
    //cccc_rrr_l    cccc_rrr_l    cccc_rrr_l        xo   hex  dec  text
    0x001b_01b_1, 0x0000_000_0, 0x0000_000_0,  //    0  0x01    1  ESC
    0x0031_031_1, 0x0000_000_0, 0x0000_000_0,  //    1  0x02    2  １！ぬ　
    0x0032_032_1, 0x0000_000_0, 0x0000_000_0,  //    2  0x03    3  ２＂ふ　
    0x0033_033_1, 0x0000_000_0, 0x0000_000_0,  //    3  0x04    4  ３＃あぁ
    0x0034_034_1, 0x0000_000_0, 0x0000_000_0,  //    4  0x05    5  ４＄うぅ
    0x0035_035_1, 0x0000_000_0, 0x0000_000_0,  //    5  0x06    6  ５％えぇ
    0x0036_036_1, 0x0000_000_0, 0x0000_000_0,  //    6  0x07    7  ６＆おぉ
    0x0037_037_1, 0x0000_000_0, 0x0000_000_0,  //    7  0x08    8  ７＇やゃ
    0x0038_038_1, 0x0000_000_0, 0x0000_000_0,  //    8  0x09    9  ８（ゆゅ
    0x0039_039_1, 0x0000_000_0, 0x0000_000_0,  //    9  0x0a   10  ９）よょ
    0x0030_030_1, 0x0000_000_0, 0x0000_000_0,  //   10  0x0b   11  ０　わを
    0x002d_0bd_1, 0x0000_000_0, 0x0000_000_0,  //   11  0x0c   12  －＝ほ　
    0x0202_0de_1, 0x0000_000_0, 0x0000_000_0,  //   12  0x0d   13  ＾～へ　
    0x005c_0dc_1, 0x0000_000_0, 0x0000_000_0,  //   13  0x0e   14  ￥｜ー　
    0x0008_008_1, 0x0000_000_0, 0x0000_000_0,  //   14  0x0f   15  BS
    0x0009_009_1, 0x0000_000_0, 0x0000_000_0,  //   15  0x10   16  TAB
    0x0051_051_1, 0x0000_000_0, 0x0000_000_0,  //   16  0x11   17  Ｑ　た　
    0x0057_057_1, 0x0000_000_0, 0x0000_000_0,  //   17  0x12   18  Ｗ　て　
    0x0045_045_1, 0x0000_000_0, 0x0000_000_0,  //   18  0x13   19  Ｅ　いぃ
    0x0052_052_1, 0x0000_000_0, 0x0000_000_0,  //   19  0x14   20  Ｒ　す　
    0x0054_054_1, 0x0000_000_0, 0x0000_000_0,  //   20  0x15   21  Ｔ　か　
    0x0059_059_1, 0x0000_000_0, 0x0000_000_0,  //   21  0x16   22  Ｙ　ん　
    0x0055_055_1, 0x0000_000_0, 0x0000_000_0,  //   22  0x17   23  Ｕ　な　
    0x0049_049_1, 0x0000_000_0, 0x0000_000_0,  //   23  0x18   24  Ｉ　に　
    0x004f_04f_1, 0x0000_000_0, 0x0000_000_0,  //   24  0x19   25  Ｏ　ら　
    0x0050_050_1, 0x0000_000_0, 0x0000_000_0,  //   25  0x1a   26  Ｐ　せ　
    0x0200_0c0_1, 0x0000_000_0, 0x0000_000_0,  //   26  0x1b   27  ＠｀゛　
    0x005b_0db_1, 0x0000_000_0, 0x0000_000_0,  //   27  0x1c   28  ［｛゜「
    0x000a_00d_1, 0x0000_000_0, 0x0000_000_0,  //   28  0x1d   29  リターン
    0x0041_041_1, 0x0000_000_0, 0x0000_000_0,  //   29  0x1e   30  Ａ　ち　
    0x0053_053_1, 0x0000_000_0, 0x0000_000_0,  //   30  0x1f   31  Ｓ　と　
    0x0044_044_1, 0x0000_000_0, 0x0000_000_0,  //   31  0x20   32  Ｄ　し　
    0x0046_046_1, 0x0000_000_0, 0x0000_000_0,  //   32  0x21   33  Ｆ　は　
    0x0047_047_1, 0x0000_000_0, 0x0000_000_0,  //   33  0x22   34  Ｇ　き　
    0x0048_048_1, 0x0000_000_0, 0x0000_000_0,  //   34  0x23   35  Ｈ　く　
    0x004a_04a_1, 0x0000_000_0, 0x0000_000_0,  //   35  0x24   36  Ｊ　ま　
    0x004b_04b_1, 0x0000_000_0, 0x0000_000_0,  //   36  0x25   37  Ｋ　の　
    0x004c_04c_1, 0x0000_000_0, 0x0000_000_0,  //   37  0x26   38  Ｌ　り　
    0x003b_0bb_1, 0x0000_000_0, 0x0000_000_0,  //   38  0x27   39  ；＋れ　
    0x0201_0ba_1, 0x0000_000_0, 0x0000_000_0,  //   39  0x28   40  ：＊け　
    0x005d_0dd_1, 0x0000_000_0, 0x0000_000_0,  //   40  0x29   41  ］｝む」
    0x005a_05a_1, 0x0000_000_0, 0x0000_000_0,  //   41  0x2a   42  Ｚ　つっ
    0x0058_058_1, 0x0000_000_0, 0x0000_000_0,  //   42  0x2b   43  Ｘ　さ　
    0x0043_043_1, 0x0000_000_0, 0x0000_000_0,  //   43  0x2c   44  Ｃ　そ　
    0x0056_056_1, 0x0000_000_0, 0x0000_000_0,  //   44  0x2d   45  Ｖ　ひ　
    0x0042_042_1, 0x0000_000_0, 0x0000_000_0,  //   45  0x2e   46  Ｂ　こ　
    0x004e_04e_1, 0x0000_000_0, 0x0000_000_0,  //   46  0x2f   47  Ｎ　み　
    0x004d_04d_1, 0x0000_000_0, 0x0000_000_0,  //   47  0x30   48  Ｍ　も　
    0x002c_0bc_1, 0x0000_000_0, 0x0000_000_0,  //   48  0x31   49  ，＜ね、
    0x002e_0be_1, 0x0000_000_0, 0x0000_000_0,  //   49  0x32   50  ．＞る。
    0x002f_0bf_1, 0x0000_000_0, 0x0000_000_0,  //   50  0x33   51  ／？め・
    0x005c_0e2_1, 0x0000_000_0, 0x0000_000_0,  //   51  0x34   52  　＿ろ□
    0x0020_020_1, 0x0000_000_0, 0x0000_000_0,  //   52  0x35   53  スペース
    0x0024_024_1, 0x0000_000_0, 0x0000_000_0,  //   53  0x36   54  HOME
    0x007f_02e_1, 0x0000_000_0, 0x0000_000_0,  //   54  0x37   55  DEL
    0x0021_021_1, 0x0000_000_0, 0x0000_000_0,  //   55  0x38   56  ROLLUP
    0x0022_022_1, 0x0000_000_0, 0x0000_000_0,  //   56  0x39   57  ROLLDOWN
    0x0023_023_1, 0x0000_000_0, 0x0000_000_0,  //   57  0x3a   58  UNDO
    0x0025_025_1, 0x0000_000_0, 0x0000_000_0,  //   58  0x3b   59  ←
    0x0026_026_1, 0x0000_000_0, 0x0000_000_0,  //   59  0x3c   60  ↑
    0x0027_027_1, 0x0000_000_0, 0x0000_000_0,  //   60  0x3d   61  →
    0x0028_028_1, 0x0000_000_0, 0x0000_000_0,  //   61  0x3e   62  ↓
    0x0090_090_4, 0x0000_000_0, 0x0000_000_0,  //   62  0x3f   63  CLR
    0x006f_06f_4, 0x0000_000_0, 0x0000_000_0,  //   63  0x40   64  ／
    0x006a_06a_4, 0x0000_000_0, 0x0000_000_0,  //   64  0x41   65  ＊
    0x006d_06d_4, 0x0000_000_0, 0x0000_000_0,  //   65  0x42   66  －
    0x0024_024_4, 0x0067_067_4, 0x0000_000_0,  //   66  0x43   67  ７
    0x0026_026_4, 0x0068_068_4, 0x0000_000_0,  //   67  0x44   68  ８
    0x0021_021_4, 0x0069_069_4, 0x0000_000_0,  //   68  0x45   69  ９
    0x006b_06b_4, 0x0000_000_0, 0x0000_000_0,  //   69  0x46   70  ＋
    0x0025_025_4, 0x0064_064_4, 0x0000_000_0,  //   70  0x47   71  ４
    0x0065_065_4, 0x0000_000_0, 0x0000_000_0,  //   71  0x48   72  ５
    0x0027_027_4, 0x0066_066_4, 0x0000_000_0,  //   72  0x49   73  ６
    0x000c_00c_4, 0x0000_000_0, 0x0000_000_0,  //   73  0x4a   74  ＝
    0x0023_023_4, 0x0061_061_4, 0x0000_000_0,  //   74  0x4b   75  １
    0x0028_028_4, 0x0062_062_4, 0x0000_000_0,  //   75  0x4c   76  ２
    0x0022_022_4, 0x0063_063_4, 0x0000_000_0,  //   76  0x4d   77  ３
    0x000a_00d_4, 0x0000_000_0, 0x0000_000_0,  //   77  0x4e   78  ENTER
    0x009b_02d_4, 0x0060_060_4, 0x0000_000_0,  //   78  0x4f   79  ０
    0x0000_0c2_1, 0x0000_000_0, 0x0000_000_0,  //   79  0x50   80  ，
    0x007f_02e_4, 0x006e_06e_4, 0x0000_000_0,  //   80  0x51   81  ．
    0xf009_085_1, 0x0000_000_0, 0x0000_000_0,  //   81  0x52   82  記号入力     Volume Mute → F22
    0xf00a_086_1, 0x0000_000_0, 0x0000_000_0,  //   82  0x53   83  登録         Volume Down → F23
    0xf00b_087_1, 0x0000_000_0, 0x0000_000_0,  //   83  0x54   84  HELP         Volume Up → F24
    0x0012_012_2, 0x0000_000_0, 0x0000_000_0,  //   84  0x55   85  XF1          Alt (Left)
    0xf001_07d_1, 0x0000_000_0, 0x0000_000_0,  //   85  0x56   86  XF2          無変換(VK 29) → F14
    0xf002_07e_1, 0x0000_000_0, 0x0000_000_0,  //   86  0x57   87  XF3          変換(VK 28) → F15
    0xf003_07f_1, 0x0000_000_0, 0x0000_000_0,  //   87  0x58   88  XF4          ひらがな(VK 242) → F16
    0x0012_012_3, 0x0000_000_0, 0x0000_000_0,  //   88  0x59   89  XF5          Alt (Right)
    0xf005_081_1, 0x0000_000_0, 0x0000_000_0,  //   89  0x5a   90  かな         Print Screen → F18
    0xf006_082_1, 0x0000_000_0, 0x0000_000_0,  //   90  0x5b   91  ローマ字     Scroll Lock → F19
    0x0013_013_1, 0x0000_000_0, 0x0000_000_0,  //   91  0x5c   92  コード入力   Pause
    0xf004_080_1, 0x0000_000_0, 0x0000_000_0,  //   92  0x5d   93  CAPS         英数字(VK 240) → F17
    0x009b_02d_1, 0x0000_000_0, 0x0000_000_0,  //   93  0x5e   94  INS
    0xf007_083_1, 0x0000_000_0, 0x0000_000_0,  //   94  0x5f   95  ひらがな     Win (Left) → F20
    0xf008_084_1, 0x0000_000_0, 0x0000_000_0,  //   95  0x60   96  全角         Win (Right) → F21
    0x00f4_0f3_1, 0x00f3_0f4_1, 0x0000_000_0,  //   96  0x61   97  BREAK        半角(VK 244)/全角(VK 243)
    0x020d_05d_1, 0x0000_000_0, 0x0000_000_0,  //   97  0x62   98  COPY
    0x0070_070_1, 0x0000_000_0, 0x0000_000_0,  //   98  0x63   99  F1
    0x0071_071_1, 0x0000_000_0, 0x0000_000_0,  //   99  0x64  100  F2
    0x0072_072_1, 0x0000_000_0, 0x0000_000_0,  //  100  0x65  101  F3
    0x0073_073_1, 0x0000_000_0, 0x0000_000_0,  //  101  0x66  102  F4
    0x0074_074_1, 0x0000_000_0, 0x0000_000_0,  //  102  0x67  103  F5
    0x0075_075_1, 0x0000_000_0, 0x0000_000_0,  //  103  0x68  104  F6
    0x0076_076_1, 0x0000_000_0, 0x0000_000_0,  //  104  0x69  105  F7
    0x0077_077_1, 0x0000_000_0, 0x0000_000_0,  //  105  0x6a  106  F8
    0x0078_078_1, 0x0000_000_0, 0x0000_000_0,  //  106  0x6b  107  F9
    0x0079_079_1, 0x0000_000_0, 0x0000_000_0,  //  107  0x6c  108  F10
    0x0010_010_2, 0x0010_010_3, 0x0000_000_0,  //  108  0x70  112  SHIFT
    0x0011_011_2, 0x0000_000_0, 0x0000_000_0,  //  109  0x71  113  CTRL
    0xf000_07c_1, 0x0000_000_0, 0x0000_000_0,  //  110  0x72  114  OPT.1        F11 → F13
    0x007b_07b_1, 0x0000_000_0, 0x0000_000_0,  //  111  0x73  115  OPT.2        F12
    0x0000_000_0, 0x0000_000_0, 0x0000_000_0,  //  112  0x74  116  NUM
  };

  //  eo→xo
  public static final int[] KBD_EO_TO_XO = {
    //xo     eo   x68kCode
      1,  //  0  0x02  0x02  "１！ぬ　"
      2,  //  1  0x03  0x03  "２＂ふ　"
      3,  //  2  0x04  0x04  "３＃あぁ"
      4,  //  3  0x05  0x05  "４＄うぅ"
      5,  //  4  0x06  0x06  "５％えぇ"
      6,  //  5  0x07  0x07  "６＆おぉ"
      7,  //  6  0x08  0x43  "７＇やゃ"
      8,  //  7  0x09  0x44  "８（ゆゅ"
      9,  //  8  0x0a  0x45  "９）よょ"
     10,  //  9  0x0b  0x40  "０　わを"
     11,  // 10  0x0c  0x41  "－＝ほ　"
     12,  // 11  0x0d  0x0d  "＾～へ　"
     13,  // 12  0x0e  0x0e  "￥｜ー　"
     16,  // 13  0x11  0x11  "Ｑ　た　"
     17,  // 14  0x12  0x12  "Ｗ　て　"
     18,  // 15  0x13  0x13  "Ｅ　いぃ"
     19,  // 16  0x14  0x14  "Ｒ　す　"
     20,  // 17  0x15  0x15  "Ｔ　か　"
     21,  // 18  0x16  0x16  "Ｙ　ん　"
     22,  // 19  0x17  0x47  "Ｕ　な　"
     23,  // 20  0x18  0x48  "Ｉ　に　"
     24,  // 21  0x19  0x49  "Ｏ　ら　"
     25,  // 22  0x1a  0x42  "Ｐ　せ　"
     26,  // 23  0x1b  0x1b  "＠｀゛　"
     27,  // 24  0x1c  0x1c  "［｛゜「"
     29,  // 25  0x1e  0x1e  "Ａ　ち　"
     30,  // 26  0x1f  0x1f  "Ｓ　と　"
     31,  // 27  0x20  0x20  "Ｄ　し　"
     32,  // 28  0x21  0x21  "Ｆ　は　"
     33,  // 29  0x22  0x22  "Ｇ　き　"
     34,  // 30  0x23  0x23  "Ｈ　く　"
     35,  // 31  0x24  0x4b  "Ｊ　ま　"
     36,  // 32  0x25  0x4c  "Ｋ　の　"
     37,  // 33  0x26  0x4d  "Ｌ　り　"
     38,  // 34  0x27  0x46  "；＋れ　"
     39,  // 35  0x28  0x28  "：＊け　"
     40,  // 36  0x29  0x29  "］｝む」"
     41,  // 37  0x2a  0x2a  "Ｚ　つっ"
     42,  // 38  0x2b  0x2b  "Ｘ　さ　"
     43,  // 39  0x2c  0x2c  "Ｃ　そ　"
     44,  // 40  0x2d  0x2d  "Ｖ　ひ　"
     45,  // 41  0x2e  0x2e  "Ｂ　こ　"
     46,  // 42  0x2f  0x2f  "Ｎ　み　"
     47,  // 43  0x30  0x4f  "Ｍ　も　"
     48,  // 44  0x31  0x50  "，＜ね、"
     49,  // 45  0x32  0x51  "．＞る。"
     50,  // 46  0x33  0x4a  "／？め・"
     51,  // 47  0x34  0x4e  "　＿ろ□"
     89,  // 48  0x5a  0x5a  "かな"
     90,  // 49  0x5b  0x5b  "ローマ字"
     91,  // 50  0x5c  0x5c  "コード入力"
     92,  // 51  0x5d  0x5d  "CAPS"
     93,  // 52  0x5e  0x5e  "INS"
     94,  // 53  0x5f  0x5f  "ひらがな"
     95,  // 54  0x60  0x60  "全角"
    112,  // 55  0x74  0x74  "NUM"
      0,  // 56  0x01  0x01  "ESC"
     14,  // 57  0x0f  0x0f  "BS"
     15,  // 58  0x10  0x10  "TAB"
     28,  // 59  0x1d  0x1d  "リターン"
     52,  // 60  0x35  0x35  "スペース"
     53,  // 61  0x36  0x36  "HOME"
     54,  // 62  0x37  0x37  "DEL"
     55,  // 63  0x38  0x38  "ROLLUP"
     56,  // 64  0x39  0x39  "ROLLDOWN"
     57,  // 65  0x3a  0x3a  "UNDO"
     58,  // 66  0x3b  0x3b  "←"
     59,  // 67  0x3c  0x3c  "↑"
     60,  // 68  0x3d  0x3d  "→"
     61,  // 69  0x3e  0x3e  "↓"
     62,  // 70  0x3f  0x3f  "CLR"
     63,  // 71  0x40  0x40  "／"
     64,  // 72  0x41  0x41  "＊"
     65,  // 73  0x42  0x42  "－"
     66,  // 74  0x43  0x43  "７"
     67,  // 75  0x44  0x44  "８"
     68,  // 76  0x45  0x45  "９"
     69,  // 77  0x46  0x46  "＋"
     70,  // 78  0x47  0x47  "４"
     71,  // 79  0x48  0x48  "５"
     72,  // 80  0x49  0x49  "６"
     73,  // 81  0x4a  0x4a  "＝"
     74,  // 82  0x4b  0x4b  "１"
     75,  // 83  0x4c  0x4c  "２"
     76,  // 84  0x4d  0x4d  "３"
     77,  // 85  0x4e  0x4e  "ENTER"
     78,  // 86  0x4f  0x4f  "０"
     79,  // 87  0x50  0x50  "，"
     80,  // 88  0x51  0x51  "．"
     81,  // 89  0x52  0x52  "記号入力"
     82,  // 90  0x53  0x53  "登録"
     83,  // 91  0x54  0x54  "HELP"
     84,  // 92  0x55  0x55  "XF1"
     85,  // 93  0x56  0x56  "XF2"
     86,  // 94  0x57  0x57  "XF3"
     87,  // 95  0x58  0x58  "XF4"
     88,  // 96  0x59  0x59  "XF5"
     96,  // 97  0x61  0x61  "BREAK"
     97,  // 98  0x62  0x62  "COPY"
     98,  // 99  0x63  0x63  "F1"
     99,  //100  0x64  0x64  "F2"
    100,  //101  0x65  0x65  "F3"
    101,  //102  0x66  0x66  "F4"
    102,  //103  0x67  0x67  "F5"
    103,  //104  0x68  0x68  "F6"
    104,  //105  0x69  0x69  "F7"
    105,  //106  0x6a  0x6a  "F8"
    106,  //107  0x6b  0x6b  "F9"
    107,  //108  0x6c  0x6c  "F10"
    108,  //109  0x70  0x70  "SHIFT"
    109,  //110  0x71  0x71  "CTRL"
    110,  //111  0x72  0x72  "OPT.1"
    111,  //112  0x73  0x73  "OPT.2"
  };

  //  xo→eo
  public static final int[] KBD_XO_TO_EO = {
    //eo     xo   x68kCode
     56,  //  0  0x01  0x01  "ESC"
      0,  //  1  0x02  0x02  "１！ぬ　"
      1,  //  2  0x03  0x03  "２＂ふ　"
      2,  //  3  0x04  0x04  "３＃あぁ"
      3,  //  4  0x05  0x05  "４＄うぅ"
      4,  //  5  0x06  0x06  "５％えぇ"
      5,  //  6  0x07  0x07  "６＆おぉ"
      6,  //  7  0x08  0x43  "７＇やゃ"
      7,  //  8  0x09  0x44  "８（ゆゅ"
      8,  //  9  0x0a  0x45  "９）よょ"
      9,  // 10  0x0b  0x40  "０　わを"
     10,  // 11  0x0c  0x41  "－＝ほ　"
     11,  // 12  0x0d  0x0d  "＾～へ　"
     12,  // 13  0x0e  0x0e  "￥｜ー　"
     57,  // 14  0x0f  0x0f  "BS"
     58,  // 15  0x10  0x10  "TAB"
     13,  // 16  0x11  0x11  "Ｑ　た　"
     14,  // 17  0x12  0x12  "Ｗ　て　"
     15,  // 18  0x13  0x13  "Ｅ　いぃ"
     16,  // 19  0x14  0x14  "Ｒ　す　"
     17,  // 20  0x15  0x15  "Ｔ　か　"
     18,  // 21  0x16  0x16  "Ｙ　ん　"
     19,  // 22  0x17  0x47  "Ｕ　な　"
     20,  // 23  0x18  0x48  "Ｉ　に　"
     21,  // 24  0x19  0x49  "Ｏ　ら　"
     22,  // 25  0x1a  0x42  "Ｐ　せ　"
     23,  // 26  0x1b  0x1b  "＠｀゛　"
     24,  // 27  0x1c  0x1c  "［｛゜「"
     59,  // 28  0x1d  0x1d  "リターン"
     25,  // 29  0x1e  0x1e  "Ａ　ち　"
     26,  // 30  0x1f  0x1f  "Ｓ　と　"
     27,  // 31  0x20  0x20  "Ｄ　し　"
     28,  // 32  0x21  0x21  "Ｆ　は　"
     29,  // 33  0x22  0x22  "Ｇ　き　"
     30,  // 34  0x23  0x23  "Ｈ　く　"
     31,  // 35  0x24  0x4b  "Ｊ　ま　"
     32,  // 36  0x25  0x4c  "Ｋ　の　"
     33,  // 37  0x26  0x4d  "Ｌ　り　"
     34,  // 38  0x27  0x46  "；＋れ　"
     35,  // 39  0x28  0x28  "：＊け　"
     36,  // 40  0x29  0x29  "］｝む」"
     37,  // 41  0x2a  0x2a  "Ｚ　つっ"
     38,  // 42  0x2b  0x2b  "Ｘ　さ　"
     39,  // 43  0x2c  0x2c  "Ｃ　そ　"
     40,  // 44  0x2d  0x2d  "Ｖ　ひ　"
     41,  // 45  0x2e  0x2e  "Ｂ　こ　"
     42,  // 46  0x2f  0x2f  "Ｎ　み　"
     43,  // 47  0x30  0x4f  "Ｍ　も　"
     44,  // 48  0x31  0x50  "，＜ね、"
     45,  // 49  0x32  0x51  "．＞る。"
     46,  // 50  0x33  0x4a  "／？め・"
     47,  // 51  0x34  0x4e  "　＿ろ□"
     60,  // 52  0x35  0x35  "スペース"
     61,  // 53  0x36  0x36  "HOME"
     62,  // 54  0x37  0x37  "DEL"
     63,  // 55  0x38  0x38  "ROLLUP"
     64,  // 56  0x39  0x39  "ROLLDOWN"
     65,  // 57  0x3a  0x3a  "UNDO"
     66,  // 58  0x3b  0x3b  "←"
     67,  // 59  0x3c  0x3c  "↑"
     68,  // 60  0x3d  0x3d  "→"
     69,  // 61  0x3e  0x3e  "↓"
     70,  // 62  0x3f  0x3f  "CLR"
     71,  // 63  0x40  0x40  "／"
     72,  // 64  0x41  0x41  "＊"
     73,  // 65  0x42  0x42  "－"
     74,  // 66  0x43  0x43  "７"
     75,  // 67  0x44  0x44  "８"
     76,  // 68  0x45  0x45  "９"
     77,  // 69  0x46  0x46  "＋"
     78,  // 70  0x47  0x47  "４"
     79,  // 71  0x48  0x48  "５"
     80,  // 72  0x49  0x49  "６"
     81,  // 73  0x4a  0x4a  "＝"
     82,  // 74  0x4b  0x4b  "１"
     83,  // 75  0x4c  0x4c  "２"
     84,  // 76  0x4d  0x4d  "３"
     85,  // 77  0x4e  0x4e  "ENTER"
     86,  // 78  0x4f  0x4f  "０"
     87,  // 79  0x50  0x50  "，"
     88,  // 80  0x51  0x51  "．"
     89,  // 81  0x52  0x52  "記号入力"
     90,  // 82  0x53  0x53  "登録"
     91,  // 83  0x54  0x54  "HELP"
     92,  // 84  0x55  0x55  "XF1"
     93,  // 85  0x56  0x56  "XF2"
     94,  // 86  0x57  0x57  "XF3"
     95,  // 87  0x58  0x58  "XF4"
     96,  // 88  0x59  0x59  "XF5"
     48,  // 89  0x5a  0x5a  "かな"
     49,  // 90  0x5b  0x5b  "ローマ字"
     50,  // 91  0x5c  0x5c  "コード入力"
     51,  // 92  0x5d  0x5d  "CAPS"
     52,  // 93  0x5e  0x5e  "INS"
     53,  // 94  0x5f  0x5f  "ひらがな"
     54,  // 95  0x60  0x60  "全角"
     97,  // 96  0x61  0x61  "BREAK"
     98,  // 97  0x62  0x62  "COPY"
     99,  // 98  0x63  0x63  "F1"
    100,  // 99  0x64  0x64  "F2"
    101,  //100  0x65  0x65  "F3"
    102,  //101  0x66  0x66  "F4"
    103,  //102  0x67  0x67  "F5"
    104,  //103  0x68  0x68  "F6"
    105,  //104  0x69  0x69  "F7"
    106,  //105  0x6a  0x6a  "F8"
    107,  //106  0x6b  0x6b  "F9"
    108,  //107  0x6c  0x6c  "F10"
    109,  //108  0x70  0x70  "SHIFT"
    110,  //109  0x71  0x71  "CTRL"
    111,  //110  0x72  0x72  "OPT.1"
    112,  //111  0x73  0x73  "OPT.2"
     55,  //112  0x74  0x74  "NUM"
  };

  public static JFrame kbdFrame;
  public static KeyMapEditor kbdEditor;

  //kbdInit ()
  //  キーボードを初期化する
  public static void kbdInit () {

    //キーマップ
    kbdUSLayoutOn = Settings.sgsGetOnOff ("keymapus");
    //  Zキーボード以外のデフォルトのキーマップ
    kbdDefaultMapNonZ = (XEiJ.prgIsLinux ? KBD_DEFAULT_MAP_LINUX :
                         XEiJ.prgIsMac ?
                         kbdUSLayoutOn ? KBD_DEFAULT_MAP_MAC_US : KBD_DEFAULT_MAP_MAC :
                         kbdUSLayoutOn ? KBD_DEFAULT_MAP_WIN_US : KBD_DEFAULT_MAP_WIN);
    //  Zキーボード以外のキーマップ
    kbdCurrentMapNonZ = new int[KBD_DEFAULT_MAP_WIN.length];
    {
      int[] map = Settings.sgsGetIntArray ("keymap");  //保存されていたZキーボード以外のキーマップ
      if (map.length == 0 || map[0] != -3) {  //保存されていたZキーボード以外のキーマップがないかバージョンが古いとき
        System.arraycopy (kbdDefaultMapNonZ, 0,  //from
                          kbdCurrentMapNonZ, 0,  //to
                          KBD_DEFAULT_MAP_WIN.length);  //length
      } else {  //保存されていたZキーボード以外のキーマップがあるとき
        Arrays.fill (kbdCurrentMapNonZ, 0);  //array,value
        System.arraycopy (map, 1,  //from
                          kbdCurrentMapNonZ, 0,  //to
                          Math.min (map.length - 1, KBD_DEFAULT_MAP_WIN.length));  //length
      }
    }
    //  現在のキーマップ
    kbdCurrentMap = new int[KBD_DEFAULT_MAP_WIN.length];
    System.arraycopy (kbdCurrentMapNonZ, 0,  //from
                      kbdCurrentMap, 0,  //to
                      KBD_DEFAULT_MAP_WIN.length);  //length

    //キーボードの種類
    String paramKeyboard = Settings.sgsGetString ("keyboard");
    switch (paramKeyboard.toLowerCase ()) {
    case "":
    case "none":
      kbdOn = false;
      kbdType = KBD_STANDARD_TYPE;
      break;
    case "standard":
      kbdOn = true;
      kbdType = KBD_STANDARD_TYPE;
      break;
    case "compact":
      kbdOn = true;
      kbdType = KBD_COMPACT_TYPE;
      break;
    default:
      kbdOn = true;
      kbdType = KBD_STANDARD_TYPE;
    }

    if (KBD_GUIDE_ON) {
      kbdGuideThread = null;
    }

    //Zキーボード
    if (KBD_ZKEY_ON) {
      kbdZKeyOnRequest = XEiJ.prgIsWindows && Settings.sgsGetOnOff ("zkeyboard");
      kbdZKeyOn = false;
      kbdZKeyStatus = 0;
      kbdZKeyPort = null;
      kbdZKeyMode = Settings.sgsGetInt ("zkeymode", 0, 0, 2);
      kbdZKeyIsUsb = false;
      kbdZKeyDebugFlag = false;
      //  I/Oポート
      kbdZKeyIOData = 0L;
      kbdZKeyIOProgrammable = false;
      //  入力
      kbdXF4Locked = null;
      kbdSavedNumLock = false;
      kbdIgnoreNumLock = false;
      //  デモ
      demoInit ();
      //  Zキーボードのデフォルトのキーマップ
      kbdDefaultMapZ = KBD_DEFAULT_MAP_ZKEY;
      //  Zキーボードのキーマップ
      kbdCurrentMapZ = new int[KBD_DEFAULT_MAP_WIN.length];
      {
        int[] map = Settings.sgsGetIntArray ("zkeymap");  //保存されていたZキーボードのキーマップ
        if (map.length == 0 || map[0] != -3) {  //保存されていたZキーボードのキーマップがないかバージョンが古いとき
          System.arraycopy (kbdDefaultMapZ, 0,  //from
                            kbdCurrentMapZ, 0,  //to
                            KBD_DEFAULT_MAP_WIN.length);  //length
        } else {  //保存されていたZキーボードのキーマップがあるとき
          Arrays.fill (kbdCurrentMapZ, 0);  //array,value
          System.arraycopy (map, 1,  //from
                            kbdCurrentMapZ, 0,  //to
                            Math.min (map.length - 1, KBD_DEFAULT_MAP_WIN.length));  //length
        }
      }
      //メニュー
      ActionListener listener = new ActionListener () {
        @Override public void actionPerformed (ActionEvent ae) {
          Object source = ae.getSource ();
          String command = ae.getActionCommand ();
          switch (command) {
          case "Connect":  //接続
            if (KBD_ZKEY_ON) {
              kbdZKeyOnRequest = ((JCheckBoxMenuItem) source).isSelected ();
              if (kbdZKeyOnRequest) {
                kbdZKeyOpen ();
              } else {
                XEiJ.tmrTimer.schedule (new TimerTask () {
                  @Override public void run () {
                    kbdZKeyClose ();
                  }
                }, 0L);
              }
            }
            break;
          case "HID only":  //HID のみ
            kbdZKeyMode = 0;
            break;
          case "Auto (WinUSB first)":  //自動
            kbdZKeyMode = 1;
            break;
          case "WinUSB only":  //WinUSB のみ
            kbdZKeyMode = 2;
            break;
          case "LED operation chedk":  //LED の動作確認
            if (KBD_ZKEY_ON) {
              if (((JCheckBoxMenuItem) source).isSelected ()) {
                demoStart ();
              } else {
                demoEnd ();
              }
            }
            break;
          case "Debug flag":  //デバッグフラグ
            if (KBD_ZKEY_ON) {
              kbdZKeyDebugFlag = ((JCheckBoxMenuItem) source).isSelected ();
              if (kbdZKeyPort != null) {
                kbdZKeyPort.setDebugFlag (kbdZKeyDebugFlag);
              }
            }
            break;
          }
        }
      };
      ButtonGroup zkeyModeGroup = new ButtonGroup ();
      kbdZKeyMenu = !KBD_ZKEY_ON ? null : Multilingual.mlnText (
        ComponentFactory.createMenu (
          "Z keyboard",
          kbdZKeyConnectCheckBox = 
          Multilingual.mlnText (
            ComponentFactory.createCheckBoxMenuItem (kbdZKeyOnRequest, "Connect", listener),
            "ja", "接続"),
          kbdZKeyDemoCheckBox =
          ComponentFactory.setEnabled (
            Multilingual.mlnText (
              ComponentFactory.createCheckBoxMenuItem (demoThread != null, "LED operation chedk", listener),
              "ja", "LED の動作確認"),
            kbdZKeyOn),
          ComponentFactory.createHorizontalSeparator (),
          Multilingual.mlnText (
            ComponentFactory.createRadioButtonMenuItem (zkeyModeGroup, kbdZKeyMode == 0, "HID only", listener),
            "ja", "HID のみ (次回接続から)"),
          Multilingual.mlnText (
            ComponentFactory.createRadioButtonMenuItem (zkeyModeGroup, kbdZKeyMode == 1, "Auto (WinUSB first)", listener),
            "ja", "自動 (WinUSB 優先)"),
          Multilingual.mlnText (
            ComponentFactory.createRadioButtonMenuItem (zkeyModeGroup, kbdZKeyMode == 2, "WinUSB only", listener),
            "ja", "WinUSB のみ"),
          ComponentFactory.createHorizontalSeparator (),
          Multilingual.mlnText (
            ComponentFactory.createCheckBoxMenuItem (kbdZKeyDebugFlag, "Debug flag", listener),
            "ja", "デバッグフラグ")
          ),
        "ja", "Z キーボード");
    }

    //配列を初期化する
    for (int i = 0; i < KBD_VAR_KEYS; i++) {
      int t = KBD_CAPS_BASE[i];
      KBD_CAPS_CHR[i] = (byte) t;
      KBD_NORMAL_CHR[i] = (byte) (t >= 'A' && t <= 'Z' ? t ^ 0x20 : t);
      KBD_CODE_CHR[i] = (byte) (t >= '0' && t <= '9' || t >= 'A' && t <= 'F' ? t : 0);
      t = KBD_CAPS_SHIFT_BASE[i];
      KBD_CAPS_SHIFT_CHR[i] = (byte) t;
      KBD_SHIFT_CHR[i] = (byte) (t >= 'a' && t <= 'z' ? t ^ 0x20 : t);
      KBD_CTRL_CHR[i] = (byte) (t < 0x20 ? t : t >= 0x40 ? t & 0x1f : 0);
      t = KBD_KANA_BASE[i];
      KBD_KANA_CHR[i] = (byte) (t <= 0xff ? t : t - 0xfec0);
      t = KBD_KANA_SHIFT_BASE[i];
      KBD_KANA_SHIFT_CHR[i] = (byte) (t <= 0xff ? t : t - 0xfec0);
      t = KBD_KANA_AIU_BASE[i];
      KBD_KANA_AIU_CHR[i] = (byte) (t <= 0xff ? t : t - 0xfec0);
      t = KBD_KANA_AIU_SHIFT_BASE[i];
      KBD_KANA_AIU_SHIFT_CHR[i] = (byte) (t <= 0xff ? t : t - 0xfec0);
      t = KBD_ROMA_BASE[i];
      KBD_ROMA_CHR[i] = (byte) (t <= 0xff ? t : t - 0xfec0);
      t = KBD_ROMA_SHIFT_BASE[i];
      KBD_ROMA_SHIFT_CHR[i] = (byte) (t <= 0xff ? t : t - 0xfec0);
      t = KBD_NUM_BASE[i];
      if (t != ' ') {
        KBD_NUM_CAPS_CHR[i] =
          KBD_NUM_CHR[i] =
            KBD_NUM_CODE_CHR[i] =
              KBD_NUM_CAPS_SHIFT_CHR[i] =
                KBD_NUM_SHIFT_CHR[i] =
                  KBD_NUM_CTRL_CHR[i] =
                    KBD_NUM_KANA_CHR[i] =
                      KBD_NUM_KANA_SHIFT_CHR[i] =
                        KBD_NUM_KANA_AIU_CHR[i] =
                          KBD_NUM_KANA_AIU_SHIFT_CHR[i] =
                            KBD_NUM_ROMA_CHR[i] =
                              KBD_NUM_ROMA_SHIFT_CHR[i] = (byte) t;
      } else {
        KBD_NUM_CAPS_CHR[i] = KBD_CAPS_CHR[i];
        KBD_NUM_CHR[i] = KBD_NORMAL_CHR[i];
        KBD_NUM_CODE_CHR[i] = KBD_CODE_CHR[i];
        KBD_NUM_CAPS_SHIFT_CHR[i] = KBD_CAPS_SHIFT_CHR[i];
        KBD_NUM_SHIFT_CHR[i] = KBD_SHIFT_CHR[i];
        KBD_NUM_CTRL_CHR[i] = KBD_CTRL_CHR[i];
        KBD_NUM_KANA_CHR[i] = KBD_KANA_CHR[i];
        KBD_NUM_KANA_SHIFT_CHR[i] = KBD_KANA_SHIFT_CHR[i];
        KBD_NUM_KANA_AIU_CHR[i] = KBD_KANA_AIU_CHR[i];
        KBD_NUM_KANA_AIU_SHIFT_CHR[i] = KBD_KANA_AIU_SHIFT_CHR[i];
        KBD_NUM_ROMA_CHR[i] = KBD_ROMA_CHR[i];
        KBD_NUM_ROMA_SHIFT_CHR[i] = KBD_ROMA_SHIFT_CHR[i];
      }
    }

    //色を決める
    //  デフォルトのbrightnessは降順なので昇順のとき色を入れ替えてキーが凹まないようにする
    if (LnF.lnfHSB[6] <= LnF.lnfHSB[8]) {  //B0,B14
      kbdLetterRGB      = LnF.lnfRGB[KBD_LETTER_COLOR];
      kbdTopLeftRGB     = LnF.lnfRGB[KBD_TOP_LEFT_COLOR];
      kbdTopRGB         = LnF.lnfRGB[KBD_TOP_COLOR];
      kbdLeftRGB        = LnF.lnfRGB[KBD_LEFT_COLOR];
      kbdPointedRGB     = LnF.lnfRGB[KBD_POINTED_COLOR];
      kbdTopRightRGB    = LnF.lnfRGB[KBD_TOP_RIGHT_COLOR];
      kbdBottomLeftRGB  = LnF.lnfRGB[KBD_BOTTOM_LEFT_COLOR];
      kbdKeytopRGB      = LnF.lnfRGB[KBD_KEYTOP_COLOR];
      kbdRightRGB       = LnF.lnfRGB[KBD_RIGHT_COLOR];
      kbdBottomRGB      = LnF.lnfRGB[KBD_BOTTOM_COLOR];
      kbdBottomRightRGB = LnF.lnfRGB[KBD_BOTTOM_RIGHT_COLOR];
      kbdShadowRGB      = LnF.lnfRGB[KBD_SHADOW_COLOR];
      kbdBackgroundRGB  = LnF.lnfRGB[KBD_BACKGROUND_COLOR];
      kbdHoleRGB        = LnF.lnfRGB[KBD_HOLE_COLOR];
    } else {
      kbdLetterRGB      = LnF.lnfRGB[14 - KBD_LETTER_COLOR];
      kbdTopLeftRGB     = LnF.lnfRGB[14 - KBD_TOP_LEFT_COLOR];
      kbdTopRGB         = LnF.lnfRGB[14 - KBD_TOP_COLOR];
      kbdLeftRGB        = LnF.lnfRGB[14 - KBD_LEFT_COLOR];
      kbdPointedRGB     = LnF.lnfRGB[14 - KBD_POINTED_COLOR];
      kbdTopRightRGB    = LnF.lnfRGB[14 - KBD_TOP_RIGHT_COLOR];
      kbdBottomLeftRGB  = LnF.lnfRGB[14 - KBD_BOTTOM_LEFT_COLOR];
      kbdKeytopRGB      = LnF.lnfRGB[14 - KBD_KEYTOP_COLOR];
      kbdRightRGB       = LnF.lnfRGB[14 - KBD_RIGHT_COLOR];
      kbdBottomRGB      = LnF.lnfRGB[14 - KBD_BOTTOM_COLOR];
      kbdBottomRightRGB = LnF.lnfRGB[14 - KBD_BOTTOM_RIGHT_COLOR];
      kbdShadowRGB      = LnF.lnfRGB[14 - KBD_SHADOW_COLOR];
      kbdBackgroundRGB  = LnF.lnfRGB[14 - KBD_BACKGROUND_COLOR];
      kbdHoleRGB        = LnF.lnfRGB[14 - KBD_HOLE_COLOR];
    }

    //可変キーを描く
    //kbdVarStatBitmap = new int[256][][];
    for (int i = 0; i < 256; i++) {
      int[] letter = KBD_VAR_LETTER[i];
      if (letter == null) {
        kbdVarStatBitmap[i] = null;
        continue;
      }
      int[][] statBitmap = kbdVarStatBitmap[i] = new int[4][];
      for (int stat = 0; stat < 4; stat++) {
        if (KBD_USE_ROM_FONT) {  //ROMフォントを使う
          kbdDrawKey2 (statBitmap[stat] = new int[KBD_KEY_WIDTH * KBD_KEY_HEIGHT],
                       KBD_KEY_WIDTH, 0, 0, KBD_KEY_WIDTH, KBD_KEY_HEIGHT, 0, 0, null, i, stat);
        } else {
          kbdDrawKey (statBitmap[stat] = new int[KBD_KEY_WIDTH * KBD_KEY_HEIGHT],
                      KBD_KEY_WIDTH, 0, 0, KBD_KEY_WIDTH, KBD_KEY_HEIGHT, 0, 0, null, letter, stat);
        }
      }
    }

    //LEDキーを描く
    //kbdLedStatBitmap = new int[KBD_LED_KEYS][][];
    for (int i = 0; i < KBD_LED_KEYS; i++) {
      int[] letter = KBD_LED_LETTER[i];
      if (letter == null) {
        kbdLedStatBitmap[i] = null;
        continue;
      }
      int[][] statBitmap = kbdLedStatBitmap[i] = new int[32][];
      for (int stat = 0; stat < 32; stat++) {
        kbdDrawKey (statBitmap[stat] = new int[KBD_KEY_WIDTH * KBD_KEY_HEIGHT],
                    KBD_KEY_WIDTH, 0, 0, KBD_KEY_WIDTH, KBD_KEY_HEIGHT, 0, 0, KBD_LED_COLOR[i][stat >> 2], letter, stat & 3);
      }
    }

    //種類毎に別々に用意するもの
    //kbdDataTypeImage = new BufferedImage[KBD_TYPES];
    //kbdDataTypeWidth = new int[KBD_TYPES];
    //kbdDataTypeHeight = new int[KBD_TYPES];
    //kbdDataTypeBitmap = new int[KBD_TYPES][];
    //kbdDataTypeIndexMap = new byte[KBD_TYPES][];
    //kbdDataTypeStatFixBitmap = new int[KBD_TYPES][][];
    //kbdDataTypeIndexRect = new int[KBD_TYPES][][];
    //kbdDataTypeIndexCharacterStat = new int[KBD_TYPES][];
    for (int type = 0; type < KBD_TYPES; type++) {
      int cols = KBD_TYPE_COLS[type];
      int rows = KBD_TYPE_ROWS[type];
      int width = kbdDataTypeWidth[type] = KBD_PADDING_LEFT + KBD_COL_WIDTH * cols + KBD_PADDING_RIGHT;
      int height = kbdDataTypeHeight[type] = KBD_PADDING_TOP + KBD_ROW_HEIGHT * rows + KBD_PADDING_BOTTOM;
      BufferedImage image = kbdDataTypeImage[type] = new BufferedImage (width, height, BufferedImage.TYPE_INT_RGB);
      kbdDataTypeBitmap[type] = ((DataBufferInt) image.getRaster ().getDataBuffer ()).getData ();
      Arrays.fill (kbdDataTypeBitmap[type], kbdBackgroundRGB);
      byte[] indexMap = kbdDataTypeIndexMap[type] = new byte[width * height];
      Arrays.fill (kbdDataTypeIndexMap[type], (byte) -1);
      kbdDataTypeStatFixBitmap[type] = new int[4][];
      int[][] indexRect = kbdDataTypeIndexRect[type] = new int[KBD_ALL_KEYS][];
      int[] indexCharacterStat = kbdDataTypeIndexCharacterStat[type] = new int[KBD_ALL_KEYS];
      Arrays.fill (indexCharacterStat, -1);  //初回はすべてのキーを描画する

      //可変キーとLEDキーのレクタングルを作る
      int[][] ledBox = KBD_TYPE_LED_BOX[type];
      for (int i = 0; i < KBD_VAR_KEYS + KBD_LED_KEYS; i++) {
        int[] box = i < KBD_VAR_KEYS ? KBD_VAR_BOX[i] : ledBox[i - KBD_VAR_KEYS];
        if (box == null) {  //欠番。Standard KeyboardのNUMキー
          indexRect[i] = new int[0];
          continue;
        }
        int x = KBD_PADDING_LEFT + KBD_COL_WIDTH * box[0];
        int y = KBD_PADDING_TOP + KBD_ROW_HEIGHT * box[1];
        int w = KBD_COL_WIDTH * box[2] - 1;
        int h = KBD_ROW_HEIGHT * box[3] - 1;
        indexRect[i] = box == null ? new int[0] : new int[] { x, y, w, h };
        int tx = x + KBD_TOUCHABLE_AREA;
        int ty = y + KBD_TOUCHABLE_AREA;
        int tw = w - KBD_TOUCHABLE_AREA * 2;
        int th = h - KBD_TOUCHABLE_AREA * 2;
        int p = tx + width * ty;
        for (int v = 0; v < th; v++) {
          for (int u = 0; u < tw; u++) {
            indexMap[p + u] = (byte) i;
          }
          p += width;
        }
      }

      //固定キーを描く
      for (int stat = 0; stat < 4; stat++) {
        int[] bitmap = kbdDataTypeStatFixBitmap[type][stat] = new int[width * height];
        Arrays.fill (bitmap, kbdBackgroundRGB);
      }
      int[][] fixBox = KBD_TYPE_FIX_BOX[type];
      int[] fixStyle = KBD_TYPE_FIX_STYLE[type];
      for (int i = KBD_VAR_KEYS + KBD_LED_KEYS; i < KBD_ALL_KEYS; i++) {
        int[] box = fixBox[i - (KBD_VAR_KEYS + KBD_LED_KEYS)];
        if (box == null) {  //欠番。Compact Keyboardのテンキー
          indexRect[i] = new int[0];
          continue;
        }
        int col = box[0];  //列
        int row = box[1];  //行
        int wide = box[2];  //幅
        int tall = box[3];  //高さ
        int x = KBD_PADDING_LEFT + KBD_COL_WIDTH * col;
        int y = KBD_PADDING_TOP + KBD_ROW_HEIGHT * row;
        int w = KBD_COL_WIDTH * wide - 1;
        int h = KBD_ROW_HEIGHT * tall - 1;
        int style = fixStyle[i - (KBD_VAR_KEYS + KBD_LED_KEYS)];
        if (style == KBD_RETURN_STYLE) {  //RETURNキー
          //レクタングルの左下を1列x4行削る。キートップの左右を1列ずつ削る
          int wl = KBD_COL_WIDTH;  //左側
          int hb = KBD_ROW_HEIGHT * 4;  //下側
          int wr = w - wl;  //右側
          int ht = h - hb;  //上側
          indexRect[i] = new int[] { x, y, w, ht, x + wl, y + ht, wr, hb };
          int tx = x + (KBD_TOUCHABLE_AREA == 0 ? 0 : KBD_TOUCHABLE_AREA + KBD_COL_WIDTH);
          int ty = y + KBD_TOUCHABLE_AREA;
          int tw = w - (KBD_TOUCHABLE_AREA == 0 ? 0 : KBD_TOUCHABLE_AREA + KBD_COL_WIDTH) * 2;
          int th = ht - KBD_TOUCHABLE_AREA * 2;
          int p = tx + width * ty;
          for (int v = 0; v < th; v++) {
            for (int u = 0; u < tw; u++) {
              indexMap[p + u] = (byte) i;
            }
            p += width;
          }
          tx = x + wl + (KBD_TOUCHABLE_AREA == 0 ? 0 : KBD_TOUCHABLE_AREA + KBD_COL_WIDTH);
          ty = y + ht - KBD_TOUCHABLE_AREA;
          tw = wr - (KBD_TOUCHABLE_AREA == 0 ? 0 : KBD_TOUCHABLE_AREA + KBD_COL_WIDTH) * 2;
          th = hb;
          p = tx + width * ty;
          for (int v = 0; v < th; v++) {
            for (int u = 0; u < tw; u++) {
              indexMap[p + u] = (byte) i;
            }
            p += width;
          }
          //1段目
          for (int stat = 0; stat < 2; stat++) {  //!pressedのみ
            int[] bitmap = kbdDataTypeStatFixBitmap[type][stat];
            kbdDrawLine (bitmap, x + width * y,
                         1, 1, kbdTopLeftRGB, 1, w - 2, kbdTopRGB,  //→→
                         width, 1, kbdTopRightRGB, width, h - 2, kbdRightRGB,  //↓↓
                         -1, 1, kbdBottomRightRGB, -1, wr - 2, kbdBottomRGB,  //←
                         -width, 1, kbdBottomLeftRGB, -width, hb - 1, kbdLeftRGB,  //↑
                         -1, 1, kbdBottomLeftRGB,  -1, wl - 1, kbdBottomRGB,  //←
                         -width, 1, kbdBottomLeftRGB, -width, ht - 2, kbdLeftRGB);  //↑
          }
          for (int stat = 2; stat < 4; stat++) {  //pressedのみ
            int[] bitmap = kbdDataTypeStatFixBitmap[type][stat];
            kbdDrawLine (bitmap, x + width * y,
                         1, 1, kbdHoleRGB, 1, w - 2, kbdHoleRGB,  //→→
                         width, 1, kbdHoleRGB, width, h - 2, kbdHoleRGB,  //↓↓
                         -1, 1, kbdHoleRGB, -1, wr - 2, kbdHoleRGB,  //←
                         -width, 1, kbdHoleRGB, -width, hb - 1, kbdHoleRGB,  //↑
                         -1, 1, kbdHoleRGB,  -1, wl - 1, kbdHoleRGB,  //←
                         -width, 1, kbdHoleRGB, -width, ht - 2, kbdHoleRGB);  //↑
          }
          int u = KBD_COL_WIDTH;
          x += 1 + u;
          y += 1;
          w -= 2 + u * 2;
          h -= 2;
          wr -= 2 + u * 2;
          ht -= 2;
          //島
          for (int stat = 0; stat < 4; stat++) {
            int[] bitmap = kbdDataTypeStatFixBitmap[type][stat];
            kbdFillRect (bitmap, width, x - u, y, u, ht, kbdKeytopRGB);  //左側の上半分
            kbdFillRect (bitmap, width, x + wl - u, y + ht, u, h - ht, kbdKeytopRGB);  //左側の下半分
            kbdFillRect (bitmap, width, x + w, y, u, h, kbdKeytopRGB);  //右側
          }
          //2段目
          int lx = x;
          int ly = y;
          int lw = w;
          int lh = h;
          for (int stat = 0; stat < 4; stat++) {
            int[] bitmap = kbdDataTypeStatFixBitmap[type][stat];
            kbdDrawLine (bitmap, x + width * y,
                         1, 1, kbdTopLeftRGB, 1, w - 2, kbdTopRGB,  //→→
                         width, 1, kbdTopRightRGB, width, h - 2, kbdRightRGB,  //↓↓
                         -1, 1, kbdBottomRightRGB, -1, wr - 2, kbdBottomRGB,  //←
                         -width, 1, kbdBottomLeftRGB, -width, hb - 1, kbdLeftRGB,  //↑
                         -1, 1, kbdBottomLeftRGB, -1, wl - 1, kbdBottomRGB,  //←
                         -width, 1, kbdBottomLeftRGB, -width, ht - 2, kbdLeftRGB);  //↑
          }
          x++;
          y++;
          w -= 2;
          h -= 2;
          wr -= 2;
          ht -= 2;
          //3段目
          for (int stat = 0; stat < 4; stat++) {
            int[] bitmap = kbdDataTypeStatFixBitmap[type][stat];
            kbdDrawLine (bitmap, x + width * y,
                         1, 1, kbdTopLeftRGB, 1, w - 2, kbdTopRGB,  //→→
                         width, 1, kbdTopRightRGB, width, h - 2, kbdRightRGB,  //↓↓
                         -1, 1, kbdBottomRightRGB, -1, wr - 2, kbdBottomRGB,  //←
                         -width, 1, kbdBottomLeftRGB, -width, hb - 1, kbdLeftRGB,  //↑
                         -1, 1, kbdBottomLeftRGB, -1, wl - 1, kbdBottomRGB,  //←
                         -width, 1, kbdBottomLeftRGB, -width, ht - 2, kbdLeftRGB);  //↑
          }
          x++;
          y++;
          w -= 2;
          h -= 2;
          wr -= 2;
          ht -= 2;
          //キートップと文字
          int[] letter = KBD_FIX_LETTER[KBD_RETURN_INDEX - (KBD_VAR_KEYS + KBD_LED_KEYS)];
          for (int stat = 0; stat < 4; stat++) {
            int[] bitmap = kbdDataTypeStatFixBitmap[type][stat];
            boolean pointed = (stat & KBD_POINTED_MASK) != 0;
            int color = pointed ? kbdPointedRGB : kbdKeytopRGB;
            kbdFillRect (bitmap, width, x, y, w, ht, color);  //上半分
            kbdFillRect (bitmap, width, x + wl, y + ht, w - wl, h - ht, color);  //下半分
            kbdDrawLetter (bitmap, width, lx, ly, lw, lh, letter);
          }
        } else if (style == KBD_SHIFT_STYLE) {  //SHIFTキー
          int xr = x + KBD_COL_WIDTH * (wide - 9);  //右
          w = KBD_COL_WIDTH * 9 - 1;  //幅
          indexRect[i] = new int[] { x, y, w, h, xr, y, w, h };
          int tx = x + (KBD_TOUCHABLE_AREA == 0 ? 0 : KBD_TOUCHABLE_AREA + KBD_COL_WIDTH);
          int ty = y + KBD_TOUCHABLE_AREA;
          int tw = w - (KBD_TOUCHABLE_AREA == 0 ? 0 : KBD_TOUCHABLE_AREA + KBD_COL_WIDTH) * 2;
          int th = h - KBD_TOUCHABLE_AREA * 2;
          int p = tx + width * ty;
          for (int v = 0; v < th; v++) {
            for (int u = 0; u < tw; u++) {
              indexMap[p + u] = (byte) i;
            }
            p += width;
          }
          tx = xr + (KBD_TOUCHABLE_AREA == 0 ? 0 : KBD_TOUCHABLE_AREA + KBD_COL_WIDTH);
          p = tx + width * ty;
          for (int v = 0; v < th; v++) {
            for (int u = 0; u < tw; u++) {
              indexMap[p + u] = (byte) i;
            }
            p += width;
          }
          int[] letter = KBD_FIX_LETTER[KBD_SHIFT_INDEX - (KBD_VAR_KEYS + KBD_LED_KEYS)];
          for (int stat = 0; stat < 4; stat++) {
            int[] bitmap = kbdDataTypeStatFixBitmap[type][stat];
            kbdDrawKey (bitmap, width, x, y, w, h, KBD_COL_WIDTH, 0, null, letter, stat);
            kbdDrawKey (bitmap, width, xr, y, w, h, KBD_COL_WIDTH, 0, null, letter, stat);
          }
        } else {  //RETURNキーとSHIFTキー以外
          if (style == KBD_FUNCTION_STYLE) {  //F1～F10
            y += KBD_ROW_HEIGHT >> 1;  //キーの上端を1/2行削る
            h -= KBD_ROW_HEIGHT >> 1;
          }
          int pw = (style == KBD_NARROWER_STYLE ? KBD_COL_WIDTH >> 1 :  //キートップの左右を1/2列ずつ削る
                    style == KBD_NARROW_STYLE ? KBD_COL_WIDTH :  //キートップの左右を1列ずつ削る
                    0);
          int ph = (style == KBD_SHORTER_STYLE ? KBD_ROW_HEIGHT :  //キートップの上下を1行ずつ削る
                    style == KBD_SHORT_STYLE ? KBD_ROW_HEIGHT << 1 :  //キートップの上下を2行ずつ削る
                    0);
          indexRect[i] = new int[] { x, y, w, h };
          int tx = x + (KBD_TOUCHABLE_AREA == 0 ? 0 : KBD_TOUCHABLE_AREA + pw);
          int ty = y + (KBD_TOUCHABLE_AREA == 0 ? 0 : KBD_TOUCHABLE_AREA + ph);
          int tw = w - (KBD_TOUCHABLE_AREA == 0 ? 0 : KBD_TOUCHABLE_AREA + pw) * 2;
          int th = h - (KBD_TOUCHABLE_AREA == 0 ? 0 : KBD_TOUCHABLE_AREA + ph) * 2;
          int p = tx + width * ty;
          for (int v = 0; v < th; v++) {
            for (int u = 0; u < tw; u++) {
              indexMap[p + u] = (byte) i;
            }
            p += width;
          }
          for (int stat = 0; stat < 4; stat++) {
            int[] bitmap = kbdDataTypeStatFixBitmap[type][stat];
            kbdDrawKey (bitmap, width, x, y, w, h, pw, ph, null, KBD_FIX_LETTER[i - (KBD_VAR_KEYS + KBD_LED_KEYS)], stat);
          }
        }  //RETURNキー以外
      }  //for i
    }  //for type

    //現在の状態
    if (kbdOn) {
      kbdWidth = kbdDataTypeWidth[kbdType];
      kbdHeight = kbdDataTypeHeight[kbdType];
      kbdImage = kbdDataTypeImage[kbdType];
      kbdBitmap = kbdDataTypeBitmap[kbdType];
      kbdIndexMap = kbdDataTypeIndexMap[kbdType];
      kbdFlush ();
    } else {
      kbdWidth = 0;
      kbdHeight = 0;
      kbdImage = null;
      kbdBitmap = null;
      kbdIndexMap = null;
    }
    //kbdStat = new int[KBD_ALL_KEYS];
    Arrays.fill (kbdStat, 0);
    kbdPointedIndex = -1;
    if (!MC68901.MFP_KBD_ON) {
      kbdRepeatTask = null;
    }

    //kbdRepeatDelay = 200 + 100 * 3;
    //kbdRepeatInterval = 30 + 5 * 4 * 4;
    kbdSetRepeatDelay (MainMemory.mmrRbs (0x00ed003a));
    kbdSetRepeatInterval (MainMemory.mmrRbs (0x00ed003b));

    kbdLedBrightness = 0;  //明るい

  }  //kbdInit()

  //kbdTini ()
  //  後始末
  public static void kbdTini () {

    //キーマップを保存する
    if (KBD_ZKEY_ON && kbdZKeyPort != null && !kbdZKeyIsUsb) {  //Zキーボード
      System.arraycopy (kbdCurrentMap, 0,  //from
                        kbdCurrentMapZ, 0,  //to
                        KBD_DEFAULT_MAP_WIN.length);  //length
    } else {  //Zキーボード以外
      System.arraycopy (kbdCurrentMap, 0,  //from
                        kbdCurrentMapNonZ, 0,  //to
                        KBD_DEFAULT_MAP_WIN.length);  //length
    }

    //キーマップ
    Settings.sgsPutOnOff ("keymapus", kbdUSLayoutOn);
    {
      int[] map;
      if (Arrays.equals (kbdCurrentMapNonZ, kbdDefaultMapNonZ)) {  //デフォルトマップと同じ
        map = new int[0];
      } else {  //デフォルトマップと違う
        map = new int[1 + KBD_DEFAULT_MAP_WIN.length];
        map[0] = -3;
        Arrays.fill (map, 1, 1 + KBD_DEFAULT_MAP_WIN.length, 0);  //array,from,to,value
        System.arraycopy (kbdCurrentMapNonZ, 0,  //from
                          map, 1,  //to
                          KBD_DEFAULT_MAP_WIN.length);  //length
      }
      Settings.sgsPutIntArray ("keymap", map);
    }

    //キーボードの種類
    Settings.sgsPutString ("keyboard",
                           !kbdOn ? "none" :
                           kbdType == KBD_COMPACT_TYPE ? "compact" :
                           kbdType == KBD_STANDARD_TYPE ? "standard" :
                           "standard");

    if (KBD_GUIDE_ON) {
      if (kbdGuideThread != null) {
        kbdGuideThread.interrupt ();
        try {
          kbdGuideThread.join (100L);
        } catch (InterruptedException ie) {
        }
        kbdGuideThread = null;
      }
    }

    //Zキーボード
    if (KBD_ZKEY_ON) {
      Settings.sgsPutOnOff ("zkeyboard", kbdZKeyOnRequest);
      Settings.sgsPutInt ("zkeymode", kbdZKeyMode);
      kbdZKeyClose ();
      int[] map;
      if (Arrays.equals (kbdCurrentMapZ, kbdDefaultMapZ)) {  //デフォルトマップと同じ
        map = new int[0];
      } else {  //デフォルトマップと違う
        map = new int[1 + KBD_DEFAULT_MAP_WIN.length];
        map[0] = -3;
        Arrays.fill (map, 1, 1 + KBD_DEFAULT_MAP_WIN.length, 0);  //array,from,to,value
        System.arraycopy (kbdCurrentMapZ, 0,  //from
                          map, 1,  //to
                          KBD_DEFAULT_MAP_WIN.length);  //length
      }
      Settings.sgsPutIntArray ("zkeymap", map);
    }

  }

  //kbdReset ()
  //  リセット
  public static void kbdReset () {
    kbdRePress ();  //押されているキーを再入力する
    if (KBD_ZKEY_ON) {
      if (!kbdZKeyOn && kbdZKeyOnRequest) {
        kbdZKeyOpen ();
      }
      kbdZKeyIOData = 0L;
      kbdZKeyIOProgrammable = false;
      kbdZKeyUpdate ();
    }
  }

  //kbdDrawKey (bitmap, width, x, y, w, h, u, v, ledColors, letter, stat)
  //  キーを描く
  //  bitmap     ビットマップ
  //  width      bitmapの幅(px)
  //  x          キーのx座標(px)
  //  y          キーのy座標(px)
  //  w          キーの幅(px)
  //  h          キーの高さ(px)
  //  u          キートップの左右を削る幅(px)
  //  v          キートップの上下を削る幅(px)
  //  ledColors  LEDの色。[0]=キートップ側の色(rgb),[1]=ボーダー側の色(rgb)
  //  letter     文字のパターン
  //  stat       キーの状態。bit0=pointed,bit1=pressed
  public static void kbdDrawKey (int[] bitmap, int width, int x, int y, int w, int h, int u, int v, int[] ledColors, int[] letter, int stat) {
    boolean pointed = (stat & KBD_POINTED_MASK) != 0;
    boolean pressed = (stat & KBD_PRESSED_MASK) != 0;
    //1段目
    if (!pressed) {
      kbdDrawLine (bitmap, x + width * y,
                   1, 1, kbdTopLeftRGB, 1, w - 2, kbdTopRGB,
                   width, 1, kbdTopRightRGB, width, h - 2, kbdRightRGB,
                   -1, 1, kbdBottomRightRGB, -1, w - 2, kbdBottomRGB,
                   -width, 1, kbdBottomLeftRGB, -width, h - 2, kbdLeftRGB);
    } else {
      kbdDrawLine (bitmap, x + width * y,
                   1, 1, kbdHoleRGB, 1, w - 2, kbdHoleRGB,
                   width, 1, kbdHoleRGB, width, h - 2, kbdHoleRGB,
                   -1, 1, kbdHoleRGB, -1, w - 2, kbdHoleRGB,
                   -width, 1, kbdHoleRGB, -width, h - 2, kbdHoleRGB);
    }
    x += 1 + u;
    y += 1 + v;
    w -= 2 + u * 2;
    h -= 2 + v * 2;
    //島
    if (u > 0) {
      kbdFillRect (bitmap, width, x - u, y, u, h, kbdKeytopRGB);  //左側
      kbdFillRect (bitmap, width, x + w, y, u, h, kbdKeytopRGB);  //右側
    }
    if (v > 0) {
      kbdFillRect (bitmap, width, x, y - v, w, v, kbdKeytopRGB);  //上側
      kbdFillRect (bitmap, width, x, y + h, w, v, kbdKeytopRGB);  //下側
    }
    //2段目
    int lx = x;
    int ly = y;
    int lw = w;
    int lh = h;
    kbdDrawLine (bitmap, x + width * y,
                 1, 1, kbdTopLeftRGB, 1, w - 2, kbdTopRGB,
                 width, 1, kbdTopRightRGB, width, h - 2, kbdRightRGB,
                 -1, 1, kbdBottomRightRGB, -1, w - 2, kbdBottomRGB,
                 -width, 1, kbdBottomLeftRGB, -width, h - 2, kbdLeftRGB);
    x++;
    y++;
    w -= 2;
    h -= 2;
    //3段目
    kbdDrawLine (bitmap, x + width * y,
                 1, 1, kbdTopLeftRGB, 1, w - 2, kbdTopRGB,
                 width, 1, kbdTopRightRGB, width, h - 2, kbdRightRGB,
                 -1, 1, kbdBottomRightRGB, -1, w - 2, kbdBottomRGB,
                 -width, 1, kbdBottomLeftRGB, -width, h - 2, kbdLeftRGB);
    x++;
    y++;
    w -= 2;
    h -= 2;
    //キートップ
    kbdFillRect (bitmap, width, x, y, w, h, pointed ? kbdPointedRGB : kbdKeytopRGB);
    //LED
    if (ledColors != null) {
      x += w - KBD_LED_WIDTH >> 1;
      y += h;
      kbdFillRect (bitmap, width, x, y - KBD_LED_HEIGHT, KBD_LED_WIDTH, KBD_LED_HEIGHT, ledColors[0]);  //上半分
      kbdFillRect (bitmap, width, x, y, KBD_LED_WIDTH, pressed ? 2 : 3, ledColors[1]);  //下半分の高さはボーダーの幅
    }
    //文字
    if (letter != null) {  //文字あり
      kbdDrawLetter (bitmap, width, lx, ly, lw, lh, letter);
    }
  }  //kbdDrawKey

  //kbdDrawKey2 (bitmap, width, x, y, w, h, u, v, ledColors, code, stat)
  //  キーを描く
  //  bitmap     ビットマップ
  //  width      bitmapの幅(px)
  //  x          キーのx座標(px)
  //  y          キーのy座標(px)
  //  w          キーの幅(px)
  //  h          キーの高さ(px)
  //  u          キートップの左右を削る幅(px)
  //  v          キートップの上下を削る幅(px)
  //  ledColors  LEDの色。[0]=キートップ側の色(rgb),[1]=ボーダー側の色(rgb)
  //  code       文字コード
  //  stat       キーの状態。bit0=pointed,bit1=pressed
  public static void kbdDrawKey2 (int[] bitmap, int width, int x, int y, int w, int h, int u, int v, int[] ledColors, int code, int stat) {
    if (KBD_USE_ROM_FONT) {  //ROMフォントを使う
      boolean pointed = (stat & KBD_POINTED_MASK) != 0;
      boolean pressed = (stat & KBD_PRESSED_MASK) != 0;
      //1段目
      if (!pressed) {
        kbdDrawLine (bitmap, x + width * y,
                     1, 1, kbdTopLeftRGB, 1, w - 2, kbdTopRGB,
                     width, 1, kbdTopRightRGB, width, h - 2, kbdRightRGB,
                     -1, 1, kbdBottomRightRGB, -1, w - 2, kbdBottomRGB,
                     -width, 1, kbdBottomLeftRGB, -width, h - 2, kbdLeftRGB);
      } else {
        kbdDrawLine (bitmap, x + width * y,
                     1, 1, kbdHoleRGB, 1, w - 2, kbdHoleRGB,
                     width, 1, kbdHoleRGB, width, h - 2, kbdHoleRGB,
                     -1, 1, kbdHoleRGB, -1, w - 2, kbdHoleRGB,
                     -width, 1, kbdHoleRGB, -width, h - 2, kbdHoleRGB);
      }
      x += 1 + u;
      y += 1 + v;
      w -= 2 + u * 2;
      h -= 2 + v * 2;
      //島
      if (u > 0) {
        kbdFillRect (bitmap, width, x - u, y, u, h, kbdKeytopRGB);  //左側
        kbdFillRect (bitmap, width, x + w, y, u, h, kbdKeytopRGB);  //右側
      }
      if (v > 0) {
        kbdFillRect (bitmap, width, x, y - v, w, v, kbdKeytopRGB);  //上側
        kbdFillRect (bitmap, width, x, y + h, w, v, kbdKeytopRGB);  //下側
      }
      //2段目
      int lx = x;
      int ly = y;
      int lw = w;
      int lh = h;
      kbdDrawLine (bitmap, x + width * y,
                   1, 1, kbdTopLeftRGB, 1, w - 2, kbdTopRGB,
                   width, 1, kbdTopRightRGB, width, h - 2, kbdRightRGB,
                   -1, 1, kbdBottomRightRGB, -1, w - 2, kbdBottomRGB,
                   -width, 1, kbdBottomLeftRGB, -width, h - 2, kbdLeftRGB);
      x++;
      y++;
      w -= 2;
      h -= 2;
      //3段目
      kbdDrawLine (bitmap, x + width * y,
                   1, 1, kbdTopLeftRGB, 1, w - 2, kbdTopRGB,
                   width, 1, kbdTopRightRGB, width, h - 2, kbdRightRGB,
                   -1, 1, kbdBottomRightRGB, -1, w - 2, kbdBottomRGB,
                   -width, 1, kbdBottomLeftRGB, -width, h - 2, kbdLeftRGB);
      x++;
      y++;
      w -= 2;
      h -= 2;
      //キートップ
      kbdFillRect (bitmap, width, x, y, w, h, pointed ? kbdPointedRGB : kbdKeytopRGB);
      //LED
      if (ledColors != null) {
        x += w - KBD_LED_WIDTH >> 1;
        y += h;
        kbdFillRect (bitmap, width, x, y - KBD_LED_HEIGHT, KBD_LED_WIDTH, KBD_LED_HEIGHT, ledColors[0]);  //上半分
        kbdFillRect (bitmap, width, x, y, KBD_LED_WIDTH, pressed ? 2 : 3, ledColors[1]);  //下半分の高さはボーダーの幅
      }
      //文字
      if (code != 0) {  //文字あり
        kbdDrawLetter2 (bitmap, width, lx, ly, lw, lh, code);
      }
    }  //if KBD_USE_ROM_FONT
  }  //kbdDrawKey2

  //kbdFillRect (bitmap, width, x, y, w, h, color)
  //  矩形を塗り潰す
  //  bitmap  ビットマップ
  //  width   bitmapの幅(px)
  //  x       x座標(px)
  //  y       y座標(px)
  //  w       幅(px)
  //  h       高さ(px)
  //  color   色(rgb)
  public static void kbdFillRect (int[] bitmap, int width, int x, int y, int w, int h, int color) {
    for (int j = 0; j < h; j++) {
      int dst = x + width * (y + j);
      for (int i = 0; i < w; i++) {
        bitmap[dst + i] = color;
      }
    }
  }  //kbdFillRect(int[],int,int,int,int,int,int)

  //kbdDrawLetter (bitmap, width, lx, ly, lw, lh, letter)
  //  文字のパターンを描く
  //  影を付ける
  //  bitmap  ビットマップ
  //  width   bitmapの幅(px)
  //  lx      x座標(px)
  //  ly      y座標(px)
  //  lw      幅(px)
  //  lh      高さ(px)
  //  letter  文字のパターン
  public static void kbdDrawLetter (int[] bitmap, int width, int lx, int ly, int lw, int lh, int[] letter) {
    if (lw <= 32) {
      for (int j = 0; j < lh; j++) {
        int dst = lx + width * (ly + j);
        for (int i = 0; i < lw; i++) {
          if ((letter[j] >> lw - 1 - i & 1) != 0) {
            bitmap[dst + i] = kbdLetterRGB;
          } else if (i > 0 && j > 0 && (letter[j - 1] >> lw - 1 - (i - 1) & 1) != 0) {
            bitmap[dst + i] = kbdShadowRGB;
            //bitmap[dst + i] = 0xff000000 | bitmap[dst + i] >> 2 & 0x003f3f3f;
            //bitmap[dst + i] = 0xff000000 | bitmap[dst + i] >> 1 & 0x007f7f7f;
          }
        }
      }
    } else {
      int n = lw + 31 >> 5;  //1ラスタあたりの文字データの数
      for (int j = 0; j < lh; j++) {
        int src = n * j + n - 1;  //文字データの右端
        int dst = lx + width * (ly + j) + lw - 1;  //キートップの右端
        for (int i = 0; i < lw; i++) {  //右端が0,左端がlw-1
          if ((letter[src - (i >> 5)] >> (i & 0x1f) & 1) != 0) {
            bitmap[dst - i] = kbdLetterRGB;
          } else if (i > 0 && j > 0 && (letter[src - n - (i + 1 >> 5)] >> (i + 1 & 0x1f) & 1) != 0) {
            bitmap[dst - i] = kbdShadowRGB;
            //bitmap[dst - i] = 0xff000000 | bitmap[dst - i] >> 2 & 0x003f3f3f;
            //bitmap[dst - i] = 0xff000000 | bitmap[dst - i] >> 1 & 0x007f7f7f;
          }
        }
      }
    }
  }  //kbdDrawLetter(int[],int,int,int,int,int,int[])

  //kbdDrawLetter2 (bitmap, width, lx, ly, lw, lh, code)
  //  文字のパターンを描く
  //  影を付ける
  //  bitmap  ビットマップ
  //  width   bitmapの幅(px)
  //  lx      x座標(px)
  //  ly      y座標(px)
  //  lw      幅(px)
  //  lh      高さ(px)
  //  code    文字コード
  public static void kbdDrawLetter2 (int[] bitmap, int width, int lx, int ly, int lw, int lh, int code) {
    final int fw = 8;
    final int fh = 8;
    final int fa = 0x00f3a000;  //ANK8x8
    int dst = lx + (lw - fw >> 1) + width * (ly + (lh - fh >> 1));
    int src = fa + (fw + 7 >> 3) * fh * code;
    for (int y = 0; y < fh; y++) {
      for (int x = 0; x < fw; x++) {
        if ((MainMemory.mmrM8[src + (fw + 7 >> 3) * y + (x >> 3)] & 1 << (~x & 0x07)) != 0) {
          bitmap[dst + width * y + x] = kbdLetterRGB;
        } else if (x > 0 && y > 0 &&
                   (MainMemory.mmrM8[src + (fw + 7 >> 3) * (y - 1) + (x - 1 >> 3)] & 1 << (~(x - 1) & 0x07)) != 0) {
          bitmap[dst + width * y + x] = kbdShadowRGB;
        }
      }
    }
  }  //kbdDrawLetter2(int[],int,int,int,int,int,int)

  //kbdDrawLine (bitmap, pos, vect0, step0, color0, vect1, step1, color1, ...)
  //  bitmap  ビットマップ
  //  pos     開始位置のオフセット
  //  vect    方向。オフセットの差分
  //  step    長さ(px)
  //  color   色(rgb)
  public static void kbdDrawLine (int[] bitmap, int pos, int... vect_step_color) {
    for (int i = 0; i < vect_step_color.length; i += 3) {
      int vect = vect_step_color[i];
      int step = vect_step_color[i + 1];
      int color = vect_step_color[i + 2];
      for (; step > 0; step--) {
        bitmap[pos] = color;
        pos += vect;
      }
    }
  }  //kbdDrawLine(int[],int,int...)

  //kbdStart ()
  //  キーボードのイベントリスナーを設定して動作を開始する
  //  MFPが初期化されている必要がある
  public static void kbdStart () {

    //Tabキーによるフォーカスの移動を無効にする
    //  setFocusTraversalKeysにCollections.EMPTY_SETを指定すると無検査変換の警告が出る
    //XEiJ.pnlPanel.setFocusTraversalKeys (KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, Collections.EMPTY_SET);
    //XEiJ.pnlPanel.setFocusTraversalKeys (KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, Collections.EMPTY_SET);
    //XEiJ.pnlPanel.setFocusTraversalKeys (KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS, Collections.EMPTY_SET);
    XEiJ.pnlCanvasOrPanel.setFocusTraversalKeysEnabled (false);  //Component.setFocusTraversalKeysEnabled()は1.4から

    //フォーカスリスナーとキーリスナー
    XEiJ.pnlCanvasOrPanel.setFocusable (true);  //普通のコンポーネントはクリックしてもフォーカスされないのでキーイベントが発生しない
    ComponentFactory.addListener (
      XEiJ.pnlCanvasOrPanel,
      new FocusAdapter () {
        @Override public void focusGained (FocusEvent fe) {
          kbdCloseIME ();  //IMEが開いていたら閉じる
        }
        //@Override public void focusLost (FocusEvent fe) {
        //}
      });
    XEiJ.pnlCanvasOrPanel.requestFocusInWindow ();  //フォーカスを要求する。Component.requestFocusInWindow()は1.4から
    ComponentFactory.addListener (
      XEiJ.pnlCanvasOrPanel,
      new KeyAdapter () {
        @Override public void keyPressed (KeyEvent ke) {
          if (Mouse.musOutputButtonStatus) {
            System.out.println ("when=" + ke.getWhen () + " toString=\"" + ke.toString () + "\"");
            //System.out.println (String.format ("keyPressed keyChar=0x%04x keyCode=0x%08x extendedKeyCode=0x%08x keyLocation=%d",
            //                                   (int) ke.getKeyChar (), ke.getKeyCode (), ke.getExtendedKeyCode (), ke.getKeyLocation ()));
          }
          kbdCloseIME ();  //IMEが開いていたら閉じる
          int keyCode = ke.getKeyCode ();
          if (KBD_ZKEY_ON && kbdZKeyPort != null && !kbdZKeyIsUsb) {  //Zキーボード
            switch (keyCode) {
            case KeyEvent.VK_HALF_WIDTH:  //BREAK 半角(VK 244)
            case KeyEvent.VK_FULL_WIDTH:  //BREAK 全角(VK 243)
              //pressedを無視する
              break;
            case KeyEvent.VK_F17:  //CAPS 英数字(VK 240)→F17
              //pressedで入力してすぐ離す
              kbdInput (ke, true);
              kbdInput (ke, false);
              break;
            case KeyEvent.VK_F16:  //XF4 ひらがな(VK 242)→F16
              //pressedで入力して他のキーが離されまで押されたままにする
              kbdInput (ke, true);
              kbdXF4Locked = ke;
              break;
            case KeyEvent.VK_NUM_LOCK:  //CLR NumLock
              if (kbdIgnoreNumLock) {  //NumLockを1回無視する
              } else {
                kbdInput (ke, true);
              }
              break;
            default:  //その他
              kbdInput (ke, true);
            }
          } else if (XEiJ.prgIsWindows &&  //Windows。WSLはWindowsキーではイベントが発生しない
                     keyCode == KeyEvent.VK_WINDOWS) {  //Windowsキー
            kbdInput (ke, true);  //押して離す
            kbdInput (ke, false);
          } else {  //その他
            kbdInput (ke, true);
          }
          ke.consume ();
        }
        @Override public void keyReleased (KeyEvent ke) {
          if (Mouse.musOutputButtonStatus) {
            System.out.println ("when=" + ke.getWhen () + " toString=\"" + ke.toString () + "\"");
            //System.out.println (String.format ("keyReleased keyChar=0x%04x keyCode=0x%08x extendedKeyCode=0x%08x keyLocation=%d",
            //                                   (int) ke.getKeyChar (), ke.getKeyCode (), ke.getExtendedKeyCode (), ke.getKeyLocation ()));
          }
          kbdCloseIME ();  //IMEが開いていたら閉じる
          int keyCode = ke.getKeyCode ();
          int keyLocation = ke.getKeyLocation ();
          if (KBD_ZKEY_ON && kbdZKeyPort != null && !kbdZKeyIsUsb) {  //Zキーボード
            switch (keyCode) {
            case KeyEvent.VK_HALF_WIDTH:  //BREAK 半角(VK 244)
            case KeyEvent.VK_FULL_WIDTH:  //BREAK 全角(VK 243)
              //releasedで入力してすぐ離す
              kbdInput (ke, true);
              kbdInput (ke, false);
              break;
            case KeyEvent.VK_F17:  //CAPS 英数字(VK 240)→F17
            case KeyEvent.VK_F16:  //XF4 ひらがな(VK 242)→F16
              //releasedを無視する
              break;
            case KeyEvent.VK_NUM_LOCK:  //CLR NumLock
              if (kbdIgnoreNumLock) {  //NumLockを1回無視する
                kbdIgnoreNumLock = false;
              } else {
                kbdInput (ke, false);
                //NumLockを点灯させる
                if (!kbdZKeyPort.isKeyToggled (KeyEvent.VK_NUM_LOCK)) {  //NumLockが消灯している
                  kbdIgnoreNumLock = true;  //NumLockを1回無視する
                  kbdZKeyPort.hitKey (KeyEvent.VK_NUM_LOCK);  //NumLockを叩いて点灯させる。直ちに発火することに注意
                }
              }
              break;
            default:  //その他
              kbdInput (ke, false);
              if (kbdXF4Locked != null) {  //XF4が押されている
                kbdInput (kbdXF4Locked, false);  //XF4を離す
                kbdXF4Locked = null;
              }
            }
          } else {  //Zキーボードではない
            kbdInput (ke, false);
          }
          ke.consume ();
        }
        @Override public void keyTyped (KeyEvent ke) {
          if (Mouse.musOutputButtonStatus) {
            System.out.println ("when=" + ke.getWhen () + " toString=\"" + ke.toString () + "\"");
            //System.out.println (String.format ("keyTyped keyChar=0x%04x keyCode=0x%08x extendedKeyCode=0x%08x keyLocation=%d",
            //                                   (int) ke.getKeyChar (), ke.getKeyCode (), ke.getExtendedKeyCode (), ke.getKeyLocation ()));
          }
          kbdCloseIME ();  //IMEが開いていたら閉じる
          ke.consume ();
        }
      });

    //IMEが開いていたら閉じる
    kbdCloseIME ();

    if (kbdOn) {
      //点灯要求コードを送出する
      if (MC68901.MFP_KBD_ON) {
        MC68901.mfpKbdInput (0xff, false);
      } else {
        kbdTransfer (0xff, false);
      }
    }

    //キーボード設定ウィンドウの準備
    if (RestorableFrame.rfmGetOpened (Settings.SGS_KBS_FRAME_KEY)) {
      kbdOpen ();
    }

  }  //kbdStart()

  //kbdOpen ()
  //  キーボード設定ウィンドウを開く
  public static void kbdOpen () {
    if (kbdFrame == null) {
      kbdMakeFrame ();
    }
    XEiJ.pnlExitFullScreen (false);
    kbdFrame.setVisible (true);
  }  //kbdOpen()

  //kbdMakeFrame ()
  //  キーボード設定ウィンドウを作る
  //  ここでは開かない
  public static void kbdMakeFrame () {

    //キーマップエディタ
    kbdEditor = new KeyMapEditor (kbdCurrentMap);

    //アクションリスナー
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        switch (ae.getActionCommand ()) {
        case "Blank":  //白紙
          kbdEditor.blank ();
          break;
        case "US layout":  //英語配列
          kbdUSLayoutOn = ((JCheckBox) source).isSelected ();
          kbdDefaultMapNonZ = (XEiJ.prgIsLinux ? KBD_DEFAULT_MAP_LINUX :
                               XEiJ.prgIsMac ?
                               kbdUSLayoutOn ? KBD_DEFAULT_MAP_MAC_US : KBD_DEFAULT_MAP_MAC :
                               kbdUSLayoutOn ? KBD_DEFAULT_MAP_WIN_US : KBD_DEFAULT_MAP_WIN);
          break;
        case "Default":  //初期値
          kbdEditor.reset (KBD_ZKEY_ON && kbdZKeyPort != null && !kbdZKeyIsUsb ? kbdDefaultMapZ : kbdDefaultMapNonZ);
          break;
        case "Save":  //保存
          kbdEditor.save ();
          break;
        case "Restore":  //復元
          kbdEditor.restore ();
          break;
        case "Undo":  //取り消し
          kbdEditor.undo ();
          break;
        case "Redo":  //やり直し
          kbdEditor.redo ();
          break;
        }
      }
    };

    //ウインドウ
    kbdFrame = Multilingual.mlnTitle (
      ComponentFactory.createRestorableSubFrame (
        Settings.SGS_KBS_FRAME_KEY,
        "Key assignment",
        null,
        ComponentFactory.createVerticalBox (
          ComponentFactory.createFlowPanel (
            FlowLayout.CENTER, 10, 2,
            Multilingual.mlnText (ComponentFactory.createButton ("Blank", listener), "ja", "白紙"),
            ComponentFactory.setLineBorder (
              ComponentFactory.createFlowPanel (2, 2,
                Multilingual.mlnText (ComponentFactory.createCheckBox (kbdUSLayoutOn, "US layout", listener), "ja", "英語配列"),
                Multilingual.mlnText (ComponentFactory.createButton ("Default", listener), "ja", "初期値")
                )
              ),
            Multilingual.mlnText (ComponentFactory.createButton ("Save", listener), "ja", "保存"),
            Multilingual.mlnText (ComponentFactory.createButton ("Restore", listener), "ja", "復元"),
            Multilingual.mlnText (ComponentFactory.createButton ("Undo", listener), "ja", "取り消し"),
            Multilingual.mlnText (ComponentFactory.createButton ("Redo", listener), "ja", "やり直し")
            ),
          kbdEditor.getPanel ()
          )
        ),
      "ja", "キー割り当て");

  }  //kbdMakeFrame()


  //kbdCloseIME ()
  //  IMEが開いていたら閉じる
  //  継続的にIMEを無効化する方法が見当たらないのでIMEを開くキーが押されたらすぐに閉じることで擬似的にIMEを無効化する
  public static void kbdCloseIME () {
    try {
      InputContext context = XEiJ.pnlCanvasOrPanel.getInputContext ();
      if (context != null && context.isCompositionEnabled ()) {
        context.setCompositionEnabled (false);
        context.setCharacterSubsets (null);
      }
    } catch (UnsupportedOperationException uoe) {
    }
  }  //kbdCloseIME()

  //kbdSetOn (on)
  //  キーボードを表示する/表示しない
  public static void kbdSetOn (boolean on) {
    if (kbdOn != on) {
      kbdOn = on;
      if (on) {
        if (false) {
          if (kbdType == KBD_STANDARD_TYPE) {
            System.out.println (Multilingual.mlnJapanese ?
                                "標準キーボードを接続します" :
                                "Connecting standard keyboard");
          } else if (kbdType == KBD_COMPACT_TYPE) {
            System.out.println (Multilingual.mlnJapanese ?
                                "コンパクトキーボードを接続します" :
                                "Connecting compact keyboard");
          }
        }
        kbdWidth = kbdDataTypeWidth[kbdType];
        kbdHeight = kbdDataTypeHeight[kbdType];
        kbdImage = kbdDataTypeImage[kbdType];
        kbdBitmap = kbdDataTypeBitmap[kbdType];
        kbdIndexMap = kbdDataTypeIndexMap[kbdType];
        kbdFlush ();
        //点灯要求コードを送出する
        if (MC68901.MFP_KBD_ON) {
          MC68901.mfpKbdInput (0xff, false);
        } else {
          kbdTransfer (0xff, false);
        }
      } else {
        if (false) {
          System.out.println (Multilingual.mlnJapanese ?
                              "キーボードを切り離します" :
                              "Disconnecting keyboard");
        }
        //ポイントされているキーがあれば解除する
        if (kbdPointedIndex >= 0) {
          kbdHover (0, 0);
        }
        kbdWidth = 0;
        kbdHeight = 0;
        kbdImage = null;
        kbdBitmap = null;
        kbdIndexMap = null;
      }
      if (XEiJ.pnlMode == XEiJ.PNL_FULLSCREEN) {  //全画面表示
        if (XEiJ.pnlHideKeyboard) {  //全画面表示のときキーボードを隠す
          XEiJ.pnlPrevKeyboardOn = kbdImage != null;  //変更されたので復元しない
        }
      }
    }
  }  //kbdSetOn(boolean)

  //kbdSetType (type)
  //  キーボードの種類を変更する
  //  type  キーボードの種類。KEY_STANDARD_TYPEまたはKBD_COMPACT_TYPE
  public static void kbdSetType (int type) {
    if (kbdType != type) {
      kbdType = type;
      if (kbdOn) {
        if (false) {
          System.out.println (Multilingual.mlnJapanese ?
                              "キーボードを切り離します" :
                              "Disconnecting keyboard");
        }
        //ポイントされているキーがあれば解除する
        if (kbdPointedIndex >= 0) {
          kbdHover (0, 0);
        }
        if (false) {
          if (kbdType == KBD_STANDARD_TYPE) {
            System.out.println (Multilingual.mlnJapanese ?
                                "標準キーボードを接続します" :
                                "Connecting standard keyboard");
          } else if (kbdType == KBD_COMPACT_TYPE) {
            System.out.println (Multilingual.mlnJapanese ?
                                "コンパクトキーボードを接続します" :
                                "Connecting compact keyboard");
          }
        }
        kbdWidth = kbdDataTypeWidth[kbdType];
        kbdHeight = kbdDataTypeHeight[kbdType];
        kbdImage = kbdDataTypeImage[kbdType];
        kbdBitmap = kbdDataTypeBitmap[kbdType];
        kbdIndexMap = kbdDataTypeIndexMap[kbdType];
        kbdFlush ();
        //点灯要求コードを送出する
        if (MC68901.MFP_KBD_ON) {
          MC68901.mfpKbdInput (0xff, false);
        } else {
          kbdTransfer (0xff, false);
        }
      }
    }
  }  //kbdSetType(int)

  //kbdRepaint ()
  //  キーボードが表示されているときkbdImageを作り直して再描画する
  public static void kbdRepaint () {
    if (kbdOn) {  //キーボードが表示されているとき
      kbdFlush ();  //kbdImageを作り直して
      if (!XEiJ.PNL_USE_THREAD) {
        XEiJ.pnlPanel.repaint (XEiJ.pnlKeyboardX, XEiJ.pnlKeyboardY, kbdWidth, kbdHeight);  //再描画する
      }
    }
  }  //kbdRepaint()

  //kbdFlush ()
  //  kbdImageを作り直す
  public static void kbdFlush () {
    int width = kbdDataTypeWidth[kbdType];
    int[][] indexRect = kbdDataTypeIndexRect[kbdType];
    int[] indexCharacterStat = kbdDataTypeIndexCharacterStat[kbdType];
    int[][] statFixBitmap = kbdDataTypeStatFixBitmap[kbdType];
    //LEDキーとモードキーの状態によってキートップに表示する文字の配列を選択する
    boolean numOff = KBD_LED_TEST << (kbdStat[KBD_NUM_INDEX] & (KBD_LIGHTED_MASK | KBD_PRESSED_MASK | KBD_POINTED_MASK)) >= 0;
    boolean ctrlOn = KBD_MODE_TEST << (kbdStat[KBD_CTRL_INDEX] & (KBD_LIGHTED_MASK | KBD_PRESSED_MASK | KBD_POINTED_MASK)) < 0;
    boolean codeOn = KBD_LED_TEST << (kbdStat[KBD_CODE_INDEX] & (KBD_LIGHTED_MASK | KBD_PRESSED_MASK | KBD_POINTED_MASK)) < 0;
    boolean opt1Off = KBD_MODE_TEST << (kbdStat[KBD_OPT1_INDEX] & (KBD_LIGHTED_MASK | KBD_PRESSED_MASK | KBD_POINTED_MASK)) >= 0;
    boolean kanaOn = KBD_LED_TEST << (kbdStat[KBD_KANA_INDEX] & (KBD_LIGHTED_MASK | KBD_PRESSED_MASK | KBD_POINTED_MASK)) < 0;
    boolean shiftOff = KBD_MODE_TEST << (kbdStat[KBD_SHIFT_INDEX] & (KBD_LIGHTED_MASK | KBD_PRESSED_MASK | KBD_POINTED_MASK)) >= 0;
    boolean romaOn = KBD_LED_TEST << (kbdStat[KBD_ROMA_INDEX] & (KBD_LIGHTED_MASK | KBD_PRESSED_MASK | KBD_POINTED_MASK)) < 0;
    boolean capsOff = KBD_LED_TEST << (kbdStat[KBD_CAPS_INDEX] & (KBD_LIGHTED_MASK | KBD_PRESSED_MASK | KBD_POINTED_MASK)) >= 0;
    boolean zenOn = KBD_LED_TEST << (kbdStat[KBD_ZEN_INDEX] & (KBD_LIGHTED_MASK | KBD_PRESSED_MASK | KBD_POINTED_MASK)) < 0;
    boolean hiraOn = KBD_LED_TEST << (kbdStat[KBD_HIRA_INDEX] & (KBD_LIGHTED_MASK | KBD_PRESSED_MASK | KBD_POINTED_MASK)) < 0;
    byte[] varChr =
      (numOff
       ?  //-NUM
       (ctrlOn
        ?  //-NUM+CTRL
        KBD_CTRL_CHR
        :  //-NUM-CTRL
        (codeOn
         ?  //-NUM-CTRL+CODE
         KBD_CODE_CHR
         :  //-NUM-CTRL-CODE
         (opt1Off && kanaOn
          ?  //-NUM-CTRL-CODE+KANA
          (XEiJ.busPbs (0x00ed002b) == 0
           ?  //-NUM-CTRL-CODE+KANA-AIU
           (shiftOff
            ?  //-NUM-CTRL-CODE+KANA-AIU-SHIFT
            KBD_KANA_CHR
            :  //-NUM-CTRL-CODE+KANA-AIU+SHIFT
            KBD_KANA_SHIFT_CHR
            )  //!SHIFT
           :  //-NUM-CTRL-CODE+KANA+AIU
           (shiftOff
            ?  //-NUM-CTRL-CODE+KANA+AIU-SHIFT
            KBD_KANA_AIU_CHR
            :  //-NUM-CTRL-CODE+KANA+AIU+SHIFT
            KBD_KANA_AIU_SHIFT_CHR
            )  //!SHIFT
           )  //!AIU
          :  //-NUM-CTRL-CODE-KANA
          (opt1Off && romaOn
           ?  //-NUM-CTRL-CODE-KANA+ROMA
           (shiftOff
            ?  //-NUM-CTRL-CODE-KANA+ROMA-SHIFT
            KBD_ROMA_CHR
            :  //-NUM-CTRL-CODE-KANA+ROMA+SHIFT
            KBD_ROMA_SHIFT_CHR
            )  //!SHIFT
           :  //-NUM-CTRL-CODE-KANA-ROMA
           (capsOff
            ?  //-NUM-CTRL-CODE-KANA-ROMA-CAPS
            (shiftOff
             ?  //-NUM-CTRL-CODE-KANA-ROMA-CAPS-SHIFT
             KBD_NORMAL_CHR
             :  //-NUM-CTRL-CODE-KANA-ROMA-CAPS+SHIFT
             KBD_SHIFT_CHR
             )  //!SHIFT
            :  //-NUM-CTRL-CODE-KANA-ROMA+CAPS
            (shiftOff
             ?  //-NUM-CTRL-CODE-KANA-ROMA+CAPS-SHIFT
             KBD_CAPS_CHR
             :  //-NUM-CTRL-CODE-KANA-ROMA+CAPS+SHIFT
             KBD_CAPS_SHIFT_CHR
             )  //!SHIFT
            )  //!CAPS
           )  //!OPT.1&&ROMA
          )  //!OPT.1&&KANA
         )  //CODE
        )  //CTRL
       :  //+NUM
       (ctrlOn
        ?  //+NUM+CTRL
        KBD_NUM_CTRL_CHR
        :  //+NUM-CTRL
        (codeOn
         ?  //+NUM-CTRL+CODE
         KBD_NUM_CODE_CHR
         :  //+NUM-CTRL-CODE
         (opt1Off && kanaOn
          ?  //+NUM-CTRL-CODE+KANA
          (MainMemory.mmrM8[0x00ed002b] == 0
           ?  //+NUM-CTRL-CODE+KANA-AIU
           (shiftOff
            ?  //+NUM-CTRL-CODE+KANA-AIU-SHIFT
            KBD_NUM_KANA_CHR
            :  //+NUM-CTRL-CODE+KANA-AIU+SHIFT
            KBD_NUM_KANA_SHIFT_CHR
            )  //!SHIFT
           :  //+NUM-CTRL-CODE+KANA+AIU
           (shiftOff
            ?  //+NUM-CTRL-CODE+KANA+AIU-SHIFT
            KBD_NUM_KANA_AIU_CHR
            :  //+NUM-CTRL-CODE+KANA+AIU+SHIFT
            KBD_NUM_KANA_AIU_SHIFT_CHR
            )  //!SHIFT
           )  //!AIU
          :  //+NUM-CTRL-CODE-KANA
          (opt1Off && romaOn
           ?  //+NUM-CTRL-CODE-KANA+ROMA
           (shiftOff
            ?  //+NUM-CTRL-CODE-KANA+ROMA-SHIFT
            KBD_NUM_ROMA_CHR
            :  //+NUM-CTRL-CODE-KANA+ROMA+SHIFT
            KBD_NUM_ROMA_SHIFT_CHR
            )  //!SHIFT
           :  //+NUM-CTRL-CODE-KANA-ROMA
           (capsOff
            ?  //+NUM-CTRL-CODE-KANA-ROMA-CAPS
            (shiftOff
             ?  //+NUM-CTRL-CODE-KANA-ROMA-CAPS-SHIFT
             KBD_NUM_CHR
             :  //+NUM-CTRL-CODE-KANA-ROMA-CAPS+SHIFT
             KBD_NUM_SHIFT_CHR
             )  //!SHIFT
            :  //+NUM-CTRL-CODE-KANA-ROMA+CAPS
            (shiftOff
             ?  //+NUM-CTRL-CODE-KANA-ROMA+CAPS-SHIFT
             KBD_NUM_CAPS_CHR
             :  //+NUM-CTRL-CODE-KANA-ROMA+CAPS+SHIFT
             KBD_NUM_CAPS_SHIFT_CHR
             )  //!SHIFT
            )  //!CAPS
           )  //!OPT.1&&ROMA
          )  //!OPT.1&&KANA
         )  //CODE
        )  //CTRL
       );  //!NUM
    for (int i = 0; i < KBD_ALL_KEYS; i++) {
      int chr = 0;  //可変キーの文字。固定キーの文字は変化しないので0のままでよい
      if (i < KBD_VAR_KEYS) {  //可変キー
        chr = varChr[i] & 255;
        if (0xa6 <= chr && chr <= 0xdd && chr != 0xb0 &&  //記号を除くカタカナで
            zenOn && hiraOn) {  //全角とひらがなが両方点灯しているとき
          chr ^= 0x20;  //ひらがなにする
        } else if (chr == 0x5c && (MainMemory.mmrM8[0x00ed0059] & 0b00000001) != 0) {
          chr = 0x80;
        } else if (chr == 0x7e && (MainMemory.mmrM8[0x00ed0059] & 0b00000010) != 0) {
          chr = 0x81;
        } else if (chr == 0x7c && (MainMemory.mmrM8[0x00ed0059] & 0b00000100) != 0) {
          chr = 0x82;
        }
      }
      int stat = kbdStat[i] & (KBD_BRIGHTNESS_MASK | KBD_LIGHTED_MASK | KBD_PRESSED_MASK | KBD_POINTED_MASK);
      int characterStat = chr << 8 | stat;
      if (indexCharacterStat[i] != characterStat) {  //最後に描画した状態から変化した
        indexCharacterStat[i] = characterStat;
        int[] rect = indexRect[i];
        if (i < KBD_VAR_KEYS) {  //可変キー
          int[] bitmap = kbdVarStatBitmap[chr][stat];
          int p = rect[0] + width * rect[1];
          int w = rect[2];
          int h = rect[3];
          int q = 0;
          for (int dy = 0; dy < h; dy++) {
            for (int dx = 0; dx < w; dx++) {
              kbdBitmap[p++] = bitmap[q++];
            }
            p += width - w;
          }
        } else if (i < KBD_VAR_KEYS + KBD_LED_KEYS) {  //LEDキー
          int[] bitmap = kbdLedStatBitmap[i - KBD_VAR_KEYS][stat];
          if (rect.length >= 4) {  //Standard KeyboardはNUMキーがない
            int p = rect[0] + width * rect[1];
            int w = rect[2];
            int h = rect[3];
            int q = 0;
            for (int dy = 0; dy < h; dy++) {
              for (int dx = 0; dx < w; dx++) {
                kbdBitmap[p++] = bitmap[q++];
              }
              p += width - w;
            }
          }
        } else {  //固定キー
          int[] bitmap = statFixBitmap[stat];
          if (rect.length >= 4) {  //Compact Keyboardはテンキーがない
            int p = rect[0] + width * rect[1];
            int w = rect[2];
            int h = rect[3];
            for (int dy = 0; dy < h; dy++) {
              for (int dx = 0; dx < w; dx++) {
                kbdBitmap[p] = bitmap[p];
                p++;
              }
              p += width - w;
            }
            if (rect.length >= 8) {  //RETURNキーとSHIFTキー
              p = rect[4] + width * rect[5];
              w = rect[6];
              h = rect[7];
              for (int dy = 0; dy < h; dy++) {
                for (int dx = 0; dx < w; dx++) {
                  kbdBitmap[p] = bitmap[p];
                  p++;
                }
                p += width - w;
              }
            }
          }
        }
      }
    }
  }  //kbdFlush()

  //kbdHover (x, y)
  //  ポイントされているキーを変更する
  //  座標はキーボードのローカル座標で範囲内であること
  public static void kbdHover (int x, int y) {
    boolean flag = false;
    int i = kbdIndexMap[x + kbdWidth * y];  //今回ポイントされたキー。-1=キーがポイントされていない
    if (kbdPointedIndex != i) {  //ポイントされているキーが変わった
      if (kbdPointedIndex >= 0) {  //前回ポイントされていたキーがあった
        int stat = kbdStat[kbdPointedIndex];
        kbdStat[kbdPointedIndex] = stat &= ~KBD_POINTED_MASK;  //ポイントを外す
        if ((stat & KBD_MOUSEDOWN_MASK) != 0) {  //マウスで押されていた
          kbdStat[kbdPointedIndex] = stat &= ~KBD_MOUSEDOWN_MASK;  //マウスを離す
          if ((stat & (KBD_LOCKED_MASK | KBD_KEYDOWN_MASK | KBD_MOUSEDOWN_MASK)) == 0) {  //ロックされておらずキーもマウスも押されていない
            kbdStat[kbdPointedIndex] = stat &= ~KBD_PRESSED_MASK;  //離す
            kbdRelease (kbdPointedIndex);
          }
        }
      }
      kbdPointedIndex = i;
      flag = true;
    }
    if (kbdPointedIndex >= 0) {  //ポイントされているキーがある
      int stat = kbdStat[kbdPointedIndex];
      kbdStat[kbdPointedIndex] = stat |= KBD_POINTED_MASK;  //ポイントする。前回もポイントされていた場合もある
      if (((stat & KBD_MOUSEDOWN_MASK) != 0) != (Mouse.musButtonLeft || Mouse.musButtonRight)) {  //マウスの押し下げ状態が変わった
        if (Mouse.musButtonLeft && (kbdPointedIndex == KBD_SHIFT_INDEX ||  //マウスで押されていなかったSHIFTキーが左ボタンで押された
                                    kbdPointedIndex == KBD_CTRL_INDEX) ||  //マウスで押されていなかったCTRLキーが左ボタンで押された
            Mouse.musButtonRight) {  //マウスで押されていなかったキーが右ボタンで押された
          stat ^= KBD_LOCKED_MASK;  //ロックを反転する
          kbdStat[kbdPointedIndex] = stat |= KBD_MOUSEDOWN_MASK;  //マウスで押す
          if ((stat & KBD_PRESSED_MASK) == 0) {  //押されていなかった
            kbdStat[kbdPointedIndex] = stat |= KBD_PRESSED_MASK;  //押す
            kbdPress (kbdPointedIndex);
          }
        } else if (Mouse.musButtonLeft) {  //マウスで押されていなかったキーが左ボタンで押された
          stat &= ~KBD_LOCKED_MASK;  //ロックを解除する
          kbdStat[kbdPointedIndex] = stat |= KBD_MOUSEDOWN_MASK;  //マウスで押す
          if ((stat & KBD_PRESSED_MASK) == 0) {  //押されていなかった
            kbdStat[kbdPointedIndex] = stat |= KBD_PRESSED_MASK;  //押す
            kbdPress (kbdPointedIndex);
          }
        } else {  //マウスで押されていたキーが離された
          kbdStat[kbdPointedIndex] = stat &= ~KBD_MOUSEDOWN_MASK;  //マウスを離す
          if ((stat & (KBD_LOCKED_MASK | KBD_KEYDOWN_MASK | KBD_MOUSEDOWN_MASK)) == 0) {  //ロックされておらずキーもマウスも押されていない
            kbdStat[kbdPointedIndex] = stat &= ~KBD_PRESSED_MASK;  //離す
            kbdRelease (kbdPointedIndex);
          }
        }
        flag = true;
      }
    }
    if (flag) {
      kbdRepaint ();
    }
  }  //kbdHover(int,int)

  //kbdInput (ke, pressed)
  //  リアルキーボードから入力する
  public static void kbdInput (KeyEvent ke, boolean pressed) {
    int keyCode = ke.getKeyCode ();
    int modifiersEx = ke.getModifiersEx ();
    if (pressed) {
      if ((modifiersEx & XEiJ.MNB_MODIFIERS) == XEiJ.MNB_MODIFIERS) {  //Alt+Shift
        switch (keyCode) {
        case KeyEvent.VK_C:  //Alt+Shift-C
          TextCopy.txcCopy ();
          return;
        case KeyEvent.VK_D:  //Alt+Shift-D
          XEiJ.mnbDisplayMenu.doClick ();
          return;
        case KeyEvent.VK_F:  //Alt+Shift-F
          XEiJ.mnbFileMenu.doClick ();
          return;
        case KeyEvent.VK_G:  //Alt+Shift-G
          XEiJ.mnbConfigMenu.doClick ();
          return;
        case KeyEvent.VK_I:  //Alt+Shift-I
          XEiJ.mnbInputMenu.doClick ();
          return;
        case KeyEvent.VK_K:  //Alt+Shift-K
          if (kbdOn) {
            XEiJ.mnbNoKeyboardMenuItem.doClick ();
          } else if (kbdType == KBD_COMPACT_TYPE) {
            XEiJ.mnbCompactKeyboardMenuItem.doClick ();
          } else {
            XEiJ.mnbStandardKeyboardMenuItem.doClick ();
          }
          return;
        case KeyEvent.VK_L:  //Alt+Shift-L
          XEiJ.mnbLanguageMenu.doClick ();
          return;
        case KeyEvent.VK_M:  //Alt+Shift-M
          XEiJ.mpuMenu.doClick ();
          return;
        case KeyEvent.VK_O:  //Alt+Shift-O
          XEiJ.mpuOpt1ResetMenuItem.doClick ();
          return;
        case KeyEvent.VK_P:  //Alt+Shift-P
          if (XEiJ.mnbPlayMenuItem.isEnabled ()) {
            XEiJ.mnbPlayMenuItem.doClick ();
          }
          return;
        case KeyEvent.VK_Q:  //Alt+Shift-Q
          if (XEiJ.mnbQuitMenuItem.isEnabled ()) {
            XEiJ.mnbQuitMenuItem.doClick ();
          }
          return;
        case KeyEvent.VK_R:  //Alt+Shift-R
          XEiJ.mpuResetMenuItem.doClick ();
          return;
        case KeyEvent.VK_S:  //Alt+Shift-S
          if (XEiJ.mnbSoundMenu.isEnabled ()) {
            XEiJ.mnbSoundMenu.doClick ();
          }
          return;
        case KeyEvent.VK_T:  //Alt+Shift-T
          if (XEiJ.PNL_STEREOSCOPIC_ON) {
            XEiJ.mnbStereoscopicMenuItem.doClick ();
          }
          return;
        case KeyEvent.VK_V:  //Alt+Shift-V
          if (XEiJ.mnbPasteMenuItem.isEnabled ()) {
            XEiJ.mnbPasteMenuItem.doClick ();
          }
          return;
        case KeyEvent.VK_W:  //Alt+Shift-W
          if (XEiJ.mnbFitInWindowMenuItem.isEnabled ()) {  //ウインドウに合わせられるとき
            XEiJ.mnbFitInWindowMenuItem.doClick ();
          }
          return;
        case KeyEvent.VK_X:  //Alt+Shift-X
          XEiJ.mnbFixedScaleMenuItem.doClick ();
          return;
        }
      }  //if Alt+Shift
    }  //if pressed
    if (PPI.ppiInput (ke, pressed)) {  //JOYKEY
      return;
    }

    if (keyCode == KeyEvent.VK_F11) {  //F11
      if (ButtonFunction.bfnExecute (ButtonFunction.Button.F11, modifiersEx, pressed, null)) {
        return;
      }
    } else if (keyCode == KeyEvent.VK_F12) {  //F12
      if (ButtonFunction.bfnExecute (ButtonFunction.Button.F12, modifiersEx, pressed, null)) {
        return;
      }
    }

    {
      //int keyCode = ke.getKeyLocation ();
      int keyLocation = ke.getKeyLocation ();
      int extendedOrRaw = XEiJ.prgIsMac ? ke.getExtendedKeyCode () : kbdGetRawCode (ke);
      int intCode = keyCode << 16 | extendedOrRaw << 4 | keyLocation;
      if ((keyCode & (XEiJ.prgIsMac ? 0x00000fff : 0x0000ffff)) != keyCode ||
          (extendedOrRaw & (XEiJ.prgIsMac ? 0x0f000fff : 0x00000fff)) != extendedOrRaw ||
          (keyLocation & 0x0000000f) != keyLocation ||
          intCode == 0) {  //範囲外
        System.out.printf ("KeyEvent: keyCode=0x%08x, extendedOrRaw=0x%08x, keyLocation=0x%08x\n",
                           keyCode, extendedOrRaw, keyLocation);
      } else {
        if (Mouse.musOutputButtonStatus) {
          System.out.printf ("intCode=0x%08x, pressed=%b\n", intCode, pressed);
        }
        //  macOSでextendedKeyCodeが0かつどのキーともマッチしなかったとき、
        //  extendedKeyCodeの代わりにkeyCodeを用いて再検索する
        //  参考
        //    https://gist.github.com/iwadon/b861e398e53f05fc4a63f710f9145ee7
        for (int cand = XEiJ.prgIsMac && extendedOrRaw == 0 ? 2 : 1; cand != 0; cand--) {
          for (int xo = 0; xo < KBD_ALL_KEYS; xo++) {
            if (kbdCurrentMap[3 * xo] == intCode ||  //1個目にある
                kbdCurrentMap[3 * xo + 1] == intCode ||  //2個目にある
                kbdCurrentMap[3 * xo + 2] == intCode) {  //3個目にある
              int eo = KBD_XO_TO_EO[xo];
              int stat = kbdStat[eo];
              //押す
              if (pressed) {  //キーが押された
                if ((stat & KBD_KEYDOWN_MASK) == 0) {  //キーで押されていなかった
                  kbdStat[eo] = stat |= KBD_KEYDOWN_MASK;  //キーで押す
                  if ((stat & KBD_PRESSED_MASK) == 0) {  //押されていなかった
                    kbdStat[eo] = stat |= KBD_PRESSED_MASK;  //押す
                    kbdPress (eo);
                    kbdRepaint ();
                  }
                }
              } else {  //キーが離された
                if ((stat & KBD_KEYDOWN_MASK) != 0) {  //キーで押されていた
                  kbdStat[eo] = stat &= ~KBD_KEYDOWN_MASK;  //キーを離す
                  if ((stat & (KBD_LOCKED_MASK | KBD_KEYDOWN_MASK | KBD_MOUSEDOWN_MASK)) == 0) {  //ロックされておらずキーもマウスも押されていない
                    kbdStat[eo] = stat &= ~KBD_PRESSED_MASK;  //離す
                    kbdRelease (eo);
                    kbdRepaint ();
                  }
                }
              }
              return;
            }  //if マッチした
          }  //for xo
          intCode = keyCode << 16 | keyCode << 4 | keyLocation;
        }  //for cand
      }  //if
    }
  }  //kbdInput(KeyEvent,boolean)

  public static void kbdCommandPress (int x68kCode) {
    if ((0x01 <= x68kCode && x68kCode <= 0x6c) ||
        (0x70 <= x68kCode && x68kCode <= 0x74)) {
      int eo = KBD_XO_TO_EO[x68kCode < 0x70 ? x68kCode - 1 : x68kCode - 4];
      int stat = kbdStat[eo];
      if ((stat & KBD_PRESSED_MASK) == 0) {  //押されていなかった
        kbdStat[eo] = stat |= KBD_PRESSED_MASK;  //押す
        kbdPress (eo);
        kbdRepaint ();
      }
    }
  }

  public static void kbdCommandRelease (int x68kCode) {
    if ((0x01 <= x68kCode && x68kCode <= 0x6c) ||
        (0x70 <= x68kCode && x68kCode <= 0x74)) {
      int eo = KBD_XO_TO_EO[x68kCode < 0x70 ? x68kCode - 1 : x68kCode - 4];
      int stat = kbdStat[eo];
      if ((stat & (KBD_LOCKED_MASK | KBD_KEYDOWN_MASK | KBD_MOUSEDOWN_MASK)) == 0) {  //ロックされておらずキーもマウスも押されていない
        kbdStat[eo] = stat &= ~KBD_PRESSED_MASK;  //離す
        kbdRelease (eo);
        kbdRepaint ();
      }
    }
  }

  //rawCode = kbdGetRawCode (ke)
  //  KeyEventからrawCodeを取り出す
  public static int kbdGetRawCode (KeyEvent ke) {
    int rawCode = 0;
    //KeyEvent.paramString()で出力される文字列の中からrawCode=～を取り出す
    String s = ke.paramString ();
    int i = s.indexOf ("rawCode=");
    if (0 <= i) {
      i += 8;
      for (int k = s.length (); i < k; i++) {
        char c = s.charAt (i);
        if (c < '0' || '9' < c) {
          break;
        }
        rawCode = rawCode * 10 + (c - '0');
      }
    }
    return rawCode;
  }

  //kbdRePress()
  //  押されているキーを再入力する
  //  リセット後キー入力割り込みが許可されたら直ちに押されているキーを入力しないとOPT.2キーをロックしていてもROMデバッガが起動しない
  public static void kbdRePress () {
    for (int i = 0; i < KBD_ALL_KEYS; i++) {
      if (i != KBD_NUM_INDEX &&  //NUMキーは除く
          (kbdStat[i] & KBD_PRESSED_MASK) != 0) {  //押されている
        //キーコードを求める
        int code = ((kbdStat[KBD_NUM_INDEX] & KBD_LIGHTED_MASK) != 0 ? KBD_NUM_CODE : KBD_NORMAL_CODE)[i] & 0x7f;
        //MFPに転送する
        if (MC68901.MFP_KBD_ON) {
          MC68901.mfpKbdInput (code, true);
        } else {
          kbdTransfer (code, true);
        }
      }
    }
  }  //kbdRePress()

  //kbdPress (i)
  //  番号iのキーが押された
  public static void kbdPress (int i) {
    //キーコードを求める
    int code = ((kbdStat[KBD_NUM_INDEX] & KBD_LIGHTED_MASK) != 0 ? KBD_NUM_CODE : KBD_NORMAL_CODE)[i] & 0x7f;
    //NUMキーが押されたらLEDを反転する
    if (KBD_DEBUG_LED ? KBD_KANA_INDEX <= i && i <= KBD_NUM_INDEX : i == KBD_NUM_INDEX) {
      int stat = kbdStat[i];
      if ((stat & KBD_LIGHTED_MASK) != 0) {  //点灯→消灯
        stat &= ~(KBD_BRIGHTNESS_MASK | KBD_LIGHTED_MASK);
      } else {  //消灯→点灯
        stat |= kbdLedBrightness << KBD_BRIGHTNESS_BIT | KBD_LIGHTED_MASK;
      }
      kbdStat[i] = stat;
      kbdRepaint ();
    }
    //MFPに転送する
    if (MC68901.MFP_KBD_ON) {
      MC68901.mfpKbdInput (code, true);
    } else {
      kbdTransfer (code, true);
    }
  }  //kbdPress(int)

  //kbdRelease (i)
  //  番号iのキーが離された
  public static void kbdRelease (int i) {
    //キーコードを求める
    int code = ((kbdStat[KBD_NUM_INDEX] & KBD_LIGHTED_MASK) != 0 ? KBD_NUM_CODE : KBD_NORMAL_CODE)[i] & 0x7f | 0x80;
    //MFPに転送する
    if (MC68901.MFP_KBD_ON) {
      MC68901.mfpKbdInput (code, false);
    } else {
      kbdTransfer (code, false);
    }
  }  //kbdRelease(int)

  //kbdTransfer (code, repeat)
  //  キーコードをMFPに転送する
  //  割り込み要求が競合しないようにコアのスレッドで行う
  public static void kbdTransfer (int code, boolean repeat) {
    if (KBD_GUIDE_ON) {
      if (kbdGuideThread != null) {
        kbdGuideThread.interrupt ();
        kbdGuideThread = null;
      }
      XEiJ.tmrTimer.schedule (new GuideTask (code), kbdGuideDate);
      if (repeat) {
        kbdGuideThread = new GuideThread (code);
        kbdGuideThread.start ();
      }
    } else {
      XEiJ.tmrTimer.schedule (new KBDTask (code, repeat), 0L);
    }
  }  //kbdTransfer(int)

  //kbdSetRepeatDelay (n)
  //  リピートディレイ
  //  200+100*n(ms)
  public static void kbdSetRepeatDelay (int n) {
    n &= 15;
    kbdRepeatDelay = 200 + 100 * n;
  }  //kbdSetRepeatDelay(int)

  //kbdSetRepeatInterval (n)
  //  リピートインターバル
  //  30+5*n^2(ms)
  public static void kbdSetRepeatInterval (int n) {
    n &= 15;
    kbdRepeatInterval = 30 + 5 * n * n;
  }  //kbdSetRepeatInterval(int)

  //kbdSetLedStatus (n)
  //  LEDキーのLEDの状態をまとめて設定する。各ビット0=点灯,1=消灯
  //  bit0  かな
  //  bit1  ローマ字
  //  bit2  コード入力
  //  bit3  CAPS
  //  bit4  INS
  //  bit5  ひらがな
  //  bit6  全角
  public static void kbdSetLedStatus (int n) {
    boolean flag = false;
    n = ~n & 127;
    if (KBD_ZKEY_ON) {
      if (kbdZKeyStatus != n) {
        kbdZKeyStatus = n;
        kbdZKeyUpdate ();
      }
    }
    for (int i = KBD_KANA_INDEX; i <= KBD_ZEN_INDEX; i++) {
      int mask = (n & 1) << KBD_LIGHTED_BIT;
      int stat = kbdStat[i];
      if ((stat & KBD_LIGHTED_MASK) != mask) {
        if (mask == 0) {  //点灯→消灯
          stat &= ~(KBD_BRIGHTNESS_MASK | KBD_LIGHTED_MASK);
        } else {  //消灯→点灯
          stat |= kbdLedBrightness << KBD_BRIGHTNESS_BIT | KBD_LIGHTED_MASK;
        }
        kbdStat[i] = stat;
        flag = true;
      }
      n >>= 1;
    }
    if (flag) {
      kbdRepaint ();
    }
  }  //kbdSetLedStatus(int)

  //kbdSetLedBrightness (n)
  //  LEDキーのLEDの明るさを設定する
  //  n  0  明るい
  //     1  やや明るい
  //     2  やや暗い
  //     3  暗い
  public static void kbdSetLedBrightness (int n) {
    n &= 3;
    if (kbdLedBrightness != n) {
      kbdLedBrightness = n;
      boolean flag = false;
      for (int i = KBD_KANA_INDEX; i <= KBD_ZEN_INDEX; i++) {
        int stat = kbdStat[i];
        if ((stat & KBD_LIGHTED_MASK) != 0) {  //点灯→点灯
          stat &= ~(KBD_BRIGHTNESS_MASK | KBD_LIGHTED_MASK);
          stat |= kbdLedBrightness << KBD_BRIGHTNESS_BIT | KBD_LIGHTED_MASK;
          kbdStat[i] = stat;
          flag = true;
        }
      }
      if (flag) {
        kbdRepaint ();
      }
      if (KBD_ZKEY_ON) {
        kbdZKeyUpdate ();
      }
    }
  }

  //kbdZKeyOpen ()
  //  Zキーボードのポートを開く
  public static void kbdZKeyOpen () {
    if (KBD_ZKEY_ON) {
      if (kbdZKeyPort == null) {
        //ポートを開く
        kbdZKeyIsUsb = false;
        if (kbdZKeyMode != 0) {
          try {
            kbdZKeyPort = new ZKeyUsbPort ((code, pressed) -> {
              SwingUtilities.invokeLater (() -> {
                if (kbdZKeyPort == null || !kbdZKeyIsUsb) {
                  return;
                }
                if (pressed) {
                  if (XEiJ.frmFrame != null && XEiJ.frmFrame.isActive ()) {  //フォーカスがあるときだけ押す
                    kbdCommandPress (code);
                  }
                } else {
                  kbdCommandRelease (code);
                }
              });
            }, kbdZKeyDebugFlag);
            kbdZKeyIsUsb = true;
          } catch (IOException ioe) {
            if (kbdZKeyDebugFlag || kbdZKeyMode == 2) {
              System.out.println ("WinUSB: " + ioe.getMessage ());
            }
          }
        }
        if (kbdZKeyPort == null && kbdZKeyMode != 2) {
          try {
            kbdZKeyPort = new ZKeyLEDPort (kbdZKeyDebugFlag);
          } catch (IOException ioe) {
            //ioe.printStackTrace ();
          }
        }
        //開けなかったら終了
        if (kbdZKeyPort == null) {
          System.out.printf (Multilingual.mlnJapanese ?
                             "Z キーボードが接続されていないか制御できません\n" :
                             "Z Keyboard is not connected or cannot be controlled\n");
          kbdZKeyOnRequest = false;
          if (kbdZKeyConnectCheckBox != null) {
            kbdZKeyConnectCheckBox.setSelected (false);
          }
          return;
        }
        //送信スレッドを開始する
        senderStart ();
        if (!kbdZKeyIsUsb) {
        //Zキーボード以外のキーマップを保存する
        System.arraycopy (kbdCurrentMap, 0,  //from
                          kbdCurrentMapNonZ, 0,  //to
                          KBD_DEFAULT_MAP_WIN.length);  //length
        //Zキーボードのキーマップを復元する
        System.arraycopy (kbdCurrentMapZ, 0,  //from
                          kbdCurrentMap, 0,  //to
                          KBD_DEFAULT_MAP_WIN.length);  //length
        //キーマップエディタの表示を更新する
        if (kbdEditor != null) {
          kbdEditor.updateTextAll ();
        }
        //NumLockを保存する
        kbdSavedNumLock = kbdZKeyPort.isKeyToggled (KeyEvent.VK_NUM_LOCK);
        //NumLockを点灯させる
        if (!kbdZKeyPort.isKeyToggled (KeyEvent.VK_NUM_LOCK)) {  //NumLockが消灯している
          kbdIgnoreNumLock = true;  //NumLockを1回無視する
          kbdZKeyPort.hitKey (KeyEvent.VK_NUM_LOCK);  //NumLockを叩いて点灯させる。直ちに発火することに注意
        }
        }  //if !kbdZKeyIsUsb
      }
      if (!kbdZKeyOn) {
        System.out.printf (Multilingual.mlnJapanese ?
                           "Z キーボードの制御を開始しました\n" :
                           "Z keyboard control started\n");
        kbdZKeyOn = true;
        if (kbdZKeyDemoCheckBox != null) {
          kbdZKeyDemoCheckBox.setEnabled (true);
        }
      }
    }
  }

  //kbdZKeyClose ()
  //  Zキーボードのポートを閉じる
  public static void kbdZKeyClose () {
    if (KBD_ZKEY_ON) {
      //デモを終了する
      demoEnd ();
      if (kbdZKeyPort != null) {
        //NumLockを復元する
        if (!kbdZKeyIsUsb && kbdSavedNumLock != kbdZKeyPort.isKeyToggled (KeyEvent.VK_NUM_LOCK)) {  //保存したNumLockと違う
          kbdIgnoreNumLock = true;  //NumLockを1回無視する
          kbdZKeyPort.hitKey (KeyEvent.VK_NUM_LOCK);  //NumLockを叩いて戻す
        }
        //LEDを消灯する
        //senderSend (0L);
        //送信スレッドを終了する
        senderEnd ();
        //LEDを消灯する
        //  senderSendしてすぐsenderEndすると消灯する前に送信スレッドが終了してしまう
        kbdZKeyPort.send (0L);
        //ポートを閉じる
        kbdZKeyPort.close ();
        kbdZKeyPort = null;
        if (kbdZKeyIsUsb) {
          kbdZKeyIsUsb = false;
        } else {
        //Zキーボードのキーマップを保存する
        System.arraycopy (kbdCurrentMap, 0,  //from
                          kbdCurrentMapZ, 0,  //to
                          KBD_DEFAULT_MAP_WIN.length);  //length
        //Zキーボード以外のキーマップを復元する
        System.arraycopy (kbdCurrentMapNonZ, 0,  //from
                          kbdCurrentMap, 0,  //to
                          KBD_DEFAULT_MAP_WIN.length);  //length
        //キーマップエディタの表示を更新する
        if (kbdEditor != null) {
          kbdEditor.updateTextAll ();
        }
        }  //if usb else
      }
      if (kbdZKeyOn) {
        System.out.printf (Multilingual.mlnJapanese ?
                           "Z キーボードの制御を終了しました\n" :
                           "Z keyboard control ended\n");
        kbdZKeyOn = false;
        if (kbdZKeyDemoCheckBox != null) {
          kbdZKeyDemoCheckBox.setEnabled (false);
        }
      }
    }
  }

  //kbdZKeyUpdate ()
  //  ZキーボードのLEDを更新する
  public static void kbdZKeyUpdate () {
    if (KBD_ZKEY_ON && kbdZKeyPort != null &&  //Zキーボード
        !kbdZKeyIOProgrammable &&  //ノーマルモード
        demoThread == null) {  //デモ中ではない
      long data = 0L;
      for (int i = 0; i < 7; i++) {
        if ((kbdZKeyStatus & (1 << i)) != 0) {
          data |= 1L << (8 * i);
        }
      }
      data *= (kbdLedBrightness == 0 ? 255 :
               kbdLedBrightness == 1 ? 128 :
               kbdLedBrightness == 2 ? 64 : 32);
      senderSend (data);
    }
  }



  //kbdZKeyIOReadByte (a)
  //  I/Oポートリードバイト
  public static int kbdZKeyIOReadByte (int a) {
    a &= 15;
    if (a < 8) {
      return (int) (kbdZKeyIOData >>> (a << 3)) & 0xff;
    } else if (a == 8) {
      return kbdZKeyIOProgrammable ? 1 : 0;
    } else {
      return 0xff;
    }
  }

  //kbdZKeyIOReadWord (a)
  //  I/Oポートリードワード
  public static int kbdZKeyIOReadWord (int a) {
    a &= 15;
    if (a < 8) {
      return (int) (kbdZKeyIOData >>> (a << 3)) & 0xffff;
    } else {
      return (kbdZKeyIOReadByte (a) << 8 |
              kbdZKeyIOReadByte (a + 1));
    }
  }

  //kbdZKeyIOReadLong (a)
  //  I/Oポートリードロング
  public static int kbdZKeyIOReadLong (int a) {
    a &= 15;
    if (a < 8) {
      return (int) (kbdZKeyIOData >>> (a << 3));
    } else {
      return (kbdZKeyIOReadByte (a) << 24 |
              kbdZKeyIOReadByte (a + 1) << 16 |
              kbdZKeyIOReadByte (a + 2) << 8 |
              kbdZKeyIOReadByte (a + 3));
    }
  }

  //kbdZKeyIOWriteByte (a, d)
  //  I/Oポートライトバイト
  public static void kbdZKeyIOWriteByte (int a, int d) {
    a &= 15;
    if (a < 8) {
      int bit = a << 3;
      kbdZKeyIOData = (kbdZKeyIOData & ~(0xffL << bit)) | (long) (d & 0xff) << bit;
    } else if (a == 8) {
      boolean programmable = (d & 1) != 0;  //ノーマルモードまたはプログラマブルモード
      if (kbdZKeyIOProgrammable != programmable) {
        kbdZKeyIOProgrammable = programmable;
        if (kbdZKeyIOProgrammable) {  //ノーマルモード→プログラマブルモード
        } else {  //プログラマブルモード→ノーマルモード
          kbdZKeyUpdate ();
        }
      }
      if (kbdZKeyIOProgrammable &&  //プログラマブルモード
          (d & 2) != 0 &&  //送信
          kbdZKeyPort != null &&  //ポートが開いている
          demoThread == null) {  //デモ中ではない
        long data = kbdZKeyIOData & 0x00ffffffffffffffL;
        senderSend (data);
      }
    }
  }

  //kbdZKeyIOWriteWord (a, d)
  //  I/Oポートライトワード
  public static void kbdZKeyIOWriteWord (int a, int d) {
    a &= 15;
    if (a < 8) {
      int bit = a << 3;
      kbdZKeyIOData = (kbdZKeyIOData & ~(0xffffL << bit)) | (long) (d & 0xffff) << bit;
    } else {
      kbdZKeyIOWriteByte (a, d >> 8);
      kbdZKeyIOWriteByte (a + 1, d);
    }
  }

  //kbdZKeyIOWriteLong (a, d)
  //  I/Oポートライトロング
  public static void kbdZKeyIOWriteLong (int a, int d) {
    a &= 15;
    if (a < 8) {
      int bit = a << 3;
      kbdZKeyIOData = (kbdZKeyIOData & ~(0xffffffffL << bit)) | ((long) d & 0xffffffffL) << bit;
    } else {
      kbdZKeyIOWriteByte (a, d >> 24);
      kbdZKeyIOWriteByte (a + 1, d >> 16);
      kbdZKeyIOWriteByte (a + 2, d >> 8);
      kbdZKeyIOWriteByte (a + 3, d);
    }
  }

  //kbdDoCapture ()
  //  キャプチャ実行
  //  カレントディレクトリのcaptureというサブディレクトリの中に1.png,2.png,...を作るだけ
  public static void kbdDoCapture () {
    try {
      File dir = new File ("capture");
      if (dir.exists ()) {  //captureがある
        if (!dir.isDirectory ()) {  //captureがあるがディレクトリでない
          return;
        }
      } else {  //captureがない
        if (!dir.mkdir ()) {  //captureがなくてディレクトリが作れない
          return;
        }
      }
      HashSet<String> nameSet = new HashSet<String> ();
      for (String name : dir.list ()) {
        nameSet.add (name);
      }
      int number = 0;
      for (String key : Settings.SGS_FRAME_KEYS) {
        BufferedImage image = RestorableFrame.rfmCapture (key);
        if (image != null) {  //ウインドウが開いていてキャプチャできた
          String name;
          do {
            number++;
            name = number + ".png";
          } while (!nameSet.add (name));  //セットに追加できるまで繰り返す
          name = "capture/" + name;
          if (XEiJ.saveImage (image, name)) {  //成功
            System.out.println (Multilingual.mlnJapanese ? name + " を更新しました" : name + " was updated");
          }
        }
      }
    } catch (Exception e) {
    }
  }  //kbdDoCapture()



  //$$KBT キー入力タスク
  public static class KBDTask extends TimerTask {
    public int code;
    public boolean repeat;
    public KBDTask (int c, boolean r) {
      code = c;
      repeat = r;
    }  //KBDTask(int,boolean)
    @Override public void run () {
      if (kbdRepeatTask != null) {
        //リピートを終了する
        //  開始と終了は同じスレッドで行わなければならない
        kbdRepeatTask.cancel ();
        kbdRepeatTask = null;
      }
      //MFPに転送する
      MC68901.mfpKeyboardInput (code);
      if (repeat) {
        //リピートを開始する
        XEiJ.tmrTimer.schedule (kbdRepeatTask = new KBDRepeatTask (code), (long) kbdRepeatDelay, (long) kbdRepeatInterval);
      }
    }  //run()
  }  //class KBDTask



  //$$KBR キーリピートタスク
  public static class KBDRepeatTask extends TimerTask {
    public int code;
    public KBDRepeatTask (int c) {
      code = c;
    }  //KBDRepeatTask(int)
    @Override public void run () {
      //MFPに転送する
      MC68901.mfpKeyboardInput (code);
    }  //run()
  }  //class KBDRepeatTask



  public static final boolean KBD_GUIDE_ON = true;  //キー入力にガイドスレッドを使う
  public static Thread kbdGuideThread;
  public static final Date kbdGuideDate = new Date (0L);
  static class GuideThread extends Thread {
    int code;
    public GuideThread (int code) {
      this.code = code;
    }
    @Override public void run () {
      long time = kbdRepeatDelay;
      while (!Thread.interrupted ()) {
        try {
          Thread.sleep (time);
          time = kbdRepeatInterval;
        } catch (InterruptedException ie) {
          break;
        }
        XEiJ.tmrTimer.schedule (new GuideTask (code), kbdGuideDate);
      }
    }
  }
  static class GuideTask extends TimerTask {
    int code;
    public GuideTask (int code) {
      this.code = code;
    }
    @Override public void run () {
      MC68901.mfpKeyboardInput (code);  //MFPに転送する
    }
  }



  //ZキーボードLEDデモ
  static final long DEMO_INTERVAL = 40L;  //動作間隔(ms)
  static final int DEMO_LEVEL = 60;  //明るさの数
  static final int DEMO_BRIGHTEN = 3;  //明るくなる速さ
  static final int DEMO_DIM = -1;  //暗くなる速さ
  static final int DEMO_PROPAGATION = 10;  //明るくなり始めるのが伝わる速さ
  static final int DEMO_LED = 7;  //LEDの数
  static final int DEMO_INVISIBLE = 3;  //折り返す前後にある見えないLEDの数
  static long[] demoMap;  //明るさ→輝度
  static int[] demoBrightness;  //LED→明るさ
  static int[] demoDelta;  //LED→明るさの変化
  static int demoCounter;  //カウンタ
  static Thread demoThread;  //スレッド

  //初期化
  static void demoInit () {
    demoMap = new long[DEMO_LEVEL];
    for (int i = 0; i < DEMO_LEVEL; i++) {
      demoMap[i] = (long) (i * i * 255 / ((DEMO_LEVEL - 1) * (DEMO_LEVEL - 1)));
    }
    demoBrightness = new int[DEMO_LED];
    demoDelta = new int[DEMO_LED];
    demoThread = null;
  }

  //開始
  static void demoStart () {
    if (demoThread == null) {
      Arrays.fill (demoBrightness, 0);
      Arrays.fill (demoDelta, 0);
      demoCounter = 0;
      //スレッドを開始する
      demoThread = new DemoThread ();
      demoThread.start ();
    }
  }

  //終了
  static void demoEnd () {
    if (demoThread != null) {
      //スレッドに割り込む
      demoThread.interrupt ();
      //スレッドが終了するまで待つ
      try {
        demoThread.join (100L);
      } catch (InterruptedException ie) {
      }
      demoThread = null;
    }
    //LEDを復元する
    kbdZKeyUpdate ();
  }

  //デモスレッド
  static class DemoThread extends Thread {
    @Override public void run () {
      //割り込まれるまで繰り返す
      for (;;) {
        //データを作る
        long data = 0L;
        for (int i = 0; i < DEMO_LED; i++) {
          data |= demoMap[demoBrightness[i]] << (i << 3);
        }
        //送信する
        senderSend (data);
        //明るさを変化させる
        for (int i = 0; i < DEMO_LED; i++) {  //LED毎に
          demoBrightness[i] += demoDelta[i];  //明るさを変化させる
          if (0 < demoDelta[i]) {  //明るくした
            if (DEMO_LEVEL - 1 <= demoBrightness[i]) {  //最大になった
              demoBrightness[i] = DEMO_LEVEL - 1;  //最大から
              demoDelta[i] = DEMO_DIM;  //暗くなり始める
            }
          } else if (demoDelta[i] < 0) {  //暗くした
            if (demoBrightness[i] <= 0) {  //最小になった
              demoBrightness[i] = 0;  //最小で
              demoDelta[i] = 0;  //止まる
            }
          }
        }
        //明るくなり始める
        if (demoCounter % DEMO_PROPAGATION == 0) {
          int k = demoCounter / DEMO_PROPAGATION;
          if (DEMO_INVISIBLE * 2 + DEMO_LED <= k) {  //後半
            k = (DEMO_INVISIBLE * 2 + DEMO_LED) * 2 - 1 - k;  //折り返す
          }
          if (DEMO_INVISIBLE <= k && k < DEMO_INVISIBLE + DEMO_LED) {
            demoDelta[k - DEMO_INVISIBLE] = DEMO_BRIGHTEN;  //明るくなり始める
          }
        }
        //カウンタを進める
        demoCounter++;
        if (demoCounter == DEMO_PROPAGATION * (DEMO_INVISIBLE * 2 + DEMO_LED) * 2) {
          demoCounter = 0;
        }
        //間隔を空ける
        try {
          Thread.sleep (DEMO_INTERVAL);
        } catch (InterruptedException ie) {
          break;
        }
      }
    }
  }



  //ZキーボードLEDデータ送信
  //  送信の間隔を保つ
  static final long SENDER_INTERVAL = 10L;  //送信の間隔(ms)
  static long senderLastData;  //最後に送信しようとしたデータ
  static long senderSentData;  //最後に送信したデータ
  static ArrayBlockingQueue<Long> senderQueue;  //送信するデータのキュー
  static Thread senderThread;  //スレッド

  //開始
  static void senderStart () {
    if (senderThread == null) {
      if (senderQueue == null) {
        senderLastData = -1L;
        senderSentData = -1L;
        //キューを作る
        senderQueue = new ArrayBlockingQueue<Long> (10);
      }
      //スレッドを開始する
      senderThread = new SenderThread ();
      senderThread.start ();
    }
  }

  //終了
  static void senderEnd () {
    if (senderThread != null) {
      //スレッドに割り込む
      senderThread.interrupt ();
      //スレッドが終了するまで待つ
      try {
        senderThread.join (100L);
      } catch (InterruptedException ie) {
      }
      senderThread = null;
    }
  }

  //送信
  static void senderSend (long data) {
    if (senderLastData != data) {
      senderLastData = data;
      //キューに古いデータが残っていたら消す
      senderQueue.clear ();
      //キューに新しいデータを入れる
      senderQueue.offer (data);
    }
  }

  //送信スレッド
  static class SenderThread extends Thread {
    @Override public void run () {
      //割り込まれるまで繰り返す
      for (;;) {
        long data;
        //キューからデータを取り出す。取り出せるまでブロックする
        try {
          data = senderQueue.take ();
        } catch (InterruptedException ie) {
          break;
        }
        //送信する
        if (senderSentData != data) {
          senderSentData = data;
          kbdZKeyPort.send (data);
        }
        //間隔を空ける
        try {
          Thread.sleep (SENDER_INTERVAL);
        } catch (InterruptedException ie) {
          break;
        }
      }
    }
  }



}  //class Keyboard



