//========================================================================================
//  Disassembler.java
//    en:Disassembler
//    ja:逆アセンブラ
//  Copyright (C) 2003-2025 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System

public class Disassembler {

  public static final String[] DIS_SZ = { ".b", ".w", ".l" };

  public static final String[] DIS_CC = {
    "t", "f", "hi", "ls", "cc", "cs", "ne", "eq", "vc", "vs", "pl", "mi", "ge", "lt", "gt", "le",
  };

  public static final String[] DIS_RCNAME_0XX = {
    "sfc",       //0x000 source function code
    "dfc",       //0x001 destination function code
    "cacr",      //0x002 cache control register
    "tc",        //0x003 mmu translation control register
    //"asid",    //0x003 address space identifier register (ColdFire)
    "itt0",      //0x004 instruction transparent translation register 0
    //"acr0",    //0x004 access control register 0
    "itt1",      //0x005 instruction transparent translation register 1
    //"acr1",    //0x005 access control register 1
    "dtt0",      //0x006 data transparent translation register 0
    //"acr2",    //0x006 access control register 2
    "dtt1",      //0x007 data transparent translation register 1
    //"acr3",    //0x007 access control register 3
    "buscr",     //0x008 bus control register
    //"mmubar",  //0x008 mmu base address register
  };
  public static final String[] DIS_RCNAME_8XX = {
    "usp",       //0x800 user stack pointer
    "vbr",       //0x801 vector base register
    "caar",      //0x802 cache address register
    "msp",       //0x803 master stackpointer
    "isp",       //0x804 interrupt stack pointer
    "mmusr",     //0x805 mmu status register
    "urp",       //0x806 user root pointer
    "srp",       //0x807 supervisor root pointer
    "pcr",       //0x808 processor configuration register
    //"pc",      //0x80f program counter
    //"rombar0",  //0xc00 rom base address register 0
    //"rombar1",  //0xc01 rom base address register 1
    //"rambar0",  //0xc04 ram base address register 0
    //"rambar1",  //0xc05 ram base address register 1
    //"mpcr",     //0xc0c multiprocessor control register
    //"edrambar",  //0xc0d embedded dram base address register
    //"secmbar",   //0xc0e secondary module base address register
    //"mbar",      //0xc0f primary module base address register
    //"pcr1u0",    //0xd02 32 msbs of ram 0 permutation control register 1
    //"pcr1l0",    //0xd03 32 lsbs of ram 0 permutation control register 1
    //"pcr2u0",    //0xd04 32 msbs of ram 0 permutation control register 2
    //"pcr2l0",    //0xd05 32 lsbs of ram 0 permutation control register 2
    //"pcr3u0",    //0xd06 32 msbs of ram 0 permutation control register 3
    //"pcr3l0",    //0xd07 32 lsbs of ram 0 permutation control register 3
    //"pcr1u1",    //0xd0a 32 msbs of ram 1 permutation control register 1
    //"pcr1l1",    //0xd0b 32 lsbs of ram 1 permutation control register 1
    //"pcr2u1",    //0xd0c 32 msbs of ram 1 permutation control register 2
    //"pcr2l1",    //0xd0d 32 lsbs of ram 1 permutation control register 2
    //"pcr3u1",    //0xd0e 32 msbs of ram 1 permutation control register 3
    //"pcr3l1",    //0xd0f 32 lsbs of ram 1 permutation control register 3
  };

  //IOCSコール名(0x00～0xff)
  public static final String[] DIS_IOCS_NAME = (
    "_B_KEYINP," +   //0x00 キー入力(入力があるまで待つ,入力したデータはバッファから取り除く)
    "_B_KEYSNS," +   //0x01 キーセンス(入力がなくても待たない,入力したデータをバッファから取り除かない)
    "_B_SFTSNS," +   //0x02 シフトキーとLEDの状態の取得
    "_KEY_INIT," +   //0x03 キーボードインタフェイスの初期化
    "_BITSNS," +     //0x04 キーの押し下げ状態の取得
    "_SKEYSET," +    //0x05 キー入力エミュレーション
    "_LEDCTRL," +    //0x06 キーボードのLEDの状態をまとめて設定
    "_LEDSET," +     //0x07 キーのLEDを再設定する
    "_KEYDLY," +     //0x08 キーリピートのディレイタイム設定
    "_KEYREP," +     //0x09 キーリピートのインターバル設定
    "_OPT2TVON," +   //0x0a OPT.2キーによるテレビコントロールを許可
    "_OPT2TVOFF," +  //0x0b OPT.2キーによるテレビコントロールを禁止
    "_TVCTRL," +     //0x0c テレビコントロール
    "_LEDMOD," +     //0x0d キーのLEDを設定
    "_TGUSEMD," +    //0x0e 画面の使用状態の取得と設定
    "_DEFCHR," +     //0x0f フォントパターン設定
    "_CRTMOD," +     //0x10 画面モードの取得と設定
    "_CONTRAST," +   //0x11 コントラストの取得と設定
    "_HSVTORGB," +   //0x12 HSVからRGBを求める
    "_TPALET," +     //0x13 テキストパレットの取得と設定
    "_TPALET2," +    //0x14 テキストパレットの取得と設定(全色独立)
    "_TCOLOR," +     //0x15 テキスト表示プレーンの設定
    "_FNTADR," +     //0x16 フォントアドレスの取得
    "_VRAMGET," +    //0x17 VRAMからバッファへバイト単位で転送
    "_VRAMPUT," +    //0x18 バッファからVRAMへバイト単位で転送
    "_FNTGET," +     //0x19 フォントパターンの取得
    "_TEXTGET," +    //0x1a テキストVRAMからバッファへドット単位で転送
    "_TEXTPUT," +    //0x1b バッファからテキストVRAMへドット単位で転送
    "_CLIPPUT," +    //0x1c バッファからテキストVRAMへドット単位で転送(クリッピングあり)
    "_SCROLL," +     //0x1d テキスト/グラフィックスのスクロール位置の取得と設定
    "_B_CURON," +    //0x1e テキストカーソルON
    "_B_CUROFF," +   //0x1f テキストカーソルOFF
    "_B_PUTC," +     //0x20 テキスト1文字表示
    "_B_PRINT," +    //0x21 テキスト文字列表示
    "_B_COLOR," +    //0x22 テキストカラーコード設定
    "_B_LOCATE," +   //0x23 テキストカーソル位置設定
    "_B_DOWN_S," +   //0x24 テキストカーソルを下へ1行移動(移動できないときスクロールする)
    "_B_UP_S," +     //0x25 テキストカーソルを上へ1行移動(移動できないときスクロールする)
    "_B_UP," +       //0x26 テキストカーソルを上へn行移動(移動できないときはエラー)
    "_B_DOWN," +     //0x27 テキストカーソルを下へn行移動(移動できないときは最下行で止まる)
    "_B_RIGHT," +    //0x28 テキストカーソルをn桁右へ移動(移動できないときは右端で止まる)
    "_B_LEFT," +     //0x29 テキストカーソルをn桁左へ移動(移動できないときは左端で止まる)
    "_B_CLR_ST," +   //0x2a テキスト画面クリア(クリアする範囲を選択)
    "_B_ERA_ST," +   //0x2b テキスト行クリア(クリアする範囲を選択)
    "_B_INS," +      //0x2c テキストカーソル行から下にn行空行を挿入
    "_B_DEL," +      //0x2d テキストカーソル行からn行削除
    "_B_CONSOL," +   //0x2e テキスト表示範囲を設定
    "_B_PUTMES," +   //0x2f テキスト画面の指定位置に文字列表示
    "_SET232C," +    //0x30 RS-232C通信モードと通信速度の取得と設定
    "_LOF232C," +    //0x31 RS-232C受信バッファ内のデータ数の取得
    "_INP232C," +    //0x32 RS-232C受信(受信があるまで待つ,受信バッファから取り除く)
    "_ISNS232C," +   //0x33 RS-232C受信センス(受信がなくても待たない,受信バッファから取り除かない)
    "_OSNS232C," +   //0x34 RS-232C送信ステータスチェック
    "_OUT232C," +    //0x35 RS-232C送信(送信可能になるまで待つ)
    "_MS_VCS," +     //0x36 マウス受信データ処理の設定
    "_EXESC," +      //0x37 拡張ESCシーケンス処理ルーチンの設定
    "_CHR_ADR," +    //0x38 外字フォントアドレスの設定
    "_SETBEEP," +    //0x39 BEEP処理の設定
    "_SETPRN," +     //0x3a プリンタ環境の設定
    "_JOYGET," +     //0x3b ジョイスティックの状態の取得
    "_INIT_PRN," +   //0x3c プリンタ初期化
    "_SNSPRN," +     //0x3d プリンタ出力センス
    "_OUTLPT," +     //0x3e プリンタ出力(LPT)
    "_OUTPRN," +     //0x3f プリンタ出力(PRN)
    "_B_SEEK," +     //0x40 シーク
    "_B_VERIFY," +   //0x41 ベリファイ
    "_B_READDI," +   //0x42 診断のための読み出し
    "_B_DSKINI," +   //0x43 FDインタフェイスの初期化
    "_B_DRVSNS," +   //0x44 ディスクのステータスを取得
    "_B_WRITE," +    //0x45 ディスクに書き出し
    "_B_READ," +     //0x46 ディスクから読み込み
    "_B_RECALI," +   //0x47 トラック0へのシーク
    "_B_ASSIGN," +   //0x48 代替トラックの設定
    "_B_WRITED," +   //0x49 破損データの書き込み
    "_B_READID," +   //0x4a ID情報を読む
    "_B_BADFMT," +   //0x4b バッドトラックを使用不能にする
    "_B_READDL," +   //0x4c 破損データの読み込み
    "_B_FORMAT," +   //0x4d 物理フォーマット
    "_B_DRVCHK," +   //0x4e ドライブの状態の取得と設定
    "_B_EJECT," +    //0x4f イジェクト(未使用シリンダへのシーク)
    "_DATEBCD," +    //0x50 日付を時計にセットできる形式に変換する
    "_DATESET," +    //0x51 時計に日付を設定する
    "_TIMEBCD," +    //0x52 時刻を時計にセットできる形式に変換する
    "_TIMESET," +    //0x53 時計に時刻を設定する
    "_DATEGET," +    //0x54 時計から日付を読み出す
    "_DATEBIN," +    //0x55 日付をBCDからバイナリに変換する
    "_TIMEGET," +    //0x56 時計から時刻を読み出す
    "_TIMEBIN," +    //0x57 時刻をBCDからバイナリに変換する
    "_DATECNV," +    //0x58 日付を表す文字列をバイナリに変換する
    "_TIMECNV," +    //0x59 時刻を表す文字列をバイナリに変換する
    "_DATEASC," +    //0x5a 日付をバイナリから文字列に変換する
    "_TIMEASC," +    //0x5b 時刻をバイナリから文字列に変換する
    "_DAYASC," +     //0x5c 曜日をバイナリから文字列に変換する
    "_ALARMMOD," +   //0x5d アラームの禁止/許可
    "_ALARMSET," +   //0x5e アラーム起動の時間と処理内容の設定
    "_ALARMGET," +   //0x5f アラーム起動の時間と処理内容の取得
    "_ADPCMOUT," +   //0x60 ADPCM再生
    "_ADPCMINP," +   //0x61 ADPCM録音
    "_ADPCMAOT," +   //0x62 アレイチェーンによるADPCM再生
    "_ADPCMAIN," +   //0x63 アレイチェーンによるADPCM録音
    "_ADPCMLOT," +   //0x64 リンクアレイチェーンによるADPCM再生
    "_ADPCMLIN," +   //0x65 リンクアレイチェーンによるADPCM録音
    "_ADPCMSNS," +   //0x66 ADPCMの実行モードセンス
    "_ADPCMMOD," +   //0x67 ADPCMの実行制御
    "_OPMSET," +     //0x68 FM音源レジスタの設定
    "_OPMSNS," +     //0x69 FM音源のステータス取得
    "_OPMINTST," +   //0x6a FM音源割り込み処理ルーチンの設定
    "_TIMERDST," +   //0x6b Timer-D割り込み処理ルーチンの設定
    "_VDISPST," +    //0x6c Timer-A(垂直同期カウント)割り込み処理ルーチンの設定
    "_CRTCRAS," +    //0x6d CRTCラスタ割り込み処理ルーチンの設定
    "_HSYNCST," +    //0x6e 水平同期割り込み処理ルーチンの設定
    "_PRNINTST," +   //0x6f プリンタのレディー割り込み処理ルーチンの設定
    "_MS_INIT," +    //0x70 マウス処理を初期化する
    "_MS_CURON," +   //0x71 マウスカーソルを表示する
    "_MS_CUROF," +   //0x72 マウスカーソルを消去する
    "_MS_STAT," +    //0x73 マウスカーソルの表示状態を取得する
    "_MS_GETDT," +   //0x74 マウスの状態を取得する
    "_MS_CURGT," +   //0x75 マウスカーソルの座標を取得する
    "_MS_CURST," +   //0x76 マウスカーソルの座標を設定する
    "_MS_LIMIT," +   //0x77 マウスカーソルの移動範囲を設定する
    "_MS_OFFTM," +   //0x78 マウスのボタンが離されるまでの時間を計る
    "_MS_ONTM," +    //0x79 マウスのボタンが押されるまでの時間を計る
    "_MS_PATST," +   //0x7a マウスカーソルパターンを定義する
    "_MS_SEL," +     //0x7b マウスカーソルを選ぶ
    "_MS_SEL2," +    //0x7c マウスカーソルアニメーションの設定
    "_SKEY_MOD," +   //0x7d ソフトキーボードの表示モードの取得と設定
    "_DENSNS," +     //0x7e 電卓センス
    "_ONTIME," +     //0x7f 起動後の経過時間(1/100秒単位)を求める
    "_B_INTVCS," +   //0x80 例外処理またはIOCSコールベクタ設定
    "_B_SUPER," +    //0x81 スーパーバイザモード切り替え
    "_B_BPEEK," +    //0x82 メモリ読み出し(1バイト)
    "_B_WPEEK," +    //0x83 メモリ読み出し(1ワード)
    "_B_LPEEK," +    //0x84 メモリ読み出し(1ロングワード)
    "_B_MEMSTR," +   //0x85 メモリ間転送(a1からa2へ)
    "_B_BPOKE," +    //0x86 メモリ書き込み(1バイト)
    "_B_WPOKE," +    //0x87 メモリ書き込み(1ワード)
    "_B_LPOKE," +    //0x88 メモリ書き込み(1ロングワード)
    "_B_MEMSET," +   //0x89 メモリ間転送(a2からa1へ)
    "_DMAMOVE," +    //0x8a DMA転送
    "_DMAMOV_A," +   //0x8b アレイチェーンによるDMA転送
    "_DMAMOV_L," +   //0x8c リンクアレイチェーンによるDMA転送
    "_DMAMODE," +    //0x8d DMA転送中モードの取得
    "_BOOTINF," +    //0x8e 起動情報の取得
    "_ROMVER," +     //0x8f ROMバージョンの取得
    "_G_CLR_ON," +   //0x90 グラフィックス画面の消去とパレット初期化と表示ON
    "_G_MOD," +      //0x91 グラフィックス画面モードの設定
    "_PRIORITY," +   //0x92 画面間およびグラフィックスページ間のプライオリティの設定
    "_CRTMOD2," +    //0x93 画面表示のON/OFFと特殊モードの設定
    "_GPALET," +     //0x94 グラフィックパレットの取得と設定
    "_PENCOLOR," +   //0x95 ペンカラーの設定
    "_SET_PAGE," +   //0x96 グラフィック描画ページの設定
    "_GGET," +       //0x97 グラフィックス画面からパターン読み出し
    "_MASK_GPUT," +  //0x98 グラフィックス画面にパターン書き込み(スルーカラー指定)
    "_GPUT," +       //0x99 グラフィックス画面にパターン書き込み
    "_GPTRN," +      //0x9a グラフィックス画面にビットパターン書き込み
    "_BK_GPTRN," +   //0x9b グラフィックス画面にビットパターン書き込み(バックカラー指定)
    "_X_GPTRN," +    //0x9c グラフィックス画面にビットパターン書き込み(拡大指定)
    "," +            //0x9d
    "," +            //0x9e
    "," +            //0x9f
    "_SFTJIS," +     //0xa0 SJIS→JIS変換
    "_JISSFT," +     //0xa1 JIS→SJIS変換
    "_AKCONV," +     //0xa2 半角(ANK)→全角(SJIS)変換
    "_RMACNV," +     //0xa3 ローマ字かな変換
    "_DAKJOB," +     //0xa4 濁点処理(直前の文字に゛を付ける)
    "_HANJOB," +     //0xa5 半濁点処理(直前の文字に゜を付ける)
    "," +            //0xa6
    "," +            //0xa7
    "," +            //0xa8
    "," +            //0xa9
    "," +            //0xaa
    "," +            //0xab
    "_SYS_STAT," +   //0xac システム環境の取得と設定
    "_B_CONMOD," +   //0xad テキスト画面のカーソルとスクロールの設定
    "_OS_CURON," +   //0xae カーソル表示
    "_OS_CUROF," +   //0xaf カーソル非表示(_B_CURONによる表示も禁止)
    "_DRAWMODE," +   //0xb0 グラフィックス画面の描画モードの取得と設定
    "_APAGE," +      //0xb1 グラフィックス画面の描画ページの取得と設定
    "_VPAGE," +      //0xb2 グラフィックス画面の表示ページの設定
    "_HOME," +       //0xb3 グラフィックス画面のスクロール位置の設定
    "_WINDOW," +     //0xb4 グラフィックス画面のクリッピングエリアを設定する
    "_WIPE," +       //0xb5 グラフィックス画面をパレットコード0で塗り潰す
    "_PSET," +       //0xb6 グラフィックス画面に点を描く
    "_POINT," +      //0xb7 グラフィックス画面の1点のパレットコードを得る
    "_LINE," +       //0xb8 グラフィックス画面に線分を描く
    "_BOX," +        //0xb9 グラフィックス画面に矩形を描く
    "_FILL," +       //0xba グラフィックス画面の矩形塗り潰し
    "_CIRCLE," +     //0xbb グラフィックス画面に円または楕円を描く
    "_PAINT," +      //0xbc グラフィックス画面の閉領域の塗り潰し
    "_SYMBOL," +     //0xbd グラフィックス画面に文字列表示
    "_GETGRM," +     //0xbe グラフィックス画面の読み出し
    "_PUTGRM," +     //0xbf グラフィックス画面の書き込み
    "_SP_INIT," +    //0xc0 スプライトとBGの初期化
    "_SP_ON," +      //0xc1 スプライト表示ON
    "_SP_OFF," +     //0xc2 スプライト表示OFF
    "_SP_CGCLR," +   //0xc3 スプライトパターンのクリア(16×16)
    "_SP_DEFCG," +   //0xc4 スプライトパターンの設定
    "_SP_GTPCG," +   //0xc5 スプライトパターンの取得
    "_SP_REGST," +   //0xc6 スプライトレジスタの設定
    "_SP_REGGT," +   //0xc7 スプライトレジスタの取得
    "_BGSCRLST," +   //0xc8 BGスクロールレジスタの設定
    "_BGSCRLGT," +   //0xc9 BGスクロールレジスタの取得
    "_BGCTRLST," +   //0xca BGコントロールレジスタの設定
    "_BGCTRLGT," +   //0xcb BGコントロールレジスタの取得
    "_BGTEXTCL," +   //0xcc BGテキストのクリア
    "_BGTEXTST," +   //0xcd BGテキストの設定
    "_BGTEXTGT," +   //0xce BGテキストの取得
    "_SPALET," +     //0xcf スプライトパレットの取得と設定
    "," +            //0xd0
    "," +            //0xd1
    "," +            //0xd2
    "_TXXLINE," +    //0xd3 テキスト画面に水平線を描画
    "_TXYLINE," +    //0xd4 テキスト画面に垂直線を描画
    "_TXLINE," +     //0xd5 テキスト画面に直線を描画
    "_TXBOX," +      //0xd6 テキスト画面に矩形の枠を描画
    "_TXFILL," +     //0xd7 テキスト画面に矩形を描画
    "_TXREV," +      //0xd8 テキスト画面の矩形を反転
    "," +            //0xd9
    "," +            //0xda
    "," +            //0xdb
    "," +            //0xdc
    "," +            //0xdd
    "," +            //0xde
    "_TXRASCPY," +   //0xdf テキストラスタブロックコピー
    "," +            //0xe0
    "," +            //0xe1
    "," +            //0xe2
    "," +            //0xe3
    "," +            //0xe4
    "," +            //0xe5
    "," +            //0xe6
    "," +            //0xe7
    "," +            //0xe8
    "," +            //0xe9
    "," +            //0xea
    "," +            //0xeb
    "," +            //0xec
    "," +            //0xed
    "," +            //0xee
    "," +            //0xef
    "_OPMDRV," +     //0xf0
    "_RSDRV," +      //0xf1
    "_A_JOY," +      //0xf2
    "_MIDI," +       //0xf3
    "," +            //0xf4
    "_SCSIDRV," +    //0xf5
    "," +            //0xf6
    "," +            //0xf7
    "_HIMEM," +      //0xf8
    "," +            //0xf9
    "," +            //0xfa
    "," +            //0xfb
    "," +            //0xfc
    "_ABORTRST," +   //0xfd
    "_IPLERR," +     //0xfe
    "_ABORTJOB"      //0xff
    ).split (",", 256);

  //DOSコール名(0xff00～0xffff)
  public static final String[] DIS_DOS_NAME = (
    "_EXIT," +        //0xff00 プロセスの終了(終了コード指定なし)
    "_GETCHAR," +     //0xff01 標準入力から1バイト入力(標準出力にエコーバックする)
    "_PUTCHAR," +     //0xff02 標準出力に1バイト出力
    "_COMINP," +      //0xff03 標準シリアル入出力から1バイト入力
    "_COMOUT," +      //0xff04 標準シリアル入出力に1バイト出力
    "_PRNOUT," +      //0xff05 標準プリンタ出力に1バイト出力
    "_INPOUT," +      //0xff06 標準ハンドラへの入出力
    "_INKEY," +       //0xff07 標準入力から1バイト入力(^C,^P,^Nを処理しない)
    "_GETC," +        //0xff08 標準入力から1バイト入力(^C,^P,^Nを処理する)
    "_PRINT," +       //0xff09 標準出力に文字列を出力
    "_GETS," +        //0xff0a 標準入力から文字列を入力(^C,^P,^Nを処理する)
    "_KEYSNS," +      //0xff0b 標準入力から1バイト先読み
    "_KFLUSH," +      //0xff0c 標準入力バッファをフラッシュしてから標準入力から入力
    "_FFLUSH," +      //0xff0d バッファフラッシュ
    "_CHGDRV," +      //0xff0e カレントドライブの変更
    "_DRVCTRL," +     //0xff0f ドライブコントロール
    "_CONSNS," +      //0xff10 標準出力への出力の可・不可を調べる
    "_PRNSNS," +      //0xff11 標準プリンタ出力への出力の可・不可を調べる
    "_CINSNS," +      //0xff12 標準シリアル入出力からの入力の可・不可を調べる
    "_COUTSNS," +     //0xff13 標準シリアル入出力への出力の可・不可を調べる
    "," +             //0xff14
    "," +             //0xff15
    "," +             //0xff16
    "_FATCHK," +      //0xff17 ファイルやディレクトリのFATの繋がりを調べる
    "_HENDSP," +      //0xff18 かな漢字変換ウインドウの表示
    "_CURDRV," +      //0xff19 カレントドライブ番号を得る
    "_GETSS," +       //0xff1a 標準入力から文字列を入力(^C,^P,^Nを処理しない)
    "_FGETC," +       //0xff1b ハンドラから1バイト入力
    "_FGETS," +       //0xff1c ハンドラから文字列を入力
    "_FPUTC," +       //0xff1d ハンドラへ1バイト出力
    "_FPUTS," +       //0xff1e ハンドラへ文字列を出力
    "_ALLCLOSE," +    //0xff1f 実行中のプロセスとその子プロセスがオープンしたハンドラをすべてクローズする
    "_SUPER," +       //0xff20 スーパーバイザモードの切り替え
    "_FNCKEY," +      //0xff21 再定義可能キーの読み込みと設定
    "_KNJCTRL," +     //0xff22 かな漢字変換の制御
    "_CONCTRL," +     //0xff23 コンソール出力の制御
    "_KEYCTRL," +     //0xff24 コンソール入力の制御
    "_INTVCS," +      //0xff25 例外処理ベクタの設定
    "_PSPSET," +      //0xff26 プロセス管理テーブルの作成
    "_GETTIM2," +     //0xff27 時刻を得る(ロングワード)
    "_SETTIM2," +     //0xff28 時刻を設定する(ロングワード)
    "_NAMESTS," +     //0xff29 ファイル名の分解
    "_GETDATE," +     //0xff2a 日付を得る
    "_SETDATE," +     //0xff2b 日付を設定する
    "_GETTIME," +     //0xff2c 時刻を得る(ワード)
    "_SETTIME," +     //0xff2d 時刻を設定する(ワード)
    "_VERIFY," +      //0xff2e verifyのモードの設定
    "_DUP0," +        //0xff2f 標準ハンドラの変換
    "_VERNUM," +      //0xff30 Humanのバージョンの取得
    "_KEEPPR," +      //0xff31 プロセスの常駐終了
    "_GETDPB," +      //0xff32 DPBの取得
    "_BREAKCK," +     //0xff33 breakおよびoffの取得と設定
    "_DRVXCHG," +     //0xff34 ドライブの入れ換え
    "_INTVCG," +      //0xff35 例外処理ベクタの取得
    "_DSKFRE," +      //0xff36 ドライブの空容量の取得
    "_NAMECK," +      //0xff37 ファイル名のチェック
    "," +             //0xff38
    "_MKDIR," +       //0xff39 ディレクトリの作成
    "_RMDIR," +       //0xff3a ディレクトリの削除
    "_CHDIR," +       //0xff3b カレントディレクトリの設定
    "_CREATE," +      //0xff3c 新規ファイルの作成
    "_OPEN," +        //0xff3d ファイルのオープン
    "_CLOSE," +       //0xff3e ハンドラのクローズ
    "_READ," +        //0xff3f ハンドラから指定されたサイズのデータを読み込む
    "_WRITE," +       //0xff40 ハンドラへ指定されたサイズのデータを書き込む
    "_DELETE," +      //0xff41 ファイルの削除
    "_SEEK," +        //0xff42 ハンドラのシーク位置の変更
    "_CHMOD," +       //0xff43 ファイルまたはディレクトリの属性の読み込みと設定
    "_IOCTRL," +      //0xff44 デバイスによるハンドラの直接制御
    "_DUP," +         //0xff45 ハンドラの複製
    "_DUP2," +        //0xff46 ハンドラの複写
    "_CURDIR," +      //0xff47 カレントディレクトリの取得
    "_MALLOC," +      //0xff48 メモリブロックの確保(下位から)
    "_MFREE," +       //0xff49 メモリブロックの開放
    "_SETBLOCK," +    //0xff4a メモリブロックのサイズの変更
    "_EXEC," +        //0xff4b 子プロセスの実行
    "_EXIT2," +       //0xff4c プロセスの終了(終了コード指定あり)
    "_WAIT," +        //0xff4d 子プロセスの終了コードの取得
    "_FILES," +       //0xff4e ディレクトリエントリの検索(最初)
    "_NFILES," +      //0xff4f ディレクトリエントリの検索(次)
    "_SETPDB," +      //0xff50 プロセス管理テーブルの移動
    "_GETPDB," +      //0xff51 プロセス管理テーブルの取得
    "_SETENV," +      //0xff52 環境変数の設定
    "_GETENV," +      //0xff53 環境変数の取得
    "_VERIFYG," +     //0xff54 verifyのモードの取得
    "_COMMON," +      //0xff55 common領域の制御
    "_RENAME," +      //0xff56 ファイル名またはディレクトリ名の変更およびファイルの移動
    "_FILEDATE," +    //0xff57 ハンドラの更新日時の取得と設定
    "_MALLOC2," +     //0xff58 メモリブロックの確保(モード指定あり)
    "," +             //0xff59
    "_MAKETMP," +     //0xff5a テンポラリファイルの作成
    "_NEWFILE," +     //0xff5b 新規ファイルの作成(非破壊)
    "_LOCK," +        //0xff5c ハンドラのロックの制御
    "," +             //0xff5d
    "," +             //0xff5e
    "_ASSIGN," +      //0xff5f 仮想ドライブおよび仮想ディレクトリの取得と設定
    "," +             //0xff60
    "," +             //0xff61
    "," +             //0xff62
    "," +             //0xff63
    "," +             //0xff64
    "," +             //0xff65
    "," +             //0xff66
    "," +             //0xff67
    "," +             //0xff68
    "," +             //0xff69
    "," +             //0xff6a
    "," +             //0xff6b
    "," +             //0xff6c
    "," +             //0xff6d
    "," +             //0xff6e
    "," +             //0xff6f
    "," +             //0xff70
    "," +             //0xff71
    "," +             //0xff72
    "," +             //0xff73
    "," +             //0xff74
    "," +             //0xff75
    "," +             //0xff76
    "," +             //0xff77
    "," +             //0xff78
    "," +             //0xff79
    "_FFLUSH_SET," +  //0xff7a fflushのモードの取得と設定
    "_OS_PATCH," +    //0xff7b Humanの変更
    "_GET_FCB_ADR," + //0xff7c FCBテーブルの取得
    "_S_MALLOC," +    //0xff7d メインスレッドのメモリ管理からメモリブロックを確保
    "_S_MFREE," +     //0xff7e メインスレッドのメモリ管理からメモリブロックを削除
    "_S_PROCESS," +   //0xff7f サブのメモリ管理の設定
    "_SETPDB," +      //0xff80 プロセス管理テーブルの移動
    "_GETPDB," +      //0xff81 プロセス管理テーブルの取得
    "_SETENV," +      //0xff82 環境変数の設定
    "_GETENV," +      //0xff83 環境変数の取得
    "_VERIFYG," +     //0xff84 verifyのモードの取得
    "_COMMON," +      //0xff85 common領域の制御
    "_RENAME," +      //0xff86 ファイル名またはディレクトリ名の変更およびファイルの移動
    "_FILEDATE," +    //0xff87 ハンドラの更新日時の取得と設定
    "_MALLOC2," +     //0xff88 メモリブロックの確保(モード指定あり)
    "," +             //0xff89
    "_MAKETMP," +     //0xff8a テンポラリファイルの作成
    "_NEWFILE," +     //0xff8b 新規ファイルの作成(非破壊)
    "_LOCK," +        //0xff8c ハンドラのロックの制御
    "," +             //0xff8d
    "," +             //0xff8e
    "_ASSIGN," +      //0xff8f 仮想ドライブおよび仮想ディレクトリの取得と設定
    "," +             //0xff90
    "," +             //0xff91
    "," +             //0xff92
    "," +             //0xff93
    "," +             //0xff94
    "," +             //0xff95
    "," +             //0xff96
    "," +             //0xff97
    "," +             //0xff98
    "," +             //0xff99
    "," +             //0xff9a
    "," +             //0xff9b
    "," +             //0xff9c
    "," +             //0xff9d
    "," +             //0xff9e
    "," +             //0xff9f
    "," +             //0xffa0
    "," +             //0xffa1
    "," +             //0xffa2
    "," +             //0xffa3
    "," +             //0xffa4
    "," +             //0xffa5
    "," +             //0xffa6
    "," +             //0xffa7
    "," +             //0xffa8
    "," +             //0xffa9
    "_FFLUSH_SET," +  //0xffaa fflushのモードの取得と設定
    "_OS_PATCH," +    //0xffab Humanの変更
    "_GET_FCB_ADR," + //0xffac FCBテーブルの取得
    "_S_MALLOC," +    //0xffad メインスレッドのメモリ管理からメモリブロックを確保
    "_S_MFREE," +     //0xffae メインスレッドのメモリ管理からメモリブロックを削除
    "_S_PROCESS," +   //0xffaf サブのメモリ管理の設定
    "," +             //0xffb0
    "," +             //0xffb1
    "," +             //0xffb2
    "," +             //0xffb3
    "," +             //0xffb4
    "," +             //0xffb5
    "," +             //0xffb6
    "," +             //0xffb7
    "," +             //0xffb8
    "," +             //0xffb9
    "," +             //0xffba
    "," +             //0xffbb
    "," +             //0xffbc
    "," +             //0xffbd
    "," +             //0xffbe
    "," +             //0xffbf
    "," +             //0xffc0
    "," +             //0xffc1
    "," +             //0xffc2
    "," +             //0xffc3
    "," +             //0xffc4
    "," +             //0xffc5
    "," +             //0xffc6
    "," +             //0xffc7
    "," +             //0xffc8
    "," +             //0xffc9
    "," +             //0xffca
    "," +             //0xffcb
    "," +             //0xffcc
    "," +             //0xffcd
    "," +             //0xffce
    "," +             //0xffcf
    "," +             //0xffd0
    "," +             //0xffd1
    "," +             //0xffd2
    "," +             //0xffd3
    "," +             //0xffd4
    "," +             //0xffd5
    "," +             //0xffd6
    "," +             //0xffd7
    "," +             //0xffd8
    "," +             //0xffd9
    "," +             //0xffda
    "," +             //0xffdb
    "," +             //0xffdc
    "," +             //0xffdd
    "," +             //0xffde
    "," +             //0xffdf
    "," +             //0xffe0
    "," +             //0xffe1
    "," +             //0xffe2
    "," +             //0xffe3
    "," +             //0xffe4
    "," +             //0xffe5
    "," +             //0xffe6
    "," +             //0xffe7
    "," +             //0xffe8
    "," +             //0xffe9
    "," +             //0xffea
    "," +             //0xffeb
    "," +             //0xffec
    "," +             //0xffed
    "," +             //0xffee
    "," +             //0xffef
    "_EXITVC," +      //0xfff0 _EXITVC(プロセスが終了したときのジャンプ先のベクタ)
    "_CTRLVC," +      //0xfff1 _CTRLVC(^Cのときのジャンプ先のベクタ)
    "_ERRJVC," +      //0xfff2 _ERRJVC(システムエラーが発生したときのジャンプ先のベクタ)
    "_DISKRED," +     //0xfff3 ハンドラから直接読み込む
    "_DISKWRT," +     //0xfff4 ハンドラに直接書き込む
    "_INDOSFLG," +    //0xfff5
    "_SUPER_JSR," +   //0xfff6
    "_BUS_ERR," +     //0xfff7
    "_OPEN_PR," +     //0xfff8 _OPEN_PR(スレッドが生成されたとき呼ばれるベクタ)
    "_KILL_PR," +     //0xfff9 _KILL_PR(スレッドが消滅したとき呼ばれるベクタ)
    "_GET_PR," +      //0xfffa
    "_SUSPEND_PR," +  //0xfffb
    "_SLEEP_PR," +    //0xfffc
    "_SEND_PR," +     //0xfffd
    "_TIME_PR," +     //0xfffe
    "_CHANGE_PR"      //0xffff _CHANGE_PR(スレッドが切り替わったとき呼ばれるベクタ)
    ).split (",", 256);

  //FEファンクションコール名(0xfe00～0xfeff)
  public static final String[] DIS_FPACK_NAME = (
    "__LMUL," +       //0xfe00  d0.l*=d1.l  32bit符号あり整数乗算
    "__LDIV," +       //0xfe01  d0.l/=d1.l  32bit符号あり整数除算
    "__LMOD," +       //0xfe02  d0.l%=d1.l  32bit符号あり整数剰余算
    "," +             //0xfe03
    "__UMUL," +       //0xfe04  d0.l*=d1.l  32bit符号なし整数乗算
    "__UDIV," +       //0xfe05  d0.l/=d1.l  32bit符号なし整数除算
    "__UMOD," +       //0xfe06  d0.l%=d1.l  32bit符号なし整数剰余算
    "," +             //0xfe07
    "__IMUL," +       //0xfe08  d0d1.q=d0.l*d1.l  32bit符号なし整数乗算
    "__IDIV," +       //0xfe09  d0.l余りd1.l=d0.l/%d1.l  32bit符号なし整数除算・剰余算
    "," +             //0xfe0a
    "," +             //0xfe0b
    "__RANDOMIZE," +  //0xfe0c  0<=d0.l<=65535  rnd()乱数列の初期化
    "__SRAND," +      //0xfe0d  -32768<=d0.l<=32767  rand()乱数列の初期化
    "__RAND," +       //0xfe0e  d0.l=rand()  整数乱数
    "," +             //0xfe0f
    "__STOL," +       //0xfe10  d0.l=10進数(a0)  10進数の文字列を32bit符号あり整数に変換する
    "__LTOS," +       //0xfe11  10進数(a0)=d0.l  32bit符号あり整数を10進数の文字列に変換する
    "__STOH," +       //0xfe12  d0.l=16進数(a0)  16進数の文字列を32bit符号なし整数に変換する
    "__HTOS," +       //0xfe13  16進数(a0)=d0.l  32bit符号なし整数を16進数の文字列に変換する
    "__STOO," +       //0xfe14  d0.l=8進数(a0)  8進数の文字列を32bit符号なし整数に変換する
    "__OTOS," +       //0xfe15  8進数(a0)=d0.l  32bit符号なし整数を8進数の文字列に変換する
    "__STOB," +       //0xfe16  d0.=2進数(a0)l  2進数の文字列を32bit符号なし整数に変換する
    "__BTOS," +       //0xfe17  2進数(a0)=d0.l  32bit符号なし整数を2進数の文字列に変換する
    "__IUSING," +     //0xfe18  10進数(a0)桁数d1.b=d0.l  32bit符号あり整数を文字数を指定して右詰めで10進数の文字列に変換する
    "," +             //0xfe19
    "__LTOD," +       //0xfe1a  d0d1.d=d0.l  32bit符号あり整数を64bit浮動小数点数に変換する
    "__DTOL," +       //0xfe1b  d0.l=d0d1.d  64bit浮動小数点数を32bit符号あり整数に変換する
    "__LTOF," +       //0xfe1c  d0.s=d0.l  32bit符号あり整数を32bit浮動小数点数に変換する
    "__FTOL," +       //0xfe1d  d0.l=d0.s  32bit浮動小数点数を32bit符号あり整数に変換する
    "__FTOD," +       //0xfe1e  d0d1.d=d0.s  32bit浮動小数点数を64bit浮動小数点数に変換する
    "__DTOF," +       //0xfe1f  d0.s=d0d1.d  64bit浮動小数点数を32bit浮動小数点数に変換する
    "__VAL," +        //0xfe20  d0d1.d=10進数/&H:16進数/&O:8進数/&B:2進数(a0)  文字列を64bit浮動小数点数に変換する
    "__USING," +      //0xfe21  10進数(a0)整数桁数d2.l小数桁数d3.l属性d4.l=d0d1.d  64bit浮動小数点数をアトリビュートを指定して文字列に変換する
    "__STOD," +       //0xfe22  d0d1.d=10進数(a0)  文字列を64bit浮動小数点数に変換する
    "__DTOS," +       //0xfe23  10進数(a0)=d0d1.d  64bit浮動小数点数を文字列に変換する
    "__ECVT," +       //0xfe24  10進数数字のみ(a0)全体桁数d2.l=d0d1.d  64bit浮動小数点数を全体の桁数を指定して文字列に変換する
    "__FCVT," +       //0xfe25  10進数数字のみ(a0)小数桁数d2.l=d0d1.d  64bit浮動小数点数を小数点以下の桁数を指定して文字列に変換する
    "__GCVT," +       //0xfe26  10進数(a0)全体桁数d2.l=d0d1.d  64bit浮動小数点数を全体の桁数を指定して文字列に変換する
    "," +             //0xfe27
    "__DTST," +       //0xfe28  d0d1.d<=>0  64bit浮動小数点数と0の比較
    "__DCMP," +       //0xfe29  d0d1.d<=>d2d3.d  64bit浮動小数点数の比較
    "__DNEG," +       //0xfe2a  d0d1.d=-d0d1.d  64bit浮動小数点数の符号反転
    "__DADD," +       //0xfe2b  d0d1.d+=d2d3.d  64bit浮動小数点数の加算
    "__DSUB," +       //0xfe2c  d0d1.d-=d2d3.d  64bit浮動小数点数の減算
    "__DMUL," +       //0xfe2d  d0d1.d*=d2d3.d  64bit浮動小数点数の乗算
    "__DDIV," +       //0xfe2e  d0d1.d/=d2d3.d  64bit浮動小数点数の除算
    "__DMOD," +       //0xfe2f  d0d1.d%=d2d3.d  64bit浮動小数点数の剰余算
    "__DABS," +       //0xfe30  d0d1.d=abs(d0d1.d)  64bit浮動小数点数の絶対値
    "__DCEIL," +      //0xfe31  d0d1.d=ceil(d0d1.d)  64bit浮動小数点数の天井関数(引数を下回らない最小の整数)
    "__DFIX," +       //0xfe32  d0d1.d=trunc(d0d1.d)  64bit浮動小数点数の切り落とし関数(絶対値について引数を上回らない最大の整数)
    "__DFLOOR," +     //0xfe33  d0d1.d=floor(d0d1.d)  64bit浮動小数点数の床関数(引数を上回らない最大の整数)
    "__DFRAC," +      //0xfe34  d0d1.d=frac(d0d1.d)  64bit浮動小数点数の幹小数部
    "__DSGN," +       //0xfe35  d0d1.d=signum(d0d1.d)  64bit浮動小数点数の符号
    "__SIN," +        //0xfe36  d0d1.d=sin(d0d1.d)  64bit浮動小数点数の正弦
    "__COS," +        //0xfe37  d0d1.d=cos(d0d1.d)  64bit浮動小数点数の余弦
    "__TAN," +        //0xfe38  d0d1.d=tan(d0d1.d)  64bit浮動小数点数の正接
    "__ATAN," +       //0xfe39  d0d1.d=atan(d0d1.d)  64bit浮動小数点数の逆正接
    "__LOG," +        //0xfe3a  d0d1.d=log(d0d1.d)  64bit浮動小数点数の自然対数
    "__EXP," +        //0xfe3b  d0d1.d=exp(d0d1.d)  64bit浮動小数点数の指数関数
    "__SQR," +        //0xfe3c  d0d1.d=sqrt(d0d1.d)  64bit浮動小数点数の平方根
    "__PI," +         //0xfe3d  d0d1.d=pi  64bit浮動小数点数の円周率
    "__NPI," +        //0xfe3e  d0d1.d*=pi  64bit浮動小数点数の円周率倍
    "__POWER," +      //0xfe3f  d0d1.d=pow(d0d1.d,d2d3.d)  64bit浮動小数点数の累乗
    "__RND," +        //0xfe40  d0d1.d=rnd()  64bit浮動小数点数の乱数
    "__SINH," +       //0xfe41  d0d1.d=sinh(d0d1.d)  64bit浮動小数点数の双曲線正弦
    "__COSH," +       //0xfe42  d0d1.d=cosh(d0d1.d)  64bit浮動小数点数の双曲線余弦
    "__TANH," +       //0xfe43  d0d1.d=tanh(d0d1.d)  64bit浮動小数点数の双曲線正接
    "__ATANH," +      //0xfe44  d0d1.d=atanh(d0d1.d)  64bit浮動小数点数の逆双曲線正接
    "__ASIN," +       //0xfe45  d0d1.d=asin(d0d1.d)  64bit浮動小数点数の逆正弦
    "__ACOS," +       //0xfe46  d0d1.d=acos(d0d1.d)  64bit浮動小数点数の逆余弦
    "__LOG10," +      //0xfe47  d0d1.d=log10(d0d1.d)  64bit浮動小数点数の常用対数
    "__LOG2," +       //0xfe48  d0d1.d=log2(d0d1.d)  64bit浮動小数点数の二進対数
    "__DFREXP," +     //0xfe49  d0d1.d指数部d2.l=d0d1.d  64bit浮動小数点数の分解
    "__DLDEXP," +     //0xfe4a  d0d1.d=d0d1.d指数部d2.l  64bit浮動小数点数の合成
    "__DADDONE," +    //0xfe4b  d0d1.d++  64bit浮動小数点数に1を加える
    "__DSUBONE," +    //0xfe4c  d0d1.d--  64bit浮動小数点数から1を引く
    "__DDIVTWO," +    //0xfe4d  d0d1.d/=2  64bit浮動小数点数を2で割る
    "__DIEECNV," +    //0xfe4e  d0d1.d=d0d1.d  64bit浮動小数点数をIEEEフォーマットに変換する(FLOAT1.X以外は何もしない)
    "__IEEDCNV," +    //0xfe4f  d0d1.d=d0d1.d  64bit浮動小数点数をIEEEフォーマットから変換する(FLOAT1.X以外は何もしない)
    "__FVAL," +       //0xfe50  d0.s=10進数/&H:16進数/&O:8進数/&B:2進数(a0)  文字列を32bit浮動小数点数に変換する
    "__FUSING," +     //0xfe51  10進数(a0)整数桁数d2.l小数桁数d3.l属性d4.l=d0.s  32bit浮動小数点数をアトリビュートを指定して文字列に変換する
    "__STOF," +       //0xfe52  d0.s=10進数(a0)  文字列を32bit浮動小数点数に変換する
    "__FTOS," +       //0xfe53  10進数(a0)=d0.s  32bit浮動小数点数を文字列に変換する
    "__FECVT," +      //0xfe54  10進数数字のみ(a0)全体桁数d2.l=d0.s  32bit浮動小数点数を全体の桁数を指定して文字列に変換する
    "__FFCVT," +      //0xfe55  10進数数字のみ(a0)小数桁数d2.l=d0.s  32bit浮動小数点数を小数点以下の桁数を指定して文字列に変換する
    "__FGCVT," +      //0xfe56  10進数(a0)全体桁数d2.l=d0.s  32bit浮動小数点数を全体の桁数を指定して文字列に変換する
    "," +             //0xfe57
    "__FTST," +       //0xfe58  d0.s<=>0  32bit浮動小数点数と0の比較
    "__FCMP," +       //0xfe59  d0.s<=>d1.s  32bit浮動小数点数の比較
    "__FNEG," +       //0xfe5a  d0.s=-d0.s  32bit浮動小数点数の符号反転
    "__FADD," +       //0xfe5b  d0.s+=d1.s  32bit浮動小数点数の加算
    "__FSUB," +       //0xfe5c  d0.s-=d1.s  32bit浮動小数点数の減算
    "__FMUL," +       //0xfe5d  d0.s*=d1.s  32bit浮動小数点数の乗算
    "__FDIV," +       //0xfe5e  d0.s/=d1.s  32bit浮動小数点数の除算
    "__FMOD," +       //0xfe5f  d0.s%=d1.s  32bit浮動小数点数の剰余算
    "__FABS," +       //0xfe60  d0.s=abs(d0.s)  32bit浮動小数点数の絶対値
    "__FCEIL," +      //0xfe61  d0.s=ceil(d0.s)  32bit浮動小数点数の天井関数(引数を下回らない最小の整数)
    "__FFIX," +       //0xfe62  d0.s=trunc(d0.s)  32bit浮動小数点数の切り落とし関数(絶対値について引数を上回らない最大の整数)
    "__FFLOOR," +     //0xfe63  d0.s=floor(d0.s)  32bit浮動小数点数の床関数(引数を上回らない最大の整数)
    "__FFRAC," +      //0xfe64  d0.s=frac(d0.s)  32bit浮動小数点数の幹小数部
    "__FSGN," +       //0xfe65  d0.s=signum(d0.s)  32bit浮動小数点数の符号
    "__FSIN," +       //0xfe66  d0.s=sin(d0.s)  32bit浮動小数点数の正弦
    "__FCOS," +       //0xfe67  d0.s=cos(d0.s)  32bit浮動小数点数の余弦
    "__FTAN," +       //0xfe68  d0.s=tan(d0.s)  32bit浮動小数点数の正接
    "__FATAN," +      //0xfe69  d0.s=atan(d0.s)  32bit浮動小数点数の逆正接
    "__FLOG," +       //0xfe6a  d0.s=log(d0.s)  32bit浮動小数点数の自然対数
    "__FEXP," +       //0xfe6b  d0.s=exp(d0.s)  32bit浮動小数点数の指数関数
    "__FSQR," +       //0xfe6c  d0.s=sqrt(d0.s)  32bit浮動小数点数の平方根
    "__FPI," +        //0xfe6d  d0.s=pi  32bit浮動小数点数の円周率
    "__FNPI," +       //0xfe6e  d0.s*=pi  32bit浮動小数点数の円周率倍
    "__FPOWER," +     //0xfe6f  d0.s=pow(d0.s,d1.s)  32bit浮動小数点数の累乗
    "__FRND," +       //0xfe70  d0.s=rnd()  32bit浮動小数点数の乱数
    "__FSINH," +      //0xfe71  d0.s=sinh(d0.s)  32bit浮動小数点数の双曲線正弦
    "__FCOSH," +      //0xfe72  d0.s=cosh(d0.s)  32bit浮動小数点数の双曲線余弦
    "__FTANH," +      //0xfe73  d0.s=tanh(d0.s)  32bit浮動小数点数の双曲線正接
    "__FATANH," +     //0xfe74  d0.s=atanh(d0.s)  32bit浮動小数点数の逆双曲線正接
    "__FASIN," +      //0xfe75  d0.s=asin(d0.s)  32bit浮動小数点数の逆正弦
    "__FACOS," +      //0xfe76  d0.s=acos(d0.s)  32bit浮動小数点数の逆余弦
    "__FLOG10," +     //0xfe77  d0.s=log10(d0.s)  32bit浮動小数点数の常用対数
    "__FLOG2," +      //0xfe78  d0.s=log2(d0.s)  32bit浮動小数点数の二進対数
    "__FFREXP," +     //0xfe79  d0.s指数部d1.l=d0.s  32bit浮動小数点数の分解
    "__FLDEXP," +     //0xfe7a  d0.s=d0.s指数部d1.l  32bit浮動小数点数の合成
    "__FADDONE," +    //0xfe7b  d0.s++  32bit浮動小数点数に1を加える
    "__FSUBONE," +    //0xfe7c  d0.s--  32bit浮動小数点数から1を引く
    "__FDIVTWO," +    //0xfe7d  d0.s/=2  32bit浮動小数点数を2で割る
    "__FIEECNV," +    //0xfe7e  d0.s=d0.s  32bit浮動小数点数をIEEEフォーマットに変換する(FLOAT1.X以外は何もしない)
    "__IEEFCNV," +    //0xfe7f  d0.s=d0.s  32bit浮動小数点数をIEEEフォーマットから変換する(FLOAT1.X以外は何もしない)
    "," +             //0xfe80
    "," +             //0xfe81
    "," +             //0xfe82
    "," +             //0xfe83
    "," +             //0xfe84
    "," +             //0xfe85
    "," +             //0xfe86
    "," +             //0xfe87
    "," +             //0xfe88
    "," +             //0xfe89
    "," +             //0xfe8a
    "," +             //0xfe8b
    "," +             //0xfe8c
    "," +             //0xfe8d
    "," +             //0xfe8e
    "," +             //0xfe8f
    "," +             //0xfe90
    "," +             //0xfe91
    "," +             //0xfe92
    "," +             //0xfe93
    "," +             //0xfe94
    "," +             //0xfe95
    "," +             //0xfe96
    "," +             //0xfe97
    "," +             //0xfe98
    "," +             //0xfe99
    "," +             //0xfe9a
    "," +             //0xfe9b
    "," +             //0xfe9c
    "," +             //0xfe9d
    "," +             //0xfe9e
    "," +             //0xfe9f
    "," +             //0xfea0
    "," +             //0xfea1
    "," +             //0xfea2
    "," +             //0xfea3
    "," +             //0xfea4
    "," +             //0xfea5
    "," +             //0xfea6
    "," +             //0xfea7
    "," +             //0xfea8
    "," +             //0xfea9
    "," +             //0xfeaa
    "," +             //0xfeab
    "," +             //0xfeac
    "," +             //0xfead
    "," +             //0xfeae
    "," +             //0xfeaf
    "," +             //0xfeb0
    "," +             //0xfeb1
    "," +             //0xfeb2
    "," +             //0xfeb3
    "," +             //0xfeb4
    "," +             //0xfeb5
    "," +             //0xfeb6
    "," +             //0xfeb7
    "," +             //0xfeb8
    "," +             //0xfeb9
    "," +             //0xfeba
    "," +             //0xfebb
    "," +             //0xfebc
    "," +             //0xfebd
    "," +             //0xfebe
    "," +             //0xfebf
    "," +             //0xfec0
    "," +             //0xfec1
    "," +             //0xfec2
    "," +             //0xfec3
    "," +             //0xfec4
    "," +             //0xfec5
    "," +             //0xfec6
    "," +             //0xfec7
    "," +             //0xfec8
    "," +             //0xfec9
    "," +             //0xfeca
    "," +             //0xfecb
    "," +             //0xfecc
    "," +             //0xfecd
    "," +             //0xfece
    "," +             //0xfecf
    "," +             //0xfed0
    "," +             //0xfed1
    "," +             //0xfed2
    "," +             //0xfed3
    "," +             //0xfed4
    "," +             //0xfed5
    "," +             //0xfed6
    "," +             //0xfed7
    "," +             //0xfed8
    "," +             //0xfed9
    "," +             //0xfeda
    "," +             //0xfedb
    "," +             //0xfedc
    "," +             //0xfedd
    "," +             //0xfede
    "," +             //0xfedf
    "__CLMUL," +      //0xfee0  (sp).l*=4(sp).l  32bit符号あり整数乗算
    "__CLDIV," +      //0xfee1  (sp).l/=4(sp).l  32bit符号あり整数除算
    "__CLMOD," +      //0xfee2  (sp).l%=4(sp).l  32bit符号あり整数剰余算
    "__CUMUL," +      //0xfee3  (sp).l*=4(sp).l  32bit符号なし整数乗算
    "__CUDIV," +      //0xfee4  (sp).l/=4(sp).l  32bit符号なし整数除算
    "__CUMOD," +      //0xfee5  (sp).l%=4(sp).l  32bit符号なし整数剰余算
    "__CLTOD," +      //0xfee6  (sp).d=(sp).l  32bit符号あり整数を64bit浮動小数点数に変換する
    "__CDTOL," +      //0xfee7  (sp).l=(sp).d  64bit浮動小数点数を32bit符号あり整数に変換する
    "__CLTOF," +      //0xfee8  (sp).s=(sp).l  32bit符号あり整数を32bit浮動小数点数に変換する
    "__CFTOL," +      //0xfee9  (sp).l=(sp).s  32bit浮動小数点数を32bit符号あり整数に変換する
    "__CFTOD," +      //0xfeea  (sp).d=(sp).s  32bit浮動小数点数を64bit浮動小数点数に変換する
    "__CDTOF," +      //0xfeeb  (sp).s=(sp).d  64bit浮動小数点数を32bit浮動小数点数に変換する
    "__CDCMP," +      //0xfeec  (sp).d<=>8(sp).d  64bit浮動小数点数の比較
    "__CDADD," +      //0xfeed  (sp).d+=8(sp).d  64bit浮動小数点数の加算
    "__CDSUB," +      //0xfeee  (sp).d-=8(sp).d  64bit浮動小数点数の減算
    "__CDMUL," +      //0xfeef  (sp).d*=8(sp).d  64bit浮動小数点数の乗算
    "__CDDIV," +      //0xfef0  (sp).d/=8(sp).d  64bit浮動小数点数の除算
    "__CDMOD," +      //0xfef1  (sp).d%=8(sp).d  64bit浮動小数点数の剰余算
    "__CFCMP," +      //0xfef2  (sp).s<=>4(sp).s  32bit浮動小数点数の比較
    "__CFADD," +      //0xfef3  (sp).s+=4(sp).s  32bit浮動小数点数の加算
    "__CFSUB," +      //0xfef4  (sp).s-=4(sp).s  32bit浮動小数点数の減算
    "__CFMUL," +      //0xfef5  (sp).s*=4(sp).s  32bit浮動小数点数の乗算
    "__CFDIV," +      //0xfef6  (sp).s/=4(sp).s  32bit浮動小数点数の除算
    "__CFMOD," +      //0xfef7  (sp).s%=4(sp).s  32bit浮動小数点数の剰余算
    "__CDTST," +      //0xfef8  (sp).d<=>0  64bit浮動小数点数と0の比較
    "__CFTST," +      //0xfef9  (sp).s<=>0  32bit浮動小数点数と0の比較
    "__CDINC," +      //0xfefa  (sp).d++  64bit浮動小数点数に1を加える
    "__CFINC," +      //0xfefb  (sp).s++  32bit浮動小数点数に1を加える
    "__CDDEC," +      //0xfefc  (sp).d--  64bit浮動小数点数から1を引く
    "__CFDEC," +      //0xfefd  (sp).s--  32bit浮動小数点数から1を引く
    "__FEVARG," +     //0xfefe  d0.l='HS86'(FLOAT1)/'IEEE'(FLOAT2/3/4),d1.l='SOFT'(FLOAT1/2)/'FPCP'(FLOAT3)/'FP20'(FLOAT4)  バージョン確認
    "__FEVECS"        //0xfeff  ベクタ[番号d0.l]=アドレスa0.l  ベクタ設定
    ).split (",", 256);

  //SXコール名(0xa000～0xa7ff)
  public static final String[] DIS_SXCALL_NAME = (
    "__MMInitHeap," +         //0xa000
    "__MMGetCurrentHeap," +   //0xa001
    "__MMSetCurrentHeap," +   //0xa002
    "__MMNewHandle," +        //0xa003
    "__MMSetHandleSize," +    //0xa004
    "__MMDisposeHandle," +    //0xa005
    "__MMGetHandleSize," +    //0xa006
    "__MMHLock," +            //0xa007
    "__MMHUnlock," +          //0xa008
    "__MMNewPtr," +           //0xa009
    "__MMDisposePtr," +       //0xa00a
    "__MMGetPtrSize," +       //0xa00b
    "__MMSetPtrSize," +       //0xa00c
    "__MMCompactMem," +       //0xa00d
    "__MMHeapInit," +         //0xa00e
    "__MMBlockMstGet," +      //0xa00f
    "__MMMemCompact," +       //0xa010
    "__MMMemPurge," +         //0xa011
    "__MMMemMelt," +          //0xa012
    "__MMMemReserve," +       //0xa013
    "__MMMemSizeFree," +      //0xa014
    "__MMMemSizeComp," +      //0xa015
    "__MMMemSizePurge," +     //0xa016
    "__MMMemSizeMelt," +      //0xa017
    "__MMMemErrorGet," +      //0xa018
    "__MMMemErrorSet," +      //0xa019
    "__MMMemStrictGet," +     //0xa01a
    "__MMMemStrictSet," +     //0xa01b
    "__MMChGet," +            //0xa01c
    "__MMChSet," +            //0xa01d
    "__MMChPtrNew," +         //0xa01e
    "__MMChMstMore," +        //0xa01f
    "__MMChMstNew," +         //0xa020
    "__MMChHdlNew," +         //0xa021
    "__MMChCompact," +        //0xa022
    "__MMChPurge," +          //0xa023
    "__MMChMelt," +           //0xa024
    "__MMChReserve," +        //0xa025
    "__MMChFreeSize," +       //0xa026
    "__MMChGrowHeapGet," +    //0xa027
    "__MMChGrowHeapSet," +    //0xa028
    "__MMChPurgeGet," +       //0xa029
    "__MMChPurgeSet," +       //0xa02a
    "__MMChCompactGet," +     //0xa02b
    "__MMChCompactSet," +     //0xa02c
    "__MMPtrNew," +           //0xa02d
    "__MMPtrHeap," +          //0xa02e
    "__MMPtrDispose," +       //0xa02f
    "__MMPtrSizeGet," +       //0xa030
    "__MMPtrSizeSet," +       //0xa031
    "__MMPtrPropGet," +       //0xa032
    "__MMPtrPropSet," +       //0xa033
    "__MMMstAllocate," +      //0xa034
    "__MMMstBind," +          //0xa035
    "__MMHdlNew," +           //0xa036
    "__MMHdlHeap," +          //0xa037
    "__MMHdlDispose," +       //0xa038
    "__MMHdlSizeGet," +       //0xa039
    "__MMHdlSizeSet," +       //0xa03a
    "__MMHdlEmpty," +         //0xa03b
    "__MMHdlRealloc," +       //0xa03c
    "__MMHdlMoveHi," +        //0xa03d
    "__MMHdlPropGet," +       //0xa03e
    "__MMHdlPropSet," +       //0xa03f
    "__MMHdlLock," +          //0xa040
    "__MMHdlUnlock," +        //0xa041
    "__MMHdlPurge," +         //0xa042
    "__MMHdlNoPurge," +       //0xa043
    "__MMHdlResource," +      //0xa044
    "__MMHdlNoResource," +    //0xa045
    "__MMHdlIns," +           //0xa046
    "__MMHdlDel," +           //0xa047
    "__MMBlockUsrFlagGet," +  //0xa048
    "__MMBlockUsrFlagSet," +  //0xa049
    "__MMBlockUsrWordGet," +  //0xa04a
    "__MMBlockUsrWordSet," +  //0xa04b
    "__MMMemAmiTPeach," +     //0xa04c
    "__MMMemHiReserve," +     //0xa04d
    "__MMPtrBlock," +         //0xa04e
    "__MMHdlBlock," +         //0xa04f
    "__MMHdlMstGet," +        //0xa050
    "__MMChHiReserve," +      //0xa051
    "__MMChUsrFlagGet," +     //0xa052
    "__MMChUsrFlagSet," +     //0xa053
    "__MMChUsrWordGet," +     //0xa054
    "__MMChUsrWordSet," +     //0xa055
    "," +                     //0xa056
    "," +                     //0xa057
    "," +                     //0xa058
    "," +                     //0xa059
    "," +                     //0xa05a
    "," +                     //0xa05b
    "," +                     //0xa05c
    "," +                     //0xa05d
    "," +                     //0xa05e
    "," +                     //0xa05f
    "," +                     //0xa060
    "," +                     //0xa061
    "," +                     //0xa062
    "," +                     //0xa063
    "," +                     //0xa064
    "," +                     //0xa065
    "," +                     //0xa066
    "," +                     //0xa067
    "__EXEnVDISPST," +        //0xa068
    "__EXDeVDISPST," +        //0xa069
    "__MSInitCsr," +          //0xa06a
    "__MSShowCsr," +          //0xa06b
    "__MSHideCsr," +          //0xa06c
    "__MSSetCsr," +           //0xa06d
    "__MSObscureCsr," +       //0xa06e
    "__MSShieldCsr," +        //0xa06f
    "__MSGetCurMsr," +        //0xa070
    "__MSMultiGet," +         //0xa071
    "__MSMultiSet," +         //0xa072
    "__EXAnimStart," +        //0xa073
    "__EXAnimEnd," +          //0xa074
    "__EXAnimTest," +         //0xa075
    "," +                     //0xa076
    "," +                     //0xa077
    "," +                     //0xa078
    "," +                     //0xa079
    "," +                     //0xa07a
    "," +                     //0xa07b
    "," +                     //0xa07c
    "," +                     //0xa07d
    "," +                     //0xa07e
    "," +                     //0xa07f
    "," +                     //0xa080
    "," +                     //0xa081
    "," +                     //0xa082
    "," +                     //0xa083
    "," +                     //0xa084
    "," +                     //0xa085
    "__KBMapGet," +           //0xa086
    "__KBShiftGet," +         //0xa087
    "__KBShiftSet," +         //0xa088
    "__KBSimulate," +         //0xa089
    "__KBScan," +             //0xa08a
    "__KBGet," +              //0xa08b
    "__KBEmpty," +            //0xa08c
    "__KBInit," +             //0xa08d
    "__KBTini," +             //0xa08e
    "__KBCurKbrGet," +        //0xa08f
    "__KBOldOnGet," +         //0xa090
    "__KBOldOnSet," +         //0xa091
    "__KBFlagGet," +          //0xa092
    "__KBFlagSet," +          //0xa093
    "," +                     //0xa094
    "," +                     //0xa095
    "," +                     //0xa096
    "," +                     //0xa097
    "," +                     //0xa098
    "," +                     //0xa099
    "__KMEmpty," +            //0xa09a
    "__KMPost," +             //0xa09b
    "__KMAscJobSet," +        //0xa09c
    "__KMSimulate," +         //0xa09d
    "__KMTask," +             //0xa09e
    "__KMInit," +             //0xa09f
    "__KMTini," +             //0xa0a0
    "__KMCurKmrGet," +        //0xa0a1
    "__EMInit," +             //0xa0a2
    "__EMTini," +             //0xa0a3
    "__EMSet," +              //0xa0a4
    "__EMGet," +              //0xa0a5
    "__EMScan," +             //0xa0a6
    "__EMMSLoc," +            //0xa0a7
    "__EMLBttn," +            //0xa0a8
    "__EMRBttn," +            //0xa0a9
    "__EMLStill," +           //0xa0aa
    "__EMRStill," +           //0xa0ab
    "__EMLWait," +            //0xa0ac
    "__EMRWait," +            //0xa0ad
    "__EMKMapGet," +          //0xa0ae
    "__EMSysTime," +          //0xa0af
    "__EMDClickGet," +        //0xa0b0
    "__EMBlinkGet," +         //0xa0b1
    "__EMClean," +            //0xa0b2
    "__EMMaskSet," +          //0xa0b3
    "__EMDTTskSet," +         //0xa0b4
    "__EMDClickSet," +        //0xa0b5
    "__EMBlinkSet," +         //0xa0b6
    "__EMEnCross," +          //0xa0b7
    "__EMDeCross," +          //0xa0b8
    "," +                     //0xa0b9
    "," +                     //0xa0ba
    "," +                     //0xa0bb
    "," +                     //0xa0bc
    "," +                     //0xa0bd
    "," +                     //0xa0be
    "," +                     //0xa0bf
    "," +                     //0xa0c0
    "," +                     //0xa0c1
    "," +                     //0xa0c2
    "," +                     //0xa0c3
    "," +                     //0xa0c4
    "," +                     //0xa0c5
    "," +                     //0xa0c6
    "," +                     //0xa0c7
    "," +                     //0xa0c8
    "," +                     //0xa0c9
    "," +                     //0xa0ca
    "," +                     //0xa0cb
    "," +                     //0xa0cc
    "," +                     //0xa0cd
    "," +                     //0xa0ce
    "," +                     //0xa0cf
    "," +                     //0xa0d0
    "," +                     //0xa0d1
    "," +                     //0xa0d2
    "," +                     //0xa0d3
    "," +                     //0xa0d4
    "," +                     //0xa0d5
    "," +                     //0xa0d6
    "," +                     //0xa0d7
    "," +                     //0xa0d8
    "__RMInit," +             //0xa0d9
    "__RMTini," +             //0xa0da
    "__RMResNew," +           //0xa0db
    "__RMRscAdd," +           //0xa0dc
    "__RMRscRemove," +        //0xa0dd
    "__RMTypeRemove," +       //0xa0de
    "__RMResDispose," +       //0xa0df
    "__RMResOpen," +          //0xa0e0
    "__RMRscGet," +           //0xa0e1
    "__RMResClose," +         //0xa0e2
    "__RMResRemove," +        //0xa0e3
    "__RMCurResSet," +        //0xa0e4
    "__RMRscRelease," +       //0xa0e5
    "__RMRscDetach," +        //0xa0e6
    "__RMMaxIDGet," +         //0xa0e7
    "__RMResSave," +          //0xa0e8
    "__RMHdlToRsc," +         //0xa0e9
    "__RMCurResGet," +        //0xa0ea
    "__RMLastResGet," +       //0xa0eb
    "__RMResLoad," +          //0xa0ec
    "__RMResLinkGet," +       //0xa0ed
    "__RMResTypeList," +      //0xa0ee
    "__RMResIDList," +        //0xa0ef
    "," +                     //0xa0f0
    "," +                     //0xa0f1
    "," +                     //0xa0f2
    "," +                     //0xa0f3
    "," +                     //0xa0f4
    "," +                     //0xa0f5
    "," +                     //0xa0f6
    "," +                     //0xa0f7
    "," +                     //0xa0f8
    "," +                     //0xa0f9
    "," +                     //0xa0fa
    "," +                     //0xa0fb
    "," +                     //0xa0fc
    "," +                     //0xa0fd
    "," +                     //0xa0fe
    "," +                     //0xa0ff
    "," +                     //0xa100
    "," +                     //0xa101
    "," +                     //0xa102
    "," +                     //0xa103
    "," +                     //0xa104
    "," +                     //0xa105
    "," +                     //0xa106
    "," +                     //0xa107
    "," +                     //0xa108
    "," +                     //0xa109
    "," +                     //0xa10a
    "," +                     //0xa10b
    "," +                     //0xa10c
    "," +                     //0xa10d
    "," +                     //0xa10e
    "," +                     //0xa10f
    "," +                     //0xa110
    "," +                     //0xa111
    "," +                     //0xa112
    "," +                     //0xa113
    "," +                     //0xa114
    "," +                     //0xa115
    "," +                     //0xa116
    "," +                     //0xa117
    "," +                     //0xa118
    "," +                     //0xa119
    "," +                     //0xa11a
    "," +                     //0xa11b
    "," +                     //0xa11c
    "," +                     //0xa11d
    "," +                     //0xa11e
    "," +                     //0xa11f
    "," +                     //0xa120
    "," +                     //0xa121
    "," +                     //0xa122
    "," +                     //0xa123
    "," +                     //0xa124
    "," +                     //0xa125
    "," +                     //0xa126
    "," +                     //0xa127
    "," +                     //0xa128
    "," +                     //0xa129
    "," +                     //0xa12a
    "," +                     //0xa12b
    "," +                     //0xa12c
    "__GMOpenGraph," +        //0xa12d
    "__GMCloseGraph," +       //0xa12e
    "," +                     //0xa12f
    "__GMInitGraph," +        //0xa130
    "__GMSetGraph," +         //0xa131
    "__GMGetGraph," +         //0xa132
    "__GMCopyGraph," +        //0xa133
    "," +                     //0xa134
    "," +                     //0xa135
    "__GMMoveGraph," +        //0xa136
    "__GMSlideGraph," +       //0xa137
    "__GMSetClip," +          //0xa138
    "__GMGetClip," +          //0xa139
    "__GMClipRect," +         //0xa13a
    "__GMSetHome," +          //0xa13b
    "__GMSetGraphSize," +     //0xa13c
    "__GMSetBitmap," +        //0xa13d
    "__GMLocalToGlobal," +    //0xa13e
    "__GMGlobalToLocal," +    //0xa13f
    "__GMInitPen," +          //0xa140
    "__GMPenShow," +          //0xa141
    "__GMPenHide," +          //0xa142
    "__GMPenSize," +          //0xa143
    "__GMPenMode," +          //0xa144
    "__GMPenPat," +           //0xa145
    "__GMExPat," +            //0xa146
    "__GMForeColor," +        //0xa147
    "__GMBackColor," +        //0xa148
    "__GMAPage," +            //0xa149
    "__GMGetLoc," +           //0xa14a
    "__GMGetPen," +           //0xa14b
    "__GMSetPen," +           //0xa14c
    "__GMInitialize," +       //0xa14d
    "__GMNullRect," +         //0xa14e
    "__GMSizeRect," +         //0xa14f
    "__GMAndRects," +         //0xa150
    "__GMMoveRect," +         //0xa151
    "__GMSlideRect," +        //0xa152
    "__GMInsetRect," +        //0xa153
    "__GMAndRect," +          //0xa154
    "__GMOrRect," +           //0xa155
    "__GMPtInRect," +         //0xa156
    "__GMEqualRect," +        //0xa157
    "__GMEmptyRect," +        //0xa158
    "__GMAdjustRect," +       //0xa159
    "__GMNewRgn," +           //0xa15a
    "__GMDisposeRgn," +       //0xa15b
    "__GMOpenRgn," +          //0xa15c
    "__GMCloseRgn," +         //0xa15d
    "__GMNullRgn," +          //0xa15e
    "__GMRectRgn," +          //0xa15f
    "__GMCopyRgn," +          //0xa160
    "__GMMoveRgn," +          //0xa161
    "__GMSlideRgn," +         //0xa162
    "__GMInsetRgn," +         //0xa163
    "__GMAndRgn," +           //0xa164
    "__GMOrRgn," +            //0xa165
    "__GMDiffRgn," +          //0xa166
    "__GMXorRgn," +           //0xa167
    "__GMPtInRgn," +          //0xa168
    "__GMRectInRgn," +        //0xa169
    "__GMEqualRgn," +         //0xa16a
    "__GMEmptyRgn," +         //0xa16b
    "__GMImgToRgn," +         //0xa16c
    "__GMInitBitmap," +       //0xa16d
    "__GMMove," +             //0xa16e
    "__GMMoveRel," +          //0xa16f
    "__GMLine," +             //0xa170
    "__GMLineRel," +          //0xa171
    "__GMFrameRect," +        //0xa172
    "__GMFillRect," +         //0xa173
    "__GMFrameOval," +        //0xa174
    "__GMFillOval," +         //0xa175
    "__GMFrameRRect," +       //0xa176
    "__GMFillRRect," +        //0xa177
    "__GMFrameArc," +         //0xa178
    "__GMFillArc," +          //0xa179
    "__GMFrameRgn," +         //0xa17a
    "__GMFillRgn," +          //0xa17b
    "__GMFramePoly," +        //0xa17c
    "__GMFillPoly," +         //0xa17d
    "__GMScroll," +           //0xa17e
    "__GMCopy," +             //0xa17f
    "__GMCopyMask," +         //0xa180
    "," +                     //0xa181
    "__GMPlotImg," +          //0xa182
    "__GMPutRImg," +          //0xa183
    "," +                     //0xa184
    "," +                     //0xa185
    "__GMDupHImg," +          //0xa186
    "__GMDupVImg," +          //0xa187
    "__GMDupHRImg," +         //0xa188
    "__GMDupVRImg," +         //0xa189
    "," +                     //0xa18a
    "__GMFontKind," +         //0xa18b
    "__GMFontFace," +         //0xa18c
    "__GMFontMode," +         //0xa18d
    "__GMFontSize," +         //0xa18e
    "__GMDrawChar," +         //0xa18f
    "__GMDrawStrL," +         //0xa190
    "__GMDrawStr," +          //0xa191
    "__GMDrawStrZ," +         //0xa192
    "," +                     //0xa193
    "__GMCharWidth," +        //0xa194
    "__GMStrLWidth," +        //0xa195
    "__GMStrWidth," +         //0xa196
    "__GMStrLength," +        //0xa197
    "__GMFontInfo," +         //0xa198
    "__GMOpenScript," +       //0xa199
    "__GMCloseScript," +      //0xa19a
    "__GMDisposeScript," +    //0xa19b
    "__GMDrawScript," +       //0xa19c
    "__GMGetScript," +        //0xa19d
    "__GMOpenPoly," +         //0xa19e
    "__GMClosePoly," +        //0xa19f
    "__GMDisposePoly," +      //0xa1a0
    "__GMShadowStrZ," +       //0xa1a1
    "__GMShadowRect," +       //0xa1a2
    "__GMInvertRect," +       //0xa1a3
    "," +                     //0xa1a4
    "__GMInvertBits," +       //0xa1a5
    "__GMMapPt," +            //0xa1a6
    "__GMMapRect," +          //0xa1a7
    "__GMMapPoly," +          //0xa1a8
    "__GMMapRgn," +           //0xa1a9
    "__GMScalePt," +          //0xa1aa
    "__GMInitPalet," +        //0xa1ab
    "," +                     //0xa1ac
    "__GMDrawG16," +          //0xa1ad
    "," +                     //0xa1ae
    "__GMGetPixel," +         //0xa1af
    "," +                     //0xa1b0
    "__GMCalcMask," +         //0xa1b1
    "__GMCalcFrame," +        //0xa1b2
    "__SXLongMul," +          //0xa1b3
    "__SXFixRound," +         //0xa1b4
    "," +                     //0xa1b5
    "__SXFixMul," +           //0xa1b6
    "__SXFixDiv," +           //0xa1b7
    "__GMGetFontTable," +     //0xa1b8
    "__GMCopyStdProc," +      //0xa1b9
    "__GMStrZWidth," +        //0xa1ba
    "__GMTransImg," +         //0xa1bb
    "__GMFillRImg," +         //0xa1bc
    "__GMFillImg," +          //0xa1bd
    "__GMSlidedRgn," +        //0xa1be
    "__GMPaintRgn," +         //0xa1bf
    "__GMSetRgnLine," +       //0xa1c0
    "__GMGetRgnLine," +       //0xa1c1
    "__GMInitGraphMode," +    //0xa1c2
    "__GMCurFont," +          //0xa1c3
    "__GMGetScrnSize," +      //0xa1c4
    "__GMExgGraph," +         //0xa1c5
    "__GMExgBitmap," +        //0xa1c6
    "__GMGetBitmap," +        //0xa1c7
    "__GMCalcBitmap," +       //0xa1c8
    "__GMCalcScrnSize," +     //0xa1c9
    "__GMNewBits," +          //0xa1ca
    "__GMDisposeBits," +      //0xa1cb
    "__GMLockBits," +         //0xa1cc
    "__GMUnlockBits," +       //0xa1cd
    "__GMItalicRect," +       //0xa1ce
    "__GMItalicRgn," +        //0xa1cf
    "__GMFreeBits," +         //0xa1d0
    "__GMCalcGraph," +        //0xa1d1
    "__GMPackImage," +        //0xa1d2
    "__GMUnpackImage," +      //0xa1d3
    "__GMAdjustPt," +         //0xa1d4
    "__GMPutImg," +           //0xa1d5
    "__GMCenterRect," +       //0xa1d6
    "__GMScrewRect," +        //0xa1d7
    "__GMAndRectRgn," +       //0xa1d8
    "__GMOrRectRgn," +        //0xa1d9
    "__GMDiffRectRgn," +      //0xa1da
    "__GMXorRectRgn," +       //0xa1db
    "__GMCharKind," +         //0xa1dc
    "__GMDiffRgnRect," +      //0xa1dd
    "," +                     //0xa1de
    "," +                     //0xa1df
    "__GMAddFont," +          //0xa1e0
    "__GMRemoveFont," +       //0xa1e1
    "__GMGetFontLink," +      //0xa1e2
    "__GMGetHProcTbl," +      //0xa1e3
    "," +                     //0xa1e4
    "," +                     //0xa1e5
    "__GMGetStdProcTbl," +    //0xa1e6
    "__GMGetFontProcTbl," +   //0xa1e7
    "__GMGetRgnProcTbl," +    //0xa1e8
    "__GMDrawGsOne," +        //0xa1e9
    "__GMPtInImg," +          //0xa1ea
    "__GMFrameNPoly," +       //0xa1eb
    "__GMFillNPoly," +        //0xa1ec
    "__GMNPolyFrRgn," +       //0xa1ed
    "__GMNPolyFlRgn," +       //0xa1ee
    "__GMPtInNPoly," +        //0xa1ef
    "__GMPtOnNPoly," +        //0xa1f0
    "__GMRecordScript," +     //0xa1f1
    "__GMNLineRel," +         //0xa1f2
    "__GMNLine," +            //0xa1f3
    "__GMRecordPoly," +       //0xa1f4
    "," +                     //0xa1f5
    "," +                     //0xa1f6
    "," +                     //0xa1f7
    "__WMInit," +             //0xa1f8
    "__WMOpen," +             //0xa1f9
    "__WMRefer," +            //0xa1fa
    "__WMClose," +            //0xa1fb
    "__WMDispose," +          //0xa1fc
    "__WMFind," +             //0xa1fd
    "__WMSelect," +           //0xa1fe
    "__WMSelect2," +          //0xa1ff
    "__WMCarry," +            //0xa200
    "__WMShine," +            //0xa201
    "__WMMove," +             //0xa202
    "__WMSize," +             //0xa203
    "__WMGrow," +             //0xa204
    "__WMDrag," +             //0xa205
    "__WMZoom," +             //0xa206
    "__WMShow," +             //0xa207
    "__WMHide," +             //0xa208
    "__WMShowHide," +         //0xa209
    "__WMCheckBox," +         //0xa20a
    "__WMCheckCBox," +        //0xa20b
    "__WMDrawGBox," +         //0xa20c
    "__WMUpdate," +           //0xa20d
    "__WMUpdtOver," +         //0xa20e
    "__WMActive," +           //0xa20f
    "__WMGraphGet," +         //0xa210
    "," +                     //0xa211
    "," +                     //0xa212
    "," +                     //0xa213
    "," +                     //0xa214
    "," +                     //0xa215
    "," +                     //0xa216
    "," +                     //0xa217
    "__WMAddRect," +          //0xa218
    "__WMAddRgn," +           //0xa219
    "__WMSubRect," +          //0xa21a
    "__WMSubRgn," +           //0xa21b
    "__WMGScriptSet," +       //0xa21c
    "__WMGScriptGet," +       //0xa21d
    "__WMTitleSet," +         //0xa21e
    "__WMTitleGet," +         //0xa21f
    "__WMTIDSet," +           //0xa220
    "__WMTIDGet," +           //0xa221
    "__WMPinRect," +          //0xa222
    "__WMCalcUpdt," +         //0xa223
    "__WMGetDTGS," +          //0xa224
    "__WMDragRgn," +          //0xa225
    "," +                     //0xa226
    "__WSOpen," +             //0xa227
    "__WSClose," +            //0xa228
    "__WSDispose," +          //0xa229
    "__WSEnlist," +           //0xa22a
    "__WSDelist," +           //0xa22b
    "__WMOptionGet," +        //0xa22c
    "__WMOptionSet," +        //0xa22d
    "__WMPtInGBox," +         //0xa22e
    "__WHOpen," +             //0xa22f
    "__WHGet," +              //0xa230
    "__WMOpen2," +            //0xa231
    "__WMMargineGet," +       //0xa232
    "__WMMargineSet," +       //0xa233
    "," +                     //0xa234
    "__WMMove2," +            //0xa235
    "," +                     //0xa236
    "," +                     //0xa237
    "," +                     //0xa238
    "," +                     //0xa239
    "," +                     //0xa23a
    "," +                     //0xa23b
    "," +                     //0xa23c
    "," +                     //0xa23d
    "," +                     //0xa23e
    "," +                     //0xa23f
    "," +                     //0xa240
    "," +                     //0xa241
    "," +                     //0xa242
    "," +                     //0xa243
    "," +                     //0xa244
    "," +                     //0xa245
    "," +                     //0xa246
    "," +                     //0xa247
    "," +                     //0xa248
    "," +                     //0xa249
    "," +                     //0xa24a
    "," +                     //0xa24b
    "," +                     //0xa24c
    "," +                     //0xa24d
    "," +                     //0xa24e
    "," +                     //0xa24f
    "," +                     //0xa250
    "," +                     //0xa251
    "," +                     //0xa252
    "," +                     //0xa253
    "," +                     //0xa254
    "," +                     //0xa255
    "," +                     //0xa256
    "," +                     //0xa257
    "," +                     //0xa258
    "," +                     //0xa259
    "," +                     //0xa25a
    "," +                     //0xa25b
    "," +                     //0xa25c
    "," +                     //0xa25d
    "," +                     //0xa25e
    "," +                     //0xa25f
    "," +                     //0xa260
    "," +                     //0xa261
    "," +                     //0xa262
    "," +                     //0xa263
    "," +                     //0xa264
    "," +                     //0xa265
    "__MNInit," +             //0xa266
    "__MNRefer," +            //0xa267
    "__MNSelect," +           //0xa268
    "__MNConvert," +          //0xa269
    "," +                     //0xa26a
    "__MNConvert2," +         //0xa26b
    "__MNSelect3," +          //0xa26c
    "," +                     //0xa26d
    "," +                     //0xa26e
    "," +                     //0xa26f
    "," +                     //0xa270
    "," +                     //0xa271
    "," +                     //0xa272
    "," +                     //0xa273
    "," +                     //0xa274
    "," +                     //0xa275
    "," +                     //0xa276
    "," +                     //0xa277
    "," +                     //0xa278
    "," +                     //0xa279
    "," +                     //0xa27a
    "," +                     //0xa27b
    "," +                     //0xa27c
    "," +                     //0xa27d
    "," +                     //0xa27e
    "," +                     //0xa27f
    "," +                     //0xa280
    "," +                     //0xa281
    "," +                     //0xa282
    "," +                     //0xa283
    "," +                     //0xa284
    "," +                     //0xa285
    "," +                     //0xa286
    "," +                     //0xa287
    "," +                     //0xa288
    "__CMOpen," +             //0xa289
    "__CMDispose," +          //0xa28a
    "__CMKill," +             //0xa28b
    "__CMHide," +             //0xa28c
    "__CMShow," +             //0xa28d
    "__CMDraw," +             //0xa28e
    "__CMDrawOne," +          //0xa28f
    "__CMValueSet," +         //0xa290
    "__CMValueGet," +         //0xa291
    "__CMMinSet," +           //0xa292
    "__CMMinGet," +           //0xa293
    "__CMMaxSet," +           //0xa294
    "__CMMaxGet," +           //0xa295
    "__CMMove," +             //0xa296
    "__CMSize," +             //0xa297
    "__CMShine," +            //0xa298
    "__CMFind," +             //0xa299
    "__CMCheck," +            //0xa29a
    "__CMRefer," +            //0xa29b
    "__CMTitleGet," +         //0xa29c
    "__CMDragControl," +      //0xa29d
    "__CMDraws," +            //0xa29e
    "__CMTitleSet," +         //0xa29f
    "__CMOptionGet," +        //0xa2a0
    "__CMOptionSet," +        //0xa2a1
    "__CMUserGet," +          //0xa2a2
    "__CMUserSet," +          //0xa2a3
    "__CMProcGet," +          //0xa2a4
    "__CMProcSet," +          //0xa2a5
    "__CMDefDataGet," +       //0xa2a6
    "__CMDefDataSet," +       //0xa2a7
    "," +                     //0xa2a8
    "," +                     //0xa2a9
    "," +                     //0xa2aa
    "," +                     //0xa2ab
    "," +                     //0xa2ac
    "," +                     //0xa2ad
    "," +                     //0xa2ae
    "," +                     //0xa2af
    "," +                     //0xa2b0
    "," +                     //0xa2b1
    "," +                     //0xa2b2
    "," +                     //0xa2b3
    "," +                     //0xa2b4
    "," +                     //0xa2b5
    "," +                     //0xa2b6
    "," +                     //0xa2b7
    "," +                     //0xa2b8
    "," +                     //0xa2b9
    "," +                     //0xa2ba
    "," +                     //0xa2bb
    "," +                     //0xa2bc
    "," +                     //0xa2bd
    "," +                     //0xa2be
    "," +                     //0xa2bf
    "__DMInit," +             //0xa2c0
    "__ErrorSound," +         //0xa2c1
    "__DMFontSet," +          //0xa2c2
    "__DMOpen," +             //0xa2c3
    "__DMRefer," +            //0xa2c4
    "__DMClose," +            //0xa2c5
    "__DMDispose," +          //0xa2c6
    "__DMControl," +          //0xa2c7
    "__DMDraw," +             //0xa2c8
    "__Alart," +              //0xa2c9
    "__StopAlart," +          //0xa2ca
    "__NoteAlart," +          //0xa2cb
    "__CautionAlart," +       //0xa2cc
    "__CouldAlart," +         //0xa2cd
    "__FreeAlart," +          //0xa2ce
    "__DIGet," +              //0xa2cf
    "__DISet," +              //0xa2d0
    "__DITGet," +             //0xa2d1
    "__DITSet," +             //0xa2d2
    "__DITSelect," +          //0xa2d3
    "__GetAlrtStage," +       //0xa2d4
    "__ResetAlrtStage," +     //0xa2d5
    "__DIUpdate," +           //0xa2d6
    "__DMBeep," +             //0xa2d7
    "__DIHide," +             //0xa2d8
    "__DIShow," +             //0xa2d9
    "," +                     //0xa2da
    "," +                     //0xa2db
    "," +                     //0xa2dc
    "," +                     //0xa2dd
    "," +                     //0xa2de
    "," +                     //0xa2df
    "," +                     //0xa2e0
    "," +                     //0xa2e1
    "," +                     //0xa2e2
    "," +                     //0xa2e3
    "," +                     //0xa2e4
    "," +                     //0xa2e5
    "," +                     //0xa2e6
    "," +                     //0xa2e7
    "," +                     //0xa2e8
    "," +                     //0xa2e9
    "," +                     //0xa2ea
    "," +                     //0xa2eb
    "," +                     //0xa2ec
    "," +                     //0xa2ed
    "," +                     //0xa2ee
    "," +                     //0xa2ef
    "," +                     //0xa2f0
    "," +                     //0xa2f1
    "," +                     //0xa2f2
    "," +                     //0xa2f3
    "," +                     //0xa2f4
    "," +                     //0xa2f5
    "__DMError," +            //0xa2f6
    "__DMWaitOpen," +         //0xa2f7
    "__DMWaitClose," +        //0xa2f8
    "__DMWaitWhile," +        //0xa2f9
    "__DMError2," +           //0xa2fa
    "," +                     //0xa2fb
    "," +                     //0xa2fc
    "," +                     //0xa2fd
    "," +                     //0xa2fe
    "," +                     //0xa2ff
    "," +                     //0xa300
    "," +                     //0xa301
    "," +                     //0xa302
    "," +                     //0xa303
    "," +                     //0xa304
    "," +                     //0xa305
    "," +                     //0xa306
    "," +                     //0xa307
    "," +                     //0xa308
    "," +                     //0xa309
    "__TMInit," +             //0xa30a
    "__TMNew," +              //0xa30b
    "__TMSetRect," +          //0xa30c
    "__TMChangeText," +       //0xa30d
    "__TMIdle," +             //0xa30e
    "__TMActive," +           //0xa30f
    "__TMDeactive," +         //0xa310
    "__TMCaret," +            //0xa311
    "__TMDispose," +          //0xa312
    "__TMUpDate," +           //0xa313
    "__TMSetText," +          //0xa314
    "__TMGetText," +          //0xa315
    "__TMSetSelect," +        //0xa316
    "__TMKey," +              //0xa317
    "__TMStr," +              //0xa318
    "__TMCalText," +          //0xa319
    "__TMPinScroll," +        //0xa31a
    "__TMClick," +            //0xa31b
    "__TMEvent," +            //0xa31c
    "," +                     //0xa31d
    "," +                     //0xa31e
    "," +                     //0xa31f
    "__TMCut," +              //0xa320
    "__TMCopy," +             //0xa321
    "__TMPaste," +            //0xa322
    "__TMDelete," +           //0xa323
    "__TMInsert," +           //0xa324
    "__TMFromScrap," +        //0xa325
    "__TMToScrap," +          //0xa326
    "__TMScrapHandle," +      //0xa327
    "__TMGetScrapLen," +      //0xa328
    "," +                     //0xa329
    "__TMTextBox," +          //0xa32a
    "__TMTextBox2," +         //0xa32b
    "__TMCacheON," +          //0xa32c
    "__TMCacheOFF," +         //0xa32d
    "__TMCacheFlush," +       //0xa32e
    "__TMShow," +             //0xa32f
    "__TMHide," +             //0xa330
    "__TMSelShow," +          //0xa331
    "__TMSelHide," +          //0xa332
    "__TMSearchStrF," +       //0xa333
    "__TMSearchStrB," +       //0xa334
    "__TMTextInWidth2," +     //0xa335
    "__TMTextWidth2," +       //0xa336
    "__TMDrawText2," +        //0xa337
    "__TMUpDate2," +          //0xa338
    "__TMUpDate3," +          //0xa339
    "__TMCalCOLine," +        //0xa33a
    "," +                     //0xa33b
    "__TMCalLine," +          //0xa33c
    "__TMLeftSel," +          //0xa33d
    "__TMRightSel," +         //0xa33e
    "__TMPointSel," +         //0xa33f
    "__TMOffsetSel," +        //0xa340
    "__TMPointToLine," +      //0xa341
    "," +                     //0xa342
    "__TMCalSelPoint," +      //0xa343
    "," +                     //0xa344
    "__TMSetView," +          //0xa345
    "__TMScroll," +           //0xa346
    "__TMPointScroll," +      //0xa347
    "__TMStr2," +             //0xa348
    "__TMKeyToAsk," +         //0xa349
    "__TMNextCode," +         //0xa34a
    "__TMSetTextH," +         //0xa34b
    "__TSInitTsk," +          //0xa34c
    "__TSTiniTsk2," +         //0xa34d
    "__TSInitCrtM," +         //0xa34e
    "__TSTiniCrtM," +         //0xa34f
    "," +                     //0xa350
    "__TSFock," +             //0xa351
    "__TSExit," +             //0xa352
    "__TSFockB," +            //0xa353
    "," +                     //0xa354
    "__TSFockSItem," +        //0xa355
    "__TSFockIcon," +         //0xa356
    "__TSEventAvail," +       //0xa357
    "__TSGetEvent," +         //0xa358
    "," +                     //0xa359
    "__TSPostEventTsk," +     //0xa35a
    "__TSGetTdb," +           //0xa35b
    "__TSSetTdb," +           //0xa35c
    "," +                     //0xa35d
    "__TSGetWindowPos," +     //0xa35e
    "__TSCommunicate," +      //0xa35f
    "__TSGetID," +            //0xa360
    "__TSMakeEvent," +        //0xa361
    "," +                     //0xa362
    "," +                     //0xa363
    "__TSGetStartMode," +     //0xa364
    "__TSSetStartMode," +     //0xa365
    "__TMOpen," +             //0xa366
    "__TSOpen," +             //0xa367
    "__TSClose," +            //0xa368
    "__TSRmDirH," +           //0xa369
    "__TSCopyH," +            //0xa36a
    "__TSMkDirH," +           //0xa36b
    "__TSMoveH," +            //0xa36c
    "__TSCreate," +           //0xa36d
    "__TSDeleteH," +          //0xa36e
    "__TSTrash," +            //0xa36f
    "__TSFiles," +            //0xa370
    "__TSNFiles," +           //0xa371
    "__TSCopyP," +            //0xa372
    "__TSDeleteP," +          //0xa373
    "__TSRmDirP," +           //0xa374
    "__TSMkDirP," +           //0xa375
    "__TSMoveP," +            //0xa376
    "," +                     //0xa377
    "__TSChMod," +            //0xa378
    "__TSWhatFile," +         //0xa379
    "," +                     //0xa37a
    "__TSDeleteVoname," +     //0xa37b
    "__TSCreateVoname," +     //0xa37c
    "," +                     //0xa37d
    "," +                     //0xa37e
    "," +                     //0xa37f
    "," +                     //0xa380
    "__TSSearchFileND," +     //0xa381
    "," +                     //0xa382
    "," +                     //0xa383
    "," +                     //0xa384
    "," +                     //0xa385
    "__TSGetOpen," +          //0xa386
    "__TSZeroDrag," +         //0xa387
    "__TSPutDrag," +          //0xa388
    "__TSGetDrag," +          //0xa389
    "__TSBeginDrag," +        //0xa38a
    "," +                     //0xa38b
    "__TSEndDrag," +          //0xa38c
    "__TSHideDrag," +         //0xa38d
    "__TSShowDrag," +         //0xa38e
    "__TSZeroScrap," +        //0xa38f
    "__TSPutScrap," +         //0xa390
    "__TSGetScrap," +         //0xa391
    "," +                     //0xa392
    "," +                     //0xa393
    "," +                     //0xa394
    "," +                     //0xa395
    "," +                     //0xa396
    "__TSSearchTrashpath," +  //0xa397
    "__TSSearchTrashfile," +  //0xa398
    "__TSEmptyTrash," +       //0xa399
    "," +                     //0xa39a
    "__TSSearchdpb," +        //0xa39b
    "," +                     //0xa39c
    "__TSDrvctrl," +          //0xa39d
    "__TSDrvctrl2," +         //0xa39e
    "," +                     //0xa39f
    "," +                     //0xa3a0
    "," +                     //0xa3a1
    "__SXCallWindM," +        //0xa3a2
    "__SXCallCtrlM," +        //0xa3a3
    "," +                     //0xa3a4
    "," +                     //0xa3a5
    "," +                     //0xa3a6
    "," +                     //0xa3a7
    "," +                     //0xa3a8
    "," +                     //0xa3a9
    "__SXInvalScBar," +       //0xa3aa
    "__SXValidScBar," +       //0xa3ab
    "," +                     //0xa3ac
    "," +                     //0xa3ad
    "," +                     //0xa3ae
    "," +                     //0xa3af
    "," +                     //0xa3b0
    "," +                     //0xa3b1
    "," +                     //0xa3b2
    "," +                     //0xa3b3
    "," +                     //0xa3b4
    "," +                     //0xa3b5
    "," +                     //0xa3b6
    "," +                     //0xa3b7
    "," +                     //0xa3b8
    "," +                     //0xa3b9
    "," +                     //0xa3ba
    "__TSISRecToStr," +       //0xa3bb
    "," +                     //0xa3bc
    "," +                     //0xa3bd
    "," +                     //0xa3be
    "__TSCreateISFile," +     //0xa3bf
    "," +                     //0xa3c0
    "," +                     //0xa3c1
    "," +                     //0xa3c2
    "," +                     //0xa3c3
    "," +                     //0xa3c4
    "," +                     //0xa3c5
    "," +                     //0xa3c6
    "," +                     //0xa3c7
    "," +                     //0xa3c8
    "," +                     //0xa3c9
    "," +                     //0xa3ca
    "," +                     //0xa3cb
    "__SXFileConnPath," +     //0xa3cc
    "__SXFileInPath," +       //0xa3cd
    "," +                     //0xa3ce
    "," +                     //0xa3cf
    "__SXFnamecmp," +         //0xa3d0
    "," +                     //0xa3d1
    "," +                     //0xa3d2
    "," +                     //0xa3d3
    "__SXSearchFname," +      //0xa3d4
    "," +                     //0xa3d5
    "," +                     //0xa3d6
    "," +                     //0xa3d7
    "__SXStoLower," +         //0xa3d8
    "__SXStoUpper," +         //0xa3d9
    "__SXStoUpper2," +        //0xa3da
    "," +                     //0xa3db
    "," +                     //0xa3dc
    "," +                     //0xa3dd
    "," +                     //0xa3de
    "," +                     //0xa3df
    "," +                     //0xa3e0
    "," +                     //0xa3e1
    "," +                     //0xa3e2
    "," +                     //0xa3e3
    "," +                     //0xa3e4
    "," +                     //0xa3e5
    "," +                     //0xa3e6
    "," +                     //0xa3e7
    "," +                     //0xa3e8
    "__SXVer," +              //0xa3e9
    "__TSTakeParam," +        //0xa3ea
    "," +                     //0xa3eb
    "," +                     //0xa3ec
    "," +                     //0xa3ed
    "," +                     //0xa3ee
    "," +                     //0xa3ef
    "," +                     //0xa3f0
    "," +                     //0xa3f1
    "," +                     //0xa3f2
    "," +                     //0xa3f3
    "__TSFindTskn," +         //0xa3f4
    "," +                     //0xa3f5
    "," +                     //0xa3f6
    "__TSDriveCheckAll," +    //0xa3f7
    "__TSDriveCheck," +       //0xa3f8
    "__TSISRecToExec," +      //0xa3f9
    "__TSGetDtopMode," +      //0xa3fa
    "__TSSetDtopMode," +      //0xa3fb
    "__TSSearchOpen," +       //0xa3fc
    "," +                     //0xa3fd
    "__TSFindOwn," +          //0xa3fe
    "__TSCommunicateS," +     //0xa3ff
    "," +                     //0xa400
    "__TMNew2," +             //0xa401
    "__TSSearchFile2," +      //0xa402
    "__TSSearchFile," +       //0xa403
    "," +                     //0xa404
    "," +                     //0xa405
    "__SXStrCmp," +           //0xa406
    "," +                     //0xa407
    "__TSCreateISBadge," +    //0xa408
    "," +                     //0xa409
    "__TSGetCMDS," +          //0xa40a
    "__TSFockCM," +           //0xa40b
    "," +                     //0xa40c
    "__TSTiniTsk," +          //0xa40d
    "," +                     //0xa40e
    "," +                     //0xa40f
    "," +                     //0xa410
    "," +                     //0xa411
    "__SXStrCpy," +           //0xa412
    "," +                     //0xa413
    "," +                     //0xa414
    "__TSPostEventTsk2," +    //0xa415
    "," +                     //0xa416
    "__TSAnswer," +           //0xa417
    "__TSSendMes," +          //0xa418
    "__TSGetMes," +           //0xa419
    "__TSInitTsk2," +         //0xa41a
    "," +                     //0xa41b
    "," +                     //0xa41c
    "," +                     //0xa41d
    "," +                     //0xa41e
    "__SXCallWindM2," +       //0xa41f
    "__TSBeginDrag2," +       //0xa420
    "," +                     //0xa421
    "__SXGetVector," +        //0xa422
    "__SXSetVector," +        //0xa423
    "," +                     //0xa424
    "," +                     //0xa425
    "," +                     //0xa426
    "__TSCellToStr," +        //0xa427
    "," +                     //0xa428
    "," +                     //0xa429
    "__SXLockFSX," +          //0xa42a
    "__SXUnlockFSX," +        //0xa42b
    "__TSFockMode," +         //0xa42c
    "," +                     //0xa42d
    "," +                     //0xa42e
    "," +                     //0xa42f
    "__TSSetGraphMode," +     //0xa430
    "__TSGetGraphMode," +     //0xa431
    "__SXGetDispRect," +      //0xa432
    "," +                     //0xa433
    "," +                     //0xa434
    "__SXSRAMVer," +          //0xa435
    "__SXSRAMReset," +        //0xa436
    "__SXSRAMCheck," +        //0xa437
    "__TSAdjustRect," +       //0xa438
    "," +                     //0xa439
    "," +                     //0xa43a
    "__TSPostEventTsk3," +    //0xa43b
    "," +                     //0xa43c
    "," +                     //0xa43d
    "__TSAnswer2," +          //0xa43e
    "," +                     //0xa43f
    "," +                     //0xa440
    "," +                     //0xa441
    "," +                     //0xa442
    "__TSErrDialogN," +       //0xa443
    "," +                     //0xa444
    "," +                     //0xa445
    "__TSSearchFile3," +      //0xa446
    "," +                     //0xa447
    "," +                     //0xa448
    "," +                     //0xa449
    "," +                     //0xa44a
    "__TSNameToCode," +       //0xa44b
    "__TSCodeToName," +       //0xa44c
    "__TSNameToHdl," +        //0xa44d
    "," +                     //0xa44e
    "," +                     //0xa44f
    "__SXPack," +             //0xa450
    "__SXUnpack," +           //0xa451
    "__SXGetPackSize," +      //0xa452
    "__SXGetCODFList," +      //0xa453
    "__SXCellToCODF," +       //0xa454
    "__GMDitherImg," +        //0xa455
    "," +                     //0xa456
    "," +                     //0xa457
    "," +                     //0xa458
    "," +                     //0xa459
    "," +                     //0xa45a
    "," +                     //0xa45b
    "," +                     //0xa45c
    "," +                     //0xa45d
    "," +                     //0xa45e
    "," +                     //0xa45f
    "__TMNextCodeIn," +       //0xa460
    "," +                     //0xa461
    "__TMSelReverse," +       //0xa462
    "__TMTini," +             //0xa463
    "__TMSetSelCal," +        //0xa464
    "__TMActivate2," +        //0xa465
    "__TMDeactivate2," +      //0xa466
    "__TMCheckSel," +         //0xa467
    "__TMCalPoint2," +        //0xa468
    "," +                     //0xa469
    "__TMISZen," +            //0xa46a
    "__TMSetDestOffset," +    //0xa46b
    "__TMGetDestOffset," +    //0xa46c
    "__TMGetSelect," +        //0xa46d
    "__TMEventW," +           //0xa46e
    "__TMUpDateExist," +      //0xa46f
    "__TMNewM," +             //0xa470
    "__TMSetTextM," +         //0xa471
    "__TMSetDefKind," +       //0xa472
    "__TMGetStyle," +         //0xa473
    "," +                     //0xa474
    "__TMGetStyles," +        //0xa475
    "__TMChangeStyle," +      //0xa476
    "__TMChangeFace," +       //0xa477
    "__TMSetColor," +         //0xa478
    "__TMSetMode," +          //0xa479
    "__TMPutScrapM," +        //0xa47a
    "__TMInsertM," +          //0xa47b
    "__TMStrM," +             //0xa47c
    "__TMSetStyles," +        //0xa47d
    "__TMGetExStyles," +      //0xa47e
    "__TMGetScrap," +         //0xa47f
    "__TMGetLineWidth," +     //0xa480
    "__TMGetLineHeight," +    //0xa481
    "__TMLineToHeight," +     //0xa482
    "__TMAdjustHeight," +     //0xa483
    "__TMChangeExStyle," +    //0xa484
    "__TMAnalyzeExStyle," +   //0xa485
    "__TMSetEditMode," +      //0xa486
    "__TMCellToFont," +       //0xa487
    "__TMScaleSet," +         //0xa488
    "__TMScaleStyles," +      //0xa489
    "__TMBundleExStyle," +    //0xa48a
    "__TMSetLineHeight," +    //0xa48b
    "__TMSetTabSize," +       //0xa48c
    "__TMGetStr," +           //0xa48d
    "__TMScalePtSet," +       //0xa48e
    "__TMScalePtReSet," +     //0xa48f
    "__TMGetDefKind," +       //0xa490
    "," +                     //0xa491
    "," +                     //0xa492
    "," +                     //0xa493
    "__TMVer," +              //0xa494
    "__TMSetPage," +          //0xa495
    "__TMHeightToPage," +     //0xa496
    "__TMOffsetToPage," +     //0xa497
    "__TMPageToLine," +       //0xa498
    "__TMLineToPage," +       //0xa499
    "__TMTextWidth3," +       //0xa49a
    "__TMLineToRHeight," +    //0xa49b
    "," +                     //0xa49c
    "__TMGetLineRHeight," +   //0xa49d
    "," +                     //0xa49e
    "," +                     //0xa49f
    "__TSResNew," +           //0xa4a0
    "__TSResOpen," +          //0xa4a1
    "__TSResClose," +         //0xa4a2
    "__TSResSave," +          //0xa4a3
    "__TSResRemove," +        //0xa4a4
    "__TSResLoad," +          //0xa4a5
    "__TSResDispose," +       //0xa4a6
    "__TSCurResGet," +        //0xa4a7
    "__TSLastResGet," +       //0xa4a8
    "__TSCurResSet," +        //0xa4a9
    "__TSRscAdd," +           //0xa4aa
    "__TSRscGet," +           //0xa4ab
    "__TSRscRemove," +        //0xa4ac
    "__TSTypeRemove," +       //0xa4ad
    "__TSRscRelease," +       //0xa4ae
    "__TSRscDetach," +        //0xa4af
    "__TSMaxIDGet," +         //0xa4b0
    "__TSHdlToRsc," +         //0xa4b1
    "__TSResLinkGet," +       //0xa4b2
    "__TSResRouteLink," +     //0xa4b3
    "__TSResRouteGet," +      //0xa4b4
    "__TSRscGet2," +          //0xa4b5
    "__TSRscGet3," +          //0xa4b6
    "__TSResRouteUnLink," +   //0xa4b7
    "__TSMaxIDGet2," +        //0xa4b8
    "__TSFind," +             //0xa4b9
    "__TSCurResGet2," +       //0xa4ba
    "__TSMaxIDGet3," +        //0xa4bb
    "__TSResFileGet," +       //0xa4bc
    "__TSResRouteFind," +     //0xa4bd
    "__TSResTypeList," +      //0xa4be
    "__TSResIDList," +        //0xa4bf
    "__TSRscScan2," +         //0xa4c0
    "," +                     //0xa4c1
    "," +                     //0xa4c2
    "," +                     //0xa4c3
    "," +                     //0xa4c4
    "," +                     //0xa4c5
    "," +                     //0xa4c6
    "," +                     //0xa4c7
    "," +                     //0xa4c8
    "," +                     //0xa4c9
    "," +                     //0xa4ca
    "," +                     //0xa4cb
    "," +                     //0xa4cc
    "," +                     //0xa4cd
    "," +                     //0xa4ce
    "," +                     //0xa4cf
    "," +                     //0xa4d0
    "," +                     //0xa4d1
    "," +                     //0xa4d2
    "," +                     //0xa4d3
    "," +                     //0xa4d4
    "," +                     //0xa4d5
    "," +                     //0xa4d6
    "," +                     //0xa4d7
    "," +                     //0xa4d8
    "," +                     //0xa4d9
    "," +                     //0xa4da
    "," +                     //0xa4db
    "," +                     //0xa4dc
    "," +                     //0xa4dd
    "," +                     //0xa4de
    "," +                     //0xa4df
    "__PMInit," +             //0xa4e0
    "__PMTini," +             //0xa4e1
    "__PMOpen," +             //0xa4e2
    "__PMClose," +            //0xa4e3
    "__PMSetDefault," +       //0xa4e4
    "__PMValidate," +         //0xa4e5
    "__PMImageDialog," +      //0xa4e6
    "__PMStrDialog," +        //0xa4e7
    "__PMJobDialog," +        //0xa4e8
    "__PMEnvCopy," +          //0xa4e9
    "__PMJobCopy," +          //0xa4ea
    "__PMOpenImage," +        //0xa4eb
    "__PMRecordPage," +       //0xa4ec
    "__PMPrintPage," +        //0xa4ed
    "__PMCancelPage," +       //0xa4ee
    "__PMAction," +           //0xa4ef
    "__PMCloseImage," +       //0xa4f0
    "__PMDrawString," +       //0xa4f1
    "__PMVer," +              //0xa4f2
    "__PMDrvrVer," +          //0xa4f3
    "__PMDrvrCtrl," +         //0xa4f4
    "__PMDrvrID," +           //0xa4f5
    "__PMDrvrHdl," +          //0xa4f6
    "__PMMaxRect," +          //0xa4f7
    "__PMSaveEnv," +          //0xa4f8
    "__PMReady," +            //0xa4f9
    "__PMProcPrint," +        //0xa4fa
    "__PMDrvrInfo," +         //0xa4fb
    "__PMGetDefDlog," +       //0xa4fc
    "__PMSetRange," +         //0xa4fd
    "__PMPutID," +            //0xa4fe
    "," +                     //0xa4ff
    "__FMInit," +             //0xa500
    "__FMTini," +             //0xa501
    "__FMGetFontList," +      //0xa502
    "__FMSetCacheSize," +     //0xa503
    "__FMGetCacheSize," +     //0xa504
    "__FMSetSpaceWidth," +    //0xa505
    "__FMGetSpaceWidth," +    //0xa506
    "__FMSetTracking," +      //0xa507
    "__FMGetTracking," +      //0xa508
    "__FMGetKerningWidth," +  //0xa509
    "__FMFontMenuSelect," +   //0xa50a
    "__FMGetFontPolyData," +  //0xa50b
    "," +                     //0xa50c
    "," +                     //0xa50d
    "," +                     //0xa50e
    "," +                     //0xa50f
    "," +                     //0xa510
    "," +                     //0xa511
    "," +                     //0xa512
    "," +                     //0xa513
    "," +                     //0xa514
    "," +                     //0xa515
    "," +                     //0xa516
    "," +                     //0xa517
    "," +                     //0xa518
    "," +                     //0xa519
    "," +                     //0xa51a
    "," +                     //0xa51b
    "," +                     //0xa51c
    "," +                     //0xa51d
    "," +                     //0xa51e
    "," +                     //0xa51f
    "," +                     //0xa520
    "," +                     //0xa521
    "," +                     //0xa522
    "," +                     //0xa523
    "," +                     //0xa524
    "," +                     //0xa525
    "," +                     //0xa526
    "," +                     //0xa527
    "," +                     //0xa528
    "," +                     //0xa529
    "," +                     //0xa52a
    "," +                     //0xa52b
    "," +                     //0xa52c
    "," +                     //0xa52d
    "," +                     //0xa52e
    "," +                     //0xa52f
    "," +                     //0xa530
    "," +                     //0xa531
    "," +                     //0xa532
    "," +                     //0xa533
    "," +                     //0xa534
    "," +                     //0xa535
    "," +                     //0xa536
    "," +                     //0xa537
    "," +                     //0xa538
    "," +                     //0xa539
    "," +                     //0xa53a
    "," +                     //0xa53b
    "," +                     //0xa53c
    "," +                     //0xa53d
    "," +                     //0xa53e
    "," +                     //0xa53f
    "__GMSetFlattness," +     //0xa540
    "__GMGetFlattness," +     //0xa541
    "__GMSetBSDepth," +       //0xa542
    "__GMGetBSDepth," +       //0xa543
    "__GMDrawBezier," +       //0xa544
    "__GMDrawBSpline," +      //0xa545
    "__GMSplitBezier," +      //0xa546
    "__GMSplitBSpline," +     //0xa547
    "__GMPtOnBezier," +       //0xa548
    "__GMPtOnBSpline," +      //0xa549
    "__GMSetBSError," +       //0xa54a
    "__GMGetBSError," +       //0xa54b
    "," +                     //0xa54c
    "," +                     //0xa54d
    "," +                     //0xa54e
    "," +                     //0xa54f
    "__GMSetGSDraw," +        //0xa550
    "__GMGetGSDraw," +        //0xa551
    "__GMSetGSGet," +         //0xa552
    "__GMGetGSGet," +         //0xa553
    "__GMTileRImg," +         //0xa554
    "__GMTileImg," +          //0xa555
    "__GMSetDispOffset," +    //0xa556
    "__GMGetDispOffset," +    //0xa557
    "__GMTestScrKindG," +     //0xa558
    "__GMGetScrKindG," +      //0xa559
    "," +                     //0xa55a
    "," +                     //0xa55b
    "," +                     //0xa55c
    "__GMGetGraphMode," +     //0xa55d
    "__GMSetPalet," +         //0xa55e
    "__GMGetPalet," +         //0xa55f
    "__GMCopy2," +            //0xa560
    "," +                     //0xa561
    "__GMMakeGrpBitmap," +    //0xa562
    "__GMDrawScript2," +      //0xa563
    "__GMRecordVer," +        //0xa564
    "__GMForeRGB," +          //0xa565
    "__GMBackRGB," +          //0xa566
    "__GMRecEnv," +           //0xa567
    "__GMRecPalet," +         //0xa568
    "__GMFillRImg2," +        //0xa569
    "__GMFillImg2," +         //0xa56a
    "__GMSetPutID," +         //0xa56b
    "__GMMakePalet," +        //0xa56c
    "__GMFontRealSize," +     //0xa56d
    "__GMGetCPDFInfo," +      //0xa56e
    "__GMGetCPDFList," +      //0xa56f
    "__GMScanScript," +       //0xa570
    "__GMGetGSInfo," +        //0xa571
    "__GMMovePoly," +         //0xa572
    "__GMSlidePoly," +        //0xa573
    "__GMNewBits2," +         //0xa574
    "," +                     //0xa575
    "," +                     //0xa576
    "," +                     //0xa577
    "," +                     //0xa578
    "," +                     //0xa579
    "," +                     //0xa57a
    "," +                     //0xa57b
    "," +                     //0xa57c
    "," +                     //0xa57d
    "," +                     //0xa57e
    "," +                     //0xa57f
    "," +                     //0xa580
    "," +                     //0xa581
    "," +                     //0xa582
    "," +                     //0xa583
    "," +                     //0xa584
    "," +                     //0xa585
    "," +                     //0xa586
    "," +                     //0xa587
    "," +                     //0xa588
    "," +                     //0xa589
    "," +                     //0xa58a
    "," +                     //0xa58b
    "," +                     //0xa58c
    "," +                     //0xa58d
    "," +                     //0xa58e
    "," +                     //0xa58f
    "," +                     //0xa590
    "," +                     //0xa591
    "," +                     //0xa592
    "," +                     //0xa593
    "," +                     //0xa594
    "," +                     //0xa595
    "," +                     //0xa596
    "," +                     //0xa597
    "," +                     //0xa598
    "," +                     //0xa599
    "," +                     //0xa59a
    "," +                     //0xa59b
    "," +                     //0xa59c
    "," +                     //0xa59d
    "," +                     //0xa59e
    "," +                     //0xa59f
    "," +                     //0xa5a0
    "," +                     //0xa5a1
    "," +                     //0xa5a2
    "," +                     //0xa5a3
    "," +                     //0xa5a4
    "," +                     //0xa5a5
    "," +                     //0xa5a6
    "," +                     //0xa5a7
    "," +                     //0xa5a8
    "," +                     //0xa5a9
    "," +                     //0xa5aa
    "," +                     //0xa5ab
    "," +                     //0xa5ac
    "," +                     //0xa5ad
    "," +                     //0xa5ae
    "," +                     //0xa5af
    "," +                     //0xa5b0
    "," +                     //0xa5b1
    "," +                     //0xa5b2
    "," +                     //0xa5b3
    "," +                     //0xa5b4
    "," +                     //0xa5b5
    "," +                     //0xa5b6
    "," +                     //0xa5b7
    "," +                     //0xa5b8
    "," +                     //0xa5b9
    "," +                     //0xa5ba
    "," +                     //0xa5bb
    "," +                     //0xa5bc
    "," +                     //0xa5bd
    "," +                     //0xa5be
    "," +                     //0xa5bf
    "," +                     //0xa5c0
    "," +                     //0xa5c1
    "," +                     //0xa5c2
    "," +                     //0xa5c3
    "," +                     //0xa5c4
    "," +                     //0xa5c5
    "," +                     //0xa5c6
    "," +                     //0xa5c7
    "," +                     //0xa5c8
    "," +                     //0xa5c9
    "," +                     //0xa5ca
    "," +                     //0xa5cb
    "," +                     //0xa5cc
    "," +                     //0xa5cd
    "," +                     //0xa5ce
    "," +                     //0xa5cf
    "," +                     //0xa5d0
    "," +                     //0xa5d1
    "," +                     //0xa5d2
    "," +                     //0xa5d3
    "," +                     //0xa5d4
    "," +                     //0xa5d5
    "," +                     //0xa5d6
    "," +                     //0xa5d7
    "," +                     //0xa5d8
    "," +                     //0xa5d9
    "," +                     //0xa5da
    "," +                     //0xa5db
    "," +                     //0xa5dc
    "," +                     //0xa5dd
    "," +                     //0xa5de
    "," +                     //0xa5df
    "," +                     //0xa5e0
    "," +                     //0xa5e1
    "," +                     //0xa5e2
    "," +                     //0xa5e3
    "," +                     //0xa5e4
    "," +                     //0xa5e5
    "," +                     //0xa5e6
    "," +                     //0xa5e7
    "," +                     //0xa5e8
    "," +                     //0xa5e9
    "," +                     //0xa5ea
    "," +                     //0xa5eb
    "," +                     //0xa5ec
    "," +                     //0xa5ed
    "," +                     //0xa5ee
    "," +                     //0xa5ef
    "__SXInitSemaphore," +    //0xa5f0
    "__SXAddSemaphore," +     //0xa5f1
    "__SXDelSemaphore," +     //0xa5f2
    "__SXFindSemaphore," +    //0xa5f3
    "," +                     //0xa5f4
    "," +                     //0xa5f5
    "," +                     //0xa5f6
    "," +                     //0xa5f7
    "," +                     //0xa5f8
    "," +                     //0xa5f9
    "," +                     //0xa5fa
    "," +                     //0xa5fb
    "," +                     //0xa5fc
    "," +                     //0xa5fd
    "," +                     //0xa5fe
    "," +                     //0xa5ff
    "__CLInit," +             //0xa600
    "__CLTini," +             //0xa601
    "__CLNewPalet," +         //0xa602
    "__CLRefer," +            //0xa603
    "__CLDupDevicePalet," +   //0xa604
    "__CLDisposePalet," +     //0xa605
    "__CLSetCInfo," +         //0xa606
    "__CLGetCInfo," +         //0xa607
    "__CLAlloc," +            //0xa608
    "__CLAllocOne," +         //0xa609
    "__CLFree," +             //0xa60a
    "__CLFreeOne," +          //0xa60b
    "__CLActive," +           //0xa60c
    "__CLRealloc," +          //0xa60d
    "__CLLinkPalet," +        //0xa60e
    "__CLUnlinkPalet," +      //0xa60f
    "__CLSetDeviceMode," +    //0xa610
    "__CLGetDeviceMode," +    //0xa611
    "__CLGetDevice," +        //0xa612
    "," +                     //0xa613
    "__CLDelDevice," +        //0xa614
    "__CLSetDeviceRGB," +     //0xa615
    "__CLDupPalet," +         //0xa616
    "__CLCopyPalet," +        //0xa617
    "__CLSetPickEntry," +     //0xa618
    "__CLValueToRGB," +       //0xa619
    "__CLRGBToValue," +       //0xa61a
    "__CLRefer2," +           //0xa61b
    "__CLLoadText," +         //0xa61c
    "__CLValueToPalet," +     //0xa61d
    "__CLPaletToValue," +     //0xa61e
    "__CLSetScanEntry," +     //0xa61f
    "," +                     //0xa620
    "," +                     //0xa621
    "," +                     //0xa622
    "," +                     //0xa623
    "," +                     //0xa624
    "," +                     //0xa625
    "," +                     //0xa626
    "," +                     //0xa627
    "," +                     //0xa628
    "," +                     //0xa629
    "," +                     //0xa62a
    "," +                     //0xa62b
    "," +                     //0xa62c
    "," +                     //0xa62d
    "," +                     //0xa62e
    "," +                     //0xa62f
    "," +                     //0xa630
    "," +                     //0xa631
    "," +                     //0xa632
    "," +                     //0xa633
    "," +                     //0xa634
    "," +                     //0xa635
    "," +                     //0xa636
    "," +                     //0xa637
    "," +                     //0xa638
    "," +                     //0xa639
    "," +                     //0xa63a
    "," +                     //0xa63b
    "," +                     //0xa63c
    "," +                     //0xa63d
    "," +                     //0xa63e
    "," +                     //0xa63f
    "," +                     //0xa640
    "," +                     //0xa641
    "," +                     //0xa642
    "," +                     //0xa643
    "," +                     //0xa644
    "," +                     //0xa645
    "," +                     //0xa646
    "," +                     //0xa647
    "," +                     //0xa648
    "," +                     //0xa649
    "," +                     //0xa64a
    "," +                     //0xa64b
    "," +                     //0xa64c
    "," +                     //0xa64d
    "," +                     //0xa64e
    "," +                     //0xa64f
    "," +                     //0xa650
    "," +                     //0xa651
    "," +                     //0xa652
    "," +                     //0xa653
    "," +                     //0xa654
    "," +                     //0xa655
    "," +                     //0xa656
    "," +                     //0xa657
    "," +                     //0xa658
    "," +                     //0xa659
    "," +                     //0xa65a
    "," +                     //0xa65b
    "," +                     //0xa65c
    "," +                     //0xa65d
    "," +                     //0xa65e
    "," +                     //0xa65f
    "," +                     //0xa660
    "," +                     //0xa661
    "," +                     //0xa662
    "," +                     //0xa663
    "," +                     //0xa664
    "," +                     //0xa665
    "," +                     //0xa666
    "," +                     //0xa667
    "," +                     //0xa668
    "," +                     //0xa669
    "," +                     //0xa66a
    "," +                     //0xa66b
    "," +                     //0xa66c
    "," +                     //0xa66d
    "," +                     //0xa66e
    "," +                     //0xa66f
    "," +                     //0xa670
    "," +                     //0xa671
    "," +                     //0xa672
    "," +                     //0xa673
    "," +                     //0xa674
    "," +                     //0xa675
    "," +                     //0xa676
    "," +                     //0xa677
    "," +                     //0xa678
    "," +                     //0xa679
    "," +                     //0xa67a
    "," +                     //0xa67b
    "," +                     //0xa67c
    "," +                     //0xa67d
    "," +                     //0xa67e
    "," +                     //0xa67f
    "," +                     //0xa680
    "," +                     //0xa681
    "," +                     //0xa682
    "," +                     //0xa683
    "," +                     //0xa684
    "," +                     //0xa685
    "," +                     //0xa686
    "," +                     //0xa687
    "," +                     //0xa688
    "," +                     //0xa689
    "," +                     //0xa68a
    "," +                     //0xa68b
    "," +                     //0xa68c
    "," +                     //0xa68d
    "," +                     //0xa68e
    "," +                     //0xa68f
    "," +                     //0xa690
    "," +                     //0xa691
    "," +                     //0xa692
    "," +                     //0xa693
    "," +                     //0xa694
    "," +                     //0xa695
    "," +                     //0xa696
    "," +                     //0xa697
    "," +                     //0xa698
    "," +                     //0xa699
    "," +                     //0xa69a
    "," +                     //0xa69b
    "," +                     //0xa69c
    "," +                     //0xa69d
    "," +                     //0xa69e
    "," +                     //0xa69f
    "," +                     //0xa6a0
    "," +                     //0xa6a1
    "," +                     //0xa6a2
    "," +                     //0xa6a3
    "," +                     //0xa6a4
    "," +                     //0xa6a5
    "," +                     //0xa6a6
    "," +                     //0xa6a7
    "," +                     //0xa6a8
    "," +                     //0xa6a9
    "," +                     //0xa6aa
    "," +                     //0xa6ab
    "," +                     //0xa6ac
    "," +                     //0xa6ad
    "," +                     //0xa6ae
    "," +                     //0xa6af
    "," +                     //0xa6b0
    "," +                     //0xa6b1
    "," +                     //0xa6b2
    "," +                     //0xa6b3
    "," +                     //0xa6b4
    "," +                     //0xa6b5
    "," +                     //0xa6b6
    "," +                     //0xa6b7
    "," +                     //0xa6b8
    "," +                     //0xa6b9
    "," +                     //0xa6ba
    "," +                     //0xa6bb
    "," +                     //0xa6bc
    "," +                     //0xa6bd
    "," +                     //0xa6be
    "," +                     //0xa6bf
    "," +                     //0xa6c0
    "," +                     //0xa6c1
    "," +                     //0xa6c2
    "," +                     //0xa6c3
    "," +                     //0xa6c4
    "," +                     //0xa6c5
    "," +                     //0xa6c6
    "," +                     //0xa6c7
    "," +                     //0xa6c8
    "," +                     //0xa6c9
    "," +                     //0xa6ca
    "," +                     //0xa6cb
    "," +                     //0xa6cc
    "," +                     //0xa6cd
    "," +                     //0xa6ce
    "," +                     //0xa6cf
    "," +                     //0xa6d0
    "," +                     //0xa6d1
    "," +                     //0xa6d2
    "," +                     //0xa6d3
    "," +                     //0xa6d4
    "," +                     //0xa6d5
    "," +                     //0xa6d6
    "," +                     //0xa6d7
    "," +                     //0xa6d8
    "," +                     //0xa6d9
    "," +                     //0xa6da
    "," +                     //0xa6db
    "," +                     //0xa6dc
    "," +                     //0xa6dd
    "," +                     //0xa6de
    "," +                     //0xa6df
    "," +                     //0xa6e0
    "," +                     //0xa6e1
    "," +                     //0xa6e2
    "," +                     //0xa6e3
    "," +                     //0xa6e4
    "," +                     //0xa6e5
    "," +                     //0xa6e6
    "," +                     //0xa6e7
    "," +                     //0xa6e8
    "," +                     //0xa6e9
    "," +                     //0xa6ea
    "," +                     //0xa6eb
    "," +                     //0xa6ec
    "," +                     //0xa6ed
    "," +                     //0xa6ee
    "," +                     //0xa6ef
    "," +                     //0xa6f0
    "," +                     //0xa6f1
    "," +                     //0xa6f2
    "," +                     //0xa6f3
    "," +                     //0xa6f4
    "," +                     //0xa6f5
    "," +                     //0xa6f6
    "," +                     //0xa6f7
    "," +                     //0xa6f8
    "," +                     //0xa6f9
    "," +                     //0xa6fa
    "," +                     //0xa6fb
    "," +                     //0xa6fc
    "," +                     //0xa6fd
    "," +                     //0xa6fe
    "," +                     //0xa6ff
    "__VMInit," +             //0xa700
    "__VMTini," +             //0xa701
    "," +                     //0xa702
    "," +                     //0xa703
    "," +                     //0xa704
    "," +                     //0xa705
    "," +                     //0xa706
    "," +                     //0xa707
    "," +                     //0xa708
    "," +                     //0xa709
    "," +                     //0xa70a
    "," +                     //0xa70b
    "," +                     //0xa70c
    "," +                     //0xa70d
    "," +                     //0xa70e
    "," +                     //0xa70f
    "__VMExpand," +           //0xa710
    "__VMCompress," +         //0xa711
    "__VMExpDirect," +        //0xa712
    "__VMGetInfo," +          //0xa713
    "__VMRscInfo," +          //0xa714
    "__VMRscHdlGet," +        //0xa715
    "__VMSetCurrentID," +     //0xa716
    "__VMGetCurrentID," +     //0xa717
    "__VMGetPalette," +       //0xa718
    "," +                     //0xa719
    "," +                     //0xa71a
    "," +                     //0xa71b
    "," +                     //0xa71c
    "," +                     //0xa71d
    "," +                     //0xa71e
    "," +                     //0xa71f
    "," +                     //0xa720
    "," +                     //0xa721
    "," +                     //0xa722
    "," +                     //0xa723
    "," +                     //0xa724
    "," +                     //0xa725
    "," +                     //0xa726
    "," +                     //0xa727
    "," +                     //0xa728
    "," +                     //0xa729
    "," +                     //0xa72a
    "," +                     //0xa72b
    "," +                     //0xa72c
    "," +                     //0xa72d
    "," +                     //0xa72e
    "," +                     //0xa72f
    "__VMSetAnim," +          //0xa730
    "__VMGetAnim," +          //0xa731
    "__VMSetParam," +         //0xa732
    "__VMGetParam," +         //0xa733
    "__VMCreate," +           //0xa734
    "__VMCreateF," +          //0xa735
    "__VMOpen," +             //0xa736
    "__VMClose," +            //0xa737
    "__VMDispose," +          //0xa738
    "__VMRegistSample," +     //0xa739
    "__VMDeleteSample," +     //0xa73a
    "__VMReferSample," +      //0xa73b
    "__VMGetSample," +        //0xa73c
    "__VMGetBits," +          //0xa73d
    "__VMInsertFrame," +      //0xa73e
    "__VMDeleteFrame," +      //0xa73f
    "__VMTimeToFrame," +      //0xa740
    "__VMFrameToSample," +    //0xa741
    "__VMTrans," +            //0xa742
    "__VMPlay," +             //0xa743
    "__VMEvent," +            //0xa744
    "__VMStop," +             //0xa745
    "__VMPause," +            //0xa746
    "__VMUpdate," +           //0xa747
    "__VMSetUser," +          //0xa748
    "__VMGetUser," +          //0xa749
    "__VMDisplay," +          //0xa74a
    "," +                     //0xa74b
    "," +                     //0xa74c
    "," +                     //0xa74d
    "," +                     //0xa74e
    "," +                     //0xa74f
    "," +                     //0xa750
    "," +                     //0xa751
    "," +                     //0xa752
    "," +                     //0xa753
    "," +                     //0xa754
    "," +                     //0xa755
    "," +                     //0xa756
    "," +                     //0xa757
    "," +                     //0xa758
    "," +                     //0xa759
    "," +                     //0xa75a
    "," +                     //0xa75b
    "," +                     //0xa75c
    "," +                     //0xa75d
    "," +                     //0xa75e
    "," +                     //0xa75f
    "," +                     //0xa760
    "," +                     //0xa761
    "," +                     //0xa762
    "," +                     //0xa763
    "," +                     //0xa764
    "," +                     //0xa765
    "," +                     //0xa766
    "," +                     //0xa767
    "," +                     //0xa768
    "," +                     //0xa769
    "," +                     //0xa76a
    "," +                     //0xa76b
    "," +                     //0xa76c
    "," +                     //0xa76d
    "," +                     //0xa76e
    "," +                     //0xa76f
    "," +                     //0xa770
    "," +                     //0xa771
    "," +                     //0xa772
    "," +                     //0xa773
    "," +                     //0xa774
    "," +                     //0xa775
    "," +                     //0xa776
    "," +                     //0xa777
    "," +                     //0xa778
    "," +                     //0xa779
    "," +                     //0xa77a
    "," +                     //0xa77b
    "," +                     //0xa77c
    "," +                     //0xa77d
    "," +                     //0xa77e
    "," +                     //0xa77f
    "," +                     //0xa780
    "," +                     //0xa781
    "," +                     //0xa782
    "," +                     //0xa783
    "," +                     //0xa784
    "," +                     //0xa785
    "," +                     //0xa786
    "," +                     //0xa787
    "," +                     //0xa788
    "," +                     //0xa789
    "," +                     //0xa78a
    "," +                     //0xa78b
    "," +                     //0xa78c
    "," +                     //0xa78d
    "," +                     //0xa78e
    "," +                     //0xa78f
    "," +                     //0xa790
    "," +                     //0xa791
    "," +                     //0xa792
    "," +                     //0xa793
    "," +                     //0xa794
    "," +                     //0xa795
    "," +                     //0xa796
    "," +                     //0xa797
    "," +                     //0xa798
    "," +                     //0xa799
    "," +                     //0xa79a
    "," +                     //0xa79b
    "," +                     //0xa79c
    "," +                     //0xa79d
    "," +                     //0xa79e
    "," +                     //0xa79f
    "," +                     //0xa7a0
    "," +                     //0xa7a1
    "," +                     //0xa7a2
    "," +                     //0xa7a3
    "," +                     //0xa7a4
    "," +                     //0xa7a5
    "," +                     //0xa7a6
    "," +                     //0xa7a7
    "," +                     //0xa7a8
    "," +                     //0xa7a9
    "," +                     //0xa7aa
    "," +                     //0xa7ab
    "," +                     //0xa7ac
    "," +                     //0xa7ad
    "," +                     //0xa7ae
    "," +                     //0xa7af
    "," +                     //0xa7b0
    "," +                     //0xa7b1
    "," +                     //0xa7b2
    "," +                     //0xa7b3
    "," +                     //0xa7b4
    "," +                     //0xa7b5
    "," +                     //0xa7b6
    "," +                     //0xa7b7
    "," +                     //0xa7b8
    "," +                     //0xa7b9
    "," +                     //0xa7ba
    "," +                     //0xa7bb
    "," +                     //0xa7bc
    "," +                     //0xa7bd
    "," +                     //0xa7be
    "," +                     //0xa7bf
    "," +                     //0xa7c0
    "," +                     //0xa7c1
    "," +                     //0xa7c2
    "," +                     //0xa7c3
    "," +                     //0xa7c4
    "," +                     //0xa7c5
    "," +                     //0xa7c6
    "," +                     //0xa7c7
    "," +                     //0xa7c8
    "," +                     //0xa7c9
    "," +                     //0xa7ca
    "," +                     //0xa7cb
    "," +                     //0xa7cc
    "," +                     //0xa7cd
    "," +                     //0xa7ce
    "," +                     //0xa7cf
    "," +                     //0xa7d0
    "," +                     //0xa7d1
    "," +                     //0xa7d2
    "," +                     //0xa7d3
    "," +                     //0xa7d4
    "," +                     //0xa7d5
    "," +                     //0xa7d6
    "," +                     //0xa7d7
    "," +                     //0xa7d8
    "," +                     //0xa7d9
    "," +                     //0xa7da
    "," +                     //0xa7db
    "," +                     //0xa7dc
    "," +                     //0xa7dd
    "," +                     //0xa7de
    "," +                     //0xa7df
    "," +                     //0xa7e0
    "," +                     //0xa7e1
    "," +                     //0xa7e2
    "," +                     //0xa7e3
    "," +                     //0xa7e4
    "," +                     //0xa7e5
    "," +                     //0xa7e6
    "," +                     //0xa7e7
    "," +                     //0xa7e8
    "," +                     //0xa7e9
    "," +                     //0xa7ea
    "," +                     //0xa7eb
    "," +                     //0xa7ec
    "," +                     //0xa7ed
    "," +                     //0xa7ee
    "," +                     //0xa7ef
    "," +                     //0xa7f0
    "," +                     //0xa7f1
    "," +                     //0xa7f2
    "," +                     //0xa7f3
    "," +                     //0xa7f4
    "," +                     //0xa7f5
    "," +                     //0xa7f6
    "," +                     //0xa7f7
    "," +                     //0xa7f8
    "," +                     //0xa7f9
    "," +                     //0xa7fa
    "," +                     //0xa7fb
    "," +                     //0xa7fc
    "," +                     //0xa7fd
    "," +                     //0xa7fe
    ""                        //0xa7ff
    ).split (",", 2048);  //limitを書かないと末尾の空文字列が削除されて配列が短くなり末尾の要素を参照できなくなる

  public static int disPC;  //逆アセンブル用のpc
  public static int disPC0;

  public static int disOC;  //オペコード

  public static int disSupervisor;  //0=ユーザコード,0以外=スーパーバイザコード

  //disStatus
  //  最後に逆アセンブルした命令の種類
  //  DIS_ALWAYS_BRANCH     空行あり  完全分岐命令(RTE,RTD,RTS,RTR,JMP,BRA,_ABORTJOB,_EXIT,_EXIT2,FBRA)。常に分岐して戻ってこない
  //  DIS_SOMETIMES_BRANCH  空行なし  条件分岐命令(DBcc,Bcc,PDBcc,PBcc,FDBcc,FBcc)。分岐したときは戻ってこない
  //  DIS_CALL_SUBROUTINE   空行なし  サブルーチンを呼び出す命令(BSR,JSR)。分岐するがほとんどの場合は戻ってくる
  public static final int DIS_ALWAYS_BRANCH    = 1;
  public static final int DIS_SOMETIMES_BRANCH = 2;
  public static final int DIS_CALL_SUBROUTINE  = 4;
  public static int disStatus;  //最後に逆アセンブルした命令の種類


  //プログラムの範囲
  public static int disProgramHead;  //プログラムの先頭。0=なし
  public static int disProgramTail;  //プログラムの末尾
  public static int disProgramMode;  //アドレスとオフセット。0=アドレスのみ,1=オフセットのみ,2=両方

  //disProgramHex8 (sb, a)
  public static StringBuilder disProgramHex8 (StringBuilder sb, int a) {
    //アドレス
    //  アドレスのみまたは両方のとき
    //    '$'とアドレスを表示する
    if (disProgramMode == 0 ||  //アドレスのみまたは
        disProgramMode == 2) {  //両方のとき
      XEiJ.fmtHex8 (sb.append ('$'), a);  //'$'とアドレスを表示する
    }
    //オフセット
    //  オフセットのみまたは両方のとき
    //    プログラムがありかつ先頭が0でなくかつ範囲内のとき
    //      両方のとき
    //        ':'を表示する
    //      'L'とオフセットを表示する
    //    プログラムがないまたは先頭が0または範囲外のとき
    //      オフセットのみのとき
    //        '$'とアドレスを表示する
    if (disProgramMode == 1 ||  //オフセットのみまたは
        disProgramMode == 2) {  //両方のとき
      int index = LabeledAddress.lblGetIndex (a);
      if (index != -1 &&  //プログラムがありかつ
          LabeledAddress.lblLastGetHead != 0 &&  //先頭が0でなくかつ
          disProgramHead - 256 <= a && a <= disProgramTail + 32768) {  //範囲内のとき
        if (disProgramMode == 2) {  //両方のとき
          sb.append (':');  //':'を表示する
        }
        XEiJ.fmtHex6 (sb.append ('L'), a - disProgramHead);  //'L'とオフセットを表示する
      } else {  //プログラムがないまたは先頭が0または範囲外のとき
        if (disProgramMode == 1) {  //オフセットのみのとき
          XEiJ.fmtHex8 (sb.append ('$'), a);  //'$'とアドレスを表示する
        }
      }
    }
    return sb;
  }  //disProgramHex8


  //sb = disDisassemble (sb, address, supervisor)
  //sb = disDisassemble (sb, address, supervisor, head, tail, mode)
  //  1命令逆アセンブルする
  public static StringBuilder disDisassemble (StringBuilder sb, int address, int supervisor) {
    return disDisassemble (sb, address, supervisor, 0, 0, 0);
  }
  public static StringBuilder disDisassemble (StringBuilder sb, int address, int supervisor, int head, int tail, int mode) {
    disPC = address;
    disSupervisor = supervisor;
    disProgramHead = head;
    disProgramTail = tail;
    disProgramMode = mode;
    disStatus = 0;
    disPC0 = disPC;
    disOC = MC68060.mmuPeekWordZeroCode (disPC, disSupervisor);  //第1オペコード。必ずゼロ拡張すること
    disPC += 2;
    switch (disOC >>> 6) {  //第1オペコードの上位10ビット。disOCはゼロ拡張されているので0b1111_111_111&を省略

      //                                     PRIVILEGED?|CCin |CCout|ADDRESSING|
      //                                          MPU | |XNZVC|XNZVC|DAM+-WXZPI|       FORMAT
      //----------------------------------------------+-+-----+-----+----------+--------------------
      //ORI.B #<data>,CCR                       012346|-|*****|*****|          |0000_000_000_111_100-{data}
      //ORI.B #<data>,<ea>                      012346|-|-UUUU|-**00|D M+-WXZ  |0000_000_000_mmm_rrr-{data}
    case 0b0000_000_000: return disOriByte (sb);

      //ORI.W #<data>,SR                        012346|P|*****|*****|          |0000_000_001_111_100-{data}
      //ORI.W #<data>,<ea>                      012346|-|-UUUU|-**00|D M+-WXZ  |0000_000_001_mmm_rrr-{data}
    case 0b0000_000_001: return disOriWord (sb);

      //ORI.L #<data>,<ea>                      012346|-|-UUUU|-**00|D M+-WXZ  |0000_000_010_mmm_rrr-{data}
    case 0b0000_000_010: return disOriLong (sb);

      //BITREV.L Dr                             ------|-|-----|-----|          |0000_000_011_000_rrr    (ISA_C)
      //CMP2.B <ea>,Rn                          --234S|-|-UUUU|-U*U*|  M  WXZP |0000_000_011_mmm_rrr-rnnn_000_000_000_000
      //CHK2.B <ea>,Rn                          --234S|-|-UUUU|-U*U*|  M  WXZP |0000_000_011_mmm_rrr-rnnn_100_000_000_000
    case 0b0000_000_011: return disCmp2Chk2Byte (sb);

      //ANDI.B #<data>,CCR                      012346|-|*****|*****|          |0000_001_000_111_100-{data}
      //ANDI.B #<data>,<ea>                     012346|-|-UUUU|-**00|D M+-WXZ  |0000_001_000_mmm_rrr-{data}
    case 0b0000_001_000: return disAndiByte (sb);

      //ANDI.W #<data>,SR                       012346|P|*****|*****|          |0000_001_001_111_100-{data}
      //ANDI.W #<data>,<ea>                     012346|-|-UUUU|-**00|D M+-WXZ  |0000_001_001_mmm_rrr-{data}
    case 0b0000_001_001: return disAndiWord (sb);

      //ANDI.L #<data>,<ea>                     012346|-|-UUUU|-**00|D M+-WXZ  |0000_001_010_mmm_rrr-{data}
    case 0b0000_001_010: return disAndiLong (sb);

      //BYTEREV.L Dr                            ------|-|-----|-----|          |0000_001_011_000_rrr    (ISA_C)
      //CMP2.W <ea>,Rn                          --234S|-|-UUUU|-U*U*|  M  WXZP |0000_001_011_mmm_rrr-rnnn_000_000_000_000
      //CHK2.W <ea>,Rn                          --234S|-|-UUUU|-U*U*|  M  WXZP |0000_001_011_mmm_rrr-rnnn_100_000_000_000
    case 0b0000_001_011: return disCmp2Chk2Word (sb);

      //SUBI.B #<data>,<ea>                     012346|-|UUUUU|*****|D M+-WXZ  |0000_010_000_mmm_rrr-{data}
    case 0b0000_010_000: return disSubiByte (sb);

      //SUBI.W #<data>,<ea>                     012346|-|UUUUU|*****|D M+-WXZ  |0000_010_001_mmm_rrr-{data}
    case 0b0000_010_001: return disSubiWord (sb);

      //SUBI.L #<data>,<ea>                     012346|-|UUUUU|*****|D M+-WXZ  |0000_010_010_mmm_rrr-{data}
    case 0b0000_010_010: return disSubiLong (sb);

      //FF1.L Dr                                ------|-|-UUUU|-**00|          |0000_010_011_000_rrr    (ISA_C)
      //CMP2.L <ea>,Rn                          --234S|-|-UUUU|-U*U*|  M  WXZP |0000_010_011_mmm_rrr-rnnn_000_000_000_000
      //CHK2.L <ea>,Rn                          --234S|-|-UUUU|-U*U*|  M  WXZP |0000_010_011_mmm_rrr-rnnn_100_000_000_000
    case 0b0000_010_011: return disCmp2Chk2Long (sb);

      //ADDI.B #<data>,<ea>                     012346|-|UUUUU|*****|D M+-WXZ  |0000_011_000_mmm_rrr-{data}
    case 0b0000_011_000: return disAddiByte (sb);

      //ADDI.W #<data>,<ea>                     012346|-|UUUUU|*****|D M+-WXZ  |0000_011_001_mmm_rrr-{data}
    case 0b0000_011_001: return disAddiWord (sb);

      //ADDI.L #<data>,<ea>                     012346|-|UUUUU|*****|D M+-WXZ  |0000_011_010_mmm_rrr-{data}
    case 0b0000_011_010: return disAddiLong (sb);

      //RTM Rn                                  --2---|-|UUUUU|*****|          |0000_011_011_00n_nnn
      //CALLM #<data>,<ea>                      --2---|-|-----|-----|  M  WXZP |0000_011_011_mmm_rrr-0000_000_0dd_ddd_ddd
    case 0b0000_011_011: return disCallm (sb);

      //BTST.L #<data>,Dr                       012346|-|--U--|--*--|          |0000_100_000_000_rrr-{data}
      //BTST.B #<data>,<ea>                     012346|-|--U--|--*--|  M+-WXZP |0000_100_000_mmm_rrr-{data}
    case 0b0000_100_000: return disBtstImm (sb);

      //BCHG.L #<data>,Dr                       012346|-|--U--|--*--|          |0000_100_001_000_rrr-{data}
      //BCHG.B #<data>,<ea>                     012346|-|--U--|--*--|  M+-WXZ  |0000_100_001_mmm_rrr-{data}
    case 0b0000_100_001: return disBchgImm (sb);

      //BCLR.L #<data>,Dr                       012346|-|--U--|--*--|          |0000_100_010_000_rrr-{data}
      //BCLR.B #<data>,<ea>                     012346|-|--U--|--*--|  M+-WXZ  |0000_100_010_mmm_rrr-{data}
    case 0b0000_100_010: return disBclrImm (sb);

      //BSET.L #<data>,Dr                       012346|-|--U--|--*--|          |0000_100_011_000_rrr-{data}
      //BSET.B #<data>,<ea>                     012346|-|--U--|--*--|  M+-WXZ  |0000_100_011_mmm_rrr-{data}
    case 0b0000_100_011: return disBsetImm (sb);

      //EORI.B #<data>,CCR                      012346|-|*****|*****|          |0000_101_000_111_100-{data}
      //EORI.B #<data>,<ea>                     012346|-|-UUUU|-**00|D M+-WXZ  |0000_101_000_mmm_rrr-{data}
    case 0b0000_101_000: return disEoriByte (sb);

      //EORI.W #<data>,SR                       012346|P|*****|*****|          |0000_101_001_111_100-{data}
      //EORI.W #<data>,<ea>                     012346|-|-UUUU|-**00|D M+-WXZ  |0000_101_001_mmm_rrr-{data}
    case 0b0000_101_001: return disEoriWord (sb);

      //EORI.L #<data>,<ea>                     012346|-|-UUUU|-**00|D M+-WXZ  |0000_101_010_mmm_rrr-{data}
    case 0b0000_101_010: return disEoriLong (sb);

      //CAS.B Dc,Du,<ea>                        --2346|-|-UUUU|-****|  M+-WXZ  |0000_101_011_mmm_rrr-0000_000_uuu_000_ccc
    case 0b0000_101_011: return disCasByte (sb);

      //CMPI.B #<data>,<ea>                     012346|-|-UUUU|-****|D M+-WXZ  |0000_110_000_mmm_rrr-{data}
      //CMPI.B #<data>,<ea>                     --2346|-|-UUUU|-****|        P |0000_110_000_mmm_rrr-{data}
    case 0b0000_110_000: return disCmpiByte (sb);

      //CMPI.W #<data>,<ea>                     012346|-|-UUUU|-****|D M+-WXZ  |0000_110_001_mmm_rrr-{data}
      //CMPI.W #<data>,<ea>                     --2346|-|-UUUU|-****|        P |0000_110_001_mmm_rrr-{data}
    case 0b0000_110_001: return disCmpiWord (sb);

      //CMPI.L #<data>,<ea>                     012346|-|-UUUU|-****|D M+-WXZ  |0000_110_010_mmm_rrr-{data}
      //CMPI.L #<data>,<ea>                     --2346|-|-UUUU|-****|        P |0000_110_010_mmm_rrr-{data}
    case 0b0000_110_010: return disCmpiLong (sb);

      //CAS2.W Dc1:Dc2,Du1:Du2,(Rn1):(Rn2)      --234S|-|-UUUU|-****|          |0000_110_011_111_100-rnnn_000_uuu_000_ccc(1)-rnnn_000_uuu_000_ccc(2)
      //CAS.W Dc,Du,<ea>                        --2346|-|-UUUU|-****|  M+-WXZ  |0000_110_011_mmm_rrr-0000_000_uuu_000_ccc       (68060 software emulate misaligned <ea>)
    case 0b0000_110_011: return disCasWord (sb);

      //MOVES.B <ea>,Rn                         -12346|P|-----|-----|  M+-WXZ  |0000_111_000_mmm_rrr-rnnn_000_000_000_000
      //MOVES.B Rn,<ea>                         -12346|P|-----|-----|  M+-WXZ  |0000_111_000_mmm_rrr-rnnn_100_000_000_000
    case 0b0000_111_000: return disMovesByte (sb);

      //MOVES.W <ea>,Rn                         -12346|P|-----|-----|  M+-WXZ  |0000_111_001_mmm_rrr-rnnn_000_000_000_000
      //MOVES.W Rn,<ea>                         -12346|P|-----|-----|  M+-WXZ  |0000_111_001_mmm_rrr-rnnn_100_000_000_000
    case 0b0000_111_001: return disMovesWord (sb);

      //MOVES.L <ea>,Rn                         -12346|P|-----|-----|  M+-WXZ  |0000_111_010_mmm_rrr-rnnn_000_000_000_000
      //MOVES.L Rn,<ea>                         -12346|P|-----|-----|  M+-WXZ  |0000_111_010_mmm_rrr-rnnn_100_000_000_000
    case 0b0000_111_010:  return disMovesLong (sb);

      //CAS2.L Dc1:Dc2,Du1:Du2,(Rn1):(Rn2)      --234S|-|-UUUU|-****|          |0000_111_011_111_100-rnnn_000_uuu_000_ccc(1)-rnnn_000_uuu_000_ccc(2)
      //CAS.L Dc,Du,<ea>                        --2346|-|-UUUU|-****|  M+-WXZ  |0000_111_011_mmm_rrr-0000_000_uuu_000_ccc       (68060 software emulate misaligned <ea>)
    case 0b0000_111_011: return disCasLong (sb);

      //BTST.L Dq,Dr                            012346|-|--U--|--*--|          |0000_qqq_100_000_rrr
      //MOVEP.W (d16,Ar),Dq                     01234S|-|-----|-----|          |0000_qqq_100_001_rrr-{data}
      //BTST.B Dq,<ea>                          012346|-|--U--|--*--|  M+-WXZPI|0000_qqq_100_mmm_rrr
    case 0b0000_000_100:
    case 0b0000_001_100:
    case 0b0000_010_100:
    case 0b0000_011_100:
    case 0b0000_100_100:
    case 0b0000_101_100:
    case 0b0000_110_100:
    case 0b0000_111_100: return disBtstReg (sb);

      //BCHG.L Dq,Dr                            012346|-|--U--|--*--|          |0000_qqq_101_000_rrr
      //MOVEP.L (d16,Ar),Dq                     01234S|-|-----|-----|          |0000_qqq_101_001_rrr-{data}
      //BCHG.B Dq,<ea>                          012346|-|--U--|--*--|  M+-WXZ  |0000_qqq_101_mmm_rrr
    case 0b0000_000_101:
    case 0b0000_001_101:
    case 0b0000_010_101:
    case 0b0000_011_101:
    case 0b0000_100_101:
    case 0b0000_101_101:
    case 0b0000_110_101:
    case 0b0000_111_101: return disBchgReg (sb);

      //BCLR.L Dq,Dr                            012346|-|--U--|--*--|          |0000_qqq_110_000_rrr
      //MOVEP.W Dq,(d16,Ar)                     01234S|-|-----|-----|          |0000_qqq_110_001_rrr-{data}
      //BCLR.B Dq,<ea>                          012346|-|--U--|--*--|  M+-WXZ  |0000_qqq_110_mmm_rrr
    case 0b0000_000_110:
    case 0b0000_001_110:
    case 0b0000_010_110:
    case 0b0000_011_110:
    case 0b0000_100_110:
    case 0b0000_101_110:
    case 0b0000_110_110:
    case 0b0000_111_110: return disBclrReg (sb);

      //BSET.L Dq,Dr                            012346|-|--U--|--*--|          |0000_qqq_111_000_rrr
      //MOVEP.L Dq,(d16,Ar)                     01234S|-|-----|-----|          |0000_qqq_111_001_rrr-{data}
      //BSET.B Dq,<ea>                          012346|-|--U--|--*--|  M+-WXZ  |0000_qqq_111_mmm_rrr
    case 0b0000_000_111:
    case 0b0000_001_111:
    case 0b0000_010_111:
    case 0b0000_011_111:
    case 0b0000_100_111:
    case 0b0000_101_111:
    case 0b0000_110_111:
    case 0b0000_111_111: return disBsetReg (sb);

      //MOVE.B <ea>,Dq                          012346|-|-UUUU|-**00|D M+-WXZPI|0001_qqq_000_mmm_rrr
    case 0b0001_000_000:
    case 0b0001_001_000:
    case 0b0001_010_000:
    case 0b0001_011_000:
    case 0b0001_100_000:
    case 0b0001_101_000:
    case 0b0001_110_000:
    case 0b0001_111_000: return disMoveToRegByte (sb);

      //case 0b0001_000_001:
      //case 0b0001_001_001:
      //case 0b0001_010_001:
      //case 0b0001_011_001:
      //case 0b0001_100_001:
      //case 0b0001_101_001:
      //case 0b0001_110_001:
      //case 0b0001_111_001:

      //MOVE.B <ea>,<nnnqqq>                    012346|-|-UUUU|-**00|D M+-WXZPI|0001_qqq_nnn_mmm_rrr        (nnnqqq:M+-WXZ)
    case 0b0001_000_010:
    case 0b0001_000_011:
    case 0b0001_000_100:
    case 0b0001_000_101:
    case 0b0001_000_110:
    case 0b0001_000_111:
    case 0b0001_001_010:
    case 0b0001_001_011:
    case 0b0001_001_100:
    case 0b0001_001_101:
    case 0b0001_001_110:
    case 0b0001_001_111:
    case 0b0001_010_010:
    case 0b0001_010_011:
    case 0b0001_010_100:
    case 0b0001_010_101:
    case 0b0001_010_110:
    case 0b0001_011_010:
    case 0b0001_011_011:
    case 0b0001_011_100:
    case 0b0001_011_101:
    case 0b0001_011_110:
    case 0b0001_100_010:
    case 0b0001_100_011:
    case 0b0001_100_100:
    case 0b0001_100_101:
    case 0b0001_100_110:
    case 0b0001_101_010:
    case 0b0001_101_011:
    case 0b0001_101_100:
    case 0b0001_101_101:
    case 0b0001_101_110:
    case 0b0001_110_010:
    case 0b0001_110_011:
    case 0b0001_110_100:
    case 0b0001_110_101:
    case 0b0001_110_110:
    case 0b0001_111_010:
    case 0b0001_111_011:
    case 0b0001_111_100:
    case 0b0001_111_101:
    case 0b0001_111_110: return disMoveToMemByte (sb);

      //case 0b0001_010_111:
      //case 0b0001_011_111:
      //case 0b0001_100_111:
      //case 0b0001_101_111:
      //case 0b0001_110_111:
      //case 0b0001_111_111:

      //MOVE.L <ea>,Dq                          012346|-|-UUUU|-**00|DAM+-WXZPI|0010_qqq_000_mmm_rrr
    case 0b0010_000_000:
    case 0b0010_001_000:
    case 0b0010_010_000:
    case 0b0010_011_000:
    case 0b0010_100_000:
    case 0b0010_101_000:
    case 0b0010_110_000:
    case 0b0010_111_000: return disMoveToRegLong (sb);

      //MOVEA.L <ea>,Aq                         012346|-|-----|-----|DAM+-WXZPI|0010_qqq_001_mmm_rrr
    case 0b0010_000_001:
    case 0b0010_001_001:
    case 0b0010_010_001:
    case 0b0010_011_001:
    case 0b0010_100_001:
    case 0b0010_101_001:
    case 0b0010_110_001:
    case 0b0010_111_001: return disMoveaLong (sb);

      //MOVE.L <ea>,<nnnqqq>                    012346|-|-UUUU|-**00|DAM+-WXZPI|0010_qqq_nnn_mmm_rrr        (nnnqqq:M+-WXZ)
    case 0b0010_000_010:
    case 0b0010_000_011:
    case 0b0010_000_100:
    case 0b0010_000_101:
    case 0b0010_000_110:
    case 0b0010_000_111:
    case 0b0010_001_010:
    case 0b0010_001_011:
    case 0b0010_001_100:
    case 0b0010_001_101:
    case 0b0010_001_110:
    case 0b0010_001_111:
    case 0b0010_010_010:
    case 0b0010_010_011:
    case 0b0010_010_100:
    case 0b0010_010_101:
    case 0b0010_010_110:
    case 0b0010_011_010:
    case 0b0010_011_011:
    case 0b0010_011_100:
    case 0b0010_011_101:
    case 0b0010_011_110:
    case 0b0010_100_010:
    case 0b0010_100_011:
    case 0b0010_100_100:
    case 0b0010_100_101:
    case 0b0010_100_110:
    case 0b0010_101_010:
    case 0b0010_101_011:
    case 0b0010_101_100:
    case 0b0010_101_101:
    case 0b0010_101_110:
    case 0b0010_110_010:
    case 0b0010_110_011:
    case 0b0010_110_100:
    case 0b0010_110_101:
    case 0b0010_110_110:
    case 0b0010_111_010:
    case 0b0010_111_011:
    case 0b0010_111_100:
    case 0b0010_111_101:
    case 0b0010_111_110: return disMoveToMemLong (sb);

      //case 0b0010_010_111:
      //case 0b0010_011_111:
      //case 0b0010_100_111:
      //case 0b0010_101_111:
      //case 0b0010_110_111:
      //case 0b0010_111_111:

      //MOVE.W <ea>,Dq                          012346|-|-UUUU|-**00|DAM+-WXZPI|0011_qqq_000_mmm_rrr
    case 0b0011_000_000:
    case 0b0011_001_000:
    case 0b0011_010_000:
    case 0b0011_011_000:
    case 0b0011_100_000:
    case 0b0011_101_000:
    case 0b0011_110_000:
    case 0b0011_111_000: return disMoveToRegWord (sb);

      //MOVEA.W <ea>,Aq                         012346|-|-----|-----|DAM+-WXZPI|0011_qqq_001_mmm_rrr
    case 0b0011_000_001:
    case 0b0011_001_001:
    case 0b0011_010_001:
    case 0b0011_011_001:
    case 0b0011_100_001:
    case 0b0011_101_001:
    case 0b0011_110_001:
    case 0b0011_111_001: return disMoveaWord (sb);

      //MOVE.W <ea>,<nnnqqq>                    012346|-|-UUUU|-**00|DAM+-WXZPI|0011_qqq_nnn_mmm_rrr        (nnnqqq:M+-WXZ)
    case 0b0011_000_010:
    case 0b0011_000_011:
    case 0b0011_000_100:
    case 0b0011_000_101:
    case 0b0011_000_110:
    case 0b0011_000_111:
    case 0b0011_001_010:
    case 0b0011_001_011:
    case 0b0011_001_100:
    case 0b0011_001_101:
    case 0b0011_001_110:
    case 0b0011_001_111:
    case 0b0011_010_010:
    case 0b0011_010_011:
    case 0b0011_010_100:
    case 0b0011_010_101:
    case 0b0011_010_110:
    case 0b0011_011_010:
    case 0b0011_011_011:
    case 0b0011_011_100:
    case 0b0011_011_101:
    case 0b0011_011_110:
    case 0b0011_100_010:
    case 0b0011_100_011:
    case 0b0011_100_100:
    case 0b0011_100_101:
    case 0b0011_100_110:
    case 0b0011_101_010:
    case 0b0011_101_011:
    case 0b0011_101_100:
    case 0b0011_101_101:
    case 0b0011_101_110:
    case 0b0011_110_010:
    case 0b0011_110_011:
    case 0b0011_110_100:
    case 0b0011_110_101:
    case 0b0011_110_110:
    case 0b0011_111_010:
    case 0b0011_111_011:
    case 0b0011_111_100:
    case 0b0011_111_101:
    case 0b0011_111_110: return disMoveToMemWord (sb);

      //case 0b0011_010_111:
      //case 0b0011_011_111:
      //case 0b0011_100_111:
      //case 0b0011_101_111:
      //case 0b0011_110_111:
      //case 0b0011_111_111:

      //NEGX.B <ea>                             012346|-|*UUUU|*****|D M+-WXZ  |0100_000_000_mmm_rrr
    case 0b0100_000_000: return disNegxByte (sb);

      //NEGX.W <ea>                             012346|-|*UUUU|*****|D M+-WXZ  |0100_000_001_mmm_rrr
    case 0b0100_000_001: return disNegxWord (sb);

      //NEGX.L <ea>                             012346|-|*UUUU|*****|D M+-WXZ  |0100_000_010_mmm_rrr
    case 0b0100_000_010: return disNegxLong (sb);

      //STRLDSR.W #<data>                       ------|P|*****|*****|          |0100_000_011_100_111-0100_011_011_111_100-{data}    (ISA_C)
      //MOVE.W SR,<ea>                          0-----|-|*****|-----|D M+-WXZ  |0100_000_011_mmm_rrr        (68000 and 68008 read before move)
      //MOVE.W SR,<ea>                          -12346|P|*****|-----|D M+-WXZ  |0100_000_011_mmm_rrr
    case 0b0100_000_011: return disMoveFromSR (sb);

      //CLR.B <ea>                              012346|-|-UUUU|-0100|D M+-WXZ  |0100_001_000_mmm_rrr        (68000 and 68008 read before clear)
    case 0b0100_001_000: return disClrByte (sb);

      //CLR.W <ea>                              012346|-|-UUUU|-0100|D M+-WXZ  |0100_001_001_mmm_rrr        (68000 and 68008 read before clear)
    case 0b0100_001_001: return disClrWord (sb);

      //CLR.L <ea>                              012346|-|-UUUU|-0100|D M+-WXZ  |0100_001_010_mmm_rrr        (68000 and 68008 read before clear)
    case 0b0100_001_010: return disClrLong (sb);

      //MOVE.W CCR,<ea>                         -12346|-|*****|-----|D M+-WXZ  |0100_001_011_mmm_rrr
    case 0b0100_001_011: return disMoveFromCCR (sb);

      //NEG.B <ea>                              012346|-|UUUUU|*****|D M+-WXZ  |0100_010_000_mmm_rrr
    case 0b0100_010_000: return disNegByte (sb);

      //NEG.W <ea>                              012346|-|UUUUU|*****|D M+-WXZ  |0100_010_001_mmm_rrr
    case 0b0100_010_001: return disNegWord (sb);

      //NEG.L <ea>                              012346|-|UUUUU|*****|D M+-WXZ  |0100_010_010_mmm_rrr
    case 0b0100_010_010: return disNegLong (sb);

      //MOVE.W <ea>,CCR                         012346|-|UUUUU|*****|D M+-WXZPI|0100_010_011_mmm_rrr
    case 0b0100_010_011: return disMoveToCCR (sb);

      //NOT.B <ea>                              012346|-|-UUUU|-**00|D M+-WXZ  |0100_011_000_mmm_rrr
    case 0b0100_011_000: return disNotByte (sb);

      //NOT.W <ea>                              012346|-|-UUUU|-**00|D M+-WXZ  |0100_011_001_mmm_rrr
    case 0b0100_011_001: return disNotWord (sb);

      //NOT.L <ea>                              012346|-|-UUUU|-**00|D M+-WXZ  |0100_011_010_mmm_rrr
    case 0b0100_011_010: return disNotLong (sb);

      //MOVE.W <ea>,SR                          012346|P|UUUUU|*****|D M+-WXZPI|0100_011_011_mmm_rrr
    case 0b0100_011_011: return disMoveToSR (sb);

      //LINK.L Ar,#<data>                       --2346|-|-----|-----|          |0100_100_000_001_rrr-{data}
      //NBCD.B <ea>                             012346|-|UUUUU|*U*U*|D M+-WXZ  |0100_100_000_mmm_rrr
    case 0b0100_100_000: return disNbcd (sb);

      //SWAP.W Dr                               012346|-|-UUUU|-**00|          |0100_100_001_000_rrr
      //BKPT #<data>                            -12346|-|-----|-----|          |0100_100_001_001_ddd
      //PEA.L <ea>                              012346|-|-----|-----|  M  WXZP |0100_100_001_mmm_rrr
    case 0b0100_100_001: return disPea (sb);

      //EXT.W Dr                                012346|-|-UUUU|-**00|          |0100_100_010_000_rrr
      //MOVEM.W <list>,<ea>                     012346|-|-----|-----|  M -WXZ  |0100_100_010_mmm_rrr-llllllllllllllll
    case 0b0100_100_010: return disMovemToMemWord (sb);

      //EXT.L Dr                                012346|-|-UUUU|-**00|          |0100_100_011_000_rrr
      //MOVEM.L <list>,<ea>                     012346|-|-----|-----|  M -WXZ  |0100_100_011_mmm_rrr-llllllllllllllll
    case 0b0100_100_011: return disMovemToMemLong (sb);

      //TST.B <ea>                              012346|-|-UUUU|-**00|D M+-WXZ  |0100_101_000_mmm_rrr
      //TST.B <ea>                              --2346|-|-UUUU|-**00|        PI|0100_101_000_mmm_rrr
    case 0b0100_101_000: return disTstByte (sb);

      //TST.W <ea>                              012346|-|-UUUU|-**00|D M+-WXZ  |0100_101_001_mmm_rrr
      //TST.W <ea>                              --2346|-|-UUUU|-**00| A      PI|0100_101_001_mmm_rrr
    case 0b0100_101_001: return disTstWord (sb);

      //TST.L <ea>                              012346|-|-UUUU|-**00|D M+-WXZ  |0100_101_010_mmm_rrr
      //TST.L <ea>                              --2346|-|-UUUU|-**00| A      PI|0100_101_010_mmm_rrr
    case 0b0100_101_010: return disTstLong (sb);

      //HALT                                    ------|P|-----|-----|          |0100_101_011_001_000    (ISA_A)
      //PULSE                                   ------|-|-----|-----|          |0100_101_011_001_100    (ISA_A)
      //ILLEGAL                                 012346|-|-----|-----|          |0100_101_011_111_100
      //TAS.B <ea>                              012346|-|-UUUU|-**00|D M+-WXZ  |0100_101_011_mmm_rrr
    case 0b0100_101_011: return disTas (sb);

      //MULU.L <ea>,Dl                          --2346|-|-UUUU|-***0|D M+-WXZPI|0100_110_000_mmm_rrr-0lll_000_000_000_hhh       (h is not used)
      //MULU.L <ea>,Dh:Dl                       --234S|-|-UUUU|-***0|D M+-WXZPI|0100_110_000_mmm_rrr-0lll_010_000_000_hhh       (if h=l then result is not defined)
      //MULS.L <ea>,Dl                          --2346|-|-UUUU|-***0|D M+-WXZPI|0100_110_000_mmm_rrr-0lll_100_000_000_hhh       (h is not used)
      //MULS.L <ea>,Dh:Dl                       --234S|-|-UUUU|-***0|D M+-WXZPI|0100_110_000_mmm_rrr-0lll_110_000_000_hhh       (if h=l then result is not defined)
    case 0b0100_110_000: return disMuluMulsLong (sb);

      //DIVU.L <ea>,Dq                          --2346|-|-UUUU|-***0|D M+-WXZPI|0100_110_001_mmm_rrr-0qqq_000_000_000_qqq
      //DIVUL.L <ea>,Dr:Dq                      --2346|-|-UUUU|-***0|D M+-WXZPI|0100_110_001_mmm_rrr-0qqq_000_000_000_rrr       (q is not equal to r)
      //REMU.L <ea>,Dr:Dq                       ------|-|-UUUU|-***0|D M+-W    |0100_110_001_mmm_rrr-0qqq_000_000_000_rrr       (ISA_A, q is not equal to r)
      //DIVU.L <ea>,Dr:Dq                       --234S|-|-UUUU|-***0|D M+-WXZPI|0100_110_001_mmm_rrr-0qqq_010_000_000_rrr       (q is not equal to r)
      //DIVS.L <ea>,Dq                          --2346|-|-UUUU|-***0|D M+-WXZPI|0100_110_001_mmm_rrr-0qqq_100_000_000_qqq
      //DIVSL.L <ea>,Dr:Dq                      --2346|-|-UUUU|-***0|D M+-WXZPI|0100_110_001_mmm_rrr-0qqq_100_000_000_rrr       (q is not equal to r)
      //REMS.L <ea>,Dr:Dq                       ------|-|-UUUU|-***0|D M+-W    |0100_110_001_mmm_rrr-0qqq_100_000_000_rrr       (ISA_A, q is not equal to r)
      //DIVS.L <ea>,Dr:Dq                       --234S|-|-UUUU|-***0|D M+-WXZPI|0100_110_001_mmm_rrr-0qqq_110_000_000_rrr       (q is not equal to r)
    case 0b0100_110_001: return disDivuDivsLong (sb);

      //SATS.L Dr                               ------|-|-UUUU|-**00|          |0100_110_010_000_rrr    (ISA_B)
      //MOVEM.W <ea>,<list>                     012346|-|-----|-----|  M+ WXZP |0100_110_010_mmm_rrr-llllllllllllllll
    case 0b0100_110_010: return disMovemToRegWord (sb);

      //MOVEM.L <ea>,<list>                     012346|-|-----|-----|  M+ WXZP |0100_110_011_mmm_rrr-llllllllllllllll
    case 0b0100_110_011: return disMovemToRegLong (sb);

      //HFSBOOT                                 012346|-|-----|-----|          |0100_111_000_000_000
      //HFSINST                                 012346|-|-----|-----|          |0100_111_000_000_001
      //HFSSTR                                  012346|-|-----|-----|          |0100_111_000_000_010
      //HFSINT                                  012346|-|-----|-----|          |0100_111_000_000_011
      //EMXNOP                                  012346|-|-----|-----|          |0100_111_000_000_100
    case 0b0100_111_000: return disEmx (sb);

      //TRAP #<vector>                          012346|-|-----|-----|          |0100_111_001_00v_vvv
      //LINK.W Ar,#<data>                       012346|-|-----|-----|          |0100_111_001_010_rrr-{data}
      //UNLK Ar                                 012346|-|-----|-----|          |0100_111_001_011_rrr
      //MOVE.L Ar,USP                           012346|P|-----|-----|          |0100_111_001_100_rrr
      //MOVE.L USP,Ar                           012346|P|-----|-----|          |0100_111_001_101_rrr
      //RESET                                   012346|P|-----|-----|          |0100_111_001_110_000
      //NOP                                     012346|-|-----|-----|          |0100_111_001_110_001
      //STOP #<data>                            012346|P|UUUUU|*****|          |0100_111_001_110_010-{data}
      //RTE                                     012346|P|UUUUU|*****|          |0100_111_001_110_011
      //RTD #<data>                             -12346|-|-----|-----|          |0100_111_001_110_100-{data}
      //RTS                                     012346|-|-----|-----|          |0100_111_001_110_101
      //TRAPV                                   012346|-|---*-|-----|          |0100_111_001_110_110
      //RTR                                     012346|-|UUUUU|*****|          |0100_111_001_110_111
      //MOVEC.L Rc,Rn                           -12346|P|-----|-----|          |0100_111_001_111_010-rnnn_ccc_ccc_ccc_ccc
      //MOVEC.L Rn,Rc                           -12346|P|-----|-----|          |0100_111_001_111_011-rnnn_ccc_ccc_ccc_ccc
    case 0b0100_111_001: return disMisc (sb);

      //JSR <ea>                                012346|-|-----|-----|  M  WXZP |0100_111_010_mmm_rrr
    case 0b0100_111_010: return disJsr (sb);

      //JMP <ea>                                012346|-|-----|-----|  M  WXZP |0100_111_011_mmm_rrr
    case 0b0100_111_011: return disJmp (sb);

      //CHK.L <ea>,Dq                           --2346|-|-UUUU|-*UUU|D M+-WXZPI|0100_qqq_100_mmm_rrr
    case 0b0100_000_100:
    case 0b0100_001_100:
    case 0b0100_010_100:
    case 0b0100_011_100:
    case 0b0100_100_100:
    case 0b0100_101_100:
    case 0b0100_110_100:
    case 0b0100_111_100: return disChkLong (sb);

      //case 0b0100_000_101:
      //case 0b0100_001_101:
      //case 0b0100_010_101:
      //case 0b0100_011_101:
      //case 0b0100_100_101:
      //case 0b0100_101_101:
      //case 0b0100_110_101:
      //case 0b0100_111_101:

      //CHK.W <ea>,Dq                           012346|-|-UUUU|-*UUU|D M+-WXZPI|0100_qqq_110_mmm_rrr
    case 0b0100_000_110:
    case 0b0100_001_110:
    case 0b0100_010_110:
    case 0b0100_011_110:
    case 0b0100_100_110:
    case 0b0100_101_110:
    case 0b0100_110_110:
    case 0b0100_111_110: return disChkWord (sb);

      //EXTB.L Dr                               --2346|-|-UUUU|-**00|          |0100_100_111_000_rrr
      //LEA.L <ea>,Aq                           012346|-|-----|-----|  M  WXZP |0100_qqq_111_mmm_rrr
    case 0b0100_000_111:
    case 0b0100_001_111:
    case 0b0100_010_111:
    case 0b0100_011_111:
    case 0b0100_100_111:
    case 0b0100_101_111:
    case 0b0100_110_111:
    case 0b0100_111_111: return disLea (sb);

      //ADDQ.B #<data>,<ea>                     012346|-|UUUUU|*****|D M+-WXZ  |0101_qqq_000_mmm_rrr
    case 0b0101_000_000:
    case 0b0101_001_000:
    case 0b0101_010_000:
    case 0b0101_011_000:
    case 0b0101_100_000:
    case 0b0101_101_000:
    case 0b0101_110_000:
    case 0b0101_111_000: return disAddqByte (sb);

      //ADDQ.W #<data>,Ar                       012346|-|-----|-----| A        |0101_qqq_001_001_rrr
      //ADDQ.W #<data>,<ea>                     012346|-|UUUUU|*****|D M+-WXZ  |0101_qqq_001_mmm_rrr
    case 0b0101_000_001:
    case 0b0101_001_001:
    case 0b0101_010_001:
    case 0b0101_011_001:
    case 0b0101_100_001:
    case 0b0101_101_001:
    case 0b0101_110_001:
    case 0b0101_111_001: return disAddqWord (sb);

      //ADDQ.L #<data>,Ar                       012346|-|-----|-----| A        |0101_qqq_010_001_rrr
      //ADDQ.L #<data>,<ea>                     012346|-|UUUUU|*****|D M+-WXZ  |0101_qqq_010_mmm_rrr
    case 0b0101_000_010:
    case 0b0101_001_010:
    case 0b0101_010_010:
    case 0b0101_011_010:
    case 0b0101_100_010:
    case 0b0101_101_010:
    case 0b0101_110_010:
    case 0b0101_111_010: return disAddqLong (sb);

      //SUBQ.B #<data>,<ea>                     012346|-|UUUUU|*****|D M+-WXZ  |0101_qqq_100_mmm_rrr
    case 0b0101_000_100:
    case 0b0101_001_100:
    case 0b0101_010_100:
    case 0b0101_011_100:
    case 0b0101_100_100:
    case 0b0101_101_100:
    case 0b0101_110_100:
    case 0b0101_111_100: return disSubqByte (sb);

      //SUBQ.W #<data>,Ar                       012346|-|-----|-----| A        |0101_qqq_101_001_rrr
      //SUBQ.W #<data>,<ea>                     012346|-|UUUUU|*****|D M+-WXZ  |0101_qqq_101_mmm_rrr
    case 0b0101_000_101:
    case 0b0101_001_101:
    case 0b0101_010_101:
    case 0b0101_011_101:
    case 0b0101_100_101:
    case 0b0101_101_101:
    case 0b0101_110_101:
    case 0b0101_111_101: return disSubqWord (sb);

      //SUBQ.L #<data>,Ar                       012346|-|-----|-----| A        |0101_qqq_110_001_rrr
      //SUBQ.L #<data>,<ea>                     012346|-|UUUUU|*****|D M+-WXZ  |0101_qqq_110_mmm_rrr
    case 0b0101_000_110:
    case 0b0101_001_110:
    case 0b0101_010_110:
    case 0b0101_011_110:
    case 0b0101_100_110:
    case 0b0101_101_110:
    case 0b0101_110_110:
    case 0b0101_111_110: return disSubqLong (sb);

      //DBRA.W Dr,<label>                       012346|-|-----|-----|          |0101_000_111_001_rrr-{offset}
      //DBcc.W Dr,<label>                       012346|-|-****|-----|          |0101_ccc_c11_001_rrr-{offset}
      //TRAPcc.W #<data>                        --2346|-|-****|-----|          |0101_ccc_c11_111_010-{data}
      //TRAPcc.L #<data>                        --2346|-|-****|-----|          |0101_ccc_c11_111_011-{data}
      //TRAPcc                                  --2346|-|-****|-----|          |0101_ccc_c11_111_100
      //Scc.B <ea>                              012346|-|-****|-----|D M+-WXZ  |0101_ccc_c11_mmm_rrr
    case 0b0101_000_011:
    case 0b0101_000_111:
    case 0b0101_001_011:
    case 0b0101_001_111:
    case 0b0101_010_011:
    case 0b0101_010_111:
    case 0b0101_011_011:
    case 0b0101_011_111:
    case 0b0101_100_011:
    case 0b0101_100_111:
    case 0b0101_101_011:
    case 0b0101_101_111:
    case 0b0101_110_011:
    case 0b0101_110_111:
    case 0b0101_111_011:
    case 0b0101_111_111: return disScc (sb);

      //BRA.W <label>                           012346|-|-----|-----|          |0110_000_000_000_000-{offset}
      //BRA.L <label>                           --2346|-|-----|-----|          |0110_000_011_111_111-{offset}
      //BRA.S <label>                           012346|-|-----|-----|          |0110_000_0aa_aaa_aaa        (a is not equal to 0,-1)
    case 0b0110_000_000:
    case 0b0110_000_001:
    case 0b0110_000_010:
    case 0b0110_000_011: return disBra (sb);

      //BSR.W <label>                           012346|-|-----|-----|          |0110_000_100_000_000-{offset}
      //BSR.L <label>                           --2346|-|-----|-----|          |0110_000_111_111_111-{offset}
      //BSR.S <label>                           012346|-|-----|-----|          |0110_000_1aa_aaa_aaa        (a is not equal to 0,-1)
    case 0b0110_000_100:
    case 0b0110_000_101:
    case 0b0110_000_110:
    case 0b0110_000_111: return disBsr (sb);

      //Bcc.W <label>                           012346|-|-****|-----|          |0110_ccc_c00_000_000-{offset}
      //Bcc.L <label>                           --2346|-|-****|-----|          |0110_ccc_c11_111_111-{offset}
      //Bcc.S <label>                           012346|-|-****|-----|          |0110_ccc_caa_aaa_aaa        (a is not equal to 0,-1)
    case 0b0110_001_000:
    case 0b0110_001_001:
    case 0b0110_001_010:
    case 0b0110_001_011:
    case 0b0110_001_100:
    case 0b0110_001_101:
    case 0b0110_001_110:
    case 0b0110_001_111:
    case 0b0110_010_000:
    case 0b0110_010_001:
    case 0b0110_010_010:
    case 0b0110_010_011:
    case 0b0110_010_100:
    case 0b0110_010_101:
    case 0b0110_010_110:
    case 0b0110_010_111:
    case 0b0110_011_000:
    case 0b0110_011_001:
    case 0b0110_011_010:
    case 0b0110_011_011:
    case 0b0110_011_100:
    case 0b0110_011_101:
    case 0b0110_011_110:
    case 0b0110_011_111:
    case 0b0110_100_000:
    case 0b0110_100_001:
    case 0b0110_100_010:
    case 0b0110_100_011:
    case 0b0110_100_100:
    case 0b0110_100_101:
    case 0b0110_100_110:
    case 0b0110_100_111:
    case 0b0110_101_000:
    case 0b0110_101_001:
    case 0b0110_101_010:
    case 0b0110_101_011:
    case 0b0110_101_100:
    case 0b0110_101_101:
    case 0b0110_101_110:
    case 0b0110_101_111:
    case 0b0110_110_000:
    case 0b0110_110_001:
    case 0b0110_110_010:
    case 0b0110_110_011:
    case 0b0110_110_100:
    case 0b0110_110_101:
    case 0b0110_110_110:
    case 0b0110_110_111:
    case 0b0110_111_000:
    case 0b0110_111_001:
    case 0b0110_111_010:
    case 0b0110_111_011:
    case 0b0110_111_100:
    case 0b0110_111_101:
    case 0b0110_111_110:
    case 0b0110_111_111: return disBcc (sb);

      //IOCS <name>                             012346|-|-UUUU|-**00|          |0111_000_0dd_ddd_ddd-0100_111_001_001_111
      //MOVEQ.L #<data>,Dq                      012346|-|-UUUU|-**00|          |0111_qqq_0dd_ddd_ddd
    case 0b0111_000_000:
    case 0b0111_000_001:
    case 0b0111_000_010:
    case 0b0111_000_011:
    case 0b0111_001_000:
    case 0b0111_001_001:
    case 0b0111_001_010:
    case 0b0111_001_011:
    case 0b0111_010_000:
    case 0b0111_010_001:
    case 0b0111_010_010:
    case 0b0111_010_011:
    case 0b0111_011_000:
    case 0b0111_011_001:
    case 0b0111_011_010:
    case 0b0111_011_011:
    case 0b0111_100_000:
    case 0b0111_100_001:
    case 0b0111_100_010:
    case 0b0111_100_011:
    case 0b0111_101_000:
    case 0b0111_101_001:
    case 0b0111_101_010:
    case 0b0111_101_011:
    case 0b0111_110_000:
    case 0b0111_110_001:
    case 0b0111_110_010:
    case 0b0111_110_011:
    case 0b0111_111_000:
    case 0b0111_111_001:
    case 0b0111_111_010:
    case 0b0111_111_011: return disMoveq (sb);

      //MVS.B <ea>,Dq                           ------|-|-UUUU|-**00|D M+-WXZPI|0111_qqq_100_mmm_rrr    (ISA_B)
    case 0b0111_000_100:
    case 0b0111_001_100:
    case 0b0111_010_100:
    case 0b0111_011_100:
    case 0b0111_100_100:
    case 0b0111_101_100:
    case 0b0111_110_100:
    case 0b0111_111_100: return disMvsByte (sb);

      //MVS.W <ea>,Dq                           ------|-|-UUUU|-**00|D M+-WXZPI|0111_qqq_101_mmm_rrr    (ISA_B)
    case 0b0111_000_101:
    case 0b0111_001_101:
    case 0b0111_010_101:
    case 0b0111_011_101:
    case 0b0111_100_101:
    case 0b0111_101_101:
    case 0b0111_110_101:
    case 0b0111_111_101: return disMvsWord (sb);

      //MVZ.B <ea>,Dq                           ------|-|-UUUU|-0*00|D M+-WXZPI|0111_qqq_110_mmm_rrr    (ISA_B)
    case 0b0111_000_110:
    case 0b0111_001_110:
    case 0b0111_010_110:
    case 0b0111_011_110:
    case 0b0111_100_110:
    case 0b0111_101_110:
    case 0b0111_110_110:
    case 0b0111_111_110: return disMvzByte (sb);

      //MVZ.W <ea>,Dq                           ------|-|-UUUU|-0*00|D M+-WXZPI|0111_qqq_111_mmm_rrr    (ISA_B)
    case 0b0111_000_111:
    case 0b0111_001_111:
    case 0b0111_010_111:
    case 0b0111_011_111:
    case 0b0111_100_111:
    case 0b0111_101_111:
    case 0b0111_110_111:
    case 0b0111_111_111: return disMvzWord (sb);

      //OR.B <ea>,Dq                            012346|-|-UUUU|-**00|D M+-WXZPI|1000_qqq_000_mmm_rrr
    case 0b1000_000_000:
    case 0b1000_001_000:
    case 0b1000_010_000:
    case 0b1000_011_000:
    case 0b1000_100_000:
    case 0b1000_101_000:
    case 0b1000_110_000:
    case 0b1000_111_000: return disOrToRegByte (sb);

      //OR.W <ea>,Dq                            012346|-|-UUUU|-**00|D M+-WXZPI|1000_qqq_001_mmm_rrr
    case 0b1000_000_001:
    case 0b1000_001_001:
    case 0b1000_010_001:
    case 0b1000_011_001:
    case 0b1000_100_001:
    case 0b1000_101_001:
    case 0b1000_110_001:
    case 0b1000_111_001: return disOrToRegWord (sb);

      //OR.L <ea>,Dq                            012346|-|-UUUU|-**00|D M+-WXZPI|1000_qqq_010_mmm_rrr
    case 0b1000_000_010:
    case 0b1000_001_010:
    case 0b1000_010_010:
    case 0b1000_011_010:
    case 0b1000_100_010:
    case 0b1000_101_010:
    case 0b1000_110_010:
    case 0b1000_111_010: return disOrToRegLong (sb);

      //DIVU.W <ea>,Dq                          012346|-|-UUUU|-***0|D M+-WXZPI|1000_qqq_011_mmm_rrr
    case 0b1000_000_011:
    case 0b1000_001_011:
    case 0b1000_010_011:
    case 0b1000_011_011:
    case 0b1000_100_011:
    case 0b1000_101_011:
    case 0b1000_110_011:
    case 0b1000_111_011: return disDivuWord (sb);

      //SBCD.B Dr,Dq                            012346|-|UUUUU|*U*U*|          |1000_qqq_100_000_rrr
      //SBCD.B -(Ar),-(Aq)                      012346|-|UUUUU|*U*U*|          |1000_qqq_100_001_rrr
      //OR.B Dq,<ea>                            012346|-|-UUUU|-**00|  M+-WXZ  |1000_qqq_100_mmm_rrr
    case 0b1000_000_100:
    case 0b1000_001_100:
    case 0b1000_010_100:
    case 0b1000_011_100:
    case 0b1000_100_100:
    case 0b1000_101_100:
    case 0b1000_110_100:
    case 0b1000_111_100: return disOrToMemByte (sb);

      //PACK Dr,Dq,#<data>                      --2346|-|-----|-----|          |1000_qqq_101_000_rrr-{data}
      //PACK -(Ar),-(Aq),#<data>                --2346|-|-----|-----|          |1000_qqq_101_001_rrr-{data}
      //OR.W Dq,<ea>                            012346|-|-UUUU|-**00|  M+-WXZ  |1000_qqq_101_mmm_rrr
    case 0b1000_000_101:
    case 0b1000_001_101:
    case 0b1000_010_101:
    case 0b1000_011_101:
    case 0b1000_100_101:
    case 0b1000_101_101:
    case 0b1000_110_101:
    case 0b1000_111_101: return disOrToMemWord (sb);

      //UNPK Dr,Dq,#<data>                      --2346|-|-----|-----|          |1000_qqq_110_000_rrr-{data}
      //UNPK -(Ar),-(Aq),#<data>                --2346|-|-----|-----|          |1000_qqq_110_001_rrr-{data}
      //OR.L Dq,<ea>                            012346|-|-UUUU|-**00|  M+-WXZ  |1000_qqq_110_mmm_rrr
    case 0b1000_000_110:
    case 0b1000_001_110:
    case 0b1000_010_110:
    case 0b1000_011_110:
    case 0b1000_100_110:
    case 0b1000_101_110:
    case 0b1000_110_110:
    case 0b1000_111_110: return disOrToMemLong (sb);

      //DIVS.W <ea>,Dq                          012346|-|-UUUU|-***0|D M+-WXZPI|1000_qqq_111_mmm_rrr
    case 0b1000_000_111:
    case 0b1000_001_111:
    case 0b1000_010_111:
    case 0b1000_011_111:
    case 0b1000_100_111:
    case 0b1000_101_111:
    case 0b1000_110_111:
    case 0b1000_111_111: return disDivsWord (sb);

      //SUB.B <ea>,Dq                           012346|-|UUUUU|*****|D M+-WXZPI|1001_qqq_000_mmm_rrr
    case 0b1001_000_000:
    case 0b1001_001_000:
    case 0b1001_010_000:
    case 0b1001_011_000:
    case 0b1001_100_000:
    case 0b1001_101_000:
    case 0b1001_110_000:
    case 0b1001_111_000: return disSubToRegByte (sb);

      //SUB.W <ea>,Dq                           012346|-|UUUUU|*****|DAM+-WXZPI|1001_qqq_001_mmm_rrr
    case 0b1001_000_001:
    case 0b1001_001_001:
    case 0b1001_010_001:
    case 0b1001_011_001:
    case 0b1001_100_001:
    case 0b1001_101_001:
    case 0b1001_110_001:
    case 0b1001_111_001: return disSubToRegWord (sb);

      //SUB.L <ea>,Dq                           012346|-|UUUUU|*****|DAM+-WXZPI|1001_qqq_010_mmm_rrr
    case 0b1001_000_010:
    case 0b1001_001_010:
    case 0b1001_010_010:
    case 0b1001_011_010:
    case 0b1001_100_010:
    case 0b1001_101_010:
    case 0b1001_110_010:
    case 0b1001_111_010: return disSubToRegLong (sb);

      //SUBA.W <ea>,Aq                          012346|-|-----|-----|DAM+-WXZPI|1001_qqq_011_mmm_rrr
    case 0b1001_000_011:
    case 0b1001_001_011:
    case 0b1001_010_011:
    case 0b1001_011_011:
    case 0b1001_100_011:
    case 0b1001_101_011:
    case 0b1001_110_011:
    case 0b1001_111_011: return disSubaWord (sb);

      //SUBX.B Dr,Dq                            012346|-|*UUUU|*****|          |1001_qqq_100_000_rrr
      //SUBX.B -(Ar),-(Aq)                      012346|-|*UUUU|*****|          |1001_qqq_100_001_rrr
      //SUB.B Dq,<ea>                           012346|-|UUUUU|*****|  M+-WXZ  |1001_qqq_100_mmm_rrr
    case 0b1001_000_100:
    case 0b1001_001_100:
    case 0b1001_010_100:
    case 0b1001_011_100:
    case 0b1001_100_100:
    case 0b1001_101_100:
    case 0b1001_110_100:
    case 0b1001_111_100: return disSubToMemByte (sb);

      //SUBX.W Dr,Dq                            012346|-|*UUUU|*****|          |1001_qqq_101_000_rrr
      //SUBX.W -(Ar),-(Aq)                      012346|-|*UUUU|*****|          |1001_qqq_101_001_rrr
      //SUB.W Dq,<ea>                           012346|-|UUUUU|*****|  M+-WXZ  |1001_qqq_101_mmm_rrr
    case 0b1001_000_101:
    case 0b1001_001_101:
    case 0b1001_010_101:
    case 0b1001_011_101:
    case 0b1001_100_101:
    case 0b1001_101_101:
    case 0b1001_110_101:
    case 0b1001_111_101: return disSubToMemWord (sb);

      //SUBX.L Dr,Dq                            012346|-|*UUUU|*****|          |1001_qqq_110_000_rrr
      //SUBX.L -(Ar),-(Aq)                      012346|-|*UUUU|*****|          |1001_qqq_110_001_rrr
      //SUB.L Dq,<ea>                           012346|-|UUUUU|*****|  M+-WXZ  |1001_qqq_110_mmm_rrr
    case 0b1001_000_110:
    case 0b1001_001_110:
    case 0b1001_010_110:
    case 0b1001_011_110:
    case 0b1001_100_110:
    case 0b1001_101_110:
    case 0b1001_110_110:
    case 0b1001_111_110: return disSubToMemLong (sb);

      //SUBA.L <ea>,Aq                          012346|-|-----|-----|DAM+-WXZPI|1001_qqq_111_mmm_rrr
    case 0b1001_000_111:
    case 0b1001_001_111:
    case 0b1001_010_111:
    case 0b1001_011_111:
    case 0b1001_100_111:
    case 0b1001_101_111:
    case 0b1001_110_111:
    case 0b1001_111_111: return disSubaLong (sb);

      //MOV3Q.L #<data>,<ea>                    ------|-|-UUUU|-**00|DAM+-WXZ  |1010_qqq_101_mmm_rrr    (ISA_B)
      //SXCALL <name>                           012346|-|-----|-----|          |1010_xxx_xxx_xxx_xxx
      //line 1010 emulator                      012346|-|-----|-----|          |1010_xxx_xxx_xxx_xxx
    case 0b1010_000_000:
    case 0b1010_000_001:
    case 0b1010_000_010:
    case 0b1010_000_011:
    case 0b1010_000_100:
    case 0b1010_000_101:
    case 0b1010_000_110:
    case 0b1010_000_111:
    case 0b1010_001_000:
    case 0b1010_001_001:
    case 0b1010_001_010:
    case 0b1010_001_011:
    case 0b1010_001_100:
    case 0b1010_001_101:
    case 0b1010_001_110:
    case 0b1010_001_111:
    case 0b1010_010_000:
    case 0b1010_010_001:
    case 0b1010_010_010:
    case 0b1010_010_011:
    case 0b1010_010_100:
    case 0b1010_010_101:
    case 0b1010_010_110:
    case 0b1010_010_111:
    case 0b1010_011_000:
    case 0b1010_011_001:
    case 0b1010_011_010:
    case 0b1010_011_011:
    case 0b1010_011_100:
    case 0b1010_011_101:
    case 0b1010_011_110:
    case 0b1010_011_111:
    case 0b1010_100_000:
    case 0b1010_100_001:
    case 0b1010_100_010:
    case 0b1010_100_011:
    case 0b1010_100_100:
    case 0b1010_100_101:
    case 0b1010_100_110:
    case 0b1010_100_111:
    case 0b1010_101_000:
    case 0b1010_101_001:
    case 0b1010_101_010:
    case 0b1010_101_011:
    case 0b1010_101_100:
    case 0b1010_101_101:
    case 0b1010_101_110:
    case 0b1010_101_111:
    case 0b1010_110_000:
    case 0b1010_110_001:
    case 0b1010_110_010:
    case 0b1010_110_011:
    case 0b1010_110_100:
    case 0b1010_110_101:
    case 0b1010_110_110:
    case 0b1010_110_111:
    case 0b1010_111_000:
    case 0b1010_111_001:
    case 0b1010_111_010:
    case 0b1010_111_011:
    case 0b1010_111_100:
    case 0b1010_111_101:
    case 0b1010_111_110:
    case 0b1010_111_111: return disSxcall (sb);

      //CMP.B <ea>,Dq                           012346|-|-UUUU|-****|D M+-WXZPI|1011_qqq_000_mmm_rrr
    case 0b1011_000_000:
    case 0b1011_001_000:
    case 0b1011_010_000:
    case 0b1011_011_000:
    case 0b1011_100_000:
    case 0b1011_101_000:
    case 0b1011_110_000:
    case 0b1011_111_000: return disCmpByte (sb);

      //CMP.W <ea>,Dq                           012346|-|-UUUU|-****|DAM+-WXZPI|1011_qqq_001_mmm_rrr
    case 0b1011_000_001:
    case 0b1011_001_001:
    case 0b1011_010_001:
    case 0b1011_011_001:
    case 0b1011_100_001:
    case 0b1011_101_001:
    case 0b1011_110_001:
    case 0b1011_111_001: return disCmpWord (sb);

      //CMP.L <ea>,Dq                           012346|-|-UUUU|-****|DAM+-WXZPI|1011_qqq_010_mmm_rrr
    case 0b1011_000_010:
    case 0b1011_001_010:
    case 0b1011_010_010:
    case 0b1011_011_010:
    case 0b1011_100_010:
    case 0b1011_101_010:
    case 0b1011_110_010:
    case 0b1011_111_010: return disCmpLong (sb);

      //CMPA.W <ea>,Aq                          012346|-|-UUUU|-****|DAM+-WXZPI|1011_qqq_011_mmm_rrr
    case 0b1011_000_011:
    case 0b1011_001_011:
    case 0b1011_010_011:
    case 0b1011_011_011:
    case 0b1011_100_011:
    case 0b1011_101_011:
    case 0b1011_110_011:
    case 0b1011_111_011: return disCmpaWord (sb);

      //CMPM.B (Ar)+,(Aq)+                      012346|-|-UUUU|-****|          |1011_qqq_100_001_rrr
      //EOR.B Dq,<ea>                           012346|-|-UUUU|-**00|D M+-WXZ  |1011_qqq_100_mmm_rrr
    case 0b1011_000_100:
    case 0b1011_001_100:
    case 0b1011_010_100:
    case 0b1011_011_100:
    case 0b1011_100_100:
    case 0b1011_101_100:
    case 0b1011_110_100:
    case 0b1011_111_100: return disEorToMemByte (sb);

      //CMPM.W (Ar)+,(Aq)+                      012346|-|-UUUU|-****|          |1011_qqq_101_001_rrr
      //EOR.W Dq,<ea>                           012346|-|-UUUU|-**00|D M+-WXZ  |1011_qqq_101_mmm_rrr
    case 0b1011_000_101:
    case 0b1011_001_101:
    case 0b1011_010_101:
    case 0b1011_011_101:
    case 0b1011_100_101:
    case 0b1011_101_101:
    case 0b1011_110_101:
    case 0b1011_111_101: return disEorToMemWord (sb);

      //CMPM.L (Ar)+,(Aq)+                      012346|-|-UUUU|-****|          |1011_qqq_110_001_rrr
      //EOR.L Dq,<ea>                           012346|-|-UUUU|-**00|D M+-WXZ  |1011_qqq_110_mmm_rrr
    case 0b1011_000_110:
    case 0b1011_001_110:
    case 0b1011_010_110:
    case 0b1011_011_110:
    case 0b1011_100_110:
    case 0b1011_101_110:
    case 0b1011_110_110:
    case 0b1011_111_110: return disEorToMemLong (sb);

      //CMPA.L <ea>,Aq                          012346|-|-UUUU|-****|DAM+-WXZPI|1011_qqq_111_mmm_rrr
    case 0b1011_000_111:
    case 0b1011_001_111:
    case 0b1011_010_111:
    case 0b1011_011_111:
    case 0b1011_100_111:
    case 0b1011_101_111:
    case 0b1011_110_111:
    case 0b1011_111_111: return disCmpaLong (sb);

      //AND.B <ea>,Dq                           012346|-|-UUUU|-**00|D M+-WXZPI|1100_qqq_000_mmm_rrr
    case 0b1100_000_000:
    case 0b1100_001_000:
    case 0b1100_010_000:
    case 0b1100_011_000:
    case 0b1100_100_000:
    case 0b1100_101_000:
    case 0b1100_110_000:
    case 0b1100_111_000: return disAndToRegByte (sb);

      //AND.W <ea>,Dq                           012346|-|-UUUU|-**00|D M+-WXZPI|1100_qqq_001_mmm_rrr
    case 0b1100_000_001:
    case 0b1100_001_001:
    case 0b1100_010_001:
    case 0b1100_011_001:
    case 0b1100_100_001:
    case 0b1100_101_001:
    case 0b1100_110_001:
    case 0b1100_111_001: return disAndToRegWord (sb);

      //AND.L <ea>,Dq                           012346|-|-UUUU|-**00|D M+-WXZPI|1100_qqq_010_mmm_rrr
    case 0b1100_000_010:
    case 0b1100_001_010:
    case 0b1100_010_010:
    case 0b1100_011_010:
    case 0b1100_100_010:
    case 0b1100_101_010:
    case 0b1100_110_010:
    case 0b1100_111_010: return disAndToRegLong (sb);

      //MULU.W <ea>,Dq                          012346|-|-UUUU|-***0|D M+-WXZPI|1100_qqq_011_mmm_rrr
    case 0b1100_000_011:
    case 0b1100_001_011:
    case 0b1100_010_011:
    case 0b1100_011_011:
    case 0b1100_100_011:
    case 0b1100_101_011:
    case 0b1100_110_011:
    case 0b1100_111_011: return disMuluWord (sb);

      //ABCD.B Dr,Dq                            012346|-|UUUUU|*U*U*|          |1100_qqq_100_000_rrr
      //ABCD.B -(Ar),-(Aq)                      012346|-|UUUUU|*U*U*|          |1100_qqq_100_001_rrr
      //AND.B Dq,<ea>                           012346|-|-UUUU|-**00|  M+-WXZ  |1100_qqq_100_mmm_rrr
    case 0b1100_000_100:
    case 0b1100_001_100:
    case 0b1100_010_100:
    case 0b1100_011_100:
    case 0b1100_100_100:
    case 0b1100_101_100:
    case 0b1100_110_100:
    case 0b1100_111_100: return disAndToMemByte (sb);

      //EXG.L Dq,Dr                             012346|-|-----|-----|          |1100_qqq_101_000_rrr
      //EXG.L Aq,Ar                             012346|-|-----|-----|          |1100_qqq_101_001_rrr
      //AND.W Dq,<ea>                           012346|-|-UUUU|-**00|  M+-WXZ  |1100_qqq_101_mmm_rrr
    case 0b1100_000_101:
    case 0b1100_001_101:
    case 0b1100_010_101:
    case 0b1100_011_101:
    case 0b1100_100_101:
    case 0b1100_101_101:
    case 0b1100_110_101:
    case 0b1100_111_101: return disAndToMemWord (sb);

      //EXG.L Dq,Ar                             012346|-|-----|-----|          |1100_qqq_110_001_rrr
      //AND.L Dq,<ea>                           012346|-|-UUUU|-**00|  M+-WXZ  |1100_qqq_110_mmm_rrr
    case 0b1100_000_110:
    case 0b1100_001_110:
    case 0b1100_010_110:
    case 0b1100_011_110:
    case 0b1100_100_110:
    case 0b1100_101_110:
    case 0b1100_110_110:
    case 0b1100_111_110: return disAndToMemLong (sb);

      //MULS.W <ea>,Dq                          012346|-|-UUUU|-***0|D M+-WXZPI|1100_qqq_111_mmm_rrr
    case 0b1100_000_111:
    case 0b1100_001_111:
    case 0b1100_010_111:
    case 0b1100_011_111:
    case 0b1100_100_111:
    case 0b1100_101_111:
    case 0b1100_110_111:
    case 0b1100_111_111: return disMulsWord (sb);

      //ADD.B <ea>,Dq                           012346|-|UUUUU|*****|D M+-WXZPI|1101_qqq_000_mmm_rrr
    case 0b1101_000_000:
    case 0b1101_001_000:
    case 0b1101_010_000:
    case 0b1101_011_000:
    case 0b1101_100_000:
    case 0b1101_101_000:
    case 0b1101_110_000:
    case 0b1101_111_000: return disAddToRegByte (sb);

      //ADD.W <ea>,Dq                           012346|-|UUUUU|*****|DAM+-WXZPI|1101_qqq_001_mmm_rrr
    case 0b1101_000_001:
    case 0b1101_001_001:
    case 0b1101_010_001:
    case 0b1101_011_001:
    case 0b1101_100_001:
    case 0b1101_101_001:
    case 0b1101_110_001:
    case 0b1101_111_001: return disAddToRegWord (sb);

      //ADD.L <ea>,Dq                           012346|-|UUUUU|*****|DAM+-WXZPI|1101_qqq_010_mmm_rrr
    case 0b1101_000_010:
    case 0b1101_001_010:
    case 0b1101_010_010:
    case 0b1101_011_010:
    case 0b1101_100_010:
    case 0b1101_101_010:
    case 0b1101_110_010:
    case 0b1101_111_010: return disAddToRegLong (sb);

      //ADDA.W <ea>,Aq                          012346|-|-----|-----|DAM+-WXZPI|1101_qqq_011_mmm_rrr
    case 0b1101_000_011:
    case 0b1101_001_011:
    case 0b1101_010_011:
    case 0b1101_011_011:
    case 0b1101_100_011:
    case 0b1101_101_011:
    case 0b1101_110_011:
    case 0b1101_111_011: return disAddaWord (sb);

      //ADDX.B Dr,Dq                            012346|-|*UUUU|*****|          |1101_qqq_100_000_rrr
      //ADDX.B -(Ar),-(Aq)                      012346|-|*UUUU|*****|          |1101_qqq_100_001_rrr
      //ADD.B Dq,<ea>                           012346|-|UUUUU|*****|  M+-WXZ  |1101_qqq_100_mmm_rrr
    case 0b1101_000_100:
    case 0b1101_001_100:
    case 0b1101_010_100:
    case 0b1101_011_100:
    case 0b1101_100_100:
    case 0b1101_101_100:
    case 0b1101_110_100:
    case 0b1101_111_100: return disAddToMemByte (sb);

      //ADDX.W Dr,Dq                            012346|-|*UUUU|*****|          |1101_qqq_101_000_rrr
      //ADDX.W -(Ar),-(Aq)                      012346|-|*UUUU|*****|          |1101_qqq_101_001_rrr
      //ADD.W Dq,<ea>                           012346|-|UUUUU|*****|  M+-WXZ  |1101_qqq_101_mmm_rrr
    case 0b1101_000_101:
    case 0b1101_001_101:
    case 0b1101_010_101:
    case 0b1101_011_101:
    case 0b1101_100_101:
    case 0b1101_101_101:
    case 0b1101_110_101:
    case 0b1101_111_101: return disAddToMemWord (sb);

      //ADDX.L Dr,Dq                            012346|-|*UUUU|*****|          |1101_qqq_110_000_rrr
      //ADDX.L -(Ar),-(Aq)                      012346|-|*UUUU|*****|          |1101_qqq_110_001_rrr
      //ADD.L Dq,<ea>                           012346|-|UUUUU|*****|  M+-WXZ  |1101_qqq_110_mmm_rrr
    case 0b1101_000_110:
    case 0b1101_001_110:
    case 0b1101_010_110:
    case 0b1101_011_110:
    case 0b1101_100_110:
    case 0b1101_101_110:
    case 0b1101_110_110:
    case 0b1101_111_110: return disAddToMemLong (sb);

      //ADDA.L <ea>,Aq                          012346|-|-----|-----|DAM+-WXZPI|1101_qqq_111_mmm_rrr
    case 0b1101_000_111:
    case 0b1101_001_111:
    case 0b1101_010_111:
    case 0b1101_011_111:
    case 0b1101_100_111:
    case 0b1101_101_111:
    case 0b1101_110_111:
    case 0b1101_111_111: return disAddaLong (sb);

      //ASR.B #<data>,Dr                        012346|-|UUUUU|***0*|          |1110_qqq_000_000_rrr
      //LSR.B #<data>,Dr                        012346|-|UUUUU|***0*|          |1110_qqq_000_001_rrr
      //ROXR.B #<data>,Dr                       012346|-|*UUUU|***0*|          |1110_qqq_000_010_rrr
      //ROR.B #<data>,Dr                        012346|-|-UUUU|-**0*|          |1110_qqq_000_011_rrr
      //ASR.B Dq,Dr                             012346|-|UUUUU|***0*|          |1110_qqq_000_100_rrr
      //LSR.B Dq,Dr                             012346|-|UUUUU|***0*|          |1110_qqq_000_101_rrr
      //ROXR.B Dq,Dr                            012346|-|*UUUU|***0*|          |1110_qqq_000_110_rrr
      //ROR.B Dq,Dr                             012346|-|-UUUU|-**0*|          |1110_qqq_000_111_rrr
    case 0b1110_000_000:
    case 0b1110_001_000:
    case 0b1110_010_000:
    case 0b1110_011_000:
    case 0b1110_100_000:
    case 0b1110_101_000:
    case 0b1110_110_000:
    case 0b1110_111_000: return disXxrToRegByte (sb);

      //ASR.W #<data>,Dr                        012346|-|UUUUU|***0*|          |1110_qqq_001_000_rrr
      //LSR.W #<data>,Dr                        012346|-|UUUUU|***0*|          |1110_qqq_001_001_rrr
      //ROXR.W #<data>,Dr                       012346|-|*UUUU|***0*|          |1110_qqq_001_010_rrr
      //ROR.W #<data>,Dr                        012346|-|-UUUU|-**0*|          |1110_qqq_001_011_rrr
      //ASR.W Dq,Dr                             012346|-|UUUUU|***0*|          |1110_qqq_001_100_rrr
      //LSR.W Dq,Dr                             012346|-|UUUUU|***0*|          |1110_qqq_001_101_rrr
      //ROXR.W Dq,Dr                            012346|-|*UUUU|***0*|          |1110_qqq_001_110_rrr
      //ROR.W Dq,Dr                             012346|-|-UUUU|-**0*|          |1110_qqq_001_111_rrr
    case 0b1110_000_001:
    case 0b1110_001_001:
    case 0b1110_010_001:
    case 0b1110_011_001:
    case 0b1110_100_001:
    case 0b1110_101_001:
    case 0b1110_110_001:
    case 0b1110_111_001: return disXxrToRegWord (sb);

      //ASR.L #<data>,Dr                        012346|-|UUUUU|***0*|          |1110_qqq_010_000_rrr
      //LSR.L #<data>,Dr                        012346|-|UUUUU|***0*|          |1110_qqq_010_001_rrr
      //ROXR.L #<data>,Dr                       012346|-|*UUUU|***0*|          |1110_qqq_010_010_rrr
      //ROR.L #<data>,Dr                        012346|-|-UUUU|-**0*|          |1110_qqq_010_011_rrr
      //ASR.L Dq,Dr                             012346|-|UUUUU|***0*|          |1110_qqq_010_100_rrr
      //LSR.L Dq,Dr                             012346|-|UUUUU|***0*|          |1110_qqq_010_101_rrr
      //ROXR.L Dq,Dr                            012346|-|*UUUU|***0*|          |1110_qqq_010_110_rrr
      //ROR.L Dq,Dr                             012346|-|-UUUU|-**0*|          |1110_qqq_010_111_rrr
    case 0b1110_000_010:
    case 0b1110_001_010:
    case 0b1110_010_010:
    case 0b1110_011_010:
    case 0b1110_100_010:
    case 0b1110_101_010:
    case 0b1110_110_010:
    case 0b1110_111_010: return disXxrToRegLong (sb);

      //ASR.W <ea>                              012346|-|UUUUU|***0*|  M+-WXZ  |1110_000_011_mmm_rrr
    case 0b1110_000_011: return disAsrToMem (sb);

      //LSR.W <ea>                              012346|-|UUUUU|*0*0*|  M+-WXZ  |1110_001_011_mmm_rrr
    case 0b1110_001_011: return disLsrToMem (sb);

      //ROXR.W <ea>                             012346|-|*UUUU|***0*|  M+-WXZ  |1110_010_011_mmm_rrr
    case 0b1110_010_011: return disRoxrToMem (sb);

      //ROR.W <ea>                              012346|-|-UUUU|-**0*|  M+-WXZ  |1110_011_011_mmm_rrr
    case 0b1110_011_011: return disRorToMem (sb);

      //ASL.B #<data>,Dr                        012346|-|UUUUU|*****|          |1110_qqq_100_000_rrr
      //LSL.B #<data>,Dr                        012346|-|UUUUU|***0*|          |1110_qqq_100_001_rrr
      //ROXL.B #<data>,Dr                       012346|-|*UUUU|***0*|          |1110_qqq_100_010_rrr
      //ROL.B #<data>,Dr                        012346|-|-UUUU|-**0*|          |1110_qqq_100_011_rrr
      //ASL.B Dq,Dr                             012346|-|UUUUU|*****|          |1110_qqq_100_100_rrr
      //LSL.B Dq,Dr                             012346|-|UUUUU|***0*|          |1110_qqq_100_101_rrr
      //ROXL.B Dq,Dr                            012346|-|*UUUU|***0*|          |1110_qqq_100_110_rrr
      //ROL.B Dq,Dr                             012346|-|-UUUU|-**0*|          |1110_qqq_100_111_rrr
    case 0b1110_000_100:
    case 0b1110_001_100:
    case 0b1110_010_100:
    case 0b1110_011_100:
    case 0b1110_100_100:
    case 0b1110_101_100:
    case 0b1110_110_100:
    case 0b1110_111_100: return disXxlToRegByte (sb);

      //ASL.W #<data>,Dr                        012346|-|UUUUU|*****|          |1110_qqq_101_000_rrr
      //LSL.W #<data>,Dr                        012346|-|UUUUU|***0*|          |1110_qqq_101_001_rrr
      //ROXL.W #<data>,Dr                       012346|-|*UUUU|***0*|          |1110_qqq_101_010_rrr
      //ROL.W #<data>,Dr                        012346|-|-UUUU|-**0*|          |1110_qqq_101_011_rrr
      //ASL.W Dq,Dr                             012346|-|UUUUU|*****|          |1110_qqq_101_100_rrr
      //LSL.W Dq,Dr                             012346|-|UUUUU|***0*|          |1110_qqq_101_101_rrr
      //ROXL.W Dq,Dr                            012346|-|*UUUU|***0*|          |1110_qqq_101_110_rrr
      //ROL.W Dq,Dr                             012346|-|-UUUU|-**0*|          |1110_qqq_101_111_rrr
    case 0b1110_000_101:
    case 0b1110_001_101:
    case 0b1110_010_101:
    case 0b1110_011_101:
    case 0b1110_100_101:
    case 0b1110_101_101:
    case 0b1110_110_101:
    case 0b1110_111_101: return disXxlToRegWord (sb);

      //ASL.L #<data>,Dr                        012346|-|UUUUU|*****|          |1110_qqq_110_000_rrr
      //LSL.L #<data>,Dr                        012346|-|UUUUU|***0*|          |1110_qqq_110_001_rrr
      //ROXL.L #<data>,Dr                       012346|-|*UUUU|***0*|          |1110_qqq_110_010_rrr
      //ROL.L #<data>,Dr                        012346|-|-UUUU|-**0*|          |1110_qqq_110_011_rrr
      //ASL.L Dq,Dr                             012346|-|UUUUU|*****|          |1110_qqq_110_100_rrr
      //LSL.L Dq,Dr                             012346|-|UUUUU|***0*|          |1110_qqq_110_101_rrr
      //ROXL.L Dq,Dr                            012346|-|*UUUU|***0*|          |1110_qqq_110_110_rrr
      //ROL.L Dq,Dr                             012346|-|-UUUU|-**0*|          |1110_qqq_110_111_rrr
    case 0b1110_000_110:
    case 0b1110_001_110:
    case 0b1110_010_110:
    case 0b1110_011_110:
    case 0b1110_100_110:
    case 0b1110_101_110:
    case 0b1110_110_110:
    case 0b1110_111_110: return disXxlToRegLong (sb);

      //ASL.W <ea>                              012346|-|UUUUU|*****|  M+-WXZ  |1110_000_111_mmm_rrr
    case 0b1110_000_111: return disAslToMem (sb);

      //LSL.W <ea>                              012346|-|UUUUU|***0*|  M+-WXZ  |1110_001_111_mmm_rrr
    case 0b1110_001_111: return disLslToMem (sb);

      //ROXL.W <ea>                             012346|-|*UUUU|***0*|  M+-WXZ  |1110_010_111_mmm_rrr
    case 0b1110_010_111: return disRoxlToMem (sb);

      //ROL.W <ea>                              012346|-|-UUUU|-**0*|  M+-WXZ  |1110_011_111_mmm_rrr
    case 0b1110_011_111: return disRolToMem (sb);

      //BFTST <ea>{#o:#w}                       --2346|-|-UUUU|-**00|D M  WXZP |1110_100_011_mmm_rrr-0000_0oo_ooo_0ww_www
      //BFTST <ea>{#o:Dw}                       --2346|-|-UUUU|-**00|D M  WXZP |1110_100_011_mmm_rrr-0000_0oo_ooo_100_www
      //BFTST <ea>{Do:#w}                       --2346|-|-UUUU|-**00|D M  WXZP |1110_100_011_mmm_rrr-0000_100_ooo_0ww_www
      //BFTST <ea>{Do:Dw}                       --2346|-|-UUUU|-**00|D M  WXZP |1110_100_011_mmm_rrr-0000_100_ooo_100_www
    case 0b1110_100_011: return disBftst (sb);

      //BFEXTU <ea>{#o:#w},Dn                   --2346|-|-UUUU|-**00|D M  WXZP |1110_100_111_mmm_rrr-0nnn_0oo_ooo_0ww_www
      //BFEXTU <ea>{#o:Dw},Dn                   --2346|-|-UUUU|-**00|D M  WXZP |1110_100_111_mmm_rrr-0nnn_0oo_ooo_100_www
      //BFEXTU <ea>{Do:#w},Dn                   --2346|-|-UUUU|-**00|D M  WXZP |1110_100_111_mmm_rrr-0nnn_100_ooo_0ww_www
      //BFEXTU <ea>{Do:Dw},Dn                   --2346|-|-UUUU|-**00|D M  WXZP |1110_100_111_mmm_rrr-0nnn_100_ooo_100_www
    case 0b1110_100_111: return disBfextu (sb);

      //BFCHG <ea>{#o:#w}                       --2346|-|-UUUU|-**00|D M  WXZ  |1110_101_011_mmm_rrr-0000_0oo_ooo_0ww_www
      //BFCHG <ea>{#o:Dw}                       --2346|-|-UUUU|-**00|D M  WXZ  |1110_101_011_mmm_rrr-0000_0oo_ooo_100_www
      //BFCHG <ea>{Do:#w}                       --2346|-|-UUUU|-**00|D M  WXZ  |1110_101_011_mmm_rrr-0000_100_ooo_0ww_www
      //BFCHG <ea>{Do:Dw}                       --2346|-|-UUUU|-**00|D M  WXZ  |1110_101_011_mmm_rrr-0000_100_ooo_100_www
    case 0b1110_101_011: return disBfchg (sb);

      //BFEXTS <ea>{#o:#w},Dn                   --2346|-|-UUUU|-**00|D M  WXZP |1110_101_111_mmm_rrr-0nnn_0oo_ooo_0ww_www
      //BFEXTS <ea>{#o:Dw},Dn                   --2346|-|-UUUU|-**00|D M  WXZP |1110_101_111_mmm_rrr-0nnn_0oo_ooo_100_www
      //BFEXTS <ea>{Do:#w},Dn                   --2346|-|-UUUU|-**00|D M  WXZP |1110_101_111_mmm_rrr-0nnn_100_ooo_0ww_www
      //BFEXTS <ea>{Do:Dw},Dn                   --2346|-|-UUUU|-**00|D M  WXZP |1110_101_111_mmm_rrr-0nnn_100_ooo_100_www
    case 0b1110_101_111: return disBfexts (sb);

      //BFCLR <ea>{#o:#w}                       --2346|-|-UUUU|-**00|D M  WXZ  |1110_110_011_mmm_rrr-0000_0oo_ooo_0ww_www
      //BFCLR <ea>{#o:Dw}                       --2346|-|-UUUU|-**00|D M  WXZ  |1110_110_011_mmm_rrr-0000_0oo_ooo_100_www
      //BFCLR <ea>{Do:#w}                       --2346|-|-UUUU|-**00|D M  WXZ  |1110_110_011_mmm_rrr-0000_100_ooo_0ww_www
      //BFCLR <ea>{Do:Dw}                       --2346|-|-UUUU|-**00|D M  WXZ  |1110_110_011_mmm_rrr-0000_100_ooo_100_www
    case 0b1110_110_011: return disBfclr (sb);

      //BFFFO <ea>{#o:#w},Dn                    --2346|-|-UUUU|-**00|D M  WXZP |1110_110_111_mmm_rrr-0nnn_0oo_ooo_0ww_www
      //BFFFO <ea>{#o:Dw},Dn                    --2346|-|-UUUU|-**00|D M  WXZP |1110_110_111_mmm_rrr-0nnn_0oo_ooo_100_www
      //BFFFO <ea>{Do:#w},Dn                    --2346|-|-UUUU|-**00|D M  WXZP |1110_110_111_mmm_rrr-0nnn_100_ooo_0ww_www
      //BFFFO <ea>{Do:Dw},Dn                    --2346|-|-UUUU|-**00|D M  WXZP |1110_110_111_mmm_rrr-0nnn_100_ooo_100_www
    case 0b1110_110_111: return disBfffo (sb);

      //BFSET <ea>{#o:#w}                       --2346|-|-UUUU|-**00|D M  WXZ  |1110_111_011_mmm_rrr-0000_0oo_ooo_0ww_www
      //BFSET <ea>{#o:Dw}                       --2346|-|-UUUU|-**00|D M  WXZ  |1110_111_011_mmm_rrr-0000_0oo_ooo_100_www
      //BFSET <ea>{Do:#w}                       --2346|-|-UUUU|-**00|D M  WXZ  |1110_111_011_mmm_rrr-0000_100_ooo_0ww_www
      //BFSET <ea>{Do:Dw}                       --2346|-|-UUUU|-**00|D M  WXZ  |1110_111_011_mmm_rrr-0000_100_ooo_100_www
    case 0b1110_111_011: return disBfset (sb);

      //BFINS Dn,<ea>{#o:#w}                    --2346|-|-UUUU|-**00|D M  WXZ  |1110_111_111_mmm_rrr-0nnn_0oo_ooo_0ww_www
      //BFINS Dn,<ea>{#o:Dw}                    --2346|-|-UUUU|-**00|D M  WXZ  |1110_111_111_mmm_rrr-0nnn_0oo_ooo_100_www
      //BFINS Dn,<ea>{Do:#w}                    --2346|-|-UUUU|-**00|D M  WXZ  |1110_111_111_mmm_rrr-0nnn_100_ooo_0ww_www
      //BFINS Dn,<ea>{Do:Dw}                    --2346|-|-UUUU|-**00|D M  WXZ  |1110_111_111_mmm_rrr-0nnn_100_ooo_100_www
    case 0b1110_111_111: return disBfins (sb);

      //PMOVE.L <ea>,TTn                        ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-00001n0000000000
      //PMOVEFD.L <ea>,TTn                      ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-00001n0100000000
      //PMOVE.L TTn,<ea>                        ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-00001n1000000000
      //PLOADW SFC,<ea>                         --M3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0010000000000000
      //PLOADW DFC,<ea>                         --M3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0010000000000001
      //PLOADW Dn,<ea>                          --M3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0010000000001nnn
      //PLOADW #<data>,<ea>                     ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0010000000010ddd
      //PLOADW #<data>,<ea>                     --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-001000000001dddd
      //PLOADR SFC,<ea>                         --M3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0010001000000000
      //PLOADR DFC,<ea>                         --M3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0010001000000001
      //PLOADR Dn,<ea>                          --M3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0010001000001nnn
      //PLOADR #<data>,<ea>                     ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0010001000010ddd
      //PLOADR #<data>,<ea>                     --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-001000100001dddd
      //PFLUSHA                                 --M---|P|-----|-----|          |1111_000_000_000_000-0010010000000000
      //PFLUSHA                                 ---3--|P|-----|-----|          |1111_000_000_000_000-0010010000000000
      //PVALID.L VAL,<ea>                       --M---|-|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0010100000000000
      //PVALID.L An,<ea>                        --M---|-|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0010110000000nnn
      //PFLUSH SFC,#<mask>                      --M---|P|-----|-----|          |1111_000_000_000_000-0011000mmmm00000
      //PFLUSH SFC,#<mask>                      ---3--|P|-----|-----|          |1111_000_000_000_000-00110000mmm00000
      //PFLUSH DFC,#<mask>                      --M---|P|-----|-----|          |1111_000_000_000_000-0011000mmmm00001
      //PFLUSH DFC,#<mask>                      ---3--|P|-----|-----|          |1111_000_000_000_000-00110000mmm00001
      //PFLUSH Dn,#<mask>                       --M---|P|-----|-----|          |1111_000_000_000_000-0011000mmmm01nnn
      //PFLUSH Dn,#<mask>                       ---3--|P|-----|-----|          |1111_000_000_000_000-00110000mmm01nnn
      //PFLUSH #<data>,#<mask>                  ---3--|P|-----|-----|          |1111_000_000_000_000-00110000mmm10ddd
      //PFLUSH #<data>,#<mask>                  --M---|P|-----|-----|          |1111_000_000_000_000-0011000mmmm1dddd
      //PFLUSHS SFC,#<mask>                     --M---|P|-----|-----|          |1111_000_000_000_000-0011010mmmm00000
      //PFLUSHS DFC,#<mask>                     --M---|P|-----|-----|          |1111_000_000_000_000-0011010mmmm00001
      //PFLUSHS Dn,#<mask>                      --M---|P|-----|-----|          |1111_000_000_000_000-0011010mmmm01nnn
      //PFLUSHS #<data>,#<mask>                 --M---|P|-----|-----|          |1111_000_000_000_000-0011010mmmm1dddd
      //PFLUSH SFC,#<mask>,<ea>                 --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0011100mmmm00000
      //PFLUSH SFC,#<mask>,<ea>                 ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-00111000mmm00000
      //PFLUSH DFC,#<mask>,<ea>                 --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0011100mmmm00001
      //PFLUSH DFC,#<mask>,<ea>                 ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-00111000mmm00001
      //PFLUSH Dn,#<mask>,<ea>                  ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-00111000mmm01nnn
      //PFLUSH #<data>,#<mask>,<ea>             ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-00111000mmm10ddd
      //PFLUSH Dn,#<mask>,<ea>                  --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0011100mmmm01nnn
      //PFLUSH #<data>,#<mask>,<ea>             --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0011100mmmm1dddd
      //PFLUSHS SFC,#<mask>,<ea>                --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0011110mmmm00000
      //PFLUSHS DFC,#<mask>,<ea>                --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0011110mmmm00001
      //PFLUSHS Dn,#<mask>,<ea>                 --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0011110mmmm01nnn
      //PFLUSHS #<data>,#<mask>,<ea>            --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0011110mmmm1dddd
      //PMOVE.L <ea>,TC                         ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0100000000000000
      //PMOVE.L <ea>,TC                         --M---|P|-----|-----|DAM+-WXZPI|1111_000_000_mmm_rrr-0100000000000000
      //PMOVEFD.L <ea>,TC                       ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0100000100000000
      //PMOVE.L TC,<ea>                         ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0100001000000000
      //PMOVE.L TC,<ea>                         --M---|P|-----|-----|DAM+-WXZ  |1111_000_000_mmm_rrr-0100001000000000
      //PMOVE.Q <ea>,DRP                        --M---|P|-----|-----|  M+-WXZPI|1111_000_000_mmm_rrr-0100010000000000
      //PMOVE.Q DRP,<ea>                        --M---|P|-----|-----|  M+-WXZ  |1111_000_000_mmm_rrr-0100011000000000
      //PMOVE.Q <ea>,SRP                        ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0100100000000000
      //PMOVE.Q <ea>,SRP                        --M---|P|-----|-----|  M+-WXZPI|1111_000_000_mmm_rrr-0100100000000000
      //PMOVEFD.Q <ea>,SRP                      ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0100100100000000
      //PMOVE.Q SRP,<ea>                        ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0100101000000000
      //PMOVE.Q SRP,<ea>                        --M---|P|-----|-----|  M+-WXZ  |1111_000_000_mmm_rrr-0100101000000000
      //PMOVE.Q <ea>,CRP                        ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0100110000000000
      //PMOVE.Q <ea>,CRP                        --M---|P|-----|-----|  M+-WXZPI|1111_000_000_mmm_rrr-0100110000000000
      //PMOVEFD.Q <ea>,CRP                      ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0100110100000000
      //PMOVE.Q CRP,<ea>                        ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0100111000000000
      //PMOVE.Q CRP,<ea>                        --M---|P|-----|-----|  M+-WXZ  |1111_000_000_mmm_rrr-0100111000000000
      //PMOVE.B <ea>,CAL                        --M---|P|-----|-----|DAM+-WXZPI|1111_000_000_mmm_rrr-0101000000000000
      //PMOVE.B CAL,<ea>                        --M---|P|-----|-----|DAM+-WXZ  |1111_000_000_mmm_rrr-0101001000000000
      //PMOVE.B <ea>,VAL                        --M---|P|-----|-----|DAM+-WXZPI|1111_000_000_mmm_rrr-0101010000000000
      //PMOVE.B VAL,<ea>                        --M---|P|-----|-----|DAM+-WXZ  |1111_000_000_mmm_rrr-0101011000000000
      //PMOVE.B <ea>,SCC                        --M---|P|-----|-----|DAM+-WXZPI|1111_000_000_mmm_rrr-0101100000000000
      //PMOVE.B SCC,<ea>                        --M---|P|-----|-----|DAM+-WXZ  |1111_000_000_mmm_rrr-0101101000000000
      //PMOVE.W <ea>,AC                         --M---|P|-----|-----|DAM+-WXZPI|1111_000_000_mmm_rrr-0101110000000000
      //PMOVE.W AC,<ea>                         --M---|P|-----|-----|DAM+-WXZ  |1111_000_000_mmm_rrr-0101111000000000
      //PMOVE.W <ea>,MMUSR                      ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0110000000000000
      //PMOVE.W <ea>,PSR                        --M---|P|-----|-----|DAM+-WXZPI|1111_000_000_mmm_rrr-0110000000000000
      //PMOVE.W MMUSR,<ea>                      ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0110001000000000
      //PMOVE.W PSR,<ea>                        --M---|P|-----|-----|DAM+-WXZ  |1111_000_000_mmm_rrr-0110001000000000
      //PMOVE.W <ea>,PCSR                       --M---|P|-----|-----|DAM+-WXZPI|1111_000_000_mmm_rrr-0110010000000000
      //PMOVE.W PCSR,<ea>                       --M---|P|-----|-----|DAM+-WXZ  |1111_000_000_mmm_rrr-0110011000000000
      //PMOVE.W <ea>,BADn                       --M---|P|-----|-----|DAM+-WXZPI|1111_000_000_mmm_rrr-01110000000nnn00
      //PMOVE.W BADn,<ea>                       --M---|P|-----|-----|DAM+-WXZ  |1111_000_000_mmm_rrr-01110010000nnn00
      //PMOVE.W <ea>,BACn                       --M---|P|-----|-----|DAM+-WXZPI|1111_000_000_mmm_rrr-01110100000nnn00
      //PMOVE.W BACn,<ea>                       --M---|P|-----|-----|DAM+-WXZ  |1111_000_000_mmm_rrr-01110110000nnn00
      //PTESTW SFC,<ea>,#<level>                --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll0000000000
      //PTESTW SFC,<ea>,#<level>                ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll0000000000
      //PTESTW DFC,<ea>,#<level>                --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll0000000001
      //PTESTW DFC,<ea>,#<level>                ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll0000000001
      //PTESTW Dn,<ea>,#<level>                 --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll0000001nnn
      //PTESTW Dn,<ea>,#<level>                 ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll0000001nnn
      //PTESTW #<data>,<ea>,#<level>            --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll000001dddd
      //PTESTW #<data>,<ea>,#<level>            ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll0000010ddd
      //PTESTW SFC,<ea>,#<level>,An             --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll01nnn00000
      //PTESTW SFC,<ea>,#<level>,An             ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll01nnn00000
      //PTESTW DFC,<ea>,#<level>,An             --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll01nnn00001
      //PTESTW DFC,<ea>,#<level>,An             ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll01nnn00001
      //PTESTW Dn,<ea>,#<level>,An              --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll01nnn01nnn
      //PTESTW Dn,<ea>,#<level>,An              ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll01nnn01nnn
      //PTESTW #<data>,<ea>,#<level>,An         --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll01nnn1dddd
      //PTESTW #<data>,<ea>,#<level>,An         ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll01nnn10ddd
      //PTESTR SFC,<ea>,#<level>                --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll1000000000
      //PTESTR SFC,<ea>,#<level>                ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll1000000000
      //PTESTR DFC,<ea>,#<level>                --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll1000000001
      //PTESTR DFC,<ea>,#<level>                ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll1000000001
      //PTESTR Dn,<ea>,#<level>                 --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll1000001nnn
      //PTESTR Dn,<ea>,#<level>                 ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll1000001nnn
      //PTESTR #<data>,<ea>,#<level>            --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll100001dddd
      //PTESTR #<data>,<ea>,#<level>            ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll1000010ddd
      //PTESTR SFC,<ea>,#<level>,An             --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll11nnn00000
      //PTESTR SFC,<ea>,#<level>,An             ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll11nnn00000
      //PTESTR DFC,<ea>,#<level>,An             --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll11nnn00001
      //PTESTR DFC,<ea>,#<level>,An             ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll11nnn00001
      //PTESTR Dn,<ea>,#<level>,An              --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll11nnn01nnn
      //PTESTR Dn,<ea>,#<level>,An              ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll11nnn01nnn
      //PTESTR #<data>,<ea>,#<level>,An         --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll11nnn1dddd
      //PTESTR #<data>,<ea>,#<level>,An         ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll11nnn10ddd
      //PFLUSHR <ea>                            --M---|P|-----|-----|  M+-WXZPI|1111_000_000_mmm_rrr-1010000000000000
    case 0b1111_000_000: return disPgen (sb);

      //PDBcc.W Dn,<label>                      --M---|P|-----|-----|          |1111_000_001_001_nnn-0000000000cccccc-{offset}
      //PTRAPcc.W #<data>                       --M---|P|-----|-----|          |1111_000_001_111_010-0000000000cccccc-{data}
      //PTRAPcc.L #<data>                       --M---|P|-----|-----|          |1111_000_001_111_011-0000000000cccccc-{data}
      //PTRAPcc                                 --M---|P|-----|-----|          |1111_000_001_111_100-0000000000cccccc
      //PScc.B <ea>                             --M---|P|-----|-----|D M+-WXZ  |1111_000_001_mmm_rrr-0000000000cccccc
    case 0b1111_000_001: return disPscc (sb);

      //PBcc.W <label>                          --M---|P|-----|-----|          |1111_000_010_ccc_ccc-{offset}
    case 0b1111_000_010: return disPbccWord (sb);

      //PBcc.L <label>                          --M---|P|-----|-----|          |1111_000_011_ccc_ccc-{offset}
    case 0b1111_000_011: return disPbccLong (sb);

      //PSAVE <ea>                              --M---|P|-----|-----|  M -WXZ  |1111_000_100_mmm_rrr
    case 0b1111_000_100: return disPsave (sb);

      //PRESTORE <ea>                           --M---|P|-----|-----|  M+ WXZP |1111_000_101_mmm_rrr
    case 0b1111_000_101: return disPrestore (sb);

      //line 1111 emulator                      012346|-|-----|-----|          |1111_xxx_xxx_xxx_xxx
    case 0b1111_000_110:
    case 0b1111_000_111: return disFline (sb);

      //FMOVE.X FPm,FPn                         --CC46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_000_0000
      //FINT.X FPm,FPn                          --CCS6|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_000_0001
      //FSINH.X FPm,FPn                         --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_000_0010
      //FINTRZ.X FPm,FPn                        --CCS6|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_000_0011
      //FSQRT.X FPm,FPn                         --CC46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_000_0100
      //FLOGNP1.X FPm,FPn                       --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_000_0110
      //FETOXM1.X FPm,FPn                       --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_000_1000
      //FTANH.X FPm,FPn                         --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_000_1001
      //FATAN.X FPm,FPn                         --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_000_1010
      //FASIN.X FPm,FPn                         --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_000_1100
      //FATANH.X FPm,FPn                        --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_000_1101
      //FSIN.X FPm,FPn                          --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_000_1110
      //FTAN.X FPm,FPn                          --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_000_1111
      //FETOX.X FPm,FPn                         --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_001_0000
      //FTWOTOX.X FPm,FPn                       --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_001_0001
      //FTENTOX.X FPm,FPn                       --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_001_0010
      //FLOGN.X FPm,FPn                         --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_001_0100
      //FLOG10.X FPm,FPn                        --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_001_0101
      //FLOG2.X FPm,FPn                         --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_001_0110
      //FABS.X FPm,FPn                          --CC46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_001_1000
      //FCOSH.X FPm,FPn                         --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_001_1001
      //FNEG.X FPm,FPn                          --CC46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_001_1010
      //FACOS.X FPm,FPn                         --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_001_1100
      //FCOS.X FPm,FPn                          --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_001_1101
      //FGETEXP.X FPm,FPn                       --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_001_1110
      //FGETMAN.X FPm,FPn                       --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_001_1111
      //FDIV.X FPm,FPn                          --CC46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_010_0000
      //FMOD.X FPm,FPn                          --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_010_0001
      //FADD.X FPm,FPn                          --CC46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_010_0010
      //FMUL.X FPm,FPn                          --CC46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_010_0011
      //FSGLDIV.X FPm,FPn                       --CCS6|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_010_0100
      //FREM.X FPm,FPn                          --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_010_0101
      //FSCALE.X FPm,FPn                        --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_010_0110
      //FSGLMUL.X FPm,FPn                       --CCS6|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_010_0111
      //FSUB.X FPm,FPn                          --CC46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_010_1000
      //FSINCOS.X FPm,FPc:FPs                   --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_sss_011_0ccc
      //FCMP.X FPm,FPn                          --CC46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_011_1000
      //FTST.X FPm                              --CC46|-|-----|-----|          |1111_001_000_000_000-000_mmm_000_011_1010
      //FSMOVE.X FPm,FPn                        ----46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_100_0000
      //FSSQRT.X FPm,FPn                        ----46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_100_0001
      //FDMOVE.X FPm,FPn                        ----46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_100_0100
      //FDSQRT.X FPm,FPn                        ----46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_100_0101
      //FSABS.X FPm,FPn                         ----46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_101_1000
      //FSNEG.X FPm,FPn                         ----46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_101_1010
      //FDABS.X FPm,FPn                         ----46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_101_1100
      //FDNEG.X FPm,FPn                         ----46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_101_1110
      //FSDIV.X FPm,FPn                         ----46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_110_0000
      //FSADD.X FPm,FPn                         ----46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_110_0010
      //FSMUL.X FPm,FPn                         ----46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_110_0011
      //FDDIV.X FPm,FPn                         ----46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_110_0100
      //FDADD.X FPm,FPn                         ----46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_110_0110
      //FDMUL.X FPm,FPn                         ----46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_110_0111
      //FSSUB.X FPm,FPn                         ----46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_110_1000
      //FDSUB.X FPm,FPn                         ----46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_110_1100
      //FMOVE.L <ea>,FPn                        --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_000_0000
      //FMOVE.S <ea>,FPn                        --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_000_0000
      //FMOVE.X <ea>,FPn                        --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_000_0000
      //FMOVE.P <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_000_0000
      //FMOVE.W <ea>,FPn                        --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_000_0000
      //FMOVE.D <ea>,FPn                        --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_000_0000
      //FMOVE.B <ea>,FPn                        --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_000_0000
      //FINT.L <ea>,FPn                         --CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_000_0001
      //FINT.S <ea>,FPn                         --CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_000_0001
      //FINT.X <ea>,FPn                         --CCS6|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_000_0001
      //FINT.P <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_000_0001
      //FINT.W <ea>,FPn                         --CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_000_0001
      //FINT.D <ea>,FPn                         --CCS6|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_000_0001
      //FINT.B <ea>,FPn                         --CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_000_0001
      //FSINH.L <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_000_0010
      //FSINH.S <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_000_0010
      //FSINH.X <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_000_0010
      //FSINH.P <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_000_0010
      //FSINH.W <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_000_0010
      //FSINH.D <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_000_0010
      //FSINH.B <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_000_0010
      //FINTRZ.L <ea>,FPn                       --CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_000_0011
      //FINTRZ.S <ea>,FPn                       --CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_000_0011
      //FINTRZ.X <ea>,FPn                       --CCS6|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_000_0011
      //FINTRZ.P <ea>,FPn                       --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_000_0011
      //FINTRZ.W <ea>,FPn                       --CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_000_0011
      //FINTRZ.D <ea>,FPn                       --CCS6|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_000_0011
      //FINTRZ.B <ea>,FPn                       --CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_000_0011
      //FSQRT.L <ea>,FPn                        --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_000_0100
      //FSQRT.S <ea>,FPn                        --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_000_0100
      //FSQRT.X <ea>,FPn                        --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_000_0100
      //FSQRT.P <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_000_0100
      //FSQRT.W <ea>,FPn                        --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_000_0100
      //FSQRT.D <ea>,FPn                        --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_000_0100
      //FSQRT.B <ea>,FPn                        --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_000_0100
      //FLOGNP1.L <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_000_0110
      //FLOGNP1.S <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_000_0110
      //FLOGNP1.X <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_000_0110
      //FLOGNP1.P <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_000_0110
      //FLOGNP1.W <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_000_0110
      //FLOGNP1.D <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_000_0110
      //FLOGNP1.B <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_000_0110
      //FETOXM1.L <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_000_1000
      //FETOXM1.S <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_000_1000
      //FETOXM1.X <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_000_1000
      //FETOXM1.P <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_000_1000
      //FETOXM1.W <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_000_1000
      //FETOXM1.D <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_000_1000
      //FETOXM1.B <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_000_1000
      //FTANH.L <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_000_1001
      //FTANH.S <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_000_1001
      //FTANH.X <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_000_1001
      //FTANH.P <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_000_1001
      //FTANH.W <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_000_1001
      //FTANH.D <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_000_1001
      //FTANH.B <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_000_1001
      //FATAN.L <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_000_1010
      //FATAN.S <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_000_1010
      //FATAN.X <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_000_1010
      //FATAN.P <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_000_1010
      //FATAN.W <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_000_1010
      //FATAN.D <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_000_1010
      //FATAN.B <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_000_1010
      //FASIN.L <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_000_1100
      //FASIN.S <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_000_1100
      //FASIN.X <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_000_1100
      //FASIN.P <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_000_1100
      //FASIN.W <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_000_1100
      //FASIN.D <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_000_1100
      //FASIN.B <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_000_1100
      //FATANH.L <ea>,FPn                       --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_000_1101
      //FATANH.S <ea>,FPn                       --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_000_1101
      //FATANH.X <ea>,FPn                       --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_000_1101
      //FATANH.P <ea>,FPn                       --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_000_1101
      //FATANH.W <ea>,FPn                       --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_000_1101
      //FATANH.D <ea>,FPn                       --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_000_1101
      //FATANH.B <ea>,FPn                       --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_000_1101
      //FSIN.L <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_000_1110
      //FSIN.S <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_000_1110
      //FSIN.X <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_000_1110
      //FSIN.P <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_000_1110
      //FSIN.W <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_000_1110
      //FSIN.D <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_000_1110
      //FSIN.B <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_000_1110
      //FTAN.L <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_000_1111
      //FTAN.S <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_000_1111
      //FTAN.X <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_000_1111
      //FTAN.P <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_000_1111
      //FTAN.W <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_000_1111
      //FTAN.D <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_000_1111
      //FTAN.B <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_000_1111
      //FETOX.L <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_001_0000
      //FETOX.S <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_001_0000
      //FETOX.X <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_001_0000
      //FETOX.P <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_001_0000
      //FETOX.W <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_001_0000
      //FETOX.D <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_001_0000
      //FETOX.B <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_001_0000
      //FTWOTOX.L <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_001_0001
      //FTWOTOX.S <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_001_0001
      //FTWOTOX.X <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_001_0001
      //FTWOTOX.P <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_001_0001
      //FTWOTOX.W <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_001_0001
      //FTWOTOX.D <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_001_0001
      //FTWOTOX.B <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_001_0001
      //FTENTOX.L <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_001_0010
      //FTENTOX.S <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_001_0010
      //FTENTOX.X <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_001_0010
      //FTENTOX.P <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_001_0010
      //FTENTOX.W <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_001_0010
      //FTENTOX.D <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_001_0010
      //FTENTOX.B <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_001_0010
      //FLOGN.L <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_001_0100
      //FLOGN.S <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_001_0100
      //FLOGN.X <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_001_0100
      //FLOGN.P <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_001_0100
      //FLOGN.W <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_001_0100
      //FLOGN.D <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_001_0100
      //FLOGN.B <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_001_0100
      //FLOG10.L <ea>,FPn                       --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_001_0101
      //FLOG10.S <ea>,FPn                       --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_001_0101
      //FLOG10.X <ea>,FPn                       --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_001_0101
      //FLOG10.P <ea>,FPn                       --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_001_0101
      //FLOG10.W <ea>,FPn                       --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_001_0101
      //FLOG10.D <ea>,FPn                       --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_001_0101
      //FLOG10.B <ea>,FPn                       --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_001_0101
      //FLOG2.L <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_001_0110
      //FLOG2.S <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_001_0110
      //FLOG2.X <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_001_0110
      //FLOG2.P <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_001_0110
      //FLOG2.W <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_001_0110
      //FLOG2.D <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_001_0110
      //FLOG2.B <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_001_0110
      //FABS.L <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_001_1000
      //FABS.S <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_001_1000
      //FABS.X <ea>,FPn                         --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_001_1000
      //FABS.P <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_001_1000
      //FABS.W <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_001_1000
      //FABS.D <ea>,FPn                         --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_001_1000
      //FABS.B <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_001_1000
      //FCOSH.L <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_001_1001
      //FCOSH.S <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_001_1001
      //FCOSH.X <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_001_1001
      //FCOSH.P <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_001_1001
      //FCOSH.W <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_001_1001
      //FCOSH.D <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_001_1001
      //FCOSH.B <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_001_1001
      //FNEG.L <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_001_1010
      //FNEG.S <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_001_1010
      //FNEG.X <ea>,FPn                         --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_001_1010
      //FNEG.P <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_001_1010
      //FNEG.W <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_001_1010
      //FNEG.D <ea>,FPn                         --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_001_1010
      //FNEG.B <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_001_1010
      //FACOS.L <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_001_1100
      //FACOS.S <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_001_1100
      //FACOS.X <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_001_1100
      //FACOS.P <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_001_1100
      //FACOS.W <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_001_1100
      //FACOS.D <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_001_1100
      //FACOS.B <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_001_1100
      //FCOS.L <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_001_1101
      //FCOS.S <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_001_1101
      //FCOS.X <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_001_1101
      //FCOS.P <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_001_1101
      //FCOS.W <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_001_1101
      //FCOS.D <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_001_1101
      //FCOS.B <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_001_1101
      //FGETEXP.L <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_001_1110
      //FGETEXP.S <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_001_1110
      //FGETEXP.X <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_001_1110
      //FGETEXP.P <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_001_1110
      //FGETEXP.W <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_001_1110
      //FGETEXP.D <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_001_1110
      //FGETEXP.B <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_001_1110
      //FGETMAN.L <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_001_1111
      //FGETMAN.S <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_001_1111
      //FGETMAN.X <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_001_1111
      //FGETMAN.P <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_001_1111
      //FGETMAN.W <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_001_1111
      //FGETMAN.D <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_001_1111
      //FGETMAN.B <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_001_1111
      //FDIV.L <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_010_0000
      //FDIV.S <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_010_0000
      //FDIV.X <ea>,FPn                         --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_010_0000
      //FDIV.P <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_010_0000
      //FDIV.W <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_010_0000
      //FDIV.D <ea>,FPn                         --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_010_0000
      //FDIV.B <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_010_0000
      //FMOD.L <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_010_0001
      //FMOD.S <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_010_0001
      //FMOD.X <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_010_0001
      //FMOD.P <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_010_0001
      //FMOD.W <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_010_0001
      //FMOD.D <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_010_0001
      //FMOD.B <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_010_0001
      //FADD.L <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_010_0010
      //FADD.S <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_010_0010
      //FADD.X <ea>,FPn                         --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_010_0010
      //FADD.P <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_010_0010
      //FADD.W <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_010_0010
      //FADD.D <ea>,FPn                         --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_010_0010
      //FADD.B <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_010_0010
      //FMUL.L <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_010_0011
      //FMUL.S <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_010_0011
      //FMUL.X <ea>,FPn                         --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_010_0011
      //FMUL.P <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_010_0011
      //FMUL.W <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_010_0011
      //FMUL.D <ea>,FPn                         --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_010_0011
      //FMUL.B <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_010_0011
      //FSGLDIV.L <ea>,FPn                      --CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_010_0100
      //FSGLDIV.S <ea>,FPn                      --CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_010_0100
      //FSGLDIV.X <ea>,FPn                      --CCS6|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_010_0100
      //FSGLDIV.P <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_010_0100
      //FSGLDIV.W <ea>,FPn                      --CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_010_0100
      //FSGLDIV.D <ea>,FPn                      --CCS6|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_010_0100
      //FSGLDIV.B <ea>,FPn                      --CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_010_0100
      //FREM.L <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_010_0101
      //FREM.S <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_010_0101
      //FREM.X <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_010_0101
      //FREM.P <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_010_0101
      //FREM.W <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_010_0101
      //FREM.D <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_010_0101
      //FREM.B <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_010_0101
      //FSCALE.L <ea>,FPn                       --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_010_0110
      //FSCALE.S <ea>,FPn                       --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_010_0110
      //FSCALE.X <ea>,FPn                       --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_010_0110
      //FSCALE.P <ea>,FPn                       --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_010_0110
      //FSCALE.W <ea>,FPn                       --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_010_0110
      //FSCALE.D <ea>,FPn                       --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_010_0110
      //FSCALE.B <ea>,FPn                       --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_010_0110
      //FSGLMUL.L <ea>,FPn                      --CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_010_0111
      //FSGLMUL.S <ea>,FPn                      --CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_010_0111
      //FSGLMUL.X <ea>,FPn                      --CCS6|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_010_0111
      //FSGLMUL.P <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_010_0111
      //FSGLMUL.W <ea>,FPn                      --CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_010_0111
      //FSGLMUL.D <ea>,FPn                      --CCS6|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_010_0111
      //FSGLMUL.B <ea>,FPn                      --CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_010_0111
      //FSUB.L <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_010_1000
      //FSUB.S <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_010_1000
      //FSUB.X <ea>,FPn                         --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_010_1000
      //FSUB.P <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_010_1000
      //FSUB.W <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_010_1000
      //FSUB.D <ea>,FPn                         --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_010_1000
      //FSUB.B <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_010_1000
      //FSINCOS.L <ea>,FPc:FPs                  --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_sss_011_0ccc
      //FSINCOS.S <ea>,FPc:FPs                  --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_sss_011_0ccc
      //FSINCOS.X <ea>,FPc:FPs                  --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_sss_011_0ccc
      //FSINCOS.P <ea>,FPc:FPs                  --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_sss_011_0ccc
      //FSINCOS.W <ea>,FPc:FPs                  --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_sss_011_0ccc
      //FSINCOS.D <ea>,FPc:FPs                  --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_sss_011_0ccc
      //FSINCOS.B <ea>,FPc:FPs                  --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_sss_011_0ccc
      //FCMP.L <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_011_1000
      //FCMP.S <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_011_1000
      //FCMP.X <ea>,FPn                         --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_011_1000
      //FCMP.P <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_011_1000
      //FCMP.W <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_011_1000
      //FCMP.D <ea>,FPn                         --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_011_1000
      //FCMP.B <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_011_1000
      //FTST.L <ea>                             --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_000_011_1010
      //FTST.S <ea>                             --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_000_011_1010
      //FTST.X <ea>                             --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_000_011_1010
      //FTST.P <ea>                             --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_000_011_1010
      //FTST.W <ea>                             --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_000_011_1010
      //FTST.D <ea>                             --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_000_011_1010
      //FTST.B <ea>                             --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_000_011_1010
      //FSMOVE.L <ea>,FPn                       ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_100_0000
      //FSMOVE.S <ea>,FPn                       ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_100_0000
      //FSMOVE.X <ea>,FPn                       ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_100_0000
      //FSMOVE.P <ea>,FPn                       ----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_100_0000
      //FSMOVE.W <ea>,FPn                       ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_100_0000
      //FSMOVE.D <ea>,FPn                       ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_100_0000
      //FSMOVE.B <ea>,FPn                       ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_100_0000
      //FSSQRT.L <ea>,FPn                       ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_100_0001
      //FSSQRT.S <ea>,FPn                       ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_100_0001
      //FSSQRT.X <ea>,FPn                       ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_100_0001
      //FSSQRT.P <ea>,FPn                       ----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_100_0001
      //FSSQRT.W <ea>,FPn                       ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_100_0001
      //FSSQRT.D <ea>,FPn                       ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_100_0001
      //FSSQRT.B <ea>,FPn                       ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_100_0001
      //FDMOVE.L <ea>,FPn                       ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_100_0100
      //FDMOVE.S <ea>,FPn                       ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_100_0100
      //FDMOVE.X <ea>,FPn                       ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_100_0100
      //FDMOVE.P <ea>,FPn                       ----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_100_0100
      //FDMOVE.W <ea>,FPn                       ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_100_0100
      //FDMOVE.D <ea>,FPn                       ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_100_0100
      //FDMOVE.B <ea>,FPn                       ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_100_0100
      //FDSQRT.L <ea>,FPn                       ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_100_0101
      //FDSQRT.S <ea>,FPn                       ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_100_0101
      //FDSQRT.X <ea>,FPn                       ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_100_0101
      //FDSQRT.P <ea>,FPn                       ----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_100_0101
      //FDSQRT.W <ea>,FPn                       ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_100_0101
      //FDSQRT.D <ea>,FPn                       ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_100_0101
      //FDSQRT.B <ea>,FPn                       ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_100_0101
      //FSABS.L <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_101_1000
      //FSABS.S <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_101_1000
      //FSABS.X <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_101_1000
      //FSABS.P <ea>,FPn                        ----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_101_1000
      //FSABS.W <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_101_1000
      //FSABS.D <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_101_1000
      //FSABS.B <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_101_1000
      //FSNEG.L <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_101_1010
      //FSNEG.S <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_101_1010
      //FSNEG.X <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_101_1010
      //FSNEG.P <ea>,FPn                        ----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_101_1010
      //FSNEG.W <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_101_1010
      //FSNEG.D <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_101_1010
      //FSNEG.B <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_101_1010
      //FDABS.L <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_101_1100
      //FDABS.S <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_101_1100
      //FDABS.X <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_101_1100
      //FDABS.P <ea>,FPn                        ----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_101_1100
      //FDABS.W <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_101_1100
      //FDABS.D <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_101_1100
      //FDABS.B <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_101_1100
      //FDNEG.L <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_101_1110
      //FDNEG.S <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_101_1110
      //FDNEG.X <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_101_1110
      //FDNEG.P <ea>,FPn                        ----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_101_1110
      //FDNEG.W <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_101_1110
      //FDNEG.D <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_101_1110
      //FDNEG.B <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_101_1110
      //FSDIV.L <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_110_0000
      //FSDIV.S <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_110_0000
      //FSDIV.X <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_110_0000
      //FSDIV.P <ea>,FPn                        ----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_110_0000
      //FSDIV.W <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_110_0000
      //FSDIV.D <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_110_0000
      //FSDIV.B <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_110_0000
      //FSADD.L <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_110_0010
      //FSADD.S <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_110_0010
      //FSADD.X <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_110_0010
      //FSADD.P <ea>,FPn                        ----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_110_0010
      //FSADD.W <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_110_0010
      //FSADD.D <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_110_0010
      //FSADD.B <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_110_0010
      //FSMUL.L <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_110_0011
      //FSMUL.S <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_110_0011
      //FSMUL.X <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_110_0011
      //FSMUL.P <ea>,FPn                        ----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_110_0011
      //FSMUL.W <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_110_0011
      //FSMUL.D <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_110_0011
      //FSMUL.B <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_110_0011
      //FDDIV.L <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_110_0100
      //FDDIV.S <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_110_0100
      //FDDIV.X <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_110_0100
      //FDDIV.P <ea>,FPn                        ----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_110_0100
      //FDDIV.W <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_110_0100
      //FDDIV.D <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_110_0100
      //FDDIV.B <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_110_0100
      //FDADD.L <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_110_0110
      //FDADD.S <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_110_0110
      //FDADD.X <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_110_0110
      //FDADD.P <ea>,FPn                        ----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_110_0110
      //FDADD.W <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_110_0110
      //FDADD.D <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_110_0110
      //FDADD.B <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_110_0110
      //FDMUL.L <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_110_0111
      //FDMUL.S <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_110_0111
      //FDMUL.X <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_110_0111
      //FDMUL.P <ea>,FPn                        ----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_110_0111
      //FDMUL.W <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_110_0111
      //FDMUL.D <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_110_0111
      //FDMUL.B <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_110_0111
      //FSSUB.L <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_110_1000
      //FSSUB.S <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_110_1000
      //FSSUB.X <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_110_1000
      //FSSUB.P <ea>,FPn                        ----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_110_1000
      //FSSUB.W <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_110_1000
      //FSSUB.D <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_110_1000
      //FSSUB.B <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_110_1000
      //FDSUB.L <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_110_1100
      //FDSUB.S <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_110_1100
      //FDSUB.X <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_110_1100
      //FDSUB.P <ea>,FPn                        ----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_110_1100
      //FDSUB.W <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_110_1100
      //FDSUB.D <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_110_1100
      //FDSUB.B <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_110_1100
      //FMOVECR.X #ccc,FPn                      --CCSS|-|-----|-----|          |1111_001_000_000_000-010_111_nnn_0cc_cccc
      //FMOVE.L FPn,<ea>                        --CC46|-|-----|-----|D M+-WXZ  |1111_001_000_mmm_rrr-011_000_nnn_000_0000
      //FMOVE.S FPn,<ea>                        --CC46|-|-----|-----|D M+-WXZ  |1111_001_000_mmm_rrr-011_001_nnn_000_0000
      //FMOVE.X FPn,<ea>                        --CC46|-|-----|-----|  M+-WXZ  |1111_001_000_mmm_rrr-011_010_nnn_000_0000
      //FMOVE.P FPn,<ea>{#k}                    --CCSS|-|-----|-----|  M+-WXZ  |1111_001_000_mmm_rrr-011_011_nnn_kkk_kkkk
      //FMOVE.W FPn,<ea>                        --CC46|-|-----|-----|D M+-WXZ  |1111_001_000_mmm_rrr-011_100_nnn_000_0000
      //FMOVE.D FPn,<ea>                        --CC46|-|-----|-----|  M+-WXZ  |1111_001_000_mmm_rrr-011_101_nnn_000_0000
      //FMOVE.B FPn,<ea>                        --CC46|-|-----|-----|D M+-WXZ  |1111_001_000_mmm_rrr-011_110_nnn_000_0000
      //FMOVE.P FPn,<ea>{Dl}                    --CCSS|-|-----|-----|  M+-WXZ  |1111_001_000_mmm_rrr-011_111_nnn_lll_0000
      //FMOVE.L <ea>,FPIAR                      --CC46|-|-----|-----|DAM+-WXZPI|1111_001_000_mmm_rrr-100_001_000_000_0000
      //FMOVE.L <ea>,FPSR                       --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-100_010_000_000_0000
      //FMOVE.L <ea>,FPCR                       --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-100_100_000_000_0000
      //FMOVEM.L <ea>,FPIAR                     --CC46|-|-----|-----|DAM+-WXZPI|1111_001_000_mmm_rrr-100_001_000_000_0000
      //FMOVEM.L <ea>,FPSR                      --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-100_010_000_000_0000
      //FMOVEM.L <ea>,FPCR                      --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-100_100_000_000_0000
      //FMOVEM.L <ea>,FPSR/FPIAR                --CC46|-|-----|-----|  M+-WXZP |1111_001_000_mmm_rrr-100_011_000_000_0000
      //FMOVEM.L #<data>,#<data>,FPSR/FPIAR     --CC4S|-|-----|-----|         I|1111_001_000_111_100-100_011_000_000_0000-{data}
      //FMOVEM.L <ea>,FPCR/FPIAR                --CC46|-|-----|-----|  M+-WXZP |1111_001_000_mmm_rrr-100_101_000_000_0000
      //FMOVEM.L #<data>,#<data>,FPCR/FPIAR     --CC4S|-|-----|-----|         I|1111_001_000_111_100-100_101_000_000_0000-{data}
      //FMOVEM.L <ea>,FPCR/FPSR                 --CC46|-|-----|-----|  M+-WXZP |1111_001_000_mmm_rrr-100_110_000_000_0000
      //FMOVEM.L #<data>,#<data>,FPCR/FPSR      --CC4S|-|-----|-----|         I|1111_001_000_111_100-100_110_000_000_0000-{data}
      //FMOVEM.L <ea>,FPCR/FPSR/FPIAR           --CC46|-|-----|-----|  M+-WXZP |1111_001_000_mmm_rrr-100_111_000_000_0000
      //FMOVEM.L #<d>,#<d>,#<d>,FPCR/FPSR/FPIAR --CC4S|-|-----|-----|         I|1111_001_000_111_100-100_111_000_000_0000-{data}
      //FMOVE.L FPIAR,<ea>                      --CC46|-|-----|-----|DAM+-WXZ  |1111_001_000_mmm_rrr-101_001_000_000_0000
      //FMOVE.L FPSR,<ea>                       --CC46|-|-----|-----|D M+-WXZ  |1111_001_000_mmm_rrr-101_010_000_000_0000
      //FMOVE.L FPCR,<ea>                       --CC46|-|-----|-----|D M+-WXZ  |1111_001_000_mmm_rrr-101_100_000_000_0000
      //FMOVEM.L FPIAR,<ea>                     --CC46|-|-----|-----|DAM+-WXZ  |1111_001_000_mmm_rrr-101_001_000_000_0000
      //FMOVEM.L FPSR,<ea>                      --CC46|-|-----|-----|D M+-WXZ  |1111_001_000_mmm_rrr-101_010_000_000_0000
      //FMOVEM.L FPCR,<ea>                      --CC46|-|-----|-----|D M+-WXZ  |1111_001_000_mmm_rrr-101_100_000_000_0000
      //FMOVEM.L FPSR/FPIAR,<ea>                --CC46|-|-----|-----|  M+-WXZ  |1111_001_000_mmm_rrr-101_011_000_000_0000
      //FMOVEM.L FPCR/FPIAR,<ea>                --CC46|-|-----|-----|  M+-WXZ  |1111_001_000_mmm_rrr-101_101_000_000_0000
      //FMOVEM.L FPCR/FPSR,<ea>                 --CC46|-|-----|-----|  M+-WXZ  |1111_001_000_mmm_rrr-101_110_000_000_0000
      //FMOVEM.L FPCR/FPSR/FPIAR,<ea>           --CC46|-|-----|-----|  M+-WXZ  |1111_001_000_mmm_rrr-101_111_000_000_0000
      //FMOVEM.X <ea>,#<data>                   --CC46|-|-----|-----|  M+ WXZP |1111_001_000_mmm_rrr-110_100_00d_ddd_dddd
      //FMOVEM.X <ea>,<list>                    --CC46|-|-----|-----|  M+ WXZP |1111_001_000_mmm_rrr-110_100_00l_lll_llll
      //FMOVEM.X <ea>,Dl                        --CC4S|-|-----|-----|  M+ WXZP |1111_001_000_mmm_rrr-110_110_000_lll_0000
      //FMOVEM.X #<data>,-(Ar)                  --CC46|-|-----|-----|    -     |1111_001_000_100_rrr-111_000_00d_ddd_dddd
      //FMOVEM.X #<data>,<ea>                   --CC46|-|-----|-----|  M  WXZ  |1111_001_000_mmm_rrr-111_100_00d_ddd_dddd
      //FMOVEM.X <list>,-(Ar)                   --CC46|-|-----|-----|    -     |1111_001_000_100_rrr-111_000_00l_lll_llll
      //FMOVEM.X <list>,<ea>                    --CC46|-|-----|-----|  M  WXZ  |1111_001_000_mmm_rrr-111_100_00l_lll_llll
      //FMOVEM.X Dl,-(Ar)                       --CC4S|-|-----|-----|    -     |1111_001_000_100_rrr-111_010_000_lll_0000
      //FMOVEM.X Dl,<ea>                        --CC4S|-|-----|-----|  M  WXZ  |1111_001_000_mmm_rrr-111_110_000_lll_0000
    case 0b1111_001_000: return disFgen (sb);

      //FDBcc Dr,<label>                        --CC4S|-|-----|-----|          |1111_001_001_001_rrr-000_000_000_0cc_cccc-{offset}
      //FTRAPcc.W #<data>                       --CC4S|-|-----|-----|          |1111_001_001_111_010-000_000_000_0cc_cccc-{data}
      //FTRAPcc.L #<data>                       --CC4S|-|-----|-----|          |1111_001_001_111_011-000_000_000_0cc_cccc-{data}
      //FTRAPcc                                 --CC4S|-|-----|-----|          |1111_001_001_111_100-000_000_000_0cc_cccc
      //FScc.B <ea>                             --CC4S|-|-----|-----|D M+-WXZ  |1111_001_001_mmm_rrr-000_000_000_0cc_cccc
    case 0b1111_001_001: return disFscc (sb);

      //FNOP                                    --CC46|-|-----|-----|          |1111_001_010_000_000-000_000_000_000_0000
      //FBcc.W <label>                          --CC46|-|-----|-----|          |1111_001_010_ccc_ccc-{offset}
    case 0b1111_001_010: return disFbccWord (sb);

      //FBcc.L <label>                          --CC46|-|-----|-----|          |1111_001_011_ccc_ccc-{offset}
    case 0b1111_001_011: return disFbccLong (sb);

      //FSAVE <ea>                              --CC46|P|-----|-----|  M -WXZ  |1111_001_100_mmm_rrr
    case 0b1111_001_100: return disFsave (sb);

      //FRESTORE <ea>                           --CC46|P|-----|-----|  M+ WXZP |1111_001_101_mmm_rrr
    case 0b1111_001_101: return disFrestore (sb);

      //line 1111 emulator                      012346|-|-----|-----|          |1111_xxx_xxx_xxx_xxx
    case 0b1111_001_110:
    case 0b1111_001_111: return disFline (sb);

      //CINVL NC,(An)                           ----46|P|-----|-----|          |1111_010_000_001_nnn
      //CINVP NC,(An)                           ----46|P|-----|-----|          |1111_010_000_010_nnn
      //CINVA NC                                ----46|P|-----|-----|          |1111_010_000_011_000
      //CPUSHL NC,(An)                          ----46|P|-----|-----|          |1111_010_000_101_nnn
      //INTOUCH (An)                            ------|P|-----|-----|          |1111_010_000_101_nnn    (ISA_B)
      //CPUSHP NC,(An)                          ----46|P|-----|-----|          |1111_010_000_110_nnn
      //CPUSHA NC                               ----46|P|-----|-----|          |1111_010_000_111_000
    case 0b1111_010_000: return disCinvCpush (sb);

      //CINVL DC,(An)                           ----46|P|-----|-----|          |1111_010_001_001_nnn
      //CINVP DC,(An)                           ----46|P|-----|-----|          |1111_010_001_010_nnn
      //CINVA DC                                ----46|P|-----|-----|          |1111_010_001_011_000
      //CPUSHL DC,(An)                          ----46|P|-----|-----|          |1111_010_001_101_nnn
      //CPUSHP DC,(An)                          ----46|P|-----|-----|          |1111_010_001_110_nnn
      //CPUSHA DC                               ----46|P|-----|-----|          |1111_010_001_111_000
    case 0b1111_010_001: return disCinvCpush (sb);

      //CINVL IC,(An)                           ----46|P|-----|-----|          |1111_010_010_001_nnn
      //CINVP IC,(An)                           ----46|P|-----|-----|          |1111_010_010_010_nnn
      //CINVA IC                                ----46|P|-----|-----|          |1111_010_010_011_000
      //CPUSHL IC,(An)                          ----46|P|-----|-----|          |1111_010_010_101_nnn
      //CPUSHP IC,(An)                          ----46|P|-----|-----|          |1111_010_010_110_nnn
      //CPUSHA IC                               ----46|P|-----|-----|          |1111_010_010_111_000
    case 0b1111_010_010: return disCinvCpush (sb);

      //CINVL BC,(An)                           ----46|P|-----|-----|          |1111_010_011_001_nnn
      //CINVP BC,(An)                           ----46|P|-----|-----|          |1111_010_011_010_nnn
      //CINVA BC                                ----46|P|-----|-----|          |1111_010_011_011_000
      //CPUSHL BC,(An)                          ----46|P|-----|-----|          |1111_010_011_101_nnn
      //CPUSHP BC,(An)                          ----46|P|-----|-----|          |1111_010_011_110_nnn
      //CPUSHA BC                               ----46|P|-----|-----|          |1111_010_011_111_000
    case 0b1111_010_011: return disCinvCpush (sb);

      //PFLUSHN (An)                            ----46|P|-----|-----|          |1111_010_100_000_nnn
      //PFLUSH (An)                             ----46|P|-----|-----|          |1111_010_100_001_nnn
      //PFLUSHAN                                ----46|P|-----|-----|          |1111_010_100_010_000
      //PFLUSHA                                 ----46|P|-----|-----|          |1111_010_100_011_000
    case 0b1111_010_100: return disPflush (sb);

      //PTESTW (An)                             ----4-|P|-----|-----|          |1111_010_101_001_nnn
      //PTESTR (An)                             ----4-|P|-----|-----|          |1111_010_101_101_nnn
    case 0b1111_010_101: return disPtest (sb);

      //PLPAW (An)                              -----6|P|-----|-----|          |1111_010_110_001_nnn
    case 0b1111_010_110: return disPlpaw (sb);

      //PLPAR (An)                              -----6|P|-----|-----|          |1111_010_111_001_nnn
    case 0b1111_010_111: return disPlpar (sb);

      //MOVE16 (An)+,xxx.L                      ----46|-|-----|-----|          |1111_011_000_000_nnn-{address}
      //MOVE16 xxx.L,(An)+                      ----46|-|-----|-----|          |1111_011_000_001_nnn-{address}
      //MOVE16 (An),xxx.L                       ----46|-|-----|-----|          |1111_011_000_010_nnn-{address}
      //MOVE16 xxx.L,(An)                       ----46|-|-----|-----|          |1111_011_000_011_nnn-{address}
      //MOVE16 (Ax)+,(Ay)+                      ----46|-|-----|-----|          |1111_011_000_100_xxx-1yyy000000000000
    case 0b1111_011_000: return disMove16 (sb);

      //line 1111 emulator                      012346|-|-----|-----|          |1111_xxx_xxx_xxx_xxx
    case 0b1111_011_001:
    case 0b1111_011_010:
    case 0b1111_011_011:
    case 0b1111_011_100:
    case 0b1111_011_101:
    case 0b1111_011_110:
    case 0b1111_011_111: return disFline (sb);

      //LPSTOP.W #<data>                        -----6|P|-----|-----|          |1111_100_000_000_000-0000000111000000-{data}
    case 0b1111_100_000: return disLpstop (sb);

      //line 1111 emulator                      012346|-|-----|-----|          |1111_xxx_xxx_xxx_xxx
    case 0b1111_100_001:
    case 0b1111_100_010:
    case 0b1111_100_011:
    case 0b1111_100_100:
    case 0b1111_100_101:
    case 0b1111_100_110:
    case 0b1111_100_111:
    case 0b1111_101_000:
    case 0b1111_101_001:
    case 0b1111_101_010:
    case 0b1111_101_011: return disFline (sb);

      //WDDATA.B <ea>                           ------|-|-----|-----|  M+-WXZ  |1111_101_100_mmm_rrr    (ISA_A)
    case 0b1111_101_100: return disWddataByte (sb);

      //WDDATA.W <ea>                           ------|-|-----|-----|  M+-WXZ  |1111_101_101_mmm_rrr    (ISA_A)
    case 0b1111_101_101: return disWddataWord (sb);

      //WDDATA.L <ea>                           ------|-|-----|-----|  M+-WXZ  |1111_101_110_mmm_rrr    (ISA_A)
    case 0b1111_101_110: return disWddataLong (sb);

      //WDEBUG.L <ea>                           ------|P|-----|-----|  M  W    |1111_101_111_mmm_rrr-0000000000000011   (ISA_A)
    case 0b1111_101_111: return disWdebug (sb);

      //line 1111 emulator                      012346|-|-----|-----|          |1111_xxx_xxx_xxx_xxx
    case 0b1111_110_000:
    case 0b1111_110_001:
    case 0b1111_110_010:
    case 0b1111_110_011:
    case 0b1111_110_100:
    case 0b1111_110_101:
    case 0b1111_110_110:
    case 0b1111_110_111: return disFline (sb);

      //FPACK <name>                            012346|-|-----|-----|          |1111_111_0xx_xxx_xxx
      //line 1111 emulator                      012346|-|-----|-----|          |1111_xxx_xxx_xxx_xxx
    case 0b1111_111_000:
    case 0b1111_111_001:
    case 0b1111_111_010:
    case 0b1111_111_011: return disFpack (sb);

      //DOS <name>                              012346|-|-----|-----|          |1111_111_1xx_xxx_xxx
      //line 1111 emulator                      012346|-|-----|-----|          |1111_xxx_xxx_xxx_xxx
    case 0b1111_111_100:
    case 0b1111_111_101:
    case 0b1111_111_110:
    case 0b1111_111_111: return disDos (sb);

    }
    return disIllegal (sb);
  }  //disDisassemble(StringBuilder,int,int)

  //ORI.B #<data>,CCR                       012346|-|*****|*****|          |0000_000_000_111_100-{data}
  //ORI.B #<data>,<ea>                      012346|-|-UUUU|-**00|D M+-WXZ  |0000_000_000_mmm_rrr-{data}
  public static StringBuilder disOriByte (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DLT << ea < 0L) {
      return disEab (disIMb (sb.append ("ori.b   ")).append (','), ea);
    }
    if (ea == 0b111_100) {
      return disIMb (sb.append ("ori.b   ")).append (",ccr");
    }
    return disIllegal (sb);
  }  //disOriByte(StringBuilder)

  //ORI.W #<data>,SR                        012346|P|*****|*****|          |0000_000_001_111_100-{data}
  //ORI.W #<data>,<ea>                      012346|-|-UUUU|-**00|D M+-WXZ  |0000_000_001_mmm_rrr-{data}
  public static StringBuilder disOriWord (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DLT << ea < 0L) {
      return disEaw (disIMw (sb.append ("ori.w   ")).append (','), ea);
    }
    if (ea == 0b111_100) {
      return disIMw (sb.append ("ori.w   ")).append (",sr");
    }
    return disIllegal (sb);
  }  //disOriWord(StringBuilder)

  //ORI.L #<data>,<ea>                      012346|-|-UUUU|-**00|D M+-WXZ  |0000_000_010_mmm_rrr-{data}
  public static StringBuilder disOriLong (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DLT << ea < 0L) {
      return disEal (disIMl (sb.append ("ori.l   ")).append (','), ea);
    }
    return disIllegal (sb);
  }  //disOriLong(StringBuilder)

  //ANDI.B #<data>,CCR                      012346|-|*****|*****|          |0000_001_000_111_100-{data}
  //ANDI.B #<data>,<ea>                     012346|-|-UUUU|-**00|D M+-WXZ  |0000_001_000_mmm_rrr-{data}
  public static StringBuilder disAndiByte (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DLT << ea < 0L) {
      return disEab (disIMb (sb.append ("andi.b  ")).append (','), ea);
    }
    if (ea == 0b111_100) {
      return disIMb (sb.append ("andi.b  ")).append (",ccr");
    }
    return disIllegal (sb);
  }  //disAndiByte(StringBuilder)

  //ANDI.W #<data>,SR                       012346|P|*****|*****|          |0000_001_001_111_100-{data}
  //ANDI.W #<data>,<ea>                     012346|-|-UUUU|-**00|D M+-WXZ  |0000_001_001_mmm_rrr-{data}
  public static StringBuilder disAndiWord (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DLT << ea < 0L) {
      return disEaw (disIMw (sb.append ("andi.w  ")).append (','), ea);
    }
    if (ea == 0b111_100) {
      return disIMw (sb.append ("andi.w  ")).append (",sr");
    }
    return disIllegal (sb);
  }  //disAndiWord(StringBuilder)

  //ANDI.L #<data>,<ea>                     012346|-|-UUUU|-**00|D M+-WXZ  |0000_001_010_mmm_rrr-{data}
  public static StringBuilder disAndiLong (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DLT << ea < 0L) {
      return disEal (disIMl (sb.append ("andi.l  ")).append (','), ea);
    }
    return disIllegal (sb);
  }  //disAndiLong(StringBuilder)

  //SUBI.B #<data>,<ea>                     012346|-|UUUUU|*****|D M+-WXZ  |0000_010_000_mmm_rrr-{data}
  public static StringBuilder disSubiByte (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DLT << ea < 0L) {
      return disEab (disIMb (sb.append ("subi.b  ")).append (','), ea);
    }
    return disIllegal (sb);
  }  //disSubiByte(StringBuilder)

  //SUBI.W #<data>,<ea>                     012346|-|UUUUU|*****|D M+-WXZ  |0000_010_001_mmm_rrr-{data}
  public static StringBuilder disSubiWord (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DLT << ea < 0L) {
      return disEaw (disIMw (sb.append ("subi.w  ")).append (','), ea);
    }
    return disIllegal (sb);
  }  //disSubiWord(StringBuilder)

  //SUBI.L #<data>,<ea>                     012346|-|UUUUU|*****|D M+-WXZ  |0000_010_010_mmm_rrr-{data}
  public static StringBuilder disSubiLong (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DLT << ea < 0L) {
      return disEal (disIMl (sb.append ("subi.l  ")).append (','), ea);
    }
    return disIllegal (sb);
  }  //disSubiLong(StringBuilder)

  //BITREV.L Dr                             ------|-|-----|-----|          |0000_000_011_000_rrr    (ISA_C)
  //CMP2.B <ea>,Rn                          --234S|-|-UUUU|-U*U*|  M  WXZP |0000_000_011_mmm_rrr-rnnn_000_000_000_000
  //CHK2.B <ea>,Rn                          --234S|-|-UUUU|-U*U*|  M  WXZP |0000_000_011_mmm_rrr-rnnn_100_000_000_000
  public static StringBuilder disCmp2Chk2Byte (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_CNT << ea < 0L) {
      int w = MC68060.mmuPeekWordZeroCode ((disPC += 2) - 2, disSupervisor);  //拡張ワード
      if ((w & ~0b1111_100_000_000_000) == 0) {
        return disRn (disEab (sb.append ((w & 0b0000_100_000_000_000) == 0 ? "cmp2.b  " : "chk2.b  "), ea).append (','), w >> 12);
      }
      return disIllegal (sb);
    }
    int mmm = ea >> 3;
    int rrr = ea & 7;
    if (mmm == XEiJ.MMM_DR) {
      return disDR (sb.append ("bitrev.l "), rrr);
    }
    return disIllegal (sb);
  }  //disCmp2Chk2Byte(StringBuilder)

  //BYTEREV.L Dr                            ------|-|-----|-----|          |0000_001_011_000_rrr    (ISA_C)
  //CMP2.W <ea>,Rn                          --234S|-|-UUUU|-U*U*|  M  WXZP |0000_001_011_mmm_rrr-rnnn_000_000_000_000
  //CHK2.W <ea>,Rn                          --234S|-|-UUUU|-U*U*|  M  WXZP |0000_001_011_mmm_rrr-rnnn_100_000_000_000
  public static StringBuilder disCmp2Chk2Word (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_CNT << ea < 0L) {
      int w = MC68060.mmuPeekWordZeroCode ((disPC += 2) - 2, disSupervisor);  //拡張ワード
      if ((w & ~0b1111_100_000_000_000) == 0) {
        return disRn (disEaw (sb.append ((w & 0b0000_100_000_000_000) == 0 ? "cmp2.w  " : "chk2.w  "), ea).append (','), w >> 12);
      }
      return disIllegal (sb);
    }
    int mmm = ea >> 3;
    int rrr = ea & 7;
    if (mmm == XEiJ.MMM_DR) {
      return disDR (sb.append ("byterev.l "), rrr);
    }
    return disIllegal (sb);
  }  //disCmp2Chk2Word(StringBuilder)

  //FF1.L Dr                                ------|-|-UUUU|-**00|          |0000_010_011_000_rrr    (ISA_C)
  //CMP2.L <ea>,Rn                          --234S|-|-UUUU|-U*U*|  M  WXZP |0000_010_011_mmm_rrr-rnnn_000_000_000_000
  //CHK2.L <ea>,Rn                          --234S|-|-UUUU|-U*U*|  M  WXZP |0000_010_011_mmm_rrr-rnnn_100_000_000_000
  public static StringBuilder disCmp2Chk2Long (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_CNT << ea < 0L) {
      int w = MC68060.mmuPeekWordZeroCode ((disPC += 2) - 2, disSupervisor);  //拡張ワード
      if ((w & ~0b1111_100_000_000_000) == 0) {
        return disRn (disEal (sb.append ((w & 0b0000_100_000_000_000) == 0 ? "cmp2.l  " : "chk2.l  "), ea).append (','), w >> 12);
      }
      return disIllegal (sb);
    }
    int mmm = ea >> 3;
    int rrr = ea & 7;
    if (mmm == XEiJ.MMM_DR) {
      return disDR (sb.append ("ff1.l   "), rrr);
    }
    return disIllegal (sb);
  }  //disCmp2Chk2Long(StringBuilder)

  //ADDI.B #<data>,<ea>                     012346|-|UUUUU|*****|D M+-WXZ  |0000_011_000_mmm_rrr-{data}
  public static StringBuilder disAddiByte (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DLT << ea < 0L) {
      return disEab (disIMb (sb.append ("addi.b  ")).append (','), ea);
    }
    return disIllegal (sb);
  }  //disAddiByte(StringBuilder)

  //ADDI.W #<data>,<ea>                     012346|-|UUUUU|*****|D M+-WXZ  |0000_011_001_mmm_rrr-{data}
  public static StringBuilder disAddiWord (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DLT << ea < 0L) {
      return disEaw (disIMw (sb.append ("addi.w  ")).append (','), ea);
    }
    return disIllegal (sb);
  }  //disAddiWord(StringBuilder)

  //ADDI.L #<data>,<ea>                     012346|-|UUUUU|*****|D M+-WXZ  |0000_011_010_mmm_rrr-{data}
  public static StringBuilder disAddiLong (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DLT << ea < 0L) {
      return disEal (disIMl (sb.append ("addi.l  ")).append (','), ea);
    }
    return disIllegal (sb);
  }  //disAddiLong(StringBuilder)

  //RTM Rn                                  --2---|-|UUUUU|*****|          |0000_011_011_00n_nnn
  //CALLM #<data>,<ea>                      --2---|-|-----|-----|  M  WXZP |0000_011_011_mmm_rrr-0000_000_0dd_ddd_ddd
  public static StringBuilder disCallm (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_CNT << ea < 0L) {
      return disEaw (disIMb (sb.append ("callm   ")).append (','), ea);
    }
    if (ea <= 15) {
      return disRn (sb.append ("rtm     "), ea);
    }
    return disIllegal (sb);
  }  //disCallm(StringBuilder)

  public static final String[] DIS_BXXX = { "btst", "bchg", "bclr", "bset" };

  //BTST.L #<data>,Dr                       012346|-|--U--|--*--|          |0000_100_000_000_rrr-{data}
  //BTST.B #<data>,<ea>                     012346|-|--U--|--*--|  M+-WXZP |0000_100_000_mmm_rrr-{data}
  public static StringBuilder disBtstImm (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_MEM << ea < 0L) {
      return disEab (disIMb (sb.append ("btst.b  ")).append (','), ea);
    }
    int mmm = ea >> 3;
    int rrr = ea & 7;
    if (mmm == XEiJ.MMM_DR) {
      return disDR (disIMb (sb.append ("btst.l  ")).append (','), rrr);
    }
    return disIllegal (sb);
  }  //disBtstImm(StringBuilder)

  //BCHG.L #<data>,Dr                       012346|-|--U--|--*--|          |0000_100_001_000_rrr-{data}
  //BCHG.B #<data>,<ea>                     012346|-|--U--|--*--|  M+-WXZ  |0000_100_001_mmm_rrr-{data}
  public static StringBuilder disBchgImm (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_MLT << ea < 0L) {
      return disEab (disIMb (sb.append ("bchg.b  ")).append (','), ea);
    }
    int mmm = ea >> 3;
    int rrr = ea & 7;
    if (mmm == XEiJ.MMM_DR) {
      return disDR (disIMb (sb.append ("bchg.l  ")).append (','), rrr);
    }
    return disIllegal (sb);
  }  //disBchgImm(StringBuilder)

  //BCLR.L #<data>,Dr                       012346|-|--U--|--*--|          |0000_100_010_000_rrr-{data}
  //BCLR.B #<data>,<ea>                     012346|-|--U--|--*--|  M+-WXZ  |0000_100_010_mmm_rrr-{data}
  public static StringBuilder disBclrImm (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_MLT << ea < 0L) {
      return disEab (disIMb (sb.append ("bclr.b  ")).append (','), ea);
    }
    int mmm = ea >> 3;
    int rrr = ea & 7;
    if (mmm == XEiJ.MMM_DR) {
      return disDR (disIMb (sb.append ("bclr.l  ")).append (','), rrr);
    }
    return disIllegal (sb);
  }  //disBclrImm(StringBuilder)

  //BSET.L #<data>,Dr                       012346|-|--U--|--*--|          |0000_100_011_000_rrr-{data}
  //BSET.B #<data>,<ea>                     012346|-|--U--|--*--|  M+-WXZ  |0000_100_011_mmm_rrr-{data}
  public static StringBuilder disBsetImm (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_MLT << ea < 0L) {
      return disEab (disIMb (sb.append ("bset.b  ")).append (','), ea);
    }
    int mmm = ea >> 3;
    int rrr = ea & 7;
    if (mmm == XEiJ.MMM_DR) {
      return disDR (disIMb (sb.append ("bset.l  ")).append (','), rrr);
    }
    return disIllegal (sb);
  }  //disBsetImm(StringBuilder)

  //EORI.B #<data>,CCR                      012346|-|*****|*****|          |0000_101_000_111_100-{data}
  //EORI.B #<data>,<ea>                     012346|-|-UUUU|-**00|D M+-WXZ  |0000_101_000_mmm_rrr-{data}
  public static StringBuilder disEoriByte (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DLT << ea < 0L) {
      return disEab (disIMb (sb.append ("eori.b  ")).append (','), ea);
    }
    if (ea == 0b111_100) {
      return disIMb (sb.append ("eori.b  ")).append (",ccr");
    }
    return disIllegal (sb);
  }  //disEoriByte(StringBuilder)

  //EORI.W #<data>,SR                       012346|P|*****|*****|          |0000_101_001_111_100-{data}
  //EORI.W #<data>,<ea>                     012346|-|-UUUU|-**00|D M+-WXZ  |0000_101_001_mmm_rrr-{data}
  public static StringBuilder disEoriWord (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DLT << ea < 0L) {
      return disEaw (disIMw (sb.append ("eori.w  ")).append (','), ea);
    }
    if (ea == 0b111_100) {
      return disIMw (sb.append ("eori.w  ")).append (",sr");
    }
    return disIllegal (sb);
  }  //disEoriWord(StringBuilder)

  //EORI.L #<data>,<ea>                     012346|-|-UUUU|-**00|D M+-WXZ  |0000_101_010_mmm_rrr-{data}
  public static StringBuilder disEoriLong (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DLT << ea < 0L) {
      return disEal (disIMl (sb.append ("eori.l  ")).append (','), ea);
    }
    return disIllegal (sb);
  }  //disEoriLong(StringBuilder)

  //CAS.B Dc,Du,<ea>                        --2346|-|-UUUU|-****|  M+-WXZ  |0000_101_011_mmm_rrr-0000_000_uuu_000_ccc
  public static StringBuilder disCasByte (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_MLT << ea < 0L) {
      int w = MC68060.mmuPeekWordZeroCode ((disPC += 2) - 2, disSupervisor);
      if ((w & ~0b0000_000_111_000_111) == 0) {
        return disEab (disDR (disDR (sb.append ("cas.b   "),
                                     w & 7).append (','),
                              w >> 6 & 7).append (','),
                       ea);
      }
    }
    return disIllegal (sb);
  }  //disCasByte(StringBuilder)

  //CMPI.B #<data>,<ea>                     012346|-|-UUUU|-****|D M+-WXZ  |0000_110_000_mmm_rrr-{data}
  //CMPI.B #<data>,<ea>                     --2346|-|-UUUU|-****|        P |0000_110_000_mmm_rrr-{data}
  public static StringBuilder disCmpiByte (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DME << ea < 0L) {
      return disEab (disIMb (sb.append ("cmpi.b  ")).append (','), ea);
    }
    return disIllegal (sb);
  }  //disCmpiByte(StringBuilder)

  //CMPI.W #<data>,<ea>                     012346|-|-UUUU|-****|D M+-WXZ  |0000_110_001_mmm_rrr-{data}
  //CMPI.W #<data>,<ea>                     --2346|-|-UUUU|-****|        P |0000_110_001_mmm_rrr-{data}
  public static StringBuilder disCmpiWord (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DME << ea < 0L) {
      return disEaw (disIMw (sb.append ("cmpi.w  ")).append (','), ea);
    }
    return disIllegal (sb);
  }  //disCmpiWord(StringBuilder)

  //CMPI.L #<data>,<ea>                     012346|-|-UUUU|-****|D M+-WXZ  |0000_110_010_mmm_rrr-{data}
  //CMPI.L #<data>,<ea>                     --2346|-|-UUUU|-****|        P |0000_110_010_mmm_rrr-{data}
  public static StringBuilder disCmpiLong (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DME << ea < 0L) {
      return disEal (disIMl (sb.append ("cmpi.l  ")).append (','), ea);
    }
    return disIllegal (sb);
  }  //disCmpiLong(StringBuilder)

  //CAS2.W Dc1:Dc2,Du1:Du2,(Rn1):(Rn2)      --234S|-|-UUUU|-****|          |0000_110_011_111_100-rnnn_000_uuu_000_ccc(1)-rnnn_000_uuu_000_ccc(2)
  //CAS.W Dc,Du,<ea>                        --2346|-|-UUUU|-****|  M+-WXZ  |0000_110_011_mmm_rrr-0000_000_uuu_000_ccc       (68060 software emulate misaligned <ea>)
  public static StringBuilder disCasWord (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_MLT << ea < 0L) {
      int w = MC68060.mmuPeekWordZeroCode ((disPC += 2) - 2, disSupervisor);
      if ((w & ~0b0000_000_111_000_111) == 0) {
        return disEaw (disDR (disDR (sb.append ("cas.w   "),
                                     w & 7).append (','),
                              w >> 6 & 7).append (','),
                       ea);
      }
    } else if (ea == 0b111_100) {
      int w1 = MC68060.mmuPeekWordZeroCode ((disPC += 2) - 2, disSupervisor);
      if ((w1 & ~0b1111_000_111_000_111) == 0) {
        int w2 = MC68060.mmuPeekWordZeroCode ((disPC += 2) - 2, disSupervisor);
        if ((w2 & ~0b1111_000_111_000_111) == 0) {
          return disRn (disRn (disDR (disDR (disDR (disDR (sb.append ("cas2.w  "),
                                                           w1 & 7).append (':'),
                                                    w2 & 7).append (','),
                                             w1 >> 6 & 7).append (':'),
                                      w2 >> 6 & 7).append (",("),
                               w1 >> 12).append ("):("),
                        w2 >> 12).append (')');
        }
      }
    }
    return disIllegal (sb);
  }  //disCasWord(StringBuilder)

  //MOVES.B <ea>,Rn                         -12346|P|-----|-----|  M+-WXZ  |0000_111_000_mmm_rrr-rnnn_000_000_000_000
  //MOVES.B Rn,<ea>                         -12346|P|-----|-----|  M+-WXZ  |0000_111_000_mmm_rrr-rnnn_100_000_000_000
  //  MOVES.B An,(An)+とMOVES.B An,-(An)で書き込まれるAnが更新前か更新後かは定義されていない
  public static StringBuilder disMovesByte (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_MLT << ea < 0L) {
      int w = MC68060.mmuPeekWordZeroCode ((disPC += 2) - 2, disSupervisor);
      if ((w & ~0b1111_100_000_000_000) == 0) {
        if ((w & 0b0000_100_000_000_000) == 0) {
          return disRn (disEab (sb.append ("moves.b "), ea).append (','), w >> 12);
        } else {
          return disEab (disRn (sb.append ("moves.b "), w >> 12).append (','), ea);
        }
      }
    }
    return disIllegal (sb);
  }  //disMovesByte(StringBuilder)

  //MOVES.W <ea>,Rn                         -12346|P|-----|-----|  M+-WXZ  |0000_111_001_mmm_rrr-rnnn_000_000_000_000
  //MOVES.W Rn,<ea>                         -12346|P|-----|-----|  M+-WXZ  |0000_111_001_mmm_rrr-rnnn_100_000_000_000
  //  MOVES.W An,(An)+とMOVES.W An,-(An)で書き込まれるAnが更新前か更新後かは定義されていない
  public static StringBuilder disMovesWord (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_MLT << ea < 0L) {
      int w = MC68060.mmuPeekWordZeroCode ((disPC += 2) - 2, disSupervisor);
      if ((w & ~0b1111_100_000_000_000) == 0) {
        if ((w & 0b0000_100_000_000_000) == 0) {
          return disRn (disEaw (sb.append ("moves.w "), ea).append (','), w >> 12);
        } else {
          return disEaw (disRn (sb.append ("moves.w "), w >> 12).append (','), ea);
        }
      }
    }
    return disIllegal (sb);
  }  //disMovesWord(StringBuilder)

  //MOVES.L <ea>,Rn                         -12346|P|-----|-----|  M+-WXZ  |0000_111_010_mmm_rrr-rnnn_000_000_000_000
  //MOVES.L Rn,<ea>                         -12346|P|-----|-----|  M+-WXZ  |0000_111_010_mmm_rrr-rnnn_100_000_000_000
  //  MOVES.L An,(An)+とMOVES.L An,-(An)で書き込まれるAnが更新前か更新後かは定義されていない
  public static StringBuilder disMovesLong (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_MLT << ea < 0L) {
      int w = MC68060.mmuPeekWordZeroCode ((disPC += 2) - 2, disSupervisor);
      if ((w & ~0b1111_100_000_000_000) == 0) {
        if ((w & 0b0000_100_000_000_000) == 0) {
          return disRn (disEal (sb.append ("moves.l "), ea).append (','), w >> 12);
        } else {
          return disEal (disRn (sb.append ("moves.l "), w >> 12).append (','), ea);
        }
      }
    }
    return disIllegal (sb);
  }  //disMovesLong(StringBuilder)

  //CAS2.L Dc1:Dc2,Du1:Du2,(Rn1):(Rn2)      --234S|-|-UUUU|-****|          |0000_111_011_111_100-rnnn_000_uuu_000_ccc(1)-rnnn_000_uuu_000_ccc(2)
  //CAS.L Dc,Du,<ea>                        --2346|-|-UUUU|-****|  M+-WXZ  |0000_111_011_mmm_rrr-0000_000_uuu_000_ccc       (68060 software emulate misaligned <ea>)
  public static StringBuilder disCasLong (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_MLT << ea < 0L) {
      int w = MC68060.mmuPeekWordZeroCode ((disPC += 2) - 2, disSupervisor);
      if ((w & ~0b0000_000_111_000_111) == 0) {
        return disEal (disDR (disDR (sb.append ("cas.l   "),
                                     w & 7).append (','),
                              w >> 6 & 7).append (','),
                       ea);
      }
    } else if (ea == 0b111_100) {
      int w1 = MC68060.mmuPeekWordZeroCode ((disPC += 2) - 2, disSupervisor);
      if ((w1 & ~0b1111_000_111_000_111) == 0) {
        int w2 = MC68060.mmuPeekWordZeroCode ((disPC += 2) - 2, disSupervisor);
        if ((w2 & ~0b1111_000_111_000_111) == 0) {
          return disRn (disRn (disDR (disDR (disDR (disDR (sb.append ("cas2.l  "),
                                                           w1 & 7).append (':'),
                                                    w2 & 7).append (','),
                                             w1 >> 6 & 7).append (':'),
                                      w2 >> 6 & 7).append (",("),
                               w1 >> 12).append ("):("),
                        w2 >> 12).append (')');
        }
      }
    }
    return disIllegal (sb);
  }  //disCasLong(StringBuilder)

  //BTST.L Dq,Dr                            012346|-|--U--|--*--|          |0000_qqq_100_000_rrr
  //MOVEP.W (d16,Ar),Dq                     01234S|-|-----|-----|          |0000_qqq_100_001_rrr-{data}
  //BTST.B Dq,<ea>                          012346|-|--U--|--*--|  M+-WXZPI|0000_qqq_100_mmm_rrr
  public static StringBuilder disBtstReg (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_ANY << ea < 0L) {
      return disEab (disDR (sb.append ("btst.b  "), qqq).append (','), ea);
    }
    int mmm = ea >> 3;
    int rrr = ea & 7;
    if (mmm == XEiJ.MMM_DR) {
      return disDR (disDR (sb.append ("btst.l  "), qqq).append (','), rrr);
    }
    if (mmm == XEiJ.MMM_AR) {
      return disDR (disMW (sb.append ("movep.w "), rrr).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disBtstReg(StringBuilder)

  //BCHG.L Dq,Dr                            012346|-|--U--|--*--|          |0000_qqq_101_000_rrr
  //MOVEP.L (d16,Ar),Dq                     01234S|-|-----|-----|          |0000_qqq_101_001_rrr-{data}
  //BCHG.B Dq,<ea>                          012346|-|--U--|--*--|  M+-WXZ  |0000_qqq_101_mmm_rrr
  public static StringBuilder disBchgReg (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_MLT << ea < 0L) {
      return disEab (disDR (sb.append ("bchg.b  "), qqq).append (','), ea);
    }
    int mmm = ea >> 3;
    int rrr = ea & 7;
    if (mmm == XEiJ.MMM_DR) {
      return disDR (disDR (sb.append ("bchg.l  "), qqq).append (','), rrr);
    }
    if (mmm == XEiJ.MMM_AR) {
      return disDR (disMW (sb.append ("movep.l "), rrr).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disBchgReg(StringBuilder)

  //BCLR.L Dq,Dr                            012346|-|--U--|--*--|          |0000_qqq_110_000_rrr
  //MOVEP.W Dq,(d16,Ar)                     01234S|-|-----|-----|          |0000_qqq_110_001_rrr-{data}
  //BCLR.B Dq,<ea>                          012346|-|--U--|--*--|  M+-WXZ  |0000_qqq_110_mmm_rrr
  public static StringBuilder disBclrReg (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_MLT << ea < 0L) {
      return disEab (disDR (sb.append ("bclr.b  "), qqq).append (','), ea);
    }
    int mmm = ea >> 3;
    int rrr = ea & 7;
    if (mmm == XEiJ.MMM_DR) {
      return disDR (disDR (sb.append ("bclr.l  "), qqq).append (','), rrr);
    }
    if (mmm == XEiJ.MMM_AR) {
      return disMW (disDR (sb.append ("movep.w "), qqq).append (','), rrr);
    }
    return disIllegal (sb);
  }  //disBclrReg(StringBuilder)

  //BSET.L Dq,Dr                            012346|-|--U--|--*--|          |0000_qqq_111_000_rrr
  //MOVEP.L Dq,(d16,Ar)                     01234S|-|-----|-----|          |0000_qqq_111_001_rrr-{data}
  //BSET.B Dq,<ea>                          012346|-|--U--|--*--|  M+-WXZ  |0000_qqq_111_mmm_rrr
  public static StringBuilder disBsetReg (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_MLT << ea < 0L) {
      return disEab (disDR (sb.append ("bset.b  "), qqq).append (','), ea);
    }
    int mmm = ea >> 3;
    int rrr = ea & 7;
    if (mmm == XEiJ.MMM_DR) {
      return disDR (disDR (sb.append ("bset.l  "), qqq).append (','), rrr);
    }
    if (mmm == XEiJ.MMM_AR) {
      return disMW (disDR (sb.append ("movep.l "), qqq).append (','), rrr);
    }
    return disIllegal (sb);
  }  //disBsetReg(StringBuilder)

  //MOVE.B <ea>,Dq                          012346|-|-UUUU|-**00|D M+-WXZPI|0001_qqq_000_mmm_rrr
  public static StringBuilder disMoveToRegByte (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_DAT << ea < 0L) {
      return disDR (disEab (sb.append ("move.b  "), ea).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disMoveToRegByte(StringBuilder)

  //MOVE.B <ea>,<nnnqqq>                    012346|-|-UUUU|-**00|D M+-WXZPI|0001_qqq_nnn_mmm_rrr        (nnnqqq:M+-WXZ)
  public static StringBuilder disMoveToMemByte (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int nnn = disOC >> 6 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_DAT << ea < 0L) {
      return disEab (disEab (sb.append ("move.b  "), ea).append (','), nnn << 3 | qqq);
    }
    return disIllegal (sb);
  }  //disMoveToMemByte(StringBuilder)

  //MOVE.L <ea>,Dq                          012346|-|-UUUU|-**00|DAM+-WXZPI|0010_qqq_000_mmm_rrr
  public static StringBuilder disMoveToRegLong (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_ALL << ea < 0L) {
      return disDR (disEal (sb.append ("move.l  "), ea).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disMoveToRegLong(StringBuilder)

  //MOVEA.L <ea>,Aq                         012346|-|-----|-----|DAM+-WXZPI|0010_qqq_001_mmm_rrr
  public static StringBuilder disMoveaLong (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_ALL << ea < 0L) {
      return disAR (disEal (sb.append ("movea.l "), ea).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disMoveaLong(StringBuilder)

  //MOVE.L <ea>,<nnnqqq>                    012346|-|-UUUU|-**00|DAM+-WXZPI|0010_qqq_nnn_mmm_rrr        (nnnqqq:M+-WXZ)
  public static StringBuilder disMoveToMemLong (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int nnn = disOC >> 6 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_ALL << ea < 0L) {
      return disEal (disEal (sb.append ("move.l  "), ea).append (','), nnn << 3 | qqq);
    }
    return disIllegal (sb);
  }  //disMoveToMemLong(StringBuilder)

  //MOVE.W <ea>,Dq                          012346|-|-UUUU|-**00|DAM+-WXZPI|0011_qqq_000_mmm_rrr
  public static StringBuilder disMoveToRegWord (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_ALL << ea < 0L) {
      return disDR (disEaw (sb.append ("move.w  "), ea).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disMoveToRegWord(StringBuilder)

  //MOVEA.W <ea>,Aq                         012346|-|-----|-----|DAM+-WXZPI|0011_qqq_001_mmm_rrr
  public static StringBuilder disMoveaWord (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_ALL << ea < 0L) {
      return disAR (disEaw (sb.append ("movea.w "), ea).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disMoveaWord(StringBuilder)

  //MOVE.W <ea>,<nnnqqq>                    012346|-|-UUUU|-**00|DAM+-WXZPI|0011_qqq_nnn_mmm_rrr        (nnnqqq:M+-WXZ)
  public static StringBuilder disMoveToMemWord (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int nnn = disOC >> 6 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_ALL << ea < 0L) {
      return disEaw (disEaw (sb.append ("move.w  "), ea).append (','), nnn << 3 | qqq);
    }
    return disIllegal (sb);
  }  //disMoveToMemWord(StringBuilder)

  //NEGX.B <ea>                             012346|-|*UUUU|*****|D M+-WXZ  |0100_000_000_mmm_rrr
  public static StringBuilder disNegxByte (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DLT << ea < 0L) {
      return disEab (sb.append ("negx.b  "), ea);
    }
    return disIllegal (sb);
  }  //disNegxByte(StringBuilder)

  //NEGX.W <ea>                             012346|-|*UUUU|*****|D M+-WXZ  |0100_000_001_mmm_rrr
  public static StringBuilder disNegxWord (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DLT << ea < 0L) {
      return disEaw (sb.append ("negx.w  "), ea);
    }
    return disIllegal (sb);
  }  //disNegxWord(StringBuilder)

  //NEGX.L <ea>                             012346|-|*UUUU|*****|D M+-WXZ  |0100_000_010_mmm_rrr
  public static StringBuilder disNegxLong (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DLT << ea < 0L) {
      return disEal (sb.append ("negx.l  "), ea);
    }
    return disIllegal (sb);
  }  //disNegxLong(StringBuilder)

  //STRLDSR.W #<data>                       ------|P|*****|*****|          |0100_000_011_100_111-0100_011_011_111_100-{data}    (ISA_C)
  //MOVE.W SR,<ea>                          0-----|-|*****|-----|D M+-WXZ  |0100_000_011_mmm_rrr        (68000 and 68008 read before move)
  //MOVE.W SR,<ea>                          -12346|P|*****|-----|D M+-WXZ  |0100_000_011_mmm_rrr
  public static StringBuilder disMoveFromSR (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DLT << ea < 0L) {
      return disEaw (sb.append ("move.w  sr,"), ea);
    }
    return disIllegal (sb);
  }  //disMoveFromSR(StringBuilder)

  //CLR.B <ea>                              012346|-|-UUUU|-0100|D M+-WXZ  |0100_001_000_mmm_rrr        (68000 and 68008 read before clear)
  public static StringBuilder disClrByte (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DLT << ea < 0L) {
      return disEab (sb.append ("clr.b   "), ea);
    }
    return disIllegal (sb);
  }  //disClrByte(StringBuilder)

  //CLR.W <ea>                              012346|-|-UUUU|-0100|D M+-WXZ  |0100_001_001_mmm_rrr        (68000 and 68008 read before clear)
  public static StringBuilder disClrWord (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DLT << ea < 0L) {
      return disEaw (sb.append ("clr.w   "), ea);
    }
    return disIllegal (sb);
  }  //disClrWord(StringBuilder)

  //CLR.L <ea>                              012346|-|-UUUU|-0100|D M+-WXZ  |0100_001_010_mmm_rrr        (68000 and 68008 read before clear)
  public static StringBuilder disClrLong (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DLT << ea < 0L) {
      return disEal (sb.append ("clr.l   "), ea);
    }
    return disIllegal (sb);
  }  //disClrLong(StringBuilder)

  //MOVE.W CCR,<ea>                         -12346|-|*****|-----|D M+-WXZ  |0100_001_011_mmm_rrr
  public static StringBuilder disMoveFromCCR (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DLT << ea < 0L) {
      return disEaw (sb.append ("move.w  ccr,"), ea);
    }
    return disIllegal (sb);
  }  //disMoveFromCCR(StringBuilder)

  //NEG.B <ea>                              012346|-|UUUUU|*****|D M+-WXZ  |0100_010_000_mmm_rrr
  public static StringBuilder disNegByte (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DLT << ea < 0L) {
      return disEab (sb.append ("neg.b   "), ea);
    }
    return disIllegal (sb);
  }  //disNegByte(StringBuilder)

  //NEG.W <ea>                              012346|-|UUUUU|*****|D M+-WXZ  |0100_010_001_mmm_rrr
  public static StringBuilder disNegWord (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DLT << ea < 0L) {
      return disEaw (sb.append ("neg.w   "), ea);
    }
    return disIllegal (sb);
  }  //disNegWord(StringBuilder)

  //NEG.L <ea>                              012346|-|UUUUU|*****|D M+-WXZ  |0100_010_010_mmm_rrr
  public static StringBuilder disNegLong (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DLT << ea < 0L) {
      return disEal (sb.append ("neg.l   "), ea);
    }
    return disIllegal (sb);
  }  //disNegLong(StringBuilder)

  //MOVE.W <ea>,CCR                         012346|-|UUUUU|*****|D M+-WXZPI|0100_010_011_mmm_rrr
  public static StringBuilder disMoveToCCR (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DAT << ea < 0L) {
      return disEaw (sb.append ("move.w  "), ea).append (",ccr");
    }
    return disIllegal (sb);
  }  //disMoveToCCR(StringBuilder)

  //NOT.B <ea>                              012346|-|-UUUU|-**00|D M+-WXZ  |0100_011_000_mmm_rrr
  public static StringBuilder disNotByte (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DLT << ea < 0L) {
      return disEab (sb.append ("not.b   "), ea);
    }
    return disIllegal (sb);
  }  //disNotByte(StringBuilder)

  //NOT.W <ea>                              012346|-|-UUUU|-**00|D M+-WXZ  |0100_011_001_mmm_rrr
  public static StringBuilder disNotWord (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DLT << ea < 0L) {
      return disEaw (sb.append ("not.w   "), ea);
    }
    return disIllegal (sb);
  }  //disNotWord(StringBuilder)

  //NOT.L <ea>                              012346|-|-UUUU|-**00|D M+-WXZ  |0100_011_010_mmm_rrr
  public static StringBuilder disNotLong (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DLT << ea < 0L) {
      return disEal (sb.append ("not.l   "), ea);
    }
    return disIllegal (sb);
  }  //disNotLong(StringBuilder)

  //MOVE.W <ea>,SR                          012346|P|UUUUU|*****|D M+-WXZPI|0100_011_011_mmm_rrr
  public static StringBuilder disMoveToSR (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DAT << ea < 0L) {
      return disEaw (sb.append ("move.w  "), ea).append (",sr");
    }
    return disIllegal (sb);
  }  //disMoveToSR(StringBuilder)

  //LINK.L Ar,#<data>                       --2346|-|-----|-----|          |0100_100_000_001_rrr-{data}
  //NBCD.B <ea>                             012346|-|UUUUU|*U*U*|D M+-WXZ  |0100_100_000_mmm_rrr
  public static StringBuilder disNbcd (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DLT << ea < 0L) {
      return disEab (sb.append ("nbcd.b  "), ea);
    }
    int rrr = ea & 7;
    if (ea == XEiJ.EA_AR) {
      return disIMl (disAR (sb.append ("link.l  "), rrr).append (","));
    }
    return disIllegal (sb);
  }  //disNbcd(StringBuilder)

  //SWAP.W Dr                               012346|-|-UUUU|-**00|          |0100_100_001_000_rrr
  //BKPT #<data>                            -12346|-|-----|-----|          |0100_100_001_001_ddd
  //PEA.L <ea>                              012346|-|-----|-----|  M  WXZP |0100_100_001_mmm_rrr
  public static StringBuilder disPea (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_CNT << ea < 0L) {
      return disEaz (sb.append ("pea.l   "), ea);
    }
    int mmm = ea >> 3;
    int rrr = ea & 7;
    if (mmm == XEiJ.MMM_DR) {
      return disDR (sb.append ("swap.w  "), rrr);
    }
    if (mmm == XEiJ.MMM_AR) {
      return disHex2 (sb.append ("bkpt    #"), rrr);
    }
    return disIllegal (sb);
  }  //disPea(StringBuilder)

  //EXT.W Dr                                012346|-|-UUUU|-**00|          |0100_100_010_000_rrr
  //MOVEM.W <list>,<ea>                     012346|-|-----|-----|  M -WXZ  |0100_100_010_mmm_rrr-llllllllllllllll
  public static StringBuilder disMovemToMemWord (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_WTL << ea < 0L) {
      return disEaw (disLst (sb.append ("movem.w "), ea >> 3 == XEiJ.MMM_MN).append (','), ea);
    }
    int mmm = ea >> 3;
    int rrr = ea & 7;
    if (mmm == XEiJ.MMM_DR) {
      return disDR (sb.append ("ext.w   "), rrr);
    }
    return disIllegal (sb);
  }  //disMovemToMemWord(StringBuilder)

  //EXT.L Dr                                012346|-|-UUUU|-**00|          |0100_100_011_000_rrr
  //MOVEM.L <list>,<ea>                     012346|-|-----|-----|  M -WXZ  |0100_100_011_mmm_rrr-llllllllllllllll
  public static StringBuilder disMovemToMemLong (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_WTL << ea < 0L) {
      return disEal (disLst (sb.append ("movem.l "), ea >> 3 == XEiJ.MMM_MN).append (','), ea);
    }
    int mmm = ea >> 3;
    int rrr = ea & 7;
    if (mmm == XEiJ.MMM_DR) {
      return disDR (sb.append ("ext.l   "), rrr);
    }
    return disIllegal (sb);
  }  //disMovemToMemLong(StringBuilder)

  //TST.B <ea>                              012346|-|-UUUU|-**00|D M+-WXZ  |0100_101_000_mmm_rrr
  //TST.B <ea>                              --2346|-|-UUUU|-**00|        PI|0100_101_000_mmm_rrr
  public static StringBuilder disTstByte (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DAT << ea < 0L) {
      return disEab (sb.append ("tst.b   "), ea);
    }
    return disIllegal (sb);
  }  //disTstByte(StringBuilder)

  //TST.W <ea>                              012346|-|-UUUU|-**00|D M+-WXZ  |0100_101_001_mmm_rrr
  //TST.W <ea>                              --2346|-|-UUUU|-**00| A      PI|0100_101_001_mmm_rrr
  public static StringBuilder disTstWord (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_ALL << ea < 0L) {
      return disEaw (sb.append ("tst.w   "), ea);
    }
    return disIllegal (sb);
  }  //disTstWord(StringBuilder)

  //TST.L <ea>                              012346|-|-UUUU|-**00|D M+-WXZ  |0100_101_010_mmm_rrr
  //TST.L <ea>                              --2346|-|-UUUU|-**00| A      PI|0100_101_010_mmm_rrr
  public static StringBuilder disTstLong (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_ALL << ea < 0L) {
      return disEal (sb.append ("tst.l   "), ea);
    }
    return disIllegal (sb);
  }  //disTstLong(StringBuilder)

  //HALT                                    ------|P|-----|-----|          |0100_101_011_001_000    (ISA_A)
  //PULSE                                   ------|-|-----|-----|          |0100_101_011_001_100    (ISA_A)
  //ILLEGAL                                 012346|-|-----|-----|          |0100_101_011_111_100
  //TAS.B <ea>                              012346|-|-UUUU|-**00|D M+-WXZ  |0100_101_011_mmm_rrr
  public static StringBuilder disTas (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DLT << ea < 0L) {
      return disEab (sb.append ("tas.b   "), ea);
    }
    if (ea == 0b001_000) {
      return sb.append ("halt");
    }
    if (ea == 0b001_100) {
      return sb.append ("pulse");
    }
    if (ea == 0b111_100) {
      return sb.append ("illegal");
    }
    return disIllegal (sb);
  }  //disTas(StringBuilder)

  //MULU.L <ea>,Dl                          --2346|-|-UUUU|-***0|D M+-WXZPI|0100_110_000_mmm_rrr-0lll_000_000_000_hhh       (h is not used)
  //MULU.L <ea>,Dh:Dl                       --234S|-|-UUUU|-***0|D M+-WXZPI|0100_110_000_mmm_rrr-0lll_010_000_000_hhh       (if h=l then result is not defined)
  //MULS.L <ea>,Dl                          --2346|-|-UUUU|-***0|D M+-WXZPI|0100_110_000_mmm_rrr-0lll_100_000_000_hhh       (h is not used)
  //MULS.L <ea>,Dh:Dl                       --234S|-|-UUUU|-***0|D M+-WXZPI|0100_110_000_mmm_rrr-0lll_110_000_000_hhh       (if h=l then result is not defined)
  public static StringBuilder disMuluMulsLong (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DAT << ea < 0L) {
      int w = MC68060.mmuPeekWordZeroCode ((disPC += 2) - 2, disSupervisor);  //拡張ワード。0lll_sq0_000_000_hhh
      if ((w & ~0b0111_110_000_000_111) == 0) {  //拡張ワードの0でなければならないbitがすべて0
        int low = w >> 12;  //被乗数。積の下位32bit
        int signed = w & 0b0000_100_000_000_000;  //0=符号なし,0以外=符号あり
        int quad = w & 0b0000_010_000_000_000;  //0=積は32bit,0以外=積は64bit
        int high = w & 7;  //積の上位32bit
        disEal (sb.append (signed == 0 ? "mulu.l  " : "muls.l  "), ea).append (',');
        if (quad != 0) {
          disDR (sb, high).append (':');
        }
        return disDR (sb, low);
      }
    }
    return disIllegal (sb);
  }  //disMuluMulsLong(StringBuilder)

  //DIVU.L <ea>,Dq                          --2346|-|-UUUU|-***0|D M+-WXZPI|0100_110_001_mmm_rrr-0qqq_000_000_000_qqq
  //DIVUL.L <ea>,Dr:Dq                      --2346|-|-UUUU|-***0|D M+-WXZPI|0100_110_001_mmm_rrr-0qqq_000_000_000_rrr       (q is not equal to r)
  //REMU.L <ea>,Dr:Dq                       ------|-|-UUUU|-***0|D M+-W    |0100_110_001_mmm_rrr-0qqq_000_000_000_rrr       (ISA_A, q is not equal to r)
  //DIVU.L <ea>,Dr:Dq                       --234S|-|-UUUU|-***0|D M+-WXZPI|0100_110_001_mmm_rrr-0qqq_010_000_000_rrr       (q is not equal to r)
  //DIVS.L <ea>,Dq                          --2346|-|-UUUU|-***0|D M+-WXZPI|0100_110_001_mmm_rrr-0qqq_100_000_000_qqq
  //DIVSL.L <ea>,Dr:Dq                      --2346|-|-UUUU|-***0|D M+-WXZPI|0100_110_001_mmm_rrr-0qqq_100_000_000_rrr       (q is not equal to r)
  //REMS.L <ea>,Dr:Dq                       ------|-|-UUUU|-***0|D M+-W    |0100_110_001_mmm_rrr-0qqq_100_000_000_rrr       (ISA_A, q is not equal to r)
  //DIVS.L <ea>,Dr:Dq                       --234S|-|-UUUU|-***0|D M+-WXZPI|0100_110_001_mmm_rrr-0qqq_110_000_000_rrr       (q is not equal to r)
  public static StringBuilder disDivuDivsLong (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DAT << ea < 0L) {
      int w = MC68060.mmuPeekWordZeroCode ((disPC += 2) - 2, disSupervisor);  //拡張ワード。0qqq_sq0_000_000_rrr
      if ((w & ~0b0111_110_000_000_111) == 0) {  //拡張ワードの0でなければならないbitがすべて0
        int quo = w >> 12;  //被除数の下位32bit、商
        int signed = w & 0b0000_100_000_000_000;  //0=符号なし,0以外=符号あり
        int quad = w & 0b0000_010_000_000_000;  //0=被除数は32bit,0以外=被除数は64bit
        int rem = w & 7;  //被除数の上位32bit、余り
        //  ColdFireにはDIV*L.L <ea>,Dr:Dq (q!=r)という命令が存在せず、同じオペコードにREM*.L <ea>,Dr:Dq (q!=r)が割り当てられている
        //  REM*.L <ea>,Dr:Dq (q!=r)はDrに余りを格納するがDqに商を格納しない
        disEal (sb.append (signed == 0 ? "divu" : "divs").append (quad == 0 && quo != rem ? "l.l " : ".l  "), ea).append (',');
        if (quad != 0 || quo != rem) {
          disDR (sb, rem).append (':');
        }
        return disDR (sb, quo);
      }
    }
    return disIllegal (sb);
  }  //disDivuDivsLong(StringBuilder)

  //SATS.L Dr                               ------|-|-UUUU|-**00|          |0100_110_010_000_rrr    (ISA_B)
  //MOVEM.W <ea>,<list>                     012346|-|-----|-----|  M+ WXZP |0100_110_010_mmm_rrr-llllllllllllllll
  public static StringBuilder disMovemToRegWord (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_RDL << ea < 0L) {
      StringBuilder tb = disLst (new StringBuilder (), false);  //レジスタリストと実効アドレスの順序に注意
      return disEaw (sb.append ("movem.w "), ea).append (',').append (tb);
    }
    int mmm = ea >> 3;
    int rrr = ea & 7;
    if (mmm == XEiJ.MMM_DR) {
      return disDR (sb.append ("sats.l  "), rrr);
    }
    return disIllegal (sb);
  }  //disMovemToRegWord(StringBuilder)

  //MOVEM.L <ea>,<list>                     012346|-|-----|-----|  M+ WXZP |0100_110_011_mmm_rrr-llllllllllllllll
  public static StringBuilder disMovemToRegLong (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_RDL << ea < 0L) {
      StringBuilder tb = disLst (new StringBuilder (), false);  //レジスタリストと実効アドレスの順序に注意
      return disEal (sb.append ("movem.l "), ea).append (',').append (tb);
    }
    return disIllegal (sb);
  }  //disMovemToRegLong(StringBuilder)

  //HFSBOOT                                 012346|-|-----|-----|          |0100_111_000_000_000
  //HFSINST                                 012346|-|-----|-----|          |0100_111_000_000_001
  //HFSSTR                                  012346|-|-----|-----|          |0100_111_000_000_010
  //HFSINT                                  012346|-|-----|-----|          |0100_111_000_000_011
  //EMXNOP                                  012346|-|-----|-----|          |0100_111_000_000_100
  public static StringBuilder disEmx (StringBuilder sb) {
    int ea = disOC & 63;
    if (ea < XEiJ.EMX_MNEMONIC_ARRAY.length) {
      return sb.append (XEiJ.EMX_MNEMONIC_ARRAY[ea]);
    }
    return disIllegal (sb);
  }  //disEmx(StringBuilder)

  //TRAP #<vector>                          012346|-|-----|-----|          |0100_111_001_00v_vvv
  //LINK.W Ar,#<data>                       012346|-|-----|-----|          |0100_111_001_010_rrr-{data}
  //UNLK Ar                                 012346|-|-----|-----|          |0100_111_001_011_rrr
  //MOVE.L Ar,USP                           012346|P|-----|-----|          |0100_111_001_100_rrr
  //MOVE.L USP,Ar                           012346|P|-----|-----|          |0100_111_001_101_rrr
  //RESET                                   012346|P|-----|-----|          |0100_111_001_110_000
  //NOP                                     012346|-|-----|-----|          |0100_111_001_110_001
  //STOP #<data>                            012346|P|UUUUU|*****|          |0100_111_001_110_010-{data}
  //RTE                                     012346|P|UUUUU|*****|          |0100_111_001_110_011
  //RTD #<data>                             -12346|-|-----|-----|          |0100_111_001_110_100-{data}
  //RTS                                     012346|-|-----|-----|          |0100_111_001_110_101
  //TRAPV                                   012346|-|---*-|-----|          |0100_111_001_110_110
  //RTR                                     012346|-|UUUUU|*****|          |0100_111_001_110_111
  //MOVEC.L Rc,Rn                           -12346|P|-----|-----|          |0100_111_001_111_010-rnnn_ccc_ccc_ccc_ccc
  //MOVEC.L Rn,Rc                           -12346|P|-----|-----|          |0100_111_001_111_011-rnnn_ccc_ccc_ccc_ccc
  public static StringBuilder disMisc (StringBuilder sb) {
    int ea = disOC & 63;
    switch (ea) {
    case 0b000_000:
    case 0b000_001:
    case 0b000_010:
    case 0b000_011:
    case 0b000_100:
    case 0b000_101:
    case 0b000_110:
    case 0b000_111:
    case 0b001_000:
    case 0b001_001:
    case 0b001_010:
    case 0b001_011:
    case 0b001_100:
    case 0b001_101:
    case 0b001_110:
    case 0b001_111:  //TRAP #<vector>
        return disIM8 (sb.append ("trap    "), ea & 15);
    case 0b010_000:
    case 0b010_001:
    case 0b010_010:
    case 0b010_011:
    case 0b010_100:
    case 0b010_101:
    case 0b010_110:
    case 0b010_111:  //LINK.W Ar,#<data>
      return disIMw (disAR (sb.append ("link.w  "), ea & 7).append (','));
    case 0b011_000:
    case 0b011_001:
    case 0b011_010:
    case 0b011_011:
    case 0b011_100:
    case 0b011_101:
    case 0b011_110:
    case 0b011_111:  //UNLK Ar
      return disAR (sb.append ("unlk    "), ea & 7);
    case 0b100_000:
    case 0b100_001:
    case 0b100_010:
    case 0b100_011:
    case 0b100_100:
    case 0b100_101:
    case 0b100_110:
    case 0b100_111:  //MOVE.L Ar,USP
      return disAR (sb.append ("move.l  "), ea & 7).append (",usp");
    case 0b101_000:
    case 0b101_001:
    case 0b101_010:
    case 0b101_011:
    case 0b101_100:
    case 0b101_101:
    case 0b101_110:
    case 0b101_111:  //MOVE.L USP,Ar
      return disAR (sb.append ("move.l  usp,"), ea & 7);
    case 0b110_000:  //RESET
      return sb.append ("reset   ");
    case 0b110_001:  //NOP
      return sb.append ("nop     ");
    case 0b110_010:  //STOP #<data>
      return disIMw (sb.append ("stop.w  "));
    case 0b110_011:  //RTE
      disStatus = DIS_ALWAYS_BRANCH;
      return sb.append ("rte     ");
    case 0b110_100:  //RTD
      disStatus = DIS_ALWAYS_BRANCH;
      return disIMw (sb.append ("rtd.w   "));
    case 0b110_101:  //RTS
      disStatus = DIS_ALWAYS_BRANCH;
      return sb.append ("rts     ");
    case 0b110_110:  //TRAPV
      return sb.append ("trapv   ");
    case 0b110_111:  //RTR
      disStatus = DIS_ALWAYS_BRANCH;
      return sb.append ("rtr     ");
    case 0b111_010:  //MOVEC.L Rc,Rn
      {
        int w = MC68060.mmuPeekWordZeroCode ((disPC += 2) - 2, disSupervisor);
        int c = w & 0x07ff;
        String[] disRc = (w & 0x0800) == 0 ? DIS_RCNAME_0XX : DIS_RCNAME_8XX;
        if (c < disRc.length && 0 < disRc[c].length ()) {
          return disRn (sb.append ("movec.l ").append (disRc[c]).append (','), w >> 12);
        }
      }
      break;
    case 0b111_011:  //MOVEC.L Rn,Rc
      {
        int w = MC68060.mmuPeekWordZeroCode ((disPC += 2) - 2, disSupervisor);
        int c = w & 0x07ff;
        String[] disRc = (w & 0x0800) == 0 ? DIS_RCNAME_0XX : DIS_RCNAME_8XX;
        if (c < disRc.length && 0 < disRc[c].length ()) {
          return disRn (sb.append ("movec.l "), w >> 12).append (',').append (disRc[c]);
        }
      }
      break;
    }
    return disIllegal (sb);
  }  //disMisc(StringBuilder)

  //JSR <ea>                                012346|-|-----|-----|  M  WXZP |0100_111_010_mmm_rrr
  public static StringBuilder disJsr (StringBuilder sb) {
    disStatus = DIS_CALL_SUBROUTINE;
    int ea = disOC & 63;
    if (XEiJ.EAM_CNT << ea < 0L) {
      return disEaz (sb.append ("jsr     "), ea);
    }
    return disIllegal (sb);
  }  //disJsr(StringBuilder)

  //JMP <ea>                                012346|-|-----|-----|  M  WXZP |0100_111_011_mmm_rrr
  public static StringBuilder disJmp (StringBuilder sb) {
    disStatus = DIS_ALWAYS_BRANCH;
    int ea = disOC & 63;
    if (XEiJ.EAM_CNT << ea < 0L) {
      return disEaz (sb.append ("jmp     "), ea);
    }
    return disIllegal (sb);
  }  //disJmp(StringBuilder)

  //CHK.L <ea>,Dq                           --2346|-|-UUUU|-*UUU|D M+-WXZPI|0100_qqq_100_mmm_rrr
  public static StringBuilder disChkLong (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_DAT << ea < 0L) {
      return disDR (disEaw (sb.append ("chk.l   "), ea).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disChkLong(StringBuilder)

  //CHK.W <ea>,Dq                           012346|-|-UUUU|-*UUU|D M+-WXZPI|0100_qqq_110_mmm_rrr
  public static StringBuilder disChkWord (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_DAT << ea < 0L) {
      return disDR (disEaw (sb.append ("chk.w   "), ea).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disChkWord(StringBuilder)

  //EXTB.L Dr                               --2346|-|-UUUU|-**00|          |0100_100_111_000_rrr
  //LEA.L <ea>,Aq                           012346|-|-----|-----|  M  WXZP |0100_qqq_111_mmm_rrr
  public static StringBuilder disLea (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_CNT << ea < 0L) {
      return disAR (disEaz (sb.append ("lea.l   "), ea).append (','), qqq);
    }
    int mmm = ea >> 3;
    int rrr = ea & 7;
    if (qqq == 0b100 && mmm == 0b000) {
      return disDR (sb.append ("extb.l  "), rrr);
    }
    return disIllegal (sb);
  }  //disLea(StringBuilder)

  //ADDQ.B #<data>,<ea>                     012346|-|UUUUU|*****|D M+-WXZ  |0101_qqq_000_mmm_rrr
  public static StringBuilder disAddqByte (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_DLT << ea < 0L) {
      return disEab (disIM3 (sb.append ("addq.b  "), qqq).append (','), ea);
    }
    return disIllegal (sb);
  }  //disAddqByte(StringBuilder)

  //ADDQ.W #<data>,Ar                       012346|-|-----|-----| A        |0101_qqq_001_001_rrr
  //ADDQ.W #<data>,<ea>                     012346|-|UUUUU|*****|D M+-WXZ  |0101_qqq_001_mmm_rrr
  public static StringBuilder disAddqWord (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_ALT << ea < 0L) {
      return disEaw (disIM3 (sb.append ("addq.w  "), qqq).append (','), ea);
    }
    return disIllegal (sb);
  }  //disAddqWord(StringBuilder)

  //ADDQ.L #<data>,Ar                       012346|-|-----|-----| A        |0101_qqq_010_001_rrr
  //ADDQ.L #<data>,<ea>                     012346|-|UUUUU|*****|D M+-WXZ  |0101_qqq_010_mmm_rrr
  public static StringBuilder disAddqLong (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_ALT << ea < 0L) {
      return disEal (disIM3 (sb.append ("addq.l  "), qqq).append (','), ea);
    }
    return disIllegal (sb);
  }  //disAddqLong(StringBuilder)

  //SUBQ.B #<data>,<ea>                     012346|-|UUUUU|*****|D M+-WXZ  |0101_qqq_100_mmm_rrr
  public static StringBuilder disSubqByte (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_DLT << ea < 0L) {
      return disEab (disIM3 (sb.append ("subq.b  "), qqq).append (','), ea);
    }
    return disIllegal (sb);
  }  //disSubqByte(StringBuilder)

  //SUBQ.W #<data>,Ar                       012346|-|-----|-----| A        |0101_qqq_101_001_rrr
  //SUBQ.W #<data>,<ea>                     012346|-|UUUUU|*****|D M+-WXZ  |0101_qqq_101_mmm_rrr
  public static StringBuilder disSubqWord (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_ALT << ea < 0L) {
      return disEaw (disIM3 (sb.append ("subq.w  "), qqq).append (','), ea);
    }
    return disIllegal (sb);
  }  //disSubqWord(StringBuilder)

  //SUBQ.L #<data>,Ar                       012346|-|-----|-----| A        |0101_qqq_110_001_rrr
  //SUBQ.L #<data>,<ea>                     012346|-|UUUUU|*****|D M+-WXZ  |0101_qqq_110_mmm_rrr
  public static StringBuilder disSubqLong (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_ALT << ea < 0L) {
      return disEal (disIM3 (sb.append ("subq.l  "), qqq).append (','), ea);
    }
    return disIllegal (sb);
  }  //disSubqLong(StringBuilder)

  //DBRA.W Dr,<label>                       012346|-|-----|-----|          |0101_000_111_001_rrr-{offset}
  //DBcc.W Dr,<label>                       012346|-|-****|-----|          |0101_ccc_c11_001_rrr-{offset}
  //TRAPcc.W #<data>                        --2346|-|-****|-----|          |0101_ccc_c11_111_010-{data}
  //TRAPcc.L #<data>                        --2346|-|-****|-----|          |0101_ccc_c11_111_011-{data}
  //TRAPcc                                  --2346|-|-****|-----|          |0101_ccc_c11_111_100
  //Scc.B <ea>                              012346|-|-****|-----|D M+-WXZ  |0101_ccc_c11_mmm_rrr
  public static StringBuilder disScc (StringBuilder sb) {
    int cccc = disOC >> 8 & 15;
    int ea = disOC & 63;
    if (XEiJ.EAM_DLT << ea < 0L) {
      return disEab (disOp3 (sb, "s", DIS_CC[cccc], ".b"), ea);
    }
    int mmm = ea >> 3;
    int rrr = ea & 7;
    if (mmm == XEiJ.MMM_AR) {
      if (cccc != XEiJ.CCCC_T) {
        disStatus = DIS_SOMETIMES_BRANCH;
        return disProgramHex8 (disDR (disOp3 (sb,
                                              "db",
                                              cccc == XEiJ.CCCC_F ? "ra" : DIS_CC[cccc],
                                              ".w"),
                                      rrr).append (","),
                               disPC + MC68060.mmuPeekWordSignCode ((disPC += 2) - 2,
                                                                    disSupervisor));
      }
    } else if (ea == XEiJ.EA_PW) {
      return disIMw (sb.append ("trap").append (DIS_CC[cccc]).append (".w "));
    } else if (ea == XEiJ.EA_PX) {
      return disIMl (sb.append ("trap").append (DIS_CC[cccc]).append (".l "));
    } else if (ea == XEiJ.EA_IM) {
      //  ColdFireのTRAPccはオペランドを読み飛ばすだけのTRAPFのみでTPFと書く
      return sb.append ("trap").append (DIS_CC[cccc]);
    }
    return disIllegal (sb);
  }  //disScc(StringBuilder)

  //BRA.W <label>                           012346|-|-----|-----|          |0110_000_000_000_000-{offset}
  //BRA.L <label>                           --2346|-|-----|-----|          |0110_000_011_111_111-{offset}
  //BRA.S <label>                           012346|-|-----|-----|          |0110_000_0aa_aaa_aaa        (a is not equal to 0,-1)
  public static StringBuilder disBra (StringBuilder sb) {
    disStatus = DIS_ALWAYS_BRANCH;
    int offset = (byte) disOC;
    if (offset == 0) {
      return disProgramHex8 (sb.append ("bra.w   "),
                             disPC + MC68060.mmuPeekWordSignCode ((disPC += 2) - 2,
                                                                  disSupervisor));
    } else if (offset == -1) {
      return disProgramHex8 (sb.append ("bra.l   "),
                             disPC + MC68060.mmuPeekLongCode ((disPC += 4) - 4,
                                                              disSupervisor));
    } else {
      return disProgramHex8 (sb.append ("bra.s   "),
                             disPC + offset);
    }
  }  //disBra(StringBuilder)

  //BSR.W <label>                           012346|-|-----|-----|          |0110_000_100_000_000-{offset}
  //BSR.L <label>                           --2346|-|-----|-----|          |0110_000_111_111_111-{offset}
  //BSR.S <label>                           012346|-|-----|-----|          |0110_000_1aa_aaa_aaa        (a is not equal to 0,-1)
  public static StringBuilder disBsr (StringBuilder sb) {
    disStatus = DIS_CALL_SUBROUTINE;
    int offset = (byte) disOC;
    if (offset == 0) {
      return disProgramHex8 (sb.append ("bsr.w   "),
                             disPC + MC68060.mmuPeekWordSignCode ((disPC += 2) - 2,
                                                                  disSupervisor));
    } else if (offset == -1) {
      return disProgramHex8 (sb.append ("bsr.l   "),
                             disPC + MC68060.mmuPeekLongCode ((disPC += 4) - 4,
                                                              disSupervisor));
    } else {
      return disProgramHex8 (sb.append ("bsr.s   "),
                             disPC + offset);
    }
  }  //disBsr(StringBuilder)

  //Bcc.W <label>                           012346|-|-****|-----|          |0110_ccc_c00_000_000-{offset}
  //Bcc.L <label>                           --2346|-|-****|-----|          |0110_ccc_c11_111_111-{offset}
  //Bcc.S <label>                           012346|-|-****|-----|          |0110_ccc_caa_aaa_aaa        (a is not equal to 0,-1)
  public static StringBuilder disBcc (StringBuilder sb) {
    disStatus = DIS_SOMETIMES_BRANCH;
    int cccc = disOC >> 8 & 15;
    int offset = (byte) disOC;
    if (offset == 0) {
      return disProgramHex8 (disOp3 (sb,
                                     "b",
                                     DIS_CC[cccc],
                                     ".w"),
                             disPC + MC68060.mmuPeekWordSignCode ((disPC += 2) - 2,
                                                                  disSupervisor));
    } else if (offset == -1) {
      return disProgramHex8 (disOp3 (sb,
                                     "b",
                                     DIS_CC[cccc],
                                     ".l"),
                             disPC + MC68060.mmuPeekLongCode ((disPC += 4) - 4,
                                                              disSupervisor));
    } else {
      return disProgramHex8 (disOp3 (sb,
                                     "b",
                                     DIS_CC[cccc],
                                     ".s"),
                             disPC + offset);
    }
  }  //disBcc(StringBuilder)

  //IOCS <name>                             012346|-|-UUUU|-**00|          |0111_000_0dd_ddd_ddd-0100_111_001_001_111
  //MOVEQ.L #<data>,Dq                      012346|-|-UUUU|-**00|          |0111_qqq_0dd_ddd_ddd
  public static StringBuilder disMoveq (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    if (qqq == 0 && MC68060.mmuPeekWordZeroCode (disPC, disSupervisor) == 0b0100_111_001_001_111) {  //MOVEQ.L #<data>,D0;TRAP #15 → IOCS <name>
      disPC += 2;
      sb.append ("IOCS    ");
      if (disOC == 0x70ff) {  //_ABORTJOB
        disStatus = DIS_ALWAYS_BRANCH;
      }
      String name = DIS_IOCS_NAME[disOC & 255];
      if (name.length () != 0) {
        return sb.append (name);
      }
      return disHex2 (sb, disOC & 255);
    }
    return disDR (disIM8 (sb.append ("moveq.l "), disOC).append (','), qqq);
  }  //disMoveq(StringBuilder)

  //MVS.B <ea>,Dq                           ------|-|-UUUU|-**00|D M+-WXZPI|0111_qqq_100_mmm_rrr    (ISA_B)
  public static StringBuilder disMvsByte (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_DAT << ea < 0L) {
      return disDR (disEab (sb.append ("mvs.b   "), ea).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disMvsByte(StringBuilder)

  //MVS.W <ea>,Dq                           ------|-|-UUUU|-**00|D M+-WXZPI|0111_qqq_101_mmm_rrr    (ISA_B)
  public static StringBuilder disMvsWord (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_DAT << ea < 0L) {
      return disDR (disEaw (sb.append ("mvs.w   "), ea).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disMvsWord(StringBuilder)

  //MVZ.B <ea>,Dq                           ------|-|-UUUU|-0*00|D M+-WXZPI|0111_qqq_110_mmm_rrr    (ISA_B)
  public static StringBuilder disMvzByte (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_DAT << ea < 0L) {
      return disDR (disEab (sb.append ("mvz.b   "), ea).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disMvzByte(StringBuilder)

  //MVZ.W <ea>,Dq                           ------|-|-UUUU|-0*00|D M+-WXZPI|0111_qqq_111_mmm_rrr    (ISA_B)
  public static StringBuilder disMvzWord (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_DAT << ea < 0L) {
      return disDR (disEaw (sb.append ("mvz.w   "), ea).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disMvzWord(StringBuilder)

  //OR.B <ea>,Dq                            012346|-|-UUUU|-**00|D M+-WXZPI|1000_qqq_000_mmm_rrr
  public static StringBuilder disOrToRegByte (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_DAT << ea < 0L) {
      return disDR (disEab (sb.append ("or.b    "), ea).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disOrToRegByte(StringBuilder)

  //OR.W <ea>,Dq                            012346|-|-UUUU|-**00|D M+-WXZPI|1000_qqq_001_mmm_rrr
  public static StringBuilder disOrToRegWord (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_DAT << ea < 0L) {
      return disDR (disEaw (sb.append ("or.w    "), ea).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disOrToRegWord(StringBuilder)

  //OR.L <ea>,Dq                            012346|-|-UUUU|-**00|D M+-WXZPI|1000_qqq_010_mmm_rrr
  public static StringBuilder disOrToRegLong (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_DAT << ea < 0L) {
      return disDR (disEal (sb.append ("or.l    "), ea).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disOrToRegLong(StringBuilder)

  //DIVU.W <ea>,Dq                          012346|-|-UUUU|-***0|D M+-WXZPI|1000_qqq_011_mmm_rrr
  public static StringBuilder disDivuWord (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_DAT << ea < 0L) {
      return disDR (disEaw (sb.append ("divu.w  "), ea).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disDivuWord(StringBuilder)

  //DIVS.W <ea>,Dq                          012346|-|-UUUU|-***0|D M+-WXZPI|1000_qqq_111_mmm_rrr
  public static StringBuilder disDivsWord (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_DAT << ea < 0L) {
      return disDR (disEaw (sb.append ("divs.w  "), ea).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disDivsWord(StringBuilder)

  //SBCD.B Dr,Dq                            012346|-|UUUUU|*U*U*|          |1000_qqq_100_000_rrr
  //SBCD.B -(Ar),-(Aq)                      012346|-|UUUUU|*U*U*|          |1000_qqq_100_001_rrr
  //OR.B Dq,<ea>                            012346|-|-UUUU|-**00|  M+-WXZ  |1000_qqq_100_mmm_rrr
  public static StringBuilder disOrToMemByte (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_MLT << ea < 0L) {
      return disEab (disDR (sb.append ("or.b    "), qqq).append (','), ea);
    }
    int mmm = ea >> 3;
    int rrr = ea & 7;
    if (mmm == XEiJ.MMM_DR) {
      return disDR (disDR (sb.append ("sbcd.b  "), rrr).append (','), qqq);
    }
    if (mmm == XEiJ.MMM_AR) {
      return disMN (disMN (sb.append ("sbcd.b  "), rrr).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disOrToMemByte(StringBuilder)

  //PACK Dr,Dq,#<data>                      --2346|-|-----|-----|          |1000_qqq_101_000_rrr-{data}
  //PACK -(Ar),-(Aq),#<data>                --2346|-|-----|-----|          |1000_qqq_101_001_rrr-{data}
  //OR.W Dq,<ea>                            012346|-|-UUUU|-**00|  M+-WXZ  |1000_qqq_101_mmm_rrr
  public static StringBuilder disOrToMemWord (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_MLT << ea < 0L) {
      return disEaw (disDR (sb.append ("or.w    "), qqq).append (','), ea);
    }
    int mmm = ea >> 3;
    int rrr = ea & 7;
    if (mmm == XEiJ.MMM_DR) {
      return disIMw (disDR (disDR (sb.append ("pack    "), rrr).append (','), qqq).append (','));
    }
    if (mmm == XEiJ.MMM_AR) {
      return disIMw (disMN (disMN (sb.append ("pack    "), rrr).append (','), qqq).append (','));
    }
    return disIllegal (sb);
  }  //disOrToMemWord(StringBuilder)

  //UNPK Dr,Dq,#<data>                      --2346|-|-----|-----|          |1000_qqq_110_000_rrr-{data}
  //UNPK -(Ar),-(Aq),#<data>                --2346|-|-----|-----|          |1000_qqq_110_001_rrr-{data}
  //OR.L Dq,<ea>                            012346|-|-UUUU|-**00|  M+-WXZ  |1000_qqq_110_mmm_rrr
  public static StringBuilder disOrToMemLong (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_MLT << ea < 0L) {
      return disEal (disDR (sb.append ("or.l    "), qqq).append (','), ea);
    }
    int mmm = ea >> 3;
    int rrr = ea & 7;
    if (mmm == XEiJ.MMM_DR) {
      return disIMw (disDR (disDR (sb.append ("unpk    "), rrr).append (','), qqq).append (','));
    }
    if (mmm == XEiJ.MMM_AR) {
      return disIMw (disMN (disMN (sb.append ("unpk    "), rrr).append (','), qqq).append (','));
    }
    return disIllegal (sb);
  }  //disOrToMemLong(StringBuilder)

  //SUB.B <ea>,Dq                           012346|-|UUUUU|*****|D M+-WXZPI|1001_qqq_000_mmm_rrr
  public static StringBuilder disSubToRegByte (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_DAT << ea < 0L) {
      return disDR (disEab (sb.append ("sub.b   "), ea).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disSubToRegByte(StringBuilder)

  //SUB.W <ea>,Dq                           012346|-|UUUUU|*****|DAM+-WXZPI|1001_qqq_001_mmm_rrr
  public static StringBuilder disSubToRegWord (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_ALL << ea < 0L) {
      return disDR (disEaw (sb.append ("sub.w   "), ea).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disSubToRegWord(StringBuilder)

  //SUB.L <ea>,Dq                           012346|-|UUUUU|*****|DAM+-WXZPI|1001_qqq_010_mmm_rrr
  public static StringBuilder disSubToRegLong (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_ALL << ea < 0L) {
      return disDR (disEal (sb.append ("sub.l   "), ea).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disSubToRegLong(StringBuilder)

  //SUBA.W <ea>,Aq                          012346|-|-----|-----|DAM+-WXZPI|1001_qqq_011_mmm_rrr
  public static StringBuilder disSubaWord (StringBuilder sb) {
    int ea = disOC & 63;
    int qqq = disOC >> 9 & 7;
    if (XEiJ.EAM_ALL << ea < 0L) {
      return disAR (disEaw (sb.append ("suba.w  "), ea).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disSubaWord(StringBuilder)

  //SUBA.L <ea>,Aq                          012346|-|-----|-----|DAM+-WXZPI|1001_qqq_111_mmm_rrr
  public static StringBuilder disSubaLong (StringBuilder sb) {
    int ea = disOC & 63;
    int qqq = disOC >> 9 & 7;
    if (XEiJ.EAM_ALL << ea < 0L) {
      return disAR (disEal (sb.append ("suba.l  "), ea).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disSubaLong(StringBuilder)

  //SUBX.B Dr,Dq                            012346|-|*UUUU|*****|          |1001_qqq_100_000_rrr
  //SUBX.B -(Ar),-(Aq)                      012346|-|*UUUU|*****|          |1001_qqq_100_001_rrr
  //SUB.B Dq,<ea>                           012346|-|UUUUU|*****|  M+-WXZ  |1001_qqq_100_mmm_rrr
  public static StringBuilder disSubToMemByte (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_MLT << ea < 0L) {
      return disEab (disDR (sb.append ("sub.b   "), qqq).append (','), ea);
    }
    int mmm = ea >> 3;
    int rrr = ea & 7;
    if (mmm == XEiJ.MMM_DR) {
      return disDR (disDR (sb.append ("subx.b  "), rrr).append (','), qqq);
    }
    if (mmm == XEiJ.MMM_AR) {
      return disMN (disMN (sb.append ("subx.b  "), rrr).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disSubToMemByte(StringBuilder)

  //SUBX.W Dr,Dq                            012346|-|*UUUU|*****|          |1001_qqq_101_000_rrr
  //SUBX.W -(Ar),-(Aq)                      012346|-|*UUUU|*****|          |1001_qqq_101_001_rrr
  //SUB.W Dq,<ea>                           012346|-|UUUUU|*****|  M+-WXZ  |1001_qqq_101_mmm_rrr
  public static StringBuilder disSubToMemWord (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_MLT << ea < 0L) {
      return disEaw (disDR (sb.append ("sub.w   "), qqq).append (','), ea);
    }
    int mmm = ea >> 3;
    int rrr = ea & 7;
    if (mmm == XEiJ.MMM_DR) {
      return disDR (disDR (sb.append ("subx.w  "), rrr).append (','), qqq);
    }
    if (mmm == XEiJ.MMM_AR) {
      return disMN (disMN (sb.append ("subx.w  "), rrr).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disSubToMemWord(StringBuilder)

  //SUBX.L Dr,Dq                            012346|-|*UUUU|*****|          |1001_qqq_110_000_rrr
  //SUBX.L -(Ar),-(Aq)                      012346|-|*UUUU|*****|          |1001_qqq_110_001_rrr
  //SUB.L Dq,<ea>                           012346|-|UUUUU|*****|  M+-WXZ  |1001_qqq_110_mmm_rrr
  public static StringBuilder disSubToMemLong (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_MLT << ea < 0L) {
      return disEal (disDR (sb.append ("sub.l   "), qqq).append (','), ea);
    }
    int mmm = ea >> 3;
    int rrr = ea & 7;
    if (mmm == XEiJ.MMM_DR) {
      return disDR (disDR (sb.append ("subx.l  "), rrr).append (','), qqq);
    }
    if (mmm == XEiJ.MMM_AR) {
      return disMN (disMN (sb.append ("subx.l  "), rrr).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disSubToMemLong(StringBuilder)

  //MOV3Q.L #<data>,<ea>                    ------|-|-UUUU|-**00|DAM+-WXZ  |1010_qqq_101_mmm_rrr    (ISA_B)
  //SXCALL <name>                           012346|-|-----|-----|          |1010_xxx_xxx_xxx_xxx
  //line 1010 emulator                      012346|-|-----|-----|          |1010_xxx_xxx_xxx_xxx
  public static StringBuilder disSxcall (StringBuilder sb) {
    if (disOC < 0xa800) {
      String name = DIS_SXCALL_NAME[disOC & 2047];
      if (name.length () != 0) {
        return sb.append ("SXCALL  ").append (name);
      }
    }
    return disAline (sb);
  }  //disSxcall(StringBuilder)

  //line 1010 emulator                      012346|-|-----|-----|          |1010_xxx_xxx_xxx_xxx
  public static StringBuilder disAline (StringBuilder sb) {
    return disHex4 (sb.append (".dc.w   "), disOC);
  }  //disAline(StringBuilder)

  //CMP.B <ea>,Dq                           012346|-|-UUUU|-****|D M+-WXZPI|1011_qqq_000_mmm_rrr
  public static StringBuilder disCmpByte (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_DAT << ea < 0L) {
      return disDR (disEab (sb.append ("cmp.b   "), ea).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disCmpByte(StringBuilder)

  //CMP.W <ea>,Dq                           012346|-|-UUUU|-****|DAM+-WXZPI|1011_qqq_001_mmm_rrr
  public static StringBuilder disCmpWord (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_ALL << ea < 0L) {
      return disDR (disEaw (sb.append ("cmp.w   "), ea).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disCmpWord(StringBuilder)

  //CMP.L <ea>,Dq                           012346|-|-UUUU|-****|DAM+-WXZPI|1011_qqq_010_mmm_rrr
  public static StringBuilder disCmpLong (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_ALL << ea < 0L) {
      return disDR (disEal (sb.append ("cmp.l   "), ea).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disCmpLong(StringBuilder)

  //CMPA.W <ea>,Aq                          012346|-|-UUUU|-****|DAM+-WXZPI|1011_qqq_011_mmm_rrr
  public static StringBuilder disCmpaWord (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_ALL << ea < 0L) {
      return disAR (disEaw (sb.append ("cmpa.w  "), ea).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disCmpaWord(StringBuilder)

  //CMPA.L <ea>,Aq                          012346|-|-UUUU|-****|DAM+-WXZPI|1011_qqq_111_mmm_rrr
  public static StringBuilder disCmpaLong (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_ALL << ea < 0L) {
      return disAR (disEal (sb.append ("cmpa.l  "), ea).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disCmpaLong(StringBuilder)

  //CMPM.B (Ar)+,(Aq)+                      012346|-|-UUUU|-****|          |1011_qqq_100_001_rrr
  //EOR.B Dq,<ea>                           012346|-|-UUUU|-**00|D M+-WXZ  |1011_qqq_100_mmm_rrr
  public static StringBuilder disEorToMemByte (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_DLT << ea < 0L) {
      return disEab (disDR (sb.append ("eor.b   "), qqq).append (','), ea);
    }
    int mmm = ea >> 3;
    int rrr = ea & 7;
    if (mmm == XEiJ.MMM_AR) {
      return disMP (disMP (sb.append ("cmpm.b  "), rrr).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disEorToMemByte(StringBuilder)

  //CMPM.W (Ar)+,(Aq)+                      012346|-|-UUUU|-****|          |1011_qqq_101_001_rrr
  //EOR.W Dq,<ea>                           012346|-|-UUUU|-**00|D M+-WXZ  |1011_qqq_101_mmm_rrr
  public static StringBuilder disEorToMemWord (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_DLT << ea < 0L) {
      return disEaw (disDR (sb.append ("eor.w   "), qqq).append (','), ea);
    }
    int mmm = ea >> 3;
    int rrr = ea & 7;
    if (mmm == XEiJ.MMM_AR) {
      return disMP (disMP (sb.append ("cmpm.w  "), rrr).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disEorToMemWord(StringBuilder)

  //CMPM.L (Ar)+,(Aq)+                      012346|-|-UUUU|-****|          |1011_qqq_110_001_rrr
  //EOR.L Dq,<ea>                           012346|-|-UUUU|-**00|D M+-WXZ  |1011_qqq_110_mmm_rrr
  public static StringBuilder disEorToMemLong (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_DLT << ea < 0L) {
      return disEal (disDR (sb.append ("eor.l   "), qqq).append (','), ea);
    }
    int mmm = ea >> 3;
    int rrr = ea & 7;
    if (mmm == XEiJ.MMM_AR) {
      return disMP (disMP (sb.append ("cmpm.l  "), rrr).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disEorToMemLong(StringBuilder)

  //AND.B <ea>,Dq                           012346|-|-UUUU|-**00|D M+-WXZPI|1100_qqq_000_mmm_rrr
  public static StringBuilder disAndToRegByte (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_DAT << ea < 0L) {
      return disDR (disEab (sb.append ("and.b   "), ea).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disAndToRegByte(StringBuilder)

  //AND.W <ea>,Dq                           012346|-|-UUUU|-**00|D M+-WXZPI|1100_qqq_001_mmm_rrr
  public static StringBuilder disAndToRegWord (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_DAT << ea < 0L) {
      return disDR (disEaw (sb.append ("and.w   "), ea).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disAndToRegWord(StringBuilder)

  //AND.L <ea>,Dq                           012346|-|-UUUU|-**00|D M+-WXZPI|1100_qqq_010_mmm_rrr
  public static StringBuilder disAndToRegLong (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_DAT << ea < 0L) {
      return disDR (disEal (sb.append ("and.l   "), ea).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disAndToRegLong(StringBuilder)

  //MULU.W <ea>,Dq                          012346|-|-UUUU|-***0|D M+-WXZPI|1100_qqq_011_mmm_rrr
  public static StringBuilder disMuluWord (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_DAT << ea < 0L) {
      return disDR (disEaw (sb.append ("mulu.w  "), ea).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disMuluWord(StringBuilder)

  //MULS.W <ea>,Dq                          012346|-|-UUUU|-***0|D M+-WXZPI|1100_qqq_111_mmm_rrr
  public static StringBuilder disMulsWord (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_DAT << ea < 0L) {
      return disDR (disEaw (sb.append ("muls.w  "), ea).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disMulsWord(StringBuilder)

  //ABCD.B Dr,Dq                            012346|-|UUUUU|*U*U*|          |1100_qqq_100_000_rrr
  //ABCD.B -(Ar),-(Aq)                      012346|-|UUUUU|*U*U*|          |1100_qqq_100_001_rrr
  //AND.B Dq,<ea>                           012346|-|-UUUU|-**00|  M+-WXZ  |1100_qqq_100_mmm_rrr
  public static StringBuilder disAndToMemByte (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_MLT << ea < 0L) {
      return disEab (disDR (sb.append ("and.b   "), qqq).append (','), ea);
    }
    int mmm = ea >> 3;
    int rrr = ea & 7;
    if (mmm == XEiJ.MMM_DR) {
      return disDR (disDR (sb.append ("abcd.b  "), rrr).append (','), qqq);
    }
    if (mmm == XEiJ.MMM_AR) {
      return disMN (disMN (sb.append ("abcd.b  "), rrr).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disAndToMemByte(StringBuilder)

  //EXG.L Dq,Dr                             012346|-|-----|-----|          |1100_qqq_101_000_rrr
  //EXG.L Aq,Ar                             012346|-|-----|-----|          |1100_qqq_101_001_rrr
  //AND.W Dq,<ea>                           012346|-|-UUUU|-**00|  M+-WXZ  |1100_qqq_101_mmm_rrr
  public static StringBuilder disAndToMemWord (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_MLT << ea < 0L) {
      return disEaw (disDR (sb.append ("and.w   "), qqq).append (','), ea);
    }
    int mmm = ea >> 3;
    int rrr = ea & 7;
    if (mmm == XEiJ.MMM_DR) {
      return disDR (disDR (sb.append ("exg.l   "), qqq).append (','), rrr);
    }
    if (mmm == XEiJ.MMM_AR) {
      return disAR (disAR (sb.append ("exg.l   "), qqq).append (','), rrr);
    }
    return disIllegal (sb);
  }  //disAndToMemWord(StringBuilder)

  //EXG.L Dq,Ar                             012346|-|-----|-----|          |1100_qqq_110_001_rrr
  //AND.L Dq,<ea>                           012346|-|-UUUU|-**00|  M+-WXZ  |1100_qqq_110_mmm_rrr
  public static StringBuilder disAndToMemLong (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_MLT << ea < 0L) {
      return disEal (disDR (sb.append ("and.l   "), qqq).append (','), ea);
    }
    int mmm = ea >> 3;
    int rrr = ea & 7;
    if (mmm == XEiJ.MMM_AR) {
      return disAR (disDR (sb.append ("exg.l   "), qqq).append (','), rrr);
    }
    return disIllegal (sb);
  }  //disAndToMemLong(StringBuilder)

  //ADD.B <ea>,Dq                           012346|-|UUUUU|*****|D M+-WXZPI|1101_qqq_000_mmm_rrr
  public static StringBuilder disAddToRegByte (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_DAT << ea < 0L) {
      return disDR (disEab (sb.append ("add.b   "), ea).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disAddToRegByte(StringBuilder)

  //ADD.W <ea>,Dq                           012346|-|UUUUU|*****|DAM+-WXZPI|1101_qqq_001_mmm_rrr
  public static StringBuilder disAddToRegWord (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_ALL << ea < 0L) {
      return disDR (disEaw (sb.append ("add.w   "), ea).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disAddToRegWord(StringBuilder)

  //ADD.L <ea>,Dq                           012346|-|UUUUU|*****|DAM+-WXZPI|1101_qqq_010_mmm_rrr
  public static StringBuilder disAddToRegLong (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_ALL << ea < 0L) {
      return disDR (disEal (sb.append ("add.l   "), ea).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disAddToRegLong(StringBuilder)

  //ADDA.W <ea>,Aq                          012346|-|-----|-----|DAM+-WXZPI|1101_qqq_011_mmm_rrr
  public static StringBuilder disAddaWord (StringBuilder sb) {
    int ea = disOC & 63;
    int qqq = disOC >> 9 & 7;
    if (XEiJ.EAM_ALL << ea < 0L) {
      return disAR (disEaw (sb.append ("adda.w  "), ea).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disAddaWord(StringBuilder)

  //ADDA.L <ea>,Aq                          012346|-|-----|-----|DAM+-WXZPI|1101_qqq_111_mmm_rrr
  public static StringBuilder disAddaLong (StringBuilder sb) {
    int ea = disOC & 63;
    int qqq = disOC >> 9 & 7;
    if (XEiJ.EAM_ALL << ea < 0L) {
      return disAR (disEal (sb.append ("adda.l  "), ea).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disAddaLong(StringBuilder)

  //ADDX.B Dr,Dq                            012346|-|*UUUU|*****|          |1101_qqq_100_000_rrr
  //ADDX.B -(Ar),-(Aq)                      012346|-|*UUUU|*****|          |1101_qqq_100_001_rrr
  //ADD.B Dq,<ea>                           012346|-|UUUUU|*****|  M+-WXZ  |1101_qqq_100_mmm_rrr
  public static StringBuilder disAddToMemByte (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_MLT << ea < 0L) {
      return disEab (disDR (sb.append ("add.b   "), qqq).append (','), ea);
    }
    int mmm = ea >> 3;
    int rrr = ea & 7;
    if (mmm == XEiJ.MMM_DR) {
      return disDR (disDR (sb.append ("addx.b  "), rrr).append (','), qqq);
    }
    if (mmm == XEiJ.MMM_AR) {
      return disMN (disMN (sb.append ("addx.b  "), rrr).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disAddToMemByte(StringBuilder)

  //ADDX.W Dr,Dq                            012346|-|*UUUU|*****|          |1101_qqq_101_000_rrr
  //ADDX.W -(Ar),-(Aq)                      012346|-|*UUUU|*****|          |1101_qqq_101_001_rrr
  //ADD.W Dq,<ea>                           012346|-|UUUUU|*****|  M+-WXZ  |1101_qqq_101_mmm_rrr
  public static StringBuilder disAddToMemWord (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_MLT << ea < 0L) {
      return disEaw (disDR (sb.append ("add.w   "), qqq).append (','), ea);
    }
    int mmm = ea >> 3;
    int rrr = ea & 7;
    if (mmm == XEiJ.MMM_DR) {
      return disDR (disDR (sb.append ("addx.w  "), rrr).append (','), qqq);
    }
    if (mmm == XEiJ.MMM_AR) {
      return disMN (disMN (sb.append ("addx.w  "), rrr).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disAddToMemWord(StringBuilder)

  //ADDX.L Dr,Dq                            012346|-|*UUUU|*****|          |1101_qqq_110_000_rrr
  //ADDX.L -(Ar),-(Aq)                      012346|-|*UUUU|*****|          |1101_qqq_110_001_rrr
  //ADD.L Dq,<ea>                           012346|-|UUUUU|*****|  M+-WXZ  |1101_qqq_110_mmm_rrr
  public static StringBuilder disAddToMemLong (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    int ea = disOC & 63;
    if (XEiJ.EAM_MLT << ea < 0L) {
      return disEal (disDR (sb.append ("add.l   "), qqq).append (','), ea);
    }
    int mmm = ea >> 3;
    int rrr = ea & 7;
    if (mmm == XEiJ.MMM_DR) {
      return disDR (disDR (sb.append ("addx.l  "), rrr).append (','), qqq);
    }
    if (mmm == XEiJ.MMM_AR) {
      return disMN (disMN (sb.append ("addx.l  "), rrr).append (','), qqq);
    }
    return disIllegal (sb);
  }  //disAddToMemLong(StringBuilder)

  public static final String[] DIS_XXR_BYTE = { "asr.b   ", "lsr.b   ", "roxr.b  ", "ror.b   " };
  public static final String[] DIS_XXR_WORD = { "asr.w   ", "lsr.w   ", "roxr.w  ", "ror.w   " };
  public static final String[] DIS_XXR_LONG = { "asr.l   ", "lsr.l   ", "roxr.l  ", "ror.l   " };

  //ASR.B #<data>,Dr                        012346|-|UUUUU|***0*|          |1110_qqq_000_000_rrr
  //LSR.B #<data>,Dr                        012346|-|UUUUU|***0*|          |1110_qqq_000_001_rrr
  //ROXR.B #<data>,Dr                       012346|-|*UUUU|***0*|          |1110_qqq_000_010_rrr
  //ROR.B #<data>,Dr                        012346|-|-UUUU|-**0*|          |1110_qqq_000_011_rrr
  //ASR.B Dq,Dr                             012346|-|UUUUU|***0*|          |1110_qqq_000_100_rrr
  //LSR.B Dq,Dr                             012346|-|UUUUU|***0*|          |1110_qqq_000_101_rrr
  //ROXR.B Dq,Dr                            012346|-|*UUUU|***0*|          |1110_qqq_000_110_rrr
  //ROR.B Dq,Dr                             012346|-|-UUUU|-**0*|          |1110_qqq_000_111_rrr
  public static StringBuilder disXxrToRegByte (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    sb.append (DIS_XXR_BYTE[disOC >> 3 & 3]);
    int rrr = disOC & 7;
    return ((disOC & 0b100_000) == 0 ?
            disDR (disIM3 (sb, qqq).append (','), rrr) :
            disDR (disDR (sb, qqq).append (','), rrr));
  }  //disXxrToRegByte(StringBuilder)

  //ASR.W #<data>,Dr                        012346|-|UUUUU|***0*|          |1110_qqq_001_000_rrr
  //LSR.W #<data>,Dr                        012346|-|UUUUU|***0*|          |1110_qqq_001_001_rrr
  //ROXR.W #<data>,Dr                       012346|-|*UUUU|***0*|          |1110_qqq_001_010_rrr
  //ROR.W #<data>,Dr                        012346|-|-UUUU|-**0*|          |1110_qqq_001_011_rrr
  //ASR.W Dq,Dr                             012346|-|UUUUU|***0*|          |1110_qqq_001_100_rrr
  //LSR.W Dq,Dr                             012346|-|UUUUU|***0*|          |1110_qqq_001_101_rrr
  //ROXR.W Dq,Dr                            012346|-|*UUUU|***0*|          |1110_qqq_001_110_rrr
  //ROR.W Dq,Dr                             012346|-|-UUUU|-**0*|          |1110_qqq_001_111_rrr
  public static StringBuilder disXxrToRegWord (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    sb.append (DIS_XXR_WORD[disOC >> 3 & 3]);
    int rrr = disOC & 7;
    return ((disOC & 0b100_000) == 0 ?
            disDR (disIM3 (sb, qqq).append (','), rrr) :
            disDR (disDR (sb, qqq).append (','), rrr));
  }  //disXxrToRegWord(StringBuilder)

  //ASR.L #<data>,Dr                        012346|-|UUUUU|***0*|          |1110_qqq_010_000_rrr
  //LSR.L #<data>,Dr                        012346|-|UUUUU|***0*|          |1110_qqq_010_001_rrr
  //ROXR.L #<data>,Dr                       012346|-|*UUUU|***0*|          |1110_qqq_010_010_rrr
  //ROR.L #<data>,Dr                        012346|-|-UUUU|-**0*|          |1110_qqq_010_011_rrr
  //ASR.L Dq,Dr                             012346|-|UUUUU|***0*|          |1110_qqq_010_100_rrr
  //LSR.L Dq,Dr                             012346|-|UUUUU|***0*|          |1110_qqq_010_101_rrr
  //ROXR.L Dq,Dr                            012346|-|*UUUU|***0*|          |1110_qqq_010_110_rrr
  //ROR.L Dq,Dr                             012346|-|-UUUU|-**0*|          |1110_qqq_010_111_rrr
  public static StringBuilder disXxrToRegLong (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    sb.append (DIS_XXR_LONG[disOC >> 3 & 3]);
    int rrr = disOC & 7;
    return ((disOC & 0b100_000) == 0 ?
            disDR (disIM3 (sb, qqq).append (','), rrr) :
            disDR (disDR (sb, qqq).append (','), rrr));
  }  //disXxrToRegLong(StringBuilder)

  //ASR.W <ea>                              012346|-|UUUUU|***0*|  M+-WXZ  |1110_000_011_mmm_rrr
  public static StringBuilder disAsrToMem (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_MLT << ea < 0L) {
      return disEaw (sb.append ("asr.w   "), ea);
    }
    return disIllegal (sb);
  }  //disAsrToMem(StringBuilder)

  //LSR.W <ea>                              012346|-|UUUUU|*0*0*|  M+-WXZ  |1110_001_011_mmm_rrr
  public static StringBuilder disLsrToMem (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_MLT << ea < 0L) {
      return disEaw (sb.append ("lsr.w   "), ea);
    }
    return disIllegal (sb);
  }  //disLsrToMem(StringBuilder)

  //ROXR.W <ea>                             012346|-|*UUUU|***0*|  M+-WXZ  |1110_010_011_mmm_rrr
  public static StringBuilder disRoxrToMem (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_MLT << ea < 0L) {
      return disEaw (sb.append ("roxr.w  "), ea);
    }
    return disIllegal (sb);
  }  //disRoxrToMem(StringBuilder)

  //ROR.W <ea>                              012346|-|-UUUU|-**0*|  M+-WXZ  |1110_011_011_mmm_rrr
  public static StringBuilder disRorToMem (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_MLT << ea < 0L) {
      return disEaw (sb.append ("ror.w   "), ea);
    }
    return disIllegal (sb);
  }  //disRorToMem(StringBuilder)

  public static final String[] DIS_XXL_BYTE = { "asl.b   ", "lsl.b   ", "roxl.b  ", "rol.b   " };
  public static final String[] DIS_XXL_WORD = { "asl.w   ", "lsl.w   ", "roxl.w  ", "rol.w   " };
  public static final String[] DIS_XXL_LONG = { "asl.l   ", "lsl.l   ", "roxl.l  ", "rol.l   " };

  //ASL.B #<data>,Dr                        012346|-|UUUUU|*****|          |1110_qqq_100_000_rrr
  //LSL.B #<data>,Dr                        012346|-|UUUUU|***0*|          |1110_qqq_100_001_rrr
  //ROXL.B #<data>,Dr                       012346|-|*UUUU|***0*|          |1110_qqq_100_010_rrr
  //ROL.B #<data>,Dr                        012346|-|-UUUU|-**0*|          |1110_qqq_100_011_rrr
  //ASL.B Dq,Dr                             012346|-|UUUUU|*****|          |1110_qqq_100_100_rrr
  //LSL.B Dq,Dr                             012346|-|UUUUU|***0*|          |1110_qqq_100_101_rrr
  //ROXL.B Dq,Dr                            012346|-|*UUUU|***0*|          |1110_qqq_100_110_rrr
  //ROL.B Dq,Dr                             012346|-|-UUUU|-**0*|          |1110_qqq_100_111_rrr
  public static StringBuilder disXxlToRegByte (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    sb.append (DIS_XXL_BYTE[disOC >> 3 & 3]);
    int rrr = disOC & 7;
    return ((disOC & 0b100_000) == 0 ?
            disDR (disIM3 (sb, qqq).append (','), rrr) :
            disDR (disDR (sb, qqq).append (','), rrr));
  }  //disXxlToRegByte(StringBuilder)

  //ASL.W #<data>,Dr                        012346|-|UUUUU|*****|          |1110_qqq_101_000_rrr
  //LSL.W #<data>,Dr                        012346|-|UUUUU|***0*|          |1110_qqq_101_001_rrr
  //ROXL.W #<data>,Dr                       012346|-|*UUUU|***0*|          |1110_qqq_101_010_rrr
  //ROL.W #<data>,Dr                        012346|-|-UUUU|-**0*|          |1110_qqq_101_011_rrr
  //ASL.W Dq,Dr                             012346|-|UUUUU|*****|          |1110_qqq_101_100_rrr
  //LSL.W Dq,Dr                             012346|-|UUUUU|***0*|          |1110_qqq_101_101_rrr
  //ROXL.W Dq,Dr                            012346|-|*UUUU|***0*|          |1110_qqq_101_110_rrr
  //ROL.W Dq,Dr                             012346|-|-UUUU|-**0*|          |1110_qqq_101_111_rrr
  public static StringBuilder disXxlToRegWord (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    sb.append (DIS_XXL_WORD[disOC >> 3 & 3]);
    int rrr = disOC & 7;
    return ((disOC & 0b100_000) == 0 ?
            disDR (disIM3 (sb, qqq).append (','), rrr) :
            disDR (disDR (sb, qqq).append (','), rrr));
  }  //disXxlToRegWord(StringBuilder)

  //ASL.L #<data>,Dr                        012346|-|UUUUU|*****|          |1110_qqq_110_000_rrr
  //LSL.L #<data>,Dr                        012346|-|UUUUU|***0*|          |1110_qqq_110_001_rrr
  //ROXL.L #<data>,Dr                       012346|-|*UUUU|***0*|          |1110_qqq_110_010_rrr
  //ROL.L #<data>,Dr                        012346|-|-UUUU|-**0*|          |1110_qqq_110_011_rrr
  //ASL.L Dq,Dr                             012346|-|UUUUU|*****|          |1110_qqq_110_100_rrr
  //LSL.L Dq,Dr                             012346|-|UUUUU|***0*|          |1110_qqq_110_101_rrr
  //ROXL.L Dq,Dr                            012346|-|*UUUU|***0*|          |1110_qqq_110_110_rrr
  //ROL.L Dq,Dr                             012346|-|-UUUU|-**0*|          |1110_qqq_110_111_rrr
  public static StringBuilder disXxlToRegLong (StringBuilder sb) {
    int qqq = disOC >> 9 & 7;
    sb.append (DIS_XXL_LONG[disOC >> 3 & 3]);
    int rrr = disOC & 7;
    return ((disOC & 0b100_000) == 0 ?
            disDR (disIM3 (sb, qqq).append (','), rrr) :
            disDR (disDR (sb, qqq).append (','), rrr));
  }  //disXxlToRegLong(StringBuilder)

  //ASL.W <ea>                              012346|-|UUUUU|*****|  M+-WXZ  |1110_000_111_mmm_rrr
  public static StringBuilder disAslToMem (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_MLT << ea < 0L) {
      return disEaw (sb.append ("asl.w   "), ea);
    }
    return disIllegal (sb);
  }  //disAslToMem(StringBuilder)

  //LSL.W <ea>                              012346|-|UUUUU|***0*|  M+-WXZ  |1110_001_111_mmm_rrr
  public static StringBuilder disLslToMem (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_MLT << ea < 0L) {
      return disEaw (sb.append ("lsl.w   "), ea);
    }
    return disIllegal (sb);
  }  //disLslToMem(StringBuilder)

  //ROXL.W <ea>                             012346|-|*UUUU|***0*|  M+-WXZ  |1110_010_111_mmm_rrr
  public static StringBuilder disRoxlToMem (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_MLT << ea < 0L) {
      return disEaw (sb.append ("roxl.w  "), ea);
    }
    return disIllegal (sb);
  }  //disRoxlToMem(StringBuilder)

  //ROL.W <ea>                              012346|-|-UUUU|-**0*|  M+-WXZ  |1110_011_111_mmm_rrr
  public static StringBuilder disRolToMem (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_MLT << ea < 0L) {
      return disEaw (sb.append ("rol.w   "), ea);
    }
    return disIllegal (sb);
  }  //disRolToMem(StringBuilder)

  //BFTST <ea>{#o:#w}                       --2346|-|-UUUU|-**00|D M  WXZP |1110_100_011_mmm_rrr-0000_0oo_ooo_0ww_www
  //BFTST <ea>{#o:Dw}                       --2346|-|-UUUU|-**00|D M  WXZP |1110_100_011_mmm_rrr-0000_0oo_ooo_100_www
  //BFTST <ea>{Do:#w}                       --2346|-|-UUUU|-**00|D M  WXZP |1110_100_011_mmm_rrr-0000_100_ooo_0ww_www
  //BFTST <ea>{Do:Dw}                       --2346|-|-UUUU|-**00|D M  WXZP |1110_100_011_mmm_rrr-0000_100_ooo_100_www
  public static StringBuilder disBftst (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DCN << ea < 0L) {
      int w = MC68060.mmuPeekWordZeroCode ((disPC += 2) - 2, disSupervisor);
      return disBF (disEab (sb.append ("bftst   "), ea), w);
    }
    return disIllegal (sb);
  }  //disBftst(StringBuilder)

  //BFEXTU <ea>{#o:#w},Dn                   --2346|-|-UUUU|-**00|D M  WXZP |1110_100_111_mmm_rrr-0nnn_0oo_ooo_0ww_www
  //BFEXTU <ea>{#o:Dw},Dn                   --2346|-|-UUUU|-**00|D M  WXZP |1110_100_111_mmm_rrr-0nnn_0oo_ooo_100_www
  //BFEXTU <ea>{Do:#w},Dn                   --2346|-|-UUUU|-**00|D M  WXZP |1110_100_111_mmm_rrr-0nnn_100_ooo_0ww_www
  //BFEXTU <ea>{Do:Dw},Dn                   --2346|-|-UUUU|-**00|D M  WXZP |1110_100_111_mmm_rrr-0nnn_100_ooo_100_www
  public static StringBuilder disBfextu (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DCN << ea < 0L) {
      int w = MC68060.mmuPeekWordZeroCode ((disPC += 2) - 2, disSupervisor);
      return disDR (disBF (disEab (sb.append ("bfextu  "), ea), w).append (','), w >> 12 & 7);
    }
    return disIllegal (sb);
  }  //disBfextu(StringBuilder)

  //BFCHG <ea>{#o:#w}                       --2346|-|-UUUU|-**00|D M  WXZ  |1110_101_011_mmm_rrr-0000_0oo_ooo_0ww_www
  //BFCHG <ea>{#o:Dw}                       --2346|-|-UUUU|-**00|D M  WXZ  |1110_101_011_mmm_rrr-0000_0oo_ooo_100_www
  //BFCHG <ea>{Do:#w}                       --2346|-|-UUUU|-**00|D M  WXZ  |1110_101_011_mmm_rrr-0000_100_ooo_0ww_www
  //BFCHG <ea>{Do:Dw}                       --2346|-|-UUUU|-**00|D M  WXZ  |1110_101_011_mmm_rrr-0000_100_ooo_100_www
  public static StringBuilder disBfchg (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DCL << ea < 0L) {
      int w = MC68060.mmuPeekWordZeroCode ((disPC += 2) - 2, disSupervisor);
      return disBF (disEab (sb.append ("bfchg   "), ea), w);
    }
    return disIllegal (sb);
  }  //disBfchg(StringBuilder)

  //BFEXTS <ea>{#o:#w},Dn                   --2346|-|-UUUU|-**00|D M  WXZP |1110_101_111_mmm_rrr-0nnn_0oo_ooo_0ww_www
  //BFEXTS <ea>{#o:Dw},Dn                   --2346|-|-UUUU|-**00|D M  WXZP |1110_101_111_mmm_rrr-0nnn_0oo_ooo_100_www
  //BFEXTS <ea>{Do:#w},Dn                   --2346|-|-UUUU|-**00|D M  WXZP |1110_101_111_mmm_rrr-0nnn_100_ooo_0ww_www
  //BFEXTS <ea>{Do:Dw},Dn                   --2346|-|-UUUU|-**00|D M  WXZP |1110_101_111_mmm_rrr-0nnn_100_ooo_100_www
  public static StringBuilder disBfexts (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DCN << ea < 0L) {
      int w = MC68060.mmuPeekWordZeroCode ((disPC += 2) - 2, disSupervisor);
      return disDR (disBF (disEab (sb.append ("bfexts  "), ea), w).append (','), w >> 12 & 7);
    }
    return disIllegal (sb);
  }  //disBfexts(StringBuilder)

  //BFCLR <ea>{#o:#w}                       --2346|-|-UUUU|-**00|D M  WXZ  |1110_110_011_mmm_rrr-0000_0oo_ooo_0ww_www
  //BFCLR <ea>{#o:Dw}                       --2346|-|-UUUU|-**00|D M  WXZ  |1110_110_011_mmm_rrr-0000_0oo_ooo_100_www
  //BFCLR <ea>{Do:#w}                       --2346|-|-UUUU|-**00|D M  WXZ  |1110_110_011_mmm_rrr-0000_100_ooo_0ww_www
  //BFCLR <ea>{Do:Dw}                       --2346|-|-UUUU|-**00|D M  WXZ  |1110_110_011_mmm_rrr-0000_100_ooo_100_www
  public static StringBuilder disBfclr (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DCL << ea < 0L) {
      int w = MC68060.mmuPeekWordZeroCode ((disPC += 2) - 2, disSupervisor);
      return disBF (disEab (sb.append ("bfclr   "), ea), w);
    }
    return disIllegal (sb);
  }  //disBfclr(StringBuilder)

  //BFFFO <ea>{#o:#w},Dn                    --2346|-|-UUUU|-**00|D M  WXZP |1110_110_111_mmm_rrr-0nnn_0oo_ooo_0ww_www
  //BFFFO <ea>{#o:Dw},Dn                    --2346|-|-UUUU|-**00|D M  WXZP |1110_110_111_mmm_rrr-0nnn_0oo_ooo_100_www
  //BFFFO <ea>{Do:#w},Dn                    --2346|-|-UUUU|-**00|D M  WXZP |1110_110_111_mmm_rrr-0nnn_100_ooo_0ww_www
  //BFFFO <ea>{Do:Dw},Dn                    --2346|-|-UUUU|-**00|D M  WXZP |1110_110_111_mmm_rrr-0nnn_100_ooo_100_www
  public static StringBuilder disBfffo (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DCN << ea < 0L) {
      int w = MC68060.mmuPeekWordZeroCode ((disPC += 2) - 2, disSupervisor);
      return disDR (disBF (disEab (sb.append ("bfffo   "), ea), w).append (','), w >> 12 & 7);
    }
    return disIllegal (sb);
  }  //disBfffo(StringBuilder)

  //BFSET <ea>{#o:#w}                       --2346|-|-UUUU|-**00|D M  WXZ  |1110_111_011_mmm_rrr-0000_0oo_ooo_0ww_www
  //BFSET <ea>{#o:Dw}                       --2346|-|-UUUU|-**00|D M  WXZ  |1110_111_011_mmm_rrr-0000_0oo_ooo_100_www
  //BFSET <ea>{Do:#w}                       --2346|-|-UUUU|-**00|D M  WXZ  |1110_111_011_mmm_rrr-0000_100_ooo_0ww_www
  //BFSET <ea>{Do:Dw}                       --2346|-|-UUUU|-**00|D M  WXZ  |1110_111_011_mmm_rrr-0000_100_ooo_100_www
  public static StringBuilder disBfset (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DCL << ea < 0L) {
      int w = MC68060.mmuPeekWordZeroCode ((disPC += 2) - 2, disSupervisor);
      return disBF (disEab (sb.append ("bfset   "), ea), w);
    }
    return disIllegal (sb);
  }  //disBfset(StringBuilder)

  //BFINS Dn,<ea>{#o:#w}                    --2346|-|-UUUU|-**00|D M  WXZ  |1110_111_111_mmm_rrr-0nnn_0oo_ooo_0ww_www
  //BFINS Dn,<ea>{#o:Dw}                    --2346|-|-UUUU|-**00|D M  WXZ  |1110_111_111_mmm_rrr-0nnn_0oo_ooo_100_www
  //BFINS Dn,<ea>{Do:#w}                    --2346|-|-UUUU|-**00|D M  WXZ  |1110_111_111_mmm_rrr-0nnn_100_ooo_0ww_www
  //BFINS Dn,<ea>{Do:Dw}                    --2346|-|-UUUU|-**00|D M  WXZ  |1110_111_111_mmm_rrr-0nnn_100_ooo_100_www
  public static StringBuilder disBfins (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_DCL << ea < 0L) {
      int w = MC68060.mmuPeekWordZeroCode ((disPC += 2) - 2, disSupervisor);
      return disBF (disEab (disDR (sb.append ("bfins   "), w >> 12 & 7).append (','), ea), w);
    }
    return disIllegal (sb);
  }  //disBfins(StringBuilder)

  //PMOVE.L <ea>,TTn                        ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-00001n0000000000
  //PMOVEFD.L <ea>,TTn                      ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-00001n0100000000
  //PMOVE.L TTn,<ea>                        ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-00001n1000000000
  //PLOADW SFC,<ea>                         --M3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0010000000000000
  //PLOADW DFC,<ea>                         --M3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0010000000000001
  //PLOADW Dn,<ea>                          --M3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0010000000001nnn
  //PLOADW #<data>,<ea>                     ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0010000000010ddd
  //PLOADW #<data>,<ea>                     --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-001000000001dddd
  //PLOADR SFC,<ea>                         --M3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0010001000000000
  //PLOADR DFC,<ea>                         --M3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0010001000000001
  //PLOADR Dn,<ea>                          --M3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0010001000001nnn
  //PLOADR #<data>,<ea>                     ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0010001000010ddd
  //PLOADR #<data>,<ea>                     --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-001000100001dddd
  //PFLUSHA                                 --M---|P|-----|-----|          |1111_000_000_000_000-0010010000000000
  //PFLUSHA                                 ---3--|P|-----|-----|          |1111_000_000_000_000-0010010000000000
  //PVALID.L VAL,<ea>                       --M---|-|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0010100000000000
  //PVALID.L An,<ea>                        --M---|-|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0010110000000nnn
  //PFLUSH SFC,#<mask>                      --M---|P|-----|-----|          |1111_000_000_000_000-0011000mmmm00000
  //PFLUSH SFC,#<mask>                      ---3--|P|-----|-----|          |1111_000_000_000_000-00110000mmm00000
  //PFLUSH DFC,#<mask>                      --M---|P|-----|-----|          |1111_000_000_000_000-0011000mmmm00001
  //PFLUSH DFC,#<mask>                      ---3--|P|-----|-----|          |1111_000_000_000_000-00110000mmm00001
  //PFLUSH Dn,#<mask>                       --M---|P|-----|-----|          |1111_000_000_000_000-0011000mmmm01nnn
  //PFLUSH Dn,#<mask>                       ---3--|P|-----|-----|          |1111_000_000_000_000-00110000mmm01nnn
  //PFLUSH #<data>,#<mask>                  ---3--|P|-----|-----|          |1111_000_000_000_000-00110000mmm10ddd
  //PFLUSH #<data>,#<mask>                  --M---|P|-----|-----|          |1111_000_000_000_000-0011000mmmm1dddd
  //PFLUSHS SFC,#<mask>                     --M---|P|-----|-----|          |1111_000_000_000_000-0011010mmmm00000
  //PFLUSHS DFC,#<mask>                     --M---|P|-----|-----|          |1111_000_000_000_000-0011010mmmm00001
  //PFLUSHS Dn,#<mask>                      --M---|P|-----|-----|          |1111_000_000_000_000-0011010mmmm01nnn
  //PFLUSHS #<data>,#<mask>                 --M---|P|-----|-----|          |1111_000_000_000_000-0011010mmmm1dddd
  //PFLUSH SFC,#<mask>,<ea>                 --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0011100mmmm00000
  //PFLUSH SFC,#<mask>,<ea>                 ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-00111000mmm00000
  //PFLUSH DFC,#<mask>,<ea>                 --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0011100mmmm00001
  //PFLUSH DFC,#<mask>,<ea>                 ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-00111000mmm00001
  //PFLUSH Dn,#<mask>,<ea>                  ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-00111000mmm01nnn
  //PFLUSH #<data>,#<mask>,<ea>             ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-00111000mmm10ddd
  //PFLUSH Dn,#<mask>,<ea>                  --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0011100mmmm01nnn
  //PFLUSH #<data>,#<mask>,<ea>             --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0011100mmmm1dddd
  //PFLUSHS SFC,#<mask>,<ea>                --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0011110mmmm00000
  //PFLUSHS DFC,#<mask>,<ea>                --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0011110mmmm00001
  //PFLUSHS Dn,#<mask>,<ea>                 --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0011110mmmm01nnn
  //PFLUSHS #<data>,#<mask>,<ea>            --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0011110mmmm1dddd
  //PMOVE.L <ea>,TC                         ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0100000000000000
  //PMOVE.L <ea>,TC                         --M---|P|-----|-----|DAM+-WXZPI|1111_000_000_mmm_rrr-0100000000000000
  //PMOVEFD.L <ea>,TC                       ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0100000100000000
  //PMOVE.L TC,<ea>                         ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0100001000000000
  //PMOVE.L TC,<ea>                         --M---|P|-----|-----|DAM+-WXZ  |1111_000_000_mmm_rrr-0100001000000000
  //PMOVE.Q <ea>,DRP                        --M---|P|-----|-----|  M+-WXZPI|1111_000_000_mmm_rrr-0100010000000000
  //PMOVE.Q DRP,<ea>                        --M---|P|-----|-----|  M+-WXZ  |1111_000_000_mmm_rrr-0100011000000000
  //PMOVE.Q <ea>,SRP                        ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0100100000000000
  //PMOVE.Q <ea>,SRP                        --M---|P|-----|-----|  M+-WXZPI|1111_000_000_mmm_rrr-0100100000000000
  //PMOVEFD.Q <ea>,SRP                      ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0100100100000000
  //PMOVE.Q SRP,<ea>                        ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0100101000000000
  //PMOVE.Q SRP,<ea>                        --M---|P|-----|-----|  M+-WXZ  |1111_000_000_mmm_rrr-0100101000000000
  //PMOVE.Q <ea>,CRP                        ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0100110000000000
  //PMOVE.Q <ea>,CRP                        --M---|P|-----|-----|  M+-WXZPI|1111_000_000_mmm_rrr-0100110000000000
  //PMOVEFD.Q <ea>,CRP                      ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0100110100000000
  //PMOVE.Q CRP,<ea>                        ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0100111000000000
  //PMOVE.Q CRP,<ea>                        --M---|P|-----|-----|  M+-WXZ  |1111_000_000_mmm_rrr-0100111000000000
  //PMOVE.B <ea>,CAL                        --M---|P|-----|-----|DAM+-WXZPI|1111_000_000_mmm_rrr-0101000000000000
  //PMOVE.B CAL,<ea>                        --M---|P|-----|-----|DAM+-WXZ  |1111_000_000_mmm_rrr-0101001000000000
  //PMOVE.B <ea>,VAL                        --M---|P|-----|-----|DAM+-WXZPI|1111_000_000_mmm_rrr-0101010000000000
  //PMOVE.B VAL,<ea>                        --M---|P|-----|-----|DAM+-WXZ  |1111_000_000_mmm_rrr-0101011000000000
  //PMOVE.B <ea>,SCC                        --M---|P|-----|-----|DAM+-WXZPI|1111_000_000_mmm_rrr-0101100000000000
  //PMOVE.B SCC,<ea>                        --M---|P|-----|-----|DAM+-WXZ  |1111_000_000_mmm_rrr-0101101000000000
  //PMOVE.W <ea>,AC                         --M---|P|-----|-----|DAM+-WXZPI|1111_000_000_mmm_rrr-0101110000000000
  //PMOVE.W AC,<ea>                         --M---|P|-----|-----|DAM+-WXZ  |1111_000_000_mmm_rrr-0101111000000000
  //PMOVE.W <ea>,MMUSR                      ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0110000000000000
  //PMOVE.W <ea>,PSR                        --M---|P|-----|-----|DAM+-WXZPI|1111_000_000_mmm_rrr-0110000000000000
  //PMOVE.W MMUSR,<ea>                      ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-0110001000000000
  //PMOVE.W PSR,<ea>                        --M---|P|-----|-----|DAM+-WXZ  |1111_000_000_mmm_rrr-0110001000000000
  //PMOVE.W <ea>,PCSR                       --M---|P|-----|-----|DAM+-WXZPI|1111_000_000_mmm_rrr-0110010000000000
  //PMOVE.W PCSR,<ea>                       --M---|P|-----|-----|DAM+-WXZ  |1111_000_000_mmm_rrr-0110011000000000
  //PMOVE.W <ea>,BADn                       --M---|P|-----|-----|DAM+-WXZPI|1111_000_000_mmm_rrr-01110000000nnn00
  //PMOVE.W BADn,<ea>                       --M---|P|-----|-----|DAM+-WXZ  |1111_000_000_mmm_rrr-01110010000nnn00
  //PMOVE.W <ea>,BACn                       --M---|P|-----|-----|DAM+-WXZPI|1111_000_000_mmm_rrr-01110100000nnn00
  //PMOVE.W BACn,<ea>                       --M---|P|-----|-----|DAM+-WXZ  |1111_000_000_mmm_rrr-01110110000nnn00
  //PTESTW SFC,<ea>,#<level>                --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll0000000000
  //PTESTW SFC,<ea>,#<level>                ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll0000000000
  //PTESTW DFC,<ea>,#<level>                --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll0000000001
  //PTESTW DFC,<ea>,#<level>                ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll0000000001
  //PTESTW Dn,<ea>,#<level>                 --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll0000001nnn
  //PTESTW Dn,<ea>,#<level>                 ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll0000001nnn
  //PTESTW #<data>,<ea>,#<level>            --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll000001dddd
  //PTESTW #<data>,<ea>,#<level>            ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll0000010ddd
  //PTESTW SFC,<ea>,#<level>,An             --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll01nnn00000
  //PTESTW SFC,<ea>,#<level>,An             ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll01nnn00000
  //PTESTW DFC,<ea>,#<level>,An             --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll01nnn00001
  //PTESTW DFC,<ea>,#<level>,An             ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll01nnn00001
  //PTESTW Dn,<ea>,#<level>,An              --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll01nnn01nnn
  //PTESTW Dn,<ea>,#<level>,An              ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll01nnn01nnn
  //PTESTW #<data>,<ea>,#<level>,An         --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll01nnn1dddd
  //PTESTW #<data>,<ea>,#<level>,An         ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll01nnn10ddd
  //PTESTR SFC,<ea>,#<level>                --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll1000000000
  //PTESTR SFC,<ea>,#<level>                ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll1000000000
  //PTESTR DFC,<ea>,#<level>                --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll1000000001
  //PTESTR DFC,<ea>,#<level>                ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll1000000001
  //PTESTR Dn,<ea>,#<level>                 --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll1000001nnn
  //PTESTR Dn,<ea>,#<level>                 ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll1000001nnn
  //PTESTR #<data>,<ea>,#<level>            --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll100001dddd
  //PTESTR #<data>,<ea>,#<level>            ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll1000010ddd
  //PTESTR SFC,<ea>,#<level>,An             --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll11nnn00000
  //PTESTR SFC,<ea>,#<level>,An             ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll11nnn00000
  //PTESTR DFC,<ea>,#<level>,An             --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll11nnn00001
  //PTESTR DFC,<ea>,#<level>,An             ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll11nnn00001
  //PTESTR Dn,<ea>,#<level>,An              --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll11nnn01nnn
  //PTESTR Dn,<ea>,#<level>,An              ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll11nnn01nnn
  //PTESTR #<data>,<ea>,#<level>,An         --M---|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll11nnn1dddd
  //PTESTR #<data>,<ea>,#<level>,An         ---3--|P|-----|-----|  M  WXZ  |1111_000_000_mmm_rrr-100lll11nnn10ddd
  //PFLUSHR <ea>                            --M---|P|-----|-----|  M+-WXZPI|1111_000_000_mmm_rrr-1010000000000000
  public static StringBuilder disPgen (StringBuilder sb) {
    //!!!
    return disIllegal (sb);
  }  //disPgen(StringBuilder)

  public static final String[] DIS_MM_CC = (
    "bs," +  //000000
    "bc," +  //000001
    "ls," +  //000010
    "lc," +  //000011
    "ss," +  //000100
    "sc," +  //000101
    "as," +  //000110
    "ac," +  //000111
    "ws," +  //001000
    "wc," +  //001001
    "is," +  //001010
    "ic," +  //001011
    "gs," +  //001100
    "gc," +  //001101
    "cs," +  //001110
    "cc"     //001111
    ).split (",");

  //PDBcc.W Dn,<label>                      --M---|P|-----|-----|          |1111_000_001_001_nnn-0000000000cccccc-{offset}
  //PTRAPcc.W #<data>                       --M---|P|-----|-----|          |1111_000_001_111_010-0000000000cccccc-{data}
  //PTRAPcc.L #<data>                       --M---|P|-----|-----|          |1111_000_001_111_011-0000000000cccccc-{data}
  //PTRAPcc                                 --M---|P|-----|-----|          |1111_000_001_111_100-0000000000cccccc
  //PScc.B <ea>                             --M---|P|-----|-----|D M+-WXZ  |1111_000_001_mmm_rrr-0000000000cccccc
  public static StringBuilder disPscc (StringBuilder sb) {
    int c = MC68060.mmuPeekWordZeroCode ((disPC += 2) - 2, disSupervisor) & 63;
    if (c < DIS_MM_CC.length) {
      int ea = disOC & 63;
      if (ea >> 3 == XEiJ.MMM_AR) {
        disStatus = DIS_SOMETIMES_BRANCH;
        return disOW (disDR (disOp3 (sb, "pdb", DIS_MM_CC[c], ".w"), disOC & 7).append (','));  //FDBccはUnsized、PDBccはWordらしい
      }
      if (ea == XEiJ.EA_PW) {
        return disIMw (disOp3 (sb, "ptrap", DIS_MM_CC[c], ".w"));
      }
      if (ea == XEiJ.EA_PX) {
        return disIMl (disOp3 (sb, "ptrap", DIS_MM_CC[c], ".l"));
      }
      if (ea == XEiJ.EA_IM) {
        return sb.append ("ptrap").append (DIS_MM_CC[c]);
      }
      if (XEiJ.EAM_DAT << ea < 0L) {
        return disEab (disOp3 (sb, "ps", DIS_MM_CC[c], ".b"), ea);
      }
    }
    return disIllegal (sb);
  }  //disPscc(StringBuilder)

  //PBcc.W <label>                          --M---|P|-----|-----|          |1111_000_010_ccc_ccc-{offset}
  public static StringBuilder disPbccWord (StringBuilder sb) {
    int c = disOC & 63;
    if (c < DIS_MM_CC.length) {
      disStatus = DIS_SOMETIMES_BRANCH;
      return disOW (disOp3 (sb, "pb", DIS_MM_CC[c], ".w"));
    }
    return disIllegal (sb);
  }  //disPbccWord(StringBuilder)

  //PBcc.L <label>                          --M---|P|-----|-----|          |1111_000_011_ccc_ccc-{offset}
  public static StringBuilder disPbccLong (StringBuilder sb) {
    int c = disOC & 63;
    if (c < DIS_MM_CC.length) {
      disStatus = DIS_SOMETIMES_BRANCH;
      return disOL (disOp3 (sb, "pb", DIS_MM_CC[c], ".l"));
    }
    return disIllegal (sb);
  }  //disPbccLong(StringBuilder)

  //PSAVE <ea>                              --M---|P|-----|-----|  M -WXZ  |1111_000_100_mmm_rrr
  public static StringBuilder disPsave (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_WTL << ea < 0L) {
      return disEaz (sb.append ("psave   "), ea);
    }
    return disIllegal (sb);
  }  //disPsave(StringBuilder)

  //PRESTORE <ea>                           --M---|P|-----|-----|  M+ WXZP |1111_000_101_mmm_rrr
  public static StringBuilder disPrestore (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_RDL << ea < 0L) {
      return disEaz (sb.append ("prestore        "), ea);
    }
    return disIllegal (sb);
  }  //disPrestore(StringBuilder)

  //CINVL NC,(An)                           ----46|P|-----|-----|          |1111_010_000_001_nnn
  //CINVP NC,(An)                           ----46|P|-----|-----|          |1111_010_000_010_nnn
  //CINVA NC                                ----46|P|-----|-----|          |1111_010_000_011_000
  //CPUSHL NC,(An)                          ----46|P|-----|-----|          |1111_010_000_101_nnn
  //INTOUCH (An)                            ------|P|-----|-----|          |1111_010_000_101_nnn    (ISA_B)
  //CPUSHP NC,(An)                          ----46|P|-----|-----|          |1111_010_000_110_nnn
  //CPUSHA NC                               ----46|P|-----|-----|          |1111_010_000_111_000
  //CINVL DC,(An)                           ----46|P|-----|-----|          |1111_010_001_001_nnn
  //CINVP DC,(An)                           ----46|P|-----|-----|          |1111_010_001_010_nnn
  //CINVA DC                                ----46|P|-----|-----|          |1111_010_001_011_000
  //CPUSHL DC,(An)                          ----46|P|-----|-----|          |1111_010_001_101_nnn
  //CPUSHP DC,(An)                          ----46|P|-----|-----|          |1111_010_001_110_nnn
  //CPUSHA DC                               ----46|P|-----|-----|          |1111_010_001_111_000
  //CINVL IC,(An)                           ----46|P|-----|-----|          |1111_010_010_001_nnn
  //CINVP IC,(An)                           ----46|P|-----|-----|          |1111_010_010_010_nnn
  //CINVA IC                                ----46|P|-----|-----|          |1111_010_010_011_000
  //CPUSHL IC,(An)                          ----46|P|-----|-----|          |1111_010_010_101_nnn
  //CPUSHP IC,(An)                          ----46|P|-----|-----|          |1111_010_010_110_nnn
  //CPUSHA IC                               ----46|P|-----|-----|          |1111_010_010_111_000
  //CINVL BC,(An)                           ----46|P|-----|-----|          |1111_010_011_001_nnn
  //CINVP BC,(An)                           ----46|P|-----|-----|          |1111_010_011_010_nnn
  //CINVA BC                                ----46|P|-----|-----|          |1111_010_011_011_000
  //CPUSHL BC,(An)                          ----46|P|-----|-----|          |1111_010_011_101_nnn
  //CPUSHP BC,(An)                          ----46|P|-----|-----|          |1111_010_011_110_nnn
  //CPUSHA BC                               ----46|P|-----|-----|          |1111_010_011_111_000
  public static StringBuilder disCinvCpush (StringBuilder sb) {
    int mmm = disOC >> 3 & 7;
    if ((disOC & 0b011_000) != 0) {
      sb.append ('c').append ((disOC & 0b100_000) == 0 ? "inv" : "push").
        append ((char) (('a' << 24 | 'p' << 16 | 'l' << 8) >> (disOC & 0b011_000) & 255)).
          append ((disOC & 0b100_000) == 0 ? "   " : "  ").
            append ((char) (('b' << 24 | 'i' << 16 | 'd' << 8 | 'n') >> ((disOC & 0b011_000_000) >> 3) & 255)).append ('c');
      if ((disOC & 0b011_000) != 0b011_000) {
        sb.append (",(a").append ((char) ('0' + (disOC & 7))).append (')');
      }
      return sb;
    }
    return disIllegal (sb);
  }  //disCinvCpush(StringBuilder)

  //PFLUSHN (An)                            ----46|P|-----|-----|          |1111_010_100_000_nnn
  //PFLUSH (An)                             ----46|P|-----|-----|          |1111_010_100_001_nnn
  //PFLUSHAN                                ----46|P|-----|-----|          |1111_010_100_010_000
  //PFLUSHA                                 ----46|P|-----|-----|          |1111_010_100_011_000
  public static StringBuilder disPflush (StringBuilder sb) {
    int an = disOC >> 3 & 7;
    if (an <= 3) {
      sb.append (an == 0 ? "pflushn " : an == 1 ? "pflush  " : an == 2 ? "pflushan " : "pflusha");
      if (an <= 1) {
        sb.append ("(a").append ((char) ('0' + (disOC & 7))).append (')');
      }
      return sb;
    }
    return disIllegal (sb);
  }  //disPflush(StringBuilder)

  //PTESTW (An)                             ----4-|P|-----|-----|          |1111_010_101_001_nnn
  //PTESTR (An)                             ----4-|P|-----|-----|          |1111_010_101_101_nnn
  public static StringBuilder disPtest (StringBuilder sb) {
    int wr = disOC >> 3 & 7;
    if ((wr & 3) == 1) {
      return sb.append (wr == 1 ? "ptestw  (a" : "ptestr  (a").append ((char) ('0' + (disOC & 7))).append (')');
    }
    return disIllegal (sb);
  }  //disPtest(StringBuilder)

  //PLPAW (An)                              -----6|P|-----|-----|          |1111_010_110_001_nnn
  public static StringBuilder disPlpaw (StringBuilder sb) {
    return sb.append ("plpaw   (a").append ((char) ('0' + (disOC & 7))).append (')');
  }  //disPlpaw(StringBuilder)

  //PLPAR (An)                              -----6|P|-----|-----|          |1111_010_111_001_nnn
  public static StringBuilder disPlpar (StringBuilder sb) {
    return sb.append ("plpar   (a").append ((char) ('0' + (disOC & 7))).append (')');
  }  //disPlpar(StringBuilder)

  //MOVE16 (An)+,xxx.L                      ----46|-|-----|-----|          |1111_011_000_000_nnn-{address}
  //MOVE16 xxx.L,(An)+                      ----46|-|-----|-----|          |1111_011_000_001_nnn-{address}
  //MOVE16 (An),xxx.L                       ----46|-|-----|-----|          |1111_011_000_010_nnn-{address}
  //MOVE16 xxx.L,(An)                       ----46|-|-----|-----|          |1111_011_000_011_nnn-{address}
  //MOVE16 (Ax)+,(Ay)+                      ----46|-|-----|-----|          |1111_011_000_100_xxx-1yyy000000000000
  public static StringBuilder disMove16 (StringBuilder sb) {
    switch (disOC >> 3 & 7) {
    case 0:
      return disZL (sb.append ("move16  (a").append ((char) ('0' + (disOC & 7))).append (")+,"));
    case 1:
      return disZL (sb.append ("move16  ")).append (",(a").append ((char) ('0' + (disOC & 7))).append (")+");
    case 2:
      return disZL (sb.append ("move16  (a").append ((char) ('0' + (disOC & 7))).append ("),"));
    case 3:
      return disZL (sb.append ("move16  ")).append (",(a").append ((char) ('0' + (disOC & 7))).append (')');
    case 4:
      int w = MC68060.mmuPeekWordZeroCode ((disPC += 2) - 2, disSupervisor);  //拡張ワード
      if ((w & 0b1000111111111111) == 0b1000000000000000) {
        return sb.append ("move16  (a").append ((char) ('0' + (disOC & 7))).append (")+,(a").append ((char) ('0' + (w >> 12 & 7))).append (")+");
      }
    }
    return disIllegal (sb);
  }  //disMove16(StringBuilder)

  //LPSTOP.W #<data>                        -----6|P|-----|-----|          |1111_100_000_000_000-0000000111000000-{data}
  public static StringBuilder disLpstop (StringBuilder sb) {
    if (disOC == 0xf800) {
      int w = MC68060.mmuPeekWordZeroCode ((disPC += 2) - 2, disSupervisor);  //拡張ワード
      if (w == 0x01c0) {
        return disIMw (sb.append ("lpstop.w "));
      }
    }
    return disIllegal (sb);
  }  //disLpstop(StringBuilder)

  //WDDATA.B <ea>                           ------|-|-----|-----|  M+-WXZ  |1111_101_100_mmm_rrr    (ISA_A)
  public static StringBuilder disWddataByte (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_MLT << ea < 0L) {
      return disEab (sb.append ("wddata.b "), ea);
    }
    return disIllegal (sb);
  }  //disWddataByte(StringBuilder)

  //WDDATA.W <ea>                           ------|-|-----|-----|  M+-WXZ  |1111_101_101_mmm_rrr    (ISA_A)
  public static StringBuilder disWddataWord (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_MLT << ea < 0L) {
      return disEaw (sb.append ("wddata.w "), ea);
    }
    return disIllegal (sb);
  }  //disWddataWord(StringBuilder)

  //WDDATA.L <ea>                           ------|-|-----|-----|  M+-WXZ  |1111_101_110_mmm_rrr    (ISA_A)
  public static StringBuilder disWddataLong (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_MLT << ea < 0L) {
      return disEal (sb.append ("wddata.l "), ea);
    }
    return disIllegal (sb);
  }  //disWddataLong(StringBuilder)

  //WDEBUG.L <ea>                           ------|P|-----|-----|  M  W    |1111_101_111_mmm_rrr-0000000000000011   (ISA_A)
  public static StringBuilder disWdebug (StringBuilder sb) {
    int ea = disOC & 63;
    if (ea == XEiJ.EA_MM || ea == XEiJ.EA_MW) {
      int w = MC68060.mmuPeekWordZeroCode ((disPC += 2) - 2, disSupervisor);  //拡張ワード
      if (w == 3) {
        return disEal (sb.append ("wdebug.l "), ea);
      }
    }
    return disIllegal (sb);
  }  //disWdebug(StringBuilder)

  //FPACK <name>                            012346|-|-----|-----|          |1111_111_0xx_xxx_xxx
  public static StringBuilder disFpack (StringBuilder sb) {
    String name = DIS_FPACK_NAME[disOC & 255];
    if (name.length () != 0) {
      return sb.append ("FPACK   ").append (name);
    }
    return disFline (sb);
  }  //disFpack(StringBuilder)

  //DOS <name>                              012346|-|-----|-----|          |1111_111_1xx_xxx_xxx
  //line 1111 emulator                      012346|-|-----|-----|          |1111_xxx_xxx_xxx_xxx
  public static StringBuilder disDos (StringBuilder sb) {
    if (disOC == 0xff00 || disOC == 0xff4c) {  //_EXIT,_EXIT2
      disStatus = DIS_ALWAYS_BRANCH;
    }
    String name = DIS_DOS_NAME[disOC & 255];
    if (name.length () != 0) {
      return sb.append ("DOS     ").append (name);
    }
    return disFline (sb);
  }  //disDos(StringBuilder)

  //line 1111 emulator                      012346|-|-----|-----|          |1111_xxx_xxx_xxx_xxx
  public static StringBuilder disFline (StringBuilder sb) {
    return disHex4 (sb.append (".dc.w   "), disOC);
  }  //disFline(StringBuilder)

  public static final String[] DIS_FP_SZ = { ".l", ".s", ".x", ".p", ".w", ".d", ".b", ".p" };

  public static final String[] DIS_FP_FGEN = (
    "fmove,"   +  //000_0000
    "fint,"    +  //000_0001
    "fsinh,"   +  //000_0010
    "fintrz,"  +  //000_0011
    "fsqrt,"   +  //000_0100
    ","        +  //000_0101
    "flognp1," +  //000_0110
    ","        +  //000_0111
    "fetoxm1," +  //000_1000
    "ftanh,"   +  //000_1001
    "fatan,"   +  //000_1010
    ","        +  //000_1011
    "fasin,"   +  //000_1100
    "fatanh,"  +  //000_1101
    "fsin,"    +  //000_1110
    "ftan,"    +  //000_1111
    "fetox,"   +  //001_0000
    "ftwotox," +  //001_0001
    "ftentox," +  //001_0010
    ","        +  //001_0011
    "flogn,"   +  //001_0100
    "flog10,"  +  //001_0101
    "flog2,"   +  //001_0110
    ","        +  //001_0111
    "fabs,"    +  //001_1000
    "fcosh,"   +  //001_1001
    "fneg,"    +  //001_1010
    ","        +  //001_1011
    "facos,"   +  //001_1100
    "fcos,"    +  //001_1101
    "fgetexp," +  //001_1110
    "fgetman," +  //001_1111
    "fdiv,"    +  //010_0000
    "fmod,"    +  //010_0001
    "fadd,"    +  //010_0010
    "fmul,"    +  //010_0011
    "fsgldiv," +  //010_0100
    "frem,"    +  //010_0101
    "fscale,"  +  //010_0110
    "fsglmul," +  //010_0111
    "fsub,"    +  //010_1000
    ","        +  //010_1001
    ","        +  //010_1010
    ","        +  //010_1011
    ","        +  //010_1100
    ","        +  //010_1101
    ","        +  //010_1110
    ","        +  //010_1111
    "fsincos," +  //011_0000
    "fsincos," +  //011_0001
    "fsincos," +  //011_0010
    "fsincos," +  //011_0011
    "fsincos," +  //011_0100
    "fsincos," +  //011_0101
    "fsincos," +  //011_0110
    "fsincos," +  //011_0111
    "fcmp,"    +  //011_1000
    ","        +  //011_1001
    "ftst,"    +  //011_1010
    ","        +  //011_1011
    ","        +  //011_1100
    ","        +  //011_1101
    ","        +  //011_1110
    ","        +  //011_1111
    "fsmove,"  +  //100_0000
    "fssqrt,"  +  //100_0001
    ","        +  //100_0010
    ","        +  //100_0011
    "fdmove,"  +  //100_0100
    "fdsqrt,"  +  //100_0101
    ","        +  //100_0110
    ","        +  //100_0111
    ","        +  //100_1000
    ","        +  //100_1001
    ","        +  //100_1010
    ","        +  //100_1011
    ","        +  //100_1100
    ","        +  //100_1101
    ","        +  //100_1110
    ","        +  //100_1111
    ","        +  //101_0000
    ","        +  //101_0001
    ","        +  //101_0010
    ","        +  //101_0011
    ","        +  //101_0100
    ","        +  //101_0101
    ","        +  //101_0110
    ","        +  //101_0111
    "fsabs,"   +  //101_1000
    ","        +  //101_1001
    "fsneg,"   +  //101_1010
    ","        +  //101_1011
    "fdabs,"   +  //101_1100
    ","        +  //101_1101
    "fdneg,"   +  //101_1110
    ","        +  //101_1111
    "fsdiv,"   +  //110_0000
    ","        +  //110_0001
    "fsadd,"   +  //110_0010
    "fsmul,"   +  //110_0011
    "fddiv,"   +  //110_0100
    ","        +  //110_0101
    "fdadd,"   +  //110_0110
    "fdmul,"   +  //110_0111
    "fssub,"   +  //110_1000
    ","        +  //110_1001
    ","        +  //110_1010
    ","        +  //110_1011
    "fdsub,"   +  //110_1100
    ","        +  //110_1101
    ","        +  //110_1110
    ","        +  //110_1111
    "flgamma," +  //111_0000  (EFPBox.EPB_EXTRA_OPERATION ? "flgamma," : ",") +
    "ftgamma," +  //111_0001  (EFPBox.EPB_EXTRA_OPERATION ? "ftgamma," : ",") +
    ","        +  //111_0010
    ","        +  //111_0011
    ","        +  //111_0100
    ","        +  //111_0101
    ","        +  //111_0110
    ","        +  //111_0111
    ","        +  //111_1000
    ","        +  //111_1001
    ","        +  //111_1010
    ","        +  //111_1011
    ","        +  //111_1100
    ","        +  //111_1101
    ","        +  //111_1110
    ""            //111_1111
    ).split (",", 128);  //limitを書かないと末尾の空文字列が削除されて配列が短くなり末尾の要素を参照できなくなる

  public static final String[] DIS_FP_CC = (
    "f,"   +  //000000
    "eq,"  +  //000001
    "ogt," +  //000010
    "oge," +  //000011
    "olt," +  //000100
    "ole," +  //000101
    "ogl," +  //000110
    "or,"  +  //000111
    "un,"  +  //001000
    "ueq," +  //001001
    "ugt," +  //001010
    "uge," +  //001011
    "ult," +  //001100
    "ule," +  //001101
    "ne,"  +  //001110
    "t,"   +  //001111
    "sf,"  +  //010000
    "seq," +  //010001
    "gt,"  +  //010010
    "ge,"  +  //010011
    "lt,"  +  //010100
    "le,"  +  //010101
    "gl,"  +  //010110
    "gle," +  //010111
    "ngle,"+  //011000
    "ngl," +  //011001
    "nle," +  //011010
    "nlt," +  //011011
    "nge," +  //011100
    "ngt," +  //011101
    "sne," +  //011110
    "st"      //011111
    ).split (",");

  public static final String[] DIS_FP_CR = { "fpcr", "fpsr", "fpiar" };

  //FMOVE.X FPm,FPn                         --CC46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_000_0000
  //FINT.X FPm,FPn                          --CCS6|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_000_0001
  //FSINH.X FPm,FPn                         --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_000_0010
  //FINTRZ.X FPm,FPn                        --CCS6|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_000_0011
  //FSQRT.X FPm,FPn                         --CC46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_000_0100
  //FLOGNP1.X FPm,FPn                       --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_000_0110
  //FETOXM1.X FPm,FPn                       --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_000_1000
  //FTANH.X FPm,FPn                         --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_000_1001
  //FATAN.X FPm,FPn                         --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_000_1010
  //FASIN.X FPm,FPn                         --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_000_1100
  //FATANH.X FPm,FPn                        --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_000_1101
  //FSIN.X FPm,FPn                          --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_000_1110
  //FTAN.X FPm,FPn                          --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_000_1111
  //FETOX.X FPm,FPn                         --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_001_0000
  //FTWOTOX.X FPm,FPn                       --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_001_0001
  //FTENTOX.X FPm,FPn                       --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_001_0010
  //FLOGN.X FPm,FPn                         --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_001_0100
  //FLOG10.X FPm,FPn                        --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_001_0101
  //FLOG2.X FPm,FPn                         --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_001_0110
  //FABS.X FPm,FPn                          --CC46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_001_1000
  //FCOSH.X FPm,FPn                         --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_001_1001
  //FNEG.X FPm,FPn                          --CC46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_001_1010
  //FACOS.X FPm,FPn                         --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_001_1100
  //FCOS.X FPm,FPn                          --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_001_1101
  //FGETEXP.X FPm,FPn                       --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_001_1110
  //FGETMAN.X FPm,FPn                       --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_001_1111
  //FDIV.X FPm,FPn                          --CC46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_010_0000
  //FMOD.X FPm,FPn                          --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_010_0001
  //FADD.X FPm,FPn                          --CC46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_010_0010
  //FMUL.X FPm,FPn                          --CC46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_010_0011
  //FSGLDIV.X FPm,FPn                       --CCS6|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_010_0100
  //FREM.X FPm,FPn                          --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_010_0101
  //FSCALE.X FPm,FPn                        --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_010_0110
  //FSGLMUL.X FPm,FPn                       --CCS6|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_010_0111
  //FSUB.X FPm,FPn                          --CC46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_010_1000
  //FSINCOS.X FPm,FPc:FPs                   --CCSS|-|-----|-----|          |1111_001_000_000_000-000_mmm_sss_011_0ccc
  //FCMP.X FPm,FPn                          --CC46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_011_1000
  //FTST.X FPm                              --CC46|-|-----|-----|          |1111_001_000_000_000-000_mmm_000_011_1010
  //FSMOVE.X FPm,FPn                        ----46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_100_0000
  //FSSQRT.X FPm,FPn                        ----46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_100_0001
  //FDMOVE.X FPm,FPn                        ----46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_100_0100
  //FDSQRT.X FPm,FPn                        ----46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_100_0101
  //FSABS.X FPm,FPn                         ----46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_101_1000
  //FSNEG.X FPm,FPn                         ----46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_101_1010
  //FDABS.X FPm,FPn                         ----46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_101_1100
  //FDNEG.X FPm,FPn                         ----46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_101_1110
  //FSDIV.X FPm,FPn                         ----46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_110_0000
  //FSADD.X FPm,FPn                         ----46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_110_0010
  //FSMUL.X FPm,FPn                         ----46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_110_0011
  //FDDIV.X FPm,FPn                         ----46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_110_0100
  //FDADD.X FPm,FPn                         ----46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_110_0110
  //FDMUL.X FPm,FPn                         ----46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_110_0111
  //FSSUB.X FPm,FPn                         ----46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_110_1000
  //FDSUB.X FPm,FPn                         ----46|-|-----|-----|          |1111_001_000_000_000-000_mmm_nnn_110_1100
  //FMOVE.L <ea>,FPn                        --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_000_0000
  //FMOVE.S <ea>,FPn                        --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_000_0000
  //FMOVE.X <ea>,FPn                        --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_000_0000
  //FMOVE.P <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_000_0000
  //FMOVE.W <ea>,FPn                        --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_000_0000
  //FMOVE.D <ea>,FPn                        --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_000_0000
  //FMOVE.B <ea>,FPn                        --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_000_0000
  //FINT.L <ea>,FPn                         --CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_000_0001
  //FINT.S <ea>,FPn                         --CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_000_0001
  //FINT.X <ea>,FPn                         --CCS6|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_000_0001
  //FINT.P <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_000_0001
  //FINT.W <ea>,FPn                         --CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_000_0001
  //FINT.D <ea>,FPn                         --CCS6|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_000_0001
  //FINT.B <ea>,FPn                         --CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_000_0001
  //FSINH.L <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_000_0010
  //FSINH.S <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_000_0010
  //FSINH.X <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_000_0010
  //FSINH.P <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_000_0010
  //FSINH.W <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_000_0010
  //FSINH.D <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_000_0010
  //FSINH.B <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_000_0010
  //FINTRZ.L <ea>,FPn                       --CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_000_0011
  //FINTRZ.S <ea>,FPn                       --CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_000_0011
  //FINTRZ.X <ea>,FPn                       --CCS6|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_000_0011
  //FINTRZ.P <ea>,FPn                       --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_000_0011
  //FINTRZ.W <ea>,FPn                       --CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_000_0011
  //FINTRZ.D <ea>,FPn                       --CCS6|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_000_0011
  //FINTRZ.B <ea>,FPn                       --CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_000_0011
  //FSQRT.L <ea>,FPn                        --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_000_0100
  //FSQRT.S <ea>,FPn                        --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_000_0100
  //FSQRT.X <ea>,FPn                        --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_000_0100
  //FSQRT.P <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_000_0100
  //FSQRT.W <ea>,FPn                        --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_000_0100
  //FSQRT.D <ea>,FPn                        --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_000_0100
  //FSQRT.B <ea>,FPn                        --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_000_0100
  //FLOGNP1.L <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_000_0110
  //FLOGNP1.S <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_000_0110
  //FLOGNP1.X <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_000_0110
  //FLOGNP1.P <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_000_0110
  //FLOGNP1.W <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_000_0110
  //FLOGNP1.D <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_000_0110
  //FLOGNP1.B <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_000_0110
  //FETOXM1.L <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_000_1000
  //FETOXM1.S <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_000_1000
  //FETOXM1.X <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_000_1000
  //FETOXM1.P <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_000_1000
  //FETOXM1.W <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_000_1000
  //FETOXM1.D <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_000_1000
  //FETOXM1.B <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_000_1000
  //FTANH.L <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_000_1001
  //FTANH.S <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_000_1001
  //FTANH.X <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_000_1001
  //FTANH.P <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_000_1001
  //FTANH.W <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_000_1001
  //FTANH.D <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_000_1001
  //FTANH.B <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_000_1001
  //FATAN.L <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_000_1010
  //FATAN.S <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_000_1010
  //FATAN.X <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_000_1010
  //FATAN.P <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_000_1010
  //FATAN.W <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_000_1010
  //FATAN.D <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_000_1010
  //FATAN.B <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_000_1010
  //FASIN.L <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_000_1100
  //FASIN.S <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_000_1100
  //FASIN.X <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_000_1100
  //FASIN.P <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_000_1100
  //FASIN.W <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_000_1100
  //FASIN.D <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_000_1100
  //FASIN.B <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_000_1100
  //FATANH.L <ea>,FPn                       --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_000_1101
  //FATANH.S <ea>,FPn                       --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_000_1101
  //FATANH.X <ea>,FPn                       --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_000_1101
  //FATANH.P <ea>,FPn                       --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_000_1101
  //FATANH.W <ea>,FPn                       --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_000_1101
  //FATANH.D <ea>,FPn                       --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_000_1101
  //FATANH.B <ea>,FPn                       --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_000_1101
  //FSIN.L <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_000_1110
  //FSIN.S <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_000_1110
  //FSIN.X <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_000_1110
  //FSIN.P <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_000_1110
  //FSIN.W <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_000_1110
  //FSIN.D <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_000_1110
  //FSIN.B <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_000_1110
  //FTAN.L <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_000_1111
  //FTAN.S <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_000_1111
  //FTAN.X <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_000_1111
  //FTAN.P <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_000_1111
  //FTAN.W <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_000_1111
  //FTAN.D <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_000_1111
  //FTAN.B <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_000_1111
  //FETOX.L <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_001_0000
  //FETOX.S <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_001_0000
  //FETOX.X <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_001_0000
  //FETOX.P <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_001_0000
  //FETOX.W <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_001_0000
  //FETOX.D <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_001_0000
  //FETOX.B <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_001_0000
  //FTWOTOX.L <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_001_0001
  //FTWOTOX.S <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_001_0001
  //FTWOTOX.X <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_001_0001
  //FTWOTOX.P <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_001_0001
  //FTWOTOX.W <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_001_0001
  //FTWOTOX.D <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_001_0001
  //FTWOTOX.B <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_001_0001
  //FTENTOX.L <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_001_0010
  //FTENTOX.S <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_001_0010
  //FTENTOX.X <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_001_0010
  //FTENTOX.P <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_001_0010
  //FTENTOX.W <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_001_0010
  //FTENTOX.D <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_001_0010
  //FTENTOX.B <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_001_0010
  //FLOGN.L <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_001_0100
  //FLOGN.S <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_001_0100
  //FLOGN.X <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_001_0100
  //FLOGN.P <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_001_0100
  //FLOGN.W <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_001_0100
  //FLOGN.D <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_001_0100
  //FLOGN.B <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_001_0100
  //FLOG10.L <ea>,FPn                       --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_001_0101
  //FLOG10.S <ea>,FPn                       --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_001_0101
  //FLOG10.X <ea>,FPn                       --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_001_0101
  //FLOG10.P <ea>,FPn                       --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_001_0101
  //FLOG10.W <ea>,FPn                       --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_001_0101
  //FLOG10.D <ea>,FPn                       --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_001_0101
  //FLOG10.B <ea>,FPn                       --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_001_0101
  //FLOG2.L <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_001_0110
  //FLOG2.S <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_001_0110
  //FLOG2.X <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_001_0110
  //FLOG2.P <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_001_0110
  //FLOG2.W <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_001_0110
  //FLOG2.D <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_001_0110
  //FLOG2.B <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_001_0110
  //FABS.L <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_001_1000
  //FABS.S <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_001_1000
  //FABS.X <ea>,FPn                         --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_001_1000
  //FABS.P <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_001_1000
  //FABS.W <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_001_1000
  //FABS.D <ea>,FPn                         --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_001_1000
  //FABS.B <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_001_1000
  //FCOSH.L <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_001_1001
  //FCOSH.S <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_001_1001
  //FCOSH.X <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_001_1001
  //FCOSH.P <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_001_1001
  //FCOSH.W <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_001_1001
  //FCOSH.D <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_001_1001
  //FCOSH.B <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_001_1001
  //FNEG.L <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_001_1010
  //FNEG.S <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_001_1010
  //FNEG.X <ea>,FPn                         --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_001_1010
  //FNEG.P <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_001_1010
  //FNEG.W <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_001_1010
  //FNEG.D <ea>,FPn                         --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_001_1010
  //FNEG.B <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_001_1010
  //FACOS.L <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_001_1100
  //FACOS.S <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_001_1100
  //FACOS.X <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_001_1100
  //FACOS.P <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_001_1100
  //FACOS.W <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_001_1100
  //FACOS.D <ea>,FPn                        --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_001_1100
  //FACOS.B <ea>,FPn                        --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_001_1100
  //FCOS.L <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_001_1101
  //FCOS.S <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_001_1101
  //FCOS.X <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_001_1101
  //FCOS.P <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_001_1101
  //FCOS.W <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_001_1101
  //FCOS.D <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_001_1101
  //FCOS.B <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_001_1101
  //FGETEXP.L <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_001_1110
  //FGETEXP.S <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_001_1110
  //FGETEXP.X <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_001_1110
  //FGETEXP.P <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_001_1110
  //FGETEXP.W <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_001_1110
  //FGETEXP.D <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_001_1110
  //FGETEXP.B <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_001_1110
  //FGETMAN.L <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_001_1111
  //FGETMAN.S <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_001_1111
  //FGETMAN.X <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_001_1111
  //FGETMAN.P <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_001_1111
  //FGETMAN.W <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_001_1111
  //FGETMAN.D <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_001_1111
  //FGETMAN.B <ea>,FPn                      --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_001_1111
  //FDIV.L <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_010_0000
  //FDIV.S <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_010_0000
  //FDIV.X <ea>,FPn                         --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_010_0000
  //FDIV.P <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_010_0000
  //FDIV.W <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_010_0000
  //FDIV.D <ea>,FPn                         --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_010_0000
  //FDIV.B <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_010_0000
  //FMOD.L <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_010_0001
  //FMOD.S <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_010_0001
  //FMOD.X <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_010_0001
  //FMOD.P <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_010_0001
  //FMOD.W <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_010_0001
  //FMOD.D <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_010_0001
  //FMOD.B <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_010_0001
  //FADD.L <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_010_0010
  //FADD.S <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_010_0010
  //FADD.X <ea>,FPn                         --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_010_0010
  //FADD.P <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_010_0010
  //FADD.W <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_010_0010
  //FADD.D <ea>,FPn                         --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_010_0010
  //FADD.B <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_010_0010
  //FMUL.L <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_010_0011
  //FMUL.S <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_010_0011
  //FMUL.X <ea>,FPn                         --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_010_0011
  //FMUL.P <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_010_0011
  //FMUL.W <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_010_0011
  //FMUL.D <ea>,FPn                         --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_010_0011
  //FMUL.B <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_010_0011
  //FSGLDIV.L <ea>,FPn                      --CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_010_0100
  //FSGLDIV.S <ea>,FPn                      --CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_010_0100
  //FSGLDIV.X <ea>,FPn                      --CCS6|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_010_0100
  //FSGLDIV.P <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_010_0100
  //FSGLDIV.W <ea>,FPn                      --CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_010_0100
  //FSGLDIV.D <ea>,FPn                      --CCS6|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_010_0100
  //FSGLDIV.B <ea>,FPn                      --CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_010_0100
  //FREM.L <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_010_0101
  //FREM.S <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_010_0101
  //FREM.X <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_010_0101
  //FREM.P <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_010_0101
  //FREM.W <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_010_0101
  //FREM.D <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_010_0101
  //FREM.B <ea>,FPn                         --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_010_0101
  //FSCALE.L <ea>,FPn                       --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_010_0110
  //FSCALE.S <ea>,FPn                       --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_010_0110
  //FSCALE.X <ea>,FPn                       --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_010_0110
  //FSCALE.P <ea>,FPn                       --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_010_0110
  //FSCALE.W <ea>,FPn                       --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_010_0110
  //FSCALE.D <ea>,FPn                       --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_010_0110
  //FSCALE.B <ea>,FPn                       --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_010_0110
  //FSGLMUL.L <ea>,FPn                      --CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_010_0111
  //FSGLMUL.S <ea>,FPn                      --CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_010_0111
  //FSGLMUL.X <ea>,FPn                      --CCS6|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_010_0111
  //FSGLMUL.P <ea>,FPn                      --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_010_0111
  //FSGLMUL.W <ea>,FPn                      --CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_010_0111
  //FSGLMUL.D <ea>,FPn                      --CCS6|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_010_0111
  //FSGLMUL.B <ea>,FPn                      --CCS6|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_010_0111
  //FSUB.L <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_010_1000
  //FSUB.S <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_010_1000
  //FSUB.X <ea>,FPn                         --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_010_1000
  //FSUB.P <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_010_1000
  //FSUB.W <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_010_1000
  //FSUB.D <ea>,FPn                         --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_010_1000
  //FSUB.B <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_010_1000
  //FSINCOS.L <ea>,FPc:FPs                  --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_sss_011_0ccc
  //FSINCOS.S <ea>,FPc:FPs                  --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_sss_011_0ccc
  //FSINCOS.X <ea>,FPc:FPs                  --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_sss_011_0ccc
  //FSINCOS.P <ea>,FPc:FPs                  --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_sss_011_0ccc
  //FSINCOS.W <ea>,FPc:FPs                  --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_sss_011_0ccc
  //FSINCOS.D <ea>,FPc:FPs                  --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_sss_011_0ccc
  //FSINCOS.B <ea>,FPc:FPs                  --CCSS|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_sss_011_0ccc
  //FCMP.L <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_011_1000
  //FCMP.S <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_011_1000
  //FCMP.X <ea>,FPn                         --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_011_1000
  //FCMP.P <ea>,FPn                         --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_011_1000
  //FCMP.W <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_011_1000
  //FCMP.D <ea>,FPn                         --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_011_1000
  //FCMP.B <ea>,FPn                         --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_011_1000
  //FTST.L <ea>                             --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_000_011_1010
  //FTST.S <ea>                             --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_000_011_1010
  //FTST.X <ea>                             --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_000_011_1010
  //FTST.P <ea>                             --CCSS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_000_011_1010
  //FTST.W <ea>                             --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_000_011_1010
  //FTST.D <ea>                             --CC46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_000_011_1010
  //FTST.B <ea>                             --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_000_011_1010
  //FSMOVE.L <ea>,FPn                       ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_100_0000
  //FSMOVE.S <ea>,FPn                       ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_100_0000
  //FSMOVE.X <ea>,FPn                       ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_100_0000
  //FSMOVE.P <ea>,FPn                       ----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_100_0000
  //FSMOVE.W <ea>,FPn                       ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_100_0000
  //FSMOVE.D <ea>,FPn                       ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_100_0000
  //FSMOVE.B <ea>,FPn                       ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_100_0000
  //FSSQRT.L <ea>,FPn                       ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_100_0001
  //FSSQRT.S <ea>,FPn                       ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_100_0001
  //FSSQRT.X <ea>,FPn                       ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_100_0001
  //FSSQRT.P <ea>,FPn                       ----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_100_0001
  //FSSQRT.W <ea>,FPn                       ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_100_0001
  //FSSQRT.D <ea>,FPn                       ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_100_0001
  //FSSQRT.B <ea>,FPn                       ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_100_0001
  //FDMOVE.L <ea>,FPn                       ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_100_0100
  //FDMOVE.S <ea>,FPn                       ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_100_0100
  //FDMOVE.X <ea>,FPn                       ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_100_0100
  //FDMOVE.P <ea>,FPn                       ----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_100_0100
  //FDMOVE.W <ea>,FPn                       ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_100_0100
  //FDMOVE.D <ea>,FPn                       ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_100_0100
  //FDMOVE.B <ea>,FPn                       ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_100_0100
  //FDSQRT.L <ea>,FPn                       ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_100_0101
  //FDSQRT.S <ea>,FPn                       ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_100_0101
  //FDSQRT.X <ea>,FPn                       ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_100_0101
  //FDSQRT.P <ea>,FPn                       ----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_100_0101
  //FDSQRT.W <ea>,FPn                       ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_100_0101
  //FDSQRT.D <ea>,FPn                       ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_100_0101
  //FDSQRT.B <ea>,FPn                       ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_100_0101
  //FSABS.L <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_101_1000
  //FSABS.S <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_101_1000
  //FSABS.X <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_101_1000
  //FSABS.P <ea>,FPn                        ----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_101_1000
  //FSABS.W <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_101_1000
  //FSABS.D <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_101_1000
  //FSABS.B <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_101_1000
  //FSNEG.L <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_101_1010
  //FSNEG.S <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_101_1010
  //FSNEG.X <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_101_1010
  //FSNEG.P <ea>,FPn                        ----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_101_1010
  //FSNEG.W <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_101_1010
  //FSNEG.D <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_101_1010
  //FSNEG.B <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_101_1010
  //FDABS.L <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_101_1100
  //FDABS.S <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_101_1100
  //FDABS.X <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_101_1100
  //FDABS.P <ea>,FPn                        ----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_101_1100
  //FDABS.W <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_101_1100
  //FDABS.D <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_101_1100
  //FDABS.B <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_101_1100
  //FDNEG.L <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_101_1110
  //FDNEG.S <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_101_1110
  //FDNEG.X <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_101_1110
  //FDNEG.P <ea>,FPn                        ----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_101_1110
  //FDNEG.W <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_101_1110
  //FDNEG.D <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_101_1110
  //FDNEG.B <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_101_1110
  //FSDIV.L <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_110_0000
  //FSDIV.S <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_110_0000
  //FSDIV.X <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_110_0000
  //FSDIV.P <ea>,FPn                        ----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_110_0000
  //FSDIV.W <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_110_0000
  //FSDIV.D <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_110_0000
  //FSDIV.B <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_110_0000
  //FSADD.L <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_110_0010
  //FSADD.S <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_110_0010
  //FSADD.X <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_110_0010
  //FSADD.P <ea>,FPn                        ----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_110_0010
  //FSADD.W <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_110_0010
  //FSADD.D <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_110_0010
  //FSADD.B <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_110_0010
  //FSMUL.L <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_110_0011
  //FSMUL.S <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_110_0011
  //FSMUL.X <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_110_0011
  //FSMUL.P <ea>,FPn                        ----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_110_0011
  //FSMUL.W <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_110_0011
  //FSMUL.D <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_110_0011
  //FSMUL.B <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_110_0011
  //FDDIV.L <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_110_0100
  //FDDIV.S <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_110_0100
  //FDDIV.X <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_110_0100
  //FDDIV.P <ea>,FPn                        ----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_110_0100
  //FDDIV.W <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_110_0100
  //FDDIV.D <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_110_0100
  //FDDIV.B <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_110_0100
  //FDADD.L <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_110_0110
  //FDADD.S <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_110_0110
  //FDADD.X <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_110_0110
  //FDADD.P <ea>,FPn                        ----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_110_0110
  //FDADD.W <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_110_0110
  //FDADD.D <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_110_0110
  //FDADD.B <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_110_0110
  //FDMUL.L <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_110_0111
  //FDMUL.S <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_110_0111
  //FDMUL.X <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_110_0111
  //FDMUL.P <ea>,FPn                        ----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_110_0111
  //FDMUL.W <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_110_0111
  //FDMUL.D <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_110_0111
  //FDMUL.B <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_110_0111
  //FSSUB.L <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_110_1000
  //FSSUB.S <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_110_1000
  //FSSUB.X <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_110_1000
  //FSSUB.P <ea>,FPn                        ----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_110_1000
  //FSSUB.W <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_110_1000
  //FSSUB.D <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_110_1000
  //FSSUB.B <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_110_1000
  //FDSUB.L <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_000_nnn_110_1100
  //FDSUB.S <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_001_nnn_110_1100
  //FDSUB.X <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_010_nnn_110_1100
  //FDSUB.P <ea>,FPn                        ----SS|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_011_nnn_110_1100
  //FDSUB.W <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_100_nnn_110_1100
  //FDSUB.D <ea>,FPn                        ----46|-|-----|-----|  M+-WXZPI|1111_001_000_mmm_rrr-010_101_nnn_110_1100
  //FDSUB.B <ea>,FPn                        ----46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-010_110_nnn_110_1100
  //FMOVECR.X #ccc,FPn                      --CCSS|-|-----|-----|          |1111_001_000_000_000-010_111_nnn_0cc_cccc
  //FMOVE.L FPn,<ea>                        --CC46|-|-----|-----|D M+-WXZ  |1111_001_000_mmm_rrr-011_000_nnn_000_0000
  //FMOVE.S FPn,<ea>                        --CC46|-|-----|-----|D M+-WXZ  |1111_001_000_mmm_rrr-011_001_nnn_000_0000
  //FMOVE.X FPn,<ea>                        --CC46|-|-----|-----|  M+-WXZ  |1111_001_000_mmm_rrr-011_010_nnn_000_0000
  //FMOVE.P FPn,<ea>{#k}                    --CCSS|-|-----|-----|  M+-WXZ  |1111_001_000_mmm_rrr-011_011_nnn_kkk_kkkk
  //FMOVE.W FPn,<ea>                        --CC46|-|-----|-----|D M+-WXZ  |1111_001_000_mmm_rrr-011_100_nnn_000_0000
  //FMOVE.D FPn,<ea>                        --CC46|-|-----|-----|  M+-WXZ  |1111_001_000_mmm_rrr-011_101_nnn_000_0000
  //FMOVE.B FPn,<ea>                        --CC46|-|-----|-----|D M+-WXZ  |1111_001_000_mmm_rrr-011_110_nnn_000_0000
  //FMOVE.P FPn,<ea>{Dl}                    --CCSS|-|-----|-----|  M+-WXZ  |1111_001_000_mmm_rrr-011_111_nnn_lll_0000
  //FMOVE.L <ea>,FPIAR                      --CC46|-|-----|-----|DAM+-WXZPI|1111_001_000_mmm_rrr-100_001_000_000_0000
  //FMOVE.L <ea>,FPSR                       --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-100_010_000_000_0000
  //FMOVE.L <ea>,FPCR                       --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-100_100_000_000_0000
  //FMOVEM.L <ea>,FPIAR                     --CC46|-|-----|-----|DAM+-WXZPI|1111_001_000_mmm_rrr-100_001_000_000_0000
  //FMOVEM.L <ea>,FPSR                      --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-100_010_000_000_0000
  //FMOVEM.L <ea>,FPCR                      --CC46|-|-----|-----|D M+-WXZPI|1111_001_000_mmm_rrr-100_100_000_000_0000
  //FMOVEM.L <ea>,FPSR/FPIAR                --CC46|-|-----|-----|  M+-WXZP |1111_001_000_mmm_rrr-100_011_000_000_0000
  //FMOVEM.L #<data>,#<data>,FPSR/FPIAR     --CC4S|-|-----|-----|         I|1111_001_000_111_100-100_011_000_000_0000-{data}
  //FMOVEM.L <ea>,FPCR/FPIAR                --CC46|-|-----|-----|  M+-WXZP |1111_001_000_mmm_rrr-100_101_000_000_0000
  //FMOVEM.L #<data>,#<data>,FPCR/FPIAR     --CC4S|-|-----|-----|         I|1111_001_000_111_100-100_101_000_000_0000-{data}
  //FMOVEM.L <ea>,FPCR/FPSR                 --CC46|-|-----|-----|  M+-WXZP |1111_001_000_mmm_rrr-100_110_000_000_0000
  //FMOVEM.L #<data>,#<data>,FPCR/FPSR      --CC4S|-|-----|-----|         I|1111_001_000_111_100-100_110_000_000_0000-{data}
  //FMOVEM.L <ea>,FPCR/FPSR/FPIAR           --CC46|-|-----|-----|  M+-WXZP |1111_001_000_mmm_rrr-100_111_000_000_0000
  //FMOVEM.L #<d>,#<d>,#<d>,FPCR/FPSR/FPIAR --CC4S|-|-----|-----|         I|1111_001_000_111_100-100_111_000_000_0000-{data}
  //FMOVE.L FPIAR,<ea>                      --CC46|-|-----|-----|DAM+-WXZ  |1111_001_000_mmm_rrr-101_001_000_000_0000
  //FMOVE.L FPSR,<ea>                       --CC46|-|-----|-----|D M+-WXZ  |1111_001_000_mmm_rrr-101_010_000_000_0000
  //FMOVE.L FPCR,<ea>                       --CC46|-|-----|-----|D M+-WXZ  |1111_001_000_mmm_rrr-101_100_000_000_0000
  //FMOVEM.L FPIAR,<ea>                     --CC46|-|-----|-----|DAM+-WXZ  |1111_001_000_mmm_rrr-101_001_000_000_0000
  //FMOVEM.L FPSR,<ea>                      --CC46|-|-----|-----|D M+-WXZ  |1111_001_000_mmm_rrr-101_010_000_000_0000
  //FMOVEM.L FPCR,<ea>                      --CC46|-|-----|-----|D M+-WXZ  |1111_001_000_mmm_rrr-101_100_000_000_0000
  //FMOVEM.L FPSR/FPIAR,<ea>                --CC46|-|-----|-----|  M+-WXZ  |1111_001_000_mmm_rrr-101_011_000_000_0000
  //FMOVEM.L FPCR/FPIAR,<ea>                --CC46|-|-----|-----|  M+-WXZ  |1111_001_000_mmm_rrr-101_101_000_000_0000
  //FMOVEM.L FPCR/FPSR,<ea>                 --CC46|-|-----|-----|  M+-WXZ  |1111_001_000_mmm_rrr-101_110_000_000_0000
  //FMOVEM.L FPCR/FPSR/FPIAR,<ea>           --CC46|-|-----|-----|  M+-WXZ  |1111_001_000_mmm_rrr-101_111_000_000_0000
  //FMOVEM.X <ea>,#<data>                   --CC46|-|-----|-----|  M+ WXZP |1111_001_000_mmm_rrr-110_100_00d_ddd_dddd
  //FMOVEM.X <ea>,<list>                    --CC46|-|-----|-----|  M+ WXZP |1111_001_000_mmm_rrr-110_100_00l_lll_llll
  //FMOVEM.X <ea>,Dl                        --CC4S|-|-----|-----|  M+ WXZP |1111_001_000_mmm_rrr-110_110_000_lll_0000
  //FMOVEM.X #<data>,-(Ar)                  --CC46|-|-----|-----|    -     |1111_001_000_100_rrr-111_000_00d_ddd_dddd
  //FMOVEM.X #<data>,<ea>                   --CC46|-|-----|-----|  M  WXZ  |1111_001_000_mmm_rrr-111_100_00d_ddd_dddd
  //FMOVEM.X <list>,-(Ar)                   --CC46|-|-----|-----|    -     |1111_001_000_100_rrr-111_000_00l_lll_llll
  //FMOVEM.X <list>,<ea>                    --CC46|-|-----|-----|  M  WXZ  |1111_001_000_mmm_rrr-111_100_00l_lll_llll
  //FMOVEM.X Dl,-(Ar)                       --CC4S|-|-----|-----|    -     |1111_001_000_100_rrr-111_010_000_lll_0000
  //FMOVEM.X Dl,<ea>                        --CC4S|-|-----|-----|  M  WXZ  |1111_001_000_mmm_rrr-111_110_000_lll_0000
  public static StringBuilder disFgen (StringBuilder sb) {
    int ea = disOC & 63;
    int w = MC68060.mmuPeekWordZeroCode ((disPC += 2) - 2, disSupervisor);  //拡張ワード
    int m = w >> 10 & 7;
    int n = w >> 7 & 7;
    int c = w & 0x7f;
    switch (w >> 13) {
    case 0b000:  //Fop.X FPm,FPn
      //メモ
      //  db.xとscd.xのバグ
      //    FSINCOS.X FPm,FPc:FPs をアセンブルすると FSINCOS.X FPm,FP0:FPs が出力される
      {
        String mnemonic = DIS_FP_FGEN[c];
        if (mnemonic.length () != 0) {
          disFPn (disOp2 (sb, mnemonic, ".x"), m);
          if (c == 0b011_1010) {  //FTST
            return sb;
          }
          sb.append (',');
          if (c >> 3 == 0b011_0000 >> 3) {  //FSINCOS
            disFPn (sb, c & 7).append (':');
          }
          return disFPn (sb, n);
        }
      }
      break;
    case 0b010:  //Fop.* <ea>,FPn
      if (m == 7) {  //FMOVECR.X #ccc,FPn
        return disFPn (disHex2 (sb.append ("fmovecr.x       ").append ('#'), c).append (','), n);
      }
      //      LSXPWDBP
      if (((0b11001010 << 24 << m) < 0 ? XEiJ.EAM_DAT : XEiJ.EAM_ANY) << ea < 0L) {
        String mnemonic = DIS_FP_FGEN[c];
        if (mnemonic.length () != 0) {
          disEa (disOp2 (sb, mnemonic, DIS_FP_SZ[m]), ea, 8 + m);
          if (c == 0b011_1010) {  //FTST
            return sb;
          }
          sb.append (',');
          if (c >> 3 == 0b011_0000 >> 3) {  //FSINCOS
            disFPn (sb, c & 7).append (':');
          }
          return disFPn (sb, n);
        }
      }
      break;
    case 0b011:  //FMOVE.* FPn,<ea>
      //      LSXPWDBP
      if (((0b11001010 << 24 << m) < 0 ? XEiJ.EAM_DLT : XEiJ.EAM_MLT) << ea < 0L) {
        disEa (disFPn (disOp2 (sb, "fmove", DIS_FP_SZ[m]), n).append (','), ea, 8 + m);
        if (m == 3) {  //static k-factor
          disHex2 (sb.append ('{'), c).append ('}');
        } else if (m == 7) {  //dynamic k-factor
          disDR (sb.append ('{'), c >>> 4).append ('}');
        }
        return sb;
      }
      break;
    case 0b100:  //FMOVEM.L <ea>,FPCR/FPSR/FPIAR
      {
        //メモ
        //  db.xとscd.xのバグ
        //    FMOVE.L Dn,FPCR をアセンブルできない
        //    FMOVE.L Dn,FPSR をアセンブルできない
        //    FMOVE.L Dn,FPIAR をアセンブルできない
        //    FMOVE.L An,FPIAR をアセンブルできない
        //    FMOVEM.L Dn,FPCR をアセンブルできない
        //    FMOVEM.L Dn,FPSR をアセンブルできない
        //    FMOVEM.L Dn,FPIAR をアセンブルできない
        //    FMOVEM.L An,FPIAR をアセンブルできない
        int k = 0b11_10_10_01_10_01_01_00 >> (m << 1) & 3;  //転送するレジスタの数
        if ((m == 1 ? XEiJ.EAM_ALL : k == 1 ? XEiJ.EAM_DAT : XEiJ.EAM_ANY) << ea < 0L) {
          sb.append (k == 1 ? "fmove.l " : "fmovem.l        ");
          if (ea < XEiJ.EA_IM) {
            disEaz (sb, ea).append (',');
          } else {
            for (int i = 0; i < k; i++) {
              disIMl (sb).append (',');
            }
          }
          m <<= 29;
          for (int i = 0; m != 0; i++, m <<= 1) {
            if (m < 0) {
              sb.append (DIS_FP_CR[i]);
              if (m << 1 != 0) {
                sb.append ('/');
              }
            }
          }
          return sb;
        }
      }
      break;
    case 0b101:  //FMOVEM.L FPCR/FPSR/FPIAR,<ea>
      {
        //メモ
        //  db.xとscd.xのバグ
        //    FMOVE.L FPCR,Dn をアセンブルできない
        //    FMOVE.L FPSR,Dn をアセンブルできない
        //    FMOVE.L FPIAR,Dn をアセンブルできない
        //    FMOVE.L FPIAR,An をアセンブルできない
        //    FMOVEM.L FPCR,Dn をアセンブルできない
        //    FMOVEM.L FPSR,Dn をアセンブルできない
        //    FMOVEM.L FPIAR,Dn をアセンブルできない
        //    FMOVEM.L FPIAR,An をアセンブルできない
        int k = 0b11_10_10_01_10_01_01_00 >> (m << 1) & 3;  //転送するレジスタの数
        if ((m == 1 ? XEiJ.EAM_ALT : k == 1 ? XEiJ.EAM_DLT : XEiJ.EAM_MLT) << ea < 0L) {
          sb.append (k == 1 ? "fmove.l " : "fmovem.l        ");
          m <<= 29;
          for (int i = 0; m != 0; i++, m <<= 1) {
            if (m < 0) {
              sb.append (DIS_FP_CR[i]);
              if (m << 1 != 0) {
                sb.append ('/');
              }
            }
          }
          return disEaz (sb.append (','), ea);
        }
      }
      break;
    case 0b110:  //FMOVEM.X <ea>,<list>
      if (XEiJ.EAM_RDL << ea < 0L) {
        disEaz (sb.append ("fmovem.x        "), ea).append (',');
        if ((m & 2) != 0) {  //dynamic list
          disDR (sb, w >> 4 & 7);
        } else {  //static list
          disFPLst (sb, w);
        }
        return sb;
      }
      break;
    case 0b111:  //FMOVEM.X <list>,<ea>
      {
        if (XEiJ.EAM_WTL << ea < 0L) {
          sb.append ("fmovem.x        ");
          if ((m & 2) != 0) {  //dynamic list
            disDR (sb, w >> 4 & 7);
          } else if ((m & 4) != 0) {  //static list, normal
            disFPLst (sb, w);
          } else {  //static list, reverse
            disFPLmn (sb, w);
          }
          return disEaz (sb.append (','), ea);
        }
      }
      break;
    }  //switch w>>13
    return disIllegal (sb);
  }  //disFgen(StringBuilder)

  //FDBcc Dr,<label>                        --CC4S|-|-----|-----|          |1111_001_001_001_rrr-000_000_000_0cc_cccc-{offset}
  //FTRAPcc.W #<data>                       --CC4S|-|-----|-----|          |1111_001_001_111_010-000_000_000_0cc_cccc-{data}
  //FTRAPcc.L #<data>                       --CC4S|-|-----|-----|          |1111_001_001_111_011-000_000_000_0cc_cccc-{data}
  //FTRAPcc                                 --CC4S|-|-----|-----|          |1111_001_001_111_100-000_000_000_0cc_cccc
  //FScc.B <ea>                             --CC4S|-|-----|-----|D M+-WXZ  |1111_001_001_mmm_rrr-000_000_000_0cc_cccc
  public static StringBuilder disFscc (StringBuilder sb) {
    //メモ
    //  db.xとscd.xのバグ
    //    FDBcc Dr,<label> をアセンブルすると FDBcc Dr,<label>+2 が出力される (scd060.xで修正されている)
    //    FDBcc Dr,<label> を逆アセンブルすると FDBcc Dr,<label>-2 が表示される (scd060.xで修正されている)
    int c = MC68060.mmuPeekWordZeroCode ((disPC += 2) - 2, disSupervisor) & 63;
    if (c < DIS_FP_CC.length) {
      int ea = disOC & 63;
      if (ea >> 3 == XEiJ.MMM_AR) {
        disStatus = DIS_SOMETIMES_BRANCH;
        return disOW (disDR (disOp2 (sb, "fdb", DIS_FP_CC[c]), disOC & 7).append (','));  //FDBccはUnsized、PDBccはWordらしい
      }
      if (ea == XEiJ.EA_PW) {
        return disIMw (disOp3 (sb, "ftrap", DIS_FP_CC[c], ".w"));
      }
      if (ea == XEiJ.EA_PX) {
        return disIMl (disOp3 (sb, "ftrap", DIS_FP_CC[c], ".l"));
      }
      if (ea == XEiJ.EA_IM) {
        return sb.append ("ftrap").append (DIS_FP_CC[c]);
      }
      if (XEiJ.EAM_DAT << ea < 0L) {
        return disEab (disOp3 (sb, "fs", DIS_FP_CC[c], ".b"), ea);
      }
    }
    return disIllegal (sb);
  }  //disFscc(StringBuilder)

  //FNOP                                    --CC46|-|-----|-----|          |1111_001_010_000_000-000_000_000_000_0000
  //FBcc.W <label>                          --CC46|-|-----|-----|          |1111_001_010_ccc_ccc-{offset}
  public static StringBuilder disFbccWord (StringBuilder sb) {
    int c = disOC & 63;
    if (c < DIS_FP_CC.length) {
      if (c == 0b000000) {  //FBF.W <label>
        int base = disPC;
        int offset = MC68060.mmuPeekWordSignCode (base, disSupervisor);
        disPC = base + 2;
        if (offset == 0) {  //FBF.W (*)+2 → FNOP
          return sb.append ("fnop");
        }
        return disProgramHex8 (sb.append ("fbf.w   "),
                               base + offset);
      }
      if (c == 0b001111) {  //FBT.W <labsl> → FBRA.W <label>
        disStatus = DIS_ALWAYS_BRANCH;
        return disOW (sb.append ("fbra.w  "));
      }
      //FBcc.W <label>
      disStatus = DIS_SOMETIMES_BRANCH;
      return disOW (disOp3 (sb, "fb", DIS_FP_CC[c], ".w"));
    }
    return disIllegal (sb);
  }  //disFbccWord(StringBuilder)

  //FBcc.L <label>                          --CC46|-|-----|-----|          |1111_001_011_ccc_ccc-{offset}
  public static StringBuilder disFbccLong (StringBuilder sb) {
    int c = disOC & 63;
    if (c < DIS_FP_CC.length) {
      if (c == 0b001111) {  //FBT.L <labsl> → FBRA.L <label>
        disStatus = DIS_ALWAYS_BRANCH;
        return disOW (sb.append ("fbra.l  "));
      }
      //FBcc.L <label>
      disStatus = DIS_SOMETIMES_BRANCH;
      return disOW (disOp3 (sb, "fb", DIS_FP_CC[c], ".l"));
    }
    return disIllegal (sb);
  }  //disFbccLong(StringBuilder)

  //FSAVE <ea>                              --CC46|P|-----|-----|  M -WXZ  |1111_001_100_mmm_rrr
  public static StringBuilder disFsave (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_WTL << ea < 0L) {
      return disEaz (sb.append ("fsave   "), ea);
    }
    return disIllegal (sb);
  }  //disFsave(StringBuilder)

  //FRESTORE <ea>                           --CC46|P|-----|-----|  M+ WXZP |1111_001_101_mmm_rrr
  public static StringBuilder disFrestore (StringBuilder sb) {
    int ea = disOC & 63;
    if (XEiJ.EAM_RDL << ea < 0L) {
      return disEaz (sb.append ("frestore        "), ea);
    }
    return disIllegal (sb);
  }  //disFrestore(StringBuilder)

  //不当命令
  public static StringBuilder disIllegal (StringBuilder sb) {
    sb.append ("illegal ");
    //2ワード目で不当命令と判別されたときは2ワードの不当命令とみなす
    for (int a = disPC0; a < disPC; a += 2) {
      if (disPC0 < a) {
        sb.append (',');
      }
      disHex4 (sb, MC68060.mmuPeekWordZeroCode (a, disSupervisor));
    }
    return sb;
  }  //disillegal(StringBuilder)

  //実効アドレス
  public static StringBuilder disEaz (StringBuilder sb, int ea) {
    return disEa (sb, ea, -1);
  }  //disEaz(StringBuilder,int)
  public static StringBuilder disEab (StringBuilder sb, int ea) {
    return disEa (sb, ea, XEiJ.MPU_SS_BYTE);
  }  //disEab(StringBuilder,int)
  public static StringBuilder disEaw (StringBuilder sb, int ea) {
    return disEa (sb, ea, XEiJ.MPU_SS_WORD);
  }  //disEaw(StringBuilder,int)
  public static StringBuilder disEal (StringBuilder sb, int ea) {
    return disEa (sb, ea, XEiJ.MPU_SS_LONG);
  }  //disEal(StringBuilder,int)
  public static StringBuilder disEa (StringBuilder sb, int ea, int ss) {
    int rrr = ea & 7;
    switch (ea) {
    case XEiJ.EA_DR + 0:  //d0
    case XEiJ.EA_DR + 1:  //d1
    case XEiJ.EA_DR + 2:  //d2
    case XEiJ.EA_DR + 3:  //d3
    case XEiJ.EA_DR + 4:  //d4
    case XEiJ.EA_DR + 5:  //d5
    case XEiJ.EA_DR + 6:  //d6
    case XEiJ.EA_DR + 7:  //d7
      return disDR (sb, rrr);
    case XEiJ.EA_AR + 0:  //a0
    case XEiJ.EA_AR + 1:  //a1
    case XEiJ.EA_AR + 2:  //a2
    case XEiJ.EA_AR + 3:  //a3
    case XEiJ.EA_AR + 4:  //a4
    case XEiJ.EA_AR + 5:  //a5
    case XEiJ.EA_AR + 6:  //a6
    case XEiJ.EA_AR + 7:  //sp
      return disAR (sb, rrr);
    case XEiJ.EA_MM + 0:  //(a0)
    case XEiJ.EA_MM + 1:  //(a1)
    case XEiJ.EA_MM + 2:  //(a2)
    case XEiJ.EA_MM + 3:  //(a3)
    case XEiJ.EA_MM + 4:  //(a4)
    case XEiJ.EA_MM + 5:  //(a5)
    case XEiJ.EA_MM + 6:  //(a6)
    case XEiJ.EA_MM + 7:  //(sp)
      return disMM (sb, rrr);
    case XEiJ.EA_MP + 0:  //(a0)+
    case XEiJ.EA_MP + 1:  //(a1)+
    case XEiJ.EA_MP + 2:  //(a2)+
    case XEiJ.EA_MP + 3:  //(a3)+
    case XEiJ.EA_MP + 4:  //(a4)+
    case XEiJ.EA_MP + 5:  //(a5)+
    case XEiJ.EA_MP + 6:  //(a6)+
    case XEiJ.EA_MP + 7:  //(sp)+
      return disMP (sb, rrr);
    case XEiJ.EA_MN + 0:  //-(a0)
    case XEiJ.EA_MN + 1:  //-(a1)
    case XEiJ.EA_MN + 2:  //-(a2)
    case XEiJ.EA_MN + 3:  //-(a3)
    case XEiJ.EA_MN + 4:  //-(a4)
    case XEiJ.EA_MN + 5:  //-(a5)
    case XEiJ.EA_MN + 6:  //-(a6)
    case XEiJ.EA_MN + 7:  //-(sp)
      return disMN (sb, rrr);
    case XEiJ.EA_MW + 0:  //d16(a0)
    case XEiJ.EA_MW + 1:  //d16(a1)
    case XEiJ.EA_MW + 2:  //d16(a2)
    case XEiJ.EA_MW + 3:  //d16(a3)
    case XEiJ.EA_MW + 4:  //d16(a4)
    case XEiJ.EA_MW + 5:  //d16(a5)
    case XEiJ.EA_MW + 6:  //d16(a6)
    case XEiJ.EA_MW + 7:  //d16(sp)
      return disMW (sb, rrr);
    case XEiJ.EA_MX + 0:  //d8(a0,rn.wl)
    case XEiJ.EA_MX + 1:  //d8(a1,rn.wl)
    case XEiJ.EA_MX + 2:  //d8(a2,rn.wl)
    case XEiJ.EA_MX + 3:  //d8(a3,rn.wl)
    case XEiJ.EA_MX + 4:  //d8(a4,rn.wl)
    case XEiJ.EA_MX + 5:  //d8(a5,rn.wl)
    case XEiJ.EA_MX + 6:  //d8(a6,rn.wl)
    case XEiJ.EA_MX + 7:  //d8(sp,rn.wl)
      return disMX (sb, rrr);
    case XEiJ.EA_ZW:  //(xxx).w
      return disZW (sb);
    case XEiJ.EA_ZL:  //(xxx).l
      return disZL (sb);
    case XEiJ.EA_PW:  //d16(pc)
      return disPW (sb);
    case XEiJ.EA_PX:  //d8(pc,rn.wl)
      return disPX (sb);
    case XEiJ.EA_IM:  //#<data>
      return (ss == XEiJ.MPU_SS_BYTE ? disIMb (sb) :
              ss == XEiJ.MPU_SS_WORD ? disIMw (sb) :
              ss == XEiJ.MPU_SS_LONG ? disIMl (sb) :
              ss == 8 + 0 ? disIMl (sb) :
              ss == 8 + 1 ? disIMs (sb) :
              ss == 8 + 2 ? disIMx (sb) :
              ss == 8 + 3 ? disIMp (sb) :
              ss == 8 + 4 ? disIMw (sb) :
              ss == 8 + 5 ? disIMd (sb) :
              ss == 8 + 6 ? disIMb (sb) :
              ss == 8 + 7 ? disIMp (sb) :
              sb.append ("???"));
    }  //switch
    return sb.append ("???");
  }  //disEa(StringBuilder,int,int)

  public static StringBuilder disDR (StringBuilder sb, int rrr) {
    return sb.append ('d').append ((char) ('0' + rrr));
  }  //disDR(StringBuilder,int)
  public static StringBuilder disAR (StringBuilder sb, int rrr) {
    return rrr < 7 ? sb.append ('a').append ((char) ('0' + rrr)) : sb.append ("sp");
  }  //disAR(StringBuilder,int)
  public static StringBuilder disRn (StringBuilder sb, int rnnn) {
    return rnnn < 15 ? sb.append (rnnn < 8 ? 'd' : 'a').append ((char) ('0' + (rnnn & 7))) : sb.append ("sp");
  }  //disRn(StringBuilder,int)
  public static StringBuilder disMM (StringBuilder sb, int rrr) {
    return disAR (sb.append ('('), rrr).append (')');
  }  //disMM(StringBuilder,int)
  public static StringBuilder disMP (StringBuilder sb, int rrr) {
    return disAR (sb.append ('('), rrr).append (")+");
  }  //disMP(StringBuilder,int)
  public static StringBuilder disMN (StringBuilder sb, int rrr) {
    return disAR (sb.append ("-("), rrr).append (')');
  }  //disMN(StringBuilder,int)
  public static StringBuilder disMW (StringBuilder sb, int rrr) {
    return disMM (disHex4 (sb, MC68060.mmuPeekWordSignCode ((disPC += 2) - 2, disSupervisor)), rrr);
  }  //disMW(StringBuilder,int)
  public static StringBuilder disMX (StringBuilder sb, int rrr) {
    int w = MC68060.mmuPeekWordZeroCode ((disPC += 2) - 2, disSupervisor);  //拡張ワード
    if ((w & 0x0100) == 0) {  //ブリーフフォーマット
      disHex2 (sb, w);  //ディスプレースメント
      sb.append ('(');
      disAR (sb, rrr);  //ベースレジスタ
      sb.append (',');
      disRn (sb, w >> 12).append ((w & 0x0800) == 0 ? ".w" : ".l");  //インデックス
      if ((w & 0x0600) != 0) {  //スケールファクタあり
        sb.append ('*').append ((char) ('0' + (1 << (w >> 9 & 3))));  //スケールファクタ
      }
      sb.append (')');
    } else {  //フルフォーマット
      sb.append ('(');
      if ((w & 0x0003) != 0) {  //メモリ間接あり
        sb.append ('[');
      }
      if ((w & 0x0020) != 0) {  //ベースディスプレースメントあり
        if ((w & 0x0010) == 0) {  //ワードベースディスプレースメント
          int bd = MC68060.mmuPeekWordZeroCode ((disPC += 2) - 2,
                                                disSupervisor);
          disHex4 (sb,
                   bd).append (".w");
        } else {  //ロングベースディスプレースメント
          int bd = MC68060.mmuPeekLongCode ((disPC += 4) - 4,
                                            disSupervisor);
          disProgramHex8 (sb,
                          bd).append (".l");
        }
        sb.append (',');
      }
      if ((w & 0x0080) != 0) {  //ベースサプレスあり
        sb.append ('z');
      }
      disAR (sb, rrr);  //ベースレジスタ
      if ((w & 0x0004) == 0) {  //プリインデックス
        if ((w & 0x0040) == 0) {  //インデックスサプレスなし
          sb.append (',');
          disRn (sb, w >> 12).append ((w & 0x0800) == 0 ? ".w" : ".l");
          if ((w & 0x0600) != 0) {  //スケールファクタあり
            sb.append ('*').append ((char) ('0' + (1 << (w >> 9 & 3))));  //スケールファクタ
          }
        }
      }
      if ((w & 0x0003) != 0) {  //メモリ間接あり
        sb.append (']');
      }
      if ((w & 0x0004) != 0) {  //ポストインデックス
        if ((w & 0x0040) == 0) {  //インデックスサプレスなし
          sb.append (',');
          disRn (sb, w >> 12).append ((w & 0x0800) == 0 ? ".w" : ".l");
          if ((w & 0x0600) != 0) {  //スケールファクタあり
            sb.append ('*').append ((char) ('0' + (1 << (w >> 9 & 3))));  //スケールファクタ
          }
        }
      }
      if ((w & 0x0002) != 0) {  //アウタディスプレースメントあり
        sb.append (',');
        if ((w & 0x0001) == 0) {  //ワードアウタディスプレースメント
          int od = MC68060.mmuPeekWordZeroCode ((disPC += 2) - 2,
                                                disSupervisor);
          disHex4 (sb,
                   od).append (".w");
        } else {  //ロングアウタディスプレースメント
          int od = MC68060.mmuPeekLongCode ((disPC += 4) - 4,
                                            disSupervisor);
          disHex8 (sb,
                   od).append (".l");
        }
      }
      sb.append (')');
    }
    return sb;
  }  //disMX(StringBuilder,int)
  public static StringBuilder disZW (StringBuilder sb) {
    int z = MC68060.mmuPeekWordSignCode ((disPC += 2) - 2,
                                         disSupervisor);
    return disHex4 (sb,
                    z).append (".w");
  }  //disZW(StringBuilder)
  public static StringBuilder disZL (StringBuilder sb) {
    int z = MC68060.mmuPeekLongCode ((disPC += 4) - 4,
                                     disSupervisor);
    return disProgramHex8 (sb,
                           z).append (".l");
  }  //disZL(StringBuilder)
  public static StringBuilder disOW (StringBuilder sb) {
    int b = disPC;  //ベース
    int d = MC68060.mmuPeekWordSignCode ((disPC += 2) - 2,
                                         disSupervisor);
    return disProgramHex8 (sb,
                           b + d);
  }  //disOW(StringBuilder)
  public static StringBuilder disOL (StringBuilder sb) {
    int b = disPC;  //ベース
    int d = MC68060.mmuPeekLongCode ((disPC += 4) - 4,
                                     disSupervisor);
    return disProgramHex8 (sb,
                           b + d);
  }  //disOL(StringBuilder)
  public static StringBuilder disPW (StringBuilder sb) {
    return disOW (sb).append ("(pc)");
  }  //disPW(StringBuilder)
  public static StringBuilder disPX (StringBuilder sb) {
    int b = disPC;  //ベース
    int w = MC68060.mmuPeekWordZeroCode ((disPC += 2) - 2,
                                         disSupervisor);  //拡張ワード
    if ((w & 0x0100) == 0) {  //ブリーフフォーマット
      int d = (byte) w;  //ディスプレースメント
      disProgramHex8 (sb,
                      b + d);
      sb.append ('(');
      sb.append ("pc");  //ベースレジスタ
      sb.append (',');
      disRn (sb, w >> 12).append ((w & 0x0800) == 0 ? ".w" : ".l");  //インデックス
      if ((w & 0x0600) != 0) {  //スケールファクタあり
        sb.append ('*').append ((char) ('0' + (1 << (w >> 9 & 3))));  //スケールファクタ
      }
      sb.append (')');
    } else {  //フルフォーマット
      sb.append ('(');
      if ((w & 0x0003) != 0) {  //メモリ間接あり
        sb.append ('[');
      }
      if ((w & 0x0020) != 0) {  //ベースディスプレースメントあり
        if ((w & 0x0080) != 0) {  //ベースサプレスあり
          b = 0;
        }
        if ((w & 0x0010) == 0) {  //ワードベースディスプレースメント
          int bd = MC68060.mmuPeekWordZeroCode ((disPC += 2) - 2,
                                                disSupervisor);
          disProgramHex8 (sb,
                          b + bd).append (".w");
        } else {  //ロングベースディスプレースメント
          int bd = MC68060.mmuPeekLongCode ((disPC += 4) - 4,
                                            disSupervisor);
          disProgramHex8 (sb,
                          b + bd).append (".l");
        }
        sb.append (',');
      }
      if ((w & 0x0080) != 0) {  //ベースサプレスあり
        sb.append ('z');
      }
      sb.append ("pc");  //ベースレジスタ
      if ((w & 0x0004) == 0) {  //プリインデックス
        if ((w & 0x0040) == 0) {  //インデックスサプレスなし
          sb.append (',');
          disRn (sb, w >> 12).append ((w & 0x0800) == 0 ? ".w" : ".l");
          if ((w & 0x0600) != 0) {  //スケールファクタあり
            sb.append ('*').append ((char) ('0' + (1 << (w >> 9 & 3))));  //スケールファクタ
          }
        }
      }
      if ((w & 0x0003) != 0) {  //メモリ間接あり
        sb.append (']');
      }
      if ((w & 0x0004) != 0) {  //ポストインデックス
        if ((w & 0x0040) == 0) {  //インデックスサプレスなし
          sb.append (',');
          disRn (sb, w >> 12).append ((w & 0x0800) == 0 ? ".w" : ".l");
          if ((w & 0x0600) != 0) {  //スケールファクタあり
            sb.append ('*').append ((char) ('0' + (1 << (w >> 9 & 3))));  //スケールファクタ
          }
        }
      }
      if ((w & 0x0002) != 0) {  //アウタディスプレースメントあり
        sb.append (',');
        if ((w & 0x0001) == 0) {  //ワードアウタディスプレースメント
          int od = MC68060.mmuPeekWordZeroCode ((disPC += 2) - 2,
                                                disSupervisor);
          disHex4 (sb,
                   od).append (".w");
        } else {  //ロングアウタディスプレースメント
          int od = MC68060.mmuPeekLongCode ((disPC += 4) - 4,
                                            disSupervisor);
          disHex8 (sb,
                   od).append (".l");
        }
      }
      sb.append (')');
    }
    return sb;
  }  //disPX(StringBuilder)
  public static StringBuilder disIMb (StringBuilder sb) {
    //未定義の上位バイトが$FFのとき#-$xxと書かないとアセンブルしたとき同じコードにならない
    int d = MC68060.mmuPeekWordSignCode ((disPC += 2) - 2, disSupervisor);
    if (d < 0) {
      return disHex2 (sb.append ("#-"), -d);
    } else {
      return disHex2 (sb.append ('#'), d);
    }
  }  //disIMb(StringBuilder)
  public static StringBuilder disIMw (StringBuilder sb) {
    return disHex4 (sb.append ('#'), MC68060.mmuPeekWordSignCode ((disPC += 2) - 2, disSupervisor));
  }  //disIMw(StringBuilder)
  public static StringBuilder disIMl (StringBuilder sb) {
    return disHex8 (sb.append ('#'), MC68060.mmuPeekLongCode ((disPC += 4) - 4, disSupervisor));
  }  //disIMl(StringBuilder)

  public static StringBuilder disIMs (StringBuilder sb) {
    int i = MC68060.mmuPeekLongCode ((disPC += 4) - 4, disSupervisor);
    return sb.append ("#").append (XEiJ.fpuBox.new EFP ().setf0 (i).toString ());
  }  //disIMs(StringBuilder)
  public static StringBuilder disIMd (StringBuilder sb) {
    long l = MC68060.mmuPeekQuadCode ((disPC += 8) - 8, disSupervisor);
    return sb.append ("#").append (XEiJ.fpuBox.new EFP ().setd01 (l).toString ());
  }  //disIMd(StringBuilder)
  public static StringBuilder disIMx (StringBuilder sb) {
    int i = MC68060.mmuPeekLongCode ((disPC += 4) - 4, disSupervisor);
    long l = MC68060.mmuPeekQuadCode ((disPC += 8) - 8, disSupervisor);
    return sb.append ("#").append (XEiJ.fpuBox.new EFP ().setx012 (i, l).toString ());
  }  //disIMx(StringBuilder)
  public static StringBuilder disIMp (StringBuilder sb) {
    int i = MC68060.mmuPeekLongCode ((disPC += 4) - 4, disSupervisor);
    long l = MC68060.mmuPeekQuadCode ((disPC += 8) - 8, disSupervisor);
    return sb.append ("#").append (XEiJ.fpuBox.new EFP ().setp012 (i, l).toString ());
  }  //disIMp(StringBuilder)

  public static StringBuilder disIM3 (StringBuilder sb, int qqq) {
    return disHex2 (sb.append ('#'), ((qqq & 7) - 1 & 7) + 1);
  }  //disIM3(StringBuilder,int)
  public static StringBuilder disIM8 (StringBuilder sb, int data) {
    return disHex2 (sb.append ('#'), data);
  }  //disIM8(StringBuilder,int)

  //レジスタリスト
  public static StringBuilder disLst (StringBuilder sb, boolean reverse) {
    int invert = reverse ? 0b1111 : 0b0000;
    int x = MC68060.mmuPeekWordSignCode ((disPC += 2) - 2, disSupervisor);
    int from = -1;  //開始レジスタ番号
    int to = -1;  //終了レジスタ番号
    for (int i = 0; i <= 15; i++) {
      if ((x & (1 << (i ^ invert))) != 0) {  //転送する
        if (from < 0) {  //ここから
          from = i;
        }
        if ((i & 7) == 7 || (x & (1 << (i + 1 ^ invert))) == 0) {  //ここまで
          if (to >= 0) {
            sb.append ('/');
          }
          sb.append (from < 8 ? 'd' : 'a').append ((char) ('0' + (from & 7)));
          to = i;
          if (from < to) {
            sb.append (to < 8 ? "-d" : "-a").append ((char) ('0' + (to & 7)));
          }
          from = -1;
        }
      }
    }
    if (to < 0) {  //転送するレジスタがなかった
      sb.append ("???");
    }
    return sb;
  }  //disLst(StringBuilder,int)

  public static StringBuilder disFPLst (StringBuilder sb, int l) {
    l <<= 24;
    if (false) {
      for (int i = 0; l != 0; i++, l <<= 1) {
        if (l < 0) {
          disFPn (sb, i);
          if (l << 1 != 0) {
            sb.append ('/');
          }
        }
      }
    } else {
      int s = -1;
      for (int i = 0; l != 0; i++, l <<= 1) {
        if (l < 0) {
          if (s < 0) {  //開始
            s = i;
          }
          if ((l ^ l << 1) < 0) {  //終了
            disFPn (sb, s);
            if (s < i) {
              disFPn (sb.append ('-'), i);
            }
            if (l << 1 != 0) {
              sb.append ('/');
            }
            s = -1;
          }
        }
      }
    }
    return sb;
  }  //disFPLst(StringBuilder,int)

  public static StringBuilder disFPLmn (StringBuilder sb, int l) {
    l &= 255;
    if (false) {
      for (int i = 0; l != 0; i++, l >>>= 1) {
        if ((l & 1) != 0) {
          disFPn (sb, i);
          if (l >>> 1 != 0) {
            sb.append ('/');
          }
        }
      }
    } else {
      int s = -1;
      for (int i = 0; l != 0; i++, l >>>= 1) {
        if ((l & 1) != 0) {
          if (s < 0) {  //開始
            s = i;
          }
          if (((l ^ l >>> 1) & 1) != 0) {  //終了
            disFPn (sb, s);
            if (s < i) {
              disFPn (sb.append ('-'), i);
            }
            if (l >>> 1 != 0) {
              sb.append ('/');
            }
            s = -1;
          }
        }
      }
    }
    return sb;
  }  //disFPLmn(StringBuilder,int)

  //ビットフィールド
  public static StringBuilder disBF (StringBuilder sb, int w) {
    sb.append ('{');
    if ((w & 0b0000_100_000_000_000) == 0) {
      disHex2 (sb, w >> 6 & 31);
    } else {
      disDR (sb, w >> 6 & 7);
    }
    sb.append (':');
    if ((w & 0b0000_000_000_100_000) == 0) {
      disHex2 (sb, (w - 1 & 31) + 1);
    } else {
      disDR (sb, w & 7);
    }
    return sb.append ('}');
  }  //disBF(StringBuilder,int)

  //  ニモニックと空白
  public static StringBuilder disOp2 (StringBuilder sb, String mn, String sz) {
    return sb.append (mn).append (sz).append ("        ", Math.min (7, mn.length () + sz.length ()), 8);
  }  //disOp2(StringBuilder,String,String)
  public static StringBuilder disOp3 (StringBuilder sb, String mn, String cc, String sz) {
    return sb.append (mn).append (cc).append (sz).append ("        ", Math.min (7, mn.length () + cc.length () + sz.length ()), 8);
  }  //disOp3(StringBuilder,String,String,String)

  //  '$'と2桁の16進数
  public static StringBuilder disHex2 (StringBuilder sb, int d) {
    return XEiJ.fmtHex2 (sb.append ('$'), d);
  }  //disHex2(StringBuilder,int)

  //  '$'と4桁の16進数
  public static StringBuilder disHex4 (StringBuilder sb, int d) {
    return XEiJ.fmtHex4 (sb.append ('$'), d);
  }  //disHex4(StringBuilder,int)

  //  '$'と8桁の16進数
  public static StringBuilder disHex8 (StringBuilder sb, int d) {
    return XEiJ.fmtHex8 (sb.append ('$'), d);
  }  //disHex8(StringBuilder,int)

  //  浮動小数点レジスタ
  public static StringBuilder disFPn (StringBuilder sb, int n) {
    return sb.append ("fp").append (n);
  }  //disFPn(StringBuilder,int)

}  //class Disassembler



