//========================================================================================
//  FlashingLights.java
//    en:Flashing lights reduction
//    ja:点滅光の軽減
//  Copyright (C) 2003-2026 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.awt.event.*;  //ActionListener
import java.util.*;  //Arrays
import javax.swing.*;  //JMenu
import javax.swing.event.*;  //ChangeListener

//点滅光の軽減

//class FlashingLights
public class FlashingLights {

  public static final boolean FLR_ON = true;
  public static final boolean FLR_DEBUG = false;

  //デフォルト
  public static final boolean FLR_ENABLED = false;
  public static final boolean FLR_PALETTE = true;
  public static final boolean FLR_CONTRAST = true;
  public static final boolean FLR_SCREEN = true;
  public static final int FLR_LIMIT = 13;
  public static final int FLR_WAVELENGTH = 20;
  public static final int FLR_DELAY = 20;
  public static final boolean FLR_INFORMED = false;

  //設定
  //  メニューで変更できるもの
  public static boolean flrRequestEnabled;  //点滅光を軽減する
  public static boolean flrRequestPalette;  //パレットによる点滅を止める
  public static boolean flrContrast;  //コントラストを制限する
  public static boolean flrRequestScreen;  //画面を徐々に明るくする
  public static int flrWavelength;  //パレットによる点滅の波長
  public static int flrLimit;  //コントラストの上限
  public static int flrDelay;  //画面が明るくなるまでの時間
  public static boolean flrInformed;  //案内済み
  //  flrVsyncで反映させるもの
  //  変更するとき初期化が必要なものなど
  public static boolean flrEnabled;
  public static boolean flrPalette;
  public static boolean flrScreen;
  //  flrVsyncを駆動させる条件
  //  flrEnabled || flrRequestEnabled
  public static boolean flrActive;

  //デバッグ
  public static boolean flrDebugLocation;  //観測点を表示する

  //時刻
  //  リングバッファの次に書き込む位置
  //  パレットが変化した時刻
  //  flrVsyncでインクリメントする
  public static int flrTime;

  //パレット
  //  ゴースト
  public static final int[] flrGhostG8 = new int[256];
  public static final int[] flrGhostTS = new int[256];
  //  前回と前々回のデータと時刻
  //  0=前回のgrbi,1=前回のtime,1=前々回のgrbi,2=前々回のtime
  public static final int[] flrHistoryG8 = new int[4 * 256];
  public static final int[] flrHistoryTS = new int[4 * 256];

  //画面
  //  観測点の座標
  public static final int[] flrIndex = new int[256];
  //  観測点のデータを保存するリングバッファ
  //  データは7*R+6*G+B
  //  有効なデータの位置はflrTime-flrDelay～flrTime-1
  public static final int FLR_RING_SIZE = 64;  //2の累乗
  public static final int[] flrRing = new int[FLR_RING_SIZE];
  //  リングバッファにあるデータの合計
  public static int flrTotal;
  //  画面が有効になった
  //  観測点の配置が変わった
  //  flrVsyncで画面の情報をリセットする
  public static boolean flrResetScreen;
  //  画面の明るさ
  public static float[] flrBrightness = new float[4];

  //メニュー
  public static JCheckBoxMenuItem flrMenuEnabled;
  public static JCheckBoxMenuItem flrMenuPalette;
  public static JCheckBoxMenuItem flrMenuContrast;
  public static JCheckBoxMenuItem flrMenuScreen;
  public static DecimalSpinner flrMenuWavelength;
  public static DecimalSpinner flrMenuLimit;
  public static DecimalSpinner flrMenuDelay;

  //flrInit ()
  //  初期化
  public static void flrInit () {
    //設定
    flrRequestEnabled = Settings.sgsGetOnOff ("flrenabled", FLR_ENABLED);
    flrRequestPalette = Settings.sgsGetOnOff ("flrpalette", FLR_PALETTE);
    flrContrast = Settings.sgsGetOnOff ("flrcontrast", FLR_CONTRAST);
    flrRequestScreen = Settings.sgsGetOnOff ("flrscreen", FLR_SCREEN);
    flrWavelength = Settings.sgsGetInt ("flrwavelength", FLR_WAVELENGTH, 1, 30);
    flrLimit = Settings.sgsGetInt ("flrlimit", FLR_LIMIT, 0, 15);
    flrDelay = Settings.sgsGetInt ("flrdelay", FLR_DELAY, 1, 60);
    flrInformed = Settings.sgsGetOnOff ("flrinformed", FLR_INFORMED);
    flrEnabled = false;
    flrPalette = false;
    flrScreen = false;
    flrActive = flrEnabled || flrRequestEnabled;
    //デバッグ
    flrDebugLocation = false;
    //時刻
    flrTime = 0;
    //パレット
    Arrays.fill (flrGhostG8, 0);
    Arrays.fill (flrGhostTS, 0);
    Arrays.fill (flrHistoryG8, 0);
    Arrays.fill (flrHistoryTS, 0);
    //画面
    Arrays.fill (flrIndex, 0);
    Arrays.fill (flrRing, 0);
    flrTotal = 0;
    flrResetScreen = false;
    Arrays.fill (flrBrightness, 1.0F);
    //観測点の座標を作る
    //  pnlInitより後
    flrMakeIndex ();
  }  //flrInit

  //flrInform ()
  //  案内を表示する
  public static void flrInform () {
    if (!flrInformed) {
      int result = JOptionPane.showConfirmDialog (
        XEiJ.frmFrame,
        Multilingual.mlnJapanese ?
        "光過敏性発作予防のための点滅光の軽減を有効にしますか？\n" +
        "この機能を有効にすると、エミュレーションの正確さよりも安全性が優先されます。\n" +
        "設定はアクセシビリティメニューから変更できます。" :
        "Do you want to enable reducing flashing lights to prevent photosensitive seizures?\n" +
        "Enabling this feature prioritizes safety over emulation accuracy.\n" +
        "You can change the settings in the accessibility menu.",
        Multilingual.mlnJapanese ? "点滅光の軽減" : "Flashing lights reduction",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.PLAIN_MESSAGE);
      if (result == JOptionPane.YES_OPTION) {
        flrInitialize (true);
        flrInformed = true;
      } else if (result == JOptionPane.NO_OPTION) {
        flrInitialize (false);
        flrInformed = true;
      }
    }
  }  //flrInform

  //flrTini ()
  //  後始末
  public static void flrTini () {
    Settings.sgsPutOnOff ("flrenabled", flrRequestEnabled);
    Settings.sgsPutOnOff ("flrpalette", flrRequestPalette);
    Settings.sgsPutOnOff ("flrcontrast", flrContrast);
    Settings.sgsPutOnOff ("flrscreen", flrRequestScreen);
    Settings.sgsPutInt ("flrwavelength", flrWavelength);
    Settings.sgsPutInt ("flrlimit", flrLimit);
    Settings.sgsPutInt ("flrdelay", flrDelay);
    Settings.sgsPutOnOff ("flrinformed", flrInformed);
  }  //flrTini

  //flrMakeIndex ()
  //  観測点の座標を作る
  //  細い直線を検出しないように格子点からずらす
  //  XEiJ.pnlScreenWidth,XEiJ.pnlScreenHeightを使う
  //  flrInitと、flrEnabledのときXEiJ.pnlUpdateArrangementが呼ぶ
  public static void flrMakeIndex () {
    int w = XEiJ.pnlScreenWidth;
    int h = XEiJ.pnlScreenHeight;
    int r = 26389;
    for (int n = 0; n < 256; n++) {  //YX
      r = (char) (15625 * r + 1);
      int o = r >> 8;
      int x = (w >> 9) + (((((n & 15) << 4) | (o & 15)) * w) >> 8);
      int y = (h >> 9) + ((((n & 240) | (o >> 4)) * h) >> 8);
      flrIndex[n] = x + (y << 10);
    }  //for n
    flrResetScreen = true;
  }  //flrMakeIndex

  //リスク
  //  輝度
  //    y = 0.2126*r + 0.7152*g + 0.0722*b
  //    Rec.709  https://ja.wikipedia.org/wiki/Rec._709
  //  輝度の係数は大雑把に言うと3:10:1くらい
  //    y = (3*r + 10*g + b)/14
  //  今回の目的は光過敏性発作対策なので赤の危険度を高く見積もって7:6:1とする
  //    risk = (7*r + 6*g + b)/14
  //  grbiのとき
  //    risk = (7*(2*r+i) + 6*(2*g+i) + 2*b+i)/14
  //         = (7*r + 6*g + b + 7*i)/7
  //  (iが使われない可能性は考慮しない)
  //
  //  矩形波の山と谷はriskによって明確に区別できなければならない
  //  どちらも大きくないと判断して止めないと矩形波が素通りしてしまい
  //  どちらも大きいと判断して止めると逆位相の矩形波が出力されてしまう
  //  条件分岐を増やしたくないので係数を大きくしてriskの衝突を避ける
  //  雑に探したらすぐ見つかった
  //  以下の式の分子はr,g,bそれぞれ8bitの組み合わせ16777216通りで異なる値になる
  //    risk = (70000*r + 60037*g + 10001*b)/140038
  //    perl -e "$v=qq(\0)x(255*140039+1);for$r(0..255){for$g(0..255){for$b(0..255){$i=70000*$r+60037*$g+10001*$b;vec($v,$i,8)and die;vec($v,$i,8)=1;}}}"
  //  grbiのときも6bitなので同じ式が使える
  //    risk = (70000*(2*r+i) + 60037*(2*g+i) + 10001*(2*b+i))/140038
  //         = (70000*r + 60037*g + 10001*b + 70019*i)/70019

  //flrRiskOfGRBI (grbi)
  //  grbiのrisk
  //  矩形波の条件が先にあるときは使われる機会が少ない
  //  任意の波形を対象にするときはテーブルにする
  public static int flrRiskOfGRBI (int grbi) {
    return (70000 * ((grbi >> 6) & 31) +  //R
            60037 * ((grbi >> 11) & 31) +  //G
            10001 * ((grbi >> 1) & 31) +  //B
            70019 * (grbi & 1));  //I
  }  //flrRiskOfGRBI

  //flrWriteByteG8 (a, grbi)
  //flrWriteWordG8 (a, grbi)
  //  グラフィックパレットに書き込む
  //  High→Low→Highの書き込みを矩形波とみなして2回目のHighの出力を抑制する
  //  memmoveなどでバイト転送で書き込まれると検出できない
  //  MemoryMappedDevice.MMD_VCNが呼ぶ
  public static void flrWriteByteG8 (int a, int d) {
    int t = VideoController.vcnPal16G8Port[(a >> 1) & 255];
    if ((a & 1) == 0) {
      flrWriteWordG8 (a, (d << 8) | (t & 255));
    } else {
      flrWriteWordG8 (a - 1, (t & 65280) | (d & 255));
    }
  }  //flrWriteByteG8
  public static void flrWriteWordG8 (int a, int d) {
    int n = (a >> 1) & 255;
    int grbi = (char) d;
    VideoController.vcnPal16G8Port[n] = grbi;
    if ((n & 1) == 0) {  //a=0,4,8,12 n=0,2,4,6 n+1=1,3,5,7
      VideoController.vcnPal8G16LPort[n] = grbi >> 8;
      VideoController.vcnPal8G16LPort[n + 1] = grbi & 255;
    } else {  //a=2,4,6,8 n-1=0,2,4,6 n=1,3,5,7
      VideoController.vcnPal8G16HPort[n - 1] = grbi & 65280;
      VideoController.vcnPal8G16HPort[n] = (grbi & 255) << 8;
    }
    if (flrPalette) {
      int grbi1 = flrHistoryG8[4 * n];  //前回
      if (grbi1 != grbi) {  //前回と今回のgrbiが違う
        int time1 = flrHistoryG8[4 * n + 1];
        int grbi2 = flrHistoryG8[4 * n + 2];  //前々回
        int time2 = flrHistoryG8[4 * n + 3];
        int grbiOut = grbi;  //今回のgrbiを出力する
        if (grbi2 == grbi &&  //前々回と今回のgrbiが同じ
            (flrTime - time2) <= flrWavelength &&  //前々回から今回までの時間が波長以下
            flrRiskOfGRBI (grbi1) < flrRiskOfGRBI (grbi)) {  //前回より今回の方がriskが大きい
          grbiOut = grbi1;  //前回のgrbiを出力する
        }
        flrHistoryG8[4 * n] = grbi;  //今回
        flrHistoryG8[4 * n + 1] = flrTime;
        flrHistoryG8[4 * n + 2] = grbi1;  //前回
        flrHistoryG8[4 * n + 3] = time1;
        VideoController.vcnPal16G8[n] = grbiOut;
        if ((n & 1) == 0) {  //a=0,4,8,12 n=0,2,4,6 n+1=1,3,5,7
          VideoController.vcnPal8G16L[n] = grbiOut >> 8;
          VideoController.vcnPal8G16L[n + 1] = grbiOut & 255;
        } else {  //a=2,4,6,8 n-1=0,2,4,6 n=1,3,5,7
          VideoController.vcnPal8G16H[n - 1] = grbiOut & 65280;
          VideoController.vcnPal8G16H[n] = (grbiOut & 255) << 8;
        }
      }
    }
    VideoController.vcnPal32G8[n] = VideoController.vcnPalTbl[VideoController.vcnPal16G8[n]];
    if ((VideoController.vcnReg3Curr & 0x001f) != 0) {  //グラフィックが表示されている
      CRTC.crtAllStamp += 2;
    }
  }  //flrWriteWordG8

  //flrWriteByteTS (a, grbi)
  //flrWriteWordTS (a, grbi)
  //  テキストスプライトパレットに書き込む
  //  High→Low→Highの書き込みを矩形波とみなして2回目のHighの出力を抑制する
  //  memmoveなどでバイト転送で書き込まれると検出できない
  //  MemoryMappedDevice.MMD_VCNが呼ぶ
  public static void flrWriteByteTS (int a, int d) {
    int t = VideoController.vcnPal16TSPort[(a >> 1) & 255];
    if ((a & 1) == 0) {
      flrWriteWordTS (a, (d << 8) | (t & 255));
    } else {
      flrWriteWordTS (a - 1, (t & 65280) | (d & 255));
    }
  }  //flrWriteByteTS
  public static void flrWriteWordTS (int a, int d) {
    int n = (a >> 1) & 255;
    int grbi = (char) d;
    VideoController.vcnPal16TSPort[n] = grbi;
    if (flrPalette) {
      int grbi1 = flrHistoryTS[4 * n];  //前回
      if (grbi1 != grbi) {  //前回と今回のgrbiが違う
        int time1 = flrHistoryTS[4 * n + 1];
        int grbi2 = flrHistoryTS[4 * n + 2];  //前々回
        int time2 = flrHistoryTS[4 * n + 3];
        int grbiOut = grbi;  //今回のgrbiを出力する
        if (grbi2 == grbi &&  //前々回と今回のgrbiが同じ
            (flrTime - time2) <= flrWavelength &&  //前々回から今回までの時間が波長以下
            flrRiskOfGRBI (grbi1) < flrRiskOfGRBI (grbi)) {  //前回より今回の方がriskが大きい
          grbiOut = grbi1;  //前回のgrbiを出力する
        }
        flrHistoryTS[4 * n] = grbi;  //今回
        flrHistoryTS[4 * n + 1] = flrTime;
        flrHistoryTS[4 * n + 2] = grbi1;  //前回
        flrHistoryTS[4 * n + 3] = time1;
        VideoController.vcnPal16TS[n] = grbiOut;
      }
    }
    VideoController.vcnPal32TS[n] = VideoController.vcnPalTbl[VideoController.vcnPal16TS[n]];
    if (n < 16 ?
        (VideoController.vcnReg3Curr & 0x0020) != 0 :  //テキスト画面が表示されている
        (VideoController.vcnReg3Curr & 0x0040) != 0 &&
        (SpriteScreen.sprReg4BgCtrlCurr & 0x0200) != 0) {  //スプライト画面が表示されている
      CRTC.crtAllStamp += 2;
    }
  }  //flrWriteWordTS

  //flrUpdateContrast ()
  //  コントラストを更新する
  //  点滅光を軽減するが変更された
  //  コントラストを制限するが変更された
  //  コントラストの上限が変更された
  //  設定が初期化された
  //  表示モードテストでコントラストが変更された
  //  コントラストポートに書き込まれた
  public static void flrUpdateContrast () {
    int curr = (VideoController.vcnTargetContrastMask != 0 ?
                VideoController.vcnTargetContrastTest :  //表示モードテストが優先
                flrEnabled && flrContrast ?
                Math.min (flrLimit, VideoController.vcnTargetContrastPort) :
                VideoController.vcnTargetContrastPort);
    if (VideoController.vcnTargetContrastCurr != curr) {
      VideoController.vcnTargetContrastCurr = curr;
      VideoController.vcnTargetScaledContrast = VideoController.VCN_CONTRAST_SCALE * VideoController.vcnTargetContrastCurr;
      CRTC.crtContrastClock = XEiJ.mpuClockTime;
      CRTC.crtFrameTaskClock = Math.min (CRTC.crtContrastClock, CRTC.crtCaptureClock);
    }
  }  //flrUpdateContrast

  //flrVsync ()
  //  垂直帰線期間の開始
  //  flrActiveのときCRTCが呼ぶ
  public static void flrVsync () {
    //時刻を進める
    flrTime++;
    //パレットの設定を反映する
    if (flrPalette != (flrRequestEnabled && flrRequestPalette)) {
      flrPalette = (flrRequestEnabled && flrRequestPalette);
      //グラフィックパレット
      if (flrPalette) {
        //パレットをゴーストにコピーする
        System.arraycopy (VideoController.vcnPal16G8Port, 0,  //from
                          flrGhostG8, 0,  //to
                          256);  //length
        //ゴーストを見せる
        VideoController.vcnPal16G8 = flrGhostG8;
      } else {
        //元に戻す
        VideoController.vcnPal16G8 = VideoController.vcnPal16G8Port;
      }
      for (int n = 0; n < 256; n++) {
        int grbi = VideoController.vcnPal16G8[n];
        VideoController.vcnPal32G8[n] = VideoController.vcnPalTbl[grbi];
        if ((n & 1) == 0) {  //a=0,4,8,12 n=0,2,4,6 n+1=1,3,5,7
          VideoController.vcnPal8G16L[n] = grbi >> 8;
          VideoController.vcnPal8G16L[n + 1] = grbi & 255;
        } else {  //a=2,4,6,8 n-1=0,2,4,6 n=1,3,5,7
          VideoController.vcnPal8G16H[n - 1] = grbi & 65280;
          VideoController.vcnPal8G16H[n] = (grbi & 255) << 8;
        }
      }
      //テキストスプライトパレット
      if (flrPalette) {
        //パレットをゴーストにコピーする
        System.arraycopy (VideoController.vcnPal16TSPort, 0,  //from
                          flrGhostTS, 0,  //to
                          256);  //length
        if (!ScreenModeTest.smtPatternTestOn) {  //表示モードテストが優先
          //ゴーストを見せる
          VideoController.vcnPal16TS = flrGhostTS;
        }
      } else {
        //元に戻す
        if (!ScreenModeTest.smtPatternTestOn) {  //表示モードテストが優先
          VideoController.vcnPal16TS = VideoController.vcnPal16TSPort;
        }
      }
      for (int n = 0; n < 256; n++) {
        VideoController.vcnPal32TS[n] = VideoController.vcnPalTbl[VideoController.vcnPal16TS[n]];
      }
      if ((VideoController.vcnReg3Curr & 0x007f) != 0) {  //グラフィックまたはテキストスプライトが表示されている
        CRTC.crtAllStamp += 2;
      }
      //前回と前々回のデータと時刻
      Arrays.fill (flrHistoryG8, 0);
      Arrays.fill (flrHistoryTS, 0);
    }
    //画面の設定を反映する
    if (flrScreen != (flrRequestEnabled && flrRequestScreen)) {
      flrScreen = (flrRequestEnabled && flrRequestScreen);
      if (flrScreen) {
        flrResetScreen = true;
      }
    }
    //設定を反映する
    if (flrEnabled != flrRequestEnabled) {
      flrEnabled = flrRequestEnabled;
      flrActive = flrEnabled || flrRequestEnabled;
      flrUpdateContrast ();
    }
    if (!flrEnabled) {
      return;
    }
    //画面をリセットする
    if (flrResetScreen) {
      flrResetScreen = false;
      int sum = 0;
      for (int n = 0; n < 256; n++) {
        int argb = XEiJ.pnlBM[flrIndex[n]];
        sum += (7 * (((argb >> 16) & 255)) +
                6 * ((argb >> 8) & 255) +
                (argb & 255));
      }
      Arrays.fill (flrRing, sum);
      flrTotal = sum * flrDelay;
    }
    //画面を更新する
    if (flrScreen) {
      //観測点のデータを集める
      int sum = 0;
      for (int n = 0; n < 256; n++) {
        int argb = XEiJ.pnlBM[flrIndex[n]];
        if (FLR_DEBUG &&
            flrDebugLocation) {
          XEiJ.pnlBM[flrIndex[n]] = 0xff00ff00;
        }
        sum += (7 * (((argb >> 16) & 255)) +
                6 * ((argb >> 8) & 255) +
                (argb & 255));
      }
      //データの合計から最古のデータを引いて最新のデータを加える
      flrTotal += sum - flrRing[(flrTime - flrDelay) & (FLR_RING_SIZE - 1)];
      //最新のデータを保存する
      flrRing[flrTime & (FLR_RING_SIZE - 1)] = sum;
      //平均を求める
      float average = (float) flrTotal / (float) flrDelay;
      //明るさを調整する
      flrBrightness[XEiJ.pnlBMWrite & 3] = (float) sum <= average ? 1.0F : average / (float) sum;
    }
  }  //flrVsync

  //flrMakeMenu ()
  //  メニューを作る
  public static JMenu flrMakeMenu () {
    return Multilingual.mlnText (
      ComponentFactory.createMenu (
        "Flashing lights reduction",
        //点滅光を軽減する
        flrMenuEnabled = Multilingual.mlnText (
          ComponentFactory.createCheckBoxMenuItem (
            flrRequestEnabled, "Reduce flashing lights",
            new ActionListener () {
              @Override public void actionPerformed (ActionEvent ae) {
                flrRequestEnabled = ((JCheckBoxMenuItem) ae.getSource ()).isSelected ();
                flrActive = flrEnabled || flrRequestEnabled;
              }}),
          "ja", "点滅光を軽減する"),
        //パレットによる点滅を止める
        flrMenuPalette = Multilingual.mlnText (
          ComponentFactory.createCheckBoxMenuItem (
            flrRequestPalette, "Stop flashing with palette",
            new ActionListener () {
              @Override public void actionPerformed (ActionEvent ae) {
                flrRequestPalette = ((JCheckBoxMenuItem) ae.getSource ()).isSelected ();
              }}),
          "ja", "パレットによる点滅を止める"),
        //コントラストを制限する
        flrMenuContrast = Multilingual.mlnText (
          ComponentFactory.createCheckBoxMenuItem (
            flrContrast, "Limit contrast",
            new ActionListener () {
              @Override public void actionPerformed (ActionEvent ae) {
                flrContrast = ((JCheckBoxMenuItem) ae.getSource ()).isSelected ();
                flrUpdateContrast ();
              }}),
          "ja", "コントラストを制限する"),
        //画面を徐々に明るくする
        Multilingual.mlnText (
          flrMenuScreen = ComponentFactory.createCheckBoxMenuItem (
            flrRequestScreen, "Brighten screen slowly",
            new ActionListener () {
              @Override public void actionPerformed (ActionEvent ae) {
                flrRequestScreen = ((JCheckBoxMenuItem) ae.getSource ()).isSelected ();
              }}),
          "ja", "画面を徐々に明るくする"),
        //パレットによる点滅の波長
        ComponentFactory.createHorizontalBox (
          Box.createHorizontalStrut (20),
          flrMenuWavelength = ComponentFactory.createDecimalSpinner (
            flrWavelength, 1, 30, 1, 0,
            new ChangeListener () {
              @Override public void stateChanged (ChangeEvent ce) {
                flrWavelength = ((DecimalSpinner) ce.getSource ()).getIntValue ();
              }}),
          Multilingual.mlnText (
            ComponentFactory.createLabel (" Wavelength of flashing with palette"),
            "ja", "パレットによる点滅の波長"),
          Box.createHorizontalGlue ()),
        //コントラストの上限
        ComponentFactory.createHorizontalBox (
          Box.createHorizontalStrut (20),
          flrMenuLimit = ComponentFactory.createDecimalSpinner (
            flrLimit, 0, 15, 1, 0,
            new ChangeListener () {
              @Override public void stateChanged (ChangeEvent ce) {
                flrLimit = ((DecimalSpinner) ce.getSource ()).getIntValue ();
                flrUpdateContrast ();
              }}),
          Multilingual.mlnText (
            ComponentFactory.createLabel (" Upper limit of contrast"),
            "ja", "コントラストの上限"),
          Box.createHorizontalGlue ()),
        //画面が明るくなるまでの時間
        ComponentFactory.createHorizontalBox (
          Box.createHorizontalStrut (20),
          flrMenuDelay = ComponentFactory.createDecimalSpinner (
            flrDelay, 1, 60, 1, 0,
            new ChangeListener () {
              @Override public void stateChanged (ChangeEvent ce) {
                flrDelay = ((DecimalSpinner) ce.getSource ()).getIntValue ();
                flrResetScreen = true;
              }}),
          Multilingual.mlnText (
            ComponentFactory.createLabel (" Time until the screen brighten"),
            "ja", " 画面が明るくなるまでの時間"),
          Box.createHorizontalGlue ()),
        //初期値に戻す
        ComponentFactory.createHorizontalSeparator (),
        Multilingual.mlnText (
          ComponentFactory.createMenuItem (
            "Reset to default",
            new ActionListener () {
              @Override public void actionPerformed (ActionEvent ae) {
                flrResetToDefault ();
              }}),
          "ja", "初期値に戻す"),
        //デバッグ
        !FLR_DEBUG ? null :
        ComponentFactory.createHorizontalSeparator (),
        !FLR_DEBUG ? null :
        Multilingual.mlnText (
          ComponentFactory.createMenu (
            "Debug",
            Multilingual.mlnText (
              ComponentFactory.createCheckBoxMenuItem (
                flrDebugLocation, "Display locations",
                new ActionListener () {
                  @Override public void actionPerformed (ActionEvent ae) {
                    flrDebugLocation = ((JCheckBoxMenuItem) ae.getSource ()).isSelected ();
                  }}),
              "ja", "観測点を表示する")
            ),
          "ja", "デバッグ")
        ),
      "ja", "点滅光の軽減");
  }  //flrMakeMenu

  //flrResetToDefault ()
  //  初期値に戻す
  public static void flrResetToDefault () {
    int result = JOptionPane.showConfirmDialog (
      XEiJ.frmFrame,
      Multilingual.mlnJapanese ?
      "点滅光の軽減の設定を初期値に戻しますか？\n" +
      "この機能は一旦無効になります。":
      "Do you want to reset the flashing lights reduction settings to default?" +
      "This feature will be temporarily disabled.",
      Multilingual.mlnJapanese ? "点滅光の軽減" : "Flashing lights reduction",
      JOptionPane.YES_NO_OPTION,
      JOptionPane.PLAIN_MESSAGE);
    if (result == JOptionPane.YES_OPTION) {
      flrInitialize (FLR_ENABLED);
    }
  }  //flrResetToDefault

  //flrInitialize (enabled)
  //  初期化する
  public static void flrInitialize (boolean enabled) {
    flrMenuEnabled.setSelected (flrRequestEnabled = enabled);
    flrActive = flrEnabled || flrRequestEnabled;
    flrMenuPalette.setSelected (flrRequestPalette = FLR_PALETTE);
    flrMenuContrast.setSelected (flrContrast = FLR_CONTRAST);
    flrMenuScreen.setSelected (flrRequestScreen = FLR_SCREEN);
    flrMenuWavelength.setIntValue (flrWavelength = FLR_WAVELENGTH);
    flrMenuLimit.setIntValue (flrLimit = FLR_LIMIT);
    flrUpdateContrast ();
    flrMenuDelay.setIntValue (flrDelay = FLR_DELAY);
    flrInformed = false;
  }  //flrInitialize

}  //class FlashingLights
