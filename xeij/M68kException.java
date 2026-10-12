//========================================================================================
//  M68kException.java
//    en:M68k exception
//    ja:M68kの例外
//  Copyright (C) 2003-2025 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//  throwするオブジェクトは1個だけで個々の例外の情報は持たない
//  例外の情報をフィールドに格納してからthrowする
//  プログラムカウンタはバスエラーとアドレスエラーのときはpc0、それ以外はpcを用いる
//----------------------------------------------------------------------------------------

package xeij;

import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System

public class M68kException extends Exception {

  //特殊例外
  public static final int M6E_INSTRUCTION_BREAK_POINT = -1;  //命令ブレークポイント
  public static final int M6E_WAIT_EXCEPTION          = -2;  //待機例外

  //例外ベクタ番号
  //  F=スタックされたPCは命令の先頭を指す
  //  N=スタックされたPCは次の命令を指す
  public static final int M6E_RESET_INITIAL_SSP              =  0;  //0000     012346  Reset Initial Interrupt Stack Pointer
  public static final int M6E_RESET_INITIAL_PC               =  1;  //0004     012346  Reset Initial Program Counter
  public static final int M6E_ACCESS_FAULT                   =  2;  //4008  F  012346  Access Fault アクセスフォルト
  public static final int M6E_ADDRESS_ERROR                  =  3;  //200C  F  012346  Address Error アドレスエラー
  public static final int M6E_ILLEGAL_INSTRUCTION            =  4;  //0010  F  012346  Illegal Instruction 不当命令
  public static final int M6E_DIVIDE_BY_ZERO                 =  5;  //2014  N  012346  Integer Divide by Zero ゼロ除算
  public static final int M6E_CHK_INSTRUCTION                =  6;  //2018  N  012346  CHK, CHK2 instructions CHK命令
  public static final int M6E_TRAPV_INSTRUCTION              =  7;  //201C  N  012346  FTRAPcc, TRAPcc, TRAPV instructions TRAPV命令
  public static final int M6E_PRIVILEGE_VIOLATION            =  8;  //0020  F  012346  Privilege Violation 特権違反
  public static final int M6E_TRACE                          =  9;  //2024  N  012346  Trace トレース
  public static final int M6E_LINE_1010_EMULATOR             = 10;  //0028  F  012346  Line 1010 Emulator (Unimplemented A-Line opcode) ライン1010エミュレータ(SXコール)
  public static final int M6E_LINE_1111_EMULATOR             = 11;  //002C  F  012346  Line 1111 Emulator (Unimplemented F-Line opcode) ライン1111エミュレータ(DOSコール,FEファンクションコール)
  public static final int M6E_FP_UNIMPLEMENTED_INSTRUCTION   = 11;  //202C  N  -----6  未実装浮動小数点命令
  public static final int M6E_FP_DISABLED                    = 11;  //402C  N  -----6  浮動小数点無効
  public static final int M6E_EMULATOR_INTERRUPT             = 12;  //0030  N  -----S  エミュレータ割り込み
  public static final int M6E_COPROCESSOR_PROTOCOL_VIOLATION = 13;  //0034     ---C--  コプロセッサプロトコル違反
  public static final int M6E_FORMAT_ERROR                   = 14;  //0038  F  -12346  フォーマットエラー
  public static final int M6E_UNINITIALIZED_INTERRUPT        = 15;  //003C  N  012346  未初期化割り込み
  public static final int M6E_SPURIOUS_INTERRUPT             = 24;  //0060  N  012346  スプリアス割り込み
  public static final int M6E_LEVEL_1_INTERRUPT_AUTOVECTOR   = 25;  //0064  N  012346  レベル1割り込みオートベクタ(FDC,FDD,ハードディスク,プリンタ)
  public static final int M6E_LEVEL_2_INTERRUPT_AUTOVECTOR   = 26;  //0068  N  012346  レベル2割り込みオートベクタ(拡張I/Oスロット)
  public static final int M6E_LEVEL_3_INTERRUPT_AUTOVECTOR   = 27;  //006C  N  012346  レベル3割り込みオートベクタ(DMAコントローラ転送終了など)
  public static final int M6E_LEVEL_4_INTERRUPT_AUTOVECTOR   = 28;  //0070  N  012346  レベル4割り込みオートベクタ(拡張I/Oスロット)
  public static final int M6E_LEVEL_5_INTERRUPT_AUTOVECTOR   = 29;  //0074  N  012346  レベル5割り込みオートベクタ(SCC RS-232C,マウス)
  public static final int M6E_LEVEL_6_INTERRUPT_AUTOVECTOR   = 30;  //0078  N  012346  レベル6割り込みオートベクタ(MFP 各種タイマ,KEY,同期など)
  public static final int M6E_LEVEL_7_INTERRUPT_AUTOVECTOR   = 31;  //007C  N  012346  レベル7割り込みオートベクタ(NMI)
  public static final int M6E_TRAP_0_INSTRUCTION_VECTOR      = 32;  //0080  N  012346  TRAP#0命令ベクタ
  public static final int M6E_TRAP_1_INSTRUCTION_VECTOR      = 33;  //0084  N  012346  TRAP#1命令ベクタ(MPCM,BGDRV)
  public static final int M6E_TRAP_2_INSTRUCTION_VECTOR      = 34;  //0088  N  012346  TRAP#2命令ベクタ(PCM8)
  public static final int M6E_TRAP_3_INSTRUCTION_VECTOR      = 35;  //008C  N  012346  TRAP#3命令ベクタ(ZMUSIC,ZMSC3,MIDDRV)
  public static final int M6E_TRAP_4_INSTRUCTION_VECTOR      = 36;  //0090  N  012346  TRAP#4命令ベクタ(MXDRV,MADRV,MLD,MCDRV)
  public static final int M6E_TRAP_5_INSTRUCTION_VECTOR      = 37;  //0094  N  012346  TRAP#5命令ベクタ(CDC)
  public static final int M6E_TRAP_6_INSTRUCTION_VECTOR      = 38;  //0098  N  012346  TRAP#6命令ベクタ
  public static final int M6E_TRAP_7_INSTRUCTION_VECTOR      = 39;  //009C  N  012346  TRAP#7命令ベクタ
  public static final int M6E_TRAP_8_INSTRUCTION_VECTOR      = 40;  //00A0  N  012346  TRAP#8命令ベクタ(ROMデバッガがブレークポイントに使用)M6E_
  public static final int M6E_TRAP_9_INSTRUCTION_VECTOR      = 41;  //00A4  N  012346  TRAP#9命令ベクタ(デバッガがブレークポイントに使用)
  public static final int M6E_TRAP_10_INSTRUCTION_VECTOR     = 42;  //00A8  N  012346  TRAP#10命令ベクタ(POWER OFFまたはリセット)
  public static final int M6E_TRAP_11_INSTRUCTION_VECTOR     = 43;  //00AC  N  012346  TRAP#11命令ベクタ(BREAK)
  public static final int M6E_TRAP_12_INSTRUCTION_VECTOR     = 44;  //00B0  N  012346  TRAP#12命令ベクタ(COPY)
  public static final int M6E_TRAP_13_INSTRUCTION_VECTOR     = 45;  //00B4  N  012346  TRAP#13命令ベクタ(^C)
  public static final int M6E_TRAP_14_INSTRUCTION_VECTOR     = 46;  //00B8  N  012346  TRAP#14命令ベクタ(エラー表示)
  public static final int M6E_TRAP_15_INSTRUCTION_VECTOR     = 47;  //00BC  N  012346  TRAP#15命令ベクタ(IOCSコール)
  public static final int M6E_FP_BRANCH_SET_UNORDERED        = 48;  //00C0  F  --CC46  浮動小数点比較不能状態での分岐またはセット
  public static final int M6E_FP_INEXACT_RESULT              = 49;  //00C4  N  --CC46  浮動小数点不正確な結果
  //                                                                     30C4  N
  public static final int M6E_FP_DIVIDE_BY_ZERO              = 50;  //00C8  N  --CC46  浮動小数点ゼロによる除算
  public static final int M6E_FP_UNDERFLOW                   = 51;  //00CC  N  --CC46  浮動小数点アンダーフロー
  //                                                                     30CC  N
  public static final int M6E_FP_OPERAND_ERROR               = 52;  //00D0  N  --CC46  浮動小数点オペランドエラー
  //                                                                     30D0  N
  public static final int M6E_FP_OVERFLOW                    = 53;  //00D4  N  --CC46  浮動小数点オーバーフロー
  //                                                                     30D4  N
  public static final int M6E_FP_SIGNALING_NAN               = 54;  //00D8  N  --CC46  浮動小数点シグナリングNAN
  //                                                                     30D8  N
  public static final int M6E_FP_UNSUPPORTED_DATA_TYPE       = 55;  //00DC  N  ----46  浮動小数点未実装データ型(pre- 非正規化数)
  //                                                                     20DC  N  ----46  浮動小数点未実装データ型(パックトデシマル)
  //                                                                     30DC  N  ----46  浮動小数点未実装データ型(post- 非正規化数)
  public static final int M6E_MMU_CONFIGULATION_ERROR        = 56;  // 0E0  N  --M3--  MMUコンフィギュレーションエラー
  public static final int M6E_MMU_ILLEGAL_OPERATION_ERROR    = 57;  // 0E4     --M---  MMU不当操作エラー
  public static final int M6E_MMU_ACCESS_LEVEL_VIOLATION     = 58;  // 0E8     --M---  MMUアクセスレベル違反
  //  未実装実効アドレス(MC68060)
  //    ベクタ番号60、フォーマット0。PCは例外を発生させた命令
  //      Fop.X #<data>,FPn
  //      Fop.P #<data>,FPn
  //      FMOVEM.L #<data>,#<data>,FPSR/FPIAR
  //      FMOVEM.L #<data>,#<data>,FPCR/FPIAR
  //      FMOVEM.L #<data>,#<data>,FPCR/FPSR
  //      FMOVEM.L #<data>,#<data>,#<data>,FPCR/FPSR/FPIAR
  //      FMOVEM.X <ea>,Dn
  //      FMOVEM.X Dn,<ea>
  public static final int M6E_UNIMPLEMENTED_EFFECTIVE        = 60;  //00F0  F  -----6  未実装実効アドレス
  //  未実装整数命令(MC68060)
  //    ベクタ番号61、フォーマット0。PCは例外を発生させた命令
  //      MULU.L <ea>,Dh:Dl
  //      MULS.L <ea>,Dh:Dl
  //      DIVU.L <ea>,Dr:Dq
  //      DIVS.L <ea>,Dr:Dq
  //      CAS2.W Dc1:Dc2,Du1:Du2,(Rn1):(Rn2)
  //      CAS2.L Dc1:Dc2,Du1:Du2,(Rn1):(Rn2)
  //      CHK2.B <ea>,Rn
  //      CHK2.W <ea>,Rn
  //      CHK2.L <ea>,Rn
  //      CMP2.B <ea>,Rn
  //      CMP2.W <ea>,Rn
  //      CMP2.L <ea>,Rn
  //      CAS.W Dc,Du,<ea> (misaligned <ea>)
  //      CAS.L Dc,Du,<ea> (misaligned <ea>)
  //      MOVEP.W (d16,Ar),Dq
  //      MOVEP.L (d16,Ar),Dq
  //      MOVEP.W Dq,(d16,Ar)
  //      MOVEP.L Dq,(d16,Ar)
  public static final int M6E_UNIMPLEMENTED_INSTRUCTION      = 61;  //00F4  F  -----6  未実装整数命令

  public static final String[] M6E_ERROR_NAME = {
    "undefined exception #0",  //0
    "undefined exception #1",  //1
    "Access fault",  //2
    "Address error",  //3
    "Illegal instruction",  //4
    "Divide by zero",  //5
    "CHK instruction",  //6
    "Trapv instruction",  //7
    "Privilege violation",  //8
    "Trace",  //9
    "Line 1010 emulator",  //10
    "Line 1111 emulator",  //11
    "Emulator interrupt",  //12
    "Coprocessor protocol violation",  //13
    "Format error",  //14
    "Uninitialized interrupt",  //15
    "undefined exception #16",  //16
    "undefined exception #17",  //17
    "undefined exception #18",  //18
    "undefined exception #19",  //19
    "undefined exception #20",  //20
    "undefined exception #21",  //21
    "undefined exception #22",  //22
    "undefined exception #23",  //23
    "Spurious interrupt",  //24
    "Level 1 interrupt autovector",  //25
    "Level 2 interrupt autovector",  //26
    "Level 3 interrupt autovector",  //27
    "Level 4 interrupt autovector",  //28
    "Level 5 interrupt autovector",  //29
    "Level 6 interrupt autovector",  //30
    "Level 7 interrupt autovector",  //31
    "TRAP #0 instruction vector",  //32
    "TRAP #1 instruction vector",  //33
    "TRAP #2 instruction vector",  //34
    "TRAP #3 instruction vector",  //35
    "TRAP #4 instruction vector",  //36
    "TRAP #5 instruction vector",  //37
    "TRAP #6 instruction vector",  //38
    "TRAP #7 instruction vector",  //39
    "TRAP #8 instruction vector",  //40
    "TRAP #9 instruction vector",  //41
    "TRAP #10 instruction vector",  //42
    "TRAP #11 instruction vector",  //43
    "TRAP #12 instruction vector",  //44
    "TRAP #13 instruction vector",  //45
    "TRAP #14 instruction vector",  //46
    "TRAP #15 instruction vector",  //47
    "FP branch/set on unordered ",  //48
    "FP inexact result",  //49
    "FP divide by zero",  //50
    "FP underflow",  //51
    "FP operand error",  //52
    "FP overflow",  //53
    "FP signaling NAN",  //54
    "FP unsupported data type",  //55
    "MMU configulation error",  //56
    "MMU illegal operation error",  //57
    "MMU access level violation",  //58
    "undefined exception #59",  //59
    "Unimplemented effective address",  //60
    "Unimplemented instruction",  //61
    "undefined exception #62",  //62
    "undefined exception #63",  //63
  };

  public static M68kException m6eSignal;  //throwする唯一のインスタンス
  public static int m6eNumber;  //ベクタ番号。0～255
  public static int m6eAddress;  //アクセスアドレス
  public static int m6eDirection;  //転送方向。0=WRITE,1=READ
  public static int m6eSize;  //オペレーションサイズ。0=BYTE,1=WORD,2=LONG

}  //class M68kException



