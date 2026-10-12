//========================================================================================
//  Settings.java
//    en:Settings
//    ja:設定
//  Copyright (C) 2003-2026 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

//----------------------------------------------------------------------------------------
//
//  設定ファイル
//    XEiJの動作環境を指示する「設定」が1つ以上記述されているファイル
//    場所
//      Windowsのとき(os.nameがWindowsを含むとき)
//        %APPDATA%\XEiJ\XEiJ.ini    C:\Users\%USERNAME%\AppData\Roaming\XEiJ\XEiJ.iniなど。%～%は環境変数
//      その他
//        ~/XEiJ.ini                 ~はuser.home
//        ./XEiJ.ini                 .はuser.dir
//    内容
//      [タイトルA]
//      キーワードA1=値A1
//      キーワードA2=値A2
//          :
//      [タイトルB]
//      キーワードB1=値B1
//      キーワードB2=値B2
//          :
//
//  設定の種類
//    設定にはデフォルト設定とユーザ設定がある
//    デフォルト設定(default)
//      タイトルがdefaultの設定
//      プログラムに埋め込まれている定数にhomeとlangを加えたもの
//      デフォルト設定にないキーワードは指定できない
//      デフォルト設定と同じ値のパラメータは保存されない
//    ユーザ設定
//      タイトルがdefault以外の設定
//      saveonexit=onのとき設定ファイルに保存される
//      無題設定と仮設定とその他のユーザ設定がある
//      無題設定(notitle)
//        [notitle]のユーザ設定
//        起動時にユーザ設定が1つもないときデフォルト設定が無題設定にコピーされて現在の設定になる
//        自動で作られることを除けば普通のユーザ設定
//        設定ファイルの先頭に配置される
//        設定ファイルの最初の設定が無題設定のとき[notitle]を省略できる
//      仮設定(temporary)
//        [temporary]のユーザ設定
//        -config=defaultを指定するとデフォルト設定が仮設定にコピーされて現在の設定になる
//        -config=defaultを指定するとデフォルト設定に巻き戻されるので常用は避けた方が良い
//        設定ファイルの無題設定の後に配置される
//      その他のユーザ設定
//        設定ファイルの無題設定と仮設定の後に配置される
//        その他のユーザ設定の順序はユーザが自由に並べ替えることができる
//
//  パラメータ
//    設定ファイル
//      キーワード=値
//      1行に1組ずつ書く
//      キーワードと値の前後に余分な空白があってはならない
//    コマンドライン
//      -キーワード=値
//      1引数に1組ずつ書く
//      キーワードと値の前後に余分な空白があってはならない
//    キーワード
//      英小文字で始まり英小文字と数字で構成される文字列
//    値
//      空白以外で始まり空白以外で終わる文字列
//      空列または空白を含む文字列は"～"で囲む
//      デフォルト設定で値がonまたはoffのパラメータはonまたはoffだけ指定できる
//      デフォルト設定で値がyesまたはnoのパラメータはyesまたはnoだけ指定できる
//      デフォルト設定で値が符号と数字だけのパラメータは符号と数字だけ指定できる
//      キーワードがdataで終わるパラメータ
//        値がgzip+base64で圧縮されている
//        値が非常に大きい場合があり、原則として文字列の状態では編集できない
//        バイナリデータとして編集する機能を付けてもよい
//
//  デフォルト設定で環境に合わせて作られるパラメータ
//    home=path
//      ホームディレクトリ。末尾の区切り文字は含まない
//      ファイルチューザーが最初に開くディレクトリ
//    lang=en
//    lang=ja
//      表示言語
//
//  設定ファイルの特別なパラメータ
//    [タイトル]
//      新しい設定の開始。設定のタイトル
//      []、[default]は不可
//      -config=タイトルで設定を選択するときに使う
//      タイトルが同じ設定が複数存在してはならない
//      改行を含むことはできない
//      [、]を除く文字は何でも書けるが、"、'、`、$などはコマンドラインで選択するときに書きにくいので避けた方が良い
//      設定ファイルの最初の設定が無題設定のとき[notitle]を省略できる
//    current=yes
//    current=no
//      現在の設定かどうか
//      current=yesのユーザ設定が1つだけ存在する
//    description=説明
//      設定の説明
//
//  コマンドラインの特別なパラメータ
//    -config=～はコマンドラインの最後に書いても他のパラメータよりも優先して解釈される
//    -config=default
//      デフォルト設定が仮設定にコピーされて現在の設定になる
//      一時的にデフォルト設定で起動したいときに使う
//      他のパラメータはデフォルト設定がコピーされた仮設定に上書きされる
//      仮設定は-config=defaultを指定すると消えてしまうので、仮設定の常用は避けた方が良い
//      仮設定以外の設定はcurrent=yesがcurrent=noになるものを除いて変化しない。すべての設定が初期化されるわけではない
//    -config=タイトル
//      タイトルで選択されたユーザ設定(無題設定、仮設定を含む)が現在の設定になる
//      他のパラメータは選択されたユーザ設定に上書きされる
//
//  その他の特筆すべきパラメータ
//    saveonexit=on
//    saveonexit=off
//      終了時に設定ファイルを更新するかどうか
//      現在の設定がsaveonexit=offのとき終了時に設定ファイルが更新されない
//      設定ファイルにエラーがあったとき
//        設定ファイルを更新してしまうとエラーの箇所やそれ以降に書かれていた内容が消えてしまう恐れがある
//        現在の設定がsaveonexit=onでも終了時に設定ファイルを更新せず手動で修正することを促す
//
//----------------------------------------------------------------------------------------

package xeij;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;
import javax.swing.*;
import javax.swing.event.*;

public class Settings {

  //RestorableFrameのキーの一覧
  //  ウインドウの位置とサイズ(～rect)、状態(～stat)、開くかどうか(～open)のパラメータで使う
  public static final String SGS_ACM_FRAME_KEY = "acm";  //ACM アドレス変換キャッシュモニタ
  public static final String SGS_ATW_FRAME_KEY = "atw";  //ATW 論理空間モニタ
  public static final String SGS_BFN_FRAME_KEY = "bfn";  //BFN ボタン機能
  public static final String SGS_BLG_FRAME_KEY = "blg";  //BLG 分岐ログ
  public static final String SGS_DBP_FRAME_KEY = "dbp";  //DBP データブレークポイント
  public static final String SGS_DDP_FRAME_KEY = "ddp";  //DDP 逆アセンブルリスト
  public static final String SGS_DGT_FRAME_KEY = "dgt";  //DGT コンソールウインドウ
  public static final String SGS_DMP_FRAME_KEY = "dmp";  //DMP メモリダンプリスト
  public static final String SGS_DRP_FRAME_KEY = "drp";  //DRP レジスタ
  public static final String SGS_FNT_FRAME_KEY = "fnt";  //FNT フォントエディタ
  public static final String SGS_FRM_FRAME_KEY = "frm";  //FRM メインウインドウ
  public static final String SGS_GRS_FRAME_KEY = "grs";
  public static final String SGS_GSA_FRAME_KEY = "gsa";
  public static final String SGS_KBS_FRAME_KEY = "kbs";  //KBS キーボード設定
  public static final String SGS_OLG_FRAME_KEY = "olg";  //OLG OPMログ
  public static final String SGS_PAA_FRAME_KEY = "paa";  //PAA 物理空間モニタ
  public static final String SGS_PFF_FRAME_KEY = "pff";  //PFF プロファイリング
  public static final String SGS_PFV_FRAME_KEY = "pfv";  //PFV プログラムフロービジュアライザ
  public static final String SGS_PPI_FRAME_KEY = "ppi";  //PPI ジョイスティック
  public static final String SGS_PRN_FRAME_KEY = "prn";  //PRN プリンタ
  public static final String SGS_RBP_FRAME_KEY = "rbp";  //RBP ラスタブレークポイント
  public static final String SGS_RTL_FRAME_KEY = "rtl";  //RTL ルートポインタリスト
  public static final String SGS_SMN_FRAME_KEY = "smn";  //SMN 音声モニタ
  public static final String SGS_SMT_FRAME_KEY = "smt";  //SMT 表示モードテスト
  public static final String SGS_SPV_FRAME_KEY = "spv";  //SPV スプライトパターンビュア
  public static final String SGS_PLV_FRAME_KEY = "plv";  //PLV パレットビュア
  public static final String SGS_TRM_FRAME_KEY = "trm";  //TRM ターミナル
  public static final String SGS_MLF_FRAME_KEY = "mlf";  //MLF 故障再現
  public static final String[] SGS_FRAME_KEYS = {
    SGS_ACM_FRAME_KEY,
    SGS_ATW_FRAME_KEY,
    SGS_BFN_FRAME_KEY,
    SGS_BLG_FRAME_KEY,
    SGS_DBP_FRAME_KEY,
    SGS_DDP_FRAME_KEY,
    SGS_DGT_FRAME_KEY,
    SGS_DMP_FRAME_KEY,
    SGS_DRP_FRAME_KEY,
    SGS_FNT_FRAME_KEY,
    SGS_FRM_FRAME_KEY,
    SGS_GRS_FRAME_KEY,
    SGS_GSA_FRAME_KEY,
    SGS_KBS_FRAME_KEY,
    SGS_OLG_FRAME_KEY,
    SGS_PAA_FRAME_KEY,
    SGS_PFF_FRAME_KEY,
    SGS_PFV_FRAME_KEY,
    SGS_PPI_FRAME_KEY,
    SGS_PRN_FRAME_KEY,
    SGS_RBP_FRAME_KEY,
    SGS_RTL_FRAME_KEY,
    SGS_SMN_FRAME_KEY,
    SGS_SMT_FRAME_KEY,
    SGS_SPV_FRAME_KEY,
    SGS_PLV_FRAME_KEY,
    SGS_TRM_FRAME_KEY,
    SGS_MLF_FRAME_KEY,
  };

  //デフォルト設定のベース
  //  キーワードと値の配列
  //  キーワードはnullではない。値もnullではない
  //  キーワードは""ではない。値が""のときは指定されていないものとみなす
  //  デフォルト設定で値がonまたはoffのパラメータはonまたはoffだけ指定できる
  //  デフォルト設定で値がyesまたはnoのパラメータはyesまたはnoだけ指定できる
  //  デフォルト設定で値が符号と数字だけのパラメータは符号と数字だけ指定できる
  //      キーワードがdataで終わるパラメータ
  //        値がgzip+base64で圧縮されている

  public static final String SGS_DEFAULT_PARAMETERS = (
    //PRG
    "verbose;on;" +  //冗長表示(on/off)
    "tinilog;off;" +
    //SGS
    //"_;;" +  //この設定の設定名。ここには書かない
    "saveonexit;on;" +  //終了時に設定を保存(on/off)
    "xxxopen;on;" +  //開いていたウインドウを復元する(on/off)
    "config;;" +  //復元する設定の設定名。パラメータで指定する。設定ファイルには出力しない
    "lang;en;" +  //言語(en/ja)。初期値は動作環境の言語
    "home;;" +  //ホームディレクトリ。初期値は動作環境のホームディレクトリ
    "dir;;" +  //カレントディレクトリ。初期値は動作環境のカレントディレクトリ
    //LNF
    "hsb;240,240,240,70,50,30,0,50,100;" +  //配色
    "hhssbb;none;" +  //配色(廃止)
    "fontsize;14;" +  //文字の大きさ(10/12/14/16/18)
    //KBD
    "keyboard;standard;" +  //キーボードの種類(none,standard,compact)
    "hidekeyboard;on;" +  //全画面表示のときキーボードを隠す(on/off)
    "keymapus;off;" +  //USレイアウト(on/off)
    "keymap;;" +  //Zキーボード以外のキーマップ
    "zkeyboard;off;" +  //Zキーボード(on/off)
    "zkeymap;;" +  //Zキーボードのキーマップ
    //BFN
    "f11key;fullscreen;" +  //F11 全画面表示の切り替え
    "shiftf11key;maximized;" +  //Shift+F11 最大化の切り替え
    "ctrlf11key;donothing;" +  //Ctrl+F11
    "altf11key;donothing;" +  //Alt+F11
    "f12key;seamless;" +  //F12 シームレスマウスの切り替え
    "shiftf12key;donothing;" +  //Shift+F12
    "ctrlf12key;donothing;" +  //Ctrl+F12
    "altf12key;donothing;" +  //Alt+F12
    "wheel;seamless;" +  //WheelButton シームレスマウスの切り替え
    "shiftwheel;donothing;" +  //Shift+WheelButton
    "ctrlwheel;donothing;" +  //Ctrl+WheelButton
    "altwheel;donothing;" +  //Alt+WheelButton
    "wheelup;trace1;" +  //WheelScrollUp トレース1回
    "shiftwheelup;trace10;" +  //Shift+WheelScrollUp トレース10回
    "ctrlwheelup;trace100;" +  //Ctrl+WheelScrollUp トレース100回
    "altwheelup;donothing;" +  //Alt+WheelScrollUp
    "wheeldown;step1;" +  //WheelScrollDown ステップ1回
    "shiftwheeldown;step10;" +  //Shift+WheelScrollDown ステップ10回
    "ctrlwheeldown;step100;" +  //Ctrl+WheelScrollDown ステップ100回
    "altwheeldown;return;" +  //Alt+WheelScrollDown ステップアンティルリターン
    "button4;donothing;" +  //Button4
    "shiftbutton4;donothing;" +  //Shift+Button4
    "ctrlbutton4;donothing;" +  //Ctrl+Button4
    "altbutton4;donothing;" +  //Alt+Button4
    "button5;donothing;" +  //Button5
    "shiftbutton5;donothing;" +  //Shift+Button5
    "ctrlbutton5;donothing;" +  //Ctrl+Button5
    "altbutton5;donothing;" +  //Alt+Button5
    "mousewheel;none;" +  //旧パラメータ。保存しない
    //GIF
    "gifwaitingtime;;" +  //GIFアニメーション録画の待ち時間(s)
    "gifrecordingtime;10;" +  //GIFアニメーション録画の録画時間(s)
    "gifmagnification;100;" +  //GIFアニメーション録画の倍率(%)
    "gifinterpolation;bilinear;" +  //アニメーション録画の補間アルゴリズム(nearest,bilinear,bicubic)
    //FLR
    "flrenabled;off;" +  //点滅光を軽減する
    "flrpalette;on;" +  //パレットによる点滅を止める
    "flrcontrast;on;" +  //コントラストを制限する
    "flrscreen;on;" +  //画面を徐々に明るくする
    "flrwavelength;20;" +  //パレットによる点滅の波長
    "flrlimit;13;" +  //コントラストの上限
    "flrdelay;20;" +  //画面が明るくなるまでの時間
    "flrinformed;off;" +  //案内済み
    //PNL
    "scaling;fitinwindow;" +  //画面スケーリングモード(fullscreen,maximized,fitinwindow,fixedscale)
    "fixedscale;100;" +  //固定倍率
    "aspectratio256x256;4:3;" +  //アスペクト比256x256
    "aspectratio384x256;4:3;" +  //アスペクト比384x256
    "aspectratio512x512;4:3;" +  //アスペクト比512x512
    "aspectratio640x480;4:3;" +  //アスペクト比640x480
    "aspectratio768x512;3:2;" +  //アスペクト比768x512
    "aspectratio1024x848;64:53;" +  //アスペクト比1024x848
    "interpolation;bilinear;" +  //補間アルゴリズム(nearest,bilinear,bicubic)
    "refreshrate;none;" +  //ホストのリフレッシュレート
    "adjustvsync;off;" +  //垂直同期周波数をホストのリフレッシュレートに合わせる(on/off)
    "roundedcornerratio;;" +  //角丸率
    //PNL
    "usecanvas;on;" +  //キャンバスを使う(on/off)
    "rotation;;" +  //回転(0/1/2/3)
    //MUS
    "seamless;on;" +  //シームレス/エクスクルーシブ(on/off)
    "followscroll;on;" +  //スクロールに追従する(on/off)
    "ctrlright;off;" +  //Ctrlキー＋左ボタンを右ボタンとみなす(on/off)
    "edgeaccel;off;" +  //縁部加速
    "mousespeed;;" +  //マウスカーソルの速度(0～40)
    "hostspixelunits;off;" +  //ホストの画素単位で動く(on/off)
    //TXC
    "textcopyarea;display;" +  //範囲モード(display/c_width/vram/enclosed)
    "textcopy;off;" +  //マウスで都度選択(on/off)
    //MDL
    "model;Hybrid;" +  //機種(Shodai|ACE|EXPERT|PRO|SUPER|XVI|Xellent30|Compact|Hybrid|X68030|030Compact|060turbo)
    "mpu010;off;" +  //機種のMPUが68000のとき68010に変更する(on/off)
    "mpu;none;" +  //MPUの種類(-1=機種に従う/0=MC68000/1=MC68010/3=MC68EC030/6=MC68060)
    "clock;none;" +  //MPUの動作周波数(-1=機種に従う,1..1000,Shodai,ACE,EXPERT,PRO,SUPER,XVI,Xellent30,Compact,Hybrid,X68030,030Compact,060turbo,060turboPRO)
    "mhz;200;" +  //任意の周波数(1～1000)。任意の負荷率がonのときの周波数のスピナーの値
    "util;off;" +  //任意の負荷率(on/off)
    "ratio;100;" +  //任意の負荷率(1～100)
    "waitcycles;on;" +  //ウェイトサイクル(on/off)
    //FPU
    "copro0;2;" +  //マザーボードコプロセッサ。0=なし,1=MC68881,2=MC68882,7=フルスペック,+8=三倍精度
    "copro1;2;" +  //拡張コプロセッサ#1。0=なし,1=MC68881,2=MC68882,7=フルスペック,+8=三倍精度
    "copro2;2;" +  //拡張コプロセッサ#2。0=なし,1=MC68881,2=MC68882,7=フルスペック,+8=三倍精度
    "onchipfpu;6;" +  //オンチップFPU。0=なし,6=MC68060,7=フルスペック,+8=三倍精度
    //FPK
    "fefunc;off;" +  //FEファンクション命令(on/off)
    "rejectfloat;off;" +  //FLOATn.Xを組み込まない(on/off)
    //BUS
    "himem68000;off;" +  //060turboのハイメモリはX68000でも有効(on/off)
    "highmemory;;" +  //X68030のハイメモリのサイズ(MB)(0/16)
    "highmemorysave;off;" +  //X68030のハイメモリの内容を保存する(on/off)
    "highmemorydata;;" +  //X68030のハイメモリの内容(gzip+base64)
    "highmemory060turbo;off;" +  //060turboのローカルメモリを使う(on/off)
    "localmemory;128;" +  //060turboのローカルメモリのサイズ(MB)(0/16/32/64/128/256)
    "localmemorysave;off;" +  //060turboのローカルメモリの内容を保存する(on/off)
    "localmemorydata;;" +  //060turboのローカルメモリの内容(gzip+base64)
    "cutfc2pin;off;" +  //FC2ピンをカットする(on/off)
    //MMR
    "memory;12;" +  //メインメモリのサイズ(0/1/2/4/6/8/10/12)。0は既定値
    "memorysave;on;" +  //メインメモリの内容を保存する(on/off)
    "memorydata;;" +  //メインメモリの内容(gzip+base64)
    //CON
    "pastepipe;off;" +  //貼り付けパイプと制御パイプを使う(on/off)
    "pipeencoding;sjis;" +  //貼り付けパイプと制御パイプの文字コード(sjis/utf8)
    //CRT
    "intermittent;0;" +  //間欠描画(0～4)
    "stereoscopic;off;" +  //立体視(on/off)
    "stereoscopicmethod;nakedeyecrossing;" +  //立体視の方法(nakedeyecrossing/nakedeyeparallel/sidebyside/topandbottom)
    "extendedgraphic;off;" +  //拡張グラフィック画面(on/off)
    "scanline;medium;" + //走査線エフェクト(off/weak/medium/strong/black)
    "dotclock;,,;" +  //ドットクロックオシレータ(low,high,vga)
    "eleventhbit;off;" +  //1024ドットノンインターレース(on/off)
    "sphericalscrolling;off;" +  //球面スクロール(on/off)
    "r00bit0zero;off;" +  //CRTC R00のビット0(on/off)
    //SPR
    "sprras;32;" +  //ラスタあたりのスプライトの枚数(0～/*2040*/1016)
    "dblspr;off;" +  //256枚のスプライト(on/off)(廃止)
    "numspr;128;" +  //スプライトの枚数(128/256/504/1016/* /2040*/)
    "sprbank;off;" +  //4096個のパターン(on/off)
    "spr768x512;off;" +  //768x512でスプライトを表示(on/off)
    "spr512bg1;off;" +  //512x512でBG1を表示(on/off)
    "sprrcscroll;off;" +  //行スクロールと列スクロール(on/off)
    "sprfpm;off;" +  //フルパターンメモリ(on/off)
    //SND
    "sound;on;" +  //音声出力(on/off)
    "volume;20;" +  //ボリューム(0～40)
    "soundinterpolation;linear;" +  //音声補間(thinning/linear/constant-area/linear-area)
    //OPM
    "opmoutput;on;" +  //OPM出力
    //PCM
    "pcmoutput;on;" +  //PCM出力
    "pcminterpolation;linear;" +  //PCM補間(constant/linear/hermite)
    "pcmoscfreq;0;" +  //PCM原発振周波数(0=8MHz/4MHz,1=8MHz/16MHz)
    //FDC
    //  ユニットとヒストリは後で設定する
    "fdreadonly;off;" +  //書き込み禁止(on/off)
    "fdappreboot;off;" +  //ここから再起動(on/off)
    //HDC
    //  ユニットとヒストリは後で設定する
    "sareadonly;off;" +  //書き込み禁止(on/off)
    "saappreboot;off;" +  //ここから再起動(on/off)
    //SPC
    //  ユニットとヒストリは後で設定する
    "screadonly;off;" +  //SCSI HDを書き込み禁止で開く(on/off)
    "scappreboot;off;" +  //ここから再起動(on/off)
    //HCD ホストCD-ROM
    "hcddebug;off;" +  //デバッグ情報(on/off)
    "hcdconnect;off;" +  //接続する(on/off)
    "hcdscsiex;off;" +  //off=内蔵,on=拡張
    "hcdscsiid;6;" +  //SCSI ID(0～15)
    "hcdvolume;25;" +  //音量(0～100)
    //HFS
    //  ユニットとヒストリは後で設定する
    "hfreadonly;off;" +  //書き込み禁止(on/off)
    "hfappreboot;off;" +  //ここから再起動(on/off)
    "utf8warning;off;" +  //HFSで開いたファイルがUTF-8のとき警告する
    //PPI
    "joykey;off;" +  //キーボードの一部をジョイスティックとみなす
    "joyauto;on;" +  //ジョイスティックポートが連続的に読み出されている間だけ有効
    "joyblock;on;" +  //ジョイスティック入力データとして処理されたキーボード入力データを取り除く
    "normal2button1;;" +  //ノーマル2ボタンパッド#1の設定
    "normal2button2;;" +  //ノーマル2ボタンパッド#2の設定
    "megadrive3button1;;" +  //メガドラ3ボタンパッド#1の設定
    "megadrive3button2;;" +  //メガドラ3ボタンパッド#2の設定
    "megadrive6button1;;" +  //メガドラ6ボタンパッド#1の設定
    "megadrive6button2;;" +  //メガドラ6ボタンパッド#2の設定
    "cyberstickdigital1;;" +  //サイバースティック(デジタルモード)#1の設定
    "cyberstickdigital2;;" +  //サイバースティック(デジタルモード)#2の設定
    "cyberstickanalog1;;" +  //サイバースティック(アナログモード)#1の設定
    "cyberstickanalog2;;" +  //サイバースティック(アナログモード)#2の設定
    "shiromadokun1;;" +  //白窓君1の仕様
    "shiromadokun2;;" +  //白窓君2の仕様
    "joystick1;normal2button1;" +  //ジョイスティックポート1に接続するデバイス
    "joystick2;normal2button2;" +  //ジョイスティックポート2に接続するデバイス
    "xinput;off;" +  //XInput(on/off)
    //EXS
    "scsiex;off;" +  //on=拡張SCSIポートを有効にする
    //SMR
    "boot;default;" +  //起動デバイス(default/std/fdN/hdN/scN/hfN/rom$X/ram$X)
    "keydly;-1;" +  //リピートディレイ(-1=既定/200+100*n)
    "keyrep;-1;" +  //リピートインターバル(-1=既定/30+5*n^2)
    "sram;none;" +  //SRAMイメージファイル名
    "sramdata;;" +  //SRAMの内容(gzip+base64)
    "sramsize;16;" +  //SRAMの容量(16/32/64)
    "romdb;none;" +  //ROMデバッガ起動フラグ
    "modifymemorysize;on;" +  //SRAMにあるメモリサイズを修正する
    "srambuserror;off;" +  //SRAMへの書き込み時のバスエラー
    "cache;none;" +  //起動時のキャッシュモード
    "stupsnd;none;" +  //起動音
    //BNK
    "bankdata;;" +  //バンクメモリの内容(gzip+base64)
    //ROM
    "rom;none;" +  //ROMのイメージファイル名
    "cgrom;none;" + //CGROMのイメージファイル名
    "rom30;none;" +  //X68030のIPLROMのイメージファイル名
    "scsiinrom;none;" +  //SCSIINROMのイメージファイル名
    "scsiexrom;none;" +  //SCSIEXROMのイメージファイル名
    "iplrom;none;" +  //IPLROMのイメージファイル名
    "x68000logo;none;" +  //X68000ロゴファイル名
    "iplrom16style;7;" +  //IPLROM 1.6のメッセージのスタイル
    "iplrom256k;off;" +  //X68000のIPLROMを256KBに改造する
    "iplrom16;off;" +  //IPLROM 1.6を使う
    "omusubi;off;" +  //おむすびフォントを使う
    //PRN
    "prnauto;off;" +  //自動保存
    "prnpath;printer;" +  //ディレクトリ
    "prndipsw;;" +  //ディップスイッチ
    "prnsize;A4;" +  //用紙のサイズ
    "prnorientation;portrait;" +  //用紙の方向
    "prntopmargin;11;" +  //印字不可領域
    "prnleftmargin;14;" +
    "prnrightmargin;14;" +
    "prnbottommargin;11;" +
    "prnrotation;;" +  //回転
    "prndarkmode;off;" +  //ダークモード
    "prnonline;on;" +  //オンライン
    "prnsinglecolor;;" +  //単色インクリボンの色
    "prnscalefactor;;" +  //表示倍率
    //RTC
    "rtcbankone;off;" +  //バンク
    "rtctimer;,,,,,,2,1,,1;" +  //日時カウンタ。ssmmhhwddmmyy。1980-01-01(Tue)
    "rtctwentyfour;1;" +  //12/24セレクトレジスタ
    "rtctimeren;on;" +  //タイマー
    "rtccmiloffset;32400000;" +  //歴通ミリ秒オフセット
    "rtcalarm;16,16,16,16,16,16,16;" +  //アラームレジスタ。mmhhwmm
    "rtcalarmen;off;" +  //アラーム
    "rtchzmode;0;" +  //Hzモード
    "rtcclkout;7;" +  //CLKOUTモード
    "rtctestmode;0;" +  //テストモード
    //PWC
    "powerswitch;off;" +  //電源スイッチ
    "externalswitch;off;" +  //外部電源スイッチ
    "onatstartup;on;" +  //開始時に電源を入れる
    "offatquit;on;" +  //終了時に電源を切る
    "quitwait;15;" +  //終了待機時間
    //SCC
    "sccfreq;5000000;" +  //SCCの動作周波数
    //SPV
    "spvbank;;" +  //バンク
    "spvblock;-1;" +  //パレットブロック
    "spvflip;-1;" +  //反転
    "spvhex;off;" +  //Hex
    "spvscale;;" +  //倍率
    "spvsize;-1;" +  //サイズ
    //PLV
    "plvarrgt;2;" +  //配置
    "plvhex;off;" +  //Hex
    "plvscale;;" +  //倍率
    //TRM
    "rs232cconnection;Terminal+%E2%87%94+AUX;" +  //接続。textをURLEncoderにかけて/で連結
    "terminalsettings;19200/B8/PN/S1/RTS;" +  //ターミナルの通信設定。38400などを/で連結
    "additionalport;none;" +  //追加ポート。portDescriptorを,で連結。エンコード
    "tcpiphost;0.0.0.0;" +  //TCP/IPホスト
    "tcpipport;54321;" +  //TCP/IPポート番号
    "tcpiputf8;off;" +  //SJIS⇔UTF-8変換
    //  PPPサーバー
    "pppaddress;192.168.10.1:192.168.10.2;" +  //ローカルIPアドレス:リモートIPアドレス
    "pppdebug;off;" +  //デバッグ表示あり
    "pppdirect;off;" +  //モデム層を省略する
    "ppplog;off;" +  //pppdの標準エラー出力をターミナルに出力する
    "pppserver;off;" +  //PPPサーバー有効
    //MU4
    "mercury;off;" +
    "mercuryoe;on;" +
    //MLF
    "mlfmodel;First%20gen;" +
    "mlfchip;;" +
    "mlfzero;;" +
    "mlfone;;" +
    "mlflow;;" +
    "mlfhigh;;" +
    "mlffrozen;;" +
    "mlfdynamic;;" +
    //XT3
    "xt3dipsw;3;" +  //Xellent30のDIPSW(0～3)
    "xt3memorykb;256;" +  //Xellent30のメモリのサイズ(256/1024)
    "xt3memorydata;;" +  //Xellent30のメモリの内容(gzip+base64)
    "xt3memorysave;off;"  //Xellent30のメモリの保存フラグ(on/off)
    );

  public static final String SGS_APPDATA_FOLDER = "XEiJ";  //Windowsのみ。AppData/Roamingフォルダに掘るフォルダの名前
  public static final String SGS_INI = "XEiJ.ini";  //設定ファイル名

  public static final Pattern SGS_BOOT_DEVICE_PATTERN = Pattern.compile ("^(?:default|std|(?:fd|hd|sc|hf)\\d+|r[oa]m\\$[0-9A-Fa-f]+)$", Pattern.CASE_INSENSITIVE);  //-bootに指定できる起動デバイス名

  public static String sgsAppDataRoamingFolder;  //Windowsのみ。AppData/Roamingフォルダ。1人のユーザが複数のPCで同期して利用できるファイルを入れる
  public static String sgsAppDataLocalFolder;  //Windowsのみ。AppData/Localフォルダ。1人のユーザがこのPCだけで利用できるファイルを入れる
  public static String sgsHomeDirectory;  //ホームディレクトリ
  public static String sgsCurrentDirectory;  //カレントディレクトリ

  public static File sgsIniParentDirectory;  //Windowsのみ。設定ファイルの親ディレクトリ。なければnull。設定ファイルを-iniで指定したときもnull
  public static String sgsIniParentPath;  //Windowsのみ。設定ファイルの親ディレクトリのパス。なければnull
  public static File sgsIniFile;  //設定ファイル
  public static String sgsIniPath;  //設定ファイルのパス

  public static boolean sgsSaveOnExit;  //true=終了時に設定を保存する
  public static boolean sgsXxxOpen;  //開いていたウインドウを復元する
  public static JCheckBoxMenuItem sgsSaveOnExitCheckBox;
  public static String sgsSaveiconValue;
  public static String sgsIrbbenchValue;

  public static HashMap<String,String> sgsDefaultMap;  //デフォルトの設定。SGS_DEFAULT_PARAMETERSを変換したもの
  public static HashMap<String,String> sgsStartMap;  //開始時の設定。デフォルトの設定に言語などを加えたもの。これと異なる値を持つキーだけ保存する
  public static HashMap<String,String> sgsCurrentMap;  //現在の設定
  public static HashMap<String,HashMap<String,String>> sgsRootMap;  //保存されているすべての設定。タイトル→設定

  public static JMenu sgsMenu;  //設定メニュー

  //DictionaryComparator
  //  辞書順コンパレータ
  //  大文字と小文字を区別しない
  //  数字の並びを数の大小で比較する
  //  一致したときは改めて大文字と小文字を区別して数字を特別扱いしないで比較し直す
  public static final Comparator<String> DictionaryComparator = new Comparator<String> () {
    @Override public int compare (String s1, String s2) {
      int l1 = s1.length ();
      int l2 = s2.length ();
      int b1, b2;  //部分文字列の開始位置(このインデックスを含む)
      int e1, e2;  //部分文字列の終了位置(このインデックスを含まない)
      int f = 0;  //比較結果
    compare:
      {
        for (b1 = 0, b2 = 0; b1 < l1 && b2 < l2; b1 = e1, b2 = e2) {
          int c1, c2;
          //数字と数字以外の境目を探して部分文字列の終了位置にする
          e1 = b1;
          c1 = s1.charAt (e1);
          c1 = ('0' - 1) - c1 & c1 - ('9' + 1);  //(c1<0)==isdigit(c1)
          for (e1++; e1 < l1; e1++) {
            c2 = s1.charAt (e1);
            c2 = ('0' - 1) - c2 & c2 - ('9' + 1);  //(c2<0)==isdigit(c2)
            if ((c1 ^ c2) < 0) {  //数字と数字以外の境目
              break;
            }
            c1 = c2;
          }
          e2 = b2;
          c1 = s2.charAt (e2);
          c1 = ('0' - 1) - c1 & c1 - ('9' + 1);  //(c1<0)==isdigit(c1)
          for (e2++; e2 < l2; e2++) {
            c2 = s2.charAt (e2);
            c2 = ('0' - 1) - c2 & c2 - ('9' + 1);  //(c2<0)==isdigit(c2)
            if ((c1 ^ c2) < 0) {  //数字と数字以外の境目
              break;
            }
            c1 = c2;
          }
          c1 = s1.charAt (b1);
          c2 = s2.charAt (b2);
          if ((('0' - 1) - c1 & c1 - ('9' + 1) & ('0' - 1) - c2 & c2 - ('9' + 1)) < 0) {  //両方数字のとき
            //ゼロサプレスする
            for (; b1 < e1 && s1.charAt (b1) == '0'; b1++) {
            }
            for (; b2 < e2 && s2.charAt (b2) == '0'; b2++) {
            }
            //桁数を比較する
            f = (e1 - b1) - (e2 - b2);
            if (f != 0) {
              break compare;
            }
            //数字を比較する
            for (; b1 < e1 && b2 < e2; b1++, b2++) {
              f = s1.charAt (b1) - s2.charAt (b2);
              if (f != 0) {
                break compare;
              }
            }
          } else {  //どちらかが数字ではないとき
            //大文字と小文字を区別しないで比較する
            //  小文字化してから比較する
            for (; b1 < e1 && b2 < e2; b1++, b2++) {
              c1 = s1.charAt (b1);
              c2 = s2.charAt (b2);
              f = ((c1 + ((('A' - 1) - c1 & c1 - ('Z' + 1)) >> 31 & 'a' - 'A')) -
                   (c2 + ((('A' - 1) - c2 & c2 - ('Z' + 1)) >> 31 & 'a' - 'A')));
              if (f != 0) {
                break compare;
              }
            }
            if (b1 < e1 || b2 < e2) {  //部分文字列が片方だけ残っているとき
              //  一致したまま片方だけ残るのは両方数字以外のときだけ
              //  部分文字列が先に終わった方は文字列が終わっているか数字が続いている
              //  部分文字列が残っている方は数字ではないので1文字比較するだけで大小関係がはっきりする
              //f = (b1 < l1 ? s1.charAt (b1) : -1) - (b2 < l2 ? s2.charAt (b2) : -1);
              f = (e1 - b1) - (e2 - b2);  //部分文字列が片方だけ残っているときは残っている方が大きい
              break compare;
            }
          }  //if 両方数字のとき/どちらかが数字ではないとき
        }  //for b1,b2
        f = (l1 - b1) - (l2 - b2);  //文字列が片方だけ残っているときは残っている方が大きい
        //一致したときは改めて大文字と小文字を区別して数字を特別扱いしないで比較し直す
        if (f == 0) {
          for (b1 = 0, b2 = 0; b1 < l1 && b2 < l2; b1++, b2++) {
            f = s1.charAt (b1) - s2.charAt (b2);
            if (f != 0) {
              break compare;
            }
          }
        }
      }  //compare
      return (f >> 31) - (-f >> 31);
    }  //compare(String,String)
  };  //DictionaryComparator

  //sgsInit ()
  //  設定の初期化
  public static void sgsInit () {

    sgsAppDataRoamingFolder = null;
    sgsAppDataLocalFolder = null;
    sgsHomeDirectory = null;
    sgsCurrentDirectory = null;

    sgsIniParentDirectory = null;
    sgsIniParentPath = null;
    sgsIniFile = null;
    sgsIniPath = null;

    sgsSaveOnExit = true;  //終了時に設定を保存する
    sgsSaveOnExitCheckBox = null;

    sgsSaveiconValue = null;
    sgsIrbbenchValue = null;

    //デフォルトの設定
    //  SGS_DEFAULT_PARAMETERSを分解してデフォルトの設定sgsDefaultMapを作る
    //  デフォルトの設定sgsDefaultMapには設定名を表すキー"_"が存在しない
    sgsDefaultMap = new HashMap<String,String> ();
    {
      String[] a = SGS_DEFAULT_PARAMETERS.split (";");
      for (int i = 0, l = a.length; i < l; i += 2) {
        String key = a[i];
        String value = i + 1 < l ? a[i + 1] : "";  //splitで末尾の空要素が削除されるのでa[i+1]が存在しないとき""とみなす
        sgsDefaultMap.put (key, value);
      }
    }
    //  ユニット
    //  SCSI HDのイメージファイル
    //    拡張  内蔵  sc[0-7]  sc[8-15]
    //    -----------------------------
    //    有効  有効    拡張     内蔵
    //    有効  無効    拡張     無効
    //    無効  有効    内蔵     無効
    //    無効  無効    無効     無効
    for (int i = 0; i < FDC.FDC_MAX_UNITS; i++) {
      sgsDefaultMap.put ("fd" + i, "none");  //FD
    }
    for (int i = 0; i < 16; i++) {
      sgsDefaultMap.put ("hd" + i, "none");  //SASIまたはSCSI
      sgsDefaultMap.put ("sa" + i, "none");  //SASI
      sgsDefaultMap.put ("sc" + i, "none");  //SCSI
    }
    for (int i = 0; i < 8; i++) {
      sgsDefaultMap.put ("si" + i, "none");  //内蔵SCSI
      sgsDefaultMap.put ("se" + i, "none");  //拡張SCSI
    }
    for (int i = 0; i < HFS.HFS_MAX_UNITS; i++) {
      sgsDefaultMap.put ("hf" + i, "none");  //HFS
    }
    //  ヒストリ
    for (int i = 0; i < JFileChooser2.MAXIMUM_HISTORY_COUNT; i++) {
      sgsDefaultMap.put ("fdhistory" + i, "none");
      sgsDefaultMap.put ("sahistory" + i, "none");
      sgsDefaultMap.put ("schistory" + i, "none");
      sgsDefaultMap.put ("hfhistory" + i, "none");
    }
    //  ウインドウの位置とサイズと状態
    for (String key : SGS_FRAME_KEYS) {
      sgsDefaultMap.put (key + "rect", "");  //ウインドウの位置とサイズ(x,y,width,height)
      sgsDefaultMap.put (key + "stat", "normal");  //ウインドウの状態(iconified/maximized/h-maximized/v-maximized/normal)
      sgsDefaultMap.put (key + "open", "off");  //ウインドウが開いているかどうか。メインのフレームも終了時にはoffになっている
    }

    //開始時の設定
    //  デフォルトの設定sgsDefaultMapのコピーに言語やホームディレクトリを追加して開始時の設定sgsStartMapを作る
    //  開始時の設定sgsStartMapには設定名を表すキー"_"が存在しない
    sgsStartMap = new HashMap<String,String> (sgsDefaultMap);

    //hf0の初期値はカレントディレクトリ
    sgsStartMap.put ("hf0", sgsHomeDirectory != null ? sgsHomeDirectory : HFS.HFS_DUMMY_UNIT_NAME);

    //言語
    sgsStartMap.put ("lang", Multilingual.mlnLang);

    if (false) {
      //すべての環境変数を表示する
      System.out.println ("\n[System.getenv()]");
      new TreeMap<String,String> (System.getenv ()).forEach ((k, v) -> System.out.println (k + " = " + v));  //System.getenv()はMap<String,String>
    }
    if (false) {
      //すべてのプロパティを表示する
      System.out.println ("\n[System.getProperties()]");
      TreeMap<String,String> m = new TreeMap<String,String> ();
      System.getProperties ().forEach ((k, v) -> m.put (k.toString (), v.toString ()));  //System.getProperties()はHashtable<Object,Object>
      m.forEach ((k, v) -> System.out.println (k + " = " + v));
    }

    //AppDataフォルダ
    boolean isWindows = System.getProperty ("os.name").indexOf ("Windows") >= 0;  //true=Windows
    sgsAppDataRoamingFolder = isWindows ? System.getenv ("APPDATA") : null;
    sgsAppDataLocalFolder = isWindows ? System.getenv ("LOCALAPPDATA") : null;
    //ホームディレクトリ
    //  new File("")
    sgsHomeDirectory = System.getProperty ("user.home");
    //カレントディレクトリ
    //  new File(".")
    sgsCurrentDirectory = System.getProperty ("user.dir");

    //デフォルトの設定ファイルの場所
    if (sgsAppDataRoamingFolder != null) {  //Windows
      sgsIniParentPath = new File (sgsAppDataRoamingFolder + File.separator + SGS_APPDATA_FOLDER).getAbsolutePath ();
      sgsIniParentDirectory = new File (sgsIniParentPath);
      sgsIniPath = sgsIniParentPath + File.separator + SGS_INI;
      sgsIniFile = new File (sgsIniPath);
    } else {  //Windows以外
      sgsIniParentPath = null;
      sgsIniParentDirectory = null;
      sgsIniPath = new File ((sgsHomeDirectory != null ? sgsHomeDirectory :
                              sgsCurrentDirectory != null ? sgsCurrentDirectory :
                              ".") + File.separator + SGS_INI).getAbsolutePath ();
      sgsIniFile = new File (sgsIniPath);
    }

    //現在の設定
    //  開始時の設定sgsStartMapをコピーして現在の設定sgsCurrentMapを作る
    //  ここで初めて設定名を表すキー"_"を追加する
    sgsCurrentMap = new HashMap<String,String> (sgsStartMap);
    sgsCurrentMap.put ("_", "");

    //保存されているすべての設定のマップ
    sgsRootMap = new HashMap<String,HashMap<String,String>> ();
    sgsRootMap.put ("", sgsCurrentMap);

    //コマンドラインのパラメータを読み取る
    //  -iniで設定ファイルが変更される場合がある
    HashMap<String,String> argumentMap = new HashMap<String,String> ();
    int fdNumber = 0;
    int hdNumber = 0;
    int scNumber = 0;
    int hfNumber = 0;
    for (int i = 0; i < XEiJ.prgArgs.length; i++) {
      String key = null;  //キー
      String value = XEiJ.prgArgs[i];  //引数。nullではないはず
    arg:
      {
        boolean boot = false;  //true=valueは-bootの値
        if (value.startsWith ("-")) {  //引数が"-"で始まっている
          //!!! 値が必要なものと必要でないものを区別したい
          int k = value.indexOf ('=', 1);
          if (k >= 0) {  //引数が"-"で始まっていて2文字目以降に"="がある
            key = value.substring (1, k);  //"-"の後ろから"="の手前まではキー
            value = value.substring (k + 1);  //"="の後ろは値
          } else {  //引数が"-"で始まっていて2文字目以降に"="がない
            //!!! "-"で始まる引数はすべてキーとみなされるので"-キー 値"の形では値に負の数値を書くことはできない
            key = value.substring (1);  //"-"の後ろはキー
            value = (i + 1 < XEiJ.prgArgs.length && !XEiJ.prgArgs[i + 1].startsWith ("-") ?  //次の引数があって次の引数が"-"で始まっていない
                     XEiJ.prgArgs[++i]  //次の引数は値
                     :  //次の引数がないまたは次の引数が"-"で始まっている
                     "1");  //値は"1"
          }
          if (!key.equalsIgnoreCase ("boot")) {  //-bootではない
            break arg;
          }
          boot = true;
        }
        //引数が"-"で始まっていないまたは-bootの値
        if (SGS_BOOT_DEVICE_PATTERN.matcher (value).matches ()) {  //起動デバイス名のとき
          //ファイルやディレクトリを探さず起動デバイスだけ設定する
          key = "boot";
          break arg;
        }
        String valueWithoutColonR = value.endsWith (":R") ? value.substring (0, value.length () - 2) : value;  //末尾の":R"を取り除いた部分
        File file = new File (valueWithoutColonR);
        if (file.isDirectory ()) {  //ディレクトリがある
          key = "hf" + hfNumber++;  //HFS
        } else if (file.isFile ()) {  //ファイルがある
          //FDMedia.fdmPathToMediaが大きすぎるファイルを処理できないので、
          //先に拡張子でハードディスクを区別する
          if (valueWithoutColonR.toUpperCase ().endsWith (".HDF")) {
            key = "sa" + hdNumber++;  //SASI ハードディスク
          } else if (valueWithoutColonR.toUpperCase ().endsWith (".HDS")) {
            key = "sc" + scNumber++;  //SCSI ハードディスク/CD-ROM
          } else if (FDMedia.fdmPathToMedia (valueWithoutColonR, null) != null) {
            key = "fd" + fdNumber++;  //フロッピーディスク
          } else if (HDMedia.hdmPathToMedia (valueWithoutColonR, null) != null) {
            key = "sa" + hdNumber++;  //SASI ハードディスク
          } else {
            key = "sc" + scNumber++;  //SCSI ハードディスク/CD-ROM
          }
        } else {  //ファイルもディレクトリもない
          String[] zipSplittedName = XEiJ.RSC_ZIP_SEPARATOR.split (valueWithoutColonR, 2);  //*.zip/entryを*.zipとentryに分ける
          if (zipSplittedName.length == 2 &&  //*.zip/entryで
              new File (zipSplittedName[0]).isFile ()) {  //*.zipがある
            if (FDMedia.fdmPathToMedia (valueWithoutColonR, null) != null) {
              key = "fd" + fdNumber++;  //フロッピーディスク
            } else if (HDMedia.hdmPathToMedia (valueWithoutColonR, null) != null) {
              key = "sa" + hdNumber++;  //SASI ハードディスク
            } else {
              System.out.println (Multilingual.mlnJapanese ? value + " は不明な起動デバイスです" :
                                  value + " is unknown boot device");
              continue;
            }
          } else {  //*.zip.entryでないか*.zipがない
            System.out.println (Multilingual.mlnJapanese ? value + " は不明な起動デバイスです" :
                                value + " is unknown boot device");
            continue;
          }
        }
        if (boot) {  //-bootの値のとき
          sgsPutParameter (argumentMap, "boot", key);  //起動デバイスを設定する
        }
      }  //arg
      //その他のオプション
      switch (key) {
      case "ini":  //設定ファイル
        sgsIniParentPath = null;
        sgsIniParentDirectory = null;
        sgsIniPath = new File (value).getAbsolutePath ();
        sgsIniFile = new File (sgsIniPath);
        break;
      case "saveicon":
        sgsSaveiconValue = value;
        break;
      case "irbbench":
        sgsIrbbenchValue = value;
        break;
      default:
        sgsPutParameter (argumentMap, key, value);  //パラメータを設定する
      }
    }  //for i
    System.out.println (Multilingual.mlnJapanese ? "設定ファイルは " + sgsIniPath + " です" :
                        "INI file is " + sgsIniPath);

    //設定ファイルを読み込んですべての設定sgsRootMapに格納する
    sgsDecodeRootMap (sgsLoadIniFile ());

    //コマンドラインなどのパラメータで使用する設定を選択する
    if (argumentMap.containsKey ("config")) {  //キー"config"が指定されているとき
      String name = argumentMap.get ("config");  //使用する設定名
      if (name.equals ("default")) {  //デフォルトの設定
        sgsCurrentMap.clear ();  //古いマップを消しておく
        sgsCurrentMap = new HashMap<String,String> (sgsStartMap);  //開始時の設定を現在の設定にコピーする
        sgsCurrentMap.put ("_", "");  //設定名を加える
        sgsRootMap.put ("", sgsCurrentMap);  //新しいマップを繋ぎ直す
      } else if (name.length () != 0 &&  //使用する設定名が""以外で
                 sgsRootMap.containsKey (name)) {  //存在するとき
        sgsCurrentMap.clear ();  //古いマップを消しておく
        sgsCurrentMap = new HashMap<String,String> (sgsRootMap.get (name));  //指定された設定を現在の設定にコピーする
        sgsCurrentMap.put ("_", "");  //設定名を元に戻す
        sgsRootMap.put ("", sgsCurrentMap);  //新しいマップを繋ぎ直す
      }
      argumentMap.remove ("config");  //キー"config"は指定されなかったことにする
    }

    //コマンドラインなどのパラメータを現在の設定に上書きする
    //argumentMap.forEach ((k, v) -> sgsCurrentMap.put (k, v));
    for (String key : argumentMap.keySet ()) {
      sgsCurrentMap.put (key, argumentMap.get (key));
    }

    //PRG
    String paramLang = sgsCurrentMap.get ("lang").toLowerCase ();
    Multilingual.mlnChange (paramLang.equals ("ja") ? "ja" : "en");
    XEiJ.prgVerbose = sgsGetOnOff ("verbose");  //冗長表示
    XEiJ.prgTiniLog = sgsGetOnOff ("tinilog");
    //SGS
    sgsSaveOnExit = sgsGetOnOff ("saveonexit");  //終了時に設定を保存するか
    sgsXxxOpen = sgsGetOnOff ("xxxopen");  //開いていたウインドウを復元する
    //LNF
    //  色
    //KBD
    //MUS
    //MPU
    //FPK
    //BUS
    //MMR
    //CRT
    if (CRTC.CRT_ENABLE_INTERMITTENT) {
      CRTC.crtIntermittentInterval = XEiJ.fmtParseInt (sgsCurrentMap.get ("intermittent"), 0, 0, 4, 0);  //間欠描画
    }
    if (CRTC.CRT_EXTENDED_GRAPHIC) {
      CRTC.crtExtendedGraphicRequest = sgsGetOnOff ("extendedgraphic");  //拡張グラフィック画面
    }
    //SND
    SoundSource.sndPlayOn = sgsGetOnOff ("sound");  //音声出力
    SoundSource.sndVolume = XEiJ.fmtParseInt (sgsCurrentMap.get ("volume"), 0, 0, SoundSource.SND_VOLUME_MAX, SoundSource.SND_VOLUME_DEFAULT);  //ボリューム
    {
      String s = sgsCurrentMap.get ("soundinterpolation").toLowerCase ();
      SoundSource.sndRateConverter = (s.equals ("thinning") ? SoundSource.SND_CHANNELS == 1 ? SoundSource.SNDRateConverter.THINNING_MONO : SoundSource.SNDRateConverter.THINNING_STEREO :  //間引き
                                      s.equals ("linear") ? SoundSource.SND_CHANNELS == 1 ? SoundSource.SNDRateConverter.LINEAR_MONO : SoundSource.SNDRateConverter.LINEAR_STEREO :  //線形補間
                                      s.equals ("constant-area") ? SoundSource.SNDRateConverter.CONSTANT_AREA_STEREO_48000 :  //区分定数面積補間
                                      s.equals ("linear-area") ? SoundSource.SNDRateConverter.LINEAR_AREA_STEREO_48000 :  //線形面積補間
                                      SoundSource.SND_CHANNELS == 1 ? SoundSource.SNDRateConverter.LINEAR_MONO : SoundSource.SNDRateConverter.LINEAR_STEREO);  //線形補間
    }
    //OPM
    OPM.opmOutputMask = sgsGetOnOff ("opmoutput") ? -1 : 0;  //OPM出力
    //PCM
    {
      ADPCM.pcmOutputOn = sgsGetOnOff ("pcmoutput");  //PCM出力
      String s = sgsCurrentMap.get ("pcminterpolation").toLowerCase ();
      ADPCM.pcmInterpolationAlgorithm = (s.equals ("constant") ? ADPCM.PCM_INTERPOLATION_CONSTANT :  //区分定数補間
                                         s.equals ("linear") ? ADPCM.PCM_INTERPOLATION_LINEAR :  //線形補間
                                         s.equals ("hermite") ? ADPCM.PCM_INTERPOLATION_HERMITE :  //エルミート補間
                                         ADPCM.PCM_INTERPOLATION_LINEAR);  //線形補間
      ADPCM.pcmOSCFreqRequest = XEiJ.fmtParseInt (sgsCurrentMap.get ("pcmoscfreq"), 0, 0, 1, 0);  //原発振周波数
    }
    //FDC
    //fdNはFDCで解釈する
    //HDC
    //hdNはHDCで解釈する
    //SPC
    //scNはSPCで解釈する
    //HFS
    //hfNはHFSで解釈する
    //PPI
    //SMR
    //BNK
    //FNT
    //PRN

    //ウインドウの位置とサイズと状態と開いているか
    for (String key : SGS_FRAME_KEYS) {
      //位置とサイズ
      int[] rect = sgsGetIntArray (key + "rect", 4, 0);
      //状態
      String s = sgsGetString (key + "stat").toLowerCase ();
      int state = (s.equals ("iconified") ? Frame.ICONIFIED :  //アイコン化する
                   s.equals ("maximized") ? Frame.MAXIMIZED_BOTH :  //最大化する
                   s.equals ("h-maximized") ? Frame.MAXIMIZED_HORIZ :  //水平方向だけ最大化する
                   s.equals ("v-maximized") ? Frame.MAXIMIZED_VERT :  //垂直方向だけ最大化する
                   Frame.NORMAL);  //通常表示
      //開いているか
      boolean opened = sgsXxxOpen && sgsGetOnOff (key + "open");
      RestorableFrame.rfmSet (key, rect, state, opened);
    }

    if (sgsIrbbenchValue != null) {
      InstructionBenchmark.irbBench (sgsIrbbenchValue);
      System.exit (0);
    }

  }  //sgsInit()

  //sgsTini ()
  //  後始末
  public static void sgsTini () {
    if (sgsSaveOnExit) {  //終了時に設定を保存する
      sgsSaveAllSettings ();
    }
  }  //sgsTini()



  //value = sgsGetString (key)
  //value = sgsGetString (key, def)
  //  現在の設定を読み出す。前後の空白を取り除く。noneまたは""のときdefを返す
  public static String sgsGetString (String key) {
    return sgsGetString (key, "");
  }
  public static String sgsGetString (String key, String def) {
    String value = sgsCurrentMap.get (key);
    if (value == null) {
      System.err.println ("sgsGetString: undefined key " + key);
      return def;
    }
    value = value.trim ();  //前後の空白を取り除く
    return value.equalsIgnoreCase ("none") || value.equals ("") ? def : value;  //noneまたは""のときdefを返す
  }

  //sgsPutString (key, value)
  //  現在の設定に書き込む。前後の空白を取り除く。""をnoneとみなす
  public static void sgsPutString (String key, String value) {
    if (!sgsCurrentMap.containsKey (key)) {
      System.err.println ("sgsPutString: undefined key " + key);
      return;
    }
    value = value.trim ();  //前後の空白を取り除く
    sgsCurrentMap.put (key, value.length () == 0 ? "none" : value);  //""をnoneとみなす
  }

  //b = sgsGetOnOff (key)
  //b = sgsGetOnOff (key, def)
  //  現在の設定を読み出す。前後の空白を取り除く
  //  0とoffとnoはfalse、1とonとyesはtrue、それ以外はdef
  public static boolean sgsGetOnOff (String key) {
    return sgsGetOnOff (key, false);
  }
  public static boolean sgsGetOnOff (String key, boolean def) {
    String value = sgsCurrentMap.get (key);
    if (value == null) {
      System.err.println ("sgsGetOnOff: undefined key " + key);
      return false;
    }
    value = value.trim ();  //前後の空白を取り除く
    return (value.equals ("0") ||
            value.equalsIgnoreCase ("off") ||
            value.equalsIgnoreCase ("no") ? false :  //0とoffとnoはfalse
            value.equals ("1") ||
            value.equalsIgnoreCase ("on") ||
            value.equalsIgnoreCase ("yes") ? true :  //1とonとyesはtrue
            def);  //それ以外はdef
  }

  //sgsPutOnOff (key, b)
  //  現在の設定に書き込む。trueをon、falseをoffとみなす
  public static void sgsPutOnOff (String key, boolean b) {
    if (!sgsCurrentMap.containsKey (key)) {
      System.err.println ("sgsPutOnOff: undefined key " + key);
      return;
    }
    sgsCurrentMap.put (key, b ? "on" : "off");  //trueをon、falseをoffとみなす
  }

  //i = sgsGetInt (key)
  //i = sgsGetInt (key, i0)
  //i = sgsGetInt (key, i0, min, max)
  //  現在の設定から整数を読み出す。整数でないか範囲外のときi0になる
  public static int sgsGetInt (String key) {
    return sgsGetInt (key, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
  }
  public static int sgsGetInt (String key, int i0) {
    return sgsGetInt (key, i0, Integer.MIN_VALUE, Integer.MAX_VALUE);
  }
  public static int sgsGetInt (String key, int i0, int min, int max) {
    String value = sgsCurrentMap.get (key);
    if (value == null) {
      System.err.println ("sgsGetInt: undefined key " + key);
      return i0;
    }
    value = value.trim ();  //前後の空白を取り除く
    if (value.length () != 0) {
      try {
        int i = Integer.parseInt (value, 10);
        if (min <= i && i <= max) {  //範囲内
          return i;
        }
      } catch (NumberFormatException nfe) {
      }
    }
    return i0;
  }

  //i = sgsGetLong (key)
  //i = sgsGetLong (key, i0)
  //i = sgsGetLong (key, i0, min, max)
  //  現在の設定から整数を読み出す。整数でないか範囲外のときi0になる
  public static long sgsGetLong (String key) {
    return sgsGetLong (key, 0, Long.MIN_VALUE, Long.MAX_VALUE);
  }
  public static long sgsGetLong (String key, long i0) {
    return sgsGetLong (key, i0, Long.MIN_VALUE, Long.MAX_VALUE);
  }
  public static long sgsGetLong (String key, long i0, long min, long max) {
    String value = sgsCurrentMap.get (key);
    if (value == null) {
      System.err.println ("sgsGetLong: undefined key " + key);
      return i0;
    }
    value = value.trim ();  //前後の空白を取り除く
    if (value.length () != 0) {
      try {
        long i = Long.parseLong (value, 10);
        if (min <= i && i <= max) {  //範囲内
          return i;
        }
      } catch (NumberFormatException nfe) {
      }
    }
    return i0;
  }

  //sgsPutInt (key, i)
  //sgsPutInt (key, i, i0)
  //  現在の設定に整数を書き込む。i0のとき""になる
  public static void sgsPutInt (String key, int i) {
    sgsPutInt (key, i, 0);
  }
  public static void sgsPutInt (String key, int i, int i0) {
    if (!sgsCurrentMap.containsKey (key)) {
      System.err.println ("sgsPutInt: undefined key " + key);
      return;
    }
    sgsCurrentMap.put (key, i == i0 ? "" : String.valueOf (i));
  }

  //sgsPutLong (key, i)
  //sgsPutLong (key, i, i0)
  //  現在の設定に整数を書き込む。i0のとき""になる
  public static void sgsPutLong (String key, long i) {
    sgsPutLong (key, i, 0);
  }
  public static void sgsPutLong (String key, long i, long i0) {
    if (!sgsCurrentMap.containsKey (key)) {
      System.err.println ("sgsPutLong: undefined key " + key);
      return;
    }
    sgsCurrentMap.put (key, i == i0 ? "" : String.valueOf (i));
  }

  //ia = sgsGetIntArray (key)
  //ia = sgsGetIntArray (key, n)
  //ia = sgsGetIntArray (key, n, v)
  //  現在の設定を読み出す。','で区切る。前後の空白を取り除く。""を0に、それ以外の文字列を10進数とみなして整数に変換する。失敗したときは0
  //  nは要素の数。-1=可変
  //  vは指定されなかった要素の値
  public static int[] sgsGetIntArray (String key) {
    return sgsGetIntArray (key, -1, 0);
  }
  public static int[] sgsGetIntArray (String key, int n) {
    return sgsGetIntArray (key, n, 0);
  }
  public static int[] sgsGetIntArray (String key, int n, int v) {
    String value = sgsCurrentMap.get (key);
    if (value == null) {
      System.err.println ("sgsGetIntArray: undefined key " + key);
      value = "";
    }
    String[] sa = value.length () == 0 ? new String[0] : value.split (",");  //','で区切る。"".split(",").length==1であることに注意
    if (n < 0) {  //可変
      n = sa.length;  //要素の数
    }
    int[] ia = new int[n];
    Arrays.fill (ia, v);
    for (int i = 0; i < n && i < sa.length; i++) {
      String s = sa[i].trim ();  //前後の空白を取り除く
      if (s.length () != 0) {
        try {
          ia[i] = Integer.parseInt (s, 10);  //10進数とみなして整数に変換する
        } catch (NumberFormatException nfe) {
        }
      }
    }
    return ia;
  }

  //sgsPutIntArray (key, ia)
  //sgsPutIntArray (key, ia, v)
  //  現在の設定に書き込む。vを""に、それ以外の整数を10進数の文字列に変換する。','で区切って並べる。末尾のvの並びを省略する
  public static void sgsPutIntArray (String key, int[] ia) {
    sgsPutIntArray (key, ia, 0);
  }
  public static void sgsPutIntArray (String key, int[] ia, int v) {
    if (!sgsCurrentMap.containsKey (key)) {
      System.err.println ("sgsPutIntArray: undefined key " + key);
      return;
    }
    int n = ia.length;
    while (0 < n && ia[n - 1] == v) {
      n--;  //末尾のvの並びを省略する
    }
    StringBuilder sb = new StringBuilder ();
    for (int i = 0; i < n; i++) {
      if (i != 0) {
        sb.append (',');  //','で区切って並べる
      }
      if (ia[i] != v) {  //vを""に、それ以外の整数を10進数の文字列に変換する
        sb.append (ia[i]);
      }
    }
    sgsCurrentMap.put (key, sb.toString ());
  }

  //array = sgsGetData (key)
  //  現在の設定を読み出す。gzip+base64で解凍する
  //  ""のときは長さが0の配列を返す
  public static byte[] sgsGetData (String key) {
    String value = sgsCurrentMap.get (key);
    if (value == null) {
      System.err.println ("sgsGetData: undefined key " + key);
      value = "";
    }
    if (value.length () == 0) {
      return new byte[0];
    }
    if (false) {
      return ByteArray.byaDecodeGzip (ByteArray.byaDecodeBase64 (value));
    } else {
      //Base64デコード
      byte[] decoded;
      try {
        decoded = Base64.getDecoder ().decode (value);
      } catch (IllegalArgumentException iae) {  //Base64が無効
        iae.printStackTrace ();
        return new byte[0];
      }
      //GZIP展開
      final int step = 1024 * 1024 * 10;  //10MB
      int length = 0;
      int next = step;
      ByteArrayOutputStream baos = new ByteArrayOutputStream ();
      ByteArrayInputStream bais = new ByteArrayInputStream (decoded);
      try (GZIPInputStream gzipis = new GZIPInputStream (bais)) {
        byte[] temp = new byte[65536];
        for (;;) {
          int k = gzipis.read (temp, 0, temp.length);
          if (k < 0) {
            break;
          }
          if (0 < k) {
            baos.write (temp, 0, k);
            length += k;
            if (next <= length) {
              if (next == step) {
                System.out.print (key);
              }
              System.out.print (".");
              next += step;
            }
          }
        }
        if (step <= length) {
          if (next - step < length) {
            System.out.print (".");
          }
          System.out.println ();
        }
      } catch (IOException ioe) {
        ioe.printStackTrace ();
        return new byte[0];
      }
      return baos.toByteArray ();
    }
  }

  //sgsPutData (key, array)
  //sgsPutData (key, array, offset, length)
  //  現在の設定に書き込む。gzip+base64で圧縮する
  //  中途半端に短くすると長さで種類を見分けることができなくなるので末尾の0は切り捨てない
  //  すべて0のときは""を書き込む
  public static void sgsPutData (String key, byte[] array) {
    sgsPutData (key, array, 0, array.length);
  }
  public static void sgsPutData (String key, byte[] array, int offset, int length) {
    if (!sgsCurrentMap.containsKey (key)) {
      System.err.println ("sgsPutData: undefined key " + key);
      return;
    }
    boolean zero = true;
    for (int i = 0; i < length; i++) {
      if (array[offset + i] != 0) {
        zero = false;
        break;
      }
    }
    String value = "";
    if (!zero) {
      if (false) {
        value = ByteArray.byaEncodeBase64 (ByteArray.byaEncodeGzip (array, offset, length));
      } else {
        do {
          //GZIP圧縮
          final int step = 1024 * 1024 * 10;  //10MB
          if (step <= length) {
            System.out.print (key);
          }
          ByteArrayOutputStream baos = new ByteArrayOutputStream ();
          try (GZIPOutputStream gzipos = new GZIPOutputStream (baos)) {
            for (int i = 0; i < length; i += step) {
              gzipos.write (array, offset + i, Math.min (step, length - i));
              if (step <= length) {
                System.out.print (".");
              }
            }
            gzipos.flush ();
          } catch (IOException ioe) {
            ioe.printStackTrace ();
            break;
          }
          byte[] compressed = baos.toByteArray ();
          if (step <= length) {
            System.out.println ();
          }
          //Base64エンコード
          value = Base64.getEncoder ().encodeToString (compressed);
        } while (false);
      }
    }
    sgsCurrentMap.put (key, value);
  }  //sgsPutData



  //sgsMakeMenu ()
  //  「設定」メニューを作る
  public static void sgsMakeMenu () {
    //アクションリスナー
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        String command = ae.getActionCommand ();
        switch (command) {
        case "Save settings on exit":  //終了時に設定を保存する
          sgsSaveOnExit = ((JCheckBoxMenuItem) ae.getSource ()).isSelected ();  //終了時に設定を保存するか
          break;
        case "Restore windows that were open":  //開いていたウインドウを復元する
          sgsXxxOpen = ((JCheckBoxMenuItem) ae.getSource ()).isSelected ();
          break;
        case "Delete all settings":  //すべての設定を消去する
          sgsDeleteAllSettings ();
          break;
        }
      }
    };
    //メニュー
    sgsMenu = Multilingual.mlnText (
      ComponentFactory.createMenu (
        "Configuration file",
        sgsSaveOnExitCheckBox =
        Multilingual.mlnText (
          ComponentFactory.createCheckBoxMenuItem (sgsSaveOnExit, "Save settings on exit", listener),
          "ja", "終了時に設定を保存する"),
        Multilingual.mlnText (
          ComponentFactory.createCheckBoxMenuItem (sgsXxxOpen, "Restore windows that were open", listener),
          "ja", "開いていたウインドウを復元する"),
        ComponentFactory.createHorizontalSeparator (),
        Multilingual.mlnText (
          ComponentFactory.createMenuItem ("Delete all settings", listener),
          "ja", "すべての設定を消去する")
        ),
      "ja", "設定ファイル");
  }  //sgsMakeMenu()

  //sgsSaveAllSettings ()
  //  設定ファイルを保存する
  public static void sgsSaveAllSettings () {
    //MLN
    sgsCurrentMap.put ("lang", Multilingual.mlnLang);  //言語
    //PRG
    sgsCurrentMap.put ("verbose", XEiJ.prgVerbose ? "on" : "off");  //冗長表示
    sgsPutOnOff ("tinilog", XEiJ.prgTiniLog);
    //SGS
    sgsCurrentMap.put ("saveonexit", sgsSaveOnExit ? "on" : "off");  //終了時に設定を保存する
    sgsCurrentMap.put ("xxxopen", sgsXxxOpen ? "on" : "off");  //開いていたウインドウを復元する
    //PNL
    //MUS
    //FPK
    sgsCurrentMap.put ("fefunc", FEFunction.fpkOn ? "on" : "off");  //FEファンクション命令
    sgsCurrentMap.put ("rejectfloat", FEFunction.fpkRejectFloatOn ? "on" : "off");  //FLOATn.Xを組み込まない
    //BUS
    //MMR
    sgsCurrentMap.put ("memory", String.valueOf (MainMemory.mmrMemorySizeRequest >>> 20));  //メインメモリのサイズ
    sgsCurrentMap.put ("memorysave", MainMemory.mmrMemorySaveOn ? "on" : "off");  //メインメモリの内容を保存する
    if (MainMemory.mmrMemorySaveOn) {
      if (false) {
        sgsCurrentMap.put ("memorydata", ByteArray.byaEncodeBase64 (ByteArray.byaEncodeGzip (MainMemory.mmrM8, 0x00000000, MainMemory.mmrMemorySizeCurrent)));  //メインメモリの内容
      } else {
        sgsPutData ("memorydata",
                    Arrays.copyOf (MainMemory.mmrM8, MainMemory.mmrMemorySizeCurrent));
      }
    } else {
      sgsCurrentMap.put ("memorydata", "");
    }
    //CRT
    if (CRTC.CRT_ENABLE_INTERMITTENT) {
      sgsCurrentMap.put ("intermittent", String.valueOf (CRTC.crtIntermittentInterval));  //間欠描画
    }
    if (CRTC.CRT_EXTENDED_GRAPHIC) {
      sgsCurrentMap.put ("extendedgraphic", CRTC.crtExtendedGraphicRequest ? "on" : "off");  //拡張グラフィック画面
    }
    //SND
    sgsCurrentMap.put ("sound", SoundSource.sndPlayOn ? "on" : "off");  //音声出力
    sgsCurrentMap.put ("volume", String.valueOf (SoundSource.sndVolume));  //ボリューム
    sgsCurrentMap.put ("soundinterpolation",
                       SoundSource.sndRateConverter == (SoundSource.SND_CHANNELS == 1 ? SoundSource.SNDRateConverter.THINNING_MONO : SoundSource.SNDRateConverter.THINNING_STEREO) ? "thinning" :  //間引き
                       SoundSource.sndRateConverter == (SoundSource.SND_CHANNELS == 1 ? SoundSource.SNDRateConverter.LINEAR_MONO : SoundSource.SNDRateConverter.LINEAR_STEREO) ? "linear" :  //線形補間
                       SoundSource.sndRateConverter == SoundSource.SNDRateConverter.CONSTANT_AREA_STEREO_48000 ? "constant-area" :  //区分定数面積補間
                       SoundSource.sndRateConverter == SoundSource.SNDRateConverter.LINEAR_AREA_STEREO_48000 ? "linear-area" :  //線形面積補間
                       "linear");  //線形補間
    //OPM
    sgsCurrentMap.put ("opmoutput", OPM.opmOutputMask != 0 ? "on" : "off");  //OPM出力
    //PCM
    sgsCurrentMap.put ("pcmoutput", ADPCM.pcmOutputOn ? "on" : "off");  //PCM出力
    sgsCurrentMap.put ("pcminterpolation",
                       ADPCM.pcmInterpolationAlgorithm == ADPCM.PCM_INTERPOLATION_CONSTANT ? "constant" :  //区分定数補間
                       ADPCM.pcmInterpolationAlgorithm == ADPCM.PCM_INTERPOLATION_LINEAR ? "linear" :  //線形補間
                       ADPCM.pcmInterpolationAlgorithm == ADPCM.PCM_INTERPOLATION_HERMITE ? "hermite" :  //エルミート補間
                       "linear");  //線形補間
    sgsCurrentMap.put ("pcmoscfreq", String.valueOf (ADPCM.pcmOSCFreqRequest));  //原発振周波数
    //PPI
    //SMR
    //BNK
    //FNT
    //PRN

    //ウインドウの位置とサイズと状態
    for (String key : SGS_FRAME_KEYS) {
      //位置とサイズ
      sgsPutIntArray (key + "rect", RestorableFrame.rfmGetRect (key), 0);
      //状態
      int state = RestorableFrame.rfmGetState (key);
      sgsPutString (key + "stat",
                    (state & Frame.ICONIFIED) == Frame.ICONIFIED ? "iconified" :  //アイコン化されている
                    (state & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH ? "maximized" :  //最大化されている
                    (state & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_HORIZ ? "h-maximized" :  //水平方向だけ最大化されている
                    (state & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_VERT ? "v-maximized" :  //垂直方向だけ最大化されている
                    "normal");  //通常表示
      //開いているか
      sgsPutOnOff (key + "open", sgsXxxOpen && RestorableFrame.rfmGetOpened (key));
    }

    //保存する
    sgsSaveIniFile (sgsEncodeRootMap ());

  }  //sgsSaveSettings()

  //sgsDecodeRootMap (text)
  //  テキストをsgsRootMapに変換する
  public static void sgsDecodeRootMap (String text) {
    sgsRootMap.clear ();  //すべての設定を消す
    sgsCurrentMap.clear ();  //古いマップを消しておく
    sgsCurrentMap = new HashMap<String,String> (sgsStartMap);  //開始時の設定を現在の設定にコピーする
    sgsCurrentMap.put ("_", "");  //設定名を加える
    sgsRootMap.put ("", sgsCurrentMap);  //新しいマップを繋ぎ直す
    HashMap<String,String> map = sgsCurrentMap;  //現在変換中の設定は現在の設定
    for (String line : text.split ("\n")) {
      line = line.trim ();  //キーの前の空白と値の後の空白を取り除く
      if (line.length () == 0 ||  //空行
          line.startsWith ("#")) {  //注釈
        continue;
      }
      int i = line.indexOf ('=');
      if (i < 0) {  //'='がない
        continue;
      }
      String key = line.substring (0, i).trim ().toLowerCase ();  //キー。後('='の前)の空白を取り除いて小文字化する
      String value = line.substring (i + 1).trim ();  //値。前('='の後)の空白を取り除く
      if (key.equals ("_")) {  //設定名。新しい設定の最初の行
        if (sgsRootMap.containsKey (value)) {  //同じ設定名が2回出てきたとき
          if (false) {
            map = null;  //新しい設定名が指定されるまで読み飛ばす(最初に書いた設定が残る)
          } else {
            map = sgsRootMap.get (value);  //既存の設定に上書きする(最後に書いた設定が残る)
          }
        } else {  //新しい設定
          map = new HashMap<String,String> (sgsStartMap);  //開始時の設定をコピーする
          map.put (key, value);  //sgsPutParameterは設定名のキー"_"を受け付けないことに注意
          sgsRootMap.put (value, map);
        }
        continue;
      }
      if (map == null) {  //新しい設定名が指定されるまで読み飛ばす
        continue;
      }
      sgsPutParameter (map, key, value);
    }  //for line
  }  //sgsDecodeRootMap()

  //strings = sgsEncodeRootMap ()
  //  sgsRootMapを文字列のリストに変換する
  public static ArrayList<String> sgsEncodeRootMap () {
    ArrayList<String> strings = new ArrayList<String> ();  //StringBuilderは大きすぎると失敗する
    String[] nameArray = sgsRootMap.keySet ().toArray (new String[0]);  //設定名の配列
    Arrays.sort (nameArray, DictionaryComparator);  //設定名をソートする。設定名が""の現在の設定が先頭に来る
    for (String name : nameArray) {
      HashMap<String,String> map = sgsRootMap.get (name);  //個々の設定
      if (map != sgsCurrentMap) {  //(先頭の)現在の設定でないとき
        strings.add ("\n");  //1行空ける
      }
      String[] keyArray = map.keySet ().toArray (new String[0]);  //キーの配列
      Arrays.sort (keyArray, DictionaryComparator);  //キーをソートする。設定名以外のキーはすべて英小文字で始まるので設定名のキー"_"が先頭に来る
      for (String key : keyArray) {
        String value = map.get (key);
        if (!(map == sgsCurrentMap && key.equals ("_")) &&  //現在の設定の設定名でない
            !key.equals ("config") &&  //キー"config"は設定ファイルに出力しない
            !value.equals (sgsStartMap.get (key))) {  //開始時の設定にないか、開始時の設定と異なる
          strings.add (key);
          strings.add ("=");
          strings.add (value);  //これが極端に大きい場合がある
          strings.add ("\n");
        }
      }
    }
    return strings;
  }  //sgsEncodeRootMap()

  //sgsDeleteAllSettings ()
  //  すべての設定を削除する
  public static void sgsDeleteAllSettings () {
    XEiJ.pnlExitFullScreen (true);
    if (JOptionPane.showConfirmDialog (
      XEiJ.frmFrame,
      Multilingual.mlnJapanese ? "すべての設定を消去しますか？" : "Do you want to delete all settings?",
      Multilingual.mlnJapanese ? "確認" : "Confirmation",
      JOptionPane.YES_NO_OPTION,
      JOptionPane.PLAIN_MESSAGE) == JOptionPane.YES_OPTION) {
      sgsDeleteIniFile ();  //設定ファイルを削除する
      sgsSaveOnExit = false;  //終了時に設定を保存しない
      sgsSaveOnExitCheckBox.setSelected (sgsSaveOnExit);
    }
  }  //sgsDeleteAllSettings()

  //sgsPutParameter (map, key, value)
  //  マップにパラメータを追加する
  //  デフォルトの設定sgsDefaultMapにないパラメータは無視される。設定名のキー"_"を受け付けないことに注意
  //  デフォルトの値が"off"または"on"のパラメータの値は"0","no","off"を指定すると"off"、それ以外は"on"に読み替えられる
  public static void sgsPutParameter (HashMap<String,String> map, String key, String value) {
    if (sgsDefaultMap.containsKey (key)) {  //設定できるパラメータ
      String defaultValue = sgsDefaultMap.get (key);  //デフォルトの値
      if (defaultValue.equals ("off") || defaultValue.equals ("on")) {  //デフォルトの値が"off"または"on"のとき
        value = (value.equals ("0") ||
                 value.equalsIgnoreCase ("no") ||
                 value.equalsIgnoreCase ("off") ? "off" : "on");  //"0","no","off"を"off"にそれ以外を"on"に読み替える
      }
      map.put (key, value);  //マップに追加する
    }
  }  //sgsPutParameter(HashMap<String,String>,String,String)



  //text = sgsLoadIniFile ()
  //  設定ファイルを読み込む
  public static String sgsLoadIniFile () {
    return XEiJ.rscGetTextFile (sgsIniPath);
  }  //sgsLoadIniFile()

  //sgsSaveIniFile ()
  //  設定ファイルに書き出す
  public static void sgsSaveIniFile (ArrayList<String> strings) {
    XEiJ.rscPutTextFile (sgsIniPath, strings);
  }

  //sgsDeleteIniFile ()
  //  設定ファイルを削除する
  public static void sgsDeleteIniFile () {
    if (sgsIniFile.isFile ()) {  //XEiJ.iniがある
      if (sgsIniFile.delete ()) {  //XEiJ.iniを削除する。削除できた
        System.out.println (sgsIniPath + (Multilingual.mlnJapanese ? " を削除しました" : " was removed"));
      } else {  //削除できない
        System.out.println (sgsIniPath + (Multilingual.mlnJapanese ? " を削除できません" : " cannot be removed"));
        return;
      }
    }
    String bakPath = sgsIniPath + ".bak";
    File bakFile = new File (bakPath);
    if (bakFile.isFile ()) {  //XEiJ.ini.bakがある
      if (bakFile.delete ()) {  //XEiJ.ini.bakを削除する。削除できた
        System.out.println (bakPath + (Multilingual.mlnJapanese ? " を削除しました" : " was removed"));
      } else {  //削除できない
        System.out.println (bakPath + (Multilingual.mlnJapanese ? " を削除できません" : " cannot be removed"));
        return;
      }
    }
    String tmpPath = sgsIniPath + ".tmp";
    File tmpFile = new File (tmpPath);
    if (tmpFile.isFile ()) {  //XEiJ.ini.tmpがある
      if (tmpFile.delete ()) {  //XEiJ.ini.tmpを削除する。削除できた
        System.out.println (tmpPath + (Multilingual.mlnJapanese ? " を削除しました" : " was removed"));
      } else {  //削除できない
        System.out.println (tmpPath + (Multilingual.mlnJapanese ? " を削除できません" : " cannot be removed"));
        return;
      }
    }
    if (sgsIniParentDirectory != null &&
        sgsIniParentDirectory.isDirectory ()) {  //AppData/Roaming/XEiJがある
      if (sgsIniParentDirectory.delete ()) {  //AppData/Roaming/XEiJの削除を試みる。ディレクトリが空でなければ失敗する。削除できた
        System.out.println (sgsIniParentPath + (Multilingual.mlnJapanese ? " を削除しました" : " was removed"));
      } else {  //削除できない
        System.out.println (sgsIniParentPath + (Multilingual.mlnJapanese ? " を削除できません" : " cannot be removed"));
      }
    }
  }  //sgsDeleteIniFile()



}  //class Settings



