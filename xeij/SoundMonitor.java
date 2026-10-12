//========================================================================================
//  SoundMonitor.java
//    en:Sound monitor
//    ja:音声モニタ
//  Copyright (C) 2003-2024 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.awt.*;  //BasicStroke,BorderLayout,BoxLayout,Color,Component,Container,Cursor,Desktop,Dimension,Font,FlowLayout,Frame,Graphics,Graphics2D,GraphicsDevice,GraphicsEnvironment,GridLayout,Image,Insets,Paint,Point,Rectangle,RenderingHints,Robot,Shape,Stroke,TexturePaint,Toolkit
import java.awt.event.*;  //ActionEvent,ActionListener,ComponentAdapter,ComponentEvent,ComponentListener,FocusAdapter,FocusEvent,FocusListener,InputEvent,KeyAdapter,KeyEvent,KeyListener,MouseAdapter,MouseEvent,MouseListener,MouseMotionAdapter,MouseWheelEvent,WindowAdapter,WindowEvent,WindowListener,WindowStateListener
import java.awt.image.*;  //BufferedImage,DataBuffer,DataBufferByte,DataBufferInt,IndexColorModel
import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,Timer,TimerTask,TreeMap
import javax.swing.*;  //AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JApplet,JButton,JCheckBox,JCheckBoxMenuItem,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException

public class SoundMonitor {

  //パネル
  public static final int SMN_WIDTH = 4 + 8 + 4 * (14 * 8 + 6) + 4;  //鍵盤の幅に合わせる。4の倍数に限る。488
  public static final int SMN_OFFSET = SMN_WIDTH + 3 >> 2;

  //色
  public static final byte SMN_BLACK  = (byte) 0b00000000;  //  0  0xff000000  黒  背景
  public static final byte SMN_BLUE   = (byte) 0b01010101;  //  1  0xff2020ff  青  文字,黒鍵
  public static final byte SMN_ORANGE = (byte) 0b10101010;  //  2  0xffff8740  橙  ハイライト
  public static final byte SMN_WHITE  = (byte) 0b11111111;  //  3  0xffffffff  白  文字,白鍵

  //フォント
  public static final byte[] SMN_FONT_1 = new byte[5 * 127];
  public static final byte[] SMN_FONT_2 = new byte[5 * 127];
  public static final byte[] SMN_FONT_3 = new byte[5 * 127];

  //アイコン
/*
  public static final int[] SMN_ICON_1 = {
    0b01010101,0b01010100,  //0:[1]
    0b01000000,0b00000100,
    0b01000001,0b00000100,
    0b01000001,0b00000100,
    0b01000001,0b00000100,
    0b01000001,0b00000100,
    0b01000001,0b00000100,
    0b01000000,0b00000100,
    0b01010101,0b01010100,
    0b01010101,0b01010100,  //1:[2]
    0b01000000,0b00000100,
    0b01000101,0b01000100,
    0b01000000,0b01000100,
    0b01000101,0b01000100,
    0b01000100,0b00000100,
    0b01000101,0b01000100,
    0b01000000,0b00000100,
    0b01010101,0b01010100,
    0b01010101,0b01010100,  //2:[3]
    0b01000000,0b00000100,
    0b01000101,0b01000100,
    0b01000000,0b01000100,
    0b01000101,0b01000100,
    0b01000000,0b01000100,
    0b01000101,0b01000100,
    0b01000000,0b00000100,
    0b01010101,0b01010100,
    0b01010101,0b01010100,  //3:[4]
    0b01000000,0b00000100,
    0b01000100,0b01000100,
    0b01000100,0b01000100,
    0b01000101,0b01000100,
    0b01000000,0b01000100,
    0b01000000,0b01000100,
    0b01000000,0b00000100,
    0b01010101,0b01010100,
    0b01010101,0b01010100,  //4:[5]
    0b01000000,0b00000100,
    0b01000101,0b01000100,
    0b01000100,0b00000100,
    0b01000101,0b01000100,
    0b01000000,0b01000100,
    0b01000101,0b01000100,
    0b01000000,0b00000100,
    0b01010101,0b01010100,
    0b01010101,0b01010100,  //5:[6]
    0b01000000,0b00000100,
    0b01000101,0b01000100,
    0b01000100,0b00000100,
    0b01000101,0b01000100,
    0b01000100,0b01000100,
    0b01000101,0b01000100,
    0b01000000,0b00000100,
    0b01010101,0b01010100,
    0b01010101,0b01010100,  //6:[7]
    0b01000000,0b00000100,
    0b01000101,0b01000100,
    0b01000000,0b01000100,
    0b01000000,0b01000100,
    0b01000000,0b01000100,
    0b01000000,0b01000100,
    0b01000000,0b00000100,
    0b01010101,0b01010100,
    0b01010101,0b01010100,  //7:[8]
    0b01000000,0b00000100,
    0b01000101,0b01000100,
    0b01000100,0b01000100,
    0b01000101,0b01000100,
    0b01000100,0b01000100,
    0b01000101,0b01000100,
    0b01000000,0b00000100,
    0b01010101,0b01010100,
    0b01010101,0b01010100,  //8:[W]
    0b01000000,0b00000100,
    0b01000100,0b01000100,
    0b01000100,0b01000100,
    0b01000101,0b01000100,
    0b01000101,0b01000100,
    0b01000100,0b01000100,
    0b01000000,0b00000100,
    0b01010101,0b01010100,
    0b01010101,0b01010100,  //9:[S]
    0b01000000,0b00000100,
    0b01000001,0b01000100,
    0b01000100,0b00000100,
    0b01000001,0b00000100,
    0b01000000,0b01000100,
    0b01000101,0b00000100,
    0b01000000,0b00000100,
    0b01010101,0b01010100,
    0b01010101,0b01010100,  //10:[P]
    0b01000000,0b00000100,
    0b01000101,0b00000100,
    0b01000100,0b01000100,
    0b01000101,0b00000100,
    0b01000100,0b00000100,
    0b01000100,0b00000100,
    0b01000000,0b00000100,
    0b01010101,0b01010100,
  };
*/
  //  perl misc/itob.pl xeij/SoundMonitor.java SMN_ICON_1
  public static final byte[] SMN_ICON_1 = "UT@\4A\4A\4A\4A\4A\4@\4UTUT@\4ED@DEDD\4ED@\4UTUT@\4ED@DED@DED@\4UTUT@\4DDDDED@D@D@\4UTUT@\4EDD\4ED@DED@\4UTUT@\4EDD\4EDDDED@\4UTUT@\4ED@D@D@D@D@\4UTUT@\4EDDDEDDDED@\4UTUT@\4DDDDEDEDDD@\4UTUT@\4ADD\4A\4@DE\4@\4UTUT@\4E\4DDE\4D\4D\4@\4UT".getBytes (XEiJ.ISO_8859_1);
/*
  public static final int[] SMN_ICON_3 = {
    0b11111111,0b11111100,  //0:[1]
    0b11000000,0b00001100,
    0b11000011,0b00001100,
    0b11000011,0b00001100,
    0b11000011,0b00001100,
    0b11000011,0b00001100,
    0b11000011,0b00001100,
    0b11000000,0b00001100,
    0b11111111,0b11111100,
    0b11111111,0b11111100,  //1:[2]
    0b11000000,0b00001100,
    0b11001111,0b11001100,
    0b11000000,0b11001100,
    0b11001111,0b11001100,
    0b11001100,0b00001100,
    0b11001111,0b11001100,
    0b11000000,0b00001100,
    0b11111111,0b11111100,
    0b11111111,0b11111100,  //2:[3]
    0b11000000,0b00001100,
    0b11001111,0b11001100,
    0b11000000,0b11001100,
    0b11001111,0b11001100,
    0b11000000,0b11001100,
    0b11001111,0b11001100,
    0b11000000,0b00001100,
    0b11111111,0b11111100,
    0b11111111,0b11111100,  //3:[4]
    0b11000000,0b00001100,
    0b11001100,0b11001100,
    0b11001100,0b11001100,
    0b11001111,0b11001100,
    0b11000000,0b11001100,
    0b11000000,0b11001100,
    0b11000000,0b00001100,
    0b11111111,0b11111100,
    0b11111111,0b11111100,  //4:[5]
    0b11000000,0b00001100,
    0b11001111,0b11001100,
    0b11001100,0b00001100,
    0b11001111,0b11001100,
    0b11000000,0b11001100,
    0b11001111,0b11001100,
    0b11000000,0b00001100,
    0b11111111,0b11111100,
    0b11111111,0b11111100,  //5:[6]
    0b11000000,0b00001100,
    0b11001111,0b11001100,
    0b11001100,0b00001100,
    0b11001111,0b11001100,
    0b11001100,0b11001100,
    0b11001111,0b11001100,
    0b11000000,0b00001100,
    0b11111111,0b11111100,
    0b11111111,0b11111100,  //6:[7]
    0b11000000,0b00001100,
    0b11001111,0b11001100,
    0b11000000,0b11001100,
    0b11000000,0b11001100,
    0b11000000,0b11001100,
    0b11000000,0b11001100,
    0b11000000,0b00001100,
    0b11111111,0b11111100,
    0b11111111,0b11111100,  //7:[8]
    0b11000000,0b00001100,
    0b11001111,0b11001100,
    0b11001100,0b11001100,
    0b11001111,0b11001100,
    0b11001100,0b11001100,
    0b11001111,0b11001100,
    0b11000000,0b00001100,
    0b11111111,0b11111100,
    0b11111111,0b11111100,  //8:[W]
    0b11000000,0b00001100,
    0b11001100,0b11001100,
    0b11001100,0b11001100,
    0b11001111,0b11001100,
    0b11001111,0b11001100,
    0b11001100,0b11001100,
    0b11000000,0b00001100,
    0b11111111,0b11111100,
    0b11111111,0b11111100,  //9:[S]
    0b11000000,0b00001100,
    0b11000011,0b11001100,
    0b11001100,0b00001100,
    0b11000011,0b00001100,
    0b11000000,0b11001100,
    0b11001111,0b00001100,
    0b11000000,0b00001100,
    0b11111111,0b11111100,
    0b11111111,0b11111100,  //10:[P]
    0b11000000,0b00001100,
    0b11001111,0b00001100,
    0b11001100,0b11001100,
    0b11001111,0b00001100,
    0b11001100,0b00001100,
    0b11001100,0b00001100,
    0b11000000,0b00001100,
    0b11111111,0b11111100,
  };
*/
  //  perl misc/itob.pl xeij/SoundMonitor.java SMN_ICON_3
  public static final byte[] SMN_ICON_3 = "\377\374\300\f\303\f\303\f\303\f\303\f\303\f\300\f\377\374\377\374\300\f\317\314\300\314\317\314\314\f\317\314\300\f\377\374\377\374\300\f\317\314\300\314\317\314\300\314\317\314\300\f\377\374\377\374\300\f\314\314\314\314\317\314\300\314\300\314\300\f\377\374\377\374\300\f\317\314\314\f\317\314\300\314\317\314\300\f\377\374\377\374\300\f\317\314\314\f\317\314\314\314\317\314\300\f\377\374\377\374\300\f\317\314\300\314\300\314\300\314\300\314\300\f\377\374\377\374\300\f\317\314\314\314\317\314\314\314\317\314\300\f\377\374\377\374\300\f\314\314\314\314\317\314\317\314\314\314\300\f\377\374\377\374\300\f\303\314\314\f\303\f\300\314\317\f\300\f\377\374\377\374\300\f\317\f\314\314\317\f\314\f\314\f\300\f\377\374".getBytes (XEiJ.ISO_8859_1);

  //波形
  public static final int SMN_WAVE_X = 12;  //4の倍数に限る
  public static final int SMN_WAVE_Y = 4;
  public static final int SMN_WAVE_WIDTH = SMN_WIDTH - 4 - SMN_WAVE_X;
  public static final int SMN_WAVE_VALUE_SHIFT = 11;  //振幅の縮小率
  public static final int SMN_WAVE_HEIGHT = (1 << 16 - SMN_WAVE_VALUE_SHIFT) + 1;  //49
  public static final int SMN_WAVE_SCALE_X_MAX = 5;
  public static final int SMN_WAVE_SCALE_Y_MAX = 3;
  public static final int[] smnOPMBuffer = new int[SoundSource.SND_CHANNELS * OPM.OPM_BLOCK_SAMPLES];
  public static final int[] smnPCMBuffer = new int[SoundSource.SND_CHANNELS * OPM.OPM_BLOCK_SAMPLES];
  public static final int[][] smnWaveIndex0 = new int[SMN_WAVE_SCALE_X_MAX + 1][];
  public static final int[][] smnWaveIndex1 = new int[SMN_WAVE_SCALE_X_MAX + 1][];
  public static int smnWaveScaleX;
  public static int smnWaveScaleY;
  public static int smnWaveOffsetMax;
  public static int smnWaveOffset;
  public static int smnWaveElevation;
  public static boolean smnWaveDragOn;
  public static int smnWavePressedX;
  public static int smnWavePressedY;
  public static final int[] smnWaveLastYPCMLeft = new int[2];
  public static final int[] smnWaveLastYPCMRight = new int[2];
  public static final int[] smnWaveLastYOPMLeft = new int[2];
  public static final int[] smnWaveLastYOPMRight = new int[2];

  //スペクトラムアナライザ
  //  OPM.OPM_SAMPLE_FREQ == 62500 && OPM.OPM_BLOCK_SAMPLES == 2500 && SMN_WIDTH == 480 に固定
  public static final int SMN_SPECTRUM_X = 4;  //4の倍数に限る
  public static final int SMN_SPECTRUM_Y = SMN_WAVE_Y + SMN_WAVE_HEIGHT + 4;
  public static final int SMN_SPECTRUM_WIDTH = 480;  //SMN_WIDTH-4-SMN_SPECTRUM_X
  public static final int SMN_SPECTRUM_HEIGHT = 32;
/*
  public static final int[] SMN_SPECTRUM_MAP = {
    //       C           C#               D              D#               E                               F
    0, 1, 2, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25, 27, 29, 31, 33, 35, 36, 37, 38, 39, 40, 41, 42, 43,
    //          F#               G              G#               A              A#               B
    45, 47, 49, 51, 53, 55, 57, 59, 61, 63, 65, 67, 69, 71, 73, 75, 77, 79, 81, 83, 85, 87, 89, 91, 92, 93, 94, 95,
  };
*/
  //  perl misc/itob.pl xeij/SoundMonitor.java SMN_SPECTRUM_MAP
  public static final byte[] SMN_SPECTRUM_MAP = "\0\1\2\3\5\7\t\13\r\17\21\23\25\27\31\33\35\37!#$%&\'()*+-/13579;=?ACEGIKMOQSUWY[\\]^_".getBytes (XEiJ.ISO_8859_1);
  public static final int SMN_SPECTRUM_BIT = 10;  //FFTのサンプル数のbit数
  public static final int SMN_SPECTRUM_N = 1 << SMN_SPECTRUM_BIT;  //FFTのサンプル数
  public static final int SMN_SPECTRUM_PARTITIONS = 6;  //分割数
  public static final int SMN_SPECTRUM_RANGE = 480 / SMN_SPECTRUM_PARTITIONS;  //分割幅。120
  public static final int[] SMN_SPECTRUM_INTERVAL = new int[] { 100, 100, 50, 25, 10, 5 };  //サンプリング間隔
  public static final double[][] smnSpectrumBuffer = new double[SMN_SPECTRUM_PARTITIONS][];  //バッファ。[1]は使わない
  public static final double[] smnSpectrumX = new double[SMN_SPECTRUM_N];  //実数部
  public static final double[] smnSpectrumY = new double[SMN_SPECTRUM_N];  //虚数部
  public static final int[] smnSpectrumIndex = new int[480];  //幅<<16|インデックス
  public static final int[] smnSpectrumValue = new int[480];  //前回の値
  public static final double[] smnSpectrumWindow = new double[SMN_SPECTRUM_N];  //窓関数
  public static final int SMN_SPECTRUM_TRAIL = 2;  //一度に減らす数の上限。小さくするとゆっくり下がる。小さくしすぎると最後に伸びた線がどれかわからなくなる
  public static final double SMN_SPECTRUM_SCALE = 64.0;  //拡大率
  public static FFT smnFFT;  //FFT

  //440Hzマーク
  public static final int[] SMN_440HZ_PATTERN = { 18, SMN_BLUE, 0, 0, -1, 1, 1, 1, -2, 2, 2, 2, -1, 3, 1, 3, 0, 4 };

  //鍵盤
  public static final int SMN_KEY_X = 12;
  public static final int SMN_KEY_Y = SMN_SPECTRUM_Y + SMN_SPECTRUM_HEIGHT + 1;
  public static final int SMN_KEY_HEIGHT = 6 + 12 * 8;
  public static final byte[] SMN_KEY_COLOR = {
    SMN_WHITE,  //C
    SMN_BLUE,   //C#
    SMN_WHITE,  //D
    SMN_BLUE,   //D#
    SMN_WHITE,  //E
    SMN_WHITE,  //F
    SMN_BLUE,   //F#
    SMN_WHITE,  //G
    SMN_BLUE,   //G#
    SMN_WHITE,  //A
    SMN_BLUE,   //A#
    SMN_WHITE,  //B
  };
  public static final int[] smnKey = new int[8];  //キーオンされているキー。-1=なし,0=O0C

  //音色
  public static final int SMN_TONE_X = 20;  //4の倍数に限る
  public static final int SMN_TONE_Y = SMN_KEY_Y + SMN_KEY_HEIGHT + 4;
  public static final int SMN_TONE_BOX_COLS = 3 * 12 + 2;
  public static final int SMN_TONE_BOX_HEIGHT = 6 * 7 + 4;
  public static final int SMN_TONE_HEIGHT = SMN_TONE_BOX_HEIGHT * 3 - 4;
  public static final char[] SMN_TONE_H1 = "FC SL WA SY SP PD AD PS AS PN"   .toCharArray ();
  public static final char[] SMN_TONE_V0 = "00 00 00 00 00 00 00 00 00 00 00".toCharArray ();
  public static final char[] SMN_TONE_H2 = "AR 1R 2R RR 1L TL KS ML T1 T2 AE".toCharArray ();
  public static final char[] SMN_TONE_M1 = "M1".toCharArray ();
  public static final char[] SMN_TONE_V1 = "00 00 00 00 00 00 00 00 00 00 00".toCharArray ();
  public static final char[] SMN_TONE_C1 = "C1".toCharArray ();
  public static final char[] SMN_TONE_V2 = "00 00 00 00 00 00 00 00 00 00 00".toCharArray ();
  public static final char[] SMN_TONE_M2 = "M2".toCharArray ();
  public static final char[] SMN_TONE_V3 = "00 00 00 00 00 00 00 00 00 00 00".toCharArray ();
  public static final char[] SMN_TONE_C2 = "C2".toCharArray ();
  public static final char[] SMN_TONE_V4 = "00 00 00 00 00 00 00 00 00 00 00".toCharArray ();

  //PCM
  public static final int SMN_PCM_COL = SMN_TONE_X / 4 + SMN_TONE_BOX_COLS * 2 + 6;
  public static final int SMN_PCM_Y = SMN_TONE_Y + SMN_TONE_BOX_HEIGHT * 2;
  //            11111111112222
  //  012345678901234567890123
  //  II 16MHz 1/512  31.3kHz
  //     ^^      ^^^^ ^^^^
  //  II PLAY DATA LEFT RIGHT
  //     ^^^^ ^^^^ ^^^^ ^^^^^
  public static final char[][] SMN_PCM_OSCILLATOR = {  //[ADPCM.pcmOSCFreqMode<<1|ADPCM.pcmOscillator]
    " 8".toCharArray (),
    " 4".toCharArray (),
    " 8".toCharArray (),
    "16".toCharArray (),
  };
  public static final char[][] SMN_PCM_DIVIDER = {  //[ADPCM.pcmDivider]
    "1024".toCharArray (),
    "768 ".toCharArray (),
    "512 ".toCharArray (),
    "768 ".toCharArray (),
  };
  public static final char[][] SMN_PCM_FREQ = {  //[ADPCM.pcmOSCFreqMode<<3|ADPCM.pcmOscillator<<2|ADPCM.pcmDivider]
    " 7.8".toCharArray (),
    "10.4".toCharArray (),
    "15.6".toCharArray (),
    "10.4".toCharArray (),
    " 3.9".toCharArray (),
    " 5.2".toCharArray (),
    " 7.8".toCharArray (),
    " 5.2".toCharArray (),
    " 7.8".toCharArray (),
    "10.4".toCharArray (),
    "15.6".toCharArray (),
    "10.4".toCharArray (),
    "15.6".toCharArray (),
    "20.8".toCharArray (),
    "31.3".toCharArray (),
    "20.8".toCharArray (),
  };
  public static final char[][] SMN_PCM_PLAY = {  //[ADPCM.pcmActive?1:0]
    "    ".toCharArray (),
    "PLAY".toCharArray (),
  };
  public static final char[][] SMN_PCM_DATA = {  //[ADPCM.pcmEncodedData>=0?1:0]
    "    ".toCharArray (),
    "DATA".toCharArray (),
  };
  public static final char[][] SMN_PCM_LEFT = {  //[ADPCM.pcmPanLeft==0||ADPCM.pcmPanLeft<0x80000000+ADPCM.PCM_ATTACK_SPAN*2?0:1]
    "    ".toCharArray (),
    "LEFT".toCharArray (),
  };
  public static final char[][] SMN_PCM_RIGHT = {  //[SoundSource.SND_CHANNELS==1?ADPCM.pcmPanLeft==0||ADPCM.pcmPanLeft<0x80000000+ADPCM.PCM_ATTACK_SPAN*2?0:1:ADPCM.pcmPanRight==0||ADPCM.pcmPanRight<0x80000000+ADPCM.PCM_ATTACK_SPAN*2?0:1]
    "     ".toCharArray (),
    "RIGHT".toCharArray (),
  };

  //パレット
  public static final int SMN_PALET_X = SMN_TONE_X + 4 * SMN_TONE_BOX_COLS * 2;
  public static final int SMN_PALET_Y = SMN_TONE_Y + SMN_TONE_BOX_HEIGHT * 2 + 6 * 2;
  public static final int SMN_PALET_WIDTH = 140;
  public static final int SMN_PALET_HEIGHT = 6 * 5;
  public static final int[][] SMN_SLIDER_ARRAY = {
    { SMN_PALET_X + 4 *  2, SMN_PALET_Y + 6 * 1, 48, SMN_WHITE ,  0 },  //0 H  0
    { SMN_PALET_X + 4 * 16, SMN_PALET_Y + 6 * 1, 32, SMN_WHITE ,  0 },  //0 S  0
    { SMN_PALET_X + 4 * 26, SMN_PALET_Y + 6 * 1, 32, SMN_WHITE ,  0 },  //0 B  0
    { SMN_PALET_X + 4 *  2, SMN_PALET_Y + 6 * 2, 48, SMN_BLUE  , 30 },  //1 H 32
    { SMN_PALET_X + 4 * 16, SMN_PALET_Y + 6 * 2, 32, SMN_BLUE  , 30 },  //1 S 28
    { SMN_PALET_X + 4 * 26, SMN_PALET_Y + 6 * 2, 32, SMN_BLUE  , 32 },  //1 B 32
    { SMN_PALET_X + 4 *  2, SMN_PALET_Y + 6 * 3, 48, SMN_ORANGE,  3 },  //2 H  3
    { SMN_PALET_X + 4 * 16, SMN_PALET_Y + 6 * 3, 32, SMN_ORANGE, 30 },  //2 S 24
    { SMN_PALET_X + 4 * 26, SMN_PALET_Y + 6 * 3, 32, SMN_ORANGE, 32 },  //2 B 32
    { SMN_PALET_X + 4 *  2, SMN_PALET_Y + 6 * 4, 48, SMN_WHITE , 30 },  //3 H  0
    { SMN_PALET_X + 4 * 16, SMN_PALET_Y + 6 * 4, 32, SMN_WHITE ,  3 },  //3 S  0
    { SMN_PALET_X + 4 * 26, SMN_PALET_Y + 6 * 4, 32, SMN_WHITE , 30 },  //3 B 32
  };
  public static final int[][] SMN_SLIDER_PATTERN = {
    { 18, SMN_WHITE ,   1, -2, 2, -2,   0, -1,               3, -1,                 0, 1,             3, 1,   1, 2, 2, 2,
      14, SMN_BLACK ,                          1, -1, 2, -1,          1, 0, 2, 0,         1, 1, 2, 1                     },
    { 26, SMN_BLUE  ,   1, -2, 2, -2,   0, -1, 1, -1, 2, -1, 3, -1,                 0, 1, 1, 1, 2, 1, 3, 1,   1, 2, 2, 2 },
    { 26, SMN_ORANGE,   1, -2, 2, -2,   0, -1, 1, -1, 2, -1, 3, -1,                 0, 1, 1, 1, 2, 1, 3, 1,   1, 2, 2, 2 },
    { 26, SMN_WHITE ,   1, -2, 2, -2,   0, -1, 1, -1, 2, -1, 3, -1,                 0, 1, 1, 1, 2, 1, 3, 1,   1, 2, 2, 2 },
  };
  public static int smnPaletDragSlider;
  public static int smnPaletPressedX;
  public static IndexColorModel smnColorModel;

  //一時停止
  public static final int SMN_PAUSE_WIDTH = 16;
  public static final int SMN_PAUSE_HEIGHT = 16;
  public static final int SMN_PAUSE_X = SMN_WIDTH - 4 - SMN_PAUSE_WIDTH;
  public static final int SMN_PAUSE_Y = SMN_TONE_Y + SMN_TONE_BOX_HEIGHT * 2 + 6 * 3 + (6 * 4 - SMN_PAUSE_HEIGHT >> 1);
/*
  public static final int[] SMN_PAUSE_ICON_3 = {
    0b00000000,0b00000000,0b00000000,0b00000000,
    0b00111111,0b11111111,0b11111111,0b11111100,
    0b00110000,0b00000000,0b00000000,0b00001100,
    0b00110000,0b00000000,0b00000000,0b00001100,
    0b00110000,0b11111111,0b11111111,0b00001100,
    0b00110000,0b11111111,0b11111111,0b00001100,
    0b00110000,0b11111111,0b11111111,0b00001100,
    0b00110000,0b11111111,0b11111111,0b00001100,
    0b00110000,0b11111111,0b11111111,0b00001100,
    0b00110000,0b11111111,0b11111111,0b00001100,
    0b00110000,0b11111111,0b11111111,0b00001100,
    0b00110000,0b11111111,0b11111111,0b00001100,
    0b00110000,0b00000000,0b00000000,0b00001100,
    0b00110000,0b00000000,0b00000000,0b00001100,
    0b00111111,0b11111111,0b11111111,0b11111100,
    0b00000000,0b00000000,0b00000000,0b00000000,
  };
*/
  //  perl misc/itob.pl xeij/SoundMonitor.java SMN_PAUSE_ICON_3
  public static final byte[] SMN_PAUSE_ICON_3 = "\0\0\0\0?\377\377\3740\0\0\f0\0\0\f0\377\377\f0\377\377\f0\377\377\f0\377\377\f0\377\377\f0\377\377\f0\377\377\f0\377\377\f0\0\0\f0\0\0\f?\377\377\374\0\0\0\0".getBytes (XEiJ.ISO_8859_1);
/*
  public static final int[] SMN_PLAY_ICON_3 = {
    0b00000000,0b00000000,0b00000000,0b00000000,
    0b00111111,0b11111111,0b11111111,0b11111100,
    0b00110000,0b00000000,0b00000000,0b00001100,
    0b00110000,0b00000000,0b00000000,0b00001100,
    0b00110000,0b11110000,0b00000000,0b00001100,
    0b00110000,0b11111111,0b00000000,0b00001100,
    0b00110000,0b11111111,0b11110000,0b00001100,
    0b00110000,0b11111111,0b11111111,0b00001100,
    0b00110000,0b11111111,0b11111111,0b00001100,
    0b00110000,0b11111111,0b11110000,0b00001100,
    0b00110000,0b11111111,0b00000000,0b00001100,
    0b00110000,0b11110000,0b00000000,0b00001100,
    0b00110000,0b00000000,0b00000000,0b00001100,
    0b00110000,0b00000000,0b00000000,0b00001100,
    0b00111111,0b11111111,0b11111111,0b11111100,
    0b00000000,0b00000000,0b00000000,0b00000000,
  };
*/
  //  perl misc/itob.pl xeij/SoundMonitor.java SMN_PLAY_ICON_3
  public static final byte[] SMN_PLAY_ICON_3 = "\0\0\0\0?\377\377\3740\0\0\f0\0\0\f0\360\0\f0\377\0\f0\377\360\f0\377\377\f0\377\377\f0\377\360\f0\377\0\f0\360\0\f0\0\0\f0\0\0\f?\377\377\374\0\0\0\0".getBytes (XEiJ.ISO_8859_1);
  public static boolean smnPauseRequest;  //次の更新でsmnPauseUpdateに設定する値
  public static boolean smnPauseUpdate;  //true=更新を一時停止している

  //ズーム
  public static final int SMN_ZOOM_WIDTH = 16;
  public static final int SMN_ZOOM_HEIGHT = 16;
  public static final int SMN_ZOOM_X = SMN_PAUSE_X;
  public static final int SMN_ZOOM_Y = SMN_PAUSE_Y - 24;
/*
  public static final int[] SMN_ZOOM_X1_ICON = {
    0b00000000,0b00000000,0b00000000,0b00000000,
    0b00111111,0b11111111,0b11111111,0b11111100,
    0b00110000,0b00000000,0b00000000,0b00001100,
    0b00110000,0b00000000,0b00000000,0b00001100,
    0b00110000,0b00000000,0b00001111,0b00001100,
    0b00110000,0b00000000,0b00001111,0b00001100,
    0b00110000,0b00000000,0b00001111,0b00001100,
    0b00110011,0b11001111,0b00001111,0b00001100,
    0b00110011,0b11001111,0b00001111,0b00001100,
    0b00110000,0b11111100,0b00001111,0b00001100,
    0b00110011,0b11001111,0b00001111,0b00001100,
    0b00110011,0b11001111,0b00001111,0b00001100,
    0b00110000,0b00000000,0b00000000,0b00001100,
    0b00110000,0b00000000,0b00000000,0b00001100,
    0b00111111,0b11111111,0b11111111,0b11111100,
    0b00000000,0b00000000,0b00000000,0b00000000,
  };
*/
  //  perl misc/itob.pl xeij/SoundMonitor.java SMN_ZOOM_X1_ICON
  public static final byte[] SMN_ZOOM_X1_ICON = "\0\0\0\0?\377\377\3740\0\0\f0\0\0\f0\0\17\f0\0\17\f0\0\17\f3\317\17\f3\317\17\f0\374\17\f3\317\17\f3\317\17\f0\0\0\f0\0\0\f?\377\377\374\0\0\0\0".getBytes (XEiJ.ISO_8859_1);
/*
  public static final int[] SMN_ZOOM_X2_ICON = {
    0b00000000,0b00000000,0b00000000,0b00000000,
    0b00111111,0b11111111,0b11111111,0b11111100,
    0b00110000,0b00000000,0b00000000,0b00001100,
    0b00110000,0b00000000,0b00000000,0b00001100,
    0b00110000,0b00000000,0b00111111,0b00001100,
    0b00110000,0b00000000,0b00000011,0b11001100,
    0b00110000,0b00000000,0b00000011,0b11001100,
    0b00110011,0b11001111,0b00000011,0b11001100,
    0b00110011,0b11001111,0b00001111,0b00001100,
    0b00110000,0b11111100,0b00111100,0b00001100,
    0b00110011,0b11001111,0b00111100,0b00001100,
    0b00110011,0b11001111,0b00111111,0b11001100,
    0b00110000,0b00000000,0b00000000,0b00001100,
    0b00110000,0b00000000,0b00000000,0b00001100,
    0b00111111,0b11111111,0b11111111,0b11111100,
    0b00000000,0b00000000,0b00000000,0b00000000,
  };
*/
  //  perl misc/itob.pl xeij/SoundMonitor.java SMN_ZOOM_X2_ICON
  public static final byte[] SMN_ZOOM_X2_ICON = "\0\0\0\0?\377\377\3740\0\0\f0\0\0\f0\0?\f0\0\3\3140\0\3\3143\317\17\f3\317\17\f0\374<\f3\317<\f3\317?\3140\0\0\f0\0\0\f?\377\377\374\0\0\0\0".getBytes (XEiJ.ISO_8859_1);
  public static int smnZoomRequest;  //次の更新でsmnZoomShiftに設定する値
  public static int smnZoomShift;  //0=1倍,1=2倍

  //パネル
  public static final int SMN_HEIGHT = SMN_TONE_Y + SMN_TONE_HEIGHT + 4;
  public static boolean smnIsVisible;
  public static BufferedImage smnImage;
  public static byte[] smnBitmap;
  public static JPanel smnPanel;
  public static JFrame smnFrame;

  //smnInit ()
  //  初期化
  public static void smnInit () {

    //フォント
    {
      final long m = 0b01010100_01010000_01000100_01000000_00010100_00010000_00000100_00000000L;
      int k = 0;
      for (int i = 0; i < 127; i++) {
        int t = Indicator.IND_ASCII_3X5[i];
        int d;
        //d = t >> 14 - 6 & 0b01000000 | t >> 13 - 4 & 0b00010000 | t >> 12 - 2 & 0b00000100;
        d = (int) (m >>> (t >>> 12 - 3 & 7 << 3)) & 255;
        SMN_FONT_1[k] = (byte)  d;
        SMN_FONT_2[k] = (byte) (d * 2);
        SMN_FONT_3[k] = (byte) (d * 3);
        k++;
        //d = t >> 11 - 6 & 0b01000000 | t >> 10 - 4 & 0b00010000 | t >>  9 - 2 & 0b00000100;
        d = (int) (m >>> (t >>>  9 - 3 & 7 << 3)) & 255;
        SMN_FONT_1[k] = (byte)  d;
        SMN_FONT_2[k] = (byte) (d * 2);
        SMN_FONT_3[k] = (byte) (d * 3);
        k++;
        //d = t >>  8 - 6 & 0b01000000 | t >>  7 - 4 & 0b00010000 | t >>  6 - 2 & 0b00000100;
        d = (int) (m >>> (t >>>  6 - 3 & 7 << 3)) & 255;
        SMN_FONT_1[k] = (byte)  d;
        SMN_FONT_2[k] = (byte) (d * 2);
        SMN_FONT_3[k] = (byte) (d * 3);
        k++;
        //d = t <<  6 - 5 & 0b01000000 | t           & 0b00010000 | t >>  3 - 2 & 0b00000100;
        d = (int) (m >>> (t            & 7 << 3)) & 255;
        SMN_FONT_1[k] = (byte)  d;
        SMN_FONT_2[k] = (byte) (d * 2);
        SMN_FONT_3[k] = (byte) (d * 3);
        k++;
        //d = t <<  6 - 2 & 0b01000000 | t <<  4 - 1 & 0b00010000 | t <<  2 - 0 & 0b00000100;
        d = (int) (m >>> (t <<   6 - 3 & 7 << 3)) & 255;
        SMN_FONT_1[k] = (byte)  d;
        SMN_FONT_2[k] = (byte) (d * 2);
        SMN_FONT_3[k] = (byte) (d * 3);
        k++;
      }
    }

    //波形
    //smnOPMBuffer = new int[SoundSource.SND_CHANNELS * OPM.OPM_BLOCK_SAMPLES];
    //smnPCMBuffer = new int[SoundSource.SND_CHANNELS * OPM.OPM_BLOCK_SAMPLES];
    for (int scaleX = 0; scaleX <= SMN_WAVE_SCALE_X_MAX; scaleX++) {
      int limit = SMN_WAVE_WIDTH << scaleX;
      int[] index0 = smnWaveIndex0[scaleX] = new int[limit];
      if (SoundSource.SND_CHANNELS == 1) {  //モノラル
        for (int x = 0; x < limit; x++) {
          index0[x] = x * OPM.OPM_BLOCK_SAMPLES / limit;
        }
      } else {  //ステレオ
        int[] index1 = smnWaveIndex1[scaleX] = new int[limit];
        for (int x = 0; x < limit; x++) {
          index1[x] = (index0[x] = x * OPM.OPM_BLOCK_SAMPLES / limit << 1) + 1;
        }
      }
    }
    smnWaveScaleX = 0;
    smnWaveScaleY = 0;
    smnWaveOffsetMax = (SMN_WAVE_WIDTH << smnWaveScaleX) - SMN_WAVE_WIDTH;
    smnWaveOffset = 0;
    smnWaveElevation = 0;
    smnWaveDragOn = false;
    smnWavePressedX = -1;
    smnWavePressedY = -1;
    //smnWaveLastYPCMLeft = new int[2];
    //smnWaveLastYPCMRight = new int[2];
    //smnWaveLastYOPMLeft = new int[2];
    //smnWaveLastYOPMRight = new int[2];
    smnWaveLastYPCMLeft[0] = 0;
    smnWaveLastYPCMLeft[1] = 0;
    smnWaveLastYPCMRight[0] = 0;
    smnWaveLastYPCMRight[1] = 0;
    smnWaveLastYOPMLeft[0] = 0;
    smnWaveLastYOPMLeft[1] = 0;
    smnWaveLastYOPMRight[0] = 0;
    smnWaveLastYOPMRight[1] = 0;

    //スペクトラムアナライザ
    //smnSpectrumBuffer = new double[SMN_SPECTRUM_PARTITIONS][];  //バッファ
    //smnSpectrumX = new double[SMN_SPECTRUM_N];  //実数部
    //smnSpectrumY = new double[SMN_SPECTRUM_N];  //虚数部
    //smnSpectrumIndex = new int[480];  //幅<<16|インデックス
    //smnSpectrumValue = new int[480];  //前回の値
    for (int partition = 0; partition < SMN_SPECTRUM_PARTITIONS; partition++) {
      if (partition != 1) {
        smnSpectrumBuffer[partition] = new double[SMN_SPECTRUM_N];
      }
      double coeff = (double) SMN_SPECTRUM_N * 440.0 / 62500.0 * (double) SMN_SPECTRUM_INTERVAL[partition];
      for (int x = SMN_SPECTRUM_RANGE * partition, x1 = x + SMN_SPECTRUM_RANGE; x < x1; x++) {
        int k = (int) Math.floor (coeff * Math.pow (
          2.0,
          (double) (((56 - 8 + x) / 56 - 1) * 96 + SMN_SPECTRUM_MAP[(56 - 8 + x) % 56] - (96 * 4 + 75)) / 96.0));
        int n = (int) Math.floor (coeff * Math.pow (
          2.0,
          (double) (((56 - 8 + 1 + x) / 56 - 1) * 96 + SMN_SPECTRUM_MAP[(56 - 8 + 1 + x) % 56] - (96 * 4 + 75)) / 96.0)) - k;
        smnSpectrumIndex[x] = n << 16 | k;
        //smnSpectrumValue[x] = -1;
      }
    }  //for partition
    Arrays.fill (smnSpectrumValue, -1);
    //smnSpectrumWindow = new double[SMN_SPECTRUM_N];  //窓関数
    for (int i = 0; i < SMN_SPECTRUM_N; i++) {
      smnSpectrumWindow[i] = 0.5 * (1.0 - Math.cos (2.0 * Math.PI / (double) SMN_SPECTRUM_N * (double) i));  //Hanning窓
    }
    smnFFT = new FFT (SMN_SPECTRUM_N);

    //一時停止
    //  初回の更新で一時停止の解除を行うことで一時停止アイコンを描かせる
    smnPauseRequest = false;
    smnPauseUpdate = true;

    //ズーム
    smnZoomRequest = 0;
    smnZoomShift = 1;

    //スライダ
    smnPaletDragSlider = -1;
    smnPaletPressedX = 0;

  }  //smnInit()

  //smnStart ()
  public static void smnStart () {
    if (RestorableFrame.rfmGetOpened (Settings.SGS_SMN_FRAME_KEY)) {
      smnOpen ();
    }
  }  //smnStart()

  //smnOpen ()
  //  音声モニタを開く
  public static void smnOpen () {
    if (smnFrame == null) {
      smnMakeFrame ();
    }
    smnIsVisible = true;
    smnUpdate ();
    XEiJ.pnlExitFullScreen (false);
    smnFrame.setVisible (true);
  }  //smnOpen()

  //smnMakeFrame ()
  //  ウインドウを作る
  //  ここでは開かない
  public static void smnMakeFrame () {

    //イメージ
    smnMakeImage ();

    //パネル
    smnPanel = ComponentFactory.setFixedSize (new JPanel () {
      @Override public void paint (Graphics g) {
        if (smnZoomShift == 0) {
          g.drawImage (smnImage, 0, 0, null);
        } else {
          g.drawImage (smnImage,
                       0, 0, SMN_WIDTH << smnZoomShift, SMN_HEIGHT << smnZoomShift,
                       0, 0, SMN_WIDTH, SMN_HEIGHT,
                       null);
        }
      }
      @Override protected void paintComponent (Graphics g) {
      }
      @Override protected void paintBorder (Graphics g) {
      }
      @Override protected void paintChildren (Graphics g) {
      }
      @Override public void update (Graphics g) {
      }
    }, SMN_WIDTH, SMN_HEIGHT);
    smnPanel.setOpaque (true);

    //マウスリスナー
    ComponentFactory.addListener (
      smnPanel,
      new MouseAdapter () {
        @Override public void mouseClicked (MouseEvent me) {
          int x = me.getX () >> smnZoomShift;
          int y = me.getY () >> smnZoomShift;
          int button = me.getButton ();
          int modifiersEx = me.getModifiersEx ();
          if (SMN_WAVE_X <= x && x < SMN_WAVE_X + SMN_WAVE_WIDTH &&
              SMN_WAVE_Y <= y && y < SMN_WAVE_Y + SMN_WAVE_HEIGHT) {  //波形
            if ((modifiersEx & InputEvent.SHIFT_DOWN_MASK) == 0) {  //Shiftが押されていない
              int d = 0;
              if (button == MouseEvent.BUTTON1) {  //左ボタンが押された
                if (smnWaveScaleX < SMN_WAVE_SCALE_X_MAX) {  //増やせる
                  d = 1;
                }
              } else if (button == MouseEvent.BUTTON3) {  //右ボタンが押された
                if (0 < smnWaveScaleX) {  //減らせる
                  d = -1;
                }
              }
              if (d != 0) {
                smnWaveScaleX += d;
                smnWaveOffsetMax = (SMN_WAVE_WIDTH << smnWaveScaleX) - SMN_WAVE_WIDTH;
                int o = x - SMN_WAVE_X;  //SMN_WAVE_WIDTH/2。拡大縮小の中心
                smnWaveOffset = Math.max (0, Math.min (smnWaveOffsetMax, (d >= 0 ? smnWaveOffset + o << d : smnWaveOffset + o >> -d) - o));
                if (smnPauseUpdate) {  //一時停止中
                  smnWavePaint ();
                  smnPanel.repaint ();
                }
              }
            } else {  //Shiftが押されている
              int d = 0;
              if (button == MouseEvent.BUTTON1) {  //左ボタンが押された
                if (smnWaveScaleY < SMN_WAVE_SCALE_Y_MAX) {  //増やせる
                  d = 1;
                }
              } else if (button == MouseEvent.BUTTON3) {  //右ボタンが押された
                if (0 < smnWaveScaleY) {  //減らせる
                  d = -1;
                }
              }
              if (d != 0) {
                //  smnWaveElevationはグラフが下にずれて上の方が見えているときマイナス、上にずれて下の方が見えているいるときプラス
                //  グラフの高さSMN_WAVE_HEIGHT*2^smnWaveScaleY
                //  グラフの上端から画面の中央までSMN_WAVE_HEIGHT*2^smnWaveScaleY/2+smnWaveElevation
                //  画面の中央からクリックした位置までo=y-(SMN_WAVE_Y+SMN_WAVE_HEIGHT/2)
                //  グラフの上端からクリックした位置までSMN_WAVE_HEIGHT*2^smnWaveScaleY/2+smnWaveElevation+o
                //  スケールを変更する
                //  グラフの上端からクリックした位置まで(SMN_WAVE_HEIGHT*2^smnWaveScaleY/2+smnWaveElevation+o)*2^d
                //  グラフの上端から画面の中央まで(SMN_WAVE_HEIGHT*2^smnWaveScaleY/2+smnWaveElevation+o)*2^d-o
                //  新しいelevation=(SMN_WAVE_HEIGHT*2^smnWaveScaleY/2+smnWaveElevation+o)*2^d-o-SMN_WAVE_HEIGHT*2^smnWaveScaleY*2^d/2
                //                 =(smnWaveElevation+o)*2^d-o
                smnWaveScaleY += d;
                int spaceY = (SMN_WAVE_HEIGHT >> 1 << smnWaveScaleY) - (SMN_WAVE_HEIGHT >> 1);
                int o = y - (SMN_WAVE_Y + SMN_WAVE_HEIGHT / 2);
                smnWaveElevation = Math.max (-spaceY, Math.min (spaceY, (d >= 0 ? smnWaveElevation + o << d : smnWaveElevation + o >> -d) - o));
                if (smnPauseUpdate) {  //一時停止中
                  smnWavePaint ();
                  smnPanel.repaint ();
                }
              }
            }
          } else if (SMN_PAUSE_X <= x && x < SMN_PAUSE_X + SMN_PAUSE_WIDTH &&
                     SMN_PAUSE_Y <= y && y < SMN_PAUSE_Y + SMN_PAUSE_HEIGHT) {  //一時停止
            smnPauseRequest = !smnPauseRequest;
          } else if (SMN_ZOOM_X <= x && x < SMN_ZOOM_X + SMN_ZOOM_WIDTH &&
                     SMN_ZOOM_Y <= y && y < SMN_ZOOM_Y + SMN_ZOOM_HEIGHT) {  //一時停止
            smnZoomRequest = smnZoomShift ^ 1;
          }
        }
        @Override public void mousePressed (MouseEvent me) {
          int x = me.getX () >> smnZoomShift;
          int y = me.getY () >> smnZoomShift;
          smnWaveDragOn = false;
          smnPaletDragSlider = -1;
          if (SMN_WAVE_X <= x && x < SMN_WAVE_X + SMN_WAVE_WIDTH &&
              SMN_WAVE_Y <= y && y < SMN_WAVE_Y + SMN_WAVE_HEIGHT) {  //波形
            smnWaveDragOn = true;
            smnWavePressedX = x;
            smnWavePressedY = y;
          } else if (SMN_PALET_X <= x && x < SMN_PALET_X + SMN_PALET_WIDTH &&
                     SMN_PALET_Y <= y && y < SMN_PALET_Y + SMN_PALET_HEIGHT) {  //パレット
            for (int n = 0; n < SMN_SLIDER_ARRAY.length; n++) {
              int[] slider = SMN_SLIDER_ARRAY[n];
              int x0 = slider[0];
              int y0 = slider[1];
              int max = slider[2];
              int value = slider[4];
              if (x0 <= x && x < x0 + max + 4 && y0 <= y && y < y0 + 5) {  //スライダの範囲内
                smnPaletDragSlider = n;
                smnPaletPressedX = x;
                if (x < x0 + value || x0 + value + 4 <= x) {  //つまみの範囲外
                  slider[4] = Math.max (0, Math.min (max, x - x0));
                  smnDrawPaletSlider ();
                  smnUpdateColor ();
                }
                break;
              }
            }
          }
        }
        @Override public void mouseReleased (MouseEvent me) {
          smnWaveDragOn = false;
          smnPaletDragSlider = -1;
        }
      });

    //マウスモーションリスナー
    ComponentFactory.addListener (
      smnPanel,
      new MouseMotionAdapter () {
        @Override public void mouseDragged (MouseEvent me) {
          int x = me.getX () >> smnZoomShift;
          int y = me.getY () >> smnZoomShift;
          if (smnWaveDragOn) {  //波形のドラッグ中
            int offset = Math.max (0, Math.min (smnWaveOffsetMax, smnWaveOffset - (x - smnWavePressedX)));
            int spaceY = (SMN_WAVE_HEIGHT >> 1 << smnWaveScaleY) - (SMN_WAVE_HEIGHT >> 1);
            int elevation = Math.max (-spaceY, Math.min (spaceY, smnWaveElevation - (y - smnWavePressedY)));
            smnWavePressedX = x;
            smnWavePressedY = y;
            if (smnWaveOffset != offset || smnWaveElevation != elevation) {
              smnWaveOffset = offset;
              smnWaveElevation = elevation;
              if (smnPauseUpdate) {  //一時停止中
                smnWavePaint ();
                smnPanel.repaint ();
              }
            }
          } else if (smnPaletDragSlider >= 0) {  //パレットのスライダのドラッグ中
            int[] slider = SMN_SLIDER_ARRAY[smnPaletDragSlider];
            int max = slider[2];
            int value = slider[4];
            slider[4] = Math.max (0, Math.min (max, value + x - smnPaletPressedX));
            smnPaletPressedX = x;
            smnDrawPaletSlider ();
            smnUpdateColor ();
          }
        }
      });

    //ウインドウ
    smnFrame = Multilingual.mlnTitle (
      ComponentFactory.createRestorableSubFrame (
        Settings.SGS_SMN_FRAME_KEY,
        "Sound Monitor",
        null,
        smnPanel,
        false  //リサイズ不可
        ),
      "ja", "音声モニタ");
    ComponentFactory.addListener (
      smnFrame,
      new WindowAdapter () {
        @Override public void windowClosing (WindowEvent we) {
          smnIsVisible = false;
        }
      });

  }  //smnMakeFrame()

  //smnMakeImage ()
  //  イメージを作る
  public static void smnMakeImage () {

    //ビットマップ
    smnMakeColorModel ();
    smnImage = new BufferedImage (SMN_WIDTH, SMN_HEIGHT, BufferedImage.TYPE_BYTE_BINARY, smnColorModel);
    byte[] bb = smnBitmap = ((DataBufferByte) smnImage.getRaster ().getDataBuffer ()).getData ();

    //波形
    //アイコン
    //smnDrawIcon1 (SMN_WAVE_X / 4 - 2, SMN_WAVE_Y, 8);

    //スペクトラムアナライザ
    //アイコン
    smnDrawIcon3 (SMN_SPECTRUM_X / 4, SMN_SPECTRUM_Y, 9);

    //440Hzマーク
    smnDrawPattern (SMN_KEY_X + 56 * 4 + 43, SMN_KEY_Y, SMN_440HZ_PATTERN);

    //鍵盤と音色
    for (int ch = 0; ch < 8; ch++) {
      //鍵盤
      smnDrawIcon3 (SMN_KEY_X / 4 - 2, SMN_KEY_Y + 6 + 12 * ch, ch);  //鍵盤の左のチャンネルアイコン
      smnKey[ch] = -1;
      for (int o = 0, i = SMN_KEY_X / 4 + SMN_OFFSET * (SMN_KEY_Y + 6 + 12 * ch); ; o++, i += 14) {
        smnDrawChar1 (SMN_KEY_X / 4 + 14 * o, SMN_KEY_Y, '0' + o);  //オクターブ
        smnPaintKey (i     ,  0, SMN_KEY_COLOR[ 0]);  //C
        smnPaintKey (i +  1,  1, SMN_KEY_COLOR[ 1]);  //C#
        smnPaintKey (i +  2,  2, SMN_KEY_COLOR[ 2]);  //D
        smnPaintKey (i +  3,  3, SMN_KEY_COLOR[ 3]);  //D#
        smnPaintKey (i +  4,  4, SMN_KEY_COLOR[ 4]);  //E
        if (o == 8) {
          break;
        }
        smnPaintKey (i +  6,  5, SMN_KEY_COLOR[ 5]);  //F
        smnPaintKey (i +  7,  6, SMN_KEY_COLOR[ 6]);  //F#
        smnPaintKey (i +  8,  7, SMN_KEY_COLOR[ 7]);  //G
        smnPaintKey (i +  9,  8, SMN_KEY_COLOR[ 8]);  //G#
        smnPaintKey (i + 10,  9, SMN_KEY_COLOR[ 9]);  //A
        smnPaintKey (i + 11, 10, SMN_KEY_COLOR[10]);  //A#
        smnPaintKey (i + 12, 11, SMN_KEY_COLOR[11]);  //B
      }
      //音色
      //                     1  1  1  2  2  2  3
      //         0  3  6  9  2  5  8  1  4  7  0
      //  0     FC SL WA SY SP PD AD PS AS PN
      //  1     XX XX XX XX XX XX XX XX XX XX XX
      //  2     AR 1R 2R RR 1L TL KS ML T1 T2 AE
      //  3  M1 XX XX XX XX XX XX XX XX XX XX XX
      //  4  C1 XX XX XX XX XX XX XX XX XX XX XX
      //  5  M2 XX XX XX XX XX XX XX XX XX XX XX
      //  6  C2 XX XX XX XX XX XX XX XX XX XX XX
      {
        int x = SMN_TONE_X / 4 + SMN_TONE_BOX_COLS * (0b01_00_10_01_00_10_01_00 >> (ch << 1) & 3);
        int y = SMN_TONE_Y + SMN_TONE_BOX_HEIGHT * (0b10_10_01_01_01_00_00_00 >> (ch << 1) & 3);
        smnDrawIcon3 (x, y, ch);  //音色の左上のチャンネルアイコン
        smnDrawString1 (3 + x,         y, SMN_TONE_H1);  //FC SL WA SY SP PD AD PS AS PN
        smnDrawString1 (3 + x, 6 * 2 + y, SMN_TONE_H2);  //AR 1R 2R RR 1L TL KS ML T1 T2 AE
        smnDrawString1 (    x, 6 * 3 + y, SMN_TONE_M1);  //M1
        smnDrawString1 (    x, 6 * 4 + y, SMN_TONE_C1);  //C1
        smnDrawString1 (    x, 6 * 5 + y, SMN_TONE_M2);  //M2
        smnDrawString1 (    x, 6 * 6 + y, SMN_TONE_C2);  //C2
      }
    }

    //PCM
    smnDrawIcon3 (SMN_PCM_COL, SMN_PCM_Y, 10);  //アイコン
    smnDrawString3 (SMN_PCM_COL +  5, SMN_PCM_Y    , "MHz");
    smnDrawString3 (SMN_PCM_COL +  9, SMN_PCM_Y    , "1/");
    smnDrawString3 (SMN_PCM_COL + 20, SMN_PCM_Y    , "kHz");

    //パレット
    //            1111111111222222222233333
    //  01234567890123456789012345678901234
    //          H           S         B
    //  0 hhhhhhhhhhhhh sssssssss bbbbbbbbb
    //  1 hhhhhhhhhhhhh sssssssss bbbbbbbbb
    //  2 hhhhhhhhhhhhh sssssssss bbbbbbbbb
    //  3 hhhhhhhhhhhhh sssssssss bbbbbbbbb
    smnDrawChar1 (SMN_PALET_X / 4 +  8, SMN_PALET_Y, 'H');
    smnDrawChar1 (SMN_PALET_X / 4 + 20, SMN_PALET_Y, 'S');
    smnDrawChar1 (SMN_PALET_X / 4 + 30, SMN_PALET_Y, 'B');
    for (int p = 0; p < 4; p++) {
      smnDrawChar1 (SMN_PALET_X / 4, SMN_PALET_Y + 6 + 6 * p, '0' + p);
    }
    smnDrawPaletSlider ();

  }  //smnMakeImage()

  //smnDrawPaletSlider ()
  //  パレットのスライダを描く
  public static void smnDrawPaletSlider () {
    for (int p = 0; p < 4; p++) {
      int[] pattern = SMN_SLIDER_PATTERN[p];
      smnDrawSlider (SMN_SLIDER_ARRAY[3 * p    ], pattern);
      smnDrawSlider (SMN_SLIDER_ARRAY[3 * p + 1], pattern);
      smnDrawSlider (SMN_SLIDER_ARRAY[3 * p + 2], pattern);
    }
  }  //smnDrawPaletSlider()

  //smnMakeColorModel ()
  //  SMN_SLIDER_ARRAYのvalueからsmnColorModelを作る
  public static void smnMakeColorModel () {
    byte[] r = new byte[4];
    byte[] g = new byte[4];
    byte[] b = new byte[4];
    for (int p = 0; p < 4; p++) {
      int rgb = Color.HSBtoRGB ((float) SMN_SLIDER_ARRAY[3 * p    ][4] / 48F,
                                (float) SMN_SLIDER_ARRAY[3 * p + 1][4] / 32F,
                                (float) SMN_SLIDER_ARRAY[3 * p + 2][4] / 32F);
      if (false) {
        System.out.printf ("%d 0x%08x %2d %2d %2d\n", p, rgb,
                           SMN_SLIDER_ARRAY[3 * p    ][4],
                           SMN_SLIDER_ARRAY[3 * p + 1][4],
                           SMN_SLIDER_ARRAY[3 * p + 2][4]);
      }
      r[p] = (byte) (rgb >> 16);
      g[p] = (byte) (rgb >>  8);
      b[p] = (byte)  rgb;
    }
    smnColorModel = new IndexColorModel (2, 4, r, g, b);
  }  //smnMakeColorModel()

  //smnUpdateColor ()
  public static void smnUpdateColor () {
    smnMakeColorModel ();
    smnImage = new BufferedImage (smnColorModel, smnImage.getRaster(), false, null);
    //smnBitmap = ((DataBufferByte) smnImage.getRaster ().getDataBuffer ()).getData ();
    smnPanel.repaint ();
  }  //smnUpdateColor()

  //smnUpdate ()
  //  音声モニタを更新する
  public static void smnUpdate () {
    byte[] bb = smnBitmap;

    //ズームの処理
    if (smnZoomShift != smnZoomRequest) {  //ズームの要求がある
      smnZoomShift = smnZoomRequest;
      byte[] icon = smnZoomShift == 0 ? SMN_ZOOM_X2_ICON : SMN_ZOOM_X1_ICON;
      int i = SMN_ZOOM_X / 4 + SMN_OFFSET * SMN_ZOOM_Y;
      int j = 0;
      for (int v = 0; v < SMN_ZOOM_HEIGHT; v++) {
        for (int u = 0; u < SMN_ZOOM_WIDTH; u += 4) {
          bb[i++] = icon[j++];
        }
        i += SMN_OFFSET - SMN_ZOOM_WIDTH / 4;
      }
      int panelWidth = SMN_WIDTH << smnZoomShift;
      int panelHeight = SMN_HEIGHT << smnZoomShift;
      int marginWidth = smnFrame.getWidth () - smnPanel.getWidth ();
      int marginHeight = smnFrame.getHeight () - smnPanel.getHeight ();
      ComponentFactory.setFixedSize (smnFrame, panelWidth + marginWidth, panelHeight + marginHeight);
      smnFrame.setResizable (false);
      ComponentFactory.setFixedSize (smnPanel, panelWidth, panelHeight);
      smnFrame.pack ();
    }

    //一時停止の処理
    if (smnPauseUpdate) {  //更新を一時停止している
      if (smnPauseRequest) {  //一時停止の解除の要求はない
        return;  //何もしない
      }
      smnPauseUpdate = false;  //一時停止を解除する
      int i = SMN_PAUSE_X / 4 + SMN_OFFSET * SMN_PAUSE_Y;
      int j = 0;
      for (int v = 0; v < SMN_PAUSE_HEIGHT; v++) {
        for (int u = 0; u < SMN_PAUSE_WIDTH; u += 4) {
          bb[i++] = SMN_PAUSE_ICON_3[j++];
        }
        i += SMN_OFFSET - SMN_PAUSE_WIDTH / 4;
      }
    } else if (smnPauseRequest) {  //一時停止していないが一時停止の要求がある
      smnPauseUpdate = true;  //一時停止する
      int i = SMN_PAUSE_X / 4 + SMN_OFFSET * SMN_PAUSE_Y;
      int j = 0;
      for (int v = 0; v < SMN_PAUSE_HEIGHT; v++) {
        for (int u = 0; u < SMN_PAUSE_WIDTH; u += 4) {
          bb[i++] = SMN_PLAY_ICON_3[j++];
        }
        i += SMN_OFFSET - SMN_PAUSE_WIDTH / 4;
      }
      smnPanel.repaint ();
      return;
    }

    //波形
    System.arraycopy (OPM.opmBuffer, 0, smnOPMBuffer, 0, SoundSource.SND_CHANNELS * OPM.OPM_BLOCK_SAMPLES);
    System.arraycopy (ADPCM.pcmBuffer, 0, smnPCMBuffer, 0, SoundSource.SND_CHANNELS * OPM.OPM_BLOCK_SAMPLES);
    smnWavePaint ();

    //スペクトラムアナライザ
    //
    //  x座標と周波数の関係
    //    |          1111111111222222222233333333334444444444555555|5
    //    |01234567890123456789012345678901234567890123456789012345|6
    //    |       1111122222333333344444445555566666777778888899999|9
    //    |01235791357913579135678901235791357913579135791357912345|6
    //    |   *   *   *   *   *       *   *   *   *   *   *   *    |
    //    |WWW BBBBBBB BBBBBBB WWW WWW BBBBBBB BBBBBBB BBBBBBB WWW |
    //    |WWW BBBBBBB BBBBBBB WWW WWW BBBBBBB BBBBBBB BBBBBBB WWW |
    //    |WWW BBBBBBB BBBBBBB WWW WWW BBBBBBB BBBBBBB BBBBBBB WWW |
    //    |WWW BBBBBBB BBBBBBB WWW WWW BBBBBBB BBBBBBB BBBBBBB WWW |
    //    |WWW BBBBBBB BBBBBBB WWW WWW BBBBBBB BBBBBBB BBBBBBB WWW |
    //    |WWW                 WWW WWW                         WWW |
    //    |WWWWWWW WWWWWWW WWWWWWW WWWWWWW WWWWWWW WWWWWWW WWWWWWW |
    //    |WWWWWWW WWWWWWW WWWWWWW WWWWWWW WWWWWWW WWWWWWW WWWWWWW |
    //    |WWWWWWW WWWWWWW WWWWWWW WWWWWWW WWWWWWW WWWWWWW WWWWWWW |
    //    |WWWWWWW WWWWWWW WWWWWWW WWWWWWW WWWWWWW WWWWWWW WWWWWWW |
    //    |WWWWWWW WWWWWWW WWWWWWW WWWWWWW WWWWWWW WWWWWWW WWWWWWW |
    //    鍵盤とスペクトラムアナライザの位置を合わせるため、x座標-8を56で割った商を96倍、余りをSMN_SPECTRUM_MAPで0～95に変換する
    //    O4Aのx=8+56*4+43=275の変換結果の96*4+75=459を440Hzに合わせる
    //    perl -e "@m=(0,1,2,3,5,7,9,11,13,15,17,19,21,23,25,27,29,31,33,35,36,37,38,39,40,41,42,43,45,47,49,51,53,55,57,59,61,63,65,67,69,71,73,75,77,79,81,83,85,87,89,91,92,93,94,95);for$x(0..479){$x%10 or print'    //    ';printf'|%4d%8.2f',$x,440*2**(((int(($x-8+56)/56)-1)*96+$m[($x-8+56)%56]-(96*4+75))/96);$x%10==9 and printf'|%c',10;}"
    //    |   0   14.78|   1   14.99|   2   15.21|   3   15.43|   4   15.55|   5   15.66|   6   15.77|   7   15.89|   8   16.00|   9   16.12|
    //    |  10   16.23|  11   16.35|  12   16.59|  13   16.83|  14   17.08|  15   17.32|  16   17.58|  17   17.83|  18   18.09|  19   18.35|
    //    |  20   18.62|  21   18.89|  22   19.17|  23   19.45|  24   19.73|  25   20.02|  26   20.31|  27   20.60|  28   20.75|  29   20.90|
    //    |  30   21.05|  31   21.21|  32   21.36|  33   21.51|  34   21.67|  35   21.83|  36   22.14|  37   22.47|  38   22.79|  39   23.12|
    //    |  40   23.46|  41   23.80|  42   24.15|  43   24.50|  44   24.86|  45   25.22|  46   25.58|  47   25.96|  48   26.33|  49   26.72|
    //    |  50   27.11|  51   27.50|  52   27.90|  53   28.31|  54   28.72|  55   29.14|  56   29.56|  57   29.99|  58   30.43|  59   30.87|
    //    |  60   31.09|  61   31.32|  62   31.54|  63   31.77|  64   32.00|  65   32.23|  66   32.47|  67   32.70|  68   33.18|  69   33.66|
    //    |  70   34.15|  71   34.65|  72   35.15|  73   35.66|  74   36.18|  75   36.71|  76   37.24|  77   37.78|  78   38.33|  79   38.89|
    //    |  80   39.46|  81   40.03|  82   40.61|  83   41.20|  84   41.50|  85   41.80|  86   42.11|  87   42.41|  88   42.72|  89   43.03|
    //    |  90   43.34|  91   43.65|  92   44.29|  93   44.93|  94   45.59|  95   46.25|  96   46.92|  97   47.60|  98   48.30|  99   49.00|
    //    | 100   49.71| 101   50.44| 102   51.17| 103   51.91| 104   52.67| 105   53.43| 106   54.21| 107   55.00| 108   55.80| 109   56.61|
    //    | 110   57.44| 111   58.27| 112   59.12| 113   59.98| 114   60.85| 115   61.74| 116   62.18| 117   62.63| 118   63.09| 119   63.54|
    //    | 120   64.00| 121   64.47| 122   64.94| 123   65.41| 124   66.36| 125   67.32| 126   68.30| 127   69.30| 128   70.30| 129   71.33|
    //    | 130   72.36| 131   73.42| 132   74.48| 133   75.57| 134   76.67| 135   77.78| 136   78.91| 137   80.06| 138   81.23| 139   82.41|
    //    | 140   83.00| 141   83.61| 142   84.21| 143   84.82| 144   85.44| 145   86.06| 146   86.68| 147   87.31| 148   88.58| 149   89.87|
    //    | 150   91.17| 151   92.50| 152   93.84| 153   95.21| 154   96.59| 155   98.00| 156   99.42| 157  100.87| 158  102.34| 159  103.83|
    //    | 160  105.34| 161  106.87| 162  108.42| 163  110.00| 164  111.60| 165  113.22| 166  114.87| 167  116.54| 168  118.24| 169  119.96|
    //    | 170  121.70| 171  123.47| 172  124.37| 173  125.27| 174  126.17| 175  127.09| 176  128.01| 177  128.94| 178  129.87| 179  130.81|
    //    | 180  132.72| 181  134.65| 182  136.60| 183  138.59| 184  140.61| 185  142.65| 186  144.73| 187  146.83| 188  148.97| 189  151.13|
    //    | 190  153.33| 191  155.56| 192  157.83| 193  160.12| 194  162.45| 195  164.81| 196  166.01| 197  167.21| 198  168.42| 199  169.64|
    //    | 200  170.87| 201  172.11| 202  173.36| 203  174.61| 204  177.15| 205  179.73| 206  182.34| 207  185.00| 208  187.69| 209  190.42|
    //    | 210  193.19| 211  196.00| 212  198.85| 213  201.74| 214  204.68| 215  207.65| 216  210.67| 217  213.74| 218  216.85| 219  220.00|
    //    | 220  223.20| 221  226.45| 222  229.74| 223  233.08| 224  236.47| 225  239.91| 226  243.40| 227  246.94| 228  248.73| 229  250.53|
    //    | 230  252.35| 231  254.18| 232  256.02| 233  257.87| 234  259.74| 235  261.63| 236  265.43| 237  269.29| 238  273.21| 239  277.18|
    //    | 240  281.21| 241  285.30| 242  289.45| 243  293.66| 244  297.94| 245  302.27| 246  306.67| 247  311.13| 248  315.65| 249  320.24|
    //    | 250  324.90| 251  329.63| 252  332.02| 253  334.42| 254  336.85| 255  339.29| 256  341.74| 257  344.22| 258  346.72| 259  349.23|
    //    | 260  354.31| 261  359.46| 262  364.69| 263  369.99| 264  375.38| 265  380.84| 266  386.38| 267  392.00| 268  397.70| 269  403.48|
    //    | 270  409.35| 271  415.30| 272  421.35| 273  427.47| 274  433.69| 275  440.00| 276  446.40| 277  452.89| 278  459.48| 279  466.16|
    //    | 280  472.94| 281  479.82| 282  486.80| 283  493.88| 284  497.46| 285  501.07| 286  504.70| 287  508.36| 288  512.04| 289  515.75|
    //    | 290  519.49| 291  523.25| 292  530.86| 293  538.58| 294  546.42| 295  554.37| 296  562.43| 297  570.61| 298  578.91| 299  587.33|
    //    | 300  595.87| 301  604.54| 302  613.33| 303  622.25| 304  631.30| 305  640.49| 306  649.80| 307  659.26| 308  664.03| 309  668.84|
    //    | 310  673.69| 311  678.57| 312  683.49| 313  688.44| 314  693.43| 315  698.46| 316  708.62| 317  718.92| 318  729.38| 319  739.99|
    //    | 320  750.75| 321  761.67| 322  772.75| 323  783.99| 324  795.39| 325  806.96| 326  818.70| 327  830.61| 328  842.69| 329  854.95|
    //    | 330  867.38| 331  880.00| 332  892.80| 333  905.79| 334  918.96| 335  932.33| 336  945.89| 337  959.65| 338  973.61| 339  987.77|
    //    | 340  994.92| 341 1002.13| 342 1009.40| 343 1016.71| 344 1024.08| 345 1031.50| 346 1038.97| 347 1046.50| 348 1061.72| 349 1077.17|
    //    | 350 1092.83| 351 1108.73| 352 1124.86| 353 1141.22| 354 1157.82| 355 1174.66| 356 1191.74| 357 1209.08| 358 1226.67| 359 1244.51|
    //    | 360 1262.61| 361 1280.97| 362 1299.61| 363 1318.51| 364 1328.06| 365 1337.69| 366 1347.38| 367 1357.15| 368 1366.98| 369 1376.89|
    //    | 370 1386.86| 371 1396.91| 372 1417.23| 373 1437.85| 374 1458.76| 375 1479.98| 376 1501.50| 377 1523.34| 378 1545.50| 379 1567.98|
    //    | 380 1590.79| 381 1613.93| 382 1637.40| 383 1661.22| 384 1685.38| 385 1709.90| 386 1734.77| 387 1760.00| 388 1785.60| 389 1811.57|
    //    | 390 1837.92| 391 1864.66| 392 1891.78| 393 1919.29| 394 1947.21| 395 1975.53| 396 1989.85| 397 2004.27| 398 2018.79| 399 2033.42|
    //    | 400 2048.16| 401 2063.00| 402 2077.95| 403 2093.00| 404 2123.45| 405 2154.33| 406 2185.67| 407 2217.46| 408 2249.71| 409 2282.44|
    //    | 410 2315.64| 411 2349.32| 412 2383.49| 413 2418.16| 414 2453.33| 415 2489.02| 416 2525.22| 417 2561.95| 418 2599.21| 419 2637.02|
    //    | 420 2656.13| 421 2675.38| 422 2694.76| 423 2714.29| 424 2733.96| 425 2753.77| 426 2773.73| 427 2793.83| 428 2834.46| 429 2875.69|
    //    | 430 2917.52| 431 2959.96| 432 3003.01| 433 3046.69| 434 3091.00| 435 3135.96| 436 3181.58| 437 3227.85| 438 3274.80| 439 3322.44|
    //    | 440 3370.76| 441 3419.79| 442 3469.53| 443 3520.00| 444 3571.20| 445 3623.14| 446 3675.84| 447 3729.31| 448 3783.55| 449 3838.59|
    //    | 450 3894.42| 451 3951.07| 452 3979.70| 453 4008.54| 454 4037.58| 455 4066.84| 456 4096.31| 457 4126.00| 458 4155.89| 459 4186.01|
    //    | 460 4246.90| 461 4308.67| 462 4371.34| 463 4434.92| 464 4499.43| 465 4564.88| 466 4631.27| 467 4698.64| 468 4766.98| 469 4836.32|
    //    | 470 4906.66| 471 4978.03| 472 5050.44| 473 5123.90| 474 5198.43| 475 5274.04| 476 5312.26| 477 5350.75| 478 5389.53| 479 5428.58|
    //
    //  周波数の範囲の分割
    //    周波数の範囲が広いので1回のFFTで全体を処理しようとすると端の方で大きな誤差または大きな無駄が生じる
    //    周波数の範囲を5分割してサンプリング間隔を変えて処理する
    //    分割数が少ないと区間毎のサンプリング期間の違いが大きくなり遅延時間にずれが生じて区間の境目に壁ができてしまう
    //    周波数の低い区間では完全に分解しようとすると大きな遅延が生じてしまうので分解能よりもレスポンスを優先する
    //
    //                                                サンプリング          更新サンプル数
    //             x座標          周波数         間隔   周波数   数    期間      /ブロック
    //      0,1    0..159    14.78Hz..103.83Hz    100    625Hz  1024  1.638s        25
    //       2   160..239   105.34Hz..277.18Hz     50   1250Hz  1024  0.819s        50
    //       3   240..319   281.21Hz..739.99Hz     25   2500Hz  1024  0.410s       100
    //       4   320..399   750.75Hz..2033.42Hz    10   6250Hz  1024  0.164s       250
    //       5   400..479  2048.16Hz..5428.58Hz     5  12500Hz  1024  0.082s       500
    //
    //  x座標とフーリエ変換の結果のインデックスの関係
    //
    //    perl -e "@m=(0,1,2,3,5,7,9,11,13,15,17,19,21,23,25,27,29,31,33,35,36,37,38,39,40,41,42,43,45,47,49,51,53,55,57,59,61,63,65,67,69,71,73,75,77,79,81,83,85,87,89,91,92,93,94,95);for$r([0,160,100],[160,240,50],[240,320,25],[320,400,10],[400,480,5]){for$x($r->[0]..$r->[1]){$x%10 or print'    //    ';printf'|%4d%4d',$x,int($r->[2]*1024/62500*440*2**(((int(($x-8+56)/56)-1)*96+$m[($x-8+56)%56]-(96*4+75))/96));$x%10==9 and printf'|%c',10;}printf'|%c',10;}"
    //    |   0  24|   1  24|   2  24|   3  25|   4  25|   5  25|   6  25|   7  26|   8  26|   9  26|
    //    |  10  26|  11  26|  12  27|  13  27|  14  27|  15  28|  16  28|  17  29|  18  29|  19  30|
    //    |  20  30|  21  30|  22  31|  23  31|  24  32|  25  32|  26  33|  27  33|  28  33|  29  34|
    //    |  30  34|  31  34|  32  34|  33  35|  34  35|  35  35|  36  36|  37  36|  38  37|  39  37|
    //    |  40  38|  41  38|  42  39|  43  40|  44  40|  45  41|  46  41|  47  42|  48  43|  49  43|
    //    |  50  44|  51  45|  52  45|  53  46|  54  47|  55  47|  56  48|  57  49|  58  49|  59  50|
    //    |  60  50|  61  51|  62  51|  63  52|  64  52|  65  52|  66  53|  67  53|  68  54|  69  55|
    //    |  70  55|  71  56|  72  57|  73  58|  74  59|  75  60|  76  61|  77  61|  78  62|  79  63|
    //    |  80  64|  81  65|  82  66|  83  67|  84  67|  85  68|  86  68|  87  69|  88  69|  89  70|
    //    |  90  71|  91  71|  92  72|  93  73|  94  74|  95  75|  96  76|  97  77|  98  79|  99  80|
    //    | 100  81| 101  82| 102  83| 103  85| 104  86| 105  87| 106  88| 107  90| 108  91| 109  92|
    //    | 110  94| 111  95| 112  96| 113  98| 114  99| 115 101| 116 101| 117 102| 118 103| 119 104|
    //    | 120 104| 121 105| 122 106| 123 107| 124 108| 125 110| 126 111| 127 113| 128 115| 129 116|
    //    | 130 118| 131 120| 132 122| 133 123| 134 125| 135 127| 136 129| 137 131| 138 133| 139 135|
    //    | 140 135| 141 136| 142 137| 143 138| 144 139| 145 140| 146 142| 147 143| 148 145| 149 147|
    //    | 150 149| 151 151| 152 153| 153 155| 154 158| 155 160| 156 162| 157 165| 158 167| 159 170|
    //    | 160 172|
    //    | 160  86| 161  87| 162  88| 163  90| 164  91| 165  92| 166  94| 167  95| 168  96| 169  98|
    //    | 170  99| 171 101| 172 101| 173 102| 174 103| 175 104| 176 104| 177 105| 178 106| 179 107|
    //    | 180 108| 181 110| 182 111| 183 113| 184 115| 185 116| 186 118| 187 120| 188 122| 189 123|
    //    | 190 125| 191 127| 192 129| 193 131| 194 133| 195 135| 196 135| 197 136| 198 137| 199 138|
    //    | 200 139| 201 140| 202 142| 203 143| 204 145| 205 147| 206 149| 207 151| 208 153| 209 155|
    //    | 210 158| 211 160| 212 162| 213 165| 214 167| 215 170| 216 172| 217 175| 218 177| 219 180|
    //    | 220 182| 221 185| 222 188| 223 190| 224 193| 225 196| 226 199| 227 202| 228 203| 229 205|
    //    | 230 206| 231 208| 232 209| 233 211| 234 212| 235 214| 236 217| 237 220| 238 223| 239 227|
    //    | 240 230|
    //    | 240 115| 241 116| 242 118| 243 120| 244 122| 245 123| 246 125| 247 127| 248 129| 249 131|
    //    | 250 133| 251 135| 252 135| 253 136| 254 137| 255 138| 256 139| 257 140| 258 142| 259 143|
    //    | 260 145| 261 147| 262 149| 263 151| 264 153| 265 155| 266 158| 267 160| 268 162| 269 165|
    //    | 270 167| 271 170| 272 172| 273 175| 274 177| 275 180| 276 182| 277 185| 278 188| 279 190|
    //    | 280 193| 281 196| 282 199| 283 202| 284 203| 285 205| 286 206| 287 208| 288 209| 289 211|
    //    | 290 212| 291 214| 292 217| 293 220| 294 223| 295 227| 296 230| 297 233| 298 237| 299 240|
    //    | 300 244| 301 247| 302 251| 303 254| 304 258| 305 262| 306 266| 307 270| 308 271| 309 273|
    //    | 310 275| 311 277| 312 279| 313 281| 314 284| 315 286| 316 290| 317 294| 318 298| 319 303|
    //    | 320 307|
    //    | 320 123| 321 124| 322 126| 323 128| 324 130| 325 132| 326 134| 327 136| 328 138| 329 140|
    //    | 330 142| 331 144| 332 146| 333 148| 334 150| 335 152| 336 154| 337 157| 338 159| 339 161|
    //    | 340 163| 341 164| 342 165| 343 166| 344 167| 345 169| 346 170| 347 171| 348 173| 349 176|
    //    | 350 179| 351 181| 352 184| 353 186| 354 189| 355 192| 356 195| 357 198| 358 200| 359 203|
    //    | 360 206| 361 209| 362 212| 363 216| 364 217| 365 219| 366 220| 367 222| 368 223| 369 225|
    //    | 370 227| 371 228| 372 232| 373 235| 374 239| 375 242| 376 246| 377 249| 378 253| 379 256|
    //    | 380 260| 381 264| 382 268| 383 272| 384 276| 385 280| 386 284| 387 288| 388 292| 389 296|
    //    | 390 301| 391 305| 392 309| 393 314| 394 319| 395 323| 396 326| 397 328| 398 330| 399 333|
    //    | 400 335|
    //    | 400 167| 401 169| 402 170| 403 171| 404 173| 405 176| 406 179| 407 181| 408 184| 409 186|
    //    | 410 189| 411 192| 412 195| 413 198| 414 200| 415 203| 416 206| 417 209| 418 212| 419 216|
    //    | 420 217| 421 219| 422 220| 423 222| 424 223| 425 225| 426 227| 427 228| 428 232| 429 235|
    //    | 430 239| 431 242| 432 246| 433 249| 434 253| 435 256| 436 260| 437 264| 438 268| 439 272|
    //    | 440 276| 441 280| 442 284| 443 288| 444 292| 445 296| 446 301| 447 305| 448 309| 449 314|
    //    | 450 319| 451 323| 452 326| 453 328| 454 330| 455 333| 456 335| 457 338| 458 340| 459 342|
    //    | 460 347| 461 352| 462 358| 463 363| 464 368| 465 373| 466 379| 467 384| 468 390| 469 396|
    //    | 470 401| 471 407| 472 413| 473 419| 474 425| 475 432| 476 435| 477 438| 478 441| 479 444|
    //    | 480 447|
    //
    //クリア
    //  毎回減った分だけ消しているので不要
    //Arrays.fill (bb, SMN_OFFSET * SMN_SPECTRUM_Y, SMN_OFFSET * (SMN_SPECTRUM_Y + SMN_SPECTRUM_HEIGHT), SMN_BLACK);
    //アイコン
    //smnDrawIcon3 (SMN_SPECTRUM_X / 4, SMN_SPECTRUM_Y, 9);
    //バッファを更新する
    double[] buffer0 = smnSpectrumBuffer[0];
    double[] buffer2 = smnSpectrumBuffer[2];
    double[] buffer3 = smnSpectrumBuffer[3];
    double[] buffer4 = smnSpectrumBuffer[4];
    double[] buffer5 = smnSpectrumBuffer[5];
    //  区間5
    System.arraycopy (buffer5, 500, buffer5, 0, SMN_SPECTRUM_N - 500);
    if (SoundSource.SND_CHANNELS == 1) {  //モノラル
      for (int i = SMN_SPECTRUM_N - 500, k = 0; i < SMN_SPECTRUM_N; i++, k += 5) {
        buffer5[i] = (double) (OPM.opmBuffer[k    ] + ADPCM.pcmBuffer[k    ] +
                               OPM.opmBuffer[k + 1] + ADPCM.pcmBuffer[k + 1] +
                               OPM.opmBuffer[k + 2] + ADPCM.pcmBuffer[k + 2] +
                               OPM.opmBuffer[k + 3] + ADPCM.pcmBuffer[k + 3] +
                               OPM.opmBuffer[k + 4] + ADPCM.pcmBuffer[k + 4]) * (0.2 / 32768.0);
      }
    } else {  //ステレオ
      for (int i = SMN_SPECTRUM_N - 500, k = 0; i < SMN_SPECTRUM_N; i++, k += 10) {
        buffer5[i] = (double) (OPM.opmBuffer[k    ] + OPM.opmBuffer[k + 1] + ADPCM.pcmBuffer[k    ] + ADPCM.pcmBuffer[k + 1] +
                               OPM.opmBuffer[k + 2] + OPM.opmBuffer[k + 3] + ADPCM.pcmBuffer[k + 2] + ADPCM.pcmBuffer[k + 3] +
                               OPM.opmBuffer[k + 4] + OPM.opmBuffer[k + 5] + ADPCM.pcmBuffer[k + 4] + ADPCM.pcmBuffer[k + 5] +
                               OPM.opmBuffer[k + 6] + OPM.opmBuffer[k + 7] + ADPCM.pcmBuffer[k + 6] + ADPCM.pcmBuffer[k + 7] +
                               OPM.opmBuffer[k + 8] + OPM.opmBuffer[k + 9] + ADPCM.pcmBuffer[k + 8] + ADPCM.pcmBuffer[k + 9]) * (0.1 / 32768.0);
      }
    }
    //  区間4
    System.arraycopy (buffer4, 250, buffer4, 0, SMN_SPECTRUM_N - 250);
    for (int i = SMN_SPECTRUM_N - 250, k = SMN_SPECTRUM_N - 500; i < SMN_SPECTRUM_N; i++, k += 2) {
      buffer4[i] = (buffer5[k] + buffer5[k + 1]) * 0.5;
    }
    //  区間3
    System.arraycopy (buffer3, 100, buffer3, 0, SMN_SPECTRUM_N - 100);
    for (int i = SMN_SPECTRUM_N - 100, k = SMN_SPECTRUM_N - 500; i < SMN_SPECTRUM_N; i++, k += 5) {
      buffer3[i] = (buffer5[k] + buffer5[k + 1] + buffer5[k + 2] + buffer5[k + 3] + buffer5[k + 4]) * 0.2;
    }
    //  区間2
    System.arraycopy (buffer2, 50, buffer2, 0, SMN_SPECTRUM_N - 50);
    for (int i = SMN_SPECTRUM_N - 50, k = SMN_SPECTRUM_N - 100; i < SMN_SPECTRUM_N; i++, k += 2) {
      buffer2[i] = (buffer3[k] + buffer3[k + 1]) * 0.5;
    }
    //  区間0
    System.arraycopy (buffer0, 25, buffer0, 0, SMN_SPECTRUM_N - 25);
    for (int i = SMN_SPECTRUM_N - 25, k = SMN_SPECTRUM_N - 50; i < SMN_SPECTRUM_N; i++, k += 2) {
      buffer0[i] = (buffer2[k] + buffer2[k + 1]) * 0.5;
    }
    for (int partition = 0; partition < SMN_SPECTRUM_PARTITIONS; partition++) {
      if (partition != 1) {  //区間1は区間0の結果を使う
        //実数部
        double[] buffer = smnSpectrumBuffer[partition];
        for (int i = 0; i < SMN_SPECTRUM_N; i++) {
          smnSpectrumX[i] = smnSpectrumWindow[i] * buffer[i];
        }
        //虚数部
        Arrays.fill (smnSpectrumY, 0, SMN_SPECTRUM_N, 0.0);
        //FFTを呼び出す
        //smnFFT.fftSandeTukey2 (smnSpectrumX, smnSpectrumY);  //基数2
        smnFFT.fftSandeTukey4 (smnSpectrumX, smnSpectrumY);  //基数4
      }
      //グラフを描く
      //  区間0はx=8から描く
      for (int x = partition == 0 ? 8 : SMN_SPECTRUM_RANGE * partition, x1 = x + SMN_SPECTRUM_RANGE, b = 6; x < x1; x++, b = b - 2 & 6) {  //b=6,4,2,0
        int k = smnSpectrumIndex[x];
        int n = k >> 16;
        k = (char) k;
        double level = smnSpectrumX[k];
        while (--n > 0) {
          level = Math.max (level, smnSpectrumX[++k]);
        }
        int value = Math.max (0, Math.min (SMN_SPECTRUM_HEIGHT - 1, (int) ((double) SMN_SPECTRUM_HEIGHT / (double) SMN_SPECTRUM_N * SMN_SPECTRUM_SCALE * level)));  //今回の値。0=最小,SMN_SPECTRUM_HEIGHT-1=最大
        int value0 = smnSpectrumValue[x];  //前回の値
        if (value > value0) {  //増えたとき
          smnSpectrumValue[x] = value;
          int i = SMN_OFFSET * (SMN_SPECTRUM_Y + SMN_SPECTRUM_HEIGHT - 1 - value) + SMN_SPECTRUM_X / 4 + (x >> 2);
          while (value > value0) {
            bb[i] = (byte) (bb[i] & ~(3 << b) | (SMN_WHITE & 3) << b);
            i += SMN_OFFSET;
            value--;
          }
        } else if (value < value0) {  //減ったとき
          if (value < value0 - SMN_SPECTRUM_TRAIL) {
            value = value0 - SMN_SPECTRUM_TRAIL;
          }
          smnSpectrumValue[x] = value;
          int i = SMN_OFFSET * (SMN_SPECTRUM_Y + SMN_SPECTRUM_HEIGHT - 1 - value0) + SMN_SPECTRUM_X / 4 + (x >> 2);
          while (value < value0) {
            bb[i] = (byte) (bb[i] & ~(3 << b));
            i += SMN_OFFSET;
            value++;
          }
        }
      }  //for i
    }  //for partition

    //OPM
    for (int ch = 0; ch < 8; ch++) {
      YM2151.fm_channel channel = OPM.opmYM2151.m_channel[ch];
      YM2151.fm_operator opM1 = channel.m_op[0];
      YM2151.fm_operator opM2 = channel.m_op[1];
      YM2151.fm_operator opC1 = channel.m_op[2];
      YM2151.fm_operator opC2 = channel.m_op[3];
      //鍵盤
      int k0 = smnKey[ch];  //前回キーオンされていたかリリース中だったキー。0x10000+キー=リリース中。-1=なし
      //  -=ATTACK/DECAY/SUSTAIN,0=RELEASE,+=SILENCE
      int m1 = opM1.m_env_state < YM2151.EG_RELEASE ? -1 : opM1.m_env_attenuation < YM2151.EG_QUIET ? 0 : 1;
      int m2 = opM2.m_env_state < YM2151.EG_RELEASE ? -1 : opM2.m_env_attenuation < YM2151.EG_QUIET ? 0 : 1;
      int c1 = opC1.m_env_state < YM2151.EG_RELEASE ? -1 : opC1.m_env_attenuation < YM2151.EG_QUIET ? 0 : 1;
      int c2 = opC2.m_env_state < YM2151.EG_RELEASE ? -1 : opC2.m_env_attenuation < YM2151.EG_QUIET ? 0 : 1;
      int k1;  //今回キーオンされているかリリース中のキー。0x10000+キー=リリース中。-1=なし
      if (m1 > 0 && m2 > 0 && c1 > 0 && c2 > 0) {  //すべてSILENCE。キーオフ
        k1 = -1;
      } else {
        //  KC=74のときKF=5.5くらいで440Hzになるので、KFが38以上のときKCを1増やす
        int kc = OPM.opmRegister[0x28 + ch] & 127;
        int kf = (OPM.opmRegister[0x30 + ch] >> 2) & 63;
        k1 = (((kc - (kc >> 2)) << 6 | kf) + (3 << 6) + (64 - 38)) >> 6;  //今回キーオンされているか最後にキーオンされたキー
        if (m1 >= 0 && m2 >= 0 && c1 >= 0 && c2 >= 0) {  //1個以上のRELEASEと0個以上のSILENCE。リリース中
          k1 |= 0x10000;
        }
      }
      if (k0 != k1) {  //キーオンされているキーまたはキーの状態が変わった
        int i = SMN_KEY_X / 4 + SMN_OFFSET * (SMN_KEY_Y + 6 + 12 * ch);  //鍵盤の左端
        if (k0 >= 0 && (short) k0 != (short) k1) {  //前回キーオンされていたかリリース中だったキーがあって、キーが変わった
          //perl optdiv.pl 97 12
          //  x/12==x*43>>>9 (0<=x<=130) [97*43==4171]
          int kk = (short) k0;
          int o = kk * 43 >>> 9;  //オクターブ
          kk -= 12 * o;  //鍵の番号。0..11
          smnPaintKey (i + 14 * o + (kk < 5 ? kk : kk + 1), kk, SMN_KEY_COLOR[kk]);
        }
        if (k1 >= 0 && k0 != k1) {  //今回キーオンされているかリリース中のキーがあって、キーまたはキーの状態が変わった
          //perl optdiv.pl 97 12
          //  x/12==x*43>>>9 (0<=x<=130) [97*43==4171]
          int kk = (short) k1;
          int o = kk * 43 >>> 9;  //オクターブ
          kk -= 12 * o;  //鍵の番号。0..11
          if (k1 >> 16 == 0) {  //キーオンされている
            smnPaintKey (i + 14 * o + (kk < 5 ? kk : kk + 1), kk, SMN_ORANGE);
          } else {  //リリース中
            smnPaintKey2 (i + 14 * o + (kk < 5 ? kk : kk + 1), kk, SMN_KEY_COLOR[kk], SMN_ORANGE);
          }
        }
        smnKey[ch] = k1;
      }
      //音色
      {
        XEiJ.fmtHex2 (SMN_TONE_V0,  0, OPM.opmRegister[0x20 + ch] & 63);  //FLCON
        XEiJ.fmtHex2 (SMN_TONE_V0,  3, OPM.opmRegister[256 + ch]);  //SLOT
        XEiJ.fmtHex2 (SMN_TONE_V0,  6, OPM.opmRegister[0x1b] & 3);  //WAVE
        XEiJ.fmtHex2 (SMN_TONE_V0,  9, 0);  //SYNC
        XEiJ.fmtHex2 (SMN_TONE_V0, 12, OPM.opmRegister[0x18] & 255);  //SPEED
        XEiJ.fmtHex2 (SMN_TONE_V0, 15, OPM.opmRegister[265]);  //PMD
        XEiJ.fmtHex2 (SMN_TONE_V0, 18, OPM.opmRegister[264]);  //AMD
        XEiJ.fmtHex2 (SMN_TONE_V0, 21, (OPM.opmRegister[0x38 + ch] >> 4) & 7);  //PMS
        XEiJ.fmtHex2 (SMN_TONE_V0, 24, OPM.opmRegister[0x38 + ch] & 3);  //AMS
        XEiJ.fmtHex2 (SMN_TONE_V0, 27, (OPM.opmRegister[0x20 + ch] >> 6) & 3);  //PAN
        XEiJ.fmtHex2 (SMN_TONE_V1,  0, OPM.opmRegister[0x80 + ch] & 31);  //M1 AR
        XEiJ.fmtHex2 (SMN_TONE_V2,  0, OPM.opmRegister[0x90 + ch] & 31);  //C1 AR
        XEiJ.fmtHex2 (SMN_TONE_V3,  0, OPM.opmRegister[0x88 + ch] & 31);  //M2 AR
        XEiJ.fmtHex2 (SMN_TONE_V4,  0, OPM.opmRegister[0x98 + ch] & 31);  //C2 AR
        XEiJ.fmtHex2 (SMN_TONE_V1,  3, OPM.opmRegister[0xa0 + ch] & 31);  //M1 D1R
        XEiJ.fmtHex2 (SMN_TONE_V2,  3, OPM.opmRegister[0xb0 + ch] & 31);  //C1 D1R
        XEiJ.fmtHex2 (SMN_TONE_V3,  3, OPM.opmRegister[0xa8 + ch] & 31);  //M2 D1R
        XEiJ.fmtHex2 (SMN_TONE_V4,  3, OPM.opmRegister[0xb8 + ch] & 31);  //C2 D1R
        XEiJ.fmtHex2 (SMN_TONE_V1,  6, OPM.opmRegister[0xc0 + ch] & 31);  //M1 D2R
        XEiJ.fmtHex2 (SMN_TONE_V2,  6, OPM.opmRegister[0xd0 + ch] & 31);  //C1 D2R
        XEiJ.fmtHex2 (SMN_TONE_V3,  6, OPM.opmRegister[0xc8 + ch] & 31);  //M2 D2R
        XEiJ.fmtHex2 (SMN_TONE_V4,  6, OPM.opmRegister[0xd8 + ch] & 31);  //C2 D2R
        XEiJ.fmtHex2 (SMN_TONE_V1,  9, OPM.opmRegister[0xe0 + ch] & 15);  //M1 RR
        XEiJ.fmtHex2 (SMN_TONE_V2,  9, OPM.opmRegister[0xf0 + ch] & 15);  //C1 RR
        XEiJ.fmtHex2 (SMN_TONE_V3,  9, OPM.opmRegister[0xe8 + ch] & 15);  //M2 RR
        XEiJ.fmtHex2 (SMN_TONE_V4,  9, OPM.opmRegister[0xf8 + ch] & 15);  //C2 RR
        XEiJ.fmtHex2 (SMN_TONE_V1, 12, (OPM.opmRegister[0xe0 + ch] >> 4) & 15);  //M1 D1L
        XEiJ.fmtHex2 (SMN_TONE_V2, 12, (OPM.opmRegister[0xf0 + ch] >> 4) & 15);  //C1 D1L
        XEiJ.fmtHex2 (SMN_TONE_V3, 12, (OPM.opmRegister[0xe8 + ch] >> 4) & 15);  //M2 D1L
        XEiJ.fmtHex2 (SMN_TONE_V4, 12, (OPM.opmRegister[0xf8 + ch] >> 4) & 15);  //C2 D1L
        XEiJ.fmtHex2 (SMN_TONE_V1, 15, OPM.opmRegister[0x60 + ch] & 127);  //M1 TL
        XEiJ.fmtHex2 (SMN_TONE_V2, 15, OPM.opmRegister[0x70 + ch] & 127);  //C1 TL
        XEiJ.fmtHex2 (SMN_TONE_V3, 15, OPM.opmRegister[0x68 + ch] & 127);  //M2 TL
        XEiJ.fmtHex2 (SMN_TONE_V4, 15, OPM.opmRegister[0x78 + ch] & 127);  //C2 TL
        XEiJ.fmtHex2 (SMN_TONE_V1, 18, (OPM.opmRegister[0x80 + ch] >> 6) & 3);  //M1 KS
        XEiJ.fmtHex2 (SMN_TONE_V2, 18, (OPM.opmRegister[0x90 + ch] >> 6) & 3);  //C1 KS
        XEiJ.fmtHex2 (SMN_TONE_V3, 18, (OPM.opmRegister[0x88 + ch] >> 6) & 3);  //M2 KS
        XEiJ.fmtHex2 (SMN_TONE_V4, 18, (OPM.opmRegister[0x98 + ch] >> 6) & 3);  //C2 KS
        XEiJ.fmtHex2 (SMN_TONE_V1, 21, OPM.opmRegister[0x40 + ch] & 15);  //M1 MUL
        XEiJ.fmtHex2 (SMN_TONE_V2, 21, OPM.opmRegister[0x50 + ch] & 15);  //C1 MUL
        XEiJ.fmtHex2 (SMN_TONE_V3, 21, OPM.opmRegister[0x48 + ch] & 15);  //M2 MUL
        XEiJ.fmtHex2 (SMN_TONE_V4, 21, OPM.opmRegister[0x58 + ch] & 15);  //C2 MUL
        XEiJ.fmtHex2 (SMN_TONE_V1, 24, (OPM.opmRegister[0x40 + ch] >> 4) & 7);  //M1 DT1
        XEiJ.fmtHex2 (SMN_TONE_V2, 24, (OPM.opmRegister[0x50 + ch] >> 4) & 7);  //C1 DT1
        XEiJ.fmtHex2 (SMN_TONE_V3, 24, (OPM.opmRegister[0x48 + ch] >> 4) & 7);  //M2 DT1
        XEiJ.fmtHex2 (SMN_TONE_V4, 24, (OPM.opmRegister[0x58 + ch] >> 4) & 7);  //C2 DT1
        XEiJ.fmtHex2 (SMN_TONE_V1, 27, (OPM.opmRegister[0xc0 + ch] >> 6) & 3);  //M1 DT2
        XEiJ.fmtHex2 (SMN_TONE_V2, 27, (OPM.opmRegister[0xd0 + ch] >> 6) & 3);  //C1 DT2
        XEiJ.fmtHex2 (SMN_TONE_V3, 27, (OPM.opmRegister[0xc8 + ch] >> 6) & 3);  //M2 DT2
        XEiJ.fmtHex2 (SMN_TONE_V4, 27, (OPM.opmRegister[0xd8 + ch] >> 6) & 3);  //C2 DT2
        XEiJ.fmtHex2 (SMN_TONE_V1, 30, (OPM.opmRegister[0xa0 + ch] >> 7) & 1);  //M1 AMSEN
        XEiJ.fmtHex2 (SMN_TONE_V2, 30, (OPM.opmRegister[0xb0 + ch] >> 7) & 1);  //C1 AMSEN
        XEiJ.fmtHex2 (SMN_TONE_V3, 30, (OPM.opmRegister[0xa8 + ch] >> 7) & 1);  //M2 AMSEN
        XEiJ.fmtHex2 (SMN_TONE_V4, 30, (OPM.opmRegister[0xb8 + ch] >> 7) & 1);  //C2 AMSEN
        int x = SMN_TONE_X / 4 + SMN_TONE_BOX_COLS * (0b01_00_10_01_00_10_01_00 >> (ch << 1) & 3);
        int y = SMN_TONE_Y + SMN_TONE_BOX_HEIGHT * (0b10_10_01_01_01_00_00_00 >> (ch << 1) & 3);
        smnDrawString3 (3 + x, 6     + y, SMN_TONE_V0);
        smnDrawString3 (3 + x, 6 * 3 + y, SMN_TONE_V1);
        smnDrawString3 (3 + x, 6 * 4 + y, SMN_TONE_V2);
        smnDrawString3 (3 + x, 6 * 5 + y, SMN_TONE_V3);
        smnDrawString3 (3 + x, 6 * 6 + y, SMN_TONE_V4);
      }
    }  //for ch

    //PCM
    smnDrawString3 (SMN_PCM_COL +  3, SMN_PCM_Y    , SMN_PCM_OSCILLATOR[ADPCM.pcmOSCFreqMode << 1 | ADPCM.pcmOscillator]);
    smnDrawString3 (SMN_PCM_COL + 11, SMN_PCM_Y    , SMN_PCM_DIVIDER[ADPCM.pcmDivider]);
    smnDrawString3 (SMN_PCM_COL + 16, SMN_PCM_Y    , SMN_PCM_FREQ[ADPCM.pcmOSCFreqMode << 3 | ADPCM.pcmOscillator << 2 | ADPCM.pcmDivider]);
    smnDrawString2 (SMN_PCM_COL +  3, SMN_PCM_Y + 6, SMN_PCM_PLAY[ADPCM.pcmActive ? 1 : 0]);
    smnDrawString2 (SMN_PCM_COL +  8, SMN_PCM_Y + 6, SMN_PCM_DATA[ADPCM.pcmEncodedData >= 0 ? 1 : 0]);
    smnDrawString2 (SMN_PCM_COL + 13, SMN_PCM_Y + 6, SMN_PCM_LEFT[ADPCM.pcmPanLeft == 0 || ADPCM.pcmPanLeft < 0x80000000 + ADPCM.PCM_ATTACK_SPAN * 2 ? 0 : 1]);
    smnDrawString2 (SMN_PCM_COL + 18, SMN_PCM_Y + 6, SMN_PCM_RIGHT[SoundSource.SND_CHANNELS == 1 ?
                                                                   ADPCM.pcmPanLeft == 0 || ADPCM.pcmPanLeft < 0x80000000 + ADPCM.PCM_ATTACK_SPAN * 2 ? 0 : 1 :
                                                                   ADPCM.pcmPanRight == 0 || ADPCM.pcmPanRight < 0x80000000 + ADPCM.PCM_ATTACK_SPAN * 2 ? 0 : 1]);

    smnPanel.repaint ();

  }  //smnUpdate()

  //smnWavePaint ()
  //  波形を描く
  public static void smnWavePaint () {
    byte[] bb = smnBitmap;
    //クリア
    Arrays.fill (bb, SMN_OFFSET * SMN_WAVE_Y, SMN_OFFSET * (SMN_WAVE_Y + SMN_WAVE_HEIGHT), SMN_BLACK);
    //アイコン
    smnDrawIcon3 (SMN_WAVE_X / 4 - 2, SMN_WAVE_Y, 8);
    //時間軸
    if (-SMN_WAVE_HEIGHT / 2 <= smnWaveElevation && smnWaveElevation <= SMN_WAVE_HEIGHT / 2) {
      int i = SMN_OFFSET * (SMN_WAVE_Y + SMN_WAVE_HEIGHT / 2 - smnWaveElevation) + SMN_WAVE_X / 4;
      Arrays.fill (bb, i, i + SMN_WAVE_WIDTH / 4, SMN_BLUE);
    }
    //時間目盛り
    int ms0 = (1000 / SoundSource.SND_BLOCK_FREQ * smnWaveOffset + (SMN_WAVE_WIDTH << smnWaveScaleX) - 1) / (SMN_WAVE_WIDTH << smnWaveScaleX);
    int ms1 = 1000 / SoundSource.SND_BLOCK_FREQ * (smnWaveOffset + SMN_WAVE_WIDTH) / (SMN_WAVE_WIDTH << smnWaveScaleX);
    for (int ms = ms0; ms <= ms1; ms++) {
      //  perl optdiv.pl 11800 1000
      //  x/1000==x*8389>>>23 (0<=x<=21998) [11800*8389==98990200]
      //int x = SoundSource.SND_BLOCK_FREQ * SMN_WAVE_WIDTH * ms / 1000;
      int x = Math.max (0, Math.min (SMN_WAVE_WIDTH - 1, (SoundSource.SND_BLOCK_FREQ * SMN_WAVE_WIDTH * ms * 8389 >>> 23 << smnWaveScaleX) - smnWaveOffset));
      int b = (~x & 3) << 1;  //0→6,1→4,2→2,3→0
      if (ms1 - ms0 <= 10 ||
          ms1 - ms0 <= 20 && (1L << 63 -  0 |
                              1L << 63 -  5 |
                              1L << 63 - 10 |
                              1L << 63 - 15 |
                              1L << 63 - 20 |
                              1L << 63 - 25 |
                              1L << 63 - 30 |
                              1L << 63 - 35 |
                              1L << 63 - 40 |
                              1L << 63 - 45 |
                              1L << 63 - 50 |
                              1L << 63 - 55 |
                              1L << 63 - 60) << ms < 0 ||
          (1L << 63 -  0 |
           1L << 63 - 10 |
           1L << 63 - 20 |
           1L << 63 - 30 |
           1L << 63 - 40 |
           1L << 63 - 50 |
           1L << 63 - 60) << ms < 0) {
        int t = XEiJ.FMT_BCD4[ms];
        int col = SMN_WAVE_X / 4 + Math.max (0, Math.min (SMN_WAVE_WIDTH / 4 - (t >= 0x10 ? 4 : 3), (x + 3 >> 2) - (t >= 0x10 ? 2 : 1)));
        if (t >= 0x10) {
          smnDrawChar1 (col++, SMN_WAVE_Y + SMN_WAVE_HEIGHT - 6, '0' | t >> 4);
        }
        smnDrawChar1 (col    , SMN_WAVE_Y + SMN_WAVE_HEIGHT - 6, '0' | t & 15);
        smnDrawChar1 (col + 1, SMN_WAVE_Y + SMN_WAVE_HEIGHT - 6, 'm');
        smnDrawChar1 (col + 2, SMN_WAVE_Y + SMN_WAVE_HEIGHT - 6, 's');
      }
      int v0, v1;
      if ((1L << 63 -  0 |
           1L << 63 - 10 |
           1L << 63 - 20 |
           1L << 63 - 30 |
           1L << 63 - 40 |
           1L << 63 - 50 |
           1L << 63 - 60) << ms < 0) {
        v0 = SMN_WAVE_HEIGHT / 2 - SMN_WAVE_HEIGHT * 4 / 16;  //0
        v1 = SMN_WAVE_HEIGHT / 2 + SMN_WAVE_HEIGHT * 4 / 16;  //SMN_WAVE_HEIGHT-1
      } else if ((1L << 63 -  0 |
                  1L << 63 -  5 |
                  1L << 63 - 10 |
                  1L << 63 - 15 |
                  1L << 63 - 20 |
                  1L << 63 - 25 |
                  1L << 63 - 30 |
                  1L << 63 - 35 |
                  1L << 63 - 40 |
                  1L << 63 - 45 |
                  1L << 63 - 50 |
                  1L << 63 - 55 |
                  1L << 63 - 60) << ms < 0) {
        v0 = SMN_WAVE_HEIGHT / 2 - SMN_WAVE_HEIGHT * 3 / 16;
        v1 = SMN_WAVE_HEIGHT / 2 + SMN_WAVE_HEIGHT * 3 / 16;
      } else {
        v0 = SMN_WAVE_HEIGHT / 2 - SMN_WAVE_HEIGHT * 2 / 16;
        v1 = SMN_WAVE_HEIGHT / 2 + SMN_WAVE_HEIGHT * 2 / 16;
      }
      v0 -= smnWaveElevation;
      v1 -= smnWaveElevation;
      if (v0 < 0) {
        v0 = 0;
      }
      if (v1 > SMN_WAVE_HEIGHT - 1) {
        v1 = SMN_WAVE_HEIGHT - 1;
      }
      int i = SMN_OFFSET * (SMN_WAVE_Y + v0) + SMN_WAVE_X / 4 + (x >> 2);
      for (int v = v0; v <= v1; v++) {
        bb[i] = (byte) (bb[i] & ~(3 << b) | (SMN_BLUE & 3) << b);
        i += SMN_OFFSET;
      }
    }
    if (SoundSource.SND_CHANNELS == 1) {  //モノラル
      smnWavePaint1 (smnPCMBuffer, smnWaveIndex0[smnWaveScaleX], SMN_ORANGE & 3, smnWaveLastYPCMLeft);  //PCM
      smnWavePaint1 (smnOPMBuffer, smnWaveIndex0[smnWaveScaleX], SMN_WHITE & 3, smnWaveLastYOPMLeft);  //OPM
    } else {  //ステレオ
      smnWavePaint1 (smnPCMBuffer, smnWaveIndex0[smnWaveScaleX], SMN_ORANGE & 3, smnWaveLastYPCMLeft);  //PCM left
      smnWavePaint1 (smnPCMBuffer, smnWaveIndex1[smnWaveScaleX], SMN_ORANGE & 3, smnWaveLastYPCMRight);  //PCM right
      smnWavePaint1 (smnOPMBuffer, smnWaveIndex0[smnWaveScaleX], SMN_WHITE & 3, smnWaveLastYOPMLeft);  //OPM left
      smnWavePaint1 (smnOPMBuffer, smnWaveIndex1[smnWaveScaleX], SMN_WHITE & 3, smnWaveLastYOPMRight);  //OPM right
    }
  }  //smnWavePaint()

  //smnWavePaint1 (buffer, index, palet, lastY)
  public static void smnWavePaint1 (int[] buffer, int[] index, int palet, int[] lastY) {
    byte[] bb = smnBitmap;
    int o = smnWaveOffset;
    int t;
    int y0 = lastY[0];
    int y1 = lastY[1];
    int scaleY = SMN_WAVE_VALUE_SHIFT - smnWaveScaleY;
    for (int x = 0, b = 6; x < SMN_WAVE_WIDTH; x++, b = b - 2 & 6) {  //b=6,4,2,0
      int y2 = SMN_WAVE_HEIGHT / 2 - ((t = buffer[index[o + x]]) + (t >>> -scaleY) >> scaleY);
      int min, max;  //y1がminまたはmaxのときはy1まで、y0またはy2がminまたはmaxのときはy0+y1>>1またはy1+y2>>1まで描く
      if (y0 < y1) {  //y0<y1
        if (y1 < y2) {  //y0<y1<y2
          min = y0 + y1 >> 1;
          max = y1 + y2 >> 1;
        } else if (y0 < y2) {  //y0<y2<y1
          min = y0 + y1 >> 1;
          max = y1;
        } else {  //y2<y0<y1
          min = y1 + y2 >> 1;
          max = y1;
        }
      } else {  //y1<y0
        if (y0 < y2) {  //y1<y0<y2
          min = y1;
          max = y1 + y2 >> 1;
        } else if (y1 < y2) {  //y1<y2<y0
          min = y1;
          max = y0 + y1 >> 1;
        } else {  //y2<y1<y0
          min = y1 + y2 >> 1;
          max = y0 + y1 >> 1;
        }
      }
      min -= smnWaveElevation;
      max -= smnWaveElevation;
      //[min,max]と[0,SMN_WAVE_HEIGHT-1]の重なる部分だけ描く
      if (min < 0) {
        min = 0;
      }
      if (max > SMN_WAVE_HEIGHT - 1) {
        max = SMN_WAVE_HEIGHT - 1;
      }
      for (int y = min; y <= max; y++) {
        int i = SMN_OFFSET * (SMN_WAVE_Y + y) + SMN_WAVE_X / 4 + (x >> 2);
        bb[i] = (byte) (bb[i] & ~(3 << b) | palet << b);
      }
      y0 = y1;
      y1 = y2;
    }  //for x,b
    lastY[0] = y0;
    lastY[1] = y1;
  }  //smnWavePaint1(int[],int[],int,int[])

  //smnPaintKey (x, k, c1111)
  //  鍵を描く
  //  x      x座標
  //  k      鍵の番号。0..11
  //  c1111  4ドット分の色
  //    |    |   1|    |   3|    |    |    |   6|    |   8|    |  10|    |    |
  //    |    |  C#|    |  D#|    |    |    |  F#|    |  G#|    |  A#|    |    |
  //    |   0|   1|   2|   3|   4|   5|   6|   7|   8|   9|  10|  11|  12|  13|
  //   0|WWW |BBBB|BBB |BBBB|BBB |WWW |WWW |BBBB|BBB |BBBB|BBB |BBBB|BBB |WWW |
  //   1|WWW |BBBB|BBB |BBBB|BBB |WWW |WWW |BBBB|BBB |BBBB|BBB |BBBB|BBB |WWW |
  //   2|WWW |BBBB|BBB |BBBB|BBB |WWW |WWW |BBBB|BBB |BBBB|BBB |BBBB|BBB |WWW |
  //   3|WWW |BBBB|BBB |BBBB|BBB |WWW |WWW |BBBB|BBB |BBBB|BBB |BBBB|BBB |WWW |
  //   4|WWW |BBBB|BBB |BBBB|BBB |WWW |WWW |BBBB|BBB |BBBB|BBB |BBBB|BBB |WWW |
  //   5|WWW |    |    |    |    |WWW |WWW |    |    |    |    |    |    |WWW |
  //   6|WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |
  //   7|WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |
  //   8|WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |
  //   9|WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |
  //  10|WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |
  //    |   0|   1|   2|   3|   4|   5|   6|   7|   8|   9|  10|  11|  12|  13|
  //    |   C|    |   D|    |   E|    |   F|    |   G|    |   A|    |   B|    |
  //    |   0|    |   2|    |   4|    |   5|    |   7|    |   9|    |  11|    |
  public static void smnPaintKey (int i, int k, int c1111) {
    int c1110 = c1111 & 0b11111100;
    byte[] bb = smnBitmap;
    switch (k) {
    case 0:  //C
    case 5:  //F
      bb[       SMN_OFFSET *  6 + i] = (
        bb[     SMN_OFFSET *  7 + i] =
        bb[     SMN_OFFSET *  8 + i] =
        bb[     SMN_OFFSET *  9 + i] =
        bb[     SMN_OFFSET * 10 + i] = (byte) c1111);
      bb[                       + i] = (
        bb[     SMN_OFFSET      + i] =
        bb[     SMN_OFFSET *  2 + i] =
        bb[     SMN_OFFSET *  3 + i] =
        bb[     SMN_OFFSET *  4 + i] =
        bb[     SMN_OFFSET *  5 + i] =
        bb[ 1 + SMN_OFFSET *  6 + i] =
        bb[ 1 + SMN_OFFSET *  7 + i] =
        bb[ 1 + SMN_OFFSET *  8 + i] =
        bb[ 1 + SMN_OFFSET *  9 + i] =
        bb[ 1 + SMN_OFFSET * 10 + i] = (byte) c1110);
      break;
    case 1:  //C#
    case 3:  //D#
    case 6:  //F#
    case 8:  //G#
    case 10:  //A#
      bb[                         i] = (
        bb[     SMN_OFFSET      + i] =
        bb[     SMN_OFFSET *  2 + i] =
        bb[     SMN_OFFSET *  3 + i] =
        bb[     SMN_OFFSET *  4 + i] = (byte) c1111);
      bb[   1 +                 + i] = (
        bb[ 1 + SMN_OFFSET      + i] =
        bb[ 1 + SMN_OFFSET *  2 + i] =
        bb[ 1 + SMN_OFFSET *  3 + i] =
        bb[ 1 + SMN_OFFSET *  4 + i] = (byte) c1110);
      break;
    case 2:  //D
    case 7:  //G
    case 9:  //A
      bb[       SMN_OFFSET *  6 + i] = (
        bb[     SMN_OFFSET *  7 + i] =
        bb[     SMN_OFFSET *  8 + i] =
        bb[     SMN_OFFSET *  9 + i] =
        bb[     SMN_OFFSET * 10 + i] = (byte) c1111);
      bb[   1 + SMN_OFFSET *  6 + i] = (
        bb[ 1 + SMN_OFFSET *  7 + i] =
        bb[ 1 + SMN_OFFSET *  8 + i] =
        bb[ 1 + SMN_OFFSET *  9 + i] =
        bb[ 1 + SMN_OFFSET * 10 + i] = (byte) c1110);
      break;
    case 4:  //E
    case 11:  //B
      bb[       SMN_OFFSET *  6 + i] = (
        bb[     SMN_OFFSET *  7 + i] =
        bb[     SMN_OFFSET *  8 + i] =
        bb[     SMN_OFFSET *  9 + i] =
        bb[     SMN_OFFSET * 10 + i] = (byte) c1111);
      bb[   1 +                 + i] = (
        bb[ 1 + SMN_OFFSET      + i] =
        bb[ 1 + SMN_OFFSET *  2 + i] =
        bb[ 1 + SMN_OFFSET *  3 + i] =
        bb[ 1 + SMN_OFFSET *  4 + i] =
        bb[ 1 + SMN_OFFSET *  5 + i] =
        bb[ 1 + SMN_OFFSET *  6 + i] =
        bb[ 1 + SMN_OFFSET *  7 + i] =
        bb[ 1 + SMN_OFFSET *  8 + i] =
        bb[ 1 + SMN_OFFSET *  9 + i] =
        bb[ 1 + SMN_OFFSET * 10 + i] = (byte) c1110);
      break;
    }  //switch k
  }  //smnPaintKey(int,int,int)

  //smnPaintKey2 (x, k, c1111a, c1111b)
  //  鍵を市松模様に塗る
  //  x       x座標
  //  k       鍵の番号。0..11
  //  c1111a  4ドット分の色a
  //  c1111b  4ドット分の色b
  //    |    |   1|    |   3|    |    |    |   6|    |   8|    |  10|    |    |
  //    |    |  C#|    |  D#|    |    |    |  F#|    |  G#|    |  A#|    |    |
  //    |   0|   1|   2|   3|   4|   5|   6|   7|   8|   9|  10|  11|  12|  13|
  //   0|WWW |BBBB|BBB |BBBB|BBB |WWW |WWW |BBBB|BBB |BBBB|BBB |BBBB|BBB |WWW |
  //   1|WWW |BBBB|BBB |BBBB|BBB |WWW |WWW |BBBB|BBB |BBBB|BBB |BBBB|BBB |WWW |
  //   2|WWW |BBBB|BBB |BBBB|BBB |WWW |WWW |BBBB|BBB |BBBB|BBB |BBBB|BBB |WWW |
  //   3|WWW |BBBB|BBB |BBBB|BBB |WWW |WWW |BBBB|BBB |BBBB|BBB |BBBB|BBB |WWW |
  //   4|WWW |BBBB|BBB |BBBB|BBB |WWW |WWW |BBBB|BBB |BBBB|BBB |BBBB|BBB |WWW |
  //   5|WWW |    |    |    |    |WWW |WWW |    |    |    |    |    |    |WWW |
  //   6|WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |
  //   7|WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |
  //   8|WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |
  //   9|WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |
  //  10|WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |WWWW|WWW |
  //    |   0|   1|   2|   3|   4|   5|   6|   7|   8|   9|  10|  11|  12|  13|
  //    |   C|    |   D|    |   E|    |   F|    |   G|    |   A|    |   B|    |
  //    |   0|    |   2|    |   4|    |   5|    |   7|    |   9|    |  11|    |
  public static void smnPaintKey2 (int i, int k, int c1111a, int c1111b) {
    int c1111ab = c1111a & 0b11001100 | c1111b & 0b00110011;
    int c1111ba = c1111b & 0b11001100 | c1111a & 0b00110011;
    int c1110ab = c1111ab & 0b11111100;
    int c1110ba = c1111ba & 0b11111100;
    byte[] bb = smnBitmap;
    switch (k) {
    case 0:  //C
    case 5:  //F
      bb[       SMN_OFFSET *  6 + i] = (
        bb[     SMN_OFFSET *  8 + i] =
        bb[     SMN_OFFSET * 10 + i] = (byte) c1111ab);
      bb[       SMN_OFFSET *  7 + i] = (
        bb[     SMN_OFFSET *  9 + i] = (byte) c1111ba);
      bb[                       + i] = (
        bb[     SMN_OFFSET *  2 + i] =
        bb[     SMN_OFFSET *  4 + i] =
        bb[ 1 + SMN_OFFSET *  6 + i] =
        bb[ 1 + SMN_OFFSET *  8 + i] =
        bb[ 1 + SMN_OFFSET * 10 + i] = (byte) c1110ab);
      bb[       SMN_OFFSET      + i] = (
        bb[     SMN_OFFSET *  3 + i] =
        bb[     SMN_OFFSET *  5 + i] =
        bb[ 1 + SMN_OFFSET *  7 + i] =
        bb[ 1 + SMN_OFFSET *  9 + i] = (byte) c1110ba);
      break;
    case 1:  //C#
    case 3:  //D#
    case 6:  //F#
    case 8:  //G#
    case 10:  //A#
      bb[                         i] = (
        bb[     SMN_OFFSET *  2 + i] =
        bb[     SMN_OFFSET *  4 + i] = (byte) c1111ab);
      bb[       SMN_OFFSET      + i] = (
        bb[     SMN_OFFSET *  3 + i] = (byte) c1111ba);
      bb[   1 +                 + i] = (
        bb[ 1 + SMN_OFFSET *  2 + i] =
        bb[ 1 + SMN_OFFSET *  4 + i] = (byte) c1110ab);
      bb[   1 + SMN_OFFSET      + i] = (
        bb[ 1 + SMN_OFFSET *  3 + i] = (byte) c1110ba);
      break;
    case 2:  //D
    case 7:  //G
    case 9:  //A
      bb[       SMN_OFFSET *  6 + i] = (
        bb[     SMN_OFFSET *  8 + i] =
        bb[     SMN_OFFSET * 10 + i] = (byte) c1111ab);
      bb[       SMN_OFFSET *  7 + i] = (
        bb[     SMN_OFFSET *  9 + i] = (byte) c1111ba);
      bb[   1 + SMN_OFFSET *  6 + i] = (
        bb[ 1 + SMN_OFFSET *  8 + i] =
        bb[ 1 + SMN_OFFSET * 10 + i] = (byte) c1110ab);
      bb[   1 + SMN_OFFSET *  7 + i] = (
        bb[ 1 + SMN_OFFSET *  9 + i] = (byte) c1110ba);
      break;
    case 4:  //E
    case 11:  //B
      bb[       SMN_OFFSET *  6 + i] = (
        bb[     SMN_OFFSET *  8 + i] =
        bb[     SMN_OFFSET * 10 + i] = (byte) c1111ab);
      bb[       SMN_OFFSET *  7 + i] = (
        bb[     SMN_OFFSET *  9 + i] = (byte) c1111ba);
      bb[   1 +                 + i] = (
        bb[ 1 + SMN_OFFSET *  2 + i] =
        bb[ 1 + SMN_OFFSET *  4 + i] =
        bb[ 1 + SMN_OFFSET *  6 + i] =
        bb[ 1 + SMN_OFFSET *  8 + i] =
        bb[ 1 + SMN_OFFSET * 10 + i] = (byte) c1110ab);
      bb[   1 + SMN_OFFSET      + i] = (
        bb[ 1 + SMN_OFFSET *  3 + i] =
        bb[ 1 + SMN_OFFSET *  5 + i] =
        bb[ 1 + SMN_OFFSET *  7 + i] =
        bb[ 1 + SMN_OFFSET *  9 + i] = (byte) c1110ba);
      break;
    }  //switch k
  }  //smnPaintKey2(int,int,int,int)

  //smnDrawIcon1 (x, y, c)
  //  パレットコード1でアイコンを描く
  public static void smnDrawIcon1 (int x, int y, int c) {
    byte[] bb = smnBitmap;
    x += SMN_OFFSET * y;
    c *= 18;
    bb[                    + x] = SMN_ICON_1[c     ];
    bb[1                   + x] = SMN_ICON_1[c +  1];
    bb[    SMN_OFFSET      + x] = SMN_ICON_1[c +  2];
    bb[1 + SMN_OFFSET      + x] = SMN_ICON_1[c +  3];
    bb[    SMN_OFFSET *  2 + x] = SMN_ICON_1[c +  4];
    bb[1 + SMN_OFFSET *  2 + x] = SMN_ICON_1[c +  5];
    bb[    SMN_OFFSET *  3 + x] = SMN_ICON_1[c +  6];
    bb[1 + SMN_OFFSET *  3 + x] = SMN_ICON_1[c +  7];
    bb[    SMN_OFFSET *  4 + x] = SMN_ICON_1[c +  8];
    bb[1 + SMN_OFFSET *  4 + x] = SMN_ICON_1[c +  9];
    bb[    SMN_OFFSET *  5 + x] = SMN_ICON_1[c + 10];
    bb[1 + SMN_OFFSET *  5 + x] = SMN_ICON_1[c + 11];
    bb[    SMN_OFFSET *  6 + x] = SMN_ICON_1[c + 12];
    bb[1 + SMN_OFFSET *  6 + x] = SMN_ICON_1[c + 13];
    bb[    SMN_OFFSET *  7 + x] = SMN_ICON_1[c + 14];
    bb[1 + SMN_OFFSET *  7 + x] = SMN_ICON_1[c + 15];
    bb[    SMN_OFFSET *  8 + x] = SMN_ICON_1[c + 16];
    bb[1 + SMN_OFFSET *  8 + x] = SMN_ICON_1[c + 17];
  }  //smnDrawIcon1(int,int,int)

  //smnDrawIcon3 (x, y, c)
  //  パレットコード3でアイコンを描く
  public static void smnDrawIcon3 (int x, int y, int c) {
    byte[] bb = smnBitmap;
    x += SMN_OFFSET * y;
    c *= 18;
    bb[                    + x] = SMN_ICON_3[c     ];
    bb[1                   + x] = SMN_ICON_3[c +  1];
    bb[    SMN_OFFSET      + x] = SMN_ICON_3[c +  2];
    bb[1 + SMN_OFFSET      + x] = SMN_ICON_3[c +  3];
    bb[    SMN_OFFSET *  2 + x] = SMN_ICON_3[c +  4];
    bb[1 + SMN_OFFSET *  2 + x] = SMN_ICON_3[c +  5];
    bb[    SMN_OFFSET *  3 + x] = SMN_ICON_3[c +  6];
    bb[1 + SMN_OFFSET *  3 + x] = SMN_ICON_3[c +  7];
    bb[    SMN_OFFSET *  4 + x] = SMN_ICON_3[c +  8];
    bb[1 + SMN_OFFSET *  4 + x] = SMN_ICON_3[c +  9];
    bb[    SMN_OFFSET *  5 + x] = SMN_ICON_3[c + 10];
    bb[1 + SMN_OFFSET *  5 + x] = SMN_ICON_3[c + 11];
    bb[    SMN_OFFSET *  6 + x] = SMN_ICON_3[c + 12];
    bb[1 + SMN_OFFSET *  6 + x] = SMN_ICON_3[c + 13];
    bb[    SMN_OFFSET *  7 + x] = SMN_ICON_3[c + 14];
    bb[1 + SMN_OFFSET *  7 + x] = SMN_ICON_3[c + 15];
    bb[    SMN_OFFSET *  8 + x] = SMN_ICON_3[c + 16];
    bb[1 + SMN_OFFSET *  8 + x] = SMN_ICON_3[c + 17];
  }  //smnDrawIcon3(int,int,int)

  //smnDrawChar1 (x, y, c)
  //  パレットコード1で文字を描く
  public static void smnDrawChar1 (int x, int y, int c) {
    byte[] bb = smnBitmap;
    x += SMN_OFFSET * y;
    c *= 5;
    bb[x                 ] = SMN_FONT_1[c    ];
    bb[x + SMN_OFFSET    ] = SMN_FONT_1[c + 1];
    bb[x + SMN_OFFSET * 2] = SMN_FONT_1[c + 2];
    bb[x + SMN_OFFSET * 3] = SMN_FONT_1[c + 3];
    bb[x + SMN_OFFSET * 4] = SMN_FONT_1[c + 4];
  }  //smnDrawChar1(int,int,int)

  //smnDrawChar2 (x, y, c)
  //  パレットコード2で文字を描く
  public static void smnDrawChar2 (int x, int y, int c) {
    byte[] bb = smnBitmap;
    x += SMN_OFFSET * y;
    c *= 5;
    bb[x                 ] = SMN_FONT_2[c    ];
    bb[x + SMN_OFFSET    ] = SMN_FONT_2[c + 1];
    bb[x + SMN_OFFSET * 2] = SMN_FONT_2[c + 2];
    bb[x + SMN_OFFSET * 3] = SMN_FONT_2[c + 3];
    bb[x + SMN_OFFSET * 4] = SMN_FONT_2[c + 4];
  }  //smnDrawChar2(int,int,int)

  //smnDrawChar3 (x, y, c)
  //  パレットコード3で文字を描く
  public static void smnDrawChar3 (int x, int y, int c) {
    byte[] bb = smnBitmap;
    x += SMN_OFFSET * y;
    c *= 5;
    bb[x                 ] = SMN_FONT_3[c    ];
    bb[x + SMN_OFFSET    ] = SMN_FONT_3[c + 1];
    bb[x + SMN_OFFSET * 2] = SMN_FONT_3[c + 2];
    bb[x + SMN_OFFSET * 3] = SMN_FONT_3[c + 3];
    bb[x + SMN_OFFSET * 4] = SMN_FONT_3[c + 4];
  }  //smnDrawChar3(int,int,int)

  //smnDrawString1 (x, y, s)
  //  パレットコード1で文字列を描く
  public static void smnDrawString1 (int x, int y, String s) {
    smnDrawString1 (x, y, s.toCharArray ());
  }  //smnDrawString1(int,int,String)
  public static void smnDrawString1 (int x, int y, char[] s) {
    byte[] bb = smnBitmap;
    x += SMN_OFFSET * y;
    for (char c : s) {
      c *= 5;
      bb[x                 ] = SMN_FONT_1[c    ];
      bb[x + SMN_OFFSET    ] = SMN_FONT_1[c + 1];
      bb[x + SMN_OFFSET * 2] = SMN_FONT_1[c + 2];
      bb[x + SMN_OFFSET * 3] = SMN_FONT_1[c + 3];
      bb[x + SMN_OFFSET * 4] = SMN_FONT_1[c + 4];
      x++;
    }
  }  //smnDrawString1(int,int,char[])

  //smnDrawString2 (x, y, s)
  //  パレットコード2で文字列を描く
  public static void smnDrawString2 (int x, int y, String s) {
    smnDrawString2 (x, y, s.toCharArray ());
  }  //smnDrawString2(int,int,String)
  public static void smnDrawString2 (int x, int y, char[] s) {
    byte[] bb = smnBitmap;
    x += SMN_OFFSET * y;
    for (char c : s) {
      c *= 5;
      bb[x                 ] = SMN_FONT_2[c    ];
      bb[x + SMN_OFFSET    ] = SMN_FONT_2[c + 1];
      bb[x + SMN_OFFSET * 2] = SMN_FONT_2[c + 2];
      bb[x + SMN_OFFSET * 3] = SMN_FONT_2[c + 3];
      bb[x + SMN_OFFSET * 4] = SMN_FONT_2[c + 4];
      x++;
    }
  }  //smnDrawString2(int,int,char[])

  //smnDrawString3 (x, y, s)
  //  パレットコード3で文字列を描く
  public static void smnDrawString3 (int x, int y, String s) {
    smnDrawString3 (x, y, s.toCharArray ());
  }  //smnDrawString3(int,int,String)
  public static void smnDrawString3 (int x, int y, char[] s) {
    byte[] bb = smnBitmap;
    x += SMN_OFFSET * y;
    for (char c : s) {
      c *= 5;
      bb[x                 ] = SMN_FONT_3[c    ];
      bb[x + SMN_OFFSET    ] = SMN_FONT_3[c + 1];
      bb[x + SMN_OFFSET * 2] = SMN_FONT_3[c + 2];
      bb[x + SMN_OFFSET * 3] = SMN_FONT_3[c + 3];
      bb[x + SMN_OFFSET * 4] = SMN_FONT_3[c + 4];
      x++;
    }
  }  //smnDrawString3(int,int,char[])

  //smnDrawSlider (slider, pattern)
  //  スライダを描く
  //  slider[0]  x0。4の倍数
  //  slider[1]  y0
  //  slider[2]  max。4の倍数
  //  slider[3]  palet
  //  slider[4]  value。0<=value<=max
  //  width=max+4
  //  height=5
  public static void smnDrawSlider (int[] slider, int[] pattern) {
    byte[] bb = smnBitmap;
    int x0 = slider[0];
    int y0 = slider[1];
    int max = slider[2];
    int palet = slider[3];
    int value = slider[4];
    int width = max + 4;
    for (int i = (x0 >> 2) + SMN_OFFSET * y0, l = i + (width >> 2); i < l; i++) {
      bb[  i                 ] = (
        bb[i + SMN_OFFSET * 1] =
        bb[i + SMN_OFFSET * 3] =
        bb[i + SMN_OFFSET * 4] = SMN_BLACK);
      bb[  i + SMN_OFFSET * 2] = (byte) palet;
    }
    smnDrawPattern (x0 + value, y0 + 2, pattern);
  }  //smnDrawSlider(int[],int[])

  //smnDrawPattern (x0, y0, pattern)
  //  パターンを描く
  //  pattern={n0,palet,u0,v0,u1,v1,...,n1,palet,...}
  //           <-----------n0---------->
  public static void smnDrawPattern (int x0, int y0, int[] pattern) {
    byte[] bb = smnBitmap;
    for (int k = 0; k < pattern.length; ) {
      int limit = k + pattern[k];
      int palet = pattern[k + 1] & 3;
      for (k += 2; k < limit; k += 2) {
        int x = x0 + pattern[k];
        int y = y0 + pattern[k + 1];
        int b = (~x & 3) << 1;  //0→6,1→4,2→2,3→0
        int i = SMN_OFFSET * y + (x >> 2);
        bb[i] = (byte) (bb[i] & ~(3 << b) | palet << b);
      }
    }
  }  //smnDrawPattern(int,int,int[])

}  //class SoundMonitor



