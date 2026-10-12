//========================================================================================
//  OPMLog.java
//    en:OPM log
//    ja:OPMログ
//  Copyright (C) 2003-2026 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.awt.*;  //Graphics
import java.awt.event.*;  //ActionEvent,ActionListener
import java.awt.geom.*;  //Line2D
import java.awt.image.*;  //BufferedImage
import java.io.*;  //File
import java.util.*;  //Arrays
import javax.sound.sampled.*;  //AudioFormat,AudioSystem,SourceDataLine
import javax.swing.*;  //JPanel
import javax.swing.border.*;  //EmptyBorder
import javax.swing.event.*;  //ChangeEvent,ChangeListener

public class OPMLog {

  public static final boolean OLG_ON = true;

  //log2(3.58/4.00)
  public static final double LOG2_358_400 = -0.16004041251046827164899520744140640962;

  public static final int OLG_BUFFER_SIZE = 1024 * 1024;
  public static final long OLG_UNIT = 1000000L;  //経過時刻の単位の逆数。1000000=1us
  public static final int[] olgBuffer = new int[OLG_BUFFER_SIZE];  //バッファ。[0]=経過時刻(OLG_UNIT),[1]=アドレス<<8|データ or 0x10000=CSMKON or -1=END

  //記録
  public static final int OLG_LIMIT_S = 900;  //最大記録時間(s)
  public static boolean olgRecording;  //記録中
  public static int olgLength;  //使用したバッファの長さ。エンドコードを含む
  public static long olgStartTimePS;  //記録開始時刻(TMR_FREQ)
  public static int olgCounter;  //カウンタ

  //ページ
  public static final int[] OLG_US_PER_PX = new int[] {
    1,  //1us/px 1ms/1000px
    2,  //2us/px 2ms/1000px
    5,  //5us/px 5ms/1000px
    10,  //10us/px 10ms/1000px
    20,  //20us/px 20ms/1000px
    50,  //50us/px 50ms/1000px
    100,  //100us/px 100ms/1000px
    200,  //200us/px 200ms/1000px
    500,  //500us/px 500ms/1000px
    1000,  //1ms/px 1s/1000px
    2000,  //2ms/px 2s/1000px
    5000,  //5ms/px 5s/1000px
    10000,  //10ms/px 10s/1000px
    20000,  //20ms/px 20s/1000px
    50000,  //50ms/px 50s/1000px
    100000,  //100ms/px 100s/1000px
  };
  public static final int OLG_RANGE_COUNT = OLG_US_PER_PX.length;
  public static int olgRangeIndex;  //OLG_RANGE_TEXTのインデックス
  public static int olgUsPerPx;  //1pxあたりの時間
  public static int olgRangeUs;  //表示範囲の時間
  public static int olgStartUs;  //表示範囲の開始時刻
  public static int olgEndUs;  //表示範囲の終了時刻
  public static int olgPxToUs (int px) {
    return olgStartUs + olgUsPerPx * px;
  }
  public static int olgUsToPx (int us) {
    return (us - olgStartUs) / olgUsPerPx;
  }
  public static final boolean[] olgChannelMask = new boolean[8];

  //キャレット
  public static int olgCaretPx;  //キャレットのX座標
  public static int olgCaretUs;  //キャレットの時刻

  //マスク
  public static int olgMaskLeftUs;  //左側マスクの終了時刻。-1=なし
  public static int olgMaskRightUs;  //右側マスクの開始時刻。-1=なし

  public static SimpleTask olgForcedTerminationTask;  //強制終了タスク
  public static SimpleTask olgMakeWaveTask;  //波形生成タスク

  public static File olgLastMMLFile;
  public static javax.swing.filechooser.FileFilter olgMMLFilter;
  public static File olgLastFile;
  public static javax.swing.filechooser.FileFilter olgFilter;

  //ストローク
  public static BasicStroke olgSolidStroke;
  public static BasicStroke olgDashStroke;
  public static BasicStroke olgWaveStroke;

  //ペイント
  public static TexturePaint olgMaskPaint;

  //楽譜
  public static final int OLG_SCORE_WIDTH = 1200;  //描画領域の幅
  public static final int OLG_SCORE_HEIGHT = 388;  //描画領域の高さ。64*12/6208*485=60。64*12/6208*388=48。12で割り切れてちょうどよい
  public static final int OLG_SCORE_H_MARGIN = 10;  //左右のマージン
  public static final int OLG_SCORE_V_MARGIN = 6;  //上下のマージン
  public static final int OLG_SCORE_IMAGE_WIDTH = OLG_SCORE_H_MARGIN + OLG_SCORE_WIDTH + OLG_SCORE_H_MARGIN;  //イメージの幅
  public static final int OLG_WAVE_RADIUS = 48;
  public static final int OLG_WAVE_Y1 = OLG_SCORE_V_MARGIN + OLG_SCORE_HEIGHT;
  public static final int OLG_WAVE_Y0 = OLG_WAVE_Y1 + OLG_WAVE_RADIUS;
  public static final int OLG_WAVE_YM1 = OLG_WAVE_Y0 + OLG_WAVE_RADIUS;
  public static final int OLG_SCORE_IMAGE_HEIGHT = OLG_WAVE_YM1 + OLG_SCORE_V_MARGIN;  //イメージの高さ
  public static final int OLG_KON1ST_HEIGHT = 9;  //バーのキーオンしたときの高さ(奇数)
  public static final int OLG_KON2ND_HEIGHT = 7;  //バーのキーオンしたままKC,KFが変更されたときの高さ(奇数)
  public static final int OLG_BAR_HEIGHT = 3;  //バーの高さ(奇数)
  public static final Color[] OLG_KON_COLOR = new Color[] {
    Color.getHSBColor (0.000F, 0.5F, 1.0F),
    Color.getHSBColor (0.125F, 0.5F, 1.0F),
    Color.getHSBColor (0.250F, 0.5F, 1.0F),
    Color.getHSBColor (0.375F, 0.5F, 1.0F),
    Color.getHSBColor (0.500F, 0.5F, 1.0F),
    Color.getHSBColor (0.625F, 0.5F, 1.0F),
    Color.getHSBColor (0.750F, 0.5F, 1.0F),
    Color.getHSBColor (0.875F, 0.5F, 1.0F),
  };  //チャンネル→バーの左端の色
  public static final Color[] OLG_BAR_COLOR = new Color[] {
    Color.getHSBColor (0.000F, 0.5F, 0.5F),
    Color.getHSBColor (0.125F, 0.5F, 0.5F),
    Color.getHSBColor (0.250F, 0.5F, 0.5F),
    Color.getHSBColor (0.375F, 0.5F, 0.5F),
    Color.getHSBColor (0.500F, 0.5F, 0.5F),
    Color.getHSBColor (0.625F, 0.5F, 0.5F),
    Color.getHSBColor (0.750F, 0.5F, 0.5F),
    Color.getHSBColor (0.875F, 0.5F, 0.5F),
  };  //チャンネル→バーの色
  public static BufferedImage olgCanvasImage;
  public static int[] olgCanvasBitmap;
  public static ScrollCanvas olgCanvas;
  public static int olgScaleShift;

  //音色
  public static final char[] OLG_TONE_BASE = (
    //         1111111111222222222233333
    //1234567890123456789012345678901234
    "   FC SL WA SY SP PD AD PS AS PN   " +
    "   -- -- -- -- -- -- -- -- -- -- --" +
    "   AR 1R 2R RR 1L TL KS ML T1 T2 AE" +
    "M1 -- -- -- -- -- -- -- -- -- -- --" +
    "C1 -- -- -- -- -- -- -- -- -- -- --" +
    "M2 -- -- -- -- -- -- -- -- -- -- --" +
    "C2 -- -- -- -- -- -- -- -- -- -- --" +
    "   KC KF                           " +
    "   -- --                           ").toCharArray ();
  public static final int OLG_TONE_CHAR_WIDTH = 6;  //文字の幅
  public static final int OLG_TONE_LINE_HEIGHT = 10;  //行の高さ
  public static final int OLG_TONE_COLS = 35;  //桁数
  public static final int OLG_TONE_ROWS = 9;  //行数
  public static final int OLG_TONE_H_SPACE = 6;  //水平方向の間隔
  public static final int OLG_TONE_V_SPACE = 10;  //垂直方向の間隔
  public static final int OLG_TONE_WIDTH = (OLG_TONE_CHAR_WIDTH * OLG_TONE_COLS + OLG_TONE_H_SPACE) * 4 - OLG_TONE_H_SPACE;  //描画領域の幅
  public static final int OLG_TONE_HEIGHT = (OLG_TONE_LINE_HEIGHT * OLG_TONE_ROWS + OLG_TONE_V_SPACE) * 2 - OLG_TONE_V_SPACE;  //描画領域の高さ
  public static final int OLG_TONE_H_MARGIN = 6;  //左右のマージン
  public static final int OLG_TONE_V_MARGIN = 10;  //上下のマージン
  public static final int OLG_TONE_IMAGE_WIDTH = OLG_TONE_H_MARGIN + OLG_TONE_WIDTH + OLG_TONE_H_MARGIN;  //イメージの幅
  public static final int OLG_TONE_IMAGE_HEIGHT = OLG_TONE_V_MARGIN + OLG_TONE_HEIGHT + OLG_TONE_V_MARGIN;  //イメージの高さ
  public static BufferedImage olgToneImage;
  public static int[] olgToneBitmap;
  public static JPanel olgTonePanel;

  //ダンプ
  //  OLG_SCORE_IMAGE_WIDTH=1220
  //  OLG_SCORE_IMAGE_HEIGHT=400
  //  OLG_TONE_IMAGE_WIDTH=870
  //  OLG_TONE_IMAGE_HEIGHT=210
  public static final int OLG_DUMP_TEXT_AREA_WIDTH = 340;
  public static final int OLG_DUMP_TEXT_AREA_HEIGHT = 180;
  public static ScrollTextArea olgDumpTextArea;

  //メニュー
  public static JRadioButtonMenuItem[] olgScaleMenuItem;

  //ウインドウ
  public static JFrame olgFrame;
  //
  public static JButton olgStartButton;
  public static JButton olgEndButton;
  //
  public static JButton olgPlusButton;
  public static JButton olgMinusButton;

  //再生
  public static final int OPM_CHANNELS = 2;
  public static YM2151 olgYM2151;
  public static int[] opmBuffer;

  //  初期化
  public static void olgInit () {
    //olgBuffer = new int[OLG_BUFFER_SIZE];
    //
    olgRecording = false;
    olgBuffer[0] = 0;
    olgBuffer[1] = -1;
    olgLength = 2;
    olgStartTimePS = 0L;
    olgCounter = 0;
    //
    olgRangeIndex = OLG_RANGE_COUNT - 1;
    olgUsPerPx = OLG_US_PER_PX[olgRangeIndex];
    olgRangeUs = olgUsPerPx * OLG_SCORE_WIDTH;
    olgStartUs = 0;
    olgEndUs = olgRangeUs;
    //olgChannelMask = new boolean[8];
    Arrays.fill (olgChannelMask, true);
    //
    olgCaretPx = 0;
    olgCaretUs = 0;
    //
    olgMaskLeftUs = -1;
    olgMaskRightUs = -1;
    //
    olgForcedTerminationTask = null;
    olgMakeWaveTask = null;
    //
    olgLastFile = new File (System.getProperty ("user.dir") + File.separator + "opmlog.dat");
    olgFilter = new javax.swing.filechooser.FileFilter () {
      @Override public boolean accept (File file) {
        String name = file.getName ();
        String lowerName = name.toLowerCase ();
        return (file.isDirectory () ||
                (file.isFile () &&
                 lowerName.endsWith (".dat")));
      }
      @Override public String getDescription () {
        return (Multilingual.mlnJapanese ?
                "データファイル (*.dat)" :
                "Data files (*.dat)");
      }
    };
    //
    olgLastMMLFile = new File (System.getProperty ("user.dir") + File.separator + "a.opm");
    olgMMLFilter = new javax.swing.filechooser.FileFilter () {
      @Override public boolean accept (File file) {
        String name = file.getName ();
        String lowerName = name.toLowerCase ();
        return (file.isDirectory () ||
                (file.isFile () &&
                 (lowerName.endsWith (".opm") ||
                  lowerName.endsWith (".zms"))));
      }
      @Override public String getDescription () {
        return (Multilingual.mlnJapanese ?
                "MML ファイル (*.opm *.zms)" :
                "MML files (*.opm *.zms)");
      }
    };

    olgYM2151 = new YM2151 ();
    opmBuffer = null;
    olgYM2151.setListener (new YM2151.Listener () {
      @Override public void timerA (int clocks) {
      }
      @Override public void timerB (int clocks) {
      }
      @Override public void busy (int clocks) {
      }
      @Override public boolean isBusy () {
        return false;
      }
      @Override public void irq (boolean asserted) {
      }
      @Override public void control (int data) {
      }
      @Override public void written (int pointer, int address, int data) {
      }
    });
    olgYM2151.reset ();

  }

  //  後始末
  public static void olgTini () {
    if (olgForcedTerminationTask != null) {
      olgForcedTerminationTask.stop ();
    }
    if (olgMakeWaveTask != null) {
      olgMakeWaveTask.stop ();
    }
    if (playTask != null) {
      playTask.stop ();
    }
  }

  public static void olgStart () {
    if (RestorableFrame.rfmGetOpened (Settings.SGS_OLG_FRAME_KEY)) {
      olgOpen ();
    }
  }

  public static void olgOpen () {
    if (olgFrame == null) {
      olgMakeFrame ();
    }
    XEiJ.pnlExitFullScreen (false);
    olgFrame.setVisible (true);
  }

  public static void olgMakeFrame () {

    //ストローク
    olgSolidStroke = new BasicStroke (1.0F, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0F);
    olgDashStroke = new BasicStroke (1.0F, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0F, new float[] { 1.0F, 1.0F }, 0.0F);
    olgWaveStroke = new BasicStroke (1.0F, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0F);

    //ペイント
    {
      BufferedImage image = new BufferedImage (4, 4, BufferedImage.TYPE_INT_ARGB);
      int[] bitmap = ((DataBufferInt) image.getRaster ().getDataBuffer ()).getData ();
      Arrays.fill (bitmap, 0x00000000);
      bitmap[3] = 0x80808080;
      bitmap[6] = 0x80808080;
      bitmap[9] = 0x80808080;
      bitmap[12] = 0x80808080;
      olgMaskPaint = new TexturePaint (image, new Rectangle2D.Float (0.0F, 0.0F, 4.0F, 4.0F));
    }

    //楽譜キャンバス
    olgCanvasImage = new BufferedImage (OLG_SCORE_IMAGE_WIDTH, OLG_SCORE_IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
    olgCanvasBitmap = ((DataBufferInt) olgCanvasImage.getRaster ().getDataBuffer ()).getData ();
    olgCanvas = new ScrollCanvas (olgCanvasImage);
    olgCanvas.setMargin (10, 10);
    olgCanvas.setMatColor (new Color (LnF.lnfRGB[4]));
    olgScaleShift = 0;
    ComponentFactory.setPreferredSize (olgCanvas,
                                       OLG_SCORE_IMAGE_WIDTH + 10 + 20 + 20,
                                       OLG_SCORE_IMAGE_HEIGHT + 10 + 20 + 30);  //マージンとスクロールバーとタイトルボーダー

    //アクションリスナー
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        String command = ae.getActionCommand ();
        switch (command) {
        case "Load MML":
          olgLoadMML ();
          break;
        case "Load Log":
          olgLoadLog ();
          break;
        case "Save Log":
          olgSaveLog ();
          break;
        case "Quit":
          olgFrame.setVisible (false);
          break;
          //
        case "Adjust":
          olgAdjust ();
          break;
          //
        case "6.25%":
          if (olgScaleShift != -4) {
            olgCanvas.setScaleShift (-4);
          }
          break;
        case "12.5%":
          if (olgScaleShift != -3) {
            olgCanvas.setScaleShift (-3);
          }
          break;
        case "25%":
          if (olgScaleShift != -2) {
            olgCanvas.setScaleShift (-2);
          }
          break;
        case "50%":
          if (olgScaleShift != -1) {
            olgCanvas.setScaleShift (-1);
          }
          break;
        case "100%":
          if (olgScaleShift != 0) {
            olgCanvas.setScaleShift (0);
          }
          break;
        case "200%":
          if (olgScaleShift != 1) {
            olgCanvas.setScaleShift (1);
          }
          break;
        case "400%":
          if (olgScaleShift != 2) {
            olgCanvas.setScaleShift (2);
          }
          break;
        case "800%":
          if (olgScaleShift != 3) {
            olgCanvas.setScaleShift (3);
          }
          break;
        case "1600%":
          if (olgScaleShift != 4) {
            olgCanvas.setScaleShift (4);
          }
          break;
          //
        case "●":
          olgRecordStart ();
          break;
        case "■":
          olgRecordEnd ();
          break;
          //
        case "＋":
          olgRangeIndex = Math.max (0, olgRangeIndex - 1);
          ComponentFactory.setEnabled (olgPlusButton, 0 < olgRangeIndex);
          ComponentFactory.setEnabled (olgMinusButton, olgRangeIndex < OLG_RANGE_COUNT - 1);
          olgUsPerPx = OLG_US_PER_PX[olgRangeIndex];
          olgRangeUs = olgUsPerPx * OLG_SCORE_WIDTH;
          olgCaretUs = (olgCaretUs / olgUsPerPx) * olgUsPerPx;
          olgStartUs = Math.max (0, olgCaretUs - olgUsPerPx * olgCaretPx);
          olgCaretPx = (olgCaretUs - olgStartUs) / olgUsPerPx;
          olgEndUs = olgStartUs + olgRangeUs;
          olgPaint ();
          break;
        case "－":
          olgRangeIndex = Math.min (OLG_RANGE_COUNT - 1, olgRangeIndex + 1);
          ComponentFactory.setEnabled (olgPlusButton, 0 < olgRangeIndex);
          ComponentFactory.setEnabled (olgMinusButton, olgRangeIndex < OLG_RANGE_COUNT - 1);
          olgUsPerPx = OLG_US_PER_PX[olgRangeIndex];
          olgRangeUs = olgUsPerPx * OLG_SCORE_WIDTH;
          olgCaretUs = (olgCaretUs / olgUsPerPx) * olgUsPerPx;
          olgStartUs = Math.max (0, olgCaretUs - olgUsPerPx * olgCaretPx);
          olgCaretPx = (olgCaretUs - olgStartUs) / olgUsPerPx;
          olgEndUs = olgStartUs + olgRangeUs;
          olgPaint ();
          break;
        case "丨＜":
          olgCaretUs = 0;
          olgStartUs = 0;
          olgCaretPx = 0;
          olgEndUs = olgStartUs + olgRangeUs;
          olgPaint ();
          break;
        case "≪≪":
          olgMoveCaret (-400);
          break;
        case "≪":
          olgMoveCaret (-20);
          break;
        case "＜":
          olgMoveCaret (-1);
          break;
        case "＞":
          olgMoveCaret (1);
          break;
        case "≫":
          olgMoveCaret (20);
          break;
        case "≫≫":
          olgMoveCaret (400);
          break;
        case "＞丨":
          olgCaretUs = (Math.max (0, olgBuffer[olgLength - 2] - 1) / olgUsPerPx) * olgUsPerPx;
          olgStartUs = Math.max (0, olgCaretUs - olgUsPerPx * olgCaretPx);
          olgCaretPx = (olgCaretUs - olgStartUs) / olgUsPerPx;
          olgEndUs = olgStartUs + olgRangeUs;
          olgPaint ();
          break;
          //
        case "0":
          olgChannelMask[0] = ((JCheckBox) source).isSelected ();
          olgMakeWaveData ();
          olgPaint ();
          break;
        case "1":
          olgChannelMask[1] = ((JCheckBox) source).isSelected ();
          olgMakeWaveData ();
          olgPaint ();
          break;
        case "2":
          olgChannelMask[2] = ((JCheckBox) source).isSelected ();
          olgMakeWaveData ();
          olgPaint ();
          break;
        case "3":
          olgChannelMask[3] = ((JCheckBox) source).isSelected ();
          olgMakeWaveData ();
          olgPaint ();
          break;
        case "4":
          olgChannelMask[4] = ((JCheckBox) source).isSelected ();
          olgMakeWaveData ();
          olgPaint ();
          break;
        case "5":
          olgChannelMask[5] = ((JCheckBox) source).isSelected ();
          olgMakeWaveData ();
          olgPaint ();
          break;
        case "6":
          olgChannelMask[6] = ((JCheckBox) source).isSelected ();
          olgMakeWaveData ();
          olgPaint ();
          break;
        case "7":
          olgChannelMask[7] = ((JCheckBox) source).isSelected ();
          olgMakeWaveData ();
          olgPaint ();
          break;
          //
        case "From":
          if (olgMaskRightUs == -1 || olgCaretUs < olgMaskRightUs) {
            olgMaskLeftUs = olgCaretUs;
            olgPaint ();
          }
          break;
        case "To":
          if (olgMaskLeftUs == -1 || olgMaskLeftUs < olgCaretUs) {
            olgMaskRightUs = olgCaretUs;
            olgPaint ();
          }
          break;
        case "All":
          olgMaskLeftUs = -1;
          olgMaskRightUs = -1;
          olgPaint ();
          break;
          //
        case "\u25b7":
          olgPlayStart ();
          break;
        case "□":
          olgPlayEnd ();
          break;
        }
      }
    };

    //メニューバー
    ButtonGroup zoomGroup = new ButtonGroup ();
    olgScaleMenuItem = new JRadioButtonMenuItem[9];
    JMenuBar menuBar = ComponentFactory.createMenuBar (
      //ファイルメニュー
      Multilingual.mlnText (
        ComponentFactory.createMenu (
          "File", 'F',
          Multilingual.mlnText (ComponentFactory.createMenuItem ("Load MML", 'M', listener), "ja", "ロード MML"),
          ComponentFactory.createHorizontalSeparator (),
          Multilingual.mlnText (ComponentFactory.createMenuItem ("Load Log", 'L', listener), "ja", "ロード ログ"),
          Multilingual.mlnText (ComponentFactory.createMenuItem ("Save Log", 'S', listener), "ja", "セーブ ログ"),
          ComponentFactory.createHorizontalSeparator (),
          Multilingual.mlnText (ComponentFactory.createMenuItem ("Quit", 'Q', listener), "ja", "終了")
          ),
        "ja", "ファイル"),
      //編集メニュー
      Multilingual.mlnText (
        ComponentFactory.createMenu (
          "Edit", 'E',
          Multilingual.mlnText (ComponentFactory.createMenuItem ("Adjust", 'A', listener), "ja", "アジャスト")
          ),
        "ja", "編集"),
      //表示メニュー
      Multilingual.mlnText (
        ComponentFactory.createMenu (
          "Display", 'D',
          olgScaleMenuItem[0] = ComponentFactory.createRadioButtonMenuItem (zoomGroup, olgScaleShift == -4, "6.25%", '1', listener),
          olgScaleMenuItem[1] = ComponentFactory.createRadioButtonMenuItem (zoomGroup, olgScaleShift == -3, "12.5%", '2', listener),
          olgScaleMenuItem[2] = ComponentFactory.createRadioButtonMenuItem (zoomGroup, olgScaleShift == -2, "25%", '3', listener),
          olgScaleMenuItem[3] = ComponentFactory.createRadioButtonMenuItem (zoomGroup, olgScaleShift == -1, "50%", '4', listener),
          olgScaleMenuItem[4] = ComponentFactory.createRadioButtonMenuItem (zoomGroup, olgScaleShift ==  0, "100%", '5', listener),
          olgScaleMenuItem[5] = ComponentFactory.createRadioButtonMenuItem (zoomGroup, olgScaleShift ==  1, "200%", '6', listener),
          olgScaleMenuItem[6] = ComponentFactory.createRadioButtonMenuItem (zoomGroup, olgScaleShift ==  2, "400%", '7', listener),
          olgScaleMenuItem[7] = ComponentFactory.createRadioButtonMenuItem (zoomGroup, olgScaleShift ==  3, "800%", '8', listener),
          olgScaleMenuItem[8] = ComponentFactory.createRadioButtonMenuItem (zoomGroup, olgScaleShift ==  4, "1600%", '9', listener)
          ),
        "ja", "表示")
      );

    //スケールシフトリスナー
    olgCanvas.addScaleShiftListener (new ScrollCanvas.ScaleShiftListener () {
      @Override public void scaleShiftChanged (int scaleShift) {
        if (-4 <= scaleShift && scaleShift <= 4) {
          olgScaleShift = scaleShift;
          olgScaleMenuItem[4 + scaleShift].setSelected (true);
        }
      }
    });

    //マウスリスナー
    olgCanvas.addMouseListener (new MouseAdapter () {
      @Override public void mouseClicked (MouseEvent me) {
        MouseEvent2D me2D = (MouseEvent2D) me;
        int x = (int) me2D.getX2D ();
        int y = (int) me2D.getY2D ();
        if (OLG_SCORE_H_MARGIN <= x && x < OLG_SCORE_H_MARGIN + OLG_SCORE_WIDTH &&
            OLG_SCORE_V_MARGIN <= y && y < OLG_WAVE_YM1) {
          x -= OLG_SCORE_H_MARGIN;
          y -= OLG_SCORE_V_MARGIN;
          //クリックされた位置にキャレットを動かす
          olgCaretUs = Math.min ((Math.max (0, olgBuffer[olgLength - 2] - 1) / olgUsPerPx) * olgUsPerPx,
                                 olgPxToUs (x));
          olgCaretPx = olgUsToPx (olgCaretUs);
          if (me.getClickCount () == 2) {  //ダブルクリックのとき
            //センタリングする
            olgStartUs = Math.max (0, olgStartUs + olgUsPerPx * (olgCaretPx - (OLG_SCORE_WIDTH >> 1)));
            olgCaretPx = (olgCaretUs - olgStartUs) / olgUsPerPx;
            olgEndUs = olgStartUs + olgRangeUs;
          }
          olgPaint ();
        }
      }
    });

    //キーリスナー
    olgCanvas.setFocusable (true);
    olgCanvas.addKeyListener (new KeyAdapter () {
      @Override public void keyPressed (KeyEvent ke) {
        int keyCode = ke.getKeyCode ();
        int modifierEx = ke.getModifiersEx ();
        boolean shift = (modifierEx & InputEvent.SHIFT_DOWN_MASK) == InputEvent.SHIFT_DOWN_MASK;
        boolean ctrl = (modifierEx & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK;
        int dxPx = 0;
        switch (keyCode) {
        case KeyEvent.VK_LEFT:
          dxPx = ctrl ? -100 : shift ? -10 : -1;
          break;
        case KeyEvent.VK_RIGHT:
          dxPx = ctrl ? 100 : shift ? 10 : 1;
          break;
        }
        if (dxPx != 0) {
          olgMoveCaret (dxPx);
          ke.consume ();
        }
      }
    });

    //録音ボックス
    Box recordBox = Multilingual.mlnTitledBorder (
      ComponentFactory.setTitledLineBorder (
        ComponentFactory.createHorizontalBox (
          olgStartButton = ComponentFactory.setEnabled (
            ComponentFactory.createButton ("●", listener),
            true),
          olgEndButton = ComponentFactory.setEnabled (
            ComponentFactory.createButton ("■", listener),
            false)),
        "Record"),
      "ja", "録音");

    //拡大ボックス
    Box zoomBox = Multilingual.mlnTitledBorder (
      ComponentFactory.setTitledLineBorder (
        ComponentFactory.createHorizontalBox (
          olgPlusButton =
          ComponentFactory.setEnabled (
            ComponentFactory.createButton ("＋", listener),
            0 < olgRangeIndex),
          olgMinusButton =
          ComponentFactory.setEnabled (
            ComponentFactory.createButton ("－", listener),
            olgRangeIndex < OLG_RANGE_COUNT - 1)),
        "Zoom"),
      "ja", "拡大");

    //移動ボックス
    Box moveBox = Multilingual.mlnTitledBorder (
      ComponentFactory.setTitledLineBorder (
        ComponentFactory.createHorizontalBox (
          ComponentFactory.createButton ("丨＜", listener),
          ComponentFactory.createButton ("≪≪", listener),
          ComponentFactory.createButton ("≪", listener),
          ComponentFactory.createButton ("＜", listener),
          ComponentFactory.createButton ("＞", listener),
          ComponentFactory.createButton ("≫", listener),
          ComponentFactory.createButton ("≫≫", listener),
          ComponentFactory.createButton ("＞丨", listener)),
        "Move"),
      "ja", "移動");

    //チャンネルボックス
    Box channelBox = Multilingual.mlnTitledBorder (
      ComponentFactory.setTitledLineBorder (
        ComponentFactory.createHorizontalBox (
          ComponentFactory.setEmptyBorder (
            ComponentFactory.setColor (ComponentFactory.createCheckBox (olgChannelMask[0], "0", listener), Color.black, OLG_KON_COLOR[0]),
            4, 4, 4, 4),
          ComponentFactory.setEmptyBorder (
            ComponentFactory.setColor (ComponentFactory.createCheckBox (olgChannelMask[1], "1", listener), Color.black, OLG_KON_COLOR[1]),
            4, 4, 4, 4),
          ComponentFactory.setEmptyBorder (
            ComponentFactory.setColor (ComponentFactory.createCheckBox (olgChannelMask[2], "2", listener), Color.black, OLG_KON_COLOR[2]),
            4, 4, 4, 4),
          ComponentFactory.setEmptyBorder (
            ComponentFactory.setColor (ComponentFactory.createCheckBox (olgChannelMask[3], "3", listener), Color.black, OLG_KON_COLOR[3]),
            4, 4, 4, 4),
          ComponentFactory.setEmptyBorder (
            ComponentFactory.setColor (ComponentFactory.createCheckBox (olgChannelMask[4], "4", listener), Color.black, OLG_KON_COLOR[4]),
            4, 4, 4, 4),
          ComponentFactory.setEmptyBorder (
            ComponentFactory.setColor (ComponentFactory.createCheckBox (olgChannelMask[5], "5", listener), Color.black, OLG_KON_COLOR[5]),
            4, 4, 4, 4),
          ComponentFactory.setEmptyBorder (
            ComponentFactory.setColor (ComponentFactory.createCheckBox (olgChannelMask[6], "6", listener), Color.black, OLG_KON_COLOR[6]),
            4, 4, 4, 4),
          ComponentFactory.setEmptyBorder (
            ComponentFactory.setColor (ComponentFactory.createCheckBox (olgChannelMask[7], "7", listener), Color.black, OLG_KON_COLOR[7]),
            4, 4, 4, 4)
          ),
        "Channel"),
      "ja", "チャンネル");

    //範囲ボックス
    Box rangeBox = Multilingual.mlnTitledBorder (
      ComponentFactory.setTitledLineBorder (
        ComponentFactory.createHorizontalBox (
          Multilingual.mlnText (ComponentFactory.createButton ("From", listener), "ja", "ここから"),
          Multilingual.mlnText (ComponentFactory.createButton ("To", listener), "ja", "ここまで"),
          Multilingual.mlnText (ComponentFactory.createButton ("All", listener), "ja", "全部")),
        "Range"),
      "ja", "範囲");

    //再生ボックス
    Box playBox = Multilingual.mlnTitledBorder (
      ComponentFactory.setTitledLineBorder (
        ComponentFactory.createHorizontalBox (
          ComponentFactory.createButton ("\u25b7", listener),  //右向き白三角
          ComponentFactory.createButton ("□", listener)),
        "Play"),
      "ja", "再生");

    //ツールボックス
    Box toolBox = ComponentFactory.createHorizontalBox (
      recordBox,
      zoomBox,
      moveBox,
      channelBox,
      rangeBox,
      playBox,
      Box.createHorizontalGlue ());

    //音色パネル
    olgToneImage = new BufferedImage (OLG_TONE_IMAGE_WIDTH, OLG_TONE_IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
    olgToneBitmap = ((DataBufferInt) olgToneImage.getRaster ().getDataBuffer ()).getData ();
    olgTonePanel = ComponentFactory.setPreferredSize (
      new JPanel () {
        @Override public void paintComponent (Graphics g) {
          super.paintComponent (g);
          g.drawImage (olgToneImage, 0, 0, null);
        }
      },
      OLG_TONE_IMAGE_WIDTH,
      OLG_TONE_IMAGE_HEIGHT);

    //ダンプテキストエリア
    olgDumpTextArea = ComponentFactory.createScrollTextArea ("",
                                                             OLG_DUMP_TEXT_AREA_WIDTH,
                                                             OLG_DUMP_TEXT_AREA_HEIGHT);
    olgDumpTextArea.setLineWrap (true);

    //ウインドウ
    olgFrame = Multilingual.mlnTitle (
      ComponentFactory.createRestorableSubFrame (
        Settings.SGS_OLG_FRAME_KEY,
        "OPM log",
        menuBar,
        ComponentFactory.createVerticalBox (
          toolBox,
          Multilingual.mlnTitledBorder (
            ComponentFactory.setTitledLineBorder (olgCanvas, "Score and wave"),
            "ja", "楽譜と波形"),
          ComponentFactory.createHorizontalBox (
            Multilingual.mlnTitledBorder (
              ComponentFactory.setTitledLineBorder (olgDumpTextArea, "Log"),
              "ja", "ログ"),
            Multilingual.mlnTitledBorder (
              ComponentFactory.setTitledLineBorder (
                ComponentFactory.createHorizontalBox (olgTonePanel), "Tone"),
              "ja", "音色")))),
      "ja", "OPM ログ");

    olgPaint ();

  }  //olgMakeFrame

  public static void olgMoveCaret (int dxPx) {
    olgCaretUs = Math.max (0,
                           Math.min ((Math.max (0, olgBuffer[olgLength - 2] - 1) / olgUsPerPx) * olgUsPerPx,
                                     olgCaretUs + olgUsPerPx * dxPx));
    olgCaretPx = (olgCaretUs - olgStartUs) / olgUsPerPx;
    if (olgCaretPx < 0) {
      olgStartUs = Math.max (0, olgStartUs - olgRangeUs);
      olgCaretPx = (olgCaretUs - olgStartUs) / olgUsPerPx;
      olgEndUs = olgStartUs + olgRangeUs;
    } else if (OLG_SCORE_WIDTH <= olgCaretPx) {
      olgStartUs = olgStartUs + olgRangeUs;
      olgCaretPx = (olgCaretUs - olgStartUs) / olgUsPerPx;
      olgEndUs = olgStartUs + olgRangeUs;
    }
    olgPaint ();
  }

  //録音開始
  public static void olgRecordStart () {
    if (olgRecording) {
      return;
    }
    olgRecording = true;
    olgLength = 0;
    olgStartTimePS = XEiJ.mpuClockTime;
    olgCounter = 0;
    //
    olgStartButton.setEnabled (false);
    olgEndButton.setEnabled (true);
    //
    olgForcedTerminationTask = new SimpleTask (XEiJ.tmrScheduler, (n) -> {
      olgRecordEnd ();
      return 0;
    }).oneShot (900000L);  //900秒で強制終了する
    //
    for (int a = 0x00; a <= 0xff; a++) {
      if (a == 0x08) {  //KON
      } else if (a == 0x19) {  //AMD/PMD
        for (int i = 0; i < 2; i++) {
          olgBuffer[olgCounter++] = 0;
          olgBuffer[olgCounter++] = a << 8 | (i << 7 | OPM.opmRegister[264 + i]);
        }
      } else {
        olgBuffer[olgCounter++] = 0;
        olgBuffer[olgCounter++] = a << 8 | OPM.opmRegister[a];
      }
    }
    //KONは最後に出力する
    for (int ch = 0; ch < 8; ch++) {
      olgBuffer[olgCounter++] = 0;
      olgBuffer[olgCounter++] = 0x08 << 8 | (OPM.opmRegister[256 + ch] << 3 | ch);
    }
  }

  //録音終了
  public static void olgRecordEnd () {
    if (!olgRecording) {
      return;
    }
    //
    olgBuffer[olgCounter++] = (int) ((XEiJ.mpuClockTime - olgStartTimePS) / (XEiJ.TMR_FREQ / OLG_UNIT));  //経過時間(OLG_UNIT)
    olgBuffer[olgCounter++] = -1;
    //
    olgRecording = false;
    olgLength = olgCounter;
    olgStartTimePS = 0L;
    olgCounter = 0;
    //
    if (olgForcedTerminationTask != null) {
      olgForcedTerminationTask.stop ();
      olgForcedTerminationTask = null;
    }
    //
    //olgStartButton.setEnabled (false);  //まだ次の記録を開始できない
    olgEndButton.setEnabled (false);
    //
    olgMakeWaveTask = new SimpleTask (XEiJ.tmrScheduler, (n) -> {
      olgMakeWaveData ();
      olgPaint ();
      olgStartButton.setEnabled (true);  //次の記録を開始できる
      //olgEndButton.setEnabled (false);
      olgMakeWaveTask = null;
      return 0;
    }).oneShot (0L);
  }

  //YM2151への書き込みを記録する
  public static void olgSetData (int a, int d) {
    if (olgRecording) {  //記録中
      if (OLG_BUFFER_SIZE - 2 <= olgCounter) {  //バッファが一杯になった
        olgRecordEnd ();  //記録を終了する
      } else {
        olgBuffer[olgCounter++] = (int) ((XEiJ.mpuClockTime - olgStartTimePS) / (XEiJ.TMR_FREQ / OLG_UNIT));  //経過時間(OLG_UNIT)
        olgBuffer[olgCounter++] = a << 8 | d;  //アドレス<<8|データ
      }
    }
  }

  //タイマAオーバーフローによる全スロットキーONを記録する
  public static void olgSetCSMKON () {
    if (olgRecording) {  //記録中
      if (OLG_BUFFER_SIZE - 2 <= olgCounter) {  //バッファが一杯になった
        olgRecordEnd ();  //記録を終了する
      } else {
        olgBuffer[olgCounter++] = (int) ((XEiJ.mpuClockTime - olgStartTimePS) / (XEiJ.TMR_FREQ / OLG_UNIT));  //経過時間(OLG_UNIT)
        olgBuffer[olgCounter++] = 0x10000;  //CSMKON
      }
    }
  }

  private static final int[] addressToData = new int[266];  //[address]=data,[256+ch]=slotMask,[264]=AMD,[265]=PMD
  private static final int[] addressToUs = new int[266];  //addressToDataに最後に書き込まれた時刻(us)
  private static final int[] channelToKindex = new int[8];  //[channel]=kindex。kindex=((kc-(kc>>2))<<6)|kf。(0..96)<<6|(0..63)=(0..6207)
  private static final int[] channelToKonUs = new int[8];  //[channel]=キーオンまたはキーオンしたままkc,kfが変化した時刻(us)。-1=キーオンしていない
  private static final int[] caretAddressToData = new int[266];  //キャレットを跨いだときのaddressToData
  private static final int[] caretAddressToUs = new int[266];  //キャレットを跨いだときのaddressToUs
  private static final int[] lastCaretAddressToUs = new int[266];  //最後に表示したときのcaretAddressToUs
  private static final boolean[] strong = new boolean[OLG_TONE_BASE.length];  //lastCaretAddressToUsとcaretAddressToUsが異なる表示位置

  //    |           1111111111222222222233333
  //    | 01234567890123456789012345678901234
  //  --+------------------------------------
  //   0|    FC SL WA SY SP PD AD PS AS PN   
  //   1|    -- -- -- -- -- -- -- -- -- -- --
  //   2|    AR 1R 2R RR 1L TL KS ML T1 T2 AE
  //   3| M1 -- -- -- -- -- -- -- -- -- -- --
  //   4| C1 -- -- -- -- -- -- -- -- -- -- --
  //   5| M2 -- -- -- -- -- -- -- -- -- -- --
  //   6| C2 -- -- -- -- -- -- -- -- -- -- --
  //   7|    KC KF
  //   8|    -- --
  private static final int[] TONE_MAP = new int[] {
    //[0]=address,[1]=row,[2]=mask1,[3]=col1,[4]=mask2,[5]=col2
    0x20, 1, 0b11000000, 30, 0b00111111,  3,  //PAN,FLCON
    0x28, 8, 0b01111111,  3,          0,  0,  //KC
    0x30, 8, 0b11111100,  6,          0,  0,  //KF
    0x38, 1, 0b01110000, 24, 0b00000011, 27,  //PMS,AMS
    //
    0x40, 3, 0b01110000, 27, 0b00001111, 24,  //M1 DT1,MUL
    0x48, 5, 0b01110000, 27, 0b00001111, 24,  //M2 DT1,MUL
    0x50, 4, 0b01110000, 27, 0b00001111, 24,  //C1 DT1,MUL
    0x58, 6, 0b01110000, 27, 0b00001111, 24,  //C2 DT1,MUL
    //
    0x60, 3, 0b01111111, 18,          0,  0,  //M1 TL
    0x68, 5, 0b01111111, 18,          0,  0,  //M2 TL
    0x70, 4, 0b01111111, 18,          0,  0,  //C1 TL
    0x78, 6, 0b01111111, 18,          0,  0,  //C2 TL
    //
    0x80, 3, 0b11000000, 21, 0b00011111,  3,  //M1 KS,AR
    0x88, 5, 0b11000000, 21, 0b00011111,  3,  //M2 KS,AR
    0x90, 4, 0b11000000, 21, 0b00011111,  3,  //C1 KS,AR
    0x98, 6, 0b11000000, 21, 0b00011111,  3,  //C2 KS,AR
    //
    0xa0, 3, 0b10000000, 33, 0b00011111,  6,  //M1 AMSEN,D1R
    0xa8, 5, 0b10000000, 33, 0b00011111,  6,  //M2 AMSEN,D1R
    0xb0, 4, 0b10000000, 33, 0b00011111,  6,  //C1 AMSEN,D1R
    0xb8, 6, 0b10000000, 33, 0b00011111,  6,  //C2 AMSEN,D1R
    //
    0xc0, 3, 0b11000000, 30, 0b00011111,  9,  //M1 DT2,D2R
    0xc8, 5, 0b11000000, 30, 0b00011111,  9,  //M2 DT2,D2R
    0xd0, 4, 0b11000000, 30, 0b00011111,  9,  //C1 DT2,D2R
    0xd8, 6, 0b11000000, 30, 0b00011111,  9,  //C2 DT2,D2R
    //
    0xe0, 3, 0b11110000, 15, 0b00001111, 12,  //M1 D1L,RR
    0xe8, 5, 0b11110000, 15, 0b00001111, 12,  //M2 D1L,RR
    0xf0, 4, 0b11110000, 15, 0b00001111, 12,  //C1 D1L,RR
    0xf8, 6, 0b11110000, 15, 0b00001111, 12,  //C2 D1L,RR
    //
    256,  1, 0b00001111,  6,          0,  0,  //SLOT
  };

  //描画
  public static void olgPaint () {
    Graphics2D g2 = olgCanvasImage.createGraphics ();
    //背景
    g2.setColor (Color.black);
    g2.fillRect (0, 0, OLG_SCORE_IMAGE_WIDTH, OLG_SCORE_IMAGE_HEIGHT);
    //時刻
    {
      g2.setColor (Color.darkGray);
      g2.setStroke (olgSolidStroke);
      int stepPx = 10;
      int stepUs = olgUsPerPx * stepPx;
      int us0 = ((olgStartUs + (stepUs - 1)) / stepUs) * stepUs;  //startをstepの倍数に切り上げる
      int px0 = olgUsToPx (us0);
      for (int px = px0; px < OLG_SCORE_WIDTH; px += stepPx) {
        int us = olgPxToUs (px);
        if (us % (stepUs * 10) != 0) {
          g2.drawLine (OLG_SCORE_H_MARGIN + px, OLG_SCORE_V_MARGIN,
                       //OLG_SCORE_H_MARGIN + px, OLG_SCORE_V_MARGIN + OLG_SCORE_HEIGHT - 1);
                       OLG_SCORE_H_MARGIN + px, OLG_WAVE_YM1);
        }
      }
    }
    //周波数
    g2.setColor (Color.darkGray);
    g2.setStroke (olgSolidStroke);
    //  3  4  5  6  7  8  9 10 11  0  1  2
    //  C  C# D  D# E  F  F# G  G# A  A# B
    for (int oct12 = -6; oct12 < 12 * 8 - 6; oct12++) {
      if ((1 << ((oct12 + 12) % 12) & (1 << 2 | 1 << 3 | 1 << 5 | 1 << 7 | 1 << 8 | 1 << 10)) != 0) {
        double oct = oct12 / 12.0;
        int kindex = (int) Math.floor (0.5 + 3584.0 + 64.0 * 12.0 * (oct - 4.0 + LOG2_358_400));
        int y = OLG_SCORE_HEIGHT * (6208 - 1 - kindex) / 6208;
        g2.drawLine (OLG_SCORE_H_MARGIN, OLG_SCORE_V_MARGIN + y,
                     OLG_SCORE_H_MARGIN + OLG_SCORE_WIDTH - 1, OLG_SCORE_V_MARGIN + y);
      }
    }
    //時刻
    g2.setFont (LnF.lnfMonospacedFont12);
    g2.setStroke (olgSolidStroke);
    {
      int stepPx = 100;
      int stepUs = olgUsPerPx * stepPx;
      int us0 = ((olgStartUs + (stepUs - 1)) / stepUs) * stepUs;  //startをstepの倍数に切り上げる
      int px0 = olgUsToPx (us0);
      for (int px = px0; px < OLG_SCORE_WIDTH; px += stepPx) {
        int us = olgPxToUs (px);
        g2.setColor (Color.gray);
        g2.drawLine (OLG_SCORE_H_MARGIN + px, OLG_SCORE_V_MARGIN,
                     //OLG_SCORE_H_MARGIN + px, OLG_SCORE_V_MARGIN + OLG_SCORE_HEIGHT - 1);
                     OLG_SCORE_H_MARGIN + px, OLG_WAVE_YM1);
        g2.setColor (Color.white);
        g2.drawString (String.valueOf ((double) us / 1000000.0) + "s",
                       OLG_SCORE_H_MARGIN + px + 3, OLG_SCORE_V_MARGIN + OLG_SCORE_HEIGHT - 2);
      }
    }
    //周波数
    //  kc=0..127
    //  kf=0..63
    //  kindex=((kc-(kc>>2))<<6)|kf=((0..96)<<6)|(0..63)=0..6207
    //  3.58MHzのときkc=74,kf=0,kindex=3584が440Hz
    //  4.00MHzのときkc=74,kf=0,kindex=3584が4.00/3.58*440Hz
    //  perl -e "print 2**((((0x4a-(0x4a>>2))<<6|0)-((0x4a-(0x4a>>2))<<6|0))/(12*64))*4.00/3.58*440"
    //  491.620111731844
    //  perl -e "print 2**((((0x47-(0x47>>2))<<6|0)-((0x4a-(0x4a>>2))<<6|0))/(12*64))*4.00/3.58*440"
    //  437.98372735391
    //  2^(oct-4)*440Hzのkindexは
    //  kindex=3584+(64*12)*((oct-4)+log2(3.58/4.00))
    g2.setFont (LnF.lnfMonospacedFont12);
    g2.setStroke (olgSolidStroke);
    for (int oct = 0; oct < 8; oct++) {
      int kindex = (int) Math.floor (0.5 + 3584.0 + 64.0 * 12.0 * ((double) (oct - 4) + LOG2_358_400));
      int y = OLG_SCORE_HEIGHT * (6208 - 1 - kindex) / 6208;
      g2.setColor (Color.gray);
      g2.drawLine (OLG_SCORE_H_MARGIN, OLG_SCORE_V_MARGIN + y,
                   OLG_SCORE_H_MARGIN + OLG_SCORE_WIDTH - 1, OLG_SCORE_V_MARGIN + y);
      g2.setColor (Color.white);
      g2.drawString ("o" + oct + "a", OLG_SCORE_H_MARGIN + 3, OLG_SCORE_V_MARGIN + y - 2);
    }
    //波形
    g2.setColor (Color.darkGray);
    g2.drawLine (OLG_SCORE_H_MARGIN, OLG_WAVE_Y1,
                 OLG_SCORE_H_MARGIN + OLG_SCORE_WIDTH - 1, OLG_WAVE_Y1);
    g2.setColor (Color.gray);
    g2.drawLine (OLG_SCORE_H_MARGIN, OLG_WAVE_Y0,
                 OLG_SCORE_H_MARGIN + OLG_SCORE_WIDTH - 1, OLG_WAVE_Y0);
    g2.setColor (Color.darkGray);
    g2.drawLine (OLG_SCORE_H_MARGIN, OLG_WAVE_YM1,
                 OLG_SCORE_H_MARGIN + OLG_SCORE_WIDTH - 1, OLG_WAVE_YM1);
    if (opmBuffer != null) {
      Path2D pathL = new Path2D.Float ();
      Path2D pathR = new Path2D.Float ();
      for (int x = 0; x < OLG_SCORE_WIDTH; x++) {
        int us = olgPxToUs (x);
        int i = OPM_CHANNELS * (us >> 4);
        if (opmBuffer.length <= i) {
          break;
        }
        int yL = (OLG_WAVE_RADIUS * opmBuffer[i    ]) >> 15;
        int yR = (OLG_WAVE_RADIUS * opmBuffer[i + 1]) >> 15;
        if (x == 0) {
          pathL.moveTo ((float) (OLG_SCORE_H_MARGIN + x), (float) (OLG_WAVE_Y0 - yL));
          pathR.moveTo ((float) (OLG_SCORE_H_MARGIN + x), (float) (OLG_WAVE_Y0 - yR));
        } else {
          pathL.lineTo ((float) (OLG_SCORE_H_MARGIN + x), (float) (OLG_WAVE_Y0 - yL));
          pathR.lineTo ((float) (OLG_SCORE_H_MARGIN + x), (float) (OLG_WAVE_Y0 - yR));
        }
      }
      g2.setStroke (olgWaveStroke);
      g2.setColor (Color.yellow);
      g2.draw (pathL);
      g2.setColor (Color.cyan);
      g2.draw (pathR);
    }
    //データ
    Arrays.fill (addressToData, 0);
    Arrays.fill (addressToUs, -1);
    Arrays.fill (channelToKindex, 0);
    Arrays.fill (channelToKonUs, -1);
    Arrays.fill (caretAddressToData, 0);
    Arrays.fill (caretAddressToUs, -1);
    //Arrays.fill (lastCaretAddressToUs, -1);
    int caretIndex = -1;  //olgCaretUs以上の最初のolgBufferのインデックス
    g2.setStroke (olgSolidStroke);
    for (int index = 0; index < olgLength; index += 2) {
      int us = olgBuffer[index];
      int ad = olgBuffer[index + 1];
      if (olgEndUs <= us || ad == -1) {
        break;
      }
      if (ad == 0x10000) {  //CSMKON
        continue;
      }
      if (caretIndex == -1 && olgCaretUs <= us) {
        caretIndex = index;
        System.arraycopy (addressToData, 0,
                          caretAddressToData, 0,
                          addressToData.length);
        System.arraycopy (addressToUs, 0,
                          caretAddressToUs, 0,
                          addressToUs.length);
      }
      int a = ad >> 8;
      int d = ad & 255;
      addressToData[a] = d;
      addressToUs[a] = us;
      if (!((a == 0x08 && !olgChannelMask[d & 7]) ||  //KONでチャンネルがマスクされている、または
            (0x10 <= a && a <= 0x12) ||  //CLKA1,CLKA2,CLKBのいずれか、または
            (0x20 <= a && !olgChannelMask[a & 7]))) {  //0x20～0xffでチャンネルがマスクされている、でなければ
        g2.setColor (a == 0x08 ? OLG_KON_COLOR[d & 7] :
                     0x20 <= a ? OLG_KON_COLOR[a & 7] :
                     Color.white);
        int px = olgUsToPx (us);
        g2.drawLine (OLG_SCORE_H_MARGIN + px, OLG_SCORE_V_MARGIN + OLG_SCORE_HEIGHT - 14 - (OLG_KON1ST_HEIGHT - 1),
                     OLG_SCORE_H_MARGIN + px, OLG_SCORE_V_MARGIN + OLG_SCORE_HEIGHT - 14);
      }
      if (a == 0x08) {  //KON
        int channel = d & 7;
        if (channelToKonUs[channel] != -1) {
          olgDrawKoff (g2, channel, channelToKindex[channel], channelToKonUs[channel], us);
          channelToKonUs[channel] = -1;
        }
        int slotMask = (d >> 3) & 15;
        addressToData[256 + channel] = slotMask;
        addressToUs[256 + channel] = us;
        if (slotMask != 0) {
          olgDrawKon1st (g2, channel, channelToKindex[channel], us);
          channelToKonUs[channel] = us;
        }
      } else if (a == 0x19) {  //AMD/PMD
        int aa = (d & 128) == 0 ? 264 : 265;
        addressToData[aa] = d & 127;
        addressToUs[aa] = us;
      } else if (0x28 <= a && a < 0x30) {  //KC
        int channel = a & 7;
        int kc = d & 127;
        int kf = (addressToData[0x30 + channel] >> 2) & 63;
        int kindex = ((kc - (kc >> 2)) << 6) | kf;
        if (channelToKonUs[channel] != -1) {
          olgDrawKoff (g2, channel, channelToKindex[channel], channelToKonUs[channel], us);
          olgDrawKon2nd (g2, channel, kindex, us);
          channelToKonUs[channel] = us;
        }
        channelToKindex[channel] = kindex;
      } else if (0x30 <= a && a < 0x38) {  //KF
        int channel = a & 7;
        int kc = addressToData[0x28 + channel] & 127;
        int kf = (d >> 2) & 63;
        int kindex = ((kc - (kc >> 2)) << 6) | kf;
        if (channelToKonUs[channel] != -1) {
          olgDrawKoff (g2, channel, channelToKindex[channel], channelToKonUs[channel], us);
          olgDrawKon2nd (g2, channel, kindex, us);
          channelToKonUs[channel] = us;
        }
        channelToKindex[channel] = kindex;
      }
    }  //for index
    for (int channel = 0; channel < 8; channel++) {
      if (channelToKonUs[channel] != -1) {  //キーオンしたまま終了した
        olgDrawKoff (g2, channel, channelToKindex[channel], channelToKonUs[channel], Math.min (olgBuffer[olgLength - 2], olgEndUs));
        channelToKonUs[channel] = -1;
      }
    }
    //マスク左側
    if (olgMaskLeftUs != -1 &&
        olgStartUs < olgMaskLeftUs) {
      int px0 = 0;
      int px1 = olgUsToPx (Math.min (olgEndUs, olgMaskLeftUs));
      g2.setPaint (olgMaskPaint);
      g2.fillRect (OLG_SCORE_H_MARGIN + px0, OLG_SCORE_V_MARGIN, px1 - px0, OLG_SCORE_HEIGHT);
    }
    //マスク右側
    if (olgMaskRightUs != -1 &&
        olgMaskRightUs < olgEndUs) {
      int px0 = olgUsToPx (Math.max (olgStartUs, olgMaskRightUs));
      int px1 = OLG_SCORE_WIDTH;
      g2.setPaint (olgMaskPaint);
      g2.fillRect (OLG_SCORE_H_MARGIN + px0, OLG_SCORE_V_MARGIN, px1 - px0, OLG_SCORE_HEIGHT);
    }
    //キャレット
    g2.setColor (Color.green);
    g2.setStroke (olgDashStroke);
    g2.drawLine (OLG_SCORE_H_MARGIN + olgCaretPx, OLG_SCORE_V_MARGIN,
                 //OLG_SCORE_H_MARGIN + olgCaretPx, OLG_SCORE_V_MARGIN + OLG_SCORE_HEIGHT - 1);
                 OLG_SCORE_H_MARGIN + olgCaretPx, OLG_WAVE_YM1);
    olgCanvas.repaint ();
    //
    //音色
    Arrays.fill (olgToneBitmap, 0xff000000);
    for (int ch = 0; ch < 8; ch++) {
      Arrays.fill (strong, false);
      for (int j = 0; j < TONE_MAP.length; j += 6) {
        int address = TONE_MAP[j    ];
        int row     = TONE_MAP[j + 1];
        int mask1   = TONE_MAP[j + 2];
        int col1    = TONE_MAP[j + 3];
        int mask2   = TONE_MAP[j + 4];
        int col2    = TONE_MAP[j + 5];
        int a = address + ch;
        XEiJ.fmtHex2 (OLG_TONE_BASE, OLG_TONE_COLS * row + col1, (caretAddressToData[a] & mask1) >> Integer.numberOfTrailingZeros (mask1));
        if (mask2 != 0) {
          XEiJ.fmtHex2 (OLG_TONE_BASE, OLG_TONE_COLS * row + col2, (caretAddressToData[a] & mask2) >> Integer.numberOfTrailingZeros (mask2));
        }
        if (lastCaretAddressToUs[a] != caretAddressToUs[a]) {
          strong[OLG_TONE_COLS * row + col1    ] = true;
          strong[OLG_TONE_COLS * row + col1 + 1] = true;
          if (mask2 != 0) {
            strong[OLG_TONE_COLS * row + col2    ] = true;
            strong[OLG_TONE_COLS * row + col2 + 1] = true;
          }
        }
      }  //for j
      {
        int a = 0x18;  //LFRQ
        int d = caretAddressToData[a];
        int row = 1;
        int col = 15;
        XEiJ.fmtHex2 (OLG_TONE_BASE, OLG_TONE_COLS * row + col, d & 255);
        if (lastCaretAddressToUs[a] != caretAddressToUs[a]) {
          strong[OLG_TONE_COLS * row + col    ] = true;
          strong[OLG_TONE_COLS * row + col + 1] = true;
        }
      }
      {
        int a = 0x1b;  //W
        int d = caretAddressToData[a];
        int row = 1;
        int col = 9;
        XEiJ.fmtHex2 (OLG_TONE_BASE, OLG_TONE_COLS * row + col, d & 3);
        if (lastCaretAddressToUs[a] != caretAddressToUs[a]) {
          strong[OLG_TONE_COLS * row + col    ] = true;
          strong[OLG_TONE_COLS * row + col + 1] = true;
        }
      }
      {
        int a = 264;  //AMD
        int d = caretAddressToData[a];
        int row = 1;
        int col = 21;
        XEiJ.fmtHex2 (OLG_TONE_BASE, OLG_TONE_COLS * row + col, d);
        if (lastCaretAddressToUs[a] != caretAddressToUs[a]) {
          strong[OLG_TONE_COLS * row + col    ] = true;
          strong[OLG_TONE_COLS * row + col + 1] = true;
        }
      }
      {
        int a = 265;  //PMD
        int d = caretAddressToData[a];
        int row = 1;
        int col = 18;
        XEiJ.fmtHex2 (OLG_TONE_BASE, OLG_TONE_COLS * row + col, d);
        if (lastCaretAddressToUs[a] != caretAddressToUs[a]) {
          strong[OLG_TONE_COLS * row + col    ] = true;
          strong[OLG_TONE_COLS * row + col + 1] = true;
        }
      }
      int chCol = ch & 3;
      int chRow = ch >> 2;
      int chX = OLG_TONE_H_MARGIN + (OLG_TONE_CHAR_WIDTH * OLG_TONE_COLS + OLG_TONE_H_SPACE) * chCol;
      int chY = OLG_TONE_V_MARGIN + (OLG_TONE_LINE_HEIGHT * OLG_TONE_ROWS + OLG_TONE_V_SPACE) * chRow;
      int rgb = (olgChannelMask[ch] ? (caretAddressToData[256 + ch] != 0 ? OLG_KON_COLOR : OLG_BAR_COLOR)[ch] : Color.darkGray).getRGB ();
      for (int row = 0; row < OLG_TONE_ROWS; row++) {
        for (int col = 0; col < OLG_TONE_COLS; col++) {
          int c = OLG_TONE_BASE[col + OLG_TONE_COLS * row];
          for (int v = 0; v < 8; v++) {
            int t = FontPage.Lcd.LCD6X8_FONT[8 * c + v] << 24;
            for (int u = 0; u < 6; u++) {
              olgToneBitmap[chX + OLG_TONE_CHAR_WIDTH * col + u +
                            OLG_TONE_IMAGE_WIDTH * (chY + OLG_TONE_LINE_HEIGHT * row + v)] = t < 0 ? rgb : 0xff000000;
              t <<= 1;
            }
          }
          if (strong[col + OLG_TONE_COLS * row]) {
            for (int u = 0; u < 6; u++) {
              olgToneBitmap[chX + OLG_TONE_CHAR_WIDTH * col + u +
                            OLG_TONE_IMAGE_WIDTH * (chY + OLG_TONE_LINE_HEIGHT * row + 8)] = rgb;
            }
          }
        }  //for col
      }  //for row
    }  //for ch
    System.arraycopy (caretAddressToUs, 0,
                      lastCaretAddressToUs, 0,
                      caretAddressToUs.length);
    olgTonePanel.repaint ();
    //
    //ダンプ
    if (caretIndex == -1) {  //olgCaretUs以上のデータが存在しない
      caretIndex = Math.max (0, olgLength - 2);
    }
    {
      //範囲を決める
      //  下限をolgUsPerPxずつ減らしてindexが2*10以上減ったら止まる
      int leftUs = olgCaretUs;
      int leftIndex = caretIndex;
      while (Math.max (0, caretIndex - 2 * 10) < leftIndex) {
        leftUs = Math.max (0, leftUs - olgUsPerPx);
        while (0 < leftIndex && leftUs <= olgBuffer[leftIndex - 2]) {
          leftIndex -= 2;
        }
      }
      //  上限をolgUsPerPxずつ増やしてindexが2*10以上増えたら止まる
      int rightUs = olgCaretUs;
      int rightIndex = caretIndex;
      while (rightIndex < Math.min (olgLength - 2, caretIndex + 2 * 10)) {
        rightUs = Math.min (olgBuffer[olgLength - 2], rightUs + olgUsPerPx);
        while (rightIndex < olgLength - 2 && olgBuffer[rightIndex + 2] <= rightUs) {
          rightIndex += 2;
        }
      }
      //ダンプする
      StringBuilder sb = new StringBuilder ();
      for (int index = leftIndex; index <= rightIndex; index += 2) {
        if (index == caretIndex) {
          sb.append ("--------------------\n");
        }
        int us = olgBuffer[index];
        int ad = olgBuffer[index + 1];
        sb.append (String.valueOf ((double) us / 1000000.0)).append ("s");
        if (ad == -1) {
          sb.append (" -1 END\n");
          break;
        }
        sb.append (" 0x");
        XEiJ.fmtHex4 (sb, ad);
        if (ad == 0x10000) {
          sb.append (" CSMKON\n");
          continue;
        }
        int a = (ad >> 8) & 255;
        int d = ad & 255;
        if (a < 0x20) {
          switch (a) {
          case 0x01:
            sb.append (" LFORESET=").append ((d >> 1) & 1);
            break;
          case 0x08:
            sb.append (" KON[").append (d & 7).append ("]=").append ((d >> 3) & 15);
            break;
          case 0x0f:
            sb.append (" NE=").append ((d >> 7) & 1);
            sb.append (" NFRQ=").append (d & 31);
            break;
          case 0x10:
            sb.append (" CLKA1=").append (d & 255);
            break;
          case 0x11:
            sb.append (" CLKA2=").append (d & 3);
            break;
          case 0x12:
            sb.append (" CLKB=").append (d & 255);
            break;
          case 0x14:
            sb.append (" CSM=").append ((d >> 7) & 1);
            sb.append (" RESETB=").append ((d >> 5) & 1);
            sb.append (" RESETA=").append ((d >> 4) & 1);
            sb.append (" IRQENB=").append ((d >> 3) & 1);
            sb.append (" IRQENA=").append ((d >> 2) & 1);
            sb.append (" LOADB=").append ((d >> 1) & 1);
            sb.append (" LOADA=").append (d & 1);
            break;
          case 0x18:
            sb.append (" LFRQ=").append (d & 255);
            break;
          case 0x19:
            if (((d >> 7) & 1) == 0) {
              sb.append (" AMD=").append (d & 127);
            } else {
              sb.append (" PMD=").append (d & 127);
            }
            break;
          case 0x1b:
            sb.append (" CT1=").append ((d >> 7) & 1);
            sb.append (" CT2=").append ((d >> 6) & 1);
            sb.append (" W=").append (d & 3);
            break;
          }
        } else if (a < 0x40) {
          sb.append (" CH").append (a & 7);
          switch (a >> 3) {
          case 0x20 >> 3:
            sb.append (" R=").append ((d >> 7) & 1);
            sb.append (" L=").append ((d >> 6) & 1);
            sb.append (" FL=").append ((d >> 3) & 7);
            sb.append (" CON=").append (d & 7);
            break;
          case 0x28 >> 3:
            sb.append (" KC=").append (d & 127);
            break;
          case 0x30 >> 3:
            sb.append (" KF=").append ((d >> 2) & 63);
            break;
          case 0x38 >> 3:
            sb.append (" PMS=").append ((d >> 4) & 7);
            sb.append (" AMS=").append (d & 3);
            break;
          }
        } else {
          sb.append (" CH").append (a & 7);
          sb.append (((a >> 3) & 3) == 0 ? " M1" :
                     ((a >> 3) & 3) == 1 ? " M2" :
                     ((a >> 3) & 3) == 2 ? " C1" : " C2");
          switch (a >> 5) {
          case 0x40 >> 5:
            sb.append (" DT1=").append ((d >> 4) & 7);
            sb.append (" MUL=").append (d & 15);
            break;
          case 0x60 >> 5:
            sb.append (" TL=").append (d & 127);
            break;
          case 0x80 >> 5:
            sb.append (" KS=").append ((d >> 6) & 3);
            sb.append (" AR=").append (d & 31);
            break;
          case 0xa0 >> 5:
            sb.append (" AMSEN=").append ((d >> 7) & 1);
            sb.append (" D1R=").append (d & 31);
            break;
          case 0xc0 >> 5:
            sb.append (" DT2=").append ((d >> 6) & 3);
            sb.append (" D2R=").append (d & 31);
            break;
          case 0xe0 >> 5:
            sb.append (" D1L=").append ((d >> 4) & 15);
            sb.append (" RR=").append (d & 15);
            break;
          }
        }
        sb.append ("\n");
      }  //for index
      String text = sb.toString ();
      olgDumpTextArea.setText (text);
      olgDumpTextArea.setCaretPosition (text.length ());
      olgDumpTextArea.setCaretPosition (text.indexOf ("----"));
    }
  }

  //バーを描く
  public static void olgDrawKon1st (Graphics2D g2, int channel, int kindex, int us) {
    if (olgStartUs <= us && us < olgEndUs &&
        olgChannelMask[channel]) {
      int py = OLG_SCORE_HEIGHT * (6208 - 1 - kindex) / 6208;
      int px = Math.max (0, us - olgStartUs) / olgUsPerPx;
      g2.setColor (OLG_KON_COLOR[channel]);
      g2.drawLine (OLG_SCORE_H_MARGIN + px, OLG_SCORE_V_MARGIN + py - (OLG_KON1ST_HEIGHT >> 1),
                   OLG_SCORE_H_MARGIN + px, OLG_SCORE_V_MARGIN + py + (OLG_KON1ST_HEIGHT >> 1));
    }
  }
  public static void olgDrawKon2nd (Graphics2D g2, int channel, int kindex, int us) {
    if (olgStartUs <= us && us < olgEndUs &&
        olgChannelMask[channel]) {
      int py = OLG_SCORE_HEIGHT * (6208 - 1 - kindex) / 6208;
      int px = Math.max (0, us - olgStartUs) / olgUsPerPx;
      g2.setColor (OLG_BAR_COLOR[channel]);
      g2.drawLine (OLG_SCORE_H_MARGIN + px, OLG_SCORE_V_MARGIN + py - (OLG_KON2ND_HEIGHT >> 1),
                   OLG_SCORE_H_MARGIN + px, OLG_SCORE_V_MARGIN + py + (OLG_KON2ND_HEIGHT >> 1));
    }
  }
  public static void olgDrawKoff (Graphics2D g2, int channel, int kindex, int leftUs, int rightUs) {
    if (olgStartUs < rightUs && leftUs < olgEndUs &&
        olgChannelMask[channel]) {
      int py = OLG_SCORE_HEIGHT * (6208 - 1 - kindex) / 6208;
      int px0 = Math.max (0, leftUs - olgStartUs) / olgUsPerPx;
      int px1 = (rightUs - olgStartUs) / olgUsPerPx;
      if (px0 + 1 < px1) {
        g2.setColor (OLG_BAR_COLOR[channel]);
        g2.fillRect (OLG_SCORE_H_MARGIN + px0 + 1, OLG_SCORE_V_MARGIN + py - (OLG_BAR_HEIGHT >> 1),
                     px1 - (px0 + 1), OLG_BAR_HEIGHT);
      }
    }
  }

  //  MMLをロードする
  public static void olgLoadMML () {
    JFileChooser2 fileChooser = new JFileChooser2 (olgLastMMLFile);
    fileChooser.setFileFilter (olgMMLFilter);
    if (fileChooser.showOpenDialog (null) != JFileChooser.APPROVE_OPTION) {
      return;
    }
    File file = fileChooser.getSelectedFile ();
    String program = XEiJ.rscGetTextFile (file.getPath ());
    MMLCompiler compiler = new MMLCompiler ();
    int[] array = compiler.compile (program);
    if (array == null) {  //エラー
      JOptionPane.showMessageDialog (null, compiler.getError ());
      return;
    }
    olgLastMMLFile = file;
    System.arraycopy (array, 0, olgBuffer, 0, array.length);
    olgLength = array.length;
    olgMakeWaveData ();
    olgPaint ();
  }

  //  ログをロードする
  public static void olgLoadLog () {
    JFileChooser2 fileChooser = new JFileChooser2 (olgLastFile);
    fileChooser.setFileFilter (olgFilter);
    if (fileChooser.showOpenDialog (null) != JFileChooser.APPROVE_OPTION) {
      return;
    }
    File file = fileChooser.getSelectedFile ();
    byte[] b = XEiJ.rscGetFile (file.getPath ());
    //バイト配列の長さは8以上かつバッファの長さの4倍以下かつ8の倍数であること
    boolean error = !(8 <= b.length && b.length <= 4 * OLG_BUFFER_SIZE && b.length % 8 == 0);
    if (!error) {
      olgLength = b.length >> 2;
      for (int i = 0; i < olgLength; i++) {
        olgBuffer[i] = ((b[4 * i    ]      ) << 24 |
                        (b[4 * i + 1] & 255) << 16 |
                        (b[4 * i + 2] & 255) <<  8 |
                        (b[4 * i + 3] & 255));
      }
      int lastUs = 0;
      for (int i = 0; i < olgLength; i += 2) {
        int us = olgBuffer[i];
        int ad = olgBuffer[i + 1];
        //時刻は0..999999999かつ昇順であること
        //アドレスとデータは最後以外は0..0x10000、最後は-1であること
        if (!(0 <= us && us <= 999999999 && lastUs <= us &&
              (i < olgLength - 2 ? 0 <= ad && ad <= 0x10000 : ad == -1))) {
          error = true;
          break;
        }
        lastUs = us;
      }
      if (!error) {
        olgLastFile = file;
      }
    }
    if (error) {  //エラー
      olgBuffer[0] = 0;
      olgBuffer[1] = -1;
      olgLength = 2;
    }
    olgMakeWaveData ();
    olgPaint ();
  }

  //  ログをセーブする
  public static void olgSaveLog () {
    byte[] b = new byte[4 * olgLength];
    for (int i = 0; i < olgLength; i++) {
      int t = olgBuffer[i];
      b[4 * i    ] = (byte) (t >> 24);  //big-endian
      b[4 * i + 1] = (byte) (t >> 16);
      b[4 * i + 2] = (byte) (t >>  8);
      b[4 * i + 3] = (byte)  t;
    }
    JFileChooser2 fileChooser = new JFileChooser2 (olgLastFile);
    fileChooser.setFileFilter (olgFilter);
    if (fileChooser.showSaveDialog (null) != JFileChooser.APPROVE_OPTION) {
      return;
    }
    File file = fileChooser.getSelectedFile ();
    if (XEiJ.rscPutFile (file.getPath (), b, 0, 4 * olgLength)) {
      olgLastFile = file;
    }
  }

  //アジャスト
  //  機能
  //    時刻が0でないレジスタ設定を最初のキーオンの時刻が1000000の倍数になるようにずらす
  //  手順
  //    時刻が0でない最初のレジスタ設定またはエンドコードの時刻us1を探す
  //    時刻が0でない最初のキーオンの時刻us2を探す。なければ何もしない
  //    us2-us1+1を1000000の倍数に切り上げた時刻us3を求める
  //    時刻が0でないレジスタ設定またはエンドコードの時刻にus3-us2を加える
  public static void olgAdjust () {
    int us1 = 0;  //時刻が0でない最初のレジスタ設定またはエンドコードの時刻
    int us2 = 0;  //時刻が0でない最初のキーオンの時刻
    for (int i = 0; i < olgLength; i += 2) {
      int us = olgBuffer[i];
      if (us == 0) {
        continue;
      }
      int ad = olgBuffer[i + 1];
      if (us1 == 0) {  //最初のレジスタ設定またはエンドコード
        us1 = us;
      }
      if (ad == 0x10000) {  //CSMKON
        us2 = us;
        break;
      }
      if (ad != -1) {  //CSMKON,END以外
        int a = ad >> 8;
        int d = ad & 255;
        if (a == 0x08) {  //KON
          int mask = (d >> 3) & 15;
          if (mask != 0) {  //キーオン
            us2 = us;
            break;
          }
        }
      }
    }  //for i
    if (us2 == 0) {  //キーオンがない
      return;
    }
    int us3 = ((us2 - us1 + 1 + (1000000 - 1)) / 1000000) * 1000000;  //us2-us1+1を1000000の倍数に切り上げた時刻
    for (int i = 0; i < olgLength; i += 2) {
      int us = olgBuffer[i];
      if (us == 0) {
        continue;
      }
      olgBuffer[i] += us3 - us2;
    }
    olgPaint ();
  }

  private static final int VOLUME = 1024;  //1024=1
  private static int samples62500;  //62500Hzのサンプル数
  private static int samples48000;  //48000Hzのサンプル数
  private static byte[] sampleBuffer;  //出力するデータ
  private static SourceDataLine sourceDataLine;  //ライン
  private static int totalBytes;  //出力するバイト数
  private static volatile int writtenBytes;  //出力したバイト数
  private static volatile SimpleTask playTask;  //再生タスク

  //波形データを作る
  public static void olgMakeWaveData () {
    olgYM2151.reset ();
    olgYM2151.setChannelMask ((olgChannelMask[0] ? 1 << 0 : 0) |
                              (olgChannelMask[1] ? 1 << 1 : 0) |
                              (olgChannelMask[2] ? 1 << 2 : 0) |
                              (olgChannelMask[3] ? 1 << 3 : 0) |
                              (olgChannelMask[4] ? 1 << 4 : 0) |
                              (olgChannelMask[5] ? 1 << 5 : 0) |
                              (olgChannelMask[6] ? 1 << 6 : 0) |
                              (olgChannelMask[7] ? 1 << 7 : 0));
    samples62500 = 0;
    samples48000 = 0;
    int totalUs = olgBuffer[olgLength - 2];
    if (totalUs == 0) {
      return;
    }
    //OPM→int[]
    samples62500 = totalUs >> 4;  //1000000/62500=16
    olgYM2151.allocate (OPM_CHANNELS * samples62500);
    opmBuffer = olgYM2151.getBuffer ();
    int minUS = 0;
    for (int index = 0; index < olgLength; index += 2) {
      int us = olgBuffer[index];
      int ad = olgBuffer[index + 1];
      if (ad == -1) {
        break;
      }
      us = Math.max (minUS, us);
      int pointer = OPM_CHANNELS * (us >> 4);
      if (OPM_CHANNELS * samples62500 <= pointer) {
        break;
      }
      minUS = us + 16;  //次の時刻の最小値。パラメータは同時刻に書き込めるがキーオンとキーオフは同時刻に書き込めない
      olgYM2151.generate (pointer);
      if (ad == 0x10000) {  //CSMKON
        olgYM2151.timerAExpired ();
      } else {
        olgYM2151.writeAddress (ad >> 8);
        olgYM2151.writeData (ad);
      }
    }
    olgYM2151.fill ();
    //int[]→byte[]
    samples48000 = (int) (((long) samples62500 * 48000L) / 62500L);
    sampleBuffer = new byte[2 * OPM_CHANNELS * samples48000];
    for (int i48000 = 0; i48000 < samples48000; i48000++) {
      int i62500 = (int) (((long) i48000 * 62500L) / 48000L);
      if (OPM_CHANNELS == 1) {
        int m = (opmBuffer[i62500] * VOLUME) >> 10;
        m = Math.max (-32768, Math.min (32767, m));
        sampleBuffer[2 * i48000    ] = (byte) m;
        sampleBuffer[2 * i48000 + 1] = (byte) (m >> 8);
      } else {
        int l = (opmBuffer[2 * i62500    ] * VOLUME) >> 10;
        int r = (opmBuffer[2 * i62500 + 1] * VOLUME) >> 10;
        l = Math.max (-32768, Math.min (32767, l));
        r = Math.max (-32768, Math.min (32767, r));
        sampleBuffer[4 * i48000    ] = (byte) l;
        sampleBuffer[4 * i48000 + 1] = (byte) (l >> 8);
        sampleBuffer[4 * i48000 + 2] = (byte) r;
        sampleBuffer[4 * i48000 + 3] = (byte) (r >> 8);
      }
    }
  }

  //再生開始
  //  バッファサイズ500msでラインを開く
  //  100msずつ割り込む
  //    出力するデータがあるとき
  //      200msまたは残り全部の少ない方を求める
  //      出力できるとき
  //        出力する
  //    出力するデータがないとき
  //      ラインが空のとき
  //        ラインを閉じる
  public static void olgPlayStart () {
    if (playTask != null) {  //再生中
      writtenBytes = 0;  //先頭に戻る
      return;
    }
    int fromUs = olgMaskLeftUs != -1 ? olgMaskLeftUs : 0;
    int toUs = olgMaskRightUs != -1 ? olgMaskRightUs : olgBuffer[olgLength - 2];
    int from62500 = fromUs >> 4;  //1000000/62500=16
    int to62500 = toUs >> 4;  //1000000/62500=16
    int from48000 = (int) (((long) from62500 * 48000L) / 62500L);
    int to48000 = (int) (((long) to62500 * 48000L) / 62500L);
    int fromBytes = 2 * OPM_CHANNELS * from48000;
    int toBytes = 2 * OPM_CHANNELS * to48000;
    totalBytes = toBytes;
    writtenBytes = fromBytes;
    //バッファサイズ500msでラインを開く
    try {
      AudioFormat audioFormat = new AudioFormat (48000.0F,  //sampleRate
                                                 16,  //sampleSizeInBits
                                                 OPM_CHANNELS,  //channels
                                                 true,  //signed
                                                 false);  //bigEndian
      sourceDataLine = AudioSystem.getSourceDataLine (audioFormat);
      sourceDataLine.open (audioFormat, 2 * OPM_CHANNELS * 48000 * 500 / 1000);
      sourceDataLine.start ();
    } catch (Exception e) {
      e.printStackTrace ();
      return;
    }
    //100msずつ割り込む
    playTask = new SimpleTask (XEiJ.tmrScheduler, (n) -> {
      if (writtenBytes < totalBytes) {  //出力するデータがあるとき
        //200msまたは残り全部の少ない方を求める
        int thisTimeBytes = Math.min (2 * OPM_CHANNELS * 48000 * 200 / 1000, totalBytes - writtenBytes);
        if (thisTimeBytes <= sourceDataLine.available ()) {  //出力できるとき
          //出力する
          try {
            sourceDataLine.write (sampleBuffer, writtenBytes, thisTimeBytes);
            writtenBytes += thisTimeBytes;
          } catch (Exception e) {
            e.printStackTrace ();
            writtenBytes = totalBytes;
          }
        }
        return 1;
      } else {  //出力するデータがないとき
        //ラインを閉じる
        try {
          sourceDataLine.stop ();
          sourceDataLine.close ();
        } catch (Exception e) {
          e.printStackTrace ();
        }
        sourceDataLine = null;
        playTask = null;
        return 0;
      }
    }).fixedPeriod (0L, 100L);
  }

  //再生終了
  //  出力するデータの残りを0にする
  public static void olgPlayEnd () {
    writtenBytes = totalBytes;
  }

}
