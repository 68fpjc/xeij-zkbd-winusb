//========================================================================================
//  HD63450.java
//    en:DMA controller
//    ja:DMAコントローラ
//  Copyright (C) 2003-2026 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System

public class HD63450 {

  //フラグ
  //                                          3210
  public static final int DMA_DEBUG_TRACE = 0b0000;  //トレースするチャンネルをセット

  public static final boolean DMA_ALERT_HIMEM = true;  //MAR,DAR,BARにハイメモリのアドレスが書き込まれた報告する

  //レジスタ
  //  DMA_CERはread-only、その他はread/write
  //  DMA_DCR,DMA_SCR,DMA_MTC,DMA_MAR,DMA_DAR,DMA_MFC,DMA_DFCに動作中(dmaACT[i]!=0)に書き込むとDMA_TIMING_ERRORになる

  //Channel Status Register (R/W)
  public static final int DMA_CSR = 0x00;
  public static final int DMA_COC = 0b10000000;  //Channel Operation Complete。1=チャンネル動作完了
  public static final int DMA_BLC = 0b01000000;  //BLock transfer Complete。1=ブロック転送完了
  public static final int DMA_NDT = 0b00100000;  //Normal Device Termination。1=正常終了
  public static final int DMA_ERR = 0b00010000;  //ERRor。1=エラーあり
  public static final int DMA_ACT = 0b00001000;  //channel ACTive。1=チャンネル動作中
  public static final int DMA_DIT = 0b00000100;  //! 非対応。~DONE Input Transition。1=~DONE入力があった
  public static final int DMA_PCT = 0b00000010;  //~PCL Transition。1=~PCLの立下りがあった
  public static final int DMA_PCS = 0b00000001;  //~PCL Status。~PCLの状態

  //Channel Error Register (R)
  public static final int DMA_CER = 0x01;
  public static final int DMA_ERROR_CODE           = 0b00011111;  //エラーコード
  public static final int DMA_NO_ERROR             = 0b00000000;  //  エラーなし
  public static final int DMA_CONFIGURATION_ERROR  = 0b00000001;  //  コンフィギュレーションエラー
  public static final int DMA_TIMING_ERROR         = 0b00000010;  //  動作タイミングエラー
  public static final int DMA_MEMORY_ADDRESS_ERROR = 0b00000101;  //  アドレスエラー(メモリアドレス)
  public static final int DMA_DEVICE_ADDRESS_ERROR = 0b00000110;  //  アドレスエラー(デバイスアドレス)
  public static final int DMA_BASE_ADDRESS_ERROR   = 0b00000111;  //  アドレスエラー(ベースアドレス)
  public static final int DMA_MEMORY_BUS_ERROR     = 0b00001001;  //  バスエラー(メモリアドレス)
  public static final int DMA_DEVICE_BUS_ERROR     = 0b00001010;  //  バスエラー(デバイスアドレス)
  public static final int DMA_BASE_BUS_ERROR       = 0b00001011;  //  バスエラー(ベースアドレス)
  public static final int DMA_MEMORY_COUNT_ERROR   = 0b00001101;  //  カウントエラー(メモリカウンタ)
  public static final int DMA_BASE_COUNT_ERROR     = 0b00001111;  //  カウントエラー(ベースカウンタ)
  public static final int DMA_EXTERNAL_ABORT       = 0b00010000;  //! 非対応。外部強制停止
  public static final int DMA_SOFTWARE_ABORT       = 0b00010001;  //  ソフトウェア強制停止

  //Device Control Register (R/W)
  public static final int DMA_DCR = 0x04;
  public static final int DMA_XRM                    = 0b11000000;  //eXternal Request Mode
  public static final int DMA_BURST_TRANSFER         = 0b00000000;  //  バースト転送モード
  public static final int DMA_NO_HOLD_CYCLE_STEAL    = 0b10000000;  //  ホールドなしサイクルスチールモード
  public static final int DMA_HOLD_CYCLE_STEAL       = 0b11000000;  //! 非対応。ホールドありサイクルスチールモード。ホールドなしサイクルスチールモードと同じ
  public static final int DMA_DTYP                   = 0b00110000;  //Device TYPe
  public static final int DMA_HD68000_COMPATIBLE     = 0b00000000;  //  HD68000コンパチブル(デュアルアドレスモード)
  public static final int DMA_HD6800_COMPATIBLE      = 0b00010000;  //! 非対応。HD6800コンパチブル(デュアルアドレスモード)
  public static final int DMA_ACK_DEVICE             = 0b00100000;  //! 非対応。~ACK付きデバイス(シングルアドレスモード)
  public static final int DMA_ACK_READY_DEVICE       = 0b00110000;  //! 非対応。~ACK,~READY付きデバイス(シングルアドレスモード)
  public static final int DMA_DPS                    = 0b00001000;  //Device Port Size
  public static final int DMA_PORT_8_BIT             = 0b00000000;  //  8ビットポート
  public static final int DMA_PORT_16_BIT            = 0b00001000;  //  16ビットポート
  public static final int DMA_PCL                    = 0b00000011;  //Peripheral Control Line
  public static final int DMA_STATUS_INPUT           = 0b00000000;  //  STATUS入力
  public static final int DMA_STATUS_INPUT_INTERRUPT = 0b00000001;  //! 非対応。割り込みありSTATUS入力
  public static final int DMA_EIGHTH_START_PULSE     = 0b00000010;  //! 非対応。1/8スタートパルス
  public static final int DMA_ABORT_INPUT            = 0b00000011;  //! 非対応。ABORT入力

  //Operation Control Register (R/W)
  public static final int DMA_OCR = 0x05;
  public static final int DMA_DIR                 = 0b10000000;  //DIRection
  public static final int DMA_MEMORY_TO_DEVICE    = 0b00000000;  //  メモリ→デバイス。DMA_MAR→DMA_DAR
  public static final int DMA_DEVICE_TO_MEMORY    = 0b10000000;  //  デバイス→メモリ。DMA_DAR→DMA_MAR
  public static final int DMA_BTD                 = 0b01000000;  //! 非対応。multi Block Transfer with ~DONE mode
  public static final int DMA_SIZE                = 0b00110000;  //operand SIZE
  public static final int DMA_BYTE_SIZE           = 0b00000000;  //  8ビット
  public static final int DMA_WORD_SIZE           = 0b00010000;  //  16ビット
  public static final int DMA_LONG_WORD_SIZE      = 0b00100000;  //  32ビット
  public static final int DMA_UNPACKED_8_BIT      = 0b00110000;  //  パックなし8ビット
  public static final int DMA_CHAIN               = 0b00001100;  //CHAINing operation
  public static final int DMA_NO_CHAINING         = 0b00000000;  //  チェインなし
  public static final int DMA_ARRAY_CHAINING      = 0b00001000;  //  アレイチェイン
  public static final int DMA_LINK_ARRAY_CHAINING = 0b00001100;  //  リンクアレイチェイン
  public static final int DMA_REQG                = 0b00000011;  //DMA REQuest Generation method
  public static final int DMA_AUTO_REQUEST        = 0b00000000;  //  オートリクエスト限定速度。転送中にバスを開放する
  public static final int DMA_AUTO_REQUEST_MAX    = 0b00000001;  //  オートリクエスト最大速度。転送中にバスを開放しない
  public static final int DMA_EXTERNAL_REQUEST    = 0b00000010;  //  外部転送要求
  public static final int DMA_DUAL_REQUEST        = 0b00000011;  //  最初はオートリクエスト、2番目から外部転送要求

  //Sequence Control Register (R/W)
  public static final int DMA_SCR = 0x06;
  public static final int DMA_MAC =        0b00001100;  //Memory Address register Count
  public static final int DMA_STATIC_MAR = 0b00000000;  //  DMA_MAR固定
  public static final int DMA_INC_MAR    = 0b00000100;  //  DMA_MAR++
  public static final int DMA_DEC_MAR    = 0b00001000;  //  DMA_MAR--
  public static final int DMA_DAC =        0b00000011;  //Device Address register Count
  public static final int DMA_STATIC_DAR = 0b00000000;  //  DMA_DAR固定
  public static final int DMA_INC_DAR    = 0b00000001;  //  DMA_DAR++
  public static final int DMA_DEC_DAR    = 0b00000010;  //  DMA_DAR--

  //Channel Control Register (R/W)
  public static final int DMA_CCR = 0x07;
  public static final int DMA_STR = 0b10000000;  //STaRt operation。1=動作開始
  public static final int DMA_CNT = 0b01000000;  //CoNTinue operation。1=コンティニューあり
  public static final int DMA_HLT = 0b00100000;  //Halt operation。1=動作一時停止
  public static final int DMA_SAB = 0b00010000;  //Software ABort。1=動作中止
  public static final int DMA_ITE = 0b00001000;  //InTerrupt Enable。1=割り込み許可

  //Transfer Counter, Address Register
  public static final int DMA_MTC = 0x0a;  //Memory Transfer Counter (R/W)
  public static final int DMA_MAR = 0x0c;  //Memory Address Register (R/W)
  public static final int DMA_DAR = 0x14;  //Device Address Register (R/W)
  public static final int DMA_BTC = 0x1a;  //Base Transfer Counter (R/W)
  public static final int DMA_BAR = 0x1c;  //Base Address Register (R/W)

  //Interrupt Vector
  public static final int DMA_NIV = 0x25;  //Normal Interrupt Vector (R/W)
  public static final int DMA_EIV = 0x27;  //Error Interrupt Vector (R/W)

  //Function Codes
  public static final int DMA_MFC = 0x29;  //Memory Function Codes (R/W)
  public static final int DMA_FC2 = 0b00000100;  //Function Code 2
  public static final int DMA_FC1 = 0b00000010;  //! 非対応。Function Code 1
  public static final int DMA_FC0 = 0b00000001;  //! 非対応。Function Code 0

  //Channel Priority Register (R/W)
  public static final int DMA_CPR = 0x2d;
  public static final int DMA_CP = 0b00000011;  //! 未対応。Channel Priority。0=高,1,2,3=低

  //Function Codes
  public static final int DMA_DFC = 0x31;  //Device Function Codes (R/W)
  public static final int DMA_BFC = 0x39;  //Base Function Codes (R/W)

  //General Control Register (R/W)
  public static final int DMA_GCR = 0xff;
  public static final int DMA_BT = 0b00001100;  //Burst Time。0=16clk,1=32clk,2=64clk,3=128clk
  public static final int DMA_BR = 0b00000011;  //Bandwidth Ratio。0=1/2,1=1/4,2=1/8,3=1/16

  //レジスタ
  //  すべてゼロ拡張
  public static final int[] dmaPCS = new int[4];         //DMA_CSR bit0
  public static final int[] dmaPCT = new int[4];         //        bit1
  public static final int[] dmaDIT = new int[4];         //        bit2
  public static final int[] dmaACT = new int[4];         //        bit3
  public static final int[] dmaERR = new int[4];         //        bit4
  public static final int[] dmaNDT = new int[4];         //        bit5
  public static final int[] dmaBLC = new int[4];         //        bit6
  public static final int[] dmaCOC = new int[4];         //        bit7
  public static final int[] dmaErrorCode = new int[4];   //DMA_CER bit0-4
  public static final int[] dmaPCL = new int[4];         //DMA_DCR bit0-1
  public static final int[] dmaDPS = new int[4];         //        bit3
  public static final int[] dmaDTYP = new int[4];        //        bit4-5
  public static final int[] dmaXRM = new int[4];         //        bit6-7
  public static final int[] dmaREQG = new int[4];        //DMA_OCR bit0-1
  public static final int[] dmaCHAIN = new int[4];       //        bit2-3
  public static final int[] dmaSIZE = new int[4];        //        bit4-5
  public static final int[] dmaBTD = new int[4];         //        bit6
  public static final int[] dmaDIR = new int[4];         //        bit7
  public static final int[] dmaDAC = new int[4];         //DMA_SCR bit0-1
  public static final int[] dmaDACValue = new int[4];    //           dmaDAC==(DMA_INC_DAR?1:dmaDAC==DMA_DEC_DAR?-1:0)*(dmaDPS==DMA_PORT_8_BIT?2:1)
  public static final int[] dmaMAC = new int[4];         //        bit2-3
  public static final int[] dmaMACValue = new int[4];    //           dmaMAC==DMA_INC_MAR?1:dmaMAC==DMA_DEC_MAR?-1:0
  public static final int[] dmaITE = new int[4];         //DMA_CCR bit3
  public static final int[] dmaSAB = new int[4];         //        bit4
  public static final int[] dmaHLT = new int[4];         //        bit5
  public static final int[] dmaCNT = new int[4];         //        bit6
  public static final int[] dmaSTR = new int[4];         //        bit7
  public static final int[] dmaMTC = new int[4];         //DMA_MTC bit0-15
  public static final int[] dmaMAR = new int[4];         //DMA_MAR bit0-31
  public static final int[] dmaDAR = new int[4];         //DMA_DAR bit0-31
  public static final int[] dmaBTC = new int[4];         //DMA_BTC bit0-15
  public static final int[] dmaBAR = new int[4];         //DMA_BAR bit0-31
  public static final int[] dmaNIV = new int[4];         //DMA_NIV bit0-7
  public static final int[] dmaEIV = new int[4];         //DMA_EIV bit0-7
  public static final int[] dmaMFC = new int[4];         //DMA_MFC bit2
  public static final MemoryMappedDevice[][] dmaMFCMap = new MemoryMappedDevice[4][];  //  DataBreakPoint.DBP_ON?dmaMFC[i]==0?udm:sdm:dmaMFC[i]==0?um:sm
  public static final int[] dmaCP = new int[4];          //DMA_CPR bit0-1
  public static final int[] dmaDFC = new int[4];         //DMA_DFC bit2
  public static final MemoryMappedDevice[][] dmaDFCMap = new MemoryMappedDevice[4][];  //  DataBreakPoint.DBP_ON?dmaDFC[i]==0?udm:sdm:dmaDFC[i]==0?um:sm
  public static final int[] dmaBFC = new int[4];         //DMA_BFC bit2
  public static final MemoryMappedDevice[][] dmaBFCMap = new MemoryMappedDevice[4][];  //  DataBreakPoint.DBP_ON?dmaBFC[i]==0?udm:sdm:dmaBFC[i]==0?um:sm
  public static int dmaBR;                               //DMA_GCR bit0-1。0=1/2,1=1/4,2=1/8,3=1/16
  public static int dmaBT;                               //        bit2-3。0=16clk,1=32clk,2=64clk,3=128clk
  public static long dmaBurstSpan;  //バースト期間。XEiJ.dmaCycleUnit<<(4+(dmaBT>>2))。MC68450 5-6
  public static long dmaBurstInterval;  //バースト間隔。dmaBurstSpan<<(1+(dmaBR&3))
  public static long dmaBurstStart;  //バースト開始時刻
  public static long dmaBurstEnd;  //バースト終了時刻
  public static long[] dmaRequestTime = new long[4];  //動作開始時刻。オートリクエスト最大速度のとき次の予約を入れる時刻

  public static final int[] dmaReqPin = new int[4];  //REQピンの状態。0または1

  //割り込み
  public static final int[] dmaInnerRequest = new int[8];  //割り込み要求カウンタ
  public static final int[] dmaInnerAcknowleged = new int[8];  //割り込み受付カウンタ

  //クロック
  public static final long[] dmaInnerClock = new long[4];  //転送要求時刻(XEiJ.TMR_FREQ単位)

  //パックあり
  public static final int[] dmaMemoryCarry = new int[4];  //メモリでパックするために繰り越したデータ
  public static final int[] dmaDeviceCarry = new int[4];  //デバイスでパックするために繰り越したデータ

  //サイクル数補正
  public static final int[] dmaAdditionalCycles = new int[4];  //追加のサイクル数

  //アクセスサイクル数
  public static int dmaReadCycles;  //1ワードリードの所要サイクル数
  public static int dmaWriteCycles;  //1ワードライトの所要サイクル数

  public static final TickerQueue.Ticker[] dmaTickerArray = new TickerQueue.Ticker[] {
    new TickerQueue.Ticker () {
      @Override protected void tick () {
        dmaTransfer (0);
      }
    },
    new TickerQueue.Ticker () {
      @Override protected void tick () {
        dmaTransfer (1);
      }
    },
    new TickerQueue.Ticker () {
      @Override protected void tick () {
        dmaTransfer (2);
      }
    },
    new TickerQueue.Ticker () {
      @Override protected void tick () {
        dmaTransfer (3);
      }
    },
  };

  //dmaInit ()
  //  DMAコントローラを初期化する
  public static void dmaInit () {
    //レジスタ
    //dmaPCS = new int[4];
    //dmaPCT = new int[4];
    //dmaDIT = new int[4];
    //dmaACT = new int[4];
    //dmaERR = new int[4];
    //dmaNDT = new int[4];
    //dmaBLC = new int[4];
    //dmaCOC = new int[4];
    //dmaErrorCode = new int[4];
    //dmaPCL = new int[4];
    //dmaDPS = new int[4];
    //dmaDTYP = new int[4];
    //dmaXRM = new int[4];
    //dmaREQG = new int[4];
    //dmaCHAIN = new int[4];
    //dmaSIZE = new int[4];
    //dmaBTD = new int[4];
    //dmaDIR = new int[4];
    //dmaDAC = new int[4];
    //dmaDACValue = new int[4];
    //dmaMAC = new int[4];
    //dmaMACValue = new int[4];
    //dmaITE = new int[4];
    //dmaSAB = new int[4];
    //dmaHLT = new int[4];
    //dmaCNT = new int[4];
    //dmaSTR = new int[4];
    //dmaMTC = new int[4];
    //dmaMAR = new int[4];
    //dmaDAR = new int[4];
    //dmaBTC = new int[4];
    //dmaBAR = new int[4];
    //dmaNIV = new int[4];
    //dmaEIV = new int[4];
    //dmaMFC = new int[4];
    //dmaMFCMap = new MMD[4];
    //dmaCP = new int[4];
    //dmaDFC = new int[4];
    //dmaDFCMap = new MMD[4];
    //dmaBFC = new int[4];
    //dmaBFCMap = new MMD[4];
    //dmaRequestTime = new long[4];
    //dmaPCSはresetでは操作しない
    dmaPCS[0] = 1;  //EXVSYNC
    dmaPCS[1] = 1;  //VCC
    dmaPCS[2] = 1;  //EXPCL
    dmaPCS[3] = 1;  //ADPCMREQ。PCL3とREQ3は直結
    //dmaReqPinはresetでは操作しない
    dmaReqPin[0] = 1;
    dmaReqPin[1] = 1;
    dmaReqPin[2] = 1;
    dmaReqPin[3] = 1;
    //割り込み
    //dmaInnerRequest = new int[8];
    //dmaInnerAcknowleged = new int[8];
    //クロック
    //dmaInnerClock = new long[4];
    //パックあり
    //dmaMemoryCarry = new int[4];
    //dmaDeviceCarry = new int[4];
    //サイクル数補正
    //dmaAdditionalCycles = new int[4];
    dmaReset ();
  }  //dmaInit()

  //リセット
  public static void dmaReset () {
    //レジスタ
    for (int i = 0; i < 4; i++) {
      //dmaPCSはresetでは操作しない
      dmaPCT[i] = 0;
      dmaDIT[i] = 0;
      dmaACT[i] = 0;
      dmaERR[i] = 0;
      dmaNDT[i] = 0;
      dmaBLC[i] = 0;
      dmaCOC[i] = 0;
      dmaErrorCode[i] = 0;
      dmaPCL[i] = 0;
      dmaDPS[i] = 0;
      dmaDTYP[i] = 0;
      dmaXRM[i] = 0;
      dmaREQG[i] = 0;
      dmaCHAIN[i] = 0;
      dmaSIZE[i] = 0;
      dmaBTD[i] = 0;
      dmaDIR[i] = 0;
      dmaDAC[i] = 0;
      dmaDACValue[i] = 0;
      dmaMAC[i] = 0;
      dmaMACValue[i] = 0;
      dmaITE[i] = 0;
      dmaSAB[i] = 0;
      dmaHLT[i] = 0;
      dmaCNT[i] = 0;
      dmaSTR[i] = 0;
      dmaMTC[i] = 0;
      dmaMAR[i] = 0;
      dmaDAR[i] = 0;
      dmaBTC[i] = 0;
      dmaBAR[i] = 0;
      dmaNIV[i] = 0x0f;  //割り込みベクタの初期値は未初期化割り込みを示す0x0f
      dmaEIV[i] = 0x0f;
      dmaMFC[i] = 0;
      if (DataBreakPoint.DBP_ON) {
        dmaMFCMap[i] = DataBreakPoint.dbpUserMap;
      } else {
        dmaMFCMap[i] = XEiJ.busUserMap;
      }
      dmaCP[i] = 0;
      dmaDFC[i] = 0;
      if (DataBreakPoint.DBP_ON) {
        dmaDFCMap[i] = DataBreakPoint.dbpUserMap;
      } else {
        dmaDFCMap[i] = XEiJ.busUserMap;
      }
      dmaBFC[i] = 0;
      if (DataBreakPoint.DBP_ON) {
        dmaBFCMap[i] = DataBreakPoint.dbpUserMap;
      } else {
        dmaBFCMap[i] = XEiJ.busUserMap;
      }
      dmaRequestTime[i] = XEiJ.FAR_FUTURE;
    }
    dmaBR = 0;
    dmaBT = 0;
    dmaBurstSpan = XEiJ.dmaCycleUnit << (4 + (dmaBT >> 2));
    dmaBurstInterval = dmaBurstSpan << (1 + (dmaBR & 3));
    dmaBurstStart = XEiJ.FAR_FUTURE;
    dmaBurstEnd = 0L;
    //割り込み
    for (int i = 0; i < 8; i++) {
      dmaInnerRequest[i] = 0;
      dmaInnerAcknowleged[i] = 0;
    }
    //クロック
    for (int i = 0; i < 4; i++) {
      dmaInnerClock[i] = XEiJ.FAR_FUTURE;
      TickerQueue.tkqRemove (dmaTickerArray[i]);
    }
  }  //dmaReset()

  //割り込み受付
  //  コアが割り込み要求を受け付けたときに呼び出す
  //  割り込みベクタ番号を返す
  //  割り込み要求を取り下げる場合は0を返す
  //  オートベクタを使用するデバイスはオートベクタの番号を返すこと
  public static int dmaAcknowledge () {
    for (int i = 0; i < 8; i++) {  //! 未対応。本来はチャンネルプライオリティに従うべき
      int request = dmaInnerRequest[i];
      if (dmaInnerAcknowleged[i] != request) {
        dmaInnerAcknowleged[i] = request;
        return (i & 1) == 0 ? dmaNIV[i >> 1] : dmaEIV[i >> 1];
      }
    }
    return 0;
  }  //dmaAcknowledge()

  //割り込み終了
  //  コアが割り込み処理を終了したときに呼び出す
  //  まだ処理されていない割り込みが残っていたら再度割り込み要求を出す
  public static void dmaDone () {
    for (int i = 0; i < 8; i++) {  //! 未対応。本来はチャンネルプライオリティに従うべき
      if (dmaInnerRequest[i] != dmaInnerAcknowleged[i]) {
        XEiJ.mpuIRR |= XEiJ.MPU_DMA_INTERRUPT_MASK;
        return;
      }
    }
  }  //dmaDone()

  //dmaStart (i)
  //  DMA転送開始
  public static void dmaStart (int i) {
    if (DMA_DEBUG_TRACE != 0 && (DMA_DEBUG_TRACE & 1 << i) != 0) {
      System.out.printf ("%d %08x dmaStart(%d)\n", XEiJ.mpuClockTime, XEiJ.regPC0, i);
      System.out.printf ("CSR=0x%02x,CER=0x%02x,DCR=0x%02x,OCR=0x%02x,SCR=0x%02x,CCR=0x%02x,MTC=0x%04x,MAR=0x%08x,DAR=0x%08x,BTC=0x%04x,BAR=0x%08x\n",
                         dmaCOC[i] | dmaBLC[i] | dmaNDT[i] | dmaERR[i] | dmaACT[i] | dmaDIT[i] | dmaPCT[i] | dmaPCS[i],  //CSR
                         dmaErrorCode[i],  //CER
                         dmaXRM[i] | dmaDTYP[i] | dmaDPS[i] | dmaPCL[i],  //DCR
                         dmaDIR[i] | dmaBTD[i] | dmaSIZE[i] | dmaCHAIN[i] | dmaREQG[i],  //OCR
                         dmaMAC[i] | dmaDAC[i],  //SCR
                         dmaSTR[i] | dmaCNT[i] | dmaHLT[i] | dmaSAB[i] | dmaITE[i],  //CCR
                         dmaMTC[i], dmaMAR[i], dmaDAR[i], dmaBTC[i], dmaBAR[i]);
    }
    //タイミングを確認する
    if ((dmaCOC[i] | dmaBLC[i] | dmaNDT[i] | dmaERR[i] | dmaACT[i]) != 0) {  //DMA_CSRがクリアされていない状態でSTRをセットしようとした
      dmaErrorExit (i, DMA_TIMING_ERROR);
      return;
    }
    //設定を確認する
    if (((dmaDTYP[i] == DMA_HD68000_COMPATIBLE || dmaDTYP[i] == DMA_HD6800_COMPATIBLE) &&  //デュアルアドレスモードで
         dmaDPS[i] == DMA_PORT_16_BIT && dmaSIZE[i] == DMA_BYTE_SIZE &&  //DMA_DPSが16ビットでSIZEが8ビットで
         (dmaREQG[i] == DMA_EXTERNAL_REQUEST || dmaREQG[i] == DMA_DUAL_REQUEST)) ||  //外部転送要求のとき、または
        dmaXRM[i] == 0b01000000 || dmaMAC[i] == 0b00001100 || dmaDAC[i] == 0b00000011 || dmaCHAIN[i] == 0b00000100 ||  //不正な値が指定されたとき
        (dmaSIZE[i] == 0b00000011 && !((dmaDTYP[i] == DMA_HD68000_COMPATIBLE || dmaDTYP[i] == DMA_HD6800_COMPATIBLE) && dmaDPS[i] == DMA_PORT_8_BIT))) {
      dmaErrorExit (i, DMA_CONFIGURATION_ERROR);
      return;
    }
    if (dmaDPS[i] == DMA_PORT_16_BIT &&  //16ビットポートかつ
        dmaSIZE[i] == DMA_UNPACKED_8_BIT) {  //パックなし8ビット
      dmaErrorExit (i, DMA_CONFIGURATION_ERROR);  //コンフィギュレーションエラー
      return;
    }
    //パックありの準備
    dmaMemoryCarry[i] = -1;
    dmaDeviceCarry[i] = -1;
    //strには書き込まない
    //チャンネル動作開始
    dmaRequestTime[i] = XEiJ.mpuClockTime;
    dmaACT[i] = DMA_ACT;
    if (dmaCHAIN[i] == DMA_ARRAY_CHAINING) {  //アレイチェインモードのとき
      if (dmaBTC[i] == 0) {  //カウントエラー
        dmaErrorExit (i, DMA_BASE_COUNT_ERROR);
        return;
      }
      if ((dmaBAR[i] & 1) != 0) {  //アドレスエラー
        dmaErrorExit (i, DMA_BASE_ADDRESS_ERROR);
        return;
      }
      try {
        XEiJ.busWaitTime = XEiJ.busWaitCycles ? XEiJ.dmaWaitTime : XEiJ.dmaNoWaitTime;
        MemoryMappedDevice[] mm = dmaBFCMap[i];
        int a = dmaBAR[i];
        dmaMAR[i] = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRws (a) << 16 | mm[a + 2 >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a + 2);
        dmaMTC[i] = mm[a + 4 >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a + 4);
        dmaBAR[i] += 6;
        XEiJ.mpuClockTime += XEiJ.dmaCycleUnit * dmaReadCycles * 3;
        XEiJ.busWaitTime = XEiJ.busWaitCycles ? XEiJ.mpuWaitTime : XEiJ.mpuNoWaitTime;
      } catch (M68kException e) {  //バスエラー
        XEiJ.busWaitTime = XEiJ.busWaitCycles ? XEiJ.mpuWaitTime : XEiJ.mpuNoWaitTime;
        dmaErrorExit (i, DMA_BASE_BUS_ERROR);
        return;
      }
      dmaBTC[i]--;
    } else if (dmaCHAIN[i] == DMA_LINK_ARRAY_CHAINING) {  //リンクアレイチェインモードのとき
      if ((dmaBAR[i] & 1) != 0) {  //アドレスエラー
        dmaErrorExit (i, DMA_BASE_ADDRESS_ERROR);
        return;
      }
      try {
        XEiJ.busWaitTime = XEiJ.busWaitCycles ? XEiJ.dmaWaitTime : XEiJ.dmaNoWaitTime;
        MemoryMappedDevice[] mm = dmaBFCMap[i];
        int a = dmaBAR[i];
        dmaMAR[i] = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRws (a) << 16 | mm[a + 2 >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a + 2);
        dmaMTC[i] = mm[a + 4 >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a + 4);
        dmaBAR[i] = mm[a + 6 >>> XEiJ.BUS_PAGE_BITS].mmdRws (a + 6) << 16 | mm[a + 8 >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a + 8);
        XEiJ.mpuClockTime += XEiJ.dmaCycleUnit * dmaReadCycles * 5;
        XEiJ.busWaitTime = XEiJ.busWaitCycles ? XEiJ.mpuWaitTime : XEiJ.mpuNoWaitTime;
      } catch (M68kException e) {  //バスエラー
        XEiJ.busWaitTime = XEiJ.busWaitCycles ? XEiJ.mpuWaitTime : XEiJ.mpuNoWaitTime;
        dmaErrorExit (i, DMA_BASE_BUS_ERROR);
        return;
      }
    }
    //カウントを確認する
    //  アレイチェーンとリンクアレイチェーンはMTCが確定してから行うこと
    if (dmaMTC[i] == 0) {  //カウントが0
      dmaErrorExit (i, DMA_MEMORY_COUNT_ERROR);  //カウントエラー
      return;
    }
    //サイクル数補正を準備する
    //  アレイチェーンとリンクアレイチェーンはMARが確定してから行うこと
    dmaAdditionalCycles[i] = (dmaDPS[i] == DMA_PORT_8_BIT ?  //8ビットポート
                              dmaSIZE[i] == DMA_BYTE_SIZE ?  //パックあり
                              (dmaMAR[i] & 1) == 0 && dmaMACValue[i] == 0 ? 6 :  //メモリが偶数アドレスかつカウントしないとき6、
                              (dmaMAR[i] & 1) != 0 && dmaMACValue[i] == 0 ? 8 :  //メモリが奇数アドレスかつカウントしないとき8、
                              dmaDIR[i] == DMA_MEMORY_TO_DEVICE ? 8 : 10 :  //メモリからデバイスへは8、デバイスからメモリへは10
                              dmaSIZE[i] == DMA_WORD_SIZE || dmaSIZE[i] == DMA_LONG_WORD_SIZE ?  //ワードまたはロング
                              dmaDIR[i] == DMA_MEMORY_TO_DEVICE ? 4 : 0 :  //メモリからデバイスへは4、デバイスからメモリへは0
                              0  //パックなしは0
                              :  //16ビットポート
                              dmaSIZE[i] == DMA_BYTE_SIZE ?  //パックあり
                              4 + //基本は4
                              ((dmaMAR[i] & 1) != 0 && dmaMACValue[i] == 0 ? 1 : 0) +  //メモリが奇数アドレスかつカウントしないとき1追加
                              ((dmaDAR[i] & 1) != 0 && dmaDACValue[i] == 0 ? 1 : 0) :  //デバイスが奇数アドレスかつカウントしないとき1追加
                              0);  //ワードまたはロングまたはパックなしは0
    if (dmaREQG[i] == DMA_AUTO_REQUEST) {  //オートリクエスト限定速度
      dmaBurstStart = XEiJ.mpuClockTime;  //今回のバースト開始時刻
      dmaBurstEnd = dmaBurstStart + dmaBurstSpan;  //今回のバースト終了時刻
      dmaTransfer (i);  //最初のデータを転送する
    } else if (dmaREQG[i] != DMA_EXTERNAL_REQUEST) {  //オートリクエスト最大速度または最初はオートリクエスト、2番目から外部転送要求
      dmaTransfer (i);  //最初のデータを転送する
    }
  }  //dmaStart(int)

  //dmaContinue (i)
  //  転送継続
  public static void dmaContinue (int i) {
    if (DMA_DEBUG_TRACE != 0 && (DMA_DEBUG_TRACE & 1 << i) != 0) {
      System.out.printf ("%d %08x dmaContinue(%d)\n", XEiJ.mpuClockTime, XEiJ.regPC0, i);
    }
    if (dmaREQG[i] == DMA_AUTO_REQUEST) {  //オートリクエスト限定速度
      if (XEiJ.mpuClockTime < dmaBurstEnd) {  //バースト継続
        //現在時刻に次の予約を入れる
        //dmaInnerClock[i] = XEiJ.mpuClockTime;
        //動作開始時刻に次の予約を入れる
        dmaInnerClock[i] = dmaRequestTime[i];
        TickerQueue.tkqAdd (dmaTickerArray[i], dmaInnerClock[i]);
      } else {  //バースト終了
        dmaBurstStart += dmaBurstInterval;  //次回のバースト開始時刻
        if (dmaBurstStart < XEiJ.mpuClockTime) {
          dmaBurstStart = XEiJ.mpuClockTime + dmaBurstInterval;  //間に合っていないとき1周だけ延期する
        }
        dmaBurstEnd = dmaBurstStart + dmaBurstSpan;  //次回のバースト終了時刻
        //次回のバースト開始時刻に次の予約を入れる
        dmaInnerClock[i] = dmaBurstStart;
        TickerQueue.tkqAdd (dmaTickerArray[i], dmaInnerClock[i]);
      }
    } else if (dmaREQG[i] == DMA_AUTO_REQUEST_MAX) {  //オートリクエスト最大速度
      //現在時刻に次の予約を入れる
      //dmaInnerClock[i] = XEiJ.mpuClockTime;
      //動作開始時刻に次の予約を入れる
      dmaInnerClock[i] = dmaRequestTime[i];
      TickerQueue.tkqAdd (dmaTickerArray[i], dmaInnerClock[i]);
    }
  }  //dmaContinue(int)

  //dmaHalt (i,hlt)
  //  停止と再開
  public static void dmaHalt (int i, int hlt) {
    if (DMA_DEBUG_TRACE != 0 && (DMA_DEBUG_TRACE & 1 << i) != 0) {
      System.out.printf ("%d %08x dmaHalt(%d,%d)\n", XEiJ.mpuClockTime, XEiJ.regPC0, i, hlt);
    }
    if ((~dmaHLT[i] & hlt) != 0) {  //動作→停止
      if (dmaACT[i] == 0) {  //動作中でないときHLTをセットしようとした
        dmaErrorExit (i, DMA_TIMING_ERROR);
        return;
      }
      dmaHLT[i] = DMA_HLT;
      dmaRequestTime[i] = XEiJ.FAR_FUTURE;
      if (dmaInnerClock[i] != XEiJ.FAR_FUTURE) {
        dmaInnerClock[i] = XEiJ.FAR_FUTURE;
        TickerQueue.tkqRemove (dmaTickerArray[i]);
      }
    } else if ((dmaHLT[i] & ~hlt) != 0) {  //停止→動作
      dmaHLT[i] = 0;
      if (dmaACT[i] == 0) {
        return;
      }
      dmaRequestTime[i] = XEiJ.mpuClockTime;
      if (dmaREQG[i] == DMA_AUTO_REQUEST) {  //オートリクエスト限定速度
        dmaBurstStart = XEiJ.mpuClockTime;
        dmaBurstEnd = dmaBurstStart + dmaBurstSpan;
      }
      dmaContinue (i);
    }
  }

  //dmaComplete (i)
  //  転送終了
  //  dmaBLC,dmaNDTは個別に設定すること
  public static void dmaComplete (int i) {
    if (DMA_DEBUG_TRACE != 0 && (DMA_DEBUG_TRACE & 1 << i) != 0) {
      System.out.printf ("%d %08x dmaComplete(%d)\n", XEiJ.mpuClockTime, XEiJ.regPC0, i);
    }
    dmaRequestTime[i] = XEiJ.FAR_FUTURE;
    dmaCOC[i] = DMA_COC;
    dmaERR[i] = 0;
    dmaACT[i] = 0;
    dmaSTR[i] = 0;
    dmaCNT[i] = 0;
    dmaSAB[i] = 0;
    dmaErrorCode[i] = 0;
    if (dmaITE[i] != 0) {  //インタラプトイネーブル
      dmaInnerRequest[i << 1]++;
      XEiJ.mpuIRR |= XEiJ.MPU_DMA_INTERRUPT_MASK;
    }
    if (dmaInnerClock[i] != XEiJ.FAR_FUTURE) {
      dmaInnerClock[i] = XEiJ.FAR_FUTURE;
      TickerQueue.tkqRemove (dmaTickerArray[i]);
    }
  }  //dmaComplete(int)

  //dmaErrorExit (i, code)
  //  エラー終了
  //  dmaBLC,dmaNDTは操作しない
  public static void dmaErrorExit (int i, int code) {
    if (DMA_DEBUG_TRACE != 0 && (DMA_DEBUG_TRACE & 1 << i) != 0) {
      System.out.printf ("%d %08x dmaErrorExit(%d,%d)\n", XEiJ.mpuClockTime, XEiJ.regPC0, i, code);
    }
    dmaRequestTime[i] = XEiJ.FAR_FUTURE;
    dmaCOC[i] = DMA_COC;
    dmaERR[i] = DMA_ERR;
    dmaACT[i] = 0;
    dmaSTR[i] = 0;
    dmaCNT[i] = 0;
    dmaHLT[i] = 0;
    dmaSAB[i] = 0;
    dmaErrorCode[i] = code;
    if (dmaITE[i] != 0) {  //インタラプトイネーブル
      dmaInnerRequest[i << 1 | 1]++;
      XEiJ.mpuIRR |= XEiJ.MPU_DMA_INTERRUPT_MASK;
    }
    if (dmaInnerClock[i] != XEiJ.FAR_FUTURE) {
      dmaInnerClock[i] = XEiJ.FAR_FUTURE;
      TickerQueue.tkqRemove (dmaTickerArray[i]);
    }
  }  //dmaErrorExit(int,int)

  //dmaFallPCL (i) {
  public static void dmaFallPCL (int i) {
    if (DMA_DEBUG_TRACE != 0 && (DMA_DEBUG_TRACE & 1 << i) != 0) {
      System.out.printf ("%d %08x dmaFallPCL(%d)\n", XEiJ.mpuClockTime, XEiJ.regPC0, i);
    }
    if (dmaPCS[i] != 0) {
      dmaPCS[i] = 0;
      dmaPCT[i] = DMA_PCT;
    }
  }  //dmaFallPCL

  //dmaRisePCL (i)
  public static void dmaRisePCL (int i) {
    if (DMA_DEBUG_TRACE != 0 && (DMA_DEBUG_TRACE & 1 << i) != 0) {
      System.out.printf ("%d %08x dmaRisePCL(%d)\n", XEiJ.mpuClockTime, XEiJ.regPC0, i);
    }
    dmaPCS[i] = DMA_PCS;
  }  //dmaRisePCL

  //dmaFallREQ (i) {
  //  外部転送要求
  public static void dmaFallREQ (int i) {
    dmaFallREQ (i, 0);
  }
  public static void dmaFallREQ (int i, long time) {
    if (DMA_DEBUG_TRACE != 0 && (DMA_DEBUG_TRACE & 1 << i) != 0) {
      System.out.printf ("%d %08x dmaFallREQ(%d)\n", XEiJ.mpuClockTime, XEiJ.regPC0, i);
    }
    dmaReqPin[i] = 0;
    if (dmaACT[i] != 0 &&  //動作中
        (dmaREQG[i] & (DMA_EXTERNAL_REQUEST & DMA_DUAL_REQUEST)) != 0) {  //外部転送要求または最初はオートリクエスト、2番目から外部転送要求
      //現在時刻から1clk後に次の予約を入れる
      //  0clk後だとADPCMの再生に失敗する場合がある
      dmaInnerClock[i] = time != 0 ? time : XEiJ.mpuClockTime + XEiJ.dmaCycleUnit * 1;
      TickerQueue.tkqAdd (dmaTickerArray[i], dmaInnerClock[i]);
    }
  }  //dmaFallREQ

  //dmaRiseREQ (i)
  //  外部転送要求解除
  public static void dmaRiseREQ (int i) {
    if (DMA_DEBUG_TRACE != 0 && (DMA_DEBUG_TRACE & 1 << i) != 0) {
      System.out.printf ("%d %08x dmaRiseREQ(%d)\n", XEiJ.mpuClockTime, XEiJ.regPC0, i);
    }
    dmaReqPin[i] = 1;
    dmaInnerClock[i] = XEiJ.FAR_FUTURE;
    TickerQueue.tkqRemove (dmaTickerArray[i]);
  }  //dmaRiseREQ

  //dmaTransfer (i)
  //  1データ転送する
  @SuppressWarnings ("fallthrough") public static void dmaTransfer (int i) {
    if (DMA_DEBUG_TRACE != 0 && (DMA_DEBUG_TRACE & 1 << i) != 0) {
      System.out.printf ("%d %08x dmaTransfer(%d,0x%08x,0x%08x,%d)\n", XEiJ.mpuClockTime, XEiJ.regPC0, i,
                         dmaDIR[i] == DMA_MEMORY_TO_DEVICE ? dmaMAR[i] : dmaDAR[i],
                         dmaDIR[i] == DMA_MEMORY_TO_DEVICE ? dmaDAR[i] : dmaMAR[i],
                         dmaSIZE[i] == DMA_BYTE_SIZE || dmaSIZE[i] == DMA_UNPACKED_8_BIT ? 1 : dmaSIZE[i] == DMA_WORD_SIZE ? 2 : 4);
    }
    if (dmaHLT[i] != 0) {  //一時停止中
      return;  //何もしない
    }
  transfer:
    {
      int code = 0;
      try {
        XEiJ.busWaitTime = XEiJ.busWaitCycles ? XEiJ.dmaWaitTime : XEiJ.dmaNoWaitTime;
        switch (dmaSIZE[i]) {
        case DMA_BYTE_SIZE:  //バイト、パックあり
          if (dmaDIR[i] == DMA_MEMORY_TO_DEVICE) {  //メモリからデバイスへ
            //メモリから読み出す
            code = DMA_MEMORY_BUS_ERROR;
            MemoryMappedDevice[] mm = dmaMFCMap[i];
            int a = dmaMAR[i];  //メモリアドレス
            int ac = dmaMACValue[i];  //メモリアドレスカウント
            int data = dmaMemoryCarry[i];  //繰り越したデータ
            if (0 <= data) {  //繰り越したデータがある。繰り越したデータを使う
              dmaMemoryCarry[i] = -1;
            } else if ((a & 1) == 0 && ac == 1 && 2 <= dmaMTC[i]) {  //偶数アドレスで1ずつ増えて最後ではない
              data = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a);  //ワードで読み出す
              XEiJ.mpuClockTime += XEiJ.dmaCycleUnit * dmaReadCycles;
              dmaMemoryCarry[i] = data & 255;  //下位バイトを繰り越す
              data >>= 8;  //上位バイトを使う
            } else if ((a & 1) != 0 && ac == -1 && 2 <= dmaMTC[i]) {  //奇数アドレスで1ずつ減って最後ではない
              data = mm[(a - 1) >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a - 1);  //ワードで読み出す
              XEiJ.mpuClockTime += XEiJ.dmaCycleUnit * dmaReadCycles;
              dmaMemoryCarry[i] = data >> 8;  //上位バイトを繰り越す
              data &= 255;  //下位バイトを使う
            } else {  //その他
              data = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRbz (a);  //バイトで読み出して使う
              XEiJ.mpuClockTime += XEiJ.dmaCycleUnit * dmaReadCycles;
            }
            dmaMAR[i] = a + ac;  //メモリアドレスを更新する
            //EXACKの処理
            if (i == 2) {
              if (MercuryUnit.MU4_ON && MercuryUnit.mu4On) {
                MercuryUnit.mu4ExackStart ();
              }
            }
            //デバイスへ書き込む
            code = DMA_DEVICE_BUS_ERROR;
            mm = dmaDFCMap[i];
            a = dmaDAR[i];  //デバイスアドレス
            ac = dmaDACValue[i];  //デバイスアドレスカウント
            int carry = dmaDeviceCarry[i];  //繰り越したデータ
            if (0 <= carry) {  //繰り越したデータがある
              if ((a & 1) != 0) {  //奇数アドレス
                data = carry << 8 | data;  //繰り越したデータを上位バイト、今回のデータを下位バイトとする
                mm[(a - 1) >>> XEiJ.BUS_PAGE_BITS].mmdWw (a - 1, data);  //偶数アドレスにワードで書き込む
              } else {  //偶数アドレス
                data = data << 8 | carry;  //今回のデータを上位バイト、繰り越したデータを下位バイトとする
                mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWw (a, data);  //ワードで書き込む
              }
              XEiJ.mpuClockTime += XEiJ.dmaCycleUnit * dmaWriteCycles;
              dmaDeviceCarry[i] = -1;
            } else if (((a & 1) == 0 ? ac == 1 : ac == -1) && 2 <= dmaMTC[i]) {  //偶数アドレスで1ずつ増えるか奇数アドレスで1ずつ減って、最後ではない
              dmaDeviceCarry[i] = data;  //繰り越す
            } else {  //その他
              mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWb (a, data);  //今回のデータをバイトで書き込む
              XEiJ.mpuClockTime += XEiJ.dmaCycleUnit * dmaWriteCycles;
            }
            dmaDAR[i] = a + ac;  //デバイスアドレスを更新する
          } else {  //デバイスからメモリへ
            //EXACKの処理
            if (i == 2) {
              if (MercuryUnit.MU4_ON && MercuryUnit.mu4On) {
                MercuryUnit.mu4ExackStart ();
              }
            }
            //デバイスから読み出す
            code = DMA_DEVICE_BUS_ERROR;
            MemoryMappedDevice[] mm = dmaDFCMap[i];
            int a = dmaDAR[i];  //デバイスアドレス
            int ac = dmaDACValue[i];  //デバイスアドレスカウント
            int data = dmaDeviceCarry[i];  //繰り越したデータ
            if (0 <= data) {  //繰り越したデータがある。繰り越したデータを使う
              dmaDeviceCarry[i] = -1;
            } else if ((a & 1) == 0 && ac == 1 && 2 <= dmaMTC[i]) {  //偶数アドレスで1ずつ増えて最後ではない
              data = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a);  //ワードで読み出す
              XEiJ.mpuClockTime += XEiJ.dmaCycleUnit * dmaReadCycles;
              dmaDeviceCarry[i] = data & 255;  //下位バイトを繰り越す
              data >>= 8;  //上位バイトを使う
            } else if ((a & 1) != 0 && ac == -1 && 2 <= dmaMTC[i]) {  //奇数アドレスで1ずつ減って最後ではない
              data = mm[(a - 1) >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a - 1);  //ワードで読み出す
              XEiJ.mpuClockTime += XEiJ.dmaCycleUnit * dmaReadCycles;
              dmaDeviceCarry[i] = data >> 8;  //上位バイトを繰り越す
              data &= 255;  //下位バイトを使う
            } else {  //その他
              data = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRbz (a);  //バイトで読み出して使う
              XEiJ.mpuClockTime += XEiJ.dmaCycleUnit * dmaReadCycles;
            }
            dmaDAR[i] = a + ac;  //デバイスアドレスを更新する
            //メモリへ書き込む
            code = DMA_MEMORY_BUS_ERROR;
            mm = dmaMFCMap[i];
            a = dmaMAR[i];  //メモリアドレス
            ac = dmaMACValue[i];  //メモリアドレスカウント
            int carry = dmaMemoryCarry[i];  //繰り越したデータ
            if (0 <= carry) {  //繰り越したデータがある
              if ((a & 1) != 0) {  //奇数アドレス
                data = carry << 8 | data;  //繰り越したデータを上位バイト、今回のデータを下位バイトとする
                mm[(a - 1) >>> XEiJ.BUS_PAGE_BITS].mmdWw (a - 1, data);  //偶数アドレスにワードで書き込む
              } else {  //偶数アドレス
                data = data << 8 | carry;  //今回のデータを上位バイト、繰り越したデータを下位バイトとする
                mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWw (a, data);  //ワードで書き込む
              }
              XEiJ.mpuClockTime += XEiJ.dmaCycleUnit * dmaWriteCycles;
              dmaMemoryCarry[i] = -1;
            } else if (((a & 1) == 0 ? ac == 1 : ac == -1) && 2 <= dmaMTC[i]) {  //偶数アドレスで1ずつ増えるか奇数アドレスで1ずつ減って、最後ではない
              dmaMemoryCarry[i] = data;  //繰り越す
            } else {  //その他
              mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWb (a, data);  //今回のデータをバイトで書き込む
              XEiJ.mpuClockTime += XEiJ.dmaCycleUnit * dmaWriteCycles;
            }
            dmaMAR[i] = a + ac;  //メモリアドレスを更新する
          }
          break;
          //
        case DMA_WORD_SIZE:  //ワード
          if (dmaDIR[i] == DMA_MEMORY_TO_DEVICE) {  //メモリからデバイスへ
            //メモリから読み出す
            code = DMA_MEMORY_BUS_ERROR;
            MemoryMappedDevice[] mm = dmaMFCMap[i];
            int a = dmaMAR[i];  //メモリアドレス
            int ac = dmaMACValue[i];  //メモリアドレスカウント
            if ((a & 1) != 0) {  //アドレスエラー
              dmaErrorExit (i, DMA_MEMORY_ADDRESS_ERROR);
              break transfer;
            }
            int data = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a);  //ワードで読み出す
            XEiJ.mpuClockTime += XEiJ.dmaCycleUnit * dmaReadCycles;
            dmaMAR[i] = a + ac * 2;  //メモリアドレスを更新する
            //EXACKの処理
            if (i == 2) {
              if (MercuryUnit.MU4_ON && MercuryUnit.mu4On) {
                MercuryUnit.mu4ExackStart ();
              }
            }
            //デバイスへ書き込む
            code = DMA_DEVICE_BUS_ERROR;
            mm = dmaDFCMap[i];
            a = dmaDAR[i];  //デバイスアドレス
            ac = dmaDACValue[i];  //デバイスアドレスカウント
            if (dmaDPS[i] == DMA_PORT_8_BIT) {  //8ビットポート
              mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWb (a, data >> 8);  //上位バイトをバイトで書き込む
              mm[(a + 2) >>> XEiJ.BUS_PAGE_BITS].mmdWb (a + 2, data);  //下位バイトをバイトで書き込む
              XEiJ.mpuClockTime += XEiJ.dmaCycleUnit * dmaWriteCycles * 2;
            } else {  //16ビットポート
              if ((a & 1) != 0) {  //アドレスエラー
                dmaErrorExit (i, DMA_DEVICE_ADDRESS_ERROR);
                break transfer;
              }
              mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWw (a, data);  //ワードで書き込む
              XEiJ.mpuClockTime += XEiJ.dmaCycleUnit * dmaWriteCycles;
            }
            dmaDAR[i] = a + ac * 2;  //デバイスアドレスを更新する
          } else {  //デバイスからメモリへ
            //EXACKの処理
            if (i == 2) {
              if (MercuryUnit.MU4_ON && MercuryUnit.mu4On) {
                MercuryUnit.mu4ExackStart ();
              }
            }
            //デバイスから読み出す
            code = DMA_DEVICE_BUS_ERROR;
            MemoryMappedDevice[] mm = dmaDFCMap[i];
            int a = dmaDAR[i];  //デバイスアドレス
            int ac = dmaDACValue[i];  //デバイスアドレスカウント
            int data;
            if (dmaDPS[i] == DMA_PORT_8_BIT) {  //8ビットポート
              data = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRbz (a) << 8;  //上位バイトをバイトで読み出す
              data |= mm[(a + 2) >>> XEiJ.BUS_PAGE_BITS].mmdRbz (a + 2);  //下位バイトをバイトで読み出す
              XEiJ.mpuClockTime += XEiJ.dmaCycleUnit * dmaReadCycles * 2;
            } else {  //16ビットポート
              if ((a & 1) != 0) {  //アドレスエラー
                dmaErrorExit (i, DMA_DEVICE_ADDRESS_ERROR);
                break transfer;
              }
              data = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a);  //ワードで読み出す
              XEiJ.mpuClockTime += XEiJ.dmaCycleUnit * dmaReadCycles;
            }
            dmaDAR[i] = a + ac * 2;  //デバイスアドレスを更新する
            //メモリへ書き込む
            code = DMA_MEMORY_BUS_ERROR;
            mm = dmaMFCMap[i];
            a = dmaMAR[i];  //メモリアドレス
            ac = dmaMACValue[i];  //メモリアドレスカウント
            if ((a & 1) != 0) {  //アドレスエラー
              dmaErrorExit (i, DMA_MEMORY_ADDRESS_ERROR);
              break transfer;
            }
            mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWw (a, data);  //ワードで書き込む
            XEiJ.mpuClockTime += XEiJ.dmaCycleUnit * dmaWriteCycles;
            dmaMAR[i] = a + ac * 2;  //メモリアドレスを更新する
          }
          break;
          //
        case DMA_LONG_WORD_SIZE:  //ロング
          if (dmaDIR[i] == DMA_MEMORY_TO_DEVICE) {  //メモリからデバイスへ
            //メモリから読み出す
            code = DMA_MEMORY_BUS_ERROR;
            MemoryMappedDevice[] mm = dmaMFCMap[i];
            int a = dmaMAR[i];  //メモリアドレス
            int ac = dmaMACValue[i];  //メモリアドレスカウント
            if ((a & 1) != 0) {  //アドレスエラー
              dmaErrorExit (i, DMA_MEMORY_ADDRESS_ERROR);
              break transfer;
            }
            int data = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a) << 16;  //上位ワードをワードで読み出す
            data |= mm[(a + 2) >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a + 2);  //下位ワードをワードで読み出す
            XEiJ.mpuClockTime += XEiJ.dmaCycleUnit * dmaReadCycles * 2;
            dmaMAR[i] = a + ac * 4;  //メモリアドレスを更新する
            //EXACKの処理
            if (i == 2) {
              if (MercuryUnit.MU4_ON && MercuryUnit.mu4On) {
                MercuryUnit.mu4ExackStart ();
              }
            }
            //デバイスへ書き込む
            code = DMA_DEVICE_BUS_ERROR;
            mm = dmaDFCMap[i];
            a = dmaDAR[i];  //デバイスアドレス
            ac = dmaDACValue[i];  //デバイスアドレスカウント
            if (dmaDPS[i] == DMA_PORT_8_BIT) {  //8ビットポート
              mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWb (a, data >> 24);  //上位ワードの上位バイトをバイトで書き込む
              mm[(a + 2) >>> XEiJ.BUS_PAGE_BITS].mmdWb (a + 2, data >> 16);  //上位ワードの下位バイトをバイトで書き込む
              mm[(a + 4) >>> XEiJ.BUS_PAGE_BITS].mmdWb (a + 4, data >> 8);  //下位ワードの上位バイトをバイトで書き込む
              mm[(a + 6) >>> XEiJ.BUS_PAGE_BITS].mmdWb (a + 6, data);  //下位ワードの下位バイトをバイトで書き込む
              XEiJ.mpuClockTime += XEiJ.dmaCycleUnit * dmaWriteCycles * 4;
            } else {  //16ビットポート
              if ((a & 1) != 0) {  //アドレスエラー
                dmaErrorExit (i, DMA_DEVICE_ADDRESS_ERROR);
                break transfer;
              }
              mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWw (a, data >> 16);  //上位ワードをワードで書き込む
              mm[(a + 2) >>> XEiJ.BUS_PAGE_BITS].mmdWw (a + 2, data);  //下位ワードをワードで書き込む
              XEiJ.mpuClockTime += XEiJ.dmaCycleUnit * dmaWriteCycles * 2;
            }
            dmaDAR[i] = a + ac * 4;  //デバイスアドレスを更新する
          } else {  //デバイスからメモリへ
            //EXACKの処理
            if (i == 2) {
              if (MercuryUnit.MU4_ON && MercuryUnit.mu4On) {
                MercuryUnit.mu4ExackStart ();
              }
            }
            //デバイスから読み出す
            code = DMA_DEVICE_BUS_ERROR;
            MemoryMappedDevice[] mm = dmaDFCMap[i];
            int a = dmaDAR[i];  //デバイスアドレス
            int ac = dmaDACValue[i];  //デバイスアドレスカウント
            int data;
            if (dmaDPS[i] == DMA_PORT_8_BIT) {  //8ビットポート
              data = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRbz (a) << 24;  //上位ワードの上位バイトをバイトで読み出す
              data |= mm[(a + 2) >>> XEiJ.BUS_PAGE_BITS].mmdRbz (a + 2) << 16;  //上位ワードの下位バイトをバイトで読み出す
              data |= mm[(a + 4) >>> XEiJ.BUS_PAGE_BITS].mmdRbz (a + 4) << 8;  //下位ワードの上位バイトをバイトで読み出す
              data |= mm[(a + 6) >>> XEiJ.BUS_PAGE_BITS].mmdRbz (a + 6);  //下位ワードの下位バイトをバイトで読み出す
              XEiJ.mpuClockTime += XEiJ.dmaCycleUnit * dmaReadCycles * 4;
            } else {  //16ビットポート
              if ((a & 1) != 0) {  //アドレスエラー
                dmaErrorExit (i, DMA_DEVICE_ADDRESS_ERROR);
                break transfer;
              }
              data = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a) << 16;  //上位ワードをワードで読み出す
              data |= mm[(a + 2) >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a + 2);  //下位ワードをワードで読み出す
              XEiJ.mpuClockTime += XEiJ.dmaCycleUnit * dmaReadCycles * 2;
            }
            dmaDAR[i] = a + ac * 4;  //デバイスアドレスを更新する
            //メモリへ書き込む
            code = DMA_MEMORY_BUS_ERROR;
            mm = dmaMFCMap[i];
            a = dmaMAR[i];  //メモリアドレス
            ac = dmaMACValue[i];  //メモリアドレスカウント
            if ((a & 1) != 0) {  //アドレスエラー
              dmaErrorExit (i, DMA_MEMORY_ADDRESS_ERROR);
              break transfer;
            }
            mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWw (a, data >> 16);  //上位ワードをワードで書き込む
            mm[(a + 2) >>> XEiJ.BUS_PAGE_BITS].mmdWw (a + 2, data);  //下位ワードをワードで書き込む
            XEiJ.mpuClockTime += XEiJ.dmaCycleUnit * dmaWriteCycles * 2;
            dmaMAR[i] = a + ac * 4;  //メモリアドレスを更新する
          }
          break;
          //
        case DMA_UNPACKED_8_BIT:  //バイト、パックなし
          if (dmaDIR[i] == DMA_MEMORY_TO_DEVICE) {  //メモリからデバイスへ
            //メモリから読み出す
            code = DMA_MEMORY_BUS_ERROR;
            MemoryMappedDevice[] mm = dmaMFCMap[i];
            int a = dmaMAR[i];  //メモリアドレス
            int ac = dmaMACValue[i];  //メモリアドレスカウント
            int data = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRbz (a);  //バイトで読み出す
            XEiJ.mpuClockTime += XEiJ.dmaCycleUnit * dmaReadCycles;
            dmaMAR[i] = a + ac;  //メモリアドレスを更新する
            //EXACKの処理
            if (i == 2) {
              if (MercuryUnit.MU4_ON && MercuryUnit.mu4On) {
                MercuryUnit.mu4ExackStart ();
              }
            }
            //デバイスへ書き込む
            code = DMA_DEVICE_BUS_ERROR;
            mm = dmaDFCMap[i];
            a = dmaDAR[i];  //デバイスアドレス
            ac = dmaDACValue[i];  //デバイスアドレスカウント
            mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWb (a, data);  //バイトで書き込む
            XEiJ.mpuClockTime += XEiJ.dmaCycleUnit * dmaWriteCycles;
            dmaDAR[i] = a + ac;  //デバイスアドレスを更新する
          } else {  //デバイスからメモリへ
            //EXACKの処理
            if (i == 2) {
              if (MercuryUnit.MU4_ON && MercuryUnit.mu4On) {
                MercuryUnit.mu4ExackStart ();
              }
            }
            //デバイスから読み出す
            code = DMA_DEVICE_BUS_ERROR;
            MemoryMappedDevice[] mm = dmaDFCMap[i];
            int a = dmaDAR[i];  //デバイスアドレス
            int ac = dmaDACValue[i];  //デバイスアドレスカウント
            int data = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRbz (a);  //バイトで読み出す
            XEiJ.mpuClockTime += XEiJ.dmaCycleUnit * dmaReadCycles;
            dmaDAR[i] = a + ac;  //デバイスアドレスを更新する
            //メモリへ書き込む
            code = DMA_MEMORY_BUS_ERROR;
            mm = dmaMFCMap[i];
            a = dmaMAR[i];  //メモリアドレス
            ac = dmaMACValue[i];  //メモリアドレスカウント
            mm[a >>> XEiJ.BUS_PAGE_BITS].mmdWb (a, data);  //バイトで書き込む
            XEiJ.mpuClockTime += XEiJ.dmaCycleUnit * dmaWriteCycles;
            dmaMAR[i] = a + ac;  //メモリアドレスを更新する
          }
          break;
        }  //switch dmaSIZE[i]
        //サイクル数を補正する
        XEiJ.mpuClockTime += XEiJ.dmaCycleUnit * dmaAdditionalCycles[i];
        XEiJ.busWaitTime = XEiJ.busWaitCycles ? XEiJ.mpuWaitTime : XEiJ.mpuNoWaitTime;
      } catch (M68kException e) {
        XEiJ.busWaitTime = XEiJ.busWaitCycles ? XEiJ.mpuWaitTime : XEiJ.mpuNoWaitTime;
        dmaErrorExit (i, code);
        break transfer;
      }
      dmaMTC[i]--;
      if (dmaMTC[i] != 0) {  //継続
        dmaContinue (i);
      } else if (dmaCHAIN[i] == DMA_ARRAY_CHAINING) {  //アレイチェーンモードのとき
        if (dmaBTC[i] != 0) {  //継続
          //アドレスエラーのチェックは不要
          try {
            XEiJ.busWaitTime = XEiJ.busWaitCycles ? XEiJ.dmaWaitTime : XEiJ.dmaNoWaitTime;
            MemoryMappedDevice[] mm = dmaBFCMap[i];
            int a = dmaBAR[i];
            dmaMAR[i] = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRws (a) << 16 | mm[a + 2 >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a + 2);
            dmaMTC[i] = mm[a + 4 >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a + 4);
            dmaBAR[i] += 6;
            XEiJ.mpuClockTime += XEiJ.dmaCycleUnit * dmaReadCycles * 3;
            XEiJ.busWaitTime = XEiJ.busWaitCycles ? XEiJ.mpuWaitTime : XEiJ.mpuNoWaitTime;
          } catch (M68kException e) {  //バスエラー
            XEiJ.busWaitTime = XEiJ.busWaitCycles ? XEiJ.mpuWaitTime : XEiJ.mpuNoWaitTime;
            dmaErrorExit (i, DMA_BASE_BUS_ERROR);
            break transfer;
          }
          dmaBTC[i]--;
          if (dmaMTC[i] == 0) {  //カウントエラー
            dmaErrorExit (i, DMA_MEMORY_COUNT_ERROR);
            break transfer;
          }
          if ((dmaSIZE[i] == DMA_WORD_SIZE || dmaSIZE[i] == DMA_LONG_WORD_SIZE) && (dmaMAR[i] & 1) != 0) {  //アドレスエラー
            dmaErrorExit (i, DMA_MEMORY_ADDRESS_ERROR);
            break transfer;
          }
          dmaContinue (i);
        } else {  //終了
          dmaBLC[i] = DMA_BLC;
          dmaNDT[i] = 0;
          dmaComplete (i);
        }
      } else if (dmaCHAIN[i] == DMA_LINK_ARRAY_CHAINING) {  //リンクアレイチェーンモードのとき
        if (dmaBAR[i] != 0) {  //継続
          if ((dmaBAR[i] & 1) != 0) {  //アドレスエラー
            dmaErrorExit (i, DMA_BASE_ADDRESS_ERROR);
            break transfer;
          }
          try {
            XEiJ.busWaitTime = XEiJ.busWaitCycles ? XEiJ.dmaWaitTime : XEiJ.dmaNoWaitTime;
            MemoryMappedDevice[] mm = dmaBFCMap[i];
            int a = dmaBAR[i];
            dmaMAR[i] = mm[a >>> XEiJ.BUS_PAGE_BITS].mmdRws (a) << 16 | mm[a + 2 >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a + 2);
            dmaMTC[i] = mm[a + 4 >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a + 4);
            dmaBAR[i] = mm[a + 6 >>> XEiJ.BUS_PAGE_BITS].mmdRws (a + 6) << 16 | mm[a + 8 >>> XEiJ.BUS_PAGE_BITS].mmdRwz (a + 8);
            XEiJ.mpuClockTime += XEiJ.dmaCycleUnit * dmaReadCycles * 5;
            XEiJ.busWaitTime = XEiJ.busWaitCycles ? XEiJ.mpuWaitTime : XEiJ.mpuNoWaitTime;
          } catch (M68kException e) {  //バスエラー
            XEiJ.busWaitTime = XEiJ.busWaitCycles ? XEiJ.mpuWaitTime : XEiJ.mpuNoWaitTime;
            dmaErrorExit (i, DMA_BASE_BUS_ERROR);
            break transfer;
          }
          if (dmaMTC[i] == 0) {  //カウントエラー
            dmaErrorExit (i, DMA_MEMORY_COUNT_ERROR);
            break transfer;
          }
          if ((dmaSIZE[i] == DMA_WORD_SIZE || dmaSIZE[i] == DMA_LONG_WORD_SIZE) && (dmaMAR[i] & 1) != 0) {  //アドレスエラー
            dmaErrorExit (i, DMA_MEMORY_ADDRESS_ERROR);
            break transfer;
          }
          dmaContinue (i);
        } else {  //終了
          dmaBLC[i] = DMA_BLC;
          dmaNDT[i] = 0;
          dmaComplete (i);
        }
      } else if (dmaCNT[i] != 0) {  //コンティニューモードのとき
        dmaBLC[i] = DMA_BLC;
        dmaCNT[i] = 0;
        if (dmaITE[i] != 0) {  //インタラプトイネーブル
          dmaInnerRequest[i << 1]++;
          XEiJ.mpuIRR |= XEiJ.MPU_DMA_INTERRUPT_MASK;
        }
        dmaMTC[i] = dmaBTC[i];
        dmaMAR[i] = dmaBAR[i];
        if (dmaMTC[i] == 0) {  //カウントエラー
          dmaErrorExit (i, DMA_MEMORY_COUNT_ERROR);
          break transfer;
        }
        if ((dmaSIZE[i] == DMA_WORD_SIZE || dmaSIZE[i] == DMA_LONG_WORD_SIZE) && (dmaMAR[i] & 1) != 0) {  //アドレスエラー
          dmaErrorExit (i, DMA_MEMORY_ADDRESS_ERROR);
          break transfer;
        }
        dmaContinue (i);
      } else {  //終了
        dmaBLC[i] = 0;
        dmaNDT[i] = 0;
        dmaComplete (i);
      }
    }  //transfer
  }  //dmaTransfer



  public static int dmaReadByte (int a) {
    int d;
    int al = a & 0xff;
    if (al == DMA_GCR) {
      d = dmaBT | dmaBR;
      if (DMA_DEBUG_TRACE != 0) {
        System.out.printf ("%d %08x dmaRbz(0x%08x)=0x%02x\n", XEiJ.mpuClockTime, XEiJ.regPC0, a, d);
      }
    } else {
      int i = al >> 6;  //チャンネル
      switch (al & 0x3f) {
      case DMA_CSR:
        d = dmaCOC[i] | dmaBLC[i] | dmaNDT[i] | dmaERR[i] | dmaACT[i] | dmaDIT[i] | dmaPCT[i] | dmaPCS[i];
        break;
      case DMA_CER:
        d = dmaErrorCode[i];
        break;
      case DMA_DCR:
        d = dmaXRM[i] | dmaDTYP[i] | dmaDPS[i] | dmaPCL[i];
        break;
      case DMA_OCR:
        d = dmaDIR[i] | dmaBTD[i] | dmaSIZE[i] | dmaCHAIN[i] | dmaREQG[i];
        break;
      case DMA_SCR:
        d = dmaMAC[i] | dmaDAC[i];
        break;
      case DMA_CCR:
        d = dmaSTR[i] | dmaCNT[i] | dmaHLT[i] | dmaSAB[i] | dmaITE[i];
        break;
      case DMA_MTC:
        d = dmaMTC[i] >> 8;
        break;
      case DMA_MTC + 1:
        d = dmaMTC[i] & 0xff;
        break;
      case DMA_MAR:
        d = dmaMAR[i] >>> 24;
        break;
      case DMA_MAR + 1:
        d = dmaMAR[i] >> 16 & 0xff;
        break;
      case DMA_MAR + 2:
        d = (char) dmaMAR[i] >> 8;
        break;
      case DMA_MAR + 3:
        d = dmaMAR[i] & 0xff;
        break;
      case DMA_DAR:
        d = dmaDAR[i] >>> 24;
        break;
      case DMA_DAR + 1:
        d = dmaDAR[i] >> 16 & 0xff;
        break;
      case DMA_DAR + 2:
        d = (char) dmaDAR[i] >> 8;
        break;
      case DMA_DAR + 3:
        d = dmaDAR[i] & 0xff;
        break;
      case DMA_BTC:
        d = dmaBTC[i] >> 8;
        break;
      case DMA_BTC + 1:
        d = dmaBTC[i] & 0xff;
        break;
      case DMA_BAR:
        d = dmaBAR[i] >>> 24;
        break;
      case DMA_BAR + 1:
        d = dmaBAR[i] >> 16 & 0xff;
        break;
      case DMA_BAR + 2:
        d = (char) dmaBAR[i] >> 8;
        break;
      case DMA_BAR + 3:
        d = dmaBAR[i] & 0xff;
        break;
      case DMA_NIV:
        d = dmaNIV[i];
        break;
      case DMA_EIV:
        d = dmaEIV[i];
        break;
      case DMA_MFC:
        d = dmaMFC[i];
        break;
      case DMA_CPR:
        d = dmaCP[i];
        break;
      case DMA_DFC:
        d = dmaDFC[i];
        break;
      case DMA_BFC:
        d = dmaBFC[i];
        break;
      default:
        d = 0;
      }
      if (DMA_DEBUG_TRACE != 0 && (DMA_DEBUG_TRACE & 1 << i) != 0) {
        System.out.printf ("%d %08x dmaRbz(0x%08x)=0x%02x\n", XEiJ.mpuClockTime, XEiJ.regPC0, a, d);
      }
    }
    return d;
  }  //dmaReadByte

  public static int dmaReadWord (int a) {
    int d;
    int al = a & 0xff;
    int i = al >> 6;  //チャンネル
    switch (al & 0x3f) {
    case DMA_MTC:
      d = dmaMTC[i];
      break;
    case DMA_MAR:
      d = dmaMAR[i] >>> 16;
      break;
    case DMA_MAR + 2:
      d = (char) dmaMAR[i];
      break;
    case DMA_DAR:
      d = dmaDAR[i] >>> 16;
      break;
    case DMA_DAR + 2:
      d = (char) dmaDAR[i];
      break;
    case DMA_BTC:
      d = dmaBTC[i];
      break;
    case DMA_BAR:
      d = dmaBAR[i] >>> 16;
      break;
    case DMA_BAR + 2:
      d = (char) dmaBAR[i];
      break;
    default:
      d = dmaReadByte (a) << 8 | dmaReadByte (a + 1);
    }
    if (DMA_DEBUG_TRACE != 0 && (DMA_DEBUG_TRACE & 1 << i) != 0) {
      System.out.printf ("%d %08x dmaRwz(0x%08x)=0x%04x\n", XEiJ.mpuClockTime, XEiJ.regPC0, a, d);
    }
    return d;
  }  //dmaReadWord

  public static int dmaReadLong (int a) {
    a &= XEiJ.BUS_MOTHER_MASK;
    int d;
    int al = a & 0xff;
    int i = al >> 6;  //チャンネル
    switch (al & 0x3f) {
    case DMA_MAR:
      d = dmaMAR[i];
      break;
    case DMA_DAR:
      d = dmaDAR[i];
      break;
    case DMA_BAR:
      d = dmaBAR[i];
      break;
    default:
      d = dmaReadWord (a) << 16 | dmaReadWord (a + 2);
    }
    if (DMA_DEBUG_TRACE != 0 && (DMA_DEBUG_TRACE & 1 << i) != 0) {
      System.out.printf ("%d %08x dmaRls(0x%08x)=0x%08x\n", XEiJ.mpuClockTime, XEiJ.regPC0, a, d);
    }
    return d;
  }  //dmaReadLong

  public static void dmaWriteByte (int a, int d) {
    d &= 0xff;
    int al = a & 0xff;
    if (al == DMA_GCR) {
      if (DMA_DEBUG_TRACE != 0) {
        System.out.printf ("%d %08x dmaWb(0x%08x,0x%02x)\n", XEiJ.mpuClockTime, XEiJ.regPC0, a, d);
      }
      dmaBT = d & DMA_BT;
      dmaBR = d & DMA_BR;
      dmaBurstSpan = XEiJ.dmaCycleUnit << (4 + (dmaBT >> 2));
      dmaBurstInterval = dmaBurstSpan << (1 + (dmaBR & 3));
      return;
    }
    int i = al >> 6;  //チャンネル
    if (DMA_DEBUG_TRACE != 0 && (DMA_DEBUG_TRACE & 1 << i) != 0) {
      System.out.printf ("%d %08x dmaWb(0x%08x,0x%02x)\n", XEiJ.mpuClockTime, XEiJ.regPC0, a, d);
    }
    switch (al & 0x3f) {
    case DMA_CSR:
      //HD63450,-Y,-P,-PS,-CP CMOS DMAC 1989 p37
      //  COC,PCT,BTC,NDT,ERR,DITは1を書き込むかリセットするとクリアされる
      //  PCSは書き込んでも変化しない
      //  ACTは転送が終了するかリセットするとクリアされる
      if ((d & DMA_COC) != 0) {
        dmaCOC[i] = 0;
      }
      if ((d & DMA_PCT) != 0) {
        dmaPCT[i] = 0;
      }
      if ((d & DMA_BLC) != 0) {
        dmaBLC[i] = 0;  //名前注意
      }
      if ((d & DMA_NDT) != 0) {
        dmaNDT[i] = 0;
      }
      if ((d & DMA_ERR) != 0) {
        dmaERR[i] = 0;
        dmaErrorCode[i] = 0;  //CSRのERRに1を書き込んでクリアするとCERも同時にクリアされる
      }
      if ((d & DMA_DIT) != 0) {
        dmaDIT[i] = 0;
      }
      return;
    case DMA_CER:
      //DMA_CERはread-only
      return;
    case DMA_DCR:
      if (dmaACT[i] != 0) {
        dmaErrorExit (i, DMA_TIMING_ERROR);
        return;
      }
      dmaXRM[i] = d & DMA_XRM;
      dmaDTYP[i] = d & DMA_DTYP;
      dmaDPS[i] = d & DMA_DPS;
      dmaDACValue[i] = (dmaDAC[i] == DMA_INC_DAR ? 1 : dmaDAC[i] == DMA_DEC_DAR ? -1 : 0) * (dmaDPS[i] == DMA_PORT_8_BIT ? 2 : 1);
      dmaPCL[i] = d & DMA_PCL;
      return;
    case DMA_OCR:
      dmaDIR[i] = d & DMA_DIR;
      dmaBTD[i] = d & DMA_BTD;
      dmaSIZE[i] = d & DMA_SIZE;
      dmaCHAIN[i] = d & DMA_CHAIN;
      dmaREQG[i] = d & DMA_REQG;
      return;
    case DMA_SCR:
      if (dmaACT[i] != 0) {
        dmaErrorExit (i, DMA_TIMING_ERROR);
        return;
      }
      dmaMAC[i] = d & DMA_MAC;
      dmaMACValue[i] = dmaMAC[i] == DMA_INC_MAR ? 1 : dmaMAC[i] == DMA_DEC_MAR ? -1 : 0;
      dmaDAC[i] = d & DMA_DAC;
      dmaDACValue[i] = (dmaDAC[i] == DMA_INC_DAR ? 1 : dmaDAC[i] == DMA_DEC_DAR ? -1 : 0) * (dmaDPS[i] == DMA_PORT_8_BIT ? 2 : 1);
      return;
    case DMA_CCR:
      if (dmaHLT[i] != (d & DMA_HLT)) {
        dmaHalt (i, (d & DMA_HLT));
      }
      dmaITE[i] = d & DMA_ITE;
      //DMA_CNT
      if ((d & DMA_CNT) != 0) {
        if ((dmaACT[i] == 0 && (d & DMA_STR) == 0) || dmaBLC[i] != 0) {  //動作中でないかブロック転送完了済みの状態でDMA_CNTをセットしようとした
          dmaErrorExit (i, DMA_TIMING_ERROR);
          return;
        }
        if (dmaCHAIN[i] != DMA_NO_CHAINING) {  //チェインモードのときDMA_CNTをセットしようとした
          dmaErrorExit (i, DMA_CONFIGURATION_ERROR);
          return;
        }
        dmaCNT[i] = DMA_CNT;  //DMA_CNTに0を書き込んでもクリアされない
      }
      //DMA_SAB
      if ((d & DMA_SAB) != 0) {
        //dmaSABには書き込まない。SABは読み出すと常に0
        dmaCOC[i] = 0;
        dmaBLC[i] = 0;
        dmaNDT[i] = 0;
        dmaHLT[i] = 0;
        dmaCNT[i] = 0;
        if (dmaACT[i] != 0 || (d & DMA_STR) != 0) {  //動作中
          dmaErrorExit (i, DMA_SOFTWARE_ABORT);
        }
        return;
      }
      //DMA_STR
      if ((d & DMA_STR) != 0) {
        dmaStart (i);
      }
      return;
    case DMA_MTC:
      if (dmaACT[i] != 0) {
        dmaErrorExit (i, DMA_TIMING_ERROR);
        return;
      }
      dmaMTC[i] = d << 8 | (dmaMTC[i] & 0xff);
      return;
    case DMA_MTC + 1:
      if (dmaACT[i] != 0) {
        dmaErrorExit (i, DMA_TIMING_ERROR);
        return;
      }
      dmaMTC[i] = (dmaMTC[i] & ~0xff) | d;
      return;
    case DMA_MAR:
      if (dmaACT[i] != 0) {
        dmaErrorExit (i, DMA_TIMING_ERROR);
        return;
      }
      dmaMAR[i] = d << 24 | (dmaMAR[i] & ~(0xff << 24));
      return;
    case DMA_MAR + 1:
      if (dmaACT[i] != 0) {
        dmaErrorExit (i, DMA_TIMING_ERROR);
        return;
      }
      dmaMAR[i] = d << 16 | (dmaMAR[i] & ~(0xff << 16));
      return;
    case DMA_MAR + 2:
      if (dmaACT[i] != 0) {
        dmaErrorExit (i, DMA_TIMING_ERROR);
        return;
      }
      dmaMAR[i] = (dmaMAR[i] & ~(0xff << 8)) | d << 8;
      return;
    case DMA_MAR + 3:
      if (dmaACT[i] != 0) {
        dmaErrorExit (i, DMA_TIMING_ERROR);
        return;
      }
      dmaMAR[i] = (dmaMAR[i] & ~0xff) | d;
      return;
    case DMA_DAR:
      if (dmaACT[i] != 0) {
        dmaErrorExit (i, DMA_TIMING_ERROR);
        return;
      }
      dmaDAR[i] = d << 24 | (dmaDAR[i] & ~(0xff << 24));
      return;
    case DMA_DAR + 1:
      if (dmaACT[i] != 0) {
        dmaErrorExit (i, DMA_TIMING_ERROR);
        return;
      }
      dmaDAR[i] = d << 16 | (dmaDAR[i] & ~(0xff << 16));
      return;
    case DMA_DAR + 2:
      if (dmaACT[i] != 0) {
        dmaErrorExit (i, DMA_TIMING_ERROR);
        return;
      }
      dmaDAR[i] = (dmaDAR[i] & ~(0xff << 8)) | d << 8;
      return;
    case DMA_DAR + 3:
      if (dmaACT[i] != 0) {
        dmaErrorExit (i, DMA_TIMING_ERROR);
        return;
      }
      dmaDAR[i] = (dmaDAR[i] & ~0xff) | d;
      return;
    case DMA_BTC:
      dmaBTC[i] = d << 8 | (dmaBTC[i] & 0xff);
      return;
    case DMA_BTC + 1:
      dmaBTC[i] = (dmaBTC[i] & ~0xff) | d;
      return;
    case DMA_BAR:
      dmaBAR[i] = d << 24 | (dmaBAR[i] & ~(0xff << 24));
      return;
    case DMA_BAR + 1:
      dmaBAR[i] = d << 16 | (dmaBAR[i] & ~(0xff << 16));
      return;
    case DMA_BAR + 2:
      dmaBAR[i] = (dmaBAR[i] & ~(0xff << 8)) | d << 8;
      return;
    case DMA_BAR + 3:
      dmaBAR[i] = (dmaBAR[i] & ~0xff) | d;
      return;
    case DMA_NIV:
      dmaNIV[i] = d;
      return;
    case DMA_EIV:
      dmaEIV[i] = d;
      return;
    case DMA_MFC:
      if (dmaACT[i] != 0) {
        dmaErrorExit (i, DMA_TIMING_ERROR);
        return;
      }
      dmaMFC[i] = d & DMA_FC2;
      if (DataBreakPoint.DBP_ON) {
        dmaMFCMap[i] = dmaMFC[i] == 0 ? DataBreakPoint.dbpUserMap : DataBreakPoint.dbpSuperMap;
      } else {
        dmaMFCMap[i] = dmaMFC[i] == 0 ? XEiJ.busUserMap : XEiJ.busSuperMap;
      }
      return;
    case DMA_CPR:
      dmaCP[i] = d & DMA_CP;
      return;
    case DMA_DFC:
      if (dmaACT[i] != 0) {
        dmaErrorExit (i, DMA_TIMING_ERROR);
        return;
      }
      dmaDFC[i] = d & DMA_FC2;
      if (DataBreakPoint.DBP_ON) {
        dmaDFCMap[i] = dmaDFC[i] == 0 ? DataBreakPoint.dbpUserMap : DataBreakPoint.dbpSuperMap;
      } else {
        dmaDFCMap[i] = dmaDFC[i] == 0 ? XEiJ.busUserMap : XEiJ.busSuperMap;
      }
      return;
    case DMA_BFC:
      dmaBFC[i] = d & DMA_FC2;
      if (DataBreakPoint.DBP_ON) {
        dmaBFCMap[i] = dmaBFC[i] == 0 ? DataBreakPoint.dbpUserMap : DataBreakPoint.dbpSuperMap;
      } else {
        dmaBFCMap[i] = dmaBFC[i] == 0 ? XEiJ.busUserMap : XEiJ.busSuperMap;
      }
      return;
    default:
      return;
    }
  }  //dmaWriteByte

  public static void dmaWriteWord (int a, int d) {
    d = (char) d;
    int al = a & 0xff;
    int i = al >> 6;  //チャンネル
    if (DMA_DEBUG_TRACE != 0 && (DMA_DEBUG_TRACE & 1 << i) != 0) {
      System.out.printf ("%d %08x dmaWw(0x%08x,0x%04x)\n", XEiJ.mpuClockTime, XEiJ.regPC0, a, d);
    }
    switch (al & 0x3f) {
    case DMA_MTC:
      if (dmaACT[i] != 0) {
        dmaErrorExit (i, DMA_TIMING_ERROR);
      } else {
        dmaMTC[i] = d;
      }
      return;
    case DMA_MAR:
      if (dmaACT[i] != 0) {
        dmaErrorExit (i, DMA_TIMING_ERROR);
      } else {
        dmaMAR[i] = d << 16 | (char) dmaMAR[i];
      }
      return;
    case DMA_MAR + 2:
      if (dmaACT[i] != 0) {
        dmaErrorExit (i, DMA_TIMING_ERROR);
      } else {
        dmaMAR[i] = (dmaMAR[i] & ~0xffff) | d;
      }
      return;
    case DMA_DAR:
      if (dmaACT[i] != 0) {
        dmaErrorExit (i, DMA_TIMING_ERROR);
      } else {
        dmaDAR[i] = d << 16 | (char) dmaDAR[i];
      }
      return;
    case DMA_DAR + 2:
      if (dmaACT[i] != 0) {
        dmaErrorExit (i, DMA_TIMING_ERROR);
      } else {
        dmaDAR[i] = (dmaDAR[i] & ~0xffff) | d;
      }
      return;
    case DMA_BTC:
      dmaBTC[i] = (char) d;
      return;
    case DMA_BAR:
      dmaBAR[i] = d << 16 | (char) dmaBAR[i];
      return;
    case DMA_BAR + 2:
      dmaBAR[i] = (dmaBAR[i] & ~0xffff) | d;
      return;
    default:
      dmaWriteByte (a, d >> 8);
      dmaWriteByte (a + 1, d);
    }
  }  //dmaWriteWord

  public static void dmaWriteLong (int a, int d) {
    int al = a & 0xff;
    int i = al >> 6;  //チャンネル
    if (DMA_DEBUG_TRACE != 0 && (DMA_DEBUG_TRACE & 1 << i) != 0) {
      System.out.printf ("%d %08x dmaWl(0x%08x,0x%08x)\n", XEiJ.mpuClockTime, XEiJ.regPC0, a, d);
    }
    switch (al & 0x3f) {
    case DMA_MAR:
      if (dmaACT[i] != 0) {
        dmaErrorExit (i, DMA_TIMING_ERROR);
      } else {
        dmaMAR[i] = d;
        if (DMA_ALERT_HIMEM) {
          if ((d & 0xff000000) != 0 && Model.MPU_MC68020 <= XEiJ.currentMPU) {
            System.out.printf ("%08x DMA_MAR[%d]=%08X\n", XEiJ.regPC0, i, d);
          }
        }
      }
      return;
    case DMA_DAR:
      if (dmaACT[i] != 0) {
        dmaErrorExit (i, DMA_TIMING_ERROR);
      } else {
        dmaDAR[i] = d;
        if (DMA_ALERT_HIMEM) {
          if ((d & 0xff000000) != 0 && Model.MPU_MC68020 <= XEiJ.currentMPU) {
            System.out.printf ("%08x DMA_DAR[%d]=%08X\n", XEiJ.regPC0, i, d);
          }
        }
      }
      return;
    case DMA_BAR:
      dmaBAR[i] = d;
      if (DMA_ALERT_HIMEM) {
        if ((d & 0xff000000) != 0 && Model.MPU_MC68020 <= XEiJ.currentMPU) {
          System.out.printf ("%08x DMA_BAR[%d]=%08X\n", XEiJ.regPC0, i, d);
        }
      }
      return;
    default:
      dmaWriteWord (a, d >> 16);
      dmaWriteWord (a + 2, d);
    }
  }



}  //class HD63450



