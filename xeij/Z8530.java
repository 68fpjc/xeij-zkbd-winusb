//========================================================================================
//  Z8530.java
//    en:SCC -- Serial Communication Controller
//    ja:SCC -- シリアルコミュニケーションコントローラ
//  Copyright (C) 2003-2026 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//  マウス
//    マウスイベントとマウスモーションイベントで取得したデータを返す
//  RS-232C
//    ターミナルに接続
//----------------------------------------------------------------------------------------

package xeij;

import java.awt.event.*;
import java.util.*;
import javax.swing.*;

public class Z8530 {

  public static final boolean SCC_DEBUG_ON = false;
  public static int sccDebugOn;  //1=Port0B,2=Port1A,4=Interrupt

  //設定
  public static double sccFreq = 5000000.0;  //SCCの動作周波数

  //メニュー
  public static JMenu sccMenu;

  //ポート
  public static final int SCC_0B_COMMAND = 0x00e98001;
  public static final int SCC_0B_DATA    = 0x00e98003;
  public static final int SCC_1A_COMMAND = 0x00e98005;
  public static final int SCC_1A_DATA    = 0x00e98007;

  //割り込み
  //  ベクタ
  //    0  ポート0B送信バッファ空(マウス送信)
  //    1  ポート0B外部/ステータス変化
  //    2  ポート0B受信バッファフル(マウス受信)
  //    3  ポート0B特別受信条件
  //    4  ポート1A送信バッファ空(RS-232C送信)
  //    5  ポート1A外部/ステータス変化
  //    6  ポート1A受信バッファフル(RS-232C受信)
  //    7  ポート1A特別受信条件
  //  マスク
  //    0x80  常に0
  //    0x40  常に0
  //    0x20  ポート1A受信バッファフル(RS-232C受信)
  //    0x10  ポート1A送信バッファ空(RS-232C送信)
  //    0x08  ポート1A外部/ステータス変化
  //    0x04  ポート0B受信バッファフル(マウス受信)
  //    0x02  ポート0B送信バッファ空(マウス送信)
  //    0x01  ポート0B外部/ステータス変化
  //  優先順位
  //    高い  1A受信バッファフル(RS-232C受信)
  //          1A送信バッファ空(RS-232C送信)
  //          1A外部/ステータス変化
  //          0B受信バッファフル(マウス受信)
  //          0B送信バッファ空(マウス送信)
  //    低い  0B外部/ステータス変化
  //!!! マウス送信、外部/ステータス変化、特別受信条件の割り込みは未実装
  public static int sccInterruptVector;  //非修飾ベクタ。WR2
  public static int sccVectorInclude;  //WR9&0x11
  public static int sccMIE;  //Master Interrupt Enable。(WR9&0x08)!=0?-1:0
  //  マウス受信割り込み
  public static final int SCC_0B_RECEIVE_VECTOR = 2;
  public static final int SCC_0B_RECEIVE_MASK = 0x04;
  public static int scc0bReceiveMask;  //マスク
  public static int scc0bReceiveRR3;  //RR3のペンディングビット。割り込み発生でセット
  public static int scc0bReceiveRequest;  //リクエスト。割り込み発生でセット、受け付けでクリア
  public static int scc0bReceiveVector;  //修飾ベクタ
  //  RS-232C受信割り込み
  public static final int SCC_1A_RECEIVE_VECTOR = 6;
  public static final int SCC_1A_RECEIVE_MASK = 0x20;
  public static int scc1aReceiveMask;  //マスク
  public static int scc1aReceiveRR3;  //RR3のペンディングビット。割り込み発生でセット、IUSリセットでクリア
  public static int scc1aReceiveRequest;  //リクエスト。割り込み発生でセット、受け付けでクリア
  public static int scc1aReceiveVector;  //修飾ベクタ
  //  RS-232C送信割り込み
  public static final int SCC_1A_SEND_VECTOR = 4;
  public static final int SCC_1A_SEND_MASK = 0x10;
  public static int scc1aSendMask;  //マスク
  public static int scc1aSendRR3;  //RR3のペンディングビット。割り込み発生でセット
  public static int scc1aSendRequest;  //リクエスト。割り込み発生でセット、受け付けでクリア
  public static int scc1aSendVector;  //修飾ベクタ

  //ポート0B マウス
  public static int scc0bRegisterNumber;
  public static int scc0bRts;  //RTS(0または1)
  public static int scc0bBaudRateGen;  //WR13<<8|WR12
  public static int scc0bInputCounter;  //マウスデータのカウンタ。0～2
  public static int scc0bData1;
  public static int scc0bData2;

  public static final boolean SCC_FSX_MOUSE = true;  //true=SX-Windowをシームレスにする
  public static int sccFSXMouseHook;  //FSX.Xのマウス受信データ処理ルーチンのアドレス
  public static int sccFSXMouseWork;  //FSX.Xのマウスワークのアドレス

  //ポート1A RS-232C
  public static final int SCC_1A_INPUT_BITS = 16;
  public static final int SCC_1A_INPUT_SIZE = 1 << SCC_1A_INPUT_BITS;
  public static final int SCC_1A_INPUT_MASK = SCC_1A_INPUT_SIZE - 1;
  public static int scc1aRegisterNumber;
  public static final int[] scc1aInputBuffer = new int[SCC_1A_INPUT_SIZE];  //RS-232C受信バッファ。データはゼロ拡張済み
  public static int scc1aInputRead;  //RS-232C受信バッファから次に読み出すデータの位置。read==writeのときバッファエンプティ
  public static int scc1aInputWrite;  //RS-232C受信バッファに次に書き込むデータの位置。(write+1&SCC_1A_INPUT_MASK)==readのときバッファフル
  //  ボーレート
  public static int scc1aBRGEnable;  //WR14のbit0。true=ボーレートジェネレータ動作
  public static int scc1aClockModeShift;  //WR4のbit6-7。0=2^0,1=2^4,2=2^5,3=2^6
  public static int scc1aBaudRateGen;  //WR13<<8|WR12
  public static long scc1aInterval;  //転送間隔(XEiJ.TMR_FREQ単位)
  //  通信設定
  public static int scc1aRxBits;  //WR3のbit7-6。00=5bit,01=7bit,10=6bit,11=8bit。RR8の未使用ビットは1
  public static int scc1aRxEnable;  //WR3のbit0。1=enable
  public static int scc1aStop;  //WR4のbit3-2。01=s1,10=s1.5,11=s2
  public static int scc1aParity;  //WR4のbit1-0。x0=pn,01=po,11=pe
  public static int scc1aTxBits;  //WR5のbit6-5。00=1-5bit,01=7bit,10=6bit,11=8bit。1-5bitのとき1111000x,111000xx,11000xxx,1000xxxx,000xxxxx
  public static int scc1aTxEnable;  //WR5のbit3。1=enable


  //sccInit ()
  //  初期化
  public static void sccInit () {

    sccFreq = 5000000 < Settings.sgsGetInt ("sccfreq") ? 7372800.0 : 5000000.0;

    if (SCC_DEBUG_ON) {
      sccDebugOn = 2;
    }

    //scc1aInputBuffer = new int[SCC_1A_INPUT_SIZE];

    sccReset ();

  }  //sccInit()


  //sccTini ()
  //  後始末
  public static void sccTini () {
    Settings.sgsPutInt ("sccfreq", (int) sccFreq);
  }


  //sccMakeMenu ()
  public static JMenu sccMakeMenu () {
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        String command = ae.getActionCommand ();
        switch (command) {
        case "5MHz":
          sccFreq = 5000000.0;
          break;
        case "7.3728MHz":
          sccFreq = 7372800.0;
          break;
        case "Port 0B":
          if (SCC_DEBUG_ON) {
            sccDebugOn = ((sccDebugOn & ~1) |
                          (((JCheckBoxMenuItem) source).isSelected () ? 1 : 0));
          }
          break;
        case "Port 1A":
          if (SCC_DEBUG_ON) {
            sccDebugOn = ((sccDebugOn & ~2) |
                          (((JCheckBoxMenuItem) source).isSelected () ? 2 : 0));
          }
          break;
        case "Interrupt":
          if (SCC_DEBUG_ON) {
            sccDebugOn = ((sccDebugOn & ~4) |
                          (((JCheckBoxMenuItem) source).isSelected () ? 4 : 0));
          }
          break;
        default:
          System.out.println ("unknown action command " + command);
        }
      }
    };  //ActionListener
    ButtonGroup freqGroup = new ButtonGroup ();
    return ComponentFactory.createMenu (
      "SCC",
      ComponentFactory.createRadioButtonMenuItem (
        freqGroup,
        sccFreq == 5000000.0,
        "5MHz",
        listener),
      ComponentFactory.createRadioButtonMenuItem (
        freqGroup,
        sccFreq == 7372800.0,
        "7.3728MHz",
        listener),
      !SCC_DEBUG_ON ? null :
      ComponentFactory.createHorizontalSeparator (),
      !SCC_DEBUG_ON ? null :
      ComponentFactory.createMenu (
        "Debug",
        ComponentFactory.createCheckBoxMenuItem (
          (sccDebugOn & 1) != 0, "Port 0B", listener),
        ComponentFactory.createCheckBoxMenuItem (
          (sccDebugOn & 2) != 0, "Port 1A", listener),
        ComponentFactory.createCheckBoxMenuItem (
          (sccDebugOn & 4) != 0, "Interrupt", listener)
        )  //createMenu
      );  //createMenu
  }  //sccMakeMenu


  //リセット
  public static void sccReset () {
    //割り込み
    sccInterruptVector = 0x00;
    sccVectorInclude = 0x00;
    sccMIE = 0;
    scc0bReceiveMask = 0;
    scc0bReceiveRR3 = 0;
    scc0bReceiveRequest = 0;
    scc0bReceiveVector = 0;
    scc1aReceiveMask = 0;
    scc1aReceiveRR3 = 0;
    scc1aReceiveRequest = 0;
    scc1aReceiveVector = 0;
    scc1aSendMask = 0;
    scc1aSendRR3 = 0;
    scc1aSendRequest = 0;
    scc1aSendVector = 0;
    //マウス
    scc0bRegisterNumber = 0;
    scc0bRts = 0;
    scc0bBaudRateGen = 31;  //4800bps。(5000000/2/16)/4800-2=30.552。(5000000/2/16)/(31+2)=4734.848=4800*0.986
    scc0bInputCounter = 0;
    scc0bData1 = 0;
    scc0bData2 = 0;
    if (SCC_FSX_MOUSE) {
      sccFSXMouseHook = 0;
      sccFSXMouseWork = 0;
    }
    //RS-232C
    scc1aRegisterNumber = 0;
    Arrays.fill (scc1aInputBuffer, 0);
    scc1aInputRead = 0;
    scc1aInputWrite = 0;
    scc1aRxBits = 3;  //b8
    scc1aRxEnable = 0;
    scc1aStop = 1;  //s1
    scc1aParity = 0;  //pn
    scc1aTxBits = 3;
    scc1aTxEnable = 0;
    scc1aBRGEnable = 0;
    scc1aClockModeShift = 4;  //1/16
    scc1aBaudRateGen = 14;  //9600bps
    double rate = sccFreq / (double) ((scc1aBaudRateGen + 2) << (scc1aClockModeShift + 1));
    double bits = (1.0 +  //start
                   (scc1aRxBits == 0 ? 5.0 :
                    scc1aRxBits == 1 ? 7.0 :
                    scc1aRxBits == 2 ? 6.0 : 8.0) +  //data
                   ((scc1aParity & 1) == 0 ? 0.0 : 1.0) +  //parity
                   (scc1aStop == 0 ? 0.0 :
                    scc1aStop == 1 ? 1.0 :
                    scc1aStop == 2 ? 1.5 : 2.0));  //stop
    double interval = bits / rate;
    scc1aInterval = Math.round (interval * (double) XEiJ.TMR_FREQ);
    //
    RS232CTerminal.trmAUXReset ();
  }  //sccReset()

  //割り込み受付
  //  コアが割り込み要求を受け付けたときに呼び出す
  //  割り込みベクタ番号を返す
  //  割り込み要求を取り下げる場合は0を返す
  //  オートベクタを使用するデバイスはオートベクタの番号を返すこと
  public static int sccAcknowledge () {
    int d = 0;
    //優先順位は固定
    if (scc1aReceiveRequest != 0) {  //1A受信バッファフル(RS-232C受信)
      scc1aReceiveRequest = 0;
      d = scc1aReceiveVector;
    } else if (scc1aSendRequest != 0) {  //1A送信バッファ空(RS-232C送信)
      scc1aSendRequest = 0;
      d = scc1aSendVector;
    } else if (scc0bReceiveRequest != 0) {  //0B受信バッファフル(マウス受信)
      scc0bReceiveRequest = 0;
      d = scc0bReceiveVector;
    }
    if (SCC_DEBUG_ON && (sccDebugOn & 4) != 0) {
      System.out.printf ("%08x sccAcknowledge()=0x%02x\n", XEiJ.regPC0, d);
    }
    return d;
  }  //sccAcknowledge()

  //割り込み終了
  //  コアが割り込み処理を終了したときに呼び出す
  //  まだ処理されていない割り込みが残っていたら再度割り込み要求を出す
  public static void sccDone () {
    if (SCC_DEBUG_ON && (sccDebugOn & 4) != 0) {
      System.out.printf ("%08x sccDone()\n", XEiJ.regPC0);
    }
    if ((scc1aReceiveRequest | scc1aSendRequest | scc0bReceiveRequest) != 0) {
      XEiJ.mpuIRR |= XEiJ.MPU_SCC_INTERRUPT_MASK;
    }
  }  //sccDone()

  //sccUpdateVector ()
  //  scc0bReceiveVector,scc1aReceiveVector,scc1aSendVectorを更新する
  //  sccInterruptVector,sccVectorIncludeを更新したら呼び出す
  public static void sccUpdateVector () {
    if (sccVectorInclude == 0x00) {  //(WR9&0x01)==0x00
      scc0bReceiveVector = sccInterruptVector;
      scc1aReceiveVector = sccInterruptVector;
      scc1aSendVector    = sccInterruptVector;
    } else if (sccVectorInclude == 0x01) {  //(WR9&0x11)==0x01
      int t = sccInterruptVector & 0b11110001;
      scc0bReceiveVector = t | SCC_0B_RECEIVE_VECTOR << 1;
      scc1aReceiveVector = t | SCC_1A_RECEIVE_VECTOR << 1;
      scc1aSendVector    = t | SCC_1A_SEND_VECTOR << 1;
    } else {  //(WR9&0x11)==0x11
      int t = sccInterruptVector & 0b10001111;
      scc0bReceiveVector = t | SCC_0B_RECEIVE_VECTOR << 4;
      scc1aReceiveVector = t | SCC_1A_RECEIVE_VECTOR << 4;
      scc1aSendVector    = t | SCC_1A_SEND_VECTOR << 4;
    }
    if (SCC_DEBUG_ON && (sccDebugOn & 4) != 0) {
      System.out.printf ("scc0bReceiveVector=0x%02x\n", scc0bReceiveVector);
      System.out.printf ("scc1aReceiveVector=0x%02x\n", scc1aReceiveVector);
      System.out.printf ("scc1aSendVector=0x%02x\n", scc1aSendVector);
    }
  }  //sccUpdateVector()

  //d = sccReadByte (a, peek)
  //  リードバイト
  public static int sccReadByte (int a, boolean peek) {
    int d = 0;
    switch (a & 7) {
      //------------------------------------------------
    case SCC_0B_COMMAND & 7:  //ポート0Bコマンド読み出し
      switch (scc0bRegisterNumber) {
      case 0:  //RR0
        //  0x80  ブレークまたはアボート
        //  0x40  送信アンダーラン
        //  0x20  CTS(0=送信禁止,1=送信許可)
        //  0x10  SYNC
        //  0x08  DCD
        //  0x04  送信バッファ空
        //  0x02  ボーレートカウント0
        //  0x01  受信バッファフル
        d = (RS232CTerminal.trmMUSReadRR0 () |  //CTS/DCD
             1 << 2 |  //Tx Buffer Empty
             (scc0bInputCounter < 3 ? 1 << 0 : 0 << 0));  //Rx Character Available
        break;
      case 2:  //RR2
        //修飾割り込みベクタ
        //  ポート0BのRR2はWR2に設定したベクタを割り込み要求で加工して返す
        d = (scc1aReceiveRequest != 0 ? scc1aReceiveVector :  //1A受信バッファフル(RS-232C受信)
             scc1aSendRequest != 0 ? scc1aSendVector :  //1A送信バッファ空(RS-232C送信)
             scc0bReceiveRequest != 0 ? scc0bReceiveVector :  //0B受信バッファフル(マウス受信)
             sccInterruptVector);
        break;
      case 3:  //RR3
        //ポート0BのRR3は常に0
        //  ポート0Bの割り込みペンディングを見るときはポート1AのRR3を参照する
        //d = 0;
        break;
      case 12:  //RR12
        d = scc0bBaudRateGen & 0xff;
        break;
      case 13:  //RR13
        d = scc0bBaudRateGen >> 8 & 0xff;
        break;
      default:
        if (SCC_DEBUG_ON && sccDebugOn != 0) {
          System.out.println ("unimplemented register");
        }
      }
      if (peek) {
        break;
      }
      scc0bRegisterNumber = 0;
      break;
      //------------------------------------------------
    case SCC_0B_DATA & 7:  //ポート0Bデータ読み出し(マウス受信)
      if (scc0bInputCounter == 0) {  //1バイト目
        d = Mouse.musExtraData;
        if (XEiJ.mpuClockTime < Mouse.musWheelReleaseTime) {
          d |= Mouse.musWheelButton;
        }
        if (peek) {
          break;
        }
        Mouse.musExtraData = Mouse.musData;
        if (!Mouse.musOnScreen) {  //ホストのマウスカーソルがスクリーン上にない
          //Mouse.musShow ();
          if (Mouse.musCursorNumber != 1 && Mouse.musCursorAvailable) {
            Mouse.musCursorNumber = 1;
            XEiJ.pnlPanel.setCursor (Mouse.musCursorArray[1]);  //ホストのマウスカーソルを表示する
          }
          scc0bData1 = scc0bData2 = 0;
        } else if (Mouse.musSeamlessOn) {  //シームレス
          int on, dx, dy, coeff = 256;
          if (XEiJ.currentMPU < Model.MPU_MC68LC040) {  //MMUなし
            if (SCC_FSX_MOUSE &&
                sccFSXMouseHook != 0 &&  //FSX.Xが常駐している
                MainMemory.mmrRls (0x0938) == sccFSXMouseHook) {  //マウス受信データ処理ルーチンがFSX.Xを指している。SX-Windowが動作中
              on = MainMemory.mmrRws (sccFSXMouseWork + 0x2e) == 0 ? 1 : 0;  //SX-Windowのマウスカーソルの表示状態。Obscureのときは表示されているとみなす
              int xy = MainMemory.mmrRls (sccFSXMouseWork + 0x0a);
              dx = (xy >> 16) - CRTC.crtR10TxXPort;  //SX-Windowのマウスカーソルの見かけのX座標
              dy = (short) xy - CRTC.crtR11TxYPort;  //SX-Windowのマウスカーソルの見かけのY座標
              coeff = MainMemory.mmrRwz (sccFSXMouseWork + 0x04);  //ポインタの移動量。係数*256
            } else {  //SX-Windowが動作中ではない
              on = MainMemory.mmrRbs (0x0aa2);  //IOCSのマウスカーソルの表示状態
              int xy = MainMemory.mmrRls (0x0ace);
              dx = xy >> 16;  //IOCSのマウスカーソルのX座標
              dy = (short) xy;  //IOCSのマウスカーソルのY座標
              if (Mouse.musFollowScrollOn) {
                dx -= CRTC.crtR10TxXPort;
                dy -= CRTC.crtR11TxYPort;
              }
            }
          } else {  //MMUあり
            if (SCC_FSX_MOUSE &&
                sccFSXMouseHook != 0 &&  //FSX.Xが常駐している
                MC68060.mmuPeekLongData (0x0938, 1) == sccFSXMouseHook) {  //マウス受信データ処理ルーチンがFSX.Xを指している。SX-Windowが動作中
              on = MC68060.mmuPeekWordSignData (sccFSXMouseWork + 0x2e, 1) == 0 ? 1 : 0;  //SX-Windowのマウスカーソルの表示状態。Obscureのときは表示されているとみなす
              int xy = MC68060.mmuPeekLongData (sccFSXMouseWork + 0x0a, 1);
              dx = (xy >> 16) - CRTC.crtR10TxXPort;  //SX-Windowのマウスカーソルの見かけのX座標
              dy = (short) xy - CRTC.crtR11TxYPort;  //SX-Windowのマウスカーソルの見かけのY座標
              coeff = MC68060.mmuPeekWordZeroData (sccFSXMouseWork + 0x04, 1);  //ポインタの移動量。係数*256
            } else {  //SX-Windowが動作中ではない
              on = MC68060.mmuPeekByteSignData (0x0aa2, 1);  //IOCSのマウスカーソルの表示状態
              int xy = MC68060.mmuPeekLongData (0x0ace, 1);
              dx = xy >> 16;  //IOCSのマウスカーソルのX座標
              dy = (short) xy;  //IOCSのマウスカーソルのY座標
              if (Mouse.musFollowScrollOn) {
                dx -= CRTC.crtR10TxXPort;
                dy -= CRTC.crtR11TxYPort;
              }
            }
          }
          dx = Mouse.musScreenX - dx;  //X方向の移動量
          dy = Mouse.musScreenY - dy;  //Y方向の移動量
          if (Mouse.musEdgeAccelerationOn) {  //縁部加速を行う
            final int range = 10;  //加速領域の幅
            final int speed = 10;  //移動速度
            if (Mouse.musScreenX < range) {
              dx = -speed;  //左へ
            } else if (XEiJ.pnlScreenWidth - range <= Mouse.musScreenX) {
              dx = speed;  //右へ
            }
            if (Mouse.musScreenY < range) {
              dy = -speed;  //上へ
            } else if (XEiJ.pnlScreenHeight - range <= Mouse.musScreenY) {
              dy = speed;  //下へ
            }
          }
          if (on != 0) {  //X68000のマウスカーソルが表示されている
            //Mouse.musHide ();
            if (Mouse.musCursorNumber != 0 && Mouse.musCursorAvailable) {
              Mouse.musCursorNumber = 0;
              XEiJ.pnlPanel.setCursor (Mouse.musCursorArray[0]);  //ホストのマウスカーソルを消す
            }
          } else {  //X68000のマウスカーソルが表示されていない
            //Mouse.musShow ();
            if (Mouse.musCursorNumber != 1 && Mouse.musCursorAvailable) {
              Mouse.musCursorNumber = 1;
              XEiJ.pnlPanel.setCursor (Mouse.musCursorArray[1]);  //ホストのマウスカーソルを表示する
            }
          }
          if (coeff != 256 && coeff != 0) {
            //SX-Windowのポインタの移動量の補正
            dx = (dx << 8) / coeff;
            dy = (dy << 8) / coeff;
          }
          //  Mouse.MUS_DEACCELERATION_TABLEの値が127を越えることはないのでシームレスでオーバーフローフラグがセットされることはない
          //  rbzで返すので負数のときのゼロ拡張を忘れないこと
          scc0bData1 = (dx == 0 ? 0 : dx >= 0 ?
                        Mouse.MUS_DEACCELERATION_TABLE[Math.min (1024, dx)] :
                        -Mouse.MUS_DEACCELERATION_TABLE[Math.min (1024, -dx)] & 0xff);
          scc0bData2 = (dy == 0 ? 0 : dy >= 0 ?
                        Mouse.MUS_DEACCELERATION_TABLE[Math.min (1024, dy)] :
                        -Mouse.MUS_DEACCELERATION_TABLE[Math.min (1024, -dy)] & 0xff);
        } else if (!XEiJ.frmIsActive) {  //エクスクルーシブだがフォーカスがない
          //Mouse.musShow ();
          if (Mouse.musCursorNumber != 1 && Mouse.musCursorAvailable) {
            Mouse.musCursorNumber = 1;
            XEiJ.pnlPanel.setCursor (Mouse.musCursorArray[1]);  //ホストのマウスカーソルを表示する
          }
          scc0bData1 = scc0bData2 = 0;
          Mouse.musExclusiveStart = true;  //フォーカスを得たときエクスクルーシブに切り替えた直後のように振る舞う
        } else {  //エクスクルーシブ
          //Mouse.musHide ();
          if (Mouse.musCursorNumber != 0 && Mouse.musCursorAvailable) {
            Mouse.musCursorNumber = 0;
            XEiJ.pnlPanel.setCursor (Mouse.musCursorArray[0]);  //ホストのマウスカーソルを消す
          }
          int dx, dy;
          if (XEiJ.PNL_ROTATION_ON) {
            int ox = (XEiJ.pnlScreenX1 + XEiJ.pnlScreenX2) >> 1;  //左スクリーンの中央のパネル座標
            int oy = (XEiJ.pnlScreenY1 + XEiJ.pnlScreenY2) >> 1;
            int tx = Mouse.musPanelX - ox;  //マウスカーソルの左スクリーンの中央からのパネル方向ベクトル
            int ty = Mouse.musPanelY - oy;
            XEiJ.rbtRobot.mouseMove (XEiJ.pnlGlobalX + ox,
                                     XEiJ.pnlGlobalY + oy);  //マウスカーソルを左スクリーンの中央に戻す
            dx = (int) Math.round (XEiJ.pnlInverseL00 * (double) tx + XEiJ.pnlInverseL01 * (double) ty);  //パネル方向ベクトルをスクリーン方向ベクトルに変換する
            dy = (int) Math.round (XEiJ.pnlInverseL10 * (double) tx + XEiJ.pnlInverseL11 * (double) ty);
          } else {
            int ox = XEiJ.pnlScreenX1 + (XEiJ.pnlZoomWidth >> 1);  //画面の中央
            int oy = XEiJ.pnlScreenY1 + (XEiJ.pnlZoomHeight >> 1);
            XEiJ.rbtRobot.mouseMove (XEiJ.pnlGlobalX + ox, XEiJ.pnlGlobalY + oy);  //マウスカーソルを画面の中央に戻す
            dx = Mouse.musPanelX - ox;
            dy = Mouse.musPanelY - oy;
          }
          if (Mouse.musExclusiveStart) {  //エクスクルーシブに切り替えた直後
            //エクスクルーシブに切り替えた直後の1回だけ相対位置を無視する
            //  エクスクルーシブに切り替える直前にマウスカーソルが画面の中央から離れていると切り替えた瞬間に画面の端に飛んでしまう
            dx = 0;
            dy = 0;
            Mouse.musExclusiveStart = false;
          }
          //  上下左右のレスポンスが非対称になると気持ち悪いので冗長に書く
          //  rbzで返すので負数のときのゼロ拡張を忘れないこと
          if (dx != 0) {
            if (dx >= 0) {
              //dx = dx * Mouse.musSpeedRatioX + 32768 >> 16;
              dx = dx * Mouse.musSpeedRatioX >> 16;
              if (127 < dx) {
                d |= 0x10;
                dx = 127;
              }
            } else {
              //dx = -(-dx * Mouse.musSpeedRatioX + 32768 >> 16);
              dx = -(-dx * Mouse.musSpeedRatioX >> 16);
              if (dx < -128) {
                d |= 0x20;
                dx = -128;
              }
              dx &= 0xff;
            }
          }
          if (dy != 0) {
            if (dy >= 0) {
              //dy = dy * Mouse.musSpeedRatioY + 32768 >> 16;
              dy = dy * Mouse.musSpeedRatioY >> 16;
              if (127 < dy) {
                d |= 0x40;
                dy = 127;
              }
            } else {
              //dy = -(-dy * Mouse.musSpeedRatioY + 32768 >> 16);
              dy = -(-dy * Mouse.musSpeedRatioY >> 16);
              if (dy < -128) {
                d |= 0x80;
                dy = -128;
              }
              dy &= 0xff;
            }
          }
          scc0bData1 = dx;
          scc0bData2 = dy;
        }  //if シームレス else エクスクルーシブ
        scc0bInputCounter = 1;
      } else if (scc0bInputCounter == 1) {  //2バイト目
        d = scc0bData1;
        if (peek) {
          break;
        }
        scc0bInputCounter = 2;
      } else if (scc0bInputCounter == 2) {  //3バイト目
        d = scc0bData2;
        if (peek) {
          break;
        }
        scc0bInputCounter = 3;
      }
      break;
      //------------------------------------------------
    case SCC_1A_COMMAND & 7:  //ポート1Aコマンド読み出し
      switch (scc1aRegisterNumber) {
      case 0:  //RR0
        d = RS232CTerminal.trmAUXReadRR0 ();
        break;
      case 2:  //RR2
        //非修飾割り込みベクタ
        //  ポート1AのRR2はWR2に設定したベクタをそのまま返す
        d = sccInterruptVector;
        break;
      case 3:  //RR3
        //割り込みペンディング
        //  RR3リクエストからインサービスまでの間セットされている
        //  許可されていない割り込みのビットはセットされない
        d = scc1aReceiveRR3 | scc1aSendRR3 | scc0bReceiveRR3;
        break;
      case 12:  //RR12
        d = scc1aBaudRateGen & 0xff;
        break;
      case 13:  //RR13
        d = scc1aBaudRateGen >> 8 & 0xff;
        break;
      default:
        if (SCC_DEBUG_ON && sccDebugOn != 0) {
          System.out.println ("unimplemented register");
        }
      }
      if (peek) {
        break;
      }
      scc1aRegisterNumber = 0;
      break;
      //------------------------------------------------
    case SCC_1A_DATA & 7:  //ポート1Aデータ読み出し(RS-232C受信)
      d = RS232CTerminal.trmAUXReadData ();
      break;
      //------------------------------------------------
    default:
      d = 0xff;
    }
    if (SCC_DEBUG_ON && ((a & 4) == 0 ? (sccDebugOn & 1) != 0 : (sccDebugOn & 2) != 0)) {
      System.out.printf ("%08x sccRead(0x%08x)=0x%02x\n", XEiJ.regPC0, a, d);
    }
    return d;
  }  //sccReadByte



  //sccWriteByte (a, d)
  //  ライトバイト
  public static void sccWriteByte (int a, int d) {
    d &= 0xff;
    if (SCC_DEBUG_ON && ((a & 4) == 0 ? (sccDebugOn & 1) != 0 : (sccDebugOn & 2) != 0)) {
      System.out.printf ("%08x sccWrite(0x%08x,0x%02x)\n", XEiJ.regPC0, a, d);
    }
    switch (a & 7) {
      //------------------------------------------------
    case SCC_0B_COMMAND & 7:  //ポート0Bコマンド書き込み
      switch (scc0bRegisterNumber) {
      case 0:  //WR0
        if ((d & 0xf0) == 0) {  //レジスタ選択
          scc0bRegisterNumber = d;
        } else if (d == 0x38) {  //IUSリセット。割り込み処理が終了し、次の割り込みを受け付ける
          if (scc0bReceiveRR3 != 0) {
            scc0bReceiveRR3 = 0;
            if (scc0bInputCounter < 3) {  //3バイト受信するまで割り込み要求を続ける
              if (scc0bReceiveMask != 0) {
                scc0bReceiveRR3 = SCC_0B_RECEIVE_MASK;
                scc0bReceiveRequest = SCC_0B_RECEIVE_MASK;
                XEiJ.mpuIRR |= XEiJ.MPU_SCC_INTERRUPT_MASK;
              }
            }
          }
        } else if (d == 0x10) {  //ステータス割り込みリセット
        } else if (d == 0x30) {  //エラーリセット
        } else if (d == 0x80) {  //送信CRCジェネレータリセット
        } else {
          if (SCC_DEBUG_ON && sccDebugOn != 0) {
            System.out.println ("unimplemented command");
          }
        }
        return;
      case 1:  //WR1
        scc0bReceiveMask = (d & 0x18) != 0 ? SCC_0B_RECEIVE_MASK : 0;
        if ((d & 0xec) != 0x00) {
          if (SCC_DEBUG_ON && sccDebugOn != 0) {
            System.out.println ("unimplemented interrupt mode");
          }
        }
        break;
      case 2:  //WR2
        sccInterruptVector = d;  //割り込みベクタ
        if (SCC_DEBUG_ON && sccDebugOn != 0) {
          System.out.printf ("sccInterruptVector=0x%02x\n", sccInterruptVector);
        }
        sccUpdateVector ();
        break;
      case 3:  //WR3
        if (d == 0xc0) {  //受信禁止
        } else if (d == 0xc1) {  //受信許可
        } else {
          if (SCC_DEBUG_ON && sccDebugOn != 0) {
            System.out.println ("unimplemented receiver configuration");
          }
        }
        break;
      case 4:  //WR4
        break;
      case 5:  //WR5
        //  0x80  DTR
        //  0x60  ビット長(0x00=5bit,0x20=7bit,0x40=6bit,0x60=8bit)
        //  0x10  ブレーク
        //  0x08  送信許可
        //  0x04  CRC-16
        //  0x02  RTS
        //  0x01  送信CRC
        {
          int rts = d >> 1 & 1;
          if ((~scc0bRts & rts) != 0) {  //RTS=0→1。MSCTRL=H→Lとなってマウスに送信要求が出される
            scc0bInputCounter = 0;
            //マウスデータ受信開始
            if ((sccMIE & scc0bReceiveMask) != 0) {
              scc0bReceiveRR3 = SCC_0B_RECEIVE_MASK;
              scc0bReceiveRequest = SCC_0B_RECEIVE_MASK;
              XEiJ.mpuIRR |= XEiJ.MPU_SCC_INTERRUPT_MASK;
            }
          }
          scc0bRts = rts;
          if ((d & 0x75) == 0x60) {
          } else {
            if (SCC_DEBUG_ON && sccDebugOn != 0) {
              System.out.println ("unimplemented sender configuration");
            }
          }
        }
        break;
      case 6:  //WR6
        break;
      case 7:  //WR7
        break;
      case 9:  //WR9
        sccWriteWR9 (d);
        break;
      case 10:  //WR10
        if (d == 0x00) {
        } else {
          if (SCC_DEBUG_ON && sccDebugOn != 0) {
            System.out.println ("unimplemented SDLC configuration");
          }
        }
        break;
      case 11:  //WR11
        if (d == 0x50) {  //TRxCは入力
        } else if (d == 0x56) {  //TRxCからボーレートジェネレータを出力
        } else {
          if (SCC_DEBUG_ON && sccDebugOn != 0) {
            System.out.println ("unimplemented clock control");
          }
        }
        break;
      case 12:  //WR12
        scc0bBaudRateGen = (scc0bBaudRateGen & ~0xff) | d;
        break;
      case 13:  //WR13
        scc0bBaudRateGen = d << 8 | (scc0bBaudRateGen & 0xff);
        break;
      case 14:  //WR14
        if (d == 0x02) {  //ボーレートジェネレータ停止
        } else if (d == 0x03) {  //ボーレートジェネレータ動作
        } else {
          if (SCC_DEBUG_ON && sccDebugOn != 0) {
            System.out.println ("unimplemented DPLL configuration");
          }
        }
        break;
      case 15:  //WR15
        if (d == 0x00) {
        } else if (d == 0x80) {
        } else if (d == 0x88) {
        } else {
          if (SCC_DEBUG_ON && sccDebugOn != 0) {
            System.out.println ("unimplemented status interrupt configuration");
          }
        }
        break;
      default:
        if (SCC_DEBUG_ON && sccDebugOn != 0) {
          System.out.println ("unimplemented register");
        }
      }
      scc0bRegisterNumber = 0;
      return;
      //------------------------------------------------
    case SCC_0B_DATA & 7:  //ポート0Bデータ書き込み(マウス送信)
      return;
      //------------------------------------------------
    case SCC_1A_COMMAND & 7:  //ポート1Aコマンド書き込み
      switch (scc1aRegisterNumber) {
      case 0:  //WR0
        if ((d & 0xf0) == 0) {  //レジスタ選択
          scc1aRegisterNumber = d;
        } else if (d == 0x38) {  //IUSリセット。割り込み処理が終了し、次の割り込みを受け付ける
          if (scc1aReceiveRR3 != 0) {
            scc1aReceiveRR3 = 0;
          } else if (scc1aSendRR3 != 0) {
            scc1aSendRR3 = 0;
          }
        } else if (d == 0x10) {  //ステータス割り込みリセット
        } else if (d == 0x30) {  //エラーリセット
        } else if (d == 0x80) {  //送信CRCジェネレータリセット
        } else {
          if (SCC_DEBUG_ON && sccDebugOn != 0) {
            System.out.println ("unimplemented command");
          }
        }
        return;
      case 1:  //WR1
        //  Z85C30UM 5-4
        //  WR1
        //    bit7    WAIT/DMA Request Enable
        //    bit6    /WAIT/DMA Request Function
        //    bit5    WAIT/DMA Request On Receive/Transmit
        //    bit4-3  00=Rx Int Disable
        //            01=Rx Int On First Character or Special Condition
        //            10=Int On All Rx Characters or Special Condition
        //            11=Rx Int On Special Condition Only
        //    bit2    Parity is Special Condition
        //    bit1    Tx Int Enable
        //    bit0    Ext Int Enable
        scc1aReceiveMask = (d & 0x18) != 0 ? SCC_1A_RECEIVE_MASK : 0;  //Rx Int Enable
        scc1aSendMask = (d & 0x02) != 0 ? SCC_1A_SEND_MASK : 0;  //Tx Int Enable
        if ((d & 0xe3) != 0x00) {
          if (SCC_DEBUG_ON && sccDebugOn != 0) {
            System.out.println ("unimplemented interrupt mode");
          }
        }
        break;
      case 2:  //WR2
        sccInterruptVector = d;  //割り込みベクタ
        if (SCC_DEBUG_ON && sccDebugOn != 0) {
          System.out.printf ("sccInterruptVector=0x%02x\n", sccInterruptVector);
        }
        sccUpdateVector ();
        break;
      case 3:  //WR3
        //  Z85C30UM 5-7
        //  WR3
        //    bit7-6  00=Rx 5 Bits/Character
        //            01=Rx 7 Bits/Character
        //            10=Rx 6 Bits/Character
        //            10=Rx 8 Bits/Character
        //    bit5    1=Auto Enables
        //    bit4    1=Enter Hunt Mode
        //    bit3    1=Rx CRC Enable
        //    bit2    1=Address Search Mode (SDLC)
        //    bit1    1=Sync Characster Load Inhibit
        //    bit0    1=Rx Enable
        if ((d & 0x3e) == 0x00) {
          scc1aRxBits = d >> 6;
        } else {
          if (SCC_DEBUG_ON && sccDebugOn != 0) {
            System.out.println ("unimplemented receiver configuration");
          }
        }
        break;
      case 4:  //WR4
        //  Z85C30UM 5-8
        //  WR4
        //    bit7-6  00=X1 Clock Mode
        //            01=X16 Clock Mode
        //            10=X32 Clock Mode
        //            11=X64 Clock Mode
        //    bit5-4  00=8-Bit Sync Character
        //            01=16-Bit Sync Character
        //            10=SDLC Mode (01111110 Flag)
        //            11=External Sync Mode
        //    bit3-2  00=Sync Modes Enable
        //            01=1 Stop Bit/Character
        //            10=1 1/2 Stop Bits/Character
        //            11=2 Stop Bits/Character
        //    bit1    0=Parity ODD
        //            1=Parity EVEN
        //    bit0    1=Parity Enable
        scc1aClockModeShift = d >> 6 == 0 ? 0 : (d >> 6) + 3;  //0=2^0,1=2^4,2=2^5,3=2^6
        scc1aStop = d >> 2 & 3;
        scc1aParity = d & 3;
        break;
      case 5:  //WR5
        //  Z85C30UM 5-9
        //  WR5
        //    bit7    0=/DTR is High
        //            1=/DTR is Low(準備完了)
        //    bit6-5  00=Tx 5 Bits(Or Less)/Character
        //            01=Tx 7 Bits/Character
        //            10=Tx 6 Bits/Character
        //            11=Tx 8 Bits/Character
        //    bit4    1=Send Break
        //    bit3    1=Tx Enable
        //    bit2    0=SDLC
        //            1=CRC-16
        //    bit1    0=/RTS is High  自分が受信バッファに余裕がない(3/4)から送るのやめてと言う
        //            1=/RTS is Low   自分が受信バッファに余裕がある(1/4)から送っていいよと言う
        //    bit0    1=Tx CRC Enable
        RS232CTerminal.trmAUXSetNotReceiving ((d & 0x02) == 0);
        scc1aTxBits = d >> 5 & 3;
        scc1aTxEnable = d >> 3 & 1;
        RS232CTerminal.trmAUXSetDTR ((d & 0x80) != 0);
        if ((d & 0x15) != 0x00) {
          if (SCC_DEBUG_ON && sccDebugOn != 0) {
            System.out.println ("unimplemented sender configuration");
          }
        }
        break;
      case 6:  //WR6
        break;
      case 7:  //WR7
        break;
      case 9:  //WR9
        sccWriteWR9 (d);
        break;
      case 10:  //WR10
        if (d == 0x00) {
        } else {
          if (SCC_DEBUG_ON && sccDebugOn != 0) {
            System.out.println ("unimplemented SDLC configuration");
          }
        }
        break;
      case 11:  //WR11
        if (d == 0x50) {  //TRxCは入力
        } else if (d == 0x56) {  //TRxCからボーレートジェネレータを出力
        } else {
          if (SCC_DEBUG_ON && sccDebugOn != 0) {
            System.out.println ("unimplemented clock control");
          }
        }
        break;
      case 12:  //WR12
        //  Z85C30UM 5-18
        //  WR12
        //    bit7-0  Lower Byte Of Time Constant
        //
        //                                   5000000
        //      WR13-WR12                Clock Frequency
        //    Time Constant = -------------------------------------- - 2
        //                    2 x (Desired Rate) x (BR Clock Period)
        //                                            WR4 bit7-6
        //
        //    5000000/(2*(0+2)*16)=78125  76800*1.017
        //    5000000/(2*(2+2)*16)=39062.5  38400*1.017
        //    5000000/(2*(3+2)*16)=31250
        //    5000000/(2*(6+2)*16)=19531.25  19200*1.017
        //    5000000/(2*(14+2)*16)=9765.625  9600*1.017
        //    5000000/(2*(30+2)*16)=4882.8125  4800*1.017
        scc1aBaudRateGen = (scc1aBaudRateGen & ~0xff) | d;
        break;
      case 13:  //WR13
        //  Z85C30UM 5-19
        //  WR13
        //    bit7-0  Upper Byte Of Time Constant
        scc1aBaudRateGen = d << 8 | (scc1aBaudRateGen & 0xff);
        break;
      case 14:  //WR14
        //  Z85C30UM 5-19
        //  WR14
        //    bit7-5  000=Null Command
        //            001=Enter Search Mode
        //            010=Reset Missing Clock
        //            011=Disable DPLL
        //            100=Set Source = BR Generator
        //            101=Set Source = /RTxC
        //            110=Set FM Mode
        //            111=Set NRZI Mode
        //    bit4    1=Local Loopback
        //    bit3    1=Auto Echo
        //    bit2    1=/DTR/Request Function
        //    bit1    BR Generator Source
        //            0=/RTxC pin or XTAL oscillator
        //            1=SCC's PCLK input
        //    bit0    1=BR Generator Enable
        if ((d & 0xfe) == 0x02) {
          scc1aBRGEnable = d & 1;
          if (scc1aBRGEnable != 0) {  //1が書き込まれたとき
            //本来は、ボーレートジェネレータのカウンタの初期値はゼロカウントまたはリセットでロードされる
            //一般的に、
            //動作間隔を決めるカウンタの初期値が上位と下位に分かれていて、ゼロカウントで初期値がロードされる仕組みのとき、
            //上位または下位のどちらか一方を書き換えた直後にゼロカウントになると予期しない初期値がロードされてしまうので、
            //カウンタを止めてから初期値を書き換えるのが作法である
            //それならばボーレートジェネレータの再開をボーレート変更完了の合図として使えるはずだが、
            //tmsio.xとbsio.xがカウンタを止めずにボーレートを変更してしまっているため、その方法が使えない
            //ここではBaud Rate Generator Enableに1が書き込まれたら必ず設定を更新する
            RS232CTerminal.trmReflectSettings (1);
          }
        } else {
          if (SCC_DEBUG_ON && sccDebugOn != 0) {
            System.out.println ("unimplemented DPLL configuration");
          }
        }
        break;
      case 15:  //WR15
        //  Z85C30UM 5-20
        //  WR15
        //    bit7  Break/Abort IE
        //    bit6  Tx Underrun/EOM IE
        //    bit5  CTS IE
        //      SCC 21 CTSA ← RS 5 CTS
        //    bit4  Sync/Hunt IE
        //      SCC 12 SYNCA ← VCC
        //      SCC 25 CTSB ← RS 22 CI Call Indicator (RI Ring Indicator)
        //    bit3  DCD IE
        //      SCC 22 DCDA ← RS 6 DSR Data Set Ready
        //      SCC 24 DCDB ← RS 8 CD Carrier Detect
        //    bit2  SDLC FIFO Enable (Reserved on NMOS)
        //    bit1  Zero Count IE
        //    bit0  WR7' SDLC Feature Enable (Reserved on NMOS/CMOS)
        if (d == 0x00) {
        } else if (d == 0x80) {
        } else if (d == 0x88) {
        } else {
          if (SCC_DEBUG_ON && sccDebugOn != 0) {
            System.out.println ("unimplemented status interrupt configuration");
          }
        }
        break;
      default:
        if (SCC_DEBUG_ON && sccDebugOn != 0) {
          System.out.println ("unimplemented register");
        }
      }
      scc1aRegisterNumber = 0;
      return;
      //------------------------------------------------
    case SCC_1A_DATA & 7:  //ポート1Aデータ書き込み(RS-232C送信)
      RS232CTerminal.trmAUXWriteData (d);
      return;
      //------------------------------------------------
    default:
      ;
    }
  }  //sccWriteByte


  //sccWriteWR9 (d)
  //  Z85C30UM 5-9
  //  WR9
  //    bit7-6  00 No Reset
  //            01 Channel Reset B
  //            10 Channel Reset A
  //            11 Force Hardware Reset
  //    bit5    Software INTACK Enable
  //    bit4    0 Status Low V3 V2 V1
  //            1 Status High V6 V5 V4
  //    bit3    MIE Master Interrupt Enable
  //    bit2    DLC Disable Lower Chain
  //    bit1    NV No Vector
  //    bit0    VIS Vector Includes Status
  public static void sccWriteWR9 (int d) {
    //リセット
    switch (d & 0xc0) {
    case 0x40:  //Channel Reset B
      scc0bRts = 0;
      break;
    case 0x80:  //Channel Reset A
      scc1aBRGEnable = 0;
      scc1aRxEnable = 0;
      scc1aTxEnable = 0;
      RS232CTerminal.trmAUXReset ();
      break;
    case 0xc0:  //Force Hardware Reset
      scc0bRts = 0;
      scc1aBRGEnable = 0;
      scc1aRxEnable = 0;
      scc1aTxEnable = 0;
      RS232CTerminal.trmAUXReset ();
      break;
    }
    //割り込み
    sccVectorInclude = d & 0x11;
    if (SCC_DEBUG_ON && sccDebugOn != 0) {
      System.out.printf ("sccVectorInclude=0x%02x\n", sccVectorInclude);
    }
    sccUpdateVector ();
    sccMIE = (d & 0x08) != 0 ? -1 : 0;
    if ((d & 0x26) != 0) {
      if (SCC_DEBUG_ON && sccDebugOn != 0) {
        System.out.println ("unimplemented interrupt configuration");
      }
    }
  }  //sccWriteWR9


}  //class Z8530



