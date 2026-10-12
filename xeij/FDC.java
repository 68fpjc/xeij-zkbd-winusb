//========================================================================================
//  FDC.java
//    en:Floppy disk controller
//    ja:フロッピーディスクコントローラ
//  Copyright (C) 2003-2026 Makoto Kamada
//
//  This file is part of the XEiJ (X68000 Emulator in Java).
//  You can use, modify and redistribute the XEiJ if the conditions are met.
//  Read the XEiJ License for more details.
//  https://stdkmd.net/xeij/
//========================================================================================

package xeij;

import java.awt.event.*;  //ActionEvent,ActionListener,ComponentAdapter,ComponentEvent,ComponentListener,FocusAdapter,FocusEvent,FocusListener,InputEvent,KeyAdapter,KeyEvent,KeyListener,MouseAdapter,MouseEvent,MouseListener,MouseMotionAdapter,MouseWheelEvent,WindowAdapter,WindowEvent,WindowListener,WindowStateListener
import java.io.*;  //BufferedInputStream,BufferedOutputStream,BufferedReader,BufferedWriter,File,FileInputStream,FileNotFoundException,FileReader,InputStream,InputStreamReader,IOException,OutputStreamWriter,RandomAccessFile
import java.lang.*;  //Boolean,Character,Class,Comparable,Double,Exception,Float,IllegalArgumentException,Integer,Long,Math,Number,Object,Runnable,SecurityException,String,StringBuilder,System
import java.util.*;  //ArrayList,Arrays,Calendar,GregorianCalendar,HashMap,Map,Map.Entry,Timer,TimerTask,TreeMap
import javax.swing.*;  //AbstractSpinnerModel,Box,ButtonGroup,DefaultListModel,ImageIcon,JButton,JCheckBox,JCheckBoxMenuItem,JDialog,JFileChooser,JFrame,JLabel,JList,JMenu,JMenuBar,JMenuItem,JPanel,JRadioButton,JScrollPane,JSpinner,JTextArea,JTextField,JTextPane,JViewport,ScrollPaneConstants,SpinnerListModel,SpinnerNumberModel,SwingConstants,SwingUtilities,UIManager,UIDefaults,UnsupportedLookAndFeelException
import javax.swing.filechooser.*;  //FileFilter,FileNameExtensionFilter

public class FDC {

  public static final boolean FDC_DEBUG_TRACE = false;
  public static final boolean FDC_DEBUG_DEFAULT = true;  //true=起動時からデバッグログを有効にする
  public static boolean fdcDebugLogOn;  //true=デバッグログを出力する

  //ポート
  public static final int FDC_STATUS_PORT  = 0x00e94001;  //FDC ステータス(RQM|DIO(OUT/IN)|NDM|CB|D3B|D2B|D1B|D0B)/コマンド
  public static final int FDC_DATA_PORT    = 0x00e94003;  //FDC データ/コマンド
  public static final int FDC_DRIVE_STATUS = 0x00e94005;  //FDD 状態(挿入|誤挿入|------)/機能(点滅|排出禁止|排出|-|選択####)
  public static final int FDC_DRIVE_SELECT = 0x00e94007;  //FDD $FF/選択(モータON|--|2DD|--|ドライブ##)

  //ユニット数
  public static final int FDC_MIN_UNITS = 2;  //最小ユニット数
  public static final int FDC_MAX_UNITS = 4;  //最大ユニット数

  public static FDUnit[] fdcUnitArray;  //ユニットの配列

  //メニュー
  public static JMenu fdcMenu;

  //ファイルフィルタ
  public static javax.swing.filechooser.FileFilter fdcFileFilter;  //java.io.FileFilterと紛らわしい

  //開くダイアログ
  public static OpenDialog fdcOpenDialog;  //開くダイアログ。null=作る前
  public static int fdcOpenUnit;  //開くユニットの番号
  public static ArrayList<File[]> fdcOpenHistory;  //作る前に追加されたヒストリ

  //フォーマットダイアログ
  public static JDialog fdcFormatDialog;  //ダイアログ
  public static JCheckBox fdcFormatX86SafeCheckBox;  //x86セーフチェックボックス
  public static JFileChooser2 fdcFormatFileChooser;  //ファイルチューザー
  public static FDMedia fdcFormatMedia;  //フォーマットするメディアの種類
  //public static boolean fdcFormatCopySystemFiles;  //true=システムファイルを転送する
  public static boolean fdcFormatX86SafeOn;  //true=x86セーフ
  public static JCheckBox fdcFormatCopyHumanSysCheckBox;  //HUMAN.SYSチェックボックス
  public static JCheckBox fdcFormatCopyCommandXCheckBox;  //COMMAND.Xチェックボックス
  public static boolean fdcFormatCopyHumanSysOn;  //true=HUMAN.SYSを書き込む
  public static boolean fdcFormatCopyCommandXOn;  //true=COMMAND.Xを書き込む


  //FDCステータス
  //  FDC ステータス(RQM|DIO(OUT/IN)|NDM|CB|D3B|D2B|D1B|D0B)/コマンド
  //    bit7  RQM  Request for Master  転送準備完了
  //                                     DIO=0(MPU→FDC)のときMPUはRQM=1を待ってから書き込む
  //                                     DIO=1(FDC→MPU)のときMPUはRQM=1を待ってから読み出す
  //    bit6  DIO  Data Input/Output   転送方向
  //                                     0  MPU→FDC。C-PhaseまたはE-Phase
  //                                     1  FDC→MPU。R-PhaseまたはE-Phase
  //    bit5  NDM  Non-DMA Mode        Non-DMAモード
  //                                     1  E-Phase(Non-DMA Modeで転送中)
  //    bit4  CB   FDC Busy            FDCビジー(CB)
  //                                     0  コマンド実行中ではない
  //                                        コマンドが終了してC-Phaseに戻るとき0にする
  //                                        SEEK/RECALIBRATEコマンドでC-PhaseからE-Phase(シーク中)に移行するとき0にする
  //                                        新たなコマンドを入力できる
  //                                        ただし、FDnビジー(DnB)が1のとき転送コマンドを入力してはならない
  //                                     1  コマンド実行中
  //                                        C-PhaseまたはE-Phase(シーク中)でコマンドの1バイト目が入力されたとき1にする
  //                                        新たなコマンドを入力できない
  //    bit3  D3B  FD3 Busy            FDnビジー(DnB)
  //    bit2  D2B  FD2 Busy              0  シーク実行中ではない
  //    bit1  D1B  FD1 Busy                 SESNE INTERRUPT STATUSコマンドのR-Phaseで、
  //    bit0  D0B  FD0 Busy                 シーク終了のリザルトステータスの1バイト目のST0が引き取られたとき0にする
  //                                        FDCビジー(CB)が0ならば転送コマンドを入力できる
  //                                     1  シーク実行中
  //                                        SEEK/RECALIBRATEコマンドでC-PhaseからE-Phase(シーク中)に移行するとき1にする
  //                                        FDCビジー(CB)が0でも転送コマンドを入力してはならない
  //
  //  C-PhaseまたはE-Phase(シーク中)のコマンドの1バイト目
  //    0x00e94001=0x80(RQM=1,DIO=0(MPU→FDC),NDM=0,CB=0)を待って
  //    0x00e94003にライトする
  //
  //  C-PhaseまたはE-Phase(シーク中)のコマンドの2バイト目
  //    0x00e94001=0x00(RQM=0,DIO=0(MPU→FDC),NDM=0,CB=0)ではなくて
  //    0x00e94001=0x10(RQM=0,DIO=0(MPU→FDC),NDM=0,CB=1)ではなくて
  //    0x00e94001=0x90(RQM=1,DIO=0(MPU→FDC),NDM=0,CB=1)を待って
  //    0x00e94003にライトする
  //
  //  R-Phaseのリザルトステータス
  //    0x00e94001=0x10(RQM=0,DIO=0(MPU→FDC),NDM=0,CB=1)ではなくて
  //    0x00e94001=0x50(RQM=0,DIO=1(FDC→MPU),NDM=0,CB=1)ではなくて
  //    0x00e94001=0xd0(RQM=1,DIO=1(FDC→MPU),NDM=0,CB=1)を待って
  //    0x00e94003からリードする
  //
  public static final int FDC_RQM        = 0x80;  //RQM
  public static final int FDC_MPU_TO_FDC = 0x00;  //DIO=0 OUT(MPU→FDC)
  public static final int FDC_FDC_TO_MPU = 0x40;  //DIO=1 IN(FDC→MPU)
  public static final int FDC_NDM        = 0x20;  //NDM
  public static final int FDC_CB         = 0x10;  //CB
  public static final int FDC_D3B        = 0x08;  //D3B
  public static final int FDC_D2B        = 0x04;  //D2B
  public static final int FDC_D1B        = 0x02;  //D1B
  public static final int FDC_D0B        = 0x01;  //D0B
  public static int fdcStatus;  //FDCステータス(RQM|DIO(OUT/IN)|NDM|CB|D3B|D2B|D1B|D0B)
  public static int fdcLastStatus;  //最後に表示したFDCステータス


  //リザルトステータス
  //  リザルトステータスとデバイスエラーの関係はHuman302の0x00010ceeを参照
  //    FDC_ST0_NR  「ディスクが入っていません、入れてください」
  //    FDC_ST1_NW  「プロテクトをはずして、同じディスクを入れてください」
  //    FDC_ST1_DE  「ＣＲＣエラー」
  //    FDC_ST2_DD  「ＣＲＣエラー」
  //    FDC_ST2_SN  「読み込みエラー」
  //    FDC_ST0_AT  「無効なメディアを使用しました」
  //  ST0
  //    bit6-7  IC   Interrupt Code 割り込みの発生要因
  //                 00  NT  Normal Terminate コマンドの正常終了
  //                 01  AT  Abnormal Terminate コマンドの異常終了
  //                         「無効なメディアを使用しました」
  //                 10  IC  Invalid Command 無効なコマンド
  //                 11  AI  Attention Interrupt デバイスに状態遷移があった
  //    bit5    SE   Seek End
  //                 1  SEEKコマンドまたはRECALIBRATEコマンドのシーク動作が正常終了または異常終了した
  //                    ディスクがないときもセット
  //    bit4    EC   Equipment Check
  //                 1  デバイスからFault信号を受け取った
  //                    RECALIBRATEコマンドでTrack 0信号を一定時間検出できなかった
  //                    _B_RECALIの強制レディチェックでドライブがないときセット
  //    bit3    NR   Not Ready
  //                 1  デバイスがReady状態でない
  //                    「ディスクが入っていません、入れてください」
  //    bit2    HD   Head Address 割り込み発生時のヘッドの状態
  //                 Sense Interrupt Statusコマンドでは常に0
  //    bit1    US1  Unit Select 1
  //    bit0    US0  Unit Select 0
  //                 割り込み発生時のデバイス番号
  public static final int FDC_ST0_NT = 0x00 << 24;
  public static final int FDC_ST0_AT = 0x40 << 24;
  public static final int FDC_ST0_IC = 0x80 << 24;
  public static final int FDC_ST0_AI = 0xc0 << 24;
  public static final int FDC_ST0_SE = 0x20 << 24;
  public static final int FDC_ST0_EC = 0x10 << 24;
  public static final int FDC_ST0_NR = 0x08 << 24;
  //  ST1
  //    bit7    EN   End of Cylinder
  //                 1  EOTで指定した最終セクタを超えてリードまたはライトを続けようとした
  //    bit6    -    常に0
  //    bit5    DE   Data Error
  //                 1  ディスク上のIDまたはデータのCRCエラーを検出した(READ IDコマンドを除く)
  //                    (IDとデータの区別はST2のDDによる)
  //                    「ＣＲＣエラー」
  //    bit4    OR   Overrun
  //                 1  MPUまたはDMAが規定時間内にデータ転送を行わなかった
  //    bit3    -    常に0
  //    bit2    ND   No Data
  //                 1  以下のコマンドでIDRで指定したセクタがトラック上に検出できなかった(このときST2のNCもセット)
  //                      READ DATA
  //                      READ DELETED DATA
  //                      WRITE DATA
  //                      WRITE DELETED DATA
  //                      SCAN
  //                    READ IDコマンドでトラック上にCRCエラーのないIDが見つからない
  //                    READ DIAGNOSTICコマンドでセクタIDと指定されたIDRの内容が一致しない
  //    bit1    NW   Not Writable
  //                 1  ライト系コマンドでライトプロテクト信号を検出した
  //                    「プロテクトをはずして、同じディスクを入れてください」
  //    bit0    MA   Missing Address Mark
  //                 1  IDをアクセスするコマンドでインデックスパルスを2回検出するまでにIDAMが見つからなかった
  //                    IDAMが見つかった後、DAMまたはDDAMが見つからなかった(このときST2のMDもセット)
  public static final int FDC_ST1_EN = 0x80 << 16;
  public static final int FDC_ST1_DE = 0x20 << 16;
  public static final int FDC_ST1_OR = 0x10 << 16;
  public static final int FDC_ST1_ND = 0x04 << 16;
  public static final int FDC_ST1_NW = 0x02 << 16;
  public static final int FDC_ST1_MA = 0x01 << 16;
  //  ST2
  //    bit7    -    常に0
  //    bit6    CM   Control Mark
  //                 1  READ DATAコマンドまたはREAD DIAGNOSTICコマンドまたはSCANコマンドでDDAMを検出した
  //                    READ DELETED DATAコマンドでDAMを検出した
  //                    削除データ読み込み時に通常データを読み込もうとしたまたはその逆のときセット
  //    bit5    DD   Data Error in Data Field
  //                 1  CRCエラーが検出された
  //                    「ＣＲＣエラー」
  //    bit4    NC   No Cylinder
  //                 1  ST1のNDに付帯して、IDのCバイトが一致せず0xffでもない(READ DIAGNOSTICを除く)
  //                    シリンダが見つからなかったときにセット
  //    bit3    SH   Scan Equal Hit
  //                 1  SCANコマンドでEqual条件を満足した
  //                    ベリファイコマンドで一致したときにセット
  //    bit2    SN   Scan Not Satisfied
  //                 1  SCANコマンドで最終セクタまで条件を満足しなかった
  //                    ベリファイコマンドで不一致があったときにセット
  //                    「読み込みエラー」
  //    bit1    BC   Bad Cylinder
  //                 1  ST1のNDに付帯して、IDのCバイトが0xff(READ DIAGNOSTICを除く)
  //                    シリンダの番号が規定外のときにセット
  //    bit0    MD   Missing Address Mark in Data Field
  //                 1   ST1のMAに付帯して、IDAMが見つかった後、DAMまたはDDAMが見つからなかった
  //                     データフィールドがないときにセット
  public static final int FDC_ST2_CM = 0x40 << 8;
  public static final int FDC_ST2_DD = 0x20 << 8;
  public static final int FDC_ST2_NC = 0x10 << 8;
  public static final int FDC_ST2_SH = 0x08 << 8;
  public static final int FDC_ST2_SN = 0x04 << 8;
  public static final int FDC_ST2_BC = 0x02 << 8;
  public static final int FDC_ST2_MD = 0x01 << 8;
  //  ST3
  //    bit7    FT   Fault
  //                 デバイスからのFault信号の状態
  //    bit6    WP   Write-Protected
  //                 デバイスからのWrite-Protected信号の状態
  //    bit5    RY   Ready
  //                 デバイスからのReady信号の状態
  //                 モータONから372ms後くらいに0→1
  //    bit4    T0   Track 0
  //                 デバイスからのTrack 0信号の状態
  //                 モータONまで0、モータONで0→1
  //    bit3    TS   Two Side
  //                 デバイスからのTwo Side信号の状態
  //                 常に0
  //    bit2    HD   Head Address
  //                 デバイスへのSide Select信号の状態
  //    bit1    US1  Unit Select 1
  //                 デバイスへのUnit Select 1信号の状態
  //    bit0    US0  Unit Select 0
  //                 デバイスへのUnit Select 0信号の状態
  public static final int FDC_ST3_FT = 0x80;
  public static final int FDC_ST3_WP = 0x40;
  public static final int FDC_ST3_RY = 0x20;
  public static final int FDC_ST3_T0 = 0x10;
  public static final int FDC_ST3_TS = 0x08;


  //コマンド
  public static final String[] FDC_COMMAND_NAME = {  //コマンド名(デバッグ用)
    "INVALID",  //0x00
    "INVALID",  //0x01
    "READ DIAGNOSTIC",  //0x02  トラックのフォーマットを調べる
    "SPECIFY",  //0x03  FDCの動作モードを設定する
    "SENSE DEVICE STATUS",  //0x04  FDDの状態を読み出す
    "WRITE DATA",  //0x05  セクタを指定してデータを書き込む
    "READ DATA",  //0x06  セクタを指定してデータを読み出す
    "RECALIBRATE",  //0x07  ヘッドをシリンダ0(最外周)へ移動させる
    "SENSE INTERRUPT STATUS",  //0x08  FDCの割り込み要因を読み出す
    "WRITE DELETED DATA",  //0x09  セクタを指定して削除データを書き込む
    "READ ID",  //0x0a  セクタのIDを読み出す
    "INVALID",  //0x0b
    "READ DELETED DATA",  //0x0c  セクタを指定して削除データを読み出す
    "WRITE ID",  //0x0d  トラックをフォーマットする
    "INVALID",  //0x0e
    "SEEK",  //0x0f  シリンダを指定してヘッドを移動させる
    "INVALID",  //0x10
    "SCAN EQUAL",  //0x11  条件に合うセクタを探す
    "INVALID",  //0x12
    "INVALID",  //0x13
    "RESET STANDBY",  //0x14  FDCのスタンバイ状態を解除する
    "SET STANDBY",  //0x15  FDCをスタンバイ状態にする
    "SOFTWARE RESET",  //0x16  FDCを初期状態にする
    "INVALID",  //0x17
    "INVALID",  //0x18
    "SCAN LOW OR EQUAL",  //0x19  条件に合うセクタを探す
    "INVALID",  //0x1a
    "INVALID",  //0x1b
    "INVALID",  //0x1c
    "SCAN HIGH OR EQUAL",  //0x1d  条件に合うセクタを探す
    "INVALID",  //0x1e
    "INVALID",  //0x1f
  };
/*
  public static final int[] FDC_COMMAND_LENGTH = {  //コマンドの長さ。INVALIDも含めて1以上
    1,  //0x00 INVALID
    1,  //0x01 INVALID
    9,  //0x02 READ DIAGNOSTIC
    3,  //0x03 SPECIFY
    2,  //0x04 SENSE DEVICE STATUS
    9,  //0x05 WRITE DATA
    9,  //0x06 READ DATA
    2,  //0x07 RECALIBRATE
    1,  //0x08 SENSE INTERRUPT STATUS
    9,  //0x09 WRITE DELETED DATA
    2,  //0x0a READ ID
    1,  //0x0b INVALID
    9,  //0x0c READ DELETED DATA
    6,  //0x0d WRITE ID
    1,  //0x0e INVALID
    3,  //0x0f SEEK
    1,  //0x10 INVALID
    9,  //0x11 SCAN EQUAL
    1,  //0x12 INVALID
    1,  //0x13 INVALID
    1,  //0x14 RESET STANDBY
    1,  //0x15 SET STANDBY
    1,  //0x16 SOFTWARE RESET
    1,  //0x17 INVALID
    1,  //0x18 INVALID
    9,  //0x19 SCAN LOW OR EQUAL
    1,  //0x1a INVALID
    1,  //0x1b INVALID
    1,  //0x1c INVALID
    9,  //0x1d SCAN HIGH OR EQUAL
    1,  //0x1e INVALID
    1,  //0x1f INVALID
  };
*/
  //  perl misc/itob.pl xeij/FDC.java FDC_COMMAND_LENGTH
  public static final byte[] FDC_COMMAND_LENGTH = "\1\1\t\3\2\t\t\2\1\t\2\1\t\6\1\3\1\t\1\1\1\1\1\1\1\t\1\1\1\t\1\1".getBytes (XEiJ.ISO_8859_1);
  public static int fdcCommandNumber;  //処理中のコマンド番号。C-Phaseの1バイト目まで-1、2バイト目からR-PhaseまでfdcCommandBuffer[0]&31。シークを伴うコマンドのE-Phase以降はfduCommandNumberにコピーしたものを使う

  //バッファ
  public static final byte[] fdcCommandBuffer = new byte[256];  //コマンドバッファ
  public static final byte[] fdcResultBuffer = new byte[256];  //リザルトバッファ
  public static final byte[] fdcTempBuffer = new byte[16384];  //WRITE IDまたはSCANで使用するバッファ
  public static byte[] fdcReadHandle;  //E-Phase(Read)のときfduImageまたはfdcIdBuffer、R-PhaseのときfdcResultBuffer、それ以外はnull
  public static byte[] fdcWriteHandle;  //C-Phase(Write)のときfdcCommandBuffer、E-Phase(Write)のときfduImageまたはfdcIdBuffer、それ以外はnull
  public static int fdcIndex;  //fdcReadHandleまたはfdcWriteHandleの次に読み書きするインデックス
  public static int fdcStart;  //fdcReadHandleまたはfdcWriteHandleの読み書きを開始するインデックス。デバッグ表示用
  public static int fdcLimit;  //fdcReadHandleまたはfdcWriteHandleの読み書きを終了するインデックス

  //  強制レディフラグ
  public static boolean fdcEnforcedReady;  //true=強制レディ状態(YM2151のCT2が1)

  //  ユニット選択
  //    ドライブコントロール(0x00e94005)のWriteのbit1-0
  //    ドライブステータス(0x00e94005)を読み出すユニットを選択する
  public static FDUnit fdcDriveLastSelected;  //ドライブコントロールで選択されているユニット


  //ポーリング
  //  実機のFDCはCB==1の間約1ms間隔(3.5インチは約2ms間隔)のポーリングでシークの処理とレディ信号の監視を行っている
  //  しかしポーリングのためのタスクを1個増やすとそれ自体の所要時間だけでなくタスクのスケジューリングの負荷も増えてしまう
  //  使わないときはまったく使わないフロッピーディスクのために全体のパフォーマンスを少しでも低下させたくない
  //  ここではレディ信号の監視にポーリングを使わないことにしてイベントの発生順序をなるべく実機に近付ける
  //


  //FDC割り込み
  //
  //  転送終了割り込み
  //    READ DATAやWRITE DATAなどの転送コマンドのE-Phase(転送中)が終了したとき
  //      リザルトステータス(ST0,ST1,ST2,C,H,R,N)を作る
  //      R-Phase(RQM=1,DIO=1(FDC→MPU))に移行する
  //      FDC割り込み要求(INT)を1にする
  //    メモ
  //      転送コマンドはFDC全体で同時に1つしか動かず、R-Phaseで発生するFDC割り込みは転送終了割り込みだけなので、
  //      転送終了割り込みが他のFDC割り込みと競合することはない
  //
  //  シーク終了割り込み
  //    SEEK/RECALIBRATEコマンドのE-Phase(シーク中)が終了したとき
  //      FDCビジー(CB)が1のとき
  //        (コマンド実行中。
  //         コマンドは必ず終了するので、コマンドが終了してFDCビジー(CB)が0になったときにFDC割り込み要求(INT)を1にする)
  //        シーク終了割り込み待機(seekEndInterruptWaiting)を1にする
  //      FDCビジー(CB)が0のとき
  //        シーク終了割り込み要求(seekEndInterruptRequest)が0のとき
  //          シーク終了割り込み要求(seekEndInterruptRequest)を1にする
  //          FDC割り込み要求(INT)を1にする
  //
  //  状態遷移割り込み
  //    モータONまたはモータOFFから一定の時間が経ったとき
  //      FDnビジー(DnB)が1のとき
  //        (シーク実行中。
  //         シークは必ず終了するので、シークが終了してFDnビジー(DnB)が0になったときに改めて状態遷移を確認する)
  //        状態遷移確認フラグ(attentionCheckWaiting)を1にする
  //      FDnビジー(DnB)が0のとき
  //        FDCビジー(CB)が1のとき
  //          (コマンド実行中。
  //           コマンドは必ず終了するので、コマンドが終了してFDCビジー(CB)が0になったときに改めて状態遷移を確認する)
  //          状態遷移確認フラグ(attentionCheckWaiting)を1にする
  //        FDCビジー(CB)が0のとき
  //          (シーク実行中でもコマンド実行中でもない。
  //           次のイベントがいつ起きるかわからないのでここでFDC割り込み要求(INT)を1にする。
  //           IOCSはコマンドを出力するとき割り込みを止めていないので、
  //           コマンドの1バイト目を出力した後にFDC割り込みハンドラが呼び出されて誤動作やハングアップする可能性がある)
  //          レディ信号の状態(isReady)が保存されたレディ信号の状態(savedReady)と違うとき
  //            レディ信号の状態(isReady)を保存する(isReady→RPYn)
  //            状態遷移割り込み要求(attentionInterruptRequest)が0のとき
  //              状態遷移割り込み要求(attentionInterruptRequest)を1にする
  //              FDC割り込み要求(INT)を1にする
  //    メモ
  //      μPD72065ではシーク中のユニットに対してレディ信号の監視が行われない
  //      シーク中に一瞬だけノットレディになってシークが終了する前にレディに戻った場合、その変化は検出されない
  //
  //  FDC割り込み要求(INT)
  //    0  Non-DMAモードのとき転送コマンドのE-Phase(転送中)で1バイト転送されたとき0にする
  //       転送コマンドのR-Phaseで1バイト目のST0が引き取られたとき0にする
  //       SESNE INTERRUPT STATUSコマンドでC-PhaseからR-Phaseに移行するとき0にする
  //    1  Non-DMAモードのとき転送コマンドのE-Phase(転送中)で1バイト転送するとき1にする
  //       READ DATAやWRITE DATAなどの転送コマンドでE-Phase(転送中)からR-Phaseに移行するとき1にする
  //       コマンドが終了してC-Phaseに戻るときシーク終了割り込み待機(seekEndInterruptWaiting)が1のユニットがあれば1にする
  //
  //  コマンドが終了してC-Phaseに戻るとき
  //    FDCビジー(CB)を0にする
  //    C-Phaseに移行する
  //    ユニット0..3について
  //      シークステップ待機(seekStepWaiting)が1のとき
  //        シークステップ待機(seekStepWaiting)を0にする
  //        →シークステップ
  //      FDnビジー(DnB)が1のとき
  //        シーク終了割り込み待機(seekEndInterruptWaiting)が1のとき
  //          シーク終了割り込み待機(seekEndInterruptWaiting)を0にする
  //          シーク終了割り込み要求(seekEndInterruptRequest)を1にする
  //          FDC割り込み要求(INT)を1にする
  //      FDnビジー(DnB)が0のとき
  //        状態遷移確認フラグ(attentionCheckWaiting)が1のとき
  //          状態遷移確認フラグ(attentionCheckWaiting)を0にする
  //          レディ信号の状態(isReady)が保存されたレディ信号の状態(savedReady)と違うとき
  //            レディ信号の状態(isReady)を保存する(isReady→savedReady)
  //            状態遷移割り込み要求(attentionInterruptRequest)が0のとき
  //              状態遷移割り込み要求(attentionInterruptRequest)を1にする
  //              FDC割り込み要求(INT)を1にする
  //
  //  SEEK/RECALIBRATEコマンド
  //    強制レディ状態で接続されていないとき
  //      AT(Abnormal Terminate)+SE(Seek End)+EC(Equipment Check)のリザルトステータスを準備する
  //      E-Phase(シーク中)に移行する
  //      →SEEK/RECALIBRATEコマンドのE-Phase(シーク中)が終了したとき
  //      メモ
  //        強制レディ状態でRECALIBRATEコマンドを実行してEC(Equipment Check)を返すかどうかでユニットの有無を判断できる
  //        シークティッカーが最初に呼び出された時点で強制レディ状態が解除されてしまっている場合があるので、
  //        RECALIBRATEコマンドは強制レディ状態かどうかを最初に確認しなければならない
  //    ノットレディのとき
  //      AT(Abnormal Terminate)+SE(Seek End)+NR(Not Ready)のリザルトステータスを準備する
  //      E-Phase(シーク中)に移行する
  //      →SEEK/RECALIBRATEコマンドのE-Phase(シーク中)が終了したとき
  //    レディのとき
  //      NT(Normal Terminate)+SE(Seek End)のリザルトステータスを準備する
  //      FDnビジー(DnB)を1にする
  //      目標シリンダ番号(NCNn)を設定する(既に設定されているときは上書きする)
  //      ステップレートカウンタ(SRC)を16-SRTにする
  //      シークティッカーを(あれば取り消してから)1ms後に予約する
  //      E-Phase(シーク中)に移行する
  //    メモ
  //      SEEK/RECALIBRATEコマンドは途中のE-Phase(シーク中)の期間FDCビジー(CB)が0になるので、
  //      複数のユニットで並行してSEEK/RECALIBRATEコマンドを実行できる
  //      シーク実行中のユニットの目標シリンダ番号(NCNn)はSEEK/RECALIBRATEコマンドで上書きできる
  //
  //  READ DATAやWRITE DATAなどの転送コマンド
  //    目標シリンダ番号(NCNn)を設定する(既に設定されているときは上書きする)
  //    ステップレートカウンタ(SRC)を16-SRTにする
  //    E-Phase(転送中)に移行する(RQM=0で待機する)
  //    シークティッカーを(あれば取り消してから)1ms後に予約する
  //    メモ
  //      READ DATAやWRITE DATAなどの転送コマンドは終わるまでFDCビジー(CB)が1のままなので同時に1つしか動かない
  //      シーク実行中のユニットの目標シリンダ番号(NCNn)をREAD DATAやWRITE DATAなどの転送コマンドで上書きしてはならない
  //      (ここでは上書きして動くようにしている)
  //
  //  シークティッカー
  //    FDCビジー(CB)が1のとき
  //      シークステップ待機(seekStepWaiting)を1にする
  //    FDCビジー(CB)が0のとき
  //      →シークステップ
  //
  //  シークステップ
  //    シリンダ番号(PCNn)と目標シリンダ番号(NCNn)が違うとき
  //      ステップレートカウンタ(SRC)をデクリメントする
  //      ステップレートカウンタ(SRC)が0になったとき
  //        ステップレートカウンタ(SRC)を16-SRTにする
  //        シリンダ番号(PCNn)が目標シリンダ番号(NCNn)よりも小さいとき
  //          シリンダ番号(PCNn)をインクリメントする
  //        シリンダ番号(PCNn)が目標シリンダ番号(NCNn)よりも大きいとき
  //          シリンダ番号(PCNn)をデクリメントする
  //      シークティッカーを1ms後に予約する
  //    シリンダ番号(PCNn)と目標シリンダ番号(NCNn)が同じとき
  //      SEEK/RECALIBRATEコマンドのとき
  //        →SEEK/RECALIBRATEコマンドのE-Phase(シーク中)が終了したとき
  //      READ DATAやWRITE DATAなどの転送コマンドのとき
  //        E-Phase(転送中)に移行する
  //        転送を開始する
  //
  //  SENSE INTERRUPT STATUSコマンド
  //    R-Phaseに移行するとき
  //      FDC割り込み要求(INT)を0にする
  //    ユニット0..3について
  //      FDnビジー(DnB)が1のとき
  //        シーク終了割り込み待機(seekEndInterruptWaiting)が1のとき
  //          シーク終了割り込み待機(seekEndInterruptWaiting)を0にする
  //          シーク終了割り込み要求(seekEndInterruptRequest)を1にする
  //      FDnビジー(DnB)が0のとき
  //        状態遷移確認フラグ(attentionCheckWaiting)が1のとき
  //          状態遷移確認フラグ(attentionCheckWaiting)を0にする
  //          レディ信号の状態(isReady)が保存されたレディ信号の状態(savedReady)と違うとき
  //            レディ信号の状態(isReady)を保存する(isReady→savedReady)
  //            状態遷移割り込み要求(attentionInterruptRequest)が0のとき
  //              状態遷移割り込み要求(attentionInterruptRequest)を1にする
  //    シーク終了割り込み要求(seekEndInterruptRequest)または状態遷移割り込み要求(attentionInterruptRequest)が1のユニットがないとき
  //      IC(Invalid Command)のリザルトステータスを返す
  //    シーク終了割り込み要求(seekEndInterruptRequest)または状態遷移割り込み要求(attentionInterruptRequest)が1のユニットがあるとき
  //      該当するユニットの中でユニット番号が最小のものについて
  //        シーク終了割り込み要求(seekEndInterruptRequest)が1のとき
  //          シーク終了割り込み要求(seekEndInterruptRequest)を0にする
  //          接続されていないとき
  //            AT(Abnormal Terminate)+SE(Seek End)+EC(Equipment Check)のリザルトステータスを出力する
  //          ノットレディのとき
  //            AT(Abnormal Terminate)+SE(Seek End)+NR(Not Ready)のリザルトステータスを出力する
  //          レディのとき
  //            NT(Normal Terminate)+SE(Seek End)のリザルトステータスを出力する
  //        シーク終了割り込み要求(seekEndInterruptRequest)が0のとき
  //          (状態遷移割り込み要求(attentionInterruptRequest)が1のとき)
  //          状態遷移割り込み要求(attentionInterruptRequest)を0にする
  //          AI(Attention Interrupt)のリザルトステータスを出力する
  //    R-PhaseでSE(Seek End)のリザルトステータスの1バイト目のST0が引き取られたとき
  //      ST0に対応するユニットのFDnビジー(DnB)を0にする
  //    R-Phaseが終わってC-Phaseに戻るとき
  //      シーク終了割り込み要求(seekEndInterruptRequest)が1のユニットが残っているとき
  //        FDC割り込み要求(INT)を1にする
  //        (状態遷移割り込み要求(attentionInterruptRequest)だけが残っている場合はFDC割り込み要求(INT)は1にならない)
  //
  //  FDC割り込みが受け付けられたとき
  //
  //  FDC割り込みハンドラ
  //    R-Phase(RQM=1,DIO=1(FDC→MPU))のとき
  //      リザルトステータス(ST0,ST1,ST2,C,H,R,N)を受け取る
  //      メモ
  //        FDC割り込みが発生したときR-Phase(RQM=1,DIO=1(FDC→MPU))ならば、
  //        それは実行中の転送コマンドの転送が正常終了または異常終了したことを知らせる転送終了割り込みである
  //        MPUがそのまま7バイトのリザルトステータスを引き取れば転送コマンドが終了してC-Phaseに戻る
  //        転送コマンドの実行中に他のユニットでシーク終了割り込み要求や状態遷移割り込み要求があった場合は、
  //        転送コマンドが終了してC-Phaseに戻ったときに改めてFDC割り込みが発生する
  //    C-Phase(RQM=1,DIO=0(MPU→FDC))のとき
  //      SENSE INTERRUPT STATUSコマンドでR-Phase(RQM=1,DIO=1(FDC→MPU))にする
  //      リザルトステータス(ST0,PCN)を受け取る
  //      IC(Invalid Command)でなければSENSE INTERRUPT STATUSコマンドを繰り返す
  //      メモ
  //        C-PhaseのときFDC割り込みハンドラはIC(Invalid Command)が返るまで、
  //        1回の割り込みでSENSE INTERRUPT STATUSコマンドを繰り返してすべてのリザルトステータスを引き取らなければならない
  //        複数のユニットで割り込み要求が発生した場合はユニット番号が最も小さいユニットのリザルトステータスが返る
  //        SENSE INTERRUPT STATUSコマンドが終わってC-Phaseに戻るときシーク終了割り込み要求が残っていると、
  //        改めてFDC割り込み要求が発生する
  //        状態遷移割り込み要求だけが残っているときはFDC割り込み要求は発生しない
  //
  //  FDC割り込みハンドラが終了したとき
  //
  public static int fdcSRT;  //ステップレートタイム。SPECIFYコマンドのSRT

  //fdcInit ()
  //  FDCを初期化する
  public static void fdcInit () {
    if (FDC_DEBUG_TRACE) {
      System.out.printf ("%08x fdcInit()\n", XEiJ.regPC0);
      fdcDebugLogOn = FDC_DEBUG_DEFAULT;
    }

    //ファイルフィルタ
    //  フロッピーディスクのイメージファイルかどうかを調べる
    //  ファイルチューザーとドロップターゲットで使う
    fdcFileFilter = new javax.swing.filechooser.FileFilter () {  //java.io.FileFilterと紛らわしい
      @Override public boolean accept (File file) {
        if (file.isDirectory ()) {  //ディレクトリがある
          return true;
        }
        if (!file.isFile ()) {  //ファイルがない
          return false;
        }
        String path = file.getPath ();
        if (fdcIsInsertedPath (path)) {  //既に挿入されている
          return false;
        }
        int dotIndex = path.lastIndexOf ('.');
        String upperExt = dotIndex < 0 ? "" : path.substring (dotIndex + 1).toUpperCase ();
        if (upperExt.equals ("DIM")) {  // *.dimはサイズに関係なく通す
          return true;
        }
        long longLength = file.length ();
        if (1024 * 1024 * 16 <= longLength) {  //大きすぎる
          return false;
        }
        int length = (int) longLength;
        for (FDMedia media : FDMedia.FDM_ARRAY) {
          if (media.fdmBytesPerDisk == length) {  //ファイルサイズが一致
            return true;
          }
        }
        return false;
      }
      @Override public String getDescription () {
        return (Multilingual.mlnJapanese ?
                "フロッピーディスクのイメージファイル (*.XDF,*.2HD,*.2HC,*.DIM,etc.)" :
                "Floppy disk image files (*.XDF,*.2HD,*.2HC,*.DIM,etc.)");
      }
    };

    //開くダイアログ
    fdcOpenDialog = null;
    fdcOpenUnit = 0;
    fdcOpenHistory = new ArrayList<File[]> ();
    for (int i = JFileChooser2.MAXIMUM_HISTORY_COUNT - 1; 0 <= i; i--) {
      fdcAddHistory (JFileChooser2.pathsToFiles (Settings.sgsGetString ("fdhistory" + i)));
    }

    //フォーマットダイアログ
    fdcFormatDialog = null;
    fdcFormatX86SafeCheckBox = null;
    fdcFormatFileChooser = null;
    fdcFormatMedia = FDMedia.FDM_2HD;
    //fdcFormatCopySystemFiles = false;
    fdcFormatX86SafeOn = true;
    fdcFormatCopyHumanSysCheckBox = null;
    fdcFormatCopyCommandXCheckBox = null;
    fdcFormatCopyHumanSysOn = true;
    fdcFormatCopyCommandXOn = true;

    //パラメータ
    //  ダイアログが書き込み禁止でもパラメータは:Rを付けなければ書き込み禁止にしない
    fdcUnitArray = new FDUnit[FDC_MAX_UNITS];
    for (int u = 0; u < FDC_MAX_UNITS; u++) {
      FDUnit unit = fdcUnitArray[u] = new FDUnit (u);
      if (u < FDC_MIN_UNITS) {
        unit.connect (false);  //ドライブ0とドライブ1は最初から接続されていて切り離せない
      }
      String path = Settings.sgsGetString ("fd" + u);
      boolean userWriteProtect = false;
      if (path.toUpperCase ().endsWith (":R")) {  //書き込み禁止モードで開く
        path = path.substring (0, path.length () - 2);
        userWriteProtect = true;
      }
      boolean hostWriteProtect = !new File (path).canWrite ();
      if (path.length () != 0) {
        unit.connect (true);  //接続されていなければ接続する
        if (unit.insert (path,
                         userWriteProtect || hostWriteProtect)) {  //挿入できた
          fdcAddHistory (new File (path).getAbsoluteFile ());
        }
      }
    }

    //fdcCommandBuffer = new byte[256];
    //fdcResultBuffer = new byte[256];
    //fdcTempBuffer = new byte[16384];
    fdcCPhase ();  //C-Phase
    if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
      fdcLastStatus = -1;
    }
    fdcEnforcedReady = false;  //強制レディOFF
    fdcDriveLastSelected = null;  //ドライブが選択されていない
    fdcSRT = 3;  //SRT
  }  //fdcInit()

  //fdcReset ()
  public static void fdcReset () {
    if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
      System.out.printf ("%08x fdcReset()\n", XEiJ.regPC0);
    }

    //リセットしたときCBを0にしないと
    //    ~FF8EB6:
    //            move.b  (a1),d1                 ;[$00E94001].b:FDC ステータス(RQM|DIO(OUT/IN)|NDM|CB|D3B|D2B|D1B|D0B)/コマンド
    //            btst.l  #$04,d1
    //            bne.s   ~FF8EB6
    //のループから抜けられなくなる
    fdcCPhase ();  //C-Phase
    if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
      fdcLastStatus = -1;
    }
    fdcEnforcedReady = false;  //強制レディOFF
    fdcDriveLastSelected = null;  //ドライブが選択されていない
    fdcSRT = 3;  //SRT

    //メディアが挿入されているユニットがあるときFDD割り込みを要求する
    for (int u = 0; u <= 3; u++) {  //ユニット0..3について
      FDUnit unit = fdcUnitArray[u];
      if (unit.fduIsInserted ()) {  //挿入されているとき
        IOInterrupt.ioiFddFall ();
        IOInterrupt.ioiFddRise ();
        break;
      }
    }

  }  //fdcReset()

  //fdcTini ()
  //  後始末
  //  イメージファイルに書き出す
  public static void fdcTini () {
    if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
      System.out.printf ("%08x fdcTini()\n", XEiJ.regPC0);
    }
    for (FDUnit unit : fdcUnitArray) {
      unit.fduTini ();
    }

    //開くダイアログ
    //  開くダイアログを作らなかったときはパラメータを更新しない
    if (fdcOpenDialog != null) {
      Settings.sgsPutOnOff ("fdreadonly", fdcOpenDialog.getReadOnly ());
      Settings.sgsPutOnOff ("fdappreboot", fdcOpenDialog.getReboot ());
      ArrayList<String> pathsList = fdcOpenDialog.getHistory ();
      int n = pathsList.size ();
      for (int i = 0; i < n; i++) {
        Settings.sgsPutString ("fdhistory" + i, pathsList.get (i));
      }
      for (int i = n; i < FDC_MAX_UNITS; i++) {
        Settings.sgsPutString ("fdhistory" + i, "");
      }
    }

    //ユニット
    for (int u = 0; u < FDC_MAX_UNITS; u++) {
      AbstractUnit unit = fdcUnitArray[u];
      Settings.sgsPutString (
        "fd" + u,
        unit.abuConnected && unit.abuInserted ?
        unit.abuWriteProtected ? unit.abuPath + ":R" : unit.abuPath :
        "");
    }

  }  //fdcTini()

  public static void fdcMakeMenu () {

    //アクションリスナー
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        Object source = ae.getSource ();
        String command = ae.getActionCommand ();
        switch (command) {
        case "Create new floppy disk image files":  //フロッピーディスクのイメージファイルの新規作成
          fdcOpenFormatDialog ();
          break;
        case "FDC debug log":  //FDC デバッグログ
          if (FDC_DEBUG_TRACE) {
            fdcDebugLogOn = ((JCheckBoxMenuItem) source).isSelected ();
          }
          break;
        }
      }
    };

    //FDメニュー
    fdcMenu = ComponentFactory.createMenu ("FD");  //横に長いとサブメニューを開きにくいので短くする
    ComponentFactory.addComponents (
      fdcMenu,
      ComponentFactory.createHorizontalBox (
        Multilingual.mlnText (ComponentFactory.createLabel ("Floppy disk"),
                              "ja", "フロッピーディスク")),
      ComponentFactory.createHorizontalSeparator ()
      );
    for (FDUnit unit : fdcUnitArray) {
      fdcMenu.add (unit.getMenuBox ());
    }
    ComponentFactory.addComponents (
      fdcMenu,
      ComponentFactory.createHorizontalSeparator (),
      Multilingual.mlnText (ComponentFactory.createMenuItem ("Create new floppy disk image files", listener),
                            "ja", "フロッピーディスクのイメージファイルの新規作成"),
      !FDC_DEBUG_TRACE ? null :
      Multilingual.mlnText (ComponentFactory.createCheckBoxMenuItem (fdcDebugLogOn, "FDC debug log", listener), "ja", "FDC デバッグログ")
      );

  }

  //inserted = fdcIsInsertedPath (path)
  //  パスで指定したファイルが既に挿入されているか調べる
  public static boolean fdcIsInsertedPath (String path) {
    if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
      System.out.printf ("%08x fdcIsInsertedPath(\"%s\")\n", XEiJ.regPC0, path);
    }
    boolean inserted = false;
    for (FDUnit unit : fdcUnitArray) {
      if (unit != null &&
          unit.abuConnected &&  //接続されている
          unit.fduIsInserted () &&  //挿入されている
          unit.abuPath.equals (path)) {  //パスが一致している
        inserted = true;  //既に挿入されている
        break;
      }
    }
    if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
      System.out.printf ("\tinserted=%b\n", inserted);
    }
    return inserted;
  }  //fdcIsInsertedPath(String)

  static class OpenDialog extends AbstractOpenDialog {
    public OpenDialog () {
      super (XEiJ.frmFrame,
             "Open floppy disk image files",
             "フロッピーディスクのイメージファイルを開く",
             false,  //ファイル
             fdcFileFilter);
    }
    @Override public void openFiles (File[] files, boolean reboot) {
      fdcOpenFiles (files, reboot);
    }
  }  //class OpenDialog

  //fdcOpenFiles (list, reset)
  //  開くダイアログで選択されたファイルを開く
  public static void fdcOpenFiles (File[] list, boolean reset) {
    if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
      System.out.printf ("%08x fdcOpenFiles({", XEiJ.regPC0);
      for (int i = 0; i < list.length; i++) {
        if (0 < i) {
          System.out.print (',');
        }
        System.out.printf ("\"%s\"", list[i].getPath ());
      }
      System.out.printf ("},%b)\n", reset);
    }
    boolean success = true;
    for (int u = fdcOpenUnit, k = 0; k < list.length; ) {
      if (FDC_MAX_UNITS <= u) {  //ユニットが足りない
        success = false;  //失敗
        break;
      }
      FDUnit unit = fdcUnitArray[u];  //ユニット
      if (!unit.abuConnected) {  //接続されていない
        u++;
        continue;
      }
      File file = list[k++];  //イメージファイル
      if (!file.isFile ()) {  //イメージファイルが存在しない
        success = false;  //失敗
        continue;
      }
      if (!unit.insert (file.getPath (),
                        fdcOpenDialog.getReadOnly () || !file.canWrite ())) {  //挿入できない
        success = false;  //失敗
        continue;
      }
      u++;
    }
    if (success) {  //すべて挿入できた
      fdcAddHistory (list);  //ヒストリに追加する
      if (reset) {  //ここから再起動
        XEiJ.mpuReset (0x9070 | fdcOpenUnit << 8, -1);
      }
    }
  }  //fdcOpenFiles

  //fdcMakeFormatDialog ()
  //  フォーマットダイアログを作る
  //  コマンドラインのみ
  public static void fdcMakeFormatDialog () {

    if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
      System.out.printf ("%08x fdcMakeFormatDialog()\n", XEiJ.regPC0);
    }

    //アクションリスナー
    ActionListener listener = new ActionListener () {
      @Override public void actionPerformed (ActionEvent ae) {
        switch (ae.getActionCommand ()) {
        case JFileChooser.APPROVE_SELECTION:
        case "Start formatting":  //フォーマットを開始する
          {
            File[] list = fdcFormatFileChooser.getSelectedFiles ();
            if (list.length > 0) {
              fdcFormatDialog.setVisible (false);
              if (!fdcFormatFiles (list)) {
                //!!! 失敗
              }
            }
          }
          break;
        case JFileChooser.CANCEL_SELECTION:
        case "Cancel":  //キャンセル
          fdcFormatDialog.setVisible (false);
          break;
        case "2HD (1232KB)":
          fdcFormatMedia = FDMedia.FDM_2HD;
          fdcFormatX86SafeCheckBox.setSelected (false);
          fdcFormatX86SafeCheckBox.setEnabled (false);
          break;
        case "2HC (1200KB)":
          fdcFormatMedia = FDMedia.FDM_2HC;
          fdcFormatX86SafeCheckBox.setSelected (false);
          fdcFormatX86SafeCheckBox.setEnabled (false);
          break;
        case "2DD (640KB)":
          fdcFormatMedia = FDMedia.FDM_2DD8;
          fdcFormatX86SafeCheckBox.setSelected (false);
          fdcFormatX86SafeCheckBox.setEnabled (false);
          break;
        case "2DD (720KB)":
          fdcFormatMedia = FDMedia.FDM_2DD9;
          fdcFormatX86SafeCheckBox.setSelected (false);
          fdcFormatX86SafeCheckBox.setEnabled (false);
          break;
        case "2HQ (1440KB)":
          fdcFormatMedia = FDMedia.FDM_2HQ;
          fdcFormatX86SafeCheckBox.setEnabled (true);
          fdcFormatX86SafeCheckBox.setSelected (fdcFormatX86SafeOn);
          break;
        case "2DD (800KB)":
          fdcFormatMedia = FDMedia.FDM_2DD10;
          fdcFormatX86SafeCheckBox.setSelected (false);
          fdcFormatX86SafeCheckBox.setEnabled (false);
          break;
        case "2HDE(1440KB)":
          fdcFormatMedia = FDMedia.FDM_2HDE;
          fdcFormatX86SafeCheckBox.setSelected (false);
          fdcFormatX86SafeCheckBox.setEnabled (false);
          break;
        case "2HS (1440KB)":
          fdcFormatMedia = FDMedia.FDM_2HS;
          fdcFormatX86SafeCheckBox.setSelected (false);
          fdcFormatX86SafeCheckBox.setEnabled (false);
          break;
          //case "Copy System Files":  //システムファイルを転送する
          //  fdcFormatCopySystemFiles = ((JCheckBox) ae.getSource ()).isSelected ();
          //  break;
        case "HUMAN.SYS":
          fdcFormatCopyHumanSysOn = fdcFormatCopyHumanSysCheckBox.isSelected ();  //HUMAN.SYSを書き込む/書き込まない
          if (fdcFormatCopyHumanSysOn) {  //HUMAN.SYSを書き込む
            fdcFormatCopyCommandXCheckBox.setEnabled (true);  //COMMAND.Xを書き込むかどうか選択できる
            fdcFormatCopyCommandXCheckBox.setSelected (fdcFormatCopyCommandXOn);  //COMMAND.Xを書き込む/書き込まない
          } else {  //HUMAN.SYSを書き込まない
            fdcFormatCopyCommandXCheckBox.setEnabled (false);  //COMMAND.Xを書き込むかどうか選択できない
            fdcFormatCopyCommandXCheckBox.setSelected (false);  //COMMAND.Xを書き込まない
          }
          break;
        case "COMMAND.X":
          fdcFormatCopyCommandXOn = fdcFormatCopyCommandXCheckBox.isSelected ();  //COMMAND.Xを書き込む/書き込まない
          break;
        case "x86-safe":  //x86 セーフ
          fdcFormatX86SafeOn = ((JCheckBox) ae.getSource ()).isSelected ();
          break;
        }
      }
    };

    //ファイルチューザー
    fdcMakeFormatFileChooser ();
    fdcFormatFileChooser.setFileFilter (fdcFileFilter);
    fdcFormatFileChooser.addActionListener (listener);

    //ダイアログ
    ButtonGroup mediaGroup = new ButtonGroup ();
    fdcFormatDialog = Multilingual.mlnTitle (
      ComponentFactory.createModalDialog (
        XEiJ.frmFrame,
        "Create new floppy disk image files",
        ComponentFactory.createBorderPanel (
          0, 0,
          ComponentFactory.createVerticalBox (
            fdcFormatFileChooser,
            ComponentFactory.createHorizontalBox (
              Box.createHorizontalStrut (12),
              Box.createHorizontalGlue (),
              ComponentFactory.createVerticalBox (
                ComponentFactory.createHorizontalBox (
                  ComponentFactory.createRadioButtonMenuItem (mediaGroup, fdcFormatMedia == FDMedia.FDM_2HD,   "2HD (1232KB)", listener),
                  ComponentFactory.createRadioButtonMenuItem (mediaGroup, fdcFormatMedia == FDMedia.FDM_2HC,   "2HC (1200KB)", listener),
                  ComponentFactory.createRadioButtonMenuItem (mediaGroup, fdcFormatMedia == FDMedia.FDM_2DD8,  "2DD (640KB)",  listener),
                  ComponentFactory.createRadioButtonMenuItem (mediaGroup, fdcFormatMedia == FDMedia.FDM_2DD9,  "2DD (720KB)",  listener),
                  ComponentFactory.createRadioButtonMenuItem (mediaGroup, fdcFormatMedia == FDMedia.FDM_2HQ,   "2HQ (1440KB)", listener)
                  ),
                ComponentFactory.createHorizontalBox (
                  ComponentFactory.createRadioButtonMenuItem (mediaGroup, fdcFormatMedia == FDMedia.FDM_2DD10, "2DD (800KB)",  listener),
                  ComponentFactory.createRadioButtonMenuItem (mediaGroup, fdcFormatMedia == FDMedia.FDM_2HDE,  "2HDE(1440KB)", listener),
                  ComponentFactory.createRadioButtonMenuItem (mediaGroup, fdcFormatMedia == FDMedia.FDM_2HS,   "2HS (1440KB)", listener),
                  Box.createHorizontalGlue (),
                  fdcFormatX86SafeCheckBox = ComponentFactory.setEnabled (
                    Multilingual.mlnText (ComponentFactory.createCheckBox (fdcFormatMedia == FDMedia.FDM_2HQ && fdcFormatX86SafeOn, "x86-safe", listener), "ja", "x86 セーフ"),
                    fdcFormatMedia == FDMedia.FDM_2HQ),
                  Box.createHorizontalStrut (12)
                  )
                ),
              Box.createHorizontalGlue (),
              Box.createHorizontalStrut (12)
              ),
            Box.createVerticalStrut (12),
            ComponentFactory.createHorizontalBox (
              Box.createHorizontalStrut (12),
              Box.createHorizontalGlue (),
              //Multilingual.mlnText (ComponentFactory.createCheckBox (fdcFormatCopySystemFiles, "Copy System Files", listener), "ja", "システムファイルを転送する"),
              fdcFormatCopyHumanSysCheckBox = ComponentFactory.createCheckBox (fdcFormatCopyHumanSysOn, "HUMAN.SYS", listener),
              Box.createHorizontalStrut (12),
              fdcFormatCopyCommandXCheckBox = ComponentFactory.setEnabled (
                ComponentFactory.createCheckBox (fdcFormatCopyHumanSysOn && fdcFormatCopyCommandXOn, "COMMAND.X", listener),
                fdcFormatCopyHumanSysOn),
              Box.createHorizontalGlue (),
              Box.createHorizontalStrut (12),
              Multilingual.mlnText (ComponentFactory.createButton ("Start formatting", KeyEvent.VK_F, listener), "ja", "フォーマットを開始する"),
              Box.createHorizontalStrut (12),
              Multilingual.mlnText (ComponentFactory.createButton ("Cancel", KeyEvent.VK_C, listener), "ja", "キャンセル"),
              Box.createHorizontalStrut (12)
              ),
            Box.createVerticalStrut (12)
            )
          )
        ),
      "ja", "フロッピーディスクのイメージファイルの新規作成");

  }  //fdcMakeFormatDialog()

  //fdcMakeFormatFileChooser ()
  //  フォーマットファイルチューザーを作る
  public static void fdcMakeFormatFileChooser () {
    if (fdcFormatFileChooser == null) {
      fdcFormatFileChooser = new JFileChooser2 ();
      //fdcFormatFileChooser.setMultiSelectionEnabled (true);  //複数選択可能
      fdcFormatFileChooser.setControlButtonsAreShown (false);  //デフォルトのボタンを消す
    }
  }

  //fdcOpenFormatDialog ()
  //  フォーマットダイアログを開く
  //  コマンドラインのみ
  public static void fdcOpenFormatDialog () {
    if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
      System.out.printf ("%08x fdcOpenFormatDialog()\n", XEiJ.regPC0);
    }
    if (fdcFormatDialog == null) {
      fdcMakeFormatDialog ();
    }
    fdcFormatFileChooser.rescanCurrentDirectory ();  //挿入されているファイルが変わると選択できるファイルも変わるのでリストを作り直す
    XEiJ.pnlExitFullScreen (true);
    fdcFormatDialog.setVisible (true);
  }  //fdcOpenFormatDialog()

  //success = fdcFormatFiles (list)
  //  フロッピーディスクをフォーマットする
  //  コマンドラインのみ
  public static boolean fdcFormatFiles (File[] list) {
    if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
      System.out.printf ("%08x fdcFormatFiles({", XEiJ.regPC0);
      for (int i = 0; i < list.length; i++) {
        if (0 < i) {
          System.out.print (',');
        }
        System.out.printf ("\"%s\"", list[i].getPath ());
      }
      System.out.println ('}');
    }
    boolean success = true;
  format:
    {
      //フローピーディスクのフォーマットデータを作る
      byte[] diskImage = new byte[fdcFormatMedia.fdmBytesPerDisk];
      if (!fdcFormatMedia.fdmMakeFormatData (diskImage, fdcFormatCopyHumanSysOn, fdcFormatCopyCommandXOn, fdcFormatX86SafeOn)) {
        success = false;  //失敗
        break format;
      }
      byte[] dimImage = null;
      int dimSize = 0;
      //書き出す
      int u = 0;
      for (File file : list) {
        String path = file.getPath ();
        String upperPath = path.toUpperCase ();
        if (upperPath.endsWith (".DIM")) {  // *.DIM
          if (fdcIsInsertedPath (path)) {  //他のユニットに挿入されている
            success = false;  //失敗
            break format;
          }
          if (dimImage == null) {
            dimImage = new byte[256 + fdcFormatMedia.fdmBytesPerDisk];
            dimSize = fdcFormatMedia.fdmMakeDimImage (dimImage, diskImage);  // *.DIMのイメージを作る
            if (dimSize < 0) {  // *.DIMのイメージを作れない
              success = false;  //失敗
              break format;
            }
          }
          if (!XEiJ.rscPutFile (path, dimImage, 0, dimSize)) {  //書き出せない
            success = false;  //失敗
            break format;
          }
        } else {  // *.DIM以外
          boolean extNotSpecified = true;
          for (String mediaExt : fdcFormatMedia.fdmExtensionArray) {
            if (upperPath.endsWith (mediaExt)) {  //適切な拡張子が指定されている
              extNotSpecified = false;
              break;
            }
          }
          if (extNotSpecified) {  //適切な拡張子が指定されていない
            if (!path.endsWith (".")) {
              path += ".";
            }
            path += fdcFormatMedia.fdmExtensionArray[0].toLowerCase ();  //拡張子を追加する
            upperPath = path.toUpperCase ();
          }
          if (fdcIsInsertedPath (path)) {  //他のユニットに挿入されている
            success = false;  //失敗
            break format;
          }
          if (!XEiJ.rscPutFile (path, diskImage, 0, fdcFormatMedia.fdmBytesPerDisk)) {  //書き出せない
            success = false;  //失敗
            break format;
          }
        }
        //空いているユニットがあれば挿入する
        while (u < FDC_MAX_UNITS) {
          FDUnit unit = fdcUnitArray[u++];  //ユニット
          if (unit.abuConnected &&  //接続されていて
              !unit.fduIsInserted () &&  //空いていて
              unit.insert (path,
                           false)) {  //挿入できた
            //フォーマットしたディスクの書き込みを禁止しても意味がないのでここでは書き込みを禁止しない
            break;
          }
        }
      }
    }  //format
    if (success) {  //すべてフォーマットできた
      fdcAddHistory (list);  //ヒストリに追加する
    }
    if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
      System.out.printf ("\tsuccess=%b\n", success);
    }
    return success;
  }  //fdcFormatFiles(File[])


  //fdcAddHistory (file)
  //  ファイルをヒストリに追加する
  public static void fdcAddHistory (File file) {
    fdcAddHistory (new File[] { file });
  }

  //fdcAddHistory (files)
  //  複数のファイルをヒストリに追加する
  public static void fdcAddHistory (File[] files) {
    if (fdcOpenDialog == null) {
      fdcOpenHistory.add (files);
    } else {
      fdcOpenDialog.addHistory (files);
    }
    fdcMakeFormatFileChooser ();
    fdcFormatFileChooser.addHistory (files);
    fdcFormatFileChooser.selectLastFiles ();
  }


  //d = fdcPeekStatus ()
  //  pbz (0x00e94001)
  //  FDC ステータス(RQM|DIO(OUT/IN)|NDM|CB|D3B|D2B|D1B|D0B)/コマンド
  public static int fdcPeekStatus () {
    return fdcStatus;
  }  //fdcPeekStatus()

  //d = fdcReadStatus ()
  //  rbz (0x00e94001)
  //  FDC ステータス(RQM|DIO(OUT/IN)|NDM|CB|D3B|D2B|D1B|D0B)/コマンド
  public static int fdcReadStatus () {
    int d = fdcStatus;
    if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
      if (fdcLastStatus == d) {
        System.out.print ('=');
      } else {
        fdcLastStatus = d;
        System.out.printf ("%08x fdcReadStatus(0x00e94001)=%s\n", XEiJ.regPC0, fdcStatusToString (d));
      }
    }
    return d;
  }  //fdcReadStatus

  //d = fdcPeekData ()
  //  pbz (0x00e94003)
  //  FDC データ/コマンド
  public static int fdcPeekData () {
    return (fdcReadHandle == null ? 0 :  //Read中でない
            fdcReadHandle[fdcIndex] & 255);
  }  //fdcPeekData

  //d = fdcReadData ()
  //  rbz (0x00e94003)
  //  FDC データ/コマンド
  public static int fdcReadData () {
    if (fdcReadHandle == null) {  //Read中でない
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("%08x fdcReadData(0x00e94003=???)=0x00\n", XEiJ.regPC0);
      }
      return 0;
    }
    int d = fdcReadHandle[fdcIndex] & 255;
    if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
      if (fdcIndex < fdcStart + 8 || fdcLimit - 8 <= fdcIndex) {  //先頭8バイトまたは末尾8バイト
        System.out.printf ("%08x fdcReadData(0x00e94003=%s[0x%08x])=0x%02x\n",
                           XEiJ.regPC0,
                           fdcReadHandle == fdcResultBuffer ? "fdcResultBuffer" :
                           fdcReadHandle == fdcTempBuffer ? "fdcTempBuffer" :
                           "fduImage",
                           fdcIndex,
                           d);
      }
    }
    fdcIndex++;
    if (fdcIndex < fdcLimit) {  //継続
      if (fdcReadHandle != fdcResultBuffer) {  //E-Phase
        HD63450.dmaFallREQ (0);  //DMA転送継続
      } else {  //R-Phase
        if (fdcIndex == 1) {  //R-PhaseでSE(Seek End)のリザルトステータスの1バイト目のST0が引き取られたとき
          fdcStatus &= ~(1 << (d & 3));  //ST0に対応するユニットのFDnビジー(DnB)を0にする
          if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
            System.out.printf ("\tfdcStatus=%s\n", fdcStatusToString (fdcStatus));
          }
        }
      }
    } else {  //終了
      if (fdcReadHandle != fdcResultBuffer) {  //E-Phase
        fdcEPhaseEnd ();
      } else {  //R-Phase
        fdcRPhaseEnd ();
      }
    }
    return d;
  }  //fdcReadData

  //d = fdcPeekDriveStatus ()
  //  pbz (0x00e94005)
  //  FDD 状態(挿入|誤挿入|000000)/機能(点滅|排出禁止|排出|-|選択####)
  public static int fdcPeekDriveStatus () {
    return (fdcDriveLastSelected == null ? 0 :  //ドライブが選択されていない
            fdcDriveLastSelected.fduDriveStatus ());
  }  //fdcPeekDriveStatus

  //d = fdcReadDriveStatus ()
  //  rbz (0x00e94005)
  //  FDD 状態(挿入|誤挿入|000000)/機能(点滅|排出禁止|排出|-|選択####)
  public static int fdcReadDriveStatus () {
    int d = (fdcDriveLastSelected == null ? 0 :  //ドライブが選択されていない
             fdcDriveLastSelected.fduDriveStatus ());
    if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
      System.out.printf ("%08x fdcReadDriveStatus(0x00e94005)=0x%02x(挿入=%d 誤挿入=%d)\n",
                         XEiJ.regPC0,
                         d,
                         d >> 7,
                         d >> 6 & 1);
    }
    return d;
  }  //fdcReadDriveStatus

  //fdcWriteCommand (d)
  //  wb (0x00e94001, d)
  //  FDC ステータス(RQM|DIO(OUT/IN)|NDM|CB|D3B|D2B|D1B|D0B)/コマンド
  public static void fdcWriteCommand (int d) {
    if (fdcWriteHandle != fdcCommandBuffer) {  //C-Phaseでない
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("%08x fdcWriteCommand(0x00e94001=???,0x%02x)\n",
                           XEiJ.regPC0,
                           d & 255);
      }
      return;
    }
    if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
      if (fdcIndex < fdcStart + 8 || fdcLimit - 8 <= fdcIndex) {  //先頭8バイトまたは末尾8バイト
        System.out.printf ("%08x fdcWriteCommand(0x00e94001=%s[0x%08x],0x%02x)\n",
                           XEiJ.regPC0,
                           "fdcCommandBuffer",
                           fdcIndex,
                           d & 255);
      }
    }
    fdcWriteHandle[fdcIndex++] = (byte) d;
    if (fdcLimit <= fdcIndex) {  //C-Phaseが終了した
      fdcCPhaseEnd ();
    }  //C-Phaseが終了した
  }  //fdcWriteCommand(int)

  //fdcWriteData (d)
  //  wb (0x00e94003, d)
  //  FDC データ/コマンド
  public static void fdcWriteData (int d) {
    if (fdcWriteHandle == null) {  //Write中でない
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("%08x fdcWriteData(0x00e94003=???,0x%02x)\n",
                           XEiJ.regPC0,
                           d & 255);
      }
      return;
    }
    if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
      if (fdcIndex < fdcStart + 8 || fdcLimit - 8 <= fdcIndex) {  //先頭8バイトまたは末尾8バイト
        System.out.printf ("%08x fdcWriteData(0x00e94003=%s[0x%08x],0x%02x)\n",
                           XEiJ.regPC0,
                           fdcWriteHandle == fdcCommandBuffer ? "fdcCommandBuffer" :
                           fdcWriteHandle == fdcTempBuffer ? "fdcTempBuffer" :
                           "fduImage",
                           fdcIndex,
                           d & 255);
      }
    }
    fdcWriteHandle[fdcIndex++] = (byte) d;
    if (fdcIndex < fdcLimit) {  //継続
      if (fdcWriteHandle != fdcCommandBuffer) {  //E-Phaseのとき
        HD63450.dmaFallREQ (0);  //DMA転送継続
      }
    } else if (fdcWriteHandle == fdcCommandBuffer) {  //C-Phaseが終了した
      fdcCPhaseEnd ();
    } else {  //E-Phaseが終了した
      fdcEPhaseEnd ();
    }
  }  //fdcWriteData(int)


  //fdcCPhase ()
  //  C-Phaseに戻る
  public static void fdcCPhase () {
    if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
      System.out.printf ("%08x fdcCPhase()\n", XEiJ.regPC0);
    }
    //コマンドが終了してC-Phaseに戻るとき
    fdcStatus = (FDC_RQM | FDC_MPU_TO_FDC |  //FDCビジー(CB)を0にする
                 (fdcStatus & (FDC_D3B | FDC_D2B | FDC_D1B | FDC_D0B)));
    if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
      System.out.printf ("\tfdcStatus=%s\n", fdcStatusToString (fdcStatus));
    }
    //C-Phaseに移行する
    fdcReadHandle = null;
    fdcWriteHandle = fdcCommandBuffer;  //C-Phase
    fdcIndex = fdcStart = 0;
    fdcLimit = 1;  //C-Phaseの1バイト目
    fdcCommandNumber = -1;  //C-Phaseの1バイト目
    if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
      System.out.printf ("\tfdcCommandNumber=%d\n", fdcCommandNumber);
    }
    for (int u = 0; u <= 3; u++) {  //ユニット0..3について
      FDUnit unit = fdcUnitArray[u];
      if (unit.fduSeekStepWaiting) {  //シークステップ待機(seekStepWaiting)が1のとき
        unit.fduSeekStepWaiting = false;  //シークステップ待機(seekStepWaiting)を0にする
        if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
          System.out.printf ("\tfduSeekStepWaiting%d=%b\n", u, unit.fduSeekStepWaiting);
        }
        unit.fduSeekStep ();  //シークステップ
      }
      if ((fdcStatus & 1 << u) != 0) {  //FDnビジー(DnB)が1のとき
        if (unit.fduSeekEndInterruptWaiting) {  //シーク終了割り込み待機(seekEndInterruptWaiting)が1のとき
          unit.fduSeekEndInterruptWaiting = false;  //シーク終了割り込み待機(seekEndInterruptWaiting)を0にする
          unit.fduSeekEndInterruptRequest = true;  //シーク終了割り込み要求(seekEndInterruptRequest)を1にする
          if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
            System.out.printf ("\tfduSeekEndInterruptWaiting%d=%b\n", u, unit.fduSeekEndInterruptWaiting);
            System.out.printf ("\tfduSeekEndInterruptRequest%d=%b\n", u, unit.fduSeekEndInterruptRequest);
          }
          IOInterrupt.ioiFdcRise ();  //FDC割り込み要求(INT)を1にする
        }
      } else {  //FDnビジー(DnB)が0のとき
        if (unit.fduAttentionCheckWaiting) {  //状態遷移確認フラグ(attentionCheckWaiting)が1のとき
          unit.fduAttentionCheckWaiting = false;  //状態遷移確認フラグ(attentionCheckWaiting)を0にする
          if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
            System.out.printf ("\tfduAttentionCheckWaiting%d=%b\n", u, unit.fduAttentionCheckWaiting);
          }
          boolean ready = unit.fduIsReady ();
          if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
            System.out.printf ("\tready%d=%b\n", u, ready);
          }
          if (ready != unit.fduSavedReady) {  //レディ信号の状態(isReady)が保存されたレディ信号の状態(savedReady)と違うとき
            unit.fduSavedReady = ready;  //レディ信号の状態(isReady)を保存する(isReady→savedReady)
            if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
              System.out.printf ("\tfduSavedReady%d=%b\n", u, unit.fduSavedReady);
            }
            if (!unit.fduAttentionInterruptRequest) {  //状態遷移割り込み要求(attentionInterruptRequest)が0のとき
              unit.fduAttentionInterruptRequest = true;  //状態遷移割り込み要求(attentionInterruptRequest)を1にする
              if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
                System.out.printf ("\tfduAttentionInterruptRequest%d=%b\n", u, unit.fduAttentionInterruptRequest);
              }
              IOInterrupt.ioiFdcRise ();  //FDC割り込み要求(INT)を1にする
            }
          }
        }
      }
    }  //for u
  }  //fdcCPhase()

  //fdcCPhaseEnd ()
  //  C-Phaseが終了した
  public static void fdcCPhaseEnd () {
    if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
      System.out.printf ("%08x fdcCPhaseEnd()\n", XEiJ.regPC0);
    }
    if (fdcCommandNumber < 0) {  //C-Phaseの1バイト目のとき
      fdcCommandNumber = fdcCommandBuffer[0] & 31;
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("\tfdcCommandNumber=%d\n", fdcCommandNumber);
      }
      fdcLimit = FDC_COMMAND_LENGTH[fdcCommandNumber];  //コマンドの長さ
      if (1 < fdcLimit) {  //2バイト以上のコマンドなのでC-Phaseを継続
        fdcStatus |= FDC_CB;  //2バイト目からCBをセットする
        if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
          System.out.printf ("\tfdcStatus=%s\n", fdcStatusToString (fdcStatus));
        }
        //外部転送要求モードでDMAの動作を開始してからFDCにコマンドが送られてくるので、
        //C-PhaseのときDMAに転送要求を出してはならない
        //HD63450.dmaFallREQ (0);  //DMA転送継続
        return;
      }
      //1バイトのコマンドなのでC-Phaseが終了した
    }
    //HD63450.dmaRiseREQ (0);
    if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
      System.out.printf ("********* %s {",
                         FDC_COMMAND_NAME[fdcCommandNumber]);
      for (int i = 0; i < fdcLimit; i++) {
        if (i > 0) {
          System.out.print (',');
        }
        System.out.printf ("0x%02x", fdcCommandBuffer[i] & 255);
      }
      System.out.println ("} ********");
    }
    FDUnit unit = fdcUnitArray[fdcCommandBuffer[1] & 3];  //ユニットを指定しないコマンドでは無意味だがエラーになることはないので問題ない
    switch (fdcCommandNumber) {
      //case 0x02:  //0x02  READ DIAGNOSTIC
      //  unit.fduCommandReadDiagnostic ();
      //  break;
    case 0x03:  //0x03  SPECIFY
      fdcCommandSpecify ();
      break;
    case 0x04:  //0x04  SENSE DEVICE STATUS
      unit.fduCommandSenseDeviceStatus ();
      break;
    case 0x05:  //0x05  WRITE DATA
      unit.fduCommandWriteData ();
      break;
    case 0x06:  //0x06  READ DATA
      unit.fduCommandReadData ();
      break;
    case 0x07:  //0x07  RECALIBRATE
      unit.fduCommandRecalibrate ();
      break;
    case 0x08:  //0x08  SENSE INTERRUPT STATUS
      fdcCommandSenseInterruptStatus ();
      break;
      //case 0x09:  //0x09  WRITE DELETED DATA
      //  unit.fduCommandWriteDeletedData ();
      //  break;
    case 0x0a:  //0x0a  READ ID
      unit.fduCommandReadId ();
      break;
      //case 0x0c:  //0x0c  READ DELETED DATA
      //  unit.fduCommandReadDeletedData ();
      //  break;
    case 0x0d:  //0x0d  WRITE ID
      unit.fduCommandWriteId ();
      break;
    case 0x0f:  //0x0f  SEEK
      unit.fduCommandSeek ();
      break;
    case 0x11:  //0x11  SCAN EQUAL
      unit.fduCommandScan ();
      break;
      //case 0x14:  //0x14  RESET STANDBY
      //  fdcCommandResetStandby ();
      //  break;
      //case 0x15:  //0x15  SET STANDBY
      //  fdcCommandSetStandby ();
      //  break;
    case 0x16:  //0x16  SOFTWARE RESET
      fdcCommandSoftwareReset ();
      break;
    case 0x19:  //0x19  SCAN LOW OR EQUAL
      unit.fduCommandScan ();
      break;
    case 0x1d:  //0x1d  SCAN HIGH OR EQUAL
      unit.fduCommandScan ();
      break;
      //case 0x00:  //0x00  INVALID
      //case 0x01:  //0x01  INVALID
      //case 0x0b:  //0x0b  INVALID
      //case 0x0e:  //0x0e  INVALID
      //case 0x10:  //0x10  INVALID
      //case 0x12:  //0x12  INVALID
      //case 0x13:  //0x13  INVALID
      //case 0x17:  //0x17  INVALID
      //case 0x18:  //0x18  INVALID
      //case 0x1a:  //0x1a  INVALID
      //case 0x1b:  //0x1b  INVALID
      //case 0x1c:  //0x1c  INVALID
      //case 0x1e:  //0x1e  INVALID
      //case 0x1f:  //0x1f  INVALID
    default:  //INVALID
      fdcCommandInvalid ();
    }
  }  //fdcCPhaseEnd()

  //fdcEPhaseEnd ()
  //  E-Phaseが終了した
  public static void fdcEPhaseEnd () {
    if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
      System.out.printf ("%08x fdcEPhaseEnd()\n", XEiJ.regPC0);
    }
    FDUnit unit = fdcUnitArray[fdcCommandBuffer[1] & 3];
    switch (unit.fduCommandNumber) {
    case 0x05:  //0x05  WRITE DATA
      unit.fduWriteDataEPhaseEnd ();
      break;
    case 0x06:  //0x06  READ DATA
      unit.fduReadDataEPhaseEnd ();
      break;
    case 0x0d:  //0x0d  WRITE ID
      unit.fduWriteIdEPhaseEnd ();
      break;
    case 0x11:  //0x11  SCAN EQUAL
      unit.fduScanEqualEPhaseEnd ();
      break;
    case 0x19:  //0x19  SCAN LOW OR EQUAL
      unit.fduScanLowOrEqualEPhaseEnd ();
      break;
    case 0x1d:  //0x1d  SCAN HIGH OR EQUAL
      unit.fduScanHighOrEqualEPhaseEnd ();
      break;
    }
  }  //fdcEPhaseEnd()

  //fdcRPhase (limit)
  //  R-Phaseに移行する
  public static void fdcRPhase (int limit) {
    if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
      System.out.printf ("%08x fdcRPhase({", XEiJ.regPC0);
      for (int i = 0; i < limit; i++) {
        if (0 < i) {
          System.out.print (',');
        }
        System.out.printf ("0x%02x", fdcResultBuffer[i] & 255);
      }
      System.out.println ("})");
    }
    fdcStatus = (FDC_RQM | FDC_FDC_TO_MPU | FDC_CB |
                 (fdcStatus & (FDC_D3B | FDC_D2B | FDC_D1B | FDC_D0B)));
    if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
      System.out.printf ("\tfdcStatus=%s\n", fdcStatusToString (fdcStatus));
    }
    fdcReadHandle = fdcResultBuffer;  //R-Phase
    fdcWriteHandle = null;
    fdcIndex = fdcStart = 0;
    fdcLimit = limit;
  }  //fdcRPhase(int)

  //fdcRPhaseEnd ()
  //  R-Phaseが終了した
  public static void fdcRPhaseEnd () {
    if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
      System.out.printf ("%08x fdcRPhaseEnd()\n", XEiJ.regPC0);
    }
    fdcCPhase ();  //C-Phaseに戻る
    //R-Phaseが終わってC-Phaseに戻るとき
    for (int u = 0; u <= 3; u++) {  //ユニット0..3について
      FDUnit unit = fdcUnitArray[u];
      if (unit.fduSeekEndInterruptRequest) {  //シーク終了割り込み要求(seekEndInterruptRequest)が1のユニットが残っているとき
        IOInterrupt.ioiFdcRise ();  //FDC割り込み要求(INT)を1にする
        break;
      }
    }
  }  //fdcRPhaseEnd()


  //fdcWriteDriveControl (d)
  //  wb (0x00e94005, d)
  //  FDD 状態(挿入|誤挿入|------)/機能(点滅|排出禁止|排出|-|選択####)
  public static void fdcWriteDriveControl (int d) {
    //0x00e94005にライトすると0x00e9c001のFDD割り込みステータスがクリアされる
    IOInterrupt.ioiFddFall ();
    if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
      System.out.printf ("%08x fdcWriteDriveControl(0x00e94005,0x%02x(BLK=%d PRV=%d EJT=%d US3=%d US2=%d US1=%d US0=%d))\n",
                         XEiJ.regPC0,
                         d & 255,
                         d >> 7 & 1,
                         d >> 6 & 1,
                         d >> 5 & 1,
                         d >> 3 & 1,
                         d >> 2 & 1,
                         d >> 1 & 1,
                         d & 1);
    }
    //bit0-3で選択されたドライブについてbit7=点滅,bit6=排出禁止,bit5=排出
    int u = Integer.numberOfTrailingZeros (d & 15);  //選択されたドライブの番号。なければ32
    if (u < 4) {
      FDUnit unit = fdcUnitArray[u];  //ユニット
      if (unit.abuConnected) {  //接続されている
        unit.fduDriveControl (d);
      }
      fdcDriveLastSelected = unit;
    } else {
      fdcDriveLastSelected = null;
    }
  }  //fdcWriteDriveControl

  //fdcWriteDriveSelect (d)
  //  wb (0x00e94007, d)
  //  FDD 選択(モータON|--|2DD|--|ドライブ##)
  public static void fdcWriteDriveSelect (int d) {
    if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
      System.out.printf ("%08x fdcWriteDriveSelect(0x00e94007,0x%02x(MT=%d 2DD=%d US=%d))\n",
                         XEiJ.regPC0,
                         d & 255,
                         d >> 7,
                         d >> 4 & 1,
                         d & 3);
    }
    IOInterrupt.ioiFddFall ();
    FDUnit unit = fdcUnitArray[d & 3];  //ユニット
    if (unit.abuConnected &&  //接続されている
        unit.fduIsInserted ()) {  //挿入されている
      unit.fduDoubleDensity = d << 31 - 4 < 0;
      if ((byte) d < 0) {  //モータON
        unit.fduMotorOn ();
      } else {  //モータOFF
        unit.fduMotorOff ();
      }
    }
  }  //fdcWriteDriveSelect

  //fdcSetEnforcedReady (enforcedReady)
  //  強制レディ状態(YM2151のCT2が1)の設定
  public static void fdcSetEnforcedReady (boolean enforcedReady) {
    fdcEnforcedReady = enforcedReady;
    if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
      System.out.printf ("%08x fdcSetEnforcedReady(%b)\n", XEiJ.regPC0, enforcedReady);
    }
  }  //fdcSetEnforcedReady(boolean)


  //fdcCommandSpecify ()
  //  0x03  SPECIFY
  //    タイマとNon-DMAモードの設定
  //  C-Phase
  //    [0]          CMD   Command           0x03=SPECIFY
  //    [1] bit7-4   SRT   Step Rate Time    Seekコマンドのステップパルス(シリンダ移動)の間隔。標準1ms単位、ミニ2ms単位。16から引く
  //        bit3-0   HUT   Head Unload Time  Read/Writeコマンド終了後のヘッドアンロードまでの時間。標準16ms単位、ミニ32ms単位。16倍する
  //    [2] bit7-1   HLT   Head Load Time    ヘッドロード後の安定待ち時間。標準2ms単位、ミニ4ms単位。2倍する
  //        bit0     ND    Non-DMA Mode      Read/WriteコマンドのE-Phaseについて0=DMAモード,1=Non-DMAモード
  public static void fdcCommandSpecify () {
    int srt = fdcCommandBuffer[1] >> 4 & 15;  //SRT
    int hut = fdcCommandBuffer[1] & 15;  //HUT
    int hlt = fdcCommandBuffer[2] >> 1 & 127;  //HLT
    int nd = fdcCommandBuffer[2] & 1;  //ND
    if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
      System.out.printf ("%08x fdcCommandSpecify(SRT=%d HUT=%d HLT=%d ND=%d)\n", XEiJ.regPC0, srt, hut, hlt, nd);
    }
    fdcSRT = srt;
    if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
      System.out.printf ("\tfdcSRT=%d\n", fdcSRT);
    }
    fdcCPhase ();  //C-Phaseに戻る
  }  //fdcCommandSpecify

  //fdcCommandSenseInterruptStatus ()
  //  0x08  SENSE INTERRUPT STATUS
  //    リザルトステータス0(ST0)の引き取り
  //  C-Phase
  //    [0]          CMD   Command          0x08=SENSE INTERRUPT STATUS
  //    [1]  bit2    HD    Head Address     サイド
  //         bit1-0  US    Unit Select      ユニット
  //  R-Phase
  //    [0]          ST0   Status 0         リザルトステータス0
  //    [1]          PCN   Present Cylinder Number  現在のシリンダ
  public static void fdcCommandSenseInterruptStatus () {
    if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
      System.out.printf ("%08x fdcCommandSenseInterruptStatus()\n", XEiJ.regPC0);
    }
    //R-Phaseに移行するとき
    IOInterrupt.ioiFdcFall ();  //FDC割り込み要求(INT)を0にする
    for (int u = 0; u <= 3; u++) {  //ユニット0..3について
      FDUnit unit = fdcUnitArray[u];
      if ((fdcStatus & 1 << u) != 0) {  //FDnビジー(DnB)が1のとき
        if (unit.fduSeekEndInterruptWaiting) {  //シーク終了割り込み待機(seekEndInterruptWaiting)が1のとき
          unit.fduSeekEndInterruptWaiting = false;  //シーク終了割り込み待機(seekEndInterruptWaiting)を0にする
          unit.fduSeekEndInterruptRequest = true;  //シーク終了割り込み要求(seekEndInterruptRequest)を1にする
          if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
            System.out.printf ("\tfduSeekEndInterruptWaiting%d=%b\n", u, unit.fduSeekEndInterruptWaiting);
            System.out.printf ("\tfduSeekEndInterruptRequest%d=%b\n", u, unit.fduSeekEndInterruptRequest);
          }
        } else {  //FDnビジー(DnB)が0のとき
          if (unit.fduAttentionCheckWaiting) {  //状態遷移確認フラグ(attentionCheckWaiting)が1のとき
            unit.fduAttentionCheckWaiting = false;  //状態遷移確認フラグ(attentionCheckWaiting)を0にする
            if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
              System.out.printf ("\tfduAttentionCheckWaiting%d=%b\n", u, unit.fduAttentionCheckWaiting);
            }
            boolean ready = unit.fduIsReady ();
            if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
              System.out.printf ("\tready%d=%b\n", u, ready);
            }
            if (ready != unit.fduSavedReady) {  //レディ信号の状態(isReady)が保存されたレディ信号の状態(savedReady)と違うとき
              unit.fduSavedReady = ready;  //レディ信号の状態(isReady)を保存する(isReady→savedReady)
              if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
                System.out.printf ("\tfduSavedReady%d=%b\n", u, unit.fduSavedReady);
              }
              if (!unit.fduAttentionInterruptRequest) {  //状態遷移割り込み要求(attentionInterruptRequest)が0のとき
                unit.fduAttentionInterruptRequest = true;  //状態遷移割り込み要求(attentionInterruptRequest)を1にする
                if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
                  System.out.printf ("\tfduAttentionInterruptRequest%d=%b\n", u, unit.fduAttentionInterruptRequest);
                }
              }
            }
          }
        }
      }
    }
    int status = FDC_ST0_IC;
    for (int u = 0; u <= 3; u++) {  //ユニット0..3について
      FDUnit unit = fdcUnitArray[u];
      if (unit.fduSeekEndInterruptRequest) {  //シーク終了割り込み要求(seekEndInterruptRequest)が1のとき
        unit.fduSeekEndInterruptRequest = false;  //シーク終了割り込み要求(seekEndInterruptRequest)を0にする
        if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
          System.out.printf ("\tfduSeekEndInterruptRequest%d=%b\n", u, unit.fduSeekEndInterruptRequest);
        }
        status = unit.fduSeekResultStatus | u << 24 | (unit.fduPCN & 255) << 16;
        break;
      }
      if (unit.fduAttentionInterruptRequest) {  //状態遷移割り込み要求(attentionInterruptRequest)が1のとき
        unit.fduAttentionInterruptRequest = false;  //状態遷移割り込み要求(attentionInterruptRequest)を0にする
        status = FDC_ST0_AI | u << 24 | (unit.fduPCN & 255) << 16;  //AI(Attention Interrupt)のリザルトステータスを出力する
        break;
      }
    }
    if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
      System.out.printf ("\tstatus=%s\n", fdcResultStatusToString (status & 0xff000000));
    }
    fdcResultBuffer[0] = (byte) (status >> 24);
    if (status == FDC_ST0_IC) {
      fdcRPhase (1);
    } else {
      fdcResultBuffer[1] = (byte) (status >> 16);
      fdcRPhase (2);
    }
  }  //fdcCommandSenseInterruptStatus()

  //fdcCommandResetStandby ()
  //  0x14  RESET STANDBY
  //    スタンバイ状態の解除
  public static void fdcCommandResetStandby () {
    if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
      System.out.printf ("%08x fdcCommandResetStandby()\n", XEiJ.regPC0);
    }
    //何もしない
    fdcCPhase ();  //C-Phaseに戻る
  }  //fdcCommandResetStandby()

  //fdcCommandSetStandby ()
  //  0x15  SET STANDBY
  //    スタンバイ状態への移行
  public static void fdcCommandSetStandby () {
    if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
      System.out.printf ("%08x fdcCommandSetStandby()\n", XEiJ.regPC0);
    }
    //何もしない
    fdcCPhase ();  //C-Phaseに戻る
  }  //fdcCommandSetStandby()

  //fdcCommandSoftwareReset ()
  //  0x16  SOFTWARE RESET
  //    FDCの初期化
  public static void fdcCommandSoftwareReset () {
    if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
      System.out.printf ("%08x fdcCommandSoftwareReset()\n", XEiJ.regPC0);
    }
    //何もしない
    fdcCPhase ();  //C-Phaseに戻る
  }  //fdcCommandSoftwareReset()

  //fdcCommandInvalid ()
  //  0x00  INVALID
  //  0x01  INVALID
  //  0x0b  INVALID
  //  0x0e  INVALID
  //  0x10  INVALID
  //  0x12  INVALID
  //  0x13  INVALID
  //  0x17  INVALID
  //  0x18  INVALID
  //  0x1a  INVALID
  //  0x1b  INVALID
  //  0x1c  INVALID
  //  0x1e  INVALID
  //  0x1f  INVALID
  //    不正なコマンドまたはSENSE INTERRUPT STATUSの不正使用
  public static void fdcCommandInvalid () {
    if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
      System.out.printf ("%08x fdcCommandInvalid()\n", XEiJ.regPC0);
    }
    int status = FDC_ST0_IC;
    if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
      System.out.printf ("\tstatus=%s\n", fdcResultStatusToString (status));
    }
    fdcResultBuffer[0] = (byte) (status >> 24);  //ST0
    fdcRPhase (1);
  }  //fdcCommandInvalid


  public static String fdcResultStatusToString (int status) {
    int ic = status >> 24 + 6 & 3;
    return String.format ("0x%08x\n" +
                          "\t\tST0=0x%02x(IC=%d(%s) SE=%d EC=%d NR=%d HD=%d US=%d)\n" +
                          "\t\tST1=0x%02x(EN=%d DE=%d OR=%d ND=%d NW=%d MA=%d)\n" +
                          "\t\tST2=0x%02x(CM=%d DD=%d NC=%d SH=%d SN=%d BC=%d MD=%d)",
                          status,
                          status >> 24 & 255,
                          ic, ic == 0 ? "NT" : ic == 1 ? "AT" : ic == 2 ? "IC" : "AI",  //IC
                          status >> 24 + 5 & 1,
                          status >> 24 + 4 & 1,
                          status >> 24 + 3 & 1,
                          status >> 24 + 2 & 1,
                          status >> 24 + 0 & 3,
                          status >> 16 & 255,
                          status >> 16 + 7 & 1,
                          status >> 16 + 5 & 1,
                          status >> 16 + 4 & 1,
                          status >> 16 + 2 & 1,
                          status >> 16 + 1 & 1,
                          status >> 16 + 0 & 1,
                          status >> 8 & 255,
                          status >> 8 + 6 & 1,
                          status >> 8 + 5 & 1,
                          status >> 8 + 4 & 1,
                          status >> 8 + 3 & 1,
                          status >> 8 + 2 & 1,
                          status >> 8 + 1 & 1,
                          status >> 8 + 0 & 1);
  }  //fdcResultStatusToString(int)

  //fdcStatusToString (status)
  public static String fdcStatusToString (int status) {
    return String.format ("0x%02x(RQM=%d DIO=%d(%s) NDM=%d CB=%d D3B=%d D2B=%d D1B=%d D0B=%d)",
                          status,
                          status >> 7,
                          status >> 6 & 1,
                          (status >> 6 & 1) == 0 ? "MPU->FDC" : "FDC->MPU",
                          status >> 5 & 1,
                          status >> 4 & 1,
                          status >> 3 & 1,
                          status >> 2 & 1,
                          status >> 1 & 1,
                          status & 1);
  }  //fdcStatusToString(int)



  //========================================================================================
  //$$FDU FDユニット
  //  フロッピーディスクのユニット
  //
  public static class FDUnit extends AbstractUnit {

    public FDMedia fduMedia;  //メディアの種類
    public byte[] fduImage;  //イメージ
    public boolean fduWritten;  //true=書き込みがあった

    public int fduCommandNumber;  //fdcCommandNumberのコピー。シークを伴うコマンドのE-Phase以降で使う

    //  (接続フラグ)
    //    ユニットが接続されているときON、接続されていないときOFF
    //public boolean fduIsConnected {
    //  return abuConnected;
    //}  //fduIsConnected()

    //  (挿入フラグ)
    //    ドライブステータス(0x00e94005)のReadのbit7。0=OFF,1=ON
    //    接続フラグがON、かつ、
    //    ディスクイメージファイルの読み込みが完了している、かつ、
    //    メディアの種類の判別が完了している
    public boolean fduIsInserted () {
      return abuConnected && abuInserted && fduMedia != null;
    }  //fduIsInserted()

    //  (誤挿入フラグ)
    //    ドライブステータス(0x00e94005)のReadのbit6。0=OFF,1=ON
    //    使用しない。常にOFF

    //  点滅設定フラグ
    //    ドライブコントロール(0x00e94005)のWriteのbit7。0=OFF,1=ON
    public boolean fduBlinking;  //true=点滅が設定されている

    //  排出禁止設定フラグ
    //    ドライブコントロール(0x00e94005)のWriteのbit6。0=OFF,1=ON
    public boolean fduPrevented;  //true=排出禁止が設定されている

    //  排出要求フラグ
    //    ドライブコントロール(0x00e94005)のWriteのbit5。0=OFF,1=ON

    //  モータON設定フラグ
    //    ドライブセレクト(0x00e94007)のWriteのbit7。0=OFF,1=ON

    //  2DD設定フラグ
    //    ドライブセレクト(0x00e94007)のWriteのbit5。0=OFF,1=ON
    public boolean fduDoubleDensity;  //true=2DD設定

    //  書き込み禁止フラグ
    //    デバイスステータス(ST3)のbit6。0=書き込み許可,1=書き込み禁止
    public boolean fduIsWriteProtected () {  //true=書き込み禁止
      return abuWriteProtected;
    }  //fduIsWriteProtected()

    //ready = fduIsReady ()
    //  レディ信号の状態
    //  デバイスステータス(ST3)のbit5
    //    0  ノットレディ
    //    1  レディ。強制レディ状態または接続されていてメディアが挿入されていてモータ動作中またはモータ減速中
    public boolean fduIsReady () {
      return (fdcEnforcedReady ||  //強制レディ状態または
              (abuConnected &&  //接続されていて
               fduIsInserted () &&  //メディアが挿入されていて
               (fduMotorStatus == FDU_MOTOR_RUN_SOON ||  //モータ動作前(レディ→割り込み要求)または
                fduMotorStatus == FDU_MOTOR_DECELERATING ||  //モータ減速中(レディ→ノットレディ)または
                fduMotorStatus == FDU_MOTOR_RUNNING)));  //モータ動作中(レディ)
    }  //fduIsReady()

    //  アクセスランプ
    //    消灯    挿入フラグがOFF、かつ、点滅設定フラグがOFF
    //    緑点滅  挿入フラグがOFF、かつ、点滅設定フラグがON
    //    緑点灯  挿入フラグがON、かつ、ユニットビジーフラグがOFF
    //    赤点灯  挿入フラグがON、かつ、ユニットビジーフラグがON
    public static final int FDU_ACCESS_OFF            = 0;
    public static final int FDU_ACCESS_GREEN_BLINKING = 1;
    public static final int FDU_ACCESS_GREEN_ON       = 2;
    public static final int FDU_ACCESS_RED_ON         = 3;

    //  イジェクトランプ
    //    消灯    挿入フラグがOFF
    //    消灯    挿入フラグがON、かつ、排出禁止フラグがON
    //    緑点灯  挿入フラグがON、かつ、排出禁止フラグがOFF
    public static final int FDU_EJECT_OFF      = 0;
    public static final int FDU_EJECT_GREEN_ON = 1;


    //シーク
    public static final long FDU_SEEK_INTERVAL = XEiJ.TMR_FREQ * 1 / 1000;  //1ms。シークティッカーの動作間隔
    public int fduNCN;  //NCN 目標シリンダ番号(NCNn)
    public int fduPCN;  //PCN Present Cylinder Number シリンダ番号(PCNn)。Track0信号=fduPCN==0?1:0
    public int fduPHN;  //PHN Present Head Number サイド番号。0～1。2HDEは最初のセクタを除いて128～129
    public int fduPRN;  //PRN Present Record Number セクタ番号。1～1トラックあたりのセクタ数。2HSは最初のセクタを除いて10～18
    public int fduPNN;  //PNN Present Record Length セクタ長。0～7
    public int fduEOT;  //End of Track。終了セクタ
    public int fduSTP;  //Step。1=R++,2=R+=2
    public int fduSRC;  //ステップレートカウンタ。初期値は16-SRT
    public boolean fduSeekStepWaiting;  //シークステップ待機(seekStepWaiting)
    public boolean fduSeekEndInterruptWaiting;  //シーク終了割り込み待機(seekEndInterruptWaiting)
    public boolean fduSeekEndInterruptRequest;  //シーク終了割り込み要求(seekEndInterruptRequest)
    public int fduSeekResultStatus;  //シーク終了割り込みでSENSE INTERRUPT STATUSが返すリザルトステータス

    //fduSeekTicker
    //  シークティッカー
    public final TickerQueue.Ticker fduSeekTicker = new TickerQueue.Ticker () {
      @Override protected void tick () {
        if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
          System.out.printf ("%08x fduSeekTicker%d.tick()\n", XEiJ.regPC0, abuNumber);
        }
        if ((fdcStatus & 1 << abuNumber) != 0) {  //FDnビジー(DnB)が1のとき
          if ((fdcStatus & FDC_CB) != 0) {  //FDCビジー(CB)が1のとき
            fduSeekStepWaiting = true;  //シークステップ待機(seekStepWaiting)を1にする
            if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
              System.out.printf ("\tfduSeekStepWaiting%d=%b\n", abuNumber, fduSeekStepWaiting);
            }
          } else {  //FDCビジー(CB)が0のとき
            fduSeekStep ();  //シークステップ
          }
        } else {  //FDnビジー(DnB)が0のとき
          fduSeekStep ();  //シークステップ
        }
      }  //tick()
    };  //fduSeekTicker

    //fduSeekStep ()
    //  シークステップ
    public void fduSeekStep () {
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("%08x fduSeekStep%d()\n", XEiJ.regPC0, abuNumber);
      }
      if (fduPCN != fduNCN) {  //シリンダ番号(PCNn)と目標シリンダ番号(NCNn)が違うとき
        fduSRC--;  //ステップレートカウンタ(SRC)をデクリメントする
        if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
          System.out.printf ("\tfduSRC%d=%d\n", abuNumber, fduSRC);
        }
        if (fduSRC == 0) {  //ステップレートカウンタ(SRC)が0になったとき
          fduSRC = 16 - fdcSRT;  //ステップレートカウンタ(SRC)を16-SRTにする
          if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
            System.out.printf ("\tfduSRC%d=%d\n", abuNumber, fduSRC);
          }
          if (fduPCN < fduNCN) {  //シリンダ番号(PCNn)が目標シリンダ番号(NCNn)よりも小さいとき
            fduPCN++;  //シリンダ番号(PCNn)をインクリメントする
            if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
              System.out.printf ("\tfduPCN%d=%d\n", abuNumber, fduPCN);
            }
          } else {  //シリンダ番号(PCNn)が目標シリンダ番号(NCNn)よりも大きいとき
            fduPCN--;  //シリンダ番号(PCNn)をデクリメントする
            if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
              System.out.printf ("\tfduPCN%d=%d\n", abuNumber, fduPCN);
            }
          }
        }  //if fduSRC==0
        TickerQueue.tkqAdd (fduSeekTicker, XEiJ.mpuClockTime + FDU_SEEK_INTERVAL);  //シークティッカーを1ms後に予約する
        return;
      }
      //シーク終了
      //E-Phase
      switch (fduCommandNumber) {
        //case 0x02:  //READ DIAGNOSTIC
        //  fduReadDiagnosticEPhase ();
        //  break;
      case 0x05:  //WRITE DATA
        fduWriteDataEPhase ();
        break;
      case 0x06:  //READ DATA
        fduReadDataEPhase ();
        break;
      case 0x07:  //RECALIBRATE
      case 0x0f:  //SEEK
        //SEEK/RECALIBRATEコマンドのとき
        fduSeekEnd ();  //SEEK/RECALIBRATEコマンドのE-Phase(シーク中)が終了したとき
        break;
        //case 0x09:  //WRITE DELETED DATA
        //  fduWriteDeletedDataEPhase ();
        //  break;
        //case 0x0c:  //READ DELETED DATA
        //  fduReadDeletedDataEPhase ();
        //  break;
      case 0x11:  //SCAN EQUAL
      case 0x19:  //SCAN LOW OR EQUAL
      case 0x1d:  //SCAN HIGH OR EQUAL
        fduScanEPhase ();
        break;
      }
    }  //fduSeekStep()

    //fduSeekEnd ()
    //  SEEK/RECALIBRATEコマンドのE-Phase(シーク中)が終了したとき
    public void fduSeekEnd () {
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("%08x fduSeekEnd%d()\n", XEiJ.regPC0, abuNumber);
      }
      if ((fdcStatus & FDC_CB) != 0) {  //FDCビジー(CB)が1のとき
        fduSeekEndInterruptWaiting = true;  //シーク終了割り込み待機(seekEndInterruptWaiting)を1にする
        if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
          System.out.printf ("\tfduSeekEndInterruptWaiting%d=%b\n", abuNumber, fduSeekEndInterruptWaiting);
        }
      } else {  //FDCビジー(CB)が0のとき
        if (!fduSeekEndInterruptRequest) {  //シーク終了割り込み要求(seekEndInterruptRequest)が0のとき
          fduSeekEndInterruptRequest = true;  //シーク終了割り込み要求(seekEndInterruptRequest)を1にする
          if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
            System.out.printf ("\tfduSeekEndInterruptRequest%d=%b\n", abuNumber, fduSeekEndInterruptRequest);
          }
          IOInterrupt.ioiFdcRise ();  //FDC割り込み要求(INT)を1にする
        }
      }
    }  //fduSeekEnd()


    //モータ
    //
    //  モータON/OFFのFDC割り込み
    //    0x00e94001==0x80
    //    RQM=1なので0x00e94003=0x08(SENSE INTERRUPT STATUS)
    //    0x00e94001==0x00  RQM=0,DIO=0(OUT),NDM=0,CB=0,D3B=0,D2B=0,D1B=0,D0B=0 x 2回～3回
    //    0x00e94001==0x10  RQM=0,DIO=0(OUT),NDM=0,CB=1,D3B=0,D2B=0,D1B=0,D0B=0 x 2回
    //    0x00e94001==0xd0  RQM=1,DIO=1(IN),NDM=0,CB=1,D3B=0,D2B=0,D1B=0,D0B=0
    //    0x00e94003==0xc0(モータ動作中のとき)  IC=3(AI状態遷移)
    //                0xc8(モータ停止中のとき)  IC=3(AI状態遷移),NR=1(ノットレディ)
    //    0x00e94003==0x00  PCN
    //    0x00e94001==0x80
    //
    //  謎
    //    X68030実機で2HDディスクを入れたFDD0のモータをONにすると、FDD0だけでなく、メディアが入っていないFDD1と、
    //    接続されていないFDD2とFDD3もレディ状態になったことを知らせる、4個の状態遷移割り込みステータスが出力される
    //    OFFも同様
    //    これはドライブの選択をPEDECが行うのでuPD72065 FDCがREADY信号をポーリングするときドライブを選択できないことが原因
    //    https://twitter.com/moveccr/status/1518985253049991169
    //
    //  モータON
    //      FDU_MOTOR_SLEEPING  モータ停止中(ノットレディ)
    //      ↓
    //    fdcMotorOn  モータON
    //      FDU_MOTOR_ACCELERATING  モータ加速中(ノットレディ→レディ)
    //      ↓
    //      ↓  FDU_MOTOR_ON_DELAY  モータ加速中(ノットレディ→レディ)
    //      ↓
    //    fduMotorAcceleratingTicker  モータ加速中ティッカー(ノットレディ→レディ)
    //      FDU_MOTOR_RUN_SOON  モータ動作前(レディ→割り込み要求)
    //      READY=1
    //      ↓
    //      ↓  FDU_MOTOR_INTERRUPT_DELAY  レディ→割り込み要求
    //      ↓
    //    fduMotorRunSoonTicker  モータ動作前ティッカー(レディ→割り込み要求)
    //      FDU_MOTOR_RUNNING  モータ動作中(レディ)
    //      状態遷移割り込み要求
    //
    //  モータOFF
    //      FDU_MOTOR_RUNNING  モータ動作中(レディ)
    //      ↓
    //    fdcMotorOff  モータOFF
    //      FDU_MOTOR_DECELERATING  モータ減速中(レディ→ノットレディ)
    //      ↓
    //      ↓  FDU_MOTOR_OFF_DELAY  レディ→ノットレディ
    //      ↓
    //    fduMotorDeceleratingTicker  モータ減速中ティッカー(レディ→ノットレディ)
    //      FDU_MOTOR_SLEEP_SOON  モータ停止前(ノットレディ→割り込み要求)
    //      READY=0
    //      ↓
    //      ↓  FDU_MOTOR_INTERRUPT_DELAY  ノットレディ→割り込み要求
    //      ↓
    //    fduMotorSleepSoonTicker  モータ停止前ティッカー(ノットレディ→割り込み要求)
    //      FDU_MOTOR_SLEEPING  モータ停止中(ノットレディ)
    //      状態遷移割り込み要求
    //
    //  メモ
    //    IOCSはモータONしてからSENSE DEVICE STATUSコマンドを繰り返してST3のREADYが1になるのを待つ
    //    このとき2バイトのコマンドを出力する間だけ割り込みを禁止している
    //    ST3のREADYが1になるのと同時に状態遷移割り込みを要求すると、
    //    2バイトのコマンドを出力している間にFDC割り込みが要求が発生し、
    //    コマンドの出力が終わって割り込みが許可されるとすぐに割り込み処理が開始されてしまうことがある
    //    その時点でR-Phaseになっているので割り込みハンドラが状態遷移割り込みを転送終了割り込みと誤認して、
    //    SENSE DEVICE STATUSコマンドのST3を転送コマンドのST0として回収してしまう
    //    割り込み処理が終了してSENSE DEVICE STATUSコマンドのST3を読み出そうとしたときにはC-Phaseになっており、
    //    SENSE DEVICE STATUSコマンドのR-Phaseを待つループから抜けられなくなってハングアップする
    //      FDC                     MPU
    //      C-Phase                 割り込み禁止
    //      状態遷移割り込み要求    SENSE DEVICE STATUSコマンドを出力する
    //      R-Phase                 割り込み許可
    //                              割り込み処理開始
    //                              SENSE DEVICE STATUSコマンドのST3を転送コマンドのST0として回収してしまう
    //      C-Phase                 割り込み処理終了
    //                              SENSE DEVICE STATUSコマンドのR-Phaseを待つループから抜けられなくなる
    //    実機はポーリングでREADYを監視しているので通常はREADYが1になってから割り込みが発生するまで少し間が空く
    //    最小間隔が0だと実機でも同じ問題が発生するはずだが見たことがないので最小間隔は0よりも大きいと考えられる
    //
    //    +------------------+--------------------------------------------------------+
    //    |  イジェクト許可  |                     イジェクト禁止                     |
    //    +------------------+------------------+-------------------------------------+
    //    |             ノットレディ            |                レディ               |
    //    +-------------------------------------+-------------------------------------+
    //    |                 ON                 ON                 ON                  |
    //    |                 →   ACCELERATING  →     RUN_SOON    →                  |
    //    |     SLEEPING         OFF↓  ↑ON        OFF↓  ↑ON          RUNNING      |
    //    |                 ←    SLEEP_SOON   ←   DECELERATING  ←                  |
    //    |                 OFF                OFF                OFF                 |
    //    +---------------------------------------------------------------------------+
    //    SLEEPINGからのONでACCELERATINGに移行するが、DECELERATINGからのONでACCELERATINGに移行してしまうと、
    //    RUNNINGからOFFされてDECELERATINGになった直後にONされたときノットレディの時間ができてしまう
    //    それではDECELERATINGの時間を設けてノットレディを遅延させている意味がない
    //    DECELERATINGからのONはRUN_SOONに移行しなければならない
    //
    public static final int FDU_MOTOR_SLEEPING     = 0;  //モータ停止中(ノットレディ)。イジェクト可
    public static final int FDU_MOTOR_ACCELERATING = 1;  //モータ加速中(ノットレディ→レディ)。イジェクト不可
    public static final int FDU_MOTOR_SLEEP_SOON   = 2;  //モータ停止前(ノットレディ→割り込み要求)。イジェクト不可
    public static final int FDU_MOTOR_RUN_SOON     = 3;  //モータ動作前(レディ→割り込み要求)。イジェクト不可
    public static final int FDU_MOTOR_DECELERATING = 4;  //モータ減速中(レディ→ノットレディ)。イジェクト不可
    public static final int FDU_MOTOR_RUNNING      = 5;  //モータ動作中(レディ)。イジェクト不可
    public int fduMotorStatus;  //モータの状態
    public static final long FDU_MOTOR_ON_DELAY        = XEiJ.TMR_FREQ * 100 / 1000000;  //100us。モータ加速中(ノットレディ→レディ)
    public static final long FDU_MOTOR_OFF_DELAY       = XEiJ.TMR_FREQ * 3;  //3s。モータ減速中(レディ→ノットレディ)
    public static final long FDU_MOTOR_INTERRUPT_DELAY = XEiJ.TMR_FREQ * 50 / 1000000;  //50us。割り込み待ち
    public boolean fduSavedReady;  //保存されたレディ信号の状態(savedReady)
    public boolean fduAttentionCheckWaiting;  //状態遷移確認フラグ(attentionCheckWaiting)
    public boolean fduAttentionInterruptRequest;  //状態遷移割り込み要求(attentionInterruptRequest)

    //fduMotorOn ()
    //  モータON
    public void fduMotorOn () {
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("%08x fduMotorOn%d()\n", XEiJ.regPC0, abuNumber);
      }
      if (!fduIsInserted ()) {  //メディアが挿入されていないとき
        return;  //何もしない
      }
      switch (fduMotorStatus) {
      case FDU_MOTOR_SLEEPING:  //モータ停止中(ノットレディ)のとき
        fduMotorStatus = FDU_MOTOR_ACCELERATING;  //モータ加速中(ノットレディ→レディ)にする
        if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
          System.out.printf ("\tfduMotorStatus%d=%d\n", abuNumber, fduMotorStatus);
        }
        if (!fduPrevented) {  //排出禁止設定でないとき
          prevent ();  //排出を禁止する
        }
        TickerQueue.tkqAdd (fduMotorAcceleratingTicker, XEiJ.mpuClockTime + FDU_MOTOR_ON_DELAY);  //モータ加速中ティッカー(ノットレディ→レディ)を予約する
        break;
      case FDU_MOTOR_SLEEP_SOON:  //モータ停止前(ノットレディ→割り込み要求)のとき
        TickerQueue.tkqRemove (fduMotorSleepSoonTicker);  //モータ停止前ティッカー(ノットレディ→割り込み要求)を取り消す
        fduMotorStatus = FDU_MOTOR_ACCELERATING;  //モータ加速中(ノットレディ→レディ)にする
        if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
          System.out.printf ("\tfduMotorStatus%d=%d\n", abuNumber, fduMotorStatus);
        }
        TickerQueue.tkqAdd (fduMotorAcceleratingTicker, XEiJ.mpuClockTime + FDU_MOTOR_ON_DELAY);  //モータ加速中ティッカー(ノットレディ→レディ)を予約する
        break;
      case FDU_MOTOR_DECELERATING:  //モータ減速中(レディ→ノットレディ)のとき
        TickerQueue.tkqRemove (fduMotorDeceleratingTicker);  //モータ減速中ティッカー(レディ→ノットレディ)を取り消す
        fduMotorStatus = FDU_MOTOR_RUN_SOON;  //モータ動作前(レディ→割り込み要求)にする
        if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
          System.out.printf ("\tfduMotorStatus%d=%d\n", abuNumber, fduMotorStatus);
        }
        TickerQueue.tkqAdd (fduMotorRunSoonTicker, XEiJ.mpuClockTime + FDU_MOTOR_INTERRUPT_DELAY);  //モータ動作前ティッカー(レディ→割り込み要求)を予約する
        break;
      }
    }  //fduMotorOn()

    //fduMotorAcceleratingTicker
    //  モータ加速中ティッカー(ノットレディ→レディ)
    public final TickerQueue.Ticker fduMotorAcceleratingTicker = new TickerQueue.Ticker () {
      @Override protected void tick () {
        if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
          System.out.printf ("%08x fduMotorAcceleratingTicker%d.tick()\n", XEiJ.regPC0, abuNumber);
        }
        fduMotorStatus = FDU_MOTOR_RUN_SOON;  //モータ動作前(レディ→割り込み要求)にする
        if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
          System.out.printf ("\tfduMotorStatus%d=%d\n", abuNumber, fduMotorStatus);
        }
        //(強制レディ状態でなければここでレディになる)
        TickerQueue.tkqAdd (fduMotorRunSoonTicker, XEiJ.mpuClockTime + FDU_MOTOR_INTERRUPT_DELAY);  //モータ動作前ティッカー(レディ→割り込み要求)を予約する
      }  //tick()
    };  //fduMotorAcceleratingTicker

    //fduMotorRunSoonTicker
    //  モータ動作前ティッカー(レディ→割り込み要求)
    public final TickerQueue.Ticker fduMotorRunSoonTicker = new TickerQueue.Ticker () {
      @Override protected void tick () {
        if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
          System.out.printf ("%08x fduMotorRunSoonTicker%d.tick()\n", XEiJ.regPC0, abuNumber);
        }
        fduMotorStatus = FDU_MOTOR_RUNNING;  //モータ動作中(レディ)にする
        if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
          System.out.printf ("\tfduMotorStatus%d=%d\n", abuNumber, fduMotorStatus);
        }
        fduMotorInterrupt ();  //モータONまたはモータOFFから一定の時間が経ったとき
      }  //tick()
    };  //fduMotorRunSoonTicker

    //fduMotorOff ()
    //  モータOFF
    @SuppressWarnings ("fallthrough") public void fduMotorOff () {
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("%08x fduMotorOff%d()\n", XEiJ.regPC0, abuNumber);
      }
      if (!fduIsInserted ()) {  //メディアが挿入されていないとき
        return;  //何もしない
      }
      switch (fduMotorStatus) {
      case FDU_MOTOR_ACCELERATING:  //モータ加速中(ノットレディ→レディ)のとき
        TickerQueue.tkqRemove (fduMotorAcceleratingTicker);  //モータ加速中ティッカー(ノットレディ→レディ)を取り消す
        fduMotorStatus = FDU_MOTOR_SLEEP_SOON;  //モータ停止前(ノットレディ→割り込み要求)にする
        if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
          System.out.printf ("\tfduMotorStatus%d=%d\n", abuNumber, fduMotorStatus);
        }
        TickerQueue.tkqAdd (fduMotorSleepSoonTicker, XEiJ.mpuClockTime + FDU_MOTOR_INTERRUPT_DELAY);  //モータ停止前ティッカー(ノットレディ→割り込み要求)を予約する
        break;
      case FDU_MOTOR_RUN_SOON:  //モータ動作前(レディ→割り込み要求)のとき
        TickerQueue.tkqRemove (fduMotorRunSoonTicker);  //モータ動作前ティッカー(レディ→割り込み要求)を取り消す
        //fallthrough
      case FDU_MOTOR_RUNNING:  //モータ動作中(レディ)のとき
        fduMotorStatus = FDU_MOTOR_DECELERATING;  //モータ減速中(レディ→ノットレディ)にする
        if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
          System.out.printf ("\tfduMotorStatus%d=%d\n", abuNumber, fduMotorStatus);
        }
        TickerQueue.tkqAdd (fduMotorDeceleratingTicker, XEiJ.mpuClockTime + FDU_MOTOR_OFF_DELAY);  //モータ減速中ティッカー(レディ→ノットレディ)を予約する
        break;
      }
    }  //fduMotorOff()

    //fduMotorDeceleratingTicker
    //  モータ減速中ティッカー(レディ→ノットレディ)
    public final TickerQueue.Ticker fduMotorDeceleratingTicker = new TickerQueue.Ticker () {
      @Override protected void tick () {
        if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
          System.out.printf ("%08x fduMotorDeceleratingTicker%d.tick()\n", XEiJ.regPC0, abuNumber);
        }
        fduMotorStatus = FDU_MOTOR_SLEEP_SOON;  //モータ停止前(ノットレディ→割り込み要求)にする
        if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
          System.out.printf ("\tfduMotorStatus%d=%d\n", abuNumber, fduMotorStatus);
        }
        //(強制レディ状態でなければここでノットレディになる)
        TickerQueue.tkqAdd (fduMotorSleepSoonTicker, XEiJ.mpuClockTime + FDU_MOTOR_INTERRUPT_DELAY);  //モータ停止前ティッカー(ノットレディ→割り込み要求)を予約する
      }  //tick()
    };  //fduMotorDeceleratingTicker

    //fduMotorSleepSoonTicker
    //  モータ停止前ティッカー(ノットレディ→割り込み要求)
    public final TickerQueue.Ticker fduMotorSleepSoonTicker = new TickerQueue.Ticker () {
      @Override protected void tick () {
        if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
          System.out.printf ("%08x fduMotorSleepSoonTicker%d.tick()\n", XEiJ.regPC0, abuNumber);
        }
        fduMotorStatus = FDU_MOTOR_SLEEPING;  //モータ停止中(ノットレディ)にする
        if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
          System.out.printf ("\tfduMotorStatus%d=%d\n", abuNumber, fduMotorStatus);
        }
        if (!fduPrevented) {  //排出禁止設定でないとき
          allow ();  //排出を許可する
        }
        fduMotorInterrupt ();  //モータONまたはモータOFFから一定の時間が経ったとき
      }  //tick()
    };  //fduMotorSleepSoonTicker

    //fduMotorInterrupt ()
    //  モータONまたはモータOFFから一定の時間が経ったとき
    public void fduMotorInterrupt () {
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("%08x fduMotorInterrupt%d()\n", XEiJ.regPC0, abuNumber);
      }
      if ((fdcStatus & 1 << abuNumber) != 0) {  //FDnビジー(DnB)が1のとき
        fduAttentionCheckWaiting = true;  //状態遷移確認フラグ(attentionCheckWaiting)を1にする
        if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
          System.out.printf ("\tfduAttentionCheckWaiting%d=%b\n", abuNumber, fduAttentionCheckWaiting);
        }
      } else {  //FDnビジー(DnB)が0のとき
        if ((fdcStatus & FDC_CB) != 0) {  //FDCビジー(CB)が1のとき
          fduAttentionCheckWaiting = true;  //状態遷移確認フラグ(attentionCheckWaiting)を1にする
          if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
            System.out.printf ("\tfduAttentionCheckWaiting%d=%b\n", abuNumber, fduAttentionCheckWaiting);
          }
        } else {  //FDCビジー(CB)が0のとき
          boolean ready = fduIsReady ();
          if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
            System.out.printf ("\tready%d=%b\n", abuNumber, ready);
          }
          if (ready != fduSavedReady) {  //レディ信号の状態(isReady)が保存されたレディ信号の状態(savedReady)と違うとき
            fduSavedReady = ready;  //レディ信号の状態(isReady)を保存する(isReady→RPYn)
            if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
              System.out.printf ("\tfduSavedReady%d=%b\n", abuNumber, fduSavedReady);
            }
            if (!fduAttentionInterruptRequest) {  //状態遷移割り込み要求(attentionInterruptRequest)が0のとき
              fduAttentionInterruptRequest = true;  //状態遷移割り込み要求(attentionInterruptRequest)を1にする
              if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
                System.out.printf ("\tfduAttentionInterruptRequest%d=%b\n", abuNumber, fduAttentionInterruptRequest);
              }
              IOInterrupt.ioiFdcRise ();  //FDC割り込み要求(INT)を1にする
            }
          }
        }
      }
    }  //fduMotorInterrupt()


    //コンストラクタ

    //new FDUnit (number)
    //  コンストラクタ
    public FDUnit (int number) {
      super (number);

      fduMedia = null;
      fduImage = null;
      fduWritten = false;

      fduBlinking = false;
      fduPrevented = false;
      fduDoubleDensity = false;

      fduNCN = 0;
      fduPCN = 0;
      fduPHN = 0;
      fduPRN = 1;
      fduPNN = 3;
      fduEOT = 1;
      fduSTP = 1;
      fduSeekStepWaiting = false;
      fduSeekEndInterruptWaiting = false;
      fduSeekEndInterruptRequest = false;
      fduSeekResultStatus = FDC_ST0_IC;

      fduMotorStatus = FDU_MOTOR_SLEEPING;  //モータ停止中(ノットレディ)
      fduSavedReady = false;
      fduAttentionCheckWaiting = false;
      fduAttentionInterruptRequest = false;
    }

    //fduTini ()
    //  後始末
    //  イメージファイルに書き出す
    public void fduTini () {
      if (fduIsInserted ()) {
        fduFlush ();
      }
    }  //fduTini()

    //success = unit.fduFlush ()
    //  イメージファイルに書き出す
    public boolean fduFlush () {
      if (!abuConnected ||  //接続されていない
          !fduIsInserted () ||  //挿入されていない
          !fduWritten) {  //書き込みがない
        return true;
      }
      if (fduIsWriteProtected ()) {  //書き込みが許可されていない
        return false;
      }
      int dotIndex = abuPath.lastIndexOf ('.');
      String upperExt = dotIndex < 0 ? "" : abuPath.substring (dotIndex + 1).toUpperCase ();
      if (upperExt.equals ("DIM")) {  // *.dim
        byte[] dimImage = new byte[256 + fduMedia.fdmBytesPerDisk];
        int dimSize = fduMedia.fdmMakeDimImage (dimImage, fduImage);  // *.DIMのイメージを作る
        if (dimSize < 0) {
          // *.DIMから読み込んだがFORMAT.XなどでDIMにできない形式に変更されてしまった
          XEiJ.pnlExitFullScreen (true);
          JOptionPane.showMessageDialog (null,
                                         Multilingual.mlnJapanese ?
                                         fduMedia.fdmName + " を *.DIM に変換できません" :
                                         fduMedia.fdmName + " cannot be converted to *.DIM");
          return false;
        }
        if (!XEiJ.rscPutFile (abuPath, dimImage, 0, dimSize)) {  //保存する
          return false;
        }
      } else {  // *.DIM以外
        if (!XEiJ.rscPutFile (abuPath, fduImage, 0, fduMedia.fdmBytesPerDisk)) {
          return false;
        }
      }
      fduWritten = false;
      return true;
    }  //fduFlush()

    //unit.connect (disconnectable)
    //  接続する
    @Override protected void connect (boolean disconnectable) {
      super.connect (disconnectable);
      fduImage = new byte[FDMedia.FDM_BUFFER_SIZE];
    }

    //unit.disconnect ()
    //  切り離す
    @Override protected void disconnect () {
      super.disconnect ();
      fduImage = null;
    }

    //unit.blink ()
    //  挿入されていないときLEDを点滅させる
    public void blink () {
      if (!abuConnected ||  //接続されていない
          fduBlinking) {  //既にLEDが点滅している
        return;
      }
      fduBlinking = true;
      //! 表示なし
    }

    //unit.darken ()
    //  挿入されていないときLEDを消す
    public void darken () {
      if (!abuConnected ||  //接続されていない
          !fduBlinking) {  //LEDが点滅していない
        return;
      }
      fduBlinking = false;
      //! 表示なし
    }

    //success = unit.eject ()
    //  イジェクトする
    @Override protected boolean eject () {
      if (!fduFlush ()) {  //イメージファイルに書き出す
        return false;
      }
      fduMotorStatus = FDU_MOTOR_SLEEPING;  //モータ停止中(ノットレディ)にする
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("\tfduMotorStatus%d=%d\n", abuNumber, fduMotorStatus);
      }
      boolean inserted = fduIsInserted ();
      String path = abuPath;  //イジェクトされたイメージファイルのパス。super.eject()を呼び出す前にコピーすること
      if (!super.eject ()) {  //イジェクトする
        return false;
      }
      if (fduMedia != null) {  //挿入されていたとき
        fdcAddHistory (new File (path).getAbsoluteFile ());
        System.out.println (Multilingual.mlnJapanese ?
                            path + " を fd" + abuNumber + " から取り出しました" :
                            path + " was ejected from fd" + abuNumber);
      }
      fduMedia = null;
      fduPCN = 0;
      fduPHN = 0;
      fduPRN = 1;
      fduPNN = 3;
      //イジェクトされたときFDD割り込みを要求する
      if (inserted) {
        IOInterrupt.ioiFddFall ();
        IOInterrupt.ioiFddRise ();
      }
      return true;
    }

    //success = unit.open ()
    //  開くダイアログを開く
    @Override protected boolean open () {
      if (!super.open ()) {
        return false;
      }
      fdcOpenUnit = abuNumber;
      if (fdcOpenDialog == null) {
        fdcOpenDialog = new OpenDialog ();
        fdcOpenDialog.setReadOnly (Settings.sgsGetOnOff ("fdreadonly"));
        fdcOpenDialog.setReboot (Settings.sgsGetOnOff ("fdappreboot"));
        for (File[] files : fdcOpenHistory) {
          fdcOpenDialog.addHistory (files);
        }
        fdcOpenHistory.clear ();
      }
      fdcOpenDialog.rescanCurrentDirectory ();  //挿入されているファイルが変わると選択できるファイルも変わるのでリストを作り直す
      XEiJ.pnlExitFullScreen (true);
      fdcOpenDialog.setVisible (true);
      return true;
    }  //unit.open()

    //success = unit.insert (path, writeProtected)
    //  挿入する
    public boolean insert (String path, boolean writeProtected) {
      if (fdcIsInsertedPath (path)) {  //既に挿入されている
        return false;
      }
      if (!super.insert (path, writeProtected)) {  //挿入できなかった
        return false;
      }
      if (fduMedia != null) {
        IOInterrupt.ioiFddFall ();
        IOInterrupt.ioiFddRise ();
      }
      return true;
    }  //unit.insert(String)

    //loaded = unit.load (path)
    //  読み込む
    //  挿入されていない状態で呼び出すこと
    @Override protected boolean load (String path) {
      fduMedia = FDMedia.fdmPathToMedia (path, fduImage);
      if (fduMedia == null) {  //読み込めない
        return false;
      }
      if (abuWriteProtected && !abuUnprotectable) {  //書き込みが許可されることはない
        fduMedia.fdmReviveFiles (fduImage);  //削除ファイルを復元する
      }
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.println ("media = " + fduMedia.fdmName);
        System.out.println ("------------------------------------------------------------------------");
        fduMedia.fdmPrintInfo ();
        System.out.println ("------------------------------------------------------------------------");
      }
      fduWritten = false;
      fdcAddHistory (new File (path).getAbsoluteFile ());
      System.out.println (Multilingual.mlnJapanese ?
                          path + " を fd" + abuNumber + " に挿入しました" :
                          path + " was inserted in fd" + abuNumber);
      return true;
    }  //unit.load(String)


    //name = unit.getName ()
    //  pathからnameを作る
    public String getName () {
      return abuPath.substring (abuPath.lastIndexOf (File.separatorChar) + 1);
    }

    //d = unit.fduDriveStatus ()
    //  bit7  1=挿入
    //  bit6  1=誤挿入
    public int fduDriveStatus () {
      return fduIsInserted () ? 0x80 : 0;
    }

    //fduDriveControl (d)
    //  bit7  0=消灯する,1=点滅する
    //  bit6  0=排出を許可する,1=排出を禁止する
    //  bit5  0=排出しない,1=排出する
    public void fduDriveControl (int d) {
      if (d << 31 - 5 < 0) {  //排出する
        eject ();
      }
      if (d << 31 - 6 < 0) {  //排出を禁止する
        fduPrevented = true;
        prevent ();
      } else {  //排出を許可する
        fduPrevented = false;
        allow ();
      }
      if ((byte) d < 0) {  //点滅する
        fduBlinking = true;
        blink ();
      } else {  //消灯する
        fduBlinking = false;
        darken ();
      }
    }

    //fduCommandReadDiagnostic ()
    //  0x02  READ DIAGNOSTIC
    //    診断のための読み出し
    //    セクタ1から開始し、1トラック分のエラーを累積して正常終了する
    //  C-Phase
    //    [0]  bit6    MF    MFM Mode         0=FM,1=MFM
    //         bit4-0  CMD   Command          0x02=READ DIAGNOSTIC
    //    [1]  bit2    HD    Head Address     サイド
    //         bit1-0  US    Unit Select      ユニット
    //    [2]          C     Cylinder Number  シリンダ
    //    [3]          H     Head Number      サイド
    //    [4]          R     Record Number    無意味
    //    [5]          N     Record Length    セクタ長=128<<N
    //    [6]          EOT   End of Track     終了セクタ
    //    [7]          GSL   Gap Skip Length  マルチセクタのときGap3の不連続部分を読み飛ばす長さ
    //    [8]          DTL   Data Length      N==0&&DTL<128のとき128バイト読み出してCRCをチェックするがMPUにはDTLバイトだけ転送する
    //  E-Phase(FDC→MPU)
    //    データ
    //  R-Phase
    //    [0]          ST0   Status 0         リザルトステータス0
    //    [1]          ST1   Status 1         リザルトステータス1
    //    [2]          ST2   Status 2         リザルトステータス2
    //    [3]          C     Cylinder Number  シリンダ
    //    [4]          H     Head Number      サイド
    //    [5]          R     Record Number    正常終了のときは実行終了セクタの次のセクタ
    //                                          R=EOTまで転送するとRが1に戻り、MTでないときC、MTのときHとCに繰り上がる
    //                                          ただし、NT|CMでSKでないときは実行終了セクタ
    //                                        エラーのときは異常発生セクタ
    //    [6]          N     Record Length    セクタ長=128<<N
    //  リザルトステータス
    //    正常終了  NT
    //    ノットレディ  AT|NR
    //    ID部
    //      IDAM非検出  AT|MA
    //      C不一致(終了せず)  NT|ND
    //      H不一致(終了せず)  NT|ND
    //      R不一致(終了せず)  NT|ND
    //      N不一致(終了せず)  NT|ND
    //      CRC不一致(終了せず)  NT|DE
    //    データ部
    //      DAM非検出  AT|MA|MD
    //      DDAM検出(終了せず)  NT|CM
    //      CRC不一致(終了せず)  NT|DE|DD
    //      オーバーラン  AT|OR
    //    最終セクタで未終了  AT|EN
    public void fduCommandReadDiagnostic () {
      //!!!
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("%08x READ DIAGNOSTIC is not implemented\n", XEiJ.regPC0);
      }
      fdcCommandInvalid ();
    }

    //fduCommandSenseDeviceStatus ()
    //  0x04  SENSE DEVICE STATUS
    //    状態信号(ST3)の読み出し
    //  C-Phase
    //    [0]          CMD   Command          0x04=SENSE DEVICE STATUS
    //    [1]  bit2    HD    Head Address     サイド
    //         bit1-0  US    Unit Select      ユニット
    //  R-Phase
    //    [0]          ST3   Status 3         リザルトステータス3
    public void fduCommandSenseDeviceStatus () {
      //fduPHN = fdcCommandBuffer[1] >> 2 & 1;  //HD
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("%08x fduCommandSenseDeviceStatus%d()\n", XEiJ.regPC0, abuNumber);
      }
      int st3 = ((fduIsWriteProtected () ? FDC_ST3_WP : 0) |  //Write-Protected
                 (fduIsReady () ? FDC_ST3_RY : 0) |  //Ready
                 (fduIsInserted () && fduIsReady () && fduPCN == 0 ? FDC_ST3_T0 : 0) |  //Track 0
                 //(fduIsInserted () ? fduMedia.fdmTwoSide : 0) |  //Two Side
                 (fduPHN & 1) << 2 |  //HD
                 abuNumber);  //US1,US0
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("\t\tST3=0x%02x(FT=%d WP=%d RY=%d T0=%d TS=%d HD=%d US=%d)\n",
                           st3,
                           st3 >> 7,
                           st3 >> 6 & 1,
                           st3 >> 5 & 1,
                           st3 >> 4 & 1,
                           st3 >> 3 & 1,
                           st3 >> 2 & 1,
                           st3 & 3);
      }
      fdcResultBuffer[0] = (byte) st3;
      fdcRPhase (1);
    }

    //fduCommandWriteData ()
    //  0x05  WRITE DATA
    //    データ(DAM)の書き込み
    //  C-Phase
    //    [0]  bit7    MT    Multitrack       マルチトラック
    //         bit6    MF    MFM Mode         0=FM,1=MFM
    //         bit4-0  CMD   Command          0x05=WRITE DATA
    //    [1]  bit2    HD    Head Address     サイド
    //         bit1-0  US    Unit Select      ユニット
    //    [2]          C     Cylinder Number  シリンダ
    //    [3]          H     Head Number      サイド
    //    [4]          R     Record Number    開始セクタ
    //    [5]          N     Record Length    セクタ長=128<<N
    //    [6]          EOT   End of Track     終了セクタ
    //    [7]          GSL   Gap Skip Length  マルチセクタのときGap3の不連続部分を読み飛ばす長さ
    //    [8]          DTL   Data Length      N==0&&DTL<128のときMPUからDTLバイトだけ受け取って128バイト書き込む
    //  E-Phase(MPU→FDC)
    //    データ
    //  R-Phase
    //    [0]          ST0   Status 0         リザルトステータス0
    //    [1]          ST1   Status 1         リザルトステータス1
    //    [2]          ST2   Status 2         リザルトステータス2
    //    [3]          C     Cylinder Number  シリンダ
    //    [4]          H     Head Number      サイド
    //    [5]          R     Record Number    正常終了のときは実行終了セクタの次のセクタ
    //                                          R=EOTまで転送するとRが1に戻り、MTでないときC、MTのときHとCに繰り上がる
    //                                          ただし、NT|CMでSKでないときは実行終了セクタ
    //                                        エラーのときは異常発生セクタ
    //    [6]          N     Record Length    セクタ長=128<<N
    //  リザルトステータス
    //    正常終了  NT
    //    ノットレディ  AT|NR
    //    ライトプロテクト  AT|NW
    //    ID部
    //      IDAM非検出  AT|MA
    //      C不一致(!=0xff)  AT|ND|NC
    //      C不一致(==0xff)  AT|ND|BC
    //      H不一致  AT|ND
    //      R不一致  AT|ND
    //      N不一致  AT|ND
    //      CRC不一致  AT|DE
    //    データ部
    //      フォールト  AT|EC
    //      オーバーラン  AT|OR
    //    最終セクタで未終了  AT|EN
    public void fduCommandWriteData () {
      //fduPHN = fdcCommandBuffer[1] >> 2 & 1;  //HD
      int ncn = fdcCommandBuffer[2] & 255;  //NCN
      int phn = fdcCommandBuffer[3] & 255;  //H
      int prn = fdcCommandBuffer[4] & 255;  //R
      int pnn = fdcCommandBuffer[5] & 255;  //N
      int eot = fdcCommandBuffer[6] & 255;  //EOT
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("%08x fduCommandWriteData%d(NCN=%d H=%d R=%d N=%d EOT=%d)\n",
                           XEiJ.regPC0, abuNumber, ncn, phn, prn, pnn, eot);
      }
      fduCommandNumber = fdcCommandNumber;
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("\tfduCommandNumber%d=0x%02x\n", abuNumber, fduCommandNumber);
      }
      if (!fduIsReady ()) {  //ノットレディ
        fduEPhaseEnd (FDC_ST0_AT | FDC_ST0_NR | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      if (fduIsWriteProtected ()) {  //ライトプロテクト
        fduEPhaseEnd (FDC_ST0_AT | FDC_ST1_NW | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      fduNCN = ncn;  //目標シリンダ番号(NCNn)を設定する(既に設定されているときは上書きする)
      fduPHN = phn;
      fduPRN = prn;
      fduPNN = pnn;
      fduEOT = eot;
      fduSRC = 16 - fdcSRT;  //ステップレートカウンタ(SRC)を16-SRTにする
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("\tfduNCN%d=%d\n", abuNumber, fduNCN);
        System.out.printf ("\tfduPHN%d=%d\n", abuNumber, fduPHN);
        System.out.printf ("\tfduPRN%d=%d\n", abuNumber, fduPRN);
        System.out.printf ("\tfduPNN%d=%d\n", abuNumber, fduPNN);
        System.out.printf ("\tfduEOT%d=%d\n", abuNumber, fduEOT);
        System.out.printf ("\tfduSRC%d=%d\n", abuNumber, fduSRC);
      }
      fdcStatus = (FDC_MPU_TO_FDC | FDC_CB |  //E-Phase(転送中)に移行する(RQM=0で待機する)
                   (fdcStatus & (FDC_D3B | FDC_D2B | FDC_D1B | FDC_D0B)));
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("\tfdcStatus=%s\n", fdcStatusToString (fdcStatus));
      }
      TickerQueue.tkqAdd (fduSeekTicker, XEiJ.mpuClockTime + FDU_SEEK_INTERVAL);  //シークティッカーを(あれば取り消してから)1ms後に予約する
    }  //fduCommandWriteData()

    public void fduWriteDataEPhase () {
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("%08x fduWriteDataEPhase%d()\n", XEiJ.regPC0, abuNumber);
      }
      if (fduPCN != fduNCN) {  //シークが中断されたとき
        fduEPhaseEnd (FDC_ST0_AT | FDC_ST0_NR | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      //E-Phase
      fduWritten = true;
      fdcStatus = (FDC_RQM | FDC_MPU_TO_FDC | FDC_CB |
                   (fdcStatus & (FDC_D3B | FDC_D2B | FDC_D1B | FDC_D0B)));
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("\tfdcStatus=%s\n", fdcStatusToString (fdcStatus));
      }
      //転送
      int o = fduCalcOffset ();
      if (o < 0) {  //セクタが存在しない
        fduEPhaseEnd (FDC_ST0_AT | FDC_ST1_MA | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      fdcReadHandle = null;
      fdcWriteHandle = fduImage;
      fdcIndex = fdcStart = o;
      fdcLimit = o + (128 << fduPNN);
      HD63450.dmaFallREQ (0);  //DMA転送開始
    }  //fduWriteDataEPhase()

    //fduWriteDataEPhaseEnd ()
    public void fduWriteDataEPhaseEnd () {
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("%08x fduWriteDataEPhaseEnd%d()\n", XEiJ.regPC0, abuNumber);
      }
      if (fduPRN == fduEOT) {  //終了
        HD63450.dmaRiseREQ (0);  //DMA転送終了
        fduEPhaseEnd (FDC_ST0_NT | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      //継続
      if (fduMedia == FDMedia.FDM_2HS && fduPCN == 0 && fduPHN == 0 && fduPRN == 1) {
        fduPRN = 11;
      } else {
        fduPRN++;
      }
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("\tfduPRN%d=%d\n", abuNumber, fduPRN);
      }
      int o = fduCalcOffset ();
      if (o < 0) {  //セクタが存在しない
        fduEPhaseEnd (FDC_ST0_AT | FDC_ST1_MA | (fduPHN << 2 | abuNumber << 24));
        return;
      }
      fdcIndex = fdcStart = o;
      fdcLimit = o + (128 << fduPNN);
    }  //fduWriteDataEPhaseEnd()

    //fduCommandReadData ()
    //  0x06  READ DATA
    //    データ(DAM)の読み出し
    //  C-Phase
    //    [0]  bit7    MT    Multitrack       マルチトラック
    //         bit6    MF    MFM Mode         0=FM,1=MFM
    //         bit5    SK    Skip             DDAMをスキップ
    //         bit4-0  CMD   Command          0x06=READ DATA
    //    [1]  bit2    HD    Head Address     サイド
    //         bit1-0  US    Unit Select      ユニット
    //    [2]          C     Cylinder Number  シリンダ
    //    [3]          H     Head Number      サイド
    //    [4]          R     Record Number    開始セクタ
    //    [5]          N     Record Length    セクタ長=128<<N
    //    [6]          EOT   End of Track     終了セクタ
    //    [7]          GSL   Gap Skip Length  マルチセクタのときGap3の不連続部分を読み飛ばす長さ
    //    [8]          DTL   Data Length      N==0&&DTL<128のとき128バイト読み出してCRCをチェックするがMPUにはDTLバイトだけ転送する
    //  E-Phase(FDC→MPU)
    //    データ
    //  R-Phase
    //    [0]          ST0   Status 0         リザルトステータス0
    //    [1]          ST1   Status 1         リザルトステータス1
    //    [2]          ST2   Status 2         リザルトステータス2
    //    [3]          C     Cylinder Number  シリンダ
    //    [4]          H     Head Number      サイド
    //    [5]          R     Record Number    正常終了のときは実行終了セクタの次のセクタ
    //                                          R=EOTまで転送するとRが1に戻り、MTでないときC、MTのときHとCに繰り上がる
    //                                          ただし、NT|CMでSKでないときは実行終了セクタ
    //                                        エラーのときは異常発生セクタ
    //    [6]          N     Record Length    セクタ長=128<<N
    //  リザルトステータス
    //    正常終了  NT
    //    ノットレディ  AT|NR
    //    ID部
    //      IDAM非検出  AT|MA
    //      C不一致(!=0xff)  AT|ND|NC
    //      C不一致(==0xff)  AT|ND|BC
    //      H不一致  AT|ND
    //      R不一致  AT|ND
    //      N不一致  AT|ND
    //      CRC不一致  AT|DE
    //    データ部
    //      DAM非検出  AT|MA|MD
    //      DDAM検出  NT|CM
    //      CRC不一致  AT|DE|DD
    //      オーバーラン  AT|OR
    //    最終セクタで未終了  AT|EN
    public void fduCommandReadData () {
      //fduPHN = fdcCommandBuffer[1] >> 2 & 1;  //HD
      int ncn = fdcCommandBuffer[2] & 255;  //NCN
      int phn = fdcCommandBuffer[3] & 255;  //H
      int prn = fdcCommandBuffer[4] & 255;  //R
      int pnn = fdcCommandBuffer[5] & 255;  //N
      int eot = fdcCommandBuffer[6] & 255;  //EOT
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("%08x fduCommandReadData%d(NCN=%d H=%d R=%d N=%d EOT=%d)\n",
                           XEiJ.regPC0, abuNumber, ncn, phn, prn, pnn, eot);
      }
      fduCommandNumber = fdcCommandNumber;
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("\tfduCommandNumber%d=0x%02x\n", abuNumber, fduCommandNumber);
      }
      if (!fduIsReady ()) {  //ノットレディ
        fduEPhaseEnd (FDC_ST0_AT | FDC_ST0_NR | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      fduNCN = ncn;  //目標シリンダ番号(NCNn)を設定する(既に設定されているときは上書きする)
      fduPHN = phn;
      fduPRN = prn;
      fduPNN = pnn;
      fduEOT = eot;
      fduSRC = 16 - fdcSRT;  //ステップレートカウンタ(SRC)を16-SRTにする
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("\tfduNCN%d=%d\n", abuNumber, fduNCN);
        System.out.printf ("\tfduPHN%d=%d\n", abuNumber, fduPHN);
        System.out.printf ("\tfduPRN%d=%d\n", abuNumber, fduPRN);
        System.out.printf ("\tfduPNN%d=%d\n", abuNumber, fduPNN);
        System.out.printf ("\tfduEOT%d=%d\n", abuNumber, fduEOT);
        System.out.printf ("\tfduSRC%d=%d\n", abuNumber, fduSRC);
      }
      fdcStatus = (FDC_MPU_TO_FDC | FDC_CB |  //E-Phase(転送中)に移行する(RQM=0で待機する)
                   (fdcStatus & (FDC_D3B | FDC_D2B | FDC_D1B | FDC_D0B)));
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("\tfdcStatus=%s\n", fdcStatusToString (fdcStatus));
      }
      fdcReadHandle = null;
      fdcWriteHandle = null;
      TickerQueue.tkqAdd (fduSeekTicker, XEiJ.mpuClockTime + FDU_SEEK_INTERVAL);  //シークティッカーを(あれば取り消してから)1ms後に予約する
    }  //fduCommandReadData()

    public void fduReadDataEPhase () {
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("%08x fduReadDataEPhase%d()\n", XEiJ.regPC0, abuNumber);
      }
      if (fduPCN != fduNCN) {  //シークが中断されたとき
        fduEPhaseEnd (FDC_ST0_AT | FDC_ST0_NR | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      //E-Phase
      fdcStatus = (FDC_RQM | FDC_FDC_TO_MPU | FDC_CB |
                   (fdcStatus & (FDC_D3B | FDC_D2B | FDC_D1B | FDC_D0B)));
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("\tfdcStatus=%s\n", fdcStatusToString (fdcStatus));
      }
      //転送
      int o = fduCalcOffset ();
      if (o < 0) {  //セクタが存在しない
        fduEPhaseEnd (FDC_ST0_AT | FDC_ST1_MA | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      fdcReadHandle = fduImage;
      fdcWriteHandle = null;
      fdcIndex = fdcStart = o;
      fdcLimit = o + (128 << fduPNN);
      HD63450.dmaFallREQ (0);  //DMA転送開始
    }  //fduReadDataEPhase()

    //fduReadDataEPhaseEnd ()
    public void fduReadDataEPhaseEnd () {
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("%08x fduReadDataEPhaseEnd%d()\n", XEiJ.regPC0, abuNumber);
      }
      if (fduPRN == fduEOT) {  //終了
        HD63450.dmaRiseREQ (0);  //DMA転送終了
        fduEPhaseEnd (FDC_ST0_NT | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      //継続
      if (fduMedia == FDMedia.FDM_2HS && fduPCN == 0 && fduPHN == 0 && fduPRN == 1) {
        fduPRN = 11;
      } else {
        fduPRN++;
      }
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("\tfduPRN%d=%d\n", abuNumber, fduPRN);
      }
      int o = fduCalcOffset ();
      if (o < 0) {  //セクタが存在しない
        fduEPhaseEnd (FDC_ST0_AT | FDC_ST1_MA | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      fdcIndex = fdcStart = o;
      fdcLimit = o + (128 << fduPNN);
    }  //fduReadDataEPhaseEnd()

    //fduCommandRecalibrate ()
    //  0x07  RECALIBRATE
    //    トラック0(一番外側)へのシーク
    //  C-Phase
    //    [0]          CMD   Command          0x07=RECALIBRATE
    //    [1]  bit1-0  US    Unit Select      ユニット
    //  リザルトステータス(SENSE INTERRUPT STATUSで引き取る)
    //    正常終了  NT|SE
    //    ノットレディ  AT|SE|NR
    //    トラック0非検出  AT|SE|EC
    public void fduCommandRecalibrate () {
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("%08x fduCommandRecalibrate%d()\n", XEiJ.regPC0, abuNumber);
      }
      fduCommandNumber = fdcCommandNumber;
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("\tfduCommandNumber%d=0x%02x\n", abuNumber, fduCommandNumber);
      }
      fdcStatus |= 1 << abuNumber;  //FDnビジー(DnB)を1にする
      fduNCN = 0;  //目標シリンダ番号(NCNn)を設定する(既に設定されているときは上書きする)
      fduSRC = 16 - fdcSRT;  //ステップレートカウンタ(SRC)を16-SRTにする
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("\tfdcStatus=%s\n", fdcStatusToString (fdcStatus));
        System.out.printf ("\tfduNCN%d=%d\n", abuNumber, fduNCN);
        System.out.printf ("\tfduSRC%d=%d\n", abuNumber, fduSRC);
      }
      if (fdcEnforcedReady && !abuConnected) {  //強制レディ状態で接続されていないとき
        fduSeekResultStatus = FDC_ST0_AT | FDC_ST0_SE | FDC_ST0_EC;  //AT(Abnormal Terminate)+SE(Seek End)+EC(Equipment Check)のリザルトステータスを準備する
        if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
          System.out.printf ("\tfduSeekResultStatus=%s\n", fdcResultStatusToString (fduSeekResultStatus));
        }
        fdcCPhase ();  //E-Phase(シーク中)に移行する
        fduSeekEnd ();  //SEEK/RECALIBRATEコマンドのE-Phase(シーク中)が終了したとき
      } else if (!fduIsReady ()) {  //ノットレディのとき
        fduSeekResultStatus = FDC_ST0_AT | FDC_ST0_SE | FDC_ST0_NR;  //AT(Abnormal Terminate)+SE(Seek End)+NR(Not Ready)のリザルトステータスを準備する
        if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
          System.out.printf ("\tfduSeekResultStatus=%s\n", fdcResultStatusToString (fduSeekResultStatus));
        }
        fdcCPhase ();  //E-Phase(シーク中)に移行する
        fduSeekEnd ();  //SEEK/RECALIBRATEコマンドのE-Phase(シーク中)が終了したとき
      } else {  //レディのとき
        fduSeekResultStatus = FDC_ST0_NT | FDC_ST0_SE;  //NT(Normal Terminate)+SE(Seek End)のリザルトステータスを準備する
        if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
          System.out.printf ("\tfduSeekResultStatus=%s\n", fdcResultStatusToString (fduSeekResultStatus));
        }
        TickerQueue.tkqAdd (fduSeekTicker, XEiJ.mpuClockTime + FDU_SEEK_INTERVAL);  //シークティッカーを(あれば取り消してから)1ms後に予約する
        fdcCPhase ();  //E-Phase(シーク中)に移行する
      }
    }  //fduCommandRecalibrate()

    //fduCommandWriteDeletedData ()
    //  0x09  WRITE DELETED DATA
    //    削除データ(DDAM)の書き込み
    //  C-Phase
    //    [0]  bit7    MT    Multitrack       マルチトラック
    //         bit6    MF    MFM Mode         0=FM,1=MFM
    //         bit4-0  CMD   Command          0x09=WRITE DELETED DATA
    //    [1]  bit2    HD    Head Address     サイド
    //         bit1-0  US    Unit Select      ユニット
    //    [2]          C     Cylinder Number  シリンダ
    //    [3]          H     Head Number      サイド
    //    [4]          R     Record Number    開始セクタ
    //    [5]          N     Record Length    セクタ長=128<<N
    //    [6]          EOT   End of Track     終了セクタ
    //    [7]          GSL   Gap Skip Length  マルチセクタのときGap3の不連続部分を読み飛ばす長さ
    //    [8]          DTL   Data Length      N==0&&DTL<128のときMPUからDTLバイトだけ受け取って128バイト書き込む
    //  E-Phase(MPU→FDC)
    //    データ
    //  R-Phase
    //    [0]          ST0   Status 0         リザルトステータス0
    //    [1]          ST1   Status 1         リザルトステータス1
    //    [2]          ST2   Status 2         リザルトステータス2
    //    [3]          C     Cylinder Number  シリンダ
    //    [4]          H     Head Number      サイド
    //    [5]          R     Record Number    正常終了のときは実行終了セクタの次のセクタ
    //                                          R=EOTまで転送するとRが1に戻り、MTでないときC、MTのときHとCに繰り上がる
    //                                          ただし、NT|CMでSKでないときは実行終了セクタ
    //                                        エラーのときは異常発生セクタ
    //    [6]          N     Record Length    セクタ長=128<<N
    //  リザルトステータス
    //    正常終了  NT
    //    ノットレディ  AT|NR
    //    ライトプロテクト  AT|NW
    //    ID部
    //      IDAM非検出  AT|MA
    //      C不一致(!=0xff)  AT|ND|NC
    //      C不一致(==0xff)  AT|ND|BC
    //      H不一致  AT|ND
    //      R不一致  AT|ND
    //      N不一致  AT|ND
    //      CRC不一致  AT|DE
    //    データ部
    //      フォールト  AT|EC
    //      オーバーラン  AT|OR
    //    最終セクタで未終了  AT|EN
    public void fduCommandWriteDeletedData () {
      //!!!
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("%08x WRITE DELETED DATA is not implemented\n", XEiJ.regPC0);
      }
      fdcCommandInvalid ();
    }

    //fduCommandReadId ()
    //  0x0a  READ ID
    //    現在のシリンダの指定されたサイドにあるトラックでヘッドロード後最初に見つけたDEエラーやMAエラーのないセクタのIDを返す
    //    トラック上のどのセクタのIDが返るかは不定
    //  C-Phase
    //    [0]  bit6    MF    MFM Mode         0=FM,1=MFM
    //         bit4-0  CMD   Command          0x0a=READ ID
    //    [1]  bit2    HD    Head Address     サイド
    //         bit1-0  US    Unit Select      ユニット
    //  R-Phase
    //    [0]          ST0   Status 0         リザルトステータス0
    //    [1]          ST1   Status 1         リザルトステータス1
    //    [2]          ST2   Status 2         リザルトステータス2
    //    [3]          C     Cylinder Number  シリンダ
    //    [4]          H     Head Number      サイド
    //    [5]          R     Record Number    セクタ
    //    [6]          N     Record Length    セクタ長=128<<N
    //  リザルトステータス
    //    正常終了  NT
    //    ノットレディ  AT|NR
    //    ID部
    //      IDAM非検出  AT|MA  インデックスパルスを2回検出するまでにIDAMが見つからない
    //      CRC不一致  AT|ND  IDAMを見つけたがインデックスパルスを2回検出するまでにCRCエラーのないIDが見つからない
    public void fduCommandReadId () {
      int hd = fdcCommandBuffer[1] >> 2 & 1;  //HD
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("%08x fduCommandReadId%d(HD=%d)\n", XEiJ.regPC0, abuNumber, hd);
      }
      fduCommandNumber = fdcCommandNumber;
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("\tfduCommandNumber%d=0x%02x\n", abuNumber, fduCommandNumber);
      }
      fduPHN = hd;
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("\tfduPHN%d=%d\n", abuNumber, fduPHN);
      }
      if (!fduIsReady ()) {  //ノットレディ
        fduEPhaseEnd (FDC_ST0_AT | FDC_ST0_NR | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      //セクタ番号
      //  常に1を返す方法だとREAD IDを繰り返してセクタ数を確認することができない
      if (fduMedia == FDMedia.FDM_2HS) {  //2HS
        if (fduPCN == 0 && fduPHN == 0) {  //最初のトラック
          if (fduPRN == 1) {  //最初のセクタ
            fduPRN = 11;  //2番目のセクタ
            if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
              System.out.printf ("\tfduPRN%d=%d\n", abuNumber, fduPRN);
            }
          } else if (fduPRN == 18) {  //最後のセクタ
            fduPRN = 1;  //最初のセクタ
            if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
              System.out.printf ("\tfduPRN%d=%d\n", abuNumber, fduPRN);
            }
          } else {  //その他のセクタ
            fduPRN++;  //次のセクタ
            if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
              System.out.printf ("\tfduPRN%d=%d\n", abuNumber, fduPRN);
            }
          }
        } else {  //最初のトラック以外
          if (fduPRN == 18) {  //最後のセクタ
            fduPRN = 10;  //最初のセクタ
            if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
              System.out.printf ("\tfduPRN%d=%d\n", abuNumber, fduPRN);
            }
          } else {  //その他のセクタ
            fduPRN++;  //次のセクタ
            if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
              System.out.printf ("\tfduPRN%d=%d\n", abuNumber, fduPRN);
            }
          }
        }
      } else {  //2HS以外
        if (fduPRN == fduMedia.fdmSectorsPerTrack) {  //最後のセクタ
          fduPRN = 1;  //最初のセクタ
          if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
            System.out.printf ("\tfduPRN%d=%d\n", abuNumber, fduPRN);
          }
        } else {  //その他のセクタ
          fduPRN++;  //次のセクタ
          if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
            System.out.printf ("\tfduPRN%d=%d\n", abuNumber, fduPRN);
          }
        }
      }
      //サイド番号
      if (fduMedia == FDMedia.FDM_2HDE) {
        if (!(fduPCN == 0 && fduPHN == 0 && fduPRN == 1)) {  //最初のトラックの最初のセクタ以外
          fduPHN |= 128;
          if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
            System.out.printf ("\tfduPHN%d=%d\n", abuNumber, fduPHN);
          }
        }
      }
      //セクタスケール
      fduPNN = fduMedia.fdmSectorScale;
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("\tfduPNN%d=%d\n", abuNumber, fduPNN);
      }
      int o = fduCalcOffset ();
      if (o < 0) {  //セクタが存在しない
        fduEPhaseEnd (FDC_ST0_AT | FDC_ST1_MA | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      fduEPhaseEnd (FDC_ST0_NT | ((fduPHN & 1) << 2 | abuNumber) << 24);
    }  //fduCommandReadId()

    //fduCommandReadDeletedData ()
    //  0x0c  READ DELETED DATA
    //    削除データ(DDAM)の読み出し
    //  C-Phase
    //    [0]  bit7    MT    Multitrack       マルチトラック
    //         bit6    MF    MFM Mode         0=FM,1=MFM
    //         bit5    SK    Skip             DAMをスキップ
    //         bit4-0  CMD   Command          0x0c=READ DELETED DATA
    //    [1]  bit2    HD    Head Address     サイド
    //         bit1-0  US    Unit Select      ユニット
    //    [2]          C     Cylinder Number  シリンダ
    //    [3]          H     Head Number      サイド
    //    [4]          R     Record Number    開始セクタ
    //    [5]          N     Record Length    セクタ長=128<<N
    //    [6]          EOT   End of Track     終了セクタ
    //    [7]          GSL   Gap Skip Length  マルチセクタのときGap3の不連続部分を読み飛ばす長さ
    //    [8]          DTL   Data Length      N==0&&DTL<128のとき128バイト読み出してCRCをチェックしてMPUにDTLバイトだけ渡す
    //  E-Phase(FDC→MPU)
    //    データ
    //  R-Phase
    //    [0]          ST0   Status 0         リザルトステータス0
    //    [1]          ST1   Status 1         リザルトステータス1
    //    [2]          ST2   Status 2         リザルトステータス2
    //    [3]          C     Cylinder Number  シリンダ
    //    [4]          H     Head Number      サイド
    //    [5]          R     Record Number    正常終了のときは実行終了セクタの次のセクタ
    //                                          R=EOTまで転送するとRが1に戻り、MTでないときC、MTのときHとCに繰り上がる
    //                                          ただし、NT|CMでSKでないときは実行終了セクタ
    //                                        エラーのときは異常発生セクタ
    //    [6]          N     Record Length    セクタ長=128<<N
    //  リザルトステータス
    //    正常終了  NT
    //    ノットレディ  AT|NR
    //    ID部
    //      IDAM非検出  AT|MA
    //      C不一致(!=0xff)  AT|ND|NC
    //      C不一致(==0xff)  AT|ND|BC
    //      H不一致  AT|ND
    //      R不一致  AT|ND
    //      N不一致  AT|ND
    //      CRC不一致  AT|DE
    //    データ部
    //      DDAM非検出  AT|MA|MD
    //      DAM検出  NT|CM
    //      CRC不一致  AT|DE|DD
    //      オーバーラン  AT|OR
    //    最終セクタで未終了  AT|EN
    public void fduCommandReadDeletedData () {
      //!!!
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("%08x READ DELETED DATA is not implemented\n", XEiJ.regPC0);
      }
      fdcCommandInvalid ();
    }

    //fduCommandWriteId ()
    //  0x0d  WRITE ID
    //    1トラックフォーマットする。別名Format Write
    //  C-Phase
    //    [0]  bit6    MF    MFM Mode         0=FM,1=MFM
    //         bit4-0  CMD   Command          0x0d=WRITE ID
    //    [1]  bit2    HD    Head Address     サイド
    //         bit1-0  US    Unit Select      ユニット
    //    [2]          N     Record Length    セクタ長=128<<N
    //    [3]          SC    Sector           1トラックあたりのセクタ数
    //    [4]          GPL   Gap Length       Gap3に書き込む長さ
    //    [5]          D     Data             データ部に書き込むデータ
    //  E-Phase
    //    1トラック分のID情報。4*SCバイト
    //    [0]          C     Cylinder Number  シリンダ
    //    [1]          H     Head Number      サイド
    //    [2]          R     Record Number    開始セクタ
    //    [3]          N     Record Length    セクタ長=128<<N
    //    これをSC回繰り返す
    //  R-Phase
    //    [0]          ST0   Status 0         リザルトステータス0
    //    [1]          ST1   Status 1         リザルトステータス1
    //    [2]          ST2   Status 2         リザルトステータス2
    //    [3]          C     Cylinder Number  無意味
    //    [4]          H     Head Number      無意味
    //    [5]          R     Record Number    無意味
    //    [6]          N     Record Length    セクタ長=128<<N
    //  リザルトステータス
    //    正常終了  NT
    //    ノットレディ  AT|NR
    //    ライトプロテクト  AT|NW
    //    フォールト  AT|EC
    //    オーバーラン  AT|OR
    public void fduCommandWriteId () {
      //fduPHN = fdcCommandBuffer[1] >> 2 & 1;  //HD
      fduCommandNumber = fdcCommandNumber;
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("\tfduCommandNumber%d=0x%02x\n", abuNumber, fduCommandNumber);
      }
      if (!fduIsReady ()) {  //ノットレディ
        if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
          System.out.printf ("%08x unit=%d,is not ready\n", XEiJ.regPC0, abuNumber);
        }
        fduEPhaseEnd (FDC_ST0_AT | FDC_ST0_NR | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      if (fduIsWriteProtected ()) {  //書き込めない
        if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
          System.out.printf ("%08x unit=%d,is read-only\n", XEiJ.regPC0, abuNumber);
        }
        fduEPhaseEnd (FDC_ST0_AT | FDC_ST1_NW | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      //E-Phase
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("%08x FDC E-Phase\n", XEiJ.regPC0);
      }
      fdcStatus = (FDC_RQM | FDC_MPU_TO_FDC | FDC_CB |
                   (fdcStatus & (FDC_D3B | FDC_D2B | FDC_D1B | FDC_D0B)));
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("\tfdcStatus=%s\n", fdcStatusToString (fdcStatus));
      }
      fdcReadHandle = null;
      fdcWriteHandle = fdcTempBuffer;
      fdcIndex = fdcStart = 0;
      fdcLimit = (fdcCommandBuffer[3] & 255) << 2;
      HD63450.dmaFallREQ (0);  //DMA転送開始
    }  //fduCommandWriteId()

    //fduWriteIdEPhaseEnd ()
    public void fduWriteIdEPhaseEnd () {
      HD63450.dmaRiseREQ (0);  //DMA転送終了
      //トラック0のフォーマットでメディアを判別する
      if (fdcTempBuffer[0] == 0 && fdcTempBuffer[1] == 0) {  //pcn1==0&&phn1==0。トラック0
        int prn1 = fdcTempBuffer[2] & 255;
        int pnn1 = fdcTempBuffer[3] & 255;
        int pcn2 = fdcTempBuffer[4] & 255;
        int phn2 = fdcTempBuffer[5] & 255;
        int prn2 = fdcTempBuffer[6] & 255;
        int pnn2 = fdcTempBuffer[7] & 255;
        int sectors = fdcLimit >> 2;  //1トラックあたりのセクタ数。fdcCommandBuffer[3]&255
        FDMedia media = null;
        if (phn2 == 0 && prn1 == 1 && prn2 == 2) {
          if (!fduDoubleDensity) {  //高密度
            if (pnn1 == 3 && pnn2 == 3) {  //1024バイト/セクタ
              if (sectors == 8) {  //8セクタ/トラック
                media = FDMedia.FDM_2HD;
              }
            } else if (pnn1 == 2 && pnn2 == 2) {  //512バイト/セクタ
              if (sectors == 15) {  //15セクタ/トラック
                media = FDMedia.FDM_2HC;
              } else if (sectors == 18) {  //18セクタ/トラック
                media = FDMedia.FDM_2HQ;
              }
            }
          } else {  //倍密度
            if (pnn1 == 2 && pnn2 == 2) {  //512バイト/セクタ
              if (sectors == 8) {  //8セクタ/トラック
                media = FDMedia.FDM_2DD8;
              } else if (sectors == 9) {  //9セクタ/トラック
                media = FDMedia.FDM_2DD9;
              } else if (sectors == 10) {  //10セクタ/トラック
                media = FDMedia.FDM_2DD10;
              }
            }
          }
        } else if (phn2 == 128 && prn1 == 1 && prn2 == 2) {  //2番目のセクタのサイド番号が128
          if (!fduDoubleDensity) {  //高密度
            if (pnn1 == 3 && pnn2 == 3) {  //1024バイト/セクタ
              if (sectors == 9) {  //9セクタ/トラック
                media = FDMedia.FDM_2HDE;
              }
            }
          }
        } else if (phn2 == 0 && prn1 == 1 && prn2 == 11) {  //2番目のセクタのレコード番号が11
          if (!fduDoubleDensity) {  //高密度
            if (pnn1 == 3 && pnn2 == 3) {  //1024バイト/セクタ
              if (sectors == 9) {  //9セクタ/トラック
                media = FDMedia.FDM_2HS;
              }
            }
          }
        }
        if (media != null) {
          fduMedia = media;
        } else {
          fduMedia = FDMedia.FDM_2HD;
        }
        if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
          System.out.println ("media = " + fduMedia.fdmName);
          System.out.println ("------------------------------------------------------------------------");
          fduMedia.fdmPrintInfo ();
          System.out.println ("------------------------------------------------------------------------");
        }
      }  //if トラック0
      for (int i = 0; i < fdcLimit; i += 4) {
        fduPCN = fdcTempBuffer[i    ] & 255;
        if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
          System.out.printf ("%08x unit=%d,fduPCN=%d\n", XEiJ.regPC0, abuNumber, fduPCN);
        }
        fduPHN = fdcTempBuffer[i + 1] & 255;
        fduPRN = fdcTempBuffer[i + 2] & 255;
        fduPNN = fdcTempBuffer[i + 3] & 255;
        int o = fduCalcOffset ();
        if (o < 0) {  //セクタが存在しない
          //FORMAT.Xが1トラック余分にフォーマットしようとするので、シリンダが上限+1のときはエラーにせず無視する
          if (0 < fduPCN) {
            fduPCN--;
            if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
              System.out.printf ("%08x unit=%d,fduPCN=%d\n", XEiJ.regPC0, abuNumber, fduPCN);
            }
            o = fduCalcOffset ();
            fduPCN++;
            if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
              System.out.printf ("%08x unit=%d,fduPCN=%d\n", XEiJ.regPC0, abuNumber, fduPCN);
            }
          }
          if (o < 0) {  //シリンダを1つ減らしたセクタも存在しない
            fduEPhaseEnd (FDC_ST0_AT | FDC_ST0_EC | ((fduPHN & 1) << 2 | abuNumber) << 24);
            return;
          }
        } else {  //セクタが存在する
          Arrays.fill (fduImage, o, o + (128 << fduPNN), fdcCommandBuffer[5]);  //初期化データで充填する
          fduWritten = true;
        }
      }  //for i
      fduEPhaseEnd (FDC_ST0_NT | ((fduPHN & 1) << 2 | abuNumber) << 24);
    }  //fduWriteIdEPhaseEnd()

    //fduCommandSeek ()
    //  0x0f  SEEK
    //    NCNがPCNと異なるときSPECIFYコマンドで指定されたStep Rate Time間隔でPCNをインクリメントまたはデクリメントすることを繰り返す
    //    NCNが範囲外でもエラーを出さない
    //  C-Phase
    //    [0]          CMD   Command          0x0f=SEEK
    //    [1]  bit1-0  US    Unit Select      ユニット
    //    [2]          NCN   New Cylinder Number
    //  E-Phase
    //    終了待ち
    //  リザルトステータス(SENSE INTERRUPT STATUSで引き取る)
    //    正常終了  NT|SE
    //    ノットレディ  AT|SE|NR
    public void fduCommandSeek () {
      int ncn = fdcCommandBuffer[2] & 255;  //New Cylinder Number
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("%08x fduCommandSeek%d(NCN=%d)\n", XEiJ.regPC0, abuNumber, ncn);
      }
      fduCommandNumber = fdcCommandNumber;
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("\tfduCommandNumber%d=0x%02x\n", abuNumber, fduCommandNumber);
      }
      fdcStatus |= 1 << abuNumber;  //FDnビジー(DnB)を1にする
      fduNCN = ncn;  //目標シリンダ番号(NCNn)を設定する(既に設定されているときは上書きする)
      fduSRC = 16 - fdcSRT;  //ステップレートカウンタ(SRC)を16-SRTにする
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("\tfdcStatus=%s\n", fdcStatusToString (fdcStatus));
        System.out.printf ("\tfduNCN%d=%d\n", abuNumber, fduNCN);
        System.out.printf ("\tfduSRC%d=%d\n", abuNumber, fduSRC);
      }
      if (fdcEnforcedReady && !abuConnected) {  //強制レディ状態で接続されていないとき
        fduSeekResultStatus = FDC_ST0_AT | FDC_ST0_SE | FDC_ST0_EC;  //AT(Abnormal Terminate)+SE(Seek End)+EC(Equipment Check)のリザルトステータスを準備する
        if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
          System.out.printf ("\tfduSeekResultStatus=%s\n", fdcResultStatusToString (fduSeekResultStatus));
        }
        fdcCPhase ();  //E-Phase(シーク中)に移行する
        fduSeekEnd ();  //SEEK/RECALIBRATEコマンドのE-Phase(シーク中)が終了したとき
      } else if (!fduIsReady ()) {  //ノットレディのとき
        fduSeekResultStatus = FDC_ST0_AT | FDC_ST0_SE | FDC_ST0_NR;  //AT(Abnormal Terminate)+SE(Seek End)+NR(Not Ready)のリザルトステータスを準備する
        if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
          System.out.printf ("\tfduSeekResultStatus=%s\n", fdcResultStatusToString (fduSeekResultStatus));
        }
        fdcCPhase ();  //E-Phase(シーク中)に移行する
        fduSeekEnd ();  //SEEK/RECALIBRATEコマンドのE-Phase(シーク中)が終了したとき
      } else {  //レディのとき
        fduSeekResultStatus = FDC_ST0_NT | FDC_ST0_SE;  //NT(Normal Terminate)+SE(Seek End)のリザルトステータスを準備する
        if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
          System.out.printf ("\tfduSeekResultStatus=%s\n", fdcResultStatusToString (fduSeekResultStatus));
        }
        TickerQueue.tkqAdd (fduSeekTicker, XEiJ.mpuClockTime + FDU_SEEK_INTERVAL);  //シークティッカーを(あれば取り消してから)1ms後に予約する
        fdcCPhase ();  //E-Phase(シーク中)に移行する
      }
    }  //fduCommandSeek()

    //fduCommandScan ()
    //  0x11  SCAN EQUAL
    //  0x19  SCAN LOW OR EQUAL
    //  0x1d  SCAN HIGH OR EQUAL
    //    記録データと目的データを1バイトずつ8bit符号なし数値とみなして比較する(ベリファイ)
    //  C-Phase
    //    [0]  bit7    MT    Multitrack       マルチトラック
    //         bit6    MF    MFM Mode         0=FM,1=MFM
    //         bit5    SK    Skip             DDAMをスキップ
    //         bit4-0  CMD   Command          0x11=SCAN EQUAL,0x19=SCAN LOW OR EQUAL,0x1d=SCAN HIGH OR EQUAL
    //    [1]  bit2    HD    Head Address     サイド
    //         bit1-0  US    Unit Select      ユニット
    //    [2]          C     Cylinder Number  シリンダ
    //    [3]          H     Head Number      サイド
    //    [4]          R     Record Number    開始セクタ
    //    [5]          N     Record Length    セクタ長=128<<N
    //    [6]          EOT   End of Track     終了セクタ。STP==2のときはRとEOTの奇偶が同じであること
    //    [7]          GSL   Gap Skip Length  マルチセクタのときGap3の不連続部分を読み飛ばす長さ
    //    [8]          STP   Step             条件不成立のとき1=次のセクタに進む(R++),2=次の次のセクタに進む(R+=2)
    //  E-Phase(MPU→FDC)
    //    (EOT-R)/STP+1セクタ分の目的データ
    //    条件
    //      EQUAL          目的データ==0xff||記録データ==目的データ
    //      LOW OR EQUAL   目的データ==0xff||記録データ<=目的データ
    //      HIGH OR EQUAL  目的データ==0xff||記録データ>=目的データ
    //    1セクタ分のデータがすべて条件成立したとき
    //      条件成立で終了
    //    1セクタの中に条件成立しないデータがあったとき
    //      終了セクタではないとき
    //        STPに従って次または次の次のセクタに進む
    //      終了セクタまでに1セクタ分のデータがすべて条件成立するセクタが見つからなかったとき
    //        条件不成立で終了
    //  R-Phase
    //    [0]          ST0   Status 0         リザルトステータス0
    //    [1]          ST1   Status 1         リザルトステータス1
    //    [2]          ST2   Status 2         リザルトステータス2
    //    [3]          C     Cylinder Number  シリンダ
    //    [4]          H     Head Number      サイド
    //    [5]          R     Record Number    最後に比較したセクタ
    //    [6]          N     Record Length    セクタ長=128<<N
    //  リザルトステータス
    //    条件成立  EQUAL          NT|SH
    //              LOW OR EQUAL   NT
    //              HIGH OR EQUAL  NT
    //    条件不成立  NT|SN
    //    ノットレディ  AT|NR
    //    ID部
    //      IDAM非検出  AT|MA
    //      C不一致(!=0xff)  AT|ND|NC
    //      C不一致(==0xff)  AT|ND|BC
    //      H不一致  AT|ND
    //      R不一致  AT|ND
    //      N不一致  AT|ND
    //      CRC不一致  AT|DE
    //    データ部
    //      DAM非検出  AT|MA|MD
    //      DDAM検出  NT|CM
    //      CRC不一致  AT|DE|DD
    //      オーバーラン  AT|OR
    public void fduCommandScan () {
      //fduPHN = fdcCommandBuffer[1] >> 2 & 1;  //HD
      int ncn = fdcCommandBuffer[2] & 255;  //NCN
      int phn = fdcCommandBuffer[3] & 255;  //H
      int prn = fdcCommandBuffer[4] & 255;  //R
      int pnn = fdcCommandBuffer[5] & 255;  //N
      int eot = fdcCommandBuffer[6] & 255;  //EOT
      int stp = fdcCommandBuffer[8] & 3;  //STP
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("%08x fduCommandScan%d(NCN=%d H=%d R=%d N=%d EOT=%d STP=%d)\n",
                           XEiJ.regPC0, abuNumber, ncn, phn, prn, pnn, eot, stp);
      }
      fduCommandNumber = fdcCommandNumber;
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("\tfduCommandNumber%d=0x%02x\n", abuNumber, fduCommandNumber);
      }
      if (!fduIsReady ()) {  //ノットレディ
        fduEPhaseEnd (FDC_ST0_AT | FDC_ST0_NR | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      fduNCN = ncn;  //目標シリンダ番号(NCNn)を設定する(既に設定されているときは上書きする)
      fduPHN = phn;
      fduPRN = prn;
      fduPNN = pnn;
      fduEOT = eot;
      fduSTP = stp;
      fduSRC = 16 - fdcSRT;  //ステップレートカウンタ(SRC)を16-SRTにする
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("\tfduNCN%d=%d\n", abuNumber, fduNCN);
        System.out.printf ("\tfduPHN%d=%d\n", abuNumber, fduPHN);
        System.out.printf ("\tfduPRN%d=%d\n", abuNumber, fduPRN);
        System.out.printf ("\tfduPNN%d=%d\n", abuNumber, fduPNN);
        System.out.printf ("\tfduEOT%d=%d\n", abuNumber, fduEOT);
        System.out.printf ("\tfduSTP%d=%d\n", abuNumber, fduSTP);
        System.out.printf ("\tfduSRC%d=%d\n", abuNumber, fduSRC);
      }
      fdcStatus = (FDC_MPU_TO_FDC | FDC_CB |  //E-Phase(転送中)に移行する(RQM=0で待機する)
                   (fdcStatus & (FDC_D3B | FDC_D2B | FDC_D1B | FDC_D0B)));
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("\tfdcStatus=%s\n", fdcStatusToString (fdcStatus));
      }
      fdcReadHandle = null;
      fdcWriteHandle = null;
      TickerQueue.tkqAdd (fduSeekTicker, XEiJ.mpuClockTime + FDU_SEEK_INTERVAL);  //シークティッカーを(あれば取り消してから)1ms後に予約する
    }  //fduCommandScan()

    public void fduScanEPhase () {
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("%08x fduScanEPhase%d()\n", XEiJ.regPC0, abuNumber);
      }
      //E-Phase
      fdcStatus = (FDC_RQM | FDC_MPU_TO_FDC | FDC_CB |
                   (fdcStatus & (FDC_D3B | FDC_D2B | FDC_D1B | FDC_D0B)));
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("\tfdcStatus=%s\n", fdcStatusToString (fdcStatus));
      }
      //転送
      fdcReadHandle = null;
      fdcWriteHandle = fdcTempBuffer;
      fdcIndex = fdcStart = 0;
      fdcLimit = 128 << fduPNN;
      HD63450.dmaFallREQ (0);  //DMA転送開始
    }  //fduScanEPhase()

    //fduScanEqualEPhaseEnd ()
    public void fduScanEqualEPhaseEnd () {
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("%08x fduScanEqualEPhaseEnd%d()\n", XEiJ.regPC0, abuNumber);
      }
      int o = fduCalcOffset ();
      if (o < 0) {  //セクタが存在しない
        fduEPhaseEnd (FDC_ST0_AT | FDC_ST1_MA | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      //比較
    scan:
      {
        int l = 128 << fduPNN;
        for (int i = 0; i < l; i++) {
          int d = fdcTempBuffer[i] & 255;
          if (d != 0xff && (fduImage[o + i] & 255) != d) {
            break scan;
          }
        }
        //条件成立
        HD63450.dmaRiseREQ (0);  //DMA転送終了
        fduEPhaseEnd (FDC_ST0_NT | FDC_ST2_SH | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      if (fduPRN == fduEOT) {  //条件不成立
        HD63450.dmaRiseREQ (0);  //DMA転送終了
        fduEPhaseEnd (FDC_ST0_NT | FDC_ST2_SN | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      //継続
      fduPRN += fduSTP;
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("\tfduPRN%d=%d\n", abuNumber, fduPRN);
      }
      fdcIndex = fdcStart = 0;
      //fdcLimit = 128 << fduPNN;
    }  //fduScanEqualEPhaseEnd()

    //fduScanLowOrEqualEPhaseEnd ()
    public void fduScanLowOrEqualEPhaseEnd () {
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("%08x fduScanLowOrEqualEPhaseEnd%d()\n", XEiJ.regPC0, abuNumber);
      }
      int o = fduCalcOffset ();
      if (o < 0) {  //セクタが存在しない
        fduEPhaseEnd (FDC_ST0_AT | FDC_ST1_MA | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
    scan:
      {
        int l = 128 << fduPNN;
        for (int i = 0; i < l; i++) {
          int d = fdcTempBuffer[i] & 255;
          if (d != 0xff && (fduImage[o + i] & 255) > d) {
            break scan;
          }
        }
        //条件成立
        HD63450.dmaRiseREQ (0);  //DMA転送終了
        fduEPhaseEnd (FDC_ST0_NT | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      if (fduPRN == fduEOT) {  //条件不成立
        HD63450.dmaRiseREQ (0);  //DMA転送終了
        fduEPhaseEnd (FDC_ST0_NT | FDC_ST2_SN | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      //継続
      fduPRN += fduSTP;
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("\tfduPRN%d=%d\n", abuNumber, fduPRN);
      }
      fdcIndex = fdcStart = 0;
      fdcLimit = 128 << fduPNN;
    }  //fduScanLowOrEqualEPhaseEnd()

    //fduScanHighOrEqualEPhaseEnd ()
    public void fduScanHighOrEqualEPhaseEnd () {
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("%08x fduScanHighOrEqualEPhaseEnd%d()\n", XEiJ.regPC0, abuNumber);
      }
      int o = fduCalcOffset ();
      if (o < 0) {  //セクタが存在しない
        fduEPhaseEnd (FDC_ST0_AT | FDC_ST1_MA | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
    scan:
      {
        int l = 128 << fduPNN;
        for (int i = 0; i < l; i++) {
          int d = fdcTempBuffer[i] & 255;
          if (d != 0xff && (fduImage[o + i] & 255) < d) {
            break scan;
          }
        }
        //条件成立
        HD63450.dmaRiseREQ (0);  //DMA転送終了
        fduEPhaseEnd (FDC_ST0_NT | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      if (fduPRN == fduEOT) {  //条件不成立
        HD63450.dmaRiseREQ (0);  //DMA転送終了
        fduEPhaseEnd (FDC_ST0_NT | FDC_ST2_SN | ((fduPHN & 1) << 2 | abuNumber) << 24);
        return;
      }
      //継続
      fduPRN += fduSTP;
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("\tfduPRN%d=%d\n", abuNumber, fduPRN);
      }
      fdcIndex = fdcStart = 0;
      fdcLimit = 128 << fduPNN;
    }  //fduScanHighOrEqualEPhaseEnd()

    //offset = fduCalcOffset ()
    //  指定されたセクタのオフセットを計算する。-1=範囲外
    public int fduCalcOffset () {
      int o = (fduMedia == null ? -1 :
               fduDoubleDensity == fduMedia.fdmDoubleDensity &&
               fduPNN == fduMedia.fdmSectorScale &&
               fduPCN < fduMedia.fdmCylindersPerDisk &&
               (fduMedia == FDMedia.FDM_2HDE ?
                fduPHN == (fduPCN == 0 && fduPRN == 1 ? 0 : 128) || fduPHN == 129 :
                fduPHN < fduMedia.fdmTracksPerCylinder) &&
               (fduMedia == FDMedia.FDM_2HS ?
                fduPCN == 0 && fduPHN == 0 ? fduPRN == 1 || (11 <= fduPRN && fduPRN <= 18) : 10 <= fduPRN && fduPRN <= 18 :
                1 <= fduPRN && fduPRN <= fduMedia.fdmSectorsPerTrack) ?
               fduMedia.fdmBytesPerSector * (
                 (fduPRN <= fduMedia.fdmSectorsPerTrack ? fduPRN : fduPRN - fduMedia.fdmSectorsPerTrack) - 1 +
                 fduMedia.fdmSectorsPerTrack * (
                   ((fduPHN & 1) + fduMedia.fdmTracksPerCylinder * fduPCN))) :
               -1);
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("\tfduCalcOffset(C=%d H=%d R=%d N=%d)=%d\n", fduPCN, fduPHN, fduPRN, fduPNN, o);
      }
      return o;
    }  //fduCalcOffset()

    //fduEPhaseEnd (status)
    //  E-Phaseを終了してR-Phaseに移行する
    public void fduEPhaseEnd (int status) {
      if (FDC_DEBUG_TRACE && fdcDebugLogOn) {
        System.out.printf ("%08x fduEPhaseEnd%d(%s)\n", XEiJ.regPC0, abuNumber, fdcResultStatusToString (status));
      }
      //READ DATAやWRITE DATAなどの転送コマンドのE-Phase(転送中)が終了したとき
      //リザルトステータス(ST0,ST1,ST2,C,H,R,N)を作る
      fdcResultBuffer[0] = (byte) (status >> 24);  //ST0
      fdcResultBuffer[1] = (byte) (status >> 16);  //ST1
      fdcResultBuffer[2] = (byte) (status >> 8);  //ST2
      fdcResultBuffer[3] = (byte) fduPCN;  //C
      fdcResultBuffer[4] = (byte) fduPHN;  //H
      fdcResultBuffer[5] = (byte) fduPRN;  //R
      fdcResultBuffer[6] = (byte) fduPNN;  //N
      //R-Phase(RQM=1,DIO=1(FDC→MPU))に移行する
      fdcRPhase (7);
      //FDC割り込み要求(INT)を1にする
      IOInterrupt.ioiFdcRise ();
    }  //fduEPhaseEnd(int)


  }  //class FDUnit



  public static int fdcPeekByte (int a) {
    a &= XEiJ.BUS_MOTHER_MASK;
    switch (a) {
    case FDC_STATUS_PORT:  //0x00e94001
      return fdcPeekStatus ();
    case FDC_DATA_PORT:  //0x00e94003
      return fdcPeekData ();
    case FDC_DRIVE_STATUS:  //0x00e94005
      return fdcPeekDriveStatus ();
    }
    return 0xff;
  }  //fdcPeekByte

  public static int fdcReadByte (int a) {
    a &= XEiJ.BUS_MOTHER_MASK;
    switch (a) {
    case FDC_STATUS_PORT:  //0x00e94001
      return fdcReadStatus ();
    case FDC_DATA_PORT:  //0x00e94003
      return fdcReadData ();
    case FDC_DRIVE_STATUS:  //0x00e94005
      return fdcReadDriveStatus ();
    }
    return 0xff;
  }  //fdcReadByte

  public static void fdcWriteByte (int a, int d) {
    a &= XEiJ.BUS_MOTHER_MASK;
    switch (a) {
    case FDC_STATUS_PORT:  //0x00e94001
      fdcWriteCommand (d);
      break;
    case FDC_DATA_PORT:  //0x00e94003
      fdcWriteData (d);
      break;
    case FDC_DRIVE_STATUS:  //0x00e94005
      fdcWriteDriveControl (d);
      break;
    case FDC_DRIVE_SELECT:  //0x00e94007
      fdcWriteDriveSelect (d);
      break;
    }
  }  //fdcWriteByte




}  //class FDC



