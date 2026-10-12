//========================================================================================
//  GIFAnimation.java
//    en:GIF animation recording
//    ja:GIFアニメーション録画
//  Copyright (C) 2003-2026 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.awt.*;  //Graphics2D,RenderingHints
import java.awt.event.*;  //ActionEvent,ActionListener
import java.awt.image.*;  //BufferedImage,DataBufferInt
import java.io.*;  //File
import java.util.*;  //HashSet,Timer
import javax.imageio.*;  //ImageIO
import javax.imageio.metadata.*;  //IIOMetadata
import javax.imageio.stream.*;  //ImageOutputStream
import javax.swing.*;  //JSpinner,SpinnerNumberModel
import javax.swing.event.*;  //ChangeEvent,ChangeListener
import org.w3c.dom.*;  //Node

public class GIFAnimation {

  //GIFアニメーション録画

  //待ち時間
  //  例えばゲームの画面を録画するとき、ポーズをかけた状態で録画ボタンを押してポーズを解除してプレイを再開した後に録画を開始できる

  public static final int GIF_WAITING_TIME_MIN = 0;  //待ち時間(s)の最小値
  public static final int GIF_WAITING_TIME_MAX = 30;  //待ち時間(s)の最大値
  public static final int GIF_RECORDING_TIME_MIN = 1;  //録画時間(s)の最小値
  public static final int GIF_RECORDING_TIME_MAX = 30;  //録画時間(s)の最大値
  public static final int GIF_MAGNIFICATION_MIN = 10;  //倍率(%)の最小値
  public static final int GIF_MAGNIFICATION_MAX = 200;  //倍率(%)の最大値

  public static int gifWaitingTime;  //待ち時間(s)
  public static int gifRecordingTime;  //録画時間(s)
  public static int gifMagnification;  //倍率(%)
  public static Object gifInterpolation;  //補間アルゴリズム

  public static SpinnerNumberModel gifWaitingTimeModel;  //待ち時間(s)のスピナーモデル
  public static SpinnerNumberModel gifRecordingTimeModel;  //録画時間(s)のスピナーモデル
  public static SpinnerNumberModel gifMagnificationModel;  //倍率(%)のスピナーモデル

  public static JMenuItem gifStartRecordingMenuItem;  //録画開始メニューアイテム
  public static JMenu gifSettingsMenu;  //GIFアニメーション録画設定メニュー

  public static java.util.Timer gifTimer;  //出力用のスレッド

  public static int gifScreenWidth;  //pnlScreenWidthのコピー
  public static int gifScreenHeight;  //pnlScreenHeightのコピー
  public static int gifStretchWidth;  //pnlStretchWidthのコピー
  public static int gifStretchHeight;  //pnlStretchHeightのコピー
  public static int gifStereoscopicFactor;  //pnlStereoscopicFactorのコピー
  public static boolean gifStereoscopicOn;  //pnlStereoscopicOnのコピー
  public static int gifStereoscopicMethod;  //pnlStereoscopicMethodのコピー

  public static double gifDelayTime;  //フレームの間隔(10ms単位)
  public static int gifWaitingFrames;  //待ち時間のフレーム数
  public static int gifRecordingFrames;  //録画時間のフレーム数
  public static int[] gifBuffer;  //フレームバッファ
  public static int gifPointer;  //フレームバッファのポインタ
  public static int gifWaitingCounter;  //待ち時間のフレームカウンタ
  public static int gifRecordingCounter;  //録画時間のフレームカウンタ
  public static boolean gifNowRecording;  //true=録画中

  //gifInit ()
  //  初期化
  public static void gifInit () {

    gifWaitingTime = Math.max (GIF_WAITING_TIME_MIN, Math.min (GIF_WAITING_TIME_MAX, Settings.sgsGetInt ("gifwaitingtime")));
    gifRecordingTime = Math.max (GIF_RECORDING_TIME_MIN, Math.min (GIF_RECORDING_TIME_MAX, Settings.sgsGetInt ("gifrecordingtime")));
    gifMagnification = Math.max (GIF_MAGNIFICATION_MIN, Math.min (GIF_MAGNIFICATION_MAX, Settings.sgsGetInt ("gifmagnification")));
    switch (Settings.sgsGetString ("gifinterpolation").toLowerCase ()) {
    case "nearest":  //最近傍補間
      gifInterpolation = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
      break;
    case "bilinear":  //線形補間
      gifInterpolation = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
      break;
    case "bicubic":  //三次補間
      gifInterpolation = RenderingHints.VALUE_INTERPOLATION_BICUBIC;
      break;
    default:
      gifInterpolation = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
    }

    gifWaitingTimeModel = new SpinnerNumberModel (gifWaitingTime, GIF_WAITING_TIME_MIN, GIF_WAITING_TIME_MAX, 1);
    gifRecordingTimeModel = new SpinnerNumberModel (gifRecordingTime, GIF_RECORDING_TIME_MIN, GIF_RECORDING_TIME_MAX, 1);
    gifMagnificationModel = new SpinnerNumberModel (gifMagnification, GIF_MAGNIFICATION_MIN, GIF_MAGNIFICATION_MAX, 1);

    ButtonGroup interpolationGroup = new ButtonGroup ();

    gifStartRecordingMenuItem =
      Multilingual.mlnText (
        ComponentFactory.createMenuItem (
          "Start recording",
          new ActionListener () {
            @Override public void actionPerformed (ActionEvent ae) {
              gifStartRecording ();
            }
          }),
        "ja", "録画開始");

    gifSettingsMenu =
      Multilingual.mlnText (
        ComponentFactory.createMenu (
          "GIF animation recording settings",
          ComponentFactory.createHorizontalBox (
            Multilingual.mlnText (ComponentFactory.createLabel ("Waiting time"), "ja", "待ち時間"),
            Box.createHorizontalGlue ()
            ),
          ComponentFactory.createHorizontalBox (
            Box.createHorizontalStrut (20),
            ComponentFactory.createNumberSpinner (gifWaitingTimeModel, 4, new ChangeListener () {
              @Override public void stateChanged (ChangeEvent ce) {
                gifWaitingTime = gifWaitingTimeModel.getNumber ().intValue ();
              }
            }),
            Multilingual.mlnText (ComponentFactory.createLabel ("seconds"), "ja", "秒"),
            Box.createHorizontalGlue ()
            ),
          ComponentFactory.createHorizontalBox (
            Multilingual.mlnText (ComponentFactory.createLabel ("Recording time"), "ja", "録画時間"),
            Box.createHorizontalGlue ()
            ),
          ComponentFactory.createHorizontalBox (
            Box.createHorizontalStrut (20),
            ComponentFactory.createNumberSpinner (gifRecordingTimeModel, 4, new ChangeListener () {
              @Override public void stateChanged (ChangeEvent ce) {
                gifRecordingTime = gifRecordingTimeModel.getNumber ().intValue ();
              }
            }),
            Multilingual.mlnText (ComponentFactory.createLabel ("seconds"), "ja", "秒"),
            Box.createHorizontalGlue ()
            ),
          ComponentFactory.createHorizontalBox (
            Multilingual.mlnText (ComponentFactory.createLabel ("Magnification"), "ja", "倍率"),
            Box.createHorizontalGlue ()
            ),
          ComponentFactory.createHorizontalBox (
            Box.createHorizontalStrut (20),
            ComponentFactory.createNumberSpinner (gifMagnificationModel, 4, new ChangeListener () {
              @Override public void stateChanged (ChangeEvent ce) {
                gifMagnification = gifMagnificationModel.getNumber ().intValue ();
              }
            }),
            ComponentFactory.createLabel ("%"),
            Box.createHorizontalGlue ()
            ),
          ComponentFactory.createHorizontalSeparator (),
          Multilingual.mlnText (
            ComponentFactory.createRadioButtonMenuItem (
              interpolationGroup,
              gifInterpolation == RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR,
              "Nearest neighbor",
              new ActionListener () {
                @Override public void actionPerformed (ActionEvent ae) {
                  gifInterpolation = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
                }
              }),
            "ja", "最近傍補間"),
          Multilingual.mlnText (
            ComponentFactory.createRadioButtonMenuItem (
              interpolationGroup,
              gifInterpolation == RenderingHints.VALUE_INTERPOLATION_BILINEAR,
              "Bilinear",
              new ActionListener () {
                @Override public void actionPerformed (ActionEvent ae) {
                  gifInterpolation = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
                }
              }),
            "ja", "線形補間"),
          Multilingual.mlnText (
            ComponentFactory.createRadioButtonMenuItem (
              interpolationGroup,
              gifInterpolation == RenderingHints.VALUE_INTERPOLATION_BICUBIC,
              "Bicubic",
              new ActionListener () {
                @Override public void actionPerformed (ActionEvent ae) {
                  gifInterpolation = RenderingHints.VALUE_INTERPOLATION_BICUBIC;
                }
              }),
            "ja", "三次補間")
          ),
        "ja", "GIF アニメーション録画設定");

    gifTimer = new java.util.Timer ();
  }

  //gifTini ()
  //  後始末
  public static void gifTini () {
    gifTimer.cancel ();
    Settings.sgsPutInt ("gifwaitingtime", gifWaitingTime);
    Settings.sgsPutInt ("gifrecordingtime", gifRecordingTime);
    Settings.sgsPutInt ("gifmagnification", gifMagnification);
    Settings.sgsPutString ("gifinterpolation",
                           gifInterpolation == RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR ? "nearest" :
                           gifInterpolation == RenderingHints.VALUE_INTERPOLATION_BILINEAR ? "bilinear" :
                           gifInterpolation == RenderingHints.VALUE_INTERPOLATION_BICUBIC ? "bicubic" :
                           "bilinear");
  }

  //gifStartRecording ()
  //  録画開始
  public static void gifStartRecording () {
    if (gifNowRecording) {  //録画中
      return;
    }
    //設定をコピーする
    gifScreenWidth = XEiJ.pnlScreenWidth;
    gifScreenHeight = XEiJ.pnlScreenHeight;
    gifStretchWidth = XEiJ.pnlStretchWidth;
    gifStretchHeight = XEiJ.pnlStretchHeight;
    gifStereoscopicFactor = XEiJ.pnlStereoscopicFactor;
    gifStereoscopicOn = XEiJ.pnlStereoscopicOn;
    gifStereoscopicMethod = XEiJ.pnlStereoscopicMethod;
    //フレームレートを求める
/*
    int htotal = CRTC.crtR00HFrontEndCurr + 1;
    int vtotal = CRTC.crtR04VFrontEndCurr + 1;
    if (htotal <= 0 || vtotal <= 0) {
      return;
    }
    int k = CRTC.crtHRLCurr << 3 | CRTC.crtHighResoCurr << 2 | CRTC.crtHResoCurr;
    double osc = (double) CRTC.crtFreqs[CRTC.CRT_OSCS[k]];
    int ratio = CRTC.CRT_DIVS[k];
    double hfreq = osc / (ratio * htotal << 3);
    double vfreq = hfreq / vtotal;
    gifDelayTime = 100.0 / vfreq;  //10ms単位
*/
    gifDelayTime = Math.max (2.0, (double) CRTC.crtTotalLength * 1e-1 + (double) CRTC.crtTotalLengthMNP * 1e-10);  //10ms単位、最小20ms
    //フレーム数を求める
/*
    gifWaitingFrames = (int) Math.floor (vfreq * (double) gifWaitingTime + 0.5);
    gifRecordingFrames = (int) Math.floor (vfreq * (double) gifRecordingTime + 0.5);
*/
    gifWaitingFrames = (int) Math.floor ((double) gifWaitingTime * 100.0 / gifDelayTime + 0.5);
    gifRecordingFrames = (int) Math.floor ((double) gifRecordingTime * 100.0 / gifDelayTime + 0.5);

    int fullSize = gifScreenWidth * gifScreenHeight * gifStereoscopicFactor;
    int maxNumberOfFrames = 0x7fffffff / fullSize;
    if (maxNumberOfFrames < gifRecordingFrames) {
      return;
    }
    //バッファを確保する
    int bufferSize = fullSize * gifRecordingFrames;
    try {
      gifBuffer = new int[bufferSize];
    } catch (OutOfMemoryError oome) {
      oome.printStackTrace ();
      return;
    }
    //録画開始
    gifStartRecordingMenuItem.setEnabled (false);
    gifPointer = 0;
    gifWaitingCounter = 0;
    gifRecordingCounter = 0;
    gifNowRecording = true;
    CRTC.crtCaptureClock = XEiJ.mpuClockTime;
    CRTC.crtFrameTaskClock = Math.min (CRTC.crtContrastClock, CRTC.crtCaptureClock);
  }

  //gifCaptureFrame ()
  //  フレームを取り込む
  public static void gifCaptureFrame () {
    if (gifWaitingCounter < gifWaitingFrames) {
      gifWaitingCounter++;
      return;
    }
    //ビットマップからバッファへコピーする
    if (XEiJ.PNL_USE_THREAD) {
      int[] bitmap = XEiJ.pnlBMLeftArray[XEiJ.pnlBMWrite & 3];
      for (int y = 0; y < gifScreenHeight; y++) {
        System.arraycopy (bitmap, XEiJ.PNL_BM_WIDTH * y,
                          gifBuffer, gifPointer,
                          gifScreenWidth);
        gifPointer += gifScreenWidth;
      }
      if (gifStereoscopicFactor == 2) {
        bitmap = XEiJ.pnlBMRightArray[XEiJ.pnlBMWrite & 3];
        for (int y = 0; y < gifScreenHeight; y++) {
          System.arraycopy (bitmap, XEiJ.PNL_BM_WIDTH * y,
                            gifBuffer, gifPointer,
                            gifScreenWidth);
          gifPointer += gifScreenWidth;
        }
      }
    } else {
      for (int y = 0; y < gifScreenHeight; y++) {
        System.arraycopy (XEiJ.pnlBMLeft, XEiJ.PNL_BM_WIDTH * y,
                          gifBuffer, gifPointer,
                          gifScreenWidth);
        gifPointer += gifScreenWidth;
      }
      if (gifStereoscopicFactor == 2) {
        for (int y = 0; y < gifScreenHeight; y++) {
          System.arraycopy (XEiJ.pnlBMRight, XEiJ.PNL_BM_WIDTH * y,
                            gifBuffer, gifPointer,
                            gifScreenWidth);
          gifPointer += gifScreenWidth;
        }
      }
    }
    gifRecordingCounter++;
    if (gifRecordingCounter == gifRecordingFrames) {
      //録画終了
      CRTC.crtCaptureClock = XEiJ.FAR_FUTURE;
      CRTC.crtFrameTaskClock = Math.min (CRTC.crtContrastClock, CRTC.crtCaptureClock);
      //別スレッドで圧縮してファイルに出力する
      gifTimer.schedule (new TimerTask () {
        @Override public void run () {
          gifOutput ();
        }
      }, 0L);
    }
  }

  //gifOutput ()
  //  圧縮してファイルに出力する
  public static void gifOutput () {
    System.out.println (Multilingual.mlnJapanese ? "画像を圧縮しています" : "Compressing images");
    //サイズ
    double zoomRatio = (double) gifMagnification / 100.0;
    int zoomWidth = (int) Math.floor ((double) gifStretchWidth * zoomRatio + 0.5);
    int zoomHeight = (int) Math.floor ((double) gifStretchHeight * zoomRatio + 0.5);
    //入力画像
    BufferedImage imageLeft = new BufferedImage (XEiJ.PNL_BM_WIDTH, gifScreenHeight, BufferedImage.TYPE_INT_RGB);
    BufferedImage imageRight = new BufferedImage (XEiJ.PNL_BM_WIDTH, gifScreenHeight, BufferedImage.TYPE_INT_RGB);
    int[] bmLeft = ((DataBufferInt) imageLeft.getRaster ().getDataBuffer ()).getData ();
    int[] bmRight = ((DataBufferInt) imageRight.getRaster ().getDataBuffer ()).getData ();
    //出力画像
    BufferedImage image = new BufferedImage (zoomWidth * gifStereoscopicFactor, zoomHeight, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2 = image.createGraphics ();
    g2.setRenderingHint (RenderingHints.KEY_INTERPOLATION, gifInterpolation);
    //イメージライタ
    ImageWriter imageWriter = ImageIO.getImageWritersBySuffix ("gif").next ();
    ImageWriteParam writeParam = imageWriter.getDefaultWriteParam ();
    try {
      //ファイル名を決める
      String dirName = "capture";
      File dir = new File (dirName);
      if (dir.exists () ? !dir.isDirectory () : !dir.mkdir ()) {  //ディレクトリを作れない
        gifBuffer = null;
        gifNowRecording = false;
        gifStartRecordingMenuItem.setEnabled (true);
        return;
      }
      HashSet<String> nameSet = new HashSet<String> ();  //ディレクトリにあるファイル名のセット
      for (String name : dir.list ()) {
        nameSet.add (name);
      }
      int number = 0;
      String name;
      do {
        number++;
        name = number + ".gif";
      } while (!nameSet.add (name));  //セットにない番号を探す
      name = dirName + "/" + name;
      //出力開始
      File file = new File (name);
      if (file.exists ()) {  //ないはず
        file.delete ();
      }
      ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream (file);
      imageWriter.setOutput (imageOutputStream);
      imageWriter.prepareWriteSequence (null);
      //フレーム毎の処理
      int halfSize = gifScreenWidth * gifScreenHeight;
      int fullSize = halfSize * gifStereoscopicFactor;

      double lastTime = 0.0;  //前回の終了時刻(10ms単位)
      double lastIntTime = 0.0;  //前回の終了時刻を四捨五入した時刻(10ms単位)

      for (int counter = 0; counter < gifRecordingFrames; ) {
        int pointer = fullSize * counter;
        //同じフレームをまとめる
        int span = 1;
        while (counter + span < gifRecordingFrames &&
               Arrays.equals (gifBuffer, pointer, pointer + fullSize,
                              gifBuffer, pointer + fullSize * span, pointer + fullSize * (span + 1))) {
          span++;
        }
        counter += span;

        double time = lastTime + gifDelayTime * (double) span;  //今回の終了時刻(10ms単位)
        double intTime = Math.floor (time + 0.5);  //今回の終了時刻を四捨五入した時刻(10ms単位)

        IIOMetadata metadata = makeMetadata (imageWriter,
                                             writeParam,
                                             image,
/*
                                             String.valueOf ((int) Math.floor (gifDelayTime * (double) span + 0.5)));
*/
                                             String.valueOf ((int) (intTime - lastIntTime)));
        lastTime = time;
        lastIntTime = intTime;

        //バッファからビットマップへコピーする
        for (int y = 0; y < gifScreenHeight; y++) {
          System.arraycopy (gifBuffer, pointer,
                            bmLeft, XEiJ.PNL_BM_WIDTH * y,
                            gifScreenWidth);
          pointer += gifScreenWidth;
        }
        if (gifStereoscopicFactor == 2) {
          for (int y = 0; y < gifScreenHeight; y++) {
            System.arraycopy (gifBuffer, pointer,
                              bmRight, XEiJ.PNL_BM_WIDTH * y,
                              gifScreenWidth);
            pointer += gifScreenWidth;
          }
        }
        //ビットマップを使って画面を再構築する
        g2.setColor (Color.black);
        g2.fillRect (0, 0, zoomWidth * gifStereoscopicFactor, zoomHeight);
        if (XEiJ.PNL_STEREOSCOPIC_ON && gifStereoscopicOn) {  //立体視ON
          if (gifStereoscopicMethod == XEiJ.PNL_NAKED_EYE_CROSSING) {
            g2.drawImage (imageRight,
                          0, 0, zoomWidth, zoomHeight,
                          0, 0, gifScreenWidth, gifScreenHeight,
                          null);
            g2.drawImage (imageLeft,
                          zoomWidth, 0, zoomWidth * 2, zoomHeight,
                          0, 0, gifScreenWidth, gifScreenHeight,
                          null);
          } else if (gifStereoscopicMethod == XEiJ.PNL_NAKED_EYE_PARALLEL ||
                     gifStereoscopicMethod == XEiJ.PNL_SIDE_BY_SIDE) {
            g2.drawImage (imageLeft,
                          0, 0, zoomWidth, zoomHeight,
                          0, 0, gifScreenWidth, gifScreenHeight,
                          null);
            g2.drawImage (imageRight,
                          zoomWidth, 0, zoomWidth * 2, zoomHeight,
                          0, 0, gifScreenWidth, gifScreenHeight,
                          null);
          } else {  //gifStereoscopicMethod == XEiJ.PNL_TOP_AND_BOTTOM
            g2.drawImage (imageLeft,
                          0, 0, zoomWidth, zoomHeight >> 1,
                          0, 0, gifScreenWidth, gifScreenHeight,
                          null);
            g2.drawImage (imageRight,
                          0, zoomHeight >> 1, zoomWidth, zoomHeight,
                          0, 0, gifScreenWidth, gifScreenHeight,
                          null);
          }
        } else {  //立体視OFF
          g2.drawImage (imageLeft,
                        0, 0, zoomWidth, zoomHeight,
                        0, 0, gifScreenWidth, gifScreenHeight,
                        null);
        }
        //ファイルに出力する
        imageWriter.writeToSequence (new IIOImage (image, null, metadata), writeParam);
      }
      //出力終了
      imageWriter.endWriteSequence ();
      imageOutputStream.close ();
      System.out.println (Multilingual.mlnJapanese ? name + " を更新しました" : name + " was updated");
    } catch (IOException ioe) {
      ioe.printStackTrace ();
    }
    gifBuffer = null;
    gifNowRecording = false;
    gifStartRecordingMenuItem.setEnabled (true);
  }

  public static IIOMetadata makeMetadata (ImageWriter imageWriter, ImageWriteParam writeParam, BufferedImage image, String delayTime) {
    IIOMetadata metadata = imageWriter.getDefaultImageMetadata (new ImageTypeSpecifier (image), writeParam);
    String metaFormat = metadata.getNativeMetadataFormatName ();
    Node root = metadata.getAsTree (metaFormat);
    IIOMetadataNode gce = new IIOMetadataNode ("GraphicControlExtension");
    gce.setAttribute ("delayTime", delayTime);
    gce.setAttribute ("disposalMethod", "none");
    gce.setAttribute ("transparentColorFlag", "FALSE");
    gce.setAttribute ("transparentColorIndex", "0");
    gce.setAttribute ("userInputFlag", "FALSE");
    root.appendChild (gce);
    try {
      metadata.setFromTree (metaFormat, root);
    } catch (IIOInvalidTreeException ite) {
      ite.printStackTrace ();
    }
    return metadata;
  }

}
