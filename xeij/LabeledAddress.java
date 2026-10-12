//========================================================================================
//  LabeledAddress.java
//    en:Labeled address -- It assigns labels to addresses.
//    ja:ラベル付きアドレス -- アドレスにラベルを割り当てます。
//  Copyright (C) 2003-2025 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//    例外ベクタの名前
//    割り込みベクタの名前
//    IOCSコールベクタの名前
//    DOSコールベクタの名前
//    いくつかのIOCS処理ルーチンの名前
//    Human68kのデバイスドライバの名前と先頭からのオフセット
//    Human68kのプログラムの名前と先頭からのオフセット
//----------------------------------------------------------------------------------------

package xeij;

import java.util.*;

public class LabeledAddress {

  public static final int[] LBL_VECTOR_SPECIAL = {  //いくつかのIOCS処理ルーチンの名前
    0x0934,  //マウス受信データ処理アドレス(ソフトキーボード)
    0x0938,  //マウス受信データ処理アドレス(マウスカーソル)
    0x097e,  //拡張ESCシーケンス処理ルーチンのアドレス
    0x09b6,  //マウスデータ受信処理アドレス
    0x09be,  //カーソル点滅処理アドレス
    0x09c6,  //FDモータ停止処理アドレス
    0x09ce,  //1分処理アドレス
    0x0d00,  //[13]_B_FORMATでドライブの種類が$4～$5,$A～$Fのとき呼ばれるルーチンのアドレス
    0x0d12,  //[13]BEEP処理まるごと差し換えルーチンのアドレス([$0978.w].lが-1のとき有効)
    0x0d16,  //[13]ESC [処理まるごと差し換えルーチンのアドレス(0=差し換えない)
  };
  public static final int LBL_VECTOR_LENGTH = 512 + 256 + LBL_VECTOR_SPECIAL.length;  //ベクタの数

  public static final int LBL_MAX_DEVICES = 100;  //デバイスドライバの数の上限
  public static final int LBL_MAX_PROGRAMS = 100;  //プログラムの数の上限

  public static final String[] LBL_VECTOR_NAME = (
    //ベクタの名前
    //例外ベクタ
    "RESET_SSP," +                      //0x0000 0x0000 例外ベクタ$00 リセットSSP
    "RESET_PC," +                       //0x0004 0x0001 例外ベクタ$01 リセットPC
    "BUS_ERROR," +                      //0x0008 0x0002 例外ベクタ$02 バスエラー
    "ADDRESS_ERROR," +                  //0x000c 0x0003 例外ベクタ$03 アドレスエラー
    "ILLEGAL_INSTRUCTION," +            //0x0010 0x0004 例外ベクタ$04 不当命令
    "DIVIDE_BY_ZERO," +                 //0x0014 0x0005 例外ベクタ$05 ゼロ除算
    "CHK_INSTRUCTION," +                //0x0018 0x0006 例外ベクタ$06 CHK命令
    "TRAPV_INSTRUCTION," +              //0x001c 0x0007 例外ベクタ$07 TRAPV命令
    "PRIVILEGE_VIOLATION," +            //0x0020 0x0008 例外ベクタ$08 特権違反
    "TRACE," +                          //0x0024 0x0009 例外ベクタ$09 トレース
    "LINE_1010_EMULATOR," +             //0x0028 0x000a 例外ベクタ$0A ライン1010エミュレータ
    "LINE_1111_EMULATOR," +             //0x002c 0x000b 例外ベクタ$0B ライン1111エミュレータ
    "EMULATOR_INTERRUPT," +             //0x0030 0x000c 例外ベクタ$0C エミュレータ割り込み
    "COPROCESSOR_PROTOCOL," +           //0x0034 0x000d 例外ベクタ$0D コプロセッサプロトコル違反
    "FORMAT_ERROR," +                   //0x0038 0x000e 例外ベクタ$0E フォーマットエラー
    "UNINITIALIZED_INTERRUPT," +        //0x003c 0x000f 例外ベクタ$0F 未初期化割り込み
    "," +                               //0x0040 0x0010
    "," +                               //0x0044 0x0011
    "," +                               //0x0048 0x0012
    "," +                               //0x004c 0x0013
    "," +                               //0x0050 0x0014
    "," +                               //0x0054 0x0015
    "," +                               //0x0058 0x0016
    "," +                               //0x005c 0x0017
    "SPURIOUS_INTERRUPT," +             //0x0060 0x0018 例外ベクタ$18 スプリアス割り込み
    "LEVEL_1_INTERRUPT," +              //0x0064 0x0019 例外ベクタ$19 レベル1割り込みオートベクタ(IOI)
    "LEVEL_2_INTERRUPT," +              //0x0068 0x001a 例外ベクタ$1A レベル2割り込みオートベクタ(拡張I/Oスロット)
    "LEVEL_3_INTERRUPT," +              //0x006c 0x001b 例外ベクタ$1B レベル3割り込みオートベクタ(DMA)
    "LEVEL_4_INTERRUPT," +              //0x0070 0x001c 例外ベクタ$1C レベル4割り込みオートベクタ(拡張I/Oスロット)
    "LEVEL_5_INTERRUPT," +              //0x0074 0x001d 例外ベクタ$1D レベル5割り込みオートベクタ(SCC)
    "LEVEL_6_INTERRUPT," +              //0x0078 0x001e 例外ベクタ$1E レベル6割り込みオートベクタ(MFP)
    "LEVEL_7_INTERRUPT," +              //0x007c 0x001f 例外ベクタ$1F レベル7割り込みオートベクタ(NMI)
    "TRAP_0," +                         //0x0080 0x0020 例外ベクタ$20 TRAP#0
    "TRAP_1," +                         //0x0084 0x0021 例外ベクタ$21 TRAP#1(MPCM)
    "TRAP_2," +                         //0x0088 0x0022 例外ベクタ$22 TRAP#2(PCM8)
    "TRAP_3," +                         //0x008c 0x0023 例外ベクタ$23 TRAP#3(ZMUSIC)
    "TRAP_4," +                         //0x0090 0x0024 例外ベクタ$24 TRAP#4(MXDRV)
    "TRAP_5," +                         //0x0094 0x0025 例外ベクタ$25 TRAP#5(CDC)
    "TRAP_6," +                         //0x0098 0x0026 例外ベクタ$26 TRAP#6
    "TRAP_7," +                         //0x009c 0x0027 例外ベクタ$27 TRAP#7
    "TRAP_8," +                         //0x00a0 0x0028 例外ベクタ$28 TRAP#8(ROMデバッガ)
    "TRAP_9," +                         //0x00a4 0x0029 例外ベクタ$29 TRAP#9(デバッガ)
    "TRAP_10," +                        //0x00a8 0x002a 例外ベクタ$2A TRAP#10(POWER OFFまたはリセット)
    "TRAP_11," +                        //0x00ac 0x002b 例外ベクタ$2B TRAP#11(BREAK)
    "TRAP_12," +                        //0x00b0 0x002c 例外ベクタ$2C TRAP#12(COPY)
    "TRAP_13," +                        //0x00b4 0x002d 例外ベクタ$2D TRAP#13(^C)
    "TRAP_14," +                        //0x00b8 0x002e 例外ベクタ$2E TRAP#14(エラー表示)
    "TRAP_15," +                        //0x00bc 0x002f 例外ベクタ$2F TRAP#15(IOCSコール)
    "FP_BRANCH_SET_UNORDERED," +        //0x00c0 0x0030 例外ベクタ$30 FP分岐または比較不能状態でのセット
    "FP_INEXACT_RESULT," +              //0x00c4 0x0031 例外ベクタ$31 FP不正確な結果
    "FP_DIVIDE_BY_ZERO," +              //0x00c8 0x0032 例外ベクタ$32 FPゼロによる除算
    "FP_UNDERFLOW," +                   //0x00cc 0x0033 例外ベクタ$33 FPアンダーフロー
    "FP_OPERAND_ERROR," +               //0x00d0 0x0034 例外ベクタ$34 FPオペランドエラー
    "FP_OVERFLOW," +                    //0x00d4 0x0035 例外ベクタ$35 FPオーバーフロー
    "FP_SIGNALING_NAN," +               //0x00d8 0x0036 例外ベクタ$36 FPシグナリングNAN
    "FP_UNSUPPORTED_DATA_TYPE," +       //0x00dc 0x0037 例外ベクタ$37 FP未実装データ型
    "MMU_CONFIGULATION," +              //0x00e0 0x0038 例外ベクタ$38 MMUコンフィギュレーションエラー
    "MMU_ILLEGAL_OPERATION," +          //0x00e4 0x0039 例外ベクタ$39 MMU不当操作
    "MMU_ACCESS_LEVEL," +               //0x00e8 0x003a 例外ベクタ$3A MMUアクセスレベル違反
    "," +                               //0x00ec 0x003b
    "EFFECTIVE_ADDRESS," +              //0x00f0 0x003c 例外ベクタ$3C 未実装実効アドレス
    "INTEGER_INSTRUCTION," +            //0x00f4 0x003d 例外ベクタ$3D 未実装整数命令
    "," +                               //0x00f8 0x003e
    "," +                               //0x00fc 0x003f
    "MFP_ALARM," +                      //0x0100 0x0040 割り込みベクタ$40 MFP B0 GPIP0 RTC ALARM
    "MFP_EXPWON," +                     //0x0104 0x0041 割り込みベクタ$41 MFP B1 GPIP1 EXPWON
    "MFP_POWER," +                      //0x0108 0x0042 割り込みベクタ$42 MFP B2 GPIP2 POWER
    "MFP_OPMIRQ," +                     //0x010c 0x0043 割り込みベクタ$43 MFP B3 GPIP3 FM音源
    "MFP_TIMERD," +                     //0x0110 0x0044 割り込みベクタ$44 MFP B4 Timer-D
    "MFP_TIMERC," +                     //0x0114 0x0045 割り込みベクタ$45 MFP B5 Timer-C
    "MFP_VDISP," +                      //0x0118 0x0046 割り込みベクタ$46 MFP B6 GPIP4 CRTC V-DISP
    "," +                               //0x011c 0x0047
    "MFP_TIMERB," +                     //0x0120 0x0048 割り込みベクタ$48 MFP A0 Timer-B キーボードシリアルクロック(割り込み不可)
    "MFP_TRANSMIT_ERROR," +             //0x0124 0x0049 割り込みベクタ$49 MFP A1 キーボードシリアル出力エラー
    "MFP_TRANSMIT_EMPTY," +             //0x0128 0x004a 割り込みベクタ$4A MFP A2 キーボードシリアル出力空
    "MFP_RECEIVE_ERROR," +              //0x012c 0x004b 割り込みベクタ$4B MFP A3 キーボードシリアル入力エラー
    "MFP_RECEIVE_FULL," +               //0x0130 0x004c 割り込みベクタ$4C MFP A4 キーボードシリアル入力あり
    "MFP_TIMERA," +                     //0x0134 0x004d 割り込みベクタ$4D MFP A5 Timer-A(V-DISPイベントカウント)
    "MFP_RINT," +                       //0x0138 0x004e 割り込みベクタ$4E MFP A6 GPIP6 CRTC IRQ
    "MFP_HSYNC," +                      //0x013c 0x004f 割り込みベクタ$4F MFP A7 GPIP7 CRTC H-SYNC
    "SCC_B_EMPTY," +                    //0x0140 0x0050 割り込みベクタ$50 SCC B 送信バッファ空(マウス送信)
    "SCC_B_EMPTY," +                    //0x0144 0x0051
    "SCC_B_STATUS," +                   //0x0148 0x0052 割り込みベクタ$52 SCC B 外部/ステータス変化
    "SCC_B_STATUS," +                   //0x014c 0x0053
    "SCC_B_FULL," +                     //0x0150 0x0054 割り込みベクタ$54 SCC B 受信バッファフル(マウス受信)
    "SCC_B_FULL," +                     //0x0154 0x0055
    "SCC_B_SPECIAL," +                  //0x0158 0x0056 割り込みベクタ$56 SCC B 特別受信条件
    "SCC_B_SPECIAL," +                  //0x015c 0x0057
    "SCC_A_EMPTY," +                    //0x0160 0x0058 割り込みベクタ$58 SCC A 送信バッファ空(RS-232C送信)
    "SCC_A_EMPTY," +                    //0x0164 0x0059
    "SCC_A_STATUS," +                   //0x0168 0x005a 割り込みベクタ$5A SCC A 外部/ステータス変化
    "SCC_A_STATUS," +                   //0x016c 0x005b
    "SCC_A_FULL," +                     //0x0170 0x005c 割り込みベクタ$5C SCC A 受信バッファフル(RS-232C受信)
    "SCC_A_FULL," +                     //0x0174 0x005d
    "SCC_A_SPECIAL," +                  //0x0178 0x005e 割り込みベクタ$5E SCC A 特別受信条件
    "SCC_A_SPECIAL," +                  //0x017c 0x005f
    "IOI_FDC," +                        //0x0180 0x0060 割り込みベクタ$60 IOI FDC
    "IOI_FDD," +                        //0x0184 0x0061 割り込みベクタ$61 IOI FDD
    "IOI_SASI," +                       //0x0188 0x0062 割り込みベクタ$62 IOI SASI
    "IOI_PRN," +                        //0x018c 0x0063 割り込みベクタ$63 IOI PRN
    "DMA_0_COMPLETE," +                 //0x0190 0x0064 割り込みベクタ$64 DMA 0 転送終了
    "DMA_0_ERROR," +                    //0x0194 0x0065 割り込みベクタ$65 DMA 0 エラー
    "DMA_1_COMPLETE," +                 //0x0198 0x0066 割り込みベクタ$66 DMA 1 転送終了
    "DMA_1_ERROR," +                    //0x019c 0x0067 割り込みベクタ$67 DMA 1 エラー
    "DMA_2_COMPLETE," +                 //0x01a0 0x0068 割り込みベクタ$68 DMA 2 転送終了
    "DMA_2_ERROR," +                    //0x01a4 0x0069 割り込みベクタ$69 DMA 2 エラー
    "DMA_3_COMPLETE," +                 //0x01a8 0x006a 割り込みベクタ$6A DMA 3 転送終了
    "DMA_3_ERROR," +                    //0x01ac 0x006b 割り込みベクタ$6B DMA 3 エラー
    "SPC_IN," +                         //0x01b0 0x006c 割り込みベクタ$6C 内蔵SPC
    "," +                               //0x01b4 0x006d
    "," +                               //0x01b8 0x006e
    "," +                               //0x01bc 0x006f
    "," +                               //0x01c0 0x0070
    "," +                               //0x01c4 0x0071
    "," +                               //0x01c8 0x0072
    "," +                               //0x01cc 0x0073
    "," +                               //0x01d0 0x0074
    "," +                               //0x01d4 0x0075
    "," +                               //0x01d8 0x0076
    "," +                               //0x01dc 0x0077
    "," +                               //0x01e0 0x0078
    "," +                               //0x01e4 0x0079
    "," +                               //0x01e8 0x007a
    "," +                               //0x01ec 0x007b
    "," +                               //0x01f0 0x007c
    "," +                               //0x01f4 0x007d
    "," +                               //0x01f8 0x007e
    "," +                               //0x01fc 0x007f
    "," +                               //0x0200 0x0080
    "," +                               //0x0204 0x0081
    "," +                               //0x0208 0x0082
    "," +                               //0x020c 0x0083
    "," +                               //0x0210 0x0084
    "," +                               //0x0214 0x0085
    "," +                               //0x0218 0x0086
    "," +                               //0x021c 0x0087
    "," +                               //0x0220 0x0088
    "," +                               //0x0224 0x0089
    "," +                               //0x0228 0x008a
    "," +                               //0x022c 0x008b
    "," +                               //0x0230 0x008c
    "," +                               //0x0234 0x008d
    "," +                               //0x0238 0x008e
    "," +                               //0x023c 0x008f
    "," +                               //0x0240 0x0090
    "," +                               //0x0244 0x0091
    "," +                               //0x0248 0x0092
    "," +                               //0x024c 0x0093
    "," +                               //0x0250 0x0094
    "," +                               //0x0254 0x0095
    "," +                               //0x0258 0x0096
    "," +                               //0x025c 0x0097
    "," +                               //0x0260 0x0098
    "," +                               //0x0264 0x0099
    "," +                               //0x0268 0x009a
    "," +                               //0x026c 0x009b
    "," +                               //0x0270 0x009c
    "," +                               //0x0274 0x009d
    "," +                               //0x0278 0x009e
    "," +                               //0x027c 0x009f
    "," +                               //0x0280 0x00a0
    "," +                               //0x0284 0x00a1
    "," +                               //0x0288 0x00a2
    "," +                               //0x028c 0x00a3
    "," +                               //0x0290 0x00a4
    "," +                               //0x0294 0x00a5
    "," +                               //0x0298 0x00a6
    "," +                               //0x029c 0x00a7
    "," +                               //0x02a0 0x00a8
    "," +                               //0x02a4 0x00a9
    "," +                               //0x02a8 0x00aa
    "," +                               //0x02ac 0x00ab
    "," +                               //0x02b0 0x00ac
    "," +                               //0x02b4 0x00ad
    "," +                               //0x02b8 0x00ae
    "," +                               //0x02bc 0x00af
    "AUX3_EMPTY," +                     //0x02c0 0x00b0 割り込みベクタ$B0 RS-232Cボード(CZ-6BF1)#1 AUX3 送信バッファ空
    "AUX3_EMPTY," +                     //0x02c4 0x00b1
    "AUX3_STATUS," +                    //0x02c8 0x00b2 割り込みベクタ$B2 RS-232Cボード(CZ-6BF1)#1 AUX3 外部/ステータス変化
    "AUX3_STATUS," +                    //0x02cc 0x00b3
    "AUX3_FULL," +                      //0x02d0 0x00b4 割り込みベクタ$B4 RS-232Cボード(CZ-6BF1)#1 AUX3 受信バッファフル
    "AUX3_FULL," +                      //0x02d4 0x00b5
    "AUX3_SPECIAL," +                   //0x02d8 0x00b6 割り込みベクタ$B6 RS-232Cボード(CZ-6BF1)#1 AUX3 特別受信条件
    "AUX3_SPECIAL," +                   //0x02dc 0x00b7
    "AUX2_EMPTY," +                     //0x02e0 0x00b8 割り込みベクタ$B8 RS-232Cボード(CZ-6BF1)#1 AUX2 送信バッファ空
    "AUX2_EMPTY," +                     //0x02e4 0x00b9
    "AUX2_STATUS," +                    //0x02e8 0x00ba 割り込みベクタ$BA RS-232Cボード(CZ-6BF1)#1 AUX2 外部/ステータス変化
    "AUX2_STATUS," +                    //0x02ec 0x00bb
    "AUX2_FULL," +                      //0x02f0 0x00bc 割り込みベクタ$BC RS-232Cボード(CZ-6BF1)#1 AUX2 受信バッファフル
    "AUX2_FULL," +                      //0x02f4 0x00bd
    "AUX2_SPECIAL," +                   //0x02f8 0x00be 割り込みベクタ$BE RS-232Cボード(CZ-6BF1)#1 AUX2 特別受信条件
    "AUX2_SPECIAL," +                   //0x02fc 0x00bf
    "AUX5_EMPTY," +                     //0x0300 0x00c0 割り込みベクタ$C0 RS-232Cボード(CZ-6BF1)#2 AUX5 送信バッファ空
    "AUX5_EMPTY," +                     //0x0304 0x00c1
    "AUX5_STATUS," +                    //0x0308 0x00c2 割り込みベクタ$C2 RS-232Cボード(CZ-6BF1)#2 AUX5 外部/ステータス変化
    "AUX5_STATUS," +                    //0x030c 0x00c3
    "AUX5_FULL," +                      //0x0310 0x00c4 割り込みベクタ$C4 RS-232Cボード(CZ-6BF1)#2 AUX5 受信バッファフル
    "AUX5_FULL," +                      //0x0314 0x00c5
    "AUX5_SPECIAL," +                   //0x0318 0x00c6 割り込みベクタ$C6 RS-232Cボード(CZ-6BF1)#2 AUX5 特別受信条件
    "AUX5_SPECIAL," +                   //0x031c 0x00c7
    "AUX4_EMPTY," +                     //0x0320 0x00c8 割り込みベクタ$C8 RS-232Cボード(CZ-6BF1)#2 AUX4 送信バッファ空
    "AUX4_EMPTY," +                     //0x0324 0x00c9
    "AUX4_STATUS," +                    //0x0328 0x00ca 割り込みベクタ$CA RS-232Cボード(CZ-6BF1)#2 AUX4 外部/ステータス変化
    "AUX4_STATUS," +                    //0x032c 0x00cb
    "AUX4_FULL," +                      //0x0330 0x00cc 割り込みベクタ$CC RS-232Cボード(CZ-6BF1)#2 AUX4 受信バッファフル
    "AUX4_FULL," +                      //0x0334 0x00cd
    "AUX4_SPECIAL," +                   //0x0338 0x00ce 割り込みベクタ$CE RS-232Cボード(CZ-6BF1)#2 AUX4 特別受信条件
    "AUX4_SPECIAL," +                   //0x033c 0x00cf
    "," +                               //0x0340 0x00d0
    "," +                               //0x0344 0x00d1
    "," +                               //0x0348 0x00d2
    "," +                               //0x034c 0x00d3
    "," +                               //0x0350 0x00d4
    "," +                               //0x0354 0x00d5
    "," +                               //0x0358 0x00d6
    "," +                               //0x035c 0x00d7
    "," +                               //0x0360 0x00d8
    "," +                               //0x0364 0x00d9
    "," +                               //0x0368 0x00da
    "," +                               //0x036c 0x00db
    "," +                               //0x0370 0x00dc
    "," +                               //0x0374 0x00dd
    "," +                               //0x0378 0x00de
    "," +                               //0x037c 0x00df
    "," +                               //0x0380 0x00e0
    "," +                               //0x0384 0x00e1
    "," +                               //0x0388 0x00e2
    "," +                               //0x038c 0x00e3
    "," +                               //0x0390 0x00e4
    "," +                               //0x0394 0x00e5
    "," +                               //0x0398 0x00e6
    "," +                               //0x039c 0x00e7
    "," +                               //0x03a0 0x00e8
    "," +                               //0x03a4 0x00e9
    "," +                               //0x03a8 0x00ea
    "," +                               //0x03ac 0x00eb
    "," +                               //0x03b0 0x00ec
    "," +                               //0x03b4 0x00ed
    "," +                               //0x03b8 0x00ee
    "," +                               //0x03bc 0x00ef
    "," +                               //0x03c0 0x00f0
    "," +                               //0x03c4 0x00f1
    "," +                               //0x03c8 0x00f2
    "," +                               //0x03cc 0x00f3
    "," +                               //0x03d0 0x00f4
    "," +                               //0x03d4 0x00f5
    "," +                               //0x03d8 0x00f6
    "," +                               //0x03dc 0x00f7
    "," +                               //0x03e0 0x00f8
    "," +                               //0x03e4 0x00f9
    "," +                               //0x03e8 0x00fa
    "," +                               //0x03ec 0x00fb
    "," +                               //0x03f0 0x00fc
    "," +                               //0x03f4 0x00fd
    "," +                               //0x03f8 0x00fe
    "," +                               //0x03fc 0x00ff
    //IOCSコール
    "IOCS_B_KEYINP," +                  //0x0400 0x0100 キー入力(入力があるまで待つ,入力したデータはバッファから取り除く)
    "IOCS_B_KEYSNS," +                  //0x0404 0x0101 キーセンス(入力がなくても待たない,入力したデータをバッファから取り除かない)
    "IOCS_B_SFTSNS," +                  //0x0408 0x0102 シフトキーとLEDの状態の取得
    "IOCS_KEY_INIT," +                  //0x040c 0x0103 キーボードインタフェイスの初期化
    "IOCS_BITSNS," +                    //0x0410 0x0104 キーの押し下げ状態の取得
    "IOCS_SKEYSET," +                   //0x0414 0x0105 キー入力エミュレーション
    "IOCS_LEDCTRL," +                   //0x0418 0x0106 キーボードのLEDの状態をまとめて設定
    "IOCS_LEDSET," +                    //0x041c 0x0107 キーのLEDを再設定する
    "IOCS_KEYDLY," +                    //0x0420 0x0108 キーリピートのディレイタイム設定
    "IOCS_KEYREP," +                    //0x0424 0x0109 キーリピートのインターバル設定
    "IOCS_OPT2TVON," +                  //0x0428 0x010a OPT.2キーによるテレビコントロールを許可
    "IOCS_OPT2TVOFF," +                 //0x042c 0x010b OPT.2キーによるテレビコントロールを禁止
    "IOCS_TVCTRL," +                    //0x0430 0x010c テレビコントロール
    "IOCS_LEDMOD," +                    //0x0434 0x010d キーのLEDを設定
    "IOCS_TGUSEMD," +                   //0x0438 0x010e 画面の使用状態の取得と設定
    "IOCS_DEFCHR," +                    //0x043c 0x010f フォントパターン設定
    "IOCS_CRTMOD," +                    //0x0440 0x0110 画面モードの取得と設定
    "IOCS_CONTRAST," +                  //0x0444 0x0111 コントラストの取得と設定
    "IOCS_HSVTORGB," +                  //0x0448 0x0112 HSVからRGBを求める
    "IOCS_TPALET," +                    //0x044c 0x0113 テキストパレットの取得と設定
    "IOCS_TPALET2," +                   //0x0450 0x0114 テキストパレットの取得と設定(全色独立)
    "IOCS_TCOLOR," +                    //0x0454 0x0115 テキスト表示プレーンの設定
    "IOCS_FNTADR," +                    //0x0458 0x0116 フォントアドレスの取得
    "IOCS_VRAMGET," +                   //0x045c 0x0117 VRAMからバッファへバイト単位で転送
    "IOCS_VRAMPUT," +                   //0x0460 0x0118 バッファからVRAMへバイト単位で転送
    "IOCS_FNTGET," +                    //0x0464 0x0119 フォントパターンの取得
    "IOCS_TEXTGET," +                   //0x0468 0x011a テキストVRAMからバッファへドット単位で転送
    "IOCS_TEXTPUT," +                   //0x046c 0x011b バッファからテキストVRAMへドット単位で転送
    "IOCS_CLIPPUT," +                   //0x0470 0x011c バッファからテキストVRAMへドット単位で転送(クリッピングあり)
    "IOCS_SCROLL," +                    //0x0474 0x011d テキスト/グラフィックのスクロール位置の取得と設定
    "IOCS_B_CURON," +                   //0x0478 0x011e テキストカーソルON
    "IOCS_B_CUROFF," +                  //0x047c 0x011f テキストカーソルOFF
    "IOCS_B_PUTC," +                    //0x0480 0x0120 テキスト1文字表示
    "IOCS_B_PRINT," +                   //0x0484 0x0121 テキスト文字列表示
    "IOCS_B_COLOR," +                   //0x0488 0x0122 テキストカラーコード設定
    "IOCS_B_LOCATE," +                  //0x048c 0x0123 テキストカーソル位置設定
    "IOCS_B_DOWN_S," +                  //0x0490 0x0124 テキストカーソルを下へ1行移動(移動できないときスクロールする)
    "IOCS_B_UP_S," +                    //0x0494 0x0125 テキストカーソルを上へ1行移動(移動できないときスクロールする)
    "IOCS_B_UP," +                      //0x0498 0x0126 テキストカーソルを上へn行移動(移動できないときはエラー)
    "IOCS_B_DOWN," +                    //0x049c 0x0127 テキストカーソルを下へn行移動(移動できないときは最下行で止まる)
    "IOCS_B_RIGHT," +                   //0x04a0 0x0128 テキストカーソルをn桁右へ移動(移動できないときは右端で止まる)
    "IOCS_B_LEFT," +                    //0x04a4 0x0129 テキストカーソルをn桁左へ移動(移動できないときは左端で止まる)
    "IOCS_B_CLR_ST," +                  //0x04a8 0x012a テキスト画面クリア(クリアする範囲を選択)
    "IOCS_B_ERA_ST," +                  //0x04ac 0x012b テキスト行クリア(クリアする範囲を選択)
    "IOCS_B_INS," +                     //0x04b0 0x012c テキストカーソル行から下にn行空行を挿入
    "IOCS_B_DEL," +                     //0x04b4 0x012d テキストカーソル行からn行削除
    "IOCS_B_CONSOL," +                  //0x04b8 0x012e テキスト表示範囲を設定
    "IOCS_B_PUTMES," +                  //0x04bc 0x012f テキスト画面の指定位置に文字列表示
    "IOCS_SET232C," +                   //0x04c0 0x0130 RS-232C通信モードと通信速度の取得と設定
    "IOCS_LOF232C," +                   //0x04c4 0x0131 RS-232C受信バッファ内のデータ数の取得
    "IOCS_INP232C," +                   //0x04c8 0x0132 RS-232C受信(受信があるまで待つ,受信バッファから取り除く)
    "IOCS_ISNS232C," +                  //0x04cc 0x0133 RS-232C受信センス(受信がなくても待たない,受信バッファから取り除かない)
    "IOCS_OSNS232C," +                  //0x04d0 0x0134 RS-232C送信ステータスチェック
    "IOCS_OUT232C," +                   //0x04d4 0x0135 RS-232C送信(送信可能になるまで待つ)
    "IOCS_MS_VCS," +                    //0x04d8 0x0136 マウス受信データ処理の設定
    "IOCS_EXESC," +                     //0x04dc 0x0137 拡張ESCシーケンス処理ルーチンの設定
    "IOCS_CHR_ADR," +                   //0x04e0 0x0138 外字フォントアドレスの設定
    "IOCS_SETBEEP," +                   //0x04e4 0x0139 BEEP処理の設定
    "IOCS_SETPRN," +                    //0x04e8 0x013a プリンタ環境の設定
    "IOCS_JOYGET," +                    //0x04ec 0x013b ジョイスティックの状態の取得
    "IOCS_INIT_PRN," +                  //0x04f0 0x013c プリンタ初期化
    "IOCS_SNSPRN," +                    //0x04f4 0x013d プリンタ出力センス
    "IOCS_OUTLPT," +                    //0x04f8 0x013e プリンタ出力(LPT)
    "IOCS_OUTPRN," +                    //0x04fc 0x013f プリンタ出力(PRN)
    "IOCS_B_SEEK," +                    //0x0500 0x0140 シーク
    "IOCS_B_VERIFY," +                  //0x0504 0x0141 ベリファイ
    "IOCS_B_READDI," +                  //0x0508 0x0142 診断のための読み出し
    "IOCS_B_DSKINI," +                  //0x050c 0x0143 初期化
    "IOCS_B_DRVSNS," +                  //0x0510 0x0144 ステータスを取得
    "IOCS_B_WRITE," +                   //0x0514 0x0145 書き込み
    "IOCS_B_READ," +                    //0x0518 0x0146 読み出し
    "IOCS_B_RECALI," +                  //0x051c 0x0147 トラック0へのシーク
    "IOCS_B_ASSIGN," +                  //0x0520 0x0148 代替トラックの設定
    "IOCS_B_WRITED," +                  //0x0524 0x0149 破損データの書き込み
    "IOCS_B_READID," +                  //0x0528 0x014a ID情報を読む
    "IOCS_B_BADFMT," +                  //0x052c 0x014b 破損トラックの破棄
    "IOCS_B_READDL," +                  //0x0530 0x014c 破損データの読み出し
    "IOCS_B_FORMAT," +                  //0x0534 0x014d 物理フォーマット
    "IOCS_B_DRVCHK," +                  //0x0538 0x014e 状態の取得と設定
    "IOCS_B_EJECT," +                   //0x053c 0x014f イジェクト／シッピング
    "IOCS_DATEBCD," +                   //0x0540 0x0150 日付を時計にセットできる形式に変換する
    "IOCS_DATESET," +                   //0x0544 0x0151 時計に日付を設定する
    "IOCS_TIMEBCD," +                   //0x0548 0x0152 時刻を時計にセットできる形式に変換する
    "IOCS_TIMESET," +                   //0x054c 0x0153 時計に時刻を設定する
    "IOCS_DATEGET," +                   //0x0550 0x0154 時計から日付を読み出す
    "IOCS_DATEBIN," +                   //0x0554 0x0155 日付をBCDからバイナリに変換する
    "IOCS_TIMEGET," +                   //0x0558 0x0156 時計から時刻を読み出す
    "IOCS_TIMEBIN," +                   //0x055c 0x0157 時刻をBCDからバイナリに変換する
    "IOCS_DATECNV," +                   //0x0560 0x0158 日付を表す文字列をバイナリに変換する
    "IOCS_TIMECNV," +                   //0x0564 0x0159 時刻を表す文字列をバイナリに変換する
    "IOCS_DATEASC," +                   //0x0568 0x015a 日付をバイナリから文字列に変換する
    "IOCS_TIMEASC," +                   //0x056c 0x015b 時刻をバイナリから文字列に変換する
    "IOCS_DAYASC," +                    //0x0570 0x015c 曜日をバイナリから文字列に変換する
    "IOCS_ALARMMOD," +                  //0x0574 0x015d アラームの禁止/許可
    "IOCS_ALARMSET," +                  //0x0578 0x015e アラーム起動の時間と処理内容の設定
    "IOCS_ALARMGET," +                  //0x057c 0x015f アラーム起動の時間と処理内容の取得
    "IOCS_ADPCMOUT," +                  //0x0580 0x0160 ADPCM再生
    "IOCS_ADPCMINP," +                  //0x0584 0x0161 ADPCM録音
    "IOCS_ADPCMAOT," +                  //0x0588 0x0162 アレイチェーンによるADPCM再生
    "IOCS_ADPCMAIN," +                  //0x058c 0x0163 アレイチェーンによるADPCM録音
    "IOCS_ADPCMLOT," +                  //0x0590 0x0164 リンクアレイチェーンによるADPCM再生
    "IOCS_ADPCMLIN," +                  //0x0594 0x0165 リンクアレイチェーンによるADPCM録音
    "IOCS_ADPCMSNS," +                  //0x0598 0x0166 ADPCMの実行モードセンス
    "IOCS_ADPCMMOD," +                  //0x059c 0x0167 ADPCMの実行制御
    "IOCS_OPMSET," +                    //0x05a0 0x0168 FM音源レジスタの設定
    "IOCS_OPMSNS," +                    //0x05a4 0x0169 FM音源のステータス取得
    "IOCS_OPMINTST," +                  //0x05a8 0x016a FM音源割り込み処理ルーチンの設定
    "IOCS_TIMERDST," +                  //0x05ac 0x016b Timer-D割り込み処理ルーチンの設定
    "IOCS_VDISPST," +                   //0x05b0 0x016c Timer-A(垂直同期カウント)割り込み処理ルーチンの設定
    "IOCS_CRTCRAS," +                   //0x05b4 0x016d CRTCラスタ割り込み処理ルーチンの設定
    "IOCS_HSYNCST," +                   //0x05b8 0x016e 水平同期割り込み処理ルーチンの設定
    "IOCS_PRNINTST," +                  //0x05bc 0x016f プリンタのレディー割り込み処理ルーチンの設定
    "IOCS_MS_INIT," +                   //0x05c0 0x0170 マウス処理を初期化する
    "IOCS_MS_CURON," +                  //0x05c4 0x0171 マウスカーソルを表示する
    "IOCS_MS_CUROF," +                  //0x05c8 0x0172 マウスカーソルを消去する
    "IOCS_MS_STAT," +                   //0x05cc 0x0173 マウスカーソルの表示状態を取得する
    "IOCS_MS_GETDT," +                  //0x05d0 0x0174 マウスの状態を取得する
    "IOCS_MS_CURGT," +                  //0x05d4 0x0175 マウスカーソルの座標を取得する
    "IOCS_MS_CURST," +                  //0x05d8 0x0176 マウスカーソルの座標を設定する
    "IOCS_MS_LIMIT," +                  //0x05dc 0x0177 マウスカーソルの移動範囲を設定する
    "IOCS_MS_OFFTM," +                  //0x05e0 0x0178 マウスのボタンが離されるまでの時間を計る
    "IOCS_MS_ONTM," +                   //0x05e4 0x0179 マウスのボタンが押されるまでの時間を計る
    "IOCS_MS_PATST," +                  //0x05e8 0x017a マウスカーソルパターンを定義する
    "IOCS_MS_SEL," +                    //0x05ec 0x017b マウスカーソルを選ぶ
    "IOCS_MS_SEL2," +                   //0x05f0 0x017c マウスカーソルアニメーションの設定
    "IOCS_SKEY_MOD," +                  //0x05f4 0x017d ソフトキーボードの表示モードの取得と設定
    "IOCS_DENSNS," +                    //0x05f8 0x017e 電卓センス
    "IOCS_ONTIME," +                    //0x05fc 0x017f 起動後の経過時間(1/100秒単位)を求める
    "IOCS_B_INTVCS," +                  //0x0600 0x0180 例外処理またはIOCSコールベクタ設定
    "IOCS_B_SUPER," +                   //0x0604 0x0181 スーパーバイザモード切り替え
    "IOCS_B_BPEEK," +                   //0x0608 0x0182 メモリ読み出し(1バイト)
    "IOCS_B_WPEEK," +                   //0x060c 0x0183 メモリ読み出し(1ワード)
    "IOCS_B_LPEEK," +                   //0x0610 0x0184 メモリ読み出し(1ロングワード)
    "IOCS_B_MEMSTR," +                  //0x0614 0x0185 メモリ間転送(a1からa2へ)
    "IOCS_B_BPOKE," +                   //0x0618 0x0186 メモリ書き込み(1バイト)
    "IOCS_B_WPOKE," +                   //0x061c 0x0187 メモリ書き込み(1ワード)
    "IOCS_B_LPOKE," +                   //0x0620 0x0188 メモリ書き込み(1ロングワード)
    "IOCS_B_MEMSET," +                  //0x0624 0x0189 メモリ間転送(a2からa1へ)
    "IOCS_DMAMOVE," +                   //0x0628 0x018a DMA転送
    "IOCS_DMAMOV_A," +                  //0x062c 0x018b アレイチェーンによるDMA転送
    "IOCS_DMAMOV_L," +                  //0x0630 0x018c リンクアレイチェーンによるDMA転送
    "IOCS_DMAMODE," +                   //0x0634 0x018d DMA転送中モードの取得
    "IOCS_BOOTINF," +                   //0x0638 0x018e 起動情報の取得
    "IOCS_ROMVER," +                    //0x063c 0x018f ROMバージョンの取得
    "IOCS_G_CLR_ON," +                  //0x0640 0x0190 グラフィック画面の消去とパレット初期化と表示ON
    "IOCS_G_MOD," +                     //0x0644 0x0191 グラフィック画面モードの設定
    "IOCS_PRIORITY," +                  //0x0648 0x0192 画面間およびグラフィックページ間のプライオリティの設定
    "IOCS_CRTMOD2," +                   //0x064c 0x0193 画面表示のON/OFFと特殊モードの設定
    "IOCS_GPALET," +                    //0x0650 0x0194 グラフィックパレットの取得と設定
    "IOCS_PENCOLOR," +                  //0x0654 0x0195 ペンカラーの設定
    "IOCS_SET_PAGE," +                  //0x0658 0x0196 グラフィック描画ページの設定
    "IOCS_GGET," +                      //0x065c 0x0197 グラフィック画面からパターン読み出し
    "IOCS_MASK_GPUT," +                 //0x0660 0x0198 グラフィック画面にパターン書き込み(スルーカラー指定)
    "IOCS_GPUT," +                      //0x0664 0x0199 グラフィック画面にパターン書き込み
    "IOCS_GPTRN," +                     //0x0668 0x019a グラフィック画面にビットパターン書き込み
    "IOCS_BK_GPTRN," +                  //0x066c 0x019b グラフィック画面にビットパターン書き込み(バックカラー指定)
    "IOCS_X_GPTRN," +                   //0x0670 0x019c グラフィック画面にビットパターン書き込み(拡大指定)
    "," +                               //0x0674 0x019d
    "," +                               //0x0678 0x019e
    "," +                               //0x067c 0x019f
    "IOCS_SFTJIS," +                    //0x0680 0x01a0 SJIS→JIS変換
    "IOCS_JISSFT," +                    //0x0684 0x01a1 JIS→SJIS変換
    "IOCS_AKCONV," +                    //0x0688 0x01a2 半角(ANK)→全角(SJIS)変換
    "IOCS_RMACNV," +                    //0x068c 0x01a3 ローマ字かな変換
    "IOCS_DAKJOB," +                    //0x0690 0x01a4 濁点処理(直前の文字に゛を付ける)
    "IOCS_HANJOB," +                    //0x0694 0x01a5 半濁点処理(直前の文字に゜を付ける)
    "," +                               //0x0698 0x01a6
    "," +                               //0x069c 0x01a7
    "," +                               //0x06a0 0x01a8
    "," +                               //0x06a4 0x01a9
    "," +                               //0x06a8 0x01aa
    "," +                               //0x06ac 0x01ab
    "IOCS_SYS_STAT," +                  //0x06b0 0x01ac システム環境の取得と設定
    "IOCS_B_CONMOD," +                  //0x06b4 0x01ad テキスト画面のカーソルとスクロールの設定
    "IOCS_OS_CURON," +                  //0x06b8 0x01ae カーソル表示
    "IOCS_OS_CUROF," +                  //0x06bc 0x01af カーソル非表示(_B_CURONによる表示も禁止)
    "IOCS_DRAWMODE," +                  //0x06c0 0x01b0 グラフィック画面の描画モードの取得と設定
    "IOCS_APAGE," +                     //0x06c4 0x01b1 グラフィック画面の描画ページの取得と設定
    "IOCS_VPAGE," +                     //0x06c8 0x01b2 グラフィック画面の表示ページの設定
    "IOCS_HOME," +                      //0x06cc 0x01b3 グラフィック画面のスクロール位置の設定
    "IOCS_WINDOW," +                    //0x06d0 0x01b4 グラフィック画面のクリッピングエリアを設定する
    "IOCS_WIPE," +                      //0x06d4 0x01b5 グラフィック画面をパレットコード0で塗り潰す
    "IOCS_PSET," +                      //0x06d8 0x01b6 グラフィック画面に点を描く
    "IOCS_POINT," +                     //0x06dc 0x01b7 グラフィック画面の1点のパレットコードを得る
    "IOCS_LINE," +                      //0x06e0 0x01b8 グラフィック画面に線分を描く
    "IOCS_BOX," +                       //0x06e4 0x01b9 グラフィック画面に矩形を描く
    "IOCS_FILL," +                      //0x06e8 0x01ba グラフィック画面の矩形塗り潰し
    "IOCS_CIRCLE," +                    //0x06ec 0x01bb グラフィック画面に円または楕円を描く
    "IOCS_PAINT," +                     //0x06f0 0x01bc グラフィック画面の閉領域の塗り潰し
    "IOCS_SYMBOL," +                    //0x06f4 0x01bd グラフィック画面に文字列表示
    "IOCS_GETGRM," +                    //0x06f8 0x01be グラフィック画面の読み出し
    "IOCS_PUTGRM," +                    //0x06fc 0x01bf グラフィック画面の書き込み
    "IOCS_SP_INIT," +                   //0x0700 0x01c0 スプライトとBGの初期化
    "IOCS_SP_ON," +                     //0x0704 0x01c1 スプライト表示ON
    "IOCS_SP_OFF," +                    //0x0708 0x01c2 スプライト表示OFF
    "IOCS_SP_CGCLR," +                  //0x070c 0x01c3 スプライトパターンのクリア(16x16)
    "IOCS_SP_DEFCG," +                  //0x0710 0x01c4 スプライトパターンの設定
    "IOCS_SP_GTPCG," +                  //0x0714 0x01c5 スプライトパターンの取得
    "IOCS_SP_REGST," +                  //0x0718 0x01c6 スプライトレジスタの設定
    "IOCS_SP_REGGT," +                  //0x071c 0x01c7 スプライトレジスタの取得
    "IOCS_BGSCRLST," +                  //0x0720 0x01c8 BGスクロールレジスタの設定
    "IOCS_BGSCRLGT," +                  //0x0724 0x01c9 BGスクロールレジスタの取得
    "IOCS_BGCTRLST," +                  //0x0728 0x01ca BGコントロールレジスタの設定
    "IOCS_BGCTRLGT," +                  //0x072c 0x01cb BGコントロールレジスタの取得
    "IOCS_BGTEXTCL," +                  //0x0730 0x01cc BGテキストのクリア
    "IOCS_BGTEXTST," +                  //0x0734 0x01cd BGテキストの設定
    "IOCS_BGTEXTGT," +                  //0x0738 0x01ce BGテキストの取得
    "IOCS_SPALET," +                    //0x073c 0x01cf スプライトパレットの取得と設定
    "," +                               //0x0740 0x01d0
    "," +                               //0x0744 0x01d1
    "," +                               //0x0748 0x01d2
    "IOCS_TXXLINE," +                   //0x074c 0x01d3 テキスト画面に水平線を描画
    "IOCS_TXYLINE," +                   //0x0750 0x01d4 テキスト画面に垂直線を描画
    "IOCS_TXLINE," +                    //0x0754 0x01d5 テキスト画面に直線を描画
    "IOCS_TXBOX," +                     //0x0758 0x01d6 テキスト画面に矩形の枠を描画
    "IOCS_TXFILL," +                    //0x075c 0x01d7 テキスト画面に矩形を描画
    "IOCS_TXREV," +                     //0x0760 0x01d8 テキスト画面の矩形を反転
    "," +                               //0x0764 0x01d9
    "," +                               //0x0768 0x01da
    "," +                               //0x076c 0x01db
    "," +                               //0x0770 0x01dc
    "," +                               //0x0774 0x01dd
    "," +                               //0x0778 0x01de
    "IOCS_TXRASCPY," +                  //0x077c 0x01df テキストラスタブロックコピー
    "," +                               //0x0780 0x01e0
    "," +                               //0x0784 0x01e1
    "," +                               //0x0788 0x01e2
    "," +                               //0x078c 0x01e3
    "," +                               //0x0790 0x01e4
    "," +                               //0x0794 0x01e5
    "," +                               //0x0798 0x01e6
    "," +                               //0x079c 0x01e7
    "," +                               //0x07a0 0x01e8
    "," +                               //0x07a4 0x01e9
    "," +                               //0x07a8 0x01ea
    "," +                               //0x07ac 0x01eb
    "," +                               //0x07b0 0x01ec
    "," +                               //0x07b4 0x01ed
    "," +                               //0x07b8 0x01ee
    "," +                               //0x07bc 0x01ef
    "IOCS_OPMDRV," +                    //0x07c0 0x01f0 OPMDRV.X/OPMDRV2.X/OPMDRV3.X
    "IOCS_RSDRV," +                     //0x07c4 0x01f1 RSDRV.SYS
    "IOCS_A_JOY," +                     //0x07c8 0x01f2 AJOY.X
    "IOCS_MUSICDRV," +                  //0x07cc 0x01f3 MUSICDRV.X/OPMDRV3.X
    "," +                               //0x07d0 0x01f4
    "IOCS_SCSIDRV," +                   //0x07d4 0x01f5 SCSI
    "," +                               //0x07d8 0x01f6
    "," +                               //0x07dc 0x01f7
    "IOCS_TS16DRV," +                   //0x07e0 0x01f8 HIMEM.SYS/TS16DRV.X
    "," +                               //0x07e4 0x01f9
    "," +                               //0x07e8 0x01fa
    "," +                               //0x07ec 0x01fb
    "," +                               //0x07f0 0x01fc
    "IOCS_ABORTRST," +                  //0x07f4 0x01fd アボート準備
    "IOCS_IPLERR," +                    //0x07f8 0x01fe 起動失敗
    "IOCS_ABORTJOB," +                  //0x07fc 0x01ff アボート実行
    //DOSコール
    "DOS_EXIT," +                       //0x1800 0xff00 プロセスの終了(終了コード指定なし)
    "DOS_GETCHAR," +                    //0x1804 0xff01 標準入力から1バイト入力(標準出力にエコーバックする)
    "DOS_PUTCHAR," +                    //0x1808 0xff02 標準出力に1バイト出力
    "DOS_COMINP," +                     //0x180c 0xff03 標準シリアル入出力から1バイト入力
    "DOS_COMOUT," +                     //0x1810 0xff04 標準シリアル入出力に1バイト出力
    "DOS_PRNOUT," +                     //0x1814 0xff05 標準プリンタ出力に1バイト出力
    "DOS_INPOUT," +                     //0x1818 0xff06 標準ハンドラへの入出力
    "DOS_INKEY," +                      //0x181c 0xff07 標準入力から1バイト入力(^C,^P,^Nを処理しない)
    "DOS_GETC," +                       //0x1820 0xff08 標準入力から1バイト入力(^C,^P,^Nを処理する)
    "DOS_PRINT," +                      //0x1824 0xff09 標準出力に文字列を出力
    "DOS_GETS," +                       //0x1828 0xff0a 標準入力から文字列を入力(^C,^P,^Nを処理する)
    "DOS_KEYSNS," +                     //0x182c 0xff0b 標準入力から1バイト先読み
    "DOS_KFLUSH," +                     //0x1830 0xff0c 標準入力バッファをフラッシュしてから標準入力から入力
    "DOS_FFLUSH," +                     //0x1834 0xff0d バッファフラッシュ
    "DOS_CHGDRV," +                     //0x1838 0xff0e カレントドライブの変更
    "DOS_DRVCTRL," +                    //0x183c 0xff0f ドライブコントロール
    "DOS_CONSNS," +                     //0x1840 0xff10 標準出力への出力の可・不可を調べる
    "DOS_PRNSNS," +                     //0x1844 0xff11 標準プリンタ出力への出力の可・不可を調べる
    "DOS_CINSNS," +                     //0x1848 0xff12 標準シリアル入出力からの入力の可・不可を調べる
    "DOS_COUTSNS," +                    //0x184c 0xff13 標準シリアル入出力への出力の可・不可を調べる
    "," +                               //0x1850 0xff14
    "," +                               //0x1854 0xff15
    "," +                               //0x1858 0xff16
    "DOS_FATCHK," +                     //0x185c 0xff17 ファイルやディレクトリのFATの繋がりを調べる
    "DOS_HENDSP," +                     //0x1860 0xff18 かな漢字変換ウィンドウの表示
    "DOS_CURDRV," +                     //0x1864 0xff19 カレントドライブ番号を得る
    "DOS_GETSS," +                      //0x1868 0xff1a 標準入力から文字列を入力(^C,^P,^Nを処理しない)
    "DOS_FGETC," +                      //0x186c 0xff1b ハンドラから1バイト入力
    "DOS_FGETS," +                      //0x1870 0xff1c ハンドラから文字列を入力
    "DOS_FPUTC," +                      //0x1874 0xff1d ハンドラへ1バイト出力
    "DOS_FPUTS," +                      //0x1878 0xff1e ハンドラへ文字列を出力
    "DOS_ALLCLOSE," +                   //0x187c 0xff1f 実行中のプロセスとその子プロセスがオープンしたハンドラをすべてクローズする
    "DOS_SUPER," +                      //0x1880 0xff20 スーパーバイザモードの切り替え
    "DOS_FNCKEY," +                     //0x1884 0xff21 再定義可能キーの読み込みと設定
    "DOS_KNJCTRL," +                    //0x1888 0xff22 かな漢字変換の制御
    "DOS_CONCTRL," +                    //0x188c 0xff23 コンソール出力の制御
    "DOS_KEYCTRL," +                    //0x1890 0xff24 コンソール入力の制御
    "DOS_INTVCS," +                     //0x1894 0xff25 例外処理ベクタの設定
    "DOS_PSPSET," +                     //0x1898 0xff26 プロセス管理テーブルの作成
    "DOS_GETTIM2," +                    //0x189c 0xff27 時刻を得る(ロングワード)
    "DOS_SETTIM2," +                    //0x18a0 0xff28 時刻を設定する(ロングワード)
    "DOS_NAMESTS," +                    //0x18a4 0xff29 ファイル名の分解
    "DOS_GETDATE," +                    //0x18a8 0xff2a 日付を得る
    "DOS_SETDATE," +                    //0x18ac 0xff2b 日付を設定する
    "DOS_GETTIME," +                    //0x18b0 0xff2c 時刻を得る(ワード)
    "DOS_SETTIME," +                    //0x18b4 0xff2d 時刻を設定する(ワード)
    "DOS_VERIFY," +                     //0x18b8 0xff2e verifyのモードの設定
    "DOS_DUP0," +                       //0x18bc 0xff2f 標準ハンドラの変換
    "DOS_VERNUM," +                     //0x18c0 0xff30 Humanのバージョンの取得
    "DOS_KEEPPR," +                     //0x18c4 0xff31 プロセスの常駐終了
    "DOS_GETDPB," +                     //0x18c8 0xff32 DPBの取得
    "DOS_BREAKCK," +                    //0x18cc 0xff33 breakおよびoffの取得と設定
    "DOS_DRVXCHG," +                    //0x18d0 0xff34 ドライブの入れ換え
    "DOS_INTVCG," +                     //0x18d4 0xff35 例外処理ベクタの取得
    "DOS_DSKFRE," +                     //0x18d8 0xff36 ドライブの空容量の取得
    "DOS_NAMECK," +                     //0x18dc 0xff37 ファイル名のチェック
    "," +                               //0x18e0 0xff38
    "DOS_MKDIR," +                      //0x18e4 0xff39 ディレクトリの作成
    "DOS_RMDIR," +                      //0x18e8 0xff3a ディレクトリの削除
    "DOS_CHDIR," +                      //0x18ec 0xff3b カレントディレクトリの設定
    "DOS_CREATE," +                     //0x18f0 0xff3c 新規ファイルの作成
    "DOS_OPEN," +                       //0x18f4 0xff3d ファイルのオープン
    "DOS_CLOSE," +                      //0x18f8 0xff3e ハンドラのクローズ
    "DOS_READ," +                       //0x18fc 0xff3f ハンドラから指定されたサイズのデータを読み込む
    "DOS_WRITE," +                      //0x1900 0xff40 ハンドラへ指定されたサイズのデータを書き込む
    "DOS_DELETE," +                     //0x1904 0xff41 ファイルの削除
    "DOS_SEEK," +                       //0x1908 0xff42 ハンドラのシーク位置の変更
    "DOS_CHMOD," +                      //0x190c 0xff43 ファイルまたはディレクトリの属性の読み込みと設定
    "DOS_IOCTRL," +                     //0x1910 0xff44 デバイスによるハンドラの直接制御
    "DOS_DUP," +                        //0x1914 0xff45 ハンドラの複製
    "DOS_DUP2," +                       //0x1918 0xff46 ハンドラの複写
    "DOS_CURDIR," +                     //0x191c 0xff47 カレントディレクトリの取得
    "DOS_MALLOC," +                     //0x1920 0xff48 メモリブロックの確保(下位から)
    "DOS_MFREE," +                      //0x1924 0xff49 メモリブロックの開放
    "DOS_SETBLOCK," +                   //0x1928 0xff4a メモリブロックのサイズの変更
    "DOS_EXEC," +                       //0x192c 0xff4b 子プロセスの実行
    "DOS_EXIT2," +                      //0x1930 0xff4c プロセスの終了(終了コード指定あり)
    "DOS_WAIT," +                       //0x1934 0xff4d 子プロセスの終了コードの取得
    "DOS_FILES," +                      //0x1938 0xff4e ディレクトリエントリの検索(最初)
    "DOS_NFILES," +                     //0x193c 0xff4f ディレクトリエントリの検索(次)
    "DOS_V2_SETPDB," +                  //0x1940 0xff50 プロセス管理テーブルの移動
    "DOS_V2_GETPDB," +                  //0x1944 0xff51 プロセス管理テーブルの取得
    "DOS_V2_SETENV," +                  //0x1948 0xff52 環境変数の設定
    "DOS_V2_GETENV," +                  //0x194c 0xff53 環境変数の取得
    "DOS_V2_VERIFYG," +                 //0x1950 0xff54 verifyのモードの取得
    "DOS_V2_COMMON," +                  //0x1954 0xff55 common領域の制御
    "DOS_V2_RENAME," +                  //0x1958 0xff56 ファイル名またはディレクトリ名の変更およびファイルの移動
    "DOS_V2_FILEDATE," +                //0x195c 0xff57 ハンドラの更新日時の取得と設定
    "DOS_V2_MALLOC2," +                 //0x1960 0xff58 メモリブロックの確保(モード指定あり)
    "," +                               //0x1964 0xff59
    "DOS_V2_MAKETMP," +                 //0x1968 0xff5a テンポラリファイルの作成
    "DOS_V2_NEWFILE," +                 //0x196c 0xff5b 新規ファイルの作成(非破壊)
    "DOS_V2_LOCK," +                    //0x1970 0xff5c ハンドラのロックの制御
    "," +                               //0x1974 0xff5d
    "," +                               //0x1978 0xff5e
    "DOS_V2_ASSIGN," +                  //0x197c 0xff5f 仮想ドライブおよび仮想ディレクトリの取得と設定
    "DOS_V2_MALLOC3," +                 //0x1980 0xff60 [060turbo.sys] ブロックの確保(下位から)
    "DOS_V2_SETBLOCK2," +               //0x1984 0xff61 [060turbo.sys] ブロックのサイズ変更
    "DOS_V2_MALLOC4," +                 //0x1988 0xff62 [060turbo.sys] ブロックの確保(モード指定あり)
    "DOS_V2_S_MALLOC2," +               //0x198c 0xff63 [060turbo.sys] メインスレッドのメモリ空間からのブロックの確保
    "," +                               //0x1990 0xff64
    "," +                               //0x1994 0xff65
    "," +                               //0x1998 0xff66
    "," +                               //0x199c 0xff67
    "," +                               //0x19a0 0xff68
    "," +                               //0x19a4 0xff69
    "," +                               //0x19a8 0xff6a
    "," +                               //0x19ac 0xff6b
    "," +                               //0x19b0 0xff6c
    "," +                               //0x19b4 0xff6d
    "," +                               //0x19b8 0xff6e
    "," +                               //0x19bc 0xff6f
    "," +                               //0x19c0 0xff70
    "," +                               //0x19c4 0xff71
    "," +                               //0x19c8 0xff72
    "," +                               //0x19cc 0xff73
    "," +                               //0x19d0 0xff74
    "," +                               //0x19d4 0xff75
    "," +                               //0x19d8 0xff76
    "," +                               //0x19dc 0xff77
    "," +                               //0x19e0 0xff78
    "," +                               //0x19e4 0xff79
    "DOS_V2_FFLUSH_SET," +              //0x19e8 0xff7a fflushのモードの取得と設定(2.15～)
    "DOS_V2_OS_PATCH," +                //0x19ec 0xff7b Humanの変更(2.15～)
    "DOS_V2_GETFCB," +                  //0x19f0 0xff7c FCBテーブルの取得
    "DOS_V2_S_MALLOC," +                //0x19f4 0xff7d メインスレッドのメモリ管理からメモリブロックを確保
    "DOS_V2_S_MFREE," +                 //0x19f8 0xff7e メインスレッドのメモリ管理からメモリブロックを削除
    "DOS_V2_S_PROCESS," +               //0x19fc 0xff7f サブのメモリ管理の設定
    "DOS_SETPDB," +                     //0x1a00 0xff80 プロセス管理テーブルの移動
    "DOS_GETPDB," +                     //0x1a04 0xff81 プロセス管理テーブルの取得
    "DOS_SETENV," +                     //0x1a08 0xff82 環境変数の設定
    "DOS_GETENV," +                     //0x1a0c 0xff83 環境変数の取得
    "DOS_VERIFYG," +                    //0x1a10 0xff84 verifyのモードの取得
    "DOS_COMMON," +                     //0x1a14 0xff85 common領域の制御
    "DOS_RENAME," +                     //0x1a18 0xff86 ファイル名またはディレクトリ名の変更およびファイルの移動
    "DOS_FILEDATE," +                   //0x1a1c 0xff87 ハンドラの更新日時の取得と設定
    "DOS_MALLOC2," +                    //0x1a20 0xff88 メモリブロックの確保(モード指定あり)
    "," +                               //0x1a24 0xff89
    "DOS_MAKETMP," +                    //0x1a28 0xff8a テンポラリファイルの作成
    "DOS_NEWFILE," +                    //0x1a2c 0xff8b 新規ファイルの作成(非破壊)
    "DOS_LOCK," +                       //0x1a30 0xff8c ハンドラのロックの制御
    "," +                               //0x1a34 0xff8d
    "," +                               //0x1a38 0xff8e
    "DOS_ASSIGN," +                     //0x1a3c 0xff8f 仮想ドライブおよび仮想ディレクトリの取得と設定
    "DOS_MALLOC3," +                    //0x1a40 0xff90 [060turbo.sys] ブロックの確保(下位から)
    "DOS_SETBLOCK2," +                  //0x1a44 0xff91 [060turbo.sys] ブロックのサイズ変更
    "DOS_MALLOC4," +                    //0x1a48 0xff92 [060turbo.sys] ブロックの確保(モード指定あり)
    "DOS_S_MALLOC2," +                  //0x1a4c 0xff93 [060turbo.sys] メインスレッドのメモリ空間からのブロックの確保
    "," +                               //0x1a50 0xff94
    "," +                               //0x1a54 0xff95
    "," +                               //0x1a58 0xff96
    "," +                               //0x1a5c 0xff97
    "," +                               //0x1a60 0xff98
    "," +                               //0x1a64 0xff99
    "," +                               //0x1a68 0xff9a
    "," +                               //0x1a6c 0xff9b
    "," +                               //0x1a70 0xff9c
    "," +                               //0x1a74 0xff9d
    "," +                               //0x1a78 0xff9e
    "," +                               //0x1a7c 0xff9f
    "," +                               //0x1a80 0xffa0
    "," +                               //0x1a84 0xffa1
    "," +                               //0x1a88 0xffa2
    "," +                               //0x1a8c 0xffa3
    "," +                               //0x1a90 0xffa4
    "," +                               //0x1a94 0xffa5
    "," +                               //0x1a98 0xffa6
    "," +                               //0x1a9c 0xffa7
    "," +                               //0x1aa0 0xffa8
    "," +                               //0x1aa4 0xffa9
    "DOS_FFLUSH_SET," +                 //0x1aa8 0xffaa fflushのモードの取得と設定(2.15～)
    "DOS_OS_PATCH," +                   //0x1aac 0xffab Humanの変更(2.15～)
    "DOS_GETFCB," +                     //0x1ab0 0xffac FCBテーブルの取得
    "DOS_S_MALLOC," +                   //0x1ab4 0xffad メインスレッドのメモリ管理からメモリブロックを確保
    "DOS_S_MFREE," +                    //0x1ab8 0xffae メインスレッドのメモリ管理からメモリブロックを削除
    "DOS_S_PROCESS," +                  //0x1abc 0xffaf サブのメモリ管理の設定
    "," +                               //0x1ac0 0xffb0
    "," +                               //0x1ac4 0xffb1
    "," +                               //0x1ac8 0xffb2
    "," +                               //0x1acc 0xffb3
    "," +                               //0x1ad0 0xffb4
    "," +                               //0x1ad4 0xffb5
    "," +                               //0x1ad8 0xffb6
    "," +                               //0x1adc 0xffb7
    "," +                               //0x1ae0 0xffb8
    "," +                               //0x1ae4 0xffb9
    "," +                               //0x1ae8 0xffba
    "," +                               //0x1aec 0xffbb
    "," +                               //0x1af0 0xffbc
    "," +                               //0x1af4 0xffbd
    "," +                               //0x1af8 0xffbe
    "," +                               //0x1afc 0xffbf
    "," +                               //0x1b00 0xffc0
    "," +                               //0x1b04 0xffc1
    "," +                               //0x1b08 0xffc2
    "," +                               //0x1b0c 0xffc3
    "," +                               //0x1b10 0xffc4
    "," +                               //0x1b14 0xffc5
    "," +                               //0x1b18 0xffc6
    "," +                               //0x1b1c 0xffc7
    "," +                               //0x1b20 0xffc8
    "," +                               //0x1b24 0xffc9
    "," +                               //0x1b28 0xffca
    "," +                               //0x1b2c 0xffcb
    "," +                               //0x1b30 0xffcc
    "," +                               //0x1b34 0xffcd
    "," +                               //0x1b38 0xffce
    "," +                               //0x1b3c 0xffcf
    "," +                               //0x1b40 0xffd0
    "," +                               //0x1b44 0xffd1
    "," +                               //0x1b48 0xffd2
    "," +                               //0x1b4c 0xffd3
    "," +                               //0x1b50 0xffd4
    "," +                               //0x1b54 0xffd5
    "," +                               //0x1b58 0xffd6
    "," +                               //0x1b5c 0xffd7
    "," +                               //0x1b60 0xffd8
    "," +                               //0x1b64 0xffd9
    "," +                               //0x1b68 0xffda
    "," +                               //0x1b6c 0xffdb
    "," +                               //0x1b70 0xffdc
    "," +                               //0x1b74 0xffdd
    "," +                               //0x1b78 0xffde
    "," +                               //0x1b7c 0xffdf
    "," +                               //0x1b80 0xffe0
    "," +                               //0x1b84 0xffe1
    "," +                               //0x1b88 0xffe2
    "," +                               //0x1b8c 0xffe3
    "," +                               //0x1b90 0xffe4
    "," +                               //0x1b94 0xffe5
    "," +                               //0x1b98 0xffe6
    "," +                               //0x1b9c 0xffe7
    "," +                               //0x1ba0 0xffe8
    "," +                               //0x1ba4 0xffe9
    "," +                               //0x1ba8 0xffea
    "," +                               //0x1bac 0xffeb
    "," +                               //0x1bb0 0xffec
    "," +                               //0x1bb4 0xffed
    "," +                               //0x1bb8 0xffee
    "," +                               //0x1bbc 0xffef
    "DOS_EXITVC," +                     //0x1bc0 0xfff0 _EXITVC(プロセスが終了したときのジャンプ先のベクタ)
    "DOS_CTRLVC," +                     //0x1bc4 0xfff1 _CTRLVC(^Cのときのジャンプ先のベクタ)
    "DOS_ERRJVC," +                     //0x1bc8 0xfff2 _ERRJVC(システムエラーが発生したときのジャンプ先のベクタ)
    "DOS_DISKRED," +                    //0x1bcc 0xfff3 ハンドラから直接読み込む
    "DOS_DISKWRT," +                    //0x1bd0 0xfff4 ハンドラに直接書き込む
    "DOS_INDOSFLG," +                   //0x1bd4 0xfff5 DOSワークのアドレス
    "DOS_SUPER_JSR," +                  //0x1bd8 0xfff6 スーパーバイザサブルーチンの呼び出し
    "DOS_BUS_ERR," +                    //0x1bdc 0xfff7 バスエラーチェック
    "DOS_OPEN_PR," +                    //0x1be0 0xfff8 バックグラウンドタスクの登録(スレッドが生成されたとき呼ばれるベクタ)
    "DOS_KILL_PR," +                    //0x1be4 0xfff9 プロセスの削除(スレッドが消滅したとき呼ばれるベクタ)
    "DOS_GET_PR," +                     //0x1be8 0xfffa スレッド管理情報の取得
    "DOS_SUSPEND_PR," +                 //0x1bec 0xfffb 強制スリープ
    "DOS_SLEEP_PR," +                   //0x1bf0 0xfffc スリープ
    "DOS_SEND_PR," +                    //0x1bf4 0xfffd スレッド間通信
    "DOS_TIME_PR," +                    //0x1bf8 0xfffe タイマカウンタの取得
    "DOS_CHANGE_PR," +                  //0x1bfc 0xffff 実行権の放棄(スレッドが切り替わったとき呼ばれるベクタ)
    //その他
    "BIOS_SOFTKEY_HANDLE," +            //0x0934 マウス受信データ処理アドレス(ソフトキーボード)
    "BIOS_MOUSE_HANDLE," +              //0x0938 マウス受信データ処理アドレス(マウスカーソル)
    "BIOS_ESCAPE_EXTENSION," +          //0x097e 拡張ESCシーケンス処理ルーチンのアドレス
    "BIOS_TC_MOUSE_HANDLE," +           //0x09b6 マウスデータ受信処理アドレス
    "BIOS_TC_CURSOR_HANDLE," +          //0x09be カーソル点滅処理アドレス
    "BIOS_TC_MOTOR_HANDLE," +           //0x09c6 FDモータ停止処理アドレス
    "BIOS_TC_MINUTE_HANDLE," +          //0x09ce 1分処理アドレス
    "BIOS_FDD_EXTENSION," +             //0x0d00 [13,FDDEVICE] _B_FORMATでドライブの種類が$4～$5,$A～$Fのとき呼ばれるルーチンのアドレス
    "BIOS_BEEP_EXTENSION," +            //0x0d12 [13] BEEP処理まるごと差し換えルーチンのアドレス。BIOS_BEEP_DATA=-1のとき有効
    "BIOS_CSI_EXTENSION"                //0x0d16 [13] ESC [処理まるごと差し換えルーチンのアドレス。0=差し換えない
    ).split (",", LBL_VECTOR_LENGTH);

  public static final int[] lblVectorTable = new int[LBL_VECTOR_LENGTH];  //ベクタのアドレス

  public static final String[] lblProgramName = new String[LBL_MAX_DEVICES + LBL_MAX_PROGRAMS];  //デバイスドライバとプログラムの名前
  public static final int[] lblProgramHead = new int[LBL_MAX_DEVICES + LBL_MAX_PROGRAMS];  //デバイスドライバとプログラムの先頭アドレス
  public static final int[] lblProgramTail = new int[LBL_MAX_DEVICES + LBL_MAX_PROGRAMS];  //デバイスドライバとプログラムの末尾アドレス
  public static int lblProgramCount;
  public static int lblProgramAge;  //プログラムを更新したときインクリメントするカウンタ。同期に使う

  //lblClear ()
  //  ラベルをクリアする
  //  デバイスドライバを初期化した直後とプロセスを起動する直前に呼ばれる
  public static void lblClear () {
    lblProgramCount = 0;
  }  //lblClear

  //lblUpdateVector ()
  //  ベクタを更新する
  public static void lblUpdateVector () {
    int i = 0;
    for (int v = 0x0000; v <= 0x00ff; v++) {  //例外ベクタ
      lblVectorTable[i++] = MC68060.mmuPeekLongData (XEiJ.mpuVBR + (v << 2), 1);
    }
    for (int v = 0x0100; v <= 0x01ff; v++) {  //IOCSコール
      lblVectorTable[i++] = MC68060.mmuPeekLongData (v << 2, 1);
    }
    for (int v = 0xff00; v <= 0xffff; v++) {  //DOSコール
      lblVectorTable[i++] = MC68060.mmuPeekLongData (0x1800 + ((v & 0xff) << 2), 1);
    }
    for (int v : LBL_VECTOR_SPECIAL) {
      lblVectorTable[i++] = MC68060.mmuPeekLongData (v, 1);
    }
  }

  //lblUpdateProgram ()
  //  プログラムの範囲を更新する
  public static void lblUpdateProgram () {
    int count = 0;
    int top = MainMemory.mmrHumanTop ();  //Human68kのメモリ管理の先頭(HUMAN.SYSのメモリ管理テーブル)
    int btm = MainMemory.mmrHumanBtm ();  //Human68kのメモリ管理の末尾
    int pmm = MainMemory.mmrHumanPmm ();  //Human68kの実行中のプロセスのメモリ管理テーブル
    int nul = MainMemory.mmrHumanNul ();  //Human68kのNULデバイスドライバ
    if (top >= 0 && btm >= 0 && pmm >= 0 && nul >= 0) {  //すべて確認できた
      //デバイスドライバのリストを作る
      int devEnd = MC68060.mmuPeekLongData (top + 8, 1);  //HUMAN.SYSの未使用領域の先頭
      for (int a = nul, i = 0; a < devEnd && i < LBL_MAX_DEVICES; i++) {
        int b = MC68060.mmuPeekLongData (a, 1);  //次のデバイスドライバの先頭
        if (b < 0 || devEnd < b) {  //最後のデバイスドライバまたはHUMAN.SYSの外
          b = devEnd;
        }
        int l = 8;
        for (; l > 0 && MC68060.mmuPeekByteSignData (a + (14 - 1) + l, 1) == ' '; l--) {
        }
        lblProgramName[count] = MC68060.mmuPeekStringL (new StringBuilder (), a + 14, l, 1).toString ();  //デバイス名
        lblProgramHead[count] = a;
        lblProgramTail[count] = b;
        count++;
        a = b;
      }
      //プログラムのリストを作る
      //  メモリ管理テーブル
      //       0.l      直前のメモリ管理テーブルのアドレス(なければ0)
      //       4.l      このブロックを確保したプロセスのメモリ管理テーブルのアドレス
      //                最上位1バイト
      //                  0x00  通常のメモリブロック
      //                  0xfd  _S_PROCESSによるサブのメモリ管理の親のメモリブロック
      //                  0xff  常駐したプロセスのメモリブロック
      //       8.l      このブロックの未使用領域の先頭
      //                  メモリブロックの確保
      //                    既存のメモリブロックの中から十分な未使用領域を持つものを選ぶ
      //                    未使用領域の先頭に新しいメモリ管理テーブルを構築して前後のメモリ管理テーブルのリストに挿入する
      //                  メモリブロックの開放
      //                    メモリ管理テーブルをリストから切り離す
      //                    切り離したメモリ管理テーブルの先頭から直後のメモリブロックの手前までを直前のメモリブロックの未使用領域に取り込む
      //      12.l      直後のメモリ管理テーブルのアドレス(なければ0)
      //                  直後のメモリ管理テーブルがあるときはこのブロックの未使用領域の末尾
      //                  このメモリブロックが末尾のときは[0x00001c00]をこのブロックの未使用領域の末尾とみなす
      //     (16バイト)
      //  プロセス管理テーブル
      //      16.l      環境の領域のアドレス
      //      20.l      _EXITVCのベクタ(親の_EXECの直後)
      //      24.l      _CTRLVCのベクタ
      //      28.l      _ERRJVCのベクタ
      //      32.l      コマンドラインのアドレス
      //      36.b[12]  ハンドラの使用状況
      //      48.l      bssの先頭
      //      52.l      ヒープの先頭
      //      56.l      スタックエリアの先頭
      //      60.l      親のUSP
      //      64.l      親のSSP
      //      68.w      親のSR
      //      70.w      アボート時のSR
      //      72.l      アボート時のSSP
      //      76.l      TRAP#10のベクタ
      //      80.l      TRAP#11のベクタ
      //      84.l      TRAP#12のベクタ
      //      88.l      TRAP#13のベクタ
      //      92.l      TRAP#14のベクタ
      //      96.l      OSフラグ(-1=CONFIG.SYSのSHELLで起動,0=その他)
      //     100.b      モジュール番号
      //     101.b[3]   未定義
      //     104.l      子プロセスのメモリ管理テーブル
      //     108.l[5]   予約
      //     128.b[68]  実行ファイルのパス
      //     196.b[24]  実行ファイルのファイル名
      //     220.l[9]   予約
      //    (256バイト)
      //!!! human100,human101はプロセス管理テーブルの構造が異なる
      //
      //  HUMAN.SYSから末尾のメモリブロックまで、常駐しているプロセスを記録しながら、直後のメモリ管理テーブルを辿る
      //  実行中のプロセスからHUMAN.SYSまで、プロセスを記録しながら、メモリブロックを確保したプロセスのメモリ管理テーブルを遡る
      //
      //  名前はプロセス管理テーブルにある主ファイル名以下0x00の手前までをSJISから変換した文字列
      //  先頭はプロセス管理テーブルの直後
      //  末尾はメモリ管理テーブルにある未使用領域の先頭
      //
      //  Human68kのメモリ管理が壊れているときは辿れるところまで表示する
      //  Human68kのメモリ管理が壊れたためにX68000が暴走したときでもエミュレータが一緒になってハングアップしてはならない
      //
      //HUMAN.SYSから末尾のメモリブロックまで、常駐しているプロセスを記録しながら、直後のメモリ管理テーブルを辿る
      for (int a = top; 0 < a && a < btm; a = MC68060.mmuPeekLongData (a + 12, 1)) {
        if ((MC68060.mmuPeekByteSignData (a + 4, 1) & 0xe0) == 0xe0) {  //常駐したプロセスのメモリ管理テーブル
          lblProgramName[count] = MC68060.mmuPeekStringL (new StringBuilder (), a + 196, MC68060.mmuPeekStrlen (a + 196, 24, 1), 1).toString ();
          lblProgramHead[count] = a == top ? 0x00000000 : a + 256;  //HUMAN.SYSだけ絶対アドレスで表示するため先頭を0x00000000に変更する
          lblProgramTail[count] = MC68060.mmuPeekLongData (a + 8, 1);
          count++;
        }
      }
      //実行中のプロセスからHUMAN.SYSまで、プロセスを記録しながら、メモリブロックを確保したプロセスのメモリ管理テーブルを遡る
      for (int a = pmm; 0 < a && a < btm; a = MC68060.mmuPeekLongData (a + 4, 1)) {
        lblProgramName[count] = MC68060.mmuPeekStringL (new StringBuilder (), a + 196, MC68060.mmuPeekStrlen (a + 196, 24, 1), 1).toString ();
        lblProgramHead[count] = a == top ? 0x00000000 : a + 256;  //HUMAN.SYSだけ絶対アドレスで表示するため先頭を0x00000000に変更する
        lblProgramTail[count] = MC68060.mmuPeekLongData (a + 8, 1);
        count++;
      }
    }
    lblProgramCount = count;
    //アドレス昇順にソートする
    for (int i = 0; i + 1 < count; i++) {
      int a = lblProgramHead[i];  //i..count-1の中の最小
      int j = i;
      for (int k = i + 1; k < count; k++) {
        int b = lblProgramHead[k];
        if (b < a) {
          a = b;
          j = k;
        }
      }
      if (i < j) {  //iとjを入れ替える
        int t = lblProgramHead[i];
        lblProgramHead[i] = lblProgramHead[j];
        lblProgramHead[j] = t;
        t = lblProgramTail[i];
        lblProgramTail[i] = lblProgramTail[j];
        lblProgramTail[j] = t;
        String s = lblProgramName[i];
        lblProgramName[i] = lblProgramName[j];
        lblProgramName[j] = s;
      }
    }
    //同名のラベルが複数あるときアドレス昇順に枝番を付ける
    //  数を数える
    HashMap<String,Integer> map = new HashMap<String,Integer> ();  //ラベル→個数
    for (int i = 0; i < count; i++) {
      String s = lblProgramName[i];
      if (map.containsKey (s)) {
        map.put (s, map.get (s) + 1);
      } else {
        map.put (s, 1);
      }
    }
    //  1個のものを取り除く
    for (int i = 0; i < count; i++) {
      String s = lblProgramName[i];
      if (map.get (s) == 1) {
        map.remove (s);
      }
    }
    //  2個以上あるものに枝番を付ける
    for (int i = count - 1; 0 <= i; i--) {  //後ろから
      String s = lblProgramName[i];
      if (map.containsKey (s)) {  //取り除かれていない。2個以上ある
        int n = map.get (s);
        lblProgramName[i] += "#" + n;
        map.put (s, n - 1);
      }
    }
    //更新あり
    lblProgramAge++;
  }  //lblUpdate()


  public static int lblLastGetAge = -1;
  public static int lblLastGetHead = 0;  //範囲の先頭。プログラムの内側の場合と外側の場合がある
  public static int lblLastGetTail = 0;  //範囲の末尾
  public static int lblLastGetIndex = -1;  //インデックス
  public static boolean lblLastGetInside = false;  //true=プログラムの内側,false=外側

  //index = lblGetIndex (a)
  //  現在のラベルでアドレスを含むプログラムのインデックスを返す。-1=該当なし
  public static int lblGetIndex (int a) {
    if (lblLastGetAge == lblProgramAge &&  //前回の呼び出しからラベルが変わっていない
        Integer.compareUnsigned (lblLastGetHead, a) <= 0 &&
        Integer.compareUnsigned (a, lblLastGetTail) < 0) {  //同じ範囲にある
      return lblLastGetIndex;  //前回返したインデックスを返す
    }
    lblLastGetAge = lblProgramAge;
    lblLastGetInside = false;
    lblLastGetIndex = -1;
    int outsideHead = 0x00000000;  //外側の範囲の先頭
    int outsideTail = 0xffffffff;  //外側の範囲の末尾
    for (int i = 0; i < LabeledAddress.lblProgramCount; i++) {
      int head = LabeledAddress.lblProgramHead[i];  //プログラムの先頭
      int tail = LabeledAddress.lblProgramTail[i];  //プログラムの末尾
      if (Integer.compareUnsigned (tail, head) <= 0) {
        continue;  //念の為
      }
      if (Integer.compareUnsigned (head, a) <= 0 &&
          Integer.compareUnsigned (a, tail) < 0) {  //プログラムの範囲内
        lblLastGetInside = true;
        lblLastGetIndex = i;
        lblLastGetHead = head;
        lblLastGetTail = tail;
        break;
      }
      if (Integer.compareUnsigned (outsideHead, tail) < 0 &&
          Integer.compareUnsigned (tail, a) < 0) {  //外側の範囲の先頭を更新する
        outsideHead = tail;
      } else if (Integer.compareUnsigned (a, head) < 0 &&
                 Integer.compareUnsigned (head, outsideTail) < 0) {  //外側の範囲の末尾を更新する
        outsideTail = head;
      }
    }
    if (!lblLastGetInside) {  //プログラムの範囲外
      lblLastGetHead = outsideHead;
      lblLastGetTail = outsideTail;
    }
    return lblLastGetIndex;
  }  //lblGetIndex


  //sb = lblSearch (sb, a)
  //  ラベルを検索する
  public static StringBuilder lblSearch (StringBuilder sb, int a) {
    //ベクタを更新する
    lblUpdateVector ();
    //プログラムの範囲を更新する
    if (lblProgramCount == 0) {
      lblUpdateProgram ();
    }
    //ベクタを検索する
    for (int i = 0; i < LBL_VECTOR_LENGTH; i++) {
      if (a == lblVectorTable[i]) {
        String n = LBL_VECTOR_NAME[i];
        if (n.length () > 0) {
          sb.append ('.').append (n);
        } else if (i < 256) {
          XEiJ.fmtHex4 (sb.append (".$"), i);
        } else if (i < 512) {
          XEiJ.fmtHex2 (sb.append (".IOCS_$"), i - 256);
        } else if (i < 768) {
          XEiJ.fmtHex4 (sb.append (".DOS_$"), 0xff00 - 512 + i);
        } else {
          XEiJ.fmtHex4 (sb.append (".$"), LBL_VECTOR_SPECIAL[i - 768]);
        }
      }
    }
    //プログラムの範囲を検索する
    for (int i = 0; i < lblProgramCount; i++) {
      if (lblProgramHead[i] <= a && a < lblProgramTail[i]) {
        sb.append (" @ ").append (lblProgramName[i]);
        if (lblProgramHead[i] != 0) {  //HUMAN.SYSのときは冗長なので書かない
          XEiJ.fmtHex8 (sb.append (" $"), a - lblProgramHead[i]);
        }
        return sb;
      }
    }
    return sb;
  }  //lblSearch

  //s = lblDump ()
  //  ラベル情報をダンプする
  public static String lblDump () {
    //ベクタを更新する
    lblUpdateVector ();
    //プログラムの範囲を更新する
    if (lblProgramCount == 0) {
      lblUpdateProgram ();
    }
    //ベクタをアドレスでソートする
    int[] va = new int[LBL_VECTOR_LENGTH];
    System.arraycopy (lblVectorTable, 0, va, 0, LBL_VECTOR_LENGTH);
    int[] via = new int[LBL_VECTOR_LENGTH];
    for (int vi = 0; vi < LBL_VECTOR_LENGTH; vi++) {
      via[vi] = vi;
    }
    for (int i = 0; i <= LBL_VECTOR_LENGTH - 2; i++) {
      int m = i;
      for (int j = i + 1; j <= LBL_VECTOR_LENGTH - 1; j++) {
        if (va[j] < va[m]) {
          m = j;
        }
      }
      if (i < m) {
        int v = va[m];
        int vi = via[m];
        for (int j = m - 1; i <= j; j--) {
          va[j + 1] = va[j];
          via[j + 1] = via[j];
        }
        va[i] = v;
        via[i] = vi;
      }
    }
    //ダンプする
    int undefDosCall = MC68060.mmuPeekLongData (0x1bd4, 1);  //DOS_INDOSFLGと同じアドレスは未定義DOSコール
    StringBuilder sb = new StringBuilder ();
    sb.append ("  head     tail         name\n" +
               "-------- -------- ----------------\n" +
               "     handle  address   offset     label\n" +
               "    -------- -------- -------- ------------\n");
    int prevPi = -2;
    for (int i = 0; i < LBL_VECTOR_LENGTH; i++) {
      int vi = via[i];  //ベクタの番号
      int v = (vi < 512 ? vi << 2 :  //例外とIOCSコール
               vi < 768 ? 0x1800 + ((vi - 512) << 2) :  //DOSコール
               LBL_VECTOR_SPECIAL[vi - 768]  //その他
               );  //ベクタ
      int a = lblVectorTable[vi];  //アドレス
      String n = LBL_VECTOR_NAME[vi];  //名前
      int pi = -1;  //aを範囲に含む、範囲が最も狭いプロセスの番号
      int l = 0x7fffffff;
      for (int tempPi = 0; tempPi < lblProgramCount; tempPi++) {
        int tempH = lblProgramHead[tempPi];
        int tempT = lblProgramTail[tempPi];
        int tempL = tempT - tempH;
        if (tempH <= a && a < tempT &&
            tempL < l) {
          if (true) {
            if (tempH < 0x00f00000 && 0x00ffffff < tempT) {  //ROMを跨いでいる
              continue;
            }
          }
          pi = tempPi;
          l = tempL;
          break;
        }
      }
      if (prevPi != pi) {
        if (0 <= pi) {
          XEiJ.fmtHex8 (sb, lblProgramHead[pi]);
          sb.append (' ');
          XEiJ.fmtHex8 (sb, lblProgramTail[pi]);
          sb.append (' ');
          sb.append (lblProgramName[pi]);
          sb.append ('\n');
        }
        prevPi = pi;
      }
      if (0 <= pi && a != 0 && a != undefDosCall) {
        sb.append ("    ");
        XEiJ.fmtHex8 (sb, v);
        sb.append (' ');
        XEiJ.fmtHex8 (sb, a);
        sb.append (' ');
        XEiJ.fmtHex8 (sb, a - lblProgramHead[pi]);
        sb.append (' ');
        sb.append (LBL_VECTOR_NAME[vi]);
        sb.append ('\n');
      }
    }  //for i
    return sb.toString ();
  }  //lblDump

}  //class LabeledAddress



